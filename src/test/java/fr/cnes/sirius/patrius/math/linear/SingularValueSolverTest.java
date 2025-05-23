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
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:306:14/11/2014: coverage
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.math.linear;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;

public class SingularValueSolverTest {

    private final double[][] testSquare = {
        { 24.0 / 25.0, 43.0 / 25.0 },
        { 57.0 / 25.0, 24.0 / 25.0 }
    };

    private static final double normTolerance = 10e-14;

    /** test solve dimension errors */
    @Test
    public void testSolveDimensionErrors() {
        final DecompositionSolver solver =
            new SingularValueDecomposition(MatrixUtils.createRealMatrix(this.testSquare)).getSolver();
        final RealMatrix b = MatrixUtils.createRealMatrix(new double[3][2]);
        try {
            solver.solve(b);
            Assert.fail("an exception should have been thrown");
        } catch (final MathIllegalArgumentException iae) {
            // expected behavior
        }
        try {
            solver.solve(b.getColumnVector(0));
            Assert.fail("an exception should have been thrown");
        } catch (final MathIllegalArgumentException iae) {
            // expected behavior
        }
        try {
            solver.solve(new ArrayRealVectorTest.RealVectorTestImpl(b.getColumn(0)));
            Assert.fail("an exception should have been thrown");
        } catch (final MathIllegalArgumentException iae) {
            // expected behavior
        }
    }

    /** test least square solve */
    @Test
    public void testLeastSquareSolve() {
        final RealMatrix m =
            MatrixUtils.createRealMatrix(new double[][] {
                { 1.0, 0.0 },
                { 0.0, 0.0 }
            });
        final DecompositionSolver solver = new SingularValueDecomposition(m).getSolver();
        final RealMatrix b = MatrixUtils.createRealMatrix(new double[][] {
            { 11, 12 }, { 21, 22 }
        });
        final RealMatrix xMatrix = solver.solve(b);
        Assert.assertEquals(11, xMatrix.getEntry(0, 0), 1.0e-15);
        Assert.assertEquals(12, xMatrix.getEntry(0, 1), 1.0e-15);
        Assert.assertEquals(0, xMatrix.getEntry(1, 0), 1.0e-15);
        Assert.assertEquals(0, xMatrix.getEntry(1, 1), 1.0e-15);
        final RealVector xColVec = solver.solve(b.getColumnVector(0));
        Assert.assertEquals(11, xColVec.getEntry(0), 1.0e-15);
        Assert.assertEquals(0, xColVec.getEntry(1), 1.0e-15);
        final RealVector xColOtherVec = solver.solve(new ArrayRealVectorTest.RealVectorTestImpl(b.getColumn(0)));
        Assert.assertEquals(11, xColOtherVec.getEntry(0), 1.0e-15);
        Assert.assertEquals(0, xColOtherVec.getEntry(1), 1.0e-15);
    }

    /** test solve */
    @Test
    public void testSolve() {
        final DecompositionSolver solver =
            new SingularValueDecomposition(MatrixUtils.createRealMatrix(this.testSquare)).getSolver();
        final RealMatrix b = MatrixUtils.createRealMatrix(new double[][] {
            { 1, 2, 3 }, { 0, -5, 1 }
        });
        final RealMatrix xRef = MatrixUtils.createRealMatrix(new double[][] {
            { -8.0 / 25.0, -263.0 / 75.0, -29.0 / 75.0 },
            { 19.0 / 25.0, 78.0 / 25.0, 49.0 / 25.0 }
        });

        // using RealMatrix
        Assert.assertEquals(0, solver.solve(b).subtract(xRef).getNorm(), normTolerance);

        // using ArrayRealVector
        for (int i = 0; i < b.getColumnDimension(); ++i) {
            Assert.assertEquals(0,
                solver.solve(b.getColumnVector(i)).subtract(xRef.getColumnVector(i)).getNorm(),
                1.0e-13);
        }

        // using RealVector with an alternate implementation
        for (int i = 0; i < b.getColumnDimension(); ++i) {
            final ArrayRealVectorTest.RealVectorTestImpl v =
                new ArrayRealVectorTest.RealVectorTestImpl(b.getColumn(i));
            Assert.assertEquals(0,
                solver.solve(v).subtract(xRef.getColumnVector(i)).getNorm(),
                1.0e-13);
        }

    }

    /** test condition number */
    @Test
    public void testConditionNumber() {
        final SingularValueDecomposition svd =
            new SingularValueDecomposition(MatrixUtils.createRealMatrix(this.testSquare));
        // replace 1.0e-15 with 1.5e-15
        Assert.assertEquals(3.0, svd.getConditionNumber(), 1.5e-15);
    }

    @Test
    public void testMath320B() {
        final RealMatrix rm = new Array2DRowRealMatrix(new double[][] {
            { 1.0, 2.0 }, { 1.0, 2.0 }
        });
        final SingularValueDecomposition svd =
            new SingularValueDecomposition(rm);
        final RealMatrix recomposed = svd.getU().multiply(svd.getS()).multiply(svd.getVT());
        Assert.assertEquals(0.0, recomposed.subtract(rm).getNorm(), 2.0e-15);
    }

    @Test
    public void testSingular() {
        final double[][] bigSingular = {
            { 1.0, 2.0, 3.0, 4.0 },
            { 2.0, 5.0, 3.0, 4.0 },
            { 7.0, 3.0, 256.0, 1930.0 },
            { 3.0, 7.0, 6.0, 8.0 }
        }; // 4th row = 1st + 2nd
        final SingularValueDecomposition svd =
            new SingularValueDecomposition(MatrixUtils.createRealMatrix(bigSingular));
        final RealMatrix pseudoInverse = svd.getSolver().getInverse();
        final RealMatrix expected = new Array2DRowRealMatrix(new double[][] {
            { -0.0355022687, 0.0512742236, -0.0001045523, 0.0157719549 },
            { -0.3214992438, 0.3162419255, 0.0000348508, -0.0052573183 },
            { 0.5437098346, -0.4107754586, -0.0008256918, 0.132934376 },
            { -0.0714905202, 0.053808742, 0.0006279816, -0.0176817782 }
        });
        Assert.assertEquals(0, expected.subtract(pseudoInverse).getNorm(), 1.0e-9);
    }
}
