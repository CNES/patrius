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
package fr.cnes.sirius.patrius.math.stat.descriptive.summary;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.stat.descriptive.AbstractStorelessUnivariateStatistic;
import fr.cnes.sirius.patrius.math.stat.descriptive.WeightedEvaluation;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;

/**
 * Returns the product of the available values.
 * <p>
 * If there are no values in the dataset, then 1 is returned. If any of the values are <code>NaN</code>, then
 * <code>NaN</code> is returned.
 * </p>
 * <p>
 * <strong>Note that this implementation is not synchronized.</strong> If multiple threads access an instance of this
 * class concurrently, and at least one of the threads invokes the <code>increment()</code> or <code>clear()</code>
 * method, it must be synchronized externally.
 * </p>
 * 
 * @version $Id: Product.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class Product extends AbstractStorelessUnivariateStatistic implements Serializable, WeightedEvaluation {

     /** Serializable UID. */
    private static final long serialVersionUID = 2824226005990582538L;

    /** The number of values that have been added */
    private long n;

    /**
     * The current Running Product.
     */
    private double value;

    /**
     * Create a Product instance
     */
    public Product() {
        super();
        this.n = 0;
        this.value = 1;
    }

    /**
     * Copy constructor, creates a new {@code Product} identical
     * to the {@code original}
     * 
     * @param original
     *        the {@code Product} instance to copy
     * @throws NullArgumentException
     *         if original is null
     */
    public Product(final Product original) {
        super();
        copy(original, this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void increment(final double d) {
        this.value *= d;
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
        this.value = 1;
        this.n = 0;
    }

    /**
     * Returns the product of the entries in the specified portion of
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
     * @return the product of the values or 1 if length = 0
     * @throws MathIllegalArgumentException
     *         if the array is null or the array index
     *         parameters are not valid
     */
    @Override
    public double evaluate(final double[] values, final int begin, final int length) {
        double product = Double.NaN;
        if (test(values, begin, length, true)) {
            product = 1.0;
            for (int i = begin; i < begin + length; i++) {
                product *= values[i];
            }
        }
        return product;
    }

    /**
     * <p>
     * Returns the weighted product of the entries in the specified portion of the input array, or
     * <code>Double.NaN</code> if the designated subarray is empty.
     * </p>
     * 
     * <p>
     * Throws <code>MathIllegalArgumentException</code> if any of the following are true:
     * <ul>
     * <li>the values array is null</li>
     * <li>the weights array is null</li>
     * <li>the weights array does not have the same length as the values array</li>
     * <li>the weights array contains one or more infinite values</li>
     * <li>the weights array contains one or more NaN values</li>
     * <li>the weights array contains negative values</li>
     * <li>the start and length arguments do not determine a valid array</li>
     * </ul>
     * </p>
     * 
     * <p>
     * Uses the formula,
     * 
     * <pre>
     *    weighted product = &prod;values[i]<sup>weights[i]</sup>
     * </pre>
     * 
     * that is, the weights are applied as exponents when computing the weighted product.
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
     * @return the product of the values or 1 if length = 0
     * @throws MathIllegalArgumentException
     *         if the parameters are not valid
     * @since 2.1
     */
    @Override
    public double evaluate(final double[] values, final double[] weights,
                           final int begin, final int length) {
        double product = Double.NaN;
        if (this.test(values, weights, begin, length, true)) {
            product = 1.0;
            for (int i = begin; i < begin + length; i++) {
                product *= MathLib.pow(values[i], weights[i]);
            }
        }
        return product;
    }

    /**
     * <p>
     * Returns the weighted product of the entries in the input array.
     * </p>
     * 
     * <p>
     * Throws <code>MathIllegalArgumentException</code> if any of the following are true:
     * <ul>
     * <li>the values array is null</li>
     * <li>the weights array is null</li>
     * <li>the weights array does not have the same length as the values array</li>
     * <li>the weights array contains one or more infinite values</li>
     * <li>the weights array contains one or more NaN values</li>
     * <li>the weights array contains negative values</li>
     * </ul>
     * </p>
     * 
     * <p>
     * Uses the formula,
     * 
     * <pre>
     *    weighted product = &prod;values[i]<sup>weights[i]</sup>
     * </pre>
     * 
     * that is, the weights are applied as exponents when computing the weighted product.
     * </p>
     * 
     * @param values
     *        the input array
     * @param weights
     *        the weights array
     * @return the product of the values or Double.NaN if length = 0
     * @throws MathIllegalArgumentException
     *         if the parameters are not valid
     * @since 2.1
     */
    @Override
    public double evaluate(final double[] values, final double[] weights) {
        return this.evaluate(values, weights, 0, values.length);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Product copy() {
        final Product result = new Product();
        // No try-catch or advertised exception because args are valid
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
     *        Product to copy
     * @param dest
     *        Product to copy to
     * @throws NullArgumentException
     *         if either source or dest is null
     */
    public static void copy(final Product source, final Product dest) {
        MathUtils.checkNotNull(source);
        MathUtils.checkNotNull(dest);
        dest.setData(source.getDataRef());
        dest.n = source.n;
        dest.value = source.value;
    }

}
