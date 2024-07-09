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
package fr.cnes.sirius.patrius.math.analysis.solvers;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.analysis.QuinticFunction;
import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.XMinus5Function;
import fr.cnes.sirius.patrius.math.analysis.function.Exp;
import fr.cnes.sirius.patrius.math.analysis.function.Sin;
import fr.cnes.sirius.patrius.math.analysis.solver.BisectionSolver;
import fr.cnes.sirius.patrius.math.util.FastMath;

/**
 * @version $Id: BisectionSolverTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
public final class BisectionSolverTest {
    /**
     * Test of solver for the sine function.
     */
    @Test
    public void testSinZero() {
        final UnivariateFunction f = new Sin();
        double result;

        final BisectionSolver solver = new BisectionSolver();
        result = solver.solve(100, f, 3, 4);
        Assert.assertEquals(result, FastMath.PI, solver.getAbsoluteAccuracy());

        result = solver.solve(100, f, 1, 4);
        Assert.assertEquals(result, FastMath.PI, solver.getAbsoluteAccuracy());
    }

    /**
     * Test of solver for the quintic function.
     */
    @Test
    public void testQuinticZero() {
        final UnivariateFunction f = new QuinticFunction();
        double result;

        final BisectionSolver solver = new BisectionSolver();
        result = solver.solve(100, f, -0.2, 0.2);
        Assert.assertEquals(result, 0, solver.getAbsoluteAccuracy());

        result = solver.solve(100, f, -0.1, 0.3);
        Assert.assertEquals(result, 0, solver.getAbsoluteAccuracy());

        result = solver.solve(100, f, -0.3, 0.45);
        Assert.assertEquals(result, 0, solver.getAbsoluteAccuracy());

        result = solver.solve(100, f, 0.3, 0.7);
        Assert.assertEquals(result, 0.5, solver.getAbsoluteAccuracy());

        result = solver.solve(100, f, 0.2, 0.6);
        Assert.assertEquals(result, 0.5, solver.getAbsoluteAccuracy());

        result = solver.solve(100, f, 0.05, 0.95);
        Assert.assertEquals(result, 0.5, solver.getAbsoluteAccuracy());

        result = solver.solve(100, f, 0.85, 1.25);
        Assert.assertEquals(result, 1.0, solver.getAbsoluteAccuracy());

        result = solver.solve(100, f, 0.8, 1.2);
        Assert.assertEquals(result, 1.0, solver.getAbsoluteAccuracy());

        result = solver.solve(100, f, 0.85, 1.75);
        Assert.assertEquals(result, 1.0, solver.getAbsoluteAccuracy());

        result = solver.solve(100, f, 0.55, 1.45);
        Assert.assertEquals(result, 1.0, solver.getAbsoluteAccuracy());

        result = solver.solve(100, f, 0.85, 5);
        Assert.assertEquals(result, 1.0, solver.getAbsoluteAccuracy());

        Assert.assertTrue(solver.getEvaluations() > 0);
    }

    /**
     * 
     */
    @Test
    public void testMath369() {
        final UnivariateFunction f = new Sin();
        final BisectionSolver solver = new BisectionSolver();
        Assert.assertEquals(FastMath.PI, solver.solve(100, f, 3.0, 3.2, 3.1), solver.getAbsoluteAccuracy());
    }

    /**
     * Test of solver with relative accuracy
     */
    @Test
    public void testSinZeroWithRelativeAccuracy() {
        final UnivariateFunction f = new Sin();
        double result;

        final BisectionSolver solver = new BisectionSolver(1e-10, 1e-6);
        result = solver.solve(100, f, 3, 4);
        Assert.assertEquals(result, FastMath.PI, solver.getAbsoluteAccuracy());

        result = solver.solve(100, f, 1, 4);
        Assert.assertEquals(result, FastMath.PI, solver.getAbsoluteAccuracy());
    }

    /**
     * Test of robustness of the solver
     */
    @Test
    public void testBadRoot() {
        UnivariateFunction f;
        double result;
        BisectionSolver solver;
        f = new XMinus5Function();

        // the root is the upper bound of the interval
        solver = new BisectionSolver();
        result = solver.solve(100, f, 0, 5.0);
        Assert.assertEquals(result, 5, solver.getAbsoluteAccuracy());

        // the root is the lower bound of the interval
        result = solver.solve(1000, f, 5.0, 10);
        Assert.assertEquals(result, 5, solver.getAbsoluteAccuracy());

        // there is no root in the interval. Solve method returns the upper bound of the interval
        result = solver.solve(100, f, 5.1, 20);
        Assert.assertEquals(result, 20, solver.getAbsoluteAccuracy());

        // function has no root. Solve method returns the upper bound of the interval
        f = new Exp();
        result = solver.solve(100, f, 5, 20);
        Assert.assertEquals(result, 20, solver.getAbsoluteAccuracy());
    }
}
