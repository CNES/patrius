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
package fr.cnes.sirius.patrius.math.stat.descriptive;

import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * 
 * Abstract implementation of the {@link StorelessUnivariateStatistic} interface.
 * <p>
 * Provides default <code>evaluate()</code> and <code>incrementAll(double[])</code> implementations.
 * </p>
 * <p>
 * <strong>Note that these implementations are not synchronized.</strong>
 * </p>
 * 
 * @version $Id: AbstractStorelessUnivariateStatistic.java 18108 2017-10-04 06:45:27Z bignon $
 */
public abstract class AbstractStorelessUnivariateStatistic
    extends AbstractUnivariateStatistic
    implements StorelessUnivariateStatistic {

    /** Hashcode generator. */
    private static final int HASH_CODE_GENERATOR = 31;

    /**
     * This default implementation calls {@link #clear}, then invokes {@link #increment} in a loop over the the input
     * array, and then uses {@link #getResult} to compute the return value.
     * <p>
     * Note that this implementation changes the internal state of the statistic. Its side effects are the same as
     * invoking {@link #clear} and then {@link #incrementAll(double[])}.
     * </p>
     * <p>
     * Implementations may override this method with a more efficient and possibly more accurate implementation that
     * works directly with the input array.
     * </p>
     * <p>
     * If the array is null, a MathIllegalArgumentException is thrown.
     * </p>
     * 
     * @param values
     *        input array
     * @return the value of the statistic applied to the input array
     * @throws MathIllegalArgumentException
     *         if values is null
     * @see fr.cnes.sirius.patrius.math.stat.descriptive.UnivariateStatistic#evaluate(double[])
     */
    @Override
    public double evaluate(final double[] values) {
        if (values == null) {
            throw new NullArgumentException(PatriusMessages.INPUT_ARRAY);
        }
        return this.evaluate(values, 0, values.length);
    }

    /**
     * This default implementation calls {@link #clear}, then invokes {@link #increment} in a loop over the specified
     * portion of the input
     * array, and then uses {@link #getResult} to compute the return value.
     * <p>
     * Note that this implementation changes the internal state of the statistic. Its side effects are the same as
     * invoking {@link #clear} and then {@link #incrementAll(double[], int, int)}.
     * </p>
     * <p>
     * Implementations may override this method with a more efficient and possibly more accurate implementation that
     * works directly with the input array.
     * </p>
     * <p>
     * If the array is null or the index parameters are not valid, an MathIllegalArgumentException is thrown.
     * </p>
     * 
     * @param values
     *        the input array
     * @param begin
     *        the index of the first element to include
     * @param length
     *        the number of elements to include
     * @return the value of the statistic applied to the included array entries
     * @throws MathIllegalArgumentException
     *         if the array is null or the indices are not valid
     * @see fr.cnes.sirius.patrius.math.stat.descriptive.UnivariateStatistic#evaluate(double[], int, int)
     */
    @Override
    public double evaluate(final double[] values, final int begin,
                           final int length) {
        if (this.test(values, begin, length)) {
            this.clear();
            this.incrementAll(values, begin, length);
        }
        return this.getResult();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract StorelessUnivariateStatistic copy();

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract void clear();

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract double getResult();

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract void increment(final double d);

    /**
     * This default implementation just calls {@link #increment} in a loop over
     * the input array.
     * <p>
     * Throws IllegalArgumentException if the input values array is null.
     * </p>
     * 
     * @param values
     *        values to add
     * @throws MathIllegalArgumentException
     *         if values is null
     * @see fr.cnes.sirius.patrius.math.stat.descriptive.StorelessUnivariateStatistic#incrementAll(double[])
     */
    @Override
    public void incrementAll(final double[] values) {
        if (values == null) {
            throw new NullArgumentException(PatriusMessages.INPUT_ARRAY);
        }
        this.incrementAll(values, 0, values.length);
    }

    /**
     * This default implementation just calls {@link #increment} in a loop over
     * the specified portion of the input array.
     * <p>
     * Throws IllegalArgumentException if the input values array is null.
     * </p>
     * 
     * @param values
     *        array holding values to add
     * @param begin
     *        index of the first array element to add
     * @param length
     *        number of array elements to add
     * @throws MathIllegalArgumentException
     *         if values is null
     * @see fr.cnes.sirius.patrius.math.stat.descriptive.StorelessUnivariateStatistic#incrementAll(double[], int, int)
     */
    @Override
    public void incrementAll(final double[] values, final int begin,
                             final int length) {
        if (this.test(values, begin, length)) {
            final int k = begin + length;
            for (int i = begin; i < k; i++) {
                this.increment(values[i]);
            }
        }
    }

    /**
     * Returns true iff <code>object</code> is an <code>AbstractStorelessUnivariateStatistic</code> returning the same
     * values as this for <code>getResult()</code> and <code>getN()</code>
     * 
     * @param object
     *        object to test equality against.
     * @return true if object returns the same value as this
     */
    @Override
    public boolean equals(final Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof AbstractStorelessUnivariateStatistic)) {
            return false;
        }
        // CHECKSTYLE: stop IllegalType check
        // Reason: Commons-Math code kept as such
        final AbstractStorelessUnivariateStatistic stat = (AbstractStorelessUnivariateStatistic) object;
        return Precision.equalsIncludingNaN(stat.getResult(), this.getResult()) &&
            Precision.equalsIncludingNaN(stat.getN(), this.getN());
        // CHECKSTYLE: resume IllegalType check
    }

    /**
     * Returns hash code based on getResult() and getN()
     * 
     * @return hash code
     */
    @Override
    public int hashCode() {
        return HASH_CODE_GENERATOR * (HASH_CODE_GENERATOR + MathUtils.hash(this.getResult()))
            + MathUtils.hash(this.getN());
    }

}
