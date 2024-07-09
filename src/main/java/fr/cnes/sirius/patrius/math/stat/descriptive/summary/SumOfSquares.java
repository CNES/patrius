/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
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
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.cnes.sirius.patrius.math.stat.descriptive.summary;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.stat.descriptive.AbstractStorelessUnivariateStatistic;
import fr.cnes.sirius.patrius.math.util.MathUtils;

/**
 * Returns the sum of the squares of the available values.
 * <p>
 * If there are no values in the dataset, then 0 is returned. If any of the values are <code>NaN</code>, then
 * <code>NaN</code> is returned.
 * </p>
 * <p>
 * <strong>Note that this implementation is not synchronized.</strong> If multiple threads access an instance of this
 * class concurrently, and at least one of the threads invokes the <code>increment()</code> or <code>clear()</code>
 * method, it must be synchronized externally.
 * </p>
 * 
 * @version $Id: SumOfSquares.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class SumOfSquares extends AbstractStorelessUnivariateStatistic implements Serializable {

    /** Serializable version identifier */
    private static final long serialVersionUID = 1460986908574398008L;

    /** */
    private long n;

    /**
     * The currently running sumSq
     */
    private double value;

    /**
     * Create a SumOfSquares instance
     */
    public SumOfSquares() {
        super();
        this.n = 0;
        this.value = 0;
    }

    /**
     * Copy constructor, creates a new {@code SumOfSquares} identical
     * to the {@code original}
     * 
     * @param original
     *        the {@code SumOfSquares} instance to copy
     * @throws NullArgumentException
     *         if original is null
     */
    public SumOfSquares(final SumOfSquares original) {
        super();
        copy(original, this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void increment(final double d) {
        this.value += d * d;
        this.n++;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getResult() {
        return this.value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getN() {
        return this.n;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        this.value = 0;
        this.n = 0;
    }

    /**
     * Returns the sum of the squares of the entries in the specified portion of
     * the input array, or <code>Double.NaN</code> if the designated subarray
     * is empty.
     * <p>
     * Throws <code>MathIllegalArgumentException</code> if the array is null.
     * </p>
     * 
     * @param values
     *        the input array
     * @param begin
     *        index of the first array element to include
     * @param length
     *        the number of elements to include
     * @return the sum of the squares of the values or 0 if length = 0
     * @throws MathIllegalArgumentException
     *         if the array is null or the array index
     *         parameters are not valid
     */
    @Override
    public double evaluate(final double[] values, final int begin, final int length) {
        double sumSq = Double.NaN;
        if (this.test(values, begin, length, true)) {
            sumSq = 0.0;
            for (int i = begin; i < begin + length; i++) {
                sumSq += values[i] * values[i];
            }
        }
        return sumSq;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SumOfSquares copy() {
        final SumOfSquares result = new SumOfSquares();
        // no try-catch or advertised exception here because args are valid
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
     *        SumOfSquares to copy
     * @param dest
     *        SumOfSquares to copy to
     * @throws NullArgumentException
     *         if either source or dest is null
     */
    public static void copy(final SumOfSquares source, final SumOfSquares dest) {
        MathUtils.checkNotNull(source);
        MathUtils.checkNotNull(dest);
        dest.setData(source.getDataRef());
        dest.n = source.n;
        dest.value = source.value;
    }

}
