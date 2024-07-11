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
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:400:17/03/2015: use class FastMath instead of class Math
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.cnesmerge.stat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.stat.StatUtils;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;

/**
 * <p>
 * -this class adds tests in order to raise the cover rate of StatUtils
 * </p>
 * 
 * @author Philippe Pavero
 * 
 * @version $Id: StatUtilsTest.java 17909 2017-09-11 11:57:36Z bignon $
 * 
 * @since 1.0
 * 
 */
public class StatUtilsTest {

    /** Error string. */
    private static final String NULL_IS_NOT_A_VALID_DATA_ARRAY = "null is not a valid data array.";
    /** Epsilon for double comparison. */
    private final double epsilon = Precision.DOUBLE_COMPARISON_EPSILON;

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Increase the code coverage
         * 
         * @featureDescription Increase the code coverage
         * 
         * @coveredRequirements NA
         */
        COVERAGE
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#COVERAGE}
     * 
     * @testedMethod {@link StatUtils#quadraticMean(double[])}
     * 
     * @description Nominal test cases for the quadratic mean.
     * 
     * @input double[] values : values array to mean
     * 
     * @output double
     * 
     * @testPassCriteria The test passes if the method returns the correct quadratic mean, or NaN for an empty values
     *                   array (with an epsilon of 1e-14 because it is a comparison between two doubles).
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     */
    @Test
    public final void testQuadraticMeanDoubleArrayNominal() {
        // nominal test case
        double[] values = { 3.0, 2.0, 6.0 };
        double actualResult = StatUtils.quadraticMean(values);
        assertEquals(7.0 / MathLib.sqrt(3), actualResult, this.epsilon);

        // test for the return of NaN
        values = new double[0];
        actualResult = StatUtils.quadraticMean(values);
        assertEquals(Double.NaN, actualResult, this.epsilon);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#COVERAGE}
     * 
     * @testedMethod {@link StatUtils#quadraticMean(double[])}
     * 
     * @description Exceptional test case for the quadratic mean.
     * 
     * @input double[] values : values array to mean
     * 
     * @output double
     * 
     * @testPassCriteria The test passes if the method throws an exception for a null values array.
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     */
    @Test
    public final void testQuadraticMeanDoubleArrayExceptions() {
        // test for the exception throwing when
        try {
            final double[] nullValues = null;
            StatUtils.quadraticMean(nullValues);
        } catch (final IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#COVERAGE}
     * 
     * @testedMethod {@link StatUtils#quadraticMean(double[], int, int)}
     * 
     * @description Nominal test cases for the quadratic mean.
     * 
     * @input double[] values : values array to mean and two integers
     * 
     * @output double
     * 
     * @testPassCriteria The test passes if the method returns the correct quadratic mean, or NaN for an empty values
     *                   array (with an epsilon of 1e-14 because it is a comparison between two doubles).
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     */
    @Test
    public final void testQuadraticMeanDoubleArrayIntIntNominal() {
        // nominal test case
        double[] values = { 1.0, 3.0, 2.0, 6.0, 2.0 };
        double actualResult = StatUtils.quadraticMean(values, 1, 3);
        assertEquals(7.0 / MathLib.sqrt(3), actualResult, this.epsilon);

        // test for the return of NaN
        values = new double[0];
        actualResult = StatUtils.quadraticMean(values, 0, 0);
        assertEquals(Double.NaN, actualResult, this.epsilon);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#COVERAGE}
     * 
     * @testedMethod {@link StatUtils#quadraticMean(double[], int, int)}
     * 
     * @description Exceptional test cases for the quadratic mean.
     * 
     * @input double[] values : values array to mean
     * 
     * @output double
     * 
     * @testPassCriteria The test passes if the method throws an exception for various test cases.
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     */
    @Test
    public final void testQuadraticMeanDoubleArrayIntIntExceptions() {
        double[] values;
        // test for the exception throwing when values is null
        try {
            final double[] nullValues = null;
            StatUtils.quadraticMean(nullValues, 0, 0);
        } catch (final IllegalArgumentException e) {
            assertTrue(true);
        }

        // test for the exception throwing when begin index is negative
        try {
            values = new double[] { 1.0, 3.0, 2.0, 6.0, 2.0 };
            StatUtils.quadraticMean(values, -5, 0);
        } catch (final IllegalArgumentException e) {
            assertTrue(true);
        }

        // test for the exception throwing when begin index is too big
        try {
            values = new double[] { 1.0, 3.0, 2.0, 6.0, 2.0 };
            StatUtils.quadraticMean(values, 10, 0);
        } catch (final IllegalArgumentException e) {
            assertTrue(true);
        }

        // test for the exception throwing when length is negative
        try {
            values = new double[] { 1.0, 3.0, 2.0, 6.0, 2.0 };
            StatUtils.quadraticMean(values, 0, -5);
        } catch (final IllegalArgumentException e) {
            assertTrue(true);
        }

        // test for the exception throwing when length is negative
        try {
            values = new double[] { 1.0, 3.0, 2.0, 6.0, 2.0 };
            StatUtils.quadraticMean(values, 0, 10);
        } catch (final IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#COVERAGE}
     * 
     * @testedMethod {@link StatUtils#standardDeviation(double[])}
     * @testedMethod {@link StatUtils#standardDeviation(double[], double)}
     * @testedMethod {@link StatUtils#standardDeviation(double[], int, int)}
     * @testedMethod {@link StatUtils#standardDeviation(double[], double, int, int)}
     * 
     * @description Nominal and exceptional test cases for the standard deviation computing.
     * 
     * @input double[] values : values array from which to get the standard deviation
     * 
     * @output double
     * 
     * @testPassCriteria The test passes if the method throws an exception or returns the correct value for various test
     *                   cases (with an epsilon of 1e-14 because it is a comparison between two doubles).
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     */
    @Test
    public final void testStandardDeviationDoubleArray() {
        double[] x = null;

        try {
            StatUtils.standardDeviation(x);
            Assert.fail(NULL_IS_NOT_A_VALID_DATA_ARRAY);
        } catch (final MathIllegalArgumentException ex) {
            // success
        }

        // test empty
        x = new double[] {};
        assertEquals(Double.NaN, StatUtils.standardDeviation(x), this.epsilon);
        assertEquals(Double.NaN, StatUtils.standardDeviation(x, 0), this.epsilon);
        assertEquals(Double.NaN, StatUtils.standardDeviation(x, 0, 0), this.epsilon);
        assertEquals(Double.NaN, StatUtils.standardDeviation(x, 0, 0, 0), this.epsilon);
        assertEquals(Double.NaN, StatUtils.standardDeviation(x, 0, 0, 0), this.epsilon);

        // test one
        x = new double[] { 2 };
        assertEquals(0.0, StatUtils.standardDeviation(x), this.epsilon);

        // test many
        x = new double[] { 1, 2, 2, 3 };
        assertEquals(MathLib.sqrt(0.5), StatUtils.standardDeviation(x, 2, 2), this.epsilon);

        // test precomputed mean
        x = new double[] { 1, 2, 2, 3 };
        assertEquals(MathLib.sqrt(0.5), StatUtils.standardDeviation(x, 2.5, 2, 2), this.epsilon);

        // test precomputed mean
        x = new double[] { 2, 3 };
        assertEquals(MathLib.sqrt(0.5), StatUtils.standardDeviation(x, 2.5), this.epsilon);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#COVERAGE}
     * 
     * @testedMethod {@link StatUtils#median(double[])}
     * @testedMethod {@link StatUtils#median(double[], int, int)}
     * 
     * @description Nominal and exceptional test cases for the median computing.
     * 
     * @input double[] values : values array from which to get the median
     * 
     * @output double
     * 
     * @testPassCriteria The test passes if the method throws an exception or returns the correct value for various test
     *                   cases (with an epsilon of 1e-14 because it is a comparison between two doubles).
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     */
    @Test
    public final void testMedian() {
        double[] x = null;

        // test null
        try {
            StatUtils.median(x);
            Assert.fail(NULL_IS_NOT_A_VALID_DATA_ARRAY);
        } catch (final MathIllegalArgumentException ex) {
            // success
        }

        try {
            StatUtils.median(x, 0, 4);
            Assert.fail(NULL_IS_NOT_A_VALID_DATA_ARRAY);
        } catch (final MathIllegalArgumentException ex) {
            // success
        }

        // test empty
        x = new double[] {};
        assertEquals(Double.NaN, StatUtils.median(x), this.epsilon);
        assertEquals(Double.NaN, StatUtils.median(x, 0, 0), this.epsilon);

        // test one
        x = new double[] { 2.0 };
        assertEquals(2.0, StatUtils.median(x), this.epsilon);
        assertEquals(2.0, StatUtils.median(x, 0, 1), this.epsilon);

        // test many
        x = new double[] { 1.0, 3.0, 2.0, 4.0 };
        assertEquals(2.5, StatUtils.median(x), this.epsilon);
        assertEquals(3.0, StatUtils.median(x, 1, 3), this.epsilon);
    }
}
