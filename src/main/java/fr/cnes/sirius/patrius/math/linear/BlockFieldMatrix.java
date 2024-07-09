/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * HISTORY
* VERSION:4.6:FA:FA-2542:27/01/2021:[PATRIUS] Definition d'un champ de vue avec demi-angle de 180° 
 * VERSION:4.5.1:FA:FA-2540:04/08/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear
 * VERSION:4.5:DM:DM-2300:27/05/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
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
 */
package fr.cnes.sirius.patrius.math.linear;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.Field;
import fr.cnes.sirius.patrius.math.FieldElement;
import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.NoDataException;
import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;
import fr.cnes.sirius.patrius.math.util.MathArrays;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

//CHECKSTYLE: stop CommentRatio check
//Reason: model - Commons-Math code kept as such

/**
 * Cache-friendly implementation of FieldMatrix using a flat arrays to store
 * square blocks of the matrix.
 * <p>
 * This implementation is specially designed to be cache-friendly. Square blocks are stored as small arrays and allow
 * efficient traversal of data both in row major direction and columns major direction, one block at a time. This
 * greatly increases performances for algorithms that use crossed directions loops like multiplication or transposition.
 * </p>
 * <p>
 * The size of square blocks is a static parameter. It may be tuned according to the cache size of the target computer
 * processor. As a rule of thumbs, it should be the largest value that allows three blocks to be simultaneously cached
 * (this is necessary for example for matrix multiplication). The default value is to use 36x36 blocks.
 * </p>
 * <p>
 * The regular blocks represent {@link #BLOCK_SIZE} x {@link #BLOCK_SIZE} squares. Blocks at right hand side and bottom
 * side which may be smaller to fit matrix dimensions. The square blocks are flattened in row major order in single
 * dimension arrays which are therefore {@link #BLOCK_SIZE}<sup>2</sup> elements long for regular blocks. The blocks are
 * themselves organized in row major order.
 * </p>
 * <p>
 * As an example, for a block size of 36x36, a 100x60 matrix would be stored in 6 blocks. Block 0 would be a Field[1296]
 * array holding the upper left 36x36 square, block 1 would be a Field[1296] array holding the upper center 36x36
 * square, block 2 would be a Field[1008] array holding the upper right 36x28 rectangle, block 3 would be a Field[864]
 * array holding the lower left 24x36 rectangle, block 4 would be a Field[864] array holding the lower center 24x36
 * rectangle and block 5 would be a Field[672] array holding the lower right 24x28 rectangle.
 * </p>
 * <p>
 * The layout complexity overhead versus simple mapping of matrices to java arrays is negligible for small matrices
 * (about 1%). The gain from cache efficiency leads to up to 3-fold improvements for matrices of moderate to large size.
 * </p>
 * 
 * <p>This class is up-to-date with commons-math 3.6.1.</p>
 * 
 * @param <T>
 *        the type of the field elements
 * @version $Id: BlockFieldMatrix.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 2.0
 */
public class BlockFieldMatrix<T extends FieldElement<T>> extends AbstractFieldMatrix<T> implements Serializable {

    /** Block size. */
    public static final int BLOCK_SIZE = 36;
    /** Serializable version identifier. */
    private static final long serialVersionUID = -4602336630143123183L;
    /** Blocks of matrix entries. */
    private final T[][] blocks;
    /** Number of rows of the matrix. */
    private final int rows;
    /** Number of columns of the matrix. */
    private final int columns;
    /** Number of block rows of the matrix. */
    private final int blockRows;
    /** Number of block columns of the matrix. */
    private final int blockColumns;

    /**
     * Create a new matrix with the supplied row and column dimensions.
     * 
     * @param field
     *        Field to which the elements belong.
     * @param rowsIn
     *        Number of rows in the new matrix.
     * @param columnsIn
     *        Number of columns in the new matrix.
     * @throws NotStrictlyPositiveException
     *         if row or column dimension is not
     *         positive.
     */
    public BlockFieldMatrix(final Field<T> field, final int rowsIn,
        final int columnsIn) {
        super(field, rowsIn, columnsIn);
        this.rows = rowsIn;
        this.columns = columnsIn;

        // number of blocks
        this.blockRows = (rowsIn + BLOCK_SIZE - 1) / BLOCK_SIZE;
        this.blockColumns = (columnsIn + BLOCK_SIZE - 1) / BLOCK_SIZE;

        // allocate storage blocks, taking care of smaller ones at right and bottom
        this.blocks = createBlocksLayout(field, rowsIn, columnsIn);
    }

    /**
     * Create a new dense matrix copying entries from raw layout data.
     * <p>
     * The input array <em>must</em> already be in raw layout.
     * </p>
     * <p>
     * Calling this constructor is equivalent to call:
     * 
     * <pre>
     * matrix = new BlockFieldMatrix&lt;T&gt;(getField(), rawData.length, rawData[0].length,
     *     toBlocksLayout(rawData), false);
     * </pre>
     * 
     * </p>
     * 
     * @param rawData
     *        Data for the new matrix, in raw layout.
     * @throws DimensionMismatchException
     *         if the {@code blockData} shape is
     *         inconsistent with block layout.
     * @see #BlockFieldMatrix(int, int, FieldElement[][], boolean)
     */
    public BlockFieldMatrix(final T[][] rawData) {
        this(rawData.length, rawData[0].length, toBlocksLayout(rawData), false);
    }

    /**
     * Create a new dense matrix copying entries from block layout data.
     * <p>
     * The input array <em>must</em> already be in blocks layout.
     * </p>
     * 
     * @param rowsIn
     *        the number of rows in the new matrix
     * @param columnsIn
     *        the number of columns in the new matrix
     * @param blockData
     *        data for new matrix
     * @param copyArray
     *        if true, the input array will be copied, otherwise
     *        it will be referenced
     * 
     * @throws DimensionMismatchException
     *         if the {@code blockData} shape is
     *         inconsistent with block layout.
     * @throws NotStrictlyPositiveException
     *         if row or column dimension is not
     *         positive.
     * @see #createBlocksLayout(Field, int, int)
     * @see #toBlocksLayout(FieldElement[][])
     * @see #BlockFieldMatrix(FieldElement[][])
     */
    public BlockFieldMatrix(final int rowsIn, final int columnsIn,
        final T[][] blockData, final boolean copyArray) {
        super(extractField(blockData), rowsIn, columnsIn);
        this.rows = rowsIn;
        this.columns = columnsIn;

        // number of blocks
        this.blockRows = (rowsIn + BLOCK_SIZE - 1) / BLOCK_SIZE;
        this.blockColumns = (columnsIn + BLOCK_SIZE - 1) / BLOCK_SIZE;

        if (copyArray) {
            // allocate storage blocks, taking care of smaller ones at right and bottom
            this.blocks = MathArrays.buildArray(this.getField(), this.blockRows * this.blockColumns, -1);
        } else {
            // reference existing array
            this.blocks = blockData;
        }

        int index = 0;
        for (int iBlock = 0; iBlock < this.blockRows; ++iBlock) {
            final int iHeight = this.blockHeight(iBlock);
            for (int jBlock = 0; jBlock < this.blockColumns; ++jBlock, ++index) {
                if (blockData[index].length != iHeight * this.blockWidth(jBlock)) {
                    throw new DimensionMismatchException(blockData[index].length,
                        iHeight * this.blockWidth(jBlock));
                }
                if (copyArray) {
                    this.blocks[index] = blockData[index].clone();
                }
            }
        }
    }

    /**
     * Convert a data array from raw layout to blocks layout.
     * <p>
     * Raw layout is the straightforward layout where element at row i and column j is in array element
     * <code>rawData[i][j]</code>. Blocks layout is the layout used in {@link BlockFieldMatrix} instances, where the
     * matrix is split in square blocks (except at right and bottom side where blocks may be rectangular to fit matrix
     * size) and each block is stored in a flattened one-dimensional array.
     * </p>
     * <p>
     * This method creates an array in blocks layout from an input array in raw layout. It can be used to provide the
     * array argument of the {@link #BlockFieldMatrix(int, int, FieldElement[][], boolean)} constructor.
     * </p>
     * 
     * @param <T>
     *        Type of the field elements.
     * @param rawData
     *        Data array in raw layout.
     * @return a new data array containing the same entries but in blocks layout
     * @throws DimensionMismatchException
     *         if {@code rawData} is not rectangular
     *         (not all rows have the same length).
     * @see #createBlocksLayout(Field, int, int)
     * @see #BlockFieldMatrix(int, int, FieldElement[][], boolean)
     */
    public static <T extends FieldElement<T>> T[][] toBlocksLayout(final T[][] rawData) {

        // Initialization
        final int columns = rawData[0].length;

        // safety checks
        for (final T[] element : rawData) {
            final int length = element.length;
            if (length != columns) {
                throw new DimensionMismatchException(columns, length);
            }
        }

        // Initialization
        final int rows = rawData.length;
        final int blockRows = (rows + BLOCK_SIZE - 1) / BLOCK_SIZE;
        final int blockColumns = (columns + BLOCK_SIZE - 1) / BLOCK_SIZE;

        // convert array
        final Field<T> field = extractField(rawData);
        final T[][] blocks = MathArrays.buildArray(field, blockRows * blockColumns, -1);
        int blockIndex = 0;
        for (int iBlock = 0; iBlock < blockRows; ++iBlock) {
            final int pStart = iBlock * BLOCK_SIZE;
            final int pEnd = MathLib.min(pStart + BLOCK_SIZE, rows);
            final int iHeight = pEnd - pStart;
            for (int jBlock = 0; jBlock < blockColumns; ++jBlock) {
                final int qStart = jBlock * BLOCK_SIZE;
                final int qEnd = MathLib.min(qStart + BLOCK_SIZE, columns);
                final int jWidth = qEnd - qStart;

                // allocate new block
                final T[] block = MathArrays.buildArray(field, iHeight * jWidth);
                blocks[blockIndex] = block;

                // copy data
                int index = 0;
                for (int p = pStart; p < pEnd; ++p) {
                    System.arraycopy(rawData[p], qStart, block, index, jWidth);
                    index += jWidth;
                }

                // Update loop counter
                ++blockIndex;
            }
        }

        // Return result
        return blocks;
    }

    /**
     * Create a data array in blocks layout.
     * <p>
     * This method can be used to create the array argument of the
     * {@link #BlockFieldMatrix(int, int, FieldElement[][], boolean)} constructor.
     * </p>
     * 
     * @param <T>
     *        Type of the field elements.
     * @param field
     *        Field to which the elements belong.
     * @param rows
     *        Number of rows in the new matrix.
     * @param columns
     *        Number of columns in the new matrix.
     * @return a new data array in blocks layout.
     * @see #toBlocksLayout(FieldElement[][])
     * @see #BlockFieldMatrix(int, int, FieldElement[][], boolean)
     */
    public static <T extends FieldElement<T>> T[][] createBlocksLayout(final Field<T> field,
                                                                       final int rows, final int columns) {
        final int blockRows = (rows + BLOCK_SIZE - 1) / BLOCK_SIZE;
        final int blockColumns = (columns + BLOCK_SIZE - 1) / BLOCK_SIZE;

        final T[][] blocks = MathArrays.buildArray(field, blockRows * blockColumns, -1);
        int blockIndex = 0;
        for (int iBlock = 0; iBlock < blockRows; ++iBlock) {
            final int pStart = iBlock * BLOCK_SIZE;
            final int pEnd = MathLib.min(pStart + BLOCK_SIZE, rows);
            final int iHeight = pEnd - pStart;
            for (int jBlock = 0; jBlock < blockColumns; ++jBlock) {
                final int qStart = jBlock * BLOCK_SIZE;
                final int qEnd = MathLib.min(qStart + BLOCK_SIZE, columns);
                final int jWidth = qEnd - qStart;
                blocks[blockIndex] = MathArrays.buildArray(field, iHeight * jWidth);
                ++blockIndex;
            }
        }

        return blocks;
    }

    /** {@inheritDoc} */
    @Override
    public FieldMatrix<T> createMatrix(final int rowDimension,
                                       final int columnDimension) {
        return new BlockFieldMatrix<T>(this.getField(), rowDimension,
            columnDimension);
    }

    /** {@inheritDoc} */
    @Override
    public FieldMatrix<T> copy() {

        // create an empty matrix
        final BlockFieldMatrix<T> copied = new BlockFieldMatrix<T>(this.getField(), this.rows, this.columns);

        // copy the blocks
        for (int i = 0; i < this.blocks.length; ++i) {
            System.arraycopy(this.blocks[i], 0, copied.blocks[i], 0, this.blocks[i].length);
        }

        return copied;
    }

    /** {@inheritDoc} */
    @Override
    public FieldMatrix<T> add(final FieldMatrix<T> m) {
        try {
            return this.add((BlockFieldMatrix<T>) m);
        } catch (final ClassCastException cce) {

            // safety check
            this.checkAdditionCompatible(m);

            final BlockFieldMatrix<T> out = new BlockFieldMatrix<T>(this.getField(), this.rows, this.columns);

            // perform addition block-wise, to ensure good cache behavior
            int blockIndex = 0;
            for (int iBlock = 0; iBlock < out.blockRows; ++iBlock) {
                for (int jBlock = 0; jBlock < out.blockColumns; ++jBlock) {

                    // perform addition on the current block
                    final T[] outBlock = out.blocks[blockIndex];
                    final T[] tBlock = this.blocks[blockIndex];
                    final int pStart = iBlock * BLOCK_SIZE;
                    final int pEnd = MathLib.min(pStart + BLOCK_SIZE, this.rows);
                    final int qStart = jBlock * BLOCK_SIZE;
                    final int qEnd = MathLib.min(qStart + BLOCK_SIZE, this.columns);
                    int k = 0;
                    for (int p = pStart; p < pEnd; ++p) {
                        for (int q = qStart; q < qEnd; ++q) {
                            outBlock[k] = tBlock[k].add(m.getEntry(p, q));
                            ++k;
                        }
                    }

                    // go to next block
                    ++blockIndex;

                }
            }

            return out;
        }
    }

    /**
     * Compute the sum of {@code this} and {@code m}.
     * 
     * @param m
     *        matrix to be added
     * @return {@code this + m}
     * @throws MatrixDimensionMismatchException
     *         if {@code m} is not the same
     *         size as {@code this}
     */
    public BlockFieldMatrix<T> add(final BlockFieldMatrix<T> m) {

        // safety check
        this.checkAdditionCompatible(m);

        final BlockFieldMatrix<T> out = new BlockFieldMatrix<T>(this.getField(), this.rows, this.columns);

        // perform addition block-wise, to ensure good cache behavior
        for (int blockIndex = 0; blockIndex < out.blocks.length; ++blockIndex) {
            final T[] outBlock = out.blocks[blockIndex];
            final T[] tBlock = this.blocks[blockIndex];
            final T[] mBlock = m.blocks[blockIndex];
            for (int k = 0; k < outBlock.length; ++k) {
                outBlock[k] = tBlock[k].add(mBlock[k]);
            }
        }

        return out;
    }

    /** {@inheritDoc} */
    @Override
    public FieldMatrix<T> subtract(final FieldMatrix<T> m) {
        try {
            return this.subtract((BlockFieldMatrix<T>) m);
        } catch (final ClassCastException cce) {

            // safety check
            this.checkSubtractionCompatible(m);

            final BlockFieldMatrix<T> out = new BlockFieldMatrix<T>(this.getField(), this.rows, this.columns);

            // perform subtraction block-wise, to ensure good cache behavior
            int blockIndex = 0;
            for (int iBlock = 0; iBlock < out.blockRows; ++iBlock) {
                for (int jBlock = 0; jBlock < out.blockColumns; ++jBlock) {

                    // perform subtraction on the current block
                    final T[] outBlock = out.blocks[blockIndex];
                    final T[] tBlock = this.blocks[blockIndex];
                    final int pStart = iBlock * BLOCK_SIZE;
                    final int pEnd = MathLib.min(pStart + BLOCK_SIZE, this.rows);
                    final int qStart = jBlock * BLOCK_SIZE;
                    final int qEnd = MathLib.min(qStart + BLOCK_SIZE, this.columns);
                    int k = 0;
                    for (int p = pStart; p < pEnd; ++p) {
                        for (int q = qStart; q < qEnd; ++q) {
                            outBlock[k] = tBlock[k].subtract(m.getEntry(p, q));
                            ++k;
                        }
                    }

                    // go to next block
                    ++blockIndex;

                }
            }

            return out;
        }
    }

    /**
     * Compute {@code this - m}.
     * 
     * @param m
     *        matrix to be subtracted
     * @return {@code this - m}
     * @throws MatrixDimensionMismatchException
     *         if {@code m} is not the same
     *         size as {@code this}
     */
    public BlockFieldMatrix<T> subtract(final BlockFieldMatrix<T> m) {
        // safety check
        this.checkSubtractionCompatible(m);

        final BlockFieldMatrix<T> out = new BlockFieldMatrix<T>(this.getField(), this.rows, this.columns);

        // perform subtraction block-wise, to ensure good cache behavior
        for (int blockIndex = 0; blockIndex < out.blocks.length; ++blockIndex) {
            final T[] outBlock = out.blocks[blockIndex];
            final T[] tBlock = this.blocks[blockIndex];
            final T[] mBlock = m.blocks[blockIndex];
            for (int k = 0; k < outBlock.length; ++k) {
                outBlock[k] = tBlock[k].subtract(mBlock[k]);
            }
        }

        return out;
    }

    /** {@inheritDoc} */
    @Override
    public FieldMatrix<T> scalarAdd(final T d) {
        final BlockFieldMatrix<T> out = new BlockFieldMatrix<T>(this.getField(), this.rows, this.columns);

        // perform subtraction block-wise, to ensure good cache behavior
        for (int blockIndex = 0; blockIndex < out.blocks.length; ++blockIndex) {
            final T[] outBlock = out.blocks[blockIndex];
            final T[] tBlock = this.blocks[blockIndex];
            for (int k = 0; k < outBlock.length; ++k) {
                outBlock[k] = tBlock[k].add(d);
            }
        }

        return out;
    }

    /** {@inheritDoc} */
    @Override
    public FieldMatrix<T> scalarMultiply(final T d) {

        final BlockFieldMatrix<T> out = new BlockFieldMatrix<T>(this.getField(), this.rows, this.columns);

        // perform subtraction block-wise, to ensure good cache behavior
        for (int blockIndex = 0; blockIndex < out.blocks.length; ++blockIndex) {
            final T[] outBlock = out.blocks[blockIndex];
            final T[] tBlock = this.blocks[blockIndex];
            for (int k = 0; k < outBlock.length; ++k) {
                outBlock[k] = tBlock[k].multiply(d);
            }
        }

        return out;
    }

    /** {@inheritDoc} */
    @Override
    public FieldMatrix<T> multiply(final FieldMatrix<T> m) {
        try {
            return this.multiply((BlockFieldMatrix<T>) m);
        } catch (final ClassCastException cce) {

            // safety check
            this.checkMultiplicationCompatible(m);

            final BlockFieldMatrix<T> out = new BlockFieldMatrix<T>(this.getField(), this.rows, m.getColumnDimension());
            final T zero = this.getField().getZero();

            // perform multiplication block-wise, to ensure good cache behavior
            int blockIndex = 0;
            for (int iBlock = 0; iBlock < out.blockRows; ++iBlock) {

                final int pStart = iBlock * BLOCK_SIZE;
                final int pEnd = MathLib.min(pStart + BLOCK_SIZE, this.rows);

                for (int jBlock = 0; jBlock < out.blockColumns; ++jBlock) {

                    final int qStart = jBlock * BLOCK_SIZE;
                    final int qEnd = MathLib.min(qStart + BLOCK_SIZE, m.getColumnDimension());

                    // select current block
                    final T[] outBlock = out.blocks[blockIndex];

                    // perform multiplication on current block
                    for (int kBlock = 0; kBlock < this.blockColumns; ++kBlock) {
                        final int kWidth = this.blockWidth(kBlock);
                        final T[] tBlock = this.blocks[iBlock * this.blockColumns + kBlock];
                        final int rStart = kBlock * BLOCK_SIZE;
                        int k = 0;
                        for (int p = pStart; p < pEnd; ++p) {
                            final int lStart = (p - pStart) * kWidth;
                            final int lEnd = lStart + kWidth;
                            for (int q = qStart; q < qEnd; ++q) {
                                T sum = zero;
                                int r = rStart;
                                // CHECKSTYLE: stop NestedForDepth check
                                // Reason: Commons-Math code kept as such
                                for (int l = lStart; l < lEnd; ++l) {
                                    sum = sum.add(tBlock[l].multiply(m.getEntry(r, q)));
                                    ++r;
                                }
                                // CHECKSTYLE: resume NestedForDepth check
                                outBlock[k] = outBlock[k].add(sum);
                                ++k;
                            }
                        }
                    }

                    // go to next block
                    ++blockIndex;

                }
            }

            return out;
        }
    }

    /**
     * Returns the result of postmultiplying {@code this} by {@code m}.
     * 
     * @param m
     *        matrix to postmultiply by
     * @return {@code this * m}
     * @throws DimensionMismatchException
     *         if the matrices are not compatible.
     */
    public BlockFieldMatrix<T> multiply(final BlockFieldMatrix<T> m) {

        // safety check
        this.checkMultiplicationCompatible(m);

        final BlockFieldMatrix<T> out = new BlockFieldMatrix<T>(this.getField(), this.rows, m.columns);
        final T zero = this.getField().getZero();

        // perform multiplication block-wise, to ensure good cache behavior
        int blockIndex = 0;
        for (int iBlock = 0; iBlock < out.blockRows; ++iBlock) {

            final int pStart = iBlock * BLOCK_SIZE;
            final int pEnd = MathLib.min(pStart + BLOCK_SIZE, this.rows);

            for (int jBlock = 0; jBlock < out.blockColumns; ++jBlock) {
                final int jWidth = out.blockWidth(jBlock);
                final int jWidth2 = jWidth + jWidth;
                final int jWidth3 = jWidth2 + jWidth;
                final int jWidth4 = jWidth3 + jWidth;

                // select current block
                final T[] outBlock = out.blocks[blockIndex];

                // perform multiplication on current block
                for (int kBlock = 0; kBlock < this.blockColumns; ++kBlock) {
                    final int kWidth = this.blockWidth(kBlock);
                    final T[] tBlock = this.blocks[iBlock * this.blockColumns + kBlock];
                    final T[] mBlock = m.blocks[kBlock * m.blockColumns + jBlock];
                    int k = 0;
                    for (int p = pStart; p < pEnd; ++p) {
                        final int lStart = (p - pStart) * kWidth;
                        final int lEnd = lStart + kWidth;
                        for (int nStart = 0; nStart < jWidth; ++nStart) {
                            T sum = zero;
                            int l = lStart;
                            int n = nStart;
                            while (l < lEnd - 3) {
                                sum = sum.
                                    add(tBlock[l].multiply(mBlock[n])).
                                    add(tBlock[l + 1].multiply(mBlock[n + jWidth])).
                                    add(tBlock[l + 2].multiply(mBlock[n + jWidth2])).
                                    add(tBlock[l + 3].multiply(mBlock[n + jWidth3]));
                                l += 4;
                                n += jWidth4;
                            }
                            while (l < lEnd) {
                                sum = sum.add(tBlock[l++].multiply(mBlock[n]));
                                n += jWidth;
                            }
                            outBlock[k] = outBlock[k].add(sum);
                            ++k;
                        }
                    }
                }

                // go to next block
                ++blockIndex;
            }
        }

        return out;
    }

    /** {@inheritDoc} */
    @Override
    public T[][] getData() {

        final T[][] data = MathArrays.buildArray(this.getField(), this.getRowDimension(), this.getColumnDimension());
        final int lastColumns = this.columns - (this.blockColumns - 1) * BLOCK_SIZE;

        for (int iBlock = 0; iBlock < this.blockRows; ++iBlock) {
            final int pStart = iBlock * BLOCK_SIZE;
            final int pEnd = MathLib.min(pStart + BLOCK_SIZE, this.rows);
            int regularPos = 0;
            int lastPos = 0;
            for (int p = pStart; p < pEnd; ++p) {
                final T[] dataP = data[p];
                int blockIndex = iBlock * this.blockColumns;
                int dataPos = 0;
                for (int jBlock = 0; jBlock < this.blockColumns - 1; ++jBlock) {
                    System.arraycopy(this.blocks[blockIndex++], regularPos, dataP, dataPos, BLOCK_SIZE);
                    dataPos += BLOCK_SIZE;
                }
                System.arraycopy(this.blocks[blockIndex], lastPos, dataP, dataPos, lastColumns);
                regularPos += BLOCK_SIZE;
                lastPos += lastColumns;
            }
        }

        return data;
    }

    /** {@inheritDoc} */
    @Override
    public FieldMatrix<T> getSubMatrix(final int startRow, final int endRow,
                                       final int startColumn,
                                       final int endColumn) {
        // safety checks
        this.checkSubMatrixIndex(startRow, endRow, startColumn, endColumn);

        // create the output matrix
        final BlockFieldMatrix<T> out =
            new BlockFieldMatrix<T>(this.getField(), endRow - startRow + 1, endColumn - startColumn + 1);

        // compute blocks shifts
        final int blockStartRow = startRow / BLOCK_SIZE;
        final int rowsShift = startRow % BLOCK_SIZE;
        final int blockStartColumn = startColumn / BLOCK_SIZE;
        final int columnsShift = startColumn % BLOCK_SIZE;

        // perform extraction block-wise, to ensure good cache behavior
        int pBlock = blockStartRow;
        for (int iBlock = 0; iBlock < out.blockRows; ++iBlock) {
            final int iHeight = out.blockHeight(iBlock);
            int qBlock = blockStartColumn;
            for (int jBlock = 0; jBlock < out.blockColumns; ++jBlock) {
                final int jWidth = out.blockWidth(jBlock);

                // handle one block of the output matrix
                final int outIndex = iBlock * out.blockColumns + jBlock;
                final T[] outBlock = out.blocks[outIndex];
                final int index = pBlock * this.blockColumns + qBlock;
                final int width = this.blockWidth(qBlock);

                final int heightExcess = iHeight + rowsShift - BLOCK_SIZE;
                final int widthExcess = jWidth + columnsShift - BLOCK_SIZE;
                if (heightExcess > 0) {
                    // the submatrix block spans on two blocks rows from the original matrix
                    if (widthExcess > 0) {
                        // the submatrix block spans on two blocks columns from the original matrix
                        final int width2 = this.blockWidth(qBlock + 1);
                        this.copyBlockPart(this.blocks[index], width,
                            rowsShift, BLOCK_SIZE,
                            columnsShift, BLOCK_SIZE,
                            outBlock, jWidth, 0, 0);
                        this.copyBlockPart(this.blocks[index + 1], width2,
                            rowsShift, BLOCK_SIZE,
                            0, widthExcess,
                            outBlock, jWidth, 0, jWidth - widthExcess);
                        this.copyBlockPart(this.blocks[index + this.blockColumns], width,
                            0, heightExcess,
                            columnsShift, BLOCK_SIZE,
                            outBlock, jWidth, iHeight - heightExcess, 0);
                        this.copyBlockPart(this.blocks[index + this.blockColumns + 1], width2,
                            0, heightExcess,
                            0, widthExcess,
                            outBlock, jWidth, iHeight - heightExcess, jWidth - widthExcess);
                    } else {
                        // the submatrix block spans on one block column from the original matrix
                        this.copyBlockPart(this.blocks[index], width,
                            rowsShift, BLOCK_SIZE,
                            columnsShift, jWidth + columnsShift,
                            outBlock, jWidth, 0, 0);
                        this.copyBlockPart(this.blocks[index + this.blockColumns], width,
                            0, heightExcess,
                            columnsShift, jWidth + columnsShift,
                            outBlock, jWidth, iHeight - heightExcess, 0);
                    }
                } else {
                    // the submatrix block spans on one block row from the original matrix
                    if (widthExcess > 0) {
                        // the submatrix block spans on two blocks columns from the original matrix
                        final int width2 = this.blockWidth(qBlock + 1);
                        this.copyBlockPart(this.blocks[index], width,
                            rowsShift, iHeight + rowsShift,
                            columnsShift, BLOCK_SIZE,
                            outBlock, jWidth, 0, 0);
                        this.copyBlockPart(this.blocks[index + 1], width2,
                            rowsShift, iHeight + rowsShift,
                            0, widthExcess,
                            outBlock, jWidth, 0, jWidth - widthExcess);
                    } else {
                        // the submatrix block spans on one block column from the original matrix
                        this.copyBlockPart(this.blocks[index], width,
                            rowsShift, iHeight + rowsShift,
                            columnsShift, jWidth + columnsShift,
                            outBlock, jWidth, 0, 0);
                    }
                }
                ++qBlock;
            }
            ++pBlock;
        }

        return out;
    }

    /**
     * Copy a part of a block into another one
     * <p>
     * This method can be called only when the specified part fits in both blocks, no verification is done here.
     * </p>
     * 
     * @param srcBlock
     *        source block
     * @param srcWidth
     *        source block width ({@link #BLOCK_SIZE} or smaller)
     * @param srcStartRow
     *        start row in the source block
     * @param srcEndRow
     *        end row (exclusive) in the source block
     * @param srcStartColumn
     *        start column in the source block
     * @param srcEndColumn
     *        end column (exclusive) in the source block
     * @param dstBlock
     *        destination block
     * @param dstWidth
     *        destination block width ({@link #BLOCK_SIZE} or smaller)
     * @param dstStartRow
     *        start row in the destination block
     * @param dstStartColumn
     *        start column in the destination block
     */
    private void copyBlockPart(final T[] srcBlock, final int srcWidth,
                               final int srcStartRow, final int srcEndRow,
                               final int srcStartColumn, final int srcEndColumn,
                               final T[] dstBlock, final int dstWidth,
                               final int dstStartRow, final int dstStartColumn) {
        final int length = srcEndColumn - srcStartColumn;
        int srcPos = srcStartRow * srcWidth + srcStartColumn;
        int dstPos = dstStartRow * dstWidth + dstStartColumn;
        for (int srcRow = srcStartRow; srcRow < srcEndRow; ++srcRow) {
            System.arraycopy(srcBlock, srcPos, dstBlock, dstPos, length);
            srcPos += srcWidth;
            dstPos += dstWidth;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void setSubMatrix(final T[][] subMatrix, final int row,
                             final int column) {
        // safety checks
        MathUtils.checkNotNull(subMatrix);
        final int refLength = subMatrix[0].length;
        if (refLength == 0) {
            throw new NoDataException(PatriusMessages.AT_LEAST_ONE_COLUMN);
        }
        final int endRow = row + subMatrix.length - 1;
        final int endColumn = column + refLength - 1;
        this.checkSubMatrixIndex(row, endRow, column, endColumn);
        for (final T[] subRow : subMatrix) {
            if (subRow.length != refLength) {
                throw new DimensionMismatchException(refLength, subRow.length);
            }
        }

        // compute blocks bounds
        final int blockStartRow = row / BLOCK_SIZE;
        final int blockEndRow = (endRow + BLOCK_SIZE) / BLOCK_SIZE;
        final int blockStartColumn = column / BLOCK_SIZE;
        final int blockEndColumn = (endColumn + BLOCK_SIZE) / BLOCK_SIZE;

        // perform copy block-wise, to ensure good cache behavior
        for (int iBlock = blockStartRow; iBlock < blockEndRow; ++iBlock) {
            final int iHeight = this.blockHeight(iBlock);
            final int firstRow = iBlock * BLOCK_SIZE;
            final int iStart = MathLib.max(row, firstRow);
            final int iEnd = MathLib.min(endRow + 1, firstRow + iHeight);

            for (int jBlock = blockStartColumn; jBlock < blockEndColumn; ++jBlock) {
                final int jWidth = this.blockWidth(jBlock);
                final int firstColumn = jBlock * BLOCK_SIZE;
                final int jStart = MathLib.max(column, firstColumn);
                final int jEnd = MathLib.min(endColumn + 1, firstColumn + jWidth);
                final int jLength = jEnd - jStart;

                // handle one block, row by row
                final T[] block = this.blocks[iBlock * this.blockColumns + jBlock];
                for (int i = iStart; i < iEnd; ++i) {
                    System.arraycopy(subMatrix[i - row], jStart - column,
                        block, (i - firstRow) * jWidth + (jStart - firstColumn),
                        jLength);
                }

            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public FieldMatrix<T> getRowMatrix(final int row) {
        this.checkRowIndex(row);
        final BlockFieldMatrix<T> out = new BlockFieldMatrix<T>(this.getField(), 1, this.columns);

        // perform copy block-wise, to ensure good cache behavior
        final int iBlock = row / BLOCK_SIZE;
        final int iRow = row - iBlock * BLOCK_SIZE;
        int outBlockIndex = 0;
        int outIndex = 0;
        T[] outBlock = out.blocks[outBlockIndex];
        for (int jBlock = 0; jBlock < this.blockColumns; ++jBlock) {
            final int jWidth = this.blockWidth(jBlock);
            final T[] block = this.blocks[iBlock * this.blockColumns + jBlock];
            final int available = outBlock.length - outIndex;
            if (jWidth > available) {
                System.arraycopy(block, iRow * jWidth, outBlock, outIndex, available);
                outBlock = out.blocks[++outBlockIndex];
                System.arraycopy(block, iRow * jWidth, outBlock, 0, jWidth - available);
                outIndex = jWidth - available;
            } else {
                System.arraycopy(block, iRow * jWidth, outBlock, outIndex, jWidth);
                outIndex += jWidth;
            }
        }

        return out;
    }

    /** {@inheritDoc} */
    @Override
    public void setRowMatrix(final int row, final FieldMatrix<T> matrix) {
        try {
            this.setRowMatrix(row, (BlockFieldMatrix<T>) matrix);
        } catch (final ClassCastException cce) {
            super.setRowMatrix(row, matrix);
        }
    }

    /**
     * Sets the entries in row number <code>row</code> as a row matrix. Row indices start at 0.
     * 
     * @param row
     *        the row to be set
     * @param matrix
     *        row matrix (must have one row and the same number of columns
     *        as the instance)
     * @throws MatrixDimensionMismatchException
     *         if the matrix dimensions do
     *         not match one instance row.
     * @throws OutOfRangeException
     *         if the specified row index is invalid.
     */
    public void setRowMatrix(final int row, final BlockFieldMatrix<T> matrix) {
        this.checkRowIndex(row);
        final int nCols = this.getColumnDimension();
        if ((matrix.getRowDimension() != 1) ||
            (matrix.getColumnDimension() != nCols)) {
            throw new MatrixDimensionMismatchException(matrix.getRowDimension(),
                matrix.getColumnDimension(),
                1, nCols);
        }

        // perform copy block-wise, to ensure good cache behavior
        final int iBlock = row / BLOCK_SIZE;
        final int iRow = row - iBlock * BLOCK_SIZE;
        int mBlockIndex = 0;
        int mIndex = 0;
        T[] mBlock = matrix.blocks[mBlockIndex];
        for (int jBlock = 0; jBlock < this.blockColumns; ++jBlock) {
            final int jWidth = this.blockWidth(jBlock);
            final T[] block = this.blocks[iBlock * this.blockColumns + jBlock];
            final int available = mBlock.length - mIndex;
            if (jWidth > available) {
                System.arraycopy(mBlock, mIndex, block, iRow * jWidth, available);
                mBlock = matrix.blocks[++mBlockIndex];
                System.arraycopy(mBlock, 0, block, iRow * jWidth, jWidth - available);
                mIndex = jWidth - available;
            } else {
                System.arraycopy(mBlock, mIndex, block, iRow * jWidth, jWidth);
                mIndex += jWidth;
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public FieldMatrix<T> getColumnMatrix(final int column) {
        this.checkColumnIndex(column);
        final BlockFieldMatrix<T> out = new BlockFieldMatrix<T>(this.getField(), this.rows, 1);

        // perform copy block-wise, to ensure good cache behavior
        final int jBlock = column / BLOCK_SIZE;
        final int jColumn = column - jBlock * BLOCK_SIZE;
        final int jWidth = this.blockWidth(jBlock);
        int outBlockIndex = 0;
        int outIndex = 0;
        T[] outBlock = out.blocks[outBlockIndex];
        for (int iBlock = 0; iBlock < this.blockRows; ++iBlock) {
            final int iHeight = this.blockHeight(iBlock);
            final T[] block = this.blocks[iBlock * this.blockColumns + jBlock];
            for (int i = 0; i < iHeight; ++i) {
                if (outIndex >= outBlock.length) {
                    outBlock = out.blocks[++outBlockIndex];
                    outIndex = 0;
                }
                outBlock[outIndex++] = block[i * jWidth + jColumn];
            }
        }

        return out;
    }

    /** {@inheritDoc} */
    @Override
    public void setColumnMatrix(final int column, final FieldMatrix<T> matrix) {
        try {
            this.setColumnMatrix(column, (BlockFieldMatrix<T>) matrix);
        } catch (final ClassCastException cce) {
            super.setColumnMatrix(column, matrix);
        }
    }

    /**
     * Sets the entries in column number {@code column} as a column matrix. Column indices start at 0.
     * 
     * @param column
     *        Column to be set.
     * @param matrix
     *        Column matrix (must have one column and the same number of rows
     *        as the instance).
     * @throws MatrixDimensionMismatchException
     *         if the matrix dimensions do
     *         not match one instance column.
     * @throws OutOfRangeException
     *         if the specified column index is invalid.
     */
    public void setColumnMatrix(final int column, final BlockFieldMatrix<T> matrix) {
        this.checkColumnIndex(column);
        final int nRows = this.getRowDimension();
        if ((matrix.getRowDimension() != nRows) ||
            (matrix.getColumnDimension() != 1)) {
            throw new MatrixDimensionMismatchException(matrix.getRowDimension(),
                matrix.getColumnDimension(),
                nRows, 1);
        }

        // perform copy block-wise, to ensure good cache behavior
        final int jBlock = column / BLOCK_SIZE;
        final int jColumn = column - jBlock * BLOCK_SIZE;
        final int jWidth = this.blockWidth(jBlock);
        int mBlockIndex = 0;
        int mIndex = 0;
        T[] mBlock = matrix.blocks[mBlockIndex];
        for (int iBlock = 0; iBlock < this.blockRows; ++iBlock) {
            final int iHeight = this.blockHeight(iBlock);
            final T[] block = this.blocks[iBlock * this.blockColumns + jBlock];
            for (int i = 0; i < iHeight; ++i) {
                if (mIndex >= mBlock.length) {
                    mBlock = matrix.blocks[++mBlockIndex];
                    mIndex = 0;
                }
                block[i * jWidth + jColumn] = mBlock[mIndex++];
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public FieldVector<T> getRowVector(final int row) {
        this.checkRowIndex(row);
        final T[] outData = MathArrays.buildArray(this.getField(), this.columns);

        // perform copy block-wise, to ensure good cache behavior
        final int iBlock = row / BLOCK_SIZE;
        final int iRow = row - iBlock * BLOCK_SIZE;
        int outIndex = 0;
        for (int jBlock = 0; jBlock < this.blockColumns; ++jBlock) {
            final int jWidth = this.blockWidth(jBlock);
            final T[] block = this.blocks[iBlock * this.blockColumns + jBlock];
            System.arraycopy(block, iRow * jWidth, outData, outIndex, jWidth);
            outIndex += jWidth;
        }

        return new ArrayFieldVector<T>(this.getField(), outData, false);
    }

    /** {@inheritDoc} */
    @Override
    public void setRowVector(final int row, final FieldVector<T> vector) {
        try {
            this.setRow(row, ((ArrayFieldVector<T>) vector).getDataRef());
        } catch (final ClassCastException cce) {
            super.setRowVector(row, vector);
        }
    }

    /** {@inheritDoc} */
    @Override
    public FieldVector<T> getColumnVector(final int column) {
        this.checkColumnIndex(column);
        final T[] outData = MathArrays.buildArray(this.getField(), this.rows);

        // perform copy block-wise, to ensure good cache behavior
        final int jBlock = column / BLOCK_SIZE;
        final int jColumn = column - jBlock * BLOCK_SIZE;
        final int jWidth = this.blockWidth(jBlock);
        int outIndex = 0;
        for (int iBlock = 0; iBlock < this.blockRows; ++iBlock) {
            final int iHeight = this.blockHeight(iBlock);
            final T[] block = this.blocks[iBlock * this.blockColumns + jBlock];
            for (int i = 0; i < iHeight; ++i) {
                outData[outIndex++] = block[i * jWidth + jColumn];
            }
        }

        return new ArrayFieldVector<T>(this.getField(), outData, false);
    }

    /** {@inheritDoc} */
    @Override
    public void setColumnVector(final int column, final FieldVector<T> vector) {
        try {
            this.setColumn(column, ((ArrayFieldVector<T>) vector).getDataRef());
        } catch (final ClassCastException cce) {
            super.setColumnVector(column, vector);
        }
    }

    /** {@inheritDoc} */
    @Override
    public T[] getRow(final int row) {
        this.checkRowIndex(row);
        final T[] out = MathArrays.buildArray(this.getField(), this.columns);

        // perform copy block-wise, to ensure good cache behavior
        final int iBlock = row / BLOCK_SIZE;
        final int iRow = row - iBlock * BLOCK_SIZE;
        int outIndex = 0;
        for (int jBlock = 0; jBlock < this.blockColumns; ++jBlock) {
            final int jWidth = this.blockWidth(jBlock);
            final T[] block = this.blocks[iBlock * this.blockColumns + jBlock];
            System.arraycopy(block, iRow * jWidth, out, outIndex, jWidth);
            outIndex += jWidth;
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

        // perform copy block-wise, to ensure good cache behavior
        final int iBlock = row / BLOCK_SIZE;
        final int iRow = row - iBlock * BLOCK_SIZE;
        int outIndex = 0;
        for (int jBlock = 0; jBlock < this.blockColumns; ++jBlock) {
            final int jWidth = this.blockWidth(jBlock);
            final T[] block = this.blocks[iBlock * this.blockColumns + jBlock];
            System.arraycopy(array, outIndex, block, iRow * jWidth, jWidth);
            outIndex += jWidth;
        }
    }

    /** {@inheritDoc} */
    @Override
    public T[] getColumn(final int column) {
        this.checkColumnIndex(column);
        final T[] out = MathArrays.buildArray(this.getField(), this.rows);

        // perform copy block-wise, to ensure good cache behavior
        final int jBlock = column / BLOCK_SIZE;
        final int jColumn = column - jBlock * BLOCK_SIZE;
        final int jWidth = this.blockWidth(jBlock);
        int outIndex = 0;
        for (int iBlock = 0; iBlock < this.blockRows; ++iBlock) {
            final int iHeight = this.blockHeight(iBlock);
            final T[] block = this.blocks[iBlock * this.blockColumns + jBlock];
            for (int i = 0; i < iHeight; ++i) {
                out[outIndex++] = block[i * jWidth + jColumn];
            }
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

        // perform copy block-wise, to ensure good cache behavior
        final int jBlock = column / BLOCK_SIZE;
        final int jColumn = column - jBlock * BLOCK_SIZE;
        final int jWidth = this.blockWidth(jBlock);
        int outIndex = 0;
        for (int iBlock = 0; iBlock < this.blockRows; ++iBlock) {
            final int iHeight = this.blockHeight(iBlock);
            final T[] block = this.blocks[iBlock * this.blockColumns + jBlock];
            for (int i = 0; i < iHeight; ++i) {
                block[i * jWidth + jColumn] = array[outIndex++];
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public T getEntry(final int row, final int column) {
        this.checkRowIndex(row);
        this.checkColumnIndex(column);

        final int iBlock = row / BLOCK_SIZE;
        final int jBlock = column / BLOCK_SIZE;
        final int k = (row - iBlock * BLOCK_SIZE) * this.blockWidth(jBlock) +
            (column - jBlock * BLOCK_SIZE);

        return this.blocks[iBlock * this.blockColumns + jBlock][k];
    }

    /** {@inheritDoc} */
    @Override
    public void setEntry(final int row, final int column, final T value) {
        this.checkRowIndex(row);
        this.checkColumnIndex(column);

        final int iBlock = row / BLOCK_SIZE;
        final int jBlock = column / BLOCK_SIZE;
        final int k = (row - iBlock * BLOCK_SIZE) * this.blockWidth(jBlock) +
            (column - jBlock * BLOCK_SIZE);

        this.blocks[iBlock * this.blockColumns + jBlock][k] = value;
    }

    /** {@inheritDoc} */
    @Override
    public void addToEntry(final int row, final int column, final T increment) {
        this.checkRowIndex(row);
        this.checkColumnIndex(column);

        final int iBlock = row / BLOCK_SIZE;
        final int jBlock = column / BLOCK_SIZE;
        final int k = (row - iBlock * BLOCK_SIZE) * this.blockWidth(jBlock) +
            (column - jBlock * BLOCK_SIZE);
        final T[] blockIJ = this.blocks[iBlock * this.blockColumns + jBlock];

        blockIJ[k] = blockIJ[k].add(increment);
    }

    /** {@inheritDoc} */
    @Override
    public void multiplyEntry(final int row, final int column, final T factor) {
        this.checkRowIndex(row);
        this.checkColumnIndex(column);

        final int iBlock = row / BLOCK_SIZE;
        final int jBlock = column / BLOCK_SIZE;
        final int k = (row - iBlock * BLOCK_SIZE) * this.blockWidth(jBlock) +
            (column - jBlock * BLOCK_SIZE);
        final T[] blockIJ = this.blocks[iBlock * this.blockColumns + jBlock];

        blockIJ[k] = blockIJ[k].multiply(factor);
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.AvoidArrayLoops")
    public FieldMatrix<T> transpose() {
        final int nRows = this.getRowDimension();
        final int nCols = this.getColumnDimension();
        final BlockFieldMatrix<T> out = new BlockFieldMatrix<T>(this.getField(), nCols, nRows);

        // perform transpose block-wise, to ensure good cache behavior
        int blockIndex = 0;
        for (int iBlock = 0; iBlock < this.blockColumns; ++iBlock) {
            for (int jBlock = 0; jBlock < this.blockRows; ++jBlock) {

                // transpose current block
                final T[] outBlock = out.blocks[blockIndex];
                final T[] tBlock = this.blocks[jBlock * this.blockColumns + iBlock];
                final int pStart = iBlock * BLOCK_SIZE;
                final int pEnd = MathLib.min(pStart + BLOCK_SIZE, this.columns);
                final int qStart = jBlock * BLOCK_SIZE;
                final int qEnd = MathLib.min(qStart + BLOCK_SIZE, this.rows);
                int k = 0;
                for (int p = pStart; p < pEnd; ++p) {
                    final int lInc = pEnd - pStart;
                    int l = p - pStart;
                    for (int q = qStart; q < qEnd; ++q) {
                        outBlock[k] = tBlock[l];
                        ++k;
                        l += lInc;
                    }
                }

                // go to next block
                ++blockIndex;

            }
        }

        return out;
    }

    /** {@inheritDoc} */
    @Override
    public int getRowDimension() {
        return this.rows;
    }

    /** {@inheritDoc} */
    @Override
    public int getColumnDimension() {
        return this.columns;
    }

    /** {@inheritDoc} */
    @Override
    public T[] operate(final T[] v) {
        if (v.length != this.columns) {
            throw new DimensionMismatchException(v.length, this.columns);
        }
        final T[] out = MathArrays.buildArray(this.getField(), this.rows);
        final T zero = this.getField().getZero();

        // perform multiplication block-wise, to ensure good cache behavior
        for (int iBlock = 0; iBlock < this.blockRows; ++iBlock) {
            final int pStart = iBlock * BLOCK_SIZE;
            final int pEnd = MathLib.min(pStart + BLOCK_SIZE, this.rows);
            for (int jBlock = 0; jBlock < this.blockColumns; ++jBlock) {
                final T[] block = this.blocks[iBlock * this.blockColumns + jBlock];
                final int qStart = jBlock * BLOCK_SIZE;
                final int qEnd = MathLib.min(qStart + BLOCK_SIZE, this.columns);
                int k = 0;
                for (int p = pStart; p < pEnd; ++p) {
                    T sum = zero;
                    int q = qStart;
                    while (q < qEnd - 3) {
                        sum = sum.
                            add(block[k].multiply(v[q])).
                            add(block[k + 1].multiply(v[q + 1])).
                            add(block[k + 2].multiply(v[q + 2])).
                            add(block[k + 3].multiply(v[q + 3]));
                        k += 4;
                        q += 4;
                    }
                    while (q < qEnd) {
                        sum = sum.add(block[k++].multiply(v[q++]));
                    }
                    out[p] = out[p].add(sum);
                }
            }
        }

        return out;
    }

    /** {@inheritDoc} */
    @Override
    public T[] preMultiply(final T[] v) {

        if (v.length != this.rows) {
            throw new DimensionMismatchException(v.length, this.rows);
        }
        final T[] out = MathArrays.buildArray(this.getField(), this.columns);
        final T zero = this.getField().getZero();

        // perform multiplication block-wise, to ensure good cache behavior
        for (int jBlock = 0; jBlock < this.blockColumns; ++jBlock) {
            final int jWidth = this.blockWidth(jBlock);
            final int jWidth2 = jWidth + jWidth;
            final int jWidth3 = jWidth2 + jWidth;
            final int jWidth4 = jWidth3 + jWidth;
            final int qStart = jBlock * BLOCK_SIZE;
            final int qEnd = MathLib.min(qStart + BLOCK_SIZE, this.columns);
            for (int iBlock = 0; iBlock < this.blockRows; ++iBlock) {
                final T[] block = this.blocks[iBlock * this.blockColumns + jBlock];
                final int pStart = iBlock * BLOCK_SIZE;
                final int pEnd = MathLib.min(pStart + BLOCK_SIZE, this.rows);
                for (int q = qStart; q < qEnd; ++q) {
                    int k = q - qStart;
                    T sum = zero;
                    int p = pStart;
                    while (p < pEnd - 3) {
                        sum = sum.
                            add(block[k].multiply(v[p])).
                            add(block[k + jWidth].multiply(v[p + 1])).
                            add(block[k + jWidth2].multiply(v[p + 2])).
                            add(block[k + jWidth3].multiply(v[p + 3]));
                        k += jWidth4;
                        p += 4;
                    }
                    while (p < pEnd) {
                        sum = sum.add(block[k].multiply(v[p++]));
                        k += jWidth;
                    }
                    out[q] = out[q].add(sum);
                }
            }
        }

        return out;
    }

    /** {@inheritDoc} */
    @Override
    public T walkInRowOrder(final FieldMatrixChangingVisitor<T> visitor) {
        visitor.start(this.rows, this.columns, 0, this.rows - 1, 0, this.columns - 1);
        for (int iBlock = 0; iBlock < this.blockRows; ++iBlock) {
            final int pStart = iBlock * BLOCK_SIZE;
            final int pEnd = MathLib.min(pStart + BLOCK_SIZE, this.rows);
            for (int p = pStart; p < pEnd; ++p) {
                for (int jBlock = 0; jBlock < this.blockColumns; ++jBlock) {
                    final int jWidth = this.blockWidth(jBlock);
                    final int qStart = jBlock * BLOCK_SIZE;
                    final int qEnd = MathLib.min(qStart + BLOCK_SIZE, this.columns);
                    final T[] block = this.blocks[iBlock * this.blockColumns + jBlock];
                    int k = (p - pStart) * jWidth;
                    for (int q = qStart; q < qEnd; ++q) {
                        block[k] = visitor.visit(p, q, block[k]);
                        ++k;
                    }
                }
            }
        }
        return visitor.end();
    }

    /** {@inheritDoc} */
    @Override
    public T walkInRowOrder(final FieldMatrixPreservingVisitor<T> visitor) {
        visitor.start(this.rows, this.columns, 0, this.rows - 1, 0, this.columns - 1);
        for (int iBlock = 0; iBlock < this.blockRows; ++iBlock) {
            final int pStart = iBlock * BLOCK_SIZE;
            final int pEnd = MathLib.min(pStart + BLOCK_SIZE, this.rows);
            for (int p = pStart; p < pEnd; ++p) {
                for (int jBlock = 0; jBlock < this.blockColumns; ++jBlock) {
                    final int jWidth = this.blockWidth(jBlock);
                    final int qStart = jBlock * BLOCK_SIZE;
                    final int qEnd = MathLib.min(qStart + BLOCK_SIZE, this.columns);
                    final T[] block = this.blocks[iBlock * this.blockColumns + jBlock];
                    int k = (p - pStart) * jWidth;
                    for (int q = qStart; q < qEnd; ++q) {
                        visitor.visit(p, q, block[k]);
                        ++k;
                    }
                }
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
        visitor.start(this.rows, this.columns, startRow, endRow, startColumn, endColumn);
        for (int iBlock = startRow / BLOCK_SIZE; iBlock < 1 + endRow / BLOCK_SIZE; ++iBlock) {
            final int p0 = iBlock * BLOCK_SIZE;
            final int pStart = MathLib.max(startRow, p0);
            final int pEnd = MathLib.min((iBlock + 1) * BLOCK_SIZE, 1 + endRow);
            for (int p = pStart; p < pEnd; ++p) {
                for (int jBlock = startColumn / BLOCK_SIZE; jBlock < 1 + endColumn / BLOCK_SIZE; ++jBlock) {
                    final int jWidth = this.blockWidth(jBlock);
                    final int q0 = jBlock * BLOCK_SIZE;
                    final int qStart = MathLib.max(startColumn, q0);
                    final int qEnd = MathLib.min((jBlock + 1) * BLOCK_SIZE, 1 + endColumn);
                    final T[] block = this.blocks[iBlock * this.blockColumns + jBlock];
                    int k = (p - p0) * jWidth + qStart - q0;
                    for (int q = qStart; q < qEnd; ++q) {
                        block[k] = visitor.visit(p, q, block[k]);
                        ++k;
                    }
                }
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
        visitor.start(this.rows, this.columns, startRow, endRow, startColumn, endColumn);
        for (int iBlock = startRow / BLOCK_SIZE; iBlock < 1 + endRow / BLOCK_SIZE; ++iBlock) {
            final int p0 = iBlock * BLOCK_SIZE;
            final int pStart = MathLib.max(startRow, p0);
            final int pEnd = MathLib.min((iBlock + 1) * BLOCK_SIZE, 1 + endRow);
            for (int p = pStart; p < pEnd; ++p) {
                for (int jBlock = startColumn / BLOCK_SIZE; jBlock < 1 + endColumn / BLOCK_SIZE; ++jBlock) {
                    final int jWidth = this.blockWidth(jBlock);
                    final int q0 = jBlock * BLOCK_SIZE;
                    final int qStart = MathLib.max(startColumn, q0);
                    final int qEnd = MathLib.min((jBlock + 1) * BLOCK_SIZE, 1 + endColumn);
                    final T[] block = this.blocks[iBlock * this.blockColumns + jBlock];
                    int k = (p - p0) * jWidth + qStart - q0;
                    for (int q = qStart; q < qEnd; ++q) {
                        visitor.visit(p, q, block[k]);
                        ++k;
                    }
                }
            }
        }
        return visitor.end();
    }

    /** {@inheritDoc} */
    @Override
    public T walkInOptimizedOrder(final FieldMatrixChangingVisitor<T> visitor) {
        visitor.start(this.rows, this.columns, 0, this.rows - 1, 0, this.columns - 1);
        int blockIndex = 0;
        for (int iBlock = 0; iBlock < this.blockRows; ++iBlock) {
            final int pStart = iBlock * BLOCK_SIZE;
            final int pEnd = MathLib.min(pStart + BLOCK_SIZE, this.rows);
            for (int jBlock = 0; jBlock < this.blockColumns; ++jBlock) {
                final int qStart = jBlock * BLOCK_SIZE;
                final int qEnd = MathLib.min(qStart + BLOCK_SIZE, this.columns);
                final T[] block = this.blocks[blockIndex];
                int k = 0;
                for (int p = pStart; p < pEnd; ++p) {
                    for (int q = qStart; q < qEnd; ++q) {
                        block[k] = visitor.visit(p, q, block[k]);
                        ++k;
                    }
                }
                ++blockIndex;
            }
        }
        return visitor.end();
    }

    /** {@inheritDoc} */
    @Override
    public T walkInOptimizedOrder(final FieldMatrixPreservingVisitor<T> visitor) {
        visitor.start(this.rows, this.columns, 0, this.rows - 1, 0, this.columns - 1);
        int blockIndex = 0;
        for (int iBlock = 0; iBlock < this.blockRows; ++iBlock) {
            final int pStart = iBlock * BLOCK_SIZE;
            final int pEnd = MathLib.min(pStart + BLOCK_SIZE, this.rows);
            for (int jBlock = 0; jBlock < this.blockColumns; ++jBlock) {
                final int qStart = jBlock * BLOCK_SIZE;
                final int qEnd = MathLib.min(qStart + BLOCK_SIZE, this.columns);
                final T[] block = this.blocks[blockIndex];
                int k = 0;
                for (int p = pStart; p < pEnd; ++p) {
                    for (int q = qStart; q < qEnd; ++q) {
                        visitor.visit(p, q, block[k]);
                        ++k;
                    }
                }
                ++blockIndex;
            }
        }
        return visitor.end();
    }

    /** {@inheritDoc} */
    @Override
    public T walkInOptimizedOrder(final FieldMatrixChangingVisitor<T> visitor,
                                  final int startRow, final int endRow,
                                  final int startColumn, final int endColumn) {
        this.checkSubMatrixIndex(startRow, endRow, startColumn, endColumn);
        visitor.start(this.rows, this.columns, startRow, endRow, startColumn, endColumn);
        for (int iBlock = startRow / BLOCK_SIZE; iBlock < 1 + endRow / BLOCK_SIZE; ++iBlock) {
            final int p0 = iBlock * BLOCK_SIZE;
            final int pStart = MathLib.max(startRow, p0);
            final int pEnd = MathLib.min((iBlock + 1) * BLOCK_SIZE, 1 + endRow);
            for (int jBlock = startColumn / BLOCK_SIZE; jBlock < 1 + endColumn / BLOCK_SIZE; ++jBlock) {
                final int jWidth = this.blockWidth(jBlock);
                final int q0 = jBlock * BLOCK_SIZE;
                final int qStart = MathLib.max(startColumn, q0);
                final int qEnd = MathLib.min((jBlock + 1) * BLOCK_SIZE, 1 + endColumn);
                final T[] block = this.blocks[iBlock * this.blockColumns + jBlock];
                for (int p = pStart; p < pEnd; ++p) {
                    int k = (p - p0) * jWidth + qStart - q0;
                    for (int q = qStart; q < qEnd; ++q) {
                        block[k] = visitor.visit(p, q, block[k]);
                        ++k;
                    }
                }
            }
        }
        return visitor.end();
    }

    /** {@inheritDoc} */
    @Override
    public T walkInOptimizedOrder(final FieldMatrixPreservingVisitor<T> visitor,
                                  final int startRow, final int endRow,
                                  final int startColumn, final int endColumn) {
        this.checkSubMatrixIndex(startRow, endRow, startColumn, endColumn);
        visitor.start(this.rows, this.columns, startRow, endRow, startColumn, endColumn);
        for (int iBlock = startRow / BLOCK_SIZE; iBlock < 1 + endRow / BLOCK_SIZE; ++iBlock) {
            final int p0 = iBlock * BLOCK_SIZE;
            final int pStart = MathLib.max(startRow, p0);
            final int pEnd = MathLib.min((iBlock + 1) * BLOCK_SIZE, 1 + endRow);
            for (int jBlock = startColumn / BLOCK_SIZE; jBlock < 1 + endColumn / BLOCK_SIZE; ++jBlock) {
                final int jWidth = this.blockWidth(jBlock);
                final int q0 = jBlock * BLOCK_SIZE;
                final int qStart = MathLib.max(startColumn, q0);
                final int qEnd = MathLib.min((jBlock + 1) * BLOCK_SIZE, 1 + endColumn);
                final T[] block = this.blocks[iBlock * this.blockColumns + jBlock];
                for (int p = pStart; p < pEnd; ++p) {
                    int k = (p - p0) * jWidth + qStart - q0;
                    for (int q = qStart; q < qEnd; ++q) {
                        visitor.visit(p, q, block[k]);
                        ++k;
                    }
                }
            }
        }
        return visitor.end();
    }

    /**
     * Get the height of a block.
     * 
     * @param blockRow
     *        row index (in block sense) of the block
     * @return height (number of rows) of the block
     */
    private int blockHeight(final int blockRow) {
        return (blockRow == this.blockRows - 1) ? this.rows - blockRow * BLOCK_SIZE : BLOCK_SIZE;
    }

    /**
     * Get the width of a block.
     * 
     * @param blockColumn
     *        column index (in block sense) of the block
     * @return width (number of columns) of the block
     */
    private int blockWidth(final int blockColumn) {
        return (blockColumn == this.blockColumns - 1) ? this.columns - blockColumn * BLOCK_SIZE : BLOCK_SIZE;
    }

    // CHECKSTYLE: resume CommentRatio check
}
