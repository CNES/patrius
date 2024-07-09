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
/* 
 * HISTORY
* VERSION:4.8:FA:FA-2940:15/11/2021:[PATRIUS] Anomalies suite a DM 2766 sur package fr.cnes.sirius.patrius.math.linear 
* VERSION:4.8:DM:DM-3040:15/11/2021:[PATRIUS]Reversement des evolutions de la branche patrius-for-lotus 
 * VERSION:4.7:FA:FA-2762:18/05/2021:Probleme lors des controles qualite via la PIC 
 * VERSION:4.7:DM:DM-2766:18/05/2021:Evol. et corr. dans le package fr.cnes.sirius.patrius.math.linear (suite DM 2300) 
 * VERSION:4.6:FA:FA-2542:27/01/2021:[PATRIUS] Definition d'un champ de vue avec demi-angle de 180° 
 * VERSION:4.6:FA:FA-2686:27/01/2021:Perte de performance de la m&amp;#233;thode AbstractRealMatrix.setSubMatrix
 * VERSION:4.5.1:FA:FA-2540:04/08/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear
 * VERSION:4.5:DM:DM-2300:27/05/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.linear;

import java.util.ArrayList;
import java.util.function.Function;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.NoDataException;
import fr.cnes.sirius.patrius.math.exception.NotPositiveException;
import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Basic implementation of {@linkplain RealMatrix} methods regardless of the underlying storage.
 * <p>
 * All the methods implemented here use {@link #getEntry(int, int)} to access matrix elements.
 * Derived class can provide faster implementations.
 * </p>
 * <p>
 * This class is up-to-date with commons-math 3.6.1.
 * </p>
 *
 * @version $Id: AbstractRealMatrix.java 18108 2017-10-04 06:45:27Z bignon $
 *
 * @since 2.0
 */
public abstract class AbstractRealMatrix extends RealLinearOperator implements RealMatrix {

    /** Serial UID. */
    private static final long serialVersionUID = 2636043020233182073L;

    /** Default singularity threshold for the LU decomposition. */
    private static final double DEFAULT_SINGULARITY_THRESHOLD = 1E-11;

    /** Default decomposed matrix */
    private Function<RealMatrix, Decomposition> defaultDecomposition;

    /**
     * Creates a matrix with no data.
     */
    protected AbstractRealMatrix() {
        super();
    }

    /**
     * Creates a new matrix with the supplied row and column dimensions.
     *
     * @param rowDimension
     *        the number of rows in the matrix
     * @param columnDimension
     *        the number of columns in the matrix
     * @throws NotStrictlyPositiveException
     *         if the row/column dimensions are not strictly positive
     */
    protected AbstractRealMatrix(final int rowDimension, final int columnDimension) {
        super();
        MatrixUtils.checkRowDimension(rowDimension);
        MatrixUtils.checkColumnDimension(columnDimension);
    }

    /**
     * Creates a new matrix using the supplied data array.
     *
     * @param data
     *        the data array containing the entries of the matrix
     * @throws NullArgumentException
     *         if the data array is {@code null}
     * @throws NoDataException
     *         if the data array is empty
     * @throws DimensionMismatchException
     *         if the rows of the data array do not have a constant number of columns
     */
    protected AbstractRealMatrix(final double[][] data) {
        super();
        MatrixUtils.checkMatrixArray(data);
    }

    /**
     * Checks if this is a square matrix and throws an exception if that's not the case.
     *
     * @throws NonSquareMatrixException
     *         if this is not a square matrix
     */
    protected void checkSquare() {
        if (!this.isSquare()) {
            throw new NonSquareMatrixException(this.getRowDimension(), this.getColumnDimension());
        }
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix add(final RealMatrix m) {
        // Dimensions check
        MatrixUtils.checkAdditionCompatible(this, m);

        // Dimensions of the returned matrix
        final int nbRow = this.getRowDimension();
        final int nbColumn = this.getColumnDimension();

        // Addition
        final RealMatrix out = this.createMatrix(nbRow, nbColumn);
        for (int row = 0; row < nbRow; ++row) {
            for (int col = 0; col < nbColumn; ++col) {
                out.setEntry(row, col, this.getEntry(row, col) + m.getEntry(row, col));
            }
        }
        return out;
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix subtract(final RealMatrix m) {
        // Dimensions check
        MatrixUtils.checkSubtractionCompatible(this, m);

        // Dimensions of the returned matrix
        final int nbRow = this.getRowDimension();
        final int nbColumn = this.getColumnDimension();

        // Subtraction
        final RealMatrix out = this.createMatrix(nbRow, nbColumn);
        for (int row = 0; row < nbRow; ++row) {
            for (int col = 0; col < nbColumn; ++col) {
                out.setEntry(row, col, this.getEntry(row, col) - m.getEntry(row, col));
            }
        }
        return out;
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix scalarAdd(final double d) {
        // Dimensions of the returned matrix
        final int nbRow = this.getRowDimension();
        final int nbColumn = this.getColumnDimension();

        // Create a new matrix
        final RealMatrix out = this.createMatrix(nbRow, nbColumn);
        // Fill it with scalar addition
        for (int row = 0; row < nbRow; ++row) {
            for (int col = 0; col < nbColumn; ++col) {
                out.setEntry(row, col, this.getEntry(row, col) + d);
            }
        }
        return out;
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix scalarMultiply(final double d) {
        // Matrix dimensions
        final int nbRow = this.getRowDimension();
        final int nbColumn = this.getColumnDimension();

        // Create a new matrix
        final RealMatrix out = this.createMatrix(nbRow, nbColumn);
        // Fill it with scalar multiplication
        for (int row = 0; row < nbRow; ++row) {
            for (int col = 0; col < nbColumn; ++col) {
                out.setEntry(row, col, this.getEntry(row, col) * d);
            }
        }
        return out;
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix multiply(final RealMatrix m) {
        return this.multiply(m, false, 1.0);
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix multiply(final RealMatrix m, final boolean toTranspose) {
        return this.multiply(m, toTranspose, 1.0);
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix multiply(final RealMatrix m, final boolean toTranspose, final double d) {
        final RealMatrix out;

        if (m instanceof DiagonalMatrix) {
            // Optimization for diagonal matrix
            out = this.multiply((DiagonalMatrix) m, d);
        } else {
            // Dimensions check
            MatrixUtils.checkMultiplicationCompatible(this, m, toTranspose);

            // Number of elements to sum
            final int nbSum = this.getColumnDimension();

            // Multiply this matrix by M or by M^T
            if (!toTranspose) {
                // Dimensions of the returned matrix
                final int nbRows = this.getRowDimension();
                final int nbColumns = m.getColumnDimension();

                // Multiplication (this x M)
                out = this.createMatrix(nbRows, nbColumns);
                for (int row = 0; row < nbRows; ++row) {
                    for (int col = 0; col < nbColumns; ++col) {
                        double sum = 0;
                        for (int i = 0; i < nbSum; ++i) {
                            sum += this.getEntry(row, i) * m.getEntry(i, col);
                        }
                        out.setEntry(row, col, sum * d);
                    }
                }
            } else {
                // Dimensions of the returned matrix
                final int nbRows = this.getRowDimension();
                final int nbColumns = m.getRowDimension();

                // Multiplication (this x M^T)
                out = this.createMatrix(nbRows, nbColumns);
                for (int row = 0; row < nbRows; ++row) {
                    for (int col = 0; col < nbColumns; ++col) {
                        double sum = 0;
                        for (int i = 0; i < nbSum; ++i) {
                            sum += this.getEntry(row, i) * m.getEntry(col, i);
                        }
                        out.setEntry(row, col, sum * d);
                    }
                }
            }
        }

        return out;
    }

    /**
     * Returns the result of postmultiplying this matrix by the diagonal matrix {@code m}, then by
     * the scalar {@code d}.
     *
     * @param m
     *        the diagonal matrix by which to multiply this matrix by
     * @param d
     *        the scalar by which to multiply the resulting matrix by
     * @return the matrix resulting from the product {@code this}&times;{@code m}&times;{@code d} or
     *         {@code this}&times;{@code m}<sup>T</sup>&times;{@code d}
     * @throws DimensionMismatchException
     *         if the matrices are not multiplication compatible
     */
    public RealMatrix multiply(final DiagonalMatrix m, final double d) {
        // Dimensions check
        MatrixUtils.checkMultiplicationCompatible(this, m);

        // Dimension of the returned matrix
        final int nbRows = this.getRowDimension();
        final int nbColumns = m.getColumnDimension();

        // Multiplication
        final double[] dataRef = m.getDataRef();
        final RealMatrix out = this.createMatrix(nbRows, nbColumns);
        for (int row = 0; row < nbRows; row++) {
            for (int col = 0; col < nbColumns; col++) {
                out.setEntry(row, col, this.getEntry(row, col) * dataRef[col] * d);
            }
        }
        return out;
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix preMultiply(final RealMatrix m) {
        return m.multiply(this);
    }

    /** {@inheritDoc} */
    @Override
    public double[] operate(final double[] v) {
        // Dimensions of the matrix
        final int nbCols = this.getColumnDimension();

        // Ensure the dimension of the provided vector
        // matches the column dimension of the matrix
        if (v.length != nbCols) {
            throw new DimensionMismatchException(v.length, nbCols);
        }
        final int nbRows = this.getRowDimension();

        // Multiplication
        final double[] out = new double[nbRows];
        for (int i = 0; i < nbRows; ++i) {
            double sum = 0;
            for (int j = 0; j < nbCols; ++j) {
                sum += this.getEntry(i, j) * v[j];
            }
            out[i] = sum;
        }
        return out;
    }

    /** {@inheritDoc} */
    @Override
    public RealVector operate(final RealVector v) {
        final RealVector out;

        if (v instanceof ArrayRealVector) {
            // Optimized code for ArrayRealVector
            final double[] data = ((ArrayRealVector) v).getDataRef();
            out = new ArrayRealVector(this.operate(data), false);
        } else {
            // Dimensions of the matrix
            final int nbCols = this.getColumnDimension();

            // Ensure the dimension of the provided vector matches the column dimension of the
            // matrix
            if (v.getDimension() != nbCols) {
                throw new DimensionMismatchException(v.getDimension(), nbCols);
            }
            final int nbRows = this.getRowDimension();

            // Multiplication
            final double[] data = new double[nbRows];
            for (int i = 0; i < nbRows; ++i) {
                double sum = 0;
                for (int j = 0; j < nbCols; ++j) {
                    sum += this.getEntry(i, j) * v.getEntry(j);
                }
                data[i] = sum;
            }
            out = new ArrayRealVector(data, false);
        }

        return out;
    }

    /** {@inheritDoc} */
    @Override
    public double[] preMultiply(final double[] v) {
        // Dimensions of the matrix
        final int nbRows = this.getRowDimension();

        // Ensure the dimension of the provided vector matches the row dimension of the matrix
        if (v.length != nbRows) {
            throw new DimensionMismatchException(v.length, nbRows);
        }
        final int nbCols = this.getColumnDimension();

        // Multiplication
        final double[] out = new double[nbCols];
        for (int j = 0; j < nbCols; ++j) {
            double sum = 0;
            for (int i = 0; i < nbRows; ++i) {
                sum += this.getEntry(i, j) * v[i];
            }
            out[j] = sum;
        }
        return out;
    }

    /** {@inheritDoc} */
    @Override
    public RealVector preMultiply(final RealVector v) {
        final RealVector out;

        if (v instanceof ArrayRealVector) {
            // Optimized code for ArrayRealVector
            final double[] data = ((ArrayRealVector) v).getDataRef();
            out = new ArrayRealVector(this.preMultiply(data), false);
        } else {
            // Dimensions of the matrix
            final int nbRows = this.getRowDimension();

            // Ensure the dimension of the provided vector matches the row dimension of the matrix
            if (v.getDimension() != nbRows) {
                throw new DimensionMismatchException(v.getDimension(), nbRows);
            }
            final int nbCols = this.getColumnDimension();

            // Multiplication
            final double[] data = new double[nbCols];
            for (int j = 0; j < nbCols; ++j) {
                double sum = 0;
                for (int i = 0; i < nbRows; ++i) {
                    sum += this.getEntry(i, j) * v.getEntry(i);
                }
                data[j] = sum;
            }
            // Store the new array without copy it
            out = new ArrayRealVector(data, false);
        }

        return out;
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix power(final int p) {
        // Ensure the exponent is positive
        if (p < 0) {
            throw new NotPositiveException(PatriusMessages.NOT_POSITIVE_EXPONENT, p);
        }

        // Ensure this is a square matrix
        this.checkSquare();

        // Raise this matrix to the power of p
        RealMatrix result;

        if (p == 0) {
            // Optimization for power 0
            final int dim = this.getRowDimension();
            result = this.createMatrix(dim, dim);
            for (int i = 0; i < dim; i++) {
                result.setEntry(i, i, 1.0);
            }
        } else if (p == 1) {
            // Optimization for power 1
            result = this.copy();
        } else {
            // Only log_2(p) operations is used by doing as follows:
            // 5^214 = 5^128 * 5^64 * 5^16 * 5^4 * 5^2
            // In general, the same approach is used for A^p.
            final int power = p - 1;
            final char[] binaryRepresentation = Integer.toBinaryString(power).toCharArray();
            final ArrayList<Integer> nonZeroPositions = new ArrayList<Integer>();

            int maxI = -1;
            for (int i = 0; i < binaryRepresentation.length; ++i) {
                if (binaryRepresentation[i] == '1') {
                    final int pos = binaryRepresentation.length - i - 1;
                    nonZeroPositions.add(pos);

                    // The positions are taken in turn, so maxI is only changed once
                    if (maxI == -1) {
                        maxI = pos;
                    }
                }
            }

            final RealMatrix[] results = new RealMatrix[maxI + 1];
            results[0] = this.copy();
            for (int i = 1; i <= maxI; ++i) {
                results[i] = results[i - 1].multiply(results[i - 1]);
            }

            result = this.copy();
            for (final Integer i : nonZeroPositions) {
                result = result.multiply(results[i]);
            }
        }

        return result;
    }

    /** {@inheritDoc} */
    @Override
    public double[][] getData() {
        // Dimension of the matrix
        final int nbRows = this.getRowDimension();
        final int nbColumns = this.getColumnDimension();

        // Initialize a new array
        final double[][] data = new double[nbRows][nbColumns];
        // Copy the entries into it
        for (int i = 0; i < nbRows; ++i) {
            final double[] dataI = data[i];
            for (int j = 0; j < nbColumns; ++j) {
                dataI[j] = this.getEntry(i, j);
            }
        }
        return data;
    }

    /** {@inheritDoc} */
    @Override
    public double[][] getData(final boolean forceCopy) {
        return getData();
    }

    /** {@inheritDoc} */
    @Override
    public double getNorm() {
        return this.walkInColumnOrder(new RealMatrixPreservingVisitor() {

            /** Last row index. */
            private double lastRow;

            /** Sum of absolute values on one column. */
            private double columnSum;

            /** Maximal sum across all columns. */
            private double maxColSum;

            /** {@inheritDoc} */
            @Override
            public void start(final int rows, final int columns, final int startRow,
                    final int endRow, final int startColumn, final int endColumn) {
                this.lastRow = endRow;
                this.columnSum = 0;
                this.maxColSum = 0;
            }

            /** {@inheritDoc} */
            @Override
            public void visit(final int row, final int column, final double value) {
                this.columnSum += MathLib.abs(value);
                if (row == this.lastRow) {
                    this.maxColSum = MathLib.max(this.maxColSum, this.columnSum);
                    this.columnSum = 0;
                }
            }

            /** {@inheritDoc} */
            @Override
            public double end() {
                return this.maxColSum;
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public double getFrobeniusNorm() {
        return this.walkInOptimizedOrder(new RealMatrixPreservingVisitor() {

            /** Sum of squared entries. */
            private double sum;

            /** {@inheritDoc} */
            @Override
            public void start(final int rows, final int columns, final int startRow,
                    final int endRow, final int startColumn, final int endColumn) {
                this.sum = 0;
            }

            /** {@inheritDoc} */
            @Override
            public void visit(final int row, final int column, final double value) {
                this.sum += value * value;
            }

            /** {@inheritDoc} */
            @Override
            public double end() {
                return MathLib.sqrt(this.sum);
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public double getTrace() {
        // Ensure this is a square matrix
        checkSquare();

        // Compute the trace of the matrix
        double trace = 0;
        final int dim = this.getRowDimension();
        for (int i = 0; i < dim; ++i) {
            trace += this.getEntry(i, i);
        }
        return trace;
    }

    /** {@inheritDoc} */
    @Override
    public double getMax() {
        // First element used to define the initial max value
        double maxElement = this.getEntry(0, 0);
        // Search for the entry with the highest value
        for (int i = 0; i < this.getRowDimension(); i++) {
            for (int j = 0; j < this.getColumnDimension(); j++) {
                final double entry = this.getEntry(i, j);
                if (entry > maxElement) {
                    // Update the max value
                    maxElement = entry;
                }
            }
        }
        return maxElement;
    }

    /** {@inheritDoc} */
    @Override
    public double getMin() {
        // First element used to define the initial min value
        double minElement = this.getEntry(0, 0);
        // Search for the entry with the lowest value
        for (int i = 0; i < this.getRowDimension(); i++) {
            for (int j = 0; j < this.getColumnDimension(); j++) {
                final double entry = this.getEntry(i, j);
                if (entry < minElement) {
                    // Update the min value
                    minElement = entry;
                }
            }
        }
        return minElement;
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix getSubMatrix(final int startRow, final int endRow, final int startColumn,
            final int endColumn) {
        // Check the indices
        MatrixUtils.checkSubMatrixIndex(this, startRow, endRow, startColumn, endColumn);

        // Extract the submatrix
        final RealMatrix subMatrix = this.createMatrix(endRow - startRow + 1, endColumn
                - startColumn + 1);
        for (int i = startRow; i <= endRow; ++i) {
            for (int j = startColumn; j <= endColumn; ++j) {
                subMatrix.setEntry(i - startRow, j - startColumn, this.getEntry(i, j));
            }
        }
        return subMatrix;
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix getSubMatrix(final int[] selectedRows, final int[] selectedColumns) {
        // Check the indices
        MatrixUtils.checkSubMatrixIndex(this, selectedRows, selectedColumns);

        // Extract the submatrix
        final RealMatrix subMatrix = this.createMatrix(selectedRows.length, selectedColumns.length);
        subMatrix.walkInOptimizedOrder(new DefaultRealMatrixChangingVisitor() {
            /** {@inheritDoc} */
            @Override
            public double visit(final int row, final int column, final double value) {
                return AbstractRealMatrix.this.getEntry(selectedRows[row], selectedColumns[column]);
            }
        });
        return subMatrix;
    }

    /** {@inheritDoc} */
    @Override
    public void copySubMatrix(final int startRow, final int endRow, final int startColumn,
            final int endColumn, final double[][] destination) {
        copySubMatrix(startRow, endRow, startColumn, endColumn, destination, 0, 0);
    }

    /** {@inheritDoc} */
    @Override
    public void copySubMatrix(final int startRow, final int endRow, final int startColumn,
            final int endColumn, final double[][] destination, final int startRowDest,
            final int startColumnDest) {
        // Check the indices
        MatrixUtils.checkSubMatrixIndex(this, startRow, endRow, startColumn, endColumn);

        // Dimensions of the submatrix
        final int nbRows = endRow + 1 - startRow;
        final int nbColumns = endColumn + 1 - startColumn;

        // Check the destination array
        checkDestinationArray(nbRows, nbColumns, destination, startRowDest, startColumnDest);

        // Extract the submatrix and copy it into the destination array
        for (int i = 0; i < nbRows; i++) {
            final double[] destinationI = destination[i + startRowDest];
            for (int j = 0; j < nbColumns; j++) {
                destinationI[j + startColumnDest] = this.getEntry(i + startRow, j + startColumn);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void copySubMatrix(final int[] selectedRows, final int[] selectedColumns,
            final double[][] destination) {
        copySubMatrix(selectedRows, selectedColumns, destination, 0, 0);
    }

    /** {@inheritDoc} */
    @Override
    public void copySubMatrix(final int[] selectedRows, final int[] selectedColumns,
            final double[][] destination, final int startRowDest, final int startColumnDest) {
        // Check the indices
        MatrixUtils.checkSubMatrixIndex(this, selectedRows, selectedColumns);

        // Number of rows and columns of the submatrix
        final int nbRows = selectedRows.length;
        final int nbColumns = selectedColumns.length;

        // Check the destination array
        checkDestinationArray(nbRows, nbColumns, destination, startRowDest, startColumnDest);

        // Extract the submatrix and copy it into the destination array
        for (int i = 0; i < nbRows; i++) {
            final double[] destinationI = destination[i + startRowDest];
            for (int j = 0; j < nbColumns; j++) {
                destinationI[j + startColumnDest] = this.getEntry(selectedRows[i],
                        selectedColumns[j]);
            }
        }
    }

    /**
     * Ensures the destination array is not {@code null}, and large enough to store the data of a
     * submatrix.
     *
     * @param nbRows
     *        the number of rows of the extracted submatrix
     * @param nbColumns
     *        the number of columns of the extracted submatrix
     * @param destination
     *        the 2D array where the submatrix data is to be copied
     * @param startRowDest
     *        the initial row index in the destination array
     * @param startColumnDest
     *        the initial column index in the destination array
     * @throws NullArgumentException
     *         if the destination array is {@code null}, or if any of the rows which will store the
     *         submatrix data is {@code null}
     * @throws OutOfRangeException
     *         if the initial or final index is not a valid index for the destination array
     * @throws MatrixDimensionMismatchException
     *         if the destination array is not large enough to store the submatrix data
     */
    protected static void checkDestinationArray(final int nbRows, final int nbColumns,
            final double[][] destination, final int startRowDest, final int startColumnDest) {
        // Ensure the destination array is not null
        if (destination == null) {
            throw new NullArgumentException(PatriusMessages.NULL_ARRAY_NOT_ALLOWED);
        }

        // Ensure the start row and column indices are valid indices for the destination array
        MatrixUtils.checkArrayIndex(destination, startRowDest, startColumnDest);

        // Number of available rows in the destination array
        // (taking into account the start row index)
        final int nbAvailableRows = destination.length - startRowDest;

        // Ensure the destination array has enough rows
        if (nbAvailableRows < nbRows) {
            final int nbAvailableColumns = destination[startRowDest].length - startColumnDest;
            throw new MatrixDimensionMismatchException(nbAvailableRows, nbAvailableColumns, nbRows,
                    nbColumns);
        }

        // Ensure the rows are not null and have enough columns
        for (int i = 0; i < nbRows; i++) {
            final double[] row = destination[i + startRowDest];
            if (row == null) {
                throw new NullArgumentException(PatriusMessages.NULL_ARRAY_NOT_ALLOWED);
            }

            final int nbAvailableColumns = row.length - startColumnDest;
            if (nbAvailableColumns < nbColumns) {
                throw new MatrixDimensionMismatchException(nbAvailableRows, nbAvailableColumns,
                        nbRows, nbColumns);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void setSubMatrix(final double[][] subMatrix, final int row, final int column) {
        // Check the submatrix data array
        MatrixUtils.checkMatrixArray(subMatrix);

        // Dimensions of the submatrix
        final int nbRows = subMatrix.length;
        final int nbCols = subMatrix[0].length;

        // Check the initial and final row/column
        MatrixUtils.checkRowIndex(this, row);
        MatrixUtils.checkColumnIndex(this, column);
        MatrixUtils.checkRowIndex(this, nbRows + row - 1);
        MatrixUtils.checkColumnIndex(this, nbCols + column - 1);

        // Set the submatrix
        for (int i = 0; i < nbRows; ++i) {
            for (int j = 0; j < nbCols; ++j) {
                this.setEntry(row + i, column + j, subMatrix[i][j]);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix getRowMatrix(final int row) {
        // Check the index
        MatrixUtils.checkRowIndex(this, row);

        // Extract the selected row
        final int nbCols = this.getColumnDimension();
        final RealMatrix out = this.createMatrix(1, nbCols);
        for (int i = 0; i < nbCols; ++i) {
            out.setEntry(0, i, this.getEntry(row, i));
        }
        return out;
    }

    /** {@inheritDoc} */
    @Override
    public void setRowMatrix(final int row, final RealMatrix matrix) {
        // Check the index
        MatrixUtils.checkRowIndex(this, row);

        // Ensure the provided matrix has the right dimensions
        final int nbCols = this.getColumnDimension();
        if ((matrix.getRowDimension() != 1) || (matrix.getColumnDimension() != nbCols)) {
            throw new MatrixDimensionMismatchException(matrix.getRowDimension(),
                    matrix.getColumnDimension(), 1, nbCols);
        }

        // Set the selected row
        for (int i = 0; i < nbCols; ++i) {
            this.setEntry(row, i, matrix.getEntry(0, i));
        }
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix getColumnMatrix(final int column) {
        // Check the index
        MatrixUtils.checkColumnIndex(this, column);

        // Extract the selected column
        final int nbRows = this.getRowDimension();
        final RealMatrix out = this.createMatrix(nbRows, 1);
        for (int i = 0; i < nbRows; ++i) {
            out.setEntry(i, 0, this.getEntry(i, column));
        }
        return out;
    }

    /** {@inheritDoc} */
    @Override
    public void setColumnMatrix(final int column, final RealMatrix matrix) {
        // Check the index
        MatrixUtils.checkColumnIndex(this, column);

        // Ensure the provided matrix has the right dimensions
        final int nbRows = this.getRowDimension();
        if ((matrix.getRowDimension() != nbRows) || (matrix.getColumnDimension() != 1)) {
            throw new MatrixDimensionMismatchException(matrix.getRowDimension(),
                    matrix.getColumnDimension(), nbRows, 1);
        }

        // Set the selected column
        for (int i = 0; i < nbRows; ++i) {
            this.setEntry(i, column, matrix.getEntry(i, 0));
        }
    }

    /** {@inheritDoc} */
    @Override
    public RealVector getRowVector(final int row) {
        return new ArrayRealVector(this.getRow(row), false);
    }

    /** {@inheritDoc} */
    @Override
    public void setRowVector(final int row, final RealVector vector) {
        // Check the index
        MatrixUtils.checkRowIndex(this, row);

        // Ensure the provided vector has the right dimension
        final int nbCols = this.getColumnDimension();
        if (vector.getDimension() != nbCols) {
            throw new MatrixDimensionMismatchException(1, vector.getDimension(), 1, nbCols);
        }

        // Set the selected row
        for (int i = 0; i < nbCols; ++i) {
            this.setEntry(row, i, vector.getEntry(i));
        }
    }

    /** {@inheritDoc} */
    @Override
    public RealVector getColumnVector(final int column) {
        return new ArrayRealVector(this.getColumn(column), false);
    }

    /** {@inheritDoc} */
    @Override
    public void setColumnVector(final int column, final RealVector vector) {
        // Check the index
        MatrixUtils.checkColumnIndex(this, column);

        // Ensure the provided vector has the right dimension
        final int nbRows = this.getRowDimension();
        if (vector.getDimension() != nbRows) {
            throw new MatrixDimensionMismatchException(vector.getDimension(), 1, nbRows, 1);
        }

        // Set the selected column
        for (int i = 0; i < nbRows; ++i) {
            this.setEntry(i, column, vector.getEntry(i));
        }
    }

    /** {@inheritDoc} */
    @Override
    public double[] getRow(final int row) {
        // Check the index
        MatrixUtils.checkRowIndex(this, row);

        // Extract the selected row
        final int nbCols = this.getColumnDimension();
        final double[] out = new double[nbCols];
        for (int i = 0; i < nbCols; ++i) {
            out[i] = this.getEntry(row, i);
        }
        return out;
    }

    /** {@inheritDoc} */
    @Override
    public void setRow(final int row, final double[] array) {
        // Check the index
        MatrixUtils.checkRowIndex(this, row);

        // Ensure the provided array has the right dimension
        final int nbCols = this.getColumnDimension();
        if (array.length != nbCols) {
            throw new MatrixDimensionMismatchException(1, array.length, 1, nbCols);
        }

        // Set the selected row
        for (int i = 0; i < nbCols; ++i) {
            this.setEntry(row, i, array[i]);
        }
    }

    /** {@inheritDoc} */
    @Override
    public double[] getColumn(final int column) {
        // Check the index
        MatrixUtils.checkColumnIndex(this, column);

        // Extract the selected column
        final int nbRows = this.getRowDimension();
        final double[] out = new double[nbRows];
        for (int i = 0; i < nbRows; ++i) {
            out[i] = this.getEntry(i, column);
        }
        return out;
    }

    /** {@inheritDoc} */
    @Override
    public void setColumn(final int column, final double[] array) {
        // Check the index
        MatrixUtils.checkColumnIndex(this, column);

        // Ensure the provided array has the right dimension
        final int nbRows = this.getRowDimension();
        if (array.length != nbRows) {
            throw new MatrixDimensionMismatchException(array.length, 1, nbRows, 1);
        }

        // Set the selected column
        for (int i = 0; i < nbRows; ++i) {
            this.setEntry(i, column, array[i]);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void addToEntry(final int row, final int column, final double increment) {
        MatrixUtils.checkMatrixIndex(this, row, column);
        this.setEntry(row, column, this.getEntry(row, column) + increment);
    }

    /** {@inheritDoc} */
    @Override
    public void multiplyEntry(final int row, final int column, final double factor) {
        MatrixUtils.checkMatrixIndex(this, row, column);
        this.setEntry(row, column, this.getEntry(row, column) * factor);
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix transpose() {
        // Dimensions of the matrix
        final int nbRows = this.getRowDimension();
        final int nbCols = this.getColumnDimension();

        // Compute the transpose
        final RealMatrix out = this.createMatrix(nbCols, nbRows);
        this.walkInOptimizedOrder(new DefaultRealMatrixPreservingVisitor() {
            /** {@inheritDoc} */
            @Override
            public void visit(final int row, final int column, final double value) {
                out.setEntry(column, row, value);
            }
        });

        return out;
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix transpose(final boolean forceCopy) {
        return this.transpose();
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix concatenateHorizontally(final RealMatrix m) {
        return this.concatenateHorizontally(m, true);
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix concatenateHorizontally(final RealMatrix m, final boolean rightConcatenation) {
        // Dimensions of the two matrices
        final int nbRows1 = this.getRowDimension();
        final int nbRows2 = m.getRowDimension();

        // Throw an exception if the two matrices don't have the same number of rows.
        if (nbRows1 != nbRows2) {
            throw new DimensionMismatchException(nbRows2, nbRows1);
        }
        final int nbCols1 = this.getColumnDimension();
        final int nbCols2 = m.getColumnDimension();

        // Concatenate the matrices
        final RealMatrix result = this.createMatrix(nbRows1, nbCols1 + nbCols2);

        if (rightConcatenation) {
            // Place the provided matrix in the right part of the concatenated matrix
            result.setSubMatrix(this.getData(false), 0, 0);
            result.setSubMatrix(m.getData(false), 0, nbCols1);
        } else {
            // Place the provided matrix in the left part of the concatenated matrix
            result.setSubMatrix(m.getData(false), 0, 0);
            result.setSubMatrix(this.getData(false), 0, nbCols2);
        }

        return result;
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix concatenateVertically(final RealMatrix m) {
        return this.concatenateVertically(m, true);
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix concatenateVertically(final RealMatrix m, final boolean lowerConcatenation) {
        // Dimensions of the two matrices
        final int nbCols1 = this.getColumnDimension();
        final int nbCols2 = m.getColumnDimension();

        // Throw an exception if the two matrices don't have the same number of columns.
        if (nbCols1 != nbCols2) {
            throw new DimensionMismatchException(nbCols2, nbCols1);
        }
        final int nbRows1 = this.getRowDimension();
        final int nbRows2 = m.getRowDimension();

        // Concatenate the matrices
        final RealMatrix result = this.createMatrix(nbRows1 + nbRows2, nbCols1);

        if (lowerConcatenation) {
            // Place the provided matrix in the lower part of the concatenated matrix
            result.setSubMatrix(this.getData(false), 0, 0);
            result.setSubMatrix(m.getData(false), nbRows1, 0);
        } else {
            // Place the provided matrix in the upper part of the concatenated matrix
            result.setSubMatrix(m.getData(false), 0, 0);
            result.setSubMatrix(this.getData(false), nbRows2, 0);
        }

        return result;
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix concatenateDiagonally(final RealMatrix m) {
        return concatenateDiagonally(m, true, true);
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix concatenateDiagonally(final RealMatrix m,
            final boolean lowerRightConcatenation) {
        final RealMatrix result;
        if (lowerRightConcatenation) {
            result = concatenateDiagonally(m, true, true);
        } else {
            result = concatenateDiagonally(m, false, false);
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix concatenateDiagonally(final RealMatrix m, final boolean rightConcatenation,
            final boolean lowerConcatenation) {
        // Dimensions of the two matrices
        final int nbRows1 = this.getRowDimension();
        final int nbCols1 = this.getColumnDimension();
        final int nbRows2 = m.getRowDimension();
        final int nbCols2 = m.getColumnDimension();

        // Concatenate the matrices
        final RealMatrix result = this.createMatrix(nbRows1 + nbRows2, nbCols1 + nbCols2);

        if (lowerConcatenation) {
            if (rightConcatenation) {
                // Place the provided matrix in the lower right part of the concatenated matrix
                result.setSubMatrix(this.getData(false), 0, 0);
                result.setSubMatrix(m.getData(false), nbRows1, nbCols1);
            } else {
                // Place the provided matrix in the lower left part of the concatenated matrix
                result.setSubMatrix(this.getData(false), 0, nbCols2);
                result.setSubMatrix(m.getData(false), nbRows1, 0);
            }
        } else {
            if (rightConcatenation) {
                // Place the provided matrix in the upper right part of the concatenated matrix
                result.setSubMatrix(m.getData(false), 0, nbCols1);
                result.setSubMatrix(this.getData(false), nbRows2, 0);
            } else {
                // Place the provided matrix in the upper left part of the concatenated matrix
                result.setSubMatrix(m.getData(false), 0, 0);
                result.setSubMatrix(this.getData(false), nbRows2, nbCols2);
            }
        }

        return result;
    }

    /** {@inheritDoc} */
    @Override
    public void setDefaultDecomposition(
            final Function<RealMatrix, Decomposition> defaultDecompositionBuilder) {
        this.defaultDecomposition = defaultDecompositionBuilder;
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Unless overridden by calling {@link #setDefaultDecomposition(Function)}, the default
     * decomposition is a {@linkplain LUDecomposition#decompositionBuilder(double) LU decomposition}
     * with a singularity threshold of {@value #DEFAULT_SINGULARITY_THRESHOLD}.
     * </p>
     */
    @Override
    public Function<RealMatrix, Decomposition> getDefaultDecomposition() {
        if (this.defaultDecomposition == null) {
            this.defaultDecomposition = LUDecomposition
                    .decompositionBuilder(DEFAULT_SINGULARITY_THRESHOLD);
        }
        return this.defaultDecomposition;
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix getInverse() {
        return this.getInverse(getDefaultDecomposition());
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix getInverse(final Function<RealMatrix, Decomposition> decompositionBuilder) {
        // Ensure this is a square matrix
        checkSquare();

        // Compute the inverse matrix
        final Decomposition decomposition = decompositionBuilder.apply(this);
        return decomposition.getSolver().getInverse();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isSquare() {
        return this.getColumnDimension() == this.getRowDimension();
    }

    /** {@inheritDoc} */
    @Override
    public boolean
            isOrthogonal(final double normalityThreshold, final double orthogonalityThreshold) {
        boolean orthogonal = false;

        // Only possible for square matrix, false otherwise
        if (this.isSquare()) {
            orthogonal = true;
            final int dim = this.getColumnDimension();
            // Loop on each column
            for (int i = 0; i < dim && orthogonal; i++) {
                // Retrieve the current column
                final RealVector column = this.getColumnVector(i);

                // Check the normality of the column
                final double norm = column.getNorm();
                if (!Precision.equalsWithRelativeTolerance(norm, 1, normalityThreshold)) {
                    orthogonal &= false;
                }

                // Check the orthogonality of the column with respect to the remaining columns
                for (int j = i + 1; j < dim && orthogonal; j++) {
                    final double dotProduct = column.dotProduct(this.getColumnVector(j));
                    if (!Precision.equals(dotProduct, 0, orthogonalityThreshold)) {
                        orthogonal &= false;
                    }
                }
            }
        }

        return orthogonal;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isDiagonal(final double absoluteTolerance) {
        boolean diagonal = false;

        // Only possible for square matrix, false otherwise
        if (this.isSquare()) {
            diagonal = true;
            final int dim = this.getRowDimension();

            // Check the off-diagonal elements (must be equal to zero)
            for (int i = 0; i < dim && diagonal; i++) {
                for (int j = 0; j < dim && diagonal; j++) {
                    if (i != j) {
                        // Off-diagonal elements
                        diagonal &= Precision.equals(this.getEntry(i, j), 0, absoluteTolerance);
                    }
                }
            }
        }

        return diagonal;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isInvertible(final double relativeTolerance) {
        boolean invertible = false;

        // Only possible for square matrix, false otherwise
        if (this.isSquare()) {
            invertible = true;
            final int dim = this.getColumnDimension();

            // Loop on each column
            for (int i = 0; i < dim && invertible; i++) {
                // Retrieve the current column
                final RealVector columnI = this.getColumnVector(i);
                final double normI = columnI.getNorm();

                for (int j = i + 1; j < dim && invertible; j++) {
                    final RealVector columnJ = this.getColumnVector(j);
                    final double normJ = columnJ.getNorm();

                    // Expected dot product between the two columns assuming they are
                    // colinear, and actual dot product between the two columns.
                    final double colinearDotProduct = normI * normJ;
                    final double dotProduct = columnI.dotProduct(this.getColumnVector(j));
                    if (Precision.equalsWithRelativeTolerance(dotProduct, colinearDotProduct,
                            relativeTolerance)) {
                        invertible &= false;
                    }
                }
            }
        }

        return invertible;
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * The absolute and relative tolerances both default to
     * {@linkplain Precision#DOUBLE_COMPARISON_EPSILON}.
     * </p>
     */
    @Override
    public boolean isSymmetric() {
        return this.isSymmetric(Precision.DOUBLE_COMPARISON_EPSILON,
                Precision.DOUBLE_COMPARISON_EPSILON);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isSymmetric(final double relativeTolerance) {
        return this.isSymmetric(relativeTolerance, 0.);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isSymmetric(final double relativeTolerance, final double absoluteTolerance) {
        boolean symmetric = false;

        // Only possible for square matrix, false otherwise
        if (this.isSquare()) {
            symmetric = true;
            final int dim = this.getRowDimension();

            // Check the off-diagonal elements
            for (int i = 1; i < dim && symmetric; ++i) {
                for (int j = 0; j < i && symmetric; ++j) {
                    // Symmetric elements must have the same value
                    symmetric &= Precision.equalsWithAbsoluteOrRelativeTolerances(
                            this.getEntry(i, j), this.getEntry(j, i), relativeTolerance,
                            absoluteTolerance);
                }
            }
        }

        return symmetric;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isAntisymmetric(final double relativeTolerance, final double absoluteTolerance) {
        boolean antisymmetric = false;

        // Only possible for square matrix, false otherwise
        if (this.isSquare()) {
            antisymmetric = true;
            final int dim = this.getRowDimension();

            for (int i = 0; i < dim && antisymmetric; ++i) {
                // Check the diagonal elements
                // (must be equal to zero)
                antisymmetric &= Precision.equals(this.getEntry(i, i), 0., absoluteTolerance);

                // Check the off-diagonal elements
                for (int j = 0; j < i && antisymmetric; ++j) {
                    // Symmetric elements must have the same value, but opposite signs
                    antisymmetric &= Precision.equalsWithAbsoluteOrRelativeTolerances(
                            this.getEntry(i, j), -this.getEntry(j, i), relativeTolerance,
                            absoluteTolerance);
                }
            }
        }

        return antisymmetric;
    }

    /** {@inheritDoc} */
    @Override
    public double walkInRowOrder(final RealMatrixChangingVisitor visitor) {
        // Dimensions of the matrix
        final int nbRows = this.getRowDimension();
        final int nbColumns = this.getColumnDimension();

        // Visit and change the elements of the matrix
        visitor.start(nbRows, nbColumns, 0, nbRows - 1, 0, nbColumns - 1);
        for (int row = 0; row < nbRows; ++row) {
            for (int column = 0; column < nbColumns; ++column) {
                // Visit each element
                final double oldValue = this.getEntry(row, column);
                final double newValue = visitor.visit(row, column, oldValue);
                this.setEntry(row, column, newValue);
            }
        }
        // Return the value at the end of the walk
        return visitor.end();
    }

    /** {@inheritDoc} */
    @Override
    public double walkInRowOrder(final RealMatrixPreservingVisitor visitor) {
        // Dimensions of the matrix
        final int nbRows = this.getRowDimension();
        final int nbColumns = this.getColumnDimension();

        // Visit the elements of the matrix
        visitor.start(nbRows, nbColumns, 0, nbRows - 1, 0, nbColumns - 1);
        for (int row = 0; row < nbRows; ++row) {
            for (int column = 0; column < nbColumns; ++column) {
                visitor.visit(row, column, this.getEntry(row, column));
            }
        }
        // Return the value at the end of the walk
        return visitor.end();
    }

    /** {@inheritDoc} */
    @Override
    public double walkInRowOrder(final RealMatrixChangingVisitor visitor, final int startRow,
            final int endRow, final int startColumn, final int endColumn) {
        // Check the indices
        MatrixUtils.checkSubMatrixIndex(this, startRow, endRow, startColumn, endColumn);

        // Visit and change the elements of the submatrix
        visitor.start(this.getRowDimension(), this.getColumnDimension(), startRow, endRow,
                startColumn, endColumn);
        for (int row = startRow; row <= endRow; ++row) {
            for (int column = startColumn; column <= endColumn; ++column) {
                final double oldValue = this.getEntry(row, column);
                final double newValue = visitor.visit(row, column, oldValue);
                this.setEntry(row, column, newValue);
            }
        }
        // Return the value at the end of the walk
        return visitor.end();
    }

    /** {@inheritDoc} */
    @Override
    public double walkInRowOrder(final RealMatrixPreservingVisitor visitor, final int startRow,
            final int endRow, final int startColumn, final int endColumn) {
        // Check the indices
        MatrixUtils.checkSubMatrixIndex(this, startRow, endRow, startColumn, endColumn);

        // Visit the elements of the submatrix
        visitor.start(this.getRowDimension(), this.getColumnDimension(), startRow, endRow,
                startColumn, endColumn);
        for (int row = startRow; row <= endRow; ++row) {
            for (int column = startColumn; column <= endColumn; ++column) {
                visitor.visit(row, column, this.getEntry(row, column));
            }
        }
        // Return the value at the end of the walk
        return visitor.end();
    }

    /** {@inheritDoc} */
    @Override
    public double walkInColumnOrder(final RealMatrixChangingVisitor visitor) {
        // Dimensions of the matrix
        final int nbRows = this.getRowDimension();
        final int nbColumns = this.getColumnDimension();

        // Visit and change the elements of the matrix
        visitor.start(nbRows, nbColumns, 0, nbRows - 1, 0, nbColumns - 1);
        for (int column = 0; column < nbColumns; ++column) {
            for (int row = 0; row < nbRows; ++row) {
                final double oldValue = this.getEntry(row, column);
                final double newValue = visitor.visit(row, column, oldValue);
                this.setEntry(row, column, newValue);
            }
        }
        // Return the value at the end of the walk
        return visitor.end();
    }

    /** {@inheritDoc} */
    @Override
    public double walkInColumnOrder(final RealMatrixPreservingVisitor visitor) {
        // Dimensions
        final int nbRows = this.getRowDimension();
        final int nbColumns = this.getColumnDimension();

        // Visit the elements of the matrix
        visitor.start(nbRows, nbColumns, 0, nbRows - 1, 0, nbColumns - 1);
        for (int column = 0; column < nbColumns; ++column) {
            for (int row = 0; row < nbRows; ++row) {
                visitor.visit(row, column, this.getEntry(row, column));
            }
        }
        // Return the value at the end of the walk
        return visitor.end();
    }

    /** {@inheritDoc} */
    @Override
    public double walkInColumnOrder(final RealMatrixChangingVisitor visitor, final int startRow,
            final int endRow, final int startColumn, final int endColumn) {
        // Check the indices
        MatrixUtils.checkSubMatrixIndex(this, startRow, endRow, startColumn, endColumn);

        // Visit and change the elements of the submatrix
        visitor.start(this.getRowDimension(), this.getColumnDimension(), startRow, endRow,
                startColumn, endColumn);
        for (int column = startColumn; column <= endColumn; ++column) {
            for (int row = startRow; row <= endRow; ++row) {
                final double oldValue = this.getEntry(row, column);
                final double newValue = visitor.visit(row, column, oldValue);
                this.setEntry(row, column, newValue);
            }
        }
        // Return the value at the end of the walk
        return visitor.end();
    }

    /** {@inheritDoc} */
    @Override
    public double walkInColumnOrder(final RealMatrixPreservingVisitor visitor, final int startRow,
            final int endRow, final int startColumn, final int endColumn) {
        // Check the indices
        MatrixUtils.checkSubMatrixIndex(this, startRow, endRow, startColumn, endColumn);

        // Visit the elements of the submatrix
        visitor.start(this.getRowDimension(), this.getColumnDimension(), startRow, endRow,
                startColumn, endColumn);
        for (int column = startColumn; column <= endColumn; ++column) {
            for (int row = startRow; row <= endRow; ++row) {
                visitor.visit(row, column, this.getEntry(row, column));
            }
        }
        // Return the value at the end of the walk
        return visitor.end();
    }

    /** {@inheritDoc} */
    @Override
    public double walkInOptimizedOrder(final RealMatrixChangingVisitor visitor) {
        return this.walkInRowOrder(visitor);
    }

    /** {@inheritDoc} */
    @Override
    public double walkInOptimizedOrder(final RealMatrixPreservingVisitor visitor) {
        return this.walkInRowOrder(visitor);
    }

    /** {@inheritDoc} */
    @Override
    public double walkInOptimizedOrder(final RealMatrixChangingVisitor visitor, final int startRow,
            final int endRow, final int startColumn, final int endColumn) {
        return this.walkInRowOrder(visitor, startRow, endRow, startColumn, endColumn);
    }

    /** {@inheritDoc} */
    @Override
    public double walkInOptimizedOrder(final RealMatrixPreservingVisitor visitor,
            final int startRow, final int endRow, final int startColumn, final int endColumn) {
        return this.walkInRowOrder(visitor, startRow, endRow, startColumn, endColumn);
    }

    /**
     * Get a string representation for this matrix.
     * <p>
     * Default format is {@link MatrixUtils#VISUAL_FORMAT}.
     * </p>
     *
     * @return a string representation for this matrix
     */
    @Override
    public String toString() {
        return this.toString(MatrixUtils.VISUAL_FORMAT);
    }

    /** {@inheritDoc} */
    @Override
    public String toString(final RealMatrixFormat realMatrixFormat) {
        // Build the string representation of the matrix
        final StringBuilder builder = new StringBuilder();

        // Class name
        final String className = this.getClass().getSimpleName();
        builder.append(className);

        // Line separator:
        // The use of "\n" instead of System.lineSeparator() is intentional. The aim is
        // to remain compatible with any type of line separator used by the matrix format.
        final String lineSeparator = "\n";

        // Get a string representation of the matrix and indent it if necessary
        final int n = className.length();
        final String indentation = String.format("%" + n + "c", ' ');
        final String initialString = realMatrixFormat.format(this);
        final String[] split = initialString.split(lineSeparator);

        final String indentedString = String.join(lineSeparator + indentation, split);
        builder.append(indentedString);
        if (initialString.endsWith(lineSeparator)) {
            builder.append(lineSeparator);
        }

        // Extract the String from the builder
        return builder.toString();
    }

    /**
     * Returns {@code true} if the provided object is a {@linkplain RealMatrix} instance with the
     * same dimensions as this matrix, whose entries are strictly equal to the entries of this
     * matrix (no absolute or relative tolerance is taken into account when comparing the entries).
     *
     * @param object
     *        the object to be tested for equality
     * @return {@code true} if the provided object is equal to this matrix
     */
    @Override
    public boolean equals(final Object object) {
        boolean equal = false;

        if (object == this) {
            // Same instance
            equal = true;
        } else if (object instanceof RealMatrix) {
            // No need to check for null before an instanceof;
            // the instanceof keyword returns false when given a null argument.

            // Compare the dimensions and the entries of the two matrices
            final RealMatrix m = (RealMatrix) object;
            equal = equals(m, 0., 0.);
        }

        return equal;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final RealMatrix m, final double relativeThreshold,
            final double absoluteThreshold) {
        boolean equal = false;

        if (m == this) {
            // Same instance
            equal = true;
        } else if (m != null) {
            // Dimensions of the matrix
            final int nbRows = this.getRowDimension();
            final int nbCols = this.getColumnDimension();

            // If the two matrices have the same number of rows and columns
            if (m.getRowDimension() == nbRows && m.getColumnDimension() == nbCols) {
                equal = true;
                // Check every entries
                for (int i = 0; i < nbRows && equal; i++) {
                    for (int j = 0; j < nbCols && equal; j++) {
                        equal &= Precision.equalsWithAbsoluteOrRelativeTolerances(
                                this.getEntry(i, j), m.getEntry(i, j), relativeThreshold,
                                absoluteThreshold);
                    }
                }
            }
        }

        return equal;
    }

    /**
     * Computes a hash code for the matrix.
     *
     * @return hash code for matrix
     */
    @Override
    public int hashCode() {
        // CHECKSTYLE: stop MagicNumber check
        // Reason: model - Orekit code
        int ret = 7;
        final int nRows = this.getRowDimension();
        final int nCols = this.getColumnDimension();
        ret = ret * 31 + nRows;
        ret = ret * 31 + nCols;
        for (int row = 0; row < nRows; ++row) {
            for (int col = 0; col < nCols; ++col) {
                ret = ret * 31 + (11 * (row + 1) + 17 * (col + 1))
                        * MathUtils.hash(this.getEntry(row, col));
            }
        }
        return ret;
        // CHECKSTYLE: resume MagicNumber check
    }

    // Empty implementations of these methods are provided in order
    // to allow for the use of the @Override tag with Java 1.5.

    /** {@inheritDoc} */
    @Override
    public abstract int getRowDimension();

    /** {@inheritDoc} */
    @Override
    public abstract int getColumnDimension();

    /** {@inheritDoc} */
    @Override
    public abstract double getEntry(int row, int column);

    /** {@inheritDoc} */
    @Override
    public abstract void setEntry(int row, int column, double value);

    /** {@inheritDoc} */
    @Override
    public abstract RealMatrix createMatrix(int rowDimension, int columnDimension);

    /** {@inheritDoc} */
    @Override
    public abstract RealMatrix copy();
}
