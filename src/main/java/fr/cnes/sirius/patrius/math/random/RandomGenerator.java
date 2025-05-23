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
package fr.cnes.sirius.patrius.math.random;

/**
 * Interface extracted from <code>java.util.Random</code>. This interface is
 * implemented by {@link AbstractRandomGenerator}.
 * 
 * @since 1.1
 * @version $Id: RandomGenerator.java 18108 2017-10-04 06:45:27Z bignon $
 */
public interface RandomGenerator {

    /**
     * Sets the seed of the underlying random number generator using an <code>int</code> seed.
     * <p>
     * Sequences of values generated starting with the same seeds should be identical.
     * </p>
     * 
     * @param seed
     *        the seed value
     */
    void setSeed(int seed);

    /**
     * Sets the seed of the underlying random number generator using an <code>int</code> array seed.
     * <p>
     * Sequences of values generated starting with the same seeds should be identical.
     * </p>
     * 
     * @param seed
     *        the seed value
     */
    void setSeed(int[] seed);

    /**
     * Sets the seed of the underlying random number generator using a <code>long</code> seed.
     * <p>
     * Sequences of values generated starting with the same seeds should be identical.
     * </p>
     * 
     * @param seed
     *        the seed value
     */
    void setSeed(long seed);

    /**
     * Generates random bytes and places them into a user-supplied
     * byte array. The number of random bytes produced is equal to
     * the length of the byte array.
     * 
     * @param bytes
     *        the non-null byte array in which to put the
     *        random bytes
     */
    void nextBytes(byte[] bytes);

    /**
     * Returns the next pseudorandom, uniformly distributed <code>int</code> value from this random number generator's
     * sequence.
     * All 2<font size="-1"><sup>32</sup></font> possible <tt>int</tt> values
     * should be produced with (approximately) equal probability.
     * 
     * @return the next pseudorandom, uniformly distributed <code>int</code> value from this random number generator's
     *         sequence
     */
    int nextInt();

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
    int nextInt(int n);

    /**
     * Returns the next pseudorandom, uniformly distributed <code>long</code> value from this random number generator's
     * sequence. All
     * 2<font size="-1"><sup>64</sup></font> possible <tt>long</tt> values
     * should be produced with (approximately) equal probability.
     * 
     * @return the next pseudorandom, uniformly distributed <code>long</code> value from this random number generator's
     *         sequence
     */
    long nextLong();

    /**
     * Returns the next pseudorandom, uniformly distributed <code>boolean</code> value from this random number
     * generator's
     * sequence.
     * 
     * @return the next pseudorandom, uniformly distributed <code>boolean</code> value from this random number
     *         generator's
     *         sequence
     */
    boolean nextBoolean();

    /**
     * Returns the next pseudorandom, uniformly distributed <code>float</code> value between <code>0.0</code> and
     * <code>1.0</code> from this random
     * number generator's sequence.
     * 
     * @return the next pseudorandom, uniformly distributed <code>float</code> value between <code>0.0</code> and
     *         <code>1.0</code> from this
     *         random number generator's sequence
     */
    float nextFloat();

    /**
     * Returns the next pseudorandom, uniformly distributed <code>double</code> value between <code>0.0</code> and
     * <code>1.0</code> from this random number generator's sequence.
     * 
     * @return the next pseudorandom, uniformly distributed <code>double</code> value between <code>0.0</code> and
     *         <code>1.0</code> from this random number generator's sequence
     */
    double nextDouble();

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
    double nextGaussian();
}
