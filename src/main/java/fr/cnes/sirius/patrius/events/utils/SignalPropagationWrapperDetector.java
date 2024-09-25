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
 * HISTORY
 * VERSION:4.13:FA:FA-79:08/12/2023:[PATRIUS] Probleme dans la fonction g de LocalTimeAngleDetector
 * VERSION:4.13:FA:FA-112:08/12/2023:[PATRIUS] Probleme si Earth est utilise comme corps pivot pour mar097.bsp
 * VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
 * VERSION:4.13:FA:FA-118:08/12/2023:[PATRIUS] Calcul d'union de PyramidalField invalide
 * VERSION:4.13:DM:DM-37:08/12/2023:[PATRIUS] Date d'evenement et propagation du signal
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import fr.cnes.sirius.patrius.events.EventDetector;
import fr.cnes.sirius.patrius.events.detectors.AbstractSignalPropagationDetector;
import fr.cnes.sirius.patrius.events.detectors.AbstractSignalPropagationDetector.DatationChoice;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Wrap a {@link AbstractSignalPropagationDetector signal propagation event detector} and compute the emitter & receiver
 * dates of the occurred events.<br>
 * 
 * <p>
 * The dates are store in Maps (accessible with a getter).<br>
 * For <i>N</i> events occurred, the Maps describe <i>N</i> elements.
 * </p>
 * 
 * @see AbstractSignalPropagationDetector
 * 
 * @author Thibaut BONIT
 *
 * @since 4.13
 */
public class SignalPropagationWrapperDetector implements EventDetector {

    /** Serializable UID. */
    private static final long serialVersionUID = 7211930278542226258L;

    /** Bracket right (mutualization). */
    private static final char BRACKET_RIGHT = ']';

    /** Wrapped signal propagation event detector. */
    private final AbstractSignalPropagationDetector eventDetector;

    /** The Map of the emitter (keys) & receiver (values) dates of the occurred events. */
    private final Map<AbsoluteDate, AbsoluteDate> emitterDatesMap;

    /** The Map of the receiver (keys) & emitter (values) dates of the occurred events. */
    private final Map<AbsoluteDate, AbsoluteDate> receiverDatesMap;

    /**
     * Constructor.
     * 
     * @param eventDetector
     *        Wrapped signal propagation detector
     */
    public SignalPropagationWrapperDetector(final AbstractSignalPropagationDetector eventDetector) {
        this.eventDetector = eventDetector;
        this.emitterDatesMap = new TreeMap<>();
        this.receiverDatesMap = new TreeMap<>();
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState s0, final AbsoluteDate t) throws PatriusException {
        this.emitterDatesMap.clear();
        this.receiverDatesMap.clear();
        this.eventDetector.init(s0, t);
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.ShortMethodName")
    public double g(final SpacecraftState s) throws PatriusException {
        return this.eventDetector.g(s);
    }

    /** {@inheritDoc} */
    @Override
    public Action eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
        throws PatriusException {
        // Extract the information about the emitter & receiver configuration from the wrapped event detector
        final AbsoluteDate date = s.getDate();
        final DatationChoice datationChoice = this.eventDetector.getDatationChoice();

        // Compute the emitter & receiver dates
        final AbsoluteDate emitterDate;
        final AbsoluteDate receiverDate;
        if (datationChoice == DatationChoice.EMITTER) {
            emitterDate = date;
            receiverDate = this.eventDetector.getSignalReceptionDate(s);
        } else {
            emitterDate = this.eventDetector.getSignalEmissionDate(s);
            receiverDate = date;
        }

        // Store these dates in the Maps
        this.emitterDatesMap.put(emitterDate, receiverDate);
        this.receiverDatesMap.put(receiverDate, emitterDate);

        // Call the wrapped event detector method
        return this.eventDetector.eventOccurred(s, increasing, forward);
    }

    /** {@inheritDoc} */
    @Override
    public boolean shouldBeRemoved() {
        return this.eventDetector.shouldBeRemoved();
    }

    /** {@inheritDoc} */
    @Override
    public SpacecraftState resetState(final SpacecraftState oldState) throws PatriusException {
        return this.eventDetector.resetState(oldState);
    }

    /** {@inheritDoc} */
    @Override
    public boolean filterEvent(final SpacecraftState state, final boolean increasing, final boolean forward)
        throws PatriusException {
        return this.eventDetector.filterEvent(state, increasing, forward);
    }

    /** {@inheritDoc} */
    @Override
    public double getThreshold() {
        return this.eventDetector.getThreshold();
    }

    /** {@inheritDoc} */
    @Override
    public double getMaxCheckInterval() {
        return this.eventDetector.getMaxCheckInterval();
    }

    /** {@inheritDoc} */
    @Override
    public int getMaxIterationCount() {
        return this.eventDetector.getMaxIterationCount();
    }

    /** {@inheritDoc} */
    @Override
    public int getSlopeSelection() {
        return this.eventDetector.getSlopeSelection();
    }

    /** {@inheritDoc} */
    @Override
    public EventDetector copy() {
        return new SignalPropagationWrapperDetector((AbstractSignalPropagationDetector) this.eventDetector.copy());
    }

    /**
     * Getter for the number of the occurred events.
     * 
     * @return the number of the occurred events
     */
    public int getNBOccurredEvents() {
        return this.emitterDatesMap.size();
    }

    /**
     * Getter for the Map of the emitter (keys) & receiver (values) dates of the occurred events.
     * <p>
     * For <i>N</i> events occurred, the Map describes <i>N</i> elements.
     * </p>
     * 
     * @return the Map of the emitter (keys) & receiver (values) dates
     */
    public Map<AbsoluteDate, AbsoluteDate> getEmitterDatesMap() {
        return this.emitterDatesMap;
    }

    /**
     * Getter for the list of the emitter dates of the occurred events.
     * <p>
     * For <i>N</i> events occurred, the List describes <i>N</i> elements.
     * </p>
     * 
     * @return the list of the emitter dates
     */
    public List<AbsoluteDate> getEmitterDatesList() {
        return new ArrayList<>(this.emitterDatesMap.keySet());
    }

    /**
     * Getter for the Map of the receiver (keys) & emitter (values) dates of the occurred events.
     * <p>
     * For <i>N</i> events occurred, the Map describes <i>N</i> elements.
     * </p>
     * 
     * @return the Map of the receiver (keys) & emitter (values) dates
     */
    public Map<AbsoluteDate, AbsoluteDate> getReceiverDatesMap() {
        return this.receiverDatesMap;
    }

    /**
     * Getter for the list of the receiver dates of the occurred events.
     * <p>
     * For <i>N</i> events occurred, the List describes <i>N</i> elements.
     * </p>
     * 
     * @return the list of the receiver dates
     */
    public List<AbsoluteDate> getReceiverDatesList() {
        return new ArrayList<>(this.receiverDatesMap.keySet());
    }

    /**
     * Returns the underlying wrapped detector.
     * 
     * @return the underlying wrapped detector
     */
    public AbstractSignalPropagationDetector getWrappedDetector() {
        return this.eventDetector;
    }

    /**
     * Specify if the datation choice corresponds to the emitter date or the receiver date.
     * 
     * @return the corresponding datation choice
     */
    public DatationChoice getDatationChoice() {
        return this.eventDetector.getDatationChoice();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {

        // Initialize the string builder
        final StringBuilder strBuilder = new StringBuilder();

        // Build the first row with the wrapper information
        final String className = this.getClass().getSimpleName() + ":";
        final int indentation = className.length();
        final String nSpaces = String.format("%" + indentation + "c", ' ');
        final int nbOccurredEvents = getNBOccurredEvents();
        final String wrappedEventName = this.eventDetector.getClass().getSimpleName();
        strBuilder.append(className);
        strBuilder.append(' ');
        strBuilder.append("[Wrapped event: " + wrappedEventName);
        strBuilder.append(" ; Occurred events: " + nbOccurredEvents + BRACKET_RIGHT);
        strBuilder.append('\n');

        // In the case some event occurred...
        if (nbOccurredEvents > 0) {
            // ... print the emitter & receiver dates information for each event
            int i = 1;
            for (final Entry<AbsoluteDate, AbsoluteDate> entry : this.emitterDatesMap.entrySet()) {
                strBuilder.append(nSpaces);
                strBuilder.append(' ');
                strBuilder.append("Event " + i++ + ": " + "[emitterDate: " + entry.getKey() + " ; receiverDate: "
                        + entry.getValue() + "]");
                strBuilder.append('\n');
            }
        }

        // Extract the built String
        return strBuilder.toString();
    }
}
