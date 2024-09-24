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
 * HISTORY
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.6:FA:FA-2542:27/01/2021:[PATRIUS] Definition d'un champ de vue avec demi-angle de 180° 
 * VERSION:4.5.1:FA:FA-2540:04/08/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear
 * VERSION:4.5:DM:DM-2300:27/05/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.linear;

import java.util.ArrayList;

import fr.cnes.sirius.patrius.math.Field;
import fr.cnes.sirius.patrius.math.FieldElement;
import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.NoDataException;
import fr.cnes.sirius.patrius.math.exception.NotPositiveException;
import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;
import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;
import fr.cnes.sirius.patrius.math.util.MathArrays;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

//CHECKSTYLE: stop CommentRatio check
//Reason: model - Commons-Math code kept as such

/**
 * Basic implementation of {@link FieldMatrix} methods regardless of the underlying storage.
 * <p>
 * All the methods implemented here use {@link #getEntry(int, int)} to access matrix elements. Derived class can provide
 * faster implementations.
 * </p>
 * 
 * <p>This class is up-to-date with commons-math 3.6.1.</p>
 * 
 * @param <T>
 *        Type of the field elements.
 * 
 * @version $Id: AbstractFieldMatrix.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 2.0
 */
@SuppressWarnings("PMD.NullAssignment")
public abstract class AbstractFieldMatrix<T extends FieldElement<T>>
    implements FieldMatrix<T> {
    /** Field to which the elements belong. */
    private final Field<T> field;

    /**
     * Constructor for use with Serializable
     */
    protected AbstractFieldMatrix() {
        this.field = null;
    }

    /**
     * Creates a matrix with no data
     * 
     * @param fieldIn
     *        field to which the elements belong
     */
    protected AbstractFieldMatrix(final Field<T> fieldIn) {
        this.field = fieldIn;
    }

    /**
     * Create a new FieldMatrix<T> with the supplied row and column dimensions.
     * 
     * @param fieldIn
     *        Field to which the elements belong.
     * @param rowDimension
     *        Number of rows in the new matrix.
     * @param columnDimension
     *        Number of columns in the new matrix.
     * @throws NotStrictlyPositiveException
     *         if row or column dimension is not
     *         positive.
     */
    protected AbstractFieldMatrix(final Field<T> fieldIn,
        final int rowDimension,
        final int columnDimension) {
        if (rowDimension <= 0) {
            throw new NotStrictlyPositiveException(PatriusMessages.DIMENSION,
                rowDimension);
        }
        if (columnDimension <= 0) {
            throw new NotStrictlyPositiveException(PatriusMessages.DIMENSION,
                columnDimension);
        }
        this.field = fieldIn;
    }

    /**
     * Get the elements type from an array.
     * 
     * @param <T>
     *        Type of the field elements.
     * @param d
     *        Data array.
     * @return the field to which the array elements belong.
     * @throws NullArgumentException
     *         if the array is {@code null}.
     * @throws NoDataException
     *         if the array is empty.
     */
    protected static <T extends FieldElement<T>> Field<T> extractField(final T[][] d) {
        if (d == null) {
            throw new NullArgumentException();
        }
        if (d.length == 0) {
            throw new NoDataException(PatriusMessages.AT_LEAST_ONE_ROW);
        }
        if (d[0].length == 0) {
            throw new NoDataException(PatriusMessages.AT_LEAST_ONE_COLUMN);
        }
        return d[0][0].getField();
    }

    /**
     * Get the elements type from an array.
     * 
     * @param <T>
     *        Type of the field elements.
     * @param d
     *        Data array.
     * @return the field to which the array elements belong.
     * @throws NoDataException
     *         if array is empty.
     */
    protected static <T extends FieldElement<T>> Field<T> extractField(final T[] d) {
        if (d.length == 0) {
            throw new NoDataException(PatriusMessages.AT_LEAST_ONE_ROW);
        }
        return d[0].getField();
    }

    /** {@inheritDoc} */
    @Override
    public Field<T> getField() {
        return this.field;
    }

    /** {@inheritDoc} */
    @Override
    public abstract FieldMatrix<T> createMatrix(final int rowDimension, final int columnDimension);

    /** {@inheritDoc} */
    @Override
    public abstract FieldMatrix<T> copy();

    /** {@inheritDoc} */
    @Override
    public FieldMatrix<T> add(final FieldMatrix<T> m) {
        // safety check
        this.checkAdditionCompatible(m);

        final int rowCount = this.getRowDimension();
        final int columnCount = this.getColumnDimension();
        final FieldMatrix<T> out = this.createMatrix(rowCount, columnCount);
        for (int row = 0; row < rowCount; ++row) {
            for (int col = 0; col < columnCount; ++col) {
                out.setEntry(row, col, this.getEntry(row, col).add(m.getEntry(row, col)));
            }
        }

        return out;
    }

    /** {@inheritDoc} */
    @Override
    public FieldMatrix<T> subtract(final FieldMatrix<T> m) {
        // safety check
        this.checkSubtractionCompatible(m);

        final int rowCount = this.getRowDimension();
        final int columnCount = this.getColumnDimension();
        final FieldMatrix<T> out = this.createMatrix(rowCount, columnCount);
        for (int row = 0; row < rowCount; ++row) {
            for (int col = 0; col < columnCount; ++col) {
                out.setEntry(row, col, this.getEntry(row, col).subtract(m.getEntry(row, col)));
            }
        }

        return out;
    }

    /** {@inheritDoc} */
    @Override
    public FieldMatrix<T> scalarAdd(final T d) {

        final int rowCount = this.getRowDimension();
        final int columnCount = this.getColumnDimension();
        final FieldMatrix<T> out = this.createMatrix(rowCount, columnCount);
        for (int row = 0; row < rowCount; ++row) {
            for (int col = 0; col < columnCount; ++col) {
                out.setEntry(row, col, this.getEntry(row, col).add(d));
            }
        }

        return out;
    }

    /** {@inheritDoc} */
    @Override
    public FieldMatrix<T> scalarMultiply(final T d) {
        final int rowCount = this.getRowDimension();
        final int columnCount = this.getColumnDimension();
        final FieldMatrix<T> out = this.createMatrix(rowCount, columnCount);
        for (int row = 0; row < rowCount; ++row) {
            for (int col = 0; col < columnCount; ++col) {
                out.setEntry(row, col, this.getEntry(row, col).multiply(d));
            }
        }

        return out;
    }

    /** {@inheritDoc} */
    @Override
    public FieldMatrix<T> multiply(final FieldMatrix<T> m) {
        // safety check
        this.checkMultiplicationCompatible(m);

        final int nRows = this.getRowDimension();
        final int nCols = m.getColumnDimension();
        final int nSum = this.getColumnDimension();
        final FieldMatrix<T> out = this.createMatrix(nRows, nCols);
        for (int row = 0; row < nRows; ++row) {
            for (int col = 0; col < nCols; ++col) {
                T sum = this.field.getZero();
                for (int i = 0; i < nSum; ++i) {
                    sum = sum.add(this.getEntry(row, i).multiply(m.getEntry(i, col)));
                }
                out.setEntry(row, col, sum);
            }
        }

        return out;
    }

    /** {@inheritDoc} */
    @Override
    public FieldMatrix<T> preMultiply(final FieldMatrix<T> m) {
        return m.multiply(this);
    }

    /** {@inheritDoc} */
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Commons-Math code kept as such
    @Override
    public FieldMatrix<T> power(final int p) {
        // CHECKSTYLE: resume ReturnCount check
        if (p < 0) {
            throw new NotPositiveException(p);
        }

        if (!this.isSquare()) {
            throw new NonSquareMatrixException(this.getRowDimension(), this.getColumnDimension());
        }

        if (p == 0) {
            return MatrixUtils.createFieldIdentityMatrix(this.getField(), this.getRowDimension());
        }

        if (p == 1) {
            return this.copy();
        }

        final int power = p - 1;

        /*
         * Only log_2(p) operations is used by doing as follows:
         * 5^214 = 5^128 * 5^64 * 5^16 * 5^4 * 5^2
         * In general, the same approach is used for A^p.
         */

        final char[] binaryRepresentation = Integer.toBinaryString(power)
            .toCharArray();
        final ArrayList<Integer> nonZeroPositions = new ArrayList<>();

        for (int i = 0; i < binaryRepresentation.length; ++i) {
            if (binaryRepresentation[i] == '1') {
                final int pos = binaryRepresentation.length - i - 1;
                nonZeroPositions.add(pos);
            }
        }

        final ArrayList<FieldMatrix<T>> results = new ArrayList<>(
            binaryRepresentation.length);

        results.add(0, this.copy());

        for (int i = 1; i < binaryRepresentation.length; ++i) {
            final FieldMatrix<T> s = results.get(i - 1);
            final FieldMatrix<T> r = s.multiply(s);
            results.add(i, r);
        }

        FieldMatrix<T> result = this.copy();

        for (final Integer i : nonZeroPositions) {
            result = result.multiply(results.get(i));
        }

        return result;
    }

    /** {@inheritDoc} */
    @Override
    public T[][] getData() {
        final T[][] data = MathArrays.buildArray(this.field, getRowDimension(), getColumnDimension());

        for (int i = 0; i < data.length; ++i) {
            final T[] dataI = data[i];
            for (int j = 0; j < dataI.length; ++j) {
                dataI[j] = this.getEntry(i, j);
            }
        }

        return data;
    }

    /** {@inheritDoc} */
    @Override
    public FieldMatrix<T> getSubMatrix(final int startRow, final int endRow,
                                       final int startColumn, final int endColumn) {
        this.checkSubMatrixIndex(startRow, endRow, startColumn, endColumn);

        final FieldMatrix<T> subMatrix =
            this.createMatrix(endRow - startRow + 1, endColumn - startColumn + 1);
        for (int i = startRow; i <= endRow; ++i) {
            for (int j = startColumn; j <= endColumn; ++j) {
                subMatrix.setEntry(i - startRow, j - startColumn, this.getEntry(i, j));
            }
        }

        return subMatrix;

    }

    /** {@inheritDoc} */
    @Override
    public FieldMatrix<T> getSubMatrix(final int[] selectedRows,
                                       final int[] selectedColumns) {

        // safety checks
        this.checkSubMatrixIndex(selectedRows, selectedColumns);

        // copy entries
        final FieldMatrix<T> subMatrix =
            this.createMatrix(selectedRows.length, selectedColumns.length);
        subMatrix.walkInOptimizedOrder(new DefaultFieldMatrixChangingVisitor<T>(this.field.getZero()){

            /** {@inheritDoc} */
            @Override
            public T visit(final int row, final int column, final T value) {
                return AbstractFieldMatrix.this.getEntry(selectedRows[row], selectedColumns[column]);
            }

        });

        return subMatrix;

    }

    /** {@inheritDoc} */
    @Override
    public void copySubMatrix(final int startRow, final int endRow,
                              final int startColumn, final int endColumn,
                              final T[][] destination) {
        // safety checks
        this.checkSubMatrixIndex(startRow, endRow, startColumn, endColumn);
        final int rowsCount = endRow + 1 - startRow;
        final int columnsCount = endColumn + 1 - startColumn;
        if ((destination.length < rowsCount) || (destination[0].length < columnsCount)) {
            throw new MatrixDimensionMismatchException(destination.length,
                destination[0].length,
                rowsCount,
                columnsCount);
        }

        // copy entries
        this.walkInOptimizedOrder(new DefaultFieldMatrixPreservingVisitor<T>(this.field.getZero()){

            /** Initial row index. */
            private int startRow;

            /** Initial column index. */
            private int startColumn;

            /** {@inheritDoc} */
            @Override
            public void start(final int rows, final int columns,
                              final int startRow, final int endRow,
                              final int startColumn, final int endColumn) {
                this.startRow = startRow;
                this.startColumn = startColumn;
            }

            /** {@inheritDoc} */
            @Override
            public void visit(final int row, final int column, final T value) {
                destination[row - this.startRow][column - this.startColumn] = value;
            }

        }, startRow, endRow, startColumn, endColumn);

    }

    /** {@inheritDoc} */
    @Override
    public void copySubMatrix(final int[] selectedRows, final int[] selectedColumns, final T[][] destination) {
        // safety checks
        this.checkSubMatrixIndex(selectedRows, selectedColumns);
        if ((destination.length < selectedRows.length) ||
            (destination[0].length < selectedColumns.length)) {
            throw new MatrixDimensionMismatchException(destination.length,
                destination[0].length,
                selectedRows.length,
                selectedColumns.length);
        }

        // copy entries
        for (int i = 0; i < selectedRows.length; i++) {
            final T[] destinationI = destination[i];
            for (int j = 0; j < selectedColumns.length; j++) {
                destinationI[j] = this.getEntry(selectedRows[i], selectedColumns[j]);
            }
        }

    }

    /** {@inheritDoc} */
    @Override
    public void setSubMatrix(final T[][] subMatrix, final int row,
                             final int column) {
        if (subMatrix == null) {
            throw new NullArgumentException();
        }
        final int nRows = subMatrix.length;
        if (nRows == 0) {
            throw new NoDataException(PatriusMessages.AT_LEAST_ONE_ROW);
        }

        final int nCols = subMatrix[0].length;
        if (nCols == 0) {
            throw new NoDataException(PatriusMessages.AT_LEAST_ONE_COLUMN);
        }

        for (int r = 1; r < nRows; ++r) {
            if (subMatrix[r].length != nCols) {
                throw new DimensionMismatchException(nCols, subMatrix[r].length);
            }
        }

        this.checkRowIndex(row);
        this.checkColumnIndex(column);
        this.checkRowIndex(nRows + row - 1);
        this.checkColumnIndex(nCols + column - 1);

        for (int i = 0; i < nRows; ++i) {
            for (int j = 0; j < nCols; ++j) {
                this.setEntry(row + i, column + j, subMatrix[i][j]);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public FieldMatrix<T> getRowMatrix(final int row) {
        this.checkRowIndex(row);
        final int nCols = this.getColumnDimension();
        final FieldMatrix<T> out = this.createMatrix(1, nCols);
        for (int i = 0; i < nCols; ++i) {
            out.setEntry(0, i, this.getEntry(row, i));
        }

        return out;

    }

    /** {@inheritDoc} */
    @Override
    public void setRowMatrix(final int row, final FieldMatrix<T> matrix) {
        this.checkRowIndex(row);
        final int nCols = this.getColumnDimension();
        if ((matrix.getRowDimension() != 1) ||
            (matrix.getColumnDimension() != nCols)) {
            throw new MatrixDimensionMismatchException(matrix.getRowDimension(),
                matrix.getColumnDimension(),
                1, nCols);
        }
        for (int i = 0; i < nCols; ++i) {
            this.setEntry(row, i, matrix.getEntry(0, i));
        }

    }

    /** {@inheritDoc} */
    @Override
    public FieldMatrix<T> getColumnMatrix(final int column) {

        this.checkColumnIndex(column);
        final int nRows = this.getRowDimension();
        final FieldMatrix<T> out = this.createMatrix(nRows, 1);
        for (int i = 0; i < nRows; ++i) {
            out.setEntry(i, 0, this.getEntry(i, column));
        }

        return out;

    }

    /** {@inheritDoc} */
    @Override
    public void setColumnMatrix(final int column, final FieldMatrix<T> matrix) {
        this.checkColumnIndex(column);
        final int nRows = this.getRowDimension();
        if ((matrix.getRowDimension() != nRows) ||
            (matrix.getColumnDimension() != 1)) {
            throw new MatrixDimensionMismatchException(matrix.getRowDimension(),
                matrix.getColumnDimension(),
                nRows, 1);
        }
        for (int i = 0; i < nRows; ++i) {
            this.setEntry(i, column, matrix.getEntry(i, 0));
        }

    }

    /** {@inheritDoc} */
    @Override
    public FieldVector<T> getRowVector(final int row) {
        return new ArrayFieldVector<>(this.field, this.getRow(row), false);
    }

    /** {@inheritDoc} */
    @Override
    public void setRowVector(final int row, final FieldVector<T> vector) {
        this.checkRowIndex(row);
        final int nCols = this.getColumnDimension();
        if (vector.getDimension() != nCols) {
            throw new MatrixDimensionMismatchException(1, vector.getDimension(),
                1, nCols);
        }
        for (int i = 0; i < nCols; ++i) {
            this.setEntry(row, i, vector.getEntry(i));
        }

    }

    /** {@inheritDoc} */
    @Override
    public FieldVector<T> getColumnVector(final int column) {
        return new ArrayFieldVector<>(this.field, this.getColumn(column), false);
    }

    /** {@inheritDoc} */
    @Override
    public void setColumnVector(final int column, final FieldVector<T> vector) {

        this.checkColumnIndex(column);
        final int nRows = this.getRowDimension();
        if (vector.getDimension() != nRows) {
            throw new MatrixDimensionMismatchException(vector.getDimension(), 1,
                nRows, 1);
        }
        for (int i = 0; i < nRows; ++i) {
            this.setEntry(i, column, vector.getEntry(i));
        }

    }

    /** {@inheritDoc} */
    @Override
    public T[] getRow(final int row) {
        this.checkRowIndex(row);
        final int nCols = this.getColumnDimension();
        final T[] out = MathArrays.buildArray(this.field, nCols);
        for (int i = 0; i < nCols; ++i) {
            out[i] = this.getEntry(row, i);
        }

        return out;

    }

    /** {@inheritDoc} */
    @Override
    public void setRow(final int row, final T[] array) {
        this.checkRowIndex(row);
        final int nCols = this.getColumnDimension();
        if (array.length != nCols) {
            throw new MatrixDimensionMismatchException(1, array.length, 1, nCols);
        }
        for (int i = 0; i < nCols; ++i) {
            this.setEntry(row, i, array[i]);
        }

    }

    /** {@inheritDoc} */
    @Override
    public T[] getColumn(final int column) {
        this.checkColumnIndex(column);
        final int nRows = this.getRowDimension();
        final T[] out = MathArrays.buildArray(this.field, nRows);
        for (int i = 0; i < nRows; ++i) {
            out[i] = this.getEntry(i, column);
        }

        return out;

    }

    /** {@inheritDoc} */
    @Override
    public void setColumn(final int column, final T[] array) {
        this.checkColumnIndex(column);
        final int nRows = this.getRowDimension();
        if (array.length != nRows) {
            throw new MatrixDimensionMismatchException(array.length, 1, nRows, 1);
        }
        for (int i = 0; i < nRows; ++i) {
            this.setEntry(i, column, array[i]);
        }
    }

    /** {@inheritDoc} */
    @Override
    public abstract T getEntry(int row, int column);

    /** {@inheritDoc} */
    @Override
    public abstract void setEntry(int row, int column, T value);

    /** {@inheritDoc} */
    @Override
    public abstract void addToEntry(int row, int column, T increment);

    /** {@inheritDoc} */
    @Override
    public abstract void multiplyEntry(int row, int column, T factor);

    /** {@inheritDoc} */
    @Override
    public FieldMatrix<T> transpose() {
        final int nRows = this.getRowDimension();
        final int nCols = this.getColumnDimension();
        final FieldMatrix<T> out = this.createMatrix(nCols, nRows);
        this.walkInOptimizedOrder(new DefaultFieldMatrixPreservingVisitor<T>(this.field.getZero()){
            /** {@inheritDoc} */
            @Override
            public void visit(final int row, final int column, final T value) {
                out.setEntry(column, row, value);
            }
        });

        return out;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isSquare() {
        return this.getColumnDimension() == this.getRowDimension();
    }

    /** {@inheritDoc} */
    @Override
    public abstract int getRowDimension();

    /** {@inheritDoc} */
    @Override
    public abstract int getColumnDimension();

    /** {@inheritDoc} */
    @Override
    public T getTrace() {
        final int nRows = this.getRowDimension();
        final int nCols = this.getColumnDimension();
        if (nRows != nCols) {
            throw new NonSquareMatrixException(nRows, nCols);
        }
        T trace = this.field.getZero();
        for (int i = 0; i < nRows; ++i) {
            trace = trace.add(this.getEntry(i, i));
        }
        return trace;
    }

    /** {@inheritDoc} */
    @Override
    public T[] operate(final T[] v) {

        final int nCols = this.getColumnDimension();
        if (v.length != nCols) {
            throw new DimensionMismatchException(v.length, nCols);
        }

        final int nRows = this.getRowDimension();

        final T[] out = MathArrays.buildArray(this.field, nRows);
        for (int row = 0; row < nRows; ++row) {
            T sum = this.field.getZero();
            for (int i = 0; i < nCols; ++i) {
                sum = sum.add(this.getEntry(row, i).multiply(v[i]));
            }
            out[row] = sum;
        }

        return out;
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.PreserveStackTrace")
    public FieldVector<T> operate(final FieldVector<T> v) {
        try {
            return new ArrayFieldVector<>(this.field, this.operate(((ArrayFieldVector<T>) v).getDataRef()), false);
        } catch (final ClassCastException cce) {
            final int nCols = this.getColumnDimension();
            if (v.getDimension() != nCols) {
                throw new DimensionMismatchException(v.getDimension(), nCols);
            }

            final int nRows = this.getRowDimension();

            final T[] out = MathArrays.buildArray(this.field, nRows);
            for (int row = 0; row < nRows; ++row) {
                T sum = this.field.getZero();
                for (int i = 0; i < nCols; ++i) {
                    sum = sum.add(this.getEntry(row, i).multiply(v.getEntry(i)));
                }
                out[row] = sum;
            }

            return new ArrayFieldVector<>(this.field, out, false);
        }
    }

    /** {@inheritDoc} */
    @Override
    public T[] preMultiply(final T[] v) {

        final int nRows = this.getRowDimension();
        if (v.length != nRows) {
            throw new DimensionMismatchException(v.length, nRows);
        }

        final int nCols = this.getColumnDimension();

        final T[] out = MathArrays.buildArray(this.field, nCols);
        for (int col = 0; col < nCols; ++col) {
            T sum = this.field.getZero();
            for (int i = 0; i < nRows; ++i) {
                sum = sum.add(this.getEntry(i, col).multiply(v[i]));
            }
            out[col] = sum;
        }

        return out;
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.PreserveStackTrace")
    public FieldVector<T> preMultiply(final FieldVector<T> v) {
        try {
            return new ArrayFieldVector<>(this.field, this.preMultiply(((ArrayFieldVector<T>) v).getDataRef()), false);
        } catch (final ClassCastException cce) {
            final int nRows = this.getRowDimension();
            if (v.getDimension() != nRows) {
                throw new DimensionMismatchException(v.getDimension(), nRows);
            }

            final int nCols = this.getColumnDimension();

            final T[] out = MathArrays.buildArray(this.field, nCols);
            for (int col = 0; col < nCols; ++col) {
                T sum = this.field.getZero();
                for (int i = 0; i < nRows; ++i) {
                    sum = sum.add(this.getEntry(i, col).multiply(v.getEntry(i)));
                }
                out[col] = sum;
            }

            return new ArrayFieldVector<>(this.field, out, false);
        }
    }

    /** {@inheritDoc} */
    @Override
    public T walkInRowOrder(final FieldMatrixChangingVisitor<T> visitor) {
        final int rows = this.getRowDimension();
        final int columns = this.getColumnDimension();
        visitor.start(rows, columns, 0, rows - 1, 0, columns - 1);
        for (int row = 0; row < rows; ++row) {
            for (int column = 0; column < columns; ++column) {
                final T oldValue = this.getEntry(row, column);
                final T newValue = visitor.visit(row, column, oldValue);
                this.setEntry(row, column, newValue);
            }
        }
        return visitor.end();
    }

    /** {@inheritDoc} */
    @Override
    public T walkInRowOrder(final FieldMatrixPreservingVisitor<T> visitor) {
        final int rows = this.getRowDimension();
        final int columns = this.getColumnDimension();
        visitor.start(rows, columns, 0, rows - 1, 0, columns - 1);
        for (int row = 0; row < rows; ++row) {
            for (int column = 0; column < columns; ++column) {
                visitor.visit(row, column, this.getEntry(row, column));
            }
        }
        return visitor.end();
    }

    /** {@inheritDoc} */
    @Override
    public T walkInRowOrder(final FieldMatrixChangingVisitor<T> visitor,
                            final int startRow, final int endRow,
                            final int startColumn, final int endColumn) {
        this.checkSubMatrixIndex(startRow, endRow, startColumn, endColumn);
        visitor.start(this.getRowDimension(), this.getColumnDimension(),
            startRow, endRow, startColumn, endColumn);
        for (int row = startRow; row <= endRow; ++row) {
            for (int column = startColumn; column <= endColumn; ++column) {
                final T oldValue = this.getEntry(row, column);
                final T newValue = visitor.visit(row, column, oldValue);
                this.setEntry(row, column, newValue);
            }
        }
        return visitor.end();
    }

    /** {@inheritDoc} */
    @Override
    public T walkInRowOrder(final FieldMatrixPreservingVisitor<T> visitor,
                            final int startRow, final int endRow,
                            final int startColumn, final int endColumn) {
        this.checkSubMatrixIndex(startRow, endRow, startColumn, endColumn);
        visitor.start(this.getRowDimension(), this.getColumnDimension(),
            startRow, endRow, startColumn, endColumn);
        for (int row = startRow; row <= endRow; ++row) {
            for (int column = startColumn; column <= endColumn; ++column) {
                visitor.visit(row, column, this.getEntry(row, column));
            }
        }
        return visitor.end();
    }

    /** {@inheritDoc} */
    @Override
    public T walkInColumnOrder(final FieldMatrixChangingVisitor<T> visitor) {
        final int rows = this.getRowDimension();
        final int columns = this.getColumnDimension();
        visitor.start(rows, columns, 0, rows - 1, 0, columns - 1);
        for (int column = 0; column < columns; ++column) {
            for (int row = 0; row < rows; ++row) {
                final T oldValue = this.getEntry(row, column);
                final T newValue = visitor.visit(row, column, oldValue);
                this.setEntry(row, column, newValue);
            }
        }
        return visitor.end();
    }

    /** {@inheritDoc} */
    @Override
    public T walkInColumnOrder(final FieldMatrixPreservingVisitor<T> visitor) {
        final int rows = this.getRowDimension();
        final int columns = this.getColumnDimension();
        visitor.start(rows, columns, 0, rows - 1, 0, columns - 1);
        for (int column = 0; column < columns; ++column) {
            for (int row = 0; row < rows; ++row) {
                visitor.visit(row, column, this.getEntry(row, column));
            }
        }
        return visitor.end();
    }

    /** {@inheritDoc} */
    @Override
    public T walkInColumnOrder(final FieldMatrixChangingVisitor<T> visitor,
                               final int startRow, final int endRow,
                               final int startColumn, final int endColumn) {
        this.checkSubMatrixIndex(startRow, endRow, startColumn, endColumn);
        visitor.start(this.getRowDimension(), this.getColumnDimension(),
            startRow, endRow, startColumn, endColumn);
        for (int column = startColumn; column <= endColumn; ++column) {
            for (int row = startRow; row <= endRow; ++row) {
                final T oldValue = this.getEntry(row, column);
                final T newValue = visitor.visit(row, column, oldValue);
                this.setEntry(row, column, newValue);
            }
        }
        return visitor.end();
    }

    /** {@inheritDoc} */
    @Override
    public T walkInColumnOrder(final FieldMatrixPreservingVisitor<T> visitor,
                               final int startRow, final int endRow,
                               final int startColumn, final int endColumn) {
        this.checkSubMatrixIndex(startRow, endRow, startColumn, endColumn);
        visitor.start(this.getRowDimension(), this.getColumnDimension(),
            startRow, endRow, startColumn, endColumn);
        for (int column = startColumn; column <= endColumn; ++column) {
            for (int row = startRow; row <= endRow; ++row) {
                visitor.visit(row, column, this.getEntry(row, column));
            }
        }
        return visitor.end();
    }

    /** {@inheritDoc} */
    @Override
    public T walkInOptimizedOrder(final FieldMatrixChangingVisitor<T> visitor) {
        return this.walkInRowOrder(visitor);
    }

    /** {@inheritDoc} */
    @Override
    public T walkInOptimizedOrder(final FieldMatrixPreservingVisitor<T> visitor) {
        return this.walkInRowOrder(visitor);
    }

    /** {@inheritDoc} */
    @Override
    public T walkInOptimizedOrder(final FieldMatrixChangingVisitor<T> visitor,
                                  final int startRow, final int endRow,
                                  final int startColumn, final int endColumn) {
        return this.walkInRowOrder(visitor, startRow, endRow, startColumn, endColumn);
    }

    /** {@inheritDoc} */
    @Override
    public T walkInOptimizedOrder(final FieldMatrixPreservingVisitor<T> visitor,
                                  final int startRow, final int endRow,
                                  final int startColumn, final int endColumn) {
        return this.walkInRowOrder(visitor, startRow, endRow, startColumn, endColumn);
    }

    /**
     * Get a string representation for this matrix.
     * 
     * @return a string representation for this matrix
     */
    @Override
    public String toString() {
        final String comma = ",";
        final String openBracket = "{";
        final String closeBracket = "}";
        final int nRows = this.getRowDimension();
        final int nCols = this.getColumnDimension();
        final StringBuffer res = new StringBuffer();
        final String fullClassName = this.getClass().getName();
        final String shortClassName = fullClassName.substring(fullClassName.lastIndexOf('.') + 1);
        res.append(shortClassName).append(openBracket);

        for (int i = 0; i < nRows; ++i) {
            if (i > 0) {
                res.append(comma);
            }
            res.append(openBracket);
            for (int j = 0; j < nCols; ++j) {
                if (j > 0) {
                    res.append(comma);
                }
                res.append(this.getEntry(i, j));
            }
            res.append(closeBracket);
        }

        res.append(closeBracket);
        return res.toString();
    }

    /**
     * Returns true iff <code>object</code> is a <code>FieldMatrix</code> instance with the same dimensions as this
     * and all corresponding matrix entries are equal.
     * 
     * @param object
     *        the object to test equality against.
     * @return true if object equals this
     */
    @Override
    public boolean equals(final Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof FieldMatrix<?>)) {
            return false;
        }
        final FieldMatrix<?> m = (FieldMatrix<?>) object;
        final int nRows = this.getRowDimension();
        final int nCols = this.getColumnDimension();
        if (m.getColumnDimension() != nCols || m.getRowDimension() != nRows) {
            return false;
        }
        for (int row = 0; row < nRows; ++row) {
            for (int col = 0; col < nCols; ++col) {
                if (!this.getEntry(row, col).equals(m.getEntry(row, col))) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Computes a hashcode for the matrix.
     * 
     * @return hashcode for matrix
     */
    @Override
    public int hashCode() {
        // CHECKSTYLE: stop MagicNumber check
        // Reason: model - Orekit code
        int ret = 322562;
        final int nRows = this.getRowDimension();
        final int nCols = this.getColumnDimension();
        ret = ret * 31 + nRows;
        ret = ret * 31 + nCols;
        for (int row = 0; row < nRows; ++row) {
            for (int col = 0; col < nCols; ++col) {
                ret = ret * 31 + (11 * (row + 1) + 17 * (col + 1)) * this.getEntry(row, col).hashCode();
            }
        }
        return ret;
        // CHECKSTYLE: resume MagicNumber check
    }

    /**
     * Check if a row index is valid.
     * 
     * @param row
     *        Row index to check.
     * @throws OutOfRangeException
     *         if {@code index} is not valid.
     */
    protected void checkRowIndex(final int row) {
        if (row < 0 || row >= this.getRowDimension()) {
            throw new OutOfRangeException(PatriusMessages.ROW_INDEX,
                row, 0, this.getRowDimension() - 1);
        }
    }

    /**
     * Check if a column index is valid.
     * 
     * @param column
     *        Column index to check.
     * @throws OutOfRangeException
     *         if {@code index} is not valid.
     */
    protected void checkColumnIndex(final int column) {
        if (column < 0 || column >= this.getColumnDimension()) {
            throw new OutOfRangeException(PatriusMessages.COLUMN_INDEX,
                column, 0, this.getColumnDimension() - 1);
        }
    }

    /**
     * Check if submatrix ranges indices are valid.
     * Rows and columns are indicated counting from 0 to n-1.
     * 
     * @param startRow
     *        Initial row index.
     * @param endRow
     *        Final row index.
     * @param startColumn
     *        Initial column index.
     * @param endColumn
     *        Final column index.
     * @throws OutOfRangeException
     *         if the indices are not valid.
     * @throws NumberIsTooSmallException
     *         if {@code endRow < startRow} or {@code endColumn < startColumn}.
     */
    protected void checkSubMatrixIndex(final int startRow, final int endRow,
                                       final int startColumn, final int endColumn) {
        this.checkRowIndex(startRow);
        this.checkRowIndex(endRow);
        if (endRow < startRow) {
            throw new NumberIsTooSmallException(PatriusMessages.INITIAL_ROW_AFTER_FINAL_ROW,
                endRow, startRow, true);
        }

        this.checkColumnIndex(startColumn);
        this.checkColumnIndex(endColumn);
        if (endColumn < startColumn) {
            throw new NumberIsTooSmallException(PatriusMessages.INITIAL_COLUMN_AFTER_FINAL_COLUMN,
                endColumn, startColumn, true);
        }
    }

    /**
     * Check if submatrix ranges indices are valid.
     * Rows and columns are indicated counting from 0 to n-1.
     * 
     * @param selectedRows
     *        Array of row indices.
     * @param selectedColumns
     *        Array of column indices.
     * @throws NullArgumentException
     *         if the arrays are {@code null}.
     * @throws NoDataException
     *         if the arrays have zero length.
     * @throws OutOfRangeException
     *         if row or column selections are not valid.
     */
    protected void checkSubMatrixIndex(final int[] selectedRows, final int[] selectedColumns) {
        if (selectedRows == null ||
            selectedColumns == null) {
            throw new NullArgumentException();
        }
        if (selectedRows.length == 0 ||
            selectedColumns.length == 0) {
            throw new NoDataException();
        }

        for (final int row : selectedRows) {
            this.checkRowIndex(row);
        }
        for (final int column : selectedColumns) {
            this.checkColumnIndex(column);
        }
    }

    /**
     * Check if a matrix is addition compatible with the instance.
     * 
     * @param m
     *        Matrix to check.
     * @throws MatrixDimensionMismatchException
     *         if the matrix is not
     *         addition-compatible with instance.
     */
    protected void checkAdditionCompatible(final FieldMatrix<T> m) {
        if ((this.getRowDimension() != m.getRowDimension()) ||
            (this.getColumnDimension() != m.getColumnDimension())) {
            throw new MatrixDimensionMismatchException(m.getRowDimension(), m.getColumnDimension(),
                this.getRowDimension(), this.getColumnDimension());
        }
    }

    /**
     * Check if a matrix is subtraction compatible with the instance.
     * 
     * @param m
     *        Matrix to check.
     * @throws MatrixDimensionMismatchException
     *         if the matrix is not
     *         subtraction-compatible with instance.
     */
    protected void checkSubtractionCompatible(final FieldMatrix<T> m) {
        if ((this.getRowDimension() != m.getRowDimension()) ||
            (this.getColumnDimension() != m.getColumnDimension())) {
            throw new MatrixDimensionMismatchException(m.getRowDimension(), m.getColumnDimension(),
                this.getRowDimension(), this.getColumnDimension());
        }
    }

    /**
     * Check if a matrix is multiplication compatible with the instance.
     * 
     * @param m
     *        Matrix to check.
     * @throws DimensionMismatchException
     *         if the matrix is not
     *         multiplication-compatible with instance.
     */
    protected void checkMultiplicationCompatible(final FieldMatrix<T> m) {
        if (this.getColumnDimension() != m.getRowDimension()) {
            throw new DimensionMismatchException(m.getRowDimension(), this.getColumnDimension());
        }
    }

    // CHECKSTYLE: resume CommentRatio check
}
