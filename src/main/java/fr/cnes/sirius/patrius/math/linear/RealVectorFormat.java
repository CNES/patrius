/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * HISTORY
 * VERSION:4.5:DM:DM-2300:27/05/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear 
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
package fr.cnes.sirius.patrius.math.linear;

import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import fr.cnes.sirius.patrius.math.exception.MathParseException;
import fr.cnes.sirius.patrius.math.util.CompositeFormat;

/**
 * Formats a vector in components list format "{v0; v1; ...; vk-1}".
 * <p>
 * The prefix and suffix "{" and "}" and the separator "; " can be replaced by any user-defined strings. The number
 * format for components can be configured.
 * </p>
 * <p>
 * White space is ignored at parse time, even if it is in the prefix, suffix or separator specifications. So even if the
 * default separator does include a space character that is used at format time, both input string "{1;1;1}" and
 * " { 1 ; 1 ; 1 } " will be parsed without error and the same vector will be returned. In the second case, however, the
 * parse position after parsing will be just after the closing curly brace, i.e. just before the trailing space.
 * </p>
 * 
 * @version $Id: RealVectorFormat.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 2.0
 */
public class RealVectorFormat {

    /** The default prefix: "{". */
    private static final String DEFAULT_PREFIX = "{";
    /** The default suffix: "}". */
    private static final String DEFAULT_SUFFIX = "}";
    /** The default separator: ", ". */
    private static final String DEFAULT_SEPARATOR = "; ";
    /** Prefix. */
    private final String prefix;
    /** Suffix. */
    private final String suffix;
    /** Separator. */
    private final String separator;
    /** Trimmed prefix. */
    private final String trimmedPrefix;
    /** Trimmed suffix. */
    private final String trimmedSuffix;
    /** Trimmed separator. */
    private final String trimmedSeparator;
    /** The format used for components. */
    private final NumberFormat numberFormat;

    /**
     * Create an instance with default settings.
     * <p>
     * The instance uses the default prefix, suffix and separator: "{", "}", and "; " and the default number format for
     * components.
     * </p>
     */
    public RealVectorFormat() {
        this(DEFAULT_PREFIX, DEFAULT_SUFFIX, DEFAULT_SEPARATOR,
            CompositeFormat.getDefaultNumberFormat());
    }

    /**
     * Create an instance with a custom number format for components.
     * 
     * @param formatIn
     *        the custom format for components.
     */
    public RealVectorFormat(final NumberFormat formatIn) {
        this(DEFAULT_PREFIX, DEFAULT_SUFFIX, DEFAULT_SEPARATOR, formatIn);
    }

    /**
     * Create an instance with custom prefix, suffix and separator.
     * 
     * @param prefixIn
     *        prefix to use instead of the default "{"
     * @param suffixIn
     *        suffix to use instead of the default "}"
     * @param separatorIn
     *        separator to use instead of the default "; "
     */
    public RealVectorFormat(final String prefixIn, final String suffixIn,
        final String separatorIn) {
        this(prefixIn, suffixIn, separatorIn,
            CompositeFormat.getDefaultNumberFormat());
    }

    /**
     * Create an instance with custom prefix, suffix, separator and format
     * for components.
     * 
     * @param prefixIn
     *        prefix to use instead of the default "{"
     * @param suffixIn
     *        suffix to use instead of the default "}"
     * @param separatorIn
     *        separator to use instead of the default "; "
     * @param formatIn
     *        the custom format for components.
     */
    public RealVectorFormat(final String prefixIn, final String suffixIn,
        final String separatorIn, final NumberFormat formatIn) {
        this.prefix = prefixIn;
        this.suffix = suffixIn;
        this.separator = separatorIn;
        this.trimmedPrefix = prefixIn.trim();
        this.trimmedSuffix = suffixIn.trim();
        this.trimmedSeparator = separatorIn.trim();
        this.numberFormat = formatIn;
    }

    /**
     * Get the set of locales for which real vectors formats are available.
     * <p>
     * This is the same set as the {@link NumberFormat} set.
     * </p>
     * 
     * @return available real vector format locales.
     */
    public static Locale[] getAvailableLocales() {
        return NumberFormat.getAvailableLocales();
    }

    /**
     * Get the format prefix.
     * 
     * @return format prefix.
     */
    public String getPrefix() {
        return this.prefix;
    }

    /**
     * Get the format suffix.
     * 
     * @return format suffix.
     */
    public String getSuffix() {
        return this.suffix;
    }

    /**
     * Get the format separator between components.
     * 
     * @return format separator.
     */
    public String getSeparator() {
        return this.separator;
    }

    /**
     * Get the components format.
     * 
     * @return components format.
     */
    public NumberFormat getFormat() {
        return this.numberFormat;
    }

    /**
     * Returns the default real vector format for the current locale.
     * 
     * @return the default real vector format.
     */
    public static RealVectorFormat getInstance() {
        return getInstance(Locale.getDefault());
    }

    /**
     * Returns the default real vector format for the given locale.
     * 
     * @param locale
     *        the specific locale used by the format.
     * @return the real vector format specific to the given locale.
     */
    public static RealVectorFormat getInstance(final Locale locale) {
        return new RealVectorFormat(CompositeFormat.getDefaultNumberFormat(locale));
    }

    /**
     * This method calls {@link #format(RealVector,StringBuffer,FieldPosition)}.
     * 
     * @param v
     *        RealVector object to format.
     * @return a formatted vector.
     */
    public String format(final RealVector v) {
        return this.format(v, new StringBuffer(), new FieldPosition(0)).toString();
    }

    /**
     * Formats a {@link RealVector} object to produce a string.
     * 
     * @param vector
     *        the object to format.
     * @param toAppendTo
     *        where the text is to be appended
     * @param pos
     *        On input: an alignment field, if desired. On output: the
     *        offsets of the alignment field
     * @return the value passed in as toAppendTo.
     */
    public StringBuffer format(final RealVector vector, final StringBuffer toAppendTo,
                               final FieldPosition pos) {

        pos.setBeginIndex(0);
        pos.setEndIndex(0);

        // format prefix
        toAppendTo.append(this.prefix);

        // format components
        for (int i = 0; i < vector.getDimension(); ++i) {
            if (i > 0) {
                toAppendTo.append(this.separator);
            }
            CompositeFormat.formatDouble(vector.getEntry(i), this.numberFormat, toAppendTo, pos);
        }

        // format suffix
        toAppendTo.append(this.suffix);

        return toAppendTo;
    }

    /**
     * Parse a string to produce a {@link RealVector} object.
     * 
     * @param source
     *        String to parse.
     * @return the parsed {@link RealVector} object.
     * @throws MathParseException
     *         if the beginning of the specified string
     *         cannot be parsed.
     */
    @SuppressWarnings("PMD.PrematureDeclaration")
    public ArrayRealVector parse(final String source) {
        final ParsePosition parsePosition = new ParsePosition(0);
        final ArrayRealVector result = this.parse(source, parsePosition);
        if (parsePosition.getIndex() == 0) {
            throw new MathParseException(source, parsePosition.getErrorIndex(), ArrayRealVector.class);
        }
        return result;
    }

    /**
     * Parse a string to produce a {@link RealVector} object.
     * 
     * @param source
     *        String to parse.
     * @param pos
     *        input/ouput parsing parameter.
     * @return the parsed {@link RealVector} object.
     */
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Commons-Math code kept as such
    @SuppressWarnings("PMD.PrematureDeclaration")
    public ArrayRealVector parse(final String source, final ParsePosition pos) {
        // CHECKSTYLE: resume ReturnCount check
        final int initialIndex = pos.getIndex();

        // parse prefix
        CompositeFormat.parseAndIgnoreWhitespace(source, pos);
        if (!CompositeFormat.parseFixedstring(source, this.trimmedPrefix, pos)) {
            return null;
        }

        // parse components
        final List<Number> components = new ArrayList<Number>();
        for (boolean loop = true; loop;) {

            if (!components.isEmpty()) {
                CompositeFormat.parseAndIgnoreWhitespace(source, pos);
                if (!CompositeFormat.parseFixedstring(source, this.trimmedSeparator, pos)) {
                    // CHECKSTYLE: stop ModifiedControlVariable check
                    // Reason: Commons-Math code kept as such
                    loop = false;
                    // CHECKSTYLE: resume ModifiedControlVariable check
                }
            }

            if (loop) {
                CompositeFormat.parseAndIgnoreWhitespace(source, pos);
                final Number component = CompositeFormat.parseNumber(source, this.numberFormat, pos);
                if (component == null) {
                    // invalid component
                    // set index back to initial, error index should already be set
                    pos.setIndex(initialIndex);
                    return null;
                } else {
                    components.add(component);
                }
            }

        }

        // parse suffix
        CompositeFormat.parseAndIgnoreWhitespace(source, pos);
        if (!CompositeFormat.parseFixedstring(source, this.trimmedSuffix, pos)) {
            return null;
        }

        // build vector
        final double[] data = new double[components.size()];
        for (int i = 0; i < data.length; ++i) {
            data[i] = components.get(i).doubleValue();
        }
        return new ArrayRealVector(data, false);
    }
}
