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
 * HISTORY
 * VERSION:4.9:DM:DM-3154:10/05/2022:[PATRIUS] Amelioration des methodes permettant l'extraction d'une sous-sequence 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.7:DM:DM-2801:18/05/2021:Suppression des classes et methodes depreciees suite au refactoring des slews
 * VERSION:4.7:DM:DM-2653:18/05/2021:generalisation des sequences et correction/refonte des sequences de segments 
 * VERSION:4.5:DM:DM-2465:27/05/2020:Refactoring calculs de ralliements
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.attitudes.profiles;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.Slew;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.time.TimeStamped;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 *
 **/
/**
 * <p>
 * An attitude angular velocities profile slew, whose x-y-z components are represented with
 * polynomial functions.
 * <p>
 *
 * @author Emmanuel Bignon
 *
 * @since 4.5
 */
public class AngularVelocitiesPolynomialSlew extends AngularVelocitiesPolynomialProfile implements
        Slew {

    /** Serialization UID. */
    private static final long serialVersionUID = 1378612432768646890L;

    /** Nature. */
    private static final String DEFAULT_NATURE = "ANGULAR_VELOCITIES_POLYNOMIAL_SLEW";

    /**
     * Create an empty polynomial angular velocity attitude profiles slew.
     *
     * @param nature
     *        Nature of the sequence
     */
    public AngularVelocitiesPolynomialSlew(final String nature) {
        super(nature);
    }

    /**
     * Create a polynomial, angular velocity attitude profiles slew.
     *
     * @param polynomials
     *        the list of polynomial attitude profile segments
     */
    public AngularVelocitiesPolynomialSlew(
            final List<AngularVelocitiesPolynomialProfileLeg> polynomials) {
        this(polynomials, DEFAULT_NATURE);
    }

    /**
     * Create a polynomial, angular velocity attitude profiles slew.
     *
     * @param polynomials
     *        the list of polynomial attitude profile segments
     * @param nature
     *        Nature of the sequence
     */
    public AngularVelocitiesPolynomialSlew(
            final List<AngularVelocitiesPolynomialProfileLeg> polynomials, final String nature) {
        super(polynomials, nature);
    }

    /** {@inheritDoc} */
    @Override
    public Attitude getAttitude(final AbsoluteDate date, final Frame frame) throws PatriusException {
        // Slew has been computed, PV provider is not used
        return getAttitude(null, date, frame);
    }

    /** {@inheritDoc} */
    @Override
    public AngularVelocitiesPolynomialSlew copy(final AbsoluteDateInterval newInterval) {
        return copy(newInterval, false);
    }

    /** {@inheritDoc} */
    @Override
    public AngularVelocitiesPolynomialSlew copy(final AbsoluteDateInterval newInterval,
            final boolean strict) {
        // Check that the new interval is included in the old interval
        if (!getTimeInterval().includes(newInterval)) {
            throw PatriusException
                    .createIllegalArgumentException(PatriusMessages.INTERVAL_MUST_BE_INCLUDED);
        }

        // Specific behavior: we don't want each leg to be necessarily included in global validity
        // interval
        final List<AngularVelocitiesPolynomialProfileLeg> legs = new ArrayList<AngularVelocitiesPolynomialProfileLeg>();
        for (final AngularVelocitiesPolynomialProfileLeg currentL : this) {
            final AbsoluteDateInterval intersection = currentL.getTimeInterval()
                    .getIntersectionWith(newInterval);
            if (intersection != null) {
                // Leg contained in truncation interval
                legs.add(currentL.copy(intersection));
            }
        }

        final AngularVelocitiesPolynomialSlew res = new AngularVelocitiesPolynomialSlew(legs,
                getNature());
        res.setSpinDerivativesComputation(isSpinDerivativesComputation());
        return res;
    }

    /** {@inheritDoc} */
    @Override
    public AngularVelocitiesPolynomialSlew sub(final AbsoluteDate fromT, final AbsoluteDate toT,
            final boolean strict) {
        if ((fromT == null) || (toT == null)) {
            // Null case: exception
            throw PatriusException
                    .createIllegalArgumentException(PatriusMessages.LEG_CANNOT_BE_NULL);
        }
        if (isEmpty()) {
            // Empty case
            return new AngularVelocitiesPolynomialSlew("");
        }
        // Standard case
        final SortedSet<TimeStamped> subSet = getSet().subSet(fromT, !strict, toT, !strict);
        final AngularVelocitiesPolynomialSlew subSequence = new AngularVelocitiesPolynomialSlew(
                getNature());
        subSet.forEach(t -> subSequence.add((AngularVelocitiesPolynomialProfileLeg) t));
        return subSequence;
    }

    /** {@inheritDoc} */
    @Override
    public AngularVelocitiesPolynomialSlew sub(final AbsoluteDate fromT, final AbsoluteDate toT) {
        return sub(fromT, toT, false);
    }

    /** {@inheritDoc} */
    @Override
    public AngularVelocitiesPolynomialSlew sub(final AbsoluteDateInterval interval,
            final boolean strict) {
        return sub(interval.getLowerData(), interval.getUpperData(), strict);
    }

    /** {@inheritDoc} */
    @Override
    public AngularVelocitiesPolynomialSlew sub(final AbsoluteDateInterval interval) {
        return sub(interval.getLowerData(), interval.getUpperData(), false);
    }

    /** {@inheritDoc} */
    @Override
    public AngularVelocitiesPolynomialSlew head(final AbsoluteDate toT, final boolean strict) {
        if (toT == null) {
            // Null case: exception
            throw PatriusException
                    .createIllegalArgumentException(PatriusMessages.LEG_CANNOT_BE_NULL);
        }
        if (isEmpty()) {
            // Empty case
            return new AngularVelocitiesPolynomialSlew("");
        }
        // Standard case
        final SortedSet<TimeStamped> headSet = getSet().headSet(toT, !strict);
        final AngularVelocitiesPolynomialSlew headSequence = new AngularVelocitiesPolynomialSlew(
                getNature());
        headSet.forEach(t -> headSequence.add((AngularVelocitiesPolynomialProfileLeg) t));
        return headSequence;
    }

    /** {@inheritDoc} */
    @Override
    public AngularVelocitiesPolynomialSlew head(final AbsoluteDate toT) {
        return head(toT, false);
    }

    /** {@inheritDoc} */
    @Override
    public AngularVelocitiesPolynomialSlew tail(final AbsoluteDate fromT, final boolean strict) {
        if (fromT == null) {
            // Null case: exception
            throw PatriusException
                    .createIllegalArgumentException(PatriusMessages.LEG_CANNOT_BE_NULL);
        }
        if (isEmpty()) {
            // Empty case
            return new AngularVelocitiesPolynomialSlew("");
        }
        // Standard case
        final SortedSet<TimeStamped> tailSet = getSet().tailSet(fromT, !strict);
        final AngularVelocitiesPolynomialSlew tailSequence = new AngularVelocitiesPolynomialSlew(
                getNature());
        tailSet.forEach(t -> tailSequence.add((AngularVelocitiesPolynomialProfileLeg) t));
        return tailSequence;
    }

    /** {@inheritDoc} */
    @Override
    public AngularVelocitiesPolynomialSlew tail(final AbsoluteDate fromT) {
        return tail(fromT, false);
    }
}
