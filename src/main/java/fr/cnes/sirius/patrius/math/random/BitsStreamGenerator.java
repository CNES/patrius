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
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.random;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;

//CHECKSTYLE: stop MagicNumber check
//CHECKSTYLE: stop CommentRatio check
//Reason: Commons-Math model kept as such

/**
 * Base class for random number generators that generates bits streams.
 * 
 * @version $Id: BitsStreamGenerator.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 2.0
 */
//CHECKSTYLE: stop AbstractClassName check
@SuppressWarnings("PMD.AbstractNaming")
public abstract class BitsStreamGenerator implements RandomGenerator, Serializable {
    // CHECKSTYLE: resume AbstractClassName check

    /** Serializable version identifier */
    private static final long serialVersionUID = 20130104L;

    /** Next gaussian. */
    private double nextGaussianDouble;

    /**
     * Creates a new random number generator.
     */
    public BitsStreamGenerator() {
        this.nextGaussianDouble = Double.NaN;
    }

    /** {@inheritDoc} */
    @Override
    public abstract void setSeed(int seed);

    /** {@inheritDoc} */
    @Override
    public abstract void setSeed(int[] seed);

    /** {@inheritDoc} */
    @Override
    public abstract void setSeed(long seed);

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
    protected abstract int next(int bits);

    /** {@inheritDoc} */
    @Override
    public boolean nextBoolean() {
        return this.next(1) != 0;
    }

    /** {@inheritDoc} */
    @Override
    public void nextBytes(final byte[] bytes) {
        int i = 0;
        final int iEnd = bytes.length - 3;
        while (i < iEnd) {
            final int random = this.next(32);
            bytes[i] = (byte) (random & 0xff);
            bytes[i + 1] = (byte) ((random >> 8) & 0xff);
            bytes[i + 2] = (byte) ((random >> 16) & 0xff);
            bytes[i + 3] = (byte) ((random >> 24) & 0xff);
            i += 4;
        }
        int random = this.next(32);
        while (i < bytes.length) {
            bytes[i++] = (byte) (random & 0xff);
            random = random >> 8;
        }
    }

    /** {@inheritDoc} */
    @Override
    public double nextDouble() {
        final long high = ((long) this.next(26)) << 26;
        final int low = this.next(26);
        return (high | low) * 0x1.0p-52d;
    }

    /** {@inheritDoc} */
    @Override
    public float nextFloat() {
        return this.next(23) * 0x1.0p-23f;
    }

    /** {@inheritDoc} */
    @Override
    public double nextGaussian() {

        final double random;
        if (Double.isNaN(this.nextGaussianDouble)) {
            // generate a new pair of gaussian numbers
            final double x = this.nextDouble();
            final double y = this.nextDouble();
            final double alpha = 2 * FastMath.PI * x;
            final double r = MathLib.sqrt(-2 * MathLib.log(y));
            final double[] sincos = MathLib.sinAndCos(alpha);
            final double sin = sincos[0];
            final double cos = sincos[1];
            random = r * cos;
            this.nextGaussianDouble = r * sin;
        } else {
            // use the second element of the pair already generated
            random = this.nextGaussianDouble;
            this.nextGaussianDouble = Double.NaN;
        }

        return random;

    }

    /** {@inheritDoc} */
    @Override
    public int nextInt() {
        return this.next(32);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This default implementation is copied from Apache Harmony java.util.Random (r929253).
     * </p>
     * 
     * <p>
     * Implementation notes:
     * <ul>
     * <li>If n is a power of 2, this method returns {@code (int) ((n * (long) next(31)) >> 31)}.</li>
     * 
     * <li>If n is not a power of 2, what is returned is {@code next(31) % n} with {@code next(31)} values rejected
     * (i.e. regenerated) until a value that is larger than the remainder of {@code Integer.MAX_VALUE / n} is generated.
     * Rejection of this initial segment is necessary to ensure a uniform distribution.</li>
     * </ul>
     * </p>
     */
    @Override
    public int nextInt(final int n) {
        if (n > 0) {
            if ((n & -n) == n) {
                return (int) ((n * (long) this.next(31)) >> 31);
            }
            int bits;
            int val;
            do {
                bits = this.next(31);
                val = bits % n;
            } while (bits - val + (n - 1) < 0);
            return val;
        }
        throw new NotStrictlyPositiveException(n);
    }

    /** {@inheritDoc} */
    @Override
    public long nextLong() {
        final long high = ((long) this.next(32)) << 32;
        final long low = (this.next(32)) & 0xffffffffL;
        return high | low;
    }

    /**
     * Clears the cache used by the default implementation of {@link #nextGaussianDouble}.
     */
    public void clear() {
        this.nextGaussianDouble = Double.NaN;
    }

    // CHECKSTYLE: resume MagicNumber check
    // CHECKSTYLE: resume CommentRatio check
}
