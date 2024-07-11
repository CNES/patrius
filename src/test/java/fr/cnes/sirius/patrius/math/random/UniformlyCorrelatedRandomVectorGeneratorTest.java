/**
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
 * @history creation 11/03/2015
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:291:11/03/2015: add uniformly correlated random vector generation
 * VERSION::DM:596:12/04/2016:Improve test coherence
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.random;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.linear.BlockRealMatrix;
import fr.cnes.sirius.patrius.math.linear.NonSymmetricMatrixException;
import fr.cnes.sirius.patrius.math.stat.StatUtils;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * @description <p>
 *              This file tests the class UniformlyCorrelatedRandomVectorGenerator.
 *              </p>
 * 
 * @author Emmanuel Bignon
 * 
 * @version $Id: UniformlyCorrelatedRandomVectorGeneratorTest.java 17909 2017-09-11 11:57:36Z bignon $
 * 
 * @since 3.0
 * 
 */
public class UniformlyCorrelatedRandomVectorGeneratorTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle sample generation
         * 
         * @featureDescription Generate samples based on a covariance matrix.
         * 
         * @coveredRequirements
         */
        SAMPLE_GENERATION
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#SAMPLE_GENERATION}
     * 
     * @testedMethod {@link UniformlyCorrelatedRandomVectorGenerator#nextVector()}
     * 
     * @description Generates 100000 samples based on a covariance matrix.
     * 
     * @input covariance matrix
     * 
     * @output mean and delta
     * 
     * @testPassCriteria mean and delta are as expected according to statistical theory (relative tolerance: 1E-4)
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testSampleGeneration() {

        // Initialization
        final double[][] covMatrix = {
            { 1.0842419885089E+3, 1.1406404982467E-2, 2.0855831468343E-3, -4.0669198672901E-1, -5.5803555872956E-2,
                -2.2376939343087E-2 },
            { 1.1406404982467E-2, 1.2148278051844E-7, 3.7552376808274E-8, -3.3451285777446E-6, -7.8847283744831E-7,
                -2.8875365165953E-7 },
            { 2.0855831468343E-3, 3.7552376808274E-8, 1.3771726609000E-4, 1.4108586890610E-3, -1.4146039781309E-3,
                -5.8837646814215E-7 },
            { -4.0669198672901E-1, -3.3451285777446E-6, 1.4108586890610E-3, 1.7385503686810E-2,
                -1.6854802919373E-2, -2.7926478195823E-5 },
            { -5.5803555872956E-2, -7.8847283744831E-7, -1.4146039781309E-3, -1.6854802919373E-2,
                1.6912872450250E-2, 6.8562077086609E-6 },
            { -2.2376939343087E-2, -2.8875365165953E-7, -5.8837646814215E-7, -2.7926478195823E-5,
                6.8562077086609E-6, 3.1373017650490E-6 } };
        final double[] mean = { 24238 * 1000, 0.73, 1.2, 120, 89, 18 };
        final UniformlyCorrelatedRandomVectorGenerator generator = new UniformlyCorrelatedRandomVectorGenerator(
            mean, new BlockRealMatrix(covMatrix), 1E-15, new GaussianRandomGenerator(new JDKRandomGenerator()));

        // Random pv generation
        final int n = 100000;
        final double[] a = new double[n];
        final double[] e = new double[n];
        final double[] i = new double[n];
        final double[] pom = new double[n];
        final double[] gom = new double[n];
        final double[] m = new double[n];
        for (int j = 0; j < n; j++) {
            final double[] res = generator.nextVector();
            a[j] = res[0];
            e[j] = res[1];
            i[j] = res[2];
            pom[j] = res[3];
            gom[j] = res[4];
            m[j] = res[5];
        }

        // Comparison
        final double uniformConst = MathLib.sqrt(3);
        Assert.assertEquals(this.relDiff(mean[0], StatUtils.mean(a)), 0, 1E-4);
        Assert.assertEquals(this.relDiff(mean[1], StatUtils.mean(e)), 0, 1E-4);
        Assert.assertEquals(this.relDiff(mean[2], StatUtils.mean(i)), 0, 1E-4);
        Assert.assertEquals(this.relDiff(mean[3], StatUtils.mean(pom)), 0, 1E-4);
        Assert.assertEquals(this.relDiff(mean[4], StatUtils.mean(gom)), 0, 1E-4);
        Assert.assertEquals(this.relDiff(mean[5], StatUtils.mean(m)), 0, 1E-4);

        Assert.assertEquals(
            this.relDiff(uniformConst * MathLib.sqrt(covMatrix[0][0]), (StatUtils.max(a) - StatUtils.min(a)) / 2.), 0.,
            1E-4);
        Assert.assertEquals(
            this.relDiff(uniformConst * MathLib.sqrt(covMatrix[1][1]), (StatUtils.max(e) - StatUtils.min(e)) / 2.), 0.,
            1E-4);
        Assert.assertEquals(
            this.relDiff(uniformConst * MathLib.sqrt(covMatrix[2][2]), (StatUtils.max(i) - StatUtils.min(i)) / 2.), 0.,
            1E-4);
        Assert.assertEquals(
            this.relDiff(uniformConst * MathLib.sqrt(covMatrix[3][3]), (StatUtils.max(pom) - StatUtils.min(pom)) / 2.),
            0., 1E-4);
        Assert.assertEquals(
            this.relDiff(uniformConst * MathLib.sqrt(covMatrix[4][4]), (StatUtils.max(gom) - StatUtils.min(gom)) / 2.),
            0., 1E-4);
        Assert.assertEquals(
            this.relDiff(uniformConst * MathLib.sqrt(covMatrix[5][5]), (StatUtils.max(m) - StatUtils.min(m)) / 2.), 0.,
            1E-4);

        // Check rank
        Assert.assertEquals(generator.getRank(), 6);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#SAMPLE_GENERATION}
     * 
     * @testedMethod {@link UniformlyCorrelatedRandomVectorGenerator#nextVector()}
     * 
     * @description Generates 100000 samples based on a covariance matrix needing Spearman correction.
     * 
     * @input covariance matrix
     * 
     * @output mean and delta
     * 
     * @testPassCriteria mean and delta are as expected according to statistical theory (relative tolerance: 1E-4)
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testSampleGenerationSpearman() {

        // Initialization
        final double[][] covMatrix = {
            { 361.4139961696334, 0.0019127984989923334, 0.06440299389983334, 0.7236115649338334,
                0.7137079712641667, 0.009720531398781668 },
            { 0.0019127984989923334, 4.049426017281333E-8, 6.817110093566667E-7, 7.659488176596667E-6,
                7.554657819683334E-6, 1.0289262765723336E-7 },
            { 0.06440299389983334, 6.817110093566667E-7, 4.590575536333334E-5, 2.578912366216667E-4,
                2.543616495583334E-4, 3.464344662983334E-6 },
            { 0.7236115649338334, 7.659488176596667E-6, 2.578912366216667E-4, 0.005795167895603334,
                0.0028579266296583335, 3.892427527438334E-5 },
            { 0.7137079712641667, 7.554657819683334E-6, 2.543616495583334E-4, 0.0028579266296583335,
                0.005637624150083334, 3.839154442141667E-5 },
            { 0.009720531398781668, 1.0289262765723336E-7, 3.464344662983334E-6, 3.892427527438334E-5,
                3.839154442141667E-5, 1.0457672550163337E-6 }
        };
        final double[] mean = { 24238 * 1000, 0.73, 1.2, 120, 89, 18 };
        final UniformlyCorrelatedRandomVectorGenerator generator = new UniformlyCorrelatedRandomVectorGenerator(
            mean, new BlockRealMatrix(covMatrix), 1E-15, new GaussianRandomGenerator(new JDKRandomGenerator()));

        // Random pv generation
        final int n = 100000;
        final double[] a = new double[n];
        final double[] e = new double[n];
        final double[] i = new double[n];
        final double[] pom = new double[n];
        final double[] gom = new double[n];
        final double[] m = new double[n];
        for (int j = 0; j < n; j++) {
            final double[] res = generator.nextVector();
            a[j] = res[0];
            e[j] = res[1];
            i[j] = res[2];
            pom[j] = res[3];
            gom[j] = res[4];
            m[j] = res[5];
        }

        // Comparison
        final double uniformConst = MathLib.sqrt(3);
        Assert.assertEquals(this.relDiff(mean[0], StatUtils.mean(a)), 0, 1E-4);
        Assert.assertEquals(this.relDiff(mean[1], StatUtils.mean(e)), 0, 1E-4);
        Assert.assertEquals(this.relDiff(mean[2], StatUtils.mean(i)), 0, 1E-4);
        Assert.assertEquals(this.relDiff(mean[3], StatUtils.mean(pom)), 0, 1E-4);
        Assert.assertEquals(this.relDiff(mean[4], StatUtils.mean(gom)), 0, 1E-4);
        Assert.assertEquals(this.relDiff(mean[5], StatUtils.mean(m)), 0, 1E-4);

        Assert.assertEquals(
            this.relDiff(uniformConst * MathLib.sqrt(covMatrix[0][0]), (StatUtils.max(a) - StatUtils.min(a)) / 2.), 0.,
            1E-4);
        Assert.assertEquals(
            this.relDiff(uniformConst * MathLib.sqrt(covMatrix[1][1]), (StatUtils.max(e) - StatUtils.min(e)) / 2.), 0.,
            1E-4);
        Assert.assertEquals(
            this.relDiff(uniformConst * MathLib.sqrt(covMatrix[2][2]), (StatUtils.max(i) - StatUtils.min(i)) / 2.), 0.,
            1E-4);
        Assert.assertEquals(
            this.relDiff(uniformConst * MathLib.sqrt(covMatrix[3][3]), (StatUtils.max(pom) - StatUtils.min(pom)) / 2.),
            0., 1E-4);
        Assert.assertEquals(
            this.relDiff(uniformConst * MathLib.sqrt(covMatrix[4][4]), (StatUtils.max(gom) - StatUtils.min(gom)) / 2.),
            0., 1E-4);
        Assert.assertEquals(
            this.relDiff(uniformConst * MathLib.sqrt(covMatrix[5][5]), (StatUtils.max(m) - StatUtils.min(m)) / 2.), 0.,
            1E-4);

        // Check rank
        Assert.assertEquals(generator.getRank(), 6);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#SAMPLE_GENERATION}
     * 
     * @testedMethod {@link UniformlyCorrelatedRandomVectorGenerator#nextVector()}
     * 
     * @description Generates 100000 samples based on a covariance matrix with only 4 dispersed parameters.
     * 
     * @input covariance matrix with only 4 dispersed parameters
     * 
     * @output mean and delta
     * 
     * @testPassCriteria mean and delta are as expected according to statistical theory (relative tolerance: 1E-4)
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testSampleGeneration0() {

        // Initialization
        final double[][] covMatrix = {
            { 1.0842419885089E+3, 0, 0, -4.0669198672901E-1, -5.5803555872956E-2, -2.2376939343087E-2 },
            { 0, 0, 0, 0, 0, 0 },
            { 0, 0, 0, 0, 0, 0 },
            { -4.0669198672901E-1, 0, 0, 1.7385503686810E-2, -1.6854802919373E-2, -2.7926478195823E-5 },
            { -5.5803555872956E-2, 0, 0, -1.6854802919373E-2, 1.6912872450250E-2, 6.8562077086609E-6 },
            { -2.2376939343087E-2, 0, 0, -2.7926478195823E-5, 6.8562077086609E-6, 3.1373017650490E-6 } };
        final double[] mean = { 24238 * 1000, 0.73, 1.2, 120, 89, 18 };
        final UniformlyCorrelatedRandomVectorGenerator generator = new UniformlyCorrelatedRandomVectorGenerator(
            mean, new BlockRealMatrix(covMatrix), 1E-15, new GaussianRandomGenerator(new JDKRandomGenerator()));

        // Random pv generation
        final int n = 100000;
        final double[] a = new double[n];
        final double[] e = new double[n];
        final double[] i = new double[n];
        final double[] pom = new double[n];
        final double[] gom = new double[n];
        final double[] m = new double[n];
        for (int j = 0; j < n; j++) {
            final double[] res = generator.nextVector();
            a[j] = res[0];
            e[j] = res[1];
            i[j] = res[2];
            pom[j] = res[3];
            gom[j] = res[4];
            m[j] = res[5];
        }

        // Comparison
        final double eps = 1E-4;
        final double uniformConst = MathLib.sqrt(3);
        Assert.assertEquals(this.relDiff(mean[0], StatUtils.mean(a)), 0, eps);
        Assert.assertEquals(mean[1], StatUtils.mean(e), 0);
        Assert.assertEquals(mean[2], StatUtils.mean(i), 0);
        Assert.assertEquals(this.relDiff(mean[3], StatUtils.mean(pom)), 0, eps);
        Assert.assertEquals(this.relDiff(mean[4], StatUtils.mean(gom)), 0, eps);
        Assert.assertEquals(this.relDiff(mean[5], StatUtils.mean(m)), 0, eps);

        Assert.assertEquals(
            this.relDiff(uniformConst * MathLib.sqrt(covMatrix[0][0]), (StatUtils.max(a) - StatUtils.min(a)) / 2.), 0.,
            eps);
        Assert.assertEquals(0, (StatUtils.max(e) - StatUtils.min(e)) / 2., 0);
        Assert.assertEquals(0, (StatUtils.max(i) - StatUtils.min(i)) / 2., 0);
        Assert.assertEquals(
            this.relDiff(uniformConst * MathLib.sqrt(covMatrix[3][3]), (StatUtils.max(pom) - StatUtils.min(pom)) / 2.),
            0., eps);
        Assert.assertEquals(
            this.relDiff(uniformConst * MathLib.sqrt(covMatrix[4][4]), (StatUtils.max(gom) - StatUtils.min(gom)) / 2.),
            0., eps);
        Assert.assertEquals(
            this.relDiff(uniformConst * MathLib.sqrt(covMatrix[5][5]), (StatUtils.max(m) - StatUtils.min(m)) / 2.), 0.,
            eps);

        // Check rank
        Assert.assertEquals(generator.getRank(), 4);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @description Tests wrong covariance matrices.
     * 
     * @input matrix which is not a covariance matrix
     * 
     * @output exception
     * 
     * @testPassCriteria exceptions are thrown as expected
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testException() {

        // Non-symmetric covariance matrix
        final double[][] covMatrix = {
            { 1.0842419885089E+3, 0, 0, -4.0669198672901E-1, -5.5803555872956E-2, -2.2376939343087E-2 },
            { 0, 0, 0, 0, 0, 0 },
            { 0, 0, 0, 0, 0, 0 },
            { -3.0669198672901E-1, 0, 0, 1.7385503686810E-2, -1.6854802919373E-2, -2.7926478195823E-5 },
            { -5.5803555872956E-2, 0, 0, -1.6854802919373E-2, 1.6912872450250E-2, 6.8562077086609E-6 },
            { -2.2376939343087E-2, 0, 0, -2.7926478195823E-5, 6.8562077086609E-6, 3.1373017650490E-6 } };
        try {
            new UniformlyCorrelatedRandomVectorGenerator(new BlockRealMatrix(covMatrix), 0, null);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        // Covariance matrix with negative terms on diagonal
        final double[][] covMatrix2 = {
            { -1.0842419885089E+3, 0, 0, -4.0669198672901E-1, -5.5803555872956E-2, -2.2376939343087E-2 },
            { 0, 0, 0, 0, 0, 0 },
            { 0, 0, 0, 0, 0, 0 },
            { -4.0669198672901E-1, 0, 0, 1.7385503686810E-2, -1.6854802919373E-2, -2.7926478195823E-5 },
            { -5.5803555872956E-2, 0, 0, -1.6854802919373E-2, 1.6912872450250E-2, 6.8562077086609E-6 },
            { -2.2376939343087E-2, 0, 0, -2.7926478195823E-5, 6.8562077086609E-6, 3.1373017650490E-6 } };
        try {
            new UniformlyCorrelatedRandomVectorGenerator(new BlockRealMatrix(covMatrix2), 0, null);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        // Dimension mismatch
        try {
            new UniformlyCorrelatedRandomVectorGenerator(new double[5], new BlockRealMatrix(covMatrix), 0, null);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        // Non symmetric covariance matrix
        try {
            final double[][] matCov2 = new double[][] { { 1, 0.4 }, { 0.9, 1 } };
            new UniformlyCorrelatedRandomVectorGenerator(new BlockRealMatrix(matCov2), 0., null);
            Assert.fail();
        } catch (final NonSymmetricMatrixException e) {
            Assert.assertTrue(true);
        }
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @description Tests wrong covariance matrices.
     * 
     * @input matrix which is not a covariance matrix
     * 
     * @output exception
     * 
     * @testPassCriteria exceptions are thrown as expected
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testCoverage() {

        // Initialization
        final double[][] covMatrix = new double[6][6];
        covMatrix[0][0] = 1.;
        final UniformlyCorrelatedRandomVectorGenerator generator = new UniformlyCorrelatedRandomVectorGenerator(
            new BlockRealMatrix(covMatrix), 0, new GaussianRandomGenerator(new JDKRandomGenerator()));
        for (int i = 0; i < generator.getRootMatrix().getRowDimension(); i++) {
            if (i == 0) {
                Assert.assertEquals(1.0, generator.getRootMatrix().getEntry(0, 0), Precision.EPSILON);
            } else {
                Assert.assertEquals(0.0, generator.getRootMatrix().getEntry(i, 0), Precision.EPSILON);
            }
        }
        for (int i = 0; i < generator.getStandardDeviationVector().length; i++) {
            if (i == 0) {
                Assert.assertEquals(1.0, generator.getStandardDeviationVector()[0], Precision.EPSILON);
            } else {
                Assert.assertEquals(0.0, generator.getStandardDeviationVector()[i], Precision.EPSILON);
            }
        }
    }

    /**
     * Compute relative difference.
     * 
     * @param a
     *        a double
     * @param b
     *        a double
     * @return relative difference
     */
    private double relDiff(final double a, final double b) {
        return MathLib.abs((a - b) / b);
    }
}
