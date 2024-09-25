/**
 * Copyright 2011-2021 CNES
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
* VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.7:DM:DM-2766:18/05/2021:Evol. et corr. dans le package fr.cnes.sirius.patrius.math.linear (suite DM 2300) 
 * VERSION:4.6:FA:FA-2542:27/01/2021:[PATRIUS] Definition d'un champ de vue avec demi-angle de 180° 
 * VERSION:4.5.1:FA:FA-2540:04/08/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear
 * VERSION:4.5:DM:DM-2300:27/05/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.linear;

import java.util.function.Function;

/**
 * Interface for symmetric positive semi-definite matrices.
 *
 * @author Pierre Seimandi (GMV)
 */
public interface SymmetricPositiveMatrix extends SymmetricMatrix {

    /**
     * Returns the result of adding the symmetric positive semi-definite matrix {@code m} to this
     * matrix.
     *
     * @param m
     *        the symmetric positive semi-definite matrix to be added
     * @return the symmetric positive semi-definite matrix resulting from the addition {@code this}+
     *         {@code m}
     * @throws MatrixDimensionMismatchException
     *         if the matrix {@code m} is not the same size as this matrix
     */
    SymmetricPositiveMatrix add(final SymmetricPositiveMatrix m);

    /**
     * Returns the result of adding a positive scalar {@code d} to the entries of this matrix.
     *
     * @param d
     *        the positive scalar value to be added to the entries of this matrix
     * @return the symmetric positive semi-definite matrix resulting from the addition {@code this}+
     *         {@code d}
     */
    SymmetricPositiveMatrix positiveScalarAdd(final double d);

    /**
     * Returns the result of multiplying the entries of this matrix by a positive scalar {@code d}.
     *
     * @param d
     *        the positive scalar value by which to multiply the entries of this matrix by
     * @return the symmetric positive semi-definite matrix resulting from the product {@code d}
     *         &times;{@code this}
     */
    SymmetricPositiveMatrix positiveScalarMultiply(final double d);

    /** {@inheritDoc} */
    @Override
    SymmetricPositiveMatrix copy();

    /** {@inheritDoc} */
    @Override
    SymmetricPositiveMatrix quadraticMultiplication(final RealMatrix m);

    /** {@inheritDoc} */
    @Override
    SymmetricPositiveMatrix quadraticMultiplication(final RealMatrix m, final boolean isTranspose);

    /** {@inheritDoc} */
    @Override
    SymmetricPositiveMatrix getSubMatrix(final int startIndex, final int endIndex);

    /** {@inheritDoc} */
    @Override
    SymmetricPositiveMatrix getSubMatrix(final int[] indices);

    /** {@inheritDoc} */
    @Override
    SymmetricPositiveMatrix transpose();

    /** {@inheritDoc} */
    @Override
    SymmetricPositiveMatrix transpose(boolean forceCopy);

    /** {@inheritDoc} */
    @Override
    SymmetricPositiveMatrix power(final int p);

    /** {@inheritDoc} */
    @Override
    SymmetricPositiveMatrix getInverse();

    /** {@inheritDoc} */
    @Override
    SymmetricPositiveMatrix getInverse(
            final Function<RealMatrix, Decomposition> decompositionBuilder);

    /** {@inheritDoc} */
    @Override
    SymmetricPositiveMatrix createMatrix(final int rowDimension, final int columnDimension);
}
