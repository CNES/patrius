/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
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
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.DecimalFormat;

import org.junit.Assert;

import fr.cnes.sirius.patrius.math.complex.Complex;
import fr.cnes.sirius.patrius.math.complex.ComplexFormat;
import fr.cnes.sirius.patrius.math.distribution.RealDistribution;
import fr.cnes.sirius.patrius.math.linear.FieldMatrix;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealVector;
import fr.cnes.sirius.patrius.math.stat.inference.ChiSquareTest;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;

/**
 * @version $Id: TestUtils.java 16013 2016-05-10 17:27:46Z goudot $
 */
public class TestUtils {
    /**
     * Collection of static methods used in math unit tests.
     */
    private TestUtils() {
        super();
    }

    /**
     * Verifies that expected and actual are within delta, or are both NaN or
     * infinities of the same sign.
     */
    public static void assertEquals(final double expected, final double actual, final double delta) {
        Assert.assertEquals(null, expected, actual, delta);
    }

    /**
     * Verifies that expected and actual are within delta, or are both NaN or
     * infinities of the same sign.
     */
    public static void assertEquals(final String msg, final double expected, final double actual, final double delta) {
        // check for NaN
        if (Double.isNaN(expected)) {
            Assert.assertTrue("" + actual + " is not NaN.",
                Double.isNaN(actual));
        } else {
            Assert.assertEquals(msg, expected, actual, delta);
        }
    }

    /**
     * Verifies that the two arguments are exactly the same, either
     * both NaN or infinities of same sign, or identical floating point values.
     */
    public static void assertSame(final double expected, final double actual) {
        Assert.assertEquals(expected, actual, 0);
    }

    /**
     * Verifies that real and imaginary parts of the two complex arguments
     * are exactly the same. Also ensures that NaN / infinite components match.
     */
    public static void assertSame(final Complex expected, final Complex actual) {
        assertSame(expected.getReal(), actual.getReal());
        assertSame(expected.getImaginary(), actual.getImaginary());
    }

    /**
     * Verifies that real and imaginary parts of the two complex arguments
     * differ by at most delta. Also ensures that NaN / infinite components match.
     */
    public static void assertEquals(final Complex expected, final Complex actual, final double delta) {
        Assert.assertEquals(expected.getReal(), actual.getReal(), delta);
        Assert.assertEquals(expected.getImaginary(), actual.getImaginary(), delta);
    }

    /**
     * Verifies that two double arrays have equal entries, up to tolerance
     */
    public static void assertEquals(final double expected[], final double observed[], final double tolerance) {
        assertEquals("Array comparison failure", expected, observed, tolerance);
    }

    /**
     * Serializes an object to a bytes array and then recovers the object from the bytes array.
     * Returns the deserialized object.
     * 
     * @param o
     *        object to serialize and recover
     * @return the recovered, deserialized object
     */
    public static Object serializeAndRecover(final Object o) {
        try {
            // serialize the Object
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            final ObjectOutputStream so = new ObjectOutputStream(bos);
            so.writeObject(o);

            // deserialize the Object
            final ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
            final ObjectInputStream si = new ObjectInputStream(bis);
            return si.readObject();
        } catch (final IOException ioe) {
            System.out.println("error ioe serializeAndRecover ");
            ioe.printStackTrace();
            return null;
        } catch (final ClassNotFoundException cnfe) {
            System.out.println("error cnfe serializeAndRecover ");
            cnfe.printStackTrace();
            return null;
        }
    }

    /**
     * Verifies that serialization preserves equals and hashCode.
     * Serializes the object, then recovers it and checks equals and hash code.
     * 
     * @param object
     *        the object to serialize and recover
     */
    public static void checkSerializedEquality(final Object object) {
        final Object object2 = serializeAndRecover(object);
        Assert.assertEquals("Equals check", object, object2);
        Assert.assertEquals("HashCode check", object.hashCode(), object2.hashCode());
    }

    /**
     * Verifies that the relative error in actual vs. expected is less than or
     * equal to relativeError. If expected is infinite or NaN, actual must be
     * the same (NaN or infinity of the same sign).
     * 
     * @param expected
     *        expected value
     * @param actual
     *        observed value
     * @param relativeError
     *        maximum allowable relative error
     */
    public static void assertRelativelyEquals(final double expected, final double actual,
                                              final double relativeError) {
        assertRelativelyEquals(null, expected, actual, relativeError);
    }

    /**
     * Verifies that the relative error in actual vs. expected is less than or
     * equal to relativeError. If expected is infinite or NaN, actual must be
     * the same (NaN or infinity of the same sign).
     * 
     * @param msg
     *        message to return with failure
     * @param expected
     *        expected value
     * @param actual
     *        observed value
     * @param relativeError
     *        maximum allowable relative error
     */
    public static void assertRelativelyEquals(final String msg, final double expected,
                                              final double actual, final double relativeError) {
        if (Double.isNaN(expected)) {
            Assert.assertTrue(msg, Double.isNaN(actual));
        } else if (Double.isNaN(actual)) {
            Assert.assertTrue(msg, Double.isNaN(expected));
        } else if (Double.isInfinite(actual) || Double.isInfinite(expected)) {
            Assert.assertEquals(expected, actual, relativeError);
        } else if (expected == 0.0) {
            Assert.assertEquals(msg, actual, expected, relativeError);
        } else {
            final double absError = MathLib.abs(expected) * relativeError;
            Assert.assertEquals(msg, expected, actual, absError);
        }
    }

    /**
     * Fails iff values does not contain a number within epsilon of z.
     * 
     * @param msg
     *        message to return with failure
     * @param values
     *        complex array to search
     * @param z
     *        value sought
     * @param epsilon
     *        tolerance
     */
    public static void assertContains(final String msg, final Complex[] values,
                                      final Complex z, final double epsilon) {
        for (final Complex value : values) {
            if (Precision.equals(value.getReal(), z.getReal(), epsilon) &&
                Precision.equals(value.getImaginary(), z.getImaginary(), epsilon)) {
                return;
            }
        }
        Assert.fail(msg + " Unable to find " + (new ComplexFormat()).format(z));
    }

    /**
     * Fails iff values does not contain a number within epsilon of z.
     * 
     * @param values
     *        complex array to search
     * @param z
     *        value sought
     * @param epsilon
     *        tolerance
     */
    public static void assertContains(final Complex[] values,
                                      final Complex z, final double epsilon) {
        assertContains(null, values, z, epsilon);
    }

    /**
     * Fails iff values does not contain a number within epsilon of x.
     * 
     * @param msg
     *        message to return with failure
     * @param values
     *        double array to search
     * @param x
     *        value sought
     * @param epsilon
     *        tolerance
     */
    public static void assertContains(final String msg, final double[] values,
                                      final double x, final double epsilon) {
        for (final double value : values) {
            if (Precision.equals(value, x, epsilon)) {
                return;
            }
        }
        Assert.fail(msg + " Unable to find " + x);
    }

    /**
     * Fails iff values does not contain a number within epsilon of x.
     * 
     * @param values
     *        double array to search
     * @param x
     *        value sought
     * @param epsilon
     *        tolerance
     */
    public static void assertContains(final double[] values, final double x,
                                      final double epsilon) {
        assertContains(null, values, x, epsilon);
    }

    /**
     * Asserts that all entries of the specified vectors are equal to within a
     * positive {@code delta}.
     * 
     * @param message
     *        the identifying message for the assertion error (can be {@code null})
     * @param expected
     *        expected value
     * @param actual
     *        actual value
     * @param delta
     *        the maximum difference between the entries of the expected
     *        and actual vectors for which both entries are still considered equal
     */
    public static void assertEquals(final String message,
                                    final double[] expected, final RealVector actual, final double delta) {
        final String msgAndSep = message.equals("") ? "" : message + ", ";
        Assert.assertEquals(msgAndSep + "dimension", expected.length,
            actual.getDimension());
        for (int i = 0; i < expected.length; i++) {
            Assert.assertEquals(msgAndSep + "entry #" + i, expected[i],
                actual.getEntry(i), delta);
        }
    }

    /**
     * Asserts that all entries of the specified vectors are equal to within a
     * positive {@code delta}.
     * 
     * @param message
     *        the identifying message for the assertion error (can be {@code null})
     * @param expected
     *        expected value
     * @param actual
     *        actual value
     * @param delta
     *        the maximum difference between the entries of the expected
     *        and actual vectors for which both entries are still considered equal
     */
    public static void assertEquals(final String message,
                                    final RealVector expected, final RealVector actual, final double delta) {
        final String msgAndSep = message.equals("") ? "" : message + ", ";
        Assert.assertEquals(msgAndSep + "dimension", expected.getDimension(),
            actual.getDimension());
        final int dim = expected.getDimension();
        for (int i = 0; i < dim; i++) {
            Assert.assertEquals(msgAndSep + "entry #" + i,
                expected.getEntry(i), actual.getEntry(i), delta);
        }
    }

    /** verifies that two matrices are close (1-norm) */
    public static void assertEquals(final String msg, final RealMatrix expected, final RealMatrix observed,
                                    final double tolerance) {

        Assert.assertNotNull(msg + "\nObserved should not be null", observed);

        if (expected.getColumnDimension() != observed.getColumnDimension() ||
            expected.getRowDimension() != observed.getRowDimension()) {
            final StringBuilder messageBuffer = new StringBuilder(msg);
            messageBuffer.append("\nObserved has incorrect dimensions.");
            messageBuffer.append("\nobserved is " + observed.getRowDimension() +
                " x " + observed.getColumnDimension());
            messageBuffer.append("\nexpected " + expected.getRowDimension() +
                " x " + expected.getColumnDimension());
            Assert.fail(messageBuffer.toString());
        }

        final RealMatrix delta = expected.subtract(observed);
        if (delta.getNorm() >= tolerance) {
            final StringBuilder messageBuffer = new StringBuilder(msg);
            messageBuffer.append("\nExpected: " + expected);
            messageBuffer.append("\nObserved: " + observed);
            messageBuffer.append("\nexpected - observed: " + delta);
            Assert.fail(messageBuffer.toString());
        }
    }

    /** verifies that two matrices are equal */
    public static void assertEquals(final FieldMatrix<? extends FieldElement<?>> expected,
                                    final FieldMatrix<? extends FieldElement<?>> observed) {

        Assert.assertNotNull("Observed should not be null", observed);

        if (expected.getColumnDimension() != observed.getColumnDimension() ||
            expected.getRowDimension() != observed.getRowDimension()) {
            final StringBuilder messageBuffer = new StringBuilder();
            messageBuffer.append("Observed has incorrect dimensions.");
            messageBuffer.append("\nobserved is " + observed.getRowDimension() +
                " x " + observed.getColumnDimension());
            messageBuffer.append("\nexpected " + expected.getRowDimension() +
                " x " + expected.getColumnDimension());
            Assert.fail(messageBuffer.toString());
        }

        for (int i = 0; i < expected.getRowDimension(); ++i) {
            for (int j = 0; j < expected.getColumnDimension(); ++j) {
                final FieldElement<?> eij = expected.getEntry(i, j);
                final FieldElement<?> oij = observed.getEntry(i, j);
                Assert.assertEquals(eij, oij);
            }
        }
    }

    /** verifies that two arrays are close (sup norm) */
    public static void assertEquals(final String msg, final double[] expected, final double[] observed,
                                    final double tolerance) {
        final StringBuilder out = new StringBuilder(msg);
        if (expected.length != observed.length) {
            out.append("\n Arrays not same length. \n");
            out.append("expected has length ");
            out.append(expected.length);
            out.append(" observed length = ");
            out.append(observed.length);
            Assert.fail(out.toString());
        }
        boolean failure = false;
        for (int i = 0; i < expected.length; i++) {
            if (!Precision.equalsIncludingNaN(expected[i], observed[i], tolerance)) {
                failure = true;
                out.append("\n Elements at index ");
                out.append(i);
                out.append(" differ. ");
                out.append(" expected = ");
                out.append(expected[i]);
                out.append(" observed = ");
                out.append(observed[i]);
            }
        }
        if (failure) {
            Assert.fail(out.toString());
        }
    }

    /** verifies that two arrays are equal */
    public static <T extends FieldElement<T>> void assertEquals(final T[] m, final T[] n) {
        if (m.length != n.length) {
            Assert.fail("vectors not same length");
        }
        for (int i = 0; i < m.length; i++) {
            Assert.assertEquals(m[i], n[i]);
        }
    }

    /**
     * Computes the sum of squared deviations of <values> from <target>
     * 
     * @param values
     *        array of deviates
     * @param target
     *        value to compute deviations from
     * 
     * @return sum of squared deviations
     */
    public static double sumSquareDev(final double[] values, final double target) {
        double sumsq = 0d;
        for (final double value : values) {
            final double dev = value - target;
            sumsq += (dev * dev);
        }
        return sumsq;
    }

    /**
     * Asserts the null hypothesis for a ChiSquare test. Fails and dumps arguments and test
     * statistics if the null hypothesis can be rejected with confidence 100 * (1 - alpha)%
     * 
     * @param valueLabels
     *        labels for the values of the discrete distribution under test
     * @param expected
     *        expected counts
     * @param observed
     *        observed counts
     * @param alpha
     *        significance level of the test
     */
    public static void assertChiSquareAccept(final String[] valueLabels, final double[] expected,
                                             final long[] observed, final double alpha) {
        final ChiSquareTest chiSquareTest = new ChiSquareTest();

        // Fail if we can reject null hypothesis that distributions are the same
        if (chiSquareTest.chiSquareTest(expected, observed, alpha)) {
            final StringBuilder msgBuffer = new StringBuilder();
            final DecimalFormat df = new DecimalFormat("#.##");
            msgBuffer.append("Chisquare test failed");
            msgBuffer.append(" p-value = ");
            msgBuffer.append(chiSquareTest.chiSquareTest(expected, observed));
            msgBuffer.append(" chisquare statistic = ");
            msgBuffer.append(chiSquareTest.chiSquare(expected, observed));
            msgBuffer.append(". \n");
            msgBuffer.append("value\texpected\tobserved\n");
            for (int i = 0; i < expected.length; i++) {
                msgBuffer.append(valueLabels[i]);
                msgBuffer.append("\t");
                msgBuffer.append(df.format(expected[i]));
                msgBuffer.append("\t\t");
                msgBuffer.append(observed[i]);
                msgBuffer.append("\n");
            }
            msgBuffer.append("This test can fail randomly due to sampling error with probability ");
            msgBuffer.append(alpha);
            msgBuffer.append(".");
            Assert.fail(msgBuffer.toString());
        }
    }

    /**
     * Asserts the null hypothesis for a ChiSquare test. Fails and dumps arguments and test
     * statistics if the null hypothesis can be rejected with confidence 100 * (1 - alpha)%
     * 
     * @param values
     *        integer values whose observed and expected counts are being compared
     * @param expected
     *        expected counts
     * @param observed
     *        observed counts
     * @param alpha
     *        significance level of the test
     */
    public static void assertChiSquareAccept(final int[] values, final double[] expected, final long[] observed,
                                             final double alpha) {
        final String[] labels = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            labels[i] = Integer.toString(values[i]);
        }
        assertChiSquareAccept(labels, expected, observed, alpha);
    }

    /**
     * Asserts the null hypothesis for a ChiSquare test. Fails and dumps arguments and test
     * statistics if the null hypothesis can be rejected with confidence 100 * (1 - alpha)%
     * 
     * @param expected
     *        expected counts
     * @param observed
     *        observed counts
     * @param alpha
     *        significance level of the test
     */
    public static void assertChiSquareAccept(final double[] expected, final long[] observed, final double alpha) {
        final String[] labels = new String[expected.length];
        for (int i = 0; i < labels.length; i++) {
            labels[i] = Integer.toString(i + 1);
        }
        assertChiSquareAccept(labels, expected, observed, alpha);
    }

    /**
     * Computes the 25th, 50th and 75th percentiles of the given distribution and returns
     * these values in an array.
     */
    public static double[] getDistributionQuartiles(final RealDistribution distribution) {
        final double[] quantiles = new double[3];
        quantiles[0] = distribution.inverseCumulativeProbability(0.25d);
        quantiles[1] = distribution.inverseCumulativeProbability(0.5d);
        quantiles[2] = distribution.inverseCumulativeProbability(0.75d);
        return quantiles;
    }

    /**
     * Updates observed counts of values in quartiles.
     * counts[0] <-> 1st quartile ... counts[3] <-> top quartile
     */
    public static void updateCounts(final double value, final long[] counts, final double[] quartiles) {
        if (value < quartiles[0]) {
            counts[0]++;
        } else if (value > quartiles[2]) {
            counts[3]++;
        } else if (value > quartiles[1]) {
            counts[2]++;
        } else {
            counts[1]++;
        }
    }

    /**
     * Eliminates points with zero mass from densityPoints and densityValues parallel
     * arrays. Returns the number of positive mass points and collapses the arrays so
     * that the first <returned value> elements of the input arrays represent the positive
     * mass points.
     */
    public static int eliminateZeroMassPoints(final int[] densityPoints, final double[] densityValues) {
        int positiveMassCount = 0;
        for (final double densityValue : densityValues) {
            if (densityValue > 0) {
                positiveMassCount++;
            }
        }
        if (positiveMassCount < densityValues.length) {
            final int[] newPoints = new int[positiveMassCount];
            final double[] newValues = new double[positiveMassCount];
            int j = 0;
            for (int i = 0; i < densityValues.length; i++) {
                if (densityValues[i] > 0) {
                    newPoints[j] = densityPoints[i];
                    newValues[j] = densityValues[i];
                    j++;
                }
            }
            System.arraycopy(newPoints, 0, densityPoints, 0, positiveMassCount);
            System.arraycopy(newValues, 0, densityValues, 0, positiveMassCount);
        }
        return positiveMassCount;
    }
}
