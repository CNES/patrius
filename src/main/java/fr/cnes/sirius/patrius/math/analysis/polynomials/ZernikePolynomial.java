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
 *
 * HISTORY
 * VERSION:4.13:DM:DM-120:08/12/2023:[PATRIUS] Merge de la branche patrius-for-lotus dans Patrius
 * VERSION:4.13:DM:DM-101:08/12/2023:[PATRIUS] Harmonisation des eclipses pour les evenements et pour la PRS
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.polynomials;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.NotPositiveException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;
import fr.cnes.sirius.patrius.math.parameter.IParameterizable;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Pair;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Class representing a Zernike polynomial.
 *
 * @author veuillh
 * 
 * @since 4.13
 */
public class ZernikePolynomial implements IParameterizable {

    /** Serializable UID. */
    private static final long serialVersionUID = 5472348985081599387L;

    /** The radial degree. */
    private final int radialDegree;

    /** The coefficients map. */
    private final Map<Parameter, Pair<Integer, Integer>> coefficientsMap;

    /** The coefficients. */
    private final Parameter[][] coefficients;

    /**
     * Simple constructor.
     * <p>
     * Note: the parameters are initialized with values of 0.<br>
     * If their values isn't changed, the {@link #computeValue(double, double)} method will return 0.
     * </p>
     *
     * @param radialDegree
     *        The radial degree of the polynomial (azimuthal degree is considered equal to the radial degree)
     * @throws NotPositiveException
     *         if {@code radialDegree < 0}
     */
    public ZernikePolynomial(final int radialDegree) {
        this(radialDegree, initializeCoefficients(radialDegree));
    }

    /**
     * Constructor.
     *
     * @param radialDegree
     *        The radial degree of the polynomial (azimuthal degree is considered equal to the radial degree)
     * @param coefficients
     *        The coefficients of the polynomial. The 2D array must have the correct size: {@code radialDegree+1} rows
     *        and for the column use {@link #azimuthalDegreeToArrayIndex} to get the correct indexes.
     * @throws NotPositiveException
     *         if {@code radialDegree < 0}
     * @throws DimensionMismatchException
     *         if the {@code coefficients.length != radialDegree + 1}<br>
     *         if the {@code coefficientsN.length != n + 1}
     * @throws NullArgumentException
     *         if the coefficients array contains a {@code null} element
     */
    public ZernikePolynomial(final int radialDegree, final Parameter[][] coefficients) {
        checkRadialDegree(radialDegree);

        this.radialDegree = radialDegree;

        if (coefficients.length != radialDegree + 1) {
            throw new DimensionMismatchException(coefficients.length, radialDegree + 1);
        }

        this.coefficientsMap = new LinkedHashMap<>(); // To keep the parameters order
        this.coefficients = coefficients.clone();

        for (int n = 0; n < this.coefficients.length; n++) {
            final Parameter[] coefficientsN = this.coefficients[n];
            if (coefficientsN.length != n + 1) {
                throw new DimensionMismatchException(coefficientsN.length, n + 1);
            }
            for (int j = 0; j < coefficientsN.length; j++) {
                final Parameter coefficient = coefficientsN[j];
                if (coefficient == null) {
                    throw new NullArgumentException(PatriusMessages.NULL_NOT_ALLOWED_DESCRIPTION, "coefficient");
                }
                this.coefficientsMap.put(coefficient, new Pair<>(n, j));
            }
        }
    }

    /**
     * Getter for the required coefficient of the zernike polynomial.
     *
     * @param radialDegreeIn
     *        The coefficient radial degree
     * @param azimuthalDegree
     *        the coefficient azimuthal degree
     * @return the coefficient parameter
     */
    public Parameter getCoefficient(final int radialDegreeIn, final int azimuthalDegree) {
        return this.coefficients[radialDegreeIn][azimuthalDegreeToArrayIndex(radialDegreeIn, azimuthalDegree)];
    }

    /**
     * Compute the value and partial derivatives. Equivalent to call {@link #computeValue} and
     * {@link #computeDerivatives} but performs the
     * heavy computation only once.
     *
     * @param rho
     *        The distance variable
     * @param azimuth
     *        The angular variable [rad]
     * @param params
     *        The collection of parameters for the partial derivatives computation
     * @return A pair where first element represents the value and second element represents the partial derivatives
     *         with respect to the provided parameters
     * @throws OutOfRangeException
     *         if rho is outside [0, 1]
     */
    public Pair<Double, double[]> computeValueAndDerivatives(final double rho, final double azimuth,
                                                             final Collection<Parameter> params) {
        final double[][] monomials = computeZernikeMonomials(this.radialDegree, rho, azimuth);
        return new Pair<>(computeValue(monomials), computeDerivatives(monomials, params));
    }

    /**
     * Compute the value of this zernike polynomial.
     *
     * @param rho
     *        The distance variable
     * @param azimuth
     *        The angular variable [rad]
     * @return The polynomial value
     * @throws OutOfRangeException
     *         if rho is outside [0, 1]
     */
    public double computeValue(final double rho, final double azimuth) {
        checkRho(rho);

        final double[][] monomials = computeZernikeMonomials(this.radialDegree, rho, azimuth);
        return computeValue(monomials);
    }

    /**
     * Compute the derivatives of this zernike polynomial with respect to the provided parameters.
     *
     * @param rho
     *        The distance variable
     * @param azimuth
     *        The angular variable [rad]
     * @param params
     *        The collection of parameters
     * @return the partial derivatives with respect to the provided parameters
     * @throws OutOfRangeException
     *         if rho is outside [0, 1]
     */
    public double[] computeDerivatives(final double rho, final double azimuth, final Collection<Parameter> params) {
        checkRho(rho);

        final double[][] monomials = computeZernikeMonomials(this.radialDegree, rho, azimuth);
        return computeDerivatives(monomials, params);
    }

    /** {@inheritDoc} */
    @Override
    public boolean supportsParameter(final Parameter param) {
        return this.coefficientsMap.containsKey(param);
    }

    /** {@inheritDoc} */
    @Override
    public ArrayList<Parameter> getParameters() {
        return new ArrayList<>(this.coefficientsMap.keySet());
    }

    /**
     * Compute the value of this zernike polynomial.
     *
     * @param monomials
     *        The zernike monomials
     * @return the polynomial value
     */
    private double computeValue(final double[][] monomials) {
        // Initialize the value
        double value = 0;
        for (int n = 0; n < this.coefficients.length; n++) {
            final Parameter[] coefficientsI = this.coefficients[n];
            final double[] monomialsI = monomials[n];
            for (int j = 0; j < coefficientsI.length; j++) {
                // Add the element contribution to the final value
                value += coefficientsI[j].getValue() * monomialsI[j];
            }
        }
        // Return the final value
        return value;
    }

    /**
     * Compute the partial derivatives with respect to the provided parameters.
     *
     * @param monomials
     *        The zernike monomials
     * @param params
     *        The collections of parameters
     * @return the partial derivatives
     */
    private double[] computeDerivatives(final double[][] monomials, final Collection<Parameter> params) {
        final double[] out = new double[params.size()];
        int i = 0;
        for (final Parameter param : params) {
            final Pair<Integer, Integer> index = this.coefficientsMap.get(param);
            if (index == null) {
                out[i] = 0;
            } else {
                out[i] = monomials[index.getFirst()][index.getSecond()];
            }
            i++;
        }
        return out;
    }

    /**
     * Utility function to convert an array index of the {@link #computeZernikeMonomials} to an azimuthal degree.
     *
     * @param radialDegree
     *        The radial degree
     * @param arrayIndex
     *        The array index
     * @return the azimuthal degree
     * @throws NotPositiveException
     *         if {@code radialDegree < 0}<br>
     *         if {@code arrayIndex < 0}
     * @throws DimensionMismatchException
     *         if {@code arrayIndex > radialDegree + 1}
     */
    public static int arrayIndexToAzimuthalDegree(final int radialDegree, final int arrayIndex) {
        // Check the inputs
        if (radialDegree < 0) {
            throw new NotPositiveException(radialDegree);
        }
        if (arrayIndex < 0) {
            throw new NotPositiveException(arrayIndex);
        }
        if (arrayIndex > radialDegree + 1) {
            throw new DimensionMismatchException(arrayIndex, radialDegree + 1);
        }

        return 2 * arrayIndex - radialDegree;
    }

    /**
     * Utility function to convert an azimuthal degree to the array index of the {@link #computeZernikeMonomials}.
     *
     * @param radialDegree
     *        The radial degree
     * @param azimuthalDegree
     *        The azimuthal degree
     * @return the array index of {@code zernikeMonomials[radialDegree]} corresponding to the azimuthalDegree
     * @throws NotPositiveException
     *         if {@code radialDegree < 0}
     * @throws IllegalArgumentException
     *         if the azimuthal degree is greater in absolute value to the radial degree
     *         if the difference of the two degrees is not an even number
     */
    public static int azimuthalDegreeToArrayIndex(final int radialDegree, final int azimuthalDegree) {
        checkRadialDegree(radialDegree);

        if (radialDegree - MathLib.abs(azimuthalDegree) < 0) {
            throw PatriusException.createIllegalArgumentException(
                PatriusMessages.AZIMUTHAL_DEGREE_GREATER_RADIAL_DEGREE, azimuthalDegree, radialDegree);
        }
        if ((radialDegree - azimuthalDegree) % 2 != 0) {
            throw PatriusException.createIllegalArgumentException(
                PatriusMessages.DIFFERENCE_AZIMUTHAL_AND_RADIAL_DEGREES_ODD, azimuthalDegree, radialDegree);
        }

        return (radialDegree + azimuthalDegree) / 2;
    }

    /**
     * Compute the zernike monomials.
     *
     * <p>
     * The monomials are stored in a 2D array. The row i corresponds to the radial degree i and contains an array of
     * size i+1, containing the different azimuthal degrees. Use {@link #azimuthalDegreeToArrayIndex} and
     * {@link #arrayIndexToAzimuthalDegree} to switch between azimuthal degrees and array index.
     * </p>
     *
     * @param radialDegree
     *        The radial degree
     * @param rho
     *        The distance variable
     * @param azimuth
     *        The angular variable [rad]
     * @return the zernike monomials
     * @throws NotPositiveException
     *         if {@code radialDegree < 0}
     * @throws OutOfRangeException
     *         if rho is outside [0, 1]
     */
    public static double[][] computeZernikeMonomials(final int radialDegree, final double rho, final double azimuth) {

        // Validity checks are performed in computeRadialZernikeMonomials

        // Compute radial monomials
        final double[][] radialZernikeMonomials = computeRadialZernikeMonomials(radialDegree, rho);

        // Initialize array for zernike monomials
        final double[][] zernikeMonomials = new double[radialDegree + 1][];
        zernikeMonomials[0] = new double[] { 1 };

        // Sin cos array computation
        double[][] sinCos = null;
        if (radialDegree > 0) {
            sinCos = new double[radialDegree][2];
            MathLib.sinAndCos(azimuth, sinCos[0]);
            for (int k = 1; k < radialDegree; k++) {
                sinAndCosK(sinCos[k - 1], sinCos[0], sinCos[k]);
            }
        }

        // Loop on radial degrees (n)
        for (int n = 1; n <= radialDegree; n++) {
            // Radial monomial N
            final double[] radialZernikeMonomialsN = radialZernikeMonomials[n];

            // Initialize zernike array N
            final double[] zernikeMonomialsN = new double[n + 1];
            zernikeMonomials[n] = zernikeMonomialsN;

            // Loop on azimuthal degrees (m)
            int j = 0;
            for (int m = n % 2; m <= n; m += 2) {
                final double radialZernikeMonomialsNM = radialZernikeMonomialsN[j];
                if (m == 0) {
                    // n is even, zernikeMonomialsN size is odd
                    final double normalisationFactor = normalization(n, false);
                    zernikeMonomialsN[n / 2] = normalisationFactor * radialZernikeMonomialsNM;
                } else {
                    final double[] sinCosM = sinCos[m - 1];
                    final double normalisationFactor = normalization(n, true);
                    zernikeMonomialsN[(n - m) / 2] = normalisationFactor * radialZernikeMonomialsNM * sinCosM[0];
                    zernikeMonomialsN[(n + m) / 2] = normalisationFactor * radialZernikeMonomialsNM * sinCosM[1];
                }
                j++;
            }
        }
        return zernikeMonomials;
    }

    /**
     * The normalization factor is a scaling factor, which makes the RMS of each mode equal to one on the unit pupil
     * (radius = 1).
     *
     * @param radialDegree
     *        The radial degree
     * @param isAzimuthalDegree
     *        {@code true} if the radial degree is different from 0, {@code false} otherwise
     * @return the normalization factor
     */
    private static double normalization(final int radialDegree, final boolean isAzimuthalDegree) {
        final double normalizationFactor;
        if (isAzimuthalDegree) {
            normalizationFactor = MathLib.sqrt(2. * (radialDegree + 1.));
        } else {
            normalizationFactor = MathLib.sqrt(radialDegree + 1.);
        }
        return normalizationFactor;
    }

    /**
     * Compute the radial zernike monomials.
     *
     * <p>
     * The radial zernike monomials are 0 when radialDegree-azimuthalDegree is odd. The returned array does not contain
     * these values.<br>
     * The row n corresponds to the radial degree n, while each column j corresponds to the azimuthal degree n%2+2j:
     * <ul>
     * <li>For even radial degrees 2n: R_(2n)^0, R_(2n)^2, R_(2n)^4, etc...
     * <li>For odd radial degrees 2n+1: R_(2n+1)^1, R_(2n+1)^3, R_(2n+1)^5, etc...
     * </ul>
     * </p>
     *
     * @param radialDegree
     *        The radial degree
     * @param rho
     *        The distance variable
     * @return the radial zernike monomials
     * @throws NotPositiveException
     *         if {@code radialDegree < 0}
     * @throws OutOfRangeException
     *         if rho is outside [0, 1]
     */
    public static double[][] computeRadialZernikeMonomials(final int radialDegree, final double rho) {
        // Check inputs
        checkRadialDegree(radialDegree);
        checkRho(rho);

        // Initialize the radial zernike monomials array
        final double[][] radialZernikeMonomials = new double[radialDegree + 1][];
        double rhoPowerN = 1;

        // radialDegree 0
        radialZernikeMonomials[0] = new double[] { 1 };

        // Loop on radial degrees
        for (int n = 1; n <= radialDegree; n++) {
            final int nbAzimuthalDegrees = n / 2 + 1;
            final double[] radialZernikeMonomialsN = new double[nbAzimuthalDegrees];
            radialZernikeMonomials[n] = radialZernikeMonomialsN;
            rhoPowerN *= rho;

            // Loop on azimuthal degrees
            int j = 0;
            for (int m = n % 2; m <= n; m += 2) {
                // Compute the rNM value
                final double rNM;
                if (m == n) {
                    rNM = rhoPowerN;
                } else if (m == n - 2) {
                    rNM = computeRadialZernikeMplus2M(m, rho, rhoPowerN);
                } else {
                    rNM = computeRadialZernikeNMByRecurrence(radialZernikeMonomials[n - 2][j],
                        radialZernikeMonomials[n - 4][j], n, m, rho);
                }
                radialZernikeMonomialsN[j] = rNM;
                j++;
            }
        }
        return radialZernikeMonomials;
    }

    /**
     * Initialize the array of coefficient parameters.
     * <p>
     * Note: the parameters are initialized with values of 0.<br>
     * If their values isn't changed, the {@link #computeValue(double, double)} method will return 0.
     * </p>
     * 
     * @param radialDegree
     *        The radial degree
     * @return the coefficients array
     */
    private static Parameter[][] initializeCoefficients(final int radialDegree) {
        // Initialize the coefficients array
        final Parameter[][] out = new Parameter[radialDegree + 1][];
        for (int n = 0; n < out.length; n++) {
            // Each row has "n + 1" parameters
            final Parameter[] outN = new Parameter[n + 1];
            out[n] = outN;
            for (int j = 0; j < outN.length; j++) {
                // Compute the coefficients
                outN[j] = new Parameter(String.format("Zr%daz%d", n, 2 * j - n), 0.);
            }
        }
        return out;
    }

    /**
     * Check the validity of the distance variable (rho).
     *
     * @param rho
     *        The distance variable
     * @throws OutOfRangeException
     *         if rho is outside [0, 1]
     */
    private static void checkRho(final double rho) {
        if (rho < 0. || rho > 1.) {
            throw new OutOfRangeException(rho, 0, 1);
        }
    }

    /**
     * Check the validity of the radial degree.
     *
     * @param radialDegree
     *        The radial degree to check
     * @throws NotPositiveException
     *         if {@code radialDegree < 0}
     */
    private static void checkRadialDegree(final int radialDegree) {
        if (radialDegree < 0) {
            throw new NotPositiveException(radialDegree);
        }
    }

    /**
     * Internal method to compute sin(kx) and cos(kx) from sin((k-1)x), cos((k-1)x), sin(x) and cos(x) to avoid
     * computing sin and cos too many times.
     *
     * @param sinAndCosKminus1
     *        Array with sin((k-1)x) and cos((k-1)x)
     * @param sinAndCos
     *        Array with sin(x) and cos(x)
     * @param result
     *        Array with sin(kx) and cos(kx)
     */
    private static void sinAndCosK(final double[] sinAndCosKminus1, final double[] sinAndCos, final double[] result) {
        result[0] = sinAndCosKminus1[0] * sinAndCos[1] + sinAndCosKminus1[1] * sinAndCos[0];
        result[1] = sinAndCosKminus1[1] * sinAndCos[1] - sinAndCosKminus1[0] * sinAndCos[0];
    }

    /**
     * Compute the radial zernike monomial R_(n)^m through recurrence.
     *
     * @param radialZernikNminus2M
     *        The monomial R_(n-2)^m
     * @param radialZernikNminus4M
     *        The monomial R_(n-4)^m
     * @param n
     *        The radial degree
     * @param m
     *        The azimuthal degree
     * @param rho
     *        The distance variable
     * @return the R_(n)^m monomial
     */
    private static double computeRadialZernikeNMByRecurrence(final double radialZernikNminus2M,
                                                             final double radialZernikNminus4M,
                                                             final int n, final int m, final double rho) {
        final double radialZernikNminus2MTerm =
            2. * (n - 1.) * (2. * n * (n - 2.) * rho * rho - m * m - n * (n - 2.)) * radialZernikNminus2M;
        final double radialZernikNminus4MTerm = n * (n + m - 2) * (n - m - 2.) * radialZernikNminus4M;
        return (radialZernikNminus2MTerm - radialZernikNminus4MTerm) / ((n + m) * (n - m) * (n - 2.));
    }

    /**
     * Compute the radial zernike monomial R_(m+2)^m.
     *
     * @param m
     *        The azimuthal degree
     * @param rho
     *        The distance variable
     * @param rhoPowerN
     *        The distance variable to the power n
     * @return the R_(n+2)^n monomial
     */
    private static double computeRadialZernikeMplus2M(final int m, final double rho, final double rhoPowerN) {
        final double rho2 = rho * rho;
        final double rhoPowerM = rhoPowerN / rho2;
        return ((m + 2.) * rho * rho - (m + 1.)) * rhoPowerM;
    }
}
