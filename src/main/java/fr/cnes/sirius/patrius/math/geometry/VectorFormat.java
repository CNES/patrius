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
package fr.cnes.sirius.patrius.math.geometry;

import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Locale;

import fr.cnes.sirius.patrius.math.exception.MathParseException;
import fr.cnes.sirius.patrius.math.util.CompositeFormat;

//CHECKSTYLE: stop IllegalType check
//Reason: Commons-Math code kept as such

/**
 * Formats a vector in components list format "{x; y; ...}".
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
 * @param <S>
 *        Type of the space.
 * @version $Id: VectorFormat.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.0
 */
//CHECKSTYLE: stop AbstractClassName check
@SuppressWarnings({ "PMD.AbstractNaming", "PMD.LooseCoupling" })
public abstract class VectorFormat<S extends Space> {
    //CHECKSTYLE: resume AbstractClassName check

    /** The default prefix: "{". */
    public static final String DEFAULT_PREFIX = "{";

    /** The default suffix: "}". */
    public static final String DEFAULT_SUFFIX = "}";

    /** The default separator: ", ". */
    public static final String DEFAULT_SEPARATOR = "; ";

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
    protected VectorFormat() {
        this(DEFAULT_PREFIX, DEFAULT_SUFFIX, DEFAULT_SEPARATOR,
            CompositeFormat.getDefaultNumberFormat());
    }

    /**
     * Create an instance with a custom number format for components.
     * 
     * @param formatIn
     *        the custom format for components.
     */
    protected VectorFormat(final NumberFormat formatIn) {
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
    protected VectorFormat(final String prefixIn, final String suffixIn,
        final String separatorIn) {
        this(prefixIn, suffixIn, separatorIn, CompositeFormat.getDefaultNumberFormat());
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
    protected VectorFormat(final String prefixIn, final String suffixIn,
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
     * Get the set of locales for which point/vector formats are available.
     * <p>
     * This is the same set as the {@link NumberFormat} set.
     * </p>
     * 
     * @return available point/vector format locales.
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
     * Formats a {@link Vector} object to produce a string.
     * 
     * @param vector
     *        the object to format.
     * @return a formatted string.
     */
    public String format(final Vector<S> vector) {
        return this.format(vector, new StringBuffer(), new FieldPosition(0)).toString();
    }

    /**
     * Formats a {@link Vector} object to produce a string.
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
    public abstract StringBuffer format(Vector<S> vector,
                                        StringBuffer toAppendTo, FieldPosition pos);

    /**
     * Formats the coordinates of a {@link Vector} to produce a string.
     * 
     * @param toAppendTo
     *        where the text is to be appended
     * @param pos
     *        On input: an alignment field, if desired. On output: the
     *        offsets of the alignment field
     * @param coordinates
     *        coordinates of the object to format.
     * @return the value passed in as toAppendTo.
     */
    protected StringBuffer format(final StringBuffer toAppendTo, final FieldPosition pos,
                                  final double... coordinates) {

        pos.setBeginIndex(0);
        pos.setEndIndex(0);

        // format prefix
        toAppendTo.append(this.prefix);

        // format components
        for (int i = 0; i < coordinates.length; ++i) {
            if (i > 0) {
                toAppendTo.append(this.separator);
            }
            CompositeFormat.formatDouble(coordinates[i], this.numberFormat, toAppendTo, pos);
        }

        // format suffix
        toAppendTo.append(this.suffix);

        return toAppendTo;

    }

    /**
     * Parses a string to produce a {@link Vector} object.
     * 
     * @param source
     *        the string to parse
     * @return the parsed {@link Vector} object.
     * @throws MathParseException
     *         if the beginning of the specified string
     *         cannot be parsed.
     */
    public abstract Vector<S> parse(String source);

    /**
     * Parses a string to produce a {@link Vector} object.
     * 
     * @param source
     *        the string to parse
     * @param pos
     *        input/output parsing parameter.
     * @return the parsed {@link Vector} object.
     */
    public abstract Vector<S> parse(String source, ParsePosition pos);

    /**
     * Parses a string to produce an array of coordinates.
     * 
     * @param dimension
     *        dimension of the space
     * @param source
     *        the string to parse
     * @param pos
     *        input/output parsing parameter.
     * @return coordinates array.
     */
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Commons-Math code kept as such
    @SuppressWarnings("PMD.PrematureDeclaration")
    protected double[] parseCoordinates(final int dimension, final String source, final ParsePosition pos) {
        // CHECKSTYLE: resume ReturnCount check

        final int initialIndex = pos.getIndex();

        // parse prefix
        CompositeFormat.parseAndIgnoreWhitespace(source, pos);
        if (!CompositeFormat.parseFixedstring(source, this.trimmedPrefix, pos)) {
            return null;
        }

        final double[] coordinates = new double[dimension];

        for (int i = 0; i < dimension; ++i) {

            // skip whitespace
            CompositeFormat.parseAndIgnoreWhitespace(source, pos);

            // parse separator
            if (i > 0) {
                if (!CompositeFormat.parseFixedstring(source, this.trimmedSeparator, pos)) {
                    return null;
                }
            }

            // skip whitespace
            CompositeFormat.parseAndIgnoreWhitespace(source, pos);

            // parse coordinate
            final Number c = CompositeFormat.parseNumber(source, this.numberFormat, pos);
            if (c == null) {
                // invalid coordinate
                // set index back to initial, error index should already be set
                pos.setIndex(initialIndex);
                return null;
            }

            // store coordinate
            coordinates[i] = c.doubleValue();

        }

        // parse suffix
        CompositeFormat.parseAndIgnoreWhitespace(source, pos);
        if (!CompositeFormat.parseFixedstring(source, this.trimmedSuffix, pos)) {
            return null;
        }

        // Return result
        return coordinates;
    }

    // CHECKSTYLE: resume IllegalType check
}
