/**
 * Copyright 2023-2023 CNES
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
 * VERSION:4.13:DM:DM-108:08/12/2023:[PATRIUS] Modele d'obliquite et de precession de la Terre
 * VERSION:4.13:DM:DM-120:08/12/2023:[PATRIUS] Merge de la branche patrius-for-lotus dans Patrius
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.configuration.modprecession;

import fr.cnes.sirius.patrius.math.analysis.polynomials.PolynomialFunction;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This class implement the IAU Precession models for GCRF to MOD transformation.
 *
 * @see IAUMODPrecessionConvention
 * @author Emmanuel Bignon
 *
 * @since 4.13
 */
public class IAUMODPrecession implements MODPrecessionModel {

    /** Serial UID. */
    private static final long serialVersionUID = 6316741828433234470L;

    /** IAU convention. */
    private final IAUMODPrecessionConvention convention;

    /** Obliquity polynomial development degree. */
    private final int degreeObliquity;

    /** Precession polynomial development degree. */
    private final int degreePrecession;

    /** Obliquity polynomial. */
    private final PolynomialFunction polynomialObliquity;

    /** Precession (Zeta) polynomial. */
    private final PolynomialFunction polynomialPrecessionZeta;

    /** Precession (Theta) polynomial. */
    private final PolynomialFunction polynomialPrecessionTheta;

    /** Precession (Z) polynomial. */
    private final PolynomialFunction polynomialPrecessionZ;

    /**
     * Constructor.
     *
     * @param convention IAU convention
     * @param degreeObliquity obliquity polynomial development degree
     * @param degreePrecession precession polynomial development degree
     * @throws IllegalArgumentException thrown if obliquity or precession degree is higher than degree available in
     *         convention or negative
     */
    public IAUMODPrecession(final IAUMODPrecessionConvention convention,
            final int degreeObliquity,
            final int degreePrecession) {
        this.convention = convention;
        this.degreeObliquity = degreeObliquity;
        this.degreePrecession = degreePrecession;

        // Build variables for computation times optimizations
        this.polynomialObliquity = buildPolynomial(convention.getObliquityCoefs(), degreeObliquity);
        this.polynomialPrecessionZeta = buildPolynomial(convention.getPrecessionZetaCoefs(), degreePrecession);
        this.polynomialPrecessionTheta = buildPolynomial(convention.getPrecessionThetaCoefs(), degreePrecession);
        this.polynomialPrecessionZ = buildPolynomial(convention.getPrecessionZCoefs(), degreePrecession);
    }

    /**
     * Build polynomial from coefficients and max degree.
     * @param coefficients coefficients
     * @param degree max degree
     * @return polynomial truncated to max degree
     * @throws IllegalArgumentException thrown if degree is higher than degree of provided coefficients or negative
     */
    private PolynomialFunction buildPolynomial(final double[] coefficients,
            final int degree) {
        if (degree < 0) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.NON_POSITIVE_POLYNOMIAL_DEGREE,
                    degree);
        }
        if (degree > coefficients.length - 1) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.HIGH_DEGREE, degree,
                    coefficients.length - 1);
        }
        final double[] truncatedCoefs = new double[degree + 1];
        System.arraycopy(coefficients, 0, truncatedCoefs, 0, degree + 1);
        return new PolynomialFunction(truncatedCoefs);
    }

    /** {@inheritDoc} */
    @Override
    public double getEarthObliquity(final AbsoluteDate date) {
        // Time measured in Julian centuries of 36525 ephemeris days from the epoch J2000
        final double t = date.offsetFrom(AbsoluteDate.J2000_EPOCH, TimeScalesFactory.getTT())
                / Constants.JULIAN_CENTURY;
        return polynomialObliquity.value(t);
    }

    /** {@inheritDoc} */
    @Override
    public Rotation getMODPrecession(final AbsoluteDate date) {

        // Duration in centuries since J2000 epoch
        final double ttc = date.durationFromJ2000EpochInCenturies();

        // Compute the zeta precession angle
        final double zeta = polynomialPrecessionZeta.value(ttc);

        // Compute the theta precession angle
        final double theta = polynomialPrecessionTheta.value(ttc);

        // Compute the z precession angle
        final double z = polynomialPrecessionZ.value(ttc);

        // Elementary rotations for precession
        final Rotation r1 = new Rotation(Vector3D.PLUS_K, z);
        final Rotation r2 = new Rotation(Vector3D.PLUS_J, -theta);
        final Rotation r3 = new Rotation(Vector3D.PLUS_K, zeta);

        // Complete precession
        final Rotation precession = r1.applyTo(r2.applyTo(r3));
        return precession.revert();
    }

    /**
     * Returns the IAU convention.
     * @return the IAU convention
     */
    public IAUMODPrecessionConvention getConvention() {
        return convention;
    }

    /**
     * Returns the obliquity polynomial development degree.
     * @return the obliquity polynomial development degree
     */
    public int getObliquityDegree() {
        return degreeObliquity;
    }

    /**
     * Returns the obliquity polynomial development degree.
     * @return the obliquity polynomial development degree
     */
    public int getPrecessionDegree() {
        return degreePrecession;
    }

    /**
     * Returns the Obliquity polynomial.
     * @return the Obliquity polynomial
     */
    public PolynomialFunction getPolynomialObliquity() {
        return polynomialObliquity;
    }

    /**
     * Returns the Precession (Zeta) polynomial.
     * @return the Precession (Zeta) polynomial
     */
    public PolynomialFunction getPolynomialPrecessionZeta() {
        return polynomialPrecessionZeta;
    }

    /**
     * Returns the Precession (Theta) polynomial.
     * @return the Precession (Theta) polynomial
     */
    public PolynomialFunction getPolynomialPrecessionTheta() {
        return polynomialPrecessionTheta;
    }

    /**
     * Returns the Precession (Z) polynomial.
     * @return the Precession (Z) polynomial
     */
    public PolynomialFunction getPolynomialPrecessionZ() {
        return polynomialPrecessionZ;
    }
}
