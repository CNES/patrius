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
 * VERSION:4.7:DM:DM-2766:18/05/2021:Evol. et corr. dans le package fr.cnes.sirius.patrius.math.linear (suite DM 2300) 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.distribution;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.stat.correlation.Covariance;

/**
 * Test cases for {@link MultivariateNormalDistribution}.
 */
public class MultivariateNormalDistributionTest {
    /**
     * Test the ability of the distribution to report its mean value parameter.
     */
    @Test
    public void testGetMean() {
        final double[] mu = { -1.5, 2 };
        final double[][] sigma = { { 2, -1.1 },
            { -1.1, 2 } };
        final MultivariateNormalDistribution d = new MultivariateNormalDistribution(mu, sigma);

        final double[] m = d.getMeans();
        for (int i = 0; i < m.length; i++) {
            Assert.assertEquals(mu[i], m[i], 0);
        }
    }

    /**
     * Test the ability of the distribution to report its covariance matrix parameter.
     */
    @Test
    public void testGetCovarianceMatrix() {
        final double[] mu = { -1.5, 2 };
        final double[][] sigma = { { 2, -1.1 },
            { -1.1, 2 } };
        final MultivariateNormalDistribution d = new MultivariateNormalDistribution(mu, sigma);

        final RealMatrix s = d.getCovariances();
        final int dim = d.getDimension();
        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < dim; j++) {
                Assert.assertEquals(sigma[i][j], s.getEntry(i, j), 0);
            }
        }
    }

    /**
     * Test the accuracy of sampling from the distribution.
     */
    @Test
    public void testSampling() {
        final double[] mu = { -1.5, 2 };
        final double[][] sigma = { { 2, -1.1 },
            { -1.1, 2 } };
        final MultivariateNormalDistribution d = new MultivariateNormalDistribution(mu, sigma);
        d.reseedRandomGenerator(50);

        final int n = 500000;

        final double[][] samples = d.sample(n);
        final int dim = d.getDimension();
        final double[] sampleMeans = new double[dim];

        for (final double[] sample : samples) {
            for (int j = 0; j < dim; j++) {
                sampleMeans[j] += sample[j];
            }
        }

        final double sampledValueTolerance = 1e-2;
        for (int j = 0; j < dim; j++) {
            sampleMeans[j] /= samples.length;
            Assert.assertEquals(mu[j], sampleMeans[j], sampledValueTolerance);
        }

        final double[][] sampleSigma = new Covariance(samples).getCovarianceMatrix().getData(false);
        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < dim; j++) {
                Assert.assertEquals(sigma[i][j], sampleSigma[i][j], sampledValueTolerance);
            }
        }
    }

    /**
     * Test the accuracy of the distribution when calculating densities.
     */
    @Test
    public void testDensities() {
        final double[] mu = { -1.5, 2 };
        final double[][] sigma = { { 2, -1.1 },
            { -1.1, 2 } };
        final MultivariateNormalDistribution d = new MultivariateNormalDistribution(mu, sigma);

        final double[][] testValues = { { -1.5, 2 },
            { 4, 4 },
            { 1.5, -2 },
            { 0, 0 } };
        final double[] densities = new double[testValues.length];
        for (int i = 0; i < densities.length; i++) {
            densities[i] = d.density(testValues[i]);
        }

        // From dmvnorm function in R 2.15 CRAN package Mixtools v0.4.5
        final double[] correctDensities = { 0.09528357207691344,
            5.80932710124009e-09,
            0.001387448895173267,
            0.03309922090210541 };

        for (int i = 0; i < testValues.length; i++) {
            Assert.assertEquals(correctDensities[i], densities[i], 1e-16);
        }
    }
}
