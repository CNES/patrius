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

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.util.MathLib;

public class TriDiagonalTransformerTest {

    private final double[][] testSquare5 = {
        { 1, 2, 3, 1, 1 },
        { 2, 1, 1, 3, 1 },
        { 3, 1, 1, 1, 2 },
        { 1, 3, 1, 2, 1 },
        { 1, 1, 2, 1, 3 }
    };

    private final double[][] testSquare3 = {
        { 1, 3, 4 },
        { 3, 2, 2 },
        { 4, 2, 0 }
    };

    @Test
    public void testNonSquare() {
        try {
            new TriDiagonalTransformer(MatrixUtils.createRealMatrix(new double[3][2]));
            Assert.fail("an exception should have been thrown");
        } catch (final NonSquareMatrixException ime) {
            // expected behavior
        }
    }

    @Test
    public void testAEqualQTQt() {
        this.checkAEqualQTQt(MatrixUtils.createRealMatrix(this.testSquare5));
        this.checkAEqualQTQt(MatrixUtils.createRealMatrix(this.testSquare3));
    }

    private void checkAEqualQTQt(final RealMatrix matrix) {
        final TriDiagonalTransformer transformer = new TriDiagonalTransformer(matrix);
        final RealMatrix q = transformer.getQ();
        final RealMatrix qT = transformer.getQT();
        final RealMatrix t = transformer.getT();
        final double norm = q.multiply(t).multiply(qT).subtract(matrix).getNorm();
        Assert.assertEquals(0, norm, 4.0e-15);
    }

    @Test
    public void testNoAccessBelowDiagonal() {
        this.checkNoAccessBelowDiagonal(this.testSquare5);
        this.checkNoAccessBelowDiagonal(this.testSquare3);
    }

    private void checkNoAccessBelowDiagonal(final double[][] data) {
        final double[][] modifiedData = new double[data.length][];
        for (int i = 0; i < data.length; ++i) {
            modifiedData[i] = data[i].clone();
            Arrays.fill(modifiedData[i], 0, i, Double.NaN);
        }
        final RealMatrix matrix = MatrixUtils.createRealMatrix(modifiedData, false);
        final TriDiagonalTransformer transformer = new TriDiagonalTransformer(matrix);
        final RealMatrix q = transformer.getQ();
        final RealMatrix qT = transformer.getQT();
        final RealMatrix t = transformer.getT();
        final double norm = q.multiply(t).multiply(qT).subtract(MatrixUtils.createRealMatrix(data)).getNorm();
        Assert.assertEquals(0, norm, 4.0e-15);
    }

    @Test
    public void testQOrthogonal() {
        this.checkOrthogonal(new TriDiagonalTransformer(MatrixUtils.createRealMatrix(this.testSquare5)).getQ());
        this.checkOrthogonal(new TriDiagonalTransformer(MatrixUtils.createRealMatrix(this.testSquare3)).getQ());
    }

    @Test
    public void testQTOrthogonal() {
        this.checkOrthogonal(new TriDiagonalTransformer(MatrixUtils.createRealMatrix(this.testSquare5)).getQT());
        this.checkOrthogonal(new TriDiagonalTransformer(MatrixUtils.createRealMatrix(this.testSquare3)).getQT());
    }

    private void checkOrthogonal(final RealMatrix m) {
        final RealMatrix mTm = m.transpose().multiply(m);
        final RealMatrix id = MatrixUtils.createRealIdentityMatrix(mTm.getRowDimension());
        Assert.assertEquals(0, mTm.subtract(id).getNorm(), 1.0e-15);
    }

    @Test
    public void testTTriDiagonal() {
        this.checkTriDiagonal(new TriDiagonalTransformer(MatrixUtils.createRealMatrix(this.testSquare5)).getT());
        this.checkTriDiagonal(new TriDiagonalTransformer(MatrixUtils.createRealMatrix(this.testSquare3)).getT());
    }

    private void checkTriDiagonal(final RealMatrix m) {
        final int rows = m.getRowDimension();
        final int cols = m.getColumnDimension();
        for (int i = 0; i < rows; ++i) {
            for (int j = 0; j < cols; ++j) {
                if ((i < j - 1) || (i > j + 1)) {
                    Assert.assertEquals(0, m.getEntry(i, j), 1.0e-16);
                }
            }
        }
    }

    @Test
    public void testMatricesValues5() {
        this.checkMatricesValues(this.testSquare5,
            new double[][] {
                { 1.0, 0.0, 0.0, 0.0, 0.0 },
                { 0.0, -0.5163977794943222, 0.016748280772542083, 0.839800693771262, 0.16669620021405473 },
                { 0.0, -0.7745966692414833, -0.4354553000860955, -0.44989322880603355, -0.08930153582895772 },
                { 0.0, -0.2581988897471611, 0.6364346693566014, -0.30263204032131164, 0.6608313651342882 },
                { 0.0, -0.2581988897471611, 0.6364346693566009, -0.027289660803112598, -0.7263191580755246 }
            },
            new double[] { 1, 4.4, 1.433099579242636, -0.89537362758743, 2.062274048344794 },
            new double[] { -MathLib.sqrt(15), -3.0832882879592476, 0.6082710842351517, 1.1786086405912128 });
    }

    @Test
    public void testMatricesValues3() {
        this.checkMatricesValues(this.testSquare3,
            new double[][] {
                { 1.0, 0.0, 0.0 },
                { 0.0, -0.6, 0.8 },
                { 0.0, -0.8, -0.6 },
            },
            new double[] { 1, 2.64, -0.64 },
            new double[] { -5, -1.52 });
    }

    private void checkMatricesValues(final double[][] matrix, final double[][] qRef,
                                     final double[] mainDiagnonal,
                                     final double[] secondaryDiagonal) {
        final TriDiagonalTransformer transformer =
            new TriDiagonalTransformer(MatrixUtils.createRealMatrix(matrix, false));

        // check values against known references
        final RealMatrix q = transformer.getQ();
        Assert.assertEquals(0, q.subtract(MatrixUtils.createRealMatrix(qRef)).getNorm(), 1.0e-14);

        final RealMatrix t = transformer.getT();
        final double[][] tData = new double[mainDiagnonal.length][mainDiagnonal.length];
        for (int i = 0; i < mainDiagnonal.length; ++i) {
            tData[i][i] = mainDiagnonal[i];
            if (i > 0) {
                tData[i][i - 1] = secondaryDiagonal[i - 1];
            }
            if (i < secondaryDiagonal.length) {
                tData[i][i + 1] = secondaryDiagonal[i];
            }
        }
        Assert.assertEquals(0, t.subtract(MatrixUtils.createRealMatrix(tData)).getNorm(), 1.0e-14);

        // check the same cached instance is returned the second time
        Assert.assertTrue(q == transformer.getQ());
        Assert.assertTrue(t == transformer.getT());
    }
}
