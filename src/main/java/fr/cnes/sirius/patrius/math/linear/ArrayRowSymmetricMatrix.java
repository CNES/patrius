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
 */
/* 
 * HISTORY
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
* VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
* VERSION:4.8:FA:FA-2941:15/11/2021:[PATRIUS] Correction anomalies suite a DM 2767 
* VERSION:4.8:DM:DM-3040:15/11/2021:[PATRIUS]Reversement des evolutions de la branche patrius-for-lotus 
 * VERSION:4.7:DM:DM-2766:18/05/2021:Evol. et corr. dans le package fr.cnes.sirius.patrius.math.linear (suite DM 2300) 
 * VERSION:4.7:DM:DM-2590:18/05/2021:Configuration des TransformProvider 
 * VERSION:4.6:FA:FA-2542:27/01/2021:[PATRIUS] Definition d'un champ de vue avec demi-angle de 180° 
 * VERSION:4.5.1:FA:FA-2540:04/08/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear
 * VERSION:4.5:DM:DM-2300:27/05/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.linear;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

import fr.cnes.sirius.patrius.math.exception.MathUnsupportedOperationException;
import fr.cnes.sirius.patrius.math.exception.NoDataException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Symmetric matrix defined by its lower triangular part.
 * <p>
 * This implementation stores the elements of the lower triangular part of a symmetric matrix. These
 * elements are stored in a 1D array, row after row. For example, for a 3 by 3 matrix we have:<br>
 * (1) = s<sub>1,1</sub>;<br>
 * (2) = s<sub>2,1</sub>; (3) = s<sub>2,2</sub>;<br>
 * (4) = s<sub>3,1</sub>; (5) = s<sub>3,2</sub>; (6) = s<sub>3,3</sub>;<br>
 * </p>
 * <p>
 * The elements actually stored depends on the type of symmetry specified at construction:
 * {@linkplain SymmetryType#LOWER LOWER} or {@linkplain SymmetryType#UPPER UPPER} implies the
 * symmetry is enforced by keeping only the lower or upper triangular part of the matrix, while
 * {@linkplain SymmetryType#MEAN MEAN} enforces the symmetry by computing the mean of the lower and
 * upper elements. The symmetry of the provided matrix is not checked by default, meaning that the
 * symmetry is enforced regardless of the inputs. However, such a check can be triggered by setting
 * the default absolute or relative symmetry threshold to a non-null value, or by specifying any of
 * these thresholds at construction.
 * </p>
 * <p>
 * <b>Important:</b><br>
 * Because only half the elements of the matrix are actually stored, setting an off-diagonal element
 * to a new value will automatically modify the symmetric element. As a result, the symmetry of the
 * matrix will automatically be preserved. However, note that the method
 * {@linkplain #setSubMatrix(double[][], int, int) setSubMatrix} is not supported and will
 * systematically throw a {@linkplain MathUnsupportedOperationException}.
 * </p>
 *
 * @author Pierre Seimandi (GMV)
 *
 * @since 4.5 (was previously called SymmetricMatrix, which now describes the interface)
 */
public class ArrayRowSymmetricMatrix extends AbstractRealMatrix implements SymmetricMatrix {

    /** Serial version UID. */
    private static final long serialVersionUID = -4950022702306207105L;

    /**
     * Default absolute threshold above which off-diagonal elements are considered different<br>
     * (if null, no check is made on the absolute difference between symmetric elements).
     */
    private static Double defaultAbsoluteSymmetryThreshold = null;

    /**
     * Default relative threshold above which off-diagonal elements are considered different<br>
     * (if null, no check is made on the relative difference between symmetric elements).
     */
    private static Double defaultRelativeSymmetryThreshold = null;

    /** Value used in vector to matrix dimension conversion. */
    private static final int BLOCK_SIZE = 8;

    /** Dimension of the symmetric matrix. */
    private final int dimension;

    /** Array storing the lower part of the symmetric matrix. */
    private final double[] data;

    /** Enumerate to fill in a symmetric matrix only by its lower/upper part. */
    public enum SymmetryType {
        /** The lower part. */
        LOWER,
        /** The upper part. */
        UPPER,
        /** Average value on the non-diagonal symmetric terms. */
        MEAN
    }

    /**
     * Builds a new {@linkplain ArrayRowSymmetricMatrix} of dimension n (filled with zero).
     *
     * @param n
     *        the dimension of the matrix
     */
    public ArrayRowSymmetricMatrix(final int n) {
        super(n, n);
        this.dimension = n;
        this.data = new double[this.dimension * (this.dimension + 1) / 2];
    }

    /**
     * Builds a new {@linkplain ArrayRowSymmetricMatrix} from the provided data, using the default
     * symmetry thresholds.
     *
     * @param symmetryType
     *        the type of symmetry enforced at construction
     * @param dataIn
     *        the data of the matrix (must be a NxN array)
     * @throws NonSquareMatrixException
     *         if the provided matrix is not square (unchecked)
     * @throws NonSymmetricMatrixException
     *         if the provided matrix is not symmetric (unchecked)
     * @see #getDefaultRelativeSymmetryThreshold()
     */
    public ArrayRowSymmetricMatrix(final SymmetryType symmetryType, final double[][] dataIn) {
        // Do not copy the provided array since the built matrix is only a temporary matrix.
        this(symmetryType, MatrixUtils.createRealMatrix(dataIn, false),
                getDefaultAbsoluteSymmetryThreshold(), getDefaultRelativeSymmetryThreshold());
    }

    /**
     * Builds a new {@linkplain ArrayRowSymmetricMatrix} from the provided data, using the specified
     * symmetry thresholds.
     *
     * @param symmetryType
     *        the type of symmetry enforced at construction
     * @param dataIn
     *        the data of the matrix (must be a NxN array)
     * @param absoluteSymmetryThreshold
     *        the absolute threshold above which off-diagonal elements are considered different<br>
     *        (if null, no check is made on the absolute difference between symmetric elements)
     * @param relativeSymmetryThreshold
     *        the relative threshold above which off-diagonal elements are considered different<br>
     *        (if null, no check is made on the relative difference between symmetric elements)
     * @throws IllegalArgumentException
     *         if one of the provided thresholds is {@code NaN} or is strictly negative (unchecked)
     * @throws NonSquareMatrixException
     *         if the provided matrix is not square (unchecked)
     * @throws NonSymmetricMatrixException
     *         if the provided matrix is not symmetric (unchecked)
     */
    public ArrayRowSymmetricMatrix(final SymmetryType symmetryType, final double[][] dataIn,
            final Double absoluteSymmetryThreshold, final Double relativeSymmetryThreshold) {
        // Do not copy the provided array since the built matrix is only a temporary matrix.
        this(symmetryType, MatrixUtils.createRealMatrix(dataIn, false), absoluteSymmetryThreshold,
                relativeSymmetryThreshold);
    }

    /**
     * Builds a new {@linkplain ArrayRowSymmetricMatrix} from the provided matrix, using the default
     * symmetry thresholds.
     *
     * @param symmetryType
     *        the type of symmetry enforced at construction
     * @param matrix
     *        the matrix (must NxN)
     * @throws NonSquareMatrixException
     *         if the provided matrix is not square (unchecked)
     * @throws NonSymmetricMatrixException
     *         if the provided matrix is not symmetric (unchecked)
     * @see #getDefaultRelativeSymmetryThreshold()
     */
    public ArrayRowSymmetricMatrix(final SymmetryType symmetryType, final RealMatrix matrix) {
        this(symmetryType, matrix, getDefaultAbsoluteSymmetryThreshold(),
                getDefaultRelativeSymmetryThreshold());
    }

    /**
     * Builds a new {@linkplain ArrayRowSymmetricMatrix} from the provided matrix, using the
     * specified symmetry thresholds.
     *
     * @param symmetryType
     *        the type of symmetry enforced at construction
     * @param matrix
     *        the matrix (must NxN)
     * @param absoluteSymmetryThreshold
     *        the absolute threshold above which off-diagonal elements are considered different<br>
     *        (if null, no check is made on the absolute difference between symmetric elements)
     * @param relativeSymmetryThreshold
     *        the relative threshold above which off-diagonal elements are considered different<br>
     *        (if null, no check is made on the absolute difference between symmetric elements)
     * @throws IllegalArgumentException
     *         if one of the provided thresholds is {@code NaN} or is strictly negative (unchecked)
     * @throws NonSquareMatrixException
     *         if the provided matrix is not square (unchecked)
     * @throws NonSymmetricMatrixException
     *         if the provided matrix is not symmetric (unchecked)
     */
    public ArrayRowSymmetricMatrix(final SymmetryType symmetryType, final RealMatrix matrix,
            final Double absoluteSymmetryThreshold, final Double relativeSymmetryThreshold) {
        super();

        // Check the matrix
        checkMatrix(matrix);

        // Check the absolute and relative symmetry thresholds
        checkAbsoluteThreshold(absoluteSymmetryThreshold);
        checkRelativeThreshold(relativeSymmetryThreshold);

        // If an absolute or a relative symmetry threshold was specified,
        // check the symmetry of the matrix. If one of the thresholds is
        // null, set it to zero so that it is not taken into account.
        if (absoluteSymmetryThreshold != null || relativeSymmetryThreshold != null) {
            double absTol = 0.;
            if (absoluteSymmetryThreshold != null) {
                absTol = absoluteSymmetryThreshold;
            }

            double relTol = 0.;
            if (relativeSymmetryThreshold != null) {
                relTol = relativeSymmetryThreshold;
            }

            MatrixUtils.checkSymmetry(matrix, absTol, relTol);
        }

        this.dimension = matrix.getRowDimension();
        this.data = new double[(this.dimension * (this.dimension + 1)) / 2];

        int index = 0;
        for (int i = 0; i < this.dimension; i++) {
            for (int j = 0; j <= i; j++) {
                switch (symmetryType) {
                    case LOWER:
                        this.data[index++] = matrix.getEntry(i, j);
                        break;
                    case UPPER:
                        this.data[index++] = matrix.getEntry(j, i);
                        break;
                    case MEAN:
                        this.data[index++] = (matrix.getEntry(i, j) + matrix.getEntry(j, i)) / 2.;
                        break;
                    default:
                        // Should never happen
                        throw new EnumConstantNotPresentException(SymmetryType.class,
                                symmetryType.name());
                }
            }
        }
    }

    /**
     * Builds a new {@linkplain ArrayRowSymmetricMatrix} by specifying the lower triangular part of
     * the matrix directly.
     *
     * @param dataIn
     *        the array storing the lower triangular part of the matrix
     * @param copyArray
     *        if true, copy the provided array instead
     */
    protected ArrayRowSymmetricMatrix(final double[] dataIn, final boolean copyArray) {
        super();

        // Check the data array
        checkDataArray(dataIn);

        if (copyArray) {
            this.data = dataIn.clone();
        } else {
            this.data = dataIn;
        }

        // Retrieve the dimension of the matrix from the size of the array.
        // The computed dimension is not check since this method is only used internally,
        // meaning the length of the provided array should always be compatible with this formula.
        this.dimension = (int) MathLib
                .round((MathLib.sqrt(1 + BLOCK_SIZE * this.data.length) - 1) / 2);
    }

    /**
     * Ensures the provided matrix is not {@code null} and is a square matrix.
     *
     * @param matrix
     *        the matrix to be checked
     * @throws NullArgumentException
     *         if the provided matrix is {@code null}
     * @throws NonSquareMatrixException
     *         if the provided matrix is not square
     */
    protected static void checkMatrix(final RealMatrix matrix) {
        // Ensure the matrix is not null
        if (matrix == null) {
            throw new NullArgumentException(PatriusMessages.NULL_MATRIX_NOT_ALLOWED);
        }

        // Ensure the matrix is square
        if (!matrix.isSquare()) {
            throw new NonSquareMatrixException(matrix.getColumnDimension(),
                    matrix.getRowDimension());
        }
    }

    /**
     * Checks the validity of the provided data array.
     * <p>
     * To be valid, the array must not be {@code null} or empty.
     * </p>
     *
     * @param data
     *        the data array to be checked
     * @throws NullArgumentException
     *         if the data array is {@code null}
     * @throws NoDataException
     *         if the data array is empty
     */
    protected static void checkDataArray(final double[] data) {
        // Ensure the array is not null
        if (data == null) {
            throw new NullArgumentException(PatriusMessages.NULL_ARRAY_NOT_ALLOWED);
        }

        // Ensure the array contains at least one element
        if (data.length == 0) {
            throw new NoDataException(PatriusMessages.EMPTY_ARRAY_NOT_ALLOWED);
        }
    }

    /**
     * Ensures the provided absolute threshold is either {@code null} or a positive number (zero
     * included).
     *
     * @param threshold
     *        the absolute threshold to check
     * @throws IllegalArgumentException
     *         if the provided threshold is {@code NaN} or is strictly negative
     */
    protected static void checkAbsoluteThreshold(final Double threshold) {
        if (threshold != null) {
            if (Double.isNaN(threshold)) {
                final String msg = PatriusMessages.NAN_THRESHOLD.getLocalizedString(Locale
                        .getDefault());
                throw new IllegalArgumentException(msg);
            }
            if (threshold < 0.) {
                final String msg = PatriusMessages.NOT_POSITIVE_ABSOLUTE_THRESHOLD
                        .getLocalizedString(Locale.getDefault());
                throw new IllegalArgumentException(msg);
            }
        }
    }

    /**
     * Ensures the provided relative threshold is either {@code null} or a positive number (zero
     * included).
     *
     * @param threshold
     *        the relative threshold to check
     * @throws IllegalArgumentException
     *         if the provided threshold is {@code NaN} or is strictly negative
     */
    protected static void checkRelativeThreshold(final Double threshold) {
        if (threshold != null) {
            if (Double.isNaN(threshold)) {
                final String msg = PatriusMessages.NAN_THRESHOLD.getLocalizedString(Locale
                        .getDefault());
                throw new IllegalArgumentException(msg);
            }
            if (threshold < 0.) {
                final String msg = PatriusMessages.NOT_POSITIVE_RELATIVE_THRESHOLD
                        .getLocalizedString(Locale.getDefault());
                throw new IllegalArgumentException(msg);
            }
        }
    }

    /**
     * Gets a reference to the internal data array storing the lower part of the symmetric matrix.
     * <p>
     * <em>This method is only provided for optimization purposes.<br>
     * Any modification made on the returned array will change the values of this matrix.</em>
     * </p>
     *
     * @return a direct reference to the internal data array storing the lower part of the symmetric
     *         matrix
     */
    // Reason: internal array access provided for optimization purposes
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    protected double[] getDataRef() {
        return this.data;
    }

    /** {@inheritDoc} */
    @Override
    public int getRowDimension() {
        return this.dimension;
    }

    /** {@inheritDoc} */
    @Override
    public int getColumnDimension() {
        return this.dimension;
    }

    /** {@inheritDoc} */
    @Override
    public double getEntry(final int row, final int column) {
        MatrixUtils.checkMatrixIndex(this, row, column);
        return this.data[getIndex(row, column)];
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * <em>This method automatically modify the symmetric element to ensure the symmetry of the matrix is 
     * preserved.</em>
     * </p>
     */
    @Override
    public void setEntry(final int row, final int column, final double value) {
        MatrixUtils.checkMatrixIndex(this, row, column);
        this.data[getIndex(row, column)] = value;
    }

    /** {@inheritDoc} */
    @Override
    public void addToEntry(final int row, final int column, final double increment) {
        MatrixUtils.checkMatrixIndex(this, row, column);
        final int index1D = getIndex(row, column);
        this.data[index1D] += increment;
    }

    /** {@inheritDoc} */
    @Override
    public void multiplyEntry(final int row, final int column, final double factor) {
        MatrixUtils.checkMatrixIndex(this, row, column);
        final int index1D = getIndex(row, column);
        this.data[index1D] *= factor;
    }

    /**
     * Gets the internal index corresponding to the specified row and column indices.
     *
     * @param row
     *        the row index
     * @param column
     *        the column index
     * @return the internal index corresponding to the specified row and column indices
     */
    private static int getIndex(final int row, final int column) {
        final int minIndex = MathLib.min(row, column);
        final int maxIndex = MathLib.max(row, column);

        return (maxIndex * (maxIndex + 1)) / 2 + minIndex;
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix getRowMatrix(final int row) {
        MatrixUtils.checkRowIndex(this, row);
        final int nbCols = this.getColumnDimension();
        final double[] rowData = new double[nbCols];
        for (int i = 0; i < nbCols; ++i) {
            rowData[i] = this.getEntry(row, i);
        }

        return MatrixUtils.createRowRealMatrix(rowData);
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix getColumnMatrix(final int column) {
        MatrixUtils.checkColumnIndex(this, column);
        final int nbRows = this.getRowDimension();
        final double[] columnData = new double[nbRows];
        for (int i = 0; i < nbRows; ++i) {
            columnData[i] = this.getEntry(column, i);
        }

        return MatrixUtils.createColumnRealMatrix(columnData);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * The returned matrix is an {@linkplain ArrayRowSymmetricMatrix} if the selected indices are
     * the same for the rows and the columns, and an {@linkplain Array2DRowRealMatrix} or a
     * {@linkplain BlockRealMatrix} otherwise.
     * </p>
     */
    @Override
    public RealMatrix getSubMatrix(final int startRow, final int endRow, final int startColumn,
            final int endColumn) {
        final RealMatrix subMatrix;

        if (startRow == startColumn && endRow == endColumn) {
            // The extracted submatrix is a symmetric matrix
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
     * The returned matrix is an {@linkplain ArrayRowSymmetricMatrix} if the selected indices are
     * the same for the rows and the columns (same indices, same order), and an
     * {@linkplain Array2DRowRealMatrix} or a {@linkplain BlockRealMatrix} otherwise.
     * </p>
     */
    @Override
    public RealMatrix getSubMatrix(final int[] selectedRows, final int[] selectedColumns) {
        final RealMatrix subMatrix;

        if (Arrays.equals(selectedRows, selectedColumns)) {
            // The extracted submatrix is a symmetric matrix
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
                    return ArrayRowSymmetricMatrix.this.getEntry(selectedRows[row],
                            selectedColumns[column]);
                }
            });
        }

        return subMatrix;
    }

    /** {@inheritDoc} */
    @Override
    public ArrayRowSymmetricMatrix getSubMatrix(final int startIndex, final int endIndex) {
        // Check the indices
        MatrixUtils.checkSubMatrixIndex(this, startIndex, endIndex, startIndex, endIndex);

        // Define the submatrix dimension
        final int dim = endIndex - startIndex + 1;

        final ArrayRowSymmetricMatrix subMatrix = new ArrayRowSymmetricMatrix(dim);
        // Extract the submatrix
        for (int i = startIndex; i <= endIndex; ++i) {
            for (int j = startIndex; j <= endIndex; ++j) {
                subMatrix.setEntry(i - startIndex, j - startIndex, this.getEntry(i, j));
            }
        }
        return subMatrix;
    }

    /** {@inheritDoc} */
    @Override
    public ArrayRowSymmetricMatrix getSubMatrix(final int[] indices) {
        // Check the indices
        MatrixUtils.checkSubMatrixIndex(this, indices, indices);

        // Define the submatrix dimension
        final int dim = indices.length;

        final ArrayRowSymmetricMatrix subMatrix = new ArrayRowSymmetricMatrix(dim);
        // Extract the submatrix
        for (int i = 0; i < dim; i++) {
            for (int j = 0; j <= i; j++) {
                subMatrix.setEntry(i, j, this.getEntry(indices[i], indices[j]));
            }
        }
        return subMatrix;
    }

    /**
     * Replaces part of the matrix with a given submatrix, starting at the specified row and column.
     * <b>Important:</b><br>
     * This method systematically throws a {@linkplain MathUnsupportedOperationException} since this
     * operation is not safe when dealing with symmetric matrices (the properties of the matrix are
     * not guaranteed to be preserved and this method cannot determine which value should take
     * precedence when symmetric elements are both provided).
     * </p>
     * 
     * @param subMatrix
     *        the array containing the submatrix replacement data
     * @param row
     *        the row coordinate of the top, left element to be replaced
     * @param column
     *        the column coordinate of the top, left element to be replaced
     * @throws MathUnsupportedOperationException
     *         systematically, since this operation is not supported
     * @since 4.5
     */
    @Override
    public void setSubMatrix(final double[][] subMatrix, final int row, final int column) {
        throw new MathUnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    public ArrayRowSymmetricMatrix scalarAdd(final double d) {
        final double[] out = this.data.clone();
        for (int i = 0; i < out.length; i++) {
            out[i] += d;
        }
        return new ArrayRowSymmetricMatrix(out, false);
    }

    /** {@inheritDoc} */
    @Override
    public ArrayRowSymmetricMatrix scalarMultiply(final double d) {
        final double[] out = this.data.clone();
        for (int i = 0; i < out.length; i++) {
            out[i] *= d;
        }
        return new ArrayRowSymmetricMatrix(out, false);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * The returned matrix is, in order of priority:
     * </p>
     * <ul>
     * <li>An {@linkplain ArrayRowSymmetricMatrix} if it is a {@linkplain SymmetricMatrix};</li>
     * <li>An {@linkplain Array2DRowRealMatrix} or a {@linkplain BlockRealMatrix} if it is any other
     * type of {@linkplain RealMatrix}.
     * </ul>
     */
    @Override
    public RealMatrix add(final RealMatrix m) {
        final RealMatrix out;

        if (m instanceof SymmetricMatrix) {
            // Call the optimized code for symmetric matrices
            out = this.add((SymmetricMatrix) m);
        } else {
            // Dimensions check
            MatrixUtils.checkAdditionCompatible(this, m);

            // Addition routine
            final int dim = this.getRowDimension();
            final double[][] outData = new double[dim][dim];
            for (int i = 0; i < dim; i++) {
                for (int j = 0; j < dim; j++) {
                    outData[i][j] = this.getEntry(i, j) + m.getEntry(i, j);
                }
            }
            // Store the array in a matrix
            out = MatrixUtils.createRealMatrix(outData, false);
        }

        return out;
    }

    /** {@inheritDoc} */
    @Override
    public ArrayRowSymmetricMatrix add(final SymmetricMatrix m) {
        final ArrayRowSymmetricMatrix out;

        if (m instanceof DiagonalMatrix) {
            // Call the optimized code for diagonal matrices
            out = this.add((DiagonalMatrix) m);
        } else {
            // Dimensions check
            MatrixUtils.checkAdditionCompatible(this, m);

            // Addition routine
            final int dim = this.getRowDimension();
            out = new ArrayRowSymmetricMatrix(this.data, true);
            for (int i = 0; i < dim; i++) {
                for (int j = 0; j <= i; j++) {
                    out.addToEntry(i, j, +m.getEntry(i, j));
                }
            }
        }

        return out;
    }

    /**
     * Returns the result of adding the diagonal matrix {@code m} to this matrix.
     *
     * @param m
     *        the matrix to be added
     * @return the matrix resulting from the addition {@code this} + {@code m}
     * @throws MatrixDimensionMismatchException
     *         if the matrix {@code m} is not the same size as this matrix
     */
    public ArrayRowSymmetricMatrix add(final DiagonalMatrix m) {
        // Dimensions check
        MatrixUtils.checkAdditionCompatible(this, m);

        // Addition routine
        final int dim = this.getRowDimension();
        final ArrayRowSymmetricMatrix out = new ArrayRowSymmetricMatrix(this.data, true);
        for (int i = 0; i < dim; i++) {
            out.addToEntry(i, i, m.getEntry(i, i));
        }
        return out;
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * The returned matrix is, in order of priority:
     * </p>
     * <ul>
     * <li>An {@linkplain ArrayRowSymmetricMatrix} if it is a {@linkplain SymmetricMatrix};</li>
     * <li>An {@linkplain Array2DRowRealMatrix} or a {@linkplain BlockRealMatrix} if it is any other
     * type of {@linkplain RealMatrix}.
     * </ul>
     */
    @Override
    public RealMatrix subtract(final RealMatrix m) {
        final RealMatrix out;

        if (m instanceof SymmetricMatrix) {
            // Call the optimized code for symmetric matrices
            out = this.subtract((SymmetricMatrix) m);
        } else {
            // Dimensions check
            MatrixUtils.checkAdditionCompatible(this, m);

            // Subtraction routine
            final int dim = this.getRowDimension();
            final double[][] outData = new double[dim][dim];
            for (int i = 0; i < dim; i++) {
                for (int j = 0; j < dim; j++) {
                    outData[i][j] = this.getEntry(i, j) - m.getEntry(i, j);
                }
            }
            // Store the array in a matrix
            out = MatrixUtils.createRealMatrix(outData, false);
        }

        return out;
    }

    /** {@inheritDoc} */
    @Override
    public ArrayRowSymmetricMatrix subtract(final SymmetricMatrix m) {
        final ArrayRowSymmetricMatrix out;

        if (m instanceof DiagonalMatrix) {
            // Call the optimized code for diagonal matrices
            out = this.subtract((DiagonalMatrix) m);
        } else {
            // Dimensions check
            MatrixUtils.checkSubtractionCompatible(this, m);

            // Subtraction routine
            final int dim = this.getRowDimension();
            out = new ArrayRowSymmetricMatrix(this.data, true);
            for (int i = 0; i < dim; i++) {
                for (int j = 0; j <= i; j++) {
                    out.addToEntry(i, j, -m.getEntry(i, j));
                }
            }
        }

        return out;
    }

    /**
     * Returns the result of subtracting the diagonal matrix {@code m} from this matrix.
     *
     * @param m
     *        the matrix to be subtracted
     * @return the matrix resulting from the subtraction {@code this} - {@code m}
     * @throws MatrixDimensionMismatchException
     *         if the matrix {@code m} is not the same size as this matrix
     */
    public ArrayRowSymmetricMatrix subtract(final DiagonalMatrix m) {
        // Dimensions check
        MatrixUtils.checkAdditionCompatible(this, m);

        // Subtraction routine
        final int dim = this.getRowDimension();
        final ArrayRowSymmetricMatrix out = new ArrayRowSymmetricMatrix(this.data, true);
        for (int i = 0; i < dim; i++) {
            out.addToEntry(i, i, -m.getEntry(i, i));
        }
        return out;
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix multiply(final RealMatrix m, final boolean toTranspose, final double d) {
        final RealMatrix out;

        if (m instanceof DiagonalMatrix) {
            // Optimized code for diagonal matrices
            out = this.multiply((DiagonalMatrix) m, d);
        } else if (m instanceof SymmetricMatrix) {
            // Optimized code for symmetric matrices
            final RealMatrix matrix = MatrixUtils.createRealMatrix(getData(), false);
            out = matrix.multiply(m, false, d);
        } else {
            // Multiplication routine
            final RealMatrix matrix = MatrixUtils.createRealMatrix(getData(), false);
            out = matrix.multiply(m, toTranspose, d);
        }

        return out;
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix multiply(final DiagonalMatrix m, final double d) {
        final RealMatrix matrix = MatrixUtils.createRealMatrix(getData(), false);
        return matrix.multiply(m, false, d);
    }

    /** {@inheritDoc} */
    @Override
    public ArrayRowSymmetricMatrix quadraticMultiplication(final RealMatrix m) {
        return this.quadraticMultiplication(m, false);
    }

    /** {@inheritDoc} */
    @Override
    public ArrayRowSymmetricMatrix quadraticMultiplication(final RealMatrix m,
            final boolean isTranspose) {
        // Lower triangular part of the matrix to be returned
        final double[] out;

        // Number of elements to sum
        final int nbSum = this.getRowDimension();

        if (isTranspose) {
            // Dimensions check (the provided matrix is M^T)
            MatrixUtils.checkMultiplicationCompatible(this, m);

            // Quadratic multiplication routine
            final int dim = m.getColumnDimension();
            out = new double[(dim * (dim + 1)) / 2];

            int index = 0;
            for (int i = 0; i < dim; i++) {
                for (int j = 0; j <= i; j++) {
                    // M x this x M^T
                    double sum = 0;
                    for (int k = 0; k < nbSum; k++) {
                        for (int l = 0; l < nbSum; l++) {
                            sum += m.getEntry(k, j) * m.getEntry(l, i) * this.getEntry(k, l);
                        }
                    }
                    out[index++] = sum;
                }
            }
        } else {
            // Dimensions check (the provided matrix is M)
            MatrixUtils.checkMultiplicationCompatible(m, this);

            // Quadratic multiplication routine
            final int dim = m.getRowDimension();
            out = new double[(dim * (dim + 1)) / 2];

            int index = 0;
            for (int i = 0; i < dim; i++) {
                for (int j = 0; j <= i; j++) {
                    // M x this x M^T
                    double sum = 0;
                    for (int k = 0; k < nbSum; k++) {
                        for (int l = 0; l < nbSum; l++) {
                            sum += m.getEntry(j, k) * m.getEntry(i, l) * this.getEntry(k, l);
                        }
                    }
                    out[index++] = sum;
                }
            }
        }

        // Return the matrix resulting from the quadratic multiplication
        return new ArrayRowSymmetricMatrix(out, false);
    }

    /** {@inheritDoc} */
    @Override
    public ArrayRowSymmetricMatrix power(final int p) {
        return new ArrayRowSymmetricMatrix(SymmetryType.MEAN, super.power(p), null, null);
    }

    /** {@inheritDoc} */
    @Override
    public ArrayRowSymmetricMatrix createMatrix(final int rowDimension, final int columnDimension) {
        MatrixUtils.checkDimension(rowDimension, columnDimension);
        return new ArrayRowSymmetricMatrix(rowDimension);
    }

    /**
     * Creates an identity matrix of the specified dimension.
     *
     * @param dim
     *        the dimension of the identity matrix
     * @return the identity matrix built
     */
    public static ArrayRowSymmetricMatrix createIdentityMatrix(final int dim) {
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
        return new ArrayRowSymmetricMatrix(data, false);
    }

    /** {@inheritDoc} */
    @Override
    public ArrayRowSymmetricMatrix copy() {
        return new ArrayRowSymmetricMatrix(this.data, true);
    }

    /** {@inheritDoc} */
    @Override
    public ArrayRowSymmetricMatrix transpose() {
        return this.transpose(true);
    }

    /** {@inheritDoc} */
    @Override
    public ArrayRowSymmetricMatrix transpose(final boolean forceCopy) {
        final ArrayRowSymmetricMatrix out;
        if (forceCopy) {
            out = this.copy();
        } else {
            out = this;
        }
        return out;
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix concatenateHorizontally(final RealMatrix m, final boolean rightConcatenation) {
        final RealMatrix matrix = MatrixUtils.createRealMatrix(getData(), false);
        return matrix.concatenateHorizontally(m, rightConcatenation);
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix concatenateVertically(final RealMatrix m, final boolean lowerConcatenation) {
        final RealMatrix matrix = MatrixUtils.createRealMatrix(getData(), false);
        return matrix.concatenateVertically(m, lowerConcatenation);
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix concatenateDiagonally(final RealMatrix m, final boolean rightConcatenation,
            final boolean lowerConcatenation) {
        final RealMatrix matrix = MatrixUtils.createRealMatrix(getData(), false);
        return matrix.concatenateDiagonally(m, rightConcatenation, lowerConcatenation);
    }

    /** {@inheritDoc} */
    @Override
    public ArrayRowSymmetricMatrix getInverse() {
        return new ArrayRowSymmetricMatrix(SymmetryType.MEAN, super.getInverse(), null, null);
    }

    /** {@inheritDoc} */
    @Override
    public ArrayRowSymmetricMatrix getInverse(
            final Function<RealMatrix, Decomposition> decompositionBuilder) {
        return new ArrayRowSymmetricMatrix(SymmetryType.MEAN,
                super.getInverse(decompositionBuilder), null, null);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isSquare() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isSymmetric() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isSymmetric(final double relativeTolerance) {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isSymmetric(final double relativeTolerance, final double absoluteTolerance) {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object object) {
        boolean out = false;

        if (object == this) {
            out = true;
        } else if (object != null) {
            if (object.getClass() == this.getClass()) {
                final ArrayRowSymmetricMatrix other = (ArrayRowSymmetricMatrix) object;
                out = Objects.deepEquals(this.data, other.data);
            } else if (object instanceof RealMatrix) {
                out = super.equals(object);
            }
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
     * Gets the default absolute threshold above which off-diagonal elements are considered
     * different.
     * <p>
     * A {@code null} value means the symmetry is enforced without any check on the absolute
     * difference between symmetric elements.
     * </p>
     *
     * @return the default absolute symmetry threshold (&ge;0 or {@code null})
     */
    public static Double getDefaultAbsoluteSymmetryThreshold() {
        return defaultAbsoluteSymmetryThreshold;
    }

    /**
     * Sets the default absolute threshold above which off-diagonal elements are considered
     * different.
     * <p>
     * A {@code null} value means the symmetry is enforced without any check on the absolute
     * difference between symmetric elements.
     * </p>
     *
     * @param threshold
     *        the new default absolute symmetry threshold (&ge;0 or {@code null})
     * @throws IllegalArgumentException
     *         if the provided threshold is {@code NaN} or is strictly negative
     */
    public static void setDefaultAbsoluteSymmetryThreshold(final Double threshold) {
        checkAbsoluteThreshold(threshold);
        defaultAbsoluteSymmetryThreshold = threshold;
    }

    /**
     * Gets the default relative threshold above which off-diagonal elements are considered
     * different.
     * <p>
     * A {@code null} value means the symmetry is enforced without any check on the relative
     * difference between symmetric elements.
     * </p>
     *
     * @return the default relative symmetry threshold (&ge;0 or {@code null})
     */
    public static Double getDefaultRelativeSymmetryThreshold() {
        return defaultRelativeSymmetryThreshold;
    }

    /**
     * Sets the default relative threshold above which off-diagonal elements are considered
     * different.
     * <p>
     * A {@code null} value means the symmetry is enforced without any check on the relative
     * difference between symmetric elements.
     * </p>
     *
     * @param threshold
     *        the new default relative symmetry threshold (&ge;0 or {@code null})
     * @throws IllegalArgumentException
     *         if the provided threshold is {@code NaN} or is strictly negative
     */
    public static void setDefaultRelativeSymmetryThreshold(final Double threshold) {
        checkRelativeThreshold(threshold);
        defaultRelativeSymmetryThreshold = threshold;
    }
}
