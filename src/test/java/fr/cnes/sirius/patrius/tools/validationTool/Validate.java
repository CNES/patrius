/**
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
 * 
 *
 * @history file creation, 03/08/11, Philippe Pavero
 * HISTORY
 * VERSION:4.5:FA:FA-2366:27/05/2020:Le scope de la dependance a JUnit devrait être « test » 
 * VERSION:4.3:FA:FA-2113:15/05/2019:Usage des exceptions pour gerer if/else "nominal" dans Ellipsoid.getPointLocation()
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:410:16/04/2015: Anomalies in the Patrius Javadoc
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.tools.validationTool;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;

import org.junit.Assert;

/**
 * <p>
 * The purpose of this class is to automate the logging of deviations in order to see their evolution. This way, the
 * developers can more easily ensure that a class is still valid even if its components or the classes it uses evolve.
 * This is an extension of the continuous integration to validation.
 * </p>
 * <p>
 * For each useful method of Assert, Validate provides an implementation that will compute and store the deviation, then
 * call the Assert method. At the end of the test (usually in an @AfterClass method), produceLog() has to be called,
 * otherwise the validation results are not written.
 * </p>
 * <p>
 * This class is intended as a replacement of the org.junit.Assert class for validation purposes. Its use is similar to
 * the Assert methods of JUnit.
 * </p>
 * 
 * @concurrency not thread-safe
 * 
 * @concurrency.comment two threads can add in the same time their computed deviations instead of adding them one after
 *                      the other one
 * 
 * @see Assert
 * 
 * @author Philippe Pavero
 * 
 * @version $Id: Validate.java 17578 2017-05-10 12:20:20Z bignon $
 * 
 * @since 1.0
 * 
 */
public class Validate extends Assert {

    /** String RESULTS_OF_THE_TESTS_CONDUCTED_IN */
    private static final String RESULTS_OF_THE_TESTS_CONDUCTED_IN = "=====Results of the tests conducted in ";

    /** String TEST_FAILED */
    private static final String TEST_FAILED = " : test failed";

    /** String TERM */
    private static final String TERM = "======";

    /** String LOG */
    private static final String LOG = ".log";

    /** Offset to order signed double numbers lexicographically. */
    private static final long SGN_MASK = 0x8000000000000000L;

    /**
     * Directory as a basis for output
     */
    private static final String OUTPUT_DIR = TempDirectory.
        getTemporaryDirectory("pdb.validate.results", "validationLog");

    /**
     * contains the name of the test class that uses the Validate object.
     * This name will be printed in the log.
     */
    private final Class<?> testClass;

    /**
     * contains all the deviations computed by the test class.
     */
    private final ArrayList<Deviation> deviationLogNonReg;

    /**
     * contains all the deviations computed by the test class.
     */
    private final ArrayList<Deviation> deviationLogExternalRef;

    /**
     * constructor of the Validate object.
     * Uses the properties file which path is given.
     * The resource is first searched in the classpath, then outside.
     * Therefore, a resource in the classpath might hide a resource ouside of it, but it is highly unlikely.
     * 
     * @param testClassIn
     *        the test class, which name will be stored to be printed in the logs.
     * @throws IOException
     *         if an error occurs during the reading of the file
     * @since 1.0
     */
    public Validate(final Class<?> testClassIn) throws IOException {
        // initializes the basic fields
        this.testClass = testClassIn;
        this.deviationLogNonReg = new ArrayList<Validate.Deviation>();
        this.deviationLogExternalRef = new ArrayList<Validate.Deviation>();
    }

    /**
     * Works as a replacement to the {@link Assert#assertEquals(double, double, double)} method.
     * Compute the deviation and store it to be put in the logs by
     * the produceLogs method. If the test fails, the deviation is still stored,
     * but " : test failed" is added to the message.
     * 
     * @param actual
     *        the value obtained in the test
     * @param nonRegExpected
     *        the value expected by the non-regression reference
     * @param nonRegEps
     *        the epsilon allowed with regard to the non-regression reference
     * @param externalRefExpected
     *        the value expected by the external reference
     * @param externalRefEps
     *        the epsilon allowed with regard to the external reference
     * @param deviationDescription
     *        the description of the deviation
     * 
     * @see Assert#assertEquals(double, double, double)
     * 
     * @since 1.0
     */
    public final void assertEquals(final double actual,
                                   final double nonRegExpected,
                                   final double nonRegEps, final double externalRefExpected,
                                   final double externalRefEps, final String deviationDescription) {

        // Compute absolute deviation wrt to reference
        double deviationValue = Math.abs(actual - externalRefExpected);
        try {
            // Check vs reference
            assertEquals(externalRefExpected, actual, externalRefEps);
        } catch (final AssertionError e) {
            this.deviationLogExternalRef.add(new Deviation(deviationDescription + TEST_FAILED,
                deviationValue, externalRefEps));
            deviationValue = Math.abs(actual - nonRegExpected);
            this.deviationLogNonReg.add(new Deviation(deviationDescription + TEST_FAILED,
                deviationValue, nonRegEps));
            throw e;
        }
        this.deviationLogExternalRef.add(new Deviation(deviationDescription,
            deviationValue, externalRefEps));

        // Compute absolute deviation wrt to non-regression
        deviationValue = Math.abs(actual - nonRegExpected);
        try {
            // Check vs non-regression
            assertEquals(nonRegExpected, actual, nonRegEps);
        } catch (final AssertionError e) {
            this.deviationLogNonReg.add(new Deviation(deviationDescription + TEST_FAILED,
                deviationValue, nonRegEps));
            throw e;
        }
        this.deviationLogNonReg.add(new Deviation(deviationDescription,
            deviationValue, nonRegEps));
    }

    /**
     * Compute the deviation of each table value
     * and store it to be put in the logs by the produceLogs method.
     * 
     * @param actual
     *        the array values obtained in the test
     * @param nonRegExpected
     *        the array values expected by the non-regression reference
     * @param nonRegEps
     *        the epsilon allowed with regard to the non-regression reference
     * @param externalRefExpected
     *        the array values expected by the external reference
     * @param externalRefEps
     *        the epsilon allowed with regard to the external reference
     * @param deviationDescription
     *        the description of the deviation
     * 
     * @see Assert#assertEquals(double, double, double)
     * 
     * @since 1.0
     */
    public final void assertEqualsArray(final double[] actual,
                                        final double[] nonRegExpected,
                                        final double nonRegEps, final double[] externalRefExpected,
                                        final double externalRefEps, final String deviationDescription) {

        if (actual.length == nonRegExpected.length && actual.length == externalRefExpected.length) {
            for (int i = 0; i < actual.length; i++) {
                this.assertEquals(actual[i], nonRegExpected[i],
                    nonRegEps, externalRefExpected[i], externalRefEps, deviationDescription);
            }
        }
    }

    /**
     * Works as a replacement to
     * the {@link Assert#assertEquals(double, double, double)} method
     * for a relative comparison.
     * Compute the deviation and store it to be put in the logs by
     * the produceLogs method. If the test fails,
     * the deviation is still stored, but " : test failed" is added
     * to the message.
     * 
     * @param actual
     *        the value obtained in the test
     * @param nonRegExpected
     *        the value expected by the non-regression reference
     * @param nonRegEps
     *        the epsilon allowed with regard to the non-regression reference
     * @param externalRefExpected
     *        the value expected by the external reference
     * @param externalRefEps
     *        the epsilon allowed with regard to the external reference
     * @param deviationDescription
     *        the description of the deviation
     * 
     * @see Assert#assertEquals(double, double, double)
     * 
     * @since 1.0
     */
    public final void assertEqualsWithRelativeTolerance(final double actual,
            final double nonRegExpected, final double nonRegEps, final double externalRefExpected,
            final double externalRefEps, final String deviationDescription) {
        boolean equals;

        // Compute relative deviation wrt to reference
        double deviationValueRef = 0.0;
        final double maxRef = Math.max(actual, externalRefExpected);
        if (maxRef != 0.0) {
            deviationValueRef = Math.abs(actual - externalRefExpected) / maxRef;
        }

        // Compute relative deviation wrt to non-regression
        double deviationValueReg = 0.0;
        final double maxReg = Math.max(actual, nonRegExpected);
        if (maxReg != 0.0) {
            deviationValueReg = Math.abs(actual - nonRegExpected) / maxReg;
        }

        try {
            // Check vs reference
            equals = equalsWithRelativeTolerance(actual, externalRefExpected, externalRefEps);
            assertTrue(equals);
        } catch (final AssertionError e) {
            this.deviationLogExternalRef.add(new Deviation(deviationDescription + TEST_FAILED,
                deviationValueRef, externalRefEps));
            this.deviationLogNonReg.add(new Deviation(deviationDescription + TEST_FAILED,
                deviationValueReg, nonRegEps));
            throw e;
        }
        this.deviationLogExternalRef.add(new Deviation(deviationDescription,
            deviationValueRef, externalRefEps));

        // Compute absolute deviation wrt to non-regression
        try {
            // Check vs non-regression
            equals = equalsWithRelativeTolerance(nonRegExpected, actual, nonRegEps);
            assertTrue(equals);
        } catch (final AssertionError e) {
            this.deviationLogNonReg.add(new Deviation(deviationDescription + TEST_FAILED,
                deviationValueReg, nonRegEps));
            throw e;
        }
        this.deviationLogNonReg.add(new Deviation(deviationDescription, deviationValueReg, nonRegEps));
    }

    /**
     * prints the log in the console and in files.
     * These logs are organised in their package tree, all of it put in the
     * directory given in the setup file.
     * 
     * @throws IOException
     *         if there was an error in the manipulation of the files
     * @throws URISyntaxException
     *         if there was an error in the parsing of the URI of the files
     * 
     * @since 1.0
     */
    public final void produceLog() throws IOException, URISyntaxException {
        this.produceLog(null);
    }

    /**
     * prints the log in the console and in files.
     * These logs are organised in their package tree, all of it put in the
     * directory given in the setup file.
     * 
     * @param logDir
     *        name of log file
     * 
     * @throws IOException
     *         if there was an error in the manipulation of the files
     * @throws URISyntaxException
     *         if there was an error in the parsing of the URI of the files
     * 
     * @since 1.0
     */
    public final void produceLog(final String logDir) throws IOException, URISyntaxException {
        System.out.println(RESULTS_OF_THE_TESTS_CONDUCTED_IN
            + this.testClass.getSimpleName() + TERM);

        final String directory;

        if (logDir == null) {
            // add the package as containing directory for this test
            directory = OUTPUT_DIR + File.separator
                + this.testClass.getPackage().getName();
        } else {
            directory = OUTPUT_DIR + File.separator + logDir;
        }

        // build a File object for this directory
        final File dir = new File(directory);
        final boolean status = dir.mkdirs();
        if (!status) {
            System.out.println("mkdir failed");
        }

        /*
         * add the log for the non-regression case
         */

        // get the path for this directory and add the name of the log to write
        String filePath = dir.getPath() + File.separator + this.testClass.getSimpleName() + "NonReg" + LOG;

        System.out.println(filePath);

        // build a File object from this path
        File log = new File(filePath);

        // get a writer for this log
        BufferedWriter writer = new BufferedWriter(new FileWriter(log));

        // write this header in the log
        writer.write(RESULTS_OF_THE_TESTS_CONDUCTED_IN + this.testClass.getSimpleName() + TERM);
        writer.newLine();

        // for each computed deviation
        for (final Deviation deviation : this.deviationLogNonReg) {
            // write it in the console
            System.out.println(deviation.toString());

            // write it in the log
            writer.write(deviation.toString());
            writer.newLine();
        }

        // finally, close the stream
        writer.close();

        /*
         * add the log for the external reference case
         */

        // build a File object from this path
        filePath = dir.getPath() + File.separator + this.testClass.getSimpleName() + "ExternalRef" + LOG;
        System.out.println(filePath);
        log = new File(filePath);

        // get a writer for this log
        writer = new BufferedWriter(new FileWriter(log));

        // write this header in the log
        writer.write(RESULTS_OF_THE_TESTS_CONDUCTED_IN + this.testClass.getSimpleName() + TERM);
        writer.newLine();

        // for each computed deviation
        for (final Deviation deviation : this.deviationLogExternalRef) {
            // write it in the console
            System.out.println(deviation.toString());

            // write it in the log
            writer.write(deviation.toString());
            writer.newLine();
        }

        // finally, close the stream
        writer.close();
    }

    /**
     * Compute the deviation of each table value
     * and store it to be put in the logs by the produceLogs method,
     * but with a relative tolerance.
     * 
     * @param actual
     *        the array values obtained in the test
     * @param nonRegExpected
     *        the array values expected by the non-regression reference
     * @param nonRegEps
     *        the epsilon allowed with regard to the non-regression reference
     * @param externalRefExpected
     *        the array values expected by the external reference
     * @param externalRefEps
     *        the epsilon allowed with regard to the external reference
     * @param deviationDescription
     *        the description of the deviation
     * 
     * @see Assert#assertEquals(double, double, double)
     * 
     * @since 1.0
     */
    public final void assertEqualsArrayWithRelativeTolerance(final double[] actual,
                                                             final double[] nonRegExpected,
                                                             final double nonRegEps,
                                                             final double[] externalRefExpected,
                                                             final double externalRefEps,
                                                             final String deviationDescription) {

        if (actual.length == nonRegExpected.length && actual.length == externalRefExpected.length) {
            for (int i = 0; i < actual.length; i++) {
                this.assertEqualsWithRelativeTolerance(actual[i], nonRegExpected[i],
                    nonRegEps, externalRefExpected[i], externalRefEps, deviationDescription);
            }
        }
    }

    /**
     * Returns {@code true} if there is no double value strictly between the
     * arguments or the difference between them is within the range of allowed
     * error (inclusive).
     * (method from Commons-Math).
     *
     * @param x First value.
     * @param y Second value.
     * @param eps Amount of allowed absolute error.
     * @return {@code true} if the values are two adjacent floating point
     *         numbers or they are within range of each other.
     */
    public static boolean equals(final double x, final double y, final double eps) {
        final double gap;
        if (!Double.isNaN(x) && !Double.isNaN(y)) {
            gap = Math.abs(y - x);
        } else {
            gap = Double.NaN;
        }
        return equals(x, y, 1) || gap <= eps;
    }

    /**
     * Returns true if both arguments are equal or within the range of allowed
     * error (inclusive).
     * Two float numbers are considered equal if there are {@code (maxUlps - 1)} (or fewer) floating point numbers
     * between them, i.e. two adjacent floating
     * point numbers are considered equal.
     * Adapted from <a
     * href="http://www.cygnus-software.com/papers/comparingfloats/comparingfloats.htm">
     * Bruce Dawson</a>
     *
     * @param x first value
     * @param y second value
     * @param maxUlps {@code (maxUlps - 1)} is the number of floating point
     *        values between {@code x} and {@code y}.
     * @return {@code true} if there are fewer than {@code maxUlps} floating
     *         point values between {@code x} and {@code y}.
     */
    public static boolean equals(final double x, final double y, final int maxUlps) {
        long xInt = Double.doubleToLongBits(x);
        long yInt = Double.doubleToLongBits(y);

        // Make lexicographically ordered as a two's-complement integer.
        if (xInt < 0) {
            xInt = SGN_MASK - xInt;
        }
        if (yInt < 0) {
            yInt = SGN_MASK - yInt;
        }

        final boolean isEqual = Math.abs(xInt - yInt) <= maxUlps;

        return isEqual && !Double.isNaN(x) && !Double.isNaN(y);
    }

    /**
     * Returns {@code true} if there is no double value strictly between the
     * arguments or the reltaive difference between them is smaller or equal
     * to the given tolerance.
     * (method from Commons-Math).
     *
     * @param x First value.
     * @param y Second value.
     * @param eps Amount of allowed relative error.
     * @return {@code true} if the values are two adjacent floating point
     *         numbers or they are within range of each other.
     * @since 3.1
     */
    public static boolean equalsWithRelativeTolerance(final double x, final double y, final double eps) {
        // check if the two values are equal or adjacent
        if (equals(x, y, 1)) {
            return true;
        }

        // Get absolute max between x and y
        double absoluteMax;
        try {
            absoluteMax = Math.max(Math.abs(x), Math.abs(y));
        } catch (final ArithmeticException e) {
            absoluteMax = Double.NaN;
        }

        // Get relative difference 
        double relativeDifference;
        try {
            relativeDifference = Math.abs((x - y) / absoluteMax);
        } catch (final ArithmeticException e) {
            relativeDifference = Double.NaN;
        }

        // return true if the relative difference if lower or equal to the given tolerance
        return relativeDifference <= eps;
    }

    /**
     * <p>
     * This private inner class contains a deviation's value and description
     * </p>
     * 
     * @concurrency immutable
     * 
     * @author Philippe Pavero
     * 
     * @version $Id: Validate.java 17578 2017-05-10 12:20:20Z bignon $
     * 
     * @since 1.0
     * 
     */
    private static final class Deviation {

        /** contains the description of the deviation */
        private final String description;

        /**
         * contains the deviation value
         */
        private final double deviation;

        /**
         * contains the allowed epsilon
         */
        private final double epsilon;

        /**
         * build a simple instance of DeviationLog
         * 
         * @param descriptionValue
         *        of the deviation
         * @param deviationValue
         *        the value of the deviation
         * @param epsilonValue
         *        the value of the maximum deviation allowed
         * 
         * @since 1.0
         */
        private Deviation(final String descriptionValue,
            final double deviationValue, final double epsilonValue) {
            this.description = descriptionValue;
            this.deviation = deviationValue;
            this.epsilon = epsilonValue;
        }

        /**
         * build the String representation of the DeviationLog,
         * for instance : "1.01351e-9 1.0E-8 ERAdeviation"
         * 
         * @return String
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {

            final String tab = "\t";
            return this.deviation + tab + this.epsilon + tab + this.description;
        }
    }
}
