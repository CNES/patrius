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
 */
/* 
 * HISTORY
* VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
* VERSION:4.8:FA:FA-2940:15/11/2021:[PATRIUS] Anomalies suite a DM 2766 sur package fr.cnes.sirius.patrius.math.linear 
 * VERSION:4.7:DM:DM-2766:18/05/2021:Evol. et corr. dans le package fr.cnes.sirius.patrius.math.linear (suite DM 2300) 
 * VERSION:4.7:DM:DM-2818:18/05/2021:[PATRIUS|COLOSUS] Classe GatesModel
 * VERSION:4.6:FA:FA-2542:27/01/2021:[PATRIUS] Definition d'un champ de vue avec demi-angle de 180° 
 * VERSION:4.5.1:FA:FA-2540:04/08/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear
 * END-HISTORY
 **/
package fr.cnes.sirius.patrius.math.linear;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.IntStream;

import org.junit.Assert;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.NoDataException;
import fr.cnes.sirius.patrius.math.exception.NotPositiveException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;
import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.linear.ArrayRowSymmetricMatrix.SymmetryType;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;

/**
 * Utilities for miscellaneous checks in JUnit tests.
 *
 * @author Pierre Seimandi (GMV)
 */
public final class CheckUtils {

    /** Default base to use when generating random numbers. */
    private static final double DEFAULT_BASE = 10.;

    /**
     * Expected message format for exceptions thrown when a row index is outside the valid index
     * range.
     */
    private static final String OUT_OF_RANGE_ROW_INDEX_FORMAT = "row index (%d)";

    /**
     * Expected message format for exceptions thrown when a column index is outside the valid index
     * range.
     */
    private static final String OUT_OF_RANGE_COLUMN_INDEX_FORMAT = "column index (%d)";

    /** Expected message format for exceptions thrown when a dimension mismatch is detected. */
    private static final String DIMENSION_MISMATCH_FORMAT = "%d != %d";

    /** Expected message format for exceptions thrown when a dimension mismatch is detected. */
    private static final String MATRIX_DIMENSION_MISMATCH_FORMAT = "got %dx%d but expected %dx%d";

    /** Expected message format for exceptions thrown when a non-square matrix is detected. */
    private static final String NON_SQUARE_MATRIX_FORMAT = "non square (%dx%d) matrix";

    /** Expected message format for exceptions thrown when null argument is detected. */
    private static final String NULL_ARGUMENT_FORMAT = "null is not allowed";

    /** Expected message format for exceptions thrown when an input array is null. */
    private static final String NULL_ARRAY_FORMAT = "the supplied array is null";

    /** Expected message format for exceptions thrown when an input row index array is empty. */
    private static final String EMPTY_ROW_INDEX_FORMAT = "empty selected row index array";

    /** Expected message format for exceptions thrown when an input column index array is empty. */
    private static final String EMPTY_COLUMN_INDEX_FORMAT = "empty selected column index array";

    /** Expected message format for exceptions thrown when a matrix data array has no row. */
    private static final String MATRIX_EMPTY_ROW_FORMAT = "matrix must have at least one row";

    /** Expected message format for exceptions thrown when a matrix data array has no column. */
    private static final String MATRIX_EMPTY_COLUMN_FORMAT = "matrix must have at least one column";

    /** Expected message format for exceptions thrown when a singular matrix is detected. */
    private static final String SINGULAR_MATRIX_FORMAT = "matrix is singular";

    /** Whitespace character. */
    private static final String WHITESPACE = " ";

    /** Comma character. */
    private static final String COMMA = ",";

    /** Line separator. */
    private static final String LINE_SEPARATOR = System.lineSeparator();

    /** Opening curly bracket string. */
    private static final String OPENING_CURLY_BRACKET = "{";

    /** Closing curly bracket string. */
    private static final String CLOSING_CURLY_BRACKET = "}";

    /** Row separator string. */
    private static final String ROW_SEPARATOR = COMMA + WHITESPACE;

    /** Column separator string. */
    private static final String COLUMN_SEPARATOR = COMMA + WHITESPACE;

    /** Default scientific number format. */
    public static final String SCIENTIFIC_NUMBER_FORMAT = "%+.15E";

    /** Default decimal format. */
    public static final DecimalFormat DEFAULT_DECIMAL_FORMAT;

    /** Truncated decimal format. */
    public static final DecimalFormat TRUNCATED_DECIMAL_FORMAT;

    static {
        DecimalFormat df;

        // Default decimal format
        df = new DecimalFormat();
        df.setGroupingUsed(false);
        df.setMaximumFractionDigits(50);
        df.setMinimumFractionDigits(0);
        DEFAULT_DECIMAL_FORMAT = df;

        // Truncated decimal format
        df = new DecimalFormat();
        df.setGroupingUsed(false);
        df.setMaximumFractionDigits(15);
        df.setMinimumFractionDigits(0);
        TRUNCATED_DECIMAL_FORMAT = df;
    }

    /**
     * Utility class.<br>
     * This private constructor avoid the creation of new instances.
     */
    private CheckUtils() {
    }

    /**
     * Serializes an object to a bytes array, then recovers the object from the bytes array and
     * returns it.
     *
     * @param <T>
     *        the type of the object
     * @param object
     *        the object to be serialized/deserialized
     * @param objectClass
     *        the class of the object to be serialized/deserialized
     *
     * @return the deserialized object
     */
    public static <T> T serializeAndRecover(final T object, final Class<T> objectClass) {
        try {
            // Serialize the object
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            final ObjectOutputStream outputStream = new ObjectOutputStream(bos);
            outputStream.writeObject(object);

            // Deserialize the object
            final ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
            final ObjectInputStream inputStream = new ObjectInputStream(bis);
            final Object deserialized = inputStream.readObject();

            // Cast the deserialized object to the specified class and return it
            return objectClass.cast(deserialized);
        } catch (final IOException e) {
            throw new RuntimeException("Serialization error", e);
        } catch (final ClassNotFoundException e) {
            throw new RuntimeException("Deserialization error", e);
        }
    }

    /**
     * Tests the serialization/deserialization of an object.
     *
     * @param <T>
     *        the type of the object
     * @param object
     *        the object to be serialized/deserialized
     * @param objectClass
     *        the class of the object to be serialized/deserialized
     */
    public static <T extends Serializable> void checkSerialization(final T object,
            final Class<T> objectClass) {
        final T deserialized = serializeAndRecover(object, objectClass);
        Assert.assertEquals(object, deserialized);
        Assert.assertNotSame(object, deserialized);
        Assert.assertEquals(object.hashCode(), deserialized.hashCode());
        Assert.assertEquals(object.getClass(), deserialized.getClass());
    }

    /**
     * Gets every possible combinations of k elements among the provided elements.
     *
     * @param k
     *        the number of elements to select
     * @param elements
     *        the list of elements
     *
     * @return a list containing every possible combinations of k elements
     */
    public static List<int[]> getCombinations(final int k, final int[] elements) {
        final List<int[]> out = new ArrayList<>();

        // Return an empty list if there is less
        // elements than the number requested.
        if (k == 1) {
            // If k is equal to 1, each element is
            // a possible combination.
            for (final int element : elements) {
                out.add(new int[] { element });
            }
        } else if (elements.length == k) {
            // If k is equal to the number of elements available,
            // there is only one possible combination.
            out.add(elements);
        } else if (elements.length > k) {
            for (int i = 0; i < elements.length; i++) {
                final int[] subArray = Arrays.copyOfRange(elements, i + 1, elements.length);
                final List<int[]> subCombinations = getCombinations(k - 1, subArray);

                for (final int[] subCombination : subCombinations) {
                    final int[] combination = new int[k];
                    combination[0] = elements[i];
                    for (int j = 0; j < k - 1; j++) {
                        combination[j + 1] = subCombination[j];
                        out.add(combination);
                    }
                }
            }
        }

        return out;
    }

    /**
     * Returns a deep copy of a 2D {@code double} array.
     *
     * @param array
     *        the array to be copied
     *
     * @return a deep copy of the provided array, or {@code null} if the supplied array is
     *         {@code null}
     */
    public static double[][] copyArray(final double[][] array) {
        double[][] copy = null;
        if (array != null) {
            copy = new double[array.length][];
            for (int i = 0; i < array.length; i++) {
                copy[i] = Arrays.copyOf(array[i], array[i].length);
            }
        }
        return copy;
    }

    /**
     * Returns a string representation of the provided {@code double} array.
     *
     * @param array
     *        the array to be printed
     *
     * @return the string built
     */
    public static String printArray(final double[] array) {
        return printArray(array, DEFAULT_DECIMAL_FORMAT);
    }

    /**
     * Returns a string representation of the provided {@code double} array.
     *
     * @param array
     *        the array to be printed
     *
     * @return the string built
     */
    public static String printArray(final double[][] array) {
        return printArray(array, DEFAULT_DECIMAL_FORMAT);
    }

    /**
     * Returns a string representation of the provided {@code double} array.
     *
     * @param array
     *        the array to be printed
     * @param numberFormat
     *        the format to be used to represent the values
     *
     * @return the string built
     */
    public static String printArray(final double[] array, final String numberFormat) {
        return printArray(array, OPENING_CURLY_BRACKET, CLOSING_CURLY_BRACKET, ROW_SEPARATOR,
                numberFormat);
    }

    /**
     * Returns a string representation of the provided {@code double} array.
     *
     * @param array
     *        the array to be printed
     * @param numberFormat
     *        the format to be used to represent the values
     *
     * @return the string built
     */
    public static String printArray(final double[][] array, final String numberFormat) {
        return printArray(array, OPENING_CURLY_BRACKET + WHITESPACE, WHITESPACE
                + CLOSING_CURLY_BRACKET, OPENING_CURLY_BRACKET, CLOSING_CURLY_BRACKET,
                ROW_SEPARATOR + LINE_SEPARATOR + WHITESPACE + WHITESPACE, COLUMN_SEPARATOR,
                numberFormat);
    }

    /**
     * Returns a string representation of the provided {@code double} array.
     *
     * @param array
     *        the array to be printed
     * @param numberFormat
     *        the format to be used to represent the values
     *
     * @return the string built
     */
    public static String printArray(final double[] array, final DecimalFormat numberFormat) {
        return printArray(array, OPENING_CURLY_BRACKET, CLOSING_CURLY_BRACKET, ROW_SEPARATOR,
                numberFormat);
    }

    /**
     * Returns a string representation of the provided {@code double} array.
     *
     * @param array
     *        the array to be printed
     * @param numberFormat
     *        the format to be used to represent the values
     *
     * @return the string built
     */
    public static String printArray(final double[][] array, final DecimalFormat numberFormat) {
        return printArray(array, OPENING_CURLY_BRACKET + WHITESPACE, WHITESPACE
                + CLOSING_CURLY_BRACKET, OPENING_CURLY_BRACKET, CLOSING_CURLY_BRACKET,
                ROW_SEPARATOR + LINE_SEPARATOR + WHITESPACE + WHITESPACE, COLUMN_SEPARATOR,
                numberFormat);
    }

    /**
     * Returns a string representation of the provided {@code double} array.
     *
     * @param array
     *        the array to be printed
     * @param prefix
     *        the string to append before printing the array
     * @param suffix
     *        the string to append after printing the array
     * @param separator
     *        the string to append between the printed values
     * @param numberFormat
     *        the format to be used to represent the values
     *
     * @return the string built
     */
    public static String printArray(final double[] array, final String prefix, final String suffix,
            final String separator, final String numberFormat) {
        final StringBuilder builder = new StringBuilder();
        builder.append(prefix);
        for (int i = 0; i < array.length; i++) {
            if (i > 0) {
                builder.append(separator);
            }
            builder.append(String.format(numberFormat, array[i]));
        }
        builder.append(suffix);
        return builder.toString();
    }

    /**
     * Returns a string representation of the provided {@code double} array.
     *
     * @param array
     *        the array to be printed
     * @param prefix
     *        the string to append before printing the array
     * @param suffix
     *        the string to append after printing the array
     * @param columnPrefix
     *        the string to append before printing a column
     * @param columnSuffix
     *        the string to append after printing a column
     * @param rowSeparator
     *        the string to append between rows
     * @param columnSeparator
     *        the string to append between columns
     * @param numberFormat
     *        the format to be used to represent the values
     *
     * @return the string built
     */
    public static String printArray(final double[][] array, final String prefix,
            final String suffix, final String columnPrefix, final String columnSuffix,
            final String rowSeparator, final String columnSeparator, final String numberFormat) {
        final StringBuilder builder = new StringBuilder();
        builder.append(prefix);
        for (int i = 0; i < array.length; i++) {
            if (i > 0) {
                builder.append(rowSeparator);
            }
            builder.append(printArray(array[i], columnPrefix, columnSuffix, columnSeparator,
                    numberFormat));
        }
        builder.append(suffix);
        return builder.toString();
    }

    /**
     * Returns a string representation of the provided {@code double} array.
     *
     * @param array
     *        the array to be printed
     * @param prefix
     *        the string to append before printing the array
     * @param suffix
     *        the string to append after printing the array
     * @param separator
     *        the string to append between the printed values
     * @param numberFormat
     *        the format to be used to represent the values
     *
     * @return the string built
     */
    public static String printArray(final double[] array, final String prefix, final String suffix,
            final String separator, final DecimalFormat numberFormat) {
        final StringBuilder builder = new StringBuilder();
        builder.append(prefix);
        for (int i = 0; i < array.length; i++) {
            if (i > 0) {
                builder.append(separator);
            }
            builder.append(numberFormat.format(array[i]));
        }
        builder.append(suffix);
        return builder.toString();
    }

    /**
     * Returns a string representation of the provided {@code double} array.
     *
     * @param array
     *        the array to be printed
     * @param prefix
     *        the string to append before printing the array
     * @param suffix
     *        the string to append after printing the array
     * @param columnPrefix
     *        the string to append before printing a column
     * @param columnSuffix
     *        the string to append after printing a column
     * @param rowSeparator
     *        the string to append between rows
     * @param columnSeparator
     *        the string to append between columns
     * @param numberFormat
     *        the format to be used to represent the values
     *
     * @return the string built
     */
    public static String printArray(final double[][] array, final String prefix,
            final String suffix, final String columnPrefix, final String columnSuffix,
            final String rowSeparator, final String columnSeparator,
            final DecimalFormat numberFormat) {
        final StringBuilder builder = new StringBuilder();
        builder.append(prefix);
        for (int i = 0; i < array.length; i++) {
            if (i > 0) {
                builder.append(rowSeparator);
            }
            builder.append(printArray(array[i], columnPrefix, columnSuffix, columnSeparator,
                    numberFormat));
        }
        builder.append(suffix);
        return builder.toString();
    }

    /**
     * Gets the absolute or relative difference between two values.
     *
     * @param expected
     *        the expected value
     * @param actual
     *        the value tested
     * @param comparisonType
     *        the comparison type (absolute or relative)
     *
     * @return the absolute or relative difference computed
     */
    public static double getDifference(final double expected, final double actual,
            final ComparisonType comparisonType) {
        // Absolute difference
        double delta = actual - expected;

        if (comparisonType == ComparisonType.RELATIVE && delta != 0) {
            // Maximal absolute value
            final double max = MathLib.max(MathLib.abs(expected), MathLib.abs(actual));

            // Compute the relative difference
            if (max > Precision.SAFE_MIN) {
                delta = delta / max;
            } else {
                delta = Double.NaN;
            }
        }

        // Return the absolute or relative difference
        return delta;
    }

    /**
     * Gets the absolute or relative difference between two matrices.
     *
     * @param expected
     *        the expected value
     * @param actual
     *        the value tested
     * @param comparisonType
     *        the comparison type (absolute or relative)
     *
     * @return the absolute or relative difference matrix computed
     */
    public static RealMatrix getDifference(final RealMatrix expected, final RealMatrix actual,
            final ComparisonType comparisonType) {
        // Check matrices dimensions
        MatrixUtils.checkSubtractionCompatible(expected, actual);

        // Row & column dimensions
        final int nr = expected.getRowDimension();
        final int nc = expected.getColumnDimension();

        // Build the absolute or relative difference matrix
        final RealMatrix result = new Array2DRowRealMatrix(nr, nc);
        for (int i = 0; i < nr; i++) {
            for (int j = 0; j < nc; j++) {
                result.setEntry(i, j, getDifference(expected.getEntry(i, j), actual.getEntry(i, j), comparisonType));
            }
        }

        // Return the absolute or relative difference matrix
        return result;
    }

    /**
     * Gets the absolute or relative difference between two vectors.
     *
     * @param expected
     *        the expected value
     * @param actual
     *        the value tested
     * @param comparisonType
     *        the comparison type (absolute or relative)
     *
     * @return the absolute or relative difference vector computed
     */
    public static Vector3D getDifference(final Vector3D expected, final Vector3D actual,
            final ComparisonType comparisonType) {
        return new Vector3D(getDifference(expected.toArray(), actual.toArray(), comparisonType));
    }

    /**
     * Gets the absolute or relative difference between two arrays.
     *
     * @param expected
     *        the expected value
     * @param actual
     *        the value tested
     * @param comparisonType
     *        the comparison type (absolute or relative)
     *
     * @return the absolute or relative difference array computed
     */
    public static double[] getDifference(final double[] expected, final double[] actual,
            final ComparisonType comparisonType) {
        final int n = expected.length;
        MatrixUtils.checkDimension(n, actual.length);

        final double[] result = new double[n];
        for (int i = 0; i < n; i++) {
            result[i] = getDifference(expected[i], actual[i], comparisonType);
        }
        return result;
    }

    /**
     * Asserts the equality between two values within the specified tolerances.
     *
     * @param expected
     *        the expected value
     * @param actual
     *        the value tested
     * @param absTol
     *        the absolute tolerance
     * @param relTol
     *        the relative tolerance
     */
    public static void checkEquality(final Double expected, final Double actual,
            final double absTol, final double relTol) {
        if (expected == null ^ actual == null) {
            Assert.assertEquals(expected, actual);
        } else if (expected != actual) {
            final double absDiff = getDifference(expected, actual, ComparisonType.ABSOLUTE);
            final double relDiff = getDifference(expected, actual, ComparisonType.RELATIVE);

            final StringBuilder builder = new StringBuilder();
            builder.append("The supplied value differs from the expected one.");
            builder.append(System.lineSeparator());
            builder.append("     Supplied value: ");
            builder.append(DEFAULT_DECIMAL_FORMAT.format(actual));
            builder.append(System.lineSeparator());
            builder.append("     Expected value: ");
            builder.append(DEFAULT_DECIMAL_FORMAT.format(expected));
            builder.append(System.lineSeparator());
            builder.append("Absolute difference: ");
            builder.append(String.format(SCIENTIFIC_NUMBER_FORMAT, absDiff));
            builder.append(System.lineSeparator());
            builder.append("Relative difference: ");
            builder.append(String.format(SCIENTIFIC_NUMBER_FORMAT, relDiff));
            builder.append(System.lineSeparator());

            Assert.assertTrue(builder.toString(),
                    MathLib.abs(absDiff) <= absTol || MathLib.abs(relDiff) <= relTol);
        }
    }

    /**
     * Asserts the equality between two 1D arrays within the specified tolerance.
     *
     * @param expected
     *        the expected array
     * @param actual
     *        the array tested
     * @param absTol
     *        the absolute tolerance
     * @param relTol
     *        the relative tolerance
     */
    public static void checkEquality(final double[] expected, final double[] actual,
            final double absTol, final double relTol) {
        if (expected == null ^ actual == null) {
            Assert.assertEquals(expected, actual);
        } else if (expected != actual) {
            try {
                Assert.assertEquals(expected.length, actual.length);
                for (int i = 0; i < expected.length; i++) {
                    checkEquality(expected[i], actual[i], absTol, relTol);
                }
            } catch (final AssertionError e) {
                final StringBuilder builder = new StringBuilder();
                builder.append("The supplied 1D array differs from the expected one.");
                builder.append(System.lineSeparator());

                // Actual
                builder.append("Supplied 1D array:");
                builder.append(System.lineSeparator());
                builder.append(printArray(actual));
                builder.append(System.lineSeparator());

                // Expected
                builder.append("Expected 1D array:");
                builder.append(System.lineSeparator());
                builder.append(printArray(expected));
                builder.append(System.lineSeparator());

                // Absolute difference
                final double[] absoluteDifference = getDifference(expected, actual,
                        ComparisonType.ABSOLUTE);
                builder.append(System.lineSeparator());
                builder.append("Absolute difference: ");
                builder.append(System.lineSeparator());
                builder.append(printArray(absoluteDifference, SCIENTIFIC_NUMBER_FORMAT));
                builder.append(System.lineSeparator());

                // Relative difference
                final double[] relativeDifference = getDifference(expected, actual,
                        ComparisonType.RELATIVE);
                builder.append(System.lineSeparator());
                builder.append("relative difference: ");
                builder.append(System.lineSeparator());
                builder.append(printArray(relativeDifference, SCIENTIFIC_NUMBER_FORMAT));
                builder.append(System.lineSeparator());

                throw new AssertionError(builder.toString(), e);
            }
        }
        if (expected != actual) {
            Assert.assertEquals(expected.length, actual.length);

            for (int i = 0; i < expected.length; i++) {
                checkEquality(expected[i], actual[i], absTol, relTol);
            }
        }
    }

    /**
     * Asserts the equality between two 2D arrays within the specified tolerance.
     *
     * @param expected
     *        the expected array
     * @param actual
     *        the array tested
     * @param absTol
     *        the absolute tolerance
     * @param relTol
     *        the relative tolerance
     */
    public static void checkEquality(final double[][] expected, final double[][] actual,
            final double absTol, final double relTol) {
        if (expected == null ^ actual == null) {
            Assert.assertArrayEquals(expected, actual);
        } else if (expected != actual) {
            try {
                Assert.assertEquals(expected.length, actual.length);
                for (int i = 0; i < expected.length; i++) {
                    checkEquality(expected[i], actual[i], absTol, relTol);
                }
            } catch (final AssertionError e) {
                final StringBuilder builder = new StringBuilder();
                builder.append("The supplied 2D array differs from the expected one.");
                builder.append(System.lineSeparator());

                // Actual
                builder.append("Supplied 2D array:");
                builder.append(System.lineSeparator());
                builder.append(printArray(actual));
                builder.append(System.lineSeparator());

                // Expected
                builder.append("Expected 2D array:");
                builder.append(System.lineSeparator());
                builder.append(printArray(expected));
                builder.append(System.lineSeparator());

                throw new AssertionError(builder.toString(), e);
            }
        }
    }

    /**
     * Asserts the equality between two matrices within the specified tolerance.
     *
     * @param expected
     *        the expected matrix
     * @param actual
     *        the matrix tested
     * @param absTol
     *        the absolute tolerance
     * @param relTol
     *        the relative tolerance
     */
    public static void checkEquality(final RealMatrix expected, final RealMatrix actual,
            final double absTol, final double relTol) {
        if (expected == null ^ actual == null) {
            Assert.assertEquals(expected, actual);
        } else if (expected != actual) {
            try {
                final int nr1 = expected.getRowDimension();
                final int nr2 = actual.getRowDimension();
                final int nc1 = expected.getColumnDimension();
                final int nc2 = actual.getColumnDimension();

                Assert.assertEquals(nr1, nr2);
                Assert.assertEquals(nc1, nc2);

                // Check entries
                for (int i = 0; i < nr1; i++) {
                    for (int j = 0; j < nc1; j++) {
                        checkEquality(expected.getEntry(i, j), actual.getEntry(i, j), absTol,
                                relTol);
                    }
                }

                // Check the data
                checkEquality(expected.getData(), actual.getData(), absTol, relTol);

            } catch (final AssertionError e) {
                final StringBuilder builder = new StringBuilder();
                builder.append("The supplied matrix differs from the expected one.");
                builder.append(System.lineSeparator());

                // Actual
                builder.append(System.lineSeparator());
                builder.append("Supplied matrix: ");
                builder.append(System.lineSeparator());
                builder.append(actual.getClass().getSimpleName());
                builder.append(System.lineSeparator());
                builder.append(printArray(actual.getData()));
                builder.append(System.lineSeparator());

                // Expected
                builder.append(System.lineSeparator());
                builder.append("Expected matrix: ");
                builder.append(System.lineSeparator());
                builder.append(expected.getClass().getSimpleName());
                builder.append(System.lineSeparator());
                builder.append(printArray(expected.getData()));
                builder.append(System.lineSeparator());

                // Absolute difference
                final RealMatrix absoluteDifference = getDifference(expected, actual,
                        ComparisonType.ABSOLUTE);
                builder.append(System.lineSeparator());
                builder.append("Absolute difference: ");
                builder.append(System.lineSeparator());
                builder.append(printArray(absoluteDifference.getData(), SCIENTIFIC_NUMBER_FORMAT));
                builder.append(System.lineSeparator());

                // Relative difference
                final RealMatrix relativeDifference = getDifference(expected, actual,
                        ComparisonType.RELATIVE);
                builder.append(System.lineSeparator());
                builder.append("relative difference: ");
                builder.append(System.lineSeparator());
                builder.append(printArray(relativeDifference.getData(), SCIENTIFIC_NUMBER_FORMAT));
                builder.append(System.lineSeparator());

                throw new AssertionError(builder.toString(), e);
            }
        }
    }

    /**
     * Asserts the equality between two vectors within the specified tolerance.
     *
     * @param expected
     *        the expected vector
     * @param actual
     *        the vector tested
     * @param absTol
     *        the absolute tolerance
     * @param relTol
     *        the relative tolerance
     */
    public static void checkEquality(final RealVector expected, final RealVector actual,
            final double absTol, final double relTol) {
        if (expected == null ^ actual == null) {
            Assert.assertEquals(expected, actual);
        } else if (expected != actual) {
            try {
                final int dim1 = expected.getDimension();
                final int dim2 = actual.getDimension();

                Assert.assertEquals(dim1, dim2);

                // Check entries
                for (int i = 0; i < dim1; i++) {
                    checkEquality(expected.getEntry(i), actual.getEntry(i), absTol, relTol);
                }

                // Check the data
                checkEquality(expected.toArray(), actual.toArray(), absTol, relTol);
            } catch (final AssertionError e) {
                final StringBuilder builder = new StringBuilder();
                builder.append("The supplied real vector differs from the expected one.");
                builder.append(System.lineSeparator());

                // Actual
                builder.append("Supplied real vector:");
                builder.append(System.lineSeparator());
                builder.append(actual.getClass().getSimpleName());
                builder.append(System.lineSeparator());
                builder.append(printArray(actual.toArray()));
                builder.append(System.lineSeparator());

                // Expected
                builder.append("Expected real vector:");
                builder.append(System.lineSeparator());
                builder.append(actual.getClass().getSimpleName());
                builder.append(System.lineSeparator());
                builder.append(printArray(expected.toArray()));
                builder.append(System.lineSeparator());

                throw new AssertionError(builder.toString(), e);
            }
        }
    }

    /**
     * Asserts the equality between two vectors within the specified tolerance.
     *
     * @param expected
     *        the expected vector
     * @param actual
     *        the vector tested
     * @param absTol
     *        the absolute tolerance
     * @param relTol
     *        the relative tolerance
     */
    public static void checkEquality(final Vector3D expected, final Vector3D actual,
            final double absTol, final double relTol) {
        if (expected == null ^ actual == null) {
            Assert.assertEquals(expected, actual);
        } else if (expected != actual) {
            try {
                checkEquality(expected.getX(), actual.getX(), absTol, relTol);
                checkEquality(expected.getY(), actual.getY(), absTol, relTol);
                checkEquality(expected.getZ(), actual.getZ(), absTol, relTol);
            } catch (final AssertionError e) {
                final StringBuilder builder = new StringBuilder();
                builder.append("The supplied vector differs from the expected one.");
                builder.append(System.lineSeparator());

                // Actual
                builder.append("Supplied vector:");
                builder.append(System.lineSeparator());
                builder.append(actual.getClass().getSimpleName());
                builder.append(System.lineSeparator());
                builder.append(printArray(actual.toArray()));
                builder.append(System.lineSeparator());

                // Expected
                builder.append("Expected vector:");
                builder.append(System.lineSeparator());
                builder.append(actual.getClass().getSimpleName());
                builder.append(System.lineSeparator());
                builder.append(printArray(expected.toArray()));
                builder.append(System.lineSeparator());

                // Absolute difference
                final Vector3D absoluteDifference = getDifference(expected, actual,
                        ComparisonType.ABSOLUTE);
                builder.append(System.lineSeparator());
                builder.append("Absolute difference: ");
                builder.append(System.lineSeparator());
                builder.append(printArray(absoluteDifference.toArray(), SCIENTIFIC_NUMBER_FORMAT));
                builder.append(System.lineSeparator());

                // Relative difference
                final Vector3D relativeDifference = getDifference(expected, actual,
                        ComparisonType.RELATIVE);
                builder.append(System.lineSeparator());
                builder.append("relative difference: ");
                builder.append(System.lineSeparator());
                builder.append(printArray(relativeDifference.toArray(), SCIENTIFIC_NUMBER_FORMAT));
                builder.append(System.lineSeparator());

                throw new AssertionError(builder.toString(), e);
            }
        }
    }

    /**
     * Checks if two collections are equal (that is, they contain the same elements, in the same
     * order), and throws an exception if that's
     * not the case.
     *
     * @param <T>
     *        the type of the elements in the collections
     * @param expected
     *        the expected collection
     * @param actual
     *        the tested collection
     */
    public static <T> void checkEquality(final Collection<T> expected, final Collection<T> actual) {
        if (expected == null ^ actual == null) {
            Assert.assertEquals(expected, actual);
        } else if (expected != actual) {
            try {
                Assert.assertEquals(expected.size(), actual.size());

                final Iterator<T> iteratorE = expected.iterator();
                final Iterator<T> iteratorA = actual.iterator();

                while (iteratorE.hasNext() && iteratorA.hasNext()) {
                    final T itemE = iteratorE.next();
                    final T itemA = iteratorA.next();
                    Assert.assertEquals(itemE, itemA);
                }
            } catch (final AssertionError e) {
                boolean firstElement;

                final StringBuilder builder = new StringBuilder();
                builder.append("The supplied collection differs from the expected one.");
                builder.append(System.lineSeparator());

                // Actual
                builder.append("Supplied collection:");
                builder.append(System.lineSeparator());
                builder.append(actual.getClass().getSimpleName());
                builder.append(System.lineSeparator());
                builder.append(OPENING_CURLY_BRACKET);
                firstElement = true;
                for (final T element : actual) {
                    if (!firstElement) {
                        builder.append(COMMA + WHITESPACE);
                    }
                    builder.append(element);
                    firstElement = false;
                }
                builder.append(CLOSING_CURLY_BRACKET);
                builder.append(System.lineSeparator());

                // Expected
                builder.append("Expected collection:");
                builder.append(System.lineSeparator());
                builder.append(expected.getClass().getSimpleName());
                builder.append(System.lineSeparator());
                builder.append(OPENING_CURLY_BRACKET);
                firstElement = true;
                for (final T element : expected) {
                    if (!firstElement) {
                        builder.append(COMMA + WHITESPACE);
                    }
                    builder.append(element);
                    firstElement = false;
                }
                builder.append(CLOSING_CURLY_BRACKET);
                builder.append(System.lineSeparator());

                throw new AssertionError(builder.toString(), e);
            }
        }
    }

    /**
     * Tests the method that computes the trace of a matrix.
     *
     * @param matrix
     *        the matrix to be tested
     * @param absTol
     *        the absolute tolerance to take into account when comparing the computed traces
     * @param relTol
     *        the relative tolerance to take into account when comparing the computed traces
     */
    public static void
            checkTrace(final RealMatrix matrix, final double absTol, final double relTol) {
        // Copy the matrix as a standard real matrix
        final RealMatrix referenceMatrix = new Array2DRowRealMatrix(matrix.getData(), false);

        final double result = matrix.getTrace();
        final double expected = referenceMatrix.getTrace();
        checkEquality(expected, result, absTol, relTol);
    }

    /**
     * Tests the method that computes the norm of a matrix.
     *
     * @param matrix
     *        the matrix to be tested
     * @param absTol
     *        the absolute tolerance to take into account when comparing the computed norms
     * @param relTol
     *        the relative tolerance to take into account when comparing the computed norms
     */
    public static void checkNorm(final RealMatrix matrix, final double absTol, final double relTol) {
        // Copy the matrix as a standard real matrix
        final RealMatrix referenceMatrix = new Array2DRowRealMatrix(matrix.getData(), false);

        final double result = matrix.getNorm();
        final double expected = referenceMatrix.getNorm();
        checkEquality(expected, result, absTol, relTol);
    }

    /**
     * Tests the method that computes the Frobenius norm of a matrix.
     *
     * @param matrix
     *        the matrix to be tested
     * @param absTol
     *        the absolute tolerance to take into account when comparing the computed norms
     * @param relTol
     *        the relative tolerance to take into account when comparing the computed norms
     */
    public static void checkFrobeniusNorm(final RealMatrix matrix, final double absTol,
            final double relTol) {
        // Copy the matrix as a standard real matrix
        final RealMatrix referenceMatrix = new Array2DRowRealMatrix(matrix.getData(), false);

        final double result = matrix.getFrobeniusNorm();
        final double expected = referenceMatrix.getFrobeniusNorm();
        checkEquality(expected, result, absTol, relTol);
    }

    /**
     * Tests the method that computes the minimum value of a matrix.
     *
     * @param matrix
     *        the matrix to be tested
     * @param absTol
     *        the absolute tolerance to take into account when comparing the minimum values
     * @param relTol
     *        the relative tolerance to take into account when comparing the minimum values
     */
    public static void checkMinimum(final RealMatrix matrix, final double absTol,
            final double relTol) {
        // Copy the matrix as a standard real matrix
        final RealMatrix referenceMatrix = new Array2DRowRealMatrix(matrix.getData(), false);

        final double result = matrix.getMin();
        final double expected = referenceMatrix.getMin();
        checkEquality(expected, result, absTol, relTol);
    }

    /**
     * Tests the method that computes the maximum value of a matrix.
     *
     * @param matrix
     *        the matrix to be tested
     * @param absTol
     *        the absolute tolerance to take into account when comparing the maximum values
     * @param relTol
     *        the relative tolerance to take into account when comparing the maximum values
     */
    public static void checkMaximum(final RealMatrix matrix, final double absTol,
            final double relTol) {
        // Copy the matrix as a standard real matrix
        final RealMatrix referenceMatrix = new Array2DRowRealMatrix(matrix.getData(), false);

        final double result = matrix.getMax();
        final double expected = referenceMatrix.getMax();
        checkEquality(expected, result, absTol, relTol);
    }

    /**
     * Tests the method that computes the corresponding absolute values matrix.
     *
     * @param matrix
     *        the matrix to be tested
     * @param absTol
     *        the absolute tolerance to take into account when comparing the absolute values
     * @param relTol
     *        the relative tolerance to take into account when comparing the absolute values
     */
    public static void checkAbsMatrix(final RealMatrix matrix, final double absTol,
                                      final double relTol) {
        // Copy the matrix as a standard real matrix
        final RealMatrix referenceMatrix = new Array2DRowRealMatrix(matrix.getData(), false);

        final RealMatrix result = matrix.getAbs();
        final RealMatrix expected = referenceMatrix.getAbs();
        checkEquality(expected, result, absTol, relTol);
    }

    /**
     * Tests the method that copies the matrix.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain RealMatrix#copy()}
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     */
    public static void checkCopy(final RealMatrix matrix) {
        // Copy the matrix
        final RealMatrix copy = matrix.copy();

        // Ensure the copy is strictly identical to the
        checkEquality(matrix, copy, 0., 0.);
        Assert.assertNotSame(matrix, copy);
        Assert.assertEquals(matrix.getClass(), copy.getClass());
    }

    /**
     * Tests the methods that transpose the matrix.
     *
     * <p>
     * The results are tested against the following methods:<br>
     * {@linkplain Array2DRowRealMatrix#transpose()}<br>
     * {@linkplain Array2DRowRealMatrix#transpose(boolean)}<br>
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     */
    public static void checkTranspose(final RealMatrix matrix) {
        // Copy the matrix as a standard real matrix
        final RealMatrix referenceMatrix = new Array2DRowRealMatrix(matrix.getData(), false);

        RealMatrix transpose;
        RealMatrix expected;

        transpose = matrix.transpose();
        expected = referenceMatrix.transpose();
        checkEquality(expected, transpose, 0., 0.);
        Assert.assertEquals(matrix.getClass(), transpose.getClass());
        Assert.assertNotSame(matrix, transpose);

        transpose = matrix.transpose(true);
        expected = referenceMatrix.transpose(true);
        checkEquality(expected, transpose, 0., 0.);
        Assert.assertEquals(matrix.getClass(), transpose.getClass());
        Assert.assertNotSame(matrix, transpose);

        transpose = matrix.transpose(false);
        expected = referenceMatrix.transpose(false);
        checkEquality(expected, transpose, 0., 0.);
        Assert.assertEquals(matrix.getClass(), transpose.getClass());
        if (matrix instanceof SymmetricMatrix) {
            Assert.assertSame(matrix, transpose);
        }
    }

    /**
     * Tests the method that retrieves an entry of a matrix.
     *
     * <p>
     * The results are tested against the following method:<br>
     * {@linkplain Array2DRowRealMatrix#getEntry(int, int)}
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     */
    public static void checkGetEntry(final RealMatrix matrix) {
        // Copy the matrix as a standard real matrix
        final RealMatrix referenceMatrix = new Array2DRowRealMatrix(matrix.getData(), false);

        // Dimensions
        final int nr = matrix.getRowDimension();
        final int nc = matrix.getColumnDimension();

        // Check all the entries
        for (int i = 0; i < nr; i++) {
            for (int j = 0; j < nc; j++) {
                Assert.assertEquals(referenceMatrix.getEntry(i, j), matrix.getEntry(i, j), 0.);
            }
        }
    }

    /**
     * Tests the method that retrieves an entry of a matrix, with indices that are outside of the
     * valid index range.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain RealMatrix#getEntry(int, int)}
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     */
    public static void checkGetOutOfRangeEntry(final RealMatrix matrix) {
        // Dimensions
        final int nr = matrix.getRowDimension();
        final int nc = matrix.getColumnDimension();

        // The row index is equal to -1
        try {
            matrix.getEntry(-1, 0);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_ROW_INDEX_FORMAT, -1);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // The row index is equal to the row dimension
        try {
            matrix.getEntry(nr, 0);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_ROW_INDEX_FORMAT, nr);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // The column index is equal to -1
        try {
            matrix.getEntry(0, -1);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_COLUMN_INDEX_FORMAT, -1);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // The column index is equal to the column dimension
        try {
            matrix.getEntry(0, nc);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_COLUMN_INDEX_FORMAT, nc);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the methods that sets an entry of a matrix, using indices that are outside of the valid
     * index range.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain RealMatrix#setEntry(int, int, double)}
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     */
    public static void checkSetOutOfRangeEntry(final RealMatrix matrix) {
        // Dimensions
        final int nr = matrix.getRowDimension();
        final int nc = matrix.getColumnDimension();

        // The row index is equal to -1
        try {
            matrix.getEntry(-1, 0);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_ROW_INDEX_FORMAT, -1);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // The row index is equal to the row dimension
        try {
            matrix.getEntry(nr, 0);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_ROW_INDEX_FORMAT, nr);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // The column index is equal to -1
        try {
            matrix.getEntry(0, -1);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_COLUMN_INDEX_FORMAT, -1);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // The column index is equal to the column dimension
        try {
            matrix.getEntry(0, nc);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_COLUMN_INDEX_FORMAT, nc);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the method that adds a scalar to an entry of a matrix, using indices that are outside
     * of the valid index range.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain RealMatrix#addToEntry(int, int, double)}
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     */
    public static void checkAddToOutOfRangeEntry(final RealMatrix matrix) {
        // Dimensions
        final int nr = matrix.getRowDimension();
        final int nc = matrix.getColumnDimension();

        // The row index is equal to -1
        try {
            matrix.addToEntry(-1, 0, 0.);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_ROW_INDEX_FORMAT, -1);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // The row index is equal to the row dimension
        try {
            matrix.addToEntry(nr, 0, 0.);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_ROW_INDEX_FORMAT, nr);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // The column index is equal to -1
        try {
            matrix.addToEntry(0, -1, 0.);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_COLUMN_INDEX_FORMAT, -1);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // The column index is equal to the column dimension
        try {
            matrix.addToEntry(0, nc, 0.);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_COLUMN_INDEX_FORMAT, nc);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the method that multiplies an entry of a matrix by a scalar, using indices that are
     * outside the valid index range.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain RealMatrix#multiplyEntry(int, int, double)}
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     */
    public static void checkMultiplyOutOfRangeEntry(final RealMatrix matrix) {
        // Dimensions
        final int nr = matrix.getRowDimension();
        final int nc = matrix.getColumnDimension();

        // The row index is equal to -1
        try {
            matrix.multiplyEntry(-1, 0, 0.);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_ROW_INDEX_FORMAT, -1);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // The row index is equal to the row dimension
        try {
            matrix.multiplyEntry(nr, 0, 0.);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_ROW_INDEX_FORMAT, nr);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // The column index is equal to -1
        try {
            matrix.multiplyEntry(0, -1, 0.);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_COLUMN_INDEX_FORMAT, -1);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // The column index is equal to the column dimension
        try {
            matrix.multiplyEntry(0, nc, 0.);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_COLUMN_INDEX_FORMAT, nc);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the methods that extract the rows of a matrix.
     *
     * <p>
     * The results are tested against the following method:<br>
     * {@linkplain Array2DRowRealMatrix#getRow(int)}<br>
     * {@linkplain Array2DRowRealMatrix#getRowVector(int)}<br>
     * {@linkplain Array2DRowRealMatrix#getRowMatrix(int)}<br>
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     */
    public static void checkGetRow(final RealMatrix matrix) {
        // Copy the matrix as a standard real matrix
        final RealMatrix referenceMatrix = new Array2DRowRealMatrix(matrix.getData(), false);

        // Row Dimension
        final int nr = matrix.getRowDimension();

        // Retrieve the different rows as a double array
        for (int i = 0; i < nr; i++) {
            final double[] row = matrix.getRow(i);
            final double[] expected = referenceMatrix.getRow(i);
            checkEquality(expected, row, 0., 0.);
        }

        // Retrieve the different rows as a real vector
        for (int i = 0; i < nr; i++) {
            final RealVector row = matrix.getRowVector(i);
            final RealVector expected = referenceMatrix.getRowVector(i);
            checkEquality(expected, row, 0., 0.);
        }

        // Retrieve the different rows as a real matrix
        for (int i = 0; i < nr; i++) {
            final RealMatrix row = matrix.getRowMatrix(i);
            final RealMatrix expected = referenceMatrix.getRowMatrix(i);
            checkEquality(expected, row, 0., 0.);
        }
    }

    /**
     * Tests the methods that extract the rows of a matrix, using indices that are outside the valid
     * index range.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain RealMatrix#getRow(int)}<br>
     * {@linkplain RealMatrix#getRowVector(int)}<br>
     * {@linkplain RealMatrix#getRowMatrix(int)}<br>
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     */
    public static void checkGetOutOfRangeRow(final RealMatrix matrix) {
        // Row dimension
        final int nr = matrix.getRowDimension();

        // The row index is equal to -1
        try {
            matrix.getRow(-1);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_ROW_INDEX_FORMAT, -1);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
        try {
            matrix.getRowVector(-1);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_ROW_INDEX_FORMAT, -1);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
        try {
            matrix.getRowMatrix(-1);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_ROW_INDEX_FORMAT, -1);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // The row index is equal to the row dimension
        try {
            matrix.getRow(nr);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_ROW_INDEX_FORMAT, nr);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
        try {
            matrix.getRowVector(nr);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_ROW_INDEX_FORMAT, nr);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
        try {
            matrix.getRowMatrix(nr);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_ROW_INDEX_FORMAT, nr);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the methods that set the columns of a matrix, using indices that are outside the valid
     * index range.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain RealMatrix#setRow(int, double[])}<br>
     * {@linkplain RealMatrix#setRowVector(int, RealVector)}<br>
     * {@linkplain RealMatrix#setRowMatrix(int, RealMatrix)}<br>
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     */
    public static void checkSetOutOfRangeRow(final RealMatrix matrix) {
        // Dimensions
        final int nr = matrix.getRowDimension();
        final int nc = matrix.getColumnDimension();

        final double[] columnArray = new double[nc];
        final RealVector columnVector = new ArrayRealVector(columnArray);
        final RealMatrix columnMatrix = MatrixUtils.createRowRealMatrix(columnArray);

        // The column index is equal to -1
        try {
            matrix.setRow(-1, columnArray);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_ROW_INDEX_FORMAT, -1);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
        try {
            matrix.setRowVector(-1, columnVector);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_ROW_INDEX_FORMAT, -1);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
        try {
            matrix.setRowMatrix(-1, columnMatrix);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_ROW_INDEX_FORMAT, -1);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // The column index is equal to the column dimension
        try {
            matrix.setRow(nr, columnArray);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_ROW_INDEX_FORMAT, nr);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
        try {
            matrix.setRowVector(nr, columnVector);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_ROW_INDEX_FORMAT, nr);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
        try {
            matrix.setRowMatrix(nr, columnMatrix);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_ROW_INDEX_FORMAT, nr);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the methods that change the rows of a matrix, using incompatible arrays, vectors and
     * row matrices.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain RealMatrix#setRow(int, double[])}<br>
     * {@linkplain RealMatrix#setRowVector(int, RealVector)}<br>
     * {@linkplain RealMatrix#setRowMatrix(int, RealMatrix)}<br>
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     */
    public static void checkSetIncompatibleRow(final RealMatrix matrix) {
        // Column dimension
        final int nc = matrix.getColumnDimension();

        // The dimension of the provided row is too small
        try {
            matrix.setRow(0, new double[nc - 1]);
            Assert.fail();
        } catch (final MatrixDimensionMismatchException e) {
            final String expectedMessage = String.format(MATRIX_DIMENSION_MISMATCH_FORMAT, 1,
                    nc - 1, 1, nc);
            Assert.assertEquals(MatrixDimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.setRowVector(0, new ArrayRealVector(nc - 1));
            Assert.fail();
        } catch (final MatrixDimensionMismatchException e) {
            final String expectedMessage = String.format(MATRIX_DIMENSION_MISMATCH_FORMAT, 1,
                    nc - 1, 1, nc);
            Assert.assertEquals(MatrixDimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.setRowMatrix(0, new Array2DRowRealMatrix(1, nc - 1));
            Assert.fail();
        } catch (final MatrixDimensionMismatchException e) {
            final String expectedMessage = String.format(MATRIX_DIMENSION_MISMATCH_FORMAT, 1,
                    nc - 1, 1, nc);
            Assert.assertEquals(MatrixDimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // The dimension of the provided row is too large
        try {
            matrix.setRow(0, new double[nc + 1]);
            Assert.fail();
        } catch (final MatrixDimensionMismatchException e) {
            final String expectedMessage = String.format(MATRIX_DIMENSION_MISMATCH_FORMAT, 1,
                    nc + 1, 1, nc);
            Assert.assertEquals(MatrixDimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.setRowVector(0, new ArrayRealVector(nc + 1));
            Assert.fail();
        } catch (final MatrixDimensionMismatchException e) {
            final String expectedMessage = String.format(MATRIX_DIMENSION_MISMATCH_FORMAT, 1,
                    nc + 1, 1, nc);
            Assert.assertEquals(MatrixDimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.setRowMatrix(0, new Array2DRowRealMatrix(1, nc + 1));
            Assert.fail();
        } catch (final MatrixDimensionMismatchException e) {
            final String expectedMessage = String.format(MATRIX_DIMENSION_MISMATCH_FORMAT, 1,
                    nc + 1, 1, nc);
            Assert.assertEquals(MatrixDimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // The provided matrix is not a row matrix
        try {
            matrix.setRowMatrix(0, new Array2DRowRealMatrix(2, nc));
            Assert.fail();
        } catch (final MatrixDimensionMismatchException e) {
            final String expectedMessage = String.format(MATRIX_DIMENSION_MISMATCH_FORMAT, 2, nc,
                    1, nc);
            Assert.assertEquals(MatrixDimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the methods that extract the columns of a matrix.
     *
     * <p>
     * The results are tested against the following method:<br>
     * {@linkplain Array2DRowRealMatrix#getColumn(int)}<br>
     * {@linkplain Array2DRowRealMatrix#getColumnVector(int)}<br>
     * {@linkplain Array2DRowRealMatrix#getColumnMatrix(int)}<br>
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     */
    public static void checkGetColumn(final RealMatrix matrix) {
        // Copy the matrix as a standard real matrix
        final RealMatrix referenceMatrix = new Array2DRowRealMatrix(matrix.getData(), false);

        // Column dimension
        final int nc = matrix.getColumnDimension();

        // Retrieve the different columns as a double array
        for (int i = 0; i < nc; i++) {
            final double[] column = matrix.getColumn(i);
            final double[] expected = referenceMatrix.getColumn(i);
            checkEquality(expected, column, 0., 0.);
        }

        // Retrieve the different columns as a real vector
        for (int i = 0; i < nc; i++) {
            final RealVector column = matrix.getColumnVector(i);
            final RealVector expected = referenceMatrix.getColumnVector(i);
            checkEquality(expected, column, 0., 0.);
        }

        // Retrieve the different columns as a real matrix
        for (int i = 0; i < nc; i++) {
            final RealMatrix column = matrix.getColumnMatrix(i);
            final RealMatrix expected = referenceMatrix.getColumnMatrix(i);
            checkEquality(expected, column, 0., 0.);
        }
    }

    /**
     * Tests the methods that extract the columns of a matrix, using indices that are outside the
     * valid index range.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain RealMatrix#getColumn(int)}<br>
     * {@linkplain RealMatrix#getColumnVector(int)}<br>
     * {@linkplain RealMatrix#getColumnMatrix(int)}<br>
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     */
    public static void checkGetOutOfRangeColumn(final RealMatrix matrix) {
        // Column dimension
        final int nc = matrix.getColumnDimension();

        // The column index is equal to -1
        try {
            matrix.getColumn(-1);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_COLUMN_INDEX_FORMAT, -1);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
        try {
            matrix.getColumnVector(-1);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_COLUMN_INDEX_FORMAT, -1);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
        try {
            matrix.getColumnMatrix(-1);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_COLUMN_INDEX_FORMAT, -1);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // The column index is equal to the column dimension
        try {
            matrix.getColumn(nc);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_COLUMN_INDEX_FORMAT, nc);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
        try {
            matrix.getColumnVector(nc);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_COLUMN_INDEX_FORMAT, nc);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
        try {
            matrix.getColumnMatrix(nc);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_COLUMN_INDEX_FORMAT, nc);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the methods that set the columns of a matrix, using indices that are outside the valid
     * index range.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain RealMatrix#setColumn(int, double[])}<br>
     * {@linkplain RealMatrix#setColumnVector(int, RealVector)}<br>
     * {@linkplain RealMatrix#setColumnMatrix(int, RealMatrix)}<br>
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     */
    public static void checkSetOutOfRangeColumn(final RealMatrix matrix) {
        // Dimensions
        final int nr = matrix.getRowDimension();
        final int nc = matrix.getColumnDimension();

        final double[] rowArray = new double[nr];
        final RealVector rowVector = new ArrayRealVector(rowArray);
        final RealMatrix rowMatrix = MatrixUtils.createRowRealMatrix(rowArray);

        // The column index is equal to -1
        try {
            matrix.setColumn(-1, rowArray);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_COLUMN_INDEX_FORMAT, -1);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
        try {
            matrix.setColumnVector(-1, rowVector);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_COLUMN_INDEX_FORMAT, -1);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
        try {
            matrix.setColumnMatrix(-1, rowMatrix);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_COLUMN_INDEX_FORMAT, -1);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // The column index is equal to the column dimension
        try {
            matrix.setColumn(nc, rowArray);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_COLUMN_INDEX_FORMAT, nc);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
        try {
            matrix.setColumnVector(nc, rowVector);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_COLUMN_INDEX_FORMAT, nc);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
        try {
            matrix.setColumnMatrix(nc, rowMatrix);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_COLUMN_INDEX_FORMAT, nc);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the methods that change the columns of a matrix, using incompatible arrays, vectors and
     * column matrices.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain RealMatrix#setColumn(int, double[])}<br>
     * {@linkplain RealMatrix#setColumnVector(int, RealVector)}<br>
     * {@linkplain RealMatrix#setColumnMatrix(int, RealMatrix)}<br>
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     */
    public static void checkSetIncompatibleColumn(final RealMatrix matrix) {
        // Row dimension
        final int nr = matrix.getRowDimension();

        // The dimension of the provided column is too small
        try {
            matrix.setColumn(0, new double[nr - 1]);
            Assert.fail();
        } catch (final MatrixDimensionMismatchException e) {
            final String expectedMessage = String.format(MATRIX_DIMENSION_MISMATCH_FORMAT, nr - 1,
                    1, nr, 1);
            Assert.assertEquals(MatrixDimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.setColumnVector(0, new ArrayRealVector(nr - 1));
            Assert.fail();
        } catch (final MatrixDimensionMismatchException e) {
            final String expectedMessage = String.format(MATRIX_DIMENSION_MISMATCH_FORMAT, nr - 1,
                    1, nr, 1);
            Assert.assertEquals(MatrixDimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.setColumnMatrix(0, new Array2DRowRealMatrix(nr - 1, 1));
            Assert.fail();
        } catch (final MatrixDimensionMismatchException e) {
            final String expectedMessage = String.format(MATRIX_DIMENSION_MISMATCH_FORMAT, nr - 1,
                    1, nr, 1);
            Assert.assertEquals(MatrixDimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // The dimension of the provided column is too large
        try {
            matrix.setColumn(0, new double[nr + 1]);
            Assert.fail();
        } catch (final MatrixDimensionMismatchException e) {
            final String expectedMessage = String.format(MATRIX_DIMENSION_MISMATCH_FORMAT, nr + 1,
                    1, nr, 1);
            Assert.assertEquals(MatrixDimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.setColumnVector(0, new ArrayRealVector(nr + 1));
            Assert.fail();
        } catch (final MatrixDimensionMismatchException e) {
            final String expectedMessage = String.format(MATRIX_DIMENSION_MISMATCH_FORMAT, nr + 1,
                    1, nr, 1);
            Assert.assertEquals(MatrixDimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.setColumnMatrix(0, new Array2DRowRealMatrix(nr + 1, 1));
            Assert.fail();
        } catch (final MatrixDimensionMismatchException e) {
            final String expectedMessage = String.format(MATRIX_DIMENSION_MISMATCH_FORMAT, nr + 1,
                    1, nr, 1);
            Assert.assertEquals(MatrixDimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // The provided matrix is not a column matrix
        try {
            matrix.setColumnMatrix(0, new Array2DRowRealMatrix(nr, 2));
            Assert.fail();
        } catch (final MatrixDimensionMismatchException e) {
            final String expectedMessage = String.format(MATRIX_DIMENSION_MISMATCH_FORMAT, nr, 2,
                    nr, 1);
            Assert.assertEquals(MatrixDimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the method that extracts parts of a matrix by specifying the row/column index range.
     *
     * <p>
     * The results are tested against the following method:<br>
     * {@linkplain Array2DRowRealMatrix#getSubMatrix(int, int, int, int)}<br>
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     */
    public static void checkGetSubMatrixByRange(final RealMatrix matrix) {
        // Copy the matrix as a standard real matrix
        final RealMatrix referenceMatrix = new Array2DRowRealMatrix(matrix.getData(), false);

        // Dimensions
        final int nr = matrix.getRowDimension();
        final int nc = matrix.getColumnDimension();

        RealMatrix submatrix;
        RealMatrix expected;

        // Copy the submatrix in an array having exactly the right size
        for (int i = 0; i < nr; i++) {
            for (int j = 0; j < nc; j++) {
                // Upper left submatrix
                submatrix = matrix.getSubMatrix(0, i, 0, j);
                expected = referenceMatrix.getSubMatrix(0, i, 0, j);
                checkEquality(expected, submatrix, 0., 0.);

                // Upper right submatrix
                submatrix = matrix.getSubMatrix(0, i, j, nc - 1);
                expected = referenceMatrix.getSubMatrix(0, i, j, nc - 1);
                checkEquality(expected, submatrix, 0., 0.);

                // Lower left submatrix
                submatrix = matrix.getSubMatrix(i, nr - 1, 0, j);
                expected = referenceMatrix.getSubMatrix(i, nr - 1, 0, j);
                checkEquality(expected, submatrix, 0., 0.);

                // Lower right submatrix
                submatrix = matrix.getSubMatrix(i, nr - 1, j, nc - 1);
                expected = referenceMatrix.getSubMatrix(i, nr - 1, j, nc - 1);
                checkEquality(expected, submatrix, 0., 0.);
            }
        }
    }

    /**
     * Tests the method that extracts parts of a matrix by supplying specific the row/column
     * indices.
     *
     * <p>
     * The results are tested against the following method:<br>
     * {@linkplain Array2DRowRealMatrix#getSubMatrix(int[], int[])}<br>
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     */
    public static void checkGetSubMatrixByIndex(final RealMatrix matrix) {
        // Copy the matrix as a standard real matrix
        final RealMatrix referenceMatrix = new Array2DRowRealMatrix(matrix.getData(), false);

        // Dimensions
        final int nr = matrix.getRowDimension();
        final int nc = matrix.getColumnDimension();

        // Generate every possible combination of row indices
        final int[] rowIndices = IntStream.range(0, nr).toArray();
        final List<int[]> rowCombinations = new ArrayList<>();
        for (int k = 1; k <= nr; k++) {
            rowCombinations.addAll(getCombinations(k, rowIndices));
        }

        // Generate every possible combination of column indices
        final int[] columnIndices = IntStream.range(0, nc).toArray();
        final List<int[]> columnCombinations = new ArrayList<>();
        for (int k = 1; k <= nr; k++) {
            columnCombinations.addAll(getCombinations(k, columnIndices));
        }

        RealMatrix submatrix;
        RealMatrix expected;

        // Get the submatrix when the selected row/column index arrays do not contain any duplicates
        for (final int[] selectedRows : rowCombinations) {
            for (final int[] selectedColumns : columnCombinations) {
                submatrix = matrix.getSubMatrix(selectedRows, selectedColumns);
                expected = referenceMatrix.getSubMatrix(selectedRows, selectedColumns);
                checkEquality(expected, submatrix, 0., 0.);
            }
        }

        // Get the submatrix when the selected row/column index arrays contain duplicates
        for (final int[] selectedRows : rowCombinations) {
            for (final int[] selectedColumns : columnCombinations) {
                final int[] rows = Arrays.copyOf(selectedRows, selectedRows.length + 7);
                final int[] columns = Arrays.copyOf(selectedRows, selectedColumns.length + 3);
                submatrix = matrix.getSubMatrix(rows, columns);
                expected = referenceMatrix.getSubMatrix(rows, columns);
                checkEquality(expected, submatrix, 0., 0.);
            }
        }
    }

    /**
     * Tests the method that extracts parts of a matrix, supplying an invalid row/column index
     * range.
     *
     * @param matrix
     *        the matrix to be tested
     */
    public static void checkGetSubMatrixInvalidRange(final RealMatrix matrix) {
        // Dimensions
        final int nr = matrix.getRowDimension();
        final int nc = matrix.getColumnDimension();

        try {
            matrix.getSubMatrix(-1, nr - 1, 0, nc - 1);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_ROW_INDEX_FORMAT, -1);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.getSubMatrix(0, nr, 0, nc - 1);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_ROW_INDEX_FORMAT, nr);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.getSubMatrix(0, nr - 1, -1, nc - 1);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_COLUMN_INDEX_FORMAT, -1);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.getSubMatrix(0, nr - 1, 0, nc);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_COLUMN_INDEX_FORMAT, nc);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the method that extracts parts of a matrix, supplying an invalid index.
     *
     * @param matrix
     *        the matrix to be tested
     */
    public static void checkGetSubMatrixInvalidIndex(final RealMatrix matrix) {
        // Dimensions
        final int nr = matrix.getRowDimension();
        final int nc = matrix.getColumnDimension();

        try {
            final int[] selectedRows = { -1 };
            final int[] selectedColumns = { 0, nc - 1 };
            matrix.getSubMatrix(selectedRows, selectedColumns);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_ROW_INDEX_FORMAT, -1);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            final int[] selectedRows = { nr };
            final int[] selectedColumns = { 0, nc - 1 };
            matrix.getSubMatrix(selectedRows, selectedColumns);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_ROW_INDEX_FORMAT, nr);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            final int[] selectedRows = { 0, nr - 1 };
            final int[] selectedColumns = { -1 };
            matrix.getSubMatrix(selectedRows, selectedColumns);
            matrix.getSubMatrix(0, nr - 1, -1, nc - 1);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_COLUMN_INDEX_FORMAT, -1);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            final int[] selectedRows = { 0, nr - 1 };
            final int[] selectedColumns = { nc };
            matrix.getSubMatrix(selectedRows, selectedColumns);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_COLUMN_INDEX_FORMAT, nc);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the method that extracts parts of a matrix, supplying a {@code null} index array.
     *
     * @param matrix
     *        the matrix to be tested
     */
    public static void checkGetSubMatrixNullIndex(final RealMatrix matrix) {
        // Dimensions
        final int nr = matrix.getRowDimension();
        final int nc = matrix.getColumnDimension();

        // Null row index array
        try {
            final int[] selectedRows = null;
            final int[] selectedColumns = { 0, nc - 1 };
            matrix.getSubMatrix(selectedRows, selectedColumns);
            Assert.fail();
        } catch (final NullArgumentException e) {
            final String expectedMessage = String.format(NULL_ARGUMENT_FORMAT);
            Assert.assertEquals(NullArgumentException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // Null column index array
        try {
            final int[] selectedRows = { 0, nr - 1 };
            final int[] selectedColumns = null;
            matrix.getSubMatrix(selectedRows, selectedColumns);
            Assert.fail();
        } catch (final NullArgumentException e) {
            final String expectedMessage = String.format(NULL_ARGUMENT_FORMAT);
            Assert.assertEquals(NullArgumentException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the method that extracts parts of a matrix, supplying an empty index array.
     *
     * @param matrix
     *        the matrix to be tested
     */
    public static void checkGetSubMatrixEmptyIndex(final RealMatrix matrix) {
        // Dimensions
        final int nr = matrix.getRowDimension();
        final int nc = matrix.getColumnDimension();

        // Empty row index array
        try {
            final int[] selectedRows = {};
            final int[] selectedColumns = { 0, nc - 1 };
            matrix.getSubMatrix(selectedRows, selectedColumns);
            Assert.fail();
        } catch (final NoDataException e) {
            final String expectedMessage = String.format(EMPTY_ROW_INDEX_FORMAT);
            Assert.assertEquals(NoDataException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // Empty column index array
        try {
            final int[] selectedRows = { 0, nr - 1 };
            final int[] selectedColumns = {};
            matrix.getSubMatrix(selectedRows, selectedColumns);
            Assert.fail();
        } catch (final NoDataException e) {
            final String expectedMessage = String.format(EMPTY_COLUMN_INDEX_FORMAT);
            Assert.assertEquals(NoDataException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the method that replaces parts of a matrix, supplying an invalid row or column start
     * index.
     *
     * @param matrix
     *        the matrix to be tested
     */
    public static void checkSetSubMatrixInvalidIndex(final RealMatrix matrix) {
        // Dimensions
        final int nr = matrix.getRowDimension();
        final int nc = matrix.getColumnDimension();

        // Invalid row index
        try {
            matrix.setSubMatrix(new double[1][1], -1, 0);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_ROW_INDEX_FORMAT, -1);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.setSubMatrix(new double[1][1], nr, 0);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_ROW_INDEX_FORMAT, nr);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // Invalid column index
        try {
            matrix.setSubMatrix(new double[1][1], 0, -1);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_COLUMN_INDEX_FORMAT, -1);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.setSubMatrix(new double[1][1], 0, nc);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_COLUMN_INDEX_FORMAT, nc);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the method that replaces parts of a matrix, supplying a {@code null} submatrix data
     * array.
     *
     * @param matrix
     *        the matrix to be tested
     */
    public static void checkSetSubMatrixNullData(final RealMatrix matrix) {
        try {
            matrix.setSubMatrix(null, 0, 0);
            Assert.fail();
        } catch (final NullArgumentException e) {
            final String expectedMessage = String.format(NULL_ARRAY_FORMAT);
            Assert.assertEquals(NullArgumentException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the method that replaces parts of a matrix, supplying an empty submatrix data array.
     *
     * @param matrix
     *        the matrix to be tested
     */
    public static void checkSetSubMatrixEmptyData(final RealMatrix matrix) {
        try {
            final double[][] submatrix = new double[0][0];
            matrix.setSubMatrix(submatrix, 0, 0);
            Assert.fail();
        } catch (final NoDataException e) {
            final String expectedMessage = String.format(MATRIX_EMPTY_ROW_FORMAT);
            Assert.assertEquals(NoDataException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            final double[][] submatrix = new double[1][0];
            matrix.setSubMatrix(submatrix, 0, 0);
            Assert.fail();
        } catch (final NoDataException e) {
            final String expectedMessage = String.format(MATRIX_EMPTY_COLUMN_FORMAT);
            Assert.assertEquals(NoDataException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the method that replaces parts of a matrix, supplying an invalid submatrix data array.
     *
     * @param matrix
     *        the matrix to be tested
     */
    public static void checkSetSubMatrixInvalidData(final RealMatrix matrix) {
        try {
            final double[][] invalidData = { { 1. }, { 2., 3. } };
            matrix.setSubMatrix(invalidData, 0, 0);
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, 2, 1);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the methods that extract parts of a matrix by supplying specific the start/end indices
     * (same indices for the row and columns).
     *
     * <p>
     * The results are tested against the following method:<br>
     * {@linkplain Array2DRowRealMatrix#getSubMatrix(int, int, int, int)}<br>
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     * @param expectedClass
     *        the expected class for the extracted submatrices
     */
    public static void checkGetSymmetricSubMatrixByRange(final SymmetricMatrix matrix,
            final Class<? extends RealMatrix> expectedClass) {
        // Copy the matrix as a standard real matrix
        final RealMatrix referenceMatrix = new Array2DRowRealMatrix(matrix.getData(), false);

        // Matrix dimension
        final int dim = matrix.getRowDimension();

        RealMatrix submatrix;
        RealMatrix expected;

        // Copy the submatrix in an array having exactly the right size
        for (int i = 0; i < dim; i++) {
            // Upper left submatrix
            submatrix = matrix.getSubMatrix(0, i);
            expected = referenceMatrix.getSubMatrix(0, i, 0, i);
            checkEquality(expected, submatrix, 0., 0.);
            Assert.assertEquals(expectedClass, submatrix.getClass());

            // Lower right submatrix
            submatrix = matrix.getSubMatrix(i, dim - 1);
            expected = referenceMatrix.getSubMatrix(i, dim - 1, i, dim - 1);
            checkEquality(expected, submatrix, 0., 0.);
            Assert.assertEquals(expectedClass, submatrix.getClass());
        }
    }

    /**
     * Tests the methods that extract parts of a matrix by supplying specific the row/column indices
     * (same array for both).
     *
     * <p>
     * The results are tested against the following method:<br>
     * {@linkplain Array2DRowRealMatrix#getSubMatrix(int[], int[])}<br>
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     * @param expectedClass
     *        the expected class for the extracted submatrices
     */
    public static void checkGetSymmetricSubMatrixByIndex(final SymmetricMatrix matrix,
            final Class<? extends RealMatrix> expectedClass) {
        // Copy the matrix as a standard real matrix
        final RealMatrix referenceMatrix = new Array2DRowRealMatrix(matrix.getData(), false);

        // Matrix dimension
        final int dim = matrix.getRowDimension();

        // Generate every possible combination of row indices
        final int[] indexRange = IntStream.range(0, dim).toArray();
        final List<int[]> combinations = new ArrayList<>();
        for (int k = 1; k <= dim; k++) {
            combinations.addAll(getCombinations(k, indexRange));
        }

        RealMatrix submatrix;
        RealMatrix expected;

        // Get the submatrix when the selected row/column index arrays do not contain any duplicates
        for (final int[] selectedIndices : combinations) {
            submatrix = matrix.getSubMatrix(selectedIndices, selectedIndices);
            expected = referenceMatrix.getSubMatrix(selectedIndices, selectedIndices);
            checkEquality(expected, submatrix, 0., 0.);
            Assert.assertEquals(expectedClass, submatrix.getClass());

            submatrix = matrix.getSubMatrix(selectedIndices);
            expected = referenceMatrix.getSubMatrix(selectedIndices, selectedIndices);
            checkEquality(expected, submatrix, 0., 0.);
            Assert.assertEquals(expectedClass, submatrix.getClass());
        }

        // Get the submatrix when the selected row/column index arrays contain duplicates
        for (final int[] selectedIndices : combinations) {
            final int[] indices = Arrays.copyOf(selectedIndices, selectedIndices.length + 7);

            submatrix = matrix.getSubMatrix(indices, indices);
            expected = referenceMatrix.getSubMatrix(indices, indices);
            checkEquality(expected, submatrix, 0., 0.);
            Assert.assertEquals(expectedClass, submatrix.getClass());

            submatrix = matrix.getSubMatrix(indices);
            expected = referenceMatrix.getSubMatrix(indices, indices);
            checkEquality(expected, submatrix, 0., 0.);
            Assert.assertEquals(expectedClass, submatrix.getClass());
        }
    }

    /**
     * Tests the method that extracts parts of a matrix, supplying an invalid index.
     *
     * @param matrix
     *        the matrix to be tested
     */
    public static void checkGetSymmetricSubMatrixInvalidRange(final SymmetricMatrix matrix) {
        // Dimensions
        final int dim = matrix.getRowDimension();

        try {
            matrix.getSubMatrix(-1, dim - 1);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_ROW_INDEX_FORMAT, -1);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.getSubMatrix(0, dim);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_ROW_INDEX_FORMAT, dim);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the method that extracts parts of a matrix, supplying an invalid index array.
     *
     * @param matrix
     *        the matrix to be tested
     */
    public static void checkGetSymmetricSubMatrixInvalidIndex(final SymmetricMatrix matrix) {
        // Dimensions
        final int dim = matrix.getRowDimension();

        try {
            final int[] selectedIndices = { -1 };
            matrix.getSubMatrix(selectedIndices);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_ROW_INDEX_FORMAT, -1);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            final int[] selectedIndices = { dim };
            matrix.getSubMatrix(selectedIndices);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_ROW_INDEX_FORMAT, dim);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the method that extracts parts of a matrix, supplying a {@code null} index array.
     *
     * @param matrix
     *        the matrix to be tested
     */
    public static void checkGetSymmetricSubMatrixNullIndex(final SymmetricMatrix matrix) {
        try {
            final int[] selectedIndices = null;
            matrix.getSubMatrix(selectedIndices);
            Assert.fail();
        } catch (final NullArgumentException e) {
            final String expectedMessage = String.format(NULL_ARGUMENT_FORMAT);
            Assert.assertEquals(NullArgumentException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the method that extracts parts of a matrix, supplying an empty index array.
     *
     * @param matrix
     *        the matrix to be tested
     */
    public static void checkGetSymmetricSubMatrixEmptyIndex(final SymmetricMatrix matrix) {
        try {
            final int[] selectedIndices = {};
            matrix.getSubMatrix(selectedIndices);
            Assert.fail();
        } catch (final NoDataException e) {
            final String expectedMessage = String.format(EMPTY_ROW_INDEX_FORMAT);
            Assert.assertEquals(NoDataException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the methods that copy parts of a matrix into an array by specifying row/column index
     * range.
     *
     * <p>
     * The results are tested against the following methods:<br>
     * {@linkplain Array2DRowRealMatrix#copySubMatrix(int, int, int, int, double[][])}<br>
     * {@linkplain Array2DRowRealMatrix#copySubMatrix(int, int, int, int, double[][], int, int)}<br>
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     */
    public static void checkCopySubMatrixByRange(final RealMatrix matrix) {
        // Copy the matrix as a standard real matrix
        final RealMatrix referenceMatrix = new Array2DRowRealMatrix(matrix.getData(), false);

        // Dimensions
        final int nr = matrix.getRowDimension();
        final int nc = matrix.getColumnDimension();

        double[][] data;
        double[][] expected;

        // Copy the submatrix in an array having exactly the right size
        for (int i = 0; i < nr; i++) {
            for (int j = 0; j < nc; j++) {
                // Upper left submatrix
                data = new double[i + 1][j + 1];
                expected = new double[i + 1][j + 1];
                matrix.copySubMatrix(0, i, 0, j, data);
                referenceMatrix.copySubMatrix(0, i, 0, j, expected);
                checkEquality(expected, data, 0., 0.);

                // Upper right submatrix
                data = new double[i + 1][nc - j];
                expected = new double[i + 1][nc - j];
                matrix.copySubMatrix(0, i, j, nc - 1, data);
                referenceMatrix.copySubMatrix(0, i, j, nc - 1, expected);
                checkEquality(expected, data, 0., 0.);

                // Lower left submatrix
                data = new double[nr - i][j + 1];
                expected = new double[nr - i][j + 1];
                matrix.copySubMatrix(i, nr - 1, 0, j, data);
                referenceMatrix.copySubMatrix(i, nr - 1, 0, j, expected);
                checkEquality(expected, data, 0., 0.);

                // Lower right submatrix
                data = new double[nr - i][nc - j];
                expected = new double[nr - i][nc - j];
                matrix.copySubMatrix(i, nr - 1, j, nc - 1, data);
                referenceMatrix.copySubMatrix(i, nr - 1, j, nc - 1, expected);
                checkEquality(expected, data, 0., 0.);
            }
        }

        // Copy the submatrix in an array larger than the extracted submatrix
        for (int i = 0; i < nr; i++) {
            for (int j = 0; j < nc; j++) {
                // Upper left submatrix
                data = new double[nr][nc];
                expected = new double[nr][nc];
                matrix.copySubMatrix(0, i, 0, j, data);
                referenceMatrix.copySubMatrix(0, i, 0, j, expected);
                checkEquality(expected, data, 0., 0.);

                // Upper right submatrix
                data = new double[nr][nc];
                expected = new double[nr][nc];
                matrix.copySubMatrix(0, i, j, nc - 1, data);
                referenceMatrix.copySubMatrix(0, i, j, nc - 1, expected);
                checkEquality(expected, data, 0., 0.);

                // Lower left submatrix
                data = new double[nr][nc];
                expected = new double[nr][nc];
                matrix.copySubMatrix(i, nr - 1, 0, j, data);
                referenceMatrix.copySubMatrix(i, nr - 1, 0, j, expected);
                checkEquality(expected, data, 0., 0.);

                // Lower right submatrix
                data = new double[nr][nc];
                expected = new double[nr][nc];
                matrix.copySubMatrix(i, nr - 1, j, nc - 1, data);
                referenceMatrix.copySubMatrix(i, nr - 1, j, nc - 1, expected);
                checkEquality(expected, data, 0., 0.);
            }
        }

        // Copy the submatrix in an array larger than the extracted submatrix,
        // and specify the start row and column indices
        for (int i = 0; i < nr; i++) {
            for (int j = 0; j < nc; j++) {
                // Upper left submatrix
                data = new double[nr][nc];
                expected = new double[nr][nc];
                matrix.copySubMatrix(0, i, 0, j, data, 0, 0);
                referenceMatrix.copySubMatrix(0, i, 0, j, expected, 0, 0);
                checkEquality(expected, data, 0., 0.);

                // Upper right submatrix
                data = new double[nr][nc];
                expected = new double[nr][nc];
                matrix.copySubMatrix(0, i, j, nc - 1, data, 0, j);
                referenceMatrix.copySubMatrix(0, i, j, nc - 1, expected, 0, j);
                checkEquality(expected, data, 0., 0.);

                // Lower left submatrix
                data = new double[nr][nc];
                expected = new double[nr][nc];
                matrix.copySubMatrix(i, nr - 1, 0, j, data, i, 0);
                referenceMatrix.copySubMatrix(i, nr - 1, 0, j, expected, i, 0);
                checkEquality(expected, data, 0., 0.);

                // Lower right submatrix
                data = new double[nr][nc];
                expected = new double[nr][nc];
                matrix.copySubMatrix(i, nr - 1, j, nc - 1, data, i, j);
                referenceMatrix.copySubMatrix(i, nr - 1, j, nc - 1, expected, i, j);
                checkEquality(expected, data, 0., 0.);
            }
        }
    }

    /**
     * Tests the methods that copy parts of a matrix into an array by supplying specific row/column
     * indices.
     *
     * <p>
     * The results are tested against the following methods:<br>
     * {@linkplain Array2DRowRealMatrix#copySubMatrix(int[], int[], double[][])}<br>
     * {@linkplain Array2DRowRealMatrix#copySubMatrix(int[], int[], double[][], int, int)}<br>
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     */
    public static void checkCopySubMatrixByIndex(final RealMatrix matrix) {
        // Copy the matrix as a standard real matrix
        final RealMatrix referenceMatrix = new Array2DRowRealMatrix(matrix.getData(), false);

        // Dimensions
        final int nr = matrix.getRowDimension();
        final int nc = matrix.getColumnDimension();

        // Generate every possible combination of row indices
        final int[] rowIndices = IntStream.range(0, nr).toArray();
        final List<int[]> rowCombinations = new ArrayList<>();
        for (int k = 1; k <= nr; k++) {
            rowCombinations.addAll(getCombinations(k, rowIndices));
        }

        // Generate every possible combination of column indices
        final int[] columnIndices = IntStream.range(0, nc).toArray();
        final List<int[]> columnCombinations = new ArrayList<>();
        for (int k = 1; k <= nr; k++) {
            columnCombinations.addAll(getCombinations(k, columnIndices));
        }

        double[][] data;
        double[][] expected;

        // Copy the submatrix in an array having exactly the right size
        for (final int[] selectedRows : rowCombinations) {
            for (final int[] selectedColumns : columnCombinations) {
                data = new double[selectedRows.length][selectedColumns.length];
                expected = new double[selectedRows.length][selectedColumns.length];
                matrix.copySubMatrix(selectedRows, selectedColumns, data);
                referenceMatrix.copySubMatrix(selectedRows, selectedColumns, expected);
                checkEquality(expected, data, 0., 0.);
            }
        }

        // Copy the submatrix in an array larger than the extracted submatrix
        for (final int[] selectedRows : rowCombinations) {
            for (final int[] selectedColumns : columnCombinations) {
                data = new double[nr][nc];
                expected = new double[nr][nc];
                matrix.copySubMatrix(selectedRows, selectedColumns, data);
                referenceMatrix.copySubMatrix(selectedRows, selectedColumns, expected);
                checkEquality(expected, data, 0., 0.);
            }
        }

        // Copy the submatrix in an array larger than the extracted submatrix,
        // and specify the start row and column indices in the output array.
        for (final int[] selectedRows : rowCombinations) {
            for (final int[] selectedColumns : columnCombinations) {
                data = new double[nr][nc];
                expected = new double[nr][nc];
                final int startRow = nr - selectedRows.length;
                final int startColumn = nc - selectedColumns.length;
                matrix.copySubMatrix(selectedRows, selectedColumns, data, startRow, startColumn);
                referenceMatrix.copySubMatrix(selectedRows, selectedColumns, expected, startRow,
                        startColumn);
                checkEquality(expected, data, 0., 0.);
            }
        }

        // Copy the submatrix in an array having exactly the right size.
        // The selected row and column index arrays contain duplicates.
        for (final int[] selectedRows : rowCombinations) {
            for (final int[] selectedColumns : columnCombinations) {
                final int[] rows = Arrays.copyOf(selectedRows, selectedRows.length + 4);
                final int[] columns = Arrays.copyOf(selectedRows, selectedColumns.length + 5);
                data = new double[rows.length][columns.length];
                expected = new double[rows.length][columns.length];
                matrix.copySubMatrix(rows, columns, data);
                referenceMatrix.copySubMatrix(rows, columns, expected);
                checkEquality(expected, data, 0., 0.);
            }
        }

        // Copy the submatrix in an array larger than the extracted submatrix,
        // and specify the start row and column indices in the output array.
        // The selected row and column index arrays contain duplicates.
        for (final int[] selectedRows : rowCombinations) {
            for (final int[] selectedColumns : columnCombinations) {
                final int[] rows = Arrays.copyOf(selectedRows, selectedRows.length + 2);
                final int[] columns = Arrays.copyOf(selectedRows, selectedColumns.length + 5);
                data = new double[nr + 2][nc + 5];
                expected = new double[nr + 2][nc + 5];
                final int startRow = nr + 2 - rows.length;
                final int startColumn = nc + 5 - columns.length;
                matrix.copySubMatrix(rows, columns, data, startRow, startColumn);
                referenceMatrix.copySubMatrix(rows, columns, expected, startRow, startColumn);
                checkEquality(expected, data, 0., 0.);
            }
        }
    }

    /**
     * Tests the methods that copy parts of a matrix into an array when supplying invalid row/column
     * indices.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain RealMatrix#copySubMatrix(int, int, int, int, double[][])}
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     */
    public static void checkCopySubMatrixInvalidRange1(final RealMatrix matrix) {
        // Dimensions
        final int nr = matrix.getRowDimension();
        final int nc = matrix.getColumnDimension();

        // Destination array
        final double[][] destination = new double[nr][nc];

        // Invalid start row
        try {
            matrix.copySubMatrix(-1, nr - 1, 0, nc - 1, destination);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_ROW_INDEX_FORMAT, -1);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.copySubMatrix(nr, nr - 1, 0, nc - 1, destination);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_ROW_INDEX_FORMAT, nr);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // Invalid end row
        try {
            matrix.copySubMatrix(0, -1, 0, nc - 1, destination);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_ROW_INDEX_FORMAT, -1);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.copySubMatrix(0, nr, 0, nc - 1, destination);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_ROW_INDEX_FORMAT, nr);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // Invalid start column
        try {
            matrix.copySubMatrix(0, nr - 1, -1, nc - 1, destination);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_COLUMN_INDEX_FORMAT, -1);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.copySubMatrix(0, nr - 1, nc, nc - 1, destination);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_COLUMN_INDEX_FORMAT, nc);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // Invalid end column
        try {
            matrix.copySubMatrix(0, nr - 1, 0, -1, destination);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_COLUMN_INDEX_FORMAT, -1);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.copySubMatrix(0, nr - 1, 0, nc, destination);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_COLUMN_INDEX_FORMAT, nc);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the methods that copy parts of a matrix into an array when supplying invalid row/column
     * indices.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain RealMatrix#copySubMatrix(int, int, int, int, double[][], int, int)}<br>
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     */
    public static void checkCopySubMatrixInvalidRange2(final RealMatrix matrix) {
        // Dimensions
        final int nr = matrix.getRowDimension();
        final int nc = matrix.getColumnDimension();

        // Destination array
        final double[][] destination = new double[nr][nc];

        // Invalid start row
        try {
            matrix.copySubMatrix(-1, nr - 1, 0, nc - 1, destination, 0, 0);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_ROW_INDEX_FORMAT, -1);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.copySubMatrix(nr, nr - 1, 0, nc - 1, destination, 0, 0);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_ROW_INDEX_FORMAT, nr);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // Invalid end row
        try {
            matrix.copySubMatrix(0, -1, 0, nc - 1, destination, 0, 0);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_ROW_INDEX_FORMAT, -1);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.copySubMatrix(0, nr, 0, nc - 1, destination, 0, 0);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_ROW_INDEX_FORMAT, nr);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // Invalid start column
        try {
            matrix.copySubMatrix(0, nr - 1, -1, nc - 1, destination, 0, 0);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_COLUMN_INDEX_FORMAT, -1);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.copySubMatrix(0, nr - 1, nc, nc - 1, destination, 0, 0);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_COLUMN_INDEX_FORMAT, nc);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // Invalid end column
        try {
            matrix.copySubMatrix(0, nr - 1, 0, -1, destination, 0, 0);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_COLUMN_INDEX_FORMAT, -1);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.copySubMatrix(0, nr - 1, 0, nc, destination, 0, 0);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_COLUMN_INDEX_FORMAT, nc);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // Invalid destination start row
        try {
            matrix.copySubMatrix(0, nr - 1, 0, nc - 1, destination, -1, 0);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_ROW_INDEX_FORMAT, -1);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.copySubMatrix(0, nr - 1, 0, nc - 1, destination, nr, 0);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_ROW_INDEX_FORMAT, nr);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // Invalid destination start column
        try {
            matrix.copySubMatrix(0, nr - 1, 0, nc - 1, destination, 0, -1);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_COLUMN_INDEX_FORMAT, -1);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.copySubMatrix(0, nr - 1, 0, nc - 1, destination, 0, nc);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_COLUMN_INDEX_FORMAT, nc);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the methods that copy parts of a matrix into an array when supplying invalid row/column
     * index arrays.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain RealMatrix#copySubMatrix(int[], int[], double[][])}<br>
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     */
    public static void checkCopySubMatrixInvalidIndex1(final RealMatrix matrix) {
        // Dimensions
        final int nr = matrix.getRowDimension();
        final int nc = matrix.getColumnDimension();

        // Destination array
        final double[][] destination = new double[nr][nc];

        // Invalid selected row indices
        try {
            final int[] selectedRows = { -1 };
            final int[] selectedColumns = { 0 };
            matrix.copySubMatrix(selectedRows, selectedColumns, destination);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_ROW_INDEX_FORMAT, -1);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            final int[] selectedRows = { nr };
            final int[] selectedColumns = { 0 };
            matrix.copySubMatrix(selectedRows, selectedColumns, destination);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_ROW_INDEX_FORMAT, nr);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // Invalid selected column indices
        try {
            final int[] selectedRows = { 0 };
            final int[] selectedColumns = { -1 };
            matrix.copySubMatrix(selectedRows, selectedColumns, destination);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_COLUMN_INDEX_FORMAT, -1);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            final int[] selectedRows = { 0 };
            final int[] selectedColumns = { nc };
            matrix.copySubMatrix(selectedRows, selectedColumns, destination);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_COLUMN_INDEX_FORMAT, nc);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the methods that copy parts of a matrix into an array when supplying invalid row/column
     * index arrays.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain RealMatrix#copySubMatrix(int[], int[], double[][], int, int)}<br>
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     */
    public static void checkCopySubMatrixInvalidIndex2(final RealMatrix matrix) {
        // Dimensions
        final int nr = matrix.getRowDimension();
        final int nc = matrix.getColumnDimension();

        // Destination array
        final double[][] destination = new double[nr][nc];

        // Invalid selected row indices
        try {
            final int[] selectedRows = { -1 };
            final int[] selectedColumns = { 0 };
            matrix.copySubMatrix(selectedRows, selectedColumns, destination, 0, 0);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_ROW_INDEX_FORMAT, -1);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            final int[] selectedRows = { nr };
            final int[] selectedColumns = { 0 };
            matrix.copySubMatrix(selectedRows, selectedColumns, destination, 0, 0);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_ROW_INDEX_FORMAT, nr);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // Invalid selected column indices
        try {
            final int[] selectedRows = { 0 };
            final int[] selectedColumns = { -1 };
            matrix.copySubMatrix(selectedRows, selectedColumns, destination, 0, 0);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_COLUMN_INDEX_FORMAT, -1);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            final int[] selectedRows = { 0 };
            final int[] selectedColumns = { nc };
            matrix.copySubMatrix(selectedRows, selectedColumns, destination, 0, 0);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_COLUMN_INDEX_FORMAT, nc);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // Invalid destination start row
        try {
            final int[] selectedRows = { 0 };
            final int[] selectedColumns = { 0 };
            matrix.copySubMatrix(selectedRows, selectedColumns, destination, -1, 0);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_ROW_INDEX_FORMAT, -1);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            final int[] selectedRows = { 0 };
            final int[] selectedColumns = { 0 };
            matrix.copySubMatrix(selectedRows, selectedColumns, destination, nr, 0);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_ROW_INDEX_FORMAT, nr);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // Invalid destination start column
        try {
            final int[] selectedRows = { 0 };
            final int[] selectedColumns = { 0 };
            matrix.copySubMatrix(selectedRows, selectedColumns, destination, 0, -1);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_COLUMN_INDEX_FORMAT, -1);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            final int[] selectedRows = { 0 };
            final int[] selectedColumns = { 0 };
            matrix.copySubMatrix(selectedRows, selectedColumns, destination, 0, nc);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_COLUMN_INDEX_FORMAT, nc);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the methods that copy parts of a matrix into an array when supplying a {@code null}
     * destination array.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain RealMatrix#copySubMatrix(int, int, int, int, double[][])}<br>
     * {@linkplain RealMatrix#copySubMatrix(int, int, int, int, double[][], int, int)}<br>
     * {@linkplain RealMatrix#copySubMatrix(int[], int[], double[][])}<br>
     * {@linkplain RealMatrix#copySubMatrix(int[], int[], double[][], int, int)}<br>
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     */
    public static void checkCopySubMatrixNullDestinationArray(final RealMatrix matrix) {
        // Dimensions
        final int nr = matrix.getRowDimension();
        final int nc = matrix.getColumnDimension();

        // Selected rows and columns
        final int[] selectedRows = IntStream.range(0, nr).toArray();
        final int[] selectedColumns = IntStream.range(0, nc).toArray();

        // The destination array is null (by range)
        try {
            final double[][] destination = null;
            matrix.copySubMatrix(0, nr - 1, 0, nc - 1, destination);
            Assert.fail();
        } catch (final NullArgumentException e) {
            final String expectedMessage = String.format(NULL_ARRAY_FORMAT, -1);
            Assert.assertEquals(NullArgumentException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            final double[][] destination = null;
            matrix.copySubMatrix(0, nr - 1, 0, nc - 1, destination, 0, 0);
            Assert.fail();
        } catch (final NullArgumentException e) {
            final String expectedMessage = String.format(NULL_ARRAY_FORMAT, -1);
            Assert.assertEquals(NullArgumentException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // The destination array is null (by indices)
        try {
            final double[][] destination = null;
            matrix.copySubMatrix(selectedRows, selectedColumns, destination);
            Assert.fail();
        } catch (final NullArgumentException e) {
            final String expectedMessage = String.format(NULL_ARRAY_FORMAT, -1);
            Assert.assertEquals(NullArgumentException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {

            final double[][] destination = null;
            matrix.copySubMatrix(selectedRows, selectedColumns, destination, 0, 0);
            Assert.fail();
        } catch (final NullArgumentException e) {
            final String expectedMessage = String.format(NULL_ARRAY_FORMAT, -1);
            Assert.assertEquals(NullArgumentException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // The destination array is not null, but some of its rows are (by range)
        try {
            final double[][] destination = new double[nr][nc];
            destination[nr - 1] = null;
            matrix.copySubMatrix(0, nr - 1, 0, nc - 1, destination);
            Assert.fail();
        } catch (final NullArgumentException e) {
            final String expectedMessage = String.format(NULL_ARRAY_FORMAT, -1);
            Assert.assertEquals(NullArgumentException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            final double[][] destination = new double[nr][nc];
            destination[nr - 1] = null;
            matrix.copySubMatrix(0, nr - 1, 0, nc - 1, destination, 0, 0);
            Assert.fail();
        } catch (final NullArgumentException e) {
            final String expectedMessage = String.format(NULL_ARRAY_FORMAT, -1);
            Assert.assertEquals(NullArgumentException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // The destination array is not null, but some of its rows are (by indices)
        try {
            final double[][] destination = new double[nr][nc];
            destination[nr - 1] = null;
            matrix.copySubMatrix(selectedRows, selectedColumns, destination);
            Assert.fail();
        } catch (final NullArgumentException e) {
            final String expectedMessage = String.format(NULL_ARRAY_FORMAT, -1);
            Assert.assertEquals(NullArgumentException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            final double[][] destination = new double[nr][nc];
            destination[nr - 1] = null;
            matrix.copySubMatrix(selectedRows, selectedColumns, destination, 0, 0);
            Assert.fail();
        } catch (final NullArgumentException e) {
            final String expectedMessage = String.format(NULL_ARRAY_FORMAT, -1);
            Assert.assertEquals(NullArgumentException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the methods that copy parts of a matrix into an array when supplying a destination
     * array with zero rows or columns.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain RealMatrix#copySubMatrix(int, int, int, int, double[][])}<br>
     * {@linkplain RealMatrix#copySubMatrix(int, int, int, int, double[][], int, int)}<br>
     * {@linkplain RealMatrix#copySubMatrix(int[], int[], double[][])}<br>
     * {@linkplain RealMatrix#copySubMatrix(int[], int[], double[][], int, int)}<br>
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     */
    public static void checkCopySubMatrixEmptyDestinationArray(final RealMatrix matrix) {
        // Dimensions
        final int nr = matrix.getRowDimension();
        final int nc = matrix.getColumnDimension();

        // Selected rows and columns
        final int[] selectedRows = { 0 };
        final int[] selectedColumns = { 0 };

        // The length of the destination array is 0 (by range)
        try {
            final double[][] destination = new double[0][0];
            matrix.copySubMatrix(0, nr - 1, 0, nc - 1, destination);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_ROW_INDEX_FORMAT, 0);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            final double[][] destination = new double[0][0];
            matrix.copySubMatrix(0, nr - 1, 0, nc - 1, destination, 0, 0);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_ROW_INDEX_FORMAT, 0);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // The length of the destination array is 0 (by indices)
        try {
            final double[][] destination = new double[0][0];
            matrix.copySubMatrix(selectedRows, selectedColumns, destination);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_ROW_INDEX_FORMAT, 0);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            final double[][] destination = new double[0][0];
            matrix.copySubMatrix(selectedRows, selectedColumns, destination, 0, 0);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_ROW_INDEX_FORMAT, 0);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // The length of the 1st column is zero (by range)
        try {
            final double[][] destination = new double[1][0];
            matrix.copySubMatrix(0, nr - 1, 0, nc - 1, destination);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_COLUMN_INDEX_FORMAT, 0);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            final double[][] destination = new double[1][0];
            matrix.copySubMatrix(0, nr - 1, 0, nc - 1, destination, 0, 0);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_COLUMN_INDEX_FORMAT, 0);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // The length of the 1st column is zero (by indices)
        try {
            final double[][] destination = new double[1][0];
            matrix.copySubMatrix(selectedRows, selectedColumns, destination);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_COLUMN_INDEX_FORMAT, 0);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            final double[][] destination = new double[1][0];
            matrix.copySubMatrix(selectedRows, selectedColumns, destination, 0, 0);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            final String expectedMessage = String.format(OUT_OF_RANGE_COLUMN_INDEX_FORMAT, 0);
            Assert.assertEquals(OutOfRangeException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the methods that copy parts of a matrix into an array when the destination array is too
     * small to store the extracted submatrix.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain RealMatrix#copySubMatrix(int, int, int, int, double[][])}<br>
     * {@linkplain RealMatrix#copySubMatrix(int, int, int, int, double[][], int, int)}<br>
     * {@linkplain RealMatrix#copySubMatrix(int[], int[], double[][])}<br>
     * {@linkplain RealMatrix#copySubMatrix(int[], int[], double[][], int, int)}<br>
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     */
    public static void checkCopySubMatrixIncompatibleDestinationArray(final RealMatrix matrix) {
        // Dimensions
        final int nr = matrix.getRowDimension();
        final int nc = matrix.getColumnDimension();

        // Selected rows and columns
        final int[] selectedRows = IntStream.range(0, nr).toArray();
        final int[] selectedColumns = IntStream.range(0, nc).toArray();

        // The destination array does not have enough rows (by range)
        try {
            final double[][] destination = new double[nr - 1][nc];
            matrix.copySubMatrix(0, nr - 1, 0, nc - 1, destination);
            Assert.fail();
        } catch (final MatrixDimensionMismatchException e) {
            final String expectedMessage = String.format(MATRIX_DIMENSION_MISMATCH_FORMAT, nr - 1,
                    nc, nr, nc);
            Assert.assertEquals(MatrixDimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            final double[][] destination = new double[nr][nc];
            matrix.copySubMatrix(0, nr - 1, 0, nc - 1, destination, 1, 0);
            Assert.fail();
        } catch (final MatrixDimensionMismatchException e) {
            final String expectedMessage = String.format(MATRIX_DIMENSION_MISMATCH_FORMAT, nr - 1,
                    nc, nr, nc);
            Assert.assertEquals(MatrixDimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // The destination array does not have enough rows (by indices)
        try {
            final double[][] destination = new double[nr - 1][nc];
            matrix.copySubMatrix(selectedRows, selectedColumns, destination);
            Assert.fail();
        } catch (final MatrixDimensionMismatchException e) {
            final String expectedMessage = String.format(MATRIX_DIMENSION_MISMATCH_FORMAT, nr - 1,
                    nc, nr, nc);
            Assert.assertEquals(MatrixDimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            final double[][] destination = new double[nr][nc];
            matrix.copySubMatrix(selectedRows, selectedColumns, destination, 1, 0);
            Assert.fail();
        } catch (final MatrixDimensionMismatchException e) {
            final String expectedMessage = String.format(MATRIX_DIMENSION_MISMATCH_FORMAT, nr - 1,
                    nc, nr, nc);
            Assert.assertEquals(MatrixDimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // The destination array does not have enough columns (by range)
        try {
            final double[][] destination = new double[nr][nc - 1];
            matrix.copySubMatrix(0, nr - 1, 0, nc - 1, destination);
            Assert.fail();
        } catch (final MatrixDimensionMismatchException e) {
            final String expectedMessage = String.format(MATRIX_DIMENSION_MISMATCH_FORMAT, nr,
                    nc - 1, nr, nc);
            Assert.assertEquals(MatrixDimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            final double[][] destination = new double[nr][nc];
            matrix.copySubMatrix(0, nr - 1, 0, nc - 1, destination, 0, 1);
            Assert.fail();
        } catch (final MatrixDimensionMismatchException e) {
            final String expectedMessage = String.format(MATRIX_DIMENSION_MISMATCH_FORMAT, nr,
                    nc - 1, nr, nc);
            Assert.assertEquals(MatrixDimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // The destination array does not have enough columns (by indices)
        try {
            final double[][] destination = new double[nr][nc - 1];
            matrix.copySubMatrix(selectedRows, selectedColumns, destination);
            Assert.fail();
        } catch (final MatrixDimensionMismatchException e) {
            final String expectedMessage = String.format(MATRIX_DIMENSION_MISMATCH_FORMAT, nr,
                    nc - 1, nr, nc);
            Assert.assertEquals(MatrixDimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            final double[][] destination = new double[nr][nc];
            matrix.copySubMatrix(selectedRows, selectedColumns, destination, 0, 1);
            Assert.fail();
        } catch (final MatrixDimensionMismatchException e) {
            final String expectedMessage = String.format(MATRIX_DIMENSION_MISMATCH_FORMAT, nr,
                    nc - 1, nr, nc);
            Assert.assertEquals(MatrixDimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // The destination array have enough columns on the first row, but not on some other row (by
        // range)
        try {
            final double[][] destination = new double[nr][nc];
            destination[nr - 1] = new double[nc - 1];
            matrix.copySubMatrix(0, nr - 1, 0, nc - 1, destination);
            Assert.fail();
        } catch (final MatrixDimensionMismatchException e) {
            final String expectedMessage = String.format(MATRIX_DIMENSION_MISMATCH_FORMAT, nr,
                    nc - 1, nr, nc);
            Assert.assertEquals(MatrixDimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            final double[][] destination = new double[nr][nc];
            destination[nr - 1] = new double[nc - 1];
            matrix.copySubMatrix(0, nr - 1, 0, nc - 1, destination, 0, 0);
            Assert.fail();
        } catch (final MatrixDimensionMismatchException e) {
            final String expectedMessage = String.format(MATRIX_DIMENSION_MISMATCH_FORMAT, nr,
                    nc - 1, nr, nc);
            Assert.assertEquals(MatrixDimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // The destination array have enough columns on the first row, but not on some other row (by
        // indices)
        try {
            final double[][] destination = new double[nr][nc];
            destination[nr - 1] = new double[nc - 1];
            matrix.copySubMatrix(selectedRows, selectedColumns, destination);
            Assert.fail();
        } catch (final MatrixDimensionMismatchException e) {
            final String expectedMessage = String.format(MATRIX_DIMENSION_MISMATCH_FORMAT, nr,
                    nc - 1, nr, nc);
            Assert.assertEquals(MatrixDimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            final double[][] destination = new double[nr][nc];
            destination[nr - 1] = new double[nc - 1];
            matrix.copySubMatrix(selectedRows, selectedColumns, destination, 0, 0);
            Assert.fail();
        } catch (final MatrixDimensionMismatchException e) {
            final String expectedMessage = String.format(MATRIX_DIMENSION_MISMATCH_FORMAT, nr,
                    nc - 1, nr, nc);
            Assert.assertEquals(MatrixDimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the method that adds a scalar to the entries of a matrix.
     *
     * <p>
     * The results are tested against the following methods:<br>
     * {@linkplain Array2DRowRealMatrix#scalarAdd(double)}
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     * @param scalar
     *        the scalar to be added to the tested matrix
     * @param expectedClass
     *        the expected class for the matrix yielded by the addition
     * @param absTol
     *        the absolute tolerance to take into account when comparing the computed matrices
     * @param relTol
     *        the relative tolerance to take into account when comparing the computed matrices
     */
    public static void checkScalarAdd(final RealMatrix matrix, final double scalar,
            final Class<? extends RealMatrix> expectedClass, final double absTol,
            final double relTol) {
        // Copy the matrix as a standard real matrix
        final RealMatrix referenceMatrix = new Array2DRowRealMatrix(matrix.getData(), false);

        // Test the addition
        final RealMatrix result = matrix.scalarAdd(scalar);
        final RealMatrix expected = referenceMatrix.scalarAdd(scalar);
        checkEquality(expected, result, absTol, relTol);
        Assert.assertEquals(expectedClass, result.getClass());
        Assert.assertNotSame(matrix, result);
    }

    /**
     * Tests the method that multiplies the entries of a matrix by a scalar.
     *
     * <p>
     * The results are tested against the following methods:<br>
     * {@linkplain Array2DRowRealMatrix#scalarMultiply(double)}
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     * @param scalar
     *        the scalar by which to multiply the tested matrix
     * @param expectedClass
     *        the expected class for the matrix yielded by the multiplication
     * @param absTol
     *        the absolute tolerance to take into account when comparing the computed matrices
     * @param relTol
     *        the relative tolerance to take into account when comparing the computed matrices
     */
    public static void checkScalarMultiply(final RealMatrix matrix, final double scalar,
            final Class<? extends RealMatrix> expectedClass, final double absTol,
            final double relTol) {
        // Copy the matrix as a standard real matrix
        final RealMatrix referenceMatrix = new Array2DRowRealMatrix(matrix.getData(), false);

        // Test the addition
        final RealMatrix result = matrix.scalarMultiply(scalar);
        final RealMatrix expected = referenceMatrix.scalarMultiply(scalar);
        checkEquality(expected, result, absTol, relTol);
        Assert.assertEquals(expectedClass, result.getClass());
        Assert.assertNotSame(matrix, result);
    }

    /**
     * Tests the method that adds a positive scalar to the entries of a positive semi-definite
     * matrix .
     *
     * <p>
     * The results are tested against the following methods:<br>
     * {@linkplain Array2DRowRealMatrix#scalarAdd(double)}
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     * @param scalar
     *        the scalar to be added to the tested matrix
     * @param expectedClass
     *        the expected class for the matrix yielded by the addition
     * @param absTol
     *        the absolute tolerance to take into account when comparing the computed matrices
     * @param relTol
     *        the relative tolerance to take into account when comparing the computed matrices
     */
    public static void checkPositiveScalarAdd(final SymmetricPositiveMatrix matrix,
            final double scalar, final Class<? extends RealMatrix> expectedClass,
            final double absTol, final double relTol) {
        // Copy the matrix as a standard real matrix
        final RealMatrix referenceMatrix = new Array2DRowRealMatrix(matrix.getData(), false);

        // Test the addition
        final SymmetricPositiveMatrix result = matrix.positiveScalarAdd(scalar);
        final RealMatrix expected = referenceMatrix.scalarAdd(scalar);
        checkEquality(expected, result, absTol, relTol);
        Assert.assertEquals(expectedClass, result.getClass());
        Assert.assertNotSame(matrix, result);
    }

    /**
     * Tests the method that multiplies the entries of a symmetric positive semi-definite matrix by
     * a positive scalar.
     *
     * <p>
     * The results are tested against the following methods:<br>
     * {@linkplain Array2DRowRealMatrix#scalarMultiply(double)}
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     * @param scalar
     *        the scalar by which to multiply the tested matrix
     * @param expectedClass
     *        the expected class for the matrix yielded by the multiplication
     * @param absTol
     *        the absolute tolerance to take into account when comparing the computed matrices
     * @param relTol
     *        the relative tolerance to take into account when comparing the computed matrices
     */
    public static void checkPositiveScalarMultiply(final SymmetricPositiveMatrix matrix,
            final double scalar, final Class<? extends RealMatrix> expectedClass,
            final double absTol, final double relTol) {
        // Copy the matrix as a standard real matrix
        final RealMatrix referenceMatrix = new Array2DRowRealMatrix(matrix.getData(), false);

        // Test the addition
        final SymmetricPositiveMatrix result = matrix.positiveScalarMultiply(scalar);
        final RealMatrix expected = referenceMatrix.scalarMultiply(scalar);
        checkEquality(expected, result, absTol, relTol);
        Assert.assertEquals(expectedClass, result.getClass());
        Assert.assertNotSame(matrix, result);
    }

    /**
     * Tests the method that adds a matrix to another matrix.
     *
     * <p>
     * The results are tested against the following methods:<br>
     * {@linkplain Array2DRowRealMatrix#add(RealMatrix)}
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     * @param other
     *        the matrix to be added to the tested matrix
     * @param expectedClass
     *        the expected class for the matrix yielded by the addition
     * @param absTol
     *        the absolute tolerance to take into account when comparing the computed matrices
     * @param relTol
     *        the relative tolerance to take into account when comparing the computed matrices
     */
    public static void checkAddMatrix(final RealMatrix matrix, final RealMatrix other,
            final Class<? extends RealMatrix> expectedClass, final double absTol,
            final double relTol) {
        // Copy the matrix as a standard real matrix
        final RealMatrix referenceMatrix = new Array2DRowRealMatrix(matrix.getData(), false);

        // Test the addition
        final RealMatrix result = matrix.add(other);
        final RealMatrix expected = referenceMatrix.add(other);
        checkEquality(expected, result, absTol, relTol);
        Assert.assertEquals(expectedClass, result.getClass());
        Assert.assertNotSame(matrix, result);
    }

    /**
     * Tests the method that subtracts a matrix from another matrix.
     *
     * <p>
     * The results are tested against the following methods:<br>
     * {@linkplain Array2DRowRealMatrix#subtract(RealMatrix)}
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     * @param other
     *        the matrix to be subtracted from the tested matrix
     * @param expectedClass
     *        the expected class for the matrix yielded by the subtraction
     * @param absTol
     *        the absolute tolerance to take into account when comparing the computed matrices
     * @param relTol
     *        the relative tolerance to take into account when comparing the computed matrices
     */
    public static void checkSubtractMatrix(final RealMatrix matrix, final RealMatrix other,
            final Class<? extends RealMatrix> expectedClass, final double absTol,
            final double relTol) {
        // Copy the matrix as a standard real matrix
        final RealMatrix referenceMatrix = new Array2DRowRealMatrix(matrix.getData(), false);

        // Test the subtraction
        final RealMatrix result = matrix.subtract(other);
        final RealMatrix expected = referenceMatrix.subtract(other);
        checkEquality(expected, result, absTol, relTol);
        Assert.assertEquals(expectedClass, result.getClass());
        Assert.assertNotSame(matrix, result);
    }

    /**
     * Tests the methods that multiply a matrix by another matrix.
     *
     * <p>
     * The results are tested against the following methods:<br>
     * {@linkplain Array2DRowRealMatrix#multiply(RealMatrix)}<br>
     * {@linkplain Array2DRowRealMatrix#multiply(RealMatrix, boolean)}<br>
     * {@linkplain Array2DRowRealMatrix#multiply(RealMatrix, boolean, double)}<br>
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     * @param other
     *        the matrix by which to multiply the tested matrix
     * @param scalingFactor
     *        the scaling factor to apply to the product of the two matrices
     * @param expectedClass
     *        the expected class for the matrix yielded by the multiplication
     * @param absTol
     *        the absolute tolerance to take into account when comparing the computed matrices
     * @param relTol
     *        the relative tolerance to take into account when comparing the computed matrices
     */
    public static void checkMultiplyMatrix(final RealMatrix matrix, final RealMatrix other,
            final double scalingFactor, final Class<? extends RealMatrix> expectedClass,
            final double absTol, final double relTol) {
        RealMatrix result;
        RealMatrix expected;

        // Copy the matrix as a standard real matrix
        final RealMatrix referenceMatrix = new Array2DRowRealMatrix(matrix.getData(), false);

        // Transpose of the matrix by which to multiply the tested matrix
        final RealMatrix transpose = other.transpose();

        // Test the multiplication
        result = matrix.multiply(other);
        expected = referenceMatrix.multiply(other);
        checkEquality(expected, result, absTol, relTol);
        Assert.assertEquals(expectedClass, result.getClass());
        Assert.assertNotSame(matrix, result);

        result = matrix.multiply(other, false);
        expected = referenceMatrix.multiply(other, false);
        checkEquality(expected, result, absTol, relTol);
        Assert.assertEquals(expectedClass, result.getClass());
        Assert.assertNotSame(matrix, result);

        result = matrix.multiply(other, false, 1.0);
        expected = referenceMatrix.multiply(other, false, 1.0);
        checkEquality(expected, result, absTol, relTol);
        Assert.assertEquals(expectedClass, result.getClass());
        Assert.assertNotSame(matrix, result);

        // Test the multiplication by the transpose
        result = matrix.multiply(transpose, true);
        expected = referenceMatrix.multiply(transpose, true);
        checkEquality(expected, result, absTol, relTol);
        Assert.assertEquals(expectedClass, result.getClass());
        Assert.assertNotSame(matrix, result);

        result = matrix.multiply(transpose, true, 1.0);
        expected = referenceMatrix.multiply(transpose, true, 1.0);
        checkEquality(expected, result, absTol, relTol);
        Assert.assertEquals(expectedClass, result.getClass());
        Assert.assertNotSame(matrix, result);

        // Test the multiplication by a scalar
        result = matrix.multiply(other, false, scalingFactor);
        expected = referenceMatrix.multiply(other, false, scalingFactor);
        checkEquality(expected, result, absTol, relTol);
        Assert.assertEquals(expectedClass, result.getClass());
        Assert.assertNotSame(matrix, result);

        result = matrix.multiply(transpose, true, scalingFactor);
        expected = referenceMatrix.multiply(transpose, true, scalingFactor);
        checkEquality(expected, result, absTol, relTol);
        Assert.assertEquals(expectedClass, result.getClass());
        Assert.assertNotSame(matrix, result);
    }

    /**
     * Tests the method that premultiplies a matrix by another matrix.
     *
     * <p>
     * The results are tested against the following method:<br>
     * {@linkplain Array2DRowRealMatrix#preMultiply(RealMatrix)}<br>
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     * @param other
     *        the matrix by which to multiply the tested matrix
     * @param expectedClass
     *        the expected class for the matrix yielded by the multiplication
     * @param absTol
     *        the absolute tolerance to take into account when comparing the computed matrices
     * @param relTol
     *        the relative tolerance to take into account when comparing the computed matrices
     */
    public static void checkPreMultiplyMatrix(final RealMatrix matrix, final RealMatrix other,
            final Class<? extends RealMatrix> expectedClass, final double absTol,
            final double relTol) {
        // Copy the matrix as a standard real matrix
        final RealMatrix referenceMatrix = new Array2DRowRealMatrix(matrix.getData(), false);

        // Test the premultiplication
        final RealMatrix result = matrix.preMultiply(other);
        final RealMatrix expected = referenceMatrix.preMultiply(other);
        checkEquality(expected, result, absTol, relTol);
        Assert.assertEquals(expectedClass, result.getClass());
        Assert.assertNotSame(matrix, result);
    }

    /**
     * Tests the methods that multiply a matrix by a vector.
     *
     * <p>
     * The results are tested against the following methods:<br>
     * {@linkplain Array2DRowRealMatrix#operate(double[])}<br>
     * {@linkplain Array2DRowRealMatrix#operate(RealVector)}<br>
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     * @param vector
     *        the vector to multiply the tested matrix by
     * @param absTol
     *        the absolute tolerance to take into account when comparing the computed vectors
     * @param relTol
     *        the relative tolerance to take into account when comparing the computed vectors
     */
    public static void checkOperateVector(final RealMatrix matrix, final RealVector vector,
            final double absTol, final double relTol) {
        // Copy the matrix as a standard real matrix
        final RealMatrix referenceMatrix = new Array2DRowRealMatrix(matrix.getData(), false);

        // Test the multiplication by a vector
        final double[] vectorArray = vector.toArray();
        final double[] resultArray = matrix.operate(vectorArray);
        final double[] expectedArray = referenceMatrix.operate(vectorArray);
        checkEquality(expectedArray, resultArray, absTol, relTol);

        final RealVector result = matrix.operate(vector);
        final RealVector expected = referenceMatrix.operate(vector);
        checkEquality(expected, result, absTol, relTol);
    }

    /**
     * Tests the methods that premultiply a matrix by a vector.
     *
     * <p>
     * The results are tested against the following methods:<br>
     * {@linkplain Array2DRowRealMatrix#preMultiply(double[])}<br>
     * {@linkplain Array2DRowRealMatrix#preMultiply(RealVector)}<br>
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     * @param vector
     *        the vector to premultiply the tested matrix by
     * @param absTol
     *        the absolute tolerance to take into account when comparing the computed vectors
     * @param relTol
     *        the relative tolerance to take into account when comparing the computed vectors
     */
    public static void checkPreMultiplyVector(final RealMatrix matrix, final RealVector vector,
            final double absTol, final double relTol) {
        // Copy the matrix as a standard real matrix
        final RealMatrix referenceMatrix = new Array2DRowRealMatrix(matrix.getData(), false);

        // Test the multiplication by a vector
        final double[] vectorArray = vector.toArray();
        final double[] resultArray = matrix.preMultiply(vectorArray);
        final double[] expectedArray = referenceMatrix.preMultiply(vectorArray);
        checkEquality(expectedArray, resultArray, absTol, relTol);

        final RealVector result = matrix.preMultiply(vector);
        final RealVector expected = referenceMatrix.preMultiply(vector);
        checkEquality(expected, result, absTol, relTol);
    }

    /**
     * Tests the methods that perform the quadratic multiplication M&times;S&times;M<sup>T</sup> of
     * a symmetric matrix S by a matrix M.
     *
     * @param matrix
     *        the matrix S
     * @param other
     *        the matrix M
     * @param expectedClass
     *        the expected class for the matrix yielded by the quadratic multiplication
     * @param absTol
     *        the absolute tolerance to take into account when comparing the computed matrices
     * @param relTol
     *        the relative tolerance to take into account when comparing the computed matrices
     */
    public static void checkQuadraticMultiplication(final SymmetricMatrix matrix,
            final RealMatrix other, final Class<? extends RealMatrix> expectedClass,
            final double absTol, final double relTol) {
        RealMatrix result;

        // Copy the matrix as a standard real matrix
        final RealMatrix referenceMatrix = new Array2DRowRealMatrix(matrix.getData(), false);

        // Transpose of the matrix by which to multiply the tested matrix
        final RealMatrix transpose = other.transpose();

        // Expected result
        final RealMatrix expected = referenceMatrix.multiply(other, true).preMultiply(other);

        result = matrix.quadraticMultiplication(other);
        checkEquality(expected, result, absTol, relTol);
        Assert.assertEquals(expectedClass, result.getClass());
        Assert.assertNotSame(matrix, result);

        result = matrix.quadraticMultiplication(other, false);
        checkEquality(expected, result, absTol, relTol);
        Assert.assertEquals(expectedClass, result.getClass());
        Assert.assertNotSame(matrix, result);

        result = matrix.quadraticMultiplication(transpose, true);
        checkEquality(expected, result, absTol, relTol);
        Assert.assertEquals(expectedClass, result.getClass());
        Assert.assertNotSame(matrix, result);
    }

    /**
     * Tests the method that raises a matrix to the power of p.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain RealMatrix#power(int)}
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     * @param minEponent
     *        the minimum exponent to be tested
     * @param maxEponent
     *        the maximum exponent be be tested
     * @param expectedClass
     *        the expected class for the matrix yield by the power method
     * @param absTol
     *        the absolute tolerance to take into account when comparing the computed matrices
     * @param relTol
     *        the relative tolerance to take into account when comparing the computed matrices
     */
    public static void checkPower(final RealMatrix matrix, final int minEponent,
            final int maxEponent, final Class<? extends RealMatrix> expectedClass,
            final double absTol, final double relTol) {
        // Copy the matrix as a standard real matrix
        final RealMatrix referenceMatrix = new Array2DRowRealMatrix(matrix.getData(), false);

        // Positive exponents (including 0 and 1)
        for (int p = minEponent; p <= maxEponent; p++) {
            final RealMatrix result = matrix.power(p);
            final RealMatrix expected = referenceMatrix.power(p);
            CheckUtils.checkEquality(expected, result, absTol, relTol);
            Assert.assertNotSame(matrix, result);
            Assert.assertEquals(expectedClass, result.getClass());
        }
    }

    /**
     * Tests the method that raises a matrix to the power of p, using a negative exponent.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain RealMatrix#power(int)}<br>
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     */
    public static void checkPowerNegativeExponent(final RealMatrix matrix) {
        try {
            matrix.power(-1);
            Assert.fail();
        } catch (final NotPositiveException e) {
            Assert.assertEquals(NotPositiveException.class, e.getClass());
            Assert.assertEquals("invalid exponent -1 (must be positive)", e.getMessage());
        }
    }

    /**
     * Tests the method that raises a matrix to the power of p, using a non-square matrix.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain RealMatrix#power(int)}<br>
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     */
    public static void checkPowerNonSquareMatrix(final RealMatrix matrix) {
        try {
            matrix.power(+1);
            Assert.fail();
        } catch (final NonSquareMatrixException e) {
            final String expectedMessage = String.format(NON_SQUARE_MATRIX_FORMAT,
                    matrix.getRowDimension(), matrix.getColumnDimension());
            Assert.assertEquals(NonSquareMatrixException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the method that adds a matrix to another matrix, using standard real matrices which are
     * not addition compatible.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain RealMatrix#add(RealMatrix)}
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     */
    public static void checkAddIncompatibleRealMatrix(final RealMatrix matrix) {
        // Dimensions
        final int nr = matrix.getRowDimension();
        final int nc = matrix.getColumnDimension();

        // Row dimension is too low
        try {
            matrix.add(new Array2DRowRealMatrix(nr - 1, nc));
            Assert.fail();
        } catch (final MatrixDimensionMismatchException e) {
            final String expectedMessage = String.format(MATRIX_DIMENSION_MISMATCH_FORMAT, nr - 1,
                    nc, nr, nc);
            Assert.assertEquals(MatrixDimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // Column dimension is too low
        try {
            matrix.add(new Array2DRowRealMatrix(nr, nc - 1));
            Assert.fail();
        } catch (final MatrixDimensionMismatchException e) {
            final String expectedMessage = String.format(MATRIX_DIMENSION_MISMATCH_FORMAT, nr,
                    nc - 1, nr, nc);
            Assert.assertEquals(MatrixDimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // Row dimension is too large
        try {
            matrix.add(new Array2DRowRealMatrix(nr + 1, nc));
            Assert.fail();
        } catch (final MatrixDimensionMismatchException e) {
            final String expectedMessage = String.format(MATRIX_DIMENSION_MISMATCH_FORMAT, nr + 1,
                    nc, nr, nc);
            Assert.assertEquals(MatrixDimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // Column dimension is too large
        try {
            matrix.add(new Array2DRowRealMatrix(nr, nc + 1));
            Assert.fail();
        } catch (final MatrixDimensionMismatchException e) {
            final String expectedMessage = String.format(MATRIX_DIMENSION_MISMATCH_FORMAT, nr,
                    nc + 1, nr, nc);
            Assert.assertEquals(MatrixDimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the method that adds a matrix to another matrix, using standard symmetric matrices
     * which are not addition compatible.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain RealMatrix#add(RealMatrix)}
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     */
    public static void checkAddIncompatibleSymmetricMatrix(final RealMatrix matrix) {
        // Dimensions
        final int nr = matrix.getRowDimension();
        final int nc = matrix.getColumnDimension();

        // Row/Column dimensions are too low
        try {
            matrix.add(new ArrayRowSymmetricMatrix(nr - 1));
            Assert.fail();
        } catch (final MatrixDimensionMismatchException e) {
            final String expectedMessage = String.format(MATRIX_DIMENSION_MISMATCH_FORMAT, nr - 1,
                    nr - 1, nr, nc);
            Assert.assertEquals(MatrixDimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // Row/Column dimensions are too large
        try {
            matrix.add(new ArrayRowSymmetricMatrix(nr + 1));
            Assert.fail();
        } catch (final MatrixDimensionMismatchException e) {
            final String expectedMessage = String.format(MATRIX_DIMENSION_MISMATCH_FORMAT, nr + 1,
                    nr + 1, nr, nc);
            Assert.assertEquals(MatrixDimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the method that adds a matrix to another matrix, using standard symmetric positive
     * semi-definite matrices which are not
     * addition compatible.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain RealMatrix#add(RealMatrix)}
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     */
    public static void checkAddIncompatibleSymmetricPositiveMatrix(final RealMatrix matrix) {
        // Dimensions
        final int nr = matrix.getRowDimension();
        final int nc = matrix.getColumnDimension();

        // Row/Column dimensions are too low
        try {
            matrix.add(new ArrayRowSymmetricPositiveMatrix(nr - 1));
            Assert.fail();
        } catch (final MatrixDimensionMismatchException e) {
            final String expectedMessage = String.format(MATRIX_DIMENSION_MISMATCH_FORMAT, nr - 1,
                    nr - 1, nr, nc);
            Assert.assertEquals(MatrixDimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // Row/Column dimensions are too large
        try {
            matrix.add(new ArrayRowSymmetricPositiveMatrix(nr + 1));
            Assert.fail();
        } catch (final MatrixDimensionMismatchException e) {
            final String expectedMessage = String.format(MATRIX_DIMENSION_MISMATCH_FORMAT, nr + 1,
                    nr + 1, nr, nc);
            Assert.assertEquals(MatrixDimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the method that adds a matrix to another matrix, using decomposed symmetric positive
     * semi-definite matrices which are not
     * addition compatible.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain RealMatrix#add(RealMatrix)}
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     */
    public static void checkAddIncompatibleDecomposedMatrix(final RealMatrix matrix) {
        // Dimensions
        final int nr = matrix.getRowDimension();
        final int nc = matrix.getColumnDimension();

        // Row/Column dimensions are too low
        try {
            matrix.add(new DecomposedSymmetricPositiveMatrix(nr - 1));
            Assert.fail();
        } catch (final MatrixDimensionMismatchException e) {
            final String expectedMessage = String.format(MATRIX_DIMENSION_MISMATCH_FORMAT, nr - 1,
                    nr - 1, nr, nc);
            Assert.assertEquals(MatrixDimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // Row/Column dimensions are too large
        try {
            matrix.add(new DecomposedSymmetricPositiveMatrix(nr + 1));
            Assert.fail();
        } catch (final MatrixDimensionMismatchException e) {
            final String expectedMessage = String.format(MATRIX_DIMENSION_MISMATCH_FORMAT, nr + 1,
                    nr + 1, nr, nc);
            Assert.assertEquals(MatrixDimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the method that adds a matrix to another matrix, using diagonal matrices which are not
     * addition compatible.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain RealMatrix#add(RealMatrix)}
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     */
    public static void checkAddIncompatibleDiagonalMatrix(final RealMatrix matrix) {
        // Dimensions
        final int nr = matrix.getRowDimension();
        final int nc = matrix.getColumnDimension();

        // Row/Column dimensions are too low
        try {
            matrix.add(new DiagonalMatrix(nr - 1));
            Assert.fail();
        } catch (final MatrixDimensionMismatchException e) {
            final String expectedMessage = String.format(MATRIX_DIMENSION_MISMATCH_FORMAT, nr - 1,
                    nr - 1, nr, nc);
            Assert.assertEquals(MatrixDimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // Row/Column dimensions are too large
        try {
            matrix.add(new DiagonalMatrix(nr + 1));
            Assert.fail();
        } catch (final MatrixDimensionMismatchException e) {
            final String expectedMessage = String.format(MATRIX_DIMENSION_MISMATCH_FORMAT, nr + 1,
                    nr + 1, nr, nc);
            Assert.assertEquals(MatrixDimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the method that subtracts a matrix from another matrix, using standard real matrices
     * which are not subtraction compatible.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain RealMatrix#subtract(RealMatrix)}
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     */
    public static void checkSubtractIncompatibleRealMatrix(final RealMatrix matrix) {
        // Dimensions
        final int nr = matrix.getRowDimension();
        final int nc = matrix.getColumnDimension();

        // Row dimension is too low
        try {
            matrix.subtract(new Array2DRowRealMatrix(nr - 1, nc));
            Assert.fail();
        } catch (final MatrixDimensionMismatchException e) {
            final String expectedMessage = String.format(MATRIX_DIMENSION_MISMATCH_FORMAT, nr - 1,
                    nc, nr, nc);
            Assert.assertEquals(MatrixDimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // Column dimension is too low
        try {
            matrix.subtract(new Array2DRowRealMatrix(nr, nc - 1));
            Assert.fail();
        } catch (final MatrixDimensionMismatchException e) {
            final String expectedMessage = String.format(MATRIX_DIMENSION_MISMATCH_FORMAT, nr,
                    nc - 1, nr, nc);
            Assert.assertEquals(MatrixDimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // Row dimension is too large
        try {
            matrix.subtract(new Array2DRowRealMatrix(nr + 1, nc));
            Assert.fail();
        } catch (final MatrixDimensionMismatchException e) {
            final String expectedMessage = String.format(MATRIX_DIMENSION_MISMATCH_FORMAT, nr + 1,
                    nc, nr, nc);
            Assert.assertEquals(MatrixDimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // Column dimension is too large
        try {
            matrix.subtract(new Array2DRowRealMatrix(nr, nc + 1));
            Assert.fail();
        } catch (final MatrixDimensionMismatchException e) {
            final String expectedMessage = String.format(MATRIX_DIMENSION_MISMATCH_FORMAT, nr,
                    nc + 1, nr, nc);
            Assert.assertEquals(MatrixDimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the method that subtracts a matrix from another matrix, using standard symmetric
     * matrices which are not subtraction compatible.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain RealMatrix#subtract(RealMatrix)}
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     */
    public static void checkSubtractIncompatibleSymmetricMatrix(final RealMatrix matrix) {
        // Dimensions
        final int nr = matrix.getRowDimension();
        final int nc = matrix.getColumnDimension();

        // Row/Column dimensions are too low
        try {
            matrix.subtract(new ArrayRowSymmetricMatrix(nr - 1));
            Assert.fail();
        } catch (final MatrixDimensionMismatchException e) {
            final String expectedMessage = String.format(MATRIX_DIMENSION_MISMATCH_FORMAT, nr - 1,
                    nr - 1, nr, nc);
            Assert.assertEquals(MatrixDimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // Row/Column dimensions are too large
        try {
            matrix.subtract(new ArrayRowSymmetricMatrix(nr + 1));
            Assert.fail();
        } catch (final MatrixDimensionMismatchException e) {
            final String expectedMessage = String.format(MATRIX_DIMENSION_MISMATCH_FORMAT, nr + 1,
                    nr + 1, nr, nc);
            Assert.assertEquals(MatrixDimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the method that subtracts a matrix from another matrix, using standard symmetric
     * positive semi-definite matrices which are not
     * subtraction compatible.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain RealMatrix#subtract(RealMatrix)}
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     */
    public static void checkSubtractIncompatibleSymmetricPositiveMatrix(final RealMatrix matrix) {
        // Dimensions
        final int nr = matrix.getRowDimension();
        final int nc = matrix.getColumnDimension();

        // Row/Column dimensions are too low
        try {
            matrix.subtract(new ArrayRowSymmetricPositiveMatrix(nr - 1));
            Assert.fail();
        } catch (final MatrixDimensionMismatchException e) {
            final String expectedMessage = String.format(MATRIX_DIMENSION_MISMATCH_FORMAT, nr - 1,
                    nr - 1, nr, nc);
            Assert.assertEquals(MatrixDimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // Row/Column dimensions are too large
        try {
            matrix.subtract(new ArrayRowSymmetricPositiveMatrix(nr + 1));
            Assert.fail();
        } catch (final MatrixDimensionMismatchException e) {
            final String expectedMessage = String.format(MATRIX_DIMENSION_MISMATCH_FORMAT, nr + 1,
                    nr + 1, nr, nc);
            Assert.assertEquals(MatrixDimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the method that subtracts a matrix from another matrix, using decomposed symmetric
     * positive semi-definite matrices which are
     * not subtraction compatible.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain RealMatrix#subtract(RealMatrix)}
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     */
    public static void checkSubtractIncompatibleDecomposedMatrix(final RealMatrix matrix) {
        // Dimensions
        final int nr = matrix.getRowDimension();
        final int nc = matrix.getColumnDimension();

        // Row/Column dimensions are too low
        try {
            matrix.subtract(new DecomposedSymmetricPositiveMatrix(nr - 1));
            Assert.fail();
        } catch (final MatrixDimensionMismatchException e) {
            final String expectedMessage = String.format(MATRIX_DIMENSION_MISMATCH_FORMAT, nr - 1,
                    nr - 1, nr, nc);
            Assert.assertEquals(MatrixDimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // Row/Column dimensions are too large
        try {
            matrix.subtract(new DecomposedSymmetricPositiveMatrix(nr + 1));
            Assert.fail();
        } catch (final MatrixDimensionMismatchException e) {
            final String expectedMessage = String.format(MATRIX_DIMENSION_MISMATCH_FORMAT, nr + 1,
                    nr + 1, nr, nc);
            Assert.assertEquals(MatrixDimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the method that subtracts a matrix from another matrix, using diagonal matrices which
     * are not subtraction compatible.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain RealMatrix#subtract(RealMatrix)}
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     */
    public static void checkSubtractIncompatibleDiagonalMatrix(final RealMatrix matrix) {
        // Dimensions
        final int nr = matrix.getRowDimension();
        final int nc = matrix.getColumnDimension();

        // Row/Column dimensions are too low
        try {
            matrix.subtract(new DiagonalMatrix(nr - 1));
            Assert.fail();
        } catch (final MatrixDimensionMismatchException e) {
            final String expectedMessage = String.format(MATRIX_DIMENSION_MISMATCH_FORMAT, nr - 1,
                    nr - 1, nr, nc);
            Assert.assertEquals(MatrixDimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // Row/Column dimensions are too large
        try {
            matrix.subtract(new DiagonalMatrix(nr + 1));
            Assert.fail();
        } catch (final MatrixDimensionMismatchException e) {
            final String expectedMessage = String.format(MATRIX_DIMENSION_MISMATCH_FORMAT, nr + 1,
                    nr + 1, nr, nc);
            Assert.assertEquals(MatrixDimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the methods that multiply a matrix by another matrix, using standard real matrices
     * which are not multiplication compatible.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain RealMatrix#multiply(RealMatrix)}<br>
     * {@linkplain RealMatrix#multiply(RealMatrix, boolean)}<br>
     * {@linkplain RealMatrix#multiply(RealMatrix, boolean, double)}<br>
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     */
    public static void checkMultiplyIncompatibleRealMatrix(final RealMatrix matrix) {
        // Dimensions
        final int nr = matrix.getRowDimension();
        final int nc = matrix.getColumnDimension();

        // Matching dimension is too low
        try {
            matrix.multiply(new Array2DRowRealMatrix(nc - 1, nr));
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nc - 1, nc);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.multiply(new Array2DRowRealMatrix(nc - 1, nr), false);
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nc - 1, nc);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.multiply(new Array2DRowRealMatrix(nr, nc - 1), true);
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nc - 1, nc);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.multiply(new Array2DRowRealMatrix(nc - 1, nr), false, 1.0);
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nc - 1, nc);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.multiply(new Array2DRowRealMatrix(nr, nc - 1), true, 1.0);
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nc - 1, nc);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // Matching dimension is too large
        try {
            matrix.multiply(new Array2DRowRealMatrix(nc + 1, nc));
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nc + 1, nc);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.multiply(new Array2DRowRealMatrix(nc + 1, nr), false);
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nc + 1, nc);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.multiply(new Array2DRowRealMatrix(nr, nc + 1), true);
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nc + 1, nc);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.multiply(new Array2DRowRealMatrix(nc + 1, nr), false, 1.0);
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nc + 1, nc);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.multiply(new Array2DRowRealMatrix(nr, nc + 1), true, 1.0);
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nc + 1, nc);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the methods that multiply a matrix by another matrix, using standard symmetric matrices
     * which are not multiplication
     * compatible.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain RealMatrix#multiply(RealMatrix)}<br>
     * {@linkplain RealMatrix#multiply(RealMatrix, boolean)}<br>
     * {@linkplain RealMatrix#multiply(RealMatrix, boolean, double)}<br>
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     */
    public static void checkMultiplyIncompatibleSymmetricMatrix(final RealMatrix matrix) {
        // Column dimension
        final int nc = matrix.getColumnDimension();

        // Matching dimension is too low
        try {
            matrix.multiply(new ArrayRowSymmetricMatrix(nc - 1));
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nc - 1, nc);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.multiply(new ArrayRowSymmetricMatrix(nc - 1), false);
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nc - 1, nc);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.multiply(new ArrayRowSymmetricMatrix(nc - 1), true);
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nc - 1, nc);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.multiply(new ArrayRowSymmetricMatrix(nc - 1), false, 1.0);
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nc - 1, nc);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.multiply(new ArrayRowSymmetricMatrix(nc - 1), true, 1.0);
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nc - 1, nc);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // Matching dimension is too large
        try {
            matrix.multiply(new ArrayRowSymmetricMatrix(nc + 1));
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nc + 1, nc);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.multiply(new ArrayRowSymmetricMatrix(nc + 1), false);
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nc + 1, nc);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.multiply(new ArrayRowSymmetricMatrix(nc + 1), true);
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nc + 1, nc);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.multiply(new ArrayRowSymmetricMatrix(nc + 1), false, 1.0);
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nc + 1, nc);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.multiply(new ArrayRowSymmetricMatrix(nc + 1), true, 1.0);
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nc + 1, nc);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the methods that multiply a matrix by another matrix, using standard symmetric positive
     * semi-definite matrices which are not
     * multiplication compatible.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain RealMatrix#multiply(RealMatrix)}<br>
     * {@linkplain RealMatrix#multiply(RealMatrix, boolean)}<br>
     * {@linkplain RealMatrix#multiply(RealMatrix, boolean, double)}<br>
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     */
    public static void checkMultiplyIncompatibleSymmetricPositiveMatrix(final RealMatrix matrix) {
        // Column dimension
        final int nc = matrix.getColumnDimension();

        // Matching dimension is too low
        try {
            matrix.multiply(new ArrayRowSymmetricPositiveMatrix(nc - 1));
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nc - 1, nc);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.multiply(new ArrayRowSymmetricPositiveMatrix(nc - 1), false);
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nc - 1, nc);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.multiply(new ArrayRowSymmetricPositiveMatrix(nc - 1), true);
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nc - 1, nc);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.multiply(new ArrayRowSymmetricPositiveMatrix(nc - 1), false, 1.0);
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nc - 1, nc);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.multiply(new ArrayRowSymmetricPositiveMatrix(nc - 1), true, 1.0);
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nc - 1, nc);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // Matching dimension is too large
        try {
            matrix.multiply(new ArrayRowSymmetricPositiveMatrix(nc + 1));
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nc + 1, nc);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.multiply(new ArrayRowSymmetricPositiveMatrix(nc + 1), false);
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nc + 1, nc);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.multiply(new ArrayRowSymmetricPositiveMatrix(nc + 1), true);
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nc + 1, nc);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.multiply(new ArrayRowSymmetricPositiveMatrix(nc + 1), false, 1.0);
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nc + 1, nc);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.multiply(new ArrayRowSymmetricPositiveMatrix(nc + 1), true, 1.0);
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nc + 1, nc);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the methods that multiply a matrix by another matrix, using decomposed symmetric
     * positive semi-definite matrices which are not
     * multiplication compatible.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain RealMatrix#multiply(RealMatrix)}<br>
     * {@linkplain RealMatrix#multiply(RealMatrix, boolean)}<br>
     * {@linkplain RealMatrix#multiply(RealMatrix, boolean, double)}<br>
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     */
    public static void checkMultiplyIncompatibleDecomposedMatrix(final RealMatrix matrix) {
        // Column dimension
        final int nc = matrix.getColumnDimension();

        // Matching dimension is too low
        try {
            matrix.multiply(new DecomposedSymmetricPositiveMatrix(nc - 1));
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nc - 1, nc);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.multiply(new DecomposedSymmetricPositiveMatrix(nc - 1), false);
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nc - 1, nc);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.multiply(new DecomposedSymmetricPositiveMatrix(nc - 1), true);
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nc - 1, nc);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.multiply(new DecomposedSymmetricPositiveMatrix(nc - 1), false, 1.0);
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nc - 1, nc);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.multiply(new DecomposedSymmetricPositiveMatrix(nc - 1), true, 1.0);
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nc - 1, nc);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // Matching dimension is too large
        try {
            matrix.multiply(new DecomposedSymmetricPositiveMatrix(nc + 1));
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nc + 1, nc);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.multiply(new DecomposedSymmetricPositiveMatrix(nc + 1), false);
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nc + 1, nc);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.multiply(new DecomposedSymmetricPositiveMatrix(nc + 1), true);
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nc + 1, nc);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.multiply(new DecomposedSymmetricPositiveMatrix(nc + 1), false, 1.0);
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nc + 1, nc);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.multiply(new DecomposedSymmetricPositiveMatrix(nc + 1), true, 1.0);
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nc + 1, nc);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the methods that multiply a matrix by another matrix, using diagonal matrices which are
     * not multiplication compatible.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain RealMatrix#multiply(RealMatrix)}<br>
     * {@linkplain RealMatrix#multiply(RealMatrix, boolean)}<br>
     * {@linkplain RealMatrix#multiply(RealMatrix, boolean, double)}<br>
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     */
    public static void checkMultiplyIncompatibleDiagonalMatrix(final RealMatrix matrix) {
        // Column dimension
        final int nc = matrix.getColumnDimension();

        // Matching dimension is too low
        try {
            matrix.multiply(new DiagonalMatrix(nc - 1));
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nc - 1, nc);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.multiply(new DiagonalMatrix(nc - 1), false);
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nc - 1, nc);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.multiply(new DiagonalMatrix(nc - 1), true);
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nc - 1, nc);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.multiply(new DiagonalMatrix(nc - 1), false, 1.0);
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nc - 1, nc);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.multiply(new DiagonalMatrix(nc - 1), true, 1.0);
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nc - 1, nc);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // Matching dimension is too large
        try {
            matrix.multiply(new DiagonalMatrix(nc + 1));
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nc + 1, nc);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.multiply(new DiagonalMatrix(nc + 1), false);
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nc + 1, nc);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.multiply(new DiagonalMatrix(nc + 1), true);
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nc + 1, nc);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.multiply(new DiagonalMatrix(nc + 1), false, 1.0);
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nc + 1, nc);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.multiply(new DiagonalMatrix(nc + 1), true, 1.0);
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nc + 1, nc);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the method that premultiplies a given matrix by another matrix, using standard real
     * matrices which are not multiplication
     * compatible.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain RealMatrix#preMultiply(RealMatrix)}<br>
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     */
    public static void checkPreMultiplyIncompatibleRealMatrix(final RealMatrix matrix) {
        // Dimensions
        final int nr = matrix.getRowDimension();
        final int nc = matrix.getColumnDimension();

        // Matching dimension is too low
        try {
            matrix.preMultiply(new Array2DRowRealMatrix(nc, nr - 1));
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nr, nr - 1);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // Matching dimension is too large
        try {
            matrix.preMultiply(new Array2DRowRealMatrix(nc, nr + 1));
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nr, nr + 1);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the method that premultiplies a given matrix by another matrix, using standard
     * symmetric matrices which are not multiplication
     * compatible.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain RealMatrix#preMultiply(RealMatrix)}<br>
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     */
    public static void checkPreMultiplyIncompatibleSymmetricMatrix(final RealMatrix matrix) {
        // Row dimension
        final int nr = matrix.getRowDimension();

        // Matching dimension is too low
        try {
            matrix.preMultiply(new ArrayRowSymmetricMatrix(nr - 1));
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nr, nr - 1);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // Matching dimension is too large
        try {
            matrix.preMultiply(new ArrayRowSymmetricMatrix(nr + 1));
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nr, nr + 1);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the method that premultiplies a given matrix by another matrix, using standard
     * symmetric positive semi-definite matrices which
     * are not multiplication compatible.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain RealMatrix#preMultiply(RealMatrix)}<br>
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     */
    public static void checkPreMultiplyIncompatibleSymmetricPositiveMatrix(final RealMatrix matrix) {
        // Row dimension
        final int nr = matrix.getRowDimension();

        // Matching dimension is too low
        try {
            matrix.preMultiply(new ArrayRowSymmetricPositiveMatrix(nr - 1));
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nr, nr - 1);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // Matching dimension is too large
        try {
            matrix.preMultiply(new ArrayRowSymmetricPositiveMatrix(nr + 1));
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nr, nr + 1);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the method that premultiplies a given matrix by another matrix, using decomposed
     * symmetric positive semi-definite matrices
     * which are not multiplication compatible.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain RealMatrix#preMultiply(RealMatrix)}<br>
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     */
    public static void checkPreMultiplyIncompatibleDecomposedMatrix(final RealMatrix matrix) {
        // Row dimension
        final int nr = matrix.getRowDimension();

        // Matching dimension is too low
        try {
            matrix.preMultiply(new DecomposedSymmetricPositiveMatrix(nr - 1));
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nr, nr - 1);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // Matching dimension is too large
        try {
            matrix.preMultiply(new DecomposedSymmetricPositiveMatrix(nr + 1));
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nr, nr + 1);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the method that premultiplies a given matrix by another matrix, using diagonal matrices
     * which are not multiplication
     * compatible.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain RealMatrix#preMultiply(RealMatrix)}<br>
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     */
    public static void checkPreMultiplyIncompatibleDiagonalMatrix(final RealMatrix matrix) {
        // Row dimension
        final int nr = matrix.getRowDimension();

        // Matching dimension is too low
        try {
            matrix.preMultiply(new DiagonalMatrix(nr - 1));
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nr, nr - 1);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // Matching dimension is too large
        try {
            matrix.preMultiply(new DiagonalMatrix(nr + 1));
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nr, nr + 1);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the method that multiplies a given matrix by a vector, using vectors which are not
     * multiplication compatible.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain RealMatrix#operate(RealVector)}<br>
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     */
    public static void checkOperateIncompatibleVector(final RealMatrix matrix) {
        // Column dimension
        final int nc = matrix.getColumnDimension();

        // Matching dimension is too low
        try {
            matrix.operate(new ArrayRealVector(nc - 1));
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nc - 1, nc);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.operate(new double[nc - 1]);
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nc - 1, nc);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // Matching dimension is too large
        try {
            matrix.operate(new ArrayRealVector(nc + 1));
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nc + 1, nc);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.operate(new double[nc + 1]);
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nc + 1, nc);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the method that premultiplies a given matrix by a vector, using vectors which are not
     * multiplication compatible.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain RealMatrix#preMultiply(RealVector)}<br>
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     */
    public static void checkPreMultiplyIncompatibleVector(final RealMatrix matrix) {
        // Row dimension
        final int nr = matrix.getRowDimension();

        // Matching dimension is too low
        try {
            matrix.preMultiply(new ArrayRealVector(nr - 1));
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nr - 1, nr);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.preMultiply(new double[nr - 1]);
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nr - 1, nr);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // Matching dimension is too large
        try {
            matrix.preMultiply(new ArrayRealVector(nr + 1));
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nr + 1, nr);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.preMultiply(new double[nr + 1]);
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nr + 1, nr);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the methods that perform the quadratic multiplication M&times;S&times;M<sup>T</sup> of
     * a symmetric matrix S by a matrix M,
     * using matrices which have incompatible dimensions.
     *
     * @param matrix
     *        the matrix to be tested
     */
    public static void checkQuadraticMultiplicationIncompatibleMatrix(final SymmetricMatrix matrix) {
        // Dimension of the matrix
        final int dim = matrix.getRowDimension();

        // Matching dimension is too low
        try {
            matrix.quadraticMultiplication(new Array2DRowRealMatrix(2 * dim, dim - 1));
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, dim, dim - 1);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.quadraticMultiplication(new Array2DRowRealMatrix(2 * dim, dim - 1), false);
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, dim, dim - 1);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.quadraticMultiplication(new Array2DRowRealMatrix(dim - 1, 2 * dim), true);
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, dim - 1, dim);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // Matching dimension is too large
        try {
            matrix.quadraticMultiplication(new Array2DRowRealMatrix(2 * dim, dim + 1));
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, dim, dim + 1);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.quadraticMultiplication(new Array2DRowRealMatrix(2 * dim, dim + 1), false);
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, dim, dim + 1);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.quadraticMultiplication(new Array2DRowRealMatrix(dim + 1, 2 * dim), true);
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, dim + 1, dim);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the methods that concatenate two matrices horizontally.
     *
     * <p>
     * The results are tested against the following methods:<br>
     * {@linkplain Array2DRowRealMatrix#concatenateHorizontally(RealMatrix)}<br>
     * {@linkplain Array2DRowRealMatrix#concatenateHorizontally(RealMatrix, boolean)}<br>
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     * @param other
     *        the matrix to be concatenated with the tested matrix
     * @param expectedClass
     *        the expected class for the matrix yielded by the concatenation
     */
    public static void checkConcatenateHorizontally(final RealMatrix matrix,
            final RealMatrix other, final Class<? extends RealMatrix> expectedClass) {
        RealMatrix result;
        RealMatrix expected;

        // Copy the matrix as a standard real matrix
        final RealMatrix referenceMatrix = new Array2DRowRealMatrix(matrix.getData(), false);

        // Horizontal concatenation
        result = matrix.concatenateHorizontally(other);
        expected = referenceMatrix.concatenateHorizontally(other);
        checkEquality(expected, result, 0., 0.);
        Assert.assertEquals(expectedClass, result.getClass());
        Assert.assertNotSame(matrix, result);

        result = matrix.concatenateHorizontally(other, true);
        expected = referenceMatrix.concatenateHorizontally(other, true);
        checkEquality(expected, result, 0., 0.);
        Assert.assertEquals(expectedClass, result.getClass());
        Assert.assertNotSame(matrix, result);

        result = matrix.concatenateHorizontally(other, false);
        expected = referenceMatrix.concatenateHorizontally(other, false);
        checkEquality(expected, result, 0., 0.);
        Assert.assertEquals(expectedClass, result.getClass());
        Assert.assertNotSame(matrix, result);
    }

    /**
     * Tests the methods that concatenate two matrices vertically.
     *
     * <p>
     * The results are tested against the following methods:<br>
     * {@linkplain Array2DRowRealMatrix#concatenateVertically(RealMatrix)}<br>
     * {@linkplain Array2DRowRealMatrix#concatenateVertically(RealMatrix, boolean)}<br>
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     * @param other
     *        the matrix to be concatenated with the tested matrix
     * @param expectedClass
     *        the expected class for the matrix yielded by the concatenation
     */
    public static void checkConcatenateVertically(final RealMatrix matrix, final RealMatrix other,
            final Class<? extends RealMatrix> expectedClass) {
        RealMatrix result;
        RealMatrix expected;

        // Copy the matrix as a standard real matrix
        final RealMatrix referenceMatrix = new Array2DRowRealMatrix(matrix.getData(), false);

        // Vertical concatenation
        result = matrix.concatenateVertically(other);
        expected = referenceMatrix.concatenateVertically(other);
        checkEquality(expected, result, 0., 0.);
        Assert.assertEquals(expectedClass, result.getClass());
        Assert.assertNotSame(matrix, result);

        result = matrix.concatenateVertically(other, true);
        expected = referenceMatrix.concatenateVertically(other, true);
        checkEquality(expected, result, 0., 0.);
        Assert.assertEquals(expectedClass, result.getClass());
        Assert.assertNotSame(matrix, result);

        result = matrix.concatenateVertically(other, false);
        expected = referenceMatrix.concatenateVertically(other, false);
        checkEquality(expected, result, 0., 0.);
        Assert.assertEquals(expectedClass, result.getClass());
        Assert.assertNotSame(matrix, result);
    }

    /**
     * Tests the method that concatenate two matrices diagonally.
     *
     * <p>
     * The results are tested against the following method:<br>
     * {@linkplain Array2DRowRealMatrix#concatenateDiagonally(RealMatrix, boolean)}<br>
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     * @param other
     *        the matrix to be concatenated with the tested matrix
     * @param expectedClass
     *        the expected class for the matrix yielded by the concatenation
     */
    public static void checkConcatenateDiagonally(final RealMatrix matrix, final RealMatrix other,
            final Class<? extends RealMatrix> expectedClass) {
        RealMatrix result;
        RealMatrix expected;

        // Copy the matrix as a standard real matrix
        final RealMatrix referenceMatrix = new Array2DRowRealMatrix(matrix.getData(), false);

        // Diagonal concatenation
        result = matrix.concatenateDiagonally(other);
        expected = referenceMatrix.concatenateDiagonally(other);
        checkEquality(expected, result, 0., 0.);
        Assert.assertEquals(expectedClass, result.getClass());
        Assert.assertNotSame(matrix, result);

        result = matrix.concatenateDiagonally(other, true);
        expected = referenceMatrix.concatenateDiagonally(other, true);
        checkEquality(expected, result, 0., 0.);
        Assert.assertEquals(expectedClass, result.getClass());
        Assert.assertNotSame(matrix, result);

        result = matrix.concatenateDiagonally(other, false);
        expected = referenceMatrix.concatenateDiagonally(other, false);
        checkEquality(expected, result, 0., 0.);
        Assert.assertEquals(expectedClass, result.getClass());
        Assert.assertNotSame(matrix, result);

        result = matrix.concatenateDiagonally(other, true, true);
        expected = referenceMatrix.concatenateDiagonally(other, true, true);
        checkEquality(expected, result, 0., 0.);
        Assert.assertEquals(expectedClass, result.getClass());
        Assert.assertNotSame(matrix, result);

        result = matrix.concatenateDiagonally(other, true, false);
        expected = referenceMatrix.concatenateDiagonally(other, true, false);
        checkEquality(expected, result, 0., 0.);
        Assert.assertEquals(expectedClass, result.getClass());
        Assert.assertNotSame(matrix, result);

        result = matrix.concatenateDiagonally(other, false, true);
        expected = referenceMatrix.concatenateDiagonally(other, false, true);
        checkEquality(expected, result, 0., 0.);
        Assert.assertEquals(expectedClass, result.getClass());
        Assert.assertNotSame(matrix, result);

        result = matrix.concatenateDiagonally(other, false, false);
        expected = referenceMatrix.concatenateDiagonally(other, false, false);
        checkEquality(expected, result, 0., 0.);
        Assert.assertEquals(expectedClass, result.getClass());
        Assert.assertNotSame(matrix, result);
    }

    /**
     * Tests the methods that concatenate another matrix to a given matrix horizontally, using
     * matrices with incompatible dimensions.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain RealMatrix#concatenateHorizontally(RealMatrix)}<br>
     * {@linkplain RealMatrix#concatenateHorizontally(RealMatrix, boolean)}<br>
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     */
    public static void checkConcatenateHorizontallyIncompatibleMatrix(final RealMatrix matrix) {
        // Dimensions
        final int nr = matrix.getRowDimension();
        final int nc = matrix.getColumnDimension();

        // Matching dimension is too low
        try {
            matrix.concatenateHorizontally(new Array2DRowRealMatrix(nr - 1, nc));
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nr - 1, nr);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.concatenateHorizontally(new Array2DRowRealMatrix(nr - 1, nc), true);
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nr - 1, nr);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.concatenateHorizontally(new Array2DRowRealMatrix(nr - 1, nc), false);
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nr - 1, nr);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // Matching dimension is too high
        try {
            matrix.concatenateHorizontally(new Array2DRowRealMatrix(nr + 1, nc));
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nr + 1, nr);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.concatenateHorizontally(new Array2DRowRealMatrix(nr + 1, nc), true);
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nr + 1, nr);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.concatenateHorizontally(new Array2DRowRealMatrix(nr + 1, nc), false);
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nr + 1, nr);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the methods that concatenate another matrix to a given matrix vertically, using
     * matrices with incompatible dimensions.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain RealMatrix#concatenateVertically(RealMatrix)}<br>
     * {@linkplain RealMatrix#concatenateVertically(RealMatrix, boolean)}<br>
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     */
    public static void checkConcatenateVerticallyIncompatibleMatrix(final RealMatrix matrix) {
        // Dimensions
        final int nr = matrix.getRowDimension();
        final int nc = matrix.getColumnDimension();

        // Matching dimension is too low
        try {
            matrix.concatenateVertically(new Array2DRowRealMatrix(nr, nc - 1));
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nc - 1, nc);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.concatenateVertically(new Array2DRowRealMatrix(nr, nc - 1), true);
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nc - 1, nc);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.concatenateVertically(new Array2DRowRealMatrix(nr, nc - 1), false);
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nc - 1, nc);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // Matching dimension is too high
        try {
            matrix.concatenateVertically(new Array2DRowRealMatrix(nr, nc + 1));
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nc + 1, nc);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.concatenateVertically(new Array2DRowRealMatrix(nr, nc + 1), true);
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nc + 1, nc);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.concatenateVertically(new Array2DRowRealMatrix(nr, nc + 1), false);
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            final String expectedMessage = String.format(DIMENSION_MISMATCH_FORMAT, nc + 1, nc);
            Assert.assertEquals(DimensionMismatchException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the method that computes the inverse of the matrix.
     *
     * <p>
     * The results are tested against the following methods:<br>
     * {@linkplain Array2DRowRealMatrix#getInverse()}<br>
     * {@linkplain Array2DRowRealMatrix#getInverse(Function)}<br>
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     * @param expectedClass
     *        the expected class for the matrix yielded by the concatenation
     * @param absTol
     *        the absolute tolerance to take into account when comparing the computed matrices
     * @param relTol
     *        the relative tolerance to take into account when comparing the computed matrices
     */
    public static void checkInverseMatrix(final RealMatrix matrix,
            final Class<? extends RealMatrix> expectedClass, final double absTol,
            final double relTol) {
        RealMatrix result;
        RealMatrix expected;

        // Copy the matrix as a standard real matrix
        final RealMatrix referenceMatrix = new Array2DRowRealMatrix(matrix.getData(), false);

        // Decomposition builder
        final Function<RealMatrix, Decomposition> decompositionBuilder = QRDecomposition
                .decompositionBuilder(1E-14);

        // Check the matrix inversion using the default decomposition
        result = matrix.getInverse();
        expected = referenceMatrix.getInverse();
        checkEquality(expected, result, absTol, relTol);
        Assert.assertEquals(expectedClass, result.getClass());
        Assert.assertNotSame(matrix, result);

        // Check the matrix inversion using the a QR decomposition
        result = matrix.getInverse(decompositionBuilder);
        expected = referenceMatrix.getInverse(decompositionBuilder);
        checkEquality(expected, result, absTol, relTol);
        Assert.assertEquals(expectedClass, result.getClass());
        Assert.assertNotSame(matrix, result);
    }

    /**
     * Tests the method that computes the inverse of the matrix when called on a rectangular matrix.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain RealMatrix#getInverse()}<br>
     * {@linkplain RealMatrix#getInverse(Function)}<br>
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     */
    public static void checkInverseNonSquareMatrix(final RealMatrix matrix) {
        // Decomposition builder
        final Function<RealMatrix, Decomposition> decompositionBuilder = QRDecomposition
                .decompositionBuilder(1E-14);

        try {
            matrix.getInverse();
            Assert.fail();
        } catch (final NonSquareMatrixException e) {
            final String expectedMessage = String.format(NON_SQUARE_MATRIX_FORMAT,
                    matrix.getRowDimension(), matrix.getColumnDimension());
            Assert.assertEquals(NonSquareMatrixException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.getInverse(decompositionBuilder);
            Assert.fail();
        } catch (final NonSquareMatrixException e) {
            final String expectedMessage = String.format(NON_SQUARE_MATRIX_FORMAT,
                    matrix.getRowDimension(), matrix.getColumnDimension());
            Assert.assertEquals(NonSquareMatrixException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the method that computes the inverse of the matrix when called on a singular matrix.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain RealMatrix#getInverse()}<br>
     * {@linkplain RealMatrix#getInverse(Function)}<br>
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     */
    public static void checkInverseSingularMatrix(final RealMatrix matrix) {
        // Decomposition builder
        final Function<RealMatrix, Decomposition> decompositionBuilder = QRDecomposition
                .decompositionBuilder(1E-14);

        try {
            matrix.getInverse();
            Assert.fail();
        } catch (final SingularMatrixException e) {
            final String expectedMessage = String.format(SINGULAR_MATRIX_FORMAT);
            Assert.assertEquals(SingularMatrixException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            matrix.getInverse(decompositionBuilder);
            Assert.fail();
        } catch (final SingularMatrixException e) {
            final String expectedMessage = String.format(SINGULAR_MATRIX_FORMAT);
            Assert.assertEquals(SingularMatrixException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the default decomposition algorithm, and the method that changes it.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain RealMatrix#getDefaultDecomposition()}<br>
     * {@linkplain RealMatrix#setDefaultDecomposition(Function)}<br>
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     */
    public static void checkDefaultDecomposition(final RealMatrix matrix) {
        Decomposition decomposition;
        Function<RealMatrix, Decomposition> decompositionBuilder;

        // The default decomposition algorithm should be a LU decomposition
        decompositionBuilder = matrix.getDefaultDecomposition();
        Assert.assertNotNull(decompositionBuilder);
        decomposition = decompositionBuilder.apply(matrix);
        Assert.assertNotNull(decomposition);
        Assert.assertEquals(LUDecomposition.class, decomposition.getClass());

        // Change the default decomposition algorithm
        decompositionBuilder = QRDecomposition.decompositionBuilder(1E-14);
        matrix.setDefaultDecomposition(decompositionBuilder);
        Assert.assertSame(decompositionBuilder, matrix.getDefaultDecomposition());

        // Reset the default decomposition
        matrix.setDefaultDecomposition(null);
        decompositionBuilder = matrix.getDefaultDecomposition();
        Assert.assertNotNull(decompositionBuilder);
        decomposition = decompositionBuilder.apply(matrix);
        Assert.assertNotNull(decomposition);
        Assert.assertEquals(LUDecomposition.class, decomposition.getClass());
    }

    /**
     * Tests the method that determines if the matrix is numerically equal to another matrix, taking
     * into account the specified tolerances.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain RealMatrix#equals(RealMatrix, double, double)}
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     */
    public static void checkEqualsWithTolerances(final RealMatrix matrix) {
        RealMatrix other;

        // Dimensions
        final int nr = matrix.getRowDimension();
        final int nc = matrix.getColumnDimension();

        // Same instance
        other = matrix;
        Assert.assertTrue(matrix.equals(other, 0., 0.));

        // Null instance
        other = null;
        Assert.assertFalse(matrix.equals(other, 0., 0.));

        // Different instance, same data, same matrix type
        other = matrix.copy();
        Assert.assertTrue(matrix.equals(other, 0., 0.));

        // Different instance, same data, different matrix type
        other = new Array2DRowRealMatrix(matrix.getData(), false);
        Assert.assertTrue(matrix.equals(other, 0., 0.));

        // Different row dimensions
        other = new Array2DRowRealMatrix(nr - 1, nc);
        Assert.assertFalse(matrix.equals(other, 0., 0.));

        other = new Array2DRowRealMatrix(nr + 1, nc);
        Assert.assertFalse(matrix.equals(other, 0., 0.));

        // Different column dimensions
        other = new Array2DRowRealMatrix(nr, nc - 1);
        Assert.assertFalse(matrix.equals(other, 0., 0.));

        other = new Array2DRowRealMatrix(nr, nc + 1);
        Assert.assertFalse(matrix.equals(other, 0., 0.));

        // A single entry is slightly different
        final double epsilon = 1E-8 * MathLib.min(MathLib.abs(matrix.getMin()),
                MathLib.abs(matrix.getMax()));

        for (int i = 0; i < nr; i++) {
            for (int j = 0; j < nc; j++) {
                final double[][] data = matrix.getData();
                final double initialEntry = data[i][j];
                final double modifiedEntry = initialEntry + epsilon;
                data[i][j] = modifiedEntry;
                other = new Array2DRowRealMatrix(data);

                final double absDiff = getDifference(initialEntry, modifiedEntry,
                        ComparisonType.ABSOLUTE);
                final double relDiff = getDifference(initialEntry, modifiedEntry,
                        ComparisonType.RELATIVE);
                final double absTol = MathLib.abs(absDiff);
                final double relTol = MathLib.abs(relDiff);

                try {
                    Assert.assertTrue(matrix.equals(other, 0., absTol));
                    Assert.assertFalse(matrix.equals(other, 0., absTol - MathLib.ulp(absTol)));
                    Assert.assertTrue(matrix.equals(other, relTol, 0.));
                    Assert.assertFalse(matrix.equals(other, relTol - MathLib.ulp(relTol), 0.));
                } catch (final AssertionError e) {
                    final StringBuilder builder = new StringBuilder();
                    builder.append(String.format("Assertion error for entry (%d,%d)", i, j));
                    builder.append(System.lineSeparator());
                    builder.append("      Initial entry: ");
                    builder.append(DEFAULT_DECIMAL_FORMAT.format(initialEntry));
                    builder.append(System.lineSeparator());
                    builder.append("     Modified entry: ");
                    builder.append(DEFAULT_DECIMAL_FORMAT.format(modifiedEntry));
                    builder.append(System.lineSeparator());
                    builder.append("Absolute difference: ");
                    builder.append(String.format(SCIENTIFIC_NUMBER_FORMAT, absDiff));
                    builder.append(System.lineSeparator());
                    builder.append("Relative difference: ");
                    builder.append(String.format(SCIENTIFIC_NUMBER_FORMAT, relDiff));
                    builder.append(System.lineSeparator());

                    throw new AssertionError(builder.toString(), e);
                }
            }
        }
    }

    /**
     * Tests the methods that visit the entries of a matrix in row order without modifying them.
     *
     * <p>
     * The results are tested against the following methods:<br>
     * {@linkplain Array2DRowRealMatrix#walkInRowOrder(RealMatrixPreservingVisitor)}<br>
     * {@linkplain Array2DRowRealMatrix#walkInRowOrder(RealMatrixPreservingVisitor, int, int, int, int)}
     * <br>
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     */
    public static void checkPreservingWalkInRowOrder(final RealMatrix matrix) {
        int nbVisited;

        // Copy the matrix as a standard real matrix
        final RealMatrix referenceMatrix = new Array2DRowRealMatrix(matrix.getData(), false);

        // Dimensions
        final int nr = matrix.getRowDimension();
        final int nc = matrix.getColumnDimension();

        // Visitor
        final PreservingVisitor visitor = new PreservingVisitor();
        final PreservingVisitor referenceVisitor = new PreservingVisitor();

        // Visit the entire matrix
        nbVisited = nr * nc;
        matrix.walkInRowOrder(visitor);
        referenceMatrix.walkInRowOrder(referenceVisitor);
        Assert.assertEquals(nr * nc, visitor.getNbVisited());
        Assert.assertEquals(referenceVisitor.getNbVisited(), visitor.getNbVisited());
        Assert.assertEquals(referenceVisitor.getResult(), visitor.getResult());

        // Visit part of the matrix
        for (int i = 0; i < nr; i++) {
            for (int j = 0; j < nc; j++) {
                // Upper left submatrix
                nbVisited = (i + 1) * (j + 1);
                matrix.walkInRowOrder(visitor, 0, i, 0, j);
                referenceMatrix.walkInRowOrder(referenceVisitor, 0, i, 0, j);
                Assert.assertEquals(nbVisited, visitor.getNbVisited());
                Assert.assertEquals(referenceVisitor.getNbVisited(), visitor.getNbVisited());
                Assert.assertEquals(referenceVisitor.getResult(), visitor.getResult());

                // Upper right submatrix
                nbVisited = (i + 1) * (nc - j);
                matrix.walkInRowOrder(visitor, 0, i, j, nc - 1);
                referenceMatrix.walkInRowOrder(referenceVisitor, 0, i, j, nc - 1);
                Assert.assertEquals(nbVisited, visitor.getNbVisited());
                Assert.assertEquals(referenceVisitor.getNbVisited(), visitor.getNbVisited());
                Assert.assertEquals(referenceVisitor.getResult(), visitor.getResult());

                // Lower left submatrix
                nbVisited = (nr - i) * (j + 1);
                matrix.walkInRowOrder(visitor, i, nr - 1, 0, j);
                referenceMatrix.walkInRowOrder(referenceVisitor, i, nr - 1, 0, j);
                Assert.assertEquals(nbVisited, visitor.getNbVisited());
                Assert.assertEquals(referenceVisitor.getNbVisited(), visitor.getNbVisited());
                Assert.assertEquals(referenceVisitor.getResult(), visitor.getResult());

                // Lower right submatrix
                nbVisited = (nr - i) * (nc - j);
                matrix.walkInRowOrder(visitor, i, nr - 1, j, nc - 1);
                referenceMatrix.walkInRowOrder(referenceVisitor, i, nr - 1, j, nc - 1);
                Assert.assertEquals(nbVisited, visitor.getNbVisited());
                Assert.assertEquals(referenceVisitor.getNbVisited(), visitor.getNbVisited());
                Assert.assertEquals(referenceVisitor.getResult(), visitor.getResult());
            }
        }
    }

    /**
     * Tests the methods that visit the entries of a matrix in column order without modifying them.
     *
     * <p>
     * The results are tested against the following methods:<br>
     * {@linkplain Array2DRowRealMatrix#walkInColumnOrder(RealMatrixPreservingVisitor)}<br>
     * {@linkplain Array2DRowRealMatrix#walkInColumnOrder(RealMatrixPreservingVisitor, int, int, int, int)}
     * <br>
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     */
    public static void checkPreservingWalkInColumnOrder(final RealMatrix matrix) {
        int nbVisited;

        // Copy the matrix as a standard real matrix
        final RealMatrix referenceMatrix = new Array2DRowRealMatrix(matrix.getData(), false);

        // Dimensions
        final int nr = matrix.getRowDimension();
        final int nc = matrix.getColumnDimension();

        // Visitor
        final PreservingVisitor visitor = new PreservingVisitor();
        final PreservingVisitor referenceVisitor = new PreservingVisitor();

        // Visit the entire matrix
        nbVisited = nr * nc;
        matrix.walkInColumnOrder(visitor);
        referenceMatrix.walkInColumnOrder(referenceVisitor);
        Assert.assertEquals(nr * nc, visitor.getNbVisited());
        Assert.assertEquals(referenceVisitor.getNbVisited(), visitor.getNbVisited());
        Assert.assertEquals(referenceVisitor.getResult(), visitor.getResult());

        // Visit part of the matrix
        for (int i = 0; i < nr; i++) {
            for (int j = 0; j < nc; j++) {
                // Upper left submatrix
                nbVisited = (i + 1) * (j + 1);
                matrix.walkInColumnOrder(visitor, 0, i, 0, j);
                referenceMatrix.walkInColumnOrder(referenceVisitor, 0, i, 0, j);
                Assert.assertEquals(nbVisited, visitor.getNbVisited());
                Assert.assertEquals(referenceVisitor.getNbVisited(), visitor.getNbVisited());
                Assert.assertEquals(referenceVisitor.getResult(), visitor.getResult());
                // Upper right submatrix
                nbVisited = (i + 1) * (nc - j);
                matrix.walkInColumnOrder(visitor, 0, i, j, nc - 1);
                referenceMatrix.walkInColumnOrder(referenceVisitor, 0, i, j, nc - 1);
                Assert.assertEquals(nbVisited, visitor.getNbVisited());
                Assert.assertEquals(referenceVisitor.getNbVisited(), visitor.getNbVisited());
                Assert.assertEquals(referenceVisitor.getResult(), visitor.getResult());

                // Lower left submatrix
                nbVisited = (nr - i) * (j + 1);
                matrix.walkInColumnOrder(visitor, i, nr - 1, 0, j);
                referenceMatrix.walkInColumnOrder(referenceVisitor, i, nr - 1, 0, j);
                Assert.assertEquals(nbVisited, visitor.getNbVisited());
                Assert.assertEquals(referenceVisitor.getNbVisited(), visitor.getNbVisited());
                Assert.assertEquals(referenceVisitor.getResult(), visitor.getResult());

                // Lower right submatrix
                nbVisited = (nr - i) * (nc - j);
                matrix.walkInColumnOrder(visitor, i, nr - 1, j, nc - 1);
                referenceMatrix.walkInColumnOrder(referenceVisitor, i, nr - 1, j, nc - 1);
                Assert.assertEquals(nbVisited, visitor.getNbVisited());
                Assert.assertEquals(referenceVisitor.getNbVisited(), visitor.getNbVisited());
                Assert.assertEquals(referenceVisitor.getResult(), visitor.getResult());
            }
        }
    }

    /**
     * Tests the methods that visit the entries of a matrix in optimized order without modifying
     * them.
     *
     * <p>
     * The results are tested against the following methods:<br>
     * {@linkplain Array2DRowRealMatrix#walkInOptimizedOrder(RealMatrixPreservingVisitor)}<br>
     * {@linkplain Array2DRowRealMatrix#walkInOptimizedOrder(RealMatrixPreservingVisitor, int, int, int, int)}
     * <br>
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     */
    public static void checkPreservingWalkInOptimizedOrder(final RealMatrix matrix) {
        int nbVisited;

        // Copy the matrix as a standard real matrix
        final RealMatrix referenceMatrix = new Array2DRowRealMatrix(matrix.getData(), false);

        // Dimensions
        final int nr = matrix.getRowDimension();
        final int nc = matrix.getColumnDimension();

        // Visitor
        final PreservingVisitor visitor = new PreservingVisitor();
        final PreservingVisitor referenceVisitor = new PreservingVisitor();

        // Visit the entire matrix
        nbVisited = nr * nc;
        matrix.walkInOptimizedOrder(visitor);
        referenceMatrix.walkInOptimizedOrder(referenceVisitor);
        Assert.assertEquals(nr * nc, visitor.getNbVisited());
        Assert.assertEquals(referenceVisitor.getNbVisited(), visitor.getNbVisited());
        Assert.assertEquals(referenceVisitor.getResult(), visitor.getResult());

        // Visit part of the matrix
        for (int i = 0; i < nr; i++) {
            for (int j = 0; j < nc; j++) {
                // Upper left submatrix
                nbVisited = (i + 1) * (j + 1);
                matrix.walkInOptimizedOrder(visitor, 0, i, 0, j);
                referenceMatrix.walkInOptimizedOrder(referenceVisitor, 0, i, 0, j);
                Assert.assertEquals(nbVisited, visitor.getNbVisited());
                Assert.assertEquals(referenceVisitor.getNbVisited(), visitor.getNbVisited());
                Assert.assertEquals(referenceVisitor.getResult(), visitor.getResult());

                // Upper right submatrix
                nbVisited = (i + 1) * (nc - j);
                matrix.walkInOptimizedOrder(visitor, 0, i, j, nc - 1);
                referenceMatrix.walkInOptimizedOrder(referenceVisitor, 0, i, j, nc - 1);
                Assert.assertEquals(nbVisited, visitor.getNbVisited());
                Assert.assertEquals(referenceVisitor.getNbVisited(), visitor.getNbVisited());
                Assert.assertEquals(referenceVisitor.getResult(), visitor.getResult());

                // Lower left submatrix
                nbVisited = (nr - i) * (j + 1);
                matrix.walkInOptimizedOrder(visitor, i, nr - 1, 0, j);
                referenceMatrix.walkInOptimizedOrder(referenceVisitor, i, nr - 1, 0, j);
                Assert.assertEquals(nbVisited, visitor.getNbVisited());
                Assert.assertEquals(referenceVisitor.getNbVisited(), visitor.getNbVisited());
                Assert.assertEquals(referenceVisitor.getResult(), visitor.getResult());

                // Lower right submatrix
                nbVisited = (nr - i) * (nc - j);
                matrix.walkInOptimizedOrder(visitor, i, nr - 1, j, nc - 1);
                referenceMatrix.walkInOptimizedOrder(referenceVisitor, i, nr - 1, j, nc - 1);
                Assert.assertEquals(nbVisited, visitor.getNbVisited());
                Assert.assertEquals(referenceVisitor.getNbVisited(), visitor.getNbVisited());
                Assert.assertEquals(referenceVisitor.getResult(), visitor.getResult());
            }
        }
    }

    /**
     * Tests the method that returns a string representation of the matrix.
     *
     * <p>
     * The results are tested against the following methods:<br>
     * {@linkplain Array2DRowRealMatrix#toString()}<br>
     * {@linkplain Array2DRowRealMatrix#toString(RealMatrixFormat)}<br>
     * </p>
     *
     * @param matrix
     *        the matrix to be tested
     */
    public static void checkToString(final RealMatrix matrix) {
        String result;
        String expected;

        // Copy the matrix as a standard real matrix
        final RealMatrix referenceMatrix = new Array2DRowRealMatrix(matrix.getData(), false);

        // Default format
        result = matrix.toString();
        expected = referenceMatrix.toString();
        expected = getExpectedString(expected, referenceMatrix.getClass(), matrix.getClass());
        Assert.assertEquals(expected, result);

        // Test all the available formats
        final List<RealMatrixFormat> formats = new ArrayList<>();
        formats.add(MatrixUtils.DEFAULT_FORMAT);
        formats.add(MatrixUtils.JAVA_FORMAT);
        formats.add(MatrixUtils.OCTAVE_FORMAT);
        formats.add(MatrixUtils.SCILAB_FORMAT);
        formats.add(MatrixUtils.SUMMARY_FORMAT);
        formats.add(MatrixUtils.VISUAL_FORMAT);
        formats.add(MatrixUtils.NUMPY_FORMAT);

        for (final RealMatrixFormat format : formats) {
            result = matrix.toString(format);
            expected = referenceMatrix.toString(format);
            expected = getExpectedString(expected, referenceMatrix.getClass(), matrix.getClass());
            Assert.assertEquals(expected, result);
        }
    }

    /**
     * Modifies the string representation of a matrix (class name and indentation) in order to build
     * the expected string representation for
     * another class.
     *
     * @param referenceString
     *        the string representation of a reference matrix
     * @param referenceClass
     *        the class of the reference matrix
     * @param actualClass
     *        the class of the matrix to be tested
     *
     * @return the expected string representation for the tested matrix
     */
    private static String getExpectedString(final String referenceString,
            final Class<? extends RealMatrix> referenceClass,
            final Class<? extends RealMatrix> actualClass) {
        // Class names
        final String actualClassName = actualClass.getSimpleName();
        final String referenceClassName = referenceClass.getSimpleName();

        String updatedString = referenceString;

        // Replace the class name
        updatedString = updatedString.replace(referenceClassName, actualClassName);

        // Correct the indentation
        final int n0 = referenceClassName.length();
        final int n1 = actualClassName.length();
        final String lineEnd = "\n";
        updatedString = updatedString.replaceAll(lineEnd + getWhitespaces(n0), lineEnd
                + getWhitespaces(n1));

        return updatedString;
    }

    /**
     * Gets a string containing the whitespace character repeated N times.
     *
     * @param n
     *        the number of whitespace characters the
     *
     * @return the string build
     */
    private static String getWhitespaces(final int n) {
        return new String(new char[n]).replace("\0", WHITESPACE);
    }

    /**
     * Creates a new random generator using the specified seed, or a random seed if the specified
     * seed is {@code null}.
     *
     * @param seed
     *        the seed to be used by the returned random number generator (can be {@code null})
     *
     * @return the random number generator build
     */
    public static Random getRandomNumberGenerator(final Long seed) {
        Random rng;
        if (seed == null) {
            rng = new Random();
        } else {
            rng = new Random(seed);
        }
        return rng;
    }

    /**
     * Generates a random number between the specified values.
     *
     * @param rng
     *        the random number generator to use
     * @param minValue
     *        the minimum value allowed for the returned value
     * @param maxValue
     *        the maximum value allowed for the returned value
     *
     * @return the generated number
     */
    public static double getRandomNumber(final Random rng, final double minValue,
            final double maxValue) {
        return getRandomNumber(rng, DEFAULT_BASE, minValue, maxValue, 0, 0);
    }

    /**
     * Generates a random number.
     *
     * <p>
     * The number is generated as follows:<br>
     * random number = mantissa &times; power(base, exponent),<br>
     * where the mantissa and the exponent are randomly generated between the specified min/max
     * values.
     * </p>
     *
     * @param rng
     *        the random number generator to use
     * @param base
     *        the base in which the generated number is defined
     * @param minMantissa
     *        the minimum value allowed for the mantissa
     * @param maxMantissa
     *        the maximum value allowed for the mantissa
     * @param minExponent
     *        the minimum value allowed for the exponent
     * @param maxExponent
     *        the maximum value allowed for the exponent
     *
     * @return the generated number
     */
    public static double getRandomNumber(final Random rng, final double base,
            final double minMantissa, final double maxMantissa, final int minExponent,
            final int maxExponent) {
        final int exponent = minExponent + rng.nextInt(1 + maxExponent - minExponent);
        final double mantissa = minMantissa + (maxMantissa - minMantissa) * rng.nextDouble();
        return mantissa * MathLib.pow(base, exponent);
    }

    /**
     * Generates an 1D-array filled with random numbers comprised between the specified values.
     *
     * @param rng
     *        the random number generator to use
     * @param length
     *        the length of the generated array
     * @param minValue
     *        the minimum value allowed for the elements of the array
     * @param maxValue
     *        the maximum value allowed for the elements of the array
     *
     * @return the generated 1D-array
     */
    public static double[] getRandom1dArray(final Random rng, final int length,
            final double minValue, final double maxValue) {
        return getRandom1dArray(rng, length, DEFAULT_BASE, minValue, maxValue, 0, 0);
    }

    /**
     * Generates an 1D-array filled with random numbers.
     *
     * <p>
     * The numbers are generated as follows:<br>
     * random number = mantissa &times; power(base, exponent),<br>
     * where the mantissa and the exponent are randomly generated between the specified min/max
     * values.
     * </p>
     *
     * @param rng
     *        the random number generator to use
     * @param length
     *        the length of the generated array
     * @param base
     *        the base in which the generated numbers are defined
     * @param minMantissa
     *        the minimum value allowed for the mantissa
     * @param maxMantissa
     *        the maximum value allowed for the mantissa
     * @param minExponent
     *        the minimum value allowed for the exponent
     * @param maxExponent
     *        the maximum value allowed for the exponent
     *
     * @return the generated 1D-array
     */
    public static double[] getRandom1dArray(final Random rng, final int length, final double base,
            final double minMantissa, final double maxMantissa, final int minExponent,
            final int maxExponent) {
        final double[] data = new double[length];
        for (int i = 0; i < length; i++) {
            data[i] = getRandomNumber(rng, base, minMantissa, maxMantissa, minExponent, maxExponent);
        }
        return data;
    }

    /**
     * Generates an 2D-array filled with random numbers comprised between the specified values.
     *
     * @param rng
     *        the random number generator to use
     * @param length1
     *        the length of the generated array along the first dimension
     * @param length2
     *        the length of the generated array along the second dimension
     * @param minValue
     *        the minimum value allowed for the elements of the array
     * @param maxValue
     *        the maximum value allowed for the elements of the array
     *
     * @return the generated 2D-array
     */
    public static double[][] getRandom2dArray(final Random rng, final int length1,
            final int length2, final double minValue, final double maxValue) {
        return getRandom2dArray(rng, length1, length2, DEFAULT_BASE, minValue, maxValue, 0, 0);
    }

    /**
     * Generates an 2D-array filled with random numbers.
     *
     * <p>
     * The numbers are generated as follows:<br>
     * random number = mantissa &times; power(base, exponent),<br>
     * where the mantissa and the exponent are randomly generated between the specified min/max
     * values.
     * </p>
     *
     * @param rng
     *        the random number generator to use
     * @param length1
     *        the length of the generated array along the first dimension
     * @param length2
     *        the length of the generated array along the second dimension
     * @param base
     *        the base in which the generated numbers are defined
     * @param minMantissa
     *        the minimum value allowed for the mantissa
     * @param maxMantissa
     *        the maximum value allowed for the mantissa
     * @param minExponent
     *        the minimum value allowed for the exponent
     * @param maxExponent
     *        the maximum value allowed for the exponent
     *
     * @return the generated 2D-array
     */
    public static double[][] getRandom2dArray(final Random rng, final int length1,
            final int length2, final double base, final double minMantissa,
            final double maxMantissa, final int minExponent, final int maxExponent) {
        final double[][] data = new double[length1][length2];
        for (int i = 0; i < length1; i++) {
            for (int j = 0; j < length2; j++) {
                data[i][j] = getRandomNumber(rng, base, minMantissa, maxMantissa, minExponent,
                        maxExponent);
            }
        }
        return data;
    }

    /**
     * Generates a symmetric 2D-array filled with random numbers.
     *
     * @param rng
     *        the random number generator to use
     * @param length
     *        the length of the generated array along the first and second dimensions
     * @param minValue
     *        the minimum value allowed for the elements of the array
     * @param maxValue
     *        the maximum value allowed for the elements of the array
     *
     * @return the generated 2D-array
     */
    public static double[][] getRandomSymmetricArray(final Random rng, final int length,
            final double minValue, final double maxValue) {
        return getRandomSymmetricArray(rng, length, DEFAULT_BASE, minValue, maxValue, 0, 0);
    }

    /**
     * Generates a symmetric 2D-array filled with random numbers.
     *
     * <p>
     * The numbers are generated as follows:<br>
     * random number = mantissa &times; power(base, exponent),<br>
     * where the mantissa and the exponent are randomly generated between the specified min/max
     * values.
     * </p>
     *
     * @param rng
     *        the random number generator to use
     * @param length
     *        the length of the generated array along the first and second dimensions
     * @param base
     *        the base in which the generated numbers are defined
     * @param minMantissa
     *        the minimum value allowed for the mantissa
     * @param maxMantissa
     *        the maximum value allowed for the mantissa
     * @param minExponent
     *        the minimum value allowed for the exponent
     * @param maxExponent
     *        the maximum value allowed for the exponent
     *
     * @return the generated 2D-array
     */
    public static double[][] getRandomSymmetricArray(final Random rng, final int length,
            final double base, final double minMantissa, final double maxMantissa,
            final int minExponent, final int maxExponent) {
        final double[][] data = new double[length][length];
        for (int i = 0; i < length; i++) {
            data[i][i] = getRandomNumber(rng, base, minMantissa, maxMantissa, minExponent,
                    maxExponent);
            for (int j = 0; j < i; j++) {
                data[i][j] = getRandomNumber(rng, base, minMantissa, maxMantissa, minExponent,
                        maxExponent);
                data[j][i] = data[i][j];
            }
        }
        return data;
    }

    /**
     * Generates a matrix filled with random numbers comprised between the specified values.
     *
     * @param rng
     *        the random number generator to use
     * @param rowDimension
     *        the row dimension of the generated matrix
     * @param columnDimension
     *        the column dimension of the generated matrix
     * @param minValue
     *        the minimum value allowed for the entries of the matrix
     * @param maxValue
     *        the maximum value allowed for the entries of the matrix
     *
     * @return the generated matrix
     */
    public static Array2DRowRealMatrix getRandomMatrix(final Random rng, final int rowDimension,
            final int columnDimension, final double minValue, final double maxValue) {
        return getRandomMatrix(rng, rowDimension, columnDimension, DEFAULT_BASE, minValue,
                maxValue, 0, 0);
    }

    /**
     * Generates an 2D-array filled with random numbers.
     *
     * <p>
     * The numbers are generated as follows:<br>
     * random number = mantissa &times; power(base, exponent),<br>
     * where the mantissa and the exponent are randomly generated between the specified min/max
     * values.
     * </p>
     *
     * @param rng
     *        the random number generator to use
     * @param rowDimension
     *        the row dimension of the generated matrix
     * @param columnDimension
     *        the column dimension of the generated matrix
     * @param base
     *        the base in which the generated numbers are defined
     * @param minMantissa
     *        the minimum value allowed for the mantissa
     * @param maxMantissa
     *        the maximum value allowed for the mantissa
     * @param minExponent
     *        the minimum value allowed for the exponent
     * @param maxExponent
     *        the maximum value allowed for the exponent
     *
     * @return the generated matrix
     */
    public static Array2DRowRealMatrix getRandomMatrix(final Random rng, final int rowDimension,
            final int columnDimension, final double base, final double minMantissa,
            final double maxMantissa, final int minExponent, final int maxExponent) {
        final double[][] data = getRandom2dArray(rng, rowDimension, columnDimension, base,
                minMantissa, maxMantissa, minExponent, maxExponent);
        return new Array2DRowRealMatrix(data);
    }

    /**
     * Generates a matrix filled with random numbers comprised between the specified values.
     *
     * @param rng
     *        the random number generator to use
     * @param dimension
     *        the dimension of the generated matrix
     * @param minValue
     *        the minimum value allowed for the entries of the matrix
     * @param maxValue
     *        the maximum value allowed for the entries of the matrix
     *
     * @return the generated matrix
     */
    public static ArrayRowSymmetricMatrix getRandomSymmetricMatrix(final Random rng,
            final int dimension, final double minValue, final double maxValue) {
        return getRandomSymmetricMatrix(rng, dimension, DEFAULT_BASE, minValue, maxValue, 0, 0);
    }

    /**
     * Generates an 2D-array filled with random numbers.
     *
     * <p>
     * The numbers are generated as follows:<br>
     * random number = mantissa &times; power(base, exponent),<br>
     * where the mantissa and the exponent are randomly generated between the specified min/max
     * values.
     * </p>
     *
     * @param rng
     *        the random number generator to use
     * @param dimension
     *        the dimension of the generated matrix
     * @param base
     *        the base in which the generated numbers are defined
     * @param minMantissa
     *        the minimum value allowed for the mantissa
     * @param maxMantissa
     *        the maximum value allowed for the mantissa
     * @param minExponent
     *        the minimum value allowed for the exponent
     * @param maxExponent
     *        the maximum value allowed for the exponent
     *
     * @return the generated matrix
     */
    public static ArrayRowSymmetricMatrix getRandomSymmetricMatrix(final Random rng,
            final int dimension, final double base, final double minMantissa,
            final double maxMantissa, final int minExponent, final int maxExponent) {
        final double[][] data = getRandomSymmetricArray(rng, dimension, base, minMantissa,
                maxMantissa, minExponent, maxExponent);
        return new ArrayRowSymmetricMatrix(SymmetryType.LOWER, data, 0., 0.);
    }

    /**
     * Generates a diagonal matrix filled with random numbers comprised between the specified
     * values.
     *
     * @param rng
     *        the random number generator to use
     * @param dimension
     *        the dimension of the generated matrix
     * @param minValue
     *        the minimum value allowed for the diagonal elements
     * @param maxValue
     *        the maximum value allowed for the diagonal elements
     *
     * @return the generated matrix
     */
    public static DiagonalMatrix getRandomDiagonalMatrix(final Random rng, final int dimension,
            final double minValue, final double maxValue) {
        return getRandomDiagonalMatrix(rng, dimension, DEFAULT_BASE, minValue, maxValue, 0, 0);
    }

    /**
     * Generates a diagonal matrix filled with random numbers.
     *
     * <p>
     * The numbers are generated as follows:<br>
     * random number = mantissa &times; power(base, exponent),<br>
     * where the mantissa and the exponent are randomly generated between the specified min/max
     * values.
     * </p>
     *
     * @param rng
     *        the random number generator to use
     * @param dimension
     *        the dimension of the generated matrix
     * @param base
     *        the base in which the generated numbers are defined
     * @param minMantissa
     *        the minimum value allowed for the mantissa
     * @param maxMantissa
     *        the maximum value allowed for the mantissa
     * @param minExponent
     *        the minimum value allowed for the exponent
     * @param maxExponent
     *        the maximum value allowed for the exponent
     *
     * @return the generated matrix
     */
    public static DiagonalMatrix getRandomDiagonalMatrix(final Random rng, final int dimension,
            final double base, final double minMantissa, final double maxMantissa,
            final int minExponent, final int maxExponent) {
        final double[] diagonalElements = getRandom1dArray(rng, dimension, base, minMantissa,
                maxMantissa, minExponent, maxExponent);
        return new DiagonalMatrix(diagonalElements, false);
    }

    /**
     * Generates a symmetric positive semi-definite matrix with random eigenvalues.
     *
     * @param rng
     *        the random number generator to use
     * @param dimension
     *        the dimension of the generated matrix
     * @param minValue
     *        the minimum value allowed for the eigenvalues
     * @param maxValue
     *        the maximum value allowed for the eigenvalues
     *
     * @return the generated matrix
     */
    public static ArrayRowSymmetricPositiveMatrix getRandomSymmetricPositiveMatrix(
            final Random rng, final int dimension, final double minValue, final double maxValue) {
        return getRandomSymmetricPositiveMatrix(rng, dimension, DEFAULT_BASE, minValue, maxValue,
                0, 0);
    }

    /**
     * Generates a symmetric positive semi-definite matrix with random eigenvalues.
     *
     * <p>
     * The eigenvalues of the matrix are generated as follows:<br>
     * random number = mantissa &times; power(base, exponent),<br>
     * where the mantissa and the exponent are randomly generated between the specified min/max
     * values.
     * </p>
     *
     * @param rng
     *        the random number generator to use
     * @param dimension
     *        the dimension of the generated matrix
     * @param base
     *        the base in which the generated numbers are defined
     * @param minMantissa
     *        the minimum value allowed for the mantissa
     * @param maxMantissa
     *        the maximum value allowed for the mantissa
     * @param minExponent
     *        the minimum value allowed for the exponent
     * @param maxExponent
     *        the maximum value allowed for the exponent
     *
     * @return the generated matrix
     */
    public static ArrayRowSymmetricPositiveMatrix getRandomSymmetricPositiveMatrix(
            final Random rng, final int dimension, final double base, final double minMantissa,
            final double maxMantissa, final int minExponent, final int maxExponent) {
        // Ensure the minimum value allowed for the mantissa is positive
        if (minMantissa < 0) {
            throw new NotPositiveException(minMantissa);
        }
        if (maxMantissa <= minMantissa) {
            throw new NumberIsTooSmallException(maxMantissa, minMantissa, false);
        }

        // Random diagonal matrix
        final double[] diagonalElements = getRandom1dArray(rng, dimension, base, minMantissa,
                maxMantissa, minExponent, maxExponent);
        final DiagonalMatrix diagonalMatrix = new DiagonalMatrix(diagonalElements, false);

        // Random orthogonal matrix
        final RealMatrix orthogonalMatrix = getRandomOrthogonalMatrix(rng, dimension, base,
                -maxMantissa, maxMantissa, minExponent, maxExponent);
        final ArrayRowSymmetricMatrix transformedMatrix = diagonalMatrix
                .quadraticMultiplication(orthogonalMatrix);
        return new ArrayRowSymmetricPositiveMatrix(transformedMatrix.getDataRef(), false);
    }

    /**
     * Generates a random orthogonal matrix.
     *
     * @param rng
     *        the random number generator to use
     * @param dimension
     *        the dimension of the generated matrix
     * @param minValue
     *        the minimum value allowed
     * @param maxValue
     *        the maximum value allowed
     *
     * @return the generated matrix
     */
    public static RealMatrix getRandomOrthogonalMatrix(final Random rng, final int dimension,
            final double minValue, final double maxValue) {
        return getRandomOrthogonalMatrix(rng, dimension, DEFAULT_BASE, minValue, maxValue, 0, 0);
    }

    /**
     * Generates a random orthogonal matrix.
     *
     * @param rng
     *        the random number generator to use
     * @param dimension
     *        the dimension of the generated matrix
     * @param base
     *        the base in which the generated numbers are defined
     * @param minMantissa
     *        the minimum value allowed for the mantissa
     * @param maxMantissa
     *        the maximum value allowed for the mantissa
     * @param minExponent
     *        the minimum value allowed for the exponent
     * @param maxExponent
     *        the maximum value allowed for the exponent
     *
     * @return the generated matrix
     */
    public static RealMatrix getRandomOrthogonalMatrix(final Random rng, final int dimension,
            final double base, final double minMantissa, final double maxMantissa,
            final int minExponent, final int maxExponent) {
        final RealMatrix randomMatrix = getRandomMatrix(rng, dimension, dimension, base,
                minMantissa, maxMantissa, minExponent, maxExponent);
        final QRDecomposition decomposition = new QRDecomposition(randomMatrix, Precision.SAFE_MIN);
        return decomposition.getQ();
    }

    /**
     * Real matrix preserving visitor which counts the number of elements in a matrix and computes
     * an hash code like number reflecting the
     * order in which the entries were visited (without taking into account their values).
     */
    public static class PreservingVisitor implements RealMatrixPreservingVisitor {
        /** Visited elements counter. */
        private long nbVisited = 0;

        /** Hash-code like number built from the visited elements. */
        private long result = 0;

        /** {@inheritDoc} */
        @Override
        public void start(final int rows, final int columns, final int startRow, final int endRow,
                final int startColumn, final int endColumn) {
            this.nbVisited = 0;
            this.result = 0;
        }

        /** {@inheritDoc} */
        @Override
        public void visit(final int row, final int column, final double value) {
            this.nbVisited++;
            this.result = 3 * this.result + (5 * (row + 1) + 7 * (column + 1));
        }

        /** {@inheritDoc} */
        @Override
        public double end() {
            return 0.;
        }

        /**
         * Gets the number of visited elements.
         *
         * @return the number of visited elements
         */
        public long getNbVisited() {
            return this.nbVisited;
        }

        /**
         * Gets the hash-code like number built from the visited elements.
         *
         * @return the hash-code like number built from the visited elements
         */
        public long getResult() {
            return this.result;
        }
    }
}
