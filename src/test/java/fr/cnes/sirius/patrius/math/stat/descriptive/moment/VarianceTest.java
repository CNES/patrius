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
 * VERSION::FA:306:26/11/2014: coverage
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.stat.descriptive.moment;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.stat.descriptive.StorelessUnivariateStatisticAbstractTest;
import fr.cnes.sirius.patrius.math.stat.descriptive.UnivariateStatistic;
import fr.cnes.sirius.patrius.math.util.MathArrays;

/**
 * Test cases for the {@link UnivariateStatistic} class.
 * 
 * @version $Id: VarianceTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class VarianceTest extends StorelessUnivariateStatisticAbstractTest {

    protected Variance stat;

    /**
     * {@inheritDoc}
     */
    @Override
    public UnivariateStatistic getUnivariateStatistic() {
        return new Variance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double expectedValue() {
        return this.var;
    }

    /** Expected value for the testArray defined in UnivariateStatisticAbstractTest */
    public double expectedWeightedValue() {
        return this.weightedVar;
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
        final SecondMoment m = new SecondMoment();
        m.evaluate(values); // side effect is to add values
        Variance v1 = new Variance();
        v1.setBiasCorrected(false);
        Assert.assertEquals(this.populationVariance(values), v1.evaluate(values), 1E-14);
        v1.incrementAll(values);
        Assert.assertEquals(this.populationVariance(values), v1.getResult(), 1E-14);
        v1 = new Variance(false, m);
        Assert.assertEquals(this.populationVariance(values), v1.getResult(), 1E-14);
        v1 = new Variance(false);
        Assert.assertEquals(this.populationVariance(values), v1.evaluate(values), 1E-14);
        v1.incrementAll(values);
        Assert.assertEquals(this.populationVariance(values), v1.getResult(), 1E-14);

        // Coverage of the Variance(Variance) constructor:
        this.stat = new Variance();
        new Variance(this.stat);
        // Coverage of the evaluate(double[], double[], double) method:
        this.stat.evaluate(this.testArray, this.testArray, this.mean);
        // Coverage of the evaluate(double[], double[], double, int, int) method:
        this.stat.setBiasCorrected(false);
        this.stat.evaluate(this.testArray, this.testArray, this.geoMean, 0, 10);
    }

    /**
     * Definitional formula for population variance
     */
    protected double populationVariance(final double[] v) {
        final double mean = new Mean().evaluate(v);
        double sum = 0;
        for (final double element : v) {
            sum += (element - mean) * (element - mean);
        }
        return sum / v.length;
    }

    @Test
    public void testWeightedVariance() {
        final Variance variance = new Variance();
        Assert.assertEquals(this.expectedWeightedValue(),
            variance.evaluate(this.testArray, this.testWeightsArray, 0, this.testArray.length), this.getTolerance());

        // All weights = 1 -> weighted variance = unweighted variance
        Assert.assertEquals(this.expectedValue(),
            variance.evaluate(this.testArray, this.unitWeightsArray, 0, this.testArray.length), this.getTolerance());

        // All weights the same -> when weights are normalized to sum to the length of the values array,
        // weighted variance = unweighted value
        Assert.assertEquals(
            this.expectedValue(),
            variance.evaluate(this.testArray,
                MathArrays.normalizeArray(this.identicalWeightsArray, this.testArray.length),
                0, this.testArray.length), this.getTolerance());

    }

}
