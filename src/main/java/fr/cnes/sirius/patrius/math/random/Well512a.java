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

//CHECKSTYLE: stop MagicNumber check
//CHECKSTYLE: stop CommentRatio check
//Reason: Commons-Math model kept as such

/**
 * This class implements the WELL512a pseudo-random number generator
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
 * @version $Id: Well512a.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 2.2
 */
public class Well512a extends AbstractWell {

     /** Serializable UID. */
    private static final long serialVersionUID = -6104179812103820574L;

    /** Number of bits in the pool. */
    private static final int K = 512;

    /** First parameter of the algorithm. */
    private static final int M1 = 13;

    /** Second parameter of the algorithm. */
    private static final int M2 = 9;

    /** Third parameter of the algorithm. */
    private static final int M3 = 5;

    /**
     * Creates a new random number generator.
     * <p>
     * The instance is initialized using the current time as the seed.
     * </p>
     */
    public Well512a() {
        super(K, M1, M2, M3);
    }

    /**
     * Creates a new random number generator using a single int seed.
     * 
     * @param seed
     *        the initial seed (32 bits integer)
     */
    public Well512a(final int seed) {
        super(K, M1, M2, M3, seed);
    }

    /**
     * Creates a new random number generator using an int array seed.
     * 
     * @param seed
     *        the initial seed (32 bits integers array), if null
     *        the seed of the generator will be related to the current time
     */
    public Well512a(final int[] seed) {
        super(K, M1, M2, M3, seed);
    }

    /**
     * Creates a new random number generator using a single long seed.
     * 
     * @param seed
     *        the initial seed (64 bits integer)
     */
    public Well512a(final long seed) {
        super(K, M1, M2, M3, seed);
    }

    /** {@inheritDoc} */
    @Override
    protected int next(final int bits) {

        final int indexRm1 = this.iRm1[this.index];

        final int vi = this.v[this.index];
        final int vi1 = this.v[this.i1[this.index]];
        final int vi2 = this.v[this.i2[this.index]];
        final int z0 = this.v[indexRm1];

        // the values below include the errata of the original article
        final int z1 = (vi ^ (vi << 16)) ^ (vi1 ^ (vi1 << 15));
        final int z2 = vi2 ^ (vi2 >>> 11);
        final int z3 = z1 ^ z2;
        final int z0z02 = z0 ^ (z0 << 2);
        final int z1z118 = z1 ^ (z1 << 18);
        final int z228 = z2 << 28;
        final int z3z35 = z3 ^ ((z3 << 5) & 0xda442d24);
        final int z4 = z0z02 ^ z1z118 ^ z228 ^ z3z35;

        this.v[this.index] = z3;
        this.v[indexRm1] = z4;
        this.index = indexRm1;

        return z4 >>> (32 - bits);

    }

    // CHECKSTYLE: resume MagicNumber check
    // CHECKSTYLE: resume CommentRatio check

}
