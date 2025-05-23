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
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.linear;

import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;

public class EigenSolverTest {

    /** test non invertible matrix */
    @Test
    public void testNonInvertible() {
        final Random r = new Random(9994100315209l);
        final RealMatrix m =
            EigenDecompositionTest.createTestMatrix(r, new double[] { 1.0, 0.0, -1.0, -2.0, -3.0 });
        final DecompositionSolver es = new EigenDecomposition(m).getSolver();
        Assert.assertFalse(es.isNonSingular());
        try {
            es.getInverse();
            Assert.fail("an exception should have been thrown");
        } catch (final SingularMatrixException ime) {
            // expected behavior
        }
    }

    /** test invertible matrix */
    @Test
    public void testInvertible() {
        final Random r = new Random(9994100315209l);
        final RealMatrix m =
            EigenDecompositionTest.createTestMatrix(r, new double[] { 1.0, 0.5, -1.0, -2.0, -3.0 });
        final DecompositionSolver es = new EigenDecomposition(m).getSolver();
        Assert.assertTrue(es.isNonSingular());
        final RealMatrix inverse = es.getInverse();
        final RealMatrix error =
            m.multiply(inverse).subtract(MatrixUtils.createRealIdentityMatrix(m.getRowDimension()));
        Assert.assertEquals(0, error.getNorm(), 4.0e-15);
    }

    /** test solve dimension errors */
    @Test
    public void testSolveDimensionErrors() {
        final double[] refValues = new double[] {
            2.003, 2.002, 2.001, 1.001, 1.000, 0.001
        };
        final RealMatrix matrix = EigenDecompositionTest.createTestMatrix(new Random(35992629946426l), refValues);

        final DecompositionSolver es = new EigenDecomposition(matrix).getSolver();
        final RealMatrix b = MatrixUtils.createRealMatrix(new double[2][2]);
        try {
            es.solve(b);
            Assert.fail("an exception should have been thrown");
        } catch (final MathIllegalArgumentException iae) {
            // expected behavior
        }
        try {
            es.solve(b.getColumnVector(0));
            Assert.fail("an exception should have been thrown");
        } catch (final MathIllegalArgumentException iae) {
            // expected behavior
        }
        try {
            es.solve(new ArrayRealVectorTest.RealVectorTestImpl(b.getColumn(0)));
            Assert.fail("an exception should have been thrown");
        } catch (final MathIllegalArgumentException iae) {
            // expected behavior
        }
    }

    /** test solve */
    @Test
    public void testSolve() {
        final RealMatrix m = MatrixUtils.createRealMatrix(new double[][] {
            { 91, 5, 29, 32, 40, 14 },
            { 5, 34, -1, 0, 2, -1 },
            { 29, -1, 12, 9, 21, 8 },
            { 32, 0, 9, 14, 9, 0 },
            { 40, 2, 21, 9, 51, 19 },
            { 14, -1, 8, 0, 19, 14 }
        });
        final DecompositionSolver es = new EigenDecomposition(m).getSolver();
        final RealMatrix b = MatrixUtils.createRealMatrix(new double[][] {
            { 1561, 269, 188 },
            { 69, -21, 70 },
            { 739, 108, 63 },
            { 324, 86, 59 },
            { 1624, 194, 107 },
            { 796, 69, 36 }
        });
        final RealMatrix xRef = MatrixUtils.createRealMatrix(new double[][] {
            { 1, 2, 1 },
            { 2, -1, 2 },
            { 4, 2, 3 },
            { 8, -1, 0 },
            { 16, 2, 0 },
            { 32, -1, 0 }
        });

        // using RealMatrix
        final RealMatrix solution = es.solve(b);
        Assert.assertEquals(0, solution.subtract(xRef).getNorm(), 2.5e-12);

        // using RealVector
        for (int i = 0; i < b.getColumnDimension(); ++i) {
            Assert.assertEquals(0,
                es.solve(b.getColumnVector(i)).subtract(xRef.getColumnVector(i)).getNorm(),
                2.0e-11);
        }

        // using RealVector with an alternate implementation
        for (int i = 0; i < b.getColumnDimension(); ++i) {
            final ArrayRealVectorTest.RealVectorTestImpl v =
                new ArrayRealVectorTest.RealVectorTestImpl(b.getColumn(i));
            Assert.assertEquals(0,
                es.solve(v).subtract(xRef.getColumnVector(i)).getNorm(),
                2.0e-11);
        }
    }
}
