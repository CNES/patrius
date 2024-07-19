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
package fr.cnes.sirius.patrius.math.util;

import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Locale;

/**
 * Base class for formatters of composite objects (complex numbers, vectors ...).
 * 
 * @version $Id: CompositeFormat.java 18108 2017-10-04 06:45:27Z bignon $
 */
public final class CompositeFormat {

    /** 10 */
    private static final int C_10 = 10;

    /**
     * Class contains only static methods.
     */
    private CompositeFormat() {
    }

    /**
     * Create a default number format. The default number format is based on {@link NumberFormat#getInstance()} with the
     * only customizing that the
     * maximum number of fraction digits is set to 10.
     * 
     * @return the default number format.
     */
    public static NumberFormat getDefaultNumberFormat() {
        return getDefaultNumberFormat(Locale.getDefault());
    }

    /**
     * Create a default number format. The default number format is based on
     * {@link NumberFormat#getInstance(java.util.Locale)} with the only
     * customizing that the maximum number of fraction digits is set to 10.
     * 
     * @param locale
     *        the specific locale used by the format.
     * @return the default number format specific to the given locale.
     */
    public static NumberFormat getDefaultNumberFormat(final Locale locale) {
        final NumberFormat nf = NumberFormat.getInstance(locale);
        nf.setMaximumFractionDigits(C_10);
        return nf;
    }

    /**
     * Parses <code>source</code> until a non-whitespace character is found.
     * 
     * @param source
     *        the string to parse
     * @param pos
     *        input/output parsing parameter. On output, <code>pos</code> holds the index of the next non-whitespace
     *        character.
     */
    public static void parseAndIgnoreWhitespace(final String source,
                                                final ParsePosition pos) {
        parseNextCharacter(source, pos);
        pos.setIndex(pos.getIndex() - 1);
    }

    /**
     * Parses <code>source</code> until a non-whitespace character is found.
     * 
     * @param source
     *        the string to parse
     * @param pos
     *        input/output parsing parameter.
     * @return the first non-whitespace character.
     */
    public static char parseNextCharacter(final String source,
                                          final ParsePosition pos) {
        // get the index
        int index = pos.getIndex();
        // source length
        final int n = source.length();
        char ret = 0;

        // search the next non-whitespace character
        if (index < n) {
            char c;
            do {
                c = source.charAt(index++);
            } while (Character.isWhitespace(c) && index < n);
            pos.setIndex(index);

            if (index < n) {
                ret = c;
            }
        }

        return ret;
    }

    /**
     * Parses <code>source</code> for special double values. These values
     * include Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY.
     * 
     * @param source
     *        the string to parse
     * @param value
     *        the special value to parse.
     * @param pos
     *        input/output parsing parameter.
     * @return the special number.
     */
    private static Number parseNumber(final String source, final double value,
                                      final ParsePosition pos) {
        // Initialization
        Number ret = null;

        // Build string
        final StringBuilder sb = new StringBuilder();
        sb.append('(');
        sb.append(value);
        sb.append(')');

        final int n = sb.length();
        final int startIndex = pos.getIndex();
        final int endIndex = startIndex + n;
        if (endIndex < source.length()) {
            if (source.substring(startIndex, endIndex).compareTo(sb.toString()) == 0) {
                ret = Double.valueOf(value);
                pos.setIndex(endIndex);
            }
        }

        // Return result
        return ret;
    }

    /**
     * Parses <code>source</code> for a number. This method can parse normal,
     * numeric values as well as special values. These special values include
     * Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY.
     * 
     * @param source
     *        the string to parse
     * @param format
     *        the number format used to parse normal, numeric values.
     * @param pos
     *        input/output parsing parameter.
     * @return the parsed number.
     */
    public static Number parseNumber(final String source, final NumberFormat format,
                                     final ParsePosition pos) {
        // get the index
        final int startIndex = pos.getIndex();
        Number number = format.parse(source, pos);
        final int endIndex = pos.getIndex();

        // check for error parsing number
        if (startIndex == endIndex) {
            // try parsing special numbers
            final double[] special = { Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY };
            for (final double element : special) {
                number = parseNumber(source, element, pos);
                if (number != null) {
                    break;
                }
            }
        }

        return number;
    }

    /**
     * Parse <code>source</code> for an expected fixed string.
     * 
     * @param source
     *        the string to parse
     * @param expected
     *        expected string
     * @param pos
     *        input/output parsing parameter.
     * @return true if the expected string was there
     */
    public static boolean parseFixedstring(final String source,
                                           final String expected,
                                           final ParsePosition pos) {

        final int startIndex = pos.getIndex();
        final int endIndex = startIndex + expected.length();
        if ((startIndex >= source.length()) ||
                (endIndex > source.length()) ||
                (source.substring(startIndex, endIndex).compareTo(expected) != 0)) {
            // set index back to start, error index should be the start index
            pos.setIndex(startIndex);
            pos.setErrorIndex(startIndex);
            return false;
        }

        // the string was here
        pos.setIndex(endIndex);
        return true;
    }

    /**
     * Formats a double value to produce a string. In general, the value is
     * formatted using the formatting rules of <code>format</code>. There are
     * three exceptions to this:
     * <ol>
     * <li>NaN is formatted as '(NaN)'</li>
     * <li>Positive infinity is formatted as '(Infinity)'</li>
     * <li>Negative infinity is formatted as '(-Infinity)'</li>
     * </ol>
     * 
     * @param value
     *        the double to format.
     * @param format
     *        the format used.
     * @param toAppendTo
     *        where the text is to be appended
     * @param pos
     *        On input: an alignment field, if desired. On output: the
     *        offsets of the alignment field
     * @return the value passed in as toAppendTo.
     */
    public static StringBuffer formatDouble(final double value, final NumberFormat format,
                                            final StringBuffer toAppendTo,
                                            final FieldPosition pos) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            toAppendTo.append('(');
            toAppendTo.append(value);
            toAppendTo.append(')');
        } else {
            format.format(value, toAppendTo, pos);
        }
        return toAppendTo;
    }
}
