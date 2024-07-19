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
 * @history created 06/02/12
 * 
  * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 *                             (added forward parameter to eventOccurred signature)
  * VERSION::DM:394:20/02/2015: Added possibility to define the action performed when event occurs
  * VERSION::DM:454:24/11/2015:Add method shouldBeRemoved()
  * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
  * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3172:10/05/2022:[PATRIUS] Ajout d'un throws PatriusException a la methode init de l'interface EDet
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events;

import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.AbstractDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * This class finds the events resulting from the combination of two phenomena. Combinations of phenomena can be done
 * using the following boolean operators:<br>
 * <ul>
 * <li>
 * AND : combined events are detected only both phenomena associated to event1 and event2 are active (i.e. an event is
 * detected if event1 is triggered and phenomenon associated to event2 is active, or vice-versa);</li>
 * <li>
 * OR : combined events are detected if at least one of the phenomena associated to event1 and event2 is active (i.e. an
 * event is detected if event1 or event2 are triggered); <br>
 * </li>
 * </ul>
 * <p>
 * The default implementation behaviour is to
 * {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#CONTINUE continue} propagation when combined
 * phnomena are detected. This can be changed by using one of the provided constructors.
 * </p>
 * 
 * @concurrency not thread-safe or thread-hostile
 * 
 * @concurrency.comment As of now, existing Orekit EventDetector implementations are either not
 *                      thread-safe or thread-hostile, so this class also is. But this class could
 *                      probably become conditionally thread-safe; the main thread safety condition
 *                      would then be that the included EventDetector should be thread-safe.
 * 
 * @see EventDetector
 * @see fr.cnes.sirius.patrius.propagation.Propagator#addEventDetector(EventDetector)
 * 
 * @author Julie Anton
 * @author Tiziana Sabatini
 * 
 * @version $Id$
 * 
 * @since 1.1
 * 
 */
public class CombinedPhenomenaDetector extends AbstractDetector {

     /** Serializable UID. */
    private static final long serialVersionUID = 6674610443713490158L;

    /** First event detector. */
    private final EventDetector detector1;

    /**
     * True if increasing of the detector1 g function (from negative to positive value) represents
     * the "beginning" of the phenomenon associated to the event.
     */
    private final boolean increasingIsStart1;

    /** Second event detector. */
    private final EventDetector detector2;

    /**
     * True if increasing of the detector2 g function (from negative to positive value) represents
     * the "beginning" of the phenomenon associated to the event.
     */
    private final boolean increasingIsStart2;

    /** True if the boolean operator is AND, false if it is OR. */
    private final boolean together;

    /** Action performed */
    private final Action actionCombinedPhenomena;

    /**
     * Constructor for the detector of the combination of two phenomena.
     * 
     * @param d1 the first {@link EventDetector}
     * @param d1IncreasingIsStart true if increasing of the g function of the the first {@link EventDetector} represents
     *        the "beginning" of the associated phenomenon
     * @param d2 the second {@link EventDetector}
     * @param d2IncreasingIsStart true if increasing of the g function of the the second {@link EventDetector}
     *        represents the "beginning" of the associated phenomenon
     * @param togetherIn true if AND, false if OR
     */
    public CombinedPhenomenaDetector(final EventDetector d1, final boolean d1IncreasingIsStart,
        final EventDetector d2, final boolean d2IncreasingIsStart, final boolean togetherIn) {
        this(d1, d1IncreasingIsStart, d2, d2IncreasingIsStart, togetherIn, Action.CONTINUE);
    }

    /**
     * Constructor for the detector of the combination of two phenomena.
     * 
     * @param d1 the first {@link EventDetector}
     * @param d1IncreasingIsStart true if increasing of the g function of the the first {@link EventDetector} represents
     *        the "beginning" of the associated phenomenon
     * @param d2 the second {@link EventDetector}
     * @param d2IncreasingIsStart true if increasing of the g function of the the second {@link EventDetector}
     *        represents the "beginning" of the associated phenomenon
     * @param togetherIn true if AND, false if OR
     * @param action action performed at combined Phenomena detection
     */
    public CombinedPhenomenaDetector(final EventDetector d1, final boolean d1IncreasingIsStart,
        final EventDetector d2, final boolean d2IncreasingIsStart, final boolean togetherIn,
        final Action action) {
        super(MathLib.min(d1.getMaxCheckInterval(), d2.getMaxCheckInterval()), MathLib.min(
            d1.getThreshold(), d2.getThreshold()));

        this.detector1 = d1;
        this.increasingIsStart1 = d1IncreasingIsStart;
        this.detector2 = d2;
        this.increasingIsStart2 = d2IncreasingIsStart;
        this.together = togetherIn;

        // action
        this.actionCombinedPhenomena = action;
    }

    /**
     * Compute the value of the switching function for a combination (AND or OR) of two phenomena.<br>
     * After computing the switching function of each detector and, if necessary, changing its sign
     * to apply a general convention (g>0 if the phenomenon associated to an event is active), it
     * returns one between the two g functions, according to the boolean operator.
     * 
     * @param s the {@link SpacecraftState} that contains the current state information
     * 
     * @return value of the switching function
     * @exception PatriusException if some specific error occurs
     */
    @Override
    @SuppressWarnings("PMD.ShortMethodName")
    public final double g(final SpacecraftState s) throws PatriusException {
        final double g1 = this.increasingIsStart1 ? this.detector1.g(s) : -this.detector1.g(s);
        final double g2 = this.increasingIsStart2 ? this.detector2.g(s) : -this.detector2.g(s);
        return this.together ? MathLib.min(g1, g2) : MathLib.max(g1, g2);
    }

    /**
     * Handle an event and choose what to do next.
     * 
     * @param s the {@link SpacecraftState} that contains the current state information
     * @param increasing if true, the value of the switching function increases when times increases
     *        around event
     * @param forward if true, the integration variable (time) increases during integration.
     * @return the action performed when the expected combined philomena is reached
     * @exception PatriusException if some specific error occurs
     */
    @Override
    public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                final boolean forward) throws PatriusException {
        return this.actionCombinedPhenomena;
    }

    /** {@inheritDoc} */
    @Override
    public boolean shouldBeRemoved() {
        if (this.together) {
            // Detector should be removed if both detectors should be removed
            return this.detector1.shouldBeRemoved() && this.detector2.shouldBeRemoved();
        } else {
            // Detector should be removed if one of detectors should be removed
            return this.detector1.shouldBeRemoved() || this.detector2.shouldBeRemoved();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState s0, final AbsoluteDate t) throws PatriusException {
        // Initialize both event detectors at the start of a propagation.
        this.detector1.init(s0, t);
        this.detector2.init(s0, t);
    }

    /**
     * Returns first detector.
     * 
     * @return EventDetector 1
     */
    public EventDetector getDetector1() {
        return this.detector1;
    }

    /**
     * Returns second detector.
     * 
     * @return EventDetector 2
     */
    public EventDetector getDetector2() {
        return this.detector2;
    }

    /** {@inheritDoc} */
    @Override
    public EventDetector copy() {
        return new CombinedPhenomenaDetector(this.detector1.copy(), this.increasingIsStart1,
            this.detector2.copy(), this.increasingIsStart2, this.together, this.actionCombinedPhenomena);
    }
}
