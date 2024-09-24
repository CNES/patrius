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
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.optim.nonlinear.scalar.noderiv;

import java.util.Arrays;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import fr.cnes.sirius.patrius.math.Retry;
import fr.cnes.sirius.patrius.math.RetryRunner;
import fr.cnes.sirius.patrius.math.analysis.MultivariateFunction;
import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.NotPositiveException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooLargeException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;
import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;
import fr.cnes.sirius.patrius.math.optim.InitialGuess;
import fr.cnes.sirius.patrius.math.optim.MaxEval;
import fr.cnes.sirius.patrius.math.optim.PointValuePair;
import fr.cnes.sirius.patrius.math.optim.SimpleBounds;
import fr.cnes.sirius.patrius.math.optim.nonlinear.scalar.GoalType;
import fr.cnes.sirius.patrius.math.optim.nonlinear.scalar.ObjectiveFunction;
import fr.cnes.sirius.patrius.math.random.MersenneTwister;

/**
 * Test for {@link CMAESOptimizer}.
 */
@RunWith(RetryRunner.class)
public class CMAESOptimizerTest {

    static final int DIM = 13;
    static final int LAMBDA = 4 + (int) (3. * Math.log(DIM));

    @Test(expected = NumberIsTooLargeException.class)
    public void testInitOutofbounds1() {
        final double[] startPoint = point(DIM, 3);
        final double[] insigma = point(DIM, 0.3);
        final double[][] boundaries = boundaries(DIM, -1, 2);
        final PointValuePair expected =
            new PointValuePair(point(DIM, 1.0), 0.0);
        this.doTest(new Rosen(), startPoint, insigma, boundaries,
            GoalType.MINIMIZE, LAMBDA, true, 0, 1e-13,
            1e-13, 1e-6, 100000, expected);
    }

    @Test(expected = NumberIsTooSmallException.class)
    public void testInitOutofbounds2() {
        final double[] startPoint = point(DIM, -2);
        final double[] insigma = point(DIM, 0.3);
        final double[][] boundaries = boundaries(DIM, -1, 2);
        final PointValuePair expected =
            new PointValuePair(point(DIM, 1.0), 0.0);
        this.doTest(new Rosen(), startPoint, insigma, boundaries,
            GoalType.MINIMIZE, LAMBDA, true, 0, 1e-13,
            1e-13, 1e-6, 100000, expected);
    }

    @Test(expected = DimensionMismatchException.class)
    public void testBoundariesDimensionMismatch() {
        final double[] startPoint = point(DIM, 0.5);
        final double[] insigma = point(DIM, 0.3);
        final double[][] boundaries = boundaries(DIM + 1, -1, 2);
        final PointValuePair expected =
            new PointValuePair(point(DIM, 1.0), 0.0);
        this.doTest(new Rosen(), startPoint, insigma, boundaries,
            GoalType.MINIMIZE, LAMBDA, true, 0, 1e-13,
            1e-13, 1e-6, 100000, expected);
    }

    @Test(expected = NotPositiveException.class)
    public void testInputSigmaNegative() {
        final double[] startPoint = point(DIM, 0.5);
        final double[] insigma = point(DIM, -0.5);
        final double[][] boundaries = null;
        final PointValuePair expected =
            new PointValuePair(point(DIM, 1.0), 0.0);
        this.doTest(new Rosen(), startPoint, insigma, boundaries,
            GoalType.MINIMIZE, LAMBDA, true, 0, 1e-13,
            1e-13, 1e-6, 100000, expected);
    }

    @Test(expected = OutOfRangeException.class)
    public void testInputSigmaOutOfRange() {
        final double[] startPoint = point(DIM, 0.5);
        final double[] insigma = point(DIM, 1.1);
        final double[][] boundaries = boundaries(DIM, -0.5, 0.5);
        final PointValuePair expected =
            new PointValuePair(point(DIM, 1.0), 0.0);
        this.doTest(new Rosen(), startPoint, insigma, boundaries,
            GoalType.MINIMIZE, LAMBDA, true, 0, 1e-13,
            1e-13, 1e-6, 100000, expected);
    }

    @Test(expected = DimensionMismatchException.class)
    public void testInputSigmaDimensionMismatch() {
        final double[] startPoint = point(DIM, 0.5);
        final double[] insigma = point(DIM + 1, 0.5);
        final double[][] boundaries = null;
        final PointValuePair expected =
            new PointValuePair(point(DIM, 1.0), 0.0);
        this.doTest(new Rosen(), startPoint, insigma, boundaries,
            GoalType.MINIMIZE, LAMBDA, true, 0, 1e-13,
            1e-13, 1e-6, 100000, expected);
    }

    @Test
    @Retry(3)
    public void testRosen() {
        final double[] startPoint = point(DIM, 0.1);
        final double[] insigma = point(DIM, 0.1);
        final double[][] boundaries = null;
        final PointValuePair expected =
            new PointValuePair(point(DIM, 1.0), 0.0);
        this.doTest(new Rosen(), startPoint, insigma, boundaries,
            GoalType.MINIMIZE, LAMBDA, true, 0, 1e-13,
            1e-13, 1e-6, 100000, expected);
        this.doTest(new Rosen(), startPoint, insigma, boundaries,
            GoalType.MINIMIZE, LAMBDA, false, 0, 1e-13,
            1e-13, 1e-6, 100000, expected);
    }

    @Test
    @Retry(3)
    public void testMaximize() {
        double[] startPoint = point(DIM, 1.0);
        final double[] insigma = point(DIM, 0.1);
        double[][] boundaries = null;
        final PointValuePair expected =
            new PointValuePair(point(DIM, 0.0), 1.0);
        this.doTest(new MinusElli(), startPoint, insigma, boundaries,
            GoalType.MAXIMIZE, LAMBDA, true, 0, 1.0 - 1e-13,
            2e-10, 5e-6, 100000, expected);
        this.doTest(new MinusElli(), startPoint, insigma, boundaries,
            GoalType.MAXIMIZE, LAMBDA, false, 0, 1.0 - 1e-13,
            2e-10, 5e-6, 100000, expected);
        boundaries = boundaries(DIM, -0.3, 0.3);
        startPoint = point(DIM, 0.1);
        this.doTest(new MinusElli(), startPoint, insigma, boundaries,
            GoalType.MAXIMIZE, LAMBDA, true, 0, 1.0 - 1e-13,
            2e-10, 5e-6, 100000, expected);
    }

    @Test
    public void testEllipse() {
        final double[] startPoint = point(DIM, 1.0);
        final double[] insigma = point(DIM, 0.1);
        final double[][] boundaries = null;
        final PointValuePair expected =
            new PointValuePair(point(DIM, 0.0), 0.0);
        this.doTest(new Elli(), startPoint, insigma, boundaries,
            GoalType.MINIMIZE, LAMBDA, true, 0, 1e-13,
            1e-13, 1e-6, 100000, expected);
        this.doTest(new Elli(), startPoint, insigma, boundaries,
            GoalType.MINIMIZE, LAMBDA, false, 0, 1e-13,
            1e-13, 1e-6, 100000, expected);
    }

    @Test
    public void testElliRotated() {
        final double[] startPoint = point(DIM, 1.0);
        final double[] insigma = point(DIM, 0.1);
        final double[][] boundaries = null;
        final PointValuePair expected =
            new PointValuePair(point(DIM, 0.0), 0.0);
        this.doTest(new ElliRotated(), startPoint, insigma, boundaries,
            GoalType.MINIMIZE, LAMBDA, true, 0, 1e-13,
            1e-13, 1e-6, 100000, expected);
        this.doTest(new ElliRotated(), startPoint, insigma, boundaries,
            GoalType.MINIMIZE, LAMBDA, false, 0, 1e-13,
            1e-13, 1e-6, 100000, expected);
    }

    @Test
    public void testCigar() {
        final double[] startPoint = point(DIM, 1.0);
        final double[] insigma = point(DIM, 0.1);
        final double[][] boundaries = null;
        final PointValuePair expected =
            new PointValuePair(point(DIM, 0.0), 0.0);
        this.doTest(new Cigar(), startPoint, insigma, boundaries,
            GoalType.MINIMIZE, LAMBDA, true, 0, 1e-13,
            1e-13, 1e-6, 200000, expected);
        this.doTest(new Cigar(), startPoint, insigma, boundaries,
            GoalType.MINIMIZE, LAMBDA, false, 0, 1e-13,
            1e-13, 1e-6, 100000, expected);
    }

    @Test
    public void testCigarWithBoundaries() {
        final double[] startPoint = point(DIM, 1.0);
        final double[] insigma = point(DIM, 0.1);
        final double[][] boundaries = boundaries(DIM, -1e100, Double.POSITIVE_INFINITY);
        final PointValuePair expected =
            new PointValuePair(point(DIM, 0.0), 0.0);
        this.doTest(new Cigar(), startPoint, insigma, boundaries,
            GoalType.MINIMIZE, LAMBDA, true, 0, 1e-13,
            1e-13, 1e-6, 200000, expected);
        this.doTest(new Cigar(), startPoint, insigma, boundaries,
            GoalType.MINIMIZE, LAMBDA, false, 0, 1e-13,
            1e-13, 1e-6, 100000, expected);
    }

    @Test
    public void testTwoAxes() {
        final double[] startPoint = point(DIM, 1.0);
        final double[] insigma = point(DIM, 0.1);
        final double[][] boundaries = null;
        final PointValuePair expected =
            new PointValuePair(point(DIM, 0.0), 0.0);
        this.doTest(new TwoAxes(), startPoint, insigma, boundaries,
            GoalType.MINIMIZE, 2 * LAMBDA, true, 0, 1e-13,
            1e-13, 1e-6, 200000, expected);
        this.doTest(new TwoAxes(), startPoint, insigma, boundaries,
            GoalType.MINIMIZE, 2 * LAMBDA, false, 0, 1e-13,
            1e-8, 1e-3, 200000, expected);
    }

    @Test
    public void testCigTab() {
        final double[] startPoint = point(DIM, 1.0);
        final double[] insigma = point(DIM, 0.3);
        final double[][] boundaries = null;
        final PointValuePair expected =
            new PointValuePair(point(DIM, 0.0), 0.0);
        this.doTest(new CigTab(), startPoint, insigma, boundaries,
            GoalType.MINIMIZE, LAMBDA, true, 0, 1e-13,
            1e-13, 5e-5, 100000, expected);
        this.doTest(new CigTab(), startPoint, insigma, boundaries,
            GoalType.MINIMIZE, LAMBDA, false, 0, 1e-13,
            1e-13, 5e-5, 100000, expected);
    }

    @Test
    public void testSphere() {
        final double[] startPoint = point(DIM, 1.0);
        final double[] insigma = point(DIM, 0.1);
        final double[][] boundaries = null;
        final PointValuePair expected =
            new PointValuePair(point(DIM, 0.0), 0.0);
        this.doTest(new Sphere(), startPoint, insigma, boundaries,
            GoalType.MINIMIZE, LAMBDA, true, 0, 1e-13,
            1e-13, 1e-6, 100000, expected);
        this.doTest(new Sphere(), startPoint, insigma, boundaries,
            GoalType.MINIMIZE, LAMBDA, false, 0, 1e-13,
            1e-13, 1e-6, 100000, expected);
    }

    @Test
    public void testTablet() {
        final double[] startPoint = point(DIM, 1.0);
        final double[] insigma = point(DIM, 0.1);
        final double[][] boundaries = null;
        final PointValuePair expected =
            new PointValuePair(point(DIM, 0.0), 0.0);
        this.doTest(new Tablet(), startPoint, insigma, boundaries,
            GoalType.MINIMIZE, LAMBDA, true, 0, 1e-13,
            1e-13, 1e-6, 100000, expected);
        this.doTest(new Tablet(), startPoint, insigma, boundaries,
            GoalType.MINIMIZE, LAMBDA, false, 0, 1e-13,
            1e-13, 1e-6, 100000, expected);
    }

    @Test
    public void testDiffPow() {
        final double[] startPoint = point(DIM, 1.0);
        final double[] insigma = point(DIM, 0.1);
        final double[][] boundaries = null;
        final PointValuePair expected =
            new PointValuePair(point(DIM, 0.0), 0.0);
        this.doTest(new DiffPow(), startPoint, insigma, boundaries,
            GoalType.MINIMIZE, 10, true, 0, 1e-13,
            1e-8, 1e-1, 100000, expected);
        this.doTest(new DiffPow(), startPoint, insigma, boundaries,
            GoalType.MINIMIZE, 10, false, 0, 1e-13,
            1e-8, 2e-1, 100000, expected);
    }

    @Test
    public void testSsDiffPow() {
        final double[] startPoint = point(DIM, 1.0);
        final double[] insigma = point(DIM, 0.1);
        final double[][] boundaries = null;
        final PointValuePair expected =
            new PointValuePair(point(DIM, 0.0), 0.0);
        this.doTest(new SsDiffPow(), startPoint, insigma, boundaries,
            GoalType.MINIMIZE, 10, true, 0, 1e-13,
            1e-4, 1e-1, 200000, expected);
        this.doTest(new SsDiffPow(), startPoint, insigma, boundaries,
            GoalType.MINIMIZE, 10, false, 0, 1e-13,
            1e-4, 1e-1, 200000, expected);
    }

    @Test
    public void testAckley() {
        final double[] startPoint = point(DIM, 1.0);
        final double[] insigma = point(DIM, 1.0);
        final double[][] boundaries = null;
        final PointValuePair expected =
            new PointValuePair(point(DIM, 0.0), 0.0);
        this.doTest(new Ackley(), startPoint, insigma, boundaries,
            GoalType.MINIMIZE, 2 * LAMBDA, true, 0, 1e-13,
            1e-9, 1e-5, 100000, expected);
        this.doTest(new Ackley(), startPoint, insigma, boundaries,
            GoalType.MINIMIZE, 2 * LAMBDA, false, 0, 1e-13,
            1e-9, 1e-5, 100000, expected);
    }

    @Test
    public void testRastrigin() {
        final double[] startPoint = point(DIM, 0.1);
        final double[] insigma = point(DIM, 0.1);
        final double[][] boundaries = null;
        final PointValuePair expected =
            new PointValuePair(point(DIM, 0.0), 0.0);
        this.doTest(new Rastrigin(), startPoint, insigma, boundaries,
            GoalType.MINIMIZE, (int) (200 * Math.sqrt(DIM)), true, 0, 1e-13,
            1e-13, 1e-6, 200000, expected);
        this.doTest(new Rastrigin(), startPoint, insigma, boundaries,
            GoalType.MINIMIZE, (int) (200 * Math.sqrt(DIM)), false, 0, 1e-13,
            1e-13, 1e-6, 200000, expected);
    }

    @Test
    public void testConstrainedRosen() {
        final double[] startPoint = point(DIM, 0.1);
        final double[] insigma = point(DIM, 0.1);
        final double[][] boundaries = boundaries(DIM, -1, 2);
        final PointValuePair expected =
            new PointValuePair(point(DIM, 1.0), 0.0);
        this.doTest(new Rosen(), startPoint, insigma, boundaries,
            GoalType.MINIMIZE, 2 * LAMBDA, true, 0, 1e-13,
            1e-13, 1e-6, 100000, expected);
        this.doTest(new Rosen(), startPoint, insigma, boundaries,
            GoalType.MINIMIZE, 2 * LAMBDA, false, 0, 1e-13,
            1e-13, 1e-6, 100000, expected);
    }

    @Test
    public void testDiagonalRosen() {
        final double[] startPoint = point(DIM, 0.1);
        final double[] insigma = point(DIM, 0.1);
        final double[][] boundaries = null;
        final PointValuePair expected =
            new PointValuePair(point(DIM, 1.0), 0.0);
        this.doTest(new Rosen(), startPoint, insigma, boundaries,
            GoalType.MINIMIZE, LAMBDA, false, 1, 1e-13,
            1e-10, 1e-4, 1000000, expected);
    }

    @Test
    public void testMath864() {
        final CMAESOptimizer optimizer = new CMAESOptimizer(30000, 0, true, 10,
            0, new MersenneTwister(), false, null);
        final MultivariateFunction fitnessFunction = new MultivariateFunction(){
            @Override
            public double value(final double[] parameters) {
                final double target = 1;
                final double error = target - parameters[0];
                return error * error;
            }
        };

        final double[] start = { 0 };
        final double[] lower = { -1e6 };
        final double[] upper = { 1.5 };
        final double[] sigma = { 1e-1 };
        final double[] result = optimizer.optimize(new MaxEval(10000),
            new ObjectiveFunction(fitnessFunction),
            GoalType.MINIMIZE,
            new CMAESOptimizer.PopulationSize(5),
            new CMAESOptimizer.Sigma(sigma),
            new InitialGuess(start),
            new SimpleBounds(lower, upper)).getPoint();
        Assert.assertTrue("Out of bounds (" + result[0] + " > " + upper[0] + ")",
            result[0] <= upper[0]);
    }

    /**
     * Cf. MATH-867
     */
    @Test
    public void testFitAccuracyDependsOnBoundary() {
        final CMAESOptimizer optimizer = new CMAESOptimizer(30000, 0, true, 10,
            0, new MersenneTwister(), false, null);
        final MultivariateFunction fitnessFunction = new MultivariateFunction(){
            @Override
            public double value(final double[] parameters) {
                final double target = 11.1;
                final double error = target - parameters[0];
                return error * error;
            }
        };

        final double[] start = { 1 };

        // No bounds.
        PointValuePair result = optimizer.optimize(new MaxEval(100000),
            new ObjectiveFunction(fitnessFunction),
            GoalType.MINIMIZE,
            SimpleBounds.unbounded(1),
            new CMAESOptimizer.PopulationSize(5),
            new CMAESOptimizer.Sigma(new double[] { 1e-1 }),
            new InitialGuess(start));
        final double resNoBound = result.getPoint()[0];

        // Optimum is near the lower bound.
        final double[] lower = { -20 };
        final double[] upper = { 5e16 };
        final double[] sigma = { 10 };
        result = optimizer.optimize(new MaxEval(100000),
            new ObjectiveFunction(fitnessFunction),
            GoalType.MINIMIZE,
            new CMAESOptimizer.PopulationSize(5),
            new CMAESOptimizer.Sigma(sigma),
            new InitialGuess(start),
            new SimpleBounds(lower, upper));
        final double resNearLo = result.getPoint()[0];

        // Optimum is near the upper bound.
        lower[0] = -5e16;
        upper[0] = 20;
        result = optimizer.optimize(new MaxEval(100000),
            new ObjectiveFunction(fitnessFunction),
            GoalType.MINIMIZE,
            new CMAESOptimizer.PopulationSize(5),
            new CMAESOptimizer.Sigma(sigma),
            new InitialGuess(start),
            new SimpleBounds(lower, upper));
        final double resNearHi = result.getPoint()[0];

        // System.out.println("resNoBound=" + resNoBound +
        // " resNearLo=" + resNearLo +
        // " resNearHi=" + resNearHi);

        // The two values currently differ by a substantial amount, indicating that
        // the bounds definition can prevent reaching the optimum.
        Assert.assertEquals(resNoBound, resNearLo, 1e-3);
        Assert.assertEquals(resNoBound, resNearHi, 1e-3);
    }

    /**
     * @param func
     *        Function to optimize.
     * @param startPoint
     *        Starting point.
     * @param inSigma
     *        Individual input sigma.
     * @param boundaries
     *        Upper / lower point limit.
     * @param goal
     *        Minimization or maximization.
     * @param lambda
     *        Population size used for offspring.
     * @param isActive
     *        Covariance update mechanism.
     * @param diagonalOnly
     *        Simplified covariance update.
     * @param stopValue
     *        Termination criteria for optimization.
     * @param fTol
     *        Tolerance relative error on the objective function.
     * @param pointTol
     *        Tolerance for checking that the optimum is correct.
     * @param maxEvaluations
     *        Maximum number of evaluations.
     * @param expected
     *        Expected point / value.
     */
    private void doTest(final MultivariateFunction func,
                        final double[] startPoint,
                        final double[] inSigma,
                        final double[][] boundaries,
                        final GoalType goal,
                        final int lambda,
                        final boolean isActive,
                        final int diagonalOnly,
                        final double stopValue,
                        final double fTol,
                        final double pointTol,
                        final int maxEvaluations,
                        final PointValuePair expected) {
        final int dim = startPoint.length;
        // test diagonalOnly = 0 - slow but normally fewer feval#
        final CMAESOptimizer optim = new CMAESOptimizer(30000, stopValue, isActive, diagonalOnly,
            0, new MersenneTwister(), false, null);
        final PointValuePair result = boundaries == null ?
            optim.optimize(new MaxEval(maxEvaluations),
                new ObjectiveFunction(func),
                goal,
                new InitialGuess(startPoint),
                SimpleBounds.unbounded(dim),
                new CMAESOptimizer.Sigma(inSigma),
                new CMAESOptimizer.PopulationSize(lambda)) :
            optim.optimize(new MaxEval(maxEvaluations),
                new ObjectiveFunction(func),
                goal,
                new SimpleBounds(boundaries[0],
                    boundaries[1]),
                new InitialGuess(startPoint),
                new CMAESOptimizer.Sigma(inSigma),
                new CMAESOptimizer.PopulationSize(lambda));

        // System.out.println("sol=" + Arrays.toString(result.getPoint()));
        Assert.assertEquals(expected.getValue(), result.getValue(), fTol);
        for (int i = 0; i < dim; i++) {
            Assert.assertEquals(expected.getPoint()[i], result.getPoint()[i], pointTol);
        }
    }

    private static double[] point(final int n, final double value) {
        final double[] ds = new double[n];
        Arrays.fill(ds, value);
        return ds;
    }

    private static double[][] boundaries(final int dim,
                                         final double lower, final double upper) {
        final double[][] boundaries = new double[2][dim];
        for (int i = 0; i < dim; i++) {
            boundaries[0][i] = lower;
        }
        for (int i = 0; i < dim; i++) {
            boundaries[1][i] = upper;
        }
        return boundaries;
    }

    private static class Sphere implements MultivariateFunction {

        @Override
        public double value(final double[] x) {
            double f = 0;
            for (final double element : x) {
                f += element * element;
            }
            return f;
        }
    }

    private static class Cigar implements MultivariateFunction {
        private final double factor;

        Cigar() {
            this(1e3);
        }

        Cigar(final double axisratio) {
            this.factor = axisratio * axisratio;
        }

        @Override
        public double value(final double[] x) {
            double f = x[0] * x[0];
            for (int i = 1; i < x.length; ++i) {
                f += this.factor * x[i] * x[i];
            }
            return f;
        }
    }

    private static class Tablet implements MultivariateFunction {
        private final double factor;

        Tablet() {
            this(1e3);
        }

        Tablet(final double axisratio) {
            this.factor = axisratio * axisratio;
        }

        @Override
        public double value(final double[] x) {
            double f = this.factor * x[0] * x[0];
            for (int i = 1; i < x.length; ++i) {
                f += x[i] * x[i];
            }
            return f;
        }
    }

    private static class CigTab implements MultivariateFunction {
        private final double factor;

        CigTab() {
            this(1e4);
        }

        CigTab(final double axisratio) {
            this.factor = axisratio;
        }

        @Override
        public double value(final double[] x) {
            final int end = x.length - 1;
            double f = x[0] * x[0] / this.factor + this.factor * x[end] * x[end];
            for (int i = 1; i < end; ++i) {
                f += x[i] * x[i];
            }
            return f;
        }
    }

    private static class TwoAxes implements MultivariateFunction {

        private final double factor;

        TwoAxes() {
            this(1e6);
        }

        TwoAxes(final double axisratio) {
            this.factor = axisratio * axisratio;
        }

        @Override
        public double value(final double[] x) {
            double f = 0;
            for (int i = 0; i < x.length; ++i) {
                f += (i < x.length / 2 ? this.factor : 1) * x[i] * x[i];
            }
            return f;
        }
    }

    private static class ElliRotated implements MultivariateFunction {
        private final Basis B = new Basis();
        private final double factor;

        ElliRotated() {
            this(1e3);
        }

        ElliRotated(final double axisratio) {
            this.factor = axisratio * axisratio;
        }

        @Override
        public double value(double[] x) {
            double f = 0;
            x = this.B.Rotate(x);
            for (int i = 0; i < x.length; ++i) {
                f += Math.pow(this.factor, i / (x.length - 1.)) * x[i] * x[i];
            }
            return f;
        }
    }

    private static class Elli implements MultivariateFunction {

        private final double factor;

        Elli() {
            this(1e3);
        }

        Elli(final double axisratio) {
            this.factor = axisratio * axisratio;
        }

        @Override
        public double value(final double[] x) {
            double f = 0;
            for (int i = 0; i < x.length; ++i) {
                f += Math.pow(this.factor, i / (x.length - 1.)) * x[i] * x[i];
            }
            return f;
        }
    }

    private static class MinusElli implements MultivariateFunction {

        @Override
        public double value(final double[] x) {
            return 1.0 - (new Elli().value(x));
        }
    }

    private static class DiffPow implements MultivariateFunction {

        @Override
        public double value(final double[] x) {
            double f = 0;
            for (int i = 0; i < x.length; ++i) {
                f += Math.pow(Math.abs(x[i]), 2. + 10 * (double) i
                    / (x.length - 1.));
            }
            return f;
        }
    }

    private static class SsDiffPow implements MultivariateFunction {

        @Override
        public double value(final double[] x) {
            final double f = Math.pow(new DiffPow().value(x), 0.25);
            return f;
        }
    }

    private static class Rosen implements MultivariateFunction {

        @Override
        public double value(final double[] x) {
            double f = 0;
            for (int i = 0; i < x.length - 1; ++i) {
                f += 1e2 * (x[i] * x[i] - x[i + 1]) * (x[i] * x[i] - x[i + 1])
                    + (x[i] - 1.) * (x[i] - 1.);
            }
            return f;
        }
    }

    private static class Ackley implements MultivariateFunction {
        private final double axisratio;

        Ackley(final double axra) {
            this.axisratio = axra;
        }

        public Ackley() {
            this(1);
        }

        @Override
        public double value(final double[] x) {
            double f = 0;
            double res2 = 0;
            double fac = 0;
            for (int i = 0; i < x.length; ++i) {
                fac = Math.pow(this.axisratio, (i - 1.) / (x.length - 1.));
                f += fac * fac * x[i] * x[i];
                res2 += Math.cos(2. * Math.PI * fac * x[i]);
            }
            f = (20. - 20. * Math.exp(-0.2 * Math.sqrt(f / x.length))
                + Math.exp(1.) - Math.exp(res2 / x.length));
            return f;
        }
    }

    private static class Rastrigin implements MultivariateFunction {

        private final double axisratio;
        private final double amplitude;

        Rastrigin() {
            this(1, 10);
        }

        Rastrigin(final double axisratio, final double amplitude) {
            this.axisratio = axisratio;
            this.amplitude = amplitude;
        }

        @Override
        public double value(final double[] x) {
            double f = 0;
            double fac;
            for (int i = 0; i < x.length; ++i) {
                fac = Math.pow(this.axisratio, (i - 1.) / (x.length - 1.));
                if (i == 0 && x[i] < 0) {
                    fac *= 1.;
                }
                f += fac * fac * x[i] * x[i] + this.amplitude
                    * (1. - Math.cos(2. * Math.PI * fac * x[i]));
            }
            return f;
        }
    }

    private static class Basis {
        double[][] basis;
        Random rand = new Random(2); // use not always the same basis

        double[] Rotate(final double[] x) {
            this.GenBasis(x.length);
            final double[] y = new double[x.length];
            for (int i = 0; i < x.length; ++i) {
                y[i] = 0;
                for (int j = 0; j < x.length; ++j) {
                    y[i] += this.basis[i][j] * x[j];
                }
            }
            return y;
        }

        void GenBasis(final int DIM) {
            if (this.basis != null ? this.basis.length == DIM : false) {
                return;
            }

            double sp;
            int i, j, k;

            /* generate orthogonal basis */
            this.basis = new double[DIM][DIM];
            for (i = 0; i < DIM; ++i) {
                /* sample components gaussian */
                for (j = 0; j < DIM; ++j) {
                    this.basis[i][j] = this.rand.nextGaussian();
                }
                /* substract projection of previous vectors */
                for (j = i - 1; j >= 0; --j) {
                    for (sp = 0., k = 0; k < DIM; ++k) {
                        sp += this.basis[i][k] * this.basis[j][k]; /* scalar product */
                    }
                    for (k = 0; k < DIM; ++k) {
                        this.basis[i][k] -= sp * this.basis[j][k]; /* substract */
                    }
                }
                /* normalize */
                for (sp = 0., k = 0; k < DIM; ++k) {
                    sp += this.basis[i][k] * this.basis[i][k]; /* squared norm */
                }
                for (k = 0; k < DIM; ++k) {
                    this.basis[i][k] /= Math.sqrt(sp);
                }
            }
        }
    }
}
