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
 */
/* 
 * HISTORY
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
* VERSION:4.8:FA:FA-2940:15/11/2021:[PATRIUS] Anomalies suite a DM 2766 sur package fr.cnes.sirius.patrius.math.linear 
 * VERSION:4.7:DM:DM-2766:18/05/2021:Evol. et corr. dans le package fr.cnes.sirius.patrius.math.linear (suite DM 2300) 
 * VERSION:4.6:FA:FA-2542:27/01/2021:[PATRIUS] Definition d'un champ de vue avec demi-angle de 180° 
 * VERSION:4.5.1:FA:FA-2540:04/08/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear
 * VERSION:4.5:DM:DM-2300:27/05/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:306:13/11/2014: coverage
 * VERSION::DM:319:05/03/2015:Corrected Rotation class (Step1)
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.linear;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.exception.MathIllegalStateException;
import fr.cnes.sirius.patrius.math.exception.NoDataException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;
import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;

/**
 * Test cases for the {@link Array2DRowRealMatrix} class.
 * 
 * @version $Id: Array2DRowRealMatrixTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
public final class Array2DRowRealMatrixTest {

    // 3 x 3 identity matrix
    protected double[][] id = { { 1d, 0d, 0d }, { 0d, 1d, 0d }, { 0d, 0d, 1d } };

    // Test data for group operations
    protected double[][] testData = { { 1d, 2d, 3d }, { 2d, 5d, 3d }, { 1d, 0d, 8d } };
    protected double[][] testDataLU = { { 2d, 5d, 3d }, { .5d, -2.5d, 6.5d }, { 0.5d, 0.2d, .2d } };
    protected double[][] testDataPlus2 = { { 3d, 4d, 5d }, { 4d, 7d, 5d }, { 3d, 2d, 10d } };
    protected double[][] testDataMinus = { { -1d, -2d, -3d }, { -2d, -5d, -3d }, { -1d, 0d, -8d } };
    protected double[] testDataRow1 = { 1d, 2d, 3d };
    protected double[] testDataCol3 = { 3d, 3d, 8d };
    protected double[][] testDataInv = { { -40d, 16d, 9d }, { 13d, -5d, -3d }, { 5d, -2d, -1d } };
    protected double[] preMultTest = { 8, 12, 33 };
    protected double[][] testData2 = { { 1d, 2d, 3d }, { 2d, 5d, 3d } };
    protected double[][] testData2T = { { 1d, 2d }, { 2d, 5d }, { 3d, 3d } };
    protected double[][] testDataPlusInv = { { -39d, 18d, 12d }, { 15d, 0d, 0d }, { 6d, -2d, 7d } };

    protected double[][] testDataEntryAdded = { { 1d, 2d, 3d }, { 2d, 10d, 3d }, { 1d, 0d, 8d } };

    // lu decomposition tests
    protected double[][] luData = { { 2d, 3d, 3d }, { 0d, 5d, 7d }, { 6d, 9d, 8d } };
    protected double[][] luDataLUDecomposition = { { 6d, 9d, 8d }, { 0d, 5d, 7d },
            { 0.33333333333333, 0d, 0.33333333333333 } };

    // symmetric and antisymmetric tests
    protected double[][] symmetricData = { { 2d, 3d, 5d }, { 3d, 4d, 8d }, { 5d, 8d, 9d } };
    protected double[][] antisymmetricData = { { 0d, 3d, 5d }, { -3d, 0d, 8d }, { -5d, -8d, 0d } };

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
    protected double[][] subTestData = { { 1, 2, 3, 4 }, { 1.5, 2.5, 3.5, 4.5 }, { 2, 4, 6, 8 },
            { 4, 5, 6, 7 } };
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
    // empty matrix
    protected double[][] empty = {};
    // one empty row matrix
    protected double[][] oneEmptyRow = { {}, { 4, 8 } };
    // bad dimensions
    protected double[][] dimensionMismatch = { { 4, 6, 8 }, { 3, 4 } };

    // tolerances
    protected double entryTolerance = 10E-16;
    protected double normTolerance = 10E-14;
    protected double powerTolerance = 10E-16;

    /** constructors test */
    @Test
    public void testConstructors() {
        // with copy array
        final Array2DRowRealMatrix m = new Array2DRowRealMatrix(this.testData, true);
        for (int row = 0; row < m.getRowDimension(); row++) {
            for (int col = 0; col < m.getColumnDimension(); col++) {
                Assert.assertEquals("matrix constructor test", m.getDataRef()[row][col],
                        this.testData[row][col], this.entryTolerance);
            }
        }
        // column constructor
        final Array2DRowRealMatrix m2 = new Array2DRowRealMatrix(this.testVector);
        for (int col = 0; col < m2.getColumnDimension(); col++) {
            Assert.assertEquals("matrix constructor test", m2.getData(false)[0][col],
                    this.testVector[col], this.entryTolerance);
        }
        // errors
        try {
            new Array2DRowRealMatrix(this.empty, false);
            Assert.fail("Expecting NoDataException");
        } catch (final NoDataException e) {
            // expected !
        }
        try {
            new Array2DRowRealMatrix(this.oneEmptyRow, false);
            Assert.fail("Expecting NoDataException");
        } catch (final NoDataException e) {
            // expected !
        }
        try {
            new Array2DRowRealMatrix(null, false);
            Assert.fail("Expecting NullArgumentException");
        } catch (final NullArgumentException e) {
            // expected !
        }
        try {
            new Array2DRowRealMatrix(this.dimensionMismatch, false);
            Assert.fail("Expecting DimensionMismatchException");
        } catch (final DimensionMismatchException e) {
            // expected !
        }
        try {
            new Array2DRowRealMatrix(this.oneEmptyRow, true);
            Assert.fail("Expecting NoDataException");
        } catch (final NoDataException e) {
            // expected !
        }
        try {
            new Array2DRowRealMatrix(this.empty, true);
            Assert.fail("Expecting NoDataException");
        } catch (final NoDataException e) {
            // expected !
        }
        try {
            new Array2DRowRealMatrix(null, true);
            Assert.fail("Expecting NullArgumentException");
        } catch (final NullArgumentException e) {
            // expected !
        }
        try {
            new Array2DRowRealMatrix(this.dimensionMismatch, true);
            Assert.fail("Expecting DimensionMismatchException");
        } catch (final DimensionMismatchException e) {
            // expected !
        }
    }

    /** test dimensions */
    @Test
    public void testDimensions() {
        final Array2DRowRealMatrix m = new Array2DRowRealMatrix(this.testData);
        final Array2DRowRealMatrix m2 = new Array2DRowRealMatrix(this.testData2);
        Assert.assertEquals("testData row dimension", 3, m.getRowDimension());
        Assert.assertEquals("testData column dimension", 3, m.getColumnDimension());
        Assert.assertTrue("testData is square", m.isSquare());
        Assert.assertEquals("testData2 row dimension", m2.getRowDimension(), 2);
        Assert.assertEquals("testData2 column dimension", m2.getColumnDimension(), 3);
        Assert.assertTrue("testData2 is not square", !m2.isSquare());
    }

    /** test symmetric and antisymmetric */
    @Test
    public void testSymmetry() {
        final double threshold = Precision.DOUBLE_COMPARISON_EPSILON;
        final Array2DRowRealMatrix sym = new Array2DRowRealMatrix(this.symmetricData);
        final Array2DRowRealMatrix antiSym = new Array2DRowRealMatrix(this.antisymmetricData);
        final Array2DRowRealMatrix m2 = new Array2DRowRealMatrix(this.testData2);
        Assert.assertTrue("testData2 is not symmetric", !m2.isSymmetric());
        Assert.assertTrue("testData2 is not antisymmetric",
                !m2.isAntisymmetric(threshold, threshold));
        Assert.assertTrue("symmetricData is symmetric", sym.isSymmetric());
        Assert.assertTrue("antisymmetricData is antisymmetric",
                antiSym.isAntisymmetric(threshold, threshold));
        Assert.assertTrue("symmetricData is not antisymmetric",
                !sym.isAntisymmetric(threshold, threshold));
        Assert.assertTrue("antisymmetricData is not symmetric", !antiSym.isSymmetric());
        antiSym.setEntry(2, 2, 1.0);
        Assert.assertTrue("antiSym is'nt antisymmetric anymore",
                !antiSym.isAntisymmetric(threshold, threshold));
    }

    /** test add to entry */
    @Test
    public void testAddToEntry() {
        final Array2DRowRealMatrix m = new Array2DRowRealMatrix(this.testData);
        m.addToEntry(1, 1, 5d);
        for (int row = 0; row < m.getRowDimension(); row++) {
            for (int col = 0; col < m.getColumnDimension(); col++) {
                Assert.assertEquals("matrix constructor test", m.getDataRef()[row][col],
                        this.testDataEntryAdded[row][col], this.entryTolerance);
            }
        }
    }

    /** test multiply entry */
    @Test
    public void testMultiplyEntry() {
        final Array2DRowRealMatrix m = new Array2DRowRealMatrix(this.testData);
        m.multiplyEntry(1, 1, 2d);
        for (int row = 0; row < m.getRowDimension(); row++) {
            for (int col = 0; col < m.getColumnDimension(); col++) {
                Assert.assertEquals("matrix constructor test", m.getDataRef()[row][col],
                        this.testDataEntryAdded[row][col], this.entryTolerance);
            }
        }
    }

    /** test copy functions */
    @Test
    public void testCopyFunctions() {
        final Array2DRowRealMatrix m1 = new Array2DRowRealMatrix(this.testData);
        final Array2DRowRealMatrix m2 = new Array2DRowRealMatrix(m1.getData(false));
        Assert.assertEquals(m2, m1);
        final Array2DRowRealMatrix m3 = new Array2DRowRealMatrix(this.testData);
        final Array2DRowRealMatrix m4 = new Array2DRowRealMatrix(m3.getData(), false);
        Assert.assertEquals(m4, m3);
    }

    /** test add */
    @Test
    public void testAdd() {
        final Array2DRowRealMatrix m = new Array2DRowRealMatrix(this.testData);
        final Array2DRowRealMatrix mInv = new Array2DRowRealMatrix(this.testDataInv);
        final RealMatrix mPlusMInv = m.add(mInv);
        final double[][] sumEntries = mPlusMInv.getData(false);
        for (int row = 0; row < m.getRowDimension(); row++) {
            for (int col = 0; col < m.getColumnDimension(); col++) {
                Assert.assertEquals("sum entry entry", this.testDataPlusInv[row][col],
                        sumEntries[row][col], this.entryTolerance);
            }
        }
    }

    /** test add failure */
    @Test
    public void testAddFail() {
        final Array2DRowRealMatrix m = new Array2DRowRealMatrix(this.testData);
        final Array2DRowRealMatrix m2 = new Array2DRowRealMatrix(this.testData2);
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
        final Array2DRowRealMatrix m = new Array2DRowRealMatrix(this.testData);
        final Array2DRowRealMatrix m2 = new Array2DRowRealMatrix(this.testData2);
        Assert.assertEquals("testData norm", 14d, m.getNorm(), this.entryTolerance);
        Assert.assertEquals("testData2 norm", 7d, m2.getNorm(), this.entryTolerance);
    }

    /** test Frobenius norm */
    @Test
    public void testFrobeniusNorm() {
        final Array2DRowRealMatrix m = new Array2DRowRealMatrix(this.testData);
        final Array2DRowRealMatrix m2 = new Array2DRowRealMatrix(this.testData2);
        Assert.assertEquals("testData Frobenius norm", MathLib.sqrt(117.0), m.getFrobeniusNorm(),
                this.entryTolerance);
        Assert.assertEquals("testData2 Frobenius norm", MathLib.sqrt(52.0), m2.getFrobeniusNorm(),
                this.entryTolerance);
    }

    /** test m-n = m + -n */
    @Test
    public void testPlusMinus() {
        final Array2DRowRealMatrix m = new Array2DRowRealMatrix(this.testData);
        final Array2DRowRealMatrix m2 = new Array2DRowRealMatrix(this.testDataInv);
        TestUtils.assertEquals("m-n = m + -n", m.subtract(m2), m2.scalarMultiply(-1d).add(m),
                this.entryTolerance);
        try {
            m.subtract(new Array2DRowRealMatrix(this.testData2));
            Assert.fail("Expecting illegalArgumentException");
        } catch (final MathIllegalArgumentException ex) {
            // ignored
        }

        Assert.assertTrue(m.equals(new Array2DRowRealMatrix(this.testData)));
        Assert.assertTrue(m2.equals(new Array2DRowRealMatrix(this.testDataInv)));
    }

    /** test multiply */
    @Test
    public void testMultiply() {
        final Array2DRowRealMatrix m = new Array2DRowRealMatrix(this.testData);
        final Array2DRowRealMatrix mInv = new Array2DRowRealMatrix(this.testDataInv);
        final Array2DRowRealMatrix identity = new Array2DRowRealMatrix(this.id);
        final Array2DRowRealMatrix m2 = new Array2DRowRealMatrix(this.testData2);
        TestUtils.assertEquals("inverse multiply", m.multiply(mInv), identity, this.entryTolerance);
        TestUtils.assertEquals("inverse multiply", mInv.multiply(m), identity, this.entryTolerance);
        TestUtils.assertEquals("identity multiply", m.multiply(identity), m, this.entryTolerance);
        TestUtils.assertEquals("identity multiply", identity.multiply(mInv), mInv,
                this.entryTolerance);
        TestUtils.assertEquals("identity multiply", m2.multiply(identity), m2, this.entryTolerance);
        try {
            m.multiply(new Array2DRowRealMatrix(this.bigSingular));
            Assert.fail("Expecting illegalArgumentException");
        } catch (final MathIllegalArgumentException ex) {
            // ignored
        }

        Assert.assertTrue(m.equals(new Array2DRowRealMatrix(this.testData)));
        Assert.assertTrue(mInv.equals(new Array2DRowRealMatrix(this.testDataInv)));
        Assert.assertTrue(identity.equals(new Array2DRowRealMatrix(this.id)));
        Assert.assertTrue(m2.equals(new Array2DRowRealMatrix(this.testData2)));

        Assert.assertTrue(m.multiply(mInv, false).equals(identity));
        Assert.assertTrue(m.multiply(mInv, false, 2.).equals(identity.scalarMultiply(2.)));

        final double[][] resTranspose = { { 14d, 21d, 25d }, { 21d, 38d, 26d }, { 25d, 26d, 65d } };
        final Array2DRowRealMatrix mResTranspose = new Array2DRowRealMatrix(resTranspose);

        Assert.assertTrue(m.multiply(m, true).equals(mResTranspose));

        final BlockRealMatrix array = new BlockRealMatrix(this.testData);
        Assert.assertTrue(m.multiply(array, true).equals(mResTranspose));
        Assert.assertTrue(m.multiply(array, true, 2.).equals(mResTranspose.scalarMultiply(2.)));

        // Non-square matrix with transposition
        final double[][] arraym = new double[][] { { 1, 2, 3 }, { 4, 5, 6 } };
        final RealMatrix mat = new Array2DRowRealMatrix(arraym);
        final RealMatrix result = mat.multiply(mat, true);
        final RealMatrix expected = new Array2DRowRealMatrix(new double[][] { { 14, 32 },
                { 32, 77 } });
        Assert.assertTrue(result.equals(expected));
    }

    // Additional Test for Array2DRowRealMatrixTest.testMultiply

    private final double[][] d3 = new double[][] { { 1, 2, 3, 4 }, { 5, 6, 7, 8 } };
    private final double[][] d4 = new double[][] { { 1 }, { 2 }, { 3 }, { 4 } };
    private final double[][] d5 = new double[][] { { 30 }, { 70 } };

    @Test
    public void testMultiply2() {
        final RealMatrix m3 = new Array2DRowRealMatrix(this.d3);
        final RealMatrix m4 = new Array2DRowRealMatrix(this.d4);
        final RealMatrix m5 = new Array2DRowRealMatrix(this.d5);
        TestUtils.assertEquals("m3*m4=m5", m3.multiply(m4), m5, this.entryTolerance);
    }

    @Test
    public void testPower() {
        final Array2DRowRealMatrix m = new Array2DRowRealMatrix(this.testData);
        final Array2DRowRealMatrix mInv = new Array2DRowRealMatrix(this.testDataInv);
        final Array2DRowRealMatrix mPlusInv = new Array2DRowRealMatrix(this.testDataPlusInv);
        final Array2DRowRealMatrix identity = new Array2DRowRealMatrix(this.id);

        TestUtils.assertEquals("m^0", m.power(0), identity, this.entryTolerance);
        TestUtils.assertEquals("mInv^0", mInv.power(0), identity, this.entryTolerance);
        TestUtils.assertEquals("mPlusInv^0", mPlusInv.power(0), identity, this.entryTolerance);

        TestUtils.assertEquals("m^1", m.power(1), m, this.entryTolerance);
        TestUtils.assertEquals("mInv^1", mInv.power(1), mInv, this.entryTolerance);
        TestUtils.assertEquals("mPlusInv^1", mPlusInv.power(1), mPlusInv, this.entryTolerance);

        RealMatrix C1 = m.copy();
        RealMatrix C2 = mInv.copy();
        RealMatrix C3 = mPlusInv.copy();

        for (int i = 2; i <= 10; ++i) {
            C1 = C1.multiply(m);
            C2 = C2.multiply(mInv);
            C3 = C3.multiply(mPlusInv);

            TestUtils.assertEquals("m^" + i, m.power(i), C1, this.entryTolerance);
            TestUtils.assertEquals("mInv^" + i, mInv.power(i), C2, this.entryTolerance);
            TestUtils.assertEquals("mPlusInv^" + i, mPlusInv.power(i), C3, this.entryTolerance);
        }

        try {
            final Array2DRowRealMatrix mNotSquare = new Array2DRowRealMatrix(this.testData2T);
            mNotSquare.power(2);
            Assert.fail("Expecting NonSquareMatrixException");
        } catch (final NonSquareMatrixException ex) {
            // ignored
        }

        try {
            m.power(-1);
            Assert.fail("Expecting IllegalArgumentException");
        } catch (final IllegalArgumentException ex) {
            // ignored
        }
    }

    /** test trace */
    @Test
    public void testTrace() {
        RealMatrix m = new Array2DRowRealMatrix(this.id);
        Assert.assertEquals("identity trace", 3d, m.getTrace(), this.entryTolerance);
        m = new Array2DRowRealMatrix(this.testData2);
        try {
            m.getTrace();
            Assert.fail("Expecting NonSquareMatrixException");
        } catch (final NonSquareMatrixException ex) {
            // ignored
        }
    }

    /** test sclarAdd */
    @Test
    public void testScalarAdd() {
        final RealMatrix m = new Array2DRowRealMatrix(this.testData);
        TestUtils.assertEquals("scalar add", new Array2DRowRealMatrix(this.testDataPlus2),
                m.scalarAdd(2d), this.entryTolerance);
    }

    /** test operate */
    @Test
    public void testOperate() {
        RealMatrix m = new Array2DRowRealMatrix(this.id);
        TestUtils.assertEquals("identity operate", this.testVector, m.operate(this.testVector),
                this.entryTolerance);
        TestUtils.assertEquals("identity operate", this.testVector,
                m.operate(new ArrayRealVector(this.testVector)).toArray(), this.entryTolerance);
        m = new Array2DRowRealMatrix(this.bigSingular);
        try {
            m.operate(this.testVector);
            Assert.fail("Expecting illegalArgumentException");
        } catch (final MathIllegalArgumentException ex) {
            // ignored
        }
    }

    /** test issue MATH-209 */
    @Test
    public void testMath209() {
        final RealMatrix a = new Array2DRowRealMatrix(
                new double[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } }, false);
        final double[] b = a.operate(new double[] { 1, 1 });
        Assert.assertEquals(a.getRowDimension(), b.length);
        Assert.assertEquals(3.0, b[0], 1.0e-12);
        Assert.assertEquals(7.0, b[1], 1.0e-12);
        Assert.assertEquals(11.0, b[2], 1.0e-12);
    }

    /** test transpose */
    @Test
    public void testTranspose() {
        RealMatrix m = new Array2DRowRealMatrix(this.testData);
        final RealMatrix mIT = new LUDecomposition(m).getSolver().getInverse().transpose();
        final RealMatrix mTI = new LUDecomposition(m.transpose()).getSolver().getInverse();
        TestUtils.assertEquals("inverse-transpose", mIT, mTI, this.normTolerance);
        m = new Array2DRowRealMatrix(this.testData2);
        final RealMatrix mt = new Array2DRowRealMatrix(this.testData2T);
        TestUtils.assertEquals("transpose", mt, m.transpose(false), this.normTolerance);
    }

    /** test preMultiply by vector */
    @Test
    public void testPremultiplyVector() {
        RealMatrix m = new Array2DRowRealMatrix(this.testData);
        TestUtils.assertEquals("premultiply", m.preMultiply(this.testVector), this.preMultTest,
                this.normTolerance);
        TestUtils.assertEquals("premultiply",
                m.preMultiply(new ArrayRealVector(this.testVector).toArray()), this.preMultTest,
                this.normTolerance);
        m = new Array2DRowRealMatrix(this.bigSingular);
        try {
            m.preMultiply(this.testVector);
            Assert.fail("expecting MathIllegalArgumentException");
        } catch (final MathIllegalArgumentException ex) {
            // ignored
        }
    }

    @Test
    public void testPremultiply() {
        final RealMatrix m3 = new Array2DRowRealMatrix(this.d3);
        final RealMatrix m4 = new Array2DRowRealMatrix(this.d4);
        final RealMatrix m5 = new Array2DRowRealMatrix(this.d5);
        TestUtils.assertEquals("m3*m4=m5", m4.preMultiply(m3), m5, this.entryTolerance);

        final Array2DRowRealMatrix m = new Array2DRowRealMatrix(this.testData);
        final Array2DRowRealMatrix mInv = new Array2DRowRealMatrix(this.testDataInv);
        final Array2DRowRealMatrix identity = new Array2DRowRealMatrix(this.id);
        TestUtils.assertEquals("inverse multiply", m.preMultiply(mInv), identity,
                this.entryTolerance);
        TestUtils.assertEquals("inverse multiply", mInv.preMultiply(m), identity,
                this.entryTolerance);
        TestUtils
                .assertEquals("identity multiply", m.preMultiply(identity), m, this.entryTolerance);
        TestUtils.assertEquals("identity multiply", identity.preMultiply(mInv), mInv,
                this.entryTolerance);
        try {
            m.preMultiply(new Array2DRowRealMatrix(this.bigSingular));
            Assert.fail("Expecting illegalArgumentException");
        } catch (final MathIllegalArgumentException ex) {
            // ignored
        }
    }

    @Test
    public void testGetVectors() {
        final RealMatrix m = new Array2DRowRealMatrix(this.testData);
        TestUtils.assertEquals("get row", m.getRow(0), this.testDataRow1, this.entryTolerance);
        TestUtils.assertEquals("get col", m.getColumn(2), this.testDataCol3, this.entryTolerance);
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
        final RealMatrix m = new Array2DRowRealMatrix(this.testData);
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
        final RealMatrix m = new Array2DRowRealMatrix(matrixData);
        // One more with three rows, two columns
        final double[][] matrixData2 = { { 1d, 2d }, { 2d, 5d }, { 1d, 7d } };
        final RealMatrix n = new Array2DRowRealMatrix(matrixData2);
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
        final RealMatrix coefficients = new Array2DRowRealMatrix(coefficientsData);
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
        final RealMatrix m = new Array2DRowRealMatrix(this.subTestData);
        this.checkGetSubMatrix(m, this.subRows23Cols00, 2, 3, 0, 0, false);
        this.checkGetSubMatrix(m, this.subRows00Cols33, 0, 0, 3, 3, false);
        this.checkGetSubMatrix(m, this.subRows01Cols23, 0, 1, 2, 3, false);
        this.checkGetSubMatrix(m, this.subRows02Cols13, new int[] { 0, 2 }, new int[] { 1, 3 },
                false);
        this.checkGetSubMatrix(m, this.subRows03Cols12, new int[] { 0, 3 }, new int[] { 1, 2 },
                false);
        this.checkGetSubMatrix(m, this.subRows03Cols123, new int[] { 0, 3 }, new int[] { 1, 2, 3 },
                false);
        this.checkGetSubMatrix(m, this.subRows20Cols123, new int[] { 2, 0 }, new int[] { 1, 2, 3 },
                false);
        this.checkGetSubMatrix(m, this.subRows31Cols31, new int[] { 3, 1 }, new int[] { 3, 1 },
                false);
        this.checkGetSubMatrix(m, this.subRows31Cols31, new int[] { 3, 1 }, new int[] { 3, 1 },
                false);
        this.checkGetSubMatrix(m, null, 1, 0, 2, 4, true);
        this.checkGetSubMatrix(m, null, -1, 1, 2, 2, true);
        this.checkGetSubMatrix(m, null, 1, 0, 2, 2, true);
        this.checkGetSubMatrix(m, null, 1, 0, 2, 4, true);
        this.checkGetSubMatrix(m, null, new int[] {}, new int[] { 0 }, true);
        this.checkGetSubMatrix(m, null, new int[] { 0 }, new int[] { 4 }, true);
    }

    private void checkGetSubMatrix(final RealMatrix m, final double[][] reference,
            final int startRow, final int endRow, final int startColumn, final int endColumn,
            final boolean mustFail) {
        try {
            final RealMatrix sub = m.getSubMatrix(startRow, endRow, startColumn, endColumn);
            Assert.assertEquals(new Array2DRowRealMatrix(reference), sub);
            if (mustFail) {
                Assert.fail("Expecting OutOfRangeException or NumberIsTooSmallException or NoDataException");
            }
        } catch (final OutOfRangeException e) {
            if (!mustFail) {
                throw e;
            }
        } catch (final NumberIsTooSmallException e) {
            if (!mustFail) {
                throw e;
            }
        } catch (final NoDataException e) {
            if (!mustFail) {
                throw e;
            }
        }
    }

    private void checkGetSubMatrix(final RealMatrix m, final double[][] reference,
            final int[] selectedRows, final int[] selectedColumns, final boolean mustFail) {
        try {
            final RealMatrix sub = m.getSubMatrix(selectedRows, selectedColumns);
            Assert.assertEquals(new Array2DRowRealMatrix(reference), sub);
            if (mustFail) {
                Assert.fail("Expecting OutOfRangeException or NumberIsTooSmallException or NoDataException");
            }
        } catch (final OutOfRangeException e) {
            if (!mustFail) {
                throw e;
            }
        } catch (final NumberIsTooSmallException e) {
            if (!mustFail) {
                throw e;
            }
        } catch (final NoDataException e) {
            if (!mustFail) {
                throw e;
            }
        }
    }

    @Test
    public void testCopySubMatrix() {
        final RealMatrix m = new Array2DRowRealMatrix(this.subTestData);
        this.checkCopy(m, this.subRows23Cols00, 2, 3, 0, 0, false);
        this.checkCopy(m, this.subRows00Cols33, 0, 0, 3, 3, false);
        this.checkCopy(m, this.subRows01Cols23, 0, 1, 2, 3, false);
        this.checkCopy(m, this.subRows02Cols13, new int[] { 0, 2 }, new int[] { 1, 3 }, false);
        this.checkCopy(m, this.subRows03Cols12, new int[] { 0, 3 }, new int[] { 1, 2 }, false);
        this.checkCopy(m, this.subRows03Cols123, new int[] { 0, 3 }, new int[] { 1, 2, 3 }, false);
        this.checkCopy(m, this.subRows20Cols123, new int[] { 2, 0 }, new int[] { 1, 2, 3 }, false);
        this.checkCopy(m, this.subRows31Cols31, new int[] { 3, 1 }, new int[] { 3, 1 }, false);
        this.checkCopy(m, this.subRows31Cols31, new int[] { 3, 1 }, new int[] { 3, 1 }, false);

        double[][] destination = { { 1, 2, 2, }, { 0, 2, 0 } };
        final double[][] ref = { { 2, 2, 2 }, { 4, 2, 0 } };
        this.checkCopy(m, destination, ref, 2, 3, 0, 0, 0, 0, false);
        this.checkCopy(m, destination, ref, 0, 0, 0, 3, 0, 0, true);
        this.checkCopy(m, new double[][] { { 0, 1 }, { 1 } }, ref, 0, 1, 0, 1, 0, 0, true);

        destination = new double[][] { { 1, 2, 2, }, { 0, 2, 0 } };
        this.checkCopy(m, destination, ref, new int[] { 2, 3 }, new int[] { 0 }, 0, 0, false);
        this.checkCopy(m, destination, ref, new int[] { 0 }, new int[] { 0, 1, 2, 3 }, 0, 0, true);
        this.checkCopy(m, new double[][] { { 0, 1 }, { 1 } }, ref, new int[] { 0, 1 }, new int[] {
                0, 1 }, 0, 0, true);

        this.checkCopy(m, null, 1, 0, 2, 4, true);
        this.checkCopy(m, null, -1, 1, 2, 2, true);
        this.checkCopy(m, null, 1, 0, 2, 2, true);
        this.checkCopy(m, null, 1, 0, 2, 4, true);
        this.checkCopy(m, null, new int[] {}, new int[] { 0 }, true);
        this.checkCopy(m, null, new int[] { 0 }, new int[] { 4 }, true);
    }

    @Test
    public void testCopySubMatrixException() {
        final RealMatrix m = new Array2DRowRealMatrix(this.subTestData);
        try {
            m.copySubMatrix(new int[] { 0, 1 }, new int[] { 0, 2 }, new double[2][1]);
            Assert.fail("Expecting MatrixDimensionMismatchException");
        } catch (final MatrixDimensionMismatchException e) {
            // Expected
        }
    }

    private void checkCopy(final RealMatrix m, final double[][] reference, final int startRow,
            final int endRow, final int startColumn, final int endColumn, final boolean mustFail) {
        try {
            final double[][] sub = (reference == null) ? new double[1][1]
                    : new double[reference.length][reference[0].length];
            m.copySubMatrix(startRow, endRow, startColumn, endColumn, sub);
            Assert.assertEquals(new Array2DRowRealMatrix(reference), new Array2DRowRealMatrix(sub));
            if (mustFail) {
                Assert.fail("Expecting OutOfRangeException or NumberIsTooSmallException or NoDataException");
            }
        } catch (final OutOfRangeException e) {
            if (!mustFail) {
                throw e;
            }
        } catch (final NumberIsTooSmallException e) {
            if (!mustFail) {
                throw e;
            }
        } catch (final NoDataException e) {
            if (!mustFail) {
                throw e;
            }
        }
    }

    private void checkCopy(final RealMatrix m, final double[][] destination,
            final double[][] reference, final int startRow, final int endRow,
            final int startColumn, final int endColumn, final int startRowDest,
            final int startColumnDest, final boolean mustFail) {
        try {
            if (destination == null || reference == null) {
                Assert.fail("Destination and reference data is needed");
            }
            m.copySubMatrix(startRow, endRow, startColumn, endColumn, destination, startRowDest,
                    startColumnDest);
            Assert.assertEquals(new Array2DRowRealMatrix(reference), new Array2DRowRealMatrix(
                    destination));
            if (mustFail) {
                Assert.fail("Expecting OutOfRangeException or NumberIsTooSmallException or NoDataException");
            }
        } catch (final MatrixDimensionMismatchException e) {
            if (!mustFail) {
                throw e;
            }
        } catch (final NumberIsTooSmallException e) {
            if (!mustFail) {
                throw e;
            }
        }
    }

    private void checkCopy(final RealMatrix m, final double[][] reference,
            final int[] selectedRows, final int[] selectedColumns, final boolean mustFail) {
        try {
            final double[][] sub = (reference == null) ? new double[1][1]
                    : new double[reference.length][reference[0].length];
            m.copySubMatrix(selectedRows, selectedColumns, sub);
            Assert.assertEquals(new Array2DRowRealMatrix(reference), new Array2DRowRealMatrix(sub));
            if (mustFail) {
                Assert.fail("Expecting OutOfRangeException or NumberIsTooSmallException or NoDataException");
            }
        } catch (final OutOfRangeException e) {
            if (!mustFail) {
                throw e;
            }
        } catch (final NumberIsTooSmallException e) {
            if (!mustFail) {
                throw e;
            }
        } catch (final NoDataException e) {
            if (!mustFail) {
                throw e;
            }
        }
    }

    private void checkCopy(final RealMatrix m, final double[][] destination,
            final double[][] reference, final int[] selectedRows, final int[] selectedColumns,
            final int startRowDest, final int startColumnDest, final boolean mustFail) {
        try {
            if (destination == null || reference == null) {
                Assert.fail("Destination and reference data is needed");
            }
            m.copySubMatrix(selectedRows, selectedColumns, destination, startRowDest,
                    startColumnDest);
            Assert.assertEquals(new Array2DRowRealMatrix(reference), new Array2DRowRealMatrix(
                    destination));
            if (mustFail) {
                Assert.fail("Expecting OutOfRangeException or NumberIsTooSmallException or NoDataException");
            }
        } catch (final MatrixDimensionMismatchException e) {
            if (!mustFail) {
                throw e;
            }
        } catch (final NumberIsTooSmallException e) {
            if (!mustFail) {
                throw e;
            }
        }
    }

    @Test
    public void testGetRowMatrix() {
        final RealMatrix m = new Array2DRowRealMatrix(this.subTestData);
        final RealMatrix mRow0 = new Array2DRowRealMatrix(this.subRow0);
        final RealMatrix mRow3 = new Array2DRowRealMatrix(this.subRow3);
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
        final RealMatrix m = new Array2DRowRealMatrix(this.subTestData);
        final RealMatrix mRow3 = new Array2DRowRealMatrix(this.subRow3);
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
    }

    @Test
    public void testGetColumnMatrix() {
        final RealMatrix m = new Array2DRowRealMatrix(this.subTestData);
        final RealMatrix mColumn1 = new Array2DRowRealMatrix(this.subColumn1);
        final RealMatrix mColumn3 = new Array2DRowRealMatrix(this.subColumn3);
        Assert.assertEquals("Column1", mColumn1, m.getColumnMatrix(1));
        Assert.assertEquals("Column3", mColumn3, m.getColumnMatrix(3));
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
        final RealMatrix m = new Array2DRowRealMatrix(this.subTestData);
        final RealMatrix mColumn3 = new Array2DRowRealMatrix(this.subColumn3);
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
    }

    @Test
    public void testGetRowVector() {
        final RealMatrix m = new Array2DRowRealMatrix(this.subTestData);
        final RealVector mRow0 = new ArrayRealVector(this.subRow0[0]);
        final RealVector mRow3 = new ArrayRealVector(this.subRow3[0]);
        Assert.assertEquals("Row0", mRow0, m.getRowVector(0));
        Assert.assertEquals("Row3", mRow3, m.getRowVector(3));
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
        final RealMatrix m = new Array2DRowRealMatrix(this.subTestData);
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
    public void testGetColumnVector() {
        final RealMatrix m = new Array2DRowRealMatrix(this.subTestData);
        final RealVector mColumn1 = this.columnToVector(this.subColumn1);
        final RealVector mColumn3 = this.columnToVector(this.subColumn3);
        Assert.assertEquals("Column1", mColumn1, m.getColumnVector(1));
        Assert.assertEquals("Column3", mColumn3, m.getColumnVector(3));
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
        final RealMatrix m = new Array2DRowRealMatrix(this.subTestData);
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

    private RealVector columnToVector(final double[][] column) {
        final double[] data = new double[column.length];
        for (int i = 0; i < data.length; ++i) {
            data[i] = column[i][0];
        }
        return new ArrayRealVector(data, false);
    }

    @Test
    public void testGetRow() {
        final RealMatrix m = new Array2DRowRealMatrix(this.subTestData);
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
        final RealMatrix m = new Array2DRowRealMatrix(this.subTestData);
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
    public void testGetColumn() {
        final RealMatrix m = new Array2DRowRealMatrix(this.subTestData);
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
        final RealMatrix m = new Array2DRowRealMatrix(this.subTestData);
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
        final Array2DRowRealMatrix m = new Array2DRowRealMatrix(this.testData);
        final Array2DRowRealMatrix m1 = (Array2DRowRealMatrix) m.copy();
        final Array2DRowRealMatrix mt = (Array2DRowRealMatrix) m.transpose();
        Assert.assertTrue(m.hashCode() != mt.hashCode());
        Assert.assertEquals(m.hashCode(), m1.hashCode());
        Assert.assertEquals(m, m);
        Assert.assertEquals(m, m1);
        Assert.assertFalse(m.equals(null));
        Assert.assertFalse(m.equals(mt));
        Assert.assertFalse(m.equals(new Array2DRowRealMatrix(this.bigSingular)));
    }

    @Test
    public void testToString() {
        StringBuilder builder;
        Array2DRowRealMatrix m;

        // Line separator
        // (the default matrix format uses "\n" instead of System.lineSeparator())
        final String lineSeparator = "\n";

        // Multi-row matrix
        m = new Array2DRowRealMatrix(this.testData);
        builder = new StringBuilder();
        builder.append("Array2DRowRealMatrix[[      1.0000,      2.0000,      3.0000]");
        builder.append(lineSeparator);
        builder.append("                     [      2.0000,      5.0000,      3.0000]");
        builder.append(lineSeparator);
        builder.append("                     [      1.0000,      0.0000,      8.0000]]");
        Assert.assertEquals(builder.toString(), m.toString());

        // Empty matrix
        m = new Array2DRowRealMatrix();
        builder = new StringBuilder();
        builder.append("Array2DRowRealMatrix[]");
        Assert.assertEquals(builder.toString(), m.toString());
    }

    @Test
    public void testSetSubMatrix() {
        final Array2DRowRealMatrix m = new Array2DRowRealMatrix(this.testData);
        m.setSubMatrix(this.detData2, 1, 1);
        RealMatrix expected = MatrixUtils.createRealMatrix(new double[][] { { 1.0, 2.0, 3.0 },
                { 2.0, 1.0, 3.0 }, { 1.0, 2.0, 4.0 } });
        Assert.assertEquals(expected, m);

        m.setSubMatrix(this.detData2, 0, 0);
        expected = MatrixUtils.createRealMatrix(new double[][] { { 1.0, 3.0, 3.0 },
                { 2.0, 4.0, 3.0 }, { 1.0, 2.0, 4.0 } });
        Assert.assertEquals(expected, m);

        m.setSubMatrix(this.testDataPlus2, 0, 0);
        expected = MatrixUtils.createRealMatrix(new double[][] { { 3.0, 4.0, 5.0 },
                { 4.0, 7.0, 5.0 }, { 3.0, 2.0, 10.0 } });
        Assert.assertEquals(expected, m);

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
        final Array2DRowRealMatrix m2 = new Array2DRowRealMatrix();
        try {
            m2.setSubMatrix(this.testData, 0, 1);
            Assert.fail("expecting MathIllegalStateException");
        } catch (final MathIllegalStateException e) {
            // expected
        }
        try {
            m2.setSubMatrix(this.testData, 1, 0);
            Assert.fail("expecting MathIllegalStateException");
        } catch (final MathIllegalStateException e) {
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
        try {
            m.setSubMatrix(this.empty, 0, 0);
            Assert.fail("expecting NoDataException");
        } catch (final NoDataException e) {
            // expected
        }
    }

    @Test
    public void testWalk() {
        final int rows = 150;
        final int columns = 75;

        RealMatrix m = new Array2DRowRealMatrix(rows, columns);
        m.walkInRowOrder(new SetVisitor());
        GetVisitor getVisitor = new GetVisitor();
        m.walkInOptimizedOrder(getVisitor);
        Assert.assertEquals(rows * columns, getVisitor.getCount());

        m = new Array2DRowRealMatrix(rows, columns);
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

        m = new Array2DRowRealMatrix(rows, columns);
        m.walkInColumnOrder(new SetVisitor());
        getVisitor = new GetVisitor();
        m.walkInOptimizedOrder(getVisitor);
        Assert.assertEquals(rows * columns, getVisitor.getCount());

        m = new Array2DRowRealMatrix(rows, columns);
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

        m = new Array2DRowRealMatrix(rows, columns);
        m.walkInOptimizedOrder(new SetVisitor());
        getVisitor = new GetVisitor();
        m.walkInRowOrder(getVisitor);
        Assert.assertEquals(rows * columns, getVisitor.getCount());

        m = new Array2DRowRealMatrix(rows, columns);
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

        m = new Array2DRowRealMatrix(rows, columns);
        m.walkInOptimizedOrder(new SetVisitor());
        getVisitor = new GetVisitor();
        m.walkInColumnOrder(getVisitor);
        Assert.assertEquals(rows * columns, getVisitor.getCount());

        m = new Array2DRowRealMatrix(rows, columns);
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
        final Array2DRowRealMatrix m = new Array2DRowRealMatrix(this.testData);
        Assert.assertEquals(m, TestUtils.serializeAndRecover(m));
    }

    /**
     * Here is the test of the method that indicates if a matrix is orthogonal.
     * The method is tested on a non square matrix, an orthogonal matrix,
     * a non orthogonal matrix and a matrix given by the object Rotation.
     * The test is done with several thresholds.
     */
    @Test
    public void testIsOrthogonal() {
        // case : non square matrix
        final RealMatrix nonSquareMatrix = new Array2DRowRealMatrix(2, 3);
        Assert.assertFalse(nonSquareMatrix.isDiagonal(Precision.EPSILON));

        // case : orthogonal matrix
        final double[][] orthogonalMatrix = { { 8.0 / 9.0, 1.0 / 9.0, -4.0 / 9.0 },
                { -4.0 / 9.0, 4.0 / 9.0, -7.0 / 9.0 }, { 1.0 / 9.0, 8.0 / 9.0, 4.0 / 9.0 } };
        final RealMatrix matrix1 = new Array2DRowRealMatrix(orthogonalMatrix);
        Assert.assertTrue(matrix1.isOrthogonal(Precision.EPSILON, Precision.EPSILON));

        // case : non orthogonal matrix (the column vectors are thus orthogonal with
        // each other but non normalized)
        final double[][] nonOrthogonalMatrix = { { 8.0, 1.0, -4.0 }, { -4.0, 4.0, -7.0 },
                { 1.0, 8.0, 4.0 } };
        final RealMatrix matrix2 = new Array2DRowRealMatrix(nonOrthogonalMatrix);
        Assert.assertFalse(matrix2.isOrthogonal(Precision.EPSILON, Precision.EPSILON));

        // case : non orthogonal matrix (the column vectors are non orthogonal)
        final double[][] nonOrthogonalMatrix2 = { { 8.0 / 9.0, 1.0 / 9.0, -4.0 / 9.0 },
                { -4.0 / 9.0, 4.0 / 9.0, -7.0 / 9.0 }, { 2.0 / 9.0, 8.0 / 9.0, 4.0 / 9.0 } };
        final RealMatrix matrix3 = new Array2DRowRealMatrix(nonOrthogonalMatrix2);
        Assert.assertFalse(matrix3.isOrthogonal(Precision.EPSILON, Precision.EPSILON));

        // case : almost orthogonal matrix
        final double[][] almostOrthogonalMatrix = { { 8.0 / 9.0, 1.0 / 9.0, -4.0 / 9.0 },
                { -4.0000001 / 9.0, 4.0 / 9.0, -7.0 / 9.0 }, { 1.0 / 9.0, 8.0 / 9.0, 4.0 / 9.0 } };
        final RealMatrix matrix4 = new Array2DRowRealMatrix(almostOrthogonalMatrix);
        // with a threshold of 1E-6
        Assert.assertTrue(matrix4.isOrthogonal(1E-6, 1E-6));
        // with an implicit threshold of 1E-16
        Assert.assertFalse(matrix4.isOrthogonal(Precision.EPSILON, Precision.EPSILON));
        // with a threshold of 1E-16 (for the normality) and 1E-6
        // (for the orthogonality)
        Assert.assertFalse(matrix4.isOrthogonal(1E-16, 1E-6));
        // with a threshold of 1E-16
        Assert.assertFalse(matrix4.isOrthogonal(1E-6, 1E-16));

        // case : rotation matrix
        final Rotation rotation = new Rotation(true, -1, 5.8, 7, 9.2);
        final double[][] rotationMatrix = rotation.getMatrix();
        final RealMatrix matrix5 = new Array2DRowRealMatrix(rotationMatrix);
        Assert.assertTrue(matrix5.isOrthogonal(1E-6, 1E-6));

        // case : almost orthogonal matrix
        final double alpha = MathLib.toRadians(45);
        final double[][] rotationMatrixBis = { { MathLib.cos(alpha), -MathLib.sin(alpha), 0.0 },
                { MathLib.sin(alpha), MathLib.cos(alpha), 0.0 }, { 0.0, 0.0, 1.0 } };
        final RealMatrix matrix6 = new Array2DRowRealMatrix(rotationMatrixBis);
        // with a threshold of 1E-6
        Assert.assertTrue(matrix6.isOrthogonal(1E-16, 1E-16));

        final double[][] nonRotationMatrixBis = { { MathLib.cos(alpha), -MathLib.sin(alpha), 0.0 },
                { MathLib.sin(alpha), MathLib.cos(alpha), 0.0 }, { 0.0, 0.0, 1.01 } };
        final RealMatrix matrix7 = new Array2DRowRealMatrix(nonRotationMatrixBis);
        // with a threshold of 1E-6
        Assert.assertFalse(matrix7.isOrthogonal(1E-16, 1E-16));
    }

    /**
     * Here is the test of the method that indicates if a matrix is diagonal.
     * The method is tested on a non square matrix, a diagonal matrix and
     * a non diagonal matrix.
     * The test is done with several thresholds.
     */
    @Test
    public void testIsDiagonal() {
        // case : non square matrix
        final RealMatrix nonSquareMatrix = new Array2DRowRealMatrix(2, 3);
        Assert.assertFalse(nonSquareMatrix.isDiagonal(Precision.EPSILON));

        // case : diagonal matrix
        final double[][] diagonalMatrix = { { 4.0, 0.0, 0.0, 0.0 }, { 0.0, -1.0, 0.0, 0.0 },
                { 0.0, 0.0, 5.0, 0.0 }, { 0.0, 0.0, 0.0, 9.0 } };
        final RealMatrix matrix1 = new Array2DRowRealMatrix(diagonalMatrix);
        Assert.assertTrue(matrix1.isDiagonal(Precision.EPSILON));

        // case : almost diagonal matrix
        final double[][] nonDiagonalMatrix = { { 4.0, 0.0, 0.0, 0.0 }, { 0.0, -1.0, 0.00001, 0.0 },
                { 0.0, 0.0, 5.0, 0.0 }, { 0.0, 0.0, 0.0, 9.0 } };
        final RealMatrix matrix2 = new Array2DRowRealMatrix(nonDiagonalMatrix);
        Assert.assertFalse(matrix2.isDiagonal(Precision.EPSILON));
        Assert.assertTrue(matrix2.isDiagonal(1E-4));
    }

    /**
     * Here is the test of the method that indicates if a matrix is invertible.
     * The method is tested on a non square matrix, a singular matrix and on
     * a non singular matrix.
     * The test is done with several thresholds.
     */
    @Test
    public void testIsInvertible() {
        // case : non square matrix
        final RealMatrix nonSquareMatrix = new Array2DRowRealMatrix(2, 3);
        Assert.assertFalse(nonSquareMatrix.isInvertible(Precision.EPSILON));

        // case : singular matrix
        final double[][] singularMatrix1 = { { 4.0, 2.0, -1.5, 2.0 }, { 5.0, 8.0, 2.1, 2.5 },
                { 2.0, 1.0, -0.75, 1.0 }, { -1.0, 0.0, 0.0, -0.5 } };
        final RealMatrix matrix1 = new Array2DRowRealMatrix(singularMatrix1);
        Assert.assertFalse(matrix1.isInvertible(Precision.EPSILON));

        // case : singular matrix
        final double[][] singularMatrix2 = { { 4.0, 2.0, -1.5, 0.0 }, { 5.0, 8.0, 2.1, 0.0 },
                { 2.0, 1.0, -0.75, 0.0 }, { -1.0, 0.0, 0.0, 0.0 } };
        final RealMatrix matrix2 = new Array2DRowRealMatrix(singularMatrix2);
        Assert.assertFalse(matrix2.isInvertible(Precision.EPSILON));

        // case : singular matrix
        final double[][] singularMatrix3 = { { 4.0, 2.0, -1.5, 2.0 }, { 5.00001, 8.0, 2.1, 2.5 },
                { 2.0, 1.0, -0.75, 1.0 }, { -1.0, 0.0, 0.0, -0.5 } };
        final RealMatrix matrix3 = new Array2DRowRealMatrix(singularMatrix3);
        Assert.assertFalse(matrix3.isInvertible(1E-4));
        Assert.assertTrue(matrix3.isInvertible(Precision.EPSILON));

        // case : non singular matrix
        final double[][] nonSingularMatrix = { { 4.0, 2.0, -1.5, 2.0 }, { 6.0, 8.0, 2.1, 2.5 },
                { 2.0, 1.0, -0.75, 1.0 }, { -1.0, 0.0, 0.0, -0.5 } };
        final RealMatrix matrix4 = new Array2DRowRealMatrix(nonSingularMatrix);
        Assert.assertTrue(matrix4.isInvertible(Precision.EPSILON));
    }

    /**
     * For coverage purpose, checks the matrix dimension in first method copySubMatrix
     * if ((destination.length < rowsCount) || (destination[0].length < columnsCount))
     */
    @Test(expected = MatrixDimensionMismatchException.class)
    public void testMatrixDimensionMismatchExceptionCopySubMatrix() {
        final double[][] v = { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } };
        final double[][] destination = new double[1][1];
        final Array2DRowRealMatrix m = new Array2DRowRealMatrix(v);
        m.copySubMatrix(1, 2, 1, 2, destination);
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

    /** extracts the l and u matrices from compact lu representation */
    protected void splitLU(final RealMatrix lu, final double[][] lowerData,
            final double[][] upperData) {
        if (!lu.isSquare()) {
            throw new NonSquareMatrixException(lu.getRowDimension(), lu.getColumnDimension());
        }
        if (lowerData.length != lowerData[0].length) {
            throw new DimensionMismatchException(lowerData.length, lowerData[0].length);
        }
        if (upperData.length != upperData[0].length) {
            throw new DimensionMismatchException(upperData.length, upperData[0].length);
        }
        if (lowerData.length != upperData.length) {
            throw new DimensionMismatchException(lowerData.length, upperData.length);
        }
        if (lowerData.length != lu.getRowDimension()) {
            throw new DimensionMismatchException(lowerData.length, lu.getRowDimension());
        }

        final int n = lu.getRowDimension();
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (j < i) {
                    lowerData[i][j] = lu.getEntry(i, j);
                    upperData[i][j] = 0d;
                } else if (i == j) {
                    lowerData[i][j] = 1d;
                    upperData[i][j] = lu.getEntry(i, j);
                } else {
                    lowerData[i][j] = 0d;
                    upperData[i][j] = lu.getEntry(i, j);
                }
            }
        }
    }

    /** Returns the result of applying the given row permutation to the matrix */
    protected RealMatrix permuteRows(final RealMatrix matrix, final int[] permutation) {
        if (!matrix.isSquare()) {
            throw new NonSquareMatrixException(matrix.getRowDimension(),
                    matrix.getColumnDimension());
        }
        if (matrix.getRowDimension() != permutation.length) {
            throw new DimensionMismatchException(matrix.getRowDimension(), permutation.length);
        }

        final int n = matrix.getRowDimension();
        final int m = matrix.getColumnDimension();
        final double out[][] = new double[m][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                out[i][j] = matrix.getEntry(permutation[i], j);
            }
        }
        return new Array2DRowRealMatrix(out);
    }

    // /** Useful for debugging */
    // private void dumpMatrix(RealMatrix m) {
    // for (int i = 0; i < m.getRowDimension(); i++) {
    // String os = "";
    // for (int j = 0; j < m.getColumnDimension(); j++) {
    // os += m.getEntry(i, j) + " ";
    // }
    // System.out.println(os);
    // }
    // }
}
