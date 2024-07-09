/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 *
 * Copyright 2011-2017 CNES
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
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.random;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.linear.Array2DRowRealMatrix;
import fr.cnes.sirius.patrius.math.linear.MatrixUtils;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.stat.correlation.StorelessCovariance;
import fr.cnes.sirius.patrius.math.stat.descriptive.moment.VectorialCovariance;
import fr.cnes.sirius.patrius.math.stat.descriptive.moment.VectorialMean;
import fr.cnes.sirius.patrius.math.util.MathLib;

public class CorrelatedRandomVectorGeneratorTest {
    private final double[] mean;
    private final RealMatrix covariance;
    private final CorrelatedRandomVectorGenerator generator;

    public CorrelatedRandomVectorGeneratorTest() {
        this.mean = new double[] { 0.0, 1.0, -3.0, 2.3 };

        final RealMatrix b = MatrixUtils.createRealMatrix(4, 3);
        int counter = 0;
        for (int i = 0; i < b.getRowDimension(); ++i) {
            for (int j = 0; j < b.getColumnDimension(); ++j) {
                b.setEntry(i, j, 1.0 + 0.1 * ++counter);
            }
        }
        final RealMatrix bbt = b.multiply(b.transpose());
        this.covariance = MatrixUtils.createRealMatrix(this.mean.length, this.mean.length);
        for (int i = 0; i < this.covariance.getRowDimension(); ++i) {
            this.covariance.setEntry(i, i, bbt.getEntry(i, i));
            for (int j = 0; j < this.covariance.getColumnDimension(); ++j) {
                final double s = bbt.getEntry(i, j);
                this.covariance.setEntry(i, j, s);
                this.covariance.setEntry(j, i, s);
            }
        }

        final RandomGenerator rg = new JDKRandomGenerator();
        rg.setSeed(17399225432l);
        final GaussianRandomGenerator rawGenerator = new GaussianRandomGenerator(rg);
        this.generator = new CorrelatedRandomVectorGenerator(this.mean,
            this.covariance,
            1.0e-12 * this.covariance.getNorm(),
            rawGenerator);
    }

    @Test
    public void testRank() {
        Assert.assertEquals(2, this.generator.getRank());
    }

    @Test
    public void testMath226() {
        final double[] mean = { 1, 1, 10, 1 };
        final double[][] cov = {
            { 1, 3, 2, 6 },
            { 3, 13, 16, 2 },
            { 2, 16, 38, -1 },
            { 6, 2, -1, 197 }
        };
        final RealMatrix covRM = MatrixUtils.createRealMatrix(cov);
        final JDKRandomGenerator jg = new JDKRandomGenerator();
        jg.setSeed(5322145245211l);
        final NormalizedRandomGenerator rg = new GaussianRandomGenerator(jg);
        final CorrelatedRandomVectorGenerator sg =
            new CorrelatedRandomVectorGenerator(mean, covRM, 0.00001, rg);

        final double[] min = new double[mean.length];
        Arrays.fill(min, Double.POSITIVE_INFINITY);
        final double[] max = new double[mean.length];
        Arrays.fill(max, Double.NEGATIVE_INFINITY);
        for (int i = 0; i < 10; i++) {
            final double[] generated = sg.nextVector();
            for (int j = 0; j < generated.length; ++j) {
                min[j] = MathLib.min(min[j], generated[j]);
                max[j] = MathLib.max(max[j], generated[j]);
            }
        }
        for (int j = 0; j < min.length; ++j) {
            Assert.assertTrue(max[j] - min[j] > 2.0);
        }

    }

    @Test
    public void testRootMatrix() {
        final RealMatrix b = this.generator.getRootMatrix();
        final RealMatrix bbt = b.multiply(b.transpose());
        for (int i = 0; i < this.covariance.getRowDimension(); ++i) {
            for (int j = 0; j < this.covariance.getColumnDimension(); ++j) {
                Assert.assertEquals(this.covariance.getEntry(i, j), bbt.getEntry(i, j), 1.0e-12);
            }
        }
    }

    @Test
    public void testMeanAndCovariance() {

        final VectorialMean meanStat = new VectorialMean(this.mean.length);
        final VectorialCovariance covStat = new VectorialCovariance(this.mean.length, true);
        for (int i = 0; i < 5000; ++i) {
            final double[] v = this.generator.nextVector();
            meanStat.increment(v);
            covStat.increment(v);
        }

        final double[] estimatedMean = meanStat.getResult();
        final RealMatrix estimatedCovariance = covStat.getResult();
        for (int i = 0; i < estimatedMean.length; ++i) {
            Assert.assertEquals(this.mean[i], estimatedMean[i], 0.07);
            for (int j = 0; j <= i; ++j) {
                Assert.assertEquals(this.covariance.getEntry(i, j),
                    estimatedCovariance.getEntry(i, j),
                    0.1 * (1.0 + MathLib.abs(this.mean[i])) * (1.0 + MathLib.abs(this.mean[j])));
            }
        }

    }

    @Test
    public void testSampleWithZeroCovariance() {
        final double[][] covMatrix1 = new double[][] {
            { 0.013445532, 0.010394690, 0.009881156, 0.010499559 },
            { 0.010394690, 0.023006616, 0.008196856, 0.010732709 },
            { 0.009881156, 0.008196856, 0.019023866, 0.009210099 },
            { 0.010499559, 0.010732709, 0.009210099, 0.019107243 }
        };

        final double[][] covMatrix2 = new double[][] {
            { 0.0, 0.0, 0.0, 0.0, 0.0 },
            { 0.0, 0.013445532, 0.010394690, 0.009881156, 0.010499559 },
            { 0.0, 0.010394690, 0.023006616, 0.008196856, 0.010732709 },
            { 0.0, 0.009881156, 0.008196856, 0.019023866, 0.009210099 },
            { 0.0, 0.010499559, 0.010732709, 0.009210099, 0.019107243 }
        };

        final double[][] covMatrix3 = new double[][] {
            { 0.013445532, 0.010394690, 0.0, 0.009881156, 0.010499559 },
            { 0.010394690, 0.023006616, 0.0, 0.008196856, 0.010732709 },
            { 0.0, 0.0, 0.0, 0.0, 0.0 },
            { 0.009881156, 0.008196856, 0.0, 0.019023866, 0.009210099 },
            { 0.010499559, 0.010732709, 0.0, 0.009210099, 0.019107243 }
        };

        this.testSampler(covMatrix1, 10000, 0.001);
        this.testSampler(covMatrix2, 10000, 0.001);
        this.testSampler(covMatrix3, 10000, 0.001);

    }

    private CorrelatedRandomVectorGenerator createSampler(final double[][] cov) {
        final RealMatrix matrix = new Array2DRowRealMatrix(cov);
        final double small = 10e-12 * matrix.getNorm();
        return new CorrelatedRandomVectorGenerator(
            new double[cov.length],
            matrix,
            small,
            new GaussianRandomGenerator(new JDKRandomGenerator()));
    }

    private void testSampler(final double[][] covMatrix, final int samples, final double epsilon) {
        final CorrelatedRandomVectorGenerator sampler = this.createSampler(covMatrix);

        final StorelessCovariance cov = new StorelessCovariance(covMatrix.length);
        for (int i = 0; i < samples; ++i) {
            cov.increment(sampler.nextVector());
        }

        final double[][] sampleCov = cov.getData();
        for (int r = 0; r < covMatrix.length; ++r) {
            TestUtils.assertEquals(covMatrix[r], sampleCov[r], epsilon);
        }

    }

}
