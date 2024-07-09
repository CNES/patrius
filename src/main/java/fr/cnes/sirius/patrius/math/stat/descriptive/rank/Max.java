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
package fr.cnes.sirius.patrius.math.stat.descriptive.rank;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.stat.descriptive.AbstractStorelessUnivariateStatistic;
import fr.cnes.sirius.patrius.math.util.MathUtils;

/**
 * Returns the maximum of the available values.
 * <p>
 * <ul>
 * <li>The result is <code>NaN</code> iff all values are <code>NaN</code> (i.e. <code>NaN</code> values have no impact
 * on the value of the statistic).</li>
 * <li>If any of the values equals <code>Double.POSITIVE_INFINITY</code>, the result is
 * <code>Double.POSITIVE_INFINITY.</code></li>
 * </ul>
 * </p>
 * <p>
 * <strong>Note that this implementation is not synchronized.</strong> If multiple threads access an instance of this
 * class concurrently, and at least one of the threads invokes the <code>increment()</code> or <code>clear()</code>
 * method, it must be synchronized externally.
 * </p>
 * 
 * @version $Id: Max.java 18108 2017-10-04 06:45:27Z bignon $
 */
@SuppressWarnings("PMD.ShortClassName")
public class Max extends AbstractStorelessUnivariateStatistic implements Serializable {

    /** Serializable version identifier */
    private static final long serialVersionUID = -5593383832225844641L;

    /** Number of values that have been added */
    private long n;

    /** Current value of the statistic */
    private double value;

    /**
     * Create a Max instance
     */
    public Max() {
        super();
        this.n = 0;
        this.value = Double.NaN;
    }

    /**
     * Copy constructor, creates a new {@code Max} identical
     * to the {@code original}
     * 
     * @param original
     *        the {@code Max} instance to copy
     * @throws NullArgumentException
     *         if original is null
     */
    public Max(final Max original) {
        super();
        copy(original, this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void increment(final double d) {
        if (d > this.value || Double.isNaN(this.value)) {
            this.value = d;
        }
        this.n++;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        this.value = Double.NaN;
        this.n = 0;
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
     * Returns the maximum of the entries in the specified portion of
     * the input array, or <code>Double.NaN</code> if the designated subarray
     * is empty.
     * <p>
     * Throws <code>MathIllegalArgumentException</code> if the array is null or the array index parameters are not
     * valid.
     * </p>
     * <p>
     * <ul>
     * <li>The result is <code>NaN</code> iff all values are <code>NaN</code> (i.e. <code>NaN</code> values have no
     * impact on the value of the statistic).</li>
     * <li>If any of the values equals <code>Double.POSITIVE_INFINITY</code>, the result is
     * <code>Double.POSITIVE_INFINITY.</code></li>
     * </ul>
     * </p>
     * 
     * @param values
     *        the input array
     * @param begin
     *        index of the first array element to include
     * @param length
     *        the number of elements to include
     * @return the maximum of the values or Double.NaN if length = 0
     * @throws MathIllegalArgumentException
     *         if the array is null or the array index
     *         parameters are not valid
     */
    @Override
    public double evaluate(final double[] values, final int begin, final int length) {
        double max = Double.NaN;
        if (this.test(values, begin, length)) {
            max = values[begin];
            for (int i = begin; i < begin + length; i++) {
                if (!Double.isNaN(values[i])) {
                    max = (max > values[i]) ? max : values[i];
                }
            }
        }
        return max;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Max copy() {
        final Max result = new Max();
        // No try-catch or advertised exception because args are non-null
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
     *        Max to copy
     * @param dest
     *        Max to copy to
     * @throws NullArgumentException
     *         if either source or dest is null
     */
    public static void copy(final Max source, final Max dest) {
        MathUtils.checkNotNull(source);
        MathUtils.checkNotNull(dest);
        dest.setData(source.getDataRef());
        dest.n = source.n;
        dest.value = source.value;
    }
}
