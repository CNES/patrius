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
 * VERSION:4.5:DM:DM-2300:27/05/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear 
* VERSION:4.4:FA:FA-2137:04/10/2019:FA mineure Patrius V4.3
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.util;

import java.math.BigDecimal;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.exception.MathArithmeticException;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;

/**
 * Test cases for the {@link Precision} class.
 * 
 * @version $Id: PrecisionTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class PrecisionTest {
    @Test
    public void testEqualsWithRelativeTolerance() {
        Assert.assertTrue(Precision.equalsWithRelativeTolerance(0d, 0d, 0d));
        Assert.assertTrue(Precision.equalsWithRelativeTolerance(0d, 1 / Double.NEGATIVE_INFINITY, 0d));

        final double eps = 1e-14;
        Assert.assertFalse(Precision.equalsWithRelativeTolerance(1.987654687654968, 1.987654687654988, eps));
        Assert.assertTrue(Precision.equalsWithRelativeTolerance(1.987654687654968, 1.987654687654987, eps));
        Assert.assertFalse(Precision.equalsWithRelativeTolerance(1.987654687654968, 1.987654687654948, eps));
        Assert.assertTrue(Precision.equalsWithRelativeTolerance(1.987654687654968, 1.987654687654949, eps));

        Assert.assertFalse(Precision.equalsWithRelativeTolerance(Precision.SAFE_MIN, 0.0, eps));

        Assert.assertFalse(Precision.equalsWithRelativeTolerance(1.0000000000001e-300, 1e-300, eps));
        Assert.assertTrue(Precision.equalsWithRelativeTolerance(1.00000000000001e-300, 1e-300, eps));

        Assert.assertFalse(Precision.equalsWithRelativeTolerance(Double.NEGATIVE_INFINITY, 1.23, eps));
        Assert.assertFalse(Precision.equalsWithRelativeTolerance(Double.POSITIVE_INFINITY, 1.23, eps));

        Assert.assertTrue(Precision
            .equalsWithRelativeTolerance(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, eps));
        Assert.assertTrue(Precision
            .equalsWithRelativeTolerance(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, eps));
        Assert.assertFalse(Precision.equalsWithRelativeTolerance(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
            eps));

        Assert.assertFalse(Precision.equalsWithRelativeTolerance(Double.NaN, 1.23, eps));
        Assert.assertFalse(Precision.equalsWithRelativeTolerance(Double.NaN, Double.NaN, eps));
    }

    @Test
    public void testEqualsWithAbsoluteAndRelativeTolerances() {

        Assert.assertTrue(Precision.equalsWithAbsoluteAndRelativeTolerances(0d, 0d, 0d, 0d));
        Assert.assertTrue(Precision.equalsWithAbsoluteAndRelativeTolerances(0d, 1 / Double.NEGATIVE_INFINITY, 0d, 0d));

        double eps = 1e-14;
        double abs = 1e-13;
        Assert.assertFalse(Precision.equalsWithAbsoluteAndRelativeTolerances(1.987654687654968, 1.987654687654988, eps,
            abs));
        Assert.assertTrue(Precision.equalsWithAbsoluteAndRelativeTolerances(1.987654687654968, 1.987654687654987, eps,
            abs));
        Assert.assertFalse(Precision.equalsWithAbsoluteAndRelativeTolerances(1.987654687654968, 1.987654687654948, eps,
            abs));
        Assert.assertTrue(Precision.equalsWithAbsoluteAndRelativeTolerances(1.987654687654968, 1.987654687654949, eps,
            abs));

        eps = 1e-3;
        Assert.assertTrue(Precision.equalsWithAbsoluteAndRelativeTolerances(153.0000, 153.0000, eps, .0625));
        Assert.assertTrue(Precision.equalsWithAbsoluteAndRelativeTolerances(153.0000, 153.0625, eps, .0625));
        Assert.assertTrue(Precision.equalsWithAbsoluteAndRelativeTolerances(152.9375, 153.0000, eps, .0625));
        Assert.assertFalse(Precision.equalsWithAbsoluteAndRelativeTolerances(153.0000, 153.0625, eps, .0624));
        Assert.assertFalse(Precision.equalsWithAbsoluteAndRelativeTolerances(152.9374, 153.0000, eps, .0625));
        Assert.assertFalse(Precision.equalsWithAbsoluteAndRelativeTolerances(Double.NaN, Double.NaN, eps, 1.0));
        Assert.assertTrue(Precision.equalsWithAbsoluteAndRelativeTolerances(Double.POSITIVE_INFINITY,
            Double.POSITIVE_INFINITY, eps, 1.0));
        Assert.assertTrue(Precision.equalsWithAbsoluteAndRelativeTolerances(Double.NEGATIVE_INFINITY,
            Double.NEGATIVE_INFINITY, eps, 1.0));
        Assert.assertFalse(Precision.equalsWithAbsoluteAndRelativeTolerances(Double.NEGATIVE_INFINITY,
            Double.POSITIVE_INFINITY, eps, 1.0));
    }

    @Test
    public void testEqualsIncludingNaN() {
        final double[] testArray = {
            Double.NaN,
            Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY,
            1d,
            0d };
        for (int i = 0; i < testArray.length; i++) {
            for (int j = 0; j < testArray.length; j++) {
                if (i == j) {
                    Assert.assertTrue(Precision.equalsIncludingNaN(testArray[i], testArray[j]));
                    Assert.assertTrue(Precision.equalsIncludingNaN(testArray[j], testArray[i]));
                } else {
                    Assert.assertTrue(!Precision.equalsIncludingNaN(testArray[i], testArray[j]));
                    Assert.assertTrue(!Precision.equalsIncludingNaN(testArray[j], testArray[i]));
                }
            }
        }
    }

    @Test
    public void testEqualsWithAllowedDelta() {
        Assert.assertTrue(Precision.equals(153.0000, 153.0000, .0625));
        Assert.assertTrue(Precision.equals(153.0000, 153.0625, .0625));
        Assert.assertTrue(Precision.equals(152.9375, 153.0000, .0625));
        Assert.assertFalse(Precision.equals(153.0000, 153.0625, .0624));
        Assert.assertFalse(Precision.equals(152.9374, 153.0000, .0625));
        Assert.assertFalse(Precision.equals(Double.NaN, Double.NaN, 1.0));
        Assert.assertTrue(Precision.equals(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, 1.0));
        Assert.assertTrue(Precision.equals(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, 1.0));
        Assert.assertFalse(Precision.equals(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 1.0));
    }

    @Test
    public void testMath475() {
        final double a = 1.7976931348623182E16;
        final double b = MathLib.nextUp(a);

        double diff = MathLib.abs(a - b);
        // Because they are adjacent floating point numbers, "a" and "b" are
        // considered equal even though the allowed error is smaller than
        // their difference.
        Assert.assertTrue(Precision.equals(a, b, 0.5 * diff));

        final double c = MathLib.nextUp(b);
        diff = MathLib.abs(a - c);
        // Because "a" and "c" are not adjacent, the tolerance is taken into
        // account for assessing equality.
        Assert.assertTrue(Precision.equals(a, c, diff));
        Assert.assertFalse(Precision.equals(a, c, (1 - 1e-16) * diff));
    }

    @Test
    public void testEqualsIncludingNaNWithAllowedDelta() {
        Assert.assertTrue(Precision.equalsIncludingNaN(153.0000, 153.0000, .0625));
        Assert.assertTrue(Precision.equalsIncludingNaN(153.0000, 153.0625, .0625));
        Assert.assertTrue(Precision.equalsIncludingNaN(152.9375, 153.0000, .0625));
        Assert.assertTrue(Precision.equalsIncludingNaN(Double.NaN, Double.NaN, 1.0));
        Assert.assertTrue(Precision.equalsIncludingNaN(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, 1.0));
        Assert.assertTrue(Precision.equalsIncludingNaN(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, 1.0));
        Assert.assertFalse(Precision.equalsIncludingNaN(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 1.0));
        Assert.assertFalse(Precision.equalsIncludingNaN(153.0000, 153.0625, .0624));
        Assert.assertFalse(Precision.equalsIncludingNaN(152.9374, 153.0000, .0625));
    }

    // Tests for floating point equality
    @Test
    public void testFloatEqualsWithAllowedUlps() {
        Assert.assertTrue("+0.0f == -0.0f", Precision.equals(0.0f, -0.0f));
        Assert.assertTrue("+0.0f == -0.0f (1 ulp)", Precision.equals(0.0f, -0.0f, 1));
        final float oneFloat = 1.0f;
        Assert.assertTrue("1.0f == 1.0f + 1 ulp",
            Precision.equals(oneFloat, Float.intBitsToFloat(1 + Float.floatToIntBits(oneFloat))));
        Assert.assertTrue("1.0f == 1.0f + 1 ulp (1 ulp)",
            Precision.equals(oneFloat, Float.intBitsToFloat(1 + Float.floatToIntBits(oneFloat)), 1));
        Assert.assertFalse("1.0f != 1.0f + 2 ulp (1 ulp)",
            Precision.equals(oneFloat, Float.intBitsToFloat(2 + Float.floatToIntBits(oneFloat)), 1));

        Assert.assertTrue(Precision.equals(153.0f, 153.0f, 1));

        // These tests need adjusting for floating point precision
        // Assert.assertTrue(Precision.equals(153.0f, 153.00000000000003f, 1));
        // Assert.assertFalse(Precision.equals(153.0f, 153.00000000000006f, 1));
        // Assert.assertTrue(Precision.equals(153.0f, 152.99999999999997f, 1));
        // Assert.assertFalse(Precision.equals(153f, 152.99999999999994f, 1));
        //
        // Assert.assertTrue(Precision.equals(-128.0f, -127.99999999999999f, 1));
        // Assert.assertFalse(Precision.equals(-128.0f, -127.99999999999997f, 1));
        // Assert.assertTrue(Precision.equals(-128.0f, -128.00000000000003f, 1));
        // Assert.assertFalse(Precision.equals(-128.0f, -128.00000000000006f, 1));

        Assert.assertTrue(Precision.equals(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, 1));
        Assert.assertTrue(Precision.equals(Double.MAX_VALUE, Float.POSITIVE_INFINITY, 1));

        Assert.assertTrue(Precision.equals(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, 1));
        Assert.assertTrue(Precision.equals(-Float.MAX_VALUE, Float.NEGATIVE_INFINITY, 1));

        Assert.assertFalse(Precision.equals(Float.NaN, Float.NaN, 1));
        Assert.assertFalse(Precision.equals(Float.NaN, Float.NaN, 0));
        Assert.assertFalse(Precision.equals(Float.NaN, 0, 0));
        Assert.assertFalse(Precision.equals(Float.NaN, Float.POSITIVE_INFINITY, 0));
        Assert.assertFalse(Precision.equals(Float.NaN, Float.NEGATIVE_INFINITY, 0));

        Assert.assertFalse(Precision.equals(Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, 100000));
    }

    @Test
    public void testEqualsWithAllowedUlps() {
        Assert.assertTrue(Precision.equals(0.0, -0.0, 1));

        Assert.assertTrue(Precision.equals(1.0, 1 + MathLib.ulp(1d), 1));
        Assert.assertFalse(Precision.equals(1.0, 1 + 2 * MathLib.ulp(1d), 1));

        final double nUp1 = MathLib.nextAfter(1d, Double.POSITIVE_INFINITY);
        final double nnUp1 = MathLib.nextAfter(nUp1, Double.POSITIVE_INFINITY);
        Assert.assertTrue(Precision.equals(1.0, nUp1, 1));
        Assert.assertTrue(Precision.equals(nUp1, nnUp1, 1));
        Assert.assertFalse(Precision.equals(1.0, nnUp1, 1));

        Assert.assertTrue(Precision.equals(0.0, MathLib.ulp(0d), 1));
        Assert.assertTrue(Precision.equals(0.0, -MathLib.ulp(0d), 1));

        Assert.assertTrue(Precision.equals(153.0, 153.0, 1));

        Assert.assertTrue(Precision.equals(153.0, 153.00000000000003, 1));
        Assert.assertFalse(Precision.equals(153.0, 153.00000000000006, 1));
        Assert.assertTrue(Precision.equals(153.0, 152.99999999999997, 1));
        Assert.assertFalse(Precision.equals(153, 152.99999999999994, 1));

        Assert.assertTrue(Precision.equals(-128.0, -127.99999999999999, 1));
        Assert.assertFalse(Precision.equals(-128.0, -127.99999999999997, 1));
        Assert.assertTrue(Precision.equals(-128.0, -128.00000000000003, 1));
        Assert.assertFalse(Precision.equals(-128.0, -128.00000000000006, 1));

        Assert.assertTrue(Precision.equals(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, 1));
        Assert.assertTrue(Precision.equals(Double.MAX_VALUE, Double.POSITIVE_INFINITY, 1));

        Assert.assertTrue(Precision.equals(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, 1));
        Assert.assertTrue(Precision.equals(-Double.MAX_VALUE, Double.NEGATIVE_INFINITY, 1));

        Assert.assertFalse(Precision.equals(Double.NaN, Double.NaN, 1));
        Assert.assertFalse(Precision.equals(Double.NaN, Double.NaN, 0));
        Assert.assertFalse(Precision.equals(Double.NaN, 0, 0));
        Assert.assertFalse(Precision.equals(Double.NaN, Double.POSITIVE_INFINITY, 0));
        Assert.assertFalse(Precision.equals(Double.NaN, Double.NEGATIVE_INFINITY, 0));

        Assert.assertFalse(Precision.equals(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 100000));
    }

    @Test
    public void testEqualsIncludingNaNWithAllowedUlps() {
        Assert.assertTrue(Precision.equalsIncludingNaN(0.0, -0.0, 1));

        Assert.assertTrue(Precision.equalsIncludingNaN(1.0, 1 + MathLib.ulp(1d), 1));
        Assert.assertFalse(Precision.equalsIncludingNaN(1.0, 1 + 2 * MathLib.ulp(1d), 1));

        final double nUp1 = MathLib.nextAfter(1d, Double.POSITIVE_INFINITY);
        final double nnUp1 = MathLib.nextAfter(nUp1, Double.POSITIVE_INFINITY);
        Assert.assertTrue(Precision.equalsIncludingNaN(1.0, nUp1, 1));
        Assert.assertTrue(Precision.equalsIncludingNaN(nUp1, nnUp1, 1));
        Assert.assertFalse(Precision.equalsIncludingNaN(1.0, nnUp1, 1));

        Assert.assertTrue(Precision.equalsIncludingNaN(0.0, MathLib.ulp(0d), 1));
        Assert.assertTrue(Precision.equalsIncludingNaN(0.0, -MathLib.ulp(0d), 1));

        Assert.assertTrue(Precision.equalsIncludingNaN(153.0, 153.0, 1));

        Assert.assertTrue(Precision.equalsIncludingNaN(153.0, 153.00000000000003, 1));
        Assert.assertFalse(Precision.equalsIncludingNaN(153.0, 153.00000000000006, 1));
        Assert.assertTrue(Precision.equalsIncludingNaN(153.0, 152.99999999999997, 1));
        Assert.assertFalse(Precision.equalsIncludingNaN(153, 152.99999999999994, 1));

        Assert.assertTrue(Precision.equalsIncludingNaN(-128.0, -127.99999999999999, 1));
        Assert.assertFalse(Precision.equalsIncludingNaN(-128.0, -127.99999999999997, 1));
        Assert.assertTrue(Precision.equalsIncludingNaN(-128.0, -128.00000000000003, 1));
        Assert.assertFalse(Precision.equalsIncludingNaN(-128.0, -128.00000000000006, 1));

        Assert.assertTrue(Precision.equalsIncludingNaN(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, 1));
        Assert.assertTrue(Precision.equalsIncludingNaN(Double.MAX_VALUE, Double.POSITIVE_INFINITY, 1));

        Assert.assertTrue(Precision.equalsIncludingNaN(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, 1));
        Assert.assertTrue(Precision.equalsIncludingNaN(-Double.MAX_VALUE, Double.NEGATIVE_INFINITY, 1));

        Assert.assertTrue(Precision.equalsIncludingNaN(Double.NaN, Double.NaN, 1));

        Assert.assertFalse(Precision.equalsIncludingNaN(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 100000));
    }

    @Test
    public void testCompareToEpsilon() {
        Assert.assertEquals(0, Precision.compareTo(152.33, 152.32, .011));
        Assert.assertTrue(Precision.compareTo(152.308, 152.32, .011) < 0);
        Assert.assertTrue(Precision.compareTo(152.33, 152.318, .011) > 0);
        Assert.assertEquals(0, Precision.compareTo(Double.MIN_VALUE, +0.0, Double.MIN_VALUE));
        Assert.assertEquals(0, Precision.compareTo(Double.MIN_VALUE, -0.0, Double.MIN_VALUE));
    }

    @Test
    public void testCompareToMaxUlps() {
        final double a = 152.32;
        final double delta = MathLib.ulp(a);
        for (int i = 0; i <= 10; ++i) {
            if (i <= 5) {
                Assert.assertEquals(0, Precision.compareTo(a, a + i * delta, 5));
                Assert.assertEquals(0, Precision.compareTo(a, a - i * delta, 5));
            } else {
                Assert.assertEquals(-1, Precision.compareTo(a, a + i * delta, 5));
                Assert.assertEquals(+1, Precision.compareTo(a, a - i * delta, 5));
            }
        }

        Assert.assertEquals(0, Precision.compareTo(-0.0, 0.0, 0));

        Assert.assertEquals(-1, Precision.compareTo(-Double.MIN_VALUE, -0.0, 0));
        Assert.assertEquals(0, Precision.compareTo(-Double.MIN_VALUE, -0.0, 1));
        Assert.assertEquals(-1, Precision.compareTo(-Double.MIN_VALUE, +0.0, 0));
        Assert.assertEquals(0, Precision.compareTo(-Double.MIN_VALUE, +0.0, 1));

        Assert.assertEquals(+1, Precision.compareTo(Double.MIN_VALUE, -0.0, 0));
        Assert.assertEquals(0, Precision.compareTo(Double.MIN_VALUE, -0.0, 1));
        Assert.assertEquals(+1, Precision.compareTo(Double.MIN_VALUE, +0.0, 0));
        Assert.assertEquals(0, Precision.compareTo(Double.MIN_VALUE, +0.0, 1));

        Assert.assertEquals(-1, Precision.compareTo(-Double.MIN_VALUE, Double.MIN_VALUE, 0));
        Assert.assertEquals(-1, Precision.compareTo(-Double.MIN_VALUE, Double.MIN_VALUE, 1));
        Assert.assertEquals(0, Precision.compareTo(-Double.MIN_VALUE, Double.MIN_VALUE, 2));

        Assert.assertEquals(0, Precision.compareTo(Double.MAX_VALUE, Double.POSITIVE_INFINITY, 1));
        Assert.assertEquals(-1, Precision.compareTo(Double.MAX_VALUE, Double.POSITIVE_INFINITY, 0));

        Assert.assertEquals(+1, Precision.compareTo(Double.MAX_VALUE, Double.NaN, Integer.MAX_VALUE));
        Assert.assertEquals(+1, Precision.compareTo(Double.NaN, Double.MAX_VALUE, Integer.MAX_VALUE));
    }

    @Test
    public void testRoundDouble() {
        final double x = 1.234567890;
        Assert.assertEquals(1.23, Precision.round(x, 2), 0.0);
        Assert.assertEquals(1.235, Precision.round(x, 3), 0.0);
        Assert.assertEquals(1.2346, Precision.round(x, 4), 0.0);

        // JIRA MATH-151
        Assert.assertEquals(39.25, Precision.round(39.245, 2), 0.0);
        Assert.assertEquals(39.24, Precision.round(39.245, 2, BigDecimal.ROUND_DOWN), 0.0);
        double xx = 39.0;
        xx = xx + 245d / 1000d;
        Assert.assertEquals(39.25, Precision.round(xx, 2), 0.0);

        // BZ 35904
        Assert.assertEquals(30.1d, Precision.round(30.095d, 2), 0.0d);
        Assert.assertEquals(30.1d, Precision.round(30.095d, 1), 0.0d);
        Assert.assertEquals(33.1d, Precision.round(33.095d, 1), 0.0d);
        Assert.assertEquals(33.1d, Precision.round(33.095d, 2), 0.0d);
        Assert.assertEquals(50.09d, Precision.round(50.085d, 2), 0.0d);
        Assert.assertEquals(50.19d, Precision.round(50.185d, 2), 0.0d);
        Assert.assertEquals(50.01d, Precision.round(50.005d, 2), 0.0d);
        Assert.assertEquals(30.01d, Precision.round(30.005d, 2), 0.0d);
        Assert.assertEquals(30.65d, Precision.round(30.645d, 2), 0.0d);

        Assert.assertEquals(1.24, Precision.round(x, 2, BigDecimal.ROUND_CEILING), 0.0);
        Assert.assertEquals(1.235, Precision.round(x, 3, BigDecimal.ROUND_CEILING), 0.0);
        Assert.assertEquals(1.2346, Precision.round(x, 4, BigDecimal.ROUND_CEILING), 0.0);
        Assert.assertEquals(-1.23, Precision.round(-x, 2, BigDecimal.ROUND_CEILING), 0.0);
        Assert.assertEquals(-1.234, Precision.round(-x, 3, BigDecimal.ROUND_CEILING), 0.0);
        Assert.assertEquals(-1.2345, Precision.round(-x, 4, BigDecimal.ROUND_CEILING), 0.0);

        Assert.assertEquals(1.23, Precision.round(x, 2, BigDecimal.ROUND_DOWN), 0.0);
        Assert.assertEquals(1.234, Precision.round(x, 3, BigDecimal.ROUND_DOWN), 0.0);
        Assert.assertEquals(1.2345, Precision.round(x, 4, BigDecimal.ROUND_DOWN), 0.0);
        Assert.assertEquals(-1.23, Precision.round(-x, 2, BigDecimal.ROUND_DOWN), 0.0);
        Assert.assertEquals(-1.234, Precision.round(-x, 3, BigDecimal.ROUND_DOWN), 0.0);
        Assert.assertEquals(-1.2345, Precision.round(-x, 4, BigDecimal.ROUND_DOWN), 0.0);

        Assert.assertEquals(1.23, Precision.round(x, 2, BigDecimal.ROUND_FLOOR), 0.0);
        Assert.assertEquals(1.234, Precision.round(x, 3, BigDecimal.ROUND_FLOOR), 0.0);
        Assert.assertEquals(1.2345, Precision.round(x, 4, BigDecimal.ROUND_FLOOR), 0.0);
        Assert.assertEquals(-1.24, Precision.round(-x, 2, BigDecimal.ROUND_FLOOR), 0.0);
        Assert.assertEquals(-1.235, Precision.round(-x, 3, BigDecimal.ROUND_FLOOR), 0.0);
        Assert.assertEquals(-1.2346, Precision.round(-x, 4, BigDecimal.ROUND_FLOOR), 0.0);

        Assert.assertEquals(1.23, Precision.round(x, 2, BigDecimal.ROUND_HALF_DOWN), 0.0);
        Assert.assertEquals(1.235, Precision.round(x, 3, BigDecimal.ROUND_HALF_DOWN), 0.0);
        Assert.assertEquals(1.2346, Precision.round(x, 4, BigDecimal.ROUND_HALF_DOWN), 0.0);
        Assert.assertEquals(-1.23, Precision.round(-x, 2, BigDecimal.ROUND_HALF_DOWN), 0.0);
        Assert.assertEquals(-1.235, Precision.round(-x, 3, BigDecimal.ROUND_HALF_DOWN), 0.0);
        Assert.assertEquals(-1.2346, Precision.round(-x, 4, BigDecimal.ROUND_HALF_DOWN), 0.0);
        Assert.assertEquals(1.234, Precision.round(1.2345, 3, BigDecimal.ROUND_HALF_DOWN), 0.0);
        Assert.assertEquals(-1.234, Precision.round(-1.2345, 3, BigDecimal.ROUND_HALF_DOWN), 0.0);

        Assert.assertEquals(1.23, Precision.round(x, 2, BigDecimal.ROUND_HALF_EVEN), 0.0);
        Assert.assertEquals(1.235, Precision.round(x, 3, BigDecimal.ROUND_HALF_EVEN), 0.0);
        Assert.assertEquals(1.2346, Precision.round(x, 4, BigDecimal.ROUND_HALF_EVEN), 0.0);
        Assert.assertEquals(-1.23, Precision.round(-x, 2, BigDecimal.ROUND_HALF_EVEN), 0.0);
        Assert.assertEquals(-1.235, Precision.round(-x, 3, BigDecimal.ROUND_HALF_EVEN), 0.0);
        Assert.assertEquals(-1.2346, Precision.round(-x, 4, BigDecimal.ROUND_HALF_EVEN), 0.0);
        Assert.assertEquals(1.234, Precision.round(1.2345, 3, BigDecimal.ROUND_HALF_EVEN), 0.0);
        Assert.assertEquals(-1.234, Precision.round(-1.2345, 3, BigDecimal.ROUND_HALF_EVEN), 0.0);
        Assert.assertEquals(1.236, Precision.round(1.2355, 3, BigDecimal.ROUND_HALF_EVEN), 0.0);
        Assert.assertEquals(-1.236, Precision.round(-1.2355, 3, BigDecimal.ROUND_HALF_EVEN), 0.0);

        Assert.assertEquals(1.23, Precision.round(x, 2, BigDecimal.ROUND_HALF_UP), 0.0);
        Assert.assertEquals(1.235, Precision.round(x, 3, BigDecimal.ROUND_HALF_UP), 0.0);
        Assert.assertEquals(1.2346, Precision.round(x, 4, BigDecimal.ROUND_HALF_UP), 0.0);
        Assert.assertEquals(-1.23, Precision.round(-x, 2, BigDecimal.ROUND_HALF_UP), 0.0);
        Assert.assertEquals(-1.235, Precision.round(-x, 3, BigDecimal.ROUND_HALF_UP), 0.0);
        Assert.assertEquals(-1.2346, Precision.round(-x, 4, BigDecimal.ROUND_HALF_UP), 0.0);
        Assert.assertEquals(1.235, Precision.round(1.2345, 3, BigDecimal.ROUND_HALF_UP), 0.0);
        Assert.assertEquals(-1.235, Precision.round(-1.2345, 3, BigDecimal.ROUND_HALF_UP), 0.0);

        Assert.assertEquals(-1.23, Precision.round(-1.23, 2, BigDecimal.ROUND_UNNECESSARY), 0.0);
        Assert.assertEquals(1.23, Precision.round(1.23, 2, BigDecimal.ROUND_UNNECESSARY), 0.0);

        try {
            Precision.round(1.234, 2, BigDecimal.ROUND_UNNECESSARY);
            Assert.fail();
        } catch (final ArithmeticException ex) {
            // expected
        }

        Assert.assertEquals(1.24, Precision.round(x, 2, BigDecimal.ROUND_UP), 0.0);
        Assert.assertEquals(1.235, Precision.round(x, 3, BigDecimal.ROUND_UP), 0.0);
        Assert.assertEquals(1.2346, Precision.round(x, 4, BigDecimal.ROUND_UP), 0.0);
        Assert.assertEquals(-1.24, Precision.round(-x, 2, BigDecimal.ROUND_UP), 0.0);
        Assert.assertEquals(-1.235, Precision.round(-x, 3, BigDecimal.ROUND_UP), 0.0);
        Assert.assertEquals(-1.2346, Precision.round(-x, 4, BigDecimal.ROUND_UP), 0.0);

        // MATH-151
        Assert.assertEquals(39.25, Precision.round(39.245, 2, BigDecimal.ROUND_HALF_UP), 0.0);

        // special values
        TestUtils.assertEquals(Double.NaN, Precision.round(1.234, 2, 1923), 0.0);
        TestUtils.assertEquals(Double.NaN, Precision.round(Double.NaN, 2), 0.0);
        Assert.assertEquals(0.0, Precision.round(0.0, 2), 0.0);
        Assert.assertEquals(Double.POSITIVE_INFINITY, Precision.round(Double.POSITIVE_INFINITY, 2), 0.0);
        Assert.assertEquals(Double.NEGATIVE_INFINITY, Precision.round(Double.NEGATIVE_INFINITY, 2), 0.0);
    }

    @Test
    public void testRoundFloat() {
        final float x = 1.234567890f;
        Assert.assertEquals(1.23f, Precision.round(x, 2), 0.0);
        Assert.assertEquals(1.235f, Precision.round(x, 3), 0.0);
        Assert.assertEquals(1.2346f, Precision.round(x, 4), 0.0);

        // BZ 35904
        Assert.assertEquals(30.1f, Precision.round(30.095f, 2), 0.0f);
        Assert.assertEquals(30.1f, Precision.round(30.095f, 1), 0.0f);
        Assert.assertEquals(50.09f, Precision.round(50.085f, 2), 0.0f);
        Assert.assertEquals(50.19f, Precision.round(50.185f, 2), 0.0f);
        Assert.assertEquals(50.01f, Precision.round(50.005f, 2), 0.0f);
        Assert.assertEquals(30.01f, Precision.round(30.005f, 2), 0.0f);
        Assert.assertEquals(30.65f, Precision.round(30.645f, 2), 0.0f);

        Assert.assertEquals(1.24f, Precision.round(x, 2, BigDecimal.ROUND_CEILING), 0.0);
        Assert.assertEquals(1.235f, Precision.round(x, 3, BigDecimal.ROUND_CEILING), 0.0);
        Assert.assertEquals(1.2346f, Precision.round(x, 4, BigDecimal.ROUND_CEILING), 0.0);
        Assert.assertEquals(-1.23f, Precision.round(-x, 2, BigDecimal.ROUND_CEILING), 0.0);
        Assert.assertEquals(-1.234f, Precision.round(-x, 3, BigDecimal.ROUND_CEILING), 0.0);
        Assert.assertEquals(-1.2345f, Precision.round(-x, 4, BigDecimal.ROUND_CEILING), 0.0);

        Assert.assertEquals(1.23f, Precision.round(x, 2, BigDecimal.ROUND_DOWN), 0.0);
        Assert.assertEquals(1.234f, Precision.round(x, 3, BigDecimal.ROUND_DOWN), 0.0);
        Assert.assertEquals(1.2345f, Precision.round(x, 4, BigDecimal.ROUND_DOWN), 0.0);
        Assert.assertEquals(-1.23f, Precision.round(-x, 2, BigDecimal.ROUND_DOWN), 0.0);
        Assert.assertEquals(-1.234f, Precision.round(-x, 3, BigDecimal.ROUND_DOWN), 0.0);
        Assert.assertEquals(-1.2345f, Precision.round(-x, 4, BigDecimal.ROUND_DOWN), 0.0);

        Assert.assertEquals(1.23f, Precision.round(x, 2, BigDecimal.ROUND_FLOOR), 0.0);
        Assert.assertEquals(1.234f, Precision.round(x, 3, BigDecimal.ROUND_FLOOR), 0.0);
        Assert.assertEquals(1.2345f, Precision.round(x, 4, BigDecimal.ROUND_FLOOR), 0.0);
        Assert.assertEquals(-1.24f, Precision.round(-x, 2, BigDecimal.ROUND_FLOOR), 0.0);
        Assert.assertEquals(-1.235f, Precision.round(-x, 3, BigDecimal.ROUND_FLOOR), 0.0);
        Assert.assertEquals(-1.2346f, Precision.round(-x, 4, BigDecimal.ROUND_FLOOR), 0.0);

        Assert.assertEquals(1.23f, Precision.round(x, 2, BigDecimal.ROUND_HALF_DOWN), 0.0);
        Assert.assertEquals(1.235f, Precision.round(x, 3, BigDecimal.ROUND_HALF_DOWN), 0.0);
        Assert.assertEquals(1.2346f, Precision.round(x, 4, BigDecimal.ROUND_HALF_DOWN), 0.0);
        Assert.assertEquals(-1.23f, Precision.round(-x, 2, BigDecimal.ROUND_HALF_DOWN), 0.0);
        Assert.assertEquals(-1.235f, Precision.round(-x, 3, BigDecimal.ROUND_HALF_DOWN), 0.0);
        Assert.assertEquals(-1.2346f, Precision.round(-x, 4, BigDecimal.ROUND_HALF_DOWN), 0.0);
        Assert.assertEquals(1.234f, Precision.round(1.2345f, 3, BigDecimal.ROUND_HALF_DOWN), 0.0);
        Assert.assertEquals(-1.234f, Precision.round(-1.2345f, 3, BigDecimal.ROUND_HALF_DOWN), 0.0);

        Assert.assertEquals(1.23f, Precision.round(x, 2, BigDecimal.ROUND_HALF_EVEN), 0.0);
        Assert.assertEquals(1.235f, Precision.round(x, 3, BigDecimal.ROUND_HALF_EVEN), 0.0);
        Assert.assertEquals(1.2346f, Precision.round(x, 4, BigDecimal.ROUND_HALF_EVEN), 0.0);
        Assert.assertEquals(-1.23f, Precision.round(-x, 2, BigDecimal.ROUND_HALF_EVEN), 0.0);
        Assert.assertEquals(-1.235f, Precision.round(-x, 3, BigDecimal.ROUND_HALF_EVEN), 0.0);
        Assert.assertEquals(-1.2346f, Precision.round(-x, 4, BigDecimal.ROUND_HALF_EVEN), 0.0);
        Assert.assertEquals(1.234f, Precision.round(1.2345f, 3, BigDecimal.ROUND_HALF_EVEN), 0.0);
        Assert.assertEquals(-1.234f, Precision.round(-1.2345f, 3, BigDecimal.ROUND_HALF_EVEN), 0.0);
        Assert.assertEquals(1.236f, Precision.round(1.2355f, 3, BigDecimal.ROUND_HALF_EVEN), 0.0);
        Assert.assertEquals(-1.236f, Precision.round(-1.2355f, 3, BigDecimal.ROUND_HALF_EVEN), 0.0);

        Assert.assertEquals(1.23f, Precision.round(x, 2, BigDecimal.ROUND_HALF_UP), 0.0);
        Assert.assertEquals(1.235f, Precision.round(x, 3, BigDecimal.ROUND_HALF_UP), 0.0);
        Assert.assertEquals(1.2346f, Precision.round(x, 4, BigDecimal.ROUND_HALF_UP), 0.0);
        Assert.assertEquals(-1.23f, Precision.round(-x, 2, BigDecimal.ROUND_HALF_UP), 0.0);
        Assert.assertEquals(-1.235f, Precision.round(-x, 3, BigDecimal.ROUND_HALF_UP), 0.0);
        Assert.assertEquals(-1.2346f, Precision.round(-x, 4, BigDecimal.ROUND_HALF_UP), 0.0);
        Assert.assertEquals(1.235f, Precision.round(1.2345f, 3, BigDecimal.ROUND_HALF_UP), 0.0);
        Assert.assertEquals(-1.235f, Precision.round(-1.2345f, 3, BigDecimal.ROUND_HALF_UP), 0.0);

        Assert.assertEquals(-1.23f, Precision.round(-1.23f, 2, BigDecimal.ROUND_UNNECESSARY), 0.0);
        Assert.assertEquals(1.23f, Precision.round(1.23f, 2, BigDecimal.ROUND_UNNECESSARY), 0.0);

        try {
            Precision.round(1.234f, 2, BigDecimal.ROUND_UNNECESSARY);
            Assert.fail();
        } catch (final MathArithmeticException ex) {
            // success
        }

        Assert.assertEquals(1.24f, Precision.round(x, 2, BigDecimal.ROUND_UP), 0.0);
        Assert.assertEquals(1.235f, Precision.round(x, 3, BigDecimal.ROUND_UP), 0.0);
        Assert.assertEquals(1.2346f, Precision.round(x, 4, BigDecimal.ROUND_UP), 0.0);
        Assert.assertEquals(-1.24f, Precision.round(-x, 2, BigDecimal.ROUND_UP), 0.0);
        Assert.assertEquals(-1.235f, Precision.round(-x, 3, BigDecimal.ROUND_UP), 0.0);
        Assert.assertEquals(-1.2346f, Precision.round(-x, 4, BigDecimal.ROUND_UP), 0.0);

        try {
            Precision.round(1.234f, 2, 1923);
            Assert.fail();
        } catch (final MathIllegalArgumentException ex) {
            // success
        }

        // special values
        Float resRound;
        try {
            resRound = Precision.round(Float.NaN, 2);
        } catch (final ArithmeticException e) {
            resRound = Float.NaN;
        }

        TestUtils.assertEquals(Float.NaN, resRound, 0.0f);
        Assert.assertEquals(0.0f, Precision.round(0.0f, 2), 0.0f);
        Assert.assertEquals(Float.POSITIVE_INFINITY, Precision.round(Float.POSITIVE_INFINITY, 2), 0.0f);
        Assert.assertEquals(Float.NEGATIVE_INFINITY, Precision.round(Float.NEGATIVE_INFINITY, 2), 0.0f);
    }

    @Test
    public void testIssue721() {
        Assert.assertEquals(-53, MathLib.getExponent(Precision.EPSILON));
        Assert.assertEquals(-1022, MathLib.getExponent(Precision.SAFE_MIN));
    }

    @Test
    public void testRepresentableDelta() {
        int nonRepresentableCount = 0;
        final double x = 100;
        final int numTrials = 10000;
        for (int i = 0; i < numTrials; i++) {
            final double originalDelta = Math.random();
            final double delta = Precision.representableDelta(x, originalDelta);
            if (delta != originalDelta) {
                ++nonRepresentableCount;
            }
        }

        Assert.assertTrue(nonRepresentableCount / (double) numTrials > 0.9);
    }

    @Test
    public void testMath843() {
        final double afterEpsilon = MathLib.nextAfter(Precision.EPSILON,
            Double.POSITIVE_INFINITY);

        // a) 1 + EPSILON is equal to 1.
        Assert.assertTrue(1 + Precision.EPSILON == 1);

        // b) 1 + "the number after EPSILON" is not equal to 1.
        Assert.assertFalse(1 + afterEpsilon == 1);
    }
}
