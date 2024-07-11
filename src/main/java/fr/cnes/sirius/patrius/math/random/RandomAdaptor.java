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
package fr.cnes.sirius.patrius.math.random;

import java.util.Random;

/**
 * Extension of <code>java.util.Random</code> wrapping a {@link RandomGenerator}.
 * 
 * @since 1.1
 * @version $Id: RandomAdaptor.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class RandomAdaptor extends Random implements RandomGenerator {

    /** Serializable version identifier. */
    private static final long serialVersionUID = 2306581345647615033L;

    /** Wrapped randomGenerator instance */
    private final RandomGenerator randomGenerator;

    /**
     * Construct a RandomAdaptor wrapping the supplied RandomGenerator.
     * 
     * @param randomGeneratorIn
     *        the wrapped generator
     */
    public RandomAdaptor(final RandomGenerator randomGeneratorIn) {
        super();
        this.randomGenerator = randomGeneratorIn;
    }

    /**
     * Factory method to create a <code>Random</code> using the supplied <code>RandomGenerator</code>.
     * 
     * @param randomGenerator
     *        wrapped RandomGenerator instance
     * @return a Random instance wrapping the RandomGenerator
     */
    public static Random createAdaptor(final RandomGenerator randomGenerator) {
        return new RandomAdaptor(randomGenerator);
    }

    /**
     * Returns the next pseudorandom, uniformly distributed <code>boolean</code> value from this random number
     * generator's
     * sequence.
     * 
     * @return the next pseudorandom, uniformly distributed <code>boolean</code> value from this random number
     *         generator's
     *         sequence
     */
    @Override
    public boolean nextBoolean() {
        return this.randomGenerator.nextBoolean();
    }

    /**
     * Generates random bytes and places them into a user-supplied
     * byte array. The number of random bytes produced is equal to
     * the length of the byte array.
     * 
     * @param bytes
     *        the non-null byte array in which to put the
     *        random bytes
     */
    @Override
    public void nextBytes(final byte[] bytes) {
        this.randomGenerator.nextBytes(bytes);
    }

    /**
     * Returns the next pseudorandom, uniformly distributed <code>double</code> value between <code>0.0</code> and
     * <code>1.0</code> from this random number generator's sequence.
     * 
     * @return the next pseudorandom, uniformly distributed <code>double</code> value between <code>0.0</code> and
     *         <code>1.0</code> from this random number generator's sequence
     */
    @Override
    public double nextDouble() {
        return this.randomGenerator.nextDouble();
    }

    /**
     * Returns the next pseudorandom, uniformly distributed <code>float</code> value between <code>0.0</code> and
     * <code>1.0</code> from this random
     * number generator's sequence.
     * 
     * @return the next pseudorandom, uniformly distributed <code>float</code> value between <code>0.0</code> and
     *         <code>1.0</code> from this
     *         random number generator's sequence
     */
    @Override
    public float nextFloat() {
        return this.randomGenerator.nextFloat();
    }

    /**
     * Returns the next pseudorandom, Gaussian ("normally") distributed <code>double</code> value with mean
     * <code>0.0</code> and standard
     * deviation <code>1.0</code> from this random number generator's sequence.
     * 
     * @return the next pseudorandom, Gaussian ("normally") distributed <code>double</code> value with mean
     *         <code>0.0</code> and
     *         standard deviation <code>1.0</code> from this random number
     *         generator's sequence
     */
    @Override
    public double nextGaussian() {
        return this.randomGenerator.nextGaussian();
    }

    /**
     * Returns the next pseudorandom, uniformly distributed <code>int</code> value from this random number generator's
     * sequence.
     * All 2<font size="-1"><sup>32</sup></font> possible <tt>int</tt> values
     * should be produced with (approximately) equal probability.
     * 
     * @return the next pseudorandom, uniformly distributed <code>int</code> value from this random number generator's
     *         sequence
     */
    @Override
    public int nextInt() {
        return this.randomGenerator.nextInt();
    }

    /**
     * Returns a pseudorandom, uniformly distributed <tt>int</tt> value
     * between 0 (inclusive) and the specified value (exclusive), drawn from
     * this random number generator's sequence.
     * 
     * @param n
     *        the bound on the random number to be returned. Must be
     *        positive.
     * @return a pseudorandom, uniformly distributed <tt>int</tt> value between 0 (inclusive) and n (exclusive).
     * @throws IllegalArgumentException
     *         if n is not positive.
     */
    @Override
    public int nextInt(final int n) {
        return this.randomGenerator.nextInt(n);
    }

    /**
     * Returns the next pseudorandom, uniformly distributed <code>long</code> value from this random number generator's
     * sequence. All
     * 2<font size="-1"><sup>64</sup></font> possible <tt>long</tt> values
     * should be produced with (approximately) equal probability.
     * 
     * @return the next pseudorandom, uniformly distributed <code>long</code> value from this random number generator's
     *         sequence
     */
    @Override
    public long nextLong() {
        return this.randomGenerator.nextLong();
    }

    /** {@inheritDoc} */
    @Override
    public void setSeed(final int seed) {
        if (this.randomGenerator != null) {
            // required to avoid NPE in constructor
            this.randomGenerator.setSeed(seed);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void setSeed(final int[] seed) {
        if (this.randomGenerator != null) {
            // required to avoid NPE in constructor
            this.randomGenerator.setSeed(seed);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void setSeed(final long seed) {
        if (this.randomGenerator != null) {
            // required to avoid NPE in constructor
            this.randomGenerator.setSeed(seed);
        }
    }

}
