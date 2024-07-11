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
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.stat.descriptive.rank;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.stat.descriptive.UnivariateStatistic;
import fr.cnes.sirius.patrius.math.stat.descriptive.UnivariateStatisticAbstractTest;

/**
 * Test cases for the {@link UnivariateStatistic} class.
 * 
 * @version $Id: PercentileTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class PercentileTest extends UnivariateStatisticAbstractTest {

    protected Percentile stat;

    /**
     * {@inheritDoc}
     */
    @Override
    public UnivariateStatistic getUnivariateStatistic() {
        return new Percentile(95.0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double expectedValue() {
        return this.percentile95;
    }

    @Test
    public void testHighPercentile() {
        final double[] d = new double[] { 1, 2, 3 };
        final Percentile p = new Percentile(75);
        Assert.assertEquals(3.0, p.evaluate(d), 1.0e-5);
    }

    @Test
    public void testLowPercentile() {
        final double[] d = new double[] { 0, 1 };
        final Percentile p = new Percentile(25);
        Assert.assertEquals(0d, p.evaluate(d), Double.MIN_VALUE);
    }

    @Test
    public void testPercentile() {
        final double[] d = new double[] { 1, 3, 2, 4 };
        final Percentile p = new Percentile(30);
        Assert.assertEquals(1.5, p.evaluate(d), 1.0e-5);
        p.setQuantile(25);
        Assert.assertEquals(1.25, p.evaluate(d), 1.0e-5);
        p.setQuantile(75);
        Assert.assertEquals(3.75, p.evaluate(d), 1.0e-5);
        p.setQuantile(50);
        Assert.assertEquals(2.5, p.evaluate(d), 1.0e-5);

        // invalid percentiles
        try {
            p.evaluate(d, 0, d.length, -1.0);
            Assert.fail();
        } catch (final MathIllegalArgumentException ex) {
            // success
        }
        try {
            p.evaluate(d, 0, d.length, 101.0);
            Assert.fail();
        } catch (final MathIllegalArgumentException ex) {
            // success
        }
    }

    @Test
    public void testNISTExample() {
        final double[] d = new double[] { 95.1772, 95.1567, 95.1937, 95.1959,
            95.1442, 95.0610, 95.1591, 95.1195, 95.1772, 95.0925, 95.1990, 95.1682
        };
        final Percentile p = new Percentile(90);
        Assert.assertEquals(95.1981, p.evaluate(d), 1.0e-4);
        Assert.assertEquals(95.1990, p.evaluate(d, 0, d.length, 100d), 0);
    }

    @Test
    public void test5() {
        final Percentile percentile = new Percentile(5);
        Assert.assertEquals(this.percentile5, percentile.evaluate(this.testArray), this.getTolerance());
    }

    @Test
    public void testNullEmpty() {
        final Percentile percentile = new Percentile(50);
        final double[] nullArray = null;
        final double[] emptyArray = new double[] {};
        try {
            percentile.evaluate(nullArray);
            Assert.fail("Expecting MathIllegalArgumentException for null array");
        } catch (final MathIllegalArgumentException ex) {
            // expected
        }
        Assert.assertTrue(Double.isNaN(percentile.evaluate(emptyArray)));
    }

    @Test
    public void testSingleton() {
        final Percentile percentile = new Percentile(50);
        final double[] singletonArray = new double[] { 1d };
        Assert.assertEquals(1d, percentile.evaluate(singletonArray), 0);
        Assert.assertEquals(1d, percentile.evaluate(singletonArray, 0, 1), 0);
        Assert.assertEquals(1d, percentile.evaluate(singletonArray, 0, 1, 5), 0);
        Assert.assertEquals(1d, percentile.evaluate(singletonArray, 0, 1, 100), 0);
        Assert.assertTrue(Double.isNaN(percentile.evaluate(singletonArray, 0, 0)));
    }

    @Test
    public void testSpecialValues() {
        final Percentile percentile = new Percentile(50);
        double[] specialValues = new double[] { 0d, 1d, 2d, 3d, 4d, Double.NaN };
        Assert.assertEquals(2.5d, percentile.evaluate(specialValues), 0);
        specialValues = new double[] { Double.NEGATIVE_INFINITY, 1d, 2d, 3d,
            Double.NaN, Double.POSITIVE_INFINITY };
        Assert.assertEquals(2.5d, percentile.evaluate(specialValues), 0);
        specialValues = new double[] { 1d, 1d, Double.POSITIVE_INFINITY,
            Double.POSITIVE_INFINITY };
        Assert.assertTrue(Double.isInfinite(percentile.evaluate(specialValues)));
        specialValues = new double[] { 1d, 1d, Double.NaN,
            Double.NaN };
        Assert.assertTrue(Double.isNaN(percentile.evaluate(specialValues)));
        specialValues = new double[] { 1d, 1d, Double.NEGATIVE_INFINITY,
            Double.NEGATIVE_INFINITY };
        // Interpolation results in NEGATIVE_INFINITY + POSITIVE_INFINITY
        Assert.assertTrue(Double.isNaN(percentile.evaluate(specialValues)));
    }

    @Test
    public void testSetQuantile() {
        final Percentile percentile = new Percentile(10);
        percentile.setQuantile(100); // OK
        Assert.assertEquals(100, percentile.getQuantile(), 0);
        try {
            percentile.setQuantile(0);
            Assert.fail("Expecting MathIllegalArgumentException");
        } catch (final MathIllegalArgumentException ex) {
            // expected
        }
        try {
            new Percentile(0);
            Assert.fail("Expecting MathIllegalArgumentException");
        } catch (final MathIllegalArgumentException ex) {
            // expected
        }
    }

}
