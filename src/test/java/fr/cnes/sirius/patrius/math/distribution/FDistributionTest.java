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
package fr.cnes.sirius.patrius.math.distribution;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;

/**
 * Test cases for FDistribution.
 * Extends ContinuousDistributionAbstractTest. See class javadoc for
 * ContinuousDistributionAbstractTest for details.
 * 
 * @version $Id: FDistributionTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class FDistributionTest extends RealDistributionAbstractTest {

    // -------------- Implementations for abstract methods -----------------------

    /** Creates the default continuous distribution instance to use in tests. */
    @Override
    public FDistribution makeDistribution() {
        return new FDistribution(5.0, 6.0);
    }

    /** Creates the default cumulative probability distribution test input values */
    @Override
    public double[] makeCumulativeTestPoints() {
        // quantiles computed using R version 2.9.2
        return new double[] { 0.0346808448626, 0.0937009113303, 0.143313661184, 0.202008445998, 0.293728320107,
            20.8026639595, 8.74589525602, 5.98756512605, 4.38737418741, 3.10751166664 };
    }

    /** Creates the default cumulative probability density test expected values */
    @Override
    public double[] makeCumulativeTestValues() {
        return new double[] { 0.001, 0.01, 0.025, 0.05, 0.1, 0.999, 0.990, 0.975, 0.950, 0.900 };
    }

    /** Creates the default probability density test expected values */
    @Override
    public double[] makeDensityTestValues() {
        return new double[] { 0.0689156576706, 0.236735653193, 0.364074131941, 0.481570789649, 0.595880479994,
            0.000133443915657, 0.00286681303403, 0.00969192007502, 0.0242883861471, 0.0605491314658 };
    }

    // --------------------- Override tolerance --------------
    @Override
    public void setUp() {
        super.setUp();
        this.setTolerance(1e-9);
    }

    // ---------------------------- Additional test cases -------------------------

    @Test
    public void testCumulativeProbabilityExtremes() {
        this.setCumulativeTestPoints(new double[] { -2, 0 });
        this.setCumulativeTestValues(new double[] { 0, 0 });
        this.verifyCumulativeProbabilities();
    }

    @Test
    public void testInverseCumulativeProbabilityExtremes() {
        this.setInverseCumulativeTestPoints(new double[] { 0, 1 });
        this.setInverseCumulativeTestValues(new double[] { 0, Double.POSITIVE_INFINITY });
        this.verifyInverseCumulativeProbabilities();
    }

    @Test
    public void testDfAccessors() {
        final FDistribution dist = (FDistribution) this.getDistribution();
        Assert.assertEquals(5d, dist.getNumeratorDegreesOfFreedom(), Double.MIN_VALUE);
        Assert.assertEquals(6d, dist.getDenominatorDegreesOfFreedom(), Double.MIN_VALUE);
    }

    @Test
    public void testPreconditions() {
        try {
            new FDistribution(0, 1);
            Assert.fail("Expecting NotStrictlyPositiveException for df = 0");
        } catch (final NotStrictlyPositiveException ex) {
            // Expected.
        }
        try {
            new FDistribution(1, 0);
            Assert.fail("Expecting NotStrictlyPositiveException for df = 0");
        } catch (final NotStrictlyPositiveException ex) {
            // Expected.
        }
    }

    @Test
    public void testLargeDegreesOfFreedom() {
        final FDistribution fd = new FDistribution(100000, 100000);
        final double p = fd.cumulativeProbability(.999);
        final double x = fd.inverseCumulativeProbability(p);
        Assert.assertEquals(.999, x, 1.0e-5);
    }

    @Test
    public void testSmallDegreesOfFreedom() {
        FDistribution fd = new FDistribution(1, 1);
        double p = fd.cumulativeProbability(0.975);
        double x = fd.inverseCumulativeProbability(p);
        Assert.assertEquals(0.975, x, 1.0e-5);

        fd = new FDistribution(1, 2);
        p = fd.cumulativeProbability(0.975);
        x = fd.inverseCumulativeProbability(p);
        Assert.assertEquals(0.975, x, 1.0e-5);
    }

    @Test
    public void testMoments() {
        final double tol = 1e-9;
        FDistribution dist;

        dist = new FDistribution(1, 2);
        Assert.assertTrue(Double.isNaN(dist.getNumericalMean()));
        Assert.assertTrue(Double.isNaN(dist.getNumericalVariance()));

        dist = new FDistribution(1, 3);
        Assert.assertEquals(dist.getNumericalMean(), 3d / (3d - 2d), tol);
        Assert.assertTrue(Double.isNaN(dist.getNumericalVariance()));

        dist = new FDistribution(1, 5);
        Assert.assertEquals(dist.getNumericalMean(), 5d / (5d - 2d), tol);
        Assert.assertEquals(dist.getNumericalVariance(), (2d * 5d * 5d * 4d) / 9d, tol);
    }

    @Test
    public void testMath785() {
        // this test was failing due to inaccurate results from ContinuedFraction.

        try {
            final double prob = 0.01;
            final FDistribution f = new FDistribution(200000, 200000);
            final double result = f.inverseCumulativeProbability(prob);
            Assert.assertTrue(result < 1.0);
        } catch (final Exception e) {
            Assert.fail("Failing to calculate inverse cumulative probability");
        }
    }
}
