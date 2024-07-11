/**
 * Copyright 2011-2022 CNES
 * Copyright 2011-2014 JOptimizer
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
 * VERSION:4.6:DM:DM-2591:27/01/2021:[PATRIUS] Intigration et validation JOptimizer
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.optim.joptimizer.util;

import java.lang.reflect.Array;
import java.util.BitSet;
import java.util.HashMap;

/**
 * <p>
 * Operations on arrays, primitive arrays (like {@code int[]}) and primitive wrapper arrays (like {@code Integer[]}).
 *
 * <p>
 * This class tries to handle {@code null} input gracefully. An exception will not be thrown for a {@code null} array
 * input. However, an Object array that contains a {@code null} element may throw an exception. Each method documents
 * its behavior.
 *
 * <p>
 *
 * @since 4.6
 */
public final class ArrayUtils {
    
    /** String Index */
    private static final String INDEX = "Index: ";
    /** String Length */
    private static final String LEN = ", Length: ";
    
    /**
     * Private constructor
     */
    private ArrayUtils() {
    }

    /**
     * Underlying implementation of add(array, index, element) methods.
     * The last parameter is the class, which may not equal element.getClass
     * for primitives.
     *
     * @param array the array to add the element to, may be {@code null}
     * @param index the position of the new object
     * @param element the object to add
     * @return A new array containing the existing elements and the new element
     */
    public static int[] add(final int[] array,
            final int index,
            final int element) {
        return (int[]) add(array, index, Integer.valueOf(element), Integer.TYPE);
    }

    /**
     * Underlying implementation of add(array, index, element) methods.
     * The last parameter is the class, which may not equal element.getClass
     * for primitives.
     *
     * @param array  the array to add the element to, may be {@code null}
     * @param index  the position of the new object
     * @param element  the object to add
     * @param clss the type of the element being added
     * @return A new array containing the existing elements and the new element
     */
    private static Object add(final Object array,
            final int index,
            final Object element,
            final Class<?> clss) {
        if (array == null) {
            if (index != 0) {
                // Error: The array is null and the start index is different to 0 
                throw new IndexOutOfBoundsException(INDEX + index + ", Length: 0");
            }
            // Creates a new array with the specified component type and length.
            final Object joinedArray = Array.newInstance(clss, 1);
            Array.set(joinedArray, 0, element);
            return joinedArray;
        }
        final int length = Array.getLength(array);
        if (index > length || index < 0) {
            // Error: the index surpasses the length of the array or is negative
            throw new IndexOutOfBoundsException(INDEX + index + LEN + length);
        }
        final Object result = Array.newInstance(clss, length + 1);
        System.arraycopy(array, 0, result, 0, index);
        Array.set(result, index, element);
        if (index < length) {
            System.arraycopy(array, index, result, index + 1, length - index);
        }
        return result;
        
    }

    /**
     * Inserts elements into an array at the given index (starting from zero).</p>
     * 
     * @param index the position within {@code array} to insert the new values
     * @param array the array to insert the values into, may be {@code null}
     * @param values the new values to insert, may be {@code null}
     * @return The new array.
     * @throws IndexOutOfBoundsException if {@code array} is provided
     *         and either {@code index < 0} or {@code index > array.length}
     */
    public static int[] insert(final int index,
            final int[] array,
            final int... values) {
        if (array == null) {
            return null;
        }
        // If the values to insert are null it returns the initial array
        if (values.length == 0) {
            return array.clone();
        }
        if (index < 0 || index > array.length) {
         // Error: the index surpasses the length of the array or is negative
            throw new IndexOutOfBoundsException(INDEX + index + LEN + array.length);
        }
        
        // Inserting the values
        final int[] result = new int[array.length + values.length];

        System.arraycopy(values, 0, result, index, values.length);
        if (index > 0) {
            System.arraycopy(array, 0, result, 0, index);
        }
        if (index < array.length) {
            System.arraycopy(array, index, result, index + values.length, array.length - index);
        }
        return result;
        
    }

    /**
     * Removes the element at the specified position from the specified array.
     * All subsequent elements are shifted to the left (subtracts one from
     * their indices).
     *
     * @param array the array to remove the element from, may not be {@code null}
     * @param index the position of the element to be removed
     * @return A new array containing the existing elements except the element
     *         at the specified position.
     * @throws IndexOutOfBoundsException if the index is out of range
     *         (index &lt; 0 || index &gt;= array.length), or if the array is {@code null}.
     */
    public static Object remove(final Object array,
            final int index) {
        final int length = Array.getLength(array);
        if (index < 0 || index >= length) {
            throw new IndexOutOfBoundsException(INDEX + index + LEN + length);
        }

        final Object result = Array.newInstance(array.getClass().getComponentType(), length - 1);
        System.arraycopy(array, 0, result, 0, index);
        if (index < length - 1) {
            System.arraycopy(array, index + 1, result, index, length - index - 1);
        }

        return result;
    }

    /**
     * Copies the given array and adds the given element at the end of the new array.
     * 
     * from
     * https://github.com/apache/commons-lang/blob/61e82e832e31037810cec0c526ecd83fbeb62227/src/
     * main/java/org/apache/commons/lang3/ArrayUtils.java#L4656
     * 
     * @param array the array to copy and add the element to, may be {@code null}
     * @param element the object to add at the last index of the new array
     * @return A new array containing the existing elements plus the new element
     */
    public static int[] add(final int[] array,
            final int element) {
        final int[] newArray = (int[]) copyArrayGrow1(array, Integer.TYPE);
        newArray[newArray.length - 1] = element;
        return newArray;
    }

    /**
     * Checks if the value is in the given array.
     * 
     * from
     * https://github.com/apache/commons-lang/blob/61e82e832e31037810cec0c526ecd83fbeb62227/src/
     * main/java/org/apache/commons/lang3/ArrayUtils.java#L4656
     * 
     * The method returns {@code false} if a {@code null} array is passed in.
     *
     * @param array the array to search through
     * @param valueToFind the value to find
     * @return {@code true} if the array contains the object
     */
    public static boolean contains(final int[] array,
            final int valueToFind) {
        return getArrayIndex(array, valueToFind) != -1;
    }

    /**
     * Removes occurrences of specified elements, in specified quantities,
     * from the specified array. All subsequent elements are shifted left.
     *
     * @param array the array to remove the element from, may be {@code null}
     * @param values the elements to be removed
     * @return A new array containing the existing elements except the
     *         earliest-encountered occurrences of the specified elements.
     */
    public static int[] removeElements(final int[] array,
            final int... values) {
        // If the array or the values are null it returns the initial array
        if (array == null) {
            return null;
        }
        if(values == null){
            return array.clone();
        }
        // Uses a HashMap to save the elements to delete
        final HashMap<Integer, MutableInt> occurrences = new HashMap<>(values.length);
        for (final int v : values) {
            final Integer boxed = Integer.valueOf(v);
            final MutableInt count = occurrences.get(boxed);
            if (count == null) {
                occurrences.put(boxed, new MutableInt(1));
            } else {
                count.increment();
            }
        }
        // Saves the elements to remove in a BitSet
        final BitSet toRemove = new BitSet();
        // Looks for the elements to delete
        for (int i = 0; i < array.length; i++) {
            final int key = array[i];
            final MutableInt count = occurrences.get(key);
            if (count != null) {
                count.decrement();
                if (count.getValue() == 0) {
                    occurrences.remove(key);
                }
                toRemove.set(i);
            }
        }
        // Return a new array removing the occurrences of the specified elements
        return (int[]) removeAll(array, toRemove);
    }

    /**
     * Returns a copy of the given array of size 1 greater than the argument.
     * The last value of the array is left to the default value.
     *
     * @param array The array to copy, must not be {@code null}.
     * @param newArrayComponentType If {@code array} is {@code null}, create a
     *        size 1 array of this type.
     * @return A new copy of the array of size 1 greater than the input.
     */
    private static Object copyArrayGrow1(final Object array,
            final Class<?> newArrayComponentType) {
        if (array != null) {
            final int arrayLength = Array.getLength(array);
            final Object newArray = Array.newInstance(array.getClass().getComponentType(), arrayLength + 1);
            System.arraycopy(array, 0, newArray, 0, arrayLength);
            return newArray;
        }
        return Array.newInstance(newArrayComponentType, 1);

    }

    /**
     * Removes multiple array elements specified by indices.
     *
     * @param array source
     * @param indices to remove
     * @return new array of same type minus elements specified by the set bits in {@code indices}
     **/
    private static Object removeAll(final Object array,
            final BitSet indices) {
        
        final int srcLength = Array.getLength(array);
        final int removals = indices.cardinality(); // true bits are items to remove
        //array to return
        final Object result = Array.newInstance(array.getClass().getComponentType(), srcLength - removals); 
        int srcIndex = 0;
        int destIndex = 0;
        int count;
        int set = indices.nextSetBit(srcIndex);
        // Removing elements
        while (set != -1) {
            count = set - srcIndex;
            if (count > 0) {
                System.arraycopy(array, srcIndex, result, destIndex, count);
                destIndex += count;
            }
            srcIndex = indices.nextClearBit(set);
            set = indices.nextSetBit(srcIndex);
        }
        count = srcLength - srcIndex;
        if (count > 0) {
            System.arraycopy(array, srcIndex, result, destIndex, count);
        }
        return result; // array minus elements specified by the set bits in indices
    }

    /**
     * Finds the index of the given value in the array.
     *
     * @param arr array to search through for the object, may be null
     * @param value to find
     * @return the index of the value within the array, (-1) if not found or null array input
     */

    public static int getArrayIndex(final int[] arr,
            final int value) {
        int k = -1;
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == value) {
                k = i;
                break;
            }
        }
        return k;
    }
}