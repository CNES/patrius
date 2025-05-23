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
 * VERSION:4.5:DM:DM-2300:27/05/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.linear;

import fr.cnes.sirius.patrius.math.analysis.function.Sqrt;
import fr.cnes.sirius.patrius.math.util.MathArrays;

/**
 * This class implements the standard Jacobi (diagonal) preconditioner. For a
 * matrix A<sub>ij</sub>, this preconditioner is
 * M = diag(1 / A<sub>11</sub>, 1 / A<sub>22</sub>, &hellip;).
 * 
 * @version $Id: JacobiPreconditioner.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.0
 */
public class JacobiPreconditioner extends RealLinearOperator {

    /** The diagonal coefficients of the preconditioner. */
    private final ArrayRealVector diag;

    /**
     * Creates a new instance of this class.
     * 
     * @param diagIn
     *        the diagonal coefficients of the linear operator to be
     *        preconditioned
     * @param deep
     *        {@code true} if a deep copy of the above array should be
     *        performed
     */
    public JacobiPreconditioner(final double[] diagIn, final boolean deep) {
        super();
        this.diag = new ArrayRealVector(diagIn, deep);
    }

    /**
     * Creates a new instance of this class. This method extracts the diagonal
     * coefficients of the specified linear operator. If {@code a} does not
     * extend {@link AbstractRealMatrix}, then the coefficients of the
     * underlying matrix are not accessible, coefficient extraction is made by
     * matrix-vector products with the basis vectors (and might therefore take
     * some time). With matrices, direct entry access is carried out.
     * 
     * @param a
     *        the linear operator for which the preconditioner should be built
     * @return the diagonal preconditioner made of the inverse of the diagonal
     *         coefficients of the specified linear operator
     * @throws NonSquareOperatorException
     *         if {@code a} is not square
     */
    public static JacobiPreconditioner create(final RealLinearOperator a) {
        final int n = a.getColumnDimension();
        if (a.getRowDimension() != n) {
            throw new NonSquareOperatorException(a.getRowDimension(), n);
        }
        final double[] diag = new double[n];
        if (a instanceof AbstractRealMatrix) {
            // CHECKSTYLE: stop IllegalType check
            // Reason: Commons-Math code kept as such
            final AbstractRealMatrix m = (AbstractRealMatrix) a;
            // CHECKSTYLE: resume IllegalType check
            for (int i = 0; i < n; i++) {
                diag[i] = m.getEntry(i, i);
            }
        } else {
            final ArrayRealVector x = new ArrayRealVector(n);
            for (int i = 0; i < n; i++) {
                x.set(0.);
                x.setEntry(i, 1.);
                diag[i] = a.operate(x).getEntry(i);
            }
        }

        // Return result
        return new JacobiPreconditioner(diag, false);
    }

    /** {@inheritDoc} */
    @Override
    public int getColumnDimension() {
        return this.diag.getDimension();
    }

    /** {@inheritDoc} */
    @Override
    public int getRowDimension() {
        return this.diag.getDimension();
    }

    /** {@inheritDoc} */
    @Override
    public RealVector operate(final RealVector x) {
        // Dimension check is carried out by ebeDivide
        return new ArrayRealVector(MathArrays.ebeDivide(x.toArray(), this.diag.toArray()), false);
    }

    /**
     * Returns the square root of {@code this} diagonal operator. More
     * precisely, this method returns
     * P = diag(1 / &radic;A<sub>11</sub>, 1 / &radic;A<sub>22</sub>, &hellip;).
     * 
     * @return the square root of {@code this} preconditioner
     * @since 3.1
     */
    public RealLinearOperator sqrt() {
        final RealVector sqrtDiag = this.diag.map(new Sqrt());
        return new RealLinearOperator(){
            /** {@inheritDoc} */
            @Override
            public RealVector operate(final RealVector x) {
                return new ArrayRealVector(MathArrays.ebeDivide(x.toArray(),
                    sqrtDiag.toArray()),
                    false);
            }

            /** {@inheritDoc} */
            @Override
            public int getRowDimension() {
                return sqrtDiag.getDimension();
            }

            /** {@inheritDoc} */
            @Override
            public int getColumnDimension() {
                return sqrtDiag.getDimension();
            }
        };
    }
}
