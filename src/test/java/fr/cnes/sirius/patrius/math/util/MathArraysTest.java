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
package fr.cnes.sirius.patrius.math.util;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.MathArithmeticException;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.exception.NonMonotonicSequenceException;
import fr.cnes.sirius.patrius.math.exception.NotPositiveException;
import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.random.Well1024a;

/**
 * Test cases for the {@link MathArrays} class.
 * 
 * @version $Id: MathArraysTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class MathArraysTest {
    @Test(expected = DimensionMismatchException.class)
    public void testEbeAddPrecondition() {
        MathArrays.ebeAdd(new double[3], new double[4]);
    }

    @Test(expected = DimensionMismatchException.class)
    public void testEbeSubtractPrecondition() {
        MathArrays.ebeSubtract(new double[3], new double[4]);
    }

    @Test(expected = DimensionMismatchException.class)
    public void testEbeMultiplyPrecondition() {
        MathArrays.ebeMultiply(new double[3], new double[4]);
    }

    @Test(expected = DimensionMismatchException.class)
    public void testEbeDividePrecondition() {
        MathArrays.ebeDivide(new double[3], new double[4]);
    }

    @Test
    public void testEbeAdd() {
        final double[] a = { 0, 1, 2 };
        final double[] b = { 3, 5, 7 };
        final double[] r = MathArrays.ebeAdd(a, b);

        for (int i = 0; i < a.length; i++) {
            Assert.assertEquals(a[i] + b[i], r[i], 0);
        }
    }

    @Test
    public void testEbeSubtract() {
        final double[] a = { 0, 1, 2 };
        final double[] b = { 3, 5, 7 };
        final double[] r = MathArrays.ebeSubtract(a, b);

        for (int i = 0; i < a.length; i++) {
            Assert.assertEquals(a[i] - b[i], r[i], 0);
        }
    }

    @Test
    public void testEbeMultiply() {
        final double[] a = { 0, 1, 2 };
        final double[] b = { 3, 5, 7 };
        final double[] r = MathArrays.ebeMultiply(a, b);

        for (int i = 0; i < a.length; i++) {
            Assert.assertEquals(a[i] * b[i], r[i], 0);
        }
    }

    @Test
    public void testEbeDivide() {
        final double[] a = { 0, 1, 2 };
        final double[] b = { 3, 5, 7 };
        final double[] r = MathArrays.ebeDivide(a, b);

        for (int i = 0; i < a.length; i++) {
            Assert.assertEquals(a[i] / b[i], r[i], 0);
        }
    }

    @Test
    public void testL1DistanceDouble() {
        final double[] p1 = { 2.5, 0.0 };
        final double[] p2 = { -0.5, 4.0 };
        Assert.assertTrue(Precision.equals(7.0, MathArrays.distance1(p1, p2), 1));
    }

    @Test
    public void testL1DistanceInt() {
        final int[] p1 = { 3, 0 };
        final int[] p2 = { 0, 4 };
        Assert.assertEquals(7, MathArrays.distance1(p1, p2));
    }

    @Test
    public void testL2DistanceDouble() {
        final double[] p1 = { 2.5, 0.0 };
        final double[] p2 = { -0.5, 4.0 };
        Assert.assertTrue(Precision.equals(5.0, MathArrays.distance(p1, p2), 1));
    }

    @Test
    public void testL2DistanceInt() {
        final int[] p1 = { 3, 0 };
        final int[] p2 = { 0, 4 };
        Assert.assertTrue(Precision.equals(5, MathArrays.distance(p1, p2), 1));
    }

    @Test
    public void testLInfDistanceDouble() {
        final double[] p1 = { 2.5, 0.0 };
        final double[] p2 = { -0.5, 4.0 };
        Assert.assertTrue(Precision.equals(4.0, MathArrays.distanceInf(p1, p2), 1));
    }

    @Test
    public void testLInfDistanceInt() {
        final int[] p1 = { 3, 0 };
        final int[] p2 = { 0, 4 };
        Assert.assertEquals(4, MathArrays.distanceInf(p1, p2));
    }

    @Test
    public void testCheckOrder() {
        MathArrays.checkOrder(new double[] { -15, -5.5, -1, 2, 15 },
            MathArrays.OrderDirection.INCREASING, true);
        MathArrays.checkOrder(new double[] { -15, -5.5, -1, 2, 2 },
            MathArrays.OrderDirection.INCREASING, false);
        MathArrays.checkOrder(new double[] { 3, -5.5, -11, -27.5 },
            MathArrays.OrderDirection.DECREASING, true);
        MathArrays.checkOrder(new double[] { 3, 0, 0, -5.5, -11, -27.5 },
            MathArrays.OrderDirection.DECREASING, false);

        try {
            MathArrays.checkOrder(new double[] { -15, -5.5, -1, -1, 2, 15 },
                MathArrays.OrderDirection.INCREASING, true);
            Assert.fail("an exception should have been thrown");
        } catch (final NonMonotonicSequenceException e) {
            // Expected
        }
        try {
            MathArrays.checkOrder(new double[] { -15, -5.5, -1, -2, 2 },
                MathArrays.OrderDirection.INCREASING, false);
            Assert.fail("an exception should have been thrown");
        } catch (final NonMonotonicSequenceException e) {
            // Expected
        }
        try {
            MathArrays.checkOrder(new double[] { 3, 3, -5.5, -11, -27.5 },
                MathArrays.OrderDirection.DECREASING, true);
            Assert.fail("an exception should have been thrown");
        } catch (final NonMonotonicSequenceException e) {
            // Expected
        }
        try {
            MathArrays.checkOrder(new double[] { 3, -1, 0, -5.5, -11, -27.5 },
                MathArrays.OrderDirection.DECREASING, false);
            Assert.fail("an exception should have been thrown");
        } catch (final NonMonotonicSequenceException e) {
            // Expected
        }
        try {
            MathArrays.checkOrder(new double[] { 3, 0, -5.5, -11, -10 },
                MathArrays.OrderDirection.DECREASING, false);
            Assert.fail("an exception should have been thrown");
        } catch (final NonMonotonicSequenceException e) {
            // Expected
        }
    }

    @Test
    public void testIsMonotonic() {
        Assert.assertFalse(MathArrays.isMonotonic(new double[] { -15, -5.5, -1, -1, 2, 15 },
            MathArrays.OrderDirection.INCREASING, true));
        Assert.assertTrue(MathArrays.isMonotonic(new double[] { -15, -5.5, -1, 0, 2, 15 },
            MathArrays.OrderDirection.INCREASING, true));
        Assert.assertFalse(MathArrays.isMonotonic(new double[] { -15, -5.5, -1, -2, 2 },
            MathArrays.OrderDirection.INCREASING, false));
        Assert.assertTrue(MathArrays.isMonotonic(new double[] { -15, -5.5, -1, -1, 2 },
            MathArrays.OrderDirection.INCREASING, false));
        Assert.assertFalse(MathArrays.isMonotonic(new double[] { 3, 3, -5.5, -11, -27.5 },
            MathArrays.OrderDirection.DECREASING, true));
        Assert.assertTrue(MathArrays.isMonotonic(new double[] { 3, 2, -5.5, -11, -27.5 },
            MathArrays.OrderDirection.DECREASING, true));
        Assert.assertFalse(MathArrays.isMonotonic(new double[] { 3, -1, 0, -5.5, -11, -27.5 },
            MathArrays.OrderDirection.DECREASING, false));
        Assert.assertTrue(MathArrays.isMonotonic(new double[] { 3, 0, 0, -5.5, -11, -27.5 },
            MathArrays.OrderDirection.DECREASING, false));
    }

    @Test
    public void testIsMonotonicComparable() {
        Assert.assertFalse(MathArrays.isMonotonic(new Double[] { new Double(-15),
            new Double(-5.5),
            new Double(-1),
            new Double(-1),
            new Double(2),
            new Double(15) },
            MathArrays.OrderDirection.INCREASING, true));
        Assert.assertTrue(MathArrays.isMonotonic(new Double[] { new Double(-15),
            new Double(-5.5),
            new Double(-1),
            new Double(0),
            new Double(2),
            new Double(15) },
            MathArrays.OrderDirection.INCREASING, true));
        Assert.assertFalse(MathArrays.isMonotonic(new Double[] { new Double(-15),
            new Double(-5.5),
            new Double(-1),
            new Double(-2),
            new Double(2) },
            MathArrays.OrderDirection.INCREASING, false));
        Assert.assertTrue(MathArrays.isMonotonic(new Double[] { new Double(-15),
            new Double(-5.5),
            new Double(-1),
            new Double(-1),
            new Double(2) },
            MathArrays.OrderDirection.INCREASING, false));
        Assert.assertFalse(MathArrays.isMonotonic(new Double[] { new Double(3),
            new Double(3),
            new Double(-5.5),
            new Double(-11),
            new Double(-27.5) },
            MathArrays.OrderDirection.DECREASING, true));
        Assert.assertTrue(MathArrays.isMonotonic(new Double[] { new Double(3),
            new Double(2),
            new Double(-5.5),
            new Double(-11),
            new Double(-27.5) },
            MathArrays.OrderDirection.DECREASING, true));
        Assert.assertFalse(MathArrays.isMonotonic(new Double[] { new Double(3),
            new Double(-1),
            new Double(0),
            new Double(-5.5),
            new Double(-11),
            new Double(-27.5) },
            MathArrays.OrderDirection.DECREASING, false));
        Assert.assertTrue(MathArrays.isMonotonic(new Double[] { new Double(3),
            new Double(0),
            new Double(0),
            new Double(-5.5),
            new Double(-11),
            new Double(-27.5) },
            MathArrays.OrderDirection.DECREASING, false));
    }

    @Test
    public void testCheckRectangular() {
        final long[][] rect = new long[][] { { 0, 1 }, { 2, 3 } };
        final long[][] ragged = new long[][] { { 0, 1 }, { 2 } };
        final long[][] nullArray = null;
        final long[][] empty = new long[][] {};
        MathArrays.checkRectangular(rect);
        MathArrays.checkRectangular(empty);
        try {
            MathArrays.checkRectangular(ragged);
            Assert.fail("Expecting DimensionMismatchException");
        } catch (final DimensionMismatchException ex) {
            // Expected
        }
        try {
            MathArrays.checkRectangular(nullArray);
            Assert.fail("Expecting NullArgumentException");
        } catch (final NullArgumentException ex) {
            // Expected
        }
    }

    @Test
    public void testCheckPositive() {
        final double[] positive = new double[] { 1, 2, 3 };
        final double[] nonNegative = new double[] { 0, 1, 2 };
        final double[] nullArray = null;
        final double[] empty = new double[] {};
        MathArrays.checkPositive(positive);
        MathArrays.checkPositive(empty);
        try {
            MathArrays.checkPositive(nullArray);
            Assert.fail("Expecting NullPointerException");
        } catch (final NullPointerException ex) {
            // Expected
        }
        try {
            MathArrays.checkPositive(nonNegative);
            Assert.fail("Expecting NotStrictlyPositiveException");
        } catch (final NotStrictlyPositiveException ex) {
            // Expected
        }
    }

    @Test
    public void testCheckNonNegative() {
        final long[] nonNegative = new long[] { 0, 1 };
        final long[] hasNegative = new long[] { -1 };
        final long[] nullArray = null;
        final long[] empty = new long[] {};
        MathArrays.checkNonNegative(nonNegative);
        MathArrays.checkNonNegative(empty);
        try {
            MathArrays.checkNonNegative(nullArray);
            Assert.fail("Expecting NullPointerException");
        } catch (final NullPointerException ex) {
            // Expected
        }
        try {
            MathArrays.checkNonNegative(hasNegative);
            Assert.fail("Expecting NotPositiveException");
        } catch (final NotPositiveException ex) {
            // Expected
        }
    }

    @Test
    public void testCheckNonNegative2D() {
        final long[][] nonNegative = new long[][] { { 0, 1 }, { 1, 0 } };
        final long[][] hasNegative = new long[][] { { -1 }, { 0 } };
        final long[][] nullArray = null;
        final long[][] empty = new long[][] {};
        MathArrays.checkNonNegative(nonNegative);
        MathArrays.checkNonNegative(empty);
        try {
            MathArrays.checkNonNegative(nullArray);
            Assert.fail("Expecting NullPointerException");
        } catch (final NullPointerException ex) {
            // Expected
        }
        try {
            MathArrays.checkNonNegative(hasNegative);
            Assert.fail("Expecting NotPositiveException");
        } catch (final NotPositiveException ex) {
            // Expected
        }
    }

    @Test
    public void testSortInPlace() {
        final double[] x1 = { 2, 5, -3, 1, 4 };
        final double[] x2 = { 4, 25, 9, 1, 16 };
        final double[] x3 = { 8, 125, -27, 1, 64 };

        MathArrays.sortInPlace(x1, x2, x3);

        Assert.assertEquals(-3, x1[0], Math.ulp(1d));
        Assert.assertEquals(9, x2[0], Math.ulp(1d));
        Assert.assertEquals(-27, x3[0], Math.ulp(1d));

        Assert.assertEquals(1, x1[1], Math.ulp(1d));
        Assert.assertEquals(1, x2[1], Math.ulp(1d));
        Assert.assertEquals(1, x3[1], Math.ulp(1d));

        Assert.assertEquals(2, x1[2], Math.ulp(1d));
        Assert.assertEquals(4, x2[2], Math.ulp(1d));
        Assert.assertEquals(8, x3[2], Math.ulp(1d));

        Assert.assertEquals(4, x1[3], Math.ulp(1d));
        Assert.assertEquals(16, x2[3], Math.ulp(1d));
        Assert.assertEquals(64, x3[3], Math.ulp(1d));

        Assert.assertEquals(5, x1[4], Math.ulp(1d));
        Assert.assertEquals(25, x2[4], Math.ulp(1d));
        Assert.assertEquals(125, x3[4], Math.ulp(1d));
    }

    @Test
    /** Example in javadoc */
    public void testSortInPlaceExample() {
        final double[] x = { 3, 1, 2 };
        final double[] y = { 1, 2, 3 };
        final double[] z = { 0, 5, 7 };
        MathArrays.sortInPlace(x, y, z);
        final double[] sx = { 1, 2, 3 };
        final double[] sy = { 2, 3, 1 };
        final double[] sz = { 5, 7, 0 };
        Assert.assertTrue(Arrays.equals(sx, x));
        Assert.assertTrue(Arrays.equals(sy, y));
        Assert.assertTrue(Arrays.equals(sz, z));
    }

    @Test
    public void testSortInPlaceFailures() {
        final double[] nullArray = null;
        final double[] one = { 1 };
        final double[] two = { 1, 2 };
        final double[] onep = { 2 };
        try {
            MathArrays.sortInPlace(one, two);
            Assert.fail("Expecting DimensionMismatchException");
        } catch (final DimensionMismatchException ex) {
            // expected
        }
        try {
            MathArrays.sortInPlace(one, nullArray);
            Assert.fail("Expecting NullArgumentException");
        } catch (final NullArgumentException ex) {
            // expected
        }
        try {
            MathArrays.sortInPlace(one, onep, nullArray);
            Assert.fail("Expecting NullArgumentException");
        } catch (final NullArgumentException ex) {
            // expected
        }
    }

    @Test
    public void testCopyOfInt() {
        final int[] source = { Integer.MIN_VALUE,
            -1, 0, 1, 3, 113, 4769,
            Integer.MAX_VALUE };
        final int[] dest = MathArrays.copyOf(source);

        Assert.assertEquals(dest.length, source.length);
        for (int i = 0; i < source.length; i++) {
            Assert.assertEquals(source[i], dest[i]);
        }
    }

    @Test
    public void testCopyOfInt2() {
        final int[] source = { Integer.MIN_VALUE,
            -1, 0, 1, 3, 113, 4769,
            Integer.MAX_VALUE };
        final int offset = 3;
        final int[] dest = MathArrays.copyOf(source, source.length - offset);

        Assert.assertEquals(dest.length, source.length - offset);
        for (int i = 0; i < source.length - offset; i++) {
            Assert.assertEquals(source[i], dest[i]);
        }
    }

    @Test
    public void testCopyOfInt3() {
        final int[] source = { Integer.MIN_VALUE,
            -1, 0, 1, 3, 113, 4769,
            Integer.MAX_VALUE };
        final int offset = 3;
        final int[] dest = MathArrays.copyOf(source, source.length + offset);

        Assert.assertEquals(dest.length, source.length + offset);
        for (int i = 0; i < source.length; i++) {
            Assert.assertEquals(source[i], dest[i]);
        }
        for (int i = source.length; i < source.length + offset; i++) {
            Assert.assertEquals(0, dest[i], 0);
        }
    }

    @Test
    public void testCopyOfDouble() {
        final double[] source = { Double.NEGATIVE_INFINITY,
            -Double.MAX_VALUE,
            -1, 0,
            Double.MIN_VALUE,
            Math.ulp(1d),
            1, 3, 113, 4769,
            Double.MAX_VALUE,
            Double.POSITIVE_INFINITY };
        final double[] dest = MathArrays.copyOf(source);

        Assert.assertEquals(dest.length, source.length);
        for (int i = 0; i < source.length; i++) {
            Assert.assertEquals(source[i], dest[i], 0);
        }
    }

    @Test
    public void testCopyOfDouble2() {
        final double[] source = { Double.NEGATIVE_INFINITY,
            -Double.MAX_VALUE,
            -1, 0,
            Double.MIN_VALUE,
            Math.ulp(1d),
            1, 3, 113, 4769,
            Double.MAX_VALUE,
            Double.POSITIVE_INFINITY };
        final int offset = 3;
        final double[] dest = MathArrays.copyOf(source, source.length - offset);

        Assert.assertEquals(dest.length, source.length - offset);
        for (int i = 0; i < source.length - offset; i++) {
            Assert.assertEquals(source[i], dest[i], 0);
        }
    }

    @Test
    public void testCopyOfDouble3() {
        final double[] source = { Double.NEGATIVE_INFINITY,
            -Double.MAX_VALUE,
            -1, 0,
            Double.MIN_VALUE,
            Math.ulp(1d),
            1, 3, 113, 4769,
            Double.MAX_VALUE,
            Double.POSITIVE_INFINITY };
        final int offset = 3;
        final double[] dest = MathArrays.copyOf(source, source.length + offset);

        Assert.assertEquals(dest.length, source.length + offset);
        for (int i = 0; i < source.length; i++) {
            Assert.assertEquals(source[i], dest[i], 0);
        }
        for (int i = source.length; i < source.length + offset; i++) {
            Assert.assertEquals(0, dest[i], 0);
        }
    }

    @Test
    public void testLinearCombination1() {
        final double[] a = new double[] {
            -1321008684645961.0 / 268435456.0,
            -5774608829631843.0 / 268435456.0,
            -7645843051051357.0 / 8589934592.0
        };
        final double[] b = new double[] {
            -5712344449280879.0 / 2097152.0,
            -4550117129121957.0 / 2097152.0,
            8846951984510141.0 / 131072.0
        };

        final double abSumInline = MathArrays.linearCombination(a[0], b[0],
            a[1], b[1],
            a[2], b[2]);
        final double abSumArray = MathArrays.linearCombination(a, b);

        Assert.assertEquals(abSumInline, abSumArray, 0);
    }

    @Test
    public void testLinearCombination2() {
        // we compare accurate versus naive dot product implementations
        // on regular vectors (i.e. not extreme cases like in the previous test)
        final Well1024a random = new Well1024a(553267312521321234l);

        for (int i = 0; i < 10000; ++i) {
            final double ux = 1e17 * random.nextDouble();
            final double uy = 1e17 * random.nextDouble();
            final double uz = 1e17 * random.nextDouble();
            final double vx = 1e17 * random.nextDouble();
            final double vy = 1e17 * random.nextDouble();
            final double vz = 1e17 * random.nextDouble();
            final double sInline = MathArrays.linearCombination(ux, vx,
                uy, vy,
                uz, vz);
            final double sArray = MathArrays.linearCombination(new double[] { ux, uy, uz },
                new double[] { vx, vy, vz });
            Assert.assertEquals(sInline, sArray, 0);
        }
    }

    @Test
    public void testLinearCombinationInfinite() {
        final double[][] a = new double[][] {
            { 1, 2, 3, 4 },
            { 1, Double.POSITIVE_INFINITY, 3, 4 },
            { 1, 2, Double.POSITIVE_INFINITY, 4 },
            { 1, Double.POSITIVE_INFINITY, 3, Double.NEGATIVE_INFINITY },
            { 1, 2, 3, 4 },
            { 1, 2, 3, 4 },
            { 1, 2, 3, 4 },
            { 1, 2, 3, 4 }
        };
        final double[][] b = new double[][] {
            { 1, -2, 3, 4 },
            { 1, -2, 3, 4 },
            { 1, -2, 3, 4 },
            { 1, -2, 3, 4 },
            { 1, Double.POSITIVE_INFINITY, 3, 4 },
            { 1, -2, Double.POSITIVE_INFINITY, 4 },
            { 1, Double.POSITIVE_INFINITY, 3, Double.NEGATIVE_INFINITY },
            { Double.NaN, -2, 3, 4 }
        };

        Assert.assertEquals(-3,
            MathArrays.linearCombination(a[0][0], b[0][0],
                a[0][1], b[0][1]),
            1.0e-10);
        Assert.assertEquals(6,
            MathArrays.linearCombination(a[0][0], b[0][0],
                a[0][1], b[0][1],
                a[0][2], b[0][2]),
            1.0e-10);
        Assert.assertEquals(22,
            MathArrays.linearCombination(a[0][0], b[0][0],
                a[0][1], b[0][1],
                a[0][2], b[0][2],
                a[0][3], b[0][3]),
            1.0e-10);
        Assert.assertEquals(22, MathArrays.linearCombination(a[0], b[0]), 1.0e-10);

        Assert.assertEquals(Double.NEGATIVE_INFINITY,
            MathArrays.linearCombination(a[1][0], b[1][0],
                a[1][1], b[1][1]),
            1.0e-10);
        Assert.assertEquals(Double.NEGATIVE_INFINITY,
            MathArrays.linearCombination(a[1][0], b[1][0],
                a[1][1], b[1][1],
                a[1][2], b[1][2]),
            1.0e-10);
        Assert.assertEquals(Double.NEGATIVE_INFINITY,
            MathArrays.linearCombination(a[1][0], b[1][0],
                a[1][1], b[1][1],
                a[1][2], b[1][2],
                a[1][3], b[1][3]),
            1.0e-10);
        Assert.assertEquals(Double.NEGATIVE_INFINITY, MathArrays.linearCombination(a[1], b[1]), 1.0e-10);

        Assert.assertEquals(-3,
            MathArrays.linearCombination(a[2][0], b[2][0],
                a[2][1], b[2][1]),
            1.0e-10);
        Assert.assertEquals(Double.POSITIVE_INFINITY,
            MathArrays.linearCombination(a[2][0], b[2][0],
                a[2][1], b[2][1],
                a[2][2], b[2][2]),
            1.0e-10);
        Assert.assertEquals(Double.POSITIVE_INFINITY,
            MathArrays.linearCombination(a[2][0], b[2][0],
                a[2][1], b[2][1],
                a[2][2], b[2][2],
                a[2][3], b[2][3]),
            1.0e-10);
        Assert.assertEquals(Double.POSITIVE_INFINITY, MathArrays.linearCombination(a[2], b[2]), 1.0e-10);

        Assert.assertEquals(Double.NEGATIVE_INFINITY,
            MathArrays.linearCombination(a[3][0], b[3][0],
                a[3][1], b[3][1]),
            1.0e-10);
        Assert.assertEquals(Double.NEGATIVE_INFINITY,
            MathArrays.linearCombination(a[3][0], b[3][0],
                a[3][1], b[3][1],
                a[3][2], b[3][2]),
            1.0e-10);
        Assert.assertEquals(Double.NEGATIVE_INFINITY,
            MathArrays.linearCombination(a[3][0], b[3][0],
                a[3][1], b[3][1],
                a[3][2], b[3][2],
                a[3][3], b[3][3]),
            1.0e-10);
        Assert.assertEquals(Double.NEGATIVE_INFINITY, MathArrays.linearCombination(a[3], b[3]), 1.0e-10);

        Assert.assertEquals(Double.POSITIVE_INFINITY,
            MathArrays.linearCombination(a[4][0], b[4][0],
                a[4][1], b[4][1]),
            1.0e-10);
        Assert.assertEquals(Double.POSITIVE_INFINITY,
            MathArrays.linearCombination(a[4][0], b[4][0],
                a[4][1], b[4][1],
                a[4][2], b[4][2]),
            1.0e-10);
        Assert.assertEquals(Double.POSITIVE_INFINITY,
            MathArrays.linearCombination(a[4][0], b[4][0],
                a[4][1], b[4][1],
                a[4][2], b[4][2],
                a[4][3], b[4][3]),
            1.0e-10);
        Assert.assertEquals(Double.POSITIVE_INFINITY, MathArrays.linearCombination(a[4], b[4]), 1.0e-10);

        Assert.assertEquals(-3,
            MathArrays.linearCombination(a[5][0], b[5][0],
                a[5][1], b[5][1]),
            1.0e-10);
        Assert.assertEquals(Double.POSITIVE_INFINITY,
            MathArrays.linearCombination(a[5][0], b[5][0],
                a[5][1], b[5][1],
                a[5][2], b[5][2]),
            1.0e-10);
        Assert.assertEquals(Double.POSITIVE_INFINITY,
            MathArrays.linearCombination(a[5][0], b[5][0],
                a[5][1], b[5][1],
                a[5][2], b[5][2],
                a[5][3], b[5][3]),
            1.0e-10);
        Assert.assertEquals(Double.POSITIVE_INFINITY, MathArrays.linearCombination(a[5], b[5]), 1.0e-10);

        Assert.assertEquals(Double.POSITIVE_INFINITY,
            MathArrays.linearCombination(a[6][0], b[6][0],
                a[6][1], b[6][1]),
            1.0e-10);
        Assert.assertEquals(Double.POSITIVE_INFINITY,
            MathArrays.linearCombination(a[6][0], b[6][0],
                a[6][1], b[6][1],
                a[6][2], b[6][2]),
            1.0e-10);
        Assert.assertTrue(Double.isNaN(MathArrays.linearCombination(a[6][0], b[6][0],
            a[6][1], b[6][1],
            a[6][2], b[6][2],
            a[6][3], b[6][3])));
        Assert.assertTrue(Double.isNaN(MathArrays.linearCombination(a[6], b[6])));

        Assert.assertTrue(Double.isNaN(MathArrays.linearCombination(a[7][0], b[7][0],
            a[7][1], b[7][1])));
        Assert.assertTrue(Double.isNaN(MathArrays.linearCombination(a[7][0], b[7][0],
            a[7][1], b[7][1],
            a[7][2], b[7][2])));
        Assert.assertTrue(Double.isNaN(MathArrays.linearCombination(a[7][0], b[7][0],
            a[7][1], b[7][1],
            a[7][2], b[7][2],
            a[7][3], b[7][3])));
        Assert.assertTrue(Double.isNaN(MathArrays.linearCombination(a[7], b[7])));
    }

    @Test
    public void testArrayEquals() {
        Assert.assertFalse(MathArrays.equals(new double[] { 1d }, null));
        Assert.assertFalse(MathArrays.equals(null, new double[] { 1d }));
        Assert.assertTrue(MathArrays.equals((double[]) null, (double[]) null));

        Assert.assertFalse(MathArrays.equals(new double[] { 1d }, new double[0]));
        Assert.assertTrue(MathArrays.equals(new double[] { 1d }, new double[] { 1d }));
        Assert.assertTrue(MathArrays.equals(new double[] { Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY, 1d, 0d },
            new double[] { Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY, 1d, 0d }));
        Assert.assertFalse(MathArrays.equals(new double[] { Double.NaN },
            new double[] { Double.NaN }));
        Assert.assertFalse(MathArrays.equals(new double[] { Double.POSITIVE_INFINITY },
            new double[] { Double.NEGATIVE_INFINITY }));
        Assert.assertFalse(MathArrays.equals(new double[] { 1d },
            new double[] { MathLib.nextAfter(MathLib.nextAfter(1d, 2d), 2d) }));

    }

    @Test
    public void testArrayEqualsIncludingNaN() {
        Assert.assertFalse(MathArrays.equalsIncludingNaN(new double[] { 1d }, null));
        Assert.assertFalse(MathArrays.equalsIncludingNaN(null, new double[] { 1d }));
        Assert.assertTrue(MathArrays.equalsIncludingNaN((double[]) null, (double[]) null));

        Assert.assertFalse(MathArrays.equalsIncludingNaN(new double[] { 1d }, new double[0]));
        Assert.assertTrue(MathArrays.equalsIncludingNaN(new double[] { 1d }, new double[] { 1d }));
        Assert.assertTrue(MathArrays.equalsIncludingNaN(new double[] { Double.NaN, Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY, 1d, 0d },
            new double[] { Double.NaN, Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY, 1d, 0d }));
        Assert.assertFalse(MathArrays.equalsIncludingNaN(new double[] { Double.POSITIVE_INFINITY },
            new double[] { Double.NEGATIVE_INFINITY }));
        Assert.assertFalse(MathArrays.equalsIncludingNaN(new double[] { 1d },
            new double[] { MathLib.nextAfter(MathLib.nextAfter(1d, 2d), 2d) }));
    }

    @Test
    public void testNormalizeArray() {
        final double[] testValues1 = new double[] { 1, 1, 2 };
        TestUtils.assertEquals(new double[] { .25, .25, .5 },
            MathArrays.normalizeArray(testValues1, 1),
            Double.MIN_VALUE);

        TestUtils.assertEquals(MathLib.sqrt(6),
            MathArrays.safeNorm(testValues1),
            Double.MIN_VALUE);

        final double[] testValues2 = new double[] { -1, -1, 1 };
        TestUtils.assertEquals(new double[] { 1, 1, -1 },
            MathArrays.normalizeArray(testValues2, 1),
            Double.MIN_VALUE);

        TestUtils.assertEquals(MathLib.sqrt(3),
            MathArrays.safeNorm(testValues2),
            Double.MIN_VALUE);

        // Ignore NaNs
        final double[] testValues3 = new double[] { -1, -1, Double.NaN, 1, Double.NaN };
        TestUtils.assertEquals(new double[] { 1, 1, Double.NaN, -1, Double.NaN },
            MathArrays.normalizeArray(testValues3, 1),
            Double.MIN_VALUE);

        // Zero sum -> MathArithmeticException
        final double[] zeroSum = new double[] { -1, 1 };
        try {
            MathArrays.normalizeArray(zeroSum, 1);
            Assert.fail("expecting MathArithmeticException");
        } catch (final MathArithmeticException ex) {
        }

        // Infinite elements -> MathArithmeticException
        final double[] hasInf = new double[] { 1, 2, 1, Double.NEGATIVE_INFINITY };
        try {
            MathArrays.normalizeArray(hasInf, 1);
            Assert.fail("expecting MathIllegalArgumentException");
        } catch (final MathIllegalArgumentException ex) {
        }

        // Infinite target -> MathIllegalArgumentException
        try {
            MathArrays.normalizeArray(testValues1, Double.POSITIVE_INFINITY);
            Assert.fail("expecting MathIllegalArgumentException");
        } catch (final MathIllegalArgumentException ex) {
        }

        // NaN target -> MathIllegalArgumentException
        try {
            MathArrays.normalizeArray(testValues1, Double.NaN);
            Assert.fail("expecting MathIllegalArgumentException");
        } catch (final MathIllegalArgumentException ex) {
        }
    }

    @Test
    public void testEquals() {
        float[] testValues1 = null;
        float[] testValues2 = null;
        Assert.assertTrue(MathArrays.equals(testValues1, testValues1));
        Assert.assertTrue(MathArrays.equalsIncludingNaN(testValues1, testValues1));
        testValues1 = new float[] { 1, 1, 2 };
        testValues2 = new float[] { -1, -1, 1 };
        Assert.assertTrue(MathArrays.equals(testValues1, testValues1));
        Assert.assertTrue(MathArrays.equalsIncludingNaN(testValues1, testValues1));
        Assert.assertFalse(MathArrays.equals(testValues1, testValues2));
        Assert.assertFalse(MathArrays.equalsIncludingNaN(testValues1, testValues2));
        final float[] testValues3 = new float[] { -1, -1 };
        Assert.assertFalse(MathArrays.equals(testValues1, testValues3));
        Assert.assertFalse(MathArrays.equalsIncludingNaN(testValues1, testValues3));
    }
}
