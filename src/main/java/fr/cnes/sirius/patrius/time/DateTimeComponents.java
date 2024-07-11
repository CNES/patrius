/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 */
/*
 *
 * HISTORY
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:256:01/08/2014:Changed AbsoluteDate.toString to take into account seconds precision
 * VERSION::FA:367:21/11/2014:Corrected END-HISTORY key word
 * VERSION::FA:363:03/12/2014:Solved toString round date bug
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.time;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.Constants;

/**
 * Holder for date and time components.
 * <p>
 * This class is a simple holder with no processing methods.
 * </p>
 * <p>
 * Instance of this class are guaranteed to be immutable.
 * </p>
 * 
 * @see AbsoluteDate
 * @see DateComponents
 * @see TimeComponents
 * @author Luc Maisonobe
 */
public class DateTimeComponents implements Serializable, Comparable<DateTimeComponents> {

    /** Serializable UID. */
    private static final long serialVersionUID = 5061129505488924484L;

    /** 16. */
    private static final int SIXTEEN = 16;
    
    /** 10 */
    private static final int TEN = 10;

    /** Date component. */
    private final DateComponents date;

    /** Time component. */
    private final TimeComponents time;

    /**
     * Build a new instance from its components.
     * 
     * @param dateIn
     *        date component
     * @param timeIn
     *        time component
     */
    public DateTimeComponents(final DateComponents dateIn, final TimeComponents timeIn) {
        this.date = dateIn;
        this.time = timeIn;
    }

    /**
     * Build an instance from raw level components.
     * 
     * @param year
     *        year number (may be 0 or negative for BC years)
     * @param month
     *        month number from 1 to 12
     * @param day
     *        day number from 1 to 31
     * @param hour
     *        hour number from 0 to 23
     * @param minute
     *        minute number from 0 to 59
     * @param second
     *        second number from 0.0 to 60.0 (excluded)
     * @exception IllegalArgumentException
     *            if inconsistent arguments
     *            are given (parameters out of range, february 29 for non-leap years,
     *            dates during the gregorian leap in 1582 ...)
     */
    public DateTimeComponents(final int year, final int month, final int day,
        final int hour, final int minute, final double second) {
        this.date = new DateComponents(year, month, day);
        this.time = new TimeComponents(hour, minute, second);
    }

    /**
     * Build an instance from raw level components.
     * 
     * @param year
     *        year number (may be 0 or negative for BC years)
     * @param month
     *        month enumerate
     * @param day
     *        day number from 1 to 31
     * @param hour
     *        hour number from 0 to 23
     * @param minute
     *        minute number from 0 to 59
     * @param second
     *        second number from 0.0 to 60.0 (excluded)
     * @exception IllegalArgumentException
     *            if inconsistent arguments
     *            are given (parameters out of range, february 29 for non-leap years,
     *            dates during the gregorian leap in 1582 ...)
     */
    public DateTimeComponents(final int year, final Month month, final int day,
        final int hour, final int minute, final double second) {
        this.date = new DateComponents(year, month, day);
        this.time = new TimeComponents(hour, minute, second);
    }

    /**
     * Build an instance from raw level components.
     * <p>
     * The hour is set to 00:00:00.000.
     * </p>
     * 
     * @param year
     *        year number (may be 0 or negative for BC years)
     * @param month
     *        month number from 1 to 12
     * @param day
     *        day number from 1 to 31
     * @exception IllegalArgumentException
     *            if inconsistent arguments
     *            are given (parameters out of range, february 29 for non-leap years,
     *            dates during the gregorian leap in 1582 ...)
     */
    public DateTimeComponents(final int year, final int month, final int day) {
        this.date = new DateComponents(year, month, day);
        this.time = TimeComponents.H00;
    }

    /**
     * Build an instance from raw level components.
     * <p>
     * The hour is set to 00:00:00.000.
     * </p>
     * 
     * @param year
     *        year number (may be 0 or negative for BC years)
     * @param month
     *        month enumerate
     * @param day
     *        day number from 1 to 31
     * @exception IllegalArgumentException
     *            if inconsistent arguments
     *            are given (parameters out of range, february 29 for non-leap years,
     *            dates during the gregorian leap in 1582 ...)
     */
    public DateTimeComponents(final int year, final Month month, final int day) {
        this.date = new DateComponents(year, month, day);
        this.time = TimeComponents.H00;
    }

    /**
     * Build an instance from a seconds offset with respect to another one.
     * 
     * @param reference
     *        reference date/time
     * @param offset
     *        offset from the reference in seconds
     * @see #offsetFrom(DateTimeComponents)
     */
    public DateTimeComponents(final DateTimeComponents reference,
        final double offset) {

        // extract linear data from reference date/time
        int day = reference.getDate().getJ2000Day();
        double seconds = reference.getTime().getSecondsInDay();

        // apply offset
        seconds += offset;

        // fix range
        final int dayShift = (int) MathLib.floor(seconds / Constants.JULIAN_DAY);
        seconds -= Constants.JULIAN_DAY * dayShift;
        day += dayShift;

        // set up components
        this.date = new DateComponents(day);
        this.time = new TimeComponents(seconds);

    }

    /**
     * Parse a string in ISO-8601 format to build a date/time.
     * <p>
     * The supported formats are all date formats supported by {@link DateComponents#parseDate(String)} and all time
     * formats supported by {@link TimeComponents#parseTime(String)} separated by the standard time separator 'T', or
     * date components only (in which case a 00:00:00 hour is implied). Typical examples are 2000-01-01T12:00:00Z or
     * 1976W186T210000.
     * </p>
     * 
     * @param string
     *        string to parse
     * @return a parsed date/time
     * @exception IllegalArgumentException
     *            if string cannot be parsed
     */
    public static DateTimeComponents parseDateTime(final String string) {

        // is there a time ?
        final int tIndex = string.indexOf('T');
        if (tIndex > 0) {
            return new DateTimeComponents(DateComponents.parseDate(string.substring(0, tIndex)),
                TimeComponents.parseTime(string.substring(tIndex + 1)));
        }

        return new DateTimeComponents(DateComponents.parseDate(string), TimeComponents.H00);

    }

    /**
     * Compute the seconds offset between two instances.
     * 
     * @param dateTime
     *        dateTime to subtract from the instance
     * @return offset in seconds between the two instants
     *         (positive if the instance is posterior to the argument)
     * @see #DateTimeComponents(DateTimeComponents, double)
     */
    public double offsetFrom(final DateTimeComponents dateTime) {
        final int dateOffset = this.date.getJ2000Day() - dateTime.date.getJ2000Day();
        final double timeOffset = this.time.getSecondsInDay() - dateTime.time.getSecondsInDay();
        return Constants.JULIAN_DAY * dateOffset + timeOffset;
    }

    /**
     * Get the date component.
     * 
     * @return date component
     */
    public DateComponents getDate() {
        return this.date;
    }

    /**
     * Get the time component.
     * 
     * @return time component
     */
    public TimeComponents getTime() {
        return this.time;
    }

    /** {@inheritDoc} */
    @Override
    public int compareTo(final DateTimeComponents other) {
        final int dateComparison = this.date.compareTo(other.date);
        final int res;
        if (dateComparison < 0) {
            res = -1;
        } else if (dateComparison > 0) {
            res = 1;
        } else {
            res = this.time.compareTo(other.time);
        }
        return res;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object other) {
        try {
            final DateTimeComponents otherDateTime = (DateTimeComponents) other;
            return (otherDateTime != null) &&
                this.date.equals(otherDateTime.date) && this.time.equals(otherDateTime.time);
        } catch (final ClassCastException cce) {
            return false;
        }
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return (this.date.hashCode() << SIXTEEN) ^ this.time.hashCode();
    }

    /**
     * Return a string representation of this pair.
     * <p>
     * The format used is ISO8601.
     * </p>
     * 
     * @return string representation of this pair
     */
    @Override
    public String toString() {
        return this.date.toString() + 'T' + this.time.toString();
    }

    /**
     * Return a string representation of this pair.
     * <p>
     * The format used is ISO8601.
     * </p>
     * 
     * @param precision
     *        digit number of the seconds fractional part
     * @param isTimeNearLeapSecond
     *        true if the date is inside or immediately before
     *        a leap second. It is used to set the upper boundary of the current day:
     *        23:59:60.99.. when true, 23:59:59.99.. when false.
     * @return a string representation of the instance,
     *         in ISO-8601 format with a seconds accuracy defined as input
     */
    protected String toString(final int precision, final boolean isTimeNearLeapSecond) {
        final double limit;
        if (isTimeNearLeapSecond) {
            // compute the day's upper boundary (23:59:60.9...5) with respect to the precision:
            limit = Constants.JULIAN_DAY + 1 - 5.0 * MathLib.pow(TEN, -(precision + 1));
        } else {
            // compute the day's upper boundary (23:59:59.9...5) with respect to the precision:
            limit = Constants.JULIAN_DAY - 5.0 * MathLib.pow(TEN, -(precision + 1));
        }
        DateComponents shiftedDate = this.date;
        if (this.time.getSecondsInDay() >= limit) {
            // if the current date is beyond the limit, switch to the next day:
            shiftedDate = new DateComponents(this.date, 1);
        }
        // compute the current time string:
        final String timeStr = this.time.toString(precision, isTimeNearLeapSecond);
        return shiftedDate.toString() + 'T' + timeStr;
    }

}
