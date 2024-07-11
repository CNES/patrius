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

import java.util.Arrays;

import fr.cnes.sirius.patrius.math.FieldElement;

// CHECKSTYLE: stop MagicNumber check
// CHECKSTYLE: stop CyclomaticComplexity check
// CHECKSTYLE: stop ReturnCount check
// CHECKSTYLE: stop CommentRatio check
// Reason: model - Commons-Math code kept as such

/**
 * Decimal floating point library for Java
 * 
 * <p>
 * Another floating point class. This one is built using radix 10000 which is 10<sup>4</sup>, so its almost decimal.
 * </p>
 * 
 * <p>
 * The design goals here are:
 * <ol>
 * <li>Decimal math, or close to it</li>
 * <li>Settable precision (but no mix between numbers using different settings)</li>
 * <li>Portability. Code should be kept as portable as possible.</li>
 * <li>Performance</li>
 * <li>Accuracy - Results should always be +/- 1 ULP for basic algebraic operation</li>
 * <li>Comply with IEEE 854-1987 as much as possible. (See IEEE 854-1987 notes below)</li>
 * </ol>
 * </p>
 * 
 * <p>
 * Trade offs:
 * <ol>
 * <li>Memory foot print. I'm using more memory than necessary to represent numbers to get better performance.</li>
 * <li>Digits are bigger, so rounding is a greater loss. So, if you really need 12 decimal digits, better use 4 base
 * 10000 digits there can be one partially filled.</li>
 * </ol>
 * </p>
 * 
 * <p>
 * Numbers are represented in the following form:
 * 
 * <pre>
 * n  =  sign &times; mant &times; (radix)<sup>exp</sup>;
 * </p>
 * </pre>
 * where sign is &plusmn;1, mantissa represents a fractional number between
 * zero and one. mant[0] is the least significant digit.
 * exp is in the range of -32767 to 32768</p>
 * 
 * <p>
 * IEEE 854-1987 Notes and differences
 * </p>
 * 
 * <p>
 * IEEE 854 requires the radix to be either 2 or 10. The radix here is 10000, so that requirement is not met, but it is
 * possible that a subclassed can be made to make it behave as a radix 10 number. It is my opinion that if it looks and
 * behaves as a radix 10 number then it is one and that requirement would be met.
 * </p>
 * 
 * <p>
 * The radix of 10000 was chosen because it should be faster to operate on 4 decimal digits at once instead of one at a
 * time. Radix 10 behavior can be realized by adding an additional rounding step to ensure that the number of decimal
 * digits represented is constant.
 * </p>
 * 
 * <p>
 * The IEEE standard specifically leaves out internal data encoding, so it is reasonable to conclude that such a
 * subclass of this radix 10000 system is merely an encoding of a radix 10 system.
 * </p>
 * 
 * <p>
 * IEEE 854 also specifies the existence of "sub-normal" numbers. This class does not contain any such entities. The
 * most significant radix 10000 digit is always non-zero. Instead, we support "gradual underflow" by raising the
 * underflow flag for numbers less with exponent less than expMin, but don't flush to zero until the exponent reaches
 * MIN_EXP-digits. Thus the smallest number we can represent would be: 1E(-(MIN_EXP-digits-1)*4), eg, for digits=5,
 * MIN_EXP=-32767, that would be 1e-131092.
 * </p>
 * 
 * <p>
 * IEEE 854 defines that the implied radix point lies just to the right of the most significant digit and to the left of
 * the remaining digits. This implementation puts the implied radix point to the left of all digits including the most
 * significant one. The most significant digit here is the one just to the right of the radix point. This is a fine
 * detail and is really only a matter of definition. Any side effects of this can be rendered invisible by a subclass.
 * </p>
 * 
 * @see DfpField
 * @version $Id: Dfp.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 2.2
 */
@SuppressWarnings({"PMD.ShortClassName", "PMD.ConstructorCallsOverridableMethod"})
public class Dfp implements FieldElement<Dfp> {

    /** The radix, or base of this system. Set to 10000 */
    public static final int RADIX = 10000;

    /**
     * The minimum exponent before underflow is signaled. Flush to zero
     * occurs at minExp-DIGITS
     */
    public static final int MIN_EXP = -32767;

    /**
     * The maximum exponent before overflow is signaled and results flushed
     * to infinity
     */
    public static final int MAX_EXP = 32768;

    /** The amount under/overflows are scaled by before going to trap handler */
    public static final int ERR_SCALE = 32760;

    /** Indicator value for normal finite numbers. */
    public static final byte FINITE = 0;

    /** Indicator value for Infinity. */
    public static final byte INFINITE = 1;

    /** Indicator value for signaling NaN. */
    public static final byte SNAN = 2;

    /** Indicator value for quiet NaN. */
    public static final byte QNAN = 3;

    /** String for NaN representation. */
    private static final String NAN_STRING = "NaN";

    /** String for positive infinity representation. */
    private static final String POS_INFINITY_STRING = "Infinity";

    /** String for negative infinity representation. */
    private static final String NEG_INFINITY_STRING = "-Infinity";

    /** Name for traps triggered by addition. */
    private static final String ADD_TRAP = "add";

    /** Name for traps triggered by multiplication. */
    private static final String MULTIPLY_TRAP = "multiply";

    /** Name for traps triggered by division. */
    private static final String DIVIDE_TRAP = "divide";

    /** Name for traps triggered by square root. */
    private static final String SQRT_TRAP = "sqrt";

    /** Name for traps triggered by alignment. */
    private static final String ALIGN_TRAP = "align";

    /** Name for traps triggered by truncation. */
    private static final String TRUNC_TRAP = "trunc";

    /** Name for traps triggered by nextAfter. */
    private static final String NEXT_AFTER_TRAP = "nextAfter";

    /** Name for traps triggered by lessThan. */
    private static final String LESS_THAN_TRAP = "lessThan";

    /** Name for traps triggered by greaterThan. */
    private static final String GREATER_THAN_TRAP = "greaterThan";

    /** Name for traps triggered by newInstance. */
    private static final String NEW_INSTANCE_TRAP = "newInstance";

    /** Mantissa. */
    protected int[] mant;

    /** Sign bit: 1 for positive, -1 for negative. */
    protected byte sign;

    /** Exponent. */
    protected int exp;

    /** Indicator for non-finite / non-number values. */
    protected byte nans;

    /** Factory building similar Dfp's. */
    private final DfpField field;

    /**
     * Makes an instance with a value of zero.
     * 
     * @param fieldIn
     *        field to which this instance belongs
     */
    protected Dfp(final DfpField fieldIn) {
        this.mant = new int[fieldIn.getRadixDigits()];
        this.sign = 1;
        this.exp = 0;
        this.nans = FINITE;
        this.field = fieldIn;
    }

    /**
     * Create an instance from a byte value.
     * 
     * @param fieldIn
     *        field to which this instance belongs
     * @param x
     *        value to convert to an instance
     */
    protected Dfp(final DfpField fieldIn, final byte x) {
        this(fieldIn, (long) x);
    }

    /**
     * Create an instance from an int value.
     * 
     * @param fieldIn
     *        field to which this instance belongs
     * @param x
     *        value to convert to an instance
     */
    protected Dfp(final DfpField fieldIn, final int x) {
        this(fieldIn, (long) x);
    }

    /**
     * Create an instance from a long value.
     * 
     * @param fieldIn
     *        field to which this instance belongs
     * @param xIn
     *        value to convert to an instance
     */
    protected Dfp(final DfpField fieldIn, final long xIn) {

        long x = xIn;

        // initialize as if 0
        this.mant = new int[fieldIn.getRadixDigits()];
        this.nans = FINITE;
        this.field = fieldIn;

        boolean isLongMin = false;
        if (x == Long.MIN_VALUE) {
            // special case for Long.MIN_VALUE (-9223372036854775808)
            // we must shift it before taking its absolute value
            isLongMin = true;
            ++x;
        }

        // set the sign
        if (x < 0) {
            this.sign = -1;
            x = -x;
        } else {
            this.sign = 1;
        }

        this.exp = 0;
        while (x != 0) {
            System.arraycopy(this.mant, this.mant.length - this.exp, this.mant, this.mant.length - 1 - this.exp,
                this.exp);
            this.mant[this.mant.length - 1] = (int) (x % RADIX);
            x /= RADIX;
            this.exp++;
        }

        if (isLongMin) {
            // remove the shift added for Long.MIN_VALUE
            // we know in this case that fixing the last digit is sufficient
            for (int i = 0; i < this.mant.length - 1; i++) {
                if (this.mant[i] != 0) {
                    this.mant[i]++;
                    break;
                }
            }
        }
    }

    /**
     * Create an instance from a double value.
     * 
     * @param fieldIn
     *        field to which this instance belongs
     * @param x
     *        value to convert to an instance
     */
    protected Dfp(final DfpField fieldIn, final double x) {

        // initialize as if 0
        this.mant = new int[fieldIn.getRadixDigits()];
        this.sign = 1;
        this.exp = 0;
        this.nans = FINITE;
        this.field = fieldIn;

        final long bits = Double.doubleToLongBits(x);
        long mantissa = bits & 0x000fffffffffffffL;
        int exponent = (int) ((bits & 0x7ff0000000000000L) >> 52) - 1023;

        if (exponent == -1023) {
            // Zero or sub-normal
            if (x == 0) {
                // make sure 0 has the right sign
                if ((bits & 0x8000000000000000L) != 0) {
                    this.sign = -1;
                }
                return;
            }

            exponent++;

            // Normalize the subnormal number
            while ((mantissa & 0x0010000000000000L) == 0) {
                exponent--;
                mantissa <<= 1;
            }
            mantissa &= 0x000fffffffffffffL;
        }

        if (exponent == 1024) {
            // infinity or NAN
            if (x != x) {
                this.sign = (byte) 1;
                this.nans = QNAN;
            } else if (x < 0) {
                this.sign = (byte) -1;
                this.nans = INFINITE;
            } else {
                this.sign = (byte) 1;
                this.nans = INFINITE;
            }
            return;
        }

        Dfp xdfp = new Dfp(fieldIn, mantissa);
        // Divide by 2^52, then add one
        xdfp = xdfp.divide(new Dfp(fieldIn, 4503599627370496L)).add(fieldIn.getOne());
        xdfp = xdfp.multiply(DfpMath.pow(fieldIn.getTwo(), exponent));

        if ((bits & 0x8000000000000000L) != 0) {
            xdfp = xdfp.negate();
        }

        System.arraycopy(xdfp.mant, 0, this.mant, 0, this.mant.length);
        this.sign = xdfp.sign;
        this.exp = xdfp.exp;
        this.nans = xdfp.nans;

    }

    /**
     * Copy constructor.
     * 
     * @param d
     *        instance to copy
     */
    public Dfp(final Dfp d) {
        this.mant = d.mant.clone();
        this.sign = d.sign;
        this.exp = d.exp;
        this.nans = d.nans;
        this.field = d.field;
    }

    /**
     * Create an instance from a String representation.
     * 
     * @param fieldIn
     *        field to which this instance belongs
     * @param s
     *        string representation of the instance
     */
    // CHECKSTYLE: stop MethodLength check
    // Reason: Commons-Math code kept as such
    protected Dfp(final DfpField fieldIn, final String s) {
        // CHECKSTYLE: resume MethodLength check

        // initialize as if 0
        this.mant = new int[fieldIn.getRadixDigits()];
        this.sign = 1;
        this.exp = 0;
        this.nans = FINITE;
        this.field = fieldIn;

        // Check some special cases
        if (s.equals(POS_INFINITY_STRING)) {
            this.sign = (byte) 1;
            this.nans = INFINITE;
            return;
        }

        if (s.equals(NEG_INFINITY_STRING)) {
            this.sign = (byte) -1;
            this.nans = INFINITE;
            return;
        }

        if (s.equals(NAN_STRING)) {
            this.sign = (byte) 1;
            this.nans = QNAN;
            return;
        }

        // Check for scientific notation
        int p = s.indexOf("e");
        if (p == -1) {
            // try upper case?
            p = s.indexOf("E");
        }

        final String fpdecimal;
        int sciexp = 0;
        if (p == -1) {
            // normal case
            fpdecimal = s;
        } else {
            // scientific notation
            fpdecimal = s.substring(0, p);
            final String fpexp = s.substring(p + 1);
            boolean negative = false;

            for (int i = 0; i < fpexp.length(); i++) {
                if (fpexp.charAt(i) == '-') {
                    negative = true;
                    continue;
                }
                if (fpexp.charAt(i) >= '0' && fpexp.charAt(i) <= '9') {
                    sciexp = sciexp * 10 + fpexp.charAt(i) - '0';
                }
            }

            if (negative) {
                sciexp = -sciexp;
            }
        }

        // If there is a minus sign in the number then it is negative
        if (fpdecimal.indexOf("-") != -1) {
            this.sign = -1;
        }

        // First off, find all of the leading zeros, trailing zeros, and significant digits
        p = 0;

        boolean decimalFound = false;

        // size of radix in decimal digits
        final int rsize = 4;
        // Starting offset into Striped
        final int offset = 4;
        final char[] striped = new char[this.getRadixDigits() * rsize + offset * 2];

        // Move p to first significant digit
        int decimalPos = 0;
        for (;;) {
            if (fpdecimal.charAt(p) >= '1' && fpdecimal.charAt(p) <= '9') {
                break;
            }

            if (decimalFound && fpdecimal.charAt(p) == '0') {
                decimalPos--;
            }

            if (fpdecimal.charAt(p) == '.') {
                decimalFound = true;
            }

            p++;

            if (p == fpdecimal.length()) {
                break;
            }
        }

        // Copy the string onto Stripped
        int q = offset;
        striped[0] = '0';
        striped[1] = '0';
        striped[2] = '0';
        striped[3] = '0';
        int significantDigits = 0;
        for (;;) {
            if (p == (fpdecimal.length())) {
                break;
            }

            // Don't want to run pass the end of the array
            if (q == this.mant.length * rsize + offset + 1) {
                break;
            }

            if (fpdecimal.charAt(p) == '.') {
                decimalFound = true;
                decimalPos = significantDigits;
                p++;
                continue;
            }

            if (fpdecimal.charAt(p) < '0' || fpdecimal.charAt(p) > '9') {
                p++;
                continue;
            }

            striped[q] = fpdecimal.charAt(p);
            q++;
            p++;
            significantDigits++;
        }

        // If the decimal point has been found then get rid of trailing zeros.
        if (decimalFound && q != offset) {
            for (;;) {
                q--;
                if (q == offset) {
                    break;
                }
                if (striped[q] == '0') {
                    significantDigits--;
                } else {
                    break;
                }
            }
        }

        // special case of numbers like "0.00000"
        if (decimalFound && significantDigits == 0) {
            decimalPos = 0;
        }

        // Implicit decimal point at end of number if not present
        if (!decimalFound) {
            decimalPos = q - offset;
        }

        // Find the number of significant trailing zeros
        // set q to point to first sig digit
        q = offset;
        p = significantDigits - 1 + offset;

        while (p > q) {
            if (striped[p] != '0') {
                break;
            }
            p--;
        }

        // Make sure the decimal is on a mod 10000 boundary
        int i = ((rsize * 100) - decimalPos - sciexp % rsize) % rsize;
        q -= i;
        decimalPos += i;

        // Make the mantissa length right by adding zeros at the end if necessary
        while ((p - q) < (this.mant.length * rsize)) {
            for (i = 0; i < rsize; i++) {
                striped[++p] = '0';
            }
        }

        // Ok, now we know how many trailing zeros there are,
        // and where the least significant digit is
        for (i = this.mant.length - 1; i >= 0; i--) {
            this.mant[i] = (striped[q] - '0') * 1000 +
                    (striped[q + 1] - '0') * 100 +
                    (striped[q + 2] - '0') * 10 +
                    (striped[q + 3] - '0');
            q += 4;
        }

        this.exp = (decimalPos + sciexp) / rsize;

        if (q < striped.length) {
            // Is there possible another digit?
            this.round((striped[q] - '0') * 1000);
        }

    }

    /**
     * Creates an instance with a non-finite value.
     * 
     * @param fieldIn
     *        field to which this instance belongs
     * @param signIn
     *        sign of the Dfp to create
     * @param nansIn
     *        code of the value, must be one of {@link #INFINITE}, {@link #SNAN}, {@link #QNAN}
     */
    protected Dfp(final DfpField fieldIn, final byte signIn, final byte nansIn) {
        this.field = fieldIn;
        this.mant = new int[fieldIn.getRadixDigits()];
        this.sign = signIn;
        this.exp = 0;
        this.nans = nansIn;
    }

    /**
     * Create an instance with a value of 0.
     * Use this internally in preference to constructors to facilitate subclasses
     * 
     * @return a new instance with a value of 0
     */
    public Dfp newInstance() {
        return new Dfp(this.getField());
    }

    /**
     * Create an instance from a byte value.
     * 
     * @param x
     *        value to convert to an instance
     * @return a new instance with value x
     */
    public Dfp newInstance(final byte x) {
        return new Dfp(this.getField(), x);
    }

    /**
     * Create an instance from an int value.
     * 
     * @param x
     *        value to convert to an instance
     * @return a new instance with value x
     */
    public Dfp newInstance(final int x) {
        return new Dfp(this.getField(), x);
    }

    /**
     * Create an instance from a long value.
     * 
     * @param x
     *        value to convert to an instance
     * @return a new instance with value x
     */
    public Dfp newInstance(final long x) {
        return new Dfp(this.getField(), x);
    }

    /**
     * Create an instance from a double value.
     * 
     * @param x
     *        value to convert to an instance
     * @return a new instance with value x
     */
    public Dfp newInstance(final double x) {
        return new Dfp(this.getField(), x);
    }

    /**
     * Create an instance by copying an existing one.
     * Use this internally in preference to constructors to facilitate subclasses.
     * 
     * @param d
     *        instance to copy
     * @return a new instance with the same value as d
     */
    public Dfp newInstance(final Dfp d) {

        // make sure we don't mix number with different precision
        if (this.field.getRadixDigits() != d.field.getRadixDigits()) {
            this.field.setIEEEFlagsBits(DfpField.FLAG_INVALID);
            final Dfp result = this.newInstance(this.getZero());
            result.nans = QNAN;
            return this.dotrap(DfpField.FLAG_INVALID, NEW_INSTANCE_TRAP, d, result);
        }

        return new Dfp(d);

    }

    /**
     * Create an instance from a String representation.
     * Use this internally in preference to constructors to facilitate subclasses.
     * 
     * @param s
     *        string representation of the instance
     * @return a new instance parsed from specified string
     */
    public Dfp newInstance(final String s) {
        return new Dfp(this.field, s);
    }

    /**
     * Creates an instance with a non-finite value.
     * 
     * @param sig
     *        sign of the Dfp to create
     * @param code
     *        code of the value, must be one of {@link #INFINITE}, {@link #SNAN}, {@link #QNAN}
     * @return a new instance with a non-finite value
     */
    public Dfp newInstance(final byte sig, final byte code) {
        return this.field.newDfp(sig, code);
    }

    /**
     * Get the {@link fr.cnes.sirius.patrius.math.Field Field} (really a {@link DfpField}) to which the instance
     * belongs.
     * <p>
     * The field is linked to the number of digits and acts as a factory for {@link Dfp} instances.
     * </p>
     * 
     * @return {@link fr.cnes.sirius.patrius.math.Field Field} (really a {@link DfpField}) to which the instance belongs
     */
    @Override
    public DfpField getField() {
        return this.field;
    }

    /**
     * Get the number of radix digits of the instance.
     * 
     * @return number of radix digits
     */
    public int getRadixDigits() {
        return this.field.getRadixDigits();
    }

    /**
     * Get the constant 0.
     * 
     * @return a Dfp with value zero
     */
    public Dfp getZero() {
        return this.field.getZero();
    }

    /**
     * Get the constant 1.
     * 
     * @return a Dfp with value one
     */
    public Dfp getOne() {
        return this.field.getOne();
    }

    /**
     * Get the constant 2.
     * 
     * @return a Dfp with value two
     */
    public Dfp getTwo() {
        return this.field.getTwo();
    }

    /**
     * Shift the mantissa left, and adjust the exponent to compensate.
     */
    protected void shiftLeft() {
        for (int i = this.mant.length - 1; i > 0; i--) {
            this.mant[i] = this.mant[i - 1];
        }
        this.mant[0] = 0;
        this.exp--;
    }

    /*
     * Note that shiftRight() does not call round() as that round() itself
     * uses shiftRight()
     */
    /**
     * Shift the mantissa right, and adjust the exponent to compensate.
     */
    protected void shiftRight() {
        for (int i = 0; i < this.mant.length - 1; i++) {
            this.mant[i] = this.mant[i + 1];
        }
        this.mant[this.mant.length - 1] = 0;
        this.exp++;
    }

    /**
     * Make our exp equal to the supplied one, this may cause rounding.
     * Also causes de-normalized numbers. These numbers are generally
     * dangerous because most routines assume normalized numbers.
     * Align doesn't round, so it will return the last digit destroyed
     * by shifting right.
     * 
     * @param e
     *        desired exponent
     * @return last digit destroyed by shifting right
     */
    protected int align(final int e) {

        final int diff = this.exp - e;

        int adiff = diff;
        if (adiff < 0) {
            adiff = -adiff;
        }

        // Special case
        if (diff == 0) {
            return 0;
        }

        if (adiff > (this.mant.length + 1)) {
            // Special case
            Arrays.fill(this.mant, 0);
            this.exp = e;

            this.field.setIEEEFlagsBits(DfpField.FLAG_INEXACT);
            this.dotrap(DfpField.FLAG_INEXACT, ALIGN_TRAP, this, this);

            return 0;
        }

        // Initialization
        int lostdigit = 0;
        boolean inexact = false;

        for (int i = 0; i < adiff; i++) {
            if (diff < 0) {
                /*
                 * Keep track of loss -- only signal inexact after losing 2 digits.
                 * the first lost digit is returned to add() and may be incorporated
                 * into the result.
                 */
                if (lostdigit != 0) {
                    inexact = true;
                }

                lostdigit = this.mant[0];

                this.shiftRight();
            } else {
                this.shiftLeft();
            }
        }

        if (inexact) {
            this.field.setIEEEFlagsBits(DfpField.FLAG_INEXACT);
            this.dotrap(DfpField.FLAG_INEXACT, ALIGN_TRAP, this, this);
        }

        // Return result
        return lostdigit;

    }

    /**
     * Check if instance is less than x.
     * 
     * @param x
     *        number to check instance against
     * @return true if instance is less than x and neither are NaN, false otherwise
     */
    public boolean lessThan(final Dfp x) {

        final boolean res;

        // make sure we don't mix number with different precision
        if (this.field.getRadixDigits() != x.field.getRadixDigits()) {
            this.field.setIEEEFlagsBits(DfpField.FLAG_INVALID);
            final Dfp result = this.newInstance(this.getZero());
            result.nans = QNAN;
            this.dotrap(DfpField.FLAG_INVALID, LESS_THAN_TRAP, x, result);
            res = false;
        } else if (this.isNaN() || x.isNaN()) {
            /* if a nan is involved, signal invalid and return false */
            this.field.setIEEEFlagsBits(DfpField.FLAG_INVALID);
            this.dotrap(DfpField.FLAG_INVALID, LESS_THAN_TRAP, x, this.newInstance(this.getZero()));
            res = false;
        } else {
            res = compare(this, x) < 0;
        }
        return res;
    }

    /**
     * Check if instance is greater than x.
     * 
     * @param x
     *        number to check instance against
     * @return true if instance is greater than x and neither are NaN, false otherwise
     */
    public boolean greaterThan(final Dfp x) {

        final boolean res;

        // make sure we don't mix number with different precision
        if (this.field.getRadixDigits() != x.field.getRadixDigits()) {
            this.field.setIEEEFlagsBits(DfpField.FLAG_INVALID);
            final Dfp result = this.newInstance(this.getZero());
            result.nans = QNAN;
            this.dotrap(DfpField.FLAG_INVALID, GREATER_THAN_TRAP, x, result);
            res = false;
        } else if (this.isNaN() || x.isNaN()) {
            /* if a nan is involved, signal invalid and return false */
            this.field.setIEEEFlagsBits(DfpField.FLAG_INVALID);
            this.dotrap(DfpField.FLAG_INVALID, GREATER_THAN_TRAP, x, this.newInstance(this.getZero()));
            res = false;
        } else {
            res = compare(this, x) > 0;
        }
        return res;
    }

    /**
     * Check if instance is less than or equal to 0.
     * 
     * @return true if instance is not NaN and less than or equal to 0, false otherwise
     */
    public boolean negativeOrNull() {

        if (this.isNaN()) {
            this.field.setIEEEFlagsBits(DfpField.FLAG_INVALID);
            this.dotrap(DfpField.FLAG_INVALID, LESS_THAN_TRAP, this, this.newInstance(this.getZero()));
            return false;
        }

        return (this.sign < 0) || ((this.mant[this.mant.length - 1] == 0) && !this.isInfinite());

    }

    /**
     * Check if instance is strictly less than 0.
     * 
     * @return true if instance is not NaN and less than or equal to 0, false otherwise
     */
    public boolean strictlyNegative() {

        if (this.isNaN()) {
            this.field.setIEEEFlagsBits(DfpField.FLAG_INVALID);
            this.dotrap(DfpField.FLAG_INVALID, LESS_THAN_TRAP, this, this.newInstance(this.getZero()));
            return false;
        }

        return (this.sign < 0) && ((this.mant[this.mant.length - 1] != 0) || this.isInfinite());

    }

    /**
     * Check if instance is greater than or equal to 0.
     * 
     * @return true if instance is not NaN and greater than or equal to 0, false otherwise
     */
    public boolean positiveOrNull() {

        if (this.isNaN()) {
            this.field.setIEEEFlagsBits(DfpField.FLAG_INVALID);
            this.dotrap(DfpField.FLAG_INVALID, LESS_THAN_TRAP, this, this.newInstance(this.getZero()));
            return false;
        }

        return (this.sign > 0) || ((this.mant[this.mant.length - 1] == 0) && !this.isInfinite());

    }

    /**
     * Check if instance is strictly greater than 0.
     * 
     * @return true if instance is not NaN and greater than or equal to 0, false otherwise
     */
    public boolean strictlyPositive() {

        if (this.isNaN()) {
            this.field.setIEEEFlagsBits(DfpField.FLAG_INVALID);
            this.dotrap(DfpField.FLAG_INVALID, LESS_THAN_TRAP, this, this.newInstance(this.getZero()));
            return false;
        }

        return (this.sign > 0) && ((this.mant[this.mant.length - 1] != 0) || this.isInfinite());

    }

    /**
     * Get the absolute value of instance.
     * 
     * @return absolute value of instance
     */
    public Dfp abs() {
        final Dfp result = this.newInstance(this);
        result.sign = 1;
        return result;
    }

    /**
     * Check if instance is infinite.
     * 
     * @return true if instance is infinite
     */
    public boolean isInfinite() {
        return this.nans == INFINITE;
    }

    /**
     * Check if instance is not a number.
     * 
     * @return true if instance is not a number
     */
    public boolean isNaN() {
        return (this.nans == QNAN) || (this.nans == SNAN);
    }

    /**
     * Check if instance is equal to zero.
     * 
     * @return true if instance is equal to zero
     */
    public boolean isZero() {

        if (this.isNaN()) {
            this.field.setIEEEFlagsBits(DfpField.FLAG_INVALID);
            this.dotrap(DfpField.FLAG_INVALID, LESS_THAN_TRAP, this, this.newInstance(this.getZero()));
            return false;
        }

        return (this.mant[this.mant.length - 1] == 0) && !this.isInfinite();

    }

    /**
     * Check if instance is equal to x.
     * 
     * @param other
     *        object to check instance against
     * @return true if instance is equal to x and neither are NaN, false otherwise
     */
    @Override
    public boolean equals(final Object other) {

        if (other instanceof Dfp) {
            final Dfp x = (Dfp) other;
            if (this.isNaN() || x.isNaN() || this.field.getRadixDigits() != x.field.getRadixDigits()) {
                return false;
            }

            return compare(this, x) == 0;
        }

        return false;

    }

    /**
     * Gets a hashCode for the instance.
     * 
     * @return a hash code value for this object
     */
    @Override
    public int hashCode() {
        return 17 + (this.sign << 8) + (this.nans << 16) + this.exp + Arrays.hashCode(this.mant);
    }

    /**
     * Check if instance is not equal to x.
     * 
     * @param x
     *        number to check instance against
     * @return true if instance is not equal to x and neither are NaN, false otherwise
     */
    public boolean unequal(final Dfp x) {
        if (this.isNaN() || x.isNaN() || this.field.getRadixDigits() != x.field.getRadixDigits()) {
            return false;
        }

        return this.greaterThan(x) || this.lessThan(x);
    }

    /**
     * Compare two instances.
     * 
     * @param a
     *        first instance in comparison
     * @param b
     *        second instance in comparison
     * @return -1 if a<b, 1 if a>b and 0 if a==b
     *         Note this method does not properly handle NaNs or numbers with different precision.
     */
    private static int compare(final Dfp a, final Dfp b) {
        // Ignore the sign of zero
        if (a.mant[a.mant.length - 1] == 0 && b.mant[b.mant.length - 1] == 0 &&
                a.nans == FINITE && b.nans == FINITE) {
            return 0;
        }

        if (a.sign != b.sign) {
            if (a.sign == -1) {
                return -1;
            } else {
                return 1;
            }
        }

        // deal with the infinities
        if (a.nans == INFINITE && b.nans == FINITE) {
            return a.sign;
        }

        if (a.nans == FINITE && b.nans == INFINITE) {
            return -b.sign;
        }

        if (a.nans == INFINITE && b.nans == INFINITE) {
            return 0;
        }

        // Handle special case when a or b is zero, by ignoring the exponents
        if (b.mant[b.mant.length - 1] != 0 && a.mant[b.mant.length - 1] != 0) {
            if (a.exp < b.exp) {
                return -a.sign;
            }

            if (a.exp > b.exp) {
                return a.sign;
            }
        }

        // compare the mantissas
        for (int i = a.mant.length - 1; i >= 0; i--) {
            if (a.mant[i] > b.mant[i]) {
                return a.sign;
            }

            if (a.mant[i] < b.mant[i]) {
                return -a.sign;
            }
        }

        return 0;

    }

    /**
     * Round to nearest integer using the round-half-even method.
     * That is round to nearest integer unless both are equidistant.
     * In which case round to the even one.
     * 
     * @return rounded value
     */
    public Dfp rint() {
        return this.trunc(DfpField.RoundingMode.ROUND_HALF_EVEN);
    }

    /**
     * Round to an integer using the round floor mode.
     * That is, round toward -Infinity
     * 
     * @return rounded value
     */
    public Dfp floor() {
        return this.trunc(DfpField.RoundingMode.ROUND_FLOOR);
    }

    /**
     * Round to an integer using the round ceil mode.
     * That is, round toward +Infinity
     * 
     * @return rounded value
     */
    public Dfp ceil() {
        return this.trunc(DfpField.RoundingMode.ROUND_CEIL);
    }

    /**
     * Returns the IEEE remainder.
     * 
     * @param d
     *        divisor
     * @return this less n &times; d, where n is the integer closest to this/d
     */
    public Dfp remainder(final Dfp d) {

        final Dfp result = this.subtract(this.divide(d).rint().multiply(d));

        // IEEE 854-1987 says that if the result is zero, then it carries the sign of this
        if (result.mant[this.mant.length - 1] == 0) {
            result.sign = this.sign;
        }

        return result;

    }

    /**
     * Does the integer conversions with the specified rounding.
     * 
     * @param rmode
     *        rounding mode to use
     * @return truncated value
     */
    protected Dfp trunc(final DfpField.RoundingMode rmode) {

        if (this.isNaN()) {
            return this.newInstance(this);
        }

        if (this.nans == INFINITE) {
            return this.newInstance(this);
        }

        if (this.mant[this.mant.length - 1] == 0) {
            // a is zero
            return this.newInstance(this);
        }

        /*
         * If the exponent is less than zero then we can certainly
         * return zero
         */
        if (this.exp < 0) {
            this.field.setIEEEFlagsBits(DfpField.FLAG_INEXACT);
            Dfp result = this.newInstance(this.getZero());
            result = this.dotrap(DfpField.FLAG_INEXACT, TRUNC_TRAP, this, result);
            return result;
        }

        /*
         * If the exponent is greater than or equal to digits, then it
         * must already be an integer since there is no precision left
         * for any fractional part
         */

        if (this.exp >= this.mant.length) {
            return this.newInstance(this);
        }

        /*
         * General case: create another dfp, result, that contains the
         * a with the fractional part lopped off.
         */

        boolean changed = false;

        Dfp result = this.newInstance(this);
        for (int i = 0; i < this.mant.length - result.exp; i++) {
            changed |= result.mant[i] != 0;
            result.mant[i] = 0;
        }

        if (changed) {
            switch (rmode) {
                case ROUND_FLOOR:
                    if (result.sign == -1) {
                        // then we must increment the mantissa by one
                        result = result.add(this.newInstance(-1));
                    }
                    break;

                case ROUND_CEIL:
                    if (result.sign == 1) {
                        // then we must increment the mantissa by one
                        result = result.add(this.getOne());
                    }
                    break;

                case ROUND_HALF_EVEN:
                default:
                    final Dfp half = this.newInstance("0.5");
                    // difference between this and result
                    Dfp a = this.subtract(result);
                    // force positive (take abs)
                    a.sign = 1;
                    if (a.greaterThan(half)) {
                        a = this.newInstance(this.getOne());
                        a.sign = this.sign;
                        result = result.add(a);
                    }

                    /** If exactly equal to 1/2 and odd then increment */
                    if (a.equals(half) && result.exp > 0 && (result.mant[this.mant.length - result.exp] & 1) != 0) {
                        a = this.newInstance(this.getOne());
                        a.sign = this.sign;
                        result = result.add(a);
                    }
                    break;
            }

            // signal inexact
            this.field.setIEEEFlagsBits(DfpField.FLAG_INEXACT);
            result = this.dotrap(DfpField.FLAG_INEXACT, TRUNC_TRAP, this, result);
            return result;
        }

        // Return result
        return result;
    }

    /**
     * Convert this to an integer.
     * If greater than 2147483647, it returns 2147483647. If less than -2147483648 it returns -2147483648.
     * 
     * @return converted number
     */
    public int intValue() {
        // Initialization
        final Dfp rounded;

        rounded = this.rint();

        // Special case
        if (rounded.greaterThan(this.newInstance(2147483647))) {
            return 2147483647;
        }

        if (rounded.lessThan(this.newInstance(-2147483648))) {
            return -2147483648;
        }

        // Regular case
        int result = 0;

        for (int i = this.mant.length - 1; i >= this.mant.length - rounded.exp; i--) {
            result = result * RADIX + rounded.mant[i];
        }

        if (rounded.sign == -1) {
            result = -result;
        }

        // Return result
        return result;
    }

    /**
     * Get the exponent of the greatest power of 10000 that is
     * less than or equal to the absolute value of this. I.E. if
     * this is 10<sup>6</sup> then log10K would return 1.
     * 
     * @return integer base 10000 logarithm
     */
    public int log10K() {
        return this.exp - 1;
    }

    /**
     * Get the specified power of 10000.
     * 
     * @param e
     *        desired power
     * @return 10000<sup>e</sup>
     */
    public Dfp power10K(final int e) {
        final Dfp d = this.newInstance(this.getOne());
        d.exp = e + 1;
        return d;
    }

    /**
     * Get the exponent of the greatest power of 10 that is less than or equal to abs(this).
     * 
     * @return integer base 10 logarithm
     */
    public int log10() {
        if (this.mant[this.mant.length - 1] > 1000) {
            return this.exp * 4 - 1;
        }
        if (this.mant[this.mant.length - 1] > 100) {
            return this.exp * 4 - 2;
        }
        if (this.mant[this.mant.length - 1] > 10) {
            return this.exp * 4 - 3;
        }
        return this.exp * 4 - 4;
    }

    /**
     * Return the specified power of 10.
     * 
     * @param e
     *        desired power
     * @return 10<sup>e</sup>
     */
    public Dfp power10(final int e) {
        Dfp d = this.newInstance(this.getOne());

        if (e >= 0) {
            d.exp = e / 4 + 1;
        } else {
            d.exp = (e + 1) / 4;
        }

        switch ((e % 4 + 4) % 4) {
            case 0:
                break;
            case 1:
                d = d.multiply(10);
                break;
            case 2:
                d = d.multiply(100);
                break;
            default:
                d = d.multiply(1000);
                break;
        }

        return d;
    }

    /**
     * Negate the mantissa of this by computing the complement.
     * Leaves the sign bit unchanged, used internally by add.
     * Denormalized numbers are handled properly here.
     * 
     * @param extraIn
     *        ???
     * @return ???
     */
    protected int complement(final int extraIn) {

        int extra = extraIn;
        extra = RADIX - extra;
        for (int i = 0; i < this.mant.length; i++) {
            this.mant[i] = RADIX - this.mant[i] - 1;
        }

        int rh = extra / RADIX;
        extra = extra - rh * RADIX;
        for (int i = 0; i < this.mant.length; i++) {
            final int r = this.mant[i] + rh;
            rh = r / RADIX;
            this.mant[i] = r - rh * RADIX;
        }

        return extra;
    }

    /**
     * Add x to this.
     * 
     * @param x
     *        number to add
     * @return sum of this and x
     */
    // CHECKSTYLE: stop MethodLength check
    // Reason: Commons-Math code kept as such
    @Override
    public Dfp add(final Dfp x) {
        // CHECKSTYLE: resume MethodLength check

        // make sure we don't mix number with different precision
        if (this.field.getRadixDigits() != x.field.getRadixDigits()) {
            this.field.setIEEEFlagsBits(DfpField.FLAG_INVALID);
            final Dfp result = this.newInstance(this.getZero());
            result.nans = QNAN;
            return this.dotrap(DfpField.FLAG_INVALID, ADD_TRAP, x, result);
        }

        /* handle special cases */
        if (this.nans != FINITE || x.nans != FINITE) {
            if (this.isNaN()) {
                return this;
            }

            if (x.isNaN()) {
                return x;
            }

            if (this.nans == INFINITE && x.nans == FINITE) {
                return this;
            }

            if (x.nans == INFINITE && this.nans == FINITE) {
                return x;
            }

            if (x.nans == INFINITE && this.nans == INFINITE && this.sign == x.sign) {
                return x;
            }

            if (x.nans == INFINITE && this.nans == INFINITE && this.sign != x.sign) {
                this.field.setIEEEFlagsBits(DfpField.FLAG_INVALID);
                Dfp result = this.newInstance(this.getZero());
                result.nans = QNAN;
                result = this.dotrap(DfpField.FLAG_INVALID, ADD_TRAP, x, result);
                return result;
            }
        }

        /* copy this and the arg */
        final Dfp a = this.newInstance(this);
        final Dfp b = this.newInstance(x);

        /* initialize the result object */
        Dfp result = this.newInstance(this.getZero());

        /* Make all numbers positive, but remember their sign */
        final byte asign = a.sign;
        final byte bsign = b.sign;

        a.sign = 1;
        b.sign = 1;

        /* The result will be signed like the arg with greatest magnitude */
        byte rsign = bsign;
        if (compare(a, b) > 0) {
            rsign = asign;
        }

        /*
         * Handle special case when a or b is zero, by setting the exponent
         * of the zero number equal to the other one. This avoids an alignment
         * which would cause catastropic loss of precision
         */
        if (b.mant[this.mant.length - 1] == 0) {
            b.exp = a.exp;
        }

        if (a.mant[this.mant.length - 1] == 0) {
            a.exp = b.exp;
        }

        /* align number with the smaller exponent */
        int aextradigit = 0;
        int bextradigit = 0;
        if (a.exp < b.exp) {
            aextradigit = a.align(b.exp);
        } else {
            bextradigit = b.align(a.exp);
        }

        /* complement the smaller of the two if the signs are different */
        if (asign != bsign) {
            if (asign == rsign) {
                bextradigit = b.complement(bextradigit);
            } else {
                aextradigit = a.complement(aextradigit);
            }
        }

        /* add the mantissas */
        /* acts as a carry */
        int rh = 0;
        for (int i = 0; i < this.mant.length; i++) {
            final int r = a.mant[i] + b.mant[i] + rh;
            rh = r / RADIX;
            result.mant[i] = r - rh * RADIX;
        }
        result.exp = a.exp;
        result.sign = rsign;

        /*
         * handle overflow -- note, when asign!=bsign an overflow is
         * normal and should be ignored.
         */

        if (rh != 0 && (asign == bsign)) {
            final int lostdigit = result.mant[0];
            result.shiftRight();
            result.mant[this.mant.length - 1] = rh;
            final int excp = result.round(lostdigit);
            if (excp != 0) {
                result = this.dotrap(excp, ADD_TRAP, x, result);
            }
        }

        /* normalize the result */
        for (int i = 0; i < this.mant.length; i++) {
            if (result.mant[this.mant.length - 1] != 0) {
                break;
            }
            result.shiftLeft();
            if (i == 0) {
                result.mant[0] = aextradigit + bextradigit;
                aextradigit = 0;
                bextradigit = 0;
            }
        }

        /* result is zero if after normalization the most sig. digit is zero */
        if (result.mant[this.mant.length - 1] == 0) {
            result.exp = 0;

            if (asign != bsign) {
                // Unless adding 2 negative zeros, sign is positive
                // Per IEEE 854-1987 Section 6.3
                result.sign = 1;
            }
        }

        /* Call round to test for over/under flows */
        final int excp = result.round(aextradigit + bextradigit);
        if (excp != 0) {
            result = this.dotrap(excp, ADD_TRAP, x, result);
        }

        return result;
    }

    /**
     * Returns a number that is this number with the sign bit reversed.
     * 
     * @return the opposite of this
     */
    @Override
    public Dfp negate() {
        final Dfp result = this.newInstance(this);
        result.sign = (byte) -result.sign;
        return result;
    }

    /**
     * Subtract x from this.
     * 
     * @param x
     *        number to subtract
     * @return difference of this and a
     */
    @Override
    public Dfp subtract(final Dfp x) {
        return this.add(x.negate());
    }

    /**
     * Round this given the next digit n using the current rounding mode.
     * 
     * @param n
     *        ???
     * @return the IEEE flag if an exception occurred
     */
    protected int round(final int n) {
        boolean inc = false;
        switch (this.field.getRoundingMode()) {
            case ROUND_DOWN:
                inc = false;
                break;

            case ROUND_UP:
                // round up if n!=0
                inc = n != 0;
                break;

            case ROUND_HALF_UP:
                // round half up
                inc = n >= 5000;
                break;

            case ROUND_HALF_DOWN:
                // round half down
                inc = n > 5000;
                break;

            case ROUND_HALF_EVEN:
                // round half-even
                inc = n > 5000 || (n == 5000 && (this.mant[0] & 1) == 1);
                break;

            case ROUND_HALF_ODD:
                // round half-odd
                inc = n > 5000 || (n == 5000 && (this.mant[0] & 1) == 0);
                break;

            case ROUND_CEIL:
                // round ceil
                inc = this.sign == 1 && n != 0;
                break;

            case ROUND_FLOOR:
            default:
                // round floor
                inc = this.sign == -1 && n != 0;
                break;
        }

        if (inc) {
            // increment if necessary
            int rh = 1;
            for (int i = 0; i < this.mant.length; i++) {
                final int r = this.mant[i] + rh;
                rh = r / RADIX;
                this.mant[i] = r - rh * RADIX;
            }

            if (rh != 0) {
                this.shiftRight();
                this.mant[this.mant.length - 1] = rh;
            }
        }

        // check for exceptional cases and raise signals if necessary
        if (this.exp < MIN_EXP) {
            // Gradual Underflow
            this.field.setIEEEFlagsBits(DfpField.FLAG_UNDERFLOW);
            return DfpField.FLAG_UNDERFLOW;
        }

        if (this.exp > MAX_EXP) {
            // Overflow
            this.field.setIEEEFlagsBits(DfpField.FLAG_OVERFLOW);
            return DfpField.FLAG_OVERFLOW;
        }

        if (n != 0) {
            // Inexact
            this.field.setIEEEFlagsBits(DfpField.FLAG_INEXACT);
            return DfpField.FLAG_INEXACT;
        }

        return 0;

    }

    /**
     * Multiply this by x.
     * 
     * @param x
     *        multiplicand
     * @return product of this and x
     */
    @Override
    public Dfp multiply(final Dfp x) {

        // make sure we don't mix number with different precision
        if (this.field.getRadixDigits() != x.field.getRadixDigits()) {
            this.field.setIEEEFlagsBits(DfpField.FLAG_INVALID);
            final Dfp result = this.newInstance(this.getZero());
            result.nans = QNAN;
            return this.dotrap(DfpField.FLAG_INVALID, MULTIPLY_TRAP, x, result);
        }

        Dfp result = this.newInstance(this.getZero());

        /* handle special cases */
        if (this.nans != FINITE || x.nans != FINITE) {
            if (this.isNaN()) {
                return this;
            }

            if (x.isNaN()) {
                return x;
            }

            if (this.nans == INFINITE && x.nans == FINITE && x.mant[this.mant.length - 1] != 0) {
                result = this.newInstance(this);
                result.sign = (byte) (this.sign * x.sign);
                return result;
            }

            if (x.nans == INFINITE && this.nans == FINITE && this.mant[this.mant.length - 1] != 0) {
                result = this.newInstance(x);
                result.sign = (byte) (this.sign * x.sign);
                return result;
            }

            if (x.nans == INFINITE && this.nans == INFINITE) {
                result = this.newInstance(this);
                result.sign = (byte) (this.sign * x.sign);
                return result;
            }

            final boolean cond1 = (x.nans == INFINITE && this.nans == FINITE && this.mant[this.mant.length - 1] == 0);
            final boolean cond2 = (this.nans == INFINITE && x.nans == FINITE && x.mant[this.mant.length - 1] == 0);
            if (cond1 || cond2) {
                this.field.setIEEEFlagsBits(DfpField.FLAG_INVALID);
                result = this.newInstance(this.getZero());
                result.nans = QNAN;
                result = this.dotrap(DfpField.FLAG_INVALID, MULTIPLY_TRAP, x, result);
                return result;
            }
        }

        // Big enough to hold even the largest result
        final int[] product = new int[this.mant.length * 2];

        for (int i = 0; i < this.mant.length; i++) {
            // acts as a carry
            int rh = 0;
            for (int j = 0; j < this.mant.length; j++) {
                // multiply the 2 digits
                int r = this.mant[i] * x.mant[j];
                // add to the product digit with carry in
                r = r + product[i + j] + rh;

                rh = r / RADIX;
                product[i + j] = r - rh * RADIX;
            }
            product[i + this.mant.length] = rh;
        }

        // Find the most sig digit
        // default, in case result is zero
        int md = this.mant.length * 2 - 1;
        for (int i = this.mant.length * 2 - 1; i >= 0; i--) {
            if (product[i] != 0) {
                md = i;
                break;
            }
        }

        // Copy the digits into the result
        for (int i = 0; i < this.mant.length; i++) {
            result.mant[this.mant.length - i - 1] = product[md - i];
        }

        // Fixup the exponent.
        result.exp = this.exp + x.exp + md - 2 * this.mant.length + 1;
        result.sign = (byte) ((this.sign == x.sign) ? 1 : -1);

        if (result.mant[this.mant.length - 1] == 0) {
            // if result is zero, set exp to zero
            result.exp = 0;
        }

        final int excp;
        if (md > (this.mant.length - 1)) {
            excp = result.round(product[md - this.mant.length]);
        } else {
            // has no effect except to check status
            excp = result.round(0);
        }

        if (excp != 0) {
            result = this.dotrap(excp, MULTIPLY_TRAP, x, result);
        }

        return result;

    }

    /**
     * Multiply this by a single digit x.
     * 
     * @param x
     *        multiplicand
     * @return product of this and x
     */
    @Override
    public Dfp multiply(final int x) {
        if (x >= 0 && x < RADIX) {
            return this.multiplyFast(x);
        } else {
            return this.multiply(this.newInstance(x));
        }
    }

    /**
     * Multiply this by a single digit 0&lt;=x&lt;radix.
     * There are speed advantages in this special case.
     * 
     * @param x
     *        multiplicand
     * @return product of this and x
     */
    private Dfp multiplyFast(final int x) {
        Dfp result = this.newInstance(this);

        /* handle special cases */
        if (this.nans != FINITE) {
            if (this.isNaN()) {
                return this;
            }

            if (this.nans == INFINITE && x != 0) {
                result = this.newInstance(this);
                return result;
            }

            if (this.nans == INFINITE && x == 0) {
                this.field.setIEEEFlagsBits(DfpField.FLAG_INVALID);
                result = this.newInstance(this.getZero());
                result.nans = QNAN;
                result = this.dotrap(DfpField.FLAG_INVALID, MULTIPLY_TRAP, this.newInstance(this.getZero()), result);
                return result;
            }
        }

        /* range check x */
        if (x < 0 || x >= RADIX) {
            this.field.setIEEEFlagsBits(DfpField.FLAG_INVALID);
            result = this.newInstance(this.getZero());
            result.nans = QNAN;
            result = this.dotrap(DfpField.FLAG_INVALID, MULTIPLY_TRAP, result, result);
            return result;
        }

        int rh = 0;
        for (int i = 0; i < this.mant.length; i++) {
            final int r = this.mant[i] * x + rh;
            rh = r / RADIX;
            result.mant[i] = r - rh * RADIX;
        }

        int lostdigit = 0;
        if (rh != 0) {
            lostdigit = result.mant[0];
            result.shiftRight();
            result.mant[this.mant.length - 1] = rh;
        }

        if (result.mant[this.mant.length - 1] == 0) {
            // if result is zero, set exp to zero
            result.exp = 0;
        }

        final int excp = result.round(lostdigit);
        if (excp != 0) {
            result = this.dotrap(excp, MULTIPLY_TRAP, result, result);
        }

        return result;
    }

    /**
     * Divide this by divisor.
     * 
     * @param divisor
     *        divisor
     * @return quotient of this by divisor
     */
    // CHECKSTYLE: stop MethodLength check
    // Reason: Commons-Math code kept as such
    @Override
    public Dfp divide(final Dfp divisor) {
        // CHECKSTYLE: resume MethodLength check

        // make sure we don't mix number with different precision
        if (this.field.getRadixDigits() != divisor.field.getRadixDigits()) {
            this.field.setIEEEFlagsBits(DfpField.FLAG_INVALID);
            final Dfp result = this.newInstance(this.getZero());
            result.nans = QNAN;
            return this.dotrap(DfpField.FLAG_INVALID, DIVIDE_TRAP, divisor, result);
        }

        Dfp result = this.newInstance(this.getZero());

        /* handle special cases */
        if (this.nans != FINITE || divisor.nans != FINITE) {
            if (this.isNaN()) {
                return this;
            }

            if (divisor.isNaN()) {
                return divisor;
            }

            if (this.nans == INFINITE && divisor.nans == FINITE) {
                result = this.newInstance(this);
                result.sign = (byte) (this.sign * divisor.sign);
                return result;
            }

            if (divisor.nans == INFINITE && this.nans == FINITE) {
                result = this.newInstance(this.getZero());
                result.sign = (byte) (this.sign * divisor.sign);
                return result;
            }

            if (divisor.nans == INFINITE && this.nans == INFINITE) {
                this.field.setIEEEFlagsBits(DfpField.FLAG_INVALID);
                result = this.newInstance(this.getZero());
                result.nans = QNAN;
                result = this.dotrap(DfpField.FLAG_INVALID, DIVIDE_TRAP, divisor, result);
                return result;
            }
        }

        /* Test for divide by zero */
        if (divisor.mant[this.mant.length - 1] == 0) {
            this.field.setIEEEFlagsBits(DfpField.FLAG_DIV_ZERO);
            result = this.newInstance(this.getZero());
            result.sign = (byte) (this.sign * divisor.sign);
            result.nans = INFINITE;
            result = this.dotrap(DfpField.FLAG_DIV_ZERO, DIVIDE_TRAP, divisor, result);
            return result;
        }

        // current status of the dividend
        final int[] dividend;
        // quotient
        final int[] quotient;
        // remainder
        final int[] remainder;

        // one extra digit needed
        dividend = new int[this.mant.length + 1];
        // two extra digits needed 1 for overflow, 1 for rounding
        quotient = new int[this.mant.length + 2];
        // one extra digit needed
        remainder = new int[this.mant.length + 1];

        /* Initialize our most significant digits to zero */

        dividend[this.mant.length] = 0;
        quotient[this.mant.length] = 0;
        quotient[this.mant.length + 1] = 0;
        remainder[this.mant.length] = 0;

        // current quotient digit we're working with
        int qd;
        // number of significant quotient digits we have
        int nsqd;
        // trial quotient digit
        int trial = 0;
        // minimum adjustment
        int minadj;
        // Flag to indicate a good trail digit
        boolean trialgood;
        // most sig digit in result
        int md = 0;
        // exceptions
        final int excp;

        /*
         * copy our mantissa into the dividend, initialize the
         * quotient while we are at it
         */

        for (int i = 0; i < this.mant.length; i++) {
            dividend[i] = this.mant[i];
            quotient[i] = 0;
            remainder[i] = 0;
        }

        /* outer loop. Once per quotient digit */
        nsqd = 0;
        for (qd = this.mant.length + 1; qd >= 0; qd--) {
            /* Determine outer limits of our quotient digit */

            // r = most sig 2 digits of dividend
            final int divMsb = dividend[this.mant.length] * RADIX + dividend[this.mant.length - 1];
            int min = divMsb / (divisor.mant[this.mant.length - 1] + 1);
            int max = (divMsb + 1) / divisor.mant[this.mant.length - 1];

            trialgood = false;
            while (!trialgood) {
                // try the mean
                trial = (min + max) / 2;

                /* Multiply by divisor and store as remainder */
                int rh = 0;
                for (int i = 0; i < this.mant.length + 1; i++) {
                    final int dm = (i < this.mant.length) ? divisor.mant[i] : 0;
                    final int r = (dm * trial) + rh;
                    rh = r / RADIX;
                    remainder[i] = r - rh * RADIX;
                }

                /* subtract the remainder from the dividend */
                // carry in to aid the subtraction
                rh = 1;
                for (int i = 0; i < this.mant.length + 1; i++) {
                    final int r = ((RADIX - 1) - remainder[i]) + dividend[i] + rh;
                    rh = r / RADIX;
                    remainder[i] = r - rh * RADIX;
                }

                /* Lets analyze what we have here */
                if (rh == 0) {
                    // trial is too big -- negative remainder
                    max = trial - 1;
                    continue;
                }

                /* find out how far off the remainder is telling us we are */
                minadj = (remainder[this.mant.length] * RADIX) + remainder[this.mant.length - 1];
                minadj = minadj / (divisor.mant[this.mant.length - 1] + 1);

                if (minadj >= 2) {
                    // update the minimum
                    min = trial + minadj;
                    continue;
                }

                /*
                 * May have a good one here, check more thoroughly. Basically
                 * its a good one if it is less than the divisor
                 */
                // assume false
                trialgood = false;
                for (int i = this.mant.length - 1; i >= 0; i--) {
                    if (divisor.mant[i] > remainder[i]) {
                        trialgood = true;
                    }
                    if (divisor.mant[i] < remainder[i]) {
                        break;
                    }
                }

                if (remainder[this.mant.length] != 0) {
                    trialgood = false;
                }

                if (!trialgood) {
                    min = trial + 1;
                }
            }

            /* Great we have a digit! */
            quotient[qd] = trial;
            if (trial != 0 || nsqd != 0) {
                nsqd++;
            }

            if (this.field.getRoundingMode() == DfpField.RoundingMode.ROUND_DOWN && nsqd == this.mant.length) {
                // We have enough for this mode
                break;
            }

            if (nsqd > this.mant.length) {
                // We have enough digits
                break;
            }

            /* move the remainder into the dividend while left shifting */
            dividend[0] = 0;
            System.arraycopy(remainder, 0, dividend, 1, this.mant.length);
        }

        /* Find the most sig digit */
        // default
        md = this.mant.length;
        for (int i = this.mant.length + 1; i >= 0; i--) {
            if (quotient[i] != 0) {
                md = i;
                break;
            }
        }

        /* Copy the digits into the result */
        for (int i = 0; i < this.mant.length; i++) {
            result.mant[this.mant.length - i - 1] = quotient[md - i];
        }

        /* Fixup the exponent. */
        result.exp = this.exp - divisor.exp + md - this.mant.length;
        result.sign = (byte) ((this.sign == divisor.sign) ? 1 : -1);

        if (result.mant[this.mant.length - 1] == 0) {
            // if result is zero, set exp to zero
            result.exp = 0;
        }

        if (md > (this.mant.length - 1)) {
            excp = result.round(quotient[md - this.mant.length]);
        } else {
            excp = result.round(0);
        }

        if (excp != 0) {
            result = this.dotrap(excp, DIVIDE_TRAP, divisor, result);
        }

        return result;
    }

    /**
     * Divide by a single digit less than radix.
     * Special case, so there are speed advantages. 0 &lt;= divisor &lt; radix
     * 
     * @param divisor
     *        divisor
     * @return quotient of this by divisor
     */
    public Dfp divide(final int divisor) {

        // Handle special cases
        if (this.nans != FINITE) {
            if (this.isNaN()) {
                return this;
            }

            if (this.nans == INFINITE) {
                return this.newInstance(this);
            }
        }

        // Test for divide by zero
        if (divisor == 0) {
            this.field.setIEEEFlagsBits(DfpField.FLAG_DIV_ZERO);
            Dfp result = this.newInstance(this.getZero());
            result.sign = this.sign;
            result.nans = INFINITE;
            result = this.dotrap(DfpField.FLAG_DIV_ZERO, DIVIDE_TRAP, this.getZero(), result);
            return result;
        }

        // range check divisor
        if (divisor < 0 || divisor >= RADIX) {
            this.field.setIEEEFlagsBits(DfpField.FLAG_INVALID);
            Dfp result = this.newInstance(this.getZero());
            result.nans = QNAN;
            result = this.dotrap(DfpField.FLAG_INVALID, DIVIDE_TRAP, result, result);
            return result;
        }

        Dfp result = this.newInstance(this);

        int rl = 0;
        for (int i = this.mant.length - 1; i >= 0; i--) {
            final int r = rl * RADIX + result.mant[i];
            final int rh = r / divisor;
            rl = r - rh * divisor;
            result.mant[i] = rh;
        }

        if (result.mant[this.mant.length - 1] == 0) {
            // normalize
            result.shiftLeft();
            // compute the next digit and put it in
            final int r = rl * RADIX;
            final int rh = r / divisor;
            rl = r - rh * divisor;
            result.mant[0] = rh;
        }

        // do the rounding
        final int excp = result.round(rl * RADIX / divisor);
        if (excp != 0) {
            result = this.dotrap(excp, DIVIDE_TRAP, result, result);
        }

        return result;

    }

    /** {@inheritDoc} */
    @Override
    public Dfp reciprocal() {
        return this.field.getOne().divide(this);
    }

    /**
     * Compute the square root.
     * 
     * @return square root of the instance
     */
    public Dfp sqrt() {

        // check for unusual cases
        if (this.nans == FINITE && this.mant[this.mant.length - 1] == 0) {
            // if zero
            return this.newInstance(this);
        }

        if (this.nans != FINITE) {
            if (this.nans == INFINITE && this.sign == 1) {
                // if positive infinity
                return this.newInstance(this);
            }

            if (this.nans == QNAN) {
                return this.newInstance(this);
            }

            if (this.nans == SNAN) {
                Dfp result;

                this.field.setIEEEFlagsBits(DfpField.FLAG_INVALID);
                result = this.newInstance(this);
                result = this.dotrap(DfpField.FLAG_INVALID, SQRT_TRAP, null, result);
                return result;
            }
        }

        if (this.sign == -1) {
            // if negative
            Dfp result;

            this.field.setIEEEFlagsBits(DfpField.FLAG_INVALID);
            result = this.newInstance(this);
            result.nans = QNAN;
            result = this.dotrap(DfpField.FLAG_INVALID, SQRT_TRAP, null, result);
            return result;
        }

        Dfp x = this.newInstance(this);

        /* Lets make a reasonable guess as to the size of the square root */
        if (x.exp < -1 || x.exp > 1) {
            x.exp = this.exp / 2;
        }

        /* Coarsely estimate the mantissa */
        switch (x.mant[this.mant.length - 1] / 2000) {
            case 0:
                x.mant[this.mant.length - 1] = x.mant[this.mant.length - 1] / 2 + 1;
                break;
            case 2:
                x.mant[this.mant.length - 1] = 1500;
                break;
            case 3:
                x.mant[this.mant.length - 1] = 2200;
                break;
            default:
                x.mant[this.mant.length - 1] = 3000;
                break;
        }

        Dfp dx = this.newInstance(x);

        /*
         * Now that we have the first pass estimate, compute the rest
         * by the formula dx = (y - x*x) / (2x);
         */

        Dfp px = this.getZero();
        Dfp ppx = this.getZero();
        while (x.unequal(px)) {
            dx = this.newInstance(x);
            dx.sign = -1;
            dx = dx.add(this.divide(x));
            dx = dx.divide(2);
            ppx = px;
            px = x;
            x = x.add(dx);

            if (x.equals(ppx)) {
                // alternating between two values
                break;
            }

            // if dx is zero, break. Note testing the most sig digit
            // is a sufficient test since dx is normalized
            if (dx.mant[this.mant.length - 1] == 0) {
                break;
            }
        }

        return x;

    }

    /**
     * Get a string representation of the instance.
     * 
     * @return string representation of the instance
     */
    @Override
    public String toString() {
        if (this.nans != FINITE) {
            // if non-finite exceptional cases
            if (this.nans == INFINITE) {
                return (this.sign < 0) ? NEG_INFINITY_STRING : POS_INFINITY_STRING;
            } else {
                return NAN_STRING;
            }
        }

        if (this.exp > this.mant.length || this.exp < -1) {
            return this.dfp2sci();
        }

        return this.dfp2string();

    }

    /**
     * Convert an instance to a string using scientific notation.
     * 
     * @return string representation of the instance in scientific notation
     */
    protected String dfp2sci() {
        final char[] rawdigits = new char[this.mant.length * 4];
        final char[] outputbuffer = new char[this.mant.length * 4 + 20];
        int p;
        int q;
        final int shf;

        // Get all the digits
        p = 0;
        for (int i = this.mant.length - 1; i >= 0; i--) {
            rawdigits[p++] = (char) ((this.mant[i] / 1000) + '0');
            rawdigits[p++] = (char) (((this.mant[i] / 100) % 10) + '0');
            rawdigits[p++] = (char) (((this.mant[i] / 10) % 10) + '0');
            rawdigits[p++] = (char) (((this.mant[i]) % 10) + '0');
        }

        // Find the first non-zero one
        for (p = 0; p < rawdigits.length; p++) {
            if (rawdigits[p] != '0') {
                break;
            }
        }
        shf = p;

        // Now do the conversion
        q = 0;
        if (this.sign == -1) {
            outputbuffer[q++] = '-';
        }

        if (p == rawdigits.length) {
            outputbuffer[q++] = '0';
            outputbuffer[q++] = '.';
            outputbuffer[q++] = '0';
            outputbuffer[q++] = 'e';
            outputbuffer[q++] = '0';
            return new String(outputbuffer, 0, 5);
        } else {
            // there are non zero digits...
            outputbuffer[q++] = rawdigits[p++];
            outputbuffer[q++] = '.';

            while (p < rawdigits.length) {
                outputbuffer[q++] = rawdigits[p++];
            }
        }

        outputbuffer[q++] = 'e';

        // Initialization
        final int e;
        int ae;

        // Find the msd of the exponent

        e = this.exp * 4 - shf - 1;
        ae = e;
        if (e < 0) {
            ae = -e;
        }

        // Find the largest p such that p < e
        p = 1000000000;
        while (p > ae) {
            p /= 10;
        }

        if (e < 0) {
            outputbuffer[q++] = '-';
        }

        while (p > 0) {
            outputbuffer[q++] = (char) (ae / p + '0');
            ae = ae % p;
            p = p / 10;
        }

        return new String(outputbuffer, 0, q);

    }

    /**
     * Convert an instance to a string using normal notation.
     * 
     * @return string representation of the instance in normal notation
     */
    protected String dfp2string() {
        final char[] buffer = new char[this.mant.length * 4 + 20];
        int p = 1;
        int q;
        int e = this.exp;
        boolean pointInserted = false;

        buffer[0] = ' ';

        if (e <= 0) {
            buffer[p++] = '0';
            buffer[p++] = '.';
            pointInserted = true;
        }

        while (e < 0) {
            buffer[p++] = '0';
            buffer[p++] = '0';
            buffer[p++] = '0';
            buffer[p++] = '0';
            e++;
        }

        for (int i = this.mant.length - 1; i >= 0; i--) {
            buffer[p++] = (char) ((this.mant[i] / 1000) + '0');
            buffer[p++] = (char) (((this.mant[i] / 100) % 10) + '0');
            buffer[p++] = (char) (((this.mant[i] / 10) % 10) + '0');
            buffer[p++] = (char) (((this.mant[i]) % 10) + '0');
            e--;
            if (e == 0) {
                buffer[p++] = '.';
                pointInserted = true;
            }
        }

        while (e > 0) {
            buffer[p++] = '0';
            buffer[p++] = '0';
            buffer[p++] = '0';
            buffer[p++] = '0';
            e--;
        }

        if (!pointInserted) {
            // Ensure we have a radix point!
            buffer[p++] = '.';
        }

        // Suppress leading zeros
        q = 1;
        while (buffer[q] == '0') {
            q++;
        }
        if (buffer[q] == '.') {
            q--;
        }

        // Suppress trailing zeros
        while (buffer[p - 1] == '0') {
            p--;
        }

        // Insert sign
        if (this.sign < 0) {
            buffer[--q] = '-';
        }

        return new String(buffer, q, p - q);

    }

    /**
     * Raises a trap. This does not set the corresponding flag however.
     * 
     * @param type
     *        the trap type
     * @param what
     *        - name of routine trap occurred in
     * @param oper
     *        - input operator to function
     * @param result
     *        - the result computed prior to the trap
     * @return The suggested return value from the trap handler
     */
    public Dfp dotrap(final int type, final String what, final Dfp oper, final Dfp result) {
        Dfp def = result;

        switch (type) {
            case DfpField.FLAG_INVALID:
                def = this.newInstance(this.getZero());
                def.sign = result.sign;
                def.nans = QNAN;
                break;

            case DfpField.FLAG_DIV_ZERO:
                if (this.nans == FINITE && this.mant[this.mant.length - 1] != 0) {
                    // normal case, we are finite, non-zero
                    def = this.newInstance(this.getZero());
                    def.sign = (byte) (this.sign * oper.sign);
                    def.nans = INFINITE;
                }

                if (this.nans == FINITE && this.mant[this.mant.length - 1] == 0) {
                    // 0/0
                    def = this.newInstance(this.getZero());
                    def.nans = QNAN;
                }

                if (this.nans == INFINITE || this.nans == QNAN) {
                    def = this.newInstance(this.getZero());
                    def.nans = QNAN;
                }

                if (this.nans == INFINITE || this.nans == SNAN) {
                    def = this.newInstance(this.getZero());
                    def.nans = QNAN;
                }
                break;

            case DfpField.FLAG_UNDERFLOW:
                if ((result.exp + this.mant.length) < MIN_EXP) {
                    def = this.newInstance(this.getZero());
                    def.sign = result.sign;
                } else {
                    // gradual underflow
                    def = this.newInstance(result);
                }
                result.exp = result.exp + ERR_SCALE;
                break;

            case DfpField.FLAG_OVERFLOW:
                result.exp = result.exp - ERR_SCALE;
                def = this.newInstance(this.getZero());
                def.sign = result.sign;
                def.nans = INFINITE;
                break;

            default:
                def = result;
                break;
        }

        return this.trap(type, what, oper, def, result);

    }

    /**
     * Trap handler. Subclasses may override this to provide trap
     * functionality per IEEE 854-1987.
     * 
     * @param type
     *        The exception type - e.g. FLAG_OVERFLOW
     * @param what
     *        The name of the routine we were in e.g. divide()
     * @param oper
     *        An operand to this function if any
     * @param def
     *        The default return value if trap not enabled
     * @param result
     *        The result that is specified to be delivered per
     *        IEEE 854, if any
     * @return the value that should be return by the operation triggering the trap
     */
    protected Dfp trap(final int type, final String what, final Dfp oper, final Dfp def, final Dfp result) {
        return def;
    }

    /**
     * Returns the type - one of FINITE, INFINITE, SNAN, QNAN.
     * 
     * @return type of the number
     */
    public int classify() {
        return this.nans;
    }

    /**
     * Creates an instance that is the same as x except that it has the sign of y.
     * abs(x) = dfp.copysign(x, dfp.one)
     * 
     * @param x
     *        number to get the value from
     * @param y
     *        number to get the sign from
     * @return a number with the value of x and the sign of y
     */
    public static Dfp copysign(final Dfp x, final Dfp y) {
        final Dfp result = x.newInstance(x);
        result.sign = y.sign;
        return result;
    }

    /**
     * Returns the next number greater than this one in the direction of x.
     * If this==x then simply returns this.
     * 
     * @param x
     *        direction where to look at
     * @return closest number next to instance in the direction of x
     */
    public Dfp nextAfter(final Dfp x) {

        // make sure we don't mix number with different precision
        if (this.field.getRadixDigits() != x.field.getRadixDigits()) {
            this.field.setIEEEFlagsBits(DfpField.FLAG_INVALID);
            final Dfp result = this.newInstance(this.getZero());
            result.nans = QNAN;
            return this.dotrap(DfpField.FLAG_INVALID, NEXT_AFTER_TRAP, x, result);
        }

        // if this is greater than x
        boolean up = false;
        if (this.lessThan(x)) {
            up = true;
        }

        if (compare(this, x) == 0) {
            return this.newInstance(x);
        }

        if (this.lessThan(this.getZero())) {
            up = !up;
        }

        final Dfp inc;
        Dfp result;
        if (up) {
            inc = this.newInstance(this.getOne());
            inc.exp = this.exp - this.mant.length + 1;
            inc.sign = this.sign;

            if (this.equals(this.getZero())) {
                inc.exp = MIN_EXP - this.mant.length;
            }

            result = this.add(inc);
        } else {
            inc = this.newInstance(this.getOne());
            inc.exp = this.exp;
            inc.sign = this.sign;

            if (this.equals(inc)) {
                inc.exp = this.exp - this.mant.length;
            } else {
                inc.exp = this.exp - this.mant.length + 1;
            }

            if (this.equals(this.getZero())) {
                inc.exp = MIN_EXP - this.mant.length;
            }

            result = this.subtract(inc);
        }

        if (result.classify() == INFINITE && this.classify() != INFINITE) {
            this.field.setIEEEFlagsBits(DfpField.FLAG_INEXACT);
            result = this.dotrap(DfpField.FLAG_INEXACT, NEXT_AFTER_TRAP, x, result);
        }

        if (result.equals(this.getZero()) && !this.equals(this.getZero())) {
            this.field.setIEEEFlagsBits(DfpField.FLAG_INEXACT);
            result = this.dotrap(DfpField.FLAG_INEXACT, NEXT_AFTER_TRAP, x, result);
        }

        return result;

    }

    /**
     * Convert the instance into a double.
     * 
     * @return a double approximating the instance
     * @see #toSplitDouble()
     */
    public double toDouble() {

        if (this.isInfinite()) {
            if (this.lessThan(this.getZero())) {
                return Double.NEGATIVE_INFINITY;
            } else {
                return Double.POSITIVE_INFINITY;
            }
        }

        if (this.isNaN()) {
            return Double.NaN;
        }

        Dfp y = this;
        boolean negate = false;
        final int cmp0 = compare(this, this.getZero());
        if (cmp0 == 0) {
            return this.sign < 0 ? -0.0 : +0.0;
        } else if (cmp0 < 0) {
            y = this.negate();
            negate = true;
        }

        /*
         * Find the exponent, first estimate by integer log10, then adjust.
         * Should be faster than doing a natural logarithm.
         */
        int exponent = (int) (y.log10() * 3.32);
        if (exponent < 0) {
            exponent--;
        }

        Dfp tempDfp = DfpMath.pow(this.getTwo(), exponent);
        while (tempDfp.lessThan(y) || tempDfp.equals(y)) {
            tempDfp = tempDfp.multiply(2);
            exponent++;
        }
        exponent--;

        /* We have the exponent, now work on the mantissa */

        y = y.divide(DfpMath.pow(this.getTwo(), exponent));
        if (exponent > -1023) {
            y = y.subtract(this.getOne());
        }

        if (exponent < -1074) {
            return 0;
        }

        if (exponent > 1023) {
            return negate ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
        }

        y = y.multiply(this.newInstance(4503599627370496L)).rint();
        String str = y.toString();
        str = str.substring(0, str.length() - 1);
        long mantissa = Long.parseLong(str);

        if (mantissa == 4503599627370496L) {
            // Handle special case where we round up to next power of two
            mantissa = 0;
            exponent++;
        }

        /* Its going to be subnormal, so make adjustments */
        if (exponent <= -1023) {
            exponent--;
        }

        while (exponent < -1023) {
            exponent++;
            mantissa >>>= 1;
        }

        final long bits = mantissa | ((exponent + 1023L) << 52);
        double x = Double.longBitsToDouble(bits);

        if (negate) {
            x = -x;
        }

        return x;

    }

    /**
     * Convert the instance into a split double.
     * 
     * @return an array of two doubles which sum represent the instance
     * @see #toDouble()
     */
    public double[] toSplitDouble() {
        final double[] split = new double[2];
        final long mask = 0xffffffffc0000000L;

        split[0] = Double.longBitsToDouble(Double.doubleToLongBits(this.toDouble()) & mask);
        split[1] = this.subtract(this.newInstance(split[0])).toDouble();

        return split;
    }

    // CHECKSTYLE: resume CommentRatio check
    // CHECKSTYLE: resume MagicNumber check
    // CHECKSTYLE: resume CyclomaticComplexity check
    // CHECKSTYLE: resume ReturnCount check
}
