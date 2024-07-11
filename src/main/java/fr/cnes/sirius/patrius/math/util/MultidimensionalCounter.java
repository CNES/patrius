/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 *
 * Copyright 2011-2022 CNES
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
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2089:15/05/2019:[PATRIUS] passage a Java 8
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.util;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;

/**
 * Converter between unidimensional storage structure and multidimensional
 * conceptual structure.
 * This utility will convert from indices in a multidimensional structure
 * to the corresponding index in a one-dimensional array. For example,
 * assuming that the ranges (in 3 dimensions) of indices are 2, 4 and 3,
 * the following correspondences, between 3-tuples indices and unidimensional
 * indices, will hold:
 * <ul>
 * <li>(0, 0, 0) corresponds to 0</li>
 * <li>(0, 0, 1) corresponds to 1</li>
 * <li>(0, 0, 2) corresponds to 2</li>
 * <li>(0, 1, 0) corresponds to 3</li>
 * <li>...</li>
 * <li>(1, 0, 0) corresponds to 12</li>
 * <li>...</li>
 * <li>(1, 3, 2) corresponds to 23</li>
 * </ul>
 * 
 * @since 2.2
 * @version $Id: MultidimensionalCounter.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class MultidimensionalCounter implements Iterable<Integer> {
    /**
     * Number of dimensions.
     */
    private final int dimension;
    /**
     * Offset for each dimension.
     */
    private final int[] uniCounterOffset;
    /**
     * Counter sizes.
     */
    private final int[] size;
    /**
     * Total number of (one-dimensional) slots.
     */
    private final int totalSize;
    /**
     * Index of last dimension.
     */
    private final int last;

    /**
     * Create a counter.
     * 
     * @param sizeIn
     *        Counter sizes (number of slots in each dimension).
     * @throws NotStrictlyPositiveException
     *         if one of the sizes is
     *         negative or zero.
     */
    public MultidimensionalCounter(final int... sizeIn) {
        this.dimension = sizeIn.length;
        this.size = MathArrays.copyOf(sizeIn);

        this.uniCounterOffset = new int[this.dimension];

        this.last = this.dimension - 1;
        int tS = sizeIn[this.last];
        for (int i = 0; i < this.last; i++) {
            int count = 1;
            for (int j = i + 1; j < this.dimension; j++) {
                count *= sizeIn[j];
            }
            this.uniCounterOffset[i] = count;
            tS *= sizeIn[i];
        }
        this.uniCounterOffset[this.last] = 0;

        if (tS <= 0) {
            throw new NotStrictlyPositiveException(tS);
        }

        this.totalSize = tS;
    }

    /**
     * Create an iterator over this counter.
     * 
     * @return the iterator.
     */
    @Override
    public Iterator iterator() {
        return new Iterator();
    }

    /**
     * Get the number of dimensions of the multidimensional counter.
     * 
     * @return the number of dimensions.
     */
    public int getDimension() {
        return this.dimension;
    }

    /**
     * Convert to multidimensional counter.
     * 
     * @param index
     *        Index in unidimensional counter.
     * @return the multidimensional counts.
     * @throws OutOfRangeException
     *         if {@code index} is not between {@code 0} and the value returned by {@link #getSize()} (excluded).
     */
    public int[] getCounts(final int index) {
        if (index < 0 ||
                index >= this.totalSize) {
            // Exception
            throw new OutOfRangeException(index, 0, this.totalSize);
        }

        // Initialization
        final int[] indices = new int[this.dimension];

        int count = 0;
        for (int i = 0; i < this.last; i++) {
            int idx = 0;
            final int offset = this.uniCounterOffset[i];
            while (count <= index) {
                count += offset;
                ++idx;
            }
            --idx;
            count -= offset;
            indices[i] = idx;
        }

        // Last index
        indices[this.last] = index - count;

        // Return result
        return indices;
    }

    /**
     * Convert to unidimensional counter.
     * 
     * @param c
     *        Indices in multidimensional counter.
     * @return the index within the unidimensionl counter.
     * @throws DimensionMismatchException
     *         if the size of {@code c} does not match the size of the array given in the constructor.
     * @throws OutOfRangeException
     *         if a value of {@code c} is not in
     *         the range of the corresponding dimension, as defined in the
     *         {@link MultidimensionalCounter#MultidimensionalCounter(int...) constructor}.
     */
    public int getCount(final int... c) {
        if (c.length != this.dimension) {
            // raise an exception when the size are different
            throw new DimensionMismatchException(c.length, this.dimension);
        }
        // initialize count
        int count = 0;
        for (int i = 0; i < this.dimension; i++) {
            final int index = c[i];
            if (index < 0 ||
                    index >= this.size[i]) {
                // raise an exception if the index is out of range
                throw new OutOfRangeException(index, 0, this.size[i] - 1);
            }
            count += this.uniCounterOffset[i] * c[i];
        }
        return count + c[this.last];
    }

    /**
     * Get the total number of elements.
     * 
     * @return the total size of the unidimensional counter.
     */
    public int getSize() {
        return this.totalSize;
    }

    /**
     * Get the number of multidimensional counter slots in each dimension.
     * 
     * @return the sizes of the multidimensional counter in each dimension.
     */
    public int[] getSizes() {
        return MathArrays.copyOf(this.size);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < this.dimension; i++) {
            sb.append("[").append(this.getCount(i)).append("]");
        }
        return sb.toString();
    }

    /**
     * Perform iteration over the multidimensional counter.
     */
    public class Iterator implements java.util.Iterator<Integer> {
        /**
         * Multidimensional counter.
         */
        private final int[] counter = new int[MultidimensionalCounter.this.dimension];
        /**
         * Unidimensional counter.
         */
        private int count = -1;

        /**
         * Create an iterator
         * 
         * @see #iterator()
         */
        Iterator() {
            this.counter[MultidimensionalCounter.this.last] = -1;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasNext() {
            for (int i = 0; i < MultidimensionalCounter.this.dimension; i++) {
                if (this.counter[i] != MultidimensionalCounter.this.size[i] - 1) {
                    return true;
                }
            }
            return false;
        }

        /**
         * @return the unidimensional count after the counter has been
         *         incremented by {@code 1}.
         */
        @Override
        public Integer next() {
            for (int i = MultidimensionalCounter.this.last; i >= 0; i--) {
                if (this.counter[i] == MultidimensionalCounter.this.size[i] - 1) {
                    this.counter[i] = 0;
                } else {
                    ++this.counter[i];
                    break;
                }
            }

            return ++this.count;
        }

        /**
         * Get the current unidimensional counter slot.
         * 
         * @return the index within the unidimensionl counter.
         */
        public int getCount() {
            return this.count;
        }

        /**
         * Get the current multidimensional counter slots.
         * 
         * @return the indices within the multidimensional counter.
         */
        public int[] getCounts() {
            return MathArrays.copyOf(this.counter);
        }

        /**
         * Get the current count in the selected dimension.
         * 
         * @param dim
         *        Dimension index.
         * @return the count at the corresponding index for the current state
         *         of the iterator.
         * @throws IndexOutOfBoundsException
         *         if {@code index} is not in the
         *         correct interval (as defined by the length of the argument in the
         *         {@link MultidimensionalCounter#MultidimensionalCounter(int[])
         *         constructor of the enclosing class}).
         */
        public int getCount(final int dim) {
            return this.counter[dim];
        }

        /**
         * @throws UnsupportedOperationException thrown if the method is called
         */
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
