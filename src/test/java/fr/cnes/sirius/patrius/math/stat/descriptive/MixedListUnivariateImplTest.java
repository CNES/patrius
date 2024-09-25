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
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.stat.descriptive;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.NumberTransformer;
import fr.cnes.sirius.patrius.math.util.TransformerMap;

/**
 * Test cases for the {@link ListUnivariateImpl} class.
 * 
 * @version $Id: MixedListUnivariateImplTest.java 18108 2017-10-04 06:45:27Z bignon $
 */

public final class MixedListUnivariateImplTest {
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

    private TransformerMap transformers = new TransformerMap();

    public MixedListUnivariateImplTest() {
        this.transformers = new TransformerMap();

        this.transformers.putTransformer(Foo.class, new FooTransformer());

        this.transformers.putTransformer(Bar.class, new BarTransformer());

    }

    /** test stats */
    @Test
    public void testStats() {
        final List<Object> externalList = new ArrayList<>();

        final DescriptiveStatistics u = new ListUnivariateImpl(externalList, this.transformers);

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
        final DescriptiveStatistics u = new ListUnivariateImpl(new ArrayList<>(), this.transformers);

        Assert.assertTrue(
            "Mean of n = 0 set should be NaN",
            Double.isNaN(u.getMean()));
        Assert.assertTrue(
            "Standard Deviation of n = 0 set should be NaN",
            Double.isNaN(u.getStandardDeviation()));
        Assert.assertTrue(
            "Variance of n = 0 set should be NaN",
            Double.isNaN(u.getVariance()));

        u.addValue(this.one);

        Assert.assertTrue(
            "Mean of n = 1 set should be value of single item n1, instead it is " + u.getMean(),
            u.getMean() == this.one);

        Assert.assertTrue(
            "StdDev of n = 1 set should be zero, instead it is: "
                + u.getStandardDeviation(),
            u.getStandardDeviation() == 0);
        Assert.assertTrue(
            "Variance of n = 1 set should be zero",
            u.getVariance() == 0);
    }

    @Test
    public void testSkewAndKurtosis() {
        final ListUnivariateImpl u =
            new ListUnivariateImpl(new ArrayList<>(), this.transformers);

        u.addObject("12.5");
        u.addObject(Integer.valueOf(12));
        u.addObject("11.8");
        u.addObject("14.2");
        u.addObject(new Foo());
        u.addObject("14.5");
        u.addObject(Long.valueOf(21));
        u.addObject("8.2");
        u.addObject("10.3");
        u.addObject("11.3");
        u.addObject(Float.valueOf(14.1f));
        u.addObject("9.9");
        u.addObject("12.2");
        u.addObject(new Bar());
        u.addObject("12.1");
        u.addObject("11");
        u.addObject(Double.valueOf(19.8));
        u.addObject("11");
        u.addObject("10");
        u.addObject("8.8");
        u.addObject("9");
        u.addObject("12.3");

        Assert.assertEquals("mean", 12.40455, u.getMean(), 0.0001);
        Assert.assertEquals("variance", 10.00236, u.getVariance(), 0.0001);
        Assert.assertEquals("skewness", 1.437424, u.getSkewness(), 0.0001);
        Assert.assertEquals("kurtosis", 2.37719, u.getKurtosis(), 0.0001);
    }

    @Test
    public void testProductAndGeometricMean() {
        final ListUnivariateImpl u = new ListUnivariateImpl(new ArrayList<>(), this.transformers);
        u.setWindowSize(10);

        u.addValue(1.0);
        u.addValue(2.0);
        u.addValue(3.0);
        u.addValue(4.0);

        Assert.assertEquals(
            "Geometric mean not expected",
            2.213364,
            u.getGeometricMean(),
            0.00001);

        // Now test rolling - StorelessDescriptiveStatistics should discount the contribution
        // of a discarded element
        for (int i = 0; i < 10; i++) {
            u.addValue(i + 2);
        }
        // Values should be (2,3,4,5,6,7,8,9,10,11)
        Assert.assertEquals(
            "Geometric mean not expected",
            5.755931,
            u.getGeometricMean(),
            0.00001);

    }

    public static final class Foo {
        public String heresFoo() {
            return "14.9";
        }
    }

    public static final class FooTransformer implements NumberTransformer, Serializable {
        private static final long serialVersionUID = -4252248129291326127L;

        @Override
        public double transform(final Object o) {
            return Double.parseDouble(((Foo) o).heresFoo());
        }
    }

    public static final class Bar {
        public String heresBar() {
            return "12.0";
        }
    }

    public static final class BarTransformer implements NumberTransformer, Serializable {
        private static final long serialVersionUID = -1768345377764262043L;

        @Override
        public double transform(final Object o) {
            return Double.parseDouble(((Bar) o).heresBar());
        }
    }
}
