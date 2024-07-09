/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
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
 */
package fr.cnes.sirius.patrius.math.fitting;

import fr.cnes.sirius.patrius.math.analysis.polynomials.PolynomialFunction;
import fr.cnes.sirius.patrius.math.optim.nonlinear.vector.MultivariateVectorOptimizer;

/**
 * Polynomial fitting is a very simple case of {@link CurveFitter curve fitting}.
 * The estimated coefficients are the polynomial coefficients (see the {@link #fit(double[]) fit} method).
 * 
 * @version $Id: PolynomialFitter.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 2.0
 */
public class PolynomialFitter extends CurveFitter<PolynomialFunction.Parametric> {
    /**
     * Simple constructor.
     * 
     * @param optimizer
     *        Optimizer to use for the fitting.
     */
    public PolynomialFitter(final MultivariateVectorOptimizer optimizer) {
        super(optimizer);
    }

    /**
     * Get the coefficients of the polynomial fitting the weighted data points.
     * The degree of the fitting polynomial is {@code guess.length - 1}.
     * 
     * @param guess
     *        First guess for the coefficients. They must be sorted in
     *        increasing order of the polynomial's degree.
     * @param maxEval
     *        Maximum number of evaluations of the polynomial.
     * @return the coefficients of the polynomial that best fits the observed points.
     * @throws fr.cnes.sirius.patrius.math.exception.TooManyEvaluationsException
     *         if
     *         the number of evaluations exceeds {@code maxEval}.
     * @throws fr.cnes.sirius.patrius.math.exception.ConvergenceException
     *         if the algorithm failed to converge.
     */
    public double[] fit(final int maxEval, final double[] guess) {
        return this.fit(maxEval, new PolynomialFunction.Parametric(), guess);
    }

    /**
     * Get the coefficients of the polynomial fitting the weighted data points.
     * The degree of the fitting polynomial is {@code guess.length - 1}.
     * 
     * @param guess
     *        First guess for the coefficients. They must be sorted in
     *        increasing order of the polynomial's degree.
     * @return the coefficients of the polynomial that best fits the observed points.
     * @throws fr.cnes.sirius.patrius.math.exception.ConvergenceException
     *         if the algorithm failed to converge.
     */
    public double[] fit(final double[] guess) {
        return this.fit(new PolynomialFunction.Parametric(), guess);
    }
}
