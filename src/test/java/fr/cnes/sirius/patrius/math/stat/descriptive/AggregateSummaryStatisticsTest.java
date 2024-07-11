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
package fr.cnes.sirius.patrius.math.stat.descriptive;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.distribution.IntegerDistribution;
import fr.cnes.sirius.patrius.math.distribution.RealDistribution;
import fr.cnes.sirius.patrius.math.distribution.UniformIntegerDistribution;
import fr.cnes.sirius.patrius.math.distribution.UniformRealDistribution;
import fr.cnes.sirius.patrius.math.util.Precision;

/**
 * Test cases for {@link AggregateSummaryStatistics}
 * 
 */
public class AggregateSummaryStatisticsTest {

    /**
     * Tests the standard aggregation behavior
     */
    @Test
    public void testAggregation() {
        final AggregateSummaryStatistics aggregate = new AggregateSummaryStatistics();
        final SummaryStatistics setOneStats = aggregate.createContributingStatistics();
        final SummaryStatistics setTwoStats = aggregate.createContributingStatistics();

        Assert.assertNotNull("The set one contributing stats are null", setOneStats);
        Assert.assertNotNull("The set two contributing stats are null", setTwoStats);
        Assert.assertNotSame("Contributing stats objects are the same", setOneStats, setTwoStats);

        setOneStats.addValue(2);
        setOneStats.addValue(3);
        setOneStats.addValue(5);
        setOneStats.addValue(7);
        setOneStats.addValue(11);
        Assert.assertEquals("Wrong number of set one values", 5, setOneStats.getN());
        Assert.assertTrue("Wrong sum of set one values", Precision.equals(28.0, setOneStats.getSum(), 1));

        setTwoStats.addValue(2);
        setTwoStats.addValue(4);
        setTwoStats.addValue(8);
        Assert.assertEquals("Wrong number of set two values", 3, setTwoStats.getN());
        Assert.assertTrue("Wrong sum of set two values", Precision.equals(14.0, setTwoStats.getSum(), 1));

        Assert.assertEquals("Wrong number of aggregate values", 8, aggregate.getN());
        Assert.assertTrue("Wrong aggregate sum", Precision.equals(42.0, aggregate.getSum(), 1));
    }

    /**
     * Verify that aggregating over a partition gives the same results
     * as direct computation.
     * 
     * 1) Randomly generate a dataset of 10-100 values
     * from [-100, 100]
     * 2) Divide the dataset it into 2-5 partitions
     * 3) Create an AggregateSummaryStatistic and ContributingStatistics
     * for each partition
     * 4) Compare results from the AggregateSummaryStatistic with values
     * returned by a single SummaryStatistics instance that is provided
     * the full dataset
     */
    @Test
    public void testAggregationConsistency() {

        // Generate a random sample and random partition
        final double[] totalSample = this.generateSample();
        final double[][] subSamples = this.generatePartition(totalSample);
        final int nSamples = subSamples.length;

        // Create aggregator and total stats for comparison
        final AggregateSummaryStatistics aggregate = new AggregateSummaryStatistics();
        final SummaryStatistics totalStats = new SummaryStatistics();

        // Create array of component stats
        final SummaryStatistics componentStats[] = new SummaryStatistics[nSamples];

        for (int i = 0; i < nSamples; i++) {

            // Make componentStats[i] a contributing statistic to aggregate
            componentStats[i] = aggregate.createContributingStatistics();

            // Add values from subsample
            for (int j = 0; j < subSamples[i].length; j++) {
                componentStats[i].addValue(subSamples[i][j]);
            }
        }

        // Compute totalStats directly
        for (final double element : totalSample) {
            totalStats.addValue(element);
        }

        /*
         * Compare statistics in totalStats with aggregate.
         * Note that guaranteed success of this comparison depends on the
         * fact that <aggregate> gets values in exactly the same order
         * as <totalStats>.
         */
        Assert.assertEquals(totalStats.getSummary(), aggregate.getSummary());

    }

    /**
     * Test aggregate function by randomly generating a dataset of 10-100 values
     * from [-100, 100], dividing it into 2-5 partitions, computing stats for each
     * partition and comparing the result of aggregate(...) applied to the collection
     * of per-partition SummaryStatistics with a single SummaryStatistics computed
     * over the full sample.
     * 
     */
    @Test
    public void testAggregate() {

        // Generate a random sample and random partition
        final double[] totalSample = this.generateSample();
        final double[][] subSamples = this.generatePartition(totalSample);
        final int nSamples = subSamples.length;

        // Compute combined stats directly
        final SummaryStatistics totalStats = new SummaryStatistics();
        for (final double element : totalSample) {
            totalStats.addValue(element);
        }

        // Now compute subsample stats individually and aggregate
        final SummaryStatistics[] subSampleStats = new SummaryStatistics[nSamples];
        for (int i = 0; i < nSamples; i++) {
            subSampleStats[i] = new SummaryStatistics();
        }
        final Collection<SummaryStatistics> aggregate = new ArrayList<SummaryStatistics>();
        for (int i = 0; i < nSamples; i++) {
            for (int j = 0; j < subSamples[i].length; j++) {
                subSampleStats[i].addValue(subSamples[i][j]);
            }
            aggregate.add(subSampleStats[i]);
        }

        // Compare values
        final StatisticalSummary aggregatedStats = AggregateSummaryStatistics.aggregate(aggregate);
        assertEquals(totalStats.getSummary(), aggregatedStats, 10E-12);
    }

    @Test
    public void testAggregateDegenerate() {
        final double[] totalSample = { 1, 2, 3, 4, 5 };
        final double[][] subSamples = { { 1 }, { 2 }, { 3 }, { 4 }, { 5 } };

        // Compute combined stats directly
        final SummaryStatistics totalStats = new SummaryStatistics();
        for (final double element : totalSample) {
            totalStats.addValue(element);
        }

        // Now compute subsample stats individually and aggregate
        final SummaryStatistics[] subSampleStats = new SummaryStatistics[5];
        for (int i = 0; i < 5; i++) {
            subSampleStats[i] = new SummaryStatistics();
        }
        final Collection<SummaryStatistics> aggregate = new ArrayList<SummaryStatistics>();
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < subSamples[i].length; j++) {
                subSampleStats[i].addValue(subSamples[i][j]);
            }
            aggregate.add(subSampleStats[i]);
        }

        // Compare values
        final StatisticalSummaryValues aggregatedStats = AggregateSummaryStatistics.aggregate(aggregate);
        assertEquals(totalStats.getSummary(), aggregatedStats, 10E-12);
    }

    @Test
    public void testAggregateSpecialValues() {
        final double[] totalSample = { Double.POSITIVE_INFINITY, 2, 3, Double.NaN, 5 };
        final double[][] subSamples = { { Double.POSITIVE_INFINITY, 2 }, { 3 }, { Double.NaN }, { 5 } };

        // Compute combined stats directly
        final SummaryStatistics totalStats = new SummaryStatistics();
        for (final double element : totalSample) {
            totalStats.addValue(element);
        }

        // Now compute subsample stats individually and aggregate
        final SummaryStatistics[] subSampleStats = new SummaryStatistics[5];
        for (int i = 0; i < 4; i++) {
            subSampleStats[i] = new SummaryStatistics();
        }
        final Collection<SummaryStatistics> aggregate = new ArrayList<SummaryStatistics>();
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < subSamples[i].length; j++) {
                subSampleStats[i].addValue(subSamples[i][j]);
            }
            aggregate.add(subSampleStats[i]);
        }

        // Compare values
        final StatisticalSummaryValues aggregatedStats = AggregateSummaryStatistics.aggregate(aggregate);
        assertEquals(totalStats.getSummary(), aggregatedStats, 10E-12);

    }

    /**
     * Verifies that a StatisticalSummary and a StatisticalSummaryValues are equal up
     * to delta, with NaNs, infinities returned in the same spots. For max, min, n, values
     * have to agree exactly, delta is used only for sum, mean, variance, std dev.
     */
    protected static void assertEquals(final StatisticalSummary expected, final StatisticalSummary observed,
                                       final double delta) {
        TestUtils.assertEquals(expected.getMax(), observed.getMax(), 0);
        TestUtils.assertEquals(expected.getMin(), observed.getMin(), 0);
        Assert.assertEquals(expected.getN(), observed.getN());
        TestUtils.assertEquals(expected.getSum(), observed.getSum(), delta);
        TestUtils.assertEquals(expected.getMean(), observed.getMean(), delta);
        TestUtils.assertEquals(expected.getStandardDeviation(), observed.getStandardDeviation(), delta);
        TestUtils.assertEquals(expected.getVariance(), observed.getVariance(), delta);
    }

    /**
     * Generates a random sample of double values.
     * Sample size is random, between 10 and 100 and values are
     * uniformly distributed over [-100, 100].
     * 
     * @return array of random double values
     */
    private double[] generateSample() {
        final IntegerDistribution size = new UniformIntegerDistribution(10, 100);
        final RealDistribution randomData = new UniformRealDistribution(-100, 100);
        final int sampleSize = size.sample();
        final double[] out = randomData.sample(sampleSize);
        return out;
    }

    /**
     * Generates a partition of <sample> into up to 5 sequentially selected
     * subsamples with randomly selected partition points.
     * 
     * @param sample
     *        array to partition
     * @return rectangular array with rows = subsamples
     */
    private double[][] generatePartition(final double[] sample) {
        final int length = sample.length;
        final double[][] out = new double[5][];
        int cur = 0; // beginning of current partition segment
        int offset = 0; // end of current partition segment
        int sampleCount = 0; // number of segments defined
        for (int i = 0; i < 5; i++) {
            if (cur == length || offset == length) {
                break;
            }
            final int next;
            if (i == 4 || cur == length - 1) {
                next = length - 1;
            } else {
                next = (new UniformIntegerDistribution(cur, length - 1)).sample();
            }
            final int subLength = next - cur + 1;
            out[i] = new double[subLength];
            System.arraycopy(sample, offset, out[i], 0, subLength);
            cur = next + 1;
            sampleCount++;
            offset += subLength;
        }
        if (sampleCount < 5) {
            final double[][] out2 = new double[sampleCount][];
            for (int j = 0; j < sampleCount; j++) {
                final int curSize = out[j].length;
                out2[j] = new double[curSize];
                System.arraycopy(out[j], 0, out2[j], 0, curSize);
            }
            return out2;
        } else {
            return out;
        }
    }

}
