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
package fr.cnes.sirius.patrius.math.stat.descriptive.moment;

import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.util.MathUtils;

/**
 * Computes a statistic related to the Second Central Moment. Specifically,
 * what is computed is the sum of squared deviations from the sample mean.
 * <p>
 * The following recursive updating formula is used:
 * </p>
 * <p>
 * Let
 * <ul>
 * <li>dev = (current obs - previous mean)</li>
 * <li>n = number of observations (including current obs)</li>
 * </ul>
 * Then
 * </p>
 * <p>
 * new value = old value + dev^2 * (n -1) / n.
 * </p>
 * <p>
 * Returns <code>Double.NaN</code> if no data values have been added and returns <code>0</code> if there is just one
 * value in the data set.
 * </p>
 * <p>
 * <strong>Note that this implementation is not synchronized.</strong> If multiple threads access an instance of this
 * class concurrently, and at least one of the threads invokes the <code>increment()</code> or <code>clear()</code>
 * method, it must be synchronized externally.
 * </p>
 * 
 * @version $Id: SecondMoment.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class SecondMoment extends FirstMoment {

     /** Serializable UID. */
    private static final long serialVersionUID = 3942403127395076445L;

    /** second moment of values that have been added */
    protected double m2;

    /**
     * Create a SecondMoment instance
     */
    public SecondMoment() {
        super();
        this.m2 = Double.NaN;
    }

    /**
     * Copy constructor, creates a new {@code SecondMoment} identical
     * to the {@code original}
     * 
     * @param original
     *        the {@code SecondMoment} instance to copy
     * @throws NullArgumentException
     *         if original is null
     */
    public SecondMoment(final SecondMoment original) {
        super(original);
        this.m2 = original.m2;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void increment(final double d) {
        if (this.n < 1) {
            this.m1 = 0.0;
            this.m2 = 0.0;
        }
        super.increment(d);
        this.m2 += ((double) this.n - 1) * this.dev * this.nDev;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        super.clear();
        this.m2 = Double.NaN;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getResult() {
        return this.m2;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SecondMoment copy() {
        final SecondMoment result = new SecondMoment();
        // no try-catch or advertised NAE because args are guaranteed non-null
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
     *        SecondMoment to copy
     * @param dest
     *        SecondMoment to copy to
     * @throws NullArgumentException
     *         if either source or dest is null
     */
    public static void copy(final SecondMoment source, final SecondMoment dest) {
        MathUtils.checkNotNull(source);
        MathUtils.checkNotNull(dest);
        FirstMoment.copy(source, dest);
        dest.m2 = source.m2;
    }

}
