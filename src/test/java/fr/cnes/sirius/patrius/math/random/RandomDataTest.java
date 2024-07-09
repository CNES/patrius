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
package fr.cnes.sirius.patrius.math.random;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import fr.cnes.sirius.patrius.math.Retry;
import fr.cnes.sirius.patrius.math.RetryRunner;
import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.distribution.BetaDistribution;
import fr.cnes.sirius.patrius.math.distribution.BinomialDistribution;
import fr.cnes.sirius.patrius.math.distribution.BinomialDistributionTest;
import fr.cnes.sirius.patrius.math.distribution.CauchyDistribution;
import fr.cnes.sirius.patrius.math.distribution.ChiSquaredDistribution;
import fr.cnes.sirius.patrius.math.distribution.ExponentialDistribution;
import fr.cnes.sirius.patrius.math.distribution.FDistribution;
import fr.cnes.sirius.patrius.math.distribution.GammaDistribution;
import fr.cnes.sirius.patrius.math.distribution.HypergeometricDistribution;
import fr.cnes.sirius.patrius.math.distribution.HypergeometricDistributionTest;
import fr.cnes.sirius.patrius.math.distribution.PascalDistribution;
import fr.cnes.sirius.patrius.math.distribution.PascalDistributionTest;
import fr.cnes.sirius.patrius.math.distribution.PoissonDistribution;
import fr.cnes.sirius.patrius.math.distribution.TDistribution;
import fr.cnes.sirius.patrius.math.distribution.WeibullDistribution;
import fr.cnes.sirius.patrius.math.distribution.ZipfDistribution;
import fr.cnes.sirius.patrius.math.distribution.ZipfDistributionTest;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.stat.Frequency;
import fr.cnes.sirius.patrius.math.stat.descriptive.SummaryStatistics;
import fr.cnes.sirius.patrius.math.stat.inference.ChiSquareTest;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Test cases for the RandomData class.
 * 
 * @version $Id: RandomDataTest.java 18108 2017-10-04 06:45:27Z bignon $
 *          2009) $
 */
@RunWith(RetryRunner.class)
public class RandomDataTest {

    public RandomDataTest() {
        this.randomData = new RandomDataGenerator();
        this.randomData.reSeed(1000);
    }

    protected final long smallSampleSize = 1000;
    protected final double[] expected = { 250, 250, 250, 250 };
    protected final int largeSampleSize = 10000;
    private final String[] hex = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
        "a", "b", "c", "d", "e", "f" };
    protected RandomDataGenerator randomData = null;
    protected final ChiSquareTest testStatistic = new ChiSquareTest();

    @Test
    public void testNextIntExtremeValues() {
        final int x = this.randomData.nextInt(Integer.MIN_VALUE, Integer.MAX_VALUE);
        final int y = this.randomData.nextInt(Integer.MIN_VALUE, Integer.MAX_VALUE);
        Assert.assertFalse(x == y);
    }

    @Test
    public void testNextLongExtremeValues() {
        final long x = this.randomData.nextLong(Long.MIN_VALUE, Long.MAX_VALUE);
        final long y = this.randomData.nextLong(Long.MIN_VALUE, Long.MAX_VALUE);
        Assert.assertFalse(x == y);
    }

    @Test
    public void testNextUniformExtremeValues() {
        final double x = this.randomData.nextUniform(-Double.MAX_VALUE, Double.MAX_VALUE);
        final double y = this.randomData.nextUniform(-Double.MAX_VALUE, Double.MAX_VALUE);
        Assert.assertFalse(x == y);
        Assert.assertFalse(Double.isNaN(x));
        Assert.assertFalse(Double.isNaN(y));
        Assert.assertFalse(Double.isInfinite(x));
        Assert.assertFalse(Double.isInfinite(y));
    }

    @Test
    public void testNextIntIAE() {
        try {
            this.randomData.nextInt(4, 3);
            Assert.fail("MathIllegalArgumentException expected");
        } catch (final MathIllegalArgumentException ex) {
            // ignored
        }
    }

    @Test
    public void testNextIntNegativeToPositiveRange() {
        for (int i = 0; i < 5; i++) {
            this.checkNextIntUniform(-3, 5);
            this.checkNextIntUniform(-3, 6);
        }
    }

    @Test
    public void testNextIntNegativeRange() {
        for (int i = 0; i < 5; i++) {
            this.checkNextIntUniform(-7, -4);
            this.checkNextIntUniform(-15, -2);
        }
    }

    @Test
    public void testNextIntPositiveRange() {
        for (int i = 0; i < 5; i++) {
            this.checkNextIntUniform(0, 3);
            this.checkNextIntUniform(2, 12);
            this.checkNextIntUniform(1, 2);
        }
    }

    private void checkNextIntUniform(final int min, final int max) {
        final Frequency freq = new Frequency();
        for (int i = 0; i < this.smallSampleSize; i++) {
            final int value = this.randomData.nextInt(min, max);
            Assert.assertTrue("nextInt range", (value >= min) && (value <= max));
            freq.addValue(value);
        }
        final int len = max - min + 1;
        final long[] observed = new long[len];
        for (int i = 0; i < len; i++) {
            observed[i] = freq.getCount(min + i);
        }
        final double[] expected = new double[len];
        for (int i = 0; i < len; i++) {
            expected[i] = 1d / len;
        }

        TestUtils.assertChiSquareAccept(expected, observed, 0.001);
    }

    @Test
    public void testNextLongIAE() {
        try {
            this.randomData.nextLong(4, 3);
            Assert.fail("MathIllegalArgumentException expected");
        } catch (final MathIllegalArgumentException ex) {
            // ignored
        }
    }

    @Test
    public void testNextLongNegativeToPositiveRange() {
        for (int i = 0; i < 5; i++) {
            this.checkNextLongUniform(-3, 5);
            this.checkNextLongUniform(-3, 6);
        }
    }

    @Test
    public void testNextLongNegativeRange() {
        for (int i = 0; i < 5; i++) {
            this.checkNextLongUniform(-7, -4);
            this.checkNextLongUniform(-15, -2);
        }
    }

    @Test
    public void testNextLongPositiveRange() {
        for (int i = 0; i < 5; i++) {
            this.checkNextLongUniform(0, 3);
            this.checkNextLongUniform(2, 12);
        }
    }

    private void checkNextLongUniform(final int min, final int max) {
        final Frequency freq = new Frequency();
        for (int i = 0; i < this.smallSampleSize; i++) {
            final long value = this.randomData.nextLong(min, max);
            Assert.assertTrue("nextLong range", (value >= min) && (value <= max));
            freq.addValue(value);
        }
        final int len = max - min + 1;
        final long[] observed = new long[len];
        for (int i = 0; i < len; i++) {
            observed[i] = freq.getCount(min + i);
        }
        final double[] expected = new double[len];
        for (int i = 0; i < len; i++) {
            expected[i] = 1d / len;
        }

        TestUtils.assertChiSquareAccept(expected, observed, 0.01);
    }

    @Test
    public void testNextSecureLongIAE() {
        try {
            this.randomData.nextSecureLong(4, 3);
            Assert.fail("MathIllegalArgumentException expected");
        } catch (final MathIllegalArgumentException ex) {
            // ignored
        }
    }

    @Test
    @Retry(3)
    public void testNextSecureLongNegativeToPositiveRange() {
        for (int i = 0; i < 5; i++) {
            this.checkNextSecureLongUniform(-3, 5);
            this.checkNextSecureLongUniform(-3, 6);
        }
    }

    @Test
    @Retry(3)
    public void testNextSecureLongNegativeRange() {
        for (int i = 0; i < 5; i++) {
            this.checkNextSecureLongUniform(-7, -4);
            this.checkNextSecureLongUniform(-15, -2);
        }
    }

    @Test
    @Retry(3)
    public void testNextSecureLongPositiveRange() {
        for (int i = 0; i < 5; i++) {
            this.checkNextSecureLongUniform(0, 3);
            this.checkNextSecureLongUniform(2, 12);
        }
    }

    private void checkNextSecureLongUniform(final int min, final int max) {
        final Frequency freq = new Frequency();
        for (int i = 0; i < this.smallSampleSize; i++) {
            final long value = this.randomData.nextSecureLong(min, max);
            Assert.assertTrue("nextLong range", (value >= min) && (value <= max));
            freq.addValue(value);
        }
        final int len = max - min + 1;
        final long[] observed = new long[len];
        for (int i = 0; i < len; i++) {
            observed[i] = freq.getCount(min + i);
        }
        final double[] expected = new double[len];
        for (int i = 0; i < len; i++) {
            expected[i] = 1d / len;
        }

        TestUtils.assertChiSquareAccept(expected, observed, 0.0001);
    }

    @Test
    public void testNextSecureIntIAE() {
        try {
            this.randomData.nextSecureInt(4, 3);
            Assert.fail("MathIllegalArgumentException expected");
        } catch (final MathIllegalArgumentException ex) {
            // ignored
        }
    }

    @Test
    @Retry(3)
    public void testNextSecureIntNegativeToPositiveRange() {
        for (int i = 0; i < 5; i++) {
            this.checkNextSecureIntUniform(-3, 5);
            this.checkNextSecureIntUniform(-3, 6);
        }
    }

    @Test
    @Retry(3)
    public void testNextSecureIntNegativeRange() {
        for (int i = 0; i < 5; i++) {
            this.checkNextSecureIntUniform(-7, -4);
            this.checkNextSecureIntUniform(-15, -2);
        }
    }

    @Test
    @Retry(3)
    public void testNextSecureIntPositiveRange() {
        for (int i = 0; i < 5; i++) {
            this.checkNextSecureIntUniform(0, 3);
            this.checkNextSecureIntUniform(2, 12);
        }
    }

    private void checkNextSecureIntUniform(final int min, final int max) {
        final Frequency freq = new Frequency();
        for (int i = 0; i < this.smallSampleSize; i++) {
            final int value = this.randomData.nextSecureInt(min, max);
            Assert.assertTrue("nextInt range", (value >= min) && (value <= max));
            freq.addValue(value);
        }
        final int len = max - min + 1;
        final long[] observed = new long[len];
        for (int i = 0; i < len; i++) {
            observed[i] = freq.getCount(min + i);
        }
        final double[] expected = new double[len];
        for (int i = 0; i < len; i++) {
            expected[i] = 1d / len;
        }

        TestUtils.assertChiSquareAccept(expected, observed, 0.0001);
    }

    /**
     * Make sure that empirical distribution of random Poisson(4)'s has P(X <=
     * 5) close to actual cumulative Poisson probability and that nextPoisson
     * fails when mean is non-positive TODO: replace with statistical test,
     * adding test stat to TestStatistic
     */
    @Test
    public void testNextPoisson() {
        try {
            this.randomData.nextPoisson(0);
            Assert.fail("zero mean -- expecting MathIllegalArgumentException");
        } catch (final MathIllegalArgumentException ex) {
            // ignored
        }
        final Frequency f = new Frequency();
        for (int i = 0; i < this.largeSampleSize; i++) {
            f.addValue(this.randomData.nextPoisson(4.0d));
        }
        final long cumFreq = f.getCount(0) + f.getCount(1) + f.getCount(2)
            + f.getCount(3) + f.getCount(4) + f.getCount(5);
        final long sumFreq = f.getSumFreq();
        final double cumPct = Double.valueOf(cumFreq).doubleValue()
            / Double.valueOf(sumFreq).doubleValue();
        Assert.assertEquals("cum Poisson(4)", cumPct, 0.7851, 0.2);
        try {
            this.randomData.nextPoisson(-1);
            Assert.fail("negative mean supplied -- MathIllegalArgumentException expected");
        } catch (final MathIllegalArgumentException ex) {
            // ignored
        }
        try {
            this.randomData.nextPoisson(0);
            Assert.fail("0 mean supplied -- MathIllegalArgumentException expected");
        } catch (final MathIllegalArgumentException ex) {
            // ignored
        }

    }

    @Test
    public void testNextPoissonConsistency() {

        // Small integral means
        for (int i = 1; i < 100; i++) {
            this.checkNextPoissonConsistency(i);
        }
        // non-integer means
        for (int i = 1; i < 10; i++) {
            this.checkNextPoissonConsistency(this.randomData.nextUniform(1, 1000));
        }
        // large means
        // TODO: When MATH-282 is resolved, s/3000/10000 below
        for (int i = 1; i < 10; i++) {
            this.checkNextPoissonConsistency(this.randomData.nextUniform(1000, 3000));
        }
    }

    /**
     * Verifies that nextPoisson(mean) generates an empirical distribution of values
     * consistent with PoissonDistributionImpl by generating 1000 values, computing a
     * grouped frequency distribution of the observed values and comparing this distribution
     * to the corresponding expected distribution computed using PoissonDistributionImpl.
     * Uses ChiSquare test of goodness of fit to evaluate the null hypothesis that the
     * distributions are the same. If the null hypothesis can be rejected with confidence
     * 1 - alpha, the check fails.
     */
    public void checkNextPoissonConsistency(final double mean) {
        // Generate sample values
        final int sampleSize = 1000; // Number of deviates to generate
        final int minExpectedCount = 7; // Minimum size of expected bin count
        long maxObservedValue = 0;
        final double alpha = 0.001; // Probability of false failure
        final Frequency frequency = new Frequency();
        for (int i = 0; i < sampleSize; i++) {
            final long value = this.randomData.nextPoisson(mean);
            if (value > maxObservedValue) {
                maxObservedValue = value;
            }
            frequency.addValue(value);
        }

        /*
         * Set up bins for chi-square test.
         * Ensure expected counts are all at least minExpectedCount.
         * Start with upper and lower tail bins.
         * Lower bin = [0, lower); Upper bin = [upper, +inf).
         */
        final PoissonDistribution poissonDistribution = new PoissonDistribution(mean);
        int lower = 1;
        while (poissonDistribution.cumulativeProbability(lower - 1) * sampleSize < minExpectedCount) {
            lower++;
        }
        int upper = (int) (5 * mean); // Even for mean = 1, not much mass beyond 5
        while ((1 - poissonDistribution.cumulativeProbability(upper - 1)) * sampleSize < minExpectedCount) {
            upper--;
        }

        // Set bin width for interior bins. For poisson, only need to look at end bins.
        int binWidth = 0;
        boolean widthSufficient = false;
        double lowerBinMass = 0;
        double upperBinMass = 0;
        while (!widthSufficient) {
            binWidth++;
            lowerBinMass = poissonDistribution.cumulativeProbability(lower - 1, lower + binWidth - 1);
            upperBinMass = poissonDistribution.cumulativeProbability(upper - binWidth - 1, upper - 1);
            widthSufficient = MathLib.min(lowerBinMass, upperBinMass) * sampleSize >= minExpectedCount;
        }

        /*
         * Determine interior bin bounds. Bins are
         * [1, lower = binBounds[0]), [lower, binBounds[1]), [binBounds[1], binBounds[2]), ... ,
         * [binBounds[binCount - 2], upper = binBounds[binCount - 1]), [upper, +inf)
         */
        final List<Integer> binBounds = new ArrayList<Integer>();
        binBounds.add(lower);
        int bound = lower + binWidth;
        while (bound < upper - binWidth) {
            binBounds.add(bound);
            bound += binWidth;
        }
        binBounds.add(upper); // The size of bin [binBounds[binCount - 2], upper) satisfies binWidth <= size <
        // 2*binWidth.

        // Compute observed and expected bin counts
        final int binCount = binBounds.size() + 1;
        final long[] observed = new long[binCount];
        final double[] expected = new double[binCount];

        // Bottom bin
        observed[0] = 0;
        for (int i = 0; i < lower; i++) {
            observed[0] += frequency.getCount(i);
        }
        expected[0] = poissonDistribution.cumulativeProbability(lower - 1) * sampleSize;

        // Top bin
        observed[binCount - 1] = 0;
        for (int i = upper; i <= maxObservedValue; i++) {
            observed[binCount - 1] += frequency.getCount(i);
        }
        expected[binCount - 1] = (1 - poissonDistribution.cumulativeProbability(upper - 1)) * sampleSize;

        // Interior bins
        for (int i = 1; i < binCount - 1; i++) {
            observed[i] = 0;
            for (int j = binBounds.get(i - 1); j < binBounds.get(i); j++) {
                observed[i] += frequency.getCount(j);
            } // Expected count is (mass in [binBounds[i-1], binBounds[i])) * sampleSize
            expected[i] = (poissonDistribution.cumulativeProbability(binBounds.get(i) - 1) -
                poissonDistribution.cumulativeProbability(binBounds.get(i - 1) - 1)) * sampleSize;
        }

        // Use chisquare test to verify that generated values are poisson(mean)-distributed
        final ChiSquareTest chiSquareTest = new ChiSquareTest();
        // Fail if we can reject null hypothesis that distributions are the same
        if (chiSquareTest.chiSquareTest(expected, observed, alpha)) {
            final StringBuilder msgBuffer = new StringBuilder();
            final DecimalFormat df = new DecimalFormat("#.##");
            msgBuffer.append("Chisquare test failed for mean = ");
            msgBuffer.append(mean);
            msgBuffer.append(" p-value = ");
            msgBuffer.append(chiSquareTest.chiSquareTest(expected, observed));
            msgBuffer.append(" chisquare statistic = ");
            msgBuffer.append(chiSquareTest.chiSquare(expected, observed));
            msgBuffer.append(". \n");
            msgBuffer.append("bin\t\texpected\tobserved\n");
            for (int i = 0; i < expected.length; i++) {
                msgBuffer.append("[");
                msgBuffer.append(i == 0 ? 1 : binBounds.get(i - 1));
                msgBuffer.append(",");
                msgBuffer.append(i == binBounds.size() ? "inf" : binBounds.get(i));
                msgBuffer.append(")");
                msgBuffer.append("\t\t");
                msgBuffer.append(df.format(expected[i]));
                msgBuffer.append("\t\t");
                msgBuffer.append(observed[i]);
                msgBuffer.append("\n");
            }
            msgBuffer.append("This test can fail randomly due to sampling error with probability ");
            msgBuffer.append(alpha);
            msgBuffer.append(".");
            Assert.fail(msgBuffer.toString());
        }
    }

    /** test dispersion and failure modes for nextHex() */
    @Test
    public void testNextHex() {
        try {
            this.randomData.nextHexString(-1);
            Assert.fail("negative length supplied -- MathIllegalArgumentException expected");
        } catch (final MathIllegalArgumentException ex) {
            // ignored
        }
        try {
            this.randomData.nextHexString(0);
            Assert.fail("zero length supplied -- MathIllegalArgumentException expected");
        } catch (final MathIllegalArgumentException ex) {
            // ignored
        }
        String hexString = this.randomData.nextHexString(3);
        if (hexString.length() != 3) {
            Assert.fail("incorrect length for generated string");
        }
        hexString = this.randomData.nextHexString(1);
        if (hexString.length() != 1) {
            Assert.fail("incorrect length for generated string");
        }
        try {
            hexString = this.randomData.nextHexString(0);
            Assert.fail("zero length requested -- expecting MathIllegalArgumentException");
        } catch (final MathIllegalArgumentException ex) {
            // ignored
        }
        final Frequency f = new Frequency();
        for (int i = 0; i < this.smallSampleSize; i++) {
            hexString = this.randomData.nextHexString(100);
            if (hexString.length() != 100) {
                Assert.fail("incorrect length for generated string");
            }
            for (int j = 0; j < hexString.length(); j++) {
                f.addValue(hexString.substring(j, j + 1));
            }
        }
        final double[] expected = new double[16];
        final long[] observed = new long[16];
        for (int i = 0; i < 16; i++) {
            expected[i] = (double) this.smallSampleSize * 100 / 16;
            observed[i] = f.getCount(this.hex[i]);
        }
        TestUtils.assertChiSquareAccept(expected, observed, 0.001);
    }

    /** test dispersion and failure modes for nextHex() */
    @Test
    @Retry(3)
    public void testNextSecureHex() {
        try {
            this.randomData.nextSecureHexString(-1);
            Assert.fail("negative length -- MathIllegalArgumentException expected");
        } catch (final MathIllegalArgumentException ex) {
            // ignored
        }
        try {
            this.randomData.nextSecureHexString(0);
            Assert.fail("zero length -- MathIllegalArgumentException expected");
        } catch (final MathIllegalArgumentException ex) {
            // ignored
        }
        String hexString = this.randomData.nextSecureHexString(3);
        if (hexString.length() != 3) {
            Assert.fail("incorrect length for generated string");
        }
        hexString = this.randomData.nextSecureHexString(1);
        if (hexString.length() != 1) {
            Assert.fail("incorrect length for generated string");
        }
        try {
            hexString = this.randomData.nextSecureHexString(0);
            Assert.fail("zero length requested -- expecting MathIllegalArgumentException");
        } catch (final MathIllegalArgumentException ex) {
            // ignored
        }
        final Frequency f = new Frequency();
        for (int i = 0; i < this.smallSampleSize; i++) {
            hexString = this.randomData.nextSecureHexString(100);
            if (hexString.length() != 100) {
                Assert.fail("incorrect length for generated string");
            }
            for (int j = 0; j < hexString.length(); j++) {
                f.addValue(hexString.substring(j, j + 1));
            }
        }
        final double[] expected = new double[16];
        final long[] observed = new long[16];
        for (int i = 0; i < 16; i++) {
            expected[i] = (double) this.smallSampleSize * 100 / 16;
            observed[i] = f.getCount(this.hex[i]);
        }
        TestUtils.assertChiSquareAccept(expected, observed, 0.001);
    }

    @Test
    public void testNextUniformIAE() {
        try {
            this.randomData.nextUniform(4, 3);
            Assert.fail("MathIllegalArgumentException expected");
        } catch (final MathIllegalArgumentException ex) {
            // ignored
        }
        try {
            this.randomData.nextUniform(0, Double.POSITIVE_INFINITY);
            Assert.fail("MathIllegalArgumentException expected");
        } catch (final MathIllegalArgumentException ex) {
            // ignored
        }
        try {
            this.randomData.nextUniform(Double.NEGATIVE_INFINITY, 0);
            Assert.fail("MathIllegalArgumentException expected");
        } catch (final MathIllegalArgumentException ex) {
            // ignored
        }
        try {
            this.randomData.nextUniform(0, Double.NaN);
            Assert.fail("MathIllegalArgumentException expected");
        } catch (final MathIllegalArgumentException ex) {
            // ignored
        }
        try {
            this.randomData.nextUniform(Double.NaN, 0);
            Assert.fail("MathIllegalArgumentException expected");
        } catch (final MathIllegalArgumentException ex) {
            // ignored
        }
    }

    @Test
    public void testNextUniformUniformPositiveBounds() {
        for (int i = 0; i < 5; i++) {
            this.checkNextUniformUniform(0, 10);
        }
    }

    @Test
    public void testNextUniformUniformNegativeToPositiveBounds() {
        for (int i = 0; i < 5; i++) {
            this.checkNextUniformUniform(-3, 5);
        }
    }

    @Test
    public void testNextUniformUniformNegaiveBounds() {
        for (int i = 0; i < 5; i++) {
            this.checkNextUniformUniform(-7, -3);
        }
    }

    @Test
    public void testNextUniformUniformMaximalInterval() {
        for (int i = 0; i < 5; i++) {
            this.checkNextUniformUniform(-Double.MAX_VALUE, Double.MAX_VALUE);
        }
    }

    private void checkNextUniformUniform(final double min, final double max) {
        // Set up bin bounds - min, binBound[0], ..., binBound[binCount-2], max
        final int binCount = 5;
        final double binSize = max / binCount - min / binCount; // Prevent overflow in extreme value case
        final double[] binBounds = new double[binCount - 1];
        binBounds[0] = min + binSize;
        for (int i = 1; i < binCount - 1; i++) {
            binBounds[i] = binBounds[i - 1] + binSize; // + instead of * to avoid overflow in extreme case
        }

        final Frequency freq = new Frequency();
        for (int i = 0; i < this.smallSampleSize; i++) {
            final double value = this.randomData.nextUniform(min, max);
            Assert.assertTrue("nextUniform range", (value > min) && (value < max));
            // Find bin
            int j = 0;
            while (j < binCount - 1 && value > binBounds[j]) {
                j++;
            }
            freq.addValue(j);
        }

        final long[] observed = new long[binCount];
        for (int i = 0; i < binCount; i++) {
            observed[i] = freq.getCount(i);
        }
        final double[] expected = new double[binCount];
        for (int i = 0; i < binCount; i++) {
            expected[i] = 1d / binCount;
        }

        TestUtils.assertChiSquareAccept(expected, observed, 0.01);
    }

    /** test exclusive endpoints of nextUniform **/
    @Test
    public void testNextUniformExclusiveEndpoints() {
        for (int i = 0; i < 1000; i++) {
            final double u = this.randomData.nextUniform(0.99, 1);
            Assert.assertTrue(u > 0.99 && u < 1);
        }
    }

    /** test failure modes and distribution of nextGaussian() */
    @Test
    public void testNextGaussian() {
        try {
            this.randomData.nextGaussian(0, 0);
            Assert.fail("zero sigma -- MathIllegalArgumentException expected");
        } catch (final MathIllegalArgumentException ex) {
            // ignored
        }
        final SummaryStatistics u = new SummaryStatistics();
        for (int i = 0; i < this.largeSampleSize; i++) {
            u.addValue(this.randomData.nextGaussian(0, 1));
        }
        final double xbar = u.getMean();
        final double s = u.getStandardDeviation();
        final double n = u.getN();
        /*
         * t-test at .001-level TODO: replace with externalized t-test, with
         * test statistic defined in TestStatistic
         */
        Assert.assertTrue(MathLib.abs(xbar) / (s / MathLib.sqrt(n)) < 3.29);
    }

    /** test failure modes and distribution of nextExponential() */
    @Test
    public void testNextExponential() {
        try {
            this.randomData.nextExponential(-1);
            Assert.fail("negative mean -- expecting MathIllegalArgumentException");
        } catch (final MathIllegalArgumentException ex) {
            // ignored
        }
        try {
            this.randomData.nextExponential(0);
            Assert.fail("zero mean -- expecting MathIllegalArgumentException");
        } catch (final MathIllegalArgumentException ex) {
            // ignored
        }
        long cumFreq = 0;
        double v = 0;
        for (int i = 0; i < this.largeSampleSize; i++) {
            v = this.randomData.nextExponential(1);
            Assert.assertTrue("exponential deviate postive", v > 0);
            if (v < 2) {
                cumFreq++;
            }
        }
        /*
         * TODO: Replace with a statistical test, with statistic added to
         * TestStatistic. Check below compares observed cumulative distribution
         * evaluated at 2 with exponential CDF
         */
        Assert.assertEquals("exponential cumulative distribution", (double) cumFreq
            / (double) this.largeSampleSize, 0.8646647167633873, .2);

        /**
         * Proposal on improving the test of generating exponentials
         */
        double[] quartiles;
        long[] counts;

        // Mean 1
        quartiles = TestUtils.getDistributionQuartiles(new ExponentialDistribution(1));
        counts = new long[4];
        this.randomData.reSeed(1000);
        for (int i = 0; i < 1000; i++) {
            final double value = this.randomData.nextExponential(1);
            TestUtils.updateCounts(value, counts, quartiles);
        }
        TestUtils.assertChiSquareAccept(this.expected, counts, 0.001);

        // Mean 5
        quartiles = TestUtils.getDistributionQuartiles(new ExponentialDistribution(5));
        counts = new long[4];
        this.randomData.reSeed(1000);
        for (int i = 0; i < 1000; i++) {
            final double value = this.randomData.nextExponential(5);
            TestUtils.updateCounts(value, counts, quartiles);
        }
        TestUtils.assertChiSquareAccept(this.expected, counts, 0.001);
    }

    /** test reseeding, algorithm/provider games */
    @Test
    public void testConfig() {
        this.randomData.reSeed(1000);
        final double v = this.randomData.nextUniform(0, 1);
        this.randomData.reSeed();
        Assert.assertTrue("different seeds", Math
            .abs(v - this.randomData.nextUniform(0, 1)) > 10E-12);
        this.randomData.reSeed(1000);
        Assert.assertEquals("same seeds", v, this.randomData.nextUniform(0, 1), 10E-12);
        this.randomData.reSeedSecure(1000);
        final String hex = this.randomData.nextSecureHexString(40);
        this.randomData.reSeedSecure();
        Assert.assertTrue("different seeds", !hex.equals(this.randomData
            .nextSecureHexString(40)));
        this.randomData.reSeedSecure(1000);
        Assert.assertTrue("same seeds", !hex
            .equals(this.randomData.nextSecureHexString(40)));

        /*
         * remove this test back soon, since it takes about 4 seconds
         * try { randomData.setSecureAlgorithm("SHA1PRNG","SUN"); } catch
         * (NoSuchProviderException ex) { ; } Assert.assertTrue("different seeds",
         * !hex.equals(randomData.nextSecureHexString(40))); try {
         * randomData.setSecureAlgorithm("NOSUCHTHING","SUN");
         * Assert.fail("expecting NoSuchAlgorithmException"); } catch
         * (NoSuchProviderException ex) { ; } catch (NoSuchAlgorithmException
         * ex) { ; }
         * try { randomData.setSecureAlgorithm("SHA1PRNG","NOSUCHPROVIDER");
         * Assert.fail("expecting NoSuchProviderException"); } catch
         * (NoSuchProviderException ex) { ; }
         */

        // test reseeding without first using the generators
        RandomDataGenerator rd = new RandomDataGenerator();
        rd.reSeed(100);
        rd.nextLong(1, 2);
        RandomDataGenerator rd2 = new RandomDataGenerator();
        rd2.reSeedSecure(2000);
        rd2.nextSecureLong(1, 2);
        rd = new RandomDataGenerator();
        rd.reSeed();
        rd.nextLong(1, 2);
        rd2 = new RandomDataGenerator();
        rd2.reSeedSecure();
        rd2.nextSecureLong(1, 2);
    }

    /** tests for nextSample() sampling from Collection */
    @Test
    public void testNextSample() {
        final Object[][] c = { { "0", "1" }, { "0", "2" }, { "0", "3" },
            { "0", "4" }, { "1", "2" }, { "1", "3" }, { "1", "4" },
            { "2", "3" }, { "2", "4" }, { "3", "4" } };
        final long[] observed = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        final double[] expected = { 100, 100, 100, 100, 100, 100, 100, 100, 100, 100 };

        final HashSet<Object> cPop = new HashSet<Object>(); // {0,1,2,3,4}
        for (int i = 0; i < 5; i++) {
            cPop.add(Integer.toString(i));
        }

        final Object[] sets = new Object[10]; // 2-sets from 5
        for (int i = 0; i < 10; i++) {
            final HashSet<Object> hs = new HashSet<Object>();
            hs.add(c[i][0]);
            hs.add(c[i][1]);
            sets[i] = hs;
        }

        for (int i = 0; i < 1000; i++) {
            final Object[] cSamp = this.randomData.nextSample(cPop, 2);
            observed[this.findSample(sets, cSamp)]++;
        }

        /*
         * Use ChiSquare dist with df = 10-1 = 9, alpha = .001 Change to 21.67
         * for alpha = .01
         */
        Assert.assertTrue("chi-square test -- will fail about 1 in 1000 times",
            this.testStatistic.chiSquare(expected, observed) < 27.88);

        // Make sure sample of size = size of collection returns same collection
        HashSet<Object> hs = new HashSet<Object>();
        hs.add("one");
        Object[] one = this.randomData.nextSample(hs, 1);
        final String oneString = (String) one[0];
        if ((one.length != 1) || !oneString.equals("one")) {
            Assert.fail("bad sample for set size = 1, sample size = 1");
        }

        // Make sure we fail for sample size > collection size
        try {
            one = this.randomData.nextSample(hs, 2);
            Assert.fail("sample size > set size, expecting MathIllegalArgumentException");
        } catch (final MathIllegalArgumentException ex) {
            // ignored
        }

        // Make sure we fail for empty collection
        try {
            hs = new HashSet<Object>();
            one = this.randomData.nextSample(hs, 0);
            Assert.fail("n = k = 0, expecting MathIllegalArgumentException");
        } catch (final MathIllegalArgumentException ex) {
            // ignored
        }
    }

    @SuppressWarnings("unchecked")
    private int findSample(final Object[] u, final Object[] samp) {
        for (int i = 0; i < u.length; i++) {
            final HashSet<Object> set = (HashSet<Object>) u[i];
            final HashSet<Object> sampSet = new HashSet<Object>();
            for (final Object element : samp) {
                sampSet.add(element);
            }
            if (set.equals(sampSet)) {
                return i;
            }
        }
        Assert.fail("sample not found:{" + samp[0] + "," + samp[1] + "}");
        return -1;
    }

    /** tests for nextPermutation */
    @Test
    public void testNextPermutation() {
        final int[][] p = { { 0, 1, 2 }, { 0, 2, 1 }, { 1, 0, 2 }, { 1, 2, 0 },
            { 2, 0, 1 }, { 2, 1, 0 } };
        final long[] observed = { 0, 0, 0, 0, 0, 0 };
        final double[] expected = { 100, 100, 100, 100, 100, 100 };

        for (int i = 0; i < 600; i++) {
            final int[] perm = this.randomData.nextPermutation(3, 3);
            observed[this.findPerm(p, perm)]++;
        }

        final String[] labels = { "{0, 1, 2}", "{ 0, 2, 1 }", "{ 1, 0, 2 }",
            "{ 1, 2, 0 }", "{ 2, 0, 1 }", "{ 2, 1, 0 }" };
        TestUtils.assertChiSquareAccept(labels, expected, observed, 0.001);

        // Check size = 1 boundary case
        int[] perm = this.randomData.nextPermutation(1, 1);
        if ((perm.length != 1) || (perm[0] != 0)) {
            Assert.fail("bad permutation for n = 1, sample k = 1");

            // Make sure we fail for k size > n
            try {
                perm = this.randomData.nextPermutation(2, 3);
                Assert.fail("permutation k > n, expecting MathIllegalArgumentException");
            } catch (final MathIllegalArgumentException ex) {
                // ignored
            }

            // Make sure we fail for n = 0
            try {
                perm = this.randomData.nextPermutation(0, 0);
                Assert.fail("permutation k = n = 0, expecting MathIllegalArgumentException");
            } catch (final MathIllegalArgumentException ex) {
                // ignored
            }

            // Make sure we fail for k < n < 0
            try {
                perm = this.randomData.nextPermutation(-1, -3);
                Assert.fail("permutation k < n < 0, expecting MathIllegalArgumentException");
            } catch (final MathIllegalArgumentException ex) {
                // ignored
            }

        }
    }

    // Disable until we have equals
    // public void testSerial() {
    // Assert.assertEquals(randomData, TestUtils.serializeAndRecover(randomData));
    // }

    private int findPerm(final int[][] p, final int[] samp) {
        for (int i = 0; i < p.length; i++) {
            boolean good = true;
            for (int j = 0; j < samp.length; j++) {
                if (samp[j] != p[i][j]) {
                    good = false;
                }
            }
            if (good) {
                return i;
            }
        }
        Assert.fail("permutation not found");
        return -1;
    }

    @Test
    public void testNextBeta() {
        final double[] quartiles = TestUtils.getDistributionQuartiles(new BetaDistribution(2, 5));
        final long[] counts = new long[4];
        this.randomData.reSeed(1000);
        for (int i = 0; i < 1000; i++) {
            final double value = this.randomData.nextBeta(2, 5);
            TestUtils.updateCounts(value, counts, quartiles);
        }
        TestUtils.assertChiSquareAccept(this.expected, counts, 0.001);
    }

    @Test
    public void testNextCauchy() {
        final double[] quartiles = TestUtils.getDistributionQuartiles(new CauchyDistribution(1.2, 2.1));
        final long[] counts = new long[4];
        this.randomData.reSeed(1000);
        for (int i = 0; i < 1000; i++) {
            final double value = this.randomData.nextCauchy(1.2, 2.1);
            TestUtils.updateCounts(value, counts, quartiles);
        }
        TestUtils.assertChiSquareAccept(this.expected, counts, 0.001);
    }

    @Test
    public void testNextChiSquare() {
        final double[] quartiles = TestUtils.getDistributionQuartiles(new ChiSquaredDistribution(12));
        final long[] counts = new long[4];
        this.randomData.reSeed(1000);
        for (int i = 0; i < 1000; i++) {
            final double value = this.randomData.nextChiSquare(12);
            TestUtils.updateCounts(value, counts, quartiles);
        }
        TestUtils.assertChiSquareAccept(this.expected, counts, 0.001);
    }

    @Test
    public void testNextF() {
        final double[] quartiles = TestUtils.getDistributionQuartiles(new FDistribution(12, 5));
        final long[] counts = new long[4];
        this.randomData.reSeed(1000);
        for (int i = 0; i < 1000; i++) {
            final double value = this.randomData.nextF(12, 5);
            TestUtils.updateCounts(value, counts, quartiles);
        }
        TestUtils.assertChiSquareAccept(this.expected, counts, 0.001);
    }

    @Test
    public void testNextGamma() {
        double[] quartiles;
        long[] counts;

        // Tests shape > 1, one case in the rejection sampling
        quartiles = TestUtils.getDistributionQuartiles(new GammaDistribution(4, 2));
        counts = new long[4];
        this.randomData.reSeed(1000);
        for (int i = 0; i < 1000; i++) {
            final double value = this.randomData.nextGamma(4, 2);
            TestUtils.updateCounts(value, counts, quartiles);
        }
        TestUtils.assertChiSquareAccept(this.expected, counts, 0.001);

        // Tests shape <= 1, another case in the rejection sampling
        quartiles = TestUtils.getDistributionQuartiles(new GammaDistribution(0.3, 3));
        counts = new long[4];
        this.randomData.reSeed(1000);
        for (int i = 0; i < 1000; i++) {
            final double value = this.randomData.nextGamma(0.3, 3);
            TestUtils.updateCounts(value, counts, quartiles);
        }
        TestUtils.assertChiSquareAccept(this.expected, counts, 0.001);
    }

    @Test
    public void testNextT() {
        final double[] quartiles = TestUtils.getDistributionQuartiles(new TDistribution(10));
        final long[] counts = new long[4];
        this.randomData.reSeed(1000);
        for (int i = 0; i < 1000; i++) {
            final double value = this.randomData.nextT(10);
            TestUtils.updateCounts(value, counts, quartiles);
        }
        TestUtils.assertChiSquareAccept(this.expected, counts, 0.001);
    }

    @Test
    public void testNextWeibull() {
        final double[] quartiles = TestUtils.getDistributionQuartiles(new WeibullDistribution(1.2, 2.1));
        final long[] counts = new long[4];
        this.randomData.reSeed(1000);
        for (int i = 0; i < 1000; i++) {
            final double value = this.randomData.nextWeibull(1.2, 2.1);
            TestUtils.updateCounts(value, counts, quartiles);
        }
        TestUtils.assertChiSquareAccept(this.expected, counts, 0.001);
    }

    @Test
    public void testNextBinomial() {
        final BinomialDistributionTest testInstance = new BinomialDistributionTest();
        final int[] densityPoints = testInstance.makeDensityTestPoints();
        final double[] densityValues = testInstance.makeDensityTestValues();
        final int sampleSize = 1000;
        final int length = TestUtils.eliminateZeroMassPoints(densityPoints, densityValues);
        final BinomialDistribution distribution = (BinomialDistribution) testInstance.makeDistribution();
        final double[] expectedCounts = new double[length];
        final long[] observedCounts = new long[length];
        for (int i = 0; i < length; i++) {
            expectedCounts[i] = sampleSize * densityValues[i];
        }
        this.randomData.reSeed(1000);
        for (int i = 0; i < sampleSize; i++) {
            final int value = this.randomData.nextBinomial(distribution.getNumberOfTrials(),
                distribution.getProbabilityOfSuccess());
            for (int j = 0; j < length; j++) {
                if (value == densityPoints[j]) {
                    observedCounts[j]++;
                }
            }
        }
        TestUtils.assertChiSquareAccept(densityPoints, expectedCounts, observedCounts, .001);
    }

    @Test
    public void testNextHypergeometric() {
        final HypergeometricDistributionTest testInstance = new HypergeometricDistributionTest();
        final int[] densityPoints = testInstance.makeDensityTestPoints();
        final double[] densityValues = testInstance.makeDensityTestValues();
        final int sampleSize = 1000;
        final int length = TestUtils.eliminateZeroMassPoints(densityPoints, densityValues);
        final HypergeometricDistribution distribution = (HypergeometricDistribution) testInstance.makeDistribution();
        final double[] expectedCounts = new double[length];
        final long[] observedCounts = new long[length];
        for (int i = 0; i < length; i++) {
            expectedCounts[i] = sampleSize * densityValues[i];
        }
        this.randomData.reSeed(1000);
        for (int i = 0; i < sampleSize; i++) {
            final int value = this.randomData.nextHypergeometric(distribution.getPopulationSize(),
                distribution.getNumberOfSuccesses(), distribution.getSampleSize());
            for (int j = 0; j < length; j++) {
                if (value == densityPoints[j]) {
                    observedCounts[j]++;
                }
            }
        }
        TestUtils.assertChiSquareAccept(densityPoints, expectedCounts, observedCounts, .001);
    }

    @Test
    public void testNextPascal() {
        final PascalDistributionTest testInstance = new PascalDistributionTest();
        final int[] densityPoints = testInstance.makeDensityTestPoints();
        final double[] densityValues = testInstance.makeDensityTestValues();
        final int sampleSize = 1000;
        final int length = TestUtils.eliminateZeroMassPoints(densityPoints, densityValues);
        final PascalDistribution distribution = (PascalDistribution) testInstance.makeDistribution();
        final double[] expectedCounts = new double[length];
        final long[] observedCounts = new long[length];
        for (int i = 0; i < length; i++) {
            expectedCounts[i] = sampleSize * densityValues[i];
        }
        this.randomData.reSeed(1000);
        for (int i = 0; i < sampleSize; i++) {
            final int value = this.randomData.nextPascal(distribution.getNumberOfSuccesses(),
                distribution.getProbabilityOfSuccess());
            for (int j = 0; j < length; j++) {
                if (value == densityPoints[j]) {
                    observedCounts[j]++;
                }
            }
        }
        TestUtils.assertChiSquareAccept(densityPoints, expectedCounts, observedCounts, .001);
    }

    @Test
    public void testNextZipf() {
        final ZipfDistributionTest testInstance = new ZipfDistributionTest();
        final int[] densityPoints = testInstance.makeDensityTestPoints();
        final double[] densityValues = testInstance.makeDensityTestValues();
        final int sampleSize = 1000;
        final int length = TestUtils.eliminateZeroMassPoints(densityPoints, densityValues);
        final ZipfDistribution distribution = (ZipfDistribution) testInstance.makeDistribution();
        final double[] expectedCounts = new double[length];
        final long[] observedCounts = new long[length];
        for (int i = 0; i < length; i++) {
            expectedCounts[i] = sampleSize * densityValues[i];
        }
        this.randomData.reSeed(1000);
        for (int i = 0; i < sampleSize; i++) {
            final int value = this.randomData.nextZipf(distribution.getNumberOfElements(), distribution.getExponent());
            for (int j = 0; j < length; j++) {
                if (value == densityPoints[j]) {
                    observedCounts[j]++;
                }
            }
        }
        TestUtils.assertChiSquareAccept(densityPoints, expectedCounts, observedCounts, .001);
    }

    @Test
    /**
     * MATH-720
     */
    public void testReseed() {
        final PoissonDistribution x = new PoissonDistribution(3.0);
        x.reseedRandomGenerator(0);
        final double u = x.sample();
        final PoissonDistribution y = new PoissonDistribution(3.0);
        y.reseedRandomGenerator(0);
        Assert.assertEquals(u, y.sample(), 0);
    }

}
