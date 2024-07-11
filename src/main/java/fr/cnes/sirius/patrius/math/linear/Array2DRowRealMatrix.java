/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 *
 * Copyright 2011-2022 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
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
* VERSION:4.8:FA:FA-2940:15/11/2021:[PATRIUS] Anomalies suite a DM 2766 sur package fr.cnes.sirius.patrius.math.linear 
* VERSION:4.8:FA:FA-2956:15/11/2021:[PATRIUS] Temps de propagation non implemente pour certains evenements 
 * VERSION:4.7:DM:DM-2872:18/05/2021:Calcul de l'accélération dans la classe QuaternionPolynomialProfile 
 * VERSION:4.7:DM:DM-2766:18/05/2021:Evol. et corr. dans le package fr.cnes.sirius.patrius.math.linear (suite DM 2300) 
 * VERSION:4.5:DM:DM-2300:27/05/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.linear;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.MathIllegalStateException;
import fr.cnes.sirius.patrius.math.exception.NoDataException;
import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Implementation of {@link RealMatrix} using a {@code double[][]} array to
 * store entries.
 * 
 * @version $Id: Array2DRowRealMatrix.java 18108 2017-10-04 06:45:27Z bignon $
 */
@SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
public class Array2DRowRealMatrix extends AbstractRealMatrix {

    /** Serializable UID. */
    private static final long serialVersionUID = -1067294169172445528L;

    /** Entries of the matrix. */
    private double[][] data;

    /**
     * Creates a matrix with no data
     */
    public Array2DRowRealMatrix() {
        super();
        // Nothing to do
    }

    /**
     * Create a new RealMatrix with the supplied row and column dimensions.
     * 
     * @param rowDimension
     *        Number of rows in the new matrix
     * @param columnDimension
     *        Number of columns in the new matrix
     * @throws NotStrictlyPositiveException
     *         if the row or column dimension is not positive
     */
    public Array2DRowRealMatrix(final int rowDimension, final int columnDimension) {
        super(rowDimension, columnDimension);
        this.data = new double[rowDimension][columnDimension];
    }

    /**
     * Create a new {@code RealMatrix} using the input array as the underlying data array.
     * <p>
     * The input array is copied, not referenced. This constructor has the same effect as calling
     * {@link #Array2DRowRealMatrix(double[][], boolean)} with the second argument set to
     * {@code true}.
     * </p>
     * 
     * @param d
     *        Data for the new matrix
     * @throws DimensionMismatchException
     *         if {@code d} is not rectangular
     * @throws NoDataException
     *         if {@code d} row or column dimension is zero
     * @throws NullArgumentException
     *         if {@code d} is {@code null}
     * @see #Array2DRowRealMatrix(double[][], boolean)
     */
    public Array2DRowRealMatrix(final double[][] d) {
        super();
        this.copyIn(d);
    }

    /**
     * Create a new RealMatrix using the input array as the underlying data array.
     * If an array is built specially in order to be embedded in a RealMatrix and not used directly,
     * the {@code copyArray} may be set to {@code false}. This will prevent the copying and improve
     * performance as no new array will be built and no data will be copied.
     * 
     * @param d
     *        Data for new matrix
     * @param copyArray
     *        if {@code true}, the input array will be copied, otherwise it will be referenced.
     * @throws DimensionMismatchException
     *         if {@code d} is not rectangular
     * @throws NoDataException
     *         if {@code d} row or column dimension is zero
     * @throws NullArgumentException
     *         if {@code d} is {@code null}
     * @see #Array2DRowRealMatrix(double[][])
     */
    public Array2DRowRealMatrix(final double[][] d, final boolean copyArray) {
        super();
        if (copyArray) {
            this.copyIn(d);
        } else {
            MatrixUtils.checkMatrixArray(d);
            this.data = d;
        }
    }

    /**
     * Create a new (column) RealMatrix using {@code v} as the data for the unique column of the
     * created matrix.
     * The input array is copied.
     * 
     * @param v
     *        Column vector holding data for new matrix
     */
    public Array2DRowRealMatrix(final double[] v) {
        super();
        final int nRows = v.length;
        this.data = new double[nRows][1];
        for (int row = 0; row < nRows; row++) {
            this.data[row][0] = v[row];
        }
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix createMatrix(final int rowDimension, final int columnDimension) {
        return new Array2DRowRealMatrix(rowDimension, columnDimension);
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix copy() {
        return new Array2DRowRealMatrix(this.copyOut(), false);
    }

    /**
     * Returns the result of adding a matrix M to this matrix.
     *
     * @param m
     *        the matrix M to be added
     * @return the matrix resulting from the sum {@code this} + M
     * @throws MatrixDimensionMismatchException
     *         if the matrix M is not the same size as this matrix
     */
    public Array2DRowRealMatrix add(final Array2DRowRealMatrix m) {
        // Dimensions check
        MatrixUtils.checkAdditionCompatible(this, m);

        // Dimensions of the matrix
        final int rowCount = this.getRowDimension();
        final int columnCount = this.getColumnDimension();

        // Addition routine
        final double[][] outData = new double[rowCount][columnCount];
        for (int row = 0; row < rowCount; row++) {
            final double[] dataRow = this.data[row];
            final double[] mRow = m.data[row];
            final double[] outDataRow = outData[row];
            for (int col = 0; col < columnCount; col++) {
                outDataRow[col] = dataRow[col] + mRow[col];
            }
        }

        return new Array2DRowRealMatrix(outData, false);
    }

    /**
     * Returns the result of subtracting a matrix M from this matrix.
     *
     * @param m
     *        matrix to be subtracted
     * @return the matrix resulting from the subtraction {@code this} - M
     * @throws MatrixDimensionMismatchException
     *         if the matrix M is not the same size as this matrix
     */
    public Array2DRowRealMatrix subtract(final Array2DRowRealMatrix m) {
        // Dimensions check
        MatrixUtils.checkSubtractionCompatible(this, m);

        // Dimensions of the matrix
        final int rowCount = this.getRowDimension();
        final int columnCount = this.getColumnDimension();

        // Subtraction routine
        final double[][] outData = new double[rowCount][columnCount];
        for (int row = 0; row < rowCount; row++) {
            final double[] dataRow = this.data[row];
            final double[] mRow = m.data[row];
            final double[] outDataRow = outData[row];
            for (int col = 0; col < columnCount; col++) {
                outDataRow[col] = dataRow[col] - mRow[col];
            }
        }

        return new Array2DRowRealMatrix(outData, false);
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix multiply(final RealMatrix m, final boolean toTranspose, final double d) {
        final Array2DRowRealMatrix castMatrix;
        if (m instanceof Array2DRowRealMatrix) {
            castMatrix = (Array2DRowRealMatrix) m;
        } else {
            castMatrix = new Array2DRowRealMatrix(m.getData(), false);
        }

        return this.multiply(castMatrix, toTranspose, d);
    }

    /**
     * Returns the result of postmultiplying this matrix by a matrix M.
     *
     * @param m
     *        the matrix M by which to multiply this matrix by
     * @return the matrix resulting from the product {@code this} &times; M
     * @throws DimensionMismatchException
     *         if the matrices are not multiplication compatible
     */
    public Array2DRowRealMatrix multiply(final Array2DRowRealMatrix m) {
        return multiply(m, false, 1.0);
    }

    /**
     * Returns the result of postmultiplying this matrix by a matrix M or by its transpose
     * M<sup>T</sup>.
     *
     * @param m
     *        the matrix M by which to multiply this matrix by
     * @param toTranspose
     *        whether to compute the product {@code this} &times; M ({@code toTranspose=false}), or
     *        the product {@code this} &times; M<sup>T</sup> ({@code toTranspose=true})
     *
     * @return the matrix resulting from the product {@code this} &times; M or {@code this} &times;
     *         M<sup>T</sup>
     *
     * @throws DimensionMismatchException
     *         if the matrices are not multiplication compatible
     */
    public Array2DRowRealMatrix multiply(final Array2DRowRealMatrix m, final boolean toTranspose) {
        return multiply(m, toTranspose, 1.0);
    }

    /**
     * Returns the result of postmultiplying this matrix by a matrix M or by its transpose
     * M<sup>T</sup>, then by a scalar {@code d}.
     *
     * @param m
     *        the matrix M by which to multiply this matrix by
     * @param toTranspose
     *        whether to compute the product {@code this} &times; M &times;{@code d} (
     *        {@code toTranspose=false}), or the product {@code this} &times; M<sup>T</sup> &times;
     *        {@code d} ({@code toTranspose=true})
     * @param d
     *        the scalar by which to multiply the resulting matrix by
     * @return the matrix resulting from the product {@code this} &times; M &times; {@code d} or
     *         {@code this} &times; M<sup>T</sup> &times; {@code d}
     * @throws DimensionMismatchException
     *         if the matrices are not multiplication compatible
     */
    public Array2DRowRealMatrix multiply(final Array2DRowRealMatrix m, final boolean toTranspose,
            final double d) {
        // Dimensions check
        MatrixUtils.checkMultiplicationCompatible(this, m, toTranspose);

        // Number of elements to sum when computing the entries of the returned matrix
        final int nbSum = this.getColumnDimension();

        // Multiply this matrix by M or by M^T
        final double[][] out;

        if (!toTranspose) {
            // Dimensions of the returned matrix
            final int nbRows = this.getRowDimension();
            final int nbColumns = m.getColumnDimension();

            // Allocate the array which will store a single column of M
            final double[] columnData = new double[nbSum];

            // Multiplication routine (this x M)
            out = new double[nbRows][nbColumns];
            for (int col = 0; col < nbColumns; col++) {
                // Retrieve the column of M (so it will be in contiguous memory)
                for (int row = 0; row < nbSum; row++) {
                    columnData[row] = m.data[row][col];
                }

                // Compute the product between the current column of M and the rows of this matrix
                for (int row = 0; row < nbRows; row++) {
                    final double[] rowData = this.data[row];
                    double sum = 0;
                    for (int i = 0; i < nbSum; i++) {
                        sum += rowData[i] * columnData[i];
                    }
                    out[row][col] = sum * d;
                }
            }
        } else {
            // Dimensions of the returned matrix
            final int nbRows = this.getRowDimension();
            final int nbColumns = m.getRowDimension();

            // Multiplication routine (this x M^T)
            out = new double[nbRows][nbColumns];
            for (int col = 0; col < nbColumns; col++) {
                // Retrieve the column of M^T (which is a row of M)
                final double[] columnData = m.data[col];

                // Compute the product between the current column of M^T and the rows of this matrix
                for (int row = 0; row < nbRows; row++) {
                    final double[] rowData = this.data[row];
                    double sum = 0;
                    for (int i = 0; i < nbSum; i++) {
                        sum += rowData[i] * columnData[i];
                    }
                    out[row][col] = sum * d;
                }
            }
        }

        // Return the computed matrix
        return new Array2DRowRealMatrix(out, false);
    }

    /** {@inheritDoc} */
    @Override
    public double[][] getData() {
        return getData(true);
    }

    /** {@inheritDoc} */
    @Override
    // Reason: internal array access provided for optimization purposes
            @SuppressWarnings("PMD.MethodReturnsInternalArray")
            public
            double[][] getData(final boolean forceCopy) {
        final double[][] out;
        if (forceCopy == true) {
            out = this.copyOut();
        } else {
            out = this.data;
        }
        return out;
    }

    /**
     * Gets a direct reference to the underlying data array storing the entries of the matrix.
     * 
     * @return the underlying data array storing the entries of the matrix
     */
    // Reason: internal array access provided for optimization purposes
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    public double[][] getDataRef() {
        return this.data;
    }

    /** {@inheritDoc} */
    @Override
    public void setSubMatrix(final double[][] subMatrix, final int row, final int column) {
        // Check the submatrix data array
        MatrixUtils.checkMatrixArray(subMatrix);

        // Dimensions of the submatrix
        final int nRows = subMatrix.length;
        final int nCols = subMatrix[0].length;

        if (this.data == null) {
            if (row > 0) {
                // Exception
                throw new MathIllegalStateException(PatriusMessages.FIRST_ROWS_NOT_INITIALIZED_YET,
                        row);
            }
            if (column > 0) {
                // Exception
                throw new MathIllegalStateException(
                        PatriusMessages.FIRST_COLUMNS_NOT_INITIALIZED_YET, column);
            }

            // initialize data
            this.data = new double[subMatrix.length][nCols];
            for (int i = 0; i < this.data.length; ++i) {
                System.arraycopy(subMatrix[i], 0, this.data[i + row], column, nCols);
            }
        } else {
            MatrixUtils.checkRowIndex(this, row);
            MatrixUtils.checkColumnIndex(this, column);
            MatrixUtils.checkRowIndex(this, nRows + row - 1);
            MatrixUtils.checkColumnIndex(this, nCols + column - 1);

            for (int i = 0; i < nRows; i++) {
                System.arraycopy(subMatrix[i], 0, this.data[i + row], column, nCols);
            }
        }

    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix getSubMatrix(final int startRow, final int endRow, final int startColumn,
            final int endColumn) {
        MatrixUtils.checkSubMatrixIndex(this, startRow, endRow, startColumn, endColumn);

        final int nRows = endRow - startRow + 1;
        final int nCols = endColumn - startColumn + 1;
        final double[][] subMatrix = new double[nRows][nCols];
        for (int i = 0; i < nRows; ++i) {
            System.arraycopy(this.data[startRow + i], startColumn, subMatrix[i], 0, nCols);
        }

        return MatrixUtils.createRealMatrix(subMatrix, false);
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix getSubMatrix(final int[] selectedRows, final int[] selectedColumns) {

        MatrixUtils.checkSubMatrixIndex(this, selectedRows, selectedColumns);
        final int nRows = selectedRows.length;
        final int nCols = selectedColumns.length;
        // Initialize the submatrix with the dimensions
        // of the rows and column indices provided
        final double[][] subMatrix = new double[nRows][nCols];
        for (int i = 0; i < nRows; ++i) {
            final double[] subMatrixi = subMatrix[i];
            final double[] dataRow = this.data[selectedRows[i]];
            for (int n = 0; n < nCols; ++n) {
                subMatrixi[n] = dataRow[selectedColumns[n]];
            }
        }
        // return a matrix with the data copied
        return MatrixUtils.createRealMatrix(subMatrix, false);
    }

    /** {@inheritDoc} */
    @Override
    public void copySubMatrix(final int startRow, final int endRow, final int startColumn,
            final int endColumn, final double[][] destination) {
        MatrixUtils.checkSubMatrixIndex(this, startRow, endRow, startColumn, endColumn);
        final int rowsCount = endRow + 1 - startRow;
        final int columnsCount = endColumn + 1 - startColumn;
        if ((destination.length < rowsCount) || (destination[0].length < columnsCount)) {
            throw new MatrixDimensionMismatchException(destination.length, destination[0].length,
                    rowsCount, columnsCount);
        }

        for (int i = 0; i < rowsCount; ++i) {
            System.arraycopy(this.data[startRow + i], startColumn, destination[i], 0, columnsCount);
        }

    }

    /** {@inheritDoc} */
    @Override
    public void copySubMatrix(final int[] selectedRows, final int[] selectedColumns,
            final double[][] destination) {
        // Check index
        MatrixUtils.checkSubMatrixIndex(this, selectedRows, selectedColumns);
        // Check dimensions
        if ((destination.length < selectedRows.length)
                || (destination[0].length < selectedColumns.length)) {
            throw new MatrixDimensionMismatchException(destination.length, destination[0].length,
                    selectedRows.length, selectedColumns.length);
        }

        // Operation
        for (int i = 0; i < selectedRows.length; ++i) {
            final int row = selectedRows[i];
            final double[] destinationi = destination[i];
            for (int n = 0; n < selectedColumns.length; ++n) {
                destinationi[n] = this.data[row][selectedColumns[n]];
            }
        }
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

    /** {@inheritDoc} */
    @Override
    public void addToEntry(final int row, final int column, final double increment) {
        MatrixUtils.checkMatrixIndex(this, row, column);
        this.data[row][column] += increment;
    }

    /** {@inheritDoc} */
    @Override
    public void multiplyEntry(final int row, final int column, final double factor) {
        MatrixUtils.checkMatrixIndex(this, row, column);
        this.data[row][column] *= factor;
    }

    /** {@inheritDoc} */
    @Override
    public int getRowDimension() {
        return (this.data == null) ? 0 : this.data.length;
    }

    /** {@inheritDoc} */
    @Override
    public int getColumnDimension() {
        return ((this.data == null) || (this.data[0] == null)) ? 0 : this.data[0].length;
    }

    /** {@inheritDoc} */
    @Override
    public double[] operate(final double[] v) {
        // Initialization
        // column count of the matrix
        final int nCols = this.getColumnDimension();
        if (v.length != nCols) {
            // Exception
            throw new DimensionMismatchException(v.length, nCols);
        }
        // row count of the matrix
        final int nRows = this.getRowDimension();

        final double[] out = new double[nRows];
        for (int row = 0; row < nRows; row++) {
            final double[] dataRow = this.data[row];
            double sum = 0;
            for (int i = 0; i < nCols; i++) {
                sum += dataRow[i] * v[i];
            }
            out[row] = sum;
        }
        // Return result
        return out;
    }

    /** {@inheritDoc} */
    @Override
    public double[] preMultiply(final double[] v) {
        // row count of the matrix
        final int nRows = this.getRowDimension();
        if (v.length != nRows) {
            // raise an exception if the v length is different of the row line numbers of the matrix
            throw new DimensionMismatchException(v.length, nRows);
        }

        // column count of the matrix
        final int nCols = this.getColumnDimension();

        // initialize out
        final double[] out = new double[nCols];
        for (int col = 0; col < nCols; ++col) {
            double sum = 0;
            for (int i = 0; i < nRows; ++i) {
                sum += this.data[i][col] * v[i];
            }
            out[col] = sum;
        }

        return out;

    }

    /** {@inheritDoc} */
    @Override
    public double walkInRowOrder(final RealMatrixChangingVisitor visitor) {
        // row count of the matrix
        final int rows = this.getRowDimension();
        // column count of the matrix
        final int columns = this.getColumnDimension();
        visitor.start(rows, columns, 0, rows - 1, 0, columns - 1);
        // loop on the lines number of the matrix
        for (int i = 0; i < rows; ++i) {
            final double[] rowI = this.data[i];
            for (int j = 0; j < columns; ++j) {
                rowI[j] = visitor.visit(i, j, rowI[j]);
            }
        }
        return visitor.end();
    }

    /** {@inheritDoc} */
    @Override
    public double walkInRowOrder(final RealMatrixPreservingVisitor visitor) {
        // row count of the matrix
        final int rows = this.getRowDimension();
        // column count of the matrix
        final int columns = this.getColumnDimension();
        visitor.start(rows, columns, 0, rows - 1, 0, columns - 1);
        // loop on the lines number of the matrix
        for (int i = 0; i < rows; ++i) {
            final double[] rowI = this.data[i];
            for (int j = 0; j < columns; ++j) {
                visitor.visit(i, j, rowI[j]);
            }
        }
        return visitor.end();
    }

    /** {@inheritDoc} */
    @Override
    public double walkInRowOrder(final RealMatrixChangingVisitor visitor, final int startRow,
            final int endRow, final int startColumn, final int endColumn) {
        // safety check
        MatrixUtils.checkSubMatrixIndex(this, startRow, endRow, startColumn, endColumn);
        // visitor call at the beginning
        visitor.start(this.getRowDimension(), this.getColumnDimension(), startRow, endRow,
                startColumn, endColumn);
        for (int i = startRow; i <= endRow; ++i) {
            final double[] rowI = this.data[i];
            for (int j = startColumn; j <= endColumn; ++j) {
                // Visit matrix entries.
                rowI[j] = visitor.visit(i, j, rowI[j]);
            }
        }
        return visitor.end();
    }

    /** {@inheritDoc} */
    @Override
    public double walkInRowOrder(final RealMatrixPreservingVisitor visitor, final int startRow,
            final int endRow, final int startColumn, final int endColumn) {
        // safety check
        MatrixUtils.checkSubMatrixIndex(this, startRow, endRow, startColumn, endColumn);
        // visitor call once
        visitor.start(this.getRowDimension(), this.getColumnDimension(), startRow, endRow,
                startColumn, endColumn);
        for (int i = startRow; i <= endRow; ++i) {
            final double[] rowI = this.data[i];
            for (int j = startColumn; j <= endColumn; ++j) {
                // Visit matrix entries.
                visitor.visit(i, j, rowI[j]);
            }
        }
        return visitor.end();
    }

    /** {@inheritDoc} */
    @Override
    public double walkInColumnOrder(final RealMatrixChangingVisitor visitor) {
        // row count of the matrix
        final int rows = this.getRowDimension();
        // column count of the matrix
        final int columns = this.getColumnDimension();
        // visitor call once
        visitor.start(rows, columns, 0, rows - 1, 0, columns - 1);
        for (int j = 0; j < columns; ++j) {
            for (int i = 0; i < rows; ++i) {
                final double[] rowI = this.data[i];
                // Visit matrix entries.
                rowI[j] = visitor.visit(i, j, rowI[j]);
            }
        }
        return visitor.end();
    }

    /** {@inheritDoc} */
    @Override
    public double walkInColumnOrder(final RealMatrixPreservingVisitor visitor) {
        // row count of the matrix
        final int rows = this.getRowDimension();
        // column count of the matrix
        final int columns = this.getColumnDimension();
        // visitor call once
        visitor.start(rows, columns, 0, rows - 1, 0, columns - 1);
        for (int j = 0; j < columns; ++j) {
            for (int i = 0; i < rows; ++i) {
                // Visit matrix entries.
                visitor.visit(i, j, this.data[i][j]);
            }
        }
        return visitor.end();
    }

    /** {@inheritDoc} */
    @Override
    public double walkInColumnOrder(final RealMatrixChangingVisitor visitor, final int startRow,
            final int endRow, final int startColumn, final int endColumn) {
        // safety check
        MatrixUtils.checkSubMatrixIndex(this, startRow, endRow, startColumn, endColumn);
        // visitor call once
        visitor.start(this.getRowDimension(), this.getColumnDimension(), startRow, endRow,
                startColumn, endColumn);
        for (int j = startColumn; j <= endColumn; ++j) {
            for (int i = startRow; i <= endRow; ++i) {
                final double[] rowI = this.data[i];
                // Visit matrix entries.
                rowI[j] = visitor.visit(i, j, rowI[j]);
            }
        }
        return visitor.end();
    }

    /** {@inheritDoc} */
    @Override
    public double walkInColumnOrder(final RealMatrixPreservingVisitor visitor, final int startRow,
            final int endRow, final int startColumn, final int endColumn) {
        // safety check
        MatrixUtils.checkSubMatrixIndex(this, startRow, endRow, startColumn, endColumn);
        // visitor call once
        visitor.start(this.getRowDimension(), this.getColumnDimension(), startRow, endRow,
                startColumn, endColumn);
        for (int j = startColumn; j <= endColumn; ++j) {
            for (int i = startRow; i <= endRow; ++i) {
                // Visit matrix entries.
                visitor.visit(i, j, this.data[i][j]);
            }
        }
        return visitor.end();
    }

    /** {@inheritDoc} */
    @Override
    public final RealMatrix transpose() {
        // Necessary to mark method as "final"
        return super.transpose();
    }

    /**
     * Get a fresh copy of the underlying data array.
     * 
     * @return a copy of the underlying data array.
     */
    private double[][] copyOut() {
        final int nRows = this.getRowDimension();
        final double[][] out = new double[nRows][this.getColumnDimension()];
        // can't copy 2-d array in one shot, otherwise get row references
        for (int i = 0; i < nRows; i++) {
            System.arraycopy(this.data[i], 0, out[i], 0, this.data[i].length);
        }
        return out;
    }

    /**
     * Replace data with a fresh copy of the input array.
     * 
     * @param matrix
     *        Data to copy.
     * @throws NoDataException
     *         if the input array is empty.
     * @throws DimensionMismatchException
     *         if the input array is not rectangular.
     * @throws NullArgumentException
     *         if the input array is {@code null}.
     */
    private void copyIn(final double[][] matrix) {
        this.setSubMatrix(matrix, 0, 0);
    }
}
