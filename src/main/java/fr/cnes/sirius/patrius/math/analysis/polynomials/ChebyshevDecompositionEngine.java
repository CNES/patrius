/**
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
 */
package fr.cnes.sirius.patrius.math.analysis.polynomials;

import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.exception.NoDataException;
import fr.cnes.sirius.patrius.math.exception.NotPositiveException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Approximate a {@link UnivariateFunction} by a Chebyshev polynomial.
 * 
 * @author bonitt
* HISTORY
* VERSION:4.8:DM:DM-2997:15/11/2021:[PATRIUS] Disposer de fonctionnalites d'evaluation de l'erreur d'approximation d'une fonction 
* VERSION:4.8:DM:DM-2994:15/11/2021:[PATRIUS] Polynômes de Chebyshev pour l'interpolation et l'approximation de fonctions 
* VERSION:4.8:FA:FA-2998:15/11/2021:[PATRIUS] Discontinuite methode computeBearing de la classe ProjectionEllipsoidUtils
* VERSION:4.8:FA:FA-2982:15/11/2021:[PATRIUS] Orienter correctement les facettes de la methode toObjFile de GeodeticMeshLoader
* END-HISTORY
 */
public final class ChebyshevDecompositionEngine {

    /**
     * Private constructor.
     */
    private ChebyshevDecompositionEngine() {
        // Nothing to do
    }
    
    /**
     * Approximate the given {@link UnivariateFunction function} by a Chebyshev polynomial on the specified range
     * [start ; end].
     * 
     * @param fct
     *        Function to approximate
     * @param maxDegree
     *        Maximum Chebyshev polynomial degree (can be lower after the truncation of insignificant coefficients
     *        terms)
     * @param start
     *        Start interval
     * @param end
     *        End interval
     * @return the approximate Chebyshev polynomial
     * @throws NullArgumentException
     *         if {@code values} is {@code null}
     * @throws NotPositiveException
     *         if {@code degree < 0}
     * @throws MathIllegalArgumentException
     *         if {@code start >= end}
     */
    public static PolynomialChebyshevFunction approximateChebyshevFunction(final UnivariateFunction fct,
            final int maxDegree, final double start, final double end) {
        // Check input consistency
        MathUtils.checkNotNull(fct);
        if (maxDegree < 0) {
            throw new NotPositiveException(maxDegree);
        }
        if (start >= end) {
            throw new MathIllegalArgumentException(PatriusMessages.INCORRECT_INTERVAL);
        }

        // Compute the Chebyshev abscissas on the interval (chebyshevAbscissas.length = n)
        final int n = maxDegree + 1; // Coefficients number
        final double[] chebyshevAbscissas = PolynomialsUtils.getChebyshevAbscissas(start, end, n);

        // Compute the function values at each Chebyshev abscissa
        final double[] values = new double[n];
        for (int i = 0; i < n; i++) {
            values[i] = fct.value(chebyshevAbscissas[i]);
        }

        // Approximate the Chebyshev function
        return approximateChebyshevFunction(start, end, values);
    }

    /**
     * Approximate the values of a <cite>f</cite> function defined at the Chebyshev abscissas by a Chebyshev
     * polynomial on the specified range [start ; end].
     * 
     * @param start
     *        Start range
     * @param end
     *        End range
     * @param values
     *        Values of the function to approximate defined at the Chebyshev abscissas
     * @return the approximate Chebyshev polynomial
     * @throws NullArgumentException
     *         if {@code values} is {@code null}
     * @throws NoDataException
     *         if {@code values} is empty
     * @throws MathIllegalArgumentException
     *         if {@code start >= end}
     */
    public static PolynomialChebyshevFunction approximateChebyshevFunction(final double start,
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

        final double k = 2. / n;

        // Compute each coefficient
        final double[] c = new double[n];
        for (int i = 0; i < n; i++) {
            double sum = 0.;
            for (int j = 0; j < n; j++) {
                sum += values[j] * MathLib.cos(MathLib.PI * i * (j + 0.5) / n);
            }
            c[i] = k * sum;
        }

        // Build the approximated Chebyshev function
        return new PolynomialChebyshevFunction(start, end, c);
    }
}
