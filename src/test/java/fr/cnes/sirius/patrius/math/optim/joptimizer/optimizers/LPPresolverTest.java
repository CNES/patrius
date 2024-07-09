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
import fr.cnes.sirius.patrius.math.linear.MatrixUtils;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealVector;
import fr.cnes.sirius.patrius.math.linear.SingularValueDecomposition;
import fr.cnes.sirius.patrius.math.optim.joptimizer.TestUtils;
import fr.cnes.sirius.patrius.math.optim.joptimizer.optimizers.LPPresolver;
import fr.cnes.sirius.patrius.math.optim.joptimizer.util.Utils;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusRuntimeException;

/**
 * LP presolving test.
 * 
 * @author alberto trivellato (alberto.trivellato@gmail.com)
* HISTORY
* VERSION:4.7:DM:DM-2766:18/05/2021:Evol. et corr. dans le package fr.cnes.sirius.patrius.math.linear (suite DM 2300) 
* VERSION:4.6:DM:DM-2591:27/01/2021:[PATRIUS] Intigration et validation JOptimizer
* END-HISTORY
 */
public class LPPresolverTest extends TestCase {
    /** String lp/presolving/ */
    final String presolvingS = "lp" + File.separator + "presolving" + File.separator;
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
    /** String space */
    final String space = " ";
    /** String sol */
    final String solS = "sol";
    /** String value */
    final String val = "value";
    /** String tolerance */
    final String tol = "tolerance";
    /** String s */
    final String sS = "s";
    /**
     * Test from file 1 
     * 
     * @throws PatriusException if an error occurs
     * @throws IOException if an error occurs while reading.
     */
    public void testFromFile1() throws IOException, PatriusException {

        final String problemId = "1";

        final double[] c = TestUtils.loadDoubleArrayFromFile(presolvingS + cS + problemId + txt);
        final double[][] a = TestUtils.loadDoubleMatrixFromFile(presolvingS + aS + problemId + txt, space.charAt(0));
        final double[] b = TestUtils.loadDoubleArrayFromFile(presolvingS + bS + problemId + txt);
        final double[] lb = TestUtils.loadDoubleArrayFromFile(presolvingS + lbS + problemId + txt);
        final double[] ub = TestUtils.loadDoubleArrayFromFile(presolvingS + ubS + problemId + txt);
        double s = 0;
        try{
            s = TestUtils.loadDoubleArrayFromFile(presolvingS + sS + problemId + txt)[0];
        }catch(IOException e){}
        final double[] expectedSolution = TestUtils.loadDoubleArrayFromFile(presolvingS + solS + problemId + txt);
        final double expectedValue = TestUtils.loadDoubleArrayFromFile(presolvingS + val + problemId + txt)[0];
        double expectedTolerance = TestUtils.loadDoubleArrayFromFile(presolvingS + tol + problemId + txt)[0];

        expectedTolerance = Math.max(
                expectedTolerance,
                MatrixUtils.createRealMatrix(a)
                        .operate(MatrixUtils.createRealVector(expectedSolution))
                        .subtract(MatrixUtils.createRealVector(b)).getNorm());
        doPresolving(c, a, b, lb, ub, s, expectedSolution, expectedValue, expectedTolerance);
    }

    /**
     * This problem has a deterministic solution.
     * 
     * @throws IOException if an error occurs while reading.
     */
    public void testFromFile2() throws IOException {

        final String problemId = "2";

        final double[] c = TestUtils.loadDoubleArrayFromFile(presolvingS + cS + problemId + txt);
        final double[][] a = TestUtils.loadDoubleMatrixFromFile(presolvingS + aS + problemId + txt, space.charAt(0));
        final double[] b = TestUtils.loadDoubleArrayFromFile(presolvingS + bS + problemId + txt);
        final double[] lb = TestUtils.loadDoubleArrayFromFile(presolvingS + lbS + problemId + txt);
        final double[] ub = TestUtils.loadDoubleArrayFromFile(presolvingS + ubS + problemId + txt);
        double s = 0;
        try{
            s = TestUtils.loadDoubleArrayFromFile(presolvingS + sS + problemId + txt)[0];
        }catch(NullPointerException e){}
        final double[] expectedSolution = TestUtils.loadDoubleArrayFromFile(presolvingS + solS + problemId + txt);

        // must be: A pXn with rank(A)=p < n

        final LPPresolver lpPresolver = new LPPresolver();
        lpPresolver.setNOfSlackVariables((short) s);
        lpPresolver.setExpectedSolution(expectedSolution);// this is just for test!
        lpPresolver.presolve(c, a, b, lb, ub);  
        final int n = lpPresolver.getPresolvedN();

        // deterministic solution
        assertEquals(0, n);
        assertTrue(lpPresolver.getPresolvedC() == null);
        assertTrue(lpPresolver.getPresolvedA() == null);
        assertTrue(lpPresolver.getPresolvedB() == null);
        assertTrue(lpPresolver.getPresolvedLB() == null);
        assertTrue(lpPresolver.getPresolvedUB() == null);
        assertTrue(lpPresolver.getPresolvedYlb() == null);
        assertTrue(lpPresolver.getPresolvedYub() == null);
        assertTrue(lpPresolver.getPresolvedZlb() == null);
        assertTrue(lpPresolver.getPresolvedZub() == null);
        final double[] sol = lpPresolver.postsolve(new double[] {});
        assertEquals(expectedSolution.length, sol.length);
        for (int i = 0; i < sol.length; i++) {
            assertEquals(expectedSolution[i], sol[i], 1.e-9);
        }
    }

    /**
     * Test from file 4
     * 
     * @throws PatriusException if an error occurs
     * @throws IOException if an error occurs while reading.
     */
    public void testFromFile4() throws PatriusException, IOException {

        final String problemId = "4";

        final double[] c = TestUtils.loadDoubleArrayFromFile(presolvingS + cS + problemId + txt);
        final double[][] a = TestUtils.loadDoubleMatrixFromFile(presolvingS + aS + problemId + txt, space.charAt(0));
        final double[] b = TestUtils.loadDoubleArrayFromFile(presolvingS + bS + problemId + txt);
        final double[] lb = TestUtils.loadDoubleArrayFromFile(presolvingS + lbS + problemId + txt);
        final double[] ub = TestUtils.loadDoubleArrayFromFile(presolvingS + ubS + problemId + txt);
        double s = 0;
        try{
            s = TestUtils.loadDoubleArrayFromFile(presolvingS + sS + problemId + txt)[0];
        }catch(IOException e){}
        final double[] expectedSolution = TestUtils.loadDoubleArrayFromFile(presolvingS + solS + problemId + txt);
        final double expectedValue = TestUtils.loadDoubleArrayFromFile(presolvingS + val + problemId + txt)[0];
        double expectedTolerance = TestUtils.loadDoubleArrayFromFile(presolvingS + tol + problemId + txt)[0];

        expectedTolerance = Math.max(
                expectedTolerance,
                MatrixUtils.createRealMatrix(a)
                        .operate(MatrixUtils.createRealVector(expectedSolution))
                        .subtract(MatrixUtils.createRealVector(b)).getNorm());
        doPresolving(c, a, b, lb, ub, s, expectedSolution, expectedValue, expectedTolerance);
    }

    /**
     * This test involves duplicated columns.
     * 
     * @throws PatriusException if an error occurs
     * @throws IOException if an error occurs while reading.
     */
    public void testFromFile5() throws PatriusException, IOException {
        final String problemId = "5";

        final double[] c = TestUtils.loadDoubleArrayFromFile(presolvingS + cS + problemId + txt);
        final double[][] a = TestUtils.loadDoubleMatrixFromFile(presolvingS + aS + problemId + txt, space.charAt(0));
        final double[] b = TestUtils.loadDoubleArrayFromFile(presolvingS + bS + problemId + txt);
        final double[] lb = TestUtils.loadDoubleArrayFromFile(presolvingS + lbS + problemId + txt);
        final double[] ub = TestUtils.loadDoubleArrayFromFile(presolvingS + ubS + problemId + txt);
        double s = 0;
        try{
            s = TestUtils.loadDoubleArrayFromFile(presolvingS + sS + problemId + txt)[0];
        }catch(NullPointerException e){}
        final double[] expectedSolution = TestUtils.loadDoubleArrayFromFile(presolvingS + solS + problemId + txt);
        final double expectedValue = TestUtils.loadDoubleArrayFromFile(presolvingS + val + problemId + txt)[0];
        double expectedTolerance = TestUtils.loadDoubleArrayFromFile(presolvingS + tol + problemId + txt)[0];

        expectedTolerance = Math.max(
                expectedTolerance,
                MatrixUtils.createRealMatrix(a)
                        .operate(MatrixUtils.createRealVector(expectedSolution))
                        .subtract(MatrixUtils.createRealVector(b)).getNorm());
        doPresolving(c, a, b, lb, ub, s, expectedSolution, expectedValue, expectedTolerance);
    }

    /**
     * 
     * @throws PatriusException if an error occurs
     * @throws IOException if an error occurs while reading.
     */
    public void testFromFile8() throws PatriusException, IOException {
        final String problemId = "8";

        final double[] c = TestUtils.loadDoubleArrayFromFile(presolvingS + cS + problemId + txt);
        final double[][] a = TestUtils.loadDoubleMatrixFromFile(presolvingS + aS + problemId + txt, space.charAt(0));
        final double[] b = TestUtils.loadDoubleArrayFromFile(presolvingS + bS + problemId + txt);
        final double[] lb = TestUtils.loadDoubleArrayFromFile(presolvingS + lbS + problemId + txt);
        final double[] ub = TestUtils.loadDoubleArrayFromFile(presolvingS + ubS + problemId + txt);
        double s = 0;
        try{
            s = TestUtils.loadDoubleArrayFromFile(presolvingS + sS + problemId + txt)[0];
        }catch(IOException e){}
        final double[] expectedSolution = TestUtils.loadDoubleArrayFromFile(presolvingS + solS + problemId + txt);
        final double expectedValue = TestUtils.loadDoubleArrayFromFile(presolvingS + val + problemId + txt)[0];
        double expectedTolerance = TestUtils.loadDoubleArrayFromFile(presolvingS + tol + problemId + txt)[0];

        expectedTolerance = Math.max(
                expectedTolerance,
                MatrixUtils.createRealMatrix(a)
                        .operate(MatrixUtils.createRealVector(expectedSolution))
                        .subtract(MatrixUtils.createRealVector(b)).getNorm());
        expectedTolerance = 0.0005;
        doPresolving(c, a, b, lb, ub, s, expectedSolution, expectedValue, expectedTolerance);
    }

    /**
     * This is the afiro netlib problem presolved with JOptimizer without compareBounds.
     * 
     * @throws PatriusException if an error occurs
     * @throws IOException if an error occurs while reading.
     */
    public void testFromFile10() throws PatriusException, IOException {
        final String problemId = "10";

        final double[] c = TestUtils.loadDoubleArrayFromFile(presolvingS + cS + problemId + txt);
        final double[][] a = TestUtils.loadDoubleMatrixFromFile(presolvingS + aS + problemId + txt, space.charAt(0));
        final double[] b = TestUtils.loadDoubleArrayFromFile(presolvingS + bS + problemId + txt);
        final double[] lb = TestUtils.loadDoubleArrayFromFile(presolvingS + lbS + problemId + txt);
        final double[] ub = TestUtils.loadDoubleArrayFromFile(presolvingS + ubS + problemId + txt);
        double s = 0;
        try{
            s = TestUtils.loadDoubleArrayFromFile(presolvingS + sS + problemId + txt)[0];
        }catch(NullPointerException e){}
        final double[] expectedSolution = TestUtils.loadDoubleArrayFromFile(presolvingS + solS + problemId + txt);
        final double expectedValue = TestUtils.loadDoubleArrayFromFile(presolvingS + val + problemId + txt)[0];
        double expectedTolerance = TestUtils.loadDoubleArrayFromFile(presolvingS + tol + problemId + txt)[0];

        expectedTolerance = Math.max(
                expectedTolerance,
                MatrixUtils.createRealMatrix(a)
                        .operate(MatrixUtils.createRealVector(expectedSolution))
                        .subtract(MatrixUtils.createRealVector(b)).getNorm());
        doPresolving(c, a, b, lb, ub, s, expectedSolution, expectedValue, expectedTolerance);
    }

    /**
     * This is the presolved (with CPlex) Recipe netlib problem in standard form.
     * 
     * @throws PatriusException if an error occurs
     * @throws IOException if an error occurs while reading.
     */
    public void testFromFile11() throws PatriusException, IOException {
        final String problemId = "11";

        final double[] c = TestUtils.loadDoubleArrayFromFile(presolvingS + cS + problemId + txt);
        final double[][] a = TestUtils.loadDoubleMatrixFromFile(presolvingS + aS + problemId + txt, space.charAt(0));
        final double[] b = TestUtils.loadDoubleArrayFromFile(presolvingS + bS + problemId + txt);
        final double[] lb = TestUtils.loadDoubleArrayFromFile(presolvingS + lbS + problemId + txt);
        final double[] ub = TestUtils.loadDoubleArrayFromFile(presolvingS + ubS + problemId + txt);
        double s = 0;
        try{
            s = TestUtils.loadDoubleArrayFromFile(presolvingS + sS + problemId + txt)[0];
        }catch(NullPointerException e){}
        final double[] expectedSolution = TestUtils.loadDoubleArrayFromFile(presolvingS + solS + problemId + txt);
        final double expectedValue = TestUtils.loadDoubleArrayFromFile(presolvingS + val + problemId + txt)[0];
        double expectedTolerance = TestUtils.loadDoubleArrayFromFile(presolvingS + tol + problemId + txt)[0];

        expectedTolerance = Math.max(
                expectedTolerance,
                MatrixUtils.createRealMatrix(a)
                        .operate(MatrixUtils.createRealVector(expectedSolution))
                        .subtract(MatrixUtils.createRealVector(b)).getNorm());
        expectedTolerance = 1.e-9;
        doPresolving(c, a, b, lb, ub, s, expectedSolution, expectedValue, expectedTolerance);
    }

    /**
     * This is the VTP.BASE netlib problem in standard form.
     * 
     * @throws PatriusException if an error occurs
     * @throws IOException if an error occurs while reading.
     */
    public void testFromFile12() throws PatriusException, IOException {

        final String problemId = "12";

        final double[] c = TestUtils.loadDoubleArrayFromFile(presolvingS + cS + problemId + txt);
        final double[][] a = TestUtils.loadDoubleMatrixFromFile(presolvingS + aS + problemId + txt, space.charAt(0));
        final double[] b = TestUtils.loadDoubleArrayFromFile(presolvingS + bS + problemId + txt);
        final double[] lb = TestUtils.loadDoubleArrayFromFile(presolvingS + lbS + problemId + txt);
        final double[] ub = TestUtils.loadDoubleArrayFromFile(presolvingS + ubS + problemId + txt);
        double s = 0;
        try{
            s = TestUtils.loadDoubleArrayFromFile(presolvingS + sS + problemId + txt)[0];
        }catch(NullPointerException e){}
        final double[] expectedSolution = TestUtils.loadDoubleArrayFromFile(presolvingS + solS + problemId + txt);
        final double expectedValue = TestUtils.loadDoubleArrayFromFile(presolvingS + val + problemId + txt)[0];
        double expectedTolerance = TestUtils.loadDoubleArrayFromFile(presolvingS + tol + problemId + txt)[0];

        expectedTolerance = Math.max(
                expectedTolerance,
                MatrixUtils.createRealMatrix(a)
                        .operate(MatrixUtils.createRealVector(expectedSolution))
                        .subtract(MatrixUtils.createRealVector(b)).getNorm());
        doPresolving(c, a, b, lb, ub, s, expectedSolution, expectedValue, expectedTolerance);
    }
      

    /**
     * Presolving
     * 
     * @param c array C
     * @param a matrix A
     * @param b array B
     * @param lb array lower bounds
     * @param ub array upper bounds
     * @param s value S
     * @param expectedSolution expected solution
     * @param expectedValue expected value
     * @param expectedTolerance expected tolerance
     * @throws PatriusException if an error occurs
     * @throws PatriusRuntimeException if an error occurs
     */
    private void doPresolving(double[] c, double[][] a, double[] b, double[] lb, double[] ub,
            double s, double[] expectedSolution, double expectedValue, double expectedTolerance)
            throws PatriusRuntimeException {

        RealMatrix aMatrix = MatrixUtils.createRealMatrix(a);
        SingularValueDecomposition dec = new SingularValueDecomposition(aMatrix);
        int rankA = dec.getRank();

        final LPPresolver lpPresolver = new LPPresolver();
        lpPresolver.setNOfSlackVariables((short) s);
        lpPresolver.setExpectedSolution(expectedSolution);// this is just for test!
        lpPresolver.setZeroTolerance(Utils.getDoubleMachineEpsilon());
        // lpPresolver.setExpectedTolerance(expectedTolerance);//this is just for test!
        lpPresolver.presolve(c, a, b, lb, ub);
        final double[][] presolvedA = lpPresolver.getPresolvedA().getData(false);
        final double[] presolvedB = lpPresolver.getPresolvedB().toArray();
        final double[] presolvedLb = lpPresolver.getPresolvedLB().toArray();
        final double[] presolvedUb = lpPresolver.getPresolvedUB().toArray();

        // check objective function
        final double delta = expectedTolerance;
        final RealVector presolvedX = MatrixUtils
                .createRealVector(lpPresolver.presolve(expectedSolution));
        final RealVector postsolvedX = MatrixUtils.createRealVector(lpPresolver.postsolve(presolvedX
                .toArray()));
        final double value = MatrixUtils.createRealVector(c).dotProduct(postsolvedX);
        assertEquals(expectedValue, value, delta);

        // check postsolved constraints
        for (int i = 0; i < lb.length; i++) {
            final double di;
            if (Double.isNaN(lb[i])){
                di = -Double.MAX_VALUE;
            }else {
                di = lb[i];
            }
            assertTrue(di <= postsolvedX.getEntry(i) + delta);
        }
        for (int i = 0; i < ub.length; i++) {
            final double di;
            if (Double.isNaN(ub[i])){
                di = Double.MAX_VALUE;
            }else {
                di = ub[i];
            }
            assertTrue(di + delta >= postsolvedX.getEntry(i));
        }
        RealVector axmb = aMatrix.operate(postsolvedX).subtract(MatrixUtils.createRealVector(b));
        assertEquals(0., axmb.getNorm(), expectedTolerance);

        // check presolved constraints
        assertEquals(presolvedLb.length, presolvedX.getDimension());
        assertEquals(presolvedUb.length, presolvedX.getDimension());
        aMatrix = MatrixUtils.createRealMatrix(presolvedA);
        final RealVector bvector = MatrixUtils.createRealVector(presolvedB);
        for (int i = 0; i < presolvedLb.length; i++) {
            final double di;
            if (Double.isNaN(presolvedLb[i])) {
                di = -Double.MAX_VALUE;
            }else {
                di = presolvedLb[i];
            }
            assertTrue(di <= presolvedX.getEntry(i) + delta);
        }
        for (int i = 0; i < presolvedUb.length; i++) {
            final double di;
            if (Double.isNaN(presolvedUb[i])) {
                di = Double.MAX_VALUE;
            }else {
                di = presolvedUb[i];
            }
            assertTrue(di + delta >= presolvedX.getEntry(i));
        }
        axmb = aMatrix.operate(presolvedX).subtract(bvector);
        assertEquals(0., axmb.getNorm(), expectedTolerance);

        // check rank(A): must be A pXn with rank(A)=p < n
        aMatrix = MatrixUtils.createRealMatrix(presolvedA);
        dec = new SingularValueDecomposition(aMatrix);
        rankA = dec.getRank();
        assertEquals(aMatrix.getRowDimension(), rankA);
        assertTrue(rankA < aMatrix.getColumnDimension());
    }
    
    /**
     * Test constructor LPPresolver
     * with non-acceptable lower bound -> it throws an exception
     */
    public void testLBNonAcceptable()   {
        final double lb = 0;
        final double ub = Double.NaN ;
        try{
            new LPPresolver(lb, ub);
        }catch (IllegalArgumentException e) {
            assertTrue(true);//ok, lb not acceptable
            return;
        }
        fail();
    }
    
    /**
     * Test constructor LPPresolver
     * with non-acceptable upper bound -> it throws an exception
     */
    public void testUBNonAcceptable()   {
        final double lb = Double.NaN;
        final double ub = 0;
        try{
            new LPPresolver(lb, ub);
        }catch (IllegalArgumentException e) {
            assertTrue(true);//ok, Ub not acceptable
            return;
        }
        fail();
    }
    
    /**
     * Test presolve with different dimensions on lb and up -> it throws an exception
     */
    public void testDimensionErrorUBLB()   {
 
        final double[] c = {};
        final double[][] a = {{0,2}, {2,2}};
        final double[] b = {};
        final double[] lb = {Double.NaN, 1};
        final double[] ub = {0};

        final LPPresolver lpPresolver = new LPPresolver();
        assertFalse(lpPresolver.isAvoidIncreaseSparsity());
        try{
            lpPresolver.presolve(c, a, b, lb, ub);;
        }catch (IllegalArgumentException e) {
            assertTrue(true);//ok, ub and lb dimensions mismatch
            return;
        }
        fail();
    }
    
    /**
     * Test presolve with ub < lp -> it throws an exception
     */
    public void testErrorUBLB()   {
 
        final double[] c = {};
        final double[][] a = {{0,2}, {2,2}};
        final double[] b = {};
        final double[] lb = {Double.NaN, 1};
        final double[] ub = {0, 0};

        final LPPresolver lpPresolver = new LPPresolver();
        try{
            lpPresolver.presolve(c, a, b, lb, ub);;
        }catch (PatriusRuntimeException e) {
            assertTrue(true);//ok, ub and lb dimensions mismatch
            return;
        }
        fail();
    }
    
    /**
     * Test presolve with 
     * dimensions vector x != dimensions N -> it throws an exception
     */
    public void testErrorDimensionsPresolve()   {
        final double[] x = {0, 0}; // dimension 2

        final LPPresolver lpPresolver = new LPPresolver();
        assertEquals(0,lpPresolver.getOriginalN());  //number of variables is 0
        assertEquals(0,lpPresolver.getOriginalMeq());  //number of equalities is 0
        assertEquals(-1,lpPresolver.getPresolvedMeq()); // non presolved number of variables 
        try{
            lpPresolver.presolve(x);;
        }catch (IllegalArgumentException e) {
            assertTrue(true);//ok, dimensions mismatch
            return;
        }
        fail();
    }
    
    /**
     * Test an infeasible problem: A is set to 0 and B not
     * the problem is infeasible -> it throws an exception
     * 
     * @throws IOException if an error occurs while reading.
     */
    public void testWithZeroA() throws IOException {

        final String problemId = "2";

        final double[] c = TestUtils.loadDoubleArrayFromFile(presolvingS + cS + problemId + txt);
        final double[][] a1 = TestUtils.loadDoubleMatrixFromFile(presolvingS + aS + problemId + txt, space.charAt(0));
        final double[][] a = new double[a1.length][a1.length]; //set a to 0
        final double[] b = TestUtils.loadDoubleArrayFromFile(presolvingS + bS + problemId + txt);

        final LPPresolver lpPresolver = new LPPresolver();
        try{
            lpPresolver.presolve(c, a, b, null, null); 
        }catch (PatriusRuntimeException e) {
            assertTrue(true);//ok, infeasible problem
            return;
        }
        fail();
    }
    
    /**
     * Test an infeasible problem: A and b are set to 0 and the problem is unbound
     * -> an exception is thrown
     */
    public void testUnboundedProblem() {        
        final double[] c = {1,2,3};
        final double[][] a = {{0,0,0},{0,0,0}};
        final double[] b = {0,0};

        final LPPresolver lpPresolver = new LPPresolver();
        try{
            lpPresolver.presolve(c, a, b, null, null); 
        }catch (PatriusRuntimeException e) {
            assertTrue(true);//ok, unbounded problem
            return;
        }
        fail();
    }
    
    /**
     * Test an infeasible problem: A and b are set to 0 and the problem is unbound on the ub
     * -> an exception is thrown
     */
    public void testUnboundedProblem2(){        
        final double[] c = {1,-2,3};
        final double[][] a = {{0,0,0},{0,0,0}};
        final double[] b = {0,0};
        final double[] lb = {0,0,0};

        final LPPresolver lpPresolver = new LPPresolver();
        try{
            lpPresolver.presolve(c, a, b, lb, null); 
        }catch (PatriusRuntimeException e) {
            assertTrue(true);//ok, unbounded problem
            return;
        }
        fail();
    }
    
    /**
     * Simple test:
     * A and b variables equal to 0
     * 
     */
    public void testUnboundedProblem3(){        
        final double[] c = {1,-2,3};
        final double[][] a = {{0,0,0},{0,0,0}};
        final double[] b = {0,0};
        final double[] lb = {0,0,0};
        final double[] ub = {1,1,1};
        final double[] expectedSolution = {0,1,0}; 

        final LPPresolver lpPresolver = new LPPresolver();
        lpPresolver.setAvoidFillIn(true);
        lpPresolver.presolve(c, a, b, lb, ub); 
        //final String string = lpPresolver.toString();
        final double[] sol = lpPresolver.postsolve(new double[] {});
        assertEquals(expectedSolution.length, sol.length);
        for (int i = 0; i < sol.length; i++) {
            assertEquals(expectedSolution[i], sol[i], 1.e-9);
        }

    }
    
    /**
     * Test an infeasible problem -> it throws an exception
     * 0 < x1 <0  x1=1 not possible!
     */
    public void testInfeasibleProb() throws IOException {
        final double[] c = {1,2,-3};
        final double[][] a = {{1,0,0},{1,-2,3}};  
        final double[] b = {1,-1};
        final double[] lb = {0,0,0};
        final double[] ub = {0, Double.NaN, Double.NaN};
        double s = 0;

        final LPPresolver lpPresolver = new LPPresolver();
        lpPresolver.setNOfSlackVariables((short) s);  
        try{
            lpPresolver.presolve(c, a, b, lb, ub); 
        }catch (PatriusRuntimeException e) {
            assertTrue(true);//ok, infeasible problem
            return;
        }
        fail(); 
    }
    
    /**
     * Test an infeasible problem -> it throws an exception
     * X needs negative values but the bounds are positive
     */
    public void testInfeasibleProb2()  {
        final double[] c = {1,2,-3};
        final double[][] a = {{1,0,0},{1,2,3}};  
        final double[] b = {0,-2};
        final double[] lb = {0,0,0};
        final double[] ub = {10, 10, 10};
        double s = 0;

        final LPPresolver lpPresolver = new LPPresolver();
        lpPresolver.setNOfSlackVariables((short) s);  
        try{
            lpPresolver.presolve(c, a, b, lb, ub); 
        }catch (PatriusRuntimeException e) {
            assertTrue(true);//ok, infeasible problem
            return;
        }
        fail(); 
    }
    
    /**
     * Test an infeasible problem -> x1=0 (a[0,0]*x[0] = b[0]) !=  x1 = 0 (a[2,0]*x[0] = b[2]), not possible!!
     * Exception in method removeSingletonRows -> (!isZero(xj - b.getEntry(k) / a.getEntry(k, j)))
     */
    public void testInfeasibleProb3() {
        final double[] c = {1,2,0};
        final double[][] a = {{1,0,0},{1,2,2},{1,0,0},{1,2,2}};  
        final double[] b = {10,5, 0, 2};
        final double[] lb = {0, 0, 0};
        double s = 0;
        
        final LPPresolver lpPresolver = new LPPresolver();
        lpPresolver.setNOfSlackVariables((short) s);  
        try{
            lpPresolver.presolve(c, a, b, lb, null); 
        }catch (PatriusRuntimeException e) {
            assertTrue(true);//ok, infeasible problem
            return;
        }
        fail();
    }
       
    
    /**
     * Infeasible problem: column singletons but unbounded on the upper bound
     * -> it throws an exception
     */
    public void testInfeasibleProb4()  {
        final double[] c = {-2,2,2,3};
        final double[][] a = {{-0.5,-0,2, 2},{-1,2,0, -4}};  
        final double[] b = {10,5};
        final double[] lb = {Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY,};
        double s = 0;

        final LPPresolver lpPresolver = new LPPresolver();
        lpPresolver.setNOfSlackVariables((short) s);  
        
        try{
            lpPresolver.presolve(c, a, b, lb, null); 
        }catch (PatriusRuntimeException e) {
            assertTrue(true);//ok, unbounded problem
            return;
        }
        fail();
    }
    
    
    
    /**
     * Deterministic problem with forcing constraints
     * to test some cases of removeForcingConstraints
     */
    public void testDeterministicProb() {
        final double[] c = {1,2,-3};
        final double[][] a = {{1,0,0},{-1,-2,3}};  
        final double[] b = {0,-20};
        final double[] lb = {0,0,0};
        final double[] ub = {10, 10, 10};
        double s = 0;
        final double[] expectedSolution = {0.0, 10.0, 0.0};

        final LPPresolver lpPresolver = new LPPresolver();
        lpPresolver.setNOfSlackVariables((short) s);  
        lpPresolver.presolve(c, a, b, lb, ub); 

        final int n = lpPresolver.getPresolvedN();

        // deterministic solution
        assertEquals(0, n);
        assertTrue(lpPresolver.getPresolvedC() == null);
        assertTrue(lpPresolver.getPresolvedA() == null);
        assertTrue(lpPresolver.getPresolvedB() == null);
        assertTrue(lpPresolver.getPresolvedLB() == null);
        assertTrue(lpPresolver.getPresolvedUB() == null);
        assertTrue(lpPresolver.getPresolvedYlb() == null);
        assertTrue(lpPresolver.getPresolvedYub() == null);
        assertTrue(lpPresolver.getPresolvedZlb() == null);
        assertTrue(lpPresolver.getPresolvedZub() == null);
        final double[] sol = lpPresolver.postsolve(new double[] {});
        assertEquals(expectedSolution.length, sol.length);
        for (int i = 0; i < sol.length; i++) {
            assertEquals(expectedSolution[i], sol[i], 1.e-9);
        }
    }
    
    /**
     * Deterministic problem with forcing constraints
     * to test some cases of removeForcingConstraints
     */
    public void testDeterministicProb2() {
        final double[] c = {1,2,-3};
        final double[][] a = {{1,0,0},{-1,2,3}};  
        final double[] b = {0,50};
        final double[] lb = {0,1,0};
        final double[] ub = {10, 10, 10};
        double s = 0;
        final double[] expectedSolution = {0.0, 10.0, 10.0};

        final LPPresolver lpPresolver = new LPPresolver();
        lpPresolver.setNOfSlackVariables((short) s);  
        lpPresolver.presolve(c, a, b, lb, ub); 
        
        final int n = lpPresolver.getPresolvedN();

        // deterministic solution
        assertEquals(0, n);
        assertTrue(lpPresolver.getPresolvedC() == null);
        assertTrue(lpPresolver.getPresolvedA() == null);
        assertTrue(lpPresolver.getPresolvedB() == null);
        assertTrue(lpPresolver.getPresolvedLB() == null);
        assertTrue(lpPresolver.getPresolvedUB() == null);
        assertTrue(lpPresolver.getPresolvedYlb() == null);
        assertTrue(lpPresolver.getPresolvedYub() == null);
        assertTrue(lpPresolver.getPresolvedZlb() == null);
        assertTrue(lpPresolver.getPresolvedZub() == null);
        final double[] sol = lpPresolver.postsolve(new double[] {});
        assertEquals(expectedSolution.length, sol.length);
        for (int i = 0; i < sol.length; i++) {
            assertEquals(expectedSolution[i], sol[i], 1.e-9);
        }
 
    }
    
    /**
     * Problem with singleton columns and unboundedUB
     * to test some lines of method checkColumnSingletons
     */
    public void testSingletonCol() {
        final double[] c = {1,2,-3};
        final double[][] a = {{1,0,0},{-1,2,3}};  
        final double[] b = {0,50};
        final double[] lb = {0,1,0};
        double s = 0;
        final double[] expectedSolution = {0.0, 1.0, 16.0};

        final LPPresolver lpPresolver = new LPPresolver();
        lpPresolver.setNOfSlackVariables((short) s);  
        lpPresolver.presolve(c, a, b, lb, null); 
        

        assertTrue(lpPresolver.getPresolvedC() == null);
        assertTrue(lpPresolver.getPresolvedA() == null);
        assertTrue(lpPresolver.getPresolvedB() == null);
        assertTrue(lpPresolver.getPresolvedLB() == null);
        assertTrue(lpPresolver.getPresolvedUB() == null);
        assertTrue(lpPresolver.getPresolvedYlb() == null);
        assertTrue(lpPresolver.getPresolvedYub() == null);
        assertTrue(lpPresolver.getPresolvedZlb() == null);
        assertTrue(lpPresolver.getPresolvedZub() == null);
        final double[] sol = lpPresolver.postsolve(new double[] {});
        assertEquals(expectedSolution.length, sol.length);
        for (int i = 0; i < sol.length; i++) {
            assertEquals(expectedSolution[i], sol[i], 1.e-9);
        }
    }
    
    /**
     * Problem with singleton columns and unboundedLB
     * to test some lines of method checkColumnSingletons
     */
    public void testSingletonCol2() {
        final double[] c = {1,2,-3};
        final double[][] a = {{1,0,2},{1,2,3}};  
        final double[] b = {10,5};
        final double[] ub = {10, 10, 10};
        double s = 0;
        final double[] expectedSolution = {10.0, -2.5, 0.0};

        final LPPresolver lpPresolver = new LPPresolver();
        lpPresolver.setNOfSlackVariables((short) s);  
        lpPresolver.presolve(c, a, b, null, ub); 
        final double[] sol = lpPresolver.postsolve(new double[] {});
        assertEquals(expectedSolution.length, sol.length);
        for (int i = 0; i < sol.length; i++) {
            assertEquals(expectedSolution[i], sol[i], 1.e-9);
        }

    }
    
    /**
     * Problem with singleton columns and unboundedLB
     * to test some lines of method checkColumnSingletons
     */
    public void testSingletonCol3() {
        final double[] c = {1,2,-3};
        final double[][] a = {{1,0,-2},{1,2,0}};  
        final double[] b = {10,5};
        final double[] ub = {10, 10, 10};
        double s = 0;
        final double[] expectedSolution = {10.0, -2.5, 0.0};
        
        final LPPresolver lpPresolver = new LPPresolver();
        lpPresolver.setNOfSlackVariables((short) s);  
        lpPresolver.presolve(c, a, b, null, ub); 
        final double[] sol = lpPresolver.postsolve(new double[] {});
        assertEquals(expectedSolution.length, sol.length);
        for (int i = 0; i < sol.length; i++) {
            assertEquals(expectedSolution[i], sol[i], 1.e-9);
        }

    }
    
    /**
     * Problem with singleton columns and unboundedLB
     * to test some lines of method checkColumnSingletons
     */
    public void testSingletonCol4() {
        final double[] c = {1,2,3};
        final double[][] a = {{1,0,-2},{1,2,0}};  
        final double[] b = {10,5};
        final double[] ub = {10, 10, 10};
        double s = 0;
        final double[] expectedSolution = {-15, 10.0, -12.5};
        
        final LPPresolver lpPresolver = new LPPresolver();
        lpPresolver.setNOfSlackVariables((short) s);  
        lpPresolver.presolve(c, a, b, null, ub); 
        final double[] sol = lpPresolver.postsolve(new double[] {});
        assertEquals(expectedSolution.length, sol.length);
        for (int i = 0; i < sol.length; i++) {
            assertEquals(expectedSolution[i], sol[i], 1.e-9);
        }

    }
    
    /**
     * Problem with singleton columns and unboundedLB
     * to test some lines of method checkColumnSingletons
     * The solution to the given problem is unbounded.
     */
    public void testSingletonCol5(){
        final double[] c = {1,2,-3};
        final double[][] a = {{1,0,-2},{1,2,0}};  
        final double[] b = {10,5};
        final double[] expectedSolution = {Double.NaN,Double.NaN,Double.NaN};
        double s = 0;

        final LPPresolver lpPresolver = new LPPresolver();
        lpPresolver.setNOfSlackVariables((short) s);  
        lpPresolver.presolve(c, a, b, null, null); 
        assertTrue(lpPresolver.getPresolvedC() == null);
        assertTrue(lpPresolver.getPresolvedA() == null);
        assertTrue(lpPresolver.getPresolvedB() == null);
        assertTrue(lpPresolver.getPresolvedLB() == null);
        assertTrue(lpPresolver.getPresolvedUB() == null);
        assertTrue(lpPresolver.getPresolvedYlb() == null);
        assertTrue(lpPresolver.getPresolvedYub() == null);
        assertTrue(lpPresolver.getPresolvedZlb() == null);
        assertTrue(lpPresolver.getPresolvedZub() == null);
        final double[] sol = lpPresolver.postsolve(new double[] {});
        assertEquals(expectedSolution.length, sol.length);
        for (int i = 0; i < sol.length; i++) {
            assertEquals(expectedSolution[i], sol[i], 1.e-4);
        }

    }
    /**
     * Problem with singleton columns and unboundedLB
     * to test some lines of method checkColumnSingletons
     * The solution to the given problem is unbounded.
     */
    public void testSingletonCol6()  {
        final double[] c = {1,2,3};
        final double[][] a = {{1,0,-2},{1,2,0}};  
        final double[] b = {10,5};
        final double[] expectedSolution = {Double.NaN,Double.NaN,Double.NaN};
        double s = 0;

        final LPPresolver lpPresolver = new LPPresolver();
        lpPresolver.setNOfSlackVariables((short) s);  
        lpPresolver.presolve(c, a, b, null, null); 
        assertTrue(lpPresolver.getPresolvedC() == null);
        assertTrue(lpPresolver.getPresolvedA() == null);
        assertTrue(lpPresolver.getPresolvedB() == null);
        assertTrue(lpPresolver.getPresolvedLB() == null);
        assertTrue(lpPresolver.getPresolvedUB() == null);
        assertTrue(lpPresolver.getPresolvedYlb() == null);
        assertTrue(lpPresolver.getPresolvedYub() == null);
        assertTrue(lpPresolver.getPresolvedZlb() == null);
        assertTrue(lpPresolver.getPresolvedZub() == null);
        final double[] sol = lpPresolver.postsolve(new double[] {});
        assertEquals(expectedSolution.length, sol.length);
        for (int i = 0; i < sol.length; i++) {
            assertEquals(expectedSolution[i], sol[i], 1.e-4);
        }

    }
    
    /**
     * Problem with incorrect lower bounds for the given expected solution
     * To test that the method checkProgress throws an exception
     */
    public void testCheckProgress() {
        final double[] c = {1,2,-3};
        final double[][] a = {{1,0,0},{-1,2,3}};  
        final double[] b = {0,50};
        final double[] lb = {1,0,0};
        double s = 0;
        final double[] expectedSolution = {0.0, 1.0, 16.0};

        final LPPresolver lpPresolver = new LPPresolver();
        lpPresolver.setNOfSlackVariables((short) s);  
        lpPresolver.setExpectedSolution(expectedSolution);
        
        try{
            lpPresolver.presolve(c, a, b, lb, null); 
        }catch (IllegalStateException e) {
            assertTrue(true);//ok, lb not acceptable with provided expected solution
            return;
        }
        fail();
    }
    
    /**
     * Problem with incorrect upper bounds for the given expected solution
     * To test that the method checkProgress throws an exception
     */
    public void testCheckProgress2() {
        final double[] c = {1,2,-3};
        final double[][] a = {{1,0,0},{-1,2,3}};  
        final double[] b = {0,50};
        final double[] ub = {1,2,1};
        double s = 0;
        final double[] expectedSolution = {0, 1.0, 16.0};

        final LPPresolver lpPresolver = new LPPresolver();
        lpPresolver.setNOfSlackVariables((short) s);  
        lpPresolver.setExpectedSolution(expectedSolution);
        
        try{
            lpPresolver.presolve(c, a, b, null, ub); 
        }catch (IllegalStateException e) {
            assertTrue(true);//ok, ub not acceptable with provided expected solution
            return;
        }
        fail();
    }
    
   
    /**
     * Problem with duplicate columns 
     */
    public void testDuplicateColumns()  {
        final double[] c = {1,3,-1.5,15};
        final double[][] a = {{1,4,-2,8},{35,1,-0.5,2},{2,8,-4,16}};
        final double[] b = {10,10,20};
        final double[] lb = {0,Double.NaN,0,0};
        //final double[] ub = {10,1,23};
        final double[] expectedSolution = {0.2158, 2.4460, 0.0, 0.0};
        final double expectedValue = 7.5540;
        double s = 1;

        final LPPresolver lpPresolver = new LPPresolver();
        lpPresolver.setNOfSlackVariables((short) s);  
        lpPresolver.presolve(c, a, b, lb, null); 
        final double[] sol = lpPresolver.postsolve(new double[] {});
        
        assertEquals(expectedSolution.length, sol.length);
        for (int i = 0; i < sol.length; i++) {
            assertEquals(expectedSolution[i], sol[i], 1.e-4);
        }

        // check objective function
        final RealVector presolvedX = MatrixUtils
                .createRealVector(lpPresolver.presolve(expectedSolution));
        final RealVector postsolvedX = MatrixUtils.createRealVector(lpPresolver.postsolve(presolvedX
                .toArray()));
        final double value = MatrixUtils.createRealVector(c).dotProduct(postsolvedX);
        assertEquals(expectedValue, value, 1e-4);
        
    }
    
    /**
     * Problem with duplicate columns 
     */
    public void testDuplicateColumns2()  {
        final double[] c = {1,3,-1.5,15};
        final double[][] a = {{1,4,-2,8},{35,1,-0.5,2},{2,8,-4,16}};
        final double[] b = {10,10,20};
        final double[] lb = {0,0,Double.NaN,0};
        //final double[] ub = {10,1,23};
        final double[] expectedSolution = {0.2158, 0.0, -4.8920, 0.0};
        final double expectedValue = 7.5540;
        double s = 1;

        final LPPresolver lpPresolver = new LPPresolver();
        lpPresolver.setNOfSlackVariables((short) s);  
        lpPresolver.presolve(c, a, b, lb, null); 
        final double[] sol = lpPresolver.postsolve(new double[] {});
        
        assertEquals(expectedSolution.length, sol.length);
        for (int i = 0; i < sol.length; i++) {
            assertEquals(expectedSolution[i], sol[i], 1.e-4);
        }

        // check objective function
        final RealVector presolvedX = MatrixUtils
                .createRealVector(lpPresolver.presolve(expectedSolution));
        final RealVector postsolvedX = MatrixUtils.createRealVector(lpPresolver.postsolve(presolvedX
                .toArray()));
        final double value = MatrixUtils.createRealVector(c).dotProduct(postsolvedX);
        assertEquals(expectedValue, value, 1e-4);
        
    }
    
    /**
     * Problem with doubleton rows
     * Expected solution computed from LinearProgramming Solver (www.cbom.atozmath.com)
     */
    public void testDoubletonRow()  {
        final double[] c = {1,3,1.5,-10};
        final double[][] a = {{10,0,0,-1},{35,1,0.5,2},{20,8,4,2}};
        final double[] b = {10,10,23};
        final double[] lb = {0,0,0,Double.NaN};
        final double[] ub = {100,100,100,100};
        final double[] expectedSolution = {0.4924, 1.4562, 2.9125, -5.075};
        final double expectedValue = 59.98;
        double s = 0;

        final LPPresolver lpPresolver = new LPPresolver();
        lpPresolver.setNOfSlackVariables((short) s);  
        lpPresolver.presolve(c, a, b, lb, ub); 
        final double[] sol = lpPresolver.postsolve(new double[] {});
        assertEquals(expectedSolution.length, sol.length);
        for (int i = 0; i < sol.length; i++) {
            assertEquals(expectedSolution[i], sol[i], 1.e-4);
        }

        // check objective function
        final RealVector presolvedX = MatrixUtils
                .createRealVector(lpPresolver.presolve(expectedSolution));
        final RealVector postsolvedX = MatrixUtils.createRealVector(lpPresolver.postsolve(presolvedX
                .toArray()));
        final double value = MatrixUtils.createRealVector(c).dotProduct(postsolvedX);
        assertEquals(expectedValue, value, 1e-4);
    }
    
    /**
     * Problem with doubleton rows
     * Expected solution computed from LinearProgramming Solver (www.comnuan.com)
     */
    public void testDoubletonRow2()  {
        final double[] c = {1,3,-1,15};
        final double[][] a = {{1,0,0,8},{35,1,1,2},{2,8,4,16}};
        final double[] b = {10,10,23};
        final double[] lb = {0,0,0,Double.NaN};
        final double[] expectedSolution = {0.1942, 0.0, 0.75, 1.2257};
        final double expectedValue = 17.83;
        double s = 0;

        final LPPresolver lpPresolver = new LPPresolver();
        lpPresolver.setNOfSlackVariables((short) s);  
        lpPresolver.presolve(c, a, b, lb, null);         
        final int n = lpPresolver.getPresolvedN();

        // deterministic solution
        assertEquals(0, n);
        assertTrue(lpPresolver.getPresolvedC() == null);
        assertTrue(lpPresolver.getPresolvedA() == null);
        assertTrue(lpPresolver.getPresolvedB() == null);
        assertTrue(lpPresolver.getPresolvedLB() == null);
        assertTrue(lpPresolver.getPresolvedUB() == null);
        assertTrue(lpPresolver.getPresolvedYlb() == null);
        assertTrue(lpPresolver.getPresolvedYub() == null);
        assertTrue(lpPresolver.getPresolvedZlb() == null);
        assertTrue(lpPresolver.getPresolvedZub() == null);
        final double[] sol = lpPresolver.postsolve(new double[] {});
        assertEquals(expectedSolution.length, sol.length);
        for (int i = 0; i < sol.length; i++) {
            assertEquals(expectedSolution[i], sol[i], 1.e-4);
        }

        // check objective function
        final RealVector presolvedX = MatrixUtils
                .createRealVector(lpPresolver.presolve(expectedSolution));
        final RealVector postsolvedX = MatrixUtils.createRealVector(lpPresolver.postsolve(presolvedX
                .toArray()));
        final double value = MatrixUtils.createRealVector(c).dotProduct(postsolvedX);
        assertEquals(expectedValue, value, 1e-4);
    }
    
    /**
     * This test involves column singletons and dominated columns
     * 
     * Expected solution and value computed with calculator www.cbom.atozmath.com
     */
    public void testDominatedCol()  {
        final double[] c = {-2,2,2,3};
        final double[][] a = {{-0.5,0,2, 2},{-1,2,0, -4}};  
        final double[] b = {10,5};
        final double[] lb = {Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY,};
        final double[] ub = {100, Double.NaN,Double.NaN,Double.NaN,};
        double s = 0;
        final double[] expectedSolution = {100.0, 52.5, 30.0,0.0};
        final double expectedValue = -35;

        final LPPresolver lpPresolver = new LPPresolver();
        lpPresolver.setNOfSlackVariables((short) s);  

        lpPresolver.presolve(c, a, b, lb, ub); 
        final double[] sol = lpPresolver.postsolve(new double[] {});
        assertEquals(expectedSolution.length, sol.length);
        for (int i = 0; i < sol.length; i++) {
            assertEquals(expectedSolution[i], sol[i]);
        }
        // check objective function
        final RealVector presolvedX = MatrixUtils
                .createRealVector(lpPresolver.presolve(expectedSolution));
        final RealVector postsolvedX = MatrixUtils.createRealVector(lpPresolver.postsolve(presolvedX
                .toArray()));
        final double value = MatrixUtils.createRealVector(c).dotProduct(postsolvedX);
        assertEquals(expectedValue, value);

    }
    
    /**
     * This test involves column singletons and dominated columns
     * 
     * Expected solution and value computed with calculator www.cbom.atozmath.com
     */
    public void testDominatedCol2()  {
        final double[] c = {-1.5,2,2,3};
        final double[][] a = {{-0.5,0,2, 2},{-1,2,0, -4}};  
        final double[] b = {10,5};
        final double[] lb = {Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY,};
        final double[] ub = {100, Double.NaN,Double.NaN,Double.NaN,};
        double s = 0;
        final double[] expectedSolution = {100.0, 52.5, 30.0,0.0};
        final double expectedValue = 15;

        final LPPresolver lpPresolver = new LPPresolver();
        lpPresolver.setNOfSlackVariables((short) s);  
        
        lpPresolver.presolve(c, a, b, lb, ub); 
        final double[] sol = lpPresolver.postsolve(new double[] {});
        assertEquals(expectedSolution.length, sol.length);
        for (int i = 0; i < sol.length; i++) {
            assertEquals(expectedSolution[i], sol[i]);
        }
        // check objective function
        final RealVector presolvedX = MatrixUtils
                .createRealVector(lpPresolver.presolve(expectedSolution));
        final RealVector postsolvedX = MatrixUtils.createRealVector(lpPresolver.postsolve(presolvedX
                .toArray()));
        final double value = MatrixUtils.createRealVector(c).dotProduct(postsolvedX);
        assertEquals(expectedValue, value);

    }
    
    /**
     * This test involves a problem that does not progress
     * It throws an exception for norm > tolerance
     */
    public void testCheckProgressError()  {
        final double[] c = {-1.5,3,2,2,3};
        final double[][] a = {{-0.5,1,0,2, 2},{-1,2,2,0, -4}};  
        final double[] b = {10,5};
        final double[] lb = {0,0,0,0,0};
        double s = 0;
        final double[] expectedSolution = {0.0, 2.5, 0.0,3.75,0.0};
        final double expectedValue = 15;
        final double expectedTolerance = MatrixUtils.createRealMatrix(a)
                        .operate(MatrixUtils.createRealVector(expectedSolution))
                        .subtract(MatrixUtils.createRealVector(b)).getNorm();

        final LPPresolver lpPresolver = new LPPresolver();
        lpPresolver.setNOfSlackVariables((short) s);  
        try{
            doPresolving(c, a, b, lb, null, s, expectedSolution, expectedValue, expectedTolerance);
        }catch (IllegalStateException e) {
            assertTrue(true);//ok, norm > tolerance
            return;
        }
        fail();

    }
    
    /**
     * This test involves dominated columns with unbounded lower bound
     * It throws an exception
     */
    public void testDominatedColError()  {
        final double[] c = {2,2,2,3};
        final double[][] a = {{0.5,0,2, 2},{1,2,0, -4}};  
        final double[] b = {10,5};
        final double[] lb = {Double.NaN, Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY};
        final double[] ub = {100, Double.NaN,Double.NaN,Double.NaN};
        double s = 0;

        final LPPresolver lpPresolver = new LPPresolver();
        lpPresolver.setNOfSlackVariables((short) s);  

        try{
            lpPresolver.presolve(c, a, b, lb, ub); 
        }catch (PatriusRuntimeException e) {
            assertTrue(true);//ok, unbounded problem
            return;
        }
        fail();
    }
    
    /**
     * This test involves column singletons and dominated columns
     * 
     * Expected solution and value computed with calculator www.cbom.atozmath.com
     */
    public void xxxtestDominatedColTEST()  {
        final double[] c = {-2,2,2,3};
        final double[][] a = {{-0.5,0,2, 2},{-1,-2,0, -4}};  
        final double[] b = {10,5};
        final double[] lb = {0,Double.NaN,0,0};
        final double[] ub = {100, 100,100,100};
        double s = 0;
        final double[] expectedSolution = {100.0, 52.5, 30.0,0.0};
        final double expectedValue = -35;

        final LPPresolver lpPresolver = new LPPresolver();
        lpPresolver.setNOfSlackVariables((short) s);  

        lpPresolver.presolve(c, a, b, lb, ub); 
        final double[] sol = lpPresolver.postsolve(new double[] {});
        assertEquals(expectedSolution.length, sol.length);
        for (int i = 0; i < sol.length; i++) {
            assertEquals(expectedSolution[i], sol[i]);
        }
        // check objective function
        final RealVector presolvedX = MatrixUtils
                .createRealVector(lpPresolver.presolve(expectedSolution));
        final RealVector postsolvedX = MatrixUtils.createRealVector(lpPresolver.postsolve(presolvedX
                .toArray()));
        final double value = MatrixUtils.createRealVector(c).dotProduct(postsolvedX);
        assertEquals(expectedValue, value);

    }
}
