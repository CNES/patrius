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
import org.junit.Test;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.NoDataException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooLargeException;

/**
 * Test cases for the WilcoxonSignedRangTest class.
 * 
 * @version $Id: WilcoxonSignedRankTestTest.java 18108 2017-10-04 06:45:27Z bignon $
 */

public class WilcoxonSignedRankTestTest {

    protected WilcoxonSignedRankTest testStatistic = new WilcoxonSignedRankTest();

    @Test
    public void testWilcoxonSignedRankSimple() {
        /*
         * Target values computed using R version 2.11.1
         * x <- c(1.83, 0.50, 1.62, 2.48, 1.68, 1.88, 1.55, 3.06, 1.30)
         * y <- c(0.878, 0.647, 0.598, 2.05, 1.06, 1.29, 1.06, 3.14, 1.29)
         */
        final double x[] = { 1.83, 0.50, 1.62, 2.48, 1.68, 1.88, 1.55, 3.06, 1.30 };
        final double y[] = { 0.878, 0.647, 0.598, 2.05, 1.06, 1.29, 1.06, 3.14, 1.29 };

        /*
         * EXACT:
         * wilcox.test(x, y, alternative = "two.sided", mu = 0, paired = TRUE, exact = TRUE, correct = FALSE)
         * V = 40, p-value = 0.03906
         * Corresponds to the value obtained in R.
         */
        Assert.assertEquals(40, this.testStatistic.wilcoxonSignedRank(x, y), 1e-10);
        Assert.assertEquals(0.03906, this.testStatistic.wilcoxonSignedRankTest(x, y, true), 1e-5);

        /*
         * ASYMPTOTIC:
         * wilcox.test(x, y, alternative = "two.sided", mu = 0, paired = TRUE, exact = FALSE, correct = FALSE)
         * V = 40, p-value = 0.03815
         * This is not entirely the same due to different corrects,
         * e.g. http://mlsc.lboro.ac.uk/resources/statistics/wsrt.pdf
         * and src/library/stats/R/wilcox.test.R in the R source
         */
        Assert.assertEquals(40, this.testStatistic.wilcoxonSignedRank(x, y), 1e-10);
        Assert.assertEquals(0.0329693812, this.testStatistic.wilcoxonSignedRankTest(x, y, false), 1e-10);
    }

    @Test
    public void testWilcoxonSignedRankInputValidation() {
        /*
         * Exact only for sample size <= 30
         */
        final double[] x1 = new double[30];
        final double[] x2 = new double[31];
        final double[] y1 = new double[30];
        final double[] y2 = new double[31];
        for (int i = 0; i < 30; ++i) {
            x1[i] = x2[i] = y1[i] = y2[i] = i;
        }

        // Exactly 30 is okay
        // testStatistic.wilcoxonSignedRankTest(x1, y1, true);

        try {
            this.testStatistic.wilcoxonSignedRankTest(x2, y2, true);
            Assert.fail("More than 30 samples and exact chosen, NumberIsTooLargeException expected");
        } catch (final NumberIsTooLargeException ex) {
            // expected
        }

        /*
         * Samples must be present, i.e. length > 0
         */
        try {
            this.testStatistic.wilcoxonSignedRankTest(new double[] {}, new double[] { 1.0 }, true);
            Assert.fail("x does not contain samples (exact), NoDataException expected");
        } catch (final NoDataException ex) {
            // expected
        }

        try {
            this.testStatistic.wilcoxonSignedRankTest(new double[] {}, new double[] { 1.0 }, false);
            Assert.fail("x does not contain samples (asymptotic), NoDataException expected");
        } catch (final NoDataException ex) {
            // expected
        }

        try {
            this.testStatistic.wilcoxonSignedRankTest(new double[] { 1.0 }, new double[] {}, true);
            Assert.fail("y does not contain samples (exact), NoDataException expected");
        } catch (final NoDataException ex) {
            // expected
        }

        try {
            this.testStatistic.wilcoxonSignedRankTest(new double[] { 1.0 }, new double[] {}, false);
            Assert.fail("y does not contain samples (asymptotic), NoDataException expected");
        } catch (final NoDataException ex) {
            // expected
        }

        /*
         * Samples not same size, i.e. cannot be pairred
         */
        try {
            this.testStatistic.wilcoxonSignedRankTest(new double[] { 1.0, 2.0 }, new double[] { 3.0 }, true);
            Assert.fail("x and y not same size (exact), DimensionMismatchException expected");
        } catch (final DimensionMismatchException ex) {
            // expected
        }

        try {
            this.testStatistic.wilcoxonSignedRankTest(new double[] { 1.0, 2.0 }, new double[] { 3.0 }, false);
            Assert.fail("x and y not same size (asymptotic), DimensionMismatchException expected");
        } catch (final DimensionMismatchException ex) {
            // expected
        }

        /*
         * x and y is null
         */
        try {
            this.testStatistic.wilcoxonSignedRankTest(null, null, true);
            Assert.fail("x and y is null (exact), NullArgumentException expected");
        } catch (final NullArgumentException ex) {
            // expected
        }

        try {
            this.testStatistic.wilcoxonSignedRankTest(null, null, false);
            Assert.fail("x and y is null (asymptotic), NullArgumentException expected");
        } catch (final NullArgumentException ex) {
            // expected
        }

        /*
         * x or y is null
         */
        try {
            this.testStatistic.wilcoxonSignedRankTest(null, new double[] { 1.0 }, true);
            Assert.fail("x is null (exact), NullArgumentException expected");
        } catch (final NullArgumentException ex) {
            // expected
        }

        try {
            this.testStatistic.wilcoxonSignedRankTest(null, new double[] { 1.0 }, false);
            Assert.fail("x is null (asymptotic), NullArgumentException expected");
        } catch (final NullArgumentException ex) {
            // expected
        }

        try {
            this.testStatistic.wilcoxonSignedRankTest(new double[] { 1.0 }, null, true);
            Assert.fail("y is null (exact), NullArgumentException expected");
        } catch (final NullArgumentException ex) {
            // expected
        }

        try {
            this.testStatistic.wilcoxonSignedRankTest(new double[] { 1.0 }, null, false);
            Assert.fail("y is null (asymptotic), NullArgumentException expected");
        } catch (final NullArgumentException ex) {
            // expected
        }
    }
}
