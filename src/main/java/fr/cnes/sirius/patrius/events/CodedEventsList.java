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
 * @history created 27/01/2012
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:---:11/04/2014:Quality assurance
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.events;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import fr.cnes.sirius.patrius.time.AbsoluteDate;

/**
 * <p>
 * This class represents a list of objects {@link CodedEvent}.<br>
 * One or more lists of coded events are created during propagation when {@link CodingEventDetector} is used, via the
 * {@link CodedEventsLogger#monitorDetector(CodingEventDetector) monitorDetector} method.
 * </p>
 * 
 * 
 * @concurrency not thread-safe
 * 
 * @concurrency.comment no thread-sharing use case was identified for this class, so thread safety is not required.
 *                      Though, some precautions are taken, as an example, the method getList() returns a copy of the
 *                      list and not directly the attribute list itself.
 * 
 * @see CodedEvent
 * 
 * @author Pierre Cardoso, Tiziana Sabatini
 * 
 * @version $Id$
 * 
 * @since 1.1
 * 
 */
public final class CodedEventsList {

    /** An ordered collection of {@link CodedEvent}. */
    private final SortedSet<CodedEvent> set;

    /**
     * Default constructor.
     */
    public CodedEventsList() {
        this.set = new TreeSet<>();
    }

    /**
     * Add a {@link CodedEvent} to the list.
     * 
     * @param codedEvent
     *        the element to add.
     */
    public void add(final CodedEvent codedEvent) {
        this.set.add(codedEvent);
    }

    /**
     * Remove a {@link CodedEvent} to the list.
     * 
     * @param codedEvent
     *        the element to remove.
     * @return true if the set contains the coded event that has to be removed.
     */
    public boolean remove(final CodedEvent codedEvent) {
        return this.set.remove(codedEvent);
    }

    /**
     * Get the full list of coded events. Or more accurately : get a copy of the inside {@link SortedSet} as a full
     * list.
     * 
     * @return a copy of the list of coded events.
     */
    public List<CodedEvent> getList() {
        final ArrayList<CodedEvent> copyLst;
        copyLst = new ArrayList<>(this.set);
        return copyLst;
    }

    /**
     * Finds one/more events in the list of {@link CodedEvent} following some criteria.<br>
     * When a comment and a date are available, the method looks for a specific event in the list, otherwise if only the
     * code is given as input, it looks for a list of events with the same code. If the event does not exist in the
     * list, returns the empty set.
     * 
     * @param code
     *        the code of the event to look for in the list
     * @param comment
     *        the comment of the event to look for in the list (it can be null)
     * @param date
     *        the date of the event to look for in the list (it can be null)
     * 
     * @return the list of events with the given code, comment and date.
     */
    public Set<CodedEvent> getEvents(final String code, final String comment, final AbsoluteDate date) {
        // finds the reference event:
        final Set<CodedEvent> events = new TreeSet<>();
        for (final CodedEvent current : this.getList()) {
            if (code.equals(current.getCode())) {
                // the event has the right code:
                if (comment == null) {
                    events.add(current);
                } else {
                    // a comment has been provided: we look for events with this comment:
                    if (comment.equals(current.getComment())) {
                        // the event has the right comment:
                        if (date == null) {
                            events.add(current);
                        } else {
                            // a date has been provided: we look for events with this date:
                            if (date.equals(current.getDate())) {
                                // the event has the right date:
                                events.add(current);
                            }
                        }
                    }
                }
            }
        }
        return events;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        // Copy the list in a thread-safe manner
        final ArrayList<CodedEvent> copyLst;
        copyLst = new ArrayList<>(this.set);
        // generic part
        sb.append("List<CodedEvent>[");
        boolean first = true;
        // append all values
        for (final CodedEvent cd : copyLst) {
            if (first) {
                sb.append(" ");
                first = false;
            } else {
                sb.append(" , ");
            }
            // append string
            sb.append(cd.toString());
        }
        sb.append(" ]");
        // return result
        return sb.toString();
    }
}
