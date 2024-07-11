/**
 * HISTORY
 * VERSION:4.7:DM:DM-2766:18/05/2021:Evol. et corr. dans le package fr.cnes.sirius.patrius.math.linear (suite DM 2300) 
 * VERSION:4.6:DM:DM-2591:27/01/2021:[PATRIUS] Intigration et validation JOptimizer
 * END-HISTORY
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
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealVector;
import fr.cnes.sirius.patrius.math.optim.joptimizer.functions.LinearMultivariateRealFunction;
import fr.cnes.sirius.patrius.math.optim.joptimizer.functions.PDQuadraticMultivariateRealFunction;
import fr.cnes.sirius.patrius.math.optim.joptimizer.optimizers.NewtonLEConstrainedISP;
import fr.cnes.sirius.patrius.math.optim.joptimizer.optimizers.OptimizationRequest;
import fr.cnes.sirius.patrius.math.optim.joptimizer.optimizers.OptimizationResponse;
import fr.cnes.sirius.patrius.math.optim.joptimizer.solvers.AbstractKKTSolver;
import fr.cnes.sirius.patrius.math.optim.joptimizer.solvers.AugmentedKKTSolver;
import fr.cnes.sirius.patrius.math.optim.joptimizer.solvers.BasicKKTSolver;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusRuntimeException;

/**
 * @author alberto trivellato (alberto.trivellato@gmail.com)
 */
public class NewtonLEConstrainedISPTest extends TestCase {

    /**
     * 
     * @throws PatriusException if an error occurs
     */
    public void testOptimize1() throws PatriusException {

        // START SNIPPET: NewtonLEConstrainedISP-1

        // commons-math client code
        final RealMatrix pmatrix = new Array2DRowRealMatrix(new double[][] { { 1.68, 0.34, 0.38 },
                { 0.34, 3.09, -1.59 }, { 0.38, -1.59, 1.54 } });
        final RealVector qVector = new ArrayRealVector(new double[] { 0.018, 0.025, 0.01 });

        // Objective function
        final double theta = 0.01522;
        final RealMatrix p = pmatrix.scalarMultiply(theta);
        final RealVector q = qVector.mapMultiply(-1);
        final PDQuadraticMultivariateRealFunction objectiveFunction = new PDQuadraticMultivariateRealFunction(
                p.getData(false), q.toArray(), 0);

        final OptimizationRequest or = new OptimizationRequest();
        or.setF0(objectiveFunction);
        or.setInitialPoint(new double[] { 0.1, 0.1, 0.1 });// LE-infeasible starting point
        or.setA(new double[][] { { 1, 1, 1 } });
        or.setB(new double[] { 1 });

        // optimization
        final NewtonLEConstrainedISP opt = new NewtonLEConstrainedISP();
        opt.setOptimizationRequest(or);
        opt.setKKTSolver(new BasicKKTSolver());
        final OptimizationRequest req = opt.getOptimizationRequest();
        req.setCheckKKTSolutionAccuracy(true);
        req.setCheckProgressConditions(true);
        final int returnCode = opt.optimize();

        // END SNIPPET: NewtonLEConstrainedISP-1

        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }

        final OptimizationResponse response = opt.getOptimizationResponse();
        final double[] sol = response.getSolution();
        assertEquals(0.04632311555988555, sol[0], 0.000000000000001);
        assertEquals(0.5086308460954377, sol[1], 0.000000000000001);
        assertEquals(0.44504603834467693, sol[2], 0.000000000000001);
    }

    /**
     * Minimize x subject to
     * x+y=4,
     * x-y=2.
     * Should return (3,1).
     * This problem is the same as LPPrimalDualMethodTest.testSimple4()
     * and can be solved only with the use of a linear presolving phase:
     * if passed directly to the solver, it will fail because JOptimizer
     * does not want rank-deficient inequalities matrices like that of this problem.
     * 
     * @throws PatriusException if an error occurs
     * @throws PatriusRuntimeException if an error occurs
     */
    public void testOptimize2() throws PatriusException, PatriusRuntimeException {
        final double[] minimizeF = new double[] { 1.0, 0.0 };
        final LinearMultivariateRealFunction objectiveFunction = new LinearMultivariateRealFunction(
                minimizeF, 0.0);

        // Equalities:
        final double[][] equalityAMatrix = new double[][] { { 1.0, 1.0 }, { 1.0, -1.0 } };
        final double[] equalityBVector = new double[] { 4.0, 2.0 };

        // optimization problem
        final OptimizationRequest or = new OptimizationRequest();
        or.setF0(objectiveFunction);
        or.setA(equalityAMatrix);
        or.setB(equalityBVector);

        // optimization
        final NewtonLEConstrainedISP opt = new NewtonLEConstrainedISP();
        opt.setOptimizationRequest(or);
        try {
            opt.optimize();
            fail();
        } catch (PatriusRuntimeException e) {
            // this problem cannot be passed directly to the solvers of JOptimizer
            // because they do not want rank-deficient inequalities matrices
            assertTrue(true);
        }
    }

    /**
     * Minimize 0 subject to
     * x+y=4.
     * Should return any feasible solution.
     * 
     * @throws PatriusException if an error occurs
     */
    public void testOptimize3() throws PatriusException {
        final double[] minimizeF = new double[] { 0.0, 0.0 };
        final LinearMultivariateRealFunction objectiveFunction = new LinearMultivariateRealFunction(
                minimizeF, 0.0);

        // Equalities:
        final double[][] equalityAMatrix = new double[][] { { 1.0, 1.0 } };
        final double[] equalityBVector = new double[] { 4.0 };

        // optimization problem
        final OptimizationRequest or = new OptimizationRequest();
        or.setF0(objectiveFunction);
        or.setA(equalityAMatrix);
        or.setB(equalityBVector);
        
        // optimization
        final NewtonLEConstrainedISP opt = new NewtonLEConstrainedISP();
        opt.setOptimizationRequest(or);
        final int returnCode = opt.optimize();

        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }

        final OptimizationResponse response = opt.getOptimizationResponse();
        final double[] sol = response.getSolution();
        assertEquals(4.0, sol[0] + sol[1], 1e-8);
    }
    
    /**
     * 
     * @throws PatriusException if an error occurs
     */
    public void testOptimize1b() throws PatriusException {
        final RealMatrix pmatrix = new Array2DRowRealMatrix(new double[][] { { 1.68, 0.34, 0.38 },
                { 0.34, 3.09, -1.59 }, { 0.38, -1.59, 1.54 } });
        final RealVector qVector = new ArrayRealVector(new double[] { 0.018, 0.025, 0.01 });

        // Objective function
        final double theta = 0.01522;
        final RealMatrix p = pmatrix.scalarMultiply(theta);
        final RealVector q = qVector.mapMultiply(-1);
        final PDQuadraticMultivariateRealFunction objectiveFunction = new PDQuadraticMultivariateRealFunction(
                p.getData(false), q.toArray(), 0);

        final OptimizationRequest or = new OptimizationRequest();
        or.setF0(objectiveFunction);
        or.setB(new double[] { 1 });

        // optimization
        final NewtonLEConstrainedISP opt = new NewtonLEConstrainedISP();
        opt.setOptimizationRequest(or);
        opt.setKKTSolver(new BasicKKTSolver());
        final OptimizationRequest req = opt.getOptimizationRequest();
        req.setCheckKKTSolutionAccuracy(true);
        req.setCheckProgressConditions(true);
        final int returnCode = opt.optimize();

        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }

        final OptimizationResponse response = opt.getOptimizationResponse();
        final double[] sol = response.getSolution();
        assertEquals(-0.1258262767402377, sol[0], 0.000000000000001);
        assertEquals(1.6660619996192525, sol[1], 0.000000000000001);
        assertEquals(2.1778459661988165, sol[2], 0.000000000000001);
    }
    
    /**
     * Test a problem with no progress -> it throws an exception
     * @throws PatriusException 
     * @throws PatriusRuntimeException 
     */
    public void testOptimizeNoProgress() throws PatriusRuntimeException, PatriusException{
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
        final NewtonLEConstrainedISP opt = new NewtonLEConstrainedISP();
        opt.setOptimizationRequest(or);
        
        if (opt.optimize() == OptimizationResponse.FAILED) {
            assertTrue(true);//ok, no progress achieved
            return;
        }
        fail();

    }

    /**
     * Test with max iterations set to 1 -> it throws an exception before optimizing
     * @throws PatriusException 
     * @throws PatriusRuntimeException 
     */
    public void testMaxIterations() throws PatriusRuntimeException, PatriusException  {

        // START SNIPPET: NewtonLEConstrainedISP-1

        // commons-math client code
        final RealMatrix pmatrix = new Array2DRowRealMatrix(new double[][] { { 1.68, 0.34, 0.38 },
                { 0.34, 3.09, -1.59 }, { 0.38, -1.59, 1.54 } });
        final RealVector qVector = new ArrayRealVector(new double[] { 0.018, 0.025, 0.01 });

        // Objective function
        final double theta = 0.01522;
        final RealMatrix p = pmatrix.scalarMultiply(theta);
        final RealVector q = qVector.mapMultiply(-1);
        final PDQuadraticMultivariateRealFunction objectiveFunction = new PDQuadraticMultivariateRealFunction(
                p.getData(false), q.toArray(), 0);

        final OptimizationRequest or = new OptimizationRequest();
        or.setF0(objectiveFunction);
        or.setInitialPoint(new double[] { 0.1, 0.1, 0.1 });// LE-infeasible starting point
        or.setA(new double[][] { { 1, 1, 1 } });
        or.setB(new double[] { 1 });

        // optimization
        final NewtonLEConstrainedISP opt = new NewtonLEConstrainedISP();
        opt.setOptimizationRequest(or);
        final OptimizationRequest req = opt.getOptimizationRequest();
        req.setMaxIteration(1);
        if (opt.optimize() == OptimizationResponse.FAILED) {
            assertTrue(true);//ok, max iterations reached
            return;
        }
        fail();
    }
    
    /**
     * Test with AugmentedKKTSolver with a problem that does not pass the solution checking
     * Residual > toleranceKKT -> it throws an exception
     * 
     */
    public void testOptimizeToleranceError()  {
        final RealMatrix pmatrix = new Array2DRowRealMatrix(new double[][] { { 1.68, 0, 0.38 },
                { 0.34, 3.09, 1.59 }, { 0.38, 0, 1.54 } });
        final RealVector qVector = new ArrayRealVector(new double[] { 0.018, 0, 0.01 });

        // Objective function
        final double theta = 0.01522;
        final RealMatrix p = pmatrix.scalarMultiply(theta);
        final RealVector q = qVector.mapMultiply(-1);
        final PDQuadraticMultivariateRealFunction objectiveFunction = new PDQuadraticMultivariateRealFunction(
                p.getData(false), q.toArray(), 0);

        final OptimizationRequest or = new OptimizationRequest();
        or.setF0(objectiveFunction);
        or.setInitialPoint(new double[] { 0.1, 0.1, 0.1 });// LE-infeasible starting point
        or.setA(new double[][] { { 1, 1, 1 } });
        or.setB(new double[] { 1 });

        // optimization
        final AbstractKKTSolver kktSolver = new AugmentedKKTSolver();
        kktSolver.setCheckKKTSolutionAccuracy(true);
        
        final NewtonLEConstrainedISP opt = new NewtonLEConstrainedISP();
        opt.setOptimizationRequest(or);
        opt.setKKTSolver(new BasicKKTSolver());
        opt.setKKTSolver(kktSolver);
        
        try{
            opt.optimize();
        }catch (PatriusException e) {
            assertTrue(true);//ok, residual > toleranceKKT
            return;
        }
        fail();

    }
    
    
    /**
     * Test for checkCustomExitConditions
     * @throws PatriusException
     */
    public void testCheckCustomExitConditions() throws PatriusException {

        // START SNIPPET: NewtonLEConstrainedISP-1

        // commons-math client code
        final RealMatrix pmatrix = new Array2DRowRealMatrix(new double[][] { { 1.68, 0.34, 0.38 },
                { 0.34, 3.09, -1.59 }, { 0.38, -1.59, 1.54 } });
        final RealVector qVector = new ArrayRealVector(new double[] { 0.018, 0.025, 0.01 });

        // Objective function
        final double theta = 0.01522;
        final RealMatrix p = pmatrix.scalarMultiply(theta);
        final RealVector q = qVector.mapMultiply(-1);
        final PDQuadraticMultivariateRealFunction objectiveFunction = new PDQuadraticMultivariateRealFunction(
                p.getData(false), q.toArray(), 0);

        final OptimizationRequest or = new OptimizationRequest();
        or.setF0(objectiveFunction);
        or.setInitialPoint(new double[] { 0.1, 0.1, 0.1 });// LE-infeasible starting point
        or.setA(new double[][] { { 1, 1, 1 } });
        or.setB(new double[] { 1 });

        // optimization
        final TestNewtonLEConstrainedISP opt = new TestNewtonLEConstrainedISP();       
        //final NewtonLEConstrainedISP opt = new NewtonLEConstrainedISP();
        opt.setOptimizationRequest(or);
        opt.setKKTSolver(new BasicKKTSolver());
        final OptimizationRequest req = opt.getOptimizationRequest();
        req.setCheckKKTSolutionAccuracy(true);
        req.setCheckProgressConditions(true);
        final int returnCode = opt.optimize();

        // END SNIPPET: NewtonLEConstrainedISP-1

        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }
    }
    
    /**
     * Private class created to cover checkCustomExitConditions 
     * (it is always false in OptimizationRequestHandler)
     */
    private static class TestNewtonLEConstrainedISP extends NewtonLEConstrainedISP {
        
        public TestNewtonLEConstrainedISP() {
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
