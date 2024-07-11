/**
 * Copyright 2021-2021 CNES
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
 * VERSION:4.9:FA:FA-3125:10/05/2022:[PATRIUS] La sequence renvoyee par Sequences.unmodifiableXXX est modifiable 
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.7:DM:DM-2653:18/05/2021:generalisation des sequences et correction/refonte des sequences de segments 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.utils.legs;

import java.util.Collection;
import java.util.NavigableSet;
import java.util.Set;

import fr.cnes.sirius.patrius.time.TimeStamped;

/**
 * A {@link Collection} of {@link TimeStamped} objects.
 * <p>
 * This sequence is designed to sort objects by their date ({@link TimeStamped#getDate()}). Some might suggest that a
 * {@link NavigableSet} would have done the job, but a {@code NavigableSet} has some methods too complicated and too
 * ambiguous to implement (as {@code lower()}, {@code floor()}, …) with “time-sorted” objects.
 * <p>
 * Besides, as stated in the {@code SortedSet} interface documentation, the used {@code Comparator} must be
 * <i>“consistent with equals”</i>, and it cannot be achieved with such {@code TimeStamped} objects.
 *
 * @param <T>
 *        Any {@code TimeStamped} class.
 *
 * @see TimeStamped
 * @see LegsSequence
 *
 * @author Julien Anxionnat (CNES — DSO/DV/MP)
 * 
 * @since 4.7
 */
public interface TimeSequence<T extends TimeStamped> extends Collection<T> {

    /**
     * Returns the first element currently in this sequence.
     *
     * @return The first element currently in this sequence.
     */
    T first();

    /**
     * Returns the last element currently in this sequence.
     *
     * @return The last element currently in this sequence.
     */
    T last();

    /**
     * Returns the first element after the given date.
     * <p>
     * See {@link TimeSequence#next(TimeStamped)} for “strict” comparison.
     *
     * @param t
     *        A date from any {@link TimeStamped} object.
     *
     * @return The first element <i>starting after (or at)</i> the given date {@code t}.
     */
    T first(final TimeStamped t);

    /**
     * Returns the last element before the given date.
     * <p>
     * See {@link TimeSequence#previous(TimeStamped)} for “strict” comparison.
     *
     * @param t
     *        A date from any {@link TimeStamped} object.
     *
     * @return The last element <i>starting after (or at)</i> the given date {@code t}.
     */
    T last(final TimeStamped t);

    /**
     * Returns all simultaneous elements.
     *
     * @param t
     *        Time value to match. Not necessarily an element contained in the sequence.
     *
     * @return All elements at time {@code t}. If {@code t} is an element of the sequence, it’s among the returned
     *         elements.
     */
    Set<T> simultaneous(final TimeStamped t);

    /**
     * Returns the <i>strictly</i> previous element.
     *
     * @param t
     *        Any element of this sequence.
     *
     * @return The previous element of the given one, {@code null} if none. It’s a <i>strictly</i> previous: its date is
     *         strictly lower.
     *         Note also there may be simultaneous elements…
     *
     */
    T previous(final T t);

    /**
     * Returns the <i>strictly</i> next element.
     *
     * @param t
     *        Any element of this sequence.
     *
     * @return The previous element of the given one, {@code null} if none. It’s a <i>strictly</i> next: its date is
     *         strictly upper. Note
     *         also there may be simultaneous elements…
     */
    T next(final T t);

    /**
     * Returns a new sequence from the beginning to the given element.
     *
     * @param toT
     *        Any element of this sequence.
     *
     * @return A new {@code Sequence} object including all elements from the “beginning” to the given one (included).
     */
    TimeSequence<T> head(final T toT);

    /**
     * Returns a new sequence from the given element through the end.
     *
     * @param fromT
     *        Any element of this sequence.
     *
     * @return A new {@code Sequence} object including all elements from the given one (included) through the “end”.
     */
    TimeSequence<T> tail(final T fromT);

    /**
     * Returns a new sequence extracted.
     *
     * @param fromT
     *        Any element of this sequence.
     * @param toT
     *        Any element of this sequence.
     *
     * @return A new {@code Sequence} object including all elements from the given one {@code fromT} to the given one
     *         {@code toT} (both included).
     */
    TimeSequence<T> sub(final T fromT,
            final T toT);

    /** {@inheritDoc} */
    @Override
    default boolean contains(final Object o) {
        // We consider it’s better to consider objects identities rather than equality
        // to check if one is contained by the sequence:
        return this.stream().anyMatch(x -> x == o);
    }

    /** {@inheritDoc} */
    @Override
    default boolean containsAll(final Collection<?> c) {
        return c.stream().allMatch(x -> this.contains(x));
    }

    /**
     * @return A nice {@code String} representation.
     */
    default String toPrettyString() {
        final StringBuilder bloc = new StringBuilder();
        final String cr = "\n";
        bloc.append("Size: " + this.size() + cr);
        for (final T t : this) {
            bloc.append(t.getDate() + " — " + t.toString() + cr);
        }
        return bloc.toString();
    }

    /**
     * Returns a copy of the sequence.
     * @return a copy of the sequence.
     */
    TimeSequence<T> copy();
}
