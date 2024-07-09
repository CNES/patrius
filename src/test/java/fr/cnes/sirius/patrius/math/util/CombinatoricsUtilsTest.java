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
package fr.cnes.sirius.patrius.math.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.exception.MathArithmeticException;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.exception.NotPositiveException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooLargeException;

/**
 * Test cases for the {@link CombinatoricsUtils} class.
 * 
 */
public class CombinatoricsUtilsTest {

    /** cached binomial coefficients */
    private static final List<Map<Integer, Long>> binomialCache = new ArrayList<Map<Integer, Long>>();

    /** Verify that b(0,0) = 1 */
    @Test
    public void test0Choose0() {
        Assert.assertEquals(CombinatoricsUtils.binomialCoefficientDouble(0, 0), 1d, 0);
        Assert.assertEquals(CombinatoricsUtils.binomialCoefficientLog(0, 0), 0d, 0);
        Assert.assertEquals(CombinatoricsUtils.binomialCoefficient(0, 0), 1);
    }

    @Test
    public void testBinomialCoefficient() {
        final long[] bcoef5 = {
            1,
            5,
            10,
            10,
            5,
            1 };
        final long[] bcoef6 = {
            1,
            6,
            15,
            20,
            15,
            6,
            1 };
        for (int i = 0; i < 6; i++) {
            Assert.assertEquals("5 choose " + i, bcoef5[i], CombinatoricsUtils.binomialCoefficient(5, i));
        }
        for (int i = 0; i < 7; i++) {
            Assert.assertEquals("6 choose " + i, bcoef6[i], CombinatoricsUtils.binomialCoefficient(6, i));
        }

        for (int n = 1; n < 10; n++) {
            for (int k = 0; k <= n; k++) {
                Assert.assertEquals(n + " choose " + k, this.binomialCoefficient(n, k),
                    CombinatoricsUtils.binomialCoefficient(n, k));
                Assert.assertEquals(n + " choose " + k, this.binomialCoefficient(n, k),
                    CombinatoricsUtils.binomialCoefficientDouble(n, k), Double.MIN_VALUE);
                Assert.assertEquals(n + " choose " + k, MathLib.log(this.binomialCoefficient(n, k)),
                    CombinatoricsUtils.binomialCoefficientLog(n, k), 10E-12);
            }
        }

        final int[] n = { 34, 66, 100, 1500, 1500 };
        final int[] k = { 17, 33, 10, 1500 - 4, 4 };
        for (int i = 0; i < n.length; i++) {
            final long expected = this.binomialCoefficient(n[i], k[i]);
            Assert.assertEquals(n[i] + " choose " + k[i], expected,
                CombinatoricsUtils.binomialCoefficient(n[i], k[i]));
            Assert.assertEquals(n[i] + " choose " + k[i], expected,
                CombinatoricsUtils.binomialCoefficientDouble(n[i], k[i]), 0.0);
            Assert.assertEquals("log(" + n[i] + " choose " + k[i] + ")", MathLib.log(expected),
                CombinatoricsUtils.binomialCoefficientLog(n[i], k[i]), 0.0);
        }
    }

    @Test
    public void testBinomialCoefficientFail() {
        try {
            CombinatoricsUtils.binomialCoefficient(4, 5);
            Assert.fail("expecting MathIllegalArgumentException");
        } catch (final MathIllegalArgumentException ex) {
            // ignored
        }

        try {
            CombinatoricsUtils.binomialCoefficientDouble(4, 5);
            Assert.fail("expecting MathIllegalArgumentException");
        } catch (final MathIllegalArgumentException ex) {
            // ignored
        }

        try {
            CombinatoricsUtils.binomialCoefficientLog(4, 5);
            Assert.fail("expecting MathIllegalArgumentException");
        } catch (final MathIllegalArgumentException ex) {
            // ignored
        }

        try {
            CombinatoricsUtils.binomialCoefficient(-1, -2);
            Assert.fail("expecting MathIllegalArgumentException");
        } catch (final MathIllegalArgumentException ex) {
            // ignored
        }
        try {
            CombinatoricsUtils.binomialCoefficientDouble(-1, -2);
            Assert.fail("expecting MathIllegalArgumentException");
        } catch (final MathIllegalArgumentException ex) {
            // ignored
        }
        try {
            CombinatoricsUtils.binomialCoefficientLog(-1, -2);
            Assert.fail("expecting MathIllegalArgumentException");
        } catch (final MathIllegalArgumentException ex) {
            // ignored
        }

        try {
            CombinatoricsUtils.binomialCoefficient(67, 30);
            Assert.fail("expecting MathArithmeticException");
        } catch (final MathArithmeticException ex) {
            // ignored
        }
        try {
            CombinatoricsUtils.binomialCoefficient(67, 34);
            Assert.fail("expecting MathArithmeticException");
        } catch (final MathArithmeticException ex) {
            // ignored
        }
        final double x = CombinatoricsUtils.binomialCoefficientDouble(1030, 515);
        Assert.assertTrue("expecting infinite binomial coefficient", Double
            .isInfinite(x));
    }

    /**
     * Tests correctness for large n and sharpness of upper bound in API doc
     * JIRA: MATH-241
     */
    @Test
    public void testBinomialCoefficientLarge() throws Exception {
        // This tests all legal and illegal values for n <= 200.
        for (int n = 0; n <= 200; n++) {
            for (int k = 0; k <= n; k++) {
                long ourResult = -1;
                long exactResult = -1;
                boolean shouldThrow = false;
                boolean didThrow = false;
                try {
                    ourResult = CombinatoricsUtils.binomialCoefficient(n, k);
                } catch (final MathArithmeticException ex) {
                    didThrow = true;
                }
                try {
                    exactResult = this.binomialCoefficient(n, k);
                } catch (final MathArithmeticException ex) {
                    shouldThrow = true;
                }
                Assert.assertEquals(n + " choose " + k, exactResult, ourResult);
                Assert.assertEquals(n + " choose " + k, shouldThrow, didThrow);
                Assert.assertTrue(n + " choose " + k, (n > 66 || !didThrow));

                if (!shouldThrow && exactResult > 1) {
                    Assert.assertEquals(n + " choose " + k, 1.,
                        CombinatoricsUtils.binomialCoefficientDouble(n, k) / exactResult, 1e-10);
                    Assert.assertEquals(n + " choose " + k, 1,
                        CombinatoricsUtils.binomialCoefficientLog(n, k) / MathLib.log(exactResult), 1e-10);
                }
            }
        }

        long ourResult = CombinatoricsUtils.binomialCoefficient(300, 3);
        long exactResult = this.binomialCoefficient(300, 3);
        Assert.assertEquals(exactResult, ourResult);

        ourResult = CombinatoricsUtils.binomialCoefficient(700, 697);
        exactResult = this.binomialCoefficient(700, 697);
        Assert.assertEquals(exactResult, ourResult);

        // This one should throw
        try {
            CombinatoricsUtils.binomialCoefficient(700, 300);
            Assert.fail("Expecting MathArithmeticException");
        } catch (final MathArithmeticException ex) {
            // Expected
        }

        final int n = 10000;
        ourResult = CombinatoricsUtils.binomialCoefficient(n, 3);
        exactResult = this.binomialCoefficient(n, 3);
        Assert.assertEquals(exactResult, ourResult);
        Assert.assertEquals(1, CombinatoricsUtils.binomialCoefficientDouble(n, 3) / exactResult, 1e-10);
        Assert.assertEquals(1, CombinatoricsUtils.binomialCoefficientLog(n, 3) / MathLib.log(exactResult), 1e-10);

    }

    @Test
    public void testFactorial() {
        for (int i = 1; i < 21; i++) {
            Assert.assertEquals(i + "! ", this.factorial(i), CombinatoricsUtils.factorial(i));
            Assert.assertEquals(i + "! ", this.factorial(i), CombinatoricsUtils.factorialDouble(i), Double.MIN_VALUE);
            Assert.assertEquals(i + "! ", MathLib.log(this.factorial(i)), CombinatoricsUtils.factorialLog(i), 10E-12);
        }

        Assert.assertEquals("0", 1, CombinatoricsUtils.factorial(0));
        Assert.assertEquals("0", 1.0d, CombinatoricsUtils.factorialDouble(0), 1E-14);
        Assert.assertEquals("0", 0.0d, CombinatoricsUtils.factorialLog(0), 1E-14);
    }

    @Test
    public void testFactorialFail() {
        try {
            CombinatoricsUtils.factorial(-1);
            Assert.fail("expecting MathIllegalArgumentException");
        } catch (final MathIllegalArgumentException ex) {
            // ignored
        }
        try {
            CombinatoricsUtils.factorialDouble(-1);
            Assert.fail("expecting MathIllegalArgumentException");
        } catch (final MathIllegalArgumentException ex) {
            // ignored
        }
        try {
            CombinatoricsUtils.factorialLog(-1);
            Assert.fail("expecting MathIllegalArgumentException");
        } catch (final MathIllegalArgumentException ex) {
            // ignored
        }
        try {
            CombinatoricsUtils.factorial(21);
            Assert.fail("expecting MathArithmeticException");
        } catch (final MathArithmeticException ex) {
            // ignored
        }
        Assert.assertTrue("expecting infinite factorial value",
            Double.isInfinite(CombinatoricsUtils.factorialDouble(171)));
    }

    @Test(expected = NotPositiveException.class)
    public void testCheckBinomial1() {
        // n < 0
        CombinatoricsUtils.checkBinomial(-1, -2);
    }

    @Test(expected = NumberIsTooLargeException.class)
    public void testCheckBinomial2() {
        // k > n
        CombinatoricsUtils.checkBinomial(4, 5);
    }

    @Test
    public void testCheckBinomial3() {
        // OK (no exception thrown)
        CombinatoricsUtils.checkBinomial(5, 4);
    }

    /**
     * Exact (caching) recursive implementation to test against
     */
    private long binomialCoefficient(final int n, final int k) throws MathArithmeticException {
        if (binomialCache.size() > n) {
            final Long cachedResult = binomialCache.get(n).get(Integer.valueOf(k));
            if (cachedResult != null) {
                return cachedResult.longValue();
            }
        }
        long result = -1;
        if ((n == k) || (k == 0)) {
            result = 1;
        } else if ((k == 1) || (k == n - 1)) {
            result = n;
        } else {
            // Reduce stack depth for larger values of n
            if (k < n - 100) {
                this.binomialCoefficient(n - 100, k);
            }
            if (k > 100) {
                this.binomialCoefficient(n - 100, k - 100);
            }
            result = ArithmeticUtils.addAndCheck(this.binomialCoefficient(n - 1, k - 1),
                this.binomialCoefficient(n - 1, k));
        }
        if (result == -1) {
            throw new MathArithmeticException();
        }
        for (int i = binomialCache.size(); i < n + 1; i++) {
            binomialCache.add(new HashMap<Integer, Long>());
        }
        binomialCache.get(n).put(Integer.valueOf(k), Long.valueOf(result));
        return result;
    }

    /**
     * Exact direct multiplication implementation to test against
     */
    private long factorial(final int n) {
        long result = 1;
        for (int i = 2; i <= n; i++) {
            result *= i;
        }
        return result;
    }
}
