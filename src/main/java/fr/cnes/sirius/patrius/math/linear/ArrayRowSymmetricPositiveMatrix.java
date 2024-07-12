/**
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
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.8:FA:FA-2941:15/11/2021:[PATRIUS] Correction anomalies suite a DM 2767 
 * VERSION:4.8:DM:DM-3040:15/11/2021:[PATRIUS]Reversement des evolutions de la branche patrius-for-lotus 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.linear;

import java.util.Arrays;
import java.util.function.Function;

import fr.cnes.sirius.patrius.math.exception.MathUnsupportedOperationException;
import fr.cnes.sirius.patrius.math.exception.NotPositiveException;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Symmetric positive semi-definite matrix defined by its lower triangular part.
 * <p>
 * This implementation stores the elements of the lower triangular part of a symmetric matrix. These
 * elements are stored in a 1D array, row after row. For example, for a 3 by 3 matrix we have:<br>
 * (1) = s<sub>1,1</sub>;<br>
 * (2) = s<sub>2,1</sub>; (3) = s<sub>2,2</sub>;<br>
 * (4) = s<sub>3,1</sub>; (5) = s<sub>3,2</sub>; (6) = s<sub>3,3</sub>;<br>
 * </p>
 *
 * <p>
 * The elements actually stored depends on the type of symmetry specified at construction:
 * {@link fr.cnes.sirius.patrius.math.linear.ArrayRowSymmetricMatrix.SymmetryType#LOWER LOWER} or
 * {@link fr.cnes.sirius.patrius.math.linear.ArrayRowSymmetricMatrix.SymmetryType#UPPER UPPER}
 * implies the symmetry is enforced by keeping only the lower or upper triangular part of the
 * matrix, while
 * {@link fr.cnes.sirius.patrius.math.linear.ArrayRowSymmetricMatrix.SymmetryType#MEAN MEAN}
 * enforces the symmetry by computing the mean of the lower and upper elements. The symmetry of the
 * provided matrix is not checked by default, meaning that the symmetry is enforced regardless of
 * the inputs. However, such a check can be triggered by setting the default absolute or relative
 * symmetry threshold to a non-null value, or by specifying any of these thresholds at construction.
 * Note that the default values for these symmetry thresholds are shared with
 * {@link ArrayRowSymmetricMatrix}.
 * </p>
 * <p>
 * In contrast, the positivity of the symmetrized matrix is always checked by default. This check
 * can be disabled by setting the absolute and relative positivity thresholds to {@code null},
 * either by changing the default values or by overriding them at construction. A symmetric matrix
 * is considered to be positive semi-definite if all its pivots are strictly positive, or if the
 * pivot and the associated row/column are equal to zero. Possible numerical errors are taken into
 * account by adding a small value (which depends on the specified tolerances) to the diagonal
 * elements of the matrix. The relative tolerance is relative to the maximum row sum norm of the
 * matrix.
 * </p>
 * <p>
 * <b>Important:</b><br>
 * Since it might induce a loss of positivity or definiteness, modifying any element of the matrix
 * is forbidden.
 * </p>
 *
 * @author Pierre Seimandi (GMV)
 *
 * @see ArrayRowSymmetricMatrix
 */
public class ArrayRowSymmetricPositiveMatrix extends ArrayRowSymmetricMatrix implements
        SymmetricPositiveMatrix {

     /** Serializable UID. */
    private static final long serialVersionUID = 3851562819198760120L;

    /**
     * Default absolute positivity threshold, above which a value is considered to be strictly
     * positive.
     */
    private static Double defaultAbsolutePositivityThreshold = 0.;

    /**
     * Default relative positivity threshold, above which a value is considered to be numerically
     * significant compared to another value.
     */
    private static Double defaultRelativePositivityThreshold = Precision.DOUBLE_COMPARISON_EPSILON;

    /**
     * Builds a new {@link ArrayRowSymmetricPositiveMatrix} of dimension n (filled with zero).
     *
     * @param n
     *        the dimension of the matrix
     */
    public ArrayRowSymmetricPositiveMatrix(final int n) {
        super(n);
    }

    /**
     * Builds a new {@link ArrayRowSymmetricPositiveMatrix} from the provided data, using the
     * default symmetry and positivity thresholds.
     *
     * @param symmetryType
     *        the type of symmetry enforced at construction
     * @param dataIn
     *        the data of the matrix (must be a NxN array)
     * @see #getDefaultRelativeSymmetryThreshold()
     * @see #getDefaultAbsolutePositivityThreshold()
     */
    public ArrayRowSymmetricPositiveMatrix(final SymmetryType symmetryType, final double[][] dataIn) {
        // Do not copy the provided array since the built matrix is only a temporary matrix.
        this(symmetryType, new Array2DRowRealMatrix(dataIn, false),
                getDefaultAbsoluteSymmetryThreshold(), getDefaultRelativeSymmetryThreshold(),
                getDefaultAbsolutePositivityThreshold(), getDefaultRelativePositivityThreshold());
    }

    /**
     * Builds a new {@link ArrayRowSymmetricPositiveMatrix} from the provided data, using the
     * specified symmetry and positivity thresholds.
     * <p>
     * The provided thresholds must either be {@code null}, or set to a positive value. Setting both
     * the absolute and relative thresholds to {@code null} completely disables the associated
     * symmetry or positivity check. Any {@code null} threshold is considered to be equal to zero if
     * the associated check is not disabled.
     * </p>
     *
     * @param symmetryType
     *        the type of symmetry enforced at construction
     * @param dataIn
     *        the data of the matrix (must be a NxN array)
     * @param absoluteSymmetryThreshold
     *        the absolute symmetry threshold, above which off-diagonal elements are considered
     *        different
     * @param relativeSymmetryThreshold
     *        the relative symmetry threshold, above which off-diagonal elements are considered
     *        different
     * @param absolutePositivityThreshold
     *        the absolute positivity threshold, above which a value is considered to be strictly
     *        positive
     * @param relativePositivityThreshold
     *        the relative positivity threshold, above which a value is considered to be numerically
     *        significant when compared to anothervalue
     */
    public ArrayRowSymmetricPositiveMatrix(final SymmetryType symmetryType,
            final double[][] dataIn, final Double absoluteSymmetryThreshold,
            final Double relativeSymmetryThreshold, final Double absolutePositivityThreshold,
            final Double relativePositivityThreshold) {
        // Do not copy the provided array since the built matrix is only a temporary matrix.
        this(symmetryType, new Array2DRowRealMatrix(dataIn, false), absoluteSymmetryThreshold,
                relativeSymmetryThreshold, absolutePositivityThreshold, relativePositivityThreshold);
    }

    /**
     * Builds a new {@link ArrayRowSymmetricPositiveMatrix} from the provided matrix, using the
     * default symmetry and positivity thresholds.
     *
     * @param symmetryType
     *        the type of symmetry enforced at construction
     * @param matrix
     *        the matrix (must NxN)
     * @see #getDefaultRelativeSymmetryThreshold()
     * @see #getDefaultAbsolutePositivityThreshold()
     */
    public ArrayRowSymmetricPositiveMatrix(final SymmetryType symmetryType, final RealMatrix matrix) {
        this(symmetryType, matrix, getDefaultAbsoluteSymmetryThreshold(),
                getDefaultRelativeSymmetryThreshold(), getDefaultAbsolutePositivityThreshold(),
                getDefaultRelativePositivityThreshold());
    }

    /**
     * Builds a new {@link ArrayRowSymmetricPositiveMatrix} from the provided matrix, using the
     * specified symmetry and positivity thresholds.
     * <p>
     * The provided thresholds must either be {@code null}, or set to a positive value. Setting both
     * the absolute and relative thresholds to {@code null} completely disables the associated
     * symmetry or positivity check. Any {@code null} threshold is considered to be equal to zero if
     * the associated check is not disabled.
     * </p>
     *
     * @param symmetryType
     *        the type of symmetry enforced at construction
     * @param matrix
     *        the matrix (must NxN)
     * @param absoluteSymmetryThreshold
     *        the absolute symmetry threshold, above which off-diagonal elements are considered
     *        different
     * @param relativeSymmetryThreshold
     *        the relative symmetry threshold, above which off-diagonal elements are considered
     *        different
     * @param absolutePositivityThreshold
     *        the absolute positivity threshold, above which a value is considered to be strictly
     *        positive
     * @param relativePositivityThreshold
     *        the relative positivity threshold, above which a value is considered to be numerically
     *        significant when compared to another value
     * @throws NonPositiveDefiniteMatrixException
     *         if the matrix is not positive semi-definite
     */
    public ArrayRowSymmetricPositiveMatrix(final SymmetryType symmetryType,
            final RealMatrix matrix, final Double absoluteSymmetryThreshold,
            final Double relativeSymmetryThreshold, final Double absolutePositivityThreshold,
            final Double relativePositivityThreshold) {
        super(symmetryType, matrix, absoluteSymmetryThreshold, relativeSymmetryThreshold);

        // Check the provided positivity thresholds
        checkAbsoluteThreshold(absolutePositivityThreshold);
        checkRelativeThreshold(relativePositivityThreshold);

        // Ensure the provided matrix is positive semi-definite
        if (absolutePositivityThreshold != null || relativePositivityThreshold != null) {
            final double tolerance = getEffectiveTolerance(absolutePositivityThreshold,
                    relativePositivityThreshold, matrix.getNorm());
            checkPositiveSemiDefinite(this, tolerance);
        }
    }

    /**
     * Builds a new {@link ArrayRowSymmetricPositiveMatrix} by specifying the lower triangular part
     * of the matrix directly.
     * <p>
     * No check is made on the positivity or definiteness of the matrix. This constructor should
     * only be used when the matrix defined by the provided data is positive semi-definite by
     * construction.
     * </p>
     *
     * @param dataIn
     *        the array storing the lower triangular part of the matrix
     * @param copyArray
     *        if {@code true}, the provided array will be copied, otherwise it will be passed by
     *        reference
     */
    protected ArrayRowSymmetricPositiveMatrix(final double[] dataIn, final boolean copyArray) {
        super(dataIn, copyArray);
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>Important:</b><br>
     * This method systematically throws a {@link MathUnsupportedOperationException} since this
     * operation is not safe when dealing with symmetric positive definite matrices (the properties
     * of the matrix are not guaranteed to be preserved).
     * </p>
     *
     * @throws MathUnsupportedOperationException
     *         systematically (this operation is forbidden)
     */
    @Override
    public void setEntry(final int row, final int column, final double value) {
        throw new MathUnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * <b>Important:</b><br>
     * This method systematically throws a {@link MathUnsupportedOperationException} since this
     * operation is not safe when dealing with symmetric positive definite matrices (the properties
     * of the matrix are not guaranteed to be preserved).
     * </p>
     * 
     * @throws MathUnsupportedOperationException
     *         systematically, since this operation is not supported
     */
    @Override
    public void addToEntry(final int row, final int column, final double increment) {
        throw new MathUnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * <b>Important:</b><br>
     * This method systematically throws a {@link MathUnsupportedOperationException} since this
     * operation is not safe when dealing with symmetric positive definite matrices (the properties
     * of the matrix are not guaranteed to be preserved).
     * </p>
     *
     * @throws MathUnsupportedOperationException
     *         systematically, since this operation is not supported
     */
    @Override
    public void multiplyEntry(final int row, final int column, final double factor) {
        throw new MathUnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * The returned matrix is an {@linkplain ArrayRowSymmetricPositiveMatrix} if the selected
     * indices are the same for the rows and the columns, and an {@linkplain Array2DRowRealMatrix}
     * or a {@linkplain BlockRealMatrix} otherwise.
     * </p>
     */
    @Override
    public RealMatrix getSubMatrix(final int startRow, final int endRow, final int startColumn,
            final int endColumn) {
        final RealMatrix subMatrix;

        if (startRow == startColumn && endRow == endColumn) {
            // The extracted submatrix is a symmetric positive semi-definite matrix
            subMatrix = getSubMatrix(startRow, endRow);
        } else {
            // Check the indices
            MatrixUtils.checkSubMatrixIndex(this, startRow, endRow, startColumn, endColumn);

            // Define the submatrix dimension
            final int rowDimension = endRow - startRow + 1;
            final int columnDimension = endColumn - startColumn + 1;

            // The extracted submatrix is a regular matrix
            subMatrix = MatrixUtils.createRealMatrix(rowDimension, columnDimension);
            for (int i = startRow; i <= endRow; ++i) {
                for (int j = startColumn; j <= endColumn; ++j) {
                    subMatrix.setEntry(i - startRow, j - startColumn, this.getEntry(i, j));
                }
            }
        }

        return subMatrix;
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * The returned matrix is an {@linkplain ArrayRowSymmetricPositiveMatrix} if the selected
     * indices are the same for the rows and the columns (same indices, same order), and an
     * {@linkplain Array2DRowRealMatrix} or a {@linkplain BlockRealMatrix} otherwise.
     * </p>
     */
    @Override
    public RealMatrix getSubMatrix(final int[] selectedRows, final int[] selectedColumns) {
        final RealMatrix subMatrix;

        if (Arrays.equals(selectedRows, selectedColumns)) {
            // The extracted submatrix is a symmetric positive semi-definite matrix
            subMatrix = this.getSubMatrix(selectedRows);
        } else {
            // Check the indices
            MatrixUtils.checkSubMatrixIndex(this, selectedRows, selectedColumns);

            // The extracted submatrix is a regular matrix
            subMatrix = MatrixUtils.createRealMatrix(selectedRows.length, selectedColumns.length);
            subMatrix.walkInOptimizedOrder(new DefaultRealMatrixChangingVisitor() {
                /** {@inheritDoc} */
                @Override
                public double visit(final int row, final int column, final double value) {
                    return ArrayRowSymmetricPositiveMatrix.this.getEntry(selectedRows[row],
                            selectedColumns[column]);
                }
            });
        }

        return subMatrix;
    }

    /** {@inheritDoc} */
    @Override
    public ArrayRowSymmetricPositiveMatrix getSubMatrix(final int startIndex, final int endIndex) {
        final ArrayRowSymmetricMatrix subMatrix = super.getSubMatrix(startIndex, endIndex);
        return new ArrayRowSymmetricPositiveMatrix(subMatrix.getDataRef(), false);
    }

    /** {@inheritDoc} */
    @Override
    public ArrayRowSymmetricPositiveMatrix getSubMatrix(final int[] index) {
        final ArrayRowSymmetricMatrix subMatrix = super.getSubMatrix(index);
        return new ArrayRowSymmetricPositiveMatrix(subMatrix.getDataRef(), false);
    }

    /**
     * Replaces part of the matrix with a given submatrix, starting at the specified row and column.
     * <p>
     * <b>Important:</b><br>
     * This method systematically throws a {@link MathUnsupportedOperationException} since this
     * operation is not safe when dealing with symmetric positive definite matrices (the properties
     * of the matrix are not guaranteed to be preserved).
     * </p>
     *
     * @param subMatrix
     *        the array containing the replacement data of the targeted submatrix
     * @param row
     *        the row coordinate of the top, left element to be replaced
     * @param column
     *        the column coordinate of the top, left element to be replaced
     * @throws MathUnsupportedOperationException
     *         systematically, since this operation is not supported
     */
    @Override
    public void setSubMatrix(final double[][] subMatrix, final int row, final int column) {
        throw new MathUnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * <b>Important:</b><br>
     * This method systematically throws a {@link MathUnsupportedOperationException} since this
     * operation is not safe when dealing with symmetric positive definite matrices (the properties
     * of the matrix are not guaranteed to be preserved).
     * </p>
     *
     * @throws MathUnsupportedOperationException
     *         systematically, since this operation is not supported
     */
    @Override
    public void setRow(final int row, final double[] array) {
        throw new MathUnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * <b>Important:</b><br>
     * This method systematically throws a {@link MathUnsupportedOperationException} since this
     * operation is not safe when dealing with symmetric positive definite matrices (the properties
     * of the matrix are not guaranteed to be preserved).
     * </p>
     *
     * @throws MathUnsupportedOperationException
     *         systematically, since this operation is not supported
     */
    @Override
    public void setRowVector(final int row, final RealVector vector) {
        throw new MathUnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * <b>Important:</b><br>
     * This method systematically throws a {@link MathUnsupportedOperationException} since this
     * operation is not safe when dealing with symmetric positive definite matrices (the properties
     * of the matrix are not guaranteed to be preserved).
     * </p>
     *
     * @throws MathUnsupportedOperationException
     *         systematically, since this operation is not supported
     */
    @Override
    public void setRowMatrix(final int row, final RealMatrix matrix) {
        throw new MathUnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * <b>Important:</b><br>
     * This method systematically throws a {@link MathUnsupportedOperationException} since this
     * operation is not safe when dealing with symmetric positive definite matrices (the properties
     * of the matrix are not guaranteed to be preserved).
     * </p>
     *
     * @throws MathUnsupportedOperationException
     *         systematically, since this operation is not supported
     */
    @Override
    public void setColumn(final int column, final double[] array) {
        throw new MathUnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * <b>Important:</b><br>
     * This method systematically throws a {@link MathUnsupportedOperationException} since this
     * operation is not safe when dealing with symmetric positive definite matrices (the properties
     * of the matrix are not guaranteed to be preserved).
     * </p>
     *
     * @throws MathUnsupportedOperationException
     *         systematically, since this operation is not supported
     */
    @Override
    public void setColumnVector(final int column, final RealVector vector) {
        throw new MathUnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * <b>Important:</b><br>
     * This method systematically throws a {@link MathUnsupportedOperationException} since this
     * operation is not safe when dealing with symmetric positive definite matrices (the properties
     * of the matrix are not guaranteed to be preserved).
     * </p>
     *
     * @throws MathUnsupportedOperationException
     *         systematically, since this operation is not supported
     */
    @Override
    public void setColumnMatrix(final int column, final RealMatrix matrix) {
        throw new MathUnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    public ArrayRowSymmetricMatrix scalarAdd(final double d) {
        final ArrayRowSymmetricMatrix out;
        if (d >= 0) {
            out = this.positiveScalarAdd(d);
        } else {
            out = super.scalarAdd(d);
        }
        return out;
    }

    /** {@inheritDoc} */
    @Override
    public ArrayRowSymmetricPositiveMatrix positiveScalarAdd(final double d) {
        // Ensure the provided scalar is positive
        if (d < 0) {
            throw new NotPositiveException(PatriusMessages.NOT_POSITIVE_SCALAR, d);
        }

        // Scalar addition routine
        final ArrayRowSymmetricMatrix out = super.scalarAdd(d);
        return new ArrayRowSymmetricPositiveMatrix(out.getDataRef(), false);
    }

    /** {@inheritDoc} */
    @Override
    public ArrayRowSymmetricMatrix scalarMultiply(final double d) {
        final ArrayRowSymmetricMatrix out;
        if (d >= 0) {
            out = this.positiveScalarMultiply(d);
        } else {
            out = super.scalarMultiply(d);
        }
        return out;
    }

    /** {@inheritDoc} */
    @Override
    public ArrayRowSymmetricPositiveMatrix positiveScalarMultiply(final double d) {
        // Ensure the provided scalar is positive
        if (d < 0) {
            throw new NotPositiveException(PatriusMessages.NOT_POSITIVE_SCALAR, d);
        }

        // Scalar multiplication routine
        final ArrayRowSymmetricMatrix out = super.scalarMultiply(d);
        return new ArrayRowSymmetricPositiveMatrix(out.getDataRef(), false);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * The returned matrix is, in order of priority:
     * </p>
     * <ul>
     * <li>An {@linkplain ArrayRowSymmetricPositiveMatrix} if it is a
     * {@linkplain SymmetricPositiveMatrix};</li>
     * <li>An {@linkplain ArrayRowSymmetricMatrix} if it is any other type of
     * {@linkplain SymmetricMatrix}.</li>
     * </ul>
     */
    @Override
    public ArrayRowSymmetricMatrix add(final SymmetricMatrix m) {
        final ArrayRowSymmetricMatrix out;
        if (m instanceof SymmetricPositiveMatrix) {
            out = this.add((SymmetricPositiveMatrix) m);
        } else {
            out = super.add(m);
        }
        return out;
    }

    /** {@inheritDoc} */
    @Override
    public ArrayRowSymmetricPositiveMatrix add(final SymmetricPositiveMatrix m) {
        final ArrayRowSymmetricMatrix out = super.add(m);
        return new ArrayRowSymmetricPositiveMatrix(out.getDataRef(), false);
    }

    /** {@inheritDoc } */
    @Override
    public ArrayRowSymmetricPositiveMatrix quadraticMultiplication(final RealMatrix m) {
        return this.quadraticMultiplication(m, false);
    }

    /** {@inheritDoc } */
    @Override
    public ArrayRowSymmetricPositiveMatrix quadraticMultiplication(final RealMatrix m,
            final boolean isTranspose) {
        final ArrayRowSymmetricMatrix out = super.quadraticMultiplication(m, isTranspose);
        return new ArrayRowSymmetricPositiveMatrix(out.getDataRef(), false);
    }

    /** {@inheritDoc} */
    @Override
    public ArrayRowSymmetricPositiveMatrix power(final int p) {
        final ArrayRowSymmetricMatrix matrix = new ArrayRowSymmetricMatrix(this.getDataRef(), false);
        final ArrayRowSymmetricMatrix out = matrix.power(p);
        return new ArrayRowSymmetricPositiveMatrix(out.getDataRef(), false);
    }

    /** {@inheritDoc} */
    @Override
    public ArrayRowSymmetricPositiveMatrix createMatrix(final int rowDimension,
            final int columnDimension) {
        MatrixUtils.checkDimension(rowDimension, columnDimension);
        return new ArrayRowSymmetricPositiveMatrix(rowDimension);
    }

    /**
     * Creates an identity matrix of the specified dimension.
     *
     * @param dim
     *        the dimension of the identity matrix
     * @return the identity matrix built
     */
    public static ArrayRowSymmetricPositiveMatrix createIdentityMatrix(final int dim) {
        // Check the dimension
        MatrixUtils.checkRowDimension(dim);

        // Number of elements in the lower triangular part of the matrix
        final int nbElements = (dim * (dim + 1)) / 2;
        final double[] data = new double[nbElements];

        // Set the diagonal elements to 1.0
        int index = 0;
        data[0] = 1.0;
        for (int i = 1; i < dim; i++) {
            index += i + 1;
            data[index] = 1.0;
        }

        // Return the identity matrix
        return new ArrayRowSymmetricPositiveMatrix(data, false);
    }

    /** {@inheritDoc} */
    @Override
    public ArrayRowSymmetricPositiveMatrix copy() {
        return new ArrayRowSymmetricPositiveMatrix(this.getDataRef(), true);
    }

    /** {@inheritDoc}. */
    @Override
    public ArrayRowSymmetricPositiveMatrix transpose() {
        return this.transpose(true);
    }

    /** {@inheritDoc}. */
    @Override
    public ArrayRowSymmetricPositiveMatrix transpose(final boolean forceCopy) {
        final ArrayRowSymmetricPositiveMatrix out;
        if (forceCopy == true) {
            out = this.copy();
        } else {
            out = this;
        }
        return out;
    }

    /** {@inheritDoc} */
    @Override
    public ArrayRowSymmetricPositiveMatrix getInverse() {
        final ArrayRowSymmetricMatrix out = super.getInverse();
        return new ArrayRowSymmetricPositiveMatrix(out.getDataRef(), false);
    }

    /** {@inheritDoc} */
    @Override
    public ArrayRowSymmetricPositiveMatrix getInverse(
            final Function<RealMatrix, Decomposition> decompositionBuilder) {
        final ArrayRowSymmetricMatrix out = super.getInverse(decompositionBuilder);
        return new ArrayRowSymmetricPositiveMatrix(out.getDataRef(), false);
    }

    /**
     * Determines if this matrix is positive semi-definite or not.
     * <p>
     * A symmetric matrix is considered to be positive semi-definite if its pivots are either
     * strictly positive, or if the whole row is equal to zero after reduction (pivot included). In
     * order to take into account possible numerical errors, the specified tolerance is added to the
     * diagonal elements of the initial matrix.
     * </p>
     *
     * @param absoluteTolerance
     *        the absolute tolerance to take into account
     * @return {@code true} if this matrix is positive semi-definite, {@code false} otherwise
     */
    public boolean isPositiveSemiDefinite(final double absoluteTolerance) {
        return isPositiveSemiDefinite(this, absoluteTolerance);
    }

    /**
     * Determines if a symmetric matrix is positive semi-definite or not.
     * <p>
     * A symmetric matrix is considered to be positive semi-definite if its pivots are either
     * strictly positive, or if the whole row is equal to zero after reduction (pivot included). In
     * order to take into account possible numerical errors, the specified tolerance is added to the
     * diagonal elements of the initial matrix.
     * </p>
     *
     * @param symmetricMatrix
     *        the symmetric matrix to be checked
     * @param absoluteTolerance
     *        the absolute tolerance to take into account
     * @return {@code true} if the provided matrix is positive semi-definite, {@code false}
     *         otherwise
     */
    public static boolean isPositiveSemiDefinite(final SymmetricMatrix symmetricMatrix,
            final double absoluteTolerance) {
        boolean isPositive = true;
        try {
            checkPositiveSemiDefinite(symmetricMatrix, absoluteTolerance);
        } catch (final NonPositiveDefiniteMatrixException e) {
            isPositive = false;
        }
        return isPositive;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object object) {
        boolean out = false;

        if (object == this) {
            out = true;
        } else if (object != null) {
            out = super.equals(object);
        }

        return out;
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.UselessOverridingMethod")
    // Reason: false positive, PMD and checkstyle force to have a hashCode method
    public int hashCode() {
        return super.hashCode();
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * <b>Important:</b><br>
     * This method systematically throws a {@link MathUnsupportedOperationException} since this
     * operation is not safe when dealing with symmetric positive definite matrices (the properties
     * of the matrix are not guaranteed to be preserved).
     * </p>
     *
     * @throws MathUnsupportedOperationException
     *         systematically (this operation is forbidden)
     */
    @Override
    public double walkInRowOrder(final RealMatrixChangingVisitor visitor) {
        throw new MathUnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * <b>Important:</b><br>
     * This method systematically throws a {@link MathUnsupportedOperationException} since this
     * operation is not safe when dealing with symmetric positive definite matrices (the properties
     * of the matrix are not guaranteed to be preserved).
     * </p>
     *
     * @throws MathUnsupportedOperationException
     *         systematically (this operation is forbidden)
     */
    @Override
    public double walkInRowOrder(final RealMatrixChangingVisitor visitor, final int startRow,
            final int endRow, final int startColumn, final int endColumn) {
        throw new MathUnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * <b>Important:</b><br>
     * This method systematically throws a {@link MathUnsupportedOperationException} since this
     * operation is not safe when dealing with symmetric positive definite matrices (the properties
     * of the matrix are not guaranteed to be preserved).
     * </p>
     *
     * @throws MathUnsupportedOperationException
     *         systematically (this operation is forbidden)
     */
    @Override
    public double walkInColumnOrder(final RealMatrixChangingVisitor visitor) {
        throw new MathUnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * <b>Important:</b><br>
     * This method systematically throws a {@link MathUnsupportedOperationException} since this
     * operation is not safe when dealing with symmetric positive definite matrices (the properties
     * of the matrix are not guaranteed to be preserved).
     * </p>
     *
     * @throws MathUnsupportedOperationException
     *         systematically (this operation is forbidden)
     */
    @Override
    public double walkInColumnOrder(final RealMatrixChangingVisitor visitor, final int startRow,
            final int endRow, final int startColumn, final int endColumn) {
        throw new MathUnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * <b>Important:</b><br>
     * This method systematically throws a {@link MathUnsupportedOperationException} since this
     * operation is not safe when dealing with symmetric positive definite matrices (the properties
     * of the matrix are not guaranteed to be preserved).
     * </p>
     *
     * @throws MathUnsupportedOperationException
     *         systematically (this operation is forbidden)
     */
    @Override
    public double walkInOptimizedOrder(final RealMatrixChangingVisitor visitor) {
        throw new MathUnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * <b>Important:</b><br>
     * This method systematically throws a {@link MathUnsupportedOperationException} since this
     * operation is not safe when dealing with symmetric positive definite matrices (the properties
     * of the matrix are not guaranteed to be preserved).
     * </p>
     *
     * @throws MathUnsupportedOperationException
     *         systematically (this operation is forbidden)
     */
    @Override
    public double walkInOptimizedOrder(final RealMatrixChangingVisitor visitor, final int startRow,
            final int endRow, final int startColumn, final int endColumn) {
        throw new MathUnsupportedOperationException();
    }

    /**
     * Gets the default absolute positivity threshold, above which a value is considered to be
     * strictly positive.
     *
     * @return the default absolute positivity threshold (&ge;0 or {@code null})
     */
    public static Double getDefaultAbsolutePositivityThreshold() {
        return defaultAbsolutePositivityThreshold;
    }

    /**
     * Sets the default absolute positivity threshold, above which a value is considered to be
     * positive.
     *
     * @param threshold
     *        the new default absolute positivity threshold (&ge;0 or {@code null})
     * @throws IllegalArgumentException
     *         if the provided threshold is {@code NaN} or is strictly negative
     */
    public static void setDefaultAbsolutePositivityThreshold(final Double threshold) {
        checkAbsoluteThreshold(threshold);
        defaultAbsolutePositivityThreshold = threshold;
    }

    /**
     * Gets the default relative positivity threshold, above which a value is considered to be
     * numerically significant when compared to another value.
     *
     * @return the default relative positivity threshold (&ge;0 or {@code null})
     */
    public static Double getDefaultRelativePositivityThreshold() {
        return defaultRelativePositivityThreshold;
    }

    /**
     * Sets the default relative positivity threshold, above which a value is considered to be
     * numerically significant when compared to another value.
     *
     * @param threshold
     *        the new default relative positivity threshold (&ge;0 or {@code null})
     * @throws IllegalArgumentException
     *         if the provided threshold is {@code NaN} or is strictly negative
     */
    public static void setDefaultRelativePositivityThreshold(final Double threshold) {
        checkRelativeThreshold(threshold);
        defaultRelativePositivityThreshold = threshold;
    }

    /**
     * Ensures a symmetric matrix is positive semi-definite and throws an exception if that's not
     * the case.
     * <p>
     * A symmetric matrix is considered to be positive semi-definite if its pivots are either
     * strictly positive, or if the whole row is equal to zero after reduction (pivot included). In
     * order to take into account possible numerical errors, the specified tolerance is added to the
     * diagonal elements of the initial matrix.
     * </p>
     *
     * @param symmetricMatrix
     *        the symmetric matrix to be checked
     * @param tolerance
     *        the absolute tolerance to take into account
     * @throws NonPositiveDefiniteMatrixException
     *         if the provided matrix is not positive semi-definite
     */
    protected static void checkPositiveSemiDefinite(final SymmetricMatrix symmetricMatrix,
            final double tolerance) {
        // Store the symmetric matrix in a standard array
        final double[][] matrix = symmetricMatrix.getData();

        // Dimension of the matrix
        final int n = matrix.length;

        // Add a small value to the diagonal elements to take
        // into account possible numerical errors
        if (tolerance > 0.) {
            for (int i = 0; i < n; i++) {
                matrix[i][i] += tolerance;
            }
        }

        // Check if the provided matrix is positive semi-definite
        for (int k = 0; k < n; k++) {
            final double[] matrixK = matrix[k];

            // Pivot
            final double pivot = matrixK[k];

            if (pivot < 0) {
                // If the pivot is negative, the matrix
                // is not positive semi-definite
                throw new NonPositiveDefiniteMatrixException(pivot, k, 0.);
            } else if (k < n - 1) {
                if (MathLib.abs(pivot) <= Precision.SAFE_MIN) {
                    // If the pivot is zero, the matrix is positive semi-definite
                    // if and only if the entire row is zero and if the remaining
                    // submatrix is positive semi-definite.
                    for (int j = k; j < n; j++) {
                        final double aKJ = matrixK[j];
                        if (MathLib.abs(aKJ) > tolerance) {
                            throw new NonPositiveDefiniteMatrixException(aKJ, k, 0.);
                        }
                    }
                } else {
                    // If the pivot is not zero, reduce the remaining
                    // submatrix and check if it is positive semi-definite
                    for (int i = k + 1; i < n; i++) {
                        final double aIK = matrix[i][k];
                        final double factor = MathLib.divide(aIK, pivot);
                        for (int j = k + 1; j < n; j++) {
                            final double aKJ = matrixK[j];
                            matrix[i][j] -= factor * aKJ;
                        }
                    }
                }
            }
        }
    }

    /**
     * Gets the effective tolerance from a given absolute and relative tolerance.
     * <p>
     * The effective tolerance is the largest value between the specified absolute tolerance and the
     * relative tolerance multiplied by the provided element value. The provided tolerances default
     * to zero when they are {@code null}.
     * </p>
     *
     * @param absoluteTolerance
     *        the absolute tolerance
     * @param relativeTolerance
     *        the relative tolerance
     * @param maxValue
     *        the value to which the relative tolerance is to be applied
     * @return the effective tolerance
     */
    protected static double getEffectiveTolerance(final Double absoluteTolerance,
            final Double relativeTolerance, final double maxValue) {
        double positivityThreshold = 0;

        // Take into account the absolute tolerance, if it is not null
        if (absoluteTolerance != null) {
            positivityThreshold = MathLib.max(positivityThreshold, absoluteTolerance);
        }

        // Take into account the relative tolerance, if it is not null
        if (relativeTolerance != null) {
            positivityThreshold = MathLib.max(positivityThreshold,
                    relativeTolerance * MathLib.abs(maxValue));
        }

        return positivityThreshold;
    }
}
