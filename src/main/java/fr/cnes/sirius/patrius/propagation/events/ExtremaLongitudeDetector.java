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
 * @history created 02/03/2012
 *
 * HISTORY
 * VERSION:4.9:DM:DM-3143:10/05/2022:[PATRIUS] Nouvelle interface OrbitEventDetector et nouvelles classes
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::DM:394:20/02/2015: Added possibility to define the action performed when event occurs
 * VERSION::DM:454:24/11/2015:Add constructors, overload method shouldBeRemoved()
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.events;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Detects when the spacecraft reaches the maximal or minimal local longitude. The local minimum or
 * maximum is chosen through a constructor parameter, with values {@link ExtremaLongitudeDetector#MIN},
 * {@link ExtremaLongitudeDetector#MAX} and {@link ExtremaLongitudeDetector#MIN_MAX} for both.
 * <p>
 * The default implementation behaviour is to {@link EventDetector.Action#STOP stop} propagation when the
 * minimum/maximum longitude is reached. This can be changed by using provided constructors.
 * </p>
 * 
 * @concurrency not thread-safe
 * 
 * @concurrency.comment attributes are mutable and related to propagation.
 * 
 * @see EventDetector
 * 
 * @author Philippe CHABAUD
 * 
 * @version $Id: ExtremaLongitudeDetector.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 1.3
 * 
 */
public class ExtremaLongitudeDetector extends AbstractDetector {

    /** Flag for local minimum longitude detection (g increasing). */
    public static final int MIN = 0;

    /** Flag for local maximum longitude detection (g decreasing). */
    public static final int MAX = 1;

    /** Flag for both local minimum and maximum distance detection. */
    public static final int MIN_MAX = 2;

    /** Serial UID. */
    private static final long serialVersionUID = -247746125541484370L;

    /** The body frame. */
    private final Frame bodyFrameIn;

    /** Action performed */
    private final Action actionExtremaLongitude;

    /**
     * Constructor for a ExtremaLongitudeDetector instance.
     * 
     * @param extremumType {@link ExtremaLongitudeDetector#MIN} for minimal longitude detection,
     *        {@link ExtremaLongitudeDetector#MAX} for maximal longitude detection or
     *        {@link ExtremaLongitudeDetector#MIN_MAX} for both minimal and maximal longitude
     *        detection
     * @param bodyFrame the body attached frame (in witch the longitude is defined)
     */
    public ExtremaLongitudeDetector(final int extremumType, final Frame bodyFrame) {
        this(extremumType, bodyFrame, DEFAULT_MAXCHECK, DEFAULT_THRESHOLD);
    }

    /**
     * Constructor for a ExtremaLongitudeDetector instance.
     * <p>
     * The default implementation behavior is to {@link EventDetector.Action#STOP stop} propagation when the expected
     * extremum is reached.
     * </p>
     * 
     * @param extremumType {@link ExtremaLongitudeDetector#MIN} for minimal longitude detection,
     *        {@link ExtremaLongitudeDetector#MAX} for maximal longitude detection or
     *        {@link ExtremaLongitudeDetector#MIN_MAX} for both minimal and maximal longitude
     *        detection
     * @param bodyFrame the body attached frame (in witch the longitude is defined)
     * @param maxCheck maximum check (see {@link AbstractDetector})
     * @param threshold threshold (see {@link AbstractDetector})
     */
    public ExtremaLongitudeDetector(final int extremumType, final Frame bodyFrame,
        final double maxCheck, final double threshold) {
        this(extremumType, bodyFrame, maxCheck, threshold, Action.STOP);
    }

    /**
     * Constructor for a ExtremaLongitudeDetector instance.
     * 
     * @param extremumType {@link ExtremaLongitudeDetector#MIN} for minimal longitude detection,
     *        {@link ExtremaLongitudeDetector#MAX} for maximal longitude detection or
     *        {@link ExtremaLongitudeDetector#MIN_MAX} for both minimal and maximal longitude
     *        detection
     * @param bodyFrame the body attached frame (in witch the longitude is defined)
     * @param maxCheck maximum check (see {@link AbstractDetector})
     * @param threshold threshold (see {@link AbstractDetector})
     * @param action action performed at extrema longitude detection
     */
    public ExtremaLongitudeDetector(final int extremumType, final Frame bodyFrame,
        final double maxCheck, final double threshold, final Action action) {
        this(extremumType, bodyFrame, maxCheck, threshold, action, false);
    }

    /**
     * Constructor for a ExtremaLongitudeDetector instance.
     * 
     * @param extremumType {@link ExtremaLongitudeDetector#MIN} for minimal longitude detection,
     *        {@link ExtremaLongitudeDetector#MAX} for maximal longitude detection or
     *        {@link ExtremaLongitudeDetector#MIN_MAX} for both minimal and maximal longitude
     *        detection
     * @param bodyFrame the body attached frame (in witch the longitude is defined)
     * @param maxCheck maximum check (see {@link AbstractDetector})
     * @param threshold threshold (see {@link AbstractDetector})
     * @param action action performed at extrema longitude detection
     * @param remove true if detector should be removed at extrema longitude detection
     * @since 3.1
     */
    public ExtremaLongitudeDetector(final int extremumType, final Frame bodyFrame,
        final double maxCheck, final double threshold, final Action action, final boolean remove) {
        super(extremumType, maxCheck, threshold);
        this.bodyFrameIn = bodyFrame;
        // action
        this.actionExtremaLongitude = action;
        // remove (or not) detector
        this.shouldBeRemovedFlag = remove;
    }

    /**
     * Handle an extrema distance event and choose what to do next.
     * 
     * @param s the current state information : date, kinematics, attitude
     * @param increasing if true, the value of the switching function increases when times increases
     *        around event
     * @param forward if true, the integration variable (time) increases during integration.
     * @return the action performed when the expected extrema longitude is reached
     * @exception PatriusException if some specific error occurs
     */
    @Override
    public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                final boolean forward) throws PatriusException {
        return this.actionExtremaLongitude;
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.ShortMethodName")
    public double g(final SpacecraftState state) throws PatriusException {
        // the switching function is position.(Vz^Vrel).
        // It means that when the relative velocity is colinear to Z
        // we have a local extrema to detect
        final Vector3D p = state.getPVCoordinates(this.bodyFrameIn).getPosition();
        final Vector3D vrel = state.getPVCoordinates(this.bodyFrameIn).getVelocity();

        return Vector3D.dotProduct(p, Vector3D.crossProduct(vrel, Vector3D.PLUS_K));

    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState s0, final AbsoluteDate t) {
        // Does nothing
    }

    /**
     * Returns the body frame.
     * 
     * @return the body frame
     */
    public Frame getBodyFrame() {
        return this.bodyFrameIn;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The following attributes are not deeply copied:
     * <ul>
     * <li>bodyFrameIn: {@link Frame}</li>
     * </ul>
     * </p>
     */
    @Override
    public EventDetector copy() {
        return new ExtremaLongitudeDetector(this.getSlopeSelection(), this.bodyFrameIn,
            this.getMaxCheckInterval(), this.getThreshold(), this.actionExtremaLongitude, this.shouldBeRemovedFlag);
    }
}
