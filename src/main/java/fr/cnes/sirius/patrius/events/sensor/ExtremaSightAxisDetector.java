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
 * @history creation 23/04/2012
 *
 * HISTORY
 * VERSION:4.9:DM:DM-3181:10/05/2022:[PATRIUS] Passage a protected de la methode setPropagationDelayType
 * VERSION:4.9:DM:DM-3143:10/05/2022:[PATRIUS] Nouvelle interface OrbitEventDetector et nouvelles classes
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2460:27/05/2020:Prise en compte des temps de propagation dans les calculs evenements
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:231:03/04/2014:bad updating of the assembly's tree of frames
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::DM:394:20/02/2015: Added possibility to define the action performed when event occurs
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:454:24/11/2015:Add constructors, overload method shouldBeRemoved() and adapt eventOccured()
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events.sensor;

import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.IPart;
import fr.cnes.sirius.patrius.assembly.PropertyType;
import fr.cnes.sirius.patrius.assembly.properties.SensorProperty;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.AbstractDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.UtilsPatrius;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Detects the minimum angle between a sight view line and a PVCoordinateProvider target view from a
 * Frame which origin is on the line.
 * <p>
 * The detector is similar to the extrema three body angle detector : body one is the target point, body two is the
 * frame origin on the line, and body three is the direction vector of the line. Each one are expressed in the frame
 * with origin on the line.
 * <p>
 * The detector doesn't take into account potential masking.
 * <p>
 * The default implementation behavior is to {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#STOP
 * stop} propagation when the minimum angle is detected.
 * </p>
 * <p>
 * This detector can takes into account signal propagation duration through {@link #getPropagationDelayType()} (default
 * is signal being instantaneous).
 * </p>
 * 
 * @concurrency not thread-safe
 * 
 * @concurrency.comment attributes are mutable and related to propagation, and the direct use of a
 *                      not thread-safe Assembly makes this class not thread-safe itself
 * 
 * @see EventDetector
 * @see fr.cnes.sirius.patrius.propagation.events.ExtremaThreeBodiesAngleDetector
 * 
 * @author chabaudp
 * 
 * @version $Id$
 * 
 * @since 1.3
 * 
 */
@SuppressWarnings("PMD.NullAssignment")
public class ExtremaSightAxisDetector extends AbstractDetector {

    /** Flag for local minimum angle detection (g increasing). */
    public static final int MIN = 0;

    /** Flag for local maximum angle detection (g decreasing). */
    public static final int MAX = 1;

    /** Flag for both local minimum and maximum angle detection. */
    public static final int MIN_MAX = 2;

    /** serial ID */
    private static final long serialVersionUID = -5397188919344806100L;

    /** The target point : could be on central body or a vehicle */
    private final PVCoordinatesProvider targetPoint;

    /** The line defining the sight view expressed in attitude local frame */
    private Vector3D sightAxis;

    /** Used if the user define an assembly to define spacecraft */
    private Assembly vehicle;

    /**
     * Used if the user define an assembly containing a part with sensor property to modelize
     * spacecraft
     */
    private String sensorName;

    /**
     * Constructor to use without assembly for both minimal and maximal angle detection. The sight
     * view line will have origin on the attitude frame origin and direction of the sight axis
     * vector3D.
     * 
     * @param target the target could be on central body surface or another vehicle
     * @param sightAxisDirection the direction of the sight view line which origin is the position
     *        of the vehicle
     * @param maxCheck maximum checking interval (s)(see {@link AbstractDetector})
     * @param threshold convergence threshold (s)(see {@link AbstractDetector})
     * @param actionMin action to be performed when the expected local minimum is reached
     * @param actionMax action to be performed when the expected local maximum is reached
     * @throws IllegalArgumentException if sight axis direction is null
     */
    public ExtremaSightAxisDetector(final PVCoordinatesProvider target,
        final Vector3D sightAxisDirection, final double maxCheck, final double threshold,
        final Action actionMin, final Action actionMax) {
        this(target, sightAxisDirection, maxCheck, threshold, actionMin, actionMax, false, false);
    }

    /**
     * Constructor to use without assembly for both minimal and maximal angle detection. The sight
     * view line will have origin on the attitude frame origin and direction of the sight axis
     * vector3D.
     * 
     * @param target the target could be on central body surface or another vehicle
     * @param sightAxisDirection the direction of the sight view line which origin is the position
     *        of the vehicle
     * @param maxCheck maximum checking interval (s)(see {@link AbstractDetector})
     * @param threshold convergence threshold (s)(see {@link AbstractDetector})
     * @param actionMin action to be performed when the expected local minimum is reached
     * @param actionMax action to be performed when the expected local maximum is reached
     * @param removeMin true if detector should be removed when the expected local minimum is
     *        reached
     * @param removeMax true if detector should be removed when the expected local maximum is
     *        reached
     * @throws IllegalArgumentException if sight axis direction is null
     */
    public ExtremaSightAxisDetector(final PVCoordinatesProvider target,
        final Vector3D sightAxisDirection, final double maxCheck, final double threshold,
        final Action actionMin, final Action actionMax, final boolean removeMin,
        final boolean removeMax) {
        super(maxCheck, threshold, actionMin, actionMax, removeMin, removeMax);

        // direction norm test
        if (sightAxisDirection.getNorm() < UtilsPatrius.GEOMETRY_EPSILON) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.PDB_ZERO_NORM);
        }
        this.targetPoint = target;
        this.sightAxis = sightAxisDirection;
    }

    /**
     * Constructor to use without assembly. The sight view line will have origin on the attitude
     * frame origin and direction of the sight axis vector3D.
     * 
     * @param extremumType {@link ExtremaSightAxisDetector#MIN} for minimal angle detection,
     *        {@link ExtremaSightAxisDetector#MAX} for maximal angle detection or
     *        {@link ExtremaSightAxisDetector#MIN_MAX} for both minimal and maximal angle detection
     * @param target the target could be on central body surface or another vehicle
     * @param sightAxisDirection the direction of the sight view line which origin is the position
     *        of the vehicle
     * @param maxCheck maximum checking interval (s)(see {@link AbstractDetector})
     * @param threshold convergence threshold (s)(see {@link AbstractDetector})
     * @param action action to be performed when the expected extrema is reached.
     * @throws IllegalArgumentException thrown if sight axis direction is null
     */
    public ExtremaSightAxisDetector(final int extremumType, final PVCoordinatesProvider target,
        final Vector3D sightAxisDirection, final double maxCheck, final double threshold,
        final Action action) {
        this(extremumType, target, sightAxisDirection, maxCheck, threshold, action, false);
    }

    /**
     * Constructor to use without assembly. The sight view line will have origin on the attitude
     * frame origin and direction of the sight axis vector3D.
     * 
     * @param extremumType {@link ExtremaSightAxisDetector#MIN} for minimal angle detection,
     *        {@link ExtremaSightAxisDetector#MAX} for maximal angle detection or
     *        {@link ExtremaSightAxisDetector#MIN_MAX} for both minimal and maximal angle detection
     * @param target the target could be on central body surface or another vehicle
     * @param sightAxisDirection the direction of the sight view line which origin is the position
     *        of the vehicle
     * @param maxCheck maximum checking interval (s)(see {@link AbstractDetector})
     * @param threshold convergence threshold (s)(see {@link AbstractDetector})
     * @param action action to be performed when the expected extrema is reached.
     * @param remove true if detector should be removed when the expected extrema is reached NB : If
     *        remove is true, it means detector should be removed at detection, so the value of
     *        attributes removeMIN and removeMAX must be decided according extremumType.
     * 
     * @since 3.1
     * @throws IllegalArgumentException if sight axis direction is null
     */
    public ExtremaSightAxisDetector(final int extremumType, final PVCoordinatesProvider target,
        final Vector3D sightAxisDirection, final double maxCheck, final double threshold,
        final Action action, final boolean remove) {
        super(extremumType, maxCheck, threshold);

        // direction norm test
        if (sightAxisDirection.getNorm() < UtilsPatrius.GEOMETRY_EPSILON) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.PDB_ZERO_NORM);
        }
        this.targetPoint = target;
        this.sightAxis = sightAxisDirection;
        this.shouldBeRemovedFlag = remove;

        // If slopeSelection is different from 0, 1 or 2, an error has already been raised is
        // superclass.
        if (extremumType == MIN) {
            this.removeAtEntry = remove;
            this.removeAtExit = false;
            this.actionAtEntry = action;
            this.actionAtExit = null;
        } else if (extremumType == MAX) {
            this.removeAtEntry = false;
            this.removeAtExit = remove;
            this.actionAtEntry = null;
            this.actionAtExit = action;
        } else {
            this.removeAtEntry = remove;
            this.removeAtExit = remove;
            this.actionAtEntry = action;
            this.actionAtExit = action;
        }
    }

    /**
     * Constructor to use without assembly. The sight view line will have origin on the attitude
     * frame origin and direction of the sight axis vector3D.
     * <p>
     * The default behavior is to {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#STOP stop}
     * propagation when the minimum angle is detected
     * </p>
     * 
     * @param extremumType {@link ExtremaSightAxisDetector#MIN} for minimal angle detection,
     *        {@link ExtremaSightAxisDetector#MAX} for maximal angle detection or
     *        {@link ExtremaSightAxisDetector#MIN_MAX} for both minimal and maximal angle detection
     * @param target the target could be on central body surface or another vehicle
     * @param sightAxisDirection the direction of the sight view line which origin is the position
     *        of the vehicle
     * @param maxCheck maximum checking interval (s)(see {@link AbstractDetector})
     * @param threshold convergence threshold (s)(see {@link AbstractDetector})
     * @throws IllegalArgumentException thrown if sight axis direction is null
     */
    public ExtremaSightAxisDetector(final int extremumType, final PVCoordinatesProvider target,
        final Vector3D sightAxisDirection, final double maxCheck, final double threshold) {
        this(extremumType, target, sightAxisDirection, maxCheck, threshold, Action.STOP);
    }

    /**
     * Constructor to use without assembly. It uses default max check and default threshold.
     * <p>
     * The default behavior is to {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#STOP stop}
     * propagation when the minimum angle is detected.
     * </p>
     * 
     * @param extremumType {@link ExtremaSightAxisDetector#MIN} for minimal angle detection,
     *        {@link ExtremaSightAxisDetector#MAX} for maximal angle detection or
     *        {@link ExtremaSightAxisDetector#MIN_MAX} for both minimal and maximal angle detection
     * @param target the target could be on central body surface or another vehicle
     * @param sightAxisDirection the direction of the sight view line which origin is the position
     *        of the vehicle
     */
    public ExtremaSightAxisDetector(final int extremumType, final PVCoordinatesProvider target,
        final Vector3D sightAxisDirection) {
        this(extremumType, target, sightAxisDirection, DEFAULT_MAXCHECK, DEFAULT_THRESHOLD);
    }

    /**
     * Constructor based on an Assembly containing at least one part with sensor property describing
     * the vehicle.
     * <p>
     * The default behavior is to {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#STOP stop}
     * propagation when the minimum angle is detected
     * </p>
     * 
     * @param extremumType {@link ExtremaSightAxisDetector#MIN} for minimal angle detection,
     *        {@link ExtremaSightAxisDetector#MAX} for maximal angle detection or
     *        {@link ExtremaSightAxisDetector#MIN_MAX} for both minimal and maximal angle detection
     * @param target the target could be on central body surface or another vehicle
     * @param assembly the assembly to consider (its main part frame must have a parent frame !!).
     *        Must have at least one part with sensor property.
     * @param partName the name of the part that supports the sensor property.
     * @param maxCheck maximum checking interval (s)(see {@link AbstractDetector})
     * @param threshold convergence threshold (s)(see {@link AbstractDetector})
     * @throws IllegalArgumentException thrown if the part identified by partName doesn't have the
     *         SENSOR property.
     */
    public ExtremaSightAxisDetector(final int extremumType, final PVCoordinatesProvider target,
        final Assembly assembly, final String partName, final double maxCheck,
        final double threshold) {
        this(extremumType, target, assembly, partName, maxCheck, threshold, Action.STOP);
    }

    /**
     * Constructor based on an Assembly containing at least one part with sensor property describing
     * the vehicle. It uses default max check and default threshold.
     * <p>
     * The default behavior is to {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#STOP stop}
     * propagation when the minimum angle is detected
     * </p>
     * 
     * @param extremumType {@link ExtremaSightAxisDetector#MIN} for minimal angle detection,
     *        {@link ExtremaSightAxisDetector#MAX} for maximal angle detection or
     *        {@link ExtremaSightAxisDetector#MIN_MAX} for both minimal and maximal angle detection
     * @param target the target could be on central body surface or another vehicle
     * @param assembly the assembly to consider (its main part frame must have a parent frame !!).
     *        Must have at least one part with sensor property.
     * @param partName the name of the part that supports the sensor property.
     */
    public ExtremaSightAxisDetector(final int extremumType, final PVCoordinatesProvider target,
        final Assembly assembly, final String partName) {
        this(extremumType, target, assembly, partName, DEFAULT_MAXCHECK, DEFAULT_THRESHOLD);
    }

    /**
     * Constructor for both minimal and maximal angle based on an Assembly containing at least one
     * part with sensor property describing the vehicle.
     * 
     * @param extremumType {@link ExtremaSightAxisDetector#MIN} for minimal angle detection,
     *        {@link ExtremaSightAxisDetector#MAX} for maximal angle detection or
     *        {@link ExtremaSightAxisDetector#MIN_MAX} for both minimal and maximal angle detection
     * @param target the target could be on central body surface or another vehicle
     * @param assembly the assembly to consider (its main part frame must have a parent frame !)
     *        Must have at least one part with sensor property.
     * @param partName the name of the part that supports the sensor property.
     * @param maxCheck maximum checking interval (s)(see {@link AbstractDetector})
     * @param threshold convergence threshold (s)(see {@link AbstractDetector})
     * @param action action to be performed when the expected extrema is reached
     * @throws IllegalArgumentException if the part identified by partName doesn't have the SENSOR
     *         property
     */
    public ExtremaSightAxisDetector(final int extremumType, final PVCoordinatesProvider target,
        final Assembly assembly, final String partName, final double maxCheck,
        final double threshold, final Action action) {
        this(extremumType, target, assembly, partName, maxCheck, threshold, action, false);
    }

    /**
     * Constructor for both minimal and maximal angle based on an Assembly containing at least one
     * part with sensor property describing the vehicle.
     * 
     * @param extremumType {@link ExtremaSightAxisDetector#MIN} for minimal angle detection,
     *        {@link ExtremaSightAxisDetector#MAX} for maximal angle detection or
     *        {@link ExtremaSightAxisDetector#MIN_MAX} for both minimal and maximal angle detection
     * @param target the target could be on central body surface or another vehicle
     * @param assembly the assembly to consider (its main part frame must have a parent frame !!).
     *        Must have at least one part with sensor property.
     * @param partName the name of the part that supports the sensor property.
     * @param maxCheck maximum checking interval (s)(see {@link AbstractDetector})
     * @param threshold convergence threshold (s)(see {@link AbstractDetector})
     * @param action action to be performed when the expected extrema is reached.
     * @param remove true if detector should be removed when the expected extrema is reached
     * 
     *        NB : If remove is true, it means detector should be removed at detection, so the value
     *        of attributes removeMIN and removeMAX must be decided according extremumType.
     * 
     * @throws IllegalArgumentException if the part identified by partName doesn't have the SENSOR
     *         property.
     */
    public ExtremaSightAxisDetector(final int extremumType, final PVCoordinatesProvider target,
        final Assembly assembly, final String partName, final double maxCheck,
        final double threshold, final Action action, final boolean remove) {
        super(extremumType, maxCheck, threshold);

        final boolean hasProperty = assembly.getPart(partName).hasProperty(PropertyType.SENSOR);

        if (!hasProperty) {
            // no SENSOR property is associated to this part
            throw PatriusException
                .createIllegalArgumentException(PatriusMessages.PDB_NO_SENSOR_PROPERTY);
        }

        this.targetPoint = target;
        this.vehicle = assembly;
        this.sensorName = partName;
        this.shouldBeRemovedFlag = remove;

        // If slopeSelection is different from 0, 1 or 2, an error has already been raised is
        // superclass.
        if (extremumType == MIN) {
            this.actionAtEntry = action;
            this.actionAtExit = null;
            this.removeAtEntry = remove;
            this.removeAtExit = false;
        } else if (extremumType == MAX) {
            this.actionAtEntry = null;
            this.actionAtExit = action;
            this.removeAtEntry = false;
            this.removeAtExit = remove;
        } else {
            this.actionAtEntry = action;
            this.actionAtExit = action;
            this.removeAtEntry = remove;
            this.removeAtExit = remove;
        }
    }

    /**
     * Constructor for both minimal and maximal angle based on an Assembly containing at least one
     * part with sensor property describing the vehicle.
     * 
     * @param target the target can be on central body surface or another vehicle
     * @param assembly the assembly to consider (its main part frame must have a parent frame !!).
     *        Must have at least one part with sensor property.
     * @param partName the name of the part that supports the sensor property.
     * @param maxCheck maximum checking interval (s)(see {@link AbstractDetector})
     * @param threshold convergence threshold (s)(see {@link AbstractDetector})
     * @param actionMin action to be performed when the expected local minimum is reached
     * @param actionMax action to be performed when the expected local maximum is reached
     * @throws IllegalArgumentException if the part identified by partName doesn't have the sensor
     *         property.
     */
    public ExtremaSightAxisDetector(final PVCoordinatesProvider target, final Assembly assembly,
        final String partName, final double maxCheck, final double threshold,
        final Action actionMin, final Action actionMax) {
        this(target, assembly, partName, maxCheck, threshold, actionMin, actionMax, false, false);
    }

    /**
     * Constructor for both minimal and maximal angle based on an Assembly containing at least one
     * part with sensor property describing the vehicle.
     * 
     * @param target the target can be on central body surface or another vehicle
     * @param assembly the assembly to consider (its main part frame must have a parent frame !!).
     *        Must have at least one part with sensor property.
     * @param partName the name of the part that supports the sensor property.
     * @param maxCheck maximum checking interval (s)(see {@link AbstractDetector})
     * @param threshold convergence threshold (s)(see {@link AbstractDetector})
     * @param actionMin action to be performed when the expected local minimum is reached
     * @param actionMax action to be performed when the expected local maximum is reached
     * @param removeMin true if detector should be removed when the expected local minimum is
     *        reached
     * @param removeMax true if detector should be removed when the expected local maximum is
     *        reached
     * @throws IllegalArgumentException if the part identified by partName doesn't have the SENSOR
     *         property.
     */
    public ExtremaSightAxisDetector(final PVCoordinatesProvider target, final Assembly assembly,
        final String partName, final double maxCheck, final double threshold,
        final Action actionMin, final Action actionMax, final boolean removeMin,
        final boolean removeMax) {
        super(maxCheck, threshold, actionMin, actionMax, removeMin, removeMax);

        final boolean hasProperty = assembly.getPart(partName).hasProperty(PropertyType.SENSOR);

        if (!hasProperty) {
            // no SENSOR property is associated to this part
            throw PatriusException
                .createIllegalArgumentException(PatriusMessages.PDB_NO_SENSOR_PROPERTY);
        }

        this.targetPoint = target;
        this.vehicle = assembly;
        this.sensorName = partName;
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState s0, final AbsoluteDate t) {
        // Nothing to do
    }

    /**
     * The switching function is specific case of the extrema three bodies angle detector. It is
     * minus the derivative of the inner expression : (P1 dotProduct P3) / Norm(P1) * Norm(P3) where
     * P1 is the position of the target and P3 is the sight view axis all expressed in frame with
     * origin on the sight line view. P2 is this origin and is fix in this frame so we remove it
     * from expression
     * 
     * @see fr.cnes.sirius.patrius.propagation.events.ExtremaThreeBodiesAngleDetector
     * @param s the spacecraft state used to determine the local frame
     * @return value of the switching function
     * @throws PatriusException if some specific error occurs
     */
    @Override
    @SuppressWarnings("PMD.ShortMethodName")
    public double g(final SpacecraftState s) throws PatriusException {

        // the two vector defining the angle which the switching function find the local extrema
        final PVCoordinates p1;
        final PVCoordinates p3;

        if (this.vehicle == null) {

            // Transformation from spacecraftState orbit expression frame to orbital local attitude
            // frame
            // notice that the transform date is only informative and useless

            // Getting the translation and rotation:
            final Transform rotation = new Transform(s.getDate(), s.getAttitude().getOrientation());
            final Transform translation = new Transform(s.getDate(), s.getOrbit()
                .getPVCoordinates());
            final Transform transform = new Transform(s.getDate(), translation, rotation);

            // p1 : target PVCoordinates in orbital local attitude frame
            final AbsoluteDate targetDate = getSignalEmissionDate(targetPoint, s.getOrbit(), s.getDate(),
                getThreshold());
            p1 = transform.transformPVCoordinates(this.targetPoint.getPVCoordinates(targetDate,
                s.getFrame()));

            // Notice that the p2 of extrema three bodies angle is null.

            // p3 : SightView Axis in spacecraft frame position is Axis, velocity is null
            p3 = new PVCoordinates(this.sightAxis, Vector3D.ZERO);
        } else {
            this.vehicle.updateMainPartFrame(s);
            // get the sensor : usually the payload
            final IPart payload = this.vehicle.getPart(this.sensorName);
            // get the payload frame, sensor properties and sightAxis in this frame
            final Frame payloadFrame = payload.getFrame();
            final SensorProperty payloadSensorProperty = (SensorProperty) (payload
                .getProperty(PropertyType.SENSOR));
            final Vector3D sightViewAxis = payloadSensorProperty.getInSightAxis();

            // p1 : target PVCoordinates in payload frame
            final AbsoluteDate targetDate = getSignalEmissionDate(targetPoint, s.getOrbit(), s.getDate(),
                getThreshold());
            p1 = this.targetPoint.getPVCoordinates(targetDate, payloadFrame);

            // p3 : sight axis fixed in this frame
            p3 = new PVCoordinates(sightViewAxis, Vector3D.ZERO);
        }

        // constants for the derivative computation
        final double norm1 = p1.getPosition().getNorm();
        final double norm3 = p3.getPosition().getNorm();
        final double pos1Pos3 = Vector3D.dotProduct(p1.getPosition(), p3.getPosition());
        final double pos1Pos1 = p1.getPosition().getNormSq();
        final double pos3Pos3 = p3.getPosition().getNormSq();
        final double pos1Vel1 = Vector3D.dotProduct(p1.getPosition(), p1.getVelocity());
        final double pos3Vel3 = Vector3D.dotProduct(p3.getPosition(), p3.getVelocity());
        final double pos1Vel3 = Vector3D.dotProduct(p1.getPosition(), p3.getVelocity());
        final double pos3Vel1 = Vector3D.dotProduct(p3.getPosition(), p1.getVelocity());

        // this double has the same sign as the angle derivative :
        // (this part of the derivative expression, the ignored factor part
        // having a constant negative sign)
        return -(MathLib.divide(pos1Vel3 + pos3Vel1, norm1 * norm3) - MathLib
            .divide(pos1Pos3, pos1Pos1 * pos3Pos3)
            * (MathLib.divide(pos1Vel1 * norm3, norm1) + MathLib.divide(pos3Vel3 * norm1,
                norm3)));
    }

    /** {@inheritDoc} */
    @Override
    public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                final boolean forward) throws PatriusException {
        final Action result;
        if (this.getSlopeSelection() == 0) {
            result = this.getActionAtEntry();
            // remove (or not) detector
            this.shouldBeRemovedFlag = this.isRemoveAtEntry();
        } else if (this.getSlopeSelection() == 1) {
            result = this.getActionAtExit();
            // remove (or not) detector
            this.shouldBeRemovedFlag = this.isRemoveAtExit();
        } else {
            if (forward ^ !increasing) {
                // minimum case
                result = this.getActionAtEntry();
                // remove (or not) detector
                this.shouldBeRemovedFlag = this.isRemoveAtEntry();
            } else {
                // maximum case
                result = this.getActionAtExit();
                // remove (or not) detector
                this.shouldBeRemovedFlag = this.isRemoveAtExit();
            }
        }
        return result;
    }

    /**
     * Get the target point.
     * 
     * @return the target point
     */
    public PVCoordinatesProvider getTargetPoint() {
        return this.targetPoint;
    }

    /**
     * Get the sight axis
     * 
     * @return the sight axis
     */
    public Vector3D getSightAxis() {
        return this.sightAxis;
    }

    /**
     * Get the vehicle.
     * 
     * @return the vehicle
     */
    public Assembly getVehicle() {
        return this.vehicle;
    }

    /**
     * Get the sensor name.
     * 
     * @return the sensor name
     */
    public String getSensorName() {
        return this.sensorName;
    }
    
    /** @inheritDoc */
    @Override
    public void setPropagationDelayType(final PropagationDelayType propagationDelayType, final Frame frame){
        super.setPropagationDelayType(propagationDelayType, frame);
    }
    
    /**
     * {@inheritDoc}
     * <p>
     * The following attributes are not deeply copied:
     * <ul>
     * <li>targetPoint: {@link PVCoordinatesProvider}</li>
     * <li>vehicle (if defined): {@link Assembly}</li>
     * </ul>
     * </p>
     */
    @Override
    public EventDetector copy() {
        ExtremaSightAxisDetector result = null;
        if (this.vehicle == null) {
            result = new ExtremaSightAxisDetector(this.targetPoint, new Vector3D(this.sightAxis.getX(),
                this.sightAxis.getY(), this.sightAxis.getZ()), this.getMaxCheckInterval(), this.getThreshold(),
                this.getActionAtEntry(), this.getActionAtExit(), this.isRemoveAtEntry(), this.isRemoveAtExit());
        } else {
            result = new ExtremaSightAxisDetector(this.targetPoint, this.vehicle, this.sensorName,
                this.getMaxCheckInterval(), this.getThreshold(), this.getActionAtEntry(), this.getActionAtExit(),
                this.isRemoveAtEntry(), this.isRemoveAtExit());
        }
        result.setPropagationDelayType(getPropagationDelayType(), getInertialFrame());
        return result;
    }
}
