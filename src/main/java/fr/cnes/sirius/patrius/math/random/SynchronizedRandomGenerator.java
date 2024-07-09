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
package fr.cnes.sirius.patrius.math.random;

/**
 * Any {@link RandomGenerator} implementation can be thread-safe if it
 * is used through an instance of this class.
 * This is achieved by enclosing calls to the methods of the actual
 * generator inside the overridden {@code synchronized} methods of this
 * class.
 * 
 * @since 3.1
 * @version $Id: SynchronizedRandomGenerator.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class SynchronizedRandomGenerator implements RandomGenerator {
    /** Object to which all calls will be delegated. */
    private final RandomGenerator wrapped;

    /**
     * Creates a synchronized wrapper for the given {@code RandomGenerator} instance.
     * 
     * @param rng
     *        Generator whose methods will be called through
     *        their corresponding overridden synchronized version.
     *        To ensure thread-safety, the wrapped generator <em>must</em> not be used directly.
     */
    public SynchronizedRandomGenerator(final RandomGenerator rng) {
        this.wrapped = rng;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void setSeed(final int seed) {
        this.wrapped.setSeed(seed);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void setSeed(final int[] seed) {
        this.wrapped.setSeed(seed);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void setSeed(final long seed) {
        this.wrapped.setSeed(seed);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void nextBytes(final byte[] bytes) {
        this.wrapped.nextBytes(bytes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized int nextInt() {
        return this.wrapped.nextInt();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized int nextInt(final int n) {
        return this.wrapped.nextInt(n);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized long nextLong() {
        return this.wrapped.nextLong();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean nextBoolean() {
        return this.wrapped.nextBoolean();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized float nextFloat() {
        return this.wrapped.nextFloat();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized double nextDouble() {
        return this.wrapped.nextDouble();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized double nextGaussian() {
        return this.wrapped.nextGaussian();
    }
}
