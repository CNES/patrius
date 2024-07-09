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
import fr.cnes.sirius.patrius.math.exception.NotPositiveException;
import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.linear.ArrayRowSymmetricMatrix.SymmetryType;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Unit tests for {@link ArrayRowSymmetricPositiveMatrix}.
 *
 * @author Pierre Seimandi (GMV)
 */
public class ArrayRowSymmetricPositiveMatrixTest {

    /** Default absolute tolerance used for the tests. */
    private static final double ABSTOL = 0;

    /** Default relative tolerance used for the tests. */
    private static final double RELTOL = 1E-14;

    /** Expected message format for unsupported operation exceptions. */
    private static final String UNSUPPORTED_OPERATION_FORMAT = "unsupported operation";

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
     * Tests creation of new instances by specifying the dimension of the matrix.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#ArrayRowSymmetricPositiveMatrix(int)}
     * </p>
     */
    @Test
    public void testConstructorDimension() {
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(3);
        final RealMatrix expected = new Array2DRowRealMatrix(3, 3);
        CheckUtils.checkEquality(expected, matrix, 0., 0.);
    }

    /**
     * Tests the creation of new instances when the specified dimension is not valid.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#ArrayRowSymmetricPositiveMatrix(int)}
     * </p>
     */
    @Test
    public void testConstructorInvalidDimension() {
        try {
            new ArrayRowSymmetricPositiveMatrix(0);
            Assert.fail();
        } catch (final NotStrictlyPositiveException e) {
            final String expectedMessage = "invalid row dimension: 0 (must be positive)";
            Assert.assertEquals(NotStrictlyPositiveException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests creation of new instances when the provided matrix is symmetric and positive
     * semi-definite.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#ArrayRowSymmetricPositiveMatrix(SymmetryType, double[][])}
     * <br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#ArrayRowSymmetricPositiveMatrix(SymmetryType, double[][], Double, Double, Double, Double)}
     * <br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#ArrayRowSymmetricPositiveMatrix(SymmetryType, RealMatrix)}
     * <br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#ArrayRowSymmetricPositiveMatrix(SymmetryType, RealMatrix, Double, Double, Double, Double)}
     * <br>
     * </p>
     */
    @Test
    public void testConstructorSymmetricPositiveMatrix() {
        final double[][] data = { { 4.80634, 0.47330, -0.05160, -0.64178 },
                { 0.47330, 4.37024, -0.36566, -0.27380 }, { -0.05160, -0.36566, 4.17667, 0.25504 },
                { -0.64178, -0.27380, 0.25504, 5.42638 } };
        final RealMatrix realMatrix = new Array2DRowRealMatrix(data);

        SymmetricPositiveMatrix matrix;

        // Test every type of symmetry
        for (final SymmetryType type : SymmetryType.values()) {
            // From 2D-array
            matrix = new ArrayRowSymmetricPositiveMatrix(type, data);
            CheckUtils.checkEquality(realMatrix, matrix, 0., 0.);

            // From 2D-array with null symmetry and positivity thresholds
            matrix = new ArrayRowSymmetricPositiveMatrix(type, data, null, null, null, null);
            CheckUtils.checkEquality(realMatrix, matrix, 0., 0.);

            // From 2D-array with non-null symmetry and positivity thresholds
            matrix = new ArrayRowSymmetricPositiveMatrix(type, data, 1E-14, 1E-14, 1E-14, 1E-14);
            CheckUtils.checkEquality(realMatrix, matrix, 0., 0.);

            // From matrix
            matrix = new ArrayRowSymmetricPositiveMatrix(type, realMatrix);
            CheckUtils.checkEquality(realMatrix, matrix, 0., 0.);

            // From matrix with non-null symmetry and positivity thresholds
            matrix = new ArrayRowSymmetricPositiveMatrix(type, realMatrix, null, null, null, null);
            CheckUtils.checkEquality(realMatrix, matrix, 0., 0.);

            // From matrix with non-null symmetry and positivity thresholds
            matrix = new ArrayRowSymmetricPositiveMatrix(type, realMatrix, 1E-14, 1E-14, 1E-14,
                    1E-14);
            CheckUtils.checkEquality(realMatrix, matrix, 0., 0.);
        }
    }

    /**
     * Tests creation of new instances when the provided matrix is not symmetric and not positive
     * semi-definite.<br>
     * The symmetry should be enforced by taking into account the lower/upper triangular elements
     * only, or by taking their mean.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#ArrayRowSymmetricPositiveMatrix(SymmetryType, double[][])}
     * <br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#ArrayRowSymmetricPositiveMatrix(SymmetryType, double[][], Double, Double, Double, Double)}
     * <br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#ArrayRowSymmetricPositiveMatrix(SymmetryType, RealMatrix)}
     * <br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#ArrayRowSymmetricPositiveMatrix(SymmetryType, RealMatrix, Double, Double, Double, Double)}
     * <br>
     * </p>
     */
    @Test
    public void testConstructorAsymmetricMatrix() {
        // Asymmetric matrix
        final double[][] initialData = { { 23, 4, 0 }, { 2, 15, -4 }, { 0, -3, 17 } };

        double[][] data;
        RealMatrix expected;
        SymmetricPositiveMatrix matrix;

        // Lower symmetry
        data = new double[][] { { 23, 2, 0 }, { 2, 15, -3 }, { 0, -3, 17 } };
        expected = new Array2DRowRealMatrix(data);

        matrix = new ArrayRowSymmetricPositiveMatrix(SymmetryType.LOWER, initialData);
        CheckUtils.checkEquality(expected, matrix, 0., 0.);

        matrix = new ArrayRowSymmetricPositiveMatrix(SymmetryType.LOWER, initialData,
                Double.POSITIVE_INFINITY, null, null, null);
        CheckUtils.checkEquality(expected, matrix, 0., 0.);

        matrix = new ArrayRowSymmetricPositiveMatrix(SymmetryType.LOWER, initialData, null,
                Double.POSITIVE_INFINITY, null, null);
        CheckUtils.checkEquality(expected, matrix, 0., 0.);

        // Upper symmetry
        data = new double[][] { { 23, 4, 0 }, { 4, 15, -4 }, { 0, -4, 17 } };
        expected = new Array2DRowRealMatrix(data);

        matrix = new ArrayRowSymmetricPositiveMatrix(SymmetryType.UPPER, initialData);
        CheckUtils.checkEquality(expected, matrix, 0., 0.);

        matrix = new ArrayRowSymmetricPositiveMatrix(SymmetryType.UPPER, initialData,
                Double.POSITIVE_INFINITY, null, null, null);
        CheckUtils.checkEquality(expected, matrix, 0., 0.);

        matrix = new ArrayRowSymmetricPositiveMatrix(SymmetryType.UPPER, initialData, null,
                Double.POSITIVE_INFINITY, null, null);
        CheckUtils.checkEquality(expected, matrix, 0., 0.);

        // Mean symmetry
        data = new double[][] { { 23, 3, 0 }, { 3, 15, -3.5 }, { 0, -3.5, 17 } };
        expected = new Array2DRowRealMatrix(data);

        matrix = new ArrayRowSymmetricPositiveMatrix(SymmetryType.MEAN, initialData);
        CheckUtils.checkEquality(expected, matrix, 0., 0.);

        matrix = new ArrayRowSymmetricPositiveMatrix(SymmetryType.MEAN, initialData,
                Double.POSITIVE_INFINITY, null, null, null);
        CheckUtils.checkEquality(expected, matrix, 0., 0.);

        matrix = new ArrayRowSymmetricPositiveMatrix(SymmetryType.MEAN, initialData, null,
                Double.POSITIVE_INFINITY, null, null);
        CheckUtils.checkEquality(expected, matrix, 0., 0.);
    }

    /**
     * Tests the creation of new instances when the provided data array is {@code null}.<br>
     * An exception should be thrown.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#ArrayRowSymmetricPositiveMatrix(SymmetryType, double[][])}
     * <br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#ArrayRowSymmetricPositiveMatrix(SymmetryType, double[][], Double, Double, Double, Double)}
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
                new ArrayRowSymmetricPositiveMatrix(symmetryType, nullData);
                Assert.fail();
            } catch (final NullArgumentException e) {
                Assert.assertEquals(NullArgumentException.class, e.getClass());
                Assert.assertEquals(expectedMessage, e.getMessage());
            }

            try {
                new ArrayRowSymmetricPositiveMatrix(symmetryType, nullData, null, null, null, null);
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
     * {@linkplain ArrayRowSymmetricPositiveMatrix#ArrayRowSymmetricPositiveMatrix(SymmetryType, double[][])}
     * <br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#ArrayRowSymmetricPositiveMatrix(SymmetryType, double[][], Double, Double, Double, Double)}
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
                new ArrayRowSymmetricPositiveMatrix(symmetryType, emptyRowData);
                Assert.fail();
            } catch (final NoDataException e) {
                Assert.assertEquals(NoDataException.class, e.getClass());
                Assert.assertEquals(expectedMessage1, e.getMessage());
            }

            try {
                new ArrayRowSymmetricPositiveMatrix(symmetryType, emptyRowData, null, null, null,
                        null);
                Assert.fail();
            } catch (final NoDataException e) {
                Assert.assertEquals(NoDataException.class, e.getClass());
                Assert.assertEquals(expectedMessage1, e.getMessage());
            }

            // One empty row
            try {
                new ArrayRowSymmetricPositiveMatrix(symmetryType, emptyColumnData);
                Assert.fail();
            } catch (final NoDataException e) {
                Assert.assertEquals(NoDataException.class, e.getClass());
                Assert.assertEquals(expectedMessage2, e.getMessage());
            }

            try {
                new ArrayRowSymmetricPositiveMatrix(symmetryType, emptyColumnData, null, null,
                        null, null);
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
     * {@linkplain ArrayRowSymmetricPositiveMatrix#ArrayRowSymmetricPositiveMatrix(SymmetryType, double[][])}
     * <br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#ArrayRowSymmetricPositiveMatrix(SymmetryType, double[][], Double, Double, Double, Double)}
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
                new ArrayRowSymmetricPositiveMatrix(symmetryType, invalidData);
                Assert.fail();
            } catch (final DimensionMismatchException e) {
                Assert.assertEquals(DimensionMismatchException.class, e.getClass());
                Assert.assertEquals(expectedMessage, e.getMessage());
            }

            try {
                new ArrayRowSymmetricPositiveMatrix(symmetryType, invalidData, null, null, null,
                        null);
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
     * {@linkplain ArrayRowSymmetricPositiveMatrix#ArrayRowSymmetricPositiveMatrix(SymmetryType, RealMatrix)}
     * <br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#ArrayRowSymmetricPositiveMatrix(SymmetryType, RealMatrix, Double, Double, Double, Double)}
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
                new ArrayRowSymmetricPositiveMatrix(symmetryType, nullMatrix);
                Assert.fail();
            } catch (final NullArgumentException e) {
                Assert.assertEquals(NullArgumentException.class, e.getClass());
                Assert.assertEquals(expectedMessage, e.getMessage());
            }

            try {
                new ArrayRowSymmetricPositiveMatrix(symmetryType, nullMatrix, null, null, null,
                        null);
                Assert.fail();
            } catch (final NullArgumentException e) {
                Assert.assertEquals(NullArgumentException.class, e.getClass());
                Assert.assertEquals(expectedMessage, e.getMessage());
            }
        }
    }

    /**
     * Tests creation of new instances when the provided matrix is not a square matrix.<br>
     * An exception should be thrown.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#ArrayRowSymmetricPositiveMatrix(SymmetryType, RealMatrix)}
     * </p>
     */
    @Test
    public void testConstructorNonSquareMatrix() {
        final double[][] data = { { 1, -1 }, { -1, 1 }, { 0, 0 } };
        final RealMatrix realMatrix = new Array2DRowRealMatrix(data);

        final String expectedMessage = "non square (2x3) matrix";

        // Test every type of symmetry
        for (final SymmetryType symmetryType : SymmetryType.values()) {
            try {
                new ArrayRowSymmetricPositiveMatrix(symmetryType, data);
                Assert.fail();
            } catch (final NonSquareMatrixException e) {
                Assert.assertEquals(NonSquareMatrixException.class, e.getClass());
                Assert.assertEquals(expectedMessage, e.getMessage());
            }

            try {
                new ArrayRowSymmetricPositiveMatrix(symmetryType, data, null, null, null, null);
                Assert.fail();
            } catch (final NonSquareMatrixException e) {
                Assert.assertEquals(NonSquareMatrixException.class, e.getClass());
                Assert.assertEquals(expectedMessage, e.getMessage());
            }

            try {
                new ArrayRowSymmetricPositiveMatrix(symmetryType, realMatrix);
                Assert.fail();
            } catch (final NonSquareMatrixException e) {
                Assert.assertEquals(NonSquareMatrixException.class, e.getClass());
                Assert.assertEquals(expectedMessage, e.getMessage());
            }

            try {
                new ArrayRowSymmetricPositiveMatrix(symmetryType, realMatrix, null, null, null,
                        null);
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
     * {@linkplain ArrayRowSymmetricPositiveMatrix#ArrayRowSymmetricPositiveMatrix(double[], boolean)}
     * <br>
     * </p>
     */
    @Test
    public void testConstructorNullLowerElementsDataArray() {
        final double[] nullData = null;
        final String expectedMessage = "the supplied array is null";

        try {
            new ArrayRowSymmetricPositiveMatrix(nullData, true);
            Assert.fail();
        } catch (final NullArgumentException e) {
            Assert.assertEquals(NullArgumentException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            new ArrayRowSymmetricPositiveMatrix(nullData, false);
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
     * {@linkplain ArrayRowSymmetricPositiveMatrix#ArrayRowSymmetricPositiveMatrix(double[], boolean)}
     * <br>
     * </p>
     */
    @Test
    public void testConstructorEmptyLowerElementsDataArray() {
        final double[] emptyData = new double[0];
        final String expectedMessage = "the supplied array is empty";

        try {
            new ArrayRowSymmetricPositiveMatrix(emptyData, true);
            Assert.fail();
        } catch (final NoDataException e) {
            Assert.assertEquals(NoDataException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            new ArrayRowSymmetricPositiveMatrix(emptyData, false);
            Assert.fail();
        } catch (final NoDataException e) {
            Assert.assertEquals(NoDataException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the creation of new instances when the specified absolute or relative symmetry
     * thresholds are {@code NaN}.<br>
     * An exception should be thrown.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#ArrayRowSymmetricPositiveMatrix(SymmetryType, double[][], Double, Double, Double, Double)}
     * {@linkplain ArrayRowSymmetricPositiveMatrix#ArrayRowSymmetricPositiveMatrix(SymmetryType, RealMatrix, Double, Double, Double, Double)}
     * </p>
     */
    @Test
    public void testConstructorNaNSymmetryThresholds() {
        final double[][] data = { { 2, -1, 0 }, { -1, 2, -1 }, { 0, -1, 2 } };
        final RealMatrix realMatrix = new Array2DRowRealMatrix(data);

        final String expectedMessage = "Input threshold is NaN.";

        for (final SymmetryType symmetryType : SymmetryType.values()) {
            // NaN absolute symmetry threshold
            try {
                new ArrayRowSymmetricPositiveMatrix(symmetryType, data, Double.NaN, 0., 0., 0.);
                Assert.fail();
            } catch (final IllegalArgumentException e) {
                Assert.assertEquals(IllegalArgumentException.class, e.getClass());
                Assert.assertEquals(expectedMessage, e.getMessage());
            }

            try {
                new ArrayRowSymmetricPositiveMatrix(symmetryType, realMatrix, Double.NaN, 0., 0.,
                        0.);
                Assert.fail();
            } catch (final IllegalArgumentException e) {
                Assert.assertEquals(IllegalArgumentException.class, e.getClass());
                Assert.assertEquals(expectedMessage, e.getMessage());
            }

            // NaN relative symmetry threshold
            try {
                new ArrayRowSymmetricPositiveMatrix(symmetryType, data, 0., Double.NaN, 0., 0.);
                Assert.fail();
            } catch (final IllegalArgumentException e) {
                Assert.assertEquals(IllegalArgumentException.class, e.getClass());
                Assert.assertEquals(expectedMessage, e.getMessage());
            }

            try {
                new ArrayRowSymmetricPositiveMatrix(symmetryType, realMatrix, 0., Double.NaN, 0.,
                        0.);
                Assert.fail();
            } catch (final IllegalArgumentException e) {
                Assert.assertEquals(IllegalArgumentException.class, e.getClass());
                Assert.assertEquals(expectedMessage, e.getMessage());
            }
        }
    }

    /**
     * Tests the creation of new instances when the specified absolute or relative symmetry
     * thresholds are negative.<br>
     * An exception should be thrown.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#ArrayRowSymmetricPositiveMatrix(SymmetryType, double[][], Double, Double, Double, Double)}
     * {@linkplain ArrayRowSymmetricPositiveMatrix#ArrayRowSymmetricPositiveMatrix(SymmetryType, RealMatrix, Double, Double, Double, Double)}
     * </p>
     */
    @Test
    public void testConstructorNegativeSymmetryThresholds() {
        final double[][] data = { { 2, -1, 0 }, { -1, 2, -1 }, { 0, -1, 2 } };
        final RealMatrix realMatrix = new Array2DRowRealMatrix(data);

        final String expectedMessage1 = "Absolute threshold is not positive or null.";
        final String expectedMessage2 = "Relative threshold is not positive or null.";

        for (final SymmetryType symmetryType : SymmetryType.values()) {
            // Negative absolute symmetry threshold
            try {
                new ArrayRowSymmetricPositiveMatrix(symmetryType, data, -Double.MIN_VALUE, 0., 0.,
                        0.);
                Assert.fail();
            } catch (final IllegalArgumentException e) {
                Assert.assertEquals(IllegalArgumentException.class, e.getClass());
                Assert.assertEquals(expectedMessage1, e.getMessage());
            }

            try {
                new ArrayRowSymmetricPositiveMatrix(symmetryType, realMatrix, -Double.MIN_VALUE,
                        0., 0., 0.);
                Assert.fail();
            } catch (final IllegalArgumentException e) {
                Assert.assertEquals(IllegalArgumentException.class, e.getClass());
                Assert.assertEquals(expectedMessage1, e.getMessage());
            }

            // Negative relative symmetry threshold
            try {
                new ArrayRowSymmetricPositiveMatrix(symmetryType, data, 0., -Double.MIN_VALUE, 0.,
                        0.);
                Assert.fail();
            } catch (final IllegalArgumentException e) {
                Assert.assertEquals(IllegalArgumentException.class, e.getClass());
                Assert.assertEquals(expectedMessage2, e.getMessage());
            }

            try {
                new ArrayRowSymmetricPositiveMatrix(symmetryType, realMatrix, 0.,
                        -Double.MIN_VALUE, 0., 0.);
                Assert.fail();
            } catch (final IllegalArgumentException e) {
                Assert.assertEquals(IllegalArgumentException.class, e.getClass());
                Assert.assertEquals(expectedMessage2, e.getMessage());
            }
        }
    }

    /**
     * Tests the creation of new instances when the specified absolute or relative positivity
     * thresholds are {@code NaN}.<br>
     * An exception should be thrown.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#ArrayRowSymmetricPositiveMatrix(SymmetryType, double[][], Double, Double, Double, Double)}
     * {@linkplain ArrayRowSymmetricPositiveMatrix#ArrayRowSymmetricPositiveMatrix(SymmetryType, RealMatrix, Double, Double, Double, Double)}
     * </p>
     */
    @Test
    public void testConstructorNaNPositivityThreshold() {
        final double[][] data = { { 2, -1, 0 }, { -1, 2, -1 }, { 0, -1, 2 } };
        final RealMatrix realMatrix = new Array2DRowRealMatrix(data);

        final String expectedMessage = "Input threshold is NaN.";

        for (final SymmetryType symmetryType : SymmetryType.values()) {
            // Absolute positivity threshold
            try {
                new ArrayRowSymmetricPositiveMatrix(symmetryType, data, 0., 0., Double.NaN, 0.);
                Assert.fail();
            } catch (final IllegalArgumentException e) {
                Assert.assertEquals(IllegalArgumentException.class, e.getClass());
                Assert.assertEquals(expectedMessage, e.getMessage());
            }

            try {
                new ArrayRowSymmetricPositiveMatrix(symmetryType, realMatrix, 0., 0., Double.NaN,
                        0.);
                Assert.fail();
            } catch (final IllegalArgumentException e) {
                Assert.assertEquals(IllegalArgumentException.class, e.getClass());
                Assert.assertEquals(expectedMessage, e.getMessage());
            }

            // Relative positivity threshold
            try {
                new ArrayRowSymmetricPositiveMatrix(symmetryType, data, 0., 0., 0., Double.NaN);
                Assert.fail();
            } catch (final IllegalArgumentException e) {
                Assert.assertEquals(IllegalArgumentException.class, e.getClass());
                Assert.assertEquals(expectedMessage, e.getMessage());
            }

            try {
                new ArrayRowSymmetricPositiveMatrix(symmetryType, realMatrix, 0., 0., 0.,
                        Double.NaN);
                Assert.fail();
            } catch (final IllegalArgumentException e) {
                Assert.assertEquals(IllegalArgumentException.class, e.getClass());
                Assert.assertEquals(expectedMessage, e.getMessage());
            }
        }
    }

    /**
     * Tests the creation of new instances when the specified absolute or relative positivity
     * thresholds are negative.<br>
     * An exception should be thrown.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#ArrayRowSymmetricPositiveMatrix(SymmetryType, double[][], Double, Double, Double, Double)}
     * {@linkplain ArrayRowSymmetricPositiveMatrix#ArrayRowSymmetricPositiveMatrix(SymmetryType, RealMatrix, Double, Double, Double, Double)}
     * </p>
     */
    @Test
    public void testConstructorNegativePositivityThresholds() {
        final double[][] data = { { 2, -1, 0 }, { -1, 2, -1 }, { 0, -1, 2 } };
        final RealMatrix realMatrix = new Array2DRowRealMatrix(data);

        final String expectedMessage1 = "Absolute threshold is not positive or null.";
        final String expectedMessage2 = "Relative threshold is not positive or null.";

        for (final SymmetryType symmetryType : SymmetryType.values()) {
            // Absolute positivity threshold
            try {
                new ArrayRowSymmetricPositiveMatrix(symmetryType, data, 0., 0., -Double.MIN_VALUE,
                        0.);
                Assert.fail();
            } catch (final IllegalArgumentException e) {
                Assert.assertEquals(IllegalArgumentException.class, e.getClass());
                Assert.assertEquals(expectedMessage1, e.getMessage());
            }

            try {
                new ArrayRowSymmetricPositiveMatrix(symmetryType, realMatrix, 0., 0.,
                        -Double.MIN_VALUE, 0.);
                Assert.fail();
            } catch (final IllegalArgumentException e) {
                Assert.assertEquals(IllegalArgumentException.class, e.getClass());
                Assert.assertEquals(expectedMessage1, e.getMessage());
            }

            // Relative positivity threshold
            try {
                new ArrayRowSymmetricPositiveMatrix(symmetryType, data, 0., 0., 0.,
                        -Double.MIN_VALUE);
                Assert.fail();
            } catch (final IllegalArgumentException e) {
                Assert.assertEquals(IllegalArgumentException.class, e.getClass());
                Assert.assertEquals(expectedMessage2, e.getMessage());
            }

            try {
                new ArrayRowSymmetricPositiveMatrix(symmetryType, realMatrix, 0., 0., 0.,
                        -Double.MIN_VALUE);
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
     * Tested method:<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#createMatrix(int, int)}
     * </p>
     */
    @Test
    public void testCreateMatrix() {
        final int dim = 4;
        final ArrayRowSymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(dim);
        final ArrayRowSymmetricPositiveMatrix result = matrix.createMatrix(dim, dim);

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
     * {@linkplain ArrayRowSymmetricPositiveMatrix#createIdentityMatrix(int)}
     */
    @Test
    public void testCreateIdentityMatrix() {
        for (int dim = 1; dim <= 10; dim++) {
            final ArrayRowSymmetricMatrix matrix = ArrayRowSymmetricPositiveMatrix
                    .createIdentityMatrix(dim);
            final RealMatrix expected = MatrixUtils.createRealIdentityMatrix(dim);
            CheckUtils.checkEquality(expected, matrix, ABSTOL, RELTOL);
        }

        // Invalid dimension
        try {
            ArrayRowSymmetricPositiveMatrix.createIdentityMatrix(0);
            Assert.fail();
        } catch (final NotStrictlyPositiveException e) {
            final String expectedMessage = "invalid row dimension: 0 (must be positive)";
            Assert.assertEquals(NotStrictlyPositiveException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the method checking if a matrix is square.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#isSquare()}
     * </p>
     */
    @Test
    public void testIsSquare() {
        SymmetricPositiveMatrix matrix;

        // Matrix filled with zeros
        matrix = new ArrayRowSymmetricPositiveMatrix(8);
        Assert.assertTrue(matrix.isSquare());

        final double[][] data = { { 2, -1, 0 }, { -1, 2, -1 }, { 0, -1, 2 } };
        matrix = new ArrayRowSymmetricPositiveMatrix(SymmetryType.LOWER, data, 0., 0., 0., 0.);
        Assert.assertTrue(matrix.isSquare());
    }

    /**
     * Tests the methods checking if a matrix is diagonal.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#isDiagonal(double)}<br>
     * </p>
     */
    @Test
    public void testIsDiagonal() {
        SymmetricPositiveMatrix matrix;

        // Matrix filled with zeros
        matrix = new ArrayRowSymmetricPositiveMatrix(8);
        Assert.assertTrue(matrix.isDiagonal(0.));

        // Diagonal matrix, except for one off-diagonal element
        final double[][] data = { { 2, 0., 0. }, { -1E-2, 2, 0. }, { 0, -1E-2, 5 } };
        matrix = new ArrayRowSymmetricPositiveMatrix(SymmetryType.LOWER, data, null, null, 0., 0.);
        final double diagonalityThreshold = 1E-2;
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
     * {@linkplain ArrayRowSymmetricPositiveMatrix#isSymmetric()}<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#isSymmetric(double)}<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#isSymmetric(double, double)}<br>
     * </p>
     */
    @Test
    public void testIsSymmetric() {
        SymmetricPositiveMatrix matrix;
        final double infinity = Double.POSITIVE_INFINITY;

        // Matrix filled with zeros
        matrix = new ArrayRowSymmetricPositiveMatrix(8);
        Assert.assertTrue(matrix.isSymmetric());
        Assert.assertTrue(matrix.isSymmetric(0.));
        Assert.assertTrue(matrix.isSymmetric(0., 0.));
        Assert.assertTrue(matrix.isSymmetric(-infinity));
        Assert.assertTrue(matrix.isSymmetric(-infinity, -infinity));

        // Standard matrix
        final double[][] data = { { 2, 0., 0. }, { -1, 2, 0. }, { 0, -1, 5 } };
        matrix = new ArrayRowSymmetricPositiveMatrix(SymmetryType.LOWER, data, null, null, 0., 0.);
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
     * {@linkplain ArrayRowSymmetricPositiveMatrix#isAntisymmetric(double, double)}<br>
     * </p>
     */
    @Test
    public void testIsAntisymmetric() {
        double[][] data;
        SymmetricPositiveMatrix matrix;

        // Matrix filled with zeros
        matrix = new ArrayRowSymmetricPositiveMatrix(8);
        Assert.assertTrue(matrix.isAntisymmetric(0., 0.));

        // Standard matrix
        data = new double[][] { { 2, 0., 0. }, { -1, 2, 0. }, { 0, -1, 5 } };
        matrix = new ArrayRowSymmetricPositiveMatrix(SymmetryType.LOWER, data, null, null, 0., 0.);
        Assert.assertFalse(matrix.isAntisymmetric(0., 0.));

        // The off-diagonal elements are equal to zero, but not the diagonal elements
        data = new double[][] { { 1E-10, 0., 0. }, { 0., 1E-8, 0. }, { 0., 0., 1E-6 } };
        matrix = new ArrayRowSymmetricPositiveMatrix(SymmetryType.LOWER, data, null, null, 0., 0.);
        Assert.assertFalse(matrix.isAntisymmetric(0., 0.));
        Assert.assertFalse(matrix.isAntisymmetric(0., 1E-6 - MathLib.ulp(1E-6)));
        Assert.assertTrue(matrix.isAntisymmetric(0., 1E-6));
        Assert.assertFalse(matrix.isAntisymmetric(1E-6, 0.));

        // The diagonal elements are equal to zero, but not the off-diagonal elements
        data = new double[][] { { 0., 1E-10, 0. }, { 1E-8, 0., 0. }, { 1E-11, 1E-7, 0. } };
        matrix = new ArrayRowSymmetricPositiveMatrix(SymmetryType.LOWER, data, null, null, null,
                null);
        Assert.assertFalse(matrix.isAntisymmetric(0., 0.));
        Assert.assertFalse(matrix.isAntisymmetric(0., 2E-7 - MathLib.ulp(2E-7)));
        Assert.assertTrue(matrix.isAntisymmetric(0., 2E-7));
        Assert.assertFalse(matrix.isAntisymmetric(2. - MathLib.ulp(2.), 0.));
        Assert.assertTrue(matrix.isAntisymmetric(2., 0.));
    }

    /**
     * Tests the methods checking if a matrix is orthogonal.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#isOrthogonal(double, double)}<br>
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
        final SymmetricPositiveMatrix orthogonalMatrix = ArrayRowSymmetricPositiveMatrix
                .createIdentityMatrix(4);
        Assert.assertTrue(orthogonalMatrix.isOrthogonal(0., 0.));

        // The norm of the first row is slightly different from 1
        data = orthogonalMatrix.getData();
        data[0][0] += 1E-4;
        matrix = new ArrayRowSymmetricPositiveMatrix(SymmetryType.LOWER, data, 0., 0., 0., 0.);

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
        matrix = new ArrayRowSymmetricPositiveMatrix(SymmetryType.LOWER, data, null, null, 0., 0.);
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
     * Tests the checks performed by the constructor when symmetry thresholds are provided.<br>
     * The matrix should be built without error if the symmetry thresholds are large enough,
     * otherwise an exception should be thrown.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#ArrayRowSymmetricPositiveMatrix(SymmetryType, double[][], Double, Double, Double, Double)}
     * {@linkplain ArrayRowSymmetricPositiveMatrix#ArrayRowSymmetricPositiveMatrix(SymmetryType, RealMatrix, Double, Double, Double, Double)}
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
            new ArrayRowSymmetricPositiveMatrix(symmetryType, data, absoluteThreshold - 1E-14,
                    relativeThreshold, null, null);
            new ArrayRowSymmetricPositiveMatrix(symmetryType, realMatrix,
                    absoluteThreshold - 1E-14, relativeThreshold, null, null);

            // The relative symmetry threshold is too low,
            // but the absolute symmetry threshold is high enough.
            new ArrayRowSymmetricPositiveMatrix(symmetryType, data, absoluteThreshold,
                    relativeThreshold - 1E-14, null, null);
            new ArrayRowSymmetricPositiveMatrix(symmetryType, realMatrix, absoluteThreshold,
                    relativeThreshold - 1E-14, null, null);

            // The absolute symmetry threshold is too low
            // and no relative symmetry threshold is specified.
            try {
                new ArrayRowSymmetricPositiveMatrix(symmetryType, data, absoluteThreshold - 1E-14,
                        null, null, null);
                Assert.fail();
            } catch (final NonSymmetricMatrixException e) {
                final String expectedMessage = "non symmetric matrix: the difference between entries at (0,0) and (0,0) is larger than 0";
                Assert.assertEquals(NonSymmetricMatrixException.class, e.getClass());
                Assert.assertEquals(expectedMessage, e.getMessage());
            }

            try {
                new ArrayRowSymmetricPositiveMatrix(symmetryType, realMatrix,
                        absoluteThreshold - 1E-14, null, null, null);
                Assert.fail();
            } catch (final NonSymmetricMatrixException e) {
                final String expectedMessage = "non symmetric matrix: the difference between entries at (0,0) and (0,0) is larger than 0";
                Assert.assertEquals(NonSymmetricMatrixException.class, e.getClass());
                Assert.assertEquals(expectedMessage, e.getMessage());
            }

            // The relative symmetry threshold is too low
            // and no absolute symmetry threshold is specified.
            try {
                new ArrayRowSymmetricPositiveMatrix(symmetryType, data, null,
                        relativeThreshold - 1E-14, null, null);
                Assert.fail();
            } catch (final NonSymmetricMatrixException e) {
                final String expectedMessage = "non symmetric matrix: the difference between entries at (0,0) and (0,0) is larger than 0.002";
                Assert.assertEquals(NonSymmetricMatrixException.class, e.getClass());
                Assert.assertEquals(expectedMessage, e.getMessage());
            }

            try {
                new ArrayRowSymmetricPositiveMatrix(symmetryType, realMatrix, null,
                        relativeThreshold - 1E-14, null, null);
                Assert.fail();
            } catch (final NonSymmetricMatrixException e) {
                final String expectedMessage = "non symmetric matrix: the difference between entries at (0,0) and (0,0) is larger than 0.002";
                Assert.assertEquals(NonSymmetricMatrixException.class, e.getClass());
                Assert.assertEquals(expectedMessage, e.getMessage());
            }
        }
    }

    /**
     * Tests the checks performed by the constructor when positivity thresholds are specified.<br>
     * The matrix should be built without error if the positivity thresholds are large enough,
     * otherwise an exception should be thrown.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#ArrayRowSymmetricPositiveMatrix(SymmetryType, double[][], Double, Double, Double, Double)}
     * {@linkplain ArrayRowSymmetricPositiveMatrix#ArrayRowSymmetricPositiveMatrix(SymmetryType, RealMatrix, Double, Double, Double, Double)}
     * </p>
     */
    @Test
    public void testPositivityChecks() {
        // Symmetric matrix with a slightly negative eigenvalue
        final double[][] data = { { 10., 0., 0. }, { 0., 1., 0. }, { 0., 0., -1E-10 } };
        final RealMatrix realMatrix = new Array2DRowRealMatrix(data);
        final double norm = realMatrix.getNorm();

        // Expected exception message
        for (final SymmetryType symmetryType : SymmetryType.values()) {
            // The absolute positivity threshold is high enough.
            new ArrayRowSymmetricPositiveMatrix(symmetryType, data, null, null, 1E-10 + 1E-14, null);
            new ArrayRowSymmetricPositiveMatrix(symmetryType, realMatrix, null, null,
                    1E-10 + 1E-14, null);

            // The relative positivity threshold is high enough.
            new ArrayRowSymmetricPositiveMatrix(symmetryType, data, null, null, null,
                    1E-10 / norm + 1E-14);
            new ArrayRowSymmetricPositiveMatrix(symmetryType, realMatrix, null, null, null,
                    1E-10 / norm + 1E-14);

            // The absolute positivity threshold is not high enough.
            try {
                new ArrayRowSymmetricPositiveMatrix(symmetryType, data, null, null, 1E-10 - 1E-14,
                        null);
                Assert.fail();
            } catch (final NonPositiveDefiniteMatrixException e) {
                final String expectedMessage = "-0 is smaller than, or equal to, the minimum (0): not positive definite matrix: value -0 at index 2";
                Assert.assertEquals(NonPositiveDefiniteMatrixException.class, e.getClass());
                Assert.assertEquals(expectedMessage, e.getMessage());
            }

            try {
                new ArrayRowSymmetricPositiveMatrix(symmetryType, realMatrix, null, null,
                        1E-10 - 1E-14, null);
                Assert.fail();
            } catch (final NonPositiveDefiniteMatrixException e) {
                final String expectedMessage = "-0 is smaller than, or equal to, the minimum (0): not positive definite matrix: value -0 at index 2";
                Assert.assertEquals(NonPositiveDefiniteMatrixException.class, e.getClass());
                Assert.assertEquals(expectedMessage, e.getMessage());
            }

            // The relative positivity threshold is not high enough.
            try {
                new ArrayRowSymmetricPositiveMatrix(symmetryType, data, null, null, null,
                        1E-10 / norm - 1E-14);
                Assert.fail();
            } catch (final NonPositiveDefiniteMatrixException e) {
                final String expectedMessage = "-0 is smaller than, or equal to, the minimum (0): not positive definite matrix: value -0 at index 2";
                Assert.assertEquals(NonPositiveDefiniteMatrixException.class, e.getClass());
                Assert.assertEquals(expectedMessage, e.getMessage());
            }

            try {
                new ArrayRowSymmetricPositiveMatrix(symmetryType, realMatrix, null, null, null,
                        1E-10 / norm - 1E-14);
                Assert.fail();
            } catch (final NonPositiveDefiniteMatrixException e) {
                final String expectedMessage = "-0 is smaller than, or equal to, the minimum (0): not positive definite matrix: value -0 at index 2";
                Assert.assertEquals(NonPositiveDefiniteMatrixException.class, e.getClass());
                Assert.assertEquals(expectedMessage, e.getMessage());
            }
        }
    }

    /**
     * Tests the method that returns the minimum value in the matrix.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#getMin()}
     * </p>
     */
    @Test
    public void testGetMin() {
        // Tested matrix
        final double[][] data = { { 1.92515, -1.07311, -1.89541, -0.37143 },
                { -1.07311, 6.98741, 0.27894, -0.81209 }, { -1.89541, 0.27894, 5.54273, 1.57497 },
                { -0.37143, -0.81209, 1.57497, 4.89208 } };
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, data, 0., 0., 0., 0.);

        // Test the method
        CheckUtils.checkMinimum(matrix, 0., 0.);
        Assert.assertEquals(-1.89541, matrix.getMin(), 0.);
    }

    /**
     * Tests the method that returns the maximum value in the matrix.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#getMax()}
     * </p>
     */
    @Test
    public void testGetMax() {
        // Tested matrix
        final double[][] data = { { 5.03456, -0.95445, -0.58029, 2.27232 },
                { -0.95445, 4.70971, 1.61827, -0.31360 }, { -0.58029, 1.61827, 7.10556, 0.59123 },
                { 2.27232, -0.31360, 0.59123, 6.31869 } };
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, data, 0., 0., 0., 0.);

        // Test the method
        CheckUtils.checkMaximum(matrix, 0., 0.);
        Assert.assertEquals(7.10556, matrix.getMax(), 0.);
    }

    /**
     * Tests the method that computes the trace of the matrix.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#getTrace()}
     * </p>
     */
    @Test
    public void testGetTrace() {
        // Tested matrix
        final double[][] data = { { 4.44454, 0.01725, -1.86205, -0.28456 },
                { 0.01725, 2.77933, -0.94819, -1.68986 },
                { -1.86205, -0.94819, 3.82674, -1.20297 },
                { -0.28456, -1.68986, -1.20297, 7.50787 } };
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, data, 0., 0., 0., 0.);

        // Test the method
        CheckUtils.checkTrace(matrix, 0., 0.);
        CheckUtils.checkEquality(18.55848, matrix.getTrace(), ABSTOL, RELTOL);
    }

    /**
     * Tests the method that computes the norm of the matrix (maximum absolute row sum).
     *
     * <p>
     * Tested method:<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#getNorm()}
     * </p>
     */
    @Test
    public void testGetNorm() {
        // Tested matrix
        final double[][] data = { { 3.38342, 0.36191, -1.55233, -0.71565 },
                { 0.36191, 4.34185, 0.57492, -1.45938 }, { -1.55233, 0.57492, 4.35158, -0.18221 },
                { -0.71565, -1.45938, -0.18221, 2.99802 } };
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, data, 0., 0., 0., 0.);

        // Test the method
        CheckUtils.checkNorm(matrix, ABSTOL, RELTOL);
        CheckUtils.checkEquality(6.73806, matrix.getNorm(), ABSTOL, RELTOL);
    }

    /**
     * Tests the method that computes the Frobenius norm of the matrix.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#getFrobeniusNorm()}
     * </p>
     */
    @Test
    public void testGetFrobeniusNorm() {
        // Tested matrix
        final double[][] data = { { 4.49482, 0.20939, 0.31309, 2.03370 },
                { 0.20939, 7.11787, -0.25763, -0.46662 }, { 0.31309, -0.25763, 6.58572, -0.20194 },
                { 2.03370, -0.46662, -0.20194, 4.71094 } };
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, data, 0., 0., 0., 0.);

        // Test the method
        CheckUtils.checkFrobeniusNorm(matrix, ABSTOL, RELTOL);
        CheckUtils.checkEquality(12.06803747928801, matrix.getFrobeniusNorm(), ABSTOL, RELTOL);
    }

    /**
     * Tests the method that returns the entries of the matrix in a 2D data array.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#getData()}<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#getData(boolean)}<br>
     * </p>
     */
    @Test
    public void testGetData() {
        double[][] result;

        // Tested matrix
        final double[][] data = { { 5.57993, 0.37825, 0.68137, -0.67463 },
                { 0.37825, 6.00670, 1.08418, 1.41404 }, { 0.68137, 1.08418, 7.05114, -0.85617 },
                { -0.67463, 1.41404, -0.85617, 6.65540 } };
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, data, 0., 0., 0., 0.);

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
     * Tests the access to the internal array storing the data of the matrix. <br>
     * Modifying the returned array should also modify the associated symmetric matrix.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#getDataRef()}
     * </p>
     */
    @Test
    public void testGetDataRef() {
        // Tested matrix
        final double[][] data = { { 7.23743, 0.60184, 0.35444, -0.07380 },
                { 0.60184, 2.09240, 0.99668, -1.99425 }, { 0.35444, 0.99668, 6.35915, 0.81168 },
                { -0.07380, -1.99425, 0.81168, 6.74083 } };
        final ArrayRowSymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, data, 0., 0., 0., 0.);

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
     * {@linkplain ArrayRowSymmetricPositiveMatrix#copy()}
     * </p>
     */
    @Test
    public void testCopy() {
        // Tested matrix
        final double[][] data = { { 6.67373, 0.23850, 0.55969, 1.69953 },
                { 0.23850, 4.62797, 1.31169, 3.08572 }, { 0.55969, 1.31169, 6.78380, -0.14753 },
                { 1.69953, 3.08572, -0.14753, 3.78313 } };
        final ArrayRowSymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, data, 0., 0., 0., 0.);

        // Test the method
        final ArrayRowSymmetricPositiveMatrix result = matrix.copy();
        CheckUtils.checkEquality(matrix, result, 0., 0.);
        Assert.assertNotSame(matrix, result);
        Assert.assertNotSame(matrix.getDataRef(), result.getDataRef());
        Assert.assertEquals(ArrayRowSymmetricPositiveMatrix.class, result.getClass());
    }

    /**
     * Tests the methods that return the transpose of the matrix.<br>
     * The transpose matrix should be the same as the initial matrix, stored in a new instance
     * unless specified otherwise.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#transpose()}<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#transpose(boolean)}<br>
     * </p>
     */
    @Test
    public void testTranspose() {
        // Tested matrix
        final double[][] data = { { 2.69431, -3.43670, -1.61952, -0.61408 },
                { -3.43670, 6.42988, 0.16979, -0.49399 }, { -1.61952, 0.16979, 5.12437, -0.14773 },
                { -0.61408, -0.49399, -0.14773, 5.21921 } };
        final ArrayRowSymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, data, 0., 0., 0., 0.);

        // Test the method
        CheckUtils.checkTranspose(matrix);
    }

    /**
     * Tests the method that raises the matrix to the power of N.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#power(int)}<br>
     * </p>
     */
    @Test
    public void testPower() {
        // Tested matrix
        final double[][] data = { { 7.89169, 0.34264, 0.42622, -0.79242 },
                { 0.34264, 7.91029, 1.20078, 0.66424 }, { 0.42622, 1.20078, 6.98266, -0.40386 },
                { -0.79242, 0.66424, -0.40386, 8.19953 } };
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, data, 0., 0., 0., 0.);

        // Test the method
        CheckUtils.checkPower(matrix, 0, 10, ArrayRowSymmetricPositiveMatrix.class, ABSTOL, RELTOL);
        CheckUtils.checkPowerNegativeExponent(matrix);
    }

    /**
     * Tests the method that returns an element of the matrix.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#getEntry(int, int)}
     * </p>
     */
    @Test
    public void testGetEntry() {
        // Tested matrix
        final double[][] data = { { 4.13498, 0.99793, 0.13189, 0.04905 },
                { 0.99793, 4.75358, -0.11831, -0.09415 }, { 0.13189, -0.11831, 5.52221, 0.13448 },
                { 0.04905, -0.09415, 0.13448, 5.12143 } };
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, data, 0., 0., 0., 0.);

        // Test the method
        CheckUtils.checkGetEntry(matrix);
    }

    /**
     * Tests the method that returns an element of the matrix, using indices outside the valid index
     * range.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#getEntry(int, int)}
     * </p>
     */
    @Test
    public void testGetOutOfRangeEntry() {
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(3);
        CheckUtils.checkGetOutOfRangeEntry(matrix);
    }

    /**
     * Tests the method that sets an entry to a new value.<br>
     * This method should throw an exception since it is forbidden.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#setEntry(int, int, double)}
     * </p>
     */
    @Test
    public void testSetEntry() {
        final int dim = 4;
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(dim);

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
     * {@linkplain ArrayRowSymmetricPositiveMatrix#setEntry(int, int, double)}
     * </p>
     */
    @Test
    public void testSetOutOfRangeEntry() {
        final int dim = 3;
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(dim);

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
     * {@linkplain ArrayRowSymmetricPositiveMatrix#addToEntry(int, int, double)} <>/p
     */
    @Test
    public void testAddToEntry() {
        final int dim = 4;
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(dim);

        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < dim; j++) {
                try {
                    matrix.addToEntry(i, j, 1.);
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
     * {@linkplain ArrayRowSymmetricPositiveMatrix#addToEntry(int, int, double)}
     * </p>
     */
    @Test
    public void testAddToOutOfRangeEntry() {
        final int dim = 4;
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(dim);

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
     * {@linkplain ArrayRowSymmetricPositiveMatrix#multiplyEntry(int, int, double)} <>/p
     */
    @Test
    public void testMultiplyEntry() {
        final int dim = 4;
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(dim);

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
     * {@linkplain ArrayRowSymmetricPositiveMatrix#multiplyEntry(int, int, double)}
     * </p>
     */
    @Test
    public void testMultiplyOutOfRangeEntry() {
        final int dim = 4;
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(4);

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
     * {@linkplain ArrayRowSymmetricPositiveMatrix} when the scalar is positive.
     * Otherwise, it should be an instance of {@linkplain ArrayRowSymmetricMatrix}.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#scalarAdd(double)}
     * </p>
     */
    @Test
    public void testScalarAdd() {
        // Tested matrix
        final double[][] data = { { 7.59314, 0.79776, -1.24562, 1.87589 },
                { 0.79776, 7.72097, -2.58596, -0.14004 }, { -1.24562, -2.58596, 4.74863, 1.37350 },
                { 1.87589, -0.14004, 1.37350, 7.68295 } };
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, data, 0., 0., 0., 0.);

        // Test the method
        CheckUtils
                .checkScalarAdd(matrix, 0., ArrayRowSymmetricPositiveMatrix.class, ABSTOL, RELTOL);
        CheckUtils.checkScalarAdd(matrix, +1.718, ArrayRowSymmetricPositiveMatrix.class, ABSTOL,
                RELTOL);
        CheckUtils.checkScalarAdd(matrix, -7.274, ArrayRowSymmetricMatrix.class, ABSTOL, RELTOL);
    }

    /**
     * Tests the method that multiplies the matrix by a scalar.<br>
     * The resulting matrix should still be an instance of
     * {@linkplain ArrayRowSymmetricPositiveMatrix} when the scalar is positive.
     * Otherwise, it should be an instance of {@linkplain ArrayRowSymmetricMatrix}.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#scalarMultiply(double)}
     * </p>
     */
    @Test
    public void testScalarMultiply() {
        // Tested matrix
        final double[][] data = { { 6.57300, -0.34833, 0.22410, 0.19623 },
                { -0.34833, 5.83235, 0.08363, 0.21435 }, { 0.22410, 0.08363, 5.88146, -0.20996 },
                { 0.19623, 0.21435, -0.20996, 5.80926 } };
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, data, 0., 0., 0., 0.);

        // Test the method
        CheckUtils.checkScalarMultiply(matrix, 0.0, ArrayRowSymmetricPositiveMatrix.class, ABSTOL,
                RELTOL);
        CheckUtils.checkScalarMultiply(matrix, 1.0, ArrayRowSymmetricPositiveMatrix.class, ABSTOL,
                RELTOL);
        CheckUtils.checkScalarMultiply(matrix, +2.838, ArrayRowSymmetricPositiveMatrix.class,
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
     * {@linkplain ArrayRowSymmetricPositiveMatrix#positiveScalarAdd(double)}
     * </p>
     */
    @Test
    public void testPositiveScalarAdd() {
        // Tested matrix
        final double[][] data = { { 4.74410, 0.81431, 1.20959, -0.43738 },
                { 0.81431, 5.20300, 0.93351, -0.03581 }, { 1.20959, 0.93351, 5.93178, 0.05959 },
                { -0.43738, -0.03581, 0.05959, 4.33000 } };
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.MEAN, data, 0., 0., 0., 0.);

        // Test the method
        CheckUtils.checkPositiveScalarAdd(matrix, 0.0, ArrayRowSymmetricPositiveMatrix.class,
                ABSTOL, RELTOL);
        CheckUtils.checkPositiveScalarAdd(matrix, +1.718, ArrayRowSymmetricPositiveMatrix.class,
                ABSTOL, RELTOL);

        // Exception check
        try {
            matrix.positiveScalarAdd(-Double.MIN_VALUE);
            Assert.fail();
        } catch (final NotPositiveException e) {
            final String expectedMessage = "invalid scalar -0 (must be positive)";
            Assert.assertEquals(NotPositiveException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the method that multiplies the matrix by a positive scalar.<br>
     * An exception should be thrown if the provided scalar is negative.
     *
     * <p>
     * Tested method:<br>
     * {@link ArrayRowSymmetricPositiveMatrix#positiveScalarMultiply(double)}
     * </p>
     */
    @Test
    public void testPositiveScalarMultiply() {
        // Tested matrix
        final double[][] data = { { 2.61746, 1.30020, 1.04408, 1.07315 },
                { 1.30020, 5.76764, -1.08155, 0.64567 }, { 1.04408, -1.08155, 3.84991, -0.36580 },
                { 1.07315, 0.64567, -0.36580, 6.41081 } };
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.MEAN, data, 0., 0., 0., 0.);

        // Test the method
        CheckUtils.checkPositiveScalarMultiply(matrix, 1.0, ArrayRowSymmetricPositiveMatrix.class,
                ABSTOL, RELTOL);
        CheckUtils.checkPositiveScalarMultiply(matrix, +2.838,
                ArrayRowSymmetricPositiveMatrix.class, ABSTOL, RELTOL);

        // Exception check
        try {
            matrix.positiveScalarMultiply(-Double.MIN_VALUE);
            Assert.fail();
        } catch (final NotPositiveException e) {
            final String expectedMessage = "invalid scalar -0 (must be positive)";
            Assert.assertEquals(NotPositiveException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the methods that add another matrix.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#add(RealMatrix)}<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#add(DiagonalMatrix)}<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#add(SymmetricMatrix)}<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#add(SymmetricPositiveMatrix)}<br>
     * </p>
     */
    @Test
    public void testAddMatrix() {
        // Tested matrix
        final double[][] initialData = { { 7.52844, -0.32836, -0.30738, -0.40513 },
                { -0.32836, 6.77155, -2.12446, -1.20475 },
                { -0.30738, -2.12446, 2.80606, -2.32362 },
                { -0.40513, -1.20475, -2.32362, 7.22688 } };
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, initialData, 0., 0., 0., 0.);

        // Matrices to be added
        final double[] diagonalElements = { -7.97199, 1.30839, 5.16395, -2.73103 };
        final double[][] data = { { 3.62386, 0.69779, 2.21796, 0.07817 },
                { 0.69779, 2.74677, 1.23855, 1.58578 }, { 2.21796, 1.23855, 3.55837, -0.83389 },
                { 0.07817, 1.58578, -0.83389, 5.69480 } };

        final RealMatrix realMatrix = new Array2DRowRealMatrix(data);
        final DiagonalMatrix diagonalMatrix = new DiagonalMatrix(diagonalElements);
        final SymmetricMatrix symmetricMatrix = new ArrayRowSymmetricMatrix(SymmetryType.LOWER,
                data, 0., 0.);
        final SymmetricPositiveMatrix positiveMatrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, data, 0., 0., 0., 0.);

        // Check the add methods
        CheckUtils.checkAddMatrix(matrix, realMatrix, Array2DRowRealMatrix.class, ABSTOL, RELTOL);
        CheckUtils.checkAddMatrix(matrix, symmetricMatrix, ArrayRowSymmetricMatrix.class, ABSTOL,
                RELTOL);
        CheckUtils.checkAddMatrix(matrix, positiveMatrix, ArrayRowSymmetricPositiveMatrix.class,
                ABSTOL, RELTOL);
        CheckUtils.checkAddMatrix(matrix, diagonalMatrix, ArrayRowSymmetricMatrix.class, ABSTOL,
                RELTOL);
    }

    /**
     * Tests the methods that add another matrix, using a matrix with incompatible dimensions.<br>
     * An exception should be thrown.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#add(RealMatrix)}<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#add(DiagonalMatrix)}<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#add(SymmetricMatrix)}<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#add(SymmetricPositiveMatrix)}<br>
     * </p>
     */
    @Test
    public void testAddIncompatibleMatrix() {
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(3);
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
     * {@linkplain ArrayRowSymmetricPositiveMatrix#subtract(RealMatrix)}<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#subtract(SymmetricMatrix)}<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#subtract(DiagonalMatrix)}<br>
     * </p>
     */
    @Test
    public void testSubtractMatrix() {
        // Tested matrix
        final double[][] initialData = { { 5.55198, 0.31259, 0.35119, 0.10778 },
                { 0.31259, 5.40508, -0.46536, 1.08220 }, { 0.35119, -0.46536, 4.02100, -1.45770 },
                { 0.10778, 1.08220, -1.45770, 4.65557 } };
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, initialData, 0., 0., 0., 0.);

        // Matrices to be subtracted
        final double[] diagonalElements = { -0.67888, -6.92484, -7.03104, -1.87639 };
        final double[][] data = { { 3.23945, 1.16873, 0.18945, -2.11874 },
                { 1.16873, 5.47235, -0.02667, 0.44533 }, { 0.18945, -0.02667, 2.94376, -0.44472 },
                { -2.11874, 0.44533, -0.44472, 5.08824 } };

        final RealMatrix realMatrix = new Array2DRowRealMatrix(data);
        final DiagonalMatrix diagonalMatrix = new DiagonalMatrix(diagonalElements);
        final SymmetricMatrix symmetricMatrix = new ArrayRowSymmetricMatrix(SymmetryType.LOWER,
                data, 0., 0.);
        final SymmetricPositiveMatrix positiveMatrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, data, 0., 0., 0., 0.);

        // Check the subtract methods
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
     * {@linkplain ArrayRowSymmetricPositiveMatrix#subtract(RealMatrix)}<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#subtract(SymmetricMatrix)}<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#subtract(DiagonalMatrix)}<br>
     * </p>
     */
    @Test
    public void testSubtractIncompatibleMatrix() {
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(3);
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
     * {@linkplain ArrayRowSymmetricPositiveMatrix#multiply(RealMatrix)}<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#multiply(RealMatrix, boolean)}<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#multiply(RealMatrix, boolean, double)}<br>
     * </p>
     */
    @Test
    public void testMultiplyBySquareMatrix() {
        // Tested matrix
        final double[][] initialData = { { 3.21204, -1.23866, -1.44495, -1.21302 },
                { -1.23866, 2.38663, 0.19678, 0.58760 }, { -1.44495, 0.19678, 2.43991, 1.17180 },
                { -1.21302, 0.58760, 1.17180, 2.80044 } };
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, initialData, 0., 0., 0., 0.);

        // Matrices to multiply by
        final double[] diagonalElements = { -9.34699, -8.55929, -9.05330, 6.99469 };
        final double[][] data = { { 5.20751, -0.28432, 0.83997, 0.30677 },
                { -0.28432, 6.04237, 0.76898, -0.95425 }, { 0.83997, 0.76898, 5.55630, 0.49371 },
                { 0.30677, -0.95425, 0.49371, 5.73133 } };

        final RealMatrix realMatrix = new Array2DRowRealMatrix(data);
        final DiagonalMatrix diagonalMatrix = new DiagonalMatrix(diagonalElements);
        final SymmetricMatrix symmetricMatrix = new ArrayRowSymmetricMatrix(SymmetryType.LOWER,
                data, 0., 0.);
        final SymmetricPositiveMatrix positiveMatrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, data, 0., 0., 0., 0.);

        // Scaling factor
        final double scalingFactor = 2.877;

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
     * {@linkplain ArrayRowSymmetricPositiveMatrix#multiply(RealMatrix)}<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#multiply(RealMatrix, boolean)}<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#multiply(RealMatrix, boolean, double)}<br>
     * </p>
     */
    @Test
    public void testMultiplyByRectangularMatrix() {
        // Tested matrix
        final double[][] initialData = { { 6.76986, 1.46893, -1.88593, -1.38505 },
                { 1.46893, 4.59630, 0.02237, -1.44790 }, { -1.88593, 0.02237, 5.11859, -0.40473 },
                { -1.38505, -1.44790, -0.40473, 5.86024 } };
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, initialData, 0., 0., 0., 0.);

        // Matrices to multiply by
        final double[][] data = { { -0.36223, 7.22292, -5.52741, 0.43529, -2.18521, 9.48812 },
                { -1.32123, 3.87522, -8.47676, -7.44094, 1.47615, -6.27867 },
                { 4.02582, -9.32774, -3.05565, -5.82804, -1.85175, 5.56834 },
                { -1.18518, 7.77029, 7.71300, -7.68470, 0.94245, -3.41730 } };
        final RealMatrix realMatrix = new Array2DRowRealMatrix(data);

        // Scaling factor
        final double scalingFactor = -1.378;

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
     * {@linkplain ArrayRowSymmetricPositiveMatrix#multiply(RealMatrix)}<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#multiply(RealMatrix, boolean)}<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#multiply(RealMatrix, boolean, double)}<br>
     * </p>
     */
    @Test
    public void testMultiplyByIncompatibleMatrix() {
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(3);
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
     * {@linkplain ArrayRowSymmetricPositiveMatrix#preMultiply(RealMatrix)}
     * </p>
     */
    @Test
    public void testPreMultiplyBySquareMatrix() {
        // Tested matrix
        final double[][] initialData = { { 6.35306, -0.20162, 0.72728, -0.40190 },
                { -0.20162, 7.60672, 1.51295, 0.58209 }, { 0.72728, 1.51295, 5.89564, -2.01558 },
                { -0.40190, 0.58209, -2.01558, 5.45798 } };
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, initialData, 0., 0., 0., 0.);

        // Matrices to premultiply by
        final double[] diagonalElements = { 1.51898, 9.66205, 9.93394, -9.63718 };
        final double[][] data = { { 5.10281, -1.25067, 1.13663, -1.46569 },
                { -1.25067, 6.66876, -0.12146, -0.54248 }, { 1.13663, -0.12146, 6.43847, 0.71897 },
                { -1.46569, -0.54248, 0.71897, 6.56742 } };

        final RealMatrix realMatrix = new Array2DRowRealMatrix(data);
        final DiagonalMatrix diagonalMatrix = new DiagonalMatrix(diagonalElements);
        final SymmetricMatrix symmetricMatrix = new ArrayRowSymmetricMatrix(SymmetryType.LOWER,
                data, 0., 0.);
        final SymmetricPositiveMatrix positiveMatrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, data, 0., 0., 0., 0.);

        // Test the method
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
     * {@linkplain ArrayRowSymmetricPositiveMatrix#preMultiply(RealMatrix)}<br>
     * </p>
     */
    @Test
    public void testPreMultiplyByRectangularMatrix() {
        // Tested matrix
        final double[][] initialData = { { 6.85476, -0.35154, -2.22535, -0.52515 },
                { -0.35154, 8.82978, -0.56881, -0.42965 },
                { -2.22535, -0.56881, 6.85407, -0.76338 },
                { -0.52515, -0.42965, -0.76338, 8.49992 } };
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, initialData, 0., 0., 0., 0.);

        // Matrices to premultiply by
        final double[][] data = { { 2.87681, -6.53638, -4.54055, -2.38528 },
                { 9.37298, 9.28228, 8.07490, -2.39201 }, { 9.40481, 4.45694, 1.02639, 7.73893 },
                { 6.70829, 3.06193, -9.56546, -0.19312 }, { 5.20376, -3.72821, 4.66761, 6.52785 },
                { -9.63222, -8.40910, 5.78407, -2.44314 } };
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
     * {@linkplain ArrayRowSymmetricPositiveMatrix#preMultiply(RealMatrix)}
     * </p>
     */
    @Test
    public void testPreMultiplyByIncompatibleMatrix() {
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(3);
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
     * {@linkplain ArrayRowSymmetricPositiveMatrix#operate(double[])}<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#operate(RealVector)}<br>
     * </p>
     */
    @Test
    public void testOperateVector() {
        // Tested matrix
        final double[][] data = { { 7.37274, 0.97228, -2.65369, -0.89690 },
                { 0.97228, 6.61080, 1.64498, 0.36761 }, { -2.65369, 1.64498, 3.70932, -3.06226 },
                { -0.89690, 0.36761, -3.06226, 6.46522 } };
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, data, 0., 0., 0., 0.);

        // Vector to multiply the tested matrix by
        final double[] vectorData = { 2.02277, 1.02775, 5.84710, -6.08182 };
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
     * {@linkplain ArrayRowSymmetricPositiveMatrix#operate(double[])}<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#operate(RealVector)}<br>
     * </p>
     */
    @Test
    public void testOperateIncompatibleVector() {
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(3);
        CheckUtils.checkOperateIncompatibleVector(matrix);
    }

    /**
     * Tests the methods that premultiply the matrix by a vector.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#preMultiply(double[])}<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#preMultiply(RealVector)}<br>
     * </p>
     */
    @Test
    public void testPreMultiplyByVector() {
        // Tested matrix
        final double[][] data = { { 4.09868, -1.89346, -0.91776, 0.72992 },
                { -1.89346, 2.79712, 1.37161, 0.17155 }, { -0.91776, 1.37161, 5.26033, -0.53758 },
                { 0.72992, 0.17155, -0.53758, 3.59012 } };
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, data, 0., 0., 0., 0.);

        // Vector to premultiply the tested matrix by
        final double[] vectorData = { -7.79837, -2.86215, 8.14109, 3.48719 };
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
     * {@linkplain ArrayRowSymmetricPositiveMatrix#preMultiply(double[])}<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#preMultiply(RealVector)}<br>
     * </p>
     */
    @Test
    public void testPreMultiplyByIncompatibleVector() {
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(3);
        CheckUtils.checkPreMultiplyIncompatibleVector(matrix);
    }

    /**
     * Tests the quadratic multiplication.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#quadraticMultiplication(RealMatrix)}<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#quadraticMultiplication(RealMatrix, boolean)}<br>
     * </p>
     */
    @Test
    public void testQuadraticMultiplicationSquareMatrix() {
        // Tested matrix
        final double[][] initialData = { { 7.30395, -0.71660, -0.67193, -0.85666 },
                { -0.71660, 7.82805, -0.85216, -0.36448 },
                { -0.67193, -0.85216, 4.75822, 0.65128 }, { -0.85666, -0.36448, 0.65128, 4.15386 } };
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, initialData, 0., 0., 0., 0.);

        // Square matrix used for the quadratic multiplication
        final double[][] data = { { 2.12538, 1.53352, 5.39912, 7.20239 },
                { 4.75455, 1.47551, -1.33185, 2.71314 }, { 2.58788, -8.42338, -6.16790, 3.35389 },
                { -7.76620, -2.55742, -5.73198, 5.68952 } };
        final RealMatrix realMatrix = new Array2DRowRealMatrix(data);

        // Test the methods
        CheckUtils.checkQuadraticMultiplication(matrix, realMatrix,
                ArrayRowSymmetricPositiveMatrix.class, ABSTOL, RELTOL);
    }

    /**
     * Tests the quadratic multiplication using a rectangular matrix.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#quadraticMultiplication(RealMatrix)}<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#quadraticMultiplication(RealMatrix, boolean)}<br>
     * </p>
     */
    @Test
    public void testQuadraticMultiplicationRectangularMatrix() {
        // Tested matrix
        final double[][] initialData = { { 5.49578, -0.75087, -0.48749, -1.96696 },
                { -0.75087, 3.25037, 0.32476, 0.87626 }, { -0.48749, 0.32476, 3.20774, 0.56471 },
                { -1.96696, 0.87626, 0.56471, 4.70830 } };
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, initialData, 0., 0., 0., 0.);

        // Rectangular matrix used for the quadratic multiplication
        final double[][] data = { { 3.85791, 2.38572, 8.10193, 9.92091 },
                { 7.18048, 7.20370, 7.33192, 1.42995 }, { -6.28231, 2.48193, -7.77830, 4.01913 },
                { 8.83245, -5.19769, 6.16127, -2.10207 }, { 5.16951, 3.61254, 7.67393, 5.65505 },
                { -0.93272, 8.26473, 5.46283, -0.82851 } };
        final RealMatrix realMatrix = new Array2DRowRealMatrix(data);

        // Test the methods
        CheckUtils.checkQuadraticMultiplication(matrix, realMatrix,
                ArrayRowSymmetricPositiveMatrix.class, ABSTOL, RELTOL);
    }

    /**
     * Tests the quadratic multiplication using a matrix which have incompatible dimensions.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#quadraticMultiplication(RealMatrix)}<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#quadraticMultiplication(RealMatrix, boolean)}<br>
     * </p>
     */
    @Test
    public void testQuadraticMultiplicationIncompatibleMatrix() {
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(4);
        CheckUtils.checkQuadraticMultiplicationIncompatibleMatrix(matrix);
    }

    /**
     * Tests the methods that return a row of the matrix.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#getRow(int)}<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#getRowVector(int)}<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#getRowMatrix(int)}<br>
     * </p>
     */
    @Test
    public void testGetRow() {
        // Tested matrix
        final double[][] data = { { 5.89666, 0.04962, 1.53310, 1.90541 },
                { 0.04962, 6.15435, -0.34618, 2.46180 }, { 1.53310, -0.34618, 1.96059, -0.33808 },
                { 1.90541, 2.46180, -0.33808, 5.75406 } };
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, data, 0., 0., 0., 0.);

        // Test the methods
        CheckUtils.checkGetRow(matrix);
    }

    /**
     * Tests the methods that return a row of the matrix, using indices outside the valid index
     * range.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#getRow(int)}<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#getRowVector(int)}<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#getRowMatrix(int)}<br>
     * </p>
     */
    @Test
    public void testGetOutOfRangeRow() {
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(3);
        CheckUtils.checkGetOutOfRangeRow(matrix);
    }

    /**
     * Tests the methods that set a row of the matrix.<br>
     * This method should throw an exception since it is forbidden.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#setRow(int, double[])}<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#setRowVector(int, RealVector)}<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#setRowMatrix(int, RealMatrix)}<br>
     * </p>
     */
    @Test
    public void testSetRow() {
        final int dim = 3;
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(dim);
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
     * {@linkplain ArrayRowSymmetricPositiveMatrix#getColumn(int)}<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#getColumnVector(int)}<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#getColumnMatrix(int)}<br>
     * </p>
     */
    @Test
    public void testGetColumn() {
        // Tested matrix
        final double[][] data = { { 2.03157, -1.22838, -1.28854, -2.25379 },
                { -1.22838, 5.67012, 0.37942, 1.37839 }, { -1.28854, 0.37942, 8.52558, -2.15391 },
                { -2.25379, 1.37839, -2.15391, 5.24927 } };
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, data, 0., 0., 0., 0.);

        // Test the methods
        CheckUtils.checkGetColumn(matrix);
    }

    /**
     * Tests the methods that return a column of the matrix, using indices outside the valid index
     * range.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#getColumn(int)}<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#getColumnVector(int)}<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#getColumnMatrix(int)}<br>
     * </p>
     */
    @Test
    public void testGetOutOfRangeColumn() {
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(3);
        CheckUtils.checkGetOutOfRangeColumn(matrix);
    }

    /**
     * Tests the methods that set a column of the matrix.<br>
     * This method should throw an exception since it is forbidden.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#setColumn(int, double[])}<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#setColumnVector(int, RealVector)}<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#setColumnMatrix(int, RealMatrix)}<br>
     * </p>
     */
    @Test
    public void testSetColumn() {
        final int dim = 3;
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(dim);
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
     * {@linkplain ArrayRowSymmetricPositiveMatrix#getSubMatrix(int, int, int, int)}<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#getSubMatrix(int[], int[])}<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#getSubMatrix(int, int)}<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#getSubMatrix(int[])}<br>
     * </p>
     */
    @Test
    public void testGetSubMatrix() {
        // Tested matrix
        final double[][] data = { { 6.96048, 0.87195, -0.01541, -1.58758 },
                { 0.87195, 5.77892, -0.27296, 0.03797 }, { -0.01541, -0.27296, 6.47340, -0.37614 },
                { -1.58758, 0.03797, -0.37614, 6.07240 } };
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.UPPER, data, 0., 0., 0., 0.);

        // Test the methods
        CheckUtils.checkGetSubMatrixByRange(matrix);
        CheckUtils.checkGetSubMatrixByIndex(matrix);
        CheckUtils.checkGetSymmetricSubMatrixByRange(matrix, ArrayRowSymmetricPositiveMatrix.class);
        CheckUtils.checkGetSymmetricSubMatrixByIndex(matrix, ArrayRowSymmetricPositiveMatrix.class);
    }

    /**
     * Tests the methods returning a submatrix when supplying an invalid index range or array.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#getSubMatrix(int, int, int, int)}<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#getSubMatrix(int[], int[])}<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#getSubMatrix(int, int)}<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#getSubMatrix(int[])}<br>
     * </p>
     */
    @Test
    public void testGetSubMatrixInvalidIndices() {
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(3);
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
     * {@linkplain ArrayRowSymmetricPositiveMatrix#setSubMatrix(double[][], int, int)}
     * </p>
     */
    @Test
    public void testSetSubMatrix() {
        final int dim = 4;
        final ArrayRowSymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(dim);
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
     * {@linkplain ArrayRowSymmetricPositiveMatrix#copySubMatrix(int, int, int, int, double[][])}<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#copySubMatrix(int, int, int, int, double[][], int, int)}
     * <br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#copySubMatrix(int[], int[], double[][])}<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#copySubMatrix(int[], int[], double[][], int, int)}
     * <br>
     * </p>
     */
    @Test
    public void testCopySubMatrix() {
        // Tested matrix
        final double[][] data = { { 8.17673, -2.35017, 0.20655, 1.39066 },
                { -2.35017, 6.11588, -0.05588, 1.44357 }, { 0.20655, -0.05588, 7.91582, -1.23057 },
                { 1.39066, 1.44357, -1.23057, 8.33504 } };
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, data, 0., 0., 0., 0.);

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
     * {@linkplain ArrayRowSymmetricPositiveMatrix#copySubMatrix(int, int, int, int, double[][])}<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#copySubMatrix(int, int, int, int, double[][], int, int)}
     * <br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#copySubMatrix(int[], int[], double[][])}<br>
     * {@linkplain ArrayRowSymmetricMatrix#copySubMatrix(int[], int[], double[][], int, int)}<br>
     * </p>
     */
    @Test
    public void testCopySubMatrixInvalidIndices() {
        final RealMatrix matrix = new ArrayRowSymmetricPositiveMatrix(4);
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
     * {@linkplain ArrayRowSymmetricPositiveMatrix#copySubMatrix(int, int, int, int, double[][])}<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#copySubMatrix(int, int, int, int, double[][], int, int)}
     * <br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#copySubMatrix(int[], int[], double[][])}<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#copySubMatrix(int[], int[], double[][], int, int)}
     * <br>
     * </p>
     */
    @Test
    public void testCopySubMatrixNullDestinationArray() {
        final RealMatrix matrix = new ArrayRowSymmetricPositiveMatrix(4);
        CheckUtils.checkCopySubMatrixNullDestinationArray(matrix);
    }

    /**
     * Tests the methods that copies part of the matrix into a given array, using a destination
     * array with no rows or columns.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#copySubMatrix(int, int, int, int, double[][])}<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#copySubMatrix(int, int, int, int, double[][], int, int)}
     * <br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#copySubMatrix(int[], int[], double[][])}<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#copySubMatrix(int[], int[], double[][], int, int)}
     * <br>
     * </p>
     */
    @Test
    public void testCopySubMatrixEmptyDestinationArray() {
        final RealMatrix matrix = new ArrayRowSymmetricPositiveMatrix(4);
        CheckUtils.checkCopySubMatrixEmptyDestinationArray(matrix);
    }

    /**
     * Tests the methods that copies part of the matrix into a given array, using a destination
     * array too small to store the extracted
     * submatrix.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#copySubMatrix(int, int, int, int, double[][])}<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#copySubMatrix(int, int, int, int, double[][], int, int)}
     * <br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#copySubMatrix(int[], int[], double[][])}<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#copySubMatrix(int[], int[], double[][], int, int)}
     * <br>
     * </p>
     */
    @Test
    public void testCopySubMatrixIncompatibleDestinationArray() {
        final RealMatrix matrix = new ArrayRowSymmetricPositiveMatrix(4);
        CheckUtils.checkCopySubMatrixIncompatibleDestinationArray(matrix);
    }

    /**
     * Tests the methods that concatenate two matrices horizontally.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#concatenateHorizontally(RealMatrix)}<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#concatenateHorizontally(RealMatrix, boolean)}<br>
     * </p>
     */
    @Test
    public void testHorizontalConcatenation() {
        // Tested matrix
        final double[][] initialData = { { 6.45723, -0.86320, -2.10078, 2.32026 },
                { -0.86320, 4.90740, -0.12615, -0.86779 },
                { -2.10078, -0.12615, 5.15720, 0.37744 }, { 2.32026, -0.86779, 0.37744, 4.37590 } };
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, initialData, 0., 0., 0., 0.);

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
     * {@linkplain ArrayRowSymmetricPositiveMatrix#concatenateVertically(RealMatrix)}<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#concatenateVertically(RealMatrix, boolean)}<br>
     * </p>
     */
    @Test
    public void testVerticalConcatenation() {
        // Tested matrix
        final double[][] initialData = { { 2.88241, 0.15018, 0.93410, 1.20701 },
                { 0.15018, 3.96063, -2.79005, 0.05122 }, { 0.93410, -2.79005, 6.28477, -0.61273 },
                { 1.20701, 0.05122, -0.61273, 4.59796 } };
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, initialData, 0., 0., 0., 0.);

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
     * {@linkplain ArrayRowSymmetricPositiveMatrix#concatenateDiagonally(RealMatrix, boolean)}<br>
     * </p>
     */
    @Test
    public void testDiagonalConcatenation() {
        // Tested matrix
        final double[][] initialData = { { 6.55620, 0.01703, 0.14423, 0.03856 },
                { 0.01703, 6.73920, -0.50523, 0.64516 }, { 0.14423, -0.50523, 7.08090, 1.13959 },
                { 0.03856, 0.64516, 1.13959, 8.33869 } };
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, initialData, 0., 0., 0., 0.);

        // Matrix to be concatenated with the tested matrix (6x4)
        final double[][] data = { { 2.51173, 7.76164, -9.53554, -3.63145 },
                { -1.75996, 1.45772, 9.86776, -0.72429 },
                { -3.01483, -6.62362, 4.03318, -9.97493 },
                { -0.71896, -9.60868, 4.49939, -4.19880 }, { 4.24177, -3.54944, 2.41990, 9.25648 },
                { 6.98555, -4.44124, 6.48209, -8.40037 } };
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
     * {@linkplain ArrayRowSymmetricPositiveMatrix#concatenateHorizontally(RealMatrix)}<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#concatenateHorizontally(RealMatrix, boolean)}<br>
     * </p>
     */
    @Test
    public void testHorizontalConcatenationIncompatibleMatrix() {
        final RealMatrix matrix = new ArrayRowSymmetricPositiveMatrix(4);
        CheckUtils.checkConcatenateHorizontallyIncompatibleMatrix(matrix);
    }

    /**
     * Tests the methods that concatenate two matrices vertically, using matrices with incompatible
     * dimensions.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#concatenateVertically(RealMatrix)}<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#concatenateVertically(RealMatrix, boolean)}<br>
     * </p>
     */
    @Test
    public void testVerticalConcatenationIncompatibleMatrix() {
        final RealMatrix matrix = new ArrayRowSymmetricPositiveMatrix(4);
        CheckUtils.checkConcatenateVerticallyIncompatibleMatrix(matrix);
    }

    /**
     * Tests the matrix inversion.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#getInverse()}
     * </p>
     */
    @Test
    public void testGetMatrixInverse() {
        // Tested matrix
        final double[][] initialData = { { 7.70336, 0.08391, 0.71477, 0.76383 },
                { 0.08391, 9.08049, -0.73870, -1.09160 }, { 0.71477, -0.73870, 6.20502, -1.34915 },
                { 0.76383, -1.09160, -1.34915, 7.11831 } };
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, initialData, 0., 0., 0., 0.);

        // Test the method
        CheckUtils
                .checkInverseMatrix(matrix, ArrayRowSymmetricPositiveMatrix.class, ABSTOL, RELTOL);
    }

    /**
     * Tests the inversion of a singular matrix.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#getInverse()}
     * </p>
     */
    @Test
    public void testGetSingularMatrixInverse() {
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(4);
        CheckUtils.checkInverseSingularMatrix(matrix);
    }

    /**
     * Tests the method that sets the decomposition algorithm to use by default when computing the
     * inverse matrix.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#getDefaultDecomposition()}<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#setDefaultDecomposition(Function)}<br>
     * </p>
     */
    @Test
    public void testSetDefaultDecomposition() {
        final RealMatrix matrix = new ArrayRowSymmetricPositiveMatrix(4);
        CheckUtils.checkDefaultDecomposition(matrix);
    }

    /**
     * Tests the method:<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#getEffectiveTolerance(Double, Double, double)}
     */
    @Test
    public void testGetEffectiveTolerance() {
        Double absoluteTolerance;
        Double relativeTolerance;
        double tolerance;
        double expected;

        final double maxValue = 30;

        // Both tolerances are null
        absoluteTolerance = null;
        relativeTolerance = null;
        tolerance = ArrayRowSymmetricPositiveMatrix.getEffectiveTolerance(absoluteTolerance,
                relativeTolerance, maxValue);
        expected = 0.;
        CheckUtils.checkEquality(expected, tolerance, ABSTOL, RELTOL);

        // The absolute tolerance is not null, but the relative tolerance is
        absoluteTolerance = 1E-14;
        relativeTolerance = null;
        tolerance = ArrayRowSymmetricPositiveMatrix.getEffectiveTolerance(absoluteTolerance,
                relativeTolerance, maxValue);
        expected = 1E-14;
        CheckUtils.checkEquality(expected, tolerance, ABSTOL, RELTOL);

        // The relative tolerance is not null, but the absolute tolerance is
        absoluteTolerance = null;
        relativeTolerance = 1E-14;
        tolerance = ArrayRowSymmetricPositiveMatrix.getEffectiveTolerance(absoluteTolerance,
                relativeTolerance, maxValue);
        expected = 3E-13;
        CheckUtils.checkEquality(expected, tolerance, ABSTOL, RELTOL);

        // Both tolerances are not null, the relative tolerance taking precedence
        absoluteTolerance = 1E-4;
        relativeTolerance = 1E-14;
        tolerance = ArrayRowSymmetricPositiveMatrix.getEffectiveTolerance(absoluteTolerance,
                relativeTolerance, maxValue);
        expected = 1E-4;
        CheckUtils.checkEquality(expected, tolerance, ABSTOL, RELTOL);

        // Both tolerances are not null, the relative tolerance taking precedence
        absoluteTolerance = 1E-14;
        relativeTolerance = 1E-14;
        tolerance = ArrayRowSymmetricPositiveMatrix.getEffectiveTolerance(absoluteTolerance,
                relativeTolerance, maxValue);
        expected = 3E-13;
        CheckUtils.checkEquality(expected, tolerance, ABSTOL, RELTOL);
    }

    /**
     * Tests the method:<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#isPositiveSemiDefinite(SymmetricMatrix, double)}
     *
     * <p>
     * In addition to some predefined matrices, this tests checks multiple randomly generated
     * matrices with elements spanning multiple order of magnitudes. The tested matrices are either
     * filled with completely random elements, or build by randomly choosing its eigenvalues and
     * applying a random rotation matrix afterward. The test fails if a matrix is considered to be
     * positive semi-definite but has negative eigenvalues, or if it is not considered to be
     * positive semi-definite but all its eigenvalues are positive or numerically equal to zero.
     * </p>
     */
    @Test
    public void testIsPositiveSemiDefinite() {
        double[][] data;
        SymmetricMatrix matrix;
        ArrayRowSymmetricPositiveMatrix positiveMatrix;
        boolean isPositive;

        // Positive semi-definite matrix
        data = new double[][] { { 0., 0., 0. }, { 0., 0., 0. }, { 0., 0., 0. } };
        matrix = new ArrayRowSymmetricMatrix(SymmetryType.LOWER, data, 0., 0.);
        isPositive = ArrayRowSymmetricPositiveMatrix.isPositiveSemiDefinite(matrix, 0.);
        Assert.assertTrue(isPositive);
        positiveMatrix = new ArrayRowSymmetricPositiveMatrix(SymmetryType.LOWER, data, 0., 0.,
                null, null);
        Assert.assertTrue(positiveMatrix.isPositiveSemiDefinite(0.));

        // Positive definite matrix
        data = new double[][] { { 1E-5, 0., 0. }, { 0., 1E-8, 0. }, { 0., 0., 1E-2 } };
        matrix = new ArrayRowSymmetricMatrix(SymmetryType.LOWER, data, 0., 0.);
        isPositive = ArrayRowSymmetricPositiveMatrix.isPositiveSemiDefinite(matrix, 0.);
        Assert.assertTrue(isPositive);
        positiveMatrix = new ArrayRowSymmetricPositiveMatrix(SymmetryType.LOWER, data, 0., 0.,
                null, null);
        Assert.assertTrue(positiveMatrix.isPositiveSemiDefinite(0.));

        // Slightly negative matrix
        data = new double[][] { { 1E-5, 0., 0. }, { 0., -1E-12, 0. }, { 0., 0., 1E-2 } };
        matrix = new ArrayRowSymmetricMatrix(SymmetryType.LOWER, data, 0., 0.);
        isPositive = ArrayRowSymmetricPositiveMatrix.isPositiveSemiDefinite(matrix, 0.);
        Assert.assertFalse(isPositive);
        positiveMatrix = new ArrayRowSymmetricPositiveMatrix(SymmetryType.LOWER, data, 0., 0.,
                null, null);
        Assert.assertFalse(positiveMatrix.isPositiveSemiDefinite(0.));

        // Positive semi-definite matrix with negative value on the non diagonal elements
        data = new double[][] { { 1E-5, 0., 0. }, { 0., 0., -1E-13 }, { 0., -1E-13, 1E-2 } };
        matrix = new ArrayRowSymmetricMatrix(SymmetryType.LOWER, data, 0., 0.);
        isPositive = ArrayRowSymmetricPositiveMatrix.isPositiveSemiDefinite(matrix, 0.);
        Assert.assertFalse(isPositive);
        isPositive = ArrayRowSymmetricPositiveMatrix.isPositiveSemiDefinite(matrix, 1E-14);
        Assert.assertTrue(isPositive);
        positiveMatrix = new ArrayRowSymmetricPositiveMatrix(SymmetryType.LOWER, data, 0., 0.,
                null, null);
        Assert.assertFalse(positiveMatrix.isPositiveSemiDefinite(0.));
        Assert.assertTrue(positiveMatrix.isPositiveSemiDefinite(1E-14));

        // Test random matrices
        final int nbMatrices = 100;
        final double errorMargin = 5E-1;
        final double absoluteTolerance = 0.;
        final double relativeTolerance = 1E-14;

        for (int i = 0; i < nbMatrices; i++) {
            data = CheckUtils.getRandomSymmetricArray(RNG, 6, 10, -1., 1., -14, +14);
            matrix = new ArrayRowSymmetricMatrix(SymmetryType.LOWER, data, 0., 0.);
            final double tolerance = ArrayRowSymmetricPositiveMatrix.getEffectiveTolerance(
                    absoluteTolerance, relativeTolerance, matrix.getNorm());
            checkIsPositiveSemiDefinite(matrix, tolerance, errorMargin);
        }

        for (int i = 0; i < nbMatrices; i++) {
            final double[] diagonalElements = CheckUtils.getRandom1dArray(RNG, 6, -10., +10.);
            final DiagonalMatrix diagonalMatrix = new DiagonalMatrix(diagonalElements);
            final RealMatrix orthogonalMatrix = CheckUtils.getRandomOrthogonalMatrix(RNG, 6, -100.,
                    +100.);
            matrix = diagonalMatrix.quadraticMultiplication(orthogonalMatrix);
            final double tolerance = ArrayRowSymmetricPositiveMatrix.getEffectiveTolerance(
                    absoluteTolerance, relativeTolerance, matrix.getNorm());
            checkIsPositiveSemiDefinite(matrix, tolerance, errorMargin);
        }
    }

    /**
     * Tests the methods that returns a string representation of the matrix.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#toString()}<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#toString(RealMatrixFormat)}<br>
     * </p>
     */
    @Test
    public void testToString() {
        // Tested matrix
        final double[][] data = { { 2.61465, 0.31561, 0.32947, -1.02171 },
                { 0.31561, 2.51697, 0.34199, 1.31281 }, { 0.32947, 0.34199, 6.68626, 1.87569 },
                { -1.02171, 1.31281, 1.87569, 3.76632 } };
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, data, 0., 0., 0., 0.);

        // Test the method
        CheckUtils.checkToString(matrix);
    }

    /**
     * Tests the methods:<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#equals(Object)}<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#hashCode()}
     */
    @Test
    public void testEqualsAndHashCode() {
        RealMatrix other;

        final double[][] data = { { 4.94396, 3.13999, 1.11285, 2.13864 },
                { 3.13999, 5.27166, 2.00453, -1.08933 }, { 1.11285, 2.00453, 5.07233, -1.87322 },
                { 2.13864, -1.08933, -1.87322, 8.36032 } };
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, data, 0., 0., 0., 0.);

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
                // Modified data
                final double[][] modifiedData = CheckUtils.copyArray(data);
                modifiedData[i][j] = 0.;
                modifiedData[j][i] = 0.;

                // Matrix of the same type
                other = new ArrayRowSymmetricPositiveMatrix(SymmetryType.LOWER, modifiedData, 0.,
                        0., null, null);
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
     * {@linkplain ArrayRowSymmetricPositiveMatrix#equals(RealMatrix, double, double)}
     * </p>
     */
    @Test
    public void testEqualsWithTolerances() {
        // Tested matrix
        final double[][] data = { { 2.52870, 1.01609, 1.05648, 1.28691 },
                { 1.01609, 3.82822, 0.18878, 1.26649 }, { 1.05648, 0.18878, 1.88339, -0.13878 },
                { 1.28691, 1.26649, -0.13878, 4.37716 } };
        final RealMatrix matrix = new ArrayRowSymmetricPositiveMatrix(SymmetryType.LOWER, data, 0.,
                0., 0., 0.);

        // Test the method
        CheckUtils.checkEqualsWithTolerances(matrix);
    }

    /**
     * Tests the serialization/deserialization.
     */
    @Test
    public void testSerialization() {
        // Tested matrix
        final double[][] data = { { 4.69717, -0.90011, -0.04870, -0.69628 },
                { -0.90011, 5.50880, 0.49583, 1.06680 }, { -0.04870, 0.49583, 4.21377, 0.23546 },
                { -0.69628, 1.06680, 0.23546, 4.75414 } };
        final ArrayRowSymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, data, 0., 0., 0., 0.);

        // Test the serialization
        CheckUtils.checkSerialization(matrix, ArrayRowSymmetricPositiveMatrix.class);
    }

    /**
     * Tests the method allowing to visit the elements of the matrix without modifying them.
     *
     * <p>
     * Tested methods<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#walkInRowOrder(RealMatrixPreservingVisitor)}<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#walkInRowOrder(RealMatrixPreservingVisitor, int, int, int, int)}
     * <br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#walkInColumnOrder(RealMatrixPreservingVisitor)}<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#walkInColumnOrder(RealMatrixPreservingVisitor, int, int, int, int)}
     * <br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#walkInOptimizedOrder(RealMatrixPreservingVisitor)}
     * <br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#walkInOptimizedOrder(RealMatrixPreservingVisitor, int, int, int, int)}
     * <br>
     * </p>
     */
    @Test
    public void testPreservingVisitor() {
        // Tested matrix
        final double[][] data = { { 1.87340, -0.89571, 0.57873, 1.92832 },
                { -0.89571, 6.05591, 0.93636, -2.96204 }, { 0.57873, 0.93636, 3.28818, 2.80235 },
                { 1.92832, -2.96204, 2.80235, 5.84400 } };
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, data);

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
     * {@linkplain ArrayRowSymmetricPositiveMatrix#walkInRowOrder(RealMatrixChangingVisitor)}<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#walkInRowOrder(RealMatrixChangingVisitor, int, int, int, int)}
     * <br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#walkInColumnOrder(RealMatrixChangingVisitor)}<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#walkInColumnOrder(RealMatrixChangingVisitor, int, int, int, int)}
     * <br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#walkInOptimizedOrder(RealMatrixChangingVisitor)}<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#walkInOptimizedOrder(RealMatrixChangingVisitor, int, int, int, int)}
     * <br>
     * </p>
     */
    @Test
    public void testChangingVisitor() {
        // Tested matrix
        final double[][] data = { { 4.10447, -2.11178, 2.43989, -0.14440 },
                { -2.11178, 7.49476, 0.40555, 1.31874 }, { 2.43989, 0.40555, 5.05578, -0.68504 },
                { -0.14440, 1.31874, -0.68504, 7.22286 } };
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, data);

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

    /**
     * Tests the getter and setter for the default absolute symmetry threshold.<br>
     * This threshold should be shared with {@linkplain ArrayRowSymmetricMatrix}.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#getDefaultAbsoluteSymmetryThreshold()}<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#setDefaultAbsoluteSymmetryThreshold(Double)}<br>
     * </p>
     */
    @Test
    public void testDefaultAbsoluteSymmetryThreshold() {
        // Default value
        Assert.assertNull(ArrayRowSymmetricPositiveMatrix.getDefaultAbsoluteSymmetryThreshold());

        // Non-null value
        ArrayRowSymmetricPositiveMatrix.setDefaultAbsoluteSymmetryThreshold(1E-10);
        Assert.assertEquals(1E-10,
                ArrayRowSymmetricPositiveMatrix.getDefaultAbsoluteSymmetryThreshold(), 0);
        Assert.assertEquals(1E-10, ArrayRowSymmetricMatrix.getDefaultAbsoluteSymmetryThreshold(), 0);

        // Null value
        ArrayRowSymmetricPositiveMatrix.setDefaultAbsoluteSymmetryThreshold(null);
        Assert.assertNull(ArrayRowSymmetricPositiveMatrix.getDefaultAbsoluteSymmetryThreshold());
        Assert.assertNull(ArrayRowSymmetricMatrix.getDefaultAbsoluteSymmetryThreshold());

        // Ensure an exception is thrown when attempting to set
        // the default threshold to a negative or NaN value
        try {
            ArrayRowSymmetricPositiveMatrix.setDefaultAbsoluteSymmetryThreshold(-Double.MIN_VALUE);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            final String expectedMessage = "Absolute threshold is not positive or null.";
            Assert.assertEquals(IllegalArgumentException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            ArrayRowSymmetricPositiveMatrix.setDefaultAbsoluteSymmetryThreshold(Double.NaN);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            final String expectedMessage = "Input threshold is NaN.";
            Assert.assertEquals(IllegalArgumentException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the getter and setter for the default relative symmetry threshold.<br>
     * This threshold should be shared with {@linkplain ArrayRowSymmetricMatrix}.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#getDefaultRelativeSymmetryThreshold()}<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#setDefaultRelativeSymmetryThreshold(Double)}<br>
     * </p>
     */
    @Test
    public void testDefaultRelativeSymmetryThreshold() {
        // Default value
        Assert.assertNull(ArrayRowSymmetricPositiveMatrix.getDefaultRelativeSymmetryThreshold());

        // Non-null value
        ArrayRowSymmetricPositiveMatrix.setDefaultRelativeSymmetryThreshold(1E-10);
        Assert.assertEquals(1E-10,
                ArrayRowSymmetricPositiveMatrix.getDefaultRelativeSymmetryThreshold(), 0);
        Assert.assertEquals(1E-10, ArrayRowSymmetricMatrix.getDefaultRelativeSymmetryThreshold(), 0);

        // Null value
        ArrayRowSymmetricPositiveMatrix.setDefaultRelativeSymmetryThreshold(null);
        Assert.assertNull(ArrayRowSymmetricPositiveMatrix.getDefaultRelativeSymmetryThreshold());
        Assert.assertNull(ArrayRowSymmetricMatrix.getDefaultRelativeSymmetryThreshold());

        // Ensure an exception is thrown when attempting to set
        // the default threshold to a negative or NaN value
        try {
            ArrayRowSymmetricPositiveMatrix.setDefaultRelativeSymmetryThreshold(-Double.MIN_VALUE);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            final String expectedMessage = "Relative threshold is not positive or null.";
            Assert.assertEquals(IllegalArgumentException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            ArrayRowSymmetricPositiveMatrix.setDefaultRelativeSymmetryThreshold(Double.NaN);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            final String expectedMessage = "Input threshold is NaN.";
            Assert.assertEquals(IllegalArgumentException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the getter and setter for the default absolute positivity threshold. An exception
     * should be thrown when trying to set the
     * default threshold to a negative value.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#getDefaultAbsolutePositivityThreshold()}<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#setDefaultAbsolutePositivityThreshold(Double)}<br>
     * </p>
     */
    @Test
    public void testDefaultAbsolutePostivityThreshold() {
        Assert.assertEquals(0.,
                ArrayRowSymmetricPositiveMatrix.getDefaultAbsolutePositivityThreshold(), 0.);

        ArrayRowSymmetricPositiveMatrix.setDefaultAbsolutePositivityThreshold(1E-10);
        Assert.assertEquals(1E-10,
                ArrayRowSymmetricPositiveMatrix.getDefaultAbsolutePositivityThreshold(), 0);

        ArrayRowSymmetricPositiveMatrix.setDefaultAbsolutePositivityThreshold(null);
        Assert.assertNull(ArrayRowSymmetricPositiveMatrix.getDefaultAbsolutePositivityThreshold());

        try {
            ArrayRowSymmetricPositiveMatrix.setDefaultAbsolutePositivityThreshold(-1.);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            final String expectedMessage = "Absolute threshold is not positive or null.";
            Assert.assertEquals(IllegalArgumentException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            ArrayRowSymmetricPositiveMatrix.setDefaultAbsolutePositivityThreshold(Double.NaN);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            final String expectedMessage = "Input threshold is NaN.";
            Assert.assertEquals(IllegalArgumentException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the getter and setter for the default relative positivity threshold. An exception
     * should be thrown when trying to set the
     * default threshold to a negative value.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#getDefaultRelativePositivityThreshold()}<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#setDefaultRelativePositivityThreshold(Double)}<br>
     * </p>
     */
    @Test
    public void testDefaultRelativePostivityThreshold() {
        Assert.assertEquals(1E-14,
                ArrayRowSymmetricPositiveMatrix.getDefaultRelativePositivityThreshold(), 0.);

        ArrayRowSymmetricPositiveMatrix.setDefaultRelativePositivityThreshold(1E-10);
        Assert.assertEquals(1E-10,
                ArrayRowSymmetricPositiveMatrix.getDefaultRelativePositivityThreshold(), 0);

        ArrayRowSymmetricPositiveMatrix.setDefaultRelativePositivityThreshold(null);
        Assert.assertNull(ArrayRowSymmetricPositiveMatrix.getDefaultRelativePositivityThreshold());

        try {
            ArrayRowSymmetricPositiveMatrix.setDefaultRelativePositivityThreshold(-1.);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            final String expectedMessage = "Relative threshold is not positive or null.";
            Assert.assertEquals(IllegalArgumentException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            ArrayRowSymmetricPositiveMatrix.setDefaultRelativePositivityThreshold(Double.NaN);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            final String expectedMessage = "Input threshold is NaN.";
            Assert.assertEquals(IllegalArgumentException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the method:<br>
     * {@linkplain ArrayRowSymmetricPositiveMatrix#isPositiveSemiDefinite(SymmetricMatrix, double)}<br>
     * on a given matrix.
     *
     * <p>
     * The tests succeeds if the method considers the matrix to be positive semi-definite if and
     * only if its eigenvalues are all positive or numerically equal to zero. Since the two way to
     * check the positivity of the matrix are not strictly equivalent, failures are tolerated if the
     * largest negative eigenvalue is very close to the specified tolerance. The error margin is
     * relative to the specified tolerance.
     * </p>
     *
     * @param matrix
     *        the matrix to check
     * @param tolerance
     *        the absolute tolerance
     * @param errorMargin
     *        the error margin within which failures are to be tolerated
     */
    private static void checkIsPositiveSemiDefinite(final SymmetricMatrix matrix,
            final double tolerance, final double errorMargin) {
        // Eigen decomposition
        final EigenDecomposition decomposition = new EigenDecomposition(matrix);
        final double[] eigenvalues = decomposition.getRealEigenvalues();

        // Is the matrix truly positive semi-definite?
        // (all its eigenvalues should be positive or numerically equal to zero)
        double minNegativeEigenvalue = 0.;
        boolean areEigenvaluesPositive = true;
        for (final double eigenvalue : eigenvalues) {
            areEigenvaluesPositive &= eigenvalue >= -tolerance;
            minNegativeEigenvalue = MathLib.min(minNegativeEigenvalue, eigenvalue);
        }

        // Is the matrix positive semi-definite according to the
        // method used by the ArrayRowSymmetricPositiveMatrix class?
        final boolean isPositive = ArrayRowSymmetricPositiveMatrix.isPositiveSemiDefinite(matrix,
                tolerance);

        // Absolute and relative difference between the
        // largest negative eigenvalue and the specified tolerance
        final double absDiff = MathLib.abs(MathLib.abs(minNegativeEigenvalue) - tolerance);
        final double relDiff = absDiff / tolerance;

        // Error message
        final StringBuilder builder = new StringBuilder();

        if (isPositive) {
            builder.append("The matrix is considered to be positive semi-definite, but one of its eigenvalues is negative");
        } else {
            builder.append("The matrix is not considered to be positive semi-definite, but all its eigenvalues are positive");
        }

        builder.append(System.lineSeparator());
        builder.append("Matrix");
        builder.append(System.lineSeparator());
        builder.append(matrix);
        builder.append(System.lineSeparator());
        builder.append("Eigenvalues");
        builder.append(System.lineSeparator());
        for (final double eigenvalue : eigenvalues) {
            builder.append(eigenvalue);
            builder.append(System.lineSeparator());
        }
        builder.append(System.lineSeparator());
        builder.append("Tolerance");
        builder.append(System.lineSeparator());
        builder.append(tolerance);
        builder.append(System.lineSeparator());
        builder.append(System.lineSeparator());
        builder.append("Difference between the tolerance and the smallest negative eigenvalue");
        builder.append(System.lineSeparator());
        builder.append("Absolute difference: ");
        builder.append(absDiff);
        builder.append(System.lineSeparator());
        builder.append("Relative difference: ");
        builder.append(relDiff);
        builder.append(System.lineSeparator());

        // Ensure the static method is in agreement with the eigen decomposition,
        // or ensure the largest negative eigenvalue is close to the tolerance if that's not the
        // case.
        final boolean testOk = isPositive == areEigenvaluesPositive || relDiff < errorMargin;
        Assert.assertTrue(builder.toString(), testOk);

        // Ensure the non-static method returns the same result as the static method
        final ArrayRowSymmetricPositiveMatrix positiveMatrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, matrix.getData(), 0., 0., null, null);
        Assert.assertEquals(isPositive, positiveMatrix.isPositiveSemiDefinite(tolerance));
    }
}
