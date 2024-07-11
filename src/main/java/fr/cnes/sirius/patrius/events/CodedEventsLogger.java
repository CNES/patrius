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
 * created 27/01/2012
  * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 *                             (added forward parameter to eventOccurred signature)
  * VERSION:DM:454:24/11/2015: Overload method shouldBeRemoved()
  * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.9:DM:DM-3172:10/05/2022:[PATRIUS] Ajout d'un throws PatriusException a la methode init de l'interface EventDetector
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.propagation.events.EventsLogger;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.time.TimeStamped;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * This class logs coded events during propagation. It is based on the {@link EventsLogger} class in Orekit.
 * </p>
 * <p>
 * This class works by wrapping user-provided {@link CodingEventDetector} before they are registered to the propagator.
 * The wrapper monitors the calls to {@link CodingEventDetector#eventOccurred(SpacecraftState, boolean, boolean)
 * eventOccurred} and store the corresponding events as {@link LoggedCodedEvent} instances. After propagation is
 * complete, the user can retrieve all the events that have occurred at once by calling methods
 * {@link #getCodedEventsList()} or {@link #getLoggedCodedEventSet()}.
 * </p>
 * </p>
 * 
 * @concurrency not thread-safe
 * 
 * @concurrency.comment no thread-sharing use case was identified for this class, so thread safety is not required.
 * 
 * @see EventsLogger
 * @see CodingEventDetector
 * 
 * @author Pierre Cardoso, Tiziana Sabatini
 * 
 * @version $Id$
 * 
 * @since 1.1
 * 
 */
public final class CodedEventsLogger {

    /** Set of all monitored events. */
    private final Set<CodingEventDetector> monitoredEvents;

    /** Sorted set of LoggedCodedEvent instances. */
    private final SortedSet<LoggedCodedEvent> loggedEventsSet;

    /**
     * Default constructor.
     */
    public CodedEventsLogger() {
        this.loggedEventsSet = new TreeSet<CodedEventsLogger.LoggedCodedEvent>();
        this.monitoredEvents = new HashSet<CodingEventDetector>();
    }

    /**
     * Takes a {@link CodingEventDetector} instance and returns an {@link EventDetector} instance that will trigger this
     * {@link CodedEventsLogger} every time {@link EventDetector#eventOccurred(SpacecraftState, boolean, boolean)
     * eventOccurred} is called. The returned {@link EventDetector} is meant to be provided to a propagator.
     * 
     * @param detector
     *        the wrapped {@link CodingEventDetector}
     * @return a wrapper for the parameter, as an {@link EventDetector}.
     */
    public EventDetector monitorDetector(final CodingEventDetector detector) {
        // Add the event to the monitored events list
        this.monitoredEvents.add(detector);
        // Returns a CodingWrapper
        return new CodingWrapper(detector);
    }

    /**
     * Creates a {@link CodedEvent} from the {@link CodingEventDetector} associated to the new event and adds it to the
     * list of coded events. It can be a standard coded event, a delayed coded event or a nth occurrence coded event
     * (with a delay if necessary).
     * 
     * @param codingDetector
     *        the {@link CodingEventDetector} that creates the event.
     * @param s
     *        the {@link SpacecraftState} that caused the event.
     * @param increasing
     *        true if g function increases around event date.
     */
    private void processEvent(final CodingEventDetector codingDetector, final SpacecraftState s,
                              final boolean increasing) {
        // builds the modified coded event when necessary:
        if ("N_OCCURRENCE".equals(codingDetector.getEventType())) {
            // we look for an event with a specific occurrence number:
            final CodedEvent occurrenceEvent = codingDetector.buildOccurrenceCodedEvent(s, increasing);
            if (occurrenceEvent != null) {
                final LoggedCodedEvent occurrenceLoggedEvent = new LoggedCodedEvent(codingDetector, occurrenceEvent, s,
                    increasing);
                // adds it to the appropriate list:
                this.loggedEventsSet.add(occurrenceLoggedEvent);
            }
        } else if ("DELAY".equals(codingDetector.getEventType())) {
            // we want to add a delay to the detected events:
            final CodedEvent delayedEvent = codingDetector.buildDelayedCodedEvent(s, increasing);
            final LoggedCodedEvent delayedLoggedEvent = new LoggedCodedEvent(codingDetector, delayedEvent, s,
                increasing);
            // adds it to the appropriate list:
            this.loggedEventsSet.add(delayedLoggedEvent);
        } else {
            // builds the standard LoggedCodedEvent instance:
            final CodedEvent event = codingDetector.buildCodedEvent(s, increasing);
            final LoggedCodedEvent loggedEvent = new LoggedCodedEvent(codingDetector, event, s, increasing);
            // adds it to the appropriate list:
            this.loggedEventsSet.add(loggedEvent);
        }
    }

    /**
     * Gets the {@link CodedEventsList}. This method can be called after propagation to get the list of detected events.
     * 
     * @return the {@link CodedEventsList}.
     */
    public CodedEventsList getCodedEventsList() {
        final CodedEventsList list = new CodedEventsList();
        final Iterator<LoggedCodedEvent> i = this.getLoggedCodedEventSet().iterator();
        while (i.hasNext()) {
            final LoggedCodedEvent loggedEvent = i.next();
            final CodedEvent event = loggedEvent.getCodedEvent();
            list.add(event);
        }
        return list;
    }

    /**
     * Returns an unmodifiable view on the set of {@link LoggedCodedEvent}, sorted by date.
     * 
     * @return the set.
     */
    public SortedSet<LoggedCodedEvent> getLoggedCodedEventSet() {
        return Collections.unmodifiableSortedSet(this.loggedEventsSet);
    }

    /**
     * Builds a map of {@link CodedEventsList}, one list per {@link CodingEventDetector} instance.
     * <p>
     * The map may be empty if no {@link CodingEventDetector} was added to the logger.
     * </p>
     * 
     * @return the map. May be empty, if no {@link CodingEventDetector} was added.
     */
    public Map<CodingEventDetector, CodedEventsList> buildCodedEventListMap() {
        // Build the Map, with a CodedEventsList per CodingEventDetector
        final Map<CodingEventDetector, CodedEventsList> map =
                new ConcurrentHashMap<CodingEventDetector, CodedEventsList>();
        for (final CodingEventDetector event : this.monitoredEvents) {

            final CodedEventsList list = new CodedEventsList();
            map.put(event, list);
        }
        // Fill each list (lists may be empty if no event was detected)
        for (final LoggedCodedEvent loggedEvent : this.loggedEventsSet) {
            final CodingEventDetector key = loggedEvent.getDetector();
            // Should never fail
            final CodedEventsList currentList = map.get(key);
            currentList.add(loggedEvent.getCodedEvent());
        }
        return map;
    }

    /**
     * Builds a map of {@link PhenomenaList}, one list per {@link CodingEventDetector} instance.
     * <p>
     * The map may be empty if no {@link CodingEventDetector} was added to the logger.
     * </p>
     * 
     * @param definitionInterval
     *        an {@link AbsoluteDateInterval}. May be null; if not, is used to position uncertain boundaries on a
     *        Phenomenon.
     * @param duringState
     *        a {@link SpacecraftState}. May be null; if not, is used to compute a Phenomenon when no event has
     *        occurred.
     * 
     * @return the map. May be empty, if no {@link CodingEventDetector} handling phenomena was added.
     * 
     * @throws PatriusException
     *         if a problem occurs when g is called with the duringState parameter.
     */
    public Map<CodingEventDetector, PhenomenaList>
            buildPhenomenaListMap(final AbsoluteDateInterval definitionInterval,
                                  final SpacecraftState duringState) throws PatriusException {
        // definition boundaries
        final AbsoluteDate valBegin;
        final AbsoluteDate valEnd;
        if (definitionInterval == null) {
            // No definition interval was given.
            // We need default values that will always work.
            valBegin = AbsoluteDate.PAST_INFINITY;
            valEnd = AbsoluteDate.FUTURE_INFINITY;
        } else {
            valBegin = definitionInterval.getLowerData();
            valEnd = definitionInterval.getUpperData();
        }

        // Only event detectors that return a valid Phenomenon code can be
        // processed.
        final Set<CodingEventDetector> validDetectors = new HashSet<CodingEventDetector>();
        for (final CodingEventDetector detector : this.monitoredEvents) {
            if (detector.getPhenomenonCode() != null) {
                validDetectors.add(detector);
            }
        }

        // Build the Map, with a PhenomenaList per CodingEventDetector
        final Map<CodingEventDetector, PhenomenaList> phenomMap =
                new ConcurrentHashMap<CodingEventDetector, PhenomenaList>();
        for (final CodingEventDetector detector : validDetectors) {
            final PhenomenaList list = new PhenomenaList();
            phenomMap.put(detector, list);
        }
        // Map of events needed
        final Map<CodingEventDetector, CodedEventsList> eventsMap = this.buildCodedEventListMap();

        // Create the phenomena
        for (final CodingEventDetector event : validDetectors) {
            // Get the current phenomena list
            // final PhenomenaList phenomList = phenomMap.get(event);
            // Get the associated events
            final CodedEventsList eventsList = eventsMap.get(event);
            // .. as a proper list of CodedEvents
            final List<CodedEvent> ceList = eventsList.getList();
            final PhenomenaList phenomList =
                this.buildPhenomenaListFromEvents(duringState, valBegin, valEnd, event, ceList);
            phenomMap.put(event, phenomList);
        }
        return phenomMap;
    }

    /**
     * Build the {@link PhenomenaList} corresponding to a list of {@link CodedEvent}.
     * 
     * @param state
     *        a {@link SpacecraftState}: may be null; if not, is used to compute a Phenomenon when no event has
     *        occurred.
     * @param dateStart
     *        the definition interval start boundary
     * @param dateEnd
     *        the definition interval end boundary
     * @param event
     *        the {@link CodingEventDetector} for the current phenomenon
     * @param ceList
     *        the {@link CodedEventsList} for the current event detector
     * @throws PatriusException
     *         g function exceptions
     * 
     * @return the {@link PhenomenaList}
     * 
     */
    private PhenomenaList buildPhenomenaListFromEvents(final SpacecraftState state, final AbsoluteDate dateStart,
                                                       final AbsoluteDate dateEnd, final CodingEventDetector event,
                                                       final List<CodedEvent> ceList) throws PatriusException {

        final PhenomenaList phenomList = new PhenomenaList();
        // Process the list and build the phenomena
        CodedEvent formerEvent = null;
        final List<CodedEvent> list = this.selectStandardEvent(ceList);
        final int listSize = list.size();
        for (int i = 0; i < listSize; i++) {
            final CodedEvent curEvent = list.get(i);
            final Phenomenon phenomenon =
                this.buildPhenomenonFromEvents(dateStart, dateEnd, event, formerEvent, curEvent,
                    listSize, i);
            if (phenomenon != null) {
                phenomList.add(phenomenon);
            }
            // Update the former event
            formerEvent = curEvent;
        }

        // We try to create a Phenomenon if
        // no event was created for the CodingEventDetector.
        if ((state != null) && (listSize == 0)) {
            // No phenomenon can be found through events.
            // Is the "state" associated to the event "active"?
            // G computation.
            final double gValue = event.g(state);
            if ((event.positiveSignMeansActive() && gValue > 0.)
                || (!event.positiveSignMeansActive() && gValue < 0.)) {
                // The "state" is "always" "active"
                // We create a Phenomenon on the full validity interval
                final Phenomenon ph = new Phenomenon(CodedEvent.buildUndefinedEvent(dateStart, true), true,
                    CodedEvent.buildUndefinedEvent(dateEnd, true), true, event.getPhenomenonCode(),
                    "Caution : both beginning and ending undefined.");
                phenomList.add(ph);
            }
        }
        return phenomList;
    }

    /**
     * Apply a filter on a list of {@link CodedEvent} to retrieve only the standard CodedEvent (the delayed and nth
     * occurrence events are not included in the output list).
     * 
     * @param list
     *        the list of {@link CodedEvent} to process.
     * 
     * @return the filtered list of {@link CodedEvent}
     * 
     */
    private List<CodedEvent> selectStandardEvent(final List<CodedEvent> list) {
        final List<CodedEvent> newList = new ArrayList<CodedEvent>();
        for (int i = 0; i < list.size(); i++) {
            final CodedEvent curEvent = list.get(i);
            if (!(("event occurrence").equals(curEvent.getComment())
                    || ("delayed event").equals(curEvent.getComment()))) {
                // Skip the event if it is a delayed event or the nth occurrence:
                // no phenomenon is created in these cases. Otherwise it is.
                newList.add(curEvent);
            }
        }
        return newList;
    }

    /**
     * Build, if possible, the {@link Phenomenon} corresponding to a pair of {@link CodedEvent}.
     * 
     * @param dateStart
     *        the definition interval start boundary
     * @param dateEnd
     *        the definition interval end boundary
     * @param event
     *        the {@link CodingEventDetector} for the current phenomenon
     * @param formerEvent
     *        the former {@link CodedEvent}
     * @param currentEvent
     *        the current {@link CodedEvent}
     * @param listSize
     *        the size of the {@link CodedEventsList}
     * @param i
     *        the index of the current {@link CodedEvent} in the list
     * 
     * @return the {@link Phenomenon} (or null if the phenomenon was not created)
     * 
     * @since 1.1
     */
    private Phenomenon buildPhenomenonFromEvents(final AbsoluteDate dateStart, final AbsoluteDate dateEnd,
                                                 final CodingEventDetector event, final CodedEvent formerEvent,
                                                 final CodedEvent currentEvent,
                                                 final int listSize, final int i) {

        // Create the phenomenon
        Phenomenon phenomenon = null;
        // Process the current event:
        if (currentEvent.isStartingEvent()) {
            // Current event = Starting event
            if (i == listSize - 1) {
                // There is no ending event.
                // We create a Phenomenon with an undefined ending event.
                phenomenon = new Phenomenon(currentEvent, true, CodedEvent.buildUndefinedEvent(dateEnd, false), false,
                    event.getPhenomenonCode(), "Caution : undefined ending");
                // phenomList.add(phenomenon);
            } // else : we do nothing here, we will process the ending event instead.
        } else {
            // Current event = Ending event
            if (i == 0) {
                // There is no starting event.
                // We create a Phenomenon with an undefined beginning event.
                phenomenon = new Phenomenon(CodedEvent.buildUndefinedEvent(dateStart, true), false, currentEvent, true,
                    event.getPhenomenonCode(), "Caution : undefined beginning");

            } else {
                // We assume the former event was the starting event
                // We create a well-defined Phenomenon:
                phenomenon = new Phenomenon(formerEvent, true, currentEvent, true, event.getPhenomenonCode(), "");
            }
        }
        return phenomenon;
    }

    /**
     * <p>
     * This class wraps a {@link CodingEventDetector}, so that the {@link CodedEventsLogger} instance is triggered when
     * {@link EventDetector#eventOccurred(SpacecraftState, boolean, boolean) eventOccurred} happens, in order to log the
     * {@link CodedEvent}.
     * </p>
     * 
     * @concurrency not thread-safe
     * 
     */
    private final class CodingWrapper implements EventDetector {

        /** Serial UID. */
        private static final long serialVersionUID = -1539410653126739174L;

        /** Wrapped detector. */
        private final CodingEventDetector codingDetector;

        /**
         * Constructor; needs the wrapped {@link CodingEventDetector}.
         * 
         * @param detector
         *        a {@link CodingEventDetector} instance.
         */
        public CodingWrapper(final CodingEventDetector detector) {
            this.codingDetector = detector;
        }

        /** {@inheritDoc} */
        @Override
        @SuppressWarnings("PMD.ShortMethodName")
        public double g(final SpacecraftState s) throws PatriusException {
            // Returns the value of the coding detector g function
            return this.codingDetector.g(s);
        }

        /** {@inheritDoc} */
        @Override
        public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                    final boolean forward) throws PatriusException {
            // This call tells the CodedEventsLogger instance an event happened.
            CodedEventsLogger.this.processEvent(this.codingDetector, s, increasing);
            return this.codingDetector.eventOccurred(s, increasing, forward);
        }

        /** {@inheritDoc} */
        @Override
        public boolean shouldBeRemoved() {
            return this.codingDetector.shouldBeRemoved();
        }

        /** {@inheritDoc} */
        @Override
        public SpacecraftState resetState(final SpacecraftState oldState) throws PatriusException {
            // Reset the state prior to continue propagation.
            return this.codingDetector.resetState(oldState);
        }

        /** {@inheritDoc} */
        @Override
        public double getThreshold() {
            // Return the convergence threshold in the event time search of the coding detector.
            return this.codingDetector.getThreshold();
        }

        /** {@inheritDoc} */
        @Override
        public double getMaxCheckInterval() {
            // Return the maximal time interval between switching function checks of the coding detector.
            return this.codingDetector.getMaxCheckInterval();
        }

        /** {@inheritDoc} */
        @Override
        public int getMaxIterationCount() {
            // Return the maximal number of iterations in the event time search of the coding detector.
            return this.codingDetector.getMaxIterationCount();
        }

        /** {@inheritDoc} */
        @Override
        public void init(final SpacecraftState s0, final AbsoluteDate t) throws PatriusException {
            // Initialize event handler at the start of a propagation.
            this.codingDetector.init(s0, t);
        }

        /** {@inheritDoc} */
        @Override
        public int getSlopeSelection() {
            return this.codingDetector.getSlopeSelection();
        }

        /** {@inheritDoc} */
        @Override
        public EventDetector copy() {
            return new CodingWrapper((CodingEventDetector) this.codingDetector.copy());
        }
    }

    /**
     * This class is used to store the coded event with contextual information.
     * 
     * @concurrency not thread-safe
     * 
     * @see CodedEvent
     * @see CodingEventDetector
     * @see Phenomenon
     * @see AbsoluteDate
     * 
     */
    public static final class LoggedCodedEvent implements TimeStamped, Comparable<LoggedCodedEvent> {
        /** Detector that produced the event. */
        private final CodingEventDetector detector;
        /** Coded event. */
        private final CodedEvent event;
        /** State that caused the event. */
        private final SpacecraftState state;
        /** True if g increasing at the event. */
        private final boolean increasing;

        /**
         * Constructor for the logged event instance.
         * 
         * @param detectorIn
         *        the {@link CodingEventDetector} that generates the coded event.
         * @param eventIn
         *        the {@link CodedEvent} to be logged.
         * @param sIn
         *        the {@link SpacecraftState} that triggers the event.
         * @param increasingIn
         *        If true, g function increases around event date.
         */
        private LoggedCodedEvent(final CodingEventDetector detectorIn, final CodedEvent eventIn,
                final SpacecraftState sIn, final boolean increasingIn) {
            this.detector = detectorIn;
            this.event = eventIn;
            this.state = sIn;
            this.increasing = increasingIn;
        }

        /**
         * Gets the {@link CodingEventDetector} that generated the coded event.
         * 
         * @return the detector.
         */
        public CodingEventDetector getDetector() {
            return this.detector;
        }

        /**
         * Gets the {@link SpacecraftState} that caused the coded event.
         * 
         * @return the state.
         */
        public SpacecraftState getState() {
            return this.state;
        }

        /**
         * Tells if g increases or decreases around event date.
         * 
         * @return if true, g function increases around event date.
         */
        public boolean isIncreasing() {
            return this.increasing;
        }

        /**
         * Gets the coded event.
         * 
         * @return the coded event.
         */
        public CodedEvent getCodedEvent() {
            return this.event;
        }

        /** {@inheritDoc} */
        @Override
        public AbsoluteDate getDate() {
            // return the event date.
            return this.event.getDate();
        }

        /** {@inheritDoc} */
        @Override
        public int compareTo(final LoggedCodedEvent o) {
            if (this.getDate().compareTo(o.getDate()) == 0) {
                // when the date are the same, returns 1 :
                return 1;
            } else {
                return this.getDate().compareTo(o.getDate());
            }
        }
    }
}
