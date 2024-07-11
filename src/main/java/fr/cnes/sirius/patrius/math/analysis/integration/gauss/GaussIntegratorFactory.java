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
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.integration.gauss;

import java.math.BigDecimal;

import fr.cnes.sirius.patrius.math.util.Pair;

/**
 * Class that provides different ways to compute the nodes and weights to be
 * used by the {@link GaussIntegrator Gaussian integration rule}.
 * 
 * @since 3.1
 * @version $Id: GaussIntegratorFactory.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class GaussIntegratorFactory {

    /** Generator of Gauss-Legendre integrators. */
    private final BaseRuleFactory<Double> legendreGenerator = new LegendreRuleFactory();

    /** Generator of Gauss-Legendre integrators. */
    private final BaseRuleFactory<BigDecimal> legendreHighPrecisionGenerator = new LegendreHighPrecisionRuleFactory();

    /**
     * Creates an integrator of the given order, and whose call to the
     * {@link GaussIntegrator#integrate(fr.cnes.sirius.patrius.math.analysis.UnivariateFunction)
     * integrate} method will perform an integration on the natural interval {@code [-1 , 1]}.
     * 
     * @param numberOfPoints
     *        Order of the integration rule.
     * @return a Gauss-Legendre integrator.
     */
    public GaussIntegrator legendre(final int numberOfPoints) {
        return new GaussIntegrator(getRule(this.legendreGenerator, numberOfPoints));
    }

    /**
     * Creates an integrator of the given order, and whose call to the
     * {@link GaussIntegrator#integrate(fr.cnes.sirius.patrius.math.analysis.UnivariateFunction)
     * integrate} method will perform an integration on the given interval.
     * 
     * @param numberOfPoints
     *        Order of the integration rule.
     * @param lowerBound
     *        Lower bound of the integration interval.
     * @param upperBound
     *        Upper bound of the integration interval.
     * @return a Gauss-Legendre integrator.
     */
    public GaussIntegrator legendre(final int numberOfPoints,
                                    final double lowerBound,
                                    final double upperBound) {
        return new GaussIntegrator(transform(getRule(this.legendreGenerator, numberOfPoints),
            lowerBound, upperBound));
    }

    /**
     * Creates an integrator of the given order, and whose call to the
     * {@link GaussIntegrator#integrate(fr.cnes.sirius.patrius.math.analysis.UnivariateFunction)
     * integrate} method will perform an integration on the natural interval {@code [-1 , 1]}.
     * 
     * @param numberOfPoints
     *        Order of the integration rule.
     * @return a Gauss-Legendre integrator.
     */
    public GaussIntegrator legendreHighPrecision(final int numberOfPoints) {
        return new GaussIntegrator(getRule(this.legendreHighPrecisionGenerator, numberOfPoints));
    }

    /**
     * Creates an integrator of the given order, and whose call to the
     * {@link GaussIntegrator#integrate(fr.cnes.sirius.patrius.math.analysis.UnivariateFunction)
     * integrate} method will perform an integration on the given interval.
     * 
     * @param numberOfPoints
     *        Order of the integration rule.
     * @param lowerBound
     *        Lower bound of the integration interval.
     * @param upperBound
     *        Upper bound of the integration interval.
     * @return a Gauss-Legendre integrator.
     */
    public GaussIntegrator legendreHighPrecision(final int numberOfPoints,
                                                 final double lowerBound,
                                                 final double upperBound) {
        return new GaussIntegrator(transform(getRule(this.legendreHighPrecisionGenerator, numberOfPoints),
            lowerBound, upperBound));
    }

    /**
     * @param factory
     *        Integration rule factory.
     * @param numberOfPoints
     *        Order of the integration rule.
     * @return the integration nodes and weights.
     */
    private static Pair<double[], double[]> getRule(final BaseRuleFactory<? extends Number> factory,
                                                    final int numberOfPoints) {
        return factory.getRule(numberOfPoints);
    }

    /**
     * Performs a change of variable so that the integration can be performed
     * on an arbitrary interval {@code [a, b]}.
     * It is assumed that the natural interval is {@code [-1, 1]}.
     * 
     * @param rule
     *        Original points and weights.
     * @param a
     *        Lower bound of the integration interval.
     * @param b
     *        Lower bound of the integration interval.
     * @return the points and weights adapted to the new interval.
     */
    private static Pair<double[], double[]> transform(final Pair<double[], double[]> rule,
                                                      final double a,
                                                      final double b) {
        final double[] points = rule.getFirst();
        final double[] weights = rule.getSecond();

        // Scaling
        final double scale = (b - a) / 2;
        final double shift = a + scale;

        for (int i = 0; i < points.length; i++) {
            points[i] = points[i] * scale + shift;
            weights[i] *= scale;
        }

        return new Pair<double[], double[]>(points, weights);
    }
}
