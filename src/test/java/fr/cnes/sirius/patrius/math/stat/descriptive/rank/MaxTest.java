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
package fr.cnes.sirius.patrius.math.stat.descriptive.rank;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.stat.descriptive.StorelessUnivariateStatisticAbstractTest;
import fr.cnes.sirius.patrius.math.stat.descriptive.UnivariateStatistic;

/**
 * Test cases for the {@link UnivariateStatistic} class.
 * 
 * @version $Id: MaxTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class MaxTest extends StorelessUnivariateStatisticAbstractTest {

    protected Max stat;

    /**
     * {@inheritDoc}
     */
    @Override
    public UnivariateStatistic getUnivariateStatistic() {
        return new Max();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double expectedValue() {
        return this.max;
    }

    @Test
    public void testSpecialValues() {
        final double[] testArray = { 0d, Double.NaN, Double.NEGATIVE_INFINITY,
            Double.POSITIVE_INFINITY };
        final Max max = new Max();
        Assert.assertTrue(Double.isNaN(max.getResult()));
        max.increment(testArray[0]);
        Assert.assertEquals(0d, max.getResult(), 0);
        max.increment(testArray[1]);
        Assert.assertEquals(0d, max.getResult(), 0);
        max.increment(testArray[2]);
        Assert.assertEquals(0d, max.getResult(), 0);
        max.increment(testArray[3]);
        Assert.assertEquals(Double.POSITIVE_INFINITY, max.getResult(), 0);
        Assert.assertEquals(Double.POSITIVE_INFINITY, max.evaluate(testArray), 0);
    }

    @Test
    public void testNaNs() {
        final Max max = new Max();
        final double nan = Double.NaN;
        Assert.assertEquals(3d, max.evaluate(new double[] { nan, 2d, 3d }), 0);
        Assert.assertEquals(3d, max.evaluate(new double[] { 1d, nan, 3d }), 0);
        Assert.assertEquals(2d, max.evaluate(new double[] { 1d, 2d, nan }), 0);
        Assert.assertTrue(Double.isNaN(max.evaluate(new double[] { nan, nan, nan })));
    }

}
