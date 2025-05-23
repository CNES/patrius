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
package fr.cnes.sirius.patrius.math.analysis.solvers;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.analysis.QuinticFunction;
import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.XMinus5Function;
import fr.cnes.sirius.patrius.math.analysis.function.Sin;
import fr.cnes.sirius.patrius.math.analysis.solver.AllowedSolution;
import fr.cnes.sirius.patrius.math.analysis.solver.BaseSecantSolver;
import fr.cnes.sirius.patrius.math.analysis.solver.BracketedUnivariateSolver;
import fr.cnes.sirius.patrius.math.analysis.solver.PegasusSolver;
import fr.cnes.sirius.patrius.math.analysis.solver.UnivariateSolver;
import fr.cnes.sirius.patrius.math.analysis.solver.UnivariateSolverUtils;
import fr.cnes.sirius.patrius.math.exception.NoBracketingException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooLargeException;
import fr.cnes.sirius.patrius.math.util.FastMath;

/**
 * Base class for root-finding algorithms tests derived from {@link BaseSecantSolver}.
 * 
 * @version $Id: BaseSecantSolverAbstractTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
public abstract class BaseSecantSolverAbstractTest {
    /**
     * Returns the solver to use to perform the tests.
     * 
     * @return the solver to use to perform the tests
     */
    protected abstract UnivariateSolver getSolver();

    /**
     * Returns the expected number of evaluations for the {@link #testQuinticZero} unit test. A value of {@code -1}
     * indicates that
     * the test should be skipped for that solver.
     * 
     * @return the expected number of evaluations for the {@link #testQuinticZero} unit test
     */
    protected abstract int[] getQuinticEvalCounts();

    @Test
    public void testSinZero() {
        // The sinus function is behaved well around the root at pi. The second
        // order derivative is zero, which means linear approximating methods
        // still converge quadratically.
        final UnivariateFunction f = new Sin();
        double result;
        final UnivariateSolver solver = this.getSolver();

        result = solver.solve(100, f, 3, 4);
        // System.out.println(
        // "Root: " + result + " Evaluations: " + solver.getEvaluations());
        Assert.assertEquals(result, FastMath.PI, solver.getAbsoluteAccuracy());
        Assert.assertTrue(solver.getEvaluations() <= 6);
        result = solver.solve(100, f, 1, 4);
        // System.out.println(
        // "Root: " + result + " Evaluations: " + solver.getEvaluations());
        Assert.assertEquals(result, FastMath.PI, solver.getAbsoluteAccuracy());
        Assert.assertTrue(solver.getEvaluations() <= 7);
    }

    @Test
    public void testQuinticZero() {
        // The quintic function has zeros at 0, +-0.5 and +-1.
        // Around the root of 0 the function is well behaved, with a second
        // derivative of zero a 0.
        // The other roots are less well to find, in particular the root at 1,
        // because the function grows fast for x>1.
        // The function has extrema (first derivative is zero) at 0.27195613
        // and 0.82221643, intervals containing these values are harder for
        // the solvers.
        final UnivariateFunction f = new QuinticFunction();
        double result;
        final UnivariateSolver solver = this.getSolver();
        final double atol = solver.getAbsoluteAccuracy();
        final int[] counts = this.getQuinticEvalCounts();

        // Tests data: initial bounds, and expected solution, per test case.
        final double[][] testsData = { { -0.2, 0.2, 0.0 },
            { -0.1, 0.3, 0.0 },
            { -0.3, 0.45, 0.0 },
            { 0.3, 0.7, 0.5 },
            { 0.2, 0.6, 0.5 },
            { 0.05, 0.95, 0.5 },
            { 0.85, 1.25, 1.0 },
            { 0.8, 1.2, 1.0 },
            { 0.85, 1.75, 1.0 },
            { 0.55, 1.45, 1.0 },
            { 0.85, 5.0, 1.0 },
        };
        final int maxIter = 500;

        for (int i = 0; i < testsData.length; i++) {
            // Skip test, if needed.
            if (counts[i] == -1) {
                continue;
            }

            // Compute solution.
            final double[] testData = testsData[i];
            result = solver.solve(maxIter, f, testData[0], testData[1]);
            // System.out.println(
            // "Root: " + result + " Evaluations: " + solver.getEvaluations());

            // Check solution.
            Assert.assertEquals(result, testData[2], atol);
            Assert.assertTrue(solver.getEvaluations() <= counts[i] + 1);
        }
    }

    @Test
    public void testRootEndpoints() {
        final UnivariateFunction f = new XMinus5Function();
        final UnivariateSolver solver = this.getSolver();

        // End-point is root. This should be a special case in the solver, and
        // the initial end-point should be returned exactly.
        double result = solver.solve(100, f, 5.0, 6.0);
        Assert.assertEquals(5.0, result, 0.0);

        result = solver.solve(100, f, 4.0, 5.0);
        Assert.assertEquals(5.0, result, 0.0);

        result = solver.solve(100, f, 5.0, 6.0, 5.5);
        Assert.assertEquals(5.0, result, 0.0);

        result = solver.solve(100, f, 4.0, 5.0, 4.5);
        Assert.assertEquals(5.0, result, 0.0);
    }

    @Test
    public void testBadEndpoints() {
        final UnivariateFunction f = new Sin();
        final UnivariateSolver solver = this.getSolver();
        try { // bad interval
            solver.solve(100, f, 1, -1);
            Assert.fail("Expecting NumberIsTooLargeException - bad interval");
        } catch (final NumberIsTooLargeException ex) {
            // expected
        }
        try { // no bracket
            solver.solve(100, f, 1, 1.5);
            Assert.fail("Expecting NoBracketingException - non-bracketing");
        } catch (final NoBracketingException ex) {
            // expected
        }
        try { // no bracket
            solver.solve(100, f, 1, 1.5, 1.2);
            Assert.fail("Expecting NoBracketingException - non-bracketing");
        } catch (final NoBracketingException ex) {
            // expected
        }
    }

    @Test
    public void testSolutionLeftSide() {
        final UnivariateFunction f = new Sin();
        final UnivariateSolver solver = this.getSolver();
        double left = -1.5;
        double right = 0.05;
        for (int i = 0; i < 10; i++) {
            // Test whether the allowed solutions are taken into account.
            final double solution = this.getSolution(solver, 100, f, left, right, AllowedSolution.LEFT_SIDE);
            if (!Double.isNaN(solution)) {
                Assert.assertTrue(solution <= 0.0);
            }

            // Prepare for next test.
            left -= 0.1;
            right += 0.3;
        }
    }

    @Test
    public void testSolutionRightSide() {
        final UnivariateFunction f = new Sin();
        final UnivariateSolver solver = this.getSolver();
        double left = -1.5;
        double right = 0.05;
        for (int i = 0; i < 10; i++) {
            // Test whether the allowed solutions are taken into account.
            final double solution = this.getSolution(solver, 100, f, left, right, AllowedSolution.RIGHT_SIDE);
            if (!Double.isNaN(solution)) {
                Assert.assertTrue(solution >= 0.0);
            }

            // Prepare for next test.
            left -= 0.1;
            right += 0.3;
        }
    }

    @Test
    public void testSolutionBelowSide() {
        final UnivariateFunction f = new Sin();
        final UnivariateSolver solver = this.getSolver();
        double left = -1.5;
        double right = 0.05;
        for (int i = 0; i < 10; i++) {
            // Test whether the allowed solutions are taken into account.
            final double solution = this.getSolution(solver, 100, f, left, right, AllowedSolution.BELOW_SIDE);
            if (!Double.isNaN(solution)) {
                Assert.assertTrue(f.value(solution) <= 0.0);
            }

            // Prepare for next test.
            left -= 0.1;
            right += 0.3;
        }
    }

    @Test
    public void testSolutionAboveSide() {
        final UnivariateFunction f = new Sin();
        final UnivariateSolver solver = this.getSolver();
        double left = -1.5;
        double right = 0.05;
        for (int i = 0; i < 10; i++) {
            // Test whether the allowed solutions are taken into account.
            final double solution = this.getSolution(solver, 100, f, left, right, AllowedSolution.ABOVE_SIDE);
            if (!Double.isNaN(solution)) {
                Assert.assertTrue(f.value(solution) >= 0.0);
            }

            // Prepare for next test.
            left -= 0.1;
            right += 0.3;
        }
    }

    private double getSolution(final UnivariateSolver solver, final int maxEval, final UnivariateFunction f,
                               final double left, final double right, final AllowedSolution allowedSolution) {
        try {
            @SuppressWarnings("unchecked")
            final BracketedUnivariateSolver<UnivariateFunction> bracketing =
                (BracketedUnivariateSolver<UnivariateFunction>) solver;
            return bracketing.solve(100, f, left, right, allowedSolution);
        } catch (final ClassCastException cce) {
            final double baseRoot = solver.solve(maxEval, f, left, right);
            if ((baseRoot <= left) || (baseRoot >= right)) {
                // the solution slipped out of interval
                return Double.NaN;
            }
            final PegasusSolver bracketing =
                new PegasusSolver(solver.getRelativeAccuracy(), solver.getAbsoluteAccuracy(),
                    solver.getFunctionValueAccuracy());
            return UnivariateSolverUtils.forceSide(maxEval - solver.getEvaluations(),
                f, bracketing, baseRoot, left, right,
                allowedSolution);
        }
    }

}
