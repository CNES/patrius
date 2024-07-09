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
package fr.cnes.sirius.patrius.math.stat.descriptive.moment;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.stat.descriptive.AbstractStorelessUnivariateStatistic;
import fr.cnes.sirius.patrius.math.util.MathUtils;

/**
 * Computes the first moment (arithmetic mean). Uses the definitional formula:
 * <p>
 * mean = sum(x_i) / n
 * </p>
 * <p>
 * where <code>n</code> is the number of observations.
 * </p>
 * <p>
 * To limit numeric errors, the value of the statistic is computed using the following recursive updating algorithm:
 * </p>
 * <p>
 * <ol>
 * <li>Initialize <code>m = </code> the first value</li>
 * <li>For each additional value, update using <br>
 * <code>m = m + (new value - m) / (number of observations)</code></li>
 * </ol>
 * </p>
 * <p>
 * Returns <code>Double.NaN</code> if the dataset is empty.
 * </p>
 * <p>
 * <strong>Note that this implementation is not synchronized.</strong> If multiple threads access an instance of this
 * class concurrently, and at least one of the threads invokes the <code>increment()</code> or <code>clear()</code>
 * method, it must be synchronized externally.
 * </p>
 * 
 * @version $Id: FirstMoment.java 18108 2017-10-04 06:45:27Z bignon $
 */
class FirstMoment extends AbstractStorelessUnivariateStatistic
    implements Serializable {

    /** Serializable version identifier */
    private static final long serialVersionUID = 6112755307178490473L;

    /** Count of values that have been added */
    protected long n;

    /** First moment of values that have been added */
    protected double m1;

    /**
     * Deviation of most recently added value from previous first moment.
     * Retained to prevent repeated computation in higher order moments.
     */
    protected double dev;

    /**
     * Deviation of most recently added value from previous first moment,
     * normalized by previous sample size. Retained to prevent repeated
     * computation in higher order moments
     */
    protected double nDev;

    /**
     * Create a FirstMoment instance
     */
    public FirstMoment() {
        super();
        this.n = 0;
        this.m1 = Double.NaN;
        this.dev = Double.NaN;
        this.nDev = Double.NaN;
    }

    /**
     * Copy constructor, creates a new {@code FirstMoment} identical
     * to the {@code original}
     * 
     * @param original
     *        the {@code FirstMoment} instance to copy
     * @throws NullArgumentException
     *         if original is null
     */
    public FirstMoment(final FirstMoment original) {
        super();
        copy(original, this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void increment(final double d) {
        if (this.n == 0) {
            this.m1 = 0.0;
        }
        this.n++;
        final double n0 = this.n;
        this.dev = d - this.m1;
        this.nDev = this.dev / n0;
        this.m1 += this.nDev;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        this.m1 = Double.NaN;
        this.n = 0;
        this.dev = Double.NaN;
        this.nDev = Double.NaN;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getResult() {
        return this.m1;
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
    public FirstMoment copy() {
        final FirstMoment result = new FirstMoment();
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
     *        FirstMoment to copy
     * @param dest
     *        FirstMoment to copy to
     * @throws NullArgumentException
     *         if either source or dest is null
     */
    public static void copy(final FirstMoment source, final FirstMoment dest) {
        MathUtils.checkNotNull(source);
        MathUtils.checkNotNull(dest);
        dest.setData(source.getDataRef());
        dest.n = source.n;
        dest.m1 = source.m1;
        dest.dev = source.dev;
        dest.nDev = source.nDev;
    }
}
