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
package fr.cnes.sirius.patrius.math.linear;

import java.util.Arrays;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.exception.NoDataException;
import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;
import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;
import fr.cnes.sirius.patrius.math.fraction.Fraction;
import fr.cnes.sirius.patrius.math.fraction.FractionField;

/**
 * Test cases for the {@link BlockFieldMatrix} class.
 * 
 * @version $Id: BlockFieldMatrixTest.java 18108 2017-10-04 06:45:27Z bignon $
 */

public final class BlockFieldMatrixTest {

    // 3 x 3 identity matrix
    protected Fraction[][] id = {
        { new Fraction(1), new Fraction(0), new Fraction(0) },
        { new Fraction(0), new Fraction(1), new Fraction(0) },
        { new Fraction(0), new Fraction(0), new Fraction(1) }
    };

    // Test data for group operations
    protected Fraction[][] testData = {
        { new Fraction(1), new Fraction(2), new Fraction(3) },
        { new Fraction(2), new Fraction(5), new Fraction(3) },
        { new Fraction(1), new Fraction(0), new Fraction(8) }
    };
    protected Fraction[][] testDataLU = {
        { new Fraction(2), new Fraction(5), new Fraction(3) },
        { new Fraction(1, 2), new Fraction(-5, 2), new Fraction(13, 2) },
        { new Fraction(1, 2), new Fraction(1, 5), new Fraction(1, 5) }
    };
    protected Fraction[][] testDataPlus2 = {
        { new Fraction(3), new Fraction(4), new Fraction(5) },
        { new Fraction(4), new Fraction(7), new Fraction(5) },
        { new Fraction(3), new Fraction(2), new Fraction(10) }
    };
    protected Fraction[][] testDataMinus = {
        { new Fraction(-1), new Fraction(-2), new Fraction(-3) },
        { new Fraction(-2), new Fraction(-5), new Fraction(-3) },
        { new Fraction(-1), new Fraction(0), new Fraction(-8) }
    };
    protected Fraction[] testDataRow1 = { new Fraction(1), new Fraction(2), new Fraction(3) };
    protected Fraction[] testDataCol3 = { new Fraction(3), new Fraction(3), new Fraction(8) };
    protected Fraction[][] testDataInv = {
        { new Fraction(-40), new Fraction(16), new Fraction(9) },
        { new Fraction(13), new Fraction(-5), new Fraction(-3) },
        { new Fraction(5), new Fraction(-2), new Fraction(-1) }
    };
    protected Fraction[] preMultTest = { new Fraction(8), new Fraction(12), new Fraction(33) };
    protected Fraction[][] testData2 = {
        { new Fraction(1), new Fraction(2), new Fraction(3) },
        { new Fraction(2), new Fraction(5), new Fraction(3) }
    };
    protected Fraction[][] testData2T = {
        { new Fraction(1), new Fraction(2) },
        { new Fraction(2), new Fraction(5) },
        { new Fraction(3), new Fraction(3) }
    };
    protected Fraction[][] testDataPlusInv = {
        { new Fraction(-39), new Fraction(18), new Fraction(12) },
        { new Fraction(15), new Fraction(0), new Fraction(0) },
        { new Fraction(6), new Fraction(-2), new Fraction(7) }
    };

    // lu decomposition tests
    protected Fraction[][] luData = {
        { new Fraction(2), new Fraction(3), new Fraction(3) },
        { new Fraction(0), new Fraction(5), new Fraction(7) },
        { new Fraction(6), new Fraction(9), new Fraction(8) }
    };
    protected Fraction[][] luDataLUDecomposition = {
        { new Fraction(6), new Fraction(9), new Fraction(8) },
        { new Fraction(0), new Fraction(5), new Fraction(7) },
        { new Fraction(1, 3), new Fraction(0), new Fraction(1, 3) }
    };

    // singular matrices
    protected Fraction[][] singular = { { new Fraction(2), new Fraction(3) }, { new Fraction(2), new Fraction(3) } };
    protected Fraction[][] bigSingular = {
        { new Fraction(1), new Fraction(2), new Fraction(3), new Fraction(4) },
        { new Fraction(2), new Fraction(5), new Fraction(3), new Fraction(4) },
        { new Fraction(7), new Fraction(3), new Fraction(256), new Fraction(1930) },
        { new Fraction(3), new Fraction(7), new Fraction(6), new Fraction(8) }
    }; // 4th row = 1st + 2nd
    protected Fraction[][] detData = {
        { new Fraction(1), new Fraction(2), new Fraction(3) },
        { new Fraction(4), new Fraction(5), new Fraction(6) },
        { new Fraction(7), new Fraction(8), new Fraction(10) }
    };
    protected Fraction[][] detData2 = { { new Fraction(1), new Fraction(3) }, { new Fraction(2), new Fraction(4) } };

    // vectors
    protected Fraction[] testVector = { new Fraction(1), new Fraction(2), new Fraction(3) };
    protected Fraction[] testVector2 = { new Fraction(1), new Fraction(2), new Fraction(3), new Fraction(4) };

    // submatrix accessor tests
    protected Fraction[][] subTestData = {
        { new Fraction(1), new Fraction(2), new Fraction(3), new Fraction(4) },
        { new Fraction(3, 2), new Fraction(5, 2), new Fraction(7, 2), new Fraction(9, 2) },
        { new Fraction(2), new Fraction(4), new Fraction(6), new Fraction(8) },
        { new Fraction(4), new Fraction(5), new Fraction(6), new Fraction(7) }
    };
    // array selections
    protected Fraction[][] subRows02Cols13 = { { new Fraction(2), new Fraction(4) },
        { new Fraction(4), new Fraction(8) } };
    protected Fraction[][] subRows03Cols12 = { { new Fraction(2), new Fraction(3) },
        { new Fraction(5), new Fraction(6) } };
    protected Fraction[][] subRows03Cols123 = {
        { new Fraction(2), new Fraction(3), new Fraction(4) },
        { new Fraction(5), new Fraction(6), new Fraction(7) }
    };
    // effective permutations
    protected Fraction[][] subRows20Cols123 = {
        { new Fraction(4), new Fraction(6), new Fraction(8) },
        { new Fraction(2), new Fraction(3), new Fraction(4) }
    };
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
        final BlockFieldMatrix<Fraction> m = new BlockFieldMatrix<Fraction>(this.testData);
        final BlockFieldMatrix<Fraction> m2 = new BlockFieldMatrix<Fraction>(this.testData2);
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
        final BlockFieldMatrix<Fraction> m1 = this.createRandomMatrix(r, 47, 83);
        final BlockFieldMatrix<Fraction> m2 = new BlockFieldMatrix<Fraction>(m1.getData());
        Assert.assertEquals(m1, m2);
        final BlockFieldMatrix<Fraction> m3 = new BlockFieldMatrix<Fraction>(this.testData);
        final BlockFieldMatrix<Fraction> m4 = new BlockFieldMatrix<Fraction>(m3.getData());
        Assert.assertEquals(m3, m4);
    }

    /** test add */
    @Test
    public void testAdd() {
        final BlockFieldMatrix<Fraction> m = new BlockFieldMatrix<Fraction>(this.testData);
        final BlockFieldMatrix<Fraction> mInv = new BlockFieldMatrix<Fraction>(this.testDataInv);
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
        final BlockFieldMatrix<Fraction> m = new BlockFieldMatrix<Fraction>(this.testData);
        final BlockFieldMatrix<Fraction> m2 = new BlockFieldMatrix<Fraction>(this.testData2);
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
        final BlockFieldMatrix<Fraction> m = new BlockFieldMatrix<Fraction>(this.testData);
        final BlockFieldMatrix<Fraction> m2 = new BlockFieldMatrix<Fraction>(this.testDataInv);
        TestUtils.assertEquals(m.subtract(m2), m2.scalarMultiply(new Fraction(-1)).add(m));
        try {
            m.subtract(new BlockFieldMatrix<Fraction>(this.testData2));
            Assert.fail("Expecting illegalArgumentException");
        } catch (final MathIllegalArgumentException ex) {
            // ignored
        }
    }

    /** test multiply */
    @Test
    public void testMultiply() {
        final BlockFieldMatrix<Fraction> m = new BlockFieldMatrix<Fraction>(this.testData);
        final BlockFieldMatrix<Fraction> mInv = new BlockFieldMatrix<Fraction>(this.testDataInv);
        final BlockFieldMatrix<Fraction> identity = new BlockFieldMatrix<Fraction>(this.id);
        final BlockFieldMatrix<Fraction> m2 = new BlockFieldMatrix<Fraction>(this.testData2);
        TestUtils.assertEquals(m.multiply(mInv), identity);
        TestUtils.assertEquals(mInv.multiply(m), identity);
        TestUtils.assertEquals(m.multiply(identity), m);
        TestUtils.assertEquals(identity.multiply(mInv), mInv);
        TestUtils.assertEquals(m2.multiply(identity), m2);
        try {
            m.multiply(new BlockFieldMatrix<Fraction>(this.bigSingular));
            Assert.fail("Expecting illegalArgumentException");
        } catch (final MathIllegalArgumentException ex) {
            // expected
        }
    }

    @Test
    public void testSeveralBlocks() {
        final FieldMatrix<Fraction> m =
            new BlockFieldMatrix<Fraction>(FractionField.getInstance(), 37, 41);
        for (int i = 0; i < m.getRowDimension(); ++i) {
            for (int j = 0; j < m.getColumnDimension(); ++j) {
                m.setEntry(i, j, new Fraction(i * 11 + j, 11));
            }
        }

        final FieldMatrix<Fraction> mT = m.transpose();
        Assert.assertEquals(m.getRowDimension(), mT.getColumnDimension());
        Assert.assertEquals(m.getColumnDimension(), mT.getRowDimension());
        for (int i = 0; i < mT.getRowDimension(); ++i) {
            for (int j = 0; j < mT.getColumnDimension(); ++j) {
                Assert.assertEquals(m.getEntry(j, i), mT.getEntry(i, j));
            }
        }

        final FieldMatrix<Fraction> mPm = m.add(m);
        for (int i = 0; i < mPm.getRowDimension(); ++i) {
            for (int j = 0; j < mPm.getColumnDimension(); ++j) {
                Assert.assertEquals(m.getEntry(i, j).multiply(new Fraction(2)), mPm.getEntry(i, j));
            }
        }

        final FieldMatrix<Fraction> mPmMm = mPm.subtract(m);
        for (int i = 0; i < mPmMm.getRowDimension(); ++i) {
            for (int j = 0; j < mPmMm.getColumnDimension(); ++j) {
                Assert.assertEquals(m.getEntry(i, j), mPmMm.getEntry(i, j));
            }
        }

        final FieldMatrix<Fraction> mTm = mT.multiply(m);
        for (int i = 0; i < mTm.getRowDimension(); ++i) {
            for (int j = 0; j < mTm.getColumnDimension(); ++j) {
                Fraction sum = Fraction.ZERO;
                for (int k = 0; k < mT.getColumnDimension(); ++k) {
                    sum = sum.add(new Fraction(k * 11 + i, 11).multiply(new Fraction(k * 11 + j, 11)));
                }
                Assert.assertEquals(sum, mTm.getEntry(i, j));
            }
        }

        final FieldMatrix<Fraction> mmT = m.multiply(mT);
        for (int i = 0; i < mmT.getRowDimension(); ++i) {
            for (int j = 0; j < mmT.getColumnDimension(); ++j) {
                Fraction sum = Fraction.ZERO;
                for (int k = 0; k < m.getColumnDimension(); ++k) {
                    sum = sum.add(new Fraction(i * 11 + k, 11).multiply(new Fraction(j * 11 + k, 11)));
                }
                Assert.assertEquals(sum, mmT.getEntry(i, j));
            }
        }

        final FieldMatrix<Fraction> sub1 = m.getSubMatrix(2, 9, 5, 20);
        for (int i = 0; i < sub1.getRowDimension(); ++i) {
            for (int j = 0; j < sub1.getColumnDimension(); ++j) {
                Assert.assertEquals(new Fraction((i + 2) * 11 + (j + 5), 11), sub1.getEntry(i, j));
            }
        }

        final FieldMatrix<Fraction> sub2 = m.getSubMatrix(10, 12, 3, 40);
        for (int i = 0; i < sub2.getRowDimension(); ++i) {
            for (int j = 0; j < sub2.getColumnDimension(); ++j) {
                Assert.assertEquals(new Fraction((i + 10) * 11 + (j + 3), 11), sub2.getEntry(i, j));
            }
        }

        final FieldMatrix<Fraction> sub3 = m.getSubMatrix(30, 34, 0, 5);
        for (int i = 0; i < sub3.getRowDimension(); ++i) {
            for (int j = 0; j < sub3.getColumnDimension(); ++j) {
                Assert.assertEquals(new Fraction((i + 30) * 11 + (j + 0), 11), sub3.getEntry(i, j));
            }
        }

        final FieldMatrix<Fraction> sub4 = m.getSubMatrix(30, 32, 32, 35);
        for (int i = 0; i < sub4.getRowDimension(); ++i) {
            for (int j = 0; j < sub4.getColumnDimension(); ++j) {
                Assert.assertEquals(new Fraction((i + 30) * 11 + (j + 32), 11), sub4.getEntry(i, j));
            }
        }

    }

    // Additional Test for BlockFieldMatrix<Fraction>Test.testMultiply

    private final Fraction[][] d3 = new Fraction[][] {
        { new Fraction(1), new Fraction(2), new Fraction(3), new Fraction(4) },
        { new Fraction(5), new Fraction(6), new Fraction(7), new Fraction(8) }
    };
    private final Fraction[][] d4 = new Fraction[][] {
        { new Fraction(1) },
        { new Fraction(2) },
        { new Fraction(3) },
        { new Fraction(4) }
    };
    private final Fraction[][] d5 = new Fraction[][] { { new Fraction(30) }, { new Fraction(70) } };

    @Test
    public void testMultiply2() {
        final FieldMatrix<Fraction> m3 = new BlockFieldMatrix<Fraction>(this.d3);
        final FieldMatrix<Fraction> m4 = new BlockFieldMatrix<Fraction>(this.d4);
        final FieldMatrix<Fraction> m5 = new BlockFieldMatrix<Fraction>(this.d5);
        TestUtils.assertEquals(m3.multiply(m4), m5);
    }

    /** test trace */
    @Test
    public void testTrace() {
        FieldMatrix<Fraction> m = new BlockFieldMatrix<Fraction>(this.id);
        Assert.assertEquals(new Fraction(3), m.getTrace());
        m = new BlockFieldMatrix<Fraction>(this.testData2);
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
        final FieldMatrix<Fraction> m = new BlockFieldMatrix<Fraction>(this.testData);
        TestUtils.assertEquals(new BlockFieldMatrix<Fraction>(this.testDataPlus2),
            m.scalarAdd(new Fraction(2)));
    }

    /** test operate */
    @Test
    public void testOperate() {
        FieldMatrix<Fraction> m = new BlockFieldMatrix<Fraction>(this.id);
        TestUtils.assertEquals(this.testVector, m.operate(this.testVector));
        TestUtils.assertEquals(this.testVector, m.operate(new ArrayFieldVector<Fraction>(this.testVector)).toArray());
        m = new BlockFieldMatrix<Fraction>(this.bigSingular);
        try {
            m.operate(this.testVector);
            Assert.fail("Expecting illegalArgumentException");
        } catch (final MathIllegalArgumentException ex) {
            // ignored
        }
    }

    @Test
    public void testOperateLarge() {
        final int p = (11 * BlockFieldMatrix.BLOCK_SIZE) / 10;
        final int q = (11 * BlockFieldMatrix.BLOCK_SIZE) / 10;
        final int r = BlockFieldMatrix.BLOCK_SIZE / 2;
        final Random random = new Random(111007463902334l);
        final FieldMatrix<Fraction> m1 = this.createRandomMatrix(random, p, q);
        final FieldMatrix<Fraction> m2 = this.createRandomMatrix(random, q, r);
        final FieldMatrix<Fraction> m1m2 = m1.multiply(m2);
        for (int i = 0; i < r; ++i) {
            TestUtils.assertEquals(m1m2.getColumn(i), m1.operate(m2.getColumn(i)));
        }
    }

    @Test
    public void testOperatePremultiplyLarge() {
        final int p = (11 * BlockFieldMatrix.BLOCK_SIZE) / 10;
        final int q = (11 * BlockFieldMatrix.BLOCK_SIZE) / 10;
        final int r = BlockFieldMatrix.BLOCK_SIZE / 2;
        final Random random = new Random(111007463902334l);
        final FieldMatrix<Fraction> m1 = this.createRandomMatrix(random, p, q);
        final FieldMatrix<Fraction> m2 = this.createRandomMatrix(random, q, r);
        final FieldMatrix<Fraction> m1m2 = m1.multiply(m2);
        for (int i = 0; i < p; ++i) {
            TestUtils.assertEquals(m1m2.getRow(i), m2.preMultiply(m1.getRow(i)));
        }
    }

    /** test issue MATH-209 */
    @Test
    public void testMath209() {
        final FieldMatrix<Fraction> a = new BlockFieldMatrix<Fraction>(new Fraction[][] {
            { new Fraction(1), new Fraction(2) },
            { new Fraction(3), new Fraction(4) },
            { new Fraction(5), new Fraction(6) }
        });
        final Fraction[] b = a.operate(new Fraction[] { new Fraction(1), new Fraction(1) });
        Assert.assertEquals(a.getRowDimension(), b.length);
        Assert.assertEquals(new Fraction(3), b[0]);
        Assert.assertEquals(new Fraction(7), b[1]);
        Assert.assertEquals(new Fraction(11), b[2]);
    }

    /** test transpose */
    @Test
    public void testTranspose() {
        FieldMatrix<Fraction> m = new BlockFieldMatrix<Fraction>(this.testData);
        final FieldMatrix<Fraction> mIT = new FieldLUDecomposition<Fraction>(m).getSolver().getInverse().transpose();
        final FieldMatrix<Fraction> mTI = new FieldLUDecomposition<Fraction>(m.transpose()).getSolver().getInverse();
        TestUtils.assertEquals(mIT, mTI);
        m = new BlockFieldMatrix<Fraction>(this.testData2);
        final FieldMatrix<Fraction> mt = new BlockFieldMatrix<Fraction>(this.testData2T);
        TestUtils.assertEquals(mt, m.transpose());
    }

    /** test preMultiply by vector */
    @Test
    public void testPremultiplyVector() {
        FieldMatrix<Fraction> m = new BlockFieldMatrix<Fraction>(this.testData);
        TestUtils.assertEquals(m.preMultiply(this.testVector), this.preMultTest);
        TestUtils.assertEquals(m.preMultiply(new ArrayFieldVector<Fraction>(this.testVector).getData()),
            this.preMultTest);
        m = new BlockFieldMatrix<Fraction>(this.bigSingular);
        try {
            m.preMultiply(this.testVector);
            Assert.fail("expecting MathIllegalArgumentException");
        } catch (final MathIllegalArgumentException ex) {
            // ignored
        }
    }

    @Test
    public void testPremultiply() {
        final FieldMatrix<Fraction> m3 = new BlockFieldMatrix<Fraction>(this.d3);
        final FieldMatrix<Fraction> m4 = new BlockFieldMatrix<Fraction>(this.d4);
        final FieldMatrix<Fraction> m5 = new BlockFieldMatrix<Fraction>(this.d5);
        TestUtils.assertEquals(m4.preMultiply(m3), m5);

        final BlockFieldMatrix<Fraction> m = new BlockFieldMatrix<Fraction>(this.testData);
        final BlockFieldMatrix<Fraction> mInv = new BlockFieldMatrix<Fraction>(this.testDataInv);
        final BlockFieldMatrix<Fraction> identity = new BlockFieldMatrix<Fraction>(this.id);
        TestUtils.assertEquals(m.preMultiply(mInv), identity);
        TestUtils.assertEquals(mInv.preMultiply(m), identity);
        TestUtils.assertEquals(m.preMultiply(identity), m);
        TestUtils.assertEquals(identity.preMultiply(mInv), mInv);
        try {
            m.preMultiply(new BlockFieldMatrix<Fraction>(this.bigSingular));
            Assert.fail("Expecting illegalArgumentException");
        } catch (final MathIllegalArgumentException ex) {
            // ignored
        }
    }

    @Test
    public void testGetVectors() {
        final FieldMatrix<Fraction> m = new BlockFieldMatrix<Fraction>(this.testData);
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
        final FieldMatrix<Fraction> m = new BlockFieldMatrix<Fraction>(this.testData);
        Assert.assertEquals(m.getEntry(0, 1), new Fraction(2));
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
        final FieldMatrix<Fraction> m = new BlockFieldMatrix<Fraction>(matrixData);
        // One more with three rows, two columns
        final Fraction[][] matrixData2 = {
            { new Fraction(1), new Fraction(2) },
            { new Fraction(2), new Fraction(5) },
            { new Fraction(1), new Fraction(7) }
        };
        final FieldMatrix<Fraction> n = new BlockFieldMatrix<Fraction>(matrixData2);
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
        final FieldMatrix<Fraction> coefficients = new BlockFieldMatrix<Fraction>(coefficientsData);
        final Fraction[] constants = {
            new Fraction(1), new Fraction(-2), new Fraction(1)
        };
        Fraction[] solution;
        solution = new FieldLUDecomposition<Fraction>(coefficients)
            .getSolver()
            .solve(new ArrayFieldVector<Fraction>(constants, false)).toArray();
        Assert.assertEquals(new Fraction(2).multiply(solution[0]).
            add(new Fraction(3).multiply(solution[1])).
            subtract(new Fraction(2).multiply(solution[2])),
            constants[0]);
        Assert.assertEquals(new Fraction(-1).multiply(solution[0]).
            add(new Fraction(7).multiply(solution[1])).
            add(new Fraction(6).multiply(solution[2])),
            constants[1]);
        Assert.assertEquals(new Fraction(4).multiply(solution[0]).
            subtract(new Fraction(3).multiply(solution[1])).
            subtract(new Fraction(5).multiply(solution[2])),
            constants[2]);

    }

    // test submatrix accessors
    @Test
    public void testGetSubMatrix() {
        final FieldMatrix<Fraction> m = new BlockFieldMatrix<Fraction>(this.subTestData);
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
                Assert.assertEquals(new BlockFieldMatrix<Fraction>(reference), sub);
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
                Assert.assertEquals(new BlockFieldMatrix<Fraction>(reference), sub);
            } else {
                Assert.fail("Expecting OutOfRangeException");
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
    public void testGetSetMatrixLarge() {
        final int n = 3 * BlockFieldMatrix.BLOCK_SIZE;
        final FieldMatrix<Fraction> m =
            new BlockFieldMatrix<Fraction>(FractionField.getInstance(), n, n);
        final FieldMatrix<Fraction> sub =
            new BlockFieldMatrix<Fraction>(FractionField.getInstance(), n - 4, n - 4).scalarAdd(new Fraction(1));

        m.setSubMatrix(sub.getData(), 2, 2);
        for (int i = 0; i < n; ++i) {
            for (int j = 0; j < n; ++j) {
                if ((i < 2) || (i > n - 3) || (j < 2) || (j > n - 3)) {
                    Assert.assertEquals(new Fraction(0), m.getEntry(i, j));
                } else {
                    Assert.assertEquals(new Fraction(1), m.getEntry(i, j));
                }
            }
        }
        Assert.assertEquals(sub, m.getSubMatrix(2, n - 3, 2, n - 3));
    }

    @Test
    public void testCopySubMatrix() {
        final FieldMatrix<Fraction> m = new BlockFieldMatrix<Fraction>(this.subTestData);
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
                Assert.assertEquals(new BlockFieldMatrix<Fraction>(reference), new BlockFieldMatrix<Fraction>(sub));
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
                Assert.assertEquals(new BlockFieldMatrix<Fraction>(reference), new BlockFieldMatrix<Fraction>(sub));
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
        final FieldMatrix<Fraction> m = new BlockFieldMatrix<Fraction>(this.subTestData);
        final FieldMatrix<Fraction> mRow0 = new BlockFieldMatrix<Fraction>(this.subRow0);
        final FieldMatrix<Fraction> mRow3 = new BlockFieldMatrix<Fraction>(this.subRow3);
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
        final FieldMatrix<Fraction> m = new BlockFieldMatrix<Fraction>(this.subTestData);
        final FieldMatrix<Fraction> mRow3 = new BlockFieldMatrix<Fraction>(this.subRow3);
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
    public void testGetSetRowMatrixLarge() {
        final int n = 3 * BlockFieldMatrix.BLOCK_SIZE;
        final FieldMatrix<Fraction> m =
            new BlockFieldMatrix<Fraction>(FractionField.getInstance(), n, n);
        final FieldMatrix<Fraction> sub =
            new BlockFieldMatrix<Fraction>(FractionField.getInstance(), 1, n).scalarAdd(new Fraction(1));

        m.setRowMatrix(2, sub);
        for (int i = 0; i < n; ++i) {
            for (int j = 0; j < n; ++j) {
                if (i != 2) {
                    Assert.assertEquals(new Fraction(0), m.getEntry(i, j));
                } else {
                    Assert.assertEquals(new Fraction(1), m.getEntry(i, j));
                }
            }
        }
        Assert.assertEquals(sub, m.getRowMatrix(2));

    }

    @Test
    public void testGetColumnMatrix() {
        final FieldMatrix<Fraction> m = new BlockFieldMatrix<Fraction>(this.subTestData);
        final FieldMatrix<Fraction> mColumn1 = new BlockFieldMatrix<Fraction>(this.subColumn1);
        final FieldMatrix<Fraction> mColumn3 = new BlockFieldMatrix<Fraction>(this.subColumn3);
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
        final FieldMatrix<Fraction> m = new BlockFieldMatrix<Fraction>(this.subTestData);
        final FieldMatrix<Fraction> mColumn3 = new BlockFieldMatrix<Fraction>(this.subColumn3);
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
    public void testGetSetColumnMatrixLarge() {
        final int n = 3 * BlockFieldMatrix.BLOCK_SIZE;
        final FieldMatrix<Fraction> m =
            new BlockFieldMatrix<Fraction>(FractionField.getInstance(), n, n);
        final FieldMatrix<Fraction> sub =
            new BlockFieldMatrix<Fraction>(FractionField.getInstance(), n, 1).scalarAdd(new Fraction(1));

        m.setColumnMatrix(2, sub);
        for (int i = 0; i < n; ++i) {
            for (int j = 0; j < n; ++j) {
                if (j != 2) {
                    Assert.assertEquals(new Fraction(0), m.getEntry(i, j));
                } else {
                    Assert.assertEquals(new Fraction(1), m.getEntry(i, j));
                }
            }
        }
        Assert.assertEquals(sub, m.getColumnMatrix(2));

    }

    @Test
    public void testGetRowVector() {
        final FieldMatrix<Fraction> m = new BlockFieldMatrix<Fraction>(this.subTestData);
        final FieldVector<Fraction> mRow0 = new ArrayFieldVector<Fraction>(this.subRow0[0]);
        final FieldVector<Fraction> mRow3 = new ArrayFieldVector<Fraction>(this.subRow3[0]);
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
        final FieldMatrix<Fraction> m = new BlockFieldMatrix<Fraction>(this.subTestData);
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
    public void testGetSetRowVectorLarge() {
        final int n = 3 * BlockFieldMatrix.BLOCK_SIZE;
        final FieldMatrix<Fraction> m = new BlockFieldMatrix<Fraction>(FractionField.getInstance(), n, n);
        final FieldVector<Fraction> sub = new ArrayFieldVector<Fraction>(n, new Fraction(1));

        m.setRowVector(2, sub);
        for (int i = 0; i < n; ++i) {
            for (int j = 0; j < n; ++j) {
                if (i != 2) {
                    Assert.assertEquals(new Fraction(0), m.getEntry(i, j));
                } else {
                    Assert.assertEquals(new Fraction(1), m.getEntry(i, j));
                }
            }
        }
        Assert.assertEquals(sub, m.getRowVector(2));

    }

    @Test
    public void testGetColumnVector() {
        final FieldMatrix<Fraction> m = new BlockFieldMatrix<Fraction>(this.subTestData);
        final FieldVector<Fraction> mColumn1 = this.columnToVector(this.subColumn1);
        final FieldVector<Fraction> mColumn3 = this.columnToVector(this.subColumn3);
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
        final FieldMatrix<Fraction> m = new BlockFieldMatrix<Fraction>(this.subTestData);
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

    @Test
    public void testGetSetColumnVectorLarge() {
        final int n = 3 * BlockFieldMatrix.BLOCK_SIZE;
        final FieldMatrix<Fraction> m = new BlockFieldMatrix<Fraction>(FractionField.getInstance(), n, n);
        final FieldVector<Fraction> sub = new ArrayFieldVector<Fraction>(n, new Fraction(1));

        m.setColumnVector(2, sub);
        for (int i = 0; i < n; ++i) {
            for (int j = 0; j < n; ++j) {
                if (j != 2) {
                    Assert.assertEquals(new Fraction(0), m.getEntry(i, j));
                } else {
                    Assert.assertEquals(new Fraction(1), m.getEntry(i, j));
                }
            }
        }
        Assert.assertEquals(sub, m.getColumnVector(2));

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
        final FieldMatrix<Fraction> m = new BlockFieldMatrix<Fraction>(this.subTestData);
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
        final FieldMatrix<Fraction> m = new BlockFieldMatrix<Fraction>(this.subTestData);
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
    public void testGetSetRowLarge() {
        final int n = 3 * BlockFieldMatrix.BLOCK_SIZE;
        final FieldMatrix<Fraction> m = new BlockFieldMatrix<Fraction>(FractionField.getInstance(), n, n);
        final Fraction[] sub = new Fraction[n];
        Arrays.fill(sub, new Fraction(1));

        m.setRow(2, sub);
        for (int i = 0; i < n; ++i) {
            for (int j = 0; j < n; ++j) {
                if (i != 2) {
                    Assert.assertEquals(new Fraction(0), m.getEntry(i, j));
                } else {
                    Assert.assertEquals(new Fraction(1), m.getEntry(i, j));
                }
            }
        }
        this.checkArrays(sub, m.getRow(2));

    }

    @Test
    public void testGetColumn() {
        final FieldMatrix<Fraction> m = new BlockFieldMatrix<Fraction>(this.subTestData);
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
        final FieldMatrix<Fraction> m = new BlockFieldMatrix<Fraction>(this.subTestData);
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

    @Test
    public void testGetSetColumnLarge() {
        final int n = 3 * BlockFieldMatrix.BLOCK_SIZE;
        final FieldMatrix<Fraction> m = new BlockFieldMatrix<Fraction>(FractionField.getInstance(), n, n);
        final Fraction[] sub = new Fraction[n];
        Arrays.fill(sub, new Fraction(1));

        m.setColumn(2, sub);
        for (int i = 0; i < n; ++i) {
            for (int j = 0; j < n; ++j) {
                if (j != 2) {
                    Assert.assertEquals(new Fraction(0), m.getEntry(i, j));
                } else {
                    Assert.assertEquals(new Fraction(1), m.getEntry(i, j));
                }
            }
        }
        this.checkArrays(sub, m.getColumn(2));

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
        final BlockFieldMatrix<Fraction> m = new BlockFieldMatrix<Fraction>(this.testData);
        final BlockFieldMatrix<Fraction> m1 = (BlockFieldMatrix<Fraction>) m.copy();
        final BlockFieldMatrix<Fraction> mt = (BlockFieldMatrix<Fraction>) m.transpose();
        Assert.assertTrue(m.hashCode() != mt.hashCode());
        Assert.assertEquals(m.hashCode(), m1.hashCode());
        Assert.assertEquals(m, m);
        Assert.assertEquals(m, m1);
        Assert.assertFalse(m.equals(null));
        Assert.assertFalse(m.equals(mt));
        Assert.assertFalse(m.equals(new BlockFieldMatrix<Fraction>(this.bigSingular)));
    }

    @Test
    public void testToString() {
        final BlockFieldMatrix<Fraction> m = new BlockFieldMatrix<Fraction>(this.testData);
        Assert.assertEquals("BlockFieldMatrix{{1,2,3},{2,5,3},{1,0,8}}", m.toString());
    }

    @Test
    public void testSetSubMatrix() {
        final BlockFieldMatrix<Fraction> m = new BlockFieldMatrix<Fraction>(this.testData);
        m.setSubMatrix(this.detData2, 1, 1);
        FieldMatrix<Fraction> expected = new BlockFieldMatrix<Fraction>
            (new Fraction[][] { { new Fraction(1), new Fraction(2), new Fraction(3) },
                { new Fraction(2), new Fraction(1), new Fraction(3) },
                { new Fraction(1), new Fraction(2), new Fraction(4) } });
        Assert.assertEquals(expected, m);

        m.setSubMatrix(this.detData2, 0, 0);
        expected = new BlockFieldMatrix<Fraction>
            (new Fraction[][] { { new Fraction(1), new Fraction(3), new Fraction(3) },
                { new Fraction(2), new Fraction(4), new Fraction(3) },
                { new Fraction(1), new Fraction(2), new Fraction(4) } });
        Assert.assertEquals(expected, m);

        m.setSubMatrix(this.testDataPlus2, 0, 0);
        expected = new BlockFieldMatrix<Fraction>
            (new Fraction[][] { { new Fraction(3), new Fraction(4), new Fraction(5) },
                { new Fraction(4), new Fraction(7), new Fraction(5) },
                { new Fraction(3), new Fraction(2), new Fraction(10) } });
        Assert.assertEquals(expected, m);

        // javadoc example
        final BlockFieldMatrix<Fraction> matrix =
            new BlockFieldMatrix<Fraction>(new Fraction[][] {
                { new Fraction(1), new Fraction(2), new Fraction(3), new Fraction(4) },
                { new Fraction(5), new Fraction(6), new Fraction(7), new Fraction(8) },
                { new Fraction(9), new Fraction(0), new Fraction(1), new Fraction(2) }
            });
        matrix.setSubMatrix(new Fraction[][] {
            { new Fraction(3), new Fraction(4) },
            { new Fraction(5), new Fraction(6) }
        }, 1, 1);
        expected =
            new BlockFieldMatrix<Fraction>(new Fraction[][] {
                { new Fraction(1), new Fraction(2), new Fraction(3), new Fraction(4) },
                { new Fraction(5), new Fraction(3), new Fraction(4), new Fraction(8) },
                { new Fraction(9), new Fraction(5), new Fraction(6), new Fraction(2) }
            });
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

        FieldMatrix<Fraction> m = new BlockFieldMatrix<Fraction>(FractionField.getInstance(), rows, columns);
        m.walkInRowOrder(new SetVisitor());
        GetVisitor getVisitor = new GetVisitor();
        m.walkInOptimizedOrder(getVisitor);
        Assert.assertEquals(rows * columns, getVisitor.getCount());

        m = new BlockFieldMatrix<Fraction>(FractionField.getInstance(), rows, columns);
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

        m = new BlockFieldMatrix<Fraction>(FractionField.getInstance(), rows, columns);
        m.walkInColumnOrder(new SetVisitor());
        getVisitor = new GetVisitor();
        m.walkInOptimizedOrder(getVisitor);
        Assert.assertEquals(rows * columns, getVisitor.getCount());

        m = new BlockFieldMatrix<Fraction>(FractionField.getInstance(), rows, columns);
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

        m = new BlockFieldMatrix<Fraction>(FractionField.getInstance(), rows, columns);
        m.walkInOptimizedOrder(new SetVisitor());
        getVisitor = new GetVisitor();
        m.walkInRowOrder(getVisitor);
        Assert.assertEquals(rows * columns, getVisitor.getCount());

        m = new BlockFieldMatrix<Fraction>(FractionField.getInstance(), rows, columns);
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

        m = new BlockFieldMatrix<Fraction>(FractionField.getInstance(), rows, columns);
        m.walkInOptimizedOrder(new SetVisitor());
        getVisitor = new GetVisitor();
        m.walkInColumnOrder(getVisitor);
        Assert.assertEquals(rows * columns, getVisitor.getCount());

        m = new BlockFieldMatrix<Fraction>(FractionField.getInstance(), rows, columns);
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
        final BlockFieldMatrix<Fraction> m = new BlockFieldMatrix<Fraction>(this.testData);
        Assert.assertEquals(m, TestUtils.serializeAndRecover(m));
    }

    private static class SetVisitor extends DefaultFieldMatrixChangingVisitor<Fraction> {
        public SetVisitor() {
            super(Fraction.ZERO);
        }

        @Override
        public Fraction visit(final int i, final int j, final Fraction value) {
            return new Fraction(i * 11 + j, 11);
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
            Assert.assertEquals(new Fraction(i * 11 + j, 11), value);
        }

        public int getCount() {
            return this.count;
        }
    }

    private BlockFieldMatrix<Fraction> createRandomMatrix(final Random r, final int rows, final int columns) {
        final BlockFieldMatrix<Fraction> m =
            new BlockFieldMatrix<Fraction>(FractionField.getInstance(), rows, columns);
        for (int i = 0; i < rows; ++i) {
            for (int j = 0; j < columns; ++j) {
                final int p = r.nextInt(20) - 10;
                int q = r.nextInt(20) - 10;
                if (q == 0) {
                    q = 1;
                }
                m.setEntry(i, j, new Fraction(p, q));
            }
        }
        return m;
    }
}
