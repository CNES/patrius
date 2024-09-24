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
package fr.cnes.sirius.patrius.math.stat.descriptive.summary;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.stat.descriptive.StorelessUnivariateStatistic;
import fr.cnes.sirius.patrius.math.stat.descriptive.StorelessUnivariateStatisticAbstractTest;
import fr.cnes.sirius.patrius.math.stat.descriptive.UnivariateStatistic;

/**
 * Test cases for the {@link UnivariateStatistic} class.
 * 
 * @version $Id: SumLogTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class SumLogTest extends StorelessUnivariateStatisticAbstractTest {

    protected SumOfLogs stat;

    /**
     * {@inheritDoc}
     */
    @Override
    public UnivariateStatistic getUnivariateStatistic() {
        return new SumOfLogs();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double expectedValue() {
        return this.sumLog;
    }

    @Test
    public void testSpecialValues() {
        final SumOfLogs sum = new SumOfLogs();
        // empty
        Assert.assertEquals(0, sum.getResult(), 0);

        // finite data
        sum.increment(1d);
        Assert.assertFalse(Double.isNaN(sum.getResult()));

        // add negative infinity
        sum.increment(0d);
        Assert.assertEquals(Double.NEGATIVE_INFINITY, sum.getResult(), 0);

        // add positive infinity -- should make NaN
        sum.increment(Double.POSITIVE_INFINITY);
        Assert.assertTrue(Double.isNaN(sum.getResult()));

        // clear
        sum.clear();
        Assert.assertEquals(0, sum.getResult(), 0);

        // positive infinity by itself
        sum.increment(Double.POSITIVE_INFINITY);
        Assert.assertEquals(Double.POSITIVE_INFINITY, sum.getResult(), 0);

        // negative value -- should make NaN
        sum.increment(-2d);
        Assert.assertTrue(Double.isNaN(sum.getResult()));
    }

    @Override
    protected void checkClearValue(final StorelessUnivariateStatistic statistic) {
        Assert.assertEquals(0, statistic.getResult(), 0);
    }

}
