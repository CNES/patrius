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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.linear;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;

/**
 * This class defines a linear operator operating on real ({@code double})
 * vector spaces. No direct access to the coefficients of the underlying matrix
 * is provided.
 * 
 * The motivation for such an interface is well stated by
 * <a href="#BARR1994">Barrett et al. (1994)</a>:
 * <blockquote>
 * We restrict ourselves to iterative methods, which work by repeatedly
 * improving an approximate solution until it is accurate enough. These
 * methods access the coefficient matrix A of the linear system only via the
 * matrix-vector product y = A &middot; x
 * (and perhaps z = A<sup>T</sup> &middot; x). Thus the user need only
 * supply a subroutine for computing y (and perhaps z) given x, which permits
 * full exploitation of the sparsity or other special structure of A.
 * </blockquote> <br/>
 * 
 * <dl>
 * <dt><a name="BARR1994">Barret et al. (1994)</a></dt>
 * <dd>
 * R. Barrett, M. Berry, T. F. Chan, J. Demmel, J. M. Donato, J. Dongarra, V. Eijkhout, R. Pozo, C. Romine and H. Van
 * der Vorst, <em>Templates for the Solution of Linear Systems: Building Blocks for
 *   Iterative Methods</em>, SIAM</dd>
 * </dl>
 * 
 * @version $Id: RealLinearOperator.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.0
 */
//CHECKSTYLE: stop AbstractClassName check
@SuppressWarnings("PMD.AbstractNaming")
public abstract class RealLinearOperator {
    // CHECKSTYLE: resume AbstractClassName check

    /**
     * Returns the dimension of the codomain of this operator.
     * 
     * @return the number of rows of the underlying matrix
     */
    public abstract int getRowDimension();

    /**
     * Returns the dimension of the domain of this operator.
     * 
     * @return the number of columns of the underlying matrix
     */
    public abstract int getColumnDimension();

    /**
     * Returns the result of multiplying {@code this} by the vector {@code x}.
     * 
     * @param x
     *        the vector to operate on
     * @return the product of {@code this} instance with {@code x}
     * @throws DimensionMismatchException
     *         if the column dimension does not match
     *         the size of {@code x}
     */
    public abstract RealVector operate(final RealVector x);

    /**
     * Returns the result of multiplying the transpose of {@code this} operator
     * by the vector {@code x} (optional operation). The default implementation
     * throws an {@link UnsupportedOperationException}. Users overriding this
     * method must also override {@link #isTransposable()}.
     * 
     * @param x
     *        the vector to operate on
     * @return the product of the transpose of {@code this} instance with {@code x}
     * @throws fr.cnes.sirius.patrius.math.exception.DimensionMismatchException
     *         if the row dimension does not match the size of {@code x}
     * @throws UnsupportedOperationException
     *         if this operation is not supported
     *         by {@code this} operator
     */
    public RealVector operateTranspose(final RealVector x) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns {@code true} if this operator supports {@link #operateTranspose(RealVector)}. If {@code true} is
     * returned, {@link #operateTranspose(RealVector)} should not throw {@code UnsupportedOperationException}. The
     * default implementation returns {@code false}.
     * 
     * @return {@code false}
     */
    public boolean isTransposable() {
        return false;
    }
}
