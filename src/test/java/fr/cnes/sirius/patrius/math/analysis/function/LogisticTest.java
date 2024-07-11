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
package fr.cnes.sirius.patrius.math.analysis.function;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.differentiation.DerivativeStructure;
import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Test for class {@link Logistic}.
 */
public class LogisticTest {
    private final double EPS = Math.ulp(1d);

    @Test(expected = NotStrictlyPositiveException.class)
    public void testPreconditions1() {
        new Logistic(1, 0, 1, 1, 0, -1);
    }

    @Test(expected = NotStrictlyPositiveException.class)
    public void testPreconditions2() {
        new Logistic(1, 0, 1, 1, 0, 0);
    }

    @Test
    public void testCompareSigmoid() {
        final UnivariateFunction sig = new Sigmoid();
        final UnivariateFunction sigL = new Logistic(1, 0, 1, 1, 0, 1);

        final double min = -2;
        final double max = 2;
        final int n = 100;
        final double delta = (max - min) / n;
        for (int i = 0; i < n; i++) {
            final double x = min + i * delta;
            Assert.assertEquals("x=" + x, sig.value(x), sigL.value(x), this.EPS);
        }
    }

    @Test
    public void testSomeValues() {
        final double k = 4;
        final double m = 5;
        final double b = 2;
        final double q = 3;
        final double a = -1;
        final double n = 2;

        final UnivariateFunction f = new Logistic(k, m, b, q, a, n);

        double x;
        x = m;
        Assert.assertEquals("x=" + x, a + (k - a) / MathLib.sqrt(1 + q), f.value(x), this.EPS);

        x = Double.NEGATIVE_INFINITY;
        Assert.assertEquals("x=" + x, a, f.value(x), this.EPS);

        x = Double.POSITIVE_INFINITY;
        Assert.assertEquals("x=" + x, k, f.value(x), this.EPS);
    }

    @Test
    public void testCompareDerivativeSigmoid() {
        final double k = 3;
        final double a = 2;

        final Logistic f = new Logistic(k, 0, 1, 1, a, 1);
        final Sigmoid g = new Sigmoid(a, k);

        final double min = -10;
        final double max = 10;
        final double n = 20;
        final double delta = (max - min) / n;
        for (int i = 0; i < n; i++) {
            final DerivativeStructure x = new DerivativeStructure(1, 5, 0, min + i * delta);
            for (int order = 0; order <= x.getOrder(); ++order) {
                Assert.assertEquals("x=" + x.getValue(),
                    g.value(x).getPartialDerivative(order),
                    f.value(x).getPartialDerivative(order),
                    3.0e-15);
            }
        }
    }

    @Test(expected = NullArgumentException.class)
    public void testParametricUsage1() {
        final Logistic.Parametric g = new Logistic.Parametric();
        g.value(0, null);
    }

    @Test(expected = DimensionMismatchException.class)
    public void testParametricUsage2() {
        final Logistic.Parametric g = new Logistic.Parametric();
        g.value(0, new double[] { 0 });
    }

    @Test(expected = NullArgumentException.class)
    public void testParametricUsage3() {
        final Logistic.Parametric g = new Logistic.Parametric();
        g.gradient(0, null);
    }

    @Test(expected = DimensionMismatchException.class)
    public void testParametricUsage4() {
        final Logistic.Parametric g = new Logistic.Parametric();
        g.gradient(0, new double[] { 0 });
    }

    @Test(expected = NotStrictlyPositiveException.class)
    public void testParametricUsage5() {
        final Logistic.Parametric g = new Logistic.Parametric();
        g.value(0, new double[] { 1, 0, 1, 1, 0, 0 });
    }

    @Test(expected = NotStrictlyPositiveException.class)
    public void testParametricUsage6() {
        final Logistic.Parametric g = new Logistic.Parametric();
        g.gradient(0, new double[] { 1, 0, 1, 1, 0, 0 });
    }

    @Test
    public void testGradientComponent0Component4() {
        final double k = 3;
        final double a = 2;

        final Logistic.Parametric f = new Logistic.Parametric();
        // Compare using the "Sigmoid" function.
        final Sigmoid.Parametric g = new Sigmoid.Parametric();

        final double x = 0.12345;
        final double[] gf = f.gradient(x, new double[] { k, 0, 1, 1, a, 1 });
        final double[] gg = g.gradient(x, new double[] { a, k });

        Assert.assertEquals(gg[0], gf[4], this.EPS);
        Assert.assertEquals(gg[1], gf[0], this.EPS);
    }

    @Test
    public void testGradientComponent5() {
        final double m = 1.2;
        final double k = 3.4;
        final double a = 2.3;
        final double q = 0.567;
        final double b = -MathLib.log(q);
        final double n = 3.4;

        final Logistic.Parametric f = new Logistic.Parametric();

        final double x = m - 1;
        final double qExp1 = 2;

        final double[] gf = f.gradient(x, new double[] { k, m, b, q, a, n });

        Assert.assertEquals((k - a) * MathLib.log(qExp1) / (n * n * MathLib.pow(qExp1, 1 / n)),
            gf[5], this.EPS);
    }

    @Test
    public void testGradientComponent1Component2Component3() {
        final double m = 1.2;
        final double k = 3.4;
        final double a = 2.3;
        final double b = 0.567;
        final double q = 1 / MathLib.exp(b * m);
        final double n = 3.4;

        final Logistic.Parametric f = new Logistic.Parametric();

        final double x = 0;
        final double qExp1 = 2;

        final double[] gf = f.gradient(x, new double[] { k, m, b, q, a, n });

        final double factor = (a - k) / (n * MathLib.pow(qExp1, 1 / n + 1));
        Assert.assertEquals(factor * b, gf[1], this.EPS);
        Assert.assertEquals(factor * m, gf[2], this.EPS);
        Assert.assertEquals(factor / q, gf[3], this.EPS);
    }
}
