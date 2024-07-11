/**
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
 * HISTORY
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::DM:454:24/11/2015:Unimplemented method shouldBeRemoved()
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.validation.events;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.ApsideDetector;
import fr.cnes.sirius.patrius.propagation.events.CircularFieldOfViewDetector;
import fr.cnes.sirius.patrius.propagation.events.DihedralFieldOfViewDetector;
import fr.cnes.sirius.patrius.propagation.events.EclipseDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.propagation.events.NodeDetector;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * This class is based on the class EventsLogger in Orekit: it creates a list of LoggedEvents during a propagation. It
 * has been created to add some properties in the LoggedEvents object.
 * </p>
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id: EventsLogger.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.0
 * 
 */
public class EventsLogger {

    /** List of occurred events. */
    private final List<LoggedEvent> log;

    /**
     * Simple constructor.
     * <p>
     * Build an empty logger for events detectors.
     * </p>
     */
    public EventsLogger() {
        this.log = new ArrayList<EventsLogger.LoggedEvent>();
    }

    /**
     * Monitor an event detector.
     * <p>
     * Same function of EventsLogger in Orekit, plus the parameter "name".
     * </p>
     * 
     * @param monitoredDetector
     *        event detector to monitor
     * @param name
     *        the event name
     *        to enable logging
     * @return detector
     */
    public final EventDetector monitorDetector(final EventDetector monitoredDetector,
                                               final String name) {
        return new LoggingWrapper(monitoredDetector, name);
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
     * Same function of EventsLogger in Orekit.
     * </p>
     * 
     * @return an immutable copy of the logged events
     */
    public List<LoggedEvent> getLoggedEvents() {
        return new ArrayList<EventsLogger.LoggedEvent>(this.log);
    }

    /** Class for logged events entries. */
    public static class LoggedEvent implements Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = -4491889728766419035L;

        /** Event detector triggered. */
        private final EventDetector detector;

        /** Event name. */
        private final String name;

        /** Triggering state. */
        private final SpacecraftState state;

        /** Increasing/decreasing status. */
        private final boolean increasing;

        /**
         * Simple constructor.
         * 
         * @param detector
         *        detector
         * @param name
         *        name
         * @param state
         *        state
         * @param increasing
         *        increasing
         */
        private LoggedEvent(final EventDetector detector,
            final String name, final SpacecraftState state,
            final boolean increasing) {
            this.detector = detector;
            this.state = state;
            this.name = name;
            this.increasing = increasing;
        }

        /**
         * Get the event detector triggered.
         * 
         * @return event detector triggered
         */
        public EventDetector getEventDetector() {
            return this.detector;
        }

        /**
         * Get the event detector triggered.
         * 
         * @return event detector triggered
         */
        public String getEventName() {
            return this.name;
        }

        /**
         * Get the event detector triggered.
         * 
         * @return event detector triggered
         */
        public String getEventState() {
            String state = null;
            if (this.isIncreasing()) {
                if (EclipseDetector.class.equals(this.detector.getClass()
                    .getGenericSuperclass())) {
                    state = "Exit eclipse";
                }
                else if (NodeDetector.class.equals(this.detector.getClass()
                    .getGenericSuperclass())) {
                    state = "Ascending node";
                }
                else if (ApsideDetector.class.equals(this.detector.getClass()
                    .getGenericSuperclass())) {
                    state = "Perigee";
                }
                else if (CircularFieldOfViewDetector.class.equals(this.detector.getClass().getGenericSuperclass())) {
                    state = "Enter visibility";
                }
                else if (DihedralFieldOfViewDetector.class.equals(this.detector.getClass().getGenericSuperclass())) {
                    state = "Enter visibility";
                }
            } else {
                if (EclipseDetector.class.equals(this.detector.getClass()
                    .getGenericSuperclass())) {
                    state = "Enter eclipse";
                }
                else if (NodeDetector.class.equals(this.detector.getClass()
                    .getGenericSuperclass())) {
                    state = "Descending node";
                }
                else if (ApsideDetector.class.equals(this.detector.getClass()
                    .getGenericSuperclass())) {
                    state = "Apogee";
                }
                else if (CircularFieldOfViewDetector.class.equals(this.detector.getClass().getGenericSuperclass())) {
                    state = "Exit visibility";
                }
                else if (DihedralFieldOfViewDetector.class.equals(this.detector.getClass().getGenericSuperclass())) {
                    state = "Exit visibility";
                }
            }
            return state;
        }

        /**
         * Get the triggering state.
         * 
         * @return triggering state
         * @see EventDetector#eventOccurred(SpacecraftState, boolean, boolean)
         */
        public SpacecraftState getState() {
            return this.state;
        }

        /**
         * Get the Increasing/decreasing status of the event.
         * 
         * @return increasing/decreasing status of the event
         * @see EventDetector#eventOccurred(SpacecraftState, boolean, boolean)
         */
        public boolean isIncreasing() {
            return this.increasing;
        }

    }

    /** Internal wrapper for events detectors. */
    private class LoggingWrapper implements EventDetector {

        /** Serializable UID. */
        private static final long serialVersionUID = 2572438914929652326L;

        /** Wrapped events detector. */
        private final EventDetector detector;

        /** Wrapped events detector. */
        private final String name;

        /**
         * Simple constructor.
         * 
         * @param detector
         *        events detector to wrap
         * @param name
         *        events detector name
         */
        public LoggingWrapper(final EventDetector detector, final String name) {
            this.detector = detector;
            this.name = name;
        }

        /** {@inheritDoc} */
        @Override
        public double g(final SpacecraftState s) throws PatriusException {
            return this.detector.g(s);
        }

        /** {@inheritDoc} */
        @Override
        public boolean shouldBeRemoved() {
            return false;
        }

        /** {@inheritDoc} */
        @Override
        public
                Action
                eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                       throws PatriusException {
            EventsLogger.this.log.add(new LoggedEvent(this.detector, this.name, s, increasing));
            return this.detector.eventOccurred(s, increasing, forward);
        }

        /** {@inheritDoc} */
        @Override
        public SpacecraftState resetState(final SpacecraftState oldState)
                                                                         throws PatriusException {
            return this.detector.resetState(oldState);
        }

        /** {@inheritDoc} */
        @Override
        public double getThreshold() {
            return this.detector.getThreshold();
        }

        /** {@inheritDoc} */
        @Override
        public double getMaxCheckInterval() {
            return this.detector.getMaxCheckInterval();
        }

        /** {@inheritDoc} */
        @Override
        public int getMaxIterationCount() {
            return this.detector.getMaxIterationCount();
        }

        /**
         * Does nothing.
         * 
         * @see fr.cnes.sirius.patrius.propagation.events.EventDetector#init(fr.cnes.sirius.patrius.propagation.SpacecraftState,
         *      fr.cnes.sirius.patrius.time.AbsoluteDate)
         */
        @Override
        public void init(final SpacecraftState s0, final AbsoluteDate t) {
            // Does nothing.
        }

        @Override
        public int getSlopeSelection() {
            return 2;
        }

        @Override
        public EventDetector copy() {
            return null;
        }
    }
}
