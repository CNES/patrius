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
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealVector;
import fr.cnes.sirius.patrius.math.optim.joptimizer.functions.ConvexMultivariateRealFunction;
import fr.cnes.sirius.patrius.math.optim.joptimizer.functions.PDQuadraticMultivariateRealFunction;
import fr.cnes.sirius.patrius.math.optim.joptimizer.solvers.AbstractKKTSolver;
import fr.cnes.sirius.patrius.math.optim.joptimizer.solvers.BasicKKTSolver;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * @author alberto trivellato (alberto.trivellato@gmail.com)
 */
public class NewtonLEConstrainedFSPTest extends TestCase {
    
    /**
     * 
     * @throws PatriusException if an error occurs
     */
    public void testOptimize() throws PatriusException {
        final RealMatrix pMatrix = new BlockRealMatrix(new double[][] { { 1.68, 0.34, 0.38 },
                { 0.34, 3.09, -1.59 }, { 0.38, -1.59, 1.54 } });
        final RealVector qVector = new ArrayRealVector(new double[] { 0.018, 0.025, 0.01 });

        // Objective function (Risk-Aversion).
        final double theta = 0.01522;
        final double[][] p = pMatrix.scalarMultiply(theta).getData(false);
        final double[] q = qVector.mapMultiply(-1).toArray();
        final PDQuadraticMultivariateRealFunction objectiveFunction = new PDQuadraticMultivariateRealFunction(
                p, q, 0);

        final OptimizationRequest or = new OptimizationRequest();
        or.setF0(objectiveFunction);
        or.setInitialPoint(new double[] { 0.8, 0.1, 0.1 });
        or.setA(new double[][] { { 1, 1, 1 } });
        or.setB(new double[] { 1 });

        // optimization
        final NewtonLEConstrainedFSP opt = new NewtonLEConstrainedFSP();
        opt.setOptimizationRequest(or);
        final int returnCode = opt.optimize();

        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }

        final OptimizationResponse response = opt.getOptimizationResponse();
        final double[] sol = response.getSolution();
        assertEquals(0.04632311555988555, sol[0], 0.00000000000001);
        assertEquals(0.5086308460954377, sol[1], 0.00000000000001);
        assertEquals(0.44504603834467693, sol[2], 0.00000000000001);
    }

    /**
     * Minimize x - Log[-x^2 + 1],
     * dom f ={x | x^2<1}
     * N.B.: this simulate a centering step of the barrier method
     * applied to the problem:
     * Minimize x
     * s.t. x^2<1
     * when t=1.
     * 
     * @throws PatriusException if an error occurs
     */
    public void testOptimize2() throws PatriusException {

        // START SNIPPET: NewtonLEConstrainedFSP-1

        // Objective function
        final ConvexMultivariateRealFunction objectiveFunction = new ConvexMultivariateRealFunction() {

            @Override
            public double value(double[] valueX) {
                final double x = valueX[0];
                return x - Math.log(1 - x * x);
            }

            @Override
            public double[] gradient(double[] valueX) {
                final double x = valueX[0];
                return new double[] { 1 + 2 * x / (1 - x * x) };
            }

            @Override
            public double[][] hessian(double[] valueX) {
                final double x = valueX[0];
                return new double[][] { { 4 * Math.pow(x, 2) / Math.pow(1 - x * x, 2) + 2
                        / (1 - x * x) } };
            }

            @Override
            public int getDim() {
                return 1;
            }
        };

        final OptimizationRequest or = new OptimizationRequest();
        or.setCheckKKTSolutionAccuracy(true);
        or.setF0(objectiveFunction);
        or.setInitialPoint(new double[] { 0 });// must be feasible

        // optimization
        final NewtonLEConstrainedFSP opt = new NewtonLEConstrainedFSP();
        opt.setOptimizationRequest(or);
        final int returnCode = opt.optimize();

        // END SNIPPET: NewtonLEConstrainedFSP-1

        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }

        final OptimizationResponse response = opt.getOptimizationResponse();
        final double[] sol = response.getSolution();
        final double value = objectiveFunction.value(sol);
        assertEquals(-0.41421356, sol[0], 0.0000001);// =1-Math.sqrt(2)
        assertEquals(-0.22598716, value, 0.0000001);
    }

    /**
     * Minimize 100(2x+y) - Log[x] - Log[y],
     * s.t. x+y=1
     * N.B.: this simulate a centering step of the barrier method
     * applied to the problem:
     * Minimize 2x + y
     * s.t. -x<0,
     * -y<0
     * x+y=1
     * when t=100;
     * 
     * @throws PatriusException if an error occurs
     */
    public void testOptimize3() throws PatriusException {

        // Objective function (linear)
        final ConvexMultivariateRealFunction objectiveFunction = new ConvexMultivariateRealFunction() {

            @Override
            public double value(double[] valueX) {
                final double x = valueX[0];
                final double y = valueX[1];
                return 100 * (2 * x + y) - Math.log(x) - Math.log(y);
            }

            @Override
            public double[] gradient(double[] valueX) {
                final double x = valueX[0];
                final double y = valueX[1];
                return new double[] { 200 - 1. / x, 100 - 1. / y };
            }

            @Override
            public double[][] hessian(double[] valueX) {
                final double x = valueX[0];
                final double y = valueX[1];
                return new double[][] { { 1. / Math.pow(x, 2), 0 }, { 0, 1. / Math.pow(y, 2) } };
            }

            @Override
            public int getDim() {
                return 2;
            }
        };

        final OptimizationRequest or = new OptimizationRequest();
        or.setF0(objectiveFunction);
        or.setInitialPoint(new double[] { 0.0900980486377967, 0.9099019513622053 });
        or.setA(new double[][] { { 1, 1 } });
        or.setB(new double[] { 1 });

        // optimization
        final NewtonLEConstrainedFSP opt = new NewtonLEConstrainedFSP(true);
        opt.setOptimizationRequest(or);
        final int returnCode = opt.optimize();

        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }

        final OptimizationResponse response = opt.getOptimizationResponse();
        final double[] sol = response.getSolution();
        assertEquals(0., sol[0], 0.01);
        assertEquals(1., sol[1], 0.01);
        assertEquals(1., sol[0] + sol[1], 0.000000000001);// check constraint
    }
    
    /**
     * Not initial point
     */
    public void testError() {
        final RealMatrix pMatrix = new BlockRealMatrix(new double[][] { { 1.68, 0.34, 0.38 },
                { 0.34, 3.09, -1.59 }, { 0.38, -1.59, 1.54 } });
        final RealVector qVector = new ArrayRealVector(new double[] { 0.018, 0.025, 0.01 });

        final double theta = 0.01522;
        final double[][] p = pMatrix.scalarMultiply(theta).getData(false);
        final double[] q = qVector.mapMultiply(-1).toArray();
        final PDQuadraticMultivariateRealFunction objectiveFunction = new PDQuadraticMultivariateRealFunction(
                p, q, 0, true);

        final OptimizationRequest or = new OptimizationRequest();
        or.setF0(objectiveFunction);
        //Initial point is not set
        or.setA(new double[][] { { 1, 1, 1 } });
        or.setB(new double[] { 1 });

        // optimization
        final NewtonLEConstrainedFSP opt = new NewtonLEConstrainedFSP();
        opt.setOptimizationRequest(or);
        
        try{
            opt.optimize();
        }catch(PatriusException e){
            assertTrue(true);//ok, there is no initial point
            return;
        }
        fail();
    }
    
    /**
     * Test optimize with maximum iterations assigned to 1
     * -> it will throw an error
     * @throws PatriusException 
     */
    public void testMaxIterations() throws PatriusException  {
        final RealMatrix pMatrix = new BlockRealMatrix(new double[][] { { 1.68, 0.34, 0.38 },
                { 0.34, 3.09, -1.59 }, { 0.38, -1.59, 1.54 } });
        final RealVector qVector = new ArrayRealVector(new double[] { 0.018, 0.025, 0.01 });

        final double theta = 0.01522;
        final double[][] p = pMatrix.scalarMultiply(theta).getData(false);
        final double[] q = qVector.mapMultiply(-1).toArray();
        final PDQuadraticMultivariateRealFunction objectiveFunction = new PDQuadraticMultivariateRealFunction(
                p, q, 0, true);

        final OptimizationRequest or = new OptimizationRequest();
        or.setF0(objectiveFunction);
        or.setInitialPoint(new double[] { 0.8, 0.1, 0.1 });
        or.setMaxIteration(1);

        // optimization
        final NewtonLEConstrainedFSP opt = new NewtonLEConstrainedFSP();
        opt.setOptimizationRequest(or);
        final AbstractKKTSolver solver = new BasicKKTSolver();
        opt.setKKTSolver(solver);
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
                { 0.34, 3.09, 1.59 }, { 0.38, 1.59, 1.54 } });
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
        final NewtonLEConstrainedFSP opt = new NewtonLEConstrainedFSP();
        opt.setOptimizationRequest(or);
        if (opt.optimize() == OptimizationResponse.FAILED) {
            assertTrue(true);//ok, no progress achieved
            return;
        }
        fail();
    }

}
