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
 * VERSION::FA:306:26/11/2014: coverage
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.stat.descriptive.moment;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.stat.descriptive.StorelessUnivariateStatisticAbstractTest;
import fr.cnes.sirius.patrius.math.stat.descriptive.UnivariateStatistic;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Test cases for the {@link UnivariateStatistic} class.
 * 
 * @version $Id: StandardDeviationTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class StandardDeviationTest extends StorelessUnivariateStatisticAbstractTest {

    protected StandardDeviation stat;

    /**
     * {@inheritDoc}
     */
    @Override
    public UnivariateStatistic getUnivariateStatistic() {
        return new StandardDeviation();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double expectedValue() {
        return this.std;
    }

    /**
     * Make sure Double.NaN is returned iff n = 0
     * 
     */
    @Test
    public void testNaN() {
        final StandardDeviation std = new StandardDeviation();
        Assert.assertTrue(Double.isNaN(std.getResult()));
        std.increment(1d);
        Assert.assertEquals(0d, std.getResult(), 0);
    }

    /**
     * Test population version of variance
     */
    @Test
    public void testPopulation() {
        final double[] values = { -1.0d, 3.1d, 4.0d, -2.1d, 22d, 11.7d, 3d, 14d };
        final double sigma = this.populationStandardDeviation(values);
        final SecondMoment m = new SecondMoment();
        m.evaluate(values); // side effect is to add values
        StandardDeviation s1 = new StandardDeviation();
        s1.setBiasCorrected(false);
        Assert.assertEquals(sigma, s1.evaluate(values), 1E-14);
        s1.incrementAll(values);
        Assert.assertEquals(sigma, s1.getResult(), 1E-14);
        s1 = new StandardDeviation(false, m);
        Assert.assertEquals(sigma, s1.getResult(), 1E-14);
        s1 = new StandardDeviation(false);
        Assert.assertEquals(sigma, s1.evaluate(values), 1E-14);
        s1.incrementAll(values);
        Assert.assertEquals(sigma, s1.getResult(), 1E-14);

        // Coverage of the StandardDeviation(StandardDeviation) constructor:
        this.stat = new StandardDeviation();
        new StandardDeviation(this.stat);
        // Coverage of the StandardDeviation(SecondMoment) constructor:
        final SecondMoment sm = new SecondMoment();
        new StandardDeviation(sm);
        // Coverage of the isBiasCorrected method:
        Assert.assertTrue(this.stat.isBiasCorrected());
    }

    /**
     * Definitional formula for population standard deviation
     */
    protected double populationStandardDeviation(final double[] v) {
        final double mean = new Mean().evaluate(v);
        double sum = 0;
        for (final double element : v) {
            sum += (element - mean) * (element - mean);
        }
        return MathLib.sqrt(sum / v.length);
    }

}
