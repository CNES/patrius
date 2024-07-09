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
 * @history created 15/03/2012
 * 
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:---:11/04/2014:Quality assurance
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events.postprocessing;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import fr.cnes.sirius.patrius.events.CodedEvent;
import fr.cnes.sirius.patrius.events.Phenomenon;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;

/**
 * @description <p>
 *              Filter that removes or keeps only all of the events and the phenomena that are inside a given time
 *              interval.
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
@SuppressWarnings("PMD.NullAssignment")
public final class TimeFilter implements PostProcessing {

    /**
     * List of codes of the elements that have to be filtered.
     */
    private final List<String> code;

    /**
     * Time interval.
     */
    private final AbsoluteDateInterval interval;

    /**
     * True if all of the elements contained in the specified time interval have to be removed, false if only those
     * elements have to be kept.
     */
    private final boolean remove;

    /**
     * True if the filter is applied to all of the elements without selecting the code.
     */
    private final boolean allElements;

    /**
     * Build an instance of TimeFilter from a time interval.
     * All the elements of the timeline will be removed/kept, despite of their code
     * 
     * @param timeInterval
     *        : time interval
     * @param removeAll
     *        : true if all of the elements contained in the specified time interval have to be removed, false if
     *        only those elements have to be kept
     */
    public TimeFilter(final AbsoluteDateInterval timeInterval, final boolean removeAll) {
        this.allElements = true;
        this.code = null;
        this.interval = timeInterval;
        this.remove = removeAll;
    }

    /**
     * Build an instance of TimeFilter from a time interval.
     * Only one code is used to identify the elements that have to be removed/kept.
     * 
     * @param elementCode
     *        : code of the elements that have to be removed / kept
     * @param timeInterval
     *        : time interval
     * @param removeAll
     *        : true if all of the elements contained in the specified time interval have to be removed, false if
     *        only those elements have to be kept
     */
    public TimeFilter(final String elementCode, final AbsoluteDateInterval timeInterval, final boolean removeAll) {
        this.allElements = false;
        this.code = new ArrayList<String>();
        this.code.add(elementCode);
        this.interval = timeInterval;
        this.remove = removeAll;
    }

    /**
     * Build an instance of TimeFilter from a time interval.
     * A list of codes is used to identify the elements that have to be removed/kept.
     * 
     * @param listCode
     *        : list of codes of the elements that have to be removed / kept
     * @param timeInterval
     *        : time interval
     * @param removeAll
     *        : true if all of the elements contained in the specified time interval have to be removed, false if
     *        only those elements have to be kept
     */
    public TimeFilter(final List<String> listCode, final AbsoluteDateInterval timeInterval, final boolean removeAll) {
        this.allElements = false;
        this.code = new ArrayList<String>();
        this.code.addAll(listCode);
        this.interval = timeInterval;
        this.remove = removeAll;
    }

    /** {@inheritDoc} */
    @Override
    public void applyTo(final Timeline list) {
        if (list.getIntervalOfValidity().overlaps(this.interval)) {
            final Set<Phenomenon> phenomenaList = new TreeSet<Phenomenon>();
            final Set<CodedEvent> eventsList = new TreeSet<CodedEvent>();
            if (this.allElements) {
                // we take all the elements:
                eventsList.addAll(list.getCodedEventsList());
                phenomenaList.addAll(list.getPhenomenaList());
            } else {
                // we select the elements using the code:
                for (final String currentCode : this.code) {
                    // store all the specified phenomena in a list:
                    phenomenaList.addAll(list.getPhenomena().getPhenomena(currentCode, null, null));
                    // store all the specified events in a list:
                    eventsList.addAll(list.getCodedEvents().getEvents(currentCode, null, null));
                    for (final Phenomenon phen : phenomenaList) {
                        eventsList.add(phen.getStartingEvent());
                        eventsList.add(phen.getEndingEvent());
                    }
                }
            }

            if (this.remove) {
                // remove all the specified elements in the time interval:
                this.removePhenomenaAndEvents(list, eventsList, phenomenaList);
            } else {
                // keep only the specified elements in the time interval(the events and phenomena
                // have already been selected):
                this.keepPhenomenaAndEvents(list, eventsList, phenomenaList);
            }
        }
    }

    /**
     * Preserves only the events and phenomena of a predetermined type inside the given
     * interval of this filter.
     * 
     * @param list
     *        : timeline that has to be filtered
     * @param eventsList
     *        : selected events
     * @param phenomenaList
     *        : selected phenomena
     */
    private void keepPhenomenaAndEvents(final Timeline list,
                                        final Set<CodedEvent> eventsList, final Set<Phenomenon> phenomenaList) {
        // loop on all the phenomena of the timeline:
        for (final Phenomenon phenomenon : list.getPhenomenaList()) {

            if (phenomenaList.contains(phenomenon)) {
                // manage the phenomena with the selected code:
                this.resizeSelectedPhenomenon(list, phenomenon);
            } else {
                // manage the phenomena without the selected code:
                this.resizeNonSelectedPhenomenon(list, phenomenon);
            }
        }
        // applies the filter on the events list:
        AbsoluteDate currentDate;
        for (final CodedEvent event : list.getCodedEventsList()) {
            currentDate = event.getDate();
            if (!this.interval.contains(currentDate) && eventsList.contains(event)) {
                // removes all the selected events that are not in the time interval:
                list.removeOnlyCodedEvent(event);
            }
            if (this.interval.contains(currentDate) && !eventsList.contains(event)) {
                // removes all the other events that are in the time interval:
                list.removeOnlyCodedEvent(event);
            }
        }
    }

    /**
     * Removes all of the events and phenomena of a predetermined type inside the given
     * interval of this filter.
     * 
     * @param list
     *        : timeline that has to be filtered
     * @param eventsList
     *        : selected events
     * @param phenomenaList
     *        : selected phenomena
     */
    private void removePhenomenaAndEvents(final Timeline list,
                                          final Set<CodedEvent> eventsList, final Set<Phenomenon> phenomenaList) {
        // the filter is applied on the phenomena list
        AbsoluteDateInterval currentInterval;
        CodedEvent undefinedEvent;
        CodedEvent eventPhen;

        for (final Phenomenon phenomenon : phenomenaList) {
            currentInterval = phenomenon.getTimespan();
            if (this.interval.includes(currentInterval)) {
                // the phenomenon time interval is included in the filter interval: removes the phenomenon
                list.removePhenomenon(phenomenon);
            } else if (currentInterval.includes(this.interval)) {
                // the filter time interval is included in the phenomenon interval: creates two phenomena
                // phenomenon 1:
                undefinedEvent = CodedEvent.buildUndefinedEvent(this.interval.getLowerData(), false);
                eventPhen = phenomenon.getStartingEvent();
                list.addPhenomenon(new Phenomenon(eventPhen, true, undefinedEvent, false, phenomenon.getCode(),
                    phenomenon.getComment()));
                // phenomenon 2:
                undefinedEvent = CodedEvent.buildUndefinedEvent(this.interval.getUpperData(), true);
                eventPhen = phenomenon.getEndingEvent();
                list.addPhenomenon(new Phenomenon(undefinedEvent, false, eventPhen, true, phenomenon.getCode(),
                    phenomenon.getComment()));
                list.removePhenomenon(phenomenon);
            } else {
                this.removePhenomenaAndEvents2(list, currentInterval, phenomenon);
            }
        }
        // the filter is applied on the events list
        AbsoluteDate currentDate;
        for (final CodedEvent event : eventsList) {
            currentDate = event.getDate();
            if (this.interval.contains(currentDate)) {
                list.removeOnlyCodedEvent(event);
            }
        }
    }

    /**
     * Removes all of the events and phenomena of a predetermined type inside the given
     * interval of this filter. Continued from previous method for cyclomatic complexity.
     * 
     * @param list
     *        : timeline that has to be filtered
     * @param currentInterval
     *        : current interval
     * @param phenomenon
     *        : selected phenomenon
     */
    private void removePhenomenaAndEvents2(final Timeline list,
                                           final AbsoluteDateInterval currentInterval, final Phenomenon phenomenon) {
        final CodedEvent undefinedEvent;
        final CodedEvent eventPhen;
        if (this.interval.overlaps(currentInterval)) {
            // if the phenomenon overlaps the given interval, a new phenomenon is created with an undefined
            // starting or ending event
            if (this.interval.compareLowerEndTo(currentInterval) > 0) {
                // case : the starting event is before the interval lower date
                undefinedEvent = CodedEvent.buildUndefinedEvent(this.interval.getLowerData(), false);
                eventPhen = phenomenon.getStartingEvent();
                list.removePhenomenon(phenomenon);
                list.addPhenomenon(new Phenomenon(eventPhen, true, undefinedEvent, false, phenomenon.getCode(),
                    phenomenon.getComment()));
            } else {
                // case : the starting event is after the interval lower date
                undefinedEvent = CodedEvent.buildUndefinedEvent(this.interval.getUpperData(), true);
                eventPhen = phenomenon.getEndingEvent();
                list.removePhenomenon(phenomenon);
                list.addPhenomenon(new Phenomenon(undefinedEvent, false, eventPhen, true, phenomenon.getCode(),
                    phenomenon.getComment()));
            }
            list.addCodedEvent(eventPhen);
        }
    }

    /**
     * Resizes (or removes) the selected phenomena inside the given interval of this filter
     * 
     * @param list
     *        : timeline that has to be filtered
     * @param phenomenon
     *        : phenomenon to be resized
     */
    private void resizeSelectedPhenomenon(final Timeline list, final Phenomenon phenomenon) {
        final AbsoluteDateInterval currentInterval = phenomenon.getTimespan();
        final CodedEvent undefinedEvent;
        final CodedEvent eventPhen;
        if (currentInterval.includes(this.interval)) {
            // the interval of the phenomenon include the time interval of the filter:
            undefinedEvent = CodedEvent.buildUndefinedEvent(this.interval.getLowerData(), true);
            eventPhen = CodedEvent.buildUndefinedEvent(this.interval.getUpperData(), false);
            list.removePhenomenon(phenomenon);
            list.addPhenomenon(new Phenomenon(undefinedEvent, false, eventPhen, false, phenomenon.getCode(),
                phenomenon.getComment()));
        } else if (this.interval.overlaps(currentInterval) && !this.interval.includes(currentInterval)) {
            // the phenomenon overlaps the interval, but is not included:
            if (this.interval.compareLowerEndTo(currentInterval) > 0) {
                // case : the starting event is before the interval lower date
                undefinedEvent = CodedEvent.buildUndefinedEvent(this.interval.getLowerData(), true);
                eventPhen = phenomenon.getEndingEvent();
                list.removePhenomenon(phenomenon);
                list.addPhenomenon(new Phenomenon(undefinedEvent, false, eventPhen, true, phenomenon.getCode(),
                    phenomenon.getComment()));
            } else {
                // case : the starting event is after the interval lower date
                undefinedEvent = CodedEvent.buildUndefinedEvent(this.interval.getUpperData(), false);
                eventPhen = phenomenon.getStartingEvent();
                list.removePhenomenon(phenomenon);
                list.addPhenomenon(new Phenomenon(eventPhen, true, undefinedEvent, false, phenomenon.getCode(),
                    phenomenon.getComment()));
            }
            list.addCodedEvent(eventPhen);
            list.addCodedEvent(undefinedEvent);
        } else if (!this.interval.includes(currentInterval)) {
            // if the phenomenon does not overlap the interval, it is removed:
            list.removePhenomenon(phenomenon);
        }
    }

    /**
     * Resizes (or removes) the non-selected phenomena outside the given interval of this filter
     * 
     * @param list
     *        : timeline that has to be filtered
     * @param phenomenon
     *        : phenomenon to be resized
     */
    private void resizeNonSelectedPhenomenon(final Timeline list, final Phenomenon phenomenon) {
        final AbsoluteDateInterval currentInterval = phenomenon.getTimespan();
        if (this.interval.overlaps(currentInterval) && !this.interval.includes(currentInterval)) {
            // the phenomenon overlaps the interval, but is not included:
            final CodedEvent undefinedEvent;
            final CodedEvent eventPhen;
            if (this.interval.compareLowerEndTo(currentInterval) > 0) {
                // case : the starting even is before the interval lower date
                undefinedEvent = CodedEvent.buildUndefinedEvent(this.interval.getLowerData(), true);
                eventPhen = phenomenon.getStartingEvent();
                list.removePhenomenon(phenomenon);
                list.addPhenomenon(new Phenomenon(eventPhen, true, undefinedEvent, false, phenomenon.getCode(),
                    phenomenon.getComment()));
            } else {
                // case : the starting event is after the interval lower date
                undefinedEvent = CodedEvent.buildUndefinedEvent(this.interval.getUpperData(), false);
                eventPhen = phenomenon.getEndingEvent();
                list.removePhenomenon(phenomenon);
                list.addPhenomenon(new Phenomenon(undefinedEvent, false, eventPhen, true, phenomenon.getCode(),
                    phenomenon.getComment()));
            }
            list.addCodedEvent(eventPhen);
            list.addCodedEvent(undefinedEvent);
        } else if (this.interval.includes(currentInterval)) {
            // if the phenomenon is included in the interval, it is removed
            list.removePhenomenon(phenomenon);
        }
    }
}
