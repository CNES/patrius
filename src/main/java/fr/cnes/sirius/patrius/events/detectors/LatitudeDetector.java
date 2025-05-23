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
 * @history created 10/07/12
 *
 *
 * HISTORY
 * VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
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

package fr.cnes.sirius.patrius.events.detectors;

import fr.cnes.sirius.patrius.bodies.BodyPoint.BodyPointName;
import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.bodies.EllipsoidBodyShape;
import fr.cnes.sirius.patrius.bodies.EllipsoidPoint;
import fr.cnes.sirius.patrius.bodies.LLHCoordinatesSystem;
import fr.cnes.sirius.patrius.events.AbstractDetector;
import fr.cnes.sirius.patrius.events.EventDetector;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Detects when the spacecraft reaches a given local latitude.
 *
 * <p>
 * The default implementation behavior is to {@link EventDetector.Action#STOP stop} propagation when the expected
 * latitude is reached. This can be changed by overriding one of the following constructors :
 * </p>
 * <ul>
 * <li>
 * {@link #LatitudeDetector(double, BodyShape, int, double, double, Action) LatitudeDetector} : the defined action is
 * performed at latitude detection depending on slope type defined.
 * <li>
 * {@link #LatitudeDetector(double, BodyShape, double, double, Action, Action) LatitudeDetector} : the defined actions
 * are performed for local increasing AND decreasing latitude.
 * </ul>
 * <p>
 *
 * @concurrency not thread-safe
 *
 * @concurrency.comment attributes are mutable and related to propagation.
 *
 * @see EventDetector
 *
 * @author Thomas Trapier
 *
 * @version $Id: LatitudeDetector.java 17582 2017-05-10 12:58:16Z bignon $
 *
 * @since 1.2
 */
@SuppressWarnings("PMD.NullAssignment")
public class LatitudeDetector extends AbstractDetector {

    /** Flag for local increasing latitude detection (g increasing). */
    public static final int UP = 0;

    /** Flag for local decreasing latitude detection (g decreasing). */
    public static final int DOWN = 1;

    /** Flag for both local increasing and decreasing latitude detection. */
    public static final int UP_DOWN = 2;

    /** Serializable UID. */
    private static final long serialVersionUID = 2777403982588190629L;

    /** Latitude to detect */
    private final double latToDetect;

    /** Body shape */
    private final EllipsoidBodyShape body;

    /**
     * Constructor for the latitude detector.
     * <p>
     * The default implementation behavior is to {@link EventDetector.Action#STOP stop} propagation when the expected
     * latitude is reached.
     * </p>
     *
     * @param latitudeToDetect
     *        the latitude to detect (must be between -PI/2 and PI/2)
     * @param body
     *        the body shape
     * @param slopeType
     *        {@link LatitudeDetector#UP} for increasing latitude detection, {@link LatitudeDetector#DOWN} for
     *        decreasing latitude detection or {@link LatitudeDetector#UP_DOWN} for both increasing and decreasing
     *        latitude detection
     */
    public LatitudeDetector(final double latitudeToDetect, final EllipsoidBodyShape body, final int slopeType) {
        this(latitudeToDetect, body, slopeType, DEFAULT_MAXCHECK, DEFAULT_THRESHOLD);
    }

    /**
     * Constructor for the latitude detector.
     * <p>
     * The default implementation behavior is to {@link EventDetector.Action#STOP stop} propagation when the expected
     * latitude is reached.
     * </p>
     *
     * @param latitudeToDetect
     *        the latitude to detect (must be between -PI/2 and PI/2)
     * @param body
     *        the body shape
     * @param slopeType
     *        {@link LatitudeDetector#UP} for increasing latitude detection, {@link LatitudeDetector#DOWN} for
     *        decreasing latitude detection or {@link LatitudeDetector#UP_DOWN} for both increasing and decreasing
     *        latitude detection
     * @param maxCheck
     *        maximum check (see {@link AbstractDetector})
     * @param threshold
     *        threshold (see {@link AbstractDetector})
     */
    public LatitudeDetector(final double latitudeToDetect, final EllipsoidBodyShape body, final int slopeType,
                            final double maxCheck, final double threshold) {
        this(latitudeToDetect, body, slopeType, maxCheck, threshold, Action.STOP);
    }

    /**
     * Constructor for the latitude detector for both local increasing and decreasing latitude.
     *
     * @param latitudeToDetect
     *        the latitude to detect (must be between -PI/2 and PI/2)
     * @param body
     *        the body shape
     * @param maxCheck
     *        maximum check (see {@link AbstractDetector})
     * @param threshold
     *        threshold (see {@link AbstractDetector})
     * @param increasing
     *        action performed at latitude detection when increasing
     * @param decreasing
     *        action performed at latitude detection when decreasing
     */
    public LatitudeDetector(final double latitudeToDetect, final EllipsoidBodyShape body, final double maxCheck,
                            final double threshold, final Action increasing, final Action decreasing) {
        this(latitudeToDetect, body, maxCheck, threshold, increasing, decreasing, false, false);
    }

    /**
     * Constructor for the latitude detector for both local increasing and decreasing latitude.
     *
     * @param latitudeToDetect
     *        the latitude to detect (must be between -PI/2 and PI/2)
     * @param body
     *        the body shape
     * @param maxCheck
     *        maximum check (see {@link AbstractDetector})
     * @param threshold
     *        threshold (see {@link AbstractDetector})
     * @param increasing
     *        action performed at latitude detection when increasing
     * @param decreasing
     *        action performed at latitude detection when decreasing
     * @param removeIncreasing
     *        true if detector should be removed at latitude detection when increasing
     * @param removeDecreasing
     *        true if detector should be removed at latitude detection when decreasing
     * @since 3.1
     */
    public LatitudeDetector(final double latitudeToDetect, final EllipsoidBodyShape body, final double maxCheck,
                            final double threshold, final Action increasing, final Action decreasing,
                            final boolean removeIncreasing, final boolean removeDecreasing) {
        super(maxCheck, threshold, increasing, decreasing, removeIncreasing, removeDecreasing);
        // input latitude test
        if (latitudeToDetect > MathUtils.HALF_PI || latitudeToDetect < -MathUtils.HALF_PI) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.OUT_OF_RANGE_LATITUDE);
        }
        this.latToDetect = latitudeToDetect;
        this.body = body;
    }

    /**
     * Constructor for the latitude detector.
     *
     * @param latitudeToDetect
     *        the latitude to detect (must be between -PI/2 and PI/2)
     * @param body
     *        the body shape
     * @param slopeType
     *        {@link LatitudeDetector#UP} for increasing latitude detection, {@link LatitudeDetector#DOWN} for
     *        decreasing latitude detection or {@link LatitudeDetector#UP_DOWN} for both increasing and decreasing
     *        latitude detection
     * @param maxCheck
     *        maximum check (see {@link AbstractDetector})
     * @param threshold
     *        threshold (see {@link AbstractDetector})
     * @param action
     *        action performed at latitude detection
     */
    public LatitudeDetector(final double latitudeToDetect, final EllipsoidBodyShape body, final int slopeType,
                            final double maxCheck, final double threshold, final Action action) {
        this(latitudeToDetect, body, slopeType, maxCheck, threshold, action, false);
    }

    /**
     * Constructor for the latitude detector.
     *
     * @param latitudeToDetect
     *        the latitude to detect (must be between -PI/2 and PI/2)
     * @param body
     *        the body shape
     * @param slopeType
     *        {@link LatitudeDetector#UP} for increasing latitude detection, {@link LatitudeDetector#DOWN} for
     *        decreasing latitude detection or {@link LatitudeDetector#UP_DOWN} for both increasing and decreasing
     *        latitude detection
     * @param maxCheck
     *        maximum check (see {@link AbstractDetector})
     * @param threshold
     *        threshold (see {@link AbstractDetector})
     * @param action
     *        action performed at latitude detection
     * @param remove
     *        true if detector should be removed at latitude detection<br>
     *        NB : If remove is true, it means detector should be removed at detection, so the value of attributes
     *        removeAtIncreasing and removeAtDecreasing must be decided according slopeType. Doing it, we ensure that
     *        detector will be removed well at propagation when calling method eventOccured (in which the value of
     *        attribute shouldBeRemoved is decided). In this case, users should better create an LatitudeDetector with
     *        constructor {@link #LatitudeDetector(double, BodyShape, double, double, Action, Action, boolean, boolean)}
     * @since 3.1
     */
    public LatitudeDetector(final double latitudeToDetect, final EllipsoidBodyShape body, final int slopeType,
                            final double maxCheck, final double threshold, final Action action, final boolean remove) {
        super(slopeType, maxCheck, threshold);
        // input latitude test
        if (latitudeToDetect > MathUtils.HALF_PI || latitudeToDetect < -MathUtils.HALF_PI) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.OUT_OF_RANGE_LATITUDE);
        }
        this.latToDetect = latitudeToDetect;
        this.body = body;
        this.shouldBeRemovedFlag = remove;

        // If slopeSelection is different from 0, 1 or 2, an error has already been raised is
        // superclass.
        if (slopeType == UP) {
            this.actionAtEntry = action;
            this.actionAtExit = null;
            this.removeAtEntry = remove;
            this.removeAtExit = false;
        } else if (slopeType == DOWN) {
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

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState s0, final AbsoluteDate t) {
        // Nothing to do
    }

    /**
     * Handle a latitude reaching event and choose what to do next.
     * <p>
     * The default implementation behavior is to {@link EventDetector.Action#STOP stop} propagation when the expected
     * latitude is reached.
     * </p>
     *
     * @param s
     *        the current state information : date, kinematics, attitude
     * @param increasing
     *        if true, the value of the switching function increases when times increases around event
     * @param forward
     *        if true, the integration variable (time) increases during integration.
     * @return the action performed when the expected latitude is reached
     * @throws PatriusException
     *         if some specific error occurs
     */
    @Override
    public Action eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
        throws PatriusException {

        final Action result;
        if (getSlopeSelection() == 0) {
            result = getActionAtEntry();
            // remove (or not) detector
            this.shouldBeRemovedFlag = isRemoveAtEntry();
        } else if (getSlopeSelection() == 1) {
            result = getActionAtExit();
            // remove (or not) detector
            this.shouldBeRemovedFlag = isRemoveAtExit();
        } else {
            if (forward ^ !increasing) {
                // increasing case
                result = getActionAtEntry();
                // remove (or not) detector
                this.shouldBeRemovedFlag = isRemoveAtEntry();
            } else {
                // decreasing case
                result = getActionAtExit();
                // remove (or not) detector
                this.shouldBeRemovedFlag = isRemoveAtExit();
            }
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.ShortMethodName")
    public double g(final SpacecraftState state) throws PatriusException {
        // spacecraft position as a geodetic point
        final EllipsoidPoint point = this.body.buildPoint(state.getPVCoordinates().getPosition(), state.getFrame(),
            state.getDate(), BodyPointName.DEFAULT);
        return point.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC).getLatitude() - this.latToDetect;
    }

    /**
     * Getter for the latitude to detect.
     *
     * @return the latitude to detect
     */
    public double getLatitudeToDetect() {
        return this.latToDetect;
    }

    /**
     * Getter for the Earth shape.
     *
     * @return the earthShape
     */
    public BodyShape getEarthShape() {
        return this.body;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The following attributes are not deeply copied:
     * <ul>
     * <li>earthShape: {@link BodyShape}</li>
     * </ul>
     * </p>
     */
    @Override
    public EventDetector copy() {
        return new LatitudeDetector(this.latToDetect, this.body, getMaxCheckInterval(), getThreshold(),
            getActionAtEntry(),
            getActionAtExit(), isRemoveAtEntry(), isRemoveAtExit());
    }
}
