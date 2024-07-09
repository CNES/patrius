/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 */
/*
 *
 * HISTORY
* VERSION:4.8:DM:DM-2922:15/11/2021:[PATRIUS] suppression de l'utilisation de la reflexion Java dans patrius 
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:202:19/03/2014:Added test case for last known leap second UTC display
 * VERSION::FA:255:13/10/2014:header correction
 * VERSION::FA:306:12/11/2014:coverage
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.time;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.math.random.RandomGenerator;
import fr.cnes.sirius.patrius.math.random.Well1024a;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class UTCScaleTest {

    /**
     * Test added for FT 202 where a leap second wasn't correctly taken into account
     * 
     * @throws PatriusException
     */
    @Test
    public void lastStepTest() {

        // TAI : 2012-07-01T00:00:32.000, TUC : 2012-06-30T23:59:58.000
        // TAI : 2012-07-01T00:00:33.000, TUC : 2012-06-30T23:59:59.000
        // TAI : 2012-07-01T00:00:34.000, TUC : 2012-07-01T00:00:00.000
        // TAI : 2012-07-01T00:00:35.000, TUC : 2012-07-01T00:00:00.000
        // TAI : 2012-07-01T00:00:36.000, TUC : 2012-07-01T00:00:01.000
        // TAI : 2012-07-01T00:00:37.000, TUC : 2012-07-01T00:00:02.000

        // third line should be
        // TAI : 2012-07-01T00:00:34.000, TUC : 2012-06-30T23:59:60.000

        final AbsoluteDate date = new AbsoluteDate("2012-07-01T00:00:34.000", TimeScalesFactory.getTAI());

        final String expected = "2012-06-30T23:59:60.000";
        final String actual = date.toString(this.utc);

        final String errorMsg = "expected " + expected + ", but was " + actual;

        Assert.assertTrue(errorMsg, expected.contentEquals(actual));

    }

    /**
     * Test added for the 2012 leap second.
     */
    @Test
    public void testLeap2012() {
        final AbsoluteDate leapDate =
            new AbsoluteDate(new DateComponents(2012, 07, 01), TimeComponents.H00, this.utc);
        final AbsoluteDate d1 = leapDate.shiftedBy(-1);
        final AbsoluteDate d2 = leapDate.shiftedBy(+1);
        Assert.assertEquals(2.0, d2.durationFrom(d1), 1.0e-10);

        final AbsoluteDate d3 = new AbsoluteDate(new DateComponents(2012, 06, 30),
            new TimeComponents(23, 59, 59),
            this.utc);
        final AbsoluteDate d4 = new AbsoluteDate(new DateComponents(2012, 07, 01),
            new TimeComponents(00, 00, 01),
            this.utc);
        Assert.assertEquals(3.0, d4.durationFrom(d3), 1.0e-10);
    }

    @Test
    public void testNoLeap() {
        Assert.assertEquals("UTC", this.utc.toString());
        final AbsoluteDate d1 = new AbsoluteDate(new DateComponents(1999, 12, 31),
            new TimeComponents(23, 59, 59),
            this.utc);
        final AbsoluteDate d2 = new AbsoluteDate(new DateComponents(2000, 01, 01),
            new TimeComponents(00, 00, 01),
            this.utc);
        Assert.assertEquals(2.0, d2.durationFrom(d1), 1.0e-10);
    }

    @Test
    public void testLeap2006() {
        final AbsoluteDate leapDate =
            new AbsoluteDate(new DateComponents(2006, 01, 01), TimeComponents.H00, this.utc);
        final AbsoluteDate d1 = leapDate.shiftedBy(-1);
        final AbsoluteDate d2 = leapDate.shiftedBy(+1);
        Assert.assertEquals(2.0, d2.durationFrom(d1), 1.0e-10);

        final AbsoluteDate d3 = new AbsoluteDate(new DateComponents(2005, 12, 31),
            new TimeComponents(23, 59, 59),
            this.utc);
        final AbsoluteDate d4 = new AbsoluteDate(new DateComponents(2006, 01, 01),
            new TimeComponents(00, 00, 01),
            this.utc);
        Assert.assertEquals(3.0, d4.durationFrom(d3), 1.0e-10);
    }

    @Test
    public void testDuringLeap() {
        AbsoluteDate d = new AbsoluteDate(new DateComponents(1983, 06, 30),
            new TimeComponents(23, 59, 59),
            this.utc);
        Assert.assertEquals("1983-06-30T23:59:59.000", d.toString(this.utc));
        d = d.shiftedBy(0.251);
        Assert.assertEquals("1983-06-30T23:59:59.251", d.toString(this.utc));
        d = d.shiftedBy(0.251);
        Assert.assertEquals("1983-06-30T23:59:59.502", d.toString(this.utc));
        d = d.shiftedBy(0.251);
        Assert.assertEquals("1983-06-30T23:59:59.753", d.toString(this.utc));
        d = d.shiftedBy(0.251);
        Assert.assertEquals("1983-06-30T23:59:60.004", d.toString(this.utc));
        d = d.shiftedBy(0.251);
        Assert.assertEquals("1983-06-30T23:59:60.255", d.toString(this.utc));
        d = d.shiftedBy(0.251);
        Assert.assertEquals("1983-06-30T23:59:60.506", d.toString(this.utc));
        d = d.shiftedBy(0.251);
        Assert.assertEquals("1983-06-30T23:59:60.757", d.toString(this.utc));
        d = d.shiftedBy(0.251);
        Assert.assertEquals("1983-07-01T00:00:00.008", d.toString(this.utc));
    }

    @Test
    public void testSymmetry() {
        final TimeScale scale = TimeScalesFactory.getGPS();
        for (double dt = -10000; dt < 10000; dt += 123.456789) {
            final AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(dt * Constants.JULIAN_DAY);
            final double dt1 = scale.offsetFromTAI(date);
            final DateTimeComponents components = date.getComponents(scale);
            final double dt2 = scale.offsetToTAI(components.getDate(), components.getTime());
            Assert.assertEquals(0.0, dt1 + dt2, 1.0e-10);
        }
    }

    @Test
    public void testOffsets() {

        // we arbitrary put UTC == TAI before 1961-01-01
        this.checkOffset(1950, 1, 1, 0);

        // excerpt from UTC-TAI.history file:
        // 1961 Jan. 1 - 1961 Aug. 1 1.422 818 0s + (MJD - 37 300) x 0.001 296s
        // Aug. 1 - 1962 Jan. 1 1.372 818 0s + ""
        // 1962 Jan. 1 - 1963 Nov. 1 1.845 858 0s + (MJD - 37 665) x 0.001 123 2s
        // 1963 Nov. 1 - 1964 Jan. 1 1.945 858 0s + ""
        // 1964 Jan. 1 - April 1 3.240 130 0s + (MJD - 38 761) x 0.001 296s
        // April 1 - Sept. 1 3.340 130 0s + ""
        // Sept. 1 - 1965 Jan. 1 3.440 130 0s + ""
        // 1965 Jan. 1 - March 1 3.540 130 0s + ""
        // March 1 - Jul. 1 3.640 130 0s + ""
        // Jul. 1 - Sept. 1 3.740 130 0s + ""
        // Sept. 1 - 1966 Jan. 1 3.840 130 0s + ""
        // 1966 Jan. 1 - 1968 Feb. 1 4.313 170 0s + (MJD - 39 126) x 0.002 592s
        // 1968 Feb. 1 - 1972 Jan. 1 4.213 170 0s + ""
        this.checkOffset(1961, 1, 2, -(1.422818 + 1 * 0.001296)); // MJD 37300 + 1
        this.checkOffset(1961, 8, 2, -(1.372818 + 213 * 0.001296)); // MJD 37300 + 213
        this.checkOffset(1962, 1, 2, -(1.845858 + 1 * 0.0011232)); // MJD 37665 + 1
        this.checkOffset(1963, 11, 2, -(1.945858 + 670 * 0.0011232)); // MJD 37665 + 670
        this.checkOffset(1964, 1, 2, -(3.240130 - 365 * 0.001296)); // MJD 38761 - 365
        this.checkOffset(1964, 4, 2, -(3.340130 - 274 * 0.001296)); // MJD 38761 - 274
        this.checkOffset(1964, 9, 2, -(3.440130 - 121 * 0.001296)); // MJD 38761 - 121
        this.checkOffset(1965, 1, 2, -(3.540130 + 1 * 0.001296)); // MJD 38761 + 1
        this.checkOffset(1965, 3, 2, -(3.640130 + 60 * 0.001296)); // MJD 38761 + 60
        this.checkOffset(1965, 7, 2, -(3.740130 + 182 * 0.001296)); // MJD 38761 + 182
        this.checkOffset(1965, 9, 2, -(3.840130 + 244 * 0.001296)); // MJD 38761 + 244
        this.checkOffset(1966, 1, 2, -(4.313170 + 1 * 0.002592)); // MJD 39126 + 1
        this.checkOffset(1968, 2, 2, -(4.213170 + 762 * 0.002592)); // MJD 39126 + 762

        // since 1972-01-01, offsets are only whole seconds
        this.checkOffset(1972, 3, 5, -10);
        this.checkOffset(1972, 7, 14, -11);
        this.checkOffset(1979, 12, 31, -18);
        this.checkOffset(1980, 1, 22, -19);
        this.checkOffset(2006, 7, 7, -33);

    }

    private void checkOffset(final int year, final int month, final int day, final double offset) {
        final AbsoluteDate date = new AbsoluteDate(year, month, day, this.utc);
        Assert.assertEquals(offset, this.utc.offsetFromTAI(date), 1.0e-10);
    }

    @Test
    public void testCreatingInLeapDate() {
        AbsoluteDate previous = null;
        final double step = 0.0625;
        for (double seconds = 59.0; seconds < 61.0; seconds += step) {
            final AbsoluteDate date = new AbsoluteDate(2008, 12, 31, 23, 59, seconds, this.utc);
            if (previous != null) {
                Assert.assertEquals(step, date.durationFrom(previous), 1.0e-12);
            }
            previous = date;
        }
        final AbsoluteDate ad0 = new AbsoluteDate("2008-12-31T23:59:60", this.utc);
        Assert.assertTrue(ad0.toString(this.utc).startsWith("2008-12-31T23:59:"));
        final AbsoluteDate ad1 = new AbsoluteDate("2008-12-31T23:59:59", this.utc).shiftedBy(1);
        Assert.assertEquals(0, ad1.durationFrom(ad0), 1.0e-15);
        Assert.assertEquals(1, new AbsoluteDate("2009-01-01T00:00:00", this.utc).durationFrom(ad0), 1.0e-15);
        Assert.assertEquals(2, new AbsoluteDate("2009-01-01T00:00:01", this.utc).durationFrom(ad0), 1.0e-15);
    }

    @Test
    public void testDisplayDuringLeap() throws PatriusException {
        final AbsoluteDate t0 = this.utc.getLastKnownLeapSecond().shiftedBy(-1.0);
        for (double dt = 0.0; dt < 3.0; dt += 0.375) {
            final AbsoluteDate t = t0.shiftedBy(dt);
            final double seconds = t.getComponents(this.utc).getTime().getSecond();
            if (dt < 2.0) {
                Assert.assertEquals(dt + 59.0, seconds, 1.0e-12);
            } else {
                Assert.assertEquals(dt - 2.0, seconds, 1.0e-12);
            }
        }
    }

    @Test
    public void testMultithreading() {

        // generate reference offsets using a single thread
        final RandomGenerator random = new Well1024a(6392073424l);
        final List<AbsoluteDate> datesList = new ArrayList<AbsoluteDate>();
        final List<Double> offsetsList = new ArrayList<Double>();
        final AbsoluteDate reference = this.utc.getFirstKnownLeapSecond().shiftedBy(-Constants.JULIAN_YEAR);
        final double testRange = this.utc.getLastKnownLeapSecond().durationFrom(reference) + Constants.JULIAN_YEAR;
        for (int i = 0; i < 10000; ++i) {
            final AbsoluteDate randomDate = reference.shiftedBy(random.nextDouble() * testRange);
            datesList.add(randomDate);
            offsetsList.add(this.utc.offsetFromTAI(randomDate));
        }

        // check the offsets in multi-threaded mode
        final ExecutorService executorService = Executors.newFixedThreadPool(100);

        for (int i = 0; i < datesList.size(); ++i) {
            final AbsoluteDate date = datesList.get(i);
            final double offset = offsetsList.get(i);
            executorService.execute(new Runnable(){
                @Override
                public void run() {
                    Assert.assertEquals(offset, UTCScaleTest.this.utc.offsetFromTAI(date), 1.0e-12);
                }
            });
        }

        try {
            executorService.shutdown();
            executorService.awaitTermination(3, TimeUnit.SECONDS);
        } catch (final InterruptedException ie) {
            Assert.fail(ie.getLocalizedMessage());
        }

    }

    @Test
    public void testIssue89() throws PatriusException {
        final AbsoluteDate firstDayLastLeap = this.utc.getLastKnownLeapSecond().shiftedBy(10.0);
        final AbsoluteDate rebuilt = new AbsoluteDate(firstDayLastLeap.toString(this.utc), this.utc);
        Assert.assertEquals(0.0, rebuilt.durationFrom(firstDayLastLeap), 1.0e-12);
    }

    @Test
    public void testOffsetToTAIBeforeFirstLeapSecond() throws PatriusException {
        final TimeScale scale = TimeScalesFactory.getUTC();
        // time before first leap second
        final DateComponents dateComponents = new DateComponents(1950, 1, 1);
        final double actual = scale.offsetToTAI(dateComponents, TimeComponents.H00);
        Assert.assertEquals(0.0, actual, 1.0e-10);
    }

    @Test
    public void testCoverage() throws PatriusException {
        final UTCScale scale = TimeScalesFactory.getUTC();
        new AbsoluteDate();
        Assert.assertEquals(0, scale.getLeap(AbsoluteDate.JULIAN_EPOCH), 0.0);
    }

    @Before
    public void setUp() throws PatriusException {
        Utils.setDataRoot("regular-data");
        this.utc = TimeScalesFactory.getUTC();
    }

    @After
    public void tearDown() {
        this.utc = null;
    }

    private UTCScale utc;

}
