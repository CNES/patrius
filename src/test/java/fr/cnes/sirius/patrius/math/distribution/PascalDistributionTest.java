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
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.distribution;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test cases for PascalDistribution.
 * Extends IntegerDistributionAbstractTest. See class javadoc for
 * IntegerDistributionAbstractTest for details.
 * 
 * @version $Id: PascalDistributionTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class PascalDistributionTest extends IntegerDistributionAbstractTest {

    // --------------------- Override tolerance --------------
    protected double defaultTolerance = NormalDistribution.DEFAULT_INVERSE_ABSOLUTE_ACCURACY;

    @Override
    public void setUp() {
        super.setUp();
        this.setTolerance(this.defaultTolerance);
    }

    // -------------- Implementations for abstract methods -----------------------

    /** Creates the default discrete distribution instance to use in tests. */
    @Override
    public IntegerDistribution makeDistribution() {
        return new PascalDistribution(10, 0.70);
    }

    /** Creates the default probability density test input values */
    @Override
    public int[] makeDensityTestPoints() {
        return new int[] { -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 };
    }

    /** Creates the default probability density test expected values */
    @Override
    public double[] makeDensityTestValues() {
        return new double[] { 0, 0.0282475249, 0.0847425747, 0.139825248255, 0.167790297906, 0.163595540458,
            0.137420253985, 0.103065190489, 0.070673273478, 0.0450542118422, 0.0270325271053,
            0.0154085404500, 0.0084046584273 };
    }

    /** Creates the default cumulative probability density test input values */
    @Override
    public int[] makeCumulativeTestPoints() {
        return this.makeDensityTestPoints();
    }

    /** Creates the default cumulative probability density test expected values */
    @Override
    public double[] makeCumulativeTestValues() {
        return new double[] { 0, 0.0282475249, 0.1129900996, 0.252815347855, 0.420605645761, 0.584201186219,
            0.721621440204, 0.824686630693, 0.895359904171, 0.940414116013, 0.967446643119,
            0.982855183569, 0.991259841996 };
    }

    /** Creates the default inverse cumulative probability test input values */
    @Override
    public double[] makeInverseCumulativeTestPoints() {
        return new double[] { 0.0, 0.001, 0.010, 0.025, 0.050, 0.100, 0.999,
            0.990, 0.975, 0.950, 0.900, 1.0 };
    }

    /** Creates the default inverse cumulative probability density test expected values */
    @Override
    public int[] makeInverseCumulativeTestValues() {
        return new int[] { 0, 0, 0, 0, 1, 1, 14, 11, 10, 9, 8, Integer.MAX_VALUE };
    }

    // ----------------- Additional test cases ---------------------------------

    /** Test degenerate case p = 0 */
    @Test
    public void testDegenerate0() {
        this.setDistribution(new PascalDistribution(5, 0.0d));
        this.setCumulativeTestPoints(new int[] { -1, 0, 1, 5, 10 });
        this.setCumulativeTestValues(new double[] { 0d, 0d, 0d, 0d, 0d });
        this.setDensityTestPoints(new int[] { -1, 0, 1, 10, 11 });
        this.setDensityTestValues(new double[] { 0d, 0d, 0d, 0d, 0d });
        this.setInverseCumulativeTestPoints(new double[] { 0.1d, 0.5d });
        this.setInverseCumulativeTestValues(new int[] { Integer.MAX_VALUE, Integer.MAX_VALUE });
        this.verifyDensities();
        this.verifyCumulativeProbabilities();
        this.verifyInverseCumulativeProbabilities();
    }

    /** Test degenerate case p = 1 */
    @Test
    public void testDegenerate1() {
        this.setDistribution(new PascalDistribution(5, 1.0d));
        this.setCumulativeTestPoints(new int[] { -1, 0, 1, 2, 5, 10 });
        this.setCumulativeTestValues(new double[] { 0d, 1d, 1d, 1d, 1d, 1d });
        this.setDensityTestPoints(new int[] { -1, 0, 1, 2, 5, 10 });
        this.setDensityTestValues(new double[] { 0d, 1d, 0d, 0d, 0d, 0d });
        this.setInverseCumulativeTestPoints(new double[] { 0.1d, 0.5d });
        this.setInverseCumulativeTestValues(new int[] { 0, 0 });
        this.verifyDensities();
        this.verifyCumulativeProbabilities();
        this.verifyInverseCumulativeProbabilities();
    }

    @Test
    public void testMoments() {
        final double tol = 1e-9;
        PascalDistribution dist;

        dist = new PascalDistribution(10, 0.5);
        Assert.assertEquals(dist.getNumericalMean(), (10d * 0.5d) / 0.5d, tol);
        Assert.assertEquals(dist.getNumericalVariance(), (10d * 0.5d) / (0.5d * 0.5d), tol);

        dist = new PascalDistribution(25, 0.7);
        Assert.assertEquals(dist.getNumericalMean(), (25d * 0.3d) / 0.7d, tol);
        Assert.assertEquals(dist.getNumericalVariance(), (25d * 0.3d) / (0.7d * 0.7d), tol);
    }
}
