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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.integration.BaseAbstractUnivariateIntegrator;
import fr.cnes.sirius.patrius.math.analysis.integration.IterativeLegendreGaussIntegrator;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooLargeException;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Abstract base class for {@link RealDistribution} tests.
 * <p>
 * To create a concrete test class for a continuous distribution implementation, first implement makeDistribution() to
 * return a distribution instance to use in tests. Then implement each of the test data generation methods below. In
 * each case, the test points and test values arrays returned represent parallel arrays of inputs and expected values
 * for the distribution returned by makeDistribution(). Default implementations are provided for the makeInverseXxx
 * methods that just invert the mapping defined by the arrays returned by the makeCumulativeXxx methods.
 * <p>
 * makeCumulativeTestPoints() -- arguments used to test cumulative probabilities makeCumulativeTestValues() -- expected
 * cumulative probabilites makeDensityTestValues() -- expected density values at cumulativeTestPoints
 * makeInverseCumulativeTestPoints() -- arguments used to test inverse cdf makeInverseCumulativeTestValues() -- expected
 * inverse cdf values
 * <p>
 * To implement additional test cases with different distribution instances and test data, use the setXxx methods for
 * the instance data in test cases and call the verifyXxx methods to verify results.
 * <p>
 * Error tolerance can be overriden by implementing getTolerance().
 * <p>
 * Test data should be validated against reference tables or other packages where possible, and the source of the
 * reference data and/or validation should be documented in the test cases. A framework for validating distribution data
 * against R is included in the /src/test/R source tree.
 * <p>
 * See {@link NormalDistributionTest} and {@link ChiSquaredDistributionTest} for examples.
 * 
 * @version $Id: RealDistributionAbstractTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
public abstract class RealDistributionAbstractTest {

    // -------------------- Private test instance data -------------------------
    /** Distribution instance used to perform tests */
    private RealDistribution distribution;

    /** Tolerance used in comparing expected and returned values */
    private double tolerance = 1E-4;

    /** Arguments used to test cumulative probability density calculations */
    private double[] cumulativeTestPoints;

    /** Values used to test cumulative probability density calculations */
    private double[] cumulativeTestValues;

    /** Arguments used to test inverse cumulative probability density calculations */
    private double[] inverseCumulativeTestPoints;

    /** Values used to test inverse cumulative probability density calculations */
    private double[] inverseCumulativeTestValues;

    /** Values used to test density calculations */
    private double[] densityTestValues;

    // -------------------- Abstract methods -----------------------------------

    /** Creates the default continuous distribution instance to use in tests. */
    public abstract RealDistribution makeDistribution();

    /** Creates the default cumulative probability test input values */
    public abstract double[] makeCumulativeTestPoints();

    /** Creates the default cumulative probability test expected values */
    public abstract double[] makeCumulativeTestValues();

    /** Creates the default density test expected values */
    public abstract double[] makeDensityTestValues();

    // ---- Default implementations of inverse test data generation methods ----

    /** Creates the default inverse cumulative probability test input values */
    public double[] makeInverseCumulativeTestPoints() {
        return this.makeCumulativeTestValues();
    }

    /** Creates the default inverse cumulative probability density test expected values */
    public double[] makeInverseCumulativeTestValues() {
        return this.makeCumulativeTestPoints();
    }

    // -------------------- Setup / tear down ----------------------------------

    /**
     * Setup sets all test instance data to default values
     */
    @Before
    public void setUp() {
        this.distribution = this.makeDistribution();
        this.cumulativeTestPoints = this.makeCumulativeTestPoints();
        this.cumulativeTestValues = this.makeCumulativeTestValues();
        this.inverseCumulativeTestPoints = this.makeInverseCumulativeTestPoints();
        this.inverseCumulativeTestValues = this.makeInverseCumulativeTestValues();
        this.densityTestValues = this.makeDensityTestValues();
    }

    /**
     * Cleans up test instance data
     */
    @After
    public void tearDown() {
        this.distribution = null;
        this.cumulativeTestPoints = null;
        this.cumulativeTestValues = null;
        this.inverseCumulativeTestPoints = null;
        this.inverseCumulativeTestValues = null;
        this.densityTestValues = null;
    }

    // -------------------- Verification methods -------------------------------

    /**
     * Verifies that cumulative probability density calculations match expected values
     * using current test instance data
     */
    protected void verifyCumulativeProbabilities() {
        // verify cumulativeProbability(double)
        for (int i = 0; i < this.cumulativeTestPoints.length; i++) {
            TestUtils.assertEquals("Incorrect cumulative probability value returned for "
                + this.cumulativeTestPoints[i], this.cumulativeTestValues[i],
                this.distribution.cumulativeProbability(this.cumulativeTestPoints[i]),
                this.getTolerance());
        }
        // verify cumulativeProbability(double, double)
        // XXX In 4.0, "cumulativeProbability(double,double)" must be replaced with "probability" (MATH-839).
        for (int i = 0; i < this.cumulativeTestPoints.length; i++) {
            for (int j = 0; j < this.cumulativeTestPoints.length; j++) {
                if (this.cumulativeTestPoints[i] <= this.cumulativeTestPoints[j]) {
                    TestUtils.assertEquals(this.cumulativeTestValues[j] - this.cumulativeTestValues[i],
                        this.distribution.probability(this.cumulativeTestPoints[i], this.cumulativeTestPoints[j]),
                        this.getTolerance());
                } else {
                    try {
                        this.distribution.probability(this.cumulativeTestPoints[i], this.cumulativeTestPoints[j]);
                    } catch (final NumberIsTooLargeException e) {
                        continue;
                    }
                    Assert
                        .fail("distribution.cumulativeProbability(double, double) should have thrown an exception that second argument is too large");
                }
            }
        }
    }

    /**
     * Verifies that inverse cumulative probability density calculations match expected values
     * using current test instance data
     */
    protected void verifyInverseCumulativeProbabilities() {
        for (int i = 0; i < this.inverseCumulativeTestPoints.length; i++) {
            TestUtils.assertEquals("Incorrect inverse cumulative probability value returned for "
                + this.inverseCumulativeTestPoints[i], this.inverseCumulativeTestValues[i],
                this.distribution.inverseCumulativeProbability(this.inverseCumulativeTestPoints[i]),
                this.getTolerance());
        }
    }

    /**
     * Verifies that density calculations match expected values
     */
    protected void verifyDensities() {
        for (int i = 0; i < this.cumulativeTestPoints.length; i++) {
            TestUtils.assertEquals("Incorrect probability density value returned for "
                + this.cumulativeTestPoints[i], this.densityTestValues[i],
                this.distribution.density(this.cumulativeTestPoints[i]),
                this.getTolerance());
        }
    }

    // ------------------------ Default test cases -----------------------------

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

    /**
     * Verifies that density calculations return expected values
     * for default test instance data
     */
    @Test
    public void testDensities() {
        this.verifyDensities();
    }

    /**
     * Verifies that probability computations are consistent
     */
    @Test
    public void testConsistency() {
        for (int i = 1; i < this.cumulativeTestPoints.length; i++) {

            // check that cdf(x, x) = 0
            // XXX In 4.0, "cumulativeProbability(double,double)" must be replaced with "probability" (MATH-839).
            TestUtils.assertEquals(0d,
                ((AbstractRealDistribution) this.distribution).probability
                    (this.cumulativeTestPoints[i], this.cumulativeTestPoints[i]), this.tolerance);

            // check that P(a < X <= b) = P(X <= b) - P(X <= a)
            final double upper = MathLib.max(this.cumulativeTestPoints[i], this.cumulativeTestPoints[i - 1]);
            final double lower = MathLib.min(this.cumulativeTestPoints[i], this.cumulativeTestPoints[i - 1]);
            final double diff = this.distribution.cumulativeProbability(upper) -
                this.distribution.cumulativeProbability(lower);
            // XXX In 4.0, "cumulativeProbability(double,double)" must be replaced with "probability" (MATH-839).
            final double direct = ((AbstractRealDistribution) this.distribution).probability(lower, upper);
            TestUtils.assertEquals("Inconsistent cumulative probabilities for ("
                + lower + "," + upper + ")", diff, direct, this.tolerance);
        }
    }

    /**
     * Verifies that illegal arguments are correctly handled
     */
    @Test
    public void testIllegalArguments() {
        try {
            // XXX In 4.0, "cumulativeProbability(double,double)" must be replaced with "probability" (MATH-839).
            this.distribution.probability(1, 0);
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
        final int sampleSize = 1000;
        this.distribution.reseedRandomGenerator(1000); // Use fixed seed
        final double[] sample = this.distribution.sample(sampleSize);
        final double[] quartiles = TestUtils.getDistributionQuartiles(this.distribution);
        final double[] expected = { 250, 250, 250, 250 };
        final long[] counts = new long[4];
        for (int i = 0; i < sampleSize; i++) {
            TestUtils.updateCounts(sample[i], counts, quartiles);
        }
        TestUtils.assertChiSquareAccept(expected, counts, 0.001);
    }

    /**
     * Verify that density integrals match the distribution.
     * The (filtered, sorted) cumulativeTestPoints array is used to source
     * integration limits. The integral of the density (estimated using a
     * Legendre-Gauss integrator) is compared with the cdf over the same
     * interval. Test points outside of the domain of the density function
     * are discarded.
     */
    @Test
    public void testDensityIntegrals() {
        final double tol = 1.0e-9;
        final BaseAbstractUnivariateIntegrator integrator =
            new IterativeLegendreGaussIntegrator(5, 1.0e-12, 1.0e-10);
        final UnivariateFunction d = new UnivariateFunction(){
            /** Serializable UID. */
            private static final long serialVersionUID = 7473628483840245309L;

            @Override
            public double value(final double x) {
                return RealDistributionAbstractTest.this.distribution.density(x);
            }
        };
        final ArrayList<Double> integrationTestPoints = new ArrayList<>();
        for (int i = 0; i < this.cumulativeTestPoints.length; i++) {
            if (Double.isNaN(this.cumulativeTestValues[i]) ||
                this.cumulativeTestValues[i] < 1.0e-5 ||
                this.cumulativeTestValues[i] > 1 - 1.0e-5) {
                continue; // exclude integrals outside domain.
            }
            integrationTestPoints.add(this.cumulativeTestPoints[i]);
        }
        Collections.sort(integrationTestPoints);
        for (int i = 1; i < integrationTestPoints.size(); i++) {
            Assert.assertEquals(
                ((AbstractRealDistribution) this.distribution).probability(
                    integrationTestPoints.get(0), integrationTestPoints.get(i)),
                integrator.integrate(
                    1000000, // Triangle integrals are very slow to converge
                    d, integrationTestPoints.get(0),
                    integrationTestPoints.get(i)), tol);
        }
    }

    @Test
    public void testDistributionClone()
                                       throws IOException,
                                       ClassNotFoundException {
        // Construct a distribution and initialize its internal random
        // generator, using a fixed seed for deterministic results.
        this.distribution.reseedRandomGenerator(123);
        this.distribution.sample();

        // Clone the distribution.
        final RealDistribution cloned = this.deepClone();

        // Make sure they still produce the same samples.
        final double s1 = this.distribution.sample();
        final double s2 = cloned.sample();
        Assert.assertEquals(s1, s2, 0d);
    }

    // ------------------ Getters / Setters for test instance data -----------
    /**
     * @return Returns the cumulativeTestPoints.
     */
    protected double[] getCumulativeTestPoints() {
        return this.cumulativeTestPoints;
    }

    /**
     * @param cumulativeTestPoints
     *        The cumulativeTestPoints to set.
     */
    protected void setCumulativeTestPoints(final double[] cumulativeTestPoints) {
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

    protected double[] getDensityTestValues() {
        return this.densityTestValues;
    }

    protected void setDensityTestValues(final double[] densityTestValues) {
        this.densityTestValues = densityTestValues;
    }

    /**
     * @return Returns the distribution.
     */
    protected RealDistribution getDistribution() {
        return this.distribution;
    }

    /**
     * @param distribution
     *        The distribution to set.
     */
    protected void setDistribution(final RealDistribution distribution) {
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
    protected double[] getInverseCumulativeTestValues() {
        return this.inverseCumulativeTestValues;
    }

    /**
     * @param inverseCumulativeTestValues
     *        The inverseCumulativeTestValues to set.
     */
    protected void setInverseCumulativeTestValues(final double[] inverseCumulativeTestValues) {
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

    /**
     * Serialization and deserialization loop of the {@link #distribution}.
     */
    private RealDistribution deepClone()
                                        throws IOException,
                                        ClassNotFoundException {
        // Serialize to byte array.
        final ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        final ObjectOutputStream oOut = new ObjectOutputStream(bOut);
        oOut.writeObject(this.distribution);
        final byte[] data = bOut.toByteArray();

        // Deserialize from byte array.
        final ByteArrayInputStream bIn = new ByteArrayInputStream(data);
        final ObjectInputStream oIn = new ObjectInputStream(bIn);
        final Object clone = oIn.readObject();
        oIn.close();

        return (RealDistribution) clone;
    }
}
