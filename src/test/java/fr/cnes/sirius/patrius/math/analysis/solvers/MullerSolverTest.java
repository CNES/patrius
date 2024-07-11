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
package fr.cnes.sirius.patrius.math.analysis.solvers;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.analysis.QuinticFunction;
import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.XMinus5Function;
import fr.cnes.sirius.patrius.math.analysis.function.Expm1;
import fr.cnes.sirius.patrius.math.analysis.function.Sin;
import fr.cnes.sirius.patrius.math.analysis.solver.MullerSolver;
import fr.cnes.sirius.patrius.math.analysis.solver.UnivariateSolver;
import fr.cnes.sirius.patrius.math.exception.NoBracketingException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooLargeException;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Test case for {@link MullerSolver Muller} solver.
 * <p>
 * Muller's method converges almost quadratically near roots, but it can be very slow in regions far away from zeros.
 * Test runs show that for reasonably good initial values, for a default absolute accuracy of 1E-6, it generally takes 5
 * to 10 iterations for the solver to converge.
 * <p>
 * Tests for the exponential function illustrate the situations where Muller solver performs poorly.
 * 
 * @version $Id: MullerSolverTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
public final class MullerSolverTest {
    /**
     * Test of solver for the sine function.
     */
    @Test
    public void testSinFunction() {
        final UnivariateFunction f = new Sin();
        final UnivariateSolver solver = new MullerSolver();
        double min, max, expected, result, tolerance;

        min = 3.0;
        max = 4.0;
        expected = FastMath.PI;
        tolerance = MathLib.max(solver.getAbsoluteAccuracy(),
            MathLib.abs(expected * solver.getRelativeAccuracy()));
        result = solver.solve(100, f, min, max);
        Assert.assertEquals(expected, result, tolerance);

        min = -1.0;
        max = 1.5;
        expected = 0.0;
        tolerance = MathLib.max(solver.getAbsoluteAccuracy(),
            MathLib.abs(expected * solver.getRelativeAccuracy()));
        result = solver.solve(100, f, min, max);
        Assert.assertEquals(expected, result, tolerance);
    }

    /**
     * Test of solver for the quintic function.
     */
    @Test
    public void testQuinticFunction() {
        final UnivariateFunction f = new QuinticFunction();
        final UnivariateSolver solver = new MullerSolver();
        double min, max, expected, result, tolerance;

        min = -0.4;
        max = 0.2;
        expected = 0.0;
        tolerance = MathLib.max(solver.getAbsoluteAccuracy(),
            MathLib.abs(expected * solver.getRelativeAccuracy()));
        result = solver.solve(100, f, min, max);
        Assert.assertEquals(expected, result, tolerance);

        min = 0.75;
        max = 1.5;
        expected = 1.0;
        tolerance = MathLib.max(solver.getAbsoluteAccuracy(),
            MathLib.abs(expected * solver.getRelativeAccuracy()));
        result = solver.solve(100, f, min, max);
        Assert.assertEquals(expected, result, tolerance);

        min = -0.9;
        max = -0.2;
        expected = -0.5;
        tolerance = MathLib.max(solver.getAbsoluteAccuracy(),
            MathLib.abs(expected * solver.getRelativeAccuracy()));
        result = solver.solve(100, f, min, max);
        Assert.assertEquals(expected, result, tolerance);
    }

    /**
     * Test of solver for the exponential function.
     * <p>
     * It takes 10 to 15 iterations for the last two tests to converge. In fact, if not for the bisection alternative,
     * the solver would exceed the default maximal iteration of 100.
     */
    @Test
    public void testExpm1Function() {
        final UnivariateFunction f = new Expm1();
        final UnivariateSolver solver = new MullerSolver();
        double min, max, expected, result, tolerance;

        min = -1.0;
        max = 2.0;
        expected = 0.0;
        tolerance = MathLib.max(solver.getAbsoluteAccuracy(),
            MathLib.abs(expected * solver.getRelativeAccuracy()));
        result = solver.solve(100, f, min, max);
        Assert.assertEquals(expected, result, tolerance);

        min = -20.0;
        max = 10.0;
        expected = 0.0;
        tolerance = MathLib.max(solver.getAbsoluteAccuracy(),
            MathLib.abs(expected * solver.getRelativeAccuracy()));
        result = solver.solve(100, f, min, max);
        Assert.assertEquals(expected, result, tolerance);

        min = -50.0;
        max = 100.0;
        expected = 0.0;
        tolerance = MathLib.max(solver.getAbsoluteAccuracy(),
            MathLib.abs(expected * solver.getRelativeAccuracy()));
        result = solver.solve(100, f, min, max);
        Assert.assertEquals(expected, result, tolerance);
    }

    /**
     * Test of parameters for the solver.
     * 
     * @throws Exception
     */
    @Test
    public void testParameters() {
        final UnivariateFunction f = new Sin();
        final UnivariateSolver solver = new MullerSolver();

        try {
            // bad interval
            final double root = solver.solve(100, f, 1, -1);
            System.out.println("root=" + root);
            Assert.fail("Expecting NumberIsTooLargeException - bad interval");
        } catch (final NumberIsTooLargeException ex) {
            // expected
        }
        try {
            // no bracketing
            solver.solve(100, f, 2, 3);
            Assert.fail("Expecting NoBracketingException - no bracketing");
        } catch (final NoBracketingException ex) {
            // expected
        }
    }

    /**
     * Test of solver with relative accuracy
     */
    @Test
    public void testSinZeroWithRelativeAccuracy() {
        final UnivariateFunction f = new Sin();
        double result;
        final MullerSolver solver = new MullerSolver(1e-10, 1e-9);
        result = solver.solve(100, f, 3.141592653589793, 4);
        Assert.assertEquals(result, FastMath.PI, solver.getAbsoluteAccuracy());

        result = solver.solve(100, f, 2, 3.141592653589794);
        Assert.assertEquals(result, FastMath.PI, solver.getAbsoluteAccuracy());
    }

    /**
     * Test of robustness solver
     */
    @Test
    public void testBadEndpoints() {
        UnivariateFunction f;
        double result;
        MullerSolver solver;
        solver = new MullerSolver();

        f = new XMinus5Function();

        // the root is the upper bound of the interval
        result = solver.solve(100, f, 0, 5.0);
        Assert.assertEquals(result, 5, solver.getAbsoluteAccuracy());

        // the root is the lower bound of the interval
        result = solver.solve(1000, f, 5.0, 10);
        Assert.assertEquals(result, 5, solver.getAbsoluteAccuracy());

        // function has no root. Solve method returns an exception
        try {
            // no bracketing
            solver.solve(100, f, 2, 3);
            Assert.fail("Expecting NoBracketingException - no bracketing");
        } catch (final NoBracketingException ex) {
            // expected
        }
    }
}
