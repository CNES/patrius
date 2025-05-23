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

import fr.cnes.sirius.patrius.math.exception.NumberIsTooLargeException;

/**
 * Test cases for UniformRealDistribution. See class javadoc for {@link RealDistributionAbstractTest} for further
 * details.
 */
public class UniformRealDistributionTest extends RealDistributionAbstractTest {

    // --- Override tolerance -------------------------------------------------

    @Override
    public void setUp() {
        super.setUp();
        this.setTolerance(1e-4);
    }

    // --- Implementations for abstract methods --------------------------------

    /** Creates the default uniform real distribution instance to use in tests. */
    @Override
    public UniformRealDistribution makeDistribution() {
        return new UniformRealDistribution(-0.5, 1.25);
    }

    /** Creates the default cumulative probability distribution test input values */
    @Override
    public double[] makeCumulativeTestPoints() {
        return new double[] { -0.5001, -0.5, -0.4999, -0.25, -0.0001, 0.0,
            0.0001, 0.25, 1.0, 1.2499, 1.25, 1.2501 };
    }

    /** Creates the default cumulative probability density test expected values */
    @Override
    public double[] makeCumulativeTestValues() {
        return new double[] { 0.0, 0.0, 0.0001, 0.25 / 1.75, 0.4999 / 1.75,
            0.5 / 1.75, 0.5001 / 1.75, 0.75 / 1.75, 1.5 / 1.75,
            1.7499 / 1.75, 1.0, 1.0 };
    }

    /** Creates the default probability density test expected values */
    @Override
    public double[] makeDensityTestValues() {
        final double d = 1 / 1.75;
        return new double[] { 0, d, d, d, d, d, d, d, d, d, d, 0 };
    }

    // --- Additional test cases -----------------------------------------------

    /** Test lower bound getter. */
    @Test
    public void testGetLowerBound() {
        final UniformRealDistribution distribution = this.makeDistribution();
        Assert.assertEquals(-0.5, distribution.getSupportLowerBound(), 0);
    }

    /** Test upper bound getter. */
    @Test
    public void testGetUpperBound() {
        final UniformRealDistribution distribution = this.makeDistribution();
        Assert.assertEquals(1.25, distribution.getSupportUpperBound(), 0);
    }

    /** Test pre-condition for equal lower/upper bound. */
    @Test(expected = NumberIsTooLargeException.class)
    public void testPreconditions1() {
        new UniformRealDistribution(0, 0);
    }

    /** Test pre-condition for lower bound larger than upper bound. */
    @Test(expected = NumberIsTooLargeException.class)
    public void testPreconditions2() {
        new UniformRealDistribution(1, 0);
    }

    /** Test mean/variance. */
    @Test
    public void testMeanVariance() {
        UniformRealDistribution dist;

        dist = new UniformRealDistribution(0, 1);
        Assert.assertEquals(dist.getNumericalMean(), 0.5, 0);
        Assert.assertEquals(dist.getNumericalVariance(), 1 / 12.0, 0);

        dist = new UniformRealDistribution(-1.5, 0.6);
        Assert.assertEquals(dist.getNumericalMean(), -0.45, 0);
        Assert.assertEquals(dist.getNumericalVariance(), 0.3675, 0);

        dist = new UniformRealDistribution(-0.5, 1.25);
        Assert.assertEquals(dist.getNumericalMean(), 0.375, 0);
        Assert.assertEquals(dist.getNumericalVariance(), 0.2552083333333333, 0);
    }
}
