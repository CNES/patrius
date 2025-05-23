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
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.fitting;

import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.analysis.polynomials.PolynomialFunction;
import fr.cnes.sirius.patrius.math.analysis.polynomials.PolynomialFunction.Parametric;
import fr.cnes.sirius.patrius.math.distribution.RealDistribution;
import fr.cnes.sirius.patrius.math.distribution.UniformRealDistribution;
import fr.cnes.sirius.patrius.math.exception.ConvergenceException;
import fr.cnes.sirius.patrius.math.exception.TooManyEvaluationsException;
import fr.cnes.sirius.patrius.math.optim.SimpleVectorValueChecker;
import fr.cnes.sirius.patrius.math.optim.nonlinear.vector.MultivariateVectorOptimizer;
import fr.cnes.sirius.patrius.math.optim.nonlinear.vector.jacobian.GaussNewtonOptimizer;
import fr.cnes.sirius.patrius.math.optim.nonlinear.vector.jacobian.LevenbergMarquardtOptimizer;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Test for class {@link CurveFitter} where the function to fit is a
 * polynomial.
 */
public class PolynomialFitterTest {
    @Test
    public void testFit() {
        final RealDistribution rng = new UniformRealDistribution(-100, 100);
        rng.reseedRandomGenerator(64925784252L);

        final LevenbergMarquardtOptimizer optim = new LevenbergMarquardtOptimizer();
        final PolynomialFitter fitter = new PolynomialFitter(optim);
        final double[] coeff = { 12.9, -3.4, 2.1 }; // 12.9 - 3.4 x + 2.1 x^2
        final PolynomialFunction f = new PolynomialFunction(coeff);

        // Collect data from a known polynomial.
        for (int i = 0; i < 100; i++) {
            final double x = rng.sample();
            fitter.addObservedPoint(x, f.value(x));
        }

        // Start fit from initial guesses that are far from the optimal values.
        final double[] best = fitter.fit(new double[] { -1e-20, 3e15, -5e25 });

        TestUtils.assertEquals("best != coeff", coeff, best, 1e-12);
    }

    @Test
    public void testNoError() {
        final Random randomizer = new Random(64925784252l);
        for (int degree = 1; degree < 10; ++degree) {
            final PolynomialFunction p = this.buildRandomPolynomial(degree, randomizer);

            final PolynomialFitter fitter = new PolynomialFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i <= degree; ++i) {
                fitter.addObservedPoint(1.0, i, p.value(i));
            }

            final double[] init = new double[degree + 1];
            final PolynomialFunction fitted = new PolynomialFunction(fitter.fit(init));

            for (double x = -1.0; x < 1.0; x += 0.01) {
                final double error = MathLib.abs(p.value(x) - fitted.value(x)) /
                    (1.0 + MathLib.abs(p.value(x)));
                Assert.assertEquals(0.0, error, 1.0e-6);
            }
        }
    }

    @Test
    public void testSmallError() {
        final Random randomizer = new Random(53882150042l);
        double maxError = 0;
        for (int degree = 0; degree < 10; ++degree) {
            final PolynomialFunction p = this.buildRandomPolynomial(degree, randomizer);

            final PolynomialFitter fitter = new PolynomialFitter(new LevenbergMarquardtOptimizer());
            for (double x = -1.0; x < 1.0; x += 0.01) {
                fitter.addObservedPoint(1.0, x,
                    p.value(x) + 0.1 * randomizer.nextGaussian());
            }

            final double[] init = new double[degree + 1];
            final PolynomialFunction fitted = new PolynomialFunction(fitter.fit(init));

            for (double x = -1.0; x < 1.0; x += 0.01) {
                final double error = MathLib.abs(p.value(x) - fitted.value(x)) /
                    (1.0 + MathLib.abs(p.value(x)));
                maxError = MathLib.max(maxError, error);
                Assert.assertTrue(MathLib.abs(error) < 0.1);
            }
        }
        Assert.assertTrue(maxError > 0.01);
    }

    @Test
    public void testMath798() {
        final double tol = 1e-14;
        final SimpleVectorValueChecker checker = new SimpleVectorValueChecker(tol, tol);
        final double[] init = new double[] { 0, 0 };
        final int maxEval = 3;

        final double[] lm = doMath798(new LevenbergMarquardtOptimizer(checker), maxEval, init);
        final double[] gn = doMath798(new GaussNewtonOptimizer(checker), maxEval, init);

        for (int i = 0; i <= 1; i++) {
            Assert.assertEquals(lm[i], gn[i], tol);
        }
    }

    /**
     * This test shows that the user can set the maximum number of iterations
     * to avoid running for too long.
     * But in the test case, the real problem is that the tolerance is way too
     * stringent.
     */
    @Test(expected = TooManyEvaluationsException.class)
    public void testMath798WithToleranceTooLow() {
        final double tol = 1e-100;
        final SimpleVectorValueChecker checker = new SimpleVectorValueChecker(tol, tol);
        final double[] init = new double[] { 0, 0 };
        final int maxEval = 10000; // Trying hard to fit.

        doMath798(new GaussNewtonOptimizer(checker), maxEval, init);
    }

    /**
     * This test shows that the user can set the maximum number of iterations
     * to avoid running for too long.
     * Even if the real problem is that the tolerance is way too stringent, it
     * is possible to get the best solution so far, i.e. a checker will return
     * the point when the maximum iteration count has been reached.
     */
    @Test
    public void testMath798WithToleranceTooLowButNoException() {
        final double tol = 1e-100;
        final double[] init = new double[] { 0, 0 };
        final int maxEval = 10000; // Trying hard to fit.
        final SimpleVectorValueChecker checker = new SimpleVectorValueChecker(tol, tol, maxEval);

        final double[] lm = doMath798(new LevenbergMarquardtOptimizer(checker), maxEval, init);
        final double[] gn = doMath798(new GaussNewtonOptimizer(checker), maxEval, init);

        for (int i = 0; i <= 1; i++) {
            Assert.assertEquals(lm[i], gn[i], 1e-15);
        }
    }

    /**
     * @param optimizer
     *        Optimizer.
     * @param maxEval
     *        Maximum number of function evaluations.
     * @param init
     *        First guess.
     * @return the solution found by the given optimizer.
     */
    private static double[] doMath798(final MultivariateVectorOptimizer optimizer, final int maxEval,
                                      final double[] init) {
        final CurveFitter<Parametric> fitter = new CurveFitter<>(optimizer);

        fitter.addObservedPoint(-0.2, -7.12442E-13);
        fitter.addObservedPoint(-0.199, -4.33397E-13);
        fitter.addObservedPoint(-0.198, -2.823E-13);
        fitter.addObservedPoint(-0.197, -1.40405E-13);
        fitter.addObservedPoint(-0.196, -7.80821E-15);
        fitter.addObservedPoint(-0.195, 6.20484E-14);
        fitter.addObservedPoint(-0.194, 7.24673E-14);
        fitter.addObservedPoint(-0.193, 1.47152E-13);
        fitter.addObservedPoint(-0.192, 1.9629E-13);
        fitter.addObservedPoint(-0.191, 2.12038E-13);
        fitter.addObservedPoint(-0.19, 2.46906E-13);
        fitter.addObservedPoint(-0.189, 2.77495E-13);
        fitter.addObservedPoint(-0.188, 2.51281E-13);
        fitter.addObservedPoint(-0.187, 2.64001E-13);
        fitter.addObservedPoint(-0.186, 2.8882E-13);
        fitter.addObservedPoint(-0.185, 3.13604E-13);
        fitter.addObservedPoint(-0.184, 3.14248E-13);
        fitter.addObservedPoint(-0.183, 3.1172E-13);
        fitter.addObservedPoint(-0.182, 3.12912E-13);
        fitter.addObservedPoint(-0.181, 3.06761E-13);
        fitter.addObservedPoint(-0.18, 2.8559E-13);
        fitter.addObservedPoint(-0.179, 2.86806E-13);
        fitter.addObservedPoint(-0.178, 2.985E-13);
        fitter.addObservedPoint(-0.177, 2.67148E-13);
        fitter.addObservedPoint(-0.176, 2.94173E-13);
        fitter.addObservedPoint(-0.175, 3.27528E-13);
        fitter.addObservedPoint(-0.174, 3.33858E-13);
        fitter.addObservedPoint(-0.173, 2.97511E-13);
        fitter.addObservedPoint(-0.172, 2.8615E-13);
        fitter.addObservedPoint(-0.171, 2.84624E-13);

        final double[] coeff = fitter.fit(maxEval,
            new PolynomialFunction.Parametric(),
            init);
        return coeff;
    }

    @Test
    public void testRedundantSolvable() {
        // Levenberg-Marquardt should handle redundant information gracefully
        this.checkUnsolvableProblem(new LevenbergMarquardtOptimizer(), true);
    }

    @Test
    public void testRedundantUnsolvable() {
        // Gauss-Newton should not be able to solve redundant information
        this.checkUnsolvableProblem(new GaussNewtonOptimizer(true, new SimpleVectorValueChecker(1e-15, 1e-15)), false);
    }

    @Test
    public void testLargeSample() {
        final Random randomizer = new Random(0x5551480dca5b369bl);
        double maxError = 0;
        for (int degree = 0; degree < 10; ++degree) {
            final PolynomialFunction p = this.buildRandomPolynomial(degree, randomizer);

            final PolynomialFitter fitter = new PolynomialFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < 40000; ++i) {
                final double x = -1.0 + i / 20000.0;
                fitter.addObservedPoint(1.0, x,
                    p.value(x) + 0.1 * randomizer.nextGaussian());
            }

            final double[] init = new double[degree + 1];
            final PolynomialFunction fitted = new PolynomialFunction(fitter.fit(init));

            for (double x = -1.0; x < 1.0; x += 0.01) {
                final double error = MathLib.abs(p.value(x) - fitted.value(x)) /
                    (1.0 + MathLib.abs(p.value(x)));
                maxError = MathLib.max(maxError, error);
                Assert.assertTrue(MathLib.abs(error) < 0.01);
            }
        }
        Assert.assertTrue(maxError > 0.001);
    }

    private void checkUnsolvableProblem(final MultivariateVectorOptimizer optimizer,
                                        final boolean solvable) {
        final Random randomizer = new Random(1248788532l);
        for (int degree = 0; degree < 10; ++degree) {
            final PolynomialFunction p = this.buildRandomPolynomial(degree, randomizer);

            final PolynomialFitter fitter = new PolynomialFitter(optimizer);

            // reusing the same point over and over again does not bring
            // information, the problem cannot be solved in this case for
            // degrees greater than 1 (but one point is sufficient for
            // degree 0)
            for (double x = -1.0; x < 1.0; x += 0.01) {
                fitter.addObservedPoint(1.0, 0.0, p.value(0.0));
            }

            try {
                final double[] init = new double[degree + 1];
                fitter.fit(init);
                Assert.assertTrue(solvable || (degree == 0));
            } catch (final ConvergenceException e) {
                Assert.assertTrue((!solvable) && (degree > 0));
            }
        }
    }

    private PolynomialFunction buildRandomPolynomial(final int degree, final Random randomizer) {
        final double[] coefficients = new double[degree + 1];
        for (int i = 0; i <= degree; ++i) {
            coefficients[i] = randomizer.nextGaussian();
        }
        return new PolynomialFunction(coefficients);
    }
}
