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

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Test cases for the {@link ListUnivariateImpl} class.
 * 
 * @version $Id: ListUnivariateImplTest.java 18108 2017-10-04 06:45:27Z bignon $
 */

public final class ListUnivariateImplTest {

    private final double one = 1;
    private final float two = 2;
    private final int three = 3;

    private final double mean = 2;
    private final double sumSq = 18;
    private final double sum = 8;
    private final double var = 0.666666666666666666667;
    private final double std = MathLib.sqrt(this.var);
    private final double n = 4;
    private final double min = 1;
    private final double max = 3;
    private final double tolerance = 10E-15;

    /** test stats */
    @Test
    public void testStats() {
        final List<Object> externalList = new ArrayList<Object>();

        final DescriptiveStatistics u = new ListUnivariateImpl(externalList);

        Assert.assertEquals("total count", 0, u.getN(), this.tolerance);
        u.addValue(this.one);
        u.addValue(this.two);
        u.addValue(this.two);
        u.addValue(this.three);
        Assert.assertEquals("N", this.n, u.getN(), this.tolerance);
        Assert.assertEquals("sum", this.sum, u.getSum(), this.tolerance);
        Assert.assertEquals("sumsq", this.sumSq, u.getSumsq(), this.tolerance);
        Assert.assertEquals("var", this.var, u.getVariance(), this.tolerance);
        Assert.assertEquals("std", this.std, u.getStandardDeviation(), this.tolerance);
        Assert.assertEquals("mean", this.mean, u.getMean(), this.tolerance);
        Assert.assertEquals("min", this.min, u.getMin(), this.tolerance);
        Assert.assertEquals("max", this.max, u.getMax(), this.tolerance);
        u.clear();
        Assert.assertEquals("total count", 0, u.getN(), this.tolerance);
    }

    @Test
    public void testN0andN1Conditions() {
        final List<Object> list = new ArrayList<Object>();

        final DescriptiveStatistics u = new ListUnivariateImpl(list);

        Assert.assertTrue("Mean of n = 0 set should be NaN", Double.isNaN(u.getMean()));
        Assert.assertTrue("Standard Deviation of n = 0 set should be NaN", Double.isNaN(u.getStandardDeviation()));
        Assert.assertTrue("Variance of n = 0 set should be NaN", Double.isNaN(u.getVariance()));

        list.add(Double.valueOf(this.one));

        Assert.assertTrue("Mean of n = 1 set should be value of single item n1", u.getMean() == this.one);
        Assert.assertTrue("StdDev of n = 1 set should be zero, instead it is: " + u.getStandardDeviation(),
            u.getStandardDeviation() == 0);
        Assert.assertTrue("Variance of n = 1 set should be zero", u.getVariance() == 0);
    }

    @Test
    public void testSkewAndKurtosis() {
        final DescriptiveStatistics u = new DescriptiveStatistics();

        final double[] testArray = { 12.5, 12, 11.8, 14.2, 14.9, 14.5, 21, 8.2, 10.3, 11.3, 14.1,
            9.9, 12.2, 12, 12.1, 11, 19.8, 11, 10, 8.8, 9, 12.3 };
        for (final double element : testArray) {
            u.addValue(element);
        }

        Assert.assertEquals("mean", 12.40455, u.getMean(), 0.0001);
        Assert.assertEquals("variance", 10.00236, u.getVariance(), 0.0001);
        Assert.assertEquals("skewness", 1.437424, u.getSkewness(), 0.0001);
        Assert.assertEquals("kurtosis", 2.37719, u.getKurtosis(), 0.0001);
    }

    @Test
    public void testProductAndGeometricMean() {
        final ListUnivariateImpl u = new ListUnivariateImpl(new ArrayList<Object>());
        u.setWindowSize(10);

        u.addValue(1.0);
        u.addValue(2.0);
        u.addValue(3.0);
        u.addValue(4.0);

        Assert.assertEquals("Geometric mean not expected", 2.213364, u.getGeometricMean(), 0.00001);

        // Now test rolling - StorelessDescriptiveStatistics should discount the contribution
        // of a discarded element
        for (int i = 0; i < 10; i++) {
            u.addValue(i + 2);
        }
        // Values should be (2,3,4,5,6,7,8,9,10,11)

        Assert.assertEquals("Geometric mean not expected", 5.755931, u.getGeometricMean(), 0.00001);

    }

    /** test stats */
    @Test
    public void testSerialization() {

        final DescriptiveStatistics u = new ListUnivariateImpl();

        Assert.assertEquals("total count", 0, u.getN(), this.tolerance);
        u.addValue(this.one);
        u.addValue(this.two);

        final DescriptiveStatistics u2 = (DescriptiveStatistics) TestUtils.serializeAndRecover(u);

        u2.addValue(this.two);
        u2.addValue(this.three);

        Assert.assertEquals("N", this.n, u2.getN(), this.tolerance);
        Assert.assertEquals("sum", this.sum, u2.getSum(), this.tolerance);
        Assert.assertEquals("sumsq", this.sumSq, u2.getSumsq(), this.tolerance);
        Assert.assertEquals("var", this.var, u2.getVariance(), this.tolerance);
        Assert.assertEquals("std", this.std, u2.getStandardDeviation(), this.tolerance);
        Assert.assertEquals("mean", this.mean, u2.getMean(), this.tolerance);
        Assert.assertEquals("min", this.min, u2.getMin(), this.tolerance);
        Assert.assertEquals("max", this.max, u2.getMax(), this.tolerance);

        u2.clear();
        Assert.assertEquals("total count", 0, u2.getN(), this.tolerance);
    }
}
