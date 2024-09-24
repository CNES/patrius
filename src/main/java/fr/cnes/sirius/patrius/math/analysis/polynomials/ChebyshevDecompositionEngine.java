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
 * VERSION:4.9:DM:DM-3171:10/05/2022:[PATRIUS] Clarification de la convention utilisee pour evaluer ... 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-2994:15/11/2021:[PATRIUS] Polynômes de Chebyshev pour l'interpolation et l'approximation
 * de fonctions 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.polynomials;

import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.exception.NoDataException;
import fr.cnes.sirius.patrius.math.exception.NotPositiveException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.fitting.PolynomialChebyshevFitter;
import fr.cnes.sirius.patrius.math.optim.ConvergenceChecker;
import fr.cnes.sirius.patrius.math.optim.PointVectorValuePair;
import fr.cnes.sirius.patrius.math.optim.nonlinear.vector.MultivariateVectorOptimizer;
import fr.cnes.sirius.patrius.math.optim.nonlinear.vector.jacobian.GaussNewtonOptimizer;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.utils.UtilsPatrius;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Approximate a {@link UnivariateFunction} by a Chebyshev polynomial.
 * 
 * <p>
 * <b>Important notice</b>: two conventions exist for defining Chebyshev polynomials:<br/>
 * <ul>
 * <li>Defining the first coefficient similarly to other coefficients.
 * Chebyshev Polynomial is then &Sigma;c<sub>i</sub>T<sub>i</sub> (for i in [0, n])</li>
 * <li>Adding a 0.5 multiplier to first coefficient.
 * Chebyshev Polynomial is then c<sub>0</sub>/2 + &Sigma;c<sub>i</sub>T<sub>i</sub>  (for i in [1, n])</li>
 * </ul>
 * PATRIUS uses the <b>first</b> convention (convention used in SPICE library).
 * </p>
 *
 * @author bonitt
 */
public final class ChebyshevDecompositionEngine {

    /**
     * Private constructor.
     */
    private ChebyshevDecompositionEngine() {
        // Nothing to do
    }
    
    /**
     * Approximate the given {@link UnivariateFunction function} by a Chebyshev polynomial of the given degree on the
     * specified range [start ; end].
     * The approximation is done by interpolation if the given number of nodes is smaller or equal to the degree+1 and
     * by fitting otherwise.
     * 
     * @param fct
     *        Function to interpolate
     * @param degree
     *        Chebyshev polynomial degree
     * @param start
     *        Start interval
     * @param end
     *        End interval
     * @param nbNodes
     *        Number of nodes
     * @return the approximated Chebyshev polynomial
     * @throws NullArgumentException
     *         if {@code fct} is {@code null}
     * @throws NotPositiveException
     *         if {@code degree < 0}
     * @throws MathIllegalArgumentException
     *         if {@code start >= end}
     */
    public static PolynomialChebyshevFunction approximateChebyshevFunction(final UnivariateFunction fct,
                                                                           final int degree, final double start,
                                                                           final double end, final int nbNodes) {
        // Check input consistency
        MathUtils.checkNotNull(fct);
        if (degree < 0) {
            throw new NotPositiveException(degree);
        }
        if (start >= end) {
            throw new MathIllegalArgumentException(PatriusMessages.INCORRECT_INTERVAL);
        }

        // Define the approximated function
        PolynomialChebyshevFunction approxFct = null;
        // Compute the number of coefficients
        final int n = degree + 1;
        // If the number of nodes is less than the number of coefficients, call the interpolateChebyshevFunction
        if (nbNodes <= n) {
            // Create the approximated polynomial Chebyshev function by interpolation
            approxFct = interpolateChebyshevFunction(fct, degree, start, end);
        } else {
            // Build the convergence checker for the Gauss-Newton optimizer
            final ConvergenceChecker<PointVectorValuePair> checker = new ConvergenceChecker<PointVectorValuePair>(){
                /** {@inheritDoc} */
                @Override
                public boolean converged(final int iteration,
                                         final PointVectorValuePair previous,
                                         final PointVectorValuePair current) {
                    boolean res = true;
                    for (int i = 0; i < n; i++) {
                        res &= (previous.getSecond()[i] - current.getSecond()[i])
                            <= UtilsPatrius.DOUBLE_COMPARISON_EPSILON;
                    }
                    return res;
                }
            };
            // Create the Gauss-Newton optimizer
            final MultivariateVectorOptimizer optimizer = new GaussNewtonOptimizer(checker);
            // Create the polynomial Chebyshev fitter
            final PolynomialChebyshevFitter fitter = new PolynomialChebyshevFitter(start, end, optimizer);
            // Add observed points
            for (int i = 0; i < nbNodes; i++) {
                // Compute abscissa of observed point
                final double x = start + (i / 100.) * (end - start);
                // Add observed point
                fitter.addObservedPoint(x, fct.value(x));
            }
            // Estimate the coefficients (all to zero by default)
            final double[] guessedCoeff = new double[n];
            // Obtain the actual coefficients by fitting
            final double[] actualCoeff = fitter.fit(guessedCoeff);
            // Create the approximated polynomial Chebyshev function with the actual coefficients
            approxFct = new PolynomialChebyshevFunction(start, end, actualCoeff);
        }

        // Return the approximated polynomial Chebyshev function
        return approxFct;
    }

    /**
     * Interpolate the given {@link UnivariateFunction function} by a Chebyshev polynomial on the specified range
     * [start ; end].
     * 
     * @param fct
     *        Function to interpolate
     * @param degree
     *        Chebyshev polynomial degree
     * @param start
     *        Start interval
     * @param end
     *        End interval
     * @return the interpolated Chebyshev polynomial
     * @throws NullArgumentException
     *         if {@code fct} is {@code null}
     * @throws NotPositiveException
     *         if {@code degree < 0}
     * @throws MathIllegalArgumentException
     *         if {@code start >= end}
     */
    public static PolynomialChebyshevFunction interpolateChebyshevFunction(final UnivariateFunction fct,
            final int degree, final double start, final double end) {
        // Check input consistency
        MathUtils.checkNotNull(fct);
        if (degree < 0) {
            throw new NotPositiveException(degree);
        }
        if (start >= end) {
            throw new MathIllegalArgumentException(PatriusMessages.INCORRECT_INTERVAL);
        }

        // Compute the Chebyshev abscissas on the interval (chebyshevAbscissas.length = n)
        final int n = degree + 1; // Coefficients number
        final double[] chebyshevAbscissas = PolynomialsUtils.getChebyshevAbscissas(start, end, n);

        // Compute the function values at each Chebyshev abscissa
        final double[] values = new double[n];
        for (int i = 0; i < n; i++) {
            values[i] = fct.value(chebyshevAbscissas[i]);
        }

        // Interpolate the Chebyshev function
        return interpolateChebyshevFunction(start, end, values);
    }

    /**
     * Interpolate the values of a <cite>f</cite> function defined at the Chebyshev abscissas by a Chebyshev
     * polynomial on the specified range [start ; end].
     * 
     * @param start
     *        Start range
     * @param end
     *        End range
     * @param values
     *        Values of the function to interpolate defined at the Chebyshev abscissas
     * @return the interpolated Chebyshev polynomial
     * @throws NullArgumentException
     *         if {@code values} is {@code null}
     * @throws NoDataException
     *         if {@code values} is empty
     * @throws MathIllegalArgumentException
     *         if {@code start >= end}
     */
    public static PolynomialChebyshevFunction interpolateChebyshevFunction(final double start,
            final double end,
            final double[] values) {
        // Check input consistency
        MathUtils.checkNotNull(values);
        final int n = values.length;
        if (n == 0) {
            // No data
            throw new NoDataException();
        }
        if (start >= end) {
            // Inconsistent interval
            throw new MathIllegalArgumentException(PatriusMessages.INCORRECT_INTERVAL);
        }

        double k;

        // Compute each coefficient
        final double[] c = new double[n];
        for (int i = 0; i < n; i++) {
            double sum = 0.;
            for (int j = 0; j < n; j++) {
                sum += values[n - j - 1] * MathLib.cos(MathLib.PI * i * (j + 0.5) / n);
            }
            if (i == 0) {
                k = 1. / n;
            } else {
                k = 2. / n;
            }
            c[i] = k * sum;
        }

        // Build the approximated Chebyshev function
        return new PolynomialChebyshevFunction(start, end, c);
    }
}
