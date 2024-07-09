/**
 * Copyright 2011-2017 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * HISTORY
 * VERSION:4.7:DM:DM-2651:18/05/2021: legere amelioration de la Javadoc de ChronologicalComparator 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
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
 */
package fr.cnes.sirius.patrius.time;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Comparator for {@link TimeStamped} instance.
 * <p>
 * {@code null} is not an accepted value for generic {@link ChronologicalComparator}. In order to handle {@code null}
 * values, a null-compliant comparator should be built with one of the following methods:
 * <ul>
 * <li>{@code Comparator.nullsFirst(new ChronologicalComparator())}: {@code null} values being set first</li>
 * <li>{@code Comparator.nullsLast(new ChronologicalComparator())}: {@code null} values being set last</li>
 * </ul>
 * </p>
 * 
 * @see AbsoluteDate
 * @see TimeStamped
 * @author Luc Maisonobe
 */
public class ChronologicalComparator implements Comparator<TimeStamped>, Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 3092980292741000025L;

    /**
     * Compare two time-stamped instances.
     * 
     * @param timeStamped1
     *        first time-stamped instance
     * @param timeStamped2
     *        second time-stamped instance
     * @return a negative integer, zero, or a positive integer as the first
     *         instance is before, simultaneous, or after the second one.
     */
    @Override
    public int compare(final TimeStamped timeStamped1,
                       final TimeStamped timeStamped2) {
        return timeStamped1.getDate().compareTo(timeStamped2.getDate());
    }
}
