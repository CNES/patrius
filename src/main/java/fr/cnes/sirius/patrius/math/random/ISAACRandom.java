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

//CHECKSTYLE: stop MagicNumber check
//CHECKSTYLE: stop CommentRatio check
//Reason: Commons-Math model kept as such

/**
 * <a href="http://burtleburtle.net/bob/rand/isaacafa.html">
 * ISAAC: a fast cryptographic pseudo-random number generator</a> <br/>
 * ISAAC (Indirection, Shift, Accumulate, Add, and Count) generates 32-bit
 * random numbers.
 * ISAAC has been designed to be cryptographically secure and is inspired
 * by RC4.
 * Cycles are guaranteed to be at least 2<sup>40</sup> values long, and they
 * are 2<sup>8295</sup> values long on average.
 * The results are uniformly distributed, unbiased, and unpredictable unless
 * you know the seed. <br/>
 * This code is based (with minor changes and improvements) on the original
 * implementation of the algorithm by Bob Jenkins. <br/>
 * 
 * @version $Id: ISAACRandom.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.0
 */
@SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
public class ISAACRandom extends BitsStreamGenerator implements Serializable {

    /** Serializable version identifier */
    private static final long serialVersionUID = 7288197941165002400L;
    /** Log of size of rsl[] and mem[] */
    private static final int SIZE_L = 8;
    /** Size of rsl[] and mem[] */
    private static final int SIZE = 1 << SIZE_L;
    /** Half-size of rsl[] and mem[] */
    private static final int H_SIZE = SIZE >> 1;
    /** For pseudo-random lookup */
    private static final int MASK = SIZE - 1 << 2;
    /** The golden ratio */
    private static final int GLD_RATIO = 0x9e3779b9;
    /** The results given to the user */
    private final int[] rsl = new int[SIZE];
    /** The internal state */
    private final int[] mem = new int[SIZE];
    /** Count through the results in rsl[] */
    private int count;
    /** Accumulator */
    private int isaacA;
    /** The last result */
    private int isaacB;
    /** Counter, guarantees cycle is at least 2^40 */
    private int isaacC;
    /** Service variable. */
    private final int[] arr = new int[8];
    /** Service variable. */
    private int isaacX;
    /** Service variable. */
    private int isaacI;
    /** Service variable. */
    private int isaacJ;

    /**
     * Creates a new ISAAC random number generator. <br/>
     * The instance is initialized using a combination of the
     * current time and system hash code of the instance as the seed.
     */
    public ISAACRandom() {
        super();
        this.setSeed(System.currentTimeMillis() + System.identityHashCode(this));
    }

    /**
     * Creates a new ISAAC random number generator using a single long seed.
     * 
     * @param seed
     *        Initial seed.
     */
    public ISAACRandom(final long seed) {
        super();
        this.setSeed(seed);
    }

    /**
     * Creates a new ISAAC random number generator using an int array seed.
     * 
     * @param seed
     *        Initial seed. If {@code null}, the seed will be related
     *        to the current time.
     */
    public ISAACRandom(final int[] seed) {
        super();
        this.setSeed(seed);
    }

    /** {@inheritDoc} */
    @Override
    public void setSeed(final int seed) {
        this.setSeed(new int[] { seed });
    }

    /** {@inheritDoc} */
    @Override
    public void setSeed(final long seed) {
        this.setSeed(new int[] { (int) (seed >>> 32), (int) (seed & 0xffffffffL) });
    }

    /** {@inheritDoc} */
    @Override
    public void setSeed(final int[] seed) {
        if (seed == null) {
            this.setSeed(System.currentTimeMillis() + System.identityHashCode(this));
            return;
        }
        final int seedLen = seed.length;
        final int rslLen = this.rsl.length;
        System.arraycopy(seed, 0, this.rsl, 0, Math.min(seedLen, rslLen));
        if (seedLen < rslLen) {
            for (int j = seedLen; j < rslLen; j++) {
                final long k = this.rsl[j - seedLen];
                this.rsl[j] = (int) (0x6c078965L * (k ^ k >> 30) + j & 0xffffffffL);
            }
        }
        this.initState();
    }

    /** {@inheritDoc} */
    @Override
    protected int next(final int bits) {
        if (this.count < 0) {
            this.isaac();
            this.count = SIZE - 1;
        }
        return this.rsl[this.count--] >>> 32 - bits;
    }

    /** Generate 256 results */
    private void isaac() {
        this.isaacI = 0;
        this.isaacJ = H_SIZE;
        this.isaacB += ++this.isaacC;
        while (this.isaacI < H_SIZE) {
            this.isaac2();
        }
        this.isaacJ = 0;
        while (this.isaacJ < H_SIZE) {
            this.isaac2();
        }
    }

    /** Intermediate internal loop. */
    private void isaac2() {
        this.isaacX = this.mem[this.isaacI];
        this.isaacA ^= this.isaacA << 13;
        this.isaacA += this.mem[this.isaacJ++];
        this.isaac3();
        this.isaacX = this.mem[this.isaacI];
        this.isaacA ^= this.isaacA >>> 6;
        this.isaacA += this.mem[this.isaacJ++];
        this.isaac3();
        this.isaacX = this.mem[this.isaacI];
        this.isaacA ^= this.isaacA << 2;
        this.isaacA += this.mem[this.isaacJ++];
        this.isaac3();
        this.isaacX = this.mem[this.isaacI];
        this.isaacA ^= this.isaacA >>> 16;
        this.isaacA += this.mem[this.isaacJ++];
        this.isaac3();
    }

    /** Lowest level internal loop. */
    private void isaac3() {
        this.mem[this.isaacI] = this.mem[(this.isaacX & MASK) >> 2] + this.isaacA + this.isaacB;
        this.isaacB = this.mem[(this.mem[this.isaacI] >> SIZE_L & MASK) >> 2] + this.isaacX;
        this.rsl[this.isaacI++] = this.isaacB;
    }

    /** Initialize, or reinitialize, this instance of rand. */
    private void initState() {
        this.isaacA = 0;
        this.isaacB = 0;
        this.isaacC = 0;
        for (int j = 0; j < this.arr.length; j++) {
            this.arr[j] = GLD_RATIO;
        }
        for (int j = 0; j < 4; j++) {
            this.shuffle();
        }
        // fill in mem[] with messy stuff
        for (int j = 0; j < SIZE; j += 8) {
            this.arr[0] += this.rsl[j];
            this.arr[1] += this.rsl[j + 1];
            this.arr[2] += this.rsl[j + 2];
            this.arr[3] += this.rsl[j + 3];
            this.arr[4] += this.rsl[j + 4];
            this.arr[5] += this.rsl[j + 5];
            this.arr[6] += this.rsl[j + 6];
            this.arr[7] += this.rsl[j + 7];
            this.shuffle();
            this.setState(j);
        }
        // second pass makes all of seed affect all of mem
        for (int j = 0; j < SIZE; j += 8) {
            this.arr[0] += this.mem[j];
            this.arr[1] += this.mem[j + 1];
            this.arr[2] += this.mem[j + 2];
            this.arr[3] += this.mem[j + 3];
            this.arr[4] += this.mem[j + 4];
            this.arr[5] += this.mem[j + 5];
            this.arr[6] += this.mem[j + 6];
            this.arr[7] += this.mem[j + 7];
            this.shuffle();
            this.setState(j);
        }
        this.isaac();
        this.count = SIZE - 1;
        this.clear();
    }

    /** Shuffle array. */
    private void shuffle() {
        this.arr[0] ^= this.arr[1] << 11;
        this.arr[3] += this.arr[0];
        this.arr[1] += this.arr[2];
        this.arr[1] ^= this.arr[2] >>> 2;
        this.arr[4] += this.arr[1];
        this.arr[2] += this.arr[3];
        this.arr[2] ^= this.arr[3] << 8;
        this.arr[5] += this.arr[2];
        this.arr[3] += this.arr[4];
        this.arr[3] ^= this.arr[4] >>> 16;
        this.arr[6] += this.arr[3];
        this.arr[4] += this.arr[5];
        this.arr[4] ^= this.arr[5] << 10;
        this.arr[7] += this.arr[4];
        this.arr[5] += this.arr[6];
        this.arr[5] ^= this.arr[6] >>> 4;
        this.arr[0] += this.arr[5];
        this.arr[6] += this.arr[7];
        this.arr[6] ^= this.arr[7] << 8;
        this.arr[1] += this.arr[6];
        this.arr[7] += this.arr[0];
        this.arr[7] ^= this.arr[0] >>> 9;
        this.arr[2] += this.arr[7];
        this.arr[0] += this.arr[1];
    }

    /**
     * Set the state by copying the internal arrays.
     * 
     * @param start
     *        First index into {@link #mem} array.
     */
    private void setState(final int start) {
        this.mem[start] = this.arr[0];
        this.mem[start + 1] = this.arr[1];
        this.mem[start + 2] = this.arr[2];
        this.mem[start + 3] = this.arr[3];
        this.mem[start + 4] = this.arr[4];
        this.mem[start + 5] = this.arr[5];
        this.mem[start + 6] = this.arr[6];
        this.mem[start + 7] = this.arr[7];
    }

    // CHECKSTYLE: resume MagicNumber check
    // CHECKSTYLE: resume CommentRatio check
}
