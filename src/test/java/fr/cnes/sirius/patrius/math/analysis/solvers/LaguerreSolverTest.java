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
package fr.cnes.sirius.patrius.math.analysis.solvers;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.analysis.polynomials.PolynomialFunction;
import fr.cnes.sirius.patrius.math.analysis.solver.LaguerreSolver;
import fr.cnes.sirius.patrius.math.complex.Complex;
import fr.cnes.sirius.patrius.math.exception.NoBracketingException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooLargeException;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;

/**
 * Test case for Laguerre solver.
 * <p>
 * Laguerre's method is very efficient in solving polynomials. Test runs show that for a default absolute accuracy of
 * 1E-6, it generally takes less than 5 iterations to find one root, provided solveAll() is not invoked, and 15 to 20
 * iterations to find all roots for quintic function.
 * 
 * @version $Id: LaguerreSolverTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
public final class LaguerreSolverTest {
    /**
     * Test of solver for the linear function.
     */
    @Test
    public void testLinearFunction() {
        double min, max, expected, result, tolerance;

        // p(x) = 4x - 1
        final double coefficients[] = { -1.0, 4.0 };
        final PolynomialFunction f = new PolynomialFunction(coefficients);
        final LaguerreSolver solver = new LaguerreSolver();

        min = 0.0;
        max = 1.0;
        expected = 0.25;
        tolerance = MathLib.max(solver.getAbsoluteAccuracy(),
            MathLib.abs(expected * solver.getRelativeAccuracy()));
        result = solver.solve(100, f, min, max);
        Assert.assertEquals(expected, result, tolerance);
    }

    /**
     * Test of solver for the quadratic function.
     */
    @Test
    public void testQuadraticFunction() {
        double min, max, expected, result, tolerance;

        // p(x) = 2x^2 + 5x - 3 = (x+3)(2x-1)
        final double coefficients[] = { -3.0, 5.0, 2.0 };
        final PolynomialFunction f = new PolynomialFunction(coefficients);
        final LaguerreSolver solver = new LaguerreSolver();

        min = 0.0;
        max = 2.0;
        expected = 0.5;
        tolerance = MathLib.max(solver.getAbsoluteAccuracy(),
            MathLib.abs(expected * solver.getRelativeAccuracy()));
        result = solver.solve(100, f, min, max);
        Assert.assertEquals(expected, result, tolerance);

        min = -4.0;
        max = -1.0;
        expected = -3.0;
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
        double min, max, expected, result, tolerance;

        // p(x) = x^5 - x^4 - 12x^3 + x^2 - x - 12 = (x+1)(x+3)(x-4)(x^2-x+1)
        final double coefficients[] = { -12.0, -1.0, 1.0, -12.0, -1.0, 1.0 };
        final PolynomialFunction f = new PolynomialFunction(coefficients);
        final LaguerreSolver solver = new LaguerreSolver();

        min = -2.0;
        max = 2.0;
        expected = -1.0;
        tolerance = MathLib.max(solver.getAbsoluteAccuracy(),
            MathLib.abs(expected * solver.getRelativeAccuracy()));
        result = solver.solve(100, f, min, max);
        Assert.assertEquals(expected, result, tolerance);

        min = -5.0;
        max = -2.5;
        expected = -3.0;
        tolerance = MathLib.max(solver.getAbsoluteAccuracy(),
            MathLib.abs(expected * solver.getRelativeAccuracy()));
        result = solver.solve(100, f, min, max);
        Assert.assertEquals(expected, result, tolerance);

        min = 3.0;
        max = 6.0;
        expected = 4.0;
        tolerance = MathLib.max(solver.getAbsoluteAccuracy(),
            MathLib.abs(expected * solver.getRelativeAccuracy()));
        result = solver.solve(100, f, min, max);
        Assert.assertEquals(expected, result, tolerance);
    }

    /**
     * Test of solver for the quintic function using {@link LaguerreSolver#solveAllComplex(double[],double)
     * solveAllComplex}.
     */
    @Test
    public void testQuinticFunction2() {
        // p(x) = x^5 + 4x^3 + x^2 + 4 = (x+1)(x^2-x+1)(x^2+4)
        final double[] coefficients = { 4.0, 0.0, 1.0, 4.0, 0.0, 1.0 };
        final LaguerreSolver solver = new LaguerreSolver();
        final Complex[] result = solver.solveAllComplex(coefficients, 0);

        for (final Complex expected : new Complex[] { new Complex(0, -2),
            new Complex(0, 2),
            new Complex(0.5, 0.5 * MathLib.sqrt(3)),
            new Complex(-1, 0),
            new Complex(0.5, -0.5 * MathLib.sqrt(3.0)) }) {
            final double tolerance = MathLib.max(solver.getAbsoluteAccuracy(),
                MathLib.abs(expected.abs() * solver.getRelativeAccuracy()));
            TestUtils.assertContains(result, expected, tolerance);
        }
    }

    /**
     * Test of parameters for the solver.
     */
    @Test
    public void testParameters() {
        final double coefficients[] = { -3.0, 5.0, 2.0 };
        final PolynomialFunction f = new PolynomialFunction(coefficients);
        final LaguerreSolver solver = new LaguerreSolver();

        try {
            // bad interval
            solver.solve(100, f, 1, -1);
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
     * Test for code coverage completion only (no actual functionality tested)
     * => Use default values in constructors and compare results with default constructors
     */
    @Test
    public void testMiscCoverage() {

        // coefficients and default constructor
        final double[] coefficients = { 4.0, 0.0, 1.0, 4.0, 0.0, 1.0 };
        final LaguerreSolver solverDefault = new LaguerreSolver();

        // Const with relative and absolute accuracy.
        final LaguerreSolver ls1 = new LaguerreSolver(1e-14, 1e-6);
        final Complex[] res = ls1.solveAllComplex(coefficients, 0);
        final Complex[] resDefault = solverDefault.solveAllComplex(coefficients, 0);
        for (int i = 0; i < resDefault.length; i++) {
            Assert.assertEquals(res[i].getReal(), resDefault[i].getReal(), Precision.EPSILON);
        }

        // Const with relative, absolute and function value accuracy.
        final LaguerreSolver ls2 = new LaguerreSolver(1e-14, 1e-6, 1e-15);
        final Complex[] res2 = ls2.solveAllComplex(coefficients, 0);
        for (int i = 0; i < resDefault.length; i++) {
            Assert.assertEquals(res2[i].getReal(), resDefault[i].getReal(), Precision.EPSILON);
        }
    }
}
