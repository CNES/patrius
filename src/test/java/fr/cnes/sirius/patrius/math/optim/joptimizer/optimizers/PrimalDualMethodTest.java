/**
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
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

import java.util.Arrays;

import junit.framework.TestCase;
import fr.cnes.sirius.patrius.math.linear.Array2DRowRealMatrix;
import fr.cnes.sirius.patrius.math.linear.ArrayRealVector;
import fr.cnes.sirius.patrius.math.linear.BlockRealMatrix;
import fr.cnes.sirius.patrius.math.linear.MatrixUtils;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealVector;
import fr.cnes.sirius.patrius.math.optim.joptimizer.functions.ConvexMultivariateRealFunction;
import fr.cnes.sirius.patrius.math.optim.joptimizer.functions.FunctionsUtils;
import fr.cnes.sirius.patrius.math.optim.joptimizer.functions.LinearMultivariateRealFunction;
import fr.cnes.sirius.patrius.math.optim.joptimizer.functions.PDQuadraticMultivariateRealFunction;
import fr.cnes.sirius.patrius.math.optim.joptimizer.functions.PSDQuadraticMultivariateRealFunction;
import fr.cnes.sirius.patrius.math.optim.joptimizer.functions.StrictlyConvexMultivariateRealFunction;
import fr.cnes.sirius.patrius.math.optim.joptimizer.util.Utils;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * @author alberto trivellato (alberto.trivellato@gmail.com)
 */
public class PrimalDualMethodTest extends TestCase {

    /**
     * Quadratic objective with linear eq and ineq.
     * 
     * @throws PatriusException if an error occurs
     */
    public void testOptimize() throws PatriusException {
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
        or.setInitialPoint(new double[] { 0.25, 0.25, 0.5 });
        // inequalities
        or.setFi(inequalities);
        // equalities
        or.setA(new double[][] { { 1, 1, 1 } });
        or.setB(new double[] { 1 });
        // tolerances
        or.setTolerance(1.E-11);
        or.setToleranceFeas(1.E-8);
        or.setCheckProgressConditions(true);

        // optimization
        final PrimalDualMethod opt = new PrimalDualMethod();
        opt.setOptimizationRequest(or);
        final int returnCode = opt.optimize();
        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }
        final OptimizationResponse response = opt.getOptimizationResponse();
        final double[] sol = response.getSolution();
        assertEquals(0.04632311555988555, sol[0], 0.00000001);
        assertEquals(0.5086308460954377, sol[1], 0.00000001);
        assertEquals(0.44504603834467693, sol[2], 0.00000001);
    }

    /**
     * Quadratic objective with linear eq and ineq
     * without initial point.
     * 
     * @throws PatriusException if an error occurs
     */
    public void testOptimize2() throws PatriusException {
        final RealMatrix qq = new BlockRealMatrix(new double[][] { { 1.68, 0.34, 0.38 },
                { 0.34, 3.09, -1.59 }, { 0.38, -1.59, 1.54 } });
        final RealVector ll = new ArrayRealVector(new double[] { 0.018, 0.025, 0.01 });

        // Objective function (Risk-Aversion).
        final double theta = 0.01522;
        final RealMatrix p = qq.scalarMultiply(theta);
        final RealVector q = ll.mapMultiply(-1);
        final PDQuadraticMultivariateRealFunction objectiveFunction = new PDQuadraticMultivariateRealFunction(
                p.getData(false), q.toArray(), 0);

        final ConvexMultivariateRealFunction[] inequalities = new ConvexMultivariateRealFunction[3];
        inequalities[0] = new LinearMultivariateRealFunction(new double[] { -1, 0, 0 }, 0);
        inequalities[1] = new LinearMultivariateRealFunction(new double[] { 0, -1, 0 }, 0);
        inequalities[2] = new LinearMultivariateRealFunction(new double[] { 0, 0, -1 }, 0);

        final OptimizationRequest or = new OptimizationRequest();
        or.setCheckKKTSolutionAccuracy(true);
        or.setF0(objectiveFunction);
        // This infeasible point is already feasible, just to test
        or.setNotFeasibleInitialPoint(new double[] {0.3333333333,0.3333333333,0.3333333333});
        // inequalities
        or.setFi(inequalities);
        // equalities
        or.setA(new double[][] { { 1, 1, 1 } });
        or.setB(new double[] { 1 });
        // tolerances
        or.setTolerance(1.E-12);

        // optimization
        final PrimalDualMethod opt = new PrimalDualMethod();
        opt.setOptimizationRequest(or);
        final int returnCode = opt.optimize();

        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }

        final OptimizationResponse response = opt.getOptimizationResponse();
        final double[] sol = response.getSolution();
        assertEquals(0.04632311555988555, sol[0], 0.0000001);
        assertEquals(0.5086308460954377, sol[1], 0.0000001);
        assertEquals(0.44504603834467693, sol[2], 0.0000001);
    }

    /**
     * Quadratic objective with linear eq and quadratic ineq.
     * 
     * @throws PatriusException if an error occurs
     */
    public void testOptimize3() throws PatriusException {
        final RealMatrix qq = new BlockRealMatrix(new double[][] { { 1.68, 0.34, 0.38 },
                { 0.34, 3.09, -1.59 }, { 0.38, -1.59, 1.54 } });
        final RealVector ll = new ArrayRealVector(new double[] { 0.018, 0.025, 0.01 });

        // Objective function (Risk-Aversion).
        final double theta = 0.01522;
        final RealMatrix pMatrix = qq.scalarMultiply(theta);
        final RealVector qvector = ll.mapMultiply(-1);
        final PDQuadraticMultivariateRealFunction objectiveFunction = new PDQuadraticMultivariateRealFunction(
                pMatrix.getData(false), qvector.toArray(), 0);

        final ConvexMultivariateRealFunction[] inequalities = new ConvexMultivariateRealFunction[4];
        inequalities[0] = new LinearMultivariateRealFunction(new double[] { -1, 0, 0 }, 0);
        inequalities[1] = new LinearMultivariateRealFunction(new double[] { 0, -1, 0 }, 0);
        inequalities[2] = new LinearMultivariateRealFunction(new double[] { 0, 0, -1 }, 0);
        inequalities[3] = FunctionsUtils.createCircle(3, 5);// not linear

        final OptimizationRequest or = new OptimizationRequest();
        or.setCheckKKTSolutionAccuracy(true);
        or.setF0(objectiveFunction);
        or.setInitialPoint(new double[] { 0.2, 0.2, 0.6 });
        or.setInitialLagrangian(new double[] { 0.5, 0.5, 0.5, 0.5 });
        // Inquality constraints
        or.setFi(inequalities);
        // Equality constraints
        or.setA(new double[][] { { 1, 1, 1 } });
        or.setB(new double[] { 1 });
        // tolerances
        or.setTolerance(1.E-10);

        // optimization
        final PrimalDualMethod opt = new PrimalDualMethod();
        opt.setOptimizationRequest(or);
        final int returnCode = opt.optimize();

        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }

        final OptimizationResponse response = opt.getOptimizationResponse();
        final double[] sol = response.getSolution();
        assertEquals(0.04632311555988555, sol[0], 0.0000001);
        assertEquals(0.5086308460954377, sol[1], 0.0000001);
        assertEquals(0.44504603834467693, sol[2], 0.0000001);
    }

    /**
     * Linear objective with quadratic ineq.
     * 
     * @throws PatriusException if an error occurs
     */
    public void testOptimize4() throws PatriusException {

        // Objective function (linear)
        final LinearMultivariateRealFunction objectiveFunction = new LinearMultivariateRealFunction(
                new double[] { 1, 1 }, 0);

        // Inquality constraints
        final ConvexMultivariateRealFunction[] inequalities = new ConvexMultivariateRealFunction[1];
        inequalities[0] = FunctionsUtils.createCircle(2, 1);

        final OptimizationRequest or = new OptimizationRequest();
        or.setCheckKKTSolutionAccuracy(true);
        or.setToleranceKKT(1.E-4);
        or.setF0(objectiveFunction);
        or.setInitialPoint(new double[] { 0, 0 });
        or.setFi(inequalities);

        // optimization
        final PrimalDualMethod opt = new PrimalDualMethod();
        opt.setOptimizationRequest(or);
        final int returnCode = opt.optimize();

        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }

        final OptimizationResponse response = opt.getOptimizationResponse();
        final double[] sol = response.getSolution();
        final double value = objectiveFunction.value(sol);
        assertEquals(-Math.sqrt(2), value, 0.00000001);
        assertEquals(-Math.sqrt(2) / 2, sol[0], 0.00000001);
        assertEquals(-Math.sqrt(2) / 2, sol[1], 0.00000001);
    }

    /**
     * Linear objective with linear eq and ineq.
     * 
     * @throws PatriusException if an error occurs
     */
    public void testOptimize5() throws PatriusException {
        // START SNIPPET: PrimalDualMethod-1

        // Objective function (linear)
        final LinearMultivariateRealFunction objectiveFunction = new LinearMultivariateRealFunction(
                new double[] { 2, 1 }, 0);

        // Inquality constraints
        final ConvexMultivariateRealFunction[] inequalities = new ConvexMultivariateRealFunction[2];
        inequalities[0] = new LinearMultivariateRealFunction(new double[] { -1, 0 }, 0);
        inequalities[1] = new LinearMultivariateRealFunction(new double[] { 0, -1 }, 0);

        final OptimizationRequest or = new OptimizationRequest();
        or.setF0(objectiveFunction);
        or.setInitialPoint(new double[] { 0.9, 0.1 });
        or.setFi(inequalities);
        // Equality constraints
        or.setA(new double[][] { { 1, 1 } });
        or.setB(new double[] { 1 });
        or.setTolerance(1.E-9);

        // optimization
        final PrimalDualMethod opt = new PrimalDualMethod();
        opt.setOptimizationRequest(or);
        final int returnCode = opt.optimize();

        // END SNIPPET: PrimalDualMethod-1

        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }

        final OptimizationResponse response = opt.getOptimizationResponse();
        final double[] sol = response.getSolution();
        final double value = objectiveFunction.value(sol);
        assertEquals(0., sol[0], 0.00000001);
        assertEquals(1., sol[1], 0.00000001);
        assertEquals(1., value, 0.00000001);
    }

    /**
     * Linear objective with quadratic ineq
     * and without initial point.
     * NOTE: changing c to 1 or 10 we get a KKT solution failed error:
     * this is because rDual (that is proportional to the gradient of F0, that
     * is proportional to c) does not decrease well during the iterations.
     * 
     * @throws PatriusException if an error occurs
     */
    public void testOptimize6() throws PatriusException {
        // START SNIPPET: PrimalDualMethod-2

        // Objective function (linear)
        final double c = 0.1;
        final LinearMultivariateRealFunction objectiveFunction = new LinearMultivariateRealFunction(
                new double[] { c, c }, 0);

        // Inequality constraints
        final ConvexMultivariateRealFunction[] inequalities = new ConvexMultivariateRealFunction[1];
        inequalities[0] = FunctionsUtils.createCircle(2, 1);

        final OptimizationRequest or = new OptimizationRequest();
        or.setF0(objectiveFunction);
        or.setInitialLagrangian(new double[] { 10 });
        or.setFi(inequalities);
        or.setInteriorPointMethod(JOptimizer.PRIMAL_DUAL_METHOD);// this is also the default
        or.setToleranceFeas(5.E-6);
        // or.setCheckKKTSolutionAccuracy(true);
        // or.setCheckProgressConditions(true);

        // optimization
        final JOptimizer opt = new JOptimizer();
        opt.setOptimizationRequest(or);
        final int returnCode = opt.optimize();

        // END SNIPPET: PrimalDualMethod-2

        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }

        final OptimizationResponse response = opt.getOptimizationResponse();
        final double[] sol = response.getSolution();
        final double value = objectiveFunction.value(sol);
        assertEquals(-Math.sqrt(2) * c, value, or.getTolerance());
        assertEquals(-Math.sqrt(2) / 2, sol[0], 0.00001);
        assertEquals(-Math.sqrt(2) / 2, sol[1], 0.00001);
    }

    /**
     * Exponential objective with quadratic ineq.
     * f0 = exp[z^2], z=(x-1, y-2)
     * f1 = x^2+y^2<=3^2
     * 
     * @throws PatriusException if an error occurs
     */
    public void testOptimize7() throws PatriusException {
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
                final RealMatrix id = MatrixUtils.createRealIdentityMatrix(2);
                final RealMatrix ret = z.outerProduct(z).add(id).scalarMultiply(2 * d);
                return ret.getData();
            }

            @Override
            public int getDim() {
                return 2;
            }
        };

        // Inquality constraints
        final ConvexMultivariateRealFunction[] inequalities = new ConvexMultivariateRealFunction[1];
        inequalities[0] = FunctionsUtils.createCircle(2, 3);

        final OptimizationRequest or = new OptimizationRequest();
        or.setF0(objectiveFunction);
        or.setInitialPoint(new double[] { 0.2, 0.2 });
        or.setFi(inequalities);

        // optimization
        final PrimalDualMethod opt = new PrimalDualMethod();
        opt.setOptimizationRequest(or);
        final int returnCode = opt.optimize();

        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }

        final OptimizationResponse response = opt.getOptimizationResponse();
        final double[] sol = response.getSolution();
        assertEquals(1., sol[0], or.getTolerance());
        assertEquals(2., sol[1], or.getTolerance());
    }

    /**
     * Min(s) s.t.
     * x^2-y-s<0
     * x+y=4
     * 
     * @throws PatriusException if an error occurs
     */
    public void testOptimize8() throws PatriusException {

        // Objective function (linear)
        final LinearMultivariateRealFunction objectiveFunction = new LinearMultivariateRealFunction(
                new double[] { 0, 0, 1 }, 0);

        // Equalities:
        final double[][] equalityAMatrix = new double[][] { { 1.0, 1.0, 0 } };
        final double[] equalityBVector = new double[] { 4.0 };

        // inequalities
        final ConvexMultivariateRealFunction[] inequalities = new ConvexMultivariateRealFunction[1];
        final double[][] pMatrix = new double[][] { { 2.0, 0, 0 }, { 0, 0, 0 }, { 0, 0, 0 } };
        final double[] qVector = new double[] { 0, -1, -1 };
        inequalities[0] = new PSDQuadraticMultivariateRealFunction(pMatrix, qVector, 0);

        final OptimizationRequest or = new OptimizationRequest();
        or.setF0(objectiveFunction);
        or.setInitialPoint(new double[] { 2, 2, 2000 });
        or.setA(equalityAMatrix);
        or.setB(equalityBVector);
        or.setFi(inequalities);
        // or.setTolerance(1.E-7);//ok
        // or.setToleranceFeas(1.E-7);//ok
        or.setToleranceFeas(1E-6);// ko
        or.setTolerance(2E-6);// ko
        or.setInteriorPointMethod(JOptimizer.PRIMAL_DUAL_METHOD);// this is also the default

        // optimization
        final JOptimizer opt = new JOptimizer();
        opt.setOptimizationRequest(or);
        final int returnCode = opt.optimize();

        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }

        final OptimizationResponse response = opt.getOptimizationResponse();
        final double[] sol = response.getSolution();
        assertEquals(-0.5, sol[0], 0.01);
        assertEquals(4.5, sol[1], 0.01);
        assertEquals(-4.25, sol[2], 0.01);
    }

    /**
     * Quadratic objective with linear eq and ineq.
     * 
     * @throws PatriusException if an error occurs
     */
    public void testOptimize10D() throws PatriusException {

        final int dim = 10;

        // Objective function
        final RealMatrix p = Utils.randomValuesPositiveMatrix(dim, dim, -0.5, 0.5, 7654321L);
        final RealVector q = Utils.randomValuesMatrix(1, dim, -0.5, 0.5, 7654321L).getRowVector(0);

        final PDQuadraticMultivariateRealFunction objectiveFunction = new PDQuadraticMultivariateRealFunction(
                p.getData(false), q.toArray(), 0);

        // equalities
        final double[][] aeMatrix = new double[1][dim];
        Arrays.fill(aeMatrix[0], 1.);
        final double[] beVector = new double[] { 1 };

        // inequalities
        final double[][] aiMatrix = new double[dim][dim];
        for (int i = 0; i < dim; i++) {
            aiMatrix[i][i] = -1;
        }
        final ConvexMultivariateRealFunction[] inequalities = new ConvexMultivariateRealFunction[dim];
        for (int i = 0; i < dim; i++) {
            inequalities[i] = new LinearMultivariateRealFunction(aiMatrix[i], 0);
        }

        final OptimizationRequest or = new OptimizationRequest();
        or.setF0(objectiveFunction);
        final double[] ip = new double[dim];
        Arrays.fill(ip, 1. / dim);
        or.setInitialPoint(ip);
        or.setA(aeMatrix);
        or.setB(beVector);
        or.setFi(inequalities);

        // optimization
        final PrimalDualMethod opt = new PrimalDualMethod();
        opt.setOptimizationRequest(or);
        final int returnCode = opt.optimize();

        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }
    }
    
    /**
     * Test findFeasibleInitialPoint from of BasicPhaseIPDM 
     * with a really small tolerance that is not possible to achieve-> it throws an exception
     */
    public void testFindFIPSmallTolerance() {
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
        or.setFi(inequalities);
        or.setA(new double[][] { { 1, 1, 1 } });
        or.setB(new double[] { 1 });
        or.setTolerance(1.E-11);
        or.setToleranceFeas(1.E-50);  // Small tolerance!!

        // optimization
        final PrimalDualMethod opt = new PrimalDualMethod();
        opt.setOptimizationRequest(or);
        try{
            opt.optimize();
        }catch (PatriusException e) {
            assertTrue(true);//ok, tolerance too small, it cannot be achieved
            return;
        }
        fail();
    }
    
    /**
     * Test optimize with max iterations set to 0
     * -> It throws an exception
     */
    public void testMaxIterations() throws PatriusException {
        // START SNIPPET: PrimalDualMethod-1

        // Objective function (linear)
        final LinearMultivariateRealFunction objectiveFunction = new LinearMultivariateRealFunction(
                new double[] { 2, 1 }, 0);

        // Inquality constraints
        final ConvexMultivariateRealFunction[] inequalities = new ConvexMultivariateRealFunction[2];
        inequalities[0] = new LinearMultivariateRealFunction(new double[] { -1, 0 }, 0);
        inequalities[1] = new LinearMultivariateRealFunction(new double[] { 0, -1 }, 0);

        final OptimizationRequest or = new OptimizationRequest();
        or.setF0(objectiveFunction);
        or.setInitialPoint(new double[] { 0.9, 0.1 });
        or.setFi(inequalities);
        // Equality constraints
        or.setA(new double[][] { { 1, 1 } });
        or.setB(new double[] { 1 });
        or.setTolerance(1.E-9);
        or.setMaxIteration(0);

        // optimization
        final PrimalDualMethod opt = new PrimalDualMethod();
        opt.setOptimizationRequest(or);
        if (opt.optimize() == OptimizationResponse.FAILED) {
            assertTrue(true);//ok, max iterations reached
            return;
        }
        fail();
    }
    
    /**
     * Test optimize setting as initial point a non feasible point -> it throws an exception
     * 
     */
    public void testInfeasiblePoint() {
        final LinearMultivariateRealFunction objectiveFunction = new LinearMultivariateRealFunction(
                new double[] { 2, 1 }, 0);

        // Inquality constraints
        final ConvexMultivariateRealFunction[] inequalities = new ConvexMultivariateRealFunction[2];
        inequalities[0] = new LinearMultivariateRealFunction(new double[] { -1, 0 }, 0);
        inequalities[1] = new LinearMultivariateRealFunction(new double[] { 0, -1 }, 0);

        final OptimizationRequest or = new OptimizationRequest();
        or.setF0(objectiveFunction);
        or.setInitialPoint(new double[] { -0.9, -0.1 });
        or.setFi(inequalities);
        
        final PrimalDualMethod opt = new PrimalDualMethod();
        opt.setOptimizationRequest(or);
        try{
            opt.optimize();
        }catch (PatriusException e) {
            assertTrue(true);//ok, initial point not feasible
            return;
        }
        fail();

    }
    
    /**
     * Test optimize setting as initial point a non feasible point -> it throws an exception
     * 
     */
    public void testInfeasiblePoint2() {
        final LinearMultivariateRealFunction objectiveFunction = new LinearMultivariateRealFunction(
                new double[] { 2, 1 }, 0);

        // Inquality constraints
        final ConvexMultivariateRealFunction[] inequalities = new ConvexMultivariateRealFunction[2];
        inequalities[0] = new LinearMultivariateRealFunction(new double[] { -1, 0 }, 0);
        inequalities[1] = new LinearMultivariateRealFunction(new double[] { 0, -1 }, 0);

        final OptimizationRequest or = new OptimizationRequest();
        or.setF0(objectiveFunction);
        or.setInitialPoint(new double[] { 0.9, 0.1 });
        or.setA(new double[][] { { 10, 1 } });
        or.setB(new double[] { 0.02 });
        or.setTolerance(1.E-9);
        or.setFi(inequalities);
        
        final PrimalDualMethod opt = new PrimalDualMethod();
        opt.setOptimizationRequest(or);
        try{
            opt.optimize();
        }catch (PatriusException e) {
            assertTrue(true);//ok, initial point not feasible
            return;
        }
        fail();

    }
    
    /**
     * Test with negative Lagrangian point -> it catch an exception
     * @throws PatriusException 
     */
    public void testNegativeLagrangianPoint() throws PatriusException {
        final RealMatrix qq = new BlockRealMatrix(new double[][] { { 1.68, 0.34, 0.38 },
                { 0.34, 3.09, -1.59 }, { 0.38, -1.59, 1.54 } });
        final RealVector ll = new ArrayRealVector(new double[] { 0.018, 0.025, 0.01 });

        // Objective function (Risk-Aversion).
        final double theta = 0.01522;
        final RealMatrix pMatrix = qq.scalarMultiply(theta);
        final RealVector qvector = ll.mapMultiply(-1);
        final PDQuadraticMultivariateRealFunction objectiveFunction = new PDQuadraticMultivariateRealFunction(
                pMatrix.getData(false), qvector.toArray(), 0);

        final ConvexMultivariateRealFunction[] inequalities = new ConvexMultivariateRealFunction[4];
        inequalities[0] = new LinearMultivariateRealFunction(new double[] { -1, 0, 0 }, 0);
        inequalities[1] = new LinearMultivariateRealFunction(new double[] { 0, -1, 0 }, 0);
        inequalities[2] = new LinearMultivariateRealFunction(new double[] { 0, 0, -1 }, 0);
        inequalities[3] = FunctionsUtils.createCircle(3, 5);// not linear

        final OptimizationRequest or = new OptimizationRequest();
        or.setCheckKKTSolutionAccuracy(true);
        or.setF0(objectiveFunction);
        or.setInitialPoint(new double[] { 0.2, 0.2, 0.6 });
        or.setInitialLagrangian(new double[] { -0.5, 0.5, 0.5, 0.5 });
        
        // Inequality constraints
        or.setFi(inequalities);
        // Equality constraints
        or.setA(new double[][] { { 1, 1, 1 } });
        or.setB(new double[] { 1 });
        // tolerances
        or.setTolerance(1.E-10);

        // optimization
        final PrimalDualMethod opt = new PrimalDualMethod();
        opt.setOptimizationRequest(or);
        try{
            opt.optimize();
        }catch (IllegalArgumentException e) {
            assertTrue(true);//ok, Lagrangian point < 0
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
        final ConvexMultivariateRealFunction[] inequalities = new ConvexMultivariateRealFunction[4];
        inequalities[0] = new LinearMultivariateRealFunction(new double[] { -1, 0, 0 }, 0);
        inequalities[1] = new LinearMultivariateRealFunction(new double[] { 0, -1, 0 }, 0);
        inequalities[2] = new LinearMultivariateRealFunction(new double[] { 0, 0, -1 }, 0);
        inequalities[3] = FunctionsUtils.createCircle(3, 5);// not linear

        final OptimizationRequest or = new OptimizationRequest();
        or.setF0(objectiveFunction);
        or.setFi(inequalities);
        or.setInitialPoint(new double[] { 0.1, 0.5, 0.46 });
        or.setTolerance(1.e-8);
        or.setCheckProgressConditions(true);

        // optimization
        final PrimalDualMethod opt = new PrimalDualMethod();
        opt.setOptimizationRequest(or);
        if (opt.optimize() == OptimizationResponse.FAILED) {
            assertTrue(true);//ok, no progress achieved
            return;
        }
        fail();
    }
}
