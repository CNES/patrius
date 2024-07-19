/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 */
/*
 *
 * HISTORY
* VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
* VERSION:4.9:DM:DM-3172:10/05/2022:[PATRIUS] Ajout d'un throws PatriusException a la methode init de l'interface EDet
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
* VERSION:4.3:DM:DM-2089:15/05/2019:[PATRIUS] passage a Java 8
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 *                             (added forward parameter to eventOccurred signature)
 * VERSION::FA:410:16/04/2015: Anomalies in the Patrius Javadoc
 * VERSION::DM:454:24/11/2015:Overload method shouldBeRemoved() in class LoggingWrapper
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.events;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This class logs events detectors events during propagation.
 * 
 * <p>
 * As {@link EventDetector events detectors} are triggered during orbit propagation, an event specific
 * {@link EventDetector#eventOccurred(SpacecraftState, boolean, boolean) eventOccurred} method is called. This class can
 * be used to add a global logging feature registering all events with their corresponding states in a chronological
 * sequence (or reverse-chronological if propagation occurs backward).
 * </p>
 * <p>
 * This class works by wrapping user-provided {@link EventDetector
 * events detectors} before they are registered to the propagator. The wrapper monitor the calls to
 * {@link EventDetector#eventOccurred(SpacecraftState, boolean, boolean) eventOccurred} and store the corresponding
 * events as {@link LoggedEvent} instances. After propagation is complete, the user can retrieve all the events that
 * have occured at once by calling method {@link #getLoggedEvents()}.
 * </p>
 * 
 * @author Luc Maisonobe
 */
public class EventsLogger implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = -8643810076248572648L;

    /** List of occurred events. */
    private final List<LoggedEvent> log;

    /**
     * Simple constructor.
     * <p>
     * Build an empty logger for events detectors.
     * </p>
     */
    public EventsLogger() {
        this.log = new ArrayList<>();
    }

    /**
     * Monitor an event detector.
     * <p>
     * In order to monitor an event detector, it must be wrapped thanks to this method as follows:
     * </p>
     * 
     * <pre>
     * Propagator propagator = new XyzPropagator(...);
     * EventsLogger logger = new EventsLogger();
     * EventDetector detector = new UvwDetector(...);
     * propagator.addEventDetector(logger.monitorDetector(detector));
     * </pre>
     * <p>
     * Note that the event detector returned by the {@link LoggedEvent#getEventDetector() getEventDetector} method in
     * {@link LoggedEvent LoggedEvent} instances returned by {@link #getLoggedEvents()} are the
     * {@code monitoredDetector} instances themselves, not the wrapping detector returned by this method.
     * </p>
     * 
     * @param monitoredDetector
     *        event detector to monitor
     * @return the wrapping detector to add to the propagator
     */
    public EventDetector monitorDetector(final EventDetector monitoredDetector) {
        return new LoggingWrapper(monitoredDetector);
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
    public List<LoggedEvent> getLoggedEvents() {
        return new ArrayList<>(this.log);
    }

    /** Class for logged events entries. */
    public static final class LoggedEvent implements Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = -4491889728766419035L;

        /** Event detector triggered. */
        private final EventDetector detector;

        /** Triggering state. */
        private final SpacecraftState state;

        /** Increasing/decreasing status. */
        private final boolean increasing;

        /**
         * Simple constructor.
         * 
         * @param detectorIn
         *        detector for event that was triggered
         * @param stateIn
         *        state at event trigger date
         * @param increasingIn
         *        indicator if the event switching function was increasing
         *        or decreasing at event occurrence date
         */
        private LoggedEvent(final EventDetector detectorIn, final SpacecraftState stateIn,
            final boolean increasingIn) {
            this.detector = detectorIn;
            this.state = stateIn;
            this.increasing = increasingIn;
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

        /**
         * Simple constructor.
         * 
         * @param detectorIn
         *        events detector to wrap
         */
        public LoggingWrapper(final EventDetector detectorIn) {
            this.detector = detectorIn;
        }

        /** {@inheritDoc} */
        @Override
        public void init(final SpacecraftState s0, final AbsoluteDate t) throws PatriusException {
            this.detector.init(s0, t);
        }

        /** {@inheritDoc} */
        @Override
        @SuppressWarnings("PMD.ShortMethodName")
        public double g(final SpacecraftState s) throws PatriusException {
            return this.detector.g(s);
        }

        /** {@inheritDoc} */
        @Override
        public boolean shouldBeRemoved() {
            return this.detector.shouldBeRemoved();
        }

        /** {@inheritDoc} */
        @Override
        public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                    final boolean forward) throws PatriusException {
            EventsLogger.this.log.add(new LoggedEvent(this.detector, s, increasing));
            return this.detector.eventOccurred(s, increasing, forward);
        }

        /** {@inheritDoc} */
        @Override
        public SpacecraftState resetState(final SpacecraftState oldState) throws PatriusException {
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

        /** {@inheritDoc} */
        @Override
        public int getSlopeSelection() {
            return this.detector.getSlopeSelection();
        }

        /** {@inheritDoc} */
        @Override
        public EventDetector copy() {
            return new LoggingWrapper(this.detector.copy());
        }
    }
}
