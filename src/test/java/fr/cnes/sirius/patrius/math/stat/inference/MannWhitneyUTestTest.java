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

import fr.cnes.sirius.patrius.math.exception.NoDataException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;

/**
 * Test cases for the MannWhitneyUTestImpl class.
 * 
 * @version $Id: MannWhitneyUTestTest.java 18108 2017-10-04 06:45:27Z bignon $
 */

public class MannWhitneyUTestTest {

    protected MannWhitneyUTest testStatistic = new MannWhitneyUTest();

    @Test
    public void testMannWhitneyUSimple() {
        /*
         * Target values computed using R version 2.11.1
         * x <- c(19, 22, 16, 29, 24)
         * y <- c(20, 11, 17, 12)
         * wilcox.test(x, y, alternative = "two.sided", mu = 0, paired = FALSE, exact = FALSE, correct = FALSE)
         * W = 17, p-value = 0.08641
         */
        final double x[] = { 19, 22, 16, 29, 24 };
        final double y[] = { 20, 11, 17, 12 };

        Assert.assertEquals(17, this.testStatistic.mannWhitneyU(x, y), 1e-10);
        Assert.assertEquals(0.08641, this.testStatistic.mannWhitneyUTest(x, y), 1e-5);
    }

    @Test
    public void testMannWhitneyUInputValidation() {
        /*
         * Samples must be present, i.e. length > 0
         */
        try {
            this.testStatistic.mannWhitneyUTest(new double[] {}, new double[] { 1.0 });
            Assert.fail("x does not contain samples (exact), NoDataException expected");
        } catch (final NoDataException ex) {
            // expected
        }

        try {
            this.testStatistic.mannWhitneyUTest(new double[] { 1.0 }, new double[] {});
            Assert.fail("y does not contain samples (exact), NoDataException expected");
        } catch (final NoDataException ex) {
            // expected
        }

        /*
         * x and y is null
         */
        try {
            this.testStatistic.mannWhitneyUTest(null, null);
            Assert.fail("x and y is null (exact), NullArgumentException expected");
        } catch (final NullArgumentException ex) {
            // expected
        }

        try {
            this.testStatistic.mannWhitneyUTest(null, null);
            Assert.fail("x and y is null (asymptotic), NullArgumentException expected");
        } catch (final NullArgumentException ex) {
            // expected
        }

        /*
         * x or y is null
         */
        try {
            this.testStatistic.mannWhitneyUTest(null, new double[] { 1.0 });
            Assert.fail("x is null (exact), NullArgumentException expected");
        } catch (final NullArgumentException ex) {
            // expected
        }

        try {
            this.testStatistic.mannWhitneyUTest(new double[] { 1.0 }, null);
            Assert.fail("y is null (exact), NullArgumentException expected");
        } catch (final NullArgumentException ex) {
            // expected
        }
    }

    @Test
    public void testBigDataSet() {
        final double[] d1 = new double[1500];
        final double[] d2 = new double[1500];
        for (int i = 0; i < 1500; i++) {
            d1[i] = 2 * i;
            d2[i] = 2 * i + 1;
        }
        final double result = this.testStatistic.mannWhitneyUTest(d1, d2);
        Assert.assertTrue(result > 0.1);
    }
}
