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

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;

/**
 * Abstract base class for {@link IntegerDistribution} tests.
 * <p>
 * To create a concrete test class for an integer distribution implementation, implement makeDistribution() to return a
 * distribution instance to use in tests and each of the test data generation methods below. In each case, the test
 * points and test values arrays returned represent parallel arrays of inputs and expected values for the distribution
 * returned by makeDistribution().
 * <p>
 * makeDensityTestPoints() -- arguments used to test probability density calculation makeDensityTestValues() -- expected
 * probability densities makeCumulativeTestPoints() -- arguments used to test cumulative probabilities
 * makeCumulativeTestValues() -- expected cumulative probabilites makeInverseCumulativeTestPoints() -- arguments used to
 * test inverse cdf evaluation makeInverseCumulativeTestValues() -- expected inverse cdf values
 * <p>
 * To implement additional test cases with different distribution instances and test data, use the setXxx methods for
 * the instance data in test cases and call the verifyXxx methods to verify results.
 * 
 * @version $Id: IntegerDistributionAbstractTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
public abstract class IntegerDistributionAbstractTest {

    // -------------------- Private test instance data -------------------------
    /** Discrete distribution instance used to perform tests */
    private IntegerDistribution distribution;

    /** Tolerance used in comparing expected and returned values */
    private double tolerance = 1E-4;

    /** Arguments used to test probability density calculations */
    private int[] densityTestPoints;

    /** Values used to test probability density calculations */
    private double[] densityTestValues;

    /** Arguments used to test cumulative probability density calculations */
    private int[] cumulativeTestPoints;

    /** Values used to test cumulative probability density calculations */
    private double[] cumulativeTestValues;

    /** Arguments used to test inverse cumulative probability density calculations */
    private double[] inverseCumulativeTestPoints;

    /** Values used to test inverse cumulative probability density calculations */
    private int[] inverseCumulativeTestValues;

    // -------------------- Abstract methods -----------------------------------

    /** Creates the default discrete distribution instance to use in tests. */
    public abstract IntegerDistribution makeDistribution();

    /** Creates the default probability density test input values */
    public abstract int[] makeDensityTestPoints();

    /** Creates the default probability density test expected values */
    public abstract double[] makeDensityTestValues();

    /** Creates the default cumulative probability density test input values */
    public abstract int[] makeCumulativeTestPoints();

    /** Creates the default cumulative probability density test expected values */
    public abstract double[] makeCumulativeTestValues();

    /** Creates the default inverse cumulative probability test input values */
    public abstract double[] makeInverseCumulativeTestPoints();

    /** Creates the default inverse cumulative probability density test expected values */
    public abstract int[] makeInverseCumulativeTestValues();

    // -------------------- Setup / tear down ----------------------------------

    /**
     * Setup sets all test instance data to default values
     */
    @Before
    public void setUp() {
        this.distribution = this.makeDistribution();
        this.densityTestPoints = this.makeDensityTestPoints();
        this.densityTestValues = this.makeDensityTestValues();
        this.cumulativeTestPoints = this.makeCumulativeTestPoints();
        this.cumulativeTestValues = this.makeCumulativeTestValues();
        this.inverseCumulativeTestPoints = this.makeInverseCumulativeTestPoints();
        this.inverseCumulativeTestValues = this.makeInverseCumulativeTestValues();
    }

    /**
     * Cleans up test instance data
     */
    @After
    public void tearDown() {
        this.distribution = null;
        this.densityTestPoints = null;
        this.densityTestValues = null;
        this.cumulativeTestPoints = null;
        this.cumulativeTestValues = null;
        this.inverseCumulativeTestPoints = null;
        this.inverseCumulativeTestValues = null;
    }

    // -------------------- Verification methods -------------------------------

    /**
     * Verifies that probability density calculations match expected values
     * using current test instance data
     */
    protected void verifyDensities() {
        for (int i = 0; i < this.densityTestPoints.length; i++) {
            Assert.assertEquals("Incorrect density value returned for " + this.densityTestPoints[i],
                this.densityTestValues[i],
                this.distribution.probability(this.densityTestPoints[i]), this.tolerance);
        }
    }

    /**
     * Verifies that cumulative probability density calculations match expected values
     * using current test instance data
     */
    protected void verifyCumulativeProbabilities() {
        for (int i = 0; i < this.cumulativeTestPoints.length; i++) {
            Assert.assertEquals("Incorrect cumulative probability value returned for " + this.cumulativeTestPoints[i],
                this.cumulativeTestValues[i],
                this.distribution.cumulativeProbability(this.cumulativeTestPoints[i]), this.tolerance);
        }
    }

    /**
     * Verifies that inverse cumulative probability density calculations match expected values
     * using current test instance data
     */
    protected void verifyInverseCumulativeProbabilities() {
        for (int i = 0; i < this.inverseCumulativeTestPoints.length; i++) {
            Assert.assertEquals("Incorrect inverse cumulative probability value returned for "
                + this.inverseCumulativeTestPoints[i], this.inverseCumulativeTestValues[i],
                this.distribution.inverseCumulativeProbability(this.inverseCumulativeTestPoints[i]));
        }
    }

    // ------------------------ Default test cases -----------------------------

    /**
     * Verifies that probability density calculations match expected values
     * using default test instance data
     */
    @Test
    public void testDensities() {
        this.verifyDensities();
    }

    /**
     * Verifies that cumulative probability density calculations match expected values
     * using default test instance data
     */
    @Test
    public void testCumulativeProbabilities() {
        this.verifyCumulativeProbabilities();
    }

    /**
     * Verifies that inverse cumulative probability density calculations match expected values
     * using default test instance data
     */
    @Test
    public void testInverseCumulativeProbabilities() {
        this.verifyInverseCumulativeProbabilities();
    }

    @Test
    public void testConsistencyAtSupportBounds() {
        final int lower = this.distribution.getSupportLowerBound();
        Assert.assertEquals("Cumulative probability mmust be 0 below support lower bound.",
            0.0, this.distribution.cumulativeProbability(lower - 1), 0.0);
        Assert.assertEquals(
            "Cumulative probability of support lower bound must be equal to probability mass at this point.",
            this.distribution.probability(lower), this.distribution.cumulativeProbability(lower), this.tolerance);
        Assert.assertEquals("Inverse cumulative probability of 0 must be equal to support lower bound.",
            lower, this.distribution.inverseCumulativeProbability(0.0));

        final int upper = this.distribution.getSupportUpperBound();
        if (upper != Integer.MAX_VALUE) {
            Assert.assertEquals("Cumulative probability of support upper bound must be equal to 1.",
                1.0, this.distribution.cumulativeProbability(upper), 0.0);
        }
        Assert.assertEquals("Inverse cumulative probability of 1 must be equal to support upper bound.",
            upper, this.distribution.inverseCumulativeProbability(1.0));
    }

    /**
     * Verifies that illegal arguments are correctly handled
     */
    @Test
    public void testIllegalArguments() {
        try {
            this.distribution.cumulativeProbability(1, 0);
            Assert.fail("Expecting MathIllegalArgumentException for bad cumulativeProbability interval");
        } catch (final MathIllegalArgumentException ex) {
            // expected
        }
        try {
            this.distribution.inverseCumulativeProbability(-1);
            Assert.fail("Expecting MathIllegalArgumentException for p = -1");
        } catch (final MathIllegalArgumentException ex) {
            // expected
        }
        try {
            this.distribution.inverseCumulativeProbability(2);
            Assert.fail("Expecting MathIllegalArgumentException for p = 2");
        } catch (final MathIllegalArgumentException ex) {
            // expected
        }
    }

    /**
     * Test sampling
     */
    @Test
    public void testSampling() {
        final int[] densityPoints = this.makeDensityTestPoints();
        final double[] densityValues = this.makeDensityTestValues();
        final int sampleSize = 1000;
        final int length = TestUtils.eliminateZeroMassPoints(densityPoints, densityValues);
        final AbstractIntegerDistribution distribution = (AbstractIntegerDistribution) this.makeDistribution();
        final double[] expectedCounts = new double[length];
        final long[] observedCounts = new long[length];
        for (int i = 0; i < length; i++) {
            expectedCounts[i] = sampleSize * densityValues[i];
        }
        distribution.reseedRandomGenerator(1000); // Use fixed seed
        final int[] sample = distribution.sample(sampleSize);
        for (int i = 0; i < sampleSize; i++) {
            for (int j = 0; j < length; j++) {
                if (sample[i] == densityPoints[j]) {
                    observedCounts[j]++;
                }
            }
        }
        TestUtils.assertChiSquareAccept(densityPoints, expectedCounts, observedCounts, .001);
    }

    // ------------------ Getters / Setters for test instance data -----------
    /**
     * @return Returns the cumulativeTestPoints.
     */
    protected int[] getCumulativeTestPoints() {
        return this.cumulativeTestPoints;
    }

    /**
     * @param cumulativeTestPoints
     *        The cumulativeTestPoints to set.
     */
    protected void setCumulativeTestPoints(final int[] cumulativeTestPoints) {
        this.cumulativeTestPoints = cumulativeTestPoints;
    }

    /**
     * @return Returns the cumulativeTestValues.
     */
    protected double[] getCumulativeTestValues() {
        return this.cumulativeTestValues;
    }

    /**
     * @param cumulativeTestValues
     *        The cumulativeTestValues to set.
     */
    protected void setCumulativeTestValues(final double[] cumulativeTestValues) {
        this.cumulativeTestValues = cumulativeTestValues;
    }

    /**
     * @return Returns the densityTestPoints.
     */
    protected int[] getDensityTestPoints() {
        return this.densityTestPoints;
    }

    /**
     * @param densityTestPoints
     *        The densityTestPoints to set.
     */
    protected void setDensityTestPoints(final int[] densityTestPoints) {
        this.densityTestPoints = densityTestPoints;
    }

    /**
     * @return Returns the densityTestValues.
     */
    protected double[] getDensityTestValues() {
        return this.densityTestValues;
    }

    /**
     * @param densityTestValues
     *        The densityTestValues to set.
     */
    protected void setDensityTestValues(final double[] densityTestValues) {
        this.densityTestValues = densityTestValues;
    }

    /**
     * @return Returns the distribution.
     */
    protected IntegerDistribution getDistribution() {
        return this.distribution;
    }

    /**
     * @param distribution
     *        The distribution to set.
     */
    protected void setDistribution(final IntegerDistribution distribution) {
        this.distribution = distribution;
    }

    /**
     * @return Returns the inverseCumulativeTestPoints.
     */
    protected double[] getInverseCumulativeTestPoints() {
        return this.inverseCumulativeTestPoints;
    }

    /**
     * @param inverseCumulativeTestPoints
     *        The inverseCumulativeTestPoints to set.
     */
    protected void setInverseCumulativeTestPoints(final double[] inverseCumulativeTestPoints) {
        this.inverseCumulativeTestPoints = inverseCumulativeTestPoints;
    }

    /**
     * @return Returns the inverseCumulativeTestValues.
     */
    protected int[] getInverseCumulativeTestValues() {
        return this.inverseCumulativeTestValues;
    }

    /**
     * @param inverseCumulativeTestValues
     *        The inverseCumulativeTestValues to set.
     */
    protected void setInverseCumulativeTestValues(final int[] inverseCumulativeTestValues) {
        this.inverseCumulativeTestValues = inverseCumulativeTestValues;
    }

    /**
     * @return Returns the tolerance.
     */
    protected double getTolerance() {
        return this.tolerance;
    }

    /**
     * @param tolerance
     *        The tolerance to set.
     */
    protected void setTolerance(final double tolerance) {
        this.tolerance = tolerance;
    }

}
