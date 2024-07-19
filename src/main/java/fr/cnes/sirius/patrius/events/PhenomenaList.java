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
 * VERSION::DM:922:15/11/2017: Serializable interface implementation
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;

/**
 * <p>
 * This class represents a list of objects {@link Phenomenon}.<br>
 * One or more lists of phenomena are created during propagation when {@link CodingEventDetector} is used, via the
 * {@link CodedEventsLogger#monitorDetector(CodingEventDetector) monitorDetector} method.
 * </p>
 * 
 * @concurrency not thread-safe
 * 
 * @concurrency.comment no thread-sharing use case was identified for this class, so thread safety is not required.
 *                      Though, some precautions are taken, as an example, the method getList() returns a copy of the
 *                      list and not directly the attribute list itself.
 * 
 * @see Phenomenon
 * 
 * @author Pierre Cardoso, Tiziana Sabatini
 * 
 * @version $Id$
 * 
 * @since 1.1
 * 
 */
public final class PhenomenaList implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 6749611289379972334L;
    /** An ordered collection of {@link Phenomenon}. */
    private final SortedSet<Phenomenon> set;

    /**
     * Default constructor.
     */
    public PhenomenaList() {
        this.set = new TreeSet<>();
    }

    /**
     * Add a {@link Phenomenon} to the list.
     * 
     * @param phenomenon
     *        the {@link Phenomenon} to add.
     */
    public void add(final Phenomenon phenomenon) {
        this.set.add(phenomenon);
    }

    /**
     * Remove a {@link Phenomenon} to the list.
     * 
     * @param phenomenon
     *        the {@link Phenomenon} to add.
     * @return true if the set contains the phenomenon that has to be removed
     */
    public boolean remove(final Phenomenon phenomenon) {
        return this.set.remove(phenomenon);
    }

    /**
     * Get the full list of {@link Phenomenon}. Or more accurately : get a copy of the inside {@link SortedSet} as a
     * full list.
     * 
     * @return a copy of the list of {@link Phenomenon}.
     */
    public List<Phenomenon> getList() {
        final ArrayList<Phenomenon> copyLst;
        copyLst = new ArrayList<>(this.set);
        return copyLst;
    }

    /**
     * Finds one/more events in the list of {@link Phenomenon} following some criteria.<br>
     * When a comment and a time interval are available, the method looks for a specific phenomenon in the list,
     * otherwise if only the code is given as input, it looks for a list of phenomena with the same code. If the
     * phenomenon does not exist in the list, returns the empty set.
     * 
     * @param code
     *        the code of the phenomenon to look for in the list
     * @param comment
     *        the comment of the phenomenon to look for in the list (it can be null)
     * @param interval
     *        the time interval of the phenomenon to look for in the list (it can be null)
     * 
     * @return the list of phenomena with the given code, comment and interval.
     */
    public Set<Phenomenon> getPhenomena(final String code, final String comment, final AbsoluteDateInterval interval) {
        // finds the reference event:
        final Set<Phenomenon> phens = new TreeSet<>();
        for (final Phenomenon current : this.getList()) {
            if (code.equals(current.getCode())) {
                // the phenomenon has the right code:
                if (comment == null) {
                    phens.add(current);
                } else {
                    // a comment has been provided: we look for events with this comment:
                    if (comment.equals(current.getComment())) {
                        // the phenomenon has the right comment:
                        if (interval == null) {
                            phens.add(current);
                        } else {
                            // an interval has been provided: we look for phenomena with this interval:
                            if (interval.equals(current.getTimespan())) {
                                // the phenomenon has the right time interval:
                                phens.add(current);
                            }
                        }
                    }
                }
            }
        }
        return phens;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        // Copy the list in a thread-safe manner
        final ArrayList<Phenomenon> copyLst;
        copyLst = new ArrayList<>(this.set);
        // generic string
        sb.append("List<Phenomenon>[");
        boolean first = true;
        for (final Phenomenon ph : copyLst) {
            if (first) {
                sb.append(" ");
                first = false;
            } else {
                sb.append(" , ");
            }
            // append value
            sb.append(ph.toString());
        }
        sb.append(" ]");
        // return result
        return sb.toString();
    }
}
