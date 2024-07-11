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
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.linear;

import java.util.Locale;
import java.util.function.Function;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.MathUnsupportedOperationException;
import fr.cnes.sirius.patrius.math.exception.NoDataException;
import fr.cnes.sirius.patrius.math.exception.NotPositiveException;
import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.linear.ArrayRowSymmetricMatrix.SymmetryType;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Unit tests for {@link DecomposedSymmetricPositiveMatrix}.
 *
 * @author Pierre Seimandi (GMV)
 */
public class DecomposedSymmetricPositiveMatrixTest {

    /** Default absolute tolerance used for the tests. */
    private static final double ABSTOL = 0;

    /** Default relative tolerance used for the tests. */
    private static final double RELTOL = 1E-14;

    /** Expected message format for unsupported operation exceptions. */
    private static final String UNSUPPORTED_OPERATION_FORMAT = "unsupported operation";

    /**
     * Initialization.
     */
    @BeforeClass
    public static void init() {
        Locale.setDefault(Locale.ENGLISH);
    }

    /**
     * Tests creation of new instances when providing the dimension of the matrix.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#DecomposedSymmetricPositiveMatrix(int)}
     * </p>
     */
    @Test
    public void testConstructorDimension() {
        final DecomposedSymmetricPositiveMatrix matrix = new DecomposedSymmetricPositiveMatrix(3);
        final RealMatrix expected = new Array2DRowRealMatrix(3, 3);
        CheckUtils.checkEquality(expected, matrix, 0., 0.);
        Assert.assertEquals(3, matrix.getRowDimension());
        Assert.assertEquals(3, matrix.getColumnDimension());
        Assert.assertEquals(3, matrix.getTransparentDimension());
    }

    /**
     * Tests the creation of new instances when the specified dimension is not valid.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#DecomposedSymmetricPositiveMatrix(int)}
     * </p>
     */
    @Test
    public void testConstructorInvalidDimension() {
        try {
            new DecomposedSymmetricPositiveMatrix(0);
            Assert.fail();
        } catch (final NotStrictlyPositiveException e) {
            final String expectedMessage = "invalid row dimension: 0 (must be positive)";
            Assert.assertEquals(NotStrictlyPositiveException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests creation of new instances when providing the matrix BT.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#DecomposedSymmetricPositiveMatrix(double[][])}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#DecomposedSymmetricPositiveMatrix(double[][], boolean)}
     * <br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#DecomposedSymmetricPositiveMatrix(RealMatrix)}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#DecomposedSymmetricPositiveMatrix(RealMatrix, boolean)}
     * <br>
     * </p>
     */
    @Test
    public void testConstructorBT() {
        final double[][] dataBT = { { -2.71012, -6.90204, 0.04399, -3.06023 },
                { 0.73639, 8.54527, 5.09811, 1.03565 }, { 1.14986, -5.86624, -5.73656, 5.78988 },
                { 8.63897, 4.45998, 2.74064, -7.70595 }, { -3.08354, 0.65727, -7.34110, -0.95520 },
                { 8.54390, 7.66400, -4.35772, -6.03011 } };
        final RealMatrix matrixBT = new Array2DRowRealMatrix(dataBT, false);
        final RealMatrix expected = matrixBT.preMultiply(matrixBT.transpose());

        RealMatrix referenceBT;
        DecomposedSymmetricPositiveMatrix matrix;

        // From BT array
        matrix = new DecomposedSymmetricPositiveMatrix(dataBT);
        CheckUtils.checkEquality(expected, matrix, ABSTOL, RELTOL);
        Assert.assertEquals(4, matrix.getRowDimension());
        Assert.assertEquals(4, matrix.getColumnDimension());
        Assert.assertEquals(6, matrix.getTransparentDimension());
        referenceBT = matrix.getBT(false);
        Assert.assertNotSame(matrixBT, referenceBT);
        Assert.assertNotSame(dataBT, referenceBT.getData(false));

        // From BT array, force the copy of the data array
        matrix = new DecomposedSymmetricPositiveMatrix(dataBT, true);
        CheckUtils.checkEquality(expected, matrix, ABSTOL, RELTOL);
        Assert.assertEquals(4, matrix.getRowDimension());
        Assert.assertEquals(4, matrix.getColumnDimension());
        Assert.assertEquals(6, matrix.getTransparentDimension());
        referenceBT = matrix.getBT(false);
        Assert.assertNotSame(matrixBT, referenceBT);
        Assert.assertNotSame(dataBT, referenceBT.getData(false));

        // From BT array, do not force the copy of the data array
        matrix = new DecomposedSymmetricPositiveMatrix(dataBT, false);
        CheckUtils.checkEquality(expected, matrix, ABSTOL, RELTOL);
        Assert.assertEquals(4, matrix.getRowDimension());
        Assert.assertEquals(4, matrix.getColumnDimension());
        Assert.assertEquals(6, matrix.getTransparentDimension());
        referenceBT = matrix.getBT(false);
        Assert.assertNotSame(matrixBT, referenceBT);
        Assert.assertSame(dataBT, referenceBT.getData(false));

        // From BT matrix
        matrix = new DecomposedSymmetricPositiveMatrix(matrixBT);
        CheckUtils.checkEquality(expected, matrix, ABSTOL, RELTOL);
        Assert.assertEquals(4, matrix.getRowDimension());
        Assert.assertEquals(4, matrix.getColumnDimension());
        Assert.assertEquals(6, matrix.getTransparentDimension());
        referenceBT = matrix.getBT(false);
        Assert.assertNotSame(matrixBT, referenceBT);
        Assert.assertNotSame(dataBT, referenceBT.getData(false));

        // From BT matrix, force the copy of the matrix
        matrix = new DecomposedSymmetricPositiveMatrix(matrixBT, true);
        CheckUtils.checkEquality(expected, matrix, ABSTOL, RELTOL);
        Assert.assertEquals(4, matrix.getRowDimension());
        Assert.assertEquals(4, matrix.getColumnDimension());
        Assert.assertEquals(6, matrix.getTransparentDimension());
        referenceBT = matrix.getBT(false);
        Assert.assertNotSame(matrixBT, referenceBT);
        Assert.assertNotSame(dataBT, referenceBT.getData(false));

        // From BT matrix, do not force the copy of the matrix
        matrix = new DecomposedSymmetricPositiveMatrix(matrixBT, false);
        CheckUtils.checkEquality(expected, matrix, ABSTOL, RELTOL);
        Assert.assertEquals(4, matrix.getRowDimension());
        Assert.assertEquals(4, matrix.getColumnDimension());
        Assert.assertEquals(6, matrix.getTransparentDimension());
        referenceBT = matrix.getBT(false);
        Assert.assertSame(matrixBT, referenceBT);
        Assert.assertSame(dataBT, referenceBT.getData(false));
    }

    /**
     * Tests the creation of new instances when the provided data array is {@code null}.<br>
     * An exception should be thrown.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#DecomposedSymmetricPositiveMatrix(double[][])}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#DecomposedSymmetricPositiveMatrix(double[][], boolean)}
     * <br>
     * </p>
     */
    @Test
    @SuppressWarnings("unused")
    public void testConstructorNullDataArray() {
        final double[][] nullData = null;
        final String expectedMessage = "the supplied array is null";

        // Test every type of symmetry
        for (final SymmetryType symmetryType : SymmetryType.values()) {
            try {
                new DecomposedSymmetricPositiveMatrix(nullData);
                Assert.fail();
            } catch (final NullArgumentException e) {
                Assert.assertEquals(NullArgumentException.class, e.getClass());
                Assert.assertEquals(expectedMessage, e.getMessage());
            }

            try {
                new DecomposedSymmetricPositiveMatrix(nullData, true);
                Assert.fail();
            } catch (final NullArgumentException e) {
                Assert.assertEquals(NullArgumentException.class, e.getClass());
                Assert.assertEquals(expectedMessage, e.getMessage());
            }

            try {
                new DecomposedSymmetricPositiveMatrix(nullData, false);
                Assert.fail();
            } catch (final NullArgumentException e) {
                Assert.assertEquals(NullArgumentException.class, e.getClass());
                Assert.assertEquals(expectedMessage, e.getMessage());
            }
        }
    }

    /**
     * Tests the creation of new instances when the provided data array is empty.<br>
     * An exception should be thrown.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#DecomposedSymmetricPositiveMatrix(double[][])}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#DecomposedSymmetricPositiveMatrix(double[][], boolean)}
     * <br>
     * </p>
     */
    @Test
    public void testConstructorEmptyDataArray() {
        final double[][] emptyRowData = new double[0][0];
        final double[][] emptyColumnData = new double[1][0];
        final String expectedMessage1 = "matrix must have at least one row";
        final String expectedMessage2 = "matrix must have at least one column";

        // Zero rows and columns
        try {
            new DecomposedSymmetricPositiveMatrix(emptyRowData);
            Assert.fail();
        } catch (final NoDataException e) {
            Assert.assertEquals(NoDataException.class, e.getClass());
            Assert.assertEquals(expectedMessage1, e.getMessage());
        }

        try {
            new DecomposedSymmetricPositiveMatrix(emptyRowData, true);
            Assert.fail();
        } catch (final NoDataException e) {
            Assert.assertEquals(NoDataException.class, e.getClass());
            Assert.assertEquals(expectedMessage1, e.getMessage());
        }

        try {
            new DecomposedSymmetricPositiveMatrix(emptyRowData, false);
            Assert.fail();
        } catch (final NoDataException e) {
            Assert.assertEquals(NoDataException.class, e.getClass());
            Assert.assertEquals(expectedMessage1, e.getMessage());
        }

        // One empty row
        try {
            new DecomposedSymmetricPositiveMatrix(emptyColumnData);
            Assert.fail();
        } catch (final NoDataException e) {
            Assert.assertEquals(NoDataException.class, e.getClass());
            Assert.assertEquals(expectedMessage2, e.getMessage());
        }

        try {
            new DecomposedSymmetricPositiveMatrix(emptyColumnData, true);
            Assert.fail();
        } catch (final NoDataException e) {
            Assert.assertEquals(NoDataException.class, e.getClass());
            Assert.assertEquals(expectedMessage2, e.getMessage());
        }

        try {
            new DecomposedSymmetricPositiveMatrix(emptyColumnData, false);
            Assert.fail();
        } catch (final NoDataException e) {
            Assert.assertEquals(NoDataException.class, e.getClass());
            Assert.assertEquals(expectedMessage2, e.getMessage());
        }
    }

    /**
     * Tests the creation of new instances when the provided data array is not valid.<br>
     * An exception should be thrown.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#DecomposedSymmetricPositiveMatrix(double[][])}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#DecomposedSymmetricPositiveMatrix(double[][], boolean)}
     * <br>
     * </p>
     */
    @Test
    public void testConstructorInvalidDataArray() {
        final double[][] invalidData = { { 1. }, { 2., 3. } };
        final String expectedMessage = "2 != 1";

        // Two rows of different dimensions
        try {
            new DecomposedSymmetricPositiveMatrix(invalidData);
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            new DecomposedSymmetricPositiveMatrix(invalidData, true);
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            new DecomposedSymmetricPositiveMatrix(invalidData, false);
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the creation of new instances when the provided matrix is {@code null}.<br>
     * An exception should be thrown.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#DecomposedSymmetricPositiveMatrix(double[][])}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#DecomposedSymmetricPositiveMatrix(double[][], boolean)}
     * <br>
     * </p>
     */
    @Test
    public void testConstructorNullMatrix() {
        final RealMatrix nullMatrix = null;
        final String expectedMessage = "the supplied matrix is null";

        try {
            new DecomposedSymmetricPositiveMatrix(nullMatrix);
            Assert.fail();
        } catch (final NullArgumentException e) {
            Assert.assertEquals(NullArgumentException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            new DecomposedSymmetricPositiveMatrix(nullMatrix, true);
            Assert.fail();
        } catch (final NullArgumentException e) {
            Assert.assertEquals(NullArgumentException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            new DecomposedSymmetricPositiveMatrix(nullMatrix, false);
            Assert.fail();
        } catch (final NullArgumentException e) {
            Assert.assertEquals(NullArgumentException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the creation of new matrices of the same type by specifying the row and column
     * dimensions.<br>
     * An exception should be thrown when the specified row and columns dimensions are not equal.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#createMatrix(int, int)}
     * </p>
     */
    @Test
    public void testCreateMatrix() {
        final int dim = 4;
        final DecomposedSymmetricPositiveMatrix matrix = new DecomposedSymmetricPositiveMatrix(dim);
        final DecomposedSymmetricPositiveMatrix result = matrix.createMatrix(dim, dim);

        // Standard case
        final RealMatrix expected = new Array2DRowRealMatrix(dim, dim);
        CheckUtils.checkEquality(expected, result, 0., 0.);
        Assert.assertNotSame(matrix, result);

        // Incompatible row/column dimensions
        try {
            matrix.createMatrix(3, 2);
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals("2 != 3", e.getMessage());
        }
    }

    /**
     * Tests the method:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#createIdentityMatrix(int)}
     */
    @Test
    public void testCreateIdentityMatrix() {
        for (int dim = 1; dim <= 10; dim++) {
            final DecomposedSymmetricPositiveMatrix matrix = DecomposedSymmetricPositiveMatrix
                    .createIdentityMatrix(dim);
            final RealMatrix expected = MatrixUtils.createRealIdentityMatrix(dim);
            CheckUtils.checkEquality(expected, matrix, ABSTOL, RELTOL);
        }

        // Invalid dimension
        try {
            DecomposedSymmetricPositiveMatrix.createIdentityMatrix(0);
            Assert.fail();
        } catch (final NotStrictlyPositiveException e) {
            final String expectedMessage = "invalid row dimension: 0 (must be positive)";
            Assert.assertEquals(NotStrictlyPositiveException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the method that converts the matrix into an {@linkplain ArrayRowSymmetricMatrix}.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#toArrayRowSymmetricMatrix()}
     * </p>
     */
    @Test
    public void testToArrayRowSymmetricMatrix() {
        ArrayRowSymmetricMatrix result;
        // Tested matrix
        final double[][] dataBT = { { 5.58797, 7.56392, -9.63453, -5.02596 },
                { -0.08082, 3.50447, -8.35243, 7.23397 }, { -6.76034, 6.78911, 9.55031, 4.82303 },
                { 2.46717, 9.72957, 8.76649, 3.67761 }, { 1.49852, -6.31749, -3.29706, -0.88557 },
                { -0.79214, 5.75592, -6.52875, 4.34848 } };
        final DecomposedSymmetricPositiveMatrix matrix = new DecomposedSymmetricPositiveMatrix(
                dataBT);

        // Test the method:
        // The internal data array storing the entries is not initialized yet)
        result = matrix.toArrayRowSymmetricMatrix();
        final double[][] data = matrix.getData();
        final RealMatrix expected = new Array2DRowRealMatrix(data);
        CheckUtils.checkEquality(expected, result, 0., 0.);
        Assert.assertEquals(ArrayRowSymmetricMatrix.class, result.getClass());

        // The internal data array storing the entries is fully initialized
        result = matrix.toArrayRowSymmetricMatrix();
        CheckUtils.checkEquality(expected, result, 0., 0.);
        Assert.assertEquals(ArrayRowSymmetricMatrix.class, result.getClass());
    }

    /**
     * Tests the method that converts the matrix into an
     * {@linkplain ArrayRowSymmetricPositiveMatrix}.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#toArrayRowSymmetricPositiveMatrix()}
     * </p>
     */
    @Test
    public void testToArrayRowSymmetricPositiveMatrix() {
        // Tested matrix
        final double[][] dataBT = { { -9.55089, 6.99823, -9.72817, -9.07135 },
                { -7.22521, 5.88203, -7.68551, -6.86206 },
                { -8.08830, -9.02966, -2.39541, 2.94541 }, { 6.05289, 4.24324, 1.09127, -2.75720 },
                { 0.16640, 3.80027, -7.41603, 3.06510 }, { 7.27222, 1.78336, -5.48923, 2.51027 } };
        final DecomposedSymmetricPositiveMatrix matrix = new DecomposedSymmetricPositiveMatrix(
                dataBT);

        // Test the method
        final double[][] data = matrix.getData();
        final RealMatrix expected = new Array2DRowRealMatrix(data);
        final ArrayRowSymmetricPositiveMatrix result = matrix.toArrayRowSymmetricPositiveMatrix();
        CheckUtils.checkEquality(expected, result, 0., 0.);
        Assert.assertEquals(ArrayRowSymmetricPositiveMatrix.class, result.getClass());
    }

    /**
     * Tests the method checking if a matrix is square.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#isSquare()}
     * </p>
     */
    @Test
    public void testIsSquare() {
        SymmetricPositiveMatrix matrix;

        // Matrix filled with zeros
        matrix = new DecomposedSymmetricPositiveMatrix(8);
        Assert.assertTrue(matrix.isSquare());

        final double[][] dataBT = { { -3.94327, 0.17546, -7.02871, -4.22408, 7.57170, 2.91326 },
                { 8.84969, 9.46736, 6.05862, -7.43124, -7.63701, 7.20221 },
                { -5.05831, -5.81761, -2.36833, 2.48114, 3.43457, -8.10824 },
                { 2.84251, -5.67470, -1.24499, -8.32607, -5.45034, -8.71479 } };
        matrix = new DecomposedSymmetricPositiveMatrix(dataBT);
        Assert.assertTrue(matrix.isSquare());
    }

    /**
     * Tests the methods checking if a matrix is diagonal.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#isDiagonal(double)}<br>
     * </p>
     */
    @Test
    public void testIsDiagonal() {
        SymmetricPositiveMatrix matrix;

        // Matrix filled with zeros
        matrix = new DecomposedSymmetricPositiveMatrix(8);
        Assert.assertTrue(matrix.isDiagonal(0.));

        // Diagonal matrix, except for one off-diagonal element
        final double[][] dataBT = { { 2, 0., 0. }, { -1E-2, 2, 0. }, { 0, -1E-2, 5 } };
        matrix = new DecomposedSymmetricPositiveMatrix(dataBT);
        final double diagonalityThreshold = 5E-2;
        Assert.assertFalse(matrix.isDiagonal(0.));
        Assert.assertFalse(matrix.isDiagonal(diagonalityThreshold
                - MathLib.ulp(diagonalityThreshold)));
        Assert.assertTrue(matrix.isDiagonal(diagonalityThreshold));
    }

    /**
     * Tests the methods checking if a matrix is symmetric.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#isSymmetric()}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#isSymmetric(double)}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#isSymmetric(double, double)}<br>
     * </p>
     */
    @Test
    public void testIsSymmetric() {
        SymmetricPositiveMatrix matrix;
        final double infinity = Double.POSITIVE_INFINITY;

        // Matrix filled with zeros
        matrix = new DecomposedSymmetricPositiveMatrix(8);
        Assert.assertTrue(matrix.isSymmetric());
        Assert.assertTrue(matrix.isSymmetric(0.));
        Assert.assertTrue(matrix.isSymmetric(0., 0.));
        Assert.assertTrue(matrix.isSymmetric(-infinity));
        Assert.assertTrue(matrix.isSymmetric(-infinity, -infinity));

        // Standard matrix
        final double[][] dataBT = { { -3.04919, -0.45814, -6.47787, 7.18625 },
                { 5.84106, -0.16060, 4.18894, -6.96247 },
                { -2.06744, -2.36988, -2.98077, -8.01804 },
                { -1.56565, 9.36637, 5.33858, -9.08941 }, { -1.87519, -3.37667, 9.11082, 9.98159 },
                { -0.44695, -4.72292, 5.01566, -8.36696 } };
        matrix = new DecomposedSymmetricPositiveMatrix(dataBT);
        Assert.assertTrue(matrix.isSymmetric());
        Assert.assertTrue(matrix.isSymmetric(0.));
        Assert.assertTrue(matrix.isSymmetric(0., 0.));
        Assert.assertTrue(matrix.isSymmetric(-infinity));
        Assert.assertTrue(matrix.isSymmetric(-infinity, -infinity));
    }

    /**
     * Tests the methods checking if a matrix is antisymmetric.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#isAntisymmetric(double, double)}<br>
     * </p>
     */
    @Test
    public void testIsAntisymmetric() {
        double[][] dataBT;
        SymmetricPositiveMatrix matrix;

        // Matrix filled with zeros
        matrix = new DecomposedSymmetricPositiveMatrix(8);
        Assert.assertTrue(matrix.isAntisymmetric(0., 0.));

        // Standard matrix
        dataBT = new double[][] { { 2, 0., 0. }, { -1, 2, 0. }, { 0, -1, 5 } };
        matrix = new DecomposedSymmetricPositiveMatrix(dataBT);
        Assert.assertFalse(matrix.isAntisymmetric(0., 0.));

        // The off-diagonal elements are equal to zero, but not the diagonal elements
        dataBT = new double[][] { { 1E-5, 0., 0. }, { 0., 1E-4, 0. }, { 0., 0., 1E-3 } };
        matrix = new DecomposedSymmetricPositiveMatrix(dataBT);
        final double threshold = 1E-6;
        Assert.assertFalse(matrix.isAntisymmetric(0., 0.));
        Assert.assertFalse(matrix.isAntisymmetric(0., threshold - MathLib.ulp(threshold)));
        Assert.assertTrue(matrix.isAntisymmetric(0., threshold));
        Assert.assertFalse(matrix.isAntisymmetric(threshold, 0.));
    }

    /**
     * Tests the methods checking if a matrix is orthogonal.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#isOrthogonal(double, double)}<br>
     * </p>
     */
    @Test
    public void testIsOrthogonal() {
        double[][] data;
        SymmetricPositiveMatrix matrix;
        final double infinity = Double.POSITIVE_INFINITY;

        // Matrix filled with zeros
        matrix = new DecomposedSymmetricPositiveMatrix(8);
        Assert.assertFalse(matrix.isOrthogonal(0., 0.));
        Assert.assertTrue(matrix.isOrthogonal(infinity, infinity));

        // Perfectly orthogonal matrix
        // (only the identity matrix can be orthogonal and positive definite)
        final SymmetricPositiveMatrix orthogonalMatrix = DecomposedSymmetricPositiveMatrix
                .createIdentityMatrix(4);
        Assert.assertTrue(orthogonalMatrix.isOrthogonal(0., 0.));

        // The norm of the first row is slightly different from 1
        data = orthogonalMatrix.getData();
        data[0][0] += 1E-4;
        matrix = new DecomposedSymmetricPositiveMatrix(data);

        final double norm = matrix.getRowVector(0).getNorm();
        final double normThreshold = MathLib.abs(MathLib.divide(1. - norm, MathLib.max(1., norm)));

        Assert.assertFalse(matrix.isOrthogonal(0., 0.));
        Assert.assertFalse(matrix.isOrthogonal(normThreshold - MathLib.ulp(normThreshold), 0.));
        Assert.assertTrue(matrix.isOrthogonal(normThreshold, 0.));

        // Some of the rows are not perfectly orthogonal
        data = orthogonalMatrix.getData();
        data[1][1] += 1E-8;
        data[2][1] += 2E-7;
        data[2][2] += 4E-9;
        data[3][1] += 3E-8;
        data[3][2] += 8E-7;
        data[3][3] += 8E-9;
        matrix = new DecomposedSymmetricPositiveMatrix(data);
        final RealVector column1 = matrix.getColumnVector(1);
        final RealVector column2 = matrix.getColumnVector(2);
        final RealVector column3 = matrix.getColumnVector(3);

        double orthogonalityThreshold = 0.;
        orthogonalityThreshold = MathLib.max(orthogonalityThreshold, column1.dotProduct(column2));
        orthogonalityThreshold = MathLib.max(orthogonalityThreshold, column1.dotProduct(column3));
        orthogonalityThreshold = MathLib.max(orthogonalityThreshold, column2.dotProduct(column3));

        Assert.assertFalse(matrix.isOrthogonal(0., 0.));
        Assert.assertFalse(matrix.isOrthogonal(0., infinity));
        Assert.assertFalse(matrix.isOrthogonal(infinity, 0.));
        Assert.assertFalse(matrix.isOrthogonal(infinity,
                orthogonalityThreshold - MathLib.ulp(orthogonalityThreshold)));
        Assert.assertTrue(matrix.isOrthogonal(infinity, orthogonalityThreshold));
    }

    /**
     * Tests the method that returns the minimum value in the matrix.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#getMin()}
     * </p>
     */
    @Test
    public void testGetMin() {
        // Tested matrix
        final double[][] dataBT = { { -3.99175, 9.06658, -6.24977, -0.37046, 3.26277, 6.00224 },
                { 8.63399, 8.80960, -8.36591, -6.89850, 6.29763, 0.05222 },
                { -7.74340, 8.55730, 9.39653, -4.55794, 3.41658, -1.56481 },
                { -3.64578, -9.64676, -3.78682, 7.58164, -4.48516, 1.90669 } };
        final SymmetricPositiveMatrix matrix = new DecomposedSymmetricPositiveMatrix(dataBT);

        // Test the method
        CheckUtils.checkMinimum(matrix, 0., 0.);
        Assert.assertEquals(-176.27375227520002, matrix.getMin(), 0.);
    }

    /**
     * Tests the method that returns the maximum value in the matrix.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#getMax()}
     * </p>
     */
    @Test
    public void testGetMax() {
        // Tested matrix
        final double[][] dataBT = { { -8.54328, -4.92711, 8.13983, -0.23702, -1.63036, -4.27022 },
                { -0.94437, 8.79771, -5.90382, -0.70169, 7.32890, -6.53443 },
                { -0.55474, 1.61105, -5.42535, 2.93625, 8.59674, -0.86242 },
                { 0.01894, 2.21151, -3.70827, -5.01934, 6.05862, 1.17778 } };
        final SymmetricPositiveMatrix matrix = new DecomposedSymmetricPositiveMatrix(dataBT);

        // Test the method
        CheckUtils.checkMaximum(matrix, 0., 0.);
        Assert.assertEquals(166.9816638716, matrix.getMax(), 0.);
    }

    /**
     * Tests the method that computes the trace of the matrix.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#getTrace()}
     * </p>
     */
    @Test
    public void testGetTrace() {
        // Tested matrix
        final double[][] dataBT = { { -2.41085, -5.80989, -0.21051, 5.61953, 4.57426, -3.69889 },
                { 1.00278, 2.26042, -1.88788, 8.74422, 8.85665, -4.50717 },
                { -2.80315, -4.85162, -6.61877, -5.87744, -5.30905, -5.07255 },
                { 6.92641, 0.65053, -2.99869, -4.29310, 9.55229, 7.73173 } };
        final SymmetricPositiveMatrix positiveMatrix = new DecomposedSymmetricPositiveMatrix(dataBT);

        // Test the method
        CheckUtils.checkTrace(positiveMatrix, 0., 0.);
        CheckUtils.checkEquality(681.2036107848, positiveMatrix.getTrace(), ABSTOL, RELTOL);
    }

    /**
     * Tests the method that computes the norm of the matrix (maximum absolute row sum).
     *
     * <p>
     * Tested method:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#getNorm()}
     * </p>
     */
    @Test
    public void testGetNorm() {
        // Tested matrix
        final double[][] dataBT = { { -8.09049, -2.65157, 3.32195, -8.17397, 8.46111, 8.24185 },
                { 7.80167, 3.95492, 5.30763, 2.26923, 4.30853, 4.10952 },
                { -1.34241, -7.04308, 3.26075, 7.36370, 2.19058, 2.80422 },
                { -1.73851, -4.78692, 2.76735, -2.95991, -4.65181, 0.10587 } };
        final SymmetricPositiveMatrix matrix = new DecomposedSymmetricPositiveMatrix(dataBT);

        // Test the method
        CheckUtils.checkNorm(matrix, ABSTOL, RELTOL);
        CheckUtils.checkEquality(353.9328256875, matrix.getNorm(), ABSTOL, RELTOL);
    }

    /**
     * Tests the method that computes the Frobenius norm of the matrix.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#getFrobeniusNorm()}
     * </p>
     */
    @Test
    public void testGetFrobeniusNorm() {
        // Tested matrix
        final double[][] dataBT = { { 6.06546, -3.02660, 6.42113, 0.51216, -6.39389, 6.96960 },
                { 4.66747, 0.08870, 9.92399, -2.35477, 6.45053, -9.63171 },
                { -1.53419, 6.29673, 0.45385, -9.29306, 4.46946, -7.51393 },
                { 9.86025, 1.66353, 9.54799, 4.14175, -1.80119, 4.21226 } };
        final SymmetricPositiveMatrix matrix = new DecomposedSymmetricPositiveMatrix(dataBT);

        // Test the method
        CheckUtils.checkFrobeniusNorm(matrix, ABSTOL, RELTOL);
        CheckUtils.checkEquality(568.8930284502438, matrix.getFrobeniusNorm(), ABSTOL, RELTOL);
    }

    /**
     * Tests the method that returns the entries of the matrix in a 2D data array.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#getData()}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#getData(boolean)}<br>
     * </p>
     */
    @Test
    public void testGetData() {
        double[][] result;

        // Tested matrix
        final double[][] dataBT = { { 6.54319, 2.42542, -3.82092, -6.59115 },
                { -0.98490, 4.79951, -9.94311, -1.00044 },
                { 3.81898, -8.40472, -7.09938, 2.38443 }, { 1.44428, 3.21765, 6.80290, 9.72901 },
                { 9.62869, -5.09047, -4.35106, 5.07971 }, { -1.54683, 0.58146, -5.53790, -7.44268 } };
        final SymmetricPositiveMatrix matrix = new DecomposedSymmetricPositiveMatrix(dataBT);

        // Expected data
        final double[][] expected = {
                { 155.5582705099, -66.22130090889999, -65.8239523706, 41.43920521600001 },
                { -66.22130090889999, 136.16152876989997, 43.4971336603, -39.70957837699999 },
                { -65.8239523706, 43.4971336603, 259.7455704465, 103.5039438814 },
                { 41.43920521600001, -39.70957837699999, 103.5039438814, 225.9802197876 } };

        // Test the methods
        result = matrix.getData();
        CheckUtils.checkEquality(expected, result, ABSTOL, RELTOL);

        result = matrix.getData(true);
        CheckUtils.checkEquality(expected, result, ABSTOL, RELTOL);

        result = matrix.getData(false);
        CheckUtils.checkEquality(expected, result, ABSTOL, RELTOL);
    }

    /**
     * Tests the method that returns the matrix B.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#getB()}<br>
     * </p>
     */
    @Test
    public void testGetB() {
        // Tested matrix
        // (a block real matrix is used to make sure B^T is passed by reference)
        final double[][] dataBT = { { 9.21045, -0.89726, -7.71661, -4.41185 },
                { 1.23091, -6.87042, -7.07992, 0.32833 }, { 7.99765, 8.08556, 2.59561, 3.64954 },
                { 8.01255, -8.92592, -0.08840, -2.25020 },
                { -7.99688, 6.63646, 0.88857, -2.93678 }, { -6.88829, 4.53173, 6.85528, 6.53893 } };
        final RealMatrix matrixBT = new BlockRealMatrix(dataBT);
        final DecomposedSymmetricPositiveMatrix matrix = new DecomposedSymmetricPositiveMatrix(
                matrixBT, false);

        // Test the method
        final RealMatrix result = matrix.getB();
        final RealMatrix expected = matrixBT.transpose();
        CheckUtils.checkEquality(expected, result, 0., 0.);
        Assert.assertEquals(expected.getClass(), result.getClass());
    }

    /**
     * Tests the method that returns the matrix BT.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#getBT()}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#getBT(boolean)}<br>
     * </p>
     */
    @Test
    public void testGetBT() {
        RealMatrix result;

        // Tested matrix
        // (a block real matrix is used to make sure B^T is passed by reference)
        final double[][] dataBT = { { 7.23743, 0.60184, 0.35444, -0.07380 },
                { 0.60184, 2.09240, 0.99668, -1.99425 }, { 0.35444, 0.99668, 6.35915, 0.81168 },
                { -0.07380, -1.99425, 0.81168, 6.74083 } };
        final RealMatrix matrixBT = new BlockRealMatrix(dataBT);
        final DecomposedSymmetricPositiveMatrix matrix = new DecomposedSymmetricPositiveMatrix(
                matrixBT, false);

        // Test the methods
        result = matrix.getBT();
        CheckUtils.checkEquality(matrixBT, result, 0., 0.);
        Assert.assertNotSame(matrixBT, result);
        Assert.assertEquals(matrixBT.getClass(), result.getClass());

        result = matrix.getBT(true);
        CheckUtils.checkEquality(matrixBT, result, 0., 0.);
        Assert.assertNotSame(matrixBT, result);
        Assert.assertEquals(matrixBT.getClass(), result.getClass());

        result = matrix.getBT(false);
        CheckUtils.checkEquality(matrixBT, result, 0., 0.);
        Assert.assertSame(matrixBT, result);
        Assert.assertEquals(matrixBT.getClass(), result.getClass());
    }

    /**
     * Tests the method that performs a deep copy of the matrix.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#copy()}
     * </p>
     */
    @Test
    public void testCopy() {
        // Tested matrix
        final double[][] dataBT = { { -9.49523, -1.26183, -9.41262, 0.39354, -7.19079, 7.63638 },
                { -9.03123, 9.58549, -2.11515, 6.22861, 6.12252, -7.02527 },
                { 0.11295, 4.27432, 4.05695, 7.85931, 0.33850, 7.09086 },
                { -1.09595, -8.36135, -2.39763, -1.21444, -4.55331, 5.87837 } };
        final DecomposedSymmetricPositiveMatrix matrix = new DecomposedSymmetricPositiveMatrix(
                dataBT);

        // Test the method
        final DecomposedSymmetricPositiveMatrix result = matrix.copy();
        CheckUtils.checkEquality(matrix, result, 0., 0.);
        Assert.assertNotSame(matrix, result);
        Assert.assertNotSame(matrix.getBT(false), result.getBT(false));
        Assert.assertEquals(DecomposedSymmetricPositiveMatrix.class, result.getClass());
    }

    /**
     * Tests the methods that return the transpose of the matrix.<br>
     * The transpose matrix should be the same as the initial matrix, stored in a new instance
     * unless specified otherwise.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#transpose()}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#transpose(boolean)}<br>
     * </p>
     */
    @Test
    public void testTranspose() {
        // Tested matrix
        final double[][] dataBT = { { -0.76637, -8.08018, -5.56758, 8.15423 },
                { -3.16986, 9.83623, 9.61957, 4.29522 }, { -9.87402, 0.80271, 1.38376, -1.38312 },
                { 0.77305, 7.90462, -6.63788, 3.68477 }, { 6.34004, -6.78825, -2.76651, 9.98939 },
                { -1.68118, 6.55856, 0.69306, 8.34110 } };
        final SymmetricPositiveMatrix matrix = new DecomposedSymmetricPositiveMatrix(dataBT);

        // Test the method
        CheckUtils.checkTranspose(matrix);
    }

    /**
     * Tests the method that raises the matrix to the power of N.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#power(int)}<br>
     * </p>
     */
    @Test
    public void testPower() {
        // Tested matrix
        final double[][] dataBT = { { -5.94344, 9.51095, -9.98057, 8.84888, -5.16745, -5.31733 },
                { -2.96758, -3.64003, 4.61430, -1.23013, 2.61749, 1.34410 },
                { 2.43970, -9.93620, -2.24830, -6.98869, 4.31957, 6.38072 },
                { 4.45948, -1.18136, -4.11057, -2.53987, 6.96688, -3.54988 } };
        final SymmetricPositiveMatrix matrix = new DecomposedSymmetricPositiveMatrix(dataBT);

        // Test the method
        CheckUtils.checkPower(matrix, 0, 10, DecomposedSymmetricPositiveMatrix.class, ABSTOL,
                RELTOL);
        CheckUtils.checkPowerNegativeExponent(matrix);
    }

    /**
     * Tests the method that returns an element of the matrix.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#getEntry(int, int)}
     * </p>
     */
    @Test
    public void testGetEntry() {
        // Tested matrix
        final double[][] dataBT = { { 6.54319, 2.42542, -3.82092, -6.59115 },
                { -0.98490, 4.79951, -9.94311, -1.00044 },
                { 3.81898, -8.40472, -7.09938, 2.38443 }, { 1.44428, 3.21765, 6.80290, 9.72901 },
                { 9.62869, -5.09047, -4.35106, 5.07971 }, { -1.54683, 0.58146, -5.53790, -7.44268 } };
        final SymmetricPositiveMatrix matrix = new DecomposedSymmetricPositiveMatrix(dataBT);

        // Test the method
        CheckUtils.checkGetEntry(matrix);
    }

    /**
     * Tests the method that returns an element of the matrix, using indices outside the valid index
     * range.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#getEntry(int, int)}
     * </p>
     */
    @Test
    public void testGetOutOfRangeEntry() {
        final SymmetricPositiveMatrix matrix = new DecomposedSymmetricPositiveMatrix(3);
        CheckUtils.checkGetOutOfRangeEntry(matrix);
    }

    /**
     * Tests the method that sets an entry to a new value.<br>
     * This method should throw an exception since it is forbidden.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#setEntry(int, int, double)}
     * </p>
     */
    @Test
    public void testSetEntry() {
        final int dim = 4;
        final SymmetricPositiveMatrix matrix = new DecomposedSymmetricPositiveMatrix(dim);

        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < dim; j++) {
                try {
                    matrix.setEntry(i, j, 2.);
                    Assert.fail();
                } catch (final MathUnsupportedOperationException e) {
                    final String expectedMessage = String.format(UNSUPPORTED_OPERATION_FORMAT);
                    Assert.assertEquals(MathUnsupportedOperationException.class, e.getClass());
                    Assert.assertEquals(expectedMessage, e.getMessage());
                }
            }
        }
    }

    /**
     * Tests the method that sets an entry to a new value, using indices outside the valid index
     * range.<br>
     * An exception should be thrown.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#setEntry(int, int, double)}
     * </p>
     */
    @Test
    public void testSetOutOfRangeEntry() {
        final int dim = 4;
        final SymmetricPositiveMatrix matrix = new DecomposedSymmetricPositiveMatrix(dim);

        // Ensure an exception is throw if the row index is out of range
        try {
            matrix.setEntry(-1, dim - 1, 0.);
            Assert.fail();
        } catch (final MathUnsupportedOperationException e) {
            final String expectedMessage = String.format(UNSUPPORTED_OPERATION_FORMAT);
            Assert.assertEquals(MathUnsupportedOperationException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.setEntry(dim, dim - 1, 0.);
            Assert.fail();
        } catch (final MathUnsupportedOperationException e) {
            final String expectedMessage = String.format(UNSUPPORTED_OPERATION_FORMAT);
            Assert.assertEquals(MathUnsupportedOperationException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // Ensure an exception is throw if the column index is out of range
        try {
            matrix.setEntry(dim - 1, -1, 0.);
            Assert.fail();
        } catch (final MathUnsupportedOperationException e) {
            final String expectedMessage = String.format(UNSUPPORTED_OPERATION_FORMAT);
            Assert.assertEquals(MathUnsupportedOperationException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.setEntry(dim - 1, dim, 0.);
            Assert.fail();
        } catch (final MathUnsupportedOperationException e) {
            final String expectedMessage = String.format(UNSUPPORTED_OPERATION_FORMAT);
            Assert.assertEquals(MathUnsupportedOperationException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the method that adds a scalar to an entry.<br>
     * This method should throw an exception since it is forbidden.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#addToEntry(int, int, double)} <>/p
     */
    @Test
    public void testAddToEntry() {
        final int dim = 4;
        final SymmetricPositiveMatrix matrix = new DecomposedSymmetricPositiveMatrix(dim);

        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < dim; j++) {
                try {
                    matrix.addToEntry(i, j, 1.0);
                    Assert.fail();
                } catch (final MathUnsupportedOperationException e) {
                    final String expectedMessage = String.format(UNSUPPORTED_OPERATION_FORMAT);
                    Assert.assertEquals(MathUnsupportedOperationException.class, e.getClass());
                    Assert.assertEquals(expectedMessage, e.getMessage());
                }
            }
        }
    }

    /**
     * Tests the method that adds a scalar to an entry, using indices outside the valid index range.<br>
     * An exception should be thrown.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#addToEntry(int, int, double)}
     * </p>
     */
    @Test
    public void testAddToOutOfRangeEntry() {
        final int dim = 4;
        final SymmetricPositiveMatrix matrix = new DecomposedSymmetricPositiveMatrix(dim);

        // Ensure an exception is throw if the row index is out of range
        try {
            matrix.addToEntry(-1, dim - 1, 0.);
            Assert.fail();
        } catch (final MathUnsupportedOperationException e) {
            final String expectedMessage = String.format(UNSUPPORTED_OPERATION_FORMAT);
            Assert.assertEquals(MathUnsupportedOperationException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.addToEntry(dim, dim - 1, 0.);
            Assert.fail();
        } catch (final MathUnsupportedOperationException e) {
            final String expectedMessage = String.format(UNSUPPORTED_OPERATION_FORMAT);
            Assert.assertEquals(MathUnsupportedOperationException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // Ensure an exception is throw if the column index is out of range
        try {
            matrix.addToEntry(dim - 1, -1, 0.);
            Assert.fail();
        } catch (final MathUnsupportedOperationException e) {
            final String expectedMessage = String.format(UNSUPPORTED_OPERATION_FORMAT);
            Assert.assertEquals(MathUnsupportedOperationException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.addToEntry(dim - 1, dim, 0.);
            Assert.fail();
        } catch (final MathUnsupportedOperationException e) {
            final String expectedMessage = String.format(UNSUPPORTED_OPERATION_FORMAT);
            Assert.assertEquals(MathUnsupportedOperationException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the method that multiplies an entry by a scalar.<br>
     * This method should throw an exception since it is forbidden.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#multiplyEntry(int, int, double)} <>/p
     */
    @Test
    public void testMultiplyEntry() {
        final int dim = 4;
        final SymmetricPositiveMatrix matrix = new DecomposedSymmetricPositiveMatrix(dim);

        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < dim; j++) {
                try {
                    matrix.multiplyEntry(i, j, 2.);
                    Assert.fail();
                } catch (final MathUnsupportedOperationException e) {
                    final String expectedMessage = String.format(UNSUPPORTED_OPERATION_FORMAT);
                    Assert.assertEquals(MathUnsupportedOperationException.class, e.getClass());
                    Assert.assertEquals(expectedMessage, e.getMessage());
                }
            }
        }
    }

    /**
     * Tests the method that multiplies an entry by a scalar, using indices outside the valid index
     * range.<br>
     * An exception should be thrown.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#multiplyEntry(int, int, double)}
     * </p>
     */
    @Test
    public void testMultiplyOutOfRangeEntry() {
        final int dim = 4;
        final SymmetricPositiveMatrix matrix = new DecomposedSymmetricPositiveMatrix(4);

        // Ensure an exception is throw if the row index is out of range
        try {
            matrix.multiplyEntry(-1, dim - 1, 0.);
            Assert.fail();
        } catch (final MathUnsupportedOperationException e) {
            final String expectedMessage = String.format(UNSUPPORTED_OPERATION_FORMAT);
            Assert.assertEquals(MathUnsupportedOperationException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.multiplyEntry(dim, dim - 1, 0.);
            Assert.fail();
        } catch (final MathUnsupportedOperationException e) {
            final String expectedMessage = String.format(UNSUPPORTED_OPERATION_FORMAT);
            Assert.assertEquals(MathUnsupportedOperationException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // Ensure an exception is throw if the column index is out of range
        try {
            matrix.multiplyEntry(dim - 1, -1, 0.);
            Assert.fail();
        } catch (final MathUnsupportedOperationException e) {
            final String expectedMessage = String.format(UNSUPPORTED_OPERATION_FORMAT);
            Assert.assertEquals(MathUnsupportedOperationException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.multiplyEntry(dim - 1, dim, 0.);
            Assert.fail();
        } catch (final MathUnsupportedOperationException e) {
            final String expectedMessage = String.format(UNSUPPORTED_OPERATION_FORMAT);
            Assert.assertEquals(MathUnsupportedOperationException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the method that adds a scalar to the entries of the matrix.<br>
     * The resulting matrix should still be an instance of
     * {@linkplain DecomposedSymmetricPositiveMatrix} when the scalar is positive.
     * Otherwise, it should be an instance of {@linkplain ArrayRowSymmetricMatrix}.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#scalarAdd(double)}
     * </p>
     */
    @Test
    public void testScalarAdd() {
        // Tested matrix
        final double[][] dataBT = { { -7.08702, 1.66466, -0.64348, 9.80273 },
                { 6.26924, 1.47413, -3.67088, 8.26309 }, { 7.17518, 9.05458, -1.80849, -4.22925 },
                { 8.88202, -4.95424, 3.68824, -7.93002 },
                { -2.18303, -8.93294, -0.89970, 0.69662 }, { -5.23282, 6.91375, -8.06654, 0.09602 } };
        final SymmetricPositiveMatrix matrix = new DecomposedSymmetricPositiveMatrix(dataBT);

        // Test the method
        CheckUtils.checkScalarAdd(matrix, 0., DecomposedSymmetricPositiveMatrix.class, ABSTOL,
                RELTOL);
        CheckUtils.checkScalarAdd(matrix, +1.718, DecomposedSymmetricPositiveMatrix.class, ABSTOL,
                RELTOL);
        CheckUtils.checkScalarAdd(matrix, -7.274, ArrayRowSymmetricMatrix.class, ABSTOL, RELTOL);
    }

    /**
     * Tests the method that multiplies the matrix by a scalar.<br>
     * The resulting matrix should still be an instance of
     * {@linkplain DecomposedSymmetricPositiveMatrix} when the scalar is positive.
     * Otherwise, it should be an instance of {@linkplain ArrayRowSymmetricMatrix}.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#scalarMultiply(double)}
     * </p>
     */
    @Test
    public void testScalarMultiply() {
        // Tested matrix
        final double[][] dataBT = { { 2.86543, -3.09971, -3.54399, 6.93908 },
                { -6.33600, 3.23334, 2.10719, 8.91892 }, { 4.99363, -8.35248, 9.03232, 2.32601 },
                { -5.60208, -4.50228, -7.99457, -9.98481 },
                { -2.42128, 6.28596, -6.07644, 9.42417 }, { 4.91085, -7.80969, -2.81173, 9.65846 } };
        final SymmetricPositiveMatrix matrix = new DecomposedSymmetricPositiveMatrix(dataBT);

        // Test the method
        CheckUtils.checkScalarMultiply(matrix, 0.0, DecomposedSymmetricPositiveMatrix.class,
                ABSTOL, RELTOL);
        CheckUtils.checkScalarMultiply(matrix, 1.0, DecomposedSymmetricPositiveMatrix.class,
                ABSTOL, RELTOL);
        CheckUtils.checkScalarMultiply(matrix, +2.838, DecomposedSymmetricPositiveMatrix.class,
                ABSTOL, RELTOL);
        CheckUtils.checkScalarMultiply(matrix, -4.278, ArrayRowSymmetricMatrix.class, ABSTOL,
                RELTOL);
    }

    /**
     * Tests the method that adds a positive scalar to the entries of the matrix.<br>
     * An exception should be thrown if the provided scalar is negative.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#positiveScalarAdd(double)}
     * </p>
     */
    @Test
    public void testPositiveScalarAdd() {
        // Tested matrix
        final double[][] dataBT = { { 4.05312, -6.93443, -8.74417, 9.55189 },
                { -6.27584, -4.07642, -2.13973, -7.53473 },
                { -0.76160, -6.17546, -8.12048, 4.40377 },
                { -0.96114, -7.57949, 7.98333, -6.35512 },
                { 7.69108, 1.63957, -0.23646, -4.37926 }, { -6.22267, 3.17154, 4.88087, -9.62948 } };
        final SymmetricPositiveMatrix matrix = new DecomposedSymmetricPositiveMatrix(dataBT);

        // Test the method
        CheckUtils.checkPositiveScalarAdd(matrix, 0.0, DecomposedSymmetricPositiveMatrix.class,
                ABSTOL, RELTOL);
        CheckUtils.checkPositiveScalarAdd(matrix, +1.718, DecomposedSymmetricPositiveMatrix.class,
                ABSTOL, RELTOL);

        // Exception check
        try {
            matrix.positiveScalarAdd(-Double.MIN_VALUE);
            Assert.fail();
        } catch (final NotPositiveException e) {
            Assert.assertEquals(NotPositiveException.class, e.getClass());
            Assert.assertEquals("invalid scalar -0 (must be positive)", e.getMessage());
        }
    }

    /**
     * Tests the method that multiplies the matrix by a positive scalar.<br>
     * An exception should be thrown if the provided scalar is negative.
     *
     * <p>
     * Tested method:<br>
     * {@link DecomposedSymmetricPositiveMatrix#positiveScalarMultiply(double)}
     * </p>
     */
    @Test
    public void testPositiveScalarMultiply() {
        // Tested matrix
        final double[][] dataBT = { { -0.30086, 8.94055, -5.06504, 1.49209 },
                { -1.90506, 1.48789, 7.52955, 6.13320 }, { -0.36897, 7.44298, -2.19644, -7.93431 },
                { 7.23686, -8.23233, -7.83937, 8.11401 }, { 0.02999, 8.39619, 4.87065, 1.93103 },
                { -1.50558, 5.87311, 2.64138, -0.96333 } };
        final SymmetricPositiveMatrix matrix = new DecomposedSymmetricPositiveMatrix(dataBT);

        // Test the method
        CheckUtils.checkPositiveScalarMultiply(matrix, 1.0,
                DecomposedSymmetricPositiveMatrix.class, ABSTOL, RELTOL);
        CheckUtils.checkPositiveScalarMultiply(matrix, +2.838,
                DecomposedSymmetricPositiveMatrix.class, ABSTOL, RELTOL);

        // Exception check
        try {
            matrix.positiveScalarMultiply(-Double.MIN_VALUE);
            Assert.fail();
        } catch (final NotPositiveException e) {
            Assert.assertEquals(NotPositiveException.class, e.getClass());
            Assert.assertEquals("invalid scalar -0 (must be positive)", e.getMessage());
        }
    }

    /**
     * Tests the methods that add another matrix.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#add(RealMatrix)}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#add(SymmetricMatrix)}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#add(SymmetricPositiveMatrix)}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#add(DecomposedSymmetricPositiveMatrix)}<br>
     * </p>
     */
    @Test
    public void testAddMatrix() {
        // Tested matrix
        final double[][] initialDataBT = { { -1.27865, 0.64670, 4.49999, 0.41084 },
                { -3.92603, -2.35166, -8.89522, 6.32458 },
                { -6.86691, 2.91269, -9.21873, 1.58816 }, { -3.59147, 8.22786, 5.95615, -1.30534 },
                { 8.42666, 0.25811, 5.86310, 7.25978 }, { 8.42604, 6.67460, 4.47366, 6.22220 } };
        final SymmetricPositiveMatrix matrix = new DecomposedSymmetricPositiveMatrix(initialDataBT);

        // Matrices to be added
        final double[] diagonalElements = { -0.88783, 7.93157, -7.55840, -9.75790 };
        final double[][] data = { { 5.87869, -0.51459, -1.76840, -0.19300 },
                { -0.51459, 4.62756, -0.53358, 1.48986 },
                { -1.76840, -0.53358, 6.55206, -0.89095 }, { -0.19300, 1.48986, -0.89095, 6.73144 } };
        final double[][] dataBT = { { -5.44151, 3.48164, -5.92405, -9.69776 },
                { 8.63787, 2.64183, -4.25679, -0.17036 } };

        final RealMatrix realMatrix = new Array2DRowRealMatrix(data);
        final DiagonalMatrix diagonalMatrix = new DiagonalMatrix(diagonalElements);
        final SymmetricMatrix symmetricMatrix = new ArrayRowSymmetricMatrix(SymmetryType.LOWER,
                data, 0., 0.);
        final SymmetricPositiveMatrix positiveMatrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, data, 0., 0., 0., 0.);
        final SymmetricPositiveMatrix decomposedMatrix = new DecomposedSymmetricPositiveMatrix(
                dataBT);

        // Check the add methods
        CheckUtils.checkAddMatrix(matrix, realMatrix, Array2DRowRealMatrix.class, ABSTOL, RELTOL);
        CheckUtils.checkAddMatrix(matrix, symmetricMatrix, ArrayRowSymmetricMatrix.class, ABSTOL,
                RELTOL);
        CheckUtils.checkAddMatrix(matrix, positiveMatrix, ArrayRowSymmetricPositiveMatrix.class,
                ABSTOL, RELTOL);
        CheckUtils.checkAddMatrix(matrix, diagonalMatrix, ArrayRowSymmetricMatrix.class, ABSTOL,
                RELTOL);
        CheckUtils.checkAddMatrix(matrix, decomposedMatrix,
                DecomposedSymmetricPositiveMatrix.class, ABSTOL, RELTOL);
    }

    /**
     * Tests the methods that add another matrix.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#add(DecomposedSymmetricPositiveMatrix)}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#add(DecomposedSymmetricPositiveMatrix, boolean)}
     * <br>
     * </p>
     */
    @Test
    public void testAddMatrixAndResize() {
        double[][] dataBT;
        RealMatrix expected;
        DecomposedSymmetricPositiveMatrix result;

        // Tested matrix
        final double[][] initialDataBT = { { 2.95915, 7.77889, 5.12594, 5.29514 },
                { 1.37241, 5.76383, -3.32123, 9.92629 }, { 5.55226, -9.53113, -8.09290, 9.45415 },
                { 4.56496, -0.36941, 4.32049, -7.97593 }, { -6.83323, 0.08934, 0.28000, 2.35685 },
                { -8.29541, 7.69592, -2.09590, 6.75660 } };
        final DecomposedSymmetricPositiveMatrix matrix = new DecomposedSymmetricPositiveMatrix(
                initialDataBT);

        // Copy the matrix as a standard real matrix
        final RealMatrix referenceMatrix = new Array2DRowRealMatrix(matrix.getData(), false);

        // Matrix to be added
        dataBT = new double[][] { { -1.77650, 8.39991, 6.16110, -3.29839 },
                { 5.86259, -0.72574, -7.82747, -3.07575 } };
        final DecomposedSymmetricPositiveMatrix decomposedMatrix = new DecomposedSymmetricPositiveMatrix(
                dataBT);

        // Expected matrix B^T when resizing is enabled
        dataBT = new double[][] {
                { -14.674443836101593, 7.312363134786209, 3.813574234242793, 2.654396068972106 },
                { 0., -16.172862051867000, -6.783700876103576, -1.147993285837965 },
                { 0., 0., 12.829855199922212, -11.259232900631900 },
                { 0., 0., 0., 14.696194062485178 } };
        final RealMatrix expectedResizedBT = new Array2DRowRealMatrix(dataBT);

        // Expected matrix B^T when resizing is disabled
        dataBT = new double[][] { { 2.95915, 7.77889, 5.12594, 5.29514 },
                { 1.37241, 5.76383, -3.32123, 9.92629 }, { 5.55226, -9.53113, -8.0929, 9.45415 },
                { 4.56496, -0.36941, 4.32049, -7.97593 }, { -6.83323, 0.08934, 0.28000, 2.35685 },
                { -8.29541, 7.69592, -2.0959, 6.7566 }, { -1.7765, 8.39991, 6.16110, -3.29839 },
                { 5.86259, -0.72574, -7.82747, -3.07575 } };

        final RealMatrix expectedCombinedBT = new Array2DRowRealMatrix(dataBT);

        // Test the methods
        result = matrix.add(decomposedMatrix);
        expected = referenceMatrix.add(decomposedMatrix);
        CheckUtils.checkEquality(expected, result, ABSTOL, RELTOL);
        CheckUtils.checkEquality(expectedResizedBT, result.getBT(), ABSTOL, RELTOL);

        result = matrix.add(decomposedMatrix, true);
        expected = referenceMatrix.add(decomposedMatrix);
        CheckUtils.checkEquality(expected, result, ABSTOL, RELTOL);
        CheckUtils.checkEquality(expectedResizedBT, result.getBT(), ABSTOL, RELTOL);

        result = matrix.add(decomposedMatrix, false);
        expected = referenceMatrix.add(decomposedMatrix);
        CheckUtils.checkEquality(expected, result, ABSTOL, RELTOL);
        CheckUtils.checkEquality(expectedCombinedBT, result.getBT(), ABSTOL, RELTOL);
    }

    /**
     * Tests the methods that add another matrix, using a matrix with incompatible dimensions.<br>
     * An exception should be thrown.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#add(RealMatrix)}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#add(SymmetricMatrix)}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#add(SymmetricPositiveMatrix)}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#add(DecomposedSymmetricPositiveMatrix)}<br>
     * </p>
     */
    @Test
    public void testAddIncompatibleMatrix() {
        final SymmetricPositiveMatrix matrix = new DecomposedSymmetricPositiveMatrix(3);
        CheckUtils.checkAddIncompatibleRealMatrix(matrix);
        CheckUtils.checkAddIncompatibleSymmetricMatrix(matrix);
        CheckUtils.checkAddIncompatibleSymmetricPositiveMatrix(matrix);
        CheckUtils.checkAddIncompatibleDecomposedMatrix(matrix);
        CheckUtils.checkAddIncompatibleDiagonalMatrix(matrix);
    }

    /**
     * Tests the methods that subtract another matrix.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#subtract(RealMatrix)}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#subtract(SymmetricMatrix)}<br>
     * </p>
     */
    @Test
    public void testSubtractMatrix() {
        // Tested matrix
        final double[][] initialDataBT = { { -6.53893, -2.98619, 2.80149, 6.14671 },
                { -3.53423, 2.99553, 5.68610, 7.19508 }, { 4.92775, -5.56798, 9.14332, 9.95447 },
                { 3.34022, 9.35914, -3.59032, 7.72655 }, { 1.10391, -6.72501, -4.99888, 2.77507 },
                { -4.76658, -7.67625, -9.80665, -3.16212 } };
        final SymmetricPositiveMatrix matrix = new DecomposedSymmetricPositiveMatrix(initialDataBT);

        // Matrices to be subtracted
        final double[] diagonalElements = { -9.85665, -3.59154, 5.64314, 4.98748 };
        final double[][] data = { { 3.72329, -0.52174, 1.46074, -1.76727 },
                { -0.52174, 5.95714, 0.51317, -3.01210 }, { 1.46074, 0.51317, 8.83025, 1.28913 },
                { -1.76727, -3.01210, 1.28913, 6.04239 } };
        final double[][] dataBT = { { 2.12254, 8.34709, -0.42755, 6.59821 },
                { 4.62903, 2.11310, -1.35703, -4.25615 } };

        final RealMatrix realMatrix = new Array2DRowRealMatrix(data);
        final DiagonalMatrix diagonalMatrix = new DiagonalMatrix(diagonalElements);
        final SymmetricMatrix symmetricMatrix = new ArrayRowSymmetricMatrix(SymmetryType.LOWER,
                data, 0., 0.);
        final SymmetricPositiveMatrix positiveMatrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, data, 0., 0., 0., 0.);
        final SymmetricPositiveMatrix decomposedMatrix = new DecomposedSymmetricPositiveMatrix(
                dataBT);

        // Check the subtract methods
        CheckUtils.checkSubtractMatrix(matrix, realMatrix, Array2DRowRealMatrix.class, ABSTOL,
                RELTOL);
        CheckUtils.checkSubtractMatrix(matrix, symmetricMatrix, ArrayRowSymmetricMatrix.class,
                ABSTOL, RELTOL);
        CheckUtils.checkSubtractMatrix(matrix, positiveMatrix, ArrayRowSymmetricMatrix.class,
                ABSTOL, RELTOL);
        CheckUtils.checkSubtractMatrix(matrix, diagonalMatrix, ArrayRowSymmetricMatrix.class,
                ABSTOL, RELTOL);
        CheckUtils.checkSubtractMatrix(matrix, decomposedMatrix, ArrayRowSymmetricMatrix.class,
                ABSTOL, RELTOL);
    }

    /**
     * Tests the methods that subtract another matrix, using a matrix with incompatible dimensions.<br>
     * An exception should be thrown.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#subtract(RealMatrix)}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#subtract(SymmetricMatrix)}<br>
     * </p>
     */
    @Test
    public void testSubtractIncompatibleMatrix() {
        final SymmetricPositiveMatrix matrix = new DecomposedSymmetricPositiveMatrix(3);
        CheckUtils.checkSubtractIncompatibleRealMatrix(matrix);
        CheckUtils.checkSubtractIncompatibleSymmetricMatrix(matrix);
        CheckUtils.checkSubtractIncompatibleSymmetricPositiveMatrix(matrix);
        CheckUtils.checkSubtractIncompatibleDecomposedMatrix(matrix);
        CheckUtils.checkSubtractIncompatibleDiagonalMatrix(matrix);
    }

    /**
     * Tests the methods that postmultiply the matrix by another matrix, using a square matrix.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#multiply(RealMatrix)}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#multiply(RealMatrix, boolean)}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#multiply(RealMatrix, boolean, double)}<br>
     * </p>
     */
    @Test
    public void testMultiplyBySquareMatrix() {
        // Tested matrix
        final double[][] initialDataBT = { { -7.50320, -9.57425, -4.56238, 5.84904 },
                { 6.29731, -7.56140, 3.44560, -9.12266 },
                { -4.35464, 1.39410, -0.45898, -8.77029 }, { 9.67975, 1.92952, -3.14748, 7.75645 },
                { 6.26772, -8.84415, -2.47928, 4.92560 },
                { -2.27779, -7.35220, -8.27918, -8.83096 } };
        final SymmetricPositiveMatrix matrix = new DecomposedSymmetricPositiveMatrix(initialDataBT);

        // Matrices to multiply by
        final double[] diagonalElements = { -1.81582, -8.31359, -6.86639, 4.25045 };
        final double[][] data = { { 5.71726, -0.47839, 0.81230, -0.14456 },
                { -0.47839, 5.96526, -0.08322, 0.38137 }, { 0.81230, -0.08322, 4.64741, -0.86481 },
                { -0.14456, 0.38137, -0.86481, 5.46950 } };
        final double[][] dataBT = { { -3.64038, -0.57001, 6.67744, 5.81487 },
                { -6.17048, -3.13943, -0.08637, 8.65491 } };

        final RealMatrix realMatrix = new Array2DRowRealMatrix(data);
        final DiagonalMatrix diagonalMatrix = new DiagonalMatrix(diagonalElements);
        final SymmetricMatrix symmetricMatrix = new ArrayRowSymmetricMatrix(SymmetryType.LOWER,
                data, 0., 0.);
        final SymmetricPositiveMatrix positiveMatrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, data, 0., 0., 0., 0.);
        final SymmetricPositiveMatrix decomposedMatrix = new DecomposedSymmetricPositiveMatrix(
                dataBT);

        // Scaling factor
        final double scalingFactor = -3.193;

        // Test the methods
        CheckUtils.checkMultiplyMatrix(matrix, realMatrix, scalingFactor,
                Array2DRowRealMatrix.class, ABSTOL, RELTOL);
        CheckUtils.checkMultiplyMatrix(matrix, symmetricMatrix, scalingFactor,
                Array2DRowRealMatrix.class, ABSTOL, RELTOL);
        CheckUtils.checkMultiplyMatrix(matrix, positiveMatrix, scalingFactor,
                Array2DRowRealMatrix.class, ABSTOL, RELTOL);
        CheckUtils.checkMultiplyMatrix(matrix, diagonalMatrix, scalingFactor,
                Array2DRowRealMatrix.class, ABSTOL, RELTOL);
        CheckUtils.checkMultiplyMatrix(matrix, decomposedMatrix, scalingFactor,
                Array2DRowRealMatrix.class, ABSTOL, RELTOL);
    }

    /**
     * Tests the methods that postmultiply the matrix by another matrix, using a rectangular matrix.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#multiply(RealMatrix)}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#multiply(RealMatrix, boolean)}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#multiply(RealMatrix, boolean, double)}<br>
     * </p>
     */
    @Test
    public void testMultiplyByRectangularMatrix() {
        // Tested matrix
        final double[][] initialDataBT = { { -5.88338, -2.02082, 9.08418, 5.56497 },
                { 4.72430, 4.88847, 4.32718, -1.55272 }, { 1.15143, 3.13022, -1.01913, -2.10407 },
                { -6.15218, 6.76288, -6.17769, -4.83215 },
                { -5.86956, 8.14522, -5.43545, -6.70862 },
                { -1.22408, -9.80506, -0.76848, -3.85688 } };
        final SymmetricPositiveMatrix matrix = new DecomposedSymmetricPositiveMatrix(initialDataBT);

        // Matrices to multiply by
        final double[][] data = { { -6.36442, 6.87884, 1.97997, 9.81404, -5.94812, 7.80561 },
                { 4.77288, 3.22673, -1.66613, 3.84839, -3.39464, -1.71933 },
                { -8.30484, -6.63637, -1.93514, 6.43295, 2.38472, 9.92345 },
                { 3.17230, 1.58189, 8.75061, 1.14971, 8.02359, -5.24739 } };
        final RealMatrix realMatrix = new Array2DRowRealMatrix(data);

        // Scaling factor
        final double scalingFactor = -7.384;

        // Test the methods
        CheckUtils.checkMultiplyMatrix(matrix, realMatrix, scalingFactor,
                Array2DRowRealMatrix.class, ABSTOL, RELTOL);
    }

    /**
     * Tests the methods that postmultiply the matrix by another matrix, using a matrix with
     * incompatible dimensions.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#multiply(RealMatrix)}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#multiply(RealMatrix, boolean)}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#multiply(RealMatrix, boolean, double)}<br>
     * </p>
     */
    @Test
    public void testMultiplyByIncompatibleMatrix() {
        final SymmetricPositiveMatrix matrix = new DecomposedSymmetricPositiveMatrix(3);
        CheckUtils.checkMultiplyIncompatibleRealMatrix(matrix);
        CheckUtils.checkMultiplyIncompatibleSymmetricMatrix(matrix);
        CheckUtils.checkMultiplyIncompatibleSymmetricPositiveMatrix(matrix);
        CheckUtils.checkMultiplyIncompatibleDecomposedMatrix(matrix);
        CheckUtils.checkMultiplyIncompatibleDiagonalMatrix(matrix);
    }

    /**
     * Tests the method that premultiplies the matrix by another matrix, using a square matrix.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#preMultiply(RealMatrix)}
     * </p>
     */
    @Test
    public void testPreMultiplyBySquareMatrix() {
        // Tested matrix
        final double[][] initialDataBT = { { 0.99836, 9.27936, -9.94678, -0.00038 },
                { 6.80942, 0.87602, 7.65730, -2.20478 }, { 7.30700, 9.97432, 5.10836, -9.55483 },
                { 2.23145, -3.26990, 1.52705, 7.07870 }, { -7.82474, -0.97477, -5.32993, 6.10920 },
                { -4.35710, 4.58909, 4.36042, 4.33816 } };
        final SymmetricPositiveMatrix matrix = new DecomposedSymmetricPositiveMatrix(initialDataBT);

        // Matrices to premultiply by
        final double[] diagonalElements = { -1.07221, -6.87991, -5.78367, -4.39014 };
        final double[][] data = { { 4.15218, -3.08301, 2.38959, -0.18749 },
                { -3.08301, 4.20339, -1.60917, 0.85146 }, { 2.38959, -1.60917, 4.02908, -0.16557 },
                { -0.18749, 0.85146, -0.16557, 2.62149 } };
        final double[][] dataBT = { { -2.81085, 6.57222, 8.75145, 6.29243 },
                { -9.82840, -3.43928, 7.10955, -0.97980 } };

        final RealMatrix realMatrix = new Array2DRowRealMatrix(data);
        final DiagonalMatrix diagonalMatrix = new DiagonalMatrix(diagonalElements);
        final SymmetricMatrix symmetricMatrix = new ArrayRowSymmetricMatrix(SymmetryType.LOWER,
                data, 0., 0.);
        final SymmetricPositiveMatrix positiveMatrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, data, 0., 0., 0., 0.);
        final SymmetricPositiveMatrix decomposedMatrix = new DecomposedSymmetricPositiveMatrix(
                dataBT);

        // Test the method
        CheckUtils.checkPreMultiplyMatrix(matrix, realMatrix, Array2DRowRealMatrix.class, ABSTOL,
                RELTOL);
        CheckUtils.checkPreMultiplyMatrix(matrix, symmetricMatrix, Array2DRowRealMatrix.class,
                ABSTOL, RELTOL);
        CheckUtils.checkPreMultiplyMatrix(matrix, positiveMatrix, Array2DRowRealMatrix.class,
                ABSTOL, RELTOL);
        CheckUtils.checkPreMultiplyMatrix(matrix, diagonalMatrix, Array2DRowRealMatrix.class,
                ABSTOL, RELTOL);
        CheckUtils.checkPreMultiplyMatrix(matrix, decomposedMatrix, Array2DRowRealMatrix.class,
                ABSTOL, RELTOL);
    }

    /**
     * Tests the method that premultiplies the matrix by another matrix, using a rectangular matrix.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#preMultiply(RealMatrix)}<br>
     * </p>
     */
    @Test
    public void testPreMultiplyByRectangularMatrix() {
        // Tested matrix
        final double[][] initialDataBT = { { -3.69066, 2.72264, 4.48654, -1.97220 },
                { 6.35687, -7.66779, -5.07073, -8.93263 },
                { -7.43825, -1.73966, 6.51038, -7.12652 },
                { 7.50144, -3.05367, -7.73700, -7.73532 }, { 1.95981, 3.90182, 7.66255, -2.14276 },
                { -7.76470, 7.58139, -1.44074, 4.40658 } };
        final SymmetricPositiveMatrix matrix = new DecomposedSymmetricPositiveMatrix(initialDataBT);

        // Matrices to premultiply by
        final double[][] data = { { -3.25739, -8.56073, 8.34193, -4.48396 },
                { -3.22941, -6.87303, 2.32070, 3.63254 }, { -5.92316, -9.74408, 6.59002, 3.14803 },
                { -4.54055, -5.07744, 0.51204, 1.31609 }, { 4.35106, 7.36858, 5.89275, 7.08131 },
                { -6.11229, -9.36486, 5.88054, -1.80181 } };
        final RealMatrix realMatrix = new Array2DRowRealMatrix(data);

        // Test the methods
        CheckUtils.checkPreMultiplyMatrix(matrix, realMatrix, Array2DRowRealMatrix.class, ABSTOL,
                RELTOL);
    }

    /**
     * Tests the method that premultiplies the matrix by another matrix, using a matrix with
     * incompatible dimensions.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#preMultiply(RealMatrix)}
     * </p>
     */
    @Test
    public void testPreMultiplyByIncompatibleMatrix() {
        final SymmetricPositiveMatrix matrix = new DecomposedSymmetricPositiveMatrix(3);
        CheckUtils.checkPreMultiplyIncompatibleRealMatrix(matrix);
        CheckUtils.checkPreMultiplyIncompatibleSymmetricMatrix(matrix);
        CheckUtils.checkPreMultiplyIncompatibleSymmetricPositiveMatrix(matrix);
        CheckUtils.checkPreMultiplyIncompatibleDecomposedMatrix(matrix);
        CheckUtils.checkPreMultiplyIncompatibleDiagonalMatrix(matrix);
    }

    /**
     * Tests the methods that postmultiply the matrix by a vector.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#operate(double[])}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#operate(RealVector)}<br>
     * </p>
     */
    @Test
    public void testOperateVector() {
        // Tested matrix
        final double[][] initialDataBT = { { 1.59901, -6.90731, -3.98330, -9.90710 },
                { 7.82720, 7.21921, 5.75464, -4.70775 }, { -7.71126, 6.36149, 5.28051, 5.65166 },
                { -5.46141, -7.81584, -5.32189, 5.11172 },
                { 3.31251, -3.26905, -7.30254, 9.88507 }, { -1.15830, 1.86423, -6.25876, -4.00858 } };
        final SymmetricPositiveMatrix matrix = new DecomposedSymmetricPositiveMatrix(initialDataBT);

        // Vector to multiply the tested matrix by
        final double[] vectorData = { 3.12087, 2.98966, -1.00491, -9.46499 };
        final RealVector vector = new ArrayRealVector(vectorData);

        // Test the method
        CheckUtils.checkOperateVector(matrix, vector, ABSTOL, RELTOL);
    }

    /**
     * Tests the methods that postmultiply the matrix by a vector, using a vector with an
     * incompatible dimension.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#operate(double[])}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#operate(RealVector)}<br>
     * </p>
     */
    @Test
    public void testOperateIncompatibleVector() {
        final SymmetricPositiveMatrix matrix = new DecomposedSymmetricPositiveMatrix(3);
        CheckUtils.checkOperateIncompatibleVector(matrix);
    }

    /**
     * Tests the methods that premultiply the matrix by a vector, using a vector with an
     * incompatible dimension.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#preMultiply(double[])}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#preMultiply(RealVector)}<br>
     * </p>
     */
    @Test
    public void testPreMultiplyByVector() {
        // Tested matrix
        final double[][] initialDataBT = { { -6.41913, -7.65343, -6.98970, 0.16006 },
                { -6.69493, 6.99718, 8.36604, 3.49703 }, { -4.98325, -8.80269, -6.10468, 1.41606 },
                { 6.09205, -4.80102, -4.39199, 1.52115 }, { 1.06737, 1.33200, -3.34683, 0.03716 },
                { 2.45153, 2.59000, 7.32427, 9.29136 } };
        final SymmetricPositiveMatrix matrix = new DecomposedSymmetricPositiveMatrix(initialDataBT);

        // Vector to premultiply the tested matrix by
        final double[] vectorData = { 2.29094, -3.82040, -8.31724, -0.20550 };
        final RealVector vector = new ArrayRealVector(vectorData);

        // Test the method
        CheckUtils.checkPreMultiplyVector(matrix, vector, ABSTOL, RELTOL);
    }

    /**
     * Tests the premultiplication by a vector which has incompatible dimensions.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#preMultiply(double[])}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#preMultiply(RealVector)}<br>
     * </p>
     */
    @Test
    public void testPreMultiplyByIncompatibleVector() {
        final SymmetricPositiveMatrix matrix = new DecomposedSymmetricPositiveMatrix(3);
        CheckUtils.checkPreMultiplyIncompatibleVector(matrix);
    }

    /**
     * Tests the quadratic multiplication.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#quadraticMultiplication(RealMatrix)}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#quadraticMultiplication(RealMatrix, boolean)}<br>
     * </p>
     */
    @Test
    public void testQuadraticMultiplicationSquareMatrix() {
        // Tested matrix
        final double[][] initialDataBT = { { 0.77980, -0.29950, 8.14974, 9.11949 },
                { 5.55549, -6.81056, 3.09203, 7.17671 }, { 0.11692, -6.48556, -0.72492, 2.33701 },
                { -6.31550, 7.02678, 1.84372, 0.58709 }, { 2.77391, -1.86064, -3.05186, 0.09791 },
                { 2.61746, -2.49288, -2.36580, -8.26388 } };
        final SymmetricPositiveMatrix matrix = new DecomposedSymmetricPositiveMatrix(initialDataBT);

        // Square matrix used for the quadratic multiplication
        final double[][] data = { { 0.78597, -5.73327, 2.78938, 4.95348 },
                { -1.17709, -0.34791, -1.18072, -6.32408 },
                { -1.26601, -6.21215, 1.51582, -0.23162 }, { 4.66089, 4.83674, -8.03276, -2.65091 } };
        final RealMatrix realMatrix = new Array2DRowRealMatrix(data);

        // Test the methods
        CheckUtils.checkQuadraticMultiplication(matrix, realMatrix,
                DecomposedSymmetricPositiveMatrix.class, ABSTOL, RELTOL);
    }

    /**
     * Tests the quadratic multiplication using a rectangular matrix.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#quadraticMultiplication(RealMatrix)}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#quadraticMultiplication(RealMatrix, boolean)}<br>
     * </p>
     */
    @Test
    public void testQuadraticMultiplicationRectangularMatrix() {
        // Tested matrix
        final double[][] initialDataBT = { { -2.52671, 2.38427, 3.93063, -9.51094 },
                { 2.92562, 8.78287, 5.67477, -1.84551 }, { -1.46373, 8.90179, -7.52611, -8.48066 },
                { 9.63028, -5.28328, -7.59984, 6.57884 },
                { -4.50491, -3.63978, -0.80583, 0.96219 }, { 0.44274, 2.15061, 8.72010, -7.59817 } };
        final SymmetricPositiveMatrix matrix = new DecomposedSymmetricPositiveMatrix(initialDataBT);

        // Rectangular matrix used for the quadratic multiplication
        final double[][] data = { { -2.52671, 2.38427, 3.93063, -9.51094 },
                { 2.92562, 8.78287, 5.67477, -1.84551 }, { -1.46373, 8.90179, -7.52611, -8.48066 },
                { 9.63028, -5.28328, -7.59984, 6.57884 },
                { -4.50491, -3.63978, -0.80583, 0.96219 }, { 0.44274, 2.15061, 8.72010, -7.59817 } };
        final RealMatrix realMatrix = new Array2DRowRealMatrix(data);

        // Test the methods
        CheckUtils.checkQuadraticMultiplication(matrix, realMatrix,
                DecomposedSymmetricPositiveMatrix.class, ABSTOL, RELTOL);
    }

    /**
     * Tests the quadratic multiplication using a matrix which have incompatible dimensions.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#quadraticMultiplication(RealMatrix)}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#quadraticMultiplication(RealMatrix, boolean)}<br>
     * </p>
     */
    @Test
    public void testQuadraticMultiplicationIncompatibleMatrix() {
        final SymmetricPositiveMatrix matrix = new DecomposedSymmetricPositiveMatrix(4);
        CheckUtils.checkQuadraticMultiplicationIncompatibleMatrix(matrix);
    }

    /**
     * Tests the reduction of the matrix B<sup>T</sup>.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#resizeB()}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#getResizedB()}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#getResizedBT()}<br>
     * </p>
     */
    @Test
    public void testReduceMatrixBT() {
        RealMatrix result;

        // Tested matrix
        final double[][] dataBT = { { -7.60630, 5.34202, 6.82137, -2.67341 },
                { -6.03790, -8.34839, 8.45854, 9.22248 }, { 5.87731, -7.06662, 5.77292, -8.18020 },
                { 1.07088, 7.87809, 6.40033, 4.12566 }, { 7.84558, 3.53805, 9.87194, 2.43333 },
                { -6.68163, -4.00808, 9.67004, -5.58163 } };
        final DecomposedSymmetricPositiveMatrix matrix = new DecomposedSymmetricPositiveMatrix(
                dataBT);

        // Expected B^T matrix after reduction
        final double[][] data = {
                { 15.36876370772223, 2.03113746590528, -3.21005090192201, -1.472068071456673 },
                { 0., +15.31963436993507, -1.426846970880793, +2.154441209900655 },
                { 0., 0., -19.26118820160786, -0.3816010353662325 },
                { 0., 0., 0., -14.36173626396847 } };
        final RealMatrix expectedBT = new Array2DRowRealMatrix(data);
        final RealMatrix expectedB = expectedBT.transpose();

        // Ensure the matrix built from the expected B^T matrix is the same as the tested matrix
        CheckUtils.checkEquality(matrix, new DecomposedSymmetricPositiveMatrix(expectedBT), ABSTOL,
                RELTOL);

        // Retrieve the resized matrix B^T
        result = matrix.getResizedB();
        CheckUtils.checkEquality(expectedB, result, ABSTOL, RELTOL);
        result = matrix.getResizedBT();
        CheckUtils.checkEquality(expectedBT, result, ABSTOL, RELTOL);

        // Resize the internal matrix B^T
        matrix.resizeB();
        result = matrix.getB();
        CheckUtils.checkEquality(expectedB, result, ABSTOL, RELTOL);
        result = matrix.getBT();
        CheckUtils.checkEquality(expectedBT, result, ABSTOL, RELTOL);
        result = matrix.getResizedB();
        CheckUtils.checkEquality(expectedB, result, ABSTOL, RELTOL);
        result = matrix.getResizedBT();
        CheckUtils.checkEquality(expectedBT, result, ABSTOL, RELTOL);

        // Resize the internal matrix B^T again
        // (it should have no effect)
        matrix.resizeB();
        result = matrix.getB();
        CheckUtils.checkEquality(expectedB, result, ABSTOL, RELTOL);
        result = matrix.getBT();
        CheckUtils.checkEquality(expectedBT, result, ABSTOL, RELTOL);
        result = matrix.getResizedB();
        CheckUtils.checkEquality(expectedB, result, ABSTOL, RELTOL);
        result = matrix.getResizedBT();
        CheckUtils.checkEquality(expectedBT, result, ABSTOL, RELTOL);
    }

    /**
     * Tests the extension of the matrix B<sup>T</sup>.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#resizeB()}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#getResizedB()}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#getResizedBT()}<br>
     * </p>
     */
    @Test
    public void testExtendMatrixBT() {
        RealMatrix result;

        // Tested matrix
        final double[][] dataBT = { { -5.83473, -4.22195, -4.80148, 2.52581 },
                { -7.97699, 9.79102, -9.28219, 3.46154 } };
        final DecomposedSymmetricPositiveMatrix matrix = new DecomposedSymmetricPositiveMatrix(
                dataBT);

        // Expected B^T matrix after reduction
        final double[][] data = { { -5.83473, -4.22195, -4.80148, 2.52581 },
                { -7.97699, 9.79102, -9.28219, 3.46154 }, { 0., 0., 0., 0. }, { 0., 0., 0., 0. } };
        final RealMatrix expectedBT = new Array2DRowRealMatrix(data);
        final RealMatrix expectedB = expectedBT.transpose();

        // Ensure the matrix built from the expected B^T matrix is the same as the tested matrix
        CheckUtils.checkEquality(matrix, new DecomposedSymmetricPositiveMatrix(expectedBT), ABSTOL,
                RELTOL);

        // Retrieve the resized matrix B^T
        result = matrix.getResizedB();
        CheckUtils.checkEquality(expectedB, result, ABSTOL, RELTOL);
        result = matrix.getResizedBT();
        CheckUtils.checkEquality(expectedBT, result, ABSTOL, RELTOL);

        // Resize the internal matrix B^T
        matrix.resizeB();
        result = matrix.getB();
        CheckUtils.checkEquality(expectedB, result, ABSTOL, RELTOL);
        result = matrix.getBT();
        CheckUtils.checkEquality(expectedBT, result, ABSTOL, RELTOL);
        result = matrix.getResizedB();
        CheckUtils.checkEquality(expectedB, result, ABSTOL, RELTOL);
        result = matrix.getResizedBT();
        CheckUtils.checkEquality(expectedBT, result, ABSTOL, RELTOL);

        // Resize the internal matrix B^T again
        // (it should have no effect)
        matrix.resizeB();
        result = matrix.getB();
        CheckUtils.checkEquality(expectedB, result, ABSTOL, RELTOL);
        result = matrix.getBT();
        CheckUtils.checkEquality(expectedBT, result, ABSTOL, RELTOL);
        result = matrix.getResizedB();
        CheckUtils.checkEquality(expectedB, result, ABSTOL, RELTOL);
        result = matrix.getResizedBT();
        CheckUtils.checkEquality(expectedBT, result, ABSTOL, RELTOL);
    }

    /**
     * Tests the methods that return a row of the matrix.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#getRow(int)}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#getRowVector(int)}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#getRowMatrix(int)}<br>
     * </p>
     */
    @Test
    public void testGetRow() {
        // Tested matrix
        final double[][] initialDataBT = { { 9.78918, -3.99126, -0.68746, -0.20287 },
                { -4.92108, -9.09875, -0.98545, 2.79308 }, { -7.48139, 8.19833, 7.77823, 7.36075 },
                { 3.03955, 2.57865, -0.12426, -8.05372 }, { 9.68199, 4.25590, -4.10251, -4.93783 },
                { 4.65248, 0.40708, 1.77836, -9.26390 } };
        final SymmetricPositiveMatrix matrix = new DecomposedSymmetricPositiveMatrix(initialDataBT);

        // Test the methods
        CheckUtils.checkGetRow(matrix);
    }

    /**
     * Tests the methods that return a row of the matrix, using indices outside the valid index
     * range.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#getRow(int)}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#getRowVector(int)}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#getRowMatrix(int)}<br>
     * </p>
     */
    @Test
    public void testGetOutOfRangeRow() {
        final SymmetricPositiveMatrix matrix = new DecomposedSymmetricPositiveMatrix(3);
        CheckUtils.checkGetOutOfRangeRow(matrix);
    }

    /**
     * Tests the methods that set a row of the matrix.<br>
     * This method should throw an exception since it is forbidden.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#setRow(int, double[])}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#setRowVector(int, RealVector)}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#setRowMatrix(int, RealMatrix)}<br>
     * </p>
     */
    @Test
    public void testSetRow() {
        final int dim = 3;
        final SymmetricPositiveMatrix matrix = new DecomposedSymmetricPositiveMatrix(dim);
        final double[] rowData = { 1., 1., 1. };
        final RealVector rowVector = new ArrayRealVector(rowData);
        final RealMatrix rowMatrix = MatrixUtils.createRowRealMatrix(rowData);

        for (int i = 0; i < dim; i++) {
            // Array
            try {
                matrix.setRow(i, rowData);
                Assert.fail();
            } catch (final MathUnsupportedOperationException e) {
                final String expectedMessage = String.format(UNSUPPORTED_OPERATION_FORMAT);
                Assert.assertEquals(MathUnsupportedOperationException.class, e.getClass());
                Assert.assertEquals(expectedMessage, e.getMessage());
            }

            // Vector
            try {
                matrix.setRowVector(i, rowVector);
                Assert.fail();
            } catch (final MathUnsupportedOperationException e) {
                final String expectedMessage = String.format(UNSUPPORTED_OPERATION_FORMAT);
                Assert.assertEquals(MathUnsupportedOperationException.class, e.getClass());
                Assert.assertEquals(expectedMessage, e.getMessage());
            }

            // Matrix
            try {
                matrix.setRowMatrix(0, rowMatrix);
                Assert.fail();
            } catch (final MathUnsupportedOperationException e) {
                final String expectedMessage = String.format(UNSUPPORTED_OPERATION_FORMAT);
                Assert.assertEquals(MathUnsupportedOperationException.class, e.getClass());
                Assert.assertEquals(expectedMessage, e.getMessage());
            }
        }
    }

    /**
     * Tests the methods that return a column of the matrix.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#getColumn(int)}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#getColumnVector(int)}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#getColumnMatrix(int)}<br>
     * </p>
     */
    @Test
    public void testGetColumn() {
        // Tested matrix
        final double[][] initialDataBT = { { -0.72411, 5.99555, -4.80884, -5.54045 },
                { 7.16270, 3.53380, -4.70465, -5.06579 }, { -9.04125, 9.10542, -8.63662, 6.14486 },
                { 0.83836, -8.42831, 7.41494, -7.74526 },
                { 2.15577, -8.04971, -7.80980, -0.15455 }, { -7.57843, -1.18468, 8.54694, 1.68318 } };
        final SymmetricPositiveMatrix matrix = new DecomposedSymmetricPositiveMatrix(initialDataBT);

        // Test the methods
        CheckUtils.checkGetColumn(matrix);
    }

    /**
     * Tests the methods that return a column of the matrix, using indices outside the valid index
     * range.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#getColumn(int)}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#getColumnVector(int)}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#getColumnMatrix(int)}<br>
     * </p>
     */
    @Test
    public void testGetOutOfRangeColumn() {
        final SymmetricPositiveMatrix matrix = new DecomposedSymmetricPositiveMatrix(3);
        CheckUtils.checkGetOutOfRangeColumn(matrix);
    }

    /**
     * Tests the methods that set a column of the matrix.<br>
     * This method should throw an exception since it is forbidden.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#setColumn(int, double[])}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#setColumnVector(int, RealVector)}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#setColumnMatrix(int, RealMatrix)}<br>
     * </p>
     */
    @Test
    public void testSetColumn() {
        final int dim = 3;
        final SymmetricPositiveMatrix matrix = new DecomposedSymmetricPositiveMatrix(dim);
        final double[] columnData = { 1., 1., 1. };
        final RealVector columnVector = new ArrayRealVector(columnData);
        final RealMatrix columnMatrix = MatrixUtils.createRowRealMatrix(columnData);

        for (int j = 0; j < dim; j++) {
            // Array
            try {
                matrix.setColumn(j, columnData);
                Assert.fail();
            } catch (final MathUnsupportedOperationException e) {
                final String expectedMessage = String.format(UNSUPPORTED_OPERATION_FORMAT);
                Assert.assertEquals(MathUnsupportedOperationException.class, e.getClass());
                Assert.assertEquals(expectedMessage, e.getMessage());
            }

            // Vector
            try {
                matrix.setColumnVector(j, columnVector);
                Assert.fail();
            } catch (final MathUnsupportedOperationException e) {
                final String expectedMessage = String.format(UNSUPPORTED_OPERATION_FORMAT);
                Assert.assertEquals(MathUnsupportedOperationException.class, e.getClass());
                Assert.assertEquals(expectedMessage, e.getMessage());
            }

            // Matrix
            try {
                matrix.setColumnMatrix(j, columnMatrix);
                Assert.fail();
            } catch (final MathUnsupportedOperationException e) {
                final String expectedMessage = String.format(UNSUPPORTED_OPERATION_FORMAT);
                Assert.assertEquals(MathUnsupportedOperationException.class, e.getClass());
                Assert.assertEquals(expectedMessage, e.getMessage());
            }
        }
    }

    /**
     * Tests the methods returning a symmetric submatrix.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#getSubMatrix(int, int, int, int)}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#getSubMatrix(int[], int[])}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#getSubMatrix(int, int)}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#getSubMatrix(int[])}<br>
     * </p>
     */
    @Test
    public void testGetSubMatrix() {
        // Tested matrix
        final double[][] initialDataBT = { { 7.30244, 0.67479, 9.45484, 9.54431 },
                { -1.92417, -3.75291, 2.70389, 8.00081 }, { 3.55877, -5.61701, -9.43691, 5.64958 },
                { 1.89309, -8.15262, 8.01884, 6.39324 }, { -9.50196, -6.90930, 5.48959, -7.89926 },
                { -3.51267, -9.57435, -0.85647, -7.65036 } };
        final SymmetricPositiveMatrix matrix = new DecomposedSymmetricPositiveMatrix(initialDataBT);

        // Test the methods
        CheckUtils.checkGetSubMatrixByRange(matrix);
        CheckUtils.checkGetSubMatrixByIndex(matrix);
        CheckUtils.checkGetSymmetricSubMatrixByRange(matrix,
                DecomposedSymmetricPositiveMatrix.class);
        CheckUtils.checkGetSymmetricSubMatrixByIndex(matrix,
                DecomposedSymmetricPositiveMatrix.class);
    }

    /**
     * Tests the methods returning a submatrix when supplying an invalid index range or array.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#getSubMatrix(int, int, int, int)}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#getSubMatrix(int[], int[])}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#getSubMatrix(int, int)}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#getSubMatrix(int[])}<br>
     * </p>
     */
    @Test
    public void testGetSubMatrixInvalidIndices() {
        final SymmetricPositiveMatrix matrix = new DecomposedSymmetricPositiveMatrix(3);
        CheckUtils.checkGetSubMatrixInvalidRange(matrix);
        CheckUtils.checkGetSubMatrixInvalidIndex(matrix);
        CheckUtils.checkGetSubMatrixNullIndex(matrix);
        CheckUtils.checkGetSubMatrixEmptyIndex(matrix);
        CheckUtils.checkGetSymmetricSubMatrixInvalidRange(matrix);
        CheckUtils.checkGetSymmetricSubMatrixInvalidIndex(matrix);
        CheckUtils.checkGetSymmetricSubMatrixNullIndex(matrix);
        CheckUtils.checkGetSymmetricSubMatrixEmptyIndex(matrix);
    }

    /**
     * Tests the method allowing to set an entire submatrix.<br>
     * The method should fail as this operation is not supported for symmetric positive
     * semi-definite matrices.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#setSubMatrix(double[][], int, int)}
     * </p>
     */
    @Test
    public void testSetSubMatrix() {
        final int dim = 4;
        final SymmetricPositiveMatrix matrix = new DecomposedSymmetricPositiveMatrix(dim);
        final double[][] subMatrix = new double[][] { { 0 } };

        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < dim; j++) {
                try {
                    matrix.setSubMatrix(subMatrix, i, j);
                    Assert.fail();
                } catch (final MathUnsupportedOperationException e) {
                    final String expectedMessage = String.format(UNSUPPORTED_OPERATION_FORMAT);
                    Assert.assertEquals(MathUnsupportedOperationException.class, e.getClass());
                    Assert.assertEquals(expectedMessage, e.getMessage());
                }
            }
        }
    }

    /**
     * Tests the method allowing to copy parts of the matrix into a given array.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#copySubMatrix(int, int, int, int, double[][])}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#copySubMatrix(int, int, int, int, double[][], int, int)}
     * <br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#copySubMatrix(int[], int[], double[][])}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#copySubMatrix(int[], int[], double[][], int, int)}
     * <br>
     * </p>
     */
    @Test
    public void testCopySubMatrix() {
        // Tested matrix
        final double[][] initialDataBT = { { -0.72411, 5.99555, -4.80884, -5.54045 },
                { 7.16270, 3.53380, -4.70465, -5.06579 }, { -9.04125, 9.10542, -8.63662, 6.14486 },
                { 0.83836, -8.42831, 7.41494, -7.74526 },
                { 2.15577, -8.04971, -7.80980, -0.15455 }, { -7.57843, -1.18468, 8.54694, 1.68318 } };
        final SymmetricPositiveMatrix matrix = new DecomposedSymmetricPositiveMatrix(initialDataBT);

        // Test the methods
        CheckUtils.checkCopySubMatrixByRange(matrix);
        CheckUtils.checkCopySubMatrixByIndex(matrix);
    }

    /**
     * Tests the methods that copies part of the matrix into a given array, using indices outside
     * the valid index range.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#copySubMatrix(int, int, int, int, double[][])}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#copySubMatrix(int, int, int, int, double[][], int, int)}
     * <br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#copySubMatrix(int[], int[], double[][])}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#copySubMatrix(int[], int[], double[][], int, int)}
     * <br>
     * </p>
     */
    @Test
    public void testCopySubMatrixInvalidIndices() {
        final RealMatrix matrix = new DecomposedSymmetricPositiveMatrix(4);
        CheckUtils.checkCopySubMatrixInvalidRange1(matrix);
        CheckUtils.checkCopySubMatrixInvalidRange2(matrix);
        CheckUtils.checkCopySubMatrixInvalidIndex1(matrix);
        CheckUtils.checkCopySubMatrixInvalidIndex2(matrix);
    }

    /**
     * Tests the methods that copies part of the matrix into a given array, using a {@code null}
     * destination array.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#copySubMatrix(int, int, int, int, double[][])}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#copySubMatrix(int, int, int, int, double[][], int, int)}
     * <br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#copySubMatrix(int[], int[], double[][])}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#copySubMatrix(int[], int[], double[][], int, int)}
     * <br>
     * </p>
     */
    @Test
    public void testCopySubMatrixNullDestinationArray() {
        final RealMatrix matrix = new DecomposedSymmetricPositiveMatrix(4);
        CheckUtils.checkCopySubMatrixNullDestinationArray(matrix);
    }

    /**
     * Tests the methods that copies part of the matrix into a given array, using a destination
     * array with no rows or columns.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#copySubMatrix(int, int, int, int, double[][])}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#copySubMatrix(int, int, int, int, double[][], int, int)}
     * <br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#copySubMatrix(int[], int[], double[][])}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#copySubMatrix(int[], int[], double[][], int, int)}
     * <br>
     * </p>
     */
    @Test
    public void testCopySubMatrixEmptyDestinationArray() {
        final RealMatrix matrix = new DecomposedSymmetricPositiveMatrix(4);
        CheckUtils.checkCopySubMatrixEmptyDestinationArray(matrix);
    }

    /**
     * Tests the methods that copies part of the matrix into a given array, using a destination
     * array too small to store the extracted
     * submatrix.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#copySubMatrix(int, int, int, int, double[][])}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#copySubMatrix(int, int, int, int, double[][], int, int)}
     * <br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#copySubMatrix(int[], int[], double[][])}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#copySubMatrix(int[], int[], double[][], int, int)}
     * <br>
     * </p>
     */
    @Test
    public void testCopySubMatrixIncompatibleDestinationArray() {
        final RealMatrix matrix = new DecomposedSymmetricPositiveMatrix(4);
        CheckUtils.checkCopySubMatrixIncompatibleDestinationArray(matrix);
    }

    /**
     * Tests the methods that concatenate two matrices horizontally.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#concatenateHorizontally(RealMatrix)}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#concatenateHorizontally(RealMatrix, boolean)}<br>
     * </p>
     */
    @Test
    public void testHorizontalConcatenation() {
        // Tested matrix
        final double[][] initialDataBT = { { 9.18909, -4.20801, -0.15241, -5.68877 },
                { 0.39440, -5.16326, -6.18019, 0.69867 }, { -8.06155, 6.86466, 3.77263, 2.60844 },
                { 0.48123, 2.17777, 7.70541, 8.83958 }, { 1.26341, 1.41407, 7.15463, -1.68519 },
                { -5.26606, -0.28157, -8.84227, 1.89891 } };
        final SymmetricPositiveMatrix matrix = new DecomposedSymmetricPositiveMatrix(initialDataBT);

        // Matrix to be concatenated with the tested matrix (4x6)
        final double[][] data = { { -5.68236, 9.91773, 4.14508, 7.19292, 3.67838, 0.83263 },
                { 5.97441, 4.85393, -8.67952, -6.85196, -1.27595, 4.74385 },
                { -2.42012, -7.08931, -7.11870, -2.08707, -5.27751, -9.31370 },
                { 5.87743, -6.55651, -6.66700, 6.71889, 7.70886, 6.43455 } };
        final RealMatrix realMatrix = new Array2DRowRealMatrix(data, false);

        // Test the methods
        CheckUtils.checkConcatenateHorizontally(matrix, realMatrix, Array2DRowRealMatrix.class);
    }

    /**
     * Tests the methods that concatenate two matrices vertically.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#concatenateVertically(RealMatrix)}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#concatenateVertically(RealMatrix, boolean)}<br>
     * </p>
     */
    @Test
    public void testVerticalConcatenation() {
        // Tested matrix
        final double[][] initialDataBT = { { -5.48033, -6.32544, -5.72733, -9.78575 },
                { -7.32086, 3.71354, 4.05019, -9.80159 }, { -6.15338, 6.09879, 3.33322, -0.99441 },
                { -9.95244, -8.62101, -2.10791, 2.33554 },
                { -5.12862, -9.66642, -8.73774, -2.86970 },
                { 4.08410, -8.03656, 3.34400, -4.07254 } };
        final SymmetricPositiveMatrix matrix = new DecomposedSymmetricPositiveMatrix(initialDataBT);

        // Matrix to be concatenated with the tested matrix (6x4)
        final double[][] data = { { -4.27506, -1.36973, -9.01850, -0.65905 },
                { 7.26219, 7.72901, 4.94743, 2.89611 }, { -2.05264, 4.53595, -6.12885, 2.23167 },
                { -0.55470, -6.86551, -8.88778, -3.23847 },
                { -7.11813, -8.50818, -0.00470, 4.09204 }, { -2.51937, 4.10553, 3.81428, -1.72379 } };
        final RealMatrix realMatrix = new Array2DRowRealMatrix(data, false);

        // Test the methods
        CheckUtils.checkConcatenateVertically(matrix, realMatrix, Array2DRowRealMatrix.class);
    }

    /**
     * Tests the methods that concatenate two matrices diagonally.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#concatenateDiagonally(RealMatrix, boolean)}<br>
     * </p>
     */
    @Test
    public void testDiagonalConcatenation() {
        // Tested matrix
        final double[][] initialDataBT = {
                { -9.22891, 9.42021, -6.81346, -8.71232, -3.07786, 6.07954 },
                { -7.02975, 2.13737, 7.16539, -1.83682, -5.67332, 7.70104 },
                { 0.74993, -0.04531, -4.25825, 7.97994, 7.21703, -5.72370 },
                { -1.14719, 3.63094, -4.17873, -3.57772, 7.84431, -1.73285 } };
        final SymmetricPositiveMatrix matrix = new DecomposedSymmetricPositiveMatrix(initialDataBT);

        // Matrix to be concatenated with the tested matrix (6x4)
        final double[][] data = { { 1.17667, -2.73517, -7.64583, -0.58530 },
                { 9.39398, 2.22780, -1.03637, 0.78027 }, { -9.15980, 3.35766, -3.45872, -7.13679 },
                { -9.44693, -1.78027, -1.78497, -9.55966 },
                { -5.81097, 1.01638, 9.32935, -2.45775 }, { 2.01852, -4.71151, -9.71197, 5.27917 } };
        final RealMatrix realMatrix = new Array2DRowRealMatrix(data, false);

        // Test the methods
        CheckUtils.checkConcatenateDiagonally(matrix, realMatrix, Array2DRowRealMatrix.class);
    }

    /**
     * Tests the methods that concatenate two matrices horizontally, using matrices with
     * incompatible dimensions.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#concatenateHorizontally(RealMatrix)}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#concatenateHorizontally(RealMatrix, boolean)}<br>
     * </p>
     */
    @Test
    public void testHorizontalConcatenationIncompatibleMatrix() {
        final RealMatrix matrix = new DecomposedSymmetricPositiveMatrix(4);
        CheckUtils.checkConcatenateHorizontallyIncompatibleMatrix(matrix);
    }

    /**
     * Tests the methods that concatenate two matrices vertically, using matrices with incompatible
     * dimensions.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#concatenateVertically(RealMatrix)}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#concatenateVertically(RealMatrix, boolean)}<br>
     * </p>
     */
    @Test
    public void testVerticalConcatenationIncompatibleMatrix() {
        final RealMatrix matrix = new DecomposedSymmetricPositiveMatrix(4);
        CheckUtils.checkConcatenateVerticallyIncompatibleMatrix(matrix);
    }

    /**
     * Tests the matrix inversion.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#getInverse()}
     * </p>
     */
    @Test
    public void testGetMatrixInverse() {
        // Tested matrix
        final double[][] initialDataBT = { { 2.41826, 3.57949, -4.01817, -5.75503 },
                { -9.54448, 8.61259, -5.39957, -1.31925 },
                { 2.70731, -3.61091, 0.57848, -2.15953 }, { 6.11637, -1.71924, 9.87211, 7.24087 },
                { 0.21406, -4.61450, 7.27913, 1.73134 }, { 7.05759, -3.39381, -3.05184, 4.28465 } };
        final SymmetricPositiveMatrix matrix = new DecomposedSymmetricPositiveMatrix(initialDataBT);

        // Test the method
        CheckUtils.checkInverseMatrix(matrix, DecomposedSymmetricPositiveMatrix.class, ABSTOL,
                RELTOL);
    }

    /**
     * Tests the inversion of a singular matrix.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#getInverse()}
     * </p>
     */
    @Test
    public void testGetSingularMatrixInverse() {
        final SymmetricPositiveMatrix matrix = new DecomposedSymmetricPositiveMatrix(4);
        CheckUtils.checkInverseSingularMatrix(matrix);
    }

    /**
     * Tests the method that sets the decomposition algorithm to use by default when computing the
     * inverse matrix.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#getDefaultDecomposition()}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#setDefaultDecomposition(Function)}<br>
     * </p>
     */
    @Test
    public void testSetDefaultDecomposition() {
        final RealMatrix matrix = new DecomposedSymmetricPositiveMatrix(4);
        CheckUtils.checkDefaultDecomposition(matrix);
    }

    /**
     * Tests the methods that returns a string representation of the matrix.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#toString()}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#toString(RealMatrixFormat)}<br>
     * </p>
     */
    @Test
    public void testToString() {
        // Tested matrix
        final double[][] initialDataBT = { { 5.32131, 4.67412, 7.47825, 3.19863 },
                { 3.20593, -0.26627, -8.61463, -1.72456 },
                { -4.18667, 0.50626, -2.62443, 0.75875 },
                { -5.04392, -4.67748, -3.08788, -8.51224 },
                { 6.03183, -7.99808, -8.29219, -5.20923 }, { 4.59761, -3.26984, -6.23332, 1.82804 } };
        final SymmetricPositiveMatrix matrix = new DecomposedSymmetricPositiveMatrix(initialDataBT);

        // Test the method
        CheckUtils.checkToString(matrix);
    }

    /**
     * Tests the methods:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#equals(Object)}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#hashCode()}
     */
    @Test
    public void testEqualsAndHashCode() {
        RealMatrix other;

        // Tested matrix
        final double[][] dataBT = { { -0.70700, 3.59611, 3.08713, 0.36277 },
                { -4.65522, 1.02714, 1.41220, 2.36950 }, { 4.82623, 4.69669, 5.71259, -5.88855 },
                { 9.55016, 6.94547, -2.91517, 1.24315 }, { -7.81462, 0.56236, 8.37625, 0.49896 },
                { -8.60807, 9.03522, 5.64325, -8.53329 } };
        final SymmetricPositiveMatrix matrix = new DecomposedSymmetricPositiveMatrix(dataBT);
        final double[][] data = matrix.getData();

        // Check the hashCode consistency between calls
        final int hashCode = matrix.hashCode();
        Assert.assertEquals(hashCode, matrix.hashCode());

        // Compared object is null
        Assert.assertFalse(matrix.equals(null));

        // Compared object is a different class
        Assert.assertFalse(matrix.equals(new Object()));

        // Same instance
        Assert.assertTrue(matrix.equals(matrix));

        // Same data, but different instances (matrix of the same type)
        other = matrix.copy();
        Assert.assertTrue(matrix.equals(other));
        Assert.assertTrue(other.equals(matrix));
        Assert.assertTrue(matrix.hashCode() == other.hashCode());

        // Same data, but different instances (matrix of a different type)
        other = new Array2DRowRealMatrix(data);
        Assert.assertTrue(matrix.equals(other));
        Assert.assertTrue(other.equals(matrix));
        Assert.assertTrue(matrix.hashCode() == other.hashCode());

        // A single element is different (matrix of the same type)
        for (int i = 0; i < matrix.getRowDimension(); i++) {
            for (int j = 0; j < matrix.getColumnDimension(); j++) {
                final double[][] modifiedData = CheckUtils.copyArray(dataBT);
                modifiedData[i][j] = 0.;

                other = new DecomposedSymmetricPositiveMatrix(modifiedData);
                Assert.assertFalse(matrix.equals(other));
                Assert.assertFalse(other.equals(matrix));
                Assert.assertFalse(matrix.hashCode() == other.hashCode());
            }
        }

        // A single element is different (matrix of a different type)
        for (int i = 0; i < matrix.getRowDimension(); i++) {
            for (int j = 0; j < matrix.getColumnDimension(); j++) {
                final double[][] modifiedData = CheckUtils.copyArray(data);
                modifiedData[i][j] = 0.;

                other = new Array2DRowRealMatrix(modifiedData);
                Assert.assertFalse(matrix.equals(other));
                Assert.assertFalse(other.equals(matrix));
                Assert.assertFalse(matrix.hashCode() == other.hashCode());
            }
        }
    }

    /**
     * Tests the method that determines if the matrix is numerically equal to another matrix, taking
     * into account the specified tolerances.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#equals(RealMatrix, double, double)}
     * </p>
     */
    @Test
    public void testEqualsWithTolerances() {
        // Tested matrix
        final double[][] dataBT = { { 3.72313, 7.60074, 8.49247, 7.11944 },
                { 3.57120, -5.87209, -6.03432, 1.29538 }, { 5.63894, -3.37557, 1.09738, 7.65967 },
                { 8.56760, -1.66633, -0.73652, 2.04267 }, { 4.55149, 2.12670, 4.55034, -3.50334 } };
        final RealMatrix matrix = new DecomposedSymmetricPositiveMatrix(dataBT);

        // Test the method
        CheckUtils.checkEqualsWithTolerances(matrix);
    }

    /**
     * Tests the serialization/deserialization.
     */
    @Test
    public void testSerialization() {
        // Tested matrix
        final double[][] dataBT = { { 0.91151, -3.02110, 3.19166, -4.55209 },
                { -3.77333, -0.40653, -3.88952, 2.82102 },
                { 9.53879, 9.66496, -3.47523, -5.16484 }, { 2.84728, -6.21960, -5.23669, 1.53752 },
                { 3.66357, 6.11402, -9.16592, 8.36192 }, { -8.26226, 7.13488, 4.26612, -5.70037 } };
        final DecomposedSymmetricPositiveMatrix matrix = new DecomposedSymmetricPositiveMatrix(
                dataBT);

        // Test the serialization
        CheckUtils.checkSerialization(matrix, DecomposedSymmetricPositiveMatrix.class);
    }

    /**
     * Tests the method allowing to visit the elements of the matrix without modifying them.
     *
     * <p>
     * Tested methods<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#walkInRowOrder(RealMatrixPreservingVisitor)}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#walkInRowOrder(RealMatrixPreservingVisitor, int, int, int, int)}
     * <br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#walkInColumnOrder(RealMatrixPreservingVisitor)}
     * <br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#walkInColumnOrder(RealMatrixPreservingVisitor, int, int, int, int)}
     * <br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#walkInOptimizedOrder(RealMatrixPreservingVisitor)}
     * <br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#walkInOptimizedOrder(RealMatrixPreservingVisitor, int, int, int, int)}
     * <br>
     * </p>
     */
    @Test
    public void testPreservingVisitor() {
        // Tested matrix
        final double[][] dataBT = { { 8.59413, -0.82765, -2.27488, -6.19003 },
                { -6.54470, 6.60456, 9.67789, -6.89020 },
                { 8.53659, -8.89078, -6.75018, -7.01317 }, { -1.22241, 1.07649, 1.23439, 5.77054 },
                { 8.47715, 0.77459, -9.68250, -9.05531 }, { -5.98448, 8.98781, 1.05188, -4.69474 } };
        final DecomposedSymmetricPositiveMatrix matrix = new DecomposedSymmetricPositiveMatrix(
                dataBT);

        // Test the methods
        CheckUtils.checkPreservingWalkInRowOrder(matrix);
        CheckUtils.checkPreservingWalkInColumnOrder(matrix);
        CheckUtils.checkPreservingWalkInOptimizedOrder(matrix);
    }

    /**
     * Tests the methods allowing to visit and modify the elements of the matrix.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#walkInRowOrder(RealMatrixChangingVisitor)}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#walkInRowOrder(RealMatrixChangingVisitor, int, int, int, int)}
     * <br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#walkInColumnOrder(RealMatrixChangingVisitor)}<br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#walkInColumnOrder(RealMatrixChangingVisitor, int, int, int, int)}
     * <br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#walkInOptimizedOrder(RealMatrixChangingVisitor)}
     * <br>
     * {@linkplain DecomposedSymmetricPositiveMatrix#walkInOptimizedOrder(RealMatrixChangingVisitor, int, int, int, int)}
     * <br>
     * </p>
     */
    @Test
    public void testChangingVisitor() {
        // Tested matrix
        final double[][] dataBT = { { 3.58467, 7.62167, -7.52576, -3.86831 },
                { 9.58140, 6.98160, 9.93863, -9.28197 }, { 2.08561, 5.00501, 9.56258, 5.32996 },
                { -7.90596, -3.66155, 7.11642, -2.83331 }, { 6.45327, 6.10653, -9.42697, 5.93498 },
                { 8.93266, -7.80235, -7.73324, -9.27952 } };
        final DecomposedSymmetricPositiveMatrix matrix = new DecomposedSymmetricPositiveMatrix(
                dataBT);

        // Visitor
        final RealMatrixChangingVisitor visitor = new DefaultRealMatrixChangingVisitor();

        // Row order
        try {
            matrix.walkInRowOrder(visitor);
            Assert.fail();
        } catch (final MathUnsupportedOperationException e) {
            final String expectedMessage = String.format(UNSUPPORTED_OPERATION_FORMAT);
            Assert.assertEquals(MathUnsupportedOperationException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.walkInRowOrder(visitor, 0, 0, 0, 0);
            Assert.fail();
        } catch (final MathUnsupportedOperationException e) {
            final String expectedMessage = String.format(UNSUPPORTED_OPERATION_FORMAT);
            Assert.assertEquals(MathUnsupportedOperationException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // Column order
        try {
            matrix.walkInColumnOrder(visitor);
            Assert.fail();
        } catch (final MathUnsupportedOperationException e) {
            final String expectedMessage = String.format(UNSUPPORTED_OPERATION_FORMAT);
            Assert.assertEquals(MathUnsupportedOperationException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.walkInColumnOrder(visitor, 0, 0, 0, 0);
            Assert.fail();
        } catch (final MathUnsupportedOperationException e) {
            final String expectedMessage = String.format(UNSUPPORTED_OPERATION_FORMAT);
            Assert.assertEquals(MathUnsupportedOperationException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // Optimized order
        try {
            matrix.walkInOptimizedOrder(visitor);
            Assert.fail();
        } catch (final MathUnsupportedOperationException e) {
            final String expectedMessage = String.format(UNSUPPORTED_OPERATION_FORMAT);
            Assert.assertEquals(MathUnsupportedOperationException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.walkInOptimizedOrder(visitor, 0, 0, 0, 0);
            Assert.fail();
        } catch (final MathUnsupportedOperationException e) {
            final String expectedMessage = String.format(UNSUPPORTED_OPERATION_FORMAT);
            Assert.assertEquals(MathUnsupportedOperationException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }
}
