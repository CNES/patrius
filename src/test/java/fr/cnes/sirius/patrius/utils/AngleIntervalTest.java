/**
 * 
 * Copyright 2011-2017 CNES
 *
 * HISTORY
* VERSION:4.8:DM:DM-2996:15/11/2021:[PATRIUS] Ajout d'une methode contains(double) dans la classe AngleInterval 
 * VERSION:4.5:DM:DM-2339:27/05/2020:Intervalles d'angles predefinis 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
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
 */
package fr.cnes.sirius.patrius.utils;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.Comparators;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.interval.AngleInterval;
import fr.cnes.sirius.patrius.math.interval.IntervalEndpointType;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathUtils;

/**
 * @description <p>
 *              - unit tests for the AngleInterval class
 *              </p>
 * 
 * @see AngleInterval
 * 
 * @author Thomas TRAPIER
 * 
 * @version $Id$
 * 
 * @since 1.0
 * 
 */
public class AngleIntervalTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Angle interval
         * 
         * @featureDescription Description of angle intervals
         * 
         * @coveredRequirements DV-MATHS_50
         */
        ANGLE_INTERVAL
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ANGLE_INTERVAL}
     * 
     * @testedMethod {@link AngleInterval#AngleInterval(double, double, IntervalEndpointType, IntervalEndpointType)}
     * @testedMethod {@link AngleInterval#AngleInterval(IntervalEndpointType, double, double, IntervalEndpointType)}
     * @testedMethod {@link AngleInterval#getReference()}
     * @testedMethod {@link AngleInterval#getLowerAngle()}
     * @testedMethod {@link AngleInterval#getUpperAngle()}
     * @testedMethod {@link AngleInterval#getLength()}
     * 
     * @description In this test, two identical AngleInterval are created with the two different constructors. This a
     *              nominal test case.
     * 
     * @input double lowerAngle: the lower angle value
     * @input double upperAngle : the upper angle value
     * @input double reference : the reference angle value
     * @input double length : the length value
     * @input IntervalEndpointType lowerType = OPENED : the lower end point type
     * @input IntervalEndpointType upperType = CLOSED : the upper end point type
     * 
     * @output AngleInterval
     * 
     * @testPassCriteria The two intervals created are identical : all values are tested
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public final void constructorsTest() {
        final double lowerAngle = 1.0;
        final double upperAngle = 2.0;
        final double reference = 1.5;
        final double length = 1.0;
        final IntervalEndpointType openedType = IntervalEndpointType.OPEN;
        final IntervalEndpointType closedType = IntervalEndpointType.CLOSED;

        // end points constructor
        try {
            final AngleInterval angleInterval1 = new AngleInterval(closedType, lowerAngle, upperAngle, openedType);

            // reference and length constructor
            final AngleInterval angleInterval2 = new AngleInterval(reference, length, closedType, openedType);

            // tests
            Assert.assertEquals(angleInterval1.getLength(), angleInterval2.getLength(),
                Comparators.DOUBLE_COMPARISON_EPSILON);
            Assert.assertEquals(angleInterval1.getReference(), angleInterval2.getReference(),
                Comparators.DOUBLE_COMPARISON_EPSILON);
            Assert.assertEquals(angleInterval1.getLowerAngle(), angleInterval2.getLowerAngle(),
                Comparators.DOUBLE_COMPARISON_EPSILON);
            Assert.assertEquals(angleInterval1.getUpperAngle(), angleInterval2.getUpperAngle(),
                Comparators.DOUBLE_COMPARISON_EPSILON);
            Assert.assertEquals(angleInterval1.getLowerEndPoint(), angleInterval2.getLowerEndPoint());
            Assert.assertEquals(angleInterval1.getUpperEndPoint(), angleInterval2.getUpperEndPoint());
        } catch (final MathIllegalArgumentException e) {
            Assert.fail();
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ANGLE_INTERVAL}
     * 
     * @testedMethod {@link AngleInterval#AngleInterval(double, double, IntervalEndpointType, IntervalEndpointType)}
     * @testedMethod {@link AngleInterval#AngleInterval(IntervalEndpointType, double, double, IntervalEndpointType)}
     * 
     * @description
     * 
     * @input <p>
     *        Several inputs are tried in this test :
     *        </p>
     *        <p>
     *        - with a negative length,
     *        </p>
     *        <p>
     *        - with a length greater than 2PI,
     *        </p>
     *        <p>
     *        - with a length equal to 2PI and two closed end points,
     *        </p>
     *        <p>
     *        - with a null length and at least an opened end point.
     *        </p>
     * 
     * @output AngleInterval
     * 
     * @testPassCriteria The right exceptions must be thrown : the test tries to create invalid angle intervals with
     *                   wrong values.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public final void invalidIntervalTest() {
        final IntervalEndpointType openedType = IntervalEndpointType.OPEN;
        final IntervalEndpointType closedType = IntervalEndpointType.CLOSED;
        try {
            new AngleInterval(closedType, 1.0, 0.0, openedType);
            Assert.fail();
        } catch (final MathIllegalArgumentException e) {
            // expected
        }
        try {
            new AngleInterval(0.0, 7.0, closedType, openedType);
            Assert.fail();
        } catch (final MathIllegalArgumentException e) {
            // expected
        }
        try {
            new AngleInterval(0.0, 7.0, openedType, closedType);
            Assert.fail();
        } catch (final MathIllegalArgumentException e) {
            // expected
        }
        try {
            new AngleInterval(0.0, MathUtils.TWO_PI, closedType, closedType);
            Assert.fail();
        } catch (final MathIllegalArgumentException e) {
            // expected
        }
        try {
            new AngleInterval(0.0, MathUtils.TWO_PI, openedType, closedType);
        } catch (final MathIllegalArgumentException e) {
            Assert.fail();
        }
        try {
            new AngleInterval(0.0, MathUtils.TWO_PI, closedType, openedType);
        } catch (final MathIllegalArgumentException e) {
            Assert.fail();
        }
        try {
            new AngleInterval(0.0, MathUtils.TWO_PI, openedType, openedType);
        } catch (final MathIllegalArgumentException e) {
            Assert.fail();
        }
        try {
            new AngleInterval(0.0, 0.0, closedType, openedType);
            Assert.fail();
        } catch (final MathIllegalArgumentException e) {
            // expected
        }
        try {
            new AngleInterval(0.0, 0.0, openedType, closedType);
            Assert.fail();
        } catch (final MathIllegalArgumentException e) {
            // expected
        }
        try {
            new AngleInterval(0.0, 0.0, openedType, openedType);
            Assert.fail();
        } catch (final MathIllegalArgumentException e) {
            // expected
        }
        try {
            new AngleInterval(0.0, 0.0, closedType, closedType);
        } catch (final MathIllegalArgumentException e) {
            Assert.fail();
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ANGLE_INTERVAL}
     * 
     * @testedMethod {@link AngleInterval#toString()}
     * 
     * @description Test for the toString method.
     * 
     * @input Several angle intervals.
     * 
     * @output Result of toString for each angle interval.
     * 
     * @testPassCriteria toString results are as expected.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public final void toStringTest() {
        final double lowerAngle1 = 1.0;
        final double upperAngle1 = 2.0;
        final String expectedIn1 = " 1.0 rad , 2.0 rad ";
        final double lowerAngle2 = -2.432343;
        final double upperAngle2 = 1.22343;
        final String expectedIn2 = " -2.432343 rad , 1.22343 rad ";
        final IntervalEndpointType openedType = IntervalEndpointType.OPEN;
        final IntervalEndpointType closedType = IntervalEndpointType.CLOSED;
        final String leftB = "[";
        final String rightB = "]";

        // Tests for all interval end types
        final AngleInterval ang1A = new AngleInterval(closedType, lowerAngle1, upperAngle1, closedType);
        final String expected1A = leftB + expectedIn1 + rightB;
        Assert.assertEquals(expected1A, ang1A.toString());
        final AngleInterval ang1B = new AngleInterval(openedType, lowerAngle1, upperAngle1, closedType);
        final String expected1B = rightB + expectedIn1 + rightB;
        Assert.assertEquals(expected1B, ang1B.toString());
        final AngleInterval ang1C = new AngleInterval(openedType, lowerAngle1, upperAngle1, openedType);
        final String expected1C = rightB + expectedIn1 + leftB;
        Assert.assertEquals(expected1C, ang1C.toString());
        final AngleInterval ang1D = new AngleInterval(closedType, lowerAngle1, upperAngle1, openedType);
        final String expected1D = leftB + expectedIn1 + leftB;
        Assert.assertEquals(expected1D, ang1D.toString());
        // Test for another set of values
        final AngleInterval ang2A = new AngleInterval(closedType, lowerAngle2, upperAngle2, closedType);
        final String expected2A = leftB + expectedIn2 + rightB;
        Assert.assertEquals(expected2A, ang2A.toString());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ANGLE_INTERVAL}
     * 
     * @testedMethod {@link AngleInterval#AngleInterval(double, double, IntervalEndpointType, IntervalEndpointType)}
     * @testedMethod {@link AngleInterval#AngleInterval(IntervalEndpointType, double, double, IntervalEndpointType)}
     * 
     * @description In this test, several AngleInterval are created with wrong inputs
     * 
     * @input double lowerAngle: the lower angle value
     * @input double upperAngle : the upper angle value
     * @input double reference : the reference angle value
     * @input double length : the length value
     * @input IntervalEndpointType lowerType = OPENED : the lower end point type
     * @input IntervalEndpointType upperType = CLOSED : the upper end point type
     * 
     * @output MathIllegalArgumentException
     * 
     * @testPassCriteria A MathIllegalArgumentException exception is thrown
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public final void testWrongIntervals() {
        final double anySmall = 0.213;
        try {
            final AngleInterval wrongInterval = new AngleInterval(Double.NaN, anySmall, IntervalEndpointType.CLOSED,
                IntervalEndpointType.CLOSED);
            Assert.assertNotNull(wrongInterval);
            Assert.fail();
        } catch (final MathIllegalArgumentException e) {
            // As expected
        }
        try {
            final AngleInterval wrongInterval = new AngleInterval(anySmall, Double.NaN, IntervalEndpointType.CLOSED,
                IntervalEndpointType.CLOSED);
            Assert.assertNotNull(wrongInterval);
            Assert.fail();
        } catch (final MathIllegalArgumentException e) {
            // As expected
        }
        try {
            final AngleInterval wrongInterval = new AngleInterval(IntervalEndpointType.OPEN, Double.NaN, anySmall,
                IntervalEndpointType.OPEN);
            Assert.assertNotNull(wrongInterval);
            Assert.fail();
        } catch (final MathIllegalArgumentException e) {
            // As expected
        }
        try {
            final AngleInterval wrongInterval = new AngleInterval(IntervalEndpointType.OPEN, anySmall, Double.NaN,
                IntervalEndpointType.OPEN);
            Assert.assertNotNull(wrongInterval);
            Assert.fail();
        } catch (final MathIllegalArgumentException e) {
            // As expected
        }
        try {
            final AngleInterval wrongInterval = new AngleInterval(IntervalEndpointType.OPEN,
                Double.NEGATIVE_INFINITY, anySmall, IntervalEndpointType.OPEN);
            Assert.assertNotNull(wrongInterval);
            Assert.fail();
        } catch (final MathIllegalArgumentException e) {
            // As expected
        }
        try {
            final AngleInterval wrongInterval = new AngleInterval(IntervalEndpointType.OPEN,
                anySmall, Double.POSITIVE_INFINITY, IntervalEndpointType.OPEN);
            Assert.assertNotNull(wrongInterval);
            Assert.fail();
        } catch (final MathIllegalArgumentException e) {
            // As expected
        }
        try {
            final AngleInterval wrongInterval = new AngleInterval(IntervalEndpointType.OPEN,
                Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, IntervalEndpointType.OPEN);
            Assert.assertNotNull(wrongInterval);
            Assert.fail();
        } catch (final MathIllegalArgumentException e) {
            // As expected
        }
        try {
            final AngleInterval wrongInterval = new AngleInterval(IntervalEndpointType.OPEN,
                Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, IntervalEndpointType.OPEN);
            Assert.assertNotNull(wrongInterval);
            Assert.fail();
        } catch (final MathIllegalArgumentException e) {
            // As expected
        }
        try {
            final AngleInterval wrongInterval = new AngleInterval(IntervalEndpointType.OPEN,
                Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, IntervalEndpointType.OPEN);
            Assert.assertNotNull(wrongInterval);
            Assert.fail();
        } catch (final MathIllegalArgumentException e) {
            // As expected
        }
    }
    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ANGLE_INTERVAL}
     * 
     * @testedMethod {@link AngleInterval#ZERO_2PI}
     * @testedMethod {@link AngleInterval#MINUS2PI_ZERO}
     * @testedMethod {@link AngleInterval#MINUSPI_PI}
     * @testedMethod {@link AngleInterval#getLowerAngle()}
     * @testedMethod {@link AngleInterval#getUpperAngle()}
     * @testedMethod {@link AngleInterval#getLength()}
     * 
     * @description Check that static angle intervals are as expected.
     * 
     * @input AngleInterval
     * 
     * @output AngleInterval
     * 
     * @testPassCriteria The static intervals are as expected (functional test). Boudaries and boundary type are checked. 
     * 
     * @referenceVersion 4.5
     * 
     * @nonRegressionVersion 4.5
     */
    @Test
    public final void testStaticInterval() {
        // Definition
        final AngleInterval ZERO_2PI = AngleInterval.ZERO_2PI;
        new AngleInterval(IntervalEndpointType.CLOSED, 0.0, MathUtils.TWO_PI,
                IntervalEndpointType.OPEN);
        final AngleInterval MINUS2PI_ZERO = AngleInterval.MINUS2PI_ZERO;
        new AngleInterval(IntervalEndpointType.OPEN, -MathUtils.TWO_PI,
                0.0, IntervalEndpointType.CLOSED);
        final AngleInterval MINUSPI_PI = AngleInterval.MINUSPI_PI;
        new AngleInterval(IntervalEndpointType.CLOSED, -FastMath.PI,
                FastMath.PI, IntervalEndpointType.OPEN);

        // Check
        Assert.assertEquals(0, ZERO_2PI.getLowerAngle(), 0);
        Assert.assertEquals(MathUtils.TWO_PI, ZERO_2PI.getUpperAngle(), 0);
        Assert.assertEquals(-MathUtils.TWO_PI, MINUS2PI_ZERO.getLowerAngle(), 0);
        Assert.assertEquals(0, MINUS2PI_ZERO.getUpperAngle(), 0);
        Assert.assertEquals(-FastMath.PI, MINUSPI_PI.getLowerAngle(), 0);
        Assert.assertEquals(FastMath.PI, MINUSPI_PI.getUpperAngle(), 0);
        Assert.assertEquals(IntervalEndpointType.CLOSED, ZERO_2PI.getLowerEndPoint());
        Assert.assertEquals(IntervalEndpointType.OPEN, ZERO_2PI.getUpperEndPoint());
        Assert.assertEquals(IntervalEndpointType.OPEN, MINUS2PI_ZERO.getLowerEndPoint());
        Assert.assertEquals(IntervalEndpointType.CLOSED, MINUS2PI_ZERO.getUpperEndPoint());
        Assert.assertEquals(IntervalEndpointType.CLOSED, MINUSPI_PI.getLowerEndPoint());
        Assert.assertEquals(IntervalEndpointType.OPEN, MINUSPI_PI.getUpperEndPoint());
    }
    /**
     * @testedFeature {@link features#ANGLE_INTERVAL}
     * @testedMethod {@link AngleInterval#contains(double)}
     * 
     * @description test method contains for various input.
     * 
     * @testPassCriteria result is as expected (reference are computed mathematically)
     * 
     * @referenceVersion 4.8
     * 
     * @nonRegressionVersion 4.8
     */
    @Test
    public final void containsTest() {
        // Open / Open
        final AngleInterval interval = new AngleInterval(IntervalEndpointType.OPEN, 0, 2, IntervalEndpointType.OPEN);
        Assert.assertFalse(interval.contains(-1));
        Assert.assertFalse(interval.contains(0));
        Assert.assertTrue(interval.contains(1.));
        Assert.assertFalse(interval.contains(2));
        Assert.assertFalse(interval.contains(3.));
        // Open / Closed
        final AngleInterval interval2 = new AngleInterval(IntervalEndpointType.OPEN, 0, 2, IntervalEndpointType.CLOSED);
        Assert.assertFalse(interval2.contains(-1));
        Assert.assertFalse(interval2.contains(0));
        Assert.assertTrue(interval2.contains(1.));
        Assert.assertTrue(interval2.contains(2));
        Assert.assertFalse(interval2.contains(3.));
        // Closed / Open
        final AngleInterval interval3 = new AngleInterval(IntervalEndpointType.CLOSED, 0, 2, IntervalEndpointType.OPEN);
        Assert.assertFalse(interval3.contains(-1));
        Assert.assertTrue(interval3.contains(0));
        Assert.assertTrue(interval3.contains(1.));
        Assert.assertFalse(interval3.contains(2));
        Assert.assertFalse(interval3.contains(3.));
        // Closed / Closed
        final AngleInterval interval4 = new AngleInterval(IntervalEndpointType.CLOSED, 0, 2, IntervalEndpointType.CLOSED);
        Assert.assertFalse(interval4.contains(-1));
        Assert.assertTrue(interval4.contains(0));
        Assert.assertTrue(interval4.contains(1.));
        Assert.assertTrue(interval4.contains(2));
        Assert.assertFalse(interval4.contains(3.));
    }
}
