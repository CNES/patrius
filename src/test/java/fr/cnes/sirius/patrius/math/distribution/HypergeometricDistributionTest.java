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

import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.exception.NotPositiveException;
import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooLargeException;
import fr.cnes.sirius.patrius.math.util.Precision;

/**
 * Test cases for HyperGeometriclDistribution.
 * Extends IntegerDistributionAbstractTest. See class javadoc for
 * IntegerDistributionAbstractTest for details.
 * 
 * @version $Id: HypergeometricDistributionTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class HypergeometricDistributionTest extends IntegerDistributionAbstractTest {

    // -------------- Implementations for abstract methods -----------------------

    /** Creates the default discrete distribution instance to use in tests. */
    @Override
    public IntegerDistribution makeDistribution() {
        return new HypergeometricDistribution(10, 5, 5);
    }

    /** Creates the default probability density test input values */
    @Override
    public int[] makeDensityTestPoints() {
        return new int[] { -1, 0, 1, 2, 3, 4, 5, 10 };
    }

    /** Creates the default probability density test expected values */
    @Override
    public double[] makeDensityTestValues() {
        return new double[] { 0d, 0.003968d, 0.099206d, 0.396825d, 0.396825d,
            0.099206d, 0.003968d, 0d };
    }

    /** Creates the default cumulative probability density test input values */
    @Override
    public int[] makeCumulativeTestPoints() {
        return this.makeDensityTestPoints();
    }

    /** Creates the default cumulative probability density test expected values */
    @Override
    public double[] makeCumulativeTestValues() {
        return new double[] { 0d, .003968d, .103175d, .50000d, .896825d, .996032d,
            1.00000d, 1d };
    }

    /** Creates the default inverse cumulative probability test input values */
    @Override
    public double[] makeInverseCumulativeTestPoints() {
        return new double[] { 0d, 0.001d, 0.010d, 0.025d, 0.050d, 0.100d, 0.999d,
            0.990d, 0.975d, 0.950d, 0.900d, 1d };
    }

    /** Creates the default inverse cumulative probability density test expected values */
    @Override
    public int[] makeInverseCumulativeTestValues() {
        return new int[] { 0, 0, 1, 1, 1, 1, 5, 4, 4, 4, 4, 5 };
    }

    // -------------------- Additional test cases ------------------------------

    /** Verify that if there are no failures, mass is concentrated on sampleSize */
    @Test
    public void testDegenerateNoFailures() {
        final HypergeometricDistribution dist = new HypergeometricDistribution(5, 5, 3);
        this.setDistribution(dist);
        this.setCumulativeTestPoints(new int[] { -1, 0, 1, 3, 10 });
        this.setCumulativeTestValues(new double[] { 0d, 0d, 0d, 1d, 1d });
        this.setDensityTestPoints(new int[] { -1, 0, 1, 3, 10 });
        this.setDensityTestValues(new double[] { 0d, 0d, 0d, 1d, 0d });
        this.setInverseCumulativeTestPoints(new double[] { 0.1d, 0.5d });
        this.setInverseCumulativeTestValues(new int[] { 3, 3 });
        this.verifyDensities();
        this.verifyCumulativeProbabilities();
        this.verifyInverseCumulativeProbabilities();
        Assert.assertEquals(dist.getSupportLowerBound(), 3);
        Assert.assertEquals(dist.getSupportUpperBound(), 3);
    }

    /** Verify that if there are no successes, mass is concentrated on 0 */
    @Test
    public void testDegenerateNoSuccesses() {
        final HypergeometricDistribution dist = new HypergeometricDistribution(5, 0, 3);
        this.setDistribution(dist);
        this.setCumulativeTestPoints(new int[] { -1, 0, 1, 3, 10 });
        this.setCumulativeTestValues(new double[] { 0d, 1d, 1d, 1d, 1d });
        this.setDensityTestPoints(new int[] { -1, 0, 1, 3, 10 });
        this.setDensityTestValues(new double[] { 0d, 1d, 0d, 0d, 0d });
        this.setInverseCumulativeTestPoints(new double[] { 0.1d, 0.5d });
        this.setInverseCumulativeTestValues(new int[] { 0, 0 });
        this.verifyDensities();
        this.verifyCumulativeProbabilities();
        this.verifyInverseCumulativeProbabilities();
        Assert.assertEquals(dist.getSupportLowerBound(), 0);
        Assert.assertEquals(dist.getSupportUpperBound(), 0);
    }

    /** Verify that if sampleSize = populationSize, mass is concentrated on numberOfSuccesses */
    @Test
    public void testDegenerateFullSample() {
        final HypergeometricDistribution dist = new HypergeometricDistribution(5, 3, 5);
        this.setDistribution(dist);
        this.setCumulativeTestPoints(new int[] { -1, 0, 1, 3, 10 });
        this.setCumulativeTestValues(new double[] { 0d, 0d, 0d, 1d, 1d });
        this.setDensityTestPoints(new int[] { -1, 0, 1, 3, 10 });
        this.setDensityTestValues(new double[] { 0d, 0d, 0d, 1d, 0d });
        this.setInverseCumulativeTestPoints(new double[] { 0.1d, 0.5d });
        this.setInverseCumulativeTestValues(new int[] { 3, 3 });
        this.verifyDensities();
        this.verifyCumulativeProbabilities();
        this.verifyInverseCumulativeProbabilities();
        Assert.assertEquals(dist.getSupportLowerBound(), 3);
        Assert.assertEquals(dist.getSupportUpperBound(), 3);
    }

    @Test
    public void testPreconditions() {
        try {
            new HypergeometricDistribution(0, 3, 5);
            Assert.fail("negative population size. NotStrictlyPositiveException expected");
        } catch (final NotStrictlyPositiveException ex) {
            // Expected.
        }
        try {
            new HypergeometricDistribution(5, -1, 5);
            Assert.fail("negative number of successes. NotPositiveException expected");
        } catch (final NotPositiveException ex) {
            // Expected.
        }
        try {
            new HypergeometricDistribution(5, 3, -1);
            Assert.fail("negative sample size. NotPositiveException expected");
        } catch (final NotPositiveException ex) {
            // Expected.
        }
        try {
            new HypergeometricDistribution(5, 6, 5);
            Assert.fail("numberOfSuccesses > populationSize. NumberIsTooLargeException expected");
        } catch (final NumberIsTooLargeException ex) {
            // Expected.
        }
        try {
            new HypergeometricDistribution(5, 3, 6);
            Assert.fail("sampleSize > populationSize. NumberIsTooLargeException expected");
        } catch (final NumberIsTooLargeException ex) {
            // Expected.
        }
    }

    @Test
    public void testAccessors() {
        final HypergeometricDistribution dist = new HypergeometricDistribution(5, 3, 4);
        Assert.assertEquals(5, dist.getPopulationSize());
        Assert.assertEquals(3, dist.getNumberOfSuccesses());
        Assert.assertEquals(4, dist.getSampleSize());
    }

    @Test
    public void testLargeValues() {
        final int populationSize = 3456;
        final int sampleSize = 789;
        final int numberOfSucceses = 101;
        final double[][] data = {
            { 0.0, 2.75646034603961e-12, 2.75646034603961e-12, 1.0 },
            { 1.0, 8.55705370142386e-11, 8.83269973602783e-11, 0.999999999997244 },
            { 2.0, 1.31288129219665e-9, 1.40120828955693e-9, 0.999999999911673 },
            { 3.0, 1.32724172984193e-8, 1.46736255879763e-8, 0.999999998598792 },
            { 4.0, 9.94501711734089e-8, 1.14123796761385e-7, 0.999999985326375 },
            { 5.0, 5.89080768883643e-7, 7.03204565645028e-7, 0.999999885876203 },
            { 20.0, 0.0760051397707708, 0.27349758476299, 0.802507555007781 },
            { 21.0, 0.087144222047629, 0.360641806810619, 0.72650241523701 },
            { 22.0, 0.0940378846881819, 0.454679691498801, 0.639358193189381 },
            { 23.0, 0.0956897500614809, 0.550369441560282, 0.545320308501199 },
            { 24.0, 0.0919766921922999, 0.642346133752582, 0.449630558439718 },
            { 25.0, 0.083641637261095, 0.725987771013677, 0.357653866247418 },
            { 96.0, 5.93849188852098e-57, 1.0, 6.01900244560712e-57 },
            { 97.0, 7.96593036832547e-59, 1.0, 8.05105570861321e-59 },
            { 98.0, 8.44582921934367e-61, 1.0, 8.5125340287733e-61 },
            { 99.0, 6.63604297068222e-63, 1.0, 6.670480942963e-63 },
            { 100.0, 3.43501099007557e-65, 1.0, 3.4437972280786e-65 },
            { 101.0, 8.78623800302957e-68, 1.0, 8.78623800302957e-68 },
        };

        this.testHypergeometricDistributionProbabilities(populationSize, sampleSize, numberOfSucceses, data);
    }

    private void testHypergeometricDistributionProbabilities(final int populationSize, final int sampleSize,
                                                             final int numberOfSucceses,
                                                             final double[][] data) {
        final HypergeometricDistribution dist =
            new HypergeometricDistribution(populationSize, numberOfSucceses, sampleSize);
        for (final double[] element : data) {
            final int x = (int) element[0];
            final double pmf = element[1];
            final double actualPmf = dist.probability(x);
            TestUtils.assertRelativelyEquals("Expected equals for <" + x + "> pmf", pmf, actualPmf, 1.0e-9);

            final double cdf = element[2];
            final double actualCdf = dist.cumulativeProbability(x);
            TestUtils.assertRelativelyEquals("Expected equals for <" + x + "> cdf", cdf, actualCdf, 1.0e-9);

            final double cdf1 = element[3];
            final double actualCdf1 = dist.upperCumulativeProbability(x);
            TestUtils.assertRelativelyEquals("Expected equals for <" + x + "> cdf1", cdf1, actualCdf1, 1.0e-9);
        }
    }

    @Test
    public void testMoreLargeValues() {
        final int populationSize = 26896;
        final int sampleSize = 895;
        final int numberOfSucceses = 55;
        final double[][] data = {
            { 0.0, 0.155168304750504, 0.155168304750504, 1.0 },
            { 1.0, 0.29437545000746, 0.449543754757964, 0.844831695249496 },
            { 2.0, 0.273841321577003, 0.723385076334967, 0.550456245242036 },
            { 3.0, 0.166488572570786, 0.889873648905753, 0.276614923665033 },
            { 4.0, 0.0743969744713231, 0.964270623377076, 0.110126351094247 },
            { 5.0, 0.0260542785784855, 0.990324901955562, 0.0357293766229237 },
            { 20.0, 3.57101101678792e-16, 1.0, 3.78252101622096e-16 },
            { 21.0, 2.00551638598312e-17, 1.0, 2.11509999433041e-17 },
            { 22.0, 1.04317070180562e-18, 1.0, 1.09583608347287e-18 },
            { 23.0, 5.03153504903308e-20, 1.0, 5.266538166725e-20 },
            { 24.0, 2.2525984149695e-21, 1.0, 2.35003117691919e-21 },
            { 25.0, 9.3677424515947e-23, 1.0, 9.74327619496943e-23 },
            { 50.0, 9.83633962945521e-69, 1.0, 9.8677629437617e-69 },
            { 51.0, 3.13448949497553e-71, 1.0, 3.14233143064882e-71 },
            { 52.0, 7.82755221928122e-74, 1.0, 7.84193567329055e-74 },
            { 53.0, 1.43662126065532e-76, 1.0, 1.43834540093295e-76 },
            { 54.0, 1.72312692517348e-79, 1.0, 1.7241402776278e-79 },
            { 55.0, 1.01335245432581e-82, 1.0, 1.01335245432581e-82 },
        };
        this.testHypergeometricDistributionProbabilities(populationSize, sampleSize, numberOfSucceses, data);
    }

    @Test
    public void testMoments() {
        final double tol = 1e-9;
        HypergeometricDistribution dist;

        dist = new HypergeometricDistribution(1500, 40, 100);
        Assert.assertEquals(dist.getNumericalMean(), 40d * 100d / 1500d, tol);
        Assert.assertEquals(dist.getNumericalVariance(), (100d * 40d * (1500d - 100d) * (1500d - 40d))
            / ((1500d * 1500d * 1499d)), tol);

        dist = new HypergeometricDistribution(3000, 55, 200);
        Assert.assertEquals(dist.getNumericalMean(), 55d * 200d / 3000d, tol);
        Assert.assertEquals(dist.getNumericalVariance(), (200d * 55d * (3000d - 200d) * (3000d - 55d))
            / ((3000d * 3000d * 2999d)), tol);
    }

    @Test
    public void testMath644() {
        final int N = 14761461; // population
        final int m = 1035; // successes in population
        final int n = 1841; // number of trials

        final int k = 0;
        final HypergeometricDistribution dist = new HypergeometricDistribution(N, m, n);

        Assert.assertTrue(Precision.compareTo(1.0, dist.upperCumulativeProbability(k), 1) == 0);
        Assert.assertTrue(Precision.compareTo(dist.cumulativeProbability(k), 0.0, 1) > 0);

        // another way to calculate the upper cumulative probability
        final double upper = 1.0 - dist.cumulativeProbability(k) + dist.probability(k);
        Assert.assertTrue(Precision.compareTo(1.0, upper, 1) == 0);
    }
}
