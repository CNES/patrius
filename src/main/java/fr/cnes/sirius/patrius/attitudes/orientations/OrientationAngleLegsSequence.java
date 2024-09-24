/**
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
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3152:10/05/2022:[PATRIUS] Suppression de l'attribut "nature" dans OrientationAngleLegsSequence  
 * VERSION:4.9:DM:DM-3149:10/05/2022:[PATRIUS] Optimisation des reperes interplanetaires 
 * VERSION:4.9:DM:DM-3188:10/05/2022:[PATRIUS] Rendre generique la classe OrientationAngleLegsSequence
 * VERSION:4.9:DM:DM-3154:10/05/2022:[PATRIUS] Amelioration des methodes permettant l'extraction d'une sous-sequence 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.7:DM:DM-2859:18/05/2021:Optimisation du code ; SpacecraftState 
 * VERSION:4.7:DM:DM-2653:18/05/2021:generalisation des sequences et correction/refonte des sequences de segments 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1949:14/11/2018:add new orientation feature
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes.orientations;

import java.util.SortedSet;

import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.time.TimeStamped;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.legs.StrictLegsSequence;

/**
 * This class handles a sequence of one or several {@link OrientationAngleLeg}.
 * This sequence can be handled as an {@link StrictLegsSequence} of {@link OrientationAngleLeg}.
 *
 * @param <L> OrientationAngleLeg
 * @author delaygni
 *
 * @since 4.2
 */
public class OrientationAngleLegsSequence<L extends OrientationAngleLeg> extends
        StrictLegsSequence<L> implements OrientationAngleProvider {

    /** Default nature. */
    public static final String DEFAULT_ORIENTATION_SEQUENCE_NATURE = "ORIENTATION_ANGLE_LEGS_SEQUENCE";

    /** Serial ID. */
    private static final long serialVersionUID = -2385389105822101864L;
    
    /** Unchecked string. */
    private static final String UNCHECKED = "unchecked";

    /** Nature. */
    private final String nature;
    
    /**
     * Simple constructor with default name defining the legs sequence nature.
     */
    public OrientationAngleLegsSequence() {
        this(DEFAULT_ORIENTATION_SEQUENCE_NATURE);
    }
    
    /**
     * Constructor.
     *
     * @param nature
     *        nature of the sequence
     */
    public OrientationAngleLegsSequence(final String nature) {
        super();
        this.nature = nature;
    }

    /**
     * Get the legs sequence nature.
     *
     * @return the nature
     */
    public String getNature() {
        return this.nature;
    }

    /**
     * Compute the orientation angle corresponding to an orbital state.
     * 
     * @param pvProv position-velocity provider around current date
     * @param date date
     * @return orientation angle at the specified date and position-velocity state, {@code null} if input date is not
     *         contained in any leg's time interval of the sequence
     * @throws PatriusException thrown if the angle cannot be computed
     */
    @Override
    public Double getOrientationAngle(final PVCoordinatesProvider pvProv, final AbsoluteDate date)
            throws PatriusException {
        final OrientationAngleLeg leg = current(date);
        if (leg == null) {
            return null;
        }
        return leg.getOrientationAngle(pvProv, date);
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.UselessOverridingMethod")
    // Reason: false positive, call to super is necessary otherwise ambiguous
            public
            String toPrettyString() {
        return super.toPrettyString();
    }
    
    /**
     * {@inheritDoc}
     * 
     * Sequence is supposed to be continuous over time.
     */
    @Override
    public AbsoluteDateInterval getTimeInterval() {
        if (isEmpty()) {
            return null;
        }
        return new AbsoluteDateInterval(first().getDate(), last().getEnd());
    }

    /** {@inheritDoc} */
    @SuppressWarnings(UNCHECKED)
    @Override
    public OrientationAngleLegsSequence<L> sub(final AbsoluteDate fromT, final AbsoluteDate toT,
            final boolean strict) {
        if ((fromT == null) || (toT == null)) {
            // Null case: exception
            throw PatriusException
                    .createIllegalArgumentException(PatriusMessages.LEG_CANNOT_BE_NULL);
        }
        if (isEmpty()) {
            // Empty case
            return new OrientationAngleLegsSequence<>(this.nature);
        }
        // Standard case
        final SortedSet<TimeStamped> subSet = getSet().subSet(fromT, !strict, toT, !strict);
        final OrientationAngleLegsSequence<L> subSequence = new OrientationAngleLegsSequence<>(this.nature);
        subSet.forEach(t -> subSequence.add((L) t));
        return subSequence;
    }

    /** {@inheritDoc} */
    @Override
    public OrientationAngleLegsSequence<L> sub(final AbsoluteDate fromT, final AbsoluteDate toT) {
        return sub(fromT, toT, false);
    }

    /** {@inheritDoc} */
    @Override
    public OrientationAngleLegsSequence<L> sub(final AbsoluteDateInterval interval,
            final boolean strict) {
        return sub(interval.getLowerData(), interval.getUpperData(), strict);
    }

    /** {@inheritDoc} */
    @Override
    public OrientationAngleLegsSequence<L> sub(final AbsoluteDateInterval interval) {
        return sub(interval.getLowerData(), interval.getUpperData(), false);
    }

    /** {@inheritDoc} */
    @SuppressWarnings(UNCHECKED)
    @Override
    public OrientationAngleLegsSequence<L> head(final AbsoluteDate toT, final boolean strict) {
        if (toT == null) {
            // Null case: exception
            throw PatriusException
                    .createIllegalArgumentException(PatriusMessages.LEG_CANNOT_BE_NULL);
        }
        if (isEmpty()) {
            // Empty case
            return new OrientationAngleLegsSequence<>(this.nature);
        }
        // Standard case
        final SortedSet<TimeStamped> headSet = getSet().headSet(toT, !strict);
        final OrientationAngleLegsSequence<L> headSequence = new OrientationAngleLegsSequence<>(this.nature);
        headSet.forEach(t -> headSequence.add((L) t));
        return headSequence;
    }

    /** {@inheritDoc} */
    @Override
    public OrientationAngleLegsSequence<L> head(final AbsoluteDate toT) {
        return head(toT, false);
    }

    /** {@inheritDoc} */
    @SuppressWarnings(UNCHECKED)
    @Override
    public OrientationAngleLegsSequence<L> tail(final AbsoluteDate fromT, final boolean strict) {
        if (fromT == null) {
            // Null case: exception
            throw PatriusException
                    .createIllegalArgumentException(PatriusMessages.LEG_CANNOT_BE_NULL);
        }
        if (isEmpty()) {
            // Empty case
            return new OrientationAngleLegsSequence<>(this.nature);
        }
        // Standard case
        final SortedSet<TimeStamped> tailSet = getSet().tailSet(fromT, !strict);
        final OrientationAngleLegsSequence<L> tailSequence = new OrientationAngleLegsSequence<>(this.nature);
        tailSet.forEach(t -> tailSequence.add((L) t));
        return tailSequence;
    }

    /** {@inheritDoc} */
    @Override
    public OrientationAngleLegsSequence<L> tail(final AbsoluteDate fromT) {
        return tail(fromT, false);
    }

    /** {@inheritDoc} */
    @SuppressWarnings(UNCHECKED)
    @Override
    public OrientationAngleLegsSequence<L> copy(final AbsoluteDateInterval newInterval,
            final boolean strict) {

        // Check that the new interval is included in the old interval
        if (!getTimeInterval().includes(newInterval)) {
            throw PatriusException
                    .createIllegalArgumentException(PatriusMessages.INTERVAL_MUST_BE_INCLUDED);
        }

        // Specific behavior: we don't want each leg to be necessarily included in global validity
        // interval. NB : We do not need to deal with strict case because a StrictLegsSequence is
        // considered to have closed boundaries
        final OrientationAngleLegsSequence<L> res = new OrientationAngleLegsSequence<>(this.nature);
        for (final OrientationAngleLeg currentL : this) {
            final AbsoluteDateInterval intersection = currentL.getTimeInterval()
                    .getIntersectionWith(newInterval);
            if (intersection != null) {
                // Leg contained in truncation interval
                res.add((L) currentL.copy(intersection));
            }
        }

        return res;
    }

    /** {@inheritDoc} */
    @Override
    public OrientationAngleLegsSequence<L> copy(final AbsoluteDateInterval newInterval) {
        return copy(newInterval, false);
    }
}
