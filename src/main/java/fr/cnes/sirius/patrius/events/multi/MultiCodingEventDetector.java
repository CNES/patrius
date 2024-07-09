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
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:300:18/03/2015:Creation multi propagator
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events.multi;

import java.util.Map;

import fr.cnes.sirius.patrius.events.CodedEvent;
import fr.cnes.sirius.patrius.events.Phenomenon;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.multi.MultiEventDetector;

/**
 * <p>
 * This class is copied from {@link fr.cnes.sirius.patrius.events.CodingEventDetector} and adapted to multi propagation.
 * </p>
 * <p>
 * This interface represents a multi event detector that is able to build a {@link CodedEvent} object.
 * </p>
 * <p>
 * A {@link MultiCodingEventDetector} can be used during propagation when we want to log the occurred events. <br>
 * These events are detected by the {@link MultiEventDetector} that has been specified when creating the
 * {@link MultiCodingEventDetector}.
 * </p>
 * 
 * @see MultiEventDetector
 * 
 * @author maggioranic
 * 
 * @version $Id$
 * 
 * @since 3.0
 * 
 */
public interface MultiCodingEventDetector extends MultiEventDetector {

    /**
     * Build a {@link CodedEvent} instance appropriate for the provided map of {@link SpacecraftState}.
     * 
     * @param s
     *        the current states information
     * @param increasing
     *        if true, g function increases around event date.
     * @return the {@link CodedEvent}
     */
    CodedEvent buildCodedEvent(final Map<String, SpacecraftState> s,
                               final boolean increasing);

    /**
     * Build a delayed {@link CodedEvent} instance appropriate for the provided map of {@link SpacecraftState}.
     * This instance will have a delay with respect to the associated detected event.
     * 
     * @param s
     *        the current states information
     * @param increasing
     *        if true, g function increases around event date.
     * @return the {@link CodedEvent}
     */
    CodedEvent buildDelayedCodedEvent(Map<String, SpacecraftState> s, boolean increasing);

    /**
     * Build a {@link CodedEvent} instance appropriate for the provided map of {@link SpacecraftState}.
     * This method will return an instance only if it is be the nth occurrence of the corresponding event,
     * otherwise it will return null. A delay can be applied to the event.
     * 
     * @param s
     *        the current states information
     * @param increasing
     *        if true, g function increases around event date.
     * @return the {@link CodedEvent}
     */
    CodedEvent buildOccurrenceCodedEvent(Map<String, SpacecraftState> s, boolean increasing);

    /**
     * Gets a code indicating the type of event we want to log: DELAY when a delay
     * is associated to the logged events with respect to the detected events, N_OCCURRENCE
     * when we want to log the nth occurrence of the detected events, STANDARD when no delays
     * and no occurrence numbers are taken into consideration.
     * 
     * @return the type of event to log
     */
    String getEventType();

    /**
     * Get the sign of the g method that means "the phenomenon associated to
     * the event is active".<br>
     * This method has been implemented because of the inconsistency of the
     * sign of the g functions in the {@link MultiEventDetector} classes in Patrius:
     * for some events, g is positive when its associated phenomenon is active,
     * and for others, g is positive when its phenomenon is not active.<br>
     * WARNING : If Phenomena are not supported, the behavior of this method
     * is undefined.
     * 
     * @return true for positive, false for negative.
     */
    boolean positiveSignMeansActive();

    /**
     * If the implementation supports a {@link Phenomenon}, provides a code for
     * the phenomenon associated to the event. If not, returns null.
     * 
     * @return either a code, or null if Phenomena are not supported.
     */
    String getPhenomenonCode();
}
