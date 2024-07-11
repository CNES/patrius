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
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.dfp;

// CHECKSTYLE: stop CommentRatio check
// Reason: model - Commons-Math code kept as such

/**
 * Subclass of {@link Dfp} which hides the radix-10000 artifacts of the superclass.
 * This should give outward appearances of being a decimal number with DIGITS*4-3
 * decimal digits. This class can be subclassed to appear to be an arbitrary number
 * of decimal digits less than DIGITS*4-3.
 * 
 * @version $Id: DfpDec.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 2.2
 */
@SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
public class DfpDec extends Dfp {

    /** 1000. */
    private static final int ONE_THOUSAND = 1000;

    /** 10 */
    private static final int C_10 = 10;

    /**
     * Makes an instance with a value of zero.
     * 
     * @param factory
     *        factory linked to this instance
     */
    protected DfpDec(final DfpField factory) {
        super(factory);
    }

    /**
     * Create an instance from a byte value.
     * 
     * @param factory
     *        factory linked to this instance
     * @param x
     *        value to convert to an instance
     */
    protected DfpDec(final DfpField factory, final byte x) {
        super(factory, x);
    }

    /**
     * Create an instance from an int value.
     * 
     * @param factory
     *        factory linked to this instance
     * @param x
     *        value to convert to an instance
     */
    protected DfpDec(final DfpField factory, final int x) {
        super(factory, x);
    }

    /**
     * Create an instance from a long value.
     * 
     * @param factory
     *        factory linked to this instance
     * @param x
     *        value to convert to an instance
     */
    protected DfpDec(final DfpField factory, final long x) {
        super(factory, x);
    }

    /**
     * Create an instance from a double value.
     * 
     * @param factory
     *        factory linked to this instance
     * @param x
     *        value to convert to an instance
     */
    protected DfpDec(final DfpField factory, final double x) {
        super(factory, x);
        this.round(0);
    }

    /**
     * Copy constructor.
     * 
     * @param d
     *        instance to copy
     */
    public DfpDec(final Dfp d) {
        super(d);
        this.round(0);
    }

    /**
     * Create an instance from a String representation.
     * 
     * @param factory
     *        factory linked to this instance
     * @param s
     *        string representation of the instance
     */
    protected DfpDec(final DfpField factory, final String s) {
        super(factory, s);
        this.round(0);
    }

    /**
     * Creates an instance with a non-finite value.
     * 
     * @param factory
     *        factory linked to this instance
     * @param sign
     *        sign of the Dfp to create
     * @param nans
     *        code of the value, must be one of {@link #INFINITE}, {@link #SNAN}, {@link #QNAN}
     */
    protected DfpDec(final DfpField factory, final byte sign, final byte nans) {
        super(factory, sign, nans);
    }

    /** {@inheritDoc} */
    @Override
    public Dfp newInstance() {
        return new DfpDec(this.getField());
    }

    /** {@inheritDoc} */
    @Override
    public Dfp newInstance(final byte x) {
        return new DfpDec(this.getField(), x);
    }

    /** {@inheritDoc} */
    @Override
    public Dfp newInstance(final int x) {
        return new DfpDec(this.getField(), x);
    }

    /** {@inheritDoc} */
    @Override
    public Dfp newInstance(final long x) {
        return new DfpDec(this.getField(), x);
    }

    /** {@inheritDoc} */
    @Override
    public Dfp newInstance(final double x) {
        return new DfpDec(this.getField(), x);
    }

    /** {@inheritDoc} */
    @Override
    public Dfp newInstance(final Dfp d) {

        // make sure we don't mix number with different precision
        if (this.getField().getRadixDigits() != d.getField().getRadixDigits()) {
            this.getField().setIEEEFlagsBits(DfpField.FLAG_INVALID);
            final Dfp result = this.newInstance(this.getZero());
            result.nans = QNAN;
            return this.dotrap(DfpField.FLAG_INVALID, "newInstance", d, result);
        }

        return new DfpDec(d);

    }

    /** {@inheritDoc} */
    @Override
    public Dfp newInstance(final String s) {
        return new DfpDec(this.getField(), s);
    }

    /** {@inheritDoc} */
    @Override
    public Dfp newInstance(final byte sign, final byte nans) {
        return new DfpDec(this.getField(), sign, nans);
    }

    /**
     * Get the number of decimal digits this class is going to represent.
     * Default implementation returns {@link #getRadixDigits()}*4-3. Subclasses can
     * override this to return something less.
     * 
     * @return number of decimal digits this class is going to represent
     */
    protected int getDecimalDigits() {
        return this.getRadixDigits() * 4 - 3;
    }

    /** {@inheritDoc} */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // CHECKSTYLE: stop ReturnCount check
    // CHECKSTYLE: stop MethodLength check
    // Reason: Commons-Math code kept as such
    @Override
    protected int round(final int x) {
        // CHECKSTYLE: resume MethodLength check
        // CHECKSTYLE: resume CyclomaticComplexity check
        // CHECKSTYLE: resume ReturnCount check

        final int msb = this.mant[this.mant.length - 1];
        if (msb == 0) {
            // special case -- this == zero
            return 0;
        }

        int cmaxdigits = this.mant.length * 4;
        int lsbthreshold = ONE_THOUSAND;
        while (lsbthreshold > msb) {
            lsbthreshold /= C_10;
            cmaxdigits--;
        }

        final int digits = this.getDecimalDigits();
        final int lsbshift = cmaxdigits - digits;

        lsbthreshold = 1;
        for (int i = 0; i < lsbshift % 4; i++) {
            lsbthreshold *= C_10;
        }


        // Special case
        if (lsbthreshold <= 1 && digits == 4 * this.mant.length - 3) {
            return super.round(x);
        }

        final int lsd = lsbshift / 4;
        final int lsb = this.mant[lsd];

        // not looking at this after this point
        int discarded = x;
        final int n;
        if (lsbthreshold == 1) {
            // look to the next digit for rounding
            n = (this.mant[lsd - 1] / ONE_THOUSAND) % C_10;
            this.mant[lsd - 1] %= ONE_THOUSAND;
            discarded |= this.mant[lsd - 1];
        } else {
            n = (lsb * C_10 / lsbthreshold) % C_10;
            discarded |= lsb % (lsbthreshold / C_10);
        }

        for (int i = 0; i < lsd; i++) {
            // need to know if there are any discarded bits
            discarded |= this.mant[i];
            this.mant[i] = 0;
        }

        this.mant[lsd] = lsb / lsbthreshold * lsbthreshold;

        final boolean inc;
        switch (this.getField().getRoundingMode()) {
            case ROUND_DOWN:
                // Round down
                inc = false;
                break;

            case ROUND_UP:
                // round up if n!=0
                inc = (n != 0) || (discarded != 0);
                break;

            case ROUND_HALF_UP:
                // round half up
                inc = n >= 5;
                break;

            case ROUND_HALF_DOWN:
                // round half down
                inc = n > 5;
                break;

            case ROUND_HALF_EVEN:
                // round half-even
                final boolean cond1 = n == 5 && discarded != 0;
                final boolean cond2 = (n == 5 && discarded == 0 && ((lsb / lsbthreshold) & 1) == 1);
                inc = (n > 5) || cond1 || cond2;
                break;

            case ROUND_HALF_ODD:
                // round half-odd
                final boolean cond3 = n == 5 && discarded != 0;
                final boolean cond4 = n == 5 && discarded == 0 && ((lsb / lsbthreshold) & 1) == 0;
                inc = (n > 5) || cond3 || cond4;
                break;

            case ROUND_CEIL:
                // round ceil
                inc = (this.sign == 1) && (n != 0 || discarded != 0);
                break;

            case ROUND_FLOOR:
            default:
                // round floor
                inc = (this.sign == -1) && (n != 0 || discarded != 0);
                break;
        }

        if (inc) {
            // increment if necessary
            int rh = lsbthreshold;
            for (int i = lsd; i < this.mant.length; i++) {
                final int r = this.mant[i] + rh;
                rh = r / RADIX;
                this.mant[i] = r % RADIX;
            }

            if (rh != 0) {
                this.shiftRight();
                this.mant[this.mant.length - 1] = rh;
            }
        }

        // Check for exceptional cases and raise signals if necessary
        if (this.exp < MIN_EXP) {
            // Gradual Underflow
            this.getField().setIEEEFlagsBits(DfpField.FLAG_UNDERFLOW);
            return DfpField.FLAG_UNDERFLOW;
        }

        if (this.exp > MAX_EXP) {
            // Overflow
            this.getField().setIEEEFlagsBits(DfpField.FLAG_OVERFLOW);
            return DfpField.FLAG_OVERFLOW;
        }

        if (n != 0 || discarded != 0) {
            // Inexact
            this.getField().setIEEEFlagsBits(DfpField.FLAG_INEXACT);
            return DfpField.FLAG_INEXACT;
        }
        // Return result
        return 0;
    }

    /** {@inheritDoc} */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Commons-Math code kept as such
    @Override
    public Dfp nextAfter(final Dfp x) {
        // CHECKSTYLE: resume ReturnCount check
        // CHECKSTYLE: resume CyclomaticComplexity check

        // Initialization
        final String trapName = "nextAfter";

        // make sure we don't mix number with different precision
        if (this.getField().getRadixDigits() != x.getField().getRadixDigits()) {
            this.getField().setIEEEFlagsBits(DfpField.FLAG_INVALID);
            final Dfp result = this.newInstance(this.getZero());
            result.nans = QNAN;
            return this.dotrap(DfpField.FLAG_INVALID, trapName, x, result);
        }

        // Initialization
        boolean up = false;

        // if this is greater than x
        if (this.lessThan(x)) {
            up = true;
        }

        if (this.equals(x)) {
            // Special case
            return this.newInstance(x);
        }

        if (this.lessThan(this.getZero())) {
            up = !up;
        }

        // Initialization
        Dfp result;
        Dfp inc;

        if (up) {
            // Up case
            inc = this.power10(this.log10() - this.getDecimalDigits() + 1);
            inc = copysign(inc, this);

            if (this.equals(this.getZero())) {
                inc = this.power10K(MIN_EXP - this.mant.length - 1);
            }

            if (inc.equals(this.getZero())) {
                result = copysign(this.newInstance(this.getZero()), this);
            } else {
                result = this.add(inc);
            }
        } else {
            // Down case
            inc = this.power10(this.log10());
            inc = copysign(inc, this);

            if (this.equals(inc)) {
                inc = inc.divide(this.power10(this.getDecimalDigits()));
            } else {
                inc = inc.divide(this.power10(this.getDecimalDigits() - 1));
            }

            if (this.equals(this.getZero())) {
                inc = this.power10K(MIN_EXP - this.mant.length - 1);
            }

            if (inc.equals(this.getZero())) {
                result = copysign(this.newInstance(this.getZero()), this);
            } else {
                result = this.subtract(inc);
            }
        }

        if (result.classify() == INFINITE && this.classify() != INFINITE) {
            this.getField().setIEEEFlagsBits(DfpField.FLAG_INEXACT);
            result = this.dotrap(DfpField.FLAG_INEXACT, trapName, x, result);
        }

        if (result.equals(this.getZero()) && !this.equals(this.getZero())) {
            this.getField().setIEEEFlagsBits(DfpField.FLAG_INEXACT);
            result = this.dotrap(DfpField.FLAG_INEXACT, trapName, x, result);
        }

        // Return result
        return result;
    }

    // CHECKSTYLE: resume CommentRatio check
}
