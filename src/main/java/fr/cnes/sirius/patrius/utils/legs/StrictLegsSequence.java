package fr.cnes.sirius.patrius.utils.legs;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.time.ChronologicalComparator;
import fr.cnes.sirius.patrius.time.TimeStamped;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * A {@link LegsSequence} which does not accept simultaneous or overlapping legs.
 * Legs are considered to have closed boundaries.
 *
 * @param <L>
 *        Any {@code Leg} class.
 *
 * @see Leg
 * @see LegsSequence
 *
 * @author Julien Anxionnat (CNES — DSO/DV/MP)
* HISTORY
* VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
* VERSION:4.8:DM:DM-2992:15/11/2021:[PATRIUS] Possibilite d'interpoler l'attitude en ignorant les rotations rate 
* VERSION:4.7:DM:DM-2859:18/05/2021:Optimisation du code ; SpacecraftState 
* VERSION:4.7:DM:DM-2872:18/05/2021:Calcul de l'accélération dans la classe QuaternionPolynomialProfile 
* VERSION:4.7:DM:DM-2653:18/05/2021:generalisation des sequences et correction/refonte des sequences de segments 
* END-HISTORY
 */
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.NullAssignment"})
// Reason : false positive, performance
public class StrictLegsSequence<L extends Leg> implements LegsSequence<L> {

    /**
     * Inner {@code NavigableSet}.
     */
    private final NavigableSet<TimeStamped> set;

    /**
     * A strict comparator for legs.
     * <p>
     * Since such a {@code StrictLegsSequence} does not accept simultaneous or overlapping legs, a {@code Comparator} as
     * simple as a {@code ChronologicalComparator} may be used.
     */
    private final Comparator<TimeStamped> comparator = new ChronologicalComparator();

    /**
     * Create a new empty sequence.
     */
    public StrictLegsSequence() {
        this.set = new TreeSet<>(this.comparator);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public L current(final TimeStamped t) {
        // Get the “previous or equal” leg:
        final L leg = (L) this.set.floor(t);
        // If it contains the date, it’s the current one:
        return (leg != null) && leg.getTimeInterval().contains(t.getDate()) ? leg : null;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public L first() {
        return this.isEmpty() ? null : (L) this.set.first();
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public L last() {
        return this.isEmpty() ? null : (L) this.set.last();
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public L first(final TimeStamped t) {
        if (t == null) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.LEG_CANNOT_BE_NULL);
        }
        // Return the leg whose date is the least greatest or equal to `t` date:
        return (L) this.set.ceiling(t);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public L last(final TimeStamped t) {
        if (t == null) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.LEG_CANNOT_BE_NULL);
        }
        // Result:
        final L lastLeg;

        // Get the leg whose date is the greatest strictly less than `t` date:
        final L lowerLeg = (L) this.set.lower(t);

        if (lowerLeg == null) {
            lastLeg = null;

        } else if (lowerLeg.getEnd().compareTo(t.getDate()) <= 0) {
            // If `leg` end date is before (or at) `t` date, it’s the last one:
            lastLeg = lowerLeg;

        } else {
            // If `leg` end date is strictly after `t` date,
            // / try to get the previous (which may be `null`):
            lastLeg = (L) this.set.lower(lowerLeg);
        }

        return lastLeg;
    }

    /**
     * Such a {@code StrictLegsSequence} <strong>cannot have simultaneous legs!</strong>
     *
     * @return Since a {@code StrictLegsSequence} cannot have simultaneous legs, the returned {@code Set} is a
     *         {@code HashSet} containing
     *         just the given {@code leg}.
     *
     * @see LegsSequence#simultaneous(Leg)
     *
     * @throws IllegalArgumentException
     *         If {@code leg} is null or sequence is not empty and does not contain leg.
     */
    @Override
    public Set<L> simultaneous(final L leg) {
        if (leg == null) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.LEG_CANNOT_BE_NULL);
        }
        if (isEmpty()) {
            return new HashSet<>(0);
        }
        if (!this.contains(leg)) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.LEG_NOT_IN_SEQUENCE);
        }
        final Set<L> result = new HashSet<>(1);
        result.add(leg);
        return result;
    }

    /**
     * Such a {@code StrictLegsSequence} cannot have simultaneous legs.
     *
     * <p>Warning: legs are considered to have closed boundaries. For instance a sequence ]a, b[ U ]b, c[ and t = b
     * will return the second leg although b is not in the sequence.</p>
     * 
     * @return The leg at time {@code t}, if any. (If {@code t} is a leg of the sequence, it’s the returned one.)
     *
     * @see TimeSequence#simultaneous(TimeStamped)
     */
    @SuppressWarnings("unchecked")
    @Override
    public Set<L> simultaneous(final TimeStamped t) {
        if (t == null) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.LEG_CANNOT_BE_NULL);
        }

        // By design, there are no simultaneous legs!
        final Set<L> result = new HashSet<>(1);

        // Get the “floor” leg:
        final L leg = (L) this.set.floor(t);
        if ((leg != null) && t.getDate().compareTo(leg.getDate()) == 0) {
            result.add(leg);
        }

        return result;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public L previous(final L leg) {
        if (leg == null) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.LEG_CANNOT_BE_NULL);
        }
        if (isEmpty()) {
            return null;
        }
        if (!this.contains(leg)) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.LEG_NOT_IN_SEQUENCE);
        }
        return (L) this.set.lower(leg);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public L next(final L leg) {
        if (leg == null) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.LEG_CANNOT_BE_NULL);
        }
        if (isEmpty()) {
            return null;
        }
        if (!this.contains(leg)) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.LEG_NOT_IN_SEQUENCE);
        }
        return (L) this.set.higher(leg);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public LegsSequence<L> head(final L toLeg) {
        if (toLeg == null) {
            // Null case: exception
            throw PatriusException.createIllegalArgumentException(PatriusMessages.LEG_CANNOT_BE_NULL);
        }
        if (isEmpty()) {
            // Empty case
            return new StrictLegsSequence<>();
        }
        if (!this.contains(toLeg)) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.LEG_NOT_IN_SEQUENCE);
        }
        // Standard case
        final SortedSet<TimeStamped> headSet = this.set.headSet(toLeg, true);
        final StrictLegsSequence<L> headSequence = new StrictLegsSequence<>();
        headSet.forEach(t -> headSequence.add((L) t));
        return headSequence;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public LegsSequence<L> head(final AbsoluteDate toT, final boolean strict) {
        if (toT == null) {
            // Null case: exception
            throw PatriusException.createIllegalArgumentException(PatriusMessages.LEG_CANNOT_BE_NULL);
        }
        if (isEmpty()) {
            // Empty case
            return new StrictLegsSequence<>();
        }
        // Standard case
        final SortedSet<TimeStamped> headSet = this.set.headSet(toT, !strict);
        final StrictLegsSequence<L> headSequence = new StrictLegsSequence<>();
        headSet.forEach(t -> headSequence.add((L) t));
        return headSequence;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public LegsSequence<L> tail(final L fromLeg) {
        if (fromLeg == null) {
            // Null case: exception
            throw PatriusException.createIllegalArgumentException(PatriusMessages.LEG_CANNOT_BE_NULL);
        }
        if (isEmpty()) {
            // Empty case
            return new StrictLegsSequence<>();
        }
        if (!this.contains(fromLeg)) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.LEG_NOT_IN_SEQUENCE);
        }
        // Standard case
        final SortedSet<TimeStamped> tailSet = this.set.tailSet(fromLeg, true);
        final StrictLegsSequence<L> tailSequence = new StrictLegsSequence<>();
        tailSet.forEach(t -> tailSequence.add((L) t));
        return tailSequence;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public LegsSequence<L> tail(final AbsoluteDate fromT, final boolean strict) {
        if (fromT == null) {
            // Null case: exception
            throw PatriusException.createIllegalArgumentException(PatriusMessages.LEG_CANNOT_BE_NULL);
        }
        if (isEmpty()) {
            // Empty case
            return new StrictLegsSequence<>();
        }
        // Standard case
        final SortedSet<TimeStamped> tailSet = this.set.tailSet(fromT, !strict);
        final StrictLegsSequence<L> tailSequence = new StrictLegsSequence<>();
        tailSet.forEach(t -> tailSequence.add((L) t));
        return tailSequence;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public LegsSequence<L> sub(final L fromLeg, final L toLeg) {
        if (fromLeg == null || toLeg == null) {
            // Null case: exception
            throw PatriusException.createIllegalArgumentException(PatriusMessages.LEG_CANNOT_BE_NULL);
        }
        if (isEmpty()) {
            // Empty case
            return new StrictLegsSequence<>();
        }
        if (!this.contains(fromLeg) || !this.contains(toLeg)) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.LEG_NOT_IN_SEQUENCE);
        }
        // Standard case
        final SortedSet<TimeStamped> subSet = this.set.subSet(fromLeg, true, toLeg, true);
        final StrictLegsSequence<L> subSequence = new StrictLegsSequence<>();
        subSet.forEach(t -> subSequence.add((L) t));
        return subSequence;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public LegsSequence<L> sub(final AbsoluteDate fromT,
            final AbsoluteDate toT,
            final boolean strict) {
        if (fromT == null || toT == null) {
            // Null case: exception
            throw PatriusException.createIllegalArgumentException(PatriusMessages.LEG_CANNOT_BE_NULL);
        }
        if (isEmpty()) {
            // Empty case
            return new StrictLegsSequence<>();
        }
        // Standard case
        final SortedSet<TimeStamped> subSet = this.set.subSet(fromT, !strict, toT, !strict);
        final StrictLegsSequence<L> subSequence = new StrictLegsSequence<>();
        subSet.forEach(t -> subSequence.add((L) t));
        return subSequence;
    }

    /** {@inheritDoc} */
    @Override
    public LegsSequence<L> sub(final AbsoluteDateInterval interval, final boolean strict) {
        return sub(interval.getLowerData(), interval.getUpperData(), strict);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isEmpty(final AbsoluteDate date, final AbsoluteDate end) {
        if (date == null || end == null) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.NULL_NOT_ALLOWED);
        }
        // Test if the “previous” leg overlaps the given interval:
        final Leg lower = (Leg) this.set.floor(date);
        if ((lower != null) && lower.getEnd().compareTo(date) > 0) {
            return false;
        }

        // Test if the “next” leg overlaps the given interval:
        final Leg higher = (Leg) this.set.higher(date);
        return !((higher != null) && higher.getDate().compareTo(end) < 0);
    }

    /**
     * Check provided leg.
     * @param leg
     *        A candidate to be added to the sequence.
     *
     * @throws IllegalArgumentException
     *         If the {@code leg} cannot be added to the sequence.
     */
    private void check(final L leg) {
        if (leg == null) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.LEG_CANNOT_BE_NULL);
        }
        if (this.contains(leg)) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.LEG_ALREADY_IN_SEQUENCE);
        }
        if (!this.isEmpty(leg.getDate(), leg.getEnd())) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.SEQUENCE_MUST_BE_EMPTY);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean add(final L leg) {
        this.check(leg);
        return this.set.add(leg);
    }

    /**
     * This {@code addAll(…)} method is not supported, since it’s an optional operation as stated in
     * {@link Collection#addAll(Collection)}.
     */
    @Override
    public boolean addAll(final Collection<? extends L> legs) {
        throw new UnsupportedOperationException("the addAll operation is not supported by this sequence");
    }

    /** {@inheritDoc} */
    @Override
    public void clear() {
        this.set.clear();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isEmpty() {
        return this.set.isEmpty();
    }

    /** {@inheritDoc} */
    @Override
    public Iterator<L> iterator() {
        final Iterator<TimeStamped> iteratorT = this.set.iterator();
        return new Iterator<L>() {
            /** {@inheritDoc} */
            @Override
            public boolean hasNext() {
                return iteratorT.hasNext();
            }

            /** {@inheritDoc} */
            @SuppressWarnings("unchecked")
            @Override
            public L next() {
                return (L) iteratorT.next();
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    public boolean remove(final Object o) {
        if (o == null) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.LEG_CANNOT_BE_NULL);
        }
        if (this.contains(o)) {
            return this.set.remove(o);
        } else {
            return false;
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean removeAll(final Collection<?> c) {
        if (c == null) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.NULL_NOT_ALLOWED);
        }
        boolean res = true;
        for(final Object leg : c) {
            res &= remove(leg);
        }
        return res;
    }

    /**
     * This {@code addAll(…)} method is not supported, since it’s an optional operation as stated in
     * {@link Collection#retainAll(Collection)}.
     */
    @Override
    public boolean retainAll(final Collection<?> c) {
        throw new UnsupportedOperationException("the retainAll operation is not supported by this sequence");
    }

    /** {@inheritDoc} */
    @Override
    public int size() {
        return this.set.size();
    }

    /** {@inheritDoc} */
    @Override
    public Object[] toArray() {
        return this.set.toArray();
    }

    /** {@inheritDoc} */
    @Override
    public <T> T[] toArray(final T[] a) {
        return this.set.toArray(a);
    }

    /**
     * {@inheritDoc}
     * <p>Null is returned if the sequence is empty.</p>
     * <p>
     * Warning: in case of sequences with holes, the sequence in the returned interval will not contain continuous data.
     * </p>
     */
    @Override
    public AbsoluteDateInterval getTimeInterval() {
        if (isEmpty()) {
            return null;
        } else {
            return new AbsoluteDateInterval(first().getDate(), last().getEnd());
        }
    }

}