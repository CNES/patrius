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
package fr.cnes.sirius.patrius.propagation.events;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
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
public class DistanceDetector extends AbstractDetector {

     /** Serializable UID. */
    private static final long serialVersionUID = 8595027493313459831L;

    /** Distance triggering the event. */
    private final double distance;

    /** Distant body. */
    private final PVCoordinatesProvider body;

    /** Action performed */
    private final Action actionDistance;

    /**
     * Constructor for a DistanceDetector instance.
     * 
     * @param dBody the body whose distance is watched.
     * @param dist distance in m triggering the event.
     * @throws IllegalArgumentException when the distance is negative
     */
    public DistanceDetector(final PVCoordinatesProvider dBody, final double dist) {
        this(dBody, dist, DEFAULT_MAXCHECK, DEFAULT_THRESHOLD);
    }

    /**
     * Constructor for a DistanceDetector instance with complimentary parameters.
     * <p>
     * The default implementation behavior is to {@link EventDetector.Action#STOP stop} propagation when the distance is
     * reached.
     * </p>
     * 
     * @param dBody the body whose distance is watched.
     * @param dist distance in m triggering the event.
     * @param maxCheck maximum check (see {@link AbstractDetector})
     * @param threshold threshold (see {@link AbstractDetector})
     * @throws IllegalArgumentException when the distance is negative
     */
    public DistanceDetector(final PVCoordinatesProvider dBody, final double dist,
        final double maxCheck, final double threshold) {
        this(dBody, dist, maxCheck, threshold, Action.STOP);
    }

    /**
     * Constructor for a DistanceDetector instance with complimentary parameters.
     * 
     * @param dBody the body whose distance is watched.
     * @param dist distance in m triggering the event.
     * @param maxCheck maximum check (see {@link AbstractDetector})
     * @param threshold threshold (see {@link AbstractDetector})
     * @param action action performed at distance detection
     * @throws IllegalArgumentException when the distance is negative
     */
    public DistanceDetector(final PVCoordinatesProvider dBody, final double dist,
        final double maxCheck, final double threshold, final Action action) {
        this(dBody, dist, maxCheck, threshold, action, false);
    }

    /**
     * Constructor for a DistanceDetector instance with complimentary parameters.
     * 
     * @param dBody the body whose distance is watched.
     * @param dist distance in m triggering the event.
     * @param maxCheck maximum check (see {@link AbstractDetector})
     * @param threshold threshold (see {@link AbstractDetector})
     * @param action action performed at distance detection
     * @param remove true if detector should be removed
     * @since 3.1
     * @throws IllegalArgumentException when the distance is negative
     */
    public DistanceDetector(final PVCoordinatesProvider dBody, final double dist,
        final double maxCheck, final double threshold, final Action action, final boolean remove) {
        super(maxCheck, threshold);
        // Validate input
        if (dist < 0.) {
            throw PatriusException
                .createIllegalArgumentException(PatriusMessages.NOT_POSITIVE_DISTANCE);
        }
        // Final fields
        this.body = dBody;
        this.distance = dist;
        // action
        this.actionDistance = action;
        // remove (or not) detector
        this.shouldBeRemovedFlag = remove;
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
        return this.actionDistance;
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
        return (distVect.getNorm() - this.distance);
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState s0, final AbsoluteDate t) {
        // Does nothing
    }

    /**
     * Returns the distance triggering the event.
     * 
     * @return the distance triggering the event.
     */
    public double getDistance() {
        return this.distance;
    }

    /**
     * Returns the body.
     * 
     * @return the body.
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
        return new DistanceDetector(this.body, this.distance, this.getMaxCheckInterval(), this.getThreshold(),
            this.actionDistance, this.shouldBeRemovedFlag);
    }
}
