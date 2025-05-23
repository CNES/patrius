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
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;

/**
 * Test cases for ExponentialDistribution.
 * Extends ContinuousDistributionAbstractTest. See class javadoc for
 * ContinuousDistributionAbstractTest for details.
 * 
 * @version $Id: ExponentialDistributionTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class ExponentialDistributionTest extends RealDistributionAbstractTest {

    // --------------------- Override tolerance --------------
    @Override
    public void setUp() {
        super.setUp();
        this.setTolerance(1E-9);
    }

    // -------------- Implementations for abstract methods -----------------------

    /** Creates the default continuous distribution instance to use in tests. */
    @Override
    public ExponentialDistribution makeDistribution() {
        return new ExponentialDistribution(5.0);
    }

    /** Creates the default cumulative probability distribution test input values */
    @Override
    public double[] makeCumulativeTestPoints() {
        // quantiles computed using R version 2.9.2
        return new double[] { 0.00500250166792, 0.0502516792675, 0.126589039921, 0.256466471938,
            0.526802578289, 34.5387763949, 23.0258509299, 18.4443972706, 14.9786613678, 11.5129254650 };
    }

    /** Creates the default cumulative probability density test expected values */
    @Override
    public double[] makeCumulativeTestValues() {
        return new double[] { 0.001, 0.01, 0.025, 0.05, 0.1, 0.999,
            0.990, 0.975, 0.950, 0.900 };
    }

    /** Creates the default probability density test expected values */
    @Override
    public double[] makeDensityTestValues() {
        return new double[] { 0.1998, 0.198, 0.195, 0.19, 0.18, 0.000200000000000,
            0.00200000000002, 0.00499999999997, 0.00999999999994, 0.0199999999999 };
    }

    // ------------ Additional tests -------------------------------------------

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
    public void testCumulativeProbability2() {
        final double actual = ((AbstractRealDistribution) this.getDistribution()).probability(0.25, 0.75);
        Assert.assertEquals(0.0905214, actual, 10e-4);
    }

    @Test
    public void testDensity() {
        final ExponentialDistribution d1 = new ExponentialDistribution(1);
        Assert.assertTrue(Precision.equals(0.0, d1.density(-1e-9), 1));
        Assert.assertTrue(Precision.equals(1.0, d1.density(0.0), 1));
        Assert.assertTrue(Precision.equals(0.0, d1.density(1000.0), 1));
        Assert.assertTrue(Precision.equals(MathLib.exp(-1), d1.density(1.0), 1));
        Assert.assertTrue(Precision.equals(MathLib.exp(-2), d1.density(2.0), 1));

        final ExponentialDistribution d2 = new ExponentialDistribution(3);
        Assert.assertTrue(Precision.equals(1 / 3.0, d2.density(0.0), 1));
        // computed using print(dexp(1, rate=1/3), digits=10) in R 2.5
        Assert.assertEquals(0.2388437702, d2.density(1.0), 1e-8);

        // computed using print(dexp(2, rate=1/3), digits=10) in R 2.5
        Assert.assertEquals(0.1711390397, d2.density(2.0), 1e-8);
    }

    @Test
    public void testMeanAccessors() {
        final ExponentialDistribution distribution = (ExponentialDistribution) this.getDistribution();
        Assert.assertEquals(5d, distribution.getMean(), Double.MIN_VALUE);
    }

    @Test(expected = NotStrictlyPositiveException.class)
    public void testPreconditions() {
        new ExponentialDistribution(0);
    }

    @Test
    public void testMoments() {
        final double tol = 1e-9;
        ExponentialDistribution dist;

        dist = new ExponentialDistribution(11d);
        Assert.assertEquals(dist.getNumericalMean(), 11d, tol);
        Assert.assertEquals(dist.getNumericalVariance(), 11d * 11d, tol);

        dist = new ExponentialDistribution(10.5d);
        Assert.assertEquals(dist.getNumericalMean(), 10.5d, tol);
        Assert.assertEquals(dist.getNumericalVariance(), 10.5d * 10.5d, tol);
    }
}
