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
 * @history created 13/03/2012
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3163:10/05/2022:[PATRIUS] Enrichissement des reperes planetaires 
 * VERSION:4.9:DM:DM-3143:10/05/2022:[PATRIUS] Nouvelle interface OrbitEventDetector et nouvelles classes
 * VERSION:4.9:DM:DM-3130:10/05/2022:[PATRIUS] Robustifier le calcul des phenomenes des CodedEventsLogger, ...
 * VERSION:4.9:FA:FA-3170:10/05/2022:[PATRIUS] Incoherence de datation entre la methode getLocalRadius...
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:180:17/03/2014:removed a break instruction inside a for loop
 * VERSION::FA:345:30/10/2014:modified comments ratio
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events.postprocessing;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import fr.cnes.sirius.patrius.events.CodedEvent;
import fr.cnes.sirius.patrius.events.CodedEventsList;
import fr.cnes.sirius.patrius.events.CodedEventsLogger;
import fr.cnes.sirius.patrius.events.PhenomenaList;
import fr.cnes.sirius.patrius.events.Phenomenon;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

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
     * The list of codes of detected phenomena.
     */
    private final List<String> phenCodes;
       
    /**
     * Builds an instance of the timeline from a {@link CodedEventsLogger}, generating the list of detected events and
     * the list of corresponding phenomena.<br>
     * These events and phenomena are the output of a propagation with events detector; the coherence between events and
     * phenomena should be guaranteed by the detection process during propagation.
     * 
     * @param logger
     *        the {@link CodedEventsLogger} storing the events and phenomena created during propagation.
     * @param interval
     *        the {@link AbsoluteDateInterval} representing the validity interval of the events/phenomena lists.
     * @param spacecraftState
     *        a spacecraft state using if there is no event in logger, may be null. 
     * @throws PatriusException
     *         if a problem occurs when g is called when building phenomena.
     * 
     */
    public Timeline(final CodedEventsLogger logger, final AbsoluteDateInterval interval,
            final SpacecraftState spacecraftState) throws PatriusException {
        // builds the list of events:
        this.eventList = logger.getCodedEventsList();
        // builds the list of phenomena:
        this.phenList = new PhenomenaList();
        final SortedSet<String> phenCodesSet = new TreeSet<>();
        for (final PhenomenaList list : logger.buildPhenomenaListMap(interval, spacecraftState).values()) {
            for (final Phenomenon phenomenon : list.getList()) {
                this.phenList.add(phenomenon);
                phenCodesSet.add(phenomenon.getCode());
            }
        }
        this.phenCodes = new ArrayList<>(phenCodesSet);
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
        this.phenCodes = timeline.getPhenomenaCodes();
    }
    
    /**
     * Builds an instance of Timeline from a single time interval. The lists of
     * phenomena and events are empties.
     * 
     * @param interval
     *        the {@link AbsoluteDateInterval} representing the validity interval of the
     *        events/phenomena lists.
     */
    public Timeline(final AbsoluteDateInterval interval) {
        // builds an empty list of events:
        this.eventList = new CodedEventsList();
        // builds an empty list of phenomena:
        this.phenList = new PhenomenaList();
        // set the time interval
        this.valInterval = interval;
        // builds an empty list of detected phenomena codes
        this.phenCodes = new ArrayList<>();
    }

    /**
     * Builds an instance Timeline from another Timeline with a wider validity time
     * interval. The phenomena and events stored in the provided timeline are copied.
     * 
     * @param timeline
     *        the time line to copy.
     * @param newInterval
     *        the new validity time interval.
     * @throws PatriusException if the new validity interval does not include the one of the timeline.
     */
    public Timeline(final Timeline timeline, final AbsoluteDateInterval newInterval) throws PatriusException {
        // Check whether the time interval stored in the provided timeline
        // is included in the time new time interval
        if (!newInterval.includes(timeline.getIntervalOfValidity())) {
            throw new PatriusException(PatriusMessages.INTERVAL_MUST_INCLUDE);
        }
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
        this.valInterval = newInterval;
        this.phenCodes = timeline.getPhenomenaCodes();
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
     * @description Get the phenomena codes list.
     * 
     * @return List of phenomena codes
     */
    protected List<String> getPhenomenaCodes() {
    	return this.phenCodes;
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
                    this.removePhenomenaCode(phen.getCode(), true);
                    look = false;
                } else if (phen.getEndingEvent().equals(event)) {
                    this.eventList.remove(phen.getStartingEvent());
                    this.phenList.remove(phen);
                    this.removePhenomenaCode(phen.getCode(), true);
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
            // the phenomena code that may be removed from the codes list
            final String code = phenomenon.getCode();
            int occurence = 1;
            // the ending and starting events are removed if they are not shared with another phenomenon
            final CodedEvent startEvent = phenomenon.getStartingEvent();
            final CodedEvent endEvent = phenomenon.getEndingEvent();
            boolean removeStartEvent = true;
            boolean removeEndEvent = true;
            for (final Phenomenon phen : this.phenList.getList()) {
            	if(phen.getCode().equals(code)){
            	    occurence += 1;
            	}
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
            
            // remove the phenomena code if it is contained a single time
            if (occurence == 1) {
            	this.phenCodes.remove(code);
            }
        }
    }
    
    /**
     * Removes the provided phenomena code only if there is a single phenomena described by
     * the specified code. Otherwise, the code is still stored in the codes list of the
     * timeline. Since we check the list of phenomena whose the code is the specified one,
     * 
     * @param codeIn
     *        the phenomena code that might be removed.
     * @param hasBeenRemoved
     *        indicates whether the phenomenon has already been removed from the list.
     */
    private void removePhenomenaCode(final String codeIn, final boolean hasBeenRemoved) {
        // number of phenomenon that share the same code
        int occurence = this.getPhenomena().getPhenomena(codeIn, null, this.valInterval).size();
        if (hasBeenRemoved) {
            occurence += 1;
        }
        if (occurence == 1) {
            this.phenCodes.remove(codeIn);
        }
    }

    /**
     * Merges the provided timeline into this. The events and phenomena are added to this only if
     * the validity time intervals are equals.
     * 
     * @param otherTimeline
     *        the timeline to be merged.
     * @throws PatriusException if the validity time intervals are not strictly equal
     */
    public void merge(final Timeline otherTimeline) throws PatriusException {
        if (this.valInterval.equals(otherTimeline.getIntervalOfValidity())) {
            for (final CodedEvent eventToAdd : otherTimeline.getCodedEventsList()) {
                // add all the new events to the list:
                this.addCodedEvent(eventToAdd);
            }
            for (final Phenomenon phenomenonToAdd : otherTimeline.getPhenomenaList()) {
                // add all the new phenomena to the list:
                this.addPhenomenon(phenomenonToAdd);
            }
        } else {
            throw new PatriusException(PatriusMessages.INVALID_INTERVAL_OF_VALIDITY);
        }
    }

    /**
     * Buils a new {@link Timeline} by concatening this with the specified timeline. To concatenate
     * two timelines, their validity time intervals must not overlap (an exception is thrown
     * otherwise). The new timeline stores the phenomena/events of both timelines.
     * 
     * @param otherTimeline the timeline to concatenate.
     * 
     * @return the new timeline.
     * @throws PatriusException if time intervals overlap.
     */
    public Timeline join(final Timeline otherTimeline) throws PatriusException {

        final AbsoluteDateInterval otherInterval = otherTimeline.getIntervalOfValidity();
        if (this.valInterval.overlaps(otherInterval)) {
            throw new PatriusException(PatriusMessages.INTERVALS_OVERLAPPING_NOT_ALLOWED);
        }
        // add the phenomena of both timeline
        final List<Phenomenon> phenomena = new ArrayList<>();
        phenomena.addAll(this.getPhenomenaList());
        phenomena.addAll(otherTimeline.getPhenomenaList());

        // add the events of both timeline
        final List<CodedEvent> events = new ArrayList<>();
        events.addAll(this.getCodedEventsList());
        events.addAll(otherTimeline.getCodedEventsList());

        // the new validity time interval encloses both of them
        AbsoluteDateInterval newInterval = null;

        if (this.valInterval.getUpperData().compareTo(otherInterval.getLowerData()) < 0) {
            newInterval = new AbsoluteDateInterval(this.valInterval.getLowerEndpoint(),
                    this.valInterval.getLowerData(), otherInterval.getUpperData(),
                    otherInterval.getUpperEndpoint());
        } else {
            newInterval = new AbsoluteDateInterval(otherInterval.getLowerEndpoint(),
                    otherInterval.getLowerData(), this.valInterval.getUpperData(),
                    this.valInterval.getUpperEndpoint());
        }
        // builds a new timeline with empties lists of phenomena/events
        final Timeline newTimeline = new Timeline(newInterval);
        // add all the phenomena
        for (final Phenomenon phenomenon : phenomena) {
            newTimeline.addPhenomenon(phenomenon);
        }
        // add all the events
        for (final CodedEvent event : events) {
            newTimeline.addCodedEvent(event);
        }

        return newTimeline;
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
        // add the corresponding code if it is not in the list
        final SortedSet<String> phenCodesSet = new TreeSet<>();
        phenCodesSet.addAll(this.phenCodes);
        // check that the phenomenon code is not already present in the list (by using a set instead of looping on the
        // whole list)
        if (phenCodesSet.add(phenomenon.getCode())) {
            this.phenCodes.add(phenomenon.getCode());
        }
    }

    /**
     * @description Get the interval of validity.
     * 
     * @return the interval of validity
     */
    public AbsoluteDateInterval getIntervalOfValidity() {
        return this.valInterval;
    }

    /**
     * @description Get the list of codes corresponding to the detected phenomena.
     * 
     * @return the interval of validity
     */
    public List<String> getPhenomenaCodesList() {
        return this.phenCodes;
    }
}
