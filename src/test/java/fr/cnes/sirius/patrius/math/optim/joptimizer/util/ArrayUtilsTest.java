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

package fr.cnes.sirius.patrius.math.optim.joptimizer.util;

import junit.framework.TestCase;

/**
 * Test class for {@link ArrayUtils} class.
 * @author bdalfoferrer
 */
public class ArrayUtilsTest extends TestCase {

    /**
     * Test method insert (Inserts elements into an array at the given index)
     * Different cases are tested
     */
    public void testInsert() {
        // Case 1: array and  values are not null
        final int[] array = {1,4,5};
        final int[] values = {2,3};
        final int index = 1;
        final int[] expectedResult = {1,2,3,4,5};
        final int[] ret1 = ArrayUtils.insert(index, array, values);
        for (int i = 0; i < expectedResult.length; i++) {
            assertEquals(expectedResult[i], ret1[i]);
        }
        
        // Case 2: array is null
        final int[] ret2 = ArrayUtils.insert(index, null, values);
        assertEquals(null, ret2);
        
        // Case 3: values is null
        final int[] values2 = {};
        final int[] ret3 = ArrayUtils.insert(index, array, values2);
        for (int i = 0; i < array.length; i++) {
            assertEquals(array[i], ret3[i]);
        }        
    }
    
    /**
     * Test method insert with index value mismatch with array dimension -> it throws an exception
     */
    public void testInsertError() throws IndexOutOfBoundsException{
        final int[] array = {1,3,4};
        final int index = -1;
        final int[] values = {2,3};
        
        try{
            ArrayUtils.insert(index, array, values);
        }catch (IndexOutOfBoundsException e) {
            assertTrue(true);//ok, index value mismatch with array dimension
            return;
        }
        fail();
    }
    
    /**
     * Test method add (Inserts elements into an array at the given index)
     * Different cases are tested
     */
    public void testAdd() {
        // Case 1: array is null
        final int[] array = null;
        int index = 0;
        final int element = 2;
        final int[] expectedResult = {2};
        final int[] ret = ArrayUtils.add(array, index, element);
        for (int i = 0; i < expectedResult.length; i++) {
            assertEquals(expectedResult[i], ret[i]);
        }
        // Case 2: array and element are not null
        final int[] array2 = {1,3,4};
        final int[] expectedResult2 = {1,2,3,4};
        index = 1;
        final int[] ret2 = ArrayUtils.add(array2, index, element);
        for (int i = 0; i < expectedResult.length; i++) {
            assertEquals(expectedResult2[i], ret2[i]);
        }  
        
        //Case 3: add element at the end of an array that is null
        final int[] arr = null;
        final int[] ret3 = ArrayUtils.add(arr, element);
        for (int i = 0; i < expectedResult.length; i++) {
            assertEquals(expectedResult[i], ret3[i]);
        }
        
    }
    
    /**
     * Test method add with null array and index!=0 -> it throws an exception
     */
    public void testAddError() throws IndexOutOfBoundsException{
        final int[] array = null;
        final int index = 3;
        final int element = 2;
        
        try{
            ArrayUtils.add(array, index, element);
        }catch (IndexOutOfBoundsException e) {
            assertTrue(true);//ok, null array andn index!=0
            return;
        }
        fail();
    }
    
    /**
     * Test method add with index value mismatch with array dimension -> it throws an exception
     */
    public void testAddError2() throws IndexOutOfBoundsException{
        final int[] array = {1,3,4};
        final int index = 5;
        final int element = 2;
        
        try{
            ArrayUtils.add(array, index, element);
        }catch (IndexOutOfBoundsException e) {
            assertTrue(true);//ok, index value mismatch with array dimension
            return;
        }
        fail();
    }
    
    /**
     * Test methods removeElements for int[] and for int[]
     * Different cases are tested
     */
    public void testRemove() {
        // Case 1: array is null
        final int values = 2;
        final int[] ret1 = ArrayUtils.removeElements((int[]) null, values);
        final int[] res1 = ArrayUtils.removeElements(null, (int) values);
        assertEquals(null, ret1);
        assertEquals(null, res1);
        
        // Case 2
        final int[] array = {1,3,4};
        final int[] array2 = {1,3,4};
        final int[] ret2 = ArrayUtils.removeElements(array, null);
        final int[] res2 = ArrayUtils.removeElements(array2, null);
        for (int i = 0; i < array.length; i++) {
            assertEquals(array[i], ret2[i]);
            assertEquals(array2[i], res2[i]);
        } 
        
        // Case 3: normal case, array and value are not null
        final int[] array3 = {1,2,3,4};
        final int[] array4 = {1,2,3,4};
        final int[] expectedResult = {1,3,4};
        final int[] ret3 = ArrayUtils.removeElements(array3, values);
        final int[] res3 = ArrayUtils.removeElements(array4, (int) values);
        for (int i = 0; i < expectedResult.length; i++) {
            assertEquals(expectedResult[i], ret3[i]);
            assertEquals(expectedResult[i], res3[i]);
        }
        
        // Case 4: normal case, array and value are not null
        final int[] array5 = {1,2,2,2};
        final int[] array6 = {1,2,2,2};
        final int[] expectedResult2 = {1, 2};
        final int val = 2;
        final int[] ret4 = ArrayUtils.removeElements(array5, val, val);
        final int[] res4 = ArrayUtils.removeElements(array6, 2, 2);
        for (int i = 0; i < expectedResult2.length; i++) {
            assertEquals(expectedResult2[i], ret4[i]);
            assertEquals(expectedResult2[i], res4[i]);
        }
    }
    
    /**
     * Test method remove with index value mismatch with array dimension -> it throws an exception
     */
    public void testRemoveError() throws IndexOutOfBoundsException{
        final int[] array = {1,2,3,4};
        final int index = -1;
      
        try{
            ArrayUtils.remove(array, index);
        }catch (IndexOutOfBoundsException e) {
            assertTrue(true);//ok, index value mismatch with array dimension
            return;
        }
        fail();
    }
    
    /**
     * Test method get the index with the minimum value
     */
    public void testMinIndex() {
        // Inputs
        final int[] array1 = {1, 3, 0, 4};
        final int[] array2 = {0, 3, 0, 4};
        final int[] array3 = {0, 3, 0, -3};
        // Expected
        final int expectedResult1 = 2;
        final int expectedResult2 = 0;
        final int expectedResult3 = 3;
        // Calculations
        final int result1 = ArrayUtils.getArrayMinIndex(array1);
        final int result2 = ArrayUtils.getArrayMinIndex(array2);
        final int result3 = ArrayUtils.getArrayMinIndex(array3);
        
        // Asserts
        assertEquals(expectedResult1, result1);
        assertEquals(expectedResult2, result2);
        assertEquals(expectedResult3, result3);
        
    }
}
