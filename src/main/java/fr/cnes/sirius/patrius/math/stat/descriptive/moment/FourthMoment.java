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
 * Computes a statistic related to the Fourth Central Moment. Specifically,
 * what is computed is the sum of
 * <p>
 * (x_i - xbar) ^ 4,
 * </p>
 * <p>
 * where the x_i are the sample observations and xbar is the sample mean.
 * </p>
 * <p>
 * The following recursive updating formula is used:
 * </p>
 * <p>
 * Let
 * <ul>
 * <li>dev = (current obs - previous mean)</li>
 * <li>m2 = previous value of {@link SecondMoment}</li>
 * <li>m2 = previous value of {@link ThirdMoment}</li>
 * <li>n = number of observations (including current obs)</li>
 * </ul>
 * Then
 * </p>
 * <p>
 * new value = old value - 4 * (dev/n) * m3 + 6 * (dev/n)^2 * m2 + <br>
 * [n^2 - 3 * (n-1)] * dev^4 * (n-1) / n^3
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
 * @version $Id: FourthMoment.java 18108 2017-10-04 06:45:27Z bignon $
 */
class FourthMoment extends ThirdMoment {

     /** Serializable UID. */
    private static final long serialVersionUID = 4763990447117157611L;

    /** fourth moment of values that have been added */
    private double m4;

    /**
     * Create a FourthMoment instance
     */
    public FourthMoment() {
        super();
        this.m4 = Double.NaN;
    }

    /**
     * Copy constructor, creates a new {@code FourthMoment} identical
     * to the {@code original}
     * 
     * @param original
     *        the {@code FourthMoment} instance to copy
     * @throws NullArgumentException
     *         if original is null
     */
    public FourthMoment(final FourthMoment original) {
        super();
        copy(original, this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void increment(final double d) {
        if (this.n < 1) {
            this.m4 = 0.0;
            this.m3 = 0.0;
            this.m2 = 0.0;
            this.m1 = 0.0;
        }

        final double prevM3 = this.m3;
        final double prevM2 = this.m2;

        super.increment(d);

        final double n0 = this.n;

        this.m4 = this.m4 - 4.0 * this.nDev * prevM3 + 6.0 * this.nDevSq * prevM2 +
            ((n0 * n0) - 3 * (n0 - 1)) * (this.nDevSq * this.nDevSq * (n0 - 1) * n0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getResult() {
        return this.m4;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        super.clear();
        this.m4 = Double.NaN;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FourthMoment copy() {
        final FourthMoment result = new FourthMoment();
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
     *        FourthMoment to copy
     * @param dest
     *        FourthMoment to copy to
     * @throws NullArgumentException
     *         if either source or dest is null
     */
    public static void copy(final FourthMoment source, final FourthMoment dest) {
        MathUtils.checkNotNull(source);
        MathUtils.checkNotNull(dest);
        ThirdMoment.copy(source, dest);
        dest.m4 = source.m4;
    }
}
