/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
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
* VERSION:4.8:DM:DM-2922:15/11/2021:[PATRIUS] suppression de l'utilisation de la reflexion Java dans patrius 
* VERSION:4.8:DM:DM-3040:15/11/2021:[PATRIUS]Reversement des evolutions de la branche patrius-for-lotus 
 * VERSION:4.7:DM:DM-2766:18/05/2021:Evol. et corr. dans le package fr.cnes.sirius.patrius.math.linear (suite DM 2300) 
 * VERSION:4.6:FA:FA-2542:27/01/2021:[PATRIUS] Definition d'un champ de vue avec demi-angle de 180° 
 * VERSION:4.5.1:FA:FA-2540:04/08/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear
 * VERSION:4.5:DM:DM-2300:27/05/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.linear;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import fr.cnes.sirius.patrius.math.Field;
import fr.cnes.sirius.patrius.math.FieldElement;
import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.MathArithmeticException;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.exception.NoDataException;
import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;
import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;
import fr.cnes.sirius.patrius.math.exception.ZeroException;
import fr.cnes.sirius.patrius.math.fraction.BigFraction;
import fr.cnes.sirius.patrius.math.fraction.Fraction;
import fr.cnes.sirius.patrius.math.util.CompositeFormat;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathArrays;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

//CHECKSTYLE: stop CommentRatio check
//Reason: model - Commons-Math code kept as such

/**
 * A collection of static methods that operate on or return matrices.
 *
 * <p>
 * This class is up-to-date with commons-math 3.6.1.
 * </p>
 *
 * @version $Id: MatrixUtils.java 18108 2017-10-04 06:45:27Z bignon $
 */
public final class MatrixUtils {

    /** Format number. */
    public static final String NUMBER_FORMAT = "% 11.5g";

    /** Opening curly bracket character ('{'). */
    public static final String OPENING_CURLY_BRACKET = "{";

    /** Closing curly bracket character ('}'). */
    public static final String CLOSING_CURLY_BRACKET = "}";

    /** Opening bracket character ('['). */
    public static final String OPENING_BRACKET = "[";

    /** Closing bracket character (']'). */
    public static final String CLOSING_BRACKET = "]";

    /** Space character (' '). */
    public static final String SPACE = " ";

    /** Comma character (','). */
    public static final String COMMA = ",";

    /** Semicolon character (';') . */
    public static final String SEMICOLON = ";";

    /** Carrier return ('\n'). */
    public static final String CARRIER_RETURN = "\n";

    /**
     * The default {@link RealMatrixFormat}.
     *
     * @since 3.1
     */
    public static final RealMatrixFormat DEFAULT_FORMAT = RealMatrixFormat.getInstance(Locale.US);

    /**
     * The JAVA format for {@link RealMatrix} objects.
     *
     * @since 4.5
     */
    public static final RealMatrixFormat JAVA_FORMAT = new RealMatrixFormat(OPENING_CURLY_BRACKET,
            CLOSING_CURLY_BRACKET, OPENING_CURLY_BRACKET, CLOSING_CURLY_BRACKET, COMMA + SPACE,
            COMMA + SPACE, CompositeFormat.getDefaultNumberFormat(Locale.US));

    /**
     * A format for {@link RealMatrix} objects compatible with octave.
     *
     * @since 3.1
     */
    public static final RealMatrixFormat OCTAVE_FORMAT = new RealMatrixFormat(OPENING_BRACKET,
            CLOSING_BRACKET, "", "", SEMICOLON + SPACE, COMMA + SPACE,
            CompositeFormat.getDefaultNumberFormat(Locale.US));

    /**
     * The SCILAB format for {@link RealMatrix} objects.
     *
     * @since 4.5
     */
    public static final RealMatrixFormat SCILAB_FORMAT = new RealMatrixFormat(SPACE
            + OPENING_BRACKET, CLOSING_BRACKET, "", "", SEMICOLON + SPACE, COMMA + SPACE,
            CompositeFormat.getDefaultNumberFormat(Locale.US));

    static {
        // set the minimum fraction digits to 1 to keep compatibility
        SCILAB_FORMAT.getFormat().setMinimumFractionDigits(1);
    }

    /**
     * Visual format for {@link RealMatrix} objects displayed on several rows.
     * <p>
     * The pattern <cite>"% 11.5g"</cite> by default set the significant digit accuracy at 5, the
     * width equal to 11, the format is automatically set to digital or scientific and the space
     * between % and 11 allows to display the sign for scientific values.
     * </p>
     *
     * @since 4.5
     */
    public static final RealMatrixFormat VISUAL_FORMAT = new RealMatrixFormat(OPENING_BRACKET,
            CLOSING_BRACKET, OPENING_BRACKET + SPACE, CLOSING_BRACKET, CARRIER_RETURN + SPACE,
            COMMA + SPACE, NUMBER_FORMAT);

    /**
     * Summary visual format for {@link RealMatrix} objects displayed on several rows.
     * <p>
     * The pattern <cite>"% 11.5g"</cite> by default set the significant digit accuracy at 5, the
     * width equal to 11, the format is automatically set to digital or scientific and the space
     * between % and 11 allows to display the sign for scientific values. The summary mode, with an
     * index set to 3, will display the 3x3 sub-matrix in each corner and the rows and columns total
     * number.
     * </p>
     *
     * @since 4.5
     */
    public static final RealMatrixFormat SUMMARY_FORMAT = new RealMatrixFormat(OPENING_BRACKET,
            CLOSING_BRACKET, OPENING_BRACKET, CLOSING_BRACKET, CARRIER_RETURN + SPACE, COMMA
                    + SPACE, NUMBER_FORMAT, 3);

    /** Max matrix size above which a block matrix is used. */
    private static final double MAX_SIZE = 4096;

    /**
     * Private constructor.
     */
    private MatrixUtils() {
        super();
    }

    /**
     * Returns a {@link RealMatrix} with specified dimensions.
     * <p>
     * The type of matrix returned depends on the dimension. Below 2<sup>12</sup> elements (i.e.
     * 4096 elements or 64&times;64 for a square matrix) which can be stored in a 32kB array, a
     * {@link Array2DRowRealMatrix} instance is built. Above this threshold a
     * {@link BlockRealMatrix} instance is built.
     * </p>
     * <p>
     * The matrix elements are all set to 0.0.
     * </p>
     *
     * @param rows
     *        the number of rows of the matrix
     * @param columns
     *        the number of columns of the matrix
     * @return a new {@link RealMatrix} with specified dimensions
     * @see #createRealMatrix(double[][])
     */
    public static RealMatrix createRealMatrix(final int rows, final int columns) {
        final RealMatrix out;
        if (rows * columns <= MAX_SIZE) {
            out = new Array2DRowRealMatrix(rows, columns);
        } else {
            out = new BlockRealMatrix(rows, columns);
        }
        return out;
    }

    /**
     * Returns a {@link FieldMatrix} with specified dimensions.
     * <p>
     * The type of matrix returned depends on the dimension. Below 2<sup>12</sup> elements (i.e.
     * 4096 elements or 64&times;64 for a square matrix), a {@link FieldMatrix} instance is built.
     * Above this threshold a {@link BlockFieldMatrix} instance is built.
     * </p>
     * <p>
     * The matrix elements are all set to field.getZero().
     * </p>
     *
     * @param field
     *        the field to which the matrix elements belong
     * @param rows
     *        number of rows of the matrix
     * @param columns
     *        number of columns of the matrix
     * @param <T>
     *        the type of the field elements
     * @return a new {@link FieldMatrix} with specified dimensions
     * @see #createFieldMatrix(FieldElement[][])
     * @since 2.0
     */
    public static <T extends FieldElement<T>> FieldMatrix<T> createFieldMatrix(
            final Field<T> field, final int rows, final int columns) {
        final FieldMatrix<T> out;
        if (rows * columns <= MAX_SIZE) {
            out = new Array2DRowFieldMatrix<T>(field, rows, columns);
        } else {
            out = new BlockFieldMatrix<T>(field, rows, columns);
        }
        return out;
    }

    /**
     * Returns a {@link RealMatrix} whose entries are the the values in the the input array.
     * <p>
     * The type of matrix returned depends on the dimension. Below 2<sup>12</sup> elements (i.e.
     * 4096 elements or 64&times;64 for a square matrix) which can be stored in a 32kB array, a
     * {@link Array2DRowRealMatrix} instance is built. Above this threshold a
     * {@link BlockRealMatrix} instance is built.
     * </p>
     * <p>
     * The input array is copied, not referenced.
     * </p>
     *
     * @param data
     *        the input array
     * @return a new {@link RealMatrix} containing the values of the input array
     * @throws NullArgumentException
     *         if either {@code data} or {@code data[0]} is {@code null}
     * @throws NoDataException
     *         if a row or column is empty
     * @throws DimensionMismatchException
     *         if {@code data} is not rectangular (not all rows have the same length)
     * @see #createRealMatrix(int, int)
     */
    public static RealMatrix createRealMatrix(final double[][] data) {
        return createRealMatrix(data, true);
    }

    /**
     * Returns a {@link RealMatrix} whose entries are the the values in the the input array.
     * <p>
     * The type of matrix returned depends on the dimension. Below 2<sup>12</sup> elements (i.e.
     * 4096 elements or 64&times;64 for a square matrix) which can be stored in a 32kB array, a
     * {@link Array2DRowRealMatrix} instance is built. Above this threshold a
     * {@link BlockRealMatrix} instance is built.
     * </p>
     * <p>
     * The input array is either copied or referenced, depending on the {@code forceCopyArray}
     * argument.
     * </p>
     *
     * @param data
     *        the input array
     * @param forceCopyArray
     *        if {@code true}, the input array is copied, otherwise it is referenced
     * @return a new {@link RealMatrix} containing the values of the input array
     * @throws NullArgumentException
     *         if either {@code data} or {@code data[0]} is {@code null}.
     * @throws NoDataException
     *         if a row or column is empty.
     * @throws DimensionMismatchException
     *         if {@code data} is not rectangular (not all rows have the same length)
     * @see #createRealMatrix(int, int)
     */
    public static RealMatrix createRealMatrix(final double[][] data, final boolean forceCopyArray) {
        // Check the provided data array
        checkMatrixArray(data, false);

        // Estimate the number of elements in the created matrix
        // (assuming the provided data array is rectangular)
        final int nbElements = data.length * data[0].length;

        // Create the matrix
        final RealMatrix out;
        if (nbElements <= MAX_SIZE) {
            out = new Array2DRowRealMatrix(data, forceCopyArray);
        } else {
            out = new BlockRealMatrix(data.length, data[0].length,
                    BlockRealMatrix.toBlocksLayout(data), forceCopyArray);
        }
        return out;
    }

    /**
     * Returns a {@link FieldMatrix} whose entries are the the values in the the input array.
     * <p>
     * The type of matrix returned depends on the dimension. Below 2<sup>12</sup> elements (i.e.
     * 4096 elements or 64&times;64 for a square matrix), a {@link FieldMatrix} instance is built.
     * Above this threshold a {@link BlockFieldMatrix} instance is built.
     * </p>
     * <p>
     * The input array is copied, not referenced.
     * </p>
     *
     * @param data
     *        the input array
     * @param <T>
     *        the type of the field elements
     * @return a matrix containing the values of the array.
     * @throws NullArgumentException
     *         if either {@code data} or {@code data[0]} is {@code null}
     * @throws NoDataException
     *         if a row or column is empty.
     * @throws DimensionMismatchException
     *         if {@code data} is not rectangular (not all rows have the same length)
     * @see #createFieldMatrix(Field, int, int)
     * @since 2.0
     */
    public static <T extends FieldElement<T>> FieldMatrix<T> createFieldMatrix(final T[][] data) {
        // Check the provided data array
        checkMatrixArray(data, false);

        // Estimate the number of elements in the created matrix
        // (assuming the provided data array is rectangular)
        final int nbElements = data.length * data[0].length;

        // Create the matrix
        final FieldMatrix<T> out;
        if (nbElements <= MAX_SIZE) {
            out = new Array2DRowFieldMatrix<T>(data);
        } else {
            out = new BlockFieldMatrix<T>(data);
        }
        return out;
    }

    /**
     * Returns {@code dimension x dimension} identity matrix.
     * <p>
     * The type of matrix returned depends on the dimension. Below 2<sup>12</sup> elements (i.e.
     * 4096 elements or 64&times;64 for a square matrix) which can be stored in a 32kB array, a
     * {@link Array2DRowRealMatrix} instance is built. Above this threshold a
     * {@link BlockRealMatrix} instance is built.
     * </p>
     *
     * @param dimension
     *        the dimension of identity matrix to be generated
     * @return the generated identity matrix
     * @throws NotStrictlyPositiveException
     *         if the specified dimension is not strictly positive
     * @since 1.1
     * @see #createRealMatrix(int, int)
     */
    public static RealMatrix createRealIdentityMatrix(final int dimension) {
        return createRealIdentityMatrix(dimension, false);
    }

    /**
     * Returns {@code dimension x dimension} identity matrix.
     * <p>
     * If {@code diagonalMatrixInstance} is set to {@code true}, the generated identity matrix is a
     * {@link DiagonalMatrix} instance. Otherwise, the type of matrix returned depends on the
     * dimension. Below 2<sup>12</sup> elements (i.e. 4096 elements or 64&times;64 for a square
     * matrix) which can be stored in a 32kB array, a {@link Array2DRowRealMatrix} instance is
     * built. Above this threshold a {@link BlockRealMatrix} instance is built.
     * </p>
     * <p>
     * Although {@link DiagonalMatrix} instances are optimized for storing diagonal matrices, they
     * do have some limitations. Setting off-diagonal elements to non-zero values is forbidden,
     * which means any attempt to modify the matrix will most likely result in an exception.
     * </p>
     *
     * @param dimension
     *        the dimension of identity matrix to be generated
     * @param diagonalMatrixInstance
     *        if {@code true}, the generated identity matrix is a {@link DiagonalMatrix} instance,
     *        otherwise it is either an {@link Array2DRowRealMatrix} or a {@link BlockRealMatrix}
     *        instance
     * @return the generated identity matrix
     * @throws NotStrictlyPositiveException
     *         if the specified dimension is not strictly positive
     * @since 4.5
     * @see #createRealMatrix(int, int)
     */
    public static RealMatrix createRealIdentityMatrix(final int dimension,
            final boolean diagonalMatrixInstance) {
        final RealMatrix out;

        if (diagonalMatrixInstance) {
            // Generate a DiagonalMatrix instance
            out = DiagonalMatrix.createIdentityMatrix(dimension);
        } else {
            // Generate a regular or a block matrix
            out = createRealMatrix(dimension, dimension);
            for (int i = 0; i < dimension; ++i) {
                out.setEntry(i, i, 1.0);
            }
        }

        return out;
    }

    /**
     * Returns <code>dimension x dimension</code> identity matrix.
     *
     * @param field
     *        field to which the elements belong
     * @param dimension
     *        dimension of identity matrix to generate
     * @param <T>
     *        the type of the field elements
     * @return identity matrix
     * @throws IllegalArgumentException
     *         if dimension is not positive
     * @since 2.0
     */
    public static <T extends FieldElement<T>> FieldMatrix<T> createFieldIdentityMatrix(
            final Field<T> field, final int dimension) {
        final T zero = field.getZero();
        final T one = field.getOne();
        final T[][] d = MathArrays.buildArray(field, dimension, dimension);
        for (int row = 0; row < dimension; row++) {
            final T[] dRow = d[row];
            Arrays.fill(dRow, zero);
            dRow[row] = one;
        }
        return new Array2DRowFieldMatrix<T>(field, d, false);
    }

    /**
     * Returns a diagonal matrix with specified elements.
     *
     * @param diagonal
     *        diagonal elements of the matrix (the array elements will be copied)
     * @return diagonal matrix
     * @since 2.0
     */
    public static RealMatrix createRealDiagonalMatrix(final double[] diagonal) {
        final RealMatrix m = createRealMatrix(diagonal.length, diagonal.length);
        for (int i = 0; i < diagonal.length; ++i) {
            m.setEntry(i, i, diagonal[i]);
        }
        return m;
    }

    /**
     * Returns a diagonal matrix with specified elements.
     *
     * @param diagonal
     *        diagonal elements of the matrix (the array elements
     *        will be copied)
     * @param <T>
     *        the type of the field elements
     * @return diagonal matrix
     * @since 2.0
     */
    public static <T extends FieldElement<T>> FieldMatrix<T> createFieldDiagonalMatrix(
            final T[] diagonal) {
        final FieldMatrix<T> m = createFieldMatrix(diagonal[0].getField(), diagonal.length,
                diagonal.length);
        for (int i = 0; i < diagonal.length; ++i) {
            m.setEntry(i, i, diagonal[i]);
        }
        return m;
    }

    /**
     * Creates a {@link RealVector} using the data from the input array.
     *
     * @param data
     *        the input data
     * @return a data.length RealVector
     * @throws NoDataException
     *         if {@code data} is empty.
     * @throws NullArgumentException
     *         if {@code data} is {@code null}.
     */
    public static RealVector createRealVector(final double[] data) {
        MathUtils.checkNotNull(data);
        return new ArrayRealVector(data, true);
    }

    /**
     * Creates a {@link FieldVector} using the data from the input array.
     *
     * @param data
     *        the input data
     * @param <T>
     *        the type of the field elements
     * @return a data.length FieldVector
     * @throws NoDataException
     *         if {@code data} is empty.
     * @throws NullArgumentException
     *         if {@code data} is {@code null}.
     * @throws ZeroException
     *         if {@code data} has 0 elements
     */
    public static <T extends FieldElement<T>> FieldVector<T> createFieldVector(final T[] data) {
        MathUtils.checkNotNull(data);
        if (data.length == 0) {
            throw new ZeroException(PatriusMessages.VECTOR_MUST_HAVE_AT_LEAST_ONE_ELEMENT);
        }
        return new ArrayFieldVector<T>(data[0].getField(), data, true);
    }

    /**
     * Create a row {@link RealMatrix} using the data from the input array.
     *
     * @param rowData
     *        the input row data
     * @return a 1 x rowData.length RealMatrix
     * @throws NoDataException
     *         if {@code rowData} is empty.
     * @throws NullArgumentException
     *         if {@code rowData} is {@code null}.
     */
    public static RealMatrix createRowRealMatrix(final double[] rowData) {
        if (rowData == null) {
            throw new NullArgumentException(PatriusMessages.NULL_ARRAY_NOT_ALLOWED);
        }
        final int nCols = rowData.length;
        final RealMatrix m = createRealMatrix(1, nCols);
        for (int i = 0; i < nCols; ++i) {
            m.setEntry(0, i, rowData[i]);
        }
        return m;
    }

    /**
     * Create a row {@link FieldMatrix} using the data from the input array.
     *
     * @param rowData
     *        the input row data
     * @return a 1 x rowData.length FieldMatrix
     * @param <T>
     *        the type of the field elements
     * @throws NoDataException
     *         if {@code rowData} is empty.
     * @throws NullArgumentException
     *         if {@code rowData} is {@code null}.
     */
    public static <T extends FieldElement<T>> FieldMatrix<T>
            createRowFieldMatrix(final T[] rowData) {
        MathUtils.checkNotNull(rowData);
        final int nCols = rowData.length;
        if (nCols == 0) {
            throw new NoDataException(PatriusMessages.AT_LEAST_ONE_COLUMN);
        }
        final FieldMatrix<T> m = createFieldMatrix(rowData[0].getField(), 1, nCols);
        for (int i = 0; i < nCols; ++i) {
            m.setEntry(0, i, rowData[i]);
        }
        return m;
    }

    /**
     * Creates a column {@link RealMatrix} using the data from the input array.
     *
     * @param columnData
     *        the input column data
     * @return a columnData x 1 RealMatrix
     * @throws NoDataException
     *         if {@code columnData} is empty.
     * @throws NullArgumentException
     *         if {@code columnData} is {@code null}.
     */
    public static RealMatrix createColumnRealMatrix(final double[] columnData) {
        MathUtils.checkNotNull(columnData);
        final int nRows = columnData.length;
        final RealMatrix m = createRealMatrix(nRows, 1);
        for (int i = 0; i < nRows; ++i) {
            m.setEntry(i, 0, columnData[i]);
        }
        return m;
    }

    /**
     * Creates a column {@link FieldMatrix} using the data from the input array.
     *
     * @param columnData
     *        the input column data
     * @param <T>
     *        the type of the field elements
     * @return a columnData x 1 FieldMatrix
     * @throws NoDataException
     *         if {@code data} is empty.
     * @throws NullArgumentException
     *         if {@code columnData} is {@code null}.
     */
    public static <T extends FieldElement<T>> FieldMatrix<T> createColumnFieldMatrix(
            final T[] columnData) {
        MathUtils.checkNotNull(columnData);
        final int nRows = columnData.length;
        if (nRows == 0) {
            throw new NoDataException(PatriusMessages.AT_LEAST_ONE_ROW);
        }
        final FieldMatrix<T> m = createFieldMatrix(columnData[0].getField(), nRows, 1);
        for (int i = 0; i < nRows; ++i) {
            m.setEntry(i, 0, columnData[i]);
        }
        return m;
    }

    /**
     * Concatenates two matrices horizontally.
     *
     * @param left
     *        the left matrix [NxM]
     * @param right
     *        the right matrix [NxL]
     * @return the concatenated matrix [Nx(M+L)]
     * @throws DimensionMismatchException
     *         if the two matrices don't have the same number of rows
     */
    public static RealMatrix concatenateHorizontally(final RealMatrix left, final RealMatrix right) {
        return left.concatenateHorizontally(right, true);
    }

    /**
     * Concatenates two matrices vertically.
     *
     * @param upper
     *        the upper matrix [MxN]
     * @param lower
     *        the upper matrix [LxN]
     * @return the concatenated matrix [(M+L)xN]
     * @throws DimensionMismatchException
     *         if the two matrices don't have the same number of columns
     */
    public static RealMatrix concatenateVertically(final RealMatrix upper, final RealMatrix lower) {
        return upper.concatenateVertically(lower, true);
    }

    /**
     * Resizes the provided matrix to a NxM matrix.
     * <p>
     * The provided matrix is truncated or extended, depending on whether its dimensions are bigger
     * or smaller than the requested dimensions. If extended, the terms added are set to zero.
     * </p>
     *
     * @param matrix
     *        the matrix to be truncated
     * @param rowDim
     *        the row dimension N of the matrix returned
     * @param colDim
     *        the column dimension M of the matrix returned
     * @return the resized matrix (NxM)
     */
    public static RealMatrix resize(final RealMatrix matrix, final int rowDim, final int colDim) {
        final int nr = FastMath.min(rowDim, matrix.getRowDimension());
        final int nc = FastMath.min(colDim, matrix.getColumnDimension());

        final RealMatrix resized = new Array2DRowRealMatrix(rowDim, colDim);
        resized.setSubMatrix(matrix.getSubMatrix(0, nr - 1, 0, nc - 1).getData(false), 0, 0);

        return resized;
    }

    /**
     * Multiplies the matrix L by R<sup>T</sup>.
     * <p>
     * This methods allows to combine matrix multiplication and matrix transposition for
     * optimization purposes.
     * </p>
     *
     * @param matrixL
     *        the left-side matrix L
     * @param matrixR
     *        the right-side matrix R (not its transpose)
     * @return the result of the multiplication
     */
    public static RealMatrix
            multiplyByTranspose(final RealMatrix matrixL, final RealMatrix matrixR) {
        return multiplyByTranspose(1.0, matrixL, matrixR);
    }

    /**
     * Multiplies the matrix L by R<sup>T</sup> and by a scalar factor &alpha;.
     * <p>
     * This methods allows to combine scalar multiplication, matrix multiplication and matrix
     * transposition for optimization purposes.
     * </p>
     *
     * @param alpha
     *        the scalar factor &alpha;
     * @param matrixL
     *        the left-side matrix L
     * @param matrixR
     *        the right-side matrix R (not its transpose)
     * @return the result of the multiplication
     */
    public static RealMatrix multiplyByTranspose(final double alpha, final RealMatrix matrixL,
            final RealMatrix matrixR) {
        return matrixL.multiply(matrixR, true, alpha);
    }

    /**
     * Checks whether a matrix is symmetric, within a given relative tolerance.
     *
     * @param matrix
     *        Matrix to check.
     * @param relativeTolerance
     *        Tolerance of the symmetry check.
     * @return {@code true} if {@code matrix} is symmetric.
     * @throws NonSquareMatrixException
     *         if the matrix is not square.
     * @throws NonSymmetricMatrixException
     *         if the matrix is not symmetric.
     */
    private static boolean isSymmetric(final RealMatrix matrix, final double relativeTolerance) {
        final int rows = matrix.getRowDimension();
        if (rows != matrix.getColumnDimension()) {
            throw new NonSquareMatrixException(rows, matrix.getColumnDimension());
        }
        for (int i = 0; i < rows; i++) {
            for (int j = i + 1; j < rows; j++) {
                final double mij = matrix.getEntry(i, j);
                final double mji = matrix.getEntry(j, i);
                if (MathLib.abs(mij - mji) > MathLib.max(MathLib.abs(mij), MathLib.abs(mji))
                        * relativeTolerance) {
                    throw new NonSymmetricMatrixException(i, j, relativeTolerance);
                }
            }
        }
        return true;
    }

    /**
     * Returns a deep copy of a 2D {@code double} array.
     *
     * @param array
     *        the array to be copied
     * @return a deep copy of the provided array, or {@code null} if the supplied array is
     *         {@code null}
     */
    public static double[][] copyArray(final double[][] array) {
        double[][] copy = null;
        if (array != null) {
            copy = new double[array.length][];
            for (int i = 0; i < array.length; i++) {
                copy[i] = Arrays.copyOf(array[i], array[i].length);
            }
        }
        return copy;
    }

    /**
     * Checks the validity of a matrix data array.
     * <p>
     * To be valid, the provided data array must not be {@code null} or empty, its first row must
     * also not be {@code null} or empty, and the rows must all have the same length.
     * </p>
     *
     * @param data
     *        the matrix data array to be checked
     * @throws NullArgumentException
     *         if the data array is {@code null}
     * @throws NoDataException
     *         if the data array is empty
     * @throws DimensionMismatchException
     *         if the rows of the data array do not have a constant number of columns
     */
    public static void checkMatrixArray(final double[][] data) {
        checkMatrixArray(data, true);
    }

    /**
     * Checks the validity of a matrix data array.
     * <p>
     * To be valid, the provided data array must not be {@code null} or empty, and its first row
     * must also not be {@code null} or empty. Setting {@code checkColumnDimensions} to {@code true}
     * , enables an additional check which verifies that the rows all have the same length.
     * </p>
     *
     * @param data
     *        the matrix data array to be checked
     * @param checkColumnDimensions
     *        whether or not to check if the rows all have the same dimension
     * @throws NullArgumentException
     *         if the data array is {@code null}
     * @throws NoDataException
     *         if the data array is empty
     * @throws DimensionMismatchException
     *         if the rows of the data array do not have a constant number of columns (optional
     *         check)
     */
    public static void checkMatrixArray(final double[][] data, final boolean checkColumnDimensions) {
        // Ensure the data array is not null
        if (data == null) {
            throw new NullArgumentException(PatriusMessages.NULL_ARRAY_NOT_ALLOWED);
        }

        // Ensure the data array has at least one row
        final int nbRows = data.length;
        if (nbRows == 0) {
            throw new NoDataException(PatriusMessages.AT_LEAST_ONE_ROW);
        }

        // Ensure the first row is not null
        if (data[0] == null) {
            throw new NullArgumentException(PatriusMessages.NULL_ARRAY_NOT_ALLOWED);
        }

        // Ensure the first row has at least one column
        final int nbCols = data[0].length;
        if (nbCols == 0) {
            throw new NoDataException(PatriusMessages.AT_LEAST_ONE_COLUMN);
        }

        // Ensure the rows are not null and all have the same dimension
        if (checkColumnDimensions) {
            for (int i = 1; i < nbRows; i++) {
                if (data[i] == null) {
                    throw new NullArgumentException(PatriusMessages.NULL_ARRAY_NOT_ALLOWED);
                }

                if (data[i].length != nbCols) {
                    throw new DimensionMismatchException(data[i].length, nbCols);
                }
            }
        }
    }

    /**
     * Checks the validity of a matrix data array.
     * <p>
     * To be valid, the provided data array must not be {@code null} or empty, its first row must
     * also not be {@code null} or empty, and the rows must all have the same length.
     * </p>
     *
     * @param data
     *        the matrix data array to be checked
     * @throws NullArgumentException
     *         if the data array is {@code null}
     * @throws NoDataException
     *         if the data array is empty
     * @throws DimensionMismatchException
     *         if the rows of the data array do not have a constant number of columns
     */
    public static void checkMatrixArray(final Object[][] data) {
        checkMatrixArray(data, true);
    }

    /**
     * Checks the validity of a matrix data array.
     * <p>
     * To be valid, the provided data array must not be {@code null} or empty, and its first row
     * must also not be {@code null} or empty. Setting {@code checkColumnDimensions} to {@code true}
     * , enables an additional check which verifies that the rows all have the same length.
     * </p>
     *
     * @param data
     *        the matrix data array to be checked
     * @param checkColumnDimensions
     *        whether or not to check if the rows all have the same dimension
     * @throws NullArgumentException
     *         if the data array is {@code null}
     * @throws NoDataException
     *         if the data array is empty
     * @throws DimensionMismatchException
     *         if the rows of the data array do not have a constant number of columns (optional
     *         check)
     */
    public static void checkMatrixArray(final Object[][] data, final boolean checkColumnDimensions) {
        // Ensure the data array is not null
        if (data == null) {
            throw new NullArgumentException(PatriusMessages.NULL_ARRAY_NOT_ALLOWED);
        }

        // Ensure the data array has at least one row
        final int nbRows = data.length;
        if (nbRows == 0) {
            throw new NoDataException(PatriusMessages.AT_LEAST_ONE_ROW);
        }

        // Ensure the first row is not null
        if (data[0] == null) {
            throw new NullArgumentException(PatriusMessages.NULL_ARRAY_NOT_ALLOWED);
        }

        // Ensure the first row has at least one column
        final int nbCols = data[0].length;
        if (nbCols == 0) {
            throw new NoDataException(PatriusMessages.AT_LEAST_ONE_COLUMN);
        }

        // Ensure the rows are not null and all have the same dimension
        if (checkColumnDimensions) {
            for (int i = 1; i < nbRows; i++) {
                if (data[i] == null) {
                    throw new NullArgumentException(PatriusMessages.NULL_ARRAY_NOT_ALLOWED);
                }

                if (data[i].length != nbCols) {
                    throw new DimensionMismatchException(data[i].length, nbCols);
                }
            }
        }
    }

    /**
     * Checks the validity of the provided row dimension (must be strictly positive).
     *
     * @param rowDimension
     *        the row dimension to be checked
     * @throws NotStrictlyPositiveException
     *         if the row dimension is not strictly positive
     */
    public static void checkRowDimension(final int rowDimension) {
        if (rowDimension < 1) {
            throw new NotStrictlyPositiveException(PatriusMessages.NOT_POSITIVE_ROW_DIMENSION,
                    rowDimension);
        }
    }

    /**
     * Checks the validity of the provided column dimension (must be strictly positive).
     *
     * @param columnDimension
     *        the column dimension to be checked
     * @throws NotStrictlyPositiveException
     *         if the column dimension is not strictly positive
     */
    public static void checkColumnDimension(final int columnDimension) {
        if (columnDimension < 1) {
            throw new NotStrictlyPositiveException(PatriusMessages.NOT_POSITIVE_COLUMN_DIMENSION,
                    columnDimension);
        }
    }

    /**
     * Checks whether a matrix is symmetric.
     *
     * @param matrix
     *        Matrix to check.
     * @param eps
     *        Relative tolerance.
     * @throws NonSquareMatrixException
     *         if the matrix is not square.
     * @throws NonSymmetricMatrixException
     *         if the matrix is not symmetric.
     * @since 3.1
     */
    public static void checkSymmetric(final RealMatrix matrix, final double eps) {
        if (!matrix.isSymmetric(eps)) {
            isSymmetric(matrix, eps);
        }
    }

    /**
     * Checks if the provided indices are valid row and column indices for a given 2D-array.
     *
     * @param array
     *        the array
     * @param row
     *        the row index to be checked
     * @param column
     *        the column index to be checked
     * @throws OutOfRangeException
     *         if the provided indices are not valid row or column indices
     */
    public static void checkArrayIndex(final double[][] array, final int row, final int column) {
        if (row < 0 || row >= array.length) {
            throw new OutOfRangeException(PatriusMessages.ROW_INDEX, row, 0, array.length - 1);
        }

        if (column < 0 || column >= array[row].length) {
            throw new OutOfRangeException(PatriusMessages.COLUMN_INDEX, column, 0,
                    array[row].length - 1);
        }
    }

    /**
     * Checks if the provided indices are valid row and column indices are for a given matrix.
     *
     * @param m
     *        the matrix
     * @param row
     *        the row index to be checked
     * @param column
     *        the column index to be checked
     * @throws OutOfRangeException
     *         if the provided indices are not valid row or column indices
     */
    public static void checkMatrixIndex(final AnyMatrix m, final int row, final int column) {
        checkRowIndex(m, row);
        checkColumnIndex(m, column);
    }

    /**
     * Checks if an index is a valid row index for a given matrix.
     *
     * @param m
     *        the matrix
     * @param row
     *        the row index to be checked
     * @throws OutOfRangeException
     *         if the provided index is not a valid row index
     */
    public static void checkRowIndex(final AnyMatrix m, final int row) {
        if (row < 0 || row >= m.getRowDimension()) {
            throw new OutOfRangeException(PatriusMessages.ROW_INDEX, row, 0,
                    m.getRowDimension() - 1);
        }
    }

    /**
     * Checks if an index is a valid column index for a given matrix.
     *
     * @param m
     *        the matrix
     * @param column
     *        the column index to be checked
     * @throws OutOfRangeException
     *         if the provided index is not a valid column index
     */
    public static void checkColumnIndex(final AnyMatrix m, final int column) {
        if (column < 0 || column >= m.getColumnDimension()) {
            throw new OutOfRangeException(PatriusMessages.COLUMN_INDEX, column, 0,
                    m.getColumnDimension() - 1);
        }
    }

    /**
     * Checks if the provided indices are valid row indices for a given matrix.
     * <p>
     * This method throws an exception if the provided array is not a valid row index array. An
     * index array is considered to be valid if it is not {@code null} or empty, and if each of its
     * indices are between 0 to n-1, with n the row dimension of the matrix.
     * </p>
     *
     * @param m
     *        the matrix
     * @param indices
     *        the array of row indices
     * @throws NullArgumentException
     *         if the provided index array is {@code null}
     * @throws NoDataException
     *         if the provided index array is empty (zero length)
     * @throws OutOfRangeException
     *         if one of the provided indices is not a valid row index
     */
    public static void checkRowIndices(final AnyMatrix m, final int[] indices) {
        // Ensure the index array is not null
        MathUtils.checkNotNull(indices);

        // Ensure the index array is not empty
        if (indices.length == 0) {
            throw new NoDataException(PatriusMessages.EMPTY_SELECTED_ROW_INDEX_ARRAY);
        }

        // Ensure each of the provided indices is a valid row index
        for (final int index : indices) {
            checkRowIndex(m, index);
        }
    }

    /**
     * Checks if the provided indices are valid column indices for a given matrix.
     * <p>
     * This method throws an exception if the provided array is not a valid row index array. An
     * index array is considered to be valid if it is not {@code null} or empty, and if each of its
     * indices are between 0 to n-1, with n the column dimension of the matrix.
     * </p>
     *
     * @param m
     *        the matrix
     * @param indices
     *        the array of column indices
     * @throws NullArgumentException
     *         if the provided index array is {@code null}
     * @throws NoDataException
     *         if the provided index array is empty (zero length)
     * @throws OutOfRangeException
     *         if one of the provided indices is not a valid column index
     */
    public static void checkColumnIndices(final AnyMatrix m, final int[] indices) {
        // Ensure the index array is not null
        MathUtils.checkNotNull(indices);

        // Ensure the index array is not empty
        if (indices.length == 0) {
            throw new NoDataException(PatriusMessages.EMPTY_SELECTED_COLUMN_INDEX_ARRAY);
        }

        // Ensure each of the provided indices is a valid column index
        for (final int index : indices) {
            checkColumnIndex(m, index);
        }
    }

    /**
     * Checks if the provided submatrix ranges are valid for a given matrix.
     * <p>
     * Rows and columns are indicated counting from 0 to {@code n - 1}.
     * </p>
     *
     * @param m
     *        the matrix
     * @param startRow
     *        the initial row index
     * @param endRow
     *        the final row index
     * @param startColumn
     *        the initial column index
     * @param endColumn
     *        the final column index
     * @throws OutOfRangeException
     *         if one of the provided indices is not a valid row or column index
     * @throws NumberIsTooSmallException
     *         if {@code endRow < startRow} or {@code endColumn < startColumn}.
     */
    public static void checkSubMatrixIndex(final AnyMatrix m, final int startRow, final int endRow,
            final int startColumn, final int endColumn) {
        checkRowIndex(m, startRow);
        checkRowIndex(m, endRow);
        checkColumnIndex(m, startColumn);
        checkColumnIndex(m, endColumn);

        if (endRow < startRow) {
            throw new NumberIsTooSmallException(PatriusMessages.INITIAL_ROW_AFTER_FINAL_ROW,
                    endRow, startRow, false);
        }
        if (endColumn < startColumn) {
            throw new NumberIsTooSmallException(PatriusMessages.INITIAL_COLUMN_AFTER_FINAL_COLUMN,
                    endColumn, startColumn, false);
        }
    }

    /**
     * Checks if the provided submatrix indices are valid for a given matrix.
     * <p>
     * Rows and columns are indicated counting from 0 to n-1.
     * </p>
     *
     * @param m
     *        the matrix
     * @param selectedRows
     *        the array of row indices
     * @param selectedColumns
     *        the array of column indices
     * @throws NullArgumentException
     *         if any of the provided index arrays is {@code null}
     * @throws NoDataException
     *         if any of the provided index arrays is empty (zero length)
     * @throws OutOfRangeException
     *         if any of the provided indices is not a valid row or column index
     */
    public static void checkSubMatrixIndex(final AnyMatrix m, final int[] selectedRows,
            final int[] selectedColumns) {
        checkRowIndices(m, selectedRows);
        checkColumnIndices(m, selectedColumns);
    }

    /**
     * Checks if the provided array contains duplicates.
     *
     * @param array
     *        the array to be checked
     * @throws MathIllegalArgumentException
     *         if the provided array contains any duplicates
     */
    public static void checkDuplicates(final int[] array) {
        final Set<Integer> elements = new HashSet<Integer>(array.length);
        for (final int i : array) {
            if (elements.contains(i)) {
                throw new MathIllegalArgumentException(PatriusMessages.DUPLICATED_ELEMENT, i);
            }
            elements.add(i);
        }
    }

    /**
     * Checks if the matrix is symmetric and throws an exception if that's not the case.
     *
     * @param matrix
     *        the matrix to be checked
     * @param absTol
     *        the absolute threshold above which two off-diagonal elements are considered to be
     *        different
     * @param relTol
     *        the relative threshold above which two off-diagonal elements are considered to be
     *        different
     * @throws NonSymmetricMatrixException
     *         if the provided matrix is not symmetric
     */
    public static void checkSymmetry(final RealMatrix matrix, final double absTol,
            final double relTol) {
        if (!matrix.isSymmetric(relTol, absTol)) {
            throw new NonSymmetricMatrixException(0, 0, relTol); // Not fully implemented
        }
    }

    /**
     * Builds a row permutation index array for a given matrix, starting with the preselected rows.
     * <p>
     * This methods creates a permutation index array by completing the supplied index array with
     * the missing row indices (in increasing order). The preselected indices must be valid row
     * indices for the supplied matrix. Any duplicate found in the supplied index array is simply
     * ignored.
     * </p>
     * <p>
     * <b>Usage examples, for a 5x4 matrix:</b>
     * </p>
     * <pre>
     * getRowPermutation(m, {2, 3}) => {2, 3, 0, 1, 4}
     * getRowPermutation(m, {0, 4, 1, 0}) => {0, 4, 1, 2, 3}
     * </pre>
     *
     * @param m
     *        the matrix
     * @param preSelectedRows
     *        the preselected row indices
     * @return the row permutation index array built
     */
    public static int[] getRowPermutationIndexArray(final AnyMatrix m, final int[] preSelectedRows) {
        // Row dimension of the matrix
        final int nbRows = m.getRowDimension();

        // Set containing every possible row index
        final SortedSet<Integer> rowIndices = IntStream.range(0, nbRows).boxed()
                .collect(Collectors.toCollection(TreeSet::new));

        // Array storing the permutation indices
        int index = 0;
        final int[] permutationIndices = new int[nbRows];

        // Check the preselected row indices and remove them from the row index list.
        // When a preselected index is successfully removed, add it to the permutation
        // index array (duplicated elements in the preselected row array are ignored).
        for (final int row : preSelectedRows) {
            checkRowIndex(m, row);
            final boolean removed = rowIndices.remove(row);
            if (removed) {
                permutationIndices[index++] = row;
            }
        }

        // Add the missing row indices at the end of the permutation index array.
        for (final int row : rowIndices) {
            permutationIndices[index++] = row;
        }

        // Return the permutation index array built
        return permutationIndices;
    }

    /**
     * Builds a column permutation index array for a given matrix, starting with the preselected
     * columns.
     * <p>
     * This methods creates a permutation index array by completing the supplied index array with
     * the missing column indices (in increasing order). The preselected indices must be valid
     * column indices for the supplied matrix. Any duplicate found in the supplied index array is
     * simply ignored.
     * </p>
     * <p>
     * <b>Usage examples, for a 4x5 matrix:</b>
     * </p>
     * 
     * <pre>
     * getColumnPermutation(m, {2, 3}) => {2, 3, 0, 1, 4}
     * getColumnPermutation(m, {0, 4, 1, 0}) => {0, 4, 1, 2, 3}
     * </pre>
     *
     * @param m
     *        the matrix
     * @param preSelectedColumns
     *        the preselected column indices
     * @return the column permutation index array built
     */
    public static int[] getColumnPermutationIndexArray(final AnyMatrix m,
            final int[] preSelectedColumns) {
        // Column dimension of the matrix
        final int nbColumns = m.getColumnDimension();

        // Set containing every possible column index
        final SortedSet<Integer> columnIndices = IntStream.range(0, nbColumns).boxed()
                .collect(Collectors.toCollection(TreeSet::new));

        // Array storing the permutation indices
        int index = 0;
        final int[] permutationIndices = new int[nbColumns];

        // Check the preselected column indices and remove them from the column index list.
        // When a preselected index is successfully removed, add it to the permutation
        // index array (duplicated elements in the preselected column array are ignored).
        for (final int column : preSelectedColumns) {
            checkColumnIndex(m, column);
            final boolean removed = columnIndices.remove(column);
            if (removed) {
                permutationIndices[index++] = column;
            }
        }

        // Add the missing column indices at the end of the permutation index array.
        for (final int column : columnIndices) {
            permutationIndices[index++] = column;
        }

        // Return the permutation index array built
        return permutationIndices;
    }

    /**
     * Check if the provided dimension is the one expected.
     *
     * @param expected
     *        the expected dimension
     * @param actual
     *        the actual dimension
     * @throws DimensionMismatchException
     *         if the provided dimension is not the one expected
     */
    public static void checkDimension(final int expected, final int actual) {
        if (expected != actual) {
            throw new DimensionMismatchException(actual, expected);
        }
    }

    /**
     * Check if matrices are addition compatible.
     *
     * @param left
     *        Left hand side matrix.
     * @param right
     *        Right hand side matrix.
     * @throws MatrixDimensionMismatchException
     *         if the matrices are not addition compatible.
     */
    public static void checkAdditionCompatible(final AnyMatrix left, final AnyMatrix right) {
        if ((left.getRowDimension() != right.getRowDimension())
                || (left.getColumnDimension() != right.getColumnDimension())) {
            throw new MatrixDimensionMismatchException(right.getRowDimension(),
                    right.getColumnDimension(), left.getRowDimension(), left.getColumnDimension());
        }
    }

    /**
     * Check if matrices are subtraction compatible
     *
     * @param left
     *        Left hand side matrix.
     * @param right
     *        Right hand side matrix.
     * @throws MatrixDimensionMismatchException
     *         if the matrices are not subtraction compatible.
     */
    public static void checkSubtractionCompatible(final AnyMatrix left, final AnyMatrix right) {
        checkAdditionCompatible(left, right);
    }

    /**
     * Check if matrices are multiplication compatible
     *
     * @param left
     *        Left hand side matrix.
     * @param right
     *        Right hand side matrix.
     * @throws DimensionMismatchException
     *         if {@code getColumnDimension(left) != getRowDimension(right)}
     */
    public static void checkMultiplicationCompatible(final AnyMatrix left, final AnyMatrix right) {
        checkMultiplicationCompatible(left, right, false);
    }

    /**
     * Check if matrices are multiplication compatible
     *
     * @param left
     *        Left hand side matrix.
     * @param right
     *        Right hand side matrix.
     * @param rightToTransposed
     *        indicate if the right matrix will be transposed before the operation
     * @throws DimensionMismatchException
     *         if {@code getColumnDimension(left) != getRowDimension(right)}
     */
    public static void checkMultiplicationCompatible(final AnyMatrix left, final AnyMatrix right,
            final boolean rightToTransposed) {

        if (!rightToTransposed) {
            if (left.getColumnDimension() != right.getRowDimension()) {
                throw new DimensionMismatchException(right.getRowDimension(),
                        left.getColumnDimension());
            }
        } else {
            if (left.getColumnDimension() != right.getColumnDimension()) {
                throw new DimensionMismatchException(right.getColumnDimension(),
                        left.getColumnDimension());
            }
        }
    }

    /**
     * Convert a {@link FieldMatrix}/{@link Fraction} matrix to a {@link RealMatrix}.
     *
     * @param m
     *        Matrix to convert.
     * @return the converted matrix.
     */
    public static Array2DRowRealMatrix fractionMatrixToRealMatrix(final FieldMatrix<Fraction> m) {
        final FractionMatrixConverter converter = new FractionMatrixConverter();
        m.walkInOptimizedOrder(converter);
        return converter.getConvertedMatrix();
    }

    /**
     * Convert a {@link FieldMatrix}/{@link BigFraction} matrix to a {@link RealMatrix}.
     *
     * @param m
     *        Matrix to convert.
     * @return the converted matrix.
     */
    public static Array2DRowRealMatrix bigFractionMatrixToRealMatrix(
            final FieldMatrix<BigFraction> m) {
        final BigFractionMatrixConverter converter = new BigFractionMatrixConverter();
        m.walkInOptimizedOrder(converter);
        return converter.getConvertedMatrix();
    }

    /**
     * Serialize a {@link RealVector}.
     * <p>
     * This method is intended to be called from within a private <code>writeObject</code> method
     * (after a call to <code>oos.defaultWriteObject()</code>) in a class that has a
     * {@link RealVector} field, which should be declared <code>transient</code>. This way, the
     * default handling does not serialize the vector (the {@link RealVector} interface is not
     * serializable by default) but this method does serialize it specifically.
     * </p>
     * <p>
     * The following example shows how a simple class with a name and a real vector should be
     * written:
     * 
     * <pre>
     * <code>
     * public class NamedVector implements Serializable {
     * 
     *     private final String name;
     *     private final transient RealVector coefficients;
     * 
     *     // omitted constructors, getters ...
     * 
     *     private void writeObject(ObjectOutputStream oos) throws IOException {
     *         oos.defaultWriteObject();  // takes care of name field
     *         MatrixUtils.serializeRealVector(coefficients, oos);
     *     }
     * 
     *     private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
     *         ois.defaultReadObject();  // takes care of name field
     *         MatrixUtils.deserializeRealVector(this, "coefficients", ois);
     *     }
     * 
     * }
     * </code>
     * </pre>
     * 
     * </p>
     *
     * @param vector
     *        real vector to serialize
     * @param oos
     *        stream where the real vector should be written
     * @exception IOException
     *            if object cannot be written to stream
     * @see #deserializeRealVector(ObjectInputStream)
     */
    public static void serializeRealVector(final RealVector vector, final ObjectOutputStream oos)
            throws IOException {
        final int n = vector.getDimension();
        oos.writeInt(n);
        for (int i = 0; i < n; ++i) {
            oos.writeDouble(vector.getEntry(i));
        }
    }

    /**
     * Deserialize a {@link RealVector} field in a class.
     * <p>
     * This method is intended to be called from within a private <code>readObject</code> method
     * (after a call to <code>ois.defaultReadObject()</code>) in a class that has a
     * {@link RealVector} field, which should be declared <code>transient</code>. This way, the
     * default handling does not deserialize the vector (the {@link RealVector} interface is not
     * serializable by default) but this method does deserialize it specifically.
     * </p>
     *
     * @param ois
     *        stream from which the real vector should be read
     * @return read real vector
     * @exception IOException
     *            if object cannot be read from the stream
     * @see #serializeRealVector(RealVector, ObjectOutputStream)
     */
    public static RealVector deserializeRealVector(final ObjectInputStream ois) throws IOException {
        // read the vector data
        final int n = ois.readInt();
        final double[] data = new double[n];
        for (int i = 0; i < n; ++i) {
            data[i] = ois.readDouble();
        }

        // create the instance
        return new ArrayRealVector(data, false);
    }

    /**
     * Serialize a {@link RealMatrix}.
     * <p>
     * This method is intended to be called from within a private <code>writeObject</code> method
     * (after a call to <code>oos.defaultWriteObject()</code>) in a class that has a
     * {@link RealMatrix} field, which should be declared <code>transient</code>. This way, the
     * default handling does not serialize the matrix (the {@link RealMatrix} interface is not
     * serializable by default) but this method does serialize it specifically.
     * </p>
     * <p>
     * The following example shows how a simple class with a name and a real matrix should be
     * written:
     * 
     * <pre>
     * <code>
     * public class NamedMatrix implements Serializable {
     * 
     *     private final String name;
     *     private final transient RealMatrix coefficients;
     * 
     *     // omitted constructors, getters ...
     * 
     *     private void writeObject(ObjectOutputStream oos) throws IOException {
     *         oos.defaultWriteObject();  // takes care of name field
     *         MatrixUtils.serializeRealMatrix(coefficients, oos);
     *     }
     * 
     *     private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
     *         ois.defaultReadObject();  // takes care of name field
     *         MatrixUtils.deserializeRealMatrix(this, "coefficients", ois);
     *     }
     * 
     * }
     * </code>
     * </pre>
     * 
     * </p>
     *
     * @param matrix
     *        real matrix to serialize
     * @param oos
     *        stream where the real matrix should be written
     * @exception IOException
     *            if object cannot be written to stream
     * @see #deserializeRealMatrix(ObjectInputStream)
     */
    public static void serializeRealMatrix(final RealMatrix matrix, final ObjectOutputStream oos)
            throws IOException {
        final int n = matrix.getRowDimension();
        final int m = matrix.getColumnDimension();
        oos.writeInt(n);
        oos.writeInt(m);
        for (int i = 0; i < n; ++i) {
            for (int j = 0; j < m; ++j) {
                oos.writeDouble(matrix.getEntry(i, j));
            }
        }
    }

    /**
     * Deserialize a {@link RealMatrix} field in a class.
     * <p>
     * This method is intended to be called from within a private <code>readObject</code> method
     * (after a call to <code>ois.defaultReadObject()</code>) in a class that has a
     * {@link RealMatrix} field, which should be declared <code>transient</code>. This way, the
     * default handling does not deserialize the matrix (the {@link RealMatrix} interface is not
     * serializable by default) but this method does deserialize it specifically.
     * </p>
     *
     * @param ois
     *        stream from which the real matrix should be read
     * @return read real matrix
     * @exception IOException
     *            if object cannot be read from the stream
     * @see #serializeRealMatrix(RealMatrix, ObjectOutputStream)
     */
    public static RealMatrix deserializeRealMatrix(final ObjectInputStream ois) throws IOException {
        // read the matrix data
        final int n = ois.readInt();
        final int m = ois.readInt();
        final double[][] data = new double[n][m];
        for (int i = 0; i < n; ++i) {
            final double[] dataI = data[i];
            for (int j = 0; j < m; ++j) {
                dataI[j] = ois.readDouble();
            }
        }

        // create the instance
        return new Array2DRowRealMatrix(data, false);
    }

    /**
     * Solve a system of composed of a Lower Triangular Matrix {@link RealMatrix}.
     * <p>
     * This method is called to solve systems of equations which are of the lower triangular form.
     * The matrix {@link RealMatrix} is assumed, though not checked, to be in lower triangular form.
     * The vector {@link RealVector} is overwritten with the solution. The matrix is checked that it
     * is square and its dimensions match the length of the vector.
     * </p>
     *
     * @param rm
     *        RealMatrix which is lower triangular
     * @param b
     *        RealVector this is overwritten
     * @throws DimensionMismatchException
     *         if the matrix and vector are not compatible
     * @throws NonSquareMatrixException
     *         if the matrix {@code rm} is not square
     * @throws MathArithmeticException
     *         if the absolute value of one of the diagonal coefficient of {@code rm} is lower than
     *         {@link Precision#SAFE_MIN}
     */
    public static void solveLowerTriangularSystem(final RealMatrix rm, final RealVector b) {
        if ((rm == null) || (b == null) || (rm.getRowDimension() != b.getDimension())) {
            throw new DimensionMismatchException((rm == null) ? 0 : rm.getRowDimension(),
                    (b == null) ? 0 : b.getDimension());
        }
        if (rm.getColumnDimension() != rm.getRowDimension()) {
            throw new NonSquareMatrixException(rm.getRowDimension(), rm.getColumnDimension());
        }
        final int rows = rm.getRowDimension();
        for (int i = 0; i < rows; i++) {
            final double diag = rm.getEntry(i, i);
            if (MathLib.abs(diag) < Precision.SAFE_MIN) {
                throw new MathArithmeticException(PatriusMessages.ZERO_DENOMINATOR);
            }
            final double bi = b.getEntry(i) / diag;
            b.setEntry(i, bi);
            for (int j = i + 1; j < rows; j++) {
                b.setEntry(j, b.getEntry(j) - bi * rm.getEntry(j, i));
            }
        }
    }

    /**
     * Solver a system composed of an Upper Triangular Matrix {@link RealMatrix}.
     * <p>
     * This method is called to solve systems of equations which are of the lower triangular form.
     * The matrix {@link RealMatrix} is assumed, though not checked, to be in upper triangular form.
     * The vector {@link RealVector} is overwritten with the solution. The matrix is checked that it
     * is square and its dimensions match the length of the vector.
     * </p>
     *
     * @param rm
     *        RealMatrix which is upper triangular
     * @param b
     *        RealVector this is overwritten
     * @throws DimensionMismatchException
     *         if the matrix and vector are not compatible
     * @throws NonSquareMatrixException
     *         if the matrix {@code rm} is not square
     * @throws MathArithmeticException
     *         if the absolute value of one of the diagonal coefficient of {@code rm} is lower than
     *         {@link Precision#SAFE_MIN}
     */
    public static void solveUpperTriangularSystem(final RealMatrix rm, final RealVector b) {
        if ((rm == null) || (b == null) || (rm.getRowDimension() != b.getDimension())) {
            throw new DimensionMismatchException((rm == null) ? 0 : rm.getRowDimension(),
                    (b == null) ? 0 : b.getDimension());
        }
        if (rm.getColumnDimension() != rm.getRowDimension()) {
            throw new NonSquareMatrixException(rm.getRowDimension(), rm.getColumnDimension());
        }
        final int rows = rm.getRowDimension();
        for (int i = rows - 1; i > -1; i--) {
            final double diag = rm.getEntry(i, i);
            if (MathLib.abs(diag) < Precision.SAFE_MIN) {
                throw new MathArithmeticException(PatriusMessages.ZERO_DENOMINATOR);
            }
            final double bi = b.getEntry(i) / diag;
            b.setEntry(i, bi);
            for (int j = i - 1; j > -1; j--) {
                b.setEntry(j, b.getEntry(j) - bi * rm.getEntry(j, i));
            }
        }
    }

    /**
     * Computes the inverse of the given matrix by splitting it into 4 sub-matrices.
     *
     * @param m
     *        Matrix whose inverse must be computed.
     * @param splitIndex
     *        Index that determines the "split" line and column. The element corresponding to this
     *        index will part of the upper-left sub-matrix.
     * @return the inverse of {@code m}.
     * @throws NonSquareMatrixException
     *         if {@code m} is not square.
     */
    public static RealMatrix blockInverse(final RealMatrix m, final int splitIndex) {

        final int n = m.getRowDimension();
        if (m.getColumnDimension() != n) {
            throw new NonSquareMatrixException(m.getRowDimension(), m.getColumnDimension());
        }

        final RealMatrix a = m.getSubMatrix(0, splitIndex, 0, splitIndex);

        final SingularValueDecomposition aDec = new SingularValueDecomposition(a);
        final DecompositionSolver aSolver = aDec.getSolver();
        if (!aSolver.isNonSingular()) {
            throw new SingularMatrixException();
        }

        final int splitIndex1 = splitIndex + 1;
        final RealMatrix d = m.getSubMatrix(splitIndex1, n - 1, splitIndex1, n - 1);

        final SingularValueDecomposition dDec = new SingularValueDecomposition(d);
        final DecompositionSolver dSolver = dDec.getSolver();
        if (!dSolver.isNonSingular()) {
            throw new SingularMatrixException();
        }
        final RealMatrix dInv = dSolver.getInverse();

        final RealMatrix b = m.getSubMatrix(0, splitIndex, splitIndex1, n - 1);
        final RealMatrix c = m.getSubMatrix(splitIndex1, n - 1, 0, splitIndex);

        final RealMatrix tmp1 = a.subtract(b.multiply(dInv).multiply(c));
        final SingularValueDecomposition tmp1Dec = new SingularValueDecomposition(tmp1);
        final DecompositionSolver tmp1Solver = tmp1Dec.getSolver();
        if (!tmp1Solver.isNonSingular()) {
            throw new SingularMatrixException();
        }

        final RealMatrix aInv = aSolver.getInverse();

        final RealMatrix tmp2 = d.subtract(c.multiply(aInv).multiply(b));
        final SingularValueDecomposition tmp2Dec = new SingularValueDecomposition(tmp2);
        final DecompositionSolver tmp2Solver = tmp2Dec.getSolver();
        if (!tmp2Solver.isNonSingular()) {
            throw new SingularMatrixException();
        }

        final RealMatrix result00 = tmp1Solver.getInverse();
        final RealMatrix result11 = tmp2Solver.getInverse();

        final RealMatrix result01 = aInv.multiply(b).multiply(result11).scalarMultiply(-1);
        final RealMatrix result10 = dInv.multiply(c).multiply(result00).scalarMultiply(-1);

        final RealMatrix result = new Array2DRowRealMatrix(n, n);
        result.setSubMatrix(result00.getData(false), 0, 0);
        result.setSubMatrix(result01.getData(false), 0, splitIndex1);
        result.setSubMatrix(result10.getData(false), splitIndex1, 0);
        result.setSubMatrix(result11.getData(false), splitIndex1, splitIndex1);

        return result;
    }

    /** Converter for {@link FieldMatrix}/{@link Fraction}. */
    private static class FractionMatrixConverter extends DefaultFieldMatrixPreservingVisitor<Fraction> {

        /** Converted array. */
        private double[][] data;

        /** Simple constructor. */
        public FractionMatrixConverter() {
            super(Fraction.ZERO);
        }

        /** {@inheritDoc} */
        @Override
        public void start(final int rows, final int columns, final int startRow, final int endRow,
                final int startColumn, final int endColumn) {
            this.data = new double[rows][columns];
        }

        /** {@inheritDoc} */
        @Override
        public void visit(final int row, final int column, final Fraction value) {
            this.data[row][column] = value.doubleValue();
        }

        /**
         * Get the converted matrix.
         *
         * @return the converted matrix.
         */
        private Array2DRowRealMatrix getConvertedMatrix() {
            return new Array2DRowRealMatrix(this.data, false);
        }

    }

    /** Converter for {@link FieldMatrix}/{@link BigFraction}. */
    private static class BigFractionMatrixConverter extends
            DefaultFieldMatrixPreservingVisitor<BigFraction> {

        /** Converted array. */
        private double[][] data;

        /** Simple constructor. */
        public BigFractionMatrixConverter() {
            super(BigFraction.ZERO);
        }

        /** {@inheritDoc} */
        @Override
        public void start(final int rows, final int columns, final int startRow, final int endRow,
                final int startColumn, final int endColumn) {
            this.data = new double[rows][columns];
        }

        /** {@inheritDoc} */
        @Override
        public void visit(final int row, final int column, final BigFraction value) {
            this.data[row][column] = value.doubleValue();
        }

        /**
         * Get the converted matrix.
         *
         * @return the converted matrix.
         */
        private Array2DRowRealMatrix getConvertedMatrix() {
            return new Array2DRowRealMatrix(this.data, false);
        }
    }
    // CHECKSTYLE: resume CommentRatio check
}
