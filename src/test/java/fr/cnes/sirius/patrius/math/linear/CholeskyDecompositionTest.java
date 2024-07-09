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
* VERSION:4.7:DM:DM-2766:18/05/2021:Evol. et corr. dans le package fr.cnes.sirius.patrius.math.linear (suite DM 2300) 
* VERSION:4.6:FA:FA-2542:27/01/2021:[PATRIUS] Definition d'un champ de vue avec demi-angle de 180° 
 * VERSION:4.5.1:FA:FA-2540:04/08/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear
 * VERSION:4.5:DM:DM-2300:27/05/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.linear;

import java.util.function.Function;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.util.MathLib;

public class CholeskyDecompositionTest {

    private final double[][] testData = new double[][] {
        { 1, 2, 4, 7, 11 },
        { 2, 13, 23, 38, 58 },
        { 4, 23, 77, 122, 182 },
        { 7, 38, 122, 294, 430 },
        { 11, 58, 182, 430, 855 }
    };

    /** Test the constructors */
    @Test
    public void testConstructors() {

        final RealMatrix matrix = MatrixUtils.createRealMatrix(this.testData);

        final CholeskyDecomposition llt1 = new CholeskyDecomposition(matrix);
        final CholeskyDecomposition llt2 = new CholeskyDecomposition(matrix, 1.0e-10, 1.0e-15);

        Assert.assertTrue(llt1.getL().equals(llt2.getL()));
    }

    /** test dimensions */
    @Test
    public void testDimensions() {
        final CholeskyDecomposition llt =
            new CholeskyDecomposition(MatrixUtils.createRealMatrix(this.testData));
        Assert.assertEquals(this.testData.length, llt.getL().getRowDimension());
        Assert.assertEquals(this.testData.length, llt.getL().getColumnDimension());
        Assert.assertEquals(this.testData.length, llt.getLT().getRowDimension());
        Assert.assertEquals(this.testData.length, llt.getLT().getColumnDimension());
    }

    /** test non-square matrix */
    @Test(expected = NonSquareMatrixException.class)
    public void testNonSquare() {
        new CholeskyDecomposition(MatrixUtils.createRealMatrix(new double[3][2]));
    }

    /** test non-symmetric matrix */
    @Test(expected = NonSymmetricMatrixException.class)
    public void testNotSymmetricMatrixException() {
        final double[][] changed = this.testData.clone();
        changed[0][changed[0].length - 1] += 1.0e-5;
        new CholeskyDecomposition(MatrixUtils.createRealMatrix(changed));
    }

    /** test non positive definite matrix */
    @Test(expected = NonPositiveDefiniteMatrixException.class)
    public void testNotPositiveDefinite() {
        new CholeskyDecomposition(MatrixUtils.createRealMatrix(new double[][] {
            { 14, 11, 13, 15, 24 },
            { 11, 34, 13, 8, 25 },
            { 13, 13, 14, 15, 21 },
            { 15, 8, 15, 18, 23 },
            { 24, 25, 21, 23, 45 }
        }));
    }

    @Test(expected = NonPositiveDefiniteMatrixException.class)
    public void testMath274() {
        new CholeskyDecomposition(MatrixUtils.createRealMatrix(new double[][] {
            { 0.40434286, -0.09376327, 0.30328980, 0.04909388 },
            { -0.09376327, 0.10400408, 0.07137959, 0.04762857 },
            { 0.30328980, 0.07137959, 0.30458776, 0.04882449 },
            { 0.04909388, 0.04762857, 0.04882449, 0.07543265 }

        }));
    }

    /** test A = LLT */
    @Test
    public void testAEqualLLT() {
        final RealMatrix matrix = MatrixUtils.createRealMatrix(this.testData);
        final CholeskyDecomposition llt = new CholeskyDecomposition(matrix);
        final RealMatrix l = llt.getL();
        final RealMatrix lt = llt.getLT();
        final double norm = l.multiply(lt).subtract(matrix).getNorm();
        Assert.assertEquals(0, norm, 1.0e-15);
    }

    /** test that L is lower triangular */
    @Test
    public void testLLowerTriangular() {
        final RealMatrix matrix = MatrixUtils.createRealMatrix(this.testData);
        final RealMatrix l = new CholeskyDecomposition(matrix).getL();
        for (int i = 0; i < l.getRowDimension(); i++) {
            for (int j = i + 1; j < l.getColumnDimension(); j++) {
                Assert.assertEquals(0.0, l.getEntry(i, j), 0.0);
            }
        }
    }

    /** test that LT is transpose of L */
    @Test
    public void testLTTransposed() {
        final RealMatrix matrix = MatrixUtils.createRealMatrix(this.testData);
        final CholeskyDecomposition llt = new CholeskyDecomposition(matrix);
        final RealMatrix l = llt.getL();
        final RealMatrix lt = llt.getLT();
        final double norm = l.subtract(lt.transpose()).getNorm();
        Assert.assertEquals(0, norm, 1.0e-15);
    }

    /** test matrices values */
    @Test
    public void testMatricesValues() {
        final RealMatrix lRef = MatrixUtils.createRealMatrix(new double[][] {
            { 1, 0, 0, 0, 0 },
            { 2, 3, 0, 0, 0 },
            { 4, 5, 6, 0, 0 },
            { 7, 8, 9, 10, 0 },
            { 11, 12, 13, 14, 15 }
        });
        final CholeskyDecomposition llt =
            new CholeskyDecomposition(MatrixUtils.createRealMatrix(this.testData));

        // check values against known references
        final RealMatrix l = llt.getL();
        Assert.assertEquals(0, l.subtract(lRef).getNorm(), 1.0e-13);
        final RealMatrix lt = llt.getLT();
        Assert.assertEquals(0, lt.subtract(lRef.transpose()).getNorm(), 1.0e-13);
    }

    /** test matrices values */
    @Test
    public void testDiagonalMatrix() {

        double[] data1 = { 1., 3.4, 2.8 };
        double[][] data2 = { { data1[0], 0, 0 }, { 0, data1[1], 0 }, { 0, 0, data1[2] } };

        DiagonalMatrix diag = new DiagonalMatrix(data1);
        final RealMatrix ref = MatrixUtils.createRealMatrix(data2, false);

        final CholeskyDecomposition decomp1 = new CholeskyDecomposition(diag);
        final CholeskyDecomposition decomp2 = new CholeskyDecomposition(ref);

        Assert.assertTrue(decomp1.getL().equals(decomp2.getL()));
        Assert.assertTrue(decomp1.getLT().equals(decomp2.getLT()));
        Assert.assertEquals(decomp1.getDeterminant(), decomp2.getDeterminant(), 1e-13);

        Assert.assertEquals(MathLib.sqrt(data1[0]), decomp1.getL().getEntry(0, 0), 1e-13);
        Assert.assertEquals(MathLib.sqrt(data1[1]), decomp1.getL().getEntry(1, 1), 1e-13);
        Assert.assertEquals(MathLib.sqrt(data1[2]), decomp1.getL().getEntry(2, 2), 1e-13);
    }
    
    /** test getInvert and decompositionBuilder methods */
    @Test
    public void testInverse() {
        final RealMatrix matrix = MatrixUtils.createRealMatrix(this.testData);
        RealMatrix inv = new CholeskyDecomposition(matrix).getSolver().getInverse();
        Assert.assertTrue(inv.equals(matrix.getInverse(CholeskyDecomposition.decompositionBuilder(1.0e-15, 1.0e-10))));
    }
}
