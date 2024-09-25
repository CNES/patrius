/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 *
 * Copyright 2011-2021 CNES
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
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.covariance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.exception.NotPositiveException;
import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;
import fr.cnes.sirius.patrius.math.linear.Array2DRowRealMatrix;
import fr.cnes.sirius.patrius.math.linear.ArrayRealVector;
import fr.cnes.sirius.patrius.math.linear.ArrayRowSymmetricMatrix.SymmetryType;
import fr.cnes.sirius.patrius.math.linear.ArrayRowSymmetricPositiveMatrix;
import fr.cnes.sirius.patrius.math.linear.CheckUtils;
import fr.cnes.sirius.patrius.math.linear.DiagonalMatrix;
import fr.cnes.sirius.patrius.math.linear.MatrixUtils;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealMatrixFormat;
import fr.cnes.sirius.patrius.math.linear.RealVector;
import fr.cnes.sirius.patrius.math.linear.SymmetricPositiveMatrix;
import fr.cnes.sirius.patrius.math.parameter.FieldDescriptor;
import fr.cnes.sirius.patrius.math.parameter.ParameterDescriptor;
import fr.cnes.sirius.patrius.math.parameter.StandardFieldDescriptors;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.CartesianCoordinate;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.OrbitalCoordinate;

/**
 * Unit tests for {@linkplain Covariance}.
 *
 * @author Pierre Seimandi (GMV)
 */
public class CovarianceTest {

    /** Default absolute tolerance used for the tests. */
    private static final double ABSTOL = 0;

    /** Default relative tolerance used for the tests. */
    private static final double RELTOL = 1E-14;

    /** Prefix used for the default parameter names. */
    private static final String PARAMETER_NAME_PREFIX = "p";

    /**
     * Expected message format for exceptions thrown when a row index is outside the valid index
     * range.
     */
    private static final String OUT_OF_RANGE_ROW_INDEX_FORMAT = "row index (%d)";

    /**
     * Expected message format for exceptions thrown when a column index is outside the valid index
     * range.
     */
    private static final String OUT_OF_RANGE_COLUMN_INDEX_FORMAT = "column index (%d)";

    /** Expected message format for exceptions thrown when a null argument is detected. */
    private static final String NULL_ARGUMENT_FORMAT = "A non-null value is expected (%s)";

    /** Expected message format for exceptions thrown when a null argument is detected. */
    private static final String WRONG_PARAMETER_DESCRIPTOR_FORMAT = "The provided parameter descriptor (%s) is not associated with this covariance matrix";

    /**
     * Expected message format for exceptions thrown when the list of parameter descriptors contains
     * duplicates.
     */
    private static final String DUPLICATE_PARAMETER_DESCRIPTOR_FORMAT = "The collection of parameter descriptors contains duplicates";

    /**
     * Initialization.
     */
    @BeforeClass
    public static void init() {
        Locale.setDefault(Locale.ENGLISH);
    }

    /**
     * Tests the creation of new instances and tests the basic getters when no parameter descriptors
     * are provided.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain Covariance#Covariance(SymmetricPositiveMatrix)}<br>
     * {@linkplain Covariance#Covariance(SymmetricPositiveMatrix, Collection)}<br>
     * <p/>
     */
    @Test
    public void testConstructorNoDescriptors() {
        Covariance covariance;

        // Covariance matrix
        final double[][] data = { { 8.27249, -0.40635, 1.09182 }, { -0.40635, 7.81477, 0.10501 },
                { 1.09182, 0.10501, 7.74229 } };
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, data);

        // Expected default parameter descriptors
        final List<ParameterDescriptor> defaultDescriptors = new ArrayList<>();
        defaultDescriptors.add(new ParameterDescriptor("p0"));
        defaultDescriptors.add(new ParameterDescriptor("p1"));
        defaultDescriptors.add(new ParameterDescriptor("p2"));

        // No parameter descriptors are provided
        covariance = new Covariance(matrix);
        Assert.assertEquals(matrix, covariance.getCovarianceMatrix());
        CheckUtils.checkEquality(defaultDescriptors, covariance.getParameterDescriptors());
        Assert.assertEquals(3, covariance.getSize());

        // The list of parameter descriptors is null
        covariance = new Covariance(matrix, null);
        Assert.assertEquals(matrix, covariance.getCovarianceMatrix());
        CheckUtils.checkEquality(defaultDescriptors, covariance.getParameterDescriptors());
        Assert.assertEquals(3, covariance.getSize());

        // The list of parameter descriptors is empty
        covariance = new Covariance(matrix, new ArrayList<>());
        Assert.assertEquals(matrix, covariance.getCovarianceMatrix());
        CheckUtils.checkEquality(defaultDescriptors, covariance.getParameterDescriptors());
        Assert.assertEquals(3, covariance.getSize());
    }

    /**
     * Tests the creation of new instances and tests the basic getters when parameter descriptors
     * are provided.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain Covariance#Covariance(SymmetricPositiveMatrix, Collection)}
     * <p/>
     */
    @Test
    public void testConstructorWithDescriptors() {
        ParameterDescriptor initialDescriptor;
        ParameterDescriptor returnedDescriptor;

        // Covariance matrix
        final double[][] data = { { 8.00577, -1.22868, 1.92701 }, { -1.22868, 5.90444, 2.80285 },
                { 1.92701, 2.80285, 5.30935 } };
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, data);

        // Parameter descriptors
        final List<ParameterDescriptor> parameterDescriptors = new ArrayList<>();
        parameterDescriptors.add(new ParameterDescriptor("x"));
        parameterDescriptors.add(new ParameterDescriptor("y"));
        parameterDescriptors.add(new ParameterDescriptor("z"));

        // Tests the constructor
        final Covariance covariance = new Covariance(matrix, parameterDescriptors);
        Assert.assertEquals(3, covariance.getSize());

        // Check the covariance matrix
        // (the matrix is passed by reference)
        Assert.assertEquals(matrix, covariance.getCovarianceMatrix());
        Assert.assertSame(matrix, covariance.getCovarianceMatrix());

        // Check the list of parameter descriptors
        CheckUtils.checkEquality(parameterDescriptors, covariance.getParameterDescriptors());
        Assert.assertNotSame(parameterDescriptors, covariance.getParameterDescriptors());

        for (int i = 0; i < covariance.getSize(); i++) {
            initialDescriptor = parameterDescriptors.get(0);
            returnedDescriptor = covariance.getParameterDescriptor(0);
            Assert.assertEquals(initialDescriptor, returnedDescriptor);
            Assert.assertNotSame(initialDescriptor, returnedDescriptor);
        }
    }

    /**
     * Tests the creation of new instances with a null covariance matrix.<br>
     * An exception should be thrown.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain Covariance#Covariance(SymmetricPositiveMatrix)}<br>
     * {@linkplain Covariance#Covariance(SymmetricPositiveMatrix, Collection)}
     * </p>
     */
    @Test
    public void testConstructorNullCovarianceMatrix() {
        final String expectedMessage = String.format(NULL_ARGUMENT_FORMAT, "covariance matrix");

        try {
            new Covariance(null);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            new Covariance(null, null);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the creation of new instances when the collection of parameter descriptors contains
     * null elements.<br>
     * An exception should be thrown.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain Covariance#Covariance(SymmetricPositiveMatrix, Collection)}
     * </p>
     */
    @Test
    public void testConstructorNullDescriptor() {
        // Covariance matrix
        final double[][] data = { { 6.33851, -0.1412, 1.53232 }, { -0.1412, 8.25238, -0.55901 },
                { 1.53232, -0.55901, 6.57742 } };
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, data);

        // Parameter descriptors
        final List<ParameterDescriptor> parameterDescriptors = new ArrayList<>();
        parameterDescriptors.add(new ParameterDescriptor("x"));
        parameterDescriptors.add(new ParameterDescriptor("y"));
        parameterDescriptors.add(null);

        try {
            new Covariance(matrix, parameterDescriptors);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals("Invalid parameter descriptors: element number 2 is null",
                    e.getMessage());
        }
    }

    /**
     * Tests the creation of new instances when the collection of parameter descriptors contains
     * empty elements.<br>
     * An exception should be thrown.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain Covariance#Covariance(SymmetricPositiveMatrix, Collection)}
     * </p>
     */
    @Test
    public void testConstructorEmptyDescriptor() {
        // Covariance matrix
        final double[][] data = { { 7.32313, 0.04871, 0.71207 }, { 0.04871, 7.23017, -0.27874 },
                { 0.71207, -0.27874, 8.63085 } };
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, data);

        // Parameter descriptors
        final List<ParameterDescriptor> parameterDescriptors = new ArrayList<>();
        parameterDescriptors.add(new ParameterDescriptor("x"));
        parameterDescriptors.add(new ParameterDescriptor());
        parameterDescriptors.add(new ParameterDescriptor("z"));

        try {
            new Covariance(matrix, parameterDescriptors);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals("Invalid parameter descriptors: element number 1 is empty",
                    e.getMessage());
        }
    }

    /**
     * Tests the creation of new instances when the number of parameter descriptors is lower than
     * the size of the covariance matrix.<br>
     * An exception should be thrown.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain Covariance#Covariance(SymmetricPositiveMatrix)}
     * </p>
     */
    @Test
    public void testConstructorNotEnoughDescritpors() {
        // Covariance matrix
        final double[][] data = { { 3.02669, -0.66529, -2.26256 }, { -0.66529, 3.78196, -0.43502 },
                { -2.26256, -0.43502, 3.76387 } };
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, data);

        // Not enough parameter descriptors
        final List<ParameterDescriptor> parameterDescriptors = new ArrayList<>();
        parameterDescriptors.add(new ParameterDescriptor("x"));
        parameterDescriptors.add(new ParameterDescriptor("y"));

        try {
            new Covariance(matrix, parameterDescriptors);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals(
                    "The number of parameter descriptors does not match the size of the covariance matrix (2 != 3)",
                    e.getMessage());
        }
    }

    /**
     * Tests the creation of new instances when the number of parameter descriptors is greater than
     * the size of the covariance matrix.<br>
     * An exception should be thrown.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain Covariance#Covariance(SymmetricPositiveMatrix)}
     * </p>
     */
    @Test
    public void testConstructorTooManyDescritpors() {
        // Covariance matrix
        final double[][] data = { { 6.12195, -0.0465, 0.22182 }, { -0.0465, 4.28308, -0.29165 },
                { 0.22182, -0.29165, 4.19838 } };
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, data);

        // Too many parameter descriptors
        final List<ParameterDescriptor> parameterDescriptors = new ArrayList<>();
        parameterDescriptors.add(new ParameterDescriptor("p0"));
        parameterDescriptors.add(new ParameterDescriptor("p1"));
        parameterDescriptors.add(new ParameterDescriptor("p2"));
        parameterDescriptors.add(new ParameterDescriptor("p3"));

        try {
            new Covariance(matrix, parameterDescriptors);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals(
                    "The number of parameter descriptors does not match the size of the covariance matrix (4 != 3)",
                    e.getMessage());
        }
    }

    /**
     * Tests the creation of new instances when the collection of parameter descriptors contains
     * duplicates. An exception should be thrown.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain Covariance#Covariance(SymmetricPositiveMatrix)}
     * </p>
     */
    @Test
    public void testConstructorDuplicatedDescriptors() {
        // Covariance matrix
        final double[][] data = { { 3.3624, -0.682, 0.93764 }, { -0.682, 3.8178, -1.21258 },
                { 0.93764, -1.21258, 3.65017 } };
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, data);

        // Parameter descriptors
        final List<ParameterDescriptor> parameterDescriptors = new ArrayList<>();
        parameterDescriptors.add(new ParameterDescriptor("x"));
        parameterDescriptors.add(new ParameterDescriptor("y"));
        parameterDescriptors.add(new ParameterDescriptor("y"));

        try {
            new Covariance(matrix, parameterDescriptors);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            final String expectedMessage = String.format(DUPLICATE_PARAMETER_DESCRIPTOR_FORMAT);
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the computation of the variance matrix.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain Covariance#getVarianceMatrix()}<br>
     * {@linkplain Covariance#getVariance(int)}<br>
     * {@linkplain Covariance#getVariance(ParameterDescriptor)}<br>
     * </p>
     */
    @Test
    public void testVariances() {
        double[][] data;

        // Covariance matrix
        data = new double[][] { { 6.96737, -0.60533, -0.06339 }, { -0.60533, 8.81563, -0.06726 },
                { -0.06339, -0.06726, 9.52571 } };
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, data);
        final Covariance covariance = new Covariance(matrix);

        // Variance matrix
        data = new double[][] { { 6.96737, 0, 0 }, { 0, 8.81563, 0 }, { 0, 0, 9.52571 } };
        final RealMatrix expected = new Array2DRowRealMatrix(data);
        final RealMatrix result = covariance.getVarianceMatrix();
        CheckUtils.checkEquality(expected, result, 0., 0.);
        Assert.assertEquals(DiagonalMatrix.class, result.getClass());

        // Variance coefficient (retrieved using the row/column index)
        for (int i = 0; i < matrix.getRowDimension(); i++) {
            final double variance = covariance.getVariance(i);
            CheckUtils.checkEquality(expected.getEntry(i, i), variance, 0, 0);
        }

        // Variance coefficient (retrieved using the parameter descriptor)
        for (int i = 0; i < matrix.getRowDimension(); i++) {
            final ParameterDescriptor parameterDescriptor = new ParameterDescriptor(
                    PARAMETER_NAME_PREFIX + i);
            final double variance = covariance.getVariance(parameterDescriptor);
            CheckUtils.checkEquality(expected.getEntry(i, i), variance, 0., 0.);
        }
    }

    /**
     * Tests the computation of the variance when the provided index is not a valid index.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain Covariance#getVariance(int)}<br>
     * </p>
     */
    @Test
    public void testVarianceInvalidIndex() {
        // Covariance matrix
        final double[][] data = new double[][] { { 5.63933, -4.07589, -0.10789 },
                { -4.07589, 4.25501, -0.16633 }, { -0.10789, -0.16633, 9.3106 } };
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, data);
        final Covariance covariance = new Covariance(matrix);

        // Index lower than 0
        try {
            covariance.getVariance(-1);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_ROW_INDEX_FORMAT, -1);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // Index greater than 2
        try {
            covariance.getVariance(3);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_ROW_INDEX_FORMAT, 3);
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the computation of the variance when the provided parameter descriptor is not
     * associated with the covariance matrix.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain Covariance#getVariance(ParameterDescriptor)}<br>
     * </p>
     */
    @Test
    public void testVarianceInvalidDescriptor() {
        // Covariance matrix
        final double[][] data = new double[][] { { 3.98362, 0.31851, -2.4477 },
                { 0.31851, 4.51838, 3.18117 }, { -2.4477, 3.18117, 6.56474 } };
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, data);
        final Covariance covariance = new Covariance(matrix);

        // Invalid parameter descriptor
        try {
            covariance.getVariance(new ParameterDescriptor("x"));
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            final String expectedMessage = String.format(WRONG_PARAMETER_DESCRIPTOR_FORMAT, "x");
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the computation of the variance when the provided parameter descriptor is not
     * associated with the covariance matrix.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain Covariance#getVariance(ParameterDescriptor)}<br>
     * </p>
     */
    @Test
    public void testVarianceNullDescriptor() {
        // Covariance matrix
        final double[][] data = new double[][] { { 3.98362, 0.31851, -2.4477 },
                { 0.31851, 4.51838, 3.18117 }, { -2.4477, 3.18117, 6.56474 } };
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, data);
        final Covariance covariance = new Covariance(matrix);

        // Invalid parameter descriptor
        try {
            covariance.getVariance(null);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            final String expectedMessage = String.format(NULL_ARGUMENT_FORMAT,
                    "parameter descriptor");
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the computation of the standard deviations matrix.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain Covariance#getStandardDeviationMatrix()}<br>
     * {@linkplain Covariance#getStandardDeviation(int)}<br>
     * {@linkplain Covariance#getStandardDeviation(ParameterDescriptor)}<br>
     * </p>
     */
    @Test
    public void testStandardDeviations() {
        double[][] data;
        ParameterDescriptor parameterDescriptor;

        // Covariance matrix
        data = new double[][] { { 1.0, 0.5, -0.7 }, { 0.5, 4.0, 2.0 }, { -0.7, 2.0, 9.0 } };
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, data);
        final Covariance covariance = new Covariance(matrix);

        // Standard deviation matrix
        data = new double[][] { { 1.0, 0.0, 0.0 }, { 0.0, 2.0, 0.0 }, { 0.0, 0.0, 3.0 } };
        final RealMatrix expected = new Array2DRowRealMatrix(data);
        final RealMatrix result = covariance.getStandardDeviationMatrix();
        CheckUtils.checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertEquals(DiagonalMatrix.class, result.getClass());

        // Standard deviation coefficients (retrieved using the row/column index)
        Assert.assertEquals(1.0, covariance.getStandardDeviation(0), 0);
        Assert.assertEquals(2.0, covariance.getStandardDeviation(1), 0);
        Assert.assertEquals(3.0, covariance.getStandardDeviation(2), 0);

        // Standard deviation coefficients (retrieved using the parameter descriptor)
        parameterDescriptor = new ParameterDescriptor("p0");
        Assert.assertEquals(1.0, covariance.getStandardDeviation(parameterDescriptor), 0);
        parameterDescriptor = new ParameterDescriptor("p1");
        Assert.assertEquals(2.0, covariance.getStandardDeviation(parameterDescriptor), 0);
        parameterDescriptor = new ParameterDescriptor("p2");
        Assert.assertEquals(3.0, covariance.getStandardDeviation(parameterDescriptor), 0);
    }

    /**
     * Tests the computation of the standard deviation when the provided index is not a valid index.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain Covariance#getStandardDeviation(int)}<br>
     * </p>
     */
    @Test
    public void testStandarDeviationInvalidIndex() {
        // Covariance matrix
        final double[][] data = { { 6.56855, 0.55238, 0.39299 }, { 0.55238, 6.2607, -0.77222 },
                { 0.39299, -0.77222, 6.09654 } };
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, data);
        final Covariance covariance = new Covariance(matrix);

        // Index lower than 0
        try {
            covariance.getStandardDeviation(-1);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_ROW_INDEX_FORMAT, -1);
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // Index greater than 2
        try {
            covariance.getStandardDeviation(3);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_ROW_INDEX_FORMAT, 3);
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the computation of the standard deviation when the provided parameter descriptor is not
     * associated with the covariance matrix.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain Covariance#getStandardDeviation(ParameterDescriptor)}<br>
     * </p>
     */
    @Test
    public void testStandardDeviationInvalidDescriptor() {
        // Covariance matrix
        final double[][] data = { { 7.19779, -1.12108, 0.89517 }, { -1.12108, 6.91006, 1.11643 },
                { 0.89517, 1.11643, 8.37477 } };
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, data);
        final Covariance covariance = new Covariance(matrix);

        // Invalid parameter descriptor
        try {
            covariance.getStandardDeviation(new ParameterDescriptor("x"));
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            final String expectedMessage = String.format(WRONG_PARAMETER_DESCRIPTOR_FORMAT, "x");
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the computation of the standard deviation when the provided parameter descriptor is not
     * associated with the covariance matrix.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain Covariance#getStandardDeviation(ParameterDescriptor)}<br>
     * </p>
     */
    @Test
    public void testStandardDeviationNullDescriptor() {
        // Covariance matrix
        final double[][] data = { { 8.86385, 0.37774, 0.55514 }, { 0.37774, 9.85085, -0.12459 },
                { 0.55514, -0.12459, 9.53879 } };
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, data);
        final Covariance covariance = new Covariance(matrix);

        // Invalid parameter descriptor
        try {
            covariance.getStandardDeviation(null);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            final String expectedMessage = String.format(NULL_ARGUMENT_FORMAT,
                    "parameter descriptor");
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Computes the correlation coefficients of a covariance matrix.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain Covariance#getCorrelationCoefficientsMatrix()}<br>
     * {@linkplain Covariance#getCorrelationCoefficient(int, int)}<br>
     * {@linkplain Covariance#getCorrelationCoefficient(ParameterDescriptor, ParameterDescriptor)}<br>
     * </p>
     */
    @Test
    public void testCorrelationCoefficients() {
        double[][] data;

        // Covariance matrix
        data = new double[][] { { 6.83691, -0.36057, -1.07289 }, { -0.36057, 6.54864, 2.1778 },
                { -1.07289, 2.1778, 3.10049 } };
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, data);
        final Covariance covariance = new Covariance(matrix);

        // Correlation coefficients matrix
        data = new double[][] { { 1, -0.053887011658649, -0.233029068132915 },
                { -0.053887011658649, 1, 0.483311654462081 },
                { -0.233029068132915, 0.483311654462081, 1 } };
        final RealMatrix expected = new Array2DRowRealMatrix(data);
        final RealMatrix result = covariance.getCorrelationCoefficientsMatrix();
        CheckUtils.checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertEquals(ArrayRowSymmetricPositiveMatrix.class, result.getClass());

        // Correlation coefficients (retrieved using the row/column index)
        for (int i = 0; i < matrix.getRowDimension(); i++) {
            for (int j = 0; j < matrix.getColumnDimension(); j++) {
                final double rhoIJ = covariance.getCorrelationCoefficient(i, j);
                CheckUtils.checkEquality(expected.getEntry(i, j), rhoIJ, ABSTOL, RELTOL);
            }
        }

        // Correlation coefficients (retrieved using the parameter descriptors)
        for (int i = 0; i < matrix.getRowDimension(); i++) {
            final ParameterDescriptor parameterDescriptorI = new ParameterDescriptor(
                    PARAMETER_NAME_PREFIX + i);
            for (int j = 0; j < matrix.getColumnDimension(); j++) {
                final ParameterDescriptor parameterDescriptorJ = new ParameterDescriptor(
                        PARAMETER_NAME_PREFIX + j);
                final double rhoIJ = covariance.getCorrelationCoefficient(parameterDescriptorI,
                        parameterDescriptorJ);
                CheckUtils.checkEquality(expected.getEntry(i, j), rhoIJ, ABSTOL, RELTOL);
            }
        }
    }

    /**
     * Computes the correlation coefficients of a non positive definite covariance matrix.<br>
     * Some of the computed coefficients should be outside of [-1,+1].
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain Covariance#getCorrelationCoefficientsMatrix()}<br>
     * {@linkplain Covariance#getCorrelationCoefficient(int, int)}<br>
     * {@linkplain Covariance#getCorrelationCoefficient(ParameterDescriptor, ParameterDescriptor)}<br>
     * </p>
     */
    @Test
    public void testCorrelationCoefficientNonSPD() {
        double[][] data;

        // Covariance matrix
        data = new double[][] { { 5.05196, -20.35774, 20.79626 }, { -20.35774, 4.01828, -0.99995 },
                { 20.79626, -0.99995, 4.83458 } };
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, data, 0., 0., null, null);
        final Covariance covariance = new Covariance(matrix);

        // Correlation coefficients matrix
        data = new double[][] { { 1, -4.51834630406718, 4.208001745246253 },
                { -4.51834630406718, 1, -0.226870909085376 },
                { 4.208001745246253, -0.226870909085376, 1 } };
        final RealMatrix expected = new Array2DRowRealMatrix(data);
        final RealMatrix result = covariance.getCorrelationCoefficientsMatrix();
        CheckUtils.checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertEquals(ArrayRowSymmetricPositiveMatrix.class, result.getClass());

        // Correlation coefficients (retrieved using the row/column index)
        for (int i = 0; i < matrix.getRowDimension(); i++) {
            for (int j = 0; j < matrix.getColumnDimension(); j++) {
                final double rhoIJ = covariance.getCorrelationCoefficient(i, j);
                CheckUtils.checkEquality(expected.getEntry(i, j), rhoIJ, ABSTOL, RELTOL);
            }
        }

        // Correlation coefficients (retrieved using the parameter descriptors)
        for (int i = 0; i < matrix.getRowDimension(); i++) {
            final ParameterDescriptor parameterDescriptorI = new ParameterDescriptor(
                    PARAMETER_NAME_PREFIX + i);
            for (int j = 0; j < matrix.getColumnDimension(); j++) {
                final ParameterDescriptor parameterDescriptorJ = new ParameterDescriptor(
                        PARAMETER_NAME_PREFIX + j);
                final double rhoIJ = covariance.getCorrelationCoefficient(parameterDescriptorI,
                        parameterDescriptorJ);
                CheckUtils.checkEquality(expected.getEntry(i, j), rhoIJ, ABSTOL, RELTOL);
            }
        }
    }

    /**
     * Tests the computation of the correlation coefficients when the provided row index is not a
     * valid index.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain Covariance#getCorrelationCoefficient(int, int)}<br>
     * </p>
     */
    @Test
    public void testCorrelationCoefficientInvalidRowIndex() {
        // Covariance matrix
        final double[][] data = { { 1.86378, 0.17573, 0.32238 }, { 0.17573, 1.23644, 0.4617 },
                { 0.32238, 0.4617, 1.43763 } };
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, data);
        final Covariance covariance = new Covariance(matrix);

        // Row index lower than 0
        try {
            covariance.getCorrelationCoefficient(-1, 0);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_ROW_INDEX_FORMAT, -1);
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // Row index greater than 2
        try {
            covariance.getCorrelationCoefficient(3, 0);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_ROW_INDEX_FORMAT, 3);
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the computation of the correlation coefficients when the provided column index is not a
     * valid index.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain Covariance#getCorrelationCoefficient(int, int)}<br>
     * </p>
     */
    @Test
    public void testCorrelationCoefficientInvalidColumnIndex() {
        // Covariance matrix
        final double[][] data = { { 4.90442, -2.40088, -0.53763 }, { -2.40088, 3.87369, -0.23737 },
                { -0.53763, -0.23737, 2.44149 } };
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, data);
        final Covariance covariance = new Covariance(matrix);

        // Column index lower than 0
        try {
            covariance.getCorrelationCoefficient(0, -1);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_COLUMN_INDEX_FORMAT, -1);
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // Column index greater than 2
        try {
            covariance.getCorrelationCoefficient(0, 3);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_COLUMN_INDEX_FORMAT, 3);
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the computation of the standard deviation when the provided parameter descriptor is not
     * associated with the covariance matrix.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain Covariance#getCorrelationCoefficient(ParameterDescriptor, ParameterDescriptor)}<br>
     * </p>
     */
    @Test
    public void testCorrelationCoefficientInvalidDescriptor() {
        // Covariance matrix
        final double[][] data = { { 5.14635, -0.05623, -0.80188 }, { -0.05623, 8.1558, 0.17517 },
                { -0.80188, 0.17517, 3.76994 } };
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, data);
        final Covariance covariance = new Covariance(matrix);

        // Invalid parameter descriptor (row)
        try {
            covariance.getCorrelationCoefficient(new ParameterDescriptor("x"),
                    new ParameterDescriptor("p0"));
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            final String expectedMessage = String.format(WRONG_PARAMETER_DESCRIPTOR_FORMAT, "x");
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // Invalid parameter descriptor (column)
        try {
            covariance.getCorrelationCoefficient(new ParameterDescriptor("p0"),
                    new ParameterDescriptor("x"));
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            final String expectedMessage = String.format(WRONG_PARAMETER_DESCRIPTOR_FORMAT, "x");
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Test the quadratic multiplication.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain Covariance#quadraticMultiplication(RealMatrix, Collection)}<br>
     * {@linkplain Covariance#quadraticMultiplication(RealMatrix, Collection, boolean)}<br>
     * </p>
     */
    @Test
    public void testQuadraticMultiplication() {
        double[][] data;
        Covariance result;
        Covariance expected;
        List<ParameterDescriptor> expectedDescriptors;

        // Initial parameter descriptors
        final List<ParameterDescriptor> initialDescriptors = new ArrayList<>();
        initialDescriptors.add(new ParameterDescriptor("c1"));
        initialDescriptors.add(new ParameterDescriptor("c2"));
        initialDescriptors.add(new ParameterDescriptor("c3"));

        // Initial covariance
        data = new double[][] { { 4.33553, -2.38325, -0.59474 }, { -2.38325, 5.45334, -2.50262 },
                { -0.59474, -2.50262, 7.87554 } };
        final SymmetricPositiveMatrix initialMatrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, data);
        final Covariance covariance = new Covariance(initialMatrix, initialDescriptors);

        // Matrix used for the quadratic multiplication (3x4)
        data = new double[][] { { -1.69148, -9.76464, -6.77547 }, { 3.80326, 5.65757, 2.22957 },
                { 6.79814, -3.40359, 2.28712 }, { -3.35997, 9.29307, -8.34709 } };
        final RealMatrix jacobians = new Array2DRowRealMatrix(data);

        // Expected covariance matrix
        data = new double[][] {
                { 470.4071471064641, -168.82698812925346, 181.7008714258779, -133.87816352719233 },
                { -168.82698812925346, 100.62875552023883, -41.1453729904925, 135.48329551770922 },
                { 181.7008714258779, -41.1453729904925, 435.4916824637588, -685.659085111525 },
                { -133.87816352719233, 135.48329551770922, -685.659085111525, 1572.3499950399555 } };
        final SymmetricPositiveMatrix expectedMatrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, data);

        // Test quadratic multiplication when the new
        // parameter descriptors are not specified
        expectedDescriptors = new ArrayList<>();
        expectedDescriptors.add(new ParameterDescriptor("p0"));
        expectedDescriptors.add(new ParameterDescriptor("p1"));
        expectedDescriptors.add(new ParameterDescriptor("p2"));
        expectedDescriptors.add(new ParameterDescriptor("p3"));

        result = covariance.quadraticMultiplication(jacobians);
        expected = new Covariance(expectedMatrix, expectedDescriptors);
        checkEquality(expected, result, ABSTOL, RELTOL);

        result = covariance.quadraticMultiplication(jacobians, false);
        expected = new Covariance(expectedMatrix, expectedDescriptors);
        checkEquality(expected, result, ABSTOL, RELTOL);

        result = covariance.quadraticMultiplication(jacobians.transpose(), true);
        expected = new Covariance(expectedMatrix, expectedDescriptors);
        checkEquality(expected, result, ABSTOL, RELTOL);

        // Test quadratic multiplication when the
        // new parameter descriptors are specified
        expectedDescriptors = new ArrayList<>();
        expectedDescriptors.add(new ParameterDescriptor("parameter n°0"));
        expectedDescriptors.add(new ParameterDescriptor("parameter n°1"));
        expectedDescriptors.add(new ParameterDescriptor("parameter n°2"));
        expectedDescriptors.add(new ParameterDescriptor("parameter n°3"));

        result = covariance.quadraticMultiplication(jacobians, expectedDescriptors);
        expected = new Covariance(expectedMatrix, expectedDescriptors);
        checkEquality(expected, result, ABSTOL, RELTOL);

        result = covariance.quadraticMultiplication(jacobians, expectedDescriptors, false);
        expected = new Covariance(expectedMatrix, expectedDescriptors);
        checkEquality(expected, result, ABSTOL, RELTOL);

        result = covariance.quadraticMultiplication(jacobians.transpose(), expectedDescriptors,
                true);
        expected = new Covariance(expectedMatrix, expectedDescriptors);
        checkEquality(expected, result, ABSTOL, RELTOL);
    }

    /**
     * Test the addition of another covariance matrix.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain Covariance#add(SymmetricPositiveMatrix)}<br>
     * </p>
     */
    @Test
    public void testAddMatrix() {
        double[][] data;
        SymmetricPositiveMatrix matrix;

        // Initial covariance
        data = new double[][] { { 4.76139, 1.12315, 0.49826 }, { 1.12315, 6.50572, -1.50203 },
                { 0.49826, -1.50203, 8.05317 } };
        matrix = new ArrayRowSymmetricPositiveMatrix(SymmetryType.LOWER, data);
        final Covariance covariance1 = new Covariance(matrix);

        // Covariance matrix to be added
        data = new double[][] { { 6.73019, -1.26835, 0.64269 }, { -1.26835, 6.4915, 0.44554 },
                { 0.64269, 0.44554, 8.88272 } };
        final SymmetricPositiveMatrix sdp = new ArrayRowSymmetricPositiveMatrix(SymmetryType.LOWER,
                data);

        // Expected covariance matrix
        data = new double[][] { { 11.49158, -0.1452, 1.14095 }, { -0.1452, 12.99722, -1.05649 },
                { 1.14095, -1.05649, 16.93589 } };
        matrix = new ArrayRowSymmetricPositiveMatrix(SymmetryType.LOWER, data);
        final Covariance expected = new Covariance(matrix);

        // Test addition
        final Covariance result = covariance1.add(sdp);
        checkEquality(expected, result, ABSTOL, RELTOL);
    }

    /**
     * Test the multiplication by a positive scalar.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain Covariance#positiveScalarMultiply(double)}
     * </p>
     */
    @Test
    public void testPositiveScalarMultiply() {
        double[][] data;
        SymmetricPositiveMatrix matrix;

        // Initial covariance
        data = new double[][] { { 3.53724, 0.54984, 0.7962 }, { 0.54984, 5.81217, -3.46858 },
                { 0.7962, -3.46858, 6.29766 } };
        matrix = new ArrayRowSymmetricPositiveMatrix(SymmetryType.LOWER, data);
        final Covariance covariance = new Covariance(matrix);

        // Expected covariance matrix
        data = new double[][] { { 7.07448, 1.09968, 1.5924 }, { 1.09968, 11.62434, -6.93716 },
                { 1.5924, -6.93716, 12.59532 } };
        matrix = new ArrayRowSymmetricPositiveMatrix(SymmetryType.LOWER, data);
        final Covariance expected = new Covariance(matrix);

        // Test multiplication by a positive scalar
        final Covariance result = covariance.positiveScalarMultiply(2.0);
        checkEquality(expected, result, ABSTOL, RELTOL);

        // Test the multiplication by a negative scalar (must fail)
        try {
            covariance.positiveScalarMultiply(-Double.MIN_VALUE);
            Assert.fail();
        } catch (final NotPositiveException e) {
            final String expectedString = "invalid scalar -0 (must be positive)";
            Assert.assertEquals(expectedString, e.getMessage());
        }
    }

    /**
     * Test the computation of the Mahalanobis distance.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain Covariance#getMahalanobisDistance(RealVector)}
     * </p>
     */
    @Test
    public void testMahalanobisDistance() {
        RealVector point;
        double mahalanobisDistance;

        // Initial covariance
        final double[][] data = { { 7.46775, 0.12555, 0.28984 }, { 0.12555, 8.05915, 1.02183 },
                { 0.28984, 1.02183, 9.06848 } };
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, data);
        final Covariance covariance = new Covariance(matrix);

        // Test the computation of the Mahalanobis distance
        point = new ArrayRealVector(new double[] { 0.0, 0.0, 0.0 });
        mahalanobisDistance = covariance.getMahalanobisDistance(point);
        CheckUtils.checkEquality(0.0, mahalanobisDistance, ABSTOL, RELTOL);

        point = new ArrayRealVector(new double[] { 1.0, 0.0, 0.0 });
        mahalanobisDistance = covariance.getMahalanobisDistance(point);
        CheckUtils.checkEquality(+3.661898279644082E-01, mahalanobisDistance, ABSTOL, RELTOL);

        point = new ArrayRealVector(new double[] { 0.0, 1.0, 0.0 });
        mahalanobisDistance = covariance.getMahalanobisDistance(point);
        CheckUtils.checkEquality(+3.548229562781701E-01, mahalanobisDistance, ABSTOL, RELTOL);

        point = new ArrayRealVector(new double[] { 0.0, 0.0, 1.0 });
        mahalanobisDistance = covariance.getMahalanobisDistance(point);
        CheckUtils.checkEquality(+3.346584032788348E-01, mahalanobisDistance, ABSTOL, RELTOL);
    }

    /**
     * Tests the method that extracts parts of the covariance matrix by specifying the indices of
     * the row/columns to be retrieved.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain Covariance#getSubCovariance(int[])}
     * </p>
     */
    @Test
    public void testGetSubCovarianceByIndex() {
        double[][] data;
        int[] indices;
        Covariance result;
        Covariance expected;
        SymmetricPositiveMatrix matrix;
        List<ParameterDescriptor> parameterDescriptors;

        // Covariance matrix
        data = new double[][] { { 1.03018, -0.52697, 0.07221, -1.00215 },
                { -0.52697, 2.92375, 0.39976, -1.69065 }, { 0.07221, 0.39976, 3.02665, -1.37601 },
                { -1.00215, -1.69065, -1.37601, 3.28196 } };
        matrix = new ArrayRowSymmetricPositiveMatrix(SymmetryType.LOWER, data);
        final Covariance covariance = new Covariance(matrix);

        // Standard case
        indices = new int[] { 0, 2 };
        result = covariance.getSubCovariance(indices);
        data = new double[][] { { 1.03018, 0.07221 }, { 0.07221, 3.02665 } };
        matrix = new ArrayRowSymmetricPositiveMatrix(SymmetryType.LOWER, data);
        parameterDescriptors = new ArrayList<>();
        parameterDescriptors.add(covariance.getParameterDescriptor(0));
        parameterDescriptors.add(covariance.getParameterDescriptor(2));
        expected = new Covariance(matrix, parameterDescriptors);
        checkEquality(expected, result, 0., 0.);

        // Permutation
        indices = new int[] { 0, 2, 3, 1 };
        result = covariance.getSubCovariance(indices);
        data = new double[][] { { 1.03018, 0.07221, -1.00215, -0.52697 },
                { 0.07221, 3.02665, -1.37601, 0.39976 }, { -1.00215, -1.37601, 3.28196, -1.69065 },
                { -0.52697, 0.39976, -1.69065, 2.92375 } };
        matrix = new ArrayRowSymmetricPositiveMatrix(SymmetryType.LOWER, data);
        parameterDescriptors = new ArrayList<>();
        parameterDescriptors.add(covariance.getParameterDescriptor(0));
        parameterDescriptors.add(covariance.getParameterDescriptor(2));
        parameterDescriptors.add(covariance.getParameterDescriptor(3));
        parameterDescriptors.add(covariance.getParameterDescriptor(1));
        expected = new Covariance(matrix, parameterDescriptors);
        checkEquality(expected, result, 0., 0.);

        // With duplicates
        try {
            indices = new int[] { 0, 1, 0 };
            covariance.getSubCovariance(indices);
            Assert.fail();
        } catch (final MathIllegalArgumentException e) {
            final String expectedMessage = "Element : 0 is duplicated";
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // Invalid index
        try {
            indices = new int[] { -1 };
            covariance.getSubCovariance(indices);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = "row index (-1)";
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            indices = new int[] { 4 };
            covariance.getSubCovariance(indices);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = "row index (4)";
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the method that extracts parts of the covariance matrix by specifying the parameter
     * descriptors associated with the row/columns to be retrieved.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain Covariance#getSubCovariance(Collection)}
     * </p>
     */
    @Test
    public void testGetSubCovarianceByParameterDescriptor() {
        double[][] data;
        Covariance result;
        Covariance expected;
        SymmetricPositiveMatrix matrix;
        List<ParameterDescriptor> parameterDescriptors;

        // Covariance matrix
        data = new double[][] { { 6.76789, -0.42727, -1.15846, -2.14264 },
                { -0.42727, 2.99393, -0.24115, 0.47056 },
                { -1.15846, -0.24115, 2.85927, -0.07078 }, { -2.14264, 0.47056, -0.07078, 6.78934 } };
        matrix = new ArrayRowSymmetricPositiveMatrix(SymmetryType.LOWER, data);
        final Covariance covariance = new Covariance(matrix);

        // Standard case
        parameterDescriptors = new ArrayList<>();
        parameterDescriptors.add(covariance.getParameterDescriptor(0));
        parameterDescriptors.add(covariance.getParameterDescriptor(2));
        result = covariance.getSubCovariance(parameterDescriptors);
        data = new double[][] { { 6.76789, -1.15846 }, { -1.15846, 2.85927 } };
        matrix = new ArrayRowSymmetricPositiveMatrix(SymmetryType.LOWER, data);
        expected = new Covariance(matrix, parameterDescriptors);
        checkEquality(expected, result, 0., 0.);

        // Permutation
        parameterDescriptors = new ArrayList<>();
        parameterDescriptors.add(covariance.getParameterDescriptor(0));
        parameterDescriptors.add(covariance.getParameterDescriptor(2));
        parameterDescriptors.add(covariance.getParameterDescriptor(3));
        parameterDescriptors.add(covariance.getParameterDescriptor(1));
        result = covariance.getSubCovariance(parameterDescriptors);
        data = new double[][] { { 6.76789, -1.15846, -2.14264, -0.42727 },
                { -1.15846, 2.85927, -0.07078, -0.24115 },
                { -2.14264, -0.07078, 6.78934, 0.47056 }, { -0.42727, -0.24115, 0.47056, 2.99393 } };
        matrix = new ArrayRowSymmetricPositiveMatrix(SymmetryType.LOWER, data);
        expected = new Covariance(matrix, parameterDescriptors);
        checkEquality(expected, result, 0., 0.);

        // With duplicates
        try {
            parameterDescriptors = new ArrayList<>();
            parameterDescriptors.add(covariance.getParameterDescriptor(0));
            parameterDescriptors.add(covariance.getParameterDescriptor(1));
            parameterDescriptors.add(covariance.getParameterDescriptor(0));
            covariance.getSubCovariance(parameterDescriptors);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            final String expectedMessage = String.format(DUPLICATE_PARAMETER_DESCRIPTOR_FORMAT);
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // Invalid parameter descriptor
        try {
            parameterDescriptors = new ArrayList<>();
            parameterDescriptors.add(new ParameterDescriptor("other descriptor"));
            covariance.getSubCovariance(parameterDescriptors);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            final String expectedMessage = "The provided parameter descriptor (other descriptor) is not associated with this covariance matrix";
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Test the copy of a covariance. The covariance copied should be identical to the initial
     * covariance and stored in a different instance.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain Covariance#copy()}
     * </p>
     */
    @Test
    public void testCopy() {
        // Initial covariance
        final double[][] data = { { 6.82049, 0.38027, 3.16364 }, { 0.38027, 0.34885, -0.99639 },
                { 3.16364, -0.99639, 6.45579 } };
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, data);
        final Covariance covariance = new Covariance(matrix);

        // Copy the initial covariance:
        // It should contain exactly the same data,
        // stored in a different instance.
        final Covariance copy = covariance.copy();
        checkEquality(covariance, copy, 0, 0);
        Assert.assertFalse(copy == covariance);
    }

    /**
     * Tests the equals and hashCode methods.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain Covariance#equals(Object)}<br>
     * {@linkplain Covariance#hashCode()}<br>
     * </p>
     */
    @Test
    public void testEqualsAndHashCode() {
        double[][] data;
        SymmetricPositiveMatrix matrix;
        Covariance other;

        data = new double[][] { { 5.21737, 2.22453, -0.92646 }, { 2.22453, 1.97164, -0.56579 },
                { -0.92646, -0.56579, 1.20958 } };
        matrix = new ArrayRowSymmetricPositiveMatrix(SymmetryType.LOWER, data);
        final Covariance instance = new Covariance(matrix);

        // Check the hashCode consistency between calls
        final int hashCode = instance.hashCode();
        Assert.assertEquals(hashCode, instance.hashCode());

        // Compared object is null
        Assert.assertFalse(instance.equals(null));

        // Compared object is a different class
        Assert.assertFalse(instance.equals(new Object()));

        // Same instance
        Assert.assertTrue(instance.equals(instance));

        // Same data, but different instances
        other = new Covariance(matrix);
        Assert.assertTrue(instance.equals(other));
        Assert.assertTrue(other.equals(instance));
        Assert.assertTrue(instance.hashCode() == other.hashCode());

        // Different covariance matrix
        data = new double[][] { { 5.74298, 0.48051, -2.39055 }, { 0.48051, 6.03401, -0.47853 },
                { -2.39055, -0.47853, 6.14391 } };
        matrix = new ArrayRowSymmetricPositiveMatrix(SymmetryType.LOWER, data);
        other = new Covariance(matrix);
        Assert.assertFalse(instance.equals(other));
        Assert.assertFalse(other.equals(instance));
        Assert.assertFalse(instance.hashCode() == other.hashCode());
    }

    /**
     * Tests the method that returns a string representation of the covariance.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain Covariance#toString()}<br>
     * {@linkplain Covariance#toString(RealMatrixFormat)}<br>
     * {@linkplain Covariance#toString(RealMatrixFormat, String, String, boolean, boolean)}<br>
     * </p>
     */
    @Test
    public void testToString() {
        String result;
        StringBuilder builder;
        RealMatrixFormat format;

        final double[][] data = { { 6.64932, -0.71215, -0.57605 }, { -0.71215, 6.91963, -0.42339 },
                { -0.57605, -0.42339, 7.27005 } };
        final SymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, data);
        final Covariance covariance = new Covariance(matrix);

        final FieldDescriptor<OrbitalCoordinate> fieldDescriptor = StandardFieldDescriptors.ORBITAL_COORDINATE;
        covariance.getParameterDescriptor(0).addField(fieldDescriptor, CartesianCoordinate.X);
        covariance.getParameterDescriptor(1).addField(fieldDescriptor, CartesianCoordinate.Y);
        covariance.getParameterDescriptor(2).addField(fieldDescriptor, CartesianCoordinate.Z);

        // Default format
        result = covariance.toString();
        builder = new StringBuilder();
        builder.append("Covariance[Parameters: X_p0, Y_p1, Z_p2]");
        Assert.assertEquals(builder.toString(), result);

        // Java format
        format = MatrixUtils.JAVA_FORMAT;
        result = covariance.toString(format);
        builder = new StringBuilder();
        builder.append("Covariance[Parameters: X_p0, Y_p1, Z_p2;");
        builder.append(System.lineSeparator());
        builder.append("          {{6.64932, -0.71215, -0.57605}, {-0.71215, 6.91963, -0.42339}, {-0.57605, -0.42339, 7.27005}}]");
        Assert.assertEquals(builder.toString(), result);

        // Octave format
        format = MatrixUtils.OCTAVE_FORMAT;
        result = covariance.toString(format);
        builder = new StringBuilder();
        builder.append("Covariance[Parameters: X_p0, Y_p1, Z_p2;");
        builder.append(System.lineSeparator());
        builder.append("          [6.64932, -0.71215, -0.57605; -0.71215, 6.91963, -0.42339; -0.57605, -0.42339, 7.27005]]");
        Assert.assertEquals(builder.toString(), result);

        // Scilab format
        format = MatrixUtils.SCILAB_FORMAT;
        result = covariance.toString(format);
        builder = new StringBuilder();
        builder.append("Covariance[Parameters: X_p0, Y_p1, Z_p2;");
        builder.append(System.lineSeparator());
        builder.append("           [6.64932, -0.71215, -0.57605; -0.71215, 6.91963, -0.42339; -0.57605, -0.42339, 7.27005]]");
        Assert.assertEquals(builder.toString(), result);

        // Visual format
        format = MatrixUtils.VISUAL_FORMAT;
        result = covariance.toString(format);
        builder = new StringBuilder();
        builder.append("Covariance[Parameters: X_p0, Y_p1, Z_p2;");
        builder.append(System.lineSeparator());
        builder.append("          [[      6.6493,    -0.71215,    -0.57605]");
        builder.append(System.lineSeparator());
        builder.append("           [    -0.71215,      6.9196,    -0.42339]");
        builder.append(System.lineSeparator());
        builder.append("           [    -0.57605,    -0.42339,      7.2701]]]");
        Assert.assertEquals(builder.toString(), result);

        // Visual format (no class name)
        format = MatrixUtils.VISUAL_FORMAT;
        result = covariance.toString(format, Covariance.DEFAULT_NAME_SEPARATOR,
                Covariance.DEFAULT_FIELD_SEPARATOR, false, false);
        builder = new StringBuilder();
        builder.append("Parameters: p0_X, p1_Y, p2_Z;");
        builder.append(System.lineSeparator());
        builder.append("[[      6.6493,    -0.71215,    -0.57605]");
        builder.append(System.lineSeparator());
        builder.append(" [    -0.71215,      6.9196,    -0.42339]");
        builder.append(System.lineSeparator());
        builder.append(" [    -0.57605,    -0.42339,      7.2701]]");
        Assert.assertEquals(builder.toString(), result);

        // Java format (reverse field descriptors order)
        format = MatrixUtils.JAVA_FORMAT;
        result = covariance.toString(format, Covariance.DEFAULT_NAME_SEPARATOR,
                Covariance.DEFAULT_FIELD_SEPARATOR, true, true);
        builder = new StringBuilder();
        builder.append("Covariance[Parameters: X_p0, Y_p1, Z_p2;");
        builder.append(System.lineSeparator());
        builder.append("          {{6.64932, -0.71215, -0.57605}, {-0.71215, 6.91963, -0.42339}, {-0.57605, -0.42339, 7.27005}}]");
        Assert.assertEquals(builder.toString(), result);

        // Java format (custom name and field separator)
        format = MatrixUtils.JAVA_FORMAT;
        result = covariance.toString(format, " | ", ".", true, true);
        builder = new StringBuilder();
        builder.append("Covariance[Parameters: X.p0 | Y.p1 | Z.p2;");
        builder.append(System.lineSeparator());
        builder.append("          {{6.64932, -0.71215, -0.57605}, {-0.71215, 6.91963, -0.42339}, {-0.57605, -0.42339, 7.27005}}]");
        Assert.assertEquals(builder.toString(), result);
    }

    /**
     * Asserts the equality between two covariances within the specified tolerance.
     *
     * @param expected
     *        the expected covariance
     * @param actual
     *        the covariance tested
     * @param absTol
     *        the absolute tolerance
     * @param relTol
     *        the relative tolerance
     */
    private static void checkEquality(final Covariance expected, final Covariance actual,
            final double absTol, final double relTol) {
        if (expected == null ^ actual == null) {
            Assert.assertEquals(expected, actual);
        } else if (expected != actual) {
            CheckUtils.checkEquality(expected.getCovarianceMatrix(), actual.getCovarianceMatrix(),
                    absTol, relTol);
            CheckUtils.checkEquality(expected.getParameterDescriptors(),
                    actual.getParameterDescriptors());
        }
    }
}
