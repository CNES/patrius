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
* VERSION:4.13:DM:DM-120:08/12/2023:[PATRIUS] Merge de la branche patrius-for-lotus dans Patrius
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:FA:FA-2940:15/11/2021:[PATRIUS] Anomalies suite a DM 2766 sur package fr.cnes.sirius.patrius.math.linear 
 * VERSION:4.8:DM:DM-3040:15/11/2021:[PATRIUS]Reversement des evolutions de la branche patrius-for-lotus 
 * VERSION:4.7:DM:DM-2872:18/05/2021:Calcul de l'accélération dans la classe QuaternionPolynomialProfile 
 * VERSION:4.7:DM:DM-2767:18/05/2021:Evolutions et corrections diverses 
 * VERSION:4.7:DM:DM-2766:18/05/2021:Evol. et corr. dans le package fr.cnes.sirius.patrius.math.linear (suite DM 2300) 
 * VERSION:4.6:FA:FA-2542:27/01/2021:[PATRIUS] Definition d'un champ de vue avec demi-angle de 180° 
 * VERSION:4.5.1:FA:FA-2540:04/08/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear
 * VERSION:4.5:DM:DM-2300:27/05/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.linear;

import java.util.Arrays;
import java.util.function.Function;

import fr.cnes.sirius.patrius.math.exception.MathUnsupportedOperationException;
import fr.cnes.sirius.patrius.math.exception.NotPositiveException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Stores a symmetric positive semi-definite matrix as A&nbsp;=&nbsp;B&times;B<sup>T</sup>.
 * <p>
 * The matrix A is symmetric positive semi-definite by definition for any matrix B. Although the
 * matrix A is always a NxN matrix, this is not necessarily the case for the matrix B: any NxM
 * matrix multiplied by its transpose will yield a NxN matrix.
 * </p>
 * <p>
 * The decomposition matrix B is not unique and building it is left to the user. Typically, this
 * matrix is computed through a Cholesky decomposition A&nbsp;=&nbsp;L&times;L<sup>T</sup>, where L
 * is a lower-triangular matrix. The matrix yielded by this decomposition is unique for positive
 * definite matrices, but is not for positive semi-definite matrices (the implementation
 * {@link CholeskyDecomposition} does not even support the latter). It can also be obtained through
 * a LDL decomposition (A&nbsp;=&nbsp;L&times;D&times;L<sup>T</sup>), or through an eigen
 * decomposition (A&nbsp;=&nbsp;V&times;D&times;V<sup>T</sup>).
 * </p>
 *
 * @author Pierre Seimandi (GMV)
 */
public class DecomposedSymmetricPositiveMatrix extends AbstractRealMatrix implements
        SymmetricPositiveMatrix {

     /** Serializable UID. */
    private static final long serialVersionUID = -6677890418754861398L;

    /** Matrix B<sup>T</sup> of the decomposition A = B&times;B<sup>T</sup>. */
    private RealMatrix matrixBT;

    /** Matrix A (cached to optimize {@link #getEntry(int, int)}) (lower triangular part only). */
    private double[] cachedA;

    /**
     * Builds a new {@linkplain DecomposedSymmetricPositiveMatrix} of dimension n (filled with
     * zero).
     *
     * @param n
     *        the dimension of the matrix
     */
    public DecomposedSymmetricPositiveMatrix(final int n) {
        super(n, n);
        this.matrixBT = MatrixUtils.createRealMatrix(n, n);
    }

    /**
     * Builds a new {@link DecomposedSymmetricPositiveMatrix} by specifying the data of a matrix
     * B<sup>T</sup> which satisfies A&nbsp;=&nbsp;B&times;B<sup>T</sup>.
     * <p>
     * The provided array is copied, not referenced.
     * </p>
     *
     * @param dataBT
     *        the data of the matrix B<sup>T</sup>
     */
    public DecomposedSymmetricPositiveMatrix(final double[][] dataBT) {
        this(dataBT, true);
    }

    /**
     * Builds a new {@link DecomposedSymmetricPositiveMatrix} by specifying the data of a matrix
     * B<sup>T</sup> which satisfies A&nbsp;=&nbsp;B&times;B<sup>T</sup>.
     *
     * @param dataBT
     *        the data of the matrix B<sup>T</sup>
     * @param copyArray
     *        if {@code false}, the provided array will be referenced instead of being copied
     */
    public DecomposedSymmetricPositiveMatrix(final double[][] dataBT, final boolean copyArray) {
        this(new Array2DRowRealMatrix(dataBT, copyArray), false);
    }

    /**
     * Builds a new {@link SymmetricPositiveMatrix} by specifying a matrix B<sup>T</sup> which
     * satisfies A&nbsp;=&nbsp;B&times;B<sup>T</sup>.
     *
     * @param matrixBTranspose
     *        the matrix B<sup>T</sup>
     */
    public DecomposedSymmetricPositiveMatrix(final RealMatrix matrixBTranspose) {
        this(matrixBTranspose, true);
    }

    /**
     * Builds a new {@link SymmetricPositiveMatrix} by specifying a matrix B<sup>T</sup> which
     * satisfies A&nbsp;=&nbsp;B&times;B<sup>T</sup>.
     *
     * @param matrixBTranspose
     *        the matrix B<sup>T</sup>
     * @param copyMatrix
     *        if {@code false}, the provided matrix will be referenced instead of being copied
     */
    public DecomposedSymmetricPositiveMatrix(final RealMatrix matrixBTranspose,
            final boolean copyMatrix) {
        super();

        // Ensure the matrix is not null
        if (matrixBTranspose == null) {
            throw new NullArgumentException(PatriusMessages.NULL_MATRIX_NOT_ALLOWED);
        }

        if (copyMatrix) {
            this.matrixBT = matrixBTranspose.copy();
        } else {
            this.matrixBT = matrixBTranspose;
        }
    }

    /**
     * Gets the matrix B of the decomposition A = B&times;B<sup>T</sup> of this matrix.
     * <p>
     * This method returns a copy of the B<sup>T</sup> matrix stored internally.
     * </p>
     *
     * @return the matrix B
     */
    public RealMatrix getB() {
        return this.matrixBT.transpose();
    }

    /**
     * Gets the matrix B<sup>T</sup> of the decomposition A = B&times;B<sup>T</sup> of this matrix.
     *
     * @return the matrix B<sup>T</sup> (copy)
     */
    public RealMatrix getBT() {
        return getBT(true);
    }

    /**
     * Gets the matrix B<sup>T</sup> of the decomposition A = B&times;B<sup>T</sup> of this matrix.
     * <p>
     * This method returns a copy of the matrix B<sup>T</sup> if {@code copyMatrix} is set to
     * {@code true}. Otherwise, it returns a direct reference to the matrix stored internally.
     * Access to the internal matrix is provided for optimization purposes only. Modifying the
     * returned matrix is strongly discouraged, as any change made on it will also apply to this
     * matrix.
     * </p>
     *
     * @param copyMatrix
     *        whether to return a copy of the B<sup>T</sup> matrix ({@code true}) or its reference (
     *        {@code false})
     * @return the matrix B<sup>T</sup> (copy or reference)
     */
    public RealMatrix getBT(final boolean copyMatrix) {
        final RealMatrix out;
        if (copyMatrix) {
            out = this.matrixBT.copy();
        } else {
            out = this.matrixBT;
        }
        return out;
    }

    /** {@inheritDoc} */
    @Override
    public int getRowDimension() {
        return this.matrixBT.getColumnDimension();
    }

    /** {@inheritDoc} */
    @Override
    public int getColumnDimension() {
        return this.matrixBT.getColumnDimension();
    }

    /**
     * Gets this matrix's transparent dimension (i.e. the number of columns of the matrix B).
     *
     * @return this matrix's transparent dimension
     */
    public int getTransparentDimension() {
        return this.matrixBT.getRowDimension();
    }

    /**
     * Gets the matrix A = B&times;B<sup>T</sup> represented by this instance, stored in a new
     * {@linkplain ArrayRowSymmetricMatrix}.
     *
     * @return a new {@linkplain ArrayRowSymmetricMatrix} instance storing the matrix A represented
     *         by this instance
     */
    public ArrayRowSymmetricMatrix toArrayRowSymmetricMatrix() {
        final int n = this.getRowDimension();
        final int l = (n * (n + 1)) / 2;

        // Initialize the cache if it has not be done yet.
        // (only the lower-triangular part is stored since the matrix is symmetric)
        if (this.cachedA == null) {
            this.cachedA = new double[l];
        }

        // Compute and build the matrix A if not computed yet
        int index = 0;
        for (int row = 0; row < this.getRowDimension(); row++) {
            for (int column = 0; column <= row; column++) {
                // Compute A(i,j) if it was not computed yet.
                if (this.cachedA[index] == 0.) {
                    final RealVector rowVector = this.matrixBT.getColumnVector(row);
                    final RealVector columnVector = this.matrixBT.getColumnVector(column);
                    this.cachedA[index] = rowVector.dotProduct(columnVector);
                }
                index++;
            }
        }

        // Build the symmetric matrix
        return new ArrayRowSymmetricMatrix(this.cachedA, true);
    }

    /**
     * Gets the matrix A = B&times;B<sup>T</sup> represented by this instance, stored in a new
     * {@linkplain ArrayRowSymmetricPositiveMatrix}.
     *
     * @return a new {@linkplain ArrayRowSymmetricPositiveMatrix} instance storing the matrix A
     *         represented by this instance
     */
    public ArrayRowSymmetricPositiveMatrix toArrayRowSymmetricPositiveMatrix() {
        final ArrayRowSymmetricMatrix symmetricMatrix = this.toArrayRowSymmetricMatrix();
        return new ArrayRowSymmetricPositiveMatrix(symmetricMatrix.getDataRef(), false);
    }

    /** {@inheritDoc} */
    @Override
    public double getEntry(final int row, final int column) {
        // Check the indices
        MatrixUtils.checkMatrixIndex(this, row, column);

        // Matrix dimension and size of the cache array
        final int n = this.getRowDimension();
        final int l = (n * (n + 1)) / 2;

        // Initialize the cache if it has not be done yet.
        // (only the lower-triangular part is stored since the matrix is symmetric)
        if (this.cachedA == null) {
            this.cachedA = new double[l];
        }

        // Index of A(i,j) in the cache
        final int i = MathLib.min(row, column);
        final int j = MathLib.max(row, column);
        final int index = ((j * (j + 1)) / 2) + i;

        // Compute A(i,j) if it was not computed yet.
        if (this.cachedA[index] == 0.) {
            final RealVector rowVector = this.matrixBT.getColumnVector(row);
            final RealVector columnVector = this.matrixBT.getColumnVector(column);
            this.cachedA[index] = rowVector.dotProduct(columnVector);
        }

        // Return A(i,j).
        return this.cachedA[index];
    }

    /**
     * Sets the entry for the specified row and column.
     * <p>
     * <b>Important:</b><br>
     * This method systematically throws a {@link MathUnsupportedOperationException} since this
     * operation is not safe when dealing with symmetric positive definite matrices (the properties
     * of the matrix are not guaranteed to be preserved).
     * </p>
     *
     * @param row
     *        the row index of entry to be set.
     * @param column
     *        the column index of entry to be set.
     * @param value
     *        the new value of the entry.
     * @throws MathUnsupportedOperationException
     *         systematically, since this operation is not supported
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
     *         systematically (this operation is forbidden)
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
     */
    @Override
    public void multiplyEntry(final int row, final int column, final double factor) {
        throw new MathUnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * The returned matrix is an {@linkplain DecomposedSymmetricPositiveMatrix} if the selected
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

            // The extraction of a submatrix is a regular matrix
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
     * The returned matrix is a {@linkplain DecomposedSymmetricPositiveMatrix} if the selected
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
                    return DecomposedSymmetricPositiveMatrix.this.getEntry(selectedRows[row],
                            selectedColumns[column]);
                }
            });
        }

        return subMatrix;
    }

    /** {@inheritDoc} */
    @Override
    public DecomposedSymmetricPositiveMatrix getSubMatrix(final int startIndex, final int endIndex) {
        // Check the indices
        MatrixUtils.checkSubMatrixIndex(this, startIndex, endIndex, startIndex, endIndex);

        // Extract the submatrix
        final int dimension = endIndex - startIndex + 1;
        final int transparentDimension = this.matrixBT.getRowDimension();
        final RealMatrix extractedBT = MatrixUtils
                .createRealMatrix(transparentDimension, dimension);
        for (int j = startIndex; j <= endIndex; j++) {
            extractedBT.setColumn(j - startIndex, this.matrixBT.getColumn(j));
        }
        return new DecomposedSymmetricPositiveMatrix(extractedBT, false);
    }

    /** {@inheritDoc} */
    @Override
    public DecomposedSymmetricPositiveMatrix getSubMatrix(final int[] indices) {
        // Check the indices
        MatrixUtils.checkSubMatrixIndex(this, indices, indices);

        // Extract the submatrix
        final int dimension = indices.length;
        final int transparentDimension = this.matrixBT.getRowDimension();
        final RealMatrix extractedBT = MatrixUtils
                .createRealMatrix(transparentDimension, dimension);
        for (int j = 0; j < dimension; j++) {
            extractedBT.setColumn(j, this.matrixBT.getColumn(indices[j]));
        }
        return new DecomposedSymmetricPositiveMatrix(extractedBT, false);
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

    /** {@inheritDoc} */
    @Override
    public RealMatrix getRowMatrix(final int row) {
        MatrixUtils.checkRowIndex(this, row);
        final int nCols = this.getColumnDimension();
        final double[] rowData = new double[nCols];
        for (int i = 0; i < nCols; ++i) {
            rowData[i] = this.getEntry(row, i);
        }
        return MatrixUtils.createRowRealMatrix(rowData);
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix getColumnMatrix(final int column) {
        MatrixUtils.checkColumnIndex(this, column);
        final int nRows = this.getRowDimension();
        final double[] colData = new double[nRows];
        for (int i = 0; i < nRows; ++i) {
            colData[i] = this.getEntry(column, i);
        }
        return MatrixUtils.createColumnRealMatrix(colData);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * <b>Important:</b><br>
     * This method systematically throws a {@link MathUnsupportedOperationException} since this operation is not safe
     * when dealing with symmetric positive definite matrices (the properties of the matrix are not guaranteed to be
     * preserved).
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
    public SymmetricMatrix scalarAdd(final double d) {
        final SymmetricMatrix out;

        if (d == 0) {
            // Simple case, just copy the matrix
            out = this.copy();
        } else if (d > 0) {
            // Call the optimized code for positive scalars
            out = positiveScalarAdd(d);
        } else {
            // Addition routine
            out = this.toArrayRowSymmetricMatrix();
            for (int i = 0; i < this.getRowDimension(); i++) {
                for (int j = 0; j <= i; j++) {
                    out.addToEntry(i, j, d);
                }
            }
        }

        return out;
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * The scalar addition is computed by doing a vertical concatenation between the B<sup>T</sup>
     * matrix and a row matrix storing the square root of the scalar to add.
     * </p>
     */
    @Override
    public DecomposedSymmetricPositiveMatrix positiveScalarAdd(final double d) {
        // Ensure the provided scalar is positive
        if (d < 0) {
            throw new NotPositiveException(PatriusMessages.NOT_POSITIVE_SCALAR, d);
        }

        // Create the B^T matrix to be concatenated with the current B^T matrix
        final RealMatrix rowMatrix = MatrixUtils.createRealMatrix(1, this.getColumnDimension());
        final double sqrtD = MathLib.sqrt(d);
        for (int i = 0; i < this.getColumnDimension(); i++) {
            rowMatrix.setEntry(0, i, sqrtD);
        }

        // Concatenate the two B^T matrices and use the result to build a new decomposed matrix
        final RealMatrix concatenatedBT = MatrixUtils.concatenateVertically(this.matrixBT,
                rowMatrix);
        return new DecomposedSymmetricPositiveMatrix(concatenatedBT);
    }

    /** {@inheritDoc} */
    @Override
    public SymmetricMatrix scalarMultiply(final double d) {
        final SymmetricMatrix out;

        // Quick escape
        if (d == 0) {
            // Simple case
            out = new DecomposedSymmetricPositiveMatrix(this.getRowDimension());
        } else if (d > 0) {
            // Call the optimized code for positive scalars
            out = this.positiveScalarMultiply(d);
        } else {
            // Scalar multiplication routine on the matrix A
            out = this.toArrayRowSymmetricMatrix();
            for (int i = 0; i < this.getRowDimension(); i++) {
                for (int j = 0; j <= i; j++) {
                    out.multiplyEntry(i, j, d);
                }
            }
        }

        return out;
    }

    /**  {@inheritDoc} */
    @Override
    public DecomposedSymmetricPositiveMatrix positiveScalarMultiply(final double d) {
        // Ensure the provided scalar is positive
        if (d < 0) {
            throw new NotPositiveException(PatriusMessages.NOT_POSITIVE_SCALAR, d);
        }

        // Scalar multiplication routine
        final RealMatrix matrixB = this.matrixBT.scalarMultiply(MathLib.sqrt(d));
        return new DecomposedSymmetricPositiveMatrix(matrixB);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * The returned matrix is, in order of priority:
     * </p>
     * <ul>
     * <li>A {@linkplain DecomposedSymmetricPositiveMatrix} if the provided matrix is a
     * {@linkplain DecomposedSymmetricPositiveMatrix};</li>
     * <li>An {@linkplain ArrayRowSymmetricPositiveMatrix} if it is any other type of
     * {@linkplain SymmetricPositiveMatrix};</li>
     * <li>An {@linkplain ArrayRowSymmetricMatrix} if it is any other type of
     * {@linkplain SymmetricMatrix};</li>
     * <li>An {@linkplain Array2DRowRealMatrix} or a {@linkplain BlockRealMatrix} if it is any other
     * type of {@linkplain RealMatrix}.
     * </ul>
     */
    @Override
    public RealMatrix add(final RealMatrix m) {
        final RealMatrix out;

        if (m instanceof SymmetricMatrix) {
            // Call the optimized code for symmetric matrix
            out = this.add((SymmetricMatrix) m);
        } else {
            // Dimensions check
            MatrixUtils.checkAdditionCompatible(this, m);

            // Addition routine
            final int dim = this.getRowDimension();
            final double[][] outData = m.getData();
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

    /**
     * {@inheritDoc}
     *
     * <p>
     * The returned matrix is, in order of priority:
     * </p>
     * <ul>
     * <li>A {@linkplain DecomposedSymmetricPositiveMatrix} if the provided matrix is a
     * {@linkplain DecomposedSymmetricPositiveMatrix};</li>
     * <li>An {@linkplain ArrayRowSymmetricPositiveMatrix} if it is any other type of
     * {@linkplain SymmetricPositiveMatrix};</li>
     * <li>An {@linkplain ArrayRowSymmetricMatrix} if it is any other type of
     * {@linkplain SymmetricMatrix}.</li>
     * </ul>
     */
    @Override
    public SymmetricMatrix add(final SymmetricMatrix m) {
        final SymmetricMatrix out;

        if (m instanceof SymmetricPositiveMatrix) {
            // Call the optimized code for symmetric positive matrices
            out = this.add((SymmetricPositiveMatrix) m);
        } else {
            // Dimensions check
            MatrixUtils.checkAdditionCompatible(this, m);

            // Addition routine
            // (relies on the fact that the symmetric element is automatically modified)
            out = this.toArrayRowSymmetricMatrix();
            for (int i = 0; i < this.getRowDimension(); i++) {
                for (int j = 0; j <= i; j++) {
                    out.addToEntry(i, j, m.getEntry(i, j));
                }
            }
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
     * <li>A {@linkplain DecomposedSymmetricPositiveMatrix} if the provided matrix is a
     * {@linkplain DecomposedSymmetricPositiveMatrix};</li>
     * <li>An {@linkplain ArrayRowSymmetricPositiveMatrix} if it is any other type of
     * {@linkplain SymmetricPositiveMatrix};</li>
     * </ul>
     */
    @Override
    public SymmetricPositiveMatrix add(final SymmetricPositiveMatrix m) {
        final SymmetricPositiveMatrix out;

        if (m instanceof DecomposedSymmetricPositiveMatrix) {
            // Call the optimized code for decomposed symmetric positive matrices
            out = this.add((DecomposedSymmetricPositiveMatrix) m);
        } else {
            // Dimensions check
            MatrixUtils.checkAdditionCompatible(this, m);

            // Addition routine
            // (relies on the fact that the symmetric element is automatically modified)
            final ArrayRowSymmetricMatrix sum = this.toArrayRowSymmetricMatrix();
            for (int i = 0; i < this.getRowDimension(); i++) {
                for (int j = 0; j <= i; j++) {
                    sum.addToEntry(i, j, m.getEntry(i, j));
                }
            }
            out = new ArrayRowSymmetricPositiveMatrix(sum.getDataRef(), false);
        }

        return out;
    }

    /**
     * Adds another {@link DecomposedSymmetricPositiveMatrix} to this matrix.
     * <p>
     * Adding another {@link DecomposedSymmetricPositiveMatrix} amounts to concatenating their B
     * matrices horizontally. As a result, the number of column of the matrix B increases with each
     * addition. The concatenated matrix B is automatically resized afterward to ensure it is a NxN
     * matrix.
     * </p>
     *
     * @param m
     *        the matrix to add
     * @return the sum of the two matrices
     * @see DecomposedSymmetricPositiveMatrix#add(DecomposedSymmetricPositiveMatrix, boolean)
     * @see DecomposedSymmetricPositiveMatrix#resizeB()
     */
    public DecomposedSymmetricPositiveMatrix add(final DecomposedSymmetricPositiveMatrix m) {
        return this.add(m, true);
    }

    /**
     * Adds another {@link DecomposedSymmetricPositiveMatrix} to this matrix.
     * <p>
     * Adding another {@link DecomposedSymmetricPositiveMatrix} amounts to concatenating their B
     * matrices horizontally. As a result, the number of column of the matrix B increases with each
     * addition. Setting {@code resize} to {@code true} triggers a reduction or an extension of the
     * matrix B afterward to ensure it is a NxN matrix.
     * </p>
     *
     * @param m
     *        the matrix to add
     * @param resize
     *        if {@code true}, the matrix B is resized afterward to ensure it is NxN
     * @return the sum of the two matrices
     * @see DecomposedSymmetricPositiveMatrix#resizeB()
     */
    public DecomposedSymmetricPositiveMatrix add(final DecomposedSymmetricPositiveMatrix m,
            final boolean resize) {
        // Dimensions check
        MatrixUtils.checkAdditionCompatible(this, m);

        // Concatenate the two matrices B^T
        final RealMatrix concatenatedBT = MatrixUtils.concatenateVertically(this.matrixBT,
                m.matrixBT);

        // Build the new symmetric positive matrix
        DecomposedSymmetricPositiveMatrix out = new DecomposedSymmetricPositiveMatrix(
                concatenatedBT);
        if (resize) {
            out = out.resizeB();
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
     * <li>An {@linkplain ArrayRowSymmetricPositiveMatrix} if it is a
     * {@linkplain SymmetricPositiveMatrix};</li>
     * <li>An {@linkplain ArrayRowSymmetricMatrix} if it is any other type of
     * {@linkplain SymmetricMatrix}.</li>
     * </ul>
     */
    @Override
    public RealMatrix subtract(final RealMatrix m) {
        final RealMatrix out;

        if (m instanceof SymmetricMatrix) {
            // Call the optimized code for symmetric matrix
            out = subtract((SymmetricMatrix) m);
        } else {
            // Dimensions check
            MatrixUtils.checkSubtractionCompatible(this, m);

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
        // Dimensions check
        MatrixUtils.checkSubtractionCompatible(this, m);

        // Subtraction routine
        // (relies on the fact that the symmetric element is automatically modified)
        final ArrayRowSymmetricMatrix out = this.toArrayRowSymmetricMatrix();
        for (int i = 0; i < this.getRowDimension(); i++) {
            for (int j = 0; j <= i; j++) {
                out.addToEntry(i, j, -m.getEntry(i, j));
            }
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

    /** {@inheritDoc}. */
    @Override
    public DecomposedSymmetricPositiveMatrix quadraticMultiplication(final RealMatrix m) {
        return this.quadraticMultiplication(m, false);
    }

    /** {@inheritDoc}. */
    @Override
    public DecomposedSymmetricPositiveMatrix quadraticMultiplication(final RealMatrix m,
            final boolean isTranspose) {
        // Dimensions check
        if (isTranspose) {
            MatrixUtils.checkMultiplicationCompatible(this, m);
        } else {
            MatrixUtils.checkMultiplicationCompatible(m, this);
        }

        // Quadratic multiplication routine:
        // Since M x this x M^T = (M x B) x (B^T x M^T), only B^T x M^T needs to be computed.
        return new DecomposedSymmetricPositiveMatrix(this.matrixBT.multiply(m, !isTranspose));
    }

    /** {@inheritDoc} */
    @Override
    public DecomposedSymmetricPositiveMatrix power(final int p) {
        // Only positive exponents are accepted
        if (p < 0) {
            throw new NotPositiveException(PatriusMessages.NOT_POSITIVE_EXPONENT, p);
        }

        final DecomposedSymmetricPositiveMatrix out;

        if (p == 0) {
            // If p is equal to 0, return the identity matrix
            final RealMatrix matI = MatrixUtils.createRealIdentityMatrix(this.getRowDimension(),
                    true);
            out = new DecomposedSymmetricPositiveMatrix(matI, false);
        } else if (p == 1) {
            // If p is equal to 1, return a copy of this matrix 
            out = this.copy();
        } else {
            RealMatrix matrix = new Array2DRowRealMatrix(this.getData(), false);
            // Power computation routine
            matrix = matrix.power(p / 2);
            if ((p % 2) != 0) {
                matrix = matrix.preMultiply(this.matrixBT);
            }
            out = new DecomposedSymmetricPositiveMatrix(matrix, false);
        }

        return out;
    }

    /**
     * Reduces or extends the matrix B to ensure it is a NxN matrix.
     * <p>
     * If the matrix B has more columns than rows, this method replaces the current matrix B by the
     * matrix R resulting from the QR decomposition of B<sup>T</sup>, after truncating it to its
     * first N rows (where N is the dimension of this matrix). If it has less columns than rows, it
     * simply adds new columns filled with zero.
     * </p>
     *
     * @return this matrix
     */
    // Reason: null value used on purpose
    @SuppressWarnings("PMD.NullAssignment")
    public DecomposedSymmetricPositiveMatrix resizeB() {
        this.matrixBT = this.getResizedBT();
        this.cachedA = null;
        return this;
    }

    /**
     * Gets the matrix B after reducing or extending it to match this matrix's dimensions.
     * <p>
     * If the matrix B has more columns than rows, this method returns the transpose of the matrix R
     * resulting from the QR decomposition of B<sup>T</sup>, after truncating it to its first N rows
     * (where N is the dimension of this matrix). If the matrix B has less columns than rows, this
     * methods simply adds columns filled with zero to match its row dimensions.
     * </p>
     *
     * @return the resized matrix B
     */
    public RealMatrix getResizedB() {
        return this.getResizedBT().transpose();
    }

    /**
     * Gets the matrix B<sup>T</sup> after reducing or extending it to match this matrix's
     * dimensions.
     * <p>
     * If the matrix B has more columns than rows, this method returns the matrix R resulting from
     * the QR decomposition of B<sup>T</sup>, after truncating it to its first N rows (where N is
     * the dimension of this matrix). If the matrix B has less columns than rows, this methods
     * simply adds columns filled with zero to match its row dimensions.
     * </p>
     *
     * @return the resized matrix B<sup>T</sup>
     */
    public RealMatrix getResizedBT() {
        RealMatrix matBT = this.matrixBT;
        final int dim = this.getRowDimension();

        if (this.getTransparentDimension() < dim) {
            matBT = MatrixUtils.resize(matBT, this.getRowDimension(), this.getColumnDimension());
        } else if (this.getTransparentDimension() > dim) {
            final QRDecomposition decomposition = new QRDecomposition(matBT);
            matBT = decomposition.getR().getSubMatrix(0, dim - 1, 0, dim - 1);
        }

        return matBT;
    }

    /** {@inheritDoc} */
    @Override
    public DecomposedSymmetricPositiveMatrix createMatrix(final int rowDimension,
            final int columnDimension) {
        MatrixUtils.checkDimension(rowDimension, columnDimension);
        return new DecomposedSymmetricPositiveMatrix(rowDimension);
    }

    /**
     * Creates an identity matrix of the specified dimension.
     *
     * @param dim
     *        the dimension of the identity matrix
     * @return the identity matrix built
     */
    public static DecomposedSymmetricPositiveMatrix createIdentityMatrix(final int dim) {
        MatrixUtils.checkRowDimension(dim);
        final RealMatrix identityMatrix = MatrixUtils.createRealIdentityMatrix(dim);
        return new DecomposedSymmetricPositiveMatrix(identityMatrix, false);
    }

    /** {@inheritDoc} */
    @Override
    public DecomposedSymmetricPositiveMatrix copy() {
        return new DecomposedSymmetricPositiveMatrix(this.matrixBT.getData(), false);
    }

    /** {@inheritDoc}. */
    @Override
    public DecomposedSymmetricPositiveMatrix transpose() {
        return this.transpose(true);
    }

    /** {@inheritDoc}. */
    @Override
    public DecomposedSymmetricPositiveMatrix transpose(final boolean forceCopy) {
        final DecomposedSymmetricPositiveMatrix out;
        if (forceCopy == true) {
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

    /**
     * Gets the inverse of the matrix.
     * <p>
     * The inverse matrix is calculated using the default decomposition as follows:<br>
     * If A<sub>1</sub>&nbsp;=&nbsp;B<sub>1</sub>&nbsp;&times&nbsp;B<sub>1</sub><sup>T</sup> and
     * A<sub>2</sub>&nbsp;=&nbsp;B<sub>2</sub>&nbsp;&times&nbsp;B<sub>2</sub><sup>T</sup>&nbsp;
     * =&nbsp;A<sub>1</sub><sup>-1</sup>, then we have
     * B<sub>2</sub><sup>T</sup>&nbsp;=&nbsp;B<sub>1</sub><sup>-1</sup>.
     * </p>
     *
     * @return the inverse matrix
     */
    @Override
    public DecomposedSymmetricPositiveMatrix getInverse() {
        return getInverse(getDefaultDecomposition());
    }

    /**
     * Gets the inverse of the matrix.
     * <p>
     * The inverse of B<sub>1</sub> is computed using the provided decomposition as follows:<br>
     * If A<sub>1</sub>&nbsp;=&nbsp;B<sub>1</sub>&nbsp;&times&nbsp;B<sub>1</sub><sup>T</sup> and
     * A<sub>2</sub>&nbsp;=&nbsp;B<sub>2</sub>&nbsp;&times&nbsp;B<sub>2</sub><sup>T</sup>&nbsp;
     * =&nbsp;A<sub>1</sub><sup>-1</sup>, then we have
     * B<sub>2</sub><sup>T</sup>&nbsp;=&nbsp;B<sub>1</sub><sup>-1</sup>.
     * </p>
     *
     * @param decompositionBuilder
     *        the decomposition algorithm to use
     * @return the inverse matrix
     */
    @Override
    public DecomposedSymmetricPositiveMatrix getInverse(
            final Function<RealMatrix, Decomposition> decompositionBuilder) {
        final RealMatrix matrixBT2 = this.getResizedB().getInverse(decompositionBuilder);
        return new DecomposedSymmetricPositiveMatrix(matrixBT2);
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
     * Cast or transform the provided matrix into a {@link DecomposedSymmetricPositiveMatrix}.
     *
     * <p>
     * The transformation is performed thanks to a {@link CholeskyDecomposition}. In this case, the input matrix must be
     * positive definite otherwise an exception is thrown).
     * </p>
     *
     * @param matrix
     *        The symmetric positive matrix to cast or transform
     * @return the decomposed symmetric matrix (might be the same instance as the input if only a cast was performed)
     * @throws NonPositiveDefiniteMatrixException
     *         if a transformation is necessary and the matrix is not positive definite
     */
    public static final DecomposedSymmetricPositiveMatrix castOrTransform(final SymmetricPositiveMatrix matrix) {
        final DecomposedSymmetricPositiveMatrix outputMatrix;
        if (matrix instanceof DecomposedSymmetricPositiveMatrix) {
            outputMatrix = ((DecomposedSymmetricPositiveMatrix) matrix);
        } else {
            final CholeskyDecomposition chol = new CholeskyDecomposition(matrix);
            outputMatrix = new DecomposedSymmetricPositiveMatrix(chol.getLT(), false);
        }
        return outputMatrix;
    }
}
