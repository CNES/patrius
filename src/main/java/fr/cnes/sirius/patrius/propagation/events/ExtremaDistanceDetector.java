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
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3143:10/05/2022:[PATRIUS] Nouvelle interface OrbitEventDetector et nouvelles classes
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::DM:394:20/02/2015: Added possibility to define the action performed when event occurs
 * VERSION::DM:454:24/11/2015:Add constructors, overload method shouldBeRemoved() and adapt eventOccured()
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::FA:1970:03/01/2019:quality corrections
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.events;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Detects when the distance from the spacecraft to a given body reaches either a local minimum or a
 * local maximum.
 * <p>
 * The local minimum or maximum is chosen through a constructor parameter, with values
 * {@link ExtremaDistanceDetector#MIN}, {@link ExtremaDistanceDetector#MAX} and {@link ExtremaDistanceDetector#MIN_MAX}
 * for both.
 * 
 * <p>
 * The default implementation behavior is to {@link EventDetector.Action#STOP stop} propagation at minimum or/and
 * maximum distance depending on extremum type defined. This can be changed by using one of the following constructors :
 * </p>
 * <ul>
 * <li>
 * {@link #ExtremaDistanceDetector(PVCoordinatesProvider, int, double, double, 
 * fr.cnes.sirius.patrius.propagation.events.EventDetector.Action)
 * ExtremaDistanceDetector} : the defined action is performed at local minimum OR/AND maximum depending on slope
 * selection defined.
 * <li>
 * {@link #ExtremaDistanceDetector(PVCoordinatesProvider, double, double, 
 * fr.cnes.sirius.patrius.propagation.events.EventDetector.Action, 
 * fr.cnes.sirius.patrius.propagation.events.EventDetector.Action)
 * ExtremaDistanceDetector} : the defined actions are performed at local minimum AND maximum.
 * </ul>
 * <p>
 * 
 * @concurrency not thread-safe
 * 
 * @concurrency.comment attributes are mutable and related to propagation.
 * 
 * @see EventDetector
 * 
 * @author cardosop
 * 
 * @version $Id: ExtremaDistanceDetector.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 1.1
 */
@SuppressWarnings("PMD.NullAssignment")
public class ExtremaDistanceDetector extends AbstractDetector {

    /** Flag for local minimum distance detection (g increasing). */
    public static final int MIN = 0;

    /** Flag for local maximum distance detection (g decreasing). */
    public static final int MAX = 1;

    /** Flag for both local minimum and maximum distance detection. */
    public static final int MIN_MAX = 2;

     /** Serializable UID. */
    private static final long serialVersionUID = -6420170265427361960L;

    /** Distant body. */
    private final PVCoordinatesProvider body;

    /**
     * Constructor for a ExtremaDistanceDetector instance.
     * 
     * <p>
     * The default implementation behavior is to {@link EventDetector.Action#STOP stop} propagation when the expected
     * extrema is reached.
     * </p>
     * 
     * @param dBody the body whose distance is watched.
     * @param extremumType {@link ExtremaDistanceDetector#MIN} for shortest distance detection,
     *        {@link ExtremaDistanceDetector#MAX} for farthest distance detection or
     *        {@link ExtremaDistanceDetector#MIN_MAX} for both shortest and farthest distance
     *        detection
     */
    public ExtremaDistanceDetector(final PVCoordinatesProvider dBody, final int extremumType) {
        this(dBody, extremumType, DEFAULT_MAXCHECK, DEFAULT_THRESHOLD);
    }

    /**
     * Constructor for a ExtremaDistanceDetector instance with complimentary parameters.
     * 
     * <p>
     * The default implementation behavior is to {@link EventDetector.Action#STOP stop} propagation when the expected
     * extrema is reached.
     * </p>
     * 
     * @param dBody the body whose distance is watched.
     * @param extremumType {@link ExtremaDistanceDetector#MIN} for shortest distance detection,
     *        {@link ExtremaDistanceDetector#MAX} for farthest distance detection or
     *        {@link ExtremaDistanceDetector#MIN_MAX} for both shortest and farthest distance
     *        detection
     * @param maxCheck maximum check (see {@link AbstractDetector})
     * @param threshold threshold (see {@link AbstractDetector})
     */
    public ExtremaDistanceDetector(final PVCoordinatesProvider dBody, final int extremumType,
        final double maxCheck, final double threshold) {
        this(dBody, extremumType, maxCheck, threshold, Action.STOP);
    }

    /**
     * Constructor for both minimum and maximum distance .
     * 
     * @param dBody the body whose distance is watched.
     * @param maxCheck maximum check (see {@link AbstractDetector})
     * @param threshold threshold (see {@link AbstractDetector})
     * @param actionMin action to be perform when the expected local minimum is reached
     * @param actionMax action to be perform when the expected local maximum is reached
     */
    public ExtremaDistanceDetector(final PVCoordinatesProvider dBody, final double maxCheck,
        final double threshold, final Action actionMin, final Action actionMax) {
        this(dBody, maxCheck, threshold, actionMin, actionMax, false, false);
    }

    /**
     * Constructor for both minimum and maximum distance .
     * 
     * @param dBody the body whose distance is watched.
     * @param maxCheck maximum check (see {@link AbstractDetector})
     * @param threshold threshold (see {@link AbstractDetector})
     * @param actionMin action to be perform when the expected local minimum is reached
     * @param actionMax action to be perform when the expected local maximum is reached
     * @param removeMin true if detector should be removed when the expected local minimum is
     *        reached
     * @param removeMax true if detector should be removed when the expected local maximum is
     *        reached
     * @since 3.1
     */
    public ExtremaDistanceDetector(final PVCoordinatesProvider dBody, final double maxCheck,
        final double threshold, final Action actionMin, final Action actionMax,
        final boolean removeMin, final boolean removeMax) {
        super(maxCheck, threshold, actionMin, actionMax, removeMin, removeMax);
        // Final fields
        this.body = dBody;
    }

    /**
     * Constructor for a ExtremaDistanceDetector instance with specified action when extrema is
     * detected.
     * 
     * @param dBody the body whose distance is watched.
     * @param extremumType {@link ExtremaDistanceDetector#MIN} for shortest distance detection,
     *        {@link ExtremaDistanceDetector#MAX} for farthest distance detection or
     *        {@link ExtremaDistanceDetector#MIN_MAX} for both shortest and farthest distance
     *        detection
     * @param maxCheck maximum check (see {@link AbstractDetector})
     * @param threshold threshold (see {@link AbstractDetector})
     * @param action action to be performed when the expected extrema is reached
     * 
     */
    public ExtremaDistanceDetector(final PVCoordinatesProvider dBody, final int extremumType,
        final double maxCheck, final double threshold, final Action action) {
        this(dBody, extremumType, maxCheck, threshold, action, false);
    }

    /**
     * Constructor for a ExtremaDistanceDetector instance with specified action when extrema is
     * detected.
     * 
     * @param dBody the body whose distance is watched.
     * @param extremumType {@link ExtremaDistanceDetector#MIN} for shortest distance detection,
     *        {@link ExtremaDistanceDetector#MAX} for farthest distance detection or
     *        {@link ExtremaDistanceDetector#MIN_MAX} for both shortest and farthest distance
     *        detection
     * @param maxCheck maximum check (see {@link AbstractDetector})
     * @param threshold threshold (see {@link AbstractDetector})
     * @param action action to be performed when the expected extrema is reached
     * @param remove true if detector should be removed
     * 
     *        NB : If remove is true, it means detector should be removed at detection, so the value
     *        of attributes removeMIN and removeMAX must be decided according extremumType. Doing
     *        it, we ensure that detector will be removed well at propagation when calling method
     *        eventOccured (in which the value of attribute shouldBeRemoved is decided). In this
     *        case, users should better create an ExtremaDistanceDectector with constructor
     *        {@link ExtremaDistanceDetector#ExtremaDistanceDetector(PVCoordinatesProvider, double, double, 
     *        EventDetector.Action, EventDetector.Action, boolean, boolean)}
     *        .
     */
    public ExtremaDistanceDetector(final PVCoordinatesProvider dBody, final int extremumType,
        final double maxCheck, final double threshold, final Action action, final boolean remove) {
        super(extremumType, maxCheck, threshold);
        // Final fields
        this.body = dBody;
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
     * Handle an extrema distance event and choose what to do next.
     * 
     * @param s the current state information : date, kinematics, attitude
     * @param increasing if true, the value of the switching function increases when times increases
     *        around event
     * @param forward if true, the integration variable (time) increases during integration.
     * @return the action performed when the expected extremum is reached.
     * @exception PatriusException if some specific error occurs.
     */
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

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.ShortMethodName")
    public double g(final SpacecraftState state) throws PatriusException {
        final PVCoordinates sPV = state.getPVCoordinates();
        final PVCoordinates bPV = this.body.getPVCoordinates(state.getDate(), state.getFrame());
        // Computing the velocity of the body relative to the spacecraft
        final Vector3D sVelo = sPV.getVelocity();
        final Vector3D bodyVelo = bPV.getVelocity();
        final Vector3D distVelo = bodyVelo.subtract(sVelo);
        // Computing the position of the body relative to the spacecraft
        final Vector3D sVect = sPV.getPosition();
        final Vector3D bVect = bPV.getPosition();
        final Vector3D distVect = bVect.subtract(sVect);
        // Positive dot product means body going farther
        return Vector3D.dotProduct(distVect, distVelo);
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState s0, final AbsoluteDate t) {
        // Does nothing
    }

    /**
     * @return the body
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
        return new ExtremaDistanceDetector(this.body, this.getMaxCheckInterval(), this.getThreshold(),
            this.getActionAtEntry(), this.getActionAtExit(), this.isRemoveAtEntry(), this.isRemoveAtExit());
    }
}
