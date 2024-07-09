/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
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
package fr.cnes.sirius.patrius.math.optim.nonlinear.scalar.noderiv;

import java.util.Arrays;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.analysis.MultivariateFunction;
import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooLargeException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;
import fr.cnes.sirius.patrius.math.exception.TooManyEvaluationsException;
import fr.cnes.sirius.patrius.math.optim.InitialGuess;
import fr.cnes.sirius.patrius.math.optim.MaxEval;
import fr.cnes.sirius.patrius.math.optim.PointValuePair;
import fr.cnes.sirius.patrius.math.optim.SimpleBounds;
import fr.cnes.sirius.patrius.math.optim.nonlinear.scalar.GoalType;
import fr.cnes.sirius.patrius.math.optim.nonlinear.scalar.ObjectiveFunction;

/**
 * Test for {@link BOBYQAOptimizer}.
 */
public class BOBYQAOptimizerTest {

    static final int DIM = 13;

    @Test(expected = NumberIsTooLargeException.class)
    public void testInitOutOfBounds() {
        final double[] startPoint = point(DIM, 3);
        final double[][] boundaries = boundaries(DIM, -1, 2);
        this.doTest(new Rosen(), startPoint, boundaries,
            GoalType.MINIMIZE,
            1e-13, 1e-6, 2000, null);
    }

    @Test(expected = DimensionMismatchException.class)
    public void testBoundariesDimensionMismatch() {
        final double[] startPoint = point(DIM, 0.5);
        final double[][] boundaries = boundaries(DIM + 1, -1, 2);
        this.doTest(new Rosen(), startPoint, boundaries,
            GoalType.MINIMIZE,
            1e-13, 1e-6, 2000, null);
    }

    @Test(expected = NumberIsTooSmallException.class)
    public void testProblemDimensionTooSmall() {
        final double[] startPoint = point(1, 0.5);
        this.doTest(new Rosen(), startPoint, null,
            GoalType.MINIMIZE,
            1e-13, 1e-6, 2000, null);
    }

    @Test(expected = TooManyEvaluationsException.class)
    public void testMaxEvaluations() {
        final int lowMaxEval = 2;
        final double[] startPoint = point(DIM, 0.1);
        final double[][] boundaries = null;
        this.doTest(new Rosen(), startPoint, boundaries,
            GoalType.MINIMIZE,
            1e-13, 1e-6, lowMaxEval, null);
    }

    @Test
    public void testRosen() {
        final double[] startPoint = point(DIM, 0.1);
        final double[][] boundaries = null;
        final PointValuePair expected = new PointValuePair(point(DIM, 1.0), 0.0);
        this.doTest(new Rosen(), startPoint, boundaries,
            GoalType.MINIMIZE,
            1e-13, 1e-6, 2000, expected);
    }

    @Test
    public void testMaximize() {
        double[] startPoint = point(DIM, 1.0);
        double[][] boundaries = null;
        final PointValuePair expected = new PointValuePair(point(DIM, 0.0), 1.0);
        this.doTest(new MinusElli(), startPoint, boundaries,
            GoalType.MAXIMIZE,
            2e-10, 5e-6, 1000, expected);
        boundaries = boundaries(DIM, -0.3, 0.3);
        startPoint = point(DIM, 0.1);
        this.doTest(new MinusElli(), startPoint, boundaries,
            GoalType.MAXIMIZE,
            2e-10, 5e-6, 1000, expected);
    }

    @Test
    public void testEllipse() {
        final double[] startPoint = point(DIM, 1.0);
        final double[][] boundaries = null;
        final PointValuePair expected =
            new PointValuePair(point(DIM, 0.0), 0.0);
        this.doTest(new Elli(), startPoint, boundaries,
            GoalType.MINIMIZE,
            1e-13, 1e-6, 1000, expected);
    }

    @Test
    public void testElliRotated() {
        final double[] startPoint = point(DIM, 1.0);
        final double[][] boundaries = null;
        final PointValuePair expected =
            new PointValuePair(point(DIM, 0.0), 0.0);
        this.doTest(new ElliRotated(), startPoint, boundaries,
            GoalType.MINIMIZE,
            1e-12, 1e-6, 10000, expected);
    }

    @Test
    public void testCigar() {
        final double[] startPoint = point(DIM, 1.0);
        final double[][] boundaries = null;
        final PointValuePair expected =
            new PointValuePair(point(DIM, 0.0), 0.0);
        this.doTest(new Cigar(), startPoint, boundaries,
            GoalType.MINIMIZE,
            1e-13, 1e-6, 100, expected);
    }

    @Test
    public void testTwoAxes() {
        final double[] startPoint = point(DIM, 1.0);
        final double[][] boundaries = null;
        final PointValuePair expected =
            new PointValuePair(point(DIM, 0.0), 0.0);
        this.doTest(new TwoAxes(), startPoint, boundaries,
            GoalType.MINIMIZE, 2 *
            1e-13, 1e-6, 100, expected);
    }

    @Test
    public void testCigTab() {
        final double[] startPoint = point(DIM, 1.0);
        final double[][] boundaries = null;
        final PointValuePair expected =
            new PointValuePair(point(DIM, 0.0), 0.0);
        this.doTest(new CigTab(), startPoint, boundaries,
            GoalType.MINIMIZE,
            1e-13, 5e-5, 100, expected);
    }

    @Test
    public void testSphere() {
        final double[] startPoint = point(DIM, 1.0);
        final double[][] boundaries = null;
        final PointValuePair expected =
            new PointValuePair(point(DIM, 0.0), 0.0);
        this.doTest(new Sphere(), startPoint, boundaries,
            GoalType.MINIMIZE,
            1e-13, 1e-6, 100, expected);
    }

    @Test
    public void testTablet() {
        final double[] startPoint = point(DIM, 1.0);
        final double[][] boundaries = null;
        final PointValuePair expected =
            new PointValuePair(point(DIM, 0.0), 0.0);
        this.doTest(new Tablet(), startPoint, boundaries,
            GoalType.MINIMIZE,
            1e-13, 1e-6, 100, expected);
    }

    @Test
    public void testDiffPow() {
        final double[] startPoint = point(DIM / 2, 1.0);
        final double[][] boundaries = null;
        final PointValuePair expected =
            new PointValuePair(point(DIM / 2, 0.0), 0.0);
        this.doTest(new DiffPow(), startPoint, boundaries,
            GoalType.MINIMIZE,
            1e-8, 1e-1, 120000, expected);
    }

    @Test
    public void testSsDiffPow() {
        final double[] startPoint = point(DIM / 2, 1.0);
        final double[][] boundaries = null;
        final PointValuePair expected =
            new PointValuePair(point(DIM / 2, 0.0), 0.0);
        this.doTest(new SsDiffPow(), startPoint, boundaries,
            GoalType.MINIMIZE,
            1e-2, 1.3e-1, 50000, expected);
    }

    @Test
    public void testAckley() {
        final double[] startPoint = point(DIM, 0.1);
        final double[][] boundaries = null;
        final PointValuePair expected =
            new PointValuePair(point(DIM, 0.0), 0.0);
        this.doTest(new Ackley(), startPoint, boundaries,
            GoalType.MINIMIZE,
            1.1e-8, 1e-5, 1000, expected);
    }

    @Test
    public void testRastrigin() {
        final double[] startPoint = point(DIM, 1.0);

        final double[][] boundaries = null;
        final PointValuePair expected =
            new PointValuePair(point(DIM, 0.0), 0.0);
        this.doTest(new Rastrigin(), startPoint, boundaries,
            GoalType.MINIMIZE,
            1e-13, 1e-6, 1000, expected);
    }

    @Test
    public void testConstrainedRosen() {
        final double[] startPoint = point(DIM, 0.1);

        final double[][] boundaries = boundaries(DIM, -1, 2);
        final PointValuePair expected =
            new PointValuePair(point(DIM, 1.0), 0.0);
        this.doTest(new Rosen(), startPoint, boundaries,
            GoalType.MINIMIZE,
            1e-13, 1e-6, 2000, expected);
    }

    // See MATH-728
    @Test
    public void testConstrainedRosenWithMoreInterpolationPoints() {
        final double[] startPoint = point(DIM, 0.1);
        final double[][] boundaries = boundaries(DIM, -1, 2);
        final PointValuePair expected = new PointValuePair(point(DIM, 1.0), 0.0);

        // This should have been 78 because in the code the hard limit is
        // said to be
        // ((DIM + 1) * (DIM + 2)) / 2 - (2 * DIM + 1)
        // i.e. 78 in this case, but the test fails for 48, 59, 62, 63, 64,
        // 65, 66, ...
        final int maxAdditionalPoints = 47;

        for (int num = 1; num <= maxAdditionalPoints; num++) {
            this.doTest(new Rosen(), startPoint, boundaries,
                GoalType.MINIMIZE,
                1e-12, 1e-6, 2000,
                num,
                expected,
                "num=" + num);
        }
    }

    /**
     * @param func
     *        Function to optimize.
     * @param startPoint
     *        Starting point.
     * @param boundaries
     *        Upper / lower point limit.
     * @param goal
     *        Minimization or maximization.
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
                        final double[][] boundaries,
                        final GoalType goal,
                        final double fTol,
                        final double pointTol,
                        final int maxEvaluations,
                        final PointValuePair expected) {
        this.doTest(func,
            startPoint,
            boundaries,
            goal,
            fTol,
            pointTol,
            maxEvaluations,
            0,
            expected,
            "");
    }

    /**
     * @param func
     *        Function to optimize.
     * @param startPoint
     *        Starting point.
     * @param boundaries
     *        Upper / lower point limit.
     * @param goal
     *        Minimization or maximization.
     * @param fTol
     *        Tolerance relative error on the objective function.
     * @param pointTol
     *        Tolerance for checking that the optimum is correct.
     * @param maxEvaluations
     *        Maximum number of evaluations.
     * @param additionalInterpolationPoints
     *        Number of interpolation to used
     *        in addition to the default (2 * dim + 1).
     * @param expected
     *        Expected point / value.
     */
    private void doTest(final MultivariateFunction func,
                        final double[] startPoint,
                        final double[][] boundaries,
                        final GoalType goal,
                        final double fTol,
                        final double pointTol,
                        final int maxEvaluations,
                        final int additionalInterpolationPoints,
                        final PointValuePair expected,
                        final String assertMsg) {

        // System.out.println(func.getClass().getName() + " BEGIN"); // XXX

        final int dim = startPoint.length;
        final int numIterpolationPoints = 2 * dim + 1 + additionalInterpolationPoints;
        final BOBYQAOptimizer optim = new BOBYQAOptimizer(numIterpolationPoints);
        final PointValuePair result = boundaries == null ?
            optim.optimize(new MaxEval(maxEvaluations),
                new ObjectiveFunction(func),
                goal,
                SimpleBounds.unbounded(dim),
                new InitialGuess(startPoint)) :
            optim.optimize(new MaxEval(maxEvaluations),
                new ObjectiveFunction(func),
                goal,
                new InitialGuess(startPoint),
                new SimpleBounds(boundaries[0],
                    boundaries[1]));
        // System.out.println(func.getClass().getName() + " = "
        // + optim.getEvaluations() + " f(");
        // for (double x: result.getPoint()) System.out.print(x + " ");
        // System.out.println(") = " + result.getValue());
        Assert.assertEquals(assertMsg, expected.getValue(), result.getValue(), fTol);
        for (int i = 0; i < dim; i++) {
            Assert.assertEquals(expected.getPoint()[i],
                result.getPoint()[i], pointTol);
        }

        // System.out.println(func.getClass().getName() + " END"); // XXX
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
        private final Elli elli = new Elli();

        @Override
        public double value(final double[] x) {
            return 1.0 - this.elli.value(x);
        }
    }

    private static class DiffPow implements MultivariateFunction {
        // private int fcount = 0;
        @Override
        public double value(final double[] x) {
            double f = 0;
            for (int i = 0; i < x.length; ++i) {
                f += Math.pow(Math.abs(x[i]), 2. + 10 * (double) i
                    / (x.length - 1.));
            }
            // System.out.print("" + (fcount++) + ") ");
            // for (int i = 0; i < x.length; i++)
            // System.out.print(x[i] + " ");
            // System.out.println(" = " + f);
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
