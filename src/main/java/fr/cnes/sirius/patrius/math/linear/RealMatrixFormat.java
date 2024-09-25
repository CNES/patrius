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
 * VERSION:4.5:DM:DM-2300:27/05/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
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

//CHECKSTYLE: stop ModifiedControlVariable check
//CHECKSTYLE: stop CommentRatio check
//Reason: Commons-Math code kept as such

/**
 * Formats a {@code nxm} matrix in components list format
 * "{{a<sub>0</sub><sub>0</sub>,a<sub>0</sub><sub>1</sub>, ...,
 * a<sub>0</sub><sub>m-1</sub>},{a<sub>1</sub><sub>0</sub>,
 * a<sub>1</sub><sub>1</sub>, ..., a<sub>1</sub><sub>m-1</sub>},{...},{
 * a<sub>n-1</sub><sub>0</sub>, a<sub>n-1</sub><sub>1</sub>, ...,
 * a<sub>n-1</sub><sub>m-1</sub>}}".
 * <p>
 * The prefix and suffix "{" and "}", the row prefix and suffix "{" and "}", the row separator "," and the column
 * separator "," can be replaced by any user-defined strings. The number format for components can be configured.
 * </p>
 * 
 * <p>
 * White space is ignored at parse time, even if it is in the prefix, suffix or separator specifications. So even if the
 * default separator does include a space character that is used at format time, both input string "{{1,1,1}}" and
 * " { { 1 , 1 , 1 } } " will be parsed without error and the same matrix will be returned. In the second case, however,
 * the parse position after parsing will be just after the closing curly brace, i.e. just before the trailing space.
 * </p>
 * 
 * <p>
 * <b>Note:</b> the grouping functionality of the used {@link NumberFormat} is disabled to prevent problems when parsing
 * (e.g. 1,345.34 would be a valid number but conflicts with the default column separator).
 * </p>
 * 
 * @since 3.1
 * @version $Id: RealMatrixFormat.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class RealMatrixFormat {

    /** The default prefix: "{". */
    private static final String DEFAULT_PREFIX = "{";
    /** The default suffix: "}". */
    private static final String DEFAULT_SUFFIX = "}";
    /** The default row prefix: "{". */
    private static final String DEFAULT_ROW_PREFIX = DEFAULT_PREFIX;
    /** The default row suffix: "}". */
    private static final String DEFAULT_ROW_SUFFIX = DEFAULT_SUFFIX;
    /** The default row separator: ",". */
    private static final String DEFAULT_ROW_SEPARATOR = ",";
    /** The default column separator: ",". */
    private static final String DEFAULT_COLUMN_SEPARATOR = DEFAULT_ROW_SEPARATOR;
    /** The default summary separator: "...". */
    private static final String DEFAULT_SUMMARY_SEPARATOR = "...";
    /** Prefix. */
    private final String prefix;
    /** Suffix. */
    private final String suffix;
    /** Row prefix. */
    private final String rowPrefix;
    /** Row suffix. */
    private final String rowSuffix;
    /** Row separator. */
    private final String rowSeparator;
    /** Column separator. */
    private final String columnSeparator;
    /** The format used for components. */
    private final NumberFormat numberFormat;
    /** The string pattern used for components. */
    private final String patternFormat;
    /** Corner blocs size to display in summary view. */
    private final int summaryIndex;

    /**
     * Create an instance with default settings.
     * <p>
     * The instance uses the default prefix, suffix and row/column separator: "[", "]", ";" and ", " and the default
     * number format for components.
     * </p>
     */
    public RealMatrixFormat() {
        this(DEFAULT_PREFIX, DEFAULT_SUFFIX, DEFAULT_ROW_PREFIX, DEFAULT_ROW_SUFFIX,
                DEFAULT_ROW_SEPARATOR, DEFAULT_COLUMN_SEPARATOR, CompositeFormat.getDefaultNumberFormat());
    }

    /**
     * Create an instance with a custom number format for components.
     * 
     * @param formatIn
     *        the custom format for components.
     */
    public RealMatrixFormat(final NumberFormat formatIn) {
        this(DEFAULT_PREFIX, DEFAULT_SUFFIX, DEFAULT_ROW_PREFIX, DEFAULT_ROW_SUFFIX,
                DEFAULT_ROW_SEPARATOR, DEFAULT_COLUMN_SEPARATOR, formatIn);
    }

    /**
     * Create an instance with custom prefix, suffix and separator.
     * 
     * @param prefixIn
     *        prefix to use instead of the default "{"
     * @param suffixIn
     *        suffix to use instead of the default "}"
     * @param rowPrefixIn
     *        row prefix to use instead of the default "{"
     * @param rowSuffixIn
     *        row suffix to use instead of the default "}"
     * @param rowSeparatorIn
     *        tow separator to use instead of the default ";"
     * @param columnSeparatorIn
     *        column separator to use instead of the default ", "
     */
    public RealMatrixFormat(final String prefixIn, final String suffixIn,
                            final String rowPrefixIn, final String rowSuffixIn,
                            final String rowSeparatorIn, final String columnSeparatorIn) {
        this(prefixIn, suffixIn, rowPrefixIn, rowSuffixIn, rowSeparatorIn, columnSeparatorIn,
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
     * @param rowPrefixIn
     *        row prefix to use instead of the default "{"
     * @param rowSuffixIn
     *        row suffix to use instead of the default "}"
     * @param rowSeparatorIn
     *        tow separator to use instead of the default ";"
     * @param columnSeparatorIn
     *        column separator to use instead of the default ", "
     * @param formatIn
     *        the custom format for components.
     */
    public RealMatrixFormat(final String prefixIn, final String suffixIn,
                            final String rowPrefixIn, final String rowSuffixIn,
                            final String rowSeparatorIn, final String columnSeparatorIn,
                            final NumberFormat formatIn) {
        this.prefix = prefixIn;
        this.suffix = suffixIn;
        this.rowPrefix = rowPrefixIn;
        this.rowSuffix = rowSuffixIn;
        this.rowSeparator = rowSeparatorIn;
        this.columnSeparator = columnSeparatorIn;
        this.numberFormat = formatIn;
        // disable grouping to prevent parsing problems
        this.numberFormat.setGroupingUsed(false);
        this.patternFormat = "";
        this.summaryIndex = 0;
    }

    /**
     * Create an instance with custom prefix, suffix, separator and format
     * for components.
     * 
     * @param prefixIn
     *        prefix to use instead of the default "{"
     * @param suffixIn
     *        suffix to use instead of the default "}"
     * @param rowPrefixIn
     *        row prefix to use instead of the default "{"
     * @param rowSuffixIn
     *        row suffix to use instead of the default "}"
     * @param rowSeparatorIn
     *        tow separator to use instead of the default ";"
     * @param columnSeparatorIn
     *        column separator to use instead of the default ", "
     * @param patternFormat
     *        the custom format for components.
     */
    public RealMatrixFormat(final String prefixIn, final String suffixIn,
                            final String rowPrefixIn, final String rowSuffixIn,
                            final String rowSeparatorIn, final String columnSeparatorIn,
                            final String patternFormat) {
        this(prefixIn, suffixIn, rowPrefixIn, rowSuffixIn, rowSeparatorIn, columnSeparatorIn, patternFormat, 0);
    }

    /**
     * Create an instance with custom prefix, suffix, separator and format
     * for components.
     * 
     * @param prefixIn
     *        prefix to use instead of the default "{"
     * @param suffixIn
     *        suffix to use instead of the default "}"
     * @param rowPrefixIn
     *        row prefix to use instead of the default "{"
     * @param rowSuffixIn
     *        row suffix to use instead of the default "}"
     * @param rowSeparatorIn
     *        tow separator to use instead of the default ";"
     * @param columnSeparatorIn
     *        column separator to use instead of the default ", "
     * @param patternFormat
     *        the custom format for components.
     * @param summaryIndex
     *        sub-corners blocs square dimensions for summary view
     */
    @SuppressWarnings("PMD.NullAssignment")
    public RealMatrixFormat(final String prefixIn, final String suffixIn,
                            final String rowPrefixIn, final String rowSuffixIn,
                            final String rowSeparatorIn, final String columnSeparatorIn,
                            final String patternFormat, final int summaryIndex) {
        this.prefix = prefixIn;
        this.suffix = suffixIn;
        this.rowPrefix = rowPrefixIn;
        this.rowSuffix = rowSuffixIn;
        this.rowSeparator = rowSeparatorIn;
        this.columnSeparator = columnSeparatorIn;
        this.numberFormat = null;
        this.patternFormat = patternFormat;
        this.summaryIndex = summaryIndex;
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
     * Get the format prefix.
     * 
     * @return format prefix.
     */
    public String getRowPrefix() {
        return this.rowPrefix;
    }

    /**
     * Get the format suffix.
     * 
     * @return format suffix.
     */
    public String getRowSuffix() {
        return this.rowSuffix;
    }

    /**
     * Get the format separator between rows of the matrix.
     * 
     * @return format separator for rows.
     */
    public String getRowSeparator() {
        return this.rowSeparator;
    }

    /**
     * Get the format separator between components.
     * 
     * @return format separator between components.
     */
    public String getColumnSeparator() {
        return this.columnSeparator;
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
    public static RealMatrixFormat getInstance() {
        return getInstance(Locale.getDefault());
    }

    /**
     * Returns the default real vector format for the given locale.
     * 
     * @param locale
     *        the specific locale used by the format.
     * @return the real vector format specific to the given locale.
     */
    public static RealMatrixFormat getInstance(final Locale locale) {
        return new RealMatrixFormat(CompositeFormat.getDefaultNumberFormat(locale));
    }

    /**
     * This method calls {@link #format(RealMatrix,StringBuffer,FieldPosition)}.
     * 
     * @param m
     *        RealMatrix object to format.
     * @return a formatted matrix.
     */
    public String format(final RealMatrix m) {
        return this.format(m, new StringBuffer(), new FieldPosition(0)).toString();
    }

    /**
     * Formats a {@link RealMatrix} object to produce a string.
     * 
     * @param matrix
     *        the object to format.
     * @param toAppendTo
     *        where the text is to be appended
     * @param pos
     *        On input: an alignment field, if desired. On output: the
     *        offsets of the alignment field
     * @return the value passed in as toAppendTo.
     */
	// CHECKSTYLE: stop CyclomaticComplexity check
	// Reason: Commons-Math code kept as such
    public StringBuffer format(final RealMatrix matrix, final StringBuffer toAppendTo,
            final FieldPosition pos) {
    	// CHECKSTYLE: resume CyclomaticComplexity check

        pos.setBeginIndex(0);
        pos.setEndIndex(0);

        // format prefix
        toAppendTo.append(this.prefix);

        // format rows
        final int rows = matrix.getRowDimension();
        final int cols = matrix.getColumnDimension();
        final boolean summaryMode = (this.summaryIndex > 0 && (rows > 2 * this.summaryIndex
            || cols > 2 * this.summaryIndex));
        boolean rowSummaryDone = false;
        boolean colSummaryDone = false;

        for (int i = 0; i < rows; ++i) {

            // Row summary displaying
            if (summaryMode && (i > this.summaryIndex - 1 && i < (rows - this.summaryIndex))) {
                if (!rowSummaryDone) {
                    final int sizeRow = toAppendTo.indexOf("\n") - this.rowSuffix.length() - this.rowSeparator.length();
                    final String shortSummarySeparator = DEFAULT_SUMMARY_SEPARATOR.substring(0, 1);
                    final StringBuilder r = new StringBuilder();
                    for (int j = 0; j < sizeRow; j++) {
                        r.append(shortSummarySeparator);
                    }
                    toAppendTo.append(this.rowPrefix + r.toString() + this.rowSuffix + this.rowSeparator);
                    rowSummaryDone = true;
                }

            } else {
                // Row classic displaying
                toAppendTo.append(this.rowPrefix);

                for (int j = 0; j < cols; ++j) {

                    // Column summary displaying
                    if (summaryMode && (j > this.summaryIndex - 1 && j < (cols - this.summaryIndex))) {
                        if (!colSummaryDone) {
                            toAppendTo.append(this.columnSeparator + DEFAULT_SUMMARY_SEPARATOR);
                            colSummaryDone = true;
                        }

                    } else {
                        // Column classic displaying
                        if (j > 0) {
                            toAppendTo.append(this.columnSeparator);
                        }

                        if (this.numberFormat != null) {
                            CompositeFormat.formatDouble(matrix.getEntry(i, j), this.numberFormat, toAppendTo, pos);
                        } else {
                            toAppendTo.append(String.format(Locale.US, this.patternFormat, matrix.getEntry(i, j)));
                        }
                    }
                }

                colSummaryDone = false;
                toAppendTo.append(this.rowSuffix);
                if (i < rows - 1) {
                    toAppendTo.append(this.rowSeparator);
                }
            }
        }

        // format suffix
        toAppendTo.append(this.suffix);

        if (this.summaryIndex > 0) {
            toAppendTo.append("\nRows number : " + rows + "\t Columns number : " + cols);
        }

        return toAppendTo;
    }

    /**
     * Parse a string to produce a {@link RealMatrix} object.
     * 
     * @param source
     *        String to parse.
     * @return the parsed {@link RealMatrix} object.
     * @throws MathParseException
     *         if the beginning of the specified string
     *         cannot be parsed.
     */
    @SuppressWarnings("PMD.PrematureDeclaration")
    public RealMatrix parse(final String source) {
        final ParsePosition parsePosition = new ParsePosition(0);
        final RealMatrix result = this.parse(source, parsePosition);
        if (parsePosition.getIndex() == 0) {
            throw new MathParseException(source,
                parsePosition.getErrorIndex(),
                Array2DRowRealMatrix.class);
        }
        return result;
    }

    /**
     * Parse a string to produce a {@link RealMatrix} object.
     * 
     * @param source
     *        String to parse.
     * @param pos
     *        input/ouput parsing parameter.
     * @return the parsed {@link RealMatrix} object.
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Commons-Math code kept as such
    @SuppressWarnings("PMD.PrematureDeclaration")
    public RealMatrix parse(final String source, final ParsePosition pos) {
        // CHECKSTYLE: resume CyclomaticComplexity check
        // CHECKSTYLE: resume ReturnCount check

        final int initialIndex = pos.getIndex();

        final String trimmedPrefix = this.prefix.trim();

        // parse prefix
        CompositeFormat.parseAndIgnoreWhitespace(source, pos);
        if (!CompositeFormat.parseFixedstring(source, trimmedPrefix, pos)) {
            return null;
        }

        final String trimmedRowPrefix = this.rowPrefix.trim();
        final String trimmedRowSuffix = this.rowSuffix.trim();
        final String trimmedColumnSeparator = this.columnSeparator.trim();
        final String trimmedRowSeparator = this.rowSeparator.trim();

        // parse components
        final List<List<Number>> matrix = new ArrayList<>();
        List<Number> rowComponents = new ArrayList<>();
        for (boolean loop = true; loop;) {

            if (rowComponents.isEmpty()) {
                CompositeFormat.parseAndIgnoreWhitespace(source, pos);
                if (trimmedRowPrefix.length() != 0 &&
                        !CompositeFormat.parseFixedstring(source, trimmedRowPrefix, pos)) {
                    return null;
                }
            } else {
                CompositeFormat.parseAndIgnoreWhitespace(source, pos);
                if (!CompositeFormat.parseFixedstring(source, trimmedColumnSeparator, pos)) {
                    if (trimmedRowSuffix.length() != 0 &&
                            !CompositeFormat.parseFixedstring(source, trimmedRowSuffix, pos)) {
                        return null;
                    }
                    CompositeFormat.parseAndIgnoreWhitespace(source, pos);
                    if (CompositeFormat.parseFixedstring(source, trimmedRowSeparator, pos)) {
                        matrix.add(rowComponents);
                        rowComponents = new ArrayList<>();
                        continue;
                    }
                    loop = false;
                }
            }

            if (loop) {
                CompositeFormat.parseAndIgnoreWhitespace(source, pos);
                final Number component = CompositeFormat.parseNumber(source, this.numberFormat, pos);
                if (component == null) {
                    if (rowComponents.isEmpty()) {
                        loop = false;
                    } else {
                        // invalid component
                        // set index back to initial, error index should already be set
                        pos.setIndex(initialIndex);
                        return null;
                    }
                } else {
                    rowComponents.add(component);
                }
            }

        }

        if (!rowComponents.isEmpty()) {
            matrix.add(rowComponents);
        }

        final String trimmedSuffix = this.suffix.trim();

        // parse suffix
        CompositeFormat.parseAndIgnoreWhitespace(source, pos);
        if (!CompositeFormat.parseFixedstring(source, trimmedSuffix, pos)) {
            return null;
        }

        // do not allow an empty matrix
        if (matrix.isEmpty()) {
            pos.setIndex(initialIndex);
            return null;
        }

        // build vector
        final double[][] data = new double[matrix.size()][];
        int row = 0;
        for (final List<Number> rowList : matrix) {
            data[row] = new double[rowList.size()];
            for (int i = 0; i < rowList.size(); i++) {
                data[row][i] = rowList.get(i).doubleValue();
            }
            row++;
        }
        return MatrixUtils.createRealMatrix(data, false);
    }

    // CHECKSTYLE: resume CommentRatio check
    // CHECKSTYLE: resume ModifiedControlVariable check
}
