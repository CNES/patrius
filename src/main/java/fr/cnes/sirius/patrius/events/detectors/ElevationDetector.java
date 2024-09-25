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
 */
/*
 *
 * HISTORY
* VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
* VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
* VERSION:4.9:DM:DM-3143:10/05/2022:[PATRIUS] Nouvelle interface OrbitEventDetector et nouvelles classes
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 *                             (added forward parameter to eventOccurred signature)
 * VERSION::DM:394:20/02/2015: Added possibility to define the action performed when event occurs
 * VERSION::DM:454:24/11/2015:Add constructors, overload method shouldBeRemoved() and adapt eventOccured()
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events.detectors;

import fr.cnes.sirius.patrius.events.AbstractDetector;
import fr.cnes.sirius.patrius.events.EventDetector;
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
        super(maxCheck, threshold, raising, setting, removeRaising, removeSetting);
        this.elevation = elevationIn;
        this.topo = topoIn;
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
        return super.eventOccurred(s, increasing, forward);
    }

    /**
     * Compute the value of the switching function. This function measures the difference between
     * the current elevation and the threshold elevation.
     * 
     * @param state state
     * @return value of the switching function
     * @throws PatriusException
     *         if some specific error occurs
     */
    @Override
    @SuppressWarnings("PMD.ShortMethodName")
    public double g(final SpacecraftState state) throws PatriusException {
        return this.topo.getElevation(state.getPVCoordinates().getPosition(), state.getFrame(), state.getDate())
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
            this.getActionAtEntry(), this.getActionAtExit(), this.isRemoveAtEntry(), this.isRemoveAtExit());
    }
}
