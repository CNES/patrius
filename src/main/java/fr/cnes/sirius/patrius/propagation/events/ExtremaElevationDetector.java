/**
 * 
 * Copyright 2011-2022 CNES
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
 * @history created 16/05/12
 * 
  * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 *                             (added forward parameter to eventOccurred signature)
  * VERSION::DM:394:20/02/2015: Added possibility to define the action performed when event occurs
  * VERSION::DM:454:24/11/2015:Add constructors, overload method shouldBeRemoved()
  * VERSION::FA:1307:11/09/2017:correct formulation of g() function
  * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 *
 * HISTORY
 * VERSION:4.9:DM:DM-3143:10/05/2022:[PATRIUS] Nouvelle interface OrbitEventDetector et nouvelles classes
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.events;

import fr.cnes.sirius.patrius.frames.TopocentricFrame;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Detects the time when the spacecraft reaches the elevation extrema in a given topocentric
 * frame.<pr> The local minimum or maximum is chosen through a constructor parameter, with values
 * {@link ExtremaElevationDetector#MIN}, {@link ExtremaElevationDetector#MAX} and
 * {@link ExtremaElevationDetector#MIN_MAX} for both.
 * <p>
 * The default implementation behaviour is to {@link EventDetector.Action#STOP stop} propagation when the
 * minimum/maximum elevation is reached. This can be changed by using provided constructors.
 * </p>
 * 
 * @concurrency not thread-safe
 * 
 * @concurrency.comment the link to the tree of frames makes this class not thread-safe
 * 
 * @see EventDetector
 * 
 * @author Thomas Trapier
 * 
 * @version $Id: ExtremaElevationDetector.java 17902 2017-09-11 09:17:02Z bignon $
 * 
 * @since 1.2
 */
public class ExtremaElevationDetector extends AbstractDetector {

    /** Flag for local minimum elevation detection. */
    public static final int MIN = 0;

    /** Flag for local maximum elevation detection. */
    public static final int MAX = 1;

    /** Flag for both local minimum and maximum elevation detection. */
    public static final int MIN_MAX = 2;

    /** Serial UID. */
    private static final long serialVersionUID = -4883716696126831700L;

    /** Topocentric frame in which elevation should be evaluated. */
    private final TopocentricFrame topo;

    /** Action performed */
    private final Action actionExtremaElevation;

    /**
     * Constructor for a min and max elevation detector.
     * <p>
     * This constructor takes default value for convergence threshold ({@link #DEFAULT_THRESHOLD}).
     * </p>
     * <p>
     * The maximal interval between elevation checks should be smaller than the half duration of the minimal pass to
     * handle, otherwise some short passes could be missed.
     * </p>
     * 
     * @param topoFrame topocentric frame in which elevation should be evaluated
     * @param extremumType {@link ExtremaElevationDetector#MIN} for minimal elevation detection,
     *        {@link ExtremaElevationDetector#MAX} for maximal elevation detection or
     *        {@link ExtremaElevationDetector#MIN_MAX} for both shortest and farthest elevation
     *        detection
     * @param maxCheck maximal checking interval (s)
     */
    public ExtremaElevationDetector(final TopocentricFrame topoFrame, final int extremumType,
        final double maxCheck) {
        this(topoFrame, extremumType, maxCheck, DEFAULT_THRESHOLD);
    }

    /**
     * Constructor for a min or max elevation detector.
     * <p>
     * The maximal interval between elevation checks should be smaller than the half duration of the minimal pass to
     * handle, otherwise some short passes could be missed.
     * </p>
     * <p>
     * The default implementation behavior is to {@link EventDetector.Action#STOP stop} propagation when the expected
     * extremum is reached.
     * </p>
     * 
     * @param topoFrame topocentric frame in which elevation should be evaluated
     * @param extremumType {@link ExtremaElevationDetector#MIN} for minimal elevation detection,
     *        {@link ExtremaElevationDetector#MAX} for maximal elevation detection or
     *        {@link ExtremaElevationDetector#MIN_MAX} for both shortest and farthest elevation
     *        detection
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     */
    public ExtremaElevationDetector(final TopocentricFrame topoFrame, final int extremumType,
        final double maxCheck, final double threshold) {
        this(topoFrame, extremumType, maxCheck, threshold, Action.STOP);
    }

    /**
     * Constructor for a min or max elevation detector.
     * <p>
     * The maximal interval between elevation checks should be smaller than the half duration of the minimal pass to
     * handle, otherwise some short passes could be missed.
     * </p>
     * 
     * @param topoFrame topocentric frame in which elevation should be evaluated
     * @param extremumType {@link ExtremaElevationDetector#MIN} for minimal elevation detection,
     *        {@link ExtremaElevationDetector#MAX} for maximal elevation detection or
     *        {@link ExtremaElevationDetector#MIN_MAX} for both shortest and farthest elevation
     *        detection
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param action action performed at extrema elevation detection
     */
    public ExtremaElevationDetector(final TopocentricFrame topoFrame, final int extremumType,
        final double maxCheck, final double threshold, final Action action) {
        this(topoFrame, extremumType, maxCheck, threshold, action, false);
    }

    /**
     * Constructor for a min or max elevation detector.
     * <p>
     * The maximal interval between elevation checks should be smaller than the half duration of the minimal pass to
     * handle, otherwise some short passes could be missed.
     * </p>
     * 
     * @param topoFrame topocentric frame in which elevation should be evaluated
     * @param extremumType {@link ExtremaElevationDetector#MIN} for minimal elevation detection,
     *        {@link ExtremaElevationDetector#MAX} for maximal elevation detection or
     *        {@link ExtremaElevationDetector#MIN_MAX} for both shortest and farthest elevation
     *        detection
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param action action performed at extrema elevation detection
     * @param remove true if detector should be removed at extrema elevation detection
     * @since 3.1
     */
    public ExtremaElevationDetector(final TopocentricFrame topoFrame, final int extremumType,
        final double maxCheck, final double threshold, final Action action, final boolean remove) {
        super(extremumType, maxCheck, threshold);
        this.topo = topoFrame;
        // action
        this.actionExtremaElevation = action;
        // remove (or not) detector
        this.shouldBeRemovedFlag = remove;
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState s0, final AbsoluteDate t) {
        // does nothing
    }

    /**
     * Handle an extrema distance event and choose what to do next.
     * 
     * @param s the current state information : date, kinematics, attitude
     * @param increasing if true, the value of the switching function increases when times increases
     *        around event
     * @param forward if true, the integration variable (time) increases during integration.
     * @return the action performed when the expected extrema elevation is reached
     * @exception PatriusException if some specific error occurs
     */
    @Override
    public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                final boolean forward) throws PatriusException {
        return this.actionExtremaElevation;
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.ShortMethodName")
    public double g(final SpacecraftState state) throws PatriusException {
        // Event occurs when elevation rate in topocentric frame cancels
        return this.topo.getElevationRate(state.getPVCoordinates(), state.getFrame(), state.getDate());
    }

    /**
     * @return the Topocentric frame in which elevation should be evaluated.
     */
    public TopocentricFrame getTopocentricFrame() {
        return this.topo;
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
        return new ExtremaElevationDetector(this.topo, this.getSlopeSelection(), this.getMaxCheckInterval(),
            this.getThreshold(), this.actionExtremaElevation, this.shouldBeRemovedFlag);
    }
}
