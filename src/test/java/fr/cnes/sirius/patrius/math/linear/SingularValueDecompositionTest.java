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
 * VERSION:4.6:FA:FA-2542:27/01/2021:[PATRIUS] Definition d'un champ de vue avec demi-angle de 180° 
 * VERSION:4.5.1:FA:FA-2540:04/08/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear
 * VERSION:4.5:DM:DM-2300:27/05/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:306:19/11/2014: coverage
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.math.linear;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.exception.NumberIsTooLargeException;

public class SingularValueDecompositionTest {

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

    private static final double normTolerance = 10e-14;

    @Test
    public void testMoreRows() {
        final double[] singularValues = { 123.456, 2.3, 1.001, 0.999 };
        final int rows = singularValues.length + 2;
        final int columns = singularValues.length;
        final Random r = new Random(15338437322523l);
        final SingularValueDecomposition svd =
            new SingularValueDecomposition(this.createTestMatrix(r, rows, columns, singularValues));
        final double[] computedSV = svd.getSingularValues();
        Assert.assertEquals(singularValues.length, computedSV.length);
        for (int i = 0; i < singularValues.length; ++i) {
            Assert.assertEquals(singularValues[i], computedSV[i], 1.0e-10);
        }
    }

    @Test
    public void testMoreColumns() {
        final double[] singularValues = { 123.456, 2.3, 1.001, 0.999 };
        final int rows = singularValues.length;
        final int columns = singularValues.length + 2;
        final Random r = new Random(732763225836210l);
        final SingularValueDecomposition svd =
            new SingularValueDecomposition(this.createTestMatrix(r, rows, columns, singularValues));
        final double[] computedSV = svd.getSingularValues();
        Assert.assertEquals(singularValues.length, computedSV.length);
        for (int i = 0; i < singularValues.length; ++i) {
            Assert.assertEquals(singularValues[i], computedSV[i], 1.0e-10);
        }
    }

    /** test dimensions */
    @Test
    public void testDimensions() {
        final RealMatrix matrix = MatrixUtils.createRealMatrix(this.testSquare);
        final int m = matrix.getRowDimension();
        final int n = matrix.getColumnDimension();
        final SingularValueDecomposition svd = new SingularValueDecomposition(matrix);
        Assert.assertEquals(m, svd.getU().getRowDimension());
        Assert.assertEquals(m, svd.getU().getColumnDimension());
        Assert.assertEquals(m, svd.getS().getColumnDimension());
        Assert.assertEquals(n, svd.getS().getColumnDimension());
        Assert.assertEquals(n, svd.getV().getRowDimension());
        Assert.assertEquals(n, svd.getV().getColumnDimension());

    }

    /** Test based on a dimension 4 Hadamard matrix. */
    @Test
    public void testHadamard() {
        final RealMatrix matrix = new Array2DRowRealMatrix(new double[][] {
            { 15.0 / 2.0, 5.0 / 2.0, 9.0 / 2.0, 3.0 / 2.0 },
            { 5.0 / 2.0, 15.0 / 2.0, 3.0 / 2.0, 9.0 / 2.0 },
            { 9.0 / 2.0, 3.0 / 2.0, 15.0 / 2.0, 5.0 / 2.0 },
            { 3.0 / 2.0, 9.0 / 2.0, 5.0 / 2.0, 15.0 / 2.0 }
        }, false);
        final SingularValueDecomposition svd = new SingularValueDecomposition(matrix);
        Assert.assertEquals(16.0, svd.getSingularValues()[0], 1.0e-14);
        Assert.assertEquals(8.0, svd.getSingularValues()[1], 1.0e-14);
        Assert.assertEquals(4.0, svd.getSingularValues()[2], 1.0e-14);
        Assert.assertEquals(2.0, svd.getSingularValues()[3], 1.0e-14);

        final RealMatrix fullCovariance = new Array2DRowRealMatrix(new double[][] {
            { 85.0 / 1024, -51.0 / 1024, -75.0 / 1024, 45.0 / 1024 },
            { -51.0 / 1024, 85.0 / 1024, 45.0 / 1024, -75.0 / 1024 },
            { -75.0 / 1024, 45.0 / 1024, 85.0 / 1024, -51.0 / 1024 },
            { 45.0 / 1024, -75.0 / 1024, -51.0 / 1024, 85.0 / 1024 }
        }, false);
        Assert.assertEquals(0.0,
            fullCovariance.subtract(svd.getCovariance(0.0)).getNorm(),
            1.0e-14);

        final RealMatrix halfCovariance = new Array2DRowRealMatrix(new double[][] {
            { 5.0 / 1024, -3.0 / 1024, 5.0 / 1024, -3.0 / 1024 },
            { -3.0 / 1024, 5.0 / 1024, -3.0 / 1024, 5.0 / 1024 },
            { 5.0 / 1024, -3.0 / 1024, 5.0 / 1024, -3.0 / 1024 },
            { -3.0 / 1024, 5.0 / 1024, -3.0 / 1024, 5.0 / 1024 }
        }, false);
        Assert.assertEquals(0.0,
            halfCovariance.subtract(svd.getCovariance(6.0)).getNorm(),
            1.0e-14);

    }

    /** test A = USVt */
    @Test
    public void testAEqualUSVt() {
        this.checkAEqualUSVt(MatrixUtils.createRealMatrix(this.testSquare));
        this.checkAEqualUSVt(MatrixUtils.createRealMatrix(this.testNonSquare));
        this.checkAEqualUSVt(MatrixUtils.createRealMatrix(this.testNonSquare).transpose());
    }

    public void checkAEqualUSVt(final RealMatrix matrix) {
        final SingularValueDecomposition svd = new SingularValueDecomposition(matrix);
        final RealMatrix u = svd.getU();
        final RealMatrix s = svd.getS();
        final RealMatrix v = svd.getV();
        final double norm = u.multiply(s).multiply(v.transpose()).subtract(matrix).getNorm();
        Assert.assertEquals(0, norm, normTolerance);

    }

    /** test that U is orthogonal */
    @Test
    public void testUOrthogonal() {
        this.checkOrthogonal(new SingularValueDecomposition(MatrixUtils.createRealMatrix(this.testSquare)).getU());
        this.checkOrthogonal(new SingularValueDecomposition(MatrixUtils.createRealMatrix(this.testNonSquare)).getU());
        this.checkOrthogonal(new SingularValueDecomposition(MatrixUtils.createRealMatrix(this.testNonSquare)
            .transpose()).getU());
    }

    /** test that V is orthogonal */
    @Test
    public void testVOrthogonal() {
        this.checkOrthogonal(new SingularValueDecomposition(MatrixUtils.createRealMatrix(this.testSquare)).getV());
        this.checkOrthogonal(new SingularValueDecomposition(MatrixUtils.createRealMatrix(this.testNonSquare)).getV());
        this.checkOrthogonal(new SingularValueDecomposition(MatrixUtils.createRealMatrix(this.testNonSquare)
            .transpose()).getV());
    }

    public void checkOrthogonal(final RealMatrix m) {
        final RealMatrix mTm = m.transpose().multiply(m);
        final RealMatrix id = MatrixUtils.createRealIdentityMatrix(mTm.getRowDimension());
        Assert.assertEquals(0, mTm.subtract(id).getNorm(), normTolerance);
    }

    /** test matrices values */
    // This test is useless since whereas the columns of U and V are linked
    // together, the actual triplet (U,S,V) is not uniquely defined.
    public void testMatricesValues1() {
        final SingularValueDecomposition svd =
            new SingularValueDecomposition(MatrixUtils.createRealMatrix(this.testSquare));
        final RealMatrix uRef = MatrixUtils.createRealMatrix(new double[][] {
            { 3.0 / 5.0, -4.0 / 5.0 },
            { 4.0 / 5.0, 3.0 / 5.0 }
        });
        final RealMatrix sRef = MatrixUtils.createRealMatrix(new double[][] {
            { 3.0, 0.0 },
            { 0.0, 1.0 }
        });
        final RealMatrix vRef = MatrixUtils.createRealMatrix(new double[][] {
            { 4.0 / 5.0, 3.0 / 5.0 },
            { 3.0 / 5.0, -4.0 / 5.0 }
        });

        // check values against known references
        final RealMatrix u = svd.getU();
        Assert.assertEquals(0, u.subtract(uRef).getNorm(), normTolerance);
        final RealMatrix s = svd.getS();
        Assert.assertEquals(0, s.subtract(sRef).getNorm(), normTolerance);
        final RealMatrix v = svd.getV();
        Assert.assertEquals(0, v.subtract(vRef).getNorm(), normTolerance);

        // check the same cached instance is returned the second time
        Assert.assertTrue(u == svd.getU());
        Assert.assertTrue(s == svd.getS());
        Assert.assertTrue(v == svd.getV());

    }

    /** test matrices values */
    // This test is useless since whereas the columns of U and V are linked
    // together, the actual triplet (U,S,V) is not uniquely defined.
    public void useless_testMatricesValues2() {

        final RealMatrix uRef = MatrixUtils.createRealMatrix(new double[][] {
            { 0.0 / 5.0, 3.0 / 5.0, 0.0 / 5.0 },
            { -4.0 / 5.0, 0.0 / 5.0, -3.0 / 5.0 },
            { 0.0 / 5.0, 4.0 / 5.0, 0.0 / 5.0 },
            { -3.0 / 5.0, 0.0 / 5.0, 4.0 / 5.0 }
        });
        final RealMatrix sRef = MatrixUtils.createRealMatrix(new double[][] {
            { 4.0, 0.0, 0.0 },
            { 0.0, 3.0, 0.0 },
            { 0.0, 0.0, 2.0 }
        });
        final RealMatrix vRef = MatrixUtils.createRealMatrix(new double[][] {
            { 80.0 / 125.0, -60.0 / 125.0, 75.0 / 125.0 },
            { 24.0 / 125.0, 107.0 / 125.0, 60.0 / 125.0 },
            { -93.0 / 125.0, -24.0 / 125.0, 80.0 / 125.0 }
        });

        // check values against known references
        final SingularValueDecomposition svd =
            new SingularValueDecomposition(MatrixUtils.createRealMatrix(this.testNonSquare));
        final RealMatrix u = svd.getU();
        Assert.assertEquals(0, u.subtract(uRef).getNorm(), normTolerance);
        final RealMatrix s = svd.getS();
        Assert.assertEquals(0, s.subtract(sRef).getNorm(), normTolerance);
        final RealMatrix v = svd.getV();
        Assert.assertEquals(0, v.subtract(vRef).getNorm(), normTolerance);

        // check the same cached instance is returned the second time
        Assert.assertTrue(u == svd.getU());
        Assert.assertTrue(s == svd.getS());
        Assert.assertTrue(v == svd.getV());

    }

    /**
     * For coverage purposes, tests the if (dimension == 0) of method getCovariance
     */
    @Test(expected = NumberIsTooLargeException.class)
    public void testNumberIsTooLargeExceptionGetCovariance() {
        final double[][] d = { { 1, 1, 1 }, { 0, 0, 0 }, { 1, 2, 3 } };
        final RealMatrix m = new Array2DRowRealMatrix(d);
        final SingularValueDecomposition svd = new SingularValueDecomposition(m);
        // its singular values are 0, sqrt( (17+sqrt(17*17-4*6))/2) = 4.0791433 and
        // sqrt( (17 - sqrt(17*17-4*6) ) / 2 ) = 0.6004912
        svd.getCovariance(5);
    }

    /** test MATH-465 */
    @Test
    public void testRank() {
        final double[][] d = { { 1, 1, 1 }, { 0, 0, 0 }, { 1, 2, 3 } };
        final RealMatrix m = new Array2DRowRealMatrix(d);
        final SingularValueDecomposition svd = new SingularValueDecomposition(m);
        Assert.assertEquals(2, svd.getRank());
    }

    /** test MATH-583 */
    @Test
    public void testStability1() {
        final RealMatrix m = new Array2DRowRealMatrix(201, 201);
        this.loadRealMatrix(m, "matrix1.csv");
        try {
            new SingularValueDecomposition(m);
        } catch (final Exception e) {
            Assert.fail("Exception whilst constructing SVD");
        }
    }

    /** test MATH-327 */
    @Test
    public void testStability2() {
        final RealMatrix m = new Array2DRowRealMatrix(7, 168);
        this.loadRealMatrix(m, "matrix2.csv");
        try {
            new SingularValueDecomposition(m);
        } catch (final Throwable e) {
            Assert.fail("Exception whilst constructing SVD");
        }
    }

    private void loadRealMatrix(final RealMatrix m, final String resourceName) {
        try {
            final DataInputStream in = new DataInputStream(this.getClass().getResourceAsStream(resourceName));
            final BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String strLine;
            int row = 0;
            while ((strLine = br.readLine()) != null) {
                int col = 0;
                for (final String entry : strLine.split(",")) {
                    m.setEntry(row, col++, Double.parseDouble(entry));
                }
                row++;
            }
            in.close();
        } catch (final IOException e) {
        }
    }

    /** test condition number */
    @Test
    public void testConditionNumber() {
        final SingularValueDecomposition svd =
            new SingularValueDecomposition(MatrixUtils.createRealMatrix(this.testSquare));
        // replace 1.0e-15 with 1.5e-15
        Assert.assertEquals(3.0, svd.getConditionNumber(), 1.5e-15);
    }

    @Test
    public void testInverseConditionNumber() {
        final SingularValueDecomposition svd =
            new SingularValueDecomposition(MatrixUtils.createRealMatrix(this.testSquare));
        Assert.assertEquals(1.0 / 3.0, svd.getInverseConditionNumber(), 1.5e-15);
    }

    private RealMatrix createTestMatrix(final Random r, final int rows, final int columns,
                                        final double[] singularValues) {
        final RealMatrix u = EigenDecompositionTest.createOrthogonalMatrix(r, rows);
        final RealMatrix d = new Array2DRowRealMatrix(rows, columns);
        d.setSubMatrix(MatrixUtils.createRealDiagonalMatrix(singularValues).getData(false), 0, 0);
        final RealMatrix v = EigenDecompositionTest.createOrthogonalMatrix(r, columns);
        return u.multiply(d).multiply(v);
    }
    
    /** test getInvert and decompositionBuilder methods */
    @Test
    public void testInverse() {
        final RealMatrix m = MatrixUtils.createRealMatrix(this.testSquare);
        RealMatrix inv = new SingularValueDecomposition(m).getSolver().getInverse();
        Assert.assertTrue(inv.equals(m.getInverse(SingularValueDecomposition.decompositionBuilder())));
    }
}
