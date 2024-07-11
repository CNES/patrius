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
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.stat.descriptive;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.distribution.IntegerDistribution;
import fr.cnes.sirius.patrius.math.distribution.NormalDistribution;
import fr.cnes.sirius.patrius.math.distribution.RealDistribution;
import fr.cnes.sirius.patrius.math.distribution.UniformIntegerDistribution;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Test cases for the {@link UnivariateStatistic} class.
 * 
 * @version $Id: UnivariateStatisticAbstractTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
public abstract class UnivariateStatisticAbstractTest {

    protected double mean = 12.404545454545455d;
    protected double geoMean = 12.070589161633011d;

    protected double var = 10.00235930735931d;
    protected double std = MathLib.sqrt(this.var);
    protected double skew = 1.437423729196190d;
    protected double kurt = 2.377191264804700d;

    protected double min = 8.2d;
    protected double max = 21d;
    protected double median = 12d;
    protected double percentile5 = 8.29d;
    protected double percentile95 = 20.82d;

    protected double product = 628096400563833396009676.9200400128d;
    protected double sumLog = 54.7969806116451507d;
    protected double sumSq = 3595.250d;
    protected double sum = 272.90d;
    protected double secondMoment = 210.04954545454547d;
    protected double thirdMoment = 868.0906859504136;
    protected double fourthMoment = 9244.080993773481;

    protected double weightedMean = 12.366995073891626d;
    protected double weightedVar = 9.974760968886391d;
    protected double weightedStd = MathLib.sqrt(this.weightedVar);
    protected double weightedProduct = 8517647448765288000000d;
    protected double weightedSum = 251.05d;

    protected double tolerance = 10E-12;

    protected double[] testArray =
    { 12.5, 12.0, 11.8, 14.2, 14.9, 14.5, 21.0, 8.2, 10.3, 11.3,
        14.1, 9.9, 12.2, 12.0, 12.1, 11.0, 19.8, 11.0, 10.0, 8.8,
        9.0, 12.3 };

    protected double[] testWeightsArray =
    { 1.5, 0.8, 1.2, 0.4, 0.8, 1.8, 1.2, 1.1, 1.0, 0.7,
        1.3, 0.6, 0.7, 1.3, 0.7, 1.0, 0.4, 0.1, 1.4, 0.9,
        1.1, 0.3 };

    protected double[] identicalWeightsArray =
    { 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5,
        0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5,
        0.5, 0.5 };

    protected double[] unitWeightsArray =
    { 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0,
        1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0,
        1.0, 1.0 };

    public abstract UnivariateStatistic getUnivariateStatistic();

    public abstract double expectedValue();

    public double getTolerance() {
        return this.tolerance;
    }

    @Test
    public void testEvaluation() {
        Assert.assertEquals(
            this.expectedValue(),
            this.getUnivariateStatistic().evaluate(this.testArray),
            this.getTolerance());
    }

    @Test
    public void testEvaluateArraySegment() {
        final UnivariateStatistic stat = this.getUnivariateStatistic();
        final double[] arrayZero = new double[5];
        System.arraycopy(this.testArray, 0, arrayZero, 0, 5);
        Assert.assertEquals(stat.evaluate(arrayZero), stat.evaluate(this.testArray, 0, 5), 0);
        final double[] arrayOne = new double[5];
        System.arraycopy(this.testArray, 5, arrayOne, 0, 5);
        Assert.assertEquals(stat.evaluate(arrayOne), stat.evaluate(this.testArray, 5, 5), 0);
        final double[] arrayEnd = new double[5];
        System.arraycopy(this.testArray, this.testArray.length - 5, arrayEnd, 0, 5);
        Assert.assertEquals(stat.evaluate(arrayEnd), stat.evaluate(this.testArray, this.testArray.length - 5, 5), 0);
    }

    @Test
    public void testEvaluateArraySegmentWeighted() {
        // See if this statistic computes weighted statistics
        // If not, skip this test
        final UnivariateStatistic statistic = this.getUnivariateStatistic();
        if (!(statistic instanceof WeightedEvaluation)) {
            return;
        }
        final WeightedEvaluation stat = (WeightedEvaluation) this.getUnivariateStatistic();
        final double[] arrayZero = new double[5];
        final double[] weightZero = new double[5];
        System.arraycopy(this.testArray, 0, arrayZero, 0, 5);
        System.arraycopy(this.testWeightsArray, 0, weightZero, 0, 5);
        Assert.assertEquals(stat.evaluate(arrayZero, weightZero),
            stat.evaluate(this.testArray, this.testWeightsArray, 0, 5), 0);
        final double[] arrayOne = new double[5];
        final double[] weightOne = new double[5];
        System.arraycopy(this.testArray, 5, arrayOne, 0, 5);
        System.arraycopy(this.testWeightsArray, 5, weightOne, 0, 5);
        Assert.assertEquals(stat.evaluate(arrayOne, weightOne),
            stat.evaluate(this.testArray, this.testWeightsArray, 5, 5), 0);
        final double[] arrayEnd = new double[5];
        final double[] weightEnd = new double[5];
        System.arraycopy(this.testArray, this.testArray.length - 5, arrayEnd, 0, 5);
        System.arraycopy(this.testWeightsArray, this.testArray.length - 5, weightEnd, 0, 5);
        Assert.assertEquals(stat.evaluate(arrayEnd, weightEnd),
            stat.evaluate(this.testArray, this.testWeightsArray, this.testArray.length - 5, 5), 0);
    }

    @Test
    public void testCopy() {
        final UnivariateStatistic original = this.getUnivariateStatistic();
        final UnivariateStatistic copy = original.copy();
        Assert.assertEquals(
            this.expectedValue(),
            copy.evaluate(this.testArray),
            this.getTolerance());
    }

    /**
     * Tests consistency of weighted statistic computation.
     * For statistics that support weighted evaluation, this test case compares
     * the result of direct computation on an array with repeated values with
     * a weighted computation on the corresponding (shorter) array with each
     * value appearing only once but with a weight value equal to its multiplicity
     * in the repeating array.
     */

    @Test
    public void testWeightedConsistency() {

        // See if this statistic computes weighted statistics
        // If not, skip this test
        final UnivariateStatistic statistic = this.getUnivariateStatistic();
        if (!(statistic instanceof WeightedEvaluation)) {
            return;
        }

        // Create arrays of values and corresponding integral weights
        // and longer array with values repeated according to the weights
        final int len = 10; // length of values array
        final double mu = 0; // mean of test data
        final double sigma = 5; // std dev of test data
        final double[] values = new double[len];
        final double[] weights = new double[len];

        // Fill weights array with random int values between 1 and 5
        final int[] intWeights = new int[len];
        final IntegerDistribution weightDist = new UniformIntegerDistribution(1, 5);
        for (int i = 0; i < len; i++) {
            intWeights[i] = weightDist.sample();
            weights[i] = intWeights[i];
        }

        // Fill values array with random data from N(mu, sigma)
        // and fill valuesList with values from values array with
        // values[i] repeated weights[i] times, each i
        final RealDistribution valueDist = new NormalDistribution(mu, sigma);
        final List<Double> valuesList = new ArrayList<Double>();
        for (int i = 0; i < len; i++) {
            final double value = valueDist.sample();
            values[i] = value;
            for (int j = 0; j < intWeights[i]; j++) {
                valuesList.add(new Double(value));
            }
        }

        // Dump valuesList into repeatedValues array
        final int sumWeights = valuesList.size();
        final double[] repeatedValues = new double[sumWeights];
        for (int i = 0; i < sumWeights; i++) {
            repeatedValues[i] = valuesList.get(i);
        }

        // Compare result of weighted statistic computation with direct computation
        // on array of repeated values
        final WeightedEvaluation weightedStatistic = (WeightedEvaluation) statistic;
        TestUtils.assertRelativelyEquals(statistic.evaluate(repeatedValues),
            weightedStatistic.evaluate(values, weights, 0, values.length),
            10E-12);

        // Check consistency of weighted evaluation methods
        Assert.assertEquals(weightedStatistic.evaluate(values, weights, 0, values.length),
            weightedStatistic.evaluate(values, weights), Double.MIN_VALUE);

    }

}
