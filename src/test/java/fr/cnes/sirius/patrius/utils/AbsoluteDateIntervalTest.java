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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.7:DM:DM-2650:18/05/2021:constructeur d intervalle de temps avec date de debut et duree
 * VERSION:4.5:DM:DM-2471:27/05/2020:Ajout d'une methode toString(TimeScale) a la classe AbsoluteDateInterval
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1936:23/10/2018: add new methods to intervals classes
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.testng.Assert;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.interval.ComparableInterval;
import fr.cnes.sirius.patrius.math.interval.IntervalEndpointType;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.time.TimeScale;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Unit tests for <code>AbsoluteDateInterval</code>.
 * 
 * @author cardosop
 * 
 * @version $Id: AbsoluteDateIntervalTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.0
 */
public class AbsoluteDateIntervalTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle DATE_INTERVAL
         * 
         * @featureDescription implementation of a date interval
         * 
         * @coveredRequirements DV-DATES_150, DV-DATES_160, DV-DATES_180
         * 
         */
        DATE_INTERVAL,
        /**
         * @featureTitle DATE_INTERVAL_DURATION
         * 
         * @featureDescription implementation of a date interval duration
         * 
         * @coveredRequirements DV-DATES_200
         * 
         */
        DATE_INTERVAL_DURATION,
        /**
         * @featureTitle DATE_MIDDLE_DATE
         * 
         * @featureDescription implementation of a date interval middle date extraction
         * 
         */
        DATE_MIDDLE_DATE,
        /**
         * @featureTitle DATE_INTERVAL_DURATIONFROM
         * 
         * @featureDescription implementation of a method computing duration between intervals
         * 
         * @coveredRequirements DV-DATES_200
         * 
         */
        DATE_INTERVAL_DURATIONFROM,
        /**
         * @featureTitle DATE_INTERVAL_COMPAREDURATION
         * 
         * @featureDescription implementation of a method comparing the duration between intervals
         * 
         * @coveredRequirements DV-DATES_210
         * 
         */
        DATE_INTERVAL_COMPAREDURATION,
        /**
         * @featureTitle MERGE_DATE_INTERVALS
         * 
         * @featureDescription implementation of a method testing intervals merging
         * 
         * @coveredRequirements DV-DATES_200
         */
        MERGE_DATE_INTERVALS,
        /**
         * @featureTitle INTERSECT_DATE_INTERVALS
         * 
         * @featureDescription implementation of a method testing intervals intersection
         * 
         * @coveredRequirements DV-DATES_210
         */
        INTERSECT_DATE_INTERVALS
    }

    /** Epsilon taking into account the machine error. */
    private final double zeroEpsilon = 0.0;

    /** The OPEN end point type. */
    private final IntervalEndpointType open = IntervalEndpointType.OPEN;

    /** The CLOSED end point type. */
    private final IntervalEndpointType closed = IntervalEndpointType.CLOSED;

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#DATE_INTERVAL}
     * 
     * @testedMethod {@link AbsoluteDateInterval#AbsoluteDateInterval(IntervalEndpointType, AbsoluteDate, AbsoluteDate, IntervalEndpointType)}
     * 
     * @description unit test for the constructor and getters
     * 
     * @input data for the constructor
     * 
     * @output data from the getters
     * 
     * @testPassCriteria input and output data must match
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testAbsoluteDateInterval() {
        final AbsoluteDate lowEnd = new AbsoluteDate();
        final AbsoluteDate upEnd = new AbsoluteDate(lowEnd, 4578.14);

        final AbsoluteDateInterval dateInterval = new AbsoluteDateInterval(this.closed, lowEnd,
                upEnd, this.open);
        assertNotNull(dateInterval);
        // Tests for the getters
        final AbsoluteDate gLowEnd = dateInterval.getLowerData();
        final AbsoluteDate gUpEnd = dateInterval.getUpperData();
        final IntervalEndpointType gLowInt = dateInterval.getLowerEndpoint();
        final IntervalEndpointType gUpInt = dateInterval.getUpperEndpoint();
        assertTrue(lowEnd.equals(gLowEnd));
        assertTrue(upEnd.equals(gUpEnd));
        assertEquals(this.closed, gLowInt);
        assertEquals(this.open, gUpInt);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#DATE_INTERVAL}
     * 
     * @testedMethod {@link AbsoluteDateInterval#AbsoluteDateInterval(IntervalEndpointType, AbsoluteDate, AbsoluteDate, IntervalEndpointType)}
     * 
     * @description unit test for the constructor using infinite dates
     * 
     * @input data for the constructor, including infinite dates (
     *        {@link AbsoluteDate#PAST_INFINITY}, {@link AbsoluteDate#FUTURE_INFINITY} )
     * 
     * @output <code>AbsoluteDateInterval</code> instances
     * 
     * @testPassCriteria instances correctly created
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testValidInfinities() {
        final AbsoluteDate someDate = new AbsoluteDate();
        final AbsoluteDate future = AbsoluteDate.FUTURE_INFINITY;
        final AbsoluteDate past = AbsoluteDate.PAST_INFINITY;
        // Creating some intervals...
        AbsoluteDateInterval dateInterval = new AbsoluteDateInterval(past, future);
        assertNotNull(dateInterval);
        dateInterval = new AbsoluteDateInterval(this.closed, someDate, future, this.open);
        assertNotNull(dateInterval);
        dateInterval = new AbsoluteDateInterval(this.open, past, future, this.open);
        assertNotNull(dateInterval);
        dateInterval = new AbsoluteDateInterval(this.open, past, someDate, this.closed);
        assertNotNull(dateInterval);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#DATE_INTERVAL}
     * 
     * @testedMethod {@link AbsoluteDateInterval#AbsoluteDateInterval(IntervalEndpointType, AbsoluteDate, AbsoluteDate, IntervalEndpointType)}
     * 
     * @description unit test for invalid constructor parameters (infinite dates)
     * 
     * @input invalid constructor parameters
     * 
     * @output {@link MathIllegalArgumentException}
     * 
     * @testPassCriteria exceptions as expected
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testInvalidInfinities() {
        final AbsoluteDate past = AbsoluteDate.PAST_INFINITY;
        final AbsoluteDate future = AbsoluteDate.FUTURE_INFINITY;
        // Tests for closed infinite endpoints
        // (many tests needed for branch coverage)
        boolean asExpected = false;
        try {
            new AbsoluteDateInterval(this.closed, past, future, this.closed);
        } catch (final MathIllegalArgumentException e) {
            asExpected = true;
        }
        assertTrue(asExpected);
        asExpected = false;
        try {
            new AbsoluteDateInterval(this.open, past, future, this.closed);
        } catch (final MathIllegalArgumentException e) {
            asExpected = true;
        }
        assertTrue(asExpected);
        asExpected = false;
        try {
            new AbsoluteDateInterval(this.closed, past, future, this.open);
        } catch (final MathIllegalArgumentException e) {
            asExpected = true;
        }
        asExpected = false;
        try {
            new AbsoluteDateInterval(this.closed, past, past, this.open);
        } catch (final MathIllegalArgumentException e) {
            asExpected = true;
        }
        asExpected = false;
        try {
            new AbsoluteDateInterval(this.closed, future, future, this.open);
        } catch (final MathIllegalArgumentException e) {
            asExpected = true;
        }
        assertTrue(asExpected);
        // Test for wrong interval order
        asExpected = false;
        try {
            new AbsoluteDateInterval(this.open, future, past, this.open);
        } catch (final MathIllegalArgumentException e) {
            asExpected = true;
        }
        assertTrue(asExpected);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#DATE_INTERVAL}
     * 
     * @testedMethod {@link AbsoluteDateInterval#AbsoluteDateInterval(IntervalEndpointType, AbsoluteDate, AbsoluteDate, IntervalEndpointType)}
     * 
     * @description unit test for invalid constructor parameters (empty intervals)
     * 
     * @input invalid constructor parameters
     * 
     * @output {@link MathIllegalArgumentException}
     * 
     * @testPassCriteria exceptions as expected
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testForbidEmpty() {
        final AbsoluteDate t1 = new AbsoluteDate("1969-09-03", TimeScalesFactory.getTT());
        // non-empty interval
        // [t1;t1]
        final AbsoluteDateInterval adne1 = new AbsoluteDateInterval(this.closed, t1, t1,
                this.closed);
        assertNotNull(adne1);
        // empty intervals:
        // [t1;t1[
        boolean asExpected = false;
        try {
            new AbsoluteDateInterval(this.closed, t1, t1, this.open);
        } catch (final MathIllegalArgumentException e) {
            asExpected = true;
        }
        assertTrue(asExpected);
        // ]t1;t1]
        asExpected = false;
        try {
            new AbsoluteDateInterval(this.open, t1, t1, this.closed);
        } catch (final MathIllegalArgumentException e) {
            asExpected = true;
        }
        assertTrue(asExpected);
        // ]t1;t1[
        asExpected = false;
        try {
            new AbsoluteDateInterval(this.open, t1, t1, this.open);
        } catch (final MathIllegalArgumentException e) {
            asExpected = true;
        }
        assertTrue(asExpected);
    }

    /*
     * Tests <code>toString</code> for an infinite interval.<br>
     * Complimentary test added to double-check a bug found in Orekit.
     */

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#DATE_INTERVAL}
     * 
     * @testedMethod {@link AbsoluteDateInterval#toString()}
     * 
     * @description unit test for toString (infinite dates)
     * 
     * @input constructor data (with infinite dates)
     * 
     * @output strings from toString()
     * 
     * @testPassCriteria strings with format and values as expected
     * 
     * @referenceVersion 4.5
     * 
     * @nonRegressionVersion 4.5
     */
    @Test
    public void testToString() {
        final AbsoluteDate past = AbsoluteDate.PAST_INFINITY;
        final AbsoluteDate future = AbsoluteDate.FUTURE_INFINITY;
        final String expectedString = "] " + past.toString() + " ; " + future.toString() + " [";
        final AbsoluteDateInterval adi = new AbsoluteDateInterval(this.open, past, future,
                this.open);
        assertEquals(expectedString, adi.toString());

        // With time scale
        final String expectedString2 = "] "
                + AbsoluteDate.JAVA_EPOCH.toString(TimeScalesFactory.getTT()) + " ; "
                + AbsoluteDate.J2000_EPOCH.toString(TimeScalesFactory.getTT()) + " [";
        final AbsoluteDateInterval adi2 = new AbsoluteDateInterval(this.open,
                AbsoluteDate.JAVA_EPOCH, AbsoluteDate.J2000_EPOCH, this.open);
        assertEquals(expectedString2, adi2.toString(TimeScalesFactory.getTT()));

        // 2nd method
        final AbsoluteDate d1 = AbsoluteDate.J2000_EPOCH.shiftedBy(10.);
        final AbsoluteDate d2 = AbsoluteDate.J2000_EPOCH.shiftedBy(30.);
        final AbsoluteDateInterval interval = new AbsoluteDateInterval(d1, d2.durationFrom(d1));
        final AbsoluteDate reference = AbsoluteDate.J2000_EPOCH;
        assertEquals("[ 10.0 ; 30.0 ]", interval.toString(reference));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#DATE_INTERVAL_DURATION}
     * 
     * @testedMethod {@link AbsoluteDateInterval#getDuration()}
     * 
     * @description unit test for the <code>getDuration</code> method
     * 
     * @input several <code>AbsoluteDateInterval</code> instances
     * 
     * @output durations for these intervals
     * 
     * @testPassCriteria the durations are exactly as expected
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testGetDuration() {
        final AbsoluteDate lowerDate = new AbsoluteDate("1969-11-23", TimeScalesFactory.getTT());
        final AbsoluteDate upperDate = new AbsoluteDate("1969-11-24", TimeScalesFactory.getTT());
        final AbsoluteDate past = AbsoluteDate.PAST_INFINITY;
        final AbsoluteDate future = AbsoluteDate.FUTURE_INFINITY;
        final AbsoluteDateInterval adi1 = new AbsoluteDateInterval(this.closed, lowerDate,
                upperDate, this.closed);
        final AbsoluteDateInterval adi2 = new AbsoluteDateInterval(this.open, lowerDate, upperDate,
                this.closed);
        final AbsoluteDateInterval adi3 = new AbsoluteDateInterval(this.closed, lowerDate,
                upperDate, this.open);
        final AbsoluteDateInterval adi4 = new AbsoluteDateInterval(this.open, lowerDate, upperDate,
                this.open);
        // First test : we expect the interval to be a day long.
        final double expectedDuration = Constants.JULIAN_DAY;
        assertEquals(expectedDuration, adi1.getDuration(), this.zeroEpsilon);
        // Second test : the endpoint types should make no difference.
        assertEquals(adi1.getDuration(), adi2.getDuration(), this.zeroEpsilon);
        assertEquals(adi1.getDuration(), adi3.getDuration(), this.zeroEpsilon);
        assertEquals(adi1.getDuration(), adi4.getDuration(), this.zeroEpsilon);
        // Third test : several infinite intervals : they should all be infinitely long
        final AbsoluteDateInterval infiniteInterval1 = new AbsoluteDateInterval(this.open, past,
                future, this.open);
        assertEquals(Double.POSITIVE_INFINITY, infiniteInterval1.getDuration(), this.zeroEpsilon);
        final AbsoluteDateInterval infiniteInterval2 = new AbsoluteDateInterval(this.open, past,
                upperDate, this.closed);
        assertEquals(Double.POSITIVE_INFINITY, infiniteInterval2.getDuration(), this.zeroEpsilon);
        final AbsoluteDateInterval infiniteInterval3 = new AbsoluteDateInterval(this.closed,
                lowerDate, future, this.open);
        assertEquals(Double.POSITIVE_INFINITY, infiniteInterval3.getDuration(), this.zeroEpsilon);
        // Fourth test : interval with equal endpoints : should be of length 0.
        final AbsoluteDateInterval adie1 = new AbsoluteDateInterval(this.closed, lowerDate,
                lowerDate, this.closed);
        assertEquals(0., adie1.getDuration(), this.zeroEpsilon);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#DATE_MIDDLE_DATE}
     * 
     * @testedMethod {@link AbsoluteDateInterval#getMiddleDate()}
     * 
     * @description unit test for the <code>getMiddleDate</code> method
     * 
     * @input several <code>AbsoluteDateInterval</code> instances
     * 
     * @output middle dates for these intervals
     * 
     * @testPassCriteria the middle dates are exactly as expected
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testMiddleDate() {
        final TimeScale tt = TimeScalesFactory.getTT();
        final AbsoluteDate lowerDate = new AbsoluteDate("1969-11-23", tt);
        final AbsoluteDate upperDate = new AbsoluteDate("1969-11-24", tt);
        final AbsoluteDateInterval adi1 = new AbsoluteDateInterval(this.closed, lowerDate,
                upperDate, this.closed);
        final AbsoluteDateInterval adi2 = new AbsoluteDateInterval(this.open, lowerDate, upperDate,
                this.closed);
        final AbsoluteDateInterval adi3 = new AbsoluteDateInterval(this.closed, lowerDate,
                upperDate, this.open);
        final AbsoluteDateInterval adi4 = new AbsoluteDateInterval(this.open, lowerDate, upperDate,
                this.open);
        // First test : we expect the middle date to be 1969-11-23T12:00:00.000[TT].
        final AbsoluteDate expectedMiddleDate = new AbsoluteDate("1969-11-23T12:00:00.000", tt);
        assertTrue(adi1.getMiddleDate().equals(expectedMiddleDate));
        // Second test : the endpoint types should make no difference.
        assertTrue(adi2.getMiddleDate().equals(expectedMiddleDate));
        assertTrue(adi3.getMiddleDate().equals(expectedMiddleDate));
        assertTrue(adi4.getMiddleDate().equals(expectedMiddleDate));
        // Third test : interval with equal endpoints : middle date should be the endpoint date.
        final AbsoluteDateInterval adie1 = new AbsoluteDateInterval(this.closed, lowerDate,
                lowerDate, this.closed);
        assertTrue(adie1.getMiddleDate().equals(lowerDate));
        // Fourth test : several infinite intervals: should return PAST_INFINITY when the lower
        // bound is PAST_INFINITY, FUTURE_INFINITY otherwise
        final AbsoluteDate past = AbsoluteDate.PAST_INFINITY;
        final AbsoluteDate future = AbsoluteDate.FUTURE_INFINITY;
        final AbsoluteDateInterval infiniteInterval1 = new AbsoluteDateInterval(this.open, past,
                future, this.open);
        assertTrue(infiniteInterval1.getMiddleDate().equals(past));
        final AbsoluteDateInterval infiniteInterval2 = new AbsoluteDateInterval(this.open, past,
                upperDate, this.closed);
        assertTrue(infiniteInterval2.getMiddleDate().equals(past));
        final AbsoluteDateInterval infiniteInterval3 = new AbsoluteDateInterval(this.closed,
                lowerDate, future, this.open);
        assertTrue(infiniteInterval3.getMiddleDate().equals(future));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#DATE_INTERVAL_DURATIONFROM}
     * 
     * @testedMethod {@link AbsoluteDateInterval#durationFrom(AbsoluteDateInterval)}
     * 
     * @description unit test for the <code>durationFrom</code> method
     * 
     * @input several <code>AbsoluteDateInterval</code> instances
     * 
     * @output several durations between two intervals
     * 
     * @testPassCriteria the durations are exactly as expected
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testDurationFrom() {
        final AbsoluteDate t1 = new AbsoluteDate("1969-11-03", TimeScalesFactory.getTT());
        final AbsoluteDate t2 = new AbsoluteDate("1969-11-04", TimeScalesFactory.getTT());
        final AbsoluteDate t3 = new AbsoluteDate("1969-11-06", TimeScalesFactory.getTT());
        final AbsoluteDate t4 = new AbsoluteDate("1969-11-07", TimeScalesFactory.getTT());
        final AbsoluteDate past = AbsoluteDate.PAST_INFINITY;
        final AbsoluteDate future = AbsoluteDate.FUTURE_INFINITY;
        final double dayInSeconds = Constants.JULIAN_DAY;
        // Test : two separated intervals, two days of separation :
        // ..] t1 ; t2 [....] t3 ; t4 [..
        final AbsoluteDateInterval ad1A = new AbsoluteDateInterval(this.open, t1, t2, this.open);
        final AbsoluteDateInterval ad1B = new AbsoluteDateInterval(this.open, t3, t4, this.open);
        final double dur11 = ad1B.durationFrom(ad1A);
        final double dur12 = ad1A.durationFrom(ad1B);
        // ad1B begins 2 days after ad1A ends : DurationFrom is + 2 days
        assertEquals(2. * dayInSeconds, dur11, this.zeroEpsilon);
        // ad1A ends 2 days before ad1B begins : DurationFrom is - 2 days
        assertEquals(-2. * dayInSeconds, dur12, this.zeroEpsilon);
        // Test : two overlapping intervals , one day of overlap :
        // ..] t1 ; t3 [.....
        // .....] t2 ; t4 [..
        final AbsoluteDateInterval ad2A = new AbsoluteDateInterval(this.open, t1, t3, this.open);
        final AbsoluteDateInterval ad2B = new AbsoluteDateInterval(this.open, t2, t4, this.open);
        final double dur21 = ad1B.durationFrom(ad2A);
        final double dur22 = ad1A.durationFrom(ad2B);
        // ad2A and ad2B overlap : duration between the two is 0.
        assertEquals(0., dur21, this.zeroEpsilon);
        assertEquals(0., dur22, this.zeroEpsilon);
        // Test : an interval completely inside another
        // ..] t1... ; ...t4 [..
        // .....] t2 ; t3 [..
        final AbsoluteDateInterval ad3A = new AbsoluteDateInterval(this.open, t1, t4, this.open);
        final AbsoluteDateInterval ad3B = new AbsoluteDateInterval(this.open, t2, t3, this.open);
        final double dur31 = ad3B.durationFrom(ad3A);
        final double dur32 = ad3A.durationFrom(ad3B);
        // ad3A and ad3B overlap : duration between the two is 0.
        assertEquals(0., dur31, this.zeroEpsilon);
        assertEquals(0., dur32, this.zeroEpsilon);
        // Test : two non-overlapping intervals BUT duration between the two is 0.
        // ..] t1 ; t2 [.....
        // .......] t2 ; t3 [
        final AbsoluteDateInterval ad4A = new AbsoluteDateInterval(this.open, t1, t2, this.open);
        final AbsoluteDateInterval ad4B = new AbsoluteDateInterval(this.open, t2, t3, this.open);
        final double dur41 = ad4B.durationFrom(ad4A);
        final double dur42 = ad4A.durationFrom(ad4B);
        assertEquals(0., dur41, this.zeroEpsilon);
        assertEquals(0., dur42, this.zeroEpsilon);
        // Test : two non-overlapping intervals with some infinite endpoints
        // ] Past infinite ; t2 [....] t3 ; Future infinite [
        final AbsoluteDateInterval ad5A = new AbsoluteDateInterval(this.open, past, t2, this.open);
        final AbsoluteDateInterval ad5B = new AbsoluteDateInterval(this.open, t3, future, this.open);
        final double dur51 = ad5B.durationFrom(ad5A);
        final double dur52 = ad5A.durationFrom(ad5B);
        // 2 days of separation
        assertEquals(2. * dayInSeconds, dur51, this.zeroEpsilon);
        assertEquals(-2. * dayInSeconds, dur52, this.zeroEpsilon);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#DATE_INTERVAL_COMPAREDURATION}
     * 
     * @testedMethod {@link AbsoluteDateInterval#compareDurationTo(AbsoluteDateInterval)}
     * 
     * @description unit test for the <code>compareDurationTo</code> method
     * 
     * @input several <code>AbsoluteDateInterval</code> instances
     * 
     * @output several durations between two intervals
     * 
     * @testPassCriteria the result of the comparison between durations is the expected one.
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void testCompareDurationTo() {
        final AbsoluteDate t1 = new AbsoluteDate("1980-11-03", TimeScalesFactory.getTT());
        final AbsoluteDate t2 = new AbsoluteDate("1980-11-05", TimeScalesFactory.getTT());
        final AbsoluteDate t3 = new AbsoluteDate("1980-11-06", TimeScalesFactory.getTT());
        final AbsoluteDate t4 = new AbsoluteDate("1980-11-07", TimeScalesFactory.getTT());

        // Test: two intervals with different durations:
        // ] t1 ; t2 [
        // [ t3 ; t4 ]
        final AbsoluteDateInterval ad1A = new AbsoluteDateInterval(this.open, t1, t2, this.open);
        final AbsoluteDateInterval ad1B = new AbsoluteDateInterval(this.open, t3, t4, this.open);
        // ad1A is longer than ad1B: returns +1:
        assertEquals(+1, ad1A.compareDurationTo(ad1B));
        // ad1B is shorter than ad1A: returns -1:
        assertEquals(-1, ad1B.compareDurationTo(ad1A));
        // Test: two intervals with the same durations, different lower end point:
        // ] t1 ; t3 [
        // [ t1 ; t3 [
        final AbsoluteDateInterval ad2A = new AbsoluteDateInterval(this.open, t1, t3, this.open);
        final AbsoluteDateInterval ad2B = new AbsoluteDateInterval(this.closed, t1, t3, this.open);
        // ad2A is shorter than ad2B: returns -1:
        assertEquals(-1, ad2A.compareDurationTo(ad2B));
        // ad2B is longer than ad2A: returns +1:
        assertEquals(+1, ad2B.compareDurationTo(ad2A));
        // Test: two intervals with the same durations, different upper end point:
        // [ t1 ; t3 ]
        // [ t1 ; t3 [
        final AbsoluteDateInterval ad3A = new AbsoluteDateInterval(this.closed, t1, t3, this.closed);
        final AbsoluteDateInterval ad3B = new AbsoluteDateInterval(this.closed, t1, t3, this.open);
        // ad3A is longer than ad3B: returns +1:
        assertEquals(+1, ad3A.compareDurationTo(ad3B));
        // ad3B is shorter than ad3A: returns -1:
        assertEquals(-1, ad3B.compareDurationTo(ad3A));
        // Test: two intervals with the same durations, same lower/upper end points:
        // [ t1 ; t3 [
        // [ t1 ; t3 [
        final AbsoluteDateInterval ad4A = new AbsoluteDateInterval(this.closed, t1, t3, this.open);
        final AbsoluteDateInterval ad4B = new AbsoluteDateInterval(this.closed, t1, t3, this.open);
        // ad4A and ad4B are identical: returns 0:
        assertEquals(0, ad4A.compareDurationTo(ad4B));
        // ad4B and ad4A are identical: returns 0:
        assertEquals(0, ad4B.compareDurationTo(ad4A));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#MERGE_DATE_INTERVALS}
     * 
     * @testedMethod {@link AbsoluteDateInterval#mergeTo(AbsoluteDateInterval)}
     * 
     * @description unit test for the <code>mergeTo</code> method
     * 
     * @input several <code>AbsoluteDateInterval</code> instances
     * 
     * @output the output intervals of <code>mergeTo</code>
     * 
     * @testPassCriteria the merged intervals are equal to the expected intervals
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void testMergeTo() {
        final AbsoluteDate d1 = new AbsoluteDate("1990-11-03", TimeScalesFactory.getTT());
        final AbsoluteDate d2 = new AbsoluteDate("1990-11-05", TimeScalesFactory.getTT());
        final AbsoluteDate d3 = new AbsoluteDate("1990-11-06", TimeScalesFactory.getTT());
        final AbsoluteDate d4 = new AbsoluteDate("1990-11-07", TimeScalesFactory.getTT());

        // Test : two disjoint intervals
        // ] d1 ; d2 [
        // [ d3 ; d4 ]
        final AbsoluteDateInterval i11 = new AbsoluteDateInterval(this.open, d1, d2, this.open);
        final AbsoluteDateInterval i12 = new AbsoluteDateInterval(this.closed, d3, d4, this.closed);
        assertEquals(null, i11.mergeTo(i12));
        assertEquals(null, i12.mergeTo(i11));
        // Test : two overlapping intervals
        // [ d1 ; d3 ]
        // ] d2 ; d4 [
        final AbsoluteDateInterval i21 = new AbsoluteDateInterval(this.closed, d1, d3, this.closed);
        final AbsoluteDateInterval i22 = new AbsoluteDateInterval(this.open, d2, d4, this.open);
        final AbsoluteDateInterval expected2 = new AbsoluteDateInterval(this.closed, d1, d4,
                this.open);
        assertEquals(expected2, i21.mergeTo(i22));
        assertEquals(expected2, i22.mergeTo(i21));
        // Test : two connected intervals
        // [ d1 ; d2 ]
        // ] d2 ; d3 [
        final AbsoluteDateInterval i31 = new AbsoluteDateInterval(this.closed, d1, d2, this.closed);
        final AbsoluteDateInterval i32 = new AbsoluteDateInterval(this.open, d2, d3, this.open);
        final AbsoluteDateInterval expected3 = new AbsoluteDateInterval(this.closed, d1, d3,
                this.open);
        assertEquals(expected3, i31.mergeTo(i32));
        assertEquals(expected3, i32.mergeTo(i31));
        // Test : two non-connected intervals
        // [ d1 ; d2 [
        // ] d2 ; d3 [
        final AbsoluteDateInterval i41 = new AbsoluteDateInterval(this.closed, d1, d2, this.open);
        final AbsoluteDateInterval i42 = new AbsoluteDateInterval(this.open, d2, d3, this.open);
        assertEquals(null, i41.mergeTo(i42));
        assertEquals(null, i42.mergeTo(i41));
        // Test : two intervals, one included in the other one
        // [ d1 ; d4 ]
        // ] d2 ; d3 [
        final AbsoluteDateInterval i51 = new AbsoluteDateInterval(this.closed, d1, d4, this.closed);
        final AbsoluteDateInterval i52 = new AbsoluteDateInterval(this.open, d2, d3, this.open);
        final AbsoluteDateInterval expected5 = new AbsoluteDateInterval(this.closed, d1, d4,
                this.closed);
        assertEquals(expected5, i51.mergeTo(i52));
        assertEquals(expected5, i52.mergeTo(i51));
        // Test : two intervals, one included in the other one and with same upper end point
        // [ d1 ; d3 ]
        // ] d2 ; d3 [
        final AbsoluteDateInterval i61 = new AbsoluteDateInterval(this.closed, d1, d3, this.closed);
        final AbsoluteDateInterval i62 = new AbsoluteDateInterval(this.open, d2, d3, this.open);
        final AbsoluteDateInterval expected6 = new AbsoluteDateInterval(this.closed, d1, d3,
                this.closed);
        assertEquals(expected6, i61.mergeTo(i62));
        assertEquals(expected6, i62.mergeTo(i61));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#INTERSECT_DATE_INTERVALS}
     * 
     * @testedMethod {@link AbsoluteDateInterval#getIntersectionWith(AbsoluteDateInterval)}
     * 
     * @description unit test for the <code>getIntersectionWith</code> method
     * 
     * @input several <code>AbsoluteDateInterval</code> instances
     * 
     * @output the output intervals of <code>getIntersectionWith</code>
     * 
     * @testPassCriteria the intersection intervals are equal to the expected intervals
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void testGetIntersectionWith() {
        final AbsoluteDate d1 = new AbsoluteDate("1991-11-03", TimeScalesFactory.getTT());
        final AbsoluteDate d2 = new AbsoluteDate("1991-11-05", TimeScalesFactory.getTT());
        final AbsoluteDate d3 = new AbsoluteDate("1991-11-06", TimeScalesFactory.getTT());
        final AbsoluteDate d4 = new AbsoluteDate("1991-11-07", TimeScalesFactory.getTT());

        // Test : two disjoint intervals
        // ] d1 ; d2 [
        // [ d3 ; d4 ]
        final AbsoluteDateInterval i11 = new AbsoluteDateInterval(this.open, d1, d2, this.open);
        final AbsoluteDateInterval i12 = new AbsoluteDateInterval(this.closed, d3, d4, this.closed);
        assertEquals(null, i11.getIntersectionWith(i12));
        assertEquals(null, i12.getIntersectionWith(i11));
        // Test : two overlapping intervals
        // [ d1 ; d3 ]
        // ] d2 ; d4 [
        final AbsoluteDateInterval i21 = new AbsoluteDateInterval(this.closed, d1, d3, this.closed);
        final AbsoluteDateInterval i22 = new AbsoluteDateInterval(this.open, d2, d4, this.open);
        final AbsoluteDateInterval expected2 = new AbsoluteDateInterval(this.open, d2, d3,
                this.closed);
        assertEquals(expected2, i21.getIntersectionWith(i22));
        assertEquals(expected2, i22.getIntersectionWith(i21));
        // Test : two connected intervals
        // [ d1 ; d2 ]
        // ] d2 ; d3 [
        final AbsoluteDateInterval i31 = new AbsoluteDateInterval(this.closed, d1, d2, this.closed);
        final AbsoluteDateInterval i32 = new AbsoluteDateInterval(this.open, d2, d3, this.open);
        assertEquals(null, i31.getIntersectionWith(i32));
        assertEquals(null, i32.getIntersectionWith(i31));
        // Test : two non-connected intervals
        // [ d1 ; d2 [
        // ] d2 ; d3 [
        final AbsoluteDateInterval i41 = new AbsoluteDateInterval(this.closed, d1, d2, this.open);
        final AbsoluteDateInterval i42 = new AbsoluteDateInterval(this.open, d2, d3, this.open);
        assertEquals(null, i41.getIntersectionWith(i42));
        assertEquals(null, i42.getIntersectionWith(i41));
        // Test : two intervals, one included in the other one
        // [ d1 ; d4 ]
        // ] d2 ; d3 [
        final AbsoluteDateInterval i51 = new AbsoluteDateInterval(this.closed, d1, d4, this.closed);
        final AbsoluteDateInterval i52 = new AbsoluteDateInterval(this.open, d2, d3, this.open);
        assertEquals(i52, i51.getIntersectionWith(i52));
        assertEquals(i52, i52.getIntersectionWith(i51));
        // Test : two intervals, one included in the other one and with same upper end point
        // [ d1 ; d3 ]
        // ] d2 ; d3 [
        final AbsoluteDateInterval i61 = new AbsoluteDateInterval(this.closed, d1, d3, this.closed);
        final AbsoluteDateInterval i62 = new AbsoluteDateInterval(this.open, d2, d3, this.open);
        final AbsoluteDateInterval expected6 = new AbsoluteDateInterval(this.open, d2, d3,
                this.open);
        assertEquals(expected6, i61.getIntersectionWith(i62));
        assertEquals(expected6, i62.getIntersectionWith(i61));
        // Test : two intervals, with same upper and lower end point
        // [ d1 ; d3 ]
        // ] d1 ; d3 [
        final AbsoluteDateInterval i71 = new AbsoluteDateInterval(this.closed, d1, d3, this.closed);
        final AbsoluteDateInterval i72 = new AbsoluteDateInterval(this.open, d1, d3, this.open);
        final AbsoluteDateInterval expected7 = new AbsoluteDateInterval(this.open, d1, d3,
                this.open);
        assertEquals(expected7, i71.getIntersectionWith(i72));
        assertEquals(expected7, i72.getIntersectionWith(i71));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#MERGE_DATE_INTERVALS}
     * 
     * @testedMethod {@link AbsoluteDateInterval#shift(double, double)}
     * @testedMethod {@link AbsoluteDateInterval#shift(double)}
     * 
     * @description unit test for the <code>shift</code> method
     * 
     * @input a <code>AbsoluteDateInterval</code> instance
     * 
     * @output the output intervals of <code>shift</code>
     * 
     * @testPassCriteria the interval is shifted as expected
     * 
     * @referenceVersion 4.2
     * 
     * @nonRegressionVersion 4.2
     */
    @Test
    public void testShift() {
        final AbsoluteDate d1 = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate d2 = AbsoluteDate.J2000_EPOCH.shiftedBy(10.);
        final AbsoluteDate d3 = AbsoluteDate.J2000_EPOCH.shiftedBy(20.);
        final AbsoluteDate d4 = AbsoluteDate.J2000_EPOCH.shiftedBy(30.);
        final AbsoluteDateInterval i1 = new AbsoluteDateInterval(this.closed, d1, d2, this.open);

        final AbsoluteDateInterval actual1 = i1.shift(10.);
        final AbsoluteDateInterval expected1 = new AbsoluteDateInterval(this.closed, d2, d3,
                this.open);
        Assert.assertEquals(expected1, actual1);

        final AbsoluteDateInterval actual2 = i1.shift(10., 20.);
        final AbsoluteDateInterval expected2 = new AbsoluteDateInterval(this.closed, d2, d4,
                this.open);
        Assert.assertEquals(expected2, actual2);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#MERGE_DATE_INTERVALS}
     * 
     * @testedMethod {@link AbsoluteDateInterval#extendTo(AbsoluteDate)}
     * 
     * @description unit test for the <code>extendTo</code> method
     * 
     * @input a <code>AbsoluteDateInterval</code> instance
     * 
     * @output the output intervals of <code>extendTo</code>
     * 
     * @testPassCriteria the interval is extended as expected
     * 
     * @referenceVersion 4.2
     * 
     * @nonRegressionVersion 4.2
     */
    @Test
    public void testExtendTo() {
        final AbsoluteDate d1 = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate d2 = AbsoluteDate.J2000_EPOCH.shiftedBy(10.);
        final AbsoluteDate d4 = AbsoluteDate.J2000_EPOCH.shiftedBy(30.);
        final ComparableInterval<Double> doubleInterval = new ComparableInterval<>(
                this.closed, 10., 20., this.open);
        final AbsoluteDateInterval interval = new AbsoluteDateInterval(doubleInterval, d1);

        // Only one simple test, method is already validated in parent class
        final AbsoluteDateInterval actual = interval.extendTo(d4);
        final AbsoluteDateInterval expected = new AbsoluteDateInterval(this.closed, d2, d4,
                this.open);
        Assert.assertEquals(expected, actual);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#MERGE_DATE_INTERVALS}
     * 
     * @testedMethod {@link AbsoluteDateInterval#scale(double)}
     * 
     * @description unit test for the <code>scale</code> method
     * 
     * @input a <code>AbsoluteDateInterval</code> instance
     * 
     * @output the output intervals of <code>scale</code>
     * 
     * @testPassCriteria the interval is scaled as expected
     * 
     * @referenceVersion 4.2
     * 
     * @nonRegressionVersion 4.2
     */
    @Test
    public void testScale() {
        final AbsoluteDate d0 = AbsoluteDate.J2000_EPOCH.shiftedBy(-5.);
        final AbsoluteDate d1 = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate d2 = AbsoluteDate.J2000_EPOCH.shiftedBy(10.);
        final AbsoluteDate d3 = AbsoluteDate.J2000_EPOCH.shiftedBy(15.);
        final AbsoluteDate ref = AbsoluteDate.J2000_EPOCH.shiftedBy(20.);
        final AbsoluteDateInterval interval = new AbsoluteDateInterval(this.closed, d1, d2,
                this.open);

        // General case
        final AbsoluteDateInterval actual1 = interval.scale(0.5, ref);
        final AbsoluteDateInterval expected1 = new AbsoluteDateInterval(this.closed, d2, d3,
                this.open);
        Assert.assertEquals(expected1, actual1);

        // Mid-point case
        final AbsoluteDateInterval actual2 = interval.scale(2.);
        final AbsoluteDateInterval expected2 = new AbsoluteDateInterval(this.closed, d0, d3,
                this.open);
        Assert.assertEquals(expected2, actual2);

        // Exception case (negative scale factor)
        try {
            interval.scale(-2.);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#DATE_INTERVAL_DATALIST}
     * 
     * @testedMethod {@link AbsoluteDateInterval#getDateList(double)} and
     *               {@link AbsoluteDateInterval#getDateList(int)}
     * 
     * @description unit test for getDateList(double) and getDateList(int) (various cases)
     * 
     * @input constructor data, step and n
     * 
     * @output AbsolutDate list from getDateList
     * 
     * @testPassCriteria AbsolutDate values match the expected values
     * 
     */
    @Test
    public void testGetDateList() {
        final AbsoluteDate lowEnd = new AbsoluteDate();
        final AbsoluteDate upEnd = new AbsoluteDate(lowEnd, 1);
        final double step = 0.5;
        final int n = 3;
        // Case1: [x1,x2]
        AbsoluteDateInterval dateInterval = new AbsoluteDateInterval(this.closed, lowEnd, upEnd,
                this.closed);
        assertNotNull(dateInterval);
        final AbsoluteDate[] ref1 = { lowEnd, lowEnd.shiftedBy(0.5), upEnd };

        List<AbsoluteDate> list1 = dateInterval.getDateList(step);
        List<AbsoluteDate> list2 = dateInterval.getDateList(n);
        assertEquals(3, list2.size());
        for (int i = 0; i < list1.size(); i++) {
            assertEquals(0, ref1[i].durationFrom(list1.get(i)), 1e-4);
            assertEquals(0, ref1[i].durationFrom(list2.get(i)), 1e-4);
        }

        // Case2: ]x1,x2]
        final AbsoluteDate upEnd2 = new AbsoluteDate(lowEnd, 0.9);
        dateInterval = new AbsoluteDateInterval(this.open, lowEnd, upEnd2, this.closed);
        assertNotNull(dateInterval);
        final AbsoluteDate[] ref1a = { lowEnd.shiftedBy(0.5) };
        final AbsoluteDate[] ref1b = { lowEnd.shiftedBy(0.9 / 3), lowEnd.shiftedBy(0.9 * 2. / 3),
                upEnd2 };

        list1 = dateInterval.getDateList(step);
        list2 = dateInterval.getDateList(n);
        assertEquals(3, list2.size());
        for (int i = 0; i < list1.size(); i++) {
            assertEquals(0, ref1a[i].durationFrom(list1.get(i)), 1e-4);
        }
        for (int i = 0; i < list2.size(); i++) {
            assertEquals(0, ref1b[i].durationFrom(list2.get(i)), 1e-4);
        }

        // Case3: [x1,x2[
        dateInterval = new AbsoluteDateInterval(this.closed, lowEnd, upEnd, this.open);
        assertNotNull(dateInterval);
        final AbsoluteDate[] ref3a = { lowEnd, lowEnd.shiftedBy(0.5) };
        final AbsoluteDate[] ref3b = { lowEnd, lowEnd.shiftedBy(1. / 3), lowEnd.shiftedBy(2. / 3) };

        list1 = dateInterval.getDateList(step);
        list2 = dateInterval.getDateList(n);
        assertEquals(3, list2.size());
        for (int i = 0; i < list1.size(); i++) {
            assertEquals(0, ref3a[i].durationFrom(list1.get(i)), 1e-4);
        }
        for (int i = 0; i < list2.size(); i++) {
            assertEquals(0, ref3b[i].durationFrom(list2.get(i)), 1e-4);
        }

        // Case4: ]x1,x2[
        dateInterval = new AbsoluteDateInterval(this.open, lowEnd, upEnd, this.open);
        assertNotNull(dateInterval);
        final AbsoluteDate[] ref4a = { lowEnd.shiftedBy(0.5) };
        final AbsoluteDate[] ref4b = { lowEnd.shiftedBy(1. / 4), lowEnd.shiftedBy(2. / 4),
                lowEnd.shiftedBy(3. / 4) };
        list1 = dateInterval.getDateList(step);
        list2 = dateInterval.getDateList(n);
        assertEquals(3, list2.size());
        for (int i = 0; i < list1.size(); i++) {
            assertEquals(0, ref4a[i].durationFrom(list1.get(i)), 1e-4);
        }
        for (int i = 0; i < list2.size(); i++) {
            assertEquals(0, ref4b[i].durationFrom(list2.get(i)), 1e-4);
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#DATE_INTERVAL_DATALIST}
     * 
     * @testedMethod {@link AbsoluteDateInterval#getDateList(double)} and
     *               {@link AbsoluteDateInterval#getDateList(int)}
     * 
     * @description unit test for getDateList exceptions (inf dates, negative step, n<2)
     * 
     * @input constructor data, step and n
     * 
     * @output MathIllegalArgumentException
     * 
     * @testPassCriteria an exception is thrown
     * 
     */
    @Test
    public void testGetDateListExceptions() {
        final AbsoluteDate lowEnd = new AbsoluteDate();
        final AbsoluteDate upEnd = new AbsoluteDate(lowEnd, 1);
        final AbsoluteDate past = AbsoluteDate.PAST_INFINITY;
        final AbsoluteDate future = AbsoluteDate.FUTURE_INFINITY;

        // Case1: step < 0
        AbsoluteDateInterval dateInterval = new AbsoluteDateInterval(lowEnd, upEnd);
        boolean asExpected = false;
        final double step = -0.5;
        try {
            dateInterval.getDateList(step);
        } catch (final MathIllegalArgumentException e) {
            asExpected = true;
        }
        assertTrue(asExpected);

        // Case2: n < 2
        asExpected = false;
        final int n = 0;
        try {
            dateInterval.getDateList(n);
        } catch (final MathIllegalArgumentException e) {
            asExpected = true;
        }
        assertTrue(asExpected);

        // Case3: infinity date
        dateInterval = new AbsoluteDateInterval(past, future);
        asExpected = false;
        try {
            dateInterval.getDateList(0.5);
        } catch (final MathIllegalArgumentException e) {
            asExpected = true;
        }
        assertTrue(asExpected);
    }

    /**
     * Setup for AbsoluteDate tests.
     * 
     * @throws PatriusException
     *         if problem with data
     */
    @Before
    public void setUp() throws PatriusException {
        Utils.setDataRoot("regular-dataCNES-2003");
        FramesFactory.setConfiguration(fr.cnes.sirius.patrius.Utils.getIERS2003Configuration(true));
    }
}
