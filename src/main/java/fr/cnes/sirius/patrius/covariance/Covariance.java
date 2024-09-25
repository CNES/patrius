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
 * VERSION:4.8:DM:DM-3040:15/11/2021:[PATRIUS]Reversement des evolutions de la branche patrius-for-lotus 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.covariance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.NotPositiveException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;
import fr.cnes.sirius.patrius.math.linear.ArrayRealVector;
import fr.cnes.sirius.patrius.math.linear.ArrayRowSymmetricMatrix.SymmetryType;
import fr.cnes.sirius.patrius.math.linear.ArrayRowSymmetricPositiveMatrix;
import fr.cnes.sirius.patrius.math.linear.DiagonalMatrix;
import fr.cnes.sirius.patrius.math.linear.MatrixDimensionMismatchException;
import fr.cnes.sirius.patrius.math.linear.MatrixUtils;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealMatrixFormat;
import fr.cnes.sirius.patrius.math.linear.RealVector;
import fr.cnes.sirius.patrius.math.linear.SingularMatrixException;
import fr.cnes.sirius.patrius.math.linear.SymmetricPositiveMatrix;
import fr.cnes.sirius.patrius.math.parameter.ParameterDescriptor;
import fr.cnes.sirius.patrius.math.parameter.ParameterUtils;
import fr.cnes.sirius.patrius.math.parameter.StandardFieldDescriptors;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Covariance representation.
 * <p>
 * The covariance matrix is stored in a {@link SymmetricPositiveMatrix} object.<br>
 * The covariance columns/rows are described by a list of parameter descriptors.
 * </p>
 *
 * @author Hugo Veuillez (CNES)
 * @author Pierre Seimandi (GMV)
 */
public class Covariance {

    /** Default field separator for the {@code toString()} methods. */
    protected static final String DEFAULT_FIELD_SEPARATOR = "_";

    /** Default name separator for the {@code toString()} methods. */
    protected static final String DEFAULT_NAME_SEPARATOR = ", ";

    /** Short argument description for matrices. */
    private static final String MATRIX = "matrix";

    /** Short argument description for covariance matrices. */
    private static final String COVARIANCE_MATRIX = "covariance matrix";

    /** Short argument description for parameter descriptors. */
    private static final String PARAMETER_DESCRIPTOR = "parameter descriptor";

    /** Short argument description for lists of parameter descriptors. */
    private static final String PARAMETER_DESCRIPTORS_LIST = "parameter descriptors list";

    /** Covariance matrix. */
    private final SymmetricPositiveMatrix covarianceMatrix;

    /** Parameter descriptors associated with the covariance matrix. */
    private final List<ParameterDescriptor> parameterDescriptors;

    /**
     * Creates a new instance using the specified covariance matrix.
     * <p>
     * The rows/columns of the covariance matrix are automatically associated with default parameter
     * descriptors. These descriptors are comprised of a single field descriptor (
     * {@linkplain StandardFieldDescriptors#PARAMETER_NAME PARAMETER_NAME}), mapped to the string
     * "p" + i, where i is the index of the corresponding row/column in the covariance matrix.
     * </p>
     *
     * @param covarianceMatrixIn
     *        the covariance matrix
     * @throws NullPointerException
     *         if the provided covariance matrix is {@code null}
     * @see ParameterUtils#buildDefaultParameterDescriptors(int)
     */
    public Covariance(final SymmetricPositiveMatrix covarianceMatrixIn) {
        this(covarianceMatrixIn, null);
    }

    /**
     * Creates a new instance using the specified covariance matrix and parameter descriptors.
     * <p>
     * The provided parameter descriptors are automatically associated with the rows/colums of the
     * covariance matrix (in the order they are returned by the collection's iterator). The number
     * of parameter descriptors must match the size the covariance matrix. If the provided
     * collection is {@code null} or empty, default parameter descriptors are used instead. These
     * default descriptors are comprised of a single field descriptor (
     * {@linkplain StandardFieldDescriptors#PARAMETER_NAME PARAMETER_NAME}), mapped to the string
     * "p" + i, where i is the index of the corresponding row/column in the covariance matrix.
     * </p>
     * <p>
     * <em> Note that this constructor performs a shallow copy of the provided parameter descriptors. 
     * The covariance matrix itself is passed by reference.</em>
     * </p>
     *
     * @param covarianceMatrixIn
     *        the covariance matrix
     * @param parameterDescriptorsIn
     *        the parameter descriptors associated with the covariance matrix
     * @throws IllegalArgumentException
     *         if the provided covariance matrix is {@code null}, if the number of parameter
     *         descriptors does not match the size of the covariance
     *         matrix, or if the collection of parameter descriptors contains any duplicate
     * @see ParameterUtils#buildDefaultParameterDescriptors(int)
     */
    public Covariance(final SymmetricPositiveMatrix covarianceMatrixIn,
            final Collection<ParameterDescriptor> parameterDescriptorsIn) {
        requireNonNull(covarianceMatrixIn, COVARIANCE_MATRIX);
        this.covarianceMatrix = covarianceMatrixIn;
        final int dimension = covarianceMatrixIn.getRowDimension();

        // If no parameter descriptors were provided, use the default ones
        if (parameterDescriptorsIn == null || parameterDescriptorsIn.isEmpty()) {
            this.parameterDescriptors = ParameterUtils.buildDefaultParameterDescriptors(dimension);
        } else {
            checkParameterDescriptors(parameterDescriptorsIn, dimension);
            this.parameterDescriptors = copy(parameterDescriptorsIn);
        }
    }

    /**
     * Checks an object and throws an exception if it is {@code null}.
     *
     * @param object
     *        the object to be checked
     * @param description
     *        a short description of the object
     * @param <T>
     *        the type of the object
     * @return the specified object, if it is not {@code null}
     * @throws IllegalArgumentException
     *         if the object is {@code null}
     */
    private static <T> T requireNonNull(final T object, final String description) {
        if (object == null) {
            throw PatriusException.createIllegalArgumentException(
                    PatriusMessages.NULL_NOT_ALLOWED_DESCRIPTION, description);
        }
        return object;
    }

    /**
     * Checks a collection of parameter descriptors and throws an exception if it is {@code null},
     * if it does not have the expected size, or if it contains any duplicate or empty parameter
     * descriptor.
     *
     * @param parameterDescriptors
     *        the collection of parameter descriptors to be checked
     * @param expectedSize
     *        the expected size
     * @throws IllegalArgumentException
     *         if the provided collection is {@code null}, if the provided collection does not have
     *         the expected size, or if it contains any duplicate
     */
    private static void checkParameterDescriptors(
            final Collection<ParameterDescriptor> parameterDescriptors, final int expectedSize) {
        // Ensure the collection is not null
        requireNonNull(parameterDescriptors, PARAMETER_DESCRIPTORS_LIST);

        // Ensure the collection has the expected size
        if (parameterDescriptors.size() != expectedSize) {
            throw PatriusException.createIllegalArgumentException(
                    PatriusMessages.INVALID_PARAM_DESCRIPTORS_NUMBER_COVARIANCE_SIZE,
                    parameterDescriptors.size(), expectedSize);
        }

        // Ensure the collection does not contain duplicates
        if (new HashSet<>(parameterDescriptors).size() != parameterDescriptors.size()) {
            throw PatriusException
                    .createIllegalArgumentException(PatriusMessages.PARAM_DESCRIPTORS_COLLECTION_DUPLICATES);
        }

        // Ensure the collection does not contain any empty parameter descriptors
        int index = 0;
        for (final ParameterDescriptor parameterDescriptor : parameterDescriptors) {
            if (parameterDescriptor == null) {
                throw PatriusException.createIllegalArgumentException(
                        PatriusMessages.NULL_PARAM_DESCRIPTOR, index);
            } else if (parameterDescriptor.isEmpty()) {
                throw PatriusException.createIllegalArgumentException(
                        PatriusMessages.EMPTY_PARAM_DESCRIPTOR, index);
            }
            index++;
        }
    }

    /**
     * Performs a copy of the provided parameter descriptors and returns them into a new list.
     * <p>
     * Note that the copy of the parameter descriptors is a shallow copy: the associated field
     * descriptors and the values mapped to them are copied by reference.
     * </p>
     *
     * @param parameterDescriptors
     *        the parameter descriptors to be copied
     * @return a new list containing a copy of the provided parameter descriptors, or {@code null}
     *         if the provided collection is {@code null}
     * @see ParameterDescriptor#copy()
     */
    private static List<ParameterDescriptor> copy(
            final Collection<ParameterDescriptor> parameterDescriptors) {
        List<ParameterDescriptor> copy = null;

        if (parameterDescriptors != null) {
            copy = new ArrayList<>(parameterDescriptors.size());
            for (final ParameterDescriptor parameterDescriptor : parameterDescriptors) {
                // parameterDescriptor shouldn't be null here as previous safety check should be
                // made
                copy.add(parameterDescriptor.copy());
            }
        }

        return copy;
    }

    /**
     * Gets the index of a given parameter descriptor in the list stored internally.
     *
     * @param parameterDescriptor
     *        the parameter descriptor whose index must be retrieved
     * @return the index of the parameter descriptor in the list stored internally.
     * @throws IllegalArgumentException
     *         if the parameter descriptor is not associated with this covariance matrix
     */
    protected int indexOf(final ParameterDescriptor parameterDescriptor) {
        final int index = this.parameterDescriptors.indexOf(parameterDescriptor);
        if (index < 0) {
            throw PatriusException.createIllegalArgumentException(
                    PatriusMessages.PARAM_DESCRIPTOR_NOT_ASSOCIATED_WITH_COVARIANCE,
                    parameterDescriptor.getName());
        }

        return index;
    }

    /**
     * Gets the size of the covariance matrix.
     *
     * @return the size of the covariance matrix
     */
    public int getSize() {
        return this.covarianceMatrix.getRowDimension();
    }

    /**
     * Gets the covariance matrix.
     * <p>
     * A covariance matrix is symmetric positive semi-definite by definition. Whether these
     * properties are actually respected depends on the implementation of
     * {@linkplain SymmetricPositiveMatrix} used to store the matrix.
     * </p>
     * <p>
     * <em>Note that this method provides a direct access to the covariance matrix stored internally, 
     * which is possibly mutable.</em>
     * </p>
     *
     * @return the covariance matrix
     */
    public SymmetricPositiveMatrix getCovarianceMatrix() {
        return this.covarianceMatrix;
    }

    /**
     * Gets the parameter descriptors associated with the rows/columns of the covariance matrix.
     * <p>
     * <em> Note that this methods returns a shallow copy of the list of parameter descriptors stored internally. 
     * However, this list provides a direct access to the parameter descriptors stored by this instance, which 
     * are mutable.</em>
     * </p>
     *
     * @return the parameter descriptors associated with the covariance matrix
     */
    public List<ParameterDescriptor> getParameterDescriptors() {
        return new ArrayList<>(this.parameterDescriptors);
    }

    /**
     * Gets the parameter descriptors associated with the specified row/column of the covariance
     * matrix.
     * <p>
     * <em>Note that this method provides a direct access to the parameter descriptors stored internally, 
     * which are mutable.</em>
     * </p>
     *
     * @param index
     *        the row/column index of the parameter descriptor to be retrieved
     * @return the parameter descriptors associated with the covariance matrix
     */
    public ParameterDescriptor getParameterDescriptor(final int index) {
        MatrixUtils.checkRowIndex(this.covarianceMatrix, index);
        return this.parameterDescriptors.get(index);
    }

    /**
     * Extracts the parts of the covariance associated with the specified row/column indices.
     * <p>
     * This method extracts the specified rows/columns associated in order to build a new
     * {@linkplain Covariance} instance. The provided index array may contain duplicates, but an
     * exception will be thrown if it any of the indices is not a valid row/column index. This
     * method can be used to extract any submatrix and/or to perform a symmetric reordering of the
     * rows/columns of the covariance matrix.
     * </p>
     * <p>
     * <b>Important:</b><br>
     * Since a parameter descriptor cannot be associated with multiple rows/columns of the
     * covariance, the provided index array must not contain any duplicate (an exception will be
     * thrown if that occurs).
     * </p>
     *
     * @param indices
     *        the indices of the rows/columns to be extracted
     * @return the part of the covariance associated with the specified indices
     * @throws IllegalArgumentException
     *         if one of the specified indices is not a valid row/column index for this covariance
     *         matrix, or the provided index array contains any duplicate
     */
    public Covariance getSubCovariance(final int[] indices) {
        // Ensure the index array does not contain any duplicate
        MatrixUtils.checkDuplicates(indices);

        // Build the parameter descriptors list to be associated with the new covariance matrix
        final int size = indices.length;
        final List<ParameterDescriptor> extractedParameterDescriptors = new ArrayList<>(size);
        for (final int index : indices) {
            MatrixUtils.checkRowIndex(this.covarianceMatrix, index);
            extractedParameterDescriptors.add(this.parameterDescriptors.get(index));
        }

        // Extract the submatrix and return the new covariance
        final SymmetricPositiveMatrix subMatrix = this.covarianceMatrix.getSubMatrix(indices);
        return new Covariance(subMatrix, extractedParameterDescriptors);
    }

    /**
     * Extracts the parts of the covariance associated with the specified parameter descriptors.
     * <p>
     * This method extracts the rows/columns associated with the specified parameter descriptors in
     * order to build a new {@linkplain Covariance} instance. The provided collection may contain
     * duplicates, but an exception will be thrown if any of the parameter descriptors is not
     * associated with this covariance. This method can be used to extract any submatrix and/or to
     * perform a symmetric reordering of the rows/columns of the covariance matrix.
     * </p>
     * <p>
     * <b>Important:</b><br>
     * Since a parameter descriptor cannot be associated with multiple rows/columns of the
     * covariance matrix, the provided collection must not contain any duplicate (an exception will
     * be thrown if that occurs).
     * </p>
     *
     * @param selectedParameterDescriptors
     *        the parameter descriptors associated with the rows/columns to be extracted
     * @return the part of the covariance associated with the specified parameter descriptors
     * @throws IllegalArgumentException
     *         if one of the provided parameter descriptors is not associated with this covariance
     *         matrix, or if the provided collection contains any duplicate
     */
    public Covariance getSubCovariance(
            final Collection<ParameterDescriptor> selectedParameterDescriptors) {
        // Build the index array from the parameter descriptors
        int index = 0;
        final int size = selectedParameterDescriptors.size();
        final int[] indices = new int[size];
        for (final ParameterDescriptor parameterDescriptor : selectedParameterDescriptors) {
            indices[index++] = indexOf(parameterDescriptor);
        }

        // Extract the submatrix and return the new covariance
        final SymmetricPositiveMatrix subMatrix = this.covarianceMatrix.getSubMatrix(indices);
        return new Covariance(subMatrix, selectedParameterDescriptors);
    }

    /**
     * Gets the variance matrix.
     * <p>
     * The variance matrix is a diagonal matrix which contains the diagonal elements of the
     * covariance matrix.
     * </p>
     *
     * @return the variance matrix
     */
    public DiagonalMatrix getVarianceMatrix() {
        final int n = this.covarianceMatrix.getRowDimension();
        final double[] data = new double[n];

        for (int i = 0; i < n; i++) {
            data[i] = this.getVariance(i);
        }

        return new DiagonalMatrix(data, false);
    }

    /**
     * Gets the variance &sigma;<sub>i</sub>&nbsp;=&nbsp;C<sub>i,i</sub> for the specified
     * row/column index.
     *
     * @param index
     *        the row/column index
     * @return the variance for the specified row/column index
     * @throws IllegalArgumentException
     *         if the provided index is not valid
     */
    public double getVariance(final int index) {
        MatrixUtils.checkMatrixIndex(this.covarianceMatrix, index, index);
        return this.covarianceMatrix.getEntry(index, index);
    }

    /**
     * Gets the variance &sigma;<sub>i</sub>&nbsp;=&nbsp;C<sub>i,i</sub> associated with the
     * specified parameter descriptor.
     *
     * @param parameterDescriptor
     *        the parameter descriptor
     * @return the variance for the specified parameter descriptor
     * @throws IllegalArgumentException
     *         if the provided collection is {@code null} or if the parameter descriptor is not
     *         associated with this covariance matrix
     */
    public double getVariance(final ParameterDescriptor parameterDescriptor) {
        requireNonNull(parameterDescriptor, PARAMETER_DESCRIPTOR);
        return getVariance(indexOf(parameterDescriptor));
    }

    /**
     * Gets the standard deviation matrix.
     * <p>
     * The standard deviation matrix is a diagonal matrix which contains the square root of the
     * diagonal elements of this covariance matrix.
     * </p>
     *
     * @return the standard deviation matrix
     */
    public DiagonalMatrix getStandardDeviationMatrix() {
        final int n = this.covarianceMatrix.getRowDimension();
        final double[] data = new double[n];

        for (int i = 0; i < n; i++) {
            data[i] = this.getStandardDeviation(i);
        }

        return new DiagonalMatrix(data, false);
    }

    /**
     * Gets the standard deviation &sigma;<sub>i</sub>&nbsp;=&nbsp;sqrt(C<sub>i,i</sub>) for the
     * specified row/column index.
     *
     * @param index
     *        the row/column index
     * @return the standard deviation for the specified row/column index
     * @throws OutOfRangeException
     *         if the provided index is not valid
     */
    public double getStandardDeviation(final int index) {
        return MathLib.sqrt(this.getVariance(index));
    }

    /**
     * Gets the standard deviation &sigma;<sub>i</sub>&nbsp;=&nbsp;sqrt(C<sub>i,i</sub>) associated
     * with the specified parameter descriptor.
     *
     * @param parameterDescriptor
     *        the parameter descriptor
     * @return the standard deviation of the specified parameter descriptor
     * @throws IllegalArgumentException
     *         if the parameter descriptor is not associated with this covariance matrix
     */
    public double getStandardDeviation(final ParameterDescriptor parameterDescriptor) {
        return MathLib.sqrt(this.getVariance(parameterDescriptor));
    }

    /**
     * Gets the correlation matrix.
     * <p>
     * The correlation matrix is a symmetric positive matrix which contains the correlation
     * coefficients of this covariance matrix. The correlation coefficients for the diagonal
     * elements are equal to 1 by definition. For off-diagonal elements, they are equal to
     * C<sub>i,j</sub>/&sigma;<sub>i</sub>&sigma;<sub>j</sub>, where &sigma;<sub>i</sub> and
     * &sigma;<sub>j</sub> are the standard deviations for the i<sup>th</sup> and j<sup>th</sup>
     * elements.
     * </p>
     *
     * @return the correlation coefficients matrix
     */
    public SymmetricPositiveMatrix getCorrelationCoefficientsMatrix() {
        // Initialization
        final int n = this.covarianceMatrix.getRowDimension();
        final double[][] data = new double[n][n];

        // Get correlation coefficient
        for (int i = 0; i < n; i++) {
            data[i][i] = this.getCorrelationCoefficient(i, i);

            for (int j = 0; j < i; j++) {
                data[i][j] = this.getCorrelationCoefficient(i, j);
            }
        }

        // Build matrix
        return new ArrayRowSymmetricPositiveMatrix(SymmetryType.LOWER, data, null, null, null, null);
    }

    /**
     * Gets the correlation coefficient &rho;<sub>i,j</sub> for the specified row and column
     * indexes.
     * <p>
     * If i is equal to j, &rho;<sub>i,j</sub> is equal to 1 by definition. Otherwise,
     * &rho;<sub>i,j</sub> is equal to C<sub>i,j</sub> / (&sigma;<sub>i</sub>*&sigma;<sub>j</sub>).
     * If the division cannot be computed because &sigma;<sub>i</sub>*&sigma;<sub>j</sub> is too
     * small, &rho;<sub>i,j</sub> is set to 0.
     * </p>
     *
     * @param row
     *        the row index
     * @param column
     *        the column index
     * @return the correlation coefficient for the specified row and column
     * @throws IllegalArgumentException
     *         if one of the provided row/column indexes is not valid
     */
    public double getCorrelationCoefficient(final int row, final int column) {
        MatrixUtils.checkMatrixIndex(this.covarianceMatrix, row, column);
        double rhoIJ = 0.;

        if (row != column) {
            // Compute sigmaI * sigmaJ
            final double sigmaIJ = MathLib.sqrt(this.getVariance(row) * this.getVariance(column));

            // If sigmaI * sigmaJ is 0, then consider the correlation coefficient is 0. Otherwise,
            // compute it.
            if (sigmaIJ != 0.) {
                rhoIJ = MathLib.divide(this.covarianceMatrix.getEntry(row, column), sigmaIJ);
            }
        } else {
            // If i=j, the correlation coefficient is 1 by definition
            rhoIJ = 1.;
        }

        return rhoIJ;
    }

    /**
     * Gets the correlation coefficient &rho;<sub>i,j</sub> associated with the specified parameter
     * descriptors, the first parameter* descriptor being mapped to the rows of the covariance
     * matrix (index "i"), while the second parameter descriptor is mapped to the columns of the
     * covariance matrix (index "j").
     * <p>
     * If i is equal to j, &rho;<sub>i,j</sub> is equal to 1 by definition. Otherwise,
     * &rho;<sub>i,j</sub> is equal to C<sub>i,j</sub> / (&sigma;<sub>i</sub>*&sigma;<sub>j</sub>).
     * If the division cannot be computed because &sigma;<sub>i</sub>*&sigma;<sub>j</sub> is too
     * small, &rho;<sub>i,j</sub> is set to 0.
     * </p>
     *
     * @param parameterDescriptor1
     *        the first parameter descriptor
     * @param parameterDescriptor2
     *        the second parameter descriptor
     * @return the correlation coefficient associated with the provided parameter descriptors
     * @throws IllegalArgumentException
     *         if one of the parameter descriptors is not associated with this covariance matrix
     */
    public double getCorrelationCoefficient(final ParameterDescriptor parameterDescriptor1,
            final ParameterDescriptor parameterDescriptor2) {
        return getCorrelationCoefficient(indexOf(parameterDescriptor1),
                indexOf(parameterDescriptor2));
    }

    /**
     * Gets the result of the quadratic multiplication M&times;C&times;M<sup>T</sup>, where C is
     * this covariance matrix and M is the provided matrix, and associates the computed matrix with
     * default parameter descriptors.
     *
     * @param m
     *        the matrix M
     * @return M&times;C&times;M<sup>T</sup>
     * @throws NullArgumentException
     *         if the matrix M is @{@code null}
     * @throws DimensionMismatchException
     *         if this matrix and the matrices M and M<sup>T</sup> are not multiplication compatible
     */
    public Covariance quadraticMultiplication(final RealMatrix m) {
        return this.quadraticMultiplication(m, null);
    }

    /**
     * Gets the result of the quadratic multiplication M&times;C&times;M<sup>T</sup>, where C is
     * this covariance matrix and M or M<sup>T</sup> is the provided matrix, and associates the
     * computed matrix with default parameter descriptors.
     *
     * @param m
     *        the matrix M
     * @param isTranspose
     *        if {@code true}, assume the matrix provided is M<sup>T</sup>, otherwise assume it is M
     * @return M&times;C&times;M<sup>T</sup>
     * @throws NullArgumentException
     *         if the matrix M is @{@code null}
     * @throws DimensionMismatchException
     *         if this matrix and the matrices M and M<sup>T</sup> are not multiplication compatible
     */
    public Covariance quadraticMultiplication(final RealMatrix m, final boolean isTranspose) {
        return this.quadraticMultiplication(m, null, isTranspose);
    }

    /**
     * Gets the result of the quadratic multiplication M&times;C&times;M<sup>T</sup>, where C is
     * this covariance matrix and M is the provided matrix, and associates the computed matrix with
     * the specified parameter descriptors.
     *
     * @param m
     *        the matrix M
     * @param newParameterDescriptors
     *        the parameter descriptors to be associated with the computed covariance matrix
     * @return M&times;C&times;M<sup>T</sup>
     * @throws NullArgumentException
     *         if the matrix M is @{@code null}
     * @throws DimensionMismatchException
     *         if this matrix and the matrices M and M<sup>T</sup> are not multiplication compatible
     * @throws IllegalArgumentException
     *         if the number of parameter descriptors does not match the size of the covariance
     *         matrix, or if the collection of parameter descriptors contains any duplicate
     */
    public Covariance quadraticMultiplication(final RealMatrix m,
            final Collection<ParameterDescriptor> newParameterDescriptors) {
        return this.quadraticMultiplication(m, newParameterDescriptors, false);
    }

    /**
     * Gets the result of the quadratic multiplication M&times;C&times;M<sup>T</sup>, where C is
     * this covariance matrix and M or M<sup>T</sup> is the provided matrix, and associates the
     * computed matrix with the specified parameter descriptors.
     *
     * @param m
     *        the matrix M
     * @param newParameterDescriptors
     *        the parameter descriptors to be associated with the computed covariance matrix
     * @param isTranspose
     *        if {@code true}, assume the matrix provided is M<sup>T</sup>, otherwise assume it is M
     * @return M&times;C&times;M<sup>T</sup>
     * @throws NullArgumentException
     *         if the matrix M is @{@code null}
     * @throws DimensionMismatchException
     *         if this matrix and the matrix M or M<sup>T</sup> is not multiplication compatible
     * @throws IllegalArgumentException
     *         if the provided matrix M is {@code null}, if the number of parameter descriptors does
     *         not match the size of the covariance matrix, or if the collection of parameter
     *         descriptors contains any duplicate
     */
    public Covariance
            quadraticMultiplication(final RealMatrix m,
                    final Collection<ParameterDescriptor> newParameterDescriptors,
                    final boolean isTranspose) {
        requireNonNull(m, MATRIX);
        MatrixUtils.checkMultiplicationCompatible(this.covarianceMatrix, m, !isTranspose);

        // Return the covariance matrix resulting from the quadratic multiplication.
        // The consistency between the number of parameter descriptors and the dimension
        // of the new covariance matrix is automatically checked at by the constructor.
        return new Covariance(this.covarianceMatrix.quadraticMultiplication(m, isTranspose),
                newParameterDescriptors);
    }

    /**
     * Adds the symmetric positive semi-definite matrix M to this covariance matrix and returns a
     * new {@linkplain Covariance} instance associated with the computed matrix and the same
     * parameter descriptors.
     *
     * @param m
     *        the covariance matrix M to be added
     * @return a new {@linkplain Covariance} instance whose matrix is the sum of the two covariance
     *         matrices
     * @throws NullArgumentException
     *         if the matrix M is @{@code null}
     * @throws MatrixDimensionMismatchException
     *         if the two matrices are not addition compatible
     */
    public Covariance add(final SymmetricPositiveMatrix m) {
        requireNonNull(m, MATRIX);
        MatrixUtils.checkAdditionCompatible(this.covarianceMatrix, m);
        return new Covariance(this.covarianceMatrix.add(m), this.parameterDescriptors);
    }

    /**
     * Multiplies this covariance matrix by a positive scalar.
     *
     * @param d
     *        the scalar by which the matrix is multiplied (&ge;0)
     * @return this covariance matrix multiplied by the provided scalar
     * @throws NotPositiveException
     *         if the provided scalar is negative
     */
    public Covariance positiveScalarMultiply(final double d) {
        if (d < 0) {
            throw new NotPositiveException(PatriusMessages.NOT_POSITIVE_SCALAR, d);
        }

        return new Covariance(this.covarianceMatrix.positiveScalarMultiply(d),
                this.parameterDescriptors);
    }

    /**
     * Gets the Mahalanobis distance of a point with respect to this covariance matrix, assuming its
     * mean value is zero.
     * <p>
     * The Mahalanobis distance is defined by:<br>
     * dM(P) = sqrt(P<sup>T</sup> x C<sup>-1</sup> x P)<br>
     * with P the point and C the covariance matrix (centered on 0).
     * </p>
     *
     * @param point
     *        the point P
     * @return the Mahalanobis distance of the provided point with respect to this covariance matrix
     * @throws SingularMatrixException
     *         if the covariance matrix is singular
     * @throws DimensionMismatchException
     *         if the provided vector do not have the same size as the covariance matrix
     * @throws ArithmeticException
     *         if the computed Mahalanobis distance is negative
     * @see <a href="https://en.wikipedia.org/wiki/Mahalanobis_distance"> Mahalanobis distance
     *      (wikipedia)</a>
     */
    public double getMahalanobisDistance(final RealVector point) {
        final RealVector mean = new ArrayRealVector(point.getDimension());
        return getMahalanobisDistance(point, mean);
    }

    /**
     * Gets the Mahalanobis distance of a point with respect to this covariance matrix.
     * <p>
     * The Mahalanobis distance is defined by:<br>
     * dM(P,M) = sqrt((P-M)<sup>T</sup> x C<sup>-1</sup> x (P-M))<br>
     * with P the point and C the covariance matrix centered on M.
     * </p>
     *
     * @param point
     *        the point
     * @param mean
     *        the mean value of the covariance
     * @return the Mahalanobis distance of the provided point with respect to this covariance matrix
     * @throws SingularMatrixException
     *         if the covariance matrix is singular
     * @throws DimensionMismatchException
     *         if the provided vectors have different sizes, or if they do not have the same size as
     *         the covariance matrix
     * @throws ArithmeticException
     *         if the computed Mahalanobis distance is negative
     * @see <a href="https://en.wikipedia.org/wiki/Mahalanobis_distance">Mahalanobis distance
     *      (wikipedia)</a>
     */
    public double getMahalanobisDistance(final RealVector point, final RealVector mean) {
        // Relative data
        final RealVector relative = point.subtract(mean);

        // Covariance matrix inverse
        final SymmetricPositiveMatrix inverse = this.covarianceMatrix.getInverse();

        // Mahalanobis distance
        return MathLib.sqrt(inverse.preMultiply(relative).dotProduct(relative));
    }

    /**
     * Returns a string representation of this instance which includes the name of the class and the
     * names of the associated parameter descriptors (the covariance matrix itself is not printed).
     *
     * @return string representation of this instance
     */
    @Override
    public String toString() {
        return toString(null);
    }

    /**
     * Returns a string representation of this instance which includes the name of the class (if
     * requested), the names of the associated parameter descriptors and the the covariance matrix
     * (if the specified matrix format is not {@code null}).
     *
     * @param realMatrixFormat
     *        the format to use when printing the covariance matrix
     * @return string representation of this instance
     */
    public String toString(final RealMatrixFormat realMatrixFormat) {
        return toString(realMatrixFormat, DEFAULT_NAME_SEPARATOR, DEFAULT_FIELD_SEPARATOR, true,
            true);
    }

    /**
     * Returns a string representation of this instance which includes the name of the class (if
     * requested), the names of the associated parameter descriptors and the the covariance matrix
     * (if the specified matrix format is not {@code null}).
     *
     * @param realMatrixFormat
     *        the format to use when printing the covariance matrix
     * @param nameSeparator
     *        the string to use as a separator between the names of the parameter descriptors
     * @param fieldSeparator
     *        the string to use as a separator between the field values of a parameter descriptor
     * @param printClassName
     *        whether or not the name of this class should be printed
     * @param reverseOrder
     *        whether or not the field values of each parameter descriptor should be printed in
     *        reverse order
     * @return string representation of this instance
     */
    public String toString(final RealMatrixFormat realMatrixFormat, final String nameSeparator,
            final String fieldSeparator, final boolean printClassName, final boolean reverseOrder) {
        // Separators
        final String semicolon = ";";
        final String lineSeparator = System.lineSeparator();

        // Build the string
        final StringBuilder builder = new StringBuilder();

        // Class name
        final String className = this.getClass().getSimpleName();
        if (printClassName) {
            builder.append(className);
            builder.append("[");
        }

        // Parameter descriptors
        builder.append("Parameters: ");
        builder.append(ParameterUtils.concatenateParameterDescriptorNames(
                this.parameterDescriptors, nameSeparator, fieldSeparator, reverseOrder));

        // Covariance matrix
        if (realMatrixFormat != null) {
            final String regex = "(?:\\s*[\\n|\\r]+)";
            final String initialString = realMatrixFormat.format(this.covarianceMatrix);

            builder.append(semicolon);
            builder.append(lineSeparator);

            if (!printClassName) {
                builder.append(initialString.replaceAll(regex, lineSeparator));
            } else {
                final int nbSpaces = className.length();
                final String indentation = String.format("%" + nbSpaces + "c", ' ');
                final String[] split = initialString.split(regex);
                final String indentedString = String.join(lineSeparator + indentation, split);
                builder.append(indentation);
                builder.append(indentedString);
            }
        }

        // Closing bracket
        if (printClassName) {
            builder.append("]");
        }

        return builder.toString();
    }

    /**
     * Gets a copy of this {@linkplain Covariance} instance.
     * <p>
     * This method performs a shallow copy of the parameter descriptors list and a deep copy of the
     * covariance matrix.
     * </p>
     *
     * @return a copy of this {@linkplain Covariance} instance
     * @see ParameterDescriptor#copy()
     */
    public Covariance copy() {
        return new Covariance(this.covarianceMatrix.copy(), this.parameterDescriptors);
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object object) {
        boolean isEqual = false;

        if (object == this) {
            isEqual = true;
        } else if ((object != null) && (object.getClass() == this.getClass())) {
            final Covariance other = (Covariance) object;
            isEqual = true;
            isEqual &= Objects.equals(this.covarianceMatrix, other.covarianceMatrix);
            isEqual &= Objects.equals(this.parameterDescriptors, other.parameterDescriptors);
        }

        return isEqual;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hash(this.covarianceMatrix, this.parameterDescriptors);
    }
}
