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
 * VERSION:4.8:DM:DM-2922:15/11/2021:[PATRIUS] suppression de l'utilisation de la reflexion Java dans patrius 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.stat.descriptive;

import java.util.Locale;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.stat.descriptive.moment.GeometricMean;
import fr.cnes.sirius.patrius.math.stat.descriptive.moment.Mean;
import fr.cnes.sirius.patrius.math.stat.descriptive.moment.Variance;
import fr.cnes.sirius.patrius.math.stat.descriptive.rank.Max;
import fr.cnes.sirius.patrius.math.stat.descriptive.rank.Min;
import fr.cnes.sirius.patrius.math.stat.descriptive.rank.Percentile;
import fr.cnes.sirius.patrius.math.stat.descriptive.summary.Sum;
import fr.cnes.sirius.patrius.math.stat.descriptive.summary.SumOfSquares;
import fr.cnes.sirius.patrius.math.util.Precision;

/**
 * Test cases for the DescriptiveStatistics class.
 * 
 * @version $Id: DescriptiveStatisticsTest.java 18108 2017-10-04 06:45:27Z bignon $
 *          2007) $
 */
public class DescriptiveStatisticsTest {

    protected DescriptiveStatistics createDescriptiveStatistics() {
        return new DescriptiveStatistics();
    }

    @Test
    public void testSetterInjection() {
        final DescriptiveStatistics stats = this.createDescriptiveStatistics();
        stats.addValue(1);
        stats.addValue(3);
        Assert.assertEquals(2, stats.getMean(), 1E-10);
        // Now lets try some new math
        stats.setMeanImpl(new deepMean());
        Assert.assertEquals(42, stats.getMean(), 1E-10);
    }

    @Test
    public void testCopy() {
        final DescriptiveStatistics stats = this.createDescriptiveStatistics();
        stats.addValue(1);
        stats.addValue(3);
        DescriptiveStatistics copy = new DescriptiveStatistics(stats);
        Assert.assertEquals(2, copy.getMean(), 1E-10);
        // Now lets try some new math
        stats.setMeanImpl(new deepMean());
        copy = stats.copy();
        Assert.assertEquals(42, copy.getMean(), 1E-10);
    }

    @Test
    public void testWindowSize() {
        final DescriptiveStatistics stats = this.createDescriptiveStatistics();
        stats.setWindowSize(300);
        for (int i = 0; i < 100; ++i) {
            stats.addValue(i + 1);
        }
        final int refSum = (100 * 101) / 2;
        Assert.assertEquals(refSum / 100.0, stats.getMean(), 1E-10);
        Assert.assertEquals(300, stats.getWindowSize());
        try {
            stats.setWindowSize(-3);
            Assert.fail("an exception should have been thrown");
        } catch (final IllegalArgumentException iae) {
            // expected
        }
        Assert.assertEquals(300, stats.getWindowSize());
        stats.setWindowSize(50);
        Assert.assertEquals(50, stats.getWindowSize());
        final int refSum2 = refSum - (50 * 51) / 2;
        Assert.assertEquals(refSum2 / 50.0, stats.getMean(), 1E-10);
    }

    @Test
    public void testGetValues() {
        final DescriptiveStatistics stats = this.createDescriptiveStatistics();
        for (int i = 100; i > 0; --i) {
            stats.addValue(i);
        }
        final int refSum = (100 * 101) / 2;
        Assert.assertEquals(refSum / 100.0, stats.getMean(), 1E-10);
        final double[] v = stats.getValues();
        for (int i = 0; i < v.length; ++i) {
            Assert.assertEquals(100.0 - i, v[i], 1.0e-10);
        }
        final double[] s = stats.getSortedValues();
        for (int i = 0; i < s.length; ++i) {
            Assert.assertEquals(i + 1.0, s[i], 1.0e-10);
        }
        Assert.assertEquals(12.0, stats.getElement(88), 1.0e-10);
    }

    @Test
    public void testToString() {
        final DescriptiveStatistics stats = this.createDescriptiveStatistics();
        stats.addValue(1);
        stats.addValue(2);
        stats.addValue(3);
        final Locale d = Locale.getDefault();
        Locale.setDefault(Locale.US);
        Assert.assertEquals("DescriptiveStatistics:\n" +
            "n: 3\n" +
            "min: 1.0\n" +
            "max: 3.0\n" +
            "mean: 2.0\n" +
            "std dev: 1.0\n" +
            "median: 2.0\n" +
            "skewness: 0.0\n" +
            "kurtosis: NaN\n", stats.toString());
        Locale.setDefault(d);
    }

    @Test
    public void testShuffledStatistics() {
        // the purpose of this test is only to check the get/set methods
        // we are aware shuffling statistics like this is really not
        // something sensible to do in production ...
        final DescriptiveStatistics reference = this.createDescriptiveStatistics();
        final DescriptiveStatistics shuffled = this.createDescriptiveStatistics();

        final UnivariateStatistic tmp = shuffled.getGeometricMeanImpl();
        shuffled.setGeometricMeanImpl(shuffled.getMeanImpl());
        shuffled.setMeanImpl(shuffled.getKurtosisImpl());
        shuffled.setKurtosisImpl(shuffled.getSkewnessImpl());
        shuffled.setSkewnessImpl(shuffled.getVarianceImpl());
        shuffled.setVarianceImpl(shuffled.getMaxImpl());
        shuffled.setMaxImpl(shuffled.getMinImpl());
        shuffled.setMinImpl(shuffled.getSumImpl());
        shuffled.setSumImpl(shuffled.getSumsqImpl());
        shuffled.setSumsqImpl(tmp);

        for (int i = 100; i > 0; --i) {
            reference.addValue(i);
            shuffled.addValue(i);
        }

        Assert.assertEquals(reference.getMean(), shuffled.getGeometricMean(), 1.0e-10);
        Assert.assertEquals(reference.getKurtosis(), shuffled.getMean(), 1.0e-10);
        Assert.assertEquals(reference.getSkewness(), shuffled.getKurtosis(), 1.0e-10);
        Assert.assertEquals(reference.getVariance(), shuffled.getSkewness(), 1.0e-10);
        Assert.assertEquals(reference.getMax(), shuffled.getVariance(), 1.0e-10);
        Assert.assertEquals(reference.getMin(), shuffled.getMax(), 1.0e-10);
        Assert.assertEquals(reference.getSum(), shuffled.getMin(), 1.0e-10);
        Assert.assertEquals(reference.getSumsq(), shuffled.getSum(), 1.0e-10);
        Assert.assertEquals(reference.getGeometricMean(), shuffled.getSumsq(), 1.0e-10);

    }

    @Test
    public void testPercentileSetter() {
        final DescriptiveStatistics stats = this.createDescriptiveStatistics();
        stats.addValue(1);
        stats.addValue(2);
        stats.addValue(3);
        Assert.assertEquals(2, stats.getPercentile(50.0), 1E-10);

        // Try "new math" impl
        stats.setPercentileImpl(new subPercentile());
        Assert.assertEquals(10.0, stats.getPercentile(10.0), 1E-10);

        // Try to set bad impl
        try {
            stats.setPercentileImpl(new badPercentile());
            Assert.fail("Expecting IllegalArgumentException");
        } catch (final IllegalArgumentException ex) {
            // expected
        }
    }

    @Test
    public void test20090720() {
        final DescriptiveStatistics descriptiveStatistics = new DescriptiveStatistics(100);
        for (int i = 0; i < 161; i++) {
            descriptiveStatistics.addValue(1.2);
        }
        descriptiveStatistics.clear();
        descriptiveStatistics.addValue(1.2);
        Assert.assertEquals(1, descriptiveStatistics.getN());
    }

    @Test
    public void testRemoval() {

        final DescriptiveStatistics dstat = this.createDescriptiveStatistics();

        this.checkremoval(dstat, 1, 6.0, 0.0, Double.NaN);
        this.checkremoval(dstat, 3, 5.0, 3.0, 4.5);
        this.checkremoval(dstat, 6, 3.5, 2.5, 3.0);
        this.checkremoval(dstat, 9, 3.5, 2.5, 3.0);
        this.checkremoval(dstat, DescriptiveStatistics.INFINITE_WINDOW, 3.5, 2.5, 3.0);

    }

    @Test
    public void testSummaryConsistency() {
        final DescriptiveStatistics dstats = new DescriptiveStatistics();
        final SummaryStatistics sstats = new SummaryStatistics();
        final int windowSize = 5;
        dstats.setWindowSize(windowSize);
        final double tol = 1E-12;
        for (int i = 0; i < 20; i++) {
            dstats.addValue(i);
            sstats.clear();
            final double[] values = dstats.getValues();
            for (final double value : values) {
                sstats.addValue(value);
            }
            TestUtils.assertEquals(dstats.getMean(), sstats.getMean(), tol);
            TestUtils.assertEquals(new Mean().evaluate(values), dstats.getMean(), tol);
            TestUtils.assertEquals(dstats.getMax(), sstats.getMax(), tol);
            TestUtils.assertEquals(new Max().evaluate(values), dstats.getMax(), tol);
            TestUtils.assertEquals(dstats.getGeometricMean(), sstats.getGeometricMean(), tol);
            TestUtils.assertEquals(new GeometricMean().evaluate(values), dstats.getGeometricMean(), tol);
            TestUtils.assertEquals(dstats.getMin(), sstats.getMin(), tol);
            TestUtils.assertEquals(new Min().evaluate(values), dstats.getMin(), tol);
            TestUtils.assertEquals(dstats.getStandardDeviation(), sstats.getStandardDeviation(), tol);
            TestUtils.assertEquals(dstats.getVariance(), sstats.getVariance(), tol);
            TestUtils.assertEquals(new Variance().evaluate(values), dstats.getVariance(), tol);
            TestUtils.assertEquals(dstats.getSum(), sstats.getSum(), tol);
            TestUtils.assertEquals(new Sum().evaluate(values), dstats.getSum(), tol);
            TestUtils.assertEquals(dstats.getSumsq(), sstats.getSumsq(), tol);
            TestUtils.assertEquals(new SumOfSquares().evaluate(values), dstats.getSumsq(), tol);
            TestUtils.assertEquals(dstats.getPopulationVariance(), sstats.getPopulationVariance(), tol);
            TestUtils.assertEquals(new Variance(false).evaluate(values), dstats.getPopulationVariance(), tol);
        }
    }

    public void checkremoval(final DescriptiveStatistics dstat, final int wsize,
                             final double mean1, final double mean2, final double mean3) {

        dstat.setWindowSize(wsize);
        dstat.clear();

        for (int i = 1; i <= 6; ++i) {
            dstat.addValue(i);
        }

        Assert.assertTrue(Precision.equalsIncludingNaN(mean1, dstat.getMean()));
        dstat.replaceMostRecentValue(0);
        Assert.assertTrue(Precision.equalsIncludingNaN(mean2, dstat.getMean()));
        dstat.removeMostRecentValue();
        Assert.assertTrue(Precision.equalsIncludingNaN(mean3, dstat.getMean()));

    }

    // Test UnivariateStatistics impls for setter injection tests

    /**
     * A new way to compute the mean
     */
    static class deepMean implements UnivariateStatistic {

        @Override
        public double evaluate(final double[] values, final int begin, final int length) {
            return 42;
        }

        @Override
        public double evaluate(final double[] values) {
            return 42;
        }

        @Override
        public UnivariateStatistic copy() {
            return new deepMean();
        }
    }

    /**
     * Test percentile implementation - wraps a Percentile
     */
    static class goodPercentile implements UnivariateStatistic {
        private final Percentile percentile = new Percentile();

        public void setQuantile(final double quantile) {
            this.percentile.setQuantile(quantile);
        }

        @Override
        public double evaluate(final double[] values, final int begin, final int length) {
            return this.percentile.evaluate(values, begin, length);
        }

        @Override
        public double evaluate(final double[] values) {
            return this.percentile.evaluate(values);
        }

        @Override
        public UnivariateStatistic copy() {
            final goodPercentile result = new goodPercentile();
            result.setQuantile(this.percentile.getQuantile());
            return result;
        }
    }

    /**
     * Test percentile subclass - another "new math" impl
     * Always returns currently set quantile
     */
    static class subPercentile extends Percentile {
        @Override
        public double evaluate(final double[] values, final int begin, final int length) {
            return this.getQuantile();
        }

        @Override
        public double evaluate(final double[] values) {
            return this.getQuantile();
        }

        private static final long serialVersionUID = 8040701391045914979L;

        @Override
        public Percentile copy() {
            final subPercentile result = new subPercentile();
            return result;
        }
    }

    /**
     * "Bad" test percentile implementation - no setQuantile
     */
    static class badPercentile implements UnivariateStatistic {
        private final Percentile percentile = new Percentile();

        @Override
        public double evaluate(final double[] values, final int begin, final int length) {
            return this.percentile.evaluate(values, begin, length);
        }

        @Override
        public double evaluate(final double[] values) {
            return this.percentile.evaluate(values);
        }

        @Override
        public UnivariateStatistic copy() {
            return new badPercentile();
        }
    }

}
