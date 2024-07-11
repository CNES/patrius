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
 * VERSION:4.5:DM:DM-2300:27/05/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.linear;

import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.distribution.NormalDistribution;

public class HessenbergTransformerTest {

    private final double[][] testSquare5 = {
        { 5, 4, 3, 2, 1 },
        { 1, 4, 0, 3, 3 },
        { 2, 0, 3, 0, 0 },
        { 3, 2, 1, 2, 5 },
        { 4, 2, 1, 4, 1 }
    };

    private final double[][] testSquare3 = {
        { 2, -1, 1 },
        { -1, 2, 1 },
        { 1, -1, 2 }
    };

    // from http://eigen.tuxfamily.org/dox/classEigen_1_1HessenbergDecomposition.html

    private final double[][] testRandom = {
        { 0.680, 0.823, -0.4440, -0.2700 },
        { -0.211, -0.605, 0.1080, 0.0268 },
        { 0.566, -0.330, -0.0452, 0.9040 },
        { 0.597, 0.536, 0.2580, 0.8320 }
    };

    @Test
    public void testNonSquare() {
        try {
            new HessenbergTransformer(MatrixUtils.createRealMatrix(new double[3][2]));
            Assert.fail("an exception should have been thrown");
        } catch (final NonSquareMatrixException ime) {
            // expected behavior
        }
    }

    @Test
    public void testAEqualPHPt() {
        this.checkAEqualPHPt(MatrixUtils.createRealMatrix(this.testSquare5));
        this.checkAEqualPHPt(MatrixUtils.createRealMatrix(this.testSquare3));
        this.checkAEqualPHPt(MatrixUtils.createRealMatrix(this.testRandom));
    }

    @Test
    public void testPOrthogonal() {
        this.checkOrthogonal(new HessenbergTransformer(MatrixUtils.createRealMatrix(this.testSquare5)).getP());
        this.checkOrthogonal(new HessenbergTransformer(MatrixUtils.createRealMatrix(this.testSquare3)).getP());
    }

    @Test
    public void testPTOrthogonal() {
        this.checkOrthogonal(new HessenbergTransformer(MatrixUtils.createRealMatrix(this.testSquare5)).getPT());
        this.checkOrthogonal(new HessenbergTransformer(MatrixUtils.createRealMatrix(this.testSquare3)).getPT());
    }

    @Test
    public void testHessenbergForm() {
        this.checkHessenbergForm(new HessenbergTransformer(MatrixUtils.createRealMatrix(this.testSquare5)).getH());
        this.checkHessenbergForm(new HessenbergTransformer(MatrixUtils.createRealMatrix(this.testSquare3)).getH());
    }

    @Test
    public void testRandomData() {
        for (int run = 0; run < 100; run++) {
            final Random r = new Random(System.currentTimeMillis());

            // matrix size
            final int size = r.nextInt(20) + 4;

            final double[][] data = new double[size][size];
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    data[i][j] = r.nextInt(100);
                }
            }

            final RealMatrix m = MatrixUtils.createRealMatrix(data);
            final RealMatrix h = this.checkAEqualPHPt(m);
            this.checkHessenbergForm(h);
        }
    }

    @Test
    public void testRandomDataNormalDistribution() {
        for (int run = 0; run < 100; run++) {
            final Random r = new Random(System.currentTimeMillis());
            final NormalDistribution dist = new NormalDistribution(0.0, r.nextDouble() * 5);

            // matrix size
            final int size = r.nextInt(20) + 4;

            final double[][] data = new double[size][size];
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    data[i][j] = dist.sample();
                }
            }

            final RealMatrix m = MatrixUtils.createRealMatrix(data);
            final RealMatrix h = this.checkAEqualPHPt(m);
            this.checkHessenbergForm(h);
        }
    }

    @Test
    public void testMatricesValues5() {
        this.checkMatricesValues(this.testSquare5,
            new double[][] {
                { 1.0, 0.0, 0.0, 0.0, 0.0 },
                { 0.0, -0.182574185835055, 0.784218758628863, 0.395029040913988, -0.442289115981669 },
                { 0.0, -0.365148371670111, -0.337950625265477, -0.374110794088820, -0.782621974707823 },
                { 0.0, -0.547722557505166, 0.402941130124223, -0.626468266309003, 0.381019628053472 },
                { 0.0, -0.730296743340221, -0.329285224617644, 0.558149336547665, 0.216118545309225 }
            },
            new double[][] {
                { 5.0, -3.65148371670111, 2.59962019434982, -0.237003414680848, -3.13886458663398 },
                { -5.47722557505166, 6.9, -2.29164066120599, 0.207283564429169, 0.703858369151728 },
                { 0.0, -4.21386600008432, 2.30555659846067, 2.74935928725112, 0.857569835914113 },
                { 0.0, 0.0, 2.86406180891882, -1.11582249161595, 0.817995267184158 },
                { 0.0, 0.0, 0.0, 0.683518597386085, 1.91026589315528 }
            });
    }

    @Test
    public void testMatricesValues3() {
        this.checkMatricesValues(this.testSquare3,
            new double[][] {
                { 1.0, 0.0, 0.0 },
                { 0.0, -0.707106781186547, 0.707106781186547 },
                { 0.0, 0.707106781186547, 0.707106781186548 },
            },
            new double[][] {
                { 2.0, 1.41421356237309, 0.0 },
                { 1.41421356237310, 2.0, -1.0 },
                { 0.0, 1.0, 2.0 },
            });
    }

    // /////////////////////////////////////////////////////////////////////////
    // Test helpers
    // /////////////////////////////////////////////////////////////////////////

    private RealMatrix checkAEqualPHPt(final RealMatrix matrix) {
        final HessenbergTransformer transformer = new HessenbergTransformer(matrix);
        final RealMatrix p = transformer.getP();
        final RealMatrix pT = transformer.getPT();
        final RealMatrix h = transformer.getH();

        final RealMatrix result = p.multiply(h).multiply(pT);
        final double norm = result.subtract(matrix).getNorm();
        Assert.assertEquals(0, norm, 1.0e-10);

        for (int i = 0; i < matrix.getRowDimension(); ++i) {
            for (int j = 0; j < matrix.getColumnDimension(); ++j) {
                if (i > j + 1) {
                    Assert.assertEquals(matrix.getEntry(i, j), result.getEntry(i, j), 1.0e-12);
                }
            }
        }

        return transformer.getH();
    }

    private void checkOrthogonal(final RealMatrix m) {
        final RealMatrix mTm = m.transpose().multiply(m);
        final RealMatrix id = MatrixUtils.createRealIdentityMatrix(mTm.getRowDimension());
        Assert.assertEquals(0, mTm.subtract(id).getNorm(), 1.0e-14);
    }

    private void checkHessenbergForm(final RealMatrix m) {
        final int rows = m.getRowDimension();
        final int cols = m.getColumnDimension();
        for (int i = 0; i < rows; ++i) {
            for (int j = 0; j < cols; ++j) {
                if (i > j + 1) {
                    Assert.assertEquals(0, m.getEntry(i, j), 1.0e-16);
                }
            }
        }
    }

    private void checkMatricesValues(final double[][] matrix, final double[][] pRef, final double[][] hRef) {
        final HessenbergTransformer transformer =
            new HessenbergTransformer(MatrixUtils.createRealMatrix(matrix, false));

        // check values against known references
        final RealMatrix p = transformer.getP();
        Assert.assertEquals(0, p.subtract(MatrixUtils.createRealMatrix(pRef)).getNorm(), 1.0e-14);

        final RealMatrix h = transformer.getH();
        Assert.assertEquals(0, h.subtract(MatrixUtils.createRealMatrix(hRef)).getNorm(), 1.0e-14);

        // check the same cached instance is returned the second time
        Assert.assertTrue(p == transformer.getP());
        Assert.assertTrue(h == transformer.getH());
    }
}
