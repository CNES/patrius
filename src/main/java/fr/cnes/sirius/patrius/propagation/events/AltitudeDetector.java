/**
 * Copyright 2002-2012 CS Systèmes d'Information
 * Copyright 2011-2017 CNES
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
 */
/*
 *
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 *                             (added forward parameter to eventOccurred signature)
 * VERSION::DM:394:20/02/2015: Added possibility to define the action performed when event occurs
 * VERSION::DM:454:24/11/2015:Add constructors, overload method shouldBeRemoved() and adapt eventOccured()
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.events;

import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Finder for satellite altitude crossing events.
 * <p>
 * This class finds altitude events (i.e. satellite crossing a predefined altitude level above ground).
 * </p>
 * <p>
 * The default implementation behavior is to {@link EventDetector.Action#CONTINUE continue} propagation when ascending
 * and to {@link EventDetector.Action#STOP stop} propagation when descending. This can be changed by using provided
 * constructors.
 * </p>
 * 
 * @see fr.cnes.sirius.patrius.propagation.Propagator#addEventDetector(EventDetector)
 * @author Luc Maisonobe
 */
@SuppressWarnings("PMD.NullAssignment")
public class AltitudeDetector extends AbstractDetector {

    /** Flag for ascending altitude detection (slopeSelection = 0). */
    public static final int ASCENDING = 0;

    /** Flag for descending altitude detection (slopeSelection = 1). */
    public static final int DESCENDING = 1;

    /** Flag for both ascending and descending altitude detection (slopeSelection = 2). */
    public static final int ASCENDING_DESCENDING = 2;

    /** Serializable UID. */
    private static final long serialVersionUID = -1552109617025755015L;

    /** Threshold altitude value (m). */
    private final double altitude;

    /** Body shape with respect to which altitude should be evaluated. */
    private final BodyShape bodyShape;

    /** Action performed at altitude detection when ascending. */
    private final Action actionAtAscending;

    /** Action performed at altitude detection when descending. */
    private final Action actionAtDescending;

    /** True if detector should be removed at ascending altitude detection. */
    private final boolean removeAtAscendingFlag;

    /** True if detector should be removed at ascending altitude detection. */
    private final boolean removeAtDescendingFlag;

    /** True if detector should be removed (updated by eventOccured). */
    private boolean shouldBeRemovedFlag = false;

    /**
     * Build a new altitude detector.
     * <p>
     * This simple constructor takes default values for maximal checking interval ( {@link #DEFAULT_MAXCHECK}) and
     * convergence threshold ({@link #DEFAULT_THRESHOLD}).
     * </p>
     * <p>
     * The default implementation behavior is to {@link EventDetector.Action#CONTINUE continue} propagation when
     * ascending and to {@link EventDetector.Action#STOP stop} propagation when descending.
     * </p>
     * 
     * @param altitudeIn threshold altitude value
     * @param bodyShapeIn body shape with respect to which altitude should be evaluated
     */
    public AltitudeDetector(final double altitudeIn, final BodyShape bodyShapeIn) {
        this(altitudeIn, bodyShapeIn, DEFAULT_MAXCHECK);
    }

    /**
     * Build a new altitude detector.
     * <p>
     * This simple constructor takes default value for convergence threshold ( {@link #DEFAULT_THRESHOLD}).
     * </p>
     * <p>
     * The maximal interval between altitude checks should be smaller than the half duration of the minimal pass to
     * handle, otherwise some short passes could be missed.
     * </p>
     * <p>
     * The default implementation behavior is to {@link EventDetector.Action#CONTINUE continue} propagation when
     * ascending and to {@link EventDetector.Action#STOP stop} propagation when descending.
     * </p>
     * 
     * @param altitudeIn threshold altitude value (m)
     * @param bodyShapeIn body shape with respect to which altitude should be evaluated
     * @param maxCheck maximal checking interval (s)
     */
    public AltitudeDetector(final double altitudeIn, final BodyShape bodyShapeIn,
        final double maxCheck) {
        this(altitudeIn, bodyShapeIn, maxCheck, DEFAULT_THRESHOLD, Action.CONTINUE, Action.STOP);
    }

    /**
     * Build a new altitude detector.
     * <p>
     * The maximal interval between altitude checks should be smaller than the half duration of the minimal pass to
     * handle, otherwise some short passes could be missed.
     * </p>
     * <p>
     * The default implementation behavior is to {@link EventDetector.Action#CONTINUE continue} propagation when
     * ascending and to {@link EventDetector.Action#STOP stop} propagation when descending.
     * </p>
     * 
     * @param altitudeIn threshold altitude value (m)
     * @param bodyShapeIn body shape with respect to which altitude should be evaluated
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     */
    public AltitudeDetector(final double altitudeIn, final BodyShape bodyShapeIn,
        final double maxCheck, final double threshold) {
        this(altitudeIn, bodyShapeIn, maxCheck, threshold, Action.CONTINUE, Action.STOP);
    }

    /**
     * Build a new altitude detector.
     * <p>
     * The maximal interval between altitude checks should be smaller than the half duration of the minimal pass to
     * handle, otherwise some short passes could be missed.
     * </p>
     * 
     * @param altitudeIn threshold altitude value (m)
     * @param bodyShapeIn body shape with respect to which altitude should be evaluated
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param ascending action performed when ascending
     * @param descending action performed when descending
     */
    public AltitudeDetector(final double altitudeIn, final BodyShape bodyShapeIn,
        final double maxCheck, final double threshold, final Action ascending,
        final Action descending) {
        this(altitudeIn, bodyShapeIn, maxCheck, threshold, ascending, descending, false, false);
    }

    /**
     * Build a new altitude detector.
     * <p>
     * The maximal interval between altitude checks should be smaller than the half duration of the minimal pass to
     * handle, otherwise some short passes could be missed.
     * </p>
     * 
     * @param altitudeIn threshold altitude value (m)
     * @param bodyShapeIn body shape with respect to which altitude should be evaluated
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param ascending action performed when ascending
     * @param descending action performed when descending
     * @param removeAscending true if detector should be removed at ascending altitude detection
     * @param removeDescending true if detector should be removed at descending altitude detection
     * @since 3.1
     */
    public AltitudeDetector(final double altitudeIn, final BodyShape bodyShapeIn,
        final double maxCheck, final double threshold, final Action ascending,
        final Action descending, final boolean removeAscending, final boolean removeDescending) {
        super(maxCheck, threshold);
        this.altitude = altitudeIn;
        this.bodyShape = bodyShapeIn;
        this.actionAtAscending = ascending;
        this.actionAtDescending = descending;
        this.removeAtAscendingFlag = removeAscending;
        this.removeAtDescendingFlag = removeDescending;
    }

    /**
     * Build a new altitude detector with slope selection.
     * <p>
     * The maximal interval between altitude checks should be smaller than the half duration of the minimal pass to
     * handle, otherwise some short passes could be missed.
     * </p>
     * 
     * @param altitudeIn threshold altitude value (m)
     * @param bodyShapeIn body shape with respect to which altitude should be evaluated
     * @param slopeSelection slope selection
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param action action performed
     * @param remove true if detector should be removed at altitude detection
     * @since 3.1
     */
    public AltitudeDetector(final double altitudeIn, final BodyShape bodyShapeIn,
        final int slopeSelection, final double maxCheck, final double threshold,
        final Action action, final boolean remove) {
        super(slopeSelection, maxCheck, threshold);
        this.altitude = altitudeIn;
        this.bodyShape = bodyShapeIn;

        if (slopeSelection == ASCENDING) {
            this.actionAtAscending = action;
            this.actionAtDescending = null;
            this.removeAtAscendingFlag = remove;
            this.removeAtDescendingFlag = false;
        } else if (slopeSelection == DESCENDING) {
            this.actionAtAscending = null;
            this.actionAtDescending = action;
            this.removeAtAscendingFlag = false;
            this.removeAtDescendingFlag = remove;
        } else {
            this.actionAtAscending = action;
            this.actionAtDescending = action;
            this.removeAtAscendingFlag = remove;
            this.removeAtDescendingFlag = remove;
        }
    }

    /**
     * Get the threshold altitude value.
     * 
     * @return the threshold altitude value (m)
     */
    public double getAltitude() {
        return this.altitude;
    }

    /**
     * Get the body shape.
     * 
     * @return the body shape
     */
    public BodyShape getBodyShape() {
        return this.bodyShape;
    }

    /**
     * Returns action at ascending detection.
     * 
     * @return action at ascending detection
     */
    public Action getActionAtAscending() {
        return this.actionAtAscending;
    }

    /**
     * Returns action at descending detection.
     * 
     * @return action at descending detection
     */
    public Action getActionAtDescending() {
        return this.actionAtDescending;
    }

    /**
     * Returns true if detection is removed after ascending detection.
     * 
     * @return true if detection is removed after ascending detection
     */
    public boolean removeAtAscending() {
        return this.removeAtAscendingFlag;
    }

    /**
     * Returns true if detection is removed after descending detection.
     * 
     * @return true if detection is removed after descending detection
     */
    public boolean removeAtDescending() {
        return this.removeAtDescendingFlag;
    }

    /** {@inheritDoc} */
    @Override
    public boolean shouldBeRemoved() {
        return this.shouldBeRemovedFlag;
    }

    /**
     * Handle an altitude event and choose what to do next.
     * 
     * @param s the current state information : date, kinematics, attitude
     * @param increasing if true, the value of the switching function increases when times increases
     *        around event
     * @param forward if true, the integration variable (time) increases during integration.
     * @return the action performed when altitude is reached.
     * @exception PatriusException if some specific error occurs
     */
    @Override
    public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                final boolean forward) throws PatriusException {
        final Action result;
        if (this.getSlopeSelection() == ASCENDING) {
            result = this.actionAtAscending;
            // remove (or not) detector
            this.shouldBeRemovedFlag = this.removeAtAscendingFlag;
        } else if (this.getSlopeSelection() == DESCENDING) {
            result = this.actionAtDescending;
            // remove (or not) detector
            this.shouldBeRemovedFlag = this.removeAtDescendingFlag;
        } else {
            if (forward ^ !increasing) {
                // increasing case
                result = this.actionAtAscending;
                // remove (or not) detector
                this.shouldBeRemovedFlag = this.removeAtAscendingFlag;
            } else {
                // decreasing case
                result = this.actionAtDescending;
                // remove (or not) detector
                this.shouldBeRemovedFlag = this.removeAtDescendingFlag;
            }
        }
        return result;
    }

    /**
     * Compute the value of the switching function. This function measures the difference between
     * the current altitude and the threshold altitude.
     * 
     * @param s the current state information: date, kinematics, attitude
     * @return value of the switching function
     * @exception PatriusException if some specific error occurs
     */
    @Override
    @SuppressWarnings("PMD.ShortMethodName")
    public double g(final SpacecraftState s) throws PatriusException {
        final Frame bodyFrame = this.bodyShape.getBodyFrame();
        final PVCoordinates pvBody = s.getPVCoordinates(bodyFrame);
        final GeodeticPoint point = this.bodyShape.transform(pvBody.getPosition(), bodyFrame,
            s.getDate());
        return point.getAltitude() - this.altitude;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The following attributes are not deeply copied:
     * <ul>
     * <li>bodyShape: {@link BodyShape}</li>
     * </ul>
     * </p>
     */
    @Override
    public EventDetector copy() {
        final EventDetector detector;
        if (this.getSlopeSelection() == ASCENDING) {
            detector = new AltitudeDetector(this.altitude, this.bodyShape, this.getSlopeSelection(),
                this.getMaxCheckInterval(), this.getThreshold(), this.actionAtAscending, this.removeAtAscendingFlag);
        } else if (this.getSlopeSelection() == DESCENDING) {
            detector = new AltitudeDetector(this.altitude, this.bodyShape, this.getSlopeSelection(),
                this.getMaxCheckInterval(), this.getThreshold(), this.actionAtDescending, this.removeAtDescendingFlag);
        } else {
            detector = new AltitudeDetector(this.altitude, this.bodyShape, this.getMaxCheckInterval(),
                this.getThreshold(), this.actionAtAscending, this.actionAtDescending, this.removeAtAscendingFlag,
                this.removeAtDescendingFlag);
        }
        return detector;
    }
}