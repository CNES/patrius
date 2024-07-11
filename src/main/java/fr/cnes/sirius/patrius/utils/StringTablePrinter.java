/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.10:DM:DM-3223:03/11/2022:[PATRIUS] Frame implements PVCoordinatesProvider
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import fr.cnes.sirius.patrius.math.exception.NotPositiveException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;

/**
 * This utility class allows to print a table as a formatted String with dynamic columns widths.
 *
 * @author veuillh, bonitt, barthet
 */
public class StringTablePrinter {

    /** The default String representing the bold line separator. */
    public static final String DEFAULT_BOLD_LINE_SEPARATOR = "=";

    /** The default String representing the standard line separator. */
    public static final String DEFAULT_STANDARD_LINE_SEPARATOR = "-";

    /** The default String representing the vertical separator of the middle columns. */
    public static final String DEFAULT_VERTICAL_SEPARATOR = " | ";

    /** The default String representing the vertical separator of the left column. */
    public static final String DEFAULT_LEFT_VERTICAL_SEPARATOR = "| ";

    /** The default String representing the vertical separator of the right column. */
    public static final String DEFAULT_RIGHT_VERTICAL_SEPARATOR = " |";

    /** Bold line separator indicator. */
    private static final String[] BOLD_LINE = new String[0];

    /** Standard line separator indicator. */
    private static final String[] STANDARD_LINE = new String[0];

    /** Carrier return. */
    private static final String CARRIER_RETURN = "\n";

    /** End parenthesis. */
    private static final String END_PARENTHESIS = ")";

    /** End parenthesis. */
    private static final String PERCENT = "%";

    /** End parenthesis. */
    private static final String SECOND = "s";

    /** End parenthesis. */
    private static final String SPACE = " ";
            
    
    /** The locale used to print double. */
    private static Locale locale = Locale.US;

    /** Text alignment mode ({@link StringAlign#RIGHT RIGHT} by default). */
    private StringAlign stringAlign;

    /** The String representing the bold line separator. */
    private String boldLineSeparator;

    /** The String representing the standard line separator. */
    private String standardLineSeparator;

    /** The String representing the vertical separator of the middle columns. */
    private String verticalSeparator;

    /** The String representing the vertical separator of the left column. */
    private String verticalLeftSeparator;

    /** The String representing the vertical separator of the right column. */
    private String verticalRightSeparator;

    /** The centered table title (optional, {@code null} if not initialized). */
    private String title;

    /**
     * The structure table that will be printed. Each element of the list represents one row, the String table
     * represents the columns.
     */
    private final List<String[]> table;

    /** The number of columns in the table. */
    private final int nbColumns;

    /**
     * Constructor to initialize the header.
     * <p>
     * Note: the header dimension constrains the table dimension. The values added later must be compatible with this
     * dimension.
     * </p>
     *
     * @param header
     *        The header describing each column information
     */
    public StringTablePrinter(final String[] header) {
        this(null, header);
    }

    /**
     * Constructor to initialize the header and add a centered title to the table.
     * <p>
     * Note: the header dimension constrains the table dimension. The values added later must be compatible with this
     * dimension.
     * </p>
     *
     * @param title
     *        Centered table title ({@code null} if not initialized)
     * @param header
     *        The header describing each column information
     * @throws NullArgumentException if {@code header} is {@code null}
     * @throws IllegalArgumentException the header doesn't describe at least one column (size can't be 0)
     */
    public StringTablePrinter(final String title, final String[] header) {
        // Check the consistency of the header
        if (header == null) {
            throw new NullArgumentException();
        }
        if (header.length == 0) {
            throw new IllegalArgumentException("The header should describe at least one column (size can't be 0)");
        }

        this.table = new ArrayList<>();
        this.boldLineSeparator = DEFAULT_BOLD_LINE_SEPARATOR;
        this.standardLineSeparator = DEFAULT_STANDARD_LINE_SEPARATOR;
        this.verticalSeparator = DEFAULT_VERTICAL_SEPARATOR;
        this.verticalLeftSeparator = DEFAULT_LEFT_VERTICAL_SEPARATOR;
        this.verticalRightSeparator = DEFAULT_RIGHT_VERTICAL_SEPARATOR;
        this.stringAlign = StringAlign.RIGHT;
        this.title = title;
        this.nbColumns = header.length;
        this.table.add(header);
    }

    /**
     * Add a line to the table.
     * <p>
     * Note: the given array must have the same size as the header used in the constructor (see {@link #getNbColumns()}
     * ).
     * </p>
     *
     * @param line
     *        The array describing each column value of the new line
     * @throws IllegalArgumentException if the provided array does not have the same dimension than the header
     */
    public void addLine(final String[] line) {
        checkDimension(line.length);
        this.table.add(line.clone());
    }

    /**
     * Add the values as a line to the table.
     *
     * @param values
     *        The array describing each column value of the new line
     * @param fmt
     *        The format used to convert the double values into String
     * @throws IllegalArgumentException if the provided array does not have the same dimension than the header
     */
    public void addLine(final double[] values, final String fmt) {
        checkDimension(values.length);
        final String[] line = new String[this.nbColumns];
        for (int i = 0; i < line.length; i++) {
            line[i] = printDouble(values[i], fmt);
        }
        this.table.add(line);
    }

    /**
     * Add the values as a line to the table.<br>
     * The first column is described by a String.
     *
     * @param leftColumn
     *        The first column of the line represented by a String
     * @param values
     *        The other columns of the line represented by double values (must have the same dimension as the header
     *        minus one, as the left column is not described by this array)
     * @param fmt
     *        The format used to convert the double values into String
     * @throws IllegalArgumentException if the provided array dimension is not compatible with the header
     *         ({@code values.length + 1 =! getNbColumns()})
     */
    public void addLine(final String leftColumn, final double[] values, final String fmt) {
        checkDimension(1 + values.length); // The left column + the values size must be compatible with the header
        final String[] line = new String[this.nbColumns];
        line[0] = leftColumn;
        for (int i = 1; i < line.length; i++) {
            line[i] = printDouble(values[i - 1], fmt);
        }
        this.table.add(line);
    }

    /**
     * Check that the provided length is compatible with the header dimension.
     * 
     * @param length
     *        The provided length to check
     * @throws IllegalArgumentException if the provided dimension isn't compatible with the header
     */
    private void checkDimension(final int length) {
        if (this.getNbColumns() != length) {
            throw new IllegalArgumentException(
                "The provided array must have the same dimension than the header (" + length + " vs "
                        + this.getNbColumns() + END_PARENTHESIS);
        }
    }

    /**
     * Check if the title can fit in the table.
     * 
     * @param availableWidth
     *        Available width
     * @throws IllegalStateException
     *         if the title is to too long (wider than the table minus the left and right separator lengths)
     */
    private void checkTitle(final int availableWidth) {
        if (this.title.length() > availableWidth) {
            throw new IllegalStateException(
                "The title is to too long (" + this.title.length()
                        + ") which is wider than the table minus the left and right separator lengths ("
                        + availableWidth + END_PARENTHESIS);
        }
    }

    /**
     * Optionally, add a centered title (will be displayed before the header).
     * <p>
     * Note: the title's length must be shorter than the total table width minus the left and right columns separator
     * lengths.
     * </p>
     * 
     * @param title
     *        Centered table ({@code null} to disable)
     */
    public void setTitle(final String title) {
        this.title = title;
    }

    /**
     * Add a line full of bold separators.
     */
    public void addBoldLineSeparator() {
        this.table.add(BOLD_LINE);
    }

    /**
     * Change the last table line into a line full of bold separators.
     * <p>
     * This method can be useful when the table is built with several standard lines separator depending on conditions
     * and we still want to use a bold line at the end of the table.
     * </p>
     */
    public void switchLastLineBold() {
        this.table.set(this.table.size() - 1, BOLD_LINE);
    }

    /**
     * Add a line full of standard separators.
     */
    public void addStandardLineSeparator() {
        this.table.add(STANDARD_LINE);
    }

    /**
     * Clear the title and the table (the header remains).
     */
    @SuppressWarnings("PMD.NullAssignment")
    // Reason: we want explicitely the title to be not provided (= undefined)
    public void clear() {
        this.title = null;
        final String[] header = this.table.get(0).clone();
        this.table.clear();
        this.table.add(header);
    }

    /**
     * Return a string representation of the formatted table.
     *
     * @return a string representation of the formatted table
     */
    @Override
    public String toString() {
        return toString(0);
    }

    /**
     * Return a string representation of the formatted table.
     *
     * @param indentation
     *        Indent the array of N spaces
     * @return a string representation of the formatted table
     * @throws NotPositiveException if {@code indentation < 0}
     */
    public String toString(final int indentation) {
        if (indentation < 0) {
            throw new NotPositiveException(indentation);
        }

        // Compute the maximal length of each column of the table
        final int[] columnsMaxLength = buildColumnsMaxLength();

        // Compute the total table width
        int tableWidth = 0;
        for (int i = 0; i < columnsMaxLength.length; i++) {
            tableWidth += columnsMaxLength[i];
        }
        tableWidth += (columnsMaxLength.length - 1) * this.verticalSeparator.length()
                + this.verticalLeftSeparator.length() + this.verticalRightSeparator.length();
        // To take into account the vertical columns separators

        final String boldLine = printBoldLineSeparator(tableWidth);
        final String standardLine = printStandardLineSeparator(tableWidth);

        // Initialize the string builder
        // Initial capacity:
        // (tableWidth + indentation + return carrier) * (titleNbLines + nbLines + 2 header separators)
        final int titleNbLines = this.title == null ? 0 : 2;
        final int initialCapacity = (tableWidth + indentation + 1) * (titleNbLines + this.table.size() + 2);
        final StringBuilder stringBuilder = new StringBuilder(initialCapacity);

        // If the centered titled is initialized, display it
        if (this.title != null) {
            appendIndentation(stringBuilder, indentation);
            stringBuilder.append(boldLine);
            stringBuilder.append(CARRIER_RETURN);

            // Build title
            final int availableWidth =
                tableWidth - this.verticalLeftSeparator.length() - this.verticalRightSeparator.length();
            this.checkTitle(availableWidth);

            appendIndentation(stringBuilder, indentation);
            stringBuilder.append(this.verticalLeftSeparator);
            stringBuilder.append(StringAlign.CENTER.pad(this.title, availableWidth));
            stringBuilder.append(this.verticalRightSeparator);
            stringBuilder.append(CARRIER_RETURN);
        }

        appendIndentation(stringBuilder, indentation);
        stringBuilder.append(boldLine);
        stringBuilder.append(CARRIER_RETURN);

        // Go through the table
        for (int i = 0; i < this.table.size(); i++) {
            // For each line
            final String[] line = this.table.get(i);

            if (i == 1) {
                // End of the header
                appendIndentation(stringBuilder, indentation);
                stringBuilder.append(boldLine);
                stringBuilder.append(CARRIER_RETURN);
            }

            appendIndentation(stringBuilder, indentation);

            if (line == BOLD_LINE) {
                stringBuilder.append(boldLine);
            } else if (line == STANDARD_LINE) {
                stringBuilder.append(standardLine);
            } else {
                stringBuilder.append(this.verticalLeftSeparator);
                for (int j = 0; j < line.length; j++) {
                    // For each column
                    final String column = line[j];

                    final int columnMaxLength = columnsMaxLength[j];
                    stringBuilder.append(this.stringAlign.pad(column, columnMaxLength));

                    if (j < line.length - 1) {
                        stringBuilder.append(this.verticalSeparator);
                    } else {
                        stringBuilder.append(this.verticalRightSeparator); // Last column separator on the right
                    }
                }
            }
            stringBuilder.append(CARRIER_RETURN);
        }
        return stringBuilder.toString();
    }

    /**
     * Getter for the text alignment mode.
     *
     * @return the text alignment mode
     */
    public StringAlign getStringAlign() {
        return this.stringAlign;
    }

    /**
     * Setter for the text alignment mode ({@link StringAlign#RIGHT RIGHT} by default).
     *
     * @param stringAlign
     *        Text alignment mode to set
     */
    public void setStringAlign(final StringAlign stringAlign) {
        this.stringAlign = stringAlign;
    }

    /**
     * Getter for the String representing the bold line separator.
     *
     * @return the String representing the bold line separator
     */
    public String getBoldLineSeparator() {
        return this.boldLineSeparator;
    }

    /**
     * Setter for the String representing the bold line separator.
     *
     * @param boldLineSeparator
     *        String representing the bold line separator to set
     */
    public void setBoldLineSeparator(final String boldLineSeparator) {
        this.boldLineSeparator = boldLineSeparator;
    }

    /**
     * Getter for the String representing the standard line separator.
     *
     * @return the String representing the standard line separator
     */
    public String getStandardLineSeparator() {
        return this.standardLineSeparator;
    }

    /**
     * Setter for the String representing the standard line separator.
     *
     * @param standardLineSeparator
     *        String representing the standard line separator to set
     */
    public void setStandardLineSeparator(final String standardLineSeparator) {
        this.standardLineSeparator = standardLineSeparator;
    }

    /**
     * Getter for the String representing the vertical separator of the middle columns.
     *
     * @return the String representing the vertical separator of the middle columns
     */
    public String getVerticalSeparator() {
        return this.verticalSeparator;
    }

    /**
     * Setter for the String representing the vertical separator of the middle columns.
     *
     * @param verticalSeparator
     *        String representing the vertical separator of the middle columns to set
     */
    public void setVerticalSeparator(final String verticalSeparator) {
        this.verticalSeparator = verticalSeparator;
    }

    /**
     * Getter for the String representing the vertical separator of the left column.
     *
     * @return the String representing the vertical separator of the left column
     */
    public String getVerticalLeftSeparator() {
        return this.verticalLeftSeparator;
    }

    /**
     * Setter for the String representing the vertical separator of the left column.
     *
     * @param verticalLeftSeparator
     *        String representing the vertical separator of the left column to set
     */
    public void setVerticalLeftSeparator(final String verticalLeftSeparator) {
        this.verticalLeftSeparator = verticalLeftSeparator;
    }

    /**
     * Getter for the String representing the vertical separator of the right column.
     *
     * @return the String representing the vertical separator of the right column
     */
    public String getVerticalRightSeparator() {
        return this.verticalRightSeparator;
    }

    /**
     * Setter for the String representing the vertical separator of the right column.
     *
     * @param verticalRightSeparator
     *        String representing the vertical separator of the right column to set
     */
    public void setVerticalRightSeparator(final String verticalRightSeparator) {
        this.verticalRightSeparator = verticalRightSeparator;
    }

    /**
     * Getter for the number of columns in the table.
     *
     * @return the number of columns in the table
     */
    public int getNbColumns() {
        return this.nbColumns;
    }

    /**
     * Build the maximal length of each column of the table.<br>
     * The returned array describes the dynamic width/length each column should have so the elements of all the lines
     * can fit inside.
     * 
     * @return the array describing the maximal length of each column of the table
     */
    private int[] buildColumnsMaxLength() {
        final int[] columnsMaxLength = new int[this.nbColumns];
        // Loop on each line of the table
        for (final String[] line : this.table) {
            if (line != BOLD_LINE && line != STANDARD_LINE) {
                // Loop on each column of the current line and update the maximum length array
                for (int i = 0; i < this.nbColumns; i++) {
                    final int lineLength = line[i].length();
                    if (lineLength > columnsMaxLength[i]) {
                        columnsMaxLength[i] = lineLength;
                    }
                }
            }
        }
        return columnsMaxLength;
    }

    /**
     * Print a line full of bold separators.
     * 
     * @param tableWidth
     *        Total table width
     * @return the line full of bold separators
     */
    private String printBoldLineSeparator(final int tableWidth) {
        return String.format(PERCENT + tableWidth + SECOND, ' ').replace(SPACE, this.boldLineSeparator)
                .substring(0, tableWidth);
    }

    /**
     * Print a line full of standard separators.
     * 
     * @param tableWidth
     *        Total table width
     * @return the line full of standard separators
     */
    private String printStandardLineSeparator(final int tableWidth) {
        return String.format(PERCENT + tableWidth + SECOND, ' ').replace(SPACE, this.standardLineSeparator)
                .substring(0, tableWidth);
    }

    /**
     * Print a double as a String in the provided format.
     *
     * @param value
     *        Value to print
     * @param fmt
     *        The format used to convert the double values into String
     * @return the printed value
     */
    public static String printDouble(final double value, final String fmt) {
        return String.format(StringTablePrinter.locale, fmt, value);
    }

    /**
     * Add indentation in the builder.
     * 
     * @param builder
     *        Builder to update
     * @param indentation
     *        Indent the builder of N spaces
     */
    private static void appendIndentation(final StringBuilder builder, final int indentation) {
        if (indentation > 0) {
            builder.append(String.format(PERCENT + indentation + "c", ' '));
        }
    }

    /** Text alignment enumerate. */
    // TODO en faire un classe à part ?
    public enum StringAlign {

        /** Align the text on the left. */
        LEFT {
            /** {@inheritDoc} */
            @Override
            protected String internalPadding(final String value, final int valueLength, final int width) {
                // Add spaces to the right
                return String.format("%-" + width + SECOND, value);
            }
        },

        /** Align the text on the right. */
        RIGHT {
            /** {@inheritDoc} */
            @Override
            public String internalPadding(final String value, final int valueLength, final int width) {
                // Add spaces to the left
                return String.format(PERCENT + width + SECOND, value);
            }
        },

        /** Align the text at the center. */
        CENTER {
            /** {@inheritDoc} */
            @Override
            public String internalPadding(final String value, final int valueLength, final int width) {
                // Add spaces both right and left
                final int nbSpaces = width - valueLength;
                final int leftSpaces = (nbSpaces % 2 == 0) ? nbSpaces / 2 : nbSpaces / 2 + 1;

                final char[] newCharArray = new char[width];
                // Initially, fill the array with spaces
                for (int i = 0; i < width; i++) {
                    newCharArray[i] = ' ';
                }
                // Copy content of value in newCharArray with leftSpaces offset
                value.getChars(0, valueLength, newCharArray, leftSpaces);
                return new String(newCharArray);
            }
        };

        /**
         * Returns the provided String with spaces to match the expected width.
         * 
         * @param value
         *        The String to pad
         * @param width
         *        The final length of the String to reach
         * @return the String with expected length of characters
         * @throws IllegalStateException if the provided String exceeds the provided target width
         */
        public String pad(final String value, final int width) {

            final int valueLength = value.length();
            final String out;
            if (valueLength > width) {
                throw new IllegalStateException("The provided String exceeds the provided target width.");
            } else if (valueLength == width) {
                out = value; // No need to pad
            } else {
                out = internalPadding(value, valueLength, width);
            }
            return out;
        }

        /**
         * Returns the provided String with spaces to match the expected width (internal method without verification).
         * 
         * @param value
         *        The String to pad
         * @param valueLength
         *        The String length
         * @param width
         *        The final length of the String to reach
         * @return the String with expected length of characters
         */
        protected abstract String internalPadding(String value, int valueLength, int width);
    }
}
