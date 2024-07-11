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
 * @history created 28/02/12
 *
 * HISTORY
 * VERSION:4.9:DM:DM-3154:10/05/2022:[PATRIUS] Amelioration des methodes permettant l'extraction d'une sous-sequence 
 * VERSION:4.9:DM:DM-3152:10/05/2022:[PATRIUS] Suppression de l'attribut "nature" dans OrientationAngleLegsSequence  
 * VERSION:4.9:DM:DM-3149:10/05/2022:[PATRIUS] Optimisation des reperes interplanetaires 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.7:DM:DM-2859:18/05/2021:Optimisation du code ; SpacecraftState 
 * VERSION:4.7:DM:DM-2653:18/05/2021:generalisation des sequences et correction/refonte des sequences de segments 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1950:14/11/2018:new attitude profile design
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes.profiles;

import java.util.Iterator;
import java.util.SortedSet;

import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.StrictAttitudeLegsSequence;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.time.TimeStamped;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This class handles a sequence of {@link AttitudeProfile}.
 *
 * @author delaygni
 *
 * @since 4.2
 */
public final class AttitudeProfilesSequence extends StrictAttitudeLegsSequence<AttitudeProfile> {

    /** Serial UID. */
    private static final long serialVersionUID = -1887681878952009836L;

    /**
     * Gets the attitude from the sequence.<br>
     * The {@link AttitudeProfile} matching the date is called to compute the attitude. If none is
     * found, an {@link PatriusException} is thrown. If two laws are juxtaposed at the given date,
     * the SECOND law is the valid one.
     *
     * @param pvProv spacecraft's position and velocity coordinates provider
     * @param date the date for which the attitude is computed
     * @param frame the frame for which the attitude is computed
     * @return the attitude
     * @throws PatriusException thrown if the date is out of the sequence's range
     */
    @Override
    public Attitude getAttitude(final PVCoordinatesProvider pvProv, final AbsoluteDate date,
            final Frame frame) throws PatriusException {
        final AttitudeProfile leg = current(date);
        if (leg == null) {
            throw new PatriusException(PatriusMessages.DATE_OUTSIDE_LEGS_SEQUENCE_INTERVAL, date,
                    getTimeInterval());
        }
        return leg.getAttitude(pvProv, date, frame);
    }

    /** {@inheritDoc} */
    @Override
    public void setSpinDerivativesComputation(final boolean computeSpinDerivatives) {
        super.setSpinDerivativesComputation(computeSpinDerivatives);
        // Change spin derivative flag of all legs
        final Iterator<AttitudeProfile> iterator = iterator();
        while (iterator.hasNext()) {
            iterator.next().setSpinDerivativesComputation(computeSpinDerivatives);
        }
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
    public AttitudeProfilesSequence sub(final AbsoluteDate fromT, final AbsoluteDate toT,
            final boolean strict) {
        if ((fromT == null) || (toT == null)) {
            // Null case: exception
            throw PatriusException
                    .createIllegalArgumentException(PatriusMessages.LEG_CANNOT_BE_NULL);
        }
        if (isEmpty()) {
            // Empty case
            return new AttitudeProfilesSequence();
        }
        // Standard case
        final SortedSet<TimeStamped> subSet = getSet().subSet(fromT, !strict, toT, !strict);
        final AttitudeProfilesSequence subSequence = new AttitudeProfilesSequence();
        subSet.forEach(t -> subSequence.add((AttitudeProfile) t));
        return subSequence;
    }

    /** {@inheritDoc} */
    @Override
    public AttitudeProfilesSequence sub(final AbsoluteDate fromT, final AbsoluteDate toT) {
        return sub(fromT, toT, false);
    }

    /** {@inheritDoc} */
    @Override
    public AttitudeProfilesSequence sub(final AbsoluteDateInterval interval, final boolean strict) {
        return sub(interval.getLowerData(), interval.getUpperData(), strict);
    }

    /** {@inheritDoc} */
    @Override
    public AttitudeProfilesSequence sub(final AbsoluteDateInterval interval) {
        return sub(interval.getLowerData(), interval.getUpperData(), false);
    }

    /** {@inheritDoc} */
    @Override
    public AttitudeProfilesSequence head(final AbsoluteDate toT, final boolean strict) {
        if (toT == null) {
            // Null case: exception
            throw PatriusException
                    .createIllegalArgumentException(PatriusMessages.LEG_CANNOT_BE_NULL);
        }
        if (isEmpty()) {
            // Empty case
            return new AttitudeProfilesSequence();
        }
        // Standard case
        final SortedSet<TimeStamped> headSet = getSet().headSet(toT, !strict);
        final AttitudeProfilesSequence headSequence = new AttitudeProfilesSequence();
        headSet.forEach(t -> headSequence.add((AttitudeProfile) t));
        return headSequence;
    }

    /** {@inheritDoc} */
    @Override
    public AttitudeProfilesSequence head(final AbsoluteDate toT) {
        return head(toT, false);
    }

    /** {@inheritDoc} */
    @Override
    public AttitudeProfilesSequence tail(final AbsoluteDate fromT, final boolean strict) {
        if (fromT == null) {
            // Null case: exception
            throw PatriusException
                    .createIllegalArgumentException(PatriusMessages.LEG_CANNOT_BE_NULL);
        }
        if (isEmpty()) {
            // Empty case
            return new AttitudeProfilesSequence();
        }
        // Standard case
        final SortedSet<TimeStamped> tailSet = getSet().tailSet(fromT, !strict);
        final AttitudeProfilesSequence tailSequence = new AttitudeProfilesSequence();
        tailSet.forEach(t -> tailSequence.add((AttitudeProfile) t));
        return tailSequence;
    }

    /** {@inheritDoc} */
    @Override
    public AttitudeProfilesSequence tail(final AbsoluteDate fromT) {
        return tail(fromT, false);
    }

    /** {@inheritDoc} */
    @Override
    public AttitudeProfilesSequence copy(final AbsoluteDateInterval newInterval,
            final boolean strict) {

        // Check that the new interval is included in the old interval
        if (!getTimeInterval().includes(newInterval)) {
            throw PatriusException
                    .createIllegalArgumentException(PatriusMessages.INTERVAL_MUST_BE_INCLUDED);
        }

        // Res
        final AttitudeProfilesSequence res = new AttitudeProfilesSequence();

        // Specific behavior: we don't want each leg to be necessarily included in global validity
        // interval. NB : We do not need to deal with strict case because a StrictLegsSequence is
        // considered to have closed boundaries
        for (final AttitudeProfile currentL : this) {
            final AbsoluteDateInterval intersection = currentL.getTimeInterval()
                    .getIntersectionWith(newInterval);
            if (intersection != null) {
                // Leg contained in truncation interval
                res.add(currentL.copy(intersection));
            }
        }

        // Res
        return res;
    }

    /** {@inheritDoc} */
    @Override
    public AttitudeProfilesSequence copy(final AbsoluteDateInterval newInterval) {
        return copy(newInterval, false);
    }
}
