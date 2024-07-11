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
* VERSION:4.8:FA:FA-2941:15/11/2021:[PATRIUS] Correction anomalies suite a DM 2767 
* VERSION:4.8:DM:DM-3040:15/11/2021:[PATRIUS]Reversement des evolutions de la branche patrius-for-lotus 
 * VERSION:4.7:DM:DM-2766:18/05/2021:Evol. et corr. dans le package fr.cnes.sirius.patrius.math.linear (suite DM 2300) 
 * VERSION:4.6:FA:FA-2542:27/01/2021:[PATRIUS] Definition d'un champ de vue avec demi-angle de 180° 
 * VERSION:4.5.1:FA:FA-2540:04/08/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear
 * VERSION:4.5:DM:DM-2300:27/05/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear 
 * VERSION:4.3:DM:DM-2089:15/05/2019:[PATRIUS] passage a Java 8
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION:4.3:FA:FA-1996:15/05/2019:[PATRIUS] Erreur de la méthode getSubMatrix avec DiagonalMatrix
 * VERSION::DM:482:02/11/2015:Add methods addSym, subtractSym, multiplySym to match the new class SymmetricMatrix
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.linear;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;

import fr.cnes.sirius.patrius.math.exception.MathUnsupportedOperationException;
import fr.cnes.sirius.patrius.math.exception.NoDataException;
import fr.cnes.sirius.patrius.math.exception.NotPositiveException;
import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooLargeException;
import fr.cnes.sirius.patrius.math.linear.ArrayRowSymmetricMatrix.SymmetryType;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Diagonal matrix.
 * <p>
 * This implementation only stores the diagonal elements of the matrix. Setting any off-diagonal
 * element to a value that is not 0 is forbidden (otherwise the matrix would not be diagonal
 * anymore) and attempting to do so will systematically result in a
 * {@linkplain MathUnsupportedOperationException} being thrown. As a result, any method which
 * modifies the entries of the matrix (setRow, setColumn, setSubMatrix, etc) will most likely fail,
 * unless only the diagonal elements are actually modified.
 * </p>
 * <p>
 * This class is up-to-date with commons-math 3.6.1, but has been mostly rewritten since.
 * </p>
 *
 * @version $Id: DiagonalMatrix.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class DiagonalMatrix extends AbstractRealMatrix implements SymmetricMatrix {

    /** Serial version UID. */
    private static final long serialVersionUID = 20121229L;

    /** Entries of the diagonal. */
    private final double[] data;

    /**
     * Creates a matrix with the supplied dimension.
     *
     * @param n
     *        the dimension of the matrix
     *
     * @throws NotStrictlyPositiveException
     *         if the provided dimension is not positive
     */
    public DiagonalMatrix(final int n) {
        super(n, n);
        this.data = new double[n];
    }

    /**
     * Creates a matrix using the value of the provided array as diagonal elements.
     * <p>
     * The input array is copied, not referenced.
     * </p>
     *
     * @param diagonalElements
     *        the diagonal elements of the matrix
     * @throws NullArgumentException
     *         if {@code diagonalElements} is {@code null}
     */
    public DiagonalMatrix(final double[] diagonalElements) {
        this(diagonalElements, true);
    }

    /**
     * Creates a matrix using the value of the provided array as diagonal elements.
     * <p>
     * If an array is created specifically for this constructor (in order to be embedded in the
     * created instance and not used directly), {@code copyArray} may be set to {@code false}. This
     * will improve performance as no new array will be built and no data will be copied.
     * </p>
     *
     * @param diagonalElements
     *        the diagonal elements of the matrix
     * @param copyArray
     *        if {@code true}, the input array will be copied, otherwise it will be referenced
     * @throws NullArgumentException
     *         if {@code diagonalElements} is {@code null}
     */
    public DiagonalMatrix(final double[] diagonalElements, final boolean copyArray) {
        super();
        checkDataArray(diagonalElements);

        if (copyArray) {
            this.data = diagonalElements.clone();
        } else {
            this.data = diagonalElements;
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

    /** {@inheritDoc} */
    @Override
    public int getRowDimension() {
        return this.data.length;
    }

    /** {@inheritDoc} */
    @Override
    public int getColumnDimension() {
        return this.data.length;
    }

    /** {@inheritDoc} */
    @Override
    public double getMin() {
        double min;

        if (this.getRowDimension() == 1) {
            // There is no off-diagonal elements: the only entry is the minimum
            min = this.getEntry(0, 0);
        } else {
            // There are off-diagonal elements
            // The minimum is initially set to zero
            min = 0.;
            final int n = this.getRowDimension();
            for (int i = 0; i < n; i++) {
                // Search the min element on the diagonal
                min = MathLib.min(min, this.data[i]);
            }
        }

        return min;
    }

    /** {@inheritDoc} */
    @Override
    public double getMax() {
        double max;

        if (this.getRowDimension() == 1) {
            // There is no off-diagonal elements: the only entry is the maximum
            max = this.getEntry(0, 0);
        } else {
            // There are off-diagonal elements
            // The maximum is initially set to zero
            max = 0.;
            final int n = this.getRowDimension();
            for (int i = 0; i < n; i++) {
                // Search the max element on the diagonal
                max = MathLib.max(max, this.data[i]);
            }
        }

        return max;
    }

    /** {@inheritDoc} */
    @Override
    public double getTrace() {
        double trace = 0.;
        final int n = this.getRowDimension();
        for (int i = 0; i < n; i++) {
            trace += this.data[i];
        }
        return trace;
    }

    /** {@inheritDoc} */
    @Override
    public double getNorm() {
        double norm = 0;
        for (int i = 0; i < this.data.length; i++) {
            norm = MathLib.max(norm, MathLib.abs(this.data[i]));
        }
        return norm;
    }

    /** {@inheritDoc} */
    @Override
    public double getFrobeniusNorm() {
        double norm = 0;
        for (int i = 0; i < this.data.length; i++) {
            norm += this.data[i] * this.data[i];
        }
        return MathLib.sqrt(norm);
    }

    /** {@inheritDoc} */
    @Override
    public double[][] getData() {
        final int n = this.getRowDimension();
        final double[][] out = new double[n][n];
        for (int i = 0; i < n; i++) {
            out[i][i] = this.data[i];
        }
        return out;
    }

    /**
     * Gets a reference to the internal data array storing the diagonal elements of the matrix.
     * <p>
     * <em>This method is only provided for optimization purposes.<br>
     * Any modification made on the returned array will change the values of this matrix.</em>
     * </p>
     *
     * @return a direct reference to the internal data array storing the diagonal elements of the
     *         matrix
     */
    // Reason: internal array access provided for optimization purposes
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    public double[] getDataRef() {
        return this.data;
    }

    /** {@inheritDoc} */
    @Override
    public double getEntry(final int row, final int column) {
        MatrixUtils.checkMatrixIndex(this, row, column);
        double entry = 0;
        if (row == column) {
            entry = this.data[row];
        }
        return entry;
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * <b>Important:</b><br>
     * This method throws a {@linkplain NumberIsTooLargeException} when asked to set an
     * extra-diagonal element to a non-zero value, since this operation is not allowed when dealing
     * with diagonal matrices (the properties of the matrix are not guaranteed to be preserved).
     * </p>
     *
     * @throws NumberIsTooLargeException
     *         if the method is asked to set an extra-diagonal element to a non-zero value
     */
    @Override
    public void setEntry(final int row, final int column, final double value) {
        MatrixUtils.checkMatrixIndex(this, row, column);
        if (row == column) {
            this.data[row] = value;
        } else {
            ensureZero(value);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * <b>Important:</b><br>
     * This method throws a {@linkplain NumberIsTooLargeException} when asked to add a non-zero
     * value to an extra-diagonal element, since this operation is not allowed when dealing with
     * diagonal matrices (the properties of the matrix are not guaranteed to be preserved).
     * </p>
     *
     * @throws NumberIsTooLargeException
     *         if the method is asked to add a non-zero value to an extra-diagonal element
     */
    @Override
    public void addToEntry(final int row, final int column, final double increment) {
        MatrixUtils.checkMatrixIndex(this, row, column);
        if (row == column) {
            this.data[row] += increment;
        } else {
            ensureZero(increment);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void multiplyEntry(final int row, final int column, final double factor) {
        MatrixUtils.checkMatrixIndex(this, row, column);
        if (row == column) {
            this.data[row] *= factor;
        }
    }

    /** {@inheritDoc} */
    @Override
    public double[] getRow(final int row) {
        MatrixUtils.checkRowIndex(this, row);
        final int n = this.getColumnDimension();
        final double[] rowData = new double[n];
        rowData[row] = this.data[row];
        return rowData;
    }

    /** {@inheritDoc} */
    @Override
    public RealVector getRowVector(final int row) {
        MatrixUtils.checkRowIndex(this, row);
        final int n = this.getColumnDimension();
        final RealVector rowVector = new ArrayRealVector(n);
        rowVector.setEntry(row, this.data[row]);
        return rowVector;
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix getRowMatrix(final int row) {
        MatrixUtils.checkRowIndex(this, row);
        final int n = this.getColumnDimension();
        final RealMatrix rowMatrix = MatrixUtils.createRealMatrix(1, n);
        rowMatrix.setEntry(0, row, this.data[row]);
        return rowMatrix;
    }

    /** {@inheritDoc} */
    @Override
    public double[] getColumn(final int column) {
        MatrixUtils.checkColumnIndex(this, column);
        final int n = this.getRowDimension();
        final double[] columnData = new double[n];
        columnData[column] = this.data[column];
        return columnData;
    }

    /** {@inheritDoc} */
    @Override
    public RealVector getColumnVector(final int column) {
        MatrixUtils.checkColumnIndex(this, column);
        final int n = this.getRowDimension();
        final RealVector columnVector = new ArrayRealVector(n);
        columnVector.setEntry(column, this.data[column]);
        return columnVector;
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix getColumnMatrix(final int column) {
        MatrixUtils.checkColumnIndex(this, column);
        final int n = this.getColumnDimension();
        final RealMatrix columnMatrix = MatrixUtils.createRealMatrix(n, 1);
        columnMatrix.setEntry(column, 0, this.data[column]);
        return columnMatrix;
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * The returned matrix is a {@linkplain DiagonalMatrix} if the selected rows and columns are the
     * same, and an {@linkplain Array2DRowRealMatrix} or a {@linkplain BlockRealMatrix} otherwise.
     * </p>
     */
    @Override
    public RealMatrix getSubMatrix(final int startRow, final int endRow, final int startColumn,
            final int endColumn) {
        final RealMatrix subMatrix;

        if ((startRow == startColumn) && (endRow == endColumn)) {
            // The extracted submatrix is a diagonal matrix
            subMatrix = getSubMatrix(startRow, endRow);
        } else {
            // Check the indices
            MatrixUtils.checkSubMatrixIndex(this, startRow, endRow, startColumn, endColumn);

            // The extracted submatrix is a regular matrix
            final int rowDimension = (endRow - startRow) + 1;
            final int columnDimension = (endColumn - startColumn) + 1;
            subMatrix = MatrixUtils.createRealMatrix(rowDimension, columnDimension);
            for (int i = startRow; i <= endColumn; i++) {
                for (int j = startColumn; j <= endRow; j++) {
                    if (i == j) {
                        subMatrix.setEntry(i - startRow, j - startColumn, this.data[i]);
                    }
                }
            }
        }

        // Return the submatrix
        return subMatrix;
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * The returned matrix is an {@linkplain ArrayRowSymmetricMatrix} if the selected rows and
     * columns are the same (same indices, same order), and an {@linkplain Array2DRowRealMatrix} or
     * a {@linkplain BlockRealMatrix} otherwise.
     * </p>
     */
    @Override
    public RealMatrix getSubMatrix(final int[] selectedRows, final int[] selectedColumns) {
        final RealMatrix subMatrix;

        if (Arrays.equals(selectedRows, selectedColumns)) {
            // The extracted submatrix is a symmetric matrix
            subMatrix = this.getSubMatrix(selectedRows);
        } else {
            // The extracted submatrix is a regular matrix
            MatrixUtils.checkSubMatrixIndex(this, selectedRows, selectedColumns);
            subMatrix = MatrixUtils.createRealMatrix(selectedRows.length, selectedColumns.length);
            subMatrix.walkInOptimizedOrder(new DefaultRealMatrixChangingVisitor() {
                /** {@inheritDoc} */
                @Override
                public double visit(final int row, final int column, final double value) {
                    return DiagonalMatrix.this.getEntry(selectedRows[row], selectedColumns[column]);
                }
            });
        }

        return subMatrix;
    }

    /** {@inheritDoc} */
    @Override
    public DiagonalMatrix getSubMatrix(final int startIndex, final int endIndex) {
        // Check the indices
        MatrixUtils.checkSubMatrixIndex(this, startIndex, endIndex, startIndex, endIndex);

        // Symmetric indices, the submatrix is a diagonal matrix
        final int dim = (endIndex - startIndex) + 1;
        final double[] diagonalElements = new double[dim];
        for (int i = startIndex; i <= endIndex; i++) {
            diagonalElements[i - startIndex] = this.data[i];
        }
        return new DiagonalMatrix(diagonalElements);
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
     * <p>
     * <b>Important:</b><br>
     * This method systematically throws a {@linkplain MathUnsupportedOperationException} since this
     * operation is not safe when dealing with diagonal matrices (the properties of the matrix are
     * not guaranteed to be preserved).
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
     * The returned matrix is an {@linkplain ArrayRowSymmetricMatrix}.
     * </p>
     */
    @Override
    public ArrayRowSymmetricMatrix scalarAdd(final double d) {
        // Initialize the array
        final int n = this.getRowDimension();
        final double[][] out = new double[n][n];
        // Scalar addition on each diagonal element
        for (int i = 0; i < n; i++) {
            out[i][i] = d + this.data[i];
            for (int j = 0; j < i; j++) {
                out[i][j] = d;
            }
        }
        // Store the array in a matrix
        return new ArrayRowSymmetricMatrix(SymmetryType.LOWER, out, null, null);
    }

    /** {@inheritDoc} */
    @Override
    public DiagonalMatrix scalarMultiply(final double d) {
        // Initialize the array
        final int n = this.getRowDimension();
        final double[] out = new double[n];
        // Scalar multiplication on each diagonal element
        for (int i = 0; i < n; i++) {
            out[i] = d * this.data[i];
        }
        // Store the array in a matrix
        return new DiagonalMatrix(out, false);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * The returned matrix is, in order of priority:
     * </p>
     * <ul>
     * <li>A {@linkplain DiagonalMatrix} if the provided matrix is a {@linkplain DiagonalMatrix};</li>
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
            // Call the optimized code for symmetric matrices
            out = this.add((SymmetricMatrix) m);
        } else {
            // Dimensions check
            MatrixUtils.checkAdditionCompatible(this, m);

            // Addition routine
            final int n = this.getRowDimension();
            final double[][] outData = m.getData();
            for (int i = 0; i < n; i++) {
                outData[i][i] += this.data[i];
            }
            return MatrixUtils.createRealMatrix(outData, false);
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
     * <li>A {@linkplain DiagonalMatrix} if the provided matrix is a {@linkplain DiagonalMatrix};</li>
     * <li>An {@linkplain ArrayRowSymmetricMatrix} if it is any other type of
     * {@linkplain SymmetricMatrix}.</li>
     * </ul>
     */
    @Override
    public SymmetricMatrix add(final SymmetricMatrix m) {
        final SymmetricMatrix out;

        if (m instanceof DiagonalMatrix) {
            // Call the optimized code for diagonal matrices
            out = this.add((DiagonalMatrix) m);
        } else {
            // Dimensions check
            MatrixUtils.checkAdditionCompatible(this, m);

            // Addition routine
            final int n = this.getRowDimension();
            final double[][] outData = m.getData();
            for (int i = 0; i < n; i++) {
                outData[i][i] += this.data[i];
            }
            out = new ArrayRowSymmetricMatrix(SymmetryType.LOWER, outData, null, null);
        }

        return out;
    }

    /**
     * Returns the result of adding the diagonal matrix {@code m} to this matrix.
     *
     * @param m
     *        the matrix to be added
     * @return the matrix resulting from the addition {@code this}+{@code m}
     * @throws MatrixDimensionMismatchException
     *         if the matrix {@code m} is not the same size as this matrix
     */
    public DiagonalMatrix add(final DiagonalMatrix m) {
        // Dimensions check
        MatrixUtils.checkAdditionCompatible(this, m);

        // Addition routine on the diagonal values
        final int n = this.getRowDimension();
        final double[] outData = new double[n];
        final double[] refData = m.getDataRef();
        for (int i = 0; i < n; i++) {
            outData[i] = this.data[i] + refData[i];
        }

        return new DiagonalMatrix(outData, false);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * The returned matrix is, in order of priority:
     * </p>
     * <ul>
     * <li>A {@linkplain DiagonalMatrix} if the provided matrix is a {@linkplain DiagonalMatrix};</li>
     * <li>An {@linkplain ArrayRowSymmetricMatrix} if it is any other type of
     * {@linkplain SymmetricMatrix};</li>
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
            MatrixUtils.checkSubtractionCompatible(this, m);

            // Subtraction routine
            final int n = this.getRowDimension();
            final double[][] outData = m.getData();
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (i == j) {
                        // Diagonal element, use the diagonal matrix
                        outData[i][j] = this.data[i] - outData[i][j];
                    } else {
                        // Non-diagonal element, the diagonal matrix isn't used (0)
                        outData[i][j] = -outData[i][j];
                    }
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
     * <li>A {@linkplain DiagonalMatrix} if the provided matrix is a {@linkplain DiagonalMatrix};</li>
     * <li>An {@linkplain ArrayRowSymmetricMatrix} if it is any other type of
     * {@linkplain SymmetricMatrix}.</li>
     * </ul>
     */
    @Override
    public SymmetricMatrix subtract(final SymmetricMatrix m) {
        final SymmetricMatrix out;

        if (m instanceof DiagonalMatrix) {
            // Call the optimized code for diagonal matrices
            out = this.subtract((DiagonalMatrix) m);
        } else {
            // Dimensions check
            MatrixUtils.checkSubtractionCompatible(this, m);

            // Subtraction routine
            final int n = this.getRowDimension();
            final double[][] outData = m.getData();
            for (int i = 0; i < n; i++) {
                // Diagonal element, use the diagonal matrix
                outData[i][i] = this.data[i] - outData[i][i];
                for (int j = 0; j < i; j++) {
                    // Non-diagonal element, the diagonal matrix isn't used (0)
                    outData[i][j] = -outData[i][j];
                }
            }
            // Store the array in a matrix
            out = new ArrayRowSymmetricMatrix(SymmetryType.LOWER, outData, null, null);
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
    public DiagonalMatrix subtract(final DiagonalMatrix m) {
        // Dimensions check
        MatrixUtils.checkSubtractionCompatible(this, m);

        // Subtraction routine
        final int n = this.getRowDimension();
        final double[] outData = new double[n];
        for (int i = 0; i < n; i++) {
            outData[i] = this.data[i] - m.data[i];
        }
        return new DiagonalMatrix(outData, false);
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix multiply(final RealMatrix m, final boolean toTranspose, final double d) {
        final RealMatrix out;

        if (m instanceof DiagonalMatrix) {
            // Call the optimized code for diagonal matrices
            out = multiply((DiagonalMatrix) m, d);
        } else {
            // Dimensions check
            MatrixUtils.checkMultiplicationCompatible(this, m, toTranspose);

            // Dimensions of the matrix
            final int nr = m.getRowDimension();
            final int nc = m.getColumnDimension();

            // Multiply the matrix
            final double[][] product;

            if (!toTranspose) {
                // Postmultiplication between this matrix and the provided matrix
                product = new double[nr][nc];
                for (int i = 0; i < nr; i++) {
                    for (int j = 0; j < nc; j++) {
                        product[i][j] = this.data[i] * m.getEntry(i, j) * d;
                    }
                }
            } else {
                // Postmultiplication between this matrix and the transpose of the provided matrix
                product = new double[nc][nr];
                for (int i = 0; i < nc; i++) {
                    for (int j = 0; j < nr; j++) {
                        product[i][j] = this.data[i] * m.getEntry(j, i) * d;
                    }
                }
            }

            out = new Array2DRowRealMatrix(product, false);
        }

        return out;
    }

    /** {@inheritDoc} */
    @Override
    public DiagonalMatrix multiply(final DiagonalMatrix m, final double d) {
        // Dimensions check
        MatrixUtils.checkMultiplicationCompatible(this, m);

        // Multiplication routine
        final int n = this.getRowDimension();
        final double[] out = new double[n];
        for (int i = 0; i < n; i++) {
            out[i] = this.data[i] * m.data[i] * d;
        }
        return new DiagonalMatrix(out, false);
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

        // Check the matrix transposability
        if (isTranspose) {
            // Dimensions check (the provided matrix is M^T)
            MatrixUtils.checkMultiplicationCompatible(this, m);

            // Quadratic multiplication routine
            final int n = m.getColumnDimension();
            out = new double[(n * (n + 1)) / 2];

            int index = 0;
            for (int i = 0; i < n; i++) {
                for (int j = 0; j <= i; j++) {
                    double sum = 0;
                    for (int k = 0; k < nbSum; k++) {
                        sum += m.getEntry(k, j) * m.getEntry(k, i) * this.getEntry(k, k);
                    }
                    out[index++] = sum;
                }
            }
        } else {
            // Dimensions check (the provided matrix is M)
            MatrixUtils.checkMultiplicationCompatible(m, this);

            // Quadratic multiplication routine
            final int n = m.getRowDimension();
            out = new double[(n * (n + 1)) / 2];

            int index = 0;
            for (int i = 0; i < n; i++) {
                for (int j = 0; j <= i; j++) {
                    double sum = 0;
                    for (int k = 0; k < nbSum; k++) {
                        sum += m.getEntry(j, k) * m.getEntry(i, k) * this.getEntry(k, k);
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
    public double[] operate(final double[] v) {
        // Dimension check
        final int n = this.getRowDimension();
        MatrixUtils.checkDimension(n, v.length);

        // Multiplication routine
        final double[] out = new double[n];
        for (int i = 0; i < n; i++) {
            out[i] = this.data[i] * v[i];
        }
        return out;
    }

    /** {@inheritDoc} */
    @Override
    public RealVector operate(final RealVector v) {
        // Dimension check
        final int n = this.getRowDimension();
        MatrixUtils.checkDimension(n, v.getDimension());

        // Multiplication routine
        final RealVector out = new ArrayRealVector(n);
        for (int i = 0; i < n; i++) {
            out.setEntry(i, this.data[i] * v.getEntry(i));
        }
        return out;
    }

    /** {@inheritDoc} */
    @Override
    public double[] preMultiply(final double[] v) {
        return this.operate(v);
    }

    /** {@inheritDoc} */
    @Override
    public RealVector preMultiply(final RealVector v) {
        return this.operate(v);
    }

    /** {@inheritDoc} */
    @Override
    public DiagonalMatrix power(final int p) {
        // Ensure the power exponent is positive
        if (p < 0) {
            throw new NotPositiveException(PatriusMessages.NOT_POSITIVE_EXPONENT, p);
        }

        // Power computation routine
        final double[] out;
        final int n = this.getRowDimension();

        if (p == 0) {
            // Exponent equals to 0
            out = new double[n];
            for (int i = 0; i < n; ++i) {
                out[i] = 1.0;
            }
        } else if (p == 1) {
            // Exponent equals to 1
            out = this.data.clone();
        } else {
            // Exponent greater than 1
            out = new double[n];
            for (int i = 0; i < n; i++) {
                out[i] = MathLib.pow(this.data[i], p);
            }
        }

        return new DiagonalMatrix(out, false);
    }

    /** {@inheritDoc} */
    @Override
    public DiagonalMatrix createMatrix(final int rowDimension, final int columnDimension) {
        MatrixUtils.checkDimension(rowDimension, columnDimension);
        return new DiagonalMatrix(rowDimension);
    }

    /**
     * Creates an identity matrix of the specified dimension.
     *
     * @param n
     *        the dimension of the identity matrix
     * @return the identity matrix built
     */
    public static DiagonalMatrix createIdentityMatrix(final int n) {
        final DiagonalMatrix identity = new DiagonalMatrix(n);
        for (int i = 0; i < n; i++) {
            identity.setEntry(i, i, 1.0);
        }
        return identity;
    }

    /** {@inheritDoc} */
    @Override
    public DiagonalMatrix copy() {
        return new DiagonalMatrix(this.data);
    }

    /** {@inheritDoc} */
    @Override
    public DiagonalMatrix transpose() {
        return this.transpose(true);
    }

    /** {@inheritDoc} */
    @Override
    public DiagonalMatrix transpose(final boolean forceCopy) {
        DiagonalMatrix out = this;
        if (forceCopy == true) {
            out = this.copy();
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
    public DiagonalMatrix getInverse() {
        // Ensure the matrix is not singular
        if (isSingular()) {
            throw new SingularMatrixException();
        }

        // Compute the inverse
        final int n = this.getRowDimension();
        final double[] result = new double[n];
        for (int i = 0; i < n; i++) {
            result[i] = MathLib.divide(1.0, this.data[i]);
        }
        return new DiagonalMatrix(result, false);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * <em>The provided decomposition algorithm is never used, since the computation of the inverse is straightforward 
     * for diagonal matrices.</em>
     * </p>
     */
    @Override
    public DiagonalMatrix
            getInverse(final Function<RealMatrix, Decomposition> decompositionBuilder) {
        return this.getInverse();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isDiagonal(final double threshold) {
        return true;
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
     * Checks if this is an antisymmetric matrix.
     * <p>
     * A diagonal matrix can only be antisymmetric if its diagonal elements are all equal to zero.<br>
     * The specified absolute threshold is taken into account to assess if this is the case or not.
     * </p>
     *
     * @param absoluteTolerance
     *        the absolute threshold to take into account when checking if the diagonal elements are
     *        equal to zero
     * @return {@code true} if the matrix is antisymmetric, {@code false} otherwise
     */
    public boolean isAntisymmetric(final double absoluteTolerance) {
        boolean isAntisymmetric = true;
        for (int i = 0; (i < this.data.length) && isAntisymmetric; i++) {
            isAntisymmetric = Precision.equals(this.data[i], 0., absoluteTolerance);
        }
        return isAntisymmetric;
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * A diagonal matrix can only be antisymmetric if its diagonal elements are all equal to zero.<br>
     * Only the absolute threshold is taken into account to assess if this is the case or not.
     * </p>
     */
    @Override
    public boolean isAntisymmetric(final double relativeTolerance, final double absoluteTolerance) {
        return isAntisymmetric(absoluteTolerance);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * A diagonal matrix can only be orthogonal if its diagonal elements are all equal to +1 or -1.
     * </p>
     */
    @Override
    public boolean isOrthogonal(final double thresholdNorm, final double thresholdOrthogonality) {
        boolean isOrthogonal = true;
        for (int i = 0; (i < this.data.length) && isOrthogonal; i++) {
            final double absVal = MathLib.abs(this.data[i]);
            isOrthogonal = Precision.equalsWithAbsoluteAndRelativeTolerances(absVal, 1.,
                    thresholdNorm, thresholdOrthogonality);
        }
        return isOrthogonal;
    }

    /**
     * Checks whether this diagonal matrix is singular or not.
     * <p>
     * A diagonal matrix is singular if any of its diagonal elements is equal to 0.<br>
     * The default absolute tolerance ({@linkplain Precision#SAFE_MIN}) is taken into account when
     * checking the values.
     * </p>
     *
     * @return {@code true} if the matrix is singular, {@code false} otherwise
     */
    public boolean isSingular() {
        return isSingular(Precision.SAFE_MIN);
    }

    /**
     * Checks whether this diagonal matrix is singular or not.
     * <p>
     * A diagonal matrix is singular if any of its diagonal elements is equal to 0.<br>
     * The specified absolute tolerance is taken into account when checking the values.
     * </p>
     *
     * @param absoluteTolerance
     *        the absolute tolerance to take into account when checking if an element is equal to 0
     * @return {@code true} if the matrix is singular, {@code false} otherwise
     */
    public boolean isSingular(final double absoluteTolerance) {
        boolean isSingular = false;
        for (int i = 0; (i < this.data.length) && !isSingular; i++) {
            isSingular = Precision.equals(this.data[i], 0., absoluteTolerance);
        }
        return isSingular;
    }

    /**
     * Checks if the matrix is invertible.
     * <p>
     * A diagonal matrix is invertible if its diagonal elements are all different from zero.<br>
     * The default absolute tolerance ({@linkplain Precision#SAFE_MIN}) is taken into account when
     * checking the values.
     * </p>
     *
     * @return {@code true} if the matrix is invertible, {@code false} otherwise
     */
    public boolean isInvertible() {
        return isInvertible(Precision.SAFE_MIN);
    }

    /**
     * Checks if the matrix is invertible.
     * <p>
     * A diagonal matrix is invertible if its diagonal elements are all different from zero.<br>
     * The specified absolute tolerance is taken into account when checking the values.
     * </p>
     *
     * @param absoluteTolerance
     *        the absolute tolerance to take into account when checking if an element is equal to 0
     * @return {@code true} if the matrix is invertible, {@code false} otherwise
     */
    @Override
    public boolean isInvertible(final double absoluteTolerance) {
        return !isSingular(absoluteTolerance);
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object object) {
        boolean out = false;

        if (object == this) {
            out = true;
        } else if (object != null) {
            if (object.getClass() == this.getClass()) {
                final DiagonalMatrix other = (DiagonalMatrix) object;
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
     * Ensures a given value exactly equal to zero.
     *
     * @param value
     *        the value to check
     * @throws NumberIsTooLargeException
     *         if value is not equal to zero
     */
    private static void ensureZero(final double value) {
        if (!Precision.equals(0., value, 1)) {
            throw new NumberIsTooLargeException(MathLib.abs(value), 0., true);
        }
    }
}
