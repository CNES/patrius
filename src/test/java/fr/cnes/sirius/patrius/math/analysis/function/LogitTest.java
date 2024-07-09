/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
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
 * 
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.function;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.analysis.FunctionUtils;
import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.differentiation.DerivativeStructure;
import fr.cnes.sirius.patrius.math.analysis.differentiation.UnivariateDifferentiableFunction;
import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;
import fr.cnes.sirius.patrius.math.random.RandomGenerator;
import fr.cnes.sirius.patrius.math.random.Well1024a;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Test for class {@link Logit}.
 */
public class LogitTest {
    private final double EPS = Math.ulp(1d);

    @Test(expected = OutOfRangeException.class)
    public void testPreconditions1() {
        final double lo = -1;
        final double hi = 2;
        final UnivariateFunction f = new Logit(lo, hi);

        f.value(lo - 1);
    }

    @Test(expected = OutOfRangeException.class)
    public void testPreconditions2() {
        final double lo = -1;
        final double hi = 2;
        final UnivariateFunction f = new Logit(lo, hi);

        f.value(hi + 1);
    }

    @Test
    public void testSomeValues() {
        final double lo = 1;
        final double hi = 2;
        final UnivariateFunction f = new Logit(lo, hi);

        Assert.assertEquals(Double.NEGATIVE_INFINITY, f.value(1), this.EPS);
        Assert.assertEquals(Double.POSITIVE_INFINITY, f.value(2), this.EPS);
        Assert.assertEquals(0, f.value(1.5), this.EPS);
    }

    @Test
    public void testDerivative() {
        final double lo = 1;
        final double hi = 2;
        final Logit f = new Logit(lo, hi);
        final DerivativeStructure f15 = f.value(new DerivativeStructure(1, 1, 0, 1.5));

        Assert.assertEquals(4, f15.getPartialDerivative(1), this.EPS);
    }

    @Test
    public void testDerivativeLargeArguments() {
        final Logit f = new Logit(1, 2);

        for (final double arg : new double[] {
            Double.NEGATIVE_INFINITY, -Double.MAX_VALUE, -1e155, 1e155, Double.MAX_VALUE, Double.POSITIVE_INFINITY
        }) {
            try {
                f.value(new DerivativeStructure(1, 1, 0, arg));
                Assert.fail("an exception should have been thrown");
            } catch (final OutOfRangeException ore) {
                // expected
            } catch (final Exception e) {
                Assert.fail("wrong exception caught: " + e.getMessage());
            }
        }
    }

    @Test
    public void testDerivativesHighOrder() {
        final DerivativeStructure l = new Logit(1, 3).value(new DerivativeStructure(1, 5, 0, 1.2));
        Assert.assertEquals(-2.1972245773362193828, l.getPartialDerivative(0), 1.0e-16);
        Assert.assertEquals(5.5555555555555555555, l.getPartialDerivative(1), 9.0e-16);
        Assert.assertEquals(-24.691358024691358025, l.getPartialDerivative(2), 2.0e-14);
        Assert.assertEquals(250.34293552812071331, l.getPartialDerivative(3), 2.0e-13);
        Assert.assertEquals(-3749.4284407864654778, l.getPartialDerivative(4), 4.0e-12);
        Assert.assertEquals(75001.270131585632282, l.getPartialDerivative(5), 8.0e-11);
    }

    @Test(expected = NullArgumentException.class)
    public void testParametricUsage1() {
        final Logit.Parametric g = new Logit.Parametric();
        g.value(0, null);
    }

    @Test(expected = DimensionMismatchException.class)
    public void testParametricUsage2() {
        final Logit.Parametric g = new Logit.Parametric();
        g.value(0, new double[] { 0 });
    }

    @Test(expected = NullArgumentException.class)
    public void testParametricUsage3() {
        final Logit.Parametric g = new Logit.Parametric();
        g.gradient(0, null);
    }

    @Test(expected = DimensionMismatchException.class)
    public void testParametricUsage4() {
        final Logit.Parametric g = new Logit.Parametric();
        g.gradient(0, new double[] { 0 });
    }

    @Test(expected = OutOfRangeException.class)
    public void testParametricUsage5() {
        final Logit.Parametric g = new Logit.Parametric();
        g.value(-1, new double[] { 0, 1 });
    }

    @Test(expected = OutOfRangeException.class)
    public void testParametricUsage6() {
        final Logit.Parametric g = new Logit.Parametric();
        g.value(2, new double[] { 0, 1 });
    }

    @Test
    public void testParametricValue() {
        final double lo = 2;
        final double hi = 3;
        final Logit f = new Logit(lo, hi);

        final Logit.Parametric g = new Logit.Parametric();
        Assert.assertEquals(f.value(2), g.value(2, new double[] { lo, hi }), 0);
        Assert.assertEquals(f.value(2.34567), g.value(2.34567, new double[] { lo, hi }), 0);
        Assert.assertEquals(f.value(3), g.value(3, new double[] { lo, hi }), 0);
    }

    @Test
    public void testValueWithInverseFunction() {
        final double lo = 2;
        final double hi = 3;
        final Logit f = new Logit(lo, hi);
        final Sigmoid g = new Sigmoid(lo, hi);
        final RandomGenerator random = new Well1024a(0x49914cdd9f0b8db5l);
        final UnivariateDifferentiableFunction id = FunctionUtils.compose(g,
            f);

        for (int i = 0; i < 10; i++) {
            final double x = lo + random.nextDouble() * (hi - lo);
            Assert.assertEquals(x, id.value(new DerivativeStructure(1, 1, 0, x)).getValue(), this.EPS);
        }

        Assert.assertEquals(lo, id.value(new DerivativeStructure(1, 1, 0, lo)).getValue(), this.EPS);
        Assert.assertEquals(hi, id.value(new DerivativeStructure(1, 1, 0, hi)).getValue(), this.EPS);
    }

    @Test
    public void testDerivativesWithInverseFunction() {
        final double[] epsilon = new double[] { 1.0e-20, 4.0e-16, 3.0e-15, 2.0e-11, 3.0e-9, 1.0e-6 };
        final double lo = 2;
        final double hi = 3;
        final Logit f = new Logit(lo, hi);
        final Sigmoid g = new Sigmoid(lo, hi);
        final RandomGenerator random = new Well1024a(0x96885e9c1f81cea5l);
        final UnivariateDifferentiableFunction id =
            FunctionUtils.compose(g, f);
        for (int maxOrder = 0; maxOrder < 6; ++maxOrder) {
            double max = 0;
            for (int i = 0; i < 10; i++) {
                final double x = lo + random.nextDouble() * (hi - lo);
                final DerivativeStructure dsX = new DerivativeStructure(1, maxOrder, 0, x);
                max = MathLib.max(max, MathLib.abs(dsX.getPartialDerivative(maxOrder) -
                    id.value(dsX).getPartialDerivative(maxOrder)));
                Assert.assertEquals(dsX.getPartialDerivative(maxOrder),
                    id.value(dsX).getPartialDerivative(maxOrder),
                    epsilon[maxOrder]);
            }

            // each function evaluates correctly near boundaries,
            // but combination leads to NaN as some intermediate point is infinite
            final DerivativeStructure dsLo = new DerivativeStructure(1, maxOrder, 0, lo);
            if (maxOrder == 0) {
                Assert.assertTrue(Double.isInfinite(f.value(dsLo).getPartialDerivative(maxOrder)));
                Assert.assertEquals(lo, id.value(dsLo).getPartialDerivative(maxOrder), epsilon[maxOrder]);
            } else if (maxOrder == 1) {
                Assert.assertTrue(Double.isInfinite(f.value(dsLo).getPartialDerivative(maxOrder)));
                Assert.assertTrue(Double.isNaN(id.value(dsLo).getPartialDerivative(maxOrder)));
            } else {
                Assert.assertTrue(Double.isNaN(f.value(dsLo).getPartialDerivative(maxOrder)));
                Assert.assertTrue(Double.isNaN(id.value(dsLo).getPartialDerivative(maxOrder)));
            }

            final DerivativeStructure dsHi = new DerivativeStructure(1, maxOrder, 0, hi);
            if (maxOrder == 0) {
                Assert.assertTrue(Double.isInfinite(f.value(dsHi).getPartialDerivative(maxOrder)));
                Assert.assertEquals(hi, id.value(dsHi).getPartialDerivative(maxOrder), epsilon[maxOrder]);
            } else if (maxOrder == 1) {
                Assert.assertTrue(Double.isInfinite(f.value(dsHi).getPartialDerivative(maxOrder)));
                Assert.assertTrue(Double.isNaN(id.value(dsHi).getPartialDerivative(maxOrder)));
            } else {
                Assert.assertTrue(Double.isNaN(f.value(dsHi).getPartialDerivative(maxOrder)));
                Assert.assertTrue(Double.isNaN(id.value(dsHi).getPartialDerivative(maxOrder)));
            }

        }
    }
}
