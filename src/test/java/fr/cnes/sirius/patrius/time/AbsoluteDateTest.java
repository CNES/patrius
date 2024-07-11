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
 * VERSION:4.9.4:FA:FA-107:08/08/2023:[PATRIUS] Correction du problème de 
 *          qualité numérique dans AbsoluteDate.getComponents sur toutes les branches 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-2962:15/11/2021:[PATRIUS] Precision numerique lors du ShiftedBy avec TimeScale 
 * VERSION:4.8:FA:FA-2956:15/11/2021:[PATRIUS] Temps de propagation non implemente pour certains evenements 
 * VERSION:4.8:DM:DM-2967:15/11/2021:[PATRIUS] corriger les utilisations de java.util.Date 
 * VERSION:4.7:DM:DM-2801:18/05/2021:Suppression des classes et methodes depreciees suite au refactoring des slews
 * VERSION:4.7:FA:FA-2887:18/05/2021:Probleme de micro-pas dans la propagation
 * VERSION:4.7:DM:DM-2767:18/05/2021:Evolutions et corrections diverses 
 * VERSION:4.7:DM:DM-2683:18/05/2021:Methode shiftedBy (AbsoluteDate) avec echelles de temps
 * VERSION:4.7:DM:DM-2647:18/05/2021:constructeur de AbsoluteDate avec TAI par defaut 
 * VERSION:4.6:FA:FA-2608:27/01/2021:Mauvaise date de reference pour le Galileo System Time
 * VERSION:4.5:DM:DM-2431:27/05/2020:Date 
 * VERSION:4.5:DM:DM-2340:27/05/2020:Conversion AbsoluteDate - cjd 
 * VERSION:4.4:FA:FA-2121:04/10/2019:[PATRIUS] precision de la methode shiftedBy de AbsoluteDate
 * VERSION:4.3:FA:FA-1978:15/05/2019:Anomalie calcul orientation corps celeste (UAI)
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:158:24/10/2013:Changed AbsoluteDate.toString to return date in TAI
 * VERSION::DM:256:01/08/2014:Changed AbsoluteDate.toString to take into account seconds precision
 * VERSION::FA:367:21/11/2014:Corrected END-HISTORY key
 * VERSION::FA:363:03/12/2014:Solved toString round date bug
 * VERSION::FA:397:20/02/2015:Corrected wrong definition of AbsoluteDate.JAVA_EPOCH (1/1/1970 UTC)
 * VERSION::DM:524:10/03/2016:serialization test
 * VERSION::FA:712:23/12/2016:Raise IllegalArgumentException if date has a bad format instead of NullPointerException
 * VERSION::FA:1312:15/11/2017:Improve TimeComponents accuracy
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.time;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.TestUtils;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class AbsoluteDateTest {
    
    @Test
    public void testSmallOffsetGetComponents() {
        final AbsoluteDate date = new AbsoluteDate(744724800, -4.7580986769649594E-17);
        final AbsoluteDate date2 = new AbsoluteDate(744724800, 4.7580986769649594E-17);
        Assert.assertEquals(date.getComponents(TimeScalesFactory.getTAI()).toString(),
            date2.getComponents(TimeScalesFactory.getTAI()).toString());
    }

    @Test
    public void testAccuracyBasic() {
        final TimeScale tai = TimeScalesFactory.getTAI();
        final double sec = 0.281;
        final AbsoluteDate t = new AbsoluteDate(2010, 6, 21, 18, 42, sec, tai);
        final double res = t.shiftedBy(1038434.3498579).shiftedBy(-1038434.3498579).durationFrom(t);
        Assert.assertEquals(0., res, MathLib.ulp(sec));
    }

    @Test
    public void testAccuracyTAI() {
        final TimeScale tai = TimeScalesFactory.getTAI();
        final double sec = 0.281;
        final AbsoluteDate t = new AbsoluteDate(2010, 6, 21, 18, 42, sec, tai);
        final double res = t.shiftedBy(1038434.3498579, tai).shiftedBy(-1038434.3498579, tai).durationFrom(t, tai);
        Assert.assertEquals(0., res, MathLib.ulp(sec));
    }

    @Test
    public void testAccuracyTDB() {
        final TimeScale tdb = TimeScalesFactory.getTDB();
        final double sec = 0.281;
        final AbsoluteDate t = new AbsoluteDate(2010, 6, 21, 18, 42, sec, tdb);
        final double res = t.shiftedBy(1038434.3498579, tdb).shiftedBy(-1038434.3498579, tdb).durationFrom(t, tdb);
        Assert.assertEquals(0., res, 1E-14);
    }

    @Test
    public void testShiftedBy0() {
        final AbsoluteDate date = new AbsoluteDate(668843107, -5.551115123125783E-17);
        final AbsoluteDate date2 = date.shiftedBy(0.);
        Assert.assertEquals(date.getEpoch(), date2.getEpoch());
        Assert.assertEquals(date.getOffset(), date2.getOffset(), 0.);
    }

    @Test
    public void testFA2121() {
        final AbsoluteDate start = new AbsoluteDate(690679842, 0.443); 
        final AbsoluteDate expectedEnd = new AbsoluteDate(690681172, 0.92); 
        final double duration = expectedEnd.durationFrom(start);
        final double actualOffset = start.shiftedBy(duration).getOffset();
//        Assert.assertEquals(expectedEnd.getOffset(), actualOffset, 0.);
        System.out.println("Expected offset : " + expectedEnd.getOffset() + "  Actual offset : " + actualOffset);
    }

    @Test
    public void testFA2121_2() {
        final AbsoluteDate start = new AbsoluteDate(3888, 0.9756228409693998); 
        final AbsoluteDate expectedEnd = new AbsoluteDate(3888, 0.9756228409693999); 
        final double duration = expectedEnd.durationFrom(start);
        final double actualOffset = start.shiftedBy(duration).getOffset();
        System.out.println("Expected offset : " + expectedEnd.getOffset() + "  Actual offset : " + actualOffset);
    }
    
    @Test
    public void testCNESJulianDateConversion() throws PatriusException {
        // To CNES Julian Date
        // Reference Celestlab
        final AbsoluteDate date = new AbsoluteDate(2020, 02, 19, TimeScalesFactory.getUTC());
        Assert.assertEquals(25616, date.toCNESJulianDate(TimeScalesFactory.getUTC()), 0.);
        final AbsoluteDate date2 = new AbsoluteDate(2020, 02, 19, TimeScalesFactory.getTAI());
        Assert.assertEquals(25616, date2.toCNESJulianDate(TimeScalesFactory.getTAI()), 0.);
        
        // From CNES Julian Date
        // Reference Celestlab
        final AbsoluteDate date4 = new AbsoluteDate(25616, TimeScalesFactory.getUTC());
        Assert.assertEquals("2020-02-19T00:00:00.000", date4.toString(TimeScalesFactory.getUTC()));
    }

    /**
     * Test shiftedBy method with boundary not to overpass.
     */
    @Test
    public void testShiftedByWithBound() {
        final AbsoluteDate start = new AbsoluteDate(690679842, 0.443); 
        final AbsoluteDate expectedEnd = new AbsoluteDate(690681172, 0.92); 
        final double duration = expectedEnd.durationFrom(start);
        // Old method, wrong result
        final double actualOffset1 = start.shiftedBy(duration).getOffset();
        Assert.assertFalse(expectedEnd.getOffset() == actualOffset1);
        // New method, right result
        final double actualOffset2 = start.shiftedBy(duration, expectedEnd, true).getOffset();
        Assert.assertTrue(expectedEnd.getOffset() == actualOffset2);
    }

    /**
     * Test shiftedBy method with time scale.
     */
    @Test
    public void testShiftedByWithTimeScale() {
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final double dt = 100.;
        final TimeScale timescale = TimeScalesFactory.getTDB();
        final AbsoluteDate expected = new AbsoluteDate(date, dt, timescale);
        final AbsoluteDate actual = date.shiftedBy(dt, timescale);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testStandardEpoch() throws PatriusException {
        final TimeScale utc = TimeScalesFactory.getUTC();
        final TimeScale tai = TimeScalesFactory.getTAI();
        final TimeScale tt = TimeScalesFactory.getTT();
        // toDate
        Assert.assertEquals(-210866760000000l, AbsoluteDate.JULIAN_EPOCH.toDate(tt).getTime());
        Assert.assertEquals(-3506716800000l, AbsoluteDate.MODIFIED_JULIAN_EPOCH.toDate(tt).getTime());
        Assert.assertEquals(-631152000000l, AbsoluteDate.FIFTIES_EPOCH_TT.toDate(tt).getTime());
        Assert.assertEquals(-631152000000l, AbsoluteDate.FIFTIES_EPOCH_TAI.toDate(tai).getTime());
        Assert.assertEquals(-631152000000l, AbsoluteDate.FIFTIES_EPOCH_UTC.toDate(utc).getTime());
        Assert.assertEquals(-378691200000l, AbsoluteDate.CCSDS_EPOCH.toDate(tai).getTime());
        Assert.assertEquals(935280019000l, AbsoluteDate.GALILEO_EPOCH.toDate(tai).getTime());
        Assert.assertEquals(315964819000l, AbsoluteDate.GPS_EPOCH.toDate(tai).getTime());
        Assert.assertEquals(946728000000l, AbsoluteDate.J2000_EPOCH.toDate(tt).getTime());
        // toLocalDateTime
        Assert.assertEquals(-210866760000000l, AbsoluteDate.JULIAN_EPOCH.toLocalDateTime(tt).toInstant(ZoneOffset.UTC).toEpochMilli());
        Assert.assertEquals(-3506716800000l, AbsoluteDate.MODIFIED_JULIAN_EPOCH.toLocalDateTime(tt).toInstant(ZoneOffset.UTC).toEpochMilli());
        Assert.assertEquals(-631152000000l, AbsoluteDate.FIFTIES_EPOCH_TT.toLocalDateTime(tt).toInstant(ZoneOffset.UTC).toEpochMilli());
        Assert.assertEquals(-631152000000l, AbsoluteDate.FIFTIES_EPOCH_TAI.toLocalDateTime(tai).toInstant(ZoneOffset.UTC).toEpochMilli());
        Assert.assertEquals(-631152000000l, AbsoluteDate.FIFTIES_EPOCH_UTC.toLocalDateTime(utc).toInstant(ZoneOffset.UTC).toEpochMilli());
        Assert.assertEquals(-378691200000l, AbsoluteDate.CCSDS_EPOCH.toLocalDateTime(tai).toInstant(ZoneOffset.UTC).toEpochMilli());
        Assert.assertEquals(935280019000l, AbsoluteDate.GALILEO_EPOCH.toLocalDateTime(tai).toInstant(ZoneOffset.UTC).toEpochMilli());
        Assert.assertEquals(315964819000l, AbsoluteDate.GPS_EPOCH.toLocalDateTime(tai).toInstant(ZoneOffset.UTC).toEpochMilli());
        Assert.assertEquals(946728000000l, AbsoluteDate.J2000_EPOCH.toLocalDateTime(tt).toInstant(ZoneOffset.UTC).toEpochMilli());
    }

    @Test
    public void testStandardEpochStrings() throws PatriusException {
        Assert.assertEquals("-4712-01-01T12:00:00.000",
            AbsoluteDate.JULIAN_EPOCH.toString(TimeScalesFactory.getTT()));
        Assert.assertEquals("1858-11-17T00:00:00.000",
            AbsoluteDate.MODIFIED_JULIAN_EPOCH.toString(TimeScalesFactory.getTT()));
        Assert.assertEquals("1950-01-01T00:00:00.000",
            AbsoluteDate.FIFTIES_EPOCH_TT.toString(TimeScalesFactory.getTT()));
        Assert.assertEquals("1950-01-01T00:00:00.000",
            AbsoluteDate.FIFTIES_EPOCH_TAI.toString(TimeScalesFactory.getTAI()));
        Assert.assertEquals("1950-01-01T00:00:00.000",
            AbsoluteDate.FIFTIES_EPOCH_UTC.toString(TimeScalesFactory.getUTC()));
        Assert.assertEquals("1958-01-01T00:00:00.000",
            AbsoluteDate.CCSDS_EPOCH.toString(TimeScalesFactory.getTAI()));
        // FA 2608 : Correction of the wrong definition of GALILEO_EPOCH.
        // GALILEO_EPOCH : 1999-08-21T23:59:47 UTC | 1999-08-22T00:00:19 TAI
        Assert.assertEquals("1999-08-21T23:59:47.000",
            AbsoluteDate.GALILEO_EPOCH.toString(TimeScalesFactory.getUTC()));
        Assert.assertEquals("1980-01-06T00:00:00.000",
            AbsoluteDate.GPS_EPOCH.toString(TimeScalesFactory.getUTC()));
        Assert.assertEquals("2000-01-01T12:00:00.000",
            AbsoluteDate.J2000_EPOCH.toString(TimeScalesFactory.getTT()));
        // FA 397 : Correction of the wrong definition of JAVA_EPOCH.
        // JAVA_EPOCH : 1970-01-01T00:00:00 UTC
        // 1970-01-01T00:00:08 TAI
        Assert.assertEquals("1970-01-01T00:00:08.000",
            AbsoluteDate.JAVA_EPOCH.toString(TimeScalesFactory.getTAI()));
        Assert.assertEquals("1970-01-01T00:00:00.000",
            AbsoluteDate.JAVA_EPOCH.toString(TimeScalesFactory.getUTC()));
    }

    @Test
    public void testInfinityStrings() throws PatriusException {
        Assert.assertEquals("Past infinity",
            AbsoluteDate.PAST_INFINITY.toString());
        Assert.assertEquals("Future infinity",
            AbsoluteDate.FUTURE_INFINITY.toString());
    }

    @Test
    public void testParse() throws PatriusException {
        Assert.assertEquals(AbsoluteDate.MODIFIED_JULIAN_EPOCH,
            new AbsoluteDate("1858-W46-3", TimeScalesFactory.getTT()));
        Assert.assertEquals(AbsoluteDate.JULIAN_EPOCH,
            new AbsoluteDate("-4712-01-01T12:00:00.000", TimeScalesFactory.getTT()));
        Assert.assertEquals(AbsoluteDate.FIFTIES_EPOCH_TT,
            new AbsoluteDate("1950-01-01", TimeScalesFactory.getTT()));
        Assert.assertEquals(AbsoluteDate.FIFTIES_EPOCH_TAI,
            new AbsoluteDate("1950-01-01", TimeScalesFactory.getTAI()));
        Assert.assertEquals(AbsoluteDate.FIFTIES_EPOCH_UTC,
            new AbsoluteDate("1950-01-01", TimeScalesFactory.getUTC()));
        Assert.assertEquals(AbsoluteDate.CCSDS_EPOCH,
            new AbsoluteDate("1958-001", TimeScalesFactory.getTAI()));
    }

    @Test
    public void testOutput() throws PatriusException {
        final TimeScale utc = TimeScalesFactory.getUTC();
        final TimeScale tai = TimeScalesFactory.getTAI();
        final TimeScale tt = TimeScalesFactory.getTT();
        Assert.assertEquals("1950-01-01T01:01:01.000",
            AbsoluteDate.FIFTIES_EPOCH_TT.shiftedBy(3661.0).toString(tt));
        Assert.assertEquals("1950-01-01T01:01:01.000",
            AbsoluteDate.FIFTIES_EPOCH_TAI.shiftedBy(3661.0).toString(tai));
        Assert.assertEquals("1950-01-01T01:01:01.000",
            AbsoluteDate.FIFTIES_EPOCH_UTC.shiftedBy(3661.0).toString(utc));
        Assert.assertEquals("2000-01-01T13:01:01.000",
            AbsoluteDate.J2000_EPOCH.shiftedBy(3661.0).toString(tt));
    }

    @Test
    public void testJ2000() {
        Assert.assertEquals("2000-01-01T12:00:00.000",
            AbsoluteDate.J2000_EPOCH.toString(TimeScalesFactory.getTT()));
        Assert.assertEquals("2000-01-01T11:59:27.816",
            AbsoluteDate.J2000_EPOCH.toString(TimeScalesFactory.getTAI()));
        Assert.assertEquals("2000-01-01T11:58:55.816",
            AbsoluteDate.J2000_EPOCH.toString(this.utc));
    }

    @Test
    public void testFraction() {
        final AbsoluteDate d =
            new AbsoluteDate(new DateComponents(2000, 01, 01), new TimeComponents(11, 59, 27.816),
                TimeScalesFactory.getTAI());
        Assert.assertEquals(0, d.durationFrom(AbsoluteDate.J2000_EPOCH), 1.0e-10);
    }

    @Test
    public void testScalesOffset() {
        final AbsoluteDate date = new AbsoluteDate(new DateComponents(2006, 02, 24),
            new TimeComponents(15, 38, 00),
            this.utc);
        Assert.assertEquals(33,
            date.timeScalesOffset(TimeScalesFactory.getTAI(), this.utc),
            1.0e-10);
    }

    @Test
    public void testTimeDilatation() {
        final TimeScale TDB = TimeScalesFactory.getTDB();
        final AbsoluteDate D1 = new AbsoluteDate(2000, 1, 1, 12, 0, 0, TDB);
        final AbsoluteDate D2 = new AbsoluteDate(2000, 1, 11, 12, 0, 0, TDB);
        Assert.assertEquals(10. * 86400., D2.durationFrom(D1, TDB), 0);
    }

    @Test
    public void testUTC() throws PatriusException {
        final AbsoluteDate date = new AbsoluteDate(new DateComponents(2002, 01, 01),
            new TimeComponents(00, 00, 01),
            this.utc);
        Assert.assertEquals("2002-01-01T00:00:01.000", date.toString(TimeScalesFactory.getUTC()));
        Utils.setDataRoot("no-data");
        try {
            Assert.assertEquals("2002-01-01T00:00:01.000", date.toString(TimeScalesFactory.getUTC()));
            Assert.fail("an exception should have been thrown");
        } catch (final PatriusException e) {
            // expected !
        }
        Utils.setDataRoot("regular-data");
    }

    @Test
    public void test1970() throws PatriusException {
        final AbsoluteDate date = new AbsoluteDate(new Date(0l), this.utc);
        Assert.assertEquals("1970-01-01T00:00:00.000", date.toString(TimeScalesFactory.getUTC()));

        final LocalDateTime ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(0l), ZoneOffset.UTC);
        final AbsoluteDate date2 = new AbsoluteDate(ldt, this.utc);
        Assert.assertEquals("1970-01-01T00:00:00.000", date2.toString(TimeScalesFactory.getUTC()));
    }

    @Test
    public void testUtcGpsOffset() {
        final AbsoluteDate date1 = new AbsoluteDate(new DateComponents(2005, 8, 9),
            new TimeComponents(16, 31, 17),
            this.utc);
        final AbsoluteDate date2 = new AbsoluteDate(new DateComponents(2006, 8, 9),
            new TimeComponents(16, 31, 17),
            this.utc);
        final AbsoluteDate dateRef = new AbsoluteDate(new DateComponents(1980, 1, 6),
            TimeComponents.H00,
            this.utc);

        // 13 seconds offset between GPS time and UTC in 2005
        long noLeapGap = ((9347 * 24 + 16) * 60 + 31) * 60 + 17;
        long realGap = (long) date1.durationFrom(dateRef);
        Assert.assertEquals(13l, realGap - noLeapGap);

        // 14 seconds offset between GPS time and UTC in 2006
        noLeapGap = ((9712 * 24 + 16) * 60 + 31) * 60 + 17;
        realGap = (long) date2.durationFrom(dateRef);
        Assert.assertEquals(14l, realGap - noLeapGap);

    }

    @Test
    public void testGpsDate() {
        final AbsoluteDate date = AbsoluteDate.createGPSDate(1387, 318677000.0);
        final AbsoluteDate ref = new AbsoluteDate(new DateComponents(2006, 8, 9),
            new TimeComponents(16, 31, 03),
            this.utc);
        Assert.assertEquals(0, date.durationFrom(ref), 1.0e-15);
        
        // Test the methods that reconvert a GPS date into week number and milliseconds in week
        final double miliweek = date.getMilliInWeek();
        final int weeknum = date.getWeekNumber();
        Assert.assertEquals(318677000.0, miliweek, 1.0e-15);
        Assert.assertEquals(1387, weeknum, 1.0e-15);

        final AbsoluteDate date2 = AbsoluteDate.createGPSDate(101, 12345600);
        Assert.assertEquals(101, date2.getWeekNumber(), 0);
        Assert.assertEquals(12345600, date2.getMilliInWeek(), 0);
    }
    
    @Test
    public void testGetSecondsInDay() {
        final AbsoluteDate date1 = new AbsoluteDate(new DateComponents(2006, 8, 9),
                new TimeComponents(0, 0, 0), this.utc);
        Assert.assertEquals(0.0, date1.getSecondsInDay(utc), 1.0e-10);
        
        final AbsoluteDate date2 = new AbsoluteDate(new DateComponents(2006, 8, 9),
                new TimeComponents(6, 0, 0), this.utc);
        Assert.assertEquals(21600.0, date2.getSecondsInDay(utc), 1.0e-10);
        
        final AbsoluteDate date3 = new AbsoluteDate(new DateComponents(2006, 8, 9),
                new TimeComponents(12, 0, 0), this.utc);
        Assert.assertEquals(43200.0, date3.getSecondsInDay(utc), 1.0e-10);
    }

    @Test
    public void testOffsets() {
        final TimeScale tai = TimeScalesFactory.getTAI();
        final AbsoluteDate leapStartUTC = new AbsoluteDate(1976, 12, 31, 23, 59, 59, this.utc);
        final AbsoluteDate leapEndUTC = new AbsoluteDate(1977, 1, 1, 0, 0, 0, this.utc);
        final AbsoluteDate leapStartTAI = new AbsoluteDate(1977, 1, 1, 0, 0, 14, tai);
        final AbsoluteDate leapEndTAI = new AbsoluteDate(1977, 1, 1, 0, 0, 16, tai);
        Assert.assertEquals(leapStartUTC, leapStartTAI);
        Assert.assertEquals(leapEndUTC, leapEndTAI);
        Assert.assertEquals(1, leapEndUTC.offsetFrom(leapStartUTC, this.utc), 1.0e-10);
        Assert.assertEquals(1, leapEndTAI.offsetFrom(leapStartTAI, this.utc), 1.0e-10);
        Assert.assertEquals(2, leapEndUTC.offsetFrom(leapStartUTC, tai), 1.0e-10);
        Assert.assertEquals(2, leapEndTAI.offsetFrom(leapStartTAI, tai), 1.0e-10);
        Assert.assertEquals(2, leapEndUTC.durationFrom(leapStartUTC), 1.0e-10);
        Assert.assertEquals(2, leapEndTAI.durationFrom(leapStartTAI), 1.0e-10);
    }

    @Test
    public void testBeforeAndAfterLeap() {
        final TimeScale tai = TimeScalesFactory.getTAI();
        final AbsoluteDate leapStart = new AbsoluteDate(1977, 1, 1, 0, 0, 14, tai);
        final AbsoluteDate leapEnd = new AbsoluteDate(1977, 1, 1, 0, 0, 16, tai);
        for (int i = -10; i < 10; ++i) {
            final double dt = 1.1 * (2 * i - 1);
            final AbsoluteDate d1 = leapStart.shiftedBy(dt);
            final AbsoluteDate d2 = new AbsoluteDate(leapStart, dt, tai);
            final AbsoluteDate d3 = new AbsoluteDate(leapStart, dt, this.utc);
            final AbsoluteDate d4 = new AbsoluteDate(leapEnd, dt, tai);
            final AbsoluteDate d5 = new AbsoluteDate(leapEnd, dt, this.utc);
            Assert.assertTrue(MathLib.abs(d1.durationFrom(d2)) < 1.0e-10);
            if (dt < 0) {
                Assert.assertTrue(MathLib.abs(d2.durationFrom(d3)) < 1.0e-10);
                Assert.assertTrue(d4.durationFrom(d5) > (1.0 - 1.0e-10));
            } else {
                Assert.assertTrue(d2.durationFrom(d3) < (-1.0 + 1.0e-10));
                Assert.assertTrue(MathLib.abs(d4.durationFrom(d5)) < 1.0e-10);
            }
        }
    }

    @Test
    public void testSymmetry() {
        final TimeScale tai = TimeScalesFactory.getTAI();
        final AbsoluteDate leapStart = new AbsoluteDate(1977, 1, 1, 0, 0, 14, tai);
        for (int i = -10; i < 10; ++i) {
            final double dt = 1.1 * (2 * i - 1);
            Assert.assertEquals(dt, new AbsoluteDate(leapStart, dt, this.utc).offsetFrom(leapStart, this.utc), 1.0e-10);
            Assert.assertEquals(dt, new AbsoluteDate(leapStart, dt, tai).offsetFrom(leapStart, tai), 1.0e-10);
            Assert.assertEquals(dt, leapStart.shiftedBy(dt).durationFrom(leapStart), 1.0e-10);
        }
    }

    @Test
    public void testEquals() {
        final AbsoluteDate d1 =
            new AbsoluteDate(new DateComponents(2006, 2, 25),
                new TimeComponents(17, 10, 34),
                this.utc);
        final AbsoluteDate d2 = new AbsoluteDate(new DateComponents(2006, 2, 25),
            new TimeComponents(17, 10, 0),
            this.utc).shiftedBy(34);
        Assert.assertTrue(d1.equals(d2));
        Assert.assertFalse(d1.equals(this));
    }

    @Test
    public void testEqualsEpsilon() {
        // Initialization
        final AbsoluteDate d1 = new AbsoluteDate(new DateComponents(2006, 2, 25), new TimeComponents(17, 10, 34),
                this.utc);
        final AbsoluteDate d2 = d1.shiftedBy(0.09);
        // Check
        Assert.assertFalse(d1.equals(d2));
        Assert.assertTrue(d1.equals(d2, 0.1));
        Assert.assertFalse(d1.equals(d2, 0.08));
    }

    @Test
    public void testComponents() throws PatriusException {
        // this is NOT J2000.0,
        // it is either a few seconds before or after depending on time scale
        final DateComponents date = new DateComponents(2000, 01, 01);
        final TimeComponents time = new TimeComponents(11, 59, 10);
        final TimeScale[] scales = {
            TimeScalesFactory.getTAI(), TimeScalesFactory.getUTC(),
            TimeScalesFactory.getTT(), TimeScalesFactory.getTCG()
        };
        for (int i = 0; i < scales.length; ++i) {
            final AbsoluteDate in = new AbsoluteDate(date, time, scales[i]);
            for (int j = 0; j < scales.length; ++j) {
                final DateTimeComponents pair = in.getComponents(scales[j]);
                if (i == j) {
                    Assert.assertEquals(date, pair.getDate());
                    Assert.assertEquals(time, pair.getTime());
                } else {
                    Assert.assertNotSame(date, pair.getDate());
                    Assert.assertNotSame(time, pair.getTime());
                }
            }
        }
    }

    @Test
    public void testMonth() throws PatriusException {
        final TimeScale utc = TimeScalesFactory.getUTC();
        Assert.assertEquals(new AbsoluteDate(2011, 2, 23, utc),
            new AbsoluteDate(2011, Month.FEBRUARY, 23, utc));
        Assert.assertEquals(new AbsoluteDate(2011, 2, 23, 1, 2, 3.4, utc),
            new AbsoluteDate(2011, Month.FEBRUARY, 23, 1, 2, 3.4, utc));
    }

    @Test
    public void testCCSDSUnsegmentedNoExtension() throws PatriusException {

        final AbsoluteDate reference = new AbsoluteDate("2002-05-23T12:34:56.789", this.utc);
        final double lsb = MathLib.pow(2.0, -24);

        final byte[] timeCCSDSEpoch = new byte[] { 0x53, 0x7F, 0x40, -0x70, -0x37, -0x05, -0x19 };
        for (int preamble = 0x00; preamble < 0x80; ++preamble) {
            if (preamble == 0x1F) {
                // using CCSDS reference epoch
                final AbsoluteDate ccsds1 =
                    AbsoluteDate.parseCCSDSUnsegmentedTimeCode((byte) preamble, (byte) 0x0, timeCCSDSEpoch, null);
                Assert.assertEquals(0, ccsds1.durationFrom(reference), lsb / 2);
            } else {
                try {
                    AbsoluteDate.parseCCSDSUnsegmentedTimeCode((byte) preamble, (byte) 0x0, timeCCSDSEpoch, null);
                    Assert.fail("an exception should have been thrown");
                } catch (final PatriusException iae) {
                    // expected
                }

            }
        }

        // missing epoch
        final byte[] timeJ2000Epoch = new byte[] { 0x04, 0x7E, -0x0B, -0x10, -0x07, 0x16, -0x79 };
        try {
            AbsoluteDate.parseCCSDSUnsegmentedTimeCode((byte) 0x2F, (byte) 0x0, timeJ2000Epoch, null);
            Assert.fail("an exception should have been thrown");
        } catch (final PatriusException iae) {
            // expected
        }

        // using J2000.0 epoch
        final AbsoluteDate ccsds3 =
            AbsoluteDate.parseCCSDSUnsegmentedTimeCode((byte) 0x2F, (byte) 0x0, timeJ2000Epoch,
                AbsoluteDate.J2000_EPOCH);
        Assert.assertEquals(0, ccsds3.durationFrom(reference), lsb / 2);

    }

    @Test
    public void testCCSDSUnsegmentedWithExtendedPreamble() throws PatriusException {

        final AbsoluteDate reference = new AbsoluteDate("2095-03-03T22:02:45.789012345678901", this.utc);
        final int leap = (int) MathLib.rint(this.utc.offsetFromTAI(reference));
        final double lsb = MathLib.pow(2.0, -48);

        final byte extendedPreamble = (byte) -0x80;
        final byte identification = (byte) 0x10;
        final byte coarseLength1 = (byte) 0x0C; // four (3 + 1) bytes
        final byte fineLength1 = (byte) 0x03; // 3 bytes
        final byte coarseLength2 = (byte) 0x20; // 1 additional byte for coarse time
        final byte fineLength2 = (byte) 0x0C; // 3 additional bytes for fine time
        final byte[] timeCCSDSEpoch = new byte[] {
            0x01, 0x02, 0x03, 0x04, (byte) (0x05 - leap), // 5 bytes for coarse time (seconds)
            -0x37, -0x04, -0x4A, -0x74, -0x2C, -0x3C // 6 bytes for fine time (sub-seconds)
        };
        final byte preamble1 = (byte) (extendedPreamble | identification | coarseLength1 | fineLength1);
        final byte preamble2 = (byte) (coarseLength2 | fineLength2);
        final AbsoluteDate ccsds1 =
            AbsoluteDate.parseCCSDSUnsegmentedTimeCode(preamble1, preamble2, timeCCSDSEpoch, null);
        Assert.assertEquals(0, ccsds1.durationFrom(reference), lsb / 2);

    }

    @Test
    public void testCCSDSDaySegmented() throws PatriusException {

        final AbsoluteDate reference = new AbsoluteDate("2002-05-23T12:34:56.789012345678", TimeScalesFactory.getUTC());
        final double lsb = 1.0e-13;
        final byte[] timeCCSDSEpoch = new byte[] { 0x3F, 0x55, 0x02, -0x4D, 0x2C, -0x6B, 0x00, -0x44, 0x61, 0x4E };

        for (int preamble = 0x00; preamble < 0x100; ++preamble) {
            if (preamble == 0x42) {
                // using CCSDS reference epoch
                final AbsoluteDate ccsds1 =
                    AbsoluteDate.parseCCSDSDaySegmentedTimeCode((byte) preamble, timeCCSDSEpoch, null);
                Assert.assertEquals(0, ccsds1.durationFrom(reference), lsb / 2);
            } else {
                try {
                    AbsoluteDate.parseCCSDSDaySegmentedTimeCode((byte) preamble, timeCCSDSEpoch, null);
                    Assert.fail("an exception should have been thrown");
                } catch (final PatriusException iae) {
                    // expected
                }

            }
        }

        // missing epoch
        final byte[] timeJ2000Epoch = new byte[] { 0x03, 0x69, 0x02, -0x4D, 0x2C, -0x6B, 0x00, -0x44, 0x61, 0x4E };
        try {
            AbsoluteDate.parseCCSDSDaySegmentedTimeCode((byte) 0x4A, timeJ2000Epoch, null);
            Assert.fail("an exception should have been thrown");
        } catch (final PatriusException iae) {
            // expected
        }

        // using J2000.0 epoch
        final AbsoluteDate ccsds3 =
            AbsoluteDate.parseCCSDSDaySegmentedTimeCode((byte) 0x4A, timeJ2000Epoch, DateComponents.J2000_EPOCH);
        Assert.assertEquals(0, ccsds3.durationFrom(reference), lsb / 2);

        // limit to microsecond
        final byte[] timeMicrosecond = new byte[] { 0x03, 0x69, 0x02, -0x4D, 0x2C, -0x6B, 0x00, 0x0C };
        final AbsoluteDate ccsds4 =
            AbsoluteDate.parseCCSDSDaySegmentedTimeCode((byte) 0x49, timeMicrosecond, DateComponents.J2000_EPOCH);
        Assert.assertEquals(-0.345678e-6, ccsds4.durationFrom(reference), lsb / 2);

    }

    @Test
    public void testCCSDSCalendarSegmented() throws PatriusException {

        final AbsoluteDate reference = new AbsoluteDate("2002-05-23T12:34:56.789012345678", TimeScalesFactory.getUTC());
        final double lsb = 1.0e-13;

        // month of year / day of month variation
        final byte[] timeMonthDay =
            new byte[] { 0x07, -0x2E, 0x05, 0x17, 0x0C, 0x22, 0x38, 0x4E, 0x5A, 0x0C, 0x22, 0x38,
                0x4E };
        for (int preamble = 0x00; preamble < 0x100; ++preamble) {
            if (preamble == 0x56) {
                final AbsoluteDate ccsds1 =
                    AbsoluteDate.parseCCSDSCalendarSegmentedTimeCode((byte) preamble, timeMonthDay);
                Assert.assertEquals(0, ccsds1.durationFrom(reference), lsb / 2);
            } else {
                try {
                    AbsoluteDate.parseCCSDSCalendarSegmentedTimeCode((byte) preamble, timeMonthDay);
                    Assert.fail("an exception should have been thrown");
                } catch (final PatriusException iae) {
                    // expected
                } catch (final IllegalArgumentException iae) {
                    // should happen when preamble specifies day of year variation
                    // since there is no day 1303 (= 5 * 256 + 23) in any year ...
                    Assert.assertEquals(preamble & 0x08, 0x08);
                }

            }
        }

        // day of year variation
        final byte[] timeDay =
            new byte[] { 0x07, -0x2E, 0x00, -0x71, 0x0C, 0x22, 0x38, 0x4E, 0x5A, 0x0C, 0x22, 0x38, 0x4E };
        for (int preamble = 0x00; preamble < 0x100; ++preamble) {
            if (preamble == 0x5E) {
                final AbsoluteDate ccsds1 =
                    AbsoluteDate.parseCCSDSCalendarSegmentedTimeCode((byte) preamble, timeDay);
                Assert.assertEquals(0, ccsds1.durationFrom(reference), lsb / 2);
            } else {
                try {
                    AbsoluteDate.parseCCSDSCalendarSegmentedTimeCode((byte) preamble, timeDay);
                    Assert.fail("an exception should have been thrown");
                } catch (final PatriusException iae) {
                    // expected
                } catch (final IllegalArgumentException iae) {
                    // should happen when preamble specifies month of year / day of month variation
                    // since there is no month 0 in any year ...
                    Assert.assertEquals(preamble & 0x08, 0x00);
                }

            }
        }

        // limit to microsecond
        final byte[] timeMicrosecond = new byte[] { 0x07, -0x2E, 0x00, -0x71, 0x0C, 0x22, 0x38, 0x4E, 0x5A, 0x0C };
        final AbsoluteDate ccsds4 =
            AbsoluteDate.parseCCSDSCalendarSegmentedTimeCode((byte) 0x5B, timeMicrosecond);
        Assert.assertEquals(-0.345678e-6, ccsds4.durationFrom(reference), lsb / 2);

    }

    @Test(expected = IllegalArgumentException.class)
    public void testExpandedConstructors() throws PatriusException {
        Assert.assertEquals(new AbsoluteDate(new DateComponents(2002, 05, 28),
            new TimeComponents(15, 30, 0),
            TimeScalesFactory.getUTC()),
            new AbsoluteDate(2002, 05, 28, 15, 30, 0, TimeScalesFactory.getUTC()));
        Assert.assertEquals(new AbsoluteDate(new DateComponents(2002, 05, 28), TimeComponents.H00,
            TimeScalesFactory.getUTC()),
            new AbsoluteDate(2002, 05, 28, TimeScalesFactory.getUTC()));
        new AbsoluteDate(2002, 05, 28, 25, 30, 0, TimeScalesFactory.getUTC());
    }

    // FT 712 : Test with a date created with a bad format, i.e no seconds in its time part
    // An IllegalArgumentException must be risen.
    @Test(expected = IllegalArgumentException.class)
    public void testBadFormatConstructor() {
        new AbsoluteDate("2016-11-16T14:43", TimeScalesFactory.getTAI());
    }

    @Test
    public void testHashcode() {
        final AbsoluteDate d1 =
            new AbsoluteDate(new DateComponents(2006, 2, 25),
                new TimeComponents(17, 10, 34),
                this.utc);
        final AbsoluteDate d2 = new AbsoluteDate(new DateComponents(2006, 2, 25),
            new TimeComponents(17, 10, 0),
            this.utc).shiftedBy(34);
        Assert.assertEquals(d1.hashCode(), d2.hashCode());
        Assert.assertTrue(d1.hashCode() != d1.shiftedBy(1.0e-3).hashCode());
    }

    @Test
    public void testInfinity() {
        Assert.assertTrue(AbsoluteDate.JULIAN_EPOCH.compareTo(AbsoluteDate.PAST_INFINITY) > 0);
        Assert.assertTrue(AbsoluteDate.JULIAN_EPOCH.compareTo(AbsoluteDate.FUTURE_INFINITY) < 0);
        Assert.assertTrue(AbsoluteDate.J2000_EPOCH.compareTo(AbsoluteDate.PAST_INFINITY) > 0);
        Assert.assertTrue(AbsoluteDate.J2000_EPOCH.compareTo(AbsoluteDate.FUTURE_INFINITY) < 0);
        Assert.assertTrue(AbsoluteDate.PAST_INFINITY.compareTo(AbsoluteDate.JULIAN_EPOCH) < 0);
        Assert.assertTrue(AbsoluteDate.PAST_INFINITY.compareTo(AbsoluteDate.J2000_EPOCH) < 0);
        Assert.assertTrue(AbsoluteDate.PAST_INFINITY.compareTo(AbsoluteDate.FUTURE_INFINITY) < 0);
        Assert.assertTrue(AbsoluteDate.FUTURE_INFINITY.compareTo(AbsoluteDate.JULIAN_EPOCH) > 0);
        Assert.assertTrue(AbsoluteDate.FUTURE_INFINITY.compareTo(AbsoluteDate.J2000_EPOCH) > 0);
        Assert.assertTrue(AbsoluteDate.FUTURE_INFINITY.compareTo(AbsoluteDate.PAST_INFINITY) > 0);
        Assert.assertTrue(Double.isInfinite(AbsoluteDate.FUTURE_INFINITY.durationFrom(AbsoluteDate.J2000_EPOCH)));
        Assert.assertTrue(Double.isInfinite(AbsoluteDate.FUTURE_INFINITY.durationFrom(AbsoluteDate.PAST_INFINITY)));
        Assert.assertTrue(Double.isInfinite(AbsoluteDate.PAST_INFINITY.durationFrom(AbsoluteDate.J2000_EPOCH)));
    }

    @Test
    public void testAccuracy() {
        final TimeScale tai = TimeScalesFactory.getTAI();
        final double sec = 0.281;
        final AbsoluteDate t = new AbsoluteDate(2010, 6, 21, 18, 42, sec, tai);
        final double recomputedSec = t.getComponents(tai).getTime().getSecond();
        Assert.assertEquals(sec, recomputedSec, MathLib.ulp(sec));
    }

    @Test
    public void testIterationAccuracy() {

        final TimeScale tai = TimeScalesFactory.getTAI();
        final AbsoluteDate t0 = new AbsoluteDate(2010, 6, 21, 18, 42, 0.281, tai);

        // 0.1 is not representable exactly in double precision
        // we will accumulate error, between -0.5ULP and -3ULP at each iteration
        this.checkIteration(0.1, t0, 10000, 3.0, -1.19, 1.0e-4);

        // 0.125 is representable exactly in double precision
        // error will be null
        this.checkIteration(0.125, t0, 10000, 1.0e-15, 0.0, 1.0e-15);

    }

    @Test
    public void testEpochOffsetCtor() throws PatriusException {
        // REMINDER : AbsoluteDate(epoch,offset) is an unsafe ctor
        // meant for users who know what they are doing.
        final AbsoluteDate someDate = new AbsoluteDate("1969-07-21T02:56:00", this.utc);
        final long someEpoch = someDate.getEpoch();
        final double someOffset = someDate.getOffset();
        final AbsoluteDate otherDate = new AbsoluteDate(someEpoch, someOffset);
        Assert.assertTrue(someDate.equals(otherDate));
        Assert.assertEquals(-960887033L, someEpoch);
        Assert.assertEquals(0.5753108000000005, someOffset, 0.);
    }

    @Test
    public void testToStringPrecision() throws PatriusException {
        Assert.assertEquals("1950-01-01T00:00:00",
            AbsoluteDate.FIFTIES_EPOCH_TAI.toString(0));
        Assert.assertEquals("1950-01-01T00:00:00.0000",
            AbsoluteDate.FIFTIES_EPOCH_TAI.toString(4));
        Assert.assertEquals("1950-01-01T00:00:00.000000000000000000",
            AbsoluteDate.FIFTIES_EPOCH_TAI.toString(18));
    }

    private void checkIteration(final double step, final AbsoluteDate t0, final int nMax,
                                final double maxErrorFactor,
                                final double expectedMean, final double meanTolerance) {
        final double epsilon = MathLib.ulp(step);
        AbsoluteDate iteratedDate = t0;
        double mean = 0;
        for (int i = 1; i < nMax; ++i) {
            iteratedDate = iteratedDate.shiftedBy(step);
            final AbsoluteDate directDate = t0.shiftedBy(i * step);
            final double error = iteratedDate.durationFrom(directDate);
            mean += error / (i * epsilon);
            Assert.assertEquals(0.0, iteratedDate.durationFrom(directDate), maxErrorFactor * i * epsilon);
        }
        mean /= nMax;
        Assert.assertEquals(expectedMean, mean, meanTolerance);
    }

    @Test
    public void testSerialization() {

        // Random test
        final AbsoluteDate ad = new AbsoluteDate(new DateComponents(2008, 3, 9), new TimeComponents(8, 12, 13.816),
            TimeScalesFactory.getTAI());
        TestUtils.checkSerializedEquality(ad);

        // Test constants
        TestUtils.checkSerializedEquality(AbsoluteDate.JULIAN_EPOCH);
        TestUtils.checkSerializedEquality(AbsoluteDate.MODIFIED_JULIAN_EPOCH);
        TestUtils.checkSerializedEquality(AbsoluteDate.FIFTIES_EPOCH_TT);
        TestUtils.checkSerializedEquality(AbsoluteDate.FIFTIES_EPOCH_TAI);
        TestUtils.checkSerializedEquality(AbsoluteDate.FIFTIES_EPOCH_UTC);
        TestUtils.checkSerializedEquality(AbsoluteDate.CCSDS_EPOCH);
        TestUtils.checkSerializedEquality(AbsoluteDate.GALILEO_EPOCH);
        TestUtils.checkSerializedEquality(AbsoluteDate.GPS_EPOCH);
        TestUtils.checkSerializedEquality(AbsoluteDate.J2000_EPOCH);
        TestUtils.checkSerializedEquality(AbsoluteDate.JAVA_EPOCH);
        TestUtils.checkSerializedEquality(AbsoluteDate.PAST_INFINITY);
        TestUtils.checkSerializedEquality(AbsoluteDate.FUTURE_INFINITY);
    }

    /**
     * Test TAI constructors. Reference: same constructors with TAI scale.
     */
    @Test
    public void testTAIConstructors() {
        final AbsoluteDate actualDate1 = new AbsoluteDate("2018-06-21T07:43:59.544");
        final AbsoluteDate expectedDate1 = new AbsoluteDate("2018-06-21T07:43:59.544", TimeScalesFactory.getTAI()); 
        Assert.assertEquals(actualDate1, expectedDate1);

        final AbsoluteDate actualDate2 = new AbsoluteDate(2002, 01, 02, 10, 20, 30);
        final AbsoluteDate expectedDate2 = new AbsoluteDate(2002, 01, 02, 10, 20, 30, TimeScalesFactory.getTAI()); 
        Assert.assertEquals(actualDate2, expectedDate2);
    }

    /**
     * @testType UT
     * 
     * @testedMethod {@link AbsoluteDate#getOffset()}
     * 
     * @description test numerical quality issue with getOffset() method: offset number should be exactly as expected
     * 
     * @input date
     *
     * @output offset
     *
     * @testPassCriteria offset is exactly as provided
     * 
     * @referenceVersion 4.0
     * 
     * @nonRegressionVersion 4.0
     */
    @Test
    public void testOffset() {
        final String d = "2018-06-21T07:43:59.544";
        final AbsoluteDate date = new AbsoluteDate(d, TimeScalesFactory.getTAI());
        Assert.assertEquals(0.544, date.getOffset(), 0.);
    }

    @Before
    public void setUp() throws PatriusException {
        Utils.setDataRoot("regular-data");
        this.utc = TimeScalesFactory.getUTC();
    }

    private TimeScale utc;

}
