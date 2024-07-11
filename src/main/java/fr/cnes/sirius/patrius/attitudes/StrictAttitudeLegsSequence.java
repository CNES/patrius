/**
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3154:10/05/2022:[PATRIUS] Amelioration des methodes permettant l'extraction d'une sous-sequence 
 * VERSION:4.9:DM:DM-3149:10/05/2022:[PATRIUS] Optimisation des reperes interplanetaires 
 * VERSION:4.7:DM:DM-2767:18/05/2021:Evolutions et corrections diverses 
 * VERSION:4.7:DM:DM-2653:18/05/2021:generalisation des sequences et correction/refonte des
 * sequences de segments 
 * END-HISTORY
 */
/*
 */
/*
 */
/*
 */
/*
 */
/*
 */
/*
 */
/*
 */
/*
 */
package fr.cnes.sirius.patrius.attitudes;

import java.util.SortedSet;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.time.TimeStamped;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.legs.StrictLegsSequence;

/**
 * A “base” implementation of an <i>attitude legs sequence</i>.
 * This implementation has strict legs which means legs cannot be simultaneous or overlap and are
 * strictly ordered by
 * starting date.
 *
 * @param <L>
 *        Any {@code AttitudeLeg} class.
 *
 * @author Julien Anxionnat (CNES — DSO/DV/MP)
 *
 * @see AttitudeLeg
 *
 * @since 4.7
 */
@SuppressWarnings( "PMD.AvoidDuplicateLiterals")
//Reason : false positive
public class StrictAttitudeLegsSequence<L extends AttitudeLeg> extends StrictLegsSequence<L>
        implements AttitudeProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = 3148900747224919462L;

    /** Spin derivative computation. */
    private boolean computeSpinDerivatives = false;

    /** {@inheritDoc} */
    @Override
    public Attitude getAttitude(final PVCoordinatesProvider pvProv, final AbsoluteDate date,
            final Frame frame) throws PatriusException {
        final L leg = current(date);
        if (leg == null) {
            throw new PatriusException(PatriusMessages.DATE_OUTSIDE_ATTITUDE_SEQUENCE, date);
        }
        return leg.getAttitude(pvProv, date, frame);
    }

    /** {@inheritDoc} */
    @Override
    public void setSpinDerivativesComputation(final boolean computeSpinDerivativesIn) {
        this.computeSpinDerivatives = computeSpinDerivativesIn;
        for (final AttitudeLeg attitudeLeg : this) {
            attitudeLeg.setSpinDerivativesComputation(computeSpinDerivativesIn);
        }
    }

    /**
     * Returns the spin derivatives computation flag.
     * @return the spin derivatives computation flag
     */
    public boolean isSpinDerivativesComputation() {
        return this.computeSpinDerivatives;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public StrictAttitudeLegsSequence<L> sub(final AbsoluteDate fromT, final AbsoluteDate toT,
            final boolean strict) {
        if ((fromT == null) || (toT == null)) {
            // Null case: exception
            throw PatriusException
                    .createIllegalArgumentException(PatriusMessages.LEG_CANNOT_BE_NULL);
        }
        if (isEmpty()) {
            // Empty case
            return new StrictAttitudeLegsSequence<>();
        }
        // Standard case
        final SortedSet<TimeStamped> subSet = getSet().subSet(fromT, !strict, toT, !strict);
        final StrictAttitudeLegsSequence<L> subSequence = new StrictAttitudeLegsSequence<>();
        subSet.forEach(t -> subSequence.add((L) t));
        return subSequence;
    }

    /** {@inheritDoc} */
    @Override
    public StrictAttitudeLegsSequence<L> sub(final AbsoluteDate fromT, final AbsoluteDate toT) {
        return sub(fromT, toT, false);
    }

    /** {@inheritDoc} */
    @Override
    public StrictAttitudeLegsSequence<L> sub(final AbsoluteDateInterval interval,
            final boolean strict) {
        return sub(interval.getLowerData(), interval.getUpperData(), strict);
    }

    /** {@inheritDoc} */
    @Override
    public StrictAttitudeLegsSequence<L> sub(final AbsoluteDateInterval interval) {
        return sub(interval.getLowerData(), interval.getUpperData(), false);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public StrictAttitudeLegsSequence<L> head(final AbsoluteDate toT, final boolean strict) {
        if (toT == null) {
            // Null case: exception
            throw PatriusException
                    .createIllegalArgumentException(PatriusMessages.LEG_CANNOT_BE_NULL);
        }
        if (isEmpty()) {
            // Empty case
            return new StrictAttitudeLegsSequence<>();
        }
        // Standard case
        final SortedSet<TimeStamped> headSet = getSet().headSet(toT, !strict);
        final StrictAttitudeLegsSequence<L> headSequence = new StrictAttitudeLegsSequence<>();
        headSet.forEach(t -> headSequence.add((L) t));
        return headSequence;
    }

    /** {@inheritDoc} */
    @Override
    public StrictAttitudeLegsSequence<L> head(final AbsoluteDate toT) {
        return head(toT, false);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public StrictAttitudeLegsSequence<L> tail(final AbsoluteDate fromT, final boolean strict) {
        if (fromT == null) {
            // Null case: exception
            throw PatriusException
                    .createIllegalArgumentException(PatriusMessages.LEG_CANNOT_BE_NULL);
        }
        if (isEmpty()) {
            // Empty case
            return new StrictAttitudeLegsSequence<>();
        }
        // Standard case
        final SortedSet<TimeStamped> tailSet = getSet().tailSet(fromT, !strict);
        final StrictAttitudeLegsSequence<L> tailSequence = new StrictAttitudeLegsSequence<>();
        tailSet.forEach(t -> tailSequence.add((L) t));
        return tailSequence;
    }

    /** {@inheritDoc} */
    @Override
    public StrictAttitudeLegsSequence<L> tail(final AbsoluteDate fromT) {
        return tail(fromT, false);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public StrictAttitudeLegsSequence<L> copy(final AbsoluteDateInterval newInterval,
            final boolean strict) {

        // Check that the new interval is included in the old interval
        if (!getTimeInterval().includes(newInterval)) {
            throw PatriusException
                    .createIllegalArgumentException(PatriusMessages.INTERVAL_MUST_BE_INCLUDED);
        }

        // Res
        final StrictAttitudeLegsSequence<L> res = new StrictAttitudeLegsSequence<>();

        // Specific behavior: we don't want each leg to be necessarily included in global validity
        // interval. NB : We do not need to deal with strict case because a StrictLegsSequence is
        // considered to have closed boundaries
        for (final L currentL : this) {
            final AbsoluteDateInterval intersection = currentL.getTimeInterval()
                    .getIntersectionWith(newInterval);
            if (intersection != null) {
                // Leg contained in truncation interval
                res.add((L) currentL.copy(intersection));
            }
        }

        // Res
        return res;
    }

    /** {@inheritDoc} */
    @Override
    public StrictAttitudeLegsSequence<L> copy(final AbsoluteDateInterval newInterval) {
        return copy(newInterval, false);
    }
}
