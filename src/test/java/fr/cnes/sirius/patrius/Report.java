/**
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
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Locale;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.geometry.euclidean.twod.Vector2D;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Class handling test report writing.
 * 
 * @author Emmanuel Bignon
 * @version $Id: Report.java 18088 2017-10-02 17:01:51Z bignon $
 */
public class Report {

    /** Header line. */
    private static final String HEADER_LINE =
        "=========================================================================================================================";

    /** Sub-header line. */
    private static final String SUB_HEADER_LINE =
        "    ---------------------------------------------------------------------------------------------------------------------";

    /** Column size. */
    private static final int COLUMN_SIZE = 23;

    /** ID size. */
    private static final int ID_SIZE = 37;

    /** Buffered output to which the report should be written. */
    private static BufferedWriter bufferedWriter;

    /** Scientific notation formatter. */
    private static DecimalFormat formatter;

    /** Comparison type. */
    private static ComparisonType comparisonType;

    static {
        // Create report file
        try {
            final File file = File.createTempFile("Report", ".txt");
            System.out.println("Report location: " + file.getAbsolutePath());
            bufferedWriter = new BufferedWriter(new FileWriter(file));
        } catch (final IOException e) {
            throw new RuntimeException("Failed to create report.");
        }

        // Report header
        // System.out.println("Version ${project.version}");

        // Formatter
        formatter = new DecimalFormat("0.################E0");

        // Comparison type
        comparisonType = null;
    }

    /**
     * Private constructor.
     */
    private Report() {
    }

    // ========================================= PUBLIC METHODS ========================================= //

    /**
     * Prints class header.
     * 
     * @param className
     *        class name
     * @param classPurpose
     *        class purpose
     */
    public static void printClassHeader(final String className, final String classPurpose) {
        print(HEADER_LINE);
        print("Test class name    : " + className);
        print("Test class purpose : " + classPurpose);
        print(HEADER_LINE);
    }

    /**
     * Prints test header.
     * 
     * @param methodName
     *        method name
     * @param purpose
     *        purpose
     * @param reference
     *        reference
     * @param threshold
     *        threshold
     * @param differenceType
     *        comparison type
     */
    public static void printMethodHeader(final String methodName, final String purpose, final String reference,
                                         final double threshold, final ComparisonType differenceType) {
        print(SUB_HEADER_LINE);
        print("    Test method name : " + methodName);
        print("    Test purpose     : " + purpose);
        print("    Test threshold   : " + threshold + " (" + differenceType + ")");
        print("    Test reference   : " + reference);
        print(SUB_HEADER_LINE);
        print("Tag", "Reference", "Actual", "Difference");
        comparisonType = differenceType;
    }

    /**
     * Prints report line (double).
     * 
     * @param id
     *        identifier (max length: {@link #ID_SIZE}
     * @param expected
     *        expected
     * @param actual
     *        actual
     */
    public static void printToReport(final String id, final double expected, final double actual) {
        double eps;

        // NaN case treatment
        if (Double.isNaN(expected) || Double.isNaN(actual)) {
            if (Double.isNaN(expected) && Double.isNaN(actual)) {
                print(id, "NaN", "NaN", "-");
            } else if (Double.isNaN(expected) && !Double.isNaN(actual)) {
                print(id, "NaN", formatter.format(actual), "NaN");
            } else if (!Double.isNaN(expected) && Double.isNaN(actual)) {
                print(id, formatter.format(expected), "NaN", "NaN");
            }
            return;
        }

        if (Double.isInfinite(expected) || Double.isInfinite(actual)) {
            // Infinite case treatment
            if (expected == actual) {
                eps = 0;
            } else {
                eps = Double.POSITIVE_INFINITY;
            }
        } else {
            // Regular case - Compute difference
            eps = MathLib.abs(expected - actual);
            if (comparisonType == ComparisonType.RELATIVE && expected != 0) {
                eps = MathLib.abs(eps / expected);
            }
        }
        print(id, formatter.format(expected), formatter.format(actual), formatter.format(eps));
    }

    /**
     * Prints report line (String).
     * 
     * @param id
     *        identifier (max length: {@link #ID_SIZE}
     * @param expected
     *        expected
     * @param actual
     *        actual
     */
    public static void printToReport(final String id, final String expected, final String actual) {
        print(id, expected, actual, "-");
    }

    /**
     * Prints report line (double[]).
     * 
     * @param id
     *        identifier (max length: {@link #ID_SIZE}
     * @param expected
     *        expected
     * @param actual
     *        actual
     */
    public static void printToReport(final String id, final double[] expected, final double[] actual) {
        for (int i = 0; i < expected.length; i++) {
            printToReport(id + " [" + i + "]", expected[i], actual[i]);
        }
    }

    /**
     * Prints report line (double[][]).
     * 
     * @param id
     *        identifier (max length: {@link #ID_SIZE}
     * @param expected
     *        expected
     * @param actual
     *        actual
     */
    public static void printToReport(final String id, final double[][] expected, final double[][] actual) {
        for (int i = 0; i < expected.length; i++) {
            for (int j = 0; j < expected.length; j++) {
                printToReport(id + " [" + i + "][" + j + "]", expected[i][j], actual[i][j]);
            }
        }
    }

    /**
     * Prints report line (int).
     * 
     * @param id
     *        identifier (max length: {@link #ID_SIZE}
     * @param expected
     *        expected
     * @param actual
     *        actual
     */
    public static void printToReport(final String id, final int expected, final int actual) {
        final int eps = MathLib.abs(actual - expected);
        print(id, String.valueOf(expected), String.valueOf(actual), String.valueOf(eps));
    }

    /**
     * Prints report line (Vector3D).
     * 
     * @param id
     *        identifier (max length: {@link #ID_SIZE}
     * @param expected
     *        expected
     * @param actual
     *        actual
     */
    public static void printToReport(final String id, final Vector3D expected, final Vector3D actual) {
        printToReport(id + " (X)", expected.getX(), actual.getX());
        printToReport(id + " (Y)", expected.getY(), actual.getY());
        printToReport(id + " (Z)", expected.getZ(), actual.getZ());
    }

    /**
     * Prints report line (Vector2D).
     * 
     * @param id
     *        identifier (max length: {@link #ID_SIZE}
     * @param expected
     *        expected
     * @param actual
     *        actual
     */
    public static void printToReport(final String id, final Vector2D expected, final Vector2D actual) {
        printToReport(id + " (X)", expected.getX(), actual.getX());
        printToReport(id + " (Y)", expected.getY(), actual.getY());
    }

    /**
     * Prints report line (Rotation).
     * 
     * @param id
     *        identifier (max length: {@link #ID_SIZE}
     * @param expected
     *        expected
     * @param actual
     *        actual
     */
    public static void printToReport(final String id, final Rotation expected, final Rotation actual) {
        printToReport(id + " (Q0)", expected.getQuaternion().getQ0(), actual.getQuaternion().getQ0());
        printToReport(id + " (Q1)", expected.getQuaternion().getQ1(), actual.getQuaternion().getQ1());
        printToReport(id + " (Q2)", expected.getQuaternion().getQ2(), actual.getQuaternion().getQ2());
        printToReport(id + " (Q3)", expected.getQuaternion().getQ3(), actual.getQuaternion().getQ3());
    }

    // ========================================= PRIVATE METHODS ========================================= //

    /**
     * Prints report line (string).
     * 
     * @param id
     *        identifier (max length: {@link #ID_SIZE}
     * @param expected
     *        expected
     * @param actual
     *        actual
     * @param eps
     *        difference between actual and expected (absolute or relative)
     */
    private static void print(final String id, final String expected, final String actual, final String eps) {
        print(String.format(Locale.US, "    %s | %s | %s | %s |",
            extendID(id), extendString(expected), extendString(actual), extendString(eps)));
    }

    /**
     * Writes a line.
     * 
     * @param s
     *        the string to write
     */
    private static void print(final String s) {
        System.out.println(s);
        try {
            bufferedWriter.write(s + '\n');
            bufferedWriter.flush();
        } catch (final IOException e) {
            throw new RuntimeException("Failed to write in report.");
        }
    }

    /**
     * Extend identifier to match {@link #ID_SIZE}.
     * 
     * @param id
     *        identifier
     * @return extended identifier
     */
    private static String extendID(final String id) {
        return extend(id, ID_SIZE);
    }

    /**
     * Extend string to match {@link #COLUMN_SIZE}.
     * 
     * @param s
     *        string
     * @return extended string
     */
    private static String extendString(final String s) {
        return extend(s, COLUMN_SIZE);
    }

    /**
     * Extend string to match provided size.
     * 
     * @param s
     *        string
     * @param size
     *        required string size
     * @return extended string
     */
    private static String extend(final String s, final int size) {
        String extendedS = s;
        while (extendedS.length() < size) {
            extendedS += " ";
        }
        return extendedS;
    }
}
