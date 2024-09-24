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
* VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
* VERSION:4.11:DM:DM-3232:22/05/2023:[PATRIUS] Detection d'extrema dans la classe ExtremaGenericDetector
* VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
* VERSION:4.8:DM:DM-2962:15/11/2021:[PATRIUS] Precision numerique lors du ShiftedBy avec TimeScale 
* VERSION:4.8:FA:FA-2941:15/11/2021:[PATRIUS] Correction anomalies suite a DM 2767 
* VERSION:4.8:DM:DM-2967:15/11/2021:[PATRIUS] corriger les utilisations de java.util.Date 
 * VERSION:4.7:FA:FA-2887:18/05/2021:Probleme de micro-pas dans la propagation
 * VERSION:4.7:DM:DM-2683:18/05/2021:Methode shiftedBy (AbsoluteDate) avec echelles de temps
 * VERSION:4.7:DM:DM-2647:18/05/2021:constructeur de AbsoluteDate avec TAI par defaut
 * VERSION:4.7:DM:DM-2767:18/05/2021:Evolutions et corrections diverses 
 * VERSION:4.6:FA:FA-2608:27/01/2021:Mauvaise date de reference pour le Galileo System Time
 * VERSION:4.5:DM:DM-2431:27/05/2020:Date
 * VERSION:4.5:DM:DM-2340:27/05/2020:Conversion AbsoluteDate - cjd 
 * VERSION:4.4:FA:FA-2121:04/10/2019:[PATRIUS] precision de la methode shiftedBy de AbsoluteDate
 * VERSION:4.3:FA:FA-1978:15/05/2019:Anomalie calcul orientation corps celeste (UAI)
 * VERSION:4.3:DM:DM-2089:15/05/2019:[PATRIUS] passage a Java 8
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:158:24/10/2013:Changed AbsoluteDate.toString to return date in TAI
 * VERSION::DM:256:01/08/2014:Changed AbsoluteDate.toString to take into account seconds precision
 * VERSION::FA:367:21/11/2014:Corrected END-HISTORY key word
 * VERSION::FA:363:03/12/2014:Solved toString round date bug
 * VERSION::FA:397:20/02/2015:Corrected wrong definition of AbsoluteDate.JAVA_EPOCH (1/1/1970 UTC)
 * VERSION::FA:1312:15/11/2017:Improve TimeComponents accuracy
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * VERSION::FA:1868:31/10/2018: handle proper end of integration
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.time;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

//CHECKSTYLE: stop MagicNumber check
// Reason: constants - Orekit code

/**
 * This class represents a specific instant in time.
 * <p>
 * Instances of this class are considered to be absolute in the sense that each one represent the occurrence of some
 * event and can be compared to other instances or located in <em>any</em> {@link TimeScale time scale}. In other words
 * the different locations of an event with respect to two different time scale (say {@link TAIScale TAI} and
 * {@link UTCScale UTC} for example) are simply different perspective related to a single object. Only one
 * <code>AbsoluteDate</code> instance is needed, both representations being available from this single instance by
 * specifying the time scales as parameter when calling the ad-hoc methods.
 * </p>
 *
 * <p>
 * Since an instance is not bound to a specific time-scale, all methods related to the location of the date within some
 * time scale require to provide the time scale as an argument. It is therefore possible to define a date in one time
 * scale and to use it in another one. An example of such use is to read a date from a file in UTC and write it in
 * another file in TAI. This can be done as follows:
 * </p>
 *
 * <pre>
 * DateTimeComponents utcComponents = readNextDate();
 * AbsoluteDate date = new AbsoluteDate(utcComponents, TimeScalesFactory.getUTC());
 * writeNextDate(date.getComponents(TimeScalesFactory.getTAI()));
 * </pre>
 *
 * <p>
 * Two complementary views are available:
 * </p>
 * <ul>
 * <li>
 * <p>
 * location view (mainly for input/output or conversions)
 * </p>
 * <p>
 * locations represent the coordinate of one event with respect to a {@link TimeScale time scale}. The related methods
 * are {@link #AbsoluteDate(DateComponents, TimeComponents, TimeScale)},
 * {@link #AbsoluteDate(int, int, int, int, int, double, TimeScale)}, {@link #AbsoluteDate(int, int, int, TimeScale)},
 * {@link #AbsoluteDate(LocalDateTime, TimeScale)}, {@link #createGPSDate(int, double)},
 * {@link #parseCCSDSCalendarSegmentedTimeCode(byte, byte[])}, toString(){@link #toLocalDateTime(TimeScale)},
 * {@link #toString(TimeScale) toString(timeScale)}, {@link #toString()}, and {@link #timeScalesOffset}.
 * </p>
 * </li>
 * <li>
 * <p>
 * offset view (mainly for physical computation)
 * </p>
 * <p>
 * offsets represent either the flow of time between two events (two instances of the class) or durations. They are
 * counted in seconds, are continuous and could be measured using only a virtually perfect stopwatch. The related
 * methods are {@link #AbsoluteDate(AbsoluteDate, double)},
 * {@link #parseCCSDSUnsegmentedTimeCode(byte, byte, byte[], AbsoluteDate)},
 * {@link #parseCCSDSDaySegmentedTimeCode(byte, byte[], DateComponents)}, {@link #durationFrom(AbsoluteDate)},
 * {@link #compareTo(AbsoluteDate)}, {@link #equals(Object)} and {@link #hashCode()}.
 * </p>
 * </li>
 * </ul>
 * <p>
 * A few reference epochs which are commonly used in space systems have been defined. These epochs can be used as the
 * basis for offset computation. The supported epochs are: {@link #JULIAN_EPOCH}, {@link #MODIFIED_JULIAN_EPOCH},
 * {@link #FIFTIES_EPOCH_TT}, {@link #CCSDS_EPOCH}, {@link #GALILEO_EPOCH}, {@link #GPS_EPOCH}, {@link #J2000_EPOCH},
 * {@link #JAVA_EPOCH}. In addition to these reference epochs, two other constants are defined for convenience:
 * {@link #PAST_INFINITY} and {@link #FUTURE_INFINITY}, which can be used either as dummy dates when a date is not yet
 * initialized, or for initialization of loops searching for a min or max date.
 * </p>
 * <p>
 * Instances of the <code>AbsoluteDate</code> class are guaranteed to be immutable.
 * </p>
 *
 * @author Luc Maisonobe
 * @see TimeScale
 * @see TimeStamped
 * @see ChronologicalComparator
 */
public class AbsoluteDate
    implements TimeStamped, TimeShiftable<AbsoluteDate>, Comparable<AbsoluteDate>, Serializable {

    /**
     * Reference epoch for julian dates: -4712-01-01T12:00:00 Terrestrial Time.
     * <p>
     * Both <code>java.util.Date</code> and {@link DateComponents} classes follow the astronomical conventions and
     * consider a year 0 between years -1 and +1, hence this reference date lies in year -4712 and not in year -4713 as
     * can be seen in other documents or programs that obey a different convention (for example the <code>convcal</code>
     * utility).
     * </p>
     */
    public static final AbsoluteDate JULIAN_EPOCH =
        new AbsoluteDate(DateComponents.JULIAN_EPOCH, TimeComponents.H12, TimeScalesFactory.getTT());

    /** Reference epoch for modified julian dates: 1858-11-17T00:00:00 Terrestrial Time. */
    public static final AbsoluteDate MODIFIED_JULIAN_EPOCH =
        new AbsoluteDate(DateComponents.MODIFIED_JULIAN_EPOCH, TimeComponents.H00, TimeScalesFactory.getTT());

    /** Reference epoch for 1950 dates: 1950-01-01T00:00:00 Terrestrial Time. */
    public static final AbsoluteDate FIFTIES_EPOCH_TT =
        new AbsoluteDate(DateComponents.FIFTIES_EPOCH, TimeComponents.H00, TimeScalesFactory.getTT());

    /** Reference epoch for 1950 dates: 1950-01-01T00:00:00 TAI. */
    public static final AbsoluteDate FIFTIES_EPOCH_TAI =
        new AbsoluteDate(DateComponents.FIFTIES_EPOCH, TimeComponents.H00,
            TimeScalesFactory.getTAI());

    /** Reference epoch for 1950 dates: 1950-01-01T00:00:00 UTC. */
    public static final AbsoluteDate FIFTIES_EPOCH_UTC = FIFTIES_EPOCH_TAI;

    /**
     * Reference epoch for CCSDS Time Code Format (CCSDS 301.0-B-4):
     * 1958-01-01T00:00:00 International Atomic Time (<em>not</em> UTC).
     */
    public static final AbsoluteDate CCSDS_EPOCH =
        new AbsoluteDate(DateComponents.CCSDS_EPOCH, TimeComponents.H00, TimeScalesFactory.getTAI());

    /**
     * Reference epoch for Galileo System Time: 1999-08-21T23:59:47 UTC.
     * Java Reference epoch: 1999-08-21T23:59:47 Universal Time Coordinate is equivalent to
     * Java Reference epoch: 1999-08-22T00:00:19 TAI.
     */
    public static final AbsoluteDate GALILEO_EPOCH =
        new AbsoluteDate(DateComponents.GALILEO_EPOCH, new TimeComponents(0, 0, 19.), TimeScalesFactory.getTAI());

    /** Reference epoch for GPS weeks: 1980-01-06T00:00:00 GPS time. */
    public static final AbsoluteDate GPS_EPOCH =
        new AbsoluteDate(DateComponents.GPS_EPOCH, TimeComponents.H00, TimeScalesFactory.getGPS());

    /**
     * Reference epoch for BeiDou System Time: 2006-01-01 00:00:00 UTC.
     * Java Reference epoch: 2006-01-01 00:00:00 Universal Time Coordinate is equivalent to
     * Java Reference epoch: 2006-01-01 00:00:33 TAI.
     */
    public static final AbsoluteDate BEIDOU_EPOCH = new AbsoluteDate(DateComponents.BEIDOU_EPOCH, new TimeComponents(0,
            0, 33.), TimeScalesFactory.getTAI());

    /** J2000.0 Reference epoch: 2000-01-01T12:00:00 Terrestrial Time (<em>not</em> UTC). */
    public static final AbsoluteDate J2000_EPOCH =
        new AbsoluteDate(DateComponents.J2000_EPOCH, TimeComponents.H12, TimeScalesFactory.getTT());

    /**
     * Java Reference epoch: 1970-01-01T00:00:00 Universal Time Coordinate is equivalent to
     * Java Reference epoch: 1970-01-01T00:00:08 TAI.
     */
    public static final AbsoluteDate JAVA_EPOCH =
        new AbsoluteDate(DateComponents.JAVA_EPOCH, new TimeComponents(0, 0, 8.0), TimeScalesFactory.getTAI());

    /** Dummy date at infinity in the past direction. */
    public static final AbsoluteDate PAST_INFINITY = JAVA_EPOCH.shiftedBy(Double.NEGATIVE_INFINITY);

    /** Dummy date at infinity in the future direction. */
    public static final AbsoluteDate FUTURE_INFINITY = JAVA_EPOCH.shiftedBy(Double.POSITIVE_INFINITY);

    /** Serializable UID. */
    private static final long serialVersionUID = 617061803741806846L;

    /**
     * Reference epoch in seconds from 2000-01-01T12:00:00 TAI.
     * <p>
     * Beware, it is not {@link #J2000_EPOCH} since it is in TAI and not in TT.
     * </p>
     */
    private final long epoch;

    /** Offset from the reference epoch in seconds. */
    private final double offset;

    /**
     * Create an instance with a default value ({@link #J2000_EPOCH}).
     */
    public AbsoluteDate() {
        this.epoch = J2000_EPOCH.epoch;
        this.offset = J2000_EPOCH.offset;
    }

    /**
     * Create an instance with epoch and offset <i>directly</i>.<br>
     * <b>WARNING :</b> this constructor is meant for advanced users only,
     * since epoch and offset have special constraints and values
     * that are not checked or enforced here. Use at own risk!
     *
     * @param epochIn
     *        epoch value
     * @param offsetIn
     *        offset value
     */
    public AbsoluteDate(final long epochIn, final double offsetIn) {
        this.epoch = epochIn;
        this.offset = offsetIn;
    }

    /**
     * Build an instance from a location (parsed from a string) in a {@link TimeScale time scale}.
     * <p>
     * The supported formats for location are mainly the ones defined in ISO-8601 standard, the exact subset is
     * explained in {@link DateTimeComponents#parseDateTime(String)}, {@link DateComponents#parseDate(String)} and
     * {@link TimeComponents#parseTime(String)}.
     * </p>
     * <p>
     * As CCSDS ASCII calendar segmented time code is a trimmed down version of ISO-8601, it is also supported by this
     * constructor.
     * </p>
     *
     * @param location
     *        location in the time scale, must be in a supported format
     * @param timeScale
     *        time scale
     * @exception IllegalArgumentException
     *            if location string is not in a supported format
     */
    public AbsoluteDate(final String location, final TimeScale timeScale) {
        this(DateTimeComponents.parseDateTime(location), timeScale);
    }

    /**
     * Build an instance from a location (parsed from a string) in {@link TAIScale} scale.
     * <p>
     * The supported formats for location are mainly the ones defined in ISO-8601 standard, the exact subset is
     * explained in {@link DateTimeComponents#parseDateTime(String)}, {@link DateComponents#parseDate(String)} and
     * {@link TimeComponents#parseTime(String)}.
     * </p>
     * <p>
     * As CCSDS ASCII calendar segmented time code is a trimmed down version of ISO-8601, it is also supported by this
     * constructor.
     * </p>
     *
     * @param location
     *        location in the time scale, must be in a supported format
     * @exception IllegalArgumentException
     *            if location string is not in a supported format
     */
    public AbsoluteDate(final String location) {
        this(DateTimeComponents.parseDateTime(location), TimeScalesFactory.getTAI());
    }

    /**
     * Build an instance from a location in a {@link TimeScale time scale}.
     *
     * @param location
     *        location in the time scale
     * @param timeScale
     *        time scale
     */
    public AbsoluteDate(final DateTimeComponents location, final TimeScale timeScale) {
        this(location.getDate(), location.getTime(), timeScale);
    }

    /**
     * Build an instance from a location in a {@link TimeScale time scale}.
     *
     * @param date
     *        date location in the time scale
     * @param time
     *        time location in the time scale
     * @param timeScale
     *        time scale
     */
    public AbsoluteDate(final DateComponents date, final TimeComponents time,
        final TimeScale timeScale) {

        final double seconds = time.getSecond();
        final double tsOffset = timeScale.offsetToTAI(date, time);

        final long dl;
        if (tsOffset == 0) {
            // Particular case: fraction of second can be retrieved exactly
            dl = (long) MathLib.floor(seconds);

            this.offset = time.getFractionSeconds();
        } else {
            // compute sum exactly, using Møller-Knuth TwoSum algorithm without branching
            // the following statements must NOT be simplified, they rely on floating point
            // arithmetic properties (rounding and representable numbers)
            // at the end, the EXACT result of addition seconds + tsOffset
            // is sum + residual, where sum is the closest representable number to the exact
            // result and residual is the missing part that does not fit in the first number
            final double sum = seconds + tsOffset;
            final double sPrime = sum - tsOffset;
            final double tPrime = sum - sPrime;
            final double deltaS = seconds - sPrime;
            final double deltaT = tsOffset - tPrime;
            final double residual = deltaS + deltaT;
            dl = (long) MathLib.floor(sum);

            this.offset = (sum - dl) + residual;
        }
        this.epoch = 60L * ((date.getJ2000Day() * 24L + time.getHour()) * 60L + time.getMinute() - 720L) + dl;
    }

    /**
     * Build an instance from a location in a {@link TimeScale time scale}.
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
     * @param timeScale
     *        time scale
     * @exception IllegalArgumentException
     *            if inconsistent arguments
     *            are given (parameters out of range)
     */
    public AbsoluteDate(final int year, final int month, final int day,
        final int hour, final int minute, final double second,
        final TimeScale timeScale) {
        this(new DateComponents(year, month, day), new TimeComponents(hour, minute, second), timeScale);
    }

    /**
     * Build an instance from a location in TAI scale.
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
     *            are given (parameters out of range)
     */
    public AbsoluteDate(final int year, final int month, final int day,
        final int hour, final int minute, final double second) {
        this(year, month, day, hour, minute, second, TimeScalesFactory.getTAI());
    }

    /**
     * Build an instance from a location in a {@link TimeScale time scale}.
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
     * @param timeScale
     *        time scale
     * @exception IllegalArgumentException
     *            if inconsistent arguments
     *            are given (parameters out of range)
     */
    public AbsoluteDate(final int year, final Month month, final int day,
        final int hour, final int minute, final double second,
        final TimeScale timeScale) {
        this(new DateComponents(year, month, day), new TimeComponents(hour, minute, second), timeScale);
    }

    /**
     * Build an instance from a location in a {@link TimeScale time scale}.
     * <p>
     * The hour is set to 00:00:00.000.
     * </p>
     *
     * @param date
     *        date location in the time scale
     * @param timeScale
     *        time scale
     * @exception IllegalArgumentException
     *            if inconsistent arguments
     *            are given (parameters out of range)
     */
    public AbsoluteDate(final DateComponents date, final TimeScale timeScale) {
        this(date, TimeComponents.H00, timeScale);
    }

    /**
     * Build an instance from a location in a {@link TimeScale time scale}.
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
     * @param timeScale
     *        time scale
     * @exception IllegalArgumentException
     *            if inconsistent arguments
     *            are given (parameters out of range)
     */
    public AbsoluteDate(final int year, final int month, final int day,
        final TimeScale timeScale) {
        this(new DateComponents(year, month, day), TimeComponents.H00, timeScale);
    }

    /**
     * Build an instance from a location in a {@link TimeScale time scale}.
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
     * @param timeScale
     *        time scale
     * @exception IllegalArgumentException
     *            if inconsistent arguments
     *            are given (parameters out of range)
     */
    public AbsoluteDate(final int year, final Month month, final int day,
        final TimeScale timeScale) {
        this(new DateComponents(year, month, day), TimeComponents.H00, timeScale);
    }

    /**
     * Build an instance from a location in a {@link TimeScale time scale}.
     *
     * @param location
     *        location in the time scale
     * @param timeScale
     *        time scale
     * @deprecated As of PATRIUS 4.8, method using {@link Date} are deprecated. Use
     * {@link #AbsoluteDate(LocalDateTime, TimeScale)} instead.
     */
    @Deprecated
    public AbsoluteDate(final Date location, final TimeScale timeScale) {
        this(new DateComponents(DateComponents.JAVA_EPOCH,
            (int) (location.getTime() / 86400000L)),
            new TimeComponents(0.001 * (location.getTime() % 86400000L)),
            timeScale);
    }

    /**
     * Build an instance from a location in a {@link TimeScale time scale}.
     *
     * @param location
     *        location in the time scale
     * @param timeScale
     *        time scale
     */
    public AbsoluteDate(final LocalDateTime location, final TimeScale timeScale) {
        this(new DateComponents(DateComponents.JAVA_EPOCH,
            (int) (location.toInstant(ZoneOffset.UTC).toEpochMilli() / 86400000L)),
            new TimeComponents(0.001 * (location.toInstant(ZoneOffset.UTC).toEpochMilli() % 86400000L)),
            timeScale);
    }

    /**
     * Build an instance from an elapsed duration since to another instant.
     * <p>
     * It is important to note that the elapsed duration is <em>not</em> the difference between two readings on a time
     * scale. As an example, the duration between the two instants leading to the readings 2005-12-31T23:59:59 and
     * 2006-01-01T00:00:00 in the {@link UTCScale UTC} time scale is <em>not</em> 1 second, but a stop watch would have
     * measured an elapsed duration of 2 seconds between these two instances because a leap second was introduced at the
     * end of 2005 in this time scale.
     * </p>
     * <p>
     * This constructor is the reverse of the {@link #durationFrom(AbsoluteDate)} method.
     * </p>
     *
     * @param since
     *        start instant of the measured duration
     * @param elapsedDuration
     *        physically elapsed duration from the <code>since</code> instant, as measured in a regular time scale
     * @see #durationFrom(AbsoluteDate)
     */
    public AbsoluteDate(final AbsoluteDate since, final double elapsedDuration) {
        if (elapsedDuration == 0) {
            // No delta-t
            this.offset = since.offset;
            this.epoch = since.epoch;
        } else {
            // Regular case
            final double sum = since.offset + elapsedDuration;
            if (Double.isInfinite(sum)) {
                this.offset = sum;
                this.epoch = (sum < 0) ? Long.MIN_VALUE : Long.MAX_VALUE;
            } else {
                // compute sum exactly, using Møller-Knuth TwoSum algorithm without branching
                // the following statements must NOT be simplified, they rely on floating point
                // arithmetic properties (rounding and representable numbers)
                // at the end, the EXACT result of addition since.offset + elapsedDuration
                // is sum + residual, where sum is the closest representable number to the exact
                // result and residual is the missing part that does not fit in the first number
                final double oPrime = sum - elapsedDuration;
                final double dPrime = sum - oPrime;
                final double deltaO = since.offset - oPrime;
                final double deltaD = elapsedDuration - dPrime;
                final double residual = deltaO + deltaD;
                final long dl = (long) MathLib.floor(sum);
                this.offset = (sum - dl) + residual;
                this.epoch = since.epoch + dl;
            }
        }
    }

    /**
     * Build an instance from an apparent clock offset with respect to another
     * instant <em>in the perspective of a specific {@link TimeScale time scale}</em>.
     * <p>
     * It is important to note that the apparent clock offset <em>is</em> the difference between two readings on a time
     * scale and <em>not</em> an elapsed duration. As an example, the apparent clock offset between the two instants
     * leading to the readings 2005-12-31T23:59:59 and 2006-01-01T00:00:00 in the {@link UTCScale UTC} time scale is 1
     * second, but the elapsed duration is 2 seconds because a leap second has been introduced at the end of 2005 in
     * this time scale.
     * </p>
     * <p>
     * This constructor is the reverse of the {@link #offsetFrom(AbsoluteDate, TimeScale)} method.
     * </p>
     *
     * @param reference
     *        reference instant
     * @param apparentOffset
     *        apparent clock offset from the reference instant
     *        (difference between two readings in the specified time scale)
     * @param timeScale
     *        time scale with respect to which the offset is defined
     * @see #offsetFrom(AbsoluteDate, TimeScale)
     */
    public AbsoluteDate(final AbsoluteDate reference, final double apparentOffset,
        final TimeScale timeScale) {
        final AbsoluteDate date = preciseShiftedBy(reference, apparentOffset, timeScale);
        this.epoch = date.epoch;
        this.offset = date.offset;
    }

    /**
     * Build an instance from a cjd in a {@link TimeScale time scale}.
     * @param cjd
     *        CNES julian date (number of days since epoch 1950)
     * @param timeScale
     *        time scale
     */
    public AbsoluteDate(final double cjd, final TimeScale timeScale) {
        this(new DateComponents(DateComponents.FIFTIES_EPOCH, (int) cjd), new TimeComponents((cjd - (int) cjd)
                * Constants.JULIAN_DAY), timeScale);
    }

    /**
     * Precise shiftedBy method.
     * @param since initial date
     * @param elapsedDuration elapsed duration
     * @param timeScale time scale
     * @return date shifted by duration
     */
    private static AbsoluteDate preciseShiftedBy(final AbsoluteDate since,
            final double elapsedDuration,
            final TimeScale timeScale) {
        final AbsoluteDate date1 = new AbsoluteDate(since, elapsedDuration);
        final double offset0 = timeScale.offsetFromTAI(since);
        final double offset1 = timeScale.offsetFromTAI(date1);
        return date1.shiftedBy(offset0 - offset1);
    }

    /**
     * Build an instance from a CCSDS Unsegmented Time Code (CUC).
     * <p>
     * CCSDS Unsegmented Time Code is defined in the blue book: CCSDS Time Code Format (CCSDS 301.0-B-4) published in
     * November 2010
     * </p>
     * <p>
     * If the date to be parsed is formatted using version 3 of the standard (CCSDS 301.0-B-3 published in 2002) or if
     * the extension of the preamble field introduced in version 4 of the standard is not used, then the
     * {@code preambleField2} parameter can be set to 0.
     * </p>
     *
     * @param preambleField1
     *        first byte of the field specifying the format, often
     *        not transmitted in data interfaces, as it is constant for a given data interface
     * @param preambleField2
     *        second byte of the field specifying the format
     *        (added in revision 4 of the CCSDS standard in 2010), often not transmitted in data
     *        interfaces, as it is constant for a given data interface (value ignored if presence
     *        not signaled in {@code preambleField1})
     * @param timeField
     *        byte array containing the time code
     * @param agencyDefinedEpoch
     *        reference epoch, ignored if the preamble field
     *        specifies the {@link #CCSDS_EPOCH CCSDS reference epoch} is used (and hence
     *        may be null in this case)
     * @return an instance corresponding to the specified date
     * @throws PatriusException
     *         if preamble is inconsistent with Unsegmented Time Code,
     *         or if it is inconsistent with time field, or if agency epoch is needed but not provided
     */
    public static AbsoluteDate
            parseCCSDSUnsegmentedTimeCode(final byte preambleField1,
                                          final byte preambleField2,
                                          final byte[] timeField,
                                          final AbsoluteDate agencyDefinedEpoch) throws PatriusException {

        // time code identification and reference epoch
        final AbsoluteDate epoch;
        switch (preambleField1 & 0x70) {
            case 0x10:
                // the reference epoch is CCSDS epoch 1958-01-01T00:00:00 TAI
                epoch = CCSDS_EPOCH;
                break;
            case 0x20:
                // the reference epoch is agency defined
                if (agencyDefinedEpoch == null) {
                    // Exception : epoch not agency defined
                    throw new PatriusException(PatriusMessages.CCSDS_DATE_MISSING_AGENCY_EPOCH);
                }
                epoch = agencyDefinedEpoch;
                break;
            default:
                // Exception : missing preamble field
                throw new PatriusException(PatriusMessages.CCSDS_DATE_INVALID_PREAMBLE_FIELD,
                    formatByte(preambleField1));
        }

        // time field lengths
        int coarseTimeLength = 1 + ((preambleField1 & 0x0C) >>> 2);
        int fineTimeLength = preambleField1 & 0x03;

        if ((preambleField1 & 0x80) != 0x0) {
            // there is an additional octet in preamble field
            coarseTimeLength += (preambleField2 & 0x60) >>> 5;
            fineTimeLength += (preambleField2 & 0x1C) >>> 2;
        }

        if (timeField.length != coarseTimeLength + fineTimeLength) {
            throw new PatriusException(PatriusMessages.CCSDS_DATE_INVALID_LENGTH_TIME_FIELD,
                timeField.length, coarseTimeLength + fineTimeLength);
        }

        double seconds = 0;
        for (int i = 0; i < coarseTimeLength; ++i) {
            seconds = seconds * 256 + toUnsigned(timeField[i]);
        }
        double subseconds = 0;
        for (int i = timeField.length - 1; i >= coarseTimeLength; --i) {
            subseconds = (subseconds + toUnsigned(timeField[i])) / 256;
        }

        return new AbsoluteDate(epoch, seconds).shiftedBy(subseconds);

    }

    /**
     * Build an instance from a CCSDS Day Segmented Time Code (CDS).
     * <p>
     * CCSDS Day Segmented Time Code is defined in the blue book: CCSDS Time Code Format (CCSDS 301.0-B-4) published in
     * November 2010
     * </p>
     *
     * @param preambleField
     *        field specifying the format, often not transmitted in
     *        data interfaces, as it is constant for a given data interface
     * @param timeField
     *        byte array containing the time code
     * @param agencyDefinedEpoch
     *        reference epoch, ignored if the preamble field
     *        specifies the {@link #CCSDS_EPOCH CCSDS reference epoch} is used (and hence
     *        may be null in this case)
     * @return an instance corresponding to the specified date
     * @throws PatriusException
     *         if preamble is inconsistent with Day Segmented Time Code,
     *         or if it is inconsistent with time field, or if agency epoch is needed but not provided,
     *         or it UTC time scale cannot be retrieved
     */
    public static AbsoluteDate
            parseCCSDSDaySegmentedTimeCode(final byte preambleField, final byte[] timeField,
                                           final DateComponents agencyDefinedEpoch) throws PatriusException {

        // time code identification
        if ((preambleField & 0xF0) != 0x40) {
            throw new PatriusException(PatriusMessages.CCSDS_DATE_INVALID_PREAMBLE_FIELD,
                formatByte(preambleField));
        }

        // reference epoch
        final DateComponents epoch;
        if ((preambleField & 0x08) == 0x00) {
            // the reference epoch is CCSDS epoch 1958-01-01T00:00:00 TAI
            epoch = DateComponents.CCSDS_EPOCH;
        } else {
            // the reference epoch is agency defined
            if (agencyDefinedEpoch == null) {
                throw new PatriusException(PatriusMessages.CCSDS_DATE_MISSING_AGENCY_EPOCH);
            }
            epoch = agencyDefinedEpoch;
        }

        // time field lengths
        final int subMillisecondLength = (preambleField & 0x03) << 1;
        if (subMillisecondLength == 6) {
            throw new PatriusException(PatriusMessages.CCSDS_DATE_INVALID_PREAMBLE_FIELD,
                formatByte(preambleField));
        }
        final int daySegmentLength = ((preambleField & 0x04) == 0x0) ? 2 : 3;
        if (timeField.length != daySegmentLength + 4 + subMillisecondLength) {
            // Exception
            throw new PatriusException(PatriusMessages.CCSDS_DATE_INVALID_LENGTH_TIME_FIELD,
                timeField.length, daySegmentLength + 4 + subMillisecondLength);
        }

        int i = 0;
        int day = 0;
        while (i < daySegmentLength) {
            day = day * 256 + toUnsigned(timeField[i++]);
        }

        long milliInDay = 0L;
        while (i < daySegmentLength + 4) {
            milliInDay = milliInDay * 256 + toUnsigned(timeField[i++]);
        }
        final int milli = (int) (milliInDay % 1000L);
        final int seconds = (int) ((milliInDay - milli) / 1000L);

        double subMilli = 0;
        double divisor = 1;
        while (i < timeField.length) {
            subMilli = subMilli * 256 + toUnsigned(timeField[i++]);
            divisor *= 1000;
        }

        // Build result
        final DateComponents date = new DateComponents(epoch, day);
        final TimeComponents time = new TimeComponents(seconds);
        return new AbsoluteDate(date, time, TimeScalesFactory.getUTC()).shiftedBy(milli * 1.0e-3 + subMilli / divisor);

    }

    /**
     * Build an instance from a CCSDS Calendar Segmented Time Code (CCS).
     * <p>
     * CCSDS Calendar Segmented Time Code is defined in the blue book: CCSDS Time Code Format (CCSDS 301.0-B-4)
     * published in November 2010
     * </p>
     *
     * @param preambleField
     *        field specifying the format, often not transmitted in
     *        data interfaces, as it is constant for a given data interface
     * @param timeField
     *        byte array containing the time code
     * @return an instance corresponding to the specified date
     * @throws PatriusException
     *         if preamble is inconsistent with Calendar Segmented Time Code,
     *         or if it is inconsistent with time field, or it UTC time scale cannot be retrieved
     */
    public static AbsoluteDate parseCCSDSCalendarSegmentedTimeCode(final byte preambleField,
                                                                   final byte[] timeField) throws PatriusException {

        // time code identification
        if ((preambleField & 0xF0) != 0x50) {
            throw new PatriusException(PatriusMessages.CCSDS_DATE_INVALID_PREAMBLE_FIELD,
                formatByte(preambleField));
        }

        // time field length
        final int length = 7 + (preambleField & 0x07);
        if (length == 14) {
            throw new PatriusException(PatriusMessages.CCSDS_DATE_INVALID_PREAMBLE_FIELD,
                formatByte(preambleField));
        }
        if (timeField.length != length) {
            throw new PatriusException(PatriusMessages.CCSDS_DATE_INVALID_LENGTH_TIME_FIELD,
                timeField.length, length);
        }

        // date part in the first four bytes
        final DateComponents date;
        if ((preambleField & 0x08) == 0x00) {
            // month of year and day of month variation
            date = new DateComponents(toUnsigned(timeField[0]) * 256 + toUnsigned(timeField[1]),
                toUnsigned(timeField[2]),
                toUnsigned(timeField[3]));
        } else {
            // day of year variation
            date = new DateComponents(toUnsigned(timeField[0]) * 256 + toUnsigned(timeField[1]),
                toUnsigned(timeField[2]) * 256 + toUnsigned(timeField[3]));
        }

        // time part from bytes 5 to last (between 7 and 13 depending on precision)
        final TimeComponents time = new TimeComponents(toUnsigned(timeField[4]),
            toUnsigned(timeField[5]),
            toUnsigned(timeField[6]));
        double subSecond = 0;
        double divisor = 1;
        for (int i = 7; i < length; ++i) {
            subSecond = subSecond * 100 + toUnsigned(timeField[i]);
            divisor *= 100;
        }

        return new AbsoluteDate(date, time, TimeScalesFactory.getUTC()).shiftedBy(subSecond / divisor);

    }

    /**
     * Decode a signed byte as an unsigned int value.
     *
     * @param b
     *        byte to decode
     * @return an unsigned int value
     */
    private static int toUnsigned(final byte b) {
        final int i = b;
        return (i < 0) ? 256 + i : i;
    }

    /**
     * Format a byte as an hex string for error messages.
     *
     * @param data
     *        byte to format
     * @return a formatted string
     */
    private static String formatByte(final byte data) {
        return "0x" + Integer.toHexString(data).toUpperCase();
    }

    /**
     * Build an instance corresponding to a GPS date.
     * <p>
     * GPS dates are provided as a week number starting at {@link #GPS_EPOCH GPS epoch} and as a number of milliseconds
     * since week start.
     * </p>
     *
     * @param weekNumber
     *        week number since {@link #GPS_EPOCH GPS epoch}
     * @param milliInWeek
     *        number of milliseconds since week start
     * @return a new instant
     */
    public static AbsoluteDate createGPSDate(final int weekNumber,
                                             final double milliInWeek) {
        final int day = (int) MathLib.floor(milliInWeek / (1000.0 * Constants.JULIAN_DAY));
        final double secondsInDay = milliInWeek / 1000.0 - day * Constants.JULIAN_DAY;
        return new AbsoluteDate(new DateComponents(DateComponents.GPS_EPOCH, weekNumber * 7 + day),
            new TimeComponents(secondsInDay),
            TimeScalesFactory.getGPS());
    }

    /**
     * Get a time-shifted date.
     * <p>
     * Calling this method is equivalent to call <code>new AbsoluteDate(this, dt)</code>.
     * </p>
     *
     * @param dt
     *        time shift in seconds
     * @return a new date, shifted with respect to instance (which is immutable)
     * @see fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates#shiftedBy(double)
     * @see fr.cnes.sirius.patrius.attitudes.Attitude#shiftedBy(double)
     * @see fr.cnes.sirius.patrius.orbits.Orbit#shiftedBy(double)
     * @see fr.cnes.sirius.patrius.propagation.SpacecraftState#shiftedBy(double)
     */
    @Override
    public AbsoluteDate shiftedBy(final double dt) {
        return new AbsoluteDate(this, dt);
    }

    /**
     * Get a time-shifted date in provided time scale.
     * <p>
     * Calling this method is equivalent to call <code>new AbsoluteDate(this, dt, timeScale)</code>.
     * </p>
     *
     * @param dt
     *        time shift in seconds
     * @param timeScale time scale in which the shift is performed
     * @return a new date, shifted with respect to instance (which is immutable)
     */
    public AbsoluteDate shiftedBy(final double dt, final TimeScale timeScale) {
        return new AbsoluteDate(this, dt, timeScale);
    }

    /**
     * Shift a date taking into account a limit date which shall not be overpassed by this + dt.
     * "forward" boolean indicating in which direction the limit shall not be overpassed.
     * @param dt time shift in seconds
     * @param limit limit not to be overpassed by result taking into account direction (through "forward" boolean)
     * @param forward true if limit
     * @return shifted date, limit date if shifted date > limit and forward = true or shifted date < limit
     * and forward = false
     */
    public AbsoluteDate shiftedBy(final double dt, final AbsoluteDate limit, final boolean forward) {
        AbsoluteDate date = new AbsoluteDate(this, dt);
        final int dateCompare = date.compareTo(limit);
        if ((dateCompare > 0 && forward) || (dateCompare < 0 && !forward)) {
            date = limit;
        }
        return date;
    }

    /**
     * Compute the physically elapsed duration between two instants.
     * <p>
     * The returned duration is the number of seconds physically elapsed between the two instants, measured in a regular
     * time scale with respect to surface of the Earth (i.e either the {@link TAIScale TAI scale}, the {@link TTScale TT
     * scale} or the {@link GPSScale GPS scale}). It is the only method that gives a duration with a physical meaning.
     * </p>
     * <p>
     * This method gives the same result (with less computation) as calling {@link #offsetFrom(AbsoluteDate, TimeScale)}
     * with a second argument set to one of the regular scales cited above.
     * </p>
     * <p>
     * This method is the reverse of the {@link #AbsoluteDate(AbsoluteDate, double)} constructor.
     * </p>
     *
     * @param instant
     *        instant to subtract from the instance
     * @return offset in seconds between the two instants (positive
     *         if the instance is posterior to the argument)
     * @see #offsetFrom(AbsoluteDate, TimeScale)
     * @see #AbsoluteDate(AbsoluteDate, double)
     */
    public double durationFrom(final AbsoluteDate instant) {
        return (this.epoch - instant.epoch) + (this.offset - instant.offset);
    }

    /**
     * Compute elapsed duration between two instants in provided time scale.
     * This duration is different from duration returned by {@link #durationFrom(AbsoluteDate)} method since it accounts
     * for time dilation.
     * Time dilation only occurs in some specific time scales such as TDB.
     *
     * @param instant instant
     * @param timeScale time scale
     * @return duration from instant accounting for time dilatation
     * @see #durationFrom(AbsoluteDate)
     */
    public double durationFrom(final AbsoluteDate instant, final TimeScale timeScale) {
        // Physical duration in TAI scale
        final double deltaTAI = this.durationFrom(instant);

        // Add offset due to time dilatation
        final double offset1 = instant.timeScalesOffset(timeScale, TimeScalesFactory.getTAI());
        final double offset2 = this.timeScalesOffset(timeScale, TimeScalesFactory.getTAI());
        final double offsetTotal = offset2 - offset1;

        return deltaTAI + offsetTotal;
    }

    /**
     * Compute the physically elapsed duration between two instants.
     * <p>
     * This method is more accurate than method {@link #durationFrom(AbsoluteDate)} since it used Moller-Kuth algorithm
     * which returns the closest (numerically speaking) duration to exact duration between two dates (max 1ULP).
     * </p>
     *
     * @param instant
     *        instant to subtract from the instance
     * @return offset in seconds between the two instants (positive
     *         if the instance is posterior to the argument)
     * @see #durationFrom(AbsoluteDate)
     */
    public double preciseDurationFrom(final AbsoluteDate instant) {
        if (!Double.isInfinite(this.offset) && Double.isInfinite(instant.offset)) {
            return this.durationFrom(instant);
        }

        // TwoSum Moller-Kuth algorithm
        // Initialize offsets
        final double dOffset = this.offset - instant.offset;
        final double dOffsetAprime = dOffset + instant.offset;
        final double dOffsetBprime = dOffset - dOffsetAprime;
        final double dOffsetDeltaA = this.offset - dOffsetAprime;
        final double dOffsetDeltaB = -instant.offset - dOffsetBprime;
        final double dOffsetResidual = dOffsetDeltaA + dOffsetDeltaB;

        // TwoSum Moller-Kuth algorithm
        final double dEpoch = (this.epoch - instant.epoch);
        final double sum = dEpoch + dOffset;
        final double aPrime = sum - dOffset;
        final double bPrime = sum - aPrime;
        final double deltaA = dEpoch - aPrime;
        final double deltaB = dOffset - bPrime;
        final double residual = deltaA + deltaB;

        // offset in seconds between the two instants
        return sum + (residual + dOffsetResidual);
    }

    /**
     * Compute the apparent clock offset between two instant <em>in the
     * perspective of a specific {@link TimeScale time scale}</em>.
     * <p>
     * The offset is the number of seconds counted in the given time scale between the locations of the two instants,
     * with all time scale irregularities removed (i.e. considering all days are exactly 86400 seconds long). This
     * method will give a result that may not have a physical meaning if the time scale is irregular. For example since
     * a leap second was introduced at the end of 2005, the apparent offset between 2005-12-31T23:59:59 and
     * 2006-01-01T00:00:00 is 1 second, but the physical duration of the corresponding time interval as returned by the
     * {@link #durationFrom(AbsoluteDate)} method is 2 seconds.
     * </p>
     * <p>
     * This method is the reverse of the {@link #AbsoluteDate(AbsoluteDate, double, TimeScale)} constructor.
     * </p>
     *
     * @param instant
     *        instant to subtract from the instance
     * @param timeScale
     *        time scale with respect to which the offset should
     *        be computed
     * @return apparent clock offset in seconds between the two instants
     *         (positive if the instance is posterior to the argument)
     * @see #durationFrom(AbsoluteDate)
     * @see #AbsoluteDate(AbsoluteDate, double, TimeScale)
     */
    public double offsetFrom(final AbsoluteDate instant, final TimeScale timeScale) {
        final long elapsedDurationA = this.epoch - instant.epoch;
        final double elapsedDurationB = (this.offset + timeScale.offsetFromTAI(this)) -
            (instant.offset + timeScale.offsetFromTAI(instant));
        return elapsedDurationA + elapsedDurationB;
    }

    /**
     * Compute the offset between two time scales at the current instant.
     * <p>
     * The offset is defined as <i>l<sub>1</sub>-l<sub>2</sub></i> where <i>l<sub>1</sub></i> is the location of the
     * instant in the <code>scale1</code> time scale and <i>l<sub>2</sub></i> is the location of the instant in the
     * <code>scale2</code> time scale.
     * </p>
     *
     * @param scale1
     *        first time scale
     * @param scale2
     *        second time scale
     * @return offset in seconds between the two time scales at the
     *         current instant
     */
    public double timeScalesOffset(final TimeScale scale1, final TimeScale scale2) {
        return scale1.offsetFromTAI(this) - scale2.offsetFromTAI(this);
    }

    /**
     * Convert the instance to a Java {@link java.util.Date Date}.
     * <p>
     * Conversion to the Date class induces a loss of precision because the Date class does not provide sub-millisecond
     * information. Java Dates are considered to be locations in some times scales.
     * </p>
     *
     * @param timeScale
     *        time scale to use
     * @return a {@link java.util.Date Date} instance representing the location
     *         of the instant in the time scale
     * @deprecated As of PATRIUS 4.8, method using {@link Date} are deprecated. Use
     * {@link #toLocalDateTime(TimeScale)} instead.
     */
    @Deprecated
    public Date toDate(final TimeScale timeScale) {
        final double time = this.epoch + (this.offset + timeScale.offsetFromTAI(this));
        return new Date(MathLib.round((time + 10957.5 * 86400.0) * 1000));
    }

    /**
     * Convert the instance to a Java {@link LocalDateTime}.
     * <p>
     * Conversion to the Instant class induces a loss of precision because the LocalDateTime
     * class does not provide sub-nanosecond
     * information. Java LocalDateTime are considered to be locations in some times scales.
     * </p>
     *
     * @param timeScale
     *        time scale to use
     * @return a {@link LocalDateTime} instance representing the location
     *         of the instant in the time scale
     */
    public LocalDateTime toLocalDateTime(final TimeScale timeScale) {
        final double time = this.epoch + (this.offset + timeScale.offsetFromTAI(this));
        final double res = time + 10957.5 * 86400.0;
        return LocalDateTime.ofEpochSecond((long) MathLib.floor(res),
                (int) MathLib.round((res - MathLib.floor(res)) * 1E9), ZoneOffset.UTC);
    }

    /**
     * Convert the date to CNES Julian date.
     * @param timeScale
     *        time scale to use
     * @return the corresponding CNES Julian date (number of days since epoch 1950)
     */
    public double toCNESJulianDate(final TimeScale timeScale) {
        return this.offsetFrom(FIFTIES_EPOCH_TAI, timeScale) / Constants.JULIAN_DAY;
    }


    /**
     * Split the instance into date/time components.
     *
     * @param timeScale
     *        time scale to use
     * @return date/time components
     */
    public DateTimeComponents getComponents(final TimeScale timeScale) {

        // compute offset from 2000-01-01T00:00:00 in specified time scale
        final double offsetInTS = this.offset + timeScale.offsetFromTAI(this);
        final long carry = (long) MathLib.floor(offsetInTS);
        final double offset2000B = offsetInTS - carry;
        final long offset2000A = this.epoch + carry + 43200L;
        long time = offset2000A % 86400L;
        if (time < 0L) {
            time += 86400L;
        }
        final int date = (int) ((offset2000A - time) / 86400L);

        // extract calendar elements
        final DateComponents dateComponents = new DateComponents(DateComponents.J2000_EPOCH, date);
        TimeComponents timeComponents = new TimeComponents((int) time, offset2000B);

        if (timeScale instanceof UTCScale) {
            final UTCScale utc = (UTCScale) timeScale;
            if (utc.insideLeap(this)) {
                // fix the seconds number to take the leap into account
                timeComponents = new TimeComponents(timeComponents.getHour(), timeComponents.getMinute(),
                    timeComponents.getSecond() + utc.getLeap(this));
            }
        }

        // build the components
        return new DateTimeComponents(dateComponents, timeComponents);

    }

    /**
     * Compare the instance with another date.
     *
     * @param date
     *        other date to compare the instance to
     * @return a negative integer, zero, or a positive integer as this date
     *         is before, simultaneous, or after the specified date.
     */
    @Override
    public int compareTo(final AbsoluteDate date) {
        final double delta = this.durationFrom(date);
        int res = 0;
        if (delta < 0) {
            res = -1;
        } else if (delta > 0) {
            res = +1;
        }
        return res;
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getDate() {
        return this;
    }

    /**
     * Check if the instance represent the same time as another instance.
     * Two dates are considered equals if |d1 - d2| <= epsilon. For strict equality use {@link #equals(Object)}.
     *
     * @param date
     *        other date
     * @param epsilon epsilon for comparison
     * @return true if the instance and the other date refer to the same instant with epsilon threhold
     */
    @SuppressWarnings("PMD.SuspiciousEqualsMethodName")
    public boolean equals(final Object date, final double epsilon) {

        if (date == this) {
            // first fast check
            return true;
        }

        if ((date != null) && (date instanceof AbsoluteDate)) {
            if (Double.isInfinite(this.offset)) {
                return this.offset == ((AbsoluteDate) date).getOffset();
            }
            return MathLib.abs(this.durationFrom((AbsoluteDate) date)) <= epsilon;
        }

        return false;
    }

    /**
     * Check if the instance represent the same time as another instance.
     * Two dates are considered equals if d1 = d2. For equality with epsilon use {@link #equals(Object, double)}.
     *
     * @param date
     *        other date
     * @return true if the instance and the other date refer to the same instant
     */
    @Override
    public boolean equals(final Object date) {
        return equals(date, 0.);
    }

    /**
     * Get a hashcode for this date.
     *
     * @return hashcode
     */
    @Override
    public int hashCode() {
        final long l = Double.doubleToLongBits(this.durationFrom(J2000_EPOCH));
        return (int) (l ^ (l >>> 32));
    }

    /**
     * Get a String representation of the instant location in TAI time scale.
     *
     * @return a string representation of the instance,
     *         in ISO-8601 format with milliseconds accuracy
     */
    @Override
    public String toString() {
        return this.toString(TimeScalesFactory.getTAI());
    }

    /**
     * Get a String representation of the instant location in TAI time scale.
     *
     * @param precision
     *        digit number of the seconds fractional part
     * @return a string representation of the instance,
     *         in ISO-8601 format with a seconds accuracy defined as input
     */
    public String toString(final int precision) {
        return this.toString(precision, TimeScalesFactory.getTAI());
    }

    /**
     * Get a String representation of the instant location.
     *
     * @param timeScale
     *        time scale to use
     * @return a string representation of the instance,
     *         in ISO-8601 format with milliseconds accuracy
     */
    public String toString(final TimeScale timeScale) {
        return this.toString(TimeComponents.DEFAULT_SECONDS_PRECISION, timeScale);
    }

    /**
     * Get a String representation of the instant location.
     *
     * @param precision
     *        digit number of the seconds fractional part
     * @param timeScale
     *        time scale to use
     * @return a string representation of the instance,
     *         in ISO-8601 format with a seconds accuracy defined as input
     */
    public String toString(final int precision, final TimeScale timeScale) {
        // Initialize result
        String rez = null;
        if (this.equals(AbsoluteDate.PAST_INFINITY)) {
            // Past infinity
            rez = "Past infinity";
        } else if (this.equals(AbsoluteDate.FUTURE_INFINITY)) {
            // Future infinity
            rez = "Future infinity";
        } else {
            final DateTimeComponents comps = this.getComponents(timeScale);
            if (timeScale instanceof UTCScale) {
                // when time scale is UTC, check the date is not inside or immediately
                // before a leap second:
                final UTCScale utc = (UTCScale) timeScale;
                final double dt = 5.0 * MathLib.pow(10, -(precision + 1));
                if (utc.insideLeap(this) || utc.insideLeap(this.shiftedBy(dt))) {
                    rez = comps.toString(precision, true);
                } else {
                    rez = comps.toString(precision, false);
                }
            } else {
                rez = comps.toString(precision, false);
            }
        }
        // Return result
        return rez;
    }

    /**
     * Returns the epoch attribute.<br>
     * May be used, with a matching offset value, to rebuild an AbsoluteDate with the
     * {@link AbsoluteDate#AbsoluteDate(long, double)} constructor.
     *
     * @return the epoch attribute
     */
    public long getEpoch() {
        return this.epoch;
    }

    /**
     * Returns the offset attribute.<br>
     * May be used, with a matching epoch value, to rebuild an AbsoluteDate with the
     * {@link AbsoluteDate#AbsoluteDate(long, double)} constructor.
     *
     * @return the offset attribute
     */
    public double getOffset() {
        return this.offset;
    }

    /**
     * Returns the week number (corresponding to a GPS date).
     * <p>
     * GPS dates are provided as a week number starting at {@link #GPS_EPOCH GPS epoch} and as a number of milliseconds
     * since week start.
     * </p>
     *
     * @return week number
     */
    public int getWeekNumber() {
        final double totalSeconds = this.durationFrom(GPS_EPOCH);
        final double days = totalSeconds / Constants.JULIAN_DAY;

        return (int) MathLib.floor(days / 7);
    }

    /**
     * Returns the number of milliseconds since week (corresponding to a GPS date).
     * <p>
     * GPS dates are provided as a week number starting at {@link #GPS_EPOCH GPS epoch} and as a number of milliseconds
     * since week start.
     * </p>
     *
     * @return number of milliseconds since week start
     */
    public double getMilliInWeek() {
        final DateTimeComponents components = this.getComponents(TimeScalesFactory.getGPS());
        final DateComponents date = components.getDate();
        final TimeComponents time = components.getTime();

        final double secInDay = time.getSecondsInDay();
        // 1st day of week (GPS epoch) is Sunday - getDayOfWeek returns 1 for Monday which is what is expected
        // in the formula
        final int dayWeek = date.getDayOfWeek() % 7;

        return (dayWeek * Constants.JULIAN_DAY + secInDay) * 1000;
    }

    /**
     * Get the number of seconds within the day.
     *
     * @param timeScale the time scale to use
     * @return second number from 0.0 to Constants.JULIAN_DAY
     */
    public double getSecondsInDay(final TimeScale timeScale) {
        final DateTimeComponents components = this.getComponents(timeScale);
        final TimeComponents time = components.getTime();
        return time.getSecondsInDay();
    }

    // CHECKSTYLE: resume MagicNumber check
}
