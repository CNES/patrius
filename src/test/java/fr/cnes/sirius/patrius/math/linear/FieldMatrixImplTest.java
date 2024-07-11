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

import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.exception.MathIllegalStateException;
import fr.cnes.sirius.patrius.math.exception.NoDataException;
import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;
import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;
import fr.cnes.sirius.patrius.math.fraction.Fraction;
import fr.cnes.sirius.patrius.math.fraction.FractionField;

/**
 * Test cases for the {@link Array2DRowFieldMatrix} class.
 * 
 * @version $Id: FieldMatrixImplTest.java 18108 2017-10-04 06:45:27Z bignon $
 */

public final class FieldMatrixImplTest {

    // 3 x 3 identity matrix
    protected Fraction[][] id = { { new Fraction(1), new Fraction(0), new Fraction(0) },
        { new Fraction(0), new Fraction(1), new Fraction(0) },
        { new Fraction(0), new Fraction(0), new Fraction(1) } };

    // Test data for group operations
    protected Fraction[][] testData = { { new Fraction(1), new Fraction(2), new Fraction(3) },
        { new Fraction(2), new Fraction(5), new Fraction(3) },
        { new Fraction(1), new Fraction(0), new Fraction(8) } };
    protected Fraction[][] testDataLU = { { new Fraction(2), new Fraction(5), new Fraction(3) },
        { new Fraction(1, 2), new Fraction(-5, 2), new Fraction(13, 2) },
        { new Fraction(1, 2), new Fraction(1, 5), new Fraction(1, 5) } };
    protected Fraction[][] testDataPlus2 = { { new Fraction(3), new Fraction(4), new Fraction(5) },
        { new Fraction(4), new Fraction(7), new Fraction(5) },
        { new Fraction(3), new Fraction(2), new Fraction(10) } };
    protected Fraction[][] testDataMinus = { { new Fraction(-1), new Fraction(-2), new Fraction(-3) },
        { new Fraction(-2), new Fraction(-5), new Fraction(-3) },
        { new Fraction(-1), new Fraction(0), new Fraction(-8) } };
    protected Fraction[] testDataRow1 = { new Fraction(1), new Fraction(2), new Fraction(3) };
    protected Fraction[] testDataCol3 = { new Fraction(3), new Fraction(3), new Fraction(8) };
    protected Fraction[][] testDataInv =
    { { new Fraction(-40), new Fraction(16), new Fraction(9) },
        { new Fraction(13), new Fraction(-5), new Fraction(-3) },
        { new Fraction(5), new Fraction(-2), new Fraction(-1) } };
    protected Fraction[] preMultTest = { new Fraction(8), new Fraction(12), new Fraction(33) };
    protected Fraction[][] testData2 = { { new Fraction(1), new Fraction(2), new Fraction(3) },
        { new Fraction(2), new Fraction(5), new Fraction(3) } };
    protected Fraction[][] testData2T = { { new Fraction(1), new Fraction(2) }, { new Fraction(2), new Fraction(5) },
        { new Fraction(3), new Fraction(3) } };
    protected Fraction[][] testDataPlusInv =
    { { new Fraction(-39), new Fraction(18), new Fraction(12) },
        { new Fraction(15), new Fraction(0), new Fraction(0) },
        { new Fraction(6), new Fraction(-2), new Fraction(7) } };

    // lu decomposition tests
    protected Fraction[][] luData = { { new Fraction(2), new Fraction(3), new Fraction(3) },
        { new Fraction(0), new Fraction(5), new Fraction(7) },
        { new Fraction(6), new Fraction(9), new Fraction(8) } };
    protected Fraction[][] luDataLUDecomposition = { { new Fraction(6), new Fraction(9), new Fraction(8) },
        { new Fraction(0), new Fraction(5), new Fraction(7) },
        { new Fraction(1, 3), new Fraction(0), new Fraction(1, 3) } };

    // singular matrices
    protected Fraction[][] singular = { { new Fraction(2), new Fraction(3) }, { new Fraction(2), new Fraction(3) } };
    protected Fraction[][] bigSingular = { { new Fraction(1), new Fraction(2), new Fraction(3), new Fraction(4) },
        { new Fraction(2), new Fraction(5), new Fraction(3), new Fraction(4) },
        { new Fraction(7), new Fraction(3), new Fraction(256), new Fraction(1930) },
        { new Fraction(3), new Fraction(7), new Fraction(6), new Fraction(8) } }; // 4th row = 1st + 2nd
    protected Fraction[][] detData = { { new Fraction(1), new Fraction(2), new Fraction(3) },
        { new Fraction(4), new Fraction(5), new Fraction(6) },
        { new Fraction(7), new Fraction(8), new Fraction(10) } };
    protected Fraction[][] detData2 = { { new Fraction(1), new Fraction(3) }, { new Fraction(2), new Fraction(4) } };

    // vectors
    protected Fraction[] testVector = { new Fraction(1), new Fraction(2), new Fraction(3) };
    protected Fraction[] testVector2 = { new Fraction(1), new Fraction(2), new Fraction(3), new Fraction(4) };

    // submatrix accessor tests
    protected Fraction[][] subTestData = { { new Fraction(1), new Fraction(2), new Fraction(3), new Fraction(4) },
        { new Fraction(3, 2), new Fraction(5, 2), new Fraction(7, 2), new Fraction(9, 2) },
        { new Fraction(2), new Fraction(4), new Fraction(6), new Fraction(8) },
        { new Fraction(4), new Fraction(5), new Fraction(6), new Fraction(7) } };
    // array selections
    protected Fraction[][] subRows02Cols13 = { { new Fraction(2), new Fraction(4) },
        { new Fraction(4), new Fraction(8) } };
    protected Fraction[][] subRows03Cols12 = { { new Fraction(2), new Fraction(3) },
        { new Fraction(5), new Fraction(6) } };
    protected Fraction[][] subRows03Cols123 = { { new Fraction(2), new Fraction(3), new Fraction(4) },
        { new Fraction(5), new Fraction(6), new Fraction(7) } };
    // effective permutations
    protected Fraction[][] subRows20Cols123 = { { new Fraction(4), new Fraction(6), new Fraction(8) },
        { new Fraction(2), new Fraction(3), new Fraction(4) } };
    protected Fraction[][] subRows31Cols31 = { { new Fraction(7), new Fraction(5) },
        { new Fraction(9, 2), new Fraction(5, 2) } };
    // contiguous ranges
    protected Fraction[][] subRows01Cols23 = { { new Fraction(3), new Fraction(4) },
        { new Fraction(7, 2), new Fraction(9, 2) } };
    protected Fraction[][] subRows23Cols00 = { { new Fraction(2) }, { new Fraction(4) } };
    protected Fraction[][] subRows00Cols33 = { { new Fraction(4) } };
    // row matrices
    protected Fraction[][] subRow0 = { { new Fraction(1), new Fraction(2), new Fraction(3), new Fraction(4) } };
    protected Fraction[][] subRow3 = { { new Fraction(4), new Fraction(5), new Fraction(6), new Fraction(7) } };
    // column matrices
    protected Fraction[][] subColumn1 = { { new Fraction(2) }, { new Fraction(5, 2) }, { new Fraction(4) },
        { new Fraction(5) } };
    protected Fraction[][] subColumn3 = { { new Fraction(4) }, { new Fraction(9, 2) }, { new Fraction(8) },
        { new Fraction(7) } };

    // tolerances
    protected double entryTolerance = 10E-16;
    protected double normTolerance = 10E-14;

    /** test dimensions */
    @Test
    public void testDimensions() {
        final Array2DRowFieldMatrix<Fraction> m = new Array2DRowFieldMatrix<Fraction>(this.testData);
        final Array2DRowFieldMatrix<Fraction> m2 = new Array2DRowFieldMatrix<Fraction>(this.testData2);
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
        final Array2DRowFieldMatrix<Fraction> m1 = new Array2DRowFieldMatrix<Fraction>(this.testData);
        final Array2DRowFieldMatrix<Fraction> m2 = new Array2DRowFieldMatrix<Fraction>(m1.getData());
        Assert.assertEquals(m2, m1);
        final Array2DRowFieldMatrix<Fraction> m3 = new Array2DRowFieldMatrix<Fraction>(this.testData);
        final Array2DRowFieldMatrix<Fraction> m4 = new Array2DRowFieldMatrix<Fraction>(m3.getData(), false);
        Assert.assertEquals(m4, m3);
    }

    /** test add */
    @Test
    public void testAdd() {
        final Array2DRowFieldMatrix<Fraction> m = new Array2DRowFieldMatrix<Fraction>(this.testData);
        final Array2DRowFieldMatrix<Fraction> mInv = new Array2DRowFieldMatrix<Fraction>(this.testDataInv);
        final FieldMatrix<Fraction> mPlusMInv = m.add(mInv);
        final Fraction[][] sumEntries = mPlusMInv.getData();
        for (int row = 0; row < m.getRowDimension(); row++) {
            for (int col = 0; col < m.getColumnDimension(); col++) {
                Assert.assertEquals(this.testDataPlusInv[row][col], sumEntries[row][col]);
            }
        }
    }

    /** test add failure */
    @Test
    public void testAddFail() {
        final Array2DRowFieldMatrix<Fraction> m = new Array2DRowFieldMatrix<Fraction>(this.testData);
        final Array2DRowFieldMatrix<Fraction> m2 = new Array2DRowFieldMatrix<Fraction>(this.testData2);
        try {
            m.add(m2);
            Assert.fail("MathIllegalArgumentException expected");
        } catch (final MathIllegalArgumentException ex) {
            // ignored
        }
    }

    /** test m-n = m + -n */
    @Test
    public void testPlusMinus() {
        final Array2DRowFieldMatrix<Fraction> m = new Array2DRowFieldMatrix<Fraction>(this.testData);
        final Array2DRowFieldMatrix<Fraction> m2 = new Array2DRowFieldMatrix<Fraction>(this.testDataInv);
        TestUtils.assertEquals(m.subtract(m2), m2.scalarMultiply(new Fraction(-1)).add(m));
        try {
            m.subtract(new Array2DRowFieldMatrix<Fraction>(this.testData2));
            Assert.fail("Expecting illegalArgumentException");
        } catch (final MathIllegalArgumentException ex) {
            // ignored
        }
    }

    /** test multiply */
    @Test
    public void testMultiply() {
        final Array2DRowFieldMatrix<Fraction> m = new Array2DRowFieldMatrix<Fraction>(this.testData);
        final Array2DRowFieldMatrix<Fraction> mInv = new Array2DRowFieldMatrix<Fraction>(this.testDataInv);
        final Array2DRowFieldMatrix<Fraction> identity = new Array2DRowFieldMatrix<Fraction>(this.id);
        final Array2DRowFieldMatrix<Fraction> m2 = new Array2DRowFieldMatrix<Fraction>(this.testData2);
        TestUtils.assertEquals(m.multiply(mInv), identity);
        TestUtils.assertEquals(mInv.multiply(m), identity);
        TestUtils.assertEquals(m.multiply(identity), m);
        TestUtils.assertEquals(identity.multiply(mInv), mInv);
        TestUtils.assertEquals(m2.multiply(identity), m2);
        try {
            m.multiply(new Array2DRowFieldMatrix<Fraction>(this.bigSingular));
            Assert.fail("Expecting illegalArgumentException");
        } catch (final MathIllegalArgumentException ex) {
            // ignored
        }
    }

    // Additional Test for Array2DRowFieldMatrix<Fraction>Test.testMultiply

    private final Fraction[][] d3 = new Fraction[][] {
        { new Fraction(1), new Fraction(2), new Fraction(3), new Fraction(4) },
        { new Fraction(5), new Fraction(6), new Fraction(7), new Fraction(8) } };
    private final Fraction[][] d4 = new Fraction[][] { { new Fraction(1) }, { new Fraction(2) }, { new Fraction(3) },
        { new Fraction(4) } };
    private final Fraction[][] d5 = new Fraction[][] { { new Fraction(30) }, { new Fraction(70) } };

    @Test
    public void testMultiply2() {
        final FieldMatrix<Fraction> m3 = new Array2DRowFieldMatrix<Fraction>(this.d3);
        final FieldMatrix<Fraction> m4 = new Array2DRowFieldMatrix<Fraction>(this.d4);
        final FieldMatrix<Fraction> m5 = new Array2DRowFieldMatrix<Fraction>(this.d5);
        TestUtils.assertEquals(m3.multiply(m4), m5);
    }

    @Test
    public void testPower() {
        final FieldMatrix<Fraction> m = new Array2DRowFieldMatrix<Fraction>(this.testData);
        final FieldMatrix<Fraction> mInv = new Array2DRowFieldMatrix<Fraction>(this.testDataInv);
        final FieldMatrix<Fraction> mPlusInv = new Array2DRowFieldMatrix<Fraction>(this.testDataPlusInv);
        final FieldMatrix<Fraction> identity = new Array2DRowFieldMatrix<Fraction>(this.id);

        TestUtils.assertEquals(m.power(0), identity);
        TestUtils.assertEquals(mInv.power(0), identity);
        TestUtils.assertEquals(mPlusInv.power(0), identity);

        TestUtils.assertEquals(m.power(1), m);
        TestUtils.assertEquals(mInv.power(1), mInv);
        TestUtils.assertEquals(mPlusInv.power(1), mPlusInv);

        FieldMatrix<Fraction> C1 = m.copy();
        FieldMatrix<Fraction> C2 = mInv.copy();
        FieldMatrix<Fraction> C3 = mPlusInv.copy();

        // stop at 5 to avoid overflow
        for (int i = 2; i <= 5; ++i) {
            C1 = C1.multiply(m);
            C2 = C2.multiply(mInv);
            C3 = C3.multiply(mPlusInv);

            TestUtils.assertEquals(m.power(i), C1);
            TestUtils.assertEquals(mInv.power(i), C2);
            TestUtils.assertEquals(mPlusInv.power(i), C3);
        }

        try {
            final FieldMatrix<Fraction> mNotSquare = new Array2DRowFieldMatrix<Fraction>(this.testData2T);
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
        FieldMatrix<Fraction> m = new Array2DRowFieldMatrix<Fraction>(this.id);
        Assert.assertEquals("identity trace", new Fraction(3), m.getTrace());
        m = new Array2DRowFieldMatrix<Fraction>(this.testData2);
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
        final FieldMatrix<Fraction> m = new Array2DRowFieldMatrix<Fraction>(this.testData);
        TestUtils.assertEquals(new Array2DRowFieldMatrix<Fraction>(this.testDataPlus2), m.scalarAdd(new Fraction(2)));
    }

    /** test operate */
    @Test
    public void testOperate() {
        FieldMatrix<Fraction> m = new Array2DRowFieldMatrix<Fraction>(this.id);
        TestUtils.assertEquals(this.testVector, m.operate(this.testVector));
        TestUtils.assertEquals(this.testVector, m.operate(new ArrayFieldVector<Fraction>(this.testVector)).toArray());
        m = new Array2DRowFieldMatrix<Fraction>(this.bigSingular);
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
        final FieldMatrix<Fraction> a = new Array2DRowFieldMatrix<Fraction>(new Fraction[][] {
            { new Fraction(1), new Fraction(2) }, { new Fraction(3), new Fraction(4) },
            { new Fraction(5), new Fraction(6) }
        }, false);
        final Fraction[] b = a.operate(new Fraction[] { new Fraction(1), new Fraction(1) });
        Assert.assertEquals(a.getRowDimension(), b.length);
        Assert.assertEquals(new Fraction(3), b[0]);
        Assert.assertEquals(new Fraction(7), b[1]);
        Assert.assertEquals(new Fraction(11), b[2]);
    }

    /** test transpose */
    @Test
    public void testTranspose() {
        FieldMatrix<Fraction> m = new Array2DRowFieldMatrix<Fraction>(this.testData);
        final FieldMatrix<Fraction> mIT = new FieldLUDecomposition<Fraction>(m).getSolver().getInverse().transpose();
        final FieldMatrix<Fraction> mTI = new FieldLUDecomposition<Fraction>(m.transpose()).getSolver().getInverse();
        TestUtils.assertEquals(mIT, mTI);
        m = new Array2DRowFieldMatrix<Fraction>(this.testData2);
        final FieldMatrix<Fraction> mt = new Array2DRowFieldMatrix<Fraction>(this.testData2T);
        TestUtils.assertEquals(mt, m.transpose());
    }

    /** test preMultiply by vector */
    @Test
    public void testPremultiplyVector() {
        FieldMatrix<Fraction> m = new Array2DRowFieldMatrix<Fraction>(this.testData);
        TestUtils.assertEquals(m.preMultiply(this.testVector), this.preMultTest);
        TestUtils.assertEquals(m.preMultiply(new ArrayFieldVector<Fraction>(this.testVector).getData()),
            this.preMultTest);
        m = new Array2DRowFieldMatrix<Fraction>(this.bigSingular);
        try {
            m.preMultiply(this.testVector);
            Assert.fail("expecting MathIllegalArgumentException");
        } catch (final MathIllegalArgumentException ex) {
            // ignored
        }
    }

    @Test
    public void testPremultiply() {
        final FieldMatrix<Fraction> m3 = new Array2DRowFieldMatrix<Fraction>(this.d3);
        final FieldMatrix<Fraction> m4 = new Array2DRowFieldMatrix<Fraction>(this.d4);
        final FieldMatrix<Fraction> m5 = new Array2DRowFieldMatrix<Fraction>(this.d5);
        TestUtils.assertEquals(m4.preMultiply(m3), m5);

        final Array2DRowFieldMatrix<Fraction> m = new Array2DRowFieldMatrix<Fraction>(this.testData);
        final Array2DRowFieldMatrix<Fraction> mInv = new Array2DRowFieldMatrix<Fraction>(this.testDataInv);
        final Array2DRowFieldMatrix<Fraction> identity = new Array2DRowFieldMatrix<Fraction>(this.id);
        TestUtils.assertEquals(m.preMultiply(mInv), identity);
        TestUtils.assertEquals(mInv.preMultiply(m), identity);
        TestUtils.assertEquals(m.preMultiply(identity), m);
        TestUtils.assertEquals(identity.preMultiply(mInv), mInv);
        try {
            m.preMultiply(new Array2DRowFieldMatrix<Fraction>(this.bigSingular));
            Assert.fail("Expecting illegalArgumentException");
        } catch (final MathIllegalArgumentException ex) {
            // ignored
        }
    }

    @Test
    public void testGetVectors() {
        final FieldMatrix<Fraction> m = new Array2DRowFieldMatrix<Fraction>(this.testData);
        TestUtils.assertEquals(m.getRow(0), this.testDataRow1);
        TestUtils.assertEquals(m.getColumn(2), this.testDataCol3);
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
        final FieldMatrix<Fraction> m = new Array2DRowFieldMatrix<Fraction>(this.testData);
        Assert.assertEquals("get entry", m.getEntry(0, 1), new Fraction(2));
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
        final Fraction[][] matrixData = {
            { new Fraction(1), new Fraction(2), new Fraction(3) },
            { new Fraction(2), new Fraction(5), new Fraction(3) }
        };
        final FieldMatrix<Fraction> m = new Array2DRowFieldMatrix<Fraction>(matrixData);
        // One more with three rows, two columns
        final Fraction[][] matrixData2 = {
            { new Fraction(1), new Fraction(2) },
            { new Fraction(2), new Fraction(5) },
            { new Fraction(1), new Fraction(7) }
        };
        final FieldMatrix<Fraction> n = new Array2DRowFieldMatrix<Fraction>(matrixData2);
        // Now multiply m by n
        final FieldMatrix<Fraction> p = m.multiply(n);
        Assert.assertEquals(2, p.getRowDimension());
        Assert.assertEquals(2, p.getColumnDimension());
        // Invert p
        final FieldMatrix<Fraction> pInverse = new FieldLUDecomposition<Fraction>(p).getSolver().getInverse();
        Assert.assertEquals(2, pInverse.getRowDimension());
        Assert.assertEquals(2, pInverse.getColumnDimension());

        // Solve example
        final Fraction[][] coefficientsData = {
            { new Fraction(2), new Fraction(3), new Fraction(-2) },
            { new Fraction(-1), new Fraction(7), new Fraction(6) },
            { new Fraction(4), new Fraction(-3), new Fraction(-5) }
        };
        final FieldMatrix<Fraction> coefficients = new Array2DRowFieldMatrix<Fraction>(coefficientsData);
        final Fraction[] constants = {
            new Fraction(1), new Fraction(-2), new Fraction(1)
        };
        Fraction[] solution;
        solution = new FieldLUDecomposition<Fraction>(coefficients)
            .getSolver()
            .solve(new ArrayFieldVector<Fraction>(constants, false)).toArray();
        Assert.assertEquals(new Fraction(2).multiply(solution[0]).
            add(new Fraction(3).multiply(solution[1])).
            subtract(new Fraction(2).multiply(solution[2])), constants[0]);
        Assert.assertEquals(new Fraction(-1).multiply(solution[0]).
            add(new Fraction(7).multiply(solution[1])).
            add(new Fraction(6).multiply(solution[2])), constants[1]);
        Assert.assertEquals(new Fraction(4).multiply(solution[0]).
            subtract(new Fraction(3).multiply(solution[1])).
            subtract(new Fraction(5).multiply(solution[2])), constants[2]);

    }

    // test submatrix accessors
    @Test
    public void testGetSubMatrix() {
        final FieldMatrix<Fraction> m = new Array2DRowFieldMatrix<Fraction>(this.subTestData);
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

    private void checkGetSubMatrix(final FieldMatrix<Fraction> m, final Fraction[][] reference,
                                   final int startRow, final int endRow, final int startColumn, final int endColumn) {
        try {
            final FieldMatrix<Fraction> sub = m.getSubMatrix(startRow, endRow, startColumn, endColumn);
            if (reference != null) {
                Assert.assertEquals(new Array2DRowFieldMatrix<Fraction>(reference), sub);
            } else {
                Assert.fail("Expecting OutOfRangeException or NotStrictlyPositiveException"
                    + " or NumberIsTooSmallException or NoDataException");
            }
        } catch (final OutOfRangeException e) {
            if (reference != null) {
                throw e;
            }
        } catch (final NotStrictlyPositiveException e) {
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

    private void checkGetSubMatrix(final FieldMatrix<Fraction> m, final Fraction[][] reference,
                                   final int[] selectedRows, final int[] selectedColumns) {
        try {
            final FieldMatrix<Fraction> sub = m.getSubMatrix(selectedRows, selectedColumns);
            if (reference != null) {
                Assert.assertEquals(new Array2DRowFieldMatrix<Fraction>(reference), sub);
            } else {
                Assert.fail("Expecting OutOfRangeException or NotStrictlyPositiveException"
                    + " or NumberIsTooSmallException or NoDataException");
            }
        } catch (final OutOfRangeException e) {
            if (reference != null) {
                throw e;
            }
        } catch (final NotStrictlyPositiveException e) {
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
    public void testCopySubMatrix() {
        final FieldMatrix<Fraction> m = new Array2DRowFieldMatrix<Fraction>(this.subTestData);
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

    private void checkCopy(final FieldMatrix<Fraction> m, final Fraction[][] reference,
                           final int startRow, final int endRow, final int startColumn, final int endColumn) {
        try {
            final Fraction[][] sub = (reference == null) ?
                new Fraction[1][1] :
                new Fraction[reference.length][reference[0].length];
            m.copySubMatrix(startRow, endRow, startColumn, endColumn, sub);
            if (reference != null) {
                Assert.assertEquals(new Array2DRowFieldMatrix<Fraction>(reference),
                    new Array2DRowFieldMatrix<Fraction>(sub));
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

    private void checkCopy(final FieldMatrix<Fraction> m, final Fraction[][] reference,
                           final int[] selectedRows, final int[] selectedColumns) {
        try {
            final Fraction[][] sub = (reference == null) ?
                new Fraction[1][1] :
                new Fraction[reference.length][reference[0].length];
            m.copySubMatrix(selectedRows, selectedColumns, sub);
            if (reference != null) {
                Assert.assertEquals(new Array2DRowFieldMatrix<Fraction>(reference),
                    new Array2DRowFieldMatrix<Fraction>(sub));
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
        final FieldMatrix<Fraction> m = new Array2DRowFieldMatrix<Fraction>(this.subTestData);
        final FieldMatrix<Fraction> mRow0 = new Array2DRowFieldMatrix<Fraction>(this.subRow0);
        final FieldMatrix<Fraction> mRow3 = new Array2DRowFieldMatrix<Fraction>(this.subRow3);
        Assert.assertEquals("Row0", mRow0,
            m.getRowMatrix(0));
        Assert.assertEquals("Row3", mRow3,
            m.getRowMatrix(3));
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
        final FieldMatrix<Fraction> m = new Array2DRowFieldMatrix<Fraction>(this.subTestData);
        final FieldMatrix<Fraction> mRow3 = new Array2DRowFieldMatrix<Fraction>(this.subRow3);
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
        final FieldMatrix<Fraction> m = new Array2DRowFieldMatrix<Fraction>(this.subTestData);
        final FieldMatrix<Fraction> mColumn1 = new Array2DRowFieldMatrix<Fraction>(this.subColumn1);
        final FieldMatrix<Fraction> mColumn3 = new Array2DRowFieldMatrix<Fraction>(this.subColumn3);
        Assert.assertEquals("Column1", mColumn1,
            m.getColumnMatrix(1));
        Assert.assertEquals("Column3", mColumn3,
            m.getColumnMatrix(3));
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
        final FieldMatrix<Fraction> m = new Array2DRowFieldMatrix<Fraction>(this.subTestData);
        final FieldMatrix<Fraction> mColumn3 = new Array2DRowFieldMatrix<Fraction>(this.subColumn3);
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
        final FieldMatrix<Fraction> m = new Array2DRowFieldMatrix<Fraction>(this.subTestData);
        final FieldVector<Fraction> mRow0 = new ArrayFieldVector<Fraction>(this.subRow0[0]);
        final FieldVector<Fraction> mRow3 = new ArrayFieldVector<Fraction>(this.subRow3[0]);
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
        final FieldMatrix<Fraction> m = new Array2DRowFieldMatrix<Fraction>(this.subTestData);
        final FieldVector<Fraction> mRow3 = new ArrayFieldVector<Fraction>(this.subRow3[0]);
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
            m.setRowVector(0, new ArrayFieldVector<Fraction>(FractionField.getInstance(), 5));
            Assert.fail("Expecting MatrixDimensionMismatchException");
        } catch (final MatrixDimensionMismatchException ex) {
            // expected
        }
    }

    @Test
    public void testGetColumnVector() {
        final FieldMatrix<Fraction> m = new Array2DRowFieldMatrix<Fraction>(this.subTestData);
        final FieldVector<Fraction> mColumn1 = this.columnToVector(this.subColumn1);
        final FieldVector<Fraction> mColumn3 = this.columnToVector(this.subColumn3);
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
        final FieldMatrix<Fraction> m = new Array2DRowFieldMatrix<Fraction>(this.subTestData);
        final FieldVector<Fraction> mColumn3 = this.columnToVector(this.subColumn3);
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
            m.setColumnVector(0, new ArrayFieldVector<Fraction>(FractionField.getInstance(), 5));
            Assert.fail("Expecting MatrixDimensionMismatchException");
        } catch (final MatrixDimensionMismatchException ex) {
            // expected
        }
    }

    private FieldVector<Fraction> columnToVector(final Fraction[][] column) {
        final Fraction[] data = new Fraction[column.length];
        for (int i = 0; i < data.length; ++i) {
            data[i] = column[i][0];
        }
        return new ArrayFieldVector<Fraction>(data, false);
    }

    @Test
    public void testGetRow() {
        final FieldMatrix<Fraction> m = new Array2DRowFieldMatrix<Fraction>(this.subTestData);
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
        final FieldMatrix<Fraction> m = new Array2DRowFieldMatrix<Fraction>(this.subTestData);
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
            m.setRow(0, new Fraction[5]);
            Assert.fail("Expecting MatrixDimensionMismatchException");
        } catch (final MatrixDimensionMismatchException ex) {
            // expected
        }
    }

    @Test
    public void testGetColumn() {
        final FieldMatrix<Fraction> m = new Array2DRowFieldMatrix<Fraction>(this.subTestData);
        final Fraction[] mColumn1 = this.columnToArray(this.subColumn1);
        final Fraction[] mColumn3 = this.columnToArray(this.subColumn3);
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
        final FieldMatrix<Fraction> m = new Array2DRowFieldMatrix<Fraction>(this.subTestData);
        final Fraction[] mColumn3 = this.columnToArray(this.subColumn3);
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
            m.setColumn(0, new Fraction[5]);
            Assert.fail("Expecting MatrixDimensionMismatchException");
        } catch (final MatrixDimensionMismatchException ex) {
            // expected
        }
    }

    private Fraction[] columnToArray(final Fraction[][] column) {
        final Fraction[] data = new Fraction[column.length];
        for (int i = 0; i < data.length; ++i) {
            data[i] = column[i][0];
        }
        return data;
    }

    private void checkArrays(final Fraction[] expected, final Fraction[] actual) {
        Assert.assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; ++i) {
            Assert.assertEquals(expected[i], actual[i]);
        }
    }

    @Test
    public void testEqualsAndHashCode() {
        final Array2DRowFieldMatrix<Fraction> m = new Array2DRowFieldMatrix<Fraction>(this.testData);
        final Array2DRowFieldMatrix<Fraction> m1 = (Array2DRowFieldMatrix<Fraction>) m.copy();
        final Array2DRowFieldMatrix<Fraction> mt = (Array2DRowFieldMatrix<Fraction>) m.transpose();
        Assert.assertTrue(m.hashCode() != mt.hashCode());
        Assert.assertEquals(m.hashCode(), m1.hashCode());
        Assert.assertEquals(m, m);
        Assert.assertEquals(m, m1);
        Assert.assertFalse(m.equals(null));
        Assert.assertFalse(m.equals(mt));
        Assert.assertFalse(m.equals(new Array2DRowFieldMatrix<Fraction>(this.bigSingular)));
    }

    @Test
    public void testToString() {
        Array2DRowFieldMatrix<Fraction> m = new Array2DRowFieldMatrix<Fraction>(this.testData);
        Assert.assertEquals("Array2DRowFieldMatrix{{1,2,3},{2,5,3},{1,0,8}}", m.toString());
        m = new Array2DRowFieldMatrix<Fraction>(FractionField.getInstance());
        Assert.assertEquals("Array2DRowFieldMatrix{}", m.toString());
    }

    @Test
    public void testSetSubMatrix() {
        final Array2DRowFieldMatrix<Fraction> m = new Array2DRowFieldMatrix<Fraction>(this.testData);
        m.setSubMatrix(this.detData2, 1, 1);
        FieldMatrix<Fraction> expected = new Array2DRowFieldMatrix<Fraction>
            (new Fraction[][] {
                { new Fraction(1), new Fraction(2), new Fraction(3) },
                { new Fraction(2), new Fraction(1), new Fraction(3) },
                { new Fraction(1), new Fraction(2), new Fraction(4) }
            });
        Assert.assertEquals(expected, m);

        m.setSubMatrix(this.detData2, 0, 0);
        expected = new Array2DRowFieldMatrix<Fraction>
            (new Fraction[][] {
                { new Fraction(1), new Fraction(3), new Fraction(3) },
                { new Fraction(2), new Fraction(4), new Fraction(3) },
                { new Fraction(1), new Fraction(2), new Fraction(4) }
            });
        Assert.assertEquals(expected, m);

        m.setSubMatrix(this.testDataPlus2, 0, 0);
        expected = new Array2DRowFieldMatrix<Fraction>
            (new Fraction[][] {
                { new Fraction(3), new Fraction(4), new Fraction(5) },
                { new Fraction(4), new Fraction(7), new Fraction(5) },
                { new Fraction(3), new Fraction(2), new Fraction(10) }
            });
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
        final Array2DRowFieldMatrix<Fraction> m2 = new Array2DRowFieldMatrix<Fraction>(FractionField.getInstance());
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
            m.setSubMatrix(new Fraction[][] { { new Fraction(1) }, { new Fraction(2), new Fraction(3) } }, 0, 0);
            Assert.fail("expecting MathIllegalArgumentException");
        } catch (final MathIllegalArgumentException e) {
            // expected
        }

        // empty
        try {
            m.setSubMatrix(new Fraction[][] { {} }, 0, 0);
            Assert.fail("expecting MathIllegalArgumentException");
        } catch (final MathIllegalArgumentException e) {
            // expected
        }

    }

    @Test
    public void testWalk() {
        final int rows = 150;
        final int columns = 75;

        FieldMatrix<Fraction> m =
            new Array2DRowFieldMatrix<Fraction>(FractionField.getInstance(), rows, columns);
        m.walkInRowOrder(new SetVisitor());
        GetVisitor getVisitor = new GetVisitor();
        m.walkInOptimizedOrder(getVisitor);
        Assert.assertEquals(rows * columns, getVisitor.getCount());

        m = new Array2DRowFieldMatrix<Fraction>(FractionField.getInstance(), rows, columns);
        m.walkInRowOrder(new SetVisitor(), 1, rows - 2, 1, columns - 2);
        getVisitor = new GetVisitor();
        m.walkInOptimizedOrder(getVisitor, 1, rows - 2, 1, columns - 2);
        Assert.assertEquals((rows - 2) * (columns - 2), getVisitor.getCount());
        for (int i = 0; i < rows; ++i) {
            Assert.assertEquals(new Fraction(0), m.getEntry(i, 0));
            Assert.assertEquals(new Fraction(0), m.getEntry(i, columns - 1));
        }
        for (int j = 0; j < columns; ++j) {
            Assert.assertEquals(new Fraction(0), m.getEntry(0, j));
            Assert.assertEquals(new Fraction(0), m.getEntry(rows - 1, j));
        }

        m = new Array2DRowFieldMatrix<Fraction>(FractionField.getInstance(), rows, columns);
        m.walkInColumnOrder(new SetVisitor());
        getVisitor = new GetVisitor();
        m.walkInOptimizedOrder(getVisitor);
        Assert.assertEquals(rows * columns, getVisitor.getCount());

        m = new Array2DRowFieldMatrix<Fraction>(FractionField.getInstance(), rows, columns);
        m.walkInColumnOrder(new SetVisitor(), 1, rows - 2, 1, columns - 2);
        getVisitor = new GetVisitor();
        m.walkInOptimizedOrder(getVisitor, 1, rows - 2, 1, columns - 2);
        Assert.assertEquals((rows - 2) * (columns - 2), getVisitor.getCount());
        for (int i = 0; i < rows; ++i) {
            Assert.assertEquals(new Fraction(0), m.getEntry(i, 0));
            Assert.assertEquals(new Fraction(0), m.getEntry(i, columns - 1));
        }
        for (int j = 0; j < columns; ++j) {
            Assert.assertEquals(new Fraction(0), m.getEntry(0, j));
            Assert.assertEquals(new Fraction(0), m.getEntry(rows - 1, j));
        }

        m = new Array2DRowFieldMatrix<Fraction>(FractionField.getInstance(), rows, columns);
        m.walkInOptimizedOrder(new SetVisitor());
        getVisitor = new GetVisitor();
        m.walkInRowOrder(getVisitor);
        Assert.assertEquals(rows * columns, getVisitor.getCount());

        m = new Array2DRowFieldMatrix<Fraction>(FractionField.getInstance(), rows, columns);
        m.walkInOptimizedOrder(new SetVisitor(), 1, rows - 2, 1, columns - 2);
        getVisitor = new GetVisitor();
        m.walkInRowOrder(getVisitor, 1, rows - 2, 1, columns - 2);
        Assert.assertEquals((rows - 2) * (columns - 2), getVisitor.getCount());
        for (int i = 0; i < rows; ++i) {
            Assert.assertEquals(new Fraction(0), m.getEntry(i, 0));
            Assert.assertEquals(new Fraction(0), m.getEntry(i, columns - 1));
        }
        for (int j = 0; j < columns; ++j) {
            Assert.assertEquals(new Fraction(0), m.getEntry(0, j));
            Assert.assertEquals(new Fraction(0), m.getEntry(rows - 1, j));
        }

        m = new Array2DRowFieldMatrix<Fraction>(FractionField.getInstance(), rows, columns);
        m.walkInOptimizedOrder(new SetVisitor());
        getVisitor = new GetVisitor();
        m.walkInColumnOrder(getVisitor);
        Assert.assertEquals(rows * columns, getVisitor.getCount());

        m = new Array2DRowFieldMatrix<Fraction>(FractionField.getInstance(), rows, columns);
        m.walkInOptimizedOrder(new SetVisitor(), 1, rows - 2, 1, columns - 2);
        getVisitor = new GetVisitor();
        m.walkInColumnOrder(getVisitor, 1, rows - 2, 1, columns - 2);
        Assert.assertEquals((rows - 2) * (columns - 2), getVisitor.getCount());
        for (int i = 0; i < rows; ++i) {
            Assert.assertEquals(new Fraction(0), m.getEntry(i, 0));
            Assert.assertEquals(new Fraction(0), m.getEntry(i, columns - 1));
        }
        for (int j = 0; j < columns; ++j) {
            Assert.assertEquals(new Fraction(0), m.getEntry(0, j));
            Assert.assertEquals(new Fraction(0), m.getEntry(rows - 1, j));
        }
    }

    @Test
    public void testSerial() {
        final Array2DRowFieldMatrix<Fraction> m = new Array2DRowFieldMatrix<Fraction>(this.testData);
        Assert.assertEquals(m, TestUtils.serializeAndRecover(m));
    }

    private static class SetVisitor extends DefaultFieldMatrixChangingVisitor<Fraction> {
        public SetVisitor() {
            super(Fraction.ZERO);
        }

        @Override
        public Fraction visit(final int i, final int j, final Fraction value) {
            return new Fraction(i * 1024 + j, 1024);
        }
    }

    private static class GetVisitor extends DefaultFieldMatrixPreservingVisitor<Fraction> {
        private int count;

        public GetVisitor() {
            super(Fraction.ZERO);
            this.count = 0;
        }

        @Override
        public void visit(final int i, final int j, final Fraction value) {
            ++this.count;
            Assert.assertEquals(new Fraction(i * 1024 + j, 1024), value);
        }

        public int getCount() {
            return this.count;
        }
    }

    // --------------- -----------------Protected methods

    /** extracts the l and u matrices from compact lu representation */
    protected void splitLU(final FieldMatrix<Fraction> lu,
                           final Fraction[][] lowerData,
                           final Fraction[][] upperData) {
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
                    upperData[i][j] = Fraction.ZERO;
                } else if (i == j) {
                    lowerData[i][j] = Fraction.ONE;
                    upperData[i][j] = lu.getEntry(i, j);
                } else {
                    lowerData[i][j] = Fraction.ZERO;
                    upperData[i][j] = lu.getEntry(i, j);
                }
            }
        }
    }

    /** Returns the result of applying the given row permutation to the matrix */
    protected FieldMatrix<Fraction> permuteRows(final FieldMatrix<Fraction> matrix, final int[] permutation) {
        if (!matrix.isSquare()) {
            throw new NonSquareMatrixException(matrix.getRowDimension(),
                matrix.getColumnDimension());
        }
        if (matrix.getRowDimension() != permutation.length) {
            throw new DimensionMismatchException(matrix.getRowDimension(), permutation.length);
        }
        final int n = matrix.getRowDimension();
        final int m = matrix.getColumnDimension();
        final Fraction out[][] = new Fraction[m][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                out[i][j] = matrix.getEntry(permutation[i], j);
            }
        }
        return new Array2DRowFieldMatrix<Fraction>(out);
    }
}
