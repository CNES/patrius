/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
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
 *
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.stat.descriptive;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.stat.descriptive.moment.GeometricMean;
import fr.cnes.sirius.patrius.math.stat.descriptive.moment.Mean;
import fr.cnes.sirius.patrius.math.stat.descriptive.moment.Variance;
import fr.cnes.sirius.patrius.math.stat.descriptive.summary.Sum;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Test cases for the {@link SummaryStatistics} class.
 * 
 * @version $Id: SummaryStatisticsTest.java 18108 2017-10-04 06:45:27Z bignon $
 */

public class SummaryStatisticsTest {

    private final double one = 1;
    private final float twoF = 2;
    private final long twoL = 2;
    private final int three = 3;
    private final double mean = 2;
    private final double sumSq = 18;
    private final double sum = 8;
    private final double var = 0.666666666666666666667;
    private final double popVar = 0.5;
    private final double std = MathLib.sqrt(this.var);
    private final double n = 4;
    private final double min = 1;
    private final double max = 3;
    private final double tolerance = 10E-15;

    protected SummaryStatistics createSummaryStatistics() {
        return new SummaryStatistics();
    }

    /** test stats */
    @Test
    public void testStats() {
        final SummaryStatistics u = this.createSummaryStatistics();
        Assert.assertEquals("total count", 0, u.getN(), this.tolerance);
        u.addValue(this.one);
        u.addValue(this.twoF);
        u.addValue(this.twoL);
        u.addValue(this.three);
        Assert.assertEquals("N", this.n, u.getN(), this.tolerance);
        Assert.assertEquals("sum", this.sum, u.getSum(), this.tolerance);
        Assert.assertEquals("sumsq", this.sumSq, u.getSumsq(), this.tolerance);
        Assert.assertEquals("var", this.var, u.getVariance(), this.tolerance);
        Assert.assertEquals("population var", this.popVar, u.getPopulationVariance(), this.tolerance);
        Assert.assertEquals("std", this.std, u.getStandardDeviation(), this.tolerance);
        Assert.assertEquals("mean", this.mean, u.getMean(), this.tolerance);
        Assert.assertEquals("min", this.min, u.getMin(), this.tolerance);
        Assert.assertEquals("max", this.max, u.getMax(), this.tolerance);
        u.clear();
        Assert.assertEquals("total count", 0, u.getN(), this.tolerance);
    }

    @Test
    public void testN0andN1Conditions() {
        final SummaryStatistics u = this.createSummaryStatistics();
        Assert.assertTrue("Mean of n = 0 set should be NaN",
            Double.isNaN(u.getMean()));
        Assert.assertTrue("Standard Deviation of n = 0 set should be NaN",
            Double.isNaN(u.getStandardDeviation()));
        Assert.assertTrue("Variance of n = 0 set should be NaN",
            Double.isNaN(u.getVariance()));

        /* n=1 */
        u.addValue(this.one);
        Assert.assertTrue("mean should be one (n = 1)",
            u.getMean() == this.one);
        Assert.assertTrue("geometric should be one (n = 1) instead it is " + u.getGeometricMean(),
            u.getGeometricMean() == this.one);
        Assert.assertTrue("Std should be zero (n = 1)",
            u.getStandardDeviation() == 0.0);
        Assert.assertTrue("variance should be zero (n = 1)",
            u.getVariance() == 0.0);

        /* n=2 */
        u.addValue(this.twoF);
        Assert.assertTrue("Std should not be zero (n = 2)",
            u.getStandardDeviation() != 0.0);
        Assert.assertTrue("variance should not be zero (n = 2)",
            u.getVariance() != 0.0);

    }

    @Test
    public void testProductAndGeometricMean() {
        final SummaryStatistics u = this.createSummaryStatistics();
        u.addValue(1.0);
        u.addValue(2.0);
        u.addValue(3.0);
        u.addValue(4.0);

        Assert.assertEquals("Geometric mean not expected", 2.213364,
            u.getGeometricMean(), 0.00001);
    }

    @Test
    public void testNaNContracts() {
        final SummaryStatistics u = this.createSummaryStatistics();
        Assert.assertTrue("mean not NaN", Double.isNaN(u.getMean()));
        Assert.assertTrue("min not NaN", Double.isNaN(u.getMin()));
        Assert.assertTrue("std dev not NaN", Double.isNaN(u.getStandardDeviation()));
        Assert.assertTrue("var not NaN", Double.isNaN(u.getVariance()));
        Assert.assertTrue("geom mean not NaN", Double.isNaN(u.getGeometricMean()));

        u.addValue(1.0);

        Assert.assertEquals("mean not expected", 1.0,
            u.getMean(), Double.MIN_VALUE);
        Assert.assertEquals("variance not expected", 0.0,
            u.getVariance(), Double.MIN_VALUE);
        Assert.assertEquals("geometric mean not expected", 1.0,
            u.getGeometricMean(), Double.MIN_VALUE);

        u.addValue(-1.0);

        Assert.assertTrue("geom mean not NaN", Double.isNaN(u.getGeometricMean()));

        u.addValue(0.0);

        Assert.assertTrue("geom mean not NaN", Double.isNaN(u.getGeometricMean()));

        // FiXME: test all other NaN contract specs
    }

    @Test
    public void testGetSummary() {
        final SummaryStatistics u = this.createSummaryStatistics();
        StatisticalSummary summary = u.getSummary();
        this.verifySummary(u, summary);
        u.addValue(1d);
        summary = u.getSummary();
        this.verifySummary(u, summary);
        u.addValue(2d);
        summary = u.getSummary();
        this.verifySummary(u, summary);
        u.addValue(2d);
        summary = u.getSummary();
        this.verifySummary(u, summary);
    }

    @Test
    public void testSerialization() {
        final SummaryStatistics u = this.createSummaryStatistics();
        // Empty test
        TestUtils.checkSerializedEquality(u);
        SummaryStatistics s = (SummaryStatistics) TestUtils.serializeAndRecover(u);
        StatisticalSummary summary = s.getSummary();
        this.verifySummary(u, summary);

        // Add some data
        u.addValue(2d);
        u.addValue(1d);
        u.addValue(3d);
        u.addValue(4d);
        u.addValue(5d);

        // Test again
        TestUtils.checkSerializedEquality(u);
        s = (SummaryStatistics) TestUtils.serializeAndRecover(u);
        summary = s.getSummary();
        this.verifySummary(u, summary);

    }

    @Test
    public void testEqualsAndHashCode() {
        final SummaryStatistics u = this.createSummaryStatistics();
        SummaryStatistics t = null;
        final int emptyHash = u.hashCode();
        Assert.assertTrue("reflexive", u.equals(u));
        Assert.assertFalse("non-null compared to null", u.equals(t));
        Assert.assertFalse("wrong type", u.equals(Double.valueOf(0)));
        t = this.createSummaryStatistics();
        Assert.assertTrue("empty instances should be equal", t.equals(u));
        Assert.assertTrue("empty instances should be equal", u.equals(t));
        Assert.assertEquals("empty hash code", emptyHash, t.hashCode());

        // Add some data to u
        u.addValue(2d);
        u.addValue(1d);
        u.addValue(3d);
        u.addValue(4d);
        Assert.assertFalse("different n's should make instances not equal", t.equals(u));
        Assert.assertFalse("different n's should make instances not equal", u.equals(t));
        Assert.assertTrue("different n's should make hashcodes different",
            u.hashCode() != t.hashCode());

        // Add data in same order to t
        t.addValue(2d);
        t.addValue(1d);
        t.addValue(3d);
        t.addValue(4d);
        Assert.assertTrue("summaries based on same data should be equal", t.equals(u));
        Assert.assertTrue("summaries based on same data should be equal", u.equals(t));
        Assert.assertEquals("summaries based on same data should have same hashcodes",
            u.hashCode(), t.hashCode());

        // Clear and make sure summaries are indistinguishable from empty summary
        u.clear();
        t.clear();
        Assert.assertTrue("empty instances should be equal", t.equals(u));
        Assert.assertTrue("empty instances should be equal", u.equals(t));
        Assert.assertEquals("empty hash code", emptyHash, t.hashCode());
        Assert.assertEquals("empty hash code", emptyHash, u.hashCode());
    }

    @Test
    public void testCopy() {
        final SummaryStatistics u = this.createSummaryStatistics();
        u.addValue(2d);
        u.addValue(1d);
        u.addValue(3d);
        u.addValue(4d);
        final SummaryStatistics v = new SummaryStatistics(u);
        Assert.assertEquals(u, v);
        Assert.assertEquals(v, u);

        // Make sure both behave the same with additional values added
        u.addValue(7d);
        u.addValue(9d);
        u.addValue(11d);
        u.addValue(23d);
        v.addValue(7d);
        v.addValue(9d);
        v.addValue(11d);
        v.addValue(23d);
        Assert.assertEquals(u, v);
        Assert.assertEquals(v, u);

        // Check implementation pointers are preserved
        u.clear();
        u.setSumImpl(new Sum());
        SummaryStatistics.copy(u, v);
        Assert.assertEquals(u.getSumImpl(), v.getSumImpl());

    }

    private void verifySummary(final SummaryStatistics u, final StatisticalSummary s) {
        Assert.assertEquals("N", s.getN(), u.getN());
        TestUtils.assertEquals("sum", s.getSum(), u.getSum(), this.tolerance);
        TestUtils.assertEquals("var", s.getVariance(), u.getVariance(), this.tolerance);
        TestUtils.assertEquals("std", s.getStandardDeviation(), u.getStandardDeviation(), this.tolerance);
        TestUtils.assertEquals("mean", s.getMean(), u.getMean(), this.tolerance);
        TestUtils.assertEquals("min", s.getMin(), u.getMin(), this.tolerance);
        TestUtils.assertEquals("max", s.getMax(), u.getMax(), this.tolerance);
    }

    @Test
    public void testSetterInjection() {
        final SummaryStatistics u = this.createSummaryStatistics();
        u.setMeanImpl(new Sum());
        u.setSumLogImpl(new Sum());
        u.addValue(1);
        u.addValue(3);
        Assert.assertEquals(4, u.getMean(), 1E-14);
        Assert.assertEquals(4, u.getSumOfLogs(), 1E-14);
        Assert.assertEquals(MathLib.exp(2), u.getGeometricMean(), 1E-14);
        u.clear();
        u.addValue(1);
        u.addValue(2);
        Assert.assertEquals(3, u.getMean(), 1E-14);
        u.clear();
        u.setMeanImpl(new Mean()); // OK after clear
    }

    @Test
    public void testSetterIllegalState() {
        final SummaryStatistics u = this.createSummaryStatistics();
        u.addValue(1);
        u.addValue(3);
        try {
            u.setMeanImpl(new Sum());
            Assert.fail("Expecting IllegalStateException");
        } catch (final IllegalStateException ex) {
            // expected
        }
    }

    /**
     * JIRA: MATH-691
     */
    @Test
    public void testOverrideVarianceWithMathClass() {
        final double[] scores = { 1, 2, 3, 4 };
        final SummaryStatistics stats = new SummaryStatistics();
        stats.setVarianceImpl(new Variance(false)); // use "population variance"
        for (final double i : scores) {
            stats.addValue(i);
        }
        Assert.assertEquals((new Variance(false)).evaluate(scores), stats.getVariance(), 0);
    }

    @Test
    public void testOverrideMeanWithMathClass() {
        final double[] scores = { 1, 2, 3, 4 };
        final SummaryStatistics stats = new SummaryStatistics();
        stats.setMeanImpl(new Mean());
        for (final double i : scores) {
            stats.addValue(i);
        }
        Assert.assertEquals((new Mean()).evaluate(scores), stats.getMean(), 0);
    }

    @Test
    public void testOverrideGeoMeanWithMathClass() {
        final double[] scores = { 1, 2, 3, 4 };
        final SummaryStatistics stats = new SummaryStatistics();
        stats.setGeoMeanImpl(new GeometricMean());
        for (final double i : scores) {
            stats.addValue(i);
        }
        Assert.assertEquals((new GeometricMean()).evaluate(scores), stats.getGeometricMean(), 0);
    }
}
