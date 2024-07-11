/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 *
 * Copyright 2011-2022 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
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
package fr.cnes.sirius.patrius.math.geometry.partitioning.utilities;

import java.util.Arrays;

import fr.cnes.sirius.patrius.math.util.MathLib;

// CHECKSTYLE: stop MagicNumber check
// CHECKSTYLE: stop NestedBlockDepth check
// Reason: model - Commons-Math code

/**
 * This class implements an ordering operation for T-uples.
 * 
 * <p>
 * Ordering is done by encoding all components of the T-uple into a single scalar value and using this value as the
 * sorting key. Encoding is performed using the method invented by Georg Cantor in 1877 when he proved it was possible
 * to establish a bijection between a line and a plane. The binary representations of the components of the T-uple are
 * mixed together to form a single scalar. This means that the 2<sup>k</sup> bit of component 0 is followed by the
 * 2<sup>k</sup> bit of component 1, then by the 2<sup>k</sup> bit of component 2 up to the 2<sup>k</sup> bit of
 * component {@code t}, which is followed by the 2<sup>k-1</sup> bit of component 0, followed by the 2<sup>k-1</sup> bit
 * of component 1 ... The binary representations are extended as needed to handle numbers with different scales and a
 * suitable 2<sup>p</sup> offset is added to the components in order to avoid negative numbers (this offset is adjusted
 * as needed during the comparison operations).
 * </p>
 * 
 * <p>
 * The more interesting property of the encoding method for our purpose is that it allows to select all the points that
 * are in a given range. This is depicted in dimension 2 by the following picture:
 * </p>
 * 
 * <p>
 * T-uples with negative infinite or positive infinite components are sorted logically.
 * </p>
 * 
 * <p>
 * Since the specification of the {@code Comparator} interface allows only {@code ClassCastException} errors, some
 * arbitrary choices have been made to handle specific cases. The rationale for these choices is to keep
 * <em>regular</em> and consistent T-uples together.
 * </p>
 * <ul>
 * <li>instances with different dimensions are sorted according to their dimension regardless of their components values
 * </li>
 * <li>instances with {@code Double.NaN} components are sorted after all other ones (even after instances with positive
 * infinite components</li>
 * <li>instances with both positive and negative infinite components are considered as if they had {@code Double.NaN}
 * components</li>
 * </ul>
 * 
 * @version $Id: OrderedTuple.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.0
 */
public class OrderedTuple implements Comparable<OrderedTuple> {

    /** Sign bit mask. */
    private static final long SIGN_MASK = 0x8000000000000000L;

    /** Exponent bits mask. */
    private static final long EXPONENT_MASK = 0x7ff0000000000000L;

    /** Mantissa bits mask. */
    private static final long MANTISSA_MASK = 0x000fffffffffffffL;

    /** Implicit MSB for normalized numbers. */
    private static final long IMPLICIT_ONE = 0x0010000000000000L;

    /** Double components of the T-uple. */
    private final double[] components;

    /** Offset scale. */
    private int offset;

    /** Least Significant Bit scale. */
    @SuppressWarnings("PMD.ImmutableField")
    private int lsb;

    /** Ordering encoding of the double components. */
    private long[] encoding;

    /** Positive infinity marker. */
    private boolean posInf;

    /** Negative infinity marker. */
    private boolean negInf;

    /** Not A Number marker. */
    private boolean nan;

    /**
     * Build an ordered T-uple from its components.
     * 
     * @param componentsIn
     *        double components of the T-uple
     */
    public OrderedTuple(final double... componentsIn) {
        this.components = componentsIn.clone();
        int msb = Integer.MIN_VALUE;
        this.lsb = Integer.MAX_VALUE;
        this.posInf = false;
        this.negInf = false;
        this.nan = false;
        for (final double element : componentsIn) {
            if (Double.isInfinite(element)) {
                if (element < 0) {
                    this.negInf = true;
                } else {
                    this.posInf = true;
                }
            } else if (Double.isNaN(element)) {
                this.nan = true;
            } else {
                final long b = Double.doubleToLongBits(element);
                final long m = mantissa(b);
                if (m != 0) {
                    final int e = exponent(b);
                    msb = MathLib.max(msb, e + computeMSB(m));
                    this.lsb = MathLib.min(this.lsb, e + computeLSB(m));
                }
            }
        }

        if (this.posInf && this.negInf) {
            // instance cannot be sorted logically
            this.posInf = false;
            this.negInf = false;
            this.nan = true;
        }

        if (this.lsb <= msb) {
            // encode the T-upple with the specified offset
            this.encode(msb + 16);
        } else {
            this.encoding = new long[] { 0x0L };
        }

    }

    /**
     * Encode the T-uple with a given offset.
     * 
     * @param minOffset
     *        minimal scale of the offset to add to all
     *        components (must be greater than the MSBs of all components)
     */
    private void encode(final int minOffset) {

        // choose an offset with some margins
        this.offset = minOffset + 31;
        this.offset -= this.offset % 32;

        if ((this.encoding != null) && (this.encoding.length == 1) && (this.encoding[0] == 0x0L)) {
            // the components are all zeroes
            return;
        }

        // allocate an integer array to encode the components (we use only
        // 63 bits per element because there is no unsigned long in Java)
        final int neededBits = this.offset + 1 - this.lsb;
        final int neededLongs = (neededBits + 62) / 63;
        this.encoding = new long[this.components.length * neededLongs];

        // mix the bits from all components
        int eIndex = 0;
        int shift = 62;
        long word = 0x0L;
        for (int k = this.offset; eIndex < this.encoding.length; --k) {
            for (int vIndex = 0; vIndex < this.components.length; ++vIndex) {
                if (this.getBit(vIndex, k) != 0) {
                    word |= 0x1L << shift;
                }
                shift--;
                if (shift == 0) {
                    this.encoding[eIndex++] = word;
                    word = 0x0L;
                    shift = 62;
                }
            }
        }

    }

    /**
     * Compares this ordered T-uple with the specified object.
     * 
     * <p>
     * The ordering method is detailed in the general description of the class. Its main property is to be consistent
     * with distance: geometrically close T-uples stay close to each other when stored in a sorted collection using this
     * comparison method.
     * </p>
     * 
     * <p>
     * T-uples with negative infinite, positive infinite are sorted logically.
     * </p>
     * 
     * <p>
     * Some arbitrary choices have been made to handle specific cases. The rationale for these choices is to keep
     * <em>normal</em> and consistent T-uples together.
     * </p>
     * <ul>
     * <li>instances with different dimensions are sorted according to their dimension regardless of their components
     * values</li>
     * <li>instances with {@code Double.NaN} components are sorted after all other ones (evan after instances with
     * positive infinite components</li>
     * <li>instances with both positive and negative infinite components are considered as if they had
     * {@code Double.NaN} components</li>
     * </ul>
     * 
     * @param ot
     *        T-uple to compare instance with
     * @return a negative integer if the instance is less than the
     *         object, zero if they are equal, or a positive integer if the
     *         instance is greater than the object
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Commons-Math code kept as such
    @Override
    public int compareTo(final OrderedTuple ot) {
        // CHECKSTYLE: resume CyclomaticComplexity check
        // CHECKSTYLE: resume ReturnCount check
        // compare the length of the two Tuple
        if (this.components.length == ot.components.length) {
            // Direct case
            if (this.nan) {
                return +1;
            } else if (ot.nan) {
                return -1;
            } else if (this.negInf || ot.posInf) {
                return -1;
            } else if (this.posInf || ot.negInf) {
                return +1;
            } else {

                if (this.offset < ot.offset) {
                    this.encode(ot.offset);
                } else if (this.offset > ot.offset) {
                    ot.encode(this.offset);
                }
                // search min between the ot encoding length with the current Tuple encoding lenght
                final int limit = MathLib.min(this.encoding.length, ot.encoding.length);
                // loop on this minimum
                for (int i = 0; i < limit; ++i) {
                    if (this.encoding[i] < ot.encoding[i]) {
                        return -1;
                    } else if (this.encoding[i] > ot.encoding[i]) {
                        return +1;
                    }
                }

                if (this.encoding.length < ot.encoding.length) {
                    return -1;
                } else if (this.encoding.length > ot.encoding.length) {
                    return +1;
                } else {
                    return 0;
                }

            }
        }

        // General case
        //
        return this.components.length - ot.components.length;

    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        } else if (other instanceof OrderedTuple) {
            return this.compareTo((OrderedTuple) other) == 0;
        } else {
            return false;
        }
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        // the following constants are arbitrary small primes
        final int multiplier = 37;
        final int trueHash = 97;
        final int falseHash = 71;

        // hash fields and combine them
        // (we rely on the multiplier to have different combined weights
        // for all int fields and all boolean fields)
        int hash = Arrays.hashCode(this.components);
        hash = hash * multiplier + this.offset;
        hash = hash * multiplier + this.lsb;
        hash = hash * multiplier + (this.posInf ? trueHash : falseHash);
        hash = hash * multiplier + (this.negInf ? trueHash : falseHash);
        hash = hash * multiplier + (this.nan ? trueHash : falseHash);

        return hash;

    }

    /**
     * Get the components array.
     * 
     * @return array containing the T-uple components
     */
    public double[] getComponents() {
        return this.components.clone();
    }

    /**
     * Extract the sign from the bits of a double.
     * 
     * @param bits
     *        binary representation of the double
     * @return sign bit (zero if positive, non zero if negative)
     */
    private static long sign(final long bits) {
        return bits & SIGN_MASK;
    }

    /**
     * Extract the exponent from the bits of a double.
     * 
     * @param bits
     *        binary representation of the double
     * @return exponent
     */
    private static int exponent(final long bits) {
        return ((int) ((bits & EXPONENT_MASK) >> 52)) - 1075;
    }

    /**
     * Extract the mantissa from the bits of a double.
     * 
     * @param bits
     *        binary representation of the double
     * @return mantissa
     */
    private static long mantissa(final long bits) {
        final long masked = bits & EXPONENT_MASK;
        return (masked == 0) ?
            // subnormal number
            ((bits & MANTISSA_MASK) << 1) :
            // normal number
            (IMPLICIT_ONE | (bits & MANTISSA_MASK));
    }

    /**
     * Compute the most significant bit of a long.
     * 
     * @param l
     *        long from which the most significant bit is requested
     * @return scale of the most significant bit of {@code l},
     *         or 0 if {@code l} is zero
     * @see #computeLSB
     */
    private static int computeMSB(final long l) {

        long ll = l;
        // initialize the mask
        long mask = 0xffffffffL;
        // initialize the scale
        int scale = 32;
        // initialize the significant bit
        int msb = 0;

        while (scale != 0) {
            if ((ll & mask) != ll) {
                msb |= scale;
                ll = ll >> scale;
            }
            scale = scale >> 1;
            mask = mask >> scale;
        }

        return msb;

    }

    /**
     * Compute the least significant bit of a long.
     * 
     * @param l
     *        long from which the least significant bit is requested
     * @return scale of the least significant bit of {@code l},
     *         or 63 if {@code l} is zero
     * @see #computeMSB
     */
    private static int computeLSB(final long l) {

        long ll = l;
        // initialize the mask
        long mask = 0xffffffff00000000L;
        // initialize the scale
        int scale = 32;
        // initialize the significant bit
        int lsb = 0;

        while (scale != 0) {
            if ((ll & mask) == ll) {
                lsb |= scale;
                ll = ll >> scale;
            }
            scale = scale >> 1;
            mask = mask >> scale;
        }

        return lsb;

    }

    /**
     * Get a bit from the mantissa of a double.
     * 
     * @param i
     *        index of the component
     * @param k
     *        scale of the requested bit
     * @return the specified bit (either 0 or 1), after the offset has
     *         been added to the double
     */
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Commons-Math code kept as such
    private int getBit(final int i, final int k) {
        // CHECKSTYLE: resume ReturnCount check
        // components values conversion ( double to bits )
        final long bits = Double.doubleToLongBits(this.components[i]);
        // evaluate the exponent with the long(bits)
        final int e = exponent(bits);
        if ((k < e) || (k > this.offset)) {
            return 0;
        } else if (k == this.offset) {
            return (sign(bits) == 0L) ? 1 : 0;
        } else if (k > (e + 52)) {
            return (sign(bits) == 0L) ? 0 : 1;
        } else {
            final long m = (sign(bits) == 0L) ? mantissa(bits) : -mantissa(bits);
            return (int) ((m >> (k - e)) & 0x1L);
        }
    }

    // CHECKSTYLE: resume MagicNumber check
    // CHECKSTYLE: resume NestedBlockDepth check
}
