/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
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
 */
package fr.cnes.sirius.patrius.math.stat.inference;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.NotPositiveException;
import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;
import fr.cnes.sirius.patrius.math.exception.ZeroException;

/**
 * Test cases for the ChiSquareTestImpl class.
 * 
 * @version $Id: ChiSquareTestTest.java 18108 2017-10-04 06:45:27Z bignon $
 */

public class ChiSquareTestTest {

    protected ChiSquareTest testStatistic = new ChiSquareTest();

    @Test
    public void testChiSquare() {

        // Target values computed using R version 1.8.1
        // Some assembly required ;-)
        // Use sum((obs - exp)^2/exp) for the chi-square statistic and
        // 1 - pchisq(sum((obs - exp)^2/exp), length(obs) - 1) for the p-value

        final long[] observed = { 10, 9, 11 };
        final double[] expected = { 10, 10, 10 };
        Assert.assertEquals("chi-square statistic", 0.2, this.testStatistic.chiSquare(expected, observed), 10E-12);
        Assert.assertEquals("chi-square p-value", 0.904837418036, this.testStatistic.chiSquareTest(expected, observed),
            1E-10);

        final long[] observed1 = { 500, 623, 72, 70, 31 };
        final double[] expected1 = { 485, 541, 82, 61, 37 };
        Assert.assertEquals("chi-square test statistic", 9.023307936427388,
            this.testStatistic.chiSquare(expected1, observed1), 1E-10);
        Assert.assertEquals("chi-square p-value", 0.06051952647453607,
            this.testStatistic.chiSquareTest(expected1, observed1), 1E-9);
        Assert.assertTrue("chi-square test reject", this.testStatistic.chiSquareTest(expected1, observed1, 0.08));
        Assert.assertTrue("chi-square test accept", !this.testStatistic.chiSquareTest(expected1, observed1, 0.05));

        try {
            this.testStatistic.chiSquareTest(expected1, observed1, 95);
            Assert.fail("alpha out of range, OutOfRangeException expected");
        } catch (final OutOfRangeException ex) {
            // expected
        }

        final long[] tooShortObs = { 0 };
        final double[] tooShortEx = { 1 };
        try {
            this.testStatistic.chiSquare(tooShortEx, tooShortObs);
            Assert.fail("arguments too short, DimensionMismatchException expected");
        } catch (final DimensionMismatchException ex) {
            // expected
        }

        // unmatched arrays
        final long[] unMatchedObs = { 0, 1, 2, 3 };
        final double[] unMatchedEx = { 1, 1, 2 };
        try {
            this.testStatistic.chiSquare(unMatchedEx, unMatchedObs);
            Assert.fail("arrays have different lengths, DimensionMismatchException expected");
        } catch (final DimensionMismatchException ex) {
            // expected
        }

        // 0 expected count
        expected[0] = 0;
        try {
            this.testStatistic.chiSquareTest(expected, observed, .01);
            Assert.fail("bad expected count, NotStrictlyPositiveException expected");
        } catch (final NotStrictlyPositiveException ex) {
            // expected
        }

        // negative observed count
        expected[0] = 1;
        observed[0] = -1;
        try {
            this.testStatistic.chiSquareTest(expected, observed, .01);
            Assert.fail("bad expected count, NotPositiveException expected");
        } catch (final NotPositiveException ex) {
            // expected
        }

    }

    @Test
    public void testChiSquareIndependence() {

        // Target values computed using R version 1.8.1

        final long[][] counts = { { 40, 22, 43 }, { 91, 21, 28 }, { 60, 10, 22 } };
        Assert.assertEquals("chi-square test statistic", 22.709027688, this.testStatistic.chiSquare(counts), 1E-9);
        Assert.assertEquals("chi-square p-value", 0.000144751460134, this.testStatistic.chiSquareTest(counts), 1E-9);
        Assert.assertTrue("chi-square test reject", this.testStatistic.chiSquareTest(counts, 0.0002));
        Assert.assertTrue("chi-square test accept", !this.testStatistic.chiSquareTest(counts, 0.0001));

        final long[][] counts2 = { { 10, 15 }, { 30, 40 }, { 60, 90 } };
        Assert.assertEquals("chi-square test statistic", 0.168965517241, this.testStatistic.chiSquare(counts2), 1E-9);
        Assert.assertEquals("chi-square p-value", 0.918987499852, this.testStatistic.chiSquareTest(counts2), 1E-9);
        Assert.assertTrue("chi-square test accept", !this.testStatistic.chiSquareTest(counts2, 0.1));

        // ragged input array
        final long[][] counts3 = { { 40, 22, 43 }, { 91, 21, 28 }, { 60, 10 } };
        try {
            this.testStatistic.chiSquare(counts3);
            Assert.fail("Expecting DimensionMismatchException");
        } catch (final DimensionMismatchException ex) {
            // expected
        }

        // insufficient data
        final long[][] counts4 = { { 40, 22, 43 } };
        try {
            this.testStatistic.chiSquare(counts4);
            Assert.fail("Expecting DimensionMismatchException");
        } catch (final DimensionMismatchException ex) {
            // expected
        }
        final long[][] counts5 = { { 40 }, { 40 }, { 30 }, { 10 } };
        try {
            this.testStatistic.chiSquare(counts5);
            Assert.fail("Expecting DimensionMismatchException");
        } catch (final DimensionMismatchException ex) {
            // expected
        }

        // negative counts
        final long[][] counts6 = { { 10, -2 }, { 30, 40 }, { 60, 90 } };
        try {
            this.testStatistic.chiSquare(counts6);
            Assert.fail("Expecting NotPositiveException");
        } catch (final NotPositiveException ex) {
            // expected
        }

        // bad alpha
        try {
            this.testStatistic.chiSquareTest(counts, 0);
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
            114875.90421929007, this.testStatistic.chiSquare(exp, obs), 1E-9);
    }

    /** Contingency table containing zeros - PR # 32531 */
    @Test
    public void testChiSquareZeroCount() {
        // Target values computed using R version 1.8.1
        final long[][] counts = { { 40, 0, 4 }, { 91, 1, 2 }, { 60, 2, 0 } };
        Assert.assertEquals("chi-square test statistic", 9.67444662263,
            this.testStatistic.chiSquare(counts), 1E-9);
        Assert.assertEquals("chi-square p-value", 0.0462835770603,
            this.testStatistic.chiSquareTest(counts), 1E-9);
    }

    /** Target values verified using DATAPLOT version 2006.3 */
    @Test
    public void testChiSquareDataSetsComparisonEqualCounts()
    {
        final long[] observed1 = { 10, 12, 12, 10 };
        final long[] observed2 = { 5, 15, 14, 10 };
        Assert.assertEquals("chi-square p value", 0.541096,
            this.testStatistic.chiSquareTestDataSetsComparison(
                observed1, observed2), 1E-6);
        Assert.assertEquals("chi-square test statistic", 2.153846,
            this.testStatistic.chiSquareDataSetsComparison(
                observed1, observed2), 1E-6);
        Assert.assertFalse("chi-square test result",
            this.testStatistic.chiSquareTestDataSetsComparison(
                observed1, observed2, 0.4));
    }

    /** Target values verified using DATAPLOT version 2006.3 */
    @Test
    public void testChiSquareDataSetsComparisonUnEqualCounts()
    {
        final long[] observed1 = { 10, 12, 12, 10, 15 };
        final long[] observed2 = { 15, 10, 10, 15, 5 };
        Assert.assertEquals("chi-square p value", 0.124115,
            this.testStatistic.chiSquareTestDataSetsComparison(
                observed1, observed2), 1E-6);
        Assert.assertEquals("chi-square test statistic", 7.232189,
            this.testStatistic.chiSquareDataSetsComparison(
                observed1, observed2), 1E-6);
        Assert.assertTrue("chi-square test result",
            this.testStatistic.chiSquareTestDataSetsComparison(
                observed1, observed2, 0.13));
        Assert.assertFalse("chi-square test result",
            this.testStatistic.chiSquareTestDataSetsComparison(
                observed1, observed2, 0.12));
    }

    @Test
    public void testChiSquareDataSetsComparisonBadCounts()
    {
        final long[] observed1 = { 10, -1, 12, 10, 15 };
        final long[] observed2 = { 15, 10, 10, 15, 5 };
        try {
            this.testStatistic.chiSquareTestDataSetsComparison(
                observed1, observed2);
            Assert.fail("Expecting NotPositiveException - negative count");
        } catch (final NotPositiveException ex) {
            // expected
        }
        final long[] observed3 = { 10, 0, 12, 10, 15 };
        final long[] observed4 = { 15, 0, 10, 15, 5 };
        try {
            this.testStatistic.chiSquareTestDataSetsComparison(
                observed3, observed4);
            Assert.fail("Expecting ZeroException - double 0's");
        } catch (final ZeroException ex) {
            // expected
        }
        final long[] observed5 = { 10, 10, 12, 10, 15 };
        final long[] observed6 = { 0, 0, 0, 0, 0 };
        try {
            this.testStatistic.chiSquareTestDataSetsComparison(
                observed5, observed6);
            Assert.fail("Expecting ZeroException - vanishing counts");
        } catch (final ZeroException ex) {
            // expected
        }
    }
}
