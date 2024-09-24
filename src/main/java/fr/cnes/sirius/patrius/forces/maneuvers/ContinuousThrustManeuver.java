/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 * HISTORY
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11:FA:FA-3314:22/05/2023:[PATRIUS] Anomalie evaluation ForceModel SpacecraftState en ITRF
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3172:10/05/2022:[PATRIUS] Ajout d'un throws PatriusException a la methode init de l'interface
 * EventDetector
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.6:DM:DM-2571:27/01/2021:[PATRIUS] Integrateur Stormer-Cowell 
 * VERSION:4.5:FA:FA-2417:27/05/2020:ContinuousThrustManeuver
 * VERSION:4.5:FA:FA-2447:27/05/2020:Mathlib.divide() incomplète 
 * VERSION:4.5:DM:DM-2445:27/05/2020:optimisation de SolarActivityReader 
 * VERSION:4.4:DM:DM-2126:04/10/2019:[PATRIUS] Calcul du DeltaV realise
 * VERSION:4.4:DM:DM-2112:04/10/2019:[PATRIUS] Manoeuvres impulsionnelles sur increments orbitaux
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:93:08/08/2013:completed Javadoc concerning the partial derivatives parameters
 * VERSION::FA:86:22/10/2013:Added Additional equations methods to api
 * VERSION::DM:190:29/07/2014:Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:293:01/10/2014:Allowed users to define a maneuver by a inDirection in any frame
 * VERSION::DM:284:06/10/2014:New architecture for parameterizable Parameters
 * VERSION::FA:200:28/08/2014: dealing with a negative mass in the propagator
 * VERSION::FA:373:12/01/2015: proper handling of mass event detection
 * VERSION::FA:411:10/02/2015:javadoc
 * VERSION::FA:388:19/02/2015:Restored deprecated constructor + raised exception if SpacecraftFrame as input.
 * VERSION::FA:414:24/03/2015: proper handling of mass evolution
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:424:10/11/2015:Event detectors for maneuvers start and end
 * VERSION::FA:453:13/11/2015:Handling propagation starting during a maneuver
 * VERSION::FA:465:16/06/2015:Added analytical computation of partial derivatives
 * VERSION::FA:487:06/11/2015:Start/Stop maneuver correction
 * VERSION::FA:500:22/09/2015:Improved Javadoc
 * VERSION::DM:454:24/11/2015:Overload method shouldBeRemoved()
 * VERSION::DM:534:10/02/2016:Parametrization of force models
 * VERSION::DM:570:05/04/2017:add PropulsiveProperty and TankProperty
 * VERSION::DM:976:16/11/2017:Merge continuous maneuvers classes
 * VERSION::FA:1449:15/03/2018:remove TankProperty name attribute
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::FA:1851:09/10/2018: Fix thrust flow behavior to avoid numerical leaks
 * VERSION::FA:1774:22/10/2018: Javadoc correction
 * VERSION::FA:1970:03/01/2019:quality corrections
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.maneuvers;

import java.util.List;

import fr.cnes.sirius.patrius.assembly.properties.PropulsiveProperty;
import fr.cnes.sirius.patrius.assembly.properties.TankProperty;
import fr.cnes.sirius.patrius.forces.ForceModel;
import fr.cnes.sirius.patrius.forces.GradientModel;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.analysis.IDependentVectorVariable;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.parameter.JacobiansParameterizable;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.parameter.ParameterUtils;
import fr.cnes.sirius.patrius.math.parameter.StandardFieldDescriptors;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.propagation.MassProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.AbstractDetector;
import fr.cnes.sirius.patrius.propagation.events.DateDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.propagation.events.NullMassPartDetector;
import fr.cnes.sirius.patrius.propagation.numerical.TimeDerivativesEquations;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusStepHandler;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusStepInterpolator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 * This class implements a thrust (constant or variable).
 * <p>
 * The maneuver is defined by a direction in satellite frame or in a frame defined by user or in a LOF with type defined
 * by user. In first case, the current attitude of the spacecraft, defined by the current spacecraft state, will be used
 * to convert the thrust inDirection in satellite frame into inertial frame when inDirection is defined in satellite
 * frame. A typical case for tangential maneuvers is to use a {@link fr.cnes.sirius.patrius.attitudes.LofOffset LOF
 * aligned} attitude provider for state propagation and a velocity increment along the +X satellite axis.
 * </p>
 * <p>
 * The implementation of this class enables the computation of partial derivatives by finite differences with respect to
 * <b>thrust</b> and <b>flow rate</b> if they have been defined as constant (partial derivatives are not available if
 * parameters are not constant).
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
 * <p>
 * Note: including this force in a numerical propagator with adaptive step-size integrator may require to set up a small
 * lower bound for step-size (such as 1E-8s) since force discontinuity may cause difficulties to the integrator when the
 * maneuver stops.
 * </p>
 * 
 * <p>
 * Warning: if variable ISP and thrust are used (using {@link PropulsiveProperty}), ISP and thrust parameters cannot be
 * used (set as NaN) and partial derivatives cannot be computed.
 * </p>
 * 
 * @author Fabien Maussion
 * @author V&eacute;ronique Pommier-Maurussane
 * @author Luc Maisonobe
 */
@SuppressWarnings("PMD.NullAssignment")
public class ContinuousThrustManeuver extends JacobiansParameterizable implements ForceModel,
    GradientModel, Maneuver, PatriusStepHandler {

    /** Parameter name for thrust. */
    public static final String THRUST = "thrust";

    /** Parameter name for flow rate. */
    public static final String FLOW_RATE = "flow rate";

    /** Serializable UID. */
    private static final long serialVersionUID = 5349622732741384211L;

    /** Firing start detector. */
    private final EventDetector firingStartDetector;

    /** Firing stop detector. */
    private final EventDetector firingStopDetector;

    /** Direction of the acceleration in the frame defined by the user. */
    private final IDependentVectorVariable<SpacecraftState> direction;

    /**
     * Frame of the acceleration direction. If null and no LOF has been provided, the acceleration
     * is expressed in the satellite frame.
     */
    private final Frame frame;

    /**
     * Local orbital frame type. If null and no frame has been provided, the acceleration is
     * expressed in the satellite frame.
     */
    private final LOFType lofType;

    /** Mass model. */
    private final MassProvider massModel;

    /** Tank property. */
    private final TankProperty tankProp;

    /** Propulsive property. */
    private final PropulsiveProperty engineProp;

    /** State of the engine (internal variable). */
    private boolean firing;
    
    /** Maneuver instantaneous consumption deltaV. */
    private Vector3D usedDV;
    
    /** Total mass cached used to compute DV evolution. */
    private double currentTotalMass;

    // =================== Constructors with date and duration =================== //

    /**
     * 
     * Constructor for a constant direction in satellite frame.
     * 
     * @param date maneuver date
     * @param duration the duration of the thrust (s) (if negative, the date is considered to be the
     *        stop date)
     * @param engine engine property gathering thrust force (N) and isp (s) information
     * @param inDirection the constant thrust direction in satellite frame
     * @param massProvider the mass provider
     * @param tank tank property gathering mass and part name information
     */
    public ContinuousThrustManeuver(final AbsoluteDate date, final double duration,
        final PropulsiveProperty engine, final Vector3D inDirection,
        final MassProvider massProvider, final TankProperty tank) {
        this(date, duration, engine, new ConstantManeuverDirection(inDirection.normalize()),
            massProvider, tank, null, null);
    }

    /**
     * 
     * Constructor for a variable direction in satellite frame.
     * 
     * @param date maneuver date
     * @param duration the duration of the thrust (s) (if negative, the date is considered to be the
     *        stop date)
     * @param engine engine property gathering thrust force (N) and isp (s) information
     * @param inDirection the variable thrust direction in satellite frame
     * @param massProvider the mass provider
     * @param tank tank property gathering mass and part name information
     */
    public ContinuousThrustManeuver(final AbsoluteDate date, final double duration,
        final PropulsiveProperty engine,
        final IDependentVectorVariable<SpacecraftState> inDirection,
        final MassProvider massProvider, final TankProperty tank) {
        this(date, duration, engine, inDirection, massProvider, tank, null, null);
    }

    /**
     * 
     * Constructor for a constant direction in provided frame.
     * 
     * @param date maneuver date
     * @param duration the duration of the thrust (s) (if negative, the date is considered to be the
     *        stop date)
     * @param engine engine property gathering thrust force (N) and isp (s) information
     * @param inDirection the constant thrust direction in provided frame
     * @param massProvider the mass provider
     * @param tank tank property gathering mass and part name information
     * @param frameIn the frame of the acceleration direction.
     */
    public ContinuousThrustManeuver(final AbsoluteDate date, final double duration,
        final PropulsiveProperty engine, final Vector3D inDirection,
        final MassProvider massProvider, final TankProperty tank, final Frame frameIn) {
        this(date, duration, engine, new ConstantManeuverDirection(inDirection.normalize()),
            massProvider, tank, frameIn, null);
    }

    /**
     * 
     * Constructor for a variable direction in provided frame.
     * 
     * @param date maneuver date
     * @param duration the duration of the thrust (s) (if negative, the date is considered to be the
     *        stop date)
     * @param engine engine property gathering thrust force (N) and isp (s) information
     * @param inDirection the variable thrust direction in provided frame
     * @param massProvider the mass provider
     * @param tank tank property gathering mass and part name information
     * @param frameIn the frame of the acceleration direction.
     */
    public ContinuousThrustManeuver(final AbsoluteDate date, final double duration,
        final PropulsiveProperty engine,
        final IDependentVectorVariable<SpacecraftState> inDirection,
        final MassProvider massProvider, final TankProperty tank, final Frame frameIn) {
        this(date, duration, engine, inDirection, massProvider, tank, frameIn, null);
    }

    /**
     * 
     * Constructor for a constant direction in provided local orbital frame.
     * 
     * @param date maneuver date
     * @param duration the duration of the thrust (s) (if negative, the date is considered to be the
     *        stop date)
     * @param engine engine property gathering thrust force (N) and isp (s) information
     * @param inDirection the constant thrust direction in LOF
     * @param massProvider the mass provider
     * @param tank tank property gathering mass and part name information
     * @param lofTyp the LOF type of the acceleration inDirection
     */
    public ContinuousThrustManeuver(final AbsoluteDate date, final double duration,
        final PropulsiveProperty engine, final Vector3D inDirection,
        final MassProvider massProvider, final TankProperty tank, final LOFType lofTyp) {
        this(date, duration, engine, new ConstantManeuverDirection(inDirection.normalize()),
            massProvider, tank, null, lofTyp);
    }

    /**
     * 
     * Constructor for a variable direction in provided local orbital frame.
     * 
     * @param date maneuver date
     * @param duration the duration of the thrust (s) (if negative, the date is considered to be the
     *        stop date)
     * @param engine engine property gathering thrust force (N) and isp (s) information
     * @param inDirection the variable thrust direction in LOF
     * @param massProvider the mass provider
     * @param tank tank property gathering mass and part name information
     * @param lofTyp the LOF type of the acceleration inDirection
     */
    public ContinuousThrustManeuver(final AbsoluteDate date, final double duration,
        final PropulsiveProperty engine,
        final IDependentVectorVariable<SpacecraftState> inDirection,
        final MassProvider massProvider, final TankProperty tank, final LOFType lofTyp) {
        this(date, duration, engine, inDirection, massProvider, tank, null, lofTyp);
    }

    /**
     * Private constructor to handle all (date + duration cases).
     * 
     * @param date maneuver date
     * @param duration the duration of the thrust (s) (if negative, the date is considered to be the
     *        stop date)
     * @param engine engine property gathering thrust force (N) and isp (s) information
     * @param inDirection the thrust direction in provided frame
     * @param massProvider the mass provider
     * @param tank tank property gathering mass and part name information
     * @param frameIn the frame of the acceleration direction.
     * @param lofTyp the LOF type of the acceleration inDirection
     */
    private ContinuousThrustManeuver(final AbsoluteDate date, final double duration,
        final PropulsiveProperty engine,
        final IDependentVectorVariable<SpacecraftState> inDirection,
        final MassProvider massProvider, final TankProperty tank, final Frame frameIn,
        final LOFType lofTyp) {
        super();

        // Constant thrust maneuver case (engine properties are constant)
        if (!Double.isNaN(engine.getThrustParam().getValue())) {
            // Add flow rate as parameter
            final Parameter flowRate = new Parameter(FLOW_RATE, -engine.getThrustParam().getValue()
                / (Constants.G0_STANDARD_GRAVITY * engine.getIspParam().getValue()));
            this.addParameter(flowRate);

            // Add thrust as jacobian parameter
            this.addJacobiansParameter(engine.getThrustParam());
            ParameterUtils.addFieldToParameters(getParameters(), StandardFieldDescriptors.FORCE_MODEL, this.getClass());
        }

        // Attributes
        this.engineProp = engine;
        this.tankProp = tank;
        this.massModel = massProvider;
        this.direction = inDirection;
        this.frame = frameIn;
        this.lofType = lofTyp;
        if (duration >= 0) {
            this.firingStartDetector = new DateDetector(date);
            this.firingStopDetector = new DateDetector(date.shiftedBy(duration));
        } else {
            this.firingStopDetector = new DateDetector(date);
            this.firingStartDetector = new DateDetector(date.shiftedBy(duration));
        }

        // Internal variable
        this.firing = false;
        this.usedDV = Vector3D.ZERO;
    }

    // =================== Constructors with event detectors =================== //

    /**
     * Constructor for a constant direction in satellite frame.
     * 
     * @param startEventDetector event detector upon which maneuver should starts
     * @param stopEventDetector event detector upon which maneuver should stops
     * @param engine engine property gathering thrust force (N) and isp (s) information
     * @param inDirection the constant thrust direction in satellite frame
     * @param massProvider the mass provider
     * @param tank tank property gathering mass and part name information
     * @throws PatriusException if mass from mass provider is negative
     */
    public ContinuousThrustManeuver(final EventDetector startEventDetector,
        final EventDetector stopEventDetector, final PropulsiveProperty engine,
        final Vector3D inDirection, final MassProvider massProvider, final TankProperty tank)
        throws PatriusException {
        this(startEventDetector, stopEventDetector, engine, new ConstantManeuverDirection(
            inDirection.normalize()), massProvider, tank, null, null);
    }

    /**
     * Constructor for a variable direction in satellite frame.
     * 
     * @param startEventDetector event detector upon which maneuver should starts
     * @param stopEventDetector event detector upon which maneuver should stops
     * @param engine engine property gathering thrust force (N) and isp (s) information
     * @param inDirection the variable thrust direction in satellite frame
     * @param massProvider the mass provider
     * @param tank tank property gathering mass and part name information
     * @throws PatriusException if mass from mass provider is negative
     */
    public ContinuousThrustManeuver(final EventDetector startEventDetector,
        final EventDetector stopEventDetector, final PropulsiveProperty engine,
        final IDependentVectorVariable<SpacecraftState> inDirection,
        final MassProvider massProvider, final TankProperty tank) throws PatriusException {
        this(startEventDetector, stopEventDetector, engine, inDirection, massProvider, tank, null,
            null);
    }

    /**
     * Constructor for a constant direction in provided frame.
     * 
     * @param startEventDetector event detector upon which maneuver should starts
     * @param stopEventDetector event detector upon which maneuver should stops
     * @param engine engine property gathering thrust force (N) and isp (s) information
     * @param inDirection the constant thrust direction in provided frame
     * @param massProvider the mass provider
     * @param tank tank property gathering mass and part name information
     * @param frameIn the frame of the acceleration inDirection
     * @throws PatriusException if mass from mass provider is negative
     */
    public ContinuousThrustManeuver(final EventDetector startEventDetector,
        final EventDetector stopEventDetector, final PropulsiveProperty engine,
        final Vector3D inDirection, final MassProvider massProvider, final TankProperty tank,
        final Frame frameIn) throws PatriusException {
        this(startEventDetector, stopEventDetector, engine, new ConstantManeuverDirection(
            inDirection.normalize()), massProvider, tank, frameIn, null);
    }

    /**
     * Constructor for a variable direction in provided frame.
     * 
     * @param startEventDetector event detector upon which maneuver should starts
     * @param stopEventDetector event detector upon which maneuver should stops
     * @param engine engine property gathering thrust force (N) and isp (s) information
     * @param inDirection the variable thrust direction in provided frame
     * @param massProvider the mass provider
     * @param tank tank property gathering mass and part name information
     * @param frameIn the frame of the acceleration inDirection
     * @throws PatriusException if mass from mass provider is negative
     */
    public ContinuousThrustManeuver(final EventDetector startEventDetector,
        final EventDetector stopEventDetector, final PropulsiveProperty engine,
        final IDependentVectorVariable<SpacecraftState> inDirection,
        final MassProvider massProvider, final TankProperty tank, final Frame frameIn)
        throws PatriusException {
        this(startEventDetector, stopEventDetector, engine, inDirection, massProvider, tank,
            frameIn, null);
    }

    /**
     * Constructor for a constant direction in local orbital frame.
     * 
     * @param startEventDetector event detector upon which maneuver should starts
     * @param stopEventDetector event detector upon which maneuver should stops
     * @param engine engine property gathering thrust force (N) and isp (s) information
     * @param inDirection the constant thrust direction in LOF
     * @param massProvider the mass provider
     * @param tank tank property gathering mass and part name information
     * @param lofTyp the LOF type of the acceleration direction
     * @throws PatriusException if mass from mass provider is negative
     */
    public ContinuousThrustManeuver(final EventDetector startEventDetector,
        final EventDetector stopEventDetector, final PropulsiveProperty engine,
        final Vector3D inDirection, final MassProvider massProvider, final TankProperty tank,
        final LOFType lofTyp) throws PatriusException {
        this(startEventDetector, stopEventDetector, engine, new ConstantManeuverDirection(
            inDirection.normalize()), massProvider, tank, null, lofTyp);
    }

    /**
     * Constructor for a variable direction in local orbital frame.
     * 
     * @param startEventDetector event detector upon which maneuver should starts
     * @param stopEventDetector event detector upon which maneuver should stops
     * @param engine engine property gathering thrust force (N) and isp (s) information
     * @param inDirection the variable thrust direction in LOF
     * @param massProvider the mass provider
     * @param tank tank property gathering mass and part name information
     * @param lofTyp the LOF type of the acceleration direction
     * @throws PatriusException if mass from mass provider is negative
     */
    public ContinuousThrustManeuver(final EventDetector startEventDetector,
        final EventDetector stopEventDetector, final PropulsiveProperty engine,
        final IDependentVectorVariable<SpacecraftState> inDirection,
        final MassProvider massProvider, final TankProperty tank, final LOFType lofTyp)
        throws PatriusException {
        this(startEventDetector, stopEventDetector, engine, inDirection, massProvider, tank, null,
            lofTyp);
    }

    /**
     * Private constructor to handle all event detectors cases.
     * 
     * @param startEventDetector event detector upon which maneuver should starts
     * @param stopEventDetector event detector upon which maneuver should stops
     * @param engine engine property gathering thrust force (N) and isp (s) information
     * @param inDirection the thrust direction in the frame defined by the user
     * @param massProvider the mass provider
     * @param tank tank property gathering mass and part name information
     * @param frameIn the frame of the acceleration direction.
     * @param lofTyp the LOF type of the acceleration direction
     * @throws PatriusException if mass from mass provider is negative
     */
    private ContinuousThrustManeuver(final EventDetector startEventDetector,
        final EventDetector stopEventDetector, final PropulsiveProperty engine,
        final IDependentVectorVariable<SpacecraftState> inDirection,
        final MassProvider massProvider, final TankProperty tank, final Frame frameIn,
        final LOFType lofTyp) throws PatriusException {
        super();

        // Constant thrust maneuver case (engine properties are constant)
        if (!Double.isNaN(engine.getThrustParam().getValue())) {
            // Add flow rate as parameter
            final Parameter flowRate = new Parameter(FLOW_RATE, -engine.getThrustParam().getValue()
                / (Constants.G0_STANDARD_GRAVITY * engine.getIspParam().getValue()));
            this.addParameter(flowRate);

            // Add thrust as jacobian parameter
            this.addJacobiansParameter(engine.getThrustParam());
            ParameterUtils.addFieldToParameters(getParameters(), StandardFieldDescriptors.FORCE_MODEL, this.getClass());
        }

        // Attributes
        this.engineProp = engine;
        this.tankProp = tank;
        this.massModel = massProvider;
        this.direction = inDirection;
        this.frame = frameIn;
        this.lofType = lofTyp;
        this.firingStartDetector = startEventDetector;
        this.firingStopDetector = stopEventDetector;

        // Internal variable
        this.firing = false;
        this.usedDV = Vector3D.ZERO;
    }
    
    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState s0, final AbsoluteDate t) {
        this.currentTotalMass = this.massModel.getTotalMass();
    }
    
    /** {@inheritDoc} */
    @Override
    public void handleStep(final PatriusStepInterpolator interpolator,
            final boolean isLast) throws PropagationException {

        // Compute used DV if maneuver is firing
        if (this.firing) {

            final SpacecraftState state;
            try {
                state = interpolator.getInterpolatedState();
            } catch (final PatriusException e) {
                throw new PropagationException(e);
            }
            
            /*
             * Save totalMassStartStep & totalMassEndStep 
             * (respectively total mass at the integration step start and end)
             * Update totalMassStartStep cached value for next step
             */
            final double totalMassStartStep = this.currentTotalMass;
            final double totalMassEndStep = this.massModel.getTotalMass();
            
            // Tsiolkovsky equation to compute consumed dV on the integration step
            final double dv;
            if (totalMassEndStep != 0) {
                dv = this.getISP(state) * Constants.G0_STANDARD_GRAVITY
                        * MathLib.log(MathLib.divide(totalMassStartStep, totalMassEndStep));
            } else {
                // Should never happen on real satellites since dry mass is not zero
                dv = Double.POSITIVE_INFINITY;
            }
            final Vector3D dvVector = this.direction.value(state).scalarMultiply(dv);
            this.usedDV = this.usedDV.add(dvVector);

            // Update current mass
            this.currentTotalMass = totalMassEndStep;
        }
    }

    /**
     * Get the thrust.
     * 
     * @param s the current state information: date, kinematics, attitude.
     *        Unused in case of constant maneuver.
     * @return thrust force (N).
     */
    public double getThrust(final SpacecraftState s) {
        return this.engineProp.getThrust(s);
    }

    /**
     * Get the thrust direction.
     * 
     * @param s the current state information: date, kinematics, attitude.
     *        Unused in case of constant maneuver.
     * @return the thrust direction.
     */
    public Vector3D getDirection(final SpacecraftState s) {
        return this.direction.value(s);
    }

    /**
     * Get the specific impulse.
     * 
     * @param s the current state information: date, kinematics, attitude.
     *        Unused in case of constant maneuver.
     * @return specific impulse (s).
     */
    public double getISP(final SpacecraftState s) {
        return this.engineProp.getIsp(s);
    }

    /**
     * Get the flow rate.
     * 
     * @param s the current state information: date, kinematics, attitude
     * @return flow rate (negative, kg/s).
     */
    public double getFlowRate(final SpacecraftState s) {
        return -this.getThrust(s) / (Constants.G0_STANDARD_GRAVITY * this.getISP(s));
    }

    /**
     * Get the specific impulse.
     * <p>
     * Warning: if a variable ISP has been used, NaN will be returned.
     * </p>
     * 
     * @return specific impulse (s).
     */
    public double getISP() {
        return this.engineProp.getIspParam().getValue();
    }

    /**
     * Get the tank property.
     * 
     * @return tank property
     */
    public TankProperty getTankProperty() {
        return this.tankProp;
    }

    /**
     * Get the propulsive property.
     * 
     * @return propulsive property
     */
    public PropulsiveProperty getPropulsiveProperty() {
        return this.engineProp;
    }

    /**
     * Return the maneuver start date (if a date or a {@link DateDetector} as been provided).
     * 
     * @return the maneuver start date if a date or a {@link DateDetector} as been provided, null
     *         otherwise.
     */
    public AbsoluteDate getStartDate() {
        return this.firingStartDetector instanceof DateDetector ? ((DateDetector) this.firingStartDetector)
            .getDate() : null;
    }

    /**
     * Return the maneuver stop date (if a date or a {@link DateDetector} as been provided).
     * 
     * @return the maneuver stop date if a date or a {@link DateDetector} as been provided, null
     *         otherwise.
     */
    public AbsoluteDate getEndDate() {
        return this.firingStopDetector instanceof DateDetector ? ((DateDetector) this.firingStopDetector)
            .getDate() : null;
    }
    
    /**
     * Get the maneuver instantaneous consumption deltaV.
     * 
     * @return maneuver instantaneous consumption deltaV in maneuver frame (inertial, LOF or satellite)
     */
    public Vector3D getUsedDV() {
        return this.usedDV;
    }

    /**
     * Get the frame of the acceleration inDirection. Null if the thrust is expressed in the
     * satellite frame or in a local orbital frame.
     * 
     * @return the frame of the acceleration
     */
    public Frame getFrame() {
        return this.frame;
    }

    /**
     * Set maneuver status. This method is meant to be used if the propagation starts during a
     * maneuver. As a result thrust will be firing at the beginning of the propagation and stops
     * when crossing stop event.
     * <p>
     * Used in conjunction with {@link #isFiring()}, the user can stop/restart a propagation during a maneuver.
     * </p>
     * 
     * @param isFiring true if propagation should start during the maneuver
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
     * @return the lofType
     */
    public LOFType getLofType() {
        return this.lofType;
    }

    /**
     * @return the massModel
     */
    public MassProvider getMassModel() {
        return this.massModel;
    }

    /** {@inheritDoc} */
    @Override
    public void addContribution(final SpacecraftState s, final TimeDerivativesEquations adder)
                                                                                              throws PatriusException {

        if (this.firing) {
            // compute thrust acceleration in inertial frame
            adder.addAcceleration(this.computeAcceleration(s), s.getFrame());
        }
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D computeAcceleration(final SpacecraftState s) throws PatriusException {

        Vector3D res = Vector3D.ZERO;

        if (this.massModel.getMass(this.tankProp.getPartName()) == 0 && this.firing) {
            this.firing = false;
        }

        if (this.firing) {
            // Compute total mass
            double mass = 0.;
            final List<String> massPartNames = this.massModel.getAllPartsNames();
            for (int i = 0; i < massPartNames.size(); i++) {
                mass += s.getMass(massPartNames.get(i));
            }

            // Check on part mass
            // Mass may be currently negative as null mass event will be checked at the end of current
            // step
            if (s.getMass(this.tankProp.getPartName()) == 0) {
                res = Vector3D.ZERO;
            } else {
                if (this.frame == null) {
                    if (this.lofType == null) {
                        // Direction in satellite frame
                        // Convert it into inertial frame
                        res = new Vector3D(this.engineProp.getThrust(s) / mass, s.getAttitude()
                                .getRotation().applyTo(this.direction.value(s)).normalize());
                    } else {
                        // Verify that the spacecraftFrame is pseudo-inertial
                        if (!s.getFrame().isPseudoInertial()) {
                            // If frame is not pseudo-inertial, an exception is thrown
                            throw new PatriusException(PatriusMessages.NOT_INERTIAL_FRAME);
                        }
                        // Direction in a LOF where type is defined by user
                        final Transform transform = this.lofType.transformFromInertial(s.getDate(),
                                s.getPVCoordinates());
                        res = new Vector3D(this.engineProp.getThrust(s) / mass, transform.getInverse()
                                .transformVector(this.direction.value(s)).normalize());
                    }
                } else {
                    // Direction in a frame defined by the user
                    // Convert it into inertial frame (getFrame method of SpacecraftState
                    final Transform transform = this.frame.getTransformTo(s.getFrame(), s.getDate());
                    res = new Vector3D(this.engineProp.getThrust(s) / mass, transform.transformVector(
                            this.direction.value(s)).normalize());
                }
            }

            final double flowRate = this.getFlowRate(s);
            this.massModel.addMassDerivative(this.tankProp.getPartName(), flowRate);
        }
        return res;
    }

    /** {@inheritDoc} */
    @Override
    public void addDAccDState(final SpacecraftState s, final double[][] dAccdPos,
                              final double[][] dAccdVel) throws PatriusException {
        // does nothing
    }

    /**
     * {@inheritDoc}. This method can be called only for constant thrust maneuvers (ie. if {@link PropulsiveProperty}
     * has been defined with thrust and isp as constants/parameters.
     */
    @Override
    public void addDAccDParam(final SpacecraftState s, final Parameter param,
                              final double[] dAccdParam) throws PatriusException {

        if (this.firing) {
            // throw an exception if the parameter is not handled:
            if (!this.supportsJacobianParameter(param)) {
                throw new PatriusException(PatriusMessages.UNKNOWN_PARAMETER, param);
            }

            // Compute total mass
            double mass = 0.;
            final List<String> massPartNames = this.massModel.getAllPartsNames();
            for (int i = 0; i < massPartNames.size(); i++) {
                mass += s.getMass(massPartNames.get(i));
            }
            Vector3D res = null;

            // Check on part mass
            // Mass may be currently negative as null mass event will be checked at the end of
            // current step
            if (s.getMass(this.tankProp.getPartName()) == 0) {
                res = Vector3D.ZERO;
            } else {
                if (this.frame == null) {
                    if (this.lofType == null) {
                        // inDirection in satellite frame
                        // Convert it into inertial frame
                        res = new Vector3D(1 / mass, s.getAttitude().getRotation()
                            .applyTo(this.direction.value(s)));
                    } else {
                        // inDirection in a LOF where type is defined by user
                        final Transform transform = this.lofType.transformFromInertial(s.getDate(),
                            s.getPVCoordinates());
                        res = new Vector3D(1 / mass, transform.getInverse()
                            .transformVector(this.direction.value(s)).normalize());
                    }
                } else {
                    // inDirection in a frame defined by the user
                    // Convert it into inertial frame (getFrame method of SpacecraftState
                    final Transform transform = this.frame.getTransformTo(s.getFrame(), s.getDate());
                    res = new Vector3D(1 / mass, transform.transformVector(this.direction.value(s))
                        .normalize());
                }
            }
            dAccdParam[0] += res.getX();
            dAccdParam[1] += res.getY();
            dAccdParam[2] += res.getZ();
        }
    }

    /** {@inheritDoc} */
    @Override
    public EventDetector[] getEventsDetectors() {
        return new EventDetector[] { new FiringStartDetector(), new FiringStopDetector(),
            new NullMassPartDetector(this.massModel, this.tankProp.getPartName()) };
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
    public void checkData(final AbsoluteDate start, final AbsoluteDate end) throws PatriusException {
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
            super(ContinuousThrustManeuver.this.firingStartDetector.getSlopeSelection(),
                ContinuousThrustManeuver.this.firingStartDetector
                    .getMaxCheckInterval(), ContinuousThrustManeuver.this.firingStartDetector.getThreshold());
        }

        /** {@inheritDoc} */
        @Override
        @SuppressWarnings("PMD.ShortMethodName")
        public double g(final SpacecraftState s) throws PatriusException {
            return ContinuousThrustManeuver.this.firingStartDetector.g(s);
        }

        /** {@inheritDoc} */
        @Override
        public void init(final SpacecraftState s0, final AbsoluteDate t) throws PatriusException {
            ContinuousThrustManeuver.this.firingStartDetector.init(s0, t);
        }

        /** {@inheritDoc} */
        @Override
        public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                    final boolean forward) throws PatriusException {

            // Get underlying action
            final Action underlyingAction =
                ContinuousThrustManeuver.this.firingStartDetector.eventOccurred(s, increasing,
                    forward);
            
            this.isForward = forward;

            if (forward ^ ContinuousThrustManeuver.this.firing && underlyingAction == Action.STOP) {
                return Action.RESET_STATE;
            } else {
                // Forward propagation : already firing
                // Backward propagation: not firing
                // Don't do anything
                return Action.CONTINUE;
            }
        }

        /** {@inheritDoc} */
        @Override
        public boolean shouldBeRemoved() {
            return ContinuousThrustManeuver.this.firingStartDetector.shouldBeRemoved();
        }

        /** {@inheritDoc} */
        @Override
        public SpacecraftState resetState(final SpacecraftState oldState) throws PatriusException {
            // start the maneuver if the natural integration inDirection is forward
            // stop the maneuver if the natural integration inDirection is backward
            ContinuousThrustManeuver.this.firing = this.isForward;
            return oldState;
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
            super(ContinuousThrustManeuver.this.firingStopDetector.getSlopeSelection(),
                ContinuousThrustManeuver.this.firingStopDetector.getMaxCheckInterval(),
                ContinuousThrustManeuver.this.firingStopDetector.getThreshold());
        }

        /** {@inheritDoc} */
        @Override
        @SuppressWarnings("PMD.ShortMethodName")
        public double g(final SpacecraftState s) throws PatriusException {
            return ContinuousThrustManeuver.this.firingStopDetector.g(s);
        }

        /** {@inheritDoc} */
        @Override
        public void init(final SpacecraftState s0, final AbsoluteDate t) throws PatriusException {
            ContinuousThrustManeuver.this.firingStopDetector.init(s0, t);
        }

        /** {@inheritDoc} */
        @Override
        public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                    final boolean forward) throws PatriusException {

            // Get underlying action
            final Action underlyingAction = ContinuousThrustManeuver.this.firingStopDetector
                .eventOccurred(s, increasing, forward);

            this.isForward = forward;

            // Perform action only if still firing!
            if (forward ^ !ContinuousThrustManeuver.this.firing && underlyingAction == Action.STOP) {
                return Action.RESET_STATE;
            } else {
                // Forward propagation : not firing
                // Backward propagation: already firing
                // Don't do anything
                return Action.CONTINUE;
            }
        }

        /** {@inheritDoc} */
        @Override
        public boolean shouldBeRemoved() {
            return ContinuousThrustManeuver.this.firingStopDetector.shouldBeRemoved();
        }

        /** {@inheritDoc} */
        @Override
        public SpacecraftState resetState(final SpacecraftState oldState) throws PatriusException {
            // start the maneuver if the natural integration direction is forward
            // stop the maneuver if the natural integration direction is backward
            ContinuousThrustManeuver.this.firing = !this.isForward;
            // Update mass model
            final String partName = ContinuousThrustManeuver.this.tankProp.getPartName(); 
            ContinuousThrustManeuver.this.massModel.updateMass(partName, oldState.getMass(partName)); 
            return oldState;
        }

        /** {@inheritDoc} */
        @Override
        public EventDetector copy() {
            return new FiringStopDetector();
        }
    }
}
