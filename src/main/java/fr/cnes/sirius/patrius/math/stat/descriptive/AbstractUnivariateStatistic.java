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
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.stat.descriptive;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.exception.NotPositiveException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooLargeException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Abstract base class for all implementations of the {@link UnivariateStatistic} interface.
 * <p>
 * Provides a default implementation of <code>evaluate(double[]),</code> delegating to
 * <code>evaluate(double[], int, int)</code> in the natural way.
 * </p>
 * <p>
 * Also includes a <code>test</code> method that performs generic parameter validation for the <code>evaluate</code>
 * methods.
 * </p>
 * 
 * @version $Id: AbstractUnivariateStatistic.java 18108 2017-10-04 06:45:27Z bignon $
 */
@SuppressWarnings("PMD.NullAssignment")
public abstract class AbstractUnivariateStatistic
    implements UnivariateStatistic {

    /** Stored data. */
    private double[] storedData;

    /**
     * Set the data array.
     * <p>
     * The stored value is a copy of the parameter array, not the array itself.
     * </p>
     * 
     * @param values
     *        data array to store (may be null to remove stored data)
     * @see #evaluate()
     */
    public void setData(final double[] values) {
        this.storedData = (values == null) ? null : values.clone();
    }

    /**
     * Get a copy of the stored data array.
     * 
     * @return copy of the stored data array (may be null)
     */
    public double[] getData() {
        return (this.storedData == null) ? null : this.storedData.clone();
    }

    /**
     * Get a reference to the stored data array.
     * 
     * @return reference to the stored data array (may be null)
     */
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    protected double[] getDataRef() {
        return this.storedData;
    }

    /**
     * Set the data array. The input array is copied, not referenced.
     * 
     * @param values
     *        data array to store
     * @param begin
     *        the index of the first element to include
     * @param length
     *        the number of elements to include
     * @throws MathIllegalArgumentException
     *         if values is null or the indices
     *         are not valid
     * @see #evaluate()
     */
    public void setData(final double[] values, final int begin, final int length) {
        if (values == null) {
            throw new NullArgumentException(PatriusMessages.INPUT_ARRAY);
        }

        if (begin < 0) {
            throw new NotPositiveException(PatriusMessages.START_POSITION, begin);
        }

        if (length < 0) {
            throw new NotPositiveException(PatriusMessages.LENGTH, length);
        }

        if (begin + length > values.length) {
            throw new NumberIsTooLargeException(PatriusMessages.SUBARRAY_ENDS_AFTER_ARRAY_END,
                begin + length, values.length, true);
        }
        this.storedData = new double[length];
        System.arraycopy(values, begin, this.storedData, 0, length);
    }

    /**
     * Returns the result of evaluating the statistic over the stored data.
     * <p>
     * The stored array is the one which was set by previous calls to {@link #setData(double[])}.
     * </p>
     * 
     * @return the value of the statistic applied to the stored data
     * @throws MathIllegalArgumentException
     *         if the stored data array is null
     */
    public double evaluate() {
        return this.evaluate(this.storedData);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double evaluate(final double[] values) {
        this.test(values, 0, 0);
        return this.evaluate(values, 0, values.length);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract double evaluate(final double[] values, final int begin, final int length);

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract UnivariateStatistic copy();

    /**
     * This method is used by <code>evaluate(double[], int, int)</code> methods
     * to verify that the input parameters designate a subarray of positive length.
     * <p>
     * <ul>
     * <li>returns <code>true</code> iff the parameters designate a subarray of positive length</li>
     * <li>throws <code>MathIllegalArgumentException</code> if the array is null or or the indices are invalid</li>
     * <li>returns <code>false</li> if the array is non-null, but <code>length</code> is 0.
     * </ul>
     * </p>
     * 
     * @param values
     *        the input array
     * @param begin
     *        index of the first array element to include
     * @param length
     *        the number of elements to include
     * @return true if the parameters are valid and designate a subarray of positive length
     * @throws MathIllegalArgumentException
     *         if the indices are invalid or the array is null
     */
    protected boolean test(
                           final double[] values,
                           final int begin,
                           final int length) {
        return this.test(values, begin, length, false);
    }

    /**
     * This method is used by <code>evaluate(double[], int, int)</code> methods
     * to verify that the input parameters designate a subarray of positive length.
     * <p>
     * <ul>
     * <li>returns <code>true</code> iff the parameters designate a subarray of non-negative length</li>
     * <li>throws <code>IllegalArgumentException</code> if the array is null or or the indices are invalid</li>
     * <li>returns <code>false</li> if the array is non-null, but <code>length</code> is 0 unless
     * <code>allowEmpty</code> is <code>true</code>
     * </ul>
     * </p>
     * 
     * @param values
     *        the input array
     * @param begin
     *        index of the first array element to include
     * @param length
     *        the number of elements to include
     * @param allowEmpty
     *        if <code>true</code> then zero length arrays are allowed
     * @return true if the parameters are valid
     * @throws MathIllegalArgumentException
     *         if the indices are invalid or the array is null
     * @since 3.0
     */
    protected boolean test(final double[] values, final int begin,
                           final int length, final boolean allowEmpty) {

        if (values == null) {
            throw new NullArgumentException(PatriusMessages.INPUT_ARRAY);
        }

        if (begin < 0) {
            throw new NotPositiveException(PatriusMessages.START_POSITION, begin);
        }

        if (length < 0) {
            throw new NotPositiveException(PatriusMessages.LENGTH, length);
        }

        if (begin + length > values.length) {
            throw new NumberIsTooLargeException(PatriusMessages.SUBARRAY_ENDS_AFTER_ARRAY_END,
                begin + length, values.length, true);
        }

        return (!(length == 0 && !allowEmpty));
    }

    /**
     * This method is used by <code>evaluate(double[], double[], int, int)</code> methods
     * to verify that the begin and length parameters designate a subarray of positive length
     * and the weights are all non-negative, non-NaN, finite, and not all zero.
     * <p>
     * <ul>
     * <li>returns <code>true</code> iff the parameters designate a subarray of positive length and the weights array
     * contains legitimate values.</li>
     * <li>throws <code>IllegalArgumentException</code> if any of the following are true:
     * <ul>
     * <li>the values array is null</li>
     * <li>the weights array is null</li>
     * <li>the weights array does not have the same length as the values array</li>
     * <li>the weights array contains one or more infinite values</li>
     * <li>the weights array contains one or more NaN values</li>
     * <li>the weights array contains negative values</li>
     * <li>the start and length arguments do not determine a valid array</li>
     * </ul>
     * </li>
     * <li>returns <code>false</li> if the array is non-null, but <code>length</code> is 0.
     * </ul>
     * </p>
     * 
     * @param values
     *        the input array
     * @param weights
     *        the weights array
     * @param begin
     *        index of the first array element to include
     * @param length
     *        the number of elements to include
     * @return true if the parameters are valid and designate a subarray of positive length
     * @throws MathIllegalArgumentException
     *         if the indices are invalid or the array is null
     * @since 2.1
     */
    protected boolean test(
                           final double[] values,
                           final double[] weights,
                           final int begin,
                           final int length) {
        return this.test(values, weights, begin, length, false);
    }

    /**
     * This method is used by <code>evaluate(double[], double[], int, int)</code> methods
     * to verify that the begin and length parameters designate a subarray of positive length
     * and the weights are all non-negative, non-NaN, finite, and not all zero.
     * <p>
     * <ul>
     * <li>returns <code>true</code> iff the parameters designate a subarray of non-negative length and the weights
     * array contains legitimate values.</li>
     * <li>throws <code>MathIllegalArgumentException</code> if any of the following are true:
     * <ul>
     * <li>the values array is null</li>
     * <li>the weights array is null</li>
     * <li>the weights array does not have the same length as the values array</li>
     * <li>the weights array contains one or more infinite values</li>
     * <li>the weights array contains one or more NaN values</li>
     * <li>the weights array contains negative values</li>
     * <li>the start and length arguments do not determine a valid array</li>
     * </ul>
     * </li>
     * <li>returns <code>false</li> if the array is non-null, but <code>length</code> is 0 unless
     * <code>allowEmpty</code> is <code>true</code>.
     * </ul>
     * </p>
     * 
     * @param values
     *        the input array.
     * @param weights
     *        the weights array.
     * @param begin
     *        index of the first array element to include.
     * @param length
     *        the number of elements to include.
     * @param allowEmpty
     *        if {@code true} than allow zero length arrays to pass.
     * @return {@code true} if the parameters are valid.
     * @throws NullArgumentException
     *         if either of the arrays are null
     * @throws MathIllegalArgumentException
     *         if the array indices are not valid,
     *         the weights array contains NaN, infinite or negative elements, or there
     *         are no positive weights.
     * @since 3.0
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Commons-Math code kept as such
    protected boolean test(final double[] values, final double[] weights,
                           final int begin, final int length, final boolean allowEmpty) {
        // CHECKSTYLE: resume CyclomaticComplexity check
        if (weights == null || values == null) {
            // Exception
            throw new NullArgumentException(PatriusMessages.INPUT_ARRAY);
        }

        if (weights.length != values.length) {
            // Exception
            throw new DimensionMismatchException(weights.length, values.length);
        }

        boolean containsPositiveWeight = false;
        for (int i = begin; i < begin + length; i++) {
            if (Double.isNaN(weights[i])) {
                throw new MathIllegalArgumentException(PatriusMessages.NAN_ELEMENT_AT_INDEX, i);
            }
            if (Double.isInfinite(weights[i])) {
                throw new MathIllegalArgumentException(PatriusMessages.INFINITE_ARRAY_ELEMENT, weights[i], i);
            }
            if (weights[i] < 0) {
                throw new MathIllegalArgumentException(PatriusMessages.NEGATIVE_ELEMENT_AT_INDEX, i, weights[i]);
            }
            if (!containsPositiveWeight && weights[i] > 0.0) {
                containsPositiveWeight = true;
            }
        }

        if (!containsPositiveWeight) {
            throw new MathIllegalArgumentException(PatriusMessages.WEIGHT_AT_LEAST_ONE_NON_ZERO);
        }

        // Return result
        return this.test(values, begin, length, allowEmpty);
    }
}
