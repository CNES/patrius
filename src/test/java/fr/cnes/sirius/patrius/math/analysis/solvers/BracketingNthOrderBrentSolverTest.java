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
 * 
 * @history created 16/11/17
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.4:DM:DM-2153:04/10/2019:[PATRIUS] PVCoordinatePropagator
 * VERSION:4.3.1:FA:FA-2136:11/07/2019:[PATRIUS] Exception NumberIsTooLarge lors de la propagation
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1305:16/11/2017: Serializable interface implementation
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.solvers;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.analysis.QuinticFunction;
import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.differentiation.DerivativeStructure;
import fr.cnes.sirius.patrius.math.analysis.differentiation.UnivariateDifferentiableFunction;
import fr.cnes.sirius.patrius.math.analysis.solver.AllowedSolution;
import fr.cnes.sirius.patrius.math.analysis.solver.BracketingNthOrderBrentSolver;
import fr.cnes.sirius.patrius.math.analysis.solver.NewtonRaphsonSolver;
import fr.cnes.sirius.patrius.math.analysis.solver.UnivariateSolver;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooLargeException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;
import fr.cnes.sirius.patrius.math.exception.TooManyEvaluationsException;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Test case for {@link BracketingNthOrderBrentSolver bracketing n<sup>th</sup> order Brent} solver.
 * 
 * @version $Id: BracketingNthOrderBrentSolverTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
public final class BracketingNthOrderBrentSolverTest extends BaseSecantSolverAbstractTest {
    /** {@inheritDoc} */
    @Override
    protected UnivariateSolver getSolver() {
        return new BracketingNthOrderBrentSolver();
    }

    /** {@inheritDoc} */
    @Override
    protected int[] getQuinticEvalCounts() {
        return new int[] { 1, 3, 8, 1, 9, 4, 8, 1, 12, 1, 16 };
    }

    @Test(expected = NumberIsTooSmallException.class)
    public void testInsufficientOrder1() {
        new BracketingNthOrderBrentSolver(1.0e-10, 1);
    }

    @Test(expected = NumberIsTooSmallException.class)
    public void testInsufficientOrder2() {
        new BracketingNthOrderBrentSolver(1.0e-10, 1.0e-10, 1);
    }

    @Test(expected = NumberIsTooSmallException.class)
    public void testInsufficientOrder3() {
        new BracketingNthOrderBrentSolver(1.0e-10, 1.0e-10, 1.0e-10, 1);
    }

    @Test
    public void testConstructorsOK() {
        Assert.assertEquals(2, new BracketingNthOrderBrentSolver(1.0e-10, 2).getMaximalOrder());
        Assert.assertEquals(2,
                new BracketingNthOrderBrentSolver(1.0e-10, 1.0e-10, 2).getMaximalOrder());
        Assert.assertEquals(2,
                new BracketingNthOrderBrentSolver(1.0e-10, 1.0e-10, 1.0e-10, 2).getMaximalOrder());
    }

    @Test
    public void testConvergenceOnFunctionAccuracy() {
        final BracketingNthOrderBrentSolver solver = new BracketingNthOrderBrentSolver(1.0e-12,
                1.0e-10, 0.001, 3);
        final QuinticFunction f = new QuinticFunction();
        double result = solver.solve(20, f, 0.2, 0.9, 0.4, AllowedSolution.BELOW_SIDE);
        Assert.assertEquals(0, f.value(result), solver.getFunctionValueAccuracy());
        Assert.assertTrue(f.value(result) <= 0);
        Assert.assertTrue(result - 0.5 > solver.getAbsoluteAccuracy());
        result = solver.solve(20, f, -0.9, -0.2, -0.4, AllowedSolution.ABOVE_SIDE);
        Assert.assertEquals(0, f.value(result), solver.getFunctionValueAccuracy());
        Assert.assertTrue(f.value(result) >= 0);
        Assert.assertTrue(result + 0.5 < -solver.getAbsoluteAccuracy());
    }

    @Test
    public void testIssue716() {
        final BracketingNthOrderBrentSolver solver = new BracketingNthOrderBrentSolver(1.0e-12,
                1.0e-10, 1.0e-22, 5);
        final UnivariateFunction sharpTurn = new UnivariateFunction() {
            /** Serializable UID. */
            private static final long serialVersionUID = 7854712853613658394L;

            @Override
            public double value(final double x) {
                return (2 * x + 1) / (1.0e9 * (x + 1));
            }
        };
        final double result = solver.solve(100, sharpTurn, -0.9999999, 30, 15,
                AllowedSolution.RIGHT_SIDE);
        Assert.assertEquals(0, sharpTurn.value(result), solver.getFunctionValueAccuracy());
        Assert.assertTrue(sharpTurn.value(result) >= 0);
        Assert.assertEquals(-0.5, result, 1.0e-10);
    }

    @Test
    public void testULPAccuracy() {

        final double ULPtarget = 1000.;
        final double oneULP = MathLib.nextAfter(ULPtarget, Double.POSITIVE_INFINITY) - ULPtarget;
        final BracketingNthOrderBrentSolver solver = new BracketingNthOrderBrentSolver(1.0e-12,
                1.0e-10, oneULP, 3);
        final double anyValueBtwOneULP = oneULP * 0.3;

        try {
            final UnivariateFunction f = new TestStepFunction(ULPtarget, anyValueBtwOneULP);
            final double result = solver.solve(20, f, ULPtarget, (ULPtarget + oneULP), ULPtarget,
                    AllowedSolution.BELOW_SIDE);
            Assert.assertEquals(ULPtarget, result, 0.);
        } catch (final NumberIsTooLargeException e) {
            Assert.fail();
        }
    }

    /**
     * This function is specifically developed for
     * {@link BracketingNthOrderBrentSolverTest#testULPAccuracy} test.
     * It represents a step function with a root at <cite>x = target + step</cite>.
     * In the current use, the step should be set as <cite>0 < step < 1ULP</cite> but the actual
     * value doesn't matter.
     * The solver which uses this function, should be set with such limits : min = target / max =
     * target + 1ULP.
     */
    @SuppressWarnings("serial")
    public class TestStepFunction implements UnivariateDifferentiableFunction {

        /** Serializable UID. */
        private static final long serialVersionUID = 7234860444102652783L;
        double step;
        double target;

        public TestStepFunction(final double target, final double step) {
            this.step = step;
            this.target = target;
        }

        @Override
        public double value(double x) {
            double value;
            x = x - this.target;
            if (x <= this.step) {
                value = -1;
            } else {
                value = 1;
            }

            return value;
        }

        @Override
        public DerivativeStructure value(final DerivativeStructure t) {
            return t.multiply(0.);
        }
    }

    @Test
    public void testFasterThanNewton() {
        // the following test functions come from Beny Neta's paper:
        // "Several New Methods for solving Equations"
        // intern J. Computer Math Vol 23 pp 265-282
        // available here: http://www.math.nps.navy.mil/~bneta/SeveralNewMethods.PDF
        // the reference roots have been computed by the Dfp solver to more than
        // 80 digits and checked with emacs (only the first 20 digits are reproduced here)
        compare(new TestFunction(0.0, -2, 2) {
            /** Serializable UID. */
            private static final long serialVersionUID = 7171625402515151071L;

            @Override
            public DerivativeStructure value(final DerivativeStructure x) {
                return x.sin().subtract(x.multiply(0.5));
            }
        });
        compare(new TestFunction(6.3087771299726890947, -5, 10) {
            /** Serializable UID. */
            private static final long serialVersionUID = 6733230934894755318L;

            @Override
            public DerivativeStructure value(final DerivativeStructure x) {
                return x.pow(5).add(x).subtract(10000);
            }
        });
        compare(new TestFunction(9.6335955628326951924, 0.001, 10) {
            /** Serializable UID. */
            private static final long serialVersionUID = 7708253975409411839L;

            @Override
            public DerivativeStructure value(final DerivativeStructure x) {
                return x.sqrt().subtract(x.reciprocal()).subtract(3);
            }
        });
        compare(new TestFunction(2.8424389537844470678, -5, 5) {
            /** Serializable UID. */
            private static final long serialVersionUID = -3820844208588419005L;

            @Override
            public DerivativeStructure value(final DerivativeStructure x) {
                return x.exp().add(x).subtract(20);
            }
        });
        compare(new TestFunction(8.3094326942315717953, 0.001, 10) {
            /** Serializable UID. */
            private static final long serialVersionUID = -3832852629200907442L;

            @Override
            public DerivativeStructure value(final DerivativeStructure x) {
                return x.log().add(x.sqrt()).subtract(5);
            }
        });
        compare(new TestFunction(1.4655712318767680266, -0.5, 1.5) {
            /** Serializable UID. */
            private static final long serialVersionUID = 5617871916012051378L;

            @Override
            public DerivativeStructure value(final DerivativeStructure x) {
                return x.subtract(1).multiply(x).multiply(x).subtract(1);
            }
        });

    }

    @Test
    public void testSerialization() {
        // Random BracketingNthOrderBrentSolver
        final BracketingNthOrderBrentSolver solver = new BracketingNthOrderBrentSolver(1.0e-12,
                1.0e-10, 1.0e-22, 5);

        // Creation of solver2 for serialization test purpose
        final BracketingNthOrderBrentSolver solver2 = TestUtils.serializeAndRecover(solver);

        // Test between the 2 objects
        Assert.assertEquals(solver.getAbsoluteAccuracy(), solver2.getAbsoluteAccuracy(), 0.);
        Assert.assertEquals(solver.getFunctionValueAccuracy(), solver2.getFunctionValueAccuracy(),
                0.);
        Assert.assertEquals(solver.getMax(), solver2.getMax(), 0.);
        Assert.assertEquals(solver.getMin(), solver2.getMin(), 0.);
        Assert.assertEquals(solver.getRelativeAccuracy(), solver2.getRelativeAccuracy(), 0.);
        Assert.assertEquals(solver.getStartValue(), solver2.getStartValue(), 0.);
        Assert.assertEquals(solver.getEvaluations(), solver2.getEvaluations(), 0);
        Assert.assertEquals(solver.getMaxEvaluations(), solver2.getMaxEvaluations(), 0);
        Assert.assertEquals(solver.getMaximalOrder(), solver2.getMaximalOrder(), 0);

    }

    private static void compare(final TestFunction f) {
        compare(f, f.getRoot(), f.getMin(), f.getMax());
    }

    private static void compare(final UnivariateDifferentiableFunction f, final double root,
            final double min, final double max) {
        final NewtonRaphsonSolver newton = new NewtonRaphsonSolver(1.0e-12);
        final BracketingNthOrderBrentSolver bracketing = new BracketingNthOrderBrentSolver(1.0e-12,
                1.0e-12, 1.0e-18, 5);
        double resultN;
        try {
            resultN = newton.solve(100, f, min, max);
        } catch (final TooManyEvaluationsException tmee) {
            resultN = Double.NaN;
        }
        double resultB;
        try {
            resultB = bracketing.solve(100, f, min, max);
        } catch (final TooManyEvaluationsException tmee) {
            resultB = Double.NaN;
        }
        Assert.assertEquals(root, resultN, newton.getAbsoluteAccuracy());
        Assert.assertEquals(root, resultB, bracketing.getAbsoluteAccuracy());

        // bracketing solver evaluates only function value, we set the weight to 1
        final int weightedBracketingEvaluations = bracketing.getEvaluations();

        // Newton-Raphson solver evaluates both function value and derivative, we set the weight to
        // 2
        final int weightedNewtonEvaluations = 2 * newton.getEvaluations();

        Assert.assertTrue(weightedBracketingEvaluations < weightedNewtonEvaluations);

    }

    private static abstract class TestFunction implements UnivariateDifferentiableFunction {

        /** Serializable UID. */
        private static final long serialVersionUID = -7045824137106843848L;
        private final double root;
        private final double min;
        private final double max;

        protected TestFunction(final double root, final double min, final double max) {
            this.root = root;
            this.min = min;
            this.max = max;
        }

        public double getRoot() {
            return this.root;
        }

        public double getMin() {
            return this.min;
        }

        public double getMax() {
            return this.max;
        }

        @Override
        public double value(final double x) {
            return value(new DerivativeStructure(0, 0, x)).getValue();
        }

        @Override
        public abstract DerivativeStructure value(final DerivativeStructure t);
    }
}
