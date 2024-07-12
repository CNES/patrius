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

import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.stat.descriptive.AbstractStorelessUnivariateStatistic;
import fr.cnes.sirius.patrius.math.util.MathUtils;

/**
 * Returns the minimum of the available values.
 * <p>
 * <ul>
 * <li>The result is <code>NaN</code> iff all values are <code>NaN</code> (i.e. <code>NaN</code> values have no impact
 * on the value of the statistic).</li>
 * <li>If any of the values equals <code>Double.NEGATIVE_INFINITY</code>, the result is
 * <code>Double.NEGATIVE_INFINITY.</code></li>
 * </ul>
 * </p>
 * <p>
 * <strong>Note that this implementation is not synchronized.</strong> If multiple threads access an instance of this
 * class concurrently, and at least one of the threads invokes the <code>increment()</code> or <code>clear()</code>
 * method, it must be synchronized externally.
 * </p>
 * 
 * @version $Id: Min.java 18108 2017-10-04 06:45:27Z bignon $
 */
@SuppressWarnings("PMD.ShortClassName")
public class Min extends AbstractStorelessUnivariateStatistic implements Serializable {

     /** Serializable UID. */
    private static final long serialVersionUID = -2941995784909003131L;

    /** Number of values that have been added */
    private long n;

    /** Current value of the statistic */
    private double value;

    /**
     * Create a Min instance
     */
    public Min() {
        super();
        this.n = 0;
        this.value = Double.NaN;
    }

    /**
     * Copy constructor, creates a new {@code Min} identical
     * to the {@code original}
     * 
     * @param original
     *        the {@code Min} instance to copy
     * @throws NullArgumentException
     *         if original is null
     */
    public Min(final Min original) {
        super();
        copy(original, this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void increment(final double d) {
        if (d < this.value || Double.isNaN(this.value)) {
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
     * Returns the minimum of the entries in the specified portion of
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
     * <li>If any of the values equals <code>Double.NEGATIVE_INFINITY</code>, the result is
     * <code>Double.NEGATIVE_INFINITY.</code></li>
     * </ul>
     * </p>
     * 
     * @param values
     *        the input array
     * @param begin
     *        index of the first array element to include
     * @param length
     *        the number of elements to include
     * @return the minimum of the values or Double.NaN if length = 0
     * @throws MathIllegalArgumentException
     *         if the array is null or the array index
     *         parameters are not valid
     */
    @Override
    public double evaluate(final double[] values, final int begin, final int length) {
        double min = Double.NaN;
        if (test(values, begin, length)) {
            min = values[begin];
            for (int i = begin; i < begin + length; i++) {
                if (!Double.isNaN(values[i])) {
                    min = (min < values[i]) ? min : values[i];
                }
            }
        }
        return min;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Min copy() {
        final Min result = new Min();
        // No try-catch or advertised exception - args are non-null
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
     *        Min to copy
     * @param dest
     *        Min to copy to
     * @throws NullArgumentException
     *         if either source or dest is null
     */
    public static void copy(final Min source, final Min dest) {
        MathUtils.checkNotNull(source);
        MathUtils.checkNotNull(dest);
        dest.setData(source.getDataRef());
        dest.n = source.n;
        dest.value = source.value;
    }
}
