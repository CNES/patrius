/**
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.8:FA:FA-2954:15/11/2021:[PATRIUS] Problemes lors de l'integration de JOptimizer dans Patrius 
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

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;
import fr.cnes.sirius.patrius.math.linear.ArrayRealVector;
import fr.cnes.sirius.patrius.math.linear.MatrixUtils;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealVector;
import fr.cnes.sirius.patrius.math.optim.joptimizer.TestUtils;
import fr.cnes.sirius.patrius.math.optim.joptimizer.util.Utils;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusRuntimeException;

/**
 * @author alberto trivellato (alberto.trivellato@gmail.com)
 */
public class LPPrimalDualMethodTest extends TestCase {

    /** String lp/ */
    final String pathLP = "lp" + File.separator;
    /** String c */
    final String cS = "c";
    /** String G */
    final String gS = "G";
    /** String h */
    final String hS = "h";
    /** String A */
    final String aS = "A";
    /** String b */
    final String bS = "b";
    /** String lb */
    final String lbS = "lb";
    /** String ub */
    final String ubS = "ub";
    /** String .txt */
    final String txt = ".txt";
    /** String sol */
    final String solS = "sol";
    /** String value */
    final String val = "value";
    /** String space */
    final String space = " ";

    /**
     * Simple problem in the form
     * min(100x + y) s.t.
     * x - y = 0
     * 0 <= x <= 1
     * 0 <= y <= 1
     * 
     * @throws PatriusException if an error occurs
     */
    public void testSimple1() throws PatriusException {

        final double[] c = new double[] { -100, 1 };
        final double[][] a = new double[][] { { 1, -1 } };
        final double[] b = new double[] { 0 };
        final double[] lb = new double[] { 0, 0 };
        final double[] ub = new double[] { 1, 1 };
        final double minLb = LPPrimalDualMethod.DEFAULT_MIN_LOWER_BOUND;
        final double maxUb = LPPrimalDualMethod.DEFAULT_MAX_UPPER_BOUND;

        final LPOptimizationRequest or = new LPOptimizationRequest();
        or.setC(c);
        or.setA(a);
        or.setB(b);
        or.setLb(lb);
        or.setUb(ub);
        or.setCheckKKTSolutionAccuracy(true);
        or.setToleranceFeas(1.E-7);
        or.setTolerance(1.E-7);
        or.setPresolvingDisabled(true);
        or.setRescalingDisabled(true);
        or.setNotFeasibleInitialPoint(new double[] { 0, 0 });
        or.setCheckProgressConditions(true);

        // optimization
        final LPPrimalDualMethod opt = new LPPrimalDualMethod(minLb, maxUb);

        opt.setLPOptimizationRequest(or);
        final int returnCode = opt.optimize();

        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }

        final LPOptimizationResponse response = opt.getLPOptimizationResponse();
        final double[] sol = response.getSolution();
        final RealVector cVector = new ArrayRealVector(c);
        final RealVector solVector = new ArrayRealVector(sol);
        final double value = cVector.dotProduct(solVector);

        assertEquals(2, sol.length);
        assertEquals(1, sol[0], or.getTolerance());
        assertEquals(1, sol[1], or.getTolerance());
        assertEquals(-99, value, or.getTolerance());

        final String string = or.toString();
        final String expectedString = "fr.cnes.sirius.patrius.math.optim.joptimizer.optimizers.LPOptimizationRequest: "
                + "\nmin(c) s.t." + "\nA.x = b" + "\nlb <= x <= ub" + "\nc: {-100; 1}" + "\nA: " + or.getA().toString()
                + "\nb: {0}" + "\nlb: {0; 0}" + "\nub: {1; 1}";
        assertEquals(string, expectedString);
    }

    /**
     * Simple problem in the form
     * min(c.x) s.t.
     * A.x = b
     * x >=0
     * 
     * @throws PatriusException if an error occurs
     */
    public void testSimple2() throws PatriusException {

        final double[] c = new double[] { -1, -2 };
        final double[][] a = new double[][] { { 1, 1 } };
        final double[] b = new double[] { 1 };

        final LPOptimizationRequest or = new LPOptimizationRequest();
        or.setC(c);
        or.setA(a);
        or.setB(b);
        or.setLb(new double[] { 0, 0 });
        or.setNotFeasibleInitialPoint(new double[] { -0.5, 1.5 });
        or.setCheckKKTSolutionAccuracy(true);
        or.setToleranceFeas(1.E-7);
        or.setTolerance(1.E-7);

        // optimization
        final LPPrimalDualMethod opt = new LPPrimalDualMethod();

        opt.setLPOptimizationRequest(or);
        final int returnCode = opt.optimize();

        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }

        final LPOptimizationResponse response = opt.getLPOptimizationResponse();
        final double[] sol = response.getSolution();
        final RealVector cVector = new ArrayRealVector(c);
        final RealVector solVector = new ArrayRealVector(sol);
        final double value = cVector.dotProduct(solVector);
        final double f0 = opt.getF0(solVector);
        assertEquals(-2.0, f0);
        assertEquals(2, sol.length);
        assertEquals(0, sol[0], or.getTolerance());
        assertEquals(1, sol[1], or.getTolerance());
        assertEquals(-2, value, or.getTolerance());

        // Test method toString
        final String string = or.toString();
        final String expectedString = "fr.cnes.sirius.patrius.math.optim.joptimizer.optimizers.LPOptimizationRequest: "
                + "\nmin(c) s.t." + "\nA.x = b" + "\nlb <= x" + "\nc: {-1; -2}" + "\nA: " + or.getA().toString()
                + "\nb: {1}" + "\nlb: {0; 0}";
        assertEquals(string, expectedString);
    }

    /**
     * Simple problem in the form
     * min(c.x) s.t.
     * A.x = b
     * lb <= x <= ub
     * with a free variable.
     * This test shows that it is necessary to provide bounds for all the variables in order to
     * avoid singular KKT systems.
     * 
     * @throws PatriusException if an error occurs
     */
    public void testSimple3() throws PatriusException {

        final double[] c = new double[] { -1, -2, 0 };
        final double[][] a = new double[][] { { 1, 1, 0 } };
        final double[] b = new double[] { 1 };
        final double minLb = -99;
        final double maxUb = +99;

        final LPOptimizationRequest or = new LPOptimizationRequest();
        or.setC(c);
        or.setA(a);
        or.setB(b);
        or.setLb(new double[] { -1, -1, -100 });// this will be limited to minLb
        or.setUb(new double[] { 1, 1, 100 });// this will be limited to maxUb
        or.setCheckKKTSolutionAccuracy(true);

        // optimization
        final LPPrimalDualMethod opt = new LPPrimalDualMethod(minLb, maxUb);
        opt.setLPOptimizationRequest(or);
        final int returnCode = opt.optimize();
        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }
        final LPOptimizationResponse response = opt.getLPOptimizationResponse();
        final double[] sol = response.getSolution();
        final RealVector cVector = new ArrayRealVector(c);
        final RealVector solVector = new ArrayRealVector(sol);
        final double value = cVector.dotProduct(solVector);

        assertEquals(3, sol.length);
        assertEquals(0, sol[0], or.getTolerance());
        assertEquals(1, sol[1], or.getTolerance());
        assertEquals(-2, value, or.getTolerance());
    }

    /**
     * Minimize x subject to
     * x+y=4,
     * x-y=2.
     * Should return (3,1).
     * This problem is the same as NewtonLEConstrainedISPTest.testOptimize2()
     * and can be solved only with the use of a linear presolving phase:
     * if passed directly to the solver, it will fail because JOptimizer
     * does not want rank-deficient inequalities matrices like that of this problem.
     * 
     * @throws PatriusException a
     */
    public void testSimple4() throws PatriusException {
        final double[] c = new double[] { 1, 0 };
        final double[][] a = new double[][] { { 1.0, 1.0 }, { 1.0, -1.0 } };
        final double[] b = new double[] { 4.0, 2.0 };

        final LPOptimizationRequest or = new LPOptimizationRequest();
        or.setC(c);
        or.setA(a);
        or.setB(b);
        or.setLb(new double[] { -100, -100 });
        or.setUb(new double[] { 100, 100 });
        or.setCheckKKTSolutionAccuracy(true);

        // optimization
        final LPPrimalDualMethod opt = new LPPrimalDualMethod();
        opt.setLPOptimizationRequest(or);
        final int returnCode = opt.optimize();
        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }
        final LPOptimizationResponse response = opt.getLPOptimizationResponse();
        final double[] sol = response.getSolution();

        assertEquals(2, sol.length);
        assertEquals(3.0, sol[0], or.getTolerance());
        assertEquals(1.0, sol[1], or.getTolerance());
    }

    /**
     * Problem in the form
     * min(c.x) s.t.
     * G.x < h
     * A.x = b
     * 
     * This is a good for testing with a small size problem.
     * Submitted 01/09/2013 by Chris Myers.
     * 
     * @throws PatriusException if an error occurs
     * @throws IOException if an error occurs while reading.
     */
    public void testCGhAb1() throws PatriusException, IOException {

        final String problemId = "1";

        // the original problem: ok until precision 1.E-7
        final double[] c = TestUtils.loadDoubleArrayFromFile(this.pathLP + this.cS + problemId + this.txt);
        final double[][] g = TestUtils.loadDoubleMatrixFromFile(this.pathLP + this.gS + problemId + this.txt,
            this.space.charAt(0));
        final double[] h = TestUtils.loadDoubleArrayFromFile(this.pathLP + this.hS + problemId + this.txt);
        final double[][] a = TestUtils.loadDoubleMatrixFromFile(this.pathLP + this.aS + problemId + this.txt,
            this.space.charAt(0));
        final double[] b = TestUtils.loadDoubleArrayFromFile(this.pathLP + this.bS + problemId + this.txt);
        final double expectedvalue = TestUtils.loadDoubleArrayFromFile(this.pathLP + this.val + problemId + this.txt)[0];

        // double norm =
        // MatrixUtils.createRealMatrix(A).operate(MatrixUtils.createRealVector(expectedSol)).subtract(MatrixUtils.createRealVector(b)).getNorm();
        // assertTrue(norm < 1.e-10);

        final LPOptimizationRequest or = new LPOptimizationRequest();
        or.setC(c);
        or.setG(g);
        or.setH(h);
        or.setA(a);
        or.setB(b);
        or.setCheckKKTSolutionAccuracy(true);
        or.setToleranceKKT(1.E-7);
        or.setToleranceFeas(1.E-7);
        or.setTolerance(1.E-7);
        or.setAlpha(0.75);
        or.setInitialPoint(new double[] { 0.9999998735888544, -999.0000001264111, 1000.0, 0.9999998735888544, 0.0,
                -999.0000001264111, 0.9999999661257591, 0.9999998735888544, 1000.0, 0.0, 0.9999998735888544, 0.0,
                0.9999998735888544, 0.9999998735888544, 0.9999998735888544, 0.0, 0.0, 0.9999998735888544, -1000.0,
                0.9999999198573067, 9.253690467190285E-8, 1000.0, -999.0000001264111, 0.9999998735888544, -1000.0,
                -1000.0 });

        // optimization
        final LPPrimalDualMethod opt = new LPPrimalDualMethod();

        opt.setLPOptimizationRequest(or);
        final int returnCode = opt.optimize();

        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }

        final LPOptimizationResponse response = opt.getLPOptimizationResponse();
        final double[] sol = response.getSolution();
        final RealVector cVector = new ArrayRealVector(c);
        final RealVector solVector = new ArrayRealVector(sol);
        final double value = cVector.dotProduct(solVector);

        // check constraints
        final RealVector x = MatrixUtils.createRealVector(sol);
        final RealMatrix gMatrix = MatrixUtils.createRealMatrix(g);
        final RealVector hvector = MatrixUtils.createRealVector(h);
        final RealMatrix aMatrix = MatrixUtils.createRealMatrix(a);
        final RealVector bvector = MatrixUtils.createRealVector(b);
        final RealVector gxh = gMatrix.operate(x).subtract(hvector);
        for (int i = 0; i < gxh.getDimension(); i++) {
            assertTrue(gxh.getEntry(i) <= 0);// not strictly because some constraint has been
                                             // treated as a bound
        }
        final RealVector axb = aMatrix.operate(x).subtract(bvector);
        assertEquals(0., axb.getNorm(), or.getToleranceFeas());

        // check value
        assertEquals(expectedvalue, value, or.getTolerance());

    }

    /**
     * Problem in the form
     * min(c.x) s.t.
     * G.x < h
     * A.x = b
     * 
     * This is the same problem as testCGhAb1 but defining
     * the not feasible point as a feasible initial point, just for test
     * 
     * @throws PatriusException if an error occurs
     * @throws IOException if an error occurs while reading.
     */
    public void testCGhAb1b() throws PatriusException, IOException {

        final String problemId = "1";

        // the original problem: ok until precision 1.E-7
        final double[] c = TestUtils.loadDoubleArrayFromFile(this.pathLP + this.cS + problemId + this.txt);
        final double[][] g = TestUtils.loadDoubleMatrixFromFile(this.pathLP + this.gS + problemId + this.txt,
            this.space.charAt(0));
        final double[] h = TestUtils.loadDoubleArrayFromFile(this.pathLP + this.hS + problemId + this.txt);
        final double[][] a = TestUtils.loadDoubleMatrixFromFile(this.pathLP + this.aS + problemId + this.txt,
            this.space.charAt(0));
        final double[] b = TestUtils.loadDoubleArrayFromFile(this.pathLP + this.bS + problemId + this.txt);
        final double expectedvalue = TestUtils.loadDoubleArrayFromFile(this.pathLP + this.val + problemId + this.txt)[0];

        final LPOptimizationRequest or = new LPOptimizationRequest();
        or.setC(c);
        or.setG(g);
        or.setH(h);
        or.setA(a);
        or.setB(b);
        or.setCheckKKTSolutionAccuracy(true);
        or.setToleranceKKT(1.E-7);
        or.setToleranceFeas(1.E-7);
        or.setTolerance(1.E-7);
        or.setAlpha(0.75);
        or.setNotFeasibleInitialPoint(new double[] { 0.9999998735888544, -999.0000001264111, 1000.0,
                0.9999998735888544, 0.0, -999.0000001264111, 0.9999999661257591, 0.9999998735888544, 1000.0, 0.0,
                0.9999998735888544, 0.0, 0.9999998735888544, 0.9999998735888544, 0.9999998735888544, 0.0, 0.0,
                0.9999998735888544, -1000.0, 0.9999999198573067, 9.253690467190285E-8, 1000.0, -999.0000001264111,
                0.9999998735888544, -1000.0, -1000.0 });

        // optimization
        // LPPrimalDualMethodOLD opt = new LPPrimalDualMethodOLD();
        final LPPrimalDualMethod opt = new LPPrimalDualMethod();

        opt.setLPOptimizationRequest(or);
        final int returnCode = opt.optimize();

        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }

        final LPOptimizationResponse response = opt.getLPOptimizationResponse();
        final double[] sol = response.getSolution();
        final RealVector cVector = new ArrayRealVector(c);
        final RealVector solVector = new ArrayRealVector(sol);
        final double value = cVector.dotProduct(solVector);

        // check constraints
        final RealVector x = MatrixUtils.createRealVector(sol);
        final RealMatrix gMatrix = MatrixUtils.createRealMatrix(g);
        final RealVector hvector = MatrixUtils.createRealVector(h);
        final RealMatrix aMatrix = MatrixUtils.createRealMatrix(a);
        final RealVector bvector = MatrixUtils.createRealVector(b);
        final RealVector gxh = gMatrix.operate(x).subtract(hvector);
        for (int i = 0; i < gxh.getDimension(); i++) {
            assertTrue(gxh.getEntry(i) <= 0);// not strictly because some constraint has been
                                             // treated as a bound
        }
        final RealVector axb = aMatrix.operate(x).subtract(bvector);
        assertEquals(0., axb.getNorm(), or.getToleranceFeas());

        // check value
        assertEquals(expectedvalue, value, or.getTolerance());

    }

    /**
     * Problem in the form
     * min(c.x) s.t.
     * G.x < h
     * A.x = b
     * lb <= x <= ub
     * 
     * This is the same as testCGhAb3, but lb and ub are outside G.
     * The presolved problem has a deterministic solution, that is, all the variables have a fixed
     * value.
     * Submitted 01/09/2013 by Chris Myers.
     * 
     * @throws PatriusException if an error occurs
     * @throws IOException if an error occurs while reading.
     */
    public void testCGhAbLbUb2() throws PatriusException, IOException {

        final String problemId = "2";

        final double[] c = TestUtils.loadDoubleArrayFromFile(this.pathLP + this.cS + problemId + this.txt);
        final double[][] g = TestUtils.loadDoubleMatrixFromFile(this.pathLP + this.gS + problemId + this.txt,
            this.space.charAt(0));
        final double[] h = TestUtils.loadDoubleArrayFromFile(this.pathLP + this.hS + problemId + this.txt);
        final double[][] a = TestUtils.loadDoubleMatrixFromFile(this.pathLP + this.aS + problemId + this.txt,
            this.space.charAt(0));
        final double[] b = TestUtils.loadDoubleArrayFromFile(this.pathLP + this.bS + problemId + this.txt);
        final double[] lb = TestUtils.loadDoubleArrayFromFile(this.pathLP + this.lbS + problemId + this.txt);
        final double[] ub = TestUtils.loadDoubleArrayFromFile(this.pathLP + this.ubS + problemId + this.txt);
        final double[] expectedSol = TestUtils.loadDoubleArrayFromFile(this.pathLP + this.solS + problemId + this.txt);
        final double expectedvalue = TestUtils.loadDoubleArrayFromFile(this.pathLP + this.val + problemId + this.txt)[0];
        final double minLb = 0;
        final double maxUb = 1.0E15;// it is do high because of the very high values of the elements of h

        final LPOptimizationRequest or = new LPOptimizationRequest();
        or.setC(c);
        or.setG(g);
        or.setH(h);
        or.setA(a);
        or.setB(b);
        or.setLb(lb);
        or.setUb(ub);
        or.setCheckKKTSolutionAccuracy(true);

        // optimization
        final LPPrimalDualMethod opt = new LPPrimalDualMethod(minLb, maxUb);

        opt.setLPOptimizationRequest(or);
        final int returnCode = opt.optimize();

        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }

        final LPOptimizationResponse response = opt.getLPOptimizationResponse();
        final double[] sol = response.getSolution();
        final RealVector cVector = new ArrayRealVector(c);
        final RealVector solVector = new ArrayRealVector(sol);
        final double value = cVector.dotProduct(solVector);

        // check constraints
        assertEquals(lb.length, sol.length);
        assertEquals(ub.length, sol.length);
        final RealVector x = MatrixUtils.createRealVector(sol);
        final RealMatrix gMatrix = MatrixUtils.createRealMatrix(g);
        final RealVector hvector = MatrixUtils.createRealVector(h);
        final RealMatrix aMatrix = MatrixUtils.createRealMatrix(a);
        final RealVector bvector = MatrixUtils.createRealVector(b);
        for (int i = 0; i < lb.length; i++) {
            final double di;
            if (Double.isNaN(lb[i])) {
                di = -Double.MAX_VALUE;
            } else {
                di = lb[i];
            }
            assertTrue(di <= x.getEntry(i));
        }
        for (int i = 0; i < ub.length; i++) {
            final double di;
            if (Double.isNaN(ub[i])) {
                di = Double.MAX_VALUE;
            } else {
                di = ub[i];
            }
            assertTrue(di >= x.getEntry(i));
        }
        final RealVector gxh = gMatrix.operate(x).subtract(hvector);
        for (int i = 0; i < gxh.getDimension(); i++) {
            assertTrue(gxh.getEntry(i) < 0);
        }
        final RealVector axb = aMatrix.operate(x).subtract(bvector);
        assertEquals(0., axb.getNorm(), or.getToleranceFeas());

        assertEquals(expectedSol.length, sol.length);
        for (int i = 0; i < sol.length; i++) {
            assertEquals(expectedSol[0], sol[0], 1.e-7);
        }
        assertEquals(expectedvalue, value, 1.e-7);

    }

    /**
     * Simple problem in the form
     * min(c.x) s.t.
     * G.x < h
     * A.x = b
     * 
     * This is the same as testCGhAbLbUb2, but lb and ub are into G.
     * The presolved problem has a deterministic solution, that is, all the variables have a fixed
     * value.
     * 
     * @throws PatriusException if an error occurs
     * @throws IOException if an error occurs while reading.
     */
    public void testCGhAb3() throws PatriusException, IOException {

        final String problemId = "3";

        final double[] c = TestUtils.loadDoubleArrayFromFile(this.pathLP + this.cS + problemId + this.txt);
        final double[][] g = TestUtils.loadDoubleMatrixFromFile(this.pathLP + this.gS + problemId + this.txt,
            this.space.charAt(0));
        final double[] h = TestUtils.loadDoubleArrayFromFile(this.pathLP + this.hS + problemId + this.txt);
        final double[][] a = TestUtils.loadDoubleMatrixFromFile(this.pathLP + this.aS + problemId + this.txt,
            this.space.charAt(0));
        final double[] b = TestUtils.loadDoubleArrayFromFile(this.pathLP + this.bS + problemId + this.txt);
        final double[] expectedSol = TestUtils.loadDoubleArrayFromFile(this.pathLP + this.solS + problemId + this.txt);
        final double expectedvalue = TestUtils.loadDoubleArrayFromFile(this.pathLP + this.val + problemId + this.txt)[0];
        final double minLb = 0;
        final double maxUb = 1.0E15;// it is so high because of the very high values of the elements of h

        final LPOptimizationRequest or = new LPOptimizationRequest();
        or.setC(c);
        or.setG(g);
        or.setH(h);
        or.setA(a);
        or.setB(b);
        or.setCheckKKTSolutionAccuracy(true);

        // optimization
        final LPPrimalDualMethod opt = new LPPrimalDualMethod(minLb, maxUb);

        opt.setLPOptimizationRequest(or);
        final int returnCode = opt.optimize();

        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }

        final LPOptimizationResponse response = opt.getLPOptimizationResponse();
        final double[] sol = response.getSolution();
        final RealVector cVector = new ArrayRealVector(c);
        final RealVector solVector = new ArrayRealVector(sol);
        final double value = cVector.dotProduct(solVector);

        // check constraints
        final RealVector x = MatrixUtils.createRealVector(sol);
        final RealMatrix gMatrix = MatrixUtils.createRealMatrix(g);
        final RealVector hvector = MatrixUtils.createRealVector(h);
        final RealMatrix aMatrix = MatrixUtils.createRealMatrix(a);
        final RealVector bvector = MatrixUtils.createRealVector(b);
        final RealVector gxh = gMatrix.operate(x).subtract(hvector);
        for (int i = 0; i < gxh.getDimension(); i++) {
            assertTrue(gxh.getEntry(i) <= 0);
        }
        final RealVector axb = aMatrix.operate(x).subtract(bvector);
        assertEquals(0., axb.getNorm(), or.getToleranceFeas());

        assertEquals(expectedSol.length, sol.length);
        for (int i = 0; i < sol.length; i++) {
            assertEquals(expectedSol[0], sol[0], or.getTolerance());
        }
        assertEquals(expectedvalue, value, or.getTolerance());

    }

    /**
     * Problem in the form
     * min(c.x) s.t.
     * A.x = b
     * lb <= x <= ub
     * 
     * This problem involves recursive column duplicate reductions.
     * This is a good for testing with a small size problem.
     * 
     * @throws PatriusException if an error occurs
     * @throws IOException if an error occurs while reading.
     */
    public void testCAbLbUb5() throws PatriusException, IOException {

        final String problemId = "5";

        final double[] c = TestUtils.loadDoubleArrayFromFile(this.pathLP + this.cS + problemId + this.txt);
        final double[][] a = TestUtils.loadDoubleMatrixFromFile(this.pathLP + this.aS + problemId + this.txt,
            this.space.charAt(0));
        final double[] b = TestUtils.loadDoubleArrayFromFile(this.pathLP + this.bS + problemId + this.txt);
        final double[] lb = TestUtils.loadDoubleArrayFromFile(this.pathLP + this.lbS + problemId + this.txt);
        final double[] ub = TestUtils.loadDoubleArrayFromFile(this.pathLP + this.ubS + problemId + this.txt);
        final double expectedvalue = TestUtils.loadDoubleArrayFromFile(this.pathLP + this.val + problemId + this.txt)[0];

        final LPOptimizationRequest or = new LPOptimizationRequest();
        or.setC(c);
        or.setA(a);
        or.setB(b);
        or.setLb(lb);
        or.setUb(ub);
        or.setCheckKKTSolutionAccuracy(true);
        // or.setToleranceKKT(1.e-7);
        // or.setToleranceFeas(1.E-7);
        // or.setTolerance(1.E-7);

        // optimization
        final LPPrimalDualMethod opt = new LPPrimalDualMethod();

        opt.setLPOptimizationRequest(or);
        final int returnCode = opt.optimize();

        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }

        final LPOptimizationResponse response = opt.getLPOptimizationResponse();
        final double[] sol = response.getSolution();
        final RealVector cVector = new ArrayRealVector(c);
        final RealVector solVector = new ArrayRealVector(sol);
        final double value = cVector.dotProduct(solVector);

        // check constraints
        assertEquals(lb.length, sol.length);
        assertEquals(ub.length, sol.length);
        final RealVector x = MatrixUtils.createRealVector(sol);
        final RealMatrix aMatrix = MatrixUtils.createRealMatrix(a);
        final RealVector bvector = MatrixUtils.createRealVector(b);
        for (int i = 0; i < lb.length; i++) {
            final double di;
            if (Double.isNaN(lb[i])) {
                di = -Double.MAX_VALUE;
            } else {
                di = lb[i];
            }
            assertTrue(di <= x.getEntry(i));
        }
        for (int i = 0; i < ub.length; i++) {
            final double di;
            if (Double.isNaN(ub[i])) {
                di = Double.MAX_VALUE;
            } else {
                di = ub[i];
            }
            assertTrue(di >= x.getEntry(i));
        }
        final RealVector axb = aMatrix.operate(x).subtract(bvector);
        assertEquals(0., axb.getNorm(), or.getToleranceFeas());

        // check value
        assertEquals(expectedvalue, value, or.getTolerance());
    }

    /**
     * Problem in the form
     * min(c.x) s.t.
     * A.x = b
     * lb <= x <= ub
     * 
     * This problem involves column duplicate reduction.
     * This is a good for testing with a small size problem.
     * 
     * @throws PatriusException if an error occurs
     * @throws IOException if an error occurs while reading.
     */
    public void testCAbLbUb6() throws PatriusException, IOException {

        final String problemId = "6";

        final double[] c = TestUtils.loadDoubleArrayFromFile(this.pathLP + this.cS + problemId + this.txt);
        final double[][] a = TestUtils.loadDoubleMatrixFromFile(this.pathLP + this.aS + problemId + this.txt,
            this.space.charAt(0));
        final double[] b = TestUtils.loadDoubleArrayFromFile(this.pathLP + this.bS + problemId + this.txt);
        final double[] lb = TestUtils.loadDoubleArrayFromFile(this.pathLP + this.lbS + problemId + this.txt);
        final double[] ub = TestUtils.loadDoubleArrayFromFile(this.pathLP + this.ubS + problemId + this.txt);
        final double expectedvalue = TestUtils.loadDoubleArrayFromFile(this.pathLP + this.val + problemId + this.txt)[0];

        final LPOptimizationRequest or = new LPOptimizationRequest();
        or.setC(c);
        or.setA(a);
        or.setB(b);
        or.setLb(lb);
        or.setUb(ub);
        or.setCheckKKTSolutionAccuracy(true);

        // optimization
        final LPPrimalDualMethod opt = new LPPrimalDualMethod();

        opt.setLPOptimizationRequest(or);
        final int returnCode = opt.optimize();

        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }

        final LPOptimizationResponse response = opt.getLPOptimizationResponse();
        final double[] sol = response.getSolution();
        final RealVector cVector = new ArrayRealVector(c);
        final RealVector solVector = new ArrayRealVector(sol);
        final double value = cVector.dotProduct(solVector);

        // check constraints
        assertEquals(lb.length, sol.length);
        assertEquals(ub.length, sol.length);
        final RealVector x = MatrixUtils.createRealVector(sol);
        final RealMatrix aMatrix = MatrixUtils.createRealMatrix(a);
        final RealVector bvector = MatrixUtils.createRealVector(b);
        for (int i = 0; i < lb.length; i++) {
            final double di;
            if (Double.isNaN(lb[i])) {
                di = -Double.MAX_VALUE;
            } else {
                di = lb[i];
            }
            assertTrue(di <= x.getEntry(i));
        }
        for (int i = 0; i < ub.length; i++) {
            final double di;
            if (Double.isNaN(ub[i])) {
                di = Double.MAX_VALUE;
            } else {
                di = ub[i];
            }
            assertTrue(di >= x.getEntry(i));
        }
        final RealVector axb = aMatrix.operate(x).subtract(bvector);
        assertEquals(0., axb.getNorm(), or.getToleranceFeas());

        // check value
        assertEquals(expectedvalue, value, or.getTolerance());
    }

    /**
     * Problem in the form
     * min(c.x) s.t.
     * G.x < h
     * A.x = b
     * lb <= x <= ub
     * 
     * @throws PatriusException if an error occurs
     * @throws IOException if an error occurs while reading.
     */
    public void testCGhAbLbUb7() throws PatriusException, IOException {

        final String problemId = "7";

        final double[] c = TestUtils.loadDoubleArrayFromFile(this.pathLP + this.cS + problemId + this.txt);
        final double[][] g = TestUtils.loadDoubleMatrixFromFile(this.pathLP + this.gS + problemId + this.txt,
            this.space.charAt(0));
        final double[] h = TestUtils.loadDoubleArrayFromFile(this.pathLP + this.hS + problemId + this.txt);
        final double[][] a = TestUtils.loadDoubleMatrixFromFile(this.pathLP + this.aS + problemId + this.txt,
            this.space.charAt(0));
        final double[] b = TestUtils.loadDoubleArrayFromFile(this.pathLP + this.bS + problemId + this.txt);
        double[] lb = TestUtils.loadDoubleArrayFromFile(this.pathLP + this.lbS + problemId + this.txt);
        double[] ub = TestUtils.loadDoubleArrayFromFile(this.pathLP + this.ubS + problemId + this.txt);
        final double[] expectedSol = TestUtils.loadDoubleArrayFromFile(this.pathLP + this.solS + problemId + this.txt);
        final double expectedvalue = TestUtils.loadDoubleArrayFromFile(this.pathLP + this.val + problemId + this.txt)[0];

        // the unbounded bounds are saved on the files with NaN values, so substitute them with
        // acceptable values
        lb = Utils.replaceValues(lb, Double.NaN, LPPrimalDualMethod.DEFAULT_MIN_LOWER_BOUND);
        ub = Utils.replaceValues(ub, Double.NaN, LPPrimalDualMethod.DEFAULT_MAX_UPPER_BOUND);

        final LPOptimizationRequest or = new LPOptimizationRequest();
        or.setC(c);
        or.setG(g);
        or.setH(h);
        or.setA(a);
        or.setB(b);
        or.setLb(lb);
        or.setUb(ub);
        or.setCheckKKTSolutionAccuracy(true);

        // optimization
        final LPPrimalDualMethod opt = new LPPrimalDualMethod();

        opt.setLPOptimizationRequest(or);
        final int returnCode = opt.optimize();

        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }

        final LPOptimizationResponse response = opt.getLPOptimizationResponse();
        final double[] sol = response.getSolution();
        final RealVector cVector = new ArrayRealVector(c);
        final RealVector solVector = new ArrayRealVector(sol);
        final double value = cVector.dotProduct(solVector);

        // check constraints
        assertEquals(lb.length, sol.length);
        assertEquals(ub.length, sol.length);
        final RealVector x = MatrixUtils.createRealVector(sol);
        final RealMatrix gMatrix = MatrixUtils.createRealMatrix(g);
        final RealVector hvector = MatrixUtils.createRealVector(h);
        final RealMatrix aMatrix = MatrixUtils.createRealMatrix(a);
        final RealVector bvector = MatrixUtils.createRealVector(b);
        for (int i = 0; i < lb.length; i++) {
            assertTrue(lb[i] <= x.getEntry(i));
        }
        for (int i = 0; i < ub.length; i++) {
            final double di;
            if (Double.isNaN(lb[i])) {
                di = -Double.MAX_VALUE;
            } else {
                di = lb[i];
            }
            assertTrue(di <= x.getEntry(i));
        }
        final RealVector gxh = gMatrix.operate(x).subtract(hvector);
        for (int i = 0; i < gxh.getDimension(); i++) {
            final double di;
            if (Double.isNaN(ub[i])) {
                di = Double.MAX_VALUE;
            } else {
                di = ub[i];
            }
            assertTrue(di >= x.getEntry(i));
        }
        final RealVector axb = aMatrix.operate(x).subtract(bvector);
        assertEquals(0., axb.getNorm(), or.getToleranceFeas());

        assertEquals(expectedSol.length, sol.length);
        for (int i = 0; i < sol.length; i++) {
            assertEquals(expectedSol[0], sol[0], 1E-4);
        }
        assertEquals(expectedvalue, value, 1E-4);

    }

    /**
     * Problem in the form
     * min(c.x) s.t.
     * A.x = b
     * lb <= x <= ub
     * 
     * @throws PatriusException if an error occurs
     * @throws IOException if an error occurs while reading.
     */
    public void testCAbLbUb8() throws PatriusException, IOException {

        final String problemId = "8";

        final double[] c = TestUtils.loadDoubleArrayFromFile(this.pathLP + this.cS + problemId + this.txt);
        final double[][] a = TestUtils.loadDoubleMatrixFromFile(this.pathLP + this.aS + problemId + this.txt,
            this.space.charAt(0));
        final double[] b = TestUtils.loadDoubleArrayFromFile(this.pathLP + this.bS + problemId + this.txt);
        double[] lb = TestUtils.loadDoubleArrayFromFile(this.pathLP + this.lbS + problemId + this.txt);
        double[] ub = TestUtils.loadDoubleArrayFromFile(this.pathLP + this.ubS + problemId + this.txt);
        final double expectedvalue = TestUtils.loadDoubleArrayFromFile(this.pathLP + this.val + problemId + this.txt)[0];

        // the unbounded bounds are saved on the files with NaN values, so substitute them with
        // acceptable values
        lb = Utils.replaceValues(lb, Double.NaN, LPPrimalDualMethod.DEFAULT_MIN_LOWER_BOUND);
        ub = Utils.replaceValues(ub, Double.NaN, LPPrimalDualMethod.DEFAULT_MAX_UPPER_BOUND);

        final LPOptimizationRequest or = new LPOptimizationRequest();
        or.setC(c);
        or.setA(a);
        or.setB(b);
        or.setLb(lb);
        or.setUb(ub);
        or.setCheckKKTSolutionAccuracy(true);
        or.setRescalingDisabled(true);

        // optimization
        final LPPrimalDualMethod opt = new LPPrimalDualMethod();

        opt.setLPOptimizationRequest(or);
        final int returnCode = opt.optimize();

        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }

        final LPOptimizationResponse response = opt.getLPOptimizationResponse();
        final double[] sol = response.getSolution();
        final RealVector cVector = new ArrayRealVector(c);
        final RealVector solVector = new ArrayRealVector(sol);
        final double value = cVector.dotProduct(solVector);

        // check constraints
        assertEquals(lb.length, sol.length);
        assertEquals(ub.length, sol.length);
        final RealVector x = MatrixUtils.createRealVector(sol);
        final RealMatrix aMatrix = MatrixUtils.createRealMatrix(a);
        final RealVector bvector = MatrixUtils.createRealVector(b);
        for (int i = 0; i < lb.length; i++) {
            final double di;
            if (Double.isNaN(lb[i])) {
                di = -Double.MAX_VALUE;
            } else {
                di = lb[i];
            }
            assertTrue(di <= x.getEntry(i));
        }
        for (int i = 0; i < ub.length; i++) {
            final double di;
            if (Double.isNaN(ub[i])) {
                di = Double.MAX_VALUE;
            } else {
                di = ub[i];
            }
            assertTrue(di >= x.getEntry(i));
        }
        final RealVector axb = aMatrix.operate(x).subtract(bvector);
        assertEquals(0., axb.getNorm(), or.getToleranceFeas());

        // check value
        assertEquals(expectedvalue, value, 1E-2);
    }

    /**
     * Problem in the form
     * min(c.x) s.t.
     * G.x < h
     * A.x = b
     * lb <= x <= ub
     * 
     * @throws PatriusException if an error occurs
     * @throws IOException if an error occurs while reading.
     */
    public void testCGhAbLbUb10() throws PatriusException, IOException {

        final String problemId = "10";

        final double[] c = TestUtils.loadDoubleArrayFromFile(this.pathLP + this.cS + problemId + this.txt);
        final double[][] g = TestUtils.loadDoubleMatrixFromFile(this.pathLP + this.gS + problemId + this.txt,
            this.space.charAt(0));
        final double[] h = TestUtils.loadDoubleArrayFromFile(this.pathLP + this.hS + problemId + this.txt);
        final double[][] a = TestUtils.loadDoubleMatrixFromFile(this.pathLP + this.aS + problemId + this.txt,
            this.space.charAt(0));
        final double[] b = TestUtils.loadDoubleArrayFromFile(this.pathLP + this.bS + problemId + this.txt);
        double[] lb = TestUtils.loadDoubleArrayFromFile(this.pathLP + this.lbS + problemId + this.txt);
        double[] ub = TestUtils.loadDoubleArrayFromFile(this.pathLP + this.ubS + problemId + this.txt);
        final double[] expectedSol = TestUtils.loadDoubleArrayFromFile(this.pathLP + this.solS + problemId + this.txt);
        final double expectedvalue = TestUtils.loadDoubleArrayFromFile(this.pathLP + this.val + problemId + this.txt)[0];

        // the unbounded bounds are saved on the files with NaN values, so substitute them with
        // acceptable values
        lb = Utils.replaceValues(lb, Double.NaN, LPPrimalDualMethod.DEFAULT_MIN_LOWER_BOUND);
        ub = Utils.replaceValues(ub, Double.NaN, LPPrimalDualMethod.DEFAULT_MAX_UPPER_BOUND);

        final LPOptimizationRequest or = new LPOptimizationRequest();
        or.setC(c);
        or.setG(g);
        or.setH(h);
        or.setA(a);
        or.setB(b);
        or.setLb(lb);
        or.setUb(ub);
        or.setCheckKKTSolutionAccuracy(true);

        // optimization
        final LPPrimalDualMethod opt = new LPPrimalDualMethod();

        opt.setLPOptimizationRequest(or);
        final int returnCode = opt.optimize();

        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }

        final LPOptimizationResponse response = opt.getLPOptimizationResponse();
        final double[] sol = response.getSolution();
        final RealVector cVector = new ArrayRealVector(c);
        final RealVector solVector = new ArrayRealVector(sol);
        final double value = cVector.dotProduct(solVector);

        // check constraints
        assertEquals(lb.length, sol.length);
        assertEquals(ub.length, sol.length);
        final RealVector x = MatrixUtils.createRealVector(sol);
        final RealMatrix gMatrix = MatrixUtils.createRealMatrix(g);
        final RealVector hvector = MatrixUtils.createRealVector(h);
        final RealMatrix aMatrix = MatrixUtils.createRealMatrix(a);
        final RealVector bvector = MatrixUtils.createRealVector(b);
        for (int i = 0; i < lb.length; i++) {
            assertTrue(lb[i] <= x.getEntry(i));
        }
        for (int i = 0; i < ub.length; i++) {
            final double di;
            if (Double.isNaN(lb[i])) {
                di = -Double.MAX_VALUE;
            } else {
                di = lb[i];
            }
            assertTrue(di <= x.getEntry(i));
        }
        final RealVector gxh = gMatrix.operate(x).subtract(hvector);
        for (int i = 0; i < gxh.getDimension(); i++) {
            final double di;
            if (Double.isNaN(ub[i])) {
                di = Double.MAX_VALUE;
            } else {
                di = ub[i];
            }
            assertTrue(di >= x.getEntry(i));
        }
        final RealVector axb = aMatrix.operate(x).subtract(bvector);
        assertEquals(0., axb.getNorm(), or.getToleranceFeas());

        assertEquals(expectedSol.length, sol.length);
        for (int i = 0; i < sol.length; i++) {
            assertEquals(expectedSol[0], sol[0], or.getTolerance());
        }
        assertEquals(expectedvalue, value, 1E-2);

    }

    /**
     * Test constructor LPPrimalDualMethod
     * when value of the minimum lower bound is not acceptable
     * -> it has to throw an exception
     */
    public void testConstructor() throws IllegalArgumentException {
        try {
            new LPPrimalDualMethod(Double.NaN, 1);
        } catch (final IllegalArgumentException e) {
            assertTrue(true);// ok, min value lb is not acceptable
            return;
        }
        fail();
    }

    /**
     * Test constructor LPPrimalDualMethod
     * when value of the maximum upper bound is not acceptable
     * -> it has to throw an exception
     */
    public void testConstructor2() throws IllegalArgumentException {
        try {
            new LPPrimalDualMethod(1, Double.NaN);
        } catch (final IllegalArgumentException e) {
            assertTrue(true);// ok, max value ub is not acceptable
            return;
        }
        fail();
    }

    /**
     * Test setOptimizationRequest with a request not instance of LPOptimizationRequest
     * -> it throws an exception
     * 
     * @throws PatriusException if an error occurs
     */
    public void testSetOptimizationRequestError() {
        final LPOptimizationResponse or = new LPOptimizationResponse();
        final AbstractLPOptimizationRequestHandler opt = new LPPrimalDualMethod();
        opt.setOptimizationResponse(or);
        try {
            opt.setOptimizationRequest(null);
        } catch (final UnsupportedOperationException e) {
            assertTrue(true);// ok, request not OptimizationRequest
            return;
        }
        fail();
    }

    /**
     * Test setOptimizationResponse with a response not instance of LPOptimizationResponse
     * -> it throws an exception
     * 
     * @throws PatriusException if an error occurs
     */
    public void testSetOptimizationResponseError() {
        final LPOptimizationRequest or = new LPOptimizationRequest();
        final AbstractLPOptimizationRequestHandler opt = new LPPrimalDualMethod();
        opt.setOptimizationRequest(or);
        try {
            opt.setOptimizationResponse(null);
        } catch (final UnsupportedOperationException e) {
            assertTrue(true);// ok, request not OptimizationRequest
            return;
        }
        fail();
    }

    /**
     * Test findFeasibleInitialPoint of BasicPhaseILPPDM
     * with negative toleranceFeas that is not possible to achieve -> it throws an error
     * @throws IOException
     */
    public void testBasicPhaseILPPDMSmallTolerance() throws IOException {
        final String problemId = "2";

        final double[] c = TestUtils.loadDoubleArrayFromFile(this.pathLP + this.cS + problemId + this.txt);
        final double[][] g = TestUtils.loadDoubleMatrixFromFile(this.pathLP + this.gS + problemId + this.txt,
            this.space.charAt(0));
        final double[] h = TestUtils.loadDoubleArrayFromFile(this.pathLP + this.hS + problemId + this.txt);
        final double[][] a = TestUtils.loadDoubleMatrixFromFile(this.pathLP + this.aS + problemId + this.txt,
            this.space.charAt(0));
        final double[] b = TestUtils.loadDoubleArrayFromFile(this.pathLP + this.bS + problemId + this.txt);
        final double[] lb = TestUtils.loadDoubleArrayFromFile(this.pathLP + this.lbS + problemId + this.txt);
        final double[] ub = TestUtils.loadDoubleArrayFromFile(this.pathLP + this.ubS + problemId + this.txt);
        final double minLb = 0;
        final double maxUb = 1.0E15;// it is do high because of the very high values of the elements of h

        final LPOptimizationRequest or = new LPOptimizationRequest();
        or.setC(c);
        or.setG(g);
        or.setH(h);
        or.setA(a);
        or.setB(b);
        or.setLb(lb);
        or.setUb(ub);
        or.setToleranceFeas(-1); // negative tolerance
        final LPPrimalDualMethod opt = new LPPrimalDualMethod(minLb, maxUb);
        opt.setLPOptimizationRequest(or);

        try {
            opt.optimizePresolvedStandardLP();
        } catch (final PatriusException e) {
            assertTrue(true);// ok, impossible to reach a negative tolerance
            return;
        }
        fail();
    }

    /**
     * Check that getHessF0 throws an exceptions
     * because hessians are null for linear problems
     */
    public void testGetHessF0() {
        final LPPrimalDualMethod opt = new LPPrimalDualMethod();
        try {
            opt.getHessF0(null);
        } catch (final PatriusRuntimeException e) {
            assertTrue(true); // ok, hessians are null for linear problems
            return;
        }
        fail();
    }

    /**
     * Check that getGradFi throws an exception
     * because GradFi are not used for LP
     */
    public void testGetGradFi() {
        final LPPrimalDualMethod opt = new LPPrimalDualMethod();
        try {
            opt.getGradFi(null);
        } catch (final PatriusRuntimeException e) {
            assertTrue(true); // ok, GradFi are not used for LP
            return;
        }
        fail();
    }

    /**
     * Check that getHessFi throws an exception
     * because hessians are null for linear problems
     */
    public void testGetHessFi() {
        final LPPrimalDualMethod opt = new LPPrimalDualMethod();
        try {
            opt.getHessFi(null);
        } catch (final PatriusRuntimeException e) {
            assertTrue(true); // ok, hessians are null for linear problems
            return;
        }
        fail();
    }

    /**
     * Test method toString from LPOptimizationRequest
     * with all possible components
     */
    public void testToString() {
        final double[] c = new double[] { 1, 1 };
        final double[][] g = new double[][] { { 1, -1 }, { 2, 0 } };
        final double[] h = new double[] { 2, 4 };
        final double[][] a = new double[][] { { 1, -1 } };
        final double[] b = new double[] { 0 };
        final double[] lb = new double[] { 0, 0 };
        final double[] ub = new double[] { 1, 1 };
        final double[] ylb = new double[] { 0, 0 };
        final double[] yub = new double[] { 1, 1 };
        final double[] zlb = new double[] { 0, 0 };
        final double[] zub = new double[] { 1, 1 };

        final LPOptimizationRequest or = new LPOptimizationRequest();
        or.setC(c);
        or.setG(g);
        or.setH(h);
        or.setA(a);
        or.setB(b);
        or.setLb(lb);
        or.setUb(ub);
        or.setYlb(new ArrayRealVector(ylb));
        or.setYub(new ArrayRealVector(yub));
        or.setZlb(new ArrayRealVector(zlb));
        or.setZub(new ArrayRealVector(zub));

        final String string = or.toString();
        final String expectedString = "fr.cnes.sirius.patrius.math.optim.joptimizer.optimizers.LPOptimizationRequest: "
                + "\nmin(c) s.t." + "\nG.x < h" + "\nA.x = b" + "\nlb <= x <= ub" + "\nc: {1; 1}" + "\nG: "
                + or.getG().toString() + "\nh: {2; 4}" + "\nA: " + or.getA().toString() + "\nb: {0}" + "\nlb: {0; 0}"
                + "\nub: {1; 1}" + "\nylb: {0; 0}" + "\nyub: {1; 1}" + "\nzlb: {0; 0}" + "\nzub: {1; 1}";
        assertEquals(string, expectedString);
    }

    /**
     * Test method toString from LPOptimizationRequest
     * with upper bound defined, but not lower bound
     */
    public void testToString2() {
        final double[] c = new double[] { 1, 1 };
        final double[][] a = new double[][] { { 1, -1 } };
        final double[] b = new double[] { 0 };
        final double[] ub = new double[] { 1, 1 };

        final LPOptimizationRequest or = new LPOptimizationRequest();
        or.setC(c);
        or.setA(a);
        or.setB(b);
        or.setUb(ub);

        final String string = or.toString();
        final String expectedString = "fr.cnes.sirius.patrius.math.optim.joptimizer.optimizers.LPOptimizationRequest: "
                + "\nmin(c) s.t." + "\nA.x = b" + "\nx <= ub" + "\nc: {1; 1}" + "\nA: " + or.getA().toString()
                + "\nb: {0}" + "\nub: {1; 1}";
        assertEquals(string, expectedString);
    }

    /**
     * Test optimizePresolvedStandardLP
     * with different dimensions lower and upper bounds -> it throws an error
     * @throws PatriusException
     */
    public void testErrorOptimizePresolvedStandardLP() throws PatriusException {

        final double[] lb = new double[] { 0, 0, 0 };
        final double[] ub = new double[] { 1, 1 };
        final double minLb = LPPrimalDualMethod.DEFAULT_MIN_LOWER_BOUND;
        final double maxUb = LPPrimalDualMethod.DEFAULT_MAX_UPPER_BOUND;

        final LPOptimizationRequest or = new LPOptimizationRequest();
        or.setLb(lb);
        or.setUb(ub);

        // optimization
        final LPPrimalDualMethod opt = new LPPrimalDualMethod(minLb, maxUb);
        opt.setLPOptimizationRequest(or);
        try {
            opt.optimizePresolvedStandardLP();
        } catch (final IllegalArgumentException e) {
            assertTrue(true);// ok, lb and ub dimensions mismatch
            return;
        }
        fail();
    }

    /**
     * Problem in the form
     * min(c.x) s.t.
     * G.x < h
     * A.x = b
     * 
     * Defining not feasible initial point -> it throws an exception
     * 
     * @throws IOException if an error occurs while reading.
     */
    public void testNotFeasibleInitialPoint() throws IOException {

        final String problemId = "1";

        // the original problem: ok until precision 1.E-7
        final double[] c = TestUtils.loadDoubleArrayFromFile(this.pathLP + this.cS + problemId + this.txt);
        final double[][] g = TestUtils.loadDoubleMatrixFromFile(this.pathLP + this.gS + problemId + this.txt,
            this.space.charAt(0));
        final double[] h = TestUtils.loadDoubleArrayFromFile(this.pathLP + this.hS + problemId + this.txt);
        final double[][] a = TestUtils.loadDoubleMatrixFromFile(this.pathLP + this.aS + problemId + this.txt,
            this.space.charAt(0));
        final double[] b = TestUtils.loadDoubleArrayFromFile(this.pathLP + this.bS + problemId + this.txt);

        final LPOptimizationRequest or = new LPOptimizationRequest();
        or.setC(c);
        or.setG(g);
        or.setH(h);
        or.setA(a);
        or.setB(b);
        or.setCheckKKTSolutionAccuracy(true);
        or.setToleranceKKT(1.E-7);
        or.setToleranceFeas(1.E-7);
        or.setTolerance(1.E-7);
        or.setAlpha(0.75);
        or.setInitialPoint(new double[] { 100, -999.0000001264111, 1000.0, 0.9999998735888544, 0.0, -999.0000001264111,
                0.9999999661257591, 0.9999998735888544, 1000.0, 0.0, 0.9999998735888544, 0.0, 0.9999998735888544,
                0.9999998735888544, 0.9999998735888544, 0.0, 0.0, 200, -1000.0, 0.9999999198573067,
                9.253690467190285E8, 1000.0, -999.0000001264111, 0.9999998735888544, -1000.0, -1000.0 });

        // optimization
        final LPPrimalDualMethod opt = new LPPrimalDualMethod();

        opt.setLPOptimizationRequest(or);
        try {
            opt.optimize();
        } catch (final PatriusException e) {
            assertTrue(true);// ok, the initial point is not feasible
            return;
        }
        fail();
    }

    /**
     * Problem in the form
     * min(c.x) s.t.
     * A.x = b
     * lb <= x <= ub
     * 
     * With maximum iterations set to 10-> it fails to optimize in less than 10 iterations
     * 
     * @throws PatriusException if an error occurs
     * @throws IOException if an error occurs while reading.
     */
    public void testMaxIterations() throws PatriusException, IOException {

        final String problemId = "5";

        final double[] c = TestUtils.loadDoubleArrayFromFile(this.pathLP + this.cS + problemId + this.txt);
        final double[][] a = TestUtils.loadDoubleMatrixFromFile(this.pathLP + this.aS + problemId + this.txt,
            this.space.charAt(0));
        final double[] b = TestUtils.loadDoubleArrayFromFile(this.pathLP + this.bS + problemId + this.txt);
        final double[] lb = TestUtils.loadDoubleArrayFromFile(this.pathLP + this.lbS + problemId + this.txt);
        final double[] ub = TestUtils.loadDoubleArrayFromFile(this.pathLP + this.ubS + problemId + this.txt);

        final LPOptimizationRequest or = new LPOptimizationRequest();
        or.setC(c);
        or.setA(a);
        or.setB(b);
        or.setLb(lb);
        or.setUb(ub);
        or.setCheckKKTSolutionAccuracy(true);
        or.setMaxIteration(10);

        // optimization
        final LPPrimalDualMethod opt = new LPPrimalDualMethod();

        opt.setLPOptimizationRequest(or);
        final int returnCode = opt.optimize();

        if (returnCode == OptimizationResponse.FAILED) {
            assertTrue(true); // ok, max iterations reached
            return;
        }
        fail();
    }

    /**
     * Problem in the form
     * min(c.x) s.t.
     * A.x = b
     * lb <= x <= ub
     * 
     * With maximum iterations set to 10-> it fails to optimize in less than 10 iterations
     * Presolving disabled
     * 
     * @throws PatriusException if an error occurs
     * @throws IOException if an error occurs while reading.
     */
    public void testMaxIterations2() throws PatriusException, IOException {

        final String problemId = "5";

        final double[] c = TestUtils.loadDoubleArrayFromFile(this.pathLP + this.cS + problemId + this.txt);
        final double[][] a = TestUtils.loadDoubleMatrixFromFile(this.pathLP + this.aS + problemId + this.txt,
            this.space.charAt(0));
        final double[] b = TestUtils.loadDoubleArrayFromFile(this.pathLP + this.bS + problemId + this.txt);
        final double[] lb = TestUtils.loadDoubleArrayFromFile(this.pathLP + this.lbS + problemId + this.txt);
        final double[] ub = TestUtils.loadDoubleArrayFromFile(this.pathLP + this.ubS + problemId + this.txt);

        final LPOptimizationRequest or = new LPOptimizationRequest();
        or.setC(c);
        or.setA(a);
        or.setB(b);
        or.setLb(lb);
        or.setUb(ub);
        or.setCheckKKTSolutionAccuracy(true);
        or.setMaxIteration(10);
        or.setPresolvingDisabled(true);

        // optimization
        final LPPrimalDualMethod opt = new LPPrimalDualMethod();

        opt.setLPOptimizationRequest(or);
        final int returnCode = opt.optimize();

        if (returnCode == OptimizationResponse.FAILED) {
            assertTrue(true); // ok, max iterations reached
            return;
        }
        fail();
    }

    /**
     * Test with negative Lagrangian point -> it throws an exception
     * 
     * @throws IOException if an error occurs while reading.
     * @throws PatriusException
     */
    public void testNegativeLagrangianPoint() throws IOException, PatriusException {

        final String problemId = "1";

        // the original problem: ok until precision 1.E-7
        final double[] c = TestUtils.loadDoubleArrayFromFile(this.pathLP + this.cS + problemId + this.txt);
        final double[][] a = TestUtils.loadDoubleMatrixFromFile(this.pathLP + this.aS + problemId + this.txt,
            this.space.charAt(0));
        final double[] b = TestUtils.loadDoubleArrayFromFile(this.pathLP + this.bS + problemId + this.txt);

        final LPOptimizationRequest or = new LPOptimizationRequest();
        or.setC(c);
        or.setA(a);
        or.setB(b);
        or.setInitialLagrangian(new double[] { -0.5, 0.5, 0.5, 0.5 });
        or.setCheckKKTSolutionAccuracy(true);
        or.setToleranceKKT(1.E-7);
        or.setToleranceFeas(1.E-7);
        or.setTolerance(1.E-7);
        or.setAlpha(0.75);
        or.setMaxIteration(0);
        or.setInitialPoint(new double[] { 0.9999998735888544, -999.0000001264111, 1000.0, 0.9999998735888544, 0.0,
                -999.0000001264111, 0.9999999661257591, 0.9999998735888544, 1000.0, 0.0, 0.9999998735888544, 0.0,
                0.9999998735888544, 0.9999998735888544, 0.9999998735888544, 0.0, 0.0, 0.9999998735888544, -1000.0,
                0.9999999198573067, 9.253690467190285E-8, 1000.0, -999.0000001264111, 0.9999998735888544, -1000.0,
                -1000.0 });

        // optimization
        final LPPrimalDualMethod opt = new LPPrimalDualMethod();
        opt.setLPOptimizationRequest(or);
        try {
            opt.optimizePresolvedStandardLP();
        } catch (final IllegalArgumentException e) {
            assertTrue(true);// ok, negative Lagrangian point
            return;
        }
        fail();
    }

    /**
     * Presolved standard form LP problem with Lagrangian bounds
     * 
     * @throws PatriusException if an error occurs
     */
    public void testStandardPresolved() throws PatriusException {

        final double[] c = new double[] { 0, -100, 1 };
        final double[][] a = new double[][] { { 1, 1, 1 }, { 0, 1, -1 } };
        final double[] b = new double[] { 2, 0 };
        final double[] lb = new double[] { 0, 0, 0 };
        final double[] ub = new double[] { 1, 1, 1 };
        final double[] Ylb = new double[] { 0, 0 };
        final double[] Yub = new double[] { 1, Double.NaN };
        final double[] Zlb = new double[] { 0, 0, Double.NaN };
        final double[] Zub = new double[] { 1, Double.NaN, 1 };
        final double[] expectedSolution = { 0, 1, 1 };

        final double minLb = LPPrimalDualMethod.DEFAULT_MIN_LOWER_BOUND;
        final double maxUb = LPPrimalDualMethod.DEFAULT_MAX_UPPER_BOUND;

        final LPOptimizationRequest or = new LPOptimizationRequest();
        or.setC(c);
        or.setA(a);
        or.setB(b);
        or.setLb(lb);
        or.setUb(ub);
        or.setYlb(new ArrayRealVector(Ylb));
        or.setYub(new ArrayRealVector(Yub));
        or.setZlb(new ArrayRealVector(Zlb));
        or.setZub(new ArrayRealVector(Zub));
        or.setCheckKKTSolutionAccuracy(true);
        or.setToleranceFeas(1.2E-7);
        or.setTolerance(1.E-7);
        or.setPresolvingDisabled(true);
        or.setRescalingDisabled(true);
        or.setInitialPoint(new double[] { 0.644444444444, 0.6777777, 0.67777777 });
        or.setCheckProgressConditions(true);

        // optimization
        final LPPrimalDualMethod opt = new LPPrimalDualMethod(minLb, maxUb);

        opt.setLPOptimizationRequest(or);
        // opt.optimize();
        final int returnCode = opt.optimizePresolvedStandardLP();

        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }

        // check constraints
        final LPOptimizationResponse response = opt.getLPOptimizationResponse();
        final double[] sol = response.getSolution();
        final RealVector x = MatrixUtils.createRealVector(sol);
        final RealMatrix aMatrix = MatrixUtils.createRealMatrix(a);
        final RealVector bvector = MatrixUtils.createRealVector(b);
        final RealVector axb = aMatrix.operate(x).subtract(bvector);
        assertEquals(0., axb.getNorm(), or.getToleranceFeas());
        for (int i = 0; i < expectedSolution.length; i++) {
            assertEquals(expectedSolution[i], sol[i], or.getTolerance());
        }

    }

    /**
     * Presolved standard form LP problem with Lagrangian bounds
     * 
     * @throws PatriusException if an error occurs
     */
    public void testStandardPresolved2() throws PatriusException {

        final double[] c = new double[] { 0, -100, 1 };
        final double[][] a = new double[][] { { 1, 1, 1 }, { 0, 1, -1 } };
        final double[] b = new double[] { 2, 0 };
        final double[] lb = new double[] { 0, 0, 0 };
        final double[] ub = new double[] { 1, 1, 1 };
        final double[] Ylb = new double[] { Double.NaN, Double.NaN };
        final double[] Yub = new double[] { 1, Double.NaN };
        final double[] Zlb = new double[] { 0, 0, Double.NaN };
        final double[] Zub = new double[] { 1, Double.NaN, 1 };
        final double[] expectedSolution = { 0, 1, 1 };

        final double minLb = LPPrimalDualMethod.DEFAULT_MIN_LOWER_BOUND;
        final double maxUb = LPPrimalDualMethod.DEFAULT_MAX_UPPER_BOUND;

        final LPOptimizationRequest or = new LPOptimizationRequest();
        or.setC(c);
        or.setA(a);
        or.setB(b);
        or.setLb(lb);
        or.setUb(ub);
        or.setYlb(new ArrayRealVector(Ylb));
        or.setYub(new ArrayRealVector(Yub));
        or.setZlb(new ArrayRealVector(Zlb));
        or.setZub(new ArrayRealVector(Zub));
        or.setCheckKKTSolutionAccuracy(true);
        or.setToleranceFeas(1.2E-7);
        or.setTolerance(1.E-7);
        or.setPresolvingDisabled(true);
        or.setRescalingDisabled(true);
        or.setInitialPoint(new double[] { 0.644444444444, 0.6777777, 0.67777777 });
        or.setCheckProgressConditions(true);

        // optimization
        final LPPrimalDualMethod opt = new LPPrimalDualMethod(minLb, maxUb);

        opt.setLPOptimizationRequest(or);
        // opt.optimize();
        final int returnCode = opt.optimizePresolvedStandardLP();

        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }

        // check constraints
        final LPOptimizationResponse response = opt.getLPOptimizationResponse();
        final double[] sol = response.getSolution();
        final RealVector x = MatrixUtils.createRealVector(sol);
        final RealMatrix aMatrix = MatrixUtils.createRealMatrix(a);
        final RealVector bvector = MatrixUtils.createRealVector(b);
        final RealVector axb = aMatrix.operate(x).subtract(bvector);
        assertEquals(0., axb.getNorm(), or.getToleranceFeas());
        for (int i = 0; i < expectedSolution.length; i++) {
            assertEquals(expectedSolution[i], sol[i], or.getTolerance());
        }

    }

    /**
     * Simple problem in the form
     * min(100x + y) s.t.
     * 0 <= x <= 1
     * 0 <= y <= 1
     * with no equalities
     * 
     * @throws PatriusException if an error occurs
     */
    public void testNoEqualities() throws PatriusException {

        final double[] c = new double[] { -1, -1 };
        final double[] lb = new double[] { 0, 0 };
        final double[] ub = new double[] { 0.4, 0.4 };
        final double minLb = LPPrimalDualMethod.DEFAULT_MIN_LOWER_BOUND;
        final double maxUb = LPPrimalDualMethod.DEFAULT_MAX_UPPER_BOUND;

        final LPOptimizationRequest or = new LPOptimizationRequest();
        or.setC(c);
        or.setLb(lb);
        or.setUb(ub);
        or.setCheckKKTSolutionAccuracy(true);
        or.setToleranceFeas(1.E-7);
        or.setTolerance(1.E-7);
        or.setPresolvingDisabled(true);
        or.setRescalingDisabled(true);
        or.setCheckProgressConditions(true);

        // optimization
        final LPPrimalDualMethod opt = new LPPrimalDualMethod(minLb, maxUb);

        opt.setLPOptimizationRequest(or);
        final int returnCode = opt.optimizePresolvedStandardLP();

        if (returnCode == OptimizationResponse.FAILED) {
            fail();
        }

        final LPOptimizationResponse response = opt.getLPOptimizationResponse();
        final double[] sol = response.getSolution();
        final RealVector cVector = new ArrayRealVector(c);
        final RealVector solVector = new ArrayRealVector(sol);
        final double value = cVector.dotProduct(solVector);

        assertEquals(2, sol.length);
        assertEquals(0.399999, sol[0], 0.01);
        assertEquals(0.39999, sol[1], 0.01);
        assertEquals(-0.799, value, 0.01);

    }
}