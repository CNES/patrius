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
 * VERSION:4.11:DM:DM-3197:22/05/2023:[PATRIUS] Deplacement dans PATRIUS de classes definies dans la façade ALGO DV SIRUS 
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.stat;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.exception.NoDataException;
import fr.cnes.sirius.patrius.math.stat.descriptive.DescriptiveStatistics;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;

/**
 * Test cases for the {@link StatUtils} class.
 *
 * @version $Id: StatUtilsTest.java 18108 2017-10-04 06:45:27Z bignon $
 */

public final class StatUtilsTest {

    private final double one = 1;
    private final float two = 2;
    private final int three = 3;
    private final double mean = 2;
    private final double sumSq = 18;
    private final double sum = 8;
    private final double var = 0.666666666666666666667;
    private final double min = 1;
    private final double max = 3;
    private final double tolerance = 10E-15;
    private final double nan = Double.NaN;

    /** test stats */
    @Test
    public void testStats() {
        final double[] values = new double[] { this.one, this.two, this.two, this.three };
        Assert.assertEquals("sum", this.sum, StatUtils.sum(values), this.tolerance);
        Assert.assertEquals("sumsq", this.sumSq, StatUtils.sumSq(values), this.tolerance);
        Assert.assertEquals("var", this.var, StatUtils.variance(values), this.tolerance);
        Assert.assertEquals("var with mean", this.var, StatUtils.variance(values, this.mean), this.tolerance);
        Assert.assertEquals("mean", this.mean, StatUtils.mean(values), this.tolerance);
        Assert.assertEquals("min", this.min, StatUtils.min(values), this.tolerance);
        Assert.assertEquals("max", this.max, StatUtils.max(values), this.tolerance);
    }

    @Test
    public void testN0andN1Conditions() {
        double[] values = new double[0];

        Assert.assertTrue(
            "Mean of n = 0 set should be NaN",
            Double.isNaN(StatUtils.mean(values)));
        Assert.assertTrue(
            "Variance of n = 0 set should be NaN",
            Double.isNaN(StatUtils.variance(values)));

        values = new double[] { this.one };

        Assert.assertTrue(
            "Mean of n = 1 set should be value of single item n1",
            StatUtils.mean(values) == this.one);
        Assert.assertTrue(
            "Variance of n = 1 set should be zero",
            StatUtils.variance(values) == 0);
    }

    @Test
    public void testArrayIndexConditions() {
        final double[] values = { 1.0, 2.0, 3.0, 4.0 };

        Assert.assertEquals(
            "Sum not expected",
            5.0,
            StatUtils.sum(values, 1, 2),
            Double.MIN_VALUE);
        Assert.assertEquals(
            "Sum not expected",
            3.0,
            StatUtils.sum(values, 0, 2),
            Double.MIN_VALUE);
        Assert.assertEquals(
            "Sum not expected",
            7.0,
            StatUtils.sum(values, 2, 2),
            Double.MIN_VALUE);

        try {
            StatUtils.sum(values, 2, 3);
            Assert.fail("Expected RuntimeException");
        } catch (final RuntimeException e) {
            // expected
        }

        try {
            StatUtils.sum(values, -1, 2);
            Assert.fail("Expected RuntimeException");
        } catch (final RuntimeException e) {
            // expected
        }

    }

    @Test
    public void testSumSq() {
        double[] x = null;

        // test null
        try {
            StatUtils.sumSq(x);
            Assert.fail("null is not a valid data array.");
        } catch (final MathIllegalArgumentException ex) {
            // success
        }

        try {
            StatUtils.sumSq(x, 0, 4);
            Assert.fail("null is not a valid data array.");
        } catch (final MathIllegalArgumentException ex) {
            // success
        }

        // test empty
        x = new double[] {};
        TestUtils.assertEquals(0, StatUtils.sumSq(x), this.tolerance);
        TestUtils.assertEquals(0, StatUtils.sumSq(x, 0, 0), this.tolerance);

        // test one
        x = new double[] { this.two };
        TestUtils.assertEquals(4, StatUtils.sumSq(x), this.tolerance);
        TestUtils.assertEquals(4, StatUtils.sumSq(x, 0, 1), this.tolerance);

        // test many
        x = new double[] { this.one, this.two, this.two, this.three };
        TestUtils.assertEquals(18, StatUtils.sumSq(x), this.tolerance);
        TestUtils.assertEquals(8, StatUtils.sumSq(x, 1, 2), this.tolerance);
    }

    @Test
    public void testProduct() {
        double[] x = null;

        // test null
        try {
            StatUtils.product(x);
            Assert.fail("null is not a valid data array.");
        } catch (final MathIllegalArgumentException ex) {
            // success
        }

        try {
            StatUtils.product(x, 0, 4);
            Assert.fail("null is not a valid data array.");
        } catch (final MathIllegalArgumentException ex) {
            // success
        }

        // test empty
        x = new double[] {};
        TestUtils.assertEquals(1, StatUtils.product(x), this.tolerance);
        TestUtils.assertEquals(1, StatUtils.product(x, 0, 0), this.tolerance);

        // test one
        x = new double[] { this.two };
        TestUtils.assertEquals(this.two, StatUtils.product(x), this.tolerance);
        TestUtils.assertEquals(this.two, StatUtils.product(x, 0, 1), this.tolerance);

        // test many
        x = new double[] { this.one, this.two, this.two, this.three };
        TestUtils.assertEquals(12, StatUtils.product(x), this.tolerance);
        TestUtils.assertEquals(4, StatUtils.product(x, 1, 2), this.tolerance);
    }

    @Test
    public void testSumLog() {
        double[] x = null;

        // test null
        try {
            StatUtils.sumLog(x);
            Assert.fail("null is not a valid data array.");
        } catch (final MathIllegalArgumentException ex) {
            // success
        }

        try {
            StatUtils.sumLog(x, 0, 4);
            Assert.fail("null is not a valid data array.");
        } catch (final MathIllegalArgumentException ex) {
            // success
        }

        // test empty
        x = new double[] {};
        TestUtils.assertEquals(0, StatUtils.sumLog(x), this.tolerance);
        TestUtils.assertEquals(0, StatUtils.sumLog(x, 0, 0), this.tolerance);

        // test one
        x = new double[] { this.two };
        TestUtils.assertEquals(MathLib.log(this.two), StatUtils.sumLog(x), this.tolerance);
        TestUtils.assertEquals(MathLib.log(this.two), StatUtils.sumLog(x, 0, 1), this.tolerance);

        // test many
        x = new double[] { this.one, this.two, this.two, this.three };
        TestUtils.assertEquals(MathLib.log(this.one) + 2.0 * MathLib.log(this.two) + MathLib.log(this.three),
            StatUtils.sumLog(x),
            this.tolerance);
        TestUtils.assertEquals(2.0 * MathLib.log(this.two), StatUtils.sumLog(x, 1, 2), this.tolerance);
    }

    @Test
    public void testMean() {
        double[] x = null;

        try {
            StatUtils.mean(x, 0, 4);
            Assert.fail("null is not a valid data array.");
        } catch (final MathIllegalArgumentException ex) {
            // success
        }

        // test empty
        x = new double[] {};
        TestUtils.assertEquals(Double.NaN, StatUtils.mean(x, 0, 0), this.tolerance);

        // test one
        x = new double[] { this.two };
        TestUtils.assertEquals(this.two, StatUtils.mean(x, 0, 1), this.tolerance);

        // test many
        x = new double[] { this.one, this.two, this.two, this.three };
        TestUtils.assertEquals(2.5, StatUtils.mean(x, 2, 2), this.tolerance);
    }

    @Test
    public void testVariance() {
        double[] x = null;

        try {
            StatUtils.variance(x, 0, 4);
            Assert.fail("null is not a valid data array.");
        } catch (final MathIllegalArgumentException ex) {
            // success
        }

        // test empty
        x = new double[] {};
        TestUtils.assertEquals(Double.NaN, StatUtils.variance(x, 0, 0), this.tolerance);

        // test one
        x = new double[] { this.two };
        TestUtils.assertEquals(0.0, StatUtils.variance(x, 0, 1), this.tolerance);

        // test many
        x = new double[] { this.one, this.two, this.two, this.three };
        TestUtils.assertEquals(0.5, StatUtils.variance(x, 2, 2), this.tolerance);

        // test precomputed mean
        x = new double[] { this.one, this.two, this.two, this.three };
        TestUtils.assertEquals(0.5, StatUtils.variance(x, 2.5, 2, 2), this.tolerance);
    }

    @Test
    public void testPopulationVariance() {
        double[] x = null;

        try {
            StatUtils.variance(x, 0, 4);
            Assert.fail("null is not a valid data array.");
        } catch (final MathIllegalArgumentException ex) {
            // success
        }

        // test empty
        x = new double[] {};
        TestUtils.assertEquals(Double.NaN, StatUtils.populationVariance(x, 0, 0), this.tolerance);

        // test one
        x = new double[] { this.two };
        TestUtils.assertEquals(0.0, StatUtils.populationVariance(x, 0, 1), this.tolerance);

        // test many
        x = new double[] { this.one, this.two, this.two, this.three };
        TestUtils.assertEquals(0.25, StatUtils.populationVariance(x, 0, 2), this.tolerance);

        // test precomputed mean
        x = new double[] { this.one, this.two, this.two, this.three };
        TestUtils.assertEquals(0.25, StatUtils.populationVariance(x, 2.5, 2, 2), this.tolerance);
        TestUtils.assertEquals(0.5, StatUtils.populationVariance(x), this.tolerance);
        TestUtils.assertEquals(0.5, StatUtils.populationVariance(x, 2), this.tolerance);
    }

    @Test
    public void testMax() {
        double[] x = null;

        try {
            StatUtils.max(x, 0, 4);
            Assert.fail("null is not a valid data array.");
        } catch (final MathIllegalArgumentException ex) {
            // success
        }

        // test empty
        x = new double[] {};
        TestUtils.assertEquals(Double.NaN, StatUtils.max(x, 0, 0), this.tolerance);

        // test one
        x = new double[] { this.two };
        TestUtils.assertEquals(this.two, StatUtils.max(x, 0, 1), this.tolerance);

        // test many
        x = new double[] { this.one, this.two, this.two, this.three };
        TestUtils.assertEquals(this.three, StatUtils.max(x, 1, 3), this.tolerance);

        // test first nan is ignored
        x = new double[] { this.nan, this.two, this.three };
        TestUtils.assertEquals(this.three, StatUtils.max(x), this.tolerance);

        // test middle nan is ignored
        x = new double[] { this.one, this.nan, this.three };
        TestUtils.assertEquals(this.three, StatUtils.max(x), this.tolerance);

        // test last nan is ignored
        x = new double[] { this.one, this.two, this.nan };
        TestUtils.assertEquals(this.two, StatUtils.max(x), this.tolerance);

        // test all nan returns nan
        x = new double[] { this.nan, this.nan, this.nan };
        TestUtils.assertEquals(this.nan, StatUtils.max(x), this.tolerance);
    }

    @Test
    public void testMin() {
        double[] x = null;

        try {
            StatUtils.min(x, 0, 4);
            Assert.fail("null is not a valid data array.");
        } catch (final MathIllegalArgumentException ex) {
            // success
        }

        // test empty
        x = new double[] {};
        TestUtils.assertEquals(Double.NaN, StatUtils.min(x, 0, 0), this.tolerance);

        // test one
        x = new double[] { this.two };
        TestUtils.assertEquals(this.two, StatUtils.min(x, 0, 1), this.tolerance);

        // test many
        x = new double[] { this.one, this.two, this.two, this.three };
        TestUtils.assertEquals(this.two, StatUtils.min(x, 1, 3), this.tolerance);

        // test first nan is ignored
        x = new double[] { this.nan, this.two, this.three };
        TestUtils.assertEquals(this.two, StatUtils.min(x), this.tolerance);

        // test middle nan is ignored
        x = new double[] { this.one, this.nan, this.three };
        TestUtils.assertEquals(this.one, StatUtils.min(x), this.tolerance);

        // test last nan is ignored
        x = new double[] { this.one, this.two, this.nan };
        TestUtils.assertEquals(this.one, StatUtils.min(x), this.tolerance);

        // test all nan returns nan
        x = new double[] { this.nan, this.nan, this.nan };
        TestUtils.assertEquals(this.nan, StatUtils.min(x), this.tolerance);
    }

    @Test
    public void testMaxAbs() {
        final double[] data = { -1., -0., -1.896, -452., 45., 67. };

        final double outcome = StatUtils.maxAbs(data);
        final double expectedOutcome = +452.;

        Assert.assertEquals(outcome, expectedOutcome, Precision.DOUBLE_COMPARISON_EPSILON);
    }

    @Test
    public void testPercentile() {
        double[] x = null;

        // test null
        try {
            StatUtils.percentile(x, .25);
            Assert.fail("null is not a valid data array.");
        } catch (final MathIllegalArgumentException ex) {
            // success
        }

        try {
            StatUtils.percentile(x, 0, 4, 0.25);
            Assert.fail("null is not a valid data array.");
        } catch (final MathIllegalArgumentException ex) {
            // success
        }

        // test empty
        x = new double[] {};
        TestUtils.assertEquals(Double.NaN, StatUtils.percentile(x, 25), this.tolerance);
        TestUtils.assertEquals(Double.NaN, StatUtils.percentile(x, 0, 0, 25), this.tolerance);

        // test one
        x = new double[] { this.two };
        TestUtils.assertEquals(this.two, StatUtils.percentile(x, 25), this.tolerance);
        TestUtils.assertEquals(this.two, StatUtils.percentile(x, 0, 1, 25), this.tolerance);

        // test many
        x = new double[] { this.one, this.two, this.two, this.three };
        TestUtils.assertEquals(2.5, StatUtils.percentile(x, 70), this.tolerance);
        TestUtils.assertEquals(2.5, StatUtils.percentile(x, 1, 3, 62.5), this.tolerance);
    }

    @Test
    public void testDifferenceStats() {
        final double sample1[] = { 1d, 2d, 3d, 4d };
        final double sample2[] = { 1d, 3d, 4d, 2d };
        final double diff[] = { 0d, -1d, -1d, 2d };
        final double small[] = { 1d, 4d };
        final double empty[] = {};
        final double meanDifference = StatUtils.meanDifference(sample1, sample2);
        Assert.assertEquals(StatUtils.sumDifference(sample1, sample2), StatUtils.sum(diff), this.tolerance);
        Assert.assertEquals(meanDifference, StatUtils.mean(diff), this.tolerance);
        Assert.assertEquals(StatUtils.varianceDifference(sample1, sample2, meanDifference),
            StatUtils.variance(diff), this.tolerance);
        try {
            StatUtils.meanDifference(sample1, small);
            Assert.fail("Expecting MathIllegalArgumentException");
        } catch (final MathIllegalArgumentException ex) {
            // expected
        }
        try {
            StatUtils.varianceDifference(sample1, small, meanDifference);
            Assert.fail("Expecting MathIllegalArgumentException");
        } catch (final MathIllegalArgumentException ex) {
            // expected
        }
        try {
            final double[] single = { 1.0 };
            StatUtils.varianceDifference(single, single, meanDifference);
            Assert.fail("Expecting MathIllegalArgumentException");
        } catch (final MathIllegalArgumentException ex) {
            // expected
        }
        try {
            StatUtils.sumDifference(empty, empty);
            Assert.fail("Expecting NoDataException");
        } catch (final NoDataException ex) {
            // expected
        }
    }

    @Test
    public void testGeometricMean() {
        double[] test = null;
        try {
            StatUtils.geometricMean(test);
            Assert.fail("Expecting MathIllegalArgumentException");
        } catch (final MathIllegalArgumentException ex) {
            // expected
        }
        test = new double[] { 2, 4, 6, 8 };
        Assert.assertEquals(MathLib.exp(0.25d * StatUtils.sumLog(test)),
            StatUtils.geometricMean(test), Double.MIN_VALUE);
        Assert.assertEquals(MathLib.exp(0.5 * StatUtils.sumLog(test, 0, 2)),
            StatUtils.geometricMean(test, 0, 2), Double.MIN_VALUE);
    }

    /**
     * Run the test with the values 50 and 100 and assume standardized values
     */

    @Test
    public void testNormalize1() {
        final double sample[] = { 50, 100 };
        final double expectedSample[] = { -25 / Math.sqrt(1250), 25 / Math.sqrt(1250) };
        final double[] out = StatUtils.normalize(sample);
        for (int i = 0; i < out.length; i++) {
            Assert.assertTrue(Precision.equals(out[i], expectedSample[i], 1));
        }

    }

    /**
     * Run with 77 random values, assuming that the outcome has a mean of 0 and a standard deviation of 1 with a
     * precision of 1E-10.
     */

    @Test
    public void testNormalize2() {
        // create an sample with 77 values
        final int length = 77;
        final double sample[] = new double[length];
        for (int i = 0; i < length; i++) {
            sample[i] = Math.random();
        }
        // normalize this sample
        final double standardizedSample[] = StatUtils.normalize(sample);

        final DescriptiveStatistics stats = new DescriptiveStatistics();
        // Add the data from the array
        for (int i = 0; i < length; i++) {
            stats.addValue(standardizedSample[i]);
        }
        // the calculations do have a limited precision
        final double distance = 1E-10;
        // check the mean an standard deviation
        Assert.assertEquals(0.0, stats.getMean(), distance);
        Assert.assertEquals(1.0, stats.getStandardDeviation(), distance);

    }

}
