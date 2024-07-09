/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * HISTORY
* VERSION:4.8:DM:DM-2996:15/11/2021:[PATRIUS] Ajout d'une methode contains(double) dans la classe AngleInterval 
 * VERSION:4.5:DM:DM-2339:27/05/2020:Intervalles d'angles predefinis 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
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
 * @history Creation 25/07/11
 */
package fr.cnes.sirius.patrius.math.interval;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.Comparators;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * <p>
 * - This class describes an angle interval.
 * </p>
 * <p>
 * - It contains no method other than getters and setters : the operations on angles are available in the AngleTools
 * class
 * </p>
 * See DV-MATHS_50.
 * 
 * @useSample <p>
 *            There are two ways of building an angle interval : with the two end points, or with a reference angle and
 *            the interval length.
 *            </p>
 *            <p>
 *            AngleInterval angleInterval = new AngleInterval(lowerType, lowerAngle, upperAngle, upperType);
 *            </p>
 *            <p>
 *            AngleInterval angleInterval = new AngleInterval(reference, length, lowerType, upperType);
 *            </p>
 * 
 * @concurrency immutable
 * 
 * @author Thomas TRAPIER
 * 
 * @version $Id: AngleInterval.java 18021 2017-09-29 15:56:26Z bignon $
 * 
 * @since 1.0
 * 
 */
public final class AngleInterval extends AbstractInterval implements Serializable {

    /** Interval [ 0 ; 2pi [. */
    public static final AngleInterval ZERO_2PI = new AngleInterval(IntervalEndpointType.CLOSED, 0.0, MathUtils.TWO_PI,
            IntervalEndpointType.OPEN);

    /** Interval ] -2pi ; 0 ]. */
    public static final AngleInterval MINUS2PI_ZERO = new AngleInterval(IntervalEndpointType.OPEN, -MathUtils.TWO_PI,
            0.0, IntervalEndpointType.CLOSED);

    /** Interval [ -pi ; pi [. */
    public static final AngleInterval MINUSPI_PI = new AngleInterval(IntervalEndpointType.CLOSED, -FastMath.PI,
            FastMath.PI, IntervalEndpointType.OPEN);

    /** Serializable version identifier. */
    private static final long serialVersionUID = -5206233637720732029L;

    /** lower end angle value */
    private final double lowerAngle;
    /** upper end angle value */
    private final double upperAngle;
    /** reference angle value */
    private final double reference;
    /** interval length value */
    private final double length;

    /**
     * Constructor<br>
     * Needs the two end points values.
     * 
     * @param lowerEndPointIn
     *        lower end point type of the interval
     * @param lowerAngleIn
     *        lower angle of the interval
     * @param upperAngleIn
     *        upper angle of the interval
     * @param upperEndPointIn
     *        upper end point type of the interval
     * 
     * @throws MathIllegalArgumentException
     *         if the interval is invalid
     * 
     * @since 1.0
     */
    public AngleInterval(final IntervalEndpointType lowerEndPointIn,
            final double lowerAngleIn,
            final double upperAngleIn,
            final IntervalEndpointType upperEndPointIn) {
        super();

        // Validity test
        if (this.angleIntervalIsOk((upperAngleIn + lowerAngleIn) / 2.0, (upperAngleIn - lowerAngleIn), lowerEndPointIn,
                upperEndPointIn)) {

            // If the asked interval is valid, the object is constructed
            this.lowerAngle = lowerAngleIn;
            this.upperAngle = upperAngleIn;
            this.reference = (upperAngleIn + lowerAngleIn) / 2.0;
            this.length = (upperAngleIn - lowerAngleIn);
            this.setLowerEndPoint(lowerEndPointIn);
            this.setUpperEndPoint(upperEndPointIn);
        } else {
            // If the interval is invalid, an exception is thrown
            throw new MathIllegalArgumentException(PatriusMessages.PDB_INVALID_ANGLE_INTERVAL);
        }
    }

    /**
     * Constructor<br>
     * Needs the reference angle and the interval length.
     * 
     * @param referenceIn
     *        reference angle of the interval
     * @param lengthIn
     *        length of the interval
     * @param lowerEndPointIn
     *        lower end point type of the interval
     * @param upperEndPointIn
     *        upper end point type of the interval
     * 
     * @throws MathIllegalArgumentException
     *         if the interval is invalid
     * 
     * @since 1.0
     */
    public AngleInterval(final double referenceIn,
            final double lengthIn,
            final IntervalEndpointType lowerEndPointIn,
            final IntervalEndpointType upperEndPointIn) {
        super();

        // Validity test
        if (this.angleIntervalIsOk(referenceIn, lengthIn, lowerEndPointIn, upperEndPointIn)) {

            // If the asked interval is valid, the object is constructed
            this.reference = referenceIn;
            this.length = lengthIn;
            this.lowerAngle = referenceIn - lengthIn / 2.0;
            this.upperAngle = referenceIn + lengthIn / 2.0;
            this.setLowerEndPoint(lowerEndPointIn);
            this.setUpperEndPoint(upperEndPointIn);
        } else {
            // If the interval is invalid, an exception is thrown
            throw new MathIllegalArgumentException(PatriusMessages.PDB_INVALID_ANGLE_INTERVAL);
        }
    }

    /**
     * This method returns a String representing the interval, with boundaries as brackets
     * and the lower/upper values.<br>
     * Example : "] 0.0 rad , 1.2534 rad [" for an open interval.<br>
     * Warning : this representation is subject to change.
     * 
     * @return a String representation
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        // Example of a toString representation for this class :
        // "] 0.0 rad , 3.1415 rad ["
        final String leftB = "[";
        final String rightB = "]";
        final String radUnit = " rad";
        final String spc = " ";
        // Starting and ending brackets determined here
        final String startB = (this.getLowerEndPoint().equals(IntervalEndpointType.CLOSED)) ? leftB : rightB;
        final String endB = (this.getUpperEndPoint().equals(IntervalEndpointType.CLOSED)) ? rightB : leftB;
        // Values added here
        return startB + spc + this.lowerAngle + radUnit + " , " + this.upperAngle + radUnit + spc + endB;
    }

    /**
     * @return the lowerAngle
     */
    public double getLowerAngle() {
        return this.lowerAngle;
    }

    /**
     * @return the upperAngle
     */
    public double getUpperAngle() {
        return this.upperAngle;
    }

    /**
     * @return the reference
     */
    public double getReference() {
        return this.reference;
    }

    /**
     * @return the length
     */
    public double getLength() {
        return this.length;
    }

    /**
     * Returns true if the angle is contained in this interval, false otherwise.
     * Boundaries OPEN/CLOSED are taken into account. No modulo is performed (i.e. -Pi is not included in [0, 2Pi]).
     * @param angle an angle
     * @return true if the angle is contained in this interval, false otherwise
     */
    public boolean contains(final double angle) {
        boolean res = true;
        if (getLowerEndPoint().equals(IntervalEndpointType.OPEN)) {
            res &= angle > lowerAngle;
        } else {
            res &= angle >= lowerAngle;
        }
        if (getUpperEndPoint().equals(IntervalEndpointType.OPEN)) {
            res &= angle < upperAngle;
        } else {
            res &= angle <= upperAngle;
        }
        return res;
    }
    
    /**
     * Tests the interval when constructed.
     * See DV-MATHS_50.
     * 
     * @param referenceIn
     *        reference angle of the interval
     * @param lengthIn
     *        length of the interval
     * @param upperEndPoint
     *        upper end point type of the interval
     * @param lowerEndPoint
     *        lower end point type of the interval
     * @return boolean : true if the tested interval is valid
     * 
     * @since 1.0
     */
    private boolean angleIntervalIsOk(final double referenceIn,
            final double lengthIn,
            final IntervalEndpointType lowerEndPoint,
            final IntervalEndpointType upperEndPoint) {
        boolean intervalIsOk = true;

        // The reference or length should not be Nan
        if (Double.isNaN(referenceIn) || Double.isNaN(lengthIn)) {
            intervalIsOk = false;
        }

        // Check the length value
        if (intervalIsOk) {
            intervalIsOk = this.angleIntervalLengthIsOK(lengthIn, lowerEndPoint, upperEndPoint);
        }

        return intervalIsOk;
    }

    /**
     * Tests the interval length when constructed.
     * 
     * @param lengthIn
     *        length
     * @param lowerEndPoint
     *        lower end point
     * @param upperEndPoint
     *        upper end point
     * 
     * @return true if the length is valid
     */
    private boolean angleIntervalLengthIsOK(final double lengthIn,
            final IntervalEndpointType lowerEndPoint,
            final IntervalEndpointType upperEndPoint) {
        boolean intervalIsOk = true;
        // The interval can't be longer than two pi
        if (Comparators.greaterStrict(lengthIn, MathUtils.TWO_PI)) {
            intervalIsOk = false;
        }
        if (Comparators.equals(lengthIn, MathUtils.TWO_PI) && upperEndPoint == IntervalEndpointType.CLOSED
                && lowerEndPoint == IntervalEndpointType.CLOSED) {
            intervalIsOk = false;
        }
        // The interval length can't be negative
        if (Comparators.lowerStrict(lengthIn, 0.0)) {
            intervalIsOk = false;
        }
        // The interval must contain at least one real value
        if (Precision.equals(lengthIn, 0.0, Comparators.DOUBLE_COMPARISON_EPSILON)
                && (upperEndPoint == IntervalEndpointType.OPEN || lowerEndPoint == IntervalEndpointType.OPEN)) {
            intervalIsOk = false;
        }
        return intervalIsOk;
    }

}
