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
 * @history created 18/03/2015
 *
 * HISTORY
 * VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
 * VERSION:4.13:FA:FA-79:08/12/2023:[PATRIUS] Probleme dans la fonction g de LocalTimeAngleDetector
 * VERSION:4.13:FA:FA-112:08/12/2023:[PATRIUS] Probleme si Earth est utilise comme corps pivot pour mar097.bsp
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3172:10/05/2022:[PATRIUS] Ajout d'un throws PatriusException a la methode init de l'interface EDet
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:300:18/03/2015:Creation multi propagator
 * VERSION::DM:454:24/11/2015:Overload method shouldBeRemoved()
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events.postprocessing;

import java.util.Map;

import fr.cnes.sirius.patrius.events.EventDetector.Action;
import fr.cnes.sirius.patrius.events.MultiEventDetector;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * This class is copied from {@link GenericCodingEventDetector} and adapted to multi propagation.
 * </p>
 * <p>
 * This class represents an all-purpose implementation of the {@link MultiCodingEventDetector} interface.<br>
 * It works using the {@link MultiEventDetector} provided in the constructor.
 * </p>
 * This detector is able to build a {@link CodedEvent} for a given date
 * using the method {@link #buildCodedEvent(Map, boolean) buildCodedEvent}.<br>
 * You cannot set the CodedEvent comment through this implementation. Subclassing is permitted for the purpose of adding
 * functionality.
 * <p>
 * It supports phenomena or not, depending on which constructor was used. When it does support phenomena, the user can
 * know for a given input if the state is active using the method {@link #isStateActive(Map) isStateActive}.
 * </p>
 * 
 * @concurrency not thread-safe or thread-hostile
 * 
 * @concurrency.comment As of now, existing MultiEventDetector implementations are either not thread-safe
 *                      or thread-hostile, so this class also is.
 *                      But this class could probably become conditionally thread-safe;
 *                      the main thread safety condition would then be that the included MultiEventDetector
 *                      should be thread-safe.
 * 
 * @see MultiCodingEventDetector
 * 
 * @author maggioranic
 * 
 * @version $Id$
 * 
 * @since 3.0
 * 
 */
public class MultiGenericCodingEventDetector implements MultiCodingEventDetector {

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

    /**
     * Code for the multi event that is triggered when g increases (from negative to positive value)
     */
    private final String increasingCode;

    /**
     * Code for the multi event that is triggered when g decreases (from positive to negative value)
     */
    private final String decreasingCode;

    /**
     * True if increasing of the g function (from negative to positive value) represents the "beginning" of the
     * phenomenon associated to the event.
     */
    private final boolean increasingIsStart;

    /** True when phenomena are supported */
    private final boolean phenomenaSupported;

    /** Inner multi event detector */
    private final MultiEventDetector multiEventDetector;

    /** Phenomenon code */
    private final String phenomenonCode;

    /** The delay of the event to log from the event detected during propagation */
    private final double delay;

    /** The occurrence number of the event to log */
    private final int occurrence;

    /** Parameter that tracks the number of occurrences of the event during propagation */
    private int nOccurrence = 0;

    /**
     * Code for the event we want to log: <br>
     * - STANDARD: no delay and no occurrence number;<br>
     * - DELAY: a delay is associated to the logged event;<br>
     * - N_OCCURRENCE: an occurrence number is associated to the logged event
     */
    private final String eventType;

    /**
     * Constructor for a {@link MultiGenericCodingEventDetector} that supports a {@link Phenomenon}.<br>
     * No delays and no occurrence numbers are associated to the events detected by this detector.
     * 
     * @param multiEventDetectorIn
     *        the {@link MultiEventDetector} of the current event
     * @param increasingCodeIn
     *        code identifying the "increasing" event
     * @param decreasingCodeIn
     *        code identifying the "decreasing" event
     * @param increasingIsStartIn
     *        true if increasing of the g function represents the
     *        "beginning" of the associated phenomenon
     * @param phenomenonCodeIn
     *        code identifying the phenomenon associated to the event
     * 
     * @see MultiCodingEventDetector
     * @see MultiEventDetector
     * @see Phenomenon
     */
    public MultiGenericCodingEventDetector(final MultiEventDetector multiEventDetectorIn,
        final String increasingCodeIn, final String decreasingCodeIn, final boolean increasingIsStartIn,
        final String phenomenonCodeIn) {
        this.multiEventDetector = multiEventDetectorIn;
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
     * Constructor for a {@link MultiGenericCodingEventDetector} that supports a {@link Phenomenon}.<br>
     * A delay and/or an occurrence number can be associated to the events detected by this detector.<br>
     * When a delay is added to the detected events, two kinds of events will be coded: <br>
     * - the original events, with their original codes;<br>
     * - the delayed events, whose codes will be "DELAYED_" followed by the original code.
     * 
     * @param multiEventDetectorIn
     *        the {@link MultiEventDetector} of the current event
     * @param increasingCodeIn
     *        code identifying the "increasing" event
     * @param decreasingCodeIn
     *        code identifying the "decreasing" event
     * @param increasingIsStartIn
     *        true if increasing of the g function represents the
     *        "beginning" of the associated phenomenon
     * @param phenomenonCodeIn
     *        code identifying the phenomenon associated to the event
     * @param delayIn
     *        the delay of the output events with respect to the events detected during propagation;
     *        if it is set to zero, no delay will be added to the events
     * @param occurrenceIn
     *        the occurrence number of the output event (only the nth event will be detected);
     *        if it is set to zero, all events will be detected
     * 
     * @see MultiCodingEventDetector
     * @see MultiEventDetector
     * @see Phenomenon
     */
    public MultiGenericCodingEventDetector(final MultiEventDetector multiEventDetectorIn,
        final String increasingCodeIn, final String decreasingCodeIn, final boolean increasingIsStartIn,
        final String phenomenonCodeIn, final double delayIn, final int occurrenceIn) {
        this.multiEventDetector = multiEventDetectorIn;
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
     * Constructor for a {@link MultiGenericCodingEventDetector} that does not
     * support a {@link Phenomenon}.
     * No delays and no occurrence numbers are associated to the events detected by this detector.
     * 
     * @param multiEventDetectorIn
     *        the {@link MultiEventDetector} of the current event
     * @param increasingCodeIn
     *        code identifying the "increasing" event
     * @param decreasingCodeIn
     *        code identifying the "decreasing" event
     * 
     * @see MultiCodingEventDetector
     * @see MultiEventDetector
     */
    public MultiGenericCodingEventDetector(final MultiEventDetector multiEventDetectorIn,
        final String increasingCodeIn, final String decreasingCodeIn) {
        this.multiEventDetector = multiEventDetectorIn;
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
     * Constructor for a {@link MultiGenericCodingEventDetector} that does not
     * support a {@link Phenomenon}.<br>
     * When a delay is added to the detected events, two kinds of events will be coded: <br>
     * - the original events, with their original codes;<br>
     * - the delayed events, whose codes will be "DELAYED_" followed by the original code.
     * 
     * @param multiEventDetectorIn
     *        the {@link MultiEventDetector} of the current event
     * @param increasingCodeIn
     *        code identifying the "increasing" event
     * @param decreasingCodeIn
     *        code identifying the "decreasing" event
     * @param delayIn
     *        the delay of the output events with respect to the events detected during propagation;
     *        if it is set to zero, no delay will be added to the events
     * @param occurrenceIn
     *        the occurrence number of the output event (only the nth event will be detected);
     *        if it is set to zero, all events will be detected
     * @see MultiCodingEventDetector
     * @see MultiEventDetector
     */
    public MultiGenericCodingEventDetector(final MultiEventDetector multiEventDetectorIn,
        final String increasingCodeIn,
        final String decreasingCodeIn,
        final double delayIn,
        final int occurrenceIn) {
        this.multiEventDetector = multiEventDetectorIn;
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
    public void init(final Map<String, SpacecraftState> s0, final AbsoluteDate t) throws PatriusException {
        // Call forwarded to the inner event detector instance
        this.multiEventDetector.init(s0, t);
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.ShortMethodName")
    public double g(final Map<String, SpacecraftState> s) throws PatriusException {
        // Call forwarded to the inner event detector instance
        return this.multiEventDetector.g(s);
    }

    /** {@inheritDoc} */
    @Override
    public Action eventOccurred(final Map<String, SpacecraftState> s, final boolean increasing,
                                final boolean forward) throws PatriusException {
        // Call forwarded to the inner event detector instance
        return this.multiEventDetector.eventOccurred(s, increasing, forward);
    }

    /** {@inheritDoc} */
    @Override
    public boolean shouldBeRemoved() {
        return this.multiEventDetector.shouldBeRemoved();
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, SpacecraftState>
            resetStates(
                        final Map<String, SpacecraftState> oldStates) throws PatriusException {
        // Call forwarded to the inner event detector instance
        return this.multiEventDetector.resetStates(oldStates);
    }

    /** {@inheritDoc} */
    @Override
    public double getThreshold() {
        // Call forwarded to the inner event detector instance
        return this.multiEventDetector.getThreshold();
    }

    /** {@inheritDoc} */
    @Override
    public double getMaxCheckInterval() {
        // Call forwarded to the inner event detector instance
        return this.multiEventDetector.getMaxCheckInterval();
    }

    /** {@inheritDoc} */
    @Override
    public int getMaxIterationCount() {
        // Call forwarded to the inner event detector instance
        return this.multiEventDetector.getMaxIterationCount();
    }

    /** {@inheritDoc} */
    @Override
    public int getSlopeSelection() {
        return this.multiEventDetector.getSlopeSelection();
    }

    /**
     * Provides a code for a multi event, knowing if the g function increases or
     * decreases.
     * 
     * @param increasing
     *        if true, g function increases around event date.
     * @return a code
     */
    private String getMultiEventCode(final boolean increasing) {
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
    public CodedEvent buildCodedEvent(final Map<String, SpacecraftState> states, final boolean increasing) {
        // The CodedEvent built here is a generic coded event
        // as described by the constructor's parameters
        final String multiEventCode = this.getMultiEventCode(increasing);
        // the comment emphasizes the genericity of the built event
        final String comment = GENERIC;
        final AbsoluteDate eventDate = states.get(states.keySet().iterator().next()).getDate();
        final boolean isStarting =
            (this.increasingIsStart ? increasing : !increasing);
        return new CodedEvent(multiEventCode, comment, eventDate, isStarting);
    }

    /** {@inheritDoc} */
    @Override
    public CodedEvent buildDelayedCodedEvent(final Map<String, SpacecraftState> states, final boolean increasing) {
        // The CodedEvent built here is a generic coded event
        // as described by the constructor's parameters
        final String multiEventCode = this.getMultiEventCode(increasing);
        final String comment = GENERIC;
        // Delay added to the event
        final AbsoluteDate eventDate = states.get(states.keySet().iterator().next()).getDate().shiftedBy(this.delay);
        final boolean isStarting =
            (this.increasingIsStart ? increasing : !increasing);
        return new CodedEvent(DELAYED + multiEventCode, comment, eventDate, isStarting);
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.UseStringBufferForStringAppends")
    public CodedEvent buildOccurrenceCodedEvent(final Map<String, SpacecraftState> states, final boolean increasing) {
        // increases the counter of the number of occurrences when one CodedEvent is detected
        this.nOccurrence++;

        // Only build the nth occurrence
        if (this.nOccurrence == this.occurrence) {
            String multiEventCode = this.getMultiEventCode(increasing);
            if (this.delay != 0) {
                // Delay in code
                multiEventCode = DELAYED + multiEventCode;
            }
            final String comment = GENERIC;
            // delay added
            final AbsoluteDate eventDate =
                states.get(states.keySet().iterator().next()).getDate().shiftedBy(this.delay);
            final boolean isStarting =
                (this.increasingIsStart ? increasing : !increasing);
            // Occurrence number in code
            return new CodedEvent(multiEventCode + " N." + this.occurrence, comment, eventDate, isStarting);
        } else {
            // when the CodedEvent has not the right occurrence number, returns null
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean positiveSignMeansActive() {
        // return the appropriate boolean
        return this.increasingIsStart;
    }

    /** {@inheritDoc} */
    @Override
    public String getPhenomenonCode() {
        String rez = null;
        // Only returns non-null if phenomena is supported
        if (this.phenomenaSupported) {
            rez = this.phenomenonCode;
        }
        return rez;
    }

    /**
     * Tells if the multi event state is "active" for the given input. If phenomena are unsupported by this instance, it
     * will always return false.
     * 
     * @param states
     *        the input map of {@link SpacecraftState}
     * @return true if the state is "active" according to the convention
     *         chosen in the constructor.
     * @throws PatriusException
     *         from the wrapped event detector.
     */
    public final boolean isStateActive(final Map<String, SpacecraftState> states) throws PatriusException {
        boolean rez = false;
        if (this.phenomenaSupported) {
            final boolean posSig = (this.multiEventDetector.g(states) >= 0.);
            rez = (this.positiveSignMeansActive() ? posSig : !posSig);
        }
        return rez;
    }

    /** {@inheritDoc} */
    @Override
    public String getEventType() {
        // Appropriate value is returned
        return this.eventType;
    }

    /** {@inheritDoc} */
    @Override
    public boolean filterEvent(final Map<String, SpacecraftState> states,
            final boolean increasing,
            final boolean forward) throws PatriusException {
        return multiEventDetector.filterEvent(states, increasing, forward);
    }
}
