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
 * VERSION:4.4:FA:FA-2137:04/10/2019:FA mineure Patrius V4.3
 * VERSION:4.3:FA:FA-2109:15/05/2019:[PATRIUS] anomalie de cadrage d'angle
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * VERSION::FA:2109:23/04/2019:correction of quality issue with angle normalization
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.util;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.distribution.RealDistribution;
import fr.cnes.sirius.patrius.math.distribution.UniformRealDistribution;
import fr.cnes.sirius.patrius.math.exception.MathArithmeticException;
import fr.cnes.sirius.patrius.math.exception.NotFiniteNumberException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.random.RandomDataGenerator;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Test cases for the MathUtils class.
 * 
 * @version $Id: MathUtilsTest.java 18108 2017-10-04 06:45:27Z bignon $
 *          2007) $
 */
public final class MathUtilsTest {
    
    /**
     * Evaluate and cover the two specific cases in {@link MathUtils#normalizeAngle(double, double)} method.
     */
    @Test
    public void testFA2109() {

        final double twoPi = 2 * FastMath.PI;

        Assert.assertEquals(0, MathUtils.normalizeAngle(-4.9E-324, FastMath.PI), twoPi);
        Assert.assertEquals(0, MathUtils.normalizeAngle(-1.0E-323, FastMath.PI), twoPi);

        Assert.assertEquals(0, MathUtils.normalizeAngle(-6.9175290276410808E16, 0.), twoPi);
        Assert.assertEquals(0, MathUtils.normalizeAngle(-6.9175290276410808E16, FastMath.PI), twoPi);
    }

    @Test
    public void testConstants() {
        Assert.assertEquals(6.283185307179586, MathUtils.TWO_PI, 1.0e-20);
        Assert.assertEquals(1.5707963267948966, MathUtils.HALF_PI, 1.0e-20);
        Assert.assertEquals(0.0174532925199432950, MathUtils.DEG_TO_RAD, 1.0e-20);
        Assert.assertEquals(57.29577951308232, MathUtils.RAD_TO_DEG, 1.0e-20);
    }

    @Test
    public void testHash() {
        final double[] testArray = {
            Double.NaN,
            Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY,
            1d,
            0d,
            1E-14,
            (1 + 1E-14),
            Double.MIN_VALUE,
            Double.MAX_VALUE };
        for (int i = 0; i < testArray.length; i++) {
            for (int j = 0; j < testArray.length; j++) {
                if (i == j) {
                    Assert.assertEquals(MathUtils.hash(testArray[i]), MathUtils.hash(testArray[j]));
                    Assert.assertEquals(MathUtils.hash(testArray[j]), MathUtils.hash(testArray[i]));
                } else {
                    Assert.assertTrue(MathUtils.hash(testArray[i]) != MathUtils.hash(testArray[j]));
                    Assert.assertTrue(MathUtils.hash(testArray[j]) != MathUtils.hash(testArray[i]));
                }
            }
        }
    }

    @Test
    public void testArrayHash() {
        Assert.assertEquals(0, MathUtils.hash((double[]) null));
        Assert.assertEquals(MathUtils.hash(new double[] {
            Double.NaN, Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY, 1d, 0d
        }),
            MathUtils.hash(new double[] {
                Double.NaN, Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY, 1d, 0d
            }));
        Assert.assertFalse(MathUtils.hash(new double[] { 1d }) ==
            MathUtils.hash(new double[] { MathLib.nextAfter(1d, 2d) }));
        Assert.assertFalse(MathUtils.hash(new double[] { 1d }) ==
            MathUtils.hash(new double[] { 1d, 1d }));
    }

    /**
     * Make sure that permuted arrays do not hash to the same value.
     */
    @Test
    public void testPermutedArrayHash() {
        final double[] original = new double[10];
        final double[] permuted = new double[10];
        final RandomDataGenerator random = new RandomDataGenerator();

        // Generate 10 distinct random values
        for (int i = 0; i < 10; i++) {
            final RealDistribution u = new UniformRealDistribution(i + 0.5, i + 0.75);
            original[i] = u.sample();
        }

        // Generate a random permutation, making sure it is not the identity
        boolean isIdentity = true;
        do {
            final int[] permutation = random.nextPermutation(10, 10);
            for (int i = 0; i < 10; i++) {
                if (i != permutation[i]) {
                    isIdentity = false;
                }
                permuted[i] = original[permutation[i]];
            }
        } while (isIdentity);

        // Verify that permuted array has different hash
        Assert.assertFalse(MathUtils.hash(original) == MathUtils.hash(permuted));
    }

    @Test
    public void testIndicatorByte() {
        Assert.assertEquals((byte) 1, MathUtils.copySign((byte) 1, (byte) 2));
        Assert.assertEquals((byte) 1, MathUtils.copySign((byte) 1, (byte) 0));
        Assert.assertEquals((byte) (-1), MathUtils.copySign((byte) 1, (byte) (-2)));
    }

    @Test
    public void testIndicatorInt() {
        Assert.assertEquals(1, MathUtils.copySign(1, 2));
        Assert.assertEquals(1, MathUtils.copySign(1, 0));
        Assert.assertEquals((-1), MathUtils.copySign(1, -2));
    }

    @Test
    public void testIndicatorLong() {
        Assert.assertEquals(1L, MathUtils.copySign(1L, 2L));
        Assert.assertEquals(1L, MathUtils.copySign(1L, 0L));
        Assert.assertEquals(-1L, MathUtils.copySign(1L, -2L));
    }

    @Test
    public void testIndicatorShort() {
        Assert.assertEquals((short) 1, MathUtils.copySign((short) 1, (short) 2));
        Assert.assertEquals((short) 1, MathUtils.copySign((short) 1, (short) 0));
        Assert.assertEquals((short) (-1), MathUtils.copySign((short) 1, (short) (-2)));
    }

    @Test
    public void testNormalizeAngle() {
        for (double a = -15.0; a <= 15.0; a += 0.1) {
            for (double b = -15.0; b <= 15.0; b += 0.2) {
                final double c = MathUtils.normalizeAngle(a, b);
                Assert.assertTrue((b - FastMath.PI) <= c);
                Assert.assertTrue(c <= (b + FastMath.PI));
                final double twoK = MathLib.rint((a - c) / FastMath.PI);
                Assert.assertEquals(c, a - twoK * FastMath.PI, 1.0e-14);
            }
        }
    }

    @Test
    public void testReduce() {
        final double period = -12.222;
        final double offset = 13;

        final double delta = 1.5;

        double orig = offset + 122456789 * period + delta;
        double expected = delta;
        Assert.assertEquals(expected,
            MathUtils.reduce(orig, period, offset),
            1e-7);
        Assert.assertEquals(expected,
            MathUtils.reduce(orig, -period, offset),
            1e-7);

        orig = offset - 123356789 * period - delta;
        expected = Math.abs(period) - delta;
        Assert.assertEquals(expected,
            MathUtils.reduce(orig, period, offset),
            1e-6);
        Assert.assertEquals(expected,
            MathUtils.reduce(orig, -period, offset),
            1e-6);

        orig = offset - 123446789 * period + delta;
        expected = delta;
        Assert.assertEquals(expected,
            MathUtils.reduce(orig, period, offset),
            1e-6);
        Assert.assertEquals(expected,
            MathUtils.reduce(orig, -period, offset),
            1e-6);

        try {
            MathUtils.reduce(orig, Double.NaN, offset);
        } catch (final ArithmeticException e) {
            Assert.assertTrue(true);
        }
        try {
            MathUtils.reduce(Double.NaN, period, offset);
        } catch (final ArithmeticException e) {
            Assert.assertTrue(true);
        }
        try {
            MathUtils.reduce(orig, period, Double.NaN);
        } catch (final ArithmeticException e) {
            Assert.assertTrue(true);
        }
        try {
            MathUtils.reduce(orig, period, Double.POSITIVE_INFINITY);
        } catch (final ArithmeticException e) {
            Assert.assertTrue(true);
        }
        try {
            MathUtils.reduce(Double.POSITIVE_INFINITY, period, offset);
        } catch (final ArithmeticException e) {
            Assert.assertTrue(true);
        }
        try {
            MathUtils.reduce(orig, Double.POSITIVE_INFINITY, offset);
        } catch (final ArithmeticException e) {
            Assert.assertTrue(true);
        }
        try {
            MathUtils.reduce(orig, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
        } catch (final ArithmeticException e) {
            Assert.assertTrue(true);
        }
        try {
            MathUtils.reduce(Double.POSITIVE_INFINITY, period, Double.POSITIVE_INFINITY);
        } catch (final ArithmeticException e) {
            Assert.assertTrue(true);
        }
        try {
            MathUtils.reduce(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, offset);
        } catch (final ArithmeticException e) {
            Assert.assertTrue(true);
        }
        try {
            MathUtils.reduce(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
        } catch (final ArithmeticException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testReduceComparedWithNormalizeAngle() {
        final double tol = Math.ulp(1d);
        final double period = 2 * Math.PI;
        for (double a = -15; a <= 15; a += 0.5) {
            for (double center = -15; center <= 15; center += 1) {
                final double nA = MathUtils.normalizeAngle(a, center);
                final double offset = center - Math.PI;
                final double r = MathUtils.reduce(a, period, offset);
                Assert.assertEquals(nA, r + offset, tol);
            }
        }
    }

    @Test
    public void testSignByte() {
        final byte one = (byte) 1;
        Assert.assertEquals((byte) 1, MathUtils.copySign(one, (byte) 2));
        Assert.assertEquals((byte) (-1), MathUtils.copySign(one, (byte) (-2)));
    }

    @Test
    public void testSignInt() {
        final int one = 1;
        Assert.assertEquals(1, MathUtils.copySign(one, 2));
        Assert.assertEquals((-1), MathUtils.copySign(one, -2));
    }

    @Test
    public void testSignLong() {
        final long one = 1L;
        Assert.assertEquals(1L, MathUtils.copySign(one, 2L));
        Assert.assertEquals(-1L, MathUtils.copySign(one, -2L));
    }

    @Test
    public void testSignShort() {
        final short one = (short) 1;
        Assert.assertEquals((short) 1, MathUtils.copySign(one, (short) 2));
        Assert.assertEquals((short) (-1), MathUtils.copySign(one, (short) (-2)));
    }

    @Test
    public void testCheckFinite() {
        try {
            MathUtils.checkFinite(Double.POSITIVE_INFINITY);
            Assert.fail("an exception should have been thrown");
        } catch (final NotFiniteNumberException e) {
            // Expected
        }
        try {
            MathUtils.checkFinite(Double.NEGATIVE_INFINITY);
            Assert.fail("an exception should have been thrown");
        } catch (final NotFiniteNumberException e) {
            // Expected
        }
        try {
            MathUtils.checkFinite(Double.NaN);
            Assert.fail("an exception should have been thrown");
        } catch (final NotFiniteNumberException e) {
            // Expected
        }

        try {
            MathUtils.checkFinite(new double[] { 0, -1, Double.POSITIVE_INFINITY, -2, 3 });
            Assert.fail("an exception should have been thrown");
        } catch (final NotFiniteNumberException e) {
            // Expected
        }
        try {
            MathUtils.checkFinite(new double[] { 1, Double.NEGATIVE_INFINITY, -2, 3 });
            Assert.fail("an exception should have been thrown");
        } catch (final NotFiniteNumberException e) {
            // Expected
        }
        try {
            MathUtils.checkFinite(new double[] { 4, 3, -1, Double.NaN, -2, 1 });
            Assert.fail("an exception should have been thrown");
        } catch (final NotFiniteNumberException e) {
            // Expected
        }
    }

    @Test
    public void testCheckNotNull1() {
        try {
            final Object obj = null;
            MathUtils.checkNotNull(obj);
        } catch (final NullArgumentException e) {
            // Expected.
        }
    }

    @Test
    public void testCheckNotNull2() {
        try {
            final double[] array = null;
            MathUtils.checkNotNull(array, PatriusMessages.INPUT_ARRAY);
        } catch (final NullArgumentException e) {
            // Expected.
        }
    }

    @Test
    public void testCopySignByte() {
        byte a = MathUtils.copySign(Byte.MIN_VALUE, (byte) -1);
        Assert.assertEquals(Byte.MIN_VALUE, a);

        final byte minValuePlusOne = Byte.MIN_VALUE + (byte) 1;
        a = MathUtils.copySign(minValuePlusOne, (byte) 1);
        Assert.assertEquals(Byte.MAX_VALUE, a);

        a = MathUtils.copySign(Byte.MAX_VALUE, (byte) -1);
        Assert.assertEquals(minValuePlusOne, a);

        final byte one = 1;
        byte val = -2;
        a = MathUtils.copySign(val, one);
        Assert.assertEquals(-val, a);

        final byte minusOne = -one;
        val = 2;
        a = MathUtils.copySign(val, minusOne);
        Assert.assertEquals(-val, a);

        val = 0;
        a = MathUtils.copySign(val, minusOne);
        Assert.assertEquals(val, a);

        val = 0;
        a = MathUtils.copySign(val, one);
        Assert.assertEquals(val, a);
    }

    @Test(expected = MathArithmeticException.class)
    public void testCopySignByte2() {
        MathUtils.copySign(Byte.MIN_VALUE, (byte) 1);
    }
}
