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

//CHECKSTYLE: stop MagicNumber check
//CHECKSTYLE: stop CommentRatio check
//Reason: Commons-Math model kept as such

/**
 * This class implements the WELL1024a pseudo-random number generator
 * from Fran&ccedil;ois Panneton, Pierre L'Ecuyer and Makoto Matsumoto.
 * 
 * <p>
 * This generator is described in a paper by Fran&ccedil;ois Panneton, Pierre L'Ecuyer and Makoto Matsumoto <a
 * href="http://www.iro.umontreal.ca/~lecuyer/myftp/papers/wellrng.pdf">Improved Long-Period Generators Based on Linear
 * Recurrences Modulo 2</a> ACM Transactions on Mathematical Software, 32, 1 (2006). The errata for the paper are in <a
 * href="http://www.iro.umontreal.ca/~lecuyer/myftp/papers/wellrng-errata.txt">wellrng-errata.txt</a>.
 * </p>
 * 
 * @see <a href="http://www.iro.umontreal.ca/~panneton/WELLRNG.html">WELL Random number generator</a>
 * @version $Id: Well1024a.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 2.2
 */
public class Well1024a extends AbstractWell {

    /** Serializable version identifier. */
    private static final long serialVersionUID = 5680173464174485492L;

    /** Number of bits in the pool. */
    private static final int K = 1024;

    /** First parameter of the algorithm. */
    private static final int M1 = 3;

    /** Second parameter of the algorithm. */
    private static final int M2 = 24;

    /** Third parameter of the algorithm. */
    private static final int M3 = 10;

    /**
     * Creates a new random number generator.
     * <p>
     * The instance is initialized using the current time as the seed.
     * </p>
     */
    public Well1024a() {
        super(K, M1, M2, M3);
    }

    /**
     * Creates a new random number generator using a single int seed.
     * 
     * @param seed
     *        the initial seed (32 bits integer)
     */
    public Well1024a(final int seed) {
        super(K, M1, M2, M3, seed);
    }

    /**
     * Creates a new random number generator using an int array seed.
     * 
     * @param seed
     *        the initial seed (32 bits integers array), if null
     *        the seed of the generator will be related to the current time
     */
    public Well1024a(final int[] seed) {
        super(K, M1, M2, M3, seed);
    }

    /**
     * Creates a new random number generator using a single long seed.
     * 
     * @param seed
     *        the initial seed (64 bits integer)
     */
    public Well1024a(final long seed) {
        super(K, M1, M2, M3, seed);
    }

    /** {@inheritDoc} */
    @Override
    protected int next(final int bits) {

        final int indexRm1 = this.iRm1[this.index];

        final int v0 = this.v[this.index];
        final int vM1 = this.v[this.i1[this.index]];
        final int vM2 = this.v[this.i2[this.index]];
        final int vM3 = this.v[this.i3[this.index]];

        final int z0 = this.v[indexRm1];
        final int z1 = v0 ^ (vM1 ^ (vM1 >>> 8));
        final int z2 = (vM2 ^ (vM2 << 19)) ^ (vM3 ^ (vM3 << 14));
        final int z3 = z1 ^ z2;
        final int z0z011 = z0 ^ (z0 << 11);
        final int z1z17 = z1 ^ (z1 << 7);
        final int z4 = z0z011 ^ z1z17 ^ (z2 ^ (z2 << 13));

        this.v[this.index] = z3;
        this.v[indexRm1] = z4;
        this.index = indexRm1;

        return z4 >>> (32 - bits);

    }

    // CHECKSTYLE: resume MagicNumber check
    // CHECKSTYLE: resume CommentRatio check
}
