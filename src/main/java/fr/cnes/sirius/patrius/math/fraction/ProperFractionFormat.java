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
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.fraction;

import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;

import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Formats a Fraction number in proper format. The number format for each of
 * the whole number, numerator and, denominator can be configured.
 * <p>
 * Minus signs are only allowed in the whole number part - i.e., "-3 1/2" is legitimate and denotes -7/2, but "-3 -1/2"
 * is invalid and will result in a <code>ParseException</code>.
 * </p>
 * 
 * @since 1.1
 * @version $Id: ProperFractionFormat.java 18108 2017-10-04 06:45:27Z bignon $
 */
@SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
public class ProperFractionFormat extends FractionFormat {

    /** Serializable version identifier */
    private static final long serialVersionUID = 760934726031766749L;

    /** The format used for the whole number. */
    private NumberFormat wholeFormat;

    /**
     * Create a proper formatting instance with the default number format for
     * the whole, numerator, and denominator.
     */
    public ProperFractionFormat() {
        this(getDefaultNumberFormat());
    }

    /**
     * Create a proper formatting instance with a custom number format for the
     * whole, numerator, and denominator.
     * 
     * @param format
     *        the custom format for the whole, numerator, and
     *        denominator.
     */
    public ProperFractionFormat(final NumberFormat format) {
        this(format, (NumberFormat) format.clone(), (NumberFormat) format.clone());
    }

    /**
     * Create a proper formatting instance with a custom number format for each
     * of the whole, numerator, and denominator.
     * 
     * @param wholeFormatIn
     *        the custom format for the whole.
     * @param numeratorFormat
     *        the custom format for the numerator.
     * @param denominatorFormat
     *        the custom format for the denominator.
     */
    public ProperFractionFormat(final NumberFormat wholeFormatIn,
        final NumberFormat numeratorFormat,
        final NumberFormat denominatorFormat) {
        super(numeratorFormat, denominatorFormat);
        this.setWholeFormat(wholeFormatIn);
    }

    /**
     * Formats a {@link Fraction} object to produce a string. The fraction
     * is output in proper format.
     * 
     * @param fraction
     *        the object to format.
     * @param toAppendTo
     *        where the text is to be appended
     * @param pos
     *        On input: an alignment field, if desired. On output: the
     *        offsets of the alignment field
     * @return the value passed in as toAppendTo.
     */
    @Override
    public StringBuffer format(final Fraction fraction, final StringBuffer toAppendTo,
                               final FieldPosition pos) {

        // Initialization
        pos.setBeginIndex(0);
        pos.setEndIndex(0);

        int num = fraction.getNumerator();
        final int den = fraction.getDenominator();
        final int whole = num / den;
        num = num % den;

        if (whole != 0) {
            // Not zero
            this.getWholeFormat().format(whole, toAppendTo, pos);
            toAppendTo.append(' ');
            num = Math.abs(num);
        }
        this.getNumeratorFormat().format(num, toAppendTo, pos);
        toAppendTo.append(" / ");
        this.getDenominatorFormat().format(den, toAppendTo,
            pos);

        // Return result
        //
        return toAppendTo;
    }

    /**
     * Access the whole format.
     * 
     * @return the whole format.
     */
    public NumberFormat getWholeFormat() {
        return this.wholeFormat;
    }

    /**
     * Parses a string to produce a {@link Fraction} object. This method
     * expects the string to be formatted as a proper fraction.
     * <p>
     * Minus signs are only allowed in the whole number part - i.e., "-3 1/2" is legitimate and denotes -7/2, but
     * "-3 -1/2" is invalid and will result in a <code>ParseException</code>.
     * </p>
     * 
     * @param source
     *        the string to parse
     * @param pos
     *        input/ouput parsing parameter.
     * @return the parsed {@link Fraction} object.
     */
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Commons-Math code kept as such
    @Override
    public Fraction parse(final String source, final ParsePosition pos) {
        // CHECKSTYLE: resume ReturnCount check
        // try to parse improper fraction
        final Fraction ret = super.parse(source, pos);
        if (ret != null) {
            return ret;
        }

        final int initialIndex = pos.getIndex();

        // parse whitespace
        parseAndIgnoreWhitespace(source, pos);

        // parse whole
        final Number whole = this.getWholeFormat().parse(source, pos);
        if (whole == null) {
            // invalid integer number
            // set index back to initial, error index should already be set
            // character examined.
            pos.setIndex(initialIndex);
            return null;
        }

        // parse whitespace
        parseAndIgnoreWhitespace(source, pos);

        // parse numerator
        final Number num = this.getNumeratorFormat().parse(source, pos);
        if (num == null) {
            // invalid integer number
            // set index back to initial, error index should already be set
            // character examined.
            pos.setIndex(initialIndex);
            return null;
        }

        if (num.intValue() < 0) {
            // minus signs should be leading, invalid expression
            pos.setIndex(initialIndex);
            return null;
        }

        // parse '/'
        final int startIndex = pos.getIndex();
        final char c = parseNextCharacter(source, pos);
        switch (c) {
            case 0:
                // no '/'
                // return num as a fraction
                return new Fraction(num.intValue(), 1);
            case '/':
                // found '/', continue parsing denominator
                break;
            default:
                // invalid '/'
                // set index back to initial, error index should be the last
                // character examined.
                pos.setIndex(initialIndex);
                pos.setErrorIndex(startIndex);
                return null;
        }

        // parse whitespace
        parseAndIgnoreWhitespace(source, pos);

        // parse denominator
        final Number den = this.getDenominatorFormat().parse(source, pos);
        if (den == null) {
            // invalid integer number
            // set index back to initial, error index should already be set
            // character examined.
            pos.setIndex(initialIndex);
            return null;
        }

        if (den.intValue() < 0) {
            // minus signs must be leading, invalid
            pos.setIndex(initialIndex);
            return null;
        }

        final int w = whole.intValue();
        final int n = num.intValue();
        final int d = den.intValue();
        return new Fraction(((Math.abs(w) * d) + n) * MathUtils.copySign(1, w), d);
    }

    /**
     * Modify the whole format.
     * 
     * @param format
     *        The new whole format value.
     * @throws NullArgumentException
     *         if {@code format} is {@code null}.
     */
    public void setWholeFormat(final NumberFormat format) {
        if (format == null) {
            throw new NullArgumentException(PatriusMessages.WHOLE_FORMAT);
        }
        this.wholeFormat = format;
    }
}
