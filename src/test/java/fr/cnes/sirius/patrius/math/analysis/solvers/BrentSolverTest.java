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

import fr.cnes.sirius.patrius.math.analysis.FunctionUtils;
import fr.cnes.sirius.patrius.math.analysis.MonitoredFunction;
import fr.cnes.sirius.patrius.math.analysis.QuinticFunction;
import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.differentiation.DerivativeStructure;
import fr.cnes.sirius.patrius.math.analysis.differentiation.UnivariateDifferentiableFunction;
import fr.cnes.sirius.patrius.math.analysis.function.Constant;
import fr.cnes.sirius.patrius.math.analysis.function.Exp;
import fr.cnes.sirius.patrius.math.analysis.function.Inverse;
import fr.cnes.sirius.patrius.math.analysis.function.Sin;
import fr.cnes.sirius.patrius.math.analysis.function.Sqrt;
import fr.cnes.sirius.patrius.math.analysis.solver.BrentSolver;
import fr.cnes.sirius.patrius.math.analysis.solver.UnivariateSolver;
import fr.cnes.sirius.patrius.math.exception.NoBracketingException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooLargeException;
import fr.cnes.sirius.patrius.math.exception.TooManyEvaluationsException;
import fr.cnes.sirius.patrius.math.util.FastMath;

/**
 * Test case for {@link BrentSolver Brent} solver.
 * Because Brent-Dekker is guaranteed to converge in less than the default
 * maximum iteration count due to bisection fallback, it is quite hard to
 * debug. I include measured iteration counts plus one in order to detect
 * regressions. On average Brent-Dekker should use 4..5 iterations for the
 * default absolute accuracy of 10E-8 for sinus and the quintic function around
 * zero, and 5..10 iterations for the other zeros.
 * 
 * @version $Id: BrentSolverTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
public final class BrentSolverTest {
    /**
     * Test of solver for the sine function.
     */
    @Test
    public void testSinZero() {
        // The sinus function is behaved well around the root at pi. The second
        // order derivative is zero, which means linar approximating methods will
        // still converge quadratically.
        final UnivariateFunction f = new Sin();
        double result;
        final UnivariateSolver solver = new BrentSolver();
        // Somewhat benign interval. The function is monotone.
        result = solver.solve(100, f, 3, 4);
        // System.out.println(
        // "Root: " + result + " Evaluations: " + solver.getEvaluations());
        Assert.assertEquals(result, FastMath.PI, solver.getAbsoluteAccuracy());
        Assert.assertTrue(solver.getEvaluations() <= 7);
        // Larger and somewhat less benign interval. The function is grows first.
        result = solver.solve(100, f, 1, 4);
        // System.out.println(
        // "Root: " + result + " Evaluations: " + solver.getEvaluations());
        Assert.assertEquals(result, FastMath.PI, solver.getAbsoluteAccuracy());
        Assert.assertTrue(solver.getEvaluations() <= 8);
    }

    /**
     * Test of solver for the quintic function.
     */
    @Test
    public void testQuinticZero() {
        // The quintic function has zeros at 0, +-0.5 and +-1.
        // Around the root of 0 the function is well behaved, with a second derivative
        // of zero a 0.
        // The other roots are less well to find, in particular the root at 1, because
        // the function grows fast for x>1.
        // The function has extrema (first derivative is zero) at 0.27195613 and 0.82221643,
        // intervals containing these values are harder for the solvers.
        final UnivariateFunction f = new QuinticFunction();
        double result;
        // Brent-Dekker solver.
        final UnivariateSolver solver = new BrentSolver();
        // Symmetric bracket around 0. Test whether solvers can handle hitting
        // the root in the first iteration.
        result = solver.solve(100, f, -0.2, 0.2);
        // System.out.println(
        // "Root: " + result + " Evaluations: " + solver.getEvaluations());
        Assert.assertEquals(result, 0, solver.getAbsoluteAccuracy());
        Assert.assertTrue(solver.getEvaluations() <= 3);
        // 1 iterations on i586 JDK 1.4.1.
        // Asymmetric bracket around 0, just for fun. Contains extremum.
        result = solver.solve(100, f, -0.1, 0.3);
        // System.out.println(
        // "Root: " + result + " Evaluations: " + solver.getEvaluations());
        Assert.assertEquals(result, 0, solver.getAbsoluteAccuracy());
        // 5 iterations on i586 JDK 1.4.1.
        Assert.assertTrue(solver.getEvaluations() <= 7);
        // Large bracket around 0. Contains two extrema.
        result = solver.solve(100, f, -0.3, 0.45);
        // System.out.println(
        // "Root: " + result + " Evaluations: " + solver.getEvaluations());
        Assert.assertEquals(result, 0, solver.getAbsoluteAccuracy());
        // 6 iterations on i586 JDK 1.4.1.
        Assert.assertTrue(solver.getEvaluations() <= 8);
        // Benign bracket around 0.5, function is monotonous.
        result = solver.solve(100, f, 0.3, 0.7);
        // System.out.println(
        // "Root: " + result + " Evaluations: " + solver.getEvaluations());
        Assert.assertEquals(result, 0.5, solver.getAbsoluteAccuracy());
        // 6 iterations on i586 JDK 1.4.1.
        Assert.assertTrue(solver.getEvaluations() <= 9);
        // Less benign bracket around 0.5, contains one extremum.
        result = solver.solve(100, f, 0.2, 0.6);
        // System.out.println(
        // "Root: " + result + " Evaluations: " + solver.getEvaluations());
        Assert.assertEquals(result, 0.5, solver.getAbsoluteAccuracy());
        Assert.assertTrue(solver.getEvaluations() <= 10);
        // Large, less benign bracket around 0.5, contains both extrema.
        result = solver.solve(100, f, 0.05, 0.95);
        // System.out.println(
        // "Root: " + result + " Evaluations: " + solver.getEvaluations());
        Assert.assertEquals(result, 0.5, solver.getAbsoluteAccuracy());
        Assert.assertTrue(solver.getEvaluations() <= 11);
        // Relatively benign bracket around 1, function is monotonous. Fast growth for x>1
        // is still a problem.
        result = solver.solve(100, f, 0.85, 1.25);
        // System.out.println(
        // "Root: " + result + " Evaluations: " + solver.getEvaluations());
        Assert.assertEquals(result, 1.0, solver.getAbsoluteAccuracy());
        Assert.assertTrue(solver.getEvaluations() <= 11);
        // Less benign bracket around 1 with extremum.
        result = solver.solve(100, f, 0.8, 1.2);
        // System.out.println(
        // "Root: " + result + " Evaluations: " + solver.getEvaluations());
        Assert.assertEquals(result, 1.0, solver.getAbsoluteAccuracy());
        Assert.assertTrue(solver.getEvaluations() <= 11);
        // Large bracket around 1. Monotonous.
        result = solver.solve(100, f, 0.85, 1.75);
        // System.out.println(
        // "Root: " + result + " Evaluations: " + solver.getEvaluations());
        Assert.assertEquals(result, 1.0, solver.getAbsoluteAccuracy());
        Assert.assertTrue(solver.getEvaluations() <= 13);
        // Large bracket around 1. Interval contains extremum.
        result = solver.solve(100, f, 0.55, 1.45);
        // System.out.println(
        // "Root: " + result + " Evaluations: " + solver.getEvaluations());
        Assert.assertEquals(result, 1.0, solver.getAbsoluteAccuracy());
        Assert.assertTrue(solver.getEvaluations() <= 10);
        // Very large bracket around 1 for testing fast growth behaviour.
        result = solver.solve(100, f, 0.85, 5);
        // System.out.println(
        // "Root: " + result + " Evaluations: " + solver.getEvaluations());
        Assert.assertEquals(result, 1.0, solver.getAbsoluteAccuracy());
        Assert.assertTrue(solver.getEvaluations() <= 15);

        try {
            result = solver.solve(5, f, 0.85, 5);
            Assert.fail("Expected TooManyEvaluationsException");
        } catch (final TooManyEvaluationsException e) {
            // Expected.
        }
    }

    /**
     * Test of solver around the end points.
     */
    @Test
    public void testRootEndpoints() {
        final UnivariateFunction f = new Sin();
        final BrentSolver solver = new BrentSolver();

        // endpoint is root
        double result = solver.solve(100, f, FastMath.PI, 4);
        Assert.assertEquals(FastMath.PI, result, solver.getAbsoluteAccuracy());

        result = solver.solve(100, f, 3, FastMath.PI);
        Assert.assertEquals(FastMath.PI, result, solver.getAbsoluteAccuracy());

        result = solver.solve(100, f, FastMath.PI, 4, 3.5);
        Assert.assertEquals(FastMath.PI, result, solver.getAbsoluteAccuracy());

        result = solver.solve(100, f, 3, FastMath.PI, 3.07);
        Assert.assertEquals(FastMath.PI, result, solver.getAbsoluteAccuracy());
    }

    /**
     * Test of solver with the bad end points.
     */
    @Test
    public void testBadEndpoints() {
        final UnivariateFunction f = new Sin();
        final BrentSolver solver = new BrentSolver();
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

    /**
     * testInitialGuess.
     */
    @Test
    public void testInitialGuess() {
        final MonitoredFunction f = new MonitoredFunction(new QuinticFunction());
        final BrentSolver solver = new BrentSolver();
        double result;

        // no guess
        result = solver.solve(100, f, 0.6, 7.0);
        Assert.assertEquals(result, 1.0, solver.getAbsoluteAccuracy());
        final int referenceCallsCount = f.getCallsCount();
        Assert.assertTrue(referenceCallsCount >= 13);

        // invalid guess (it *is* a root, but outside of the range)
        try {
            result = solver.solve(100, f, 0.6, 7.0, 0.0);
            Assert.fail("a NumberIsTooLargeException was expected");
        } catch (final NumberIsTooLargeException iae) {
            // expected behaviour
        }

        // bad guess
        f.setCallsCount(0);
        result = solver.solve(100, f, 0.6, 7.0, 0.61);
        Assert.assertEquals(result, 1.0, solver.getAbsoluteAccuracy());
        Assert.assertTrue(f.getCallsCount() > referenceCallsCount);

        // good guess
        f.setCallsCount(0);
        result = solver.solve(100, f, 0.6, 7.0, 0.999999);
        Assert.assertEquals(result, 1.0, solver.getAbsoluteAccuracy());
        Assert.assertTrue(f.getCallsCount() < referenceCallsCount);

        // perfect guess
        f.setCallsCount(0);
        result = solver.solve(100, f, 0.6, 7.0, 1.0);
        Assert.assertEquals(result, 1.0, solver.getAbsoluteAccuracy());
        Assert.assertEquals(1, solver.getEvaluations());
        Assert.assertEquals(1, f.getCallsCount());
    }

    /**
     * Test of solver with relative accuracy
     */
    @Test
    public void testSinZeroWithRelativeAccuracy() {
        final UnivariateFunction f = new Sin();
        double result;
        final UnivariateSolver solver = new BrentSolver(1e-10, 1e-8);
        // Somewhat benign interval. The function is monotone.
        result = solver.solve(100, f, 3, 4);
        Assert.assertEquals(result, FastMath.PI, solver.getAbsoluteAccuracy());
        Assert.assertTrue(solver.getAbsoluteAccuracy() == 1e-8);
        Assert.assertTrue(solver.getRelativeAccuracy() == 1e-10);
        Assert.assertTrue(solver.getMaxEvaluations() == 100);
    }

    /**
     * Test of solver with function value accuracy
     */
    @Test
    public void testSinZeroWithFunctionValueAccuracy() {
        UnivariateFunction f;
        double result;
        UnivariateSolver solver;
        f = new Sin();
        solver = new BrentSolver(1e-10, 1e-8, 1e-11);
        result = solver.solve(100, f, 6, 7);
        Assert.assertEquals(result, 2 * FastMath.PI, 1e-11);
        Assert.assertTrue(solver.getFunctionValueAccuracy() == 1e-11);

        f = new Exp();
        try { // no bracket
            result = solver.solve(100, f, 1, 7);
            Assert.fail("Expecting NoBracketingException - non-bracketing");
        } catch (final NoBracketingException ex) {
            // expected
        }
    }

    /**
     * Test of robustness of the solver
     */
    @Test
    public void testBadRoot() {
        UnivariateDifferentiableFunction f;
        double result;
        final BrentSolver solver = new BrentSolver();
        f = new Sin();

        // function has several roots in the interval. Solve method returns one of the roots
        result = solver.solve(1000, f, 1, 20);
        Assert.assertEquals(result, FastMath.PI, solver.getAbsoluteAccuracy());

        // function has several roots in the interval. Solve method returns one of the roots
        result = solver.solve(1000, f, 2, 20);
        Assert.assertEquals(result, 3 * FastMath.PI, solver.getAbsoluteAccuracy());

        try { // function has several roots in the interval. Solve method returns an exception
            result = solver.solve(100, f, 7, 20);
            Assert.fail("Expecting NoBracketingException - non-bracketing");
        } catch (final NoBracketingException ex) {
            // expected
        }
    }

    @Test
    public void testMath832() {
        final UnivariateFunction f = new UnivariateFunction(){
            /** Serializable UID. */
            private static final long serialVersionUID = -4947965201252081565L;
            private final UnivariateDifferentiableFunction sqrt = new Sqrt();
            private final UnivariateDifferentiableFunction inv = new Inverse();
            private final UnivariateDifferentiableFunction func
            = FunctionUtils.add(FunctionUtils.multiply(new Constant(1e2), this.sqrt),
                FunctionUtils.multiply(new Constant(1e6), this.inv),
                FunctionUtils.multiply(new Constant(1e4),
                    FunctionUtils.compose(this.inv, this.sqrt)));

            @Override
            public double value(final double x) {
                return this.func.value(new DerivativeStructure(1, 1, 0, x)).getPartialDerivative(1);
            }

        };

        final BrentSolver solver = new BrentSolver();
        final double result = solver.solve(99, f, 1, 1e30, 1 + 1e-10);
        Assert.assertEquals(804.93558250, result, 1e-8);
    }
}
