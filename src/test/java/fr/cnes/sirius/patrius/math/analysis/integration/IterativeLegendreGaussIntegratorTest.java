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
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.integration;

import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.analysis.QuinticFunction;
import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.function.Sin;
import fr.cnes.sirius.patrius.math.analysis.polynomials.PolynomialFunction;
import fr.cnes.sirius.patrius.math.exception.TooManyEvaluationsException;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;

public class IterativeLegendreGaussIntegratorTest {

    @Test
    public void testSinFunction() {
        final UnivariateFunction f = new Sin();
        final BaseAbstractUnivariateIntegrator integrator =
            new IterativeLegendreGaussIntegrator(5, 1.0e-14, 1.0e-10, 2, 15);
        double min, max, expected, result, tolerance;

        min = 0;
        max = FastMath.PI;
        expected = 2;
        tolerance = MathLib.max(integrator.getAbsoluteAccuracy(),
            MathLib.abs(expected * integrator.getRelativeAccuracy()));
        result = integrator.integrate(10000, f, min, max);
        Assert.assertEquals(expected, result, tolerance);

        min = -FastMath.PI / 3;
        max = 0;
        expected = -0.5;
        tolerance = MathLib.max(integrator.getAbsoluteAccuracy(),
            MathLib.abs(expected * integrator.getRelativeAccuracy()));
        result = integrator.integrate(10000, f, min, max);
        Assert.assertEquals(expected, result, tolerance);
    }

    @Test
    public void testQuinticFunction() {
        final UnivariateFunction f = new QuinticFunction();
        final UnivariateIntegrator integrator =
            new IterativeLegendreGaussIntegrator(3,
                BaseAbstractUnivariateIntegrator.DEFAULT_RELATIVE_ACCURACY,
                BaseAbstractUnivariateIntegrator.DEFAULT_ABSOLUTE_ACCURACY,
                BaseAbstractUnivariateIntegrator.DEFAULT_MIN_ITERATIONS_COUNT,
                64);
        double min, max, expected, result;

        min = 0;
        max = 1;
        expected = -1.0 / 48;
        result = integrator.integrate(10000, f, min, max);
        Assert.assertEquals(expected, result, 1.0e-16);

        min = 0;
        max = 0.5;
        expected = 11.0 / 768;
        result = integrator.integrate(10000, f, min, max);
        Assert.assertEquals(expected, result, 1.0e-16);

        min = -1;
        max = 4;
        expected = 2048 / 3.0 - 78 + 1.0 / 48;
        result = integrator.integrate(10000, f, min, max);
        Assert.assertEquals(expected, result, 1.0e-16);
    }

    @Test
    public void testExactIntegration() {
        final Random random = new Random(86343623467878363l);
        for (int n = 2; n < 6; ++n) {
            final IterativeLegendreGaussIntegrator integrator =
                new IterativeLegendreGaussIntegrator(n,
                    BaseAbstractUnivariateIntegrator.DEFAULT_RELATIVE_ACCURACY,
                    BaseAbstractUnivariateIntegrator.DEFAULT_ABSOLUTE_ACCURACY,
                    BaseAbstractUnivariateIntegrator.DEFAULT_MIN_ITERATIONS_COUNT,
                    64);

            // an n points Gauss-Legendre integrator integrates 2n-1 degree polynoms exactly
            for (int degree = 0; degree <= 2 * n - 1; ++degree) {
                for (int i = 0; i < 10; ++i) {
                    final double[] coeff = new double[degree + 1];
                    for (int k = 0; k < coeff.length; ++k) {
                        coeff[k] = 2 * random.nextDouble() - 1;
                    }
                    final PolynomialFunction p = new PolynomialFunction(coeff);
                    final double result = integrator.integrate(10000, p, -5.0, 15.0);
                    final double reference = this.exactIntegration(p, -5.0, 15.0);
                    Assert.assertEquals(n + " " + degree + " " + i, reference, result,
                        1.0e-12 * (1.0 + MathLib.abs(reference)));
                }
            }

        }
    }

    @Test
    public void testIssue464() {
        final double value = 0.2;
        final UnivariateFunction f = new UnivariateFunction(){
            @Override
            public double value(final double x) {
                return (x >= 0 && x <= 5) ? value : 0.0;
            }
        };
        final IterativeLegendreGaussIntegrator gauss = new IterativeLegendreGaussIntegrator(5, 3, 100);

        // due to the discontinuity, integration implies *many* calls
        final double maxX = 0.32462367623786328;
        Assert.assertEquals(maxX * value, gauss.integrate(Integer.MAX_VALUE, f, -10, maxX), 1.0e-7);
        Assert.assertTrue(gauss.getEvaluations() > 37000000);
        Assert.assertTrue(gauss.getIterations() < 30);

        // setting up limits prevents such large number of calls
        try {
            gauss.integrate(1000, f, -10, maxX);
            Assert.fail("expected TooManyEvaluationsException");
        } catch (final TooManyEvaluationsException tmee) {
            // expected
            Assert.assertEquals(1000, tmee.getMax());
        }

        // integrating on the two sides should be simpler
        final double sum1 = gauss.integrate(1000, f, -10, 0);
        final int eval1 = gauss.getEvaluations();
        final double sum2 = gauss.integrate(1000, f, 0, maxX);
        final int eval2 = gauss.getEvaluations();
        Assert.assertEquals(maxX * value, sum1 + sum2, 1.0e-7);
        Assert.assertTrue(eval1 + eval2 < 200);

    }

    private double exactIntegration(final PolynomialFunction p, final double a, final double b) {
        final double[] coeffs = p.getCoefficients();
        double yb = coeffs[coeffs.length - 1] / coeffs.length;
        double ya = yb;
        for (int i = coeffs.length - 2; i >= 0; --i) {
            yb = yb * b + coeffs[i] / (i + 1);
            ya = ya * a + coeffs[i] / (i + 1);
        }
        return yb * b - ya * a;
    }
}
