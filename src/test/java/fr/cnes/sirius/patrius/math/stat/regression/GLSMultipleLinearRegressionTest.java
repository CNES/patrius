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
 * VERSION:4.7:DM:DM-2766:18/05/2021:Evol. et corr. dans le package fr.cnes.sirius.patrius.math.linear (suite DM 2300) 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.stat.regression;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.linear.MatrixUtils;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealVector;
import fr.cnes.sirius.patrius.math.random.CorrelatedRandomVectorGenerator;
import fr.cnes.sirius.patrius.math.random.GaussianRandomGenerator;
import fr.cnes.sirius.patrius.math.random.JDKRandomGenerator;
import fr.cnes.sirius.patrius.math.random.RandomGenerator;
import fr.cnes.sirius.patrius.math.stat.correlation.Covariance;
import fr.cnes.sirius.patrius.math.stat.descriptive.DescriptiveStatistics;

public class GLSMultipleLinearRegressionTest extends MultipleLinearRegressionAbstractTest {

    private double[] y;
    private double[][] x;
    private double[][] omega;
    private final double[] longley = new double[] {
        60323, 83.0, 234289, 2356, 1590, 107608, 1947,
        61122, 88.5, 259426, 2325, 1456, 108632, 1948,
        60171, 88.2, 258054, 3682, 1616, 109773, 1949,
        61187, 89.5, 284599, 3351, 1650, 110929, 1950,
        63221, 96.2, 328975, 2099, 3099, 112075, 1951,
        63639, 98.1, 346999, 1932, 3594, 113270, 1952,
        64989, 99.0, 365385, 1870, 3547, 115094, 1953,
        63761, 100.0, 363112, 3578, 3350, 116219, 1954,
        66019, 101.2, 397469, 2904, 3048, 117388, 1955,
        67857, 104.6, 419180, 2822, 2857, 118734, 1956,
        68169, 108.4, 442769, 2936, 2798, 120445, 1957,
        66513, 110.8, 444546, 4681, 2637, 121950, 1958,
        68655, 112.6, 482704, 3813, 2552, 123366, 1959,
        69564, 114.2, 502601, 3931, 2514, 125368, 1960,
        69331, 115.7, 518173, 4806, 2572, 127852, 1961,
        70551, 116.9, 554894, 4007, 2827, 130081, 1962
    };

    @Before
    @Override
    public void setUp() {
        this.y = new double[] { 11.0, 12.0, 13.0, 14.0, 15.0, 16.0 };
        this.x = new double[6][];
        this.x[0] = new double[] { 0, 0, 0, 0, 0 };
        this.x[1] = new double[] { 2.0, 0, 0, 0, 0 };
        this.x[2] = new double[] { 0, 3.0, 0, 0, 0 };
        this.x[3] = new double[] { 0, 0, 4.0, 0, 0 };
        this.x[4] = new double[] { 0, 0, 0, 5.0, 0 };
        this.x[5] = new double[] { 0, 0, 0, 0, 6.0 };
        this.omega = new double[6][];
        this.omega[0] = new double[] { 1.0, 0, 0, 0, 0, 0 };
        this.omega[1] = new double[] { 0, 2.0, 0, 0, 0, 0 };
        this.omega[2] = new double[] { 0, 0, 3.0, 0, 0, 0 };
        this.omega[3] = new double[] { 0, 0, 0, 4.0, 0, 0 };
        this.omega[4] = new double[] { 0, 0, 0, 0, 5.0, 0 };
        this.omega[5] = new double[] { 0, 0, 0, 0, 0, 6.0 };
        super.setUp();
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotAddXSampleData() {
        this.createRegression().newSampleData(new double[] {}, null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotAddNullYSampleData() {
        this.createRegression().newSampleData(null, new double[][] {}, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotAddSampleDataWithSizeMismatch() {
        final double[] y = new double[] { 1.0, 2.0 };
        final double[][] x = new double[1][];
        x[0] = new double[] { 1.0, 0 };
        this.createRegression().newSampleData(y, x, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotAddNullCovarianceData() {
        this.createRegression().newSampleData(new double[] {}, new double[][] {}, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void notEnoughData() {
        final double[] reducedY = new double[this.y.length - 1];
        final double[][] reducedX = new double[this.x.length - 1][];
        final double[][] reducedO = new double[this.omega.length - 1][];
        System.arraycopy(this.y, 0, reducedY, 0, reducedY.length);
        System.arraycopy(this.x, 0, reducedX, 0, reducedX.length);
        System.arraycopy(this.omega, 0, reducedO, 0, reducedO.length);
        this.createRegression().newSampleData(reducedY, reducedX, reducedO);
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotAddCovarianceDataWithSampleSizeMismatch() {
        final double[] y = new double[] { 1.0, 2.0 };
        final double[][] x = new double[2][];
        x[0] = new double[] { 1.0, 0 };
        x[1] = new double[] { 0, 1.0 };
        final double[][] omega = new double[1][];
        omega[0] = new double[] { 1.0, 0 };
        this.createRegression().newSampleData(y, x, omega);
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotAddCovarianceDataThatIsNotSquare() {
        final double[] y = new double[] { 1.0, 2.0 };
        final double[][] x = new double[2][];
        x[0] = new double[] { 1.0, 0 };
        x[1] = new double[] { 0, 1.0 };
        final double[][] omega = new double[3][];
        omega[0] = new double[] { 1.0, 0 };
        omega[1] = new double[] { 0, 1.0 };
        omega[2] = new double[] { 0, 2.0 };
        this.createRegression().newSampleData(y, x, omega);
    }

    @Override
    protected GLSMultipleLinearRegression createRegression() {
        final GLSMultipleLinearRegression regression = new GLSMultipleLinearRegression();
        regression.newSampleData(this.y, this.x, this.omega);
        return regression;
    }

    @Override
    protected int getNumberOfRegressors() {
        return this.x[0].length + 1;
    }

    @Override
    protected int getSampleSize() {
        return this.y.length;
    }

    /**
     * test calculateYVariance
     */
    @Test
    public void testYVariance() {

        // assumes: y = new double[]{11.0, 12.0, 13.0, 14.0, 15.0, 16.0};

        final GLSMultipleLinearRegression model = new GLSMultipleLinearRegression();
        model.newSampleData(this.y, this.x, this.omega);
        TestUtils.assertEquals(model.calculateYVariance(), 3.5, 0);
    }

    /**
     * Verifies that setting X, Y and covariance separately has the same effect as newSample(X,Y,cov).
     */
    @Test
    public void testNewSample2() {
        final double[] y = new double[] { 1, 2, 3, 4 };
        final double[][] x = new double[][] {
            { 19, 22, 33 },
            { 20, 30, 40 },
            { 25, 35, 45 },
            { 27, 37, 47 }
        };
        final double[][] covariance = MatrixUtils.createRealIdentityMatrix(4).scalarMultiply(2).getData(false);
        final GLSMultipleLinearRegression regression = new GLSMultipleLinearRegression();
        regression.newSampleData(y, x, covariance);
        final RealMatrix combinedX = regression.getX().copy();
        final RealVector combinedY = regression.getY().copy();
        final RealMatrix combinedCovInv = regression.getOmegaInverse();
        regression.newXSampleData(x);
        regression.newYSampleData(y);
        Assert.assertEquals(combinedX, regression.getX());
        Assert.assertEquals(combinedY, regression.getY());
        Assert.assertEquals(combinedCovInv, regression.getOmegaInverse());
    }

    /**
     * Verifies that GLS with identity covariance matrix gives the same results
     * as OLS.
     */
    @Test
    public void testGLSOLSConsistency() {
        final RealMatrix identityCov = MatrixUtils.createRealIdentityMatrix(16);
        final GLSMultipleLinearRegression glsModel = new GLSMultipleLinearRegression();
        final OLSMultipleLinearRegression olsModel = new OLSMultipleLinearRegression();
        glsModel.newSampleData(this.longley, 16, 6);
        olsModel.newSampleData(this.longley, 16, 6);
        glsModel.newCovarianceData(identityCov.getData(false));
        final double[] olsBeta = olsModel.calculateBeta().toArray();
        final double[] glsBeta = glsModel.calculateBeta().toArray();
        for (int i = 0; i < olsBeta.length; i++) {
            TestUtils.assertRelativelyEquals(olsBeta[i], glsBeta[i], 10E-7);
        }
    }

    /**
     * Generate an error covariance matrix and sample data representing models
     * with this error structure. Then verify that GLS estimated coefficients,
     * on average, perform better than OLS.
     */
    @Test
    public void testGLSEfficiency() {
        final RandomGenerator rg = new JDKRandomGenerator();
        rg.setSeed(200); // Seed has been selected to generate non-trivial covariance

        // Assume model has 16 observations (will use Longley data). Start by generating
        // non-constant variances for the 16 error terms.
        final int nObs = 16;
        final double[] sigma = new double[nObs];
        for (int i = 0; i < nObs; i++) {
            sigma[i] = 10 * rg.nextDouble();
        }

        // Now generate 1000 error vectors to use to estimate the covariance matrix
        // Columns are draws on N(0, sigma[col])
        final int numSeeds = 1000;
        final RealMatrix errorSeeds = MatrixUtils.createRealMatrix(numSeeds, nObs);
        for (int i = 0; i < numSeeds; i++) {
            for (int j = 0; j < nObs; j++) {
                errorSeeds.setEntry(i, j, rg.nextGaussian() * sigma[j]);
            }
        }

        // Get covariance matrix for columns
        final RealMatrix cov = (new Covariance(errorSeeds)).getCovarianceMatrix();

        // Create a CorrelatedRandomVectorGenerator to use to generate correlated errors
        final GaussianRandomGenerator rawGenerator = new GaussianRandomGenerator(rg);
        final double[] errorMeans = new double[nObs]; // Counting on init to 0 here
        final CorrelatedRandomVectorGenerator gen = new CorrelatedRandomVectorGenerator(errorMeans, cov,
            1.0e-12 * cov.getNorm(), rawGenerator);

        // Now start generating models. Use Longley X matrix on LHS
        // and Longley OLS beta vector as "true" beta. Generate
        // Y values by XB + u where u is a CorrelatedRandomVector generated
        // from cov.
        final OLSMultipleLinearRegression ols = new OLSMultipleLinearRegression();
        ols.newSampleData(this.longley, nObs, 6);
        final RealVector b = ols.calculateBeta().copy();
        final RealMatrix x = ols.getX().copy();

        // Create a GLS model to reuse
        final GLSMultipleLinearRegression gls = new GLSMultipleLinearRegression();
        gls.newSampleData(this.longley, nObs, 6);
        gls.newCovarianceData(cov.getData(false));

        // Create aggregators for stats measuring model performance
        final DescriptiveStatistics olsBetaStats = new DescriptiveStatistics();
        final DescriptiveStatistics glsBetaStats = new DescriptiveStatistics();

        // Generate Y vectors for 10000 models, estimate GLS and OLS and
        // Verify that OLS estimates are better
        final int nModels = 10000;
        for (int i = 0; i < nModels; i++) {

            // Generate y = xb + u with u cov
            final RealVector u = MatrixUtils.createRealVector(gen.nextVector());
            final double[] y = u.add(x.operate(b)).toArray();

            // Estimate OLS parameters
            ols.newYSampleData(y);
            final RealVector olsBeta = ols.calculateBeta();

            // Estimate GLS parameters
            gls.newYSampleData(y);
            final RealVector glsBeta = gls.calculateBeta();

            // Record deviations from "true" beta
            double dist = olsBeta.getDistance(b);
            olsBetaStats.addValue(dist * dist);
            dist = glsBeta.getDistance(b);
            glsBetaStats.addValue(dist * dist);

        }

        // Verify that GLS is on average more efficient, lower variance
        assert (olsBetaStats.getMean() > 1.5 * glsBetaStats.getMean());
        assert (olsBetaStats.getStandardDeviation() > glsBetaStats.getStandardDeviation());
    }

}
