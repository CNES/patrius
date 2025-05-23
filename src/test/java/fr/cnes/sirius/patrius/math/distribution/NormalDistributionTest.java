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
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
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
 * Test cases for {@link NormalDistribution}. Extends {@link RealDistributionAbstractTest}. See class javadoc of that
 * class
 * for details.
 * 
 * @version $Id: NormalDistributionTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class NormalDistributionTest extends RealDistributionAbstractTest {

    // -------------- Implementations for abstract methods -----------------------

    /** Creates the default real distribution instance to use in tests. */
    @Override
    public NormalDistribution makeDistribution() {
        return new NormalDistribution(2.1, 1.4);
    }

    /** Creates the default cumulative probability distribution test input values */
    @Override
    public double[] makeCumulativeTestPoints() {
        // quantiles computed using R
        return new double[] { -2.226325228634938d, -1.156887023657177d, -0.643949578356075d, -0.2027950777320613d,
            0.305827808237559d,
            6.42632522863494d, 5.35688702365718d, 4.843949578356074d, 4.40279507773206d, 3.89417219176244d };
    }

    /** Creates the default cumulative probability density test expected values */
    @Override
    public double[] makeCumulativeTestValues() {
        return new double[] { 0.001d, 0.01d, 0.025d, 0.05d, 0.1d, 0.999d,
            0.990d, 0.975d, 0.950d, 0.900d };
    }

    /** Creates the default probability density test expected values */
    @Override
    public double[] makeDensityTestValues() {
        return new double[] { 0.00240506434076, 0.0190372444310, 0.0417464784322, 0.0736683145538, 0.125355951380,
            0.00240506434076, 0.0190372444310, 0.0417464784322, 0.0736683145538, 0.125355951380 };
    }

    // --------------------- Override tolerance --------------
    protected double defaultTolerance = NormalDistribution.DEFAULT_INVERSE_ABSOLUTE_ACCURACY;

    @Override
    public void setUp() {
        super.setUp();
        this.setTolerance(this.defaultTolerance);
    }

    // ---------------------------- Additional test cases -------------------------

    private void verifyQuantiles() {
        final NormalDistribution distribution = (NormalDistribution) this.getDistribution();
        final double mu = distribution.getMean();
        final double sigma = distribution.getStandardDeviation();
        this.setCumulativeTestPoints(new double[] { mu - 2 * sigma, mu - sigma,
            mu, mu + sigma, mu + 2 * sigma, mu + 3 * sigma, mu + 4 * sigma,
            mu + 5 * sigma });
        // Quantiles computed using R (same as Mathematica)
        this.setCumulativeTestValues(new double[] { 0.02275013194817921, 0.158655253931457, 0.5, 0.841344746068543,
            0.977249868051821, 0.99865010196837, 0.999968328758167, 0.999999713348428 });
        this.verifyCumulativeProbabilities();
    }

    @Test
    public void testQuantiles() {
        this.setDensityTestValues(new double[] { 0.0385649760808, 0.172836231799, 0.284958771715, 0.172836231799,
            0.0385649760808,
            0.00316560600853, 9.55930184035e-05, 1.06194251052e-06 });
        this.verifyQuantiles();
        this.verifyDensities();

        this.setDistribution(new NormalDistribution(0, 1));
        this.setDensityTestValues(new double[] { 0.0539909665132, 0.241970724519, 0.398942280401, 0.241970724519,
            0.0539909665132,
            0.00443184841194, 0.000133830225765, 1.48671951473e-06 });
        this.verifyQuantiles();
        this.verifyDensities();

        this.setDistribution(new NormalDistribution(0, 0.1));
        this.setDensityTestValues(new double[] { 0.539909665132, 2.41970724519, 3.98942280401, 2.41970724519,
            0.539909665132, 0.0443184841194, 0.00133830225765, 1.48671951473e-05 });
        this.verifyQuantiles();
        this.verifyDensities();
    }

    @Test
    public void testInverseCumulativeProbabilityExtremes() {
        this.setInverseCumulativeTestPoints(new double[] { 0, 1 });
        this.setInverseCumulativeTestValues(new double[] { Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY });
        this.verifyInverseCumulativeProbabilities();
    }

    @Test
    public void testGetMean() {
        final NormalDistribution distribution = (NormalDistribution) this.getDistribution();
        Assert.assertEquals(2.1, distribution.getMean(), 0);
    }

    @Test
    public void testGetStandardDeviation() {
        final NormalDistribution distribution = (NormalDistribution) this.getDistribution();
        Assert.assertEquals(1.4, distribution.getStandardDeviation(), 0);
    }

    @Test(expected = NotStrictlyPositiveException.class)
    public void testPreconditions() {
        new NormalDistribution(1, 0);
    }

    @Test
    public void testDensity() {
        final double[] x = new double[] { -2, -1, 0, 1, 2 };
        // R 2.5: print(dnorm(c(-2,-1,0,1,2)), digits=10)
        this.checkDensity(0, 1, x,
            new double[] { 0.05399096651, 0.24197072452, 0.39894228040, 0.24197072452, 0.05399096651 });
        // R 2.5: print(dnorm(c(-2,-1,0,1,2), mean=1.1), digits=10)
        this.checkDensity(1.1, 1, x, new double[] { 0.003266819056, 0.043983595980, 0.217852177033, 0.396952547477,
            0.266085249899 });
    }

    private void checkDensity(final double mean, final double sd, final double[] x, final double[] expected) {
        final NormalDistribution d = new NormalDistribution(mean, sd);
        for (int i = 0; i < x.length; i++) {
            Assert.assertEquals(expected[i], d.density(x[i]), 1e-9);
        }
    }

    /**
     * Check to make sure top-coding of extreme values works correctly.
     * Verifies fixes for JIRA MATH-167, MATH-414
     */
    @Test
    public void testExtremeValues() {
        final NormalDistribution distribution = new NormalDistribution(0, 1);
        for (int i = 0; i < 100; i++) { // make sure no convergence exception
            final double lowerTail = distribution.cumulativeProbability(-i);
            final double upperTail = distribution.cumulativeProbability(i);
            if (i < 9) { // make sure not top-coded
                // For i = 10, due to bad tail precision in erf (MATH-364), 1 is returned
                Assert.assertTrue(lowerTail > 0.0d);
                Assert.assertTrue(upperTail < 1.0d);
            }
            else { // make sure top coding not reversed
                Assert.assertTrue(lowerTail < 0.00001);
                Assert.assertTrue(upperTail > 0.99999);
            }
        }

        Assert.assertEquals(distribution.cumulativeProbability(Double.MAX_VALUE), 1, 0);
        Assert.assertEquals(distribution.cumulativeProbability(-Double.MAX_VALUE), 0, 0);
        Assert.assertEquals(distribution.cumulativeProbability(Double.POSITIVE_INFINITY), 1, 0);
        Assert.assertEquals(distribution.cumulativeProbability(Double.NEGATIVE_INFINITY), 0, 0);
    }

    @Test
    public void testMath280() {
        final NormalDistribution normal = new NormalDistribution(0, 1);
        double result = normal.inverseCumulativeProbability(0.9986501019683698);
        Assert.assertEquals(3.0, result, this.defaultTolerance);
        result = normal.inverseCumulativeProbability(0.841344746068543);
        Assert.assertEquals(1.0, result, this.defaultTolerance);
        result = normal.inverseCumulativeProbability(0.9999683287581673);
        Assert.assertEquals(4.0, result, this.defaultTolerance);
        result = normal.inverseCumulativeProbability(0.9772498680518209);
        Assert.assertEquals(2.0, result, this.defaultTolerance);
    }

    @Test
    public void testMoments() {
        final double tol = 1e-9;
        NormalDistribution dist;

        dist = new NormalDistribution(0, 1);
        Assert.assertEquals(dist.getNumericalMean(), 0, tol);
        Assert.assertEquals(dist.getNumericalVariance(), 1, tol);

        dist = new NormalDistribution(2.2, 1.4);
        Assert.assertEquals(dist.getNumericalMean(), 2.2, tol);
        Assert.assertEquals(dist.getNumericalVariance(), 1.4 * 1.4, tol);

        dist = new NormalDistribution(-2000.9, 10.4);
        Assert.assertEquals(dist.getNumericalMean(), -2000.9, tol);
        Assert.assertEquals(dist.getNumericalVariance(), 10.4 * 10.4, tol);
    }
}
