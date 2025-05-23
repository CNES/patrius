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

import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Abstract class implementing the {@link RandomGenerator} interface.
 * Default implementations for all methods other than {@link #nextDouble()} and {@link #setSeed(long)} are provided.
 * <p>
 * All data generation methods are based on {@code code nextDouble()}. Concrete implementations <strong>must</strong>
 * override this method and <strong>should</strong> provide better / more performant implementations of the other
 * methods if the underlying PRNG supplies them.
 * </p>
 * 
 * @since 1.1
 * @version $Id: AbstractRandomGenerator.java 18108 2017-10-04 06:45:27Z bignon $
 */
public abstract class AbstractRandomGenerator implements RandomGenerator {

    /** 0.5. */
    private static final double HALF = 0.5;
    
    /** 8 */
    private static final int EIGHT = 8;

    /**
     * Cached random normal value. The default implementation for {@link #nextGaussian} generates pairs of values and
     * this field caches the
     * second value so that the full algorithm is not executed for every
     * activation. The value {@code Double.NaN} signals that there is
     * no cached value. Use {@link #clear} to clear the cached value.
     */
    private double cachedNormalDeviate = Double.NaN;

    /**
     * Clears the cache used by the default implementation of {@link #nextGaussian}. Implementations that do not
     * override the
     * default implementation of {@code nextGaussian} should call this
     * method in the implementation of {@link #setSeed(long)}
     */
    public void clear() {
        this.cachedNormalDeviate = Double.NaN;
    }

    /** {@inheritDoc} */
    @Override
    public void setSeed(final int seed) {
        this.setSeed((long) seed);
    }

    /** {@inheritDoc} */
    @Override
    public void setSeed(final int[] seed) {
        // the following number is the largest prime that fits in 32 bits (it is 2^32 - 5)
        final long prime = 4294967291L;

        long combined = 0L;
        for (final int s : seed) {
            combined = combined * prime + s;
        }
        this.setSeed(combined);
    }

    /**
     * Sets the seed of the underlying random number generator using a {@code long} seed. Sequences of values generated
     * starting with the
     * same seeds should be identical.
     * <p>
     * Implementations that do not override the default implementation of {@code nextGaussian} should include a call to
     * {@link #clear} in the implementation of this method.
     * </p>
     * 
     * @param seed
     *        the seed value
     */
    @Override
    public abstract void setSeed(long seed);

    /**
     * Generates random bytes and places them into a user-supplied
     * byte array. The number of random bytes produced is equal to
     * the length of the byte array.
     * <p>
     * The default implementation fills the array with bytes extracted from random integers generated using
     * {@link #nextInt}.
     * </p>
     * 
     * @param bytes
     *        the non-null byte array in which to put the
     *        random bytes
     */
    @Override
    public void nextBytes(final byte[] bytes) {
        // initialize variable to count the number of bytes generated
        int bytesOut = 0;
        //loop until the required number of bytes have been generated
        while (bytesOut < bytes.length) {
            // generate random byte using nextInt
            int randInt = this.nextInt();
            for (int i = 0; i < 3; i++) {
                if (i > 0) {
                    randInt = randInt >> EIGHT;
                }
                bytes[bytesOut++] = (byte) randInt;
                if (bytesOut == bytes.length) {
                    return;
                }
            }
        }
    }

    /**
     * Returns the next pseudorandom, uniformly distributed {@code int} value from this random number generator's
     * sequence.
     * All 2<font size="-1"><sup>32</sup></font> possible {@code int} values
     * should be produced with (approximately) equal probability.
     * <p>
     * The default implementation provided here returns
     * 
     * <pre>
     * <code>(int) (nextDouble() * Integer.MAX_VALUE)</code>
     * </pre>
     * 
     * </p>
     * 
     * @return the next pseudorandom, uniformly distributed {@code int} value from this random number generator's
     *         sequence
     */
    @Override
    public int nextInt() {
        return (int) ((2d * this.nextDouble() - 1d) * Integer.MAX_VALUE);
    }

    /**
     * Returns a pseudorandom, uniformly distributed {@code int} value
     * between 0 (inclusive) and the specified value (exclusive), drawn from
     * this random number generator's sequence.
     * <p>
     * The default implementation returns
     * 
     * <pre>
     * <code>(int) (nextDouble() * n</code>
     * </pre>
     * 
     * </p>
     * 
     * @param n
     *        the bound on the random number to be returned. Must be
     *        positive.
     * @return a pseudorandom, uniformly distributed {@code int} value between 0 (inclusive) and n (exclusive).
     * @throws NotStrictlyPositiveException
     *         if {@code n <= 0}.
     */
    @Override
    public int nextInt(final int n) {
        if (n <= 0) {
            throw new NotStrictlyPositiveException(n);
        }
        final int result = (int) (this.nextDouble() * n);
        return result < n ? result : n - 1;
    }

    /**
     * Returns the next pseudorandom, uniformly distributed {@code long} value from this random number generator's
     * sequence. All
     * 2<font size="-1"><sup>64</sup></font> possible {@code long} values
     * should be produced with (approximately) equal probability.
     * <p>
     * The default implementation returns
     * 
     * <pre>
     * <code>(long) (nextDouble() * Long.MAX_VALUE)</code>
     * </pre>
     * 
     * </p>
     * 
     * @return the next pseudorandom, uniformly distributed {@code long} value from this random number generator's
     *         sequence
     */
    @Override
    public long nextLong() {
        return (long) ((2d * this.nextDouble() - 1d) * Long.MAX_VALUE);
    }

    /**
     * Returns the next pseudorandom, uniformly distributed {@code boolean} value from this random number generator's
     * sequence.
     * <p>
     * The default implementation returns
     * 
     * <pre>
     * <code>nextDouble() <= 0.5</code>
     * </pre>
     * 
     * </p>
     * 
     * @return the next pseudorandom, uniformly distributed {@code boolean} value from this random number generator's
     *         sequence
     */
    @Override
    public boolean nextBoolean() {
        return this.nextDouble() <= HALF;
    }

    /**
     * Returns the next pseudorandom, uniformly distributed {@code float} value between {@code 0.0} and {@code 1.0} from
     * this random
     * number generator's sequence.
     * <p>
     * The default implementation returns
     * 
     * <pre>
     * <code>(float) nextDouble() </code>
     * </pre>
     * 
     * </p>
     * 
     * @return the next pseudorandom, uniformly distributed {@code float} value between {@code 0.0} and {@code 1.0} from
     *         this
     *         random number generator's sequence
     */
    @Override
    public float nextFloat() {
        return (float) this.nextDouble();
    }

    /**
     * Returns the next pseudorandom, uniformly distributed {@code double} value between {@code 0.0} and {@code 1.0}
     * from this random number generator's sequence.
     * <p>
     * This method provides the underlying source of random data used by the other methods.
     * </p>
     * 
     * @return the next pseudorandom, uniformly distributed {@code double} value between {@code 0.0} and {@code 1.0}
     *         from this random number generator's sequence
     */
    @Override
    public abstract double nextDouble();

    /**
     * Returns the next pseudorandom, Gaussian ("normally") distributed {@code double} value with mean {@code 0.0} and
     * standard
     * deviation {@code 1.0} from this random number generator's sequence.
     * <p>
     * The default implementation uses the <em>Polar Method</em> due to G.E.P. Box, M.E. Muller and G. Marsaglia, as
     * described in D. Knuth, <u>The Art of Computer Programming</u>, 3.4.1C.
     * </p>
     * <p>
     * The algorithm generates a pair of independent random values. One of these is cached for reuse, so the full
     * algorithm is not executed on each activation. Implementations that do not override this method should make sure
     * to call {@link #clear} to clear the cached value in the implementation of {@link #setSeed(long)}.
     * </p>
     * 
     * @return the next pseudorandom, Gaussian ("normally") distributed {@code double} value with mean {@code 0.0} and
     *         standard deviation {@code 1.0} from this random number
     *         generator's sequence
     */
    @Override
    public double nextGaussian() {
        if (!Double.isNaN(this.cachedNormalDeviate)) {
            // Data stored in cache
            // Return result
            final double dev = this.cachedNormalDeviate;
            this.cachedNormalDeviate = Double.NaN;
            return dev;
        }
        // Compute data
        double v1 = 0;
        double v2 = 0;
        double s = 1;
        while (s >= 1) {
            v1 = 2 * this.nextDouble() - 1;
            v2 = 2 * this.nextDouble() - 1;
            s = v1 * v1 + v2 * v2;
        }
        if (s != 0) {
            s = MathLib.sqrt(- (2 * MathLib.log(s)) / s);
        }
        // Store in cache
        this.cachedNormalDeviate = v2 * s;
        return v1 * s;
    }
}
