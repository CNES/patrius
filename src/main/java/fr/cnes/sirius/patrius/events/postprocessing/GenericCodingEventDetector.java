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
 * @history created 27/01/12
 *
 * HISTORY
 * VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
 * VERSION:4.13:FA:FA-79:08/12/2023:[PATRIUS] Probleme dans la fonction g de LocalTimeAngleDetector
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3146:10/05/2022:[PATRIUS] Ajout d'une methode static logEventsOverTimeInterval
 * VERSION:4.9:DM:DM-3172:10/05/2022:[PATRIUS] Ajout d'un throws PatriusException a la methode init de l'interface EDet
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::DM:454:24/11/2015:Overload method shouldBeRemoved()
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events.postprocessing;

import fr.cnes.sirius.patrius.events.EventDetector;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * This class represents an all-purpose implementation of the {@link CodingEventDetector} interface. <br>
 * It works using the {@link EventDetector} provided in the constructor.
 * </p>
 * This detector is able to build a {@link CodedEvent} for a given date using the method
 * {@link GenericCodingEventDetector#buildCodedEvent(SpacecraftState, boolean) buildCodedEvent}.<br>
 * You cannot set the CodedEvent comment through this implementation. Subclassing is permitted for
 * the purpose of adding functionality.
 * <p>
 * It supports phenomena or not, depending on which constructor was used. When it does support phenomena, the user can
 * know for a given input if the state is active using the method
 * {@link GenericCodingEventDetector#isStateActive(SpacecraftState) isStateActive}.
 * </p>
 * 
 * @concurrency not thread-safe or thread-hostile
 * 
 * @concurrency.comment As of now, existing Orekit EventDetector implementations are either not
 *                      thread-safe or thread-hostile, so this class also is. But this class could
 *                      probably become conditionally thread-safe; the main thread safety condition
 *                      would then be that the included EventDetector should be thread-safe.
 * 
 * @see CodingEventDetector
 * 
 * @author Pierre Cardoso
 * @author Tiziana Sabatini
 * 
 * @version $Id$
 * 
 * @since 1.1
 * 
 */
public class GenericCodingEventDetector implements CodingEventDetector {

    /** String "STANDARD". */
    private static final String STD_TYPE = "STANDARD";
    /** String "DELAY". */
    private static final String DELAY_TYPE = "DELAY";
    /** String "N_OCCURRENCE". */
    private static final String OCCUR_TYPE = "N_OCCURRENCE";
    /** The comment string for generic coded events. */
    private static final String GENERIC = "generic event";
    /** The string for delayed coded events. */
    private static final String DELAYED = "DELAYED ";

    /** Serializable UID. */
    private static final long serialVersionUID = -8199109406635873283L;

    /** Inner event detector. */
    private final EventDetector eventDetector;

    /**
     * Code for the event that is triggered when g increases (from negative to positive value).
     */
    private final String increasingCode;

    /**
     * Code for the event that is triggered when g decreases (from positive to negative value).
     */
    private final String decreasingCode;

    /**
     * True if increasing of the g function (from negative to positive value) represents the
     * "beginning" of the phenomenon associated to the event.
     */
    private final boolean increasingIsStart;

    /** True when phenomena are supported. */
    private final boolean phenomenaSupported;

    /** Phenomenon code. */
    private final String phenomenonCode;

    /** The delay of the event to log from the event detected during propagation. */
    private final double delay;

    /** The occurrence number of the event to log. */
    private final int occurrence;

    /**
     * Parameter that tracks the number of occurrences of the event during propagation.
     */
    private int nOccurrence = 0;

    /**
     * Code for the event we want to log: <br>
     * - STANDARD: no delay and no occurrence number;<br>
     * - DELAY: a delay is associated to the logged event;<br>
     * - N_OCCURRENCE: an occurrence number is associated to the logged event.
     */
    private final String eventType;

    /**
     * Constructor for a {@link GenericCodingEventDetector} that supports a {@link Phenomenon}.<br>
     * No delays and no occurrence numbers are associated to the events detected by this detector.
     * 
     * @param eventDetectorIn the {@link EventDetector} of the current event
     * @param increasingCodeIn code identifying the "increasing" event
     * @param decreasingCodeIn code identifying the "decreasing" event
     * @param increasingIsStartIn true if increasing of the g function represents the "beginning" of
     *        the associated phenomenon
     * @param phenomenonCodeIn code identifying the phenomenon associated to the event
     * 
     * @see CodingEventDetector
     * @see EventDetector
     * @see Phenomenon
     */
    public GenericCodingEventDetector(final EventDetector eventDetectorIn,
        final String increasingCodeIn, final String decreasingCodeIn,
        final boolean increasingIsStartIn, final String phenomenonCodeIn) {
        this.eventDetector = eventDetectorIn;
        this.decreasingCode = decreasingCodeIn;
        this.increasingCode = increasingCodeIn;
        this.increasingIsStart = increasingIsStartIn;
        this.phenomenaSupported = true;
        this.phenomenonCode = phenomenonCodeIn;
        this.delay = 0;
        this.occurrence = 0;
        this.eventType = STD_TYPE;
    }

    /**
     * Constructor for a {@link GenericCodingEventDetector} that supports a {@link Phenomenon}.<br>
     * A delay and/or an occurrence number can be associated to the events detected by this
     * detector.<br>
     * When a delay is added to the detected events, two kinds of events will be coded: <br>
     * - the original events, with their original codes;<br>
     * - the delayed events, whose codes will be "DELAYED_" followed by the original code.
     * 
     * @param eventDetectorIn the {@link EventDetector} of the current event
     * @param increasingCodeIn code identifying the "increasing" event
     * @param decreasingCodeIn code identifying the "decreasing" event
     * @param increasingIsStartIn true if increasing of the g function represents the "beginning" of
     *        the associated phenomenon
     * @param phenomenonCodeIn code identifying the phenomenon associated to the event
     * @param delayIn the delay of the output events with respect to the events detected during
     *        propagation; if it is set to zero, no delay will be added to the events
     * @param occurrenceIn the occurrence number of the output event (only the nth event will be
     *        detected); if it is set to zero, all events will be detected
     * 
     * @see CodingEventDetector
     * @see EventDetector
     * @see Phenomenon
     */
    public GenericCodingEventDetector(final EventDetector eventDetectorIn,
        final String increasingCodeIn, final String decreasingCodeIn,
        final boolean increasingIsStartIn, final String phenomenonCodeIn, final double delayIn,
        final int occurrenceIn) {
        this.eventDetector = eventDetectorIn;
        this.decreasingCode = decreasingCodeIn;
        this.increasingCode = increasingCodeIn;
        this.increasingIsStart = increasingIsStartIn;
        this.phenomenaSupported = true;
        this.phenomenonCode = phenomenonCodeIn;
        this.delay = delayIn;
        this.occurrence = occurrenceIn;
        if (this.occurrence != 0) {
            this.eventType = OCCUR_TYPE;
        } else if (this.delay != 0) {
            this.eventType = DELAY_TYPE;
        } else {
            this.eventType = STD_TYPE;
        }
    }

    /**
     * Constructor for a {@link GenericCodingEventDetector} that does not support a {@link Phenomenon}. No delays and no
     * occurrence numbers are associated to the events detected
     * by this detector.
     * 
     * @param eventDetectorIn the {@link EventDetector} of the current event
     * @param increasingCodeIn code identifying the "increasing" event
     * @param decreasingCodeIn code identifying the "decreasing" event
     * 
     * @see CodingEventDetector
     * @see EventDetector
     */
    public GenericCodingEventDetector(final EventDetector eventDetectorIn,
        final String increasingCodeIn, final String decreasingCodeIn) {
        this.eventDetector = eventDetectorIn;
        this.increasingCode = increasingCodeIn;
        this.decreasingCode = decreasingCodeIn;
        // the following parameter is set to false because we don't need it
        // (phenomena are not supported)
        this.increasingIsStart = false;
        this.phenomenaSupported = false;
        this.phenomenonCode = "";
        this.delay = 0;
        this.occurrence = 0;
        this.eventType = STD_TYPE;
    }

    /**
     * Constructor for a {@link GenericCodingEventDetector} that does not support a {@link Phenomenon}.<br>
     * When a delay is added to the detected events, two kinds of events will be coded: <br>
     * - the original events, with their original codes;<br>
     * - the delayed events, whose codes will be "DELAYED_" followed by the original code.
     * 
     * @param eventDetectorIn the {@link EventDetector} of the current event
     * @param increasingCodeIn code identifying the "increasing" event
     * @param decreasingCodeIn code identifying the "decreasing" event
     * @param delayIn the delay of the output events with respect to the events detected during
     *        propagation; if it is set to zero, no delay will be added to the events
     * @param occurrenceIn the occurrence number of the output event (only the nth event will be
     *        detected); if it is set to zero, all events will be detected
     * @see CodingEventDetector
     * @see EventDetector
     */
    public GenericCodingEventDetector(final EventDetector eventDetectorIn,
        final String increasingCodeIn, final String decreasingCodeIn, final double delayIn,
        final int occurrenceIn) {
        this.eventDetector = eventDetectorIn;
        this.increasingCode = increasingCodeIn;
        this.decreasingCode = decreasingCodeIn;
        // the following parameter is set to false because we don't need it
        // (phenomena are not supported)
        this.increasingIsStart = false;
        this.phenomenaSupported = false;
        this.phenomenonCode = "";
        this.delay = delayIn;
        this.occurrence = occurrenceIn;
        if (this.occurrence != 0) {
            this.eventType = OCCUR_TYPE;
        } else if (this.delay != 0) {
            this.eventType = DELAY_TYPE;
        } else {
            this.eventType = STD_TYPE;
        }
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.ShortMethodName")
    public final double g(final SpacecraftState s) throws PatriusException {
        // Call forwarded to the inner event detector instance
        return this.eventDetector.g(s);
    }

    /** {@inheritDoc} */
    @Override
    public final Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                      final boolean forward) throws PatriusException {
        // Call forwarded to the inner event detector instance
        return this.eventDetector.eventOccurred(s, increasing, forward);
    }

    /** {@inheritDoc} */
    @Override
    public boolean shouldBeRemoved() {
        return this.eventDetector.shouldBeRemoved();
    }

    /** {@inheritDoc} */
    @Override
    public final SpacecraftState resetState(final SpacecraftState oldState) throws PatriusException {
        // Call forwarded to the inner event detector instance
        return this.eventDetector.resetState(oldState);
    }

    /** {@inheritDoc} */
    @Override
    public final double getThreshold() {
        // Call forwarded to the inner event detector instance
        return this.eventDetector.getThreshold();
    }

    /** {@inheritDoc} */
    @Override
    public final double getMaxCheckInterval() {
        // Call forwarded to the inner event detector instance
        return this.eventDetector.getMaxCheckInterval();
    }

    /** {@inheritDoc} */
    @Override
    public final int getMaxIterationCount() {
        // Call forwarded to the inner event detector instance
        return this.eventDetector.getMaxIterationCount();
    }

    /**
     * Provides a code for an event, knowing if the g function increases or decreases.
     * 
     * @param increasing if true, g function increases around event date.
     * @return a code
     */
    private String getEventCode(final boolean increasing) {
        final String code;
        if (increasing) {
            code = this.increasingCode;
        } else {
            code = this.decreasingCode;
        }
        return code;
    }

    /** {@inheritDoc} */
    @Override
    public final CodedEvent buildCodedEvent(final SpacecraftState s, final boolean increasing) {
        // The CodedEvent built here is a generic coded event
        // as described by the constructor's parameters
        final String eventCode = this.getEventCode(increasing);
        // the comment emphasizes the genericity of the built event
        final String comment = GENERIC;
        final AbsoluteDate eventDate = s.getDate();
        final boolean isStarting = (this.increasingIsStart ? increasing : !increasing);
        return new CodedEvent(eventCode, comment, eventDate, isStarting);
    }

    /** {@inheritDoc} */
    @Override
    public final CodedEvent buildDelayedCodedEvent(final SpacecraftState s, final boolean increasing) {
        // The CodedEvent built here is a generic coded event
        // as described by the constructor's parameters
        final String eventCode = this.getEventCode(increasing);
        final String comment = GENERIC;
        // Delay added to the event
        final AbsoluteDate eventDate = s.getDate().shiftedBy(this.delay);
        final boolean isStarting = (this.increasingIsStart ? increasing : !increasing);
        return new CodedEvent(DELAYED + eventCode, comment, eventDate, isStarting);
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.UseStringBufferForStringAppends")
    public final CodedEvent buildOccurrenceCodedEvent(final SpacecraftState s,
                                                      final boolean increasing) {
        // increases the counter of the number of occurrences when one CodedEvent is detected
        this.nOccurrence++;

        // Only build the nth occurrence
        if (this.nOccurrence == this.occurrence) {
            String eventCode = this.getEventCode(increasing);
            if (this.delay != 0) {
                // Delay in code
                eventCode = DELAYED + eventCode;
            }
            final String comment = GENERIC;
            // delay added
            final AbsoluteDate eventDate = s.getDate().shiftedBy(this.delay);
            final boolean isStarting = (this.increasingIsStart ? increasing : !increasing);
            // Occurrence number in code
            return new CodedEvent(eventCode + " N." + this.occurrence, comment, eventDate, isStarting);
        } else {
            // when the CodedEvent has not the right occurrence number, returns null
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public final boolean positiveSignMeansActive() {
        // return the appropriate boolean
        return this.increasingIsStart;
    }

    /** {@inheritDoc} */
    @Override
    public final String getPhenomenonCode() {
        String rez = null;
        // Only returns non-null if phenomena is supported
        if (this.phenomenaSupported) {
            rez = this.phenomenonCode;
        }
        return rez;
    }

    /**
     * Tells if the event state is "active" for the given input. If phenomena are unsupported by
     * this instance, it will always return false.
     * 
     * @param state the input {@link SpacecraftState}
     * @return true if the state is "active" according to the convention chosen in the constructor.
     * @throws PatriusException from the wrapped event detector.
     */
    public final boolean isStateActive(final SpacecraftState state) throws PatriusException {
        boolean rez = false;
        if (this.phenomenaSupported) {
            final boolean posSig = (this.eventDetector.g(state) >= 0.);
            rez = (this.positiveSignMeansActive() ? posSig : !posSig);
        }
        return rez;
    }

    /** {@inheritDoc} */
    @Override
    public final String getEventType() {
        // Appropriate value is returned
        return this.eventType;
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState s0, final AbsoluteDate t) throws PatriusException {
        // Call forwarded to the inner event detector instance
        this.eventDetector.init(s0, t);
    }

    /** {@inheritDoc} */
    @Override
    public int getSlopeSelection() {
        return this.eventDetector.getSlopeSelection();
    }

    /** {@inheritDoc} */
    @Override
    public EventDetector copy() {
        return new GenericCodingEventDetector(this.eventDetector.copy(), this.increasingCode, this.decreasingCode,
            this.increasingIsStart, this.phenomenonCode, this.delay, this.occurrence);
    }

    /**
     * Log detected events on a given time interval into the entered events logger.
     * 
     * @param eventsLogger
     *        input events logger in which the events must be recorded
     * @param satProp
     *        spacecraft orbit propagator
     * @param detector
     *        detector to be monitored
     * @param interval
     *        time interval on which the events are looked for
     * 
     * @return the spacecraft state after propagation
     * @throws PatriusException
     *         if spacecraft state cannot be propagated
     */
    public static SpacecraftState logEventsOverTimeInterval(final CodedEventsLogger eventsLogger,
                                                            final Propagator satProp,
                                                            final CodingEventDetector detector,
                                                            final AbsoluteDateInterval interval)
        throws PatriusException {
        // the logger monitors the visibility detector
        final EventDetector monitoredDetector = eventsLogger.monitorDetector(detector);
        // add the detector to the propagator
        satProp.addEventDetector(monitoredDetector);
        // propagation
        final SpacecraftState spacecraftState = satProp.propagate(interval.getLowerData(), interval.getUpperData());
        // clear events detectors
        satProp.clearEventsDetectors();
        return spacecraftState;
    }

    /** {@inheritDoc} */
    @Override
    public boolean filterEvent(final SpacecraftState state,
            final boolean increasing,
            final boolean forward) throws PatriusException {
        // Do nothing by default, event is not filtered
        return eventDetector.filterEvent(state, increasing, forward);
    }
}
