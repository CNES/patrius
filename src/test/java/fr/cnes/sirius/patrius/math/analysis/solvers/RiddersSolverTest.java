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

import fr.cnes.sirius.patrius.math.analysis.QuinticFunction;
import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.function.Expm1;
import fr.cnes.sirius.patrius.math.analysis.function.Sin;
import fr.cnes.sirius.patrius.math.analysis.solver.RiddersSolver;
import fr.cnes.sirius.patrius.math.analysis.solver.UnivariateSolver;
import fr.cnes.sirius.patrius.math.exception.NoBracketingException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooLargeException;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Test case for {@link RiddersSolver Ridders} solver.
 * <p>
 * Ridders' method converges superlinearly, more specific, its rate of convergence is sqrt(2). Test runs show that for a
 * default absolute accuracy of 1E-6, it generally takes less than 5 iterations for close initial bracket and 5 to 10
 * iterations for distant initial bracket to converge.
 * 
 * @version $Id: RiddersSolverTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
public final class RiddersSolverTest {
    /**
     * Test of solver for the sine function.
     */
    @Test
    public void testSinFunction() {
        final UnivariateFunction f = new Sin();
        final UnivariateSolver solver = new RiddersSolver();
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
        final UnivariateSolver solver = new RiddersSolver();
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
     */
    @Test
    public void testExpm1Function() {
        final UnivariateFunction f = new Expm1();
        final UnivariateSolver solver = new RiddersSolver();
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
     */
    @Test
    public void testParameters() {
        final UnivariateFunction f = new Sin();
        final UnivariateSolver solver = new RiddersSolver();

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
}
