/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 *
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
 * HISTORY
 * VERSION:4.5:DM:DM-2300:27/05/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear 
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.linear;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;

public class LUSolverTest {
    private final double[][] testData = {
        { 1.0, 2.0, 3.0 },
        { 2.0, 5.0, 3.0 },
        { 1.0, 0.0, 8.0 }
    };
    private final double[][] luData = {
        { 2.0, 3.0, 3.0 },
        { 0.0, 5.0, 7.0 },
        { 6.0, 9.0, 8.0 }
    };

    // singular matrices
    private final double[][] singular = {
        { 2.0, 3.0 },
        { 2.0, 3.0 }
    };
    private final double[][] bigSingular = {
        { 1.0, 2.0, 3.0, 4.0 },
        { 2.0, 5.0, 3.0, 4.0 },
        { 7.0, 3.0, 256.0, 1930.0 },
        { 3.0, 7.0, 6.0, 8.0 }
    }; // 4th row = 1st + 2nd

    /** test threshold impact */
    @Test
    public void testThreshold() {
        final RealMatrix matrix = MatrixUtils.createRealMatrix(new double[][] {
            { 1.0, 2.0, 3.0 },
            { 2.0, 5.0, 3.0 },
            { 4.000001, 9.0, 9.0 }
        });
        Assert.assertFalse(new LUDecomposition(matrix, 1.0e-5).getSolver().isNonSingular());
        Assert.assertTrue(new LUDecomposition(matrix, 1.0e-10).getSolver().isNonSingular());
    }

    /** test singular */
    @Test
    public void testSingular() {
        DecompositionSolver solver =
            new LUDecomposition(MatrixUtils.createRealMatrix(this.testData)).getSolver();
        Assert.assertTrue(solver.isNonSingular());
        solver = new LUDecomposition(MatrixUtils.createRealMatrix(this.singular)).getSolver();
        Assert.assertFalse(solver.isNonSingular());
        solver = new LUDecomposition(MatrixUtils.createRealMatrix(this.bigSingular)).getSolver();
        Assert.assertFalse(solver.isNonSingular());
    }

    /** test solve dimension errors */
    @Test
    public void testSolveDimensionErrors() {
        final DecompositionSolver solver =
            new LUDecomposition(MatrixUtils.createRealMatrix(this.testData)).getSolver();
        final RealMatrix b = MatrixUtils.createRealMatrix(new double[2][2]);
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

    /** test solve singularity errors */
    @Test
    public void testSolveSingularityErrors() {
        final DecompositionSolver solver =
            new LUDecomposition(MatrixUtils.createRealMatrix(this.singular)).getSolver();
        final RealMatrix b = MatrixUtils.createRealMatrix(new double[2][2]);
        try {
            solver.solve(b);
            Assert.fail("an exception should have been thrown");
        } catch (final SingularMatrixException ime) {
            // expected behavior
        }
        try {
            solver.solve(b.getColumnVector(0));
            Assert.fail("an exception should have been thrown");
        } catch (final SingularMatrixException ime) {
            // expected behavior
        }
        try {
            solver.solve(new ArrayRealVectorTest.RealVectorTestImpl(b.getColumn(0)));
            Assert.fail("an exception should have been thrown");
        } catch (final SingularMatrixException ime) {
            // expected behavior
        }
    }

    /** test solve */
    @Test
    public void testSolve() {
        final DecompositionSolver solver =
            new LUDecomposition(MatrixUtils.createRealMatrix(this.testData)).getSolver();
        final RealMatrix b = MatrixUtils.createRealMatrix(new double[][] {
            { 1, 0 }, { 2, -5 }, { 3, 1 }
        });
        final RealMatrix xRef = MatrixUtils.createRealMatrix(new double[][] {
            { 19, -71 }, { -6, 22 }, { -2, 9 }
        });

        // using RealMatrix
        Assert.assertEquals(0, solver.solve(b).subtract(xRef).getNorm(), 1.0e-13);

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

    /** test determinant */
    @Test
    public void testDeterminant() {
        Assert.assertEquals(-1, this.getDeterminant(MatrixUtils.createRealMatrix(this.testData)), 1.0e-15);
        Assert.assertEquals(-10, this.getDeterminant(MatrixUtils.createRealMatrix(this.luData)), 1.0e-14);
        Assert.assertEquals(0, this.getDeterminant(MatrixUtils.createRealMatrix(this.singular)), 1.0e-17);
        Assert.assertEquals(0, this.getDeterminant(MatrixUtils.createRealMatrix(this.bigSingular)), 1.0e-10);
    }

    private double getDeterminant(final RealMatrix m) {
        return new LUDecomposition(m).getDeterminant();
    }
}
