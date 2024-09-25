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

package fr.cnes.sirius.patrius.math.optim.joptimizer.algebra;

import junit.framework.TestCase;
import fr.cnes.sirius.patrius.math.linear.Array2DRowRealMatrix;
import fr.cnes.sirius.patrius.math.linear.ArrayRealVector;
import fr.cnes.sirius.patrius.math.linear.BlockRealMatrix;
import fr.cnes.sirius.patrius.math.linear.DefaultRealMatrixChangingVisitor;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealVector;

/**
 * 
 * @author x
 *
 */
public class AlgebraUtilsTest extends TestCase {

    /**
     * Use of a Colt sparse matrix
     */
    public void testDumpSparseMatrix() {
        final double[][] a = new double[][] { { 1, 0, 0, 2 }, { 0, 0, 2, 0 }, { 2, 3, 0, 0 },
                { 0, 0, 4, 4 } };

        final BlockRealMatrix s2 = new BlockRealMatrix(a);
        s2.walkInOptimizedOrder(new DefaultRealMatrixChangingVisitor() {
            @Override
            public double visit(final int i, final int j, final double sij) {
                if (sij != 0) {
                    assertEquals(sij, a[i][j]);
                }
                return sij;
            }
        });
    }

    /**
     * Test subdiagonal multiplication
     */
    public void testSubdiagonalMultiply() {
        final double[][] a = { { 1, 2, 3, 4 }, { 5, 6, 7, 8 }, { 1, 3, 5, 7 } };
        final double[][] b = { { 1, 2, 3 }, { 3, 4, 2 }, { 5, 6, 7 }, { 7, 8, 9 } };
        final double[][] expectedResult = { { 50, 0, 0 }, { 114, 140, 0 }, { 84, 100, 107 } };

        final RealMatrix aMatrix = new BlockRealMatrix(a);
        final RealMatrix bMatrix = new BlockRealMatrix(b);
        final RealMatrix ret = AlgebraUtils.subdiagonalMultiply(aMatrix, bMatrix);
        for (int i = 0; i < expectedResult.length; i++) {
            for (int j = 0; j < expectedResult[i].length; j++) {
                assertEquals(expectedResult[i][j], ret.getEntry(i, j));
            }
        }
    }
    
    /**
     * Test sub-diagonal multiplication on matrix with different dimensions
     * It cannot calculate the multiplication, it has to throw an exception
     */
    public void testSubdiagonalMultiplyError() throws IllegalArgumentException{
        final double[][] a = { { 1, 2 }, { 5, 6 } };
        final double[][] b = { { 1, 2, 3 }, { 3, 4, 2 }, { 5, 6, 7 }, { 7, 8, 9 } };

        final RealMatrix aMatrix = new BlockRealMatrix(a);
        final RealMatrix bMatrix = new BlockRealMatrix(b);
        try{
            AlgebraUtils.subdiagonalMultiply(aMatrix, bMatrix);
        }catch (final IllegalArgumentException e) {
            assertTrue(true);//ok, matrix dimensions mismatch
            return;
        }
        fail();
    }

    /**
     * Test z multiplication 
     * Matrix and vector with different dimensions
     * It cannot calculate the multiplication, it has to throw an exception
     */
    public void testZMultError() throws IllegalArgumentException{
        final double[][] aMat = {{1, 0},{0, 1}};
        final double[] a = {1, 0, 3};
        final double[] b = {1, 0};
        final double beta = 1;
        
        final RealMatrix aMatrix = new BlockRealMatrix(aMat);
        final RealVector aVector = new ArrayRealVector(a);
        final RealVector bVector = new ArrayRealVector(b);

        try{
            AlgebraUtils.zMult(aMatrix, aVector, bVector, beta);
        }catch (final IllegalArgumentException e) {
            assertTrue(true);//ok, matrix and vector dimensions mismatch
            return;
        }
        fail();
    }
    
    /**
     * Test z multiplication
     * Matrix and vector with different dimensions -> it has to throw an exception
     */
    public void testZMultError2() throws IllegalArgumentException{
        final double[][] aMat = {{1, 0, 1},{0, 1, 2}};
        final double[] a = {1, 0, 3};
        final double[] b = {1, 0, 3};
        final double beta = 1;
        
        final RealMatrix aMatrix = new BlockRealMatrix(aMat);
        final RealVector aVector = new ArrayRealVector(a);
        final RealVector bVector = new ArrayRealVector(b);

        try{
            AlgebraUtils.zMult(aMatrix, aVector, bVector, beta);
        }catch (final IllegalArgumentException e) {
            assertTrue(true);//ok, matrix and vector dimensions mismatch
            return;
        }
        fail();
    }
    
    /**
     * Test z multiplication transpose
     * Matrix and vector with different dimensions -> it has to throw an exception
     */
    public void testZMultTranspose() throws IllegalArgumentException{
        final double[][] aMat = {{1, 0, 1},{0, 1, 2}};
        final double[] a = {1, 0, 3};
        final double[] b = {1, 0, 3};
        final double beta = 1;
        
        final RealMatrix aMatrix = new BlockRealMatrix(aMat);
        final RealVector aVector = new ArrayRealVector(a);
        final RealVector bVector = new ArrayRealVector(b);

        try{
            AlgebraUtils.zMultTranspose(aMatrix, aVector, bVector, beta);
        }catch (final IllegalArgumentException e) {
            assertTrue(true);//ok, matrix and vector dimensions mismatch
            return;
        }
        fail();
    }
    
    /**
     * Test get condition number ranges
     */
    public void testGetConditionNumberRanges() {
        final double[][] a = new double[][] { { 1., 0, 0 }, { 0., 2., 0 }, { 0., 0., 3. } };
        final double kExpected2 = 3;
        final double kExpected00 = 3;
        final RealMatrix aMatrix = new Array2DRowRealMatrix(a);
        final double[] cn2 = AlgebraUtils.getConditionNumberRange(aMatrix, 2);
        final double[] cn00 = AlgebraUtils.getConditionNumberRange(aMatrix, Integer.MAX_VALUE);
        assertTrue(kExpected2 >= cn2[0]);
        assertTrue(kExpected00 >= cn00[0]);
        
    }
    
    /**
     * Test get condition number ranges 
     * With p != 2 or Integer.MAX_VALUE -> it throws an exception
     * @throws IllegalArgumentException
     */
    public void testGetConditionNumberRangesError() throws IllegalArgumentException {
        final double[][] a = new double[][] { { 1., 0, 0 }, { 0., 2., 0 }, { 0., 0., 3. } };
        final RealMatrix aMatrix = new Array2DRowRealMatrix(a);
        try{
            AlgebraUtils.getConditionNumberRange(aMatrix, 1);
        }catch (final IllegalArgumentException e) {
            assertTrue(true);//ok, p!= 2 or Integer.MAX_VALUE
            return;
        }
        fail();
    }
    
    /**
     * Test fillSubdiagonalSymmetricMatrix
     * Matrix is not square -> it throws an exception
     * @throws IllegalArgumentException 
     */
    public void testfillSubdiagonalSymmetricMatrix() throws IllegalArgumentException {
        final double[][] a = new double[][] { { 1., 0, 0 }, { 0., 2., 0 } };
        final RealMatrix aMatrix = new Array2DRowRealMatrix(a);
        try{
            AlgebraUtils.fillSubdiagonalSymmetricMatrix(aMatrix);
        }catch (final IllegalArgumentException e) {
            assertTrue(true);//ok, matrix is not square
            return;
        }
        fail();
    }
    
    /**
     * Test add method:  ret1 = a + b
     */
    public void testAdd(){
        final double[][] a = { { 1, 2, 3 }, { 5, 6, 7}, { 1, 3, 5} };
        final double[][] b = { { 1, 2, 3 }, { 3, 4, 2 }, { 5, 6, 7 }};
        final double[][] expectedResult = { { 2, 4, 6 }, { 8, 10, 9 }, { 6, 9, 12 } };
        final RealMatrix aMatrix = new BlockRealMatrix(a);
        final RealMatrix bMatrix = new BlockRealMatrix(b);
        final RealMatrix ret = aMatrix.add(bMatrix);
        for (int i = 0; i < expectedResult.length; i++) {
            for (int j = 0; j < expectedResult[i].length; j++) {
                assertEquals(expectedResult[i][j], ret.getEntry(i, j));
            }
        }      
    }
    
    /**
     * Test add
     * Matrix with different dimensions -> it has to throw an exception
     */
    public void testAddError() throws IllegalArgumentException{
        final double[][] a = {{1, 0, 1},{0, 1, 2}};
        final double[][] b = {{1, 0},{0, 1}};
        
        final RealMatrix aMatrix = new BlockRealMatrix(a);
        final RealMatrix bMatrix = new BlockRealMatrix(b);

        try{
            aMatrix.add(bMatrix);
        }catch (final IllegalArgumentException e) {
            assertTrue(true);//ok, matrix dimensions mismatch
            return;
        }
        fail();
    }
    
    /**
     * Test add
     * Matrix with different dimensions -> it has to throw an exception
     */
    public void testAddError2() throws IllegalArgumentException{
        final double[][] a = {{1, 0, 1},{0, 1, 2}};
        final double[][] b = {{1, 0},{0, 1}};
        final double beta = 1;
        
        final RealMatrix aMatrix = new BlockRealMatrix(a);
        final RealMatrix bMatrix = new BlockRealMatrix(b);

        try{
            AlgebraUtils.add(aMatrix, bMatrix, beta);
        }catch (final IllegalArgumentException e) {
            assertTrue(true);//ok, matrix dimensions mismatch
            return;
        }
        fail();
    }
    
    /**
     * Test add
     * Vector with different dimensions -> it has to throw an exception
     */
    public void testAddError4() throws IllegalArgumentException{
        final double[] a = {1, 0, 1};
        final double[] b = {1, 0};
        final double beta = 1;
        
        final RealVector aMatrix = new ArrayRealVector(a);
        final RealVector bMatrix = new ArrayRealVector(b);

        try{
            AlgebraUtils.add(aMatrix, bMatrix, beta);
        }catch (final IllegalArgumentException e) {
            assertTrue(true);//ok, vector dimensions mismatch
            return;
        }
        fail();
    }
    
    /**
     * Test diagonal matrix multiplication:  ret1 = a.diagonalU
     */
    public void testDiagonalMatrixMult(){
        final double[][] a = { { 1, 2, 3 }, { 5, 6, 7}, { 1, 3, 5} };
        final double[] diagonalU = { 1, 2, 3 };
        final double[][] expectedResult = { { 1, 4, 9 }, { 5, 12, 21 }, { 1, 6, 15 } };
        final RealMatrix aMatrix = new BlockRealMatrix(a);
        final RealVector uVector = new ArrayRealVector(diagonalU);
        final RealMatrix ret = AlgebraUtils.diagonalMatrixMult(aMatrix, uVector);
        for (int i = 0; i < expectedResult.length; i++) {
            for (int j = 0; j < expectedResult[i].length; j++) {
                assertEquals(expectedResult[i][j], ret.getEntry(i, j));
            }
        }      
    }
    
    /**
     * Test case when vector is null
     */
    public void testReplaceValues(){
        final double oldValue = 2;
        final double newValue = 3;
        final RealVector aVector = null;
        final RealVector ret = AlgebraUtils.replaceValues(aVector, oldValue, newValue);
        assertEquals(null, ret);

    }
    /**
     * Test compose with different numbers of columns between the parts
     * -> it throws an exception
     */
    public void testComposeDifferentCol()  {
        final double[][] p1 = new double[][] {{0,2}};
        final double[][] p2 = new double[][] {{0,2,3}, {2,1,0}};
        final RealMatrix[][] parts = 
                new RealMatrix[][] {{new BlockRealMatrix(p1)},{new BlockRealMatrix(p2)}};
        try{
            AlgebraUtils.composeMatrix(parts);
        }catch(final IllegalArgumentException e){
            assertTrue(true); //ok, different number of columns
            return;
        }
        fail();
    }
    
    /**
     * Test compose with different numbers of rows between the parts
     * -> it throws an exception
     */
    public void testComposeDifferentRow()  {
        final double[][] p1 = new double[][] {{0,2}};
        final double[][] p2 = new double[][] {{0,2}, {2,1}, {0,2}, {2,1}};
        final RealMatrix[][] parts = 
                new RealMatrix[][] {{new BlockRealMatrix(p1), new BlockRealMatrix(p2)},{new BlockRealMatrix(p1), new BlockRealMatrix(p2)}};
        try{
            AlgebraUtils.composeMatrix(parts);
        }catch(final IllegalArgumentException e){
            assertTrue(true); //ok, different number of rows
            return;
        }
        fail();
    }
    
    /**
     * Test checkRectangularShape with dimension mismatch -> it throws an exception
     */
    public void testcheckRectangularShapeError()  {
        final double[][] p1 = new double[][] {{0,2}};
        final double[][] p2 = new double[][] {{0,2,3}, {2,1,0}};
        final RealMatrix[][] parts = 
                new RealMatrix[][] {{null,new BlockRealMatrix(p1)},{new BlockRealMatrix(p2)}};
        try{
            AlgebraUtils.checkRectangularShape(parts);
        }catch(final IllegalArgumentException e){
            assertTrue(true); //ok, he rows of the array have different number of columns
            return;
        }
        fail();
    }
}
