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
 * VERSION::FA:229:24/03/2014:Corrected digital quality absorption problem
 * VERSION::DM:256:01/08/2014:Changed AbsoluteDate.toString to take into account seconds precision
 * VERSION::FA:363:03/12/2014:Solved toString round date bug
 * VERSION::FA:383:08/12/2014:Corrected digital quality absorption problem
 * VERSION::FA:712:23/12/2016:Raise IllegalArgumentException if date has a bad format instead of NullPointerException
 * VERSION::FA:1312:15/11/2017:Improve TimeComponents accuracy
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.time;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Class representing a time within the day broken up as hour,
 * minute and second components.
 * <p>
 * Instances of this class are guaranteed to be immutable.
 * </p>
 * 
 * @see DateComponents
 * @see DateTimeComponents
 * @author Luc Maisonobe
 */
public class TimeComponents implements Serializable, Comparable<TimeComponents> {

    /** Default digit number of the seconds fractional part. */
    public static final int DEFAULT_SECONDS_PRECISION = 3;

    /** Constant for commonly used hour 00:00:00. */
    public static final TimeComponents H00 = new TimeComponents(0, 0, 0);

    /** Constant for commonly used hour 12:00:00. */
    public static final TimeComponents H12 = new TimeComponents(12, 0, 0);

    /** Hash code generator. */
    private static final int HASH_CODE_GENERATOR_1 = 16;

    /** Hash code generator. */
    private static final int HASH_CODE_GENERATOR_2 = 32;

    /** Number of hours in a day. */
    private static final double HOURS_IN_DAY = 24;

    /** Number of minutes in an hour. */
    private static final int MIN_IN_HOUR = 60;

    /** Number of seconds in a minute. */
    private static final double SEC_IN_MIN = 60;
    
    /** Number of seconds in an hour. */
    private static final double SEC_IN_HOUR = 3600;

    /** Decimal separator. */
    private static final CharSequence DOT = ".";

    /** Serializable UID. */
    private static final long serialVersionUID = -8566834296299377436L;

    /** Format for hours and minutes. */
    private static final DecimalFormat TWO_DIGITS = new DecimalFormat("00");

    /** Basic and extends formats for local time, UTC time (only 0 difference with UTC is supported). */
    private static final Pattern ISO8601_FORMATS = Pattern
        .compile("^(\\d\\d):?(\\d\\d):?(\\d\\d(?:[.,]\\d+)?)(?:Z|[-+]00(?::00)?)?$");
    
    /** 8. */
    private static final int EIGHT = 8;
    
    /** 10. */
    private static final int TEN = 10;

    /** Hour number. */
    private final int hour;

    /** Minute number. */
    private final int minute;

    /** Second number. */
    private final int second;

    /** Fractional part of seconds. */
    private final double fracSeconds;

    /**
     * Build a time from its clock elements.
     * <p>
     * Note that seconds between 60.0 (inclusive) and 61.0 (exclusive) are allowed in this method, since they do occur
     * during leap seconds introduction in the {@link UTCScale UTC} time scale.
     * </p>
     * 
     * @param hourIn
     *        hour number from 0 to 23
     * @param minuteIn
     *        minute number from 0 to 59
     * @param secondIn
     *        second number from 0.0 to 61.0 (excluded)
     * @exception IllegalArgumentException
     *            if inconsistent arguments
     *            are given (parameters out of range)
     */
    public TimeComponents(final int hourIn, final int minuteIn, final double secondIn) {
        this(hourIn, minuteIn, (int) MathLib.floor(secondIn), secondIn - MathLib.floor(secondIn));
    }

    /**
     * Build a time from its clock elements.
     * <p>
     * Note that seconds between 60.0 (inclusive) and 61.0 (exclusive) are allowed in this method, since they do occur
     * during leap seconds introduction in the {@link UTCScale UTC} time scale.
     * </p>
     * 
     * @param hourIn
     *        hour number from 0 to 23
     * @param minuteIn
     *        minute number from 0 to 59
     * @param secondIn
     *        second number from 0.0 to 61.0 (excluded)
     * @param fracSecondsIn
     *        fractional part of seconds
     * @exception IllegalArgumentException
     *            if inconsistent arguments
     *            are given (parameters out of range)
     */
    public TimeComponents(final int hourIn, final int minuteIn, final int secondIn, final double fracSecondsIn) {

        // range check
        final boolean checkHours = (hourIn < 0) || (hourIn > HOURS_IN_DAY - 1);
        final boolean checkMinutes = (minuteIn < 0) || (minuteIn > MIN_IN_HOUR - 1);
        final boolean checkSeconds = (secondIn < 0) || (secondIn >= SEC_IN_MIN + 1);
        if (checkHours || checkMinutes || checkSeconds) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.NON_EXISTENT_HMS_TIME,
                hourIn, minuteIn, secondIn);
        }

        this.hour = hourIn;
        this.minute = minuteIn;
        this.second = secondIn;
        this.fracSeconds = fracSecondsIn;
    }

    /**
     * Build a time from the second number within the day.
     * 
     * @param secondInDay
     *        second number from 0.0 to {@link Constants#JULIAN_DAY} (excluded)
     */
    public TimeComponents(final double secondInDay) {
        this(0, secondInDay);
    }

    /**
     * Build a time from the second number within the day.
     * <p>
     * The second number is defined here as the sum {@code secondInDayA + secondInDayB} from 0.0 to
     * {@link Constants#JULIAN_DAY} (excluded). The two parameters are used for increased accuracy.
     * </p>
     * 
     * @param secondInDayA
     *        first part of the second number
     * @param secondInDayB
     *        last part of the second number
     * @exception IllegalArgumentException
     *            if seconds number is out of range
     */
    public TimeComponents(final int secondInDayA, final double secondInDayB) {
        // range check
        if ((secondInDayB < -secondInDayA) || (secondInDayB >= (Constants.JULIAN_DAY - secondInDayA))) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.OUT_OF_RANGE_SECONDS_NUMBER,
                secondInDayA + secondInDayB);
        }

        // extract the time components
        final int hourA = (int) MathLib.floor(secondInDayA / 3600.0);
        final int hsA = 3600 * hourA;
        final int minuteA = (int) MathLib.floor((secondInDayA - hsA) / 60.0);
        final int msA = 60 * minuteA;
        final double secondA = secondInDayA - hsA - msA;

        final int hourB = (int) MathLib.floor(secondInDayB / 3600.0);
        final int hsB = 3600 * hourB;
        final int minuteB = (int) MathLib.floor((secondInDayB - hsB) / 60.0);
        final int msB = 60 * minuteB;
        final double secondB = secondInDayB - hsB - msB;

        final double secondTmp = secondA + secondB;
        int addMin = 0;
        if (secondTmp >= SEC_IN_MIN) {
            final double secondFrac = secondTmp - 60.0;
            this.second = (int) MathLib.floor(secondFrac);
            this.fracSeconds = secondFrac - this.second;
            addMin = 1;
        } else {
            this.second = (int) MathLib.floor(secondTmp);
            this.fracSeconds = secondTmp - this.second;
        }

        int addH = 0;
        final int minuteTmp = addMin + minuteA + minuteB;
        if (minuteTmp >= MIN_IN_HOUR) {
            this.minute = minuteTmp - MIN_IN_HOUR;
            addH = 1;
        } else {
            this.minute = minuteTmp;
        }

        this.hour = hourA + hourB + addH;
    }

    /**
     * Parse a string in ISO-8601 format to build a time.
     * <p>
     * The supported formats are:
     * <ul>
     * <li>basic format local time: hhmmss (with optional decimals in seconds)</li>
     * <li>extended format local time: hh:mm:ss (with optional decimals in seconds)</li>
     * <li>basic format UTC time: hhmmssZ (with optional decimals in seconds)</li>
     * <li>extended format UTC time: hh:mm:ssZ (with optional decimals in seconds)</li>
     * <li>basic format local time with 00h UTC offset: hhmmss+00 (with optional decimals in seconds)</li>
     * <li>extended format local time with 00h UTC offset: hhmmss+00 (with optional decimals in seconds)</li>
     * <li>basic format local time with 00h and 00m UTC offset: hhmmss+00:00 (with optional decimals in seconds)</li>
     * <li>extended format local time with 00h and 00m UTC offset: hhmmss+00:00 (with optional decimals in seconds)</li>
     * </ul>
     * As shown by the list above, only the complete representations defined in section 4.2 of ISO-8601 standard are
     * supported, neither expended representations nor representations with reduced accuracy are supported.
     * </p>
     * <p>
     * As this class does not support time zones (because space flight dynamics uses {@link TimeScale time scales} with
     * offsets from UTC having sub-second accuracy), only UTC is zone is supported (and in fact ignored). It is the
     * responsibility of the {@link AbsoluteDate} class to handle time scales appropriately.
     * </p>
     * 
     * @param string
     *        string to parse
     * @return a parsed time
     * @exception IllegalArgumentException
     *            if string cannot be parsed
     */
    public static TimeComponents parseTime(final String string) {

        // is the date a calendar date ?
        final Matcher timeMatcher = ISO8601_FORMATS.matcher(string);
        if (timeMatcher.matches()) {
            final String secondms = timeMatcher.group(3).replace(",", DOT);
            final int sec;
            final double msec;
            if (secondms.contains(DOT)) {
                // There is a fraction of seconds
                final String[] array = secondms.split("[.]");
                sec = Integer.parseInt(array[0]);
                msec = Double.parseDouble(array[1]) / MathLib.pow(TEN, array[1].length());
            } else {
                sec = Integer.parseInt(secondms);
                msec = 0;
            }
            return new TimeComponents(Integer.parseInt(timeMatcher.group(1)),
                Integer.parseInt(timeMatcher.group(2)),
                sec, msec);
        }

        // Could not parse string
        throw PatriusException.createIllegalArgumentException(PatriusMessages.NON_EXISTENT_TIME, string);

    }

    /**
     * Get the hour number.
     * 
     * @return hour number from 0 to 23
     */
    public int getHour() {
        return this.hour;
    }

    /**
     * Get the minute number.
     * 
     * @return minute minute number from 0 to 59
     */
    public int getMinute() {
        return this.minute;
    }

    /**
     * Get the seconds number (it includes the fractional part of seconds).
     * 
     * @return second second number from 0.0 to 60.0 (excluded)
     */
    public double getSecond() {
        return this.second + this.fracSeconds;
    }

    /**
     * Get the fractional part of seconds.
     * 
     * @return fractional part of seconds
     */
    public double getFractionSeconds() {
        return this.fracSeconds;
    }

    /**
     * Get the second number within the day.
     * 
     * @return second number from 0.0 to Constants.JULIAN_DAY
     */
    public double getSecondsInDay() {
        return this.fracSeconds + this.second + SEC_IN_MIN * this.minute + SEC_IN_HOUR * this.hour;
    }

    /**
     * Get a string representation of the time.
     * 
     * @return string representation of the time
     */
    @Override
    public String toString() {
        return this.toString(DEFAULT_SECONDS_PRECISION, false);
    }

    /**
     * Get a string representation of the time.
     * 
     * @param precision
     *        digit number of the seconds fractional part
     * @param isTimeNearLeapSecond
     *        true if the date is inside or immediately before
     *        a leap second. It is used to set the upper boundary of the current day:
     *        23:59:60.99.. when true, 23:59:59.99.. when false.
     * @return string representation of the time
     * @exception IllegalArgumentException
     *            if inconsistent arguments
     *            are given (negative precision)
     */
    protected String toString(final int precision, final boolean isTimeNearLeapSecond) {
        // precision check
        if (precision < 0) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.NEGATIVE_PRECISION, precision);
        }
        final double limit;
        if (isTimeNearLeapSecond) {
            // compute maximum number of seconds in a minute (60.9...5) with respect to the precision:
            limit = SEC_IN_MIN + 1 - 5.0 * MathLib.pow(TEN, -(precision + 1));
        } else {
            // compute maximum number of seconds in a minute (59.9...5) with respect to the precision:
            limit = SEC_IN_MIN - 5.0 * MathLib.pow(TEN, -(precision + 1));
        }
        // create the pattern for the seconds format with respect to the precision:
        final StringBuffer pattern = new StringBuffer();
        pattern.append("00.");
        for (int i = 0; i < precision; ++i) {
            pattern.append("0");
        }
        final DecimalFormat secondsFormat =
            new DecimalFormat(pattern.toString(), new DecimalFormatSymbols(Locale.US));

        double hrs = this.hour;
        double mnts = this.minute;
        double scnds = this.second + this.fracSeconds;
        if (scnds >= limit) {
            // The number of seconds is beyond the computed limit, switch to the next minute:
            mnts = this.minute + 1;
            scnds = 0.0;
            if (mnts == MIN_IN_HOUR) {
                // 60 minutes, switch to the next hour:
                mnts = 0;
                hrs = hrs + 1;
            }
            if (hrs == HOURS_IN_DAY) {
                // 24 hours, switch to 0:
                hrs = 0;
            }
        }
        String scndsStr = secondsFormat.format(scnds);
        if (precision == 0) {
            // When precision is zero, do not print the point:
            scndsStr = scndsStr.replace(DOT, "");
        }

        return new StringBuffer().
            append(TWO_DIGITS.format(hrs)).append(':').
            append(TWO_DIGITS.format(mnts)).append(':').
            append(scndsStr).toString();
    }

    /** {@inheritDoc} */
    @Override
    public int compareTo(final TimeComponents other) {
        final double seconds = this.getSecondsInDay();
        final double otherSeconds = other.getSecondsInDay();
        int res = 0;
        if (seconds < otherSeconds) {
            res = -1;
        } else if (seconds > otherSeconds) {
            res = 1;
        }
        return res;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object other) {
        try {
            final TimeComponents otherTime = (TimeComponents) other;
            return (otherTime != null) && (this.hour == otherTime.hour) &&
                (this.minute == otherTime.minute) && (this.second == otherTime.second)
                && (this.fracSeconds == otherTime.fracSeconds);
        } catch (final ClassCastException cce) {
            return false;
        }
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final long bits = Double.doubleToLongBits(this.second + this.fracSeconds);
        return ((this.hour << HASH_CODE_GENERATOR_1) ^ (this.minute << EIGHT))
            ^ (int) (bits ^ (bits >>> HASH_CODE_GENERATOR_2));
    }

}
