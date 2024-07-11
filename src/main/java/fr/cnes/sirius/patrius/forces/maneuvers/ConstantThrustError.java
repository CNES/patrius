/**
 * 
 * Copyright 2011-2022 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * 
 * @history created 17/10/2014
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3172:10/05/2022:[PATRIUS] Ajout d'un throws PatriusException a la methode init de l'interface
 * EventDetector
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.8:DM:DM-3040:15/11/2021:[PATRIUS]Reversement des evolutions de la branche patrius-for-lotus 
 * VERSION:4.5:DM:DM-2445:27/05/2020:optimisation de SolarActivityReader 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:305:17/10/2014:Partial derivatives of constant thrust error wrt thrust parameters
 * VERSION::DM:284:06/01/2015:New architecture for parameterizable Parameters
 * VERSION::FA:410:16/04/2015: Anomalies in the Patrius Javadoc
 * VERSION::FA:452:12/06/2015:Partial derivatives computation only when firing
 * VERSION::FA:462:12/06/2015:Thrust error frame defined with a LOF type
 * VERSION::FA:500:22/09/2015:New management of frames in acceleration computation
 * VERSION::FA:487:06/11/2015:Start/Stop maneuver correction
 * VERSION::DM:424:10/11/2015:Event detectors for maneuvers start and end
 * VERSION::FA:453:13/11/2015:Handling propagation starting during a maneuver
 * VERSION::DM:454:24/11/2015:Overload method shouldBeRemoved()
 * VERSION::DM:534:10/02/2016:Parametrization of force models
 * VERSION::DM:570:05/04/2017:add PropulsiveProperty and TankProperty
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.maneuvers;

import fr.cnes.sirius.patrius.forces.ForceModel;
import fr.cnes.sirius.patrius.forces.GradientModel;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.frames.LocalOrbitalFrame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.parameter.ConstantFunction;
import fr.cnes.sirius.patrius.math.parameter.IParamDiffFunction;
import fr.cnes.sirius.patrius.math.parameter.JacobiansParameterizable;
import fr.cnes.sirius.patrius.math.parameter.LinearFunction;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.parameter.ParameterUtils;
import fr.cnes.sirius.patrius.math.parameter.StandardFieldDescriptors;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.AbstractDetector;
import fr.cnes.sirius.patrius.propagation.events.DateDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.propagation.numerical.TimeDerivativesEquations;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * <p>
 * This class is a model of the error of a simple maneuver with constant thrust.
 * </p>
 * <p>
 * The architecture of this force model is similar to ConstantThrustManeuver class.
 * </p>
 * 
 * <p>
 * The maneuver is associated to two triggering {@link fr.cnes.sirius.patrius.propagation.events.EventDetector
 * EventDetector} (one to start the thrust, the other one to stop the thrust): the maneuver is triggered <b>only if</b>
 * the underlying event generates a {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#STOP STOP}
 * event, in which case this class will generate a
 * {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#RESET_STATE RESET_STATE} event (the stop event
 * from the underlying object is therefore filtered out).
 * </p>
 * 
 * @concurrency not thread-safe
 * 
 * @concurrency.comment internal mutable attributes
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id: ConstantThrustError.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 2.3
 * 
 */
@SuppressWarnings("PMD.NullAssignment")
public final class ConstantThrustError extends JacobiansParameterizable implements ForceModel, GradientModel {

    /** Serializable UID. */
    private static final long serialVersionUID = -5479613740327735623L;

    /** Firing start detector. */
    private final EventDetector firingStartDetector;

    /** Firing stop detector. */
    private final EventDetector firingStopDetector;

    /**
     * Frame of the acceleration direction
     * If null and no LOF has been provided, the acceleration is expressed in the satellite frame.
     */
    private final Frame maneuverFrame;

    /**
     * Local orbital frame type.
     * If null and no frame has been provided, the acceleration is expressed in the satellite frame.
     */
    private final LOFType frameLofType;

    /** The parameterizable and differentiable function representing the x component of the thrust error. */
    private final IParamDiffFunction errorx;

    /** The parameterizable and differentiable function representing the y component of the thrust error. */
    private final IParamDiffFunction errory;

    /** The parameterizable and differentiable function representing the z component of the thrust error. */
    private final IParamDiffFunction errorz;

    /** State of the maneuver engine. */
    private boolean firing;

    // =================== Constructors with date and duration =================== //

    /**
     * Create a constant thrust error model whose x, y and z components are parameterizable and differentiable function.
     * <p>
     * Errors components are expressed in spacecraft frame.
     * </p>
     * 
     * @param date
     *        maneuver date
     * @param duration
     *        the duration of the thrust (s) (if negative, the date is considered to be the stop date)
     * @param fx
     *        the parameterizable and differentiable function representing the x component of the thrust error.
     * @param fy
     *        the parameterizable and differentiable function representing the y component of the thrust error.
     * @param fz
     *        the parameterizable and differentiable function representing the z component of the thrust error.
     */
    public ConstantThrustError(final AbsoluteDate date, final double duration,
        final IParamDiffFunction fx, final IParamDiffFunction fy, final IParamDiffFunction fz) {
        this(date, duration, null, null, fx, fy, fz);
    }

    /**
     * Create a constant thrust error model whose x, y and z components are linear functions:<br>
     * fx = ax*t + bx<br>
     * fy = ay*t + by<br>
     * fz = az*t + bz<br>
     * <p>
     * Errors components are expressed in spacecraft frame.
     * </p>
     * 
     * @param date
     *        maneuver date
     * @param duration
     *        the duration of the thrust (s) (if negative, the date is considered to be the stop date)
     * @param ax
     *        the parameter representing the slope of the linear function
     *        for the x component of the constant thrust error.
     * @param bx
     *        the parameter representing the zero value of the linear function
     *        for the x component of the constant thrust error.
     * @param ay
     *        the parameter representing the slope of the linear function
     *        for the y component of the constant thrust error.
     * @param by
     *        the parameter representing the zero value of the linear function
     *        for the y component of the constant thrust error.
     * @param az
     *        the parameter representing the slope of the linear function
     *        for the z component of the constant thrust error.
     * @param bz
     *        the parameter representing the zero value of the linear function
     *        for the z component of the constant thrust error.
     * @param date0
     *        the zero value date
     */
    public ConstantThrustError(final AbsoluteDate date, final double duration,
        final Parameter ax, final Parameter bx, final Parameter ay, final Parameter by,
        final Parameter az, final Parameter bz, final AbsoluteDate date0) {
        this(date, duration, null, null, new LinearFunction(date0, bx, ax), new LinearFunction(
                date0, by, ay), new LinearFunction(date0, bz, az));
    }

    /**
     * Create a constant thrust error model whose x, y and z components are linear functions:<br>
     * fx = ax*t + bx<br>
     * fy = ay*t + by<br>
     * fz = az*t + bz<br>
     * <p>
     * Errors components are expressed in spacecraft frame.
     * </p>
     * 
     * @param date
     *        maneuver date
     * @param duration
     *        the duration of the thrust (s) (if negative, the date is considered to be the stop date)
     * @param ax
     *        the slope of the linear function for the x component of the constant thrust error.
     * @param bx
     *        the zero value of the linear function for the x component of the constant thrust error.
     * @param ay
     *        the slope of the linear function for the y component of the constant thrust error.
     * @param by
     *        the zero value of the linear function for the y component of the constant thrust error
     * @param az
     *        the slope of the linear function for the z component of the constant thrust error
     * @param bz
     *        the zero value of the linear function for the z component of the constant thrust error
     * @param date0
     *        the zero value date
     */
    public ConstantThrustError(final AbsoluteDate date, final double duration,
        final double ax, final double bx, final double ay, final double by,
        final double az, final double bz, final AbsoluteDate date0) {
        this(date, duration, null, null, new LinearFunction(date0, bx, ax), new LinearFunction(
                date0, by, ay), new LinearFunction(date0, bz, az));
    }

    /**
     * Create a constant thrust error model whose x, y and z components are constant functions:<br>
     * fx = cx <br>
     * fy = cy <br>
     * fz = cz <br>
     * <p>
     * Errors components are expressed in spacecraft frame.
     * </p>
     * 
     * @param date
     *        maneuver date
     * @param duration
     *        the duration of the thrust (s) (if negative, the date is considered to be the stop date)
     * @param cx
     *        the parameter for the x component of the constant thrust error
     * @param cy
     *        the parameter for the y component of the constant thrust error
     * @param cz
     *        the parameter for the z component of the constant thrust error
     */
    public ConstantThrustError(final AbsoluteDate date, final double duration,
        final Parameter cx, final Parameter cy, final Parameter cz) {
        this(date, duration, null, null, new ConstantFunction(cx), new ConstantFunction(cy),
            new ConstantFunction(cz));
    }

    /**
     * Create a constant thrust error model whose x, y and z components are constant functions:<br>
     * fx = cx <br>
     * fy = cy <br>
     * fz = cz <br>
     * <p>
     * Errors components are expressed in spacecraft frame.
     * </p>
     * 
     * @param date
     *        maneuver date
     * @param duration
     *        the duration of the thrust (s) (if negative, the date is considered to be the stop date)
     * @param cx
     *        the x component of the constant thrust error
     * @param cy
     *        the y component of the constant thrust error
     * @param cz
     *        the z component of the constant thrust error
     */
    public ConstantThrustError(final AbsoluteDate date, final double duration,
        final double cx, final double cy, final double cz) {
        this(date, duration, null, null, new ConstantFunction(cx), new ConstantFunction(cy),
            new ConstantFunction(cz));
    }

    /**
     * Create a constant thrust error model whose x, y and z components are parameterizable and differentiable function.
     * <p>
     * Errors components are expressed in provided frame.
     * </p>
     * WARNING : This constructor must not be used with a {@link LocalOrbitalFrame},
     * acceleration computation would fail.
     * 
     * @param date
     *        maneuver date
     * @param duration
     *        the duration of the thrust (s) (if negative, the date is considered to be the stop date)
     * @param frame
     *        the maneuver frame
     * @param fx
     *        the parameterizable and differentiable function representing the x component of the thrust error.
     * @param fy
     *        the parameterizable and differentiable function representing the y component of the thrust error.
     * @param fz
     *        the parameterizable and differentiable function representing the z component of the thrust error.
     */
    public ConstantThrustError(final AbsoluteDate date, final double duration, final Frame frame,
        final IParamDiffFunction fx, final IParamDiffFunction fy, final IParamDiffFunction fz) {
        this(date, duration, frame, null, fx, fy, fz);
    }

    /**
     * Create a constant thrust error model whose x, y and z components are linear functions:<br>
     * fx = ax*t + bx<br>
     * fy = ay*t + by<br>
     * fz = az*t + bz<br>
     * <p>
     * Errors components are expressed in provided frame.
     * </p>
     * WARNING : This constructor must not be used with a {@link LocalOrbitalFrame},<br>
     * acceleration computation would fail.<br>
     * 
     * @param date
     *        maneuver date
     * @param duration
     *        the duration of the thrust (s) (if negative, the date is considered to be the stop date)
     * @param frame
     *        the maneuver frame
     * @param ax
     *        the parameter representing the slope of the linear function
     *        for the x component of the constant thrust error.
     * @param bx
     *        the parameter representing the zero value of the linear function
     *        for the x component of the constant thrust error.
     * @param ay
     *        the parameter representing the slope of the linear function
     *        for the y component of the constant thrust error.
     * @param by
     *        the parameter representing the zero value of the linear function
     *        for the y component of the constant thrust error.
     * @param az
     *        the parameter representing the slope of the linear function
     *        for the z component of the constant thrust error.
     * @param bz
     *        the parameter representing the zero value of the linear function
     *        for the z component of the constant thrust error.
     * @param date0
     *        the zero value date
     */
    public ConstantThrustError(final AbsoluteDate date, final double duration, final Frame frame,
        final Parameter ax, final Parameter bx, final Parameter ay, final Parameter by,
        final Parameter az, final Parameter bz, final AbsoluteDate date0) {
        this(date, duration, frame, new LinearFunction(date0, bx, ax), new LinearFunction(date0,
                by, ay), new LinearFunction(date0, bz, az));
    }

    /**
     * Create a constant thrust error model whose x, y and z components are linear functions:<br>
     * fx = ax*t + bx<br>
     * fy = ay*t + by<br>
     * fz = az*t + bz<br>
     * <p>
     * Errors components are expressed in provided frame.
     * </p>
     * WARNING : This constructor must not be used with a {@link LocalOrbitalFrame},<br>
     * acceleration computation would fail.
     * 
     * @param date
     *        maneuver date
     * @param duration
     *        the duration of the thrust (s) (if negative, the date is considered to be the stop date)
     * @param frame
     *        the maneuver frame
     * @param ax
     *        the slope of the linear function for the x component of the constant thrust error.
     * @param bx
     *        the zero value of the linear function for the x component of the constant thrust error.
     * @param ay
     *        the slope of the linear function for the y component of the constant thrust error.
     * @param by
     *        the zero value of the linear function for the y component of the constant thrust error
     * @param az
     *        the slope of the linear function for the z component of the constant thrust error
     * @param bz
     *        the zero value of the linear function for the z component of the constant thrust error
     * @param date0
     *        the zero value date
     */
    public ConstantThrustError(final AbsoluteDate date, final double duration, final Frame frame,
        final double ax, final double bx, final double ay, final double by,
        final double az, final double bz, final AbsoluteDate date0) {
        this(date, duration, frame, new LinearFunction(date0, bx, ax), new LinearFunction(date0,
                by, ay), new LinearFunction(date0, bz, az));
    }

    /**
     * Create a constant thrust error model whose x, y and z components are constant functions:<br>
     * fx = cx <br>
     * fy = cy <br>
     * fz = cz <br>
     * <p>
     * Errors components are expressed in provided frame.
     * </p>
     * WARNING : This constructor must not be used with a {@link LocalOrbitalFrame},<br>
     * acceleration computation would fail.<br>
     * 
     * @param date
     *        maneuver date
     * @param duration
     *        the duration of the thrust (s) (if negative, the date is considered to be the stop date)
     * @param frame
     *        the maneuver frame
     * @param cx
     *        the parameter for the x component of the constant thrust error
     * @param cy
     *        the parameter for the y component of the constant thrust error
     * @param cz
     *        the parameter for the z component of the constant thrust error
     */
    public ConstantThrustError(final AbsoluteDate date, final double duration, final Frame frame,
        final Parameter cx, final Parameter cy, final Parameter cz) {
        this(date, duration, frame, new ConstantFunction(cx), new ConstantFunction(cy),
            new ConstantFunction(cz));
    }

    /**
     * Create a constant thrust error model whose x, y and z components are constant functions:<br>
     * fx = cx <br>
     * fy = cy <br>
     * fz = cz <br>
     * <p>
     * Errors components are expressed in provided frame.
     * </p>
     * WARNING : This constructor must not be used with a {@link LocalOrbitalFrame},<br>
     * acceleration computation would fail.<br>
     * 
     * @param date
     *        maneuver date
     * @param duration
     *        the duration of the thrust (s) (if negative, the date is considered to be the stop date)
     * @param frame
     *        the maneuver frame
     * @param cx
     *        the x component of the constant thrust error
     * @param cy
     *        the y component of the constant thrust error
     * @param cz
     *        the z component of the constant thrust error
     */
    public ConstantThrustError(final AbsoluteDate date, final double duration, final Frame frame,
        final double cx, final double cy, final double cz) {
        this(date, duration, frame, new ConstantFunction(cx), new ConstantFunction(cy),
            new ConstantFunction(cz));
    }

    /**
     * Create a constant thrust error model whose x, y and z components are parameterizable and differentiable function.
     * <p>
     * Errors components are expressed in provided local orbital frame.
     * </p>
     * 
     * @param date
     *        maneuver date
     * @param duration
     *        the duration of the thrust (s) (if negative, the date is considered to be the stop date)
     * @param lofType
     *        the LOF type of the maneuver frame
     * @param fx
     *        the parameterizable and differentiable function representing the x component of the thrust error.
     * @param fy
     *        the parameterizable and differentiable function representing the y component of the thrust error.
     * @param fz
     *        the parameterizable and differentiable function representing the z component of the thrust error.
     */
    public ConstantThrustError(final AbsoluteDate date, final double duration, final LOFType lofType,
        final IParamDiffFunction fx, final IParamDiffFunction fy, final IParamDiffFunction fz) {
        this(date, duration, null, lofType, fx, fy, fz);
    }

    /**
     * Create a constant thrust error model whose x, y and z components are linear functions:<br>
     * fx = ax*t + bx<br>
     * fy = ay*t + by<br>
     * fz = az*t + bz<br>
     * <p>
     * Errors components are expressed in provided local orbital frame.
     * </p>
     * 
     * @param date
     *        maneuver date
     * @param duration
     *        the duration of the thrust (s) (if negative, the date is considered to be the stop date)
     * @param lofType
     *        the LOF type of the maneuver frame
     * @param ax
     *        the parameter representing the slope of the linear function
     *        for the x component of the constant thrust error
     * @param bx
     *        the parameter representing the zero value of the linear function
     *        for the x component of the constant thrust error
     * @param ay
     *        the parameter representing the slope of the linear function
     *        for the y component of the constant thrust error
     * @param by
     *        the parameter representing the zero value of the linear function
     *        for the y component of the constant thrust error
     * @param az
     *        the parameter representing the slope of the linear function
     *        for the z component of the constant thrust error
     * @param bz
     *        the parameter representing the zero value of the linear function
     *        for the z component of the constant thrust error
     * @param date0
     *        the zero value date
     */
    public ConstantThrustError(final AbsoluteDate date, final double duration, final LOFType lofType,
        final Parameter ax, final Parameter bx, final Parameter ay, final Parameter by,
        final Parameter az, final Parameter bz, final AbsoluteDate date0) {
        this(date, duration, lofType, new LinearFunction(date0, bx, ax), new LinearFunction(date0,
                by, ay), new LinearFunction(date0, bz, az));
    }

    /**
     * Create a constant thrust error model whose x, y and z components are linear functions:<br>
     * fx = ax*t + bx<br>
     * fy = ay*t + by<br>
     * fz = az*t + bz<br>
     * <p>
     * Errors components are expressed in provided local orbital frame.
     * </p>
     * 
     * @param date
     *        maneuver date
     * @param duration
     *        the duration of the thrust (s) (if negative, the date is considered to be the stop date)
     * @param lofType
     *        the LOF type of the maneuver frame
     * @param ax
     *        the slope of the linear function
     *        for the x component of the constant thrust error
     * @param bx
     *        the zero value of the linear function
     *        for the x component of the constant thrust error
     * @param ay
     *        the slope of the linear function
     *        for the y component of the constant thrust error
     * @param by
     *        the zero value of the linear function
     *        for the y component of the constant thrust error
     * @param az
     *        the slope of the linear function
     *        for the z component of the constant thrust error
     * @param bz
     *        the zero value of the linear function
     *        for the z component of the constant thrust error
     * @param date0
     *        the zero value date
     */
    public ConstantThrustError(final AbsoluteDate date, final double duration, final LOFType lofType,
        final double ax, final double bx, final double ay, final double by,
        final double az, final double bz, final AbsoluteDate date0) {
        this(date, duration, lofType, new LinearFunction(date0, bx, ax), new LinearFunction(date0,
                by, ay), new LinearFunction(date0, bz, az));
    }

    /**
     * Create a constant thrust error model whose x, y and z components are constant functions:<br>
     * fx = cx <br>
     * fy = cy <br>
     * fz = cz <br>
     * <p>
     * Errors components are expressed in provided local orbital frame.
     * </p>
     * 
     * @param date
     *        maneuver date
     * @param duration
     *        the duration of the thrust (s) (if negative, the date is considered to be the stop date)
     * @param lofType
     *        the LOF type of the maneuver frame
     * @param cx
     *        the parameter for the x component of the constant thrust error
     * @param cy
     *        the parameter for the y component of the constant thrust error
     * @param cz
     *        the parameter for the z component of the constant thrust error
     */
    public ConstantThrustError(final AbsoluteDate date, final double duration, final LOFType lofType,
        final Parameter cx, final Parameter cy, final Parameter cz) {
        this(date, duration, lofType, new ConstantFunction(cx), new ConstantFunction(cy),
            new ConstantFunction(cz));
    }

    /**
     * Create a constant thrust error model whose x, y and z components are constant functions:<br>
     * fx = cx <br>
     * fy = cy <br>
     * fz = cz <br>
     * <p>
     * Errors components are expressed in provided local orbital frame.
     * </p>
     * 
     * @param date
     *        maneuver date
     * @param duration
     *        the duration of the thrust (s) (if negative, the date is considered to be the stop date)
     * @param lofType
     *        the LOF type of the maneuver frame
     * @param cx
     *        the x component of the constant thrust error
     * @param cy
     *        the y component of the constant thrust error
     * @param cz
     *        the z component of the constant thrust error
     */
    public ConstantThrustError(final AbsoluteDate date, final double duration, final LOFType lofType,
        final double cx, final double cy, final double cz) {
        this(date, duration, lofType, new ConstantFunction(cx), new ConstantFunction(cy),
            new ConstantFunction(cz));
    }

    /**
     * Private constructor to handle all cases.
     * 
     * @param date
     *        maneuver date
     * @param duration
     *        the duration of the thrust (s) (if negative, the date is considered to be the stop date)
     * @param frame
     *        the maneuver frame
     * @param lofType
     *        the LOF type of the maneuver frame
     * @param fx
     *        the parameterizable and differentiable function representing the x component of the thrust error.
     * @param fy
     *        the parameterizable and differentiable function representing the y component of the thrust error.
     * @param fz
     *        the parameterizable and differentiable function representing the z component of the thrust error.
     */
    private ConstantThrustError(final AbsoluteDate date, final double duration, final Frame frame,
        final LOFType lofType, final IParamDiffFunction fx, final IParamDiffFunction fy,
        final IParamDiffFunction fz) {
        super();
        this.addJacobiansParameter(fx.getParameters());
        this.addJacobiansParameter(fy.getParameters());
        this.addJacobiansParameter(fz.getParameters());
        ParameterUtils.addFieldToParameters(getParameters(), StandardFieldDescriptors.FORCE_MODEL, this.getClass());
        if (duration >= 0) {
            this.firingStartDetector = new DateDetector(date);
            this.firingStopDetector = new DateDetector(date.shiftedBy(duration));
        } else {
            this.firingStopDetector = new DateDetector(date);
            this.firingStartDetector = new DateDetector(date.shiftedBy(duration));
        }
        this.errorx = fx;
        this.errory = fy;
        this.errorz = fz;
        this.maneuverFrame = frame;
        this.frameLofType = lofType;
        this.firing = false;
    }

    // =================== Constructors with event detectors =================== //

    /**
     * Create a constant thrust error model whose x, y and z components are constant functions:<br>
     * fx = cx <br>
     * fy = cy <br>
     * fz = cz <br>
     * <p>
     * Errors components are expressed in spacecraft frame.
     * </p>
     * 
     * @param startEventDetector
     *        event detector upon which thrust should starts
     * @param stopEventDetector
     *        event detector upon which thrust should stops
     * @param fx
     *        the parameterizable and differentiable function representing the x component of the thrust error.
     * @param fy
     *        the parameterizable and differentiable function representing the y component of the thrust error.
     * @param fz
     *        the parameterizable and differentiable function representing the z component of the thrust error.
     */
    public ConstantThrustError(final EventDetector startEventDetector, final EventDetector stopEventDetector,
        final IParamDiffFunction fx, final IParamDiffFunction fy, final IParamDiffFunction fz) {
        this(startEventDetector, stopEventDetector, null, null, fx, fy, fz);
    }

    /**
     * Create a constant thrust error model whose x, y and z components are constant functions:<br>
     * fx = cx <br>
     * fy = cy <br>
     * fz = cz <br>
     * <p>
     * Errors components are expressed in spacecraft frame.
     * </p>
     * 
     * @param startEventDetector
     *        event detector upon which thrust should starts
     * @param stopEventDetector
     *        event detector upon which thrust should stops
     * @param cx
     *        the x component of the constant thrust error
     * @param cy
     *        the y component of the constant thrust error
     * @param cz
     *        the z component of the constant thrust error
     */
    public ConstantThrustError(final EventDetector startEventDetector, final EventDetector stopEventDetector,
        final Parameter cx, final Parameter cy, final Parameter cz) {
        this(startEventDetector, stopEventDetector, null, null, new ConstantFunction(cx), new ConstantFunction(cy),
            new ConstantFunction(cz));
    }

    /**
     * Create a constant thrust error model whose x, y and z components are constant functions:<br>
     * fx = cx <br>
     * fy = cy <br>
     * fz = cz <br>
     * <p>
     * Errors components are expressed in provided frame.
     * </p>
     * WARNING : This constructor must not be used with a {@link LocalOrbitalFrame},<br>
     * acceleration computation would fail.<br>
     * 
     * @param startEventDetector
     *        event detector upon which thrust should starts
     * @param stopEventDetector
     *        event detector upon which thrust should stops
     * @param frame
     *        the maneuver frame
     * @param fx
     *        the parameterizable and differentiable function representing the x component of the thrust error.
     * @param fy
     *        the parameterizable and differentiable function representing the y component of the thrust error.
     * @param fz
     *        the parameterizable and differentiable function representing the z component of the thrust error.
     */
    public ConstantThrustError(final EventDetector startEventDetector, final EventDetector stopEventDetector,
        final Frame frame, final IParamDiffFunction fx, final IParamDiffFunction fy, final IParamDiffFunction fz) {
        this(startEventDetector, stopEventDetector, frame, null, fx, fy, fz);
    }

    /**
     * Create a constant thrust error model whose x, y and z components are constant functions:<br>
     * fx = cx <br>
     * fy = cy <br>
     * fz = cz <br>
     * <p>
     * Errors components are expressed in provided frame.
     * </p>
     * WARNING : This constructor must not be used with a {@link LocalOrbitalFrame},<br>
     * acceleration computation would fail.<br>
     * 
     * @param startEventDetector
     *        event detector upon which thrust should starts
     * @param stopEventDetector
     *        event detector upon which thrust should stops
     * @param frame
     *        the maneuver frame
     * @param cx
     *        the x component of the constant thrust error
     * @param cy
     *        the y component of the constant thrust error
     * @param cz
     *        the z component of the constant thrust error
     */
    public ConstantThrustError(final EventDetector startEventDetector, final EventDetector stopEventDetector,
        final Frame frame, final Parameter cx, final Parameter cy, final Parameter cz) {
        this(startEventDetector, stopEventDetector, frame, null, new ConstantFunction(cx), new ConstantFunction(cy),
            new ConstantFunction(cz));
    }

    /**
     * Create a constant thrust error model whose x, y and z components are constant functions:<br>
     * fx = cx <br>
     * fy = cy <br>
     * fz = cz <br>
     * <p>
     * Errors components are expressed in provided local orbital frame.
     * </p>
     * 
     * @param startEventDetector
     *        event detector upon which thrust should starts
     * @param stopEventDetector
     *        event detector upon which thrust should stops
     * @param lofType
     *        the LOF type of the maneuver frame
     * @param fx
     *        the parameterizable and differentiable function representing the x component of the thrust error.
     * @param fy
     *        the parameterizable and differentiable function representing the y component of the thrust error.
     * @param fz
     *        the parameterizable and differentiable function representing the z component of the thrust error.
     */
    public ConstantThrustError(final EventDetector startEventDetector, final EventDetector stopEventDetector,
        final LOFType lofType, final IParamDiffFunction fx, final IParamDiffFunction fy,
        final IParamDiffFunction fz) {
        this(startEventDetector, stopEventDetector, null, lofType, fx, fy, fz);
    }

    /**
     * Create a constant thrust error model whose x, y and z components are constant functions:<br>
     * fx = cx <br>
     * fy = cy <br>
     * fz = cz <br>
     * <p>
     * Errors components are expressed in provided local orbital frame.
     * </p>
     * 
     * @param startEventDetector
     *        event detector upon which thrust should starts
     * @param stopEventDetector
     *        event detector upon which thrust should stops
     * @param lofType
     *        the LOF type of the maneuver frame
     * @param cx
     *        the x component of the constant thrust error
     * @param cy
     *        the y component of the constant thrust error
     * @param cz
     *        the z component of the constant thrust error
     */
    public ConstantThrustError(final EventDetector startEventDetector, final EventDetector stopEventDetector,
        final LOFType lofType, final Parameter cx, final Parameter cy, final Parameter cz) {
        this(startEventDetector, stopEventDetector, null, lofType, new ConstantFunction(cx), new ConstantFunction(cy),
            new ConstantFunction(cz));
    }

    /**
     * Private constructor to handle all cases.
     * 
     * @param startEventDetector
     *        event detector upon which thrust should starts
     * @param stopEventDetector
     *        event detector upon which thrust should stops
     * @param frame
     *        the maneuver frame
     * @param lofType
     *        the LOF type of the maneuver frame
     * @param fx
     *        the parameterizable and differentiable function representing the x component of the thrust error.
     * @param fy
     *        the parameterizable and differentiable function representing the y component of the thrust error.
     * @param fz
     *        the parameterizable and differentiable function representing the z component of the thrust error.
     */
    private ConstantThrustError(final EventDetector startEventDetector, final EventDetector stopEventDetector,
        final Frame frame, final LOFType lofType, final IParamDiffFunction fx, final IParamDiffFunction fy,
        final IParamDiffFunction fz) {
        super();
        this.addJacobiansParameter(fx.getParameters());
        this.addJacobiansParameter(fy.getParameters());
        this.addJacobiansParameter(fz.getParameters());
        ParameterUtils.addFieldToParameters(getParameters(), StandardFieldDescriptors.FORCE_MODEL, this.getClass());
        this.firingStartDetector = startEventDetector;
        this.firingStopDetector = stopEventDetector;
        this.errorx = fx;
        this.errory = fy;
        this.errorz = fz;
        this.maneuverFrame = frame;
        this.frameLofType = lofType;
        this.firing = false;
    }

    /**
     * Set maneuver status. This method is meant to be used if the propagation starts during a maneuver.
     * As a result thrust will be firing at the beginning of the propagation and stops when crossing stop event.
     * <p>
     * Used in conjunction with {@link #isFiring()}, the user can stop/restart a propagation during a maneuver.
     * </p>
     * 
     * @param isFiring
     *        true if propagation should start during the maneuver
     */
    public void setFiring(final boolean isFiring) {
        this.firing = isFiring;
    }

    /**
     * Returns maneuver status (firing or not).
     * <p>
     * Used in conjunction with {@link #setFiring(boolean)}, the user can stop/restart a propagation during a maneuver.
     * </p>
     * 
     * @return true if maneuver is thrust firing.
     */
    public boolean isFiring() {
        return this.firing;
    }

    /**
     * Return the maneuver start date (if a date or a {@link DateDetector} as been provided).
     * 
     * @return the maneuver start date if a date or a {@link DateDetector} as been provided, null otherwise.
     */
    public AbsoluteDate getStartDate() {
        return this.firingStartDetector instanceof DateDetector ? ((DateDetector) this.firingStartDetector).getDate()
            : null;
    }

    /**
     * Return the maneuver stop date (if a date or a {@link DateDetector} as been provided).
     * 
     * @return the maneuver stop date if a date or a {@link DateDetector} as been provided, null otherwise.
     */
    public AbsoluteDate getEndDate() {
        return this.firingStopDetector instanceof DateDetector ? ((DateDetector) this.firingStopDetector).getDate()
            : null;
    }

    /**
     * Compute the contribution of the constant thrust error model to the perturbing acceleration.
     * The contribution is zero when the date is outside the thrust time interval.
     * 
     * @param s
     *        current state information: date, kinematics, attitude
     * @param adder
     *        object where the contribution should be added
     * @exception PatriusException
     *            if some specific error occurs
     */
    @Override
    public void addContribution(final SpacecraftState s, final TimeDerivativesEquations adder) throws PatriusException {
        if (this.firing) {
            adder.addAcceleration(this.computeAcceleration(s), s.getFrame());
        }
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D computeAcceleration(final SpacecraftState s) throws PatriusException {
        // use the value() method to retrieve the fx, fy and fz values.
        final Vector3D errorVector = new Vector3D(this.errorx.value(s), this.errory.value(s), this.errorz.value(s));

        // Convert the error vector in inertial frame
        final Vector3D res;

        if (this.maneuverFrame == null) {
            if (this.frameLofType == null) {
                // Direction in satellite frame
                // Convert it into inertial frame
                res = s.getAttitude().getRotation().applyTo(errorVector);
            } else {
                // the maneuver frame is defined by a LOF type
                final Transform transform = this.frameLofType.transformFromInertial(s.getDate(), s.getPVCoordinates())
                    .getInverse();
                res = transform.transformVector(errorVector);
            }
        } else {
            // Direction in a frame defined by the user
            // Convert it into inertial frame (getFrame method of SpacecraftState
            final Transform transform = this.maneuverFrame.getTransformTo(s.getFrame(), s.getDate());
            res = transform.transformVector(errorVector);
        }
        return res;
    }

    /** {@inheritDoc} */
    @Override
    public EventDetector[] getEventsDetectors() {
        return new EventDetector[] { new FiringStartDetector(), new FiringStopDetector() };

    }

    /** {@inheritDoc} */
    @Override
    public void addDAccDState(final SpacecraftState state, final double[][] dAccdPos,
                              final double[][] dAccdVel) throws PatriusException {
        // does nothing
    }

    /** {@inheritDoc} */
    @Override
    public void addDAccDParam(final SpacecraftState state, final Parameter param,
                              final double[] dAccdParam) throws PatriusException {
        if (this.firing) {
            // throw an exception if the parameter is not handled:
            if (!this.supportsJacobianParameter(param)) {
                throw new PatriusException(PatriusMessages.UNKNOWN_PARAMETER, param);
            }
            // use the derivativeValue() method to retrieve the f'x, f'y and f'z values:
            final Vector3D der = new Vector3D(this.errorx.derivativeValue(param, state),
                this.errory.derivativeValue(param, state), this.errorz.derivativeValue(param, state));
            final Vector3D tder;

            // Convert the derivative error vector in inertial frame
            if (this.maneuverFrame == null) {
                if (this.frameLofType == null) {
                    // Direction in satellite frame
                    // Convert it into inertial frame
                    tder = state.getAttitude().getRotation().applyTo(der);
                } else {
                    // the maneuver frame is defined by a LOF type
                    final Transform tranform = this.frameLofType.transformFromInertial(state.getDate(),
                        state.getPVCoordinates()).getInverse();
                    tder = tranform.transformVector(der);
                }
            } else {
                // the maneuver frame is defined by the user
                final Transform tranform = this.maneuverFrame.getTransformTo(state.getFrame(), state.getDate());
                tder = tranform.transformVector(der);
            }
            dAccdParam[0] += tder.getX();
            dAccdParam[1] += tder.getY();
            dAccdParam[2] += tder.getZ();
        }
        // ELSE : does nothing
    }

    /** {@inheritDoc} */
    @Override
    public boolean computeGradientPosition() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean computeGradientVelocity() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public void checkData(final AbsoluteDate start,
            final AbsoluteDate end) throws PatriusException {
        // Nothing to do
    }

    /** Detector for start of maneuver. */
    private class FiringStartDetector extends AbstractDetector {

        /** Serializable UID. */
        private static final long serialVersionUID = -6518235379334993498L;

        /** Flag indicating if propagation is forward. */
        private boolean isForward;

        /** Build an instance. */
        public FiringStartDetector() {
            super(ConstantThrustError.this.firingStartDetector.getSlopeSelection(),
                ConstantThrustError.this.firingStartDetector.getMaxCheckInterval(),
                ConstantThrustError.this.firingStartDetector.getThreshold());
        }

        /** {@inheritDoc} */
        @Override
        @SuppressWarnings("PMD.ShortMethodName")
        public double g(final SpacecraftState s) throws PatriusException {
            return ConstantThrustError.this.firingStartDetector.g(s);
        }

        /** {@inheritDoc} */
        @Override
        public void init(final SpacecraftState s0, final AbsoluteDate t) throws PatriusException {
            ConstantThrustError.this.firingStartDetector.init(s0, t);
        }

        /** {@inheritDoc} */
        @Override
        public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                    final boolean forward) throws PatriusException {

            // Get underlying action
            final Action underlyingAction =
                ConstantThrustError.this.firingStartDetector.eventOccurred(s, increasing, forward);

            this.isForward = forward;

            if (forward ^ ConstantThrustError.this.firing && underlyingAction == Action.STOP) {
                // Reset state if underlying action is "stop"
                return Action.RESET_STATE;
            }
            // Forward propagation : already firing
            // Backward propagation: not firing
            // Don't do anything
            return Action.CONTINUE;
        }

        /** {@inheritDoc} */
        @Override
        public SpacecraftState resetState(final SpacecraftState oldState) throws PatriusException {
            // Start the maneuver if the natural integration direction is forward
            // Stop the maneuver if the natural integration direction is backward
            ConstantThrustError.this.firing = this.isForward;
            return oldState;
        }

        /** {@inheritDoc} */
        @Override
        public boolean shouldBeRemoved() {
            return ConstantThrustError.this.firingStartDetector.shouldBeRemoved();
        }

        /** {@inheritDoc} */
        @Override
        public EventDetector copy() {
            return new FiringStartDetector();
        }
    }

    /** Detector for end of maneuver. */
    private class FiringStopDetector extends AbstractDetector {

        /** Serializable UID. */
        private static final long serialVersionUID = -8037677613943782679L;

        /** Flag indicating if propagation is forward. */
        private boolean isForward;

        /** Build an instance. */
        public FiringStopDetector() {
            super(ConstantThrustError.this.firingStopDetector.getSlopeSelection(),
                ConstantThrustError.this.firingStopDetector.getMaxCheckInterval(),
                ConstantThrustError.this.firingStopDetector.getThreshold());
        }

        /** {@inheritDoc} */
        @Override
        @SuppressWarnings("PMD.ShortMethodName")
        public double g(final SpacecraftState s) throws PatriusException {
            return ConstantThrustError.this.firingStopDetector.g(s);
        }

        /** {@inheritDoc} */
        @Override
        public void init(final SpacecraftState s0, final AbsoluteDate t) throws PatriusException {
            ConstantThrustError.this.firingStopDetector.init(s0, t);
        }

        /** {@inheritDoc} */
        @Override
        public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                    final boolean forward) throws PatriusException {

            // Get underlying action
            final Action underlyingAction =
                ConstantThrustError.this.firingStopDetector.eventOccurred(s, increasing, forward);

            this.isForward = forward;

            // Perform action only if still firing!
            if (forward ^ !ConstantThrustError.this.firing && underlyingAction == Action.STOP) {
                // Reset state if underlying action is "stop"
                return Action.RESET_STATE;
            }
            // Forward propagation : not firing
            // Backward propagation: already firing
            // Don't do anything
            return Action.CONTINUE;
        }

        /** {@inheritDoc} */
        @Override
        public SpacecraftState resetState(final SpacecraftState oldState) throws PatriusException {
            // Stop the maneuver if the natural integration direction is forward
            // Start the maneuver if the natural integration direction is backward
            ConstantThrustError.this.firing = !this.isForward;
            return oldState;
        }

        /** {@inheritDoc} */
        @Override
        public boolean shouldBeRemoved() {
            return ConstantThrustError.this.firingStopDetector.shouldBeRemoved();
        }

        /** {@inheritDoc} */
        @Override
        public EventDetector copy() {
            return new FiringStopDetector();
        }
    }
}
