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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3125:10/05/2022:[PATRIUS] La sequence renvoyee par Sequences.unmodifiableXXX est modifiable 
 * VERSION:4.9:DM:DM-3154:10/05/2022:[PATRIUS] Amelioration des methodes permettant l'extraction d'une sous-sequence 
 * VERSION:4.8:DM:DM-3040:15/11/2021:[PATRIUS]Reversement des evolutions de la branche patrius-for-lotus 
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.utils.legs;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.time.TimeStamped;

//CHECKSTYLE: stop MethodLength check
//Reason: anonymous implementations of sequence

/**
 * Collection of static method for handling sequences of legs {@link LegsSequence} ands time sequences
 * {@link TimeSequence}.
 *
 * @see LegsSequence
 * @see TimeSequence
 *
 * @author Emmanuel Bignon
 *
 * @since 4.8
 */
public final class Sequences {

    /**
     * Private constructor.
     */
    private Sequences() {
        // Nothing to do
    }

    /**
     * Build an empty time sequence.
     * @return an empty time sequence
     * @param <T> type of {@link TimeStamped}
     */
    public static <T extends TimeStamped> TimeSequence<T> emptyTimeSequence() {
        return new TimeSequence<T>() {

            /** {@inheritDoc} */
            @Override
            public int size() {
                return 0;
            }

            /** {@inheritDoc} */
            @Override
            public boolean isEmpty() {
                return true;
            }

            /** {@inheritDoc} */
            @Override
            public boolean contains(final Object o) {
                return false;
            }

            /** {@inheritDoc} */
            @Override
            public Iterator<T> iterator() {
                return new Iterator<T>() {
                    /** {@inheritDoc} */
                    @Override
                    public boolean hasNext() {
                        return false;
                    }

                    /** {@inheritDoc} */
                    @Override
                    public T next() {
                        throw new NoSuchElementException();
                    }
                };
            }

            /** {@inheritDoc} */
            @Override
            public Object[] toArray() {
                return new Object[0];
            }

            /** {@inheritDoc} */
            @SuppressWarnings("hiding")
            @Override
            public <T> T[] toArray(final T[] a) {
                if (a.length > 0) {
                    a[0] = null;
                }
                return a;
            }

            /** {@inheritDoc} */
            @Override
            public boolean add(final T e) {
                throw new UnsupportedOperationException();
            }

            /** {@inheritDoc} */
            @Override
            public boolean remove(final Object o) {
                throw new UnsupportedOperationException();
            }

            /** {@inheritDoc} */
            @Override
            public boolean containsAll(final Collection<?> c) {
                return c.isEmpty();
            }

            /** {@inheritDoc} */
            @Override
            public boolean addAll(final Collection<? extends T> c) {
                throw new UnsupportedOperationException();
            }

            /** {@inheritDoc} */
            @Override
            public boolean removeAll(final Collection<?> c) {
                throw new UnsupportedOperationException();
            }

            /** {@inheritDoc} */
            @Override
            public boolean retainAll(final Collection<?> c) {
                throw new UnsupportedOperationException();
            }

            /** {@inheritDoc} */
            @Override
            public void clear() {
                throw new UnsupportedOperationException();
            }

            /** {@inheritDoc} */
            @Override
            public T first() {
                return null;
            }

            /** {@inheritDoc} */
            @Override
            public T last() {
                return null;
            }

            /** {@inheritDoc} */
            @Override
            public T first(final TimeStamped t) {
                return null;
            }

            /** {@inheritDoc} */
            @Override
            public T last(final TimeStamped t) {
                return null;
            }

            /** {@inheritDoc} */
            @Override
            public Set<T> simultaneous(final TimeStamped t) {
                return new HashSet<>();
            }

            /** {@inheritDoc} */
            @Override
            public T previous(final T t) {
                return null;
            }

            /** {@inheritDoc} */
            @Override
            public T next(final T t) {
                return null;
            }

            /** {@inheritDoc} */
            @Override
            public TimeSequence<T> head(final T toT) {
                return emptyTimeSequence();
            }

            /** {@inheritDoc} */
            @Override
            public TimeSequence<T> tail(final T fromT) {
                return emptyTimeSequence();
            }

            /** {@inheritDoc} */
            @Override
            public TimeSequence<T> sub(final T fromT,
                    final T toT) {
                return emptyTimeSequence();
            }

            /** {@inheritDoc} */
            @Override
            public TimeSequence<T> copy() {
                return this;
            }
        };
    }

    /**
     * Build an empty legs sequence.
     * @return an empty legs sequence
     * @param <L> type of {@link Leg}
     */
    public static <L extends Leg> LegsSequence<L> emptyLegsSequence() {
        return new LegsSequence<L>() {

            /** {@inheritDoc} */
            @Override
            public Set<L> simultaneous(final TimeStamped t) {
                return new HashSet<>();
            }

            /** {@inheritDoc} */
            @Override
            public int size() {
                return 0;
            }

            /** {@inheritDoc} */
            @Override
            public boolean isEmpty() {
                return true;
            }

            /** {@inheritDoc} */
            @Override
            public boolean contains(final Object o) {
                return false;
            }

            /** {@inheritDoc} */
            @Override
            public Iterator<L> iterator() {
                return new Iterator<L>() {
                    /** {@inheritDoc} */
                    @Override
                    public boolean hasNext() {
                        return false;
                    }

                    /** {@inheritDoc} */
                    @Override
                    public L next() {
                        throw new NoSuchElementException();
                    }
                };
            }

            /** {@inheritDoc} */
            @Override
            public Object[] toArray() {
                return new Object[0];
            }

            /** {@inheritDoc} */
            @Override
            public <T> T[] toArray(final T[] a) {
                if (a.length > 0) {
                    a[0] = null;
                }
                return a;
            }

            /** {@inheritDoc} */
            @Override
            public boolean add(final L e) {
                throw new UnsupportedOperationException();
            }

            /** {@inheritDoc} */
            @Override
            public boolean remove(final Object o) {
                throw new UnsupportedOperationException();
            }

            /** {@inheritDoc} */
            @Override
            public boolean containsAll(final Collection<?> c) {
                return c.isEmpty();
            }

            /** {@inheritDoc} */
            @Override
            public boolean addAll(final Collection<? extends L> c) {
                throw new UnsupportedOperationException();
            }

            /** {@inheritDoc} */
            @Override
            public boolean removeAll(final Collection<?> c) {
                throw new UnsupportedOperationException();
            }

            /** {@inheritDoc} */
            @Override
            public boolean retainAll(final Collection<?> c) {
                throw new UnsupportedOperationException();
            }

            /** {@inheritDoc} */
            @Override
            public void clear() {
                throw new UnsupportedOperationException();
            }

            /** {@inheritDoc} */
            @Override
            public L current(final TimeStamped t) {
                return null;
            }

            /** {@inheritDoc} */
            @Override
            public L first() {
                return null;
            }

            /** {@inheritDoc} */
            @Override
            public L last() {
                return null;
            }

            /** {@inheritDoc} */
            @Override
            public L first(final TimeStamped t) {
                return null;
            }

            /** {@inheritDoc} */
            @Override
            public L last(final TimeStamped t) {
                return null;
            }

            /** {@inheritDoc} */
            @Override
            public Set<L> simultaneous(final L leg) {
                return new HashSet<>();
            }

            /** {@inheritDoc} */
            @Override
            public L previous(final L leg) {
                return null;
            }

            /** {@inheritDoc} */
            @Override
            public L next(final L leg) {
                return null;
            }

            /** {@inheritDoc} */
            @Override
            public LegsSequence<L> head(final L toLeg) {
                return emptyLegsSequence();
            }

            /** {@inheritDoc} */
            @Override
            public LegsSequence<L> tail(final L fromLeg) {
                return emptyLegsSequence();
            }

            /** {@inheritDoc} */
            @Override
            public LegsSequence<L> sub(final L fromLeg,
                    final L toLeg) {
                return emptyLegsSequence();
            }

            /** {@inheritDoc} */
            @Override
            public LegsSequence<L> sub(final AbsoluteDate fromT, final AbsoluteDate toT,
                    final boolean strict) {
                return emptyLegsSequence();
            }

            /** {@inheritDoc} */
            @Override
            public LegsSequence<L> sub(final AbsoluteDate fromT, final AbsoluteDate toT) {
                return emptyLegsSequence();
            }

            /** {@inheritDoc} */
            @Override
            public LegsSequence<L> sub(final AbsoluteDateInterval interval,
                    final boolean strict) {
                return emptyLegsSequence();
            }

            /** {@inheritDoc} */
            @Override
            public LegsSequence<L> sub(final AbsoluteDateInterval interval) {
                return emptyLegsSequence();
            }

            /** {@inheritDoc} */
            @Override
            public LegsSequence<L> head(final AbsoluteDate toT,
                    final boolean strict) {
                return emptyLegsSequence();
            }

            /** {@inheritDoc} */
            @Override
            public LegsSequence<L> head(final AbsoluteDate toT) {
                return emptyLegsSequence();
            }

            /** {@inheritDoc} */
            @Override
            public LegsSequence<L> tail(final AbsoluteDate fromT,
                    final boolean strict) {
                return emptyLegsSequence();
            }

            /** {@inheritDoc} */
            @Override
            public LegsSequence<L> tail(final AbsoluteDate fromT) {
                return emptyLegsSequence();
            }

            /** {@inheritDoc} */
            @Override
            public boolean isEmpty(final AbsoluteDate date,
                    final AbsoluteDate end) {
                return true;
            }

            /** {@inheritDoc} */
            @Override
            public AbsoluteDateInterval getTimeInterval() {
                return null;
            }

            /** {@inheritDoc} */
            @Override
            public LegsSequence<L> copy() {
                return this;
            }

            /** {@inheritDoc} */
            @Override
            public LegsSequence<L> copy(final AbsoluteDateInterval newInterval, final boolean strict) {
                return emptyLegsSequence();
            }

            /** {@inheritDoc} */
            @Override
            public LegsSequence<L> copy(final AbsoluteDateInterval newInterval) {
                return copy(newInterval, false);
            }
        };
    }

    /**
     * Build an unmodifiable time sequence. This sequence can be manipulated like any sequence but no element can be
     * added or removed.
     * @param sequence a time sequence
     * @return an unmodifiable time sequence
     * @param <T> type of {@link TimeStamped}
     */
    public static <T extends TimeStamped> TimeSequence<T> unmodifiableTimeSequence(final TimeSequence<T> sequence) {
        final TimeSequence<T> copiedSequence = sequence.copy();
        return new TimeSequence<T>() {

            /** {@inheritDoc} */
            @Override
            public int size() {
                return copiedSequence.size();
            }

            /** {@inheritDoc} */
            @Override
            public boolean isEmpty() {
                return copiedSequence.isEmpty();
            }

            /** {@inheritDoc} */
            @Override
            public boolean contains(final Object o) {
                return copiedSequence.contains(o);
            }

            /** {@inheritDoc} */
            @Override
            public Iterator<T> iterator() {
                return copiedSequence.iterator();
            }

            /** {@inheritDoc} */
            @Override
            public Object[] toArray() {
                return copiedSequence.toArray();
            }

            /** {@inheritDoc} */
            @SuppressWarnings("hiding")
            @Override
            public <T> T[] toArray(final T[] a) {
                return copiedSequence.toArray(a);
            }

            /** {@inheritDoc} */
            @Override
            public boolean add(final T e) {
                throw new UnsupportedOperationException();
            }

            /** {@inheritDoc} */
            @Override
            public boolean remove(final Object o) {
                throw new UnsupportedOperationException();
            }

            /** {@inheritDoc} */
            @Override
            public boolean containsAll(final Collection<?> c) {
                return copiedSequence.containsAll(c);
            }

            /** {@inheritDoc} */
            @Override
            public boolean addAll(final Collection<? extends T> c) {
                throw new UnsupportedOperationException();
            }

            /** {@inheritDoc} */
            @Override
            public boolean removeAll(final Collection<?> c) {
                throw new UnsupportedOperationException();
            }

            /** {@inheritDoc} */
            @Override
            public boolean retainAll(final Collection<?> c) {
                throw new UnsupportedOperationException();
            }

            /** {@inheritDoc} */
            @Override
            public void clear() {
                throw new UnsupportedOperationException();
            }

            /** {@inheritDoc} */
            @Override
            public T first() {
                return copiedSequence.first();
            }

            /** {@inheritDoc} */
            @Override
            public T last() {
                return copiedSequence.last();
            }

            /** {@inheritDoc} */
            @Override
            public T first(final TimeStamped t) {
                return copiedSequence.first(t);
            }

            /** {@inheritDoc} */
            @Override
            public T last(final TimeStamped t) {
                return copiedSequence.last(t);
            }

            /** {@inheritDoc} */
            @Override
            public Set<T> simultaneous(final TimeStamped t) {
                return copiedSequence.simultaneous(t);
            }

            /** {@inheritDoc} */
            @Override
            public T previous(final T t) {
                return copiedSequence.previous(t);
            }

            /** {@inheritDoc} */
            @Override
            public T next(final T t) {
                return copiedSequence.next(t);
            }

            /** {@inheritDoc} */
            @Override
            public TimeSequence<T> head(final T toT) {
                return copiedSequence.head(toT);
            }

            /** {@inheritDoc} */
            @Override
            public TimeSequence<T> tail(final T fromT) {
                return copiedSequence.tail(fromT);
            }

            /** {@inheritDoc} */
            @Override
            public TimeSequence<T> sub(final T fromT,
                    final T toT) {
                return copiedSequence.sub(fromT, toT);
            }

            /** {@inheritDoc} */
            @Override
            public TimeSequence<T> copy() {
                return copiedSequence.copy();
            }
        };
    }

    /**
     * Build an unmodifiable legs sequence. This sequence can be manipulated like any sequence but no element can be
     * added or removed.
     * @param sequence a legs sequence
     * @return an unmodifiable legs sequence
     * @param <L> type of {@link Leg}
     */
    public static <L extends Leg> LegsSequence<L> unmodifiableLegsSequence(final LegsSequence<L> sequence) {
        final LegsSequence<L> copiedSequence = sequence.copy();
        return new LegsSequence<L>() {

            /** {@inheritDoc} */
            @Override
            public Set<L> simultaneous(final TimeStamped t) {
                return copiedSequence.simultaneous(t);
            }

            /** {@inheritDoc} */
            @Override
            public int size() {
                return sequence.size();
            }

            /** {@inheritDoc} */
            @Override
            public boolean isEmpty() {
                return copiedSequence.isEmpty();
            }

            /** {@inheritDoc} */
            @Override
            public boolean contains(final Object o) {
                return copiedSequence.contains(o);
            }

            /** {@inheritDoc} */
            @Override
            public Iterator<L> iterator() {
                return copiedSequence.iterator();
            }

            /** {@inheritDoc} */
            @Override
            public Object[] toArray() {
                return copiedSequence.toArray();
            }

            /** {@inheritDoc} */
            @Override
            public <T> T[] toArray(final T[] a) {
                return copiedSequence.toArray(a);
            }

            /** {@inheritDoc} */
            @Override
            public boolean add(final L e) {
                throw new UnsupportedOperationException();
            }

            /** {@inheritDoc} */
            @Override
            public boolean remove(final Object o) {
                throw new UnsupportedOperationException();
            }

            /** {@inheritDoc} */
            @Override
            public boolean containsAll(final Collection<?> c) {
                return copiedSequence.containsAll(c);
            }

            /** {@inheritDoc} */
            @Override
            public boolean addAll(final Collection<? extends L> c) {
                throw new UnsupportedOperationException();
            }

            /** {@inheritDoc} */
            @Override
            public boolean removeAll(final Collection<?> c) {
                throw new UnsupportedOperationException();
            }

            /** {@inheritDoc} */
            @Override
            public boolean retainAll(final Collection<?> c) {
                throw new UnsupportedOperationException();
            }

            /** {@inheritDoc} */
            @Override
            public void clear() {
                throw new UnsupportedOperationException();
            }

            /** {@inheritDoc} */
            @Override
            public L current(final TimeStamped t) {
                return copiedSequence.current(t);
            }

            /** {@inheritDoc} */
            @Override
            public L first() {
                return copiedSequence.first();
            }

            /** {@inheritDoc} */
            @Override
            public L last() {
                return copiedSequence.last();
            }

            /** {@inheritDoc} */
            @Override
            public L first(final TimeStamped t) {
                return copiedSequence.first(t);
            }

            /** {@inheritDoc} */
            @Override
            public L last(final TimeStamped t) {
                return copiedSequence.last(t);
            }

            /** {@inheritDoc} */
            @Override
            public Set<L> simultaneous(final L leg) {
                return copiedSequence.simultaneous(leg);
            }

            /** {@inheritDoc} */
            @Override
            public L previous(final L leg) {
                return copiedSequence.previous(leg);
            }

            /** {@inheritDoc} */
            @Override
            public L next(final L leg) {
                return copiedSequence.next(leg);
            }

            /** {@inheritDoc} */
            @Override
            public LegsSequence<L> head(final L toLeg) {
                return copiedSequence.head(toLeg);
            }

            /** {@inheritDoc} */
            @Override
            public LegsSequence<L> tail(final L fromLeg) {
                return copiedSequence.tail(fromLeg);
            }

            /** {@inheritDoc} */
            @Override
            public LegsSequence<L> sub(final L fromLeg,
                    final L toLeg) {
                return copiedSequence.sub(fromLeg, toLeg);
            }

            /** {@inheritDoc} */
            @Override
            public LegsSequence<L> sub(final AbsoluteDate fromT, final AbsoluteDate toT,
                    final boolean strict) {
                return copiedSequence.sub(fromT, toT, strict);
            }

            /** {@inheritDoc} */
            @Override
            public LegsSequence<L> sub(final AbsoluteDate fromT, final AbsoluteDate toT) {
                return copiedSequence.sub(fromT, toT);
            }

            /** {@inheritDoc} */
            @Override
            public LegsSequence<L> sub(final AbsoluteDateInterval interval,
                    final boolean strict) {
                return copiedSequence.sub(interval, strict);
            }

            /** {@inheritDoc} */
            @Override
            public LegsSequence<L> sub(final AbsoluteDateInterval interval) {
                return copiedSequence.sub(interval);
            }

            /** {@inheritDoc} */
            @Override
            public LegsSequence<L> head(final AbsoluteDate toT, final boolean strict) {
                return copiedSequence.head(toT, strict);
            }

            /** {@inheritDoc} */
            @Override
            public LegsSequence<L> head(final AbsoluteDate toT) {
                return copiedSequence.head(toT);
            }

            /** {@inheritDoc} */
            @Override
            public LegsSequence<L> tail(final AbsoluteDate fromT, final boolean strict) {
                return copiedSequence.tail(fromT, strict);
            }

            /** {@inheritDoc} */
            @Override
            public LegsSequence<L> tail(final AbsoluteDate fromT) {
                return copiedSequence.tail(fromT);
            }

            /** {@inheritDoc} */
            @Override
            public boolean isEmpty(final AbsoluteDate date,
                    final AbsoluteDate end) {
                return copiedSequence.isEmpty();
            }

            /** {@inheritDoc} */
            @Override
            public AbsoluteDateInterval getTimeInterval() {
                return copiedSequence.getTimeInterval();
            }

            /** {@inheritDoc} */
            @Override
            public LegsSequence<L> copy() {
                return copiedSequence.copy();
            }

            /** {@inheritDoc} */
            @Override
            public LegsSequence<L> copy(final AbsoluteDateInterval newInterval, final boolean strict) {
                return copiedSequence.copy(newInterval, strict);
            }

            /** {@inheritDoc} */
            @Override
            public LegsSequence<L> copy(final AbsoluteDateInterval newInterval) {
                return copy(newInterval, false);
            }
        };
    }

    // CHECKSTYLE: resume MethodLength check
}
