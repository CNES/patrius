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
package fr.cnes.sirius.patrius.math.complex;

import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Locale;

import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.exception.MathParseException;
import fr.cnes.sirius.patrius.math.exception.NoDataException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.util.CompositeFormat;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Formats a Complex number in cartesian format "Re(c) + Im(c)i". 'i' can
 * be replaced with 'j' (or anything else), and the number format for both real
 * and imaginary parts can be configured.
 * 
 * @version $Id: ComplexFormat.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class ComplexFormat {

    /** The default imaginary character. */
    private static final String DEFAULT_IMAGINARY_CHARACTER = "i";
    /** The notation used to signify the imaginary part of the complex number. */
    private final String imaginaryCharacter;
    /** The format used for the imaginary part. */
    private final NumberFormat imaginaryFormat;
    /** The format used for the real part. */
    private final NumberFormat realFormat;

    /**
     * Create an instance with the default imaginary character, 'i', and the
     * default number format for both real and imaginary parts.
     */
    public ComplexFormat() {
        this.imaginaryCharacter = DEFAULT_IMAGINARY_CHARACTER;
        this.imaginaryFormat = CompositeFormat.getDefaultNumberFormat();
        this.realFormat = this.imaginaryFormat;
    }

    /**
     * Create an instance with a custom number format for both real and
     * imaginary parts.
     * 
     * @param format
     *        the custom format for both real and imaginary parts.
     * @throws NullArgumentException
     *         if {@code realFormat} is {@code null}.
     */
    public ComplexFormat(final NumberFormat format) {
        if (format == null) {
            throw new NullArgumentException(PatriusMessages.IMAGINARY_FORMAT);
        }
        this.imaginaryCharacter = DEFAULT_IMAGINARY_CHARACTER;
        this.imaginaryFormat = format;
        this.realFormat = format;
    }

    /**
     * Create an instance with a custom number format for the real part and a
     * custom number format for the imaginary part.
     * 
     * @param realFormatIn
     *        the custom format for the real part.
     * @param imaginaryFormatIn
     *        the custom format for the imaginary part.
     * @throws NullArgumentException
     *         if {@code imaginaryFormat} is {@code null}.
     * @throws NullArgumentException
     *         if {@code realFormat} is {@code null}.
     */
    public ComplexFormat(final NumberFormat realFormatIn, final NumberFormat imaginaryFormatIn) {
        if (imaginaryFormatIn == null) {
            throw new NullArgumentException(PatriusMessages.IMAGINARY_FORMAT);
        }
        if (realFormatIn == null) {
            throw new NullArgumentException(PatriusMessages.REAL_FORMAT);
        }

        this.imaginaryCharacter = DEFAULT_IMAGINARY_CHARACTER;
        this.imaginaryFormat = imaginaryFormatIn;
        this.realFormat = realFormatIn;
    }

    /**
     * Create an instance with a custom imaginary character, and the default
     * number format for both real and imaginary parts.
     * 
     * @param imaginaryCharacterIn
     *        The custom imaginary character.
     * @throws NullArgumentException
     *         if {@code imaginaryCharacter} is {@code null}.
     * @throws NoDataException
     *         if {@code imaginaryCharacter} is an
     *         empty string.
     */
    public ComplexFormat(final String imaginaryCharacterIn) {
        this(imaginaryCharacterIn, CompositeFormat.getDefaultNumberFormat());
    }

    /**
     * Create an instance with a custom imaginary character, and a custom number
     * format for both real and imaginary parts.
     * 
     * @param imaginaryCharacterIn
     *        The custom imaginary character.
     * @param format
     *        the custom format for both real and imaginary parts.
     * @throws NullArgumentException
     *         if {@code imaginaryCharacter} is {@code null}.
     * @throws NoDataException
     *         if {@code imaginaryCharacter} is an
     *         empty string.
     * @throws NullArgumentException
     *         if {@code format} is {@code null}.
     */
    public ComplexFormat(final String imaginaryCharacterIn, final NumberFormat format) {
        this(imaginaryCharacterIn, format, format);
    }

    /**
     * Create an instance with a custom imaginary character, a custom number
     * format for the real part, and a custom number format for the imaginary
     * part.
     * 
     * @param imaginaryCharacterIn
     *        The custom imaginary character.
     * @param realFormatIn
     *        the custom format for the real part.
     * @param imaginaryFormatIn
     *        the custom format for the imaginary part.
     * @throws NullArgumentException
     *         if {@code imaginaryCharacter} is {@code null}.
     * @throws NoDataException
     *         if {@code imaginaryCharacter} is an
     *         empty string.
     * @throws NullArgumentException
     *         if {@code imaginaryFormat} is {@code null}.
     * @throws NullArgumentException
     *         if {@code realFormat} is {@code null}.
     */
    public ComplexFormat(final String imaginaryCharacterIn,
        final NumberFormat realFormatIn,
        final NumberFormat imaginaryFormatIn) {
        if (imaginaryCharacterIn == null) {
            throw new NullArgumentException();
        }
        if (imaginaryCharacterIn.length() == 0) {
            throw new NoDataException();
        }
        if (imaginaryFormatIn == null) {
            throw new NullArgumentException(PatriusMessages.IMAGINARY_FORMAT);
        }
        if (realFormatIn == null) {
            throw new NullArgumentException(PatriusMessages.REAL_FORMAT);
        }

        this.imaginaryCharacter = imaginaryCharacterIn;
        this.imaginaryFormat = imaginaryFormatIn;
        this.realFormat = realFormatIn;
    }

    /**
     * Get the set of locales for which complex formats are available.
     * <p>
     * This is the same set as the {@link NumberFormat} set.
     * </p>
     * 
     * @return available complex format locales.
     */
    public static Locale[] getAvailableLocales() {
        return NumberFormat.getAvailableLocales();
    }

    /**
     * This method calls {@link #format(Object,StringBuffer,FieldPosition)}.
     * 
     * @param c
     *        Complex object to format.
     * @return A formatted number in the form "Re(c) + Im(c)i".
     */
    public String format(final Complex c) {
        return this.format(c, new StringBuffer(), new FieldPosition(0)).toString();
    }

    /**
     * This method calls {@link #format(Object,StringBuffer,FieldPosition)}.
     * 
     * @param c
     *        Double object to format.
     * @return A formatted number.
     */
    public String format(final Double c) {
        return this.format(new Complex(c, 0), new StringBuffer(), new FieldPosition(0)).toString();
    }

    /**
     * Formats a {@link Complex} object to produce a string.
     * 
     * @param complex
     *        the object to format.
     * @param toAppendTo
     *        where the text is to be appended
     * @param pos
     *        On input: an alignment field, if desired. On output: the
     *        offsets of the alignment field
     * @return the value passed in as toAppendTo.
     */
    public StringBuffer format(final Complex complex, final StringBuffer toAppendTo,
                               final FieldPosition pos) {
        // Initialization
        pos.setBeginIndex(0);
        pos.setEndIndex(0);

        // format real
        final double re = complex.getReal();
        CompositeFormat.formatDouble(re, this.getRealFormat(), toAppendTo, pos);

        // format sign and imaginary
        final double im = complex.getImaginary();
        final StringBuffer imAppendTo;
        if (im < 0.0) {
            toAppendTo.append(" - ");
            imAppendTo = this.formatImaginary(-im, new StringBuffer(), pos);
            toAppendTo.append(imAppendTo);
            toAppendTo.append(this.getImaginaryCharacter());
        } else if (im > 0.0 || Double.isNaN(im)) {
            toAppendTo.append(" + ");
            imAppendTo = this.formatImaginary(im, new StringBuffer(), pos);
            toAppendTo.append(imAppendTo);
            toAppendTo.append(this.getImaginaryCharacter());
        }

        // Return result
        return toAppendTo;
    }

    /**
     * Format the absolute value of the imaginary part.
     * 
     * @param absIm
     *        Absolute value of the imaginary part of a complex number.
     * @param toAppendTo
     *        where the text is to be appended.
     * @param pos
     *        On input: an alignment field, if desired. On output: the
     *        offsets of the alignment field.
     * @return the value passed in as toAppendTo.
     */
    private StringBuffer formatImaginary(final double absIm,
                                         final StringBuffer toAppendTo, final FieldPosition pos) {
        pos.setBeginIndex(0);
        pos.setEndIndex(0);

        CompositeFormat.formatDouble(absIm, this.getImaginaryFormat(), toAppendTo, pos);
        if ("1".equals(toAppendTo.toString())) {
            // Remove the character "1" if it is the only one.
            toAppendTo.setLength(0);
        }

        return toAppendTo;
    }

    /**
     * Formats a object to produce a string. {@code obj} must be either a {@link Complex} object or a {@link Number}
     * object. Any other type of
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
     * @throws MathIllegalArgumentException
     *         is {@code obj} is not a valid type.
     */
    public StringBuffer format(final Object obj, final StringBuffer toAppendTo,
                               final FieldPosition pos) {

        StringBuffer ret = null;

        if (obj instanceof Complex) {
            ret = this.format((Complex) obj, toAppendTo, pos);
        } else if (obj instanceof Number) {
            ret = this.format(new Complex(((Number) obj).doubleValue(), 0.0),
                toAppendTo, pos);
        } else {
            throw new MathIllegalArgumentException(PatriusMessages.CANNOT_FORMAT_INSTANCE_AS_COMPLEX,
                obj.getClass().getName());
        }

        return ret;
    }

    /**
     * Access the imaginaryCharacter.
     * 
     * @return the imaginaryCharacter.
     */
    public String getImaginaryCharacter() {
        return this.imaginaryCharacter;
    }

    /**
     * Access the imaginaryFormat.
     * 
     * @return the imaginaryFormat.
     */
    public NumberFormat getImaginaryFormat() {
        return this.imaginaryFormat;
    }

    /**
     * Returns the default complex format for the current locale.
     * 
     * @return the default complex format.
     */
    public static ComplexFormat getInstance() {
        return getInstance(Locale.getDefault());
    }

    /**
     * Returns the default complex format for the given locale.
     * 
     * @param locale
     *        the specific locale used by the format.
     * @return the complex format specific to the given locale.
     */
    public static ComplexFormat getInstance(final Locale locale) {
        final NumberFormat f = CompositeFormat.getDefaultNumberFormat(locale);
        return new ComplexFormat(f);
    }

    /**
     * Returns the default complex format for the given locale.
     * 
     * @param locale
     *        the specific locale used by the format.
     * @param imaginaryCharacter
     *        Imaginary character.
     * @return the complex format specific to the given locale.
     * @throws NullArgumentException
     *         if {@code imaginaryCharacter} is {@code null}.
     * @throws NoDataException
     *         if {@code imaginaryCharacter} is an
     *         empty string.
     */
    public static ComplexFormat getInstance(final String imaginaryCharacter, final Locale locale) {
        final NumberFormat f = CompositeFormat.getDefaultNumberFormat(locale);
        return new ComplexFormat(imaginaryCharacter, f);
    }

    /**
     * Access the realFormat.
     * 
     * @return the realFormat.
     */
    public NumberFormat getRealFormat() {
        return this.realFormat;
    }

    /**
     * Parses a string to produce a {@link Complex} object.
     * 
     * @param source
     *        the string to parse.
     * @return the parsed {@link Complex} object.
     * @throws MathParseException
     *         if the beginning of the specified string
     *         cannot be parsed.
     */
    @SuppressWarnings("PMD.PrematureDeclaration")
    public Complex parse(final String source) {
        final ParsePosition parsePosition = new ParsePosition(0);
        final Complex result = this.parse(source, parsePosition);
        if (parsePosition.getIndex() == 0) {
            throw new MathParseException(source,
                parsePosition.getErrorIndex(),
                Complex.class);
        }
        return result;
    }

    /**
     * Parses a string to produce a {@link Complex} object.
     * 
     * @param source
     *        the string to parse
     * @param pos
     *        input/ouput parsing parameter.
     * @return the parsed {@link Complex} object.
     */
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Commons-Math code kept as such
    public Complex parse(final String source, final ParsePosition pos) {
        // CHECKSTYLE: resume ReturnCount check
        final int initialIndex = pos.getIndex();

        // parse whitespace
        CompositeFormat.parseAndIgnoreWhitespace(source, pos);

        // parse real
        final Number re = CompositeFormat.parseNumber(source, this.getRealFormat(), pos);
        if (re == null) {
            // invalid real number
            // set index back to initial, error index should already be set
            pos.setIndex(initialIndex);
            return null;
        }

        // parse sign
        final int startIndex = pos.getIndex();
        final char c = CompositeFormat.parseNextCharacter(source, pos);
        int sign = 0;
        switch (c) {
            case 0:
                // no sign
                // return real only complex number
                return new Complex(re.doubleValue(), 0.0);
            case '-':
                sign = -1;
                break;
            case '+':
                sign = 1;
                break;
            default:
                // invalid sign
                // set index back to initial, error index should be the last
                // character examined.
                pos.setIndex(initialIndex);
                pos.setErrorIndex(startIndex);
                return null;
        }

        // parse whitespace
        CompositeFormat.parseAndIgnoreWhitespace(source, pos);

        // parse imaginary
        final Number im = CompositeFormat.parseNumber(source, this.getRealFormat(), pos);
        if (im == null) {
            // invalid imaginary number
            // set index back to initial, error index should already be set
            pos.setIndex(initialIndex);
            return null;
        }

        // parse imaginary character
        if (!CompositeFormat.parseFixedstring(source, this.getImaginaryCharacter(), pos)) {
            return null;
        }

        return new Complex(re.doubleValue(), im.doubleValue() * sign);

    }
}
