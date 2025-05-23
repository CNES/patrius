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
package fr.cnes.sirius.patrius.math.stat.descriptive.rank;

import java.io.Serializable;
import java.util.Arrays;

import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;
import fr.cnes.sirius.patrius.math.stat.descriptive.AbstractUnivariateStatistic;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

//CHECKSTYLE: stop CommentRatio check
//Reason: model - Commons-Math code kept as such

/**
 * Provides percentile computation.
 * <p>
 * There are several commonly used methods for estimating percentiles (a.k.a. quantiles) based on sample data. For large
 * samples, the different methods agree closely, but when sample sizes are small, different methods will give
 * significantly different results. The algorithm implemented here works as follows:
 * <ol>
 * <li>Let <code>n</code> be the length of the (sorted) array and <code>0 < p <= 100</code> be 
 * the desired percentile.</li>
 * <li>If <code> n = 1 </code> return the unique array element (regardless of the value of 
 * <code>p</code>); otherwise</li>
 * <li>Compute the estimated percentile position <code> pos = p * (n + 1) / 100</code> and the difference,
 * <code>d</code> between <code>pos</code> and <code>floor(pos)</code> (i.e. the fractional 
 * part of <code>pos</code>).</li>
 * <li>If <code>pos < 1</code> return the smallest element in the array.</li>
 * <li>Else if <code>pos >= n</code> return the largest element in the array.</li>
 * <li>Else let <code>lower</code> be the element in position <code>floor(pos)</code> in the array and let
 * <code>upper</code> be the next element in the array. Return <code>lower + d * (upper - lower)</code></li>
 * </ol>
 * </p>
 * <p>
 * To compute percentiles, the data must be at least partially ordered. Input arrays are copied and recursively
 * partitioned using an ordering definition. The ordering used by <code>Arrays.sort(double[])</code> is the one
 * determined by {@link java.lang.Double#compareTo(Double)}. This ordering makes <code>Double.NaN</code> larger than any
 * other value (including <code>Double.POSITIVE_INFINITY</code>). Therefore, for example, the median (50th percentile)
 * of <code>{0, 1, 2, 3, 4, Double.NaN}</code> evaluates to <code>2.5.</code>
 * </p>
 * <p>
 * Since percentile estimation usually involves interpolation between array elements, arrays containing <code>NaN</code>
 * or infinite values will often result in <code>NaN</code> or infinite values returned.
 * </p>
 * <p>
 * Since 2.2, Percentile uses only selection instead of complete sorting and caches selection algorithm state between
 * calls to the various {@code evaluate} methods. This greatly improves efficiency, both for a single percentile and
 * multiple percentile computations. To maximize performance when multiple percentiles are computed based on the same
 * data, users should set the data array once using either one of the {@link #evaluate(double[], double)} or
 * {@link #setData(double[])} methods and thereafter {@link #evaluate(double)} with just the percentile provided.
 * </p>
 * <p>
 * <strong>Note that this implementation is not synchronized.</strong> If multiple threads access an instance of this
 * class concurrently, and at least one of the threads invokes the <code>increment()</code> or <code>clear()</code>
 * method, it must be synchronized externally.
 * </p>
 * 
 * @version $Id: Percentile.java 18108 2017-10-04 06:45:27Z bignon $
 */
@SuppressWarnings("PMD.NullAssignment")
public class Percentile extends AbstractUnivariateStatistic implements Serializable {

    /** 100. */
    private static final double ONE_HUNDRED = 100;

     /** Serializable UID. */
    private static final long serialVersionUID = -8091216485095130416L;

    /** Minimum size under which we use a simple insertion sort rather than Hoare's select. */
    private static final int MIN_SELECT_SIZE = 15;

    /** Maximum number of partitioning pivots cached (each level double the number of pivots). */
    private static final int MAX_CACHED_LEVELS = 10;

    /**
     * Determines what percentile is computed when evaluate() is activated
     * with no quantile argument
     */
    private double quantile = 0.0;

    /** Cached pivots. */
    private int[] cachedPivots;

    /**
     * Constructs a Percentile with a default quantile
     * value of 50.0.
     */
    public Percentile() {
        // No try-catch or advertised exception here - arg is valid
        this(ONE_HUNDRED / 2.);
    }

    /**
     * Constructs a Percentile with the specific quantile value.
     * 
     * @param p
     *        the quantile
     * @throws MathIllegalArgumentException
     *         if p is not greater than 0 and less
     *         than or equal to 100
     */
    public Percentile(final double p) {
        super();
        this.setQuantile(p);
        this.cachedPivots = null;
    }

    /**
     * Copy constructor, creates a new {@code Percentile} identical
     * to the {@code original}
     * 
     * @param original
     *        the {@code Percentile} instance to copy
     * @throws NullArgumentException
     *         if original is null
     */
    public Percentile(final Percentile original) {
        super();
        copy(original, this);
    }

    /** {@inheritDoc} */
    @Override
    public void setData(final double[] values) {
        if (values == null) {
            this.cachedPivots = null;
        } else {
            this.cachedPivots = new int[(0x1 << MAX_CACHED_LEVELS) - 1];
            Arrays.fill(this.cachedPivots, -1);
        }
        super.setData(values);
    }

    /** {@inheritDoc} */
    @Override
    public void setData(final double[] values, final int begin, final int length) {
        if (values == null) {
            this.cachedPivots = null;
        } else {
            this.cachedPivots = new int[(0x1 << MAX_CACHED_LEVELS) - 1];
            Arrays.fill(this.cachedPivots, -1);
        }
        super.setData(values, begin, length);
    }

    /**
     * Returns the result of evaluating the statistic over the stored data.
     * <p>
     * The stored array is the one which was set by previous calls to {@link #setData(double[])}
     * </p>
     * 
     * @param p
     *        the percentile value to compute
     * @return the value of the statistic applied to the stored data
     * @throws MathIllegalArgumentException
     *         if p is not a valid quantile value
     *         (p must be greater than 0 and less than or equal to 100)
     */
    public double evaluate(final double p) {
        return this.evaluate(this.getDataRef(), p);
    }

    /**
     * Returns an estimate of the <code>quantile</code>th percentile of the <code>values</code> array. The quantile
     * estimated is determined by
     * the <code>quantile</code> property.
     * <p>
     * <ul>
     * <li>Returns <code>Double.NaN</code> if <code>values</code> has length <code>0</code></li>
     * <li>Returns (for any value of <code>p</code>) <code>values[0]</code> if <code>values</code> has length
     * <code>1</code></li>
     * <li>Throws <code>IllegalArgumentException</code> if <code>values</code> is null</li>
     * </ul>
     * </p>
     * <p>
     * See {@link Percentile} for a description of the percentile estimation algorithm used.
     * </p>
     * 
     * @param values
     *        input array of values
     * @return the percentile value or Double.NaN if the array is empty
     * @throws IllegalArgumentException
     *         if <code>values</code> is null
     *         or p is invalid
     */
    @Override
    public double evaluate(final double[] values) {
        return this.evaluate(values, this.quantile);
    }

    /**
     * Returns an estimate of the <code>p</code>th percentile of the values
     * in the <code>values</code> array.
     * <p>
     * Calls to this method do not modify the internal <code>quantile</code> state of this statistic.
     * </p>
     * <p>
     * <ul>
     * <li>Returns <code>Double.NaN</code> if <code>values</code> has length <code>0</code></li>
     * <li>Returns (for any value of <code>p</code>) <code>values[0]</code> if <code>values</code> has length
     * <code>1</code></li>
     * <li>Throws <code>MathIllegalArgumentException</code> if <code>values</code> is null or p is not a valid quantile
     * value (p must be greater than 0 and less than or equal to 100)</li>
     * </ul>
     * </p>
     * <p>
     * See {@link Percentile} for a description of the percentile estimation algorithm used.
     * </p>
     * 
     * @param values
     *        input array of values
     * @param p
     *        the percentile value to compute
     * @return the percentile value or Double.NaN if the array is empty
     * @throws MathIllegalArgumentException
     *         if <code>values</code> is null
     *         or p is invalid
     */
    public double evaluate(final double[] values, final double p) {
        test(values, 0, 0);
        return this.evaluate(values, 0, values.length, p);
    }

    /**
     * Returns an estimate of the <code>quantile</code>th percentile of the
     * designated values in the <code>values</code> array. The quantile
     * estimated is determined by the <code>quantile</code> property.
     * <p>
     * <ul>
     * <li>Returns <code>Double.NaN</code> if <code>length = 0</code></li>
     * <li>Returns (for any value of <code>quantile</code>) <code>values[begin]</code> if <code>length = 1 </code></li>
     * <li>Throws <code>MathIllegalArgumentException</code> if <code>values</code> is null, or <code>start</code> or
     * <code>length</code> is invalid</li>
     * </ul>
     * </p>
     * <p>
     * See {@link Percentile} for a description of the percentile estimation algorithm used.
     * </p>
     * 
     * @param values
     *        the input array
     * @param start
     *        index of the first array element to include
     * @param length
     *        the number of elements to include
     * @return the percentile value
     * @throws MathIllegalArgumentException
     *         if the parameters are not valid
     * 
     */
    @Override
    public double evaluate(final double[] values, final int start, final int length) {
        return this.evaluate(values, start, length, this.quantile);
    }

    /**
     * Returns an estimate of the <code>p</code>th percentile of the values
     * in the <code>values</code> array, starting with the element in (0-based)
     * position <code>begin</code> in the array and including <code>length</code> values.
     * <p>
     * Calls to this method do not modify the internal <code>quantile</code> state of this statistic.
     * </p>
     * <p>
     * <ul>
     * <li>Returns <code>Double.NaN</code> if <code>length = 0</code></li>
     * <li>Returns (for any value of <code>p</code>) <code>values[begin]</code> if <code>length = 1 </code></li>
     * <li>Throws <code>MathIllegalArgumentException</code> if <code>values</code> is null , <code>begin</code> or
     * <code>length</code> is invalid, or <code>p</code> is not a valid quantile value (p must be greater than 0 and
     * less than or equal to 100)</li>
     * </ul>
     * </p>
     * <p>
     * See {@link Percentile} for a description of the percentile estimation algorithm used.
     * </p>
     * 
     * @param values
     *        array of input values
     * @param p
     *        the percentile to compute
     * @param begin
     *        the first (0-based) element to include in the computation
     * @param length
     *        the number of array elements to include
     * @return the percentile value
     * @throws MathIllegalArgumentException
     *         if the parameters are not valid or the
     *         input array is null
     */
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Commons-Math code kept as such
    public double evaluate(final double[] values, final int begin,
                           final int length, final double p) {
        // CHECKSTYLE: resume ReturnCount check

        test(values, begin, length);

        if ((p > ONE_HUNDRED) || (p <= 0)) {
            throw new OutOfRangeException(
                PatriusMessages.OUT_OF_BOUNDS_QUANTILE_VALUE, p, 0, ONE_HUNDRED);
        }
        if (length == 0) {
            return Double.NaN;
        }
        if (length == 1) {
            // always return single value for n = 1
            return values[begin];
        }
        final double n = length;
        final double pos = p * (n + 1) / ONE_HUNDRED;
        final double[] work;
        final int[] pivotsHeap;
        if (values == this.getDataRef()) {
            work = this.getDataRef();
            pivotsHeap = this.cachedPivots;
        } else {
            work = new double[length];
            System.arraycopy(values, begin, work, 0, length);
            pivotsHeap = new int[(0x1 << MAX_CACHED_LEVELS) - 1];
            Arrays.fill(pivotsHeap, -1);
        }

        // Check pos
        if (pos < 1) {
            return this.select(work, pivotsHeap, 0);
        }
        if (pos >= n) {
            return this.select(work, pivotsHeap, length - 1);
        }
        // Return result
        final double fpos = MathLib.floor(pos);
        final int intPos = (int) fpos;
        final double dif = pos - fpos;
        final double lower = this.select(work, pivotsHeap, intPos - 1);
        final double upper = this.select(work, pivotsHeap, intPos);
        return lower + dif * (upper - lower);
    }

    /**
     * Select the k<sup>th</sup> smallest element from work array
     * 
     * @param work
     *        work array (will be reorganized during the call)
     * @param pivotsHeap
     *        set of pivot index corresponding to elements that
     *        are already at their sorted location, stored as an implicit heap
     *        (i.e. a sorted binary tree stored in a flat array, where the
     *        children of a node at index n are at indices 2n+1 for the left
     *        child and 2n+2 for the right child, with 0-based indices)
     * @param k
     *        index of the desired element
     * @return k<sup>th</sup> smallest element
     */
    private double select(final double[] work, final int[] pivotsHeap, final int k) {

        int begin = 0;
        int end = work.length;
        int node = 0;

        while (end - begin > MIN_SELECT_SIZE) {

            final int pivot;
            if ((node < pivotsHeap.length) && (pivotsHeap[node] >= 0)) {
                // the pivot has already been found in a previous call
                // and the array has already been partitioned around it
                pivot = pivotsHeap[node];
            } else {
                // select a pivot and partition work array around it
                pivot = partition(work, begin, end, this.medianOf3(work, begin, end));
                if (node < pivotsHeap.length) {
                    pivotsHeap[node] = pivot;
                }
            }

            if (k == pivot) {
                // the pivot was exactly the element we wanted
                return work[k];
            } else if (k < pivot) {
                // the element is in the left partition
                end = pivot;
                // the min is here to avoid integer overflow
                node = MathLib.min(2 * node + 1, pivotsHeap.length);
            } else {
                // the element is in the right partition
                begin = pivot + 1;
                // the min is here to avoid integer overflow
                node = MathLib.min(2 * node + 2, pivotsHeap.length);
            }

        }

        // the element is somewhere in the small sub-array
        // sort the sub-array using insertion sort
        insertionSort(work, begin, end);
        return work[k];

    }

    /**
     * Select a pivot index as the median of three
     * 
     * @param work
     *        data array
     * @param begin
     *        index of the first element of the slice
     * @param end
     *        index after the last element of the slice
     * @return the index of the median element chosen between the
     *         first, the middle and the last element of the array slice
     */
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Commons-Math code kept as such
    public int medianOf3(final double[] work, final int begin, final int end) {
        // CHECKSTYLE: resume ReturnCount check

        final int inclusiveEnd = end - 1;
        final int middle = begin + (inclusiveEnd - begin) / 2;
        final double wBegin = work[begin];
        final double wMiddle = work[middle];
        final double wEnd = work[inclusiveEnd];

        if (wBegin < wMiddle) {
            if (wMiddle < wEnd) {
                return middle;
            }
            return (wBegin < wEnd) ? inclusiveEnd : begin;
        }
        if (wBegin < wEnd) {
            return begin;
        }
        return (wMiddle < wEnd) ? inclusiveEnd : middle;
    }

    /**
     * Partition an array slice around a pivot
     * <p>
     * Partitioning exchanges array elements such that all elements smaller than pivot are before it and all elements
     * larger than pivot are after it
     * </p>
     * 
     * @param work
     *        data array
     * @param begin
     *        index of the first element of the slice
     * @param end
     *        index after the last element of the slice
     * @param pivot
     *        initial index of the pivot
     * @return index of the pivot after partition
     */
    private static int partition(final double[] work, final int begin, final int end, final int pivot) {

        final double value = work[pivot];
        work[pivot] = work[begin];

        int i = begin + 1;
        int j = end - 1;
        while (i < j) {
            while ((i < j) && (work[j] > value)) {
                --j;
            }
            while ((i < j) && (work[i] < value)) {
                ++i;
            }

            if (i < j) {
                final double tmp = work[i];
                work[i++] = work[j];
                work[j--] = tmp;
            }
        }

        if ((i >= end) || (work[i] > value)) {
            --i;
        }
        work[begin] = work[i];
        work[i] = value;
        return i;

    }

    /**
     * Sort in place a (small) array slice using insertion sort
     * 
     * @param work
     *        array to sort
     * @param begin
     *        index of the first element of the slice to sort
     * @param end
     *        index after the last element of the slice to sort
     */
    @SuppressWarnings("PMD.AvoidArrayLoops")
    private static void insertionSort(final double[] work, final int begin, final int end) {
        for (int j = begin + 1; j < end; j++) {
            final double saved = work[j];
            int i = j - 1;
            while ((i >= begin) && (saved < work[i])) {
                work[i + 1] = work[i];
                i--;
            }
            work[i + 1] = saved;
        }
    }

    /**
     * Returns the value of the quantile field (determines what percentile is
     * computed when evaluate() is called with no quantile argument).
     * 
     * @return quantile
     */
    public double getQuantile() {
        return this.quantile;
    }

    /**
     * Sets the value of the quantile field (determines what percentile is
     * computed when evaluate() is called with no quantile argument).
     * 
     * @param p
     *        a value between 0 < p <= 100
     * @throws MathIllegalArgumentException
     *         if p is not greater than 0 and less
     *         than or equal to 100
     */
    public void setQuantile(final double p) {
        if (p <= 0 || p > ONE_HUNDRED) {
            throw new OutOfRangeException(
                PatriusMessages.OUT_OF_BOUNDS_QUANTILE_VALUE, p, 0, ONE_HUNDRED);
        }
        this.quantile = p;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Percentile copy() {
        final Percentile result = new Percentile();
        // No try-catch or advertised exception because args are guaranteed non-null
        copy(this, result);
        return result;
    }

    /**
     * Copies source to dest.
     * <p>
     * Neither source nor dest can be null.
     * </p>
     * 
     * @param source
     *        Percentile to copy
     * @param dest
     *        Percentile to copy to
     * @throws NullArgumentException
     *         if either source or dest is null
     */
    public static void copy(final Percentile source, final Percentile dest) {
        MathUtils.checkNotNull(source);
        MathUtils.checkNotNull(dest);
        dest.setData(source.getDataRef());
        if (source.cachedPivots != null) {
            System.arraycopy(source.cachedPivots, 0, dest.cachedPivots, 0, source.cachedPivots.length);
        }
        dest.quantile = source.quantile;
    }

    // CHECKSTYLE: resume CommentRatio check
}
