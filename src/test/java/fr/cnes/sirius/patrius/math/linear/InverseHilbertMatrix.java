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
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.linear;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.util.ArithmeticUtils;

/**
 * This class implements inverses of Hilbert Matrices as {@link RealLinearOperator}.
 */
public class InverseHilbertMatrix
    extends RealLinearOperator {

    /** The size of the matrix. */
    private final int n;

    /**
     * Creates a new instance of this class.
     * 
     * @param n
     *        Size of the matrix to be created.
     */
    public InverseHilbertMatrix(final int n) {
        this.n = n;
    }

    /** {@inheritDoc} */
    @Override
    public int getColumnDimension() {
        return this.n;
    }

    /**
     * Returns the {@code (i, j)} entry of the inverse Hilbert matrix. Exact
     * arithmetic is used; in case of overflow, an exception is thrown.
     * 
     * @param i
     *        Row index (starts at 0).
     * @param j
     *        Column index (starts at 0).
     * @return The coefficient of the inverse Hilbert matrix.
     */
    public long getEntry(final int i, final int j) {
        long val = i + j + 1;
        long aux = ArithmeticUtils.binomialCoefficient(this.n + i, this.n - j - 1);
        val = ArithmeticUtils.mulAndCheck(val, aux);
        aux = ArithmeticUtils.binomialCoefficient(this.n + j, this.n - i - 1);
        val = ArithmeticUtils.mulAndCheck(val, aux);
        aux = ArithmeticUtils.binomialCoefficient(i + j, i);
        val = ArithmeticUtils.mulAndCheck(val, aux);
        val = ArithmeticUtils.mulAndCheck(val, aux);
        return ((i + j) & 1) == 0 ? val : -val;
    }

    /** {@inheritDoc} */
    @Override
    public int getRowDimension() {
        return this.n;
    }

    /** {@inheritDoc} */
    @Override
    public RealVector operate(final RealVector x) {
        if (x.getDimension() != this.n) {
            throw new DimensionMismatchException(x.getDimension(), this.n);
        }
        final double[] y = new double[this.n];
        for (int i = 0; i < this.n; i++) {
            double pos = 0.;
            double neg = 0.;
            for (int j = 0; j < this.n; j++) {
                final double xj = x.getEntry(j);
                final long coeff = this.getEntry(i, j);
                final double daux = coeff * xj;
                // Positive and negative values are sorted out in order to limit
                // catastrophic cancellations (do not forget that Hilbert
                // matrices are *very* ill-conditioned!
                if (daux > 0.) {
                    pos += daux;
                } else {
                    neg += daux;
                }
            }
            y[i] = pos + neg;
        }
        return new ArrayRealVector(y, false);
    }
}
