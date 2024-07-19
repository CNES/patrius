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
 * VERSION::FA:345:31/10/2014: coverage
 * VERSION::DM:1305:16/11/2017: Serializable interface implementation
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.math.analysis;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.analysis.differentiation.DerivativeStructure;
import fr.cnes.sirius.patrius.math.analysis.differentiation.UnivariateDifferentiableFunction;
import fr.cnes.sirius.patrius.math.analysis.function.Add;
import fr.cnes.sirius.patrius.math.analysis.function.Constant;
import fr.cnes.sirius.patrius.math.analysis.function.Cos;
import fr.cnes.sirius.patrius.math.analysis.function.Cosh;
import fr.cnes.sirius.patrius.math.analysis.function.Divide;
import fr.cnes.sirius.patrius.math.analysis.function.Identity;
import fr.cnes.sirius.patrius.math.analysis.function.Inverse;
import fr.cnes.sirius.patrius.math.analysis.function.Log;
import fr.cnes.sirius.patrius.math.analysis.function.Max;
import fr.cnes.sirius.patrius.math.analysis.function.Min;
import fr.cnes.sirius.patrius.math.analysis.function.Minus;
import fr.cnes.sirius.patrius.math.analysis.function.Multiply;
import fr.cnes.sirius.patrius.math.analysis.function.Pow;
import fr.cnes.sirius.patrius.math.analysis.function.Power;
import fr.cnes.sirius.patrius.math.analysis.function.Sin;
import fr.cnes.sirius.patrius.math.analysis.function.Sinc;
import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooLargeException;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Test for {@link FunctionUtils}.
 */
public class FunctionUtilsTest {
    private final double EPS = Math.ulp(1d);

    @Test
    public void testCompose() {
        final UnivariateFunction id = new Identity();
        Assert.assertEquals(3, FunctionUtils.compose(id, id, id).value(3), this.EPS);

        final UnivariateFunction c = new Constant(4);
        Assert.assertEquals(4, FunctionUtils.compose(id, c).value(3), this.EPS);
        Assert.assertEquals(4, FunctionUtils.compose(c, id).value(3), this.EPS);

        final UnivariateFunction m = new Minus();
        Assert.assertEquals(-3, FunctionUtils.compose(m).value(3), this.EPS);
        Assert.assertEquals(3, FunctionUtils.compose(m, m).value(3), this.EPS);

        final UnivariateFunction inv = new Inverse();
        Assert.assertEquals(-0.25, FunctionUtils.compose(inv, m, c, id).value(3), this.EPS);

        final UnivariateFunction pow = new Power(2);
        Assert.assertEquals(81, FunctionUtils.compose(pow, pow).value(3), this.EPS);
    }

    /**
     * Tests the value(double) method of compose for UnivariateDifferentiableFunction.
     * Same test as the previous one, testCompose() made for UnivariateFunction.
     * 
     * @since 2.3
     */
    @Test
    public void testComposeUnivariateDifferentiableFunction() {
        final UnivariateDifferentiableFunction id = new Identity();
        Assert.assertEquals(3, FunctionUtils.compose(id, id, id).value(3), this.EPS);

        final UnivariateDifferentiableFunction c = new Constant(4);
        Assert.assertEquals(4, FunctionUtils.compose(id, c).value(3), this.EPS);
        Assert.assertEquals(4, FunctionUtils.compose(c, id).value(3), this.EPS);

        final UnivariateDifferentiableFunction m = new Minus();
        Assert.assertEquals(-3, FunctionUtils.compose(m).value(3), this.EPS);
        Assert.assertEquals(3, FunctionUtils.compose(m, m).value(3), this.EPS);

        final UnivariateDifferentiableFunction inv = new Inverse();
        Assert.assertEquals(-0.25, FunctionUtils.compose(inv, m, c, id).value(3), this.EPS);

        final UnivariateDifferentiableFunction pow = new Power(2);
        Assert.assertEquals(81, FunctionUtils.compose(pow, pow).value(3), this.EPS);
    }

    @Test
    public void testComposeDifferentiable() {
        final UnivariateDifferentiableFunction id = new Identity();
        Assert.assertEquals(1, FunctionUtils.compose(id, id, id).value(new DerivativeStructure(1, 1, 0, 3))
            .getPartialDerivative(1), this.EPS);

        final UnivariateDifferentiableFunction c = new Constant(4);
        Assert.assertEquals(0, FunctionUtils.compose(id, c).value(new DerivativeStructure(1, 1, 0, 3))
            .getPartialDerivative(1), this.EPS);
        Assert.assertEquals(0, FunctionUtils.compose(c, id).value(new DerivativeStructure(1, 1, 0, 3))
            .getPartialDerivative(1), this.EPS);

        final UnivariateDifferentiableFunction m = new Minus();
        Assert.assertEquals(-1, FunctionUtils.compose(m).value(new DerivativeStructure(1, 1, 0, 3))
            .getPartialDerivative(1), this.EPS);
        Assert.assertEquals(1, FunctionUtils.compose(m, m).value(new DerivativeStructure(1, 1, 0, 3))
            .getPartialDerivative(1), this.EPS);

        final UnivariateDifferentiableFunction inv = new Inverse();
        Assert.assertEquals(0.25, FunctionUtils.compose(inv, m, id).value(new DerivativeStructure(1, 1, 0, 2))
            .getPartialDerivative(1), this.EPS);

        final UnivariateDifferentiableFunction pow = new Power(2);
        Assert.assertEquals(108, FunctionUtils.compose(pow, pow).value(new DerivativeStructure(1, 1, 0, 3))
            .getPartialDerivative(1), this.EPS);

        final UnivariateDifferentiableFunction log = new Log();
        final double a = 9876.54321;
        Assert.assertEquals(pow.value(new DerivativeStructure(1, 1, 0, a)).getPartialDerivative(1) / pow.value(a),
            FunctionUtils.compose(log, pow).value(new DerivativeStructure(1, 1, 0, a)).getPartialDerivative(1),
            this.EPS);
    }

    @Test
    public void testAdd() {
        final UnivariateFunction id = new Identity();
        final UnivariateFunction c = new Constant(4);
        final UnivariateFunction m = new Minus();
        final UnivariateFunction inv = new Inverse();

        Assert.assertEquals(4.5, FunctionUtils.add(inv, m, c, id).value(2), this.EPS);
        Assert.assertEquals(4 + 2, FunctionUtils.add(c, id).value(2), this.EPS);
        Assert.assertEquals(4 - 2, FunctionUtils.add(c, FunctionUtils.compose(m, id)).value(2), this.EPS);
    }

    @Test
    public void testAddDifferentiable() {
        final UnivariateDifferentiableFunction sin = new Sin();
        final UnivariateDifferentiableFunction c = new Constant(4);
        final UnivariateDifferentiableFunction m = new Minus();
        final UnivariateDifferentiableFunction inv = new Inverse();

        final double a = 123.456;
        Assert.assertEquals(-1 / (a * a) - 1 + Math.cos(a),
            FunctionUtils.add(inv, m, c, sin).value(new DerivativeStructure(1, 1, 0, a)).getPartialDerivative(1),
            this.EPS);
        Assert.assertEquals(2, FunctionUtils.add(c, m).value(2), this.EPS);

    }

    @Test
    public void testMultiply() {
        final UnivariateFunction c = new Constant(4);
        Assert.assertEquals(16, FunctionUtils.multiply(c, c).value(12345), this.EPS);

        final UnivariateFunction inv = new Inverse();
        final UnivariateFunction pow = new Power(2);
        Assert.assertEquals(1, FunctionUtils.multiply(FunctionUtils.compose(inv, pow), pow).value(3.5), this.EPS);
    }

    @Test
    public void testMultiplyDifferentiable() {
        final UnivariateDifferentiableFunction c = new Constant(4);
        final UnivariateDifferentiableFunction id = new Identity();
        final double a = 1.2345678;
        Assert.assertEquals(8 * a, FunctionUtils.multiply(c, id, id).value(new DerivativeStructure(1, 1, 0, a))
            .getPartialDerivative(1), this.EPS);

        final UnivariateDifferentiableFunction inv = new Inverse();
        final UnivariateDifferentiableFunction pow = new Power(2.5);
        final UnivariateDifferentiableFunction cos = new Cos();
        Assert.assertEquals(1.5 * Math.sqrt(a) * Math.cos(a) - Math.pow(a, 1.5) * Math.sin(a),
            FunctionUtils.multiply(inv, pow, cos).value(new DerivativeStructure(1, 1, 0, a))
                .getPartialDerivative(1), this.EPS);

        final UnivariateDifferentiableFunction cosh = new Cosh();
        Assert.assertEquals(
            1.5 * Math.sqrt(a) * Math.cosh(a) + Math.pow(a, 1.5) * Math.sinh(a),
            FunctionUtils.multiply(inv, pow, cosh).value(new DerivativeStructure(1, 1, 0, a))
                .getPartialDerivative(1), 8 * this.EPS);

        Assert.assertEquals(12, FunctionUtils.multiply(id, c).value(3), this.EPS);
    }

    @Test
    public void testCombine() {
        BivariateFunction bi = new Add();
        final UnivariateFunction id = new Identity();
        final UnivariateFunction m = new Minus();
        UnivariateFunction c = FunctionUtils.combine(bi, id, m);
        Assert.assertEquals(0, c.value(2.3456), this.EPS);

        bi = new Multiply();
        final UnivariateFunction inv = new Inverse();
        c = FunctionUtils.combine(bi, id, inv);
        Assert.assertEquals(1, c.value(2.3456), this.EPS);
    }

    @Test
    public void testCollector() {
        BivariateFunction bi = new Add();
        MultivariateFunction coll = FunctionUtils.collector(bi, 0);
        Assert.assertEquals(10, coll.value(new double[] { 1, 2, 3, 4 }), this.EPS);

        bi = new Multiply();
        coll = FunctionUtils.collector(bi, 1);
        Assert.assertEquals(24, coll.value(new double[] { 1, 2, 3, 4 }), this.EPS);

        bi = new Max();
        coll = FunctionUtils.collector(bi, Double.NEGATIVE_INFINITY);
        Assert.assertEquals(10, coll.value(new double[] { 1, -2, 7.5, 10, -24, 9.99 }), 0);

        bi = new Min();
        coll = FunctionUtils.collector(bi, Double.POSITIVE_INFINITY);
        Assert.assertEquals(-24, coll.value(new double[] { 1, -2, 7.5, 10, -24, 9.99 }), 0);
    }

    @Test
    public void testSinc() {
        final BivariateFunction div = new Divide();
        final UnivariateFunction sin = new Sin();
        final UnivariateFunction id = new Identity();
        final UnivariateFunction sinc1 = FunctionUtils.combine(div, sin, id);
        final UnivariateFunction sinc2 = new Sinc();

        for (int i = 0; i < 10; i++) {
            final double x = Math.random();
            Assert.assertEquals(sinc1.value(x), sinc2.value(x), this.EPS);
        }
    }

    @Test
    public void testFixingArguments() {
        final UnivariateFunction scaler = FunctionUtils.fix1stArgument(new Multiply(), 10);
        Assert.assertEquals(1.23456, scaler.value(0.123456), this.EPS);

        final UnivariateFunction pow1 = new Power(2);
        final UnivariateFunction pow2 = FunctionUtils.fix2ndArgument(new Pow(), 2);

        for (int i = 0; i < 10; i++) {
            final double x = Math.random() * 10;
            Assert.assertEquals(pow1.value(x), pow2.value(x), 0);
        }
    }

    @Test(expected = NumberIsTooLargeException.class)
    public void testSampleWrongBounds() {
        FunctionUtils.sample(new Sin(), Math.PI, 0.0, 10);
    }

    @Test(expected = NotStrictlyPositiveException.class)
    public void testSampleNegativeNumberOfPoints() {
        FunctionUtils.sample(new Sin(), 0.0, Math.PI, -1);
    }

    @Test(expected = NotStrictlyPositiveException.class)
    public void testSampleNullNumberOfPoints() {
        FunctionUtils.sample(new Sin(), 0.0, Math.PI, 0);
    }

    @Test
    public void testSample() {
        final int n = 11;
        final double min = 0.0;
        final double max = Math.PI;
        final double[] actual = FunctionUtils.sample(new Sin(), min, max, n);
        for (int i = 0; i < n; i++) {
            final double x = min + (max - min) / n * i;
            Assert.assertEquals("x = " + x, MathLib.sin(x), actual[i], 0.0);
        }
    }

    @Test
    public void testSerialization() {
        // Random test
        final UnivariateFunction ad = new Sin();

        // Creation for serialization test purpose
        final UnivariateFunction ad2 = TestUtils.serializeAndRecover(ad);

        // Test between the 2 objects
        TestUtils.checkSerializedEquality(ad.equals(ad2));
    }
}
