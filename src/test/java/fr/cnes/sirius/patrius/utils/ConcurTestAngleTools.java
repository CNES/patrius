/**
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
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.utils;

import static org.junit.Assert.assertEquals;

import java.util.Random;

import org.testng.Assert;
import org.testng.annotations.Test;

import fr.cnes.sirius.patrius.math.Comparators;
import fr.cnes.sirius.patrius.math.exception.MathArithmeticException;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.geometry.euclidean.twod.Vector2D;
import fr.cnes.sirius.patrius.math.interval.AngleInterval;
import fr.cnes.sirius.patrius.math.interval.AngleTools;
import fr.cnes.sirius.patrius.math.interval.IntervalEndpointType;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;

/**
 * Concurrency tests for the AngleTools class.
 * 
 * @author cardosop
 * 
 * @version $Id: ConcurTestAngleTools.java 17918 2017-09-11 13:04:41Z bignon $
 * 
 * @since 1.1
 * 
 */
public class ConcurTestAngleTools {

    /** Error string. */
    private static final String EXPECTING_MATH_ILLEGAL_ARGUMENT_EXCEPTION_BUT_GOT =
        "expecting MathIllegalArgumentException but got : ";
    /** Error string. */
    private static final String EXPECTING_MATH_ARITHMETIC_EXCEPTION_BUT_GOT =
        "expecting MathArithmeticException but got : ";
    /** Error string. */
    private static final String ANGLE_SHOULD_NOT_BE_DEFINED_NA_N = "angle should not be defined : NaN";

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Computation of angles
         * 
         * @featureDescription Computes an angle between 2 vectors of R2 or R3, from a sine or a cosine.
         * 
         * @coveredRequirements DV-MATHS_100
         */
        ANGLE_COMPUTATION,

        /**
         * @featureTitle Angle in interval
         * 
         * @featureDescription Computes the 2PI modulo in a given interval.
         * 
         * @coveredRequirements DV-MATHS_60
         */
        ANGLE_IN_INTERVAL,

        /**
         * @featureTitle Angle comparisons
         * 
         * @featureDescription 5 method to compare angles in the same interval.
         * 
         * @coveredRequirements DV-MATHS_90
         */
        ANGLE_COMPARISONS,

        /**
         * @featureTitle Angle operations with intervals
         * 
         * @featureDescription Supplementary, complementary and opposite angles computation in an interval.
         * 
         * @coveredRequirements DV-MATHS_80
         */
        ANGLE_OPERATION
    }

    /** Number of trials. */
    private static final int NUMBER_OF_TRIALS = 10;

    /** Thread pool for TestNG. */
    private static final int NUMBER_OF_THREADS = 10;

    /** Invocations for TestNG. */
    private static final int NUMBER_OF_INVOCATIONS = 1000;

    /** Time-out for TestNG (in case of deadlocks) (milliseconds). */
    private static final int TIMEOUT = 10000;

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ANGLE_COMPUTATION}
     * 
     * @testedMethod {@link AngleTools#getAngleBewteen2Vector3D(Vector3D, Vector3D)}
     * 
     * @description Nominal cases and degraded cases for angle computation between 2 vectors 3D (without orientation).
     * 
     * @input Vecstor3D vector1 : real values, very small values, very large values, NaN value, infinite value, vector
     *        with a zero norm.
     * @input Vecstor3D vector2 : real values, very small values, very large values, NaN value, infinite value, vector
     *        with a zero norm.
     * 
     * @output Double angle : defined on [0, PI].
     * 
     * @testPassCriteria The angle computed is one which is expected.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test(invocationCount = NUMBER_OF_INVOCATIONS, threadPoolSize = NUMBER_OF_THREADS, timeOut = TIMEOUT)
    public final void testGetAngleBetween2Vector3D() {

        // nominal cases
        final Vector3D vector1 = new Vector3D(1, 0, 0);
        final Vector3D vector2 = new Vector3D(0, 1, 0);

        assertEquals(AngleTools.getAngleBewteen2Vector3D(vector1, vector2), MathUtils.HALF_PI,
            Comparators.DOUBLE_COMPARISON_EPSILON);
        assertEquals(AngleTools.getAngleBewteen2Vector3D(vector2, vector1), MathUtils.HALF_PI,
            Comparators.DOUBLE_COMPARISON_EPSILON);

        assertEquals(AngleTools.getAngleBewteen2Vector3D(vector1, vector1.negate()), FastMath.PI,
            Comparators.DOUBLE_COMPARISON_EPSILON);
        assertEquals(AngleTools.getAngleBewteen2Vector3D(vector1, vector1), 0.0, Comparators.DOUBLE_COMPARISON_EPSILON);

        // one coordinate with a very small value
        final Vector3D vector3 = new Vector3D(0.0000000000001, 0, 0);
        assertEquals(AngleTools.getAngleBewteen2Vector3D(vector3, vector2), MathUtils.HALF_PI,
            Comparators.DOUBLE_COMPARISON_EPSILON);

        // one coordinate with a very large value
        final Vector3D vector4 = new Vector3D(100000000000000000.0, 0, 0);
        assertEquals(AngleTools.getAngleBewteen2Vector3D(vector4, vector2), MathUtils.HALF_PI,
            Comparators.DOUBLE_COMPARISON_EPSILON);

        double x;

        // one coordinate is not a number (NaN)
        final Vector3D vector6 = new Vector3D(Double.NaN, 0, 0);
        x = AngleTools.getAngleBewteen2Vector3D(vector1, vector6);
        if (x == x) {
            throw new RuntimeException(ANGLE_SHOULD_NOT_BE_DEFINED_NA_N);
        }
        // one vector has a zero norm
        final Vector3D vector5 = Vector3D.ZERO;
        try {
            x = AngleTools.getAngleBewteen2Vector3D(vector1, vector5);
            Assert.fail(EXPECTING_MATH_ARITHMETIC_EXCEPTION_BUT_GOT + x);
        } catch (final MathArithmeticException ex) {
            // expected
        }
        try {
            x = AngleTools.getAngleBewteen2Vector3D(vector5, vector1);
            Assert.fail(EXPECTING_MATH_ARITHMETIC_EXCEPTION_BUT_GOT + x);
        } catch (final MathArithmeticException ex) {
            // expected
        }
        // one coordinate is +infinity
        final Vector3D vector7 = new Vector3D(Double.POSITIVE_INFINITY, 0, 0);
        x = AngleTools.getAngleBewteen2Vector3D(vector1, vector7);
        if (x == x) {
            throw new RuntimeException(ANGLE_SHOULD_NOT_BE_DEFINED_NA_N);
        }
        // one coordinate is -infinity
        final Vector3D vector8 = new Vector3D(Double.NEGATIVE_INFINITY, 0, 0);
        x = AngleTools.getAngleBewteen2Vector3D(vector8, vector1);
        if (x == x) {
            throw new RuntimeException(ANGLE_SHOULD_NOT_BE_DEFINED_NA_N);
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ANGLE_COMPUTATION}
     * 
     * @testedMethod {@link AngleTools#getOrientedAngleBetween2Vector2D(Vector2D, Vector2D)}
     * 
     * @description Cases and degraded cases for oriented angle computation between 2 vectors 2D.
     * 
     * @input Vecstor3D vector1 : real values, very small values, very large values, NaN value, infinite value, vector
     *        with a zero norm.
     * @input Vecstor3D vector2 : real values, very small values, very large values, NaN value, infinite value, vector
     *        with a zero norm.
     * 
     * @output Double angle : defined on [-2 PI, 2 PI].
     * 
     * @testPassCriteria The angle computed is one which is expected.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test(invocationCount = NUMBER_OF_INVOCATIONS, threadPoolSize = NUMBER_OF_THREADS, timeOut = TIMEOUT)
    public final void testGetOrientedAngleBetween2Vector2D() {

        // nominal cases
        final Vector2D vector1 = new Vector2D(2, 2);
        final Vector2D vector2 = new Vector2D(0, 1);

        assertEquals(AngleTools.getOrientedAngleBetween2Vector2D(vector1, vector2), FastMath.PI / 4.0,
            Comparators.DOUBLE_COMPARISON_EPSILON);
        assertEquals(AngleTools.getOrientedAngleBetween2Vector2D(vector2, vector1), -FastMath.PI / 4.0,
            Comparators.DOUBLE_COMPARISON_EPSILON);
        assertEquals(AngleTools.getOrientedAngleBetween2Vector2D(vector1, vector1), 0.0,
            Comparators.DOUBLE_COMPARISON_EPSILON);

        double x;

        // one coordinate is not a number (NaN)
        final Vector2D vector4 = new Vector2D(1, Double.NaN);
        x = AngleTools.getOrientedAngleBetween2Vector2D(vector1, vector4);
        if (x == x) {
            throw new RuntimeException(ANGLE_SHOULD_NOT_BE_DEFINED_NA_N);
        }
        // one vector has a zero norm
        final Vector2D vector3 = new Vector2D(0, 0);
        try {
            x = AngleTools.getOrientedAngleBetween2Vector2D(vector3, vector2);
            Assert.fail(EXPECTING_MATH_ILLEGAL_ARGUMENT_EXCEPTION_BUT_GOT + x);
        } catch (final MathIllegalArgumentException ex) {
            // expected
        }
        try {
            x = AngleTools.getOrientedAngleBetween2Vector2D(vector2, vector3);
            Assert.fail(EXPECTING_MATH_ILLEGAL_ARGUMENT_EXCEPTION_BUT_GOT + x);
        } catch (final MathIllegalArgumentException ex) {
            // expected
        }
        // one coordinate is +infinity
        final Vector2D vector5 = new Vector2D(Double.POSITIVE_INFINITY, 1);
        try {
            x = AngleTools.getOrientedAngleBetween2Vector2D(vector1, vector5);
            Assert.fail(EXPECTING_MATH_ILLEGAL_ARGUMENT_EXCEPTION_BUT_GOT + x);
        } catch (final MathIllegalArgumentException ex) {
            // expected
        }
        // one coordinate is -infinity
        final Vector2D vector6 = new Vector2D(1, Double.NEGATIVE_INFINITY);
        try {
            x = AngleTools.getOrientedAngleBetween2Vector2D(vector6, vector1);
            Assert.fail(EXPECTING_MATH_ILLEGAL_ARGUMENT_EXCEPTION_BUT_GOT + x);
        } catch (final MathIllegalArgumentException ex) {
            // expected
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ANGLE_COMPUTATION}
     * 
     * @testedMethod {@link AngleTools#getOrientedAngleBewteen2Vector3D(Vector3D, Vector3D, Vector3D)}
     * 
     * @description Nominal cases and degraded cases for oriented angle computation between 2 vectors 3D.
     * 
     * @input Vecstor3D vector1 : real values.
     * @input Vecstor3D vector2 : real values.
     * @input Vecstor3D vector3 : real values, NaN value, infinite value, vector with a zero norm, normal to the plane
     *        formed by the others, non normal to the plane formed by the others.
     * 
     * @output Double angle : defined on [-PI, PI].
     * 
     * @testPassCriteria The angle computed is one which is expected.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test(invocationCount = NUMBER_OF_INVOCATIONS, threadPoolSize = NUMBER_OF_THREADS, timeOut = TIMEOUT)
    public final void testGetOrientedAngleBetween2Vector3D() {

        // nominal cases with canonical vectors
        assertEquals(AngleTools.getOrientedAngleBewteen2Vector3D(Vector3D.PLUS_I, Vector3D.PLUS_J, Vector3D.PLUS_K),
            MathUtils.HALF_PI, Comparators.DOUBLE_COMPARISON_EPSILON);
        assertEquals(AngleTools.getOrientedAngleBewteen2Vector3D(Vector3D.PLUS_I, Vector3D.PLUS_J, Vector3D.MINUS_K),
            -MathUtils.HALF_PI, Comparators.DOUBLE_COMPARISON_EPSILON);

        // nominal cases with ordinary vectors
        double x;
        double y;

        final Vector3D vector1 = new Vector3D(7, 5.4, 1.2);
        final Vector3D vector2 = new Vector3D(1.4, 8, 9.6);

        x = AngleTools.getOrientedAngleBewteen2Vector3D(vector1, vector2, Vector3D.crossProduct(vector1, vector2));
        y = AngleTools.getAngleBewteen2Vector3D(vector1, vector2);
        assertEquals(x, y, Comparators.DOUBLE_COMPARISON_EPSILON);

        x = AngleTools.getOrientedAngleBewteen2Vector3D(vector1, vector2, Vector3D.crossProduct(vector1, vector2)
            .negate());
        assertEquals(x, -y, Comparators.DOUBLE_COMPARISON_EPSILON);

        // the third vector is not orthogonal to the plane defined by the others
        final Vector3D vector6 = new Vector3D(4, 8.3, 5.2);
        try {
            x = AngleTools.getOrientedAngleBewteen2Vector3D(vector1, vector2, vector6);
            Assert.fail("expecting IllegalArgumentException but got : " + x);
        } catch (final IllegalArgumentException ex) {
            // expected
        }

        // one vector has an infinite coordinate
        final Vector3D vector7 = new Vector3D(Double.POSITIVE_INFINITY, 0, 0);
        x = AngleTools.getOrientedAngleBewteen2Vector3D(vector1, vector2, vector7);
        if (x == x) {
            throw new RuntimeException(ANGLE_SHOULD_NOT_BE_DEFINED_NA_N);
        }

        // one vector has a coordinate which is not a number (NaN)
        final Vector3D vector9 = new Vector3D(Double.NaN, 0, 0);
        x = AngleTools.getOrientedAngleBewteen2Vector3D(vector1, vector2, vector9);
        if (x == x) {
            throw new RuntimeException(ANGLE_SHOULD_NOT_BE_DEFINED_NA_N);
        }

        // one vector is the vector null
        try {
            x = AngleTools.getOrientedAngleBewteen2Vector3D(vector1, vector2, Vector3D.ZERO);
            Assert.fail(EXPECTING_MATH_ARITHMETIC_EXCEPTION_BUT_GOT + x);
        } catch (final MathArithmeticException ex) {
            // expected
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ANGLE_COMPUTATION}
     * 
     * @testedMethod {@link AngleTools#getAngleFromCosineAndSine(double, double)}
     * 
     * @description Nominal cases and degraded cases for angles computed from a cosine and a sine.
     * 
     * @input The inputs are real numbers given by the cosine and the sine of a random angle. The case when the
     *        inputs are a cosine and a sine of different angles is tested. The limits cases when the inputs are the
     *        cosine and the sine of {@code 0}, {@code 2 PI}, {@code PI} and {@code -PI} are tested.
     * 
     * @output Double angle : defined between {@code -PI} and {@code PI}.
     * 
     * @testPassCriteria The angle used to compute the cosine and the sine given as entries of the tested function.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test(invocationCount = NUMBER_OF_INVOCATIONS, threadPoolSize = NUMBER_OF_THREADS, timeOut = TIMEOUT)
    public final void testGetAngleFromCosineAndSine() {

        // nominal cases
        final Random rand = new Random();
        double angle;
        double x;
        for (int i = 0; i < NUMBER_OF_TRIALS; i++) {
            angle = 2 * (rand.nextDouble() - 0.5) * FastMath.PI;
            x = AngleTools.getAngleFromCosineAndSine(MathLib.cos(angle), MathLib.sin(angle));
            assertEquals(x, angle, Comparators.DOUBLE_COMPARISON_EPSILON);
        }

        // cosine and sine of different angles
        try {
            x = AngleTools.getAngleFromCosineAndSine(MathLib.cos(30 * MathUtils.DEG_TO_RAD),
                MathLib.sin(45 * MathUtils.DEG_TO_RAD));
            Assert.fail(EXPECTING_MATH_ILLEGAL_ARGUMENT_EXCEPTION_BUT_GOT + x * MathUtils.RAD_TO_DEG + " °.");
        } catch (final MathIllegalArgumentException ex) {
            // expected
        }

        // cosine and sine of different angles which are closed
        try {
            x = AngleTools.getAngleFromCosineAndSine(MathLib.cos(30 * MathUtils.DEG_TO_RAD),
                MathLib.sin(30.0001 * MathUtils.DEG_TO_RAD));
            Assert.fail(EXPECTING_MATH_ILLEGAL_ARGUMENT_EXCEPTION_BUT_GOT + x);
        } catch (final MathIllegalArgumentException ex) {
            // expected
        }

        // limit case : cosine and sine of {@code PI}
        x = AngleTools.getAngleFromCosineAndSine(MathLib.cos(FastMath.PI), MathLib.sin(FastMath.PI));
        assertEquals(x, FastMath.PI, Comparators.DOUBLE_COMPARISON_EPSILON);

        // limit case : cosine and sine of {@code -PI}
        x = AngleTools.getAngleFromCosineAndSine(MathLib.cos(-FastMath.PI), MathLib.sin(-FastMath.PI));
        assertEquals(x, -FastMath.PI, Comparators.DOUBLE_COMPARISON_EPSILON);

        // limit case : cosine and sine of {@code 2 PI}
        x = AngleTools.getAngleFromCosineAndSine(MathLib.cos(MathUtils.TWO_PI), MathLib.sin(MathUtils.TWO_PI));
        assertEquals(x, 0.0, Comparators.DOUBLE_COMPARISON_EPSILON);

        // limit case : cosine and sine of {@code 0}
        x = AngleTools.getAngleFromCosineAndSine(MathLib.cos(0.0), MathLib.sin(0.0));
        assertEquals(x, 0.0, Comparators.DOUBLE_COMPARISON_EPSILON);

        // the angle from cos(30°) and sin(-30°) is seen as -30°
        x = AngleTools.getAngleFromCosineAndSine(MathLib.cos(30 * MathUtils.DEG_TO_RAD),
            MathLib.sin(-30 * MathUtils.DEG_TO_RAD));
        assertEquals(x, -30 * MathUtils.DEG_TO_RAD, Comparators.DOUBLE_COMPARISON_EPSILON);

        // the angle from cos(30°) and sin(210°) is seen as -30°
        x = AngleTools.getAngleFromCosineAndSine(MathLib.cos(30 * MathUtils.DEG_TO_RAD),
            MathLib.sin(210 * MathUtils.DEG_TO_RAD));
        assertEquals(x, -30 * MathUtils.DEG_TO_RAD, Comparators.DOUBLE_COMPARISON_EPSILON);

        // one entry is NaN
        x = AngleTools.getAngleFromCosineAndSine(Double.NaN, 1);
        if (x == x) {
            throw new RuntimeException(ANGLE_SHOULD_NOT_BE_DEFINED_NA_N);
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ANGLE_IN_INTERVAL}
     * 
     * @testedMethod {@link AngleTools#angleInInterval(double, AngleInterval)}
     * 
     * @description Test of the modulo function with nominal and error cases
     * 
     * @input The inputs are a double (angle) and an interval. Several doubles are tested : one whose 2PI modulo belongs
     *        to the interval, and some when an exception must be thrown.
     * 
     * @output Double angle expressed in given interval.
     * 
     * @testPassCriteria <p>
     *                   The first test must return a double res, with :<br>
     *                    res = angle (2PI)<br>
     *                    and res belongs to the given interval<br>
     *                   In the second test, the right exception must be caught.
     *                   </p>
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test(invocationCount = NUMBER_OF_INVOCATIONS, threadPoolSize = NUMBER_OF_THREADS, timeOut = TIMEOUT)
    public final void angleInIntervalTest() {
        final double eps = 1.0e-13;

        // First test : nominal
        try {
            // Angle
            final double angle = 6 * FastMath.PI - 0.1;
            // Interval creation
            final AngleInterval angleInterval = new AngleInterval(IntervalEndpointType.OPEN, -MathUtils.HALF_PI,
                MathUtils.HALF_PI, IntervalEndpointType.OPEN);
            // modulo
            final double res = AngleTools.angleInInterval(angle, angleInterval);

            Assert.assertEquals(-0.1, res, eps);
        } catch (final MathIllegalArgumentException e) {
            Assert.fail();
        }
        try {
            // Angle
            final double angle = 6 * FastMath.PI - 0.1;
            // Interval creation
            final AngleInterval angleInterval = new AngleInterval(IntervalEndpointType.CLOSED, -MathUtils.HALF_PI,
                MathUtils.HALF_PI, IntervalEndpointType.CLOSED);
            // modulo
            final double res = AngleTools.angleInInterval(angle, angleInterval);

            Assert.assertEquals(-0.1, res, eps);
        } catch (final MathIllegalArgumentException e) {
            Assert.fail();
        }

        // Second test : modulo outside the interval
        // Four cases are needed to cover all possibilities
        // of the method "isIn" used by AngleTools.angleInInterval

        // First Case : 2 OPENED end points and a greater corrected angle
        try {
            // Interval creation
            final AngleInterval angleInterval = new AngleInterval(IntervalEndpointType.OPEN, -MathUtils.HALF_PI,
                MathUtils.HALF_PI, IntervalEndpointType.OPEN);
            AngleTools.angleInInterval(7 * FastMath.PI, angleInterval);
            Assert.fail();
        } catch (final MathIllegalArgumentException e) {
            // test OK !
        }
        // Second Case : 2 CLOSED end points and a greater corrected angle
        try {
            // Interval creation
            final AngleInterval angleInterval = new AngleInterval(IntervalEndpointType.CLOSED, -MathUtils.HALF_PI,
                MathUtils.HALF_PI, IntervalEndpointType.CLOSED);
            AngleTools.angleInInterval(7 * FastMath.PI, angleInterval);
            Assert.fail();
        } catch (final MathIllegalArgumentException e) {
            // test OK !
        }
        // Third Case : 2 CLOSED end points and a lower corrected angle
        try {
            // Interval creation
            final AngleInterval angleInterval = new AngleInterval(IntervalEndpointType.CLOSED, -MathUtils.HALF_PI,
                MathUtils.HALF_PI, IntervalEndpointType.CLOSED);
            AngleTools.angleInInterval(-MathUtils.HALF_PI - 0.1, angleInterval);
            Assert.fail();
        } catch (final MathIllegalArgumentException e) {
            // test OK !
        }
        // Fourth Case : 2 OPENED end points and a lower corrected angle
        try {
            // Interval creation
            final AngleInterval angleInterval = new AngleInterval(IntervalEndpointType.OPEN, -MathUtils.HALF_PI,
                MathUtils.HALF_PI, IntervalEndpointType.OPEN);
            AngleTools.angleInInterval(-MathUtils.HALF_PI - 0.1, angleInterval);
            Assert.fail();
        } catch (final MathIllegalArgumentException e) {
            // test OK !
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ANGLE_COMPARISONS}
     * 
     * @testedMethod {@link AngleTools#equal(double, double, AngleInterval)}
     * 
     * @description <p>
     *              Three test cases :<br>
     *               - the angles can be expressed in the interval and are equal<br>
     *               - the angles can be expressed in the interval and are not equal<br>
     *               - one of the angles can't be expressed in the interval<br>
     *              </p>
     * 
     * @input 2 doubles (the angles) and a the AngleInterval.
     * 
     * @output Boolean set to true if the angles are equal once expressed in the interval, false otherwise.
     * 
     * @testPassCriteria For the first test, the boolean returned must be true, for the second it must be false, for the
     *                   third,
     *                   the right exception must be thrown.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test(invocationCount = NUMBER_OF_INVOCATIONS, threadPoolSize = NUMBER_OF_THREADS, timeOut = TIMEOUT)
    public final void equalTest() {

        // First test
        try {
            // Interval creation
            final AngleInterval angleInterval = new AngleInterval(IntervalEndpointType.OPEN, -MathUtils.HALF_PI,
                MathUtils.HALF_PI, IntervalEndpointType.OPEN);
            // test
            final boolean isOk = AngleTools.equal(0.0, 6 * FastMath.PI, angleInterval);
            Assert.assertEquals(isOk, true);
        } catch (final MathIllegalArgumentException e) {
            Assert.fail();
        }
        // Second test
        try {
            // Interval creation
            final AngleInterval angleInterval = new AngleInterval(IntervalEndpointType.OPEN, -MathUtils.HALF_PI,
                MathUtils.HALF_PI, IntervalEndpointType.OPEN);
            // test
            final boolean isOk = AngleTools.equal(0.0, 6 * FastMath.PI + 0.1, angleInterval);
            Assert.assertEquals(isOk, false);
        } catch (final MathIllegalArgumentException e) {
            Assert.fail();
        }
        // Third test
        try {
            // Interval creation
            final AngleInterval angleInterval = new AngleInterval(IntervalEndpointType.OPEN, -MathUtils.HALF_PI,
                MathUtils.HALF_PI, IntervalEndpointType.OPEN);
            AngleTools.equal(0.0, 7 * FastMath.PI, angleInterval);
            Assert.fail();
        } catch (final MathIllegalArgumentException e) {
            // Test ok !
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ANGLE_COMPARISONS}
     * 
     * @testedMethod {@link AngleTools#lowerOrEqual(double, double, AngleInterval)}
     * 
     * @description <p>
     *              Three test cases :<br>
     *               - the angles can be expressed in the interval and a < b<br>
     *               - the angles can be expressed in the interval and a > b<br>
     *               - one of the angles can't be expressed in the interval<br>
     *              </p>
     * 
     * @input 2 doubles (the angles) and a the AngleInterval.
     * 
     * @output Boolean set to true if a <= b once expressed in the interval, false otherwise.
     * 
     * @testPassCriteria For the first test, the boolean returned must be true, for the second it must be false, for the
     *                   third,
     *                   the right exception must be thrown.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test(invocationCount = NUMBER_OF_INVOCATIONS, threadPoolSize = NUMBER_OF_THREADS, timeOut = TIMEOUT)
    public final void lowerOrEqualTest() {

        // First test
        try {
            // Interval creation
            final AngleInterval angleInterval = new AngleInterval(IntervalEndpointType.OPEN, -MathUtils.HALF_PI,
                MathUtils.HALF_PI, IntervalEndpointType.OPEN);
            // test
            final boolean isOk = AngleTools.lowerOrEqual(0.0, 6 * FastMath.PI + 0.1, angleInterval);
            Assert.assertEquals(isOk, true);
        } catch (final MathIllegalArgumentException e) {
            Assert.fail();
        }
        // Second test
        try {
            // Interval creation
            final AngleInterval angleInterval = new AngleInterval(IntervalEndpointType.OPEN, -MathUtils.HALF_PI,
                MathUtils.HALF_PI, IntervalEndpointType.OPEN);
            // test
            final boolean isOk = AngleTools.lowerOrEqual(0.0, 6 * FastMath.PI - 0.1, angleInterval);
            Assert.assertEquals(isOk, false);
        } catch (final MathIllegalArgumentException e) {
            Assert.fail();
        }
        // Third test
        try {
            // Interval creation
            final AngleInterval angleInterval = new AngleInterval(IntervalEndpointType.OPEN, -MathUtils.HALF_PI,
                MathUtils.HALF_PI, IntervalEndpointType.OPEN);
            AngleTools.lowerOrEqual(0.0, 7 * FastMath.PI, angleInterval);
            Assert.fail();
        } catch (final MathIllegalArgumentException e) {
            // Test ok !
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ANGLE_COMPARISONS}
     * 
     * @testedMethod {@link AngleTools#lowerStrict(double, double, AngleInterval)}
     * 
     * @description <p>
     *              Three test cases :<br>
     *              - the angles can be expressed in the interval and a < b<br>
     *              - the angles can be expressed in the interval and a > b<br>
     *              - one of the angles can't be expressed in the interval<br>
     *              </p>
     * 
     * @input 2 doubles (the angles) and a the AngleInterval.
     * 
     * @output Boolean set to true if a < b once expressed in the interval, false otherwise.
     * 
     * @testPassCriteria For the first test, the boolean returned must be true, for the second it must be false, for the
     *                   third,
     *                   the right exception must be thrown.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test(invocationCount = NUMBER_OF_INVOCATIONS, threadPoolSize = NUMBER_OF_THREADS, timeOut = TIMEOUT)
    public final void lowerStrictTest() {

        // First test
        try {
            // Interval creation
            final AngleInterval angleInterval = new AngleInterval(IntervalEndpointType.OPEN, -MathUtils.HALF_PI,
                MathUtils.HALF_PI, IntervalEndpointType.OPEN);
            // test
            final boolean isOk = AngleTools.lowerStrict(0.0, 6 * FastMath.PI + 0.1, angleInterval);
            Assert.assertEquals(isOk, true);
        } catch (final MathIllegalArgumentException e) {
            Assert.fail();
        }
        // Second test
        try {
            // Interval creation
            final AngleInterval angleInterval = new AngleInterval(IntervalEndpointType.OPEN, -MathUtils.HALF_PI,
                MathUtils.HALF_PI, IntervalEndpointType.OPEN);
            // test
            final boolean isOk = AngleTools.lowerStrict(0.0, 6 * FastMath.PI - 0.1, angleInterval);
            Assert.assertEquals(isOk, false);
        } catch (final MathIllegalArgumentException e) {
            Assert.fail();
        }
        // Third test
        try {
            // Interval creation
            final AngleInterval angleInterval = new AngleInterval(IntervalEndpointType.OPEN, -MathUtils.HALF_PI,
                MathUtils.HALF_PI, IntervalEndpointType.OPEN);
            AngleTools.lowerStrict(0.0, 7 * FastMath.PI, angleInterval);
            Assert.fail();
        } catch (final MathIllegalArgumentException e) {
            // Test ok !
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ANGLE_COMPARISONS}
     * 
     * @testedMethod {@link AngleTools#greaterOrEqual(double, double, AngleInterval)}
     * 
     * @description <p>
     *              Three test cases :<br>
     *               - the angles can be expressed in the interval and a > b<br>
     *               - the angles can be expressed in the interval and a < b<br>
     *               - one of the angles can't be expressed in the interval<br>
     *              </p>
     * 
     * @input 2 doubles (the angles) and a the AngleInterval.
     * 
     * @output Boolean set to true if a >= b once expressed in the interval, false otherwise.
     * 
     * @testPassCriteria For the first test, the boolean returned must be true, for the second it must be false, for the
     *                   third,
     *                   the right exception must be thrown.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test(invocationCount = NUMBER_OF_INVOCATIONS, threadPoolSize = NUMBER_OF_THREADS, timeOut = TIMEOUT)
    public final void greaterOrEqualTest() {

        // First test
        try {
            // Interval creation
            final AngleInterval angleInterval = new AngleInterval(IntervalEndpointType.OPEN, -MathUtils.HALF_PI,
                MathUtils.HALF_PI, IntervalEndpointType.OPEN);
            // test
            final boolean isOk = AngleTools.greaterOrEqual(0.0, 6 * FastMath.PI - 0.1, angleInterval);
            Assert.assertEquals(isOk, true);
        } catch (final MathIllegalArgumentException e) {
            Assert.fail();
        }
        // Second test
        try {
            // Interval creation
            final AngleInterval angleInterval = new AngleInterval(IntervalEndpointType.OPEN, -MathUtils.HALF_PI,
                MathUtils.HALF_PI, IntervalEndpointType.OPEN);
            // test
            final boolean isOk = AngleTools.greaterOrEqual(0.0, 6 * FastMath.PI + 0.1, angleInterval);
            Assert.assertEquals(isOk, false);
        } catch (final MathIllegalArgumentException e) {
            Assert.fail();
        }
        // Third test
        try {
            // Interval creation
            final AngleInterval angleInterval = new AngleInterval(IntervalEndpointType.OPEN, -MathUtils.HALF_PI,
                MathUtils.HALF_PI, IntervalEndpointType.OPEN);
            AngleTools.greaterOrEqual(0.0, 7 * FastMath.PI, angleInterval);
            Assert.fail();
        } catch (final MathIllegalArgumentException e) {
            // Test ok !
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ANGLE_COMPARISONS}
     * 
     * @testedMethod {@link AngleTools#greaterStrict(double, double, AngleInterval)}
     * 
     * @description <p>
     *              Three test cases :<br>
     *               - the angles can be expressed in the interval and a > b<br>
     *               - the angles can be expressed in the interval and a < b<br>
     *               - one of the angles can't be expressed in the interval<br>
     *              </p>
     * 
     * @input 2 doubles (the angles) and a the AngleInterval.
     * 
     * @output Boolean set to true if a > b once expressed in the interval, false otherwise.
     * 
     * @testPassCriteria For the first test, the boolean returned must be true, for the second it must be false, for the
     *                   third,
     *                   the right exception must be thrown.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test(invocationCount = NUMBER_OF_INVOCATIONS, threadPoolSize = NUMBER_OF_THREADS, timeOut = TIMEOUT)
    public final void greaterStrictTest() {

        // First test
        try {
            // Interval creation
            final AngleInterval angleInterval = new AngleInterval(IntervalEndpointType.OPEN, -MathUtils.HALF_PI,
                MathUtils.HALF_PI, IntervalEndpointType.OPEN);
            // test
            final boolean isOk = AngleTools.greaterStrict(0.0, 6 * FastMath.PI - 0.1, angleInterval);
            Assert.assertEquals(isOk, true);
        } catch (final MathIllegalArgumentException e) {
            Assert.fail();
        }
        // Second test
        try {
            // Interval creation
            final AngleInterval angleInterval = new AngleInterval(IntervalEndpointType.OPEN, -MathUtils.HALF_PI,
                MathUtils.HALF_PI, IntervalEndpointType.OPEN);
            // test
            final boolean isOk = AngleTools.greaterStrict(0.0, 6 * FastMath.PI + 0.1, angleInterval);
            Assert.assertEquals(isOk, false);
        } catch (final MathIllegalArgumentException e) {
            Assert.fail();
        }
        // Third test
        try {
            // Interval creation
            final AngleInterval angleInterval = new AngleInterval(IntervalEndpointType.OPEN, -MathUtils.HALF_PI,
                MathUtils.HALF_PI, IntervalEndpointType.OPEN);
            AngleTools.greaterStrict(0.0, 7 * FastMath.PI, angleInterval);
            Assert.fail();
        } catch (final MathIllegalArgumentException e) {
            // Test ok !
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ANGLE_OPERATION}
     * 
     * @testedMethod {@link AngleTools#supplementaryAngle(double, AngleInterval)}
     * 
     * @description Test of the supplementaryAngle method : this method returns the
     *              supplementary angle (PI - angle) once expressed in the given interval
     * 
     * @input The inputs are a double (angle) and an interval. Two doubles
     *        are tested : one with which the computation is possible,
     *        one with which the exception must be thrown.
     * 
     * @output The supplementary angle (PI - angle) expressed in the given interval as a double.
     * 
     * @testPassCriteria For first test : the returned double must be
     *                   the right supplementary angle value. For the second test, the right
     *                   exception must be thrown.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test(invocationCount = NUMBER_OF_INVOCATIONS, threadPoolSize = NUMBER_OF_THREADS, timeOut = TIMEOUT)
    public final void supplementaryAngleTest() {
        double angle;
        final double eps = 1.0e-10;
        // First test
        try {
            // Interval creation
            final AngleInterval angleInterval = new AngleInterval(IntervalEndpointType.OPEN, -MathUtils.HALF_PI,
                MathUtils.HALF_PI, IntervalEndpointType.OPEN);

            // test
            angle = 3.0 / 4.0 * FastMath.PI + 6.0 * FastMath.PI;
            final double res = AngleTools.supplementaryAngle(angle, angleInterval);

            Assert.assertEquals(res, 1 / 4.0 * FastMath.PI, eps);
        } catch (final MathIllegalArgumentException e) {
            Assert.fail();
        }
        // Second test
        try {
            // Interval creation
            final AngleInterval angleInterval = new AngleInterval(IntervalEndpointType.OPEN, -MathUtils.HALF_PI,
                MathUtils.HALF_PI, IntervalEndpointType.OPEN);

            // test
            angle = 1 / 4.0 * FastMath.PI;
            AngleTools.supplementaryAngle(angle, angleInterval);
            Assert.fail();
        } catch (final MathIllegalArgumentException e) {
            // Test ok !
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ANGLE_OPERATION}
     * 
     * @testedMethod {@link AngleTools#complementaryAngle(double, AngleInterval)}
     * 
     * @description Test of the complementaryAngle method : this method returns the
     *              complementary angle (PI/2 - angle) once expressed in the given interval.
     * 
     * @input The inputs are a double (angle) and an interval. Two doubles
     *        are tested : one with which the computation is possible,
     *        one with which the exception must be thrown.
     * 
     * @output the supplementary angle (PI/2 - angle) expressed in the given interval as a double.
     * 
     * @testPassCriteria For the first test : the returned double must be
     *                   the right complementary angle value. For the second test, the right
     *                   exception must be thrown.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test(invocationCount = NUMBER_OF_INVOCATIONS, threadPoolSize = NUMBER_OF_THREADS, timeOut = TIMEOUT)
    public final void complementaryAngleTest() {
        double angle;
        final double eps = 1.0e-10;
        // First test
        try {
            // Interval creation
            final AngleInterval angleInterval = new AngleInterval(IntervalEndpointType.OPEN, -MathUtils.HALF_PI,
                MathUtils.HALF_PI, IntervalEndpointType.OPEN);

            // test
            angle = 3.0 / 8.0 * FastMath.PI + 6.0 * FastMath.PI;
            final double res = AngleTools.complementaryAngle(angle, angleInterval);

            Assert.assertEquals(res, 1 / 8.0 * FastMath.PI, eps);
        } catch (final MathIllegalArgumentException e) {
            Assert.fail();
        }
        // Second test
        try {
            // Interval creation
            final AngleInterval angleInterval = new AngleInterval(IntervalEndpointType.OPEN, -MathUtils.HALF_PI,
                MathUtils.HALF_PI, IntervalEndpointType.OPEN);

            // test
            angle = 3 / 2.0 * FastMath.PI;
            AngleTools.complementaryAngle(angle, angleInterval);
            Assert.fail();
        } catch (final MathIllegalArgumentException e) {
            // Test ok !
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ANGLE_OPERATION}
     * 
     * @testedMethod {@link AngleTools#oppositeAngle(double, AngleInterval)}
     * 
     * @description Test of the oppositeAngle method : this method returns the
     *              opposite angle (angle + PI) once expressed in the given interval.
     * 
     * @input The inputs are a double (angle) and an interval. Two doubles
     *        are tested : one with which the computation is possible,
     *        one with which the exception must be thrown.
     * 
     * @output the supplementary angle (PI + angle) expressed in the given interval as a double.
     * 
     * @testPassCriteria For the first test : the returned double must be
     *                   the right opposite angle value. For the second test, the right
     *                   exception must be thrown.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test(invocationCount = NUMBER_OF_INVOCATIONS, threadPoolSize = NUMBER_OF_THREADS, timeOut = TIMEOUT)
    public final void oppositeAngleTest() {
        double angle;
        final double eps = 1.0e-10;
        // First test
        try {
            // Interval creation
            final AngleInterval angleInterval = new AngleInterval(IntervalEndpointType.OPEN, -MathUtils.HALF_PI,
                MathUtils.HALF_PI, IntervalEndpointType.OPEN);

            // test
            angle = 3.0 / 4.0 * FastMath.PI + 6.0 * FastMath.PI;
            final double res = AngleTools.oppositeAngle(angle, angleInterval);

            Assert.assertEquals(res, -1 / 4.0 * FastMath.PI, eps);
        } catch (final MathIllegalArgumentException e) {
            Assert.fail();
        }
        // Second test
        try {
            // Interval creation
            final AngleInterval angleInterval = new AngleInterval(IntervalEndpointType.OPEN, -MathUtils.HALF_PI,
                MathUtils.HALF_PI, IntervalEndpointType.OPEN);

            // test
            angle = 1 / 4.0 * FastMath.PI;
            AngleTools.oppositeAngle(angle, angleInterval);
            Assert.fail();
        } catch (final MathIllegalArgumentException e) {
            // Test ok !
        }
    }

}
