/**
 *
 * Copyright 2011-2017 CNES
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
 * @history created 04/04/2013
 *
 * HISTORY
* VERSION:4.7:DM:DM-2859:18/05/2021:Optimisation du code ; SpacecraftState 
* VERSION:4.7:DM:DM-2653:18/05/2021:generalisation des sequences et correction/refonte des sequences de segments 
 * VERSION:4.4:DM:DM-2231:04/10/2019:[PATRIUS] Creation d'un cache dans les profils de vitesse angulaire
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION:4.3:FA:FA-2084:15/05/2019:[PATRIUS] Suppression de la classe AbstractGuidanceProfile
 * VERSION::FA:180:27/03/2014:Removed DynamicsElements - frames transformations derivatives unknown
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:489:06/10/2015:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * VERSION::FA:1287:13/11/2017: Integration problem in AngularVelocitiesPolynomialProfile.java
 * VERSION::DM:1948:14/11/2018: new attitude leg sequence design
 * VERSION::DM:1951:10/12/2018: add method truncateTimeInterval
 * VERSION::DM:1950:11/12/2018:move guidance package to attitudes.profiles
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.attitudes.profiles;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.StrictAttitudeLegsSequence;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * <p>
 * An attitude angular velocities profile sequence, whose x-y-z components are represented with polynomial functions.
 * <p>
 *
 * @author Tiziana Sabatini, Pierre Brechard
 *
 * @since 2.0
 */
public class AngularVelocitiesPolynomialProfile extends
        StrictAttitudeLegsSequence<AngularVelocitiesPolynomialProfileLeg> implements AttitudeProfile {

    /** Serialization UID. */
    private static final long serialVersionUID = 1378612432768646890L;

    /** Nature. */
    private static final String DEFAULT_NATURE = "ANGULAR_VELOCITIES_POLYNOMIAL_PROFILE";

    /** Nature. */
    private final String nature;

    /**
     * Create an empty polynomial angular velocity attitude profiles sequence.
     *
     * @param nature
     *        Nature of the sequence
     */
    public AngularVelocitiesPolynomialProfile(final String nature) {
        super();
        this.nature = nature;
    }

    /**
     * Create a polynomial, angular velocity attitude profiles sequence.
     *
     * @param polynomials
     *        the list of polynomial attitude profile segments
     */
    public AngularVelocitiesPolynomialProfile(final List<AngularVelocitiesPolynomialProfileLeg> polynomials) {
        this(polynomials, DEFAULT_NATURE);
    }

    /**
     * Create a polynomial, angular velocity attitude profiles sequence.
     *
     * @param polynomials
     *        the list of polynomial attitude profile segments
     * @param nature
     *        Nature of the sequence
     */
    public AngularVelocitiesPolynomialProfile(final List<AngularVelocitiesPolynomialProfileLeg> polynomials,
            final String nature) {
        this(nature);
        for (final AngularVelocitiesPolynomialProfileLeg p : polynomials) {
            add(p);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Attitude getAttitude(final PVCoordinatesProvider pvProv,
            final AbsoluteDate date,
            final Frame frame) throws PatriusException {
        final AngularVelocitiesPolynomialProfileLeg leg = current(date);
        if (leg == null) {
            throw new PatriusException(PatriusMessages.DATE_OUTSIDE_LEGS_SEQUENCE_INTERVAL, date, getTimeInterval());
        }
        return leg.getAttitude(pvProv, date, frame);
    }

    /** {@inheritDoc} */
    @Override
    public void setSpinDerivativesComputation(final boolean computeSpinDerivatives) {
        super.setSpinDerivativesComputation(computeSpinDerivatives);
        // Change spin derivative flag of all legs
        final Iterator<AngularVelocitiesPolynomialProfileLeg> iterator = iterator();
        while (iterator.hasNext()) {
            iterator.next().setSpinDerivativesComputation(computeSpinDerivatives);
        }
    }

    /**
     * @return the map containing the coefficients of the polynomial function representing x, and their
     *         time interval of validity.
     */
    public Map<AbsoluteDateInterval, double[]> getXCoefficients() {
        final Map<AbsoluteDateInterval, double[]> coeffs = new ConcurrentHashMap<>();
        for (final AngularVelocitiesPolynomialProfileLeg leg : this) {
            coeffs.put(leg.getTimeInterval(), leg.getXCoefficients());
        }
        return coeffs;
    }

    /**
     * @return the map containing the coefficients of the polynomial function representing y, and their time
     *         interval of validity.
     */
    public Map<AbsoluteDateInterval, double[]> getYCoefficients() {
        final Map<AbsoluteDateInterval, double[]> coeffs = new ConcurrentHashMap<>();
        for (final AngularVelocitiesPolynomialProfileLeg leg : this) {
            coeffs.put(leg.getTimeInterval(), leg.getYCoefficients());
        }
        return coeffs;
    }

    /**
     * @return the map containing the coefficients of the polynomial function representing z, and their time interval
     *         of validity.
     */
    public Map<AbsoluteDateInterval, double[]> getZCoefficients() {
        final Map<AbsoluteDateInterval, double[]> coeffs = new ConcurrentHashMap<>();
        for (final AngularVelocitiesPolynomialProfileLeg leg : this) {
            coeffs.put(leg.getTimeInterval(), leg.getZCoefficients());
        }
        return coeffs;
    }

    /** {@inheritDoc} */
    @Override
    public String getNature() {
        return nature;
    }

    /** {@inheritDoc} */
    @Override
    public AngularVelocitiesPolynomialProfile copy(final AbsoluteDateInterval newInterval) {
        // Check that the new interval is included in the old interval
        if (!getTimeInterval().includes(newInterval)) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.INTERVAL_MUST_BE_INCLUDED);
        }

        // Specific behavior: we don't want each leg to be necessarily included in global validity interval
        final List<AngularVelocitiesPolynomialProfileLeg> legs = new ArrayList<AngularVelocitiesPolynomialProfileLeg>();
        for (final AngularVelocitiesPolynomialProfileLeg currentL : this) {
            final AbsoluteDateInterval intersection = currentL.getTimeInterval().getIntersectionWith(newInterval);
            if (intersection != null) {
                // Leg contained in truncation interval
                legs.add(currentL.copy(intersection));
            }
        }

        final AngularVelocitiesPolynomialProfile res = new AngularVelocitiesPolynomialProfile(legs, getNature());
        res.setSpinDerivativesComputation(isSpinDerivativesComputation());
        return res;
    }

    /** {@inheritDoc} */
    @Override
    public String toPrettyString() {
        return AttitudeProfile.super.toPrettyString();
    }

    /**
     * {@inheritDoc}
     * Sequence is supposed to be continuous.
     */
    @Override
    public AbsoluteDateInterval getTimeInterval() {
        if (isEmpty()) {
            return null;
        } else  {
            return new AbsoluteDateInterval(first().getDate(), last().getEnd());
        }
    }
}
