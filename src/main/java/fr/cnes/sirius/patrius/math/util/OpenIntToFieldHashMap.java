/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 *
 * Copyright 2011-2017 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.cnes.sirius.patrius.math.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;

import fr.cnes.sirius.patrius.math.Field;
import fr.cnes.sirius.patrius.math.FieldElement;

// CHECKSTYLE: stop CommentRatio check
// CHECKSTYLE: stop MagicNumber check
// Reason: model - Commons-Math code

/**
 * Open addressed map from int to FieldElement.
 * <p>
 * This class provides a dedicated map from integers to FieldElements with a much smaller memory overhead than standard
 * <code>java.util.Map</code>.
 * </p>
 * <p>
 * This class is not synchronized. The specialized iterators returned by {@link #iterator()} are fail-fast: they throw a
 * <code>ConcurrentModificationException</code> when they detect the map has been modified during iteration.
 * </p>
 * 
 * @param <T>
 *        the type of the field elements
 * @version $Id: OpenIntToFieldHashMap.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 2.0
 */
public class OpenIntToFieldHashMap<T extends FieldElement<T>> implements Serializable {

    /** Status indicator for free table entries. */
    protected static final byte FREE = 0;

    /** Status indicator for full table entries. */
    protected static final byte FULL = 1;

    /** Status indicator for removed table entries. */
    protected static final byte REMOVED = 2;

    /** Serializable version identifier. */
    private static final long serialVersionUID = -9179080286849120720L;

    /** Load factor for the map. */
    private static final float LOAD_FACTOR = 0.5f;

    /**
     * Default starting size.
     * <p>
     * This must be a power of two for bit mask to work properly.
     * </p>
     */
    private static final int DEFAULT_EXPECTED_SIZE = 16;

    /**
     * Multiplier for size growth when map fills up.
     * <p>
     * This must be a power of two for bit mask to work properly.
     * </p>
     */
    private static final int RESIZE_MULTIPLIER = 2;

    /** Number of bits to perturb the index when probing for collision resolution. */
    private static final int PERTURB_SHIFT = 5;

    /** Field to which the elements belong. */
    private final Field<T> field;

    /** Keys table. */
    private int[] keys;

    /** Values table. */
    private T[] values;

    /** States table. */
    private byte[] states;

    /** Return value for missing entries. */
    private final T missingEntries;

    /** Current size of the map. */
    private int mapSize;

    /** Bit mask for hash values. */
    private int mask;

    /** Modifications count. */
    private transient int count;

    /**
     * Build an empty map with default size and using zero for missing entries.
     * 
     * @param fieldIn
     *        field to which the elements belong
     */
    public OpenIntToFieldHashMap(final Field<T> fieldIn) {
        this(fieldIn, DEFAULT_EXPECTED_SIZE, fieldIn.getZero());
    }

    /**
     * Build an empty map with default size
     * 
     * @param fieldIn
     *        field to which the elements belong
     * @param missingEntriesIn
     *        value to return when a missing entry is fetched
     */
    public OpenIntToFieldHashMap(final Field<T> fieldIn, final T missingEntriesIn) {
        this(fieldIn, DEFAULT_EXPECTED_SIZE, missingEntriesIn);
    }

    /**
     * Build an empty map with specified size and using zero for missing entries.
     * 
     * @param fieldIn
     *        field to which the elements belong
     * @param expectedSize
     *        expected number of elements in the map
     */
    public OpenIntToFieldHashMap(final Field<T> fieldIn, final int expectedSize) {
        this(fieldIn, expectedSize, fieldIn.getZero());
    }

    /**
     * Build an empty map with specified size.
     * 
     * @param fieldIn
     *        field to which the elements belong
     * @param expectedSizeIn
     *        expected number of elements in the map
     * @param missingEntriesIn
     *        value to return when a missing entry is fetched
     */
    public OpenIntToFieldHashMap(final Field<T> fieldIn, final int expectedSizeIn,
                                 final T missingEntriesIn) {
        this.field = fieldIn;
        final int capacity = computeCapacity(expectedSizeIn);
        this.keys = new int[capacity];
        this.values = this.buildArray(capacity);
        this.states = new byte[capacity];
        this.missingEntries = missingEntriesIn;
        this.mask = capacity - 1;
    }

    /**
     * Copy constructor.
     * 
     * @param source
     *        map to copy
     */
    public OpenIntToFieldHashMap(final OpenIntToFieldHashMap<T> source) {
        this.field = source.field;
        final int length = source.keys.length;
        this.keys = new int[length];
        System.arraycopy(source.keys, 0, this.keys, 0, length);
        this.values = this.buildArray(length);
        System.arraycopy(source.values, 0, this.values, 0, length);
        this.states = new byte[length];
        System.arraycopy(source.states, 0, this.states, 0, length);
        this.missingEntries = source.missingEntries;
        this.mapSize = source.mapSize;
        this.mask = source.mask;
        this.count = source.count;
    }

    /**
     * Compute the capacity needed for a given size.
     * 
     * @param expectedSizeIn
     *        expected size of the map
     * @return capacity to use for the specified size
     */
    private static int computeCapacity(final int expectedSizeIn) {
        // Specific case
        if (expectedSizeIn == 0) {
            return 1;
        }

        // General case
        final int capacity = (int) MathLib.ceil(expectedSizeIn / LOAD_FACTOR);
        final int powerOfTwo = Integer.highestOneBit(capacity);

        final int res;
        if (powerOfTwo == capacity) {
            res = capacity;
        } else {
            res = nextPowerOfTwo(capacity);
        }
        return res;
    }

    /**
     * Find the smallest power of two greater than the input value
     * 
     * @param i
     *        input value
     * @return smallest power of two greater than the input value
     */
    private static int nextPowerOfTwo(final int i) {
        return Integer.highestOneBit(i) << 1;
    }

    /**
     * Get the stored value associated with the given key
     * 
     * @param key
     *        key associated with the data
     * @return data associated with the key
     */
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Commons-Math code kept as such
    public T get(final int key) {
        // CHECKSTYLE: resume ReturnCount check

        final int hash = hashOf(key);
        int index = hash & this.mask;
        if (this.containsKey(key, index)) {
            return this.values[index];
        }

        if (this.states[index] == FREE) {
            return this.missingEntries;
        }

        int j = index;
        for (int perturb = perturb(hash); this.states[index] != FREE; perturb >>= PERTURB_SHIFT) {
            j = probe(perturb, j);
            index = j & this.mask;
            if (this.containsKey(key, index)) {
                return this.values[index];
            }
        }

        return this.missingEntries;

    }

    /**
     * Check if a value is associated with a key.
     * 
     * @param key
     *        key to check
     * @return true if a value is associated with key
     */
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Commons-Math code kept as such
    public boolean containsKey(final int key) {
        // CHECKSTYLE: resume ReturnCount check

        final int hash = hashOf(key);
        int index = hash & this.mask;
        if (this.containsKey(key, index)) {
            return true;
        }

        if (this.states[index] == FREE) {
            return false;
        }

        int j = index;
        for (int perturb = perturb(hash); this.states[index] != FREE; perturb >>= PERTURB_SHIFT) {
            j = probe(perturb, j);
            index = j & this.mask;
            if (this.containsKey(key, index)) {
                return true;
            }
        }

        return false;

    }

    /**
     * Get an iterator over map elements.
     * <p>
     * The specialized iterators returned are fail-fast: they throw a <code>ConcurrentModificationException</code> when
     * they detect the map has been modified during iteration.
     * </p>
     * 
     * @return iterator over the map elements
     */
    public Iterator iterator() {
        return new Iterator();
    }

    /**
     * Perturb the hash for starting probing.
     * 
     * @param hash
     *        initial hash
     * @return perturbed hash
     */
    private static int perturb(final int hash) {
        return hash & 0x7fffffff;
    }

    /**
     * Find the index at which a key should be inserted
     * 
     * @param key
     *        key to lookup
     * @return index at which key should be inserted
     */
    private int findInsertionIndex(final int key) {
        return findInsertionIndex(this.keys, this.states, key, this.mask);
    }

    /**
     * Find the index at which a key should be inserted
     * 
     * @param keys
     *        keys table
     * @param states
     *        states table
     * @param key
     *        key to lookup
     * @param mask
     *        bit mask for hash values
     * @return index at which key should be inserted
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Commons-Math code kept as such
    private static int findInsertionIndex(final int[] keys, final byte[] states,
                                          final int key, final int mask) {
        // CHECKSTYLE: resume CyclomaticComplexity check
        // CHECKSTYLE: resume ReturnCount check
        final int hash = hashOf(key);
        int index = hash & mask;
        if (states[index] == FREE) {
            return index;
        } else if (states[index] == FULL && keys[index] == key) {
            return changeIndexSign(index);
        }

        int perturb = perturb(hash);
        int j = index;
        if (states[index] == FULL) {
            while (true) {
                j = probe(perturb, j);
                index = j & mask;
                perturb >>= PERTURB_SHIFT;

                if (states[index] != FULL || keys[index] == key) {
                    break;
                }
            }
        }

        if (states[index] == FREE) {
            return index;
        } else if (states[index] == FULL) {
            // due to the loop exit condition,
            // if (states[index] == FULL) then keys[index] == key
            return changeIndexSign(index);
        }

        final int firstRemoved = index;
        while (true) {
            j = probe(perturb, j);
            index = j & mask;

            if (states[index] == FREE) {
                return firstRemoved;
            } else if (states[index] == FULL && keys[index] == key) {
                return changeIndexSign(index);
            }

            perturb >>= PERTURB_SHIFT;

        }

    }

    /**
     * Compute next probe for collision resolution
     * 
     * @param perturb
     *        perturbed hash
     * @param j
     *        previous probe
     * @return next probe
     */
    private static int probe(final int perturb, final int j) {
        return (j << 2) + j + perturb + 1;
    }

    /**
     * Change the index sign
     * 
     * @param index
     *        initial index
     * @return changed index
     */
    private static int changeIndexSign(final int index) {
        return -index - 1;
    }

    /**
     * Get the number of elements stored in the map.
     * 
     * @return number of elements stored in the map
     */
    public int size() {
        return this.mapSize;
    }

    /**
     * Remove the value associated with a key.
     * 
     * @param key
     *        key to which the value is associated
     * @return removed value
     */
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Commons-Math code kept as such
    public T remove(final int key) {
        // CHECKSTYLE: resume ReturnCount check

        final int hash = hashOf(key);
        int index = hash & this.mask;
        if (this.containsKey(key, index)) {
            return this.doRemove(index);
        }

        if (this.states[index] == FREE) {
            return this.missingEntries;
        }

        int j = index;
        for (int perturb = perturb(hash); this.states[index] != FREE; perturb >>= PERTURB_SHIFT) {
            j = probe(perturb, j);
            index = j & this.mask;
            if (this.containsKey(key, index)) {
                return this.doRemove(index);
            }
        }

        return this.missingEntries;

    }

    /**
     * Check if the tables contain an element associated with specified key
     * at specified index.
     * 
     * @param key
     *        key to check
     * @param index
     *        index to check
     * @return true if an element is associated with key at index
     */
    private boolean containsKey(final int key, final int index) {
        return (key != 0 || this.states[index] == FULL) && this.keys[index] == key;
    }

    /**
     * Remove an element at specified index.
     * 
     * @param index
     *        index of the element to remove
     * @return removed value
     */
    private T doRemove(final int index) {
        this.keys[index] = 0;
        this.states[index] = REMOVED;
        final T previous = this.values[index];
        this.values[index] = this.missingEntries;
        --this.mapSize;
        ++this.count;
        return previous;
    }

    /**
     * Put a value associated with a key in the map.
     * 
     * @param key
     *        key to which value is associated
     * @param value
     *        value to put in the map
     * @return previous value associated with the key
     */
    public T put(final int key, final T value) {
        int index = this.findInsertionIndex(key);
        T previous = this.missingEntries;
        boolean newMapping = true;
        if (index < 0) {
            index = changeIndexSign(index);
            previous = this.values[index];
            newMapping = false;
        }
        this.keys[index] = key;
        this.states[index] = FULL;
        this.values[index] = value;
        if (newMapping) {
            ++this.mapSize;
            if (this.shouldGrowTable()) {
                this.growTable();
            }
            ++this.count;
        }
        return previous;

    }

    /**
     * Grow the tables.
     */
    private void growTable() {

        final int oldLength = this.states.length;
        final int[] oldKeys = this.keys;
        final T[] oldValues = this.values;
        final byte[] oldStates = this.states;

        final int newLength = RESIZE_MULTIPLIER * oldLength;
        final int[] newKeys = new int[newLength];
        final T[] newValues = this.buildArray(newLength);
        final byte[] newStates = new byte[newLength];
        final int newMask = newLength - 1;
        for (int i = 0; i < oldLength; ++i) {
            if (oldStates[i] == FULL) {
                final int key = oldKeys[i];
                final int index = findInsertionIndex(newKeys, newStates, key, newMask);
                newKeys[index] = key;
                newValues[index] = oldValues[i];
                newStates[index] = FULL;
            }
        }

        this.mask = newMask;
        this.keys = newKeys;
        this.values = newValues;
        this.states = newStates;

    }

    /**
     * Check if tables should grow due to increased size.
     * 
     * @return true if tables should grow
     */
    private boolean shouldGrowTable() {
        return this.mapSize > (this.mask + 1) * LOAD_FACTOR;
    }

    /**
     * Compute the hash value of a key
     * 
     * @param key
     *        key to hash
     * @return hash value of the key
     */
    private static int hashOf(final int key) {
        final int h = key ^ ((key >>> 20) ^ (key >>> 12));
        return h ^ (h >>> 7) ^ (h >>> 4);
    }

    /**
     * Read a serialized object.
     * 
     * @param stream
     *        input stream
     * @throws IOException
     *         if object cannot be read
     * @throws ClassNotFoundException
     *         if the class corresponding
     *         to the serialized object cannot be found
     */
    private void readObject(final ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        this.count = 0;
    }

    /**
     * Build an array of elements.
     * 
     * @param length
     *        size of the array to build
     * @return a new array
     */
    @SuppressWarnings("unchecked")
    // field is of type T
        private
        T[]
            buildArray(final int length) {
        return (T[]) Array.newInstance(this.field.getRuntimeClass(), length);
    }

    // CHECKSTYLE: resume CommentRatio check
    // CHECKSTYLE: resume MagicNumber check

    /** Iterator class for the map. */
    public final class Iterator {

        /** -2 */
        private static final int C_N2 = -2;

        /** Reference modification count. */
        private final int referenceCount;

        /** Index of current element. */
        private int current;

        /** Index of next element. */
        private int next;

        /**
         * Simple constructor.
         */
        @SuppressWarnings("PMD.EmptyCatchBlock")
        private Iterator() {

            // preserve the modification count of the map to detect concurrent modifications later
            this.referenceCount = OpenIntToFieldHashMap.this.count;

            // initialize current index
            this.next = -1;
            try {
                this.advance();
            } catch (final NoSuchElementException nsee) {
                // ignored
            }

        }

        /**
         * Check if there is a next element in the map.
         * 
         * @return true if there is a next element
         */
        public boolean hasNext() {
            return this.next >= 0;
        }

        /**
         * Get the key of current entry.
         * 
         * @return key of current entry
         * @exception ConcurrentModificationException
         *            if the map is modified during iteration
         * @exception NoSuchElementException
         *            if there is no element left in the map
         */
        public int key() {
            if (this.referenceCount != OpenIntToFieldHashMap.this.count) {
                throw new ConcurrentModificationException();
            }
            if (this.current < 0) {
                throw new NoSuchElementException();
            }
            return OpenIntToFieldHashMap.this.keys[this.current];
        }

        /**
         * Get the value of current entry.
         * 
         * @return value of current entry
         * @exception ConcurrentModificationException
         *            if the map is modified during iteration
         * @exception NoSuchElementException
         *            if there is no element left in the map
         */
        public T value() {
            if (this.referenceCount != OpenIntToFieldHashMap.this.count) {
                throw new ConcurrentModificationException();
            }
            if (this.current < 0) {
                throw new NoSuchElementException();
            }
            return OpenIntToFieldHashMap.this.values[this.current];
        }

        /**
         * Advance iterator one step further.
         * 
         * @exception ConcurrentModificationException
         *            if the map is modified during iteration
         * @exception NoSuchElementException
         *            if there is no element left in the map
         */
        @SuppressWarnings("PMD.PreserveStackTrace")
        public void advance() {

            if (this.referenceCount != OpenIntToFieldHashMap.this.count) {
                throw new ConcurrentModificationException();
            }

            // advance on step
            this.current = this.next;

            // prepare next step
            try {
                this.next++;
                while (OpenIntToFieldHashMap.this.states[this.next] != FULL) {
                    this.next++;
                }
            } catch (final ArrayIndexOutOfBoundsException e) {
                // Exception
                this.next = C_N2;
                if (this.current < 0) {
                    // No element found
                    throw new NoSuchElementException();
                }
            }

        }

    }
}
