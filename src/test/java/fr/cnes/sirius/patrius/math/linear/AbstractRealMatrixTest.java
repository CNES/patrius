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
* VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.6:FA:FA-2542:27/01/2021:[PATRIUS] Definition d'un champ de vue avec demi-angle de 180° 
 * VERSION:4.5.1:FA:FA-2540:04/08/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear
 * VERSION:4.5:DM:DM-2300:27/05/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.linear;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.IntStream;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.NoDataException;
import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Unit tests for {@linkplain AbstractRealMatrix}.
 *
 * @author Pierre Seimandi (GMV)
 */
public class AbstractRealMatrixTest {

    /** Default absolute tolerance used for the tests. */
    private static final double ABSTOL = 0.;

    /** Default relative tolerance used for the tests. */
    private static final double RELTOL = 1E-14;

    /** Fixed seed for the random number generator. */
    private static final Long FIXED_SEED = 5132923565228263612L;

    /** Random number generator used for the tests. */
    private static final Random RNG = CheckUtils.getRandomNumberGenerator(FIXED_SEED);

    /**
     * Tests the creation of new instances by specifying the dimensions of the matrix.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain AbstractRealMatrix#AbstractRealMatrix(int, int)}
     * </p>
     */
    @Test
    public void testConstructorDimension() {
        final SimpleMatrix matrix = new SimpleMatrix(3, 4);
        final double[][] expectedData = new double[3][4];
        CheckUtils.checkEquality(expectedData, matrix.getData(), 0., 0.);
        Assert.assertEquals(3, matrix.getRowDimension());
        Assert.assertEquals(4, matrix.getColumnDimension());
    }

    /**
     * Tests the creation of new instances when the specified dimensions are not valid.<br>
     * An exception should be thrown, since the constructor of {@linkplain AbstractRealMatrix}
     * protects against this.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain AbstractRealMatrix#AbstractRealMatrix(int, int)}
     * </p>
     */
    @Test
    public void testConstructorInvalidDimension() {
        try {
            new SimpleMatrix(0, 1);
            Assert.fail();
        } catch (final NotStrictlyPositiveException e) {
            final String expectedMessage = "invalid row dimension: 0 (must be positive)";
            Assert.assertEquals(NotStrictlyPositiveException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            new SimpleMatrix(1, 0);
            Assert.fail();
        } catch (final NotStrictlyPositiveException e) {
            final String expectedMessage = "invalid column dimension: 0 (must be positive)";
            Assert.assertEquals(NotStrictlyPositiveException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the creation of new instances by supplying the data array storing the entries of the
     * matrix.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain AbstractRealMatrix#AbstractRealMatrix(double[][])}<br>
     * </p>
     */
    @Test
    public void testConstructorDataArray() {
        // Data array
        final double[][] data = { { 8.07905, -4.25766, 6.12480, 5.74861 },
                { 4.34826, 4.46287, 1.70499, 2.79208 }, { 1.60868, 5.30236, -1.52292, 2.70868 },
                { -5.98578, -5.82153, 5.80140, 0.52514 }, { 3.14664, -0.44902, 1.56720, 9.27143 },
                { -2.91169, -5.59110, -4.33626, 8.44569 } };

        final RealMatrix matrix = new SimpleMatrix(data);
        Assert.assertEquals(6, matrix.getRowDimension());
        Assert.assertEquals(4, matrix.getColumnDimension());
        CheckUtils.checkEquality(data, matrix.getData(), 0., 0.);
    }

    /**
     * Tests the creation of new instances when the provided data array is {@code null}.<br>
     * An exception should be thrown, since the constructor of {@linkplain AbstractRealMatrix}
     * protects against this.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain AbstractRealMatrix#AbstractRealMatrix(double[][])}<br>
     * </p>
     */
    @Test
    public void testConstructorNullDataArray() {
        final double[][] nullData = null;
        final String expectedMessage = "the supplied array is null";

        try {
            new SimpleMatrix(nullData);
            Assert.fail();
        } catch (final NullArgumentException e) {
            Assert.assertEquals(NullArgumentException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the creation of new instances when the provided data array is empty.<br>
     * An exception should be thrown, since the constructor of {@linkplain AbstractRealMatrix}
     * protects against this.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain AbstractRealMatrix#AbstractRealMatrix(double[][])}<br>
     * </p>
     */
    @Test
    public void testConstructorEmptyDataArray() {
        final double[][] emptyRowData = new double[0][0];
        final double[][] emptyColumnData = new double[1][0];
        final String expectedMessage1 = "matrix must have at least one row";
        final String expectedMessage2 = "matrix must have at least one column";

        try {
            new SimpleMatrix(emptyRowData);
            Assert.fail();
        } catch (final NoDataException e) {
            Assert.assertEquals(NoDataException.class, e.getClass());
            Assert.assertEquals(expectedMessage1, e.getMessage());
        }

        try {
            new SimpleMatrix(emptyColumnData);
            Assert.fail();
        } catch (final NoDataException e) {
            Assert.assertEquals(NoDataException.class, e.getClass());
            Assert.assertEquals(expectedMessage2, e.getMessage());
        }
    }

    /**
     * Tests the creation of new instances when the provided data array is not valid.<br>
     * An exception should be thrown, since the constructor of {@linkplain AbstractRealMatrix}
     * protects against this.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain AbstractRealMatrix#AbstractRealMatrix(double[][])}<br>
     * </p>
     */
    @Test
    public void testConstructorInvalidDataArray() {
        final double[][] invalidData = { { 1. }, { 2., 3. } };
        final String expectedMessage = "2 != 1";

        // Two rows of different dimensions
        try {
            new SimpleMatrix(invalidData);
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the method that checks if the matrix is square and throws an exception if it is not.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain AbstractRealMatrix#checkSquare()}
     * </p>
     */
    @Test
    public void testCheckSquare() {
        // Square
        final SimpleMatrix squareMatrix = new SimpleMatrix(3, 3);
        squareMatrix.checkSquare();

        // Non-square matrix
        final SimpleMatrix rectangularMatrix = new SimpleMatrix(7, 4);
        try {
            rectangularMatrix.checkSquare();
            Assert.fail();
        } catch (final NonSquareMatrixException e) {
            Assert.assertEquals(NonSquareMatrixException.class, e.getClass());
            Assert.assertEquals("non square (7x4) matrix", e.getMessage());
        }
    }

    /**
     * Tests the method that determines if a matrix is square.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain AbstractRealMatrix#isSquare()}
     * </p>
     */
    @Test
    public void testIsSquare() {
        RealMatrix matrix;
        double[][] data;

        // Matrix filled with zeros
        matrix = new SimpleMatrix(3, 4);
        Assert.assertFalse(matrix.isSquare());

        matrix = new SimpleMatrix(4, 3);
        Assert.assertFalse(matrix.isSquare());

        matrix = new SimpleMatrix(4, 4);
        Assert.assertTrue(matrix.isSquare());

        // Standard matrix (3x3)
        data = new double[][] { { 9.44653, 4.00291, -8.76783 }, { 8.73896, 1.06224, 1.58758 },
                { 0.27935, -6.43146, 1.98667 } };
        matrix = new SimpleMatrix(data);
        Assert.assertTrue(matrix.isSquare());

        // Standard matrix (2x3)
        data = new double[][] { { 9.89649, 7.21517, 8.49849 }, { 2.55844, -1.72568, -3.32638 } };
        matrix = new SimpleMatrix(data);
        Assert.assertFalse(matrix.isSquare());

        // Standard matrix (3x2)
        data = new double[][] { { -7.95484, -9.71130 }, { -7.19630, -6.58251 },
                { -5.55104, 1.02669 } };
        matrix = new SimpleMatrix(data);
        Assert.assertFalse(matrix.isSquare());
    }

    /**
     * Tests the methods that determines if a matrix is diagonal.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain AbstractRealMatrix#isDiagonal(double)}<br>
     * </p>
     */
    @Test
    public void testIsDiagonal() {
        RealMatrix matrix;

        // Non-square matrix:
        // The method should always return false, regardless of the provided tolerance.
        matrix = new SimpleMatrix(2, 3);
        Assert.assertFalse(matrix.isDiagonal(Double.POSITIVE_INFINITY));

        matrix = new SimpleMatrix(3, 2);
        Assert.assertFalse(matrix.isDiagonal(Double.POSITIVE_INFINITY));

        // Square matrix filled with zeros:
        // The method should always return true, regardless of the provided tolerance.
        matrix = new SimpleMatrix(3, 3);
        Assert.assertTrue(matrix.isDiagonal(0.));

        // Diagonal matrix, except for one off-diagonal element
        final int dim = 4;
        final double threshold = 1E-8;
        final double epsilon = MathLib.ulp(threshold);
        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < dim; j++) {
                if (i != j) {
                    matrix = SimpleMatrix.createIdentityMatrix(dim);
                    matrix.setEntry(i, j, threshold);
                    Assert.assertFalse(matrix.isDiagonal(0.));
                    Assert.assertFalse(matrix.isDiagonal(threshold - epsilon));
                    Assert.assertTrue(matrix.isDiagonal(threshold));
                }
            }
        }
    }

    /**
     * Tests the methods that determines if a matrix is invertible.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain AbstractRealMatrix#isInvertible(double)}<br>
     * </p>
     */
    @Test
    public void testIsInvertible() {
        RealMatrix matrix;
        double[][] data;

        // Non-square matrix:
        // The method should always return false, regardless of the provided tolerance.
        matrix = new SimpleMatrix(2, 3);
        Assert.assertFalse(matrix.isInvertible(0.));

        matrix = new SimpleMatrix(3, 2);
        Assert.assertFalse(matrix.isInvertible(0.));

        // Square matrix filled with zeros:
        // The method should always return false, regardless of the provided tolerance.
        matrix = new SimpleMatrix(3, 3);
        Assert.assertFalse(matrix.isInvertible(0.));

        // Linearly dependent columns:
        // The method should always return false, regardless of the provided tolerance.
        data = new double[][] { { 1, 0, 0 }, { 0, 1, 1 }, { 0, 1, 1 } };
        matrix = new SimpleMatrix(data);
        Assert.assertFalse(matrix.isInvertible(0.));

        // Linearly independent columns:
        // The method should return true, unless the provided tolerance is too high.
        data = new double[][] { { 1, 0, 0 }, { 0, 1, 0 }, { 0, 0, 1 } };
        matrix = new SimpleMatrix(data);
        Assert.assertTrue(matrix.isInvertible(0.));
        Assert.assertTrue(matrix.isInvertible(1. - MathLib.ulp(1.)));
        Assert.assertFalse(matrix.isInvertible(1.));

        // The columns are not linearly independent, unless the tolerance is high enough.
        // The normality threshold should not be taken into account.
        final double epsilon = 7.397E-5;
        final List<int[]> indexPairs = CheckUtils.getCombinations(2, IntStream.range(0, 4)
                .toArray());

        for (final int[] indexPair : indexPairs) {
            final int index1 = indexPair[0];
            final int index2 = indexPair[1];

            matrix = SimpleMatrix.createIdentityMatrix(4);
            final RealVector column1 = matrix.getColumnVector(index1);
            final RealVector column2 = matrix.getColumnVector(index2);

            column1.setEntry(index1, 1. + epsilon);
            column1.setEntry(index2, 1. - epsilon);
            column2.setEntry(index1, 1. - epsilon);
            column2.setEntry(index2, 1. + epsilon);

            matrix.setColumnVector(index1, column1);
            matrix.setColumnVector(index2, column2);

            final double dotProduct1 = column1.dotProduct(column2);
            final double dotProduct2 = column1.getNorm() * column2.getNorm();

            final double absoluteMax = MathLib.max(MathLib.abs(dotProduct1),
                    MathLib.abs(dotProduct2));
            final double absoluteDifference = MathLib.abs(dotProduct1 - dotProduct2);
            final double relativeDifference = MathLib.divide(absoluteDifference, absoluteMax);

            Assert.assertTrue(matrix.isInvertible(0.));
            Assert.assertTrue(matrix.isInvertible(relativeDifference
                    - MathLib.ulp(relativeDifference)));
            Assert.assertFalse(matrix.isInvertible(relativeDifference));
        }
    }

    /**
     * Tests the methods that determines if a matrix is symmetric.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain AbstractRealMatrix#isSymmetric()}<br>
     * {@linkplain AbstractRealMatrix#isSymmetric(double)}<br>
     * {@linkplain AbstractRealMatrix#isSymmetric(double, double)}<br>
     * </p>
     */
    @Test
    public void testIsSymmetric() {
        RealMatrix matrix;
        double[][] data;

        // Non-square matrix:
        // The method should always return false, regardless of the provided tolerances.
        matrix = new SimpleMatrix(2, 3);
        Assert.assertFalse(matrix.isSymmetric());
        Assert.assertFalse(matrix.isSymmetric(Double.POSITIVE_INFINITY));
        Assert.assertFalse(matrix.isSymmetric(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));

        matrix = new SimpleMatrix(3, 2);
        Assert.assertFalse(matrix.isSymmetric());
        Assert.assertFalse(matrix.isSymmetric(Double.POSITIVE_INFINITY));
        Assert.assertFalse(matrix.isSymmetric(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));

        // Square matrix filled with zeros:
        // The method should always return true, regardless of the provided tolerances.
        matrix = new SimpleMatrix(3, 3);
        Assert.assertTrue(matrix.isSymmetric());
        Assert.assertTrue(matrix.isSymmetric(0.));
        Assert.assertTrue(matrix.isSymmetric(0., 0.));

        // Perfectly symmetric matrix:
        // The method should always return true, regardless of the provided tolerances.
        final double[][] initialData = { { 8.62673, -8.58350, 0.35693, 6.82708 },
                { -8.58350, 7.07194, 1.82748, -0.24792 }, { 0.35693, 1.82748, -3.42832, 6.75684 },
                { 6.82708, -0.24792, 6.75684, -5.21040 } };
        matrix = new SimpleMatrix(initialData);
        Assert.assertTrue(matrix.isSymmetric());
        Assert.assertTrue(matrix.isSymmetric(0.));
        Assert.assertTrue(matrix.isSymmetric(0., 0.));

        // Add a small perturbation to each element:
        // The method should always return true or false depending on the provided tolerances.
        final double epsilon = 4.6789E-5;

        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < i; j++) {
                data = CheckUtils.copyArray(initialData);
                data[i][j] += epsilon;
                data[j][i] -= epsilon;
                matrix = new SimpleMatrix(data);

                final double absoluteMax = MathLib.max(MathLib.abs(data[i][j]),
                        MathLib.abs(data[j][i]));
                final double absoluteDifference = MathLib.abs(data[i][j] - data[j][i]);
                final double relativeDifference = MathLib.divide(absoluteDifference, absoluteMax);

                Assert.assertFalse(matrix.isSymmetric());

                Assert.assertFalse(matrix.isSymmetric(0.));
                Assert.assertFalse(matrix.isSymmetric(relativeDifference
                        - MathLib.ulp(relativeDifference)));
                Assert.assertTrue(matrix.isSymmetric(relativeDifference));

                Assert.assertFalse(matrix.isSymmetric(0., 0.));
                Assert.assertFalse(matrix.isSymmetric(
                        relativeDifference - MathLib.ulp(relativeDifference), absoluteDifference
                                - MathLib.ulp(absoluteDifference)));
                Assert.assertTrue(matrix.isSymmetric(relativeDifference, absoluteDifference
                        - MathLib.ulp(absoluteDifference)));
                Assert.assertTrue(matrix.isSymmetric(
                        relativeDifference - MathLib.ulp(relativeDifference), absoluteDifference));
            }
        }
    }

    /**
     * Tests the methods that determines if a matrix is antisymmetric.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain AbstractRealMatrix#isAntisymmetric(double, double)}<br>
     * </p>
     */
    @Test
    public void testIsAntisymmetric() {
        RealMatrix matrix;
        double[][] data;

        // Non-square matrix:
        // The method should always return false, regardless of the provided tolerances.
        matrix = new SimpleMatrix(2, 3);
        Assert.assertFalse(matrix.isAntisymmetric(Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY));

        matrix = new SimpleMatrix(3, 2);
        Assert.assertFalse(matrix.isAntisymmetric(Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY));

        // Square matrix filled with zeros:
        // The method should always return true, regardless of the provided tolerances.
        matrix = new SimpleMatrix(3, 3);
        Assert.assertTrue(matrix.isAntisymmetric(0., 0.));

        // Perfectly antisymmetric matrix:
        // The method should always return true, regardless of the provided tolerances.
        final double[][] initialData = { { 0., +2.14975, -6.60512, +9.74352 },
                { -2.14975, 0., +7.11508, -5.18442 }, { +6.60512, -7.11508, 0., +5.31786 },
                { -9.74352, +5.18442, -5.31786, 0. } };
        matrix = new SimpleMatrix(initialData);
        Assert.assertTrue(matrix.isAntisymmetric(0., 0.));

        // Add a small perturbation to each element:
        // The method should always return true or false depending on the provided tolerances.
        final double epsilon = 2.3998E-7;

        for (int i = 0; i < 4; i++) {
            // Non-zero diagonal element:
            // Only the absolute tolerance should be taken into account.
            data = CheckUtils.copyArray(initialData);
            data[i][i] += epsilon;
            matrix = new SimpleMatrix(data);
            Assert.assertFalse(matrix.isAntisymmetric(Double.POSITIVE_INFINITY, 0.));
            Assert.assertFalse(matrix.isAntisymmetric(Double.POSITIVE_INFINITY,
                    epsilon - MathLib.ulp(epsilon)));
            Assert.assertTrue(matrix.isAntisymmetric(Double.POSITIVE_INFINITY, epsilon));

            for (int j = 0; j < i; j++) {
                data = CheckUtils.copyArray(initialData);
                data[i][j] += epsilon;
                matrix = new SimpleMatrix(data);

                final double absoluteMax = MathLib.max(MathLib.abs(data[i][j]),
                        MathLib.abs(data[j][i]));
                final double absoluteDifference = MathLib.abs(data[i][j] + data[j][i]);
                final double relativeDifference = MathLib.divide(absoluteDifference, absoluteMax);

                Assert.assertFalse(matrix.isAntisymmetric(0., 0.));
                Assert.assertFalse(matrix.isAntisymmetric(
                        relativeDifference - MathLib.ulp(relativeDifference), absoluteDifference
                                - MathLib.ulp(absoluteDifference)));
                Assert.assertTrue(matrix.isAntisymmetric(relativeDifference, absoluteDifference
                        - MathLib.ulp(absoluteDifference)));
                Assert.assertTrue(matrix.isAntisymmetric(
                        relativeDifference - MathLib.ulp(relativeDifference), absoluteDifference));
            }
        }
    }

    /**
     * Tests the methods that determines if a matrix is orthogonal.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain AbstractRealMatrix#isOrthogonal(double, double)}<br>
     * </p>
     */
    @Test
    public void testIsOrthogonal() {
        RealMatrix matrix;
        double[][] data;

        // Non-square matrix:
        // The method should always return false, regardless of the provided tolerances.
        matrix = new SimpleMatrix(2, 3);
        Assert.assertFalse(matrix.isOrthogonal(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));

        // Square matrix filled with zeros:
        // The method should always return false, regardless of the provided tolerances.
        matrix = new SimpleMatrix(3, 3);
        Assert.assertTrue(matrix.isOrthogonal(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));

        // Perfectly orthogonal matrix (Householder transformation matrix):
        // The method should always return true, regardless of the provided tolerances.
        final double[][] initialData = new double[4][4];
        initialData[0][0] = 1.;
        initialData[1][1] = -1. / 3.;
        initialData[2][1] = +2. / 3.;
        initialData[1][2] = +2. / 3.;
        initialData[2][2] = +2. / 3.;
        initialData[3][1] = -2. / 3.;
        initialData[1][3] = -2. / 3.;
        initialData[3][2] = +1. / 3.;
        initialData[2][3] = +1. / 3.;
        initialData[3][3] = +2. / 3.;

        matrix = new SimpleMatrix(initialData);
        Assert.assertTrue(matrix.isOrthogonal(0., 0.));

        // The norm of the column is not exactly equal to 1.
        // The orthogonality threshold should not be taken into account.
        final double epsilon = 1.493E-6;

        for (int i = 0; i < 4; i++) {
            data = CheckUtils.copyArray(initialData);
            data[i][i] += epsilon;
            matrix = new SimpleMatrix(data);

            final RealVector column = matrix.getColumnVector(i);
            final double norm = column.getNorm();
            final double absoluteMax = MathLib.max(norm, 1.0);
            final double absoluteDifference = MathLib.abs(norm - 1.0);
            final double relativeDifference = MathLib.divide(absoluteDifference, absoluteMax);

            Assert.assertFalse(matrix.isOrthogonal(0., Double.POSITIVE_INFINITY));
            Assert.assertFalse(matrix.isOrthogonal(
                    relativeDifference - MathLib.ulp(relativeDifference), Double.POSITIVE_INFINITY));
            Assert.assertTrue(matrix.isOrthogonal(relativeDifference, Double.POSITIVE_INFINITY));
        }

        // The columns are not perfectly orthogonal.
        // The normality threshold should not be taken into account.
        final List<int[]> indexPairs = CheckUtils.getCombinations(2, IntStream.range(0, 4)
                .toArray());

        for (final int[] indexPair : indexPairs) {
            matrix = SimpleMatrix.createIdentityMatrix(4);

            // Retrieve the two columns to be tested
            final int index1 = indexPair[0];
            final int index2 = indexPair[1];

            final RealVector column1 = matrix.getColumnVector(index1);
            final RealVector column2 = matrix.getColumnVector(index2);

            // Set the cross term to a non-zero value
            column1.setEntry(index2, epsilon);
            column2.setEntry(index1, epsilon);

            matrix.setColumnVector(index1, column1);
            matrix.setColumnVector(index2, column2);

            // Test the method
            final double dotProduct = column1.dotProduct(column2);
            Assert.assertFalse(matrix.isOrthogonal(Double.POSITIVE_INFINITY, 0.));
            Assert.assertFalse(matrix.isOrthogonal(Double.POSITIVE_INFINITY,
                    dotProduct - MathLib.ulp(dotProduct)));
            Assert.assertTrue(matrix.isOrthogonal(Double.POSITIVE_INFINITY, dotProduct));
        }
    }

    /**
     * Tests the method that returns the minimum value in the matrix.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain AbstractRealMatrix#getMin()}
     * </p>
     */
    @Test
    public void testGetMin() {
        // Tested matrix
        final double[][] data = { { 2.35639, -2.78746, 4.82891, -0.71887 },
                { -6.24815, 0.88230, 8.32589, 3.31554 },
                { -1.62108, -9.49048, -9.15960, -2.29497 },
                { 4.26757, 5.89620, -2.90152, -3.79535 }, { -6.72737, 1.89045, 3.51700, 4.79905 },
                { -3.67920, 9.03245, 5.52913, -8.62147 } };
        final RealMatrix matrix = new SimpleMatrix(data);

        // Test the method
        Assert.assertEquals(-9.49048, matrix.getMin(), 0.);
    }

    /**
     * Tests the method that returns the maximum value in the matrix.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain AbstractRealMatrix#getMax()}
     * </p>
     */
    @Test
    public void testGetMax() {
        // Tested matrix
        final double[][] data = { { -8.51756, 6.35722, -8.51210, -7.07860 },
                { 2.79282, 9.68088, 5.00724, -5.65473 }, { -4.53290, -5.92541, 5.59738, 3.20725 },
                { 2.95600, -4.48622, 8.36183, 5.60165 },
                { -1.80463, -7.58314, -7.78892, -8.82253 }, { -8.63062, 1.17801, 0.81403, 1.61800 } };
        final RealMatrix matrix = new SimpleMatrix(data);

        // Test the method
        Assert.assertEquals(9.68088, matrix.getMax(), 0.);
    }

    /**
     * Tests the method that computes the trace of the matrix.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain AbstractRealMatrix#getTrace()}
     * </p>
     */
    @Test
    public void testGetTrace() {
        // Tested matrix
        final double[][] data = { { 1.32408, -1.94712, 2.12876, 7.66507 },
                { -5.74402, 3.22441, -4.73223, -5.80499 },
                { -1.01981, -3.13665, 4.48773, -1.98536 },
                { -9.31868, -0.84648, -7.21073, -5.08395 } };
        final RealMatrix matrix = new SimpleMatrix(data);

        // Test the method
        CheckUtils.checkEquality(3.95227, matrix.getTrace(), ABSTOL, RELTOL);
    }

    /**
     * Tests the method that computes the trace of the matrix on a non-square matrix.<br>
     * An exception should be thrown, as the trace is not defined for non-square matrices.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain AbstractRealMatrix#getTrace()}
     * </p>
     */
    @Test
    public void testGetTraceNonSquareMatrix() {
        // Tested matrix
        final RealMatrix matrix = new SimpleMatrix(3, 4);

        // Test the method
        try {
            matrix.getTrace();
            Assert.fail();
        } catch (final NonSquareMatrixException e) {
            Assert.assertEquals(NonSquareMatrixException.class, e.getClass());
            Assert.assertEquals("non square (3x4) matrix", e.getMessage());
        }
    }

    /**
     * Tests the method that computes the norm of the matrix (maximum absolute row sum).
     *
     * <p>
     * Tested method:<br>
     * {@linkplain AbstractRealMatrix#getNorm()}
     * </p>
     */
    @Test
    public void testGetNorm() {
        // Tested matrix
        final double[][] data = { { 1.06942, -8.39389, -9.51299, -3.70379 },
                { 1.15014, -2.19417, 6.12791, -3.97649 }, { 6.94004, -9.50095, -3.22220, 4.56187 },
                { 8.38183, 3.24942, 2.80059, -8.05784 },
                { -2.35060, -8.04953, -0.92369, -0.59453 }, { 7.25848, 0.47525, 4.13992, 3.57105 } };
        final RealMatrix matrix = new SimpleMatrix(data);

        // Test the method
        CheckUtils.checkEquality(31.86321, matrix.getNorm(), ABSTOL, RELTOL);
    }

    /**
     * Tests the method that computes the Frobenius norm of the matrix.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain AbstractRealMatrix#getFrobeniusNorm()}
     * </p>
     */
    @Test
    public void testGetFrobeniusNorm() {
        // Tested matrix
        final double[][] data = { { -8.47651, -3.56742, 4.13445, -8.88334 },
                { 2.66942, 9.87758, -5.77456, -3.35504 }, { 9.02067, 1.99827, 5.90245, -6.45388 },
                { 3.58690, -5.98330, -2.37203, 0.06113 },
                { -2.40398, 3.00973, -8.30864, -1.13753 },
                { -0.78670, -7.22250, -6.71463, -8.19335 } };
        final RealMatrix matrix = new SimpleMatrix(data);

        // Test the method
        CheckUtils.checkEquality(28.21698244040812, matrix.getFrobeniusNorm(), ABSTOL, RELTOL);
    }

    /**
     * Tests the methods that return the entries of the matrix in a 2D data array.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain AbstractRealMatrix#getData()}<br>
     * {@linkplain AbstractRealMatrix#getData(boolean)}<br>
     * </p>
     */
    @Test
    public void testGetData() {
        double[][] result;

        // Tested matrix
        final double[][] data = { { 6.04301, 1.04544, 2.65879, -6.17061 },
                { 0.97161, 9.96295, -3.16362, 0.41029 }, { 8.31406, 4.14577, 0.52750, 6.03044 },
                { 6.06159, -7.90833, -0.84542, 6.64255 }, { 8.29085, -9.95362, 3.39271, 3.18244 },
                { 1.03313, 5.33374, -6.56457, 3.86709 } };
        final RealMatrix matrix = new SimpleMatrix(data);

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
     * Tests the methods that return the transpose of the matrix, using a square matrix.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain AbstractRealMatrix#transpose()}<br>
     * {@linkplain AbstractRealMatrix#transpose(boolean)}<br>
     * </p>
     */
    @Test
    public void testTransposeSquareMatrix() {
        double[][] data;
        RealMatrix result;

        // Tested matrix
        data = new double[][] { { -1.95014, -4.72816, -9.96638, -6.79215 },
                { -5.62136, 0.64761, -8.77481, 6.09092 },
                { -2.97550, -1.05437, 4.88507, -5.52749 }, { -8.55663, -3.14475, 9.09392, 6.68311 } };
        final RealMatrix matrix = new SimpleMatrix(data);

        // Expected matrix
        data = new double[][] { { -1.95014, -5.62136, -2.97550, -8.55663 },
                { -4.72816, 0.64761, -1.05437, -3.14475 },
                { -9.96638, -8.77481, 4.88507, 9.09392 }, { -6.79215, 6.09092, -5.52749, 6.68311 } };
        final RealMatrix expected = new SimpleMatrix(data);

        // Test the method
        result = matrix.transpose();
        CheckUtils.checkEquality(expected, result, 0., 0.);
        Assert.assertEquals(matrix.getClass(), result.getClass());
        Assert.assertNotSame(matrix, result);

        result = matrix.transpose(true);
        CheckUtils.checkEquality(expected, result, 0., 0.);
        Assert.assertEquals(matrix.getClass(), result.getClass());
        Assert.assertNotSame(matrix, result);

        result = matrix.transpose(false);
        CheckUtils.checkEquality(expected, result, 0., 0.);
        Assert.assertEquals(matrix.getClass(), result.getClass());
        Assert.assertNotSame(matrix, result);
    }

    /**
     * Tests the methods that return the transpose of the matrix, using a symmetric matrix.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain AbstractRealMatrix#transpose()}<br>
     * {@linkplain AbstractRealMatrix#transpose(boolean)}<br>
     * </p>
     */
    @Test
    public void testTransposeSymmetricMatrix() {
        RealMatrix result;

        // Tested matrix
        final double[][] data = { { 2.04493, 0.74917, 6.39556, -4.00843 },
                { 0.74917, -6.20111, -9.74255, -9.30975 },
                { 6.39556, -9.74255, 1.74216, -3.56708 },
                { -4.00843, -9.30975, -3.56708, -0.13275 } };
        final RealMatrix matrix = new SimpleMatrix(data);

        // Expected matrix
        final RealMatrix expected = new SimpleMatrix(data);

        // Test the method
        result = matrix.transpose();
        CheckUtils.checkEquality(expected, result, 0., 0.);
        Assert.assertEquals(matrix.getClass(), result.getClass());
        Assert.assertNotSame(matrix, result);

        result = matrix.transpose(true);
        CheckUtils.checkEquality(expected, result, 0., 0.);
        Assert.assertEquals(matrix.getClass(), result.getClass());
        Assert.assertNotSame(matrix, result);

        result = matrix.transpose(false);
        CheckUtils.checkEquality(expected, result, 0., 0.);
        Assert.assertEquals(matrix.getClass(), result.getClass());
        Assert.assertNotSame(matrix, result);
    }

    /**
     * Tests the methods that return the transpose of the matrix, using a rectangular matrix.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain AbstractRealMatrix#transpose()}<br>
     * {@linkplain AbstractRealMatrix#transpose(boolean)}<br>
     * </p>
     */
    @Test
    public void testTransposeRectangularMatrix() {
        double[][] data;
        RealMatrix result;

        // Tested matrix
        data = new double[][] { { 5.90968, -7.81299, 5.47840, 6.73873 },
                { 8.23359, 8.19101, 1.11566, 1.78065 }, { -9.89735, 9.20106, 4.01106, 2.36496 },
                { 0.47556, 7.75391, 7.90534, 4.66031 }, { -3.29091, -2.85529, -0.04891, 4.44521 },
                { -6.54234, 2.66521, 1.98073, -7.21107 } };
        final RealMatrix matrix = new SimpleMatrix(data);

        // Expected matrix
        data = new double[][] { { 5.90968, 8.23359, -9.89735, 0.47556, -3.29091, -6.54234 },
                { -7.81299, 8.19101, 9.20106, 7.75391, -2.85529, 2.66521 },
                { 5.47840, 1.11566, 4.01106, 7.90534, -0.04891, 1.98073 },
                { 6.73873, 1.78065, 2.36496, 4.66031, 4.44521, -7.21107 } };
        final RealMatrix expected = new SimpleMatrix(data);

        // Test the method
        result = matrix.transpose();
        CheckUtils.checkEquality(expected, result, 0., 0.);
        Assert.assertEquals(matrix.getClass(), result.getClass());
        Assert.assertNotSame(matrix, result);

        result = matrix.transpose(true);
        CheckUtils.checkEquality(expected, result, 0., 0.);
        Assert.assertEquals(matrix.getClass(), result.getClass());
        Assert.assertNotSame(matrix, result);

        result = matrix.transpose(false);
        CheckUtils.checkEquality(expected, result, 0., 0.);
        Assert.assertEquals(matrix.getClass(), result.getClass());
        Assert.assertNotSame(matrix, result);
    }

    /**
     * Tests the method that raises a matrix to the power of N.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain AbstractRealMatrix#power(int)}<br>
     * </p>
     */
    @Test
    public void testPower() {
        double[][] data;
        RealMatrix result;
        RealMatrix expected;

        // Tested matrix
        data = new double[][] { { 7.90942, -9.31048, 9.51292, 0.75187 },
                { 2.14933, -0.07709, -7.02984, -7.03785 },
                { 5.18587, -8.62475, -7.83277, 7.39978 },
                { -3.41992, -6.74785, -2.76617, -4.30689 } };
        final RealMatrix matrix = new SimpleMatrix(data);

        // Exponent is 0
        result = matrix.power(0);
        expected = SimpleMatrix.createIdentityMatrix(4);
        CheckUtils.checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertNotSame(matrix, result);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());

        // Exponent is 1
        result = matrix.power(1);
        expected = new SimpleMatrix(data);
        CheckUtils.checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertNotSame(matrix, result);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());

        // Exponent is 2
        result = matrix.power(2);
        data = new double[][] {
                { 89.309061948, -160.0428145679, 64.100549803299980, 138.6279110567 },
                { 4.4473094501, 88.1156175522, 95.5193441005, -19.549459045100004 },
                { -43.4465925996, -29.994979355599995, 150.8466154107, -25.232016410399996 },
                { -41.168718856, 85.2810609121, 48.4832437798, 42.9992729016 } };
        expected = new SimpleMatrix(data);
        CheckUtils.checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertNotSame(matrix, result);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());

        // Exponent is 7
        result = matrix.power(7);
        data = new double[][] {
                { -12432472.720135866, 4996069.28989578, -16375832.266333774, -4039168.5027812035 },
                { 11789049.125014637, -10566870.550200066, -28622932.344708823, 30797492.470740683 },
                { 3739808.891224562, 15665546.512515996, -12854251.158658992, 16397293.744294755 },
                { 18372991.046097297, -4074419.5972587527, -38088994.97008134, 19165923.712162435 } };
        expected = new SimpleMatrix(data);
        CheckUtils.checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertNotSame(matrix, result);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());
    }

    /**
     * Tests the method that raises a matrix to the power of N, using a negative exponent.<br>
     * An exception should be thrown as negative exponents are not allowed.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain AbstractRealMatrix#power(int)}<br>
     * </p>
     */
    @Test
    public void testPowerNegativeExponent() {
        // Tested matrix
        final RealMatrix matrix = new SimpleMatrix(4, 4);

        // Test the method
        CheckUtils.checkPowerNegativeExponent(matrix);
    }

    /**
     * Tests the method that raises a matrix to the power of N, using a non-square matrix.<br>
     * An exception should be thrown as the power operation is not defined for rectangular matrices.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain AbstractRealMatrix#power(int)}<br>
     * </p>
     */
    @Test
    public void testPowerRectangularMatrix() {
        // Tested matrix
        final RealMatrix matrix = new SimpleMatrix(4, 5);

        // Test the method
        CheckUtils.checkPowerNonSquareMatrix(matrix);
    }

    /**
     * Tests the method that returns an element of the matrix.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain AbstractRealMatrix#getEntry(int, int)}
     * </p>
     */
    @Test
    public void testGetEntry() {
        // Tested matrix
        final double[][] data = { { -5.06431, 3.92955, -5.02609, -0.52662 },
                { -9.80586, -0.06692, 3.51117, -9.38936 }, { 7.31116, 4.01831, 9.29466, 5.45151 },
                { -0.67706, -2.38185, 5.11011, -9.05432 }, { 7.12084, -9.88894, 5.69452, 8.92780 },
                { -5.93043, -2.52412, 2.69914, -0.78720 } };
        final RealMatrix matrix = new SimpleMatrix(data);

        // Dimensions
        final int nr = matrix.getRowDimension();
        final int nc = matrix.getColumnDimension();

        // Check all the entries
        for (int i = 0; i < nr; i++) {
            for (int j = 0; j < nc; j++) {
                Assert.assertEquals(data[i][j], matrix.getEntry(i, j), 0.);
            }
        }
    }

    /**
     * Tests the method that returns an element of the matrix, using indices outside the valid index
     * range.<br>
     * An exception should be thrown.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain AbstractRealMatrix#getEntry(int, int)}
     * </p>
     */
    @Test
    public void testGetOutOfRangeEntry() {
        final RealMatrix matrix = new SimpleMatrix(3, 4);
        CheckUtils.checkGetOutOfRangeEntry(matrix);
    }

    /**
     * Tests the method that sets an entry to a new value.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain AbstractRealMatrix#setEntry(int, int, double)}
     * </p>
     */
    @Test
    public void testSetEntry() {
        // Tested matrix
        final double[][] data = { { 6.90067, 1.70885, 9.34277, 1.72382 },
                { 6.87144, -4.32911, -1.20759, -3.79111 },
                { 4.32784, -5.37467, 9.26793, -3.02010 },
                { -0.89087, -0.92666, -8.19668, -3.37851 },
                { -7.80286, 8.95894, 3.29125, 6.34449 }, { 9.22768, -2.57639, -0.23349, 3.25758 } };
        final RealMatrix matrix = new SimpleMatrix(data);

        // Dimensions of the matrix
        final int nr = matrix.getRowDimension();
        final int nc = matrix.getColumnDimension();

        // Test the method
        final double[][] expected = matrix.getData();
        for (int i = 0; i < nr; i++) {
            for (int j = 0; j < nc; j++) {
                final double d = -1. + 2. * RNG.nextDouble();
                matrix.setEntry(i, j, d);
                expected[i][j] = d;
                CheckUtils.checkEquality(expected, matrix.getData(), 0., 0.);
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
     * {@linkplain AbstractRealMatrix#setEntry(int, int, double)}
     * </p>
     */
    @Test
    public void testSetOutOfRangeEntry() {
        final RealMatrix symmetricMatrix = new SimpleMatrix(3, 5);
        CheckUtils.checkSetOutOfRangeEntry(symmetricMatrix);
    }

    /**
     * Tests the method that adds a scalar to an entry.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain AbstractRealMatrix#addToEntry(int, int, double)}
     * </p>
     */
    @Test
    public void testAddToEntry() {
        // Tested matrix
        final double[][] data = { { 3.05774, 9.37121, -9.06466, -7.82415 },
                { -1.78567, -5.69972, 5.47404, 4.72836 },
                { -0.71335, -3.45773, -5.21115, -1.26375 }, { 0.14733, 3.43350, 8.49616, 7.87796 },
                { 3.74105, -5.39193, 7.21555, -5.38046 }, { 5.14404, 7.23341, -2.38862, -6.67693 } };
        final RealMatrix matrix = new SimpleMatrix(data);

        // Dimensions of the matrix
        final int nr = matrix.getRowDimension();
        final int nc = matrix.getColumnDimension();

        // Test the method
        final double[][] expected = matrix.getData();
        for (int i = 0; i < nr; i++) {
            for (int j = 0; j < nc; j++) {
                final double d = CheckUtils.getRandomNumber(RNG, -10., +10.);
                matrix.addToEntry(i, j, d);
                expected[i][j] += d;
                CheckUtils.checkEquality(expected, matrix.getData(), ABSTOL, RELTOL);
            }
        }
    }

    /**
     * Tests the method that adds a scalar to an entry, using indices outside the valid index range.<br>
     * An exception should be thrown.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain AbstractRealMatrix#addToEntry(int, int, double)}
     * </p>
     */
    @Test
    public void testAddToOutOfRangeEntry() {
        final RealMatrix matrix = new SimpleMatrix(3, 5);
        CheckUtils.checkAddToOutOfRangeEntry(matrix);
    }

    /**
     * Tests the method that multiplies an entry by a scalar.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain AbstractRealMatrix#multiplyEntry(int, int, double)}
     * </p>
     */
    @Test
    public void testMultiplyEntry() {
        final double[][] data = { { 4.23285, -3.76782, 3.92024, -6.58892 },
                { 8.06170, 5.59611, 4.64394, 7.86111 }, { -4.29893, 4.92651, -3.08284, 4.28784 },
                { -4.91506, 8.60632, 2.02705, -9.03510 }, { 7.16547, 5.00814, -6.21599, -5.62552 },
                { 9.84080, 6.25591, -6.01215, -2.89511 } };
        final RealMatrix matrix = new SimpleMatrix(data);

        // Dimensions of the matrix
        final int nr = matrix.getRowDimension();
        final int nc = matrix.getColumnDimension();

        final double[][] expected = matrix.getData();
        for (int i = 0; i < nr; i++) {
            for (int j = 0; j < nc; j++) {
                final double d = CheckUtils.getRandomNumber(RNG, -10., +10.);
                matrix.multiplyEntry(i, j, d);
                expected[i][j] *= d;
                CheckUtils.checkEquality(expected, matrix.getData(), ABSTOL, RELTOL);
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
     * {@linkplain AbstractRealMatrix#multiplyEntry(int, int, double)}
     * </p>
     */
    @Test
    public void testMultiplyOutOfRangeEntry() {
        final RealMatrix matrix = new SimpleMatrix(7, 4);
        CheckUtils.checkAddToOutOfRangeEntry(matrix);
    }

    /**
     * Tests the method that adds a scalar to the entries of the matrix.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain AbstractRealMatrix#scalarAdd(double)}
     * </p>
     */
    @Test
    public void testScalarAdd() {
        double[][] expected;
        RealMatrix result;

        // Tested matrix
        final double[][] data = { { -6.25799, -0.13132, 6.52096, 1.82394 },
                { -4.17817, 4.28442, -5.83205, -3.77100 }, { 7.86189, 2.56983, 5.12726, -9.52565 },
                { -9.09668, -3.12166, -6.45518, 2.57388 },
                { -8.30885, -1.51484, 5.58490, 0.96323 }, { 6.57250, -9.10574, -7.35489, 6.49309 } };
        final RealMatrix matrix = new SimpleMatrix(data);

        // Test the method
        result = matrix.scalarAdd(0);
        expected = data;
        CheckUtils.checkEquality(expected, result.getData(), ABSTOL, RELTOL);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());
        Assert.assertNotSame(matrix, result);

        result = matrix.scalarAdd(+3.784);
        expected = new double[][] { { -2.47399, 3.65268, 10.30496, 5.60794 },
                { -0.39417, 8.06842, -2.04805, 0.01300 }, { 11.64589, 6.35383, 8.91126, -5.74165 },
                { -5.31268, 0.66234, -2.67118, 6.35788 }, { -4.52485, 2.26916, 9.36890, 4.74723 },
                { 10.35650, -5.32174, -3.57089, 10.27709 } };
        CheckUtils.checkEquality(expected, result.getData(), ABSTOL, RELTOL);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());
        Assert.assertNotSame(matrix, result);

        result = matrix.scalarAdd(-8.193);
        expected = new double[][] { { -14.45099, -8.32432, -1.67204, -6.36906 },
                { -12.37117, -3.90858, -14.02505, -11.96400 },
                { -0.33111, -5.62317, -3.06574, -17.71865 },
                { -17.28968, -11.31466, -14.64818, -5.61912 },
                { -16.50185, -9.70784, -2.60810, -7.22977 },
                { -1.62050, -17.29874, -15.54789, -1.69991 } };
        CheckUtils.checkEquality(expected, result.getData(), ABSTOL, RELTOL);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());
        Assert.assertNotSame(matrix, result);
    }

    /**
     * Tests the method that multiplies the matrix by a scalar.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain AbstractRealMatrix#scalarMultiply(double)}
     * </p>
     */
    @Test
    public void testScalarMultiply() {
        double[][] expected;
        RealMatrix result;

        // Tested matrix
        final double[][] data = { { -8.44271, -1.26763, -5.44517, 5.73372 },
                { -4.05037, -6.15121, 6.90927, 0.70271 }, { 6.56914, -0.54960, 0.71674, 2.83920 },
                { -3.45643, -4.22348, 4.06180, 6.63078 }, { 4.11040, 3.99004, 7.08375, 7.77574 },
                { -8.19322, -8.88164, 4.94769, -5.95611 } };
        final RealMatrix matrix = new SimpleMatrix(data);

        // Test the method
        result = matrix.scalarMultiply(0);
        expected = new double[6][4];
        CheckUtils.checkEquality(expected, result.getData(), ABSTOL, RELTOL);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());
        Assert.assertNotSame(matrix, result);

        result = matrix.scalarMultiply(1.0);
        expected = data;
        CheckUtils.checkEquality(expected, result.getData(), ABSTOL, RELTOL);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());
        Assert.assertNotSame(matrix, result);

        result = matrix.scalarMultiply(-5.248);
        expected = new double[][] { { 44.30734208, 6.65252224, 28.57625216, -30.09056256 },
                { 21.25634176, 32.28155008, -36.25984896, -3.68782208 },
                { -34.47484672, 2.88430080, -3.76145152, -14.90012160 },
                { 18.13934464, 22.16482304, -21.31632640, -34.79833344 },
                { -21.57137920, -20.93972992, -37.17552000, -40.80708352 },
                { 42.99801856, 46.61084672, -25.96547712, 31.25766528 } };
        CheckUtils.checkEquality(expected, result.getData(), ABSTOL, RELTOL);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());
        Assert.assertNotSame(matrix, result);
    }

    /**
     * Tests the method that adds another matrix.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain AbstractRealMatrix#add(RealMatrix)}
     * </p>
     */
    @Test
    public void testAddMatrix() {
        double[][] data;

        // Tested matrix
        data = new double[][] { { 3.09135, -3.32412, -2.82266, 2.38291 },
                { 2.47701, -9.43211, 7.18339, -8.61639 }, { 3.48075, -5.07374, 9.36887, -8.95910 },
                { 8.91607, -5.51814, 7.99765, 8.86519 }, { -4.12613, 6.80467, 6.71488, -8.95761 },
                { -7.68337, -9.98993, 4.66697, -6.80508 } };
        final RealMatrix matrix = new SimpleMatrix(data);

        // Matrix to be added
        data = new double[][] { { -8.28850, 5.41008, -2.92502, 6.37318 },
                { 4.65508, -8.18728, 6.70973, -8.06043 }, { 9.86096, -2.07911, -2.55828, 5.18662 },
                { -2.58706, -3.64396, -9.72160, 4.05661 }, { 9.10790, 2.40293, -4.98456, 2.76404 },
                { 8.91405, 3.37240, -3.59142, 7.46110 } };
        final RealMatrix realMatrix = new Array2DRowRealMatrix(data);

        // Expected matrix
        data = new double[][] { { -5.19715, 2.08596, -5.74768, 8.75609 },
                { 7.13209, -17.61939, 13.89312, -16.67682 },
                { 13.34171, -7.15285, 6.81059, -3.77248 },
                { 6.32901, -9.16210, -1.72395, 12.92180 }, { 4.98177, 9.20760, 1.73032, -6.19357 },
                { 1.23068, -6.61753, 1.07555, 0.65602 } };
        final RealMatrix expected = new Array2DRowRealMatrix(data);

        // Test the addition
        final RealMatrix result = matrix.add(realMatrix);
        CheckUtils.checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());
        Assert.assertNotSame(matrix, result);
    }

    /**
     * Tests the method that adds another matrix, using a matrix with incompatible dimensions.<br>
     * An exception should be thrown.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain AbstractRealMatrix#add(RealMatrix)}<br>
     * </p>
     */
    @Test
    public void testAddIncompatibleMatrix() {
        final RealMatrix matrix = new SimpleMatrix(4, 8);
        CheckUtils.checkAddIncompatibleRealMatrix(matrix);
        CheckUtils.checkAddIncompatibleSymmetricMatrix(matrix);
        CheckUtils.checkAddIncompatibleSymmetricPositiveMatrix(matrix);
        CheckUtils.checkAddIncompatibleDecomposedMatrix(matrix);
        CheckUtils.checkAddIncompatibleDiagonalMatrix(matrix);
    }

    /**
     * Tests the method that subtracts another matrix.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain AbstractRealMatrix#subtract(RealMatrix)}
     * </p>
     */
    @Test
    public void testSubtractMatrix() {
        double[][] data;

        // Tested matrix
        data = new double[][] { { -3.18265, -2.98696, -6.01868, 6.45569 },
                { -9.34605, 9.69786, -4.34381, -3.40056 },
                { -3.96186, -7.71684, 6.63645, -8.47673 },
                { 1.03894, -4.12821, 4.96148, -3.74774 }, { 0.35333, 0.59696, 6.90685, -7.09741 },
                { -6.39440, -4.28782, 1.15821, -7.74573 } };
        final RealMatrix matrix = new SimpleMatrix(data);

        // Matrix to be added
        data = new double[][] { { -9.51797, -4.41123, 0.76504, 5.77317 },
                { 3.25983, 6.20043, 1.60987, -4.96042 },
                { -2.94356, -9.69898, -5.21312, -7.18918 },
                { 8.29710, -9.98866, 3.61404, -3.46804 }, { 6.86864, -1.66500, 3.24311, -6.86151 },
                { -7.47169, -8.68203, -5.34267, 7.78580 } };
        final RealMatrix realMatrix = new Array2DRowRealMatrix(data);

        // Expected matrix
        data = new double[][] { { 6.335319999999999, 1.42427, -6.78372, 0.682519999999999 },
                { -12.605879999999999, 3.49743, -5.95368, 1.55986 },
                { -1.0183, 1.98214, 11.84957, -1.28755 },
                { -7.25816, 5.860449999999999, 1.34744, -0.2797 },
                { -6.51531, 2.26196, 3.66374, -0.2359 },
                { 1.07729, 4.394209999999999, 6.50088, -15.53153 } };
        final RealMatrix expected = new Array2DRowRealMatrix(data);

        // Test the subtraction
        final RealMatrix result = matrix.subtract(realMatrix);
        CheckUtils.checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());
        Assert.assertNotSame(matrix, result);
    }

    /**
     * Tests the method that subtracts another matrix, using a matrix with incompatible dimensions.<br>
     * An exception should be thrown.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain AbstractRealMatrix#subtract(RealMatrix)}<br>
     * </p>
     */
    @Test
    public void testSubtractIncompatibleMatrix() {
        final RealMatrix matrix = new SimpleMatrix(6, 3);
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
     * {@linkplain AbstractRealMatrix#multiply(RealMatrix)}<br>
     * {@linkplain AbstractRealMatrix#multiply(RealMatrix, boolean)}<br>
     * {@linkplain AbstractRealMatrix#multiply(RealMatrix, boolean, double)}<br>
     * {@linkplain MatrixUtils#multiplyByTranspose(RealMatrix, RealMatrix)}<br>
     * {@linkplain MatrixUtils#multiplyByTranspose(double, RealMatrix, RealMatrix)}<br>
     * </p>
     */
    @Test
    public void testMultiplyBySquareMatrix() {
        double[][] data;
        RealMatrix result;
        RealMatrix expected;

        // Tested matrix
        data = new double[][] { { 7.14488, 7.70285, -0.00999, -1.02543 },
                { 3.54792, -5.17457, 9.14450, 7.88733 }, { 2.22814, 1.57837, 5.37195, -5.24397 },
                { 7.20418, 9.26362, 0.97310, -9.39937 }, { 7.14401, 3.49443, 1.69478, -7.08771 },
                { 7.39136, -1.16028, 1.78989, -8.92695 } };
        final RealMatrix matrix = new SimpleMatrix(data);

        // Matrix by which to multiply by
        data = new double[][] { { 9.08247, 7.89094, 9.49794, -2.12282 },
                { 5.75970, 0.64482, -1.79979, 7.70594 }, { 1.36289, 9.76961, 4.13272, 9.36172 },
                { 1.37165, 8.60949, -7.02000, -8.00656 } };
        final RealMatrix realMatrix = new Array2DRowRealMatrix(data);
        final RealMatrix realMatrixT = realMatrix.transpose();

        // Scaling factor
        final double scalingFactor = 6.38947;

        // Test the multiplication by the provided matrix
        data = new double[][] { { 107.8391170680, 52.4207433896, 61.1553618729, 52.3070490054 },
                { 25.7015099329, 181.9038450241, 25.4336720651, -24.9486538250 },
                { 29.4564378798, 25.9338327592, 77.3354400933, 99.7097365602 },
                { 107.2210032371, -8.5958549527, 121.7574259814, 140.4582321144 },
                { 77.6000662684, 14.1620038899, 118.3239135713, 84.3766518052 },
                { 50.6437129578, -1.7933529838, 142.3552573404, 63.5990349044 } };
        expected = new Array2DRowRealMatrix(data);

        result = matrix.multiply(realMatrix);
        CheckUtils.checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());
        Assert.assertNotSame(matrix, result);

        result = matrix.multiply(realMatrix, false);
        CheckUtils.checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());
        Assert.assertNotSame(matrix, result);

        result = matrix.multiply(realMatrix, false, 1.0);
        CheckUtils.checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());
        Assert.assertNotSame(matrix, result);

        // Test the multiplication by the transpose of the provided matrix
        result = matrix.multiply(realMatrixT, true);
        CheckUtils.checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());
        Assert.assertNotSame(matrix, result);

        result = MatrixUtils.multiplyByTranspose(matrix, realMatrixT);
        CheckUtils.checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());
        Assert.assertNotSame(matrix, result);

        result = matrix.multiply(realMatrixT, true, 1.0);
        CheckUtils.checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());
        Assert.assertNotSame(matrix, result);

        result = MatrixUtils.multiplyByTranspose(1.0, matrix, realMatrixT);
        CheckUtils.checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());
        Assert.assertNotSame(matrix, result);

        // Test the multiplication by the provided matrix and by a scaling factor
        data = new double[][] {
                { 689.03480333247410, 334.94076726554750, 390.75035002603830, 334.21432040853320 },
                { 164.21902667096663, 1162.26916066613630, 162.50768464979453, -159.40867515522277 },
                { 188.21102613984570, 165.70344639992570, 494.13247441293754, 637.09237045930120 },
                { 685.08538355335330, -54.92295734462804, 777.96542058537580, 897.45366034799540 },
                { 495.82329541995375, 90.48769899439935, 756.02709604641420, 539.12208540977120 },
                { 323.58648463247450, -11.45857508940053, 909.57464611876550, 406.36412555061680 } };
        expected = new Array2DRowRealMatrix(data);

        result = matrix.multiply(realMatrix, false, scalingFactor);
        CheckUtils.checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());
        Assert.assertNotSame(matrix, result);

        // Test the multiplication by the transpose of the provided matrix and by a scaling factor
        result = matrix.multiply(realMatrixT, true, scalingFactor);
        CheckUtils.checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());
        Assert.assertNotSame(matrix, result);
    }

    /**
     * Tests the methods that postmultiply the matrix by another matrix, using a diagonal matrix.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain AbstractRealMatrix#multiply(DiagonalMatrix, double)}<br>
     * {@linkplain MatrixUtils#multiplyByTranspose(RealMatrix, RealMatrix)}<br>
     * {@linkplain MatrixUtils#multiplyByTranspose(double, RealMatrix, RealMatrix)}<br>
     * </p>
     */
    @Test
    public void testMultiplyByDiagonalMatrix() {
        double[][] data;
        RealMatrix result;
        RealMatrix expected;

        // Tested matrix
        data = new double[][] { { 0.92807, -0.19492, -4.19385, 7.35377 },
                { -0.56499, 1.88090, 2.65588, -6.09890 },
                { 4.91342, -9.82480, -8.78269, -7.56939 }, { -3.43288, 0.83248, 0.26660, 1.43728 },
                { -7.95902, -6.67414, -3.36423, 7.52587 }, { 3.21570, -0.50039, 8.71022, -6.33408 } };

        final SimpleMatrix matrix = new SimpleMatrix(data);

        // Matrix by which to multiply by
        final double[] diagonalElements = { -4.73296, -8.73128, 1.52024, 2.39431 };
        final DiagonalMatrix diagonalMatrix = new DiagonalMatrix(diagonalElements);

        // Scaling factor
        final double scalingFactor = 4.87993;

        // Test the multiplication by the provided matrix
        data = new double[][] { { -4.3925181872, 1.7019010976, -6.3756585240, 17.6072050487 },
                { 2.6740750704, -16.4226645520, 4.0375750112, -14.6026572590 },
                { -23.2550203232, 85.7830797440, -13.3517966456, -18.1234661709 },
                { 16.2476837248, -7.2686159744, 0.4052959840, 3.4412938768 },
                { 37.6697232992, 58.2737850992, -5.1144370152, 18.0192657997 },
                { -15.2197794720, 4.3690451992, 13.2416248528, -15.1657510848 } };
        expected = new Array2DRowRealMatrix(data);

        result = matrix.multiply(diagonalMatrix);
        CheckUtils.checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());
        Assert.assertNotSame(matrix, result);

        result = matrix.multiply(diagonalMatrix, false, 1.0);
        CheckUtils.checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());
        Assert.assertNotSame(matrix, result);

        result = matrix.multiply(diagonalMatrix, true, 1.0);
        CheckUtils.checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());
        Assert.assertNotSame(matrix, result);

        result = MatrixUtils.multiplyByTranspose(matrix, diagonalMatrix);
        CheckUtils.checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());
        Assert.assertNotSame(matrix, result);

        result = matrix.multiply(diagonalMatrix, 1.0);
        CheckUtils.checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());
        Assert.assertNotSame(matrix, result);

        result = MatrixUtils.multiplyByTranspose(1.0, matrix, diagonalMatrix);
        CheckUtils.checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());
        Assert.assertNotSame(matrix, result);

        // Test the multiplication by the provided matrix and by a scaling factor
        data = new double[][] {
                { -21.43518127726290, 8.30515822321117, -31.11276730102332, 85.92192813330259 },
                { 13.04929915829707, -80.14145342724136, 19.70308342440521, -71.25994523791188 },
                { -113.48287132579340, 418.61542433513785, -65.15583300476281, -88.44124627136003 },
                { 79.28755923916327, -35.47033715195379, 1.97781603120112, 16.79327322821262 },
                { 183.82561281946505, 284.37199211913907, -24.95809462358493, 87.93275575393002 },
                { -74.27145843879696, 21.32063473893206, 64.61820236792430, -74.00780369124806 } };
        expected = new Array2DRowRealMatrix(data);

        result = matrix.multiply(diagonalMatrix, false, scalingFactor);
        CheckUtils.checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());
        Assert.assertNotSame(matrix, result);

        result = matrix.multiply(diagonalMatrix, true, scalingFactor);
        CheckUtils.checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());
        Assert.assertNotSame(matrix, result);
    }

    /**
     * Tests the methods that postmultiply the matrix by another matrix, using a rectangular matrix.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain AbstractRealMatrix#multiply(RealMatrix)}<br>
     * {@linkplain AbstractRealMatrix#multiply(RealMatrix, boolean)}<br>
     * {@linkplain AbstractRealMatrix#multiply(RealMatrix, boolean, double)}<br>
     * {@linkplain MatrixUtils#multiplyByTranspose(RealMatrix, RealMatrix)}<br>
     * {@linkplain MatrixUtils#multiplyByTranspose(double, RealMatrix, RealMatrix)}<br>
     * </p>
     */
    @Test
    public void testMultiplyByRectangularMatrix() {
        double[][] data;
        RealMatrix result;
        RealMatrix expected;

        // Tested matrix
        data = new double[][] { { -5.90147, 4.10100, -1.70040, -3.84128 },
                { 9.22401, 0.98870, 4.73801, 9.41422 }, { -4.63679, -6.77577, -0.82087, -0.38029 },
                { -8.68184, -8.68777, 0.14034, -5.86787 }, { -4.79780, 2.64755, 0.46852, 4.97306 },
                { -7.51489, 1.17785, -1.69122, -2.48513 } };
        final RealMatrix matrix = new SimpleMatrix(data);

        // Matrix by which to multiply by
        data = new double[][] { { 2.95576, 2.76666, -6.36547, -2.82603, 4.12100, 3.86735 },
                { 2.41881, -8.84257, -5.15398, 8.90532, -2.32767, 0.91425 },
                { -0.16214, 1.48595, 2.59368, 8.77136, -9.17646, -4.71997 },
                { -9.41256, 0.80643, -5.29695, -8.21579, -1.71191, 4.96974 } };
        final RealMatrix realMatrix = new Array2DRowRealMatrix(data);
        final RealMatrix realMatrixT = realMatrix.transpose();

        // Scaling factor
        final double scalingFactor = 2.86741;

        // Test the multiplication by the provided matrix
        data = new double[][] {
                { 28.908192175599996, -58.2151733706, 32.3659328849, 69.8427778513,
                        -11.686154311200001, -30.138036633700004 },
                { -59.72469429999998, 31.4094059417, -101.3886698129, -53.049102236500005,
                        -23.883678723800003, 60.999354691600004 },
                { -26.381940339899998, 45.5603500663, 64.3228337198, -51.312455956799994,
                        4.847247939999995, -22.142298179700003 },
                { 8.533423367499992, 48.27902673340001, 131.4863959371, -3.392071311500011,
                        -6.798160108800003, -71.3426964901 },
                { -54.6624163789, -31.978405481699998, -8.232026996399998, 0.387947669799992,
                        -38.747142692299995, 6.369165617499998 },
                { 4.302433735699996, -35.72351838680001, 50.5423173692, 37.309442492200006,
                        -13.936776220000006, -32.3538027818 } };
        expected = new Array2DRowRealMatrix(data);

        result = matrix.multiply(realMatrix);
        CheckUtils.checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());
        Assert.assertNotSame(matrix, result);

        result = matrix.multiply(realMatrix, false);
        CheckUtils.checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());
        Assert.assertNotSame(matrix, result);

        result = matrix.multiply(realMatrix, false, 1.0);
        CheckUtils.checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());
        Assert.assertNotSame(matrix, result);

        // Test the multiplication by the transpose of the provided matrix
        result = matrix.multiply(realMatrixT, true);
        CheckUtils.checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());
        Assert.assertNotSame(matrix, result);

        result = MatrixUtils.multiplyByTranspose(matrix, realMatrixT);
        CheckUtils.checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());
        Assert.assertNotSame(matrix, result);

        result = matrix.multiply(realMatrixT, true, 1.0);
        CheckUtils.checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());
        Assert.assertNotSame(matrix, result);

        result = MatrixUtils.multiplyByTranspose(1.0, matrix, realMatrixT);
        CheckUtils.checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());
        Assert.assertNotSame(matrix, result);

        // Test the multiplication by the provided matrix and by a scaling factor
        data = new double[][] {
                { 82.89163932623718, -166.92677027459214, 92.80639961349111, 200.26787963859613,
                        -33.508995733477995, -86.41810762383773 },
                { -171.25518568276294, 90.06364469129, -290.7228857082076, -152.11352624396247,
                        -68.48429920941136, 174.91015963624076 },
                { -75.64783955003266, 130.6402033836093, 184.43993663649172, -147.13384933508786,
                        13.899047215635385, -63.491047223453585 },
                { 24.468823498203154, 138.43576404561853, 377.02540657399993, -9.726459199308247,
                        -19.493112277574216, -204.56876134267762 },
                { -156.73955934902165, -91.6951996622814, -23.60459652974732, 1.112405027861195,
                        -111.10394442732793, 18.26300918327567 },
                { 12.336841518083524, -102.43397385749421, 144.92554624761777, 106.98146849655922,
                        -39.96245150099022, -92.77161763456114 } };
        expected = new Array2DRowRealMatrix(data);

        result = matrix.multiply(realMatrix, false, scalingFactor);
        CheckUtils.checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());
        Assert.assertNotSame(matrix, result);

        // Test the multiplication by the transpose of the provided matrix and by a scaling factor
        result = matrix.multiply(realMatrixT, true, scalingFactor);
        CheckUtils.checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());
        Assert.assertNotSame(matrix, result);
    }

    /**
     * Tests the methods that postmultiply the matrix by another matrix, using a matrix with
     * incompatible dimensions.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain AbstractRealMatrix#multiply(RealMatrix)}<br>
     * {@linkplain AbstractRealMatrix#multiply(RealMatrix, boolean)}<br>
     * {@linkplain AbstractRealMatrix#multiply(RealMatrix, boolean, double)}<br>
     * {@linkplain AbstractRealMatrix#multiply(DiagonalMatrix, double)}<br>
     * </p>
     */
    @Test
    public void testMultiplyByIncompatibleMatrix() {
        final RealMatrix matrix = new SimpleMatrix(3, 4);
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
     * Tested methods:<br>
     * {@linkplain AbstractRealMatrix#preMultiply(RealMatrix)}<br>
     * </p>
     */
    @Test
    public void testPreMultiplyBySquareMatrix() {
        double[][] data;

        // Tested matrix
        data = new double[][] { { -5.16304, 9.27796, -6.59509, -0.23149, 3.00790, 5.17496 },
                { -9.74274, -6.96808, 0.88857, 8.65139, -7.54912, 3.40294 },
                { -2.29318, 5.09194, -3.14066, -6.12028, 3.57690, -2.72164 },
                { 9.88873, -9.75657, -1.08264, 3.39490, 6.24546, 8.52395 } };
        final RealMatrix matrix = new SimpleMatrix(data);

        // Matrix by which to multiply by
        data = new double[][] { { 1.65745, -0.75049, -0.90747, 7.39779 },
                { 5.78099, 3.48325, -7.75027, 4.38043 },
                { -9.57517, -0.80936, -0.98423, -5.04529 },
                { -1.62493, -5.25101, 0.39331, -0.36059 } };
        final RealMatrix realMatrix = new Array2DRowRealMatrix(data);

        // Test the multiplication by the provided matrix
        data = new double[][] {
                { 73.99008825589999, -56.1906096109, -16.7569834552, 23.792262981,
                        53.60765501419999, 71.55156373269999 },
                { -2.694228002100004, -52.83765242849999, -15.432503643600004, 91.10165712499999,
                        -9.270972834200009, 100.2016939067 },
                { 9.687875732899997, -38.9852740753, 70.98329945750001, -15.890024743699996,
                        -57.72184725019999, -92.6324052199 },
                { 55.081095978099995, 27.034254715699994, 5.205805810999999, -48.68371467599999,
                        33.9076577818, -30.4219190811 } };
        final RealMatrix expected = new Array2DRowRealMatrix(data);

        final RealMatrix result = matrix.preMultiply(realMatrix);
        CheckUtils.checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertEquals(Array2DRowRealMatrix.class, result.getClass());
        Assert.assertNotSame(matrix, result);
    }

    /**
     * Tests the method that premultiplies the matrix by another matrix, using a rectangular matrix.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain AbstractRealMatrix#preMultiply(RealMatrix)}<br>
     * </p>
     */
    @Test
    public void testPreMultiplyByRectangularMatrix() {
        double[][] data;

        // Tested matrix
        data = new double[][] { { -1.31916, -0.98116, -0.26670, -9.39782 },
                { 5.28704, -6.59822, 4.39523, 7.39843 }, { 3.26254, -3.94198, 7.98162, -6.57357 },
                { -4.60632, 3.44102, -6.75784, -2.70299 },
                { -3.15696, -8.59134, 6.21856, 8.54047 },
                { -1.93386, -7.92352, -7.17905, -5.59813 } };
        final RealMatrix matrix = new SimpleMatrix(data);

        // Matrix by which to multiply by
        data = new double[][] { { -1.34401, -2.89439, 6.93420, -1.25001, -6.33784, -6.46969 },
                { 7.99221, 9.52753, 0.29842, -6.47496, -6.55516, -0.84333 },
                { -9.84399, -2.02250, 4.12900, 8.87905, -6.79125, -6.81208 },
                { -3.85412, -8.73306, 7.48926, -0.00319, -2.70916, 1.44151 } };
        final RealMatrix realMatrix = new Array2DRowRealMatrix(data);

        // Test the multiplication by the provided matrix
        data = new double[][] {
                { 47.371041526999996, 94.49398012560002, 58.4644443738, -68.8968290187 },
                { 92.954033469, -31.16345785500001, 51.17343446860001, -40.343596185599985 },
                { 9.477355141199999, 149.60157228600002, -26.638583489999995, 6.540910060200016 },
                { -10.874100897000002, 43.724150902999995, -4.753674964099993, -108.82041947900001 } };
        final RealMatrix expected = new Array2DRowRealMatrix(data);

        final RealMatrix result = matrix.preMultiply(realMatrix);
        CheckUtils.checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertEquals(Array2DRowRealMatrix.class, result.getClass());
        Assert.assertNotSame(matrix, result);
    }

    /**
     * Tests the method that premultiplies the matrix by another matrix, using a matrix with
     * incompatible dimensions.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain AbstractRealMatrix#preMultiply(RealMatrix)}<br>
     * </p>
     */
    @Test
    public void testPreMultiplyByIncompatibleMatrix() {
        final RealMatrix matrix = new SimpleMatrix(3, 4);
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
     * {@linkplain AbstractRealMatrix#operate(double[])}<br>
     * {@linkplain AbstractRealMatrix#operate(RealVector)}<br>
     * </p>
     */
    @Test
    public void testOperateVector() {
        // Tested matrix
        final double[][] data = { { 5.14372, 7.90499, -2.00005, 1.01988 },
                { -9.45348, 0.20679, -5.78836, -8.90768 }, { 7.91234, 8.23059, 2.54383, 2.21557 },
                { 9.49012, -3.25264, -6.47138, -4.08389 },
                { 5.94144, -2.02162, 8.04359, -3.41976 }, { -1.54535, 6.56718, -0.41893, -8.11804 } };
        final RealMatrix matrix = new SimpleMatrix(data);

        // Vector to multiply the tested matrix by
        final double[] vectorData = { -1.86189, 6.28353, -2.86734, 1.94436 };
        final RealVector vector1 = new ArrayRealVector(vectorData);
        final RealVector vector2 = new SimpleVector(vectorData);

        // Test the methods
        final double[] expectedData = { 47.8120382277, 18.1781705235, 33.9990926331,
                -27.4925261772, -53.4781695444, 29.5591667487 };
        final double[] resultData = matrix.operate(vectorData);
        CheckUtils.checkEquality(expectedData, resultData, ABSTOL, RELTOL);

        final RealVector expectedVector1 = new ArrayRealVector(expectedData);
        final RealVector resultVector1 = matrix.operate(vector1);
        CheckUtils.checkEquality(expectedVector1, resultVector1, ABSTOL, RELTOL);
        Assert.assertEquals(ArrayRealVector.class, resultVector1.getClass());

        final RealVector expectedVector2 = new ArrayRealVector(expectedData);
        final RealVector resultVector2 = matrix.operate(vector2);
        CheckUtils.checkEquality(expectedVector2, resultVector2, ABSTOL, RELTOL);
        Assert.assertEquals(ArrayRealVector.class, resultVector2.getClass());
    }

    /**
     * Tests the methods that postmultiply the matrix by a vector, using a vector with an
     * incompatible dimension.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain AbstractRealMatrix#operate(double[])}<br>
     * {@linkplain AbstractRealMatrix#operate(RealVector)}<br>
     * </p>
     */
    @Test
    public void testOperateIncompatibleVector() {
        final RealMatrix matrix = new SimpleMatrix(4, 3);
        CheckUtils.checkOperateIncompatibleVector(matrix);

        // Test the method using a vector which is not an ArrayRealVector
        final String format = "%d != %d";
        final int nc = matrix.getColumnDimension();

        try {
            matrix.operate(new SimpleVector(nc - 1));
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(format, nc - 1, nc);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.operate(new SimpleVector(nc + 1));
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(format, nc + 1, nc);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the methods that premultiply the matrix by a vector.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain AbstractRealMatrix#preMultiply(double[])}<br>
     * {@linkplain AbstractRealMatrix#preMultiply(RealVector)}<br>
     * </p>
     */
    @Test
    public void testPreMultiplyByVector() {
        // Tested matrix
        final double[][] data = { { 9.35199, 2.40067, 9.00865, -0.06058 },
                { 1.29484, 7.47181, -8.50892, 0.24949 }, { 1.08932, -0.83274, -0.15979, -4.64643 },
                { 2.05041, -8.44097, -5.06540, -7.85520 },
                { -9.71245, 9.67063, -3.33485, -2.99358 },
                { -1.96777, -6.48039, 3.04405, -9.95078 } };
        final RealMatrix matrix = new SimpleMatrix(data);

        // Vectors to multiply the tested matrix by
        final double[] vectorData = { -1.86189, 6.28353, -2.86734, 1.94436, -5.44195, 8.84281 };
        final RealVector vector1 = new ArrayRealVector(vectorData);
        final RealVector vector2 = new SimpleVector(vectorData);

        // Test the methods
        final double[] expectedData = { 27.041124746700007, -81.476919319, -34.563915583500005,
                -71.97191509070001 };
        final double[] resultData = matrix.preMultiply(vectorData);
        CheckUtils.checkEquality(expectedData, resultData, ABSTOL, RELTOL);

        final RealVector expectedVector1 = new ArrayRealVector(expectedData);
        final RealVector resultVector1 = matrix.preMultiply(vector1);
        CheckUtils.checkEquality(expectedVector1, resultVector1, ABSTOL, RELTOL);
        Assert.assertEquals(ArrayRealVector.class, resultVector1.getClass());

        final RealVector expectedVector2 = new ArrayRealVector(expectedData);
        final RealVector resultVector2 = matrix.preMultiply(vector2);
        CheckUtils.checkEquality(expectedVector2, resultVector2, ABSTOL, RELTOL);
        Assert.assertEquals(ArrayRealVector.class, resultVector2.getClass());
    }

    /**
     * Tests the methods that premultiply the matrix by a vector, using a vector with an
     * incompatible dimension.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain AbstractRealMatrix#preMultiply(double[])}<br>
     * {@linkplain AbstractRealMatrix#preMultiply(RealVector)}<br>
     * </p>
     */
    @Test
    public void testPreMultiplyByIncompatibleVector() {
        final RealMatrix matrix = new SimpleMatrix(4, 3);
        CheckUtils.checkPreMultiplyIncompatibleVector(matrix);

        // Test the method using a vector which is not an ArrayRealVector
        final String format = "%d != %d";
        final int nr = matrix.getRowDimension();

        try {
            matrix.preMultiply(new SimpleVector(nr - 1));
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(format, nr - 1, nr);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.preMultiply(new SimpleVector(nr + 1));
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(format, nr + 1, nr);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the method that returns a row of the matrix.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain AbstractRealMatrix#getRow(int)}<br>
     * {@linkplain AbstractRealMatrix#getRowVector(int)}<br>
     * {@linkplain AbstractRealMatrix#getRowMatrix(int)}<br>
     * </p>
     */
    @Test
    public void testGetRow() {
        int rowIndex;
        double[] rowData;
        RealVector rowVector;
        RealMatrix rowMatrix;
        double[] expected;

        // Tested matrix
        final double[][] data = { { 2.94076, -0.78998, 9.96936, 2.37319 },
                { -5.22542, 2.96368, 8.46227, 8.67292 }, { 8.09547, 0.32004, 3.25670, -1.73102 },
                { 7.24723, 8.10069, -5.27963, -4.42086 }, { 9.22963, -7.52253, 6.25804, 3.65865 },
                { 6.82303, 3.60468, -7.56471, 5.07371 } };
        final RealMatrix matrix = new SimpleMatrix(data);

        // First row
        rowIndex = 0;
        rowData = matrix.getRow(rowIndex);
        rowVector = matrix.getRowVector(rowIndex);
        rowMatrix = matrix.getRowMatrix(rowIndex);
        expected = new double[] { 2.94076, -0.78998, 9.96936, 2.37319 };
        CheckUtils.checkEquality(expected, rowData, 0., 0.);
        CheckUtils.checkEquality(new ArrayRealVector(expected), rowVector, 0., 0.);
        CheckUtils.checkEquality(MatrixUtils.createRowRealMatrix(expected), rowMatrix, 0., 0.);

        // Second row
        rowIndex = 1;
        rowData = matrix.getRow(rowIndex);
        rowVector = matrix.getRowVector(rowIndex);
        rowMatrix = matrix.getRowMatrix(rowIndex);
        expected = new double[] { -5.22542, 2.96368, 8.46227, 8.67292 };
        CheckUtils.checkEquality(expected, rowData, 0., 0.);
        CheckUtils.checkEquality(new ArrayRealVector(expected), rowVector, 0., 0.);
        CheckUtils.checkEquality(MatrixUtils.createRowRealMatrix(expected), rowMatrix, 0., 0.);

        // Third row
        rowIndex = 2;
        rowData = matrix.getRow(rowIndex);
        rowVector = matrix.getRowVector(rowIndex);
        rowMatrix = matrix.getRowMatrix(rowIndex);
        expected = new double[] { 8.09547, 0.32004, 3.25670, -1.73102 };
        CheckUtils.checkEquality(expected, rowData, 0., 0.);
        CheckUtils.checkEquality(new ArrayRealVector(expected), rowVector, 0., 0.);
        CheckUtils.checkEquality(MatrixUtils.createRowRealMatrix(expected), rowMatrix, 0., 0.);

        // Fourth row
        rowIndex = 3;
        rowData = matrix.getRow(rowIndex);
        rowVector = matrix.getRowVector(rowIndex);
        rowMatrix = matrix.getRowMatrix(rowIndex);
        expected = new double[] { 7.24723, 8.10069, -5.27963, -4.42086 };
        CheckUtils.checkEquality(expected, rowData, 0., 0.);
        CheckUtils.checkEquality(new ArrayRealVector(expected), rowVector, 0., 0.);
        CheckUtils.checkEquality(MatrixUtils.createRowRealMatrix(expected), rowMatrix, 0., 0.);

        // Fifth row
        rowIndex = 4;
        rowData = matrix.getRow(rowIndex);
        rowVector = matrix.getRowVector(rowIndex);
        rowMatrix = matrix.getRowMatrix(rowIndex);
        expected = new double[] { 9.22963, -7.52253, 6.25804, 3.65865 };
        CheckUtils.checkEquality(expected, rowData, 0., 0.);
        CheckUtils.checkEquality(new ArrayRealVector(expected), rowVector, 0., 0.);
        CheckUtils.checkEquality(MatrixUtils.createRowRealMatrix(expected), rowMatrix, 0., 0.);

        // Sixth row
        rowIndex = 5;
        rowData = matrix.getRow(rowIndex);
        rowVector = matrix.getRowVector(rowIndex);
        rowMatrix = matrix.getRowMatrix(rowIndex);
        expected = new double[] { 6.82303, 3.60468, -7.56471, 5.07371 };
        CheckUtils.checkEquality(expected, rowData, 0., 0.);
        CheckUtils.checkEquality(new ArrayRealVector(expected), rowVector, 0., 0.);
        CheckUtils.checkEquality(MatrixUtils.createRowRealMatrix(expected), rowMatrix, 0., 0.);
    }

    /**
     * Tests the methods that return a row of the matrix, using indices outside the valid index
     * range.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain AbstractRealMatrix#getRow(int)}<br>
     * {@linkplain AbstractRealMatrix#getRowVector(int)}<br>
     * {@linkplain AbstractRealMatrix#getRowMatrix(int)}<br>
     * </p>
     */
    @Test
    public void testGetOutOfRangeRow() {
        final RealMatrix matrix = new SimpleMatrix(5, 3);
        CheckUtils.checkGetOutOfRangeRow(matrix);
    }

    /**
     * Tests methods that set a row of the matrix.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain AbstractRealMatrix#setRow(int, double[])}<br>
     * {@linkplain AbstractRealMatrix#setRowVector(int, RealVector)}<br>
     * {@linkplain AbstractRealMatrix#setRowMatrix(int, RealMatrix)}<br>
     * </p>
     */
    @Test
    public void testSetRow() {
        RealMatrix matrix;

        // Expected matrix after setting the rows
        final double[][] data = { { -0.29194, 6.02603, -8.98172, -7.19432 },
                { -7.20525, -9.08779, -4.62751, -9.50621 },
                { 4.29811, -1.15018, 0.85215, -4.70215 }, { 8.73871, 5.87935, 3.42769, 1.52484 },
                { 8.43256, -9.52783, 0.62482, -1.86380 }, { -3.59643, 8.72403, -7.35601, 5.98880 } };
        final RealMatrix expected = new Array2DRowRealMatrix(data);

        // Rows to be set
        final double[] row0 = { -0.29194, 6.02603, -8.98172, -7.19432 };
        final double[] row1 = { -7.20525, -9.08779, -4.62751, -9.50621 };
        final double[] row2 = { 4.29811, -1.15018, 0.85215, -4.70215 };
        final double[] row3 = { 8.73871, 5.87935, 3.42769, 1.52484 };
        final double[] row4 = { 8.43256, -9.52783, 0.62482, -1.86380 };
        final double[] row5 = { -3.59643, 8.72403, -7.35601, 5.98880 };

        // Check the method that takes an array as argument
        matrix = new SimpleMatrix(6, 4);
        matrix.setRow(0, row0);
        matrix.setRow(1, row1);
        matrix.setRow(2, row2);
        matrix.setRow(3, row3);
        matrix.setRow(4, row4);
        matrix.setRow(5, row5);
        CheckUtils.checkEquality(expected, matrix, ABSTOL, RELTOL);

        // Check the method that takes a vector as argument
        matrix = new SimpleMatrix(6, 4);
        matrix.setRowVector(0, new ArrayRealVector(row0));
        matrix.setRowVector(1, new ArrayRealVector(row1));
        matrix.setRowVector(2, new ArrayRealVector(row2));
        matrix.setRowVector(3, new ArrayRealVector(row3));
        matrix.setRowVector(4, new ArrayRealVector(row4));
        matrix.setRowVector(5, new ArrayRealVector(row5));
        CheckUtils.checkEquality(expected, matrix, ABSTOL, RELTOL);

        // Check the method that takes a matrix as argument
        matrix = new SimpleMatrix(6, 4);
        matrix.setRowMatrix(0, MatrixUtils.createRowRealMatrix(row0));
        matrix.setRowMatrix(1, MatrixUtils.createRowRealMatrix(row1));
        matrix.setRowMatrix(2, MatrixUtils.createRowRealMatrix(row2));
        matrix.setRowMatrix(3, MatrixUtils.createRowRealMatrix(row3));
        matrix.setRowMatrix(4, MatrixUtils.createRowRealMatrix(row4));
        matrix.setRowMatrix(5, MatrixUtils.createRowRealMatrix(row5));
        CheckUtils.checkEquality(expected, matrix, ABSTOL, RELTOL);
    }

    /**
     * Tests the methods that set a row of the matrix, using indices outside the valid index range.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain AbstractRealMatrix#setRow(int, double[])}<br>
     * {@linkplain AbstractRealMatrix#setRowVector(int, RealVector)}<br>
     * {@linkplain AbstractRealMatrix#setRowMatrix(int, RealMatrix)}<br>
     * </p>
     */
    @Test
    public void testSetOutOfRangeRow() {
        final RealMatrix matrix = new SimpleMatrix(5, 3);
        CheckUtils.checkSetOutOfRangeRow(matrix);
    }

    /**
     * Tests the methods that set a row of the matrix, using incompatible arrays, vectors and row
     * matrices.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain AbstractRealMatrix#setRow(int, double[])}<br>
     * {@linkplain AbstractRealMatrix#setRowVector(int, RealVector)}<br>
     * {@linkplain AbstractRealMatrix#setRowMatrix(int, RealMatrix)}<br>
     * </p>
     */
    @Test
    public void testSetIncompatibleRow() {
        final RealMatrix matrix = new SimpleMatrix(5, 3);
        CheckUtils.checkSetIncompatibleRow(matrix);
    }

    /**
     * Tests the methods that return a column of the matrix.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain AbstractRealMatrix#getColumn(int)}<br>
     * {@linkplain AbstractRealMatrix#getColumnVector(int)}<br>
     * {@linkplain AbstractRealMatrix#getColumnMatrix(int)}<br>
     * </p>
     */
    @Test
    public void testGetColumn() {
        int columnIndex;
        double[] columnData;
        RealVector columnVector;
        RealMatrix columnMatrix;
        double[] expected;

        // Tested matrix
        final double[][] data = { { 2.94076, -0.78998, 9.96936, 2.37319 },
                { -5.22542, 2.96368, 8.46227, 8.67292 }, { 8.09547, 0.32004, 3.25670, -1.73102 },
                { 7.24723, 8.10069, -5.27963, -4.42086 }, { 9.22963, -7.52253, 6.25804, 3.65865 },
                { 6.82303, 3.60468, -7.56471, 5.07371 } };
        final RealMatrix matrix = new SimpleMatrix(data);

        // First column
        columnIndex = 0;
        columnData = matrix.getColumn(columnIndex);
        columnVector = matrix.getColumnVector(columnIndex);
        columnMatrix = matrix.getColumnMatrix(columnIndex);
        expected = new double[] { 2.94076, -5.22542, 8.09547, 7.24723, 9.22963, 6.82303 };
        CheckUtils.checkEquality(expected, columnData, 0., 0.);
        CheckUtils.checkEquality(new ArrayRealVector(expected), columnVector, 0., 0.);
        CheckUtils
                .checkEquality(MatrixUtils.createColumnRealMatrix(expected), columnMatrix, 0., 0.);

        // Second column
        columnIndex = 1;
        columnData = matrix.getColumn(columnIndex);
        columnVector = matrix.getColumnVector(columnIndex);
        columnMatrix = matrix.getColumnMatrix(columnIndex);
        expected = new double[] { -0.78998, 2.96368, 0.32004, 8.10069, -7.52253, 3.60468 };
        CheckUtils.checkEquality(expected, columnData, 0., 0.);
        CheckUtils.checkEquality(new ArrayRealVector(expected), columnVector, 0., 0.);
        CheckUtils
                .checkEquality(MatrixUtils.createColumnRealMatrix(expected), columnMatrix, 0., 0.);

        // Third column
        columnIndex = 2;
        columnData = matrix.getColumn(columnIndex);
        columnVector = matrix.getColumnVector(columnIndex);
        columnMatrix = matrix.getColumnMatrix(columnIndex);
        expected = new double[] { 9.96936, 8.46227, 3.2567, -5.27963, 6.25804, -7.56471 };
        CheckUtils.checkEquality(expected, columnData, 0., 0.);
        CheckUtils.checkEquality(new ArrayRealVector(expected), columnVector, 0., 0.);
        CheckUtils
                .checkEquality(MatrixUtils.createColumnRealMatrix(expected), columnMatrix, 0., 0.);

        // Fourth column
        columnIndex = 3;
        columnData = matrix.getColumn(columnIndex);
        columnVector = matrix.getColumnVector(columnIndex);
        columnMatrix = matrix.getColumnMatrix(columnIndex);
        expected = new double[] { 2.37319, 8.67292, -1.73102, -4.42086, 3.65865, 5.07371 };
        CheckUtils.checkEquality(expected, columnData, 0., 0.);
        CheckUtils.checkEquality(new ArrayRealVector(expected), columnVector, 0., 0.);
        CheckUtils
                .checkEquality(MatrixUtils.createColumnRealMatrix(expected), columnMatrix, 0., 0.);
    }

    /**
     * Tests the methods that return a column of the matrix, using indices outside the valid index
     * range.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain AbstractRealMatrix#getColumn(int)}<br>
     * {@linkplain AbstractRealMatrix#getColumnVector(int)}<br>
     * {@linkplain AbstractRealMatrix#getColumnMatrix(int)}<br>
     * </p>
     */
    @Test
    public void testGetOutOfRangeColumn() {
        final RealMatrix matrix = new SimpleMatrix(5, 3);
        CheckUtils.checkGetOutOfRangeColumn(matrix);
    }

    /**
     * Tests methods that set a column of the matrix.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain AbstractRealMatrix#setColumn(int, double[])}<br>
     * {@linkplain AbstractRealMatrix#setColumnVector(int, RealVector)}<br>
     * {@linkplain AbstractRealMatrix#setColumnMatrix(int, RealMatrix)}<br>
     * </p>
     */
    @Test
    public void testSetColumn() {
        RealMatrix matrix;

        // Expected matrix after setting the columns
        final double[][] data = { { 8.86924, -9.21356, -9.38527, 2.68315, 3.87355, -7.82382 },
                { 5.02376, -9.30702, 9.56157, -4.96050, -0.16282, 8.45550 },
                { 2.98990, 4.61418, 3.64978, -6.88957, 2.34944, 9.30522 },
                { 4.98585, 3.93419, -4.07147, 6.72977, -8.38600, -3.71675 } };
        final RealMatrix expected = new Array2DRowRealMatrix(data);

        // Columns to be set
        final double[] column0 = { 8.86924, 5.02376, 2.9899, 4.98585 };
        final double[] column1 = { -9.21356, -9.30702, 4.61418, 3.93419 };
        final double[] column2 = { -9.38527, 9.56157, 3.64978, -4.07147 };
        final double[] column3 = { 2.68315, -4.9605, -6.88957, 6.72977 };
        final double[] column4 = { 3.87355, -0.16282, 2.34944, -8.386 };
        final double[] column5 = { -7.82382, 8.4555, 9.30522, -3.71675 };

        // Check the method that takes an array as argument
        matrix = new SimpleMatrix(4, 6);
        matrix.setColumn(0, column0);
        matrix.setColumn(1, column1);
        matrix.setColumn(2, column2);
        matrix.setColumn(3, column3);
        matrix.setColumn(4, column4);
        matrix.setColumn(5, column5);
        CheckUtils.checkEquality(expected, matrix, ABSTOL, RELTOL);

        // Check the method that takes a vector as argument
        matrix = new SimpleMatrix(4, 6);
        matrix.setColumnVector(0, new ArrayRealVector(column0));
        matrix.setColumnVector(1, new ArrayRealVector(column1));
        matrix.setColumnVector(2, new ArrayRealVector(column2));
        matrix.setColumnVector(3, new ArrayRealVector(column3));
        matrix.setColumnVector(4, new ArrayRealVector(column4));
        matrix.setColumnVector(5, new ArrayRealVector(column5));
        CheckUtils.checkEquality(expected, matrix, ABSTOL, RELTOL);

        // Check the method that takes a matrix as argument
        matrix = new SimpleMatrix(4, 6);
        matrix.setColumnMatrix(0, MatrixUtils.createColumnRealMatrix(column0));
        matrix.setColumnMatrix(1, MatrixUtils.createColumnRealMatrix(column1));
        matrix.setColumnMatrix(2, MatrixUtils.createColumnRealMatrix(column2));
        matrix.setColumnMatrix(3, MatrixUtils.createColumnRealMatrix(column3));
        matrix.setColumnMatrix(4, MatrixUtils.createColumnRealMatrix(column4));
        matrix.setColumnMatrix(5, MatrixUtils.createColumnRealMatrix(column5));
        CheckUtils.checkEquality(expected, matrix, ABSTOL, RELTOL);
    }

    /**
     * Tests the methods that set a column of the matrix, using indices outside the valid index
     * range.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain AbstractRealMatrix#setColumn(int, double[])}<br>
     * {@linkplain AbstractRealMatrix#setColumnVector(int, RealVector)}<br>
     * {@linkplain AbstractRealMatrix#setColumnMatrix(int, RealMatrix)}<br>
     * </p>
     */
    @Test
    public void testSetOutOfRangeColumn() {
        final RealMatrix matrix = new SimpleMatrix(5, 3);
        CheckUtils.checkSetOutOfRangeColumn(matrix);
    }

    /**
     * Tests the methods that set a column of the matrix, using incompatible arrays, vectors and
     * column matrices.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain AbstractRealMatrix#setColumn(int, double[])}<br>
     * {@linkplain AbstractRealMatrix#setColumnVector(int, RealVector)}<br>
     * {@linkplain AbstractRealMatrix#setColumnMatrix(int, RealMatrix)}<br>
     * </p>
     */
    @Test
    public void testSetIncompatibleColumn() {
        final RealMatrix matrix = new SimpleMatrix(5, 3);
        CheckUtils.checkSetIncompatibleColumn(matrix);
    }

    /**
     * Tests the method that returns a submatrix defined by the specified row/column index ranges.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain AbstractRealMatrix#getSubMatrix(int, int, int, int)}
     * </p>
     */
    @Test
    public void testGetSubMatrixByRange() {
        double[][] data;
        RealMatrix result;
        RealMatrix expected;

        // Tested matrix
        data = new double[][] { { 5.32034, 0.35039, -3.42622, 3.72874 },
                { 8.51414, 2.68578, -2.56611, 1.90477 }, { -7.15970, 3.69259, -5.13112, -9.74559 },
                { -0.60779, 4.10544, -0.08811, -3.07181 },
                { -0.99924, -2.17495, -7.41185, -8.22758 }, { 4.00549, -4.08488, 9.43623, 8.97924 } };
        final RealMatrix matrix = new SimpleMatrix(data);

        // Dimensions of the matrix
        final int nr = matrix.getRowDimension();
        final int nc = matrix.getColumnDimension();

        // Entire matrix
        result = matrix.getSubMatrix(0, nr - 1, 0, nc - 1);
        expected = new Array2DRowRealMatrix(data);
        CheckUtils.checkEquality(expected, result, 0., 0.);

        // Upper left matrix
        result = matrix.getSubMatrix(0, 2, 0, 1);
        data = new double[][] { { 5.32034, 0.35039 }, { 8.51414, 2.68578 }, { -7.1597, 3.69259 } };
        expected = new Array2DRowRealMatrix(data);
        CheckUtils.checkEquality(expected, result, 0., 0.);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());

        // Upper right matrix
        result = matrix.getSubMatrix(0, 2, 2, 3);
        data = new double[][] { { -3.42622, 3.72874 }, { -2.56611, 1.90477 },
                { -5.13112, -9.74559 } };
        expected = new Array2DRowRealMatrix(data);
        CheckUtils.checkEquality(expected, result, 0., 0.);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());

        // Lower left matrix
        result = matrix.getSubMatrix(3, 5, 0, 1);
        data = new double[][] { { -0.60779, 4.10544 }, { -0.99924, -2.17495 },
                { 4.00549, -4.08488 } };
        expected = new Array2DRowRealMatrix(data);
        CheckUtils.checkEquality(expected, result, 0., 0.);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());

        // Lower right matrix
        result = matrix.getSubMatrix(3, 5, 2, 3);
        data = new double[][] { { -0.08811, -3.07181 }, { -7.41185, -8.22758 },
                { 9.43623, 8.97924 } };
        expected = new Array2DRowRealMatrix(data);
        CheckUtils.checkEquality(expected, result, 0., 0.);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());

        // First row
        result = matrix.getSubMatrix(0, 0, 0, nc - 1);
        data = new double[][] { { 5.32034, 0.35039, -3.42622, 3.72874 } };
        expected = new Array2DRowRealMatrix(data);
        CheckUtils.checkEquality(expected, result, 0., 0.);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());

        // Last row
        result = matrix.getSubMatrix(nr - 1, nr - 1, 0, nc - 1);
        data = new double[][] { { 4.00549, -4.08488, 9.43623, 8.97924 } };
        expected = new Array2DRowRealMatrix(data);
        CheckUtils.checkEquality(expected, result, 0., 0.);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());

        // First column
        result = matrix.getSubMatrix(0, nr - 1, 0, 0);
        data = new double[][] { { 5.32034 }, { 8.51414 }, { -7.1597 }, { -0.60779 }, { -0.99924 },
                { 4.00549 } };
        expected = new Array2DRowRealMatrix(data);
        CheckUtils.checkEquality(expected, result, 0., 0.);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());

        // Last column
        result = matrix.getSubMatrix(0, nr - 1, nc - 1, nc - 1);
        data = new double[][] { { 3.72874 }, { 1.90477 }, { -9.74559 }, { -3.07181 }, { -8.22758 },
                { 8.97924 } };
        expected = new Array2DRowRealMatrix(data);
        CheckUtils.checkEquality(expected, result, 0., 0.);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());
    }

    /**
     * Tests the method that returns a submatrix defined by the row/column indices.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain AbstractRealMatrix#getSubMatrix(int[], int[])}
     * </p>
     */
    @Test
    public void testGetSubMatrixByIndices() {
        double[][] data;
        RealMatrix result;
        RealMatrix expected;
        int[] selectedRows;
        int[] selectedColumns;

        // Tested matrix
        data = new double[][] { { 2.05704, 9.27639, -9.20527, 9.03791 },
                { -0.68031, 1.52920, -7.40761, 0.96179 },
                { -4.38463, -5.38145, -9.14620, 1.09617 },
                { -5.66327, -0.97158, -1.06450, 5.28730 },
                { -1.61180, 8.28033, -6.87339, -3.94671 }, { 6.82787, 3.44034, -3.13464, 4.83780 } };
        final RealMatrix matrix = new SimpleMatrix(data);

        // Dimensions
        final int nr = matrix.getRowDimension();
        final int nc = matrix.getColumnDimension();

        // Entire matrix
        selectedRows = IntStream.range(0, nr).toArray();
        selectedColumns = IntStream.range(0, nc).toArray();
        result = matrix.getSubMatrix(selectedRows, selectedColumns);
        expected = new Array2DRowRealMatrix(data);
        CheckUtils.checkEquality(expected, result, 0., 0.);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());

        // Upper left part of the matrix
        selectedRows = IntStream.range(0, 3).toArray();
        selectedColumns = IntStream.range(0, 2).toArray();
        result = matrix.getSubMatrix(selectedRows, selectedColumns);
        data = new double[][] { { 2.05704, 9.27639 }, { -0.68031, 1.5292 }, { -4.38463, -5.38145 } };
        expected = new Array2DRowRealMatrix(data);
        CheckUtils.checkEquality(expected, result, 0., 0.);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());

        // Upper right part of the matrix
        selectedRows = IntStream.range(0, 3).toArray();
        selectedColumns = IntStream.range(2, nc).toArray();
        result = matrix.getSubMatrix(selectedRows, selectedColumns);
        data = new double[][] { { -9.20527, 9.03791 }, { -7.40761, 0.96179 }, { -9.1462, 1.09617 } };
        expected = new Array2DRowRealMatrix(data);
        CheckUtils.checkEquality(expected, result, 0., 0.);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());

        // Lower left part of the matrix
        selectedRows = IntStream.range(3, nr).toArray();
        selectedColumns = IntStream.range(0, 2).toArray();
        result = matrix.getSubMatrix(selectedRows, selectedColumns);
        data = new double[][] { { -5.66327, -0.97158 }, { -1.6118, 8.28033 }, { 6.82787, 3.44034 } };
        expected = new Array2DRowRealMatrix(data);
        CheckUtils.checkEquality(expected, result, 0., 0.);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());

        // Lower right part of the matrix
        selectedRows = IntStream.range(3, nr).toArray();
        selectedColumns = IntStream.range(2, nc).toArray();
        result = matrix.getSubMatrix(selectedRows, selectedColumns);
        data = new double[][] { { -1.0645, 5.2873 }, { -6.87339, -3.94671 }, { -3.13464, 4.8378 } };
        expected = new Array2DRowRealMatrix(data);
        CheckUtils.checkEquality(expected, result, 0., 0.);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());

        // First row of the matrix
        selectedRows = new int[] { 0 };
        selectedColumns = IntStream.range(0, nc).toArray();
        result = matrix.getSubMatrix(selectedRows, selectedColumns);
        data = new double[][] { { 2.05704, 9.27639, -9.20527, 9.03791 } };
        expected = new Array2DRowRealMatrix(data);
        CheckUtils.checkEquality(expected, result, 0., 0.);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());

        // Last row of the matrix
        selectedRows = new int[] { nr - 1 };
        selectedColumns = IntStream.range(0, nc).toArray();
        result = matrix.getSubMatrix(selectedRows, selectedColumns);
        data = new double[][] { { 6.82787, 3.44034, -3.13464, 4.8378 } };
        expected = new Array2DRowRealMatrix(data);
        CheckUtils.checkEquality(expected, result, 0., 0.);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());

        // First column of the matrix
        selectedRows = IntStream.range(0, nr).toArray();
        selectedColumns = new int[] { 0 };
        result = matrix.getSubMatrix(selectedRows, selectedColumns);
        data = new double[][] { { 2.05704 }, { -0.68031 }, { -4.38463 }, { -5.66327 }, { -1.6118 },
                { 6.82787 } };
        expected = new Array2DRowRealMatrix(data);
        CheckUtils.checkEquality(expected, result, 0., 0.);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());

        // Last column of the matrix
        selectedRows = IntStream.range(0, nr).toArray();
        selectedColumns = new int[] { nc - 1 };
        result = matrix.getSubMatrix(selectedRows, selectedColumns);
        data = new double[][] { { 9.03791 }, { 0.96179 }, { 1.09617 }, { 5.2873 }, { -3.94671 },
                { 4.8378 } };
        expected = new Array2DRowRealMatrix(data);
        CheckUtils.checkEquality(expected, result, 0., 0.);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());

        // Permutation
        selectedRows = new int[] { 4, 5, 3, 1, 0, 2 };
        selectedColumns = new int[] { 3, 1, 2, 0 };
        result = matrix.getSubMatrix(selectedRows, selectedColumns);
        data = new double[][] { { -3.94671, 8.28033, -6.87339, -1.6118 },
                { 4.8378, 3.44034, -3.13464, 6.82787 }, { 5.2873, -0.97158, -1.0645, -5.66327 },
                { 0.96179, 1.5292, -7.40761, -0.68031 }, { 9.03791, 9.27639, -9.20527, 2.05704 },
                { 1.09617, -5.38145, -9.1462, -4.38463 } };
        expected = new Array2DRowRealMatrix(data);
        CheckUtils.checkEquality(expected, result, 0., 0.);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());

        // Duplication
        selectedRows = new int[] { 1, 2, 0, 1, 2, 0 };
        selectedColumns = new int[] { 3, 1, 2, 0 };
        result = matrix.getSubMatrix(selectedRows, selectedColumns);
        data = new double[][] { { 0.96179, 1.5292, -7.40761, -0.68031 },
                { 1.09617, -5.38145, -9.1462, -4.38463 }, { 9.03791, 9.27639, -9.20527, 2.05704 },
                { 0.96179, 1.5292, -7.40761, -0.68031 }, { 1.09617, -5.38145, -9.1462, -4.38463 },
                { 9.03791, 9.27639, -9.20527, 2.05704 } };
        expected = new Array2DRowRealMatrix(data);
        CheckUtils.checkEquality(expected, result, 0., 0.);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());
    }

    /**
     * Tests the methods that return a submatrix, using indices outside the valid index range.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain AbstractRealMatrix#getSubMatrix(int, int, int, int)}<br>
     * {@linkplain AbstractRealMatrix#getSubMatrix(int[], int[])}<br>
     * </p>
     */
    @Test
    public void testGetSubMatrixInvalidIndices() {
        final RealMatrix matrix = new SimpleMatrix(5, 3);
        CheckUtils.checkGetSubMatrixInvalidRange(matrix);
        CheckUtils.checkGetSubMatrixInvalidIndex(matrix);
        CheckUtils.checkGetSubMatrixNullIndex(matrix);
        CheckUtils.checkGetSubMatrixEmptyIndex(matrix);
    }

    /**
     * Tests the method that replaces part of the matrix by a given the submatrix.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain AbstractRealMatrix#setSubMatrix(double[][], int, int)}
     * </p>
     */
    @Test
    public void testSetSubMatrix() {
        double[][] data;
        RealMatrix matrix;
        RealMatrix expected;

        // Submatrices
        final double[][] submatrix6x4 = { { -1.00832, 5.13473, 6.39178, 9.77271 },
                { -7.88255, 0.42501, 5.83694, 7.11972 }, { -6.99135, -7.18788, -7.09059, 1.93101 },
                { -5.76198, -0.78993, 3.62893, -2.03061 },
                { 4.44305, 1.60204, -6.36648, -9.17854 }, { 0.12533, 7.37073, 9.32005, -4.13415 } };
        final double[][] submatrix3x2 = new double[][] { { 6.48476, 7.55958 },
                { 3.92639, 0.57625 }, { 9.62896, -4.76990 } };

        // Entire matrix
        expected = new Array2DRowRealMatrix(submatrix6x4);
        matrix = new SimpleMatrix(6, 4);
        matrix.setSubMatrix(submatrix6x4, 0, 0);
        CheckUtils.checkEquality(expected, matrix, 0., 0.);

        // Upper left part of the matrix
        data = new double[][] { { 6.48476, 7.55958, 0, 0 }, { 3.92639, 0.57625, 0, 0 },
                { 9.62896, -4.7699, 0, 0 }, { 0, 0, 0, 0 }, { 0, 0, 0, 0 }, { 0, 0, 0, 0 } };
        expected = new Array2DRowRealMatrix(data);
        matrix = new SimpleMatrix(6, 4);
        matrix.setSubMatrix(submatrix3x2, 0, 0);
        CheckUtils.checkEquality(expected, matrix, 0., 0.);

        // Upper right part of the matrix
        data = new double[][] { { 0, 0, 6.48476, 7.55958 }, { 0, 0, 3.92639, 0.57625 },
                { 0, 0, 9.62896, -4.7699 }, { 0, 0, 0, 0 }, { 0, 0, 0, 0 }, { 0, 0, 0, 0 } };
        expected = new Array2DRowRealMatrix(data);
        matrix = new SimpleMatrix(6, 4);
        matrix.setSubMatrix(submatrix3x2, 0, 2);
        CheckUtils.checkEquality(expected, matrix, 0., 0.);

        // Lower left part of the matrix
        data = new double[][] { { 0, 0, 0, 0 }, { 0, 0, 0, 0 }, { 0, 0, 0, 0 },
                { 6.48476, 7.55958, 0, 0 }, { 3.92639, 0.57625, 0, 0 }, { 9.62896, -4.7699, 0, 0 } };
        expected = new Array2DRowRealMatrix(data);
        matrix = new SimpleMatrix(6, 4);
        matrix.setSubMatrix(submatrix3x2, 3, 0);
        CheckUtils.checkEquality(expected, matrix, 0., 0.);

        // Lower right part of the matrix
        data = new double[][] { { 0, 0, 0, 0 }, { 0, 0, 0, 0 }, { 0, 0, 0, 0 },
                { 0, 0, 6.48476, 7.55958 }, { 0, 0, 3.92639, 0.57625 }, { 0, 0, 9.62896, -4.7699 } };
        expected = new Array2DRowRealMatrix(data);
        matrix = new SimpleMatrix(6, 4);
        matrix.setSubMatrix(submatrix3x2, 3, 2);
        CheckUtils.checkEquality(expected, matrix, 0., 0.);
    }

    /**
     * Tests the method that replaces part of the matrix by a given submatrix, using indices outside
     * the valid index range.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain AbstractRealMatrix#setSubMatrix(double[][], int, int)}<br>
     * </p>
     */
    @Test
    public void testSetSubMatrixInvalidIndices() {
        final RealMatrix matrix = new SimpleMatrix(5, 3);
        CheckUtils.checkSetSubMatrixInvalidIndex(matrix);
    }

    /**
     * Tests the method that replaces part of the matrix by a given submatrix, using a {@code null}
     * submatrix data array.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain AbstractRealMatrix#setSubMatrix(double[][], int, int)}<br>
     * </p>
     */
    @Test
    public void testSetSubMatrixInvalidData() {
        final RealMatrix matrix = new SimpleMatrix(5, 3);
        CheckUtils.checkSetSubMatrixNullData(matrix);
        CheckUtils.checkSetSubMatrixEmptyData(matrix);
        CheckUtils.checkSetSubMatrixInvalidData(matrix);
    }

    /**
     * Tests the method that copies part of the matrix into a given array.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain AbstractRealMatrix#copySubMatrix(int, int, int, int, double[][])}<br>
     * {@linkplain AbstractRealMatrix#copySubMatrix(int, int, int, int, double[][], int, int)}<br>
     * </p>
     */
    @Test
    public void testCopySubMatrixByRange() {
        double[][] expected;
        double[][] submatrix;

        // Tested matrix
        final double[][] data = new double[][] { { 5.59212, 6.27471, 2.48383, -9.64752 },
                { 7.23152, 9.40270, 1.66868, 7.85333 }, { -9.76979, -9.69494, -7.34750, -2.96427 },
                { -1.74174, 1.41624, -3.40988, 3.70185 },
                { -6.90758, -1.74495, -8.04319, -4.10418 },
                { -1.16996, -2.64074, 1.78689, 2.75978 } };
        final RealMatrix matrix = new SimpleMatrix(data);

        // Dimensions of the matrix
        final int nr = matrix.getRowDimension();
        final int nc = matrix.getColumnDimension();

        // Copy the entire matrix
        submatrix = new double[nr][nc];
        matrix.copySubMatrix(0, nr - 1, 0, nc - 1, submatrix);
        CheckUtils.checkEquality(data, submatrix, 0., 0.);

        submatrix = new double[nr][nc];
        matrix.copySubMatrix(0, nr - 1, 0, nc - 1, submatrix, 0, 0);
        CheckUtils.checkEquality(data, submatrix, 0., 0.);

        // Copy the upper left part of the matrix in the lower right part of the destination array
        submatrix = new double[nr][nc];
        matrix.copySubMatrix(0, 2, 0, 1, submatrix, 3, 2);
        expected = new double[][] { { 0, 0, 0, 0 }, { 0, 0, 0, 0 }, { 0, 0, 0, 0 },
                { 0, 0, 5.59212, 6.27471 }, { 0, 0, 7.23152, 9.4027 }, { 0, 0, -9.76979, -9.69494 } };
        CheckUtils.checkEquality(expected, submatrix, 0., 0.);

        // Copy the lower left part of the matrix in the upper right part of the destination array
        submatrix = new double[nr][nc];
        matrix.copySubMatrix(3, nr - 1, 0, 1, submatrix, 0, 2);
        expected = new double[][] { { 0, 0, -1.74174, 1.41624 }, { 0, 0, -6.90758, -1.74495 },
                { 0, 0, -1.16996, -2.64074 }, { 0, 0, 0, 0 }, { 0, 0, 0, 0 }, { 0, 0, 0, 0 } };
        CheckUtils.checkEquality(expected, submatrix, 0., 0.);

        // Copy the upper right part of the matrix in the lower left part of the destination array
        submatrix = new double[nr][nc];
        matrix.copySubMatrix(0, 2, 2, nc - 1, submatrix, 3, 0);
        expected = new double[][] { { 0, 0, 0, 0 }, { 0, 0, 0, 0 }, { 0, 0, 0, 0 },
                { 2.48383, -9.64752, 0, 0 }, { 1.66868, 7.85333, 0, 0 },
                { -7.3475, -2.96427, 0, 0 } };
        CheckUtils.checkEquality(expected, submatrix, 0., 0.);

        // Copy the lower right part of the matrix in the upper left part of the destination array
        submatrix = new double[nr][nc];
        matrix.copySubMatrix(3, nr - 1, 2, nc - 1, submatrix, 0, 0);
        expected = new double[][] { { -3.40988, 3.70185, 0, 0 }, { -8.04319, -4.10418, 0, 0 },
                { 1.78689, 2.75978, 0, 0 }, { 0, 0, 0, 0 }, { 0, 0, 0, 0 }, { 0, 0, 0, 0 } };
        CheckUtils.checkEquality(expected, submatrix, 0., 0.);
    }

    /**
     * Tests the method that copies part of the matrix into a given array.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain AbstractRealMatrix#copySubMatrix(int[], int[], double[][])}<br>
     * {@linkplain AbstractRealMatrix#copySubMatrix(int[], int[], double[][], int, int)}<br>
     * </p>
     */
    @Test
    public void testCopySubMatrixByIndices() {
        double[][] expected;
        double[][] submatrix;
        int[] selectedRows;
        int[] selectedColumns;

        // Tested matrix
        final double[][] data = new double[][] { { -3.02176, -9.15007, -9.95371, 8.76845 },
                { -8.18002, -2.59318, 8.98976, 8.25781 }, { 2.45232, -6.06212, 5.18294, -1.99645 },
                { -7.32928, -6.57202, 2.44599, 7.44474 },
                { -5.18495, -8.24847, 2.60614, -6.25629 },
                { -4.38797, -5.76646, -2.39938, -0.84136 } };
        final RealMatrix matrix = new SimpleMatrix(data);

        // Dimensions of the matrix
        final int nr = matrix.getRowDimension();
        final int nc = matrix.getColumnDimension();

        // Copy the entire matrix
        selectedRows = IntStream.range(0, nr).toArray();
        selectedColumns = IntStream.range(0, nc).toArray();
        submatrix = new double[nr][nc];
        matrix.copySubMatrix(selectedRows, selectedColumns, submatrix);
        CheckUtils.checkEquality(data, submatrix, 0., 0.);

        submatrix = new double[nr][nc];
        matrix.copySubMatrix(selectedRows, selectedColumns, submatrix, 0, 0);
        CheckUtils.checkEquality(data, submatrix, 0., 0.);

        // Copy the upper left part of the matrix in the lower right part of the destination array
        selectedRows = IntStream.range(0, 3).toArray();
        selectedColumns = IntStream.range(0, 2).toArray();
        submatrix = new double[nr][nc];
        matrix.copySubMatrix(selectedRows, selectedColumns, submatrix, 3, 2);
        expected = new double[][] { { 0, 0, 0, 0 }, { 0, 0, 0, 0 }, { 0, 0, 0, 0 },
                { 0, 0, -3.02176, -9.15007 }, { 0, 0, -8.18002, -2.59318 },
                { 0, 0, 2.45232, -6.06212 } };
        CheckUtils.checkEquality(expected, submatrix, 0., 0.);

        // Copy the lower left part of the matrix in the upper right part of the destination array
        selectedRows = IntStream.range(3, nr).toArray();
        selectedColumns = IntStream.range(0, 2).toArray();
        submatrix = new double[nr][nc];
        matrix.copySubMatrix(selectedRows, selectedColumns, submatrix, 0, 2);
        expected = new double[][] { { 0, 0, -7.32928, -6.57202 }, { 0, 0, -5.18495, -8.24847 },
                { 0, 0, -4.38797, -5.76646 }, { 0, 0, 0, 0 }, { 0, 0, 0, 0 }, { 0, 0, 0, 0 } };
        CheckUtils.checkEquality(expected, submatrix, 0., 0.);

        // Copy the upper right part of the matrix in the lower left part of the destination array
        selectedRows = IntStream.range(0, 3).toArray();
        selectedColumns = IntStream.range(2, nc).toArray();
        submatrix = new double[nr][nc];
        matrix.copySubMatrix(selectedRows, selectedColumns, submatrix, 3, 0);
        expected = new double[][] { { 0, 0, 0, 0 }, { 0, 0, 0, 0 }, { 0, 0, 0, 0 },
                { -9.95371, 8.76845, 0, 0 }, { 8.98976, 8.25781, 0, 0 },
                { 5.18294, -1.99645, 0, 0 } };
        CheckUtils.checkEquality(expected, submatrix, 0., 0.);

        // Copy the lower right part of the matrix in the upper left part of the destination array
        selectedRows = IntStream.range(3, nr).toArray();
        selectedColumns = IntStream.range(2, nc).toArray();
        submatrix = new double[nr][nc];
        matrix.copySubMatrix(selectedRows, selectedColumns, submatrix, 0, 0);
        expected = new double[][] { { 2.44599, 7.44474, 0, 0 }, { 2.60614, -6.25629, 0, 0 },
                { -2.39938, -0.84136, 0, 0 }, { 0, 0, 0, 0 }, { 0, 0, 0, 0 }, { 0, 0, 0, 0 } };
        CheckUtils.checkEquality(expected, submatrix, 0., 0.);

        // Permutation
        selectedRows = new int[] { 4, 5, 3, 1, 0, 2 };
        selectedColumns = new int[] { 3, 1, 2, 0 };
        submatrix = new double[nr][nc];
        matrix.copySubMatrix(selectedRows, selectedColumns, submatrix);
        expected = new double[][] { { -6.25629, -8.24847, 2.60614, -5.18495 },
                { -0.84136, -5.76646, -2.39938, -4.38797 },
                { 7.44474, -6.57202, 2.44599, -7.32928 }, { 8.25781, -2.59318, 8.98976, -8.18002 },
                { 8.76845, -9.15007, -9.95371, -3.02176 }, { -1.99645, -6.06212, 5.18294, 2.45232 } };
        CheckUtils.checkEquality(expected, submatrix, 0., 0.);

        // Duplication
        selectedRows = new int[] { 1, 2, 0, 1, 2, 0 };
        selectedColumns = new int[] { 3, 1, 2, 0 };
        submatrix = new double[nr][nc];
        matrix.copySubMatrix(selectedRows, selectedColumns, submatrix);
        expected = new double[][] { { 8.25781, -2.59318, 8.98976, -8.18002 },
                { -1.99645, -6.06212, 5.18294, 2.45232 },
                { 8.76845, -9.15007, -9.95371, -3.02176 },
                { 8.25781, -2.59318, 8.98976, -8.18002 }, { -1.99645, -6.06212, 5.18294, 2.45232 },
                { 8.76845, -9.15007, -9.95371, -3.02176 } };
        CheckUtils.checkEquality(expected, submatrix, 0., 0.);
    }

    /**
     * Tests the methods that copies part of the matrix into a given array, using indices outside
     * the valid index range.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain AbstractRealMatrix#copySubMatrix(int, int, int, int, double[][])}<br>
     * {@linkplain AbstractRealMatrix#copySubMatrix(int, int, int, int, double[][], int, int)}<br>
     * {@linkplain AbstractRealMatrix#copySubMatrix(int[], int[], double[][])}<br>
     * {@linkplain AbstractRealMatrix#copySubMatrix(int[], int[], double[][], int, int)}<br>
     * </p>
     */
    @Test
    public void testCopySubMatrixInvalidIndices() {
        final RealMatrix matrix = new SimpleMatrix(5, 3);
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
     * {@linkplain AbstractRealMatrix#copySubMatrix(int, int, int, int, double[][])}<br>
     * {@linkplain AbstractRealMatrix#copySubMatrix(int, int, int, int, double[][], int, int)}<br>
     * {@linkplain AbstractRealMatrix#copySubMatrix(int[], int[], double[][])}<br>
     * {@linkplain AbstractRealMatrix#copySubMatrix(int[], int[], double[][], int, int)}<br>
     * </p>
     */
    @Test
    public void testCopySubMatrixNullDestinationArray() {
        final RealMatrix matrix = new SimpleMatrix(5, 3);
        CheckUtils.checkCopySubMatrixNullDestinationArray(matrix);
    }

    /**
     * Tests the methods that copies part of the matrix into a given array, using a destination
     * array with no rows or columns.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain AbstractRealMatrix#copySubMatrix(int, int, int, int, double[][])}<br>
     * {@linkplain AbstractRealMatrix#copySubMatrix(int, int, int, int, double[][], int, int)}<br>
     * {@linkplain AbstractRealMatrix#copySubMatrix(int[], int[], double[][])}<br>
     * {@linkplain AbstractRealMatrix#copySubMatrix(int[], int[], double[][], int, int)}<br>
     * </p>
     */
    @Test
    public void testCopySubMatrixEmptyDestinationArray() {
        final RealMatrix matrix = new SimpleMatrix(5, 3);
        CheckUtils.checkCopySubMatrixEmptyDestinationArray(matrix);
    }

    /**
     * Tests the methods that copies part of the matrix into a given array, using a destination
     * array too small to store the extracted
     * submatrix.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain AbstractRealMatrix#copySubMatrix(int, int, int, int, double[][])}<br>
     * {@linkplain AbstractRealMatrix#copySubMatrix(int, int, int, int, double[][], int, int)}<br>
     * {@linkplain AbstractRealMatrix#copySubMatrix(int[], int[], double[][])}<br>
     * {@linkplain AbstractRealMatrix#copySubMatrix(int[], int[], double[][], int, int)}<br>
     * </p>
     */
    @Test
    public void testCopySubMatrixIncompatibleDestinationArray() {
        final RealMatrix matrix = new SimpleMatrix(5, 3);
        CheckUtils.checkCopySubMatrixIncompatibleDestinationArray(matrix);
    }

    /**
     * Tests the methods that concatenate two matrices horizontally.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain AbstractRealMatrix#concatenateHorizontally(RealMatrix)}<br>
     * {@linkplain AbstractRealMatrix#concatenateHorizontally(RealMatrix, boolean)}<br>
     * {@linkplain MatrixUtils#concatenateHorizontally(RealMatrix, RealMatrix)}<br>
     * </p>
     */
    @Test
    public void testHorizontalConcatenation() {
        double[][] data;
        RealMatrix result;
        RealMatrix expected;

        // Tested matrix (4x6)
        data = new double[][] { { -5.24410, 5.59921, 6.40271, 9.99344, 1.23839, 4.90865 },
                { -6.77380, -4.19439, -5.34003, 8.35519, 9.04127, 6.55394 },
                { -5.39947, -7.13824, -1.52986, -8.31252, -0.43015, -2.67379 },
                { -8.18425, 2.65689, 6.22165, 9.15687, 0.33558, -9.45451 } };
        final RealMatrix matrix = new SimpleMatrix(data);

        // Matrix to be concatenated with the tested matrix (4x5)
        data = new double[][] { { 8.78396, -0.05360, -2.53900, -1.50246, -9.93174 },
                { 1.85599, -8.93560, 1.79946, 5.86008, 2.78559 },
                { 5.46882, -9.78652, 7.27244, -6.27767, -2.10563 },
                { 1.92494, 0.09125, 9.39511, -1.90504, 7.67314 } };
        final RealMatrix realMatrix = new Array2DRowRealMatrix(data, false);

        // Matrix placed to the right of the concatenated matrix
        data = new double[][] {
                { -5.2441, 5.59921, 6.40271, 9.99344, 1.23839, 4.90865, 8.78396, -0.0536, -2.539,
                        -1.50246, -9.93174 },
                { -6.7738, -4.19439, -5.34003, 8.35519, 9.04127, 6.55394, 1.85599, -8.9356,
                        1.79946, 5.86008, 2.78559 },
                { -5.39947, -7.13824, -1.52986, -8.31252, -0.43015, -2.67379, 5.46882, -9.78652,
                        7.27244, -6.27767, -2.10563 },
                { -8.18425, 2.65689, 6.22165, 9.15687, 0.33558, -9.45451, 1.92494, 0.09125,
                        9.39511, -1.90504, 7.67314 } };
        expected = new Array2DRowRealMatrix(data);

        result = matrix.concatenateHorizontally(realMatrix);
        CheckUtils.checkEquality(expected, result, 0., 0.);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());
        Assert.assertNotSame(matrix, result);

        result = MatrixUtils.concatenateHorizontally(matrix, realMatrix);
        CheckUtils.checkEquality(expected, result, 0., 0.);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());
        Assert.assertNotSame(matrix, result);

        result = matrix.concatenateHorizontally(realMatrix, true);
        CheckUtils.checkEquality(expected, result, 0., 0.);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());
        Assert.assertNotSame(matrix, result);

        result = MatrixUtils.concatenateHorizontally(matrix, realMatrix);
        CheckUtils.checkEquality(expected, result, 0., 0.);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());
        Assert.assertNotSame(matrix, result);

        // Matrix placed to the left of the concatenated matrix
        data = new double[][] {
                { 8.78396, -0.0536, -2.539, -1.50246, -9.93174, -5.2441, 5.59921, 6.40271, 9.99344,
                        1.23839, 4.90865 },
                { 1.85599, -8.9356, 1.79946, 5.86008, 2.78559, -6.7738, -4.19439, -5.34003,
                        8.35519, 9.04127, 6.55394 },
                { 5.46882, -9.78652, 7.27244, -6.27767, -2.10563, -5.39947, -7.13824, -1.52986,
                        -8.31252, -0.43015, -2.67379 },
                { 1.92494, 0.09125, 9.39511, -1.90504, 7.67314, -8.18425, 2.65689, 6.22165,
                        9.15687, 0.33558, -9.45451 } };
        expected = new Array2DRowRealMatrix(data);

        result = matrix.concatenateHorizontally(realMatrix, false);
        CheckUtils.checkEquality(expected, result, 0., 0.);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());
        Assert.assertNotSame(matrix, result);

        result = MatrixUtils.concatenateHorizontally(realMatrix, matrix);
        CheckUtils.checkEquality(expected, result, 0., 0.);
        Assert.assertEquals(Array2DRowRealMatrix.class, result.getClass());
        Assert.assertNotSame(matrix, result);
    }

    /**
     * Tests the methods that concatenate two matrices vertically.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain AbstractRealMatrix#concatenateVertically(RealMatrix)}<br>
     * {@linkplain AbstractRealMatrix#concatenateVertically(RealMatrix, boolean)}<br>
     * {@linkplain MatrixUtils#concatenateVertically(RealMatrix, RealMatrix)}<br>
     * </p>
     */
    @Test
    public void testVerticalConcatenation() {
        double[][] data;
        RealMatrix result;
        RealMatrix expected;

        // Tested matrix (6x4)
        data = new double[][] { { -0.48373, 2.55704, -1.94812, 9.63349 },
                { 2.50019, -3.36846, -9.86821, -5.57649 },
                { 8.03139, 8.61764, -6.42881, -4.01976 },
                { 2.52776, -8.65045, -1.18961, -8.97491 }, { 9.61542, 7.44474, 8.02087, 6.28806 },
                { 8.99886, 2.23283, 8.80636, -5.37418 } };
        final RealMatrix matrix = new SimpleMatrix(data);

        // Matrix to be concatenated with the tested matrix (5x4)
        data = new double[][] { { 1.48196, 9.65360, 6.86106, -0.68446 },
                { -4.58650, -3.79222, -5.86321, 8.99294 },
                { -1.45266, 7.66243, -1.24818, -0.90154 },
                { -8.93709, -1.94457, -2.15958, 0.87090 }, { -4.16504, 0.50900, -4.87274, 3.51643 } };
        final RealMatrix realMatrix = new Array2DRowRealMatrix(data);

        // Matrix placed at the bottom of the concatenated matrix
        data = new double[][] { { -0.48373, 2.55704, -1.94812, 9.63349 },
                { 2.50019, -3.36846, -9.86821, -5.57649 },
                { 8.03139, 8.61764, -6.42881, -4.01976 },
                { 2.52776, -8.65045, -1.18961, -8.97491 }, { 9.61542, 7.44474, 8.02087, 6.28806 },
                { 8.99886, 2.23283, 8.80636, -5.37418 }, { 1.48196, 9.6536, 6.86106, -0.68446 },
                { -4.5865, -3.79222, -5.86321, 8.99294 },
                { -1.45266, 7.66243, -1.24818, -0.90154 },
                { -8.93709, -1.94457, -2.15958, 0.8709 }, { -4.16504, 0.509, -4.87274, 3.51643 } };
        expected = new Array2DRowRealMatrix(data);

        result = matrix.concatenateVertically(realMatrix);
        CheckUtils.checkEquality(expected, result, 0., 0.);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());
        Assert.assertNotSame(matrix, result);

        result = MatrixUtils.concatenateVertically(matrix, realMatrix);
        CheckUtils.checkEquality(expected, result, 0., 0.);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());
        Assert.assertNotSame(matrix, result);

        result = matrix.concatenateVertically(realMatrix, true);
        CheckUtils.checkEquality(expected, result, 0., 0.);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());
        Assert.assertNotSame(matrix, result);

        result = MatrixUtils.concatenateVertically(matrix, realMatrix);
        CheckUtils.checkEquality(expected, result, 0., 0.);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());
        Assert.assertNotSame(matrix, result);

        // Matrix placed at the top of the concatenated matrix
        data = new double[][] { { 1.48196, 9.6536, 6.86106, -0.68446 },
                { -4.5865, -3.79222, -5.86321, 8.99294 },
                { -1.45266, 7.66243, -1.24818, -0.90154 },
                { -8.93709, -1.94457, -2.15958, 0.8709 }, { -4.16504, 0.509, -4.87274, 3.51643 },
                { -0.48373, 2.55704, -1.94812, 9.63349 },
                { 2.50019, -3.36846, -9.86821, -5.57649 },
                { 8.03139, 8.61764, -6.42881, -4.01976 },
                { 2.52776, -8.65045, -1.18961, -8.97491 }, { 9.61542, 7.44474, 8.02087, 6.28806 },
                { 8.99886, 2.23283, 8.80636, -5.37418 } };
        expected = new Array2DRowRealMatrix(data);

        result = matrix.concatenateVertically(realMatrix, false);
        CheckUtils.checkEquality(expected, result, 0., 0.);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());
        Assert.assertNotSame(matrix, result);

        result = MatrixUtils.concatenateVertically(realMatrix, matrix);
        CheckUtils.checkEquality(expected, result, 0., 0.);
        Assert.assertEquals(Array2DRowRealMatrix.class, result.getClass());
        Assert.assertNotSame(matrix, result);
    }

    /**
     * Tests the methods that concatenate two matrices diagonally.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain AbstractRealMatrix#concatenateDiagonally(RealMatrix)}<br>
     * {@linkplain AbstractRealMatrix#concatenateDiagonally(RealMatrix, boolean)}<br>
     * {@linkplain AbstractRealMatrix#concatenateDiagonally(RealMatrix, boolean, boolean)}<br>
     * </p>
     */
    @Test
    public void testDiagonalConcatenation() {
        double[][] data;
        RealMatrix result;
        RealMatrix expected;

        // Tested matrix (4x6)
        data = new double[][] { { 9.31078, 6.58960, 5.25627, -6.78272, -7.17920, -6.20628 },
                { -4.53330, 3.04306, -6.64664, -7.53566, 8.47092, -7.07428 },
                { -8.62286, 3.51007, 9.62413, 5.35454, -5.38309, -4.08529 },
                { 3.11153, -4.04070, 2.18051, -6.12007, 7.22345, 9.35937 } };
        final RealMatrix matrix = new SimpleMatrix(data);

        // Matrix to be concatenated with the tested matrix (3x2)
        data = new double[][] { { -9.73643, 7.55991 }, { 0.89731, -3.48116 }, { -2.18543, 7.10951 } };
        final RealMatrix realMatrix = new Array2DRowRealMatrix(data);

        // Matrix placed at the bottom right of the concatenated matrix
        data = new double[][] { { 9.31078, 6.5896, 5.25627, -6.78272, -7.1792, -6.20628, 0, 0 },
                { -4.5333, 3.04306, -6.64664, -7.53566, 8.47092, -7.07428, 0, 0 },
                { -8.62286, 3.51007, 9.62413, 5.35454, -5.38309, -4.08529, 0, 0 },
                { 3.11153, -4.0407, 2.18051, -6.12007, 7.22345, 9.35937, 0, 0 },
                { 0, 0, 0, 0, 0, 0, -9.73643, 7.55991 }, { 0, 0, 0, 0, 0, 0, 0.89731, -3.48116 },
                { 0, 0, 0, 0, 0, 0, -2.18543, 7.10951 } };
        expected = new Array2DRowRealMatrix(data);

        result = matrix.concatenateDiagonally(realMatrix);
        CheckUtils.checkEquality(expected, result, 0., 0.);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());
        Assert.assertNotSame(matrix, result);

        result = matrix.concatenateDiagonally(realMatrix, true);
        CheckUtils.checkEquality(expected, result, 0., 0.);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());
        Assert.assertNotSame(matrix, result);

        result = matrix.concatenateDiagonally(realMatrix, true, true);
        CheckUtils.checkEquality(expected, result, 0., 0.);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());
        Assert.assertNotSame(matrix, result);

        // Matrix placed at the top left of the concatenated matrix
        data = new double[][] { { -9.73643, 7.55991, 0, 0, 0, 0, 0, 0 },
                { 0.89731, -3.48116, 0, 0, 0, 0, 0, 0 }, { -2.18543, 7.10951, 0, 0, 0, 0, 0, 0 },
                { 0, 0, 9.31078, 6.5896, 5.25627, -6.78272, -7.1792, -6.20628 },
                { 0, 0, -4.5333, 3.04306, -6.64664, -7.53566, 8.47092, -7.07428 },
                { 0, 0, -8.62286, 3.51007, 9.62413, 5.35454, -5.38309, -4.08529 },
                { 0, 0, 3.11153, -4.0407, 2.18051, -6.12007, 7.22345, 9.35937 } };
        expected = new Array2DRowRealMatrix(data);

        result = matrix.concatenateDiagonally(realMatrix, false);
        CheckUtils.checkEquality(expected, result, 0., 0.);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());
        Assert.assertNotSame(matrix, result);

        result = matrix.concatenateDiagonally(realMatrix, false, false);
        CheckUtils.checkEquality(expected, result, 0., 0.);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());
        Assert.assertNotSame(matrix, result);

        // Matrix placed at the bottom left of the concatenated matrix
        data = new double[][] { { 0, 0, 9.31078, 6.5896, 5.25627, -6.78272, -7.1792, -6.20628 },
                { 0, 0, -4.5333, 3.04306, -6.64664, -7.53566, 8.47092, -7.07428 },
                { 0, 0, -8.62286, 3.51007, 9.62413, 5.35454, -5.38309, -4.08529 },
                { 0, 0, 3.11153, -4.0407, 2.18051, -6.12007, 7.22345, 9.35937 },
                { -9.73643, 7.55991, 0, 0, 0, 0, 0, 0 }, { 0.89731, -3.48116, 0, 0, 0, 0, 0, 0 },
                { -2.18543, 7.10951, 0, 0, 0, 0, 0, 0 } };
        expected = new Array2DRowRealMatrix(data);

        result = matrix.concatenateDiagonally(realMatrix, false, true);
        CheckUtils.checkEquality(expected, result, 0., 0.);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());
        Assert.assertNotSame(matrix, result);

        // Matrix placed at the top right of the concatenated matrix
        data = new double[][] { { 0, 0, 0, 0, 0, 0, -9.73643, 7.55991 },
                { 0, 0, 0, 0, 0, 0, 0.89731, -3.48116 }, { 0, 0, 0, 0, 0, 0, -2.18543, 7.10951 },
                { 9.31078, 6.5896, 5.25627, -6.78272, -7.1792, -6.20628, 0, 0 },
                { -4.5333, 3.04306, -6.64664, -7.53566, 8.47092, -7.07428, 0, 0 },
                { -8.62286, 3.51007, 9.62413, 5.35454, -5.38309, -4.08529, 0, 0 },
                { 3.11153, -4.0407, 2.18051, -6.12007, 7.22345, 9.35937, 0, 0 } };
        expected = new Array2DRowRealMatrix(data);

        result = matrix.concatenateDiagonally(realMatrix, true, false);
        CheckUtils.checkEquality(expected, result, 0., 0.);
        Assert.assertEquals(SimpleMatrix.class, result.getClass());
        Assert.assertNotSame(matrix, result);
    }

    /**
     * Tests the methods that concatenate two matrices horizontally, using matrices with
     * incompatible dimensions.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain AbstractRealMatrix#concatenateHorizontally(RealMatrix)}<br>
     * {@linkplain AbstractRealMatrix#concatenateHorizontally(RealMatrix, boolean)}<br>
     * </p>
     */
    @Test
    public void testHorizontalConcatenationIncompatibleMatrix() {
        final RealMatrix matrix = new SimpleMatrix(4, 5);
        CheckUtils.checkConcatenateHorizontallyIncompatibleMatrix(matrix);
    }

    /**
     * Tests the methods that concatenate two matrices vertically, using matrices with incompatible
     * dimensions.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain AbstractRealMatrix#concatenateVertically(RealMatrix)}<br>
     * {@linkplain AbstractRealMatrix#concatenateVertically(RealMatrix, boolean)}<br>
     * </p>
     */
    @Test
    public void testVerticalConcatenationIncompatibleMatrix() {
        final RealMatrix matrix = new SimpleMatrix(4, 5);
        CheckUtils.checkConcatenateVerticallyIncompatibleMatrix(matrix);
    }

    /**
     * Tests the matrix inversion.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain AbstractRealMatrix#getInverse()}
     * </p>
     */
    @Test
    public void testGetMatrixInverse() {
        RealMatrix result;
        double[][] data;

        // Tested matrix
        data = new double[][] { { -7.22581, -1.94773, 3.18567, 2.97745 },
                { -4.38107, -9.01950, -2.08494, -1.14831 },
                { 2.34893, -0.91182, 4.24831, 0.28872 }, { 2.44524, -9.96399, -4.26385, 8.47213 } };
        final RealMatrix matrix = new SimpleMatrix(data);

        // Expected matrix
        data = new double[][] {
                { -0.090570907552804, -0.01828956422856, 0.085475371556638, 0.026438429287318 },
                { 0.02261997627636, -0.088743219581611, -0.077900838473902, -0.017323039750054 },
                { 0.049649723199482, -0.002127473380242, 0.173380524247418, -0.023645882706979 },
                { 0.077731549877825, -0.100161990646636, -0.029029585804087, 0.078129420033709 } };
        final RealMatrix expected = new Array2DRowRealMatrix(data);

        // Decomposition builder
        final Function<RealMatrix, Decomposition> decompositionBuilder = QRDecomposition
                .decompositionBuilder(1E-14);

        // Tolerances:
        // (a higher relative tolerance is required since different types of decomposition are used
        // )
        final double absTol = 0.;
        final double relTol = 1E-12;

        // Check the matrix inversion using the default decomposition
        result = matrix.getInverse();
        CheckUtils.checkEquality(expected, result, absTol, relTol);
        Assert.assertEquals(Array2DRowRealMatrix.class, result.getClass());
        Assert.assertNotSame(matrix, result);

        // Check the matrix inversion using the a QR decomposition
        result = matrix.getInverse(decompositionBuilder);
        CheckUtils.checkEquality(expected, result, absTol, relTol);
        Assert.assertEquals(BlockRealMatrix.class, result.getClass());
        Assert.assertNotSame(matrix, result);
    }

    /**
     * Tests the inversion of a rectangular matrix.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain AbstractRealMatrix#getInverse()}
     * </p>
     */
    @Test
    public void testGetNonSquareMatrixInverse() {
        final RealMatrix matrix = new SimpleMatrix(4, 5);
        CheckUtils.checkInverseNonSquareMatrix(matrix);
    }

    /**
     * Tests the inversion of a singular matrix.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain AbstractRealMatrix#getInverse()}
     * </p>
     */
    @Test
    public void testGetSingularMatrixInverse() {
        final RealMatrix matrix = new SimpleMatrix(4, 4);
        CheckUtils.checkInverseSingularMatrix(matrix);
    }

    /**
     * Tests the method that sets the decomposition algorithm to use by default when computing the
     * inverse matrix.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain AbstractRealMatrix#getDefaultDecomposition()}<br>
     * {@linkplain AbstractRealMatrix#setDefaultDecomposition(Function)}<br>
     * </p>
     */
    @Test
    public void testSetDefaultDecomposition() {
        final RealMatrix matrix = new SimpleMatrix(4, 4);
        CheckUtils.checkDefaultDecomposition(matrix);
    }

    /**
     * Tests the methods that returns a string representation of the matrix.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain AbstractRealMatrix#toString()}<br>
     * {@linkplain AbstractRealMatrix#toString(RealMatrixFormat)}<br>
     * </p>
     */
    @Test
    public void testToString() {
        String result;
        StringBuilder builder;
        RealMatrixFormat format;

        // Tested matrix
        final double[][] data = { { 0.97900, 6.08152, -1.10616 }, { -2.90091, 4.06284, -6.07261 } };
        final RealMatrix matrix = new SimpleMatrix(data);

        // Line separator
        // (the default matrix format uses "\n" instead of System.lineSeparator())
        final String lineSeparator = "\n";

        // Visual format (format used by default)
        format = MatrixUtils.VISUAL_FORMAT;
        result = matrix.toString(format);
        builder = new StringBuilder();
        builder.append("SimpleMatrix[[     0.97900,      6.0815,     -1.1062]");
        builder.append(lineSeparator);
        builder.append("             [     -2.9009,      4.0628,     -6.0726]]");
        Assert.assertEquals(builder.toString(), result);

        result = matrix.toString();
        Assert.assertEquals(builder.toString(), result);

        // Default format
        format = MatrixUtils.DEFAULT_FORMAT;
        result = matrix.toString(format);
        builder = new StringBuilder();
        builder.append("SimpleMatrix{{0.979,6.08152,-1.10616},{-2.90091,4.06284,-6.07261}}");
        Assert.assertEquals(builder.toString(), result);

        // Java format
        format = MatrixUtils.JAVA_FORMAT;
        result = matrix.toString(format);
        builder = new StringBuilder();
        builder.append("SimpleMatrix{{0.979, 6.08152, -1.10616}, {-2.90091, 4.06284, -6.07261}}");
        Assert.assertEquals(builder.toString(), result);

        // Octave format
        format = MatrixUtils.OCTAVE_FORMAT;
        result = matrix.toString(format);
        builder = new StringBuilder();
        builder.append("SimpleMatrix[0.979, 6.08152, -1.10616; -2.90091, 4.06284, -6.07261]");
        Assert.assertEquals(builder.toString(), result);

        // Scilab format
        format = MatrixUtils.SCILAB_FORMAT;
        result = matrix.toString(format);
        builder = new StringBuilder();
        builder.append("SimpleMatrix [0.979, 6.08152, -1.10616; -2.90091, 4.06284, -6.07261]");
        Assert.assertEquals(builder.toString(), result);

        // Summary format
        format = MatrixUtils.SUMMARY_FORMAT;
        result = matrix.toString(format);
        builder = new StringBuilder();
        builder.append("SimpleMatrix[[    0.97900,      6.0815,     -1.1062]");
        builder.append(lineSeparator);
        builder.append("             [    -2.9009,      4.0628,     -6.0726]]");
        builder.append(lineSeparator);
        builder.append("            Rows number : 2\t Columns number : 3");
        Assert.assertEquals(builder.toString(), result);
    }

    /**
     * Tests the methods:<br>
     * {@linkplain AbstractRealMatrix#equals(Object)}<br>
     * {@linkplain AbstractRealMatrix#hashCode()}
     */
    @Test
    public void testEqualsAndHashCode() {
        RealMatrix other;
        double[][] modifiedData;

        final double[][] data = { { 5.10386, -6.70946, 6.04388, 8.08793 },
                { 9.95630, 7.68698, 0.72113, 5.67456 }, { 3.58498, -2.31324, -8.79325, 2.06179 },
                { -9.73792, -0.80867, -2.14529, -8.12200 }, { 7.33858, 4.23093, 3.27284, 9.79652 },
                { -2.58775, -4.43977, -7.45064, -3.50991 } };
        final RealMatrix matrix = new SimpleMatrix(data);

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

        // A single element is different
        for (int i = 0; i < matrix.getRowDimension(); i++) {
            for (int j = 0; j < matrix.getColumnDimension(); j++) {
                // Modified data
                modifiedData = CheckUtils.copyArray(data);
                modifiedData[i][j] = 0.;

                // Matrix of the same type
                other = new SimpleMatrix(modifiedData);
                Assert.assertFalse(matrix.equals(other));
                Assert.assertFalse(other.equals(matrix));
                Assert.assertFalse(matrix.hashCode() == other.hashCode());

                // Matrix of a different type
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
     * {@linkplain AbstractRealMatrix#equals(RealMatrix, double, double)}
     * </p>
     */
    @Test
    public void testEqualsWithTolerances() {
        // Tested matrix
        final double[][] data = { { -8.41942, -0.22899, 9.44679, -3.57354 },
                { 3.13388, 7.21844, 2.13616, -8.38167 }, { 0.35435, -1.08532, 5.62851, -7.78223 },
                { 5.47237, -6.08878, -3.09927, 8.17347 }, { 2.29795, 8.08612, -0.30362, 0.79525 },
                { -1.55144, 4.33268, -8.71018, -7.51458 } };
        final RealMatrix matrix = new SimpleMatrix(data);

        // Test the method
        CheckUtils.checkEqualsWithTolerances(matrix);
    }

    /**
     * Tests the serialization/deserialization.
     */
    @Test
    public void testSerialization() {
        // Tested matrix
        final double[][] data = { { -4.61084, 8.66655, -8.55081, -6.54440 },
                { 6.82577, 4.46169, 4.72610, -2.35097 }, { 0.72531, 8.36855, -9.24632, -0.21435 },
                { 5.27255, -2.58580, -1.69568, 4.70098 }, { 9.33794, 3.54240, 8.55567, 2.34832 },
                { 4.68092, 0.29454, -6.74633, -9.61338 } };
        final SimpleMatrix matrix = new SimpleMatrix(data);

        // Test the serialization
        CheckUtils.checkSerialization(matrix, SimpleMatrix.class);
    }

    /**
     * Tests the method allowing to visit the elements of the matrix without modifying them.
     *
     * <p>
     * Tested methods<br>
     * {@linkplain AbstractRealMatrix#walkInRowOrder(RealMatrixPreservingVisitor)}<br>
     * {@linkplain AbstractRealMatrix#walkInRowOrder(RealMatrixPreservingVisitor, int, int, int, int)}
     * <br>
     * {@linkplain AbstractRealMatrix#walkInColumnOrder(RealMatrixPreservingVisitor)}<br>
     * {@linkplain AbstractRealMatrix#walkInColumnOrder(RealMatrixPreservingVisitor, int, int, int, int)}
     * <br>
     * {@linkplain AbstractRealMatrix#walkInOptimizedOrder(RealMatrixPreservingVisitor)}<br>
     * {@linkplain AbstractRealMatrix#walkInOptimizedOrder(RealMatrixPreservingVisitor, int, int, int, int)}
     * <br>
     * </p>
     */
    @Test
    public void testPreservingVisitor() {
        // Tested matrix
        final double[][] data = { { -5.69092, -0.05147, -9.30622, -2.00998 },
                { -0.05147, 4.99218, -9.53533, -8.40028 },
                { -9.30622, -9.53533, 9.12427, 3.70791 }, { -2.00998, -8.40028, 3.70791, 6.48046 } };
        final SimpleMatrix matrix = new SimpleMatrix(data);

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
     * {@linkplain AbstractRealMatrix#walkInRowOrder(RealMatrixChangingVisitor)}<br>
     * {@linkplain AbstractRealMatrix#walkInRowOrder(RealMatrixChangingVisitor, int, int, int, int)}
     * <br>
     * {@linkplain AbstractRealMatrix#walkInColumnOrder(RealMatrixChangingVisitor)}<br>
     * {@linkplain AbstractRealMatrix#walkInColumnOrder(RealMatrixChangingVisitor, int, int, int, int)}
     * <br>
     * {@linkplain AbstractRealMatrix#walkInOptimizedOrder(RealMatrixChangingVisitor)}<br>
     * {@linkplain AbstractRealMatrix#walkInOptimizedOrder(RealMatrixChangingVisitor, int, int, int, int)}
     * <br>
     * </p>
     */
    @Test
    public void testChangingVisitor() {
        double[][] data;
        RealMatrix matrix;
        RealMatrix expected;

        final int nr = 4;
        final int nc = 3;
        final ChangingVisitor visitor = new ChangingVisitor();

        // Visit the entire matrix
        matrix = new SimpleMatrix(nr, nc);
        matrix.walkInRowOrder(visitor);
        data = new double[][] { { 12, 55, 191 }, { 590, 1794, 5413 }, { 16261, 48812, 146472 },
                { 439443, 1318363, 3955130 } };
        expected = new Array2DRowRealMatrix(data);
        CheckUtils.checkEquality(expected, matrix, ABSTOL, RELTOL);
        Assert.assertEquals(12, visitor.getNbVisited());
        Assert.assertEquals(3955130, visitor.getResult());

        matrix = new SimpleMatrix(nr, nc);
        matrix.walkInColumnOrder(visitor);
        data = new double[][] { { 12, 1729, 141086 }, { 53, 5211, 423289 },
                { 181, 15662, 1269903 }, { 570, 47020, 3809750 } };
        expected = new Array2DRowRealMatrix(data);
        CheckUtils.checkEquality(expected, matrix, ABSTOL, RELTOL);
        Assert.assertEquals(12, visitor.getNbVisited());
        Assert.assertEquals(3809750, visitor.getResult());

        matrix = new SimpleMatrix(nr, nc);
        matrix.walkInOptimizedOrder(visitor);
        data = new double[][] { { 12, 55, 191 }, { 590, 1794, 5413 }, { 16261, 48812, 146472 },
                { 439443, 1318363, 3955130 } };
        expected = new Array2DRowRealMatrix(data);
        CheckUtils.checkEquality(expected, matrix, ABSTOL, RELTOL);
        Assert.assertEquals(12, visitor.getNbVisited());
        Assert.assertEquals(3955130, visitor.getResult());

        // Visit part of the matrix
        matrix = new SimpleMatrix(nr, nc);
        matrix.walkInRowOrder(visitor, 1, 2, 0, 1);
        data = new double[][] { { 0, 0, 0 }, { 17, 75, 0 }, { 247, 770, 0 }, { 0, 0, 0 } };
        expected = new Array2DRowRealMatrix(data);
        CheckUtils.checkEquality(expected, matrix, ABSTOL, RELTOL);
        Assert.assertEquals(4, visitor.getNbVisited());
        Assert.assertEquals(770, visitor.getResult());

        matrix = new SimpleMatrix(nr, nc);
        matrix.walkInColumnOrder(visitor, 1, 2, 0, 1);
        data = new double[][] { { 0, 0, 0 }, { 17, 243, 0 }, { 73, 758, 0 }, { 0, 0, 0 } };
        expected = new Array2DRowRealMatrix(data);
        CheckUtils.checkEquality(expected, matrix, ABSTOL, RELTOL);
        Assert.assertEquals(4, visitor.getNbVisited());
        Assert.assertEquals(758, visitor.getResult());

        matrix = new SimpleMatrix(nr, nc);
        matrix.walkInOptimizedOrder(visitor, 1, 2, 0, 1);
        data = new double[][] { { 0, 0, 0 }, { 17, 75, 0 }, { 247, 770, 0 }, { 0, 0, 0 } };
        expected = new Array2DRowRealMatrix(data);
        CheckUtils.checkEquality(expected, matrix, ABSTOL, RELTOL);
        Assert.assertEquals(4, visitor.getNbVisited());
        Assert.assertEquals(770, visitor.getResult());
    }

    /**
     * Simple real matrix implementation for testing purposes, which only implements the methods
     * that are not already defined by {@linkplain AbstractRealMatrix}.
     */
    private static class SimpleMatrix extends AbstractRealMatrix {
        /** Serial version UID. */
        private static final long serialVersionUID = 7224883390775653391L;

        /** Data array storing the entries of the matrix. */
        private final double[][] data;

        /**
         * Creates a new matrix with the supplied row and column dimensions.
         *
         * @param rowDimension
         *        the number of rows in the matrix
         * @param columnDimension
         *        the number of columns in the matrix
         *
         * @throws NotStrictlyPositiveException
         *         if the row/column dimensions are not strictly positive
         */
        public SimpleMatrix(final int rowDimension, final int columnDimension) {
            super(rowDimension, columnDimension);
            this.data = new double[rowDimension][columnDimension];
        }

        /**
         * Creates a new instance using the provided data array.
         *
         * @param d
         *        the data array containing the entries of the matrix
         */
        public SimpleMatrix(final double[][] d) {
            super(d);
            this.data = MatrixUtils.copyArray(d);
        }

        /**
         * Creates an identity matrix of the specified dimension.
         *
         * @param dim
         *        the dimension of the identity matrix
         *
         * @return the identity matrix built
         */
        public static SimpleMatrix createIdentityMatrix(final int dim) {
            // Check the dimension
            MatrixUtils.checkRowDimension(dim);

            // Set the diagonal elements to 1.0
            final double[][] data = new double[dim][dim];
            for (int i = 0; i < dim; i++) {
                data[i][i] = 1.0;
            }

            // Return the identity matrix
            return new SimpleMatrix(data);
        }

        /** {@inheritDoc} */
        @Override
        public int getRowDimension() {
            int dim = 0;
            if (this.data != null) {
                dim = this.data.length;
            }
            return dim;
        }

        /** {@inheritDoc} */
        @Override
        public int getColumnDimension() {
            int dim = 0;
            if (this.data != null && this.data[0] != null) {
                dim = this.data[0].length;
            }
            return dim;
        }

        /** {@inheritDoc} */
        @Override
        public RealMatrix createMatrix(final int rowDimension, final int columnDimension) {
            return new SimpleMatrix(rowDimension, columnDimension);
        }

        /** {@inheritDoc} */
        @Override
        public RealMatrix copy() {
            final double[][] copy = MatrixUtils.copyArray(this.data);
            return new SimpleMatrix(copy);
        }

        /** {@inheritDoc} */
        @Override
        public double getEntry(final int row, final int column) {
            MatrixUtils.checkMatrixIndex(this, row, column);
            return this.data[row][column];
        }

        /** {@inheritDoc} */
        @Override
        public void setEntry(final int row, final int column, final double value) {
            MatrixUtils.checkMatrixIndex(this, row, column);
            this.data[row][column] = value;
        }
    }

    /**
     * Simple real vector implementation for testing purposes, which only implements the methods
     * that are not already defined by {@linkplain RealVector}.
     */
    private static class SimpleVector extends RealVector {
        /** Vector data. */
        private final double[] data;

        /**
         * Creates a new vector with the supplied dimension.
         *
         * @param dimension
         *        the dimension of the vector
         */
        public SimpleVector(final int dimension) {
            this.data = new double[dimension];
        }

        /**
         * Creates a new vector using the supplied data array.
         *
         * @param v
         *        the data array containing the elements of the vector
         */
        public SimpleVector(final double[] v) {
            this.data = Arrays.copyOf(v, v.length);
        }

        /** {@inheritDoc} */
        @Override
        public int getDimension() {
            return this.data.length;
        }

        /** {@inheritDoc} */
        @Override
        public double getEntry(final int index) {
            return this.data[index];
        }

        /** {@inheritDoc} */
        @Override
        public void setEntry(final int index, final double value) {
            this.data[index] = value;
        }

        /** {@inheritDoc} */
        @Override
        public RealVector append(final RealVector v) {
            final int l0 = this.data.length;
            final int l1 = v.getDimension();
            final double[] vectorData = new double[l0 + l1];
            System.arraycopy(vectorData, 0, this.data, 0, l0);
            for (int i = 0; i < l1; ++i) {
                vectorData[l0 + i] = v.getEntry(i);
            }
            return new SimpleVector(vectorData);
        }

        /** {@inheritDoc} */
        @Override
        public RealVector append(final double d) {
            final int l0 = this.data.length;
            final double[] vectorData = new double[l0 + 1];
            System.arraycopy(vectorData, 0, this.data, 0, l0);
            vectorData[l0] = d;
            return new SimpleVector(vectorData);
        }

        /** {@inheritDoc} */
        @Override
        public RealVector getSubVector(final int index, final int n) {
            final double[] vectorData = new double[n];
            System.arraycopy(vectorData, 0, this.data, index, n);
            return new SimpleVector(vectorData);
        }

        /** {@inheritDoc} */
        @Override
        public void setSubVector(final int index, final RealVector v) {
            final double[] vectorData = v.toArray();
            System.arraycopy(this.data, index, vectorData, 0, vectorData.length);
        }

        /** {@inheritDoc} */
        @Override
        public boolean isNaN() {
            boolean isNaN = false;
            for (int i = 0; i < this.data.length && !isNaN; i++) {
                if (Double.isNaN(this.data[i])) {
                    isNaN |= true;
                }
            }
            return isNaN;
        }

        /** {@inheritDoc} */
        @Override
        public boolean isInfinite() {
            boolean isInfinite = false;
            for (int i = 0; i < this.data.length && !isInfinite; i++) {
                if (!Double.isFinite(this.data[i])) {
                    isInfinite |= true;
                }
            }
            return isInfinite;
        }

        /** {@inheritDoc} */
        @Override
        public RealVector copy() {
            return new SimpleVector(this.data);
        }

        /** {@inheritDoc} */
        @Override
        public RealVector ebeDivide(final RealVector v) {
            checkVectorDimensions(v);
            final int length = this.data.length;
            final double[] out = Arrays.copyOf(this.data, length);
            for (int i = 0; i < length; i++) {
                out[i] /= v.getEntry(i);
            }
            return new SimpleVector(out);
        }

        /** {@inheritDoc} */
        @Override
        public RealVector ebeMultiply(final RealVector v) {
            checkVectorDimensions(v);
            final int length = this.data.length;
            final double[] out = Arrays.copyOf(this.data, length);
            for (int i = 0; i < length; i++) {
                out[i] *= v.getEntry(i);
            }
            return new SimpleVector(out);
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
         * @return the hash-code like number built from the visited elements
         */
        public long getResult() {
            return this.result;
        }
    }
}
