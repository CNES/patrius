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
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.math.distribution;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.math.special.Gamma;
import fr.cnes.sirius.patrius.math.stat.descriptive.SummaryStatistics;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Test cases for GammaDistribution.
 * Extends ContinuousDistributionAbstractTest. See class javadoc for
 * ContinuousDistributionAbstractTest for details.
 * 
 * @version $Id: GammaDistributionTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class GammaDistributionTest extends RealDistributionAbstractTest {

    // -------------- Implementations for abstract methods -----------------------

    /** Creates the default continuous distribution instance to use in tests. */
    @Override
    public GammaDistribution makeDistribution() {
        return new GammaDistribution(4d, 2d);
    }

    /** Creates the default cumulative probability distribution test input values */
    @Override
    public double[] makeCumulativeTestPoints() {
        // quantiles computed using R version 2.9.2
        return new double[] { 0.857104827257, 1.64649737269, 2.17973074725, 2.7326367935, 3.48953912565,
            26.1244815584, 20.0902350297, 17.5345461395, 15.5073130559, 13.3615661365 };
    }

    /** Creates the default cumulative probability density test expected values */
    @Override
    public double[] makeCumulativeTestValues() {
        return new double[] { 0.001, 0.01, 0.025, 0.05, 0.1, 0.999, 0.990, 0.975, 0.950, 0.900 };
    }

    /** Creates the default probability density test expected values */
    @Override
    public double[] makeDensityTestValues() {
        return new double[] { 0.00427280075546, 0.0204117166709, 0.0362756163658, 0.0542113174239, 0.0773195272491,
            0.000394468852816, 0.00366559696761, 0.00874649473311, 0.0166712508128, 0.0311798227954 };
    }

    // --------------------- Override tolerance --------------
    @Override
    public void setUp() {
        super.setUp();
        this.setTolerance(1e-9);
    }

    // ---------------------------- Additional test cases -------------------------

    @Test
    public void testPreconditions() {
        try {
            new GammaDistribution(0, 1);
            Assert.fail("Expecting NotStrictlyPositiveException for alpha = 0");
        } catch (final NotStrictlyPositiveException ex) {
            // Expected.
        }
        try {
            new GammaDistribution(1, 0);
            Assert.fail("Expecting NotStrictlyPositiveException for alpha = 0");
        } catch (final NotStrictlyPositiveException ex) {
            // Expected.
        }
    }

    @Test
    public void testProbabilities() {
        this.testProbability(-1.000, 4.0, 2.0, .0000);
        this.testProbability(15.501, 4.0, 2.0, .9499);
        this.testProbability(0.504, 4.0, 1.0, .0018);
        this.testProbability(10.011, 1.0, 2.0, .9933);
        this.testProbability(5.000, 2.0, 2.0, .7127);
    }

    @Test
    public void testValues() {
        this.testValue(15.501, 4.0, 2.0, .9499);
        this.testValue(0.504, 4.0, 1.0, .0018);
        this.testValue(10.011, 1.0, 2.0, .9933);
        this.testValue(5.000, 2.0, 2.0, .7127);
    }

    private void testProbability(final double x, final double a, final double b, final double expected) {
        final GammaDistribution distribution = new GammaDistribution(a, b);
        final double actual = distribution.cumulativeProbability(x);
        Assert.assertEquals("probability for " + x, expected, actual, 10e-4);
    }

    private void testValue(final double expected, final double a, final double b, final double p) {
        final GammaDistribution distribution = new GammaDistribution(a, b);
        final double actual = distribution.inverseCumulativeProbability(p);
        Assert.assertEquals("critical value for " + p, expected, actual, 10e-4);
    }

    @Test
    public void testDensity() {
        final double[] x = new double[] { -0.1, 1e-6, 0.5, 1, 2, 5 };
        // R2.5: print(dgamma(x, shape=1, rate=1), digits=10)
        this.checkDensity(1, 1, x, new double[] { 0.000000000000, 0.999999000001, 0.606530659713, 0.367879441171,
            0.135335283237, 0.006737946999 });
        // R2.5: print(dgamma(x, shape=2, rate=1), digits=10)
        this.checkDensity(2, 1, x, new double[] { 0.000000000000, 0.000000999999, 0.303265329856, 0.367879441171,
            0.270670566473, 0.033689734995 });
        // R2.5: print(dgamma(x, shape=4, rate=1), digits=10)
        this.checkDensity(4, 1, x, new double[] { 0.000000000e+00, 1.666665000e-19, 1.263605541e-02, 6.131324020e-02,
            1.804470443e-01, 1.403738958e-01 });
        // R2.5: print(dgamma(x, shape=4, rate=10), digits=10)
        this.checkDensity(4, 10, x, new double[] { 0.000000000e+00, 1.666650000e-15, 1.403738958e+00, 7.566654960e-02,
            2.748204830e-05, 4.018228850e-17 });
        // R2.5: print(dgamma(x, shape=.1, rate=10), digits=10)
        this.checkDensity(0.1, 10, x, new double[] { 0.000000000e+00, 3.323953832e+04, 1.663849010e-03,
            6.007786726e-06,
            1.461647647e-10, 5.996008322e-24 });
        // R2.5: print(dgamma(x, shape=.1, rate=20), digits=10)
        this.checkDensity(0.1, 20, x, new double[] { 0.000000000e+00, 3.562489883e+04, 1.201557345e-05,
            2.923295295e-10,
            3.228910843e-19, 1.239484589e-45 });
        // R2.5: print(dgamma(x, shape=.1, rate=4), digits=10)
        this.checkDensity(0.1, 4, x, new double[] { 0.000000000e+00, 3.032938388e+04, 3.049322494e-02, 2.211502311e-03,
            2.170613371e-05, 5.846590589e-11 });
        // R2.5: print(dgamma(x, shape=.1, rate=1), digits=10)
        this.checkDensity(0.1, 1, x, new double[] { 0.000000000e+00, 2.640334143e+04, 1.189704437e-01, 3.866916944e-02,
            7.623306235e-03, 1.663849010e-04 });
    }

    private void checkDensity(final double alpha, final double rate, final double[] x, final double[] expected) {
        final GammaDistribution d = new GammaDistribution(alpha, 1 / rate);
        for (int i = 0; i < x.length; i++) {
            Assert.assertEquals(expected[i], d.density(x[i]), 1e-5);
        }
    }

    @Test
    public void testInverseCumulativeProbabilityExtremes() {
        this.setInverseCumulativeTestPoints(new double[] { 0, 1 });
        this.setInverseCumulativeTestValues(new double[] { 0, Double.POSITIVE_INFINITY });
        this.verifyInverseCumulativeProbabilities();
    }

    @Test
    public void testMoments() {
        final double tol = 1e-9;
        GammaDistribution dist;

        dist = new GammaDistribution(1, 2);
        Assert.assertEquals(dist.getNumericalMean(), 2, tol);
        Assert.assertEquals(dist.getNumericalVariance(), 4, tol);

        dist = new GammaDistribution(1.1, 4.2);
        Assert.assertEquals(dist.getNumericalMean(), 1.1d * 4.2d, tol);
        Assert.assertEquals(dist.getNumericalVariance(), 1.1d * 4.2d * 4.2d, tol);
    }

    private static final double HALF_LOG_2_PI = 0.5 * MathLib.log(2.0 * FastMath.PI);

    public static double logGamma(final double x) {
        /*
         * This is a copy of
         * double Gamma.logGamma(double)
         * prior to MATH-849
         */
        double ret;

        if (Double.isNaN(x) || (x <= 0.0)) {
            ret = Double.NaN;
        } else {
            final double sum = Gamma.lanczos(x);
            final double tmp = x + Gamma.LANCZOS_G + .5;
            ret = ((x + .5) * MathLib.log(tmp)) - tmp +
                HALF_LOG_2_PI + MathLib.log(sum / x);
        }

        return ret;
    }

    public static double density(final double x, final double shape,
                                 final double scale) {
        /*
         * This is a copy of
         * double GammaDistribution.density(double)
         * prior to MATH-753.
         */
        if (x < 0) {
            return 0;
        }
        return MathLib.pow(x / scale, shape - 1) / scale *
            MathLib.exp(-x / scale) / MathLib.exp(logGamma(shape));
    }

    /*
     * MATH-753: large values of x or shape parameter cause density(double) to
     * overflow. Reference data is generated with the Maxima script
     * gamma-distribution.mac, which can be found in
     * src/test/resources/org/apache/commons/math3/distribution.
     */

    private void doTestMath753(final double shape,
                               final double meanNoOF, final double sdNoOF,
                               final double meanOF, final double sdOF,
                               final String resourceName) throws IOException {
        final GammaDistribution distribution = new GammaDistribution(shape, 1.0);
        final SummaryStatistics statOld = new SummaryStatistics();
        final SummaryStatistics statNewNoOF = new SummaryStatistics();
        final SummaryStatistics statNewOF = new SummaryStatistics();

        final InputStream resourceAsStream;
        resourceAsStream = this.getClass().getResourceAsStream(resourceName);
        Assert.assertNotNull("Could not find resource " + resourceName,
            resourceAsStream);
        final BufferedReader in;
        in = new BufferedReader(new InputStreamReader(resourceAsStream));

        try {
            for (String line = in.readLine(); line != null; line = in
                .readLine()) {
                final String[] tokens = line.split(", ");
                Assert.assertTrue("expected two floating-point values",
                    tokens.length == 2);
                final double x = Double.parseDouble(tokens[0]);
                final String msg = "x = " + x + ", shape = " + shape +
                    ", scale = 1.0";
                final double expected = Double.parseDouble(tokens[1]);
                final double ulp = MathLib.ulp(expected);
                final double actualOld = density(x, shape, 1.0);
                final double actualNew = distribution.density(x);
                double errOld, errNew;
                try {
                    errOld = MathLib.abs((actualOld - expected) / ulp);
                } catch (final ArithmeticException e) {
                    errOld = Double.NaN;
                }
                try {
                    errNew = MathLib.abs((actualNew - expected) / ulp);
                } catch (final ArithmeticException e) {
                    errNew = Double.NaN;
                }

                if (Double.isNaN(actualOld) || Double.isInfinite(actualOld)) {
                    Assert.assertFalse(msg, Double.isNaN(actualNew));
                    Assert.assertFalse(msg, Double.isInfinite(actualNew));
                    statNewOF.addValue(errNew);
                } else {
                    statOld.addValue(errOld);
                    statNewNoOF.addValue(errNew);
                }
            }
            if (statOld.getN() != 0) {
                /*
                 * If no overflow occurs, check that new implementation is
                 * better than old one.
                 */
                final StringBuilder sb = new StringBuilder("shape = ");
                sb.append(shape);
                sb.append(", scale = 1.0\n");
                sb.append("Old implementation\n");
                sb.append("------------------\n");
                sb.append(statOld.toString());
                sb.append("New implementation\n");
                sb.append("------------------\n");
                sb.append(statNewNoOF.toString());
                final String msg = sb.toString();

                final double oldMin = statOld.getMin();
                final double newMin = statNewNoOF.getMin();
                Assert.assertTrue(msg, newMin <= oldMin);

                final double oldMax = statOld.getMax();
                final double newMax = statNewNoOF.getMax();
                Assert.assertTrue(msg, newMax <= oldMax);

                final double oldMean = statOld.getMean();
                final double newMean = statNewNoOF.getMean();
                Assert.assertTrue(msg, newMean <= oldMean);

                final double oldSd = statOld.getStandardDeviation();
                final double newSd = statNewNoOF.getStandardDeviation();
                Assert.assertTrue(msg, newSd <= oldSd);

                Assert.assertTrue(msg, newMean <= meanNoOF);
                Assert.assertTrue(msg, newSd <= sdNoOF);
            }
            if (statNewOF.getN() != 0) {
                final double newMean = statNewOF.getMean();
                final double newSd = statNewOF.getStandardDeviation();

                final StringBuilder sb = new StringBuilder("shape = ");
                sb.append(shape);
                sb.append(", scale = 1.0");
                sb.append(", max. mean error (ulps) = ");
                sb.append(meanOF);
                sb.append(", actual mean error (ulps) = ");
                sb.append(newMean);
                sb.append(", max. sd of error (ulps) = ");
                sb.append(sdOF);
                sb.append(", actual sd of error (ulps) = ");
                sb.append(newSd);
                final String msg = sb.toString();

                Assert.assertTrue(msg, newMean <= meanOF);
                Assert.assertTrue(msg, newSd <= sdOF);
            }
        } catch (final IOException e) {
            Assert.fail(e.getMessage());
        } finally {
            in.close();
        }
    }

    @Test
    public void testMath753Shape1() throws IOException {
        this.doTestMath753(1.0, 1.5, 0.5, 0.0, 0.0, "gamma-distribution-shape-1.csv");
    }

    @Test
    public void testMath753Shape8() throws IOException {
        this.doTestMath753(8.0, 1.5, 1.0, 0.0, 0.0, "gamma-distribution-shape-8.csv");
    }

    @Test
    public void testMath753Shape10() throws IOException {
        this.doTestMath753(10.0, 1.0, 1.0, 0.0, 0.0, "gamma-distribution-shape-10.csv");
    }

    @Test
    public void testMath753Shape100() throws IOException {
        this.doTestMath753(100.0, 1.5, 1.0, 0.0, 0.0, "gamma-distribution-shape-100.csv");
    }

    @Test
    public void testMath753Shape142() throws IOException {
        this.doTestMath753(142.0, 0.5, 1.5, 40.0, 40.0, "gamma-distribution-shape-142.csv");
    }

    @Test
    public void testMath753Shape1000() throws IOException {
        this.doTestMath753(1000.0, 1.0, 1.0, 160.0, 220.0, "gamma-distribution-shape-1000.csv");
    }
}
