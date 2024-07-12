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

import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.MathInternalError;
import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;

/**
 * Utility to create <a href="http://en.wikipedia.org/wiki/Combination">
 * combinations</a> {@code (n, k)} of {@code k} elements in a set of {@code n} elements.
 * 
 * @version $Id: Combinations.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.1
 */
public class Combinations implements Iterable<int[]> {
    /** Size of the set from which combinations are drawn. */
    private final int n;
    /** Number of elements in each combination. */
    private final int k;
    /** Iteration order. */
    private final IterationOrder iterationOrder;

    /**
     * Describes the type of iteration performed by the {@link #iterator() iterator}.
     */
    private static enum IterationOrder {
        /** Lexicographic order. */
        LEXICOGRAPHIC
    }

    /**
     * Creates an instance whose range is the k-element subsets of
     * {0, ..., n - 1} represented as {@code int[]} arrays.
     * <p>
     * The iteration order is lexicographic: the arrays returned by the {@link #iterator() iterator} are sorted in
     * descending order and they are visited in lexicographic order with significance from right to left. For example,
     * {@code new Combinations(4, 2).iterator()} returns an iterator that will generate the following sequence of arrays
     * on successive calls to {@code next()}:<br/>
     * {@code [0, 1], [0, 2], [1, 2], [0, 3], [1, 3], [2, 3]}
     * </p>
     * If {@code k == 0} an iterator containing an empty array is returned;
     * if {@code k == n} an iterator containing [0, ..., n - 1] is returned.
     * 
     * @param nIn
     *        Size of the set from which subsets are selected.
     * @param kIn
     *        Size of the subsets to be enumerated.
     * @throws fr.cnes.sirius.patrius.math.exception.NotPositiveException
     *         if {@code n < 0}.
     * @throws fr.cnes.sirius.patrius.math.exception.NumberIsTooLargeException
     *         if {@code k > n}.
     */
    public Combinations(final int nIn,
        final int kIn) {
        this(nIn, kIn, IterationOrder.LEXICOGRAPHIC);
    }

    /**
     * Creates an instance whose range is the k-element subsets of
     * {0, ..., n - 1} represented as {@code int[]} arrays.
     * <p>
     * If the {@code iterationOrder} argument is set to {@link IterationOrder#LEXICOGRAPHIC}, the arrays returned by the
     * {@link #iterator() iterator} are sorted in descending order and they are visited in lexicographic order with
     * significance from right to left. For example, {@code new Combinations(4, 2).iterator()} returns an iterator that
     * will generate the following sequence of arrays on successive calls to {@code next()}:<br/>
     * {@code [0, 1], [0, 2], [1, 2], [0, 3], [1, 3], [2, 3]}
     * </p>
     * If {@code k == 0} an iterator containing an empty array is returned;
     * if {@code k == n} an iterator containing [0, ..., n - 1] is returned.
     * 
     * @param nIn
     *        Size of the set from which subsets are selected.
     * @param kIn
     *        Size of the subsets to be enumerated.
     * @param iterationOrderIn
     *        Specifies the {@link #iterator() iteration order}.
     * @throws fr.cnes.sirius.patrius.math.exception.NotPositiveException
     *         if {@code n < 0}.
     * @throws fr.cnes.sirius.patrius.math.exception.NumberIsTooLargeException
     *         if {@code k > n}.
     */
    private Combinations(final int nIn,
        final int kIn,
        final IterationOrder iterationOrderIn) {
        CombinatoricsUtils.checkBinomial(nIn, kIn);
        this.n = nIn;
        this.k = kIn;
        this.iterationOrder = iterationOrderIn;
    }

    /**
     * Gets the size of the set from which combinations are drawn.
     * 
     * @return the size of the universe.
     */
    public int getN() {
        return this.n;
    }

    /**
     * Gets the number of elements in each combination.
     * 
     * @return the size of the subsets to be enumerated.
     */
    public int getK() {
        return this.k;
    }

    /** {@inheritDoc} */
    @Override
    public Iterator<int[]> iterator() {
        if (this.k == 0 ||
            this.k == this.n) {
            return new SingletonIterator(MathArrays.natural(this.k));
        }

        switch (this.iterationOrder) {
            case LEXICOGRAPHIC:
                return new LexicographicIterator(this.n, this.k);
            default:
                // Should never happen.
                throw new MathInternalError();
        }
    }

    /**
     * Defines a lexicographic ordering of combinations.
     * The returned comparator allows to compare any two combinations
     * that can be produced by this instance's {@link #iterator() iterator}.
     * Its {@code compare(int[],int[])} method will throw exceptions if
     * passed combinations that are inconsistent with this instance:
     * <ul>
     * <li>{@code DimensionMismatchException} if the array lengths are not equal to {@code k},</li>
     * <li>{@code OutOfRangeException} if an element of the array is not within the interval [0, {@code n}).</li>
     * </ul>
     * 
     * @return a lexicographic comparator.
     */
    public Comparator<int[]> comparator() {
        return new LexicographicComparator(this.n, this.k);
    }

    /**
     * Lexicographic combinations iterator.
     * <p>
     * Implementation follows Algorithm T in <i>The Art of Computer Programming</i> Internet Draft (PRE-FASCICLE 3A), "A
     * Draft of Section 7.2.1.3 Generating All Combinations</a>, D. Knuth, 2004.
     * </p>
     * <p>
     * The degenerate cases {@code k == 0} and {@code k == n} are NOT handled by this implementation. If constructor
     * arguments satisfy {@code k == 0} or {@code k >= n}, no exception is generated, but the iterator is empty.
     * </p>
     * 
     */
    private static class LexicographicIterator implements Iterator<int[]> {
        /** Size of subsets returned by the iterator */
        private final int k;

        /**
         * c[1], ..., c[k] stores the next combination; c[k + 1], c[k + 2] are
         * sentinels.
         * <p>
         * Note that c[0] is "wasted" but this makes it a little easier to follow the code.
         * </p>
         */
        private final int[] c;

        /** Return value for {@link #hasNext()} */
        private boolean more = true;

        /** Marker: smallest index such that c[j + 1] > j */
        private int j;

        /**
         * Construct a CombinationIterator to enumerate k-sets from n.
         * <p>
         * NOTE: If {@code k === 0} or {@code k >= n}, the Iterator will be empty (that is, {@link #hasNext()} will
         * return {@code false} immediately.
         * </p>
         * 
         * @param n
         *        size of the set from which subsets are enumerated
         * @param kIn
         *        size of the subsets to enumerate
         */
        public LexicographicIterator(final int n, final int kIn) {
            this.k = kIn;
            this.c = new int[kIn + 3];
            if (kIn == 0 || kIn >= n) {
                this.more = false;
                return;
            }
            // Initialize c to start with lexicographically first k-set
            for (int i = 1; i <= kIn; i++) {
                this.c[i] = i - 1;
            }
            // Initialize sentinels
            this.c[kIn + 1] = n;
            this.c[kIn + 2] = 0;
            // Set up invariant: j is smallest index such that c[j + 1] > j
            this.j = kIn;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasNext() {
            return this.more;
        }

        /**
         * {@inheritDoc}
         */
        // CHECKSTYLE: stop ReturnCount check
        // Reason: Commons-Math code kept as such
        @Override
        public int[] next() {
            // CHECKSTYLE: resume ReturnCount check
            if (!this.more) {
                throw new NoSuchElementException();
            }
            // Copy return value (prepared by last activation)
            final int[] ret = new int[this.k];
            System.arraycopy(this.c, 1, ret, 0, this.k);

            // Prepare next iteration
            // T2 and T6 loop
            int x = 0;
            if (this.j > 0) {
                x = this.j;
                this.c[this.j] = x;
                this.j--;
                return ret;
            }
            // T3
            if (this.c[1] + 1 < this.c[2]) {
                this.c[1]++;
                return ret;
            } else {
                this.j = 2;
            }
            // T4
            boolean stepDone = false;
            while (!stepDone) {
                this.c[this.j - 1] = this.j - 2;
                x = this.c[this.j] + 1;
                if (x == this.c[this.j + 1]) {
                    this.j++;
                } else {
                    stepDone = true;
                }
            }
            // T5
            if (this.j > this.k) {
                this.more = false;
                return ret;
            }
            // T6
            this.c[this.j] = x;
            this.j--;
            return ret;
        }

        /**
         * Not supported.
         */
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Iterator with just one element to handle degenerate cases (full array,
     * empty array) for combination iterator.
     */
    private static class SingletonIterator implements Iterator<int[]> {
        /** Singleton array */
        private final int[] singleton;
        /** True on initialization, false after first call to next */
        private boolean more = true;

        /**
         * Create a singleton iterator providing the given array.
         * 
         * @param singletonIn
         *        array returned by the iterator
         */
        public SingletonIterator(final int[] singletonIn) {
            this.singleton = singletonIn;
        }

        /** @return True until next is called the first time, then false */
        @Override
        public boolean hasNext() {
            return this.more;
        }

        /** @return the singleton in first activation; throws NSEE thereafter */
        @Override
        @SuppressWarnings("PMD.MethodReturnsInternalArray")
        public int[] next() {
            if (this.more) {
                this.more = false;
                return this.singleton;
            } else {
                throw new NoSuchElementException();
            }
        }

        /** Not supported */
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Defines the lexicographic ordering of combinations, using
     * the {@link #lexNorm(int[])} method.
     */
    private static class LexicographicComparator
        implements Comparator<int[]>, Serializable {
         /** Serializable UID. */
        private static final long serialVersionUID = 20130906L;
        /** Size of the set from which combinations are drawn. */
        private final int n;
        /** Number of elements in each combination. */
        private final int k;

        /**
         * @param nIn
         *        Size of the set from which subsets are selected.
         * @param kIn
         *        Size of the subsets to be enumerated.
         */
        public LexicographicComparator(final int nIn,
            final int kIn) {
            this.n = nIn;
            this.k = kIn;
        }

        /**
         * {@inheritDoc}
         * 
         * @throws DimensionMismatchException
         *         if the array lengths are not
         *         equal to {@code k}.
         * @throws OutOfRangeException
         *         if an element of the array is not
         *         within the interval [0, {@code n}).
         */
        @Override
        public int compare(final int[] c1, final int[] c2) {
            // Checks
            if (c1.length != this.k) {
                throw new DimensionMismatchException(c1.length, this.k);
            }
            if (c2.length != this.k) {
                throw new DimensionMismatchException(c2.length, this.k);
            }

            // Method "lexNorm" works with ordered arrays.
            final int[] c1s = MathArrays.copyOf(c1);
            Arrays.sort(c1s);
            final int[] c2s = MathArrays.copyOf(c2);
            Arrays.sort(c2s);

            final long v1 = this.lexNorm(c1s);
            final long v2 = this.lexNorm(c2s);

            // Return result
            final int res;
            if (v1 < v2) {
                res = -1;
            } else if (v1 > v2) {
                res = 1;
            } else {
                res = 0;
            }
            // Result
            return res;
        }

        /**
         * Computes the value (in base 10) represented by the digit
         * (interpreted in base {@code n}) in the input array in reverse
         * order.
         * For example if {@code c} is {@code 3, 2, 1} , and {@code n} is 3, the method will return 18.
         * 
         * @param c
         *        Input array.
         * @return the lexicographic norm.
         * @throws OutOfRangeException
         *         if an element of the array is not
         *         within the interval [0, {@code n}).
         */
        private long lexNorm(final int[] c) {
            long ret = 0;
            for (int i = 0; i < c.length; i++) {
                final int digit = c[i];
                if (digit < 0 ||
                    digit >= this.n) {
                    throw new OutOfRangeException(digit, 0, this.n - 1);
                }

                ret += c[i] * ArithmeticUtils.pow(this.n, i);
            }
            return ret;
        }
    }
}
