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
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.linear;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.util.MathLib;

public class BiDiagonalTransformerTest {

    private final double[][] testSquare = {
        { 24.0 / 25.0, 43.0 / 25.0 },
        { 57.0 / 25.0, 24.0 / 25.0 }
    };

    private final double[][] testNonSquare = {
        { -540.0 / 625.0, 963.0 / 625.0, -216.0 / 625.0 },
        { -1730.0 / 625.0, -744.0 / 625.0, 1008.0 / 625.0 },
        { -720.0 / 625.0, 1284.0 / 625.0, -288.0 / 625.0 },
        { -360.0 / 625.0, 192.0 / 625.0, 1756.0 / 625.0 },
    };

    @Test
    public void testDimensions() {
        this.checkdimensions(MatrixUtils.createRealMatrix(this.testSquare));
        this.checkdimensions(MatrixUtils.createRealMatrix(this.testNonSquare));
        this.checkdimensions(MatrixUtils.createRealMatrix(this.testNonSquare).transpose());
    }

    private void checkdimensions(final RealMatrix matrix) {
        final int m = matrix.getRowDimension();
        final int n = matrix.getColumnDimension();
        final BiDiagonalTransformer transformer = new BiDiagonalTransformer(matrix);
        Assert.assertEquals(m, transformer.getU().getRowDimension());
        Assert.assertEquals(m, transformer.getU().getColumnDimension());
        Assert.assertEquals(m, transformer.getB().getRowDimension());
        Assert.assertEquals(n, transformer.getB().getColumnDimension());
        Assert.assertEquals(n, transformer.getV().getRowDimension());
        Assert.assertEquals(n, transformer.getV().getColumnDimension());

    }

    @Test
    public void testAEqualUSVt() {
        this.checkAEqualUSVt(MatrixUtils.createRealMatrix(this.testSquare));
        this.checkAEqualUSVt(MatrixUtils.createRealMatrix(this.testNonSquare));
        this.checkAEqualUSVt(MatrixUtils.createRealMatrix(this.testNonSquare).transpose());
    }

    private void checkAEqualUSVt(final RealMatrix matrix) {
        final BiDiagonalTransformer transformer = new BiDiagonalTransformer(matrix);
        final RealMatrix u = transformer.getU();
        final RealMatrix b = transformer.getB();
        final RealMatrix v = transformer.getV();
        final double norm = u.multiply(b).multiply(v.transpose()).subtract(matrix).getNorm();
        Assert.assertEquals(0, norm, 1.0e-14);
    }

    @Test
    public void testUOrthogonal() {
        this.checkOrthogonal(new BiDiagonalTransformer(MatrixUtils.createRealMatrix(this.testSquare)).getU());
        this.checkOrthogonal(new BiDiagonalTransformer(MatrixUtils.createRealMatrix(this.testNonSquare)).getU());
        this.checkOrthogonal(new BiDiagonalTransformer(MatrixUtils.createRealMatrix(this.testNonSquare).transpose())
            .getU());
    }

    @Test
    public void testVOrthogonal() {
        this.checkOrthogonal(new BiDiagonalTransformer(MatrixUtils.createRealMatrix(this.testSquare)).getV());
        this.checkOrthogonal(new BiDiagonalTransformer(MatrixUtils.createRealMatrix(this.testNonSquare)).getV());
        this.checkOrthogonal(new BiDiagonalTransformer(MatrixUtils.createRealMatrix(this.testNonSquare).transpose())
            .getV());
    }

    private void checkOrthogonal(final RealMatrix m) {
        final RealMatrix mTm = m.transpose().multiply(m);
        final RealMatrix id = MatrixUtils.createRealIdentityMatrix(mTm.getRowDimension());
        Assert.assertEquals(0, mTm.subtract(id).getNorm(), 1.0e-14);
    }

    @Test
    public void testBBiDiagonal() {
        this.checkBiDiagonal(new BiDiagonalTransformer(MatrixUtils.createRealMatrix(this.testSquare)).getB());
        this.checkBiDiagonal(new BiDiagonalTransformer(MatrixUtils.createRealMatrix(this.testNonSquare)).getB());
        this.checkBiDiagonal(new BiDiagonalTransformer(MatrixUtils.createRealMatrix(this.testNonSquare).transpose())
            .getB());
    }

    private void checkBiDiagonal(final RealMatrix m) {
        final int rows = m.getRowDimension();
        final int cols = m.getColumnDimension();
        for (int i = 0; i < rows; ++i) {
            for (int j = 0; j < cols; ++j) {
                if (rows < cols) {
                    if ((i < j) || (i > j + 1)) {
                        Assert.assertEquals(0, m.getEntry(i, j), 1.0e-16);
                    }
                } else {
                    if ((i < j - 1) || (i > j)) {
                        Assert.assertEquals(0, m.getEntry(i, j), 1.0e-16);
                    }
                }
            }
        }
    }

    @Test
    public void testSingularMatrix() {
        final BiDiagonalTransformer transformer =
            new BiDiagonalTransformer(MatrixUtils.createRealMatrix(new double[][] {
                { 1.0, 2.0, 3.0 },
                { 2.0, 3.0, 4.0 },
                { 3.0, 5.0, 7.0 }
            }));
        final double s3 = MathLib.sqrt(3.0);
        final double s14 = MathLib.sqrt(14.0);
        final double s1553 = MathLib.sqrt(1553.0);
        final RealMatrix uRef = MatrixUtils.createRealMatrix(new double[][] {
            { -1.0 / s14, 5.0 / (s3 * s14), 1.0 / s3 },
            { -2.0 / s14, -4.0 / (s3 * s14), 1.0 / s3 },
            { -3.0 / s14, 1.0 / (s3 * s14), -1.0 / s3 }
        });
        final RealMatrix bRef = MatrixUtils.createRealMatrix(new double[][] {
            { -s14, s1553 / s14, 0.0 },
            { 0.0, -87 * s3 / (s14 * s1553), -s3 * s14 / s1553 },
            { 0.0, 0.0, 0.0 }
        });
        final RealMatrix vRef = MatrixUtils.createRealMatrix(new double[][] {
            { 1.0, 0.0, 0.0 },
            { 0.0, -23 / s1553, 32 / s1553 },
            { 0.0, -32 / s1553, -23 / s1553 }
        });

        // check values against known references
        final RealMatrix u = transformer.getU();
        Assert.assertEquals(0, u.subtract(uRef).getNorm(), 1.0e-14);
        final RealMatrix b = transformer.getB();
        Assert.assertEquals(0, b.subtract(bRef).getNorm(), 1.0e-14);
        final RealMatrix v = transformer.getV();
        Assert.assertEquals(0, v.subtract(vRef).getNorm(), 1.0e-14);

        // check the same cached instance is returned the second time
        Assert.assertTrue(u == transformer.getU());
        Assert.assertTrue(b == transformer.getB());
        Assert.assertTrue(v == transformer.getV());

    }

    @Test
    public void testMatricesValues() {
        final BiDiagonalTransformer transformer =
            new BiDiagonalTransformer(MatrixUtils.createRealMatrix(this.testSquare));
        final double s17 = MathLib.sqrt(17.0);
        final RealMatrix uRef = MatrixUtils.createRealMatrix(new double[][] {
            { -8 / (5 * s17), 19 / (5 * s17) },
            { -19 / (5 * s17), -8 / (5 * s17) }
        });
        final RealMatrix bRef = MatrixUtils.createRealMatrix(new double[][] {
            { -3 * s17 / 5, 32 * s17 / 85 },
            { 0.0, -5 * s17 / 17 }
        });
        final RealMatrix vRef = MatrixUtils.createRealMatrix(new double[][] {
            { 1.0, 0.0 },
            { 0.0, -1.0 }
        });

        // check values against known references
        final RealMatrix u = transformer.getU();
        Assert.assertEquals(0, u.subtract(uRef).getNorm(), 1.0e-14);
        final RealMatrix b = transformer.getB();
        Assert.assertEquals(0, b.subtract(bRef).getNorm(), 1.0e-14);
        final RealMatrix v = transformer.getV();
        Assert.assertEquals(0, v.subtract(vRef).getNorm(), 1.0e-14);

        // check the same cached instance is returned the second time
        Assert.assertTrue(u == transformer.getU());
        Assert.assertTrue(b == transformer.getB());
        Assert.assertTrue(v == transformer.getV());

    }

    @Test
    public void testUpperOrLower() {
        Assert.assertTrue(new BiDiagonalTransformer(MatrixUtils.createRealMatrix(this.testSquare)).isUpperBiDiagonal());
        Assert.assertTrue(new BiDiagonalTransformer(MatrixUtils.createRealMatrix(this.testNonSquare))
            .isUpperBiDiagonal());
        Assert.assertFalse(new BiDiagonalTransformer(MatrixUtils.createRealMatrix(this.testNonSquare).transpose())
            .isUpperBiDiagonal());
    }

}
