/**
 * 
 * Copyright 2011-2017 CNES
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
 * @history created 13/03/2012
 * 
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:180:17/03/2014:removed a break instruction inside a for loop
 * VERSION::FA:345:30/10/2014:modified comments ratio
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events.postprocessing;

import java.util.Iterator;
import java.util.List;

import fr.cnes.sirius.patrius.events.CodedEvent;
import fr.cnes.sirius.patrius.events.CodedEventsList;
import fr.cnes.sirius.patrius.events.CodedEventsLogger;
import fr.cnes.sirius.patrius.events.PhenomenaList;
import fr.cnes.sirius.patrius.events.Phenomenon;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * @description <p>
 *              This class represents the orbital events timeline as well as the associated phenomena when the event
 *              triggers a phenomenon.
 *              </p>
 * 
 * @concurrency not thread-safe
 * 
 * @concurrency.comment the timeline is not thread safe due to the methods removeCodedEvent(CodedEvent) and
 *                      removePhenomenon(Phenomenon)
 * 
 * @author Julie Anton
 * 
 * @version $Id$
 * 
 * @since 1.1
 * 
 */
public class Timeline {

    /**
     * The list of events.
     */
    private final CodedEventsList eventList;

    /**
     * The list of phenomena corresponding to the events list.
     */
    private final PhenomenaList phenList;

    /**
     * The validity interval of the events/phenomena lists.
     */
    private final AbsoluteDateInterval valInterval;

    /**
     * Builds an instance of the timeline from a {@link CodedEventsLogger}, generating the list of detected events and
     * the list of corresponding phenomena.<br>
     * These events and phenomena are the output of a propagation with events detector; the coherence between events and
     * phenomena should be guaranteed by the detection process during propagation.
     * 
     * @precondition a propagation with events detection should have been performed.
     * 
     * @param logger
     *        the {@link CodedEventsLogger} storing the events and phenomena created during propagation.
     * @param interval
     *        the {@link AbsoluteDateInterval} representing the validity interval of the events/phenomena lists.
     * @throws PatriusException
     *         if a problem occurs when g is called when building phenomena.
     * 
     */
    public Timeline(final CodedEventsLogger logger, final AbsoluteDateInterval interval) throws PatriusException {
        // builds the list of events:
        this.eventList = logger.getCodedEventsList();
        // builds the list of phenomena:
        this.phenList = new PhenomenaList();
        for (final PhenomenaList list : logger.buildPhenomenaListMap(interval, null).values()) {
            for (final Phenomenon phenomenon : list.getList()) {
                this.phenList.add(phenomenon);
            }
        }
        this.valInterval = interval;
    }

    /**
     * Builds an instance of the Timeline from another Timeline.
     * 
     * @param timeline
     *        the timeline to copy
     */
    public Timeline(final Timeline timeline) {
        // builds the list of events:
        this.eventList = new CodedEventsList();
        for (final CodedEvent event : timeline.getCodedEventsList()) {
            this.eventList.add(event);
        }
        // builds the list of phenomena:
        this.phenList = new PhenomenaList();
        for (final Phenomenon phen : timeline.getPhenomenaList()) {
            this.phenList.add(phen);
        }
        // gets the time interval:
        this.valInterval = timeline.getIntervalOfValidity();
    }

    /**
     * @description Get the phenomena list.
     * 
     * @return PhenomenaList
     */
    protected PhenomenaList getPhenomena() {
        return this.phenList;
    }

    /**
     * @description Get the coded events list.
     * 
     * @return CodedEventsList
     */
    protected CodedEventsList getCodedEvents() {
        return this.eventList;
    }

    /**
     * @description Get a copy of the phenomena list.
     * 
     * @return list of phenomena
     */
    public List<Phenomenon> getPhenomenaList() {
        return this.phenList.getList();
    }

    /**
     * @description Get a copy of the coded events list
     * 
     * @return list of coded events
     */
    public List<CodedEvent> getCodedEventsList() {
        return this.eventList.getList();
    }

    /**
     * @description Remove one coded event from the coded events list (as well as the phenomena list when the event
     *              triggers a phenomenon).
     * 
     * @param event
     *        : coded event that has to be removed from the list
     */
    public void removeCodedEvent(final CodedEvent event) {
        if (this.eventList.remove(event)) {

            // iterator
            final Iterator<Phenomenon> iterator = this.phenList.getList().iterator();
            // control variable
            boolean look = true;
            // container
            Phenomenon phen;

            while (iterator.hasNext() && look) {
                // next in list
                phen = iterator.next();

                if (phen.getStartingEvent().equals(event)) {
                    this.eventList.remove(phen.getEndingEvent());
                    this.phenList.remove(phen);
                    look = false;
                } else if (phen.getEndingEvent().equals(event)) {
                    this.eventList.remove(phen.getStartingEvent());
                    this.phenList.remove(phen);
                    look = false;
                }
            }
        }
    }

    /**
     * @description Remove one coded event from the coded events list (without removing the associated phenomenon).
     * 
     * @param event
     *        : coded event that has to be removed from the list
     */
    public void removeOnlyCodedEvent(final CodedEvent event) {
        this.eventList.remove(event);

    }

    /**
     * @description Remove one phenomenon from the phenomena list as well as the associated coded events from the coded
     *              events list.
     * 
     * @param phenomenon
     *        : phenomenon that has to be removed from the list
     */
    public void removePhenomenon(final Phenomenon phenomenon) {
        if (this.phenList.remove(phenomenon)) {
            // the ending and starting events are removed if they are not shared with another phenomenon
            final CodedEvent startEvent = phenomenon.getStartingEvent();
            final CodedEvent endEvent = phenomenon.getEndingEvent();
            boolean removeStartEvent = true;
            boolean removeEndEvent = true;
            for (final Phenomenon phen : this.phenList.getList()) {
                if (phen.getStartingEvent().equals(startEvent) ||
                    phen.getEndingEvent().equals(startEvent)) {
                    removeStartEvent = false;
                }
                if (phen.getStartingEvent().equals(endEvent) ||
                    phen.getEndingEvent().equals(endEvent)) {
                    removeEndEvent = false;
                }
            }
            // remove starting event of the phenomenon from the list of events
            if (removeStartEvent) {
                this.eventList.remove(phenomenon.getStartingEvent());
            }
            // remove ending event of the phenomenon from the list of events
            if (removeEndEvent) {
                this.eventList.remove(phenomenon.getEndingEvent());
            }
        }
    }

    /**
     * @description Add one event to the events list
     * 
     * @param event
     *        : event that has to be added to the list
     */
    public void addCodedEvent(final CodedEvent event) {
        this.eventList.add(event);
    }

    /**
     * @description Add one phenomenon to the phenomena list (without adding the ending and starting events to the event
     *              list)
     * 
     * @param phenomenon
     *        : phenomenon that has to be added to the list
     */
    public void addPhenomenon(final Phenomenon phenomenon) {
        this.phenList.add(phenomenon);
    }

    /**
     * @description Get the interval of validity.
     * 
     * @return the interval of validity
     */
    public AbsoluteDateInterval getIntervalOfValidity() {
        return this.valInterval;
    }
}
