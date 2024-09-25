/**
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.7:DM:DM-2766:18/05/2021:Evol. et corr. dans le package fr.cnes.sirius.patrius.math.linear (suite DM 2300) 
 * VERSION:4.6:DM:DM-2591:27/01/2021:[PATRIUS] Intigration et validation JOptimizer
 * END-HISTORY
 */
/*
 */
/*
 */
/*
 */
/*
 */
/*
 */
/*
 */
/*
 */
/*
 */
/*
 */
/*
 */
/*
 * Copyright 2019-2020 CNES
 * Copyright 2011-2014 JOptimizer
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package fr.cnes.sirius.patrius.math.optim.joptimizer.optimizers;

import junit.framework.TestCase;
import fr.cnes.sirius.patrius.math.linear.Array2DRowRealMatrix;
import fr.cnes.sirius.patrius.math.linear.ArrayRealVector;
import fr.cnes.sirius.patrius.math.linear.BlockRealMatrix;
import fr.cnes.sirius.patrius.math.linear.CholeskyDecomposition;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealVector;
import fr.cnes.sirius.patrius.math.optim.joptimizer.functions.PDQuadraticMultivariateRealFunction;
import fr.cnes.sirius.patrius.math.optim.joptimizer.util.Utils;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * @author alberto trivellato (alberto.trivellato@gmail.com)
 */
public class NewtonUnconstrainedTest extends TestCase {

    /**
     * Quadratic objective.
     * 
     * @throws PatriusException if an error occurs
     */
    public void testOptimize() throws PatriusException {
        // START SNIPPET: newtonUnconstrained-1

        final RealMatrix pMatrix = new Array2DRowRealMatrix(new double[][] { { 1.68, 0.34, 0.38 },
                { 0.34, 3.09, -1.59 }, { 0.38, -1.59, 1.54 } });
        final RealVector qVector = new ArrayRealVector(new double[] { 0.018, 0.025, 0.01 });

        // Objective function.
        final double theta = 0.01522;
        final RealMatrix p = pMatrix.scalarMultiply(theta);
        final RealVector q = qVector.mapMultiply(-1);
        final PDQuadraticMultivariateRealFunction objectiveFunction = new PDQuadraticMultivariateRealFunction(
                p.getData(false), q.toArray(), 0);

        final OptimizationRequest or = new OptimizationRequest();
        or.setF0(objectiveFunction);
        or.setInitialPoint(new double[] { 0.04, 0.50, 0.46 });
        or.setTolerance(1.e-8);
        or.setCheckKKTSolutionAccuracy(true);

        // optimization
        final NewtonUnconstrained opt = new NewtonUnconstrained();
        opt.setOptimizationRequest(or);
        final int returnCode = opt.optimize();

        // END SNIPPET: newtonUnconstrained-1

        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }

        final OptimizationResponse response = opt.getOptimizationResponse();
        final double[] sol = response.getSolution();

        // we know the analytic solution of the problem
        // sol = -PInv * q
        final CholeskyDecomposition cFact = new CholeskyDecomposition(p);
        final RealVector benchSol = cFact.getSolver().solve(q).mapMultiply(-1);

        assertEquals(benchSol.getEntry(0), sol[0], 0.00000000000001);
        assertEquals(benchSol.getEntry(1), sol[1], 0.00000000000001);
        assertEquals(benchSol.getEntry(2), sol[2], 0.00000000000001);
    }

    /**
     * Test with quite large positive definite symmetric matrix.
     * 
     * @throws PatriusException if an error occurs
     */
    public void testOptimize2() throws PatriusException {

        final int dim = 40;

        // positive definite matrix
        final Long seed = new Long(54321);
        final RealMatrix mySymmPD = Utils.randomValuesPositiveMatrix(dim, dim, -0.01, 15.5, seed);
        final RealVector cVector = Utils.randomValuesMatrix(1, dim, -0.01, 15.5, seed).getRowVector(0);
        final MySymmFunction objectiveFunction = new MySymmFunction(mySymmPD, cVector);

        // optimization
        final OptimizationRequest or = new OptimizationRequest();
        or.setF0(objectiveFunction);
        or.setCheckProgressConditions(true);
        final NewtonUnconstrained opt = new NewtonUnconstrained();
        opt.setOptimizationRequest(or);
        final int returnCode = opt.optimize();

        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }

        final OptimizationResponse response = opt.getOptimizationResponse();
        final double[] sol = response.getSolution();

        // we know the analytic solution of the problem: Qinv.sol = - C
        final CholeskyDecomposition cFact = new CholeskyDecomposition(mySymmPD);
        final RealVector benchSol = cFact
                .getSolver()
                .solve(make(cVector.mapMultiply(-1).toArray(),
                        cVector.getDimension())).getColumnVector(0);
        for (int i = 0; i < dim; i++) {
            assertEquals(benchSol.getEntry(i), sol[i], 0.000001);
        }
    }

    /**
     * Test optimize
     * f0 is set to null -> it throws an error
     */
    public void testOptimizeError()  {
        final OptimizationRequest or = new OptimizationRequest();
        or.setF0(null);
        final NewtonUnconstrained opt = new NewtonUnconstrained();
        opt.setOptimizationRequest(or);
        try{
            opt.optimize();
        }catch (PatriusException e) {
            assertTrue(true);//ok, f is not instance of StrictlyConvexMultivariateRealFunction
            return;
        }
        fail();
    }
    
    /**
     * Quadratic objective.
     * Test with alpha too big, the problem cannot be optimized
     * it fails for maximum iterations reached
     * 
     * @throws PatriusException if an error occurs
     */
    public void testMaxIterations() throws PatriusException {
        // START SNIPPET: newtonUnconstrained-1

        final RealMatrix pMatrix = new Array2DRowRealMatrix(new double[][] { { 1.68, 0.34, 0.38 },
                { 0.34, 3.09, -1.59 }, { 0.38, -1.59, 1.54 } });
        final RealVector qVector = new ArrayRealVector(new double[] { 0.018, 0.025, 0.01 });

        // Objective function.
        final double theta = 0.01522;
        final RealMatrix p = pMatrix.scalarMultiply(theta);
        final RealVector q = qVector.mapMultiply(-1);
        final PDQuadraticMultivariateRealFunction objectiveFunction = new PDQuadraticMultivariateRealFunction(
                p.getData(false), q.toArray(), 0);

        final OptimizationRequest or = new OptimizationRequest();
        or.setF0(objectiveFunction);
        or.setInitialPoint(new double[] { 0.04, 0.50, 0.46 });
        or.setTolerance(1.e-8);
        or.setCheckKKTSolutionAccuracy(true);
        or.setAlpha(14);

        // optimization
        final NewtonUnconstrained opt = new NewtonUnconstrained();
        opt.setOptimizationRequest(or);

        if (opt.optimize() == OptimizationResponse.FAILED) {
            assertTrue(true);//ok, max iterations reached
            return;
        }
        fail();
    }
    
    /**
     * Test a problem with no progress -> it throws an exception
     * @throws PatriusException 
     */
    public void testOptimizeNoProgress() throws PatriusException{
        final RealMatrix pMatrix = new Array2DRowRealMatrix(new double[][] { { 1.68, 34, 0.38 },
                { 0.34, 3.09, -1.59 }, { 0.38, -1.59, 1.54 } });
        final RealVector qVector = new ArrayRealVector(new double[] { -0.018, 0.025, 0.01 });

        // Objective function.
        final double theta = 0.01522;
        final RealMatrix p = pMatrix.scalarMultiply(theta);
        final RealVector q = qVector.mapMultiply(-1);
        final PDQuadraticMultivariateRealFunction objectiveFunction = new PDQuadraticMultivariateRealFunction(
                p.getData(false), q.toArray(), 0);

        final OptimizationRequest or = new OptimizationRequest();
        or.setF0(objectiveFunction);
        or.setInitialPoint(new double[] { -10, 50, 0.46 });
        or.setTolerance(1.e-8);
        or.setCheckProgressConditions(true);

        // optimization
        final NewtonUnconstrained opt = new NewtonUnconstrained();
        opt.setOptimizationRequest(or);
        if (opt.optimize() == OptimizationResponse.FAILED) {
            assertTrue(true);//ok, no progress achieved
            return;
        }
        fail();
    }

    
    /**
     * Test getFi and getMeq from of AbstractOptimizationRequestHandler 
     */
    public void testGetAbstractOptimizationRequestHandler() {
        final OptimizationRequest or = new OptimizationRequest();
        final OptimizationRequestHandler opt = new NewtonUnconstrained(true);
        opt.setOptimizationRequest(or);
        final RealVector vecX = new ArrayRealVector();
        assertEquals(null, opt.getFi(vecX));
        assertEquals(0,opt.getMeq());
    }

    
    /**
     * Quadratic objective.
     * 
     * @throws PatriusException if an error occurs
     */
    public void testCheckCustomExitConditions() throws PatriusException {
        // START SNIPPET: newtonUnconstrained-1

        final RealMatrix pMatrix = new Array2DRowRealMatrix(new double[][] { { 1.68, 0.34, 0.38 },
                { 0.34, 3.09, -1.59 }, { 0.38, -1.59, 1.54 } });
        final RealVector qVector = new ArrayRealVector(new double[] { 0.018, 0.025, 0.01 });

        // Objective function.
        final double theta = 0.01522;
        final RealMatrix p = pMatrix.scalarMultiply(theta);
        final RealVector q = qVector.mapMultiply(-1);
        final PDQuadraticMultivariateRealFunction objectiveFunction = new PDQuadraticMultivariateRealFunction(
                p.getData(false), q.toArray(), 0);

        final OptimizationRequest or = new OptimizationRequest();
        or.setF0(objectiveFunction);
        or.setInitialPoint(new double[] { 0.04, 0.50, 0.46 });
        or.setTolerance(1.e-8);
        or.setCheckKKTSolutionAccuracy(true);

        // optimization
        final TestNewtonUnconstrained opt = new TestNewtonUnconstrained();
        opt.setOptimizationRequest(or);
        final int returnCode = opt.optimize();

        // END SNIPPET: newtonUnconstrained-1

        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }
    }
    /**
     * Construct a matrix from a one-dimensional column-major packed array, ala Fortran.
     * Has the form <tt>matrix.get(row,column) == values[row + column*rows]</tt>.
     * The values are copied.
     * @param values One-dimensional array of doubles, packed by columns (ala Fortran).
     * @param rows the number of rows.
     * @return RealMatrix
     * @exception IllegalArgumentException <tt>values.length</tt> must be a multiple of
     *            <tt>rows</tt>.
     **/
    private static RealMatrix make(double values[], int rows) {
        int columns = (rows != 0 ? values.length / rows : 0);
        if (rows * columns != values.length)
            throw new IllegalArgumentException("Array length must be a multiple of m.");

        RealMatrix matrix = new BlockRealMatrix(rows, columns);
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                matrix.setEntry(row, column, values[row + column * rows]);
            }
        }
        return matrix;
    }

    private class MySymmFunction extends PDQuadraticMultivariateRealFunction {

        /**
         * Constructor
         * @param p matrix P
         * @param q vector Q
         */
        public MySymmFunction(RealMatrix p, RealVector q) {
            super(p.getData(), q.toArray(), 0);
        }
    }
    
    /**
     * Private class created to cover checkCustomExitConditions 
     * (it is always false in OptimizationRequestHandler)
     */
    private static class TestNewtonUnconstrained extends NewtonUnconstrained {
        
        public TestNewtonUnconstrained() {
            super();
        }

        @Override
        /**
         * Check the custom exit conditions
         * TO DO: add a good exit conditions method
         * @param y vector
         * @return true/false
         */
        protected boolean checkCustomExitConditions(final RealVector y) {
            return true;
        }
    }

}
