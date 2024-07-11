/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 *
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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:FA:FA-2113:15/05/2019:Usage des exceptions pour gerer un if/else dans Ellipsoid.getPointLocation()
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.stat.descriptive.summary;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.stat.descriptive.AbstractStorelessUnivariateStatistic;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;

/**
 * Returns the sum of the natural logs for this collection of values.
 * <p>
 * Uses {@link fr.cnes.sirius.patrius.math.util.MathLib#log(double)} to compute the logs. Therefore,
 * <ul>
 * <li>If any of values are &lt; 0, the result is <code>NaN.</code></li>
 * <li>If all values are non-negative and less than <code>Double.POSITIVE_INFINITY</code>, but at least one value is 0,
 * the result is <code>Double.NEGATIVE_INFINITY.</code></li>
 * <li>If both <code>Double.POSITIVE_INFINITY</code> and <code>Double.NEGATIVE_INFINITY</code> are among the values, the
 * result is <code>NaN.</code></li>
 * </ul>
 * </p>
 * <p>
 * <strong>Note that this implementation is not synchronized.</strong> If multiple threads access an instance of this
 * class concurrently, and at least one of the threads invokes the <code>increment()</code> or <code>clear()</code>
 * method, it must be synchronized externally.
 * </p>
 * 
 * @version $Id: SumOfLogs.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class SumOfLogs extends AbstractStorelessUnivariateStatistic implements Serializable {

     /** Serializable UID. */
    private static final long serialVersionUID = -370076995648386763L;

    /** Number of values that have been added */
    private int n;

    /**
     * The currently running value
     */
    private double value;

    /**
     * Create a SumOfLogs instance
     */
    public SumOfLogs() {
        super();
        this.value = 0d;
        this.n = 0;
    }

    /**
     * Copy constructor, creates a new {@code SumOfLogs} identical
     * to the {@code original}
     * 
     * @param original
     *        the {@code SumOfLogs} instance to copy
     * @throws NullArgumentException
     *         if original is null
     */
    public SumOfLogs(final SumOfLogs original) {
        super();
        copy(original, this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void increment(final double d) {
        if (d >= 0. && !Double.isNaN(d)) {
            this.value += MathLib.log(d);
        } else {
            this.value += Double.NaN;
        }
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
        this.value = 0d;
        this.n = 0;
    }

    /**
     * Returns the sum of the natural logs of the entries in the specified portion of
     * the input array, or <code>Double.NaN</code> if the designated subarray
     * is empty.
     * <p>
     * Throws <code>MathIllegalArgumentException</code> if the array is null.
     * </p>
     * <p>
     * See {@link SumOfLogs}.
     * </p>
     * 
     * @param values
     *        the input array
     * @param begin
     *        index of the first array element to include
     * @param length
     *        the number of elements to include
     * @return the sum of the natural logs of the values or 0 if
     *         length = 0
     * @throws MathIllegalArgumentException
     *         if the array is null or the array index
     *         parameters are not valid
     */
    @Override
    public double evaluate(final double[] values, final int begin, final int length) {
        // initialize sumLog with NaN value
        double sumLog = Double.NaN;
        if (test(values, begin, length, true)) {
            sumLog = 0.0;
            // evaluate each value with log
            for (int i = begin; i < begin + length; i++) {
                if (values[i] >= 0. && !Double.isNaN(values[i])) {
                    sumLog += MathLib.log(values[i]);
                } else {
                    sumLog += Double.NaN;
                }
            }
        }
        // return the sum
        return sumLog;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SumOfLogs copy() {
        final SumOfLogs result = new SumOfLogs();
        // No try-catch or advertised exception here because args are valid
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
     *        SumOfLogs to copy
     * @param dest
     *        SumOfLogs to copy to
     * @throws NullArgumentException
     *         if either source or dest is null
     */
    public static void copy(final SumOfLogs source, final SumOfLogs dest) {
        MathUtils.checkNotNull(source);
        MathUtils.checkNotNull(dest);
        dest.setData(source.getDataRef());
        dest.n = source.n;
        dest.value = source.value;
    }
}
