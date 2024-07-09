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

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.util.MathLib;

//CHECKSTYLE: stop MagicNumber check
//CHECKSTYLE: stop CommentRatio check
//Reason: Commons-Math model kept as such

/**
 * This class implements a powerful pseudo-random number generator
 * developed by Makoto Matsumoto and Takuji Nishimura during
 * 1996-1997.
 * 
 * <p>
 * This generator features an extremely long period (2<sup>19937</sup>-1) and 623-dimensional equidistribution up to 32
 * bits accuracy. The home page for this generator is located at <a
 * href="http://www.math.sci.hiroshima-u.ac.jp/~m-mat/MT/emt.html">
 * http://www.math.sci.hiroshima-u.ac.jp/~m-mat/MT/emt.html</a>.
 * </p>
 * 
 * <p>
 * This generator is described in a paper by Makoto Matsumoto and Takuji Nishimura in 1998: <a
 * href="http://www.math.sci.hiroshima-u.ac.jp/~m-mat/MT/ARTICLES/mt.pdf">Mersenne Twister: A 623-Dimensionally
 * Equidistributed Uniform Pseudo-Random Number Generator</a>, ACM Transactions on Modeling and Computer Simulation,
 * Vol. 8, No. 1, January 1998, pp 3--30
 * </p>
 * 
 * <p>
 * This class is mainly a Java port of the 2002-01-26 version of the generator written in C by Makoto Matsumoto and
 * Takuji Nishimura. Here is their original copyright:
 * </p>
 * 
 * <table border="0" width="80%" cellpadding="10" align="center" bgcolor="#E0E0E0">
 * <tr>
 * <td>Copyright (C) 1997 - 2002, Makoto Matsumoto and Takuji Nishimura, All rights reserved.</td>
 * </tr>
 * 
 * <tr>
 * <td>Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 * <ol>
 * <li>Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 * disclaimer.</li>
 * <li>Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 * following disclaimer in the documentation and/or other materials provided with the distribution.</li>
 * <li>The names of its contributors may not be used to endorse or promote products derived from this software without
 * specific prior written permission.</li>
 * </ol>
 * </td>
 * </tr>
 * 
 * <tr>
 * <td><strong>THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.</strong></td>
 * </tr>
 * </table>
 * 
 * @version $Id: MersenneTwister.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 2.0
 */
@SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
public class MersenneTwister extends BitsStreamGenerator implements Serializable {

    /** Serializable version identifier. */
    private static final long serialVersionUID = 8661194735290153518L;

    /** Size of the bytes pool. */
    private static final int N = 624;

    /** Period second parameter. */
    private static final int M = 397;

    /** X * MATRIX_A for X = {0, 1}. */
    private static final int[] MAG01 = { 0x0, 0x9908b0df };

    /** Bytes pool. */
    private final int[] mt;

    /** Current index in the bytes pool. */
    private int mti;

    /**
     * Creates a new random number generator.
     * <p>
     * The instance is initialized using the current time plus the system identity hash code of this instance as the
     * seed.
     * </p>
     */
    public MersenneTwister() {
        super();
        this.mt = new int[N];
        this.setSeed(System.currentTimeMillis() + System.identityHashCode(this));
    }

    /**
     * Creates a new random number generator using a single int seed.
     * 
     * @param seed
     *        the initial seed (32 bits integer)
     */
    public MersenneTwister(final int seed) {
        super();
        this.mt = new int[N];
        this.setSeed(seed);
    }

    /**
     * Creates a new random number generator using an int array seed.
     * 
     * @param seed
     *        the initial seed (32 bits integers array), if null
     *        the seed of the generator will be related to the current time
     */
    public MersenneTwister(final int[] seed) {
        super();
        this.mt = new int[N];
        this.setSeed(seed);
    }

    /**
     * Creates a new random number generator using a single long seed.
     * 
     * @param seed
     *        the initial seed (64 bits integer)
     */
    public MersenneTwister(final long seed) {
        super();
        this.mt = new int[N];
        this.setSeed(seed);
    }

    /**
     * Reinitialize the generator as if just built with the given int seed.
     * <p>
     * The state of the generator is exactly the same as a new generator built with the same seed.
     * </p>
     * 
     * @param seed
     *        the initial seed (32 bits integer)
     */
    @Override
    public void setSeed(final int seed) {
        // we use a long masked by 0xffffffffL as a poor man unsigned int
        long longMT = seed;
        // NB: unlike original C code, we are working with java longs, the cast below makes masking unnecessary
        this.mt[0] = (int) longMT;
        for (this.mti = 1; this.mti < N; ++this.mti) {
            // See Knuth TAOCP Vol2. 3rd Ed. P.106 for multiplier.
            // initializer from the 2002-01-09 C version by Makoto Matsumoto
            longMT = (1812433253L * (longMT ^ (longMT >> 30)) + this.mti) & 0xffffffffL;
            this.mt[this.mti] = (int) longMT;
        }

        // Clear normal deviate cache
        this.clear();
    }

    /**
     * Reinitialize the generator as if just built with the given int array seed.
     * <p>
     * The state of the generator is exactly the same as a new generator built with the same seed.
     * </p>
     * 
     * @param seed
     *        the initial seed (32 bits integers array), if null
     *        the seed of the generator will be the current system time plus the
     *        system identity hash code of this instance
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Commons-Math code kept as such
    @Override
    public void setSeed(final int[] seed) {
        // CHECKSTYLE: resume CyclomaticComplexity check

        if (seed == null) {
            this.setSeed(System.currentTimeMillis() + System.identityHashCode(this));
            return;
        }

        this.setSeed(19650218);
        int i = 1;
        int j = 0;

        for (int k = MathLib.max(N, seed.length); k != 0; k--) {
            final long l0 = (this.mt[i] & 0x7fffffffL) | ((this.mt[i] < 0) ? 0x80000000L : 0x0L);
            final long l1 = (this.mt[i - 1] & 0x7fffffffL) | ((this.mt[i - 1] < 0) ? 0x80000000L : 0x0L);
            // non linear
            final long l = (l0 ^ ((l1 ^ (l1 >> 30)) * 1664525L)) + seed[j] + j;
            this.mt[i] = (int) (l & 0xffffffffL);
            i++;
            j++;
            if (i >= N) {
                this.mt[0] = this.mt[N - 1];
                i = 1;
            }
            if (j >= seed.length) {
                j = 0;
            }
        }

        for (int k = N - 1; k != 0; k--) {
            final long l0 = (this.mt[i] & 0x7fffffffL) | ((this.mt[i] < 0) ? 0x80000000L : 0x0L);
            final long l1 = (this.mt[i - 1] & 0x7fffffffL) | ((this.mt[i - 1] < 0) ? 0x80000000L : 0x0L);
            // non linear
            final long l = (l0 ^ ((l1 ^ (l1 >> 30)) * 1566083941L)) - i;
            this.mt[i] = (int) (l & 0xffffffffL);
            i++;
            if (i >= N) {
                this.mt[0] = this.mt[N - 1];
                i = 1;
            }
        }

        // MSB is 1; assuring non-zero initial array
        this.mt[0] = 0x80000000;

        // Clear normal deviate cache
        this.clear();

    }

    /**
     * Reinitialize the generator as if just built with the given long seed.
     * <p>
     * The state of the generator is exactly the same as a new generator built with the same seed.
     * </p>
     * 
     * @param seed
     *        the initial seed (64 bits integer)
     */
    @Override
    public void setSeed(final long seed) {
        this.setSeed(new int[] { (int) (seed >>> 32), (int) (seed & 0xffffffffL) });
    }

    /**
     * Generate next pseudorandom number.
     * <p>
     * This method is the core generation algorithm. It is used by all the public generation methods for the various
     * primitive types {@link #nextBoolean()}, {@link #nextBytes(byte[])}, {@link #nextDouble()}, {@link #nextFloat()},
     * {@link #nextGaussian()}, {@link #nextInt()}, {@link #next(int)} and {@link #nextLong()}.
     * </p>
     * 
     * @param bits
     *        number of random bits to produce
     * @return random bits generated
     */
    @Override
    protected int next(final int bits) {

        int y;

        if (this.mti >= N) {
            // generate N words at one time
            int mtNext = this.mt[0];
            for (int k = 0; k < N - M; ++k) {
                final int mtCurr = mtNext;
                mtNext = this.mt[k + 1];
                y = (mtCurr & 0x80000000) | (mtNext & 0x7fffffff);
                this.mt[k] = this.mt[k + M] ^ (y >>> 1) ^ MAG01[y & 0x1];
            }
            for (int k = N - M; k < N - 1; ++k) {
                final int mtCurr = mtNext;
                mtNext = this.mt[k + 1];
                y = (mtCurr & 0x80000000) | (mtNext & 0x7fffffff);
                this.mt[k] = this.mt[k + (M - N)] ^ (y >>> 1) ^ MAG01[y & 0x1];
            }
            y = (mtNext & 0x80000000) | (this.mt[0] & 0x7fffffff);
            this.mt[N - 1] = this.mt[M - 1] ^ (y >>> 1) ^ MAG01[y & 0x1];

            this.mti = 0;
        }

        y = this.mt[this.mti++];

        // tempering
        y ^= y >>> 11;
        y ^= (y << 7) & 0x9d2c5680;
        y ^= (y << 15) & 0xefc60000;
        y ^= y >>> 18;

        return y >>> (32 - bits);

    }

    // CHECKSTYLE: resume MagicNumber check
    // CHECKSTYLE: resume CommentRatio check
}
