/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 *
 * Copyright 2011-2022 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.util;

/**
 * Provides a standard interface for double arrays. Allows different
 * array implementations to support various storage mechanisms
 * such as automatic expansion, contraction, and array "rolling".
 * 
 * @version $Id: DoubleArray.java 18108 2017-10-04 06:45:27Z bignon $
 */
public interface DoubleArray {

    /**
     * Returns the number of elements currently in the array. Please note
     * that this may be different from the length of the internal storage array.
     * 
     * @return number of elements
     */
    int getNumElements();

    /**
     * Returns the element at the specified index. Note that if an
     * out of bounds index is supplied a ArrayIndexOutOfBoundsException
     * will be thrown.
     * 
     * @param index
     *        index to fetch a value from
     * @return value stored at the specified index
     * @throws ArrayIndexOutOfBoundsException
     *         if <code>index</code> is less than
     *         zero or is greater than <code>getNumElements() - 1</code>.
     */
    double getElement(int index);

    /**
     * Sets the element at the specified index. If the specified index is greater than <code>getNumElements() - 1</code>
     * , the <code>numElements</code> property
     * is increased to <code>index +1</code> and additional storage is allocated
     * (if necessary) for the new element and all (uninitialized) elements
     * between the new element and the previous end of the array).
     * 
     * @param index
     *        index to store a value in
     * @param value
     *        value to store at the specified index
     * @throws ArrayIndexOutOfBoundsException
     *         if <code>index</code> is less than
     *         zero.
     */
    void setElement(int index, double value);

    /**
     * Adds an element to the end of this expandable array
     * 
     * @param value
     *        to be added to end of array
     */
    void addElement(double value);

    /**
     * Adds elements to the end of this expandable array
     * 
     * @param values
     *        to be added to end of array
     */
    void addElements(double[] values);

    /**
     * <p>
     * Adds an element to the end of the array and removes the first element in the array. Returns the discarded first
     * element. The effect is similar to a push operation in a FIFO queue.
     * </p>
     * <p>
     * Example: If the array contains the elements 1, 2, 3, 4 (in that order) and addElementRolling(5) is invoked, the
     * result is an array containing the entries 2, 3, 4, 5 and the value returned is 1.
     * </p>
     * 
     * @param value
     *        the value to be added to the array
     * @return the value which has been discarded or "pushed" out of the array
     *         by this rolling insert
     */
    double addElementRolling(double value);

    /**
     * Returns a double[] array containing the elements of this <code>DoubleArray</code>. If the underlying
     * implementation is
     * array-based, this method should always return a copy, rather than a
     * reference to the underlying array so that changes made to the returned
     * array have no effect on the <code>DoubleArray.</code>
     * 
     * @return all elements added to the array
     */
    double[] getElements();

    /**
     * Clear the double array
     */
    void clear();

}
