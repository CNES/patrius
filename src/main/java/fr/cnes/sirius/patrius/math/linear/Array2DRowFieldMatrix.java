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
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.6:FA:FA-2542:27/01/2021:[PATRIUS] Definition d'un champ de vue avec demi-angle de 180° 
 * VERSION:4.5.1:FA:FA-2540:04/08/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear
 * VERSION:4.5:DM:DM-2300:27/05/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.linear;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.Field;
import fr.cnes.sirius.patrius.math.FieldElement;
import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.MathIllegalStateException;
import fr.cnes.sirius.patrius.math.exception.NoDataException;
import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.util.MathArrays;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

//CHECKSTYLE: stop CommentRatio check
//Reason: model - Commons-Math code kept as such

/**
 * Implementation of FieldMatrix<T> using a {@link FieldElement}[][] array to store entries.
 * <p>
 * As specified in the {@link FieldMatrix} interface, matrix element indexing is 0-based -- e.g.,
 * <code>getEntry(0, 0)</code> returns the element in the first row, first column of the matrix.</li></ul>
 * </p>
 * 
 * <p>This class is up-to-date with commons-math 3.6.1.</p>
 * 
 * @param <T>
 *        the type of the field elements
 * @version $Id: Array2DRowFieldMatrix.java 18108 2017-10-04 06:45:27Z bignon $
 */
@SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
public class Array2DRowFieldMatrix<T extends FieldElement<T>>
    extends AbstractFieldMatrix<T>
    implements Serializable {
    /** Serializable version identifier */
    private static final long serialVersionUID = 7260756672015356458L;
    /** Entries of the matrix */
    private T[][] data;

    /**
     * Creates a matrix with no data
     * 
     * @param field
     *        field to which the elements belong
     */
    public Array2DRowFieldMatrix(final Field<T> field) {
        super(field);
    }

    /**
     * Create a new {@code FieldMatrix<T>} with the supplied row and column dimensions.
     * 
     * @param field
     *        Field to which the elements belong.
     * @param rowDimension
     *        Number of rows in the new matrix.
     * @param columnDimension
     *        Number of columns in the new matrix.
     * @throws NotStrictlyPositiveException
     *         if row or column dimension is not positive.
     */
    public Array2DRowFieldMatrix(final Field<T> field, final int rowDimension,
        final int columnDimension) {
        super(field, rowDimension, columnDimension);
        this.data = MathArrays.buildArray(field, rowDimension, columnDimension);
    }

    /**
     * Create a new {@code FieldMatrix<T>} using the input array as the underlying
     * data array.
     * <p>
     * The input array is copied, not referenced. This constructor has the same effect as calling
     * {@link #Array2DRowFieldMatrix(FieldElement[][], boolean)} with the second argument set to {@code true}.
     * </p>
     * 
     * @param d
     *        Data for the new matrix.
     * @throws DimensionMismatchException
     *         if {@code d} is not rectangular.
     * @throws NullArgumentException
     *         if {@code d} is {@code null}.
     * @throws NoDataException
     *         if there are not at least one row and one column.
     * @see #Array2DRowFieldMatrix(FieldElement[][], boolean)
     */
    public Array2DRowFieldMatrix(final T[][] d) {
        this(extractField(d), d);
    }

    /**
     * Create a new {@code FieldMatrix<T>} using the input array as the underlying
     * data array.
     * <p>
     * The input array is copied, not referenced. This constructor has the same effect as calling
     * {@link #Array2DRowFieldMatrix(FieldElement[][], boolean)} with the second argument set to {@code true}.
     * </p>
     * 
     * @param field
     *        Field to which the elements belong.
     * @param d
     *        Data for the new matrix.
     * @throws DimensionMismatchException
     *         if {@code d} is not rectangular.
     * @throws NullArgumentException
     *         if {@code d} is {@code null}.
     * @throws NoDataException
     *         if there are not at least one row and one column.
     * @see #Array2DRowFieldMatrix(FieldElement[][], boolean)
     */
    public Array2DRowFieldMatrix(final Field<T> field, final T[][] d) {
        super(field);
        this.copyIn(d);
    }

    /**
     * Create a new {@code FieldMatrix<T>} using the input array as the underlying
     * data array.
     * <p>
     * If an array is built specially in order to be embedded in a {@code FieldMatrix<T>} and not used directly, the
     * {@code copyArray} may be set to {@code false}. This will prevent the copying and improve performance as no new
     * array will be built and no data will be copied.
     * </p>
     * 
     * @param d
     *        Data for the new matrix.
     * @param copyArray
     *        Whether to copy or reference the input array.
     * @throws DimensionMismatchException
     *         if {@code d} is not rectangular.
     * @throws NoDataException
     *         if there are not at least one row and one column.
     * @throws NullArgumentException
     *         if {@code d} is {@code null}.
     * @see #Array2DRowFieldMatrix(FieldElement[][])
     */
    public Array2DRowFieldMatrix(final T[][] d, final boolean copyArray) {
        this(extractField(d), d, copyArray);
    }

    /**
     * Create a new {@code FieldMatrix<T>} using the input array as the underlying
     * data array.
     * <p>
     * If an array is built specially in order to be embedded in a {@code FieldMatrix<T>} and not used directly, the
     * {@code copyArray} may be set to {@code false}. This will prevent the copying and improve performance as no new
     * array will be built and no data will be copied.
     * </p>
     * 
     * @param field
     *        Field to which the elements belong.
     * @param d
     *        Data for the new matrix.
     * @param copyArray
     *        Whether to copy or reference the input array.
     * @throws DimensionMismatchException
     *         if {@code d} is not rectangular.
     * @throws NoDataException
     *         if there are not at least one row and one column.
     * @throws NullArgumentException
     *         if {@code d} is {@code null}.
     * @see #Array2DRowFieldMatrix(FieldElement[][])
     */
    public Array2DRowFieldMatrix(final Field<T> field, final T[][] d, final boolean copyArray) {
        super(field);
        if (copyArray) {
            this.copyIn(d);
        } else {
            MathUtils.checkNotNull(d);
            final int nRows = d.length;
            if (nRows == 0) {
                throw new NoDataException(PatriusMessages.AT_LEAST_ONE_ROW);
            }
            final int nCols = d[0].length;
            if (nCols == 0) {
                throw new NoDataException(PatriusMessages.AT_LEAST_ONE_COLUMN);
            }
            for (int r = 1; r < nRows; r++) {
                if (d[r].length != nCols) {
                    throw new DimensionMismatchException(nCols, d[r].length);
                }
            }
            this.data = d;
        }
    }

    /**
     * Create a new (column) {@code FieldMatrix<T>} using {@code v} as the
     * data for the unique column of the created matrix.
     * The input array is copied.
     * 
     * @param v
     *        Column vector holding data for new matrix.
     * @throws NoDataException
     *         if v is empty
     */
    public Array2DRowFieldMatrix(final T[] v) {
        this(extractField(v), v);
    }

    /**
     * Create a new (column) {@code FieldMatrix<T>} using {@code v} as the
     * data for the unique column of the created matrix.
     * The input array is copied.
     * 
     * @param field
     *        Field to which the elements belong.
     * @param v
     *        Column vector holding data for new matrix.
     */
    public Array2DRowFieldMatrix(final Field<T> field, final T[] v) {
        super(field);
        final int nRows = v.length;
        this.data = MathArrays.buildArray(this.getField(), nRows, 1);
        for (int row = 0; row < nRows; row++) {
            this.data[row][0] = v[row];
        }
    }

    /** {@inheritDoc} */
    @Override
    public FieldMatrix<T> createMatrix(final int rowDimension,
                                       final int columnDimension) {
        return new Array2DRowFieldMatrix<T>(this.getField(), rowDimension, columnDimension);
    }

    /** {@inheritDoc} */
    @Override
    public FieldMatrix<T> copy() {
        return new Array2DRowFieldMatrix<T>(this.getField(), this.copyOut(), false);
    }

    /**
     * Add {@code m} to this matrix.
     * 
     * @param m
     *        Matrix to be added.
     * @return {@code this} + m.
     * @throws MatrixDimensionMismatchException
     *         if {@code m} is not the same
     *         size as this matrix.
     */
    public Array2DRowFieldMatrix<T> add(final Array2DRowFieldMatrix<T> m) {
        // safety check
        this.checkAdditionCompatible(m);

        final int rowCount = this.getRowDimension();
        final int columnCount = this.getColumnDimension();
        final T[][] outData = MathArrays.buildArray(this.getField(), rowCount, columnCount);
        for (int row = 0; row < rowCount; row++) {
            final T[] dataRow = this.data[row];
            final T[] mRow = m.data[row];
            final T[] outDataRow = outData[row];
            for (int col = 0; col < columnCount; col++) {
                outDataRow[col] = dataRow[col].add(mRow[col]);
            }
        }

        return new Array2DRowFieldMatrix<T>(this.getField(), outData, false);
    }

    /**
     * Subtract {@code m} from this matrix.
     * 
     * @param m
     *        Matrix to be subtracted.
     * @return {@code this} + m.
     * @throws MatrixDimensionMismatchException
     *         if {@code m} is not the same
     *         size as this matrix.
     */
    public Array2DRowFieldMatrix<T> subtract(final Array2DRowFieldMatrix<T> m) {
        // safety check
        this.checkSubtractionCompatible(m);

        final int rowCount = this.getRowDimension();
        final int columnCount = this.getColumnDimension();
        final T[][] outData = MathArrays.buildArray(this.getField(), rowCount, columnCount);
        for (int row = 0; row < rowCount; row++) {
            final T[] dataRow = this.data[row];
            final T[] mRow = m.data[row];
            final T[] outDataRow = outData[row];
            for (int col = 0; col < columnCount; col++) {
                outDataRow[col] = dataRow[col].subtract(mRow[col]);
            }
        }

        return new Array2DRowFieldMatrix<T>(this.getField(), outData, false);

    }

    /**
     * Postmultiplying this matrix by {@code m}.
     * 
     * @param m
     *        Matrix to postmultiply by.
     * @return {@code this} * m.
     * @throws DimensionMismatchException
     *         if the number of columns of this
     *         matrix is not equal to the number of rows of {@code m}.
     */
    public Array2DRowFieldMatrix<T> multiply(final Array2DRowFieldMatrix<T> m) {
        // safety check
        this.checkMultiplicationCompatible(m);

        final int nRows = this.getRowDimension();
        final int nCols = m.getColumnDimension();
        final int nSum = this.getColumnDimension();
        final T[][] outData = MathArrays.buildArray(this.getField(), nRows, nCols);
        for (int row = 0; row < nRows; row++) {
            final T[] dataRow = this.data[row];
            final T[] outDataRow = outData[row];
            for (int col = 0; col < nCols; col++) {
                T sum = this.getField().getZero();
                for (int i = 0; i < nSum; i++) {
                    sum = sum.add(dataRow[i].multiply(m.data[i][col]));
                }
                outDataRow[col] = sum;
            }
        }

        return new Array2DRowFieldMatrix<T>(this.getField(), outData, false);

    }

    /** {@inheritDoc} */
    @Override
    public T[][] getData() {
        return this.copyOut();
    }

    /**
     * Get a reference to the underlying data array.
     * This methods returns internal data, <strong>not</strong> fresh copy of it.
     * 
     * @return the 2-dimensional array of entries.
     */
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    public T[][] getDataRef() {
        return this.data;
    }

    /** {@inheritDoc} */
    @Override
    public void setSubMatrix(final T[][] subMatrix, final int row,
                             final int column) {
        if (this.data == null) {
            if (row > 0) {
                throw new MathIllegalStateException(PatriusMessages.FIRST_ROWS_NOT_INITIALIZED_YET, row);
            }
            if (column > 0) {
                throw new MathIllegalStateException(PatriusMessages.FIRST_COLUMNS_NOT_INITIALIZED_YET, column);
            }
            final int nRows = subMatrix.length;
            if (nRows == 0) {
                throw new NoDataException(PatriusMessages.AT_LEAST_ONE_ROW);
            }

            final int nCols = subMatrix[0].length;
            if (nCols == 0) {
                throw new NoDataException(PatriusMessages.AT_LEAST_ONE_COLUMN);
            }
            this.data = MathArrays.buildArray(this.getField(), subMatrix.length, nCols);
            for (int i = 0; i < this.data.length; ++i) {
                if (subMatrix[i].length != nCols) {
                    throw new DimensionMismatchException(nCols, subMatrix[i].length);
                }
                System.arraycopy(subMatrix[i], 0, this.data[i + row], column, nCols);
            }
        } else {
            super.setSubMatrix(subMatrix, row, column);
        }

    }

    /** {@inheritDoc} */
    @Override
    public T getEntry(final int row, final int column) {
        this.checkRowIndex(row);
        this.checkColumnIndex(column);

        return this.data[row][column];
    }

    /** {@inheritDoc} */
    @Override
    public void setEntry(final int row, final int column, final T value) {
        this.checkRowIndex(row);
        this.checkColumnIndex(column);

        this.data[row][column] = value;
    }

    /** {@inheritDoc} */
    @Override
    public void addToEntry(final int row, final int column, final T increment) {
        this.checkRowIndex(row);
        this.checkColumnIndex(column);

        this.data[row][column] = this.data[row][column].add(increment);
    }

    /** {@inheritDoc} */
    @Override
    public void multiplyEntry(final int row, final int column, final T factor) {
        this.checkRowIndex(row);
        this.checkColumnIndex(column);

        this.data[row][column] = this.data[row][column].multiply(factor);
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
    public T[] operate(final T[] v) {
        final int nCols = this.getColumnDimension();
        if (v.length != nCols) {
            throw new DimensionMismatchException(v.length, nCols);
        }
        final int nRows = this.getRowDimension();

        final T[] out = MathArrays.buildArray(this.getField(), nRows);
        for (int row = 0; row < nRows; row++) {
            final T[] dataRow = this.data[row];
            T sum = this.getField().getZero();
            for (int i = 0; i < nCols; i++) {
                sum = sum.add(dataRow[i].multiply(v[i]));
            }
            out[row] = sum;
        }
        return out;
    }

    /** {@inheritDoc} */
    @Override
    public T[] preMultiply(final T[] v) {
        final int nRows = this.getRowDimension();
        if (v.length != nRows) {
            throw new DimensionMismatchException(v.length, nRows);
        }
        final int nCols = this.getColumnDimension();

        final T[] out = MathArrays.buildArray(this.getField(), nCols);
        for (int col = 0; col < nCols; ++col) {
            T sum = this.getField().getZero();
            for (int i = 0; i < nRows; ++i) {
                sum = sum.add(this.data[i][col].multiply(v[i]));
            }
            out[col] = sum;
        }

        return out;
    }

    /** {@inheritDoc} */
    @Override
    public T walkInRowOrder(final FieldMatrixChangingVisitor<T> visitor) {
        final int rows = this.getRowDimension();
        final int columns = this.getColumnDimension();
        visitor.start(rows, columns, 0, rows - 1, 0, columns - 1);
        for (int i = 0; i < rows; ++i) {
            final T[] rowI = this.data[i];
            for (int j = 0; j < columns; ++j) {
                rowI[j] = visitor.visit(i, j, rowI[j]);
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
        for (int i = 0; i < rows; ++i) {
            final T[] rowI = this.data[i];
            for (int j = 0; j < columns; ++j) {
                visitor.visit(i, j, rowI[j]);
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
        for (int i = startRow; i <= endRow; ++i) {
            final T[] rowI = this.data[i];
            for (int j = startColumn; j <= endColumn; ++j) {
                rowI[j] = visitor.visit(i, j, rowI[j]);
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
        for (int i = startRow; i <= endRow; ++i) {
            final T[] rowI = this.data[i];
            for (int j = startColumn; j <= endColumn; ++j) {
                visitor.visit(i, j, rowI[j]);
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
        for (int j = 0; j < columns; ++j) {
            for (int i = 0; i < rows; ++i) {
                final T[] rowI = this.data[i];
                rowI[j] = visitor.visit(i, j, rowI[j]);
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
        for (int j = 0; j < columns; ++j) {
            for (int i = 0; i < rows; ++i) {
                visitor.visit(i, j, this.data[i][j]);
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
        for (int j = startColumn; j <= endColumn; ++j) {
            for (int i = startRow; i <= endRow; ++i) {
                final T[] rowI = this.data[i];
                rowI[j] = visitor.visit(i, j, rowI[j]);
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
        for (int j = startColumn; j <= endColumn; ++j) {
            for (int i = startRow; i <= endRow; ++i) {
                visitor.visit(i, j, this.data[i][j]);
            }
        }
        return visitor.end();
    }

    /**
     * Get a fresh copy of the underlying data array.
     * 
     * @return a copy of the underlying data array.
     */
    private T[][] copyOut() {
        final int nRows = this.getRowDimension();
        final T[][] out = MathArrays.buildArray(this.getField(), nRows, this.getColumnDimension());
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
    private void copyIn(final T[][] matrix) {
        this.setSubMatrix(matrix, 0, 0);
    }

    // CHECKSTYLE: resume CommentRatio check
}
