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
 * VERSION:4.7:DM:DM-2766:18/05/2021:Evol. et corr. dans le package fr.cnes.sirius.patrius.math.linear (suite DM 2300) 
 * VERSION:4.7:DM:DM-2687:18/05/2021:Traitement de modèles de gravité, autres que les harmoniques sphériques
 * VERSION:4.6:FA:FA-2542:27/01/2021:[PATRIUS] Definition d'un champ de vue avec demi-angle de 180° 
 * VERSION:4.5.1:FA:FA-2540:04/08/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear
 * VERSION:4.5:DM:DM-2300:27/05/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.linear;

import java.io.Serializable;
import java.util.function.Function;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.NoDataException;
import fr.cnes.sirius.patrius.math.exception.NotPositiveException;
import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;
import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;

/**
 * Interface defining a real-valued matrix with basic algebraic operations.
 * <p>
 * Matrix element indexing is 0-based -- e.g., <code>getEntry(0, 0)</code> returns the element in
 * the first row, first column of the matrix.
 * </p>
 * 
 * @version $Id: RealMatrix.java 18108 2017-10-04 06:45:27Z bignon $
 */
public interface RealMatrix extends AnyMatrix, Serializable {

    /**
     * Creates a new matrix of the same type as this matrix.
     * <p>
     * The returned matrix is filled with zeros. Its size is determined by the specified row and
     * column dimensions, which must both be strictly positive. Additional constraints on the
     * dimensions may apply depending on the implementation (for example, symmetric matrices must be
     * square, which implies that the row and column dimensions must be equal).
     * </p>
     *
     * @param rowDimension
     *        the number of rows in the new matrix
     * @param columnDimension
     *        the number of columns in the new matrix
     * @return a new matrix of the same type as this matrix
     * @throws NotStrictlyPositiveException
     *         if row or column dimension is not positive.
     * @since 2.0
     */
    RealMatrix createMatrix(int rowDimension, int columnDimension);

    /**
     * Returns a deep copy of this matrix.
     *
     * @return a deep copy of this matrix
     */
    RealMatrix copy();

    /**
     * Returns the result of adding the matrix {@code m} to this matrix.
     *
     * @param m
     *        the matrix to be added
     * @return the matrix resulting from the addition {@code this}+{@code m}
     * @throws MatrixDimensionMismatchException
     *         if the matrix M is not the same size as this matrix
     */
    RealMatrix add(RealMatrix m);

    /**
     * Returns the result of subtracting the matrix {@code m} from this matrix.
     *
     * @param m
     *        the matrix to be subtracted
     * @return the matrix resulting from the subtraction {@code this}-{@code m}
     * @throws MatrixDimensionMismatchException
     *         if the matrix {@code m} is not the same size as this matrix
     */
    RealMatrix subtract(RealMatrix m);

    /**
     * Returns the result of adding a scalar {@code d} to the entries of this matrix.
     *
     * @param d
     *        the scalar value to be added to the entries of this matrix
     * @return the matrix resulting from the addition {@code this}+{@code d}
     */
    RealMatrix scalarAdd(double d);

    /**
     * Returns the result of multiplying the entries of this matrix by the scalar {@code d}.
     *
     * @param d
     *        the scalar value by which to multiply the entries of this matrix by
     * @return the matrix resulting from the product {@code this}&times;{@code d}
     */
    RealMatrix scalarMultiply(double d);

    /**
     * Returns the result of postmultiplying this matrix by the matrix {@code m}.
     *
     * @param m
     *        the matrix by which to postmultiply this matrix by
     * @return the matrix resulting from the product {@code this}&times;{@code m}
     * @throws DimensionMismatchException
     *         if the matrices are not multiplication compatible
     */
    RealMatrix multiply(RealMatrix m);

    /**
     * Returns the result of postmultiplying this matrix by the matrix {@code m} or its transpose
     * {@code m}<sup>T</sup>.
     *
     * @param m
     *        the matrix by which to postmultiply this matrix by
     * @param toTranspose
     *        whether to compute the product {@code this}&times;{@code m} ({@code toTranspose=false}
     *        ), or the product {@code this}&times;{@code m}<sup>T</sup> ({@code toTranspose=true})
     * @return the matrix resulting from the product {@code this}&times;{@code m} or {@code this}
     *         &times;{@code m}<sup>T</sup>
     * @throws DimensionMismatchException
     *         if the matrices are not multiplication compatible
     */
    RealMatrix multiply(RealMatrix m, boolean toTranspose);

    /**
     * Returns the result of postmultiplying this matrix by the matrix {@code m} or its transpose
     * {@code m}<sup>T</sup>, then by the scalar {@code d}.
     *
     * @param m
     *        the matrix by which to multiply this matrix by
     * @param toTranspose
     *        whether to compute the product {@code this}&times;{@code m}&times;{@code d} (
     *        {@code toTranspose=false}), or the product {@code this}&times;{@code m}
     *        <sup>T</sup>&times;{@code d} ({@code toTranspose=true})
     * @param d
     *        the scalar by which to multiply the resulting matrix by
     * @return the matrix resulting from the product {@code this}&times;{@code m}&times;{@code d} or
     *         {@code this}&times;{@code m}<sup>T</sup>&times;{@code d}
     * @throws DimensionMismatchException
     *         if the matrices are not multiplication compatible
     */
    RealMatrix multiply(RealMatrix m, boolean toTranspose, double d);

    /**
     * Returns the result of premultiplying this matrix by the matrix {@code m}.
     *
     * @param m
     *        the matrix M by which to premultiply this matrix by
     * @return the matrix resulting from the product {@code m}&times;{@code this}
     * @throws DimensionMismatchException
     *         if the matrices are not multiplication compatible
     */
    RealMatrix preMultiply(RealMatrix m);

    /**
     * Returns the the result of multiplying this matrix with itself {@code p} times.
     * <p>
     * The exponent {@code p} must be positive or equal to zero.<br>
     * This operation is only supported for square matrices.<br>
     * Depending on the underlying storage, numerical instabilities might occur for high powers.
     * </p>
     *
     * @param p
     *        the exponent {@code p} to which this matrix is to be raised
     * @return the matrix resulting from raising this matrix to the power of {@code p}
     * @throws NotPositiveException
     *         if the exponent {@code p} is negative
     * @throws NonSquareMatrixException
     *         if this matrix is not square
     */
    RealMatrix power(final int p);

    /**
     * Horizontally concatenates this matrix and another matrix {@code m}, placing it in the right
     * part of the concatenated matrix.
     *
     * @param m
     *        the matrix to be concatenated with this matrix
     * @return the concatenated matrix
     * @throws DimensionMismatchException
     *         if the matrices have different row dimensions
     */
    RealMatrix concatenateHorizontally(final RealMatrix m);

    /**
     * Horizontally concatenates this matrix and another matrix {@code m}, , placing it in the left
     * or right part of the concatenated matrix.
     * <p>
     * The way the two matrices are concatenated depends on the provided argument:<br>
     * The matrix {@code m} is placed in the right part of the concatenated matrix if
     * {@code rightConcatenation} is set to {@code true}, and in its left part if it is set to
     * {@code false}.
     * </p>
     * <p>
     * <b>Usage examples:</b>
     * </p>
     * 
     * <pre>
     * this.concatenateHorizontally(m, true)  => [this, m] 
     * 
     * this.concatenateHorizontally(m, false) => [m, this]
     * </pre>
     *
     * @param m
     *        the matrix to be concatenated with this matrix
     * @param rightConcatenation
     *        whether the matrix {@code m} is to be placed in the right ({@code true}) or left (
     *        {@code false}) part of the concatenated matrix
     * @return the concatenated matrix
     * @throws DimensionMismatchException
     *         if the matrices have different row dimensions
     */
    RealMatrix concatenateHorizontally(final RealMatrix m, final boolean rightConcatenation);

    /**
     * Vertically concatenates this matrix and another matrix {@code m}, placing it in the lower
     * part of the concatenated matrix.
     *
     * @param m
     *        the matrix to be concatenated with this matrix
     * @return the concatenated matrix
     * @throws DimensionMismatchException
     *         if the matrices have different column dimensions
     */
    RealMatrix concatenateVertically(final RealMatrix m);

    /**
     * Vertically concatenates this matrix and another matrix {@code m}, placing it in the lower or
     * upper part of the concatenated matrix.
     * <p>
     * The way the two matrices are concatenated depends on the provided argument:<br>
     * The matrix {@code m} is placed in the lower part of the concatenated matrix if
     * {@code lowerConcatenation} is set to {@code true}, and in its upper part if it is set to
     * {@code false}.
     * </p>
     * <p>
     * <b>Usage examples:</b>
     * </p>
     * 
     * <pre>
     * this.concatenateVertically(m, true)  => [this] 
     *                                         [   m]
     *                                              
     * this.concatenateVertically(m, false) => [   m]
     *                                         [this]
     * </pre>
     *
     * @param m
     *        the matrix to be concatenated with this matrix
     * @param lowerConcatenation
     *        whether the matrix {@code m} is to be placed in the lower ({@code true}) or upper (
     *        {@code false}) part of the concatenated matrix
     * @return the concatenated matrix
     * @throws DimensionMismatchException
     *         if the matrices have different row dimensions
     */
    RealMatrix concatenateVertically(final RealMatrix m, final boolean lowerConcatenation);

    /**
     * Diagonally concatenates this matrix and another matrix {@code m}, placing it in the lower
     * right part of the concatenated matrix.
     *
     * @param m
     *        the matrix to be concatenated with this matrix
     * @return the concatenated matrix
     */
    RealMatrix concatenateDiagonally(final RealMatrix m);

    /**
     * Diagonally concatenates this matrix and another matrix {@code m}, placing it in the lower
     * right or upper left part of the concatenated matrix.
     * <p>
     * The way the two matrices are concatenated depends on the provided argument:<br>
     * The matrix {@code m} is placed in the lower right part of the concatenated matrix if
     * {@code lowerRightConcatenation} is set to {@code true}, and in its upper left part if it is
     * set to {@code false}.
     * </p>
     * <p>
     * <b>Usage examples:</b>
     * </p>
     * 
     * <pre>
     * this.concatenateDiagonally(m, true)  => [this, 0] 
     *                                         [   0, m]
     *    
     * this.concatenateDiagonally(m, false) => [m,    0]
     *                                         [0, this]
     * </pre>
     * 
     * @param m
     *        the matrix to be concatenated with this matrix
     * @param lowerRightConcatenation
     *        whether the matrix {@code m} is to be placed in the lower right ({@code true}) or
     *        upper left ({@code false}) part of the concatenated matrix
     * @return the concatenated matrix
     */
    RealMatrix concatenateDiagonally(final RealMatrix m, final boolean lowerRightConcatenation);

    /**
     * Diagonally or anti-diagonally concatenates this matrix and another matrix {@code m}.
     * <p>
     * The way the two matrices are concatenated depends on the provided arguments:<br>
     * The matrix {@code m} is placed in the right part of the concatenated matrix if
     * {@code rightConcatenation} is set to {@code true}, and in its left part if it is set to
     * {@code false}. Similarly, the matrix {@code m} is placed in the lower part of the
     * concatenated matrix if {@code lowerConcatenation} is set to {@code true}, and in its upper
     * part if it is set to {@code false}. This matrix is then placed in the opposite part of the
     * concatenated matrix (as an example, if the provided matrix is placed in the upper left part,
     * this matrix will be placed in the lower right part, the remaining parts being filled with
     * zeros).
     * </p>
     * <p>
     * <b>Usage examples:</b>
     * </p>
     * 
     * <pre>
     * // Diagonal concatenation
     * this.concatenateDiagonally(m, true, true)   => [this, 0] 
     *                                                [   0, m]
     *                                              
     * this.concatenateDiagonally(m, false, false) => [m,    0]
     *                                                [0, this]
     *                          
     * // Anti-diagonal concatenation
     * this.concatenateDiagonally(m, false, true)  => [0, this]
     *                                                [m,    0]
     *                                               
     * this.concatenateDiagonally(m, true, false)  => [   0, m]
     *                                                [this, 0]
     * </pre>
     * 
     * @param m
     *        the matrix to be concatenated with this matrix
     * @param rightConcatenation
     *        whether the matrix {@code m} is to be placed in the right ({@code true}) or left (
     *        {@code false}) part of the concatenated matrix
     * @param lowerConcatenation
     *        whether the matrix {@code m} is to be placed in the lower ({@code true}) or upper (
     *        {@code false}) part of the concatenated matrix
     * @return the concatenated matrix
     */
    RealMatrix concatenateDiagonally(final RealMatrix m, final boolean rightConcatenation,
            final boolean lowerConcatenation);

    /**
     * Sets the decomposition builder the {@link RealMatrix#getInverse() getInverse()} method should
     * use by default when computing the inverse of the matrix.
     *
     * @param defaultDecompositionBuilder
     *        the default decomposition builder
     */
    void setDefaultDecomposition(
            final Function<RealMatrix, Decomposition> defaultDecompositionBuilder);

    /**
     * Gets the decomposition builder the {@link RealMatrix#getInverse() getInverse()} method uses
     * by default when computing the inverse of the matrix.
     *
     * @return the default decomposition builder
     */
    Function<RealMatrix, Decomposition> getDefaultDecomposition();

    /**
     * Gets the inverse (or pseudo-inverse) of this matrix using the default decomposition.
     * <p>
     * The default decomposition builder can be changed using the
     * {@link RealMatrix#setDefaultDecomposition(Function) setDefaultDecomposition} method.
     * </p>
     *
     * @return the inverse matrix
     * @throws SingularMatrixException
     *         if the matrix is singular
     * @see #getDefaultDecomposition()
     * @see #setDefaultDecomposition(Function)
     */
    RealMatrix getInverse();

    /**
     * Gets the inverse (or pseudo-inverse) of this matrix using the given decomposition algorithm.
     * <p>
     * The decomposition builder is a function capable of generating new instances of the
     * decomposition algorithm to be used for the computation of the inverse matrix (like the
     * {@linkplain QRDecomposition} or the {@linkplain EigenDecomposition}, for instance).
     * </p>
     *
     * @param decompositionBuilder
     *        the decomposition builder to use
     * @return the inverse matrix
     * @throws SingularMatrixException
     *         if the matrix is singular
     */
    RealMatrix getInverse(final Function<RealMatrix, Decomposition> decompositionBuilder);

    /**
     * Returns a 2D array containing the entries of the matrix.
     *
     * @return a 2D array containing the entries of the matrix
     */
    double[][] getData();

    /**
     * Returns a 2D array containing the entries of the matrix.
     * <p>
     * If {@code forceCopy} is {@code true}, the returned array is guaranteed to be free of
     * references to any internal data array (thus, can be safely modified). Otherwise, the returned
     * array may contain references to internal data arrays (for optimization purposes). Note that
     * setting {@code forceCopy} to {@code false} does not guarantee the returned array references
     * an internal data array. For instance, implementations that do not store the entries of the
     * matrix in a 2D array have to rebuild a new array each time this method is called, regardless
     * of this parameter.
     * </p>
     *
     * @param forceCopy
     *        if {@code true}, the entries of the matrix are systematically stored in a new array;
     *        otherwise the returned array may reference internal data arrays
     * @return a 2D array containing entries of the matrix
     */
    double[][] getData(final boolean forceCopy);

    /**
     * Returns the <a href="http://mathworld.wolfram.com/MatrixTrace.html"> trace</a> of the matrix
     * (the sum of the elements on the main diagonal).
     * <p>
     * <em>The trace of the matrix is only defined for square matrices.</em>
     * </p>
     * 
     * @return the trace of the matrix
     * @throws NonSquareMatrixException
     *         if the matrix is not square
     */
    double getTrace();

    /**
     * Returns the <a href="http://mathworld.wolfram.com/MaximumAbsoluteRowSumNorm.html"> maximum
     * absolute row sum norm</a> of the matrix.
     *
     * @return the maximum absolute row sum norm of the matrix
     */
    double getNorm();

    /**
     * Returns the <a href="http://mathworld.wolfram.com/FrobeniusNorm.html"> Frobenius norm</a> of
     * the matrix.
     *
     * @return the Frobenius norm of the matrix
     */
    double getFrobeniusNorm();

    /**
     * Returns the minimum value of the matrix.
     *
     * @return the minimum value of the matrix
     */
    double getMin();

    /**
     * Returns the maximum value of the matrix.
     *
     * @return the maximum value of the matrix
     */
    double getMax();

    /**
     * Gets a submatrix.
     * <p>
     * Rows and columns are indicated counting from 0 to n-1.
     * </p>
     *
     * @param startRow
     *        the initial row index
     * @param endRow
     *        the final row index (inclusive)
     * @param startColumn
     *        the initial column index
     * @param endColumn
     *        the final column index (inclusive)
     * @return the submatrix containing the data of the specified rows and columns
     * @throws OutOfRangeException
     *         if the row/column indices are not valid
     * @throws NumberIsTooSmallException
     *         if {@code endRow < startRow} or {@code endColumn < startColumn}.
     */
    RealMatrix getSubMatrix(int startRow, int endRow, int startColumn, int endColumn);

    /**
     * Gets a submatrix.
     * <p>
     * Rows and columns are indicated counting from 0 to n-1.
     * </p>
     *
     * @param selectedRows
     *        the selected row indices
     * @param selectedColumns
     *        the selected column indices
     * @return the submatrix containing the data in the specified rows and columns
     * @throws NullArgumentException
     *         if the row or column selections are {@code null}
     * @throws NoDataException
     *         if the row or column selections are empty (zero length)
     * @throws OutOfRangeException
     *         if the selected indices are not valid
     */
    RealMatrix getSubMatrix(int[] selectedRows, int[] selectedColumns);

    /**
     * Copies a submatrix into a given 2D array.
     * <p>
     * Rows and columns are indicated counting from 0 to n-1. The submatrix data is copied in the
     * upper-left part of the destination array. Elements which are not overwritten by the submatrix
     * data are left unchanged (for example, if the destination array is larger than the size of the
     * extracted submatrix).
     * </p>
     *
     * @param startRow
     *        the initial row index
     * @param endRow
     *        the final row index (inclusive)
     * @param startColumn
     *        the initial column index
     * @param endColumn
     *        the final column index (inclusive)
     * @param destination
     *        the 2D array where the submatrix data should be copied
     * @throws OutOfRangeException
     *         if the row/column indices are not valid
     * @throws NumberIsTooSmallException
     *         if {@code endRow < startRow} or {@code endColumn < startColumn}
     * @throws MatrixDimensionMismatchException
     *         if the destination array is too small
     */
    void copySubMatrix(int startRow, int endRow, int startColumn, int endColumn,
            double[][] destination);

    /**
     * Copies a submatrix into a given 2D array.
     * <p>
     * Rows and columns are indicated counting from 0 to n-1. The submatrix data is copied in the
     * upper-left part of the destination array, starting at the specified row/column indices.
     * Elements which are not overwritten by the submatrix data are left unchanged (for example, if
     * the destination array is larger than the size of the extracted submatrix).
     * </p>
     *
     * @param startRow
     *        the initial row index
     * @param endRow
     *        the final row index (inclusive)
     * @param startColumn
     *        the initial column index
     * @param endColumn
     *        the final column index (inclusive)
     * @param destination
     *        the 2D array where the submatrix data should be copied
     * @param startRowDest
     *        the initial row index of the destination array
     * @param startColumnDest
     *        the initial column index of the destination array
     * @throws OutOfRangeException
     *         if the row/column indices are not valid
     * @throws NumberIsTooSmallException
     *         if {@code endRow < startRow} or {@code endColumn < startColumn}.
     * @throws MatrixDimensionMismatchException
     *         if the destination array is too small
     */
    void copySubMatrix(int startRow, int endRow, int startColumn, int endColumn,
            double[][] destination, int startRowDest, int startColumnDest);

    /**
     * Copies a submatrix into a given 2D array.
     * <p>
     * Rows and columns are indicated counting from 0 to n-1. The submatrix data is copied in the
     * upper-left part of the destination array. Elements which are not overwritten by the submatrix
     * data are left unchanged (for example, if the destination array is larger than the size of the
     * extracted submatrix).
     * </p>
     *
     * @param selectedRows
     *        the selected row indices
     * @param selectedColumns
     *        the selected column indices
     * @param destination
     *        the 2D array where the submatrix data should be copied
     * @throws NullArgumentException
     *         if the row or column selections are {@code null}
     * @throws NoDataException
     *         if the row or column selections are empty (zero length)
     * @throws OutOfRangeException
     *         if the selected indices are not valid
     * @throws MatrixDimensionMismatchException
     *         if the destination array is too small
     */
    void copySubMatrix(int[] selectedRows, int[] selectedColumns, double[][] destination);

    /**
     * Copies a submatrix into a given 2D array.
     * <p>
     * Rows and columns are indicated counting from 0 to n-1. The submatrix data is copied in the
     * upper-left part of the destination array, starting at the specified row/column indices.
     * Elements which are not overwritten by the submatrix data are left unchanged (for example, if
     * the destination array is larger than the size of the extracted submatrix).
     * </p>
     *
     * @param selectedRows
     *        the selected row indices
     * @param selectedColumns
     *        the selected column indices
     * @param destination
     *        the 2D array where the submatrix data should be copied
     * @param startRowDest
     *        the initial row index of the destination array
     * @param startColumnDest
     *        the initial column index of the destination array
     * @throws NullArgumentException
     *         if the row or column selections are {@code null}
     * @throws NoDataException
     *         if the row or column selections are empty (zero length)
     * @throws OutOfRangeException
     *         if the selected indices are not valid
     * @throws MatrixDimensionMismatchException
     *         if the destination array is too small
     */
    void copySubMatrix(int[] selectedRows, int[] selectedColumns, double[][] destination,
            int startRowDest, int startColumnDest);

    /**
     * Replaces part of the matrix with a given submatrix, starting at the specified row and column.
     * <p>
     * Rows and columns are indicated counting from 0 to n-1.
     * <p/>
     * <p>
     * <b>Usage example:</b>
     * </p>
     * <pre>
     * // Initial matrix
     * matrix = [a<sub>00</sub>, a<sub>10</sub>, a<sub>20</sub>]
     *          [a<sub>10</sub>, a<sub>11</sub>, a<sub>21</sub>]
     *          [a<sub>20</sub>, a<sub>21</sub>, a<sub>22</sub>]
     * // Submatrix
     * subMatrix = [b<sub>00</sub>, b<sub>01</sub>]
     *             [b<sub>10</sub>, b<sub>11</sub>]
     *            
     * // Replace part of the initial matrix 
     * matrix.setSubMatrix(subMatrix, 1, 1) =>[a<sub>00</sub>, a<sub>10</sub>, a<sub>20</sub>]
     *                                        [a<sub>10</sub>, b<sub>00</sub>, b<sub>01</sub>]
     *                                        [a<sub>20</sub>, b<sub>10</sub>, b<sub>11</sub>]
     * </pre>
     * 
     * @param subMatrix
     *        the array containing the submatrix replacement data
     * @param row
     *        the row coordinate of the top, left element to be replaced
     * @param column
     *        the column coordinate of the top, left element to be replaced
     * @throws NullArgumentException
     *         if the input submatrix array is {@code null}
     * @throws NoDataException
     *         if the input submatrix array is empty
     * @throws DimensionMismatchException
     *         if the rows of the input submatrix array have different lengths
     * @throws OutOfRangeException
     *         if the input submatrix array does not fit into this matrix when starting from the
     *         specified top, left element
     * @since 2.0
     */
    void setSubMatrix(double[][] subMatrix, int row, int column);

    /**
     * Gets the entries of a given row as a row matrix.
     * <p>
     * Row indices start at 0.
     * </p>
     * 
     * @param row
     *        the index of the row to be fetched
     * @return the extracted row matrix
     * @throws OutOfRangeException
     *         if the specified row index is invalid
     */
    RealMatrix getRowMatrix(int row);

    /**
     * Replaces the entries of a given row with the entries of the specified row matrix.
     * <p>
     * Row indices start at 0.<br>
     * The provided matrix must have one row and the same number of columns as this matrix.
     * </p>
     * 
     * @param row
     *        the index of the row to be replaced
     * @param matrix
     *        the row matrix to be copied
     * @throws OutOfRangeException
     *         if the specified row index is invalid
     * @throws MatrixDimensionMismatchException
     *         if the row dimension of the provided matrix is not 1, or if its column dimension does
     *         not match the column dimension of this matrix
     */
    void setRowMatrix(int row, RealMatrix matrix);

    /**
     * Gets the entries of a given column as a column matrix.
     * <p>
     * Column indices start at 0.
     * </p>
     * 
     * @param column
     *        the index of the column to be fetched
     * @return the extracted column matrix
     * @throws OutOfRangeException
     *         if the specified column index is invalid
     */
    RealMatrix getColumnMatrix(int column);

    /**
     * Replaces the entries of a given column with the entries of the specified column matrix.
     * <p>
     * Column indices start at 0.<br>
     * The provided matrix must have one column and the same number of rows as this matrix.
     * </p>
     * 
     * @param column
     *        the index of the column to be replaced
     * @param matrix
     *        the column matrix to be copied
     * @throws OutOfRangeException
     *         if the specified column index is invalid
     * @throws MatrixDimensionMismatchException
     *         if the column dimension of the provided matrix is not 1, or if its row dimension does
     *         not match the row dimension of this matrix
     */
    void setColumnMatrix(int column, RealMatrix matrix);

    /**
     * Gets the entries of a given row as a vector.
     * <p>
     * Row indices start at 0.
     * </p>
     * 
     * @param row
     *        the index of the row to be fetched
     * @return the extracted row vector
     * @throws OutOfRangeException
     *         if the specified row index is invalid
     */
    RealVector getRowVector(int row);

    /**
     * Replaces the entries of a given row with the entries of the specified vector.
     * <p>
     * Row indices start at 0.<br>
     * The size of the provided vector must match the column dimension of this matrix.
     * </p>
     * 
     * @param row
     *        the index of the row to be replaced
     * @param vector
     *        the row vector to be copied
     * @throws OutOfRangeException
     *         if the specified row index is invalid
     * @throws MatrixDimensionMismatchException
     *         if the dimension of the provided vector does not match the column dimension of this
     *         matrix
     */
    void setRowVector(int row, RealVector vector);

    /**
     * Gets the entries of a given column as a vector.
     * <p>
     * Column indices start at 0.
     * </p>
     * 
     * @param column
     *        the index of the column to be fetched
     * @return the extracted column vector
     * @throws OutOfRangeException
     *         if the specified column index is invalid
     */
    RealVector getColumnVector(int column);

    /**
     * Replaces the entries of a given column with the entries of the specified vector.
     * <p>
     * Column indices start at 0.<br>
     * The size of the provided vector must match the row dimension of this matrix.
     * </p>
     * 
     * @param column
     *        the index of the column to be replaced
     * @param vector
     *        the column vector to be copied
     * @throws OutOfRangeException
     *         if the specified column index is invalid
     * @throws MatrixDimensionMismatchException
     *         if the dimension of the provided vector does not match the row dimension of this
     *         matrix
     */
    void setColumnVector(int column, RealVector vector);

    /**
     * Gets the entries of a given row.
     * <p>
     * Row indices start at 0.
     * </p>
     * 
     * @param row
     *        the index of the row to be fetched
     * @return the extracted row data
     * @throws OutOfRangeException
     *         if the specified row index is invalid
     */
    double[] getRow(int row);

    /**
     * Replaces the entries of a given row with the entries of the specified data array.
     * <p>
     * Row indices start at 0.<br>
     * The size of the provided data array must match the column dimension of this matrix.
     * </p>
     * 
     * @param row
     *        the index of the row to be replaced
     * @param array
     *        the row data array to be copied
     * @throws OutOfRangeException
     *         if the specified row index is invalid
     * @throws MatrixDimensionMismatchException
     *         if the length of the provided data array does not match the column dimension of this
     *         matrix
     */
    void setRow(int row, double[] array);

    /**
     * Gets the entries of a given column.
     * <p>
     * Column indices start at 0.
     * </p>
     * 
     * @param column
     *        the index of the column to be fetched
     * @return the extracted column data
     * @throws OutOfRangeException
     *         if the specified column index is invalid
     */
    double[] getColumn(int column);

    /**
     * Replaces the entries of a given column with the entries of the specified data array.
     * <p>
     * Column indices start at 0.<br>
     * The size of the provided data array must match the row dimension of this matrix.
     * </p>
     * 
     * @param column
     *        the index of the column to be replaced
     * @param array
     *        the column data array to be copied
     * @throws OutOfRangeException
     *         if the specified column index is invalid
     * @throws MatrixDimensionMismatchException
     *         if the length of the provided data array does not match the row dimension of this
     *         matrix
     */
    void setColumn(int column, double[] array);

    /**
     * Gets the entry at the specified row and column.
     * <p>
     * Row and column indices start at 0.
     * </p>
     * 
     * @param row
     *        the row index of entry to be fetched
     * @param column
     *        the column index of entry to be fetched
     * @return the matrix entry at the specified row and column
     * @throws OutOfRangeException
     *         if the row or column index is not valid
     */
    double getEntry(int row, int column);

    /**
     * Sets the entry at the specified row and column to a new value.
     * <p>
     * Row and column indices start at 0.
     * </p>
     * 
     * @param row
     *        the row index of entry to be set
     * @param column
     *        the column index of entry to be set
     * @param value
     *        the new value of the entry
     * @throws OutOfRangeException
     *         if the row or column index is not valid
     * @since 2.0
     */
    void setEntry(int row, int column, double value);

    /**
     * Adds (in place) a given value to the specified entry of this matrix.
     * <p>
     * Row and column indices start at 0.
     * </p>
     * 
     * @param row
     *        the row index of the entry to be modified
     * @param column
     *        the column index of the entry to be modified
     * @param increment
     *        the value to add to the matrix entry
     * @throws OutOfRangeException
     *         if the row or column index is not valid
     * @since 2.0
     */
    void addToEntry(int row, int column, double increment);

    /**
     * Multiplies (in place) the specified entry of {@code this} matrix by a given value.
     * <p>
     * Row and column indices start at 0.
     * </p>
     * 
     * @param row
     *        the row index of the entry to be modified
     * @param column
     *        the column index of the entry to be modified
     * @param factor
     *        the multiplication factor for the matrix entry
     * @throws OutOfRangeException
     *         if the row or column index is not valid
     * @since 2.0
     */
    void multiplyEntry(int row, int column, double factor);

    /**
     * Returns the transpose of this matrix.
     * 
     * @return the transpose of this matrix
     */
    RealMatrix transpose();

    /**
     * Returns the transpose of this matrix.
     * <p>
     * If {@code forceCopy} is {@code true}, the returned matrix is guaranteed to be a new instance,
     * which can be modified without any risk of impacting the current instance. Otherwise, this
     * method may simply return the current instance when the matrix is its own transpose (symmetric
     * matrix).
     * </p>
     * 
     * @param forceCopy
     *        if {@code true}, the transpose of the matrix is systematically stored in a new matrix;
     *        otherwise the method may return the current instance when the matrix is its own
     *        transpose
     * @return the transpose of this matrix
     */
    RealMatrix transpose(final boolean forceCopy);

    /**
     * Is this an orthogonal matrix?
     * <p>
     * This method indicates if this matrix is orthogonal, taking into account the specified
     * tolerances.<br>
     * To do so, the method checks if the columns of the matrix form an orthonormal set (that is,
     * the column vectors are orthogonal to each other and their norm is numerically equal to 1).
     * </p>
     * <p>
     * <em>This method systematically returns {@code false} for non-square matrices.</em>
     * </p>
     * 
     * @param normalityThreshold
     *        the relative tolerance to take into account when checking the normality of the the
     *        column vectors
     * @param orthogonalityThreshold
     *        the absolute tolerance to take into account when checking the mutual orthogonality of
     *        the the column vectors
     * @return {@code true} if this is an orthogonal matrix, {@code false} otherwise
     */
    boolean isOrthogonal(final double normalityThreshold, final double orthogonalityThreshold);

    /**
     * Is this a diagonal matrix?
     * <p>
     * This method indicates if the matrix is diagonal, taking into account the specified tolerance.
     * <br>
     * To do so, the method checks if the off-diagonal elements are numerically equal to zero.
     * </p>
     * <p>
     * <em>This method systematically returns {@code false} for non-square matrices.</em>
     * </p>
     * 
     * @param absoluteTolerance
     *        the absolute threshold above which the absolute value of an off-diagonal element is
     *        considered to be strictly positive
     * @return {@code true} if this is a diagonal matrix, {@code false} otherwise
     */
    boolean isDiagonal(final double absoluteTolerance);

    /**
     * Is this an invertible matrix?
     * <p>
     * This method indicates if the matrix is invertible, taking into account the specified
     * tolerance.<br>
     * To do so, the method checks the linear independence between the column vectors of the matrix.
     * Two columns are considered to be linearly dependent if their dot product is numerically equal
     * to the product of their norm.
     * </p>
     * <p>
     * <em>This method systematically returns {@code false} for non-square matrices.</em>
     * </p>
     * 
     * @param relativeTolerance
     *        the relative tolerance to take into account when checking the independence of the
     *        column vectors
     * @return {@code true} if this is an invertible matrix, {@code false} otherwise
     */
    boolean isInvertible(final double relativeTolerance);

    /**
     * Is this a symmetric matrix?
     * <p>
     * This method indicates if the matrix is symmetric.<br>
     * To do so, the method checks that symmetric off-diagonal elements are numerically equal. Two
     * elements are considered to have different values if their absolute and relative differences
     * are both above the default tolerances.
     * </p>
     * <p>
     * <em>This method systematically returns {@code false} for non-square matrices.</em>
     * </p>
     * 
     * @return {@code true} if this is a symmetric matrix, {@code false} otherwise
     */
    boolean isSymmetric();

    /**
     * Is this a symmetric matrix?
     * <p>
     * This method indicates if the matrix is symmetric.<br>
     * To do so, the method checks that symmetric off-diagonal elements are numerically equal. Two
     * elements are considered to have different values if their relative difference is above the
     * specified tolerance.
     * </p>
     * <p>
     * <em>This method systematically returns {@code false} for non-square matrices.</em>
     * </p>
     * 
     * @param relativeTolerance
     *        the relative tolerance to take into account when comparing off-diagonal elements
     * @return {@code true} if this is a symmetric matrix, {@code false} otherwise
     */
    boolean isSymmetric(double relativeTolerance);

    /**
     * Is this a symmetric matrix?
     * <p>
     * This method indicates if the matrix is symmetric.<br>
     * To do so, the method checks that symmetric off-diagonal elements are numerically equal. Two
     * elements are considered to have different values if their absolute and relative differences
     * are both above the specified tolerances.
     * </p>
     * <p>
     * <em>This method systematically returns {@code false} for non-square matrices.</em>
     * </p>
     * 
     * @param relativeTolerance
     *        the relative tolerance to take into account when comparing off-diagonal elements
     * @param absoluteTolerance
     *        the absolute tolerance to take into account when comparing off-diagonal elements
     * @return {@code true} if this is a symmetric matrix, {@code false} otherwise
     */
    boolean isSymmetric(double relativeTolerance, double absoluteTolerance);

    /**
     * Is this a antisymmetric matrix?
     * <p>
     * This method indicates if the matrix is antisymmetric.<br>
     * To do so, the method checks that symmetric off-diagonal elements have numerically equal
     * values but opposite signs, and that diagonal elements are numerically equal to zero. Two
     * off-diagonal elements are considered to have different values if their absolute and relative
     * differences are both above the specified tolerances. Diagonal elements are considered to be
     * different from zero if their absolute value is greater than the specified absolute tolerance.
     * </p>
     * <p>
     * <em>This method systematically returns {@code false} for non-square matrices.</em>
     * </p>
     * 
     * @param relativeTolerance
     *        the relative tolerance to take into account when comparing off-diagonal elements
     * @param absoluteTolerance
     *        the absolute tolerance to take into account when comparing off-diagonal elements, and
     *        when checking if diagonal elements are
     *        equal to zero
     * @return {@code true} if this is an antisymmetric matrix, {@code false} otherwise
     */
    boolean isAntisymmetric(double relativeTolerance, double absoluteTolerance);

    /**
     * Returns the result of postmultiplying this matrix by the vector {@code v}.
     * 
     * @param v
     *        the vector by which to multiply this matrix by
     * @return the column vector resulting from the product {@code this}&times;{@code v}
     * @throws DimensionMismatchException
     *         if the length of provided array does not match the column dimension of this matrix
     */
    double[] operate(double[] v);

    /**
     * Returns the result of postmultiplying this matrix by the vector {@code v}.
     * 
     * @param v
     *        the vector by which to multiply this matrix by
     * @return the column vector resulting from the product {@code this}&times;{@code v}
     * @throws DimensionMismatchException
     *         if the dimension of the provided vector does not match the column dimension of this
     *         matrix
     */
    RealVector operate(RealVector v);

    /**
     * Returns the result of premultiplying this matrix by the vector {@code v}.
     * 
     * @param v
     *        the row vector by which to premultiply this matrix by
     * @return the row vector resulting from the product {@code v}&times;{@code this}
     * @throws DimensionMismatchException
     *         if the length of the provided array does not match the row dimension of this matrix
     */
    double[] preMultiply(double[] v);

    /**
     * Returns the result of premultiplying this matrix by the vector {@code v}.
     * 
     * @param v
     *        the vector by which to premultiply this matrix by
     * @return the row vector resulting from the product {@code v}&times;{@code this}
     * @throws DimensionMismatchException
     *         if the dimension of the provided vector does not match the row dimension of this
     *         matrix
     */
    RealVector preMultiply(RealVector v);

    /**
     * Visits (and possibly change) all matrix entries in row order.
     * <p>
     * Row order starts at upper left element, iterating through all elements of a row from left to
     * right before going to the leftmost element of the next row.
     * </p>
     * 
     * @param visitor
     *        the visitor used to process all matrix entries
     * @return the value returned by {@link RealMatrixChangingVisitor#end()} at the end of the walk
     * @see #walkInRowOrder(RealMatrixPreservingVisitor)
     * @see #walkInRowOrder(RealMatrixChangingVisitor, int, int, int, int)
     * @see #walkInRowOrder(RealMatrixPreservingVisitor, int, int, int, int)
     * @see #walkInColumnOrder(RealMatrixChangingVisitor)
     * @see #walkInColumnOrder(RealMatrixPreservingVisitor)
     * @see #walkInColumnOrder(RealMatrixChangingVisitor, int, int, int, int)
     * @see #walkInColumnOrder(RealMatrixPreservingVisitor, int, int, int, int)
     * @see #walkInOptimizedOrder(RealMatrixChangingVisitor)
     * @see #walkInOptimizedOrder(RealMatrixPreservingVisitor)
     * @see #walkInOptimizedOrder(RealMatrixChangingVisitor, int, int, int, int)
     * @see #walkInOptimizedOrder(RealMatrixPreservingVisitor, int, int, int, int)
     */
    double walkInRowOrder(RealMatrixChangingVisitor visitor);

    /**
     * Visits (but don't change) all matrix entries in row order.
     * <p>
     * Row order starts at upper left element, iterating through all elements of a row from left to
     * right before going to the leftmost element of the next row.
     * </p>
     * 
     * @param visitor
     *        the visitor used to process all matrix entries
     * @return the value returned by {@link RealMatrixPreservingVisitor#end()} at the end of the
     *         walk
     * @see #walkInRowOrder(RealMatrixChangingVisitor)
     * @see #walkInRowOrder(RealMatrixChangingVisitor, int, int, int, int)
     * @see #walkInRowOrder(RealMatrixPreservingVisitor, int, int, int, int)
     * @see #walkInColumnOrder(RealMatrixChangingVisitor)
     * @see #walkInColumnOrder(RealMatrixPreservingVisitor)
     * @see #walkInColumnOrder(RealMatrixChangingVisitor, int, int, int, int)
     * @see #walkInColumnOrder(RealMatrixPreservingVisitor, int, int, int, int)
     * @see #walkInOptimizedOrder(RealMatrixChangingVisitor)
     * @see #walkInOptimizedOrder(RealMatrixPreservingVisitor)
     * @see #walkInOptimizedOrder(RealMatrixChangingVisitor, int, int, int, int)
     * @see #walkInOptimizedOrder(RealMatrixPreservingVisitor, int, int, int, int)
     */
    double walkInRowOrder(RealMatrixPreservingVisitor visitor);

    /**
     * Visits (and possibly change) some matrix entries in row order.
     * <p>
     * Row order starts at upper left element, iterating through all elements of a row from left to
     * right before going to the leftmost element of the next row.
     * </p>
     * 
     * @param visitor
     *        the visitor used to process all matrix entries
     * @param startRow
     *        the initial row index
     * @param endRow
     *        the final row index (inclusive)
     * @param startColumn
     *        the initial column index
     * @param endColumn
     *        the final column index
     * @return the value returned by {@link RealMatrixChangingVisitor#end()} at the end of the walk
     * @throws OutOfRangeException
     *         if the specified indices are not valid
     * @throws NumberIsTooSmallException
     *         if {@code endRow < startRow} or {@code endColumn < startColumn}
     * @see #walkInRowOrder(RealMatrixChangingVisitor)
     * @see #walkInRowOrder(RealMatrixPreservingVisitor)
     * @see #walkInRowOrder(RealMatrixPreservingVisitor, int, int, int, int)
     * @see #walkInColumnOrder(RealMatrixChangingVisitor)
     * @see #walkInColumnOrder(RealMatrixPreservingVisitor)
     * @see #walkInColumnOrder(RealMatrixChangingVisitor, int, int, int, int)
     * @see #walkInColumnOrder(RealMatrixPreservingVisitor, int, int, int, int)
     * @see #walkInOptimizedOrder(RealMatrixChangingVisitor)
     * @see #walkInOptimizedOrder(RealMatrixPreservingVisitor)
     * @see #walkInOptimizedOrder(RealMatrixChangingVisitor, int, int, int, int)
     * @see #walkInOptimizedOrder(RealMatrixPreservingVisitor, int, int, int, int)
     */
    double walkInRowOrder(RealMatrixChangingVisitor visitor, int startRow, int endRow,
            int startColumn, int endColumn);

    /**
     * Visits (but don't change) some matrix entries in row order.
     * <p>
     * Row order starts at upper left element, iterating through all elements of a row from left to
     * right before going to the leftmost element of the next row.
     * </p>
     * 
     * @param visitor
     *        the visitor used to process all matrix entries
     * @param startRow
     *        the initial row index
     * @param endRow
     *        the final row index (inclusive)
     * @param startColumn
     *        the initial column index
     * @param endColumn
     *        the final column index
     * @return the value returned by {@link RealMatrixPreservingVisitor#end()} at the end of the
     *         walk
     * @throws OutOfRangeException
     *         if the specified indices are not valid
     * @throws NumberIsTooSmallException
     *         if {@code endRow < startRow} or {@code endColumn < startColumn}
     * @see #walkInRowOrder(RealMatrixChangingVisitor)
     * @see #walkInRowOrder(RealMatrixPreservingVisitor)
     * @see #walkInRowOrder(RealMatrixChangingVisitor, int, int, int, int)
     * @see #walkInColumnOrder(RealMatrixChangingVisitor)
     * @see #walkInColumnOrder(RealMatrixPreservingVisitor)
     * @see #walkInColumnOrder(RealMatrixChangingVisitor, int, int, int, int)
     * @see #walkInColumnOrder(RealMatrixPreservingVisitor, int, int, int, int)
     * @see #walkInOptimizedOrder(RealMatrixChangingVisitor)
     * @see #walkInOptimizedOrder(RealMatrixPreservingVisitor)
     * @see #walkInOptimizedOrder(RealMatrixChangingVisitor, int, int, int, int)
     * @see #walkInOptimizedOrder(RealMatrixPreservingVisitor, int, int, int, int)
     */
    double walkInRowOrder(RealMatrixPreservingVisitor visitor, int startRow, int endRow,
            int startColumn, int endColumn);

    /**
     * Visits (and possibly change) all matrix entries in column order.
     * <p>
     * Column order starts at upper left element, iterating through all elements of a column from
     * top to bottom before going to the topmost element of the next column.
     * </p>
     * 
     * @param visitor
     *        the visitor used to process all matrix entries
     * @return the value returned by {@link RealMatrixChangingVisitor#end()} at the end of the walk
     * @see #walkInRowOrder(RealMatrixChangingVisitor)
     * @see #walkInRowOrder(RealMatrixPreservingVisitor)
     * @see #walkInRowOrder(RealMatrixChangingVisitor, int, int, int, int)
     * @see #walkInRowOrder(RealMatrixPreservingVisitor, int, int, int, int)
     * @see #walkInColumnOrder(RealMatrixPreservingVisitor)
     * @see #walkInColumnOrder(RealMatrixChangingVisitor, int, int, int, int)
     * @see #walkInColumnOrder(RealMatrixPreservingVisitor, int, int, int, int)
     * @see #walkInOptimizedOrder(RealMatrixChangingVisitor)
     * @see #walkInOptimizedOrder(RealMatrixPreservingVisitor)
     * @see #walkInOptimizedOrder(RealMatrixChangingVisitor, int, int, int, int)
     * @see #walkInOptimizedOrder(RealMatrixPreservingVisitor, int, int, int, int)
     */
    double walkInColumnOrder(RealMatrixChangingVisitor visitor);

    /**
     * Visits (but don't change) all matrix entries in column order.
     * <p>
     * Column order starts at upper left element, iterating through all elements of a column from
     * top to bottom before going to the topmost element of the next column.
     * </p>
     * 
     * @param visitor
     *        the visitor used to process all matrix entries
     * @return the value returned by {@link RealMatrixPreservingVisitor#end()} at the end of the
     *         walk
     * @see #walkInRowOrder(RealMatrixChangingVisitor)
     * @see #walkInRowOrder(RealMatrixPreservingVisitor)
     * @see #walkInRowOrder(RealMatrixChangingVisitor, int, int, int, int)
     * @see #walkInRowOrder(RealMatrixPreservingVisitor, int, int, int, int)
     * @see #walkInColumnOrder(RealMatrixChangingVisitor)
     * @see #walkInColumnOrder(RealMatrixChangingVisitor, int, int, int, int)
     * @see #walkInColumnOrder(RealMatrixPreservingVisitor, int, int, int, int)
     * @see #walkInOptimizedOrder(RealMatrixChangingVisitor)
     * @see #walkInOptimizedOrder(RealMatrixPreservingVisitor)
     * @see #walkInOptimizedOrder(RealMatrixChangingVisitor, int, int, int, int)
     * @see #walkInOptimizedOrder(RealMatrixPreservingVisitor, int, int, int, int)
     */
    double walkInColumnOrder(RealMatrixPreservingVisitor visitor);

    /**
     * Visits (and possibly change) some matrix entries in column order.
     * <p>
     * Column order starts at upper left element, iterating through all elements of a column from
     * top to bottom before going to the topmost element of the next column.
     * </p>
     * 
     * @param visitor
     *        the visitor used to process all matrix entries
     * @param startRow
     *        the initial row index
     * @param endRow
     *        the final row index (inclusive)
     * @param startColumn
     *        the initial column index
     * @param endColumn
     *        the final column index
     * @return the value returned by {@link RealMatrixChangingVisitor#end()} at the end of the walk
     * @throws OutOfRangeException
     *         if the specified indices are not valid
     * @throws NumberIsTooSmallException
     *         if {@code endRow < startRow} or {@code endColumn < startColumn}
     * @see #walkInRowOrder(RealMatrixChangingVisitor)
     * @see #walkInRowOrder(RealMatrixPreservingVisitor)
     * @see #walkInRowOrder(RealMatrixChangingVisitor, int, int, int, int)
     * @see #walkInRowOrder(RealMatrixPreservingVisitor, int, int, int, int)
     * @see #walkInColumnOrder(RealMatrixChangingVisitor)
     * @see #walkInColumnOrder(RealMatrixPreservingVisitor)
     * @see #walkInColumnOrder(RealMatrixPreservingVisitor, int, int, int, int)
     * @see #walkInOptimizedOrder(RealMatrixChangingVisitor)
     * @see #walkInOptimizedOrder(RealMatrixPreservingVisitor)
     * @see #walkInOptimizedOrder(RealMatrixChangingVisitor, int, int, int, int)
     * @see #walkInOptimizedOrder(RealMatrixPreservingVisitor, int, int, int, int)
     */
    double walkInColumnOrder(RealMatrixChangingVisitor visitor, int startRow, int endRow,
            int startColumn, int endColumn);

    /**
     * Visits (but don't change) some matrix entries in column order.
     * <p>
     * Column order starts at upper left element, iterating through all elements of a column from
     * top to bottom before going to the topmost element of the next column.
     * </p>
     * 
     * @param visitor
     *        the visitor used to process all matrix entries
     * @param startRow
     *        the initial row index
     * @param endRow
     *        the final row index (inclusive)
     * @param startColumn
     *        the initial column index
     * @param endColumn
     *        the final column index
     * @return the value returned by {@link RealMatrixPreservingVisitor#end()} at the end of the
     *         walk
     * @throws OutOfRangeException
     *         if the specified indices are not valid
     * @throws NumberIsTooSmallException
     *         if {@code endRow < startRow} or {@code endColumn < startColumn}
     * @see #walkInRowOrder(RealMatrixChangingVisitor)
     * @see #walkInRowOrder(RealMatrixPreservingVisitor)
     * @see #walkInRowOrder(RealMatrixChangingVisitor, int, int, int, int)
     * @see #walkInRowOrder(RealMatrixPreservingVisitor, int, int, int, int)
     * @see #walkInColumnOrder(RealMatrixChangingVisitor)
     * @see #walkInColumnOrder(RealMatrixPreservingVisitor)
     * @see #walkInColumnOrder(RealMatrixChangingVisitor, int, int, int, int)
     * @see #walkInOptimizedOrder(RealMatrixChangingVisitor)
     * @see #walkInOptimizedOrder(RealMatrixPreservingVisitor)
     * @see #walkInOptimizedOrder(RealMatrixChangingVisitor, int, int, int, int)
     * @see #walkInOptimizedOrder(RealMatrixPreservingVisitor, int, int, int, int)
     */
    double walkInColumnOrder(RealMatrixPreservingVisitor visitor, int startRow, int endRow,
            int startColumn, int endColumn);

    /**
     * Visits (and possibly change) all matrix entries using the fastest possible order.
     * <p>
     * The fastest walking order depends on the exact matrix class. It may be different from
     * traditional row or column orders.
     * </p>
     * 
     * @param visitor
     *        the visitor used to process all matrix entries
     * @return the value returned by {@link RealMatrixChangingVisitor#end()} at the end of the walk
     * @see #walkInRowOrder(RealMatrixChangingVisitor)
     * @see #walkInRowOrder(RealMatrixPreservingVisitor)
     * @see #walkInRowOrder(RealMatrixChangingVisitor, int, int, int, int)
     * @see #walkInRowOrder(RealMatrixPreservingVisitor, int, int, int, int)
     * @see #walkInColumnOrder(RealMatrixChangingVisitor)
     * @see #walkInColumnOrder(RealMatrixPreservingVisitor)
     * @see #walkInColumnOrder(RealMatrixChangingVisitor, int, int, int, int)
     * @see #walkInColumnOrder(RealMatrixPreservingVisitor, int, int, int, int)
     * @see #walkInOptimizedOrder(RealMatrixPreservingVisitor)
     * @see #walkInOptimizedOrder(RealMatrixChangingVisitor, int, int, int, int)
     * @see #walkInOptimizedOrder(RealMatrixPreservingVisitor, int, int, int, int)
     */
    double walkInOptimizedOrder(RealMatrixChangingVisitor visitor);

    /**
     * Visits (but don't change) all matrix entries using the fastest possible order.
     * <p>
     * The fastest walking order depends on the exact matrix class. It may be different from
     * traditional row or column orders.
     * </p>
     * 
     * @param visitor
     *        the visitor used to process all matrix entries
     * @return the value returned by {@link RealMatrixPreservingVisitor#end()} at the end of the
     *         walk
     * @see #walkInRowOrder(RealMatrixChangingVisitor)
     * @see #walkInRowOrder(RealMatrixPreservingVisitor)
     * @see #walkInRowOrder(RealMatrixChangingVisitor, int, int, int, int)
     * @see #walkInRowOrder(RealMatrixPreservingVisitor, int, int, int, int)
     * @see #walkInColumnOrder(RealMatrixChangingVisitor)
     * @see #walkInColumnOrder(RealMatrixPreservingVisitor)
     * @see #walkInColumnOrder(RealMatrixChangingVisitor, int, int, int, int)
     * @see #walkInColumnOrder(RealMatrixPreservingVisitor, int, int, int, int)
     * @see #walkInOptimizedOrder(RealMatrixChangingVisitor)
     * @see #walkInOptimizedOrder(RealMatrixChangingVisitor, int, int, int, int)
     * @see #walkInOptimizedOrder(RealMatrixPreservingVisitor, int, int, int, int)
     */
    double walkInOptimizedOrder(RealMatrixPreservingVisitor visitor);

    /**
     * Visits (and possibly change) some matrix entries using the fastest possible order.
     * <p>
     * The fastest walking order depends on the exact matrix class. It may be different from
     * traditional row or column orders.
     * </p>
     * 
     * @param visitor
     *        the visitor used to process all matrix entries
     * @param startRow
     *        the initial row index
     * @param endRow
     *        the final row index (inclusive)
     * @param startColumn
     *        the initial column index
     * @param endColumn
     *        the final column index (inclusive)
     * @return the value returned by {@link RealMatrixChangingVisitor#end()} at the end of the walk
     * @throws OutOfRangeException
     *         if the specified indices are not valid
     * @throws NumberIsTooSmallException
     *         if {@code endRow < startRow} or {@code endColumn < startColumn}
     * @see #walkInRowOrder(RealMatrixChangingVisitor)
     * @see #walkInRowOrder(RealMatrixPreservingVisitor)
     * @see #walkInRowOrder(RealMatrixChangingVisitor, int, int, int, int)
     * @see #walkInRowOrder(RealMatrixPreservingVisitor, int, int, int, int)
     * @see #walkInColumnOrder(RealMatrixChangingVisitor)
     * @see #walkInColumnOrder(RealMatrixPreservingVisitor)
     * @see #walkInColumnOrder(RealMatrixChangingVisitor, int, int, int, int)
     * @see #walkInColumnOrder(RealMatrixPreservingVisitor, int, int, int, int)
     * @see #walkInOptimizedOrder(RealMatrixChangingVisitor)
     * @see #walkInOptimizedOrder(RealMatrixPreservingVisitor)
     * @see #walkInOptimizedOrder(RealMatrixPreservingVisitor, int, int, int, int)
     */
    double walkInOptimizedOrder(RealMatrixChangingVisitor visitor, int startRow, int endRow,
            int startColumn, int endColumn);

    /**
     * Visits (but don't change) some matrix entries using the fastest possible order.
     * <p>
     * The fastest walking order depends on the exact matrix class. It may be different from
     * traditional row or column orders.
     * </p>
     * 
     * @param visitor
     *        the visitor used to process all matrix entries
     * @param startRow
     *        the initial row index
     * @param endRow
     *        the final row index (inclusive)
     * @param startColumn
     *        the initial column index
     * @param endColumn
     *        the final column index (inclusive)
     * @return the value returned by {@link RealMatrixPreservingVisitor#end()} at the end of the
     *         walk
     * @throws OutOfRangeException
     *         if the specified indices are not valid
     * @throws NumberIsTooSmallException
     *         if {@code endRow < startRow} or {@code endColumn < startColumn}
     * @see #walkInRowOrder(RealMatrixChangingVisitor)
     * @see #walkInRowOrder(RealMatrixPreservingVisitor)
     * @see #walkInRowOrder(RealMatrixChangingVisitor, int, int, int, int)
     * @see #walkInRowOrder(RealMatrixPreservingVisitor, int, int, int, int)
     * @see #walkInColumnOrder(RealMatrixChangingVisitor)
     * @see #walkInColumnOrder(RealMatrixPreservingVisitor)
     * @see #walkInColumnOrder(RealMatrixChangingVisitor, int, int, int, int)
     * @see #walkInColumnOrder(RealMatrixPreservingVisitor, int, int, int, int)
     * @see #walkInOptimizedOrder(RealMatrixChangingVisitor)
     * @see #walkInOptimizedOrder(RealMatrixPreservingVisitor)
     * @see #walkInOptimizedOrder(RealMatrixChangingVisitor, int, int, int, int)
     */
    double walkInOptimizedOrder(RealMatrixPreservingVisitor visitor, int startRow, int endRow,
            int startColumn, int endColumn);

    /**
     * Gets a string representation of this matrix using the specified format.
     * <p>
     * Several predefined matrix formats are available in {@linkplain MatrixUtils}.
     * </p>
     * 
     * @param realMatrixFormat
     *        the matrix format to be used
     * @return a string representation of this matrix
     * @see MatrixUtils#JAVA_FORMAT
     * @see MatrixUtils#OCTAVE_FORMAT
     * @see MatrixUtils#VISUAL_FORMAT
     * @see MatrixUtils#SUMMARY_FORMAT
     */
    String toString(RealMatrixFormat realMatrixFormat);

    /**
     * Is this matrix numerically equivalent to another matrix?
     * <p>
     * This method indicates if this matrix is equal to another matrix.<br>
     * To do so, the method checks that the two matrices have the same row/column dimensions, and
     * that all entries are numerically equal. Two elements are considered to have different values
     * if their absolute and relative differences are both above the specified tolerances.
     * </p>
     * 
     * @param m
     *        the matrix to be tested for equality
     * @param relativeTolerance
     *        the relative tolerance to take into account when comparing the entries of the matrices
     * @param absoluteTolerance
     *        the absolute tolerance to take into account when comparing the entries of the matrices
     * @return {@code true} if the tested matrix is numerically equivalent to this matrix,
     *         {@code false} otherwise
     */
    boolean equals(RealMatrix m, double relativeTolerance, double absoluteTolerance);
}
