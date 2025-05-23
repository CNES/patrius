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
 * 
 * HISTORY
* VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.7:DM:DM-2766:18/05/2021:Evol. et corr. dans le package fr.cnes.sirius.patrius.math.linear (suite DM 2300) 
 * VERSION:4.5:DM:DM-2300:27/05/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.linear;

import java.util.Arrays;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.NoDataException;
import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

//CHECKSTYLE: stop CommentRatio check
//Reason: model - Commons-Math code kept as such

/**
 * Cache-friendly implementation of RealMatrix using a flat arrays to store
 * square blocks of the matrix.
 * <p>
 * This implementation is specially designed to be cache-friendly. Square blocks are stored as small arrays and allow
 * efficient traversal of data both in row major direction and columns major direction, one block at a time. This
 * greatly increases performances for algorithms that use crossed directions loops like multiplication or transposition.
 * </p>
 * <p>
 * The size of square blocks is a static parameter. It may be tuned according to the cache size of the target computer
 * processor. As a rule of thumbs, it should be the largest value that allows three blocks to be simultaneously cached
 * (this is necessary for example for matrix multiplication). The default value is to use 52x52 blocks which is well
 * suited for processors with 64k L1 cache (one block holds 2704 values or 21632 bytes). This value could be lowered to
 * 36x36 for processors with 32k L1 cache.
 * </p>
 * <p>
 * The regular blocks represent {@link #BLOCK_SIZE} x {@link #BLOCK_SIZE} squares. Blocks at right hand side and bottom
 * side which may be smaller to fit matrix dimensions. The square blocks are flattened in row major order in single
 * dimension arrays which are therefore {@link #BLOCK_SIZE}<sup>2</sup> elements long for regular blocks. The blocks are
 * themselves organized in row major order.
 * </p>
 * <p>
 * As an example, for a block size of 52x52, a 100x60 matrix would be stored in 4 blocks. Block 0 would be a
 * double[2704] array holding the upper left 52x52 square, block 1 would be a double[416] array holding the upper right
 * 52x8 rectangle, block 2 would be a double[2496] array holding the lower left 48x52 rectangle and block 3 would be a
 * double[384] array holding the lower right 48x8 rectangle.
 * </p>
 * <p>
 * The layout complexity overhead versus simple mapping of matrices to java arrays is negligible for small matrices
 * (about 1%). The gain from cache efficiency leads to up to 3-fold improvements for matrices of moderate to large size.
 * </p>
 * 
 * @version $Id: BlockRealMatrix.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 2.0
 */
public class BlockRealMatrix extends AbstractRealMatrix {
    /** Block size. */
    public static final int BLOCK_SIZE = 52;
     /** Serializable UID. */
    private static final long serialVersionUID = 4991895511313664478L;
    /** Blocks of matrix entries. */
    private final double[][] blocks;
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
     * @param rowsIn
     *        the number of rows in the new matrix
     * @param columnsIn
     *        the number of columns in the new matrix
     * @throws NotStrictlyPositiveException
     *         if row or column dimension is not
     *         positive.
     */
    public BlockRealMatrix(final int rowsIn, final int columnsIn) {
        super(rowsIn, columnsIn);
        this.rows = rowsIn;
        this.columns = columnsIn;

        // number of blocks
        this.blockRows = (rowsIn + BLOCK_SIZE - 1) / BLOCK_SIZE;
        this.blockColumns = (columnsIn + BLOCK_SIZE - 1) / BLOCK_SIZE;

        // allocate storage blocks, taking care of smaller ones at right and bottom
        this.blocks = createBlocksLayout(rowsIn, columnsIn);
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
     * matrix = new BlockRealMatrix(rawData.length, rawData[0].length,
     *     toBlocksLayout(rawData), false);
     * </pre>
     * 
     * </p>
     * 
     * @param rawData
     *        data for new matrix, in raw layout
     * @throws DimensionMismatchException
     *         if the shape of {@code blockData} is
     *         inconsistent with block layout.
     * @throws NotStrictlyPositiveException
     *         if row or column dimension is not
     *         positive.
     * @see #BlockRealMatrix(int, int, double[][], boolean)
     */
    public BlockRealMatrix(final double[][] rawData) {
        this(rawData.length, rawData[0].length, toBlocksLayout(rawData), false);
    }

    /**
     * Create a new dense matrix copying entries from block layout data.
     * <p>
     * The input array <em>must</em> already be in blocks layout.
     * </p>
     * 
     * @param rowsIn
     *        Number of rows in the new matrix.
     * @param columnsIn
     *        Number of columns in the new matrix.
     * @param blockData
     *        data for new matrix
     * @param copyArray
     *        Whether the input array will be copied or referenced.
     * @throws DimensionMismatchException
     *         if the shape of {@code blockData} is
     *         inconsistent with block layout.
     * @throws NotStrictlyPositiveException
     *         if row or column dimension is not
     *         positive.
     * @see #createBlocksLayout(int, int)
     * @see #toBlocksLayout(double[][])
     * @see #BlockRealMatrix(double[][])
     */
    public BlockRealMatrix(final int rowsIn, final int columnsIn,
        final double[][] blockData, final boolean copyArray) {
        super(rowsIn, columnsIn);
        this.rows = rowsIn;
        this.columns = columnsIn;

        // number of blocks
        this.blockRows = (rowsIn + BLOCK_SIZE - 1) / BLOCK_SIZE;
        this.blockColumns = (columnsIn + BLOCK_SIZE - 1) / BLOCK_SIZE;

        if (copyArray) {
            // allocate storage blocks, taking care of smaller ones at right and bottom
            this.blocks = new double[this.blockRows * this.blockColumns][];
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
     * <code>rawData[i][j]</code>. Blocks layout is the layout used in {@link BlockRealMatrix} instances, where the
     * matrix is split in square blocks (except at right and bottom side where blocks may be rectangular to fit matrix
     * size) and each block is stored in a flattened one-dimensional array.
     * </p>
     * <p>
     * This method creates an array in blocks layout from an input array in raw layout. It can be used to provide the
     * array argument of the {@link #BlockRealMatrix(int, int, double[][], boolean)} constructor.
     * </p>
     * 
     * @param rawData
     *        Data array in raw layout.
     * @return a new data array containing the same entries but in blocks layout.
     * @throws DimensionMismatchException
     *         if {@code rawData} is not rectangular.
     * @see #createBlocksLayout(int, int)
     * @see #BlockRealMatrix(int, int, double[][], boolean)
     */
    public static double[][] toBlocksLayout(final double[][] rawData) {
        // Initialization
        final int columns = rawData[0].length;

        // safety checks
        for (final double[] element : rawData) {
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
        final double[][] blocks = new double[blockRows * blockColumns][];
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
                final double[] block = new double[iHeight * jWidth];
                blocks[blockIndex] = block;

                // copy data
                int index = 0;
                for (int p = pStart; p < pEnd; ++p) {
                    System.arraycopy(rawData[p], qStart, block, index, jWidth);
                    index += jWidth;
                }
                
                // Increment loop counter
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
     * {@link #BlockRealMatrix(int, int, double[][], boolean)} constructor.
     * </p>
     * 
     * @param rows
     *        Number of rows in the new matrix.
     * @param columns
     *        Number of columns in the new matrix.
     * @return a new data array in blocks layout.
     * @see #toBlocksLayout(double[][])
     * @see #BlockRealMatrix(int, int, double[][], boolean)
     */
    public static double[][] createBlocksLayout(final int rows, final int columns) {
        final int blockRows = (rows + BLOCK_SIZE - 1) / BLOCK_SIZE;
        final int blockColumns = (columns + BLOCK_SIZE - 1) / BLOCK_SIZE;

        final double[][] blocks = new double[blockRows * blockColumns][];
        int blockIndex = 0;
        for (int iBlock = 0; iBlock < blockRows; ++iBlock) {
            final int pStart = iBlock * BLOCK_SIZE;
            final int pEnd = MathLib.min(pStart + BLOCK_SIZE, rows);
            final int iHeight = pEnd - pStart;
            for (int jBlock = 0; jBlock < blockColumns; ++jBlock) {
                final int qStart = jBlock * BLOCK_SIZE;
                final int qEnd = MathLib.min(qStart + BLOCK_SIZE, columns);
                final int jWidth = qEnd - qStart;
                blocks[blockIndex] = new double[iHeight * jWidth];
                ++blockIndex;
            }
        }

        return blocks;
    }

    /** {@inheritDoc} */
    @Override
    public BlockRealMatrix createMatrix(final int rowDimension,
                                        final int columnDimension) {
        return new BlockRealMatrix(rowDimension, columnDimension);
    }

    /** {@inheritDoc} */
    @Override
    public BlockRealMatrix copy() {
        // create an empty matrix
        final BlockRealMatrix copied = new BlockRealMatrix(this.rows, this.columns);

        // copy the blocks
        for (int i = 0; i < this.blocks.length; ++i) {
            System.arraycopy(this.blocks[i], 0, copied.blocks[i], 0, this.blocks[i].length);
        }

        return copied;
    }

    /** {@inheritDoc} */
    @Override
    public BlockRealMatrix add(final RealMatrix m) {

        if (m instanceof BlockRealMatrix) {
            return this.add((BlockRealMatrix) m);
        }

        // safety check
        MatrixUtils.checkAdditionCompatible(this, m);

        final BlockRealMatrix out = new BlockRealMatrix(this.rows, this.columns);

        // perform addition block-wise, to ensure good cache behavior
        int blockIndex = 0;
        for (int iBlock = 0; iBlock < out.blockRows; ++iBlock) {
            for (int jBlock = 0; jBlock < out.blockColumns; ++jBlock) {

                // perform addition on the current block
                final double[] outBlock = out.blocks[blockIndex];
                final double[] tBlock = this.blocks[blockIndex];
                final int pStart = iBlock * BLOCK_SIZE;
                final int pEnd = MathLib.min(pStart + BLOCK_SIZE, this.rows);
                final int qStart = jBlock * BLOCK_SIZE;
                final int qEnd = MathLib.min(qStart + BLOCK_SIZE, this.columns);
                int k = 0;
                for (int p = pStart; p < pEnd; ++p) {
                    for (int q = qStart; q < qEnd; ++q) {
                        outBlock[k] = tBlock[k] + m.getEntry(p, q);
                        ++k;
                    }
                }
                // go to next block
                ++blockIndex;
            }
        }

        return out;
    }

    /**
     * Compute the sum of this matrix and {@code m}.
     * 
     * @param m
     *        Matrix to be added.
     * @return {@code this} + m.
     * @throws MatrixDimensionMismatchException
     *         if {@code m} is not the same
     *         size as this matrix.
     */
    public BlockRealMatrix add(final BlockRealMatrix m) {

        // safety check
        MatrixUtils.checkAdditionCompatible(this, m);

        final BlockRealMatrix out = new BlockRealMatrix(this.rows, this.columns);

        // perform addition block-wise, to ensure good cache behavior
        for (int blockIndex = 0; blockIndex < out.blocks.length; ++blockIndex) {
            final double[] outBlock = out.blocks[blockIndex];
            final double[] tBlock = this.blocks[blockIndex];
            final double[] mBlock = m.blocks[blockIndex];
            for (int k = 0; k < outBlock.length; ++k) {
                outBlock[k] = tBlock[k] + mBlock[k];
            }
        }

        return out;
    }

    /** {@inheritDoc} */
    @Override
    public BlockRealMatrix subtract(final RealMatrix m) {

        if (m instanceof BlockRealMatrix) {
            return this.subtract((BlockRealMatrix) m);
        }

        // safety check
        MatrixUtils.checkSubtractionCompatible(this, m);

        final BlockRealMatrix out = new BlockRealMatrix(this.rows, this.columns);

        // perform subtraction block-wise, to ensure good cache behavior
        int blockIndex = 0;
        for (int iBlock = 0; iBlock < out.blockRows; ++iBlock) {
            for (int jBlock = 0; jBlock < out.blockColumns; ++jBlock) {

                // perform subtraction on the current block
                final double[] outBlock = out.blocks[blockIndex];
                final double[] tBlock = this.blocks[blockIndex];
                final int pStart = iBlock * BLOCK_SIZE;
                final int pEnd = MathLib.min(pStart + BLOCK_SIZE, this.rows);
                final int qStart = jBlock * BLOCK_SIZE;
                final int qEnd = MathLib.min(qStart + BLOCK_SIZE, this.columns);
                int k = 0;
                for (int p = pStart; p < pEnd; ++p) {
                    for (int q = qStart; q < qEnd; ++q) {
                        outBlock[k] = tBlock[k] - m.getEntry(p, q);
                        ++k;
                    }
                }
                // go to next block
                ++blockIndex;
            }
        }

        return out;
    }

    /**
     * Subtract {@code m} from this matrix.
     * 
     * @param m
     *        Matrix to be subtracted.
     * @return {@code this} - m.
     * @throws MatrixDimensionMismatchException
     *         if {@code m} is not the
     *         same size as this matrix.
     */
    public BlockRealMatrix subtract(final BlockRealMatrix m) {

        // safety check
        MatrixUtils.checkSubtractionCompatible(this, m);

        final BlockRealMatrix out = new BlockRealMatrix(this.rows, this.columns);

        // perform subtraction block-wise, to ensure good cache behavior
        for (int blockIndex = 0; blockIndex < out.blocks.length; ++blockIndex) {
            final double[] outBlock = out.blocks[blockIndex];
            final double[] tBlock = this.blocks[blockIndex];
            final double[] mBlock = m.blocks[blockIndex];
            for (int k = 0; k < outBlock.length; ++k) {
                outBlock[k] = tBlock[k] - mBlock[k];
            }
        }

        return out;
    }

    /** {@inheritDoc} */
    @Override
    public BlockRealMatrix scalarAdd(final double d) {

        final BlockRealMatrix out = new BlockRealMatrix(this.rows, this.columns);

        // perform subtraction block-wise, to ensure good cache behavior
        for (int blockIndex = 0; blockIndex < out.blocks.length; ++blockIndex) {
            final double[] outBlock = out.blocks[blockIndex];
            final double[] tBlock = this.blocks[blockIndex];
            for (int k = 0; k < outBlock.length; ++k) {
                outBlock[k] = tBlock[k] + d;
            }
        }

        return out;
    }

    /** {@inheritDoc} */
    @Override
    public BlockRealMatrix scalarMultiply(final double d) {
        final BlockRealMatrix out = new BlockRealMatrix(this.rows, this.columns);

        // perform subtraction block-wise, to ensure good cache behavior
        for (int blockIndex = 0; blockIndex < out.blocks.length; ++blockIndex) {
            final double[] outBlock = out.blocks[blockIndex];
            final double[] tBlock = this.blocks[blockIndex];
            for (int k = 0; k < outBlock.length; ++k) {
                outBlock[k] = tBlock[k] * d;
            }
        }

        return out;
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix multiply(final RealMatrix m, final boolean toTranspose, final double d) {

        final BlockRealMatrix castedM;
        if (m instanceof BlockRealMatrix) {
            castedM = (BlockRealMatrix) m;
        } else {
            castedM = new BlockRealMatrix(m.getData(false));
        }

        return this.multiply(castedM, toTranspose, d);
    }

    /**
     * Returns the result of postmultiplying {@code this} by {@code m}.
     * 
     * @param m
     *        matrix to postmultiply by
     * @return {@code this * m}
     * @throws DimensionMismatchException
     *         if matrices are not multiplication compatible
     */
    public BlockRealMatrix multiply(final BlockRealMatrix m) {
        return this.multiply(m, false, 1.0);
    }

    /**
     * Returns the result of postmultiplying this&nbsp;&times&nbsp;m (if {@code toTranspose = false})
     * or this&nbsp;&times&nbsp;m<sup>T</sup> (if {@code toTranspose = true})
     * 
     * @param m matrix to postmultiply by
     * @param toTranspose indication value, true if we expect the {@code m} matrix to be transposed
     * @return this&nbsp;&times&nbsp;m or this&nbsp;&times&nbsp;m<sup>T</sup>
     * @throws DimensionMismatchException
     *         if matrices are not multiplication compatible
     */
    public BlockRealMatrix multiply(final BlockRealMatrix m, final boolean toTranspose) {
        return this.multiply(m, toTranspose, 1.0);
    }

    /**
     * Returns the result of postmultiplying d&nbsp;&times;&nbsp;this&nbsp;&times&nbsp;m (if {@code toTranspose = false}
     * ) or d&nbsp;&times;&nbsp;this&nbsp;&times&nbsp;m<sup>T</sup> (if {@code toTranspose = true})
     * 
     * @param m matrix to postmultiply by
     * @param toTranspose indication value, true if we expect the {@code m} matrix to be transposed
     * @param d value to multiply all entries by
     * @return d&nbsp;&times;&nbsp;this&nbsp;&times&nbsp;m or
     *         d&nbsp;&times;&nbsp;this&nbsp;&times&nbsp;m<sup>T</sup>
     * @throws DimensionMismatchException
     *         if matrices are not multiplication compatible
     */
    public BlockRealMatrix multiply(final BlockRealMatrix m, final boolean toTranspose, final double d) {

        // safety check
        MatrixUtils.checkMultiplicationCompatible(this, m, toTranspose);

        // No optimization for the transposed scenario
        final BlockRealMatrix mWork;
        if (!toTranspose) {
            mWork = m;
        } else {
            mWork = m.transpose();
        }

        final BlockRealMatrix out = new BlockRealMatrix(this.rows, mWork.columns);

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
                final double[] outBlock = out.blocks[blockIndex];

                // perform multiplication on current block
                for (int kBlock = 0; kBlock < this.blockColumns; ++kBlock) {
                    final int kWidth = this.blockWidth(kBlock);
                    final double[] tBlock = this.blocks[iBlock * this.blockColumns + kBlock];
                    final double[] mBlock = mWork.blocks[kBlock * mWork.blockColumns + jBlock];
                    int k = 0;
                    for (int p = pStart; p < pEnd; ++p) {
                        final int lStart = (p - pStart) * kWidth;
                        final int lEnd = lStart + kWidth;
                        for (int nStart = 0; nStart < jWidth; ++nStart) {
                            double sum = 0;
                            int l = lStart;
                            int n = nStart;
                            while (l < lEnd - 3) {
                                sum += tBlock[l] * mBlock[n] +
                                    tBlock[l + 1] * mBlock[n + jWidth] +
                                    tBlock[l + 2] * mBlock[n + jWidth2] +
                                    tBlock[l + 3] * mBlock[n + jWidth3];
                                l += 4;
                                n += jWidth4;
                            }
                            while (l < lEnd) {
                                sum += tBlock[l++] * mBlock[n];
                                n += jWidth;
                            }
                            outBlock[k] += sum;
                            outBlock[k] *= d;
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
    public double[][] getData() {
        final double[][] data = new double[this.getRowDimension()][this.getColumnDimension()];
        final int lastColumns = this.columns - (this.blockColumns - 1) * BLOCK_SIZE;

        for (int iBlock = 0; iBlock < this.blockRows; ++iBlock) {
            final int pStart = iBlock * BLOCK_SIZE;
            final int pEnd = MathLib.min(pStart + BLOCK_SIZE, this.rows);
            int regularPos = 0;
            int lastPos = 0;
            for (int p = pStart; p < pEnd; ++p) {
                final double[] dataP = data[p];
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
    public double getNorm() {
        final double[] colSums = new double[BLOCK_SIZE];
        double maxColSum = 0;
        for (int jBlock = 0; jBlock < this.blockColumns; jBlock++) {
            final int jWidth = this.blockWidth(jBlock);
            Arrays.fill(colSums, 0, jWidth, 0.0);
            for (int iBlock = 0; iBlock < this.blockRows; ++iBlock) {
                final int iHeight = this.blockHeight(iBlock);
                final double[] block = this.blocks[iBlock * this.blockColumns + jBlock];
                for (int j = 0; j < jWidth; ++j) {
                    double sum = 0;
                    for (int i = 0; i < iHeight; ++i) {
                        sum += MathLib.abs(block[i * jWidth + j]);
                    }
                    colSums[j] += sum;
                }
            }
            for (int j = 0; j < jWidth; ++j) {
                maxColSum = MathLib.max(maxColSum, colSums[j]);
            }
        }
        return maxColSum;
    }

    /** {@inheritDoc} */
    @Override
    public double getFrobeniusNorm() {
        double sum2 = 0;
        for (final double[] block : this.blocks) {
            for (final double entry : block) {
                sum2 += entry * entry;
            }
        }
        return MathLib.sqrt(sum2);
    }

    /** {@inheritDoc} */
    @Override
    public BlockRealMatrix getSubMatrix(final int startRow, final int endRow,
                                        final int startColumn,
                                        final int endColumn) {
        // safety checks
        MatrixUtils.checkSubMatrixIndex(this, startRow, endRow, startColumn, endColumn);

        // create the output matrix
        final BlockRealMatrix out =
            new BlockRealMatrix(endRow - startRow + 1, endColumn - startColumn + 1);

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
                final double[] outBlock = out.blocks[outIndex];
                final int index = pBlock * this.blockColumns + qBlock;
                final int width = this.blockWidth(qBlock);

                final int heightExcess = iHeight + rowsShift - BLOCK_SIZE;
                final int widthExcess = jWidth + columnsShift - BLOCK_SIZE;
                if (heightExcess > 0) {
                    // the submatrix block spans on two blocks rows from the original matrix
                    if (widthExcess > 0) {
                        // the submatrix block spans on two blocks columns from the original matrix
                        final int width2 = this.blockWidth(qBlock + 1);
                        copyBlockPart(this.blocks[index], width,
                            rowsShift, BLOCK_SIZE,
                            columnsShift, BLOCK_SIZE,
                            outBlock, jWidth, 0, 0);
                        copyBlockPart(this.blocks[index + 1], width2,
                            rowsShift, BLOCK_SIZE,
                            0, widthExcess,
                            outBlock, jWidth, 0, jWidth - widthExcess);
                        copyBlockPart(this.blocks[index + this.blockColumns], width,
                            0, heightExcess,
                            columnsShift, BLOCK_SIZE,
                            outBlock, jWidth, iHeight - heightExcess, 0);
                        copyBlockPart(this.blocks[index + this.blockColumns + 1], width2,
                            0, heightExcess,
                            0, widthExcess,
                            outBlock, jWidth, iHeight - heightExcess, jWidth - widthExcess);
                    } else {
                        // the submatrix block spans on one block column from the original matrix
                        copyBlockPart(this.blocks[index], width,
                            rowsShift, BLOCK_SIZE,
                            columnsShift, jWidth + columnsShift,
                            outBlock, jWidth, 0, 0);
                        copyBlockPart(this.blocks[index + this.blockColumns], width,
                            0, heightExcess,
                            columnsShift, jWidth + columnsShift,
                            outBlock, jWidth, iHeight - heightExcess, 0);
                    }
                } else {
                    // the submatrix block spans on one block row from the original matrix
                    if (widthExcess > 0) {
                        // the submatrix block spans on two blocks columns from the original matrix
                        final int width2 = this.blockWidth(qBlock + 1);
                        copyBlockPart(this.blocks[index], width,
                            rowsShift, iHeight + rowsShift,
                            columnsShift, BLOCK_SIZE,
                            outBlock, jWidth, 0, 0);
                        copyBlockPart(this.blocks[index + 1], width2,
                            rowsShift, iHeight + rowsShift,
                            0, widthExcess,
                            outBlock, jWidth, 0, jWidth - widthExcess);
                    } else {
                        // the submatrix block spans on one block column from the original matrix
                        copyBlockPart(this.blocks[index], width,
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
    private static void copyBlockPart(final double[] srcBlock, final int srcWidth,
                               final int srcStartRow, final int srcEndRow,
                               final int srcStartColumn, final int srcEndColumn,
                               final double[] dstBlock, final int dstWidth,
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
    public void setSubMatrix(final double[][] subMatrix, final int row,
                             final int column) {
        // safety checks
        MathUtils.checkNotNull(subMatrix);
        final int refLength = subMatrix[0].length;
        if (refLength == 0) {
            throw new NoDataException(PatriusMessages.AT_LEAST_ONE_COLUMN);
        }
        final int endRow = row + subMatrix.length - 1;
        final int endColumn = column + refLength - 1;
        MatrixUtils.checkSubMatrixIndex(this, row, endRow, column, endColumn);
        for (final double[] subRow : subMatrix) {
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
                final double[] block = this.blocks[iBlock * this.blockColumns + jBlock];
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
    public BlockRealMatrix getRowMatrix(final int row) {
        MatrixUtils.checkRowIndex(this, row);
        final BlockRealMatrix out = new BlockRealMatrix(1, this.columns);

        // perform copy block-wise, to ensure good cache behavior
        final int iBlock = row / BLOCK_SIZE;
        final int iRow = row - iBlock * BLOCK_SIZE;
        int outBlockIndex = 0;
        int outIndex = 0;
        double[] outBlock = out.blocks[outBlockIndex];
        for (int jBlock = 0; jBlock < this.blockColumns; ++jBlock) {
            final int jWidth = this.blockWidth(jBlock);
            final double[] block = this.blocks[iBlock * this.blockColumns + jBlock];
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
    public void setRowMatrix(final int row, final RealMatrix matrix) {
        try {
            this.setRowMatrix(row, (BlockRealMatrix) matrix);
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
     * @throws OutOfRangeException
     *         if the specified row index is invalid.
     * @throws MatrixDimensionMismatchException
     *         if the matrix dimensions do
     *         not match one instance row.
     */
    public void setRowMatrix(final int row, final BlockRealMatrix matrix) {
        MatrixUtils.checkRowIndex(this, row);
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
        double[] mBlock = matrix.blocks[mBlockIndex];
        for (int jBlock = 0; jBlock < this.blockColumns; ++jBlock) {
            final int jWidth = this.blockWidth(jBlock);
            final double[] block = this.blocks[iBlock * this.blockColumns + jBlock];
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
    public BlockRealMatrix getColumnMatrix(final int column) {
        MatrixUtils.checkColumnIndex(this, column);
        final BlockRealMatrix out = new BlockRealMatrix(this.rows, 1);

        // perform copy block-wise, to ensure good cache behavior
        final int jBlock = column / BLOCK_SIZE;
        final int jColumn = column - jBlock * BLOCK_SIZE;
        final int jWidth = this.blockWidth(jBlock);
        int outBlockIndex = 0;
        int outIndex = 0;
        double[] outBlock = out.blocks[outBlockIndex];
        for (int iBlock = 0; iBlock < this.blockRows; ++iBlock) {
            final int iHeight = this.blockHeight(iBlock);
            final double[] block = this.blocks[iBlock * this.blockColumns + jBlock];
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
    public void setColumnMatrix(final int column, final RealMatrix matrix) {
        try {
            this.setColumnMatrix(column, (BlockRealMatrix) matrix);
        } catch (final ClassCastException cce) {
            super.setColumnMatrix(column, matrix);
        }
    }

    /**
     * Sets the entries in column number <code>column</code> as a column matrix. Column indices start at 0.
     * 
     * @param column
     *        the column to be set
     * @param matrix
     *        column matrix (must have one column and the same number of rows
     *        as the instance)
     * @throws OutOfRangeException
     *         if the specified column index is invalid.
     * @throws MatrixDimensionMismatchException
     *         if the matrix dimensions do
     *         not match one instance column.
     */
    public void setColumnMatrix(final int column, final BlockRealMatrix matrix) {
        MatrixUtils.checkColumnIndex(this, column);
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
        double[] mBlock = matrix.blocks[mBlockIndex];
        for (int iBlock = 0; iBlock < this.blockRows; ++iBlock) {
            final int iHeight = this.blockHeight(iBlock);
            final double[] block = this.blocks[iBlock * this.blockColumns + jBlock];
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
    public RealVector getRowVector(final int row) {
        MatrixUtils.checkRowIndex(this, row);
        final double[] outData = new double[this.columns];

        // perform copy block-wise, to ensure good cache behavior
        final int iBlock = row / BLOCK_SIZE;
        final int iRow = row - iBlock * BLOCK_SIZE;
        int outIndex = 0;
        for (int jBlock = 0; jBlock < this.blockColumns; ++jBlock) {
            final int jWidth = this.blockWidth(jBlock);
            final double[] block = this.blocks[iBlock * this.blockColumns + jBlock];
            System.arraycopy(block, iRow * jWidth, outData, outIndex, jWidth);
            outIndex += jWidth;
        }

        return new ArrayRealVector(outData, false);
    }

    /** {@inheritDoc} */
    @Override
    public void setRowVector(final int row, final RealVector vector) {
        try {
            this.setRow(row, ((ArrayRealVector) vector).getDataRef());
        } catch (final ClassCastException cce) {
            super.setRowVector(row, vector);
        }
    }

    /** {@inheritDoc} */
    @Override
    public RealVector getColumnVector(final int column) {
        MatrixUtils.checkColumnIndex(this, column);
        final double[] outData = new double[this.rows];

        // perform copy block-wise, to ensure good cache behavior
        final int jBlock = column / BLOCK_SIZE;
        final int jColumn = column - jBlock * BLOCK_SIZE;
        final int jWidth = this.blockWidth(jBlock);
        int outIndex = 0;
        for (int iBlock = 0; iBlock < this.blockRows; ++iBlock) {
            final int iHeight = this.blockHeight(iBlock);
            final double[] block = this.blocks[iBlock * this.blockColumns + jBlock];
            for (int i = 0; i < iHeight; ++i) {
                outData[outIndex++] = block[i * jWidth + jColumn];
            }
        }

        return new ArrayRealVector(outData, false);
    }

    /** {@inheritDoc} */
    @Override
    public void setColumnVector(final int column, final RealVector vector) {
        try {
            this.setColumn(column, ((ArrayRealVector) vector).getDataRef());
        } catch (final ClassCastException cce) {
            super.setColumnVector(column, vector);
        }
    }

    /** {@inheritDoc} */
    @Override
    public double[] getRow(final int row) {
        MatrixUtils.checkRowIndex(this, row);
        final double[] out = new double[this.columns];

        // perform copy block-wise, to ensure good cache behavior
        final int iBlock = row / BLOCK_SIZE;
        final int iRow = row - iBlock * BLOCK_SIZE;
        int outIndex = 0;
        for (int jBlock = 0; jBlock < this.blockColumns; ++jBlock) {
            final int jWidth = this.blockWidth(jBlock);
            final double[] block = this.blocks[iBlock * this.blockColumns + jBlock];
            System.arraycopy(block, iRow * jWidth, out, outIndex, jWidth);
            outIndex += jWidth;
        }

        return out;
    }

    /** {@inheritDoc} */
    @Override
    public void setRow(final int row, final double[] array) {
        MatrixUtils.checkRowIndex(this, row);
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
            final double[] block = this.blocks[iBlock * this.blockColumns + jBlock];
            System.arraycopy(array, outIndex, block, iRow * jWidth, jWidth);
            outIndex += jWidth;
        }
    }

    /** {@inheritDoc} */
    @Override
    public double[] getColumn(final int column) {
        MatrixUtils.checkColumnIndex(this, column);
        final double[] out = new double[this.rows];

        // perform copy block-wise, to ensure good cache behavior
        final int jBlock = column / BLOCK_SIZE;
        final int jColumn = column - jBlock * BLOCK_SIZE;
        final int jWidth = this.blockWidth(jBlock);
        int outIndex = 0;
        for (int iBlock = 0; iBlock < this.blockRows; ++iBlock) {
            final int iHeight = this.blockHeight(iBlock);
            final double[] block = this.blocks[iBlock * this.blockColumns + jBlock];
            for (int i = 0; i < iHeight; ++i) {
                out[outIndex++] = block[i * jWidth + jColumn];
            }
        }

        return out;
    }

    /** {@inheritDoc} */
    @Override
    public void setColumn(final int column, final double[] array) {
        MatrixUtils.checkColumnIndex(this, column);
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
            final double[] block = this.blocks[iBlock * this.blockColumns + jBlock];
            for (int i = 0; i < iHeight; ++i) {
                block[i * jWidth + jColumn] = array[outIndex++];
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public double getEntry(final int row, final int column) {
        MatrixUtils.checkMatrixIndex(this, row, column);
        final int iBlock = row / BLOCK_SIZE;
        final int jBlock = column / BLOCK_SIZE;
        final int k = (row - iBlock * BLOCK_SIZE) * this.blockWidth(jBlock) +
            (column - jBlock * BLOCK_SIZE);
        return this.blocks[iBlock * this.blockColumns + jBlock][k];
    }

    /** {@inheritDoc} */
    @Override
    public void setEntry(final int row, final int column, final double value) {
        MatrixUtils.checkMatrixIndex(this, row, column);
        final int iBlock = row / BLOCK_SIZE;
        final int jBlock = column / BLOCK_SIZE;
        final int k = (row - iBlock * BLOCK_SIZE) * this.blockWidth(jBlock) +
            (column - jBlock * BLOCK_SIZE);
        this.blocks[iBlock * this.blockColumns + jBlock][k] = value;
    }

    /** {@inheritDoc} */
    @Override
    public void addToEntry(final int row, final int column,
                           final double increment) {
        MatrixUtils.checkMatrixIndex(this, row, column);
        final int iBlock = row / BLOCK_SIZE;
        final int jBlock = column / BLOCK_SIZE;
        final int k = (row - iBlock * BLOCK_SIZE) * this.blockWidth(jBlock) +
            (column - jBlock * BLOCK_SIZE);
        this.blocks[iBlock * this.blockColumns + jBlock][k] += increment;
    }

    /** {@inheritDoc} */
    @Override
    public void multiplyEntry(final int row, final int column,
                              final double factor) {
        MatrixUtils.checkMatrixIndex(this, row, column);
        final int iBlock = row / BLOCK_SIZE;
        final int jBlock = column / BLOCK_SIZE;
        final int k = (row - iBlock * BLOCK_SIZE) * this.blockWidth(jBlock) +
            (column - jBlock * BLOCK_SIZE);
        this.blocks[iBlock * this.blockColumns + jBlock][k] *= factor;
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.AvoidArrayLoops")
    public BlockRealMatrix transpose() {
        final int nRows = this.getRowDimension();
        final int nCols = this.getColumnDimension();
        final BlockRealMatrix out = new BlockRealMatrix(nCols, nRows);

        // perform transpose block-wise, to ensure good cache behavior
        int blockIndex = 0;
        for (int iBlock = 0; iBlock < this.blockColumns; ++iBlock) {
            for (int jBlock = 0; jBlock < this.blockRows; ++jBlock) {
                // transpose current block
                final double[] outBlock = out.blocks[blockIndex];
                final double[] tBlock = this.blocks[jBlock * this.blockColumns + iBlock];
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
    public double[] operate(final double[] v) {
        if (v.length != this.columns) {
            throw new DimensionMismatchException(v.length, this.columns);
        }
        final double[] out = new double[this.rows];

        // perform multiplication block-wise, to ensure good cache behavior
        for (int iBlock = 0; iBlock < this.blockRows; ++iBlock) {
            final int pStart = iBlock * BLOCK_SIZE;
            final int pEnd = MathLib.min(pStart + BLOCK_SIZE, this.rows);
            for (int jBlock = 0; jBlock < this.blockColumns; ++jBlock) {
                final double[] block = this.blocks[iBlock * this.blockColumns + jBlock];
                final int qStart = jBlock * BLOCK_SIZE;
                final int qEnd = MathLib.min(qStart + BLOCK_SIZE, this.columns);
                int k = 0;
                for (int p = pStart; p < pEnd; ++p) {
                    double sum = 0;
                    int q = qStart;
                    while (q < qEnd - 3) {
                        sum += block[k] * v[q] +
                            block[k + 1] * v[q + 1] +
                            block[k + 2] * v[q + 2] +
                            block[k + 3] * v[q + 3];
                        k += 4;
                        q += 4;
                    }
                    while (q < qEnd) {
                        sum += block[k++] * v[q++];
                    }
                    out[p] += sum;
                }
            }
        }

        return out;
    }

    /** {@inheritDoc} */
    @Override
    public double[] preMultiply(final double[] v) {
        if (v.length != this.rows) {
            throw new DimensionMismatchException(v.length, this.rows);
        }
        final double[] out = new double[this.columns];

        // perform multiplication block-wise, to ensure good cache behavior
        for (int jBlock = 0; jBlock < this.blockColumns; ++jBlock) {
            final int jWidth = this.blockWidth(jBlock);
            final int jWidth2 = jWidth + jWidth;
            final int jWidth3 = jWidth2 + jWidth;
            final int jWidth4 = jWidth3 + jWidth;
            final int qStart = jBlock * BLOCK_SIZE;
            final int qEnd = MathLib.min(qStart + BLOCK_SIZE, this.columns);
            for (int iBlock = 0; iBlock < this.blockRows; ++iBlock) {
                final double[] block = this.blocks[iBlock * this.blockColumns + jBlock];
                final int pStart = iBlock * BLOCK_SIZE;
                final int pEnd = MathLib.min(pStart + BLOCK_SIZE, this.rows);
                for (int q = qStart; q < qEnd; ++q) {
                    int k = q - qStart;
                    double sum = 0;
                    int p = pStart;
                    while (p < pEnd - 3) {
                        sum += block[k] * v[p] +
                            block[k + jWidth] * v[p + 1] +
                            block[k + jWidth2] * v[p + 2] +
                            block[k + jWidth3] * v[p + 3];
                        k += jWidth4;
                        p += 4;
                    }
                    while (p < pEnd) {
                        sum += block[k] * v[p++];
                        k += jWidth;
                    }
                    out[q] += sum;
                }
            }
        }

        return out;
    }

    /** {@inheritDoc} */
    @Override
    public double walkInRowOrder(final RealMatrixChangingVisitor visitor) {
        visitor.start(this.rows, this.columns, 0, this.rows - 1, 0, this.columns - 1);
        for (int iBlock = 0; iBlock < this.blockRows; ++iBlock) {
            final int pStart = iBlock * BLOCK_SIZE;
            final int pEnd = MathLib.min(pStart + BLOCK_SIZE, this.rows);
            for (int p = pStart; p < pEnd; ++p) {
                for (int jBlock = 0; jBlock < this.blockColumns; ++jBlock) {
                    final int jWidth = this.blockWidth(jBlock);
                    final int qStart = jBlock * BLOCK_SIZE;
                    final int qEnd = MathLib.min(qStart + BLOCK_SIZE, this.columns);
                    final double[] block = this.blocks[iBlock * this.blockColumns + jBlock];
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
    public double walkInRowOrder(final RealMatrixPreservingVisitor visitor) {
        visitor.start(this.rows, this.columns, 0, this.rows - 1, 0, this.columns - 1);
        for (int iBlock = 0; iBlock < this.blockRows; ++iBlock) {
            final int pStart = iBlock * BLOCK_SIZE;
            final int pEnd = MathLib.min(pStart + BLOCK_SIZE, this.rows);
            for (int p = pStart; p < pEnd; ++p) {
                for (int jBlock = 0; jBlock < this.blockColumns; ++jBlock) {
                    final int jWidth = this.blockWidth(jBlock);
                    final int qStart = jBlock * BLOCK_SIZE;
                    final int qEnd = MathLib.min(qStart + BLOCK_SIZE, this.columns);
                    final double[] block = this.blocks[iBlock * this.blockColumns + jBlock];
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
    public double walkInRowOrder(final RealMatrixChangingVisitor visitor,
                                 final int startRow, final int endRow,
                                 final int startColumn, final int endColumn) {
        MatrixUtils.checkSubMatrixIndex(this, startRow, endRow, startColumn, endColumn);
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
                    final double[] block = this.blocks[iBlock * this.blockColumns + jBlock];
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
    public double walkInRowOrder(final RealMatrixPreservingVisitor visitor,
                                 final int startRow, final int endRow,
                                 final int startColumn, final int endColumn) {
        MatrixUtils.checkSubMatrixIndex(this, startRow, endRow, startColumn, endColumn);
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
                    final double[] block = this.blocks[iBlock * this.blockColumns + jBlock];
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
    public double walkInOptimizedOrder(final RealMatrixChangingVisitor visitor) {
        visitor.start(this.rows, this.columns, 0, this.rows - 1, 0, this.columns - 1);
        int blockIndex = 0;
        for (int iBlock = 0; iBlock < this.blockRows; ++iBlock) {
            final int pStart = iBlock * BLOCK_SIZE;
            final int pEnd = MathLib.min(pStart + BLOCK_SIZE, this.rows);
            for (int jBlock = 0; jBlock < this.blockColumns; ++jBlock) {
                final int qStart = jBlock * BLOCK_SIZE;
                final int qEnd = MathLib.min(qStart + BLOCK_SIZE, this.columns);
                final double[] block = this.blocks[blockIndex];
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
    public double walkInOptimizedOrder(final RealMatrixPreservingVisitor visitor) {
        visitor.start(this.rows, this.columns, 0, this.rows - 1, 0, this.columns - 1);
        int blockIndex = 0;
        for (int iBlock = 0; iBlock < this.blockRows; ++iBlock) {
            final int pStart = iBlock * BLOCK_SIZE;
            final int pEnd = MathLib.min(pStart + BLOCK_SIZE, this.rows);
            for (int jBlock = 0; jBlock < this.blockColumns; ++jBlock) {
                final int qStart = jBlock * BLOCK_SIZE;
                final int qEnd = MathLib.min(qStart + BLOCK_SIZE, this.columns);
                final double[] block = this.blocks[blockIndex];
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
    public double walkInOptimizedOrder(final RealMatrixChangingVisitor visitor,
                                       final int startRow, final int endRow,
                                       final int startColumn,
                                       final int endColumn) {
        MatrixUtils.checkSubMatrixIndex(this, startRow, endRow, startColumn, endColumn);
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
                final double[] block = this.blocks[iBlock * this.blockColumns + jBlock];
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
    public double walkInOptimizedOrder(final RealMatrixPreservingVisitor visitor,
                                       final int startRow, final int endRow,
                                       final int startColumn,
                                       final int endColumn) {
        MatrixUtils.checkSubMatrixIndex(this, startRow, endRow, startColumn, endColumn);
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
                final double[] block = this.blocks[iBlock * this.blockColumns + jBlock];
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
