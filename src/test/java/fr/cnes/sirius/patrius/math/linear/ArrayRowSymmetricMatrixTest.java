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
 */
/* 
 * HISTORY
 * VERSION:4.6:FA:FA-2542:27/01/2021:[PATRIUS] Definition d'un champ de vue avec demi-angle de 180° 
 * VERSION:4.5.1:FA:FA-2540:04/08/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear
 * VERSION:4.5:DM:DM-2300:27/05/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.linear;

import java.util.Locale;
import java.util.Random;
import java.util.function.Function;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.MathUnsupportedOperationException;
import fr.cnes.sirius.patrius.math.exception.NoDataException;
import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.linear.ArrayRowSymmetricMatrix.SymmetryType;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Unit tests for {@link ArrayRowSymmetricMatrix}.
 *
 * @author Pierre Seimandi (GMV)
 */
public class ArrayRowSymmetricMatrixTest {

    /** Default absolute tolerance used for the tests. */
    private static final double ABSTOL = 0.;

    /** Default relative tolerance used for the tests. */
    private static final double RELTOL = 1E-14;

    /** Fixed seed for the random number generator. */
    // private static final Long FIXED_SEED = null;
    private static final Long FIXED_SEED = 5132923565228263612L;

    /** Random number generator used for the tests. */
    private static final Random RNG = CheckUtils.getRandomNumberGenerator(FIXED_SEED);

    /**
     * Initialization.
     */
    @BeforeClass
    public static void init() {
        Locale.setDefault(Locale.ENGLISH);
    }

    /**
     * Tests the creation of new instances by specifying the dimension of the matrix.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain ArrayRowSymmetricMatrix#ArrayRowSymmetricMatrix(int)}
     * </p>
     */
    @Test
    public void testConstructorDimension() {
        final SymmetricMatrix matrix = new ArrayRowSymmetricMatrix(3);
        final RealMatrix expected = new Array2DRowRealMatrix(3, 3);
        CheckUtils.checkEquality(expected, matrix, 0., 0.);
    }

    /**
     * Tests the creation of new instances when the specified dimension is not valid.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain ArrayRowSymmetricMatrix#ArrayRowSymmetricMatrix(int)}
     * </p>
     */
    @Test
    public void testConstructorInvalidDimension() {
        try {
            new ArrayRowSymmetricMatrix(0);
            Assert.fail();
        } catch (final NotStrictlyPositiveException e) {
            final String expectedMessage = "invalid row dimension: 0 (must be positive)";
            Assert.assertEquals(NotStrictlyPositiveException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the creation of new instances when the provided matrix is a symmetric.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricMatrix#ArrayRowSymmetricMatrix(SymmetryType, double[][])}<br>
     * {@linkplain ArrayRowSymmetricMatrix#ArrayRowSymmetricMatrix(SymmetryType, double[][], Double, Double)}
     * <br>
     * {@linkplain ArrayRowSymmetricMatrix#ArrayRowSymmetricMatrix(SymmetryType, RealMatrix)}<br>
     * {@linkplain ArrayRowSymmetricMatrix#ArrayRowSymmetricMatrix(SymmetryType, RealMatrix, Double, Double)}
     * <br>
     * </p>
     */
    @Test
    public void testConstructorSymmetricMatrix() {
        // Perfectly symmetric matrix
        final double[][] data = { { 4.85874, -2.38509, 9.89436, 8.62236 },
                { -2.38509, 4.11961, -0.83238, 0.62608 }, { 9.89436, -0.83238, -3.51241, 1.14265 },
                { 8.62236, 0.62608, 1.14265, 3.41791 } };
        final RealMatrix realMatrix = new Array2DRowRealMatrix(data);

        SymmetricMatrix matrix;

        // Test every type of symmetry
        for (final SymmetryType symmetryType : SymmetryType.values()) {
            // From 2D-array
            matrix = new ArrayRowSymmetricMatrix(symmetryType, data);
            CheckUtils.checkEquality(realMatrix, matrix, 0., 0.);

            // From 2D-array with null symmetry thresholds
            matrix = new ArrayRowSymmetricMatrix(symmetryType, data, null, null);
            CheckUtils.checkEquality(realMatrix, matrix, 0., 0.);

            // From 2D-array with non-null symmetry thresholds
            matrix = new ArrayRowSymmetricMatrix(symmetryType, data, 0., 0.);
            CheckUtils.checkEquality(realMatrix, matrix, 0., 0.);

            // From matrix
            matrix = new ArrayRowSymmetricMatrix(symmetryType, realMatrix);
            CheckUtils.checkEquality(realMatrix, matrix, 0., 0.);

            // From matrix with null symmetry thresholds
            matrix = new ArrayRowSymmetricMatrix(symmetryType, realMatrix, null, null);
            CheckUtils.checkEquality(realMatrix, matrix, 0., 0.);

            // From matrix with non-null symmetry thresholds
            matrix = new ArrayRowSymmetricMatrix(symmetryType, realMatrix, 0., 0.);
            CheckUtils.checkEquality(realMatrix, matrix, 0., 0.);
        }
    }

    /**
     * Tests the creation of new instances when the provided matrix is not symmetric.<br>
     * The symmetry should be enforced by taking into account the lower/upper triangular elements
     * only, or by taking their mean.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricMatrix#ArrayRowSymmetricMatrix(SymmetryType, double[][])}<br>
     * {@linkplain ArrayRowSymmetricMatrix#ArrayRowSymmetricMatrix(SymmetryType, double[][], Double, Double)}
     * <br>
     * {@linkplain ArrayRowSymmetricMatrix#ArrayRowSymmetricMatrix(SymmetryType, RealMatrix)}<br>
     * {@linkplain ArrayRowSymmetricMatrix#ArrayRowSymmetricMatrix(SymmetryType, RealMatrix, Double, Double)}
     * <br>
     * </p>
     */
    @Test
    public void testConstructorAsymmetricMatrix() {
        // Asymmetric matrix
        final double[][] initialData = { { 3, 4, 0 }, { 2, 5, -6 }, { 0, -3, 7 } };

        double[][] data;
        RealMatrix expected;
        SymmetricMatrix matrix;

        // Lower symmetry
        data = new double[][] { { 3, 2, 0 }, { 2, 5, -3 }, { 0, -3, 7 } };
        expected = new Array2DRowRealMatrix(data);

        matrix = new ArrayRowSymmetricMatrix(SymmetryType.LOWER, initialData);
        CheckUtils.checkEquality(expected, matrix, 0., 0.);

        matrix = new ArrayRowSymmetricMatrix(SymmetryType.LOWER, initialData,
                Double.POSITIVE_INFINITY, null);
        CheckUtils.checkEquality(expected, matrix, 0., 0.);

        matrix = new ArrayRowSymmetricMatrix(SymmetryType.LOWER, initialData, null,
                Double.POSITIVE_INFINITY);
        CheckUtils.checkEquality(expected, matrix, 0., 0.);

        // Upper symmetry
        data = new double[][] { { 3, 4, 0 }, { 4, 5, -6 }, { 0, -6, 7 } };
        expected = new Array2DRowRealMatrix(data);

        matrix = new ArrayRowSymmetricMatrix(SymmetryType.UPPER, initialData);
        CheckUtils.checkEquality(expected, matrix, 0., 0.);

        matrix = new ArrayRowSymmetricMatrix(SymmetryType.UPPER, initialData,
                Double.POSITIVE_INFINITY, null);
        CheckUtils.checkEquality(expected, matrix, 0., 0.);

        matrix = new ArrayRowSymmetricMatrix(SymmetryType.UPPER, initialData, null,
                Double.POSITIVE_INFINITY);
        CheckUtils.checkEquality(expected, matrix, 0., 0.);

        // Mean symmetry
        data = new double[][] { { 3, 3, 0 }, { 3, 5, -4.5 }, { 0, -4.5, 7 } };
        expected = new Array2DRowRealMatrix(data);

        matrix = new ArrayRowSymmetricMatrix(SymmetryType.MEAN, initialData);
        CheckUtils.checkEquality(expected, matrix, 0., 0.);

        matrix = new ArrayRowSymmetricMatrix(SymmetryType.MEAN, initialData,
                Double.POSITIVE_INFINITY, null);
        CheckUtils.checkEquality(expected, matrix, 0., 0.);

        matrix = new ArrayRowSymmetricMatrix(SymmetryType.MEAN, initialData, null,
                Double.POSITIVE_INFINITY);
        CheckUtils.checkEquality(expected, matrix, 0., 0.);
    }

    /**
     * Tests the creation of new instances when the provided data array is {@code null}.<br>
     * An exception should be thrown.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricMatrix#ArrayRowSymmetricMatrix(SymmetryType, double[][])}<br>
     * {@linkplain ArrayRowSymmetricMatrix#ArrayRowSymmetricMatrix(SymmetryType, double[][], Double, Double)}
     * <br>
     * </p>
     */
    @Test
    public void testConstructorNullDataArray() {
        final double[][] nullData = null;
        final String expectedMessage = "the supplied array is null";

        // Test every type of symmetry
        for (final SymmetryType symmetryType : SymmetryType.values()) {
            try {
                new ArrayRowSymmetricMatrix(symmetryType, nullData);
                Assert.fail();
            } catch (final NullArgumentException e) {
                Assert.assertEquals(NullArgumentException.class, e.getClass());
                Assert.assertEquals(expectedMessage, e.getMessage());
            }

            try {
                new ArrayRowSymmetricMatrix(symmetryType, nullData, null, null);
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
     * {@linkplain ArrayRowSymmetricMatrix#ArrayRowSymmetricMatrix(SymmetryType, double[][])}<br>
     * {@linkplain ArrayRowSymmetricMatrix#ArrayRowSymmetricMatrix(SymmetryType, double[][], Double, Double)}
     * <br>
     * </p>
     */
    @Test
    public void testConstructorEmptyDataArray() {
        final double[][] emptyRowData = new double[0][0];
        final double[][] emptyColumnData = new double[1][0];
        final String expectedMessage1 = "matrix must have at least one row";
        final String expectedMessage2 = "matrix must have at least one column";

        // Test every type of symmetry
        for (final SymmetryType symmetryType : SymmetryType.values()) {
            // Zero rows and columns
            try {
                new ArrayRowSymmetricMatrix(symmetryType, emptyRowData);
                Assert.fail();
            } catch (final NoDataException e) {
                Assert.assertEquals(NoDataException.class, e.getClass());
                Assert.assertEquals(expectedMessage1, e.getMessage());
            }

            try {
                new ArrayRowSymmetricMatrix(symmetryType, emptyRowData, null, null);
                Assert.fail();
            } catch (final NoDataException e) {
                Assert.assertEquals(NoDataException.class, e.getClass());
                Assert.assertEquals(expectedMessage1, e.getMessage());
            }

            // One empty row
            try {
                new ArrayRowSymmetricMatrix(symmetryType, emptyColumnData);
                Assert.fail();
            } catch (final NoDataException e) {
                Assert.assertEquals(NoDataException.class, e.getClass());
                Assert.assertEquals(expectedMessage2, e.getMessage());
            }

            try {
                new ArrayRowSymmetricMatrix(symmetryType, emptyColumnData, null, null);
                Assert.fail();
            } catch (final NoDataException e) {
                Assert.assertEquals(NoDataException.class, e.getClass());
                Assert.assertEquals(expectedMessage2, e.getMessage());
            }
        }
    }

    /**
     * Tests the creation of new instances when the provided data array is not valid.<br>
     * An exception should be thrown.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricMatrix#ArrayRowSymmetricMatrix(SymmetryType, double[][])}<br>
     * {@linkplain ArrayRowSymmetricMatrix#ArrayRowSymmetricMatrix(SymmetryType, double[][], Double, Double)}
     * <br>
     * </p>
     */
    @Test
    public void testConstructorInvalidDataArray() {
        final double[][] invalidData = { { 1. }, { 2., 3. } };
        final String expectedMessage = "2 != 1";

        // Test every type of symmetry
        for (final SymmetryType symmetryType : SymmetryType.values()) {
            // Two rows of different dimensions
            try {
                new ArrayRowSymmetricMatrix(symmetryType, invalidData);
                Assert.fail();
            } catch (final DimensionMismatchException e) {
                Assert.assertEquals(DimensionMismatchException.class, e.getClass());
                Assert.assertEquals(expectedMessage, e.getMessage());
            }

            try {
                new ArrayRowSymmetricMatrix(symmetryType, invalidData, null, null);
                Assert.fail();
            } catch (final DimensionMismatchException e) {
                Assert.assertEquals(DimensionMismatchException.class, e.getClass());
                Assert.assertEquals(expectedMessage, e.getMessage());
            }
        }
    }

    /**
     * Tests the creation of new instances when the provided matrix is {@code null}.<br>
     * An exception should be thrown.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricMatrix#ArrayRowSymmetricMatrix(SymmetryType, RealMatrix)}<br>
     * {@linkplain ArrayRowSymmetricMatrix#ArrayRowSymmetricMatrix(SymmetryType, RealMatrix, Double, Double)}
     * <br>
     * </p>
     */
    @Test
    public void testConstructorNullMatrix() {
        final RealMatrix nullMatrix = null;
        final String expectedMessage = "the supplied matrix is null";

        // Test every type of symmetry
        for (final SymmetryType symmetryType : SymmetryType.values()) {
            try {
                new ArrayRowSymmetricMatrix(symmetryType, nullMatrix);
                Assert.fail();
            } catch (final NullArgumentException e) {
                Assert.assertEquals(NullArgumentException.class, e.getClass());
                Assert.assertEquals(expectedMessage, e.getMessage());
            }

            try {
                new ArrayRowSymmetricMatrix(symmetryType, nullMatrix, null, null);
                Assert.fail();
            } catch (final NullArgumentException e) {
                Assert.assertEquals(NullArgumentException.class, e.getClass());
                Assert.assertEquals(expectedMessage, e.getMessage());
            }
        }
    }

    /**
     * Tests the creation of new instances when the provided matrix is not a square matrix.<br>
     * An exception should be thrown.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricMatrix#ArrayRowSymmetricMatrix(SymmetryType, double[][])}<br>
     * {@linkplain ArrayRowSymmetricMatrix#ArrayRowSymmetricMatrix(SymmetryType, double[][], Double, Double)}
     * <br>
     * {@linkplain ArrayRowSymmetricMatrix#ArrayRowSymmetricMatrix(SymmetryType, RealMatrix)}<br>
     * {@linkplain ArrayRowSymmetricMatrix#ArrayRowSymmetricMatrix(SymmetryType, RealMatrix, Double, Double)}
     * <br>
     * </p>
     */
    @Test
    public void testConstructorNonSquareMatrix() {
        final double[][] nonSquareData = { { 1, -1 }, { -1, 1 }, { 0, 0 } };
        final RealMatrix nonSquareMatrix = new Array2DRowRealMatrix(nonSquareData);
        final String expectedMessage = "non square (2x3) matrix";

        // Test every type of symmetry
        for (final SymmetryType symmetryType : SymmetryType.values()) {
            try {
                new ArrayRowSymmetricMatrix(symmetryType, nonSquareData);
                Assert.fail();
            } catch (final NonSquareMatrixException e) {
                Assert.assertEquals(NonSquareMatrixException.class, e.getClass());
                Assert.assertEquals(expectedMessage, e.getMessage());
            }

            try {
                new ArrayRowSymmetricMatrix(symmetryType, nonSquareData, null, null);
                Assert.fail();
            } catch (final NonSquareMatrixException e) {
                Assert.assertEquals(NonSquareMatrixException.class, e.getClass());
                Assert.assertEquals(expectedMessage, e.getMessage());
            }

            try {
                new ArrayRowSymmetricMatrix(symmetryType, nonSquareMatrix);
                Assert.fail();
            } catch (final NonSquareMatrixException e) {
                Assert.assertEquals(NonSquareMatrixException.class, e.getClass());
                Assert.assertEquals(expectedMessage, e.getMessage());
            }

            try {
                new ArrayRowSymmetricMatrix(symmetryType, nonSquareMatrix, null, null);
                Assert.fail();
            } catch (final NonSquareMatrixException e) {
                Assert.assertEquals(NonSquareMatrixException.class, e.getClass());
                Assert.assertEquals(expectedMessage, e.getMessage());
            }
        }
    }

    /**
     * Tests the creation of new instances when the provided lower elements data array is
     * {@code null}.<br>
     * An exception should be thrown.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain ArrayRowSymmetricMatrix#ArrayRowSymmetricMatrix(double[], boolean)}<br>
     * </p>
     */
    @Test
    public void testConstructorNullLowerElementsDataArray() {
        final double[] nullData = null;
        final String expectedMessage = "the supplied array is null";

        try {
            new ArrayRowSymmetricMatrix(nullData, true);
            Assert.fail();
        } catch (final NullArgumentException e) {
            Assert.assertEquals(NullArgumentException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            new ArrayRowSymmetricMatrix(nullData, false);
            Assert.fail();
        } catch (final NullArgumentException e) {
            Assert.assertEquals(NullArgumentException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the creation of new instances when the provided lower elements data array is empty.<br>
     * An exception should be thrown.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain ArrayRowSymmetricMatrix#ArrayRowSymmetricMatrix(double[], boolean)}<br>
     * </p>
     */
    @Test
    public void testConstructorEmptyLowerElementsDataArray() {
        final double[] emptyData = new double[0];
        final String expectedMessage = "the supplied array is empty";

        try {
            new ArrayRowSymmetricMatrix(emptyData, true);
            Assert.fail();
        } catch (final NoDataException e) {
            Assert.assertEquals(NoDataException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            new ArrayRowSymmetricMatrix(emptyData, false);
            Assert.fail();
        } catch (final NoDataException e) {
            Assert.assertEquals(NoDataException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the creation of new instances when the specified absolute or relative symmetry
     * threshold are {@code NaN}.<br>
     * An exception should be thrown.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricMatrix#ArrayRowSymmetricMatrix(SymmetryType, double[][], Double, Double)}
     * <br>
     * {@linkplain ArrayRowSymmetricMatrix#ArrayRowSymmetricMatrix(SymmetryType, RealMatrix, Double, Double)}
     * <br>
     * </p>
     */
    @Test
    public void testConstructorNaNSymmetryThresholds() {
        final double[][] data = { { -0.00541, -3.56403, 7.64121, 0.59878 },
                { -3.56403, -4.09599, 1.09747, -9.72741 },
                { 7.64121, 1.09747, -4.92337, -2.59036 }, { 0.59878, -9.72741, -2.59036, 9.72538 } };
        final RealMatrix matrix = new Array2DRowRealMatrix(data);
        final String expectedMessage = "Input threshold is NaN.";

        for (final SymmetryType symmetryType : SymmetryType.values()) {
            // NaN absolute symmetry threshold
            try {
                new ArrayRowSymmetricMatrix(symmetryType, data, Double.NaN, 0.);
                Assert.fail();
            } catch (final IllegalArgumentException e) {
                Assert.assertEquals(IllegalArgumentException.class, e.getClass());
                Assert.assertEquals(expectedMessage, e.getMessage());
            }

            try {
                new ArrayRowSymmetricMatrix(symmetryType, matrix, Double.NaN, 0.);
                Assert.fail();
            } catch (final IllegalArgumentException e) {
                Assert.assertEquals(IllegalArgumentException.class, e.getClass());
                Assert.assertEquals(expectedMessage, e.getMessage());
            }

            // NaN relative symmetry threshold
            try {
                new ArrayRowSymmetricMatrix(symmetryType, data, 0., Double.NaN);
                Assert.fail();
            } catch (final IllegalArgumentException e) {
                Assert.assertEquals(IllegalArgumentException.class, e.getClass());
                Assert.assertEquals(expectedMessage, e.getMessage());
            }

            try {
                new ArrayRowSymmetricMatrix(symmetryType, matrix, 0., Double.NaN);
                Assert.fail();
            } catch (final IllegalArgumentException e) {
                Assert.assertEquals(IllegalArgumentException.class, e.getClass());
                Assert.assertEquals(expectedMessage, e.getMessage());
            }
        }
    }

    /**
     * Tests the creation of new instances when the specified absolute or relative symmetry
     * threshold are negative.<br>
     * An exception should be thrown.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricMatrix#ArrayRowSymmetricMatrix(SymmetryType, double[][], Double, Double)}
     * <br>
     * {@linkplain ArrayRowSymmetricMatrix#ArrayRowSymmetricMatrix(SymmetryType, RealMatrix, Double, Double)}
     * <br>
     * </p>
     */
    @Test
    public void testConstructorNegativeSymmetryThresholds() {
        final double[][] data = { { 0.45226, 8.80309, 4.95663, 3.81678 },
                { 8.80309, 3.85258, 1.31830, 6.16920 }, { 4.95663, 1.31830, -0.60544, -1.31199 },
                { 3.81678, 6.16920, -1.31199, -8.42421 } };
        final RealMatrix realMatrix = new Array2DRowRealMatrix(data);
        final String expectedMessage1 = "Absolute threshold is not positive or null.";
        final String expectedMessage2 = "Relative threshold is not positive or null.";

        for (final SymmetryType symmetryType : SymmetryType.values()) {
            // Negative absolute symmetry threshold
            try {
                new ArrayRowSymmetricMatrix(symmetryType, data, -Double.MIN_VALUE, 0.);
                Assert.fail();
            } catch (final IllegalArgumentException e) {
                Assert.assertEquals(IllegalArgumentException.class, e.getClass());
                Assert.assertEquals(expectedMessage1, e.getMessage());
            }

            try {
                new ArrayRowSymmetricMatrix(symmetryType, realMatrix, -Double.MIN_VALUE, 0.);
                Assert.fail();
            } catch (final IllegalArgumentException e) {
                Assert.assertEquals(IllegalArgumentException.class, e.getClass());
                Assert.assertEquals(expectedMessage1, e.getMessage());
            }

            // Negative relative symmetry threshold
            try {
                new ArrayRowSymmetricMatrix(symmetryType, data, 0., -Double.MIN_VALUE);
                Assert.fail();
            } catch (final IllegalArgumentException e) {
                Assert.assertEquals(IllegalArgumentException.class, e.getClass());
                Assert.assertEquals(expectedMessage2, e.getMessage());
            }

            try {
                new ArrayRowSymmetricMatrix(symmetryType, realMatrix, 0., -Double.MIN_VALUE);
                Assert.fail();
            } catch (final IllegalArgumentException e) {
                Assert.assertEquals(IllegalArgumentException.class, e.getClass());
                Assert.assertEquals(expectedMessage2, e.getMessage());
            }
        }
    }

    /**
     * Tests the creation of new matrices of the same type by specifying the row and column
     * dimensions.<br>
     * An exception should be thrown when the specified row and columns dimensions are not equal.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricMatrix#createMatrix(int, int)}
     * </p>
     */
    @Test
    public void testCreateMatrix() {
        final int dim = 4;
        final ArrayRowSymmetricMatrix matrix = new ArrayRowSymmetricMatrix(dim);
        final ArrayRowSymmetricMatrix result = matrix.createMatrix(dim, dim);

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
     * Tests the method that creates a new identity matrix.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain ArrayRowSymmetricMatrix#createIdentityMatrix(int)}
     * </p>
     */
    @Test
    public void testCreateIdentityMatrix() {
        for (int dim = 1; dim <= 10; dim++) {
            final ArrayRowSymmetricMatrix matrix = ArrayRowSymmetricMatrix
                    .createIdentityMatrix(dim);
            final RealMatrix expected = MatrixUtils.createRealIdentityMatrix(dim);
            CheckUtils.checkEquality(expected, matrix, ABSTOL, RELTOL);
        }

        // Invalid dimension
        try {
            ArrayRowSymmetricMatrix.createIdentityMatrix(0);
            Assert.fail();
        } catch (final NotStrictlyPositiveException e) {
            final String expectedMessage = "invalid row dimension: 0 (must be positive)";
            Assert.assertEquals(NotStrictlyPositiveException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the method that determines if a matrix is square.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain ArrayRowSymmetricMatrix#isSquare()}
     * </p>
     */
    @Test
    public void testIsSquare() {
        SymmetricMatrix matrix;

        // Matrix filled with zeros
        matrix = new ArrayRowSymmetricMatrix(8);
        Assert.assertTrue(matrix.isSquare());

        // Standard matrix
        final double[][] data = { { -8.39480, -1.85591, -5.78436, -4.55609 },
                { -1.85591, -0.33647, 0.23367, 6.72020 }, { -5.78436, 0.23367, 1.86506, 2.87025 },
                { -4.55609, 6.72020, 2.87025, -2.86169 } };
        matrix = new ArrayRowSymmetricMatrix(SymmetryType.LOWER, data, 0., 0.);
        Assert.assertTrue(matrix.isSquare());
    }

    /**
     * Tests the method that determines if a matrix is diagonal.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricMatrix#isDiagonal(double)}<br>
     * </p>
     */
    @Test
    public void testIsDiagonal() {
        SymmetricMatrix matrix;

        // Matrix filled with zeros
        matrix = new ArrayRowSymmetricMatrix(8);
        Assert.assertTrue(matrix.isDiagonal(0.));

        // Diagonal matrix, except for one off-diagonal element
        final double[][] data = { { 2, 0., 0. }, { -1E-2, 2, 0. }, { 0, -1E-2, 5 } };
        matrix = new ArrayRowSymmetricMatrix(SymmetryType.LOWER, data, null, null);
        final double diagonalityThreshold = 1E-2;
        Assert.assertFalse(matrix.isDiagonal(0.));
        Assert.assertFalse(matrix.isDiagonal(diagonalityThreshold
                - MathLib.ulp(diagonalityThreshold)));
        Assert.assertTrue(matrix.isDiagonal(diagonalityThreshold));
    }

    /**
     * Tests the methods that determine if a matrix is symmetric.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricMatrix#isSymmetric()}<br>
     * {@linkplain ArrayRowSymmetricMatrix#isSymmetric(double)}<br>
     * {@linkplain ArrayRowSymmetricMatrix#isSymmetric(double, double)}<br>
     * </p>
     */
    @Test
    public void testIsSymmetric() {
        SymmetricMatrix matrix;
        final double infinity = Double.POSITIVE_INFINITY;

        // Matrix filled with zeros
        matrix = new ArrayRowSymmetricMatrix(8);
        Assert.assertTrue(matrix.isSymmetric());
        Assert.assertTrue(matrix.isSymmetric(0.));
        Assert.assertTrue(matrix.isSymmetric(0., 0.));
        Assert.assertTrue(matrix.isSymmetric(-infinity));
        Assert.assertTrue(matrix.isSymmetric(-infinity, -infinity));

        // Standard matrix
        final double[][] data = { { 2, 0., 0. }, { -1, 2, 0. }, { 0, -1, 5 } };
        matrix = new ArrayRowSymmetricMatrix(SymmetryType.LOWER, data, null, null);
        Assert.assertTrue(matrix.isSymmetric());
        Assert.assertTrue(matrix.isSymmetric(0.));
        Assert.assertTrue(matrix.isSymmetric(0., 0.));
        Assert.assertTrue(matrix.isSymmetric(-infinity));
        Assert.assertTrue(matrix.isSymmetric(-infinity, -infinity));
    }

    /**
     * Tests the method that determines if a matrix is antisymmetric.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricMatrix#isAntisymmetric(double, double)}<br>
     * </p>
     */
    @Test
    public void testIsAntisymmetric() {
        double[][] data;
        SymmetricMatrix matrix;

        // Matrix filled with zeros
        matrix = new ArrayRowSymmetricMatrix(8);
        Assert.assertTrue(matrix.isAntisymmetric(0., 0.));

        // Standard matrix
        data = new double[][] { { 2, 0., 0. }, { -1, 2, 0. }, { 0, -1, 5 } };
        matrix = new ArrayRowSymmetricMatrix(SymmetryType.LOWER, data, null, null);
        Assert.assertFalse(matrix.isAntisymmetric(0., 0.));

        // The off-diagonal elements are equal to zero, but not the diagonal elements
        data = new double[][] { { 1E-10, 0., 0. }, { 0., 1E-8, 0. }, { 0., 0., 1E-6 } };
        matrix = new ArrayRowSymmetricMatrix(SymmetryType.LOWER, data, null, null);
        Assert.assertFalse(matrix.isAntisymmetric(0., 0.));
        Assert.assertFalse(matrix.isAntisymmetric(0., 1E-6 - MathLib.ulp(1E-6)));
        Assert.assertTrue(matrix.isAntisymmetric(0., 1E-6));
        Assert.assertFalse(matrix.isAntisymmetric(1E-6, 0.));

        // The diagonal elements are equal to zero, but not the off-diagonal elements
        data = new double[][] { { 0., 1E-10, 0. }, { 1E-8, 0., 0. }, { 1E-11, 1E-7, 0. } };
        matrix = new ArrayRowSymmetricMatrix(SymmetryType.LOWER, data, null, null);
        Assert.assertFalse(matrix.isAntisymmetric(0., 0.));
        Assert.assertFalse(matrix.isAntisymmetric(0., 2E-7 - MathLib.ulp(2E-7)));
        Assert.assertTrue(matrix.isAntisymmetric(0., 2E-7));
        Assert.assertFalse(matrix.isAntisymmetric(2. - MathLib.ulp(2.), 0.));
        Assert.assertTrue(matrix.isAntisymmetric(2., 0.));
    }

    /**
     * Tests the method that determines if a matrix is orthogonal.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain ArrayRowSymmetricMatrix#isOrthogonal(double, double)}<br>
     * </p>
     */
    @Test
    public void testIsOrthogonal() {
        double[][] data;
        SymmetricMatrix matrix;

        matrix = new ArrayRowSymmetricMatrix(8);
        Assert.assertFalse(matrix.isOrthogonal(0., 0.));

        // Perfectly orthogonal matrix
        // (Householder transformation matrix)
        data = new double[4][4];
        data[0][0] = 1.;
        data[1][1] = -1. / 3.;
        data[2][1] = +2. / 3.;
        data[2][2] = +2. / 3.;
        data[3][1] = -2. / 3.;
        data[3][2] = +1. / 3.;
        data[3][3] = +2. / 3.;
        final SymmetricMatrix householderMatrix = new ArrayRowSymmetricMatrix(SymmetryType.LOWER,
                data, null, null);
        Assert.assertTrue(householderMatrix.isOrthogonal(0., 0.));

        // The norm of the first row is slightly different from 1
        data = householderMatrix.getData();
        data[0][0] += 1E-8;
        matrix = new ArrayRowSymmetricMatrix(SymmetryType.LOWER, data, null, null);

        final double norm = MathLib.sqrt(MathLib.pow(1 + 1E-8, 2.));
        final double normThreshold = MathLib.abs(MathLib.divide(1. - norm, MathLib.max(1., norm)));

        Assert.assertFalse(matrix.isOrthogonal(0., 0.));
        Assert.assertFalse(matrix.isOrthogonal(normThreshold - MathLib.ulp(normThreshold), 0.));
        Assert.assertTrue(matrix.isOrthogonal(normThreshold, 0.));

        // Some of the rows are not perfectly orthogonal
        data = householderMatrix.getData();
        data[1][1] += 1E-8;
        data[2][1] += 2E-7;
        data[2][2] += 4E-9;
        data[3][1] += 3E-8;
        data[3][2] += 8E-7;
        data[3][3] += 8E-9;
        matrix = new ArrayRowSymmetricMatrix(SymmetryType.LOWER, data, null, null);

        final RealVector column1 = matrix.getColumnVector(1);
        final RealVector column2 = matrix.getColumnVector(2);
        final RealVector column3 = matrix.getColumnVector(3);

        final double infinity = Double.POSITIVE_INFINITY;
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
     * Tests the symmetry checks performed by the constructor when symmetry thresholds are
     * specified.<br>
     * The matrix should be built without error if the symmetry thresholds are large enough,
     * otherwise an exception should be thrown.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricMatrix#ArrayRowSymmetricMatrix(SymmetryType, double[][], Double, Double)}
     * <br>
     * {@linkplain ArrayRowSymmetricMatrix#ArrayRowSymmetricMatrix(SymmetryType, RealMatrix, Double, Double)}
     * <br>
     * </p>
     */
    @Test
    public void testSymmetryChecks() {
        // Slightly asymmetric matrix
        final double[][] data = { { 10., 0.999, 0 }, { 1.001, 2, 1.001 }, { 0, 0.999, 2 } };
        final RealMatrix realMatrix = new Array2DRowRealMatrix(data);

        // Max absolute and relative symmetry thresholds this matrix would satisfy
        final double absoluteThreshold = 2E-3;
        final double relativeThreshold = 2E-3 / 1.001;

        for (final SymmetryType symmetryType : SymmetryType.values()) {
            // The absolute symmetry threshold is too low,
            // but the relative symmetry threshold is high enough.
            new ArrayRowSymmetricMatrix(symmetryType, data, absoluteThreshold - 1E-14,
                    relativeThreshold);
            new ArrayRowSymmetricMatrix(symmetryType, realMatrix, absoluteThreshold - 1E-14,
                    relativeThreshold);

            // The relative symmetry threshold is too low,
            // but the absolute symmetry threshold is high enough.
            new ArrayRowSymmetricMatrix(symmetryType, data, absoluteThreshold,
                    relativeThreshold - 1E-14);
            new ArrayRowSymmetricMatrix(symmetryType, realMatrix, absoluteThreshold,
                    relativeThreshold - 1E-14);

            // The absolute symmetry threshold is too low
            // and no relative symmetry threshold is specified.
            try {
                new ArrayRowSymmetricMatrix(symmetryType, data, absoluteThreshold - 1E-14, null);
                Assert.fail();
            } catch (final NonSymmetricMatrixException e) {
                final String expectedMessage = "non symmetric matrix: the difference between entries at (0,0) and (0,0) is larger than 0";
                Assert.assertEquals(NonSymmetricMatrixException.class, e.getClass());
                Assert.assertEquals(expectedMessage, e.getMessage());
            }

            try {
                new ArrayRowSymmetricMatrix(symmetryType, realMatrix, absoluteThreshold - 1E-14,
                        null);
                Assert.fail();
            } catch (final NonSymmetricMatrixException e) {
                final String expectedMessage = "non symmetric matrix: the difference between entries at (0,0) and (0,0) is larger than 0";
                Assert.assertEquals(NonSymmetricMatrixException.class, e.getClass());
                Assert.assertEquals(expectedMessage, e.getMessage());
            }

            // The relative symmetry threshold is too low
            // and no absolute symmetry threshold is specified.
            try {
                new ArrayRowSymmetricMatrix(symmetryType, data, null, relativeThreshold - 1E-14);
                Assert.fail();
            } catch (final NonSymmetricMatrixException e) {
                final String expectedMessage = "non symmetric matrix: the difference between entries at (0,0) and (0,0) is larger than 0.002";
                Assert.assertEquals(NonSymmetricMatrixException.class, e.getClass());
                Assert.assertEquals(expectedMessage, e.getMessage());
            }

            try {
                new ArrayRowSymmetricMatrix(symmetryType, realMatrix, null,
                        relativeThreshold - 1E-14);
                Assert.fail();
            } catch (final NonSymmetricMatrixException e) {
                final String expectedMessage = "non symmetric matrix: the difference between entries at (0,0) and (0,0) is larger than 0.002";
                Assert.assertEquals(NonSymmetricMatrixException.class, e.getClass());
                Assert.assertEquals(expectedMessage, e.getMessage());
            }
        }
    }

    /**
     * Tests the method that returns the minimum value in the matrix.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain ArrayRowSymmetricMatrix#getMin()}
     * </p>
     */
    @Test
    public void testGetMin() {
        // Tested matrix
        final double[][] data = { { 4.71721, -9.88502, -8.27018, -8.59054 },
                { -9.88502, -2.07607, 7.63590, -9.63973 },
                { -8.27018, 7.63590, -1.65233, -2.78138 },
                { -8.59054, -9.63973, -2.78138, 6.28767 } };
        final SymmetricMatrix matrix = new ArrayRowSymmetricMatrix(SymmetryType.LOWER, data, 0., 0.);

        // Test the method
        CheckUtils.checkMinimum(matrix, 0., 0.);
        Assert.assertEquals(-9.88502, matrix.getMin(), 0.);
    }

    /**
     * Tests the method that returns the maximum value in the matrix.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain ArrayRowSymmetricMatrix#getMax()}
     * </p>
     */
    @Test
    public void testGetMax() {
        // Tested matrix
        final double[][] data = { { -0.49160, -1.54140, -8.33799, -8.51012 },
                { -1.54140, 9.45137, 3.30003, 8.85154 }, { -8.33799, 3.30003, 3.80212, -1.16059 },
                { -8.51012, 8.85154, -1.16059, -4.05650 } };
        final SymmetricMatrix matrix = new ArrayRowSymmetricMatrix(SymmetryType.LOWER, data, 0., 0.);

        // Test the method
        CheckUtils.checkMaximum(matrix, 0., 0.);
        Assert.assertEquals(9.45137, matrix.getMax(), 0.);
    }

    /**
     * Tests the method that computes the trace of the matrix.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain ArrayRowSymmetricMatrix#getTrace()}
     * </p>
     */
    @Test
    public void testGetTrace() {
        // Tested matrix
        final double[][] data = { { 1.53760, -7.04646, -9.61996, 0.23297 },
                { -7.04646, 6.37612, 0.58037, 4.15963 }, { -9.61996, 0.58037, 8.32372, -6.34288 },
                { 0.23297, 4.15963, -6.34288, -6.82713 } };
        final SymmetricMatrix matrix = new ArrayRowSymmetricMatrix(SymmetryType.LOWER, data, 0., 0.);

        // Test the method
        CheckUtils.checkTrace(matrix, 0., 0.);
        CheckUtils.checkEquality(9.41031, matrix.getTrace(), ABSTOL, RELTOL);
    }

    /**
     * Tests the method that computes the norm of the matrix (maximum absolute row sum).
     *
     * <p>
     * Tested method:<br>
     * {@linkplain ArrayRowSymmetricMatrix#getNorm()}
     * </p>
     */
    @Test
    public void testGetNorm() {
        // Tested matrix
        final double[][] data = { { -5.34608, -7.88066, -0.62624, -5.93451 },
                { -7.88066, -5.48250, -6.83505, -8.45921 },
                { -0.62624, -6.83505, -2.64313, -1.97587 },
                { -5.93451, -8.45921, -1.97587, 1.85866 } };
        final SymmetricMatrix matrix = new ArrayRowSymmetricMatrix(SymmetryType.LOWER, data, 0., 0.);

        // Test the method
        CheckUtils.checkNorm(matrix, ABSTOL, RELTOL);
        CheckUtils.checkEquality(28.65742, matrix.getNorm(), ABSTOL, RELTOL);
    }

    /**
     * Tests the method that computes the Frobenius norm of the matrix.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain ArrayRowSymmetricMatrix#getFrobeniusNorm()}
     * </p>
     */
    @Test
    public void testGetFrobeniusNorm() {
        // Tested matrix
        final double[][] data = { { -2.02511, -6.22665, 3.58945, 5.85038 },
                { -6.22665, 3.02200, -4.93961, 3.67822 }, { 3.58945, -4.93961, -3.07986, 5.69168 },
                { 5.85038, 3.67822, 5.69168, -3.83799 } };
        final SymmetricMatrix matrix = new ArrayRowSymmetricMatrix(SymmetryType.LOWER, data, 0., 0.);

        // Test the method
        CheckUtils.checkFrobeniusNorm(matrix, ABSTOL, RELTOL);
        CheckUtils.checkEquality(18.70460734141190, matrix.getFrobeniusNorm(), ABSTOL, RELTOL);
    }

    /**
     * Tests the methods that return the entries of the matrix in a 2D data array.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricMatrix#getData()}<br>
     * {@linkplain ArrayRowSymmetricMatrix#getData(boolean)}<br>
     * </p>
     */
    @Test
    public void testGetData() {
        double[][] result;

        // Tested matrix
        final double[][] data = { { 3.94547, -7.63161, 3.45261, 5.07476 },
                { -7.63161, -8.39881, -1.39172, -3.71329 },
                { 3.45261, -1.39172, -0.87210, -8.36652 },
                { 5.07476, -3.71329, -8.36652, -1.16403 } };
        final SymmetricMatrix matrix = new ArrayRowSymmetricMatrix(SymmetryType.LOWER, data, 0., 0.);

        // Test the methods
        result = matrix.getData();
        CheckUtils.checkEquality(data, result, 0., 0.);
        Assert.assertNotSame(data, result);

        result = matrix.getData(true);
        CheckUtils.checkEquality(data, result, 0., 0.);
        Assert.assertNotSame(data, result);

        result = matrix.getData(false);
        CheckUtils.checkEquality(data, result, 0., 0.);
        Assert.assertNotSame(data, result);
    }

    /**
     * Tests the access to the internal array storing the data of the matrix.<br>
     * Modifying the returned array should also modify the associated symmetric matrix.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain ArrayRowSymmetricMatrix#getDataRef()}
     * </p>
     */
    @Test
    public void testGetDataRef() {
        // Symmetric matrix
        final double[][] data = { { 5.03073, 8.72707, 0.01360, -9.89783 },
                { 8.72707, -5.12552, -9.40967, 0.78359 }, { 0.01360, -9.40967, 7.65279, -4.65583 },
                { -9.89783, 0.78359, -4.65583, 7.73762 } };
        final ArrayRowSymmetricMatrix matrix = new ArrayRowSymmetricMatrix(SymmetryType.UPPER,
                data, 0., 0.);

        // Ensure the array retrieved is as expected
        final double[] internalArray = matrix.getDataRef();
        final double[] expectedArray = { data[0][0], data[1][0], data[1][1], data[2][0],
                data[2][1], data[2][2], data[3][0], data[3][1], data[3][2], data[3][3] };
        CheckUtils.checkEquality(expectedArray, internalArray, 0., 0.);

        // Ensure the returned array is a direct reference to the internal array.
        // Since we don't have another way to access to it, this is check by modifying
        // this array and checking it has the expected impact on the symmetric matrix.
        internalArray[0] = 3.9;
        Assert.assertEquals(matrix.getEntry(0, 0), 3.9, 0.);

        internalArray[1] = 8.3;
        Assert.assertEquals(matrix.getEntry(1, 0), 8.3, 0.);
        Assert.assertEquals(matrix.getEntry(0, 1), 8.3, 0.);
    }

    /**
     * Tests the method that performs a deep copy of the matrix.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain ArrayRowSymmetricMatrix#copy()}
     * </p>
     */
    @Test
    public void testCopy() {
        // Tested matrix
        final double[][] data = { { 8.50351, -3.57934, -4.46866, -4.12984 },
                { -3.57934, -2.57844, -4.28178, -1.15216 },
                { -4.46866, -4.28178, 7.89799, 4.26392 }, { -4.12984, -1.15216, 4.26392, -9.54943 } };
        final ArrayRowSymmetricMatrix matrix = new ArrayRowSymmetricMatrix(SymmetryType.LOWER,
                data, 0., 0.);

        // Test the method
        final ArrayRowSymmetricMatrix result = matrix.copy();
        CheckUtils.checkEquality(result, matrix, 0., 0.);
        Assert.assertNotSame(matrix, result);
        Assert.assertNotSame(matrix.getDataRef(), result.getDataRef());
        Assert.assertEquals(ArrayRowSymmetricMatrix.class, result.getClass());
    }

    /**
     * Tests the methods that return the transpose of the matrix.<br>
     * The transpose matrix should be the same as the initial matrix, stored in a new instance
     * unless specified otherwise.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricMatrix#transpose()}<br>
     * {@linkplain ArrayRowSymmetricMatrix#transpose(boolean)}<br>
     * </p>
     */
    @Test
    public void testTranspose() {
        // Tested matrix
        final double[][] data = { { -4.69871, -9.04464, 3.94991, 1.33259 },
                { -9.04464, -5.31749, -8.70355, -1.83852 },
                { 3.94991, -8.70355, -9.59135, 3.15441 }, { 1.33259, -1.83852, 3.15441, 4.82788 } };
        final SymmetricMatrix matrix = new ArrayRowSymmetricMatrix(SymmetryType.LOWER, data, 0., 0.);

        // Test the method
        CheckUtils.checkTranspose(matrix);
    }

    /**
     * Tests the method that raises the matrix to the power of N.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain ArrayRowSymmetricMatrix#power(int)}<br>
     * </p>
     */
    @Test
    public void testPower() {
        // Tested matrix
        final double[][] data = { { 1.54914, 1.11530, 4.45298, -9.35224 },
                { 1.11530, 8.99222, -3.78213, 2.56713 }, { 4.45298, -3.78213, -4.93795, -1.63516 },
                { -9.35224, 2.56713, -1.63516, 4.54790 } };
        final SymmetricMatrix matrix = new ArrayRowSymmetricMatrix(SymmetryType.LOWER, data, 0., 0.);

        // Test the method
        CheckUtils.checkPower(matrix, 0, 10, ArrayRowSymmetricMatrix.class, ABSTOL, RELTOL);
        CheckUtils.checkPowerNegativeExponent(matrix);
    }

    /**
     * Tests the method that returns an element of the matrix.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain ArrayRowSymmetricMatrix#getEntry(int, int)}
     * </p>
     */
    @Test
    public void testGetEntry() {
        // Tested matrix
        final double[][] data = { { 6.07313, 2.93290, -0.53093, -4.35811 },
                { 2.93290, 7.61512, 3.49134, 0.09678 }, { -0.53093, 3.49134, 1.43539, 9.81282 },
                { -4.35811, 0.09678, 9.81282, 7.02608 } };
        final SymmetricMatrix matrix = new ArrayRowSymmetricMatrix(SymmetryType.LOWER, data, 0., 0.);

        // Test the method
        CheckUtils.checkGetEntry(matrix);
    }

    /**
     * Tests the method that returns an element of the matrix, using indices outside the valid index
     * range.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain ArrayRowSymmetricMatrix#getEntry(int, int)}
     * </p>
     */
    @Test
    public void testGetOutOfRangeEntry() {
        final SymmetricMatrix matrix = new ArrayRowSymmetricMatrix(3);
        CheckUtils.checkGetOutOfRangeEntry(matrix);
    }

    /**
     * Tests the method that sets an entry to a new value.<br>
     * The symmetric element should be automatically modified when setting an off-diagonal element
     * to a new value.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain ArrayRowSymmetricMatrix#setEntry(int, int, double)}
     * </p>
     */
    @Test
    public void testSetEntry() {
        final int dim = 3;
        final SymmetricMatrix matrix = new ArrayRowSymmetricMatrix(3);

        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < i; j++) {
                final double d = -1. + 2. * RNG.nextDouble();
                matrix.setEntry(i, j, d);
                Assert.assertEquals(d, matrix.getEntry(i, j), 0.);
                Assert.assertEquals(d, matrix.getEntry(j, i), 0.);
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
     * {@linkplain ArrayRowSymmetricMatrix#setEntry(int, int, double)}
     * </p>
     */
    @Test
    public void testSetOutOfRangeEntry() {
        final SymmetricMatrix matrix = new ArrayRowSymmetricMatrix(3);
        CheckUtils.checkSetOutOfRangeEntry(matrix);
    }

    /**
     * Tests the method that adds a scalar to an entry.<br>
     * The symmetric elements should be automatically modified to maintain symmetry.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain ArrayRowSymmetricMatrix#addToEntry(int, int, double)}
     * </p>
     */
    @Test
    public void testAddToEntry() {
        final int dim = 3;
        final SymmetricMatrix matrix = new ArrayRowSymmetricMatrix(3);
        final double[][] data = matrix.getData();

        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < i; j++) {
                final double d = CheckUtils.getRandomNumber(RNG, -10., +10.);
                matrix.addToEntry(i, j, d);
                final double expected = d + data[i][j];
                CheckUtils.checkEquality(expected, matrix.getEntry(i, j), ABSTOL, RELTOL);
                CheckUtils.checkEquality(expected, matrix.getEntry(j, i), ABSTOL, RELTOL);
            }
        }
    }

    /**
     * Tests the method that adds a scalar to an entry, using indices outside the valid index range.<br>
     * An exception should be thrown.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain ArrayRowSymmetricMatrix#addToEntry(int, int, double)}
     * </p>
     */
    @Test
    public void testAddToOutOfRangeEntry() {
        final SymmetricMatrix matrix = new ArrayRowSymmetricMatrix(3);
        CheckUtils.checkAddToOutOfRangeEntry(matrix);
    }

    /**
     * Tests the method that multiplies an entry by a scalar.<br>
     * The symmetric elements should be automatically modified to maintain symmetry.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain ArrayRowSymmetricMatrix#multiplyEntry(int, int, double)}
     * </p>
     */
    @Test
    public void testMultiplyEntry() {
        final double[][] data = { { 7.41297, 6.70696, -7.63773, -3.93186 },
                { 6.70696, -7.96446, 3.18189, 2.66174 }, { -7.63773, 3.18189, -6.62809, 8.52437 },
                { -3.93186, 2.66174, 8.52437, -5.76453 } };
        final SymmetricMatrix matrix = new ArrayRowSymmetricMatrix(SymmetryType.LOWER, data, 0., 0.);
        final int dim = matrix.getRowDimension();

        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < i; j++) {
                final double d = CheckUtils.getRandomNumber(RNG, -10., +10.);
                matrix.multiplyEntry(i, j, d);
                final double expected = d * data[i][j];
                CheckUtils.checkEquality(expected, matrix.getEntry(i, j), ABSTOL, RELTOL);
                CheckUtils.checkEquality(expected, matrix.getEntry(j, i), ABSTOL, RELTOL);
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
     * {@linkplain ArrayRowSymmetricMatrix#multiplyEntry(int, int, double)}
     * </p>
     */
    @Test
    public void testMultiplyOutOfRangeEntry() {
        final SymmetricMatrix matrix = new ArrayRowSymmetricMatrix(3);
        CheckUtils.checkAddToOutOfRangeEntry(matrix);
    }

    /**
     * Tests the method that adds a scalar to the entries of the matrix.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain ArrayRowSymmetricMatrix#scalarAdd(double)}
     * </p>
     */
    @Test
    public void testScalarAdd() {
        // Tested matrix
        final double[][] data = { { 8.74075, 6.27386, -0.27883, 6.79435 },
                { 6.27386, 1.17960, -4.55575, -2.23750 },
                { -0.27883, -4.55575, -5.37170, 9.39669 }, { 6.79435, -2.23750, 9.39669, 9.68501 } };
        final SymmetricMatrix matrix = new ArrayRowSymmetricMatrix(SymmetryType.LOWER, data, 0., 0.);

        // Test the method
        CheckUtils.checkScalarAdd(matrix, 0., ArrayRowSymmetricMatrix.class, ABSTOL, RELTOL);
        CheckUtils.checkScalarAdd(matrix, +1.718, ArrayRowSymmetricMatrix.class, ABSTOL, RELTOL);
        CheckUtils.checkScalarAdd(matrix, -7.274, ArrayRowSymmetricMatrix.class, ABSTOL, RELTOL);
    }

    /**
     * Tests the method that multiplies the matrix by a scalar.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain ArrayRowSymmetricMatrix#scalarMultiply(double)}
     * </p>
     */
    @Test
    public void testScalarMultiply() {
        // Tested matrix
        final double[][] data = { { -9.89319, -0.36433, -5.42868, -6.98173 },
                { -0.36433, -0.69637, -1.88170, 0.25280 },
                { -5.42868, -1.88170, 3.15296, 1.08463 }, { -6.98173, 0.25280, 1.08463, 8.54262 } };
        final SymmetricMatrix matrix = new ArrayRowSymmetricMatrix(SymmetryType.LOWER, data, 0., 0.);

        // Test the method
        CheckUtils.checkScalarMultiply(matrix, 0.0, ArrayRowSymmetricMatrix.class, ABSTOL, RELTOL);
        CheckUtils.checkScalarMultiply(matrix, 1.0, ArrayRowSymmetricMatrix.class, ABSTOL, RELTOL);
        CheckUtils.checkScalarMultiply(matrix, +2.838, ArrayRowSymmetricMatrix.class, ABSTOL,
                RELTOL);
        CheckUtils.checkScalarMultiply(matrix, -4.278, ArrayRowSymmetricMatrix.class, ABSTOL,
                RELTOL);
    }

    /**
     * Tests the methods that add another matrix.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricMatrix#add(RealMatrix)}<br>
     * {@linkplain ArrayRowSymmetricMatrix#add(SymmetricMatrix)}<br>
     * {@linkplain ArrayRowSymmetricMatrix#add(DiagonalMatrix)}<br>
     * </p>
     */
    @Test
    public void testAddMatrix() {
        // Tested matrix
        final double[][] initialData = { { -6.49430, 0.10759, -8.90090, -2.05127 },
                { 0.10759, -9.41404, -4.80716, 9.18746 }, { -8.90090, -4.80716, 6.38011, 6.60371 },
                { -2.05127, 9.18746, 6.60371, -3.46847 } };
        final SymmetricMatrix matrix = new ArrayRowSymmetricMatrix(SymmetryType.LOWER, initialData,
                0., 0.);

        // Matrices to be added
        final double[] diagonalElements = { -3.94647, -5.69260, 4.60466, 4.71130 };
        final double[][] data = { { 4.08266, -1.24204, 0.87391, -1.14554 },
                { -1.24204, 4.84757, -0.85537, 1.06910 }, { 0.87391, -0.85537, 4.11636, -1.31590 },
                { -1.14554, 1.06910, -1.31590, 4.14346 } };

        final RealMatrix realMatrix = new Array2DRowRealMatrix(data);
        final DiagonalMatrix diagonalMatrix = new DiagonalMatrix(diagonalElements);
        final SymmetricMatrix symmetricMatrix = new ArrayRowSymmetricMatrix(SymmetryType.LOWER,
                data, 0., 0.);
        final SymmetricPositiveMatrix positiveMatrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, data, 0., 0., 0., 0.);

        // Test the methods
        CheckUtils.checkAddMatrix(matrix, realMatrix, Array2DRowRealMatrix.class, ABSTOL, RELTOL);
        CheckUtils.checkAddMatrix(matrix, symmetricMatrix, ArrayRowSymmetricMatrix.class, ABSTOL,
                RELTOL);
        CheckUtils.checkAddMatrix(matrix, positiveMatrix, ArrayRowSymmetricMatrix.class, ABSTOL,
                RELTOL);
        CheckUtils.checkAddMatrix(matrix, diagonalMatrix, ArrayRowSymmetricMatrix.class, ABSTOL,
                RELTOL);
    }

    /**
     * Tests the methods that add another matrix, using a matrix with incompatible dimensions.<br>
     * An exception should be thrown.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricMatrix#add(RealMatrix)}<br>
     * {@linkplain ArrayRowSymmetricMatrix#add(SymmetricMatrix)}<br>
     * {@linkplain ArrayRowSymmetricMatrix#add(DiagonalMatrix)}<br>
     * </p>
     */
    @Test
    public void testAddIncompatibleMatrix() {
        final SymmetricMatrix matrix = new ArrayRowSymmetricMatrix(3);
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
     * {@linkplain ArrayRowSymmetricMatrix#subtract(RealMatrix)}<br>
     * {@linkplain ArrayRowSymmetricMatrix#subtract(SymmetricMatrix)}<br>
     * {@linkplain ArrayRowSymmetricMatrix#subtract(DiagonalMatrix)}<br>
     * </p>
     */
    @Test
    public void testSubtractMatrix() {
        // Tested matrix
        final double[][] initialData = { { -5.19154, 3.17721, 8.41512, -7.21713 },
                { 3.17721, 4.67367, -5.56987, -8.69264 }, { 8.41512, -5.56987, 1.51554, 3.81419 },
                { -7.21713, -8.69264, 3.81419, 9.07735 } };
        final SymmetricMatrix matrix = new ArrayRowSymmetricMatrix(SymmetryType.LOWER, initialData,
                0., 0.);

        // Matrices to be subtracted
        final double[] diagonalElements = { 4.97926, -8.71446, -1.14737, 2.02389 };
        final double[][] data = { { 7.99201, 0.62995, -0.30526, -0.67180 },
                { 0.62995, 6.93257, 1.42626, 0.90709 }, { -0.30526, 1.42626, 7.74889, -0.16337 },
                { -0.67180, 0.90709, -0.16337, 9.06574 } };

        final RealMatrix realMatrix = new Array2DRowRealMatrix(data);
        final DiagonalMatrix diagonalMatrix = new DiagonalMatrix(diagonalElements);
        final SymmetricMatrix symmetricMatrix = new ArrayRowSymmetricMatrix(SymmetryType.LOWER,
                data, 0., 0.);
        final SymmetricPositiveMatrix positiveMatrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, data, 0., 0., 0., 0.);

        // Test the methods
        CheckUtils.checkSubtractMatrix(matrix, realMatrix, Array2DRowRealMatrix.class, ABSTOL,
                RELTOL);
        CheckUtils.checkSubtractMatrix(matrix, symmetricMatrix, ArrayRowSymmetricMatrix.class,
                ABSTOL, RELTOL);
        CheckUtils.checkSubtractMatrix(matrix, positiveMatrix, ArrayRowSymmetricMatrix.class,
                ABSTOL, RELTOL);
        CheckUtils.checkSubtractMatrix(matrix, diagonalMatrix, ArrayRowSymmetricMatrix.class,
                ABSTOL, RELTOL);
    }

    /**
     * Tests the methods that subtract another matrix, using a matrix with incompatible dimensions.<br>
     * An exception should be thrown.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricMatrix#subtract(RealMatrix)}<br>
     * {@linkplain ArrayRowSymmetricMatrix#subtract(SymmetricMatrix)}<br>
     * {@linkplain ArrayRowSymmetricMatrix#subtract(DiagonalMatrix)}<br>
     * </p>
     */
    @Test
    public void testSubtractIncompatibleMatrix() {
        final SymmetricMatrix matrix = new ArrayRowSymmetricMatrix(3);
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
     * {@linkplain ArrayRowSymmetricMatrix#multiply(RealMatrix)}<br>
     * {@linkplain ArrayRowSymmetricMatrix#multiply(RealMatrix, boolean)}<br>
     * {@linkplain ArrayRowSymmetricMatrix#multiply(RealMatrix, boolean, double)}<br>
     * </p>
     */
    @Test
    public void testMultiplyBySquareMatrix() {
        // Tested matrix
        final double[][] initialData = { { -9.41027, 0.09700, 4.18757, -7.03913 },
                { 0.09700, 6.96043, 3.13811, -6.04105 }, { 4.18757, 3.13811, 4.31027, -3.68896 },
                { -7.03913, -6.04105, -3.68896, -1.35296 } };
        final SymmetricMatrix matrix = new ArrayRowSymmetricMatrix(SymmetryType.LOWER, initialData,
                0., 0.);

        // Matrices to multiply by
        final double[] diagonalElements = { 5.89449, 8.44734, -2.31446, 2.28704 };
        final double[][] data = { { 8.63086, -0.48125, -0.43030, -0.20734 },
                { -0.48125, 7.28312, -1.94473, -0.23549 },
                { -0.43030, -1.94473, 6.56060, -0.03737 },
                { -0.20734, -0.23549, -0.03737, 8.72148 } };

        final RealMatrix realMatrix = new Array2DRowRealMatrix(data);
        final DiagonalMatrix diagonalMatrix = new DiagonalMatrix(diagonalElements);
        final SymmetricMatrix symmetricMatrix = new ArrayRowSymmetricMatrix(SymmetryType.LOWER,
                data, 0., 0.);
        final SymmetricPositiveMatrix positiveMatrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, data, 0., 0., 0., 0.);

        // Scaling factor
        final double scalingFactor = 6.38947;

        // Test the methods
        CheckUtils.checkMultiplyMatrix(matrix, realMatrix, scalingFactor,
                Array2DRowRealMatrix.class, ABSTOL, RELTOL);
        CheckUtils.checkMultiplyMatrix(matrix, symmetricMatrix, scalingFactor,
                Array2DRowRealMatrix.class, ABSTOL, RELTOL);
        CheckUtils.checkMultiplyMatrix(matrix, positiveMatrix, scalingFactor,
                Array2DRowRealMatrix.class, ABSTOL, RELTOL);
        CheckUtils.checkMultiplyMatrix(matrix, diagonalMatrix, scalingFactor,
                Array2DRowRealMatrix.class, ABSTOL, RELTOL);
    }

    /**
     * Tests the methods that postmultiply the matrix by another matrix, using a rectangular matrix.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricMatrix#multiply(RealMatrix)}<br>
     * {@linkplain ArrayRowSymmetricMatrix#multiply(RealMatrix, boolean)}<br>
     * {@linkplain ArrayRowSymmetricMatrix#multiply(RealMatrix, boolean, double)}<br>
     * </p>
     */
    @Test
    public void testMultiplyByRectangularMatrix() {
        // Tested matrix
        final double[][] initialData = { { -4.33397, -2.72985, -9.74679, 7.86466 },
                { -2.72985, 5.18750, -4.95928, -8.65226 },
                { -9.74679, -4.95928, 8.11860, 8.92771 }, { 7.86466, -8.65226, 8.92771, -5.02913 } };
        final SymmetricMatrix matrix = new ArrayRowSymmetricMatrix(SymmetryType.LOWER, initialData,
                0., 0.);

        // Matrices to multiply by
        final double[][] data = { { -3.78317, 8.16533, -4.25183, -0.92409, -3.64234, 9.95824 },
                { -0.81084, -3.52249, -0.78957, -6.18474, 2.67746, 2.33604 },
                { -1.34890, -8.97216, -8.04161, -0.10833, 2.77801, -4.63476 },
                { -5.17151, -9.90927, -6.36975, -4.12667, -3.48380, -9.62290 } };
        final RealMatrix realMatrix = new Array2DRowRealMatrix(data);

        // Scaling factor
        final double scalingFactor = -8.20034;

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
     * {@linkplain ArrayRowSymmetricMatrix#multiply(RealMatrix)}<br>
     * {@linkplain ArrayRowSymmetricMatrix#multiply(RealMatrix, boolean)}<br>
     * {@linkplain ArrayRowSymmetricMatrix#multiply(RealMatrix, boolean, double)}<br>
     * </p>
     */
    @Test
    public void testMultiplyByIncompatibleMatrix() {
        final SymmetricMatrix matrix = new ArrayRowSymmetricMatrix(3);
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
     * {@linkplain ArrayRowSymmetricMatrix#preMultiply(RealMatrix)}
     * </p>
     */
    @Test
    public void testPreMultiplyBySquareMatrix() {
        // Tested matrix
        final double[][] initialData = { { 9.06560, 0.69791, -2.73696, -5.66962 },
                { 0.69791, -8.29811, -1.44823, -3.72112 },
                { -2.73696, -1.44823, 4.52653, -9.60122 },
                { -5.66962, -3.72112, -9.60122, -8.72850 } };
        final SymmetricMatrix matrix = new ArrayRowSymmetricMatrix(SymmetryType.LOWER, initialData,
                0., 0.);

        // Matrices to premultiply by
        final double[] diagonalElements = { -3.99763, -6.33391, -2.70458, 5.38855 };
        final double[][] data = { { 4.01092, -0.58577, -0.20349, 2.57269 },
                { -0.58577, 1.16867, -0.56197, 0.12739 },
                { -0.20349, -0.56197, 1.51390, -0.03571 }, { 2.57269, 0.12739, -0.03571, 4.41839 } };

        final RealMatrix realMatrix = new Array2DRowRealMatrix(data);
        final DiagonalMatrix diagonalMatrix = new DiagonalMatrix(diagonalElements);
        final SymmetricMatrix symmetricMatrix = new ArrayRowSymmetricMatrix(SymmetryType.LOWER,
                data, 0., 0.);
        final SymmetricPositiveMatrix positiveMatrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, data, 0., 0., 0., 0.);

        // Test the methods
        CheckUtils.checkPreMultiplyMatrix(matrix, realMatrix, Array2DRowRealMatrix.class, ABSTOL,
                RELTOL);
        CheckUtils.checkPreMultiplyMatrix(matrix, symmetricMatrix, Array2DRowRealMatrix.class,
                ABSTOL, RELTOL);
        CheckUtils.checkPreMultiplyMatrix(matrix, positiveMatrix, Array2DRowRealMatrix.class,
                ABSTOL, RELTOL);
        CheckUtils.checkPreMultiplyMatrix(matrix, diagonalMatrix, Array2DRowRealMatrix.class,
                ABSTOL, RELTOL);
    }

    /**
     * Tests the method that premultiplies the matrix by another matrix, using a rectangular matrix.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain ArrayRowSymmetricMatrix#preMultiply(RealMatrix)}<br>
     * </p>
     */
    @Test
    public void testPreMultiplyByRectangularMatrix() {
        // Tested matrix
        final double[][] initialData = { { -1.26002, -6.84437, -4.46559, 5.13255 },
                { -6.84437, 9.16645, -3.29233, -3.00837 },
                { -4.46559, -3.29233, -0.21391, -3.40666 },
                { 5.13255, -3.00837, -3.40666, -5.07415 } };
        final SymmetricMatrix matrix = new ArrayRowSymmetricMatrix(SymmetryType.LOWER, initialData,
                0., 0.);

        // Matrices to premultiply by
        final double[][] data = { { 5.60924, -1.04580, -5.02092, 8.41009 },
                { 5.02054, 5.15750, -0.19297, -8.03723 }, { 4.05564, 6.08063, -8.50878, 6.48401 },
                { 5.09559, -5.32431, -5.84290, -4.93997 }, { 5.71676, 3.01054, 7.43524, -2.60146 },
                { -8.20102, -0.94202, -4.66786, 9.13414 } };
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
     * {@linkplain ArrayRowSymmetricMatrix#preMultiply(RealMatrix)}
     * </p>
     */
    @Test
    public void testPerMultiplyByIncompatibleMatrix() {
        final ArrayRowSymmetricMatrix matrix = new ArrayRowSymmetricMatrix(3);
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
     * {@linkplain ArrayRowSymmetricMatrix#operate(double[])}<br>
     * {@linkplain ArrayRowSymmetricMatrix#operate(RealVector)}<br>
     * </p>
     */
    @Test
    public void testOperateVector() {
        // Tested matrix
        final double[][] data = { { 9.66950, -6.31177, -9.87539, -8.32043 },
                { -6.31177, -3.52349, -6.73023, 2.66071 },
                { -9.87539, -6.73023, -2.25064, 0.69630 }, { -8.32043, 2.66071, 0.69630, -8.25010 } };
        final SymmetricMatrix matrix = new ArrayRowSymmetricMatrix(SymmetryType.LOWER, data, 0., 0.);

        // Vector to multiply the tested matrix by
        final double[] vectorData = { -4.23794, -1.25768, 6.41618, 6.24087 };
        final RealVector vector = new ArrayRealVector(vectorData);

        // Test the method
        CheckUtils.checkPreMultiplyVector(matrix, vector, ABSTOL, RELTOL);
    }

    /**
     * Tests the methods that postmultiply the matrix by a vector, using a vector with an
     * incompatible dimension.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricMatrix#operate(double[])}<br>
     * {@linkplain ArrayRowSymmetricMatrix#operate(RealVector)}<br>
     * </p>
     */
    @Test
    public void testOperateIncompatibleVector() {
        final SymmetricMatrix matrix = new ArrayRowSymmetricMatrix(3);
        CheckUtils.checkOperateIncompatibleVector(matrix);
    }

    /**
     * Tests the methods that premultiply the matrix by a vector.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricMatrix#preMultiply(double[])}<br>
     * {@linkplain ArrayRowSymmetricMatrix#preMultiply(RealVector)}<br>
     * </p>
     */
    @Test
    public void testPreMultiplyByVector() {
        // Tested matrix
        final double[][] data = { { 8.42590, -5.45856, 7.73629, 4.59586 },
                { -5.45856, 3.49669, 7.11326, -5.70237 }, { 7.73629, 7.11326, -8.03689, 7.32498 },
                { 4.59586, -5.70237, 7.32498, 0.35036 } };
        final SymmetricMatrix matrix = new ArrayRowSymmetricMatrix(SymmetryType.LOWER, data, 0., 0.);

        // Vector to premultiply the tested matrix by
        final double[] vectorData = { 2.77084, 5.12564, -2.95591, -7.68948 };
        final RealVector vector = new ArrayRealVector(vectorData);

        // Test the method
        CheckUtils.checkPreMultiplyVector(matrix, vector, ABSTOL, RELTOL);
    }

    /**
     * Tests the methods that premultiply the matrix by a vector, using a vector with an
     * incompatible dimension.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricMatrix#preMultiply(double[])}<br>
     * {@linkplain ArrayRowSymmetricMatrix#preMultiply(RealVector)}<br>
     * </p>
     */
    @Test
    public void testPreMultiplyByIncompatibleVector() {
        final SymmetricMatrix matrix = new ArrayRowSymmetricMatrix(3);
        CheckUtils.checkPreMultiplyIncompatibleVector(matrix);
    }

    /**
     * Tests the quadratic multiplication.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricMatrix#quadraticMultiplication(RealMatrix)}<br>
     * {@linkplain ArrayRowSymmetricMatrix#quadraticMultiplication(RealMatrix, boolean)}<br>
     * </p>
     */
    @Test
    public void testQuadraticMultiplicationSquareMatrix() {
        // Tested matrix
        final double[][] initialData = { { -3.03731, 2.74981, -1.94897, -2.07300 },
                { 2.74981, -1.34439, -9.36543, -5.52173 },
                { -1.94897, -9.36543, -9.24842, -1.79189 },
                { -2.07300, -5.52173, -1.79189, -2.95957 } };
        final SymmetricMatrix matrix = new ArrayRowSymmetricMatrix(SymmetryType.LOWER, initialData,
                0., 0.);

        // Square matrix used for the quadratic multiplication
        final double[][] data = { { 4.23187, 1.31345, 9.97799, 5.26182 },
                { -0.11155, -8.13910, -7.00925, 8.82326 },
                { -7.70064, -1.97150, -6.51543, -3.11692 },
                { -0.29533, 3.37368, -5.43956, -0.31609 } };
        final RealMatrix realMatrix = new Array2DRowRealMatrix(data);

        // Test the methods
        CheckUtils.checkQuadraticMultiplication(matrix, realMatrix, ArrayRowSymmetricMatrix.class,
                ABSTOL, RELTOL);
    }

    /**
     * Tests the quadratic multiplication using a rectangular matrix.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricMatrix#quadraticMultiplication(RealMatrix)}<br>
     * {@linkplain ArrayRowSymmetricMatrix#quadraticMultiplication(RealMatrix, boolean)}<br>
     * </p>
     */
    @Test
    public void testQuadraticMultiplicationRectangularMatrix() {
        // Tested matrix
        final double[][] initialData = { { 2.51080, -6.76582, -5.95404, -2.44639 },
                { -6.76582, 0.99552, -2.61833, 7.47610 },
                { -5.95404, -2.61833, 1.14634, -2.21852 }, { -2.44639, 7.47610, -2.21852, 8.51936 } };
        final SymmetricMatrix matrix = new ArrayRowSymmetricMatrix(SymmetryType.LOWER, initialData,
                0., 0.);

        // Rectangular matrix used for the quadratic multiplication
        final double[][] data = { { -0.26569, -2.29826, -1.96533, -9.18249 },
                { 4.31795, -5.96141, -5.43924, -1.75918 },
                { -9.06614, 3.41659, 8.77984, -9.73434 },
                { 3.61979, -4.96497, -7.99873, -5.35373 },
                { 5.36783, -3.10025, 5.84822, -2.59290 }, { 0.02128, 0.30272, 6.58746, 9.19225 } };
        final RealMatrix realMatrix = new Array2DRowRealMatrix(data);

        // Test the methods
        CheckUtils.checkQuadraticMultiplication(matrix, realMatrix, ArrayRowSymmetricMatrix.class,
                ABSTOL, RELTOL);
    }

    /**
     * Tests the quadratic multiplication using a matrix with incompatible dimensions.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricMatrix#quadraticMultiplication(RealMatrix)}<br>
     * {@linkplain ArrayRowSymmetricMatrix#quadraticMultiplication(RealMatrix, boolean)}<br>
     * </p>
     */
    @Test
    public void testQuadraticMultiplicationIncompatibleMatrix() {
        final SymmetricMatrix matrix = new ArrayRowSymmetricMatrix(4);
        CheckUtils.checkQuadraticMultiplicationIncompatibleMatrix(matrix);
    }

    /**
     * Tests the methods that return a row of the matrix.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricMatrix#getRow(int)}<br>
     * {@linkplain ArrayRowSymmetricMatrix#getRowVector(int)}<br>
     * {@linkplain ArrayRowSymmetricMatrix#getRowMatrix(int)}<br>
     * </p>
     */
    @Test
    public void testGetRow() {
        // Tested matrix
        final double[][] data = { { -9.53567, -7.82565, -4.26735, 9.60374 },
                { -7.82565, -5.72542, 9.30362, -6.01753 },
                { -4.26735, 9.30362, -0.20285, 9.62667 }, { 9.60374, -6.01753, 9.62667, 6.25247 } };
        final ArrayRowSymmetricMatrix matrix = new ArrayRowSymmetricMatrix(SymmetryType.LOWER,
                data, 0., 0.);

        // Test the methods
        CheckUtils.checkGetRow(matrix);
    }

    /**
     * Tests the methods that return a row of the matrix, using indices outside the valid index
     * range.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricMatrix#getRow(int)}<br>
     * {@linkplain ArrayRowSymmetricMatrix#getRowVector(int)}<br>
     * {@linkplain ArrayRowSymmetricMatrix#getRowMatrix(int)}<br>
     * </p>
     */
    @Test
    public void testGetOutOfRangeRow() {
        final ArrayRowSymmetricMatrix matrix = new ArrayRowSymmetricMatrix(3);
        CheckUtils.checkGetOutOfRangeRow(matrix);
    }

    /**
     * Tests methods that set a row of the matrix.<br>
     * The symmetric elements should be automatically modified.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricMatrix#setRow(int, double[])}<br>
     * {@linkplain ArrayRowSymmetricMatrix#setRowVector(int, RealVector)}<br>
     * {@linkplain ArrayRowSymmetricMatrix#setRowMatrix(int, RealMatrix)}<br>
     * </p>
     */
    @Test
    public void testSetRow() {
        RealMatrix realMatrix;
        ArrayRowSymmetricMatrix matrix;

        final int dim = 3;
        final double[] rowArray = { +5, -2, +6 };
        final RealVector rowVector = new ArrayRealVector(rowArray);
        final RealMatrix rowMatrix = MatrixUtils.createRowRealMatrix(rowArray);

        for (int i = 0; i < dim; i++) {
            // Set row array
            matrix = new ArrayRowSymmetricMatrix(dim);
            matrix.setRow(i, rowArray);
            realMatrix = new Array2DRowRealMatrix(dim, dim);
            realMatrix.setRow(i, rowArray);
            realMatrix.setColumn(i, rowArray);
            CheckUtils.checkEquality(realMatrix, matrix, 0., 0.);

            // Set row vector
            matrix = new ArrayRowSymmetricMatrix(dim);
            matrix.setRowVector(i, rowVector);
            realMatrix = new Array2DRowRealMatrix(dim, dim);
            realMatrix.setRowVector(i, rowVector);
            realMatrix.setColumnVector(i, rowVector);
            CheckUtils.checkEquality(realMatrix, matrix, 0., 0.);

            // Set row matrix
            matrix = new ArrayRowSymmetricMatrix(dim);
            matrix.setRowMatrix(i, rowMatrix);
            realMatrix = new Array2DRowRealMatrix(dim, dim);
            realMatrix.setRowMatrix(i, rowMatrix);
            realMatrix.setColumnMatrix(i, rowMatrix.transpose());
            CheckUtils.checkEquality(realMatrix, matrix, 0., 0.);
        }
    }

    /**
     * Tests the methods that set a row of the matrix, using indices outside the valid index range.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricMatrix#setRow(int, double[])}<br>
     * {@linkplain ArrayRowSymmetricMatrix#setRowVector(int, RealVector)}<br>
     * {@linkplain ArrayRowSymmetricMatrix#setRowMatrix(int, RealMatrix)}<br>
     * </p>
     */
    @Test
    public void testSetOutOfRangeRow() {
        final ArrayRowSymmetricMatrix matrix = new ArrayRowSymmetricMatrix(3);
        CheckUtils.checkSetOutOfRangeRow(matrix);
    }

    /**
     * Tests the methods that set a row of the matrix, using incompatible arrays, vectors and row
     * matrices.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricMatrix#setRow(int, double[])}<br>
     * {@linkplain ArrayRowSymmetricMatrix#setRowVector(int, RealVector)}<br>
     * {@linkplain ArrayRowSymmetricMatrix#setRowMatrix(int, RealMatrix)}<br>
     * </p>
     */
    @Test
    public void testSetIncompatibleRow() {
        final RealMatrix matrix = new ArrayRowSymmetricMatrix(3);
        CheckUtils.checkSetIncompatibleRow(matrix);
    }

    /**
     * Tests the methods that return a column of the matrix.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricMatrix#getColumn(int)}<br>
     * {@linkplain ArrayRowSymmetricMatrix#getColumnVector(int)}<br>
     * {@linkplain ArrayRowSymmetricMatrix#getColumnMatrix(int)}<br>
     * </p>
     */
    @Test
    public void testGetColumn() {
        // Tested matrix
        final double[][] data = { { -9.42443, -6.97516, 7.17199, -9.76684 },
                { -6.97516, 0.01745, -6.94909, 0.47345 }, { 7.17199, -6.94909, 9.49478, 4.03466 },
                { -9.76684, 0.47345, 4.03466, 1.74812 } };
        final SymmetricMatrix matrix = new ArrayRowSymmetricMatrix(SymmetryType.LOWER, data, 0., 0.);

        // Test the methods
        CheckUtils.checkGetColumn(matrix);
    }

    /**
     * Tests the methods that return a column of the matrix, using indices outside the valid index
     * range.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricMatrix#getColumn(int)}<br>
     * {@linkplain ArrayRowSymmetricMatrix#getColumnVector(int)}<br>
     * {@linkplain ArrayRowSymmetricMatrix#getColumnMatrix(int)}<br>
     * </p>
     */
    @Test
    public void testGetOutOfRangeColumn() {
        final SymmetricMatrix matrix = new ArrayRowSymmetricMatrix(3);
        CheckUtils.checkGetOutOfRangeColumn(matrix);
    }

    /**
     * Tests the methods that set a column of the matrix.<br>
     * The symmetric elements should be automatically modified.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricMatrix#setRow(int, double[])}<br>
     * {@linkplain ArrayRowSymmetricMatrix#setRowVector(int, RealVector)}<br>
     * {@linkplain ArrayRowSymmetricMatrix#setRowMatrix(int, RealMatrix)}<br>
     * </p>
     */
    @Test
    public void testSetColumn() {
        RealMatrix realMatrix;
        ArrayRowSymmetricMatrix matrix;

        final int dim = 3;
        final double[] columnArray = { -7, +8, -3 };
        final RealVector columnVector = new ArrayRealVector(columnArray);
        final RealMatrix columnMatrix = MatrixUtils.createColumnRealMatrix(columnArray);

        for (int j = 0; j < dim; j++) {
            // Set column array
            matrix = new ArrayRowSymmetricMatrix(dim);
            matrix.setColumn(j, columnArray);
            realMatrix = new Array2DRowRealMatrix(dim, dim);
            realMatrix.setRow(j, columnArray);
            realMatrix.setColumn(j, columnArray);
            CheckUtils.checkEquality(realMatrix, matrix, 0., 0.);

            // Set column vector
            matrix = new ArrayRowSymmetricMatrix(dim);
            matrix.setColumnVector(j, columnVector);
            realMatrix = new Array2DRowRealMatrix(dim, dim);
            realMatrix.setRowVector(j, columnVector);
            realMatrix.setColumnVector(j, columnVector);
            CheckUtils.checkEquality(realMatrix, matrix, 0., 0.);

            // Set column matrix
            matrix = new ArrayRowSymmetricMatrix(dim);
            matrix.setColumnMatrix(j, columnMatrix);
            realMatrix = new Array2DRowRealMatrix(dim, dim);
            realMatrix.setRowMatrix(j, columnMatrix.transpose());
            realMatrix.setColumnMatrix(j, columnMatrix);
            CheckUtils.checkEquality(realMatrix, matrix, 0., 0.);
        }
    }

    /**
     * Tests the methods that set a column of the matrix, using indices outside the valid index
     * range.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricMatrix#setColumn(int, double[])}<br>
     * {@linkplain ArrayRowSymmetricMatrix#setColumnVector(int, RealVector)}<br>
     * {@linkplain ArrayRowSymmetricMatrix#setColumnMatrix(int, RealMatrix)}<br>
     * </p>
     */
    @Test
    public void testSetOutOfRangeColumn() {
        final ArrayRowSymmetricMatrix matrix = new ArrayRowSymmetricMatrix(3);
        CheckUtils.checkSetOutOfRangeColumn(matrix);
    }

    /**
     * Tests the methods that set a column of the matrix, using incompatible arrays, vectors and
     * column matrices.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricMatrix#setColumn(int, double[])}<br>
     * {@linkplain ArrayRowSymmetricMatrix#setColumnVector(int, RealVector)}<br>
     * {@linkplain ArrayRowSymmetricMatrix#setColumnMatrix(int, RealMatrix)}<br>
     * </p>
     */
    @Test
    public void testSetIncompatibleColumn() {
        final RealMatrix matrix = new ArrayRowSymmetricMatrix(3);
        CheckUtils.checkSetIncompatibleColumn(matrix);
    }

    /**
     * Tests the methods returning a submatrix.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricMatrix#getSubMatrix(int, int, int, int)}<br>
     * {@linkplain ArrayRowSymmetricMatrix#getSubMatrix(int[], int[])}<br>
     * {@linkplain ArrayRowSymmetricMatrix#getSubMatrix(int, int)}<br>
     * {@linkplain ArrayRowSymmetricMatrix#getSubMatrix(int[])}<br>
     * </p>
     */
    @Test
    public void testGetSubMatrix() {
        // Tested matrix
        final double[][] data = { { -8.93952, -0.50373, -9.10896, 8.55483 },
                { -0.50373, 8.24144, 7.40364, 2.16369 }, { -9.10896, 7.40364, -3.81995, -9.18269 },
                { 8.55483, 2.16369, -9.18269, -5.77599 } };
        final ArrayRowSymmetricMatrix matrix = new ArrayRowSymmetricMatrix(SymmetryType.LOWER,
                data, 0., 0.);

        // Test the methods
        CheckUtils.checkGetSubMatrixByRange(matrix);
        CheckUtils.checkGetSubMatrixByIndex(matrix);
        CheckUtils.checkGetSymmetricSubMatrixByRange(matrix, ArrayRowSymmetricMatrix.class);
        CheckUtils.checkGetSymmetricSubMatrixByIndex(matrix, ArrayRowSymmetricMatrix.class);
    }

    /**
     * Tests the methods returning a submatrix when supplying an invalid index range or array.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricMatrix#getSubMatrix(int, int, int, int)}<br>
     * {@linkplain ArrayRowSymmetricMatrix#getSubMatrix(int[], int[])}<br>
     * {@linkplain ArrayRowSymmetricMatrix#getSubMatrix(int, int)}<br>
     * {@linkplain ArrayRowSymmetricMatrix#getSubMatrix(int[])}<br>
     * </p>
     */
    @Test
    public void testGetSubMatrixInvalidIndices() {
        final SymmetricMatrix matrix = new ArrayRowSymmetricMatrix(3);
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
     * Tests the methods setSubMatrix().<br>
     * The method should fail as this operation is not supported for symmetric matrices.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain ArrayRowSymmetricMatrix#setSubMatrix(double[][], int, int)}
     * </p>
     */
    @Test
    public void testSetSubMatrix() {
        final SymmetricMatrix matrix = new ArrayRowSymmetricMatrix(4);
        final int dim = matrix.getRowDimension();
        final double[][] subMatrix = new double[][] { { 0 } };

        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < dim; j++) {
                try {
                    matrix.setSubMatrix(subMatrix, i, j);
                    Assert.fail();
                } catch (final MathUnsupportedOperationException e) {
                    Assert.assertEquals(MathUnsupportedOperationException.class, e.getClass());
                    Assert.assertEquals("unsupported operation", e.getMessage());
                }
            }
        }
    }

    /**
     * Tests the method allowing to copy parts of the matrix into a given array.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricMatrix#copySubMatrix(int, int, int, int, double[][])}<br>
     * {@linkplain ArrayRowSymmetricMatrix#copySubMatrix(int, int, int, int, double[][], int, int)}<br>
     * {@linkplain ArrayRowSymmetricMatrix#copySubMatrix(int[], int[], double[][])}<br>
     * {@linkplain ArrayRowSymmetricMatrix#copySubMatrix(int[], int[], double[][], int, int)}<br>
     * </p>
     */
    @Test
    public void testCopySubMatrix() {
        // Tested matrix
        final double[][] data = { { -7.31621, 5.49087, 6.92449, -3.80870 },
                { 5.49087, 6.89052, -1.39558, 9.22924 }, { 6.92449, -1.39558, -4.77300, 0.12350 },
                { -3.80870, 9.22924, 0.12350, -8.56391 } };
        final ArrayRowSymmetricMatrix matrix = new ArrayRowSymmetricMatrix(SymmetryType.LOWER,
                data, 0., 0.);

        // Test the method
        CheckUtils.checkCopySubMatrixByRange(matrix);
        CheckUtils.checkCopySubMatrixByIndex(matrix);
    }

    /**
     * Tests the methods that copies part of the matrix into a given array, using indices outside
     * the valid index range.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricMatrix#copySubMatrix(int, int, int, int, double[][])}<br>
     * {@linkplain ArrayRowSymmetricMatrix#copySubMatrix(int, int, int, int, double[][], int, int)}<br>
     * {@linkplain ArrayRowSymmetricMatrix#copySubMatrix(int[], int[], double[][])}<br>
     * {@linkplain ArrayRowSymmetricMatrix#copySubMatrix(int[], int[], double[][], int, int)}<br>
     * </p>
     */
    @Test
    public void testCopySubMatrixInvalidIndices() {
        final RealMatrix matrix = new ArrayRowSymmetricMatrix(4);
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
     * {@linkplain ArrayRowSymmetricMatrix#copySubMatrix(int, int, int, int, double[][])}<br>
     * {@linkplain ArrayRowSymmetricMatrix#copySubMatrix(int, int, int, int, double[][], int, int)}<br>
     * {@linkplain ArrayRowSymmetricMatrix#copySubMatrix(int[], int[], double[][])}<br>
     * {@linkplain ArrayRowSymmetricMatrix#copySubMatrix(int[], int[], double[][], int, int)}<br>
     * </p>
     */
    @Test
    public void testCopySubMatrixNullDestinationArray() {
        final RealMatrix matrix = new ArrayRowSymmetricMatrix(4);
        CheckUtils.checkCopySubMatrixNullDestinationArray(matrix);
    }

    /**
     * Tests the methods that copies part of the matrix into a given array, using a destination
     * array with no rows or columns.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricMatrix#copySubMatrix(int, int, int, int, double[][])}<br>
     * {@linkplain ArrayRowSymmetricMatrix#copySubMatrix(int, int, int, int, double[][], int, int)}<br>
     * {@linkplain ArrayRowSymmetricMatrix#copySubMatrix(int[], int[], double[][])}<br>
     * {@linkplain ArrayRowSymmetricMatrix#copySubMatrix(int[], int[], double[][], int, int)}<br>
     * </p>
     */
    @Test
    public void testCopySubMatrixEmptyDestinationArray() {
        final RealMatrix matrix = new ArrayRowSymmetricMatrix(4);
        CheckUtils.checkCopySubMatrixEmptyDestinationArray(matrix);
    }

    /**
     * Tests the methods that copies part of the matrix into a given array, using a destination
     * array too small to store the extracted
     * submatrix.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricMatrix#copySubMatrix(int, int, int, int, double[][])}<br>
     * {@linkplain ArrayRowSymmetricMatrix#copySubMatrix(int, int, int, int, double[][], int, int)}<br>
     * {@linkplain ArrayRowSymmetricMatrix#copySubMatrix(int[], int[], double[][])}<br>
     * {@linkplain ArrayRowSymmetricMatrix#copySubMatrix(int[], int[], double[][], int, int)}<br>
     * </p>
     */
    @Test
    public void testCopySubMatrixIncompatibleDestinationArray() {
        final RealMatrix matrix = new ArrayRowSymmetricMatrix(4);
        CheckUtils.checkCopySubMatrixIncompatibleDestinationArray(matrix);
    }

    /**
     * Tests the methods that concatenate two matrices horizontally.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricMatrix#concatenateHorizontally(RealMatrix)}<br>
     * {@linkplain ArrayRowSymmetricMatrix#concatenateHorizontally(RealMatrix, boolean)}<br>
     * </p>
     */
    @Test
    public void testHorizontalConcatenation() {
        // Tested matrix
        final double[][] initialData = { { -8.85648, -8.15747, -7.22472, 4.61024 },
                { -8.15747, 0.25211, 2.44590, 8.51806 }, { -7.22472, 2.44590, -8.55265, -9.25188 },
                { 4.61024, 8.51806, -9.25188, 6.81277 } };
        final SymmetricMatrix matrix = new ArrayRowSymmetricMatrix(SymmetryType.LOWER, initialData,
                0., 0.);

        // Matrix to be concatenated with the tested matrix (4x6)
        final double[][] data = { { -0.56463, 3.76982, 2.39786, -2.30677, -5.90044, -2.36264 },
                { -1.65314, -7.04788, -9.40641, 0.50886, -1.18919, -9.57191 },
                { -7.39786, -9.19403, -2.19757, 2.89403, 6.62264, -2.07835 },
                { 7.03471, 6.22758, 2.06374, 0.93622, 0.37226, 3.32043 } };
        final RealMatrix realMatrix = new Array2DRowRealMatrix(data, false);

        // Test the methods
        CheckUtils.checkConcatenateHorizontally(matrix, realMatrix, Array2DRowRealMatrix.class);
    }

    /**
     * Tests the methods that concatenate two matrices vertically.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricMatrix#concatenateVertically(RealMatrix)}<br>
     * {@linkplain ArrayRowSymmetricMatrix#concatenateVertically(RealMatrix, boolean)}<br>
     * </p>
     */
    @Test
    public void testVerticalConcatenation() {
        // Tested matrix
        final double[][] initialData = { { 3.18800, -0.38838, -9.37687, -7.56564 },
                { -0.38838, -9.71053, 6.58601, 0.02720 }, { -9.37687, 6.58601, -0.88513, 2.44720 },
                { -7.56564, 0.02720, 2.44720, -0.30327 } };
        final SymmetricMatrix matrix = new ArrayRowSymmetricMatrix(SymmetryType.LOWER, initialData,
                0., 0.);

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
     * {@linkplain ArrayRowSymmetricMatrix#concatenateDiagonally(RealMatrix, boolean)}<br>
     * </p>
     */
    @Test
    public void testDiagonalConcatenation() {
        // Tested matrix
        final double[][] initialData = { { 9.73081, 0.75276, 7.64393, -0.60385 },
                { 0.75276, -0.01166, 9.60942, -8.06240 }, { 7.64393, 9.60942, 0.40585, 7.90540 },
                { -0.60385, -8.06240, 7.90540, -5.07011 } };
        final SymmetricMatrix matrix = new ArrayRowSymmetricMatrix(SymmetryType.LOWER, initialData,
                0., 0.);

        // Matrix to be concatenated with the tested matrix (6x4)
        final double[][] data = { { 2.21865, 6.73844, -7.29923, 6.34785 },
                { 5.42013, -6.77757, 6.14906, 0.71025 }, { 5.63779, -0.50543, -5.39247, -4.51256 },
                { 2.00983, 3.44139, -3.56028, 5.98328 }, { 4.69695, -9.52195, 6.30628, 3.10651 },
                { -9.61268, 0.64569, -4.60624, 4.38254 } };
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
     * {@linkplain ArrayRowSymmetricMatrix#concatenateHorizontally(RealMatrix)}<br>
     * {@linkplain ArrayRowSymmetricMatrix#concatenateHorizontally(RealMatrix, boolean)}<br>
     * </p>
     */
    @Test
    public void testHorizontalConcatenationIncompatibleMatrix() {
        final RealMatrix matrix = new ArrayRowSymmetricMatrix(4);
        CheckUtils.checkConcatenateHorizontallyIncompatibleMatrix(matrix);
    }

    /**
     * Tests the methods that concatenate two matrices vertically, using matrices with incompatible
     * dimensions.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricMatrix#concatenateVertically(RealMatrix)}<br>
     * {@linkplain ArrayRowSymmetricMatrix#concatenateVertically(RealMatrix, boolean)}<br>
     * </p>
     */
    @Test
    public void testVerticalConcatenationIncompatibleMatrix() {
        final RealMatrix matrix = new ArrayRowSymmetricMatrix(4);
        CheckUtils.checkConcatenateVerticallyIncompatibleMatrix(matrix);
    }

    /**
     * Tests the matrix inversion.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain ArrayRowSymmetricMatrix#getInverse()}
     * </p>
     */
    @Test
    public void testGetMatrixInverse() {
        // Tested matrix
        final double[][] initialData = { { 1.68939, -8.44983, -2.23113, 9.07069 },
                { -8.44983, 4.53264, -3.95730, -3.24908 },
                { -2.23113, -3.95730, 5.16018, 4.33011 }, { 9.07069, -3.24908, 4.33011, 8.29972 } };
        final SymmetricMatrix matrix = new ArrayRowSymmetricMatrix(SymmetryType.LOWER, initialData,
                0., 0.);

        // Test the method
        CheckUtils.checkInverseMatrix(matrix, ArrayRowSymmetricMatrix.class, ABSTOL, RELTOL);
    }

    /**
     * Tests the inversion of a singular matrix.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain ArrayRowSymmetricMatrix#getInverse()}
     * </p>
     */
    @Test
    public void testGetSingularMatrixInverse() {
        final SymmetricMatrix matrix = new ArrayRowSymmetricMatrix(4);
        CheckUtils.checkInverseSingularMatrix(matrix);
    }

    /**
     * Tests the method that sets the decomposition algorithm to use by default when computing the
     * inverse matrix.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricMatrix#getDefaultDecomposition()}<br>
     * {@linkplain ArrayRowSymmetricMatrix#setDefaultDecomposition(Function)}<br>
     * </p>
     */
    @Test
    public void testSetDefaultDecomposition() {
        final RealMatrix matrix = new ArrayRowSymmetricMatrix(4);
        CheckUtils.checkDefaultDecomposition(matrix);
    }

    /**
     * Tests the methods that returns a string representation of the matrix.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricMatrix#toString()}<br>
     * {@linkplain ArrayRowSymmetricMatrix#toString(RealMatrixFormat)}<br>
     * </p>
     */
    @Test
    public void testToString() {
        // Tested matrix
        final double[][] data = { { -6.75885, 3.50933, 0.96515, -6.14160 },
                { 3.50933, 2.00382, 2.91490, 5.56552 }, { 0.96515, 2.91490, 3.92387, -2.23495 },
                { -6.14160, 5.56552, -2.23495, -7.13806 } };
        final SymmetricMatrix matrix = new ArrayRowSymmetricMatrix(SymmetryType.LOWER, data, 0., 0.);

        // Test the method
        CheckUtils.checkToString(matrix);
    }

    /**
     * Tests the methods:<br>
     * {@linkplain ArrayRowSymmetricMatrix#equals(Object)}<br>
     * {@linkplain ArrayRowSymmetricMatrix#hashCode()}
     */
    @Test
    public void testEqualsAndHashCode() {
        RealMatrix other;

        final double[][] data = { { -1.66314, -6.57100, 0.11097, -4.15929 },
                { -6.57100, -6.51087, -9.73482, 9.60554 },
                { 0.11097, -9.73482, 4.20817, -6.24827 }, { -4.15929, 9.60554, -6.24827, -6.91460 } };
        final SymmetricMatrix matrix = new ArrayRowSymmetricMatrix(SymmetryType.LOWER, data, 0., 0.);

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
            for (int j = 0; j <= i; j++) {
                final double[][] modifiedData = CheckUtils.copyArray(data);
                modifiedData[i][j] = 0.;
                modifiedData[j][i] = 0.;

                other = new ArrayRowSymmetricMatrix(SymmetryType.LOWER, modifiedData, 0., 0.);
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
     * {@linkplain ArrayRowSymmetricMatrix#equals(RealMatrix, double, double)}
     * </p>
     */
    @Test
    public void testEqualsWithTolerances() {
        // Tested matrix
        final double[][] data = { { 7.50871, 3.09969, -1.29146, 9.46638 },
                { 3.09969, -6.00054, 4.13639, -5.00880 }, { -1.29146, 4.13639, 8.91596, 9.69174 },
                { 9.46638, -5.00880, 9.69174, -4.16018 } };
        final ArrayRowSymmetricMatrix matrix = new ArrayRowSymmetricMatrix(SymmetryType.LOWER,
                data, 0., 0.);

        // Test the method
        CheckUtils.checkEqualsWithTolerances(matrix);
    }

    /**
     * Tests the serialization/deserialization.
     */
    @Test
    public void testSerialization() {
        // Tested matrix
        final double[][] data = { { -0.52932, -0.27122, 0.51222, -5.67490 },
                { -0.27122, -0.72924, 8.18377, 6.49823 }, { 0.51222, 8.18377, 7.42162, 5.06672 },
                { -5.67490, 6.49823, 5.06672, 1.55134 } };
        final ArrayRowSymmetricMatrix matrix = new ArrayRowSymmetricMatrix(SymmetryType.LOWER,
                data, 0., 0.);

        // Test the serialization
        CheckUtils.checkSerialization(matrix, ArrayRowSymmetricMatrix.class);
    }

    /**
     * Tests the method allowing to visit the elements of the matrix without modifying them.
     *
     * <p>
     * Tested methods<br>
     * {@linkplain ArrayRowSymmetricMatrix#walkInRowOrder(RealMatrixPreservingVisitor)}<br>
     * {@linkplain ArrayRowSymmetricMatrix#walkInRowOrder(RealMatrixPreservingVisitor, int, int, int, int)}
     * <br>
     * {@linkplain ArrayRowSymmetricMatrix#walkInColumnOrder(RealMatrixPreservingVisitor)}<br>
     * {@linkplain ArrayRowSymmetricMatrix#walkInColumnOrder(RealMatrixPreservingVisitor, int, int, int, int)}
     * <br>
     * {@linkplain ArrayRowSymmetricMatrix#walkInOptimizedOrder(RealMatrixPreservingVisitor)}<br>
     * {@linkplain ArrayRowSymmetricMatrix#walkInOptimizedOrder(RealMatrixPreservingVisitor, int, int, int, int)}
     * <br>
     * </p>
     */
    @Test
    public void testPreservingVisitor() {
        // Tested matrix
        final double[][] data = { { -5.69092, -0.05147, -9.30622, -2.00998 },
                { -0.05147, 4.99218, -9.53533, -8.40028 },
                { -9.30622, -9.53533, 9.12427, 3.70791 }, { -2.00998, -8.40028, 3.70791, 6.48046 } };
        final ArrayRowSymmetricMatrix matrix = new ArrayRowSymmetricMatrix(SymmetryType.LOWER,
                data, 0., 0.);

        // Test the methods
        CheckUtils.checkPreservingWalkInRowOrder(matrix);
        CheckUtils.checkPreservingWalkInColumnOrder(matrix);
        CheckUtils.checkPreservingWalkInOptimizedOrder(matrix);
    }

    /**
     * Tests the method allowing to visit and modify the elements of the matrix.
     *
     * <p>
     * Tested methods<br>
     * {@linkplain ArrayRowSymmetricMatrix#walkInRowOrder(RealMatrixChangingVisitor)}<br>
     * {@linkplain ArrayRowSymmetricMatrix#walkInRowOrder(RealMatrixChangingVisitor, int, int, int, int)}
     * <br>
     * {@linkplain ArrayRowSymmetricMatrix#walkInColumnOrder(RealMatrixChangingVisitor)}<br>
     * {@linkplain ArrayRowSymmetricMatrix#walkInColumnOrder(RealMatrixChangingVisitor, int, int, int, int)}
     * <br>
     * {@linkplain ArrayRowSymmetricMatrix#walkInOptimizedOrder(RealMatrixChangingVisitor)}<br>
     * {@linkplain ArrayRowSymmetricMatrix#walkInOptimizedOrder(RealMatrixChangingVisitor, int, int, int, int)}
     * <br>
     * </p>
     */
    @Test
    public void testChangingVisitor() {
        double[][] data;
        SymmetricMatrix matrix;
        RealMatrix expected;

        final int dim = 3;
        final ChangingVisitor visitor = new ChangingVisitor();

        // Visit the entire matrix
        matrix = new ArrayRowSymmetricMatrix(dim);
        matrix.walkInRowOrder(visitor);
        data = new double[][] { { 12.0, 590.0, 16261.0 }, { 590.0, 1794.0, 48812.0 },
                { 16261.0, 48812.0, 146472.0 } };
        expected = new Array2DRowRealMatrix(data);
        CheckUtils.checkEquality(expected, matrix, ABSTOL, RELTOL);
        Assert.assertEquals(9, visitor.getNbVisited());
        Assert.assertEquals(146472, visitor.getResult());

        matrix = new ArrayRowSymmetricMatrix(dim);
        matrix.walkInColumnOrder(visitor);
        data = new double[][] { { 12.0, 562.0, 15503.0 }, { 562.0, 1710.0, 46540.0 },
                { 15503.0, 46540.0, 139656.0 } };
        expected = new Array2DRowRealMatrix(data);
        CheckUtils.checkEquality(expected, matrix, ABSTOL, RELTOL);
        Assert.assertEquals(9, visitor.getNbVisited());
        Assert.assertEquals(139656, visitor.getResult());

        matrix = new ArrayRowSymmetricMatrix(dim);
        matrix.walkInOptimizedOrder(visitor);
        data = new double[][] { { 12.0, 590.0, 16261.0 }, { 590.0, 1794.0, 48812.0 },
                { 16261.0, 48812.0, 146472.0 } };
        expected = new Array2DRowRealMatrix(data);
        CheckUtils.checkEquality(expected, matrix, ABSTOL, RELTOL);
        Assert.assertEquals(9, visitor.getNbVisited());
        Assert.assertEquals(146472, visitor.getResult());

        // Visit part of the matrix
        matrix = new ArrayRowSymmetricMatrix(dim);
        matrix.walkInRowOrder(visitor, 1, 2, 0, 1);
        data = new double[][] { { 0.0, 17.0, 247.0 }, { 17.0, 75.0, 770.0 }, { 247.0, 770.0, 0.0 } };
        expected = new Array2DRowRealMatrix(data);
        CheckUtils.checkEquality(expected, matrix, ABSTOL, RELTOL);
        Assert.assertEquals(4, visitor.getNbVisited());
        Assert.assertEquals(770, visitor.getResult());

        matrix = new ArrayRowSymmetricMatrix(dim);
        matrix.walkInColumnOrder(visitor, 1, 2, 0, 1);
        data = new double[][] { { 0.0, 17.0, 73.0 }, { 17.0, 243.0, 758.0 }, { 73.0, 758.0, 0.0 } };
        expected = new Array2DRowRealMatrix(data);
        CheckUtils.checkEquality(expected, matrix, ABSTOL, RELTOL);
        Assert.assertEquals(4, visitor.getNbVisited());
        Assert.assertEquals(758, visitor.getResult());

        matrix = new ArrayRowSymmetricMatrix(dim);
        matrix.walkInOptimizedOrder(visitor, 1, 2, 0, 1);
        data = new double[][] { { 0.0, 17.0, 247.0 }, { 17.0, 75.0, 770.0 }, { 247.0, 770.0, 0.0 } };
        expected = new Array2DRowRealMatrix(data);
        CheckUtils.checkEquality(expected, matrix, ABSTOL, RELTOL);
        Assert.assertEquals(4, visitor.getNbVisited());
        Assert.assertEquals(770, visitor.getResult());
    }

    /**
     * Tests the getter and setter for the default absolute symmetry threshold.<br>
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricMatrix#getDefaultAbsoluteSymmetryThreshold()}<br>
     * {@linkplain ArrayRowSymmetricMatrix#setDefaultAbsoluteSymmetryThreshold(Double)}<br>
     * </p>
     */
    @Test
    public void testDefaultAbsoluteSymmetryThreshold() {
        // Default value
        Assert.assertNull(ArrayRowSymmetricMatrix.getDefaultAbsoluteSymmetryThreshold());

        // Non-null value
        ArrayRowSymmetricMatrix.setDefaultAbsoluteSymmetryThreshold(1E-10);
        Assert.assertEquals(1E-10, ArrayRowSymmetricMatrix.getDefaultAbsoluteSymmetryThreshold(), 0);

        // Null value
        ArrayRowSymmetricMatrix.setDefaultAbsoluteSymmetryThreshold(null);
        Assert.assertNull(ArrayRowSymmetricMatrix.getDefaultAbsoluteSymmetryThreshold());

        // Ensure an exception is thrown when attempting to set
        // the default threshold to a negative or NaN value
        try {
            ArrayRowSymmetricMatrix.setDefaultAbsoluteSymmetryThreshold(-Double.MIN_VALUE);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals(IllegalArgumentException.class, e.getClass());
            Assert.assertEquals("Absolute threshold is not positive or null.", e.getMessage());
        }

        try {
            ArrayRowSymmetricMatrix.setDefaultAbsoluteSymmetryThreshold(Double.NaN);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals(IllegalArgumentException.class, e.getClass());
            Assert.assertEquals("Input threshold is NaN.", e.getMessage());
        }
    }

    /**
     * Tests the getter and setter for the default relative symmetry threshold.<br>
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricMatrix#getDefaultRelativeSymmetryThreshold()}<br>
     * {@linkplain ArrayRowSymmetricMatrix#setDefaultRelativeSymmetryThreshold(Double)}<br>
     * </p>
     */
    @Test
    public void testDefaultRelativeSymmetryThreshold() {
        // Default value
        Assert.assertNull(ArrayRowSymmetricMatrix.getDefaultRelativeSymmetryThreshold());

        // Non-null value
        ArrayRowSymmetricMatrix.setDefaultRelativeSymmetryThreshold(1E-10);
        Assert.assertEquals(1E-10, ArrayRowSymmetricMatrix.getDefaultRelativeSymmetryThreshold(), 0);

        // Null value
        ArrayRowSymmetricMatrix.setDefaultRelativeSymmetryThreshold(null);
        Assert.assertNull(ArrayRowSymmetricMatrix.getDefaultRelativeSymmetryThreshold());

        // Ensure an exception is thrown when attempting to set
        // the default threshold to a negative or NaN value
        try {
            ArrayRowSymmetricMatrix.setDefaultRelativeSymmetryThreshold(-Double.MIN_VALUE);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals(IllegalArgumentException.class, e.getClass());
            Assert.assertEquals("Relative threshold is not positive or null.", e.getMessage());
        }

        try {
            ArrayRowSymmetricMatrix.setDefaultRelativeSymmetryThreshold(Double.NaN);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals(IllegalArgumentException.class, e.getClass());
            Assert.assertEquals("Input threshold is NaN.", e.getMessage());
        }
    }

    /**
     * Real matrix changing visitor which counts the number of elements in a matrix.
     */
    private static class ChangingVisitor implements RealMatrixChangingVisitor {
        /** Visited elements counter. */
        private long nbVisited = 0;

        /** Hash-code like number built from the visited elements. */
        private long result = 0;

        /** {@inheritDoc} */
        @Override
        public void start(final int rowsIn, final int columnsIn, final int startRowIn,
                final int endRowIn, final int startColumnIn, final int endColumnIn) {
            this.nbVisited = 0;
            this.result = 0;
        }

        /** {@inheritDoc} */
        @Override
        public double visit(final int row, final int column, final double value) {
            this.nbVisited++;
            this.result = 3 * this.result + (5 * (row + 1) + 7 * (column + 1));
            return this.result;
        }

        /** {@inheritDoc} */
        @Override
        public double end() {
            return 0;
        }

        /**
         * Gets the number of visited elements.
         *
         * @return the number of visited elements
         */
        public long getNbVisited() {
            return this.nbVisited;
        }

        /**
         * Gets the hash-code like number built from the visited elements.
         *
         * @return the hash-code like number build from the visited elements
         */
        public long getResult() {
            return this.result;
        }
    }
}
