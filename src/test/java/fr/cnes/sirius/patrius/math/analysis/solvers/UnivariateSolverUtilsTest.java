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
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
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
import fr.cnes.sirius.patrius.math.analysis.function.Sin;
import fr.cnes.sirius.patrius.math.analysis.solver.UnivariateSolverUtils;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.util.FastMath;

/**
 * @version $Id: UnivariateSolverUtilsTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class UnivariateSolverUtilsTest {

    protected UnivariateFunction sin = new Sin();

    @Test(expected = MathIllegalArgumentException.class)
    public void testSolveNull() {
        UnivariateSolverUtils.solve(null, 0.0, 4.0);
    }

    @Test(expected = MathIllegalArgumentException.class)
    public void testSolveBadEndpoints() {
        final double root = UnivariateSolverUtils.solve(this.sin, 4.0, -0.1, 1e-6);
        System.out.println("root=" + root);
    }

    @Test
    public void testSolveBadAccuracy() {
        try { // bad accuracy
            UnivariateSolverUtils.solve(this.sin, 0.0, 4.0, 0.0);
            // Assert.fail("Expecting MathIllegalArgumentException");
            // was changed
        } catch (final MathIllegalArgumentException ex) {
            // expected
        }
    }

    @Test
    public void testSolveSin() {
        final double x = UnivariateSolverUtils.solve(this.sin, 1.0, 4.0);
        Assert.assertEquals(FastMath.PI, x, 1.0e-4);
    }

    @Test(expected = MathIllegalArgumentException.class)
    public void testSolveAccuracyNull() {
        final double accuracy = 1.0e-6;
        UnivariateSolverUtils.solve(null, 0.0, 4.0, accuracy);
    }

    @Test
    public void testSolveAccuracySin() {
        final double accuracy = 1.0e-6;
        final double x = UnivariateSolverUtils.solve(this.sin, 1.0,
            4.0, accuracy);
        Assert.assertEquals(FastMath.PI, x, accuracy);
    }

    @Test(expected = MathIllegalArgumentException.class)
    public void testSolveNoRoot() {
        UnivariateSolverUtils.solve(this.sin, 1.0, 1.5);
    }

    @Test
    public void testBracketSin() {
        final double[] result = UnivariateSolverUtils.bracket(this.sin,
            0.0, -2.0, 2.0);
        Assert.assertTrue(this.sin.value(result[0]) < 0);
        Assert.assertTrue(this.sin.value(result[1]) > 0);
    }

    @Test
    public void testBracketEndpointRoot() {
        final double[] result = UnivariateSolverUtils.bracket(this.sin, 1.5, 0, 2.0);
        Assert.assertEquals(0.0, this.sin.value(result[0]), 1.0e-15);
        Assert.assertTrue(this.sin.value(result[1]) > 0);
    }

    @Test(expected = MathIllegalArgumentException.class)
    public void testNullFunction() {
        UnivariateSolverUtils.bracket(null, 1.5, 0, 2.0);
    }

    @Test(expected = MathIllegalArgumentException.class)
    public void testBadInitial() {
        UnivariateSolverUtils.bracket(this.sin, 2.5, 0, 2.0);
    }

    @Test(expected = MathIllegalArgumentException.class)
    public void testBadEndpoints() {
        // endpoints not valid
        UnivariateSolverUtils.bracket(this.sin, 1.5, 2.0, 1.0);
    }

    @Test(expected = MathIllegalArgumentException.class)
    public void testBadMaximumIterations() {
        // bad maximum iterations
        UnivariateSolverUtils.bracket(this.sin, 1.5, 0, 2.0, 0);
    }

    @Test
    public void testMisc() {
        final UnivariateFunction f = new QuinticFunction();
        double result;
        // Static solve method
        result = UnivariateSolverUtils.solve(f, -0.2, 0.2);
        Assert.assertEquals(result, 0, 1E-8);
        result = UnivariateSolverUtils.solve(f, -0.1, 0.3);
        Assert.assertEquals(result, 0, 1E-8);
        result = UnivariateSolverUtils.solve(f, -0.3, 0.45);
        Assert.assertEquals(result, 0, 1E-6);
        result = UnivariateSolverUtils.solve(f, 0.3, 0.7);
        Assert.assertEquals(result, 0.5, 1E-6);
        result = UnivariateSolverUtils.solve(f, 0.2, 0.6);
        Assert.assertEquals(result, 0.5, 1E-6);
        result = UnivariateSolverUtils.solve(f, 0.05, 0.95);
        Assert.assertEquals(result, 0.5, 1E-6);
        result = UnivariateSolverUtils.solve(f, 0.85, 1.25);
        Assert.assertEquals(result, 1.0, 1E-6);
        result = UnivariateSolverUtils.solve(f, 0.8, 1.2);
        Assert.assertEquals(result, 1.0, 1E-6);
        result = UnivariateSolverUtils.solve(f, 0.85, 1.75);
        Assert.assertEquals(result, 1.0, 1E-6);
        result = UnivariateSolverUtils.solve(f, 0.55, 1.45);
        Assert.assertEquals(result, 1.0, 1E-6);
        result = UnivariateSolverUtils.solve(f, 0.85, 5);
        Assert.assertEquals(result, 1.0, 1E-6);
    }
}
