/**
 * 
 * Copyright 2011-2017 CNES
 *
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
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
 * @history created 15/03/2012
 */
package fr.cnes.sirius.patrius.events.postprocessing;

import java.util.Set;

import fr.cnes.sirius.patrius.events.CodedEvent;
import fr.cnes.sirius.patrius.events.CodedEventsList;

/**
 * @description <p>
 *              This class is a post processing criterion that delays a specified kind of events.
 *              </p>
 * 
 * @concurrency immutable
 * 
 * @author Julie Anton
 * 
 * @version $Id$
 * 
 * @since 1.1
 * 
 */
public final class DelayCriterion implements PostProcessing {

    /** Code of the element that has to be filtered. */
    private final String code;

    /** Delay. */
    private final double d;

    /** Delayed element code. */
    private final String delayCode;

    /** Comment of the new event added to the list */
    private final String commentIn;

    /**
     * Constructor
     * 
     * @description build an instance of DelayCriterion from a code
     * 
     * @param elementCode
     *        : code of the elements that has to be delayed
     * @param delay
     *        : delay (in secondes)
     */
    public DelayCriterion(final String elementCode, final double delay) {
        this(elementCode, delay, "Delayed " + elementCode, "");
    }

    /**
     * Constructor
     * 
     * @description build an instance of DelayCriterion from an event code and a code for the created delayed elements
     * 
     * @param elementCode
     *        : code of the elements that has to be delayed
     * @param delay
     *        : delay (in secondes)
     * @param delayedElementCode
     *        : delayed element code
     * @param newComment
     *        comment of the new phenomena added to the list
     */
    public DelayCriterion(final String elementCode, final double delay,
        final String delayedElementCode, final String newComment) {
        this.code = elementCode;
        this.d = delay;
        this.delayCode = delayedElementCode;
        this.commentIn = newComment;
    }

    /** {@inheritDoc} */
    @Override
    public void applyTo(final Timeline list) {
        final CodedEventsList eventsList = list.getCodedEvents();
        final Set<CodedEvent> eventsSet = eventsList.getEvents(this.code, null, null);

        CodedEvent newEvent;

        for (final CodedEvent event : eventsSet) {
            newEvent =
                new CodedEvent(this.delayCode, this.commentIn, event.getDate().shiftedBy(this.d),
                    event.isStartingEvent());
            list.addCodedEvent(newEvent);
        }
    }
}
