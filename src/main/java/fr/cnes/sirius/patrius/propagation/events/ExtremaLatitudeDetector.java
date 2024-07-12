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
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
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
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Detects when the spacecraft reaches the maximal or minimal local latitude. The local minimum or
 * maximum is chosen through a constructor parameter, with values {@link ExtremaLatitudeDetector#MIN},
 * {@link ExtremaLatitudeDetector#MAX} and {@link ExtremaLatitudeDetector#MIN_MAX} for both.
 * <p>
 * The default implementation behaviour is to {@link EventDetector.Action#STOP stop} propagation when the
 * minimum/maximum latitude is reached. This can be changed by using provided constructors.
 * </p>
 * 
 * @concurrency not thread-safe
 * 
 * @concurrency.comment attributes are mutable and related to propagation.
 * 
 * @see EventDetector
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id: ExtremaLatitudeDetector.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 1.1
 * 
 */
public class ExtremaLatitudeDetector extends AbstractDetector {

    /** Flag for local minimum latitude detection (g increasing). */
    public static final int MIN = 0;

    /** Flag for local maximum latitude detection (g decreasing). */
    public static final int MAX = 1;

    /** Flag for both local minimum and maximum distance detection. */
    public static final int MIN_MAX = 2;

     /** Serializable UID. */
    private static final long serialVersionUID = 8796265658333773051L;

    /** The body frame. */
    private final Frame bodyFrameIn;

    /** Action performed */
    private final Action actionExtremaLatitude;

    /**
     * Constructor for a ExtremaLatitudeDetector instance.
     * 
     * @param extremumType {@link ExtremaLatitudeDetector#MIN} for minimal latitude detection,
     *        {@link ExtremaLatitudeDetector#MAX} for maximal latitude detection or
     *        {@link ExtremaLatitudeDetector#MIN_MAX} for both minimal and maximal latitude
     *        detection
     * @param bodyFrame the body attached frame (in witch the latitude is defined)
     */
    public ExtremaLatitudeDetector(final int extremumType, final Frame bodyFrame) {
        this(extremumType, bodyFrame, DEFAULT_MAXCHECK, DEFAULT_THRESHOLD);
    }

    /**
     * Constructor for a ExtremaLatitudeDetector instance.
     * <p>
     * The default implementation behavior is to {@link EventDetector.Action#STOP stop} propagation when the expected
     * extremum is reached.
     * </p>
     * 
     * @param extremumType {@link ExtremaLatitudeDetector#MIN} for minimal latitude detection,
     *        {@link ExtremaLatitudeDetector#MAX} for maximal latitude detection or
     *        {@link ExtremaLatitudeDetector#MIN_MAX} for both minimal and maximal latitude
     *        detection
     * @param bodyFrame the body attached frame (in witch the latitude is defined)
     * @param maxCheck maximum check (see {@link AbstractDetector})
     * @param threshold threshold (see {@link AbstractDetector})
     */
    public ExtremaLatitudeDetector(final int extremumType, final Frame bodyFrame,
        final double maxCheck, final double threshold) {
        this(extremumType, bodyFrame, maxCheck, threshold, Action.STOP);
    }

    /**
     * Constructor for a ExtremaLatitudeDetector instance.
     * 
     * @param extremumType {@link ExtremaLatitudeDetector#MIN} for minimal latitude detection,
     *        {@link ExtremaLatitudeDetector#MAX} for maximal latitude detection or
     *        {@link ExtremaLatitudeDetector#MIN_MAX} for both minimal and maximal latitude
     *        detection
     * @param bodyFrame the body attached frame (in witch the latitude is defined)
     * @param maxCheck maximum check (see {@link AbstractDetector})
     * @param threshold threshold (see {@link AbstractDetector})
     * @param action action performed at extrema latitude detection
     */
    public ExtremaLatitudeDetector(final int extremumType, final Frame bodyFrame,
        final double maxCheck, final double threshold, final Action action) {
        this(extremumType, bodyFrame, maxCheck, threshold, action, false);
    }

    /**
     * Constructor for a ExtremaLatitudeDetector instance.
     * 
     * @param extremumType {@link ExtremaLatitudeDetector#MIN} for minimal latitude detection,
     *        {@link ExtremaLatitudeDetector#MAX} for maximal latitude detection or
     *        {@link ExtremaLatitudeDetector#MIN_MAX} for both minimal and maximal latitude
     *        detection
     * @param bodyFrame the body attached frame (in witch the latitude is defined)
     * @param maxCheck maximum check (see {@link AbstractDetector})
     * @param threshold threshold (see {@link AbstractDetector})
     * @param action action performed at extrema latitude detection
     * @param remove true if detector should be removed at extrema latitude detection
     * @since 3.1
     */
    public ExtremaLatitudeDetector(final int extremumType, final Frame bodyFrame,
        final double maxCheck, final double threshold, final Action action, final boolean remove) {
        super(extremumType, maxCheck, threshold);
        this.bodyFrameIn = bodyFrame;
        // action
        this.actionExtremaLatitude = action;
        // remove (or not) detector
        this.shouldBeRemovedFlag = remove;
    }

    /**
     * Handle an extrema latitude event and choose what to do next.
     * 
     * @param s the current state information : date, kinematics, attitude
     * @param increasing if true, the value of the switching function increases when times increases
     *        around event
     * @param forward if true, the integration variable (time) increases during integration.
     * @return the action performed when the expected extrema latitude is reached
     * @exception PatriusException if some specific error occurs
     */
    @Override
    public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                final boolean forward) throws PatriusException {
        return this.actionExtremaLatitude;
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.ShortMethodName")
    public double g(final SpacecraftState state) throws PatriusException {
        // the switching function is the z-component of the velocity in the
        // earth attached frame:
        return state.getPVCoordinates(this.bodyFrameIn).getVelocity().getZ();
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
        return new ExtremaLatitudeDetector(this.getSlopeSelection(), this.bodyFrameIn, this.getMaxCheckInterval(),
            this.getThreshold(), this.actionExtremaLatitude, this.shouldBeRemovedFlag);
    }
}
