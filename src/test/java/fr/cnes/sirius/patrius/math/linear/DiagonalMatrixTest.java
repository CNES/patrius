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
* VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.7:DM:DM-2766:18/05/2021:Evol. et corr. dans le package fr.cnes.sirius.patrius.math.linear (suite DM 2300) 
 * VERSION:4.6:FA:FA-2542:27/01/2021:[PATRIUS] Definition d'un champ de vue avec demi-angle de 180° 
 * VERSION:4.5.1:FA:FA-2540:04/08/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear
 * VERSION:4.5:DM:DM-2300:27/05/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION:4.3:FA:FA-1996:15/05/2019:[PATRIUS] Erreur de la méthode getSubMatrix avec DiagonalMatrix
 * VERSION::FA:306:14/11/2014: coverage
 * VERSION::DM:482:02/11/2015: tests for method addSym, subtractSym, multiplySym
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.linear;

import java.util.Arrays;
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
import fr.cnes.sirius.patrius.math.exception.NumberIsTooLargeException;
import fr.cnes.sirius.patrius.math.linear.ArrayRowSymmetricMatrix.SymmetryType;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Unit tests for {@code DiagonalMatrix}
 *
 * @author Pierre Seimandi (GMV)
 */
public class DiagonalMatrixTest {

    /** Default absolute tolerance used for the tests. */
    private static final double ABSTOL = 0.;

    /** Default relative tolerance used for the tests. */
    private static final double RELTOL = 1E-14;

    /** Fixed seed for the random number generator. */
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
     * {@linkplain DiagonalMatrix#DiagonalMatrix(int)}
     * </p>
     */
    @Test
    public void testConstructorDimension() {
        final DiagonalMatrix matrix = new DiagonalMatrix(3);
        final RealMatrix expected = new Array2DRowRealMatrix(3, 3);
        CheckUtils.checkEquality(expected, matrix, ABSTOL, RELTOL);
    }

    /**
     * Tests the creation of new instances when the specified dimension is not valid.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain DiagonalMatrix#DiagonalMatrix(int)}
     * </p>
     */
    @Test
    public void testConstructorInvalidDimension() {
        try {
            new DiagonalMatrix(0);
            Assert.fail();
        } catch (final NotStrictlyPositiveException e) {
            final String expectedMessage = "invalid row dimension: 0 (must be positive)";
            Assert.assertEquals(NotStrictlyPositiveException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the creation of new instances by providing the diagonal elements array.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DiagonalMatrix#DiagonalMatrix(double[])}<br>
     * {@linkplain DiagonalMatrix#DiagonalMatrix(double[], boolean)}<br>
     * {@linkplain DiagonalMatrix#getDiagonal()}<br>
     * </p>
     */
    @Test
    public void testConstructorDiagonalElements() {
        DiagonalMatrix matrix;

        final double[] diagonalElements = { 9.76533, -7.61515, 0.86068, 2.65402 };
        final RealMatrix expected = MatrixUtils.createRealDiagonalMatrix(diagonalElements);

        matrix = new DiagonalMatrix(diagonalElements);
        CheckUtils.checkEquality(expected, matrix, ABSTOL, RELTOL);
        Assert.assertNotSame(diagonalElements, matrix.getDataRef());
        Assert.assertTrue(Arrays.equals(diagonalElements, matrix.getDiagonal()));

        matrix = new DiagonalMatrix(diagonalElements, true);
        CheckUtils.checkEquality(expected, matrix, ABSTOL, RELTOL);
        Assert.assertNotSame(diagonalElements, matrix.getDataRef());
        Assert.assertTrue(Arrays.equals(diagonalElements, matrix.getDiagonal()));

        matrix = new DiagonalMatrix(diagonalElements, false);
        CheckUtils.checkEquality(expected, matrix, ABSTOL, RELTOL);
        Assert.assertSame(diagonalElements, matrix.getDataRef());
        Assert.assertTrue(Arrays.equals(diagonalElements, matrix.getDiagonal()));
    }

    /**
     * Tests the creation of new instances when the provided diagonal elements array is {@code null}
     * .
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DiagonalMatrix#DiagonalMatrix(double[])}<br>
     * {@linkplain DiagonalMatrix#DiagonalMatrix(double[], boolean)}<br>
     * </p>
     */
    @Test
    public void testConstructorNullArray() {
        final String expectedMessage = "the supplied array is null";

        try {
            new DiagonalMatrix(null);
            Assert.fail();
        } catch (final NullArgumentException e) {
            Assert.assertEquals(NullArgumentException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            new DiagonalMatrix(null, false);
            Assert.fail();
        } catch (final NullArgumentException e) {
            Assert.assertEquals(NullArgumentException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            new DiagonalMatrix(null, true);
            Assert.fail();
        } catch (final NullArgumentException e) {
            Assert.assertEquals(NullArgumentException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the creation of new instances when the provided diagonal elements array is empty.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DiagonalMatrix#DiagonalMatrix(double[])}<br>
     * {@linkplain DiagonalMatrix#DiagonalMatrix(double[], boolean)}<br>
     * </p>
     */
    @Test
    public void testConstructorEmptyArray() {
        final String expectedMessage = "the supplied array is empty";

        try {
            new DiagonalMatrix(new double[0]);
            Assert.fail();
        } catch (final NoDataException e) {
            Assert.assertEquals(NoDataException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            new DiagonalMatrix(new double[0], false);
            Assert.fail();
        } catch (final NoDataException e) {
            Assert.assertEquals(NoDataException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            new DiagonalMatrix(new double[0], true);
            Assert.fail();
        } catch (final NoDataException e) {
            Assert.assertEquals(NoDataException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the creation of new matrices of the same type by specifying the row and column
     * dimensions.<br>
     * An exception should be thrown when the specified row and columns dimensions are not equal.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DiagonalMatrix#createMatrix(int, int)}
     * </p>
     */
    @Test
    public void testCreateMatrix() {
        final int dim = 4;
        final DiagonalMatrix matrix = new DiagonalMatrix(dim);
        final DiagonalMatrix result = matrix.createMatrix(dim, dim);

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
     * {@linkplain DiagonalMatrix#createIdentityMatrix(int)}
     */
    @Test
    public void testCreateIdentityMatrix() {
        for (int dim = 1; dim <= 10; dim++) {
            final DiagonalMatrix matrix = DiagonalMatrix.createIdentityMatrix(dim);
            final RealMatrix expected = MatrixUtils.createRealIdentityMatrix(dim);
            CheckUtils.checkEquality(expected, matrix, ABSTOL, RELTOL);
        }

        // Invalid dimension
        try {
            DiagonalMatrix.createIdentityMatrix(0);
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
     * {@linkplain DiagonalMatrix#isSquare()}
     * </p>
     */
    @Test
    public void testIsSquare() {
        DiagonalMatrix matrix;

        // Matrix filled with zeros
        matrix = new DiagonalMatrix(8);
        Assert.assertTrue(matrix.isSquare());

        // Standard diagonal matrix
        final double[] diagonalElements = { 2.03886, -2.03111, 3.09894, 4.38106 };
        matrix = new DiagonalMatrix(diagonalElements);
        Assert.assertTrue(matrix.isSquare());
    }

    /**
     * Tests the methods checking if a matrix is diagonal.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DiagonalMatrix#isDiagonal(double)}<br>
     * </p>
     */
    @Test
    public void testIsDiagonal() {
        DiagonalMatrix matrix;

        // Matrix filled with zeros
        matrix = new DiagonalMatrix(8);
        Assert.assertTrue(matrix.isDiagonal(0.));

        // Standard diagonal matrix
        final double[] diagonalElements = { -0.72572, -7.93782, 6.78128, 3.57487 };
        matrix = new DiagonalMatrix(diagonalElements);
        Assert.assertTrue(matrix.isDiagonal(0.));
    }

    /**
     * Tests the methods checking if a matrix is symmetric.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DiagonalMatrix#isSymmetric()}<br>
     * {@linkplain DiagonalMatrix#isSymmetric(double)}<br>
     * {@linkplain DiagonalMatrix#isSymmetric(double, double)}<br>
     * </p>
     */
    @Test
    public void testIsSymmetric() {
        DiagonalMatrix matrix;

        // Matrix filled with zeros
        matrix = new DiagonalMatrix(8);
        Assert.assertTrue(matrix.isSymmetric());
        Assert.assertTrue(matrix.isSymmetric(0.));
        Assert.assertTrue(matrix.isSymmetric(0., 0.));

        // Standard diagonal matrix
        final double[] diagonalElements = { 5.87596, -2.92076, 2.88312, -9.91342 };
        matrix = new DiagonalMatrix(diagonalElements);
        Assert.assertTrue(matrix.isSymmetric());
        Assert.assertTrue(matrix.isSymmetric(0.));
        Assert.assertTrue(matrix.isSymmetric(0., 0.));
    }

    /**
     * Tests the method checking if a matrix is antisymmetric.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain DiagonalMatrix#isAntisymmetric(double, double)}
     * </p>
     */
    @Test
    public void testIsAntisymmetric() {
        DiagonalMatrix matrix;

        // Matrix filled with zeros
        matrix = new DiagonalMatrix(8);
        Assert.assertTrue(matrix.isAntisymmetric(0., 0.));

        // Standard diagonal matrix
        final double[] diagonalElements = { 1E-10, 1E-7, 1E-9 };
        matrix = new DiagonalMatrix(diagonalElements);

        // Only the absolute tolerance is provided
        Assert.assertFalse(matrix.isAntisymmetric(1E-10));
        Assert.assertFalse(matrix.isAntisymmetric(1E-9));
        Assert.assertTrue(matrix.isAntisymmetric(1E-7));

        // The relative and absolute tolerances are both provided
        // The relative tolerance should not have any impact
        Assert.assertFalse(matrix.isAntisymmetric(0., 0.));
        Assert.assertFalse(matrix.isAntisymmetric(0., 1E-10));
        Assert.assertFalse(matrix.isAntisymmetric(0., 1E-9));
        Assert.assertTrue(matrix.isAntisymmetric(0., 1E-7));
        Assert.assertFalse(matrix.isAntisymmetric(Double.POSITIVE_INFINITY, 0.));
        Assert.assertFalse(matrix.isAntisymmetric(Double.POSITIVE_INFINITY, 1E-10));
        Assert.assertFalse(matrix.isAntisymmetric(Double.POSITIVE_INFINITY, 1E-9));
        Assert.assertTrue(matrix.isAntisymmetric(Double.POSITIVE_INFINITY, 1E-7));
    }

    /**
     * Tests the method checking if a matrix is orthogonal.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain DiagonalMatrix#isOrthogonal(double, double)}
     * </p>
     */
    @Test
    public void testIsOrthogonal() {
        DiagonalMatrix matrix;
        double[] diagonalElements;

        // Matrix filled with zeros
        matrix = new DiagonalMatrix(8);
        Assert.assertFalse(matrix.isOrthogonal(0., 0.));

        // Perfectly orthogonal diagonal matrix
        diagonalElements = new double[] { +1., +1, -1., -1. };
        final DiagonalMatrix identityMatrix = new DiagonalMatrix(diagonalElements);
        Assert.assertTrue(identityMatrix.isOrthogonal(0., 0.));

        // Slightly perturbed orthogonal diagonal matrix
        diagonalElements = new double[] { +1E-10, -1E-7, +1E-9, -1E-8 };
        final DiagonalMatrix perturbationMatrix = new DiagonalMatrix(diagonalElements);
        matrix = identityMatrix.add(perturbationMatrix);
        Assert.assertFalse(matrix.isOrthogonal(0., 0.));
        Assert.assertFalse(matrix.isOrthogonal(1E-7, 1E-10));
        Assert.assertFalse(matrix.isOrthogonal(1E-7, 1E-9));
        Assert.assertTrue(matrix.isOrthogonal(1E-7, 1E-7));
        Assert.assertFalse(matrix.isOrthogonal(1E-9, 1E-7));
        Assert.assertFalse(matrix.isOrthogonal(1E-10, 1E-7));
    }

    /**
     * Tests the methods checking if a matrix is singular.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DiagonalMatrix#isSingular()}<br>
     * {@linkplain DiagonalMatrix#isSingular(double)}<br>
     * </p>
     */
    @Test
    public void testIsSingular() {
        DiagonalMatrix matrix;

        // Matrix filled with zeros
        matrix = new DiagonalMatrix(8);
        Assert.assertTrue(matrix.isSingular(0.));

        // Diagonal matrix with diagonal elements close to zero
        final double[] diagonalElements = { 1E-10, 1E-7, 1E-9 };
        matrix = new DiagonalMatrix(diagonalElements);
        Assert.assertFalse(matrix.isSingular(1E-10 - MathLib.ulp(1E-10)));
        Assert.assertTrue(matrix.isSingular(1E-10));
    }

    /**
     * Tests the methods checking if a matrix is invertible.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DiagonalMatrix#isInvertible()}<br>
     * {@linkplain DiagonalMatrix#isInvertible(double)}<br>
     * </p>
     */
    @Test
    public void testIsInvertible() {
        DiagonalMatrix matrix;

        // Matrix filled with zeros
        matrix = new DiagonalMatrix(8);
        Assert.assertFalse(matrix.isInvertible());
        Assert.assertFalse(matrix.isInvertible(0.));

        // Diagonal matrix with diagonal elements close to zero
        final double[] diagonalElements = { 1E-10, 1E-7, 1E-9 };
        matrix = new DiagonalMatrix(diagonalElements);
        Assert.assertTrue(matrix.isInvertible());
        Assert.assertTrue(matrix.isInvertible(1E-10 - MathLib.ulp(1E-10)));
        Assert.assertFalse(matrix.isInvertible(1E-10));
    }

    /**
     * Tests the method that returns the minimum value in the matrix.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain DiagonalMatrix#getMin()}
     * </p>
     */
    @Test
    public void testGetMin() {
        double[] diagonalElements;
        DiagonalMatrix matrix;

        // 1x1 matrix and the only element is negative
        diagonalElements = new double[] { -8.95496 };
        matrix = new DiagonalMatrix(diagonalElements);
        CheckUtils.checkMinimum(matrix, 0., 0.);
        Assert.assertEquals(-8.95496, matrix.getMin(), 0.);

        // 1x1 matrix and the only element is positive
        diagonalElements = new double[] { 2.53576 };
        matrix = new DiagonalMatrix(diagonalElements);
        CheckUtils.checkMinimum(matrix, 0., 0.);
        Assert.assertEquals(2.53576, matrix.getMin(), 0.);

        // Matrix containing negative elements
        diagonalElements = new double[] { -9.27826, -7.13773, 0.74244, 8.46577 };
        matrix = new DiagonalMatrix(diagonalElements);
        CheckUtils.checkMinimum(matrix, 0., 0.);
        Assert.assertEquals(-9.27826, matrix.getMin(), 0.);

        // Matrix containing only positive elements
        // (to make sure the minimum found is zero)
        diagonalElements = new double[] { 7.84384, 1.2285, 5.06068, 3.37446 };
        matrix = new DiagonalMatrix(diagonalElements);
        CheckUtils.checkMinimum(matrix, 0., 0.);
        Assert.assertEquals(0., matrix.getMin(), 0.);
    }

    /**
     * Tests the method that returns the maximum value in the matrix.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain DiagonalMatrix#getMax()}
     * </p>
     */
    @Test
    public void testGetMax() {
        double[] diagonalElements;
        DiagonalMatrix matrix;

        // 1x1 matrix and the only element is negative
        diagonalElements = new double[] { -6.91989 };
        matrix = new DiagonalMatrix(diagonalElements);
        CheckUtils.checkMaximum(matrix, 0., 0.);
        Assert.assertEquals(-6.91989, matrix.getMax(), 0.);

        // 1x1 matrix and the only element is positive
        diagonalElements = new double[] { 5.32576 };
        matrix = new DiagonalMatrix(diagonalElements);
        CheckUtils.checkMaximum(matrix, 0., 0.);
        Assert.assertEquals(5.32576, matrix.getMax(), 0.);

        // Matrix containing positive elements
        diagonalElements = new double[] { -1.14788, 8.75234, -3.05683, 8.04744 };
        matrix = new DiagonalMatrix(diagonalElements);
        CheckUtils.checkMaximum(matrix, 0., 0.);
        Assert.assertEquals(8.75234, matrix.getMax(), 0.);

        // Matrix containing only negative elements
        // (to make sure the minimum found is zero)
        diagonalElements = new double[] { -2.61399, -4.02865, -7.95165, -5.36487 };
        matrix = new DiagonalMatrix(diagonalElements);
        CheckUtils.checkMaximum(matrix, 0., 0.);
        Assert.assertEquals(0., matrix.getMax(), 0.);
    }

    /**
     * Tests the method that computes the trace of the matrix.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain DiagonalMatrix#getTrace()}
     * </p>
     */
    @Test
    public void testGetTrace() {
        // Tested matrix
        final double[] diagonalElements = { -8.64497, 8.61244, -2.40814, -8.54476 };
        final DiagonalMatrix matrix = new DiagonalMatrix(diagonalElements);

        // Test the method
        CheckUtils.checkTrace(matrix, 0., 0.);
        CheckUtils.checkEquality(-10.98543, matrix.getTrace(), ABSTOL, RELTOL);
    }

    /**
     * Tests the method that computes the norm of the matrix (maximum absolute row sum).
     *
     * <p>
     * Tested method:<br>
     * {@linkplain DiagonalMatrix#getNorm()}
     * </p>
     */
    @Test
    public void testGetNorm() {
        // Tested matrix
        final double[] diagonalElements = { -7.83803, -2.97830, -2.50544, 3.51246 };
        final DiagonalMatrix matrix = new DiagonalMatrix(diagonalElements);

        // Test the method
        CheckUtils.checkNorm(matrix, ABSTOL, RELTOL);
        CheckUtils.checkEquality(7.83803, matrix.getNorm(), ABSTOL, RELTOL);
    }

    /**
     * Tests the method that computes the Frobenius norm of the matrix.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain DiagonalMatrix#getFrobeniusNorm()}
     * </p>
     */
    @Test
    public void testGetFrobeniusNorm() {
        // Tested matrix
        final double[] diagonalElements = { 9.78227, -9.47648, 9.89324, -2.81067 };
        final DiagonalMatrix matrix = new DiagonalMatrix(diagonalElements);

        // Test the method
        CheckUtils.checkFrobeniusNorm(matrix, ABSTOL, RELTOL);
        CheckUtils.checkEquality(17.06670861911575, matrix.getFrobeniusNorm(), ABSTOL, RELTOL);
    }

    /**
     * Tests the methods that return the entries of the matrix in a 2D data array.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DiagonalMatrix#getData()}<br>
     * {@linkplain DiagonalMatrix#getData(boolean)}<br>
     * </p>
     */
    @Test
    public void testGetData() {
        final double[] diagonalElements = { -8.53920, -9.87767, -6.36686, -0.90119 };
        final DiagonalMatrix matrix = new DiagonalMatrix(diagonalElements);

        final int dim = diagonalElements.length;
        final double[][] expectedData = new double[dim][dim];
        for (int i = 0; i < dim; i++) {
            expectedData[i][i] = diagonalElements[i];
        }

        Assert.assertArrayEquals(expectedData, matrix.getData());
        Assert.assertArrayEquals(expectedData, matrix.getData(true));
        Assert.assertArrayEquals(expectedData, matrix.getData(false));
    }

    /**
     * Tests the method:<br>
     * {@linkplain DiagonalMatrix#getDataRef()}
     */
    @Test
    public void testGetDataRef() {
        double[] data;
        DiagonalMatrix matrix;
        final double[] diagonalElements = { -8.51893, 3.49242, -3.29484, -2.90062 };

        // The diagonal elements array is copied
        matrix = new DiagonalMatrix(diagonalElements);
        data = matrix.getDataRef();
        Assert.assertArrayEquals(diagonalElements, data, 0.);
        Assert.assertNotSame(diagonalElements, data);

        matrix = new DiagonalMatrix(diagonalElements, true);
        data = matrix.getDataRef();
        Assert.assertArrayEquals(diagonalElements, data, 0.);
        Assert.assertNotSame(diagonalElements, data);

        // The diagonal elements array is passed by reference
        matrix = new DiagonalMatrix(diagonalElements, false);
        data = matrix.getDataRef();
        Assert.assertArrayEquals(diagonalElements, data, 0.);
        Assert.assertSame(diagonalElements, data);
    }

    /**
     * Tests the method that performs a deep copy of the matrix.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain DiagonalMatrix#copy()}
     * </p>
     */
    @Test
    public void testCopy() {
        final double[] diagonalElements = { -6.22017, 8.41628, 2.26647, -8.14450 };
        final DiagonalMatrix matrix = new DiagonalMatrix(diagonalElements);

        final DiagonalMatrix result = matrix.copy();
        CheckUtils.checkEquality(result, matrix, 0., 0.);
        Assert.assertNotSame(matrix, result);
        Assert.assertNotSame(matrix.getDataRef(), result.getDataRef());
        Assert.assertEquals(DiagonalMatrix.class, result.getClass());
    }

    /**
     * Tests the methods that return the transpose of the matrix.<br>
     * The transpose matrix should be the same as the initial matrix, stored in a new instance
     * unless specified otherwise.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DiagonalMatrix#transpose()}<br>
     * {@linkplain DiagonalMatrix#transpose(boolean)}<br>
     * </p>
     */
    @Test
    public void testTranspose() {
        // Tested matrix
        final double[] diagonalElements = { 1.38883, 7.24855, -3.42540, 1.77483 };
        final DiagonalMatrix matrix = new DiagonalMatrix(diagonalElements);

        // Test the method
        CheckUtils.checkTranspose(matrix);
    }

    /**
     * Tests the method that raises the matrix to the power of N.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain DiagonalMatrix#power(int)}<br>
     * </p>
     */
    @Test
    public void testPower() {
        // Tested matrix
        final double[] diagonalElements = { -2.12672, 5.08882, -0.70264, -2.53886 };
        final DiagonalMatrix matrix = new DiagonalMatrix(diagonalElements);

        // Test the method
        CheckUtils.checkPower(matrix, 0, 100, DiagonalMatrix.class, ABSTOL, RELTOL);
        CheckUtils.checkPowerNegativeExponent(matrix);
    }

    /**
     * Tests the method that returns an element of the matrix.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain DiagonalMatrix#getEntry(int, int)}
     * </p>
     */
    @Test
    public void testGetEntry() {
        // Tested matrix
        final double[] diagonalElements = { 4.83407, 0.12269, -3.66427, 5.52965 };
        final DiagonalMatrix matrix = new DiagonalMatrix(diagonalElements);

        // Test the method
        CheckUtils.checkGetEntry(matrix);
    }

    /**
     * Tests the method that returns an element of the matrix, using indices outside the valid index
     * range.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain DiagonalMatrix#getEntry(int, int)}
     * </p>
     */
    @Test
    public void testGetOutOfRangeEntry() {
        final DiagonalMatrix matrix = new DiagonalMatrix(3);
        CheckUtils.checkGetOutOfRangeEntry(matrix);
    }

    /**
     * Tests the method that sets an entry to a new value, for diagonal elements.<br>
     * No exception should be thrown.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain DiagonalMatrix#setEntry(int, int, double)}
     * </p>
     */
    @Test
    public void testSetDiagonalEntry() {
        // Tested Matrix
        final double[] diagonalElements = { -0.03898, 5.08983, -3.47904, 1.08039 };
        final DiagonalMatrix matrix = new DiagonalMatrix(diagonalElements);

        for (int i = 0; i < 3; i++) {
            // Set the diagonal element to a non-zero value
            // No exception should be thrown
            matrix.setEntry(i, i, diagonalElements[i]);
            Assert.assertEquals(diagonalElements[i], matrix.getEntry(i, i), 0.);
        }
    }

    /**
     * Tests the method that sets an entry to a new value, for off-diagonal elements.<br>
     * An exception should be thrown if the value is not equal to zero.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain DiagonalMatrix#setEntry(int, int, double)}
     * </p>
     */
    @Test
    public void testSetOffDiagonalEntry() {
        final int dim = 3;
        final DiagonalMatrix matrix = new DiagonalMatrix(dim);

        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < dim; j++) {
                // Set the element to zero
                // No exception should be thrown
                matrix.setEntry(i, j, 0.);

                if (i != j) {
                    // Set the diagonal element to a non-zero value
                    // An exception should be thrown, as this is forbidden
                    try {
                        matrix.setEntry(i, j, 2 * Double.MIN_VALUE);
                        Assert.fail();
                    } catch (final NumberIsTooLargeException e) {
                        Assert.assertEquals(NumberIsTooLargeException.class, e.getClass());
                        Assert.assertEquals("0 is larger than the maximum (0)", e.getMessage());
                    }
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
     * {@linkplain DiagonalMatrix#setEntry(int, int, double)}
     * </p>
     */
    @Test
    public void testSetOutOfRangeEntry() {
        final DiagonalMatrix matrix = new DiagonalMatrix(3);
        CheckUtils.checkSetOutOfRangeEntry(matrix);
    }

    /**
     * Tests the method that adds a scalar to an entry, for diagonal entries.<br>
     * No exception should be thrown.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain DiagonalMatrix#addToEntry(int, int, double)}
     * </p>
     */
    @Test
    public void testAddToDiagonalEntry() {
        final int dim = 3;
        final DiagonalMatrix matrix = new DiagonalMatrix(dim);

        for (int i = 0; i < dim; i++) {
            final double d = CheckUtils.getRandomNumber(RNG, -10., +10.);
            matrix.addToEntry(i, i, d);
            Assert.assertEquals(d, matrix.getEntry(i, i), 0.);
        }
    }

    /**
     * Tests the method that adds a scalar to an entry, for off-diagonal entries.<br>
     * An exception should be thrown if the value is not equal to zero.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain DiagonalMatrix#addToEntry(int, int, double)}
     * </p>
     */
    @Test
    public void testAddToOffDiagonalEntry() {
        final int dim = 3;
        final DiagonalMatrix matrix = new DiagonalMatrix(dim);

        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < dim; j++) {
                // Add zero to the element
                // No exception should be thrown
                matrix.addToEntry(i, j, 0.);

                if (i != j) {
                    // Add a non-zero value to the off-diagonal element
                    // An exception should be thrown, as this is forbidden
                    try {
                        matrix.addToEntry(i, j, 2 * Double.MIN_VALUE);
                        Assert.fail();
                    } catch (final NumberIsTooLargeException e) {
                        Assert.assertEquals(NumberIsTooLargeException.class, e.getClass());
                        Assert.assertEquals("0 is larger than the maximum (0)", e.getMessage());
                    }
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
     * {@linkplain DiagonalMatrix#addToEntry(int, int, double)}
     * </p>
     */
    @Test
    public void testAddToOutOfRangeEntry() {
        final DiagonalMatrix matrix = new DiagonalMatrix(3);
        CheckUtils.checkAddToOutOfRangeEntry(matrix);
    }

    /**
     * Tests the method that multiplies an entry by a scalar, for diagonal and off-diagonal entries.<br>
     * No exception should be thrown.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain DiagonalMatrix#multiplyEntry(int, int, double)}
     * </p>
     */
    @Test
    public void testMultiplyEntry() {
        final int dim = 3;
        final DiagonalMatrix matrix = DiagonalMatrix.createIdentityMatrix(dim);

        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < dim; j++) {
                final double d = CheckUtils.getRandomNumber(RNG, -10., +10.);
                matrix.multiplyEntry(i, j, d);

                if (i == j) {
                    Assert.assertEquals(d, matrix.getEntry(i, j), 0.);
                } else {
                    Assert.assertEquals(0., matrix.getEntry(i, j), 0.);
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
     * {@linkplain DiagonalMatrix#multiplyEntry(int, int, double)}
     * </p>
     */
    @Test
    public void testMultiplyOutOfRangeEntry() {
        final DiagonalMatrix matrix = DiagonalMatrix.createIdentityMatrix(3);
        CheckUtils.checkAddToOutOfRangeEntry(matrix);
    }

    /**
     * Tests the method that adds a scalar to the entries of the matrix.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain DiagonalMatrix#scalarAdd(double)}
     * </p>
     */
    @Test
    public void testScalarAdd() {
        // Tested matrix
        final double[] diagonalElements = { -4.19573, 8.37636, 1.67580, -3.71605 };
        final DiagonalMatrix matrix = new DiagonalMatrix(diagonalElements);

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
     * {@linkplain DiagonalMatrix#scalarMultiply(double)}
     * </p>
     */
    @Test
    public void testScalarMultiply() {
        // Tested matrix
        final double[] diagonalElements = { 8.61905, -0.80505, -9.67916, 7.72788 };
        final DiagonalMatrix matrix = new DiagonalMatrix(diagonalElements);

        // Test the method
        CheckUtils.checkScalarMultiply(matrix, 0.0, DiagonalMatrix.class, ABSTOL, RELTOL);
        CheckUtils.checkScalarMultiply(matrix, 1.0, DiagonalMatrix.class, ABSTOL, RELTOL);
        CheckUtils.checkScalarMultiply(matrix, +2.838, DiagonalMatrix.class, ABSTOL, RELTOL);
        CheckUtils.checkScalarMultiply(matrix, -4.278, DiagonalMatrix.class, ABSTOL, RELTOL);
    }

    /**
     * Tests the methods that add another matrix.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DiagonalMatrix#add(RealMatrix)}<br>
     * {@linkplain DiagonalMatrix#add(SymmetricMatrix)}<br>
     * {@linkplain DiagonalMatrix#add(DiagonalMatrix)}<br>
     * </p>
     */
    @Test
    public void testAddMatrix() {
        // Tested Matrix
        final double[] initialData = { -6.94266, 7.40248, -4.09155, 5.79277 };
        final DiagonalMatrix matrix = new DiagonalMatrix(initialData);

        // Matrices to be added
        final double[] diagonalElements = { 3.62229, 8.93551, 0.89479, 4.21333 };
        final double[][] data = { { 5.84982, -0.23551, 0.18355, 0.28474 },
                { -0.23551, 6.13713, -0.81283, -0.63487 }, { 0.18355, -0.81283, 7.85359, 1.17726 },
                { 0.28474, -0.63487, 1.17726, 6.55094 } };

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
        CheckUtils.checkAddMatrix(matrix, diagonalMatrix, DiagonalMatrix.class, ABSTOL, RELTOL);
    }

    /**
     * Tests the methods that add another matrix, using a matrix with incompatible dimensions.<br>
     * An exception should be thrown.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DiagonalMatrix#add(RealMatrix)}<br>
     * {@linkplain DiagonalMatrix#add(SymmetricMatrix)}<br>
     * {@linkplain DiagonalMatrix#add(DiagonalMatrix)}<br>
     * </p>
     */
    @Test
    public void testAddIncompatibleMatrix() {
        final DiagonalMatrix matrix = new DiagonalMatrix(3);
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
     * {@linkplain DiagonalMatrix#subtract(RealMatrix)}<br>
     * {@linkplain DiagonalMatrix#subtract(SymmetricMatrix)}<br>
     * {@linkplain DiagonalMatrix#subtract(DiagonalMatrix)}<br>
     * </p>
     */
    @Test
    public void testSubtractMatrix() {
        // Tested Matrix
        final double[] initialData = { -4.33528, -8.94228, -4.85727, 0.81650 };
        final DiagonalMatrix matrix = new DiagonalMatrix(initialData);

        // Matrices to be subtracted
        final double[] diagonalElements = { -4.73799, 9.43237, -7.06995, 1.48683 };
        final double[][] data = { { 1.54377, -1.08798, -1.27062, -0.56702 },
                { -1.08798, 6.64827, 1.71166, 1.33115 }, { -1.27062, 1.71166, 3.06154, -1.75460 },
                { -0.56702, 1.33115, -1.75460, 7.33531 } };

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
        CheckUtils
                .checkSubtractMatrix(matrix, diagonalMatrix, DiagonalMatrix.class, ABSTOL, RELTOL);
    }

    /**
     * Tests the methods that subtract another matrix, using a matrix with incompatible dimensions.<br>
     * An exception should be thrown.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DiagonalMatrix#subtract(RealMatrix)}<br>
     * {@linkplain DiagonalMatrix#subtract(SymmetricMatrix)}<br>
     * {@linkplain DiagonalMatrix#subtract(DiagonalMatrix)}<br>
     * </p>
     */
    @Test
    public void testSubtractIncompatibleMatrix() {
        final DiagonalMatrix matrix = new DiagonalMatrix(3);
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
     * {@linkplain DiagonalMatrix#multiply(RealMatrix)}<br>
     * {@linkplain DiagonalMatrix#multiply(RealMatrix, boolean)}<br>
     * {@linkplain DiagonalMatrix#multiply(RealMatrix, boolean, double)}<br>
     * {@linkplain DiagonalMatrix#multiply(DiagonalMatrix, double)}<br>
     * </p>
     */
    @Test
    public void testMultiplyBySquareMatrix() {
        // Tested Matrix
        final double[] initialData = { -3.68081, 8.40667, 4.60508, -4.18247 };
        final DiagonalMatrix matrix = new DiagonalMatrix(initialData);

        // Matrices to multiply by
        final double[] diagonalElements = { -2.77872, 8.21764, -2.30709, -7.57104 };
        final double[][] data = { { 3.20432, -0.44211, 0.24625, 0.34356 },
                { -0.44211, 2.97929, 0.12559, 0.07947 }, { 0.24625, 0.12559, 2.90354, -0.88252 },
                { 0.34356, 0.07947, -0.88252, 2.37194 } };

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
        CheckUtils.checkMultiplyMatrix(matrix, diagonalMatrix, scalingFactor, DiagonalMatrix.class,
                ABSTOL, RELTOL);
    }

    /**
     * Tests the methods that postmultiply the matrix by another matrix, using a rectangular matrix.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DiagonalMatrix#multiply(RealMatrix)}<br>
     * {@linkplain DiagonalMatrix#multiply(RealMatrix, boolean)}<br>
     * {@linkplain DiagonalMatrix#multiply(RealMatrix, boolean, double)}<br>
     * </p>
     */
    @Test
    public void testMultiplyByRectangularMatrix() {
        // Tested Matrix
        final double[] initialData = { 7.69579, -4.56706, -9.31884, 7.86330 };
        final DiagonalMatrix matrix = new DiagonalMatrix(initialData);

        // Matrices to multiply by
        final double[][] data = { { -3.96944, 8.85561, 9.44603, -5.90960, -3.41094, -4.16636 },
                { -0.46497, 3.58099, -9.73042, 5.30965, 0.31839, -3.78434 },
                { -1.80212, 5.25001, 9.63835, -9.69400, -0.37860, -3.42618 },
                { 6.22641, -8.33688, 8.79181, -4.35656, -2.72401, 6.55978 } };
        final RealMatrix realMatrix = new Array2DRowRealMatrix(data);

        // Scaling factor
        final double scalingFactor = 7.689;

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
     * {@linkplain DiagonalMatrix#multiply(RealMatrix)}<br>
     * {@linkplain DiagonalMatrix#multiply(RealMatrix, boolean)}<br>
     * {@linkplain DiagonalMatrix#multiply(RealMatrix, boolean, double)}<br>
     * </p>
     */
    @Test
    public void testMultiplyByIncompatibleMatrix() {
        final DiagonalMatrix matrix = new DiagonalMatrix(3);
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
     * {@linkplain DiagonalMatrix#preMultiply(RealMatrix)}
     * </p>
     */
    @Test
    public void testPreMultiplyBySquareMatrix() {
        // Tested Matrix
        final double[] initialData = { -1.02177, -4.84000, -5.28850, -8.68528 };
        final DiagonalMatrix matrix = new DiagonalMatrix(initialData);

        // Matrices to premultiply by
        final double[] diagonalElements = { -0.11286, -8.31784, 8.43715, 9.97626 };
        final double[][] data = { { 1.49345, 1.12969, 0.69234, -0.32664 },
                { 1.12969, 2.55593, -0.28901, -0.71445 }, { 0.69234, -0.28901, 3.13919, -1.10450 },
                { -0.32664, -0.71445, -1.10450, 1.02784 } };

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
        CheckUtils.checkPreMultiplyMatrix(matrix, diagonalMatrix, DiagonalMatrix.class, ABSTOL,
                RELTOL);
    }

    /**
     * Tests the method that premultiplies the matrix by another matrix, using a rectangular matrix.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain DiagonalMatrix#preMultiply(RealMatrix)}<br>
     * </p>
     */
    @Test
    public void testPreMultiplyByRectangularMatrix() {
        // Tested Matrix
        final double[] initialData = { -5.61644, -8.66195, 2.41630, 5.28040 };
        final DiagonalMatrix matrix = new DiagonalMatrix(initialData);

        // Matrices to premultiply by
        final double[][] data = { { -4.22003, 7.43750, -2.76009, -8.87606 },
                { -2.45462, -6.40845, 3.76523, 8.45756 }, { -0.88478, 2.79785, 3.76542, -8.54444 },
                { 1.10619, 9.59563, 1.85648, 5.64305 }, { -5.24656, -8.78087, 3.90272, -0.66987 },
                { -1.07541, -1.02209, -0.15807, 1.45516 } };
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
     * {@linkplain DiagonalMatrix#preMultiply(RealMatrix)}
     * </p>
     */
    @Test
    public void testPreMultiplyByIncompatibleMatrix() {
        final DiagonalMatrix matrix = new DiagonalMatrix(3);
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
     * {@linkplain DiagonalMatrix#operate(double[])}<br>
     * {@linkplain DiagonalMatrix#operate(RealVector)}<br>
     * </p>
     */
    @Test
    public void testOperateVector() {
        // Tested matrix
        final double[] diagonalElements = { -3.44600, -7.37042, 7.82869, 7.03354 };
        final DiagonalMatrix matrix = new DiagonalMatrix(diagonalElements);

        // Vector to multiply the tested matrix by
        final double[] vectorData = { -5.16528, 6.03763, 3.28495, -3.50725 };
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
     * {@linkplain DiagonalMatrix#operate(double[])}<br>
     * {@linkplain DiagonalMatrix#operate(RealVector)}<br>
     * </p>
     */
    @Test
    public void testOperateIncompatibleVector() {
        final DiagonalMatrix matrix = new DiagonalMatrix(3);
        CheckUtils.checkOperateIncompatibleVector(matrix);
    }

    /**
     * Tests the methods that premultiply the matrix by a vector.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DiagonalMatrix#preMultiply(double[])}<br>
     * {@linkplain DiagonalMatrix#preMultiply(RealVector)}<br>
     * </p>
     */
    @Test
    public void testPreMultiplyByVector() {
        // Tested matrix
        final double[] diagonalElements = { -5.14973, 1.35956, 3.35600, -7.56912 };
        final DiagonalMatrix matrix = new DiagonalMatrix(diagonalElements);

        // Vector to premultiply the tested matrix by
        final double[] vectorData = { -0.56463, 3.76982, 2.39786, -2.30677 };
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
     * {@linkplain DiagonalMatrix#preMultiply(double[])}<br>
     * {@linkplain DiagonalMatrix#preMultiply(RealVector)}<br>
     * </p>
     */
    @Test
    public void testPreMultiplyByIncompatibleVector() {
        final DiagonalMatrix matrix = new DiagonalMatrix(3);
        CheckUtils.checkPreMultiplyIncompatibleVector(matrix);
    }

    /**
     * Tests the quadratic multiplication.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DiagonalMatrix#quadraticMultiplication(RealMatrix)}<br>
     * {@linkplain DiagonalMatrix#quadraticMultiplication(RealMatrix, boolean)}<br>
     * </p>
     */
    @Test
    public void testQuadraticMultiplicationSquareMatrix() {
        // Tested matrix
        final double[] diagonalElements = { 5.06203, 0.89006, 4.77144, 3.76869 };
        final DiagonalMatrix matrix = new DiagonalMatrix(diagonalElements);

        // Square matrix used for the quadratic multiplication
        final double[][] data = { { 0.46528, -3.32941, -5.19177, -9.91891 },
                { 3.22390, -5.80765, 2.66210, -0.23485 }, { -2.63616, 1.70798, -5.32601, 7.14250 },
                { -0.06331, 4.25446, 8.98565, -0.99088 } };
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
     * {@linkplain DiagonalMatrix#quadraticMultiplication(RealMatrix)}<br>
     * {@linkplain DiagonalMatrix#quadraticMultiplication(RealMatrix, boolean)}<br>
     * </p>
     */
    @Test
    public void testQuadraticMultiplicationRectangularMatrix() {
        // Tested matrix
        final double[] diagonalElements = { 5.06203, 0.89006, 4.77144, 3.76869 };
        final DiagonalMatrix matrix = new DiagonalMatrix(diagonalElements);

        // Rectangular matrix used for the quadratic multiplication
        final double[][] data = { { 1.89040, 3.70927, 3.45396, -8.20288 },
                { 0.25270, -1.01664, -0.72018, -5.61674 },
                { -8.04968, -0.42000, 8.71153, 5.54535 }, { 2.79050, 5.34018, 0.23960, 9.66970 },
                { 1.69740, 4.44017, -1.65618, -5.81734 }, { 7.36806, 8.87630, -7.25697, -9.59722 } };
        final RealMatrix realMatrix = new Array2DRowRealMatrix(data);

        // Test the methods
        CheckUtils.checkQuadraticMultiplication(matrix, realMatrix, ArrayRowSymmetricMatrix.class,
                ABSTOL, RELTOL);
    }

    /**
     * Tests the quadratic multiplication using a matrix which have incompatible dimensions.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DiagonalMatrix#quadraticMultiplication(RealMatrix)}<br>
     * {@linkplain DiagonalMatrix#quadraticMultiplication(RealMatrix, boolean)}<br>
     * </p>
     */
    @Test
    public void testQuadraticMultiplicationIncompatibleMatrix() {
        final DiagonalMatrix matrix = new DiagonalMatrix(4);
        CheckUtils.checkQuadraticMultiplicationIncompatibleMatrix(matrix);
    }

    /**
     * Tests the methods that return a row of the matrix.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DiagonalMatrix#getRow(int)}<br>
     * {@linkplain DiagonalMatrix#getRowVector(int)}<br>
     * {@linkplain DiagonalMatrix#getRowMatrix(int)}<br>
     * </p>
     */
    @Test
    public void testGetRow() {
        // Tested matrix
        final double[] diagonalElements = { 7.54325, -5.83723, 1.37035, 5.18990 };
        final DiagonalMatrix matrix = new DiagonalMatrix(diagonalElements);

        // Test the methods
        CheckUtils.checkGetRow(matrix);
    }

    /**
     * Tests the methods that return a row of the matrix, using indices outside the valid index
     * range.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DiagonalMatrix#getRow(int)}<br>
     * {@linkplain DiagonalMatrix#getRowVector(int)}<br>
     * {@linkplain DiagonalMatrix#getRowMatrix(int)}<br>
     * </p>
     */
    @Test
    public void testGetOutOfRangeRow() {
        final DiagonalMatrix matrix = new DiagonalMatrix(3);
        CheckUtils.checkGetOutOfRangeRow(matrix);
    }

    /**
     * Tests methods that set a row of the matrix.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DiagonalMatrix#setRow(int, double[])}<br>
     * {@linkplain DiagonalMatrix#setRowVector(int, RealVector)}<br>
     * {@linkplain DiagonalMatrix#setRowMatrix(int, RealMatrix)}<br>
     * </p>
     */
    @Test
    public void testSetRow() {
        RealMatrix realMatrix;
        DiagonalMatrix matrix;

        final int dim = 3;

        for (int i = 0; i < dim; i++) {
            final double[] rowArray = new double[dim];
            rowArray[i] = 1. + RNG.nextDouble();
            final RealVector rowVector = new ArrayRealVector(rowArray);
            final RealMatrix rowMatrix = MatrixUtils.createRowRealMatrix(rowArray);

            // Set row array
            matrix = new DiagonalMatrix(dim);
            matrix.setRow(i, rowArray);
            realMatrix = new Array2DRowRealMatrix(dim, dim);
            realMatrix.setRow(i, rowArray);
            realMatrix.setColumn(i, rowArray);
            CheckUtils.checkEquality(realMatrix, matrix, ABSTOL, RELTOL);

            // Set row vector
            matrix = new DiagonalMatrix(dim);
            matrix.setRowVector(i, rowVector);
            realMatrix = new Array2DRowRealMatrix(dim, dim);
            realMatrix.setRowVector(i, rowVector);
            realMatrix.setColumnVector(i, rowVector);
            CheckUtils.checkEquality(realMatrix, matrix, ABSTOL, RELTOL);

            // Set row matrix
            matrix = new DiagonalMatrix(dim);
            matrix.setRowMatrix(i, rowMatrix);
            realMatrix = new Array2DRowRealMatrix(dim, dim);
            realMatrix.setRowMatrix(i, rowMatrix);
            realMatrix.setColumnMatrix(i, rowMatrix.transpose());
            CheckUtils.checkEquality(realMatrix, matrix, ABSTOL, RELTOL);
        }
    }

    /**
     * Tests the methods that set a row of the matrix, using indices outside the valid index range.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DiagonalMatrix#setRow(int, double[])}<br>
     * {@linkplain DiagonalMatrix#setRowVector(int, RealVector)}<br>
     * {@linkplain DiagonalMatrix#setRowMatrix(int, RealMatrix)}<br>
     * </p>
     */
    @Test
    public void testSetOutOfRangeRow() {
        final DiagonalMatrix matrix = new DiagonalMatrix(3);
        CheckUtils.checkSetOutOfRangeRow(matrix);
    }

    /**
     * Tests the methods that set a row of the matrix, using incompatible arrays, vectors and row
     * matrices.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DiagonalMatrix#setRow(int, double[])}<br>
     * {@linkplain DiagonalMatrix#setRowVector(int, RealVector)}<br>
     * {@linkplain DiagonalMatrix#setRowMatrix(int, RealMatrix)}<br>
     * </p>
     */
    @Test
    public void testSetIncompatibleRow() {
        final RealMatrix matrix = new DiagonalMatrix(3);
        CheckUtils.checkSetIncompatibleRow(matrix);
    }

    /**
     * Tests methods that set a row of the matrix, the provided row containing non-zero off-diagonal
     * elements.<br>
     * An exception should be thrown when attempting to set an off-diagonal element to a non-zero
     * value.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DiagonalMatrix#setRow(int, double[])}<br>
     * {@linkplain DiagonalMatrix#setRowVector(int, RealVector)}<br>
     * {@linkplain DiagonalMatrix#setRowMatrix(int, RealMatrix)}<br>
     * </p>
     */
    @Test
    public void testSetInvalidRow() {
        final int dim = 3;
        final DiagonalMatrix matrix = new DiagonalMatrix(dim);

        final double[] rowArray = { +5, -2, +6 };
        final RealVector rowVector = new ArrayRealVector(rowArray);
        final RealMatrix rowMatrix = MatrixUtils.createRowRealMatrix(rowArray);
        final String expectedMessage = "2 is larger than the maximum (0)";

        // Set row array
        try {
            matrix.setRow(0, rowArray);
            Assert.fail();
        } catch (final NumberIsTooLargeException e) {
            Assert.assertEquals(NumberIsTooLargeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // Set row vector
        try {
            matrix.setRowVector(0, rowVector);
            Assert.fail();
        } catch (final NumberIsTooLargeException e) {
            Assert.assertEquals(NumberIsTooLargeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // Set row matrix
        try {
            matrix.setRowMatrix(0, rowMatrix);
            Assert.fail();
        } catch (final NumberIsTooLargeException e) {
            Assert.assertEquals(NumberIsTooLargeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the methods that return a column of the matrix.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DiagonalMatrix#getColumn(int)}<br>
     * {@linkplain DiagonalMatrix#getColumnVector(int)}<br>
     * {@linkplain DiagonalMatrix#getColumnMatrix(int)}<br>
     * </p>
     */
    @Test
    public void testGetColumn() {
        // Tested matrix
        final double[] diagonalElements = { 1.77997, 3.15699, 6.56337, 6.73566 };
        final DiagonalMatrix matrix = new DiagonalMatrix(diagonalElements);

        // Test the methods
        CheckUtils.checkGetColumn(matrix);
    }

    /**
     * Tests the methods that return a column of the matrix, using indices outside the valid index
     * range.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DiagonalMatrix#getColumn(int)}<br>
     * {@linkplain DiagonalMatrix#getColumnVector(int)}<br>
     * {@linkplain DiagonalMatrix#getColumnMatrix(int)}<br>
     * </p>
     */
    @Test
    public void testGetOutOfRangeColumn() {
        final DiagonalMatrix matrix = new DiagonalMatrix(3);
        CheckUtils.checkGetOutOfRangeColumn(matrix);
    }

    /**
     * Tests methods that set a column of the matrix.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DiagonalMatrix#setColumn(int, double[])}<br>
     * {@linkplain DiagonalMatrix#setColumnVector(int, RealVector)}<br>
     * {@linkplain DiagonalMatrix#setColumnMatrix(int, RealMatrix)}<br>
     * </p>
     */
    @Test
    public void testSetColumn() {
        RealMatrix realMatrix;
        DiagonalMatrix matrix;

        final int dim = 3;

        for (int i = 0; i < dim; i++) {
            final double[] columnArray = new double[dim];
            columnArray[i] = 1. + RNG.nextDouble();
            final RealVector columnVector = new ArrayRealVector(columnArray);
            final RealMatrix columnMatrix = MatrixUtils.createColumnRealMatrix(columnArray);

            // Set column array
            matrix = new DiagonalMatrix(dim);
            matrix.setColumn(i, columnArray);
            realMatrix = new Array2DRowRealMatrix(dim, dim);
            realMatrix.setRow(i, columnArray);
            realMatrix.setColumn(i, columnArray);
            CheckUtils.checkEquality(realMatrix, matrix, ABSTOL, RELTOL);

            // Set column vector
            matrix = new DiagonalMatrix(dim);
            matrix.setColumnVector(i, columnVector);
            realMatrix = new Array2DRowRealMatrix(dim, dim);
            realMatrix.setRowVector(i, columnVector);
            realMatrix.setColumnVector(i, columnVector);
            CheckUtils.checkEquality(realMatrix, matrix, ABSTOL, RELTOL);

            // Set column matrix
            matrix = new DiagonalMatrix(dim);
            matrix.setColumnMatrix(i, columnMatrix);
            realMatrix = new Array2DRowRealMatrix(dim, dim);
            realMatrix.setRowMatrix(i, columnMatrix.transpose());
            realMatrix.setColumnMatrix(i, columnMatrix);
            CheckUtils.checkEquality(realMatrix, matrix, ABSTOL, RELTOL);
        }
    }

    /**
     * Tests the methods that set a column of the matrix, using indices outside the valid index
     * range.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DiagonalMatrix#setColumn(int, double[])}<br>
     * {@linkplain DiagonalMatrix#setColumnVector(int, RealVector)}<br>
     * {@linkplain DiagonalMatrix#setColumnMatrix(int, RealMatrix)}<br>
     * </p>
     */
    @Test
    public void testSetOutOfRangeColumn() {
        final DiagonalMatrix matrix = new DiagonalMatrix(3);
        CheckUtils.checkSetOutOfRangeColumn(matrix);
    }

    /**
     * Tests the methods that set a column of the matrix, using incompatible arrays, vectors and
     * column matrices.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DiagonalMatrix#setColumn(int, double[])}<br>
     * {@linkplain DiagonalMatrix#setColumnVector(int, RealVector)}<br>
     * {@linkplain DiagonalMatrix#setColumnMatrix(int, RealMatrix)}<br>
     * </p>
     */
    @Test
    public void testSetIncompatibleColumn() {
        final RealMatrix matrix = new DiagonalMatrix(3);
        CheckUtils.checkSetIncompatibleColumn(matrix);
    }

    /**
     * Tests methods allowing to change an entire column, the provided column containing non-zero
     * off-diagonal elements.<br>
     * An exception should be thrown when attempting to set an off-diagonal element to a non-zero
     * value.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DiagonalMatrix#setColumn(int, double[])}<br>
     * {@linkplain DiagonalMatrix#setColumnVector(int, RealVector)}<br>
     * {@linkplain DiagonalMatrix#setColumnMatrix(int, RealMatrix)}<br>
     * </p>
     */
    @Test
    public void testSetInvalidColumn() {
        final int dim = 3;
        final DiagonalMatrix matrix = new DiagonalMatrix(dim);

        final double[] columnArray = { +5, -2, +6 };
        final RealVector columnVector = new ArrayRealVector(columnArray);
        final RealMatrix columnMatrix = MatrixUtils.createColumnRealMatrix(columnArray);
        final String expectedMessage = "2 is larger than the maximum (0)";

        // Set column array
        try {
            matrix.setColumn(0, columnArray);
            Assert.fail();
        } catch (final NumberIsTooLargeException e) {
            Assert.assertEquals(NumberIsTooLargeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // Set column vector
        try {
            matrix.setColumnVector(0, columnVector);
            Assert.fail();
        } catch (final NumberIsTooLargeException e) {
            Assert.assertEquals(NumberIsTooLargeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // Set column matrix
        try {
            matrix.setColumnMatrix(0, columnMatrix);
            Assert.fail();
        } catch (final NumberIsTooLargeException e) {
            Assert.assertEquals(NumberIsTooLargeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the methods returning a submatrix.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DiagonalMatrix#getSubMatrix(int, int, int, int)}<br>
     * {@linkplain DiagonalMatrix#getSubMatrix(int[], int[])}<br>
     * {@linkplain DiagonalMatrix#getSubMatrix(int, int)}<br>
     * {@linkplain DiagonalMatrix#getSubMatrix(int[])}<br>
     * </p>
     */
    @Test
    public void testGetSubMatrix() {
        // Tested matrix
        final double[] diagonalElements = { 4.01963, 7.20281, 2.69705, 9.23115 };
        final DiagonalMatrix matrix = new DiagonalMatrix(diagonalElements);

        // Test the methods
        CheckUtils.checkGetSubMatrixByRange(matrix);
        CheckUtils.checkGetSubMatrixByIndex(matrix);
        CheckUtils.checkGetSymmetricSubMatrixByRange(matrix, DiagonalMatrix.class);
        CheckUtils.checkGetSymmetricSubMatrixByIndex(matrix, ArrayRowSymmetricMatrix.class);
    }

    /**
     * Tests the methods returning a submatrix when supplying an invalid index range or array.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DiagonalMatrix#getSubMatrix(int, int, int, int)}<br>
     * {@linkplain DiagonalMatrix#getSubMatrix(int[], int[])}<br>
     * {@linkplain DiagonalMatrix#getSubMatrix(int, int)}<br>
     * {@linkplain DiagonalMatrix#getSubMatrix(int[])}<br>
     * </p>
     */
    @Test
    public void testGetSubMatrixInvalidIndices() {
        final DiagonalMatrix matrix = new DiagonalMatrix(3);
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
     * The method should fail as this operation is not supported for diagonal matrices.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DiagonalMatrix#setSubMatrix(double[][], int, int)}
     * </p>
     */
    @Test
    public void testSetSubMatrix() {
        final int dim = 3;
        final DiagonalMatrix matrix = new DiagonalMatrix(dim);
        final double[][] subMatrix = { { 0. } };

        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < dim; j++) {
                try {
                    matrix.setSubMatrix(subMatrix, 0, 0);
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
     * {@linkplain DiagonalMatrix#copySubMatrix(int, int, int, int, double[][])}<br>
     * {@linkplain DiagonalMatrix#copySubMatrix(int, int, int, int, double[][], int, int)}<br>
     * {@linkplain DiagonalMatrix#copySubMatrix(int[], int[], double[][])}<br>
     * {@linkplain DiagonalMatrix#copySubMatrix(int[], int[], double[][], int, int)}<br>
     * </p>
     */
    @Test
    public void testCopySubMatrix() {
        // Tested matrix
        final double[] diagonalElements = { 8.11412, 9.80145, 9.86566, -3.47589 };
        final DiagonalMatrix matrix = new DiagonalMatrix(diagonalElements);

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
     * {@linkplain DiagonalMatrix#copySubMatrix(int, int, int, int, double[][])}<br>
     * {@linkplain DiagonalMatrix#copySubMatrix(int, int, int, int, double[][], int, int)}<br>
     * {@linkplain DiagonalMatrix#copySubMatrix(int[], int[], double[][])}<br>
     * {@linkplain DiagonalMatrix#copySubMatrix(int[], int[], double[][], int, int)}<br>
     * </p>
     */
    @Test
    public void testCopySubMatrixInvalidIndices() {
        final RealMatrix matrix = new DiagonalMatrix(4);
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
     * {@linkplain DiagonalMatrix#copySubMatrix(int, int, int, int, double[][])}<br>
     * {@linkplain DiagonalMatrix#copySubMatrix(int, int, int, int, double[][], int, int)}<br>
     * {@linkplain DiagonalMatrix#copySubMatrix(int[], int[], double[][])}<br>
     * {@linkplain DiagonalMatrix#copySubMatrix(int[], int[], double[][], int, int)}<br>
     * </p>
     */
    @Test
    public void testCopySubMatrixNullDestinationArray() {
        final RealMatrix matrix = new DiagonalMatrix(4);
        CheckUtils.checkCopySubMatrixNullDestinationArray(matrix);
    }

    /**
     * Tests the methods that copies part of the matrix into a given array, using a destination
     * array with no rows or columns.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DiagonalMatrix#copySubMatrix(int, int, int, int, double[][])}<br>
     * {@linkplain DiagonalMatrix#copySubMatrix(int, int, int, int, double[][], int, int)}<br>
     * {@linkplain DiagonalMatrix#copySubMatrix(int[], int[], double[][])}<br>
     * {@linkplain DiagonalMatrix#copySubMatrix(int[], int[], double[][], int, int)}<br>
     * </p>
     */
    @Test
    public void testCopySubMatrixEmptyDestinationArray() {
        final RealMatrix matrix = new DiagonalMatrix(4);
        CheckUtils.checkCopySubMatrixEmptyDestinationArray(matrix);
    }

    /**
     * Tests the methods that copies part of the matrix into a given array, using a destination
     * array too small to store the extracted
     * submatrix.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DiagonalMatrix#copySubMatrix(int, int, int, int, double[][])}<br>
     * {@linkplain DiagonalMatrix#copySubMatrix(int, int, int, int, double[][], int, int)}<br>
     * {@linkplain DiagonalMatrix#copySubMatrix(int[], int[], double[][])}<br>
     * {@linkplain DiagonalMatrix#copySubMatrix(int[], int[], double[][], int, int)}<br>
     * </p>
     */
    @Test
    public void testCopySubMatrixIncompatibleDestinationArray() {
        final RealMatrix matrix = new DiagonalMatrix(4);
        CheckUtils.checkCopySubMatrixIncompatibleDestinationArray(matrix);
    }

    /**
     * Tests the methods that concatenate two matrices horizontally.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DiagonalMatrix#concatenateHorizontally(RealMatrix)}<br>
     * {@linkplain DiagonalMatrix#concatenateHorizontally(RealMatrix, boolean)}<br>
     * </p>
     */
    @Test
    public void testHorizontalConcatenation() {
        // Tested matrix
        final double[] diagonalElements = { -7.29666, 9.66608, -8.32636, -0.59870 };
        final DiagonalMatrix matrix = new DiagonalMatrix(diagonalElements);

        // Matrix to be concatenated with the tested matrix (4x6)
        final double[][] data = { { -7.96303, 5.63853, 6.36663, -3.92171, 9.07301, -1.98710 },
                { -5.38377, -9.54315, 6.24416, 3.89159, -0.20719, 6.54187 },
                { 5.99855, 1.92835, -6.34546, -0.95278, -5.09763, 0.59232 },
                { -7.98010, 8.32111, 9.35589, 5.12340, 4.84271, -2.40547 } };
        final RealMatrix realMatrix = new Array2DRowRealMatrix(data, false);

        // Test the methods
        CheckUtils.checkConcatenateHorizontally(matrix, realMatrix, Array2DRowRealMatrix.class);
    }

    /**
     * Tests the methods that concatenate two matrices vertically.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DiagonalMatrix#concatenateVertically(RealMatrix)}<br>
     * {@linkplain DiagonalMatrix#concatenateVertically(RealMatrix, boolean)}<br>
     * </p>
     */
    @Test
    public void testVerticalConcatenation() {
        // Tested matrix
        final double[] diagonalElements = { 3.91767, -1.73725, 1.46305, -0.55887 };
        final DiagonalMatrix matrix = new DiagonalMatrix(diagonalElements);

        // Matrix to be concatenated with the tested matrix (6x4)
        final double[][] data = { { 5.91326, 3.78860, -8.50447, 1.90926 },
                { -2.10697, 7.70093, -2.46120, -1.01307 },
                { 7.53040, 8.36633, -0.99134, -8.72313 },
                { -6.81181, -0.45226, -8.92962, 3.54684 },
                { -8.92604, 4.24367, 7.50876, -9.34971 }, { -5.61792, -1.21420, -2.45439, 7.05046 } };
        final RealMatrix realMatrix = new Array2DRowRealMatrix(data, false);

        // Test the methods
        CheckUtils.checkConcatenateVertically(matrix, realMatrix, Array2DRowRealMatrix.class);
    }

    /**
     * Tests the methods that concatenate two matrices diagonally.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain DiagonalMatrix#concatenateDiagonally(RealMatrix, boolean)}<br>
     * </p>
     */
    @Test
    public void testDiagonalConcatenation() {
        // Tested matrix
        final double[] diagonalElements = { 0.25467, 5.13399, -9.55204, 0.85194 };
        final DiagonalMatrix matrix = new DiagonalMatrix(diagonalElements);

        // Matrix to be concatenated with the tested matrix (6x4)
        final double[][] data = { { -9.51029, -3.08409, 9.85208, 5.70326 },
                { -8.73257, 2.97237, 9.96450, -4.03095 },
                { -5.72498, -0.96612, -6.76405, -1.96112 },
                { -8.10082, 7.77027, -3.37450, -4.67245 }, { 0.83623, 6.67787, 9.76489, 2.06827 },
                { -5.62610, -2.00559, 2.78089, 7.11152 } };
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
     * {@linkplain DiagonalMatrix#concatenateHorizontally(RealMatrix)}<br>
     * {@linkplain DiagonalMatrix#concatenateHorizontally(RealMatrix, boolean)}<br>
     * </p>
     */
    @Test
    public void testHorizontalConcatenationIncompatibleMatrix() {
        final RealMatrix matrix = new DiagonalMatrix(4);
        CheckUtils.checkConcatenateHorizontallyIncompatibleMatrix(matrix);
    }

    /**
     * Tests the methods that concatenate two matrices vertically, using matrices with incompatible
     * dimensions.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DiagonalMatrix#concatenateVertically(RealMatrix)}<br>
     * {@linkplain DiagonalMatrix#concatenateVertically(RealMatrix, boolean)}<br>
     * </p>
     */
    @Test
    public void testVerticalConcatenationIncompatibleMatrix() {
        final RealMatrix matrix = new DiagonalMatrix(4);
        CheckUtils.checkConcatenateVerticallyIncompatibleMatrix(matrix);
    }

    /**
     * Tests the matrix inversion.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain DiagonalMatrix#getInverse()}
     * </p>
     */
    @Test
    public void testGetMatrixInverse() {
        // Tested matrix
        final double[] diagonalElements = { -6.02406, 7.83181, 6.94205, 2.92086 };
        final DiagonalMatrix matrix = new DiagonalMatrix(diagonalElements);

        // Test the method
        CheckUtils.checkInverseMatrix(matrix, DiagonalMatrix.class, ABSTOL, RELTOL);
    }

    /**
     * Tests the inversion of a singular matrix.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain DiagonalMatrix#getInverse()}
     * </p>
     */
    @Test
    public void testGetSingularMatrixInverse() {
        final DiagonalMatrix matrix = new DiagonalMatrix(4);
        CheckUtils.checkInverseSingularMatrix(matrix);
    }

    /**
     * Tests the method that sets the decomposition algorithm to use by default when computing the
     * inverse matrix.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DiagonalMatrix#getDefaultDecomposition()}<br>
     * {@linkplain DiagonalMatrix#setDefaultDecomposition(Function)}<br>
     * </p>
     */
    @Test
    public void testSetDefaultDecomposition() {
        final RealMatrix matrix = new DiagonalMatrix(4);
        CheckUtils.checkDefaultDecomposition(matrix);
    }

    /**
     * Tests the methods that returns a string representation of the matrix.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain DiagonalMatrix#toString()}<br>
     * {@linkplain DiagonalMatrix#toString(RealMatrixFormat)}<br>
     * </p>
     */
    @Test
    public void testToString() {
        // Tested matrix
        final double[] diagonalElements = { -6.32712, 9.35875, -2.99139, -0.12086 };
        final DiagonalMatrix matrix = new DiagonalMatrix(diagonalElements);

        // Test the method
        CheckUtils.checkToString(matrix);
    }

    /**
     * Tests the methods:<br>
     * {@linkplain DiagonalMatrix#equals(Object)}<br>
     * {@linkplain DiagonalMatrix#hashCode()}
     */
    @Test
    public void testEqualsAndHashCode() {
        RealMatrix other;

        final double[] diagonalElements = { 0.10240, -7.71342, 4.92760, 1.05048 };
        final DiagonalMatrix matrix = new DiagonalMatrix(diagonalElements);
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
            final double[] modifiedData = Arrays.copyOf(diagonalElements, diagonalElements.length);
            modifiedData[i] = 0.;

            other = new DiagonalMatrix(modifiedData);
            Assert.assertFalse(matrix.equals(other));
            Assert.assertFalse(other.equals(matrix));
            Assert.assertFalse(matrix.hashCode() == other.hashCode());
        }

        // A single element is different (matrix of a different type)
        for (int i = 0; i < matrix.getRowDimension(); i++) {
            for (int j = 0; j < matrix.getColumnDimension(); j++) {
                final double[][] modifiedData = CheckUtils.copyArray(data);
                modifiedData[i][j] = 1.;

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
     * {@linkplain DiagonalMatrix#equals(RealMatrix, double, double)}
     * </p>
     */
    @Test
    public void testEqualsWithTolerances() {
        // Tested matrix
        final double[] diagonalElements = { -5.05875, 8.65876, -0.81063, 2.74742 };
        final RealMatrix matrix = new DiagonalMatrix(diagonalElements);

        // Test the method
        CheckUtils.checkEqualsWithTolerances(matrix);
    }

    /**
     * Tests the serialization/deserialization.
     */
    @Test
    public void testSerialization() {
        // Tested matrix
        final double[] diagonalElements = { -9.05307, -0.68567, 6.93969, 9.17306 };
        final DiagonalMatrix matrix = new DiagonalMatrix(diagonalElements);

        // Test the serialization
        CheckUtils.checkSerialization(matrix, DiagonalMatrix.class);
    }

    /**
     * Tests the method allowing to visit the elements of the matrix without modifying them.
     *
     * <p>
     * Tested methods<br>
     * {@linkplain DiagonalMatrix#walkInRowOrder(RealMatrixPreservingVisitor)}<br>
     * {@linkplain DiagonalMatrix#walkInRowOrder(RealMatrixPreservingVisitor, int, int, int, int)}<br>
     * {@linkplain DiagonalMatrix#walkInColumnOrder(RealMatrixPreservingVisitor)}<br>
     * {@linkplain DiagonalMatrix#walkInColumnOrder(RealMatrixPreservingVisitor, int, int, int, int)}
     * <br>
     * {@linkplain DiagonalMatrix#walkInOptimizedOrder(RealMatrixPreservingVisitor)}<br>
     * {@linkplain DiagonalMatrix#walkInOptimizedOrder(RealMatrixPreservingVisitor, int, int, int, int)}
     * <br>
     * </p>
     */
    @Test
    public void testPreservingVisitor() {
        // Tested matrix
        final double[] diagonalElements = { -9.05307, -0.68567, 6.93969, 9.17306 };
        final DiagonalMatrix matrix = new DiagonalMatrix(diagonalElements);

        // Test the method
        CheckUtils.checkPreservingWalkInRowOrder(matrix);
        CheckUtils.checkPreservingWalkInColumnOrder(matrix);
        CheckUtils.checkPreservingWalkInOptimizedOrder(matrix);
    }

    /**
     * Tests the method allowing to visit and modify the elements of the matrix.
     *
     * <p>
     * Tested methods<br>
     * {@linkplain DiagonalMatrix#walkInRowOrder(RealMatrixChangingVisitor)}<br>
     * {@linkplain DiagonalMatrix#walkInRowOrder(RealMatrixChangingVisitor, int, int, int, int)}<br>
     * {@linkplain DiagonalMatrix#walkInColumnOrder(RealMatrixChangingVisitor)}<br>
     * {@linkplain DiagonalMatrix#walkInColumnOrder(RealMatrixChangingVisitor, int, int, int, int)}<br>
     * {@linkplain DiagonalMatrix#walkInOptimizedOrder(RealMatrixChangingVisitor)}<br>
     * {@linkplain DiagonalMatrix#walkInOptimizedOrder(RealMatrixChangingVisitor, int, int, int, int)}
     * <br>
     * </p>
     */
    @Test
    public void testChangingVisitor() {
        double[] diagonalElements;
        DiagonalMatrix matrix;
        RealMatrix expected;

        final ChangingVisitor visitor = new ChangingVisitor();

        // Visit the entire matrix
        diagonalElements = new double[] { 1., -2., 7. };
        matrix = new DiagonalMatrix(diagonalElements);
        matrix.walkInRowOrder(visitor);
        diagonalElements = new double[] { 12., 1794., 146472. };
        expected = new DiagonalMatrix(diagonalElements);
        CheckUtils.checkEquality(expected, matrix, ABSTOL, RELTOL);
        Assert.assertEquals(9, visitor.getNbVisited());
        Assert.assertEquals(146472, visitor.getResult());

        diagonalElements = new double[] { 1., -2., 7. };
        matrix = new DiagonalMatrix(diagonalElements);
        matrix.walkInColumnOrder(visitor);
        diagonalElements = new double[] { 12., 1710., 139656. };
        expected = new DiagonalMatrix(diagonalElements);
        CheckUtils.checkEquality(expected, matrix, ABSTOL, RELTOL);
        Assert.assertEquals(9, visitor.getNbVisited());
        Assert.assertEquals(139656, visitor.getResult());

        diagonalElements = new double[] { 1., -2., 7. };
        matrix = new DiagonalMatrix(diagonalElements);
        matrix.walkInOptimizedOrder(visitor);
        diagonalElements = new double[] { 12., 1794., 146472. };
        expected = new DiagonalMatrix(diagonalElements);
        CheckUtils.checkEquality(expected, matrix, ABSTOL, RELTOL);
        Assert.assertEquals(9, visitor.getNbVisited());
        Assert.assertEquals(146472, visitor.getResult());

        // Visit part of the matrix
        diagonalElements = new double[] { 1., -2., 7. };
        matrix = new DiagonalMatrix(diagonalElements);
        matrix.walkInRowOrder(visitor, 1, 2, 0, 1);
        diagonalElements = new double[] { 1., 75., 7. };
        expected = new DiagonalMatrix(diagonalElements);
        CheckUtils.checkEquality(expected, matrix, ABSTOL, RELTOL);
        Assert.assertEquals(4, visitor.getNbVisited());
        Assert.assertEquals(770, visitor.getResult());

        diagonalElements = new double[] { 1., -2., 7. };
        matrix = new DiagonalMatrix(diagonalElements);
        matrix.walkInColumnOrder(visitor, 1, 2, 0, 1);
        diagonalElements = new double[] { 1., 243., 7. };
        expected = new DiagonalMatrix(diagonalElements);
        CheckUtils.checkEquality(expected, matrix, ABSTOL, RELTOL);
        Assert.assertEquals(4, visitor.getNbVisited());
        Assert.assertEquals(758, visitor.getResult(), 0.);

        diagonalElements = new double[] { 1., -2., 7. };
        matrix = new DiagonalMatrix(diagonalElements);
        matrix.walkInOptimizedOrder(visitor, 1, 2, 0, 1);
        diagonalElements = new double[] { 1., 75., 7. };
        expected = new DiagonalMatrix(diagonalElements);
        CheckUtils.checkEquality(expected, matrix, ABSTOL, RELTOL);
        Assert.assertEquals(4, visitor.getNbVisited());
        Assert.assertEquals(770, visitor.getResult());
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

            double newValue = 0.;
            if (row == column) {
                newValue = this.result;
            }
            return newValue;
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
