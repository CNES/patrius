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
 * 
 * @history created 16/11/17
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1305:16/11/2017: Serializable interface implementation
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.util;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.exception.MaxCountExceededException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;

/**
 * Utility that increments a counter until a maximum is reached, at
 * which point, the instance will by default throw a {@link MaxCountExceededException}.
 * However, the user is able to override this behaviour by defining a
 * custom {@link MaxCountExceededCallback callback}, in order to e.g.
 * select which exception must be thrown.
 * 
 * @since 3.0
 * @version $Id: Incrementor.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class Incrementor implements Serializable {

    /**
     * Serializable UID.
     */
    private static final long serialVersionUID = 2770477549206014684L;
    /**
     * Upper limit for the counter.
     */
    private int maximalCount;
    /**
     * Current count.
     */
    private int count = 0;
    /**
     * Function called at counter exhaustion.
     */
    private final MaxCountExceededCallback maxCountCallback;

    /**
     * Default constructor.
     * For the new instance to be useful, the maximal count must be set
     * by calling {@link #setMaximalCount(int) setMaximalCount}.
     */
    public Incrementor() {
        this(0);
    }

    /**
     * Defines a maximal count.
     * 
     * @param max
     *        Maximal count.
     */
    public Incrementor(final int max) {
        this(max,
            new MaxCountExceededCallback(){

                /** UID. */
                private static final long serialVersionUID = 2406736358117951207L;

                /** {@inheritDoc} */
                @Override
                public void trigger(final int max) {
                    throw new MaxCountExceededException(max);
                }
            });
    }

    /**
     * Defines a maximal count and a callback method to be triggered at
     * counter exhaustion.
     * 
     * @param max
     *        Maximal count.
     * @param cb
     *        Function to be called when the maximal count has been reached.
     * @throws NullArgumentException
     *         if {@code cb} is {@code null}
     */
    public Incrementor(final int max,
        final MaxCountExceededCallback cb) {
        if (cb == null) {
            throw new NullArgumentException();
        }
        this.maximalCount = max;
        this.maxCountCallback = cb;
    }

    /**
     * Sets the upper limit for the counter.
     * This does not automatically reset the current count to zero (see {@link #resetCount()}).
     * 
     * @param max
     *        Upper limit of the counter.
     */
    public void setMaximalCount(final int max) {
        this.maximalCount = max;
    }

    /**
     * Gets the upper limit of the counter.
     * 
     * @return the counter upper limit.
     */
    public int getMaximalCount() {
        return this.maximalCount;
    }

    /**
     * Gets the current count.
     * 
     * @return the current count.
     */
    public int getCount() {
        return this.count;
    }

    /**
     * Checks whether a single increment is allowed.
     * 
     * @return {@code false} if the next call to {@link #incrementCount(int)
     *         incrementCount} will trigger a {@code MaxCountExceededException}, {@code true} otherwise.
     */
    public boolean canIncrement() {
        return this.count < this.maximalCount;
    }

    /**
     * Performs multiple increments.
     * See the other {@link #incrementCount() incrementCount} method).
     * 
     * @param value
     *        Number of increments.
     * @throws MaxCountExceededException
     *         at counter exhaustion.
     */
    public void incrementCount(final int value) {
        for (int i = 0; i < value; i++) {
            this.incrementCount();
        }
    }

    /**
     * Adds one to the current iteration count.
     * At counter exhaustion, this method will call the {@link MaxCountExceededCallback#trigger(int) trigger} method of
     * the
     * callback object passed to the {@link #Incrementor(int,MaxCountExceededCallback) constructor}.
     * If not explictly set, a default callback is used that will throw
     * a {@code MaxCountExceededException}.
     * 
     * @throws MaxCountExceededException
     *         at counter exhaustion, unless a
     *         custom {@link MaxCountExceededCallback callback} has been set at
     *         construction.
     */
    public void incrementCount() {
        this.count++;
        if (this.count > this.maximalCount) {
            this.maxCountCallback.trigger(this.maximalCount);
        }
    }

    /**
     * Resets the counter to 0.
     */
    public void resetCount() {
        this.count = 0;
    }

    /**
     * Defines a method to be called at counter exhaustion.
     * The {@link #trigger(int) trigger} method should usually throw an exception.
     */
    public interface MaxCountExceededCallback extends Serializable {
        /**
         * Function called when the maximal count has been reached.
         * 
         * @param maximalCount
         *        Maximal count.
         */
        void trigger(int maximalCount);
    }
}
