/**
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
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
package fr.cnes.sirius.patrius.math.optim.joptimizer.solvers;

import junit.framework.TestCase;
import fr.cnes.sirius.patrius.math.linear.ArrayRealVector;
import fr.cnes.sirius.patrius.math.linear.BlockRealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealVector;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * @author alberto trivellato (alberto.trivellato@gmail.com)
 */
public class BasicKKTSolverTest extends TestCase {

    /**
     * Test solver simple
     * @throws PatriusException if an error occurs
     */
    public void testSolveSimple() throws PatriusException {
        final double[][] hMatrix = new double[][] { { 3 } };
        final double[][] aMatrix = new double[][] { { 2 } };
        final RealMatrix matH = new BlockRealMatrix(hMatrix);
        final RealMatrix matA = new BlockRealMatrix(aMatrix);
        final RealMatrix matAT = matA.copy().transpose();
        final RealVector g = new ArrayRealVector(1, -3);
        final RealVector h = new ArrayRealVector(1, 0);

        final AbstractKKTSolver solver = new BasicKKTSolver();
        solver.setHMatrix(matH);
        solver.setAMatrix(matA);
        solver.setGVector(g);
        solver.setHVector(h);
        solver.setCheckKKTSolutionAccuracy(true);
        final RealVector[] sol = solver.solve();
        final RealVector v = sol[0];
        final RealVector w = sol[1];

        final RealVector a = matH.operate(v).add(matAT.operate(w)).add(g);
        final RealVector b = matA.operate(v).add(h);
        for (int i = 0; i < a.getDimension(); i++) {
            assertEquals(0, a.getEntry(i), 1.E-14);
        }
        for (int i = 0; i < b.getDimension(); i++) {
            assertEquals(0, b.getEntry(i), 1.E-14);
        }
    }

    /**
     * Test solver 2
     * @throws PatriusException if an error occurs
     */
    public void testSolve2() throws PatriusException {
        final double[][] hMatrix = new double[][] { { 1.68, 0.34, 0.38 }, { 0.34, 3.09, -1.59 },
                { 0.38, -1.59, 1.54 } };
        final double[][] aMatrix = new double[][] { { 1, 2, 3 } };
        final RealMatrix matH = new BlockRealMatrix(hMatrix);
        final RealMatrix matA = new BlockRealMatrix(aMatrix);
        final RealMatrix matAT = matA.copy().transpose();
        final RealVector g = new ArrayRealVector(new double[] { 2, 5, 1 });
        final RealVector h = new ArrayRealVector(new double[] { 1 });

        final AbstractKKTSolver solver = new BasicKKTSolver();
        solver.setHMatrix(matH);
        solver.setAMatrix(matA);
        solver.setGVector(g);
        solver.setHVector(h);
        final RealVector[] sol = solver.solve();
        final RealVector v = sol[0];
        final RealVector w = sol[1];

        final RealVector a = matH.operate(v).add(matAT.operate(w)).add(g);
        final RealVector b = matA.operate(v).add(h);
        for (int i = 0; i < a.getDimension(); i++) {
            assertEquals(0, a.getEntry(i), 1.E-14);
        }
        for (int i = 0; i < b.getDimension(); i++) {
            assertEquals(0, b.getEntry(i), 1.E-14);
        }
    }
    
    /**
     * Test solver 2 with checkSolutionAccuracy -> solution KTT failed, 
     * tolerance not respected (small difference)
     */
    public void testSolve2CheckAccuracy(){
        final double[][] hMatrix = new double[][] { { 1.68, 0.34, 0.38 }, { 0.34, 3.09, -1.59 },
                { 0.38, -1.59, 1.54 } };
        final double[][] aMatrix = new double[][] { { 1, 2, 3 } };
        final RealMatrix matH = new BlockRealMatrix(hMatrix);
        final RealMatrix matA = new BlockRealMatrix(aMatrix);
        final RealVector g = new ArrayRealVector(new double[] { 2, 5, 1 });
        final RealVector h = new ArrayRealVector(new double[] { 1 });

        final AbstractKKTSolver solver = new BasicKKTSolver();
        solver.setHMatrix(matH);
        solver.setAMatrix(matA);
        solver.setGVector(g);
        solver.setHVector(h);
        solver.setCheckKKTSolutionAccuracy(true);
        try{
            solver.solve();
        }catch (PatriusException e) {
            assertTrue(true);//ok, checkSolutionAccuracy not passed
            return;
        }
        fail();
    }
    
    /**
     * Test solver with
     * H matrix non square & matrix A null -> it throws an exception
     * @throws PatriusException if an error occurs
     */
    public void testSolveError() throws PatriusException {
        final double[][] hMatrix = new double[][] { { 3, 2, 1 }, { 3, 2, 4 } };
        final RealMatrix matH = new BlockRealMatrix(hMatrix);
        final RealVector g = new ArrayRealVector(1, -3);
        final RealVector h = new ArrayRealVector(1, 0);

        final AbstractKKTSolver solver = new BasicKKTSolver();
        solver.setHMatrix(matH);
        solver.setGVector(g);
        solver.setHVector(h);
        try{
            solver.solve();
        }catch (PatriusException e) {
            assertTrue(true);//ok, A matrix null -> KTT solution failed
            return;
        }
        fail();
    }
       
    /**
     * Test solver AugmentedKKTSolver
     * matrix A null -> it throws an exception
     * @throws PatriusException if an error occurs
     */
    public void testAugmentedSolveError() throws PatriusException {
        final AugmentedKKTSolver solver = new AugmentedKKTSolver();
        solver.setS(1);
        try{
            solver.solve();
        }catch (IllegalStateException e) {
            assertTrue(true);//ok, A matrix null 
            return;
        }
        fail();
    }
    
    /**
     * Test solver AugmentedKKTSolver
     * KKT matrix is non-singular -> it throws an exception
     */
    public void testAugmentedSolveError2(){
        final double[][] hMatrix = new double[][] { { -1.68, -10, -0.38 }, { -0.34, 3, -1.59 },
                { 0.38, -1.59, 1.54 } };
        final double[][] aMatrix = new double[][] { { 1, -22, 3 } };
        final RealMatrix matH = new BlockRealMatrix(hMatrix);
        final RealMatrix matA = new BlockRealMatrix(aMatrix);
        final RealVector g = new ArrayRealVector(new double[] { -2, 5, 1 });
        final RealVector h = new ArrayRealVector(new double[] { 1 });

        final AugmentedKKTSolver solver = new AugmentedKKTSolver();
        solver.setHMatrix(matH);
        solver.setAMatrix(matA);
        solver.setGVector(g);
        solver.setHVector(h);
        
        try{
            solver.solve();
        }catch (PatriusException e) {
            assertTrue(true);//ok, A matrix null 
            return;
        }
        fail();
    }
    
    /**
     * Test solve UpperDiagonalHKKTSolver with null h
     * 
     * @throws PatriusException if an error occurs
     */
    public void testUpperDiagonalHKKTSolver() throws PatriusException {
        final double[][] hMatrix = new double[][] { { 3 } };
        final double[][] aMatrix = new double[][] { { 2 } };
        final RealMatrix matH = new BlockRealMatrix(hMatrix);
        final RealMatrix matA = new BlockRealMatrix(aMatrix);
        final RealVector g = new ArrayRealVector(1, -3);
        
        final UpperDiagonalHKKTSolver solver = new UpperDiagonalHKKTSolver(1);
        solver.setDiagonalLength(1);
        assertEquals(1, solver.getDiagonalLength());
        solver.setHMatrix(matH);
        solver.setAMatrix(matA);
        solver.setGVector(g);
        
        final RealVector[] sol = solver.solve();
        final RealVector v = sol[0];
        final RealVector w = sol[1];
        final RealVector vVector = new ArrayRealVector(v);
        final RealVector wVector = new ArrayRealVector(w);
        final boolean res = solver.checkKKTSolutionAccuracy(vVector, wVector);
        assertTrue(res);

        final RealVector a = matH.operate(v).add(matA.transpose().operate(w)).add(g);
        final RealVector b = matA.operate(v);
        for (int i = 0; i < a.getDimension(); i++) {
            assertEquals(0, a.getEntry(i), 1.E-14);
        }
        for (int i = 0; i < b.getDimension(); i++) {
            assertEquals(0, b.getEntry(i), 1.E-14);
        }
    }
       
    /**
     * Test solver UpperDiagoanlHKKTSolver
     * with non accepted solution accuracy -> it throws an exception 
     */
    public void testUpperDiagonalHKKTError()  {
        final double[][] hMatrix = new double[][] { {1,0}, {1,1 }};
        final double[][] aMatrix = new double[][] { {1,1}, {5,0} };
        final RealMatrix matH = new BlockRealMatrix(hMatrix);
        final RealMatrix matA = new BlockRealMatrix(aMatrix);
        final double[] g = new double[] {3,0};
        final double[] h = new double[] {0,0};
        final RealVector gVector = new ArrayRealVector(g);
        final RealVector hVector = new ArrayRealVector(h);

        final UpperDiagonalHKKTSolver solver = new UpperDiagonalHKKTSolver(2);
        solver.setHMatrix(matH);
        solver.setAMatrix(matA);
        solver.setGVector(gVector);
        solver.setHVector(hVector);
        solver.setCheckKKTSolutionAccuracy(true);
        try{
            solver.solve();
        }catch (PatriusException e) {
            assertTrue(true);//ok, solution accuracy checking not passed
            return;
        }
        fail();
    }
    
    /**
     * Test solve UpperDiagonalHKKTSolver with a not feasible problem, 
     * not possible to decompose either by the augmented KKT -> it throws an exception
     */
    public void testUpperDiagonalHKKTError2(){      
        final double[][] hMatrix = new double[][] { {1001,0,0,0}, {0,1001,0,0}, {0,0,1001,0}, {999,999,999,4003} };
        final double[][] aMatrix = new double[][] { { 10,-1000,78,-500 }, { 10,-1000,78,-500 } };
        final double[] g = new double[] {-42914.2286285, 42.9142286285, -42.9142286285, -85.0430284002};
        final double[] h = new double[] {0,0.001};
        final RealMatrix matH = new BlockRealMatrix(hMatrix);
        final RealMatrix matA = new BlockRealMatrix(aMatrix);
        final RealVector gVector = new ArrayRealVector(g);
        final RealVector hVector = new ArrayRealVector(h);
        
        final UpperDiagonalHKKTSolver solver = new UpperDiagonalHKKTSolver(1);
        solver.setDiagonalLength(1);
        assertEquals(1, solver.getDiagonalLength());
        
        solver.setHMatrix(matH);
        solver.setAMatrix(matA);
        solver.setGVector(gVector);
        solver.setHVector(hVector);

        try{
            solver.solve();
        }catch (PatriusException e) {
            assertTrue(true);//ok, not possible to factorize
            return;
        }
        fail();
    }
    
}