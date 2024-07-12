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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import junit.framework.TestCase;
import fr.cnes.sirius.patrius.math.linear.Array2DRowRealMatrix;
import fr.cnes.sirius.patrius.math.linear.ArrayRealVector;
import fr.cnes.sirius.patrius.math.linear.BlockRealMatrix;
import fr.cnes.sirius.patrius.math.linear.CholeskyDecomposition;
import fr.cnes.sirius.patrius.math.linear.MatrixUtils;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealVector;
import fr.cnes.sirius.patrius.math.optim.joptimizer.TestUtils;
import fr.cnes.sirius.patrius.math.optim.joptimizer.functions.ConvexMultivariateRealFunction;
import fr.cnes.sirius.patrius.math.optim.joptimizer.functions.FunctionsUtils;
import fr.cnes.sirius.patrius.math.optim.joptimizer.functions.LinearMultivariateRealFunction;
import fr.cnes.sirius.patrius.math.optim.joptimizer.functions.PDQuadraticMultivariateRealFunction;
import fr.cnes.sirius.patrius.math.optim.joptimizer.functions.PSDQuadraticMultivariateRealFunction;
import fr.cnes.sirius.patrius.math.optim.joptimizer.functions.StrictlyConvexMultivariateRealFunction;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * @author alberto trivellato (alberto.trivellato@gmail.com)
 */
public class JOptimizerTest extends TestCase {
    
    /** String qp/ */
    final String qpS = "qp" + File.separator;
    /** String lp/ */
    final String lpS = "lp" + File.separator;
    /** String .txt */
    final String txt = ".txt";
    /** String space */
    final String space = " ";

    /**
     * The simplest test.
     * @throws PatriusException if an error occurs
     */
    public void testSimplest() throws PatriusException {

        // Objective function
        final LinearMultivariateRealFunction objectiveFunction = new LinearMultivariateRealFunction(
                new double[] { 1. }, 0);

        // inequalities
        final ConvexMultivariateRealFunction[] inequalities = new ConvexMultivariateRealFunction[1];
        inequalities[0] = new LinearMultivariateRealFunction(new double[] { -1 }, 0.);

        final OptimizationRequest or = new OptimizationRequest();
        or.setF0(objectiveFunction);
        or.setFi(inequalities);
        or.setInitialPoint(new double[] { 1 });
        or.setToleranceFeas(1.E-8);
        or.setTolerance(1.E-9);

        // optimization
        final JOptimizer opt = new JOptimizer();
        opt.setOptimizationRequest(or);
        final int returnCode = opt.optimize();

        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }

        final OptimizationResponse response = opt.getOptimizationResponse();
        final double[] sol = response.getSolution();

        assertEquals(0., sol[0], 0.000000001);
    }

    /**
     * Quadratic objective, no constraints.
     * @throws PatriusException if an error occurs
     */
    public void testNewtownUnconstrained() throws PatriusException {
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

        // optimization
        final JOptimizer opt = new JOptimizer();
        opt.setOptimizationRequest(or);
        final int returnCode = opt.optimize();

        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }

        final OptimizationResponse response = opt.getOptimizationResponse();
        final double[] sol = response.getSolution();
        // we already know the analytic solution of the problem
        // sol = -invQ * C
        final CholeskyDecomposition cFact = new CholeskyDecomposition(p);
        final RealVector benchSol = cFact.getSolver().solve(q).mapMultiply(-1);

        assertEquals(benchSol.getEntry(0), sol[0], or.getTolerance());
        assertEquals(benchSol.getEntry(1), sol[1], or.getTolerance());
        assertEquals(benchSol.getEntry(2), sol[2], or.getTolerance());
    }

    /**
     * Quadratic objective with linear equality constraints and feasible starting point.
     * 
     * @throws PatriusException if an error occurs
     */
    public void testNewtonLEConstrainedFSP() throws PatriusException {
        final RealMatrix pMatrix = new BlockRealMatrix(new double[][] { { 1.68, 0.34, 0.38 },
                { 0.34, 3.09, -1.59 }, { 0.38, -1.59, 1.54 } });
        final RealVector qVector = new ArrayRealVector(new double[] { 0.018, 0.025, 0.01 });

        // Objective function.
        final double theta = 0.01522;
        final RealMatrix p = pMatrix.scalarMultiply(theta);
        final RealVector q = qVector.mapMultiply(-1);
        final PDQuadraticMultivariateRealFunction objectiveFunction = new PDQuadraticMultivariateRealFunction(
                p.getData(false), q.toArray(), 0);

        // equalities
        final double[][] a = new double[][] { { 1, 1, 1 } };
        final double[] b = new double[] { 1 };

        final OptimizationRequest or = new OptimizationRequest();
        or.setF0(objectiveFunction);
        or.setInitialPoint(new double[] { 0.04, 0.50, 0.46 });
        or.setA(a);
        or.setB(b);

        // optimization
        final JOptimizer opt = new JOptimizer();
        opt.setOptimizationRequest(or);
        final int returnCode = opt.optimize();

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
     * Quadratic objective with linear equality constraints and infeasible starting point.
     * 
     * @throws PatriusException if an error occurs
     */
    public void testNewtonLEConstrainedISP() throws PatriusException {
        final RealMatrix pMatrix = new BlockRealMatrix(new double[][] { { 1.68, 0.34, 0.38 },
                { 0.34, 3.09, -1.59 }, { 0.38, -1.59, 1.54 } });
        final RealVector qVector = new ArrayRealVector(new double[] { 0.018, 0.025, 0.01 });

        // Objective function (Risk-Aversion).
        final double theta = 0.01522;
        final RealMatrix p = pMatrix.scalarMultiply(theta);
        final RealVector q = qVector.mapMultiply(-1);
        final PDQuadraticMultivariateRealFunction objectiveFunction = new PDQuadraticMultivariateRealFunction(
                p.getData(false), q.toArray(), 0);

        final OptimizationRequest or = new OptimizationRequest();
        or.setF0(objectiveFunction);
        or.setInitialPoint(new double[] { 1, 1, 1 });
        or.setA(new double[][] { { 1, 1, 1 } });
        or.setB(new double[] { 1 });

        // optimization
        final JOptimizer opt = new JOptimizer();
        opt.setOptimizationRequest(or);
        final int returnCode = opt.optimize();

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
     * Quadratic objective with linear eq and ineq.
     * 
     * @throws PatriusException if an error occurs
     */
    public void testPrimalDualMethod() throws PatriusException {
        final RealMatrix pMatrix = new BlockRealMatrix(new double[][] { { 1.68, 0.34, 0.38 },
                { 0.34, 3.09, -1.59 }, { 0.38, -1.59, 1.54 } });
        final RealVector qVector = new ArrayRealVector(new double[] { 0.018, 0.025, 0.01 });

        // Objective function.
        final double theta = 0.01522;
        final RealMatrix p = pMatrix.scalarMultiply(theta);
        final RealVector q = qVector.mapMultiply(-1);
        final PDQuadraticMultivariateRealFunction objectiveFunction = new PDQuadraticMultivariateRealFunction(
                p.getData(false), q.toArray(), 0);

        // equalities
        final double[][] a = new double[][] { { 1, 1, 1 } };
        final double[] b = new double[] { 1 };

        // inequalities
        final ConvexMultivariateRealFunction[] inequalities = new ConvexMultivariateRealFunction[3];
        inequalities[0] = new LinearMultivariateRealFunction(new double[] { -1, 0, 0 }, 0);
        inequalities[1] = new LinearMultivariateRealFunction(new double[] { 0, -1, 0 }, 0);
        inequalities[2] = new LinearMultivariateRealFunction(new double[] { 0, 0, -1 }, 0);

        final OptimizationRequest or = new OptimizationRequest();
        or.setF0(objectiveFunction);
        or.setInitialPoint(new double[] { 0.2, 0.2, 0.6 });
        or.setFi(inequalities);
        or.setA(a);
        or.setB(b);
        or.setToleranceFeas(1.E-12);
        or.setTolerance(1.E-12);

        // optimization
        final JOptimizer opt = new JOptimizer();
        opt.setOptimizationRequest(or);
        final int returnCode = opt.optimize();

        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }

        final OptimizationResponse response = opt.getOptimizationResponse();
        final double[] sol = response.getSolution();
        assertEquals(0.04632311555988555, sol[0], 0.000000001);
        assertEquals(0.5086308460954377, sol[1], 0.000000001);
        assertEquals(0.44504603834467693, sol[2], 0.000000001);
    }

    /**
     * The same as testPrimalDualMethod, but with barrier-method.
     * Quadratic objective with linear eq and ineq.
     * 
     * @throws PatriusException if an error occurs
     */
    public void testBarrierMethod() throws PatriusException {
        final RealMatrix pMatrix = new BlockRealMatrix(new double[][] { { 1.68, 0.34, 0.38 },
                { 0.34, 3.09, -1.59 }, { 0.38, -1.59, 1.54 } });
        final RealVector qVector = new ArrayRealVector(new double[] { 0.018, 0.025, 0.01 });

        // Objective function (Risk-Aversion).
        final double theta = 0.01522;
        final RealMatrix p = pMatrix.scalarMultiply(theta);
        final RealVector q = qVector.mapMultiply(-1);
        final PDQuadraticMultivariateRealFunction objectiveFunction = new PDQuadraticMultivariateRealFunction(
                p.getData(false), q.toArray(), 0);

        // equalities
        final double[][] a = new double[][] { { 1, 1, 1 } };
        final double[] b = new double[] { 1 };

        // inequalities
        final ConvexMultivariateRealFunction[] inequalities = new ConvexMultivariateRealFunction[3];
        inequalities[0] = new LinearMultivariateRealFunction(new double[] { -1, 0, 0 }, 0);
        inequalities[1] = new LinearMultivariateRealFunction(new double[] { 0, -1, 0 }, 0);
        inequalities[2] = new LinearMultivariateRealFunction(new double[] { 0, 0, -1 }, 0);

        final OptimizationRequest or = new OptimizationRequest();
        or.setInteriorPointMethod(JOptimizer.BARRIER_METHOD);
        or.setF0(objectiveFunction);
        or.setInitialPoint(new double[] { 0.3, 0.3, 0.4 });
        or.setFi(inequalities);
        or.setA(a);
        or.setB(b);
        or.setTolerance(1.E-12);
        or.setToleranceInnerStep(1.E-5);

        // optimization
        final JOptimizer opt = new JOptimizer();
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
     * Linear programming in 2D.
     * 
     * @throws PatriusException if an error occurs
     */
    public void testLinearProgramming2D() throws PatriusException {

        // START SNIPPET: LinearProgramming-1

        // Objective function (plane)
        final LinearMultivariateRealFunction objectiveFunction = new LinearMultivariateRealFunction(
                new double[] { -1., -1. }, 4);

        // inequalities (polyhedral feasible set G.X<H )
        final ConvexMultivariateRealFunction[] inequalities = new ConvexMultivariateRealFunction[4];
        final double[][] g = new double[][] { { 4. / 3., -1 }, { -1. / 2., 1. }, { -2., -1. },
                { 1. / 3., 1. } };
        final double[] h = new double[] { 2., 1. / 2., 2., 1. / 2. };
        inequalities[0] = new LinearMultivariateRealFunction(g[0], -h[0]);
        inequalities[1] = new LinearMultivariateRealFunction(g[1], -h[1]);
        inequalities[2] = new LinearMultivariateRealFunction(g[2], -h[2]);
        inequalities[3] = new LinearMultivariateRealFunction(g[3], -h[3]);

        // optimization problem
        final OptimizationRequest or = new OptimizationRequest();
        or.setF0(objectiveFunction);
        or.setFi(inequalities);
        // or.setInitialPoint(new double[] {0.0, 0.0});//initial feasible point, not mandatory
        or.setToleranceFeas(1.E-9);
        or.setTolerance(1.E-9);

        // optimization
        final JOptimizer opt = new JOptimizer();
        opt.setOptimizationRequest(or);
        final int returnCode = opt.optimize();

        // END SNIPPET: LinearProgramming-1

        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }

        final OptimizationResponse response = opt.getOptimizationResponse();
        final double[] sol = response.getSolution();
        assertEquals(1.5, sol[0], 0.000000001);
        assertEquals(0.0, sol[1], 0.000000001);
    }

    /**
     * Linear programming in 2D in LP form.
     * This is the same problem as testLinearProgramming2D solved with LPPrimalDualMethod.
     * 
     * @throws PatriusException if an error occurs
     */
    public void testLPLinearProgramming2D() throws PatriusException {

        // START SNIPPET: LPLinearProgramming-1

        // Objective function
        final double[] c = new double[] { -1., -1. };

        // Inequalities constraints
        final double[][] g = new double[][] { { 4. / 3., -1 }, { -1. / 2., 1. }, { -2., -1. },
                { 1. / 3., 1. } };
        final double[] h = new double[] { 2., 1. / 2., 2., 1. / 2. };

        // Bounds on variables
        final double[] lb = new double[] { 0, 0 };
        final double[] ub = new double[] { 10, 10 };

        // optimization problem
        final LPOptimizationRequest or = new LPOptimizationRequest();
        or.setC(c);
        or.setG(g);
        or.setH(h);
        or.setLb(lb);
        or.setUb(ub);

        // optimization
        final LPPrimalDualMethod opt = new LPPrimalDualMethod();

        opt.setLPOptimizationRequest(or);
        final int returnCode = opt.optimize();

        // END SNIPPET: LPLinearProgramming-1

        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }

        final OptimizationResponse response = opt.getOptimizationResponse();
        final double[] sol = response.getSolution();
        assertEquals(1.5, sol[0], or.getTolerance());
        assertEquals(0.0, sol[1], or.getTolerance());
    }

    /**
     * Very simple linear.
     * 
     * @throws PatriusException if an error occurs
     */
    public void testSimpleLinear() throws PatriusException {
        // Objective function (plane)
        final LinearMultivariateRealFunction objectiveFunction = new LinearMultivariateRealFunction(
                new double[] { 1., 1. }, 0.);

        // equalities
        // RealMatrix AMatrix = new BlockRealMatrix(new double[][]{{1,-1}});
        // RealVector BVector = new ArrayRealVector(new double[]{0});

        // inequalities
        final ConvexMultivariateRealFunction[] inequalities = new ConvexMultivariateRealFunction[4];
        inequalities[0] = new LinearMultivariateRealFunction(new double[] { 1., 0. }, -3.);
        inequalities[1] = new LinearMultivariateRealFunction(new double[] { -1., 0. }, 0.);
        inequalities[2] = new LinearMultivariateRealFunction(new double[] { 0., 1. }, -3.);
        inequalities[3] = new LinearMultivariateRealFunction(new double[] { 0., -1. }, 0.);

        // optimization problem
        final OptimizationRequest or = new OptimizationRequest();
        or.setF0(objectiveFunction);
        or.setFi(inequalities);
        // or.setInitialPoint(new double[] {1., 1.});//initial feasible point, not mandatory
        or.setToleranceFeas(1.E-12);
        or.setTolerance(1.E-12);

        // optimization
        final JOptimizer opt = new JOptimizer();
        opt.setOptimizationRequest(or);
        final int returnCode = opt.optimize();

        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }

        final OptimizationResponse response = opt.getOptimizationResponse();
        final double[] sol = response.getSolution();
        assertEquals(0.0, sol[0], 0.000000000001);
        assertEquals(0.0, sol[1], 0.000000000001);
    }

    /**
     * Quadratic programming in 2D.
     * 
     * @throws PatriusException if an error occurs
     */
    public void testQuadraticProgramming2D() throws PatriusException {

        // START SNIPPET: QuadraticProgramming-1

        // Objective function
        final double[][] p = new double[][] { { 1., 0.4 }, { 0.4, 1. } };
        final PDQuadraticMultivariateRealFunction objectiveFunction = new PDQuadraticMultivariateRealFunction(
                p, null, 0);

        // equalities
        final double[][] a = new double[][] { { 1, 1 } };
        final double[] b = new double[] { 1 };

        // inequalities
        final ConvexMultivariateRealFunction[] inequalities = new ConvexMultivariateRealFunction[2];
        inequalities[0] = new LinearMultivariateRealFunction(new double[] { -1, 0 }, 0);
        inequalities[1] = new LinearMultivariateRealFunction(new double[] { 0, -1 }, 0);

        // optimization problem
        final OptimizationRequest or = new OptimizationRequest();
        or.setF0(objectiveFunction);
        or.setInitialPoint(new double[] { 0.1, 0.9 });
        // or.setFi(inequalities); //if you want x>0 and y>0
        or.setA(a);
        or.setB(b);
        or.setToleranceFeas(1.E-12);
        or.setTolerance(1.E-12);

        // optimization
        final JOptimizer opt = new JOptimizer();
        opt.setOptimizationRequest(or);
        final int returnCode = opt.optimize();

        // END SNIPPET: QuadraticProgramming-1

        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }

        final OptimizationResponse response = opt.getOptimizationResponse();
        final double[] sol = response.getSolution();
        assertEquals(0.5, sol[0], 0.0000000000001);
        assertEquals(0.5, sol[1], 0.0000000000001);
    }

    /**
     * Problem in the form
     * min(0.5 * x.P.x) s.t.
     * G.x < h
     * A.x = b
     * 
     * @throws PatriusException if an error occurs
     * @throws IOException if an error occurs while reading.
     */
    public void testPGhAb() throws PatriusException, IOException {

        final String problemId = "1";

        final double[][] p = TestUtils.loadDoubleMatrixFromFile(qpS + "P" + problemId
                + txt, space.charAt(0));
        final double[] c = TestUtils.loadDoubleArrayFromFile(qpS + "c" + problemId
                + txt);
        final double[][] g = TestUtils.loadDoubleMatrixFromFile(qpS + "G" + problemId
                + txt, space.charAt(0));
        final double[] h = TestUtils.loadDoubleArrayFromFile(qpS + "h" + problemId
                + txt);
        final double[][] a = TestUtils.loadDoubleMatrixFromFile(qpS + "A" + problemId
                + txt, space.charAt(0));
        final double[] b = TestUtils.loadDoubleArrayFromFile(qpS + "b" + problemId
                + txt);

        // Objective function
        final PDQuadraticMultivariateRealFunction objectiveFunction = new PDQuadraticMultivariateRealFunction(
                p, null, 0);

        // inequalities G.x < h
        final ConvexMultivariateRealFunction[] inequalities = new ConvexMultivariateRealFunction[g.length];
        for (int i = 0; i < g.length; i++) {
            inequalities[i] = new LinearMultivariateRealFunction(g[i], -h[i]);
        }

        // optimization problem
        final OptimizationRequest or = new OptimizationRequest();
        or.setF0(objectiveFunction);
        or.setFi(inequalities);
        final double[] nfip = new double[] { 1, 0, 0, 0, 0, 0 };
        Arrays.fill(nfip, 1. / c.length);
        // or.setNotFeasibleInitialPoint(new double[]{1,0,0,0,0,0});
        or.setA(a);
        or.setB(b);

        // optimization
        final JOptimizer opt = new JOptimizer();
        opt.setOptimizationRequest(or);
        final int returnCode = opt.optimize();

        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }

        final OptimizationResponse response = opt.getOptimizationResponse();
        final double[] sol = response.getSolution();

        final RealVector x = MatrixUtils.createRealVector(sol);
        final RealMatrix aMatrix = MatrixUtils.createRealMatrix(a);
        final RealVector bVector = MatrixUtils.createRealVector(b);
        final double rPriNorm = aMatrix.operate(x).subtract(bVector).getNorm();
        assertTrue(rPriNorm < or.getToleranceFeas());
        for (int i = 0; i < g.length; i++) {
            assertTrue(MatrixUtils.createRealVector(g[i]).dotProduct(x) < h[i]);
        }

    }

    /**
     * Quadratic programming in 2D.
     * Same as above but with an additional linear inequality constraint.
     * Submitted 28/02/2014 by Ashot Ordukhanyan.
     * 
     * @throws PatriusException if an error occurs
     */
    public void testQuadraticProgramming2Dmc() throws PatriusException {

        // Objective function
        final double[][] p = new double[][] { { 1., 0.4 }, { 0.4, 1. } };
        final PDQuadraticMultivariateRealFunction objectiveFunction = new PDQuadraticMultivariateRealFunction(
                p, null, 0);

        // equalities
        final double[][] a = new double[][] { { 1, 1 } };
        final double[] b = new double[] { 1 };

        // inequalities
        final ConvexMultivariateRealFunction[] inequalities = new ConvexMultivariateRealFunction[3];
        inequalities[0] = new LinearMultivariateRealFunction(new double[] { -1, 0 }, 0);
        inequalities[1] = new LinearMultivariateRealFunction(new double[] { 0, -1 }, 0);
        inequalities[2] = new LinearMultivariateRealFunction(new double[] { 1, 1 }, 20d);

        // optimization problem
        final OptimizationRequest or = new OptimizationRequest();
        or.setF0(objectiveFunction);
        or.setInitialPoint(new double[] { 0.1, 0.9 });
        // or.setFi(inequalities); //if you want x>0 and y>0
        or.setA(a);
        or.setB(b);
        or.setToleranceFeas(1.E-12);
        or.setTolerance(1.E-12);

        // optimization
        final JOptimizer opt = new JOptimizer();
        opt.setOptimizationRequest(or);
        final int returnCode = opt.optimize();

        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }

        final OptimizationResponse response = opt.getOptimizationResponse();
        final double[] sol = response.getSolution();
        assertEquals(0.5, sol[0], 0.0000000000001);
        assertEquals(0.5, sol[1], 0.0000000000001);
    }

    /**
     * Minimize -x-y s.t.
     * x^2 + y^2 <= 4 (1/2 [x y] [I] [x y]^T - 2 <= 0)
     * 
     * @throws PatriusException if an error occurs
     */
    public void testLinearCostQuadraticInequalityOptimizationProblem() throws PatriusException {

        final double[] minimizeF = new double[] { -1.0, -1.0 };
        final LinearMultivariateRealFunction objectiveFunction = new LinearMultivariateRealFunction(
                minimizeF, 0.0);

        // inequalities
        final ConvexMultivariateRealFunction[] inequalities = new ConvexMultivariateRealFunction[1];
        final double[][] pMatrix = new double[][] { { 1.0, 0.0 }, { 0.0, 1.0 } };
        final double[] qVector = new double[] { 0.0, 0.0 };
        final double r = -2;

        inequalities[0] = new PDQuadraticMultivariateRealFunction(pMatrix, qVector, r); // x^2+y^2
                                                                                        // <=4

        // optimization problem
        final OptimizationRequest or = new OptimizationRequest();
        or.setF0(objectiveFunction);
        or.setFi(inequalities);

        // optimization
        final JOptimizer opt = new JOptimizer();
        opt.setOptimizationRequest(or);
        final int returnCode = opt.optimize();

        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }

        final OptimizationResponse response = opt.getOptimizationResponse();
        final double[] sol = response.getSolution();
        assertEquals(Math.sqrt(2.0), sol[0], or.getTolerance());
        assertEquals(Math.sqrt(2.0), sol[1], or.getTolerance());
    }

    /**
     * min (-e.x)
     * s.t.
     * 1/2 x.P.x < v
     * x + y + z = 1
     * x > 0
     * y > 0
     * z > 0
     * 
     * @throws PatriusException if an error occurs
     */
    public void testLinearObjectiveQuadraticConstraints() throws PatriusException {

        // linear objective function
        final double[] e = { -0.018, -0.025, -0.01 };
        final LinearMultivariateRealFunction objectiveFunction = new LinearMultivariateRealFunction(e, 0);

        // quadratic constraint: 0.5 * x.P.x -v < 0
        final double[][] p = { { 1.68, 0.34, 0.38 }, { 0.34, 3.09, -1.59 }, { 0.38, -1.59, 1.54 } };
        final double v = 0.3;// quadratic constraint limit
        final PDQuadraticMultivariateRealFunction qc0 = new PDQuadraticMultivariateRealFunction(p, null,
                -v);

        // linear constraints
        // x>0
        final LinearMultivariateRealFunction lc0 = new LinearMultivariateRealFunction(new double[] { -1,
                0, 0 }, 0);
        // y>0
        final LinearMultivariateRealFunction lc1 = new LinearMultivariateRealFunction(new double[] { 0,
                -1, 0 }, 0);
        // z>0
        final LinearMultivariateRealFunction lc2 = new LinearMultivariateRealFunction(new double[] { 0,
                0, -1 }, 0);
        final ConvexMultivariateRealFunction[] constraints = new ConvexMultivariateRealFunction[] { qc0,
                lc0, lc1, lc2 };

        final OptimizationRequest or = new OptimizationRequest();
        or.setF0(objectiveFunction);
        or.setFi(constraints);
        or.setA(new double[][] { { 1, 1, 1 } });// equality constraint
        or.setB(new double[] { 1 });// equality constraint value
        or.setToleranceFeas(1.e-6);

        // optimization
        final JOptimizer opt = new JOptimizer();
        opt.setOptimizationRequest(or);
        final int returnCode = opt.optimize();

        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }

        final OptimizationResponse response = opt.getOptimizationResponse();
        final double[] sol = response.getSolution();

        // assertions
        assertEquals(1., sol[0] + sol[1] + sol[2], 1.e-6);
        assertTrue(sol[0] > 0);
        assertTrue(sol[1] > 0);
        assertTrue(sol[2] > 0);
        final RealVector xVector = MatrixUtils.createRealVector(sol);
        final RealMatrix pMatrix = MatrixUtils.createRealMatrix(p);
        final double xPx = xVector.dotProduct(pMatrix.operate(xVector));
        assertTrue(0.5 * xPx < v);
    }

    /**
     * Minimize x s.t.
     * x+y=4
     * y >= x^2.
     * 
     * @throws PatriusException if an error occurs
     */
    public void testLinearCostLinearEqualityQuadraticInequality() throws PatriusException {
        final double[] minimizeF = new double[] { 1.0, 0.0 };
        final LinearMultivariateRealFunction objectiveFunction = new LinearMultivariateRealFunction(
                minimizeF, 0.0);

        // Equalities:
        final double[][] equalityAMatrix = new double[][] { { 1.0, 1.0 } };
        final double[] equalityBVector = new double[] { 4.0 };

        // inequalities
        final ConvexMultivariateRealFunction[] inequalities = new ConvexMultivariateRealFunction[1];
        final double[][] pMatrix = new double[][] { { 2.0, 0.0 }, { 0.0, 0.0 } };
        final double[] qVector = new double[] { 0.0, -1.0 };
        final double r = 0.0;

        inequalities[0] = new PSDQuadraticMultivariateRealFunction(pMatrix, qVector, r); // x^2 - y
                                                                                         // < 0

        // optimization problem
        final OptimizationRequest or = new OptimizationRequest();
        or.setF0(objectiveFunction);
        or.setFi(inequalities);
        or.setA(equalityAMatrix);
        or.setB(equalityBVector);
        // or.setInitialPoint(new double[]{-0.5,4.5});
        // or.setNotFeasibleInitialPoint(new double[]{4,0});
        // or.setInteriorPointMethod(JOptimizer.BARRIER_METHOD);
        or.setCheckKKTSolutionAccuracy(true);

        // optimization
        final JOptimizer opt = new JOptimizer();
        opt.setOptimizationRequest(or);
        final int returnCode = opt.optimize();

        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }

        final OptimizationResponse response = opt.getOptimizationResponse();
        final double[] sol = response.getSolution();
        assertEquals(1.0 / 2.0 * (-1.0 - Math.sqrt(17.0)), sol[0], 1e-5);
        assertEquals(1.0 / 2.0 * (9.0 + Math.sqrt(17.0)), sol[1], 1e-5);
    }

    /**
     * Quadratically constrained quadratic programming in 2D.
     * 
     * @throws PatriusException if an error occurs
     */
    public void testSimpleQCQuadraticProgramming() throws PatriusException {

        // Objective function
        final double[][] p = new double[][] { { 2., 0. }, { 0., 2. } };
        final PDQuadraticMultivariateRealFunction objectiveFunction = new PDQuadraticMultivariateRealFunction(
                p, null, 0);

        // equalities
        final double[][] a = new double[][] { { 1, 1 } };
        final double[] b = new double[] { 1 };

        // inequalities
        final ConvexMultivariateRealFunction[] inequalities = new ConvexMultivariateRealFunction[1];
        inequalities[0] = FunctionsUtils.createCircle(2, 2, new double[] { 0., 0. });

        // optimization problem
        final OptimizationRequest or = new OptimizationRequest();
        or.setF0(objectiveFunction);
        or.setInitialPoint(new double[] { 0.2, 0.8 });
        or.setA(a);
        or.setB(b);
        or.setFi(inequalities);

        // optimization
        final JOptimizer opt = new JOptimizer();
        opt.setOptimizationRequest(or);
        final int returnCode = opt.optimize();

        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }

        final OptimizationResponse response = opt.getOptimizationResponse();
        final double[] sol = response.getSolution();
        assertEquals(0.5, sol[0], or.getTolerance());// NB: this is driven by the equality
                                                     // constraint
        assertEquals(0.5, sol[1], or.getTolerance());// NB: this is driven by the equality
                                                     // constraint
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
        final LinearMultivariateRealFunction objectiveFunction = new LinearMultivariateRealFunction(
                new double[] { 0, 1 }, 0);

        // inequalities x^2 < 1 + s
        final ConvexMultivariateRealFunction[] inequalities = new ConvexMultivariateRealFunction[1];
        final double[][] p1 = new double[][] { { 2., 0. }, { 0., 0. } };
        final double[] c1 = new double[] { 0, -1 };
        inequalities[0] = new PSDQuadraticMultivariateRealFunction(p1, c1, -1);

        // optimization problem
        final OptimizationRequest or = new OptimizationRequest();
        or.setF0(objectiveFunction);
        // or.setInitialPoint(new double[] { 2, 5});
        // or.setInitialPoint(new double[] {-0.1,-0.989});
        or.setInitialPoint(new double[] { 1.2, 2. });
        or.setFi(inequalities);
        or.setCheckKKTSolutionAccuracy(true);

        // optimization
        final JOptimizer opt = new JOptimizer();
        opt.setOptimizationRequest(or);
        final int returnCode = opt.optimize();

        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }

        final OptimizationResponse response = opt.getOptimizationResponse();
        final double[] sol = response.getSolution();
        assertEquals(0., sol[0], or.getTolerance());
        assertEquals(-1., sol[1], or.getTolerance());
    }

    /**
     * Linear objective, linear constrained.
     * It simulates the type of optimization occurring in feasibility searching
     * in a problem with constraints:
     * -x < 0
     * x -1 < 0
     * 
     * @throws PatriusException if an error occurs
     */
    public void testLinearProgramming() throws PatriusException {

        // Objective function (linear (x,s)->s)
        final double[] c0 = new double[] { 0, 1 };
        final LinearMultivariateRealFunction objectiveFunction = new LinearMultivariateRealFunction(c0, 0);

        // inequalities
        final ConvexMultivariateRealFunction[] inequalities = new ConvexMultivariateRealFunction[2];
        // -x -s < 0
        final double[] c1 = new double[] { -1, -1 };
        inequalities[0] = new LinearMultivariateRealFunction(c1, 0);
        // x -s -1 < 0
        final double[] c2 = new double[] { 1, -1 };
        inequalities[1] = new LinearMultivariateRealFunction(c2, -1);

        // optimization problem
        final OptimizationRequest or = new OptimizationRequest();
        or.setF0(objectiveFunction);
        or.setInitialPoint(new double[] { 1.4, 0.5 });
        // or.setInitialPoint(new double[] {-0.1,-0.989});
        // or.setInitialPoint(new double[] {1.2, 2.});
        or.setFi(inequalities);
        // or.setInitialLagrangian(new double[]{0.005263, 0.1});
        or.setMu(100d);

        // optimization
        final JOptimizer opt = new JOptimizer();
        opt.setOptimizationRequest(or);
        final int returnCode = opt.optimize();

        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }

        final OptimizationResponse response = opt.getOptimizationResponse();
        final double[] sol = response.getSolution();
        assertEquals(0.5, sol[0], or.getTolerance());
        assertEquals(-0.5, sol[1], or.getTolerance());
    }

    /**
     * Quadratic objective, no constraints.
     * 
     * @throws PatriusException if an error occurs
     */
    public void testQCQuadraticProgramming2() throws PatriusException {

        // Objective function
        final double[][] p = new double[][] { { 1., 0. }, { 0., 1. } };
        final PDQuadraticMultivariateRealFunction objectiveFunction = new PDQuadraticMultivariateRealFunction(
                p, null, 0);

        // optimization problem
        final OptimizationRequest or = new OptimizationRequest();
        or.setF0(objectiveFunction);
        or.setInitialPoint(new double[] { 2., 2. });
        or.setToleranceFeas(1.E-12);
        or.setTolerance(1.E-12);

        // optimization
        final JOptimizer opt = new JOptimizer();
        opt.setOptimizationRequest(or);
        final int returnCode = opt.optimize();

        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }

        final OptimizationResponse response = opt.getOptimizationResponse();
        final double[] sol = response.getSolution();
        assertEquals(0., sol[0], 0.000000000000001);
        assertEquals(0., sol[1], 0.000000000000001);
    }

    /**
     * Quadratically constrained quadratic programming in 2D.
     * 
     * @throws PatriusException if an error occurs
     */
    public void testQCQuadraticProgramming2D() throws PatriusException {

        // START SNIPPET: QCQuadraticProgramming-1

        // Objective function
        final double[][] p = new double[][] { { 1., 0.4 }, { 0.4, 1. } };
        final PDQuadraticMultivariateRealFunction objectiveFunction = new PDQuadraticMultivariateRealFunction(
                p, null, 0);

        // inequalities
        final ConvexMultivariateRealFunction[] inequalities = new ConvexMultivariateRealFunction[1];
        inequalities[0] = FunctionsUtils.createCircle(2, 1.75, new double[] { -2, -2 });

        // optimization problem
        final OptimizationRequest or = new OptimizationRequest();
        or.setF0(objectiveFunction);
        or.setInitialPoint(new double[] { -2., -2. });
        or.setFi(inequalities);
        or.setCheckKKTSolutionAccuracy(true);

        // optimization
        final JOptimizer opt = new JOptimizer();
        opt.setOptimizationRequest(or);
        final int returnCode = opt.optimize();

        // END SNIPPET: QCQuadraticProgramming-1

        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }

        final OptimizationResponse response = opt.getOptimizationResponse();
        final double[] sol = response.getSolution();
        assertEquals(-2 + 1.75 / Math.sqrt(2), sol[0], 0.0000001);// -0.762563132923542
        assertEquals(-2 + 1.75 / Math.sqrt(2), sol[1], 0.0000001);// -0.762563132923542
    }

    /**
     * The same as testQCQuadraticProgramming2D, but without initial point.
     * 
     * @throws PatriusException if an error occurs
     */
    public void testQCQuadraticProgramming2DNoInitialPoint() throws PatriusException {

        // Objective function
        final double[][] p = new double[][] { { 1., 0.4 }, { 0.4, 1. } };
        final PDQuadraticMultivariateRealFunction objectiveFunction = new PDQuadraticMultivariateRealFunction(
                p, null, 0);

        // inequalities
        final ConvexMultivariateRealFunction[] inequalities = new ConvexMultivariateRealFunction[1];
        inequalities[0] = FunctionsUtils.createCircle(2, 1.75, new double[] { -2, -2 });

        // optimization problem
        final OptimizationRequest or = new OptimizationRequest();
        or.setF0(objectiveFunction);
        or.setFi(inequalities);
        or.setCheckKKTSolutionAccuracy(true);

        // optimization
        final JOptimizer opt = new JOptimizer();
        opt.setOptimizationRequest(or);
        final int returnCode = opt.optimize();

        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }

        final OptimizationResponse response = opt.getOptimizationResponse();
        final double[] sol = response.getSolution();
        assertEquals(-2 + 1.75 / Math.sqrt(2), sol[0], or.getTolerance());// -0.762563132923542
        assertEquals(-2 + 1.75 / Math.sqrt(2), sol[1], or.getTolerance());// -0.762563132923542
    }

    /**
     * The basic PhaseI problem relative to testQCQuadraticProgramming2DNoInitialPoint.
     * min(s) s.t.
     * (x+2)^2 + (y+2)^2 -1.75 < s
     * This problem can't be solved without an initial point, because the relative PhaseI problem
     * min(r) s.t.
     * (x+2)^2 + (y+2)^2 -1.75 -s < r
     * is unbounded.
     * 
     * @throws PatriusException if an error occurs
     */
    public void testQCQuadraticProgramming2DNoInitialPointPhaseI() throws PatriusException {

        // Objective function
        final double[] f0 = new double[] { 0, 0, 1 };// s
        final LinearMultivariateRealFunction objectiveFunction = new LinearMultivariateRealFunction(f0, 0);

        // inequalities
        final ConvexMultivariateRealFunction[] inequalities = new ConvexMultivariateRealFunction[1];
        inequalities[0] = new ConvexMultivariateRealFunction() {

            @Override
            public double value(double[] xValue) {
                final double x = xValue[0];
                final double y = xValue[1];
                final double s = xValue[2];
                return Math.pow(x + 2, 2) + Math.pow(y + 2, 2) - 1.75 - s;
            }

            @Override
            public double[] gradient(double[] xValue) {
                final double x = xValue[0];
                final double y = xValue[1];
                return new double[] { 2 * (x + 2), 2 * (y + 2), -1 };
            }

            @Override
            public double[][] hessian(double[] xValue) {
                final double[][] ret = new double[3][3];
                ret[0][0] = 2;
                ret[1][1] = 2;
                return ret;
            }

            @Override
            public int getDim() {
                return 3;
            }
        };

        // optimization problem
        final OptimizationRequest or = new OptimizationRequest();
        or.setF0(objectiveFunction);
        // or.setInitialPoint(new double[] {0.5,0.5,94375.0});
        or.setInitialPoint(new double[] { -2, -2, 10 });
        or.setFi(inequalities);
        or.setCheckKKTSolutionAccuracy(true);

        // optimization
        final JOptimizer opt = new JOptimizer();
        opt.setOptimizationRequest(or);
        final int returnCode = opt.optimize();

        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }

        final OptimizationResponse response = opt.getOptimizationResponse();
        final double[] sol = response.getSolution();
        assertEquals(-2., sol[0], or.getTolerance());
        assertEquals(-2., sol[1], or.getTolerance());
        assertEquals(-1.75, sol[2], or.getTolerance());
    }

    /**
     * Exponential objective with quadratic ineq.
     * f0 = exp[z^2], z=(x-1, y-2)
     * f1 = x^2+y^2 < 3^2
     * 
     * @throws PatriusException if an error occurs
     */
    public void testOptimize7() throws PatriusException {
        // START SNIPPET: JOptimizer-1

        // you can implement the function definition using whatever linear algebra library you want,
        // you are not tied to Colt
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
        inequalities[0] = FunctionsUtils.createCircle(2, 3);// dim=2, radius=3, center=(0,0)

        final OptimizationRequest or = new OptimizationRequest();
        or.setF0(objectiveFunction);
        or.setInitialPoint(new double[] { 0.2, 0.2 });
        or.setFi(inequalities);

        // optimization
        final JOptimizer opt = new JOptimizer();
        opt.setOptimizationRequest(or);
        final int returnCode = opt.optimize();

        // END SNIPPET: JOptimizer-1

        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }

        final OptimizationResponse response = opt.getOptimizationResponse();
        final double[] sol = response.getSolution();
        assertEquals(1., sol[0], or.getTolerance());
        assertEquals(2., sol[1], or.getTolerance());
    }

    /**
     * Test QP in 3-dim
     * Min( 1/2 * xT.x) s.t.
     * x1 <= -10
     * This problem can't be solved without an initial point,
     * because the relative PhaseI problem is undetermined.
     * Submitted 01/06/2012 by Klaas De Craemer
     * 
     * @throws PatriusException if an error occurs
     */
    public void testQP() throws PatriusException {

        // Objective function
        final double[][] pMatrix = new double[][] { { 1, 0, 0 }, { 0, 1, 0 }, { 0, 0, 1 } };
        final PDQuadraticMultivariateRealFunction objectiveFunction = new PDQuadraticMultivariateRealFunction(
                pMatrix, null, 0);

        // inequalities
        final ConvexMultivariateRealFunction[] inequalities = new ConvexMultivariateRealFunction[1];
        inequalities[0] = new LinearMultivariateRealFunction(new double[] { 1, 0, 0 }, 10);// x1 <=
                                                                                           // -10

        // optimization problem
        final OptimizationRequest or = new OptimizationRequest();
        or.setF0(objectiveFunction);
        or.setInitialPoint(new double[] { -11, 1, 1 });
        or.setFi(inequalities);
        // or.setToleranceFeas(1.E-12);
        // or.setTolerance(1.E-12);

        // optimization
        final JOptimizer opt = new JOptimizer();
        opt.setOptimizationRequest(or);
        final int returnCode = opt.optimize();

        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }

        final OptimizationResponse response = opt.getOptimizationResponse();
        final double[] sol = response.getSolution();
        final double value = objectiveFunction.value(sol);
        assertEquals(-10., sol[0], 1.E-6);
        assertEquals(0., sol[1], 1.E-6);
        assertEquals(0., sol[2], 1.E-6);
        assertEquals(50., value, 1.E-6);
    }

    /**
     * Test QP.
     * Submitted 12/07/2012 by Katharina Schwaiger.
     * 
     * @throws PatriusException if an error occurs
     */
    public void testQPScala() throws PatriusException {

        final double[][] p = new double[2][2];
        p[0] = new double[] { 1.0, 0.4 };
        p[1] = new double[] { 0.4, 1.0 };

        final PDQuadraticMultivariateRealFunction objectiveFunction = new PDQuadraticMultivariateRealFunction(
                p, null, 0);

        final double[][] a = new double[1][2];
        a[0] = new double[] { 1.0, 1.0 };
        final double[] b = new double[] { 1.0 };

        final ConvexMultivariateRealFunction[] inequalities = new ConvexMultivariateRealFunction[4];
        inequalities[0] = new LinearMultivariateRealFunction(new double[] { -1, 0 }, -0.2);// -x1
                                                                                           // -0.2 <
                                                                                           // 0
        inequalities[1] = new LinearMultivariateRealFunction(new double[] { 0, -1 }, -0.2);// -x2
                                                                                           // -0.2 <
                                                                                           // 0

        inequalities[2] = new LinearMultivariateRealFunction(new double[] { -1, -1 }, 0.9);// -x1
                                                                                           // -x2
                                                                                           // +0.9 <
                                                                                           // 0
        inequalities[3] = new LinearMultivariateRealFunction(new double[] { 1, 1 }, -1.1);// x1 +x2
                                                                                          // -1.1 <
                                                                                          // 0

        final OptimizationRequest or = new OptimizationRequest();
        or.setF0(objectiveFunction);
        or.setA(a);
        or.setB(b);
        or.setFi(inequalities);
        or.setToleranceFeas(1.E-12);
        or.setTolerance(1.E-12);

        final JOptimizer opt = new JOptimizer();
        opt.setOptimizationRequest(or);
        final int returnCode = opt.optimize();
        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }

    }

    /**
     * Test QP.
     * Submitted 12/07/2012 by Katharina Schwaiger.
     * 
     * @throws PatriusException if an error occurs
     */
    public void testQPScala2() throws PatriusException {

        final double[][] p = new double[4][4];
        p[0] = new double[] { 0.08, -0.05, -0.05, -0.05 };
        p[1] = new double[] { -0.05, 0.16, -0.02, -0.02 };
        p[2] = new double[] { -0.05, -0.02, 0.35, 0.06 };
        p[3] = new double[] { -0.05, -0.02, 0.06, 0.35 };

        final PDQuadraticMultivariateRealFunction objectiveFunction = new PDQuadraticMultivariateRealFunction(
                p, null, 0);

        final double[][] a = new double[1][2];
        a[0] = new double[] { 1.0, 1.0 };

        final ConvexMultivariateRealFunction[] inequalities = new ConvexMultivariateRealFunction[6];
        inequalities[0] = new LinearMultivariateRealFunction(new double[] { 1, 1, 1, 1 }, -10000);// x1+x2+x3+x4+1000
                                                                                                  // <
                                                                                                  // 0
        inequalities[1] = new LinearMultivariateRealFunction(
                new double[] { -0.05, 0.2, -0.15, -0.3 }, -1000);// -0.05x1+0.2x2-0.15x3-0.3x4-1000
                                                                 // < 0
        inequalities[2] = new LinearMultivariateRealFunction(new double[] { -1, 0, 0, 0 }, 0);// -x1
                                                                                              // < 0
        inequalities[3] = new LinearMultivariateRealFunction(new double[] { 0, -1, 0, 0 }, 0);// -x2
                                                                                              // < 0
        inequalities[4] = new LinearMultivariateRealFunction(new double[] { 0, 0, -1, 0 }, 0);// -x3
                                                                                              // < 0
        inequalities[5] = new LinearMultivariateRealFunction(new double[] { 0, 0, 0, -1 }, 0);// -x4
                                                                                              // < 0

        final OptimizationRequest or = new OptimizationRequest();
        or.setF0(objectiveFunction);
        or.setFi(inequalities);
        or.setToleranceFeas(1.E-12);
        or.setTolerance(1.E-12);

        final JOptimizer opt = new JOptimizer();
        opt.setOptimizationRequest(or);
        final int returnCode = opt.optimize();
        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }

    }

    /**
     * min(100 * y) s.t.
     * x -y = 1
     * -x < 0
     * Submitted 19/10/2012 by Noreen Jamil.
     * 
     * @throws PatriusException if an error occurs
     */
    public void testLP() throws PatriusException {

        // Objective function
        final LinearMultivariateRealFunction objectiveFunction = new LinearMultivariateRealFunction(
                new double[] { 0., 100. }, 0);

        final double[][] a = new double[1][2];
        a[0] = new double[] { 1.0, -1.0 };
        final double[] b = new double[] { 1.0 };

        // inequalities
        final ConvexMultivariateRealFunction[] inequalities = new ConvexMultivariateRealFunction[1];
        inequalities[0] = new LinearMultivariateRealFunction(new double[] { -1, 0 }, 0.);

        final OptimizationRequest or = new OptimizationRequest();
        or.setF0(objectiveFunction);
        or.setA(a);
        or.setB(b);
        or.setFi(inequalities);
        or.setRescalingDisabled(true);
        // or.setInitialPoint(new double[] { 3, 2 });
        // or.setNotFeasibleInitialPoint(new double[] { 3, 2 });

        // optimization
        final JOptimizer opt = new JOptimizer();
        opt.setOptimizationRequest(or);
        final int returnCode = opt.optimize();

        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }

        final OptimizationResponse response = opt.getOptimizationResponse();
        final double[] sol = response.getSolution();

        assertEquals(0., sol[0], 0.000000001);
        assertEquals(-1., sol[1], 0.000000001);
    }

    /**
     * Linear fractional programming in 2D.
     * Original problem is: <br>
     * min (c.X/e.X) s.t. <br>
     * G.X < h <br>
     * with <br>
     * X = {x,y} <br>
     * c = {2,4} <br>
     * e = {2,3} <br>
     * G = {{-1,1},{3,1},{1/5,-1}} <br>
     * h = {0,3.2,-0.32}
     * 
     * @throws PatriusException if an error occurs
     */
    public void testLFProgramming2D() throws PatriusException {

        // START SNIPPET: LFP-1

        // Objective function (variables y0, y1, z)
        final double[] n = new double[] { 2., 4., 0. };
        final LinearMultivariateRealFunction objectiveFunction = new LinearMultivariateRealFunction(n, 0);

        // inequalities (G.y-h.z<0, z>0)
        final double[][] gmh = new double[][] { { -1.0, 1., 0. }, { 3.0, 1., -3.2 }, { 0.2, -1., 0.32 },
                { 0.0, 0., -1.0 } };
        final ConvexMultivariateRealFunction[] inequalities = new ConvexMultivariateRealFunction[4];
        for (int i = 0; i < 4; i++) {
            inequalities[i] = new LinearMultivariateRealFunction(gmh[i], 0);
        }

        // equalities (e.y+f.z=1)
        final double[][] amb = new double[][] { { 2., 3., 0. } };
        final double[] bm = new double[] { 1 };

        // optimization problem
        final OptimizationRequest or = new OptimizationRequest();
        or.setF0(objectiveFunction);
        or.setA(amb);
        or.setB(bm);
        or.setFi(inequalities);
        or.setTolerance(1.E-6);
        or.setToleranceFeas(1.E-6);

        // optimization
        final JOptimizer opt = new JOptimizer();
        opt.setOptimizationRequest(or);
        final int returnCode = opt.optimize();

        // END SNIPPET: LFP-1

        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }

        final OptimizationResponse response = opt.getOptimizationResponse();
        final double[] sol = response.getSolution();
        final double x = sol[0] / sol[2];
        final double y = sol[1] / sol[2];
        assertEquals(0.9, x, 0.00001);
        assertEquals(0.5, y, 0.00001);
    }

    /**
     * Convex-concave fractional programming in 2D.
     * Original problem is: <br>
     * min (c.X/e.X) s.t. <br>
     * (x-c0)^2 + (y-c1)^2 < R^2 <br>
     * with <br>
     * X = {x,y} <br>
     * c = {2,4} <br>
     * e = {2,3} <br>
     * c0 = 0.65 <br>
     * c1 = 0.65 <br>
     * R = 0.25
     * 
     * @throws PatriusException if an error occurs
     */
    public void testCCFProgramming2D() throws PatriusException {

        // START SNIPPET: CCFP-1

        // Objective function (variables y0, y1, t)
        final double[] n = new double[] { 2., 4., 0. };
        final LinearMultivariateRealFunction objectiveFunction = new LinearMultivariateRealFunction(n, 0);

        // inequalities
        final ConvexMultivariateRealFunction[] inequalities = new ConvexMultivariateRealFunction[2];
        // t > 0
        final double[][] gmh = new double[][] { { 0.0, 0.0, -1.0 } };// t>0
        inequalities[0] = new LinearMultivariateRealFunction(gmh[0], 0);

        // perspective function of (x-c0)^2 + (y-c1)^2 - R^2 < 0
        // this is t*((y0/t - c0)^2 + (y1/t - c1)^2 -R^2)
        // we do not multiply by t, because it would make the function no more convex
        final double c0 = 0.65;
        final double c1 = 0.65;
        final double r = 0.25;
        inequalities[1] = new ConvexMultivariateRealFunction() {

            @Override
            public double value(double[] x) {
                final double y0 = x[0];
                final double y1 = x[1];
                final double t = x[2];
                return t * (Math.pow(y0 / t - c0, 2) + Math.pow(y1 / t - c1, 2) - Math.pow(r, 2));
            }

            @Override
            public double[] gradient(double[] x) {
                final double y0 = x[0];
                final double y1 = x[1];
                final double t = x[2];
                final double[] ret = new double[3];
                ret[0] = 2 * (y0 / t - c0);
                ret[1] = 2 * (y1 / t - c1);
                ret[2] = Math.pow(c0, 2) + Math.pow(c1, 2) - Math.pow(r, 2)
                        - (Math.pow(y0, 2) + Math.pow(y1, 2)) / Math.pow(t, 2);
                return ret;
            }

            @Override
            public double[][] hessian(double[] x) {
                final double y0 = x[0];
                final double y1 = x[1];
                final double t = x[2];
                final double[][] ret = {
                        { 2 / t, 0, -2 * y0 / Math.pow(t, 2) },
                        { 0, 2 / t, -2 * y1 / Math.pow(t, 2) },
                        { -2 * y0 / Math.pow(t, 2), -2 * y1 / Math.pow(t, 2),
                                2 * (Math.pow(y0, 2) + Math.pow(y1, 2)) / Math.pow(t, 3) } };
                return ret;
            }

            @Override
            public int getDim() {
                return 3;
            }
        };

        // equalities (e.y+f.t=1), f is 0
        final double[][] amb = new double[][] { { 2., 3., 0. } };
        final double[] bm = new double[] { 1 };

        // optimization problem
        final OptimizationRequest or = new OptimizationRequest();
        or.setF0(objectiveFunction);
        or.setA(amb);
        or.setB(bm);
        or.setFi(inequalities);
        or.setTolerance(1.E-6);
        or.setToleranceFeas(1.E-6);
        or.setNotFeasibleInitialPoint(new double[] { 0.6, -0.2 / 3., 0.1 });
        or.setCheckKKTSolutionAccuracy(true);

        // optimization
        final JOptimizer opt = new JOptimizer();
        opt.setOptimizationRequest(or);
        final int returnCode = opt.optimize();

        // END SNIPPET: CCFP-1

        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }

        final OptimizationResponse response = opt.getOptimizationResponse();
        final double[] sol = response.getSolution();
        final double x = sol[0] / sol[2];
        final double y = sol[1] / sol[2];
        assertEquals(0.772036, x, 0.000001);
        assertEquals(0.431810, y, 0.000001);
    }

    /**
     * Test linear programming 7D
     * @throws PatriusException if an error occurs
     */
    public void testLinearProgramming7D() throws PatriusException {

        final double[] cVector = new double[] { 0.0, 0.0, 0.0, 1.0, 0.833, 0.833, 0.833 };
        final double[][] aMatrix = new double[][] { { 1.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0 } };
        final double[] bVector = new double[] { 1.0 };
        final double[][] gMatrix = new double[][] { { 0.014, 0.009, 0.021, 1.0, 1.0, 0.0, 0.0 },
                { 0.001, 0.002, -0.002, 1.0, 0.0, 1.0, 0.0 },
                { 0.003, -0.005, 0.002, 1.0, 0.0, 0.0, 1.0 },
                { 0.006, 0.002, 0.007, 0.0, 0.0, 0.0, 0.0 }, { 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 },
                { 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0 }, { 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0 },
                { 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0 }, { 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0 },
                { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0 }, { 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0 } };
        final double[] hVector = new double[] { 0.0, 0.0, 0.0, 0.0010, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 };

        // Objective function (plane)
        final LinearMultivariateRealFunction objectiveFunction = new LinearMultivariateRealFunction(
                cVector, 0.0);

        // inequalities (polyhedral feasible set -G.X-H<0 )
        final ConvexMultivariateRealFunction[] inequalities = new ConvexMultivariateRealFunction[gMatrix.length];
        for (int i = 0; i < gMatrix.length; i++) {
            inequalities[i] = new LinearMultivariateRealFunction(new ArrayRealVector(gMatrix[i])
                    .mapMultiply(-1.).toArray(), -hVector[i]);
        }

        // optimization problem
        final OptimizationRequest or = new OptimizationRequest();
        or.setF0(objectiveFunction);
        or.setFi(inequalities);
        or.setA(aMatrix);
        or.setB(bVector);
        or.setInitialPoint(new double[] { 0.25, 0.25, 0.5, 0.01, 0.01, 0.01, 0.01 });

        // optimization
        final JOptimizer opt = new JOptimizer();
        opt.setOptimizationRequest(or);
        final int returnCode = opt.optimize();

        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }

        final OptimizationResponse response = opt.getOptimizationResponse();
        final double[] sol = response.getSolution();
        assertTrue(sol[0] > 0);
        assertTrue(sol[1] > 0);
        assertTrue(sol[2] > 0);
        assertTrue(sol[4] > 0);
        assertTrue(sol[5] > 0);
        assertTrue(sol[6] > 0);
        assertEquals(sol[0] + sol[1] + sol[2], 1., 0.00000001);
        assertTrue(0.006 * sol[0] + 0.002 * sol[1] + 0.007 * sol[2] > 0.0010);
    }

    /**
     * LP problem with dim=26.
     * A more appropriate solution is given in LPPrimalDualMethodTest.
     * Submitted 01/09/2013 by Chris Myers.
     * 
     * @throws PatriusException if an error occurs
     * @throws IOException if an error occurs while reading.

     */
    public void testLP26Dim() throws PatriusException, IOException {
        final double[] c = TestUtils.loadDoubleArrayFromFile(lpS + "c1.txt");
        final double[][] g = TestUtils.loadDoubleMatrixFromFile(lpS + "G1.txt", space.charAt(0));
        final double[] h = TestUtils.loadDoubleArrayFromFile(lpS + "h1.txt");
        final double[][] a = TestUtils.loadDoubleMatrixFromFile(lpS + "A1.txt", space.charAt(0));
        final double[] b = TestUtils.loadDoubleArrayFromFile(lpS + "b1.txt");
        final double expectedValue = TestUtils.loadDoubleArrayFromFile(lpS + "value1.txt")[0];

        // Objective function
        final LinearMultivariateRealFunction objectiveFunction = new LinearMultivariateRealFunction(c, 0);

        // inequalities
        final ConvexMultivariateRealFunction[] inequalities = new ConvexMultivariateRealFunction[g.length];
        for (int i = 0; i < g.length; i++) {
            inequalities[i] = new LinearMultivariateRealFunction(g[i], -h[i]);
        }

        final OptimizationRequest or = new OptimizationRequest();
        or.setF0(objectiveFunction);
        or.setFi(inequalities);
        or.setA(a);
        or.setB(b);
        or.setCheckKKTSolutionAccuracy(true);
        // or.setInitialPoint(new double[] { 1 });
        // or.setToleranceFeas(1.E-8);
        // or.setTolerance(1.E-9);

        // optimization
        final JOptimizer opt = new JOptimizer();
        opt.setOptimizationRequest(or);
        final int returnCode = opt.optimize();

        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }

        final OptimizationResponse response = opt.getOptimizationResponse();
        final double[] sol = response.getSolution();
        final double value = objectiveFunction.value(sol);

        // check constraints
        final RealMatrix aMatrix = MatrixUtils.createRealMatrix(a);
        final RealVector bVector = new ArrayRealVector(b);
        final RealMatrix gMatrix = MatrixUtils.createRealMatrix(g);
        final RealVector hVector = new ArrayRealVector(h);

        // joptimizer solution
        final RealVector joptSol = new ArrayRealVector(sol);
        // A.x = b
        assertEquals(0., aMatrix.operate(joptSol).subtract(bVector).getNorm(), 1.E-7);
        // G.x < h
        final RealVector gjoptSol = gMatrix.operate(joptSol).subtract(hVector);
        for (int i = 0; i < g.length; i++) {
            assertTrue(gjoptSol.getEntry(i) < 0);
        }

        assertEquals(expectedValue, value, or.getTolerance());
    }
}
