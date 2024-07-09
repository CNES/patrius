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
 * VERSION:4.5:DM:DM-2300:27/05/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:306:14/11/2014: coverage
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.linear;

import java.util.Arrays;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.exception.NoDataException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;
import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;
import fr.cnes.sirius.patrius.math.linear.RealVectorAbstractTest.RealVectorTestImpl;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;

/**
 * Test cases for the {@link BlockRealMatrix} class.
 * 
 * @version $Id: BlockRealMatrixTest.java 18108 2017-10-04 06:45:27Z bignon $
 */

public final class BlockRealMatrixTest {

    /** Precision . */
    protected final double eps = Precision.DOUBLE_COMPARISON_EPSILON;

    // 3 x 3 identity matrix
    protected double[][] id = { { 1d, 0d, 0d }, { 0d, 1d, 0d }, { 0d, 0d, 1d } };

    // Test data for group operations
    protected double[][] testData = { { 1d, 2d, 3d }, { 2d, 5d, 3d }, { 1d, 0d, 8d } };
    protected double[][] testDataLU = { { 2d, 5d, 3d }, { .5d, -2.5d, 6.5d }, { 0.5d, 0.2d, .2d } };
    protected double[][] testDataPlus2 = { { 3d, 4d, 5d }, { 4d, 7d, 5d }, { 3d, 2d, 10d } };
    protected double[][] testDataMinus = { { -1d, -2d, -3d }, { -2d, -5d, -3d },
        { -1d, 0d, -8d } };
    protected double[] testDataRow1 = { 1d, 2d, 3d };
    protected double[] testDataCol3 = { 3d, 3d, 8d };
    protected double[][] testDataInv =
    { { -40d, 16d, 9d }, { 13d, -5d, -3d }, { 5d, -2d, -1d } };
    protected double[] preMultTest = { 8, 12, 33 };
    protected double[][] testData2 = { { 1d, 2d, 3d }, { 2d, 5d, 3d } };
    protected double[][] testData2T = { { 1d, 2d }, { 2d, 5d }, { 3d, 3d } };
    protected double[][] testDataPlusInv =
    { { -39d, 18d, 12d }, { 15d, 0d, 0d }, { 6d, -2d, 7d } };

    // lu decomposition tests
    protected double[][] luData = { { 2d, 3d, 3d }, { 0d, 5d, 7d }, { 6d, 9d, 8d } };
    protected double[][] luDataLUDecomposition = { { 6d, 9d, 8d }, { 0d, 5d, 7d },
        { 0.33333333333333, 0d, 0.33333333333333 } };

    // singular matrices
    protected double[][] singular = { { 2d, 3d }, { 2d, 3d } };
    protected double[][] bigSingular = { { 1d, 2d, 3d, 4d }, { 2d, 5d, 3d, 4d },
        { 7d, 3d, 256d, 1930d }, { 3d, 7d, 6d, 8d } }; // 4th row = 1st + 2nd
    protected double[][] detData = { { 1d, 2d, 3d }, { 4d, 5d, 6d }, { 7d, 8d, 10d } };
    protected double[][] detData2 = { { 1d, 3d }, { 2d, 4d } };

    // vectors
    protected double[] testVector = { 1, 2, 3 };
    protected double[] testVector2 = { 1, 2, 3, 4 };

    // submatrix accessor tests
    protected double[][] subTestData = { { 1, 2, 3, 4 }, { 1.5, 2.5, 3.5, 4.5 },
        { 2, 4, 6, 8 }, { 4, 5, 6, 7 } };
    // array selections
    protected double[][] subRows02Cols13 = { { 2, 4 }, { 4, 8 } };
    protected double[][] subRows03Cols12 = { { 2, 3 }, { 5, 6 } };
    protected double[][] subRows03Cols123 = { { 2, 3, 4 }, { 5, 6, 7 } };
    // effective permutations
    protected double[][] subRows20Cols123 = { { 4, 6, 8 }, { 2, 3, 4 } };
    protected double[][] subRows31Cols31 = { { 7, 5 }, { 4.5, 2.5 } };
    // contiguous ranges
    protected double[][] subRows01Cols23 = { { 3, 4 }, { 3.5, 4.5 } };
    protected double[][] subRows23Cols00 = { { 2 }, { 4 } };
    protected double[][] subRows00Cols33 = { { 4 } };
    // row matrices
    protected double[][] subRow0 = { { 1, 2, 3, 4 } };
    protected double[][] subRow3 = { { 4, 5, 6, 7 } };
    // column matrices
    protected double[][] subColumn1 = { { 2 }, { 2.5 }, { 4 }, { 5 } };
    protected double[][] subColumn3 = { { 4 }, { 4.5 }, { 8 }, { 7 } };

    // tolerances
    protected double entryTolerance = 10E-16;
    protected double normTolerance = 10E-14;

    /**
     * For coverage purposes, tests the loops in the third
     * constructor : BlockRealMatrix(final int rows, final int columns,
     * final double[][] blockData, final boolean copyArray)
     */
    @Test
    public void testSpecificCasesConstructor() {
        int rows = 2;
        int columns = 2;
        final double[][] blockData = { { 1, 2 }, { 3, 4 } };
        final boolean copyArray = true;
        try {
            new BlockRealMatrix(rows, columns, blockData, copyArray);
        } catch (final DimensionMismatchException e) {
            // expected behavior
        }

        rows = 2;
        columns = 1;
        final BlockRealMatrix matrix = new BlockRealMatrix(rows, columns, blockData, copyArray);
        final double[][] data = matrix.getData(false);
        Assert.assertEquals(data[0][0], 1.0, this.eps);
        Assert.assertEquals(data[1][0], 2.0, this.eps);
    }

    /**
     * For coverage purposes, tests the if (length != columns)
     * in method toBlocksLayout
     */
    @Test(expected = DimensionMismatchException.class)
    public void testExceptionToBlocksLayout() {
        final double[][] rawData = new double[2][2];
        rawData[0] = new double[2];
        rawData[1] = new double[3];
        BlockRealMatrix.toBlocksLayout(rawData);
    }

    /**
     * For coverage purposes, tests the catch block
     * in method add, setRowVector(final int row, final RealVector vector) and
     * setColumnVector(final int column, final RealVector vector)
     * The test required to add a RealMatrix that is not a BlockRealMatrix
     * or a RealVector that is not an ArrayRealVector
     * 
     */
    @Test
    public void testExceptionAddSetRowColumnVector() {
        final double[][] blockData = { { 1, 2, 3 }, { 3, 4, 5 }, { 3, 4, 5 } };
        final Array2DRowRealMatrix matrix2 = new Array2DRowRealMatrix(blockData);
        final BlockRealMatrix matrix = new BlockRealMatrix(blockData);
        final BlockRealMatrix result = matrix.add(matrix2);

        for (int i = 0; i < blockData.length; i++) {
            for (int j = 0; j < blockData[i].length; j++) {
                Assert.assertEquals(result.getEntry(i, j), blockData[i][j] * 2, this.eps);
            }
        }

        final double[] vecData = blockData[0];
        final RealVectorTestImpl vec2 = new RealVectorTestImpl(vecData);
        matrix.setRowVector(1, vec2);
        final double[][] blockDataRefRow = { { 1, 2, 3 }, { 1, 2, 3 }, { 3, 4, 5 } };
        for (int i = 0; i < vecData.length; i++) {
            for (int j = 0; j < blockData[i].length; j++) {
                Assert.assertEquals(matrix.getEntry(i, j), blockDataRefRow[i][j], this.eps);
            }
        }
        matrix.setColumnVector(1, vec2);
        final double[][] blockDataRefColumn = { { 1, 1, 3 }, { 1, 2, 3 }, { 3, 3, 5 } };
        for (int i = 0; i < vecData.length; i++) {
            for (int j = 0; j < blockData[i].length; j++) {
                Assert.assertEquals(matrix.getEntry(i, j), blockDataRefColumn[i][j], this.eps);
            }
        }

    }

    /** test dimensions */
    @Test
    public void testDimensions() {
        final BlockRealMatrix m = new BlockRealMatrix(this.testData);
        final BlockRealMatrix m2 = new BlockRealMatrix(this.testData2);
        Assert.assertEquals("testData row dimension", 3, m.getRowDimension());
        Assert.assertEquals("testData column dimension", 3, m.getColumnDimension());
        Assert.assertTrue("testData is square", m.isSquare());
        Assert.assertEquals("testData2 row dimension", m2.getRowDimension(), 2);
        Assert.assertEquals("testData2 column dimension", m2.getColumnDimension(), 3);
        Assert.assertTrue("testData2 is not square", !m2.isSquare());
    }

    /** test copy functions */
    @Test
    public void testCopyFunctions() {
        final Random r = new Random(66636328996002l);
        final BlockRealMatrix m1 = this.createRandomMatrix(r, 47, 83);
        final BlockRealMatrix m2 = new BlockRealMatrix(m1.getData(false));
        Assert.assertEquals(m1, m2);
        final BlockRealMatrix m3 = new BlockRealMatrix(this.testData);
        final BlockRealMatrix m4 = new BlockRealMatrix(m3.getData());
        Assert.assertEquals(m3, m4);
    }

    /** test add */
    @Test
    public void testAdd() {
        final BlockRealMatrix m = new BlockRealMatrix(this.testData);
        final BlockRealMatrix mInv = new BlockRealMatrix(this.testDataInv);
        final RealMatrix mPlusMInv = m.add(mInv);

        Assert.assertTrue(m.equals(new BlockRealMatrix(this.testData)));
        Assert.assertTrue(mInv.equals(new BlockRealMatrix(this.testDataInv)));

        final double[][] sumEntries = mPlusMInv.getData(false);
        for (int row = 0; row < m.getRowDimension(); row++) {
            for (int col = 0; col < m.getColumnDimension(); col++) {
                Assert.assertEquals("sum entry entry",
                    this.testDataPlusInv[row][col], sumEntries[row][col],
                    this.entryTolerance);
            }
        }
        
        // Add to entry
        m.addToEntry(0, 0, 10.);
        Assert.assertEquals(testData[0][0] + 10, m.getEntry(0, 0), 0.);
    }

    /** test add failure */
    @Test
    public void testAddFail() {
        final BlockRealMatrix m = new BlockRealMatrix(this.testData);
        final BlockRealMatrix m2 = new BlockRealMatrix(this.testData2);
        try {
            m.add(m2);
            Assert.fail("MathIllegalArgumentException expected");
        } catch (final MathIllegalArgumentException ex) {
            // ignored
        }
    }

    /** test norm */
    @Test
    public void testNorm() {
        final BlockRealMatrix m = new BlockRealMatrix(this.testData);
        final BlockRealMatrix m2 = new BlockRealMatrix(this.testData2);
        Assert.assertEquals("testData norm", 14d, m.getNorm(), this.entryTolerance);
        Assert.assertEquals("testData2 norm", 7d, m2.getNorm(), this.entryTolerance);
    }

    /** test Frobenius norm */
    @Test
    public void testFrobeniusNorm() {
        final BlockRealMatrix m = new BlockRealMatrix(this.testData);
        final BlockRealMatrix m2 = new BlockRealMatrix(this.testData2);
        Assert.assertEquals("testData Frobenius norm", MathLib.sqrt(117.0), m.getFrobeniusNorm(), this.entryTolerance);
        Assert.assertEquals("testData2 Frobenius norm", MathLib.sqrt(52.0), m2.getFrobeniusNorm(), this.entryTolerance);
    }

    /** test m-n = m + -n */
    @Test
    public void testPlusMinus() {
        final double[][] blockData = { { 1, 2, 3 }, { 3, 4, 5 }, { 3, 4, 5 } };
        final Array2DRowRealMatrix matrix2 = new Array2DRowRealMatrix(blockData);

        final BlockRealMatrix m = new BlockRealMatrix(this.testData);
        final BlockRealMatrix m2 = new BlockRealMatrix(this.testDataInv);
        final Array2DRowRealMatrix matrix3 = new Array2DRowRealMatrix(blockData);
        this.assertClose(m.subtract(m2), m2.scalarMultiply(-1d).add(m), this.entryTolerance);
        try {
            m.subtract(new BlockRealMatrix(this.testData2));
            Assert.fail("Expecting illegalArgumentException");
        } catch (final MathIllegalArgumentException ex) {
            // ignored
        }

        final BlockRealMatrix result = m.subtract(matrix2);
        for (int i = 0; i < blockData.length; i++) {
            for (int j = 0; j < blockData[i].length; j++) {
                Assert.assertEquals(testData[i][j] - blockData[i][j], result.getEntry(i, j), this.eps);
            }
        }

        Assert.assertTrue(m.equals(new BlockRealMatrix(this.testData)));
        Assert.assertTrue(m2.equals(new BlockRealMatrix(this.testDataInv)));
    }

    /** test multiply */
    @Test
    public void testMultiply() {
        final BlockRealMatrix m = new BlockRealMatrix(this.testData);
        final BlockRealMatrix mInv = new BlockRealMatrix(this.testDataInv);
        final BlockRealMatrix identity = new BlockRealMatrix(this.id);
        final BlockRealMatrix m2 = new BlockRealMatrix(this.testData2);
        this.assertClose(m.multiply(mInv), identity, this.entryTolerance);
        this.assertClose(mInv.multiply(m), identity, this.entryTolerance);
        this.assertClose(m.multiply(identity), m, this.entryTolerance);
        this.assertClose(identity.multiply(mInv), mInv, this.entryTolerance);
        this.assertClose(m2.multiply(identity), m2, this.entryTolerance);
        try {
            m.multiply(new BlockRealMatrix(this.bigSingular));
            Assert.fail("Expecting illegalArgumentException");
        } catch (final MathIllegalArgumentException ex) {
            // expected
        }

        Assert.assertTrue(m.equals(new BlockRealMatrix(this.testData)));
        Assert.assertTrue(mInv.equals(new BlockRealMatrix(this.testDataInv)));
        Assert.assertTrue(identity.equals(new BlockRealMatrix(this.id)));
        Assert.assertTrue(m2.equals(new BlockRealMatrix(this.testData2)));

        Assert.assertTrue(m.multiply(mInv, false).equals(identity));
        Assert.assertTrue(m.multiply(mInv, false, 2.).equals(identity.scalarMultiply(2.)));

        final double[][] resTranspose = { { 14d, 21d, 25d }, { 21d, 38d, 26d }, { 25d, 26d, 65d } };
        final BlockRealMatrix mResTranspose = new BlockRealMatrix(resTranspose);

        Assert.assertTrue(m.multiply(m, true).equals(mResTranspose));

        final Array2DRowRealMatrix array = new Array2DRowRealMatrix(this.testData);
        Assert.assertTrue(m.multiply(array, true).equals(mResTranspose));
        Assert.assertTrue(m.multiply(array, true, 2.).equals(mResTranspose.scalarMultiply(2.)));
        
        // Exception
        try {
			new BlockRealMatrix(3, 3).multiply(new BlockRealMatrix(4, 4), true, 1);
			Assert.fail();
        } catch (final DimensionMismatchException e) {
        	// Expected
        	Assert.assertTrue(true);
        }
        
        // Multiple entry
        m.multiplyEntry(0, 0, 10);
        Assert.assertEquals(testData[0][0] * 10, m.getEntry(0, 0), 0.);
    }

    @Test
    public void testSeveralBlocks() {
        final RealMatrix m = new BlockRealMatrix(35, 71);
        for (int i = 0; i < m.getRowDimension(); ++i) {
            for (int j = 0; j < m.getColumnDimension(); ++j) {
                m.setEntry(i, j, i + j / 1024.0);
            }
        }

        final RealMatrix mT = m.transpose();
        Assert.assertEquals(m.getRowDimension(), mT.getColumnDimension());
        Assert.assertEquals(m.getColumnDimension(), mT.getRowDimension());
        for (int i = 0; i < mT.getRowDimension(); ++i) {
            for (int j = 0; j < mT.getColumnDimension(); ++j) {
                Assert.assertEquals(m.getEntry(j, i), mT.getEntry(i, j), 0);
            }
        }

        final RealMatrix mPm = m.add(m);
        for (int i = 0; i < mPm.getRowDimension(); ++i) {
            for (int j = 0; j < mPm.getColumnDimension(); ++j) {
                Assert.assertEquals(2 * m.getEntry(i, j), mPm.getEntry(i, j), 0);
            }
        }

        final RealMatrix mPmMm = mPm.subtract(m);
        for (int i = 0; i < mPmMm.getRowDimension(); ++i) {
            for (int j = 0; j < mPmMm.getColumnDimension(); ++j) {
                Assert.assertEquals(m.getEntry(i, j), mPmMm.getEntry(i, j), 0);
            }
        }

        final RealMatrix mTm = mT.multiply(m);
        for (int i = 0; i < mTm.getRowDimension(); ++i) {
            for (int j = 0; j < mTm.getColumnDimension(); ++j) {
                double sum = 0;
                for (int k = 0; k < mT.getColumnDimension(); ++k) {
                    sum += (k + i / 1024.0) * (k + j / 1024.0);
                }
                Assert.assertEquals(sum, mTm.getEntry(i, j), 0);
            }
        }

        final RealMatrix mmT = m.multiply(mT);
        for (int i = 0; i < mmT.getRowDimension(); ++i) {
            for (int j = 0; j < mmT.getColumnDimension(); ++j) {
                double sum = 0;
                for (int k = 0; k < m.getColumnDimension(); ++k) {
                    sum += (i + k / 1024.0) * (j + k / 1024.0);
                }
                Assert.assertEquals(sum, mmT.getEntry(i, j), 0);
            }
        }

        final RealMatrix sub1 = m.getSubMatrix(2, 9, 5, 20);
        for (int i = 0; i < sub1.getRowDimension(); ++i) {
            for (int j = 0; j < sub1.getColumnDimension(); ++j) {
                Assert.assertEquals((i + 2) + (j + 5) / 1024.0, sub1.getEntry(i, j), 0);
            }
        }

        final RealMatrix sub2 = m.getSubMatrix(10, 12, 3, 70);
        for (int i = 0; i < sub2.getRowDimension(); ++i) {
            for (int j = 0; j < sub2.getColumnDimension(); ++j) {
                Assert.assertEquals((i + 10) + (j + 3) / 1024.0, sub2.getEntry(i, j), 0);
            }
        }

        final RealMatrix sub3 = m.getSubMatrix(30, 34, 0, 5);
        for (int i = 0; i < sub3.getRowDimension(); ++i) {
            for (int j = 0; j < sub3.getColumnDimension(); ++j) {
                Assert.assertEquals((i + 30) + (j + 0) / 1024.0, sub3.getEntry(i, j), 0);
            }
        }

        final RealMatrix sub4 = m.getSubMatrix(30, 32, 62, 65);
        for (int i = 0; i < sub4.getRowDimension(); ++i) {
            for (int j = 0; j < sub4.getColumnDimension(); ++j) {
                Assert.assertEquals((i + 30) + (j + 62) / 1024.0, sub4.getEntry(i, j), 0);
            }
        }

    }

    // Additional Test for BlockRealMatrixTest.testMultiply

    private final double[][] d3 = new double[][] { { 1, 2, 3, 4 }, { 5, 6, 7, 8 } };
    private final double[][] d4 = new double[][] { { 1 }, { 2 }, { 3 }, { 4 } };
    private final double[][] d5 = new double[][] { { 30 }, { 70 } };

    @Test
    public void testMultiply2() {
        final RealMatrix m3 = new BlockRealMatrix(this.d3);
        final RealMatrix m4 = new BlockRealMatrix(this.d4);
        final RealMatrix m5 = new BlockRealMatrix(this.d5);
        this.assertClose(m3.multiply(m4), m5, this.entryTolerance);
    }

    /** test trace */
    @Test
    public void testTrace() {
        RealMatrix m = new BlockRealMatrix(this.id);
        Assert.assertEquals("identity trace", 3d, m.getTrace(), this.entryTolerance);
        m = new BlockRealMatrix(this.testData2);
        try {
            m.getTrace();
            Assert.fail("Expecting NonSquareMatrixException");
        } catch (final NonSquareMatrixException ex) {
            // ignored
        }
    }

    /** test scalarAdd */
    @Test
    public void testScalarAdd() {
        final RealMatrix m = new BlockRealMatrix(this.testData);
        this.assertClose(new BlockRealMatrix(this.testDataPlus2), m.scalarAdd(2d), this.entryTolerance);
    }

    /** test operate */
    @Test
    public void testOperate() {
        RealMatrix m = new BlockRealMatrix(this.id);
        this.assertClose(this.testVector, m.operate(this.testVector), this.entryTolerance);
        this.assertClose(this.testVector, m.operate(new ArrayRealVector(this.testVector)).toArray(),
            this.entryTolerance);
        m = new BlockRealMatrix(this.bigSingular);
        try {
            m.operate(this.testVector);
            Assert.fail("Expecting illegalArgumentException");
        } catch (final MathIllegalArgumentException ex) {
            // ignored
        }
    }

    @Test
    public void testOperateLarge() {
        final int p = (7 * BlockRealMatrix.BLOCK_SIZE) / 2;
        final int q = (5 * BlockRealMatrix.BLOCK_SIZE) / 2;
        final int r = 3 * BlockRealMatrix.BLOCK_SIZE;
        final Random random = new Random(111007463902334l);
        final RealMatrix m1 = this.createRandomMatrix(random, p, q);
        final RealMatrix m2 = this.createRandomMatrix(random, q, r);
        final RealMatrix m1m2 = m1.multiply(m2);
        for (int i = 0; i < r; ++i) {
            this.checkArrays(m1m2.getColumn(i), m1.operate(m2.getColumn(i)));
        }
    }

    @Test
    public void testOperatePremultiplyLarge() {
        final int p = (7 * BlockRealMatrix.BLOCK_SIZE) / 2;
        final int q = (5 * BlockRealMatrix.BLOCK_SIZE) / 2;
        final int r = 3 * BlockRealMatrix.BLOCK_SIZE;
        final Random random = new Random(111007463902334l);
        final RealMatrix m1 = this.createRandomMatrix(random, p, q);
        final RealMatrix m2 = this.createRandomMatrix(random, q, r);
        final RealMatrix m1m2 = m1.multiply(m2);
        for (int i = 0; i < p; ++i) {
            this.checkArrays(m1m2.getRow(i), m2.preMultiply(m1.getRow(i)));
        }
    }

    /** test issue MATH-209 */
    @Test
    public void testMath209() {
        final RealMatrix a = new BlockRealMatrix(new double[][] {
            { 1, 2 }, { 3, 4 }, { 5, 6 }
        });
        final double[] b = a.operate(new double[] { 1, 1 });
        Assert.assertEquals(a.getRowDimension(), b.length);
        Assert.assertEquals(3.0, b[0], 1.0e-12);
        Assert.assertEquals(7.0, b[1], 1.0e-12);
        Assert.assertEquals(11.0, b[2], 1.0e-12);
    }

    /** test transpose */
    @Test
    public void testTranspose() {
        RealMatrix m = new BlockRealMatrix(this.testData);
        final RealMatrix mIT = new LUDecomposition(m).getSolver().getInverse().transpose();
        final RealMatrix mTI = new LUDecomposition(m.transpose()).getSolver().getInverse();
        this.assertClose(mIT, mTI, this.normTolerance);
        m = new BlockRealMatrix(this.testData2);
        final RealMatrix mt = new BlockRealMatrix(this.testData2T);
        this.assertClose(mt, m.transpose(), this.normTolerance);
    }

    /** test preMultiply by vector */
    @Test
    public void testPremultiplyVector() {
        RealMatrix m = new BlockRealMatrix(this.testData);
        this.assertClose(m.preMultiply(this.testVector), this.preMultTest, this.normTolerance);
        this.assertClose(m.preMultiply(new ArrayRealVector(this.testVector).toArray()),
            this.preMultTest, this.normTolerance);
        m = new BlockRealMatrix(this.bigSingular);
        try {
            m.preMultiply(this.testVector);
            Assert.fail("expecting MathIllegalArgumentException");
        } catch (final MathIllegalArgumentException ex) {
            // ignored
        }
    }

    @Test
    public void testPremultiply() {
        final RealMatrix m3 = new BlockRealMatrix(this.d3);
        final RealMatrix m4 = new BlockRealMatrix(this.d4);
        final RealMatrix m5 = new BlockRealMatrix(this.d5);
        this.assertClose(m4.preMultiply(m3), m5, this.entryTolerance);

        final BlockRealMatrix m = new BlockRealMatrix(this.testData);
        final BlockRealMatrix mInv = new BlockRealMatrix(this.testDataInv);
        final BlockRealMatrix identity = new BlockRealMatrix(this.id);
        this.assertClose(m.preMultiply(mInv), identity, this.entryTolerance);
        this.assertClose(mInv.preMultiply(m), identity, this.entryTolerance);
        this.assertClose(m.preMultiply(identity), m, this.entryTolerance);
        this.assertClose(identity.preMultiply(mInv), mInv, this.entryTolerance);
        try {
            m.preMultiply(new BlockRealMatrix(this.bigSingular));
            Assert.fail("Expecting illegalArgumentException");
        } catch (final MathIllegalArgumentException ex) {
            // ignored
        }
    }

    @Test
    public void testGetVectors() {
        final RealMatrix m = new BlockRealMatrix(this.testData);
        this.assertClose(m.getRow(0), this.testDataRow1, this.entryTolerance);
        this.assertClose(m.getColumn(2), this.testDataCol3, this.entryTolerance);
        try {
            m.getRow(10);
            Assert.fail("expecting OutOfRangeException");
        } catch (final OutOfRangeException ex) {
            // ignored
        }
        try {
            m.getColumn(-1);
            Assert.fail("expecting OutOfRangeException");
        } catch (final OutOfRangeException ex) {
            // ignored
        }
    }

    @Test
    public void testGetEntry() {
        final RealMatrix m = new BlockRealMatrix(this.testData);
        Assert.assertEquals("get entry", m.getEntry(0, 1), 2d, this.entryTolerance);
        try {
            m.getEntry(10, 4);
            Assert.fail("Expecting OutOfRangeException");
        } catch (final OutOfRangeException ex) {
            // expected
        }
    }

    /** test examples in user guide */
    @Test
    public void testExamples() {
        // Create a real matrix with two rows and three columns
        final double[][] matrixData = { { 1d, 2d, 3d }, { 2d, 5d, 3d } };
        final RealMatrix m = new BlockRealMatrix(matrixData);
        // One more with three rows, two columns
        final double[][] matrixData2 = { { 1d, 2d }, { 2d, 5d }, { 1d, 7d } };
        final RealMatrix n = new BlockRealMatrix(matrixData2);
        // Now multiply m by n
        final RealMatrix p = m.multiply(n);
        Assert.assertEquals(2, p.getRowDimension());
        Assert.assertEquals(2, p.getColumnDimension());
        // Invert p
        final RealMatrix pInverse = new LUDecomposition(p).getSolver().getInverse();
        Assert.assertEquals(2, pInverse.getRowDimension());
        Assert.assertEquals(2, pInverse.getColumnDimension());

        // Solve example
        final double[][] coefficientsData = { { 2, 3, -2 }, { -1, 7, 6 }, { 4, -3, -5 } };
        final RealMatrix coefficients = new BlockRealMatrix(coefficientsData);
        final RealVector constants = new ArrayRealVector(new double[] { 1, -2, 1 }, false);
        final RealVector solution = new LUDecomposition(coefficients).getSolver().solve(constants);
        final double cst0 = constants.getEntry(0);
        final double cst1 = constants.getEntry(1);
        final double cst2 = constants.getEntry(2);
        final double sol0 = solution.getEntry(0);
        final double sol1 = solution.getEntry(1);
        final double sol2 = solution.getEntry(2);
        Assert.assertEquals(2 * sol0 + 3 * sol1 - 2 * sol2, cst0, 1E-12);
        Assert.assertEquals(-1 * sol0 + 7 * sol1 + 6 * sol2, cst1, 1E-12);
        Assert.assertEquals(4 * sol0 - 3 * sol1 - 5 * sol2, cst2, 1E-12);
    }

    // test submatrix accessors
    @Test
    public void testGetSubMatrix() {
        final RealMatrix m = new BlockRealMatrix(this.subTestData);
        this.checkGetSubMatrix(m, this.subRows23Cols00, 2, 3, 0, 0);
        this.checkGetSubMatrix(m, this.subRows00Cols33, 0, 0, 3, 3);
        this.checkGetSubMatrix(m, this.subRows01Cols23, 0, 1, 2, 3);
        this.checkGetSubMatrix(m, this.subRows02Cols13, new int[] { 0, 2 }, new int[] { 1, 3 });
        this.checkGetSubMatrix(m, this.subRows03Cols12, new int[] { 0, 3 }, new int[] { 1, 2 });
        this.checkGetSubMatrix(m, this.subRows03Cols123, new int[] { 0, 3 }, new int[] { 1, 2, 3 });
        this.checkGetSubMatrix(m, this.subRows20Cols123, new int[] { 2, 0 }, new int[] { 1, 2, 3 });
        this.checkGetSubMatrix(m, this.subRows31Cols31, new int[] { 3, 1 }, new int[] { 3, 1 });
        this.checkGetSubMatrix(m, this.subRows31Cols31, new int[] { 3, 1 }, new int[] { 3, 1 });
        this.checkGetSubMatrix(m, null, 1, 0, 2, 4);
        this.checkGetSubMatrix(m, null, -1, 1, 2, 2);
        this.checkGetSubMatrix(m, null, 1, 0, 2, 2);
        this.checkGetSubMatrix(m, null, 1, 0, 2, 4);
        this.checkGetSubMatrix(m, null, new int[] {}, new int[] { 0 });
        this.checkGetSubMatrix(m, null, new int[] { 0 }, new int[] { 4 });
    }

    private void checkGetSubMatrix(final RealMatrix m, final double[][] reference,
            final int startRow, final int endRow, final int startColumn, final int endColumn) {
        try {
            final RealMatrix sub = m.getSubMatrix(startRow, endRow, startColumn, endColumn);
            if (reference != null) {
                Assert.assertEquals(new BlockRealMatrix(reference), sub);
            } else {
                Assert.fail("Expecting OutOfRangeException or NumberIsTooSmallException or NoDataException");
            }
        } catch (final OutOfRangeException e) {
            if (reference != null) {
                throw e;
            }
        } catch (final NumberIsTooSmallException e) {
            if (reference != null) {
                throw e;
            }
        } catch (final NoDataException e) {
            if (reference != null) {
                throw e;
            }
        }
    }

    private void checkGetSubMatrix(final RealMatrix m, final double[][] reference,
            final int[] selectedRows, final int[] selectedColumns) {
        try {
            final RealMatrix sub = m.getSubMatrix(selectedRows, selectedColumns);
            if (reference != null) {
                Assert.assertEquals(new BlockRealMatrix(reference), sub);
            } else {
                Assert.fail("Expecting OutOfRangeException or NumberIsTooSmallExceptiono r NoDataException");
            }
        } catch (final OutOfRangeException e) {
            if (reference != null) {
                throw e;
            }
        } catch (final NumberIsTooSmallException e) {
            if (reference != null) {
                throw e;
            }
        } catch (final NoDataException e) {
            if (reference != null) {
                throw e;
            }
        }
    }

    @Test
    public void testGetSetMatrixLarge() {
        final int n = 3 * BlockRealMatrix.BLOCK_SIZE;
        final RealMatrix m = new BlockRealMatrix(n, n);
        final RealMatrix sub = new BlockRealMatrix(n - 4, n - 4).scalarAdd(1);

        m.setSubMatrix(sub.getData(false), 2, 2);
        for (int i = 0; i < n; ++i) {
            for (int j = 0; j < n; ++j) {
                if ((i < 2) || (i > n - 3) || (j < 2) || (j > n - 3)) {
                    Assert.assertEquals(0.0, m.getEntry(i, j), 0.0);
                } else {
                    Assert.assertEquals(1.0, m.getEntry(i, j), 0.0);
                }
            }
        }
        Assert.assertEquals(sub, m.getSubMatrix(2, n - 3, 2, n - 3));

    }

    @Test
    public void testCopySubMatrix() {
        final RealMatrix m = new BlockRealMatrix(this.subTestData);
        this.checkCopy(m, this.subRows23Cols00, 2, 3, 0, 0);
        this.checkCopy(m, this.subRows00Cols33, 0, 0, 3, 3);
        this.checkCopy(m, this.subRows01Cols23, 0, 1, 2, 3);
        this.checkCopy(m, this.subRows02Cols13, new int[] { 0, 2 }, new int[] { 1, 3 });
        this.checkCopy(m, this.subRows03Cols12, new int[] { 0, 3 }, new int[] { 1, 2 });
        this.checkCopy(m, this.subRows03Cols123, new int[] { 0, 3 }, new int[] { 1, 2, 3 });
        this.checkCopy(m, this.subRows20Cols123, new int[] { 2, 0 }, new int[] { 1, 2, 3 });
        this.checkCopy(m, this.subRows31Cols31, new int[] { 3, 1 }, new int[] { 3, 1 });
        this.checkCopy(m, this.subRows31Cols31, new int[] { 3, 1 }, new int[] { 3, 1 });

        this.checkCopy(m, null, 1, 0, 2, 4);
        this.checkCopy(m, null, -1, 1, 2, 2);
        this.checkCopy(m, null, 1, 0, 2, 2);
        this.checkCopy(m, null, 1, 0, 2, 4);
        this.checkCopy(m, null, new int[] {}, new int[] { 0 });
        this.checkCopy(m, null, new int[] { 0 }, new int[] { 4 });
    }

    private void checkCopy(final RealMatrix m, final double[][] reference,
            final int startRow, final int endRow, final int startColumn, final int endColumn) {
        try {
            final double[][] sub = (reference == null) ?
                new double[1][1] :
                new double[reference.length][reference[0].length];
            m.copySubMatrix(startRow, endRow, startColumn, endColumn, sub);
            if (reference != null) {
                Assert.assertEquals(new BlockRealMatrix(reference), new BlockRealMatrix(sub));
            } else {
                Assert.fail("Expecting OutOfRangeException or NumberIsTooSmallException or NoDataException");
            }
        } catch (final OutOfRangeException e) {
            if (reference != null) {
                throw e;
            }
        } catch (final NumberIsTooSmallException e) {
            if (reference != null) {
                throw e;
            }
        } catch (final NoDataException e) {
            if (reference != null) {
                throw e;
            }
        }
    }

    private void checkCopy(final RealMatrix m, final double[][] reference,
            final int[] selectedRows, final int[] selectedColumns) {
        try {
            final double[][] sub = (reference == null) ?
                new double[1][1] :
                new double[reference.length][reference[0].length];
            m.copySubMatrix(selectedRows, selectedColumns, sub);
            if (reference != null) {
                Assert.assertEquals(new BlockRealMatrix(reference), new BlockRealMatrix(sub));
            } else {
                Assert.fail("Expecting OutOfRangeException or NumberIsTooSmallException or NoDataException");
            }
        } catch (final OutOfRangeException e) {
            if (reference != null) {
                throw e;
            }
        } catch (final NumberIsTooSmallException e) {
            if (reference != null) {
                throw e;
            }
        } catch (final NoDataException e) {
            if (reference != null) {
                throw e;
            }
        }
    }

    @Test
    public void testGetRowMatrix() {
        final RealMatrix m = new BlockRealMatrix(this.subTestData);
        final RealMatrix mRow0 = new BlockRealMatrix(this.subRow0);
        final RealMatrix mRow3 = new BlockRealMatrix(this.subRow3);
        Assert.assertEquals("Row0", mRow0, m.getRowMatrix(0));
        Assert.assertEquals("Row3", mRow3, m.getRowMatrix(3));
        try {
            m.getRowMatrix(-1);
            Assert.fail("Expecting OutOfRangeException");
        } catch (final OutOfRangeException ex) {
            // expected
        }
        try {
            m.getRowMatrix(4);
            Assert.fail("Expecting OutOfRangeException");
        } catch (final OutOfRangeException ex) {
            // expected
        }
    }

    @Test
    public void testSetRowMatrix() {
        final RealMatrix m = new BlockRealMatrix(this.subTestData);
        final RealMatrix mRow3 = new BlockRealMatrix(this.subRow3);
        Assert.assertNotSame(mRow3, m.getRowMatrix(0));
        m.setRowMatrix(0, mRow3);
        Assert.assertEquals(mRow3, m.getRowMatrix(0));
        try {
            m.setRowMatrix(-1, mRow3);
            Assert.fail("Expecting OutOfRangeException");
        } catch (final OutOfRangeException ex) {
            // expected
        }
        try {
            m.setRowMatrix(0, m);
            Assert.fail("Expecting MatrixDimensionMismatchException");
        } catch (final MatrixDimensionMismatchException ex) {
            // expected
        }
        
        // Handle class cast exception
        new BlockRealMatrix(5, 5).setRowMatrix(0, new Array2DRowRealMatrix(1, 5));
    }

    @Test
    public void testGetSetRowMatrixLarge() {
        final int n = 3 * BlockRealMatrix.BLOCK_SIZE;
        final RealMatrix m = new BlockRealMatrix(n, n);
        final RealMatrix sub = new BlockRealMatrix(1, n).scalarAdd(1);

        m.setRowMatrix(2, sub);
        for (int i = 0; i < n; ++i) {
            for (int j = 0; j < n; ++j) {
                if (i != 2) {
                    Assert.assertEquals(0.0, m.getEntry(i, j), 0.0);
                } else {
                    Assert.assertEquals(1.0, m.getEntry(i, j), 0.0);
                }
            }
        }
        Assert.assertEquals(sub, m.getRowMatrix(2));
    }

    @Test
    public void testGetColumnMatrix() {
        final RealMatrix m = new BlockRealMatrix(this.subTestData);
        final RealMatrix mColumn1 = new BlockRealMatrix(this.subColumn1);
        final RealMatrix mColumn3 = new BlockRealMatrix(this.subColumn3);
        Assert.assertEquals(mColumn1, m.getColumnMatrix(1));
        Assert.assertEquals(mColumn3, m.getColumnMatrix(3));
        try {
            m.getColumnMatrix(-1);
            Assert.fail("Expecting OutOfRangeException");
        } catch (final OutOfRangeException ex) {
            // expected
        }
        try {
            m.getColumnMatrix(4);
            Assert.fail("Expecting OutOfRangeException");
        } catch (final OutOfRangeException ex) {
            // expected
        }
    }

    @Test
    public void testSetColumnMatrix() {
        final RealMatrix m = new BlockRealMatrix(this.subTestData);
        final RealMatrix mColumn3 = new BlockRealMatrix(this.subColumn3);
        Assert.assertNotSame(mColumn3, m.getColumnMatrix(1));
        m.setColumnMatrix(1, mColumn3);
        Assert.assertEquals(mColumn3, m.getColumnMatrix(1));
        try {
            m.setColumnMatrix(-1, mColumn3);
            Assert.fail("Expecting OutOfRangeException");
        } catch (final OutOfRangeException ex) {
            // expected
        }
        try {
            m.setColumnMatrix(0, m);
            Assert.fail("Expecting MatrixDimensionMismatchException");
        } catch (final MatrixDimensionMismatchException ex) {
            // expected
        }
        
        // Handle class cast exception
        new BlockRealMatrix(5, 5).setColumnMatrix(0, new Array2DRowRealMatrix(5, 1));
    }

    @Test
    public void testGetSetColumnMatrixLarge() {
        final int n = 3 * BlockRealMatrix.BLOCK_SIZE;
        final RealMatrix m = new BlockRealMatrix(n, n);
        final RealMatrix sub = new BlockRealMatrix(n, 1).scalarAdd(1);

        m.setColumnMatrix(2, sub);
        for (int i = 0; i < n; ++i) {
            for (int j = 0; j < n; ++j) {
                if (j != 2) {
                    Assert.assertEquals(0.0, m.getEntry(i, j), 0.0);
                } else {
                    Assert.assertEquals(1.0, m.getEntry(i, j), 0.0);
                }
            }
        }
        Assert.assertEquals(sub, m.getColumnMatrix(2));

    }

    @Test
    public void testGetRowVector() {
        final RealMatrix m = new BlockRealMatrix(this.subTestData);
        final RealVector mRow0 = new ArrayRealVector(this.subRow0[0]);
        final RealVector mRow3 = new ArrayRealVector(this.subRow3[0]);
        Assert.assertEquals(mRow0, m.getRowVector(0));
        Assert.assertEquals(mRow3, m.getRowVector(3));
        try {
            m.getRowVector(-1);
            Assert.fail("Expecting OutOfRangeException");
        } catch (final OutOfRangeException ex) {
            // expected
        }
        try {
            m.getRowVector(4);
            Assert.fail("Expecting OutOfRangeException");
        } catch (final OutOfRangeException ex) {
            // expected
        }
    }

    @Test
    public void testSetRowVector() {
        final RealMatrix m = new BlockRealMatrix(this.subTestData);
        final RealVector mRow3 = new ArrayRealVector(this.subRow3[0]);
        Assert.assertNotSame(mRow3, m.getRowMatrix(0));
        m.setRowVector(0, mRow3);
        Assert.assertEquals(mRow3, m.getRowVector(0));
        try {
            m.setRowVector(-1, mRow3);
            Assert.fail("Expecting OutOfRangeException");
        } catch (final OutOfRangeException ex) {
            // expected
        }
        try {
            m.setRowVector(0, new ArrayRealVector(5));
            Assert.fail("Expecting MatrixDimensionMismatchException");
        } catch (final MatrixDimensionMismatchException ex) {
            // expected
        }
    }

    @Test
    public void testGetSetRowVectorLarge() {
        final int n = 3 * BlockRealMatrix.BLOCK_SIZE;
        final RealMatrix m = new BlockRealMatrix(n, n);
        final RealVector sub = new ArrayRealVector(n, 1.0);

        m.setRowVector(2, sub);
        for (int i = 0; i < n; ++i) {
            for (int j = 0; j < n; ++j) {
                if (i != 2) {
                    Assert.assertEquals(0.0, m.getEntry(i, j), 0.0);
                } else {
                    Assert.assertEquals(1.0, m.getEntry(i, j), 0.0);
                }
            }
        }
        Assert.assertEquals(sub, m.getRowVector(2));
    }

    @Test
    public void testGetColumnVector() {
        final RealMatrix m = new BlockRealMatrix(this.subTestData);
        final RealVector mColumn1 = this.columnToVector(this.subColumn1);
        final RealVector mColumn3 = this.columnToVector(this.subColumn3);
        Assert.assertEquals(mColumn1, m.getColumnVector(1));
        Assert.assertEquals(mColumn3, m.getColumnVector(3));
        try {
            m.getColumnVector(-1);
            Assert.fail("Expecting OutOfRangeException");
        } catch (final OutOfRangeException ex) {
            // expected
        }
        try {
            m.getColumnVector(4);
            Assert.fail("Expecting OutOfRangeException");
        } catch (final OutOfRangeException ex) {
            // expected
        }
    }

    @Test
    public void testSetColumnVector() {
        final RealMatrix m = new BlockRealMatrix(this.subTestData);
        final RealVector mColumn3 = this.columnToVector(this.subColumn3);
        Assert.assertNotSame(mColumn3, m.getColumnVector(1));
        m.setColumnVector(1, mColumn3);
        Assert.assertEquals(mColumn3, m.getColumnVector(1));
        try {
            m.setColumnVector(-1, mColumn3);
            Assert.fail("Expecting OutOfRangeException");
        } catch (final OutOfRangeException ex) {
            // expected
        }
        try {
            m.setColumnVector(0, new ArrayRealVector(5));
            Assert.fail("Expecting MatrixDimensionMismatchException");
        } catch (final MatrixDimensionMismatchException ex) {
            // expected
        }
    }

    @Test
    public void testGetSetColumnVectorLarge() {
        final int n = 3 * BlockRealMatrix.BLOCK_SIZE;
        final RealMatrix m = new BlockRealMatrix(n, n);
        final RealVector sub = new ArrayRealVector(n, 1.0);

        m.setColumnVector(2, sub);
        for (int i = 0; i < n; ++i) {
            for (int j = 0; j < n; ++j) {
                if (j != 2) {
                    Assert.assertEquals(0.0, m.getEntry(i, j), 0.0);
                } else {
                    Assert.assertEquals(1.0, m.getEntry(i, j), 0.0);
                }
            }
        }
        Assert.assertEquals(sub, m.getColumnVector(2));
    }

    private RealVector columnToVector(final double[][] column) {
        final double[] data = new double[column.length];
        for (int i = 0; i < data.length; ++i) {
            data[i] = column[i][0];
        }
        return new ArrayRealVector(data, false);
    }

    @Test
    public void testGetRow() {
        final RealMatrix m = new BlockRealMatrix(this.subTestData);
        this.checkArrays(this.subRow0[0], m.getRow(0));
        this.checkArrays(this.subRow3[0], m.getRow(3));
        try {
            m.getRow(-1);
            Assert.fail("Expecting OutOfRangeException");
        } catch (final OutOfRangeException ex) {
            // expected
        }
        try {
            m.getRow(4);
            Assert.fail("Expecting OutOfRangeException");
        } catch (final OutOfRangeException ex) {
            // expected
        }
    }

    @Test
    public void testSetRow() {
        final RealMatrix m = new BlockRealMatrix(this.subTestData);
        Assert.assertTrue(this.subRow3[0][0] != m.getRow(0)[0]);
        m.setRow(0, this.subRow3[0]);
        this.checkArrays(this.subRow3[0], m.getRow(0));
        try {
            m.setRow(-1, this.subRow3[0]);
            Assert.fail("Expecting OutOfRangeException");
        } catch (final OutOfRangeException ex) {
            // expected
        }
        try {
            m.setRow(0, new double[5]);
            Assert.fail("Expecting MatrixDimensionMismatchException");
        } catch (final MatrixDimensionMismatchException ex) {
            // expected
        }
    }

    @Test
    public void testGetSetRowLarge() {
        final int n = 3 * BlockRealMatrix.BLOCK_SIZE;
        final RealMatrix m = new BlockRealMatrix(n, n);
        final double[] sub = new double[n];
        Arrays.fill(sub, 1.0);

        m.setRow(2, sub);
        for (int i = 0; i < n; ++i) {
            for (int j = 0; j < n; ++j) {
                if (i != 2) {
                    Assert.assertEquals(0.0, m.getEntry(i, j), 0.0);
                } else {
                    Assert.assertEquals(1.0, m.getEntry(i, j), 0.0);
                }
            }
        }
        this.checkArrays(sub, m.getRow(2));
    }

    @Test
    public void testGetColumn() {
        final RealMatrix m = new BlockRealMatrix(this.subTestData);
        final double[] mColumn1 = this.columnToArray(this.subColumn1);
        final double[] mColumn3 = this.columnToArray(this.subColumn3);
        this.checkArrays(mColumn1, m.getColumn(1));
        this.checkArrays(mColumn3, m.getColumn(3));
        try {
            m.getColumn(-1);
            Assert.fail("Expecting OutOfRangeException");
        } catch (final OutOfRangeException ex) {
            // expected
        }
        try {
            m.getColumn(4);
            Assert.fail("Expecting OutOfRangeException");
        } catch (final OutOfRangeException ex) {
            // expected
        }
    }

    @Test
    public void testSetColumn() {
        final RealMatrix m = new BlockRealMatrix(this.subTestData);
        final double[] mColumn3 = this.columnToArray(this.subColumn3);
        Assert.assertTrue(mColumn3[0] != m.getColumn(1)[0]);
        m.setColumn(1, mColumn3);
        this.checkArrays(mColumn3, m.getColumn(1));
        try {
            m.setColumn(-1, mColumn3);
            Assert.fail("Expecting OutOfRangeException");
        } catch (final OutOfRangeException ex) {
            // expected
        }
        try {
            m.setColumn(0, new double[5]);
            Assert.fail("Expecting MatrixDimensionMismatchException");
        } catch (final MatrixDimensionMismatchException ex) {
            // expected
        }
    }

    @Test
    public void testGetSetColumnLarge() {
        final int n = 3 * BlockRealMatrix.BLOCK_SIZE;
        final RealMatrix m = new BlockRealMatrix(n, n);
        final double[] sub = new double[n];
        Arrays.fill(sub, 1.0);

        m.setColumn(2, sub);
        for (int i = 0; i < n; ++i) {
            for (int j = 0; j < n; ++j) {
                if (j != 2) {
                    Assert.assertEquals(0.0, m.getEntry(i, j), 0.0);
                } else {
                    Assert.assertEquals(1.0, m.getEntry(i, j), 0.0);
                }
            }
        }
        this.checkArrays(sub, m.getColumn(2));
    }

    private double[] columnToArray(final double[][] column) {
        final double[] data = new double[column.length];
        for (int i = 0; i < data.length; ++i) {
            data[i] = column[i][0];
        }
        return data;
    }

    private void checkArrays(final double[] expected, final double[] actual) {
        Assert.assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; ++i) {
            Assert.assertEquals(expected[i], actual[i], 0);
        }
    }

    @Test
    public void testEqualsAndHashCode() {
        final BlockRealMatrix m = new BlockRealMatrix(this.testData);
        final BlockRealMatrix m1 = m.copy();
        final BlockRealMatrix mt = m.transpose();
        Assert.assertTrue(m.hashCode() != mt.hashCode());
        Assert.assertEquals(m.hashCode(), m1.hashCode());
        Assert.assertEquals(m, m);
        Assert.assertEquals(m, m1);
        Assert.assertFalse(m.equals(null));
        Assert.assertFalse(m.equals(mt));
        Assert.assertFalse(m.equals(new BlockRealMatrix(this.bigSingular)));
    }

    @Test
    public void testSetSubMatrix() {
        final BlockRealMatrix m = new BlockRealMatrix(this.testData);
        m.setSubMatrix(this.detData2, 1, 1);
        RealMatrix expected = new BlockRealMatrix
            (new double[][] { { 1.0, 2.0, 3.0 }, { 2.0, 1.0, 3.0 }, { 1.0, 2.0, 4.0 } });
        Assert.assertEquals(expected, m);

        m.setSubMatrix(this.detData2, 0, 0);
        expected = new BlockRealMatrix
            (new double[][] { { 1.0, 3.0, 3.0 }, { 2.0, 4.0, 3.0 }, { 1.0, 2.0, 4.0 } });
        Assert.assertEquals(expected, m);

        m.setSubMatrix(this.testDataPlus2, 0, 0);
        expected = new BlockRealMatrix
            (new double[][] { { 3.0, 4.0, 5.0 }, { 4.0, 7.0, 5.0 }, { 3.0, 2.0, 10.0 } });
        Assert.assertEquals(expected, m);

        // javadoc example
        final BlockRealMatrix matrix = new BlockRealMatrix
            (new double[][] { { 1, 2, 3, 4 }, { 5, 6, 7, 8 }, { 9, 0, 1, 2 } });
        matrix.setSubMatrix(new double[][] { { 3, 4 }, { 5, 6 } }, 1, 1);
        expected = new BlockRealMatrix
            (new double[][] { { 1, 2, 3, 4 }, { 5, 3, 4, 8 }, { 9, 5, 6, 2 } });
        Assert.assertEquals(expected, matrix);

        // dimension overflow
        try {
            m.setSubMatrix(this.testData, 1, 1);
            Assert.fail("expecting OutOfRangeException");
        } catch (final OutOfRangeException e) {
            // expected
        }
        // dimension underflow
        try {
            m.setSubMatrix(this.testData, -1, 1);
            Assert.fail("expecting OutOfRangeException");
        } catch (final OutOfRangeException e) {
            // expected
        }
        try {
            m.setSubMatrix(this.testData, 1, -1);
            Assert.fail("expecting OutOfRangeException");
        } catch (final OutOfRangeException e) {
            // expected
        }

        // null
        try {
            m.setSubMatrix(null, 1, 1);
            Assert.fail("expecting NullArgumentException");
        } catch (final NullArgumentException e) {
            // expected
        }

        // ragged
        try {
            m.setSubMatrix(new double[][] { { 1 }, { 2, 3 } }, 0, 0);
            Assert.fail("expecting MathIllegalArgumentException");
        } catch (final MathIllegalArgumentException e) {
            // expected
        }

        // empty
        try {
            m.setSubMatrix(new double[][] { {} }, 0, 0);
            Assert.fail("expecting MathIllegalArgumentException");
        } catch (final MathIllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testWalk() {
        final int rows = 150;
        final int columns = 75;

        RealMatrix m = new BlockRealMatrix(rows, columns);
        m.walkInRowOrder(new SetVisitor());
        GetVisitor getVisitor = new GetVisitor();
        m.walkInOptimizedOrder(getVisitor);
        Assert.assertEquals(rows * columns, getVisitor.getCount());

        m = new BlockRealMatrix(rows, columns);
        m.walkInRowOrder(new SetVisitor(), 1, rows - 2, 1, columns - 2);
        getVisitor = new GetVisitor();
        m.walkInOptimizedOrder(getVisitor, 1, rows - 2, 1, columns - 2);
        Assert.assertEquals((rows - 2) * (columns - 2), getVisitor.getCount());
        for (int i = 0; i < rows; ++i) {
            Assert.assertEquals(0.0, m.getEntry(i, 0), 0);
            Assert.assertEquals(0.0, m.getEntry(i, columns - 1), 0);
        }
        for (int j = 0; j < columns; ++j) {
            Assert.assertEquals(0.0, m.getEntry(0, j), 0);
            Assert.assertEquals(0.0, m.getEntry(rows - 1, j), 0);
        }

        m = new BlockRealMatrix(rows, columns);
        m.walkInColumnOrder(new SetVisitor());
        getVisitor = new GetVisitor();
        m.walkInOptimizedOrder(getVisitor);
        Assert.assertEquals(rows * columns, getVisitor.getCount());

        m = new BlockRealMatrix(rows, columns);
        m.walkInColumnOrder(new SetVisitor(), 1, rows - 2, 1, columns - 2);
        getVisitor = new GetVisitor();
        m.walkInOptimizedOrder(getVisitor, 1, rows - 2, 1, columns - 2);
        Assert.assertEquals((rows - 2) * (columns - 2), getVisitor.getCount());
        for (int i = 0; i < rows; ++i) {
            Assert.assertEquals(0.0, m.getEntry(i, 0), 0);
            Assert.assertEquals(0.0, m.getEntry(i, columns - 1), 0);
        }
        for (int j = 0; j < columns; ++j) {
            Assert.assertEquals(0.0, m.getEntry(0, j), 0);
            Assert.assertEquals(0.0, m.getEntry(rows - 1, j), 0);
        }

        m = new BlockRealMatrix(rows, columns);
        m.walkInOptimizedOrder(new SetVisitor());
        getVisitor = new GetVisitor();
        m.walkInRowOrder(getVisitor);
        Assert.assertEquals(rows * columns, getVisitor.getCount());

        m = new BlockRealMatrix(rows, columns);
        m.walkInOptimizedOrder(new SetVisitor(), 1, rows - 2, 1, columns - 2);
        getVisitor = new GetVisitor();
        m.walkInRowOrder(getVisitor, 1, rows - 2, 1, columns - 2);
        Assert.assertEquals((rows - 2) * (columns - 2), getVisitor.getCount());
        for (int i = 0; i < rows; ++i) {
            Assert.assertEquals(0.0, m.getEntry(i, 0), 0);
            Assert.assertEquals(0.0, m.getEntry(i, columns - 1), 0);
        }
        for (int j = 0; j < columns; ++j) {
            Assert.assertEquals(0.0, m.getEntry(0, j), 0);
            Assert.assertEquals(0.0, m.getEntry(rows - 1, j), 0);
        }

        m = new BlockRealMatrix(rows, columns);
        m.walkInOptimizedOrder(new SetVisitor());
        getVisitor = new GetVisitor();
        m.walkInColumnOrder(getVisitor);
        Assert.assertEquals(rows * columns, getVisitor.getCount());

        m = new BlockRealMatrix(rows, columns);
        m.walkInOptimizedOrder(new SetVisitor(), 1, rows - 2, 1, columns - 2);
        getVisitor = new GetVisitor();
        m.walkInColumnOrder(getVisitor, 1, rows - 2, 1, columns - 2);
        Assert.assertEquals((rows - 2) * (columns - 2), getVisitor.getCount());
        for (int i = 0; i < rows; ++i) {
            Assert.assertEquals(0.0, m.getEntry(i, 0), 0);
            Assert.assertEquals(0.0, m.getEntry(i, columns - 1), 0);
        }
        for (int j = 0; j < columns; ++j) {
            Assert.assertEquals(0.0, m.getEntry(0, j), 0);
            Assert.assertEquals(0.0, m.getEntry(rows - 1, j), 0);
        }

    }

    @Test
    public void testSerial() {
        final BlockRealMatrix m = new BlockRealMatrix(this.testData);
        Assert.assertEquals(m, TestUtils.serializeAndRecover(m));
    }

    private static class SetVisitor extends DefaultRealMatrixChangingVisitor {
        @Override
        public double visit(final int i, final int j, final double value) {
            return i + j / 1024.0;
        }
    }

    private static class GetVisitor extends DefaultRealMatrixPreservingVisitor {
        private int count = 0;

        @Override
        public void visit(final int i, final int j, final double value) {
            ++this.count;
            Assert.assertEquals(i + j / 1024.0, value, 0.0);
        }

        public int getCount() {
            return this.count;
        }
    }

    // --------------- -----------------Protected methods

    /** verifies that two matrices are close (1-norm) */
    protected void assertClose(final RealMatrix m, final RealMatrix n, final double tolerance) {
        Assert.assertTrue(m.subtract(n).getNorm() < tolerance);
    }

    /** verifies that two vectors are close (sup norm) */
    protected void assertClose(final double[] m, final double[] n, final double tolerance) {
        if (m.length != n.length) {
            Assert.fail("vectors not same length");
        }
        for (int i = 0; i < m.length; i++) {
            Assert.assertEquals(m[i], n[i], tolerance);
        }
    }

    private BlockRealMatrix createRandomMatrix(final Random r, final int rows, final int columns) {
        final BlockRealMatrix m = new BlockRealMatrix(rows, columns);
        for (int i = 0; i < rows; ++i) {
            for (int j = 0; j < columns; ++j) {
                m.setEntry(i, j, 200 * r.nextDouble() - 100);
            }
        }
        return m;
    }
}
