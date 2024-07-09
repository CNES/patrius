/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 *
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
 * @history Created on ???
 * 
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:128:26/09/2013:Corrected bug in angleInInterval due to cancellation
 * VERSION::FA:400:17/03/2015: use class FastMath instead of class Math
 * VERSION::FA:671:30/08/2016: numerical quality issues in angleInInterval method
 * VERSION::FA:708:13/12/2016: add documentation corrections
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.interval;

import fr.cnes.sirius.patrius.math.Comparators;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.geometry.euclidean.twod.Vector2D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.utils.UtilsPatrius;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.PatriusRuntimeException;

/**
 * <p>
 * This class provides static methods for angles :
 * </p>
 * <p>
 * - angles computation,
 * </p>
 * <p>
 * - comparison,
 * </p>
 * <p>
 * - arithmetic and trigonometric operations.
 * </p>
 * 
 * @useSample <p>
 *            - In order to compute the angle between two vectors :
 *            </p>
 *            <p>
 *            double angle = AngleTools.getAngleBewteen2Vector3D(vector1, vector2);
 *            </p>
 *            <p>
 *            - To compute the angle 2PI modulo in the given interval : (can throw a MathIllegalArgumentException)
 *            </p>
 *            <p>
 *            double res = AngleTools.angleInInterval(angle, angleInterval);
 *            </p>
 *            <p>
 *            - To compare two angles in a given interval : (can throw a MathIllegalArgumentException)
 *            </p>
 *            <p>
 *            boolean isEqual = AngleTools.equal(angle1, angle2, angleInterval);
 *            </p>
 *            <p>
 *            - To compute the complementary angle in a given interval : (can throw a MathIllegalArgumentException)
 *            </p>
 *            <p>
 *            double res = AngleTools.complementaryAngle(angle, angleInterval);
 *            </p>
 *            See DV-MATHS_60, 70, 80, 90, 100 .
 * 
 * @concurrency unconditionally thread-safe
 * 
 * @author Julie Anton, Thomas Trapier
 * 
 * @version $Id: AngleTools.java 18021 2017-09-29 15:56:26Z bignon $
 * 
 * @since 1.0
 * 
 */
public final class AngleTools {

    /**
     * Constructor<br>
     * This is a private constructor which insure that this class cannot be instantiated.
     * 
     * @since 1.0
     */
    private AngleTools() {
    }

    /**
     * Computes the angle between 2 vectors 3D. To do so, we use the method angle(Vector3D, Vector3D) of
     * {@link Vector3D} .
     * 
     * @precondition either the first vector or the second one should have non zero norm.
     * 
     * @param vector1
     *        the first vector
     * @param vector2
     *        the second vector
     * 
     * @return double angle = the positive value of the angle between the two vectors, the angle is defined between
     *         {@code 0} and {@code PI}.
     * 
     *         See DV-MATHS_100
     * 
     * @see Vector3D#angle(Vector3D, Vector3D)
     * 
     * @since 1.0
     */
    public static double getAngleBewteen2Vector3D(final Vector3D vector1, final Vector3D vector2) {
        return Vector3D.angle(vector1, vector2);
    }

    /**
     * Computes the oriented angle between 2 vectors 2D.
     * 
     * @precondition either the first vector or the second one should have finite coordinate values and non zero norm.
     * 
     * @param vector1
     *        the first vector
     * @param vector2
     *        the second vector
     * 
     * @return double angle = the value of the oriented angle between the two vectors, the angle is defined between
     *         {@code -2 PI} and {@code 2 PI}.
     * 
     *         See DV-MATHS_100 .
     * @throws MathIllegalArgumentException
     *         if at least one norm is zero
     * @throws MathIllegalArgumentException
     *         if at least one norm is infinity
     * @since 1.0
     */
    public static double getOrientedAngleBetween2Vector2D(final Vector2D vector1, final Vector2D vector2) {

        // at least one norm is zero
        if (vector1.getNorm() == 0 || vector2.getNorm() == 0) {
            throw new MathIllegalArgumentException(PatriusMessages.ZERO_NORM);
        } else if (vector1.isInfinite() || vector2.isInfinite()) {
            // at least one norm is infinity
            throw new MathIllegalArgumentException(PatriusMessages.NOT_FINITE_NUMBER);
        } else {

            final double angle1 = MathLib.atan2(vector1.getY(), vector1.getX());
            final double angle2 = MathLib.atan2(vector2.getY(), vector2.getX());

            return (angle2 - angle1);
        }
    }

    /**
     * Computes the oriented angle between 2 vectors 3D.
     * 
     * @precondition either the first vector or the second one or the third one should have non zero norm.
     *               The third vector should be perpendicular to the plane formed by the formers.
     * 
     * @param vector1
     *        the first vector
     * @param vector2
     *        the second vector
     * @param vector3
     *        the third vector which defines the orientation
     * 
     * @return double angle = the value of the oriented angle between the two vectors, the angle is defined between
     *         {@code -PI} and {@code PI}.
     * 
     *         See DV-MATHS_100 .
     * @throws IllegalArgumentException
     *         if the cross product is wrong
     * @since 1.0
     */
    public static double getOrientedAngleBewteen2Vector3D(final Vector3D vector1, final Vector3D vector2,
                                                          final Vector3D vector3) {
        final double rez;
        // cross product of the 2 vectors that define the plane
        final Vector3D crossProduct = Vector3D.crossProduct(vector1, vector2);
        // non oriented angle between the 2 formers vectors
        final double angleResult = Vector3D.angle(vector1, vector2);
        // angle between the third vector and the vector resulting from the
        // cross product which should be 0 or PI i.e. colinear vectors.
        final double angleIntermediate = Vector3D.angle(vector3, crossProduct);

        if (Comparators.equals(angleIntermediate, 0.0)) {
            rez = angleResult;
        } else if (Comparators.equals(angleIntermediate, FastMath.PI)) {
            rez = -angleResult;
        } else if (angleIntermediate == angleIntermediate) {
            throw new IllegalArgumentException();
        } else {
            rez = Double.NaN;
        }

        return rez;
    }

    /**
     * Computes an angle from the sine and the cosine
     * 
     * @precondition the sine and the cosine should be of the same angle ie cos²+sin² is lower than 1.
     * 
     * @param cos
     *        : the cosine of the angle we want to know the value
     * @param sin
     *        : the sine of the angle we want to know the value
     * @return double angle = the angle given by sine and cosine between {@code -PI} and {@code PI}
     * 
     *         See DV-MATHS_100 .
     * 
     * @since 1.0
     */
    public static double getAngleFromCosineAndSine(final double cos, final double sin) {
        if (MathLib.pow(cos, 2) + MathLib.pow(sin, 2) - UtilsPatrius.EPSILON > 1) {
            throw new MathIllegalArgumentException(PatriusMessages.ILLEGAL_STATE);
        }
        return MathLib.atan2(sin, cos);
    }

    /**
     * Computes the angle in the given interval modulo 2pi.
     * There are particular cases : numerical quality issue solving in the following cases :
     * - the interval is of the form [a, a + 2PI[
     * If angle is lower than lower bound and angle + 2Pi larger than higher bound : lower bound is returned
     * 
     * - the interval is of the form ]a, a + 2PI]
     * If angle is larger than larger bound and angle - 2Pi lower than lower bound : larger bound is returned
     * These cases occur because of the non-identical repartition of doubles around the two interval boundaries.
     * 
     * @precondition the interval can represent the given angle ([0, pi[ doesn't contain -pi/2 modulo 2pi)
     * 
     * @param angle
     *        angle to be expressed inside the given interval
     * @param interval
     *        interval of expression
     * @throws MathIllegalArgumentException
     *         the angle is'nt in the interval modulo 2PI
     * @return the angle expressed in the interval
     * 
     * @since 1.0
     */
    public static double angleInInterval(final double angle, final AngleInterval interval) {

        if (isIn(angle, interval, Precision.EPSILON)) {
            return angle;
        }

        // Set angle within ± 2pi from reference
        final double dist = angle - interval.getReference();
        final double res = dist / MathUtils.TWO_PI;
        final double mult = MathLib.floor(MathLib.abs(res));

        // This angle should be within ± 2pi (exclusive) from the interval reference
        final double angleWithinPM2Pi = angle - MathLib.signum(dist) * mult * MathUtils.TWO_PI;

        // Set angle in interval (particular cases)
        double newAngle = angleInIntervalTwoPi(angleWithinPM2Pi, interval);

        // Set angle in interval (nominal case)
        if (Double.isNaN(newAngle)) {
            if (angleWithinPM2Pi < interval.getLowerAngle()) {
                newAngle = angleWithinPM2Pi + MathUtils.TWO_PI;
            } else if (angleWithinPM2Pi > interval.getUpperAngle()) {
                newAngle = angleWithinPM2Pi - MathUtils.TWO_PI;
            } else {
                newAngle = angleWithinPM2Pi;
            }
        }

        if (!isIn(newAngle, interval, Precision.EPSILON)) {
            // if not, an exception is thrown
            throw new MathIllegalArgumentException(PatriusMessages.PDB_ANGLE_OUTSIDE_INTERVAL);
        }

        return newAngle;
    }

    /**
     * 2nd part of angleInInterval method (split for checkstyle reasons).
     * 
     * @param angleWithinPM2Pi
     *        angle
     * @param interval
     *        interval
     * @return angle expressed in the provided interval
     */
    private static double angleInIntervalTwoPi(final double angleWithinPM2Pi, final AngleInterval interval) {

        // Adjusted input angle
        double newAngle = Double.NaN;

        // Particular cases: numerical quality issue solving
        // - If angle is lower than lower bound and angle + 2Pi larger than higher bound: lower bound is kept
        // - If angle is larger than larger bound and angle - 2Pi lower than lower bound: larger bound is kept
        // These cases occur because of the non-identical repartition of doubles around the two interval boundaries
        final double low = interval.getLowerAngle();
        final double high = interval.getUpperAngle();
        if (interval.getUpperEndPoint() == IntervalEndpointType.OPEN) {
            if (angleWithinPM2Pi < low && angleWithinPM2Pi + MathUtils.TWO_PI >= high) {
                newAngle = low;
            }
            if (angleWithinPM2Pi == high) {
                newAngle = low;
            }
        }
        if (interval.getLowerEndPoint() == IntervalEndpointType.OPEN) {
            if (angleWithinPM2Pi > high && angleWithinPM2Pi - MathUtils.TWO_PI <= low) {
                newAngle = high;
            }
            if (angleWithinPM2Pi == low) {
                newAngle = high;
            }
        }

        return newAngle;
    }

    /**
     * Tests if a given double belongs to an interval.
     * 
     * @precondition none
     * 
     * @param angle
     *        the angle tested
     * @param interval
     *        the interval
     * @param eps
     *        error threshold (recommended value : Precision.EPSILON)
     * 
     * @return boolean isIn (true if the angle belongs to the interval)
     * 
     * @since 1.0
     */
    private static boolean isIn(final double angle, final AngleInterval interval, final double eps) {

        // the returned boolean
        // boolean isIn = true;

        // distance between the reference and the angle
        // final double distanceToRef = angle - interval.getReference();
        // final double length = interval.getLength();
        
        // Upper bound
        final double upper = interval.getUpperAngle();

        final IntervalEndpointType upperEndPoint = interval.getUpperEndPoint();

        final boolean upperWithin;

        switch (upperEndPoint) {
            case CLOSED:
                upperWithin = Comparators.lowerOrEqual(angle, upper, eps);
                break;
            case OPEN:
                upperWithin = Comparators.lowerStrict(angle, upper, eps);
                break;
            default:
                // Cannot happen
                throw new PatriusRuntimeException(PatriusMessages.INTERNAL_ERROR, null);
        }

        // Lower bound
        final double lower = interval.getLowerAngle();
        final IntervalEndpointType lowerEndPoint = interval.getLowerEndPoint();
        final boolean lowerWithin;

        switch (lowerEndPoint) {
            case CLOSED:
                lowerWithin = Comparators.greaterOrEqual(angle, lower, eps);
                break;
            case OPEN:
                lowerWithin = Comparators.greaterStrict(angle, lower, eps);
                break;
            default:
                // Cannot happen
                throw new PatriusRuntimeException(PatriusMessages.INTERNAL_ERROR, null);
        }

        // Return result
        return lowerWithin & upperWithin;
    }

    /**
     * Tests the equality of two angles after expressing them in the same interval.
     * 
     * @precondition both angles must be in the given interval modulo 2PI
     * 
     * @param alpha
     *        one angle
     * @param beta
     *        one angle
     * @param interval
     *        the interval to express the angles
     * 
     * @return boolean : true if equal
     * 
     * @throws MathIllegalArgumentException
     *         if one angle is'nt in the interval, modulo 2PI
     * 
     * @since 1.0
     */
    public static boolean equal(final double alpha, final double beta, final AngleInterval interval) {
        // both angles are expressed in the interval...
        final double correctedAlpha = angleInInterval(alpha, interval);
        final double correctedBeta = angleInInterval(beta, interval);
        // ...then they are tested
        return Comparators.equals(correctedAlpha, correctedBeta);
    }

    /**
     * Tests if one angle is lower or equal to another after expressing them in the same interval.
     * 
     * @precondition both angles must be in the given interval modulo 2PI
     * 
     * @param alpha
     *        one angle
     * @param beta
     *        one angle
     * @param interval
     *        the interval to express the angles
     * 
     * @return boolean : true if lower or equal
     * 
     * @throws MathIllegalArgumentException
     *         if one angle is'nt in the interval, modulo 2PI
     * 
     * @since 1.0
     */
    public static boolean lowerOrEqual(final double alpha, final double beta, final AngleInterval interval) {
        // both angles are expressed in the interval...
        final double correctedAlpha = angleInInterval(alpha, interval);
        final double correctedBeta = angleInInterval(beta, interval);
        // ...then they are tested
        return Comparators.lowerOrEqual(correctedAlpha, correctedBeta);
    }

    /**
     * Tests if one angle is strictly lower than another after expressing them in the same interval.
     * 
     * @precondition both angles must be in the given interval modulo 2PI
     * 
     * @param alpha
     *        : one angle
     * @param beta
     *        : one angle
     * @param interval
     *        the interval to express the angles
     * @return boolean : true if lower
     * 
     * @throws MathIllegalArgumentException
     *         if one angle is'nt in the interval, modulo 2PI
     * 
     * @since 1.0
     */
    public static boolean lowerStrict(final double alpha, final double beta, final AngleInterval interval) {
        // both angles are expressed in the interval...
        final double correctedAlpha = angleInInterval(alpha, interval);
        final double correctedBeta = angleInInterval(beta, interval);
        // ...then they are tested
        return Comparators.lowerStrict(correctedAlpha, correctedBeta);
    }

    /**
     * Tests if one angle is greater or equal to another after expressing them in the same interval.
     * 
     * @precondition both angles must be in the given interval modulo 2PI
     * 
     * @param alpha
     *        : one angle
     * @param beta
     *        : one angle
     * @param interval
     *        the interval to express the angles
     * @return boolean : true is greater or equal
     * 
     * @throws MathIllegalArgumentException
     *         if one angle is'nt in the interval, modulo 2PI
     * 
     * @since 1.0
     */
    public static boolean greaterOrEqual(final double alpha, final double beta, final AngleInterval interval) {
        // both angles are expressed in the interval...
        final double correctedAlpha = angleInInterval(alpha, interval);
        final double correctedBeta = angleInInterval(beta, interval);
        // ...then they are tested
        return Comparators.greaterOrEqual(correctedAlpha, correctedBeta);
    }

    /**
     * Tests if one angle is strictly greater than another after expressing them in the same interval.
     * 
     * @precondition both angles must be in the given interval modulo 2PI
     * 
     * @param alpha
     *        : one angle
     * @param beta
     *        : one angle
     * @param interval
     *        the interval to express the angles
     * @return boolean : true is greater
     * 
     * @throws MathIllegalArgumentException
     *         if one angle is'nt in the interval, modulo 2PI
     * 
     * @since 1.0
     */
    public static boolean greaterStrict(final double alpha, final double beta, final AngleInterval interval) {
        // both angles are expressed in the interval...
        final double correctedAlpha = angleInInterval(alpha, interval);
        final double correctedBeta = angleInInterval(beta, interval);
        // ...then they are tested
        return Comparators.greaterStrict(correctedAlpha, correctedBeta);
    }

    /**
     * Computes the supplementary (PI - angle) of the input angle, and then tries to express it in the
     * input interval.
     * 
     * @param angle
     *        the angle to get the supplementary
     * @param interval
     *        the interval to express the result
     * @return double : supplementary angle
     * @throws MathIllegalArgumentException
     *         if the resulting angle is'nt in the interval, modulo 2PI
     * 
     *         See DV-MATHS_80 .
     * 
     * @since 1.0
     */
    public static double supplementaryAngle(final double angle, final AngleInterval interval) {
        // computation of the supplementary angle
        // the result is expressed in the interval
        return angleInInterval((FastMath.PI - angle), interval);
    }

    /**
     * Computes the complementary (PI/2 - angle) of the input angle, and then tries to express it in the
     * input interval.
     * 
     * @param angle
     *        the angle to get the complementary
     * @param interval
     *        the interval to express the result
     * @return double : complementary angle
     * @throws MathIllegalArgumentException
     *         if the resulting angle is'nt in the interval, modulo 2PI
     * 
     *         See DV-MATHS_80 .
     * 
     * @since 1.0
     */
    public static double complementaryAngle(final double angle, final AngleInterval interval) {
        // computation of the supplementary angle
        // the result is expressed in the interval
        return angleInInterval((MathUtils.HALF_PI - angle), interval);
    }

    /**
     * Computes the opposite of the input angle, and then tries to express it in the input interval.
     * 
     * @param angle
     *        the angle to get the complementary
     * @param interval
     *        the interval to express the result
     * @return double : opposite angle
     * @throws MathIllegalArgumentException
     *         if the resulting angle is'nt in the interval, modulo 2PI
     * 
     *         See DV-MATHS_80 .
     * 
     * @since 1.0
     */
    public static double oppositeAngle(final double angle, final AngleInterval interval) {
        // computation of the supplementary angle
        // the result is expressed in the interval
        return angleInInterval((angle + FastMath.PI), interval);
    }
}
