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
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:306:18/11/2014: coverage
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.math.linear;

import org.junit.Assert;
import org.junit.Test;

public class RectangularCholeskyDecompositionTest {

    /**
     * For coverage purposes, tests the constructor (Matrix, small) with a
     * non positive definite matrix to covers the if (r == 0)
     */
    @Test(expected = NonPositiveDefiniteMatrixException.class)
    public void testNonPositiveDefiniteMatrixExceptionConstructor() {

        final RealMatrix m = MatrixUtils.createRealMatrix(new double[][] {
            { 0, 0, 0 },
            { 0, 0, 0 },
            { 0, 0, 0 } });

        new RectangularCholeskyDecomposition(m, 1.0e-6);
    }

    /**
     * For coverage purposes, tests the constructor (Matrix, small) with a
     * non positive definite matrix to covers the if (c[index[i]][index[i]] < -small)
     */
    @Test(expected = NonPositiveDefiniteMatrixException.class)
    public void testNonPositiveDefiniteMatrixException2Constructor() {

        final RealMatrix m = MatrixUtils.createRealMatrix(new double[][] {
            { 1, 0, 0 },
            { 0, -1, 0 },
            { 0, 0, 0 } });

        new RectangularCholeskyDecomposition(m, 1.0e-6);
    }

    @Test
    public void testDecomposition3x3() {

        final RealMatrix m = MatrixUtils.createRealMatrix(new double[][] {
            { 1, 9, 9 },
            { 9, 225, 225 },
            { 9, 225, 625 }
        });

        final RectangularCholeskyDecomposition d =
            new RectangularCholeskyDecomposition(m, 1.0e-6);

        // as this decomposition permutes lines and columns, the root is NOT triangular
        // (in fact here it is the lower right part of the matrix which is zero and
        // the upper left non-zero)
        Assert.assertEquals(0.8, d.getRootMatrix().getEntry(0, 2), 1.0e-15);
        Assert.assertEquals(25.0, d.getRootMatrix().getEntry(2, 0), 1.0e-15);
        Assert.assertEquals(0.0, d.getRootMatrix().getEntry(2, 2), 1.0e-15);

        final RealMatrix root = d.getRootMatrix();
        final RealMatrix rebuiltM = root.multiply(root.transpose());
        Assert.assertEquals(0.0, m.subtract(rebuiltM).getNorm(), 1.0e-15);

    }

    @Test
    public void testFullRank() {

        final RealMatrix base = MatrixUtils.createRealMatrix(new double[][] {
            { 0.1159548705, 0., 0., 0. },
            { 0.0896442724, 0.1223540781, 0., 0. },
            { 0.0852155322, 4.558668e-3, 0.1083577299, 0. },
            { 0.0905486674, 0.0213768077, 0.0128878333, 0.1014155693 }
        });

        final RealMatrix m = base.multiply(base.transpose());

        final RectangularCholeskyDecomposition d =
            new RectangularCholeskyDecomposition(m, 1.0e-10);

        final RealMatrix root = d.getRootMatrix();
        final RealMatrix rebuiltM = root.multiply(root.transpose());
        Assert.assertEquals(0.0, m.subtract(rebuiltM).getNorm(), 1.0e-15);

        // the pivoted Cholesky decomposition is *not* unique. Here, the root is
        // not equal to the original trianbular base matrix
        Assert.assertTrue(root.subtract(base).getNorm() > 0.3);

    }

    @Test
    public void testMath789() {

        final RealMatrix m1 = MatrixUtils.createRealMatrix(new double[][] {
            { 0.013445532, 0.010394690, 0.009881156, 0.010499559 },
            { 0.010394690, 0.023006616, 0.008196856, 0.010732709 },
            { 0.009881156, 0.008196856, 0.019023866, 0.009210099 },
            { 0.010499559, 0.010732709, 0.009210099, 0.019107243 }
        });
        this.composeAndTest(m1, 4);

        final RealMatrix m2 = MatrixUtils.createRealMatrix(new double[][] {
            { 0.0, 0.0, 0.0, 0.0, 0.0 },
            { 0.0, 0.013445532, 0.010394690, 0.009881156, 0.010499559 },
            { 0.0, 0.010394690, 0.023006616, 0.008196856, 0.010732709 },
            { 0.0, 0.009881156, 0.008196856, 0.019023866, 0.009210099 },
            { 0.0, 0.010499559, 0.010732709, 0.009210099, 0.019107243 }
        });
        this.composeAndTest(m2, 4);

        final RealMatrix m3 = MatrixUtils.createRealMatrix(new double[][] {
            { 0.013445532, 0.010394690, 0.0, 0.009881156, 0.010499559 },
            { 0.010394690, 0.023006616, 0.0, 0.008196856, 0.010732709 },
            { 0.0, 0.0, 0.0, 0.0, 0.0 },
            { 0.009881156, 0.008196856, 0.0, 0.019023866, 0.009210099 },
            { 0.010499559, 0.010732709, 0.0, 0.009210099, 0.019107243 }
        });
        this.composeAndTest(m3, 4);

    }

    private void composeAndTest(final RealMatrix m, final int expectedRank) {
        final RectangularCholeskyDecomposition r = new RectangularCholeskyDecomposition(m);
        Assert.assertEquals(expectedRank, r.getRank());
        final RealMatrix root = r.getRootMatrix();
        final RealMatrix rebuiltMatrix = root.multiply(root.transpose());
        Assert.assertEquals(0.0, m.subtract(rebuiltMatrix).getNorm(), 1.0e-16);
    }

}
