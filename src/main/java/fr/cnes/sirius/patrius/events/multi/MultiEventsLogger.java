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
 * @history created 18/03/2015
 * 
 * HISTORY
* VERSION:4.3:DM:DM-2089:15/05/2019:[PATRIUS] passage a Java 8
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:300:18/03/2015:Creation multi propagator
 * VERSION::DM:454:24/11/2015:Overload method shouldBeRemoved()
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events.multi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.EventDetector.Action;
import fr.cnes.sirius.patrius.propagation.events.multi.MultiEventDetector;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * This class is copied from {@link fr.cnes.sirius.patrius.propagation.events.EventsLogger} and adapted to multi
 * propagation.
 * </p>
 * This class logs multi events detectors events.
 * 
 * <p>
 * As {@link MultiEventDetector multi events detectors} are triggered during orbit propagation, an event specific
 * {@link MultiEventDetector#eventOccurred(java.util.Map, boolean, boolean) eventOccurred} method is called. This class
 * can be used to add a global logging feature registering all events with their corresponding states in a chronological
 * sequence (or reverse-chronological if propagation occurs backward).
 * </p>
 * <p>
 * This class works by wrapping user-provided {@link MultiEventDetector multi events detectors} before they are
 * registered to the propagator. The wrapper monitor the calls to
 * {@link MultiEventDetector#eventOccurred(java.util.Map, boolean, boolean) eventOccurred} and store the corresponding
 * events as {@link MultiLoggedEvent} instances. After propagation is complete, the user can retrieve all the events
 * that have occurred at once by calling method {@link #getLoggedEvents()}.
 * </p>
 * 
 * @concurrency not thread-safe
 * 
 * @author maggioranic
 * 
 * @version $Id$
 * 
 * @since 3.0
 * 
 */
public class MultiEventsLogger {

    /** List of occurred events. */
    private final List<MultiLoggedEvent> log;

    /**
     * Simple constructor.
     * <p>
     * Build an empty logger for events detectors.
     * </p>
     */
    public MultiEventsLogger() {
        this.log = new ArrayList<MultiEventsLogger.MultiLoggedEvent>();
    }

    /**
     * Monitor a multi event detector.
     * <p>
     * In order to monitor a multi event detector, it must be wrapped thanks to this method as follows:
     * </p>
     * 
     * <pre>
     * MultiPropagator propagator = new XyzPropagator(...);
     * MultiEventsLogger logger = new MultiEventsLogger();
     * MultiEventDetector detector = new UvwDetector(...);
     * propagator.addEventDetector(logger.monitorDetector(detector));
     * </pre>
     * <p>
     * Note that the event detector returned by the {@link MultiLoggedEvent#getEventDetector() getEventDetector} method
     * in {@link MultiLoggedEvent LoggedEvent} instances returned by {@link #getLoggedEvents()} are the
     * {@code monitoredDetector} instances themselves, not the wrapping detector returned by this method.
     * </p>
     * 
     * @param monitoredDetector
     *        multi event detector to monitor
     * @return the wrapping detector to add to the propagator
     */
    public MultiEventDetector monitorDetector(final MultiEventDetector monitoredDetector) {
        return new MultiLoggingWrapper(monitoredDetector);
    }

    /**
     * Clear the logged events.
     */
    public void clearLoggedEvents() {
        this.log.clear();
    }

    /**
     * Get an immutable copy of the logged events.
     * <p>
     * The copy is independent of the logger. It is preserved event if the {@link #clearLoggedEvents()
     * clearLoggedEvents} method is called and the logger reused in another propagation.
     * </p>
     * 
     * @return an immutable copy of the logged events
     */
    public List<MultiLoggedEvent> getLoggedEvents() {
        return new ArrayList<MultiEventsLogger.MultiLoggedEvent>(this.log);
    }

    /** Class for logged events entries. */
    public static final class MultiLoggedEvent {

        /** Multi event detector triggered. */
        private final MultiEventDetector multiDetector;

        /** Triggering states. */
        private final Map<String, SpacecraftState> triggeringStates;

        /** Increasing/decreasing status. */
        private final boolean increasing;

        /**
         * Simple constructor.
         * 
         * @param detector
         *        detector for event that was triggered in multi propagation case
         * @param states
         *        states at event trigger date
         * @param increasingIndicator
         *        indicator if the event switching function was increasing
         *        or decreasing at event occurrence date
         */
        private MultiLoggedEvent(final MultiEventDetector detector, final Map<String, SpacecraftState> states,
            final boolean increasingIndicator) {
            this.multiDetector = detector;
            this.triggeringStates = states;
            this.increasing = increasingIndicator;
        }

        /**
         * Get the event detector triggered.
         * 
         * @return event detector triggered
         */
        public MultiEventDetector getEventDetector() {
            return this.multiDetector;
        }

        /**
         * Get the triggering states.
         * 
         * @return triggering states
         * @see MultiEventDetector#eventOccurred(Map, boolean, boolean)
         */
        public Map<String, SpacecraftState> getStates() {
            return this.triggeringStates;
        }

        /**
         * Get the Increasing/decreasing status of the event.
         * 
         * @return increasing/decreasing status of the event
         * @see MultiEventDetector#eventOccurred(Map, boolean, boolean)
         */
        public boolean isIncreasing() {
            return this.increasing;
        }

    }

    /** Internal wrapper for multi events detectors. */
    private class MultiLoggingWrapper implements MultiEventDetector {

        /** Wrapped multi events detector. */
        private final MultiEventDetector multiDetector;

        /**
         * Simple constructor.
         * 
         * @param detector
         *        multi events detector to wrap
         */
        public MultiLoggingWrapper(final MultiEventDetector detector) {
            this.multiDetector = detector;
        }

        /** {@inheritDoc} */
        @Override
        public void init(final Map<String, SpacecraftState> s0, final AbsoluteDate t) {
            this.multiDetector.init(s0, t);
        }

        /** {@inheritDoc} */
        @Override
        @SuppressWarnings("PMD.ShortMethodName")
        public double g(final Map<String, SpacecraftState> s) throws PatriusException {
            return this.multiDetector.g(s);
        }

        /** {@inheritDoc} */
        @Override
        public Action eventOccurred(final Map<String, SpacecraftState> s, final boolean increasing,
                                    final boolean forward) throws PatriusException {
            MultiEventsLogger.this.log.add(new MultiLoggedEvent(this.multiDetector, s, increasing));
            return this.multiDetector.eventOccurred(s, increasing, forward);
        }

        /** {@inheritDoc} */
        @Override
        public boolean shouldBeRemoved() {
            return this.multiDetector.shouldBeRemoved();
        }

        /** {@inheritDoc} */
        @Override
        public Map<String, SpacecraftState>
                resetStates(
                            final Map<String, SpacecraftState> oldStates) throws PatriusException {
            return this.multiDetector.resetStates(oldStates);
        }

        /** {@inheritDoc} */
        @Override
        public double getThreshold() {
            return this.multiDetector.getThreshold();
        }

        /** {@inheritDoc} */
        @Override
        public double getMaxCheckInterval() {
            return this.multiDetector.getMaxCheckInterval();
        }

        /** {@inheritDoc} */
        @Override
        public int getMaxIterationCount() {
            return this.multiDetector.getMaxIterationCount();
        }

        /** {@inheritDoc} */
        @Override
        public int getSlopeSelection() {
            return this.multiDetector.getSlopeSelection();
        }
    }
}
