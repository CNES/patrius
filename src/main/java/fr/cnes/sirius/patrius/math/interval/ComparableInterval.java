/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 *
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
 * @history 27/09/2011
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.4:FA:FA-2108:04/10/2019:[PATRIUS] Incoherence hash code/equals dans ComparableInterval
 * VERSION:4.3:DM:DM-2089:15/05/2019:[PATRIUS] passage a Java 8
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:1323:13/11/2017:change log message
 * VERSION::DM:1936:23/10/2018: add new methods to intervals classes
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.math.interval;

import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Class describing an interval of <code>Comparable</code> data.
 *
 * <p>
 * This class is copy of {@link fr.cnes.sirius.patrius.math.interval.ComparableInterval}. It adds the methods
 * {@link #mergeTo(ComparableInterval)}, {@link #getIntersectionWith(ComparableInterval)} and
 * {@link #extendTo(Comparable)}, as well as an additional constructor.
 * </p>
 *
 * <p>
 * The upper and lower boundaries have to be Comparable, so that the correct order can be checked. This class can be
 * extended and {@link #toString()} may be overridden.
 * </p>
 *
 * <p>
 * The generic class must implement <code>java.lang.Comparable</code><br>
 * It is HIGHLY recommended this class be immutable!
 * </p>
 *
 * @useSample For instance, an interval of Doubles ]1.0,2.0[ is created this way :
 *            <p>
 *            <code>
 *            Interval<Double> interval = new Interval<Double>(IntervalEndpointType.OPEN, 1.0, 2.0,
 *            IntervalEndpointType.OPEN);
 *            </code>
 *            </p>
 *
 * @param <T>
 *        the nature of ending points
 *
 * @concurrency matches the concurrency of the parameter type : using an immutable type is HIGHLY
 *              recommended
 * 
 * @author Pierre Cardoso
 * 
 * @version $Id: ComparableInterval.java 18083 2017-10-02 16:54:39Z bignon $
 * 
 * @since 3.0
 * 
 */
public class ComparableInterval<T extends Comparable<T>> extends GenericInterval<T> implements
    Comparable<ComparableInterval<T>> {

    /** Serializable version identifier. */
    private static final long serialVersionUID = -3043430443585844772L;

    /** Root int for hash code. */
    private static final int ROOTINT = 52;

    /** Hash code. */
    private transient Integer hashCodeNumber = null;

    /**
     * Builds a new {@link ComparableInterval} with closed endpoints.
     *
     * <p>
     * The input parameters must not be null. The lower and upper endpoints must also be properly ordered. Otherwise a
     * <code>MathIllegalArgumentException</code> is thrown.
     * </p>
     *
     * @param lowerData the lower end data value
     * @param upperData the upper end data value
     * @throws MathIllegalArgumentException when the interval is invalid (unchecked exception)
     * @since 4.2
     */
    public ComparableInterval(final T lowerData, final T upperData) {
        this(IntervalEndpointType.CLOSED, lowerData, upperData, IntervalEndpointType.CLOSED);
    }

    /**
     * Constructor.<br>
     * The input parameters have to be not null.<br>
     * The lower and upper endpoints also have to be properly ordered.<br>
     * Otherwise a <code>MathIllegalArgumentException</code> is thrown.
     * 
     * @param lowerDataIn
     *        lower end data value
     * @param upperDataIn
     *        upper end data value
     * @param lowerEndpointIn
     *        lower end point state
     * @param upperEndpointIn
     *        upper end point state
     * @throws MathIllegalArgumentException
     *         when the interval is invalid (unchecked exception)
     * @since 1.0
     */
    public ComparableInterval(final IntervalEndpointType lowerEndpointIn, final T lowerDataIn,
        final T upperDataIn, final IntervalEndpointType upperEndpointIn) {
        super(lowerEndpointIn, lowerDataIn, upperDataIn, upperEndpointIn);
        // Complimentary validity tests for comparable values
        if (!this.comparableIntervalIsOK(lowerDataIn, upperDataIn)) {
            throw new MathIllegalArgumentException(PatriusMessages.INCORRECT_INTERVAL);
        }
        this.hashCodeNumber = this.computeHashCode();
    }

    /**
     * Returns true if the parameters describe a valid interval.<br>
     * 
     * 
     * @param lowerDataIn
     *        lower end data value
     * @param upperDataIn
     *        upper end data value
     * 
     * @return true when the interval represented by the parameters is valid
     * 
     * @since 1.0
     */
    private boolean comparableIntervalIsOK(final T lowerDataIn, final T upperDataIn) {
        boolean validFlag = true;

        // The lower end should be lower...than the upper end
        if (lowerDataIn.compareTo(upperDataIn) > 0) {
            validFlag = false;
        }

        return validFlag;
    }

    /**
     * Returns true if the provided value belongs to the interval.
     * 
     * @param cmp
     *        the tested value
     * @return true if the value belongs to the interval
     */
    public final boolean contains(final Comparable<T> cmp) {
        boolean isIn = true;
        // Test for lower endpoint
        // (depends on endpoint type)
        switch (this.getLowerEndpoint()) {
            case CLOSED:
                if (cmp.compareTo(this.getLowerData()) < 0) {
                    isIn = false;
                }
                break;
            case OPEN:
                if (cmp.compareTo(this.getLowerData()) <= 0) {
                    isIn = false;
                }
                break;
            default:
                // Cannot happen
                break;
        }
        // Test for upper endpoint
        // (depends on endpoint type)
        if (isIn) {
            switch (this.getUpperEndpoint()) {
                case CLOSED:
                    if (cmp.compareTo(this.getUpperData()) > 0) {
                        isIn = false;
                    }
                    break;
                case OPEN:
                    if (cmp.compareTo(this.getUpperData()) >= 0) {
                        isIn = false;
                    }
                    break;
                default:
                    // Cannot happen
                    break;
            }
        }
        return isIn;
    }

    /**
     * Returns true if the two intervals overlap.
     *
     * <p>
     * The intervals [l1 ; u1] and [l2 ; u2] overlap if u1>=l2 AND l1<=u2. If one of the end-points is open, the
     * comparison is strict.
     * </p>
     * 
     * @param interval
     *        the other interval
     * @return true if the two intervals overlap
     */
    public final boolean overlaps(final ComparableInterval<T> interval) {
        // [l1 ; u1] and [l2 ; u2] overlap if
        // u1 >= l2 AND l1 <= u2
        // if one of the endpoints is open, its test changes to strict
        boolean cond1 = false;
        boolean cond2 = false;
        if (this.getUpperEndpoint() == IntervalEndpointType.OPEN ||
            interval.getLowerEndpoint() == IntervalEndpointType.OPEN) {
            // strict comparison
            cond1 = this.getUpperData().compareTo(interval.getLowerData()) > 0;
        } else {
            // non-strict comparison
            cond1 = this.getUpperData().compareTo(interval.getLowerData()) >= 0;
        }
        if (this.getLowerEndpoint() == IntervalEndpointType.OPEN ||
            interval.getUpperEndpoint() == IntervalEndpointType.OPEN) {
            // strict comparison
            cond2 = this.getLowerData().compareTo(interval.getUpperData()) < 0;
        } else {
            // non-strict comparison
            cond2 = this.getLowerData().compareTo(interval.getUpperData()) <= 0;
        }
        // intervals overlap when the two conditions are met
        return cond1 && cond2;
    }

    /**
     * Returns true if this interval includes the other.
     *
     * <p>
     * The interval [l1; u1] includes the interval [l2; u2] if l1<=l2 and u1>=u2. If the end-point of the including
     * interval is open and the end-point of the included interval is closed, the comparison is strict.
     * </p>
     * 
     * @param interval
     *        the interval tested for inclusion
     * @return true if the provided interval is included
     */
    public final boolean includes(final ComparableInterval<T> interval) {
        boolean cond1 = false;
        boolean cond2 = false;
        // [l1 ; u1] includes [l2 ; u2] if
        // l1 <= l2 AND u1 >= u2
        // if the includer endpoint is open and the included is closed,
        // the comparison becomes strict
        if (this.getLowerEndpoint() == IntervalEndpointType.OPEN &&
            interval.getLowerEndpoint() == IntervalEndpointType.CLOSED) {
            // strict comparison
            cond1 = this.getLowerData().compareTo(interval.getLowerData()) < 0;
        } else {
            cond1 = this.getLowerData().compareTo(interval.getLowerData()) <= 0;
        }
        if (this.getUpperEndpoint() == IntervalEndpointType.OPEN &&
            interval.getUpperEndpoint() == IntervalEndpointType.CLOSED) {
            // strict comparison
            cond2 = this.getUpperData().compareTo(interval.getUpperData()) > 0;
        } else {
            cond2 = this.getUpperData().compareTo(interval.getUpperData()) >= 0;
        }
        return cond1 && cond2;
    }

    /**
     * Returns true when this interval's lower end-point connects with another interval's upper
     * end-point.
     *
     * <p>
     * Two end-points connects if their values are identical and their end-points have different types (one is open and
     * the other is closed).
     * </p>
     * 
     * @param interval
     *        the interval tested for connection
     * @return true if the provided interval is connected by its upper point
     */
    public final boolean isConnectedTo(final ComparableInterval<T> interval) {
        final boolean rez;
        if (interval.getUpperData().compareTo(this.getLowerData()) == 0) {
            // the lower point of the reference interval and the upper point of the
            // input interval are the same:
            if (interval.getUpperEndpoint().compareTo(this.getLowerEndpoint()) == 0) {
                rez = false;
            } else {
                // the two points are one open and the other one closed:
                rez = true;
            }
        } else {
            rez = false;
        }
        return rez;
    }

    /**
     * Returns this interval merged with another interval (if they can be merged).
     *
     * <p>
     * To be merged, the two intervals must overlap or be connected. If that is not the case, the method returns
     * <code>null</code>.
     * </p>
     *
     * @param interval
     *        the other interval
     *
     * @return the merged interval or null if the intervals do not overlap and are not connected
     */
    public ComparableInterval<T> mergeTo(final ComparableInterval<T> interval) {
        ComparableInterval<T> mergedInterval = null;

        // The two intervals are overlapping
        if (this.overlaps(interval)) {
            // This interval includes the input interval
            if (this.includes(interval)) {
                mergedInterval = new ComparableInterval<T>(this.getLowerEndpoint(),
                    this.getLowerData(), this.getUpperData(), this.getUpperEndpoint());
            } else if (interval.includes(this)) {
                // The input interval includes the reference interval
                mergedInterval = new ComparableInterval<T>(interval.getLowerEndpoint(),
                    interval.getLowerData(), interval.getUpperData(),
                    interval.getUpperEndpoint());
            } else {
                // No interval is included in the other
                if (this.compareTo(interval) > 0) {
                    // This interval is after the input interval
                    mergedInterval = new ComparableInterval<T>(interval.getLowerEndpoint(),
                        interval.getLowerData(), this.getUpperData(),
                        this.getUpperEndpoint());
                } else {
                    // This interval is before the input interval, or the intervals are identical
                    mergedInterval = new ComparableInterval<T>(this.getLowerEndpoint(),
                        this.getLowerData(), interval.getUpperData(),
                        interval.getUpperEndpoint());
                }
            }
        } else if (this.isConnectedTo(interval)) {
            // The input interval is followed by this interval
            mergedInterval = new ComparableInterval<T>(interval.getLowerEndpoint(),
                interval.getLowerData(), this.getUpperData(), this.getUpperEndpoint());
        } else if (interval.isConnectedTo(this)) {
            // This interval is followed by the input interval
            mergedInterval = new ComparableInterval<T>(this.getLowerEndpoint(),
                this.getLowerData(), interval.getUpperData(), interval.getUpperEndpoint());
        }

        return mergedInterval;
    }

    /**
     * Returns the intersection with another interval.
     *
     * <p>
     * If the intervals do not overlap, the method returns null.
     * </p>
     *
     * @param interval
     *        the other interval
     *
     * @return the intersection between the two intervals or null if the intervals do not overlap
     */
    public ComparableInterval<T> getIntersectionWith(final ComparableInterval<T> interval) {
        ComparableInterval<T> intersection = null;

        // The two intervals are overlapping
        if (this.overlaps(interval)) {
            final T startIntersection;
            final T endIntersection;

            final IntervalEndpointType startType;
            final IntervalEndpointType endType;

            // This interval starts before the input interval
            if (this.compareLowerEndTo(interval) < 0) {
                startIntersection = interval.getLowerData();
                startType = interval.getLowerEndpoint();
            } else {
                // The input interval starts before this interval
                startIntersection = this.getLowerData();
                startType = this.getLowerEndpoint();
            }

            // This interval ends after the input interval
            if (this.compareUpperEndTo(interval) > 0) {
                endIntersection = interval.getUpperData();
                endType = interval.getUpperEndpoint();
            } else {
                // The input interval end before this interval
                endIntersection = this.getUpperData();
                endType = this.getUpperEndpoint();
            }

            // creates the new intersection interval:
            intersection = new ComparableInterval<T>(startType, startIntersection, endIntersection,
                endType);
        }

        return intersection;
    }

    /**
     * Returns the interval after extending it so that it includes (closed endpoint) or is
     * connected (open endpoint) to the specified value.
     *
     * @param value
     *        the target value
     *
     * @return the extended interval
     */
    public ComparableInterval<T> extendTo(final T value) {
        // Interval lower and upper endpoints
        T lower = this.getLowerData();
        T upper = this.getUpperData();

        // Extend the bounds
        if (lower.compareTo(value) > 0) {
            lower = value;
        }

        if (upper.compareTo(value) < 0) {
            upper = value;
        }

        return new ComparableInterval<T>(this.getLowerEndpoint(), lower, upper, this.getUpperEndpoint());
    }

    /**
     * Compares the lower end point with the lower end point of the given interval.
     *
     * <p>
     * It returns a negative integer if the lower end point is inferior than the lower end point of the given interval,
     * zero if they are equal, or a positive integer if is it superior. The method also compares the lower end point
     * type of the two intervals.
     * </p>
     * 
     * @param interval
     *        the interval tested for lower end point comparison
     * @return a negative integer, zero, or a positive integer
     */
    public final int compareLowerEndTo(final ComparableInterval<T> interval) {
        int cmp = this.getLowerData().compareTo(interval.getLowerData());
        if (cmp == 0) {
            // the lower data are the same, check the end point type:
            cmp = interval.getLowerEndpoint().compareTo(this.getLowerEndpoint());
        }
        return cmp;
    }

    /**
     * Compares the upper end point with the upper end point of the given interval.
     *
     * <p>
     * It returns a negative integer if the upper end point is inferior than the upper end point of the given interval,
     * zero if they are equal, or a positive integer if is it superior. The method also compares the upper end point
     * type of the two intervals.
     * </p>
     * 
     * @param interval
     *        the interval tested for upper end point comparison
     * @return a negative integer, zero, or a positive integer
     */
    public final int compareUpperEndTo(final ComparableInterval<T> interval) {
        int cmp = this.getUpperData().compareTo(interval.getUpperData());
        if (cmp == 0) {
            // the upper data are the same, check the end point type:
            cmp = this.getUpperEndpoint().compareTo(interval.getUpperEndpoint());
        }
        return cmp;
    }

    /**
     * Compares this interval with the specified interval. It returns a negative integer, zero, or a
     * positive integer.
     *
     * <ul>
     * <li>it checks the lower data and returns a negative (positive) integer if its lower data is inferior (superior)
     * than the lower data of the input interval;
     * <li>if the lower data are identical, it checks the lower end points and it returns a negative integer if its
     * lower end point is closed while the lower end point of the input interval is open (and vice-versa);
     * <li>if the lower data and end points are identical, it checks the upper data and it returns a negative (positive)
     * integer if its upper data is inferior (superior) than the upper data of the input interval;
     * <li>if the upper data are identical, it checks the upper end points and it returns a negative integer if its
     * upper end point is open while the upper end point of the input interval is closed (and vice-versa);
     * <li>if both upper/lower data and end points are identical, it returns zero.
     * </ul>
     * 
     * @param interval
     *        the time interval to be compared.
     * @return a negative integer, zero, or a positive integer
     * 
     * @throws ClassCastException
     *         if the specified object's type prevents it
     *         from being compared to this object.
     */
    @Override
    public int compareTo(final ComparableInterval<T> interval) {
        final int rez;
        // first fast check
        if (interval == this) {
            rez = 0;
            return rez;
        }
        // first check the lower date:
        final int lowerDateCompare = this.getLowerData().compareTo(interval.getLowerData());
        if (lowerDateCompare == 0) {
            // check the OPEN/CLOSED boundary:
            final int lowerEndpoint = interval.getLowerEndpoint().compareTo(
                    this.getLowerEndpoint());
            if (lowerEndpoint == 0) {
                // if the lower dates are the same, we order by the upper dates:
                final int upperDateCompare = this.getUpperData().compareTo(interval.getUpperData());
                if (upperDateCompare == 0) {
                    // check the OPEN/CLOSED boundary:
                    final int upperEndpoint = this.getUpperEndpoint().compareTo(
                        interval.getUpperEndpoint());
                    rez = upperEndpoint;
                } else {
                    rez = upperDateCompare;
                }
            } else {
                rez = lowerEndpoint;
            }
        } else {
            rez = lowerDateCompare;
        }
        return rez;
    }

    /**
     * Checks if the instance represents the same {@link ComparableInterval} as another instance.
     * 
     * @param interval
     *        other interval
     * @return true if the instance and the other are equals
     */
    @Override
    public boolean equals(final Object interval) {

        if (interval == this) {
            // first fast check
            return true;
        }
        if ((interval != null) && (interval instanceof ComparableInterval<?>)) {
            final ComparableInterval<?> cInt = (ComparableInterval<?>) interval;
            return this.getLowerData().equals(cInt.getLowerData()) &&
                this.getUpperData().equals(cInt.getUpperData()) &&
                this.getLowerEndpoint().equals(cInt.getLowerEndpoint()) &&
                this.getUpperEndpoint().equals(cInt.getUpperEndpoint());
        }
        return false;
    }

    /**
     * Computes the hash code.<br>
     * The standard "clever" hash algorithm is used.
     * 
     * @return the hash code value.
     */
    private int computeHashCode() {
        // A not zero random "root int"
        int result = ROOTINT;
        // An efficient multiplier (JVM optimizes 31 * i as (i << 5) - 1 )
        final int effMult = 31;
        // Good hashcode : it's the same for "equal" ComparableInterval, but
        // reasonably sure it's different otherwise.
        result = effMult * result + this.getLowerData().hashCode();
        result = effMult * result + this.getUpperData().hashCode();
        result = effMult * result + this.getLowerEndpoint().computeHashCode();
        result = effMult * result + this.getUpperEndpoint().computeHashCode();
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        if (this.hashCodeNumber == null) {
            this.hashCodeNumber = this.computeHashCode();
        }
        return this.hashCodeNumber;
    }
}
