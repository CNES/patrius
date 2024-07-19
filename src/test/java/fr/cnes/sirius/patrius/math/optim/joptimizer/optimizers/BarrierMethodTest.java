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
import fr.cnes.sirius.patrius.math.linear.ArrayRealVector;
import fr.cnes.sirius.patrius.math.linear.BlockRealMatrix;
import fr.cnes.sirius.patrius.math.linear.MatrixUtils;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealVector;
import fr.cnes.sirius.patrius.math.optim.joptimizer.functions.BarrierFunction;
import fr.cnes.sirius.patrius.math.optim.joptimizer.functions.ConvexMultivariateRealFunction;
import fr.cnes.sirius.patrius.math.optim.joptimizer.functions.FunctionsUtils;
import fr.cnes.sirius.patrius.math.optim.joptimizer.functions.LinearMultivariateRealFunction;
import fr.cnes.sirius.patrius.math.optim.joptimizer.functions.LogarithmicBarrier;
import fr.cnes.sirius.patrius.math.optim.joptimizer.functions.PDQuadraticMultivariateRealFunction;
import fr.cnes.sirius.patrius.math.optim.joptimizer.functions.PSDQuadraticMultivariateRealFunction;
import fr.cnes.sirius.patrius.math.optim.joptimizer.functions.StrictlyConvexMultivariateRealFunction;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * @author alberto trivellato (alberto.trivellato@gmail.com)
 */
public class BarrierMethodTest extends TestCase {

    /**
     * Quadratic objective with linear eq and ineq.
     * @throws PatriusException 
     */
    public void testOptimize() throws PatriusException {
        final RealMatrix pMatrix = new 
                BlockRealMatrix(new double[][] {{1.68, 0.34, 0.38 },{ 0.34, 3.09, -1.59 },{ 0.38, -1.59, 1.54 }});
        final RealVector qVector = new ArrayRealVector(new double[] { 0.018, 0.025, 0.01 });

        // Objective function
        final double theta = 0.01522;
        final RealMatrix p = pMatrix.scalarMultiply(theta);
        final RealVector q = qVector.mapMultiply(-1);
        final PDQuadraticMultivariateRealFunction objectiveFunction = new PDQuadraticMultivariateRealFunction(
                p.getData(false), q.toArray(), 0);

        final ConvexMultivariateRealFunction[] inequalities = new ConvexMultivariateRealFunction[3];
        inequalities[0] = new LinearMultivariateRealFunction(new double[] { -1, 0, 0 }, 0);
        inequalities[1] = new LinearMultivariateRealFunction(new double[] { 0, -1, 0 }, 0);
        inequalities[2] = new LinearMultivariateRealFunction(new double[] { 0, 0, -1 }, 0);

        final OptimizationRequest or = new OptimizationRequest();
        or.setCheckKKTSolutionAccuracy(true);
        or.setCheckProgressConditions(true);
        or.setF0(objectiveFunction);
        or.setInitialPoint(new double[] { 0.6, 0.2, 0.2 });
        // equalities
        or.setA(new double[][] { { 1, 1, 1 } });
        or.setB(new double[] { 1 });
        // tolerances
        or.setTolerance(1.E-5);

        // optimization
        final BarrierFunction bf = new LogarithmicBarrier(inequalities, 3);
        final BarrierMethod opt = new BarrierMethod(bf);
        opt.setOptimizationRequest(or);
        final int returnCode = opt.optimize();

        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }

        final OptimizationResponse response = opt.getOptimizationResponse();
        final double[] expectedSol = { 0.04632311555988555, 0.5086308460954377, 0.44504603834467693 };
        final double expectedValue = objectiveFunction.value(expectedSol);
        final double[] sol = response.getSolution();
        final double value = objectiveFunction.value(sol);
        assertEquals(expectedValue, value, or.getTolerance());
    }

    /**
     * Quadratic objective with linear eq and ineq
     * with not-feasible initial point.
     * 
     * @throws PatriusException if an error occurs
     */
    public void testOptimize2() throws PatriusException {
        final RealMatrix pMatrix = new BlockRealMatrix(new double[][] { { 1.68, 0.34, 0.38 },
                { 0.34, 3.09, -1.59 }, { 0.38, -1.59, 1.54 } });
        final RealVector qVector = new ArrayRealVector(new double[] { 0.018, 0.025, 0.01 });

        // Objective function.
        final double theta = 0.01522;
        final RealMatrix p = pMatrix.scalarMultiply(theta);
        final RealVector q = qVector.mapMultiply(-1);
        final PDQuadraticMultivariateRealFunction objectiveFunction = new PDQuadraticMultivariateRealFunction(
                p.getData(false), q.toArray(), 0);

        final ConvexMultivariateRealFunction[] inequalities = new ConvexMultivariateRealFunction[3];
        inequalities[0] = new LinearMultivariateRealFunction(new double[] { -1, 0, 0 }, 0);
        inequalities[1] = new LinearMultivariateRealFunction(new double[] { 0, -1, 0 }, 0);
        inequalities[2] = new LinearMultivariateRealFunction(new double[] { 0, 0, -1 }, 0);

        final OptimizationRequest or = new OptimizationRequest();
        or.setNotFeasibleInitialPoint(new double[] { -0.2, 1.0, 0.2 });
        or.setCheckKKTSolutionAccuracy(true);
        or.setF0(objectiveFunction);
        // equalities
        or.setA(new double[][] { { 1, 1, 1 } });
        or.setB(new double[] { 1 });
        // tolerances
        or.setTolerance(1.E-5);

        // optimization
        final BarrierFunction bf = new LogarithmicBarrier(inequalities, 3);
        final BarrierMethod opt = new BarrierMethod(bf);
        opt.setOptimizationRequest(or);
        final int returnCode = opt.optimize();

        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }

        final OptimizationResponse response = opt.getOptimizationResponse();
        final double[] expectedSol = { 0.04632311555988555, 0.5086308460954377, 0.44504603834467693 };
        final double expectedValue = objectiveFunction.value(expectedSol);
        final double[] sol = response.getSolution();
        final double value = objectiveFunction.value(sol);
        assertEquals(expectedValue, value, or.getTolerance());
    }

    /**
     * Quadratic objective with linear eq and ineq
     * without initial point.
     * 
     * @throws PatriusException if an error occurs
     */
    public void testOptimize3() throws PatriusException {
        final RealMatrix pMatrix = new BlockRealMatrix(new double[][] { { 1.68, 0.34, 0.38 },
                { 0.34, 3.09, -1.59 }, { 0.38, -1.59, 1.54 } });
        final RealVector qVector = new ArrayRealVector(new double[] { 0.018, 0.025, 0.01 });

        // Objective function.
        final double theta = 0.01522;
        final RealMatrix p = pMatrix.scalarMultiply(theta);
        final RealVector q = qVector.mapMultiply(-1);
        final PDQuadraticMultivariateRealFunction objectiveFunction = new PDQuadraticMultivariateRealFunction(
                p.getData(false), q.toArray(), 0);

        final ConvexMultivariateRealFunction[] inequalities = new ConvexMultivariateRealFunction[3];
        inequalities[0] = new LinearMultivariateRealFunction(new double[] { -1, 0, 0 }, 0);
        inequalities[1] = new LinearMultivariateRealFunction(new double[] { 0, -1, 0 }, 0);
        inequalities[2] = new LinearMultivariateRealFunction(new double[] { 0, 0, -1 }, 0);

        final OptimizationRequest or = new OptimizationRequest();
        or.setCheckKKTSolutionAccuracy(true);
        or.setF0(objectiveFunction);
        // equalities
        or.setA(new double[][] { { 1, 1, 1 } });
        or.setB(new double[] { 1 });
        // tolerances
        or.setTolerance(1.E-5);

        // optimization
        final BarrierFunction bf = new LogarithmicBarrier(inequalities, 3);
        final BarrierMethod opt = new BarrierMethod(bf);
        opt.setOptimizationRequest(or);
        final int returnCode = opt.optimize();

        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }

        final OptimizationResponse response = opt.getOptimizationResponse();
        final double[] expectedSol = { 0.04632311555988555, 0.5086308460954377, 0.44504603834467693 };
        final double expectedValue = objectiveFunction.value(expectedSol);
        final double[] sol = response.getSolution();
        final double value = objectiveFunction.value(sol);
        assertEquals(expectedValue, value, or.getTolerance());
    }

    /**
     * Quadratic objective with linear eq and quadratic ineq.
     * 
     * @throws PatriusException if an error occurs
     */
    public void testOptimize4() throws PatriusException {
        final RealMatrix pMatrix = new BlockRealMatrix(new double[][] { { 1.68, 0.34, 0.38 },
                { 0.34, 3.09, -1.59 }, { 0.38, -1.59, 1.54 } });
        final RealVector qVector = new ArrayRealVector(new double[] { 0.018, 0.025, 0.01 });

        // Objective function (Risk-Aversion).
        final double theta = 0.01522;
        final RealMatrix p = pMatrix.scalarMultiply(theta);
        final RealVector q = qVector.mapMultiply(-1);
        final PDQuadraticMultivariateRealFunction objectiveFunction = new PDQuadraticMultivariateRealFunction(
                p.getData(false), q.toArray(), 0);

        final ConvexMultivariateRealFunction[] inequalities = new ConvexMultivariateRealFunction[4];
        inequalities[0] = new LinearMultivariateRealFunction(new double[] { -1, 0, 0 }, 0);
        inequalities[1] = new LinearMultivariateRealFunction(new double[] { 0, -1, 0 }, 0);
        inequalities[2] = new LinearMultivariateRealFunction(new double[] { 0, 0, -1 }, 0);
        inequalities[3] = FunctionsUtils.createCircle(3, 5);// not linear

        final OptimizationRequest or = new OptimizationRequest();
        or.setCheckKKTSolutionAccuracy(true);
        or.setF0(objectiveFunction);
        or.setInitialPoint(new double[] { 0.2, 0.6, 0.2 });
        or.setInitialLagrangian(new double[] { 0.5, 0.5, 0.5, 0.5 });
        // Equality constraints
        or.setA(new double[][] { { 1, 1, 1 } });
        or.setB(new double[] { 1 });
        // tolerances
        or.setTolerance(1.E-5);

        // optimization
        final BarrierFunction bf = new LogarithmicBarrier(inequalities, 3);
        final BarrierMethod opt = new BarrierMethod(bf);
        opt.setOptimizationRequest(or);
        final int returnCode = opt.optimize();

        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }

        final OptimizationResponse response = opt.getOptimizationResponse();
        final double[] expectedSol = { 0.04632311555988555, 0.5086308460954377, 0.44504603834467693 };
        final double expectedValue = objectiveFunction.value(expectedSol);
        final double[] sol = response.getSolution();
        final double value = objectiveFunction.value(sol);
        assertEquals(expectedValue, value, or.getTolerance());
    }

    /**
     * Linear objective with quadratic ineq.
     * 
     * @throws PatriusException if an error occurs
     */
    public void testOptimize1D() throws PatriusException {
        // Objective function (linear)
        final LinearMultivariateRealFunction objectiveFunction = new LinearMultivariateRealFunction(
                new double[] { 1 }, 0);

        // Inequality constraints
        final ConvexMultivariateRealFunction[] inequalities = new ConvexMultivariateRealFunction[1];
        inequalities[0] = FunctionsUtils.createCircle(1, 1);// dim=1, radius=1, center=(0,0)
        assertEquals(1,inequalities[0].getDim());

        final OptimizationRequest or = new OptimizationRequest();
        or.setCheckKKTSolutionAccuracy(true);
        or.setF0(objectiveFunction);
        or.setInitialPoint(new double[] { 0 });
        or.setTolerance(1.E-6);

        // optimization
        final BarrierFunction bf = new LogarithmicBarrier(inequalities, 1);
        final BarrierMethod opt = new BarrierMethod(bf);
        opt.setOptimizationRequest(or);
        final int returnCode = opt.optimize();

        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }

        final OptimizationResponse response = opt.getOptimizationResponse();
        final double[] sol = response.getSolution();
        final double value = objectiveFunction.value(sol);
        assertEquals(-1, value, 0.00001);
    }

    /**
     * Linear objective with quadratic ineq.
     * 
     * @throws PatriusException if an error occurs
     */
    public void testOptimize5() throws PatriusException {
        // START SNIPPET: BarrierMethod-1

        // Objective function (linear)
        final LinearMultivariateRealFunction objectiveFunction = new LinearMultivariateRealFunction(
                new double[] { 1, 1 }, 0);

        // Inequality constraints
        final ConvexMultivariateRealFunction[] inequalities = new ConvexMultivariateRealFunction[1];
        inequalities[0] = FunctionsUtils.createCircle(2, 1);// dim=2, radius=1, center=(0,0)

        final OptimizationRequest or = new OptimizationRequest();
        or.setF0(objectiveFunction);
        or.setInitialPoint(new double[] { 0, 0 });
        or.setTolerance(1.E-5);

        // optimization
        final BarrierFunction bf = new LogarithmicBarrier(inequalities, 2);
        final BarrierMethod opt = new BarrierMethod(bf);
        opt.setOptimizationRequest(or);
        final int returnCode = opt.optimize();

        // END SNIPPET: BarrierMethod-1

        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }

        final OptimizationResponse response = opt.getOptimizationResponse();
        final double[] expectedSol = { -Math.sqrt(2) / 2, -Math.sqrt(2) / 2 };
        final double expectedValue = objectiveFunction.value(expectedSol);
        final double[] sol = response.getSolution();
        final double value = objectiveFunction.value(sol);
        assertEquals(expectedValue, value, 0.0001);// -1,41421356237
    }

    /**
     * Very simple linear.
     * 
     * @throws PatriusException if an error occurs
     */
    public void testSimpleLinear() throws PatriusException {
        // START SNIPPET: BarrierMethod-2

        // Objective function (plane)
        final double[] c = new double[] { 1., 1. };
        final LinearMultivariateRealFunction objectiveFunction = new LinearMultivariateRealFunction(c, 0.);

        // inequalities
        final ConvexMultivariateRealFunction[] inequalities = new ConvexMultivariateRealFunction[4];
        inequalities[0] = new LinearMultivariateRealFunction(new double[] { 1., 0. }, -3.);
        inequalities[1] = new LinearMultivariateRealFunction(new double[] { -1., 0. }, 0.);
        inequalities[2] = new LinearMultivariateRealFunction(new double[] { 0., 1. }, -3.);
        inequalities[3] = new LinearMultivariateRealFunction(new double[] { 0., -1. }, 0.);

        // optimization problem
        final OptimizationRequest or = new OptimizationRequest();
        or.setInteriorPointMethod(JOptimizer.BARRIER_METHOD);// select the barrier interior-point
                                                             // method
        or.setF0(objectiveFunction);
        or.setFi(inequalities);
        or.setTolerance(1.E-5);

        // optimization
        final JOptimizer opt = new JOptimizer();
        opt.setOptimizationRequest(or);
        final int returnCode = opt.optimize();

        // END SNIPPET: BarrierMethod-2

        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }

        final OptimizationResponse response = opt.getOptimizationResponse();
        final double[] expectedSol = { 0, 0 };
        final double expectedValue = objectiveFunction.value(expectedSol);
        final double[] sol = response.getSolution();
        final double value = objectiveFunction.value(sol);
        assertEquals(expectedValue, value, 0.0001);
    }

    /**
     * Linear objective with linear eq and ineq.
     * 
     * @throws PatriusException if an error occurs
     */
    public void testOptimize6() throws PatriusException {

        // Objective function (linear)
        final LinearMultivariateRealFunction objectiveFunction = new LinearMultivariateRealFunction(
                new double[] { 2, 1 }, 0);

        // Inequality constraints
        final ConvexMultivariateRealFunction[] inequalities = new ConvexMultivariateRealFunction[2];
        inequalities[0] = new LinearMultivariateRealFunction(new double[] { -1, 0 }, 0);
        inequalities[1] = new LinearMultivariateRealFunction(new double[] { 0, -1 }, 0);

        final OptimizationRequest or = new OptimizationRequest();
        or.setF0(objectiveFunction);
        or.setInitialPoint(new double[] { 0.9, 0.1 });
        // Equality constraints
        or.setA(new double[][] { { 1, 1 } });
        or.setB(new double[] { 1 });
        or.setCheckKKTSolutionAccuracy(true);
        // or.setCheckProgressConditions(true);
        or.setToleranceInnerStep(JOptimizer.DEFAULT_TOLERANCE_INNER_STEP * 10);
        or.setTolerance(JOptimizer.DEFAULT_TOLERANCE / 10);

        // optimization
        final BarrierFunction bf = new LogarithmicBarrier(inequalities, 2);
        final BarrierMethod opt = new BarrierMethod(bf);
        opt.setOptimizationRequest(or);
        final int returnCode = opt.optimize();

        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }

        final OptimizationResponse response = opt.getOptimizationResponse();
        final double[] expectedSol = { 0., 1. };
        final double expectedValue = objectiveFunction.value(expectedSol);
        final double[] sol = response.getSolution();
        final double value = objectiveFunction.value(sol);
        assertEquals(expectedValue, value, or.getTolerance());
    }

    /**
     * Linear objective with quadratic ineq
     * and without initial point.
     * 
     * @throws PatriusException if an error occurs
     */
    public void testOptimize7() throws PatriusException {

        // Objective function (linear)
        final LinearMultivariateRealFunction objectiveFunction = new LinearMultivariateRealFunction(
                new double[] { 1, 1 }, 0);

        // Inequality constraints
        final ConvexMultivariateRealFunction[] inequalities = new ConvexMultivariateRealFunction[1];
        inequalities[0] = FunctionsUtils.createCircle(2, 1);

        final OptimizationRequest or = new OptimizationRequest();
        or.setF0(objectiveFunction);

        // optimization
        final BarrierFunction bf = new LogarithmicBarrier(inequalities, 2);
        final BarrierMethod opt = new BarrierMethod(bf);
        opt.setOptimizationRequest(or);
        final int returnCode = opt.optimize();

        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }

        final OptimizationResponse response = opt.getOptimizationResponse();
        final double[] expectedSol = { -Math.sqrt(2) / 2, -Math.sqrt(2) / 2 };
        final double expectedValue = objectiveFunction.value(expectedSol);
        final double[] sol = response.getSolution();
        final double value = objectiveFunction.value(sol);
        assertEquals(expectedValue, value, 0.0001);
    }

    /**
     * Linear objective with quadratic ineq
     * and with infeasible initial point.
     * min(t) s.t.
     * x^2 <ty
     * 
     * @throws PatriusException if an error occurs
     */
    public void testOptimize7b() throws PatriusException {

        // Objective function (linear)
        final LinearMultivariateRealFunction objectiveFunction = new LinearMultivariateRealFunction(
                new double[] { 0, 1 }, 0);

        // Inequality constraints
        final ConvexMultivariateRealFunction[] inequalities = new ConvexMultivariateRealFunction[1];
        final double[][] pMatrix = new double[][] { { 2, 0 }, { 0, 0 } };
        final double[] qVector = new double[] { 0, -1 };
        inequalities[0] = new PSDQuadraticMultivariateRealFunction(pMatrix, qVector, 0, true);
        // inequalities[1] = new LinearMultivariateRealFunction(new double[]{0, -1}, 0);

        final OptimizationRequest or = new OptimizationRequest();
        or.setF0(objectiveFunction);
        // or.setNotFeasibleInitialPoint(new double[]{-1, 0.9999999});//this fails, the KKT system
        // for the Phase1 problem is singular
        or.setNotFeasibleInitialPoint(new double[] { -1, 1.0000001 });

        // optimization
        final BarrierFunction bf = new LogarithmicBarrier(inequalities, 2);
        final BarrierMethod opt = new BarrierMethod(bf);
        opt.setOptimizationRequest(or);
        final int returnCode = opt.optimize();

        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }

        final OptimizationResponse response = opt.getOptimizationResponse();
        final double[] expectedSol = { 0., 0. };
        final double expectedValue = objectiveFunction.value(expectedSol);
        final double[] sol = response.getSolution();
        final double value = objectiveFunction.value(sol);
        assertEquals(expectedValue, value, 0.0001);
    }

    /**
     * Linear objective, quadratically constrained.
     * It simulates the type of optimization occurring in feasibility searching
     * in a problem with constraints:
     * x^2 < 1
     * 
     * @throws PatriusException if an error occurs
     */
    public void testQCQuadraticProgramming() throws PatriusException {

        // Objective function (linear (x,s)->s)
        final double[] c0 = new double[] { 0, 1 };
        final LinearMultivariateRealFunction objectiveFunction = new LinearMultivariateRealFunction(c0, 0);

        // inequalities x^2 < 1 + s
        final ConvexMultivariateRealFunction[] inequalities = new ConvexMultivariateRealFunction[1];
        final double[][] p1 = new double[][] { { 2., 0. }, { 0., 0. } };
        final double[] c1 = new double[] { 0, -1 };
        inequalities[0] = new PSDQuadraticMultivariateRealFunction(p1, c1, -1);

        // optimization problem
        final OptimizationRequest or = new OptimizationRequest();
        or.setF0(objectiveFunction);
        or.setInitialPoint(new double[] { 2, 5 });

        // optimization
        final BarrierFunction bf = new LogarithmicBarrier(inequalities, 2);
        final BarrierMethod opt = new BarrierMethod(bf);
        opt.setOptimizationRequest(or);
        final int returnCode = opt.optimize();

        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }

        final OptimizationResponse response = opt.getOptimizationResponse();
        final double[] expectedSol = { 0., -1. };
        final double expectedValue = objectiveFunction.value(expectedSol);
        final double[] sol = response.getSolution();
        final double value = objectiveFunction.value(sol);
        assertEquals(expectedValue, value, 0.0001);
    }

    /**
     * Exponential objective with quadratic ineq.
     * f0 = exp[z^2], z=(x-1, y-2)
     * f1 = x^2+y^2<=3^2
     * 
     * @throws PatriusException if an error occurs
     */
    public void testOptimize8() throws PatriusException {
        final StrictlyConvexMultivariateRealFunction objectiveFunction = new StrictlyConvexMultivariateRealFunction() {

            @Override
            public double value(double[] x) {
                final RealVector z = new ArrayRealVector(new double[] { x[0] - 1, x[1] - 2, });
                return Math.exp(z.dotProduct(z));
            }

            @Override
            public double[] gradient(double[] x) {
                final RealVector z = new ArrayRealVector(new double[] { x[0] - 1, x[1] - 2, });
                return z.mapMultiply(2 * Math.exp(z.dotProduct(z))).toArray();
            }

            @Override
            public double[][] hessian(double[] x) {
                final RealVector z = new ArrayRealVector(new double[] { x[0] - 1, x[1] - 2, });
                final double d = Math.exp(z.dotProduct(z));
                final RealMatrix iM = MatrixUtils.createRealIdentityMatrix(2);
                final RealMatrix ret = z.outerProduct(z).add(iM).scalarMultiply(2 * d);
                return ret.getData();
            }

            @Override
            public int getDim() {
                return 2;
            }
        };

        // Inequality constraints
        final ConvexMultivariateRealFunction[] inequalities = new ConvexMultivariateRealFunction[1];
        inequalities[0] = FunctionsUtils.createCircle(2, 3);

        final OptimizationRequest or = new OptimizationRequest();
        or.setF0(objectiveFunction);
        or.setInitialPoint(new double[] { 0.2, 0.2 });

        // optimization
        final BarrierFunction bf = new LogarithmicBarrier(inequalities, 2);
        final BarrierMethod opt = new BarrierMethod(bf);
        opt.setOptimizationRequest(or);
        final int returnCode = opt.optimize();

        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }

        final OptimizationResponse response = opt.getOptimizationResponse();
        final double[] expectedSol = { 1., 2. };
        final double expectedValue = objectiveFunction.value(expectedSol);
        final double[] sol = response.getSolution();
        final double value = objectiveFunction.value(sol);
        assertEquals(expectedValue, value, or.getTolerance());
    }
    
    /**
     * Test optimize with non feasible initial point set as initial point
     * -> it throws an exception
     */
    public void testOptimizeNFPointError() {
        
        // Inequality constraints
        final ConvexMultivariateRealFunction[] inequalities = new ConvexMultivariateRealFunction[1];
        inequalities[0] = FunctionsUtils.createCircle(1, 1);// dim=1, radius=1, center=(0,0)

        final OptimizationRequest or = new OptimizationRequest();
        or.setInitialPoint(new double[] {Double.NaN}); //non feasible

        final BarrierFunction bf = new LogarithmicBarrier(inequalities, 1);
        final BarrierMethod opt = new BarrierMethod(bf);
        opt.setOptimizationRequest(or);
        try{
            opt.optimize();
        }catch (PatriusException e) {
            assertTrue(true);//ok, initial point non feasible
            return;
        }
        fail();    
    }
    
    /**
     * Test optimize with maximum iterations set to 1
     * -> it throws an exception 
     * @throws PatriusException if an error occurs
     */
    public void testOptimizeMaxIterations() throws PatriusException {

        // Objective function (linear)
        final LinearMultivariateRealFunction objectiveFunction = new LinearMultivariateRealFunction(
                new double[] { 1 }, 0);

        // Inequality constraints
        final ConvexMultivariateRealFunction[] inequalities = new ConvexMultivariateRealFunction[1];
        inequalities[0] = FunctionsUtils.createCircle(1, 1);// dim=1, radius=1, center=(0,0)

        final OptimizationRequest or = new OptimizationRequest();
        or.setCheckKKTSolutionAccuracy(true);
        or.setF0(objectiveFunction);
        or.setInitialPoint(new double[] { 0 });
        or.setTolerance(1.E-6);
        or.setMaxIteration(1);

        // optimization
        final BarrierFunction bf = new LogarithmicBarrier(inequalities, 1);
        final BarrierMethod opt = new BarrierMethod(bf);
        opt.setOptimizationRequest(or);
        if (opt.optimize() == OptimizationResponse.FAILED) {
            assertTrue(true);//ok, max iterations reached
            return;
        }
        fail();  
    }
    
    /**
     * Test findFeasibleInitialPoint from BasicPhaseIBM
     * with a toleranceFeas that the problem cannot achieve -> it throws an error
     */
    public void testOptimizeToleranceError() {
        final RealMatrix pMatrix = new 
                BlockRealMatrix(new double[][] {{1.68, 0.34, 0.38 },{ 0.34, 3.09, -1.59 },{ 0.38, -1.59, 1.54 }});
        final RealVector qVector = new ArrayRealVector(new double[] { 0.018, 0.025, 0.01 });

        // Objective function
        final double theta = 0.01522;
        final RealMatrix p = pMatrix.scalarMultiply(theta);
        final RealVector q = qVector.mapMultiply(-1);
        final PDQuadraticMultivariateRealFunction objectiveFunction = new PDQuadraticMultivariateRealFunction(
                p.getData(false), q.toArray(), 0);

        final ConvexMultivariateRealFunction[] inequalities = new ConvexMultivariateRealFunction[3];
        inequalities[0] = new LinearMultivariateRealFunction(new double[] { -1, 0, 0 }, 0);
        inequalities[1] = new LinearMultivariateRealFunction(new double[] { 0, -1, 0 }, 0);
        inequalities[2] = new LinearMultivariateRealFunction(new double[] { 0, 0, -1 }, 0);

        final OptimizationRequest or = new OptimizationRequest();
        or.setF0(objectiveFunction);
        // equalities
        or.setA(new double[][] { { 1, 1, 1 } });
        or.setB(new double[] { 1 });
        // tolerances
        or.setTolerance(1.E-5);
        or.setToleranceFeas(1.E-50);

        // optimization
        final BarrierFunction bf = new LogarithmicBarrier(inequalities, 3);
        final BarrierMethod opt = new BarrierMethod(bf);
        opt.setOptimizationRequest(or);
        try{
            opt.optimize();
        }catch(PatriusException e){
            assertTrue(true);//ok, tolerance cannot be achieved
            return;
        }
        fail();
    }
       
    /**
     * Linear objective with quadratic ineq.
     * 
     * Same ad testOptimize1D but without initial point
     * @throws PatriusException if an error occurs
     */
    public void testOptimize1Db() throws PatriusException {

        // Objective function (linear)
        final LinearMultivariateRealFunction objectiveFunction = new LinearMultivariateRealFunction(
                new double[] { 1 }, 0);

        // Inequality constraints
        final ConvexMultivariateRealFunction[] inequalities = new ConvexMultivariateRealFunction[1];
        inequalities[0] = FunctionsUtils.createCircle(1, 1);// dim=1, radius=1, center=(0,0)
        assertEquals(1,inequalities[0].getDim());

        final OptimizationRequest or = new OptimizationRequest();
        or.setCheckKKTSolutionAccuracy(true);
        or.setF0(objectiveFunction);
        or.setTolerance(1.E-6);

        // optimization
        final BarrierFunction bf = new LogarithmicBarrier(inequalities, 1);
        final BarrierMethod opt = new BarrierMethod(bf);
        opt.setOptimizationRequest(or);
        final int returnCode = opt.optimize();

        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }

        final OptimizationResponse response = opt.getOptimizationResponse();
        final double[] sol = response.getSolution();
        final double value = objectiveFunction.value(sol);
        assertEquals(-1, value, 0.00001);
    }
    
    /**
     * Quadratic objective with linear eq and ineq.
     * 
     * Same as testOptimize4 but without initialLagrangian
     * @throws PatriusException 
     */
    public void testOptimize4b() throws PatriusException {
        final RealMatrix pMatrix = new 
                BlockRealMatrix(new double[][] {{1.68, 0.34, 0.38 },{ 0.34, 3.09, -1.59 },{ 0.38, -1.59, 1.54 }});
        final RealVector qVector = new ArrayRealVector(new double[] { 0.018, 0.025, 0.01 });

        // Objective function
        final double theta = 0.01522;
        final RealMatrix p = pMatrix.scalarMultiply(theta);
        final RealVector q = qVector.mapMultiply(-1);
        final PDQuadraticMultivariateRealFunction objectiveFunction = new PDQuadraticMultivariateRealFunction(
                p.getData(false), q.toArray(), 0);

        final ConvexMultivariateRealFunction[] inequalities = new ConvexMultivariateRealFunction[3];
        inequalities[0] = new LinearMultivariateRealFunction(new double[] { -1, 0, 0 }, 0);
        inequalities[1] = new LinearMultivariateRealFunction(new double[] { 0, -1, 0 }, 0);
        inequalities[2] = new LinearMultivariateRealFunction(new double[] { 0, 0, -1 }, 0);

        final OptimizationRequest or = new OptimizationRequest();
        or.setCheckKKTSolutionAccuracy(true);
        or.setCheckProgressConditions(true);
        or.setF0(objectiveFunction);
        or.setInitialPoint(new double[] { 0.6, 0.2, 0.2 });
        // equalities
        or.setA(new double[][] { { 1, 1, 1 } });
        or.setB(new double[] { 1 });
        // tolerances
        or.setTolerance(1.E-5);

        // optimization
        final BarrierFunction bf = new LogarithmicBarrier(inequalities, 3);
        final OptimizationRequestHandler orh = new OptimizationRequestHandler();
        orh.successor = new BarrierMethod(bf);
        orh.setOptimizationRequest(or);
        final int returnCode = orh.optimize();

        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }

        final OptimizationResponse response = orh.getOptimizationResponse();
        final double[] expectedSol = { 0.04632311555988555, 0.5086308460954377, 0.44504603834467693 };
        final double expectedValue = objectiveFunction.value(expectedSol);
        final double[] sol = response.getSolution();
        final double value = objectiveFunction.value(sol);
        assertEquals(expectedValue, value, or.getTolerance());
    }
    
    /**
     * Test getFi
     * It throws an exception, this gets should not be used, instead use the barrier function
     */
    public void testGetFi() {
        final ConvexMultivariateRealFunction[] inequalities = new ConvexMultivariateRealFunction[3];
        final BarrierFunction bf = new LogarithmicBarrier(inequalities, 3);
        final BarrierMethod opt = new BarrierMethod(bf);
        try{
            opt.getFi(null);
        }catch (UnsupportedOperationException e){
            assertTrue(true);//ok, use BarrierFunction 
            return;
        }
        fail();
    }
    
    /**
     * Test getGradFi
     * It throws an exception, this gets should not be used, instead use the barrier function
     */
    public void testGradFi() {
        final ConvexMultivariateRealFunction[] inequalities = new ConvexMultivariateRealFunction[3];
        final BarrierFunction bf = new LogarithmicBarrier(inequalities, 3);
        final BarrierMethod opt = new BarrierMethod(bf);
        try{
            opt.getGradFi(null);
        }catch (UnsupportedOperationException e){
            assertTrue(true);//ok, use BarrierFunction 
            return;
        }
        fail();
    }
    
    /**
     * Test getHessFi
     * It throws an exception, this gets should not be used, instead use the barrier function
     */
    public void testGetHessFi() {
        final ConvexMultivariateRealFunction[] inequalities = new ConvexMultivariateRealFunction[3];
        final BarrierFunction bf = new LogarithmicBarrier(inequalities, 3);
        final BarrierMethod opt = new BarrierMethod(bf);
        try{
            opt.getHessFi(null);
        }catch (UnsupportedOperationException e){
            assertTrue(true);//ok, use BarrierFunction 
            return;
        }
        fail();
    }
    
    
    /**
     * Test method findFeasibleInitialPoint (BasicPhaseIBM) when it fails to find a feasible initial point
     * @throws PatriusException 
     */
    public void testFindInitialPointError() throws PatriusException {
        final RealMatrix pMatrix = new 
                BlockRealMatrix(new double[][] {{1.68, 0.34, 0.38 },{ 0.34, 3.09, -1.59 },{ 0.38, -1.59, 1.54 }});
        final RealVector qVector = new ArrayRealVector(new double[] { 0.018, 0.025, 0.01 });

        // Objective function
        final double theta = 0.01522;
        final RealMatrix p = pMatrix.scalarMultiply(theta);
        final RealVector q = qVector.mapMultiply(-1);
        final PDQuadraticMultivariateRealFunction objectiveFunction = new PDQuadraticMultivariateRealFunction(
                p.getData(false), q.toArray(), 0);

        final ConvexMultivariateRealFunction[] inequalities = new ConvexMultivariateRealFunction[3];
        inequalities[0] = new LinearMultivariateRealFunction(new double[] { -1, 0, 0 }, 0);
        inequalities[1] = new LinearMultivariateRealFunction(new double[] { 0, -1, 0 }, 0);
        inequalities[2] = new LinearMultivariateRealFunction(new double[] { 0, 0, -1 }, 0);

        final OptimizationRequest or = new OptimizationRequest();
        or.setCheckKKTSolutionAccuracy(true);
        or.setCheckProgressConditions(true);
        or.setF0(objectiveFunction);
        // equalities (set with values that would make it fail to find an initial feasible point)
        or.setA(new double[][] { { 1, 0, 1 } });
        or.setB(new double[] { -14 });

        // optimization
        final BarrierFunction bf = new LogarithmicBarrier(inequalities, 3);
        final BarrierMethod opt = new BarrierMethod(bf);
        opt.setOptimizationRequest(or);

        try{
            opt.optimize();
        }catch (PatriusException e){
            assertTrue(true); //ok, Failed to find an initial feasible point
            return;
        }
        fail();
    }
}
