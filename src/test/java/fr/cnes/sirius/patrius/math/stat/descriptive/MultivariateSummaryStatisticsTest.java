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
 * VERSION:4.8:DM:DM-3040:15/11/2021:[PATRIUS]Reversement des evolutions de la branche patrius-for-lotus 
 * VERSION:4.5:DM:DM-2300:27/05/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.stat.descriptive;

import java.util.Locale;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.stat.descriptive.moment.Mean;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Test cases for the {@link MultivariateSummaryStatistics} class.
 * 
 * @version $Id: MultivariateSummaryStatisticsTest.java 18108 2017-10-04 06:45:27Z bignon $
 */

public class MultivariateSummaryStatisticsTest {

    protected MultivariateSummaryStatistics
            createMultivariateSummaryStatistics(final int k, final boolean isCovarianceBiasCorrected) {
        return new MultivariateSummaryStatistics(k, isCovarianceBiasCorrected);
    }

    @Test
    public void testSetterInjection() {
        final MultivariateSummaryStatistics u = this.createMultivariateSummaryStatistics(2, true);
        u.setMeanImpl(new StorelessUnivariateStatistic[] {
            new sumMean(), new sumMean()
        });
        u.addValue(new double[] { 1, 2 });
        u.addValue(new double[] { 3, 4 });
        Assert.assertEquals(4, u.getMean()[0], 1E-14);
        Assert.assertEquals(6, u.getMean()[1], 1E-14);
        u.clear();
        u.addValue(new double[] { 1, 2 });
        u.addValue(new double[] { 3, 4 });
        Assert.assertEquals(4, u.getMean()[0], 1E-14);
        Assert.assertEquals(6, u.getMean()[1], 1E-14);
        u.clear();
        u.setMeanImpl(new StorelessUnivariateStatistic[] {
            new Mean(), new Mean()
        }); // OK after clear
        u.addValue(new double[] { 1, 2 });
        u.addValue(new double[] { 3, 4 });
        Assert.assertEquals(2, u.getMean()[0], 1E-14);
        Assert.assertEquals(3, u.getMean()[1], 1E-14);
        Assert.assertEquals(2, u.getDimension());
    }

    @Test
    public void testSetterIllegalState() {
        final MultivariateSummaryStatistics u = this.createMultivariateSummaryStatistics(2, true);
        u.addValue(new double[] { 1, 2 });
        u.addValue(new double[] { 3, 4 });
        try {
            u.setMeanImpl(new StorelessUnivariateStatistic[] {
                new sumMean(), new sumMean()
            });
            Assert.fail("Expecting IllegalStateException");
        } catch (final IllegalStateException ex) {
            // expected
        }
    }

    @Test
    public void testToString() {
        final MultivariateSummaryStatistics stats = this.createMultivariateSummaryStatistics(2, true);
        stats.addValue(new double[] { 1, 3 });
        stats.addValue(new double[] { 2, 2 });
        stats.addValue(new double[] { 3, 1 });
        final Locale d = Locale.getDefault();
        Locale.setDefault(Locale.US);
        final String suffix = System.getProperty("line.separator");

        Assert.assertEquals("MultivariateSummaryStatistics:" + suffix + "n: 3" + suffix
                + "min: 1.0, 1.0" + suffix + "max: 3.0, 3.0" + suffix + "mean: 2.0, 2.0" + suffix
                + "geometric mean: 1.817..., 1.817..." + suffix + "sum of squares: 14.0, 14.0"
                + suffix + "sum of logarithms: 1.791..., 1.791..." + suffix
                + "standard deviation: 1.0, 1.0" + suffix
                + "covariance: Array2DRowRealMatrix{{1, -1}, {-1, 1}}" + suffix, stats.toString()
                .replaceAll("([0-9]+\\.[0-9][0-9][0-9])[0-9]+", "$1..."));
        Locale.setDefault(d);
    }

    @Test
    public void testShuffledStatistics() {
        // the purpose of this test is only to check the get/set methods
        // we are aware shuffling statistics like this is really not
        // something sensible to do in production ...
        final MultivariateSummaryStatistics reference = this.createMultivariateSummaryStatistics(2, true);
        final MultivariateSummaryStatistics shuffled = this.createMultivariateSummaryStatistics(2, true);

        final StorelessUnivariateStatistic[] tmp = shuffled.getGeoMeanImpl();
        shuffled.setGeoMeanImpl(shuffled.getMeanImpl());
        shuffled.setMeanImpl(shuffled.getMaxImpl());
        shuffled.setMaxImpl(shuffled.getMinImpl());
        shuffled.setMinImpl(shuffled.getSumImpl());
        shuffled.setSumImpl(shuffled.getSumsqImpl());
        shuffled.setSumsqImpl(shuffled.getSumLogImpl());
        shuffled.setSumLogImpl(tmp);

        for (int i = 100; i > 0; --i) {
            reference.addValue(new double[] { i, i });
            shuffled.addValue(new double[] { i, i });
        }

        TestUtils.assertEquals(reference.getMean(), shuffled.getGeometricMean(), 1.0e-10);
        TestUtils.assertEquals(reference.getMax(), shuffled.getMean(), 1.0e-10);
        TestUtils.assertEquals(reference.getMin(), shuffled.getMax(), 1.0e-10);
        TestUtils.assertEquals(reference.getSum(), shuffled.getMin(), 1.0e-10);
        TestUtils.assertEquals(reference.getSumSq(), shuffled.getSum(), 1.0e-10);
        TestUtils.assertEquals(reference.getSumLog(), shuffled.getSumSq(), 1.0e-10);
        TestUtils.assertEquals(reference.getGeometricMean(), shuffled.getSumLog(), 1.0e-10);

    }

    /**
     * Bogus mean implementation to test setter injection.
     * Returns the sum instead of the mean.
     */
    static class sumMean implements StorelessUnivariateStatistic {
        private double sum = 0;
        private long n = 0;

        @Override
        public double evaluate(final double[] values, final int begin, final int length) {
            return 0;
        }

        @Override
        public double evaluate(final double[] values) {
            return 0;
        }

        @Override
        public void clear() {
            this.sum = 0;
            this.n = 0;
        }

        @Override
        public long getN() {
            return this.n;
        }

        @Override
        public double getResult() {
            return this.sum;
        }

        @Override
        public void increment(final double d) {
            this.sum += d;
            this.n++;
        }

        @Override
        public void incrementAll(final double[] values, final int start, final int length) {
        }

        @Override
        public void incrementAll(final double[] values) {
        }

        @Override
        public StorelessUnivariateStatistic copy() {
            return new sumMean();
        }
    }

    @Test
    public void testDimension() {
        try {
            this.createMultivariateSummaryStatistics(2, true).addValue(new double[3]);
            Assert.fail("Expecting DimensionMismatchException");
        } catch (final DimensionMismatchException dme) {
            // expected behavior
        }
    }

    /** test stats */
    @Test
    public void testStats() {
        final MultivariateSummaryStatistics u = this.createMultivariateSummaryStatistics(2, true);
        Assert.assertEquals(0, u.getN());
        u.addValue(new double[] { 1, 2 });
        u.addValue(new double[] { 2, 3 });
        u.addValue(new double[] { 2, 3 });
        u.addValue(new double[] { 3, 4 });
        Assert.assertEquals(4, u.getN());
        Assert.assertEquals(8, u.getSum()[0], 1.0e-10);
        Assert.assertEquals(12, u.getSum()[1], 1.0e-10);
        Assert.assertEquals(18, u.getSumSq()[0], 1.0e-10);
        Assert.assertEquals(38, u.getSumSq()[1], 1.0e-10);
        Assert.assertEquals(1, u.getMin()[0], 1.0e-10);
        Assert.assertEquals(2, u.getMin()[1], 1.0e-10);
        Assert.assertEquals(3, u.getMax()[0], 1.0e-10);
        Assert.assertEquals(4, u.getMax()[1], 1.0e-10);
        Assert.assertEquals(2.4849066497880003102, u.getSumLog()[0], 1.0e-10);
        Assert.assertEquals(4.276666119016055311, u.getSumLog()[1], 1.0e-10);
        Assert.assertEquals(1.8612097182041991979, u.getGeometricMean()[0], 1.0e-10);
        Assert.assertEquals(2.9129506302439405217, u.getGeometricMean()[1], 1.0e-10);
        Assert.assertEquals(2, u.getMean()[0], 1.0e-10);
        Assert.assertEquals(3, u.getMean()[1], 1.0e-10);
        Assert.assertEquals(MathLib.sqrt(2.0 / 3.0), u.getStandardDeviation()[0], 1.0e-10);
        Assert.assertEquals(MathLib.sqrt(2.0 / 3.0), u.getStandardDeviation()[1], 1.0e-10);
        Assert.assertEquals(2.0 / 3.0, u.getCovariance().getEntry(0, 0), 1.0e-10);
        Assert.assertEquals(2.0 / 3.0, u.getCovariance().getEntry(0, 1), 1.0e-10);
        Assert.assertEquals(2.0 / 3.0, u.getCovariance().getEntry(1, 0), 1.0e-10);
        Assert.assertEquals(2.0 / 3.0, u.getCovariance().getEntry(1, 1), 1.0e-10);
        u.clear();
        Assert.assertEquals(0, u.getN());
    }

    @Test
    public void testN0andN1Conditions() {
        final MultivariateSummaryStatistics u = this.createMultivariateSummaryStatistics(1, true);
        Assert.assertTrue(Double.isNaN(u.getMean()[0]));
        Assert.assertTrue(Double.isNaN(u.getStandardDeviation()[0]));

        /* n=1 */
        u.addValue(new double[] { 1 });
        Assert.assertEquals(1.0, u.getMean()[0], 1.0e-10);
        Assert.assertEquals(1.0, u.getGeometricMean()[0], 1.0e-10);
        Assert.assertEquals(0.0, u.getStandardDeviation()[0], 1.0e-10);

        /* n=2 */
        u.addValue(new double[] { 2 });
        Assert.assertTrue(u.getStandardDeviation()[0] > 0);

    }

    @Test
    public void testNaNContracts() {
        final MultivariateSummaryStatistics u = this.createMultivariateSummaryStatistics(1, true);
        Assert.assertTrue(Double.isNaN(u.getMean()[0]));
        Assert.assertTrue(Double.isNaN(u.getMin()[0]));
        Assert.assertTrue(Double.isNaN(u.getStandardDeviation()[0]));
        Assert.assertTrue(Double.isNaN(u.getGeometricMean()[0]));

        u.addValue(new double[] { 1.0 });
        Assert.assertFalse(Double.isNaN(u.getMean()[0]));
        Assert.assertFalse(Double.isNaN(u.getMin()[0]));
        Assert.assertFalse(Double.isNaN(u.getStandardDeviation()[0]));
        Assert.assertFalse(Double.isNaN(u.getGeometricMean()[0]));

    }

    @Test
    public void testSerialization() {
        final MultivariateSummaryStatistics u = this.createMultivariateSummaryStatistics(2, true);
        // Empty test
        TestUtils.checkSerializedEquality(u);
        MultivariateSummaryStatistics s = (MultivariateSummaryStatistics) TestUtils.serializeAndRecover(u);
        Assert.assertEquals(u, s);

        // Add some data
        u.addValue(new double[] { 2d, 1d });
        u.addValue(new double[] { 1d, 1d });
        u.addValue(new double[] { 3d, 1d });
        u.addValue(new double[] { 4d, 1d });
        u.addValue(new double[] { 5d, 1d });

        // Test again
        TestUtils.checkSerializedEquality(u);
        s = (MultivariateSummaryStatistics) TestUtils.serializeAndRecover(u);
        Assert.assertEquals(u, s);

    }

    @Test
    public void testEqualsAndHashCode() {
        final MultivariateSummaryStatistics u = this.createMultivariateSummaryStatistics(2, true);
        MultivariateSummaryStatistics t = null;
        final int emptyHash = u.hashCode();
        Assert.assertTrue(u.equals(u));
        Assert.assertFalse(u.equals(t));
        Assert.assertFalse(u.equals(Double.valueOf(0)));
        t = this.createMultivariateSummaryStatistics(2, true);
        Assert.assertTrue(t.equals(u));
        Assert.assertTrue(u.equals(t));
        Assert.assertEquals(emptyHash, t.hashCode());

        // Add some data to u
        u.addValue(new double[] { 2d, 1d });
        u.addValue(new double[] { 1d, 1d });
        u.addValue(new double[] { 3d, 1d });
        u.addValue(new double[] { 4d, 1d });
        u.addValue(new double[] { 5d, 1d });
        Assert.assertFalse(t.equals(u));
        Assert.assertFalse(u.equals(t));
        Assert.assertTrue(u.hashCode() != t.hashCode());

        // Add data in same order to t
        t.addValue(new double[] { 2d, 1d });
        t.addValue(new double[] { 1d, 1d });
        t.addValue(new double[] { 3d, 1d });
        t.addValue(new double[] { 4d, 1d });
        t.addValue(new double[] { 5d, 1d });
        Assert.assertTrue(t.equals(u));
        Assert.assertTrue(u.equals(t));
        Assert.assertEquals(u.hashCode(), t.hashCode());

        // Clear and make sure summaries are indistinguishable from empty summary
        u.clear();
        t.clear();
        Assert.assertTrue(t.equals(u));
        Assert.assertTrue(u.equals(t));
        Assert.assertEquals(emptyHash, t.hashCode());
        Assert.assertEquals(emptyHash, u.hashCode());
    }
}
