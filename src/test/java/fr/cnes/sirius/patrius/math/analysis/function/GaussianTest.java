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
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.math.analysis.function;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.differentiation.DerivativeStructure;
import fr.cnes.sirius.patrius.math.analysis.differentiation.UnivariateDifferentiableFunction;
import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Test for class {@link Gaussian}.
 */
public class GaussianTest {
    private final double EPS = Math.ulp(1d);

    @Test(expected = NotStrictlyPositiveException.class)
    public void testPreconditions() {
        new Gaussian(1, 2, -1);
    }

    @Test
    public void testSomeValues() {
        final UnivariateFunction f = new Gaussian();

        Assert.assertEquals(1 / MathLib.sqrt(2 * Math.PI), f.value(0), this.EPS);
    }

    @Test
    public void testLargeArguments() {
        final UnivariateFunction f = new Gaussian();

        Assert.assertEquals(0, f.value(Double.NEGATIVE_INFINITY), 0);
        Assert.assertEquals(0, f.value(-Double.MAX_VALUE), 0);
        Assert.assertEquals(0, f.value(-1e2), 0);
        Assert.assertEquals(0, f.value(1e2), 0);
        Assert.assertEquals(0, f.value(Double.MAX_VALUE), 0);
        Assert.assertEquals(0, f.value(Double.POSITIVE_INFINITY), 0);
    }

    @Test
    public void testDerivatives() {
        final UnivariateDifferentiableFunction gaussian = new Gaussian(2.0, 0.9, 3.0);
        final DerivativeStructure dsX = new DerivativeStructure(1, 4, 0, 1.1);
        final DerivativeStructure dsY = gaussian.value(dsX);
        Assert.assertEquals(1.9955604901712128349, dsY.getValue(), this.EPS);
        Assert.assertEquals(-0.044345788670471396332, dsY.getPartialDerivative(1), this.EPS);
        Assert.assertEquals(-0.22074348138190206174, dsY.getPartialDerivative(2), this.EPS);
        Assert.assertEquals(0.014760030401924800557, dsY.getPartialDerivative(3), this.EPS);
        Assert.assertEquals(0.073253159785035691678, dsY.getPartialDerivative(4), this.EPS);
    }

    @Test
    public void testDerivativeLargeArguments() {
        final Gaussian f = new Gaussian(0, 1e-50);

        Assert.assertEquals(0, f.value(new DerivativeStructure(1, 1, 0, Double.NEGATIVE_INFINITY))
            .getPartialDerivative(1), 0);
        Assert.assertEquals(0, f.value(new DerivativeStructure(1, 1, 0, -Double.MAX_VALUE)).getPartialDerivative(1), 0);
        Assert.assertEquals(0, f.value(new DerivativeStructure(1, 1, 0, -1e50)).getPartialDerivative(1), 0);
        Assert.assertEquals(0, f.value(new DerivativeStructure(1, 1, 0, -1e2)).getPartialDerivative(1), 0);
        Assert.assertEquals(0, f.value(new DerivativeStructure(1, 1, 0, 1e2)).getPartialDerivative(1), 0);
        Assert.assertEquals(0, f.value(new DerivativeStructure(1, 1, 0, 1e50)).getPartialDerivative(1), 0);
        Assert.assertEquals(0, f.value(new DerivativeStructure(1, 1, 0, Double.MAX_VALUE)).getPartialDerivative(1), 0);
        Assert.assertEquals(0, f.value(new DerivativeStructure(1, 1, 0, Double.POSITIVE_INFINITY))
            .getPartialDerivative(1), 0);
    }

    @Test(expected = NullArgumentException.class)
    public void testParametricUsage1() {
        final Gaussian.Parametric g = new Gaussian.Parametric();
        g.value(0, null);
    }

    @Test(expected = DimensionMismatchException.class)
    public void testParametricUsage2() {
        final Gaussian.Parametric g = new Gaussian.Parametric();
        g.value(0, new double[] { 0 });
    }

    @Test(expected = NotStrictlyPositiveException.class)
    public void testParametricUsage3() {
        final Gaussian.Parametric g = new Gaussian.Parametric();
        g.value(0, new double[] { 0, 1, 0 });
    }

    @Test(expected = NullArgumentException.class)
    public void testParametricUsage4() {
        final Gaussian.Parametric g = new Gaussian.Parametric();
        g.gradient(0, null);
    }

    @Test(expected = DimensionMismatchException.class)
    public void testParametricUsage5() {
        final Gaussian.Parametric g = new Gaussian.Parametric();
        g.gradient(0, new double[] { 0 });
    }

    @Test(expected = NotStrictlyPositiveException.class)
    public void testParametricUsage6() {
        final Gaussian.Parametric g = new Gaussian.Parametric();
        g.gradient(0, new double[] { 0, 1, 0 });
    }

    @Test
    public void testParametricValue() {
        final double norm = 2;
        final double mean = 3;
        final double sigma = 4;
        final Gaussian f = new Gaussian(norm, mean, sigma);

        final Gaussian.Parametric g = new Gaussian.Parametric();
        Assert.assertEquals(f.value(-1), g.value(-1, new double[] { norm, mean, sigma }), 0);
        Assert.assertEquals(f.value(0), g.value(0, new double[] { norm, mean, sigma }), 0);
        Assert.assertEquals(f.value(2), g.value(2, new double[] { norm, mean, sigma }), 0);
    }

    @Test
    public void testParametricGradient() {
        final double norm = 2;
        final double mean = 3;
        final double sigma = 4;
        final Gaussian.Parametric f = new Gaussian.Parametric();

        final double x = 1;
        final double[] grad = f.gradient(1, new double[] { norm, mean, sigma });
        final double diff = x - mean;
        final double n = MathLib.exp(-diff * diff / (2 * sigma * sigma));
        Assert.assertEquals(n, grad[0], this.EPS);
        final double m = norm * n * diff / (sigma * sigma);
        Assert.assertEquals(m, grad[1], this.EPS);
        final double s = m * diff / sigma;
        Assert.assertEquals(s, grad[2], this.EPS);
    }
}
