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
 * END-HISTORY
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
 * Test cases for the GTest class.
 * 
 * Data for the tests are from p64-69 in: McDonald, J.H. 2009. Handbook of
 * Biological Statistics (2nd ed.). Sparky House Publishing, Baltimore,
 * Maryland.
 * 
 */
public class GTestTest {

    protected GTest testStatistic = new GTest();

    @Test
    public void testGTestGoodnesOfFit1() throws Exception {
        final double[] exp = new double[] {
            3d, 1d
        };

        final long[] obs = new long[] {
            423, 133
        };

        Assert.assertEquals("G test statistic",
            0.348721, this.testStatistic.g(exp, obs), 1E-6);
        final double p_gtgf = this.testStatistic.gTest(exp, obs);
        Assert.assertEquals("g-Test p-value", 0.55483, p_gtgf, 1E-5);

        Assert.assertFalse(this.testStatistic.gTest(exp, obs, 0.05));
    }

    @Test
    public void testGTestGoodnesOfFit2() throws Exception {
        final double[] exp = new double[] {
            0.54d, 0.40d, 0.05d, 0.01d
        };

        final long[] obs = new long[] {
            70, 79, 3, 4
        };
        Assert.assertEquals("G test statistic",
            13.144799, this.testStatistic.g(exp, obs), 1E-6);
        final double p_gtgf = this.testStatistic.gTest(exp, obs);
        Assert.assertEquals("g-Test p-value", 0.004333, p_gtgf, 1E-5);

        Assert.assertTrue(this.testStatistic.gTest(exp, obs, 0.05));
    }

    @Test
    public void testGTestGoodnesOfFit3() throws Exception {
        final double[] exp = new double[] {
            0.167d, 0.483d, 0.350d
        };

        final long[] obs = new long[] {
            14, 21, 25
        };

        Assert.assertEquals("G test statistic",
            4.5554, this.testStatistic.g(exp, obs), 1E-4);
        // Intrinisic (Hardy-Weinberg proportions) P-Value should be 0.033
        final double p_gtgf = this.testStatistic.gTestIntrinsic(exp, obs);
        Assert.assertEquals("g-Test p-value", 0.0328, p_gtgf, 1E-4);

        Assert.assertFalse(this.testStatistic.gTest(exp, obs, 0.05));
    }

    @Test
    public void testGTestIndependance1() throws Exception {
        final long[] obs1 = new long[] {
            268, 199, 42
        };

        final long[] obs2 = new long[] {
            807, 759, 184
        };

        final double g = this.testStatistic.gDataSetsComparison(obs1, obs2);

        Assert.assertEquals("G test statistic",
            7.3008170, g, 1E-6);
        final double p_gti = this.testStatistic.gTestDataSetsComparison(obs1, obs2);

        Assert.assertEquals("g-Test p-value", 0.0259805, p_gti, 1E-6);
        Assert.assertTrue(this.testStatistic.gTestDataSetsComparison(obs1, obs2, 0.05));
    }

    @Test
    public void testGTestIndependance2() throws Exception {
        final long[] obs1 = new long[] {
            127, 99, 264
        };

        final long[] obs2 = new long[] {
            116, 67, 161
        };

        final double g = this.testStatistic.gDataSetsComparison(obs1, obs2);

        Assert.assertEquals("G test statistic",
            6.227288, g, 1E-6);
        final double p_gti = this.testStatistic.gTestDataSetsComparison(obs1, obs2);

        Assert.assertEquals("g-Test p-value", 0.04443, p_gti, 1E-5);
        Assert.assertTrue(this.testStatistic.gTestDataSetsComparison(obs1, obs2, 0.05));
    }

    @Test
    public void testGTestIndependance3() throws Exception {
        final long[] obs1 = new long[] {
            190, 149
        };

        final long[] obs2 = new long[] {
            42, 49
        };

        final double g = this.testStatistic.gDataSetsComparison(obs1, obs2);
        Assert.assertEquals("G test statistic",
            2.8187, g, 1E-4);
        final double p_gti = this.testStatistic.gTestDataSetsComparison(obs1, obs2);
        Assert.assertEquals("g-Test p-value", 0.09317325, p_gti, 1E-6);

        Assert.assertFalse(this.testStatistic.gTestDataSetsComparison(obs1, obs2, 0.05));
    }

    @Test
    public void testGTestSetsComparisonBadCounts() {
        final long[] observed1 = { 10, -1, 12, 10, 15 };
        final long[] observed2 = { 15, 10, 10, 15, 5 };
        try {
            this.testStatistic.gTestDataSetsComparison(
                observed1, observed2);
            Assert.fail("Expecting NotPositiveException - negative count");
        } catch (final NotPositiveException ex) {
            // expected
        }
        final long[] observed3 = { 10, 0, 12, 10, 15 };
        final long[] observed4 = { 15, 0, 10, 15, 5 };
        try {
            this.testStatistic.gTestDataSetsComparison(
                observed3, observed4);
            Assert.fail("Expecting ZeroException - double 0's");
        } catch (final ZeroException ex) {
            // expected
        }
        final long[] observed5 = { 10, 10, 12, 10, 15 };
        final long[] observed6 = { 0, 0, 0, 0, 0 };
        try {
            this.testStatistic.gTestDataSetsComparison(
                observed5, observed6);
            Assert.fail("Expecting ZeroException - vanishing counts");
        } catch (final ZeroException ex) {
            // expected
        }
    }

    @Test
    public void testUnmatchedArrays() {
        final long[] observed = { 0, 1, 2, 3 };
        final double[] expected = { 1, 1, 2 };
        final long[] observed2 = { 3, 4 };
        try {
            this.testStatistic.gTest(expected, observed);
            Assert.fail("arrays have different lengths, DimensionMismatchException expected");
        } catch (final DimensionMismatchException ex) {
            // expected
        }
        try {
            this.testStatistic.gTestDataSetsComparison(observed, observed2);
            Assert.fail("arrays have different lengths, DimensionMismatchException expected");
        } catch (final DimensionMismatchException ex) {
            // expected
        }
    }

    @Test
    public void testNegativeObservedCounts() {
        final long[] observed = { 0, 1, 2, -3 };
        final double[] expected = { 1, 1, 2, 3 };
        final long[] observed2 = { 3, 4, 5, 0 };
        try {
            this.testStatistic.gTest(expected, observed);
            Assert.fail("negative observed count, NotPositiveException expected");
        } catch (final NotPositiveException ex) {
            // expected
        }
        try {
            this.testStatistic.gTestDataSetsComparison(observed, observed2);
            Assert.fail("negative observed count, NotPositiveException expected");
        } catch (final NotPositiveException ex) {
            // expected
        }
    }

    @Test
    public void testZeroExpectedCounts() {
        final long[] observed = { 0, 1, 2, -3 };
        final double[] expected = { 1, 0, 2, 3 };
        try {
            this.testStatistic.gTest(expected, observed);
            Assert.fail("zero expected count, NotStrictlyPositiveException expected");
        } catch (final NotStrictlyPositiveException ex) {
            // expected
        }
    }

    @Test
    public void testBadAlpha() {
        final long[] observed = { 0, 1, 2, 3 };
        final double[] expected = { 1, 2, 2, 3 };
        final long[] observed2 = { 0, 2, 2, 3 };
        try {
            this.testStatistic.gTest(expected, observed, 0.8);
            Assert.fail("zero expected count, NotStrictlyPositiveException expected");
        } catch (final OutOfRangeException ex) {
            // expected
        }
        try {
            this.testStatistic.gTestDataSetsComparison(observed, observed2, -0.5);
            Assert.fail("zero expected count, NotStrictlyPositiveException expected");
        } catch (final OutOfRangeException ex) {
            // expected
        }
    }

    @Test
    public void testScaling() {
        final long[] observed = { 9, 11, 10, 8, 12 };
        final double[] expected1 = { 10, 10, 10, 10, 10 };
        final double[] expected2 = { 1000, 1000, 1000, 1000, 1000 };
        final double[] expected3 = { 1, 1, 1, 1, 1 };
        final double tol = 1E-15;
        Assert.assertEquals(
            this.testStatistic.gTest(expected1, observed),
            this.testStatistic.gTest(expected2, observed),
            tol);
        Assert.assertEquals(
            this.testStatistic.gTest(expected1, observed),
            this.testStatistic.gTest(expected3, observed),
            tol);
    }

    @Test
    public void testRootLogLikelihood() {
        // positive where k11 is bigger than expected.
        Assert.assertTrue(this.testStatistic.rootLogLikelihoodRatio(904, 21060, 1144, 283012) > 0.0);

        // negative because k11 is lower than expected
        Assert.assertTrue(this.testStatistic.rootLogLikelihoodRatio(36, 21928, 60280, 623876) < 0.0);

        Assert.assertEquals(Math.sqrt(2.772589), this.testStatistic.rootLogLikelihoodRatio(1, 0, 0, 1), 0.000001);
        Assert.assertEquals(-Math.sqrt(2.772589), this.testStatistic.rootLogLikelihoodRatio(0, 1, 1, 0), 0.000001);
        Assert.assertEquals(Math.sqrt(27.72589), this.testStatistic.rootLogLikelihoodRatio(10, 0, 0, 10), 0.00001);

        Assert
            .assertEquals(Math.sqrt(39.33052), this.testStatistic.rootLogLikelihoodRatio(5, 1995, 0, 100000), 0.00001);
        Assert.assertEquals(-Math.sqrt(39.33052), this.testStatistic.rootLogLikelihoodRatio(0, 100000, 5, 1995),
            0.00001);

        Assert.assertEquals(Math.sqrt(4730.737), this.testStatistic.rootLogLikelihoodRatio(1000, 1995, 1000, 100000),
            0.001);
        Assert.assertEquals(-Math.sqrt(4730.737), this.testStatistic.rootLogLikelihoodRatio(1000, 100000, 1000, 1995),
            0.001);

        Assert.assertEquals(Math.sqrt(5734.343), this.testStatistic.rootLogLikelihoodRatio(1000, 1000, 1000, 100000),
            0.001);
        Assert.assertEquals(Math.sqrt(5714.932), this.testStatistic.rootLogLikelihoodRatio(1000, 1000, 1000, 99000),
            0.001);
    }
}
