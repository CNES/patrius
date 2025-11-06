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
 * VERSION:4.15:OPENFD-275:21/11/2024:[PATRIUS] Optimisation de la decomposition QR
 * VERSION:4.15:OPENFD-385:21/11/2024:Execution en parallele des tests concernant EclipticJ2000Provider
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.7:DM:DM-2766:18/05/2021:Evol. et corr. dans le package fr.cnes.sirius.patrius.math.linear (suite DM 2300) 
 * VERSION:4.6:FA:FA-2542:27/01/2021:[PATRIUS] Definition d'un champ de vue avec demi-angle de 180° 
 * VERSION:4.5.1:FA:FA-2540:04/08/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear
 * VERSION:4.5:DM:DM-2300:27/05/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.linear;

import static org.junit.Assert.assertTrue;
import fr.cnes.sirius.patrius.Utils;

import java.util.Random;
import fr.cnes.sirius.patrius.Utils;

import org.junit.Assert;
import fr.cnes.sirius.patrius.Utils;
import org.junit.Before;
import fr.cnes.sirius.patrius.Utils;
import org.junit.Test;
import fr.cnes.sirius.patrius.Utils;

public class QRDecompositionTest {
    private final double[][] testData3x3NonSingular = {
        { 12, -51, 4 },
        { 6, 167, -68 },
        { -4, 24, -41 }, };

    private final double[][] testData3x3Singular = {
        { 1, 4, 7, },
        { 2, 5, 8, },
        { 3, 6, 9, }, };

    private final double[][] testData3x4 = {
        { 12, -51, 4, 1 },
        { 6, 167, -68, 2 },
        { -4, 24, -41, 3 }, };

    private final double[][] testData4x3 = {
        { 12, -51, 4, },
        { 6, 167, -68, },
        { -4, 24, -41, },
        { -5, 34, 7, }, };

    private static final double entryTolerance = 10e-16;

    private static final double normTolerance = 10e-14;

    /** test dimensions */
    @Test
    public void testDimensions() {
        this.checkDimension(MatrixUtils.createRealMatrix(this.testData3x3NonSingular));

        this.checkDimension(MatrixUtils.createRealMatrix(this.testData4x3));

        this.checkDimension(MatrixUtils.createRealMatrix(this.testData3x4));

        final Random r = new Random(643895747384642l);
        final int p = (5 * BlockRealMatrix.BLOCK_SIZE) / 4;
        final int q = (7 * BlockRealMatrix.BLOCK_SIZE) / 4;
        this.checkDimension(this.createTestMatrix(r, p, q));
        this.checkDimension(this.createTestMatrix(r, q, p));

    }

    /** Test the constructors */
    @Test
    public void testConstructors() {

        final RealMatrix matrix = MatrixUtils.createRealMatrix(this.testData3x3NonSingular);

        final QRDecomposition qr1 = new QRDecomposition(matrix);
        final QRDecomposition qr2 = new QRDecomposition(matrix, 0d);

        Assert.assertTrue(qr1.getR().equals(qr2.getR()));
    }

    private void checkDimension(final RealMatrix m) {
        final int rows = m.getRowDimension();
        final int columns = m.getColumnDimension();
        final QRDecomposition qr = new QRDecomposition(m);
        Assert.assertEquals(rows, qr.getQ().getRowDimension());
        Assert.assertEquals(rows, qr.getQ().getColumnDimension());
        Assert.assertEquals(rows, qr.getR().getRowDimension());
        Assert.assertEquals(columns, qr.getR().getColumnDimension());
    }

    /** test A = QR */
    @Test
    public void testAEqualQR() {
        this.checkAEqualQR(MatrixUtils.createRealMatrix(this.testData3x3NonSingular));

        this.checkAEqualQR(MatrixUtils.createRealMatrix(this.testData3x3Singular));

        this.checkAEqualQR(MatrixUtils.createRealMatrix(this.testData3x4));

        this.checkAEqualQR(MatrixUtils.createRealMatrix(this.testData4x3));

        final Random r = new Random(643895747384642l);
        final int p = (5 * BlockRealMatrix.BLOCK_SIZE) / 4;
        final int q = (7 * BlockRealMatrix.BLOCK_SIZE) / 4;
        this.checkAEqualQR(this.createTestMatrix(r, p, q));

        this.checkAEqualQR(this.createTestMatrix(r, q, p));

    }

    private void checkAEqualQR(final RealMatrix m) {
        final QRDecomposition qr = new QRDecomposition(m);
        final double norm = qr.getQ().multiply(qr.getR()).subtract(m).getNorm();
        Assert.assertEquals(0, norm, normTolerance);
    }

    /** test the orthogonality of Q */
    @Test
    public void testQOrthogonal() {
        this.checkQOrthogonal(MatrixUtils.createRealMatrix(this.testData3x3NonSingular));

        this.checkQOrthogonal(MatrixUtils.createRealMatrix(this.testData3x3Singular));

        this.checkQOrthogonal(MatrixUtils.createRealMatrix(this.testData3x4));

        this.checkQOrthogonal(MatrixUtils.createRealMatrix(this.testData4x3));

        final Random r = new Random(643895747384642l);
        final int p = (5 * BlockRealMatrix.BLOCK_SIZE) / 4;
        final int q = (7 * BlockRealMatrix.BLOCK_SIZE) / 4;
        this.checkQOrthogonal(this.createTestMatrix(r, p, q));

        this.checkQOrthogonal(this.createTestMatrix(r, q, p));

    }

    private void checkQOrthogonal(final RealMatrix m) {
        final QRDecomposition qr = new QRDecomposition(m);
        final RealMatrix eye = MatrixUtils.createRealIdentityMatrix(m.getRowDimension());
        final double norm = qr.getQT().multiply(qr.getQ()).subtract(eye).getNorm();
        Assert.assertEquals(0, norm, normTolerance);
    }

    /** test that R is express in its compact form (n * n) */
    @Test
    public void testRCompactForm() {

        final RealMatrix matrix = MatrixUtils.createRealMatrix(this.testData4x3);
        final RealMatrix compactR = new QRDecomposition(matrix).getR(true);
        final RealMatrix fullR = new QRDecomposition(matrix).getR();
        final int dim = 3;

        Assert.assertEquals(dim, compactR.getRowDimension(), 0);
        Assert.assertEquals(dim, compactR.getColumnDimension(), 0);
        Assert.assertTrue(fullR.getSubMatrix(0, dim - 1, 0, dim - 1).equals(compactR));
    }

    /** test that R is upper triangular */
    @Test
    public void testRUpperTriangular() {
        RealMatrix matrix = MatrixUtils.createRealMatrix(this.testData3x3NonSingular);
        this.checkUpperTriangular(new QRDecomposition(matrix).getR());

        matrix = MatrixUtils.createRealMatrix(this.testData3x3Singular);
        this.checkUpperTriangular(new QRDecomposition(matrix).getR());

        matrix = MatrixUtils.createRealMatrix(this.testData3x4);
        this.checkUpperTriangular(new QRDecomposition(matrix).getR());

        matrix = MatrixUtils.createRealMatrix(this.testData4x3);
        this.checkUpperTriangular(new QRDecomposition(matrix).getR());

        final Random r = new Random(643895747384642l);
        final int p = (5 * BlockRealMatrix.BLOCK_SIZE) / 4;
        final int q = (7 * BlockRealMatrix.BLOCK_SIZE) / 4;
        matrix = this.createTestMatrix(r, p, q);
        this.checkUpperTriangular(new QRDecomposition(matrix).getR());

        matrix = this.createTestMatrix(r, p, q);
        this.checkUpperTriangular(new QRDecomposition(matrix).getR());

    }

    private void checkUpperTriangular(final RealMatrix m) {
        m.walkInOptimizedOrder(new DefaultRealMatrixPreservingVisitor(){
            @Override
            public void visit(final int row, final int column, final double value) {
                if (column < row) {
                    Assert.assertEquals(0.0, value, entryTolerance);
                }
            }
        });
    }

    /** test that H is trapezoidal */
    @Test
    public void testHTrapezoidal() {
        RealMatrix matrix = MatrixUtils.createRealMatrix(this.testData3x3NonSingular);
        this.checkTrapezoidal(new QRDecomposition(matrix).getH());

        matrix = MatrixUtils.createRealMatrix(this.testData3x3Singular);
        this.checkTrapezoidal(new QRDecomposition(matrix).getH());

        matrix = MatrixUtils.createRealMatrix(this.testData3x4);
        this.checkTrapezoidal(new QRDecomposition(matrix).getH());

        matrix = MatrixUtils.createRealMatrix(this.testData4x3);
        this.checkTrapezoidal(new QRDecomposition(matrix).getH());

        final Random r = new Random(643895747384642l);
        final int p = (5 * BlockRealMatrix.BLOCK_SIZE) / 4;
        final int q = (7 * BlockRealMatrix.BLOCK_SIZE) / 4;
        matrix = this.createTestMatrix(r, p, q);
        this.checkTrapezoidal(new QRDecomposition(matrix).getH());

        matrix = this.createTestMatrix(r, p, q);
        this.checkTrapezoidal(new QRDecomposition(matrix).getH());

    }

    private void checkTrapezoidal(final RealMatrix m) {
        m.walkInOptimizedOrder(new DefaultRealMatrixPreservingVisitor(){
            @Override
            public void visit(final int row, final int column, final double value) {
                if (column > row) {
                    Assert.assertEquals(0.0, value, entryTolerance);
                }
            }
        });
    }

    /** test matrices values */
    @Test
    public void testMatricesValues() {
        final QRDecomposition qr =
            new QRDecomposition(MatrixUtils.createRealMatrix(this.testData3x3NonSingular));
        final RealMatrix qRef = MatrixUtils.createRealMatrix(new double[][] {
            { -12.0 / 14.0, 69.0 / 175.0, -58.0 / 175.0 },
            { -6.0 / 14.0, -158.0 / 175.0, 6.0 / 175.0 },
            { 4.0 / 14.0, -30.0 / 175.0, -165.0 / 175.0 }
        });
        final RealMatrix rRef = MatrixUtils.createRealMatrix(new double[][] {
            { -14.0, -21.0, 14.0 },
            { 0.0, -175.0, 70.0 },
            { 0.0, 0.0, 35.0 }
        });
        final RealMatrix hRef = MatrixUtils.createRealMatrix(new double[][] {
            { 26.0 / 14.0, 0.0, 0.0 },
            { 6.0 / 14.0, 648.0 / 325.0, 0.0 },
            { -4.0 / 14.0, 36.0 / 325.0, 2.0 }
        });

        // check values against known references
        final RealMatrix q = qr.getQ();
        Assert.assertEquals(0, q.subtract(qRef).getNorm(), 1.0e-13);
        final RealMatrix qT = qr.getQT();
        Assert.assertEquals(0, qT.subtract(qRef.transpose()).getNorm(), 1.0e-13);
        final RealMatrix r = qr.getR();
        Assert.assertEquals(0, r.subtract(rRef).getNorm(), 1.0e-13);
        final RealMatrix h = qr.getH();
        Assert.assertEquals(0, h.subtract(hRef).getNorm(), 1.0e-13);

    }

    @Test(expected = SingularMatrixException.class)
    public void testNonInvertible() {
        final QRDecomposition qr =
            new QRDecomposition(MatrixUtils.createRealMatrix(this.testData3x3Singular));

        qr.getSolver().getInverse();
    }

    private RealMatrix createTestMatrix(final Random r, final int rows, final int columns) {
        final RealMatrix m = MatrixUtils.createRealMatrix(rows, columns);
        m.walkInOptimizedOrder(new DefaultRealMatrixChangingVisitor(){
            @Override
            public double visit(final int row, final int column, final double value) {
                return 2.0 * r.nextDouble() - 1.0;
            }
        });
        return m;
    }
    
    /** test getInvert and decompositionBuilder methods */
    @Test
    public void testInverse() {
        final RealMatrix matrix = MatrixUtils.createRealMatrix(this.testData3x3NonSingular);
        final RealMatrix inv = new QRDecomposition(matrix).getSolver().getInverse();
        Assert.assertTrue(inv.equals(matrix.getInverse(QRDecomposition.decompositionBuilder(1.0e-15))));
    }

    /**
     * This test verifies that the obtained results with parallel computing and optimizations added for
     * sparse matrix are consistent with the results generated by the former architecture.
     */
    @Test
    public void testBandMatrix() {
        final RealMatrix bandMatrix = MatrixUtils.createRealMatrix(new double[][] {
            { 26.0, 5.0, 0.0, 0.0 },
            { 6.0, 68.0, 3.0, 0.0 },
            { 0.0, 36.0, 2.0, 0.0 },
            { 0.0, 0.0, 1.0, 0.0 }
        });

        final QRDecomposition decomp = new QRDecomposition(bandMatrix, entryTolerance, true);

        final QRDecomposition decomp1 = new QRDecomposition(bandMatrix, entryTolerance, false);

        assertTrue(decomp.getR().equals(decomp1.getR()));
        assertTrue(decomp.getQ().equals(decomp1.getQ()));
        assertTrue(decomp.getQT().equals(decomp1.getQT()));

        this.checkAEqualQR(bandMatrix);
        this.checkAEqualQRParallel(bandMatrix);

    }

    private void checkAEqualQRParallel(final RealMatrix m) {
        final QRDecomposition qr = new QRDecomposition(m, entryTolerance, true);
        final double norm = qr.getQ().multiply(qr.getR()).subtract(m).getNorm();
        Assert.assertEquals(0, norm, normTolerance);
    }


    @Before
    public void setUp() {
        Utils.clear();
    }
}
