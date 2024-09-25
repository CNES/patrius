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
 * @history created 15/03/2012
 *
 * HISTORY
 * VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:180:17/03/2014:removed a break instruction inside a while loop
 * VERSION::FA:---:11/04/2014:Quality assurance
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.events.postprocessing;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;

/**
 * @description <p>
 *              Filter that removes or keeps only the items of specific event types during a specific type of phenomena.
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
public final class EventsDuringPhenomenaFilter implements PostProcessing {

    /**
     * List of codes of the elements that have to be filtered.
     */
    private final List<String> codeE;

    /**
     * Code of the phenomenon during which one the events have to be filtered.
     */
    private final String codeP;

    /**
     * True if all of the events have to be removed.
     */
    private final boolean remove;

    /**
     * Constructor
     * 
     * @description build an instance of EventsDuringPhenomenaFilter from the codes of the event and the phenomenon
     * 
     * @param eventCode
     *        : code of the events that have to be removed or kept during a specified type of phenomenon
     * @param phenomenonCode
     *        : code of the phenomena during which ones a specified type of events have to be removed or kept
     * @param removeEvents
     *        : true if the specified events have to be removed
     *        during the specified phenomena, false if all of the
     *        other events have to be removed during the specified phenomena
     */
    public EventsDuringPhenomenaFilter(final String eventCode, final String phenomenonCode,
        final boolean removeEvents) {
        this.codeE = new ArrayList<>();
        this.codeE.add(eventCode);
        this.codeP = phenomenonCode;
        this.remove = removeEvents;
    }

    /**
     * Constructor
     * 
     * @description build an instance of EventsDuringPhenomenaFilter from a list of event codes and a phenomenon code.
     * 
     * @param eventCodes
     *        : list of codes of the events that have to be removed or kept during a specified type of phenomenon
     * @param phenomenonCode
     *        : code of the phenomena during which ones a specified type of events have to be removed or kept
     * @param removeEvents
     *        : true if the specified events have to be removed during the specified pehnomena, false if all of the
     *        other events have to be removed during the specified phenomena
     */
    public EventsDuringPhenomenaFilter(final List<String> eventCodes, final String phenomenonCode,
        final boolean removeEvents) {
        this.codeE = new ArrayList<>();
        this.codeE.addAll(eventCodes);
        this.codeP = phenomenonCode;
        this.remove = removeEvents;
    }

    /** {@inheritDoc} */
    @Override
    public void applyTo(final Timeline list) {
        // get the lists of events and phenomena
        final CodedEventsList eventsList = list.getCodedEvents();
        final PhenomenaList phenomenaList = list.getPhenomena();
        // initialize containers
        final Set<Phenomenon> phenomenaSet = phenomenaList.getPhenomena(this.codeP, null, null);
        final Set<CodedEvent> eventsSet = new TreeSet<>();
        for (final String currentCode : this.codeE) {
            // creates the events list of the selected type:
            eventsSet.addAll(eventsList.getEvents(currentCode, null, null));
        }
        final List<CodedEvent> eventsToRemove = new ArrayList<>(eventsSet);

        // loop on events to remove
        if (!eventsToRemove.isEmpty()) {
            final Iterator<CodedEvent> iterator = eventsToRemove.iterator();
            final CodedEvent currentEvent;

            // loop over the phenomena:
            final CodedEvent initialEvent = iterator.next();

            currentEvent = initialEvent;

            // apply to list
            this.applyPart2(list, phenomenaSet, iterator, currentEvent);
        }
    }

    /**
     * Second part of {@link #applyTo(Timeline)} method;
     * This function describes one specific processing that has to be performed on the timeline.
     * 
     * @param list
     *        the timeline that has to be processed
     * @param phenomenaSet
     *        the list of phenomena
     * @param iterator
     *        the iterator on coded events
     * @param event
     *        the current event
     * @return the current event at the end of the process
     */
    private CodedEvent applyPart2(final Timeline list, final Set<Phenomenon> phenomenaSet,
                                  final Iterator<CodedEvent> iterator, final CodedEvent event) {
        AbsoluteDate upperDate;
        AbsoluteDateInterval currentPhenInterval;
        CodedEvent currentEvent = event;
        for (final Phenomenon phen : phenomenaSet) {
            currentPhenInterval = phen.getTimespan();
            upperDate = currentPhenInterval.getUpperData();

            // interruption flag
            boolean interrupt = false;

            // loop over the events:
            while (currentEvent.getDate().compareTo(upperDate) == -1 && !interrupt) {
                if (currentPhenInterval.contains(currentEvent.getDate()) && this.remove) {
                    list.removeCodedEvent(currentEvent);
                } else if (!currentPhenInterval.contains(currentEvent.getDate()) && !this.remove) {
                    list.removeCodedEvent(currentEvent);
                }
                if (iterator.hasNext()) {
                    // there is a following event in the list:
                    currentEvent = iterator.next();
                } else {
                    // this was the last event: breaks the while cycle:
                    interrupt = true;
                }
            }
        }
        return currentEvent;
    }
}
