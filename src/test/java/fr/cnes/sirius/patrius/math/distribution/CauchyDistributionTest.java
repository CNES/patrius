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
 * Test cases for CauchyDistribution.
 * Extends ContinuousDistributionAbstractTest. See class javadoc for
 * ContinuousDistributionAbstractTest for details.
 * 
 * @version $Id: CauchyDistributionTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class CauchyDistributionTest extends RealDistributionAbstractTest {

    // --------------------- Override tolerance --------------
    protected double defaultTolerance = NormalDistribution.DEFAULT_INVERSE_ABSOLUTE_ACCURACY;

    @Override
    public void setUp() {
        super.setUp();
        this.setTolerance(this.defaultTolerance);
    }

    // -------------- Implementations for abstract methods -----------------------

    /** Creates the default continuous distribution instance to use in tests. */
    @Override
    public CauchyDistribution makeDistribution() {
        return new CauchyDistribution(1.2, 2.1);
    }

    /** Creates the default cumulative probability distribution test input values */
    @Override
    public double[] makeCumulativeTestPoints() {
        // quantiles computed using R 2.9.2
        return new double[] { -667.24856187, -65.6230835029, -25.4830299460, -12.0588781808,
            -5.26313542807, 669.64856187, 68.0230835029, 27.8830299460, 14.4588781808, 7.66313542807 };
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
        return new double[] { 1.49599158008e-06, 0.000149550440335, 0.000933076881878, 0.00370933207799,
            0.0144742330437,
            1.49599158008e-06, 0.000149550440335, 0.000933076881878, 0.00370933207799, 0.0144742330437 };
    }

    // ---------------------------- Additional test cases -------------------------

    @Test
    public void testInverseCumulativeProbabilityExtremes() {
        this.setInverseCumulativeTestPoints(new double[] { 0.0, 1.0 });
        this.setInverseCumulativeTestValues(new double[] { Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY });
        this.verifyInverseCumulativeProbabilities();
    }

    @Test
    public void testMedian() {
        final CauchyDistribution distribution = (CauchyDistribution) this.getDistribution();
        Assert.assertEquals(1.2, distribution.getMedian(), 0.0);
    }

    @Test
    public void testScale() {
        final CauchyDistribution distribution = (CauchyDistribution) this.getDistribution();
        Assert.assertEquals(2.1, distribution.getScale(), 0.0);
    }

    @Test
    public void testPreconditions() {
        try {
            new CauchyDistribution(0, 0);
            Assert.fail("Cannot have zero scale");
        } catch (final NotStrictlyPositiveException ex) {
            // Expected.
        }
        try {
            new CauchyDistribution(0, -1);
            Assert.fail("Cannot have negative scale");
        } catch (final NotStrictlyPositiveException ex) {
            // Expected.
        }
    }

    @Test
    public void testMoments() {
        CauchyDistribution dist;

        dist = new CauchyDistribution(10.2, 0.15);
        Assert.assertTrue(Double.isNaN(dist.getNumericalMean()));
        Assert.assertTrue(Double.isNaN(dist.getNumericalVariance()));

        dist = new CauchyDistribution(23.12, 2.12);
        Assert.assertTrue(Double.isNaN(dist.getNumericalMean()));
        Assert.assertTrue(Double.isNaN(dist.getNumericalVariance()));
    }
}
