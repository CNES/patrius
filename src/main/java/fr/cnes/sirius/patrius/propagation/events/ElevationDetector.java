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

import fr.cnes.sirius.patrius.frames.TopocentricFrame;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Finder for satellite raising/setting events.
 * <p>
 * This class finds elevation events (i.e. satellite raising and setting).
 * </p>
 * <p>
 * The default implementation behavior is to {@link EventDetector.Action#CONTINUE continue} propagation at raising and
 * to {@link EventDetector.Action#STOP stop} propagation at setting. This can be changed by using provided constructors.
 * </p>
 * 
 * @see fr.cnes.sirius.patrius.propagation.Propagator#addEventDetector(EventDetector)
 * @author Luc Maisonobe
 */
public class ElevationDetector extends AbstractDetector {

    /** Serializable UID. */
    private static final long serialVersionUID = 4571340030201230951L;

    /** Threshold elevation value. */
    private final double elevation;

    /** Topocentric frame in which elevation should be evaluated. */
    private final TopocentricFrame topo;

    /** Action performed when propagation at raising. */
    private final Action actionAtRaising;

    /** Action performed when propagation at setting. */
    private final Action actionAtSetting;

    /** True if detector should be removed at raising. */
    private final boolean removeAtRaising;

    /** True if detector should be removed at setting. */
    private final boolean removeAtSetting;

    /** True if detector should be removed (updated by eventOccured). */
    private boolean shouldBeRemovedFlag = false;

    /**
     * Build a new elevation detector.
     * <p>
     * This simple constructor takes default values for maximal checking interval ( {@link #DEFAULT_MAXCHECK}) and
     * convergence threshold ({@link #DEFAULT_THRESHOLD}).
     * </p>
     * 
     * @param elevationIn threshold elevation value (rad)
     * @param topoIn topocentric frame in which elevation should be evaluated
     */
    public ElevationDetector(final double elevationIn, final TopocentricFrame topoIn) {
        this(elevationIn, topoIn, DEFAULT_MAXCHECK, DEFAULT_THRESHOLD);
    }

    /**
     * Build a new elevation detector.
     * <p>
     * This constructor takes default value for convergence threshold ({@link #DEFAULT_THRESHOLD}).
     * </p>
     * <p>
     * The maximal interval between elevation checks should be smaller than the half duration of the minimal pass to
     * handle, otherwise some short passes could be missed.
     * </p>
     * 
     * @param elevationIn threshold elevation value (rad)
     * @param topoIn topocentric frame in which elevation should be evaluated
     * @param maxCheck maximal checking interval (s)
     */
    public ElevationDetector(final double elevationIn, final TopocentricFrame topoIn,
        final double maxCheck) {
        this(elevationIn, topoIn, maxCheck, DEFAULT_THRESHOLD);
    }

    /**
     * Build a new elevation detector.
     * <p>
     * The maximal interval between elevation checks should be smaller than the half duration of the minimal pass to
     * handle, otherwise some short passes could be missed.
     * </p>
     * <p>
     * The default implementation behavior is to {@link EventDetector.Action#CONTINUE continue} propagation at raising
     * and to {@link EventDetector.Action#STOP stop} propagation at setting.
     * </p>
     * 
     * @param elevationIn threshold elevation value (rad)
     * @param topoIn topocentric frame in which elevation should be evaluated
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     */
    public ElevationDetector(final double elevationIn, final TopocentricFrame topoIn,
        final double maxCheck, final double threshold) {
        this(elevationIn, topoIn, maxCheck, threshold, Action.CONTINUE, Action.STOP);

    }

    /**
     * Build a new elevation detector.
     * <p>
     * The maximal interval between elevation checks should be smaller than the half duration of the minimal pass to
     * handle, otherwise some short passes could be missed.
     * </p>
     * 
     * @param elevationIn threshold elevation value (rad)
     * @param topoIn topocentric frame in which elevation should be evaluated
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param raising action performed when propagation at raising
     * @param setting action performed when propagation at setting
     */
    public ElevationDetector(final double elevationIn, final TopocentricFrame topoIn,
        final double maxCheck, final double threshold, final Action raising,
        final Action setting) {
        this(elevationIn, topoIn, maxCheck, threshold, raising, setting, false, false);
    }

    /**
     * Build a new elevation detector.
     * <p>
     * The maximal interval between elevation checks should be smaller than the half duration of the minimal pass to
     * handle, otherwise some short passes could be missed.
     * </p>
     * 
     * @param elevationIn threshold elevation value (rad)
     * @param topoIn topocentric frame in which elevation should be evaluated
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param raising action performed when propagation at raising
     * @param setting action performed when propagation at setting
     * @param removeRaising if detector should be removed at raising
     * @param removeSetting if detector should be removed at setting
     * @since 3.1
     */
    public ElevationDetector(final double elevationIn, final TopocentricFrame topoIn,
        final double maxCheck, final double threshold, final Action raising,
        final Action setting, final boolean removeRaising, final boolean removeSetting) {
        super(maxCheck, threshold);
        this.elevation = elevationIn;
        this.topo = topoIn;
        // action
        this.actionAtRaising = raising;
        this.actionAtSetting = setting;
        // remove (or not) detector
        this.removeAtRaising = removeRaising;
        this.removeAtSetting = removeSetting;
    }

    /**
     * Get the threshold elevation value.
     * 
     * @return the threshold elevation value (rad)
     */
    public double getElevation() {
        return this.elevation;
    }

    /**
     * Get the topocentric frame.
     * 
     * @return the topocentric frame
     */
    public TopocentricFrame getTopocentricFrame() {
        return this.topo;
    }

    /**
     * Handle an elevation event and choose what to do next.
     * 
     * @param s the current state information : date, kinematics, attitude
     * @param increasing if true, the value of the switching function increases when times increases
     *        around event
     * @param forward if true, the integration variable (time) increases during integration.
     * @return the action performed when propagation raising or setting.
     * @exception PatriusException if some specific error occurs
     */
    @Override
    public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                final boolean forward) throws PatriusException {
        if (increasing) {
            this.shouldBeRemovedFlag = this.removeAtRaising;
        } else {
            this.shouldBeRemovedFlag = this.removeAtSetting;
        }
        return increasing ? this.actionAtRaising : this.actionAtSetting;
    }

    /** {@inheritDoc} */
    @Override
    public boolean shouldBeRemoved() {
        return this.shouldBeRemovedFlag;
    }

    /**
     * Compute the value of the switching function. This function measures the difference between
     * the current elevation and the threshold elevation.
     * 
     * @param s the current state information: date, kinematics, attitude
     * @return value of the switching function
     * @exception PatriusException if some specific error occurs
     */
    @Override
    @SuppressWarnings("PMD.ShortMethodName")
    public double g(final SpacecraftState s) throws PatriusException {
        return this.topo.getElevation(s.getPVCoordinates().getPosition(), s.getFrame(), s.getDate())
            - this.elevation;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The following attributes are not deeply copied:
     * <ul>
     * <li>topo: {@link TopocentricFrame}</li>
     * </ul>
     * </p>
     */
    @Override
    public EventDetector copy() {
        return new ElevationDetector(this.elevation, this.topo, this.getMaxCheckInterval(), this.getThreshold(),
            this.actionAtRaising, this.actionAtSetting, this.removeAtRaising, this.removeAtSetting);
    }
}
