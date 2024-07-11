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
 * @history 15/03/2013
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::DM:394:20/02/2015: Added possibility to define the action performed when event occurs
 * VERSION::DM:470:14/08/2015: Correction for NthOccurrenceDetector coupled with one-way detectors
 * VERSION::DM:454:24/11/2015:Add constructors, overload method shouldBeRemoved()
 * VERSION::FA:558:25/02/2016:Correction of algorithm for simultaneous events detection
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.events;

import fr.cnes.sirius.patrius.propagation.SpacecraftState;

/**
 * This event detector detects the nth occurrence of an underlying event detector.
 * <p>
 * However the {@link #eventOccurred(SpacecraftState, boolean, boolean)} method is triggered at every event of the
 * underlying detector. As a result, the behaviour of this detector is the following:
 * <ul>
 * <li>Before and after the nth occurrence, the {@link #eventOccurred(SpacecraftState, boolean, boolean)} returns
 * Action.CONTINUE.</li>
 * <li>At the nth occurrence, the {@link #eventOccurred(SpacecraftState, boolean, boolean)} returns the user-provided
 * action.</li>
 * </ul>
 * </p>
 * 
 * <p>
 * <b>Warning: </b> the {@link #eventOccurred(SpacecraftState, boolean, boolean)} method is triggered at every
 * occurrence of the underlying detector, not only at nth occurrence. Hence, overloading this detector should be
 * performed carefully: in the overloaded eventOccurred() method, the check {@link #getCurrentOccurrence()} ==
 * {@link #getOccurence()} should be performed first to ensure we are at nth occurrence before calling
 * super.eventOccurred().
 * </p>
 * 
 * @concurrency not thread-safe
 * @concurrency.comment use of internal mutable attributes
 * 
 * @author Rami Houdroge
 * 
 * @version $Id: NthOccurrenceDetector.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 1.3
 * 
 */
public class NthOccurrenceDetector extends IntervalOccurrenceDetector {

    /** Generated serial UID */
    private static final long serialVersionUID = -4109259106114272017L;

    /**
     * Constructor.
     * 
     * @param eventToDetect event to detect
     * @param occurrence occurrence of the event to detect
     * @param actionAtOccurrence action at event nth occurrence
     */
    public NthOccurrenceDetector(final EventDetector eventToDetect, final int occurrence,
        final Action actionAtOccurrence) {
        this(eventToDetect, occurrence, actionAtOccurrence, false);
    }

    /**
     * Constructor.
     * 
     * @param eventToDetect event to detect
     * @param occurrence occurrence of the event to detect
     * @param actionAtOccurrence action at event nth occurrence
     * @param remove true if detector should be removed after detection of nth occurrence
     */
    public NthOccurrenceDetector(final EventDetector eventToDetect, final int occurrence,
        final Action actionAtOccurrence, final boolean remove) {
        super(eventToDetect, occurrence, occurrence, 1, actionAtOccurrence, remove);
    }

    /**
     * Get the occurrence to detect.
     * 
     * @return occurrence to detect
     */
    public int getOccurence() {
        // Occurrence is first and last occurrence to detect
        return this.getFirstOccurrence();
    }
}
