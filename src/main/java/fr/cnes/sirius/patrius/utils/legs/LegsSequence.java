/**
 * Copyright 2011-2021 CNES
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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3125:10/05/2022:[PATRIUS] La sequence renvoyee par Sequences.unmodifiableXXX est modifiable 
 * VERSION:4.9:DM:DM-3154:10/05/2022:[PATRIUS] Amelioration des methodes permettant l'extraction d'une sous-sequence 
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.7:DM:DM-2859:18/05/2021:Optimisation du code ; SpacecraftState 
 * VERSION:4.7:DM:DM-2653:18/05/2021:generalisation des sequences et correction/refonte des sequences de segments 
 * VERSION:4.4:DM:DM-2208:04/10/2019:[PATRIUS] Ameliorations de Leg, LegsSequence et AbstractLegsSequence
 * VERSION:4.4:DM:DM-2097:04/10/2019:[PATRIUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1948:14/11/2018:new attitude leg sequence design
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.utils.legs;

import java.util.Collection;
import java.util.Set;

import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.time.TimeStamped;

/**
 * A {@link Collection} of {@link Leg} objects.
 * <p>
 * Previously, this {@code LegsSequence} has been a {@code NavigableSet}, but too much methods were
 * unsuitable for {@code Leg}s objects.
 *
 * @param <L>
 *        Any {@code Leg} class.
 *
 * @see Leg
 * @see TimeSequence
 * @see TimeStamped
 *
 * @author Julien Anxionnat (CNES — DSO/DV/MP)
 *
 * @since 4.7
 */
public interface LegsSequence<L extends Leg> extends TimeSequence<L> {

    /**
     * Returns the current leg at the given date.
     *
     * @param t
     *        A date from any {@link TimeStamped} object.
     *
     * @return The current {@code Leg} at the {@code t} date, or {@code null} if none.
     */
    L current(final TimeStamped t);

    /**
     * @return The first {@code Leg}, {@code null} if none.
     */
    @Override
    L first();

    /**
     * @return The last {@code Leg}, {@code null} if none.
     */
    @Override
    L last();

    /**
     * @return The first {@code Leg} <i>starting after (or at)</i> the given date {@code t}.
     */
    @Override
    L first(final TimeStamped t);

    /**
     * @return The last {@code Leg} <i>finishing before (or at)</i> the given date {@code t}.
     */
    @Override
    L last(final TimeStamped t);

    /**
     * Returns leg(s) at the same date.
     *
     * @param leg
     *        A {@code Leg} of this sequence.
     *
     * @return A {@code Set} of legs starting at the same date than the given {@code leg}. At least,
     *         that {@code Set} contains the given {@code leg}.
     *
     * @throws IllegalArgumentException
     *         If {@code leg} is not in the sequence.
     */
    Set<L> simultaneous(final L leg);

    /**
     * @return The previous {@code Leg} of the given {@code leg}, {@code null} if none.
     */
    @Override
    L previous(final L leg);

    /**
     * @return The next {@code Leg} of the given {@code leg}, {@code null} if none.
     */
    @Override
    L next(final L leg);

    /** {@inheritDoc} */
    @Override
    LegsSequence<L> head(final L toLeg);

    /** {@inheritDoc} */
    @Override
    LegsSequence<L> tail(final L fromLeg);

    /** {@inheritDoc} */
    @Override
    LegsSequence<L> sub(final L fromLeg, final L toLeg);

    /**
     * Returns a new sequence extracted.
     *
     * @param fromT
     *        Any element of this sequence.
     * @param toT
     *        Any element of this sequence.
     * @param strict
     *        true if boundaries shall not be included in the extracted sequence, false otherwise.
     *
     * @return A new {@code Sequence} object including all elements from the given one {@code fromT}
     *         to the given one.
     *         Elements exactly on the interval boundaries are included only if {@code strict} =
     *         false.
     */
    LegsSequence<L> sub(final AbsoluteDate fromT, AbsoluteDate toT, boolean strict);

    /**
     * Returns a new sequence extracted.
     *
     * @param fromT
     *        Any element of this sequence.
     * @param toT
     *        Any element of this sequence.
     *
     * @return A new {@code Sequence} object including all elements from the given one {@code fromT}
     *         to the given one {@code toT} (both included).
     */
    default LegsSequence<L> sub(final AbsoluteDate fromT, final AbsoluteDate toT) {
        return sub(fromT, toT, false);
    }

    /**
     * Returns a new sequence extracted.
     *
     * @param interval
     *        interval.
     * @param strict
     *        true if boundaries shall not be included in the extracted sequence, false otherwise.
     *
     * @return A new {@code Sequence} object including all elements included in the {@code interval}
     *         .
     *         Elements exactly on the interval boundaries are included only if {@code strict} =
     *         false.
     */
    LegsSequence<L> sub(final AbsoluteDateInterval interval, final boolean strict);

    /**
     * Returns a new sequence extracted.
     *
     * @param interval
     *        interval.
     *
     * @return A new {@code Sequence} object including all elements included in the {@code interval}
     *         .
     *         Elements exactly on the interval boundaries are included.
     */
    default LegsSequence<L> sub(final AbsoluteDateInterval interval) {
        return sub(interval, false);
    }

    /**
     * Returns a new sequence from the beginning to the given element.
     *
     * @param toT
     *        Any element of this sequence.
     * @param strict
     *        true if boundary shall not be included in the extracted sequence, false otherwise.
     *
     * @return A new {@code Sequence} object including all elements from the “beginning” to the
     *         given one (included only
     *         if {@code strict} = false).
     */
    LegsSequence<L> head(final AbsoluteDate toT, final boolean strict);

    /**
     * Returns a new sequence from the beginning to the given element.
     *
     * @param toT
     *        Any element of this sequence.
     *
     * @return A new {@code Sequence} object including all elements from the “beginning” to the
     *         given one (included).
     */
    default LegsSequence<L> head(final AbsoluteDate toT) {
        return head(toT, false);
    }

    /**
     * Returns a new sequence from the given element to the end of the sequence.
     *
     * @param fromT
     *        Any element of this sequence.
     * @param strict
     *        true if boundary shall not be included in the extracted sequence, false otherwise.
     *
     * @return A new {@code Sequence} object including all elements from the given one (included
     *         only
     *         if {@code strict} = false) to the “end” of the sequence.
     */
    LegsSequence<L> tail(final AbsoluteDate fromT, final boolean strict);

    /**
     * Returns a new sequence from the given element to the end of the sequence.
     *
     * @param fromT
     *        Any element of this sequence.
     *
     * @return A new {@code Sequence} object including all elements from the given one (included) to
     *         the “end” of the
     *         sequence.
     */
    default LegsSequence<L> tail(final AbsoluteDate fromT) {
        return tail(fromT, false);
    }

    /**
     * Checks whether the sequence is free on the given interval or not.
     *
     * @param date
     *        The “beginning” date.
     * @param end
     *        The “end” date.
     *
     * @return {@code true} if this sequence is completely free during the given time interval.
     */
    boolean isEmpty(final AbsoluteDate date, final AbsoluteDate end);

    /**
     * @return A nice {@code String} representation.
     */
    @Override
    default String toPrettyString() {
        if (isEmpty()) {
            return "Empty legs sequence";
        } else {
            final String cr = "\n";
            final StringBuilder bloc = new StringBuilder();
            bloc.append("Size : " + size() + cr);
            for (final Leg leg : this) {
                bloc.append(leg.toPrettyString() + cr);
            }
            return bloc.toString();
        }
    }

    /**
     * Returns the time interval of the legs sequence.
     * @return the time interval of the legs sequence.
     */
    AbsoluteDateInterval getTimeInterval();

    /** {@inheritDoc} */
    @Override
    LegsSequence<L> copy();

    /**
     * Creates a new legs sequence from this one.
     *
     * @param newInterval
     *        The time interval of the legs sequence to create
     * @param strict
     *        true if boundaries shall not be included in the new sequence, false otherwise.
     *
     * @return A new {@code LegsSequence} valid on provided interval
     *
     * @throws IllegalArgumentException
     *         If the given {@code newInterval} is problematic (too long, too short, whatever)
     */
    LegsSequence<L> copy(final AbsoluteDateInterval newInterval, boolean strict);

    /**
     * Creates a new legs sequence from this one.
     *
     * @param newInterval
     *        The time interval of the legs sequence to create
     *
     * @return A new {@code LegsSequence} valid on provided interval. Boundaries are not included in
     *         the new sequence.
     *
     * @throws IllegalArgumentException
     *         If the given {@code newInterval} is problematic (too long, too short, whatever)
     */
    default LegsSequence<L> copy(final AbsoluteDateInterval newInterval) {
        return copy(newInterval, false);
    }
}
