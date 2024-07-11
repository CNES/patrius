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
 * VERSION:4.9:DM:DM-3154:10/05/2022:[PATRIUS] Amelioration des methodes permettant l'extraction d'une sous-sequence 
 * VERSION:4.9:DM:DM-3149:10/05/2022:[PATRIUS] Optimisation des reperes interplanetaires 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
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
 * This class handles a sequence of one or several {@link OrientationAngleProfile}. This sequence
 * can be handled as an {@link StrictLegsSequence} of {@link OrientationAngleProfile}.
 *
 * @author delaygni
 *
 * @since 4.2
 */
public class OrientationAngleProfileSequence extends StrictLegsSequence<OrientationAngleProfile>
        implements OrientationAngleProfile {

    /** Serial ID. */
    private static final long serialVersionUID = -2385389105822101864L;

    /** Default nature. */
    private static final String DEFAULT_NATURE = "ORIENTATION_ANGLE_PROFILE_SEQUENCE";

    /** Nature. */
    private final String nature;

    /**
     * Constructor.
     *
     * @param nature nature of the sequence
     */
    public OrientationAngleProfileSequence(final String nature) {
        super();
        this.nature = nature;
    }

    /**
     * Constructor with default value for the profiles sequence nature.
     */
    public OrientationAngleProfileSequence() {
        this(DEFAULT_NATURE);
    }

    /** {@inheritDoc} */
    @Override
    public Double getOrientationAngle(final PVCoordinatesProvider pvProv, final AbsoluteDate date)
            throws PatriusException {
        final OrientationAngleProfile leg = current(date);
        if (leg == null) {
            return null;
        }
        return leg.getOrientationAngle(pvProv, date);
    }

    /** {@inheritDoc} */
    @Override
    public String getNature() {
        return this.nature;
    }

    /** {@inheritDoc} */
    @Override
    public OrientationAngleProfileSequence copy(final AbsoluteDateInterval newInterval) {
        return copy(newInterval, false);
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
     * {@inheritDoc} Sequence is supposed to be continuous.
     */
    @Override
    public AbsoluteDateInterval getTimeInterval() {
        if (isEmpty()) {
            return null;
        } else {
            return new AbsoluteDateInterval(first().getDate(), last().getEnd());
        }
    }

    /** {@inheritDoc} */
    @Override
    public OrientationAngleProfileSequence sub(final AbsoluteDate fromT, final AbsoluteDate toT,
            final boolean strict) {
        if ((fromT == null) || (toT == null)) {
            // Null case: exception
            throw PatriusException
                    .createIllegalArgumentException(PatriusMessages.LEG_CANNOT_BE_NULL);
        }
        if (isEmpty()) {
            // Empty case
            return new OrientationAngleProfileSequence();
        }
        // Standard case
        final SortedSet<TimeStamped> subSet = getSet().subSet(fromT, !strict, toT, !strict);
        final OrientationAngleProfileSequence subSequence = new OrientationAngleProfileSequence();
        subSet.forEach(t -> subSequence.add((OrientationAngleProfile) t));
        return subSequence;
    }

    /** {@inheritDoc} */
    @Override
    public OrientationAngleProfileSequence sub(final AbsoluteDate fromT, final AbsoluteDate toT) {
        return sub(fromT, toT, false);
    }

    /** {@inheritDoc} */
    @Override
    public OrientationAngleProfileSequence sub(final AbsoluteDateInterval interval,
            final boolean strict) {
        return sub(interval.getLowerData(), interval.getUpperData(), strict);
    }

    /** {@inheritDoc} */
    @Override
    public OrientationAngleProfileSequence sub(final AbsoluteDateInterval interval) {
        return sub(interval.getLowerData(), interval.getUpperData(), false);
    }

    /** {@inheritDoc} */
    @Override
    public OrientationAngleProfileSequence head(final AbsoluteDate toT, final boolean strict) {
        if (toT == null) {
            // Null case: exception
            throw PatriusException
                    .createIllegalArgumentException(PatriusMessages.LEG_CANNOT_BE_NULL);
        }
        if (isEmpty()) {
            // Empty case
            return new OrientationAngleProfileSequence();
        }
        // Standard case
        final SortedSet<TimeStamped> headSet = getSet().headSet(toT, !strict);
        final OrientationAngleProfileSequence headSequence = new OrientationAngleProfileSequence();
        headSet.forEach(t -> headSequence.add((OrientationAngleProfile) t));
        return headSequence;
    }

    /** {@inheritDoc} */
    @Override
    public OrientationAngleProfileSequence head(final AbsoluteDate toT) {
        return head(toT, false);
    }

    /** {@inheritDoc} */
    @Override
    public OrientationAngleProfileSequence tail(final AbsoluteDate fromT, final boolean strict) {
        if (fromT == null) {
            // Null case: exception
            throw PatriusException
                    .createIllegalArgumentException(PatriusMessages.LEG_CANNOT_BE_NULL);
        }
        if (isEmpty()) {
            // Empty case
            return new OrientationAngleProfileSequence();
        }
        // Standard case
        final SortedSet<TimeStamped> tailSet = getSet().tailSet(fromT, !strict);
        final OrientationAngleProfileSequence tailSequence = new OrientationAngleProfileSequence();
        tailSet.forEach(t -> tailSequence.add((OrientationAngleProfile) t));
        return tailSequence;
    }

    /** {@inheritDoc} */
    @Override
    public OrientationAngleProfileSequence tail(final AbsoluteDate fromT) {
        return tail(fromT, false);
    }

    /** {@inheritDoc} */
    @Override
    public OrientationAngleProfileSequence copy(final AbsoluteDateInterval newInterval,
            final boolean strict) {

        // Check that the new interval is included in the old interval
        if (!getTimeInterval().includes(newInterval)) {
            throw PatriusException
                    .createIllegalArgumentException(PatriusMessages.INTERVAL_MUST_BE_INCLUDED);
        }

        // Specific behavior: we don't want each leg to be necessarily included in global validity
        // interval. NB : We do not need to deal with strict case because a StrictLegsSequence is
        // considered to have closed boundaries
        final OrientationAngleProfileSequence res = new OrientationAngleProfileSequence(getNature());
        for (final OrientationAngleProfile currentL : this) {
            final AbsoluteDateInterval intersection = currentL.getTimeInterval()
                    .getIntersectionWith(newInterval);
            if (intersection != null) {
                // Leg contained in truncation interval
                res.add(currentL.copy(intersection));
            }
        }

        return res;
    }
}
