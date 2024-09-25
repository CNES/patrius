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
 * @history created 27/02/12
 *
 *
 * HISTORY
 * VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration gestion attractions gravitationnelles
 * VERSION:4.11:DM:DM-17:22/05/2023:[PATRIUS] Detecteur de distance a la surface d'un corps celeste
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3143:10/05/2022:[PATRIUS] Nouvelle interface OrbitEventDetector et nouvelles classes
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::DM:394:20/02/2015: Added possibility to define the action performed when event occurs
 * VERSION::DM:454:24/11/2015:Add shouldBeRemoved method
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events.detectors;

import fr.cnes.sirius.patrius.events.AbstractDetector;
import fr.cnes.sirius.patrius.events.EventDetector;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Detects when the distance from the spacecraft to a given body reaches a predetermined value.
 * <p>
 * The default implementation behaviour is to {@link EventDetector.Action#STOP stop} propagation when the distance is
 * reached. This can be changed by using provided constructors.
 * </p>
 * 
 * @concurrency not thread-safe
 * 
 * @concurrency.comment attributes are mutable and related to propagation.
 * 
 * @see EventDetector
 * 
 * @author cardosop
 * 
 * @version $Id: DistanceDetector.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 1.1
 */
@SuppressWarnings("PMD.NullAssignment")
public class DistanceDetector extends AbstractDetector {

    /** Flag for increasing distance detection (slopeSelection = 0). */
    public static final int INCREASING = 0;

    /** Flag for decreasing distance detection (slopeSelection = 1). */
    public static final int DECREASING = 1;

    /** Flag for both increasing and decreasing distance detection (slopeSelection = 2). */
    public static final int INCREASING_DECREASING = 2;

    /** Serializable UID. */
    private static final long serialVersionUID = 8595027493313459831L;

    /** Distance triggering the event. */
    protected final double distance;

    /** Distant body. */
    protected final PVCoordinatesProvider body;

    /**
     * Constructor for a DistanceDetector instance.
     * <p>
     * This simple constructor takes default values for maximal checking interval ({@link #DEFAULT_MAXCHECK}) and
     * convergence threshold ({@link #DEFAULT_THRESHOLD}).
     * </p>
     * <p>
     * The default implementation behavior is to {@link EventDetector.Action#STOP stop} propagation when the distance is
     * reached.
     * </p>
     * <p>
     * The distanceIn parameter must be positive.
     * </p>
     * 
     * @param bodyIn
     *        The body whose distance is watched.
     * @param distanceIn
     *        Distance triggering the event (m)
     * @throws IllegalArgumentException when the distance is negative
     */
    public DistanceDetector(final PVCoordinatesProvider bodyIn, final double distanceIn) {
        this(bodyIn, distanceIn, DEFAULT_MAXCHECK, DEFAULT_THRESHOLD);
    }

    /**
     * Constructor for a DistanceDetector instance with additional maxCheck and threshold parameters
     * 
     * <p>
     * The default implementation behavior is to {@link EventDetector.Action#STOP stop} propagation when the distance is
     * reached.
     * </p>
     * 
     * @param bodyIn
     *        The body whose distance is watched
     * @param distanceIn
     *        Distance triggering the event (m)
     * @param maxCheck
     *        Maximal checking interval (s)
     * @param threshold
     *        Convergence threshold (s)
     * @throws IllegalArgumentException when the distance is negative
     */
    public DistanceDetector(final PVCoordinatesProvider bodyIn, final double distanceIn,
                            final double maxCheck, final double threshold) {
        this(bodyIn, distanceIn, maxCheck, threshold, Action.STOP, Action.STOP);
    }

    /**
     * Constructor for a DistanceDetector instance with complimentary parameters.
     * 
     * @param bodyIn
     *        The body whose distance is watched
     * @param distanceIn
     *        Distance triggering the event (m)
     * @param maxCheck
     *        Maximal checking interval (s)
     * @param threshold
     *        Convergence threshold (s)
     * @param actionIncreasing
     *        Action performed at distance detection when distance is increasing
     * @param actionDecreasing
     *        Action performed at distance detection when distance is decreasing
     * @throws IllegalArgumentException when the distance is negative
     */
    public DistanceDetector(final PVCoordinatesProvider bodyIn, final double distanceIn,
                            final double maxCheck, final double threshold, final Action actionIncreasing,
                            final Action actionDecreasing) {
        this(bodyIn, distanceIn, maxCheck, threshold, actionIncreasing, actionDecreasing, false, false);
    }

    /**
     * Constructor for a DistanceDetector instance with complimentary parameters.
     * 
     * @param bodyIn
     *        The body whose distance is watched
     * @param distanceIn
     *        Distance triggering the event (m)
     * @param maxCheck
     *        Maximal checking interval (s)
     * @param threshold
     *        Convergence threshold (s)
     * @param actionIncreasing
     *        Action performed at distance detection when distance is increasing
     * @param actionDecreasing
     *        Action performed at distance detection when distance is decreasing
     * @param removeIncreasing
     *        True if detector should be removed at increasing distance detection
     * @param removeDecreasing
     *        True if detector should be removed at decreasing distance detection
     * @throws IllegalArgumentException when the distance is negative
     */
    public DistanceDetector(final PVCoordinatesProvider bodyIn, final double distanceIn,
                            final double maxCheck, final double threshold, final Action actionIncreasing,
                            final Action actionDecreasing, final boolean removeIncreasing,
                            final boolean removeDecreasing) {
        super(maxCheck, threshold, actionIncreasing, actionDecreasing, removeIncreasing, removeDecreasing);
        // Validate input
        if ((!(this instanceof SurfaceDistanceDetector)) && distanceIn < 0.) {
            throw PatriusException
                .createIllegalArgumentException(PatriusMessages.NOT_POSITIVE_DISTANCE);
        }
        // Final fields
        this.body = bodyIn;
        this.distance = distanceIn;
    }

    /**
     * Build a new altitude detector with slope selection.
     * <p>
     * The maximal interval between altitude checks should be smaller than the half duration of the minimal pass to
     * handle, otherwise some short passes could be missed.
     * </p>
     * 
     * @param bodyIn
     *        The body whose distance is watched
     * @param distanceIn
     *        Distance triggering the event (m)
     * @param slopeSelection
     *        The g-function slope selection (0,1 or 2)
     * @param maxCheck
     *        Maximal checking interval (s)
     * @param threshold
     *        Convergence threshold (s)
     * @param action
     *        Action performed at distance detection
     * @param remove
     *        True if detector should be removed at distance detection
     */
    public DistanceDetector(final PVCoordinatesProvider bodyIn, final double distanceIn,
                            final int slopeSelection, final double maxCheck, final double threshold,
                            final Action action, final boolean remove) {
        super(slopeSelection, maxCheck, threshold);        
        // Validate input
        if ((!(this instanceof SurfaceDistanceDetector)) && distanceIn < 0.) {
            throw PatriusException
                .createIllegalArgumentException(PatriusMessages.NOT_POSITIVE_DISTANCE);
        }
        this.distance = distanceIn;
        this.body = bodyIn;

        if (slopeSelection == INCREASING) {
            this.actionAtEntry = action;
            this.actionAtExit = null;
            this.removeAtEntry = remove;
            this.removeAtExit = false;
        } else if (slopeSelection == DECREASING) {
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
     * Handle a distance event and choose what to do next.
     * 
     * @param s the current state information : date, kinematics, attitude
     * @param increasing if true, the value of the switching function increases when time increases
     *        around event
     * @param forward if true, the integration variable (time) increases during integration.
     * @return the action performed when the expected distance is reached
     * @exception PatriusException if some specific error occurs
     */
    @Override
    public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                final boolean forward) throws PatriusException {
        final Action result;
        if (this.getSlopeSelection() == INCREASING) {
            result = this.getActionAtEntry();
            // remove (or not) detector
            this.shouldBeRemovedFlag = this.isRemoveAtEntry();
        } else if (this.getSlopeSelection() == DECREASING) {
            result = this.getActionAtExit();
            // remove (or not) detector
            this.shouldBeRemovedFlag = this.isRemoveAtExit();
        } else {
            if (forward ^ !increasing) {
                // increasing case
                result = this.getActionAtEntry();
                // remove (or not) detector
                this.shouldBeRemovedFlag = this.isRemoveAtEntry();
            } else {
                // decreasing case
                result = this.getActionAtExit();
                // remove (or not) detector
                this.shouldBeRemovedFlag = this.isRemoveAtExit();
            }
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.ShortMethodName")
    public double g(final SpacecraftState state) throws PatriusException {
        final PVCoordinates sPV = state.getPVCoordinates();
        final PVCoordinates bPV = this.body.getPVCoordinates(state.getDate(), state.getFrame());
        // Computing the position of the body relative to the spacecraft
        final Vector3D sVect = sPV.getPosition();
        final Vector3D bVect = bPV.getPosition();
        final Vector3D distVect = bVect.subtract(sVect);
        // The norm is the distance
        return distVect.getNorm() - this.distance;
    }

    /**
     * Returns the distance triggering the event.
     * 
     * @return the distance triggering the event (m)
     */
    public double getDistance() {
        return this.distance;
    }

    /**
     * Returns the body whose distance is watched.
     * 
     * @return the body whose distance is watched
     */
    public PVCoordinatesProvider getBody() {
        return this.body;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The following attributes are not deeply copied:
     * <ul>
     * <li>body: {@link PVCoordinatesProvider}</li>
     * </ul>
     * </p>
     */
    @Override
    public EventDetector copy() {
        final EventDetector detector;
        if (this.getSlopeSelection() == INCREASING) {
            detector = new DistanceDetector(this.body, this.distance, this.getSlopeSelection(),
                this.getMaxCheckInterval(), this.getThreshold(), this.getActionAtEntry(), this.isRemoveAtEntry());
        } else if (this.getSlopeSelection() == DECREASING) {
            detector = new DistanceDetector(this.body, this.distance, this.getSlopeSelection(),
                this.getMaxCheckInterval(), this.getThreshold(), this.getActionAtExit(), this.isRemoveAtExit());
        } else {
            detector = new DistanceDetector(this.body, this.distance, this.getMaxCheckInterval(),
                this.getThreshold(), this.getActionAtEntry(), this.getActionAtExit(), this.isRemoveAtEntry(),
                this.isRemoveAtExit());
        }
        return detector;
    }
}
