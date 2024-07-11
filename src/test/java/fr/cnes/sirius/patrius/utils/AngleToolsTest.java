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
 * @history Creation 25/07/11
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:128:26/09/2013:Corrected bug in angleInInterval due to cancellation
 * VERSION::FA:671:30/08/2016: numerical quality issues in angleInInterval method
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.utils;

import static org.junit.Assert.assertEquals;

import java.util.Random;

import junit.framework.Assert;

import org.junit.Test;

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
import fr.cnes.sirius.patrius.math.util.Precision;

/**
 * @description test class for classical operations on angles
 * 
 * @author Julie Anton, Thomas Trapier
 * 
 * @version $Id$
 * 
 * @since 1.0
 * 
 */
public class AngleToolsTest {

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
    private static final int NUMBER_OF_TRIALS = 1000;

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
    @Test
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

        // one vector has a zero norm
        final Vector3D vector5 = Vector3D.ZERO;
        try {
            x = AngleTools.getAngleBewteen2Vector3D(vector1, vector5);
            Assert.fail("expecting MathArithmeticException but got : " + x);
        } catch (final MathArithmeticException ex) {
            // expected
        }
        try {
            x = AngleTools.getAngleBewteen2Vector3D(vector5, vector1);
            Assert.fail("expecting MathArithmeticException but got : " + x);
        } catch (final MathArithmeticException ex) {
            // expected
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
    @Test
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

        // one vector has a zero norm
        final Vector2D vector3 = new Vector2D(0, 0);
        try {
            x = AngleTools.getOrientedAngleBetween2Vector2D(vector3, vector2);
            Assert.fail("expecting MathIllegalArgumentException but got : " + x);
        } catch (final MathIllegalArgumentException ex) {
            // expected
        }
        try {
            x = AngleTools.getOrientedAngleBetween2Vector2D(vector2, vector3);
            Assert.fail("expecting MathIllegalArgumentException but got : " + x);
        } catch (final MathIllegalArgumentException ex) {
            // expected
        }
        // one coordinate is +infinity
        final Vector2D vector5 = new Vector2D(Double.POSITIVE_INFINITY, 1);
        try {
            x = AngleTools.getOrientedAngleBetween2Vector2D(vector1, vector5);
            Assert.fail("expecting MathIllegalArgumentException but got : " + x);
        } catch (final MathIllegalArgumentException ex) {
            // expected
        }
        // one coordinate is -infinity
        final Vector2D vector6 = new Vector2D(1, Double.NEGATIVE_INFINITY);
        try {
            x = AngleTools.getOrientedAngleBetween2Vector2D(vector6, vector1);
            Assert.fail("expecting MathIllegalArgumentException but got : " + x);
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
    @Test
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

        // one vector is the vector null
        try {
            x = AngleTools.getOrientedAngleBewteen2Vector3D(vector1, vector2, Vector3D.ZERO);
            Assert.fail("expecting MathArithmeticException but got : " + x);
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
    @Test
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
            Assert.fail("expecting MathIllegalArgumentException but got : " + x * MathUtils.RAD_TO_DEG + " °.");
        } catch (final MathIllegalArgumentException ex) {
            // expected
        }

        // cosine and sine of different angles which are closed
        try {
            x = AngleTools.getAngleFromCosineAndSine(MathLib.cos(30 * MathUtils.DEG_TO_RAD),
                MathLib.sin(30.0001 * MathUtils.DEG_TO_RAD));
            Assert.fail("expecting MathIllegalArgumentException but got : " + x);
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
    @Test
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
     * @testedFeature {@link features#ANGLE_IN_INTERVAL}
     * 
     * @testedMethod {@link AngleTools#angleInInterval(double, AngleInterval)}
     * 
     * @description Test of the modulo function with nominal and error cases
     * 
     * @input The inputs are a double (angle) and an interval. Several doubles are tested around critical values to see
     *        if any exceptions are thrown (due to digital approximations)
     * 
     * @output Double angle expressed in given interval.
     * 
     * @testPassCriteria All the angles must be computed without exceptions being thrown.
     * 
     * @referenceVersion 2.1
     * 
     * @nonRegressionVersion 2.1
     */
    @Test
    public void angleInIntervalTest1() {

        /** definition de l'intervalle */
        final double deuxPi = 2. * FastMath.PI;
        final double dpAcc = -MathLib.nextAfter(deuxPi, 0) + deuxPi;

        AngleInterval zero2Pi;

        double azimuth;

        for (int k = -5; k < 5; k++) {
            // décalage interval
            zero2Pi = new AngleInterval(
                IntervalEndpointType.CLOSED, deuxPi * k, deuxPi * (k + 1),
                IntervalEndpointType.OPEN);

            // décalage valeur de référence des angles testés
            final double[] piv = new double[20];
            for (int i = 0; i < piv.length; i++) {
                piv[i] = FastMath.PI * (i - piv.length / 2);
            }

            for (final double element : piv) {

                // test de plein d'angles autour des valeurs critiques
                for (int ii = 0; ii < 1500; ii++) {
                    azimuth = element + dpAcc / 400 * (ii - 750);

                    try {
                        AngleTools.angleInInterval(azimuth, zero2Pi);
                    } catch (final MathIllegalArgumentException e) {
                        Assert.fail();
                    }
                }
            }
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
     * @input The inputs are a double (angle) and an interval. Several doubles are tested around critical value 0.
     * 
     * @output Double angle expressed in given interval.
     * 
     * @testPassCriteria All the angles must be computed without exceptions being thrown.
     *                   angles lower than 2pi accuracy must be exactly (0 ulp difference) equal to 0
     *                   angles bigger than 2pi accuracy must be exactly (0 ulp difference) equal to 2pi - angle.
     *                   2pi accuracy is 8.881784197001252E-16
     * 
     * @referenceVersion 2.1
     * 
     * @nonRegressionVersion 2.1
     */
    @Test
    public void angleInIntervalTest2() {

        /** definition de l'intervalle */
        final double deuxPi = 2. * FastMath.PI;
        final double threshold = MathLib.nextUp(deuxPi) - deuxPi;

        final double[] azimuth = { -FastMath.PI, -1e-14, -9e-15, -8e-15, -7e-15, -6e-15, -5e-15, -4e-15, -3e-15,
            -2e-15, -1e-15, -9e-16, -8e-16, -7e-16, -6e-16, -5e-16, -4e-16, -3e-16, -2e-16, -1e-16,
            -Precision.SAFE_MIN };

        final AngleInterval zero2Pi = new AngleInterval(
            IntervalEndpointType.CLOSED, 0, deuxPi,
            IntervalEndpointType.OPEN);

        double result;
        boolean res;

        for (final double element : azimuth) {
            try {
                result = AngleTools.angleInInterval(element, zero2Pi);
                res = Precision.equals(MathLib.abs(element) < threshold / 2. ? 0 : deuxPi
                    + element, result, 0);
                Assert.assertTrue(res);
            } catch (final MathIllegalArgumentException e) {
                System.out.printf("error for %.16f", element);
                System.out.println(e.getMessage());
                Assert.fail();
            }
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
     * @input The inputs are a double (angle) and an interval. Several doubles are tested around critical value 2pi.
     * 
     * @output Double angle expressed in given interval.
     * 
     * @testPassCriteria All the angles must be computed without exceptions being thrown.
     *                   computed angles must be exactly (0 ulp difference) equal to the expected values
     * 
     * @referenceVersion 2.1
     * 
     * @nonRegressionVersion 2.1
     */
    @Test
    public void angleInIntervalTest3() {

        /** definition de l'intervalle */
        final double deuxPi = 2. * FastMath.PI;

        final double[] azimuth = { 1e-14, 9e-15, 8e-15, 7e-15, 6e-15, 5e-15, 4e-15, 3e-15,
            2e-15, 1e-15, 9e-16, 8e-16, 7e-16, 6e-16, 5e-16, 4e-16, 3e-16, 2e-16, 1e-16,
            Precision.SAFE_MIN };

        final AngleInterval zero2Pi = new AngleInterval(
            IntervalEndpointType.CLOSED, 0, deuxPi,
            IntervalEndpointType.OPEN);

        double result;
        double expected;
        boolean res;

        for (final double element : azimuth) {
            try {
                result = AngleTools.angleInInterval(deuxPi + element, zero2Pi);
                // cancellation effects!
                expected = deuxPi + element - deuxPi;
                res = Precision.equals(expected, result, 0);
                Assert.assertTrue(res);
            } catch (final MathIllegalArgumentException e) {
                System.out.printf("error for %.16f", element);
                System.out.println(e.getMessage());
                Assert.fail();
            }
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
     * @nonRegressionVersion 1.0
     */
    @Test
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
     * @nonRegressionVersion 1.0
     */
    @Test
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
     * @nonRegressionVersion 1.0
     */
    @Test
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
     * @nonRegressionVersion 1.0
     */
    @Test
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
     * @nonRegressionVersion 1.0
     */
    @Test
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
     * @nonRegressionVersion 1.0
     */
    @Test
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
     * @nonRegressionVersion 1.0
     */
    @Test
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
     * @nonRegressionVersion 1.0
     */
    @Test
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

    /**
     * FA-671.
     */
    @Test
    public final void testInterval() {
        final double value = MathLib.toRadians(-10);
        final AngleInterval interval =
            new AngleInterval(value + FastMath.PI, MathUtils.TWO_PI, IntervalEndpointType.CLOSED,
                IntervalEndpointType.OPEN);
        final double an10ToDouble = AngleTools.angleInInterval(value, interval);
        // Check no exception occurred
        Assert.assertEquals(value, an10ToDouble, 2E-16);
    }

    /**
     * FA-671.
     */
    @Test
    public final void testInterval2() {
        final double value = MathLib.toRadians(-10);
        final double ref = 0.;

        final AngleInterval interval =
            new AngleInterval(IntervalEndpointType.CLOSED, ref - FastMath.PI, ref + FastMath.PI,
                IntervalEndpointType.OPEN);
        double valueInRefInterval = AngleTools.angleInInterval(value, interval);

        valueInRefInterval += FastMath.PI;

        // Création de l'intervalle autour de la nouvelle référence
        final AngleInterval interval2 =
            new AngleInterval(IntervalEndpointType.CLOSED, valueInRefInterval - FastMath.PI,
                valueInRefInterval + FastMath.PI, IntervalEndpointType.OPEN);

        // On recherche l'angle dans l'intervalle autour de la référence + PI --> ECHEC
        final double valueInRefIntervalPi = AngleTools.angleInInterval(value, interval2);

        // Check no exception occurred
        Assert.assertEquals(value, valueInRefIntervalPi, 2E-16);
    }
}
