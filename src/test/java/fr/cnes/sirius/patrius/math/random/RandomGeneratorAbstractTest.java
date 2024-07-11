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
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.random;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.stat.Frequency;
import fr.cnes.sirius.patrius.math.stat.descriptive.SummaryStatistics;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Base class for RandomGenerator tests.
 * 
 * Tests RandomGenerator methods directly and also executes RandomDataTest
 * test cases against a RandomDataImpl created using the provided generator.
 * 
 * RandomGenerator test classes should extend this class, implementing
 * makeGenerator() to provide a concrete generator to test. The generator
 * returned by makeGenerator should be seeded with a fixed seed.
 * 
 * @version $Id: RandomGeneratorAbstractTest.java 18108 2017-10-04 06:45:27Z bignon $
 */

public abstract class RandomGeneratorAbstractTest extends RandomDataTest {

    /** RandomGenerator under test */
    protected RandomGenerator generator;

    /**
     * Override this method in subclasses to provide a concrete generator to test.
     * Return a generator seeded with a fixed seed.
     */
    protected abstract RandomGenerator makeGenerator();

    /**
     * Initialize generator and randomData instance in superclass.
     */
    public RandomGeneratorAbstractTest() {
        this.generator = this.makeGenerator();
        this.randomData = new RandomDataGenerator(this.generator);
    }

    /**
     * Set a fixed seed for the tests
     */
    @Before
    public void setUp() {
        this.generator = this.makeGenerator();
    }

    // Omit secureXxx tests, since they do not use the provided generator
    @Override
    public void testNextSecureLongIAE() {
    }

    @Override
    public void testNextSecureLongNegativeToPositiveRange() {
    }

    @Override
    public void testNextSecureLongNegativeRange() {
    }

    @Override
    public void testNextSecureLongPositiveRange() {
    }

    @Override
    public void testNextSecureIntIAE() {
    }

    @Override
    public void testNextSecureIntNegativeToPositiveRange() {
    }

    @Override
    public void testNextSecureIntNegativeRange() {
    }

    @Override
    public void testNextSecureIntPositiveRange() {
    }

    @Override
    public void testNextSecureHex() {
    }

    @Test
    /**
     * Tests uniformity of nextInt(int) distribution by generating 1000
     * samples for each of 10 test values and for each sample performing
     * a chi-square test of homogeneity of the observed distribution with
     * the expected uniform distribution.  Tests are performed at the .01
     * level and an average failure rate higher than 2% (i.e. more than 20
     * null hypothesis rejections) causes the test case to fail.
     *
     * All random values are generated using the generator instance used by
     * other tests and the generator is not reseeded, so this is a fixed seed
     * test.
     */
    public void testNextIntDirect() {
        // Set up test values - end of the array filled randomly
        final int[] testValues = new int[] { 4, 10, 12, 32, 100, 10000, 0, 0, 0, 0 };
        for (int i = 6; i < 10; i++) {
            final int val = this.generator.nextInt();
            testValues[i] = val < 0 ? -val : val + 1;
        }

        final int numTests = 1000;
        for (final int n : testValues) {
            // Set up bins
            int[] binUpperBounds;
            if (n < 32) {
                binUpperBounds = new int[n];
                for (int k = 0; k < n; k++) {
                    binUpperBounds[k] = k;
                }
            } else {
                binUpperBounds = new int[10];
                final int step = n / 10;
                for (int k = 0; k < 9; k++) {
                    binUpperBounds[k] = (k + 1) * step;
                }
                binUpperBounds[9] = n - 1;
            }
            // Run the tests
            int numFailures = 0;
            final int binCount = binUpperBounds.length;
            final long[] observed = new long[binCount];
            final double[] expected = new double[binCount];
            expected[0] = binUpperBounds[0] == 0 ? (double) this.smallSampleSize / (double) n
                    : (double) ((binUpperBounds[0] + 1) * this.smallSampleSize) / (double) n;
            for (int k = 1; k < binCount; k++) {
                expected[k] = (double) this.smallSampleSize * (double) (binUpperBounds[k] - binUpperBounds[k - 1]) / n;
            }
            for (int j = 0; j < numTests; j++) {
                Arrays.fill(observed, 0);
                for (int k = 0; k < this.smallSampleSize; k++) {
                    final int value = this.generator.nextInt(n);
                    Assert.assertTrue("nextInt range", (value >= 0) && (value < n));
                    for (int l = 0; l < binCount; l++) {
                        if (binUpperBounds[l] >= value) {
                            observed[l]++;
                            break;
                        }
                    }
                }
                if (this.testStatistic.chiSquareTest(expected, observed) < 0.01) {
                    numFailures++;
                }
            }
            if ((double) numFailures / (double) numTests > 0.02) {
                Assert.fail("Too many failures for n = " + n + " " + numFailures + " out of " + numTests
                        + " tests failed.");
            }
        }
    }

    @Override
    @Test(expected = MathIllegalArgumentException.class)
    public void testNextIntIAE() {
        try {
            this.generator.nextInt(-1);
            Assert.fail("MathIllegalArgumentException expected");
        } catch (final MathIllegalArgumentException ex) {
            // ignored
        }
        this.generator.nextInt(0);
    }

    @Test
    public void testNextLongDirect() {
        final long q1 = Long.MAX_VALUE / 4;
        final long q2 = 2 * q1;
        final long q3 = 3 * q1;

        final Frequency freq = new Frequency();
        long val = 0;
        int value = 0;
        for (int i = 0; i < this.smallSampleSize; i++) {
            val = this.generator.nextLong();
            val = val < 0 ? -val : val;
            if (val < q1) {
                value = 0;
            } else if (val < q2) {
                value = 1;
            } else if (val < q3) {
                value = 2;
            } else {
                value = 3;
            }
            freq.addValue(value);
        }
        final long[] observed = new long[4];
        for (int i = 0; i < 4; i++) {
            observed[i] = freq.getCount(i);
        }

        /*
         * Use ChiSquare dist with df = 4-1 = 3, alpha = .001
         * Change to 11.34 for alpha = .01
         */
        Assert.assertTrue("chi-square test -- will fail about 1 in 1000 times",
                this.testStatistic.chiSquare(this.expected, observed) < 16.27);
    }

    @Test
    public void testNextBooleanDirect() {
        final long halfSampleSize = this.smallSampleSize / 2;
        final double[] expected = { halfSampleSize, halfSampleSize };
        final long[] observed = new long[2];
        for (int i = 0; i < this.smallSampleSize; i++) {
            if (this.generator.nextBoolean()) {
                observed[0]++;
            } else {
                observed[1]++;
            }
        }
        /*
         * Use ChiSquare dist with df = 2-1 = 1, alpha = .001
         * Change to 6.635 for alpha = .01
         */
        Assert.assertTrue("chi-square test -- will fail about 1 in 1000 times",
                this.testStatistic.chiSquare(expected, observed) < 10.828);
    }

    @Test
    public void testNextFloatDirect() {
        final Frequency freq = new Frequency();
        float val = 0;
        int value = 0;
        for (int i = 0; i < this.smallSampleSize; i++) {
            val = this.generator.nextFloat();
            if (val < 0.25) {
                value = 0;
            } else if (val < 0.5) {
                value = 1;
            } else if (val < 0.75) {
                value = 2;
            } else {
                value = 3;
            }
            freq.addValue(value);
        }
        final long[] observed = new long[4];
        for (int i = 0; i < 4; i++) {
            observed[i] = freq.getCount(i);
        }

        /*
         * Use ChiSquare dist with df = 4-1 = 3, alpha = .001
         * Change to 11.34 for alpha = .01
         */
        Assert.assertTrue("chi-square test -- will fail about 1 in 1000 times",
                this.testStatistic.chiSquare(this.expected, observed) < 16.27);
    }

    @Test
    public void testDoubleDirect() {
        final SummaryStatistics sample = new SummaryStatistics();
        final int N = 10000;
        for (int i = 0; i < N; ++i) {
            sample.addValue(this.generator.nextDouble());
        }
        Assert.assertEquals("Note: This test will fail randomly about 1 in 100 times.", 0.5, sample.getMean(),
                MathLib.sqrt(N / 12.0) * 2.576);
        Assert.assertEquals(1.0 / (2.0 * MathLib.sqrt(3.0)), sample.getStandardDeviation(), 0.01);
    }

    @Test
    public void testFloatDirect() {
        final SummaryStatistics sample = new SummaryStatistics();
        final int N = 1000;
        for (int i = 0; i < N; ++i) {
            sample.addValue(this.generator.nextFloat());
        }
        Assert.assertEquals("Note: This test will fail randomly about 1 in 100 times.", 0.5, sample.getMean(),
                MathLib.sqrt(N / 12.0) * 2.576);
        Assert.assertEquals(1.0 / (2.0 * MathLib.sqrt(3.0)), sample.getStandardDeviation(), 0.01);
    }

    @Test(expected = MathIllegalArgumentException.class)
    public void testNextIntNeg() {
        this.generator.nextInt(-1);
    }

    @Test
    public void testNextInt2() {
        int walk = 0;
        final int N = 10000;
        for (int k = 0; k < N; ++k) {
            if (this.generator.nextInt() >= 0) {
                ++walk;
            } else {
                --walk;
            }
        }
        Assert.assertTrue("Walked too far astray: " + walk + "\nNote: This "
                + "test will fail randomly about 1 in 100 times.", MathLib.abs(walk) < MathLib.sqrt(N) * 2.576);
    }

    @Test
    public void testNextLong2() {
        int walk = 0;
        final int N = 1000;
        for (int k = 0; k < N; ++k) {
            if (this.generator.nextLong() >= 0) {
                ++walk;
            } else {
                --walk;
            }
        }
        Assert.assertTrue("Walked too far astray: " + walk + "\nNote: This "
                + "test will fail randomly about 1 in 100 times.", MathLib.abs(walk) < MathLib.sqrt(N) * 2.576);
    }

    @Test
    public void testNexBoolean2() {
        int walk = 0;
        final int N = 10000;
        for (int k = 0; k < N; ++k) {
            if (this.generator.nextBoolean()) {
                ++walk;
            } else {
                --walk;
            }
        }
        Assert.assertTrue("Walked too far astray: " + walk + "\nNote: This "
                + "test will fail randomly about 1 in 100 times.", MathLib.abs(walk) < MathLib.sqrt(N) * 2.576);
    }

    @Test
    public void testNexBytes() {
        final long[] count = new long[256];
        final byte[] bytes = new byte[10];
        final double[] expected = new double[256];
        final int sampleSize = 100000;

        for (int i = 0; i < 256; i++) {
            expected[i] = (double) sampleSize / 265f;
        }

        for (int k = 0; k < sampleSize; ++k) {
            this.generator.nextBytes(bytes);
            for (final byte b : bytes) {
                ++count[b + 128];
            }
        }

        TestUtils.assertChiSquareAccept(expected, count, 0.001);

    }

    @Test
    public void testSeeding() {
        // makeGenerator initializes with fixed seed
        final RandomGenerator gen = this.makeGenerator();
        RandomGenerator gen1 = this.makeGenerator();
        this.checkSameSequence(gen, gen1);
        // reseed, but recreate the second one
        // verifies MATH-723
        gen.setSeed(100);
        gen1 = this.makeGenerator();
        gen1.setSeed(100);
        this.checkSameSequence(gen, gen1);
    }

    private void checkSameSequence(final RandomGenerator gen1,
            final RandomGenerator gen2) {
        final int len = 11; // Needs to be an odd number to check MATH-723
        final double[][] values = new double[2][len];
        for (int i = 0; i < len; i++) {
            values[0][i] = gen1.nextDouble();
        }
        for (int i = 0; i < len; i++) {
            values[1][i] = gen2.nextDouble();
        }
        Assert.assertTrue(Arrays.equals(values[0], values[1]));
        for (int i = 0; i < len; i++) {
            values[0][i] = gen1.nextFloat();
        }
        for (int i = 0; i < len; i++) {
            values[1][i] = gen2.nextFloat();
        }
        Assert.assertTrue(Arrays.equals(values[0], values[1]));
        for (int i = 0; i < len; i++) {
            values[0][i] = gen1.nextInt();
        }
        for (int i = 0; i < len; i++) {
            values[1][i] = gen2.nextInt();
        }
        Assert.assertTrue(Arrays.equals(values[0], values[1]));
        for (int i = 0; i < len; i++) {
            values[0][i] = gen1.nextLong();
        }
        for (int i = 0; i < len; i++) {
            values[1][i] = gen2.nextLong();
        }
        Assert.assertTrue(Arrays.equals(values[0], values[1]));
        for (int i = 0; i < len; i++) {
            values[0][i] = gen1.nextInt(len);
        }
        for (int i = 0; i < len; i++) {
            values[1][i] = gen2.nextInt(len);
        }
        Assert.assertTrue(Arrays.equals(values[0], values[1]));
        for (int i = 0; i < len; i++) {
            values[0][i] = gen1.nextBoolean() ? 1 : 0;
        }
        for (int i = 0; i < len; i++) {
            values[1][i] = gen2.nextBoolean() ? 1 : 0;
        }
        Assert.assertTrue(Arrays.equals(values[0], values[1]));
        for (int i = 0; i < len; i++) {
            values[0][i] = gen1.nextGaussian();
        }
        for (int i = 0; i < len; i++) {
            values[1][i] = gen2.nextGaussian();
        }
        Assert.assertTrue(Arrays.equals(values[0], values[1]));
    }

}
