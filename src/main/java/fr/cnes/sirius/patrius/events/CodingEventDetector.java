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
 * @history created 27/01/2012
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events;

import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;

/**
 * <p>
 * This interface represents an event detector that is able to build a {@link CodedEvent} object.
 * </p>
 * A {@link CodingEventDetector} can be used during propagation when we want to log the occurred events. <br>
 * These events are detected by the {@link EventDetector} that has been specified when creating the
 * {@link CodingEventDetector} </p>
 * 
 * @see EventDetector
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id$
 * 
 * @since 1.1
 * 
 */
public interface CodingEventDetector extends EventDetector {

    /**
     * Build a {@link CodedEvent} instance appropriate for the provided {@link SpacecraftState}.
     * 
     * @param s
     *        the current state information : date, kinematics, attitude
     * @param increasing
     *        if true, g function increases around event date.
     * @return the {@link CodedEvent}
     */
    CodedEvent buildCodedEvent(final SpacecraftState s,
                               final boolean increasing);

    /**
     * Build a delayed {@link CodedEvent} instance appropriate for the provided {@link SpacecraftState}.
     * This instance will have a delay with respect to the associated detected event.
     * 
     * @param s
     *        the current state information : date, kinematics, attitude
     * @param increasing
     *        if true, g function increases around event date.
     * @return the {@link CodedEvent}
     */
    CodedEvent buildDelayedCodedEvent(SpacecraftState s, boolean increasing);

    /**
     * Build a {@link CodedEvent} instance appropriate for the provided {@link SpacecraftState}.
     * This method will return an instance only if it is be the nth occurrence of the corresponding event,
     * otherwise it will return null. A delay can be applied to the event.
     * 
     * @param s
     *        the current state information : date, kinematics, attitude
     * @param increasing
     *        if true, g function increases around event date.
     * @return the {@link CodedEvent}
     */
    CodedEvent buildOccurrenceCodedEvent(SpacecraftState s, boolean increasing);

    /**
     * Get the sign of the g method that means "the phenomenon associated to
     * the event is active".<br>
     * This method has been implemented because of the inconsistency of the
     * sign of the g functions in the {@link EventDetector} classes in Orekit:
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

    /**
     * Gets a code indicating the type of event we want to log: DELAY when a delay
     * is associated to the logged events with respect to the detected events, N_OCCURRENCE
     * when we want to log the nth occurrence of the detected events, STANDARD when no delays
     * and no occurrence numbers are taken into consideration.
     * 
     * @return the type of event to log
     */
    String getEventType();
}
