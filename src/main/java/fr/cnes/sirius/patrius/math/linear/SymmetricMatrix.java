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
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
* VERSION:4.8:DM:DM-3040:15/11/2021:[PATRIUS]Reversement des evolutions de la branche patrius-for-lotus 
 * VERSION:4.7:DM:DM-2766:18/05/2021:Evol. et corr. dans le package fr.cnes.sirius.patrius.math.linear (suite DM 2300) 
 * VERSION:4.6:FA:FA-2542:27/01/2021:[PATRIUS] Definition d'un champ de vue avec demi-angle de 180° 
 * VERSION:4.5.1:FA:FA-2540:04/08/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear
 * VERSION:4.5:DM:DM-2300:27/05/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.linear;

import java.util.function.Function;

/**
 * Interface for symmetric matrices.
 *
 * @author Pierre Seimandi (GMV)
 */
public interface SymmetricMatrix extends RealMatrix {

    /**
     * Returns the result of adding the symmetric matrix {@code m} to this matrix.
     *
     * @param m
     *        the matrix to be added
     * @return the matrix resulting from the addition {@code this}+{@code m}
     * @throws MatrixDimensionMismatchException
     *         if the matrix {@code m} is not the same size as this matrix
     */
    SymmetricMatrix add(final SymmetricMatrix m);

    /**
     * Returns the result of subtracting the symmetric matrix {@code m} from this matrix.
     *
     * @param m
     *        the matrix to be subtracted
     * @return the matrix resulting from the subtraction {@code this}-{@code m}
     * @throws MatrixDimensionMismatchException
     *         if the matrix {@code m} is not the same size as this matrix
     */
    SymmetricMatrix subtract(final SymmetricMatrix m);

    /** {@inheritDoc} */
    @Override
    SymmetricMatrix scalarAdd(final double d);

    /** {@inheritDoc} */
    @Override
    SymmetricMatrix scalarMultiply(final double d);

    /**
     * Returns the result of the quadratic multiplication M&times;{@code this}&times;M<sup>T</sup>,
     * where M is the provided matrix.
     *
     * @param m
     *        the matrix M
     * @return the matrix resulting from the quadratic multiplication M&times;{@code this}
     *         &times;M<sup>T</sup>
     */
    SymmetricMatrix quadraticMultiplication(final RealMatrix m);

    /**
     * Returns the result of the quadratic multiplication M&times;{@code this}&times;M<sup>T</sup>,
     * where M or M<sup>T</sup> is the provided matrix.
     *
     * @param m
     *        the matrix M or the matrix M<sup>T</sup>
     * @param isTranspose
     *        if {@code true}, assumes the provided matrix is M<sup>T</sup>, otherwise assumes it is
     *        M
     * @return the matrix resulting from the quadratic multiplication M&times;{@code this}
     *         &times;M<sup>T</sup>
     */
    SymmetricMatrix quadraticMultiplication(final RealMatrix m, final boolean isTranspose);

    /**
     * Extracts the submatrix corresponding to the specified indices.
     * <p>
     * This method uses the same start/end indices to select the rows and columns to be extracted.
     * These indices must be valid indices with respect to the dimensions of the matrix (valid
     * indices range from 0 to n-1, with n the row/column dimension of the matrix). Calling this
     * method is equivalent to calling {@linkplain #getSubMatrix(int, int, int, int)} using the same
     * start/end indices for the rows and columns. The extracted submatrix is guaranteed to be
     * symmetric. It will also remain positive semi-definite if the initial matrix originally is.
     * </p>
     * <p>
     * <b>Usage examples:</b>
     * </p>
     * 
     * <pre>
     * // Initial matrix
     * matrix = [a<sub>00</sub>, a<sub>10</sub>, a<sub>20</sub>]
     *          [a<sub>10</sub>, a<sub>11</sub>, a<sub>21</sub>]
     *          [a<sub>20</sub>, a<sub>21</sub>, a<sub>22</sub>]
     * 
     * // Submatrix extraction
     * matrix.getSubMatrix(1, 2) => [a<sub>11</sub>, a<sub>21</sub>]
     *                              [a<sub>21</sub>, a<sub>22</sub>]
     * </pre>
     *
     * @param startIndex
     *        the initial row/column index
     * @param endIndex
     *        the final row/column index (inclusive)
     * @return the extracted submatrix
     */
    SymmetricMatrix getSubMatrix(final int startIndex, final int endIndex);

    /**
     * Extracts the submatrix corresponding to the specified indices.
     * <p>
     * This method uses a single index array to select the rows and columns to be extracted. All
     * indices must be valid indices with respect to the dimensions of the matrix (valid indices
     * range from 0 to n-1, with n the row/column dimension of the matrix). The provided index array
     * is allowed to contain duplicates. Calling this method is equivalent to calling
     * {@linkplain #getSubMatrix(int[], int[])} using the provided index array for the selected rows
     * and columns. This method can be used to extract any submatrix and perform a symmetric
     * reordering of its rows/columns. The extracted submatrix is guaranteed to be symmetric. It
     * will also remain positive semi-definite if the initial matrix originally is.
     * </p>
     * <p>
     * <b>Usage examples:</b>
     * </p>
     * 
     * <pre>
     * // Initial matrix
     * matrix = [a<sub>00</sub>, a<sub>10</sub>, a<sub>20</sub>]
     *          [a<sub>10</sub>, a<sub>11</sub>, a<sub>21</sub>]
     *          [a<sub>20</sub>, a<sub>21</sub>, a<sub>22</sub>]
     * 
     * // Submatrix extraction
     * matrix.getSubMatrix([1, 2]) => [a<sub>11</sub>, a<sub>21</sub>]
     *                                [a<sub>21</sub>, a<sub>22</sub>]
     * 
     * // Rows/Columns permutation
     * matrix.getSubMatrix([1, 2, 0]) => [a<sub>11</sub>, a<sub>21</sub>, a<sub>10</sub>]
     *                                   [a<sub>21</sub>, a<sub>22</sub>, a<sub>20</sub>]
     *                                   [a<sub>10</sub>, a<sub>20</sub>, a<sub>00</sub>]
     * 
     * // Submatrix extraction (with duplicated indices)
     * matrix.getSubMatrix([1, 2, 0, 1, 0]) 
     *      => [a<sub>11</sub>, a<sub>21</sub>, a<sub>10</sub>, a<sub>11</sub>, a<sub>10</sub>]
     *         [a<sub>21</sub>, a<sub>22</sub>, a<sub>20</sub>, a<sub>21</sub>, a<sub>20</sub>]
     *         [a<sub>10</sub>, a<sub>20</sub>, a<sub>00</sub>, a<sub>10</sub>, a<sub>00</sub>]
     *         [a<sub>11</sub>, a<sub>21</sub>, a<sub>10</sub>, a<sub>11</sub>, a<sub>10</sub>]
     *         [a<sub>10</sub>, a<sub>20</sub>, a<sub>00</sub>, a<sub>10</sub>, a<sub>00</sub>]
     * </pre>
     *
     * @param indices
     *        the selected indices
     * @return the extracted submatrix
     */
    SymmetricMatrix getSubMatrix(final int[] indices);

    /** {@inheritDoc} */
    @Override
    SymmetricMatrix copy();

    /** {@inheritDoc} */
    @Override
    SymmetricMatrix transpose();

    /** {@inheritDoc} */
    @Override
    SymmetricMatrix transpose(boolean forceCopy);

    /** {@inheritDoc} */
    @Override
    SymmetricMatrix power(final int p);

    /** {@inheritDoc} */
    @Override
    SymmetricMatrix getInverse();

    /** {@inheritDoc} */
    @Override
    SymmetricMatrix getInverse(final Function<RealMatrix, Decomposition> decompositionBuilder);

    /**
     * {@inheritDoc}
     *
     * <p>
     * <em>Since the matrix build is a symmetric matrix, the row and column dimensions are expected to be equal.</em>
     * </p>
     */
    @Override
    SymmetricMatrix createMatrix(final int rowDimension, final int columnDimension);
}
