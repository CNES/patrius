/**
 * 
 * Copyright 2011-2022 CNES
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
 *
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2300:27/05/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.linear;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;

/**
 * @description test class for UD solver
 * 
 * @author Julie Anton
 * 
 * @version $Id: UDSolverTest.java 17909 2017-09-11 11:57:36Z bignon $
 * 
 * @since 1.0
 * 
 */
public class UDSolverTest {

    /** Matrix test. */
    private final double[][] testData = new double[][] { { 1, 2, 4, 7, 11 }, { 2, 13, 23, 38, 58 },
        { 4, 23, 77, 122, 182 },
        { 7, 38, 122, 294, 430 }, { 11, 58, 182, 430, 855 } };

    /** Features description. */
    public enum features {
        /**
         * @featureTitle solver
         * 
         * @featureDescription UD solver
         * 
         * @coveredRequirements DV-MATHS_270
         */
        SOLVER

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#SOLVER}
     * 
     * @testedMethod {@link DecompositionSolver#solve(RealVector)}
     * @testedMethod {@link DecompositionSolver#solve(RealMatrix)}
     * 
     * @description Solve dimension errors.
     * 
     * @input A &times X = B where A and B have mismatched dimensions
     * 
     * @output Exception
     * 
     * @testPassCriteria A MathIllegalArgumentException exception should be raised.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testSolveDimensionErrors() {
        final DecompositionSolver solver =
            new UDDecompositionImpl(MatrixUtils.createRealMatrix(this.testData)).getSolver();
        final RealMatrix b = MatrixUtils.createRealMatrix(new double[2][2]);
        try {
            solver.solve(b);
            Assert.fail("an exception thrown should have been");
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

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#SOLVER}
     * 
     * @testedMethod {@link DecompositionSolver#solve(RealMatrix)}
     * @testedMethod {@link DecompositionSolver#solve(RealVector)}
     * 
     * @description Test the solver.
     * 
     * @input A and B such as A &times X = B, A is the matrix test, B is an array double[][], a real matrix, an array
     *        double[], a real vector and an array real vector {@link ArrayRealVector}. The used matrix are those used
     *        in
     *        Cholesky JUnit testorg.apache.commons.math.h3.linear.CholeskySolverTest#testSolve()).
     * 
     * @output The result X of the equation A &times X = B.
     * 
     * @testPassCriteria The norm of the difference between the expected X and the result should be null.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testSolve() {
        final DecompositionSolver solver =
            new UDDecompositionImpl(MatrixUtils.createRealMatrix(this.testData)).getSolver();
        Assert.assertTrue(solver.isNonSingular());
        final RealMatrix b = MatrixUtils.createRealMatrix(new double[][] { { 78, -13, 1 }, { 414, -62, -1 },
            { 1312, -202, -37 }, { 2989, -542, 145 }, { 5510, -1465, 201 } });
        final RealMatrix xRef = MatrixUtils.createRealMatrix(new double[][] { { 1, 0, 1 }, { 0, 1, 1 }, { 2, 1, -4 },
            { 2, 2, 2 }, { 5, -3, 0 } });

        // using RealMatrix
        Assert.assertEquals(0, solver.solve(b).subtract(xRef).getNorm(), 1.0e-13);

        // using ArrayRealVector
        for (int i = 0; i < b.getColumnDimension(); ++i) {
            Assert.assertEquals(0, solver.solve(b.getColumnVector(i)).subtract(xRef.getColumnVector(i)).getNorm(),
                1.0e-13);
        }

        // using RealVector with an alternate implementation
        for (int i = 0; i < b.getColumnDimension(); ++i) {
            final ArrayRealVectorTest.RealVectorTestImpl v = new ArrayRealVectorTest.RealVectorTestImpl(b.getColumn(i));
            Assert.assertEquals(0, solver.solve(v).subtract(xRef.getColumnVector(i)).getNorm(), 1.0e-13);
        }

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#SOLVER}
     * 
     * @testedMethod {@link UDDecompositionImpl#getDeterminant() }
     * 
     * @description Test the determinant.
     * 
     * @input The matrix test
     * 
     * @output The determinant of the matrix
     * 
     * @testPassCriteria The expected determinant.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testDeterminant() {
        Assert.assertEquals(7290000.0, this.getDeterminant(MatrixUtils.createRealMatrix(this.testData)), 1.0e-8);
    }

    /**
     * 
     * @description compute the determinant of the matrix
     * 
     * @param m
     *        : the matrix
     * @return the determinant of the matrix
     * 
     * @since 1.0
     */
    private double getDeterminant(final RealMatrix m) {
        return new UDDecompositionImpl(m).getDeterminant();
    }
}
