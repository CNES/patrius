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
 * VERSION:4.5:DM:DM-2300:27/05/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:306:17/11/2014: coverage
 * VERSION::DM:596:12/04/2016:Improve test coherence
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.linear;

import java.math.BigDecimal;
import java.util.Locale;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.MathArithmeticException;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.exception.NoDataException;
import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;
import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;
import fr.cnes.sirius.patrius.math.fraction.BigFraction;
import fr.cnes.sirius.patrius.math.fraction.Fraction;
import fr.cnes.sirius.patrius.math.fraction.FractionConversionException;
import fr.cnes.sirius.patrius.math.fraction.FractionField;
import fr.cnes.sirius.patrius.math.util.Precision;

/**
 * Test cases for the {@link MatrixUtils} class.
 * 
 * @version $Id: MatrixUtilsTest.java 18108 2017-10-04 06:45:27Z bignon $
 */

public final class MatrixUtilsTest {

    protected double[][] testData = { { 1d, 2d, 3d }, { 2d, 5d, 3d }, { 1d, 0d, 8d } };
    protected double[][] nullMatrix = null;
    protected double[] row = { 1, 2, 3 };
    protected BigDecimal[] bigRow = { new BigDecimal(1), new BigDecimal(2), new BigDecimal(3) };
    protected String[] stringRow = { "1", "2", "3" };
    protected Fraction[] fractionRow = { new Fraction(1), new Fraction(2), new Fraction(3) };
    protected double[][] rowMatrix = { { 1, 2, 3 } };
    protected BigDecimal[][] bigRowMatrix = { { new BigDecimal(1), new BigDecimal(2),
            new BigDecimal(3) } };
    protected String[][] stringRowMatrix = { { "1", "2", "3" } };
    protected Fraction[][] fractionRowMatrix = { { new Fraction(1), new Fraction(2),
            new Fraction(3) } };
    protected double[] col = { 0, 4, 6 };
    protected BigDecimal[] bigCol = { new BigDecimal(0), new BigDecimal(4), new BigDecimal(6) };
    protected String[] stringCol = { "0", "4", "6" };
    protected Fraction[] fractionCol = { new Fraction(0), new Fraction(4), new Fraction(6) };
    protected double[] nullDoubleArray = null;
    protected double[][] colMatrix = { { 0 }, { 4 }, { 6 } };
    protected BigDecimal[][] bigColMatrix = { { new BigDecimal(0) }, { new BigDecimal(4) },
            { new BigDecimal(6) } };
    protected String[][] stringColMatrix = { { "0" }, { "4" }, { "6" } };
    protected Fraction[][] fractionColMatrix = { { new Fraction(0) }, { new Fraction(4) },
            { new Fraction(6) } };

    /**
     * Initialization.
     */
    @BeforeClass
    public static void init() {
        Locale.setDefault(Locale.ENGLISH);
    }

    /**
     * For coverage purposes, tests the if(data == null) in method createRealVector.
     */
    @Test(expected = NullArgumentException.class)
    public void testExceptionCreateRealVector() {
        final double[] data = null;
        MatrixUtils.createRealVector(data);
    }

    @Test
    public void testCreateRealMatrix() {
        Assert.assertEquals(new BlockRealMatrix(this.testData),
                MatrixUtils.createRealMatrix(this.testData));
        try {
            MatrixUtils.createRealMatrix(new double[][] { { 1 }, { 1, 2 } }); // ragged
            Assert.fail("Expecting MathIllegalArgumentException");
        } catch (final MathIllegalArgumentException ex) {
            // expected
        }
        try {
            MatrixUtils.createRealMatrix(new double[][] { {}, {} }); // no columns
            Assert.fail("Expecting MathIllegalArgumentException");
        } catch (final MathIllegalArgumentException ex) {
            // expected
        }
        try {
            MatrixUtils.createRealMatrix(null); // null
            Assert.fail("Expecting NullArgumentException");
        } catch (final NullArgumentException ex) {
            // expected
        }
    }

    @Test
    public void testcreateFieldMatrix() {
        Assert.assertEquals(new Array2DRowFieldMatrix<Fraction>(asFraction(this.testData)),
                MatrixUtils.createFieldMatrix(asFraction(this.testData)));
        Assert.assertEquals(new Array2DRowFieldMatrix<Fraction>(FractionField.getInstance(),
                this.fractionColMatrix), MatrixUtils.createFieldMatrix(this.fractionColMatrix));
        try {
            MatrixUtils.createFieldMatrix(asFraction(new double[][] { { 1 }, { 1, 2 } })); // ragged
            Assert.fail("Expecting MathIllegalArgumentException");
        } catch (final MathIllegalArgumentException ex) {
            // expected
        }
        try {
            MatrixUtils.createFieldMatrix(asFraction(new double[][] { {}, {} })); // no columns
            Assert.fail("Expecting MathIllegalArgumentException");
        } catch (final MathIllegalArgumentException ex) {
            // expected
        }
        try {
            MatrixUtils.createFieldMatrix((Fraction[][]) null); // null
            Assert.fail("Expecting NullArgumentException");
        } catch (final NullArgumentException ex) {
            // expected
        }
    }

    @Test
    public void testCreateRowRealMatrix() {
        Assert.assertEquals(MatrixUtils.createRowRealMatrix(this.row), new BlockRealMatrix(
                this.rowMatrix));
        try {
            MatrixUtils.createRowRealMatrix(new double[] {}); // empty
            Assert.fail("Expecting NotStrictlyPositiveException");
        } catch (final NotStrictlyPositiveException ex) {
            // expected
        }
        try {
            MatrixUtils.createRowRealMatrix(null); // null
            Assert.fail("Expecting NullArgumentException");
        } catch (final NullArgumentException ex) {
            // expected
        }
    }

    @Test
    public void testCreateRowFieldMatrix() {
        Assert.assertEquals(MatrixUtils.createRowFieldMatrix(asFraction(this.row)),
                new Array2DRowFieldMatrix<Fraction>(asFraction(this.rowMatrix)));
        Assert.assertEquals(MatrixUtils.createRowFieldMatrix(this.fractionRow),
                new Array2DRowFieldMatrix<Fraction>(this.fractionRowMatrix));
        try {
            MatrixUtils.createRowFieldMatrix(new Fraction[] {}); // empty
            Assert.fail("Expecting MathIllegalArgumentException");
        } catch (final MathIllegalArgumentException ex) {
            // expected
        }
        try {
            MatrixUtils.createRowFieldMatrix((Fraction[]) null); // null
            Assert.fail("Expecting NullArgumentException");
        } catch (final NullArgumentException ex) {
            // expected
        }
    }

    @Test
    public void testCreateColumnRealMatrix() {
        Assert.assertEquals(MatrixUtils.createColumnRealMatrix(this.col), new BlockRealMatrix(
                this.colMatrix));
        try {
            MatrixUtils.createColumnRealMatrix(new double[] {}); // empty
            Assert.fail("Expecting MathIllegalArgumentException");
        } catch (final MathIllegalArgumentException ex) {
            // expected
        }
        try {
            MatrixUtils.createColumnRealMatrix(null); // null
            Assert.fail("Expecting NullArgumentException");
        } catch (final NullArgumentException ex) {
            // expected
        }
    }

    @Test
    public void testCreateColumnFieldMatrix() {
        Assert.assertEquals(MatrixUtils.createColumnFieldMatrix(asFraction(this.col)),
                new Array2DRowFieldMatrix<Fraction>(asFraction(this.colMatrix)));
        Assert.assertEquals(MatrixUtils.createColumnFieldMatrix(this.fractionCol),
                new Array2DRowFieldMatrix<Fraction>(this.fractionColMatrix));

        try {
            MatrixUtils.createColumnFieldMatrix(new Fraction[] {}); // empty
            Assert.fail("Expecting MathIllegalArgumentException");
        } catch (final MathIllegalArgumentException ex) {
            // expected
        }
        try {
            MatrixUtils.createColumnFieldMatrix((Fraction[]) null); // null
            Assert.fail("Expecting NullArgumentException");
        } catch (final NullArgumentException ex) {
            // expected
        }
    }

    /**
     * Verifies that the matrix is an identity matrix
     */
    protected void checkIdentityMatrix(final RealMatrix m) {
        for (int i = 0; i < m.getRowDimension(); i++) {
            for (int j = 0; j < m.getColumnDimension(); j++) {
                if (i == j) {
                    Assert.assertEquals(m.getEntry(i, j), 1d, 0);
                } else {
                    Assert.assertEquals(m.getEntry(i, j), 0d, 0);
                }
            }
        }
    }

    @Test
    public void testCreateIdentityMatrix() {
        this.checkIdentityMatrix(MatrixUtils.createRealIdentityMatrix(3));
        this.checkIdentityMatrix(MatrixUtils.createRealIdentityMatrix(2));
        this.checkIdentityMatrix(MatrixUtils.createRealIdentityMatrix(1));
        try {
            MatrixUtils.createRealIdentityMatrix(0);
            Assert.fail("Expecting MathIllegalArgumentException");
        } catch (final MathIllegalArgumentException ex) {
            // expected
        }
    }

    /**
     * Verifies that the matrix is an identity matrix
     */
    protected void checkIdentityFieldMatrix(final FieldMatrix<Fraction> m) {
        for (int i = 0; i < m.getRowDimension(); i++) {
            for (int j = 0; j < m.getColumnDimension(); j++) {
                if (i == j) {
                    Assert.assertEquals(m.getEntry(i, j), Fraction.ONE);
                } else {
                    Assert.assertEquals(m.getEntry(i, j), Fraction.ZERO);
                }
            }
        }
    }

    @Test
    public void testcreateFieldIdentityMatrix() {
        this.checkIdentityFieldMatrix(MatrixUtils.createFieldIdentityMatrix(
                FractionField.getInstance(), 3));
        this.checkIdentityFieldMatrix(MatrixUtils.createFieldIdentityMatrix(
                FractionField.getInstance(), 2));
        this.checkIdentityFieldMatrix(MatrixUtils.createFieldIdentityMatrix(
                FractionField.getInstance(), 1));
        try {
            MatrixUtils.createRealIdentityMatrix(0);
            Assert.fail("Expecting MathIllegalArgumentException");
        } catch (final MathIllegalArgumentException ex) {
            // expected
        }
    }

    @Test
    public void testBigFractionConverter() {
        final BigFraction[][] bfData = {
                { new BigFraction(1), new BigFraction(2), new BigFraction(3) },
                { new BigFraction(2), new BigFraction(5), new BigFraction(3) },
                { new BigFraction(1), new BigFraction(0), new BigFraction(8) } };
        final FieldMatrix<BigFraction> m = new Array2DRowFieldMatrix<BigFraction>(bfData, false);
        final RealMatrix converted = MatrixUtils.bigFractionMatrixToRealMatrix(m);
        final RealMatrix reference = new Array2DRowRealMatrix(this.testData, false);
        Assert.assertEquals(0.0, converted.subtract(reference).getNorm(), 0.0);
    }

    @Test
    public void testFractionConverter() {
        final Fraction[][] fData = { { new Fraction(1), new Fraction(2), new Fraction(3) },
                { new Fraction(2), new Fraction(5), new Fraction(3) },
                { new Fraction(1), new Fraction(0), new Fraction(8) } };
        final FieldMatrix<Fraction> m = new Array2DRowFieldMatrix<Fraction>(fData, false);
        final RealMatrix converted = MatrixUtils.fractionMatrixToRealMatrix(m);
        final RealMatrix reference = new Array2DRowRealMatrix(this.testData, false);
        Assert.assertEquals(0.0, converted.subtract(reference).getNorm(), 0.0);
    }

    public static final Fraction[][] asFraction(final double[][] data) {
        final Fraction d[][] = new Fraction[data.length][];
        try {
            for (int i = 0; i < data.length; ++i) {
                final double[] dataI = data[i];
                final Fraction[] dI = new Fraction[dataI.length];
                for (int j = 0; j < dataI.length; ++j) {
                    dI[j] = new Fraction(dataI[j]);
                }
                d[i] = dI;
            }
        } catch (final FractionConversionException fce) {
            Assert.fail(fce.getMessage());
        }
        return d;
    }

    public static final Fraction[] asFraction(final double[] data) {
        final Fraction d[] = new Fraction[data.length];
        try {
            for (int i = 0; i < data.length; ++i) {
                d[i] = new Fraction(data[i]);
            }
        } catch (final FractionConversionException fce) {
            Assert.fail(fce.getMessage());
        }
        return d;
    }

    @Test
    public void testSolveLowerTriangularSystem() {
        final RealMatrix rm = new Array2DRowRealMatrix(new double[][] { { 2, 0, 0, 0 },
                { 1, 1, 0, 0 }, { 3, 3, 3, 0 }, { 3, 3, 3, 4 } }, false);
        final RealVector b = new ArrayRealVector(new double[] { 2, 3, 4, 8 }, false);
        MatrixUtils.solveLowerTriangularSystem(rm, b);
        TestUtils.assertEquals(new double[] { 1, 2, -1.66666666666667, 1.0 }, b.toArray(), 1.0e-12);

        // Exception
        try {
            MatrixUtils.solveLowerTriangularSystem(null, b);
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            Assert.assertTrue(true);
        }
        try {
            MatrixUtils.solveLowerTriangularSystem(new BlockRealMatrix(4, 3), b);
            Assert.fail();
        } catch (final NonSquareMatrixException e) {
            Assert.assertTrue(true);
        }
        try {
            MatrixUtils.solveLowerTriangularSystem(new BlockRealMatrix(4, 4), b);
            Assert.fail();
        } catch (final MathArithmeticException e) {
            Assert.assertTrue(true);
        }
    }

    /*
     * Taken from R manual http://stat.ethz.ch/R-manual/R-patched/library/base/html/backsolve.html
     */
    @Test
    public void testSolveUpperTriangularSystem() {
        final RealMatrix rm = new Array2DRowRealMatrix(new double[][] { { 1, 2, 3 }, { 0, 1, 1 },
                { 0, 0, 2 } }, false);
        final RealVector b = new ArrayRealVector(new double[] { 8, 4, 2 }, false);
        MatrixUtils.solveUpperTriangularSystem(rm, b);
        TestUtils.assertEquals(new double[] { -1, 3, 1 }, b.toArray(), 1.0e-12);

        // Exception
        try {
            MatrixUtils.solveUpperTriangularSystem(null, b);
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            Assert.assertTrue(true);
        }
        try {
            MatrixUtils.solveUpperTriangularSystem(new BlockRealMatrix(3, 4), b);
            Assert.fail();
        } catch (final NonSquareMatrixException e) {
            Assert.assertTrue(true);
        }
        try {
            MatrixUtils.solveUpperTriangularSystem(new BlockRealMatrix(3, 3), b);
            Assert.fail();
        } catch (final MathArithmeticException e) {
            Assert.assertTrue(true);
        }
    }

    /**
     * This test should probably be replaced by one that could show
     * whether this algorithm can sometimes perform better (precision- or
     * performance-wise) than the direct inversion of the whole matrix.
     */
    @Test
    public void testBlockInverse() {
        final double[][] data = { { -1, 0, 123, 4 }, { -56, 78.9, -0.1, -23.4 },
                { 5.67, 8, -9, 1011 }, { 12, 345, -67.8, 9 }, };

        final RealMatrix m = new Array2DRowRealMatrix(data);
        final int len = data.length;
        final double tol = 1e-14;

        for (int splitIndex = 0; splitIndex < 3; splitIndex++) {
            final RealMatrix mInv = MatrixUtils.blockInverse(m, splitIndex);
            final RealMatrix id = m.multiply(mInv);

            // Check that we recovered the identity matrix.
            for (int i = 0; i < len; i++) {
                for (int j = 0; j < len; j++) {
                    final double entry = id.getEntry(i, j);
                    if (i == j) {
                        Assert.assertEquals("[" + i + "][" + j + "]", 1, entry, tol);
                    } else {
                        Assert.assertEquals("[" + i + "][" + j + "]", 0, entry, tol);
                    }
                }
            }
        }

        // Exception
        try {
            MatrixUtils.blockInverse(new BlockRealMatrix(3, 4), 0);
            Assert.fail();
        } catch (final NonSquareMatrixException e) {
            Assert.assertTrue(true);
        }
        try {
            MatrixUtils.blockInverse(new BlockRealMatrix(4, 4), 0);
            Assert.fail();
        } catch (final SingularMatrixException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testCheckSymmetric1() {
        final double[][] dataSym = { { 1, 2, 3 }, { 2, 2, 5 }, { 3, 5, 6 }, };
        MatrixUtils.checkSymmetric(MatrixUtils.createRealMatrix(dataSym), Math.ulp(1d));
    }

    @Test(expected = NonSymmetricMatrixException.class)
    public void testCheckSymmetric2() {
        final double[][] dataNonSym = { { 1, 2, -3 }, { 2, 2, 5 }, { 3, 5, 6 }, };
        MatrixUtils.checkSymmetric(MatrixUtils.createRealMatrix(dataNonSym), Math.ulp(1d));
    }

    /**
     * For coverage purposes, tests the method checkSubMatrixIndex(AnyMatrix, int, int, int, int)
     * and
     * checkSubMatrixIndex(AnyMatrix, int[], int[])
     * 
     */
    @Test
    public void testExceptionCheckSubMatrixIndex() {

        final double[][] data = { { 1, 2 }, { 3, 4 } };
        final Array2DRowRealMatrix matrix = new Array2DRowRealMatrix(data);
        final int[] nullVec = null;
        final int[] b = { 0, 1 };
        final int[] emptyVec = new int[] {};

        // tests if (endColumn < startColumn) in checkSubMatrixIndex(AnyMatrix, int, int, int, int).
        try {
            MatrixUtils.checkSubMatrixIndex(matrix, 0, 1, 1, 0);
            Assert.fail();
        } catch (final NumberIsTooSmallException e) {
            // expected
            Assert.assertTrue(true);
        }

        // tests if (selectedRows == null) in checkSubMatrixIndex(AnyMatrix, int[], int[])
        try {
            MatrixUtils.checkSubMatrixIndex(matrix, nullVec, b);
            Assert.fail();
        } catch (final NullArgumentException e) {
            // expected
            Assert.assertTrue(true);
        }

        // tests if (selectedColumns == null) in checkSubMatrixIndex(AnyMatrix, int[], int[])
        try {
            MatrixUtils.checkSubMatrixIndex(matrix, b, nullVec);
            Assert.fail();
        } catch (final NullArgumentException e) {
            // expected
            Assert.assertTrue(true);
        }

        // tests if (selectedColumns.length == 0) in checkSubMatrixIndex(AnyMatrix, int[], int[])
        try {
            MatrixUtils.checkSubMatrixIndex(matrix, b, emptyVec);
            Assert.fail();
        } catch (final NoDataException e) {
            // expected
            Assert.assertTrue(true);
        }
    }

    /**
     * For coverage purposes, tests the if (rows != matrix.getColumnDimension() ) in method
     * isSymmetricInternal. This method is private, therefore we use checkSymmetric (with boolean
     * equals
     * true) and isSymmetric (with boolean equals false).
     */
    @Test
    public void testExceptionIsSymmetricInternal() {

        final double[][] data = { { 1, 2 }, { 3, 4 }, { 5, 6 } };
        final Array2DRowRealMatrix matrix = new Array2DRowRealMatrix(data);
        final double eps = Precision.DOUBLE_COMPARISON_EPSILON;

        // tests if (raiseException) when calls checkSymmetric
        try {
            MatrixUtils.checkSymmetric(matrix, eps);
            Assert.fail();
        } catch (final NonSquareMatrixException e) {
            // expected
            Assert.assertTrue(true);
        }
    }

    /**
     * Tests the method that checks if an array contains any duplicates.
     * 
     * <p>
     * Tested method:<br>
     * {@linkplain MatrixUtils#checkDuplicates(int[])}
     * </p>
     */
    @Test
    public void testCheckDuplicates() {
        int[] array;

        // Empty array
        array = new int[0];
        MatrixUtils.checkDuplicates(array);

        // No duplicates
        array = new int[] { 0, 1, 2, 3 };
        MatrixUtils.checkDuplicates(array);

        // The array contains duplicates
        try {
            array = new int[] { 0, 1, 2, 3, 1 };
            MatrixUtils.checkDuplicates(array);
            Assert.fail();
        } catch (final MathIllegalArgumentException e) {
            final String expectedMessage = "Element : 1 is duplicated";
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the method that returns a row permutation index array.
     * 
     * <p>
     * Tested method:<br>
     * {@linkplain MatrixUtils#getRowPermutationIndexArray(AnyMatrix, int[])}
     * </p>
     */
    @Test
    public void testGetRowPermutationIndexArray() {
        int[] result;
        int[] expected;
        int[] preSelectedRows;

        final AnyMatrix matrix = new Array2DRowRealMatrix(5, 4);

        // No preselected rows
        preSelectedRows = new int[] {};
        expected = new int[] { 0, 1, 2, 3, 4 };
        result = MatrixUtils.getRowPermutationIndexArray(matrix, preSelectedRows);
        Assert.assertArrayEquals(expected, result);

        // The preselected rows are the first rows
        preSelectedRows = new int[] { 0, 1, 2 };
        expected = new int[] { 0, 1, 2, 3, 4 };
        result = MatrixUtils.getRowPermutationIndexArray(matrix, preSelectedRows);
        Assert.assertArrayEquals(expected, result);

        // The preselected rows are the last rows
        preSelectedRows = new int[] { 3, 4 };
        expected = new int[] { 3, 4, 0, 1, 2 };
        result = MatrixUtils.getRowPermutationIndexArray(matrix, preSelectedRows);
        Assert.assertArrayEquals(expected, result);

        // The preselected rows are not in increasing order
        preSelectedRows = new int[] { 1, 3, 0 };
        expected = new int[] { 1, 3, 0, 2, 4 };
        result = MatrixUtils.getRowPermutationIndexArray(matrix, preSelectedRows);
        Assert.assertArrayEquals(expected, result);

        // The preselected row index array contains duplicates
        preSelectedRows = new int[] { 1, 3, 0, 1 };
        expected = new int[] { 1, 3, 0, 2, 4 };
        result = MatrixUtils.getRowPermutationIndexArray(matrix, preSelectedRows);
        Assert.assertArrayEquals(expected, result);

        // The preselected row index array contains invalid row indices
        preSelectedRows = new int[] { 1, 3, 0, -1 };
        try {
            MatrixUtils.getRowPermutationIndexArray(matrix, preSelectedRows);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = "row index (-1)";
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        preSelectedRows = new int[] { 1, 3, 0, 5 };
        try {
            MatrixUtils.getRowPermutationIndexArray(matrix, preSelectedRows);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = "row index (5)";
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the method that returns a column permutation index array.
     * 
     * <p>
     * Tested method:<br>
     * {@linkplain MatrixUtils#getColumnPermutationIndexArray(AnyMatrix, int[])}
     * </p>
     */
    @Test
    public void testGetColumnPermutationIndexArray() {
        int[] result;
        int[] expected;
        int[] preSelectedColumns;

        final AnyMatrix matrix = new Array2DRowRealMatrix(4, 5);

        // No preselected columns
        preSelectedColumns = new int[] {};
        expected = new int[] { 0, 1, 2, 3, 4 };
        result = MatrixUtils.getColumnPermutationIndexArray(matrix, preSelectedColumns);
        Assert.assertArrayEquals(expected, result);

        // The preselected columns are the first columns
        preSelectedColumns = new int[] { 0, 1, 2 };
        expected = new int[] { 0, 1, 2, 3, 4 };
        result = MatrixUtils.getColumnPermutationIndexArray(matrix, preSelectedColumns);
        Assert.assertArrayEquals(expected, result);

        // The preselected columns are the last columns
        preSelectedColumns = new int[] { 3, 4 };
        expected = new int[] { 3, 4, 0, 1, 2 };
        result = MatrixUtils.getColumnPermutationIndexArray(matrix, preSelectedColumns);
        Assert.assertArrayEquals(expected, result);

        // The preselected columns are not in increasing order
        preSelectedColumns = new int[] { 1, 3, 0 };
        expected = new int[] { 1, 3, 0, 2, 4 };
        result = MatrixUtils.getColumnPermutationIndexArray(matrix, preSelectedColumns);
        Assert.assertArrayEquals(expected, result);

        // The preselected column index array contains duplicates
        preSelectedColumns = new int[] { 1, 3, 0, 1 };
        expected = new int[] { 1, 3, 0, 2, 4 };
        result = MatrixUtils.getColumnPermutationIndexArray(matrix, preSelectedColumns);
        Assert.assertArrayEquals(expected, result);

        // The preselected column index array contains invalid column indices
        preSelectedColumns = new int[] { 1, 3, 0, -1 };
        try {
            MatrixUtils.getColumnPermutationIndexArray(matrix, preSelectedColumns);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = "column index (-1)";
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        preSelectedColumns = new int[] { 1, 3, 0, 5 };
        try {
            MatrixUtils.getColumnPermutationIndexArray(matrix, preSelectedColumns);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = "column index (5)";
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }
}
