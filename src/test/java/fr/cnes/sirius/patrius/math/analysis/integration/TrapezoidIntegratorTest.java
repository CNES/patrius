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
package fr.cnes.sirius.patrius.math.analysis.integration;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.analysis.QuinticFunction;
import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.function.Sin;
import fr.cnes.sirius.patrius.math.exception.MaxCountExceededException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooLargeException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Test case for trapezoid integrator.
 * <p>
 * Test runs show that for a default relative accuracy of 1E-6, it generally takes 10 to 15 iterations for the integral
 * to converge.
 * 
 * @version $Id: TrapezoidIntegratorTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
public final class TrapezoidIntegratorTest {

    /**
     * Test of integrator for the sine function.
     */
    @Test
    public void testSinFunction() {
        final UnivariateFunction f = new Sin();
        final UnivariateIntegrator integrator = new TrapezoidIntegrator();
        double min;
        double max;
        double expected;
        double result;
        double tolerance;

        min = 0;
        max = FastMath.PI;
        expected = 2;
        tolerance = MathLib.abs(expected * integrator.getRelativeAccuracy());
        result = integrator.integrate(10000, f, min, max);
        Assert.assertTrue(integrator.getEvaluations() < 2500);
        Assert.assertTrue(integrator.getIterations() < 15);
        Assert.assertEquals(expected, result, tolerance);

        min = -FastMath.PI / 3;
        max = 0;
        expected = -0.5;
        tolerance = MathLib.abs(expected * integrator.getRelativeAccuracy());
        result = integrator.integrate(10000, f, min, max);
        Assert.assertTrue(integrator.getEvaluations() < 2500);
        Assert.assertTrue(integrator.getIterations() < 15);
        Assert.assertEquals(expected, result, tolerance);
    }

    /**
     * Test of integrator for the quintic function.
     */
    @Test
    public void testQuinticFunction() {
        final UnivariateFunction f = new QuinticFunction();
        final UnivariateIntegrator integrator = new TrapezoidIntegrator();
        double min;
        double max;
        double expected;
        double result;
        double tolerance;

        min = 0;
        max = 1;
        expected = -1.0 / 48;
        tolerance = MathLib.abs(expected * integrator.getRelativeAccuracy());
        result = integrator.integrate(10000, f, min, max);
        Assert.assertTrue(integrator.getEvaluations() < 5000);
        Assert.assertTrue(integrator.getIterations() < 15);
        Assert.assertEquals(expected, result, tolerance);

        min = 0;
        max = 0.5;
        expected = 11.0 / 768;
        tolerance = MathLib.abs(expected * integrator.getRelativeAccuracy());
        result = integrator.integrate(10000, f, min, max);
        Assert.assertTrue(integrator.getEvaluations() < 2500);
        Assert.assertTrue(integrator.getIterations() < 15);
        Assert.assertEquals(expected, result, tolerance);

        min = -1;
        max = 4;
        expected = 2048 / 3.0 - 78 + 1.0 / 48;
        tolerance = MathLib.abs(expected * integrator.getRelativeAccuracy());
        result = integrator.integrate(10000, f, min, max);
        Assert.assertTrue(integrator.getEvaluations() < 5000);
        Assert.assertTrue(integrator.getIterations() < 15);
        Assert.assertEquals(expected, result, tolerance);

    }

    /**
     * Test of parameters for the integrator.
     */
    @Test
    public void testParameters() {
        final UnivariateFunction f = new Sin();

        try {
            // bad interval
            new TrapezoidIntegrator().integrate(1000, f, 1, -1);
            Assert.fail("Expecting NumberIsTooLargeException - bad interval");
        } catch (final NumberIsTooLargeException ex) {
            // expected
        }
        try {
            // bad iteration limits
            new TrapezoidIntegrator(5, 4);
            Assert.fail("Expecting NumberIsTooSmallException - bad iteration limits");
        } catch (final NumberIsTooSmallException ex) {
            // expected
        }
        try {
            // bad iteration limits
            new TrapezoidIntegrator(10, 99);
            Assert.fail("Expecting NumberIsTooLargeException - bad iteration limits");
        } catch (final NumberIsTooLargeException ex) {
            // expected
        }
        try {
            // bad iteration limits, other constructor
            new TrapezoidIntegrator(1.0e-15, 1.0e-6, 10, 99);
            Assert.fail("Expecting NumberIsTooLargeException - bad iteration limits 2");
        } catch (final NumberIsTooLargeException ex) {
            // expected
        }
        try {
            // limit exceeded
            final UnivariateIntegrator integrator = new TrapezoidIntegrator(1.0e-15, 1.0e-6, 1, 5);
            integrator.getMaximalIterationCount();
            integrator.integrate(1000, f, 0, FastMath.PI);
            Assert.fail("Expecting MaxCountExceededException - limit exceeded");
        } catch (final MaxCountExceededException ex) {
            // expected
        }
    }

    /**
     * Test of methods from the UnivariateIntegrator class.
     * 
     * @throws Exception
     *         ex
     */
    @Test
    public void testUnivariateIntegrator() throws Exception {
        UnivariateIntegrator integrator = new TrapezoidIntegrator();

        integrator = new TrapezoidIntegrator(10, 50);
        final int max = integrator.getMaximalIterationCount();
        Assert.assertEquals(max, 50);
        final int min = integrator.getMinimalIterationCount();
        Assert.assertEquals(min, 10);

        Assert.assertEquals(integrator.getAbsoluteAccuracy(), 1.0e-15, 0.);
    }
}