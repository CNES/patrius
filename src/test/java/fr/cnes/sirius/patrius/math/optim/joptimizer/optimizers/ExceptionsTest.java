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

import fr.cnes.sirius.patrius.math.linear.ArrayRealVector;
import fr.cnes.sirius.patrius.math.linear.BlockRealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealVector;
import fr.cnes.sirius.patrius.math.optim.joptimizer.functions.PDQuadraticMultivariateRealFunction;
import fr.cnes.sirius.patrius.math.optim.joptimizer.functions.PSDQuadraticMultivariateRealFunction;
import fr.cnes.sirius.patrius.math.optim.joptimizer.functions.QuadraticMultivariateRealFunction;
import fr.cnes.sirius.patrius.math.optim.joptimizer.optimizers.LPOptimizationRequest;
import fr.cnes.sirius.patrius.math.optim.joptimizer.optimizers.NewtonUnconstrained;
import fr.cnes.sirius.patrius.math.optim.joptimizer.optimizers.OptimizationRequest;
import fr.cnes.sirius.patrius.math.optim.joptimizer.optimizers.OptimizationRequestHandler;
import fr.cnes.sirius.patrius.math.optim.joptimizer.solvers.UpperDiagonalHKKTSolver;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusRuntimeException;
import junit.framework.TestCase;

/**
 * 
 * @author bdalfoferrer
* HISTORY
* VERSION:4.6:DM:DM-2591:27/01/2021:[PATRIUS] Intigration et validation JOptimizer
* END-HISTORY
 *
 */
public class ExceptionsTest extends TestCase {

    /**
     * Test PSDQuadraticMultivariateRealFunction
     * with negative eigenvalues -> it throws an exception
     * 
     * @throws IllegalArgumentException
     */
    public void testErrorPSDQuadraticMultivariateRealFunction() throws IllegalArgumentException {
        final double[][] pMatrix = { { 2, 0, -1 }, { -10, 0, -4 } , { -10, -2, -4 }};
        final double[] qVector = { 0, -1 , 2};
        try{
            new PSDQuadraticMultivariateRealFunction(pMatrix, qVector, 0, true);
        }catch (IllegalArgumentException e) {
            assertTrue(true);//ok, not positive semi-definite matrix
            return;
        }
        fail();
    }
    
    /**
     * Test PDQuadraticMultivariateRealFunction
     * with non symmetric and non positive matrix -> it throws an exception
     * 
     * @throws IllegalArgumentException
     */
    public void testErrorPDQuadraticMultivariateRealFunction() throws IllegalArgumentException {
        final double[][] pMatrix = { { 2, 0, -1 }, { 0, -1, -0.5 } , { -1, -0.5, -4 }};
        final double[] qVector = { 0, -1 , 2};
        try{
            new PDQuadraticMultivariateRealFunction(pMatrix, qVector, 0, true);
        }catch (IllegalArgumentException e) {
            assertTrue(true);//ok, not positive semi-definite matrix
            return;
        }
        fail();
    }
    
    /**
     * Test QuadraticMultivariateRealFunction
     * with null matrix -> it throws an exception
     * 
     * @throws IllegalArgumentException
     */
    public void testErrorQuadraticMultivariateRealFunction() throws IllegalArgumentException {
        final double[][] pMatrix = null;
        final double[] qVector = null;
        final double r = 2;
        try{
            new QuadraticMultivariateRealFunction(pMatrix, qVector, r, true);
        }catch (IllegalArgumentException e) {
            assertTrue(true);//ok, p is null -> Impossible to create the function
            return;
        }
        fail();
    }
    
    /**
     * Test QuadraticMultivariateRealFunction
     * with non-square matrix -> it throws an exception
     * 
     * @throws IllegalArgumentException
     */
    public void testErrorQuadraticMultivariateRealFunction2() throws IllegalArgumentException {
        final double[][] pMatrix = { { 2, 0, -1 }, { -10, 0, -4 } };
        final double[] qVector = new double[] { 0, -1 };
        final double r = 2;
        try{
            new QuadraticMultivariateRealFunction(pMatrix, qVector, r, true);
        }catch (IllegalArgumentException e) {
            assertTrue(true);//ok, p is not square
            return;
        }
        fail();
    }
    
    /**
     * Test QuadraticMultivariateRealFunction
     * with non-symmetric matrix -> it throws an exception
     * 
     * @throws IllegalArgumentException
     */
    public void testErrorQuadraticMultivariateRealFunction3() throws IllegalArgumentException {
        final double[][] pMatrix = { { 2, 0}, { -10, 0 } };
        final double[] qVector = new double[] { 0, -1 };
        final double r = 2;
        try{
            new QuadraticMultivariateRealFunction(pMatrix, qVector, r, true);
        }catch (IllegalArgumentException e) {
            assertTrue(true);//ok, p is not symmetric
            return;
        }
        fail();
    }
    
    /**
     * Test set lower bounds in LPOptimizationRequest
     * with non acceptable bounds -> it throws an exception
     * 
     * @throws IllegalArgumentException
     */
    public void testSetLb() throws IllegalArgumentException {
        final LPOptimizationRequest or = new LPOptimizationRequest();
        final double[] lb = {Double.NaN};
        final RealVector lbVector = new ArrayRealVector(lb);
        try{
            or.setLb(lbVector);
        }catch (IllegalArgumentException e) {
            assertTrue(true);//ok, non acceptable bounds
            return;
        }
        fail();
    }
    
    /**
     * Test set upper bounds in LPOptimizationRequest
     * with non acceptable bounds -> it throws an exception
     * 
     * @throws IllegalArgumentException
     */
    public void testSetUb() throws IllegalArgumentException {
        final LPOptimizationRequest or = new LPOptimizationRequest();
        final double[] lb = {Double.NaN};
        final RealVector lbVector = new ArrayRealVector(lb);
        try{
            or.setUb(lbVector);
        }catch (IllegalArgumentException e) {
            assertTrue(true);//ok, non acceptable bounds
            return;
        }
        fail();
    }
    
    /**
     * Test findEqFeasiblePoint2 from of AbstractOptimizationRequestHandler 
     * with a matrix row dimensions > column dimensions -> it throws an exception
     */
    public void testfindEqFeasiblePoint() {
        final OptimizationRequest or = new OptimizationRequest();
        or.setA(new double[][] {{0,2}});
        final OptimizationRequestHandler opt = new NewtonUnconstrained(true);
        opt.setOptimizationRequest(or);
        assertEquals(1,opt.getMeq());
        
        final RealMatrix a = new BlockRealMatrix(new double[][] {{0,2}, {0,2}});
        final RealVector b = new ArrayRealVector(new double[] {2});
        try{
            opt.findEqFeasiblePoint2(a,b);
        }catch (PatriusRuntimeException e) {
            assertTrue(true);//ok, rank(A) = p > n
            return;
        }
        fail();
    }
    
    /**
     * Test findEqFeasiblePoint2 from of AbstractOptimizationRequestHandler 
     * with a matrix row dimensions != rantAT -> it throws an exception
     */
    public void testfindEqFeasiblePoint2() {
        final OptimizationRequest or = new OptimizationRequest();
        or.setA(new double[][] {{0,2}});
        final OptimizationRequestHandler opt = new NewtonUnconstrained(true);
        opt.setOptimizationRequest(or);
        assertEquals(1,opt.getMeq());
        
        final RealMatrix a = new BlockRealMatrix(new double[][] {{0,2,3}, {0,2,3}});
        final RealVector b = new ArrayRealVector(new double[] {2,1,1});
        try{
            opt.findEqFeasiblePoint2(a,b);
        }catch (PatriusRuntimeException e) {
            assertTrue(true);//ok, rankAT !=  matrix a row dimensions
            return;
        }
        fail();
    }
    
    /**
     * Test setFi LPOptimizationRequest
     * this method throws an exception
     * EXPLANATION
     */
    public void testExceptionSetF0LPOR() {
        final LPOptimizationRequest or = new LPOptimizationRequest();
        try{
            or.setF0(null);
        }catch (UnsupportedOperationException e) {
            assertTrue(true);//ok, exceptions thrown
            return;   
        }
        fail();
    }
    
    /**
     * Test setFi LPOptimizationRequest
     * this method throws an exception
     * EXPLANATION
     */
    public void testExceptionSetFiLPOR() {
        final LPOptimizationRequest or = new LPOptimizationRequest();
        try{
            or.setFi(null);
        }catch (UnsupportedOperationException e) {
            assertTrue(true);//ok, exceptions thrown
            return;   
        }
        fail();
    }

}
