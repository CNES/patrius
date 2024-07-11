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
package fr.cnes.sirius.patrius.math.optim.joptimizer.algebra;


import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;
import fr.cnes.sirius.patrius.math.linear.Array2DRowRealMatrix;
import fr.cnes.sirius.patrius.math.linear.ArrayRealVector;
import fr.cnes.sirius.patrius.math.linear.BlockRealMatrix;
import fr.cnes.sirius.patrius.math.linear.MatrixUtils;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealVector;
import fr.cnes.sirius.patrius.math.linear.SingularValueDecomposition;
import fr.cnes.sirius.patrius.math.optim.joptimizer.TestUtils;
import fr.cnes.sirius.patrius.math.optim.joptimizer.util.Utils;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusRuntimeException;

/**
 * @author alberto trivellato (alberto.trivellato@gmail.com)
 */
public class CholeskyFactorizationTest extends TestCase {

    /**
     * Test Invert 1
     * 
     * @throws PatriusException if an error occurs
     */
    public void testInvert1() throws PatriusException {
        final double[][] qData = new double[][] { 
                { 1, .12, .13, .14, .15 },
                { .12, 2, .23, .24, .25 }, 
                { .13, .23, 3, 0, 0 },
                { .14, .24, 0, 4, 0 }, 
                { .15, .25, 0, 0, 5 } };
        final RealMatrix q = MatrixUtils.createRealMatrix(qData);

        final CholeskyFactorization myc = new CholeskyFactorization(new BlockRealMatrix(qData));
        myc.factorize();
        final RealMatrix l = new Array2DRowRealMatrix(myc.getL().getData());
        final RealMatrix lT = new Array2DRowRealMatrix(myc.getLT().getData());
        
        // check Q = L.LT
        double norm = l.multiply(lT).subtract(q).getNorm();
        assertTrue(norm < 1.E-15);
        
        final RealMatrix lInv = new SingularValueDecomposition(l).getSolver().getInverse();
        final RealMatrix lTInv = new SingularValueDecomposition(lT).getSolver().getInverse();
       
        final RealMatrix Id = MatrixUtils.createRealIdentityMatrix(q.getRowDimension());
        //check Q.(LTInv * LInv) = 1
        norm = q.multiply(lTInv.multiply(lInv)).subtract(Id).getNorm();
        assertTrue(norm < 5.E-15);
        
        // check Q.QInv = 1
        final RealMatrix QInv = MatrixUtils.createRealMatrix(myc.getInverse().getData());
        norm = q.multiply(QInv).subtract(Id).getNorm();
        assertTrue(norm < 1.E-15);
    }
    
    /**
     * The same as before, but with rescaling.
     */
    public void testInvert2() throws PatriusException {
        final double[][] QData = new double[][] { 
                { 1, .12, .13, .14, .15 },
                { .12, 2, .23, .24, .25 }, 
                { .13, .23, 3, 0, 0 },
                { .14, .24, 0, 4, 0 }, 
                { .15, .25, 0, 0, 5 } };
        final RealMatrix Q = MatrixUtils.createRealMatrix(QData);

        final CholeskyFactorization myc = new CholeskyFactorization(new BlockRealMatrix(QData), new Matrix1NornRescaler());
        myc.factorize();
        final RealMatrix L = new Array2DRowRealMatrix(myc.getL().getData());
        final RealMatrix LT = new Array2DRowRealMatrix(myc.getLT().getData());
        
        // check Q = L.LT
        double norm = L.multiply(LT).subtract(Q).getNorm();
        assertTrue(norm < 1.E-15);
        
        final RealMatrix LInv = new SingularValueDecomposition(L).getSolver().getInverse();
        final RealMatrix LTInv = new SingularValueDecomposition(LT).getSolver().getInverse();
        
        final RealMatrix Id = MatrixUtils.createRealIdentityMatrix(Q.getRowDimension());
        //check Q.(LTInv * LInv) = 1
        norm = Q.multiply(LTInv.multiply(LInv)).subtract(Id).getNorm();
        assertTrue(norm < 5.E-15);
        
        // check Q.QInv = 1
        final RealMatrix QInv = MatrixUtils.createRealMatrix(myc.getInverse().getData());
        norm = Q.multiply(QInv).subtract(Id).getNorm();
        assertTrue(norm < 1.E-15);
    }
    
    /**
     * This test shows that the correct check of the inversion accuracy must be done with
     * the scaled residual, not with the simple norm ||A.x-b||
     */
    public void testScaledResidual() throws PatriusException, IOException {
        final double[][] G = TestUtils.loadDoubleMatrixFromFile("factorization" + File.separator  + "matrix1.csv");
        final RealMatrix Q = MatrixUtils.createRealMatrix(G);
    
        final RealVector b = new ArrayRealVector(new double[]{1,2,3,4,5,6,7,8,9,10});
        
        final CholeskyFactorization cs = new CholeskyFactorization(new BlockRealMatrix(Q.getData(false)));
        cs.factorize();
        final RealVector x = new ArrayRealVector(cs.solve(new ArrayRealVector(b.toArray())).toArray());
        
        //scaledResidual = ||Ax-b||_oo/( ||A||_oo . ||x||_oo + ||b||_oo )
        // with ||x||_oo = max(x[i])
        final double scaledResidual = Utils.calculateScaledResidual(Q, x, b);
        assertTrue(scaledResidual < Utils.getDoubleMachineEpsilon());
        
        //b - Q.x
        //checking the simple norm, this will fail
        //final double n1 = b.subtract(Q.operate(x)).getNorm();
        //assertTrue(n1 < 1.E-8);
    }
    
    /**
     * Not positive matrix, must fail
     */
    public void testInvertNotPositive() throws IOException {
        final double[][] G = TestUtils.loadDoubleMatrixFromFile("factorization" + File.separator  + "matrix4.csv");
        
        try{
            final CholeskyFactorization cs = new CholeskyFactorization(new BlockRealMatrix(G));
            cs.factorize();
        }catch(final PatriusException e){
            assertTrue(true);//ok, the matrix is not positive
            return;
        }
        
        //if here, not good
        fail();
        
    }
    
    /**
     * The matrix6 has a regular Cholesky factorization (as given by Mathematica) 
     * This test shows how rescaling a matrix can help its factorization.
     */
    public void testScale6() throws PatriusException, IOException {
        
        final String matrixId = "6";
        final double[][] A = TestUtils.loadDoubleMatrixFromFile("factorization" + File.separator + "matrix" + matrixId + ".txt", " ".charAt(0));
        final RealMatrix AMatrix = new BlockRealMatrix(A);
        final int dim = AMatrix.getRowDimension();
        
        CholeskyFactorization cs;
        try{
            cs = new CholeskyFactorization(AMatrix);
            cs.factorize();
        }catch(final PatriusException e){
            final MatrixRescaler rescaler = new Matrix1NornRescaler();
            final RealVector Uv = rescaler.getMatrixScalingFactorsSymm(AMatrix);
            final RealMatrix U = AlgebraUtils.diagonal(Uv);
            
            assertTrue(rescaler.checkScaling(AlgebraUtils.fillSubdiagonalSymmetricMatrix(AMatrix), Uv, Uv));
            
            final RealMatrix AScaled = AlgebraUtils.diagonalMatrixMult(Uv, AMatrix, Uv);
            cs = new CholeskyFactorization(AScaled);
            cs.factorize();
            
            //NOTE: with scaling, we must solve U.A.U.z = U.b, after that we have x = U.z
            
            //solve Q.x = b
            final RealVector b = AlgebraUtils.randomValuesVector(dim, -1, 1, 12345L);
            final RealVector x = cs.solve(U.operate(b));
            final double scaledResidualx = Utils.calculateScaledResidual(AMatrix, U.operate(x), b);
            assertTrue(scaledResidualx < Utils.getDoubleMachineEpsilon());
            
            //solve Q.X = B
            final RealMatrix B = Utils.randomValuesMatrix(dim, 5, -1, 1, 12345L);
            final RealMatrix X = cs.solve(U.multiply(B));
            final double scaledResidualX = Utils.calculateScaledResidual(AMatrix, U.multiply(X), B);
            assertTrue(scaledResidualX < Utils.getDoubleMachineEpsilon());
        }
    }
    
    /**
     * Test factorize
     * with non-square matrix -> it throws an exception
     * 
     * @throws PatriusException
     */
    public void testFactorizeError() throws PatriusException {
        final double[][] a = { { 2, 0, 1}, { -10, 0, 1 } };
        final RealMatrix aMatrix = new BlockRealMatrix(a);

        try{
            final CholeskyFactorization cs = new CholeskyFactorization(aMatrix);
            cs.factorize(true);
        }catch (final PatriusException e) {
            assertTrue(true);//ok, matrix is not square
            return;
        }
        fail();
    }
    
    /**
     * Test solve
     * with vector and Q matrix dimension mismatch -> it throws an exception
     * @throws PatriusException, IOException
     */
    public void testSolveError() throws PatriusException, IOException {
        final double[][] G = TestUtils.loadDoubleMatrixFromFile("factorization" + File.separator  + "matrix1.csv");
        final RealMatrix Q = MatrixUtils.createRealMatrix(G);
    
        final RealVector b = new ArrayRealVector(new double[]{1,2,3,4,5});
        
        final CholeskyFactorization cs = new CholeskyFactorization(Q);
        cs.factorize();
        try{
            cs.solve(b);
        }catch (final PatriusRuntimeException e) {
            assertTrue(true);//ok, vector and Q matrix dimension mismatch
            return;
        }
        fail();
    }
    
    /**
     * Test solve
     * with matrix and Q matrix dimension mismatch -> it throws an exception
     * @throws PatriusException, IOException
     */
    public void testSolveError2() throws PatriusException, IOException {
        final double[][] G = TestUtils.loadDoubleMatrixFromFile("factorization" + File.separator  + "matrix1.csv");
        final RealMatrix Q = MatrixUtils.createRealMatrix(G);
    
        final RealMatrix bMatrix = new BlockRealMatrix(new double[][] {{1,2},{4,5}});
        
        final CholeskyFactorization cs = new CholeskyFactorization(Q);
        cs.factorize();
        try{
            cs.solve(bMatrix);
        }catch (final PatriusRuntimeException e) {
            assertTrue(true);//ok, bMatrix and Q matrix dimension mismatch
            return;
        }
        fail();
    }
}
