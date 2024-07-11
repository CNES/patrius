/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
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
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-2994:15/11/2021:[PATRIUS] Polynômes de Chebyshev pour l'interpolation et l'approximation
 * de fonctions 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.fitting;

import fr.cnes.sirius.patrius.math.analysis.polynomials.PolynomialChebyshevFunction;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.optim.nonlinear.vector.MultivariateVectorOptimizer;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Chebyshev polynomial fitting is a very simple case of {@link CurveFitter curve fitting}.
 * The estimated coefficients are the Chebyshev polynomial coefficients (see the {@link #fit(double[]) fit} method).
 * 
 * @author bonitt
 */
public class PolynomialChebyshevFitter extends CurveFitter<PolynomialChebyshevFunction> {

    /** Approximation start range. */
    private final double start;

    /** Approximation end range. */
    private final double end;

    /**
     * Simple constructor.
     * 
     * @param start
     *        Start range
     * @param end
     *        End range
     * @param optimizer
     *        Optimizer to use for the fitting
     * @throws MathIllegalArgumentException
     *         if {@code start >= end}
     */
    public PolynomialChebyshevFitter(final double start,
            final double end,
            final MultivariateVectorOptimizer optimizer) {
        super(optimizer);

        if (start >= end) {
            throw new MathIllegalArgumentException(PatriusMessages.INCORRECT_INTERVAL);
        }
        this.start = start;
        this.end = end;
    }

    /**
     * Get the coefficients of the Chebyshev polynomial fitting the weighted data points.
     * The degree of the fitting polynomial is {@code guess.length - 1}.
     * 
     * @param guess
     *        First guess for the coefficients. They must be sorted in increasing order of the polynomial's degree.
     * @param maxEval
     *        Maximum number of evaluations of the polynomial
     * @return the coefficients of the Chebyshev polynomial that best fits the observed points
     * @throws fr.cnes.sirius.patrius.math.exception.TooManyEvaluationsException
     *         if the number of evaluations exceeds {@code maxEval}
     * @throws fr.cnes.sirius.patrius.math.exception.ConvergenceException
     *         if the algorithm failed to converge
     */
    public double[] fit(final int maxEval, final double[] guess) {
        // Coefficients of Chebychev polynomial are here unused
        return this.fit(maxEval, new PolynomialChebyshevFunction(this.start, this.end, new double[1]), guess);
    }

    /**
     * Get the coefficients of the Chebyshev polynomial fitting the weighted data points.
     * The degree of the fitting polynomial is {@code guess.length - 1}.
     * 
     * @param guess
     *        First guess for the coefficients. They must be sorted in increasing order of the polynomial's degree.
     * @return the coefficients of the Chebyshev polynomial that best fits the observed points
     * @throws fr.cnes.sirius.patrius.math.exception.ConvergenceException
     *         if the algorithm failed to converge.
     */
    public double[] fit(final double[] guess) {
        // Coefficients of Chebychev polynomial are here unused
        return this.fit(new PolynomialChebyshevFunction(this.start, this.end, new double[1]), guess);
    }
}
