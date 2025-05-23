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
package fr.cnes.sirius.patrius.math.fraction;

import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Locale;

import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.exception.MathParseException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Formats a Fraction number in proper format or improper format. The number
 * format for each of the whole number, numerator and, denominator can be
 * configured.
 * 
 * @since 1.1
 * @version $Id: FractionFormat.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class FractionFormat extends AbstractFormat {

     /** Serializable UID. */
    private static final long serialVersionUID = 3008655719530972611L;

    /**
     * Create an improper formatting instance with the default number format
     * for the numerator and denominator.
     */
    public FractionFormat() {
        super();
        // Nothing to do
    }

    /**
     * Create an improper formatting instance with a custom number format for
     * both the numerator and denominator.
     * 
     * @param format
     *        the custom format for both the numerator and denominator.
     */
    public FractionFormat(final NumberFormat format) {
        super(format);
    }

    /**
     * Create an improper formatting instance with a custom number format for
     * the numerator and a custom number format for the denominator.
     * 
     * @param numeratorFormat
     *        the custom format for the numerator.
     * @param denominatorFormat
     *        the custom format for the denominator.
     */
    public FractionFormat(final NumberFormat numeratorFormat,
        final NumberFormat denominatorFormat) {
        super(numeratorFormat, denominatorFormat);
    }

    /**
     * Get the set of locales for which complex formats are available. This
     * is the same set as the {@link NumberFormat} set.
     * 
     * @return available complex format locales.
     */
    public static Locale[] getAvailableLocales() {
        return NumberFormat.getAvailableLocales();
    }

    /**
     * This static method calls formatFraction() on a default instance of
     * FractionFormat.
     * 
     * @param f
     *        Fraction object to format
     * @return a formatted fraction in proper form.
     */
    public static String formatFraction(final Fraction f) {
        return getImproperInstance().format(f);
    }

    /**
     * Returns the default complex format for the current locale.
     * 
     * @return the default complex format.
     */
    public static FractionFormat getImproperInstance() {
        return getImproperInstance(Locale.getDefault());
    }

    /**
     * Returns the default complex format for the given locale.
     * 
     * @param locale
     *        the specific locale used by the format.
     * @return the complex format specific to the given locale.
     */
    public static FractionFormat getImproperInstance(final Locale locale) {
        return new FractionFormat(getDefaultNumberFormat(locale));
    }

    /**
     * Returns the default complex format for the current locale.
     * 
     * @return the default complex format.
     */
    public static FractionFormat getProperInstance() {
        return getProperInstance(Locale.getDefault());
    }

    /**
     * Returns the default complex format for the given locale.
     * 
     * @param locale
     *        the specific locale used by the format.
     * @return the complex format specific to the given locale.
     */
    public static FractionFormat getProperInstance(final Locale locale) {
        return new ProperFractionFormat(getDefaultNumberFormat(locale));
    }

    /**
     * Create a default number format. The default number format is based on
     * {@link NumberFormat#getNumberInstance(java.util.Locale)} with the only
     * customizing is the maximum number of fraction digits, which is set to 0.
     * 
     * @return the default number format.
     */
    protected static NumberFormat getDefaultNumberFormat() {
        return getDefaultNumberFormat(Locale.getDefault());
    }

    /**
     * Formats a {@link Fraction} object to produce a string. The fraction is
     * output in improper format.
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
    public StringBuffer format(final Fraction fraction,
                               final StringBuffer toAppendTo, final FieldPosition pos) {

        pos.setBeginIndex(0);
        pos.setEndIndex(0);

        this.getNumeratorFormat().format(fraction.getNumerator(), toAppendTo, pos);
        toAppendTo.append(" / ");
        this.getDenominatorFormat().format(fraction.getDenominator(), toAppendTo,
            pos);

        return toAppendTo;
    }

    /**
     * Formats an object and appends the result to a StringBuffer. <code>obj</code> must be either a {@link Fraction}
     * object or a {@link Number} object. Any other type of
     * object will result in an {@link IllegalArgumentException} being thrown.
     * 
     * @param obj
     *        the object to format.
     * @param toAppendTo
     *        where the text is to be appended
     * @param pos
     *        On input: an alignment field, if desired. On output: the
     *        offsets of the alignment field
     * @return the value passed in as toAppendTo.
     * @see java.text.Format#format(java.lang.Object, java.lang.StringBuffer, java.text.FieldPosition)
     * @throws FractionConversionException
     *         if the number cannot be converted to a fraction
     * @throws MathIllegalArgumentException
     *         if <code>obj</code> is not a valid type.
     */
    @Override
    public StringBuffer format(final Object obj,
                               final StringBuffer toAppendTo, final FieldPosition pos) {
        StringBuffer ret = null;

        if (obj instanceof Fraction) {
            ret = this.format((Fraction) obj, toAppendTo, pos);
        } else if (obj instanceof Number) {
            ret = this.format(new Fraction(((Number) obj).doubleValue()), toAppendTo, pos);
        } else {
            throw new MathIllegalArgumentException(PatriusMessages.CANNOT_FORMAT_OBJECT_TO_FRACTION);
        }

        return ret;
    }

    /**
     * Parses a string to produce a {@link Fraction} object.
     * 
     * @param source
     *        the string to parse
     * @return the parsed {@link Fraction} object.
     * @exception MathParseException
     *            if the beginning of the specified string
     *            cannot be parsed.
     */
    @Override
    @SuppressWarnings("PMD.PrematureDeclaration")
    public Fraction parse(final String source) {
        final ParsePosition parsePosition = new ParsePosition(0);
        final Fraction result = this.parse(source, parsePosition);
        if (parsePosition.getIndex() == 0) {
            throw new MathParseException(source, parsePosition.getErrorIndex(), Fraction.class);
        }
        return result;
    }

    /**
     * Parses a string to produce a {@link Fraction} object. This method
     * expects the string to be formatted as an improper fraction.
     * 
     * @param source
     *        the string to parse
     * @param pos
     *        input/output parsing parameter.
     * @return the parsed {@link Fraction} object.
     */
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Commons-Math code kept as such
    @Override
    public Fraction parse(final String source, final ParsePosition pos) {
        // CHECKSTYLE: resume ReturnCount check
        final int initialIndex = pos.getIndex();

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

        return new Fraction(num.intValue(), den.intValue());
    }

}
