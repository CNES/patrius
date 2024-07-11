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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;
import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;
import fr.cnes.sirius.patrius.math.stat.descriptive.SummaryStatistics;

/**
 * Test cases for the TTestImpl class.
 * 
 * @version $Id: TTestTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class TTestTest {

    protected TTest testStatistic = new TTest();

    private final double[] tooShortObs = { 1.0 };
    private final double[] emptyObs = {};
    private final SummaryStatistics emptyStats = new SummaryStatistics();
    SummaryStatistics tooShortStats = null;

    @Before
    public void setUp() {
        this.tooShortStats = new SummaryStatistics();
        this.tooShortStats.addValue(0d);
    }

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
            this.testStatistic.t(mu, observed), 10E-10);
        Assert.assertEquals("t statistic", -2.81976445346,
            this.testStatistic.t(mu, sampleStats), 10E-10);
        Assert.assertEquals("p value", 0.0136390585873,
            this.testStatistic.tTest(mu, observed), 10E-10);
        Assert.assertEquals("p value", 0.0136390585873,
            this.testStatistic.tTest(mu, sampleStats), 10E-10);

        try {
            this.testStatistic.t(mu, (double[]) null);
            Assert.fail("arguments too short, NullArgumentException expected");
        } catch (final NullArgumentException ex) {
            // expected
        }

        try {
            this.testStatistic.t(mu, (SummaryStatistics) null);
            Assert.fail("arguments too short, NullArgumentException expected");
        } catch (final NullArgumentException ex) {
            // expected
        }

        try {
            this.testStatistic.t(mu, this.emptyObs);
            Assert.fail("arguments too short, NumberIsTooSmallException expected");
        } catch (final NumberIsTooSmallException ex) {
            // expected
        }

        try {
            this.testStatistic.t(mu, this.emptyStats);
            Assert.fail("arguments too short, NumberIsTooSmallException expected");
        } catch (final NumberIsTooSmallException ex) {
            // expected
        }

        try {
            this.testStatistic.t(mu, this.tooShortObs);
            Assert.fail("insufficient data to compute t statistic, NumberIsTooSmallException expected");
        } catch (final NumberIsTooSmallException ex) {
            // expected
        }
        try {
            this.testStatistic.tTest(mu, this.tooShortObs);
            Assert.fail("insufficient data to perform t test, NumberIsTooSmallException expected");
        } catch (final NumberIsTooSmallException ex) {
            // expected
        }

        try {
            this.testStatistic.t(mu, this.tooShortStats);
            Assert.fail("insufficient data to compute t statistic, NumberIsTooSmallException expected");
        } catch (final NumberIsTooSmallException ex) {
            // expected
        }
        try {
            this.testStatistic.tTest(mu, this.tooShortStats);
            Assert.fail("insufficient data to perform t test, NumberIsTooSmallException expected");
        } catch (final NumberIsTooSmallException ex) {
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
            this.testStatistic.t(0d, oneSidedP), 10E-10);
        Assert.assertEquals("one sample t stat", 3.86485535541,
            this.testStatistic.t(0d, oneSidedPStats), 1E-10);
        Assert.assertEquals("one sample p value", 0.000521637019637,
            this.testStatistic.tTest(0d, oneSidedP) / 2d, 10E-10);
        Assert.assertEquals("one sample p value", 0.000521637019637,
            this.testStatistic.tTest(0d, oneSidedPStats) / 2d, 10E-5);
        Assert.assertTrue("one sample t-test reject", this.testStatistic.tTest(0d, oneSidedP, 0.01));
        Assert.assertTrue("one sample t-test reject", this.testStatistic.tTest(0d, oneSidedPStats, 0.01));
        Assert.assertTrue("one sample t-test accept", !this.testStatistic.tTest(0d, oneSidedP, 0.0001));
        Assert.assertTrue("one sample t-test accept", !this.testStatistic.tTest(0d, oneSidedPStats, 0.0001));

        try {
            this.testStatistic.tTest(0d, oneSidedP, 95);
            Assert.fail("alpha out of range, OutOfRangeException expected");
        } catch (final OutOfRangeException ex) {
            // expected
        }

        try {
            this.testStatistic.tTest(0d, oneSidedPStats, 95);
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
            this.testStatistic.t(sample1, sample2), 1E-10);
        Assert.assertEquals("two sample heteroscedastic t stat", 1.60371728768,
            this.testStatistic.t(sampleStats1, sampleStats2), 1E-10);
        Assert.assertEquals("two sample heteroscedastic p value", 0.128839369622,
            this.testStatistic.tTest(sample1, sample2), 1E-10);
        Assert.assertEquals("two sample heteroscedastic p value", 0.128839369622,
            this.testStatistic.tTest(sampleStats1, sampleStats2), 1E-10);
        Assert.assertTrue("two sample heteroscedastic t-test reject",
            this.testStatistic.tTest(sample1, sample2, 0.2));
        Assert.assertTrue("two sample heteroscedastic t-test reject",
            this.testStatistic.tTest(sampleStats1, sampleStats2, 0.2));
        Assert.assertTrue("two sample heteroscedastic t-test accept",
            !this.testStatistic.tTest(sample1, sample2, 0.1));
        Assert.assertTrue("two sample heteroscedastic t-test accept",
            !this.testStatistic.tTest(sampleStats1, sampleStats2, 0.1));

        try {
            this.testStatistic.tTest(sample1, sample2, .95);
            Assert.fail("alpha out of range, OutOfRangeException expected");
        } catch (final OutOfRangeException ex) {
            // expected
        }

        try {
            this.testStatistic.tTest(sampleStats1, sampleStats2, .95);
            Assert.fail("alpha out of range, OutOfRangeException expected");
        } catch (final OutOfRangeException ex) {
            // expected
        }

        try {
            this.testStatistic.tTest(sample1, this.tooShortObs, .01);
            Assert.fail("insufficient data, NumberIsTooSmallException expected");
        } catch (final NumberIsTooSmallException ex) {
            // expected
        }

        try {
            this.testStatistic.tTest(sampleStats1, this.tooShortStats, .01);
            Assert.fail("insufficient data, NumberIsTooSmallException expected");
        } catch (final NumberIsTooSmallException ex) {
            // expected
        }

        try {
            this.testStatistic.tTest(sample1, this.tooShortObs);
            Assert.fail("insufficient data, NumberIsTooSmallException expected");
        } catch (final NumberIsTooSmallException ex) {
            // expected
        }

        try {
            this.testStatistic.tTest(sampleStats1, this.tooShortStats);
            Assert.fail("insufficient data, NumberIsTooSmallException expected");
        } catch (final NumberIsTooSmallException ex) {
            // expected
        }

        try {
            this.testStatistic.t(sample1, this.tooShortObs);
            Assert.fail("insufficient data, NumberIsTooSmallException expected");
        } catch (final NumberIsTooSmallException ex) {
            // expected
        }

        try {
            this.testStatistic.t(sampleStats1, this.tooShortStats);
            Assert.fail("insufficient data, NumberIsTooSmallException expected");
        } catch (final NumberIsTooSmallException ex) {
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
            this.testStatistic.homoscedasticT(sample1, sample2), 10E-11);
        Assert.assertEquals("two sample homoscedastic p value", 0.4833963785,
            this.testStatistic.homoscedasticTTest(sampleStats1, sampleStats2), 1E-10);
        Assert.assertTrue("two sample homoscedastic t-test reject",
            this.testStatistic.homoscedasticTTest(sample1, sample2, 0.49));
        Assert.assertTrue("two sample homoscedastic t-test accept",
            !this.testStatistic.homoscedasticTTest(sample1, sample2, 0.48));
    }

    @Test
    public void testSmallSamples() {
        final double[] sample1 = { 1d, 3d };
        final double[] sample2 = { 4d, 5d };

        // Target values computed using R, version 1.8.1 (linux version)
        Assert.assertEquals(-2.2360679775, this.testStatistic.t(sample1, sample2),
            1E-10);
        Assert.assertEquals(0.198727388935, this.testStatistic.tTest(sample1, sample2),
            1E-10);
    }

    @Test
    public void testPaired() {
        final double[] sample1 = { 1d, 3d, 5d, 7d };
        final double[] sample2 = { 0d, 6d, 11d, 2d };
        final double[] sample3 = { 5d, 7d, 8d, 10d };

        // Target values computed using R, version 1.8.1 (linux version)
        Assert.assertEquals(-0.3133, this.testStatistic.pairedT(sample1, sample2), 1E-4);
        Assert.assertEquals(0.774544295819, this.testStatistic.pairedTTest(sample1, sample2), 1E-10);
        Assert.assertEquals(0.001208, this.testStatistic.pairedTTest(sample1, sample3), 1E-6);
        Assert.assertFalse(this.testStatistic.pairedTTest(sample1, sample3, .001));
        Assert.assertTrue(this.testStatistic.pairedTTest(sample1, sample3, .002));
    }
}
