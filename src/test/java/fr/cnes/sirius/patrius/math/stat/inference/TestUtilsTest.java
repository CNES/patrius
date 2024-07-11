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
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.stat.inference;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.NotPositiveException;
import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;
import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;
import fr.cnes.sirius.patrius.math.stat.descriptive.SummaryStatistics;

/**
 * Test cases for the TestUtils class.
 * 
 * @version $Id: TestUtilsTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class TestUtilsTest {

    @Test
    public void testChiSquare() {

        // Target values computed using R version 1.8.1
        // Some assembly required ;-)
        // Use sum((obs - exp)^2/exp) for the chi-square statistic and
        // 1 - pchisq(sum((obs - exp)^2/exp), length(obs) - 1) for the p-value

        final long[] observed = { 10, 9, 11 };
        final double[] expected = { 10, 10, 10 };
        Assert.assertEquals("chi-square statistic", 0.2, TestUtils.chiSquare(expected, observed), 10E-12);
        Assert.assertEquals("chi-square p-value", 0.904837418036, TestUtils.chiSquareTest(expected, observed), 1E-10);

        final long[] observed1 = { 500, 623, 72, 70, 31 };
        final double[] expected1 = { 485, 541, 82, 61, 37 };
        Assert.assertEquals("chi-square test statistic", 9.023307936427388, TestUtils.chiSquare(expected1, observed1),
            1E-10);
        Assert.assertEquals("chi-square p-value", 0.06051952647453607, TestUtils.chiSquareTest(expected1, observed1),
            1E-9);
        Assert.assertTrue("chi-square test reject", TestUtils.chiSquareTest(expected1, observed1, 0.07));
        Assert.assertTrue("chi-square test accept", !TestUtils.chiSquareTest(expected1, observed1, 0.05));

        try {
            TestUtils.chiSquareTest(expected1, observed1, 95);
            Assert.fail("alpha out of range, OutOfRangeException expected");
        } catch (final OutOfRangeException ex) {
            // expected
        }

        final long[] tooShortObs = { 0 };
        final double[] tooShortEx = { 1 };
        try {
            TestUtils.chiSquare(tooShortEx, tooShortObs);
            Assert.fail("arguments too short, DimensionMismatchException expected");
        } catch (final DimensionMismatchException ex) {
            // expected
        }

        // unmatched arrays
        final long[] unMatchedObs = { 0, 1, 2, 3 };
        final double[] unMatchedEx = { 1, 1, 2 };
        try {
            TestUtils.chiSquare(unMatchedEx, unMatchedObs);
            Assert.fail("arrays have different lengths, DimensionMismatchException expected");
        } catch (final DimensionMismatchException ex) {
            // expected
        }

        // 0 expected count
        expected[0] = 0;
        try {
            TestUtils.chiSquareTest(expected, observed, .01);
            Assert.fail("bad expected count, NotStrictlyPositiveException expected");
        } catch (final NotStrictlyPositiveException ex) {
            // expected
        }

        // negative observed count
        expected[0] = 1;
        observed[0] = -1;
        try {
            TestUtils.chiSquareTest(expected, observed, .01);
            Assert.fail("bad expected count, NotPositiveException expected");
        } catch (final NotPositiveException ex) {
            // expected
        }

    }

    @Test
    public void testChiSquareIndependence() {

        // Target values computed using R version 1.8.1

        final long[][] counts = { { 40, 22, 43 }, { 91, 21, 28 }, { 60, 10, 22 } };
        Assert.assertEquals("chi-square test statistic", 22.709027688, TestUtils.chiSquare(counts), 1E-9);
        Assert.assertEquals("chi-square p-value", 0.000144751460134, TestUtils.chiSquareTest(counts), 1E-9);
        Assert.assertTrue("chi-square test reject", TestUtils.chiSquareTest(counts, 0.0002));
        Assert.assertTrue("chi-square test accept", !TestUtils.chiSquareTest(counts, 0.0001));

        final long[][] counts2 = { { 10, 15 }, { 30, 40 }, { 60, 90 } };
        Assert.assertEquals("chi-square test statistic", 0.168965517241, TestUtils.chiSquare(counts2), 1E-9);
        Assert.assertEquals("chi-square p-value", 0.918987499852, TestUtils.chiSquareTest(counts2), 1E-9);
        Assert.assertTrue("chi-square test accept", !TestUtils.chiSquareTest(counts2, 0.1));

        // ragged input array
        final long[][] counts3 = { { 40, 22, 43 }, { 91, 21, 28 }, { 60, 10 } };
        try {
            TestUtils.chiSquare(counts3);
            Assert.fail("Expecting DimensionMismatchException");
        } catch (final DimensionMismatchException ex) {
            // expected
        }

        // insufficient data
        final long[][] counts4 = { { 40, 22, 43 } };
        try {
            TestUtils.chiSquare(counts4);
            Assert.fail("Expecting DimensionMismatchException");
        } catch (final DimensionMismatchException ex) {
            // expected
        }
        final long[][] counts5 = { { 40 }, { 40 }, { 30 }, { 10 } };
        try {
            TestUtils.chiSquare(counts5);
            Assert.fail("Expecting DimensionMismatchException");
        } catch (final DimensionMismatchException ex) {
            // expected
        }

        // negative counts
        final long[][] counts6 = { { 10, -2 }, { 30, 40 }, { 60, 90 } };
        try {
            TestUtils.chiSquare(counts6);
            Assert.fail("Expecting NotPositiveException");
        } catch (final NotPositiveException ex) {
            // expected
        }

        // bad alpha
        try {
            TestUtils.chiSquareTest(counts, 0);
            Assert.fail("Expecting OutOfRangeException");
        } catch (final OutOfRangeException ex) {
            // expected
        }
    }

    @Test
    public void testChiSquareLargeTestStatistic() {
        final double[] exp = new double[] {
            3389119.5, 649136.6, 285745.4, 25357364.76, 11291189.78, 543628.0,
            232921.0, 437665.75
        };

        final long[] obs = new long[] {
            2372383, 584222, 257170, 17750155, 7903832, 489265, 209628, 393899
        };
        final fr.cnes.sirius.patrius.math.stat.inference.ChiSquareTest csti =
            new fr.cnes.sirius.patrius.math.stat.inference.ChiSquareTest();
        final double cst = csti.chiSquareTest(exp, obs);
        Assert.assertEquals("chi-square p-value", 0.0, cst, 1E-3);
        Assert.assertEquals("chi-square test statistic",
            114875.90421929007, TestUtils.chiSquare(exp, obs), 1E-9);
    }

    /** Contingency table containing zeros - PR # 32531 */
    @Test
    public void testChiSquareZeroCount() {
        // Target values computed using R version 1.8.1
        final long[][] counts = { { 40, 0, 4 }, { 91, 1, 2 }, { 60, 2, 0 } };
        Assert.assertEquals("chi-square test statistic", 9.67444662263,
            TestUtils.chiSquare(counts), 1E-9);
        Assert.assertEquals("chi-square p-value", 0.0462835770603,
            TestUtils.chiSquareTest(counts), 1E-9);
    }

    private final double[] tooShortObs = { 1.0 };
    private final double[] emptyObs = {};
    private final SummaryStatistics emptyStats = new SummaryStatistics();

    @Test
    public void testOneSampleT() {
        final double[] observed =
        { 93.0, 103.0, 95.0, 101.0, 91.0, 105.0, 96.0, 94.0, 101.0, 88.0, 98.0, 94.0, 101.0, 92.0, 95.0 };
        final double mu = 100.0;
        SummaryStatistics sampleStats = null;
        sampleStats = new SummaryStatistics();
        for (final double element : observed) {
            sampleStats.addValue(element);
        }

        // Target comparison values computed using R version 1.8.1 (Linux version)
        Assert.assertEquals("t statistic", -2.81976445346,
            TestUtils.t(mu, observed), 10E-10);
        Assert.assertEquals("t statistic", -2.81976445346,
            TestUtils.t(mu, sampleStats), 10E-10);
        Assert.assertEquals("p value", 0.0136390585873,
            TestUtils.tTest(mu, observed), 10E-10);
        Assert.assertEquals("p value", 0.0136390585873,
            TestUtils.tTest(mu, sampleStats), 10E-10);

        try {
            TestUtils.t(mu, (double[]) null);
            Assert.fail("arguments too short, NullArgumentException expected");
        } catch (final NullArgumentException ex) {
            // expected
        }

        try {
            TestUtils.t(mu, (SummaryStatistics) null);
            Assert.fail("arguments too short, NullArgumentException expected");
        } catch (final NullArgumentException ex) {
            // expected
        }

        try {
            TestUtils.t(mu, this.emptyObs);
            Assert.fail("arguments too short, NumberIsTooSmallException expected");
        } catch (final NumberIsTooSmallException ex) {
            // expected
        }

        try {
            TestUtils.t(mu, this.emptyStats);
            Assert.fail("arguments too short, NumberIsTooSmallException expected");
        } catch (final NumberIsTooSmallException ex) {
            // expected
        }

        try {
            TestUtils.t(mu, this.tooShortObs);
            Assert.fail("insufficient data to compute t statistic, NumberIsTooSmallException expected");
        } catch (final NumberIsTooSmallException ex) {
            // expected
        }
        try {
            TestUtils.tTest(mu, this.tooShortObs);
            Assert.fail("insufficient data to perform t test, NumberIsTooSmallException expected");
        } catch (final NumberIsTooSmallException ex) {
            // expected
        }

        try {
            TestUtils.t(mu, (SummaryStatistics) null);
            Assert.fail("insufficient data to compute t statistic, NullArgumentException expected");
        } catch (final NullArgumentException ex) {
            // expected
        }
        try {
            TestUtils.tTest(mu, (SummaryStatistics) null);
            Assert.fail("insufficient data to perform t test, NullArgumentException expected");
        } catch (final NullArgumentException ex) {
            // expected
        }
    }

    @Test
    public void testOneSampleTTest() {
        final double[] oneSidedP =
        { 2d, 0d, 6d, 6d, 3d, 3d, 2d, 3d, -6d, 6d, 6d, 6d, 3d, 0d, 1d, 1d, 0d, 2d, 3d, 3d };
        final SummaryStatistics oneSidedPStats = new SummaryStatistics();
        for (final double element : oneSidedP) {
            oneSidedPStats.addValue(element);
        }
        // Target comparison values computed using R version 1.8.1 (Linux version)
        Assert.assertEquals("one sample t stat", 3.86485535541,
            TestUtils.t(0d, oneSidedP), 10E-10);
        Assert.assertEquals("one sample t stat", 3.86485535541,
            TestUtils.t(0d, oneSidedPStats), 1E-10);
        Assert.assertEquals("one sample p value", 0.000521637019637,
            TestUtils.tTest(0d, oneSidedP) / 2d, 10E-10);
        Assert.assertEquals("one sample p value", 0.000521637019637,
            TestUtils.tTest(0d, oneSidedPStats) / 2d, 10E-5);
        Assert.assertTrue("one sample t-test reject", TestUtils.tTest(0d, oneSidedP, 0.01));
        Assert.assertTrue("one sample t-test reject", TestUtils.tTest(0d, oneSidedPStats, 0.01));
        Assert.assertTrue("one sample t-test accept", !TestUtils.tTest(0d, oneSidedP, 0.0001));
        Assert.assertTrue("one sample t-test accept", !TestUtils.tTest(0d, oneSidedPStats, 0.0001));

        try {
            TestUtils.tTest(0d, oneSidedP, 95);
            Assert.fail("alpha out of range, OutOfRangeException expected");
        } catch (final OutOfRangeException ex) {
            // expected
        }

        try {
            TestUtils.tTest(0d, oneSidedPStats, 95);
            Assert.fail("alpha out of range, OutOfRangeException expected");
        } catch (final OutOfRangeException ex) {
            // expected
        }

    }

    @Test
    public void testTwoSampleTHeterscedastic() {
        final double[] sample1 = { 7d, -4d, 18d, 17d, -3d, -5d, 1d, 10d, 11d, -2d };
        final double[] sample2 = { -1d, 12d, -1d, -3d, 3d, -5d, 5d, 2d, -11d, -1d, -3d };
        final SummaryStatistics sampleStats1 = new SummaryStatistics();
        for (final double element : sample1) {
            sampleStats1.addValue(element);
        }
        final SummaryStatistics sampleStats2 = new SummaryStatistics();
        for (final double element : sample2) {
            sampleStats2.addValue(element);
        }

        // Target comparison values computed using R version 1.8.1 (Linux version)
        Assert.assertEquals("two sample heteroscedastic t stat", 1.60371728768,
            TestUtils.t(sample1, sample2), 1E-10);
        Assert.assertEquals("two sample heteroscedastic t stat", 1.60371728768,
            TestUtils.t(sampleStats1, sampleStats2), 1E-10);
        Assert.assertEquals("two sample heteroscedastic p value", 0.128839369622,
            TestUtils.tTest(sample1, sample2), 1E-10);
        Assert.assertEquals("two sample heteroscedastic p value", 0.128839369622,
            TestUtils.tTest(sampleStats1, sampleStats2), 1E-10);
        Assert.assertTrue("two sample heteroscedastic t-test reject",
            TestUtils.tTest(sample1, sample2, 0.2));
        Assert.assertTrue("two sample heteroscedastic t-test reject",
            TestUtils.tTest(sampleStats1, sampleStats2, 0.2));
        Assert.assertTrue("two sample heteroscedastic t-test accept",
            !TestUtils.tTest(sample1, sample2, 0.1));
        Assert.assertTrue("two sample heteroscedastic t-test accept",
            !TestUtils.tTest(sampleStats1, sampleStats2, 0.1));

        try {
            TestUtils.tTest(sample1, sample2, .95);
            Assert.fail("alpha out of range, OutOfRangeException expected");
        } catch (final OutOfRangeException ex) {
            // expected
        }

        try {
            TestUtils.tTest(sampleStats1, sampleStats2, .95);
            Assert.fail("alpha out of range, OutOfRangeException expected");
        } catch (final OutOfRangeException ex) {
            // expected
        }

        try {
            TestUtils.tTest(sample1, this.tooShortObs, .01);
            Assert.fail("insufficient data, NumberIsTooSmallException expected");
        } catch (final NumberIsTooSmallException ex) {
            // expected
        }

        try {
            TestUtils.tTest(sampleStats1, (SummaryStatistics) null, .01);
            Assert.fail("insufficient data, NullArgumentException expected");
        } catch (final NullArgumentException ex) {
            // expected
        }

        try {
            TestUtils.tTest(sample1, this.tooShortObs);
            Assert.fail("insufficient data, NumberIsTooSmallException expected");
        } catch (final NumberIsTooSmallException ex) {
            // expected
        }

        try {
            TestUtils.tTest(sampleStats1, (SummaryStatistics) null);
            Assert.fail("insufficient data, NullArgumentException expected");
        } catch (final NullArgumentException ex) {
            // expected
        }

        try {
            TestUtils.t(sample1, this.tooShortObs);
            Assert.fail("insufficient data, NumberIsTooSmallException expected");
        } catch (final NumberIsTooSmallException ex) {
            // expected
        }

        try {
            TestUtils.t(sampleStats1, (SummaryStatistics) null);
            Assert.fail("insufficient data, NullArgumentException expected");
        } catch (final NullArgumentException ex) {
            // expected
        }
    }

    @Test
    public void testTwoSampleTHomoscedastic() {
        final double[] sample1 = { 2, 4, 6, 8, 10, 97 };
        final double[] sample2 = { 4, 6, 8, 10, 16 };
        final SummaryStatistics sampleStats1 = new SummaryStatistics();
        for (final double element : sample1) {
            sampleStats1.addValue(element);
        }
        final SummaryStatistics sampleStats2 = new SummaryStatistics();
        for (final double element : sample2) {
            sampleStats2.addValue(element);
        }

        // Target comparison values computed using R version 1.8.1 (Linux version)
        Assert.assertEquals("two sample homoscedastic t stat", 0.73096310086,
            TestUtils.homoscedasticT(sample1, sample2), 10E-11);
        Assert.assertEquals("two sample homoscedastic p value", 0.4833963785,
            TestUtils.homoscedasticTTest(sampleStats1, sampleStats2), 1E-10);
        Assert.assertTrue("two sample homoscedastic t-test reject",
            TestUtils.homoscedasticTTest(sample1, sample2, 0.49));
        Assert.assertTrue("two sample homoscedastic t-test accept",
            !TestUtils.homoscedasticTTest(sample1, sample2, 0.48));
    }

    @Test
    public void testSmallSamples() {
        final double[] sample1 = { 1d, 3d };
        final double[] sample2 = { 4d, 5d };

        // Target values computed using R, version 1.8.1 (linux version)
        Assert.assertEquals(-2.2360679775, TestUtils.t(sample1, sample2),
            1E-10);
        Assert.assertEquals(0.198727388935, TestUtils.tTest(sample1, sample2),
            1E-10);
    }

    @Test
    public void testPaired() {
        final double[] sample1 = { 1d, 3d, 5d, 7d };
        final double[] sample2 = { 0d, 6d, 11d, 2d };
        final double[] sample3 = { 5d, 7d, 8d, 10d };

        // Target values computed using R, version 1.8.1 (linux version)
        Assert.assertEquals(-0.3133, TestUtils.pairedT(sample1, sample2), 1E-4);
        Assert.assertEquals(0.774544295819, TestUtils.pairedTTest(sample1, sample2), 1E-10);
        Assert.assertEquals(0.001208, TestUtils.pairedTTest(sample1, sample3), 1E-6);
        Assert.assertFalse(TestUtils.pairedTTest(sample1, sample3, .001));
        Assert.assertTrue(TestUtils.pairedTTest(sample1, sample3, .002));
    }

    private final double[] classA =
    { 93.0, 103.0, 95.0, 101.0 };
    private final double[] classB =
    { 99.0, 92.0, 102.0, 100.0, 102.0 };
    private final double[] classC =
    { 110.0, 115.0, 111.0, 117.0, 128.0 };

    private final List<double[]> classes = new ArrayList<double[]>();
    private final OneWayAnova oneWayAnova = new OneWayAnova();

    @Test
    public void testOneWayAnovaUtils() {
        this.classes.add(this.classA);
        this.classes.add(this.classB);
        this.classes.add(this.classC);
        Assert.assertEquals(this.oneWayAnova.anovaFValue(this.classes),
            TestUtils.oneWayAnovaFValue(this.classes), 10E-12);
        Assert.assertEquals(this.oneWayAnova.anovaPValue(this.classes),
            TestUtils.oneWayAnovaPValue(this.classes), 10E-12);
        Assert.assertEquals(this.oneWayAnova.anovaTest(this.classes, 0.01),
            TestUtils.oneWayAnovaTest(this.classes, 0.01));
    }

    @Test
    public void testGTestGoodnesOfFit() throws Exception {
        final double[] exp = new double[] {
            0.54d, 0.40d, 0.05d, 0.01d
        };

        final long[] obs = new long[] {
            70, 79, 3, 4
        };
        Assert.assertEquals("G test statistic",
            13.144799, TestUtils.g(exp, obs), 1E-5);
        final double p_gtgf = TestUtils.gTest(exp, obs);
        Assert.assertEquals("g-Test p-value", 0.004333, p_gtgf, 1E-5);

        Assert.assertTrue(TestUtils.gTest(exp, obs, 0.05));
    }

    @Test
    public void testGTestIndependance() throws Exception {
        final long[] obs1 = new long[] {
            268, 199, 42
        };

        final long[] obs2 = new long[] {
            807, 759, 184
        };

        final double g = TestUtils.gDataSetsComparison(obs1, obs2);

        Assert.assertEquals("G test statistic",
            7.3008170, g, 1E-4);
        final double p_gti = TestUtils.gTestDataSetsComparison(obs1, obs2);

        Assert.assertEquals("g-Test p-value", 0.0259805, p_gti, 1E-4);
        Assert.assertTrue(TestUtils.gTestDataSetsComparison(obs1, obs2, 0.05));
    }

    @Test
    public void testRootLogLikelihood() {
        // positive where k11 is bigger than expected.
        Assert.assertTrue(TestUtils.rootLogLikelihoodRatio(904, 21060, 1144, 283012) > 0.0);

        // negative because k11 is lower than expected
        Assert.assertTrue(TestUtils.rootLogLikelihoodRatio(36, 21928, 60280, 623876) < 0.0);

        Assert.assertEquals(Math.sqrt(2.772589), TestUtils.rootLogLikelihoodRatio(1, 0, 0, 1), 0.000001);
        Assert.assertEquals(-Math.sqrt(2.772589), TestUtils.rootLogLikelihoodRatio(0, 1, 1, 0), 0.000001);
        Assert.assertEquals(Math.sqrt(27.72589), TestUtils.rootLogLikelihoodRatio(10, 0, 0, 10), 0.00001);

        Assert.assertEquals(Math.sqrt(39.33052), TestUtils.rootLogLikelihoodRatio(5, 1995, 0, 100000), 0.00001);
        Assert.assertEquals(-Math.sqrt(39.33052), TestUtils.rootLogLikelihoodRatio(0, 100000, 5, 1995), 0.00001);

        Assert.assertEquals(Math.sqrt(4730.737), TestUtils.rootLogLikelihoodRatio(1000, 1995, 1000, 100000), 0.001);
        Assert.assertEquals(-Math.sqrt(4730.737), TestUtils.rootLogLikelihoodRatio(1000, 100000, 1000, 1995), 0.001);

        Assert.assertEquals(Math.sqrt(5734.343), TestUtils.rootLogLikelihoodRatio(1000, 1000, 1000, 100000), 0.001);
        Assert.assertEquals(Math.sqrt(5714.932), TestUtils.rootLogLikelihoodRatio(1000, 1000, 1000, 99000), 0.001);
    }
}
