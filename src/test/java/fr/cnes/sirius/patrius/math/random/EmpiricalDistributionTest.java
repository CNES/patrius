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
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.random;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.integration.BaseAbstractUnivariateIntegrator;
import fr.cnes.sirius.patrius.math.analysis.integration.IterativeLegendreGaussIntegrator;
import fr.cnes.sirius.patrius.math.distribution.NormalDistribution;
import fr.cnes.sirius.patrius.math.distribution.RealDistribution;
import fr.cnes.sirius.patrius.math.distribution.RealDistributionAbstractTest;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.stat.descriptive.SummaryStatistics;

/**
 * Test cases for the EmpiricalDistribution class
 * 
 * @version $Id: EmpiricalDistributionTest.java 18108 2017-10-04 06:45:27Z bignon $
 */

public final class EmpiricalDistributionTest extends RealDistributionAbstractTest {

    protected EmpiricalDistribution empiricalDistribution = null;
    protected EmpiricalDistribution empiricalDistribution2 = null;
    protected File file = null;
    protected URL url = null;
    protected double[] dataArray = null;
    protected final int n = 10000;

    @Override
    @Before
    public void setUp() {
        super.setUp();
        this.empiricalDistribution = new EmpiricalDistribution(100);
        // empiricalDistribution = new EmpiricalDistribution(100, new RandomDataImpl()); // XXX Deprecated API
        this.url = this.getClass().getResource("testData.txt");
        final ArrayList<Double> list = new ArrayList<Double>();
        try {
            this.empiricalDistribution2 = new EmpiricalDistribution(100);
            // empiricalDistribution2 = new EmpiricalDistribution(100, new RandomDataImpl()); // XXX Deprecated API
            BufferedReader in =
                new BufferedReader(new InputStreamReader(
                    this.url.openStream()));
            String str = null;
            while ((str = in.readLine()) != null) {
                list.add(Double.valueOf(str));
            }
            in.close();
            in = null;
        } catch (final IOException ex) {
            Assert.fail("IOException " + ex);
        }

        this.dataArray = new double[list.size()];
        int i = 0;
        for (final Double data : list) {
            this.dataArray[i] = data.doubleValue();
            i++;
        }
    }

    /**
     * Test EmpiricalDistrbution.load() using sample data file.<br>
     * Check that the sampleCount, mu and sigma match data in
     * the sample data file. Also verify that load is idempotent.
     */
    @Test
    public void testLoad() throws Exception {
        // Load from a URL
        this.empiricalDistribution.load(this.url);
        this.checkDistribution();

        // Load again from a file (also verifies idempotency of load)
        final File file = new File(this.url.getFile());
        this.empiricalDistribution.load(file);
        this.checkDistribution();
    }

    private void checkDistribution() {
        // testData File has 10000 values, with mean ~ 5.0, std dev ~ 1
        // Make sure that loaded distribution matches this
        Assert.assertEquals(this.empiricalDistribution.getSampleStats().getN(), 1000, 10E-7);
        Assert.assertEquals(this.empiricalDistribution.getSampleStats().getMean(),
            5.069831575018909, 10E-7);
        Assert.assertEquals(this.empiricalDistribution.getSampleStats().getStandardDeviation(),
            1.0173699343977738, 10E-7);
    }

    /**
     * Test EmpiricalDistrbution.load(double[]) using data taken from
     * sample data file.<br>
     * Check that the sampleCount, mu and sigma match data in
     * the sample data file.
     */
    @Test
    public void testDoubleLoad() throws Exception {
        this.empiricalDistribution2.load(this.dataArray);
        // testData File has 10000 values, with mean ~ 5.0, std dev ~ 1
        // Make sure that loaded distribution matches this
        Assert.assertEquals(this.empiricalDistribution2.getSampleStats().getN(), 1000, 10E-7);
        Assert.assertEquals(this.empiricalDistribution2.getSampleStats().getMean(),
            5.069831575018909, 10E-7);
        Assert.assertEquals(this.empiricalDistribution2.getSampleStats().getStandardDeviation(),
            1.0173699343977738, 10E-7);

        final double[] bounds = this.empiricalDistribution2.getGeneratorUpperBounds();
        Assert.assertEquals(bounds.length, 100);
        Assert.assertEquals(bounds[99], 1.0, 10e-12);

    }

    /**
     * Generate 1000 random values and make sure they look OK.<br>
     * Note that there is a non-zero (but very small) probability that
     * these tests will fail even if the code is working as designed.
     */
    @Test
    public void testNext() throws Exception {
        this.tstGen(0.1);
        this.tstDoubleGen(0.1);
    }

    /**
     * Make sure exception thrown if digest getNext is attempted
     * before loading empiricalDistribution.
     */
    @Test
    public void testNexFail() {
        try {
            this.empiricalDistribution.getNextValue();
            this.empiricalDistribution2.getNextValue();
            Assert.fail("Expecting IllegalStateException");
        } catch (final IllegalStateException ex) {
            // expected
        }
    }

    /**
     * Make sure we can handle a grid size that is too fine
     */
    @Test
    public void testGridTooFine() throws Exception {
        this.empiricalDistribution = new EmpiricalDistribution(1001);
        this.tstGen(0.1);
        this.empiricalDistribution2 = new EmpiricalDistribution(1001);
        this.tstDoubleGen(0.1);
    }

    /**
     * How about too fat?
     */
    @Test
    public void testGridTooFat() throws Exception {
        this.empiricalDistribution = new EmpiricalDistribution(1);
        this.tstGen(5); // ridiculous tolerance; but ridiculous grid size
        // really just checking to make sure we do not bomb
        this.empiricalDistribution2 = new EmpiricalDistribution(1);
        this.tstDoubleGen(5);
    }

    /**
     * Test bin index overflow problem (BZ 36450)
     */
    @Test
    public void testBinIndexOverflow() throws Exception {
        final double[] x = new double[] { 9474.94326071674, 2080107.8865462579 };
        new EmpiricalDistribution().load(x);
    }

    @Test
    public void testSerialization() {
        // Empty
        final EmpiricalDistribution dist = new EmpiricalDistribution();
        EmpiricalDistribution dist2 = (EmpiricalDistribution) TestUtils.serializeAndRecover(dist);
        this.verifySame(dist, dist2);

        // Loaded
        this.empiricalDistribution2.load(this.dataArray);
        dist2 = (EmpiricalDistribution) TestUtils.serializeAndRecover(this.empiricalDistribution2);
        this.verifySame(this.empiricalDistribution2, dist2);
    }

    @Test(expected = NullArgumentException.class)
    public void testLoadNullDoubleArray() {
        new EmpiricalDistribution().load((double[]) null);
    }

    @Test(expected = NullArgumentException.class)
    public void testLoadNullURL() throws Exception {
        new EmpiricalDistribution().load((URL) null);
    }

    @Test(expected = NullArgumentException.class)
    public void testLoadNullFile() throws Exception {
        new EmpiricalDistribution().load((File) null);
    }

    /**
     * MATH-298
     */
    @Test
    public void testGetBinUpperBounds() {
        final double[] testData = { 0, 1, 1, 2, 3, 4, 4, 5, 6, 7, 8, 9, 10 };
        final EmpiricalDistribution dist = new EmpiricalDistribution(5);
        dist.load(testData);
        final double[] expectedBinUpperBounds = { 2, 4, 6, 8, 10 };
        final double[] expectedGeneratorUpperBounds = { 4d / 13d, 7d / 13d, 9d / 13d, 11d / 13d, 1 };
        final double tol = 10E-12;
        TestUtils.assertEquals(expectedBinUpperBounds, dist.getUpperBounds(), tol);
        TestUtils.assertEquals(expectedGeneratorUpperBounds, dist.getGeneratorUpperBounds(), tol);
    }

    @Test
    public void testGeneratorConfig() {
        final double[] testData = { 0, 1, 2, 3, 4 };
        final RandomGenerator generator = new RandomAdaptorTest.ConstantGenerator(0.5);

        EmpiricalDistribution dist = new EmpiricalDistribution(5, generator);
        dist.load(testData);
        for (int i = 0; i < 5; i++) {
            Assert.assertEquals(2.0, dist.getNextValue(), 0d);
        }

        // Verify no NPE with null generator argument
        dist = new EmpiricalDistribution(5, (RandomGenerator) null);
        dist.load(testData);
        dist.getNextValue();
    }

    @Test
    public void testReSeed() throws Exception {
        this.empiricalDistribution.load(this.url);
        this.empiricalDistribution.reSeed(100);
        final double[] values = new double[10];
        for (int i = 0; i < 10; i++) {
            values[i] = this.empiricalDistribution.getNextValue();
        }
        this.empiricalDistribution.reSeed(100);
        for (int i = 0; i < 10; i++) {
            Assert.assertEquals(values[i], this.empiricalDistribution.getNextValue(), 0d);
        }
    }

    private void verifySame(final EmpiricalDistribution d1, final EmpiricalDistribution d2) {
        Assert.assertEquals(d1.isLoaded(), d2.isLoaded());
        Assert.assertEquals(d1.getBinCount(), d2.getBinCount());
        Assert.assertEquals(d1.getSampleStats(), d2.getSampleStats());
        if (d1.isLoaded()) {
            for (int i = 0; i < d1.getUpperBounds().length; i++) {
                Assert.assertEquals(d1.getUpperBounds()[i], d2.getUpperBounds()[i], 0);
            }
            Assert.assertEquals(d1.getBinStats(), d2.getBinStats());
        }
    }

    private void tstGen(final double tolerance) throws Exception {
        this.empiricalDistribution.load(this.url);
        this.empiricalDistribution.reSeed(1000);
        final SummaryStatistics stats = new SummaryStatistics();
        for (int i = 1; i < 1000; i++) {
            stats.addValue(this.empiricalDistribution.getNextValue());
        }
        Assert.assertEquals("mean", 5.069831575018909, stats.getMean(), tolerance);
        Assert.assertEquals("std dev", 1.0173699343977738, stats.getStandardDeviation(), tolerance);
    }

    private void tstDoubleGen(final double tolerance) throws Exception {
        this.empiricalDistribution2.load(this.dataArray);
        this.empiricalDistribution2.reSeed(1000);
        final SummaryStatistics stats = new SummaryStatistics();
        for (int i = 1; i < 1000; i++) {
            stats.addValue(this.empiricalDistribution2.getNextValue());
        }
        Assert.assertEquals("mean", 5.069831575018909, stats.getMean(), tolerance);
        Assert.assertEquals("std dev", 1.0173699343977738, stats.getStandardDeviation(), tolerance);
    }

    // Setup for distribution tests

    @Override
    public RealDistribution makeDistribution() {
        // Create a uniform distribution on [0, 10,000]
        final double[] sourceData = new double[this.n + 1];
        for (int i = 0; i < this.n + 1; i++) {
            sourceData[i] = i;
        }
        final EmpiricalDistribution dist = new EmpiricalDistribution();
        dist.load(sourceData);
        return dist;
    }

    /** Uniform bin mass = 10/10001 == mass of all but the first bin */
    private final double binMass = 10d / (this.n + 1);

    /** Mass of first bin = 11/10001 */
    private final double firstBinMass = 11d / (this.n + 1);

    @Override
    public double[] makeCumulativeTestPoints() {
        final double[] testPoints = new double[] { 9, 10, 15, 1000, 5004, 9999 };
        return testPoints;
    }

    @Override
    public double[] makeCumulativeTestValues() {
        /*
         * Bins should be [0, 10], (10, 20], ..., (9990, 10000]
         * Kernels should be N(4.5, 3.02765), N(14.5, 3.02765)...
         * Each bin should have mass 10/10000 = .001
         */
        final double[] testPoints = this.getCumulativeTestPoints();
        final double[] cumValues = new double[testPoints.length];
        final EmpiricalDistribution empiricalDistribution = (EmpiricalDistribution) this.makeDistribution();
        final double[] binBounds = empiricalDistribution.getUpperBounds();
        for (int i = 0; i < testPoints.length; i++) {
            final int bin = this.findBin(testPoints[i]);
            final double lower = bin == 0 ? empiricalDistribution.getSupportLowerBound() :
                binBounds[bin - 1];
            final double upper = binBounds[bin];
            // Compute bMinus = sum or mass of bins below the bin containing the point
            // First bin has mass 11 / 10000, the rest have mass 10 / 10000.
            final double bMinus = bin == 0 ? 0 : (bin - 1) * this.binMass + this.firstBinMass;
            final RealDistribution kernel = this.findKernel(lower, upper);
            final double withinBinKernelMass = kernel.probability(lower, upper);
            final double kernelCum = kernel.probability(lower, testPoints[i]);
            cumValues[i] = bMinus + (bin == 0 ? this.firstBinMass : this.binMass) * kernelCum / withinBinKernelMass;
        }
        return cumValues;
    }

    @Override
    public double[] makeDensityTestValues() {
        final double[] testPoints = this.getCumulativeTestPoints();
        final double[] densityValues = new double[testPoints.length];
        final EmpiricalDistribution empiricalDistribution = (EmpiricalDistribution) this.makeDistribution();
        final double[] binBounds = empiricalDistribution.getUpperBounds();
        for (int i = 0; i < testPoints.length; i++) {
            final int bin = this.findBin(testPoints[i]);
            final double lower = bin == 0 ? empiricalDistribution.getSupportLowerBound() :
                binBounds[bin - 1];
            final double upper = binBounds[bin];
            final RealDistribution kernel = this.findKernel(lower, upper);
            final double withinBinKernelMass = kernel.probability(lower, upper);
            final double density = kernel.density(testPoints[i]);
            densityValues[i] = density * (bin == 0 ? this.firstBinMass : this.binMass) / withinBinKernelMass;
        }
        return densityValues;
    }

    /**
     * Modify test integration bounds from the default. Because the distribution
     * has discontinuities at bin boundaries, integrals spanning multiple bins
     * will face convergence problems. Only test within-bin integrals and spans
     * across no more than 3 bin boundaries.
     */
    @Override
    @Test
    public void testDensityIntegrals() {
        final RealDistribution distribution = this.makeDistribution();
        final double tol = 1.0e-9;
        final BaseAbstractUnivariateIntegrator integrator =
            new IterativeLegendreGaussIntegrator(5, 1.0e-12, 1.0e-10);
        final UnivariateFunction d = new UnivariateFunction(){
            @Override
            public double value(final double x) {
                return distribution.density(x);
            }
        };
        final double[] lower = { 0, 5, 1000, 5001, 9995 };
        final double[] upper = { 5, 12, 1030, 5010, 10000 };
        for (int i = 1; i < 5; i++) {
            Assert.assertEquals(
                distribution.probability(
                    lower[i], upper[i]),
                integrator.integrate(
                    1000000, // Triangle integrals are very slow to converge
                    d, lower[i], upper[i]), tol);
        }
    }

    /**
     * Find the bin that x belongs (relative to {@link #makeDistribution()}).
     */
    private int findBin(final double x) {
        // Number of bins below x should be trunc(x/10)
        final double nMinus = Math.floor(x / 10);
        final int bin = (int) Math.round(nMinus);
        // If x falls on a bin boundary, it is in the lower bin
        return Math.floor(x / 10) == x / 10 ? bin - 1 : bin;
    }

    /**
     * Find the within-bin kernel for the bin with lower bound lower
     * and upper bound upper. All bins other than the first contain 10 points
     * exclusive of the lower bound and are centered at (lower + upper + 1) / 2.
     * The first bin includes its lower bound, 0, so has different mean and
     * standard deviation.
     */
    private RealDistribution findKernel(final double lower, final double upper) {
        if (lower < 1) {
            return new NormalDistribution(5d, 3.3166247903554);
        } else {
            return new NormalDistribution((upper + lower + 1) / 2d, 3.0276503540974917);
        }
    }
}
