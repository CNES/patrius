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
* VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:363:03/12/2014:Solved toString round date bug
 * VERSION::DM:524:10/03/2016:serialization test
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.time;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class DateTimeComponentsTest {

    @Test
    public void testComparisons() {
        final DateTimeComponents[] dates = {
            new DateTimeComponents(2003, 1, 1, 7, 15, 33),
            new DateTimeComponents(2003, 1, 1, 7, 15, 34),
            new DateTimeComponents(2003, 1, 1, 7, 16, 34),
            new DateTimeComponents(2003, 1, 1, 8, 16, 34),
            new DateTimeComponents(2003, 1, 2, 8, 16, 34),
            new DateTimeComponents(2003, 2, 2, 8, 16, 34),
            new DateTimeComponents(2004, 2, 2, 8, 16, 34)
        };
        for (int i = 0; i < dates.length; ++i) {
            for (int j = 0; j < dates.length; ++j) {
                Assert.assertEquals(i < j, dates[i].compareTo(dates[j]) < 0);
                Assert.assertEquals(i > j, dates[j].compareTo(dates[i]) < 0);
                Assert.assertEquals(i == j, dates[i].compareTo(dates[j]) == 0);
                Assert.assertEquals(i > j, dates[i].compareTo(dates[j]) > 0);
                Assert.assertEquals(i < j, dates[j].compareTo(dates[i]) > 0);
            }
        }
        Assert.assertFalse(dates[0].equals(this));
        Assert.assertFalse(dates[0].equals(dates[0].getDate()));
        Assert.assertFalse(dates[0].equals(dates[0].getTime()));
        Assert.assertEquals(dates[0].hashCode(), 1095143168);
    }

    @Test
    public void testOffset() {
        final DateTimeComponents reference = new DateTimeComponents(2005, 12, 31, 23, 59, 59);
        final DateTimeComponents expected = new DateTimeComponents(2006, 1, 1, 0, 0, 0);
        Assert.assertEquals(expected, new DateTimeComponents(reference, 1));
    }

    @Test
    public void testSymmetry() {
        final DateTimeComponents reference1 = new DateTimeComponents(2005, 12, 31, 12, 0, 0);
        final DateTimeComponents reference2 = new DateTimeComponents(2006, 1, 1, 1, 2, 3);
        for (double dt = -100000; dt < 100000; dt += 100) {
            Assert.assertEquals(dt, new DateTimeComponents(reference1, dt).offsetFrom(reference1), 1.0e-15);
            Assert.assertEquals(dt, new DateTimeComponents(reference2, dt).offsetFrom(reference2), 1.0e-15);
        }
    }

    @Test
    public void testString() throws PatriusException {
        final DateTimeComponents date =
            new DateTimeComponents(DateComponents.J2000_EPOCH, TimeComponents.H12);
        Assert.assertEquals("2000-01-01T12:00:00.000", date.toString());

        // Test the print function when precision is 0 (the point should not be printed):
        final AbsoluteDate d1 = new AbsoluteDate();
        final TimeScale tai = TimeScalesFactory.getTAI();
        Assert.assertEquals("2000-01-01T11:59:28", d1.toString(0, tai));

        // Test the print function when minute/hour/day switch to the next one:
        final TimeScale utc = TimeScalesFactory.getUTC();
        // 1) date = 2012-06-30T18:12:59.9999900
        AbsoluteDate d = new AbsoluteDate("2012-06-30T18:12:59.99999", utc);
        Assert.assertEquals("2012-06-30T18:12:59.9999900", d.toString(7, utc));
        Assert.assertEquals("2012-06-30T18:12:59.99999", d.toString(5, utc));
        Assert.assertEquals("2012-06-30T18:13:00.0000", d.toString(4, utc));
        Assert.assertEquals("2012-06-30T18:13:00.00", d.toString(2, utc));
        // 2) date = 2012-06-30T18:59:59.9999900
        d = new AbsoluteDate("2012-06-30T18:59:59.9999900", tai);
        Assert.assertEquals("2012-06-30T18:59:59.9999900", d.toString(7, tai));
        Assert.assertEquals("2012-06-30T18:59:59.99999", d.toString(5, tai));
        Assert.assertEquals("2012-06-30T19:00:00.0000", d.toString(4, tai));
        Assert.assertEquals("2012-06-30T19:00:00.00", d.toString(2, tai));
        // 3) date = 2012-01-31T23:59:59.99999
        d = new AbsoluteDate("2012-01-31T23:59:59.99999", utc);
        Assert.assertEquals("2012-01-31T23:59:59.9999900", d.toString(7, utc));
        Assert.assertEquals("2012-01-31T23:59:59.99999", d.toString(5, utc));
        Assert.assertEquals("2012-02-01T00:00:00.0000", d.toString(4, utc));
        Assert.assertEquals("2012-02-01T00:00:00.00", d.toString(2, utc));

        // Test the print function when the date should be rounded up to the next day (leap second):
        final AbsoluteDate d2 = new AbsoluteDate("2012-07-01T00:00:00.00000", utc);
        // 1) date = 2012-06-30T23:59:57.9999900
        d = new AbsoluteDate(d2, -3.00001);
        Assert.assertEquals("2012-06-30T23:59:57.9999900", d.toString(7, utc));
        Assert.assertEquals("2012-06-30T23:59:57.99999", d.toString(5, utc));
        Assert.assertEquals("2012-06-30T23:59:58.0000", d.toString(4, utc));
        Assert.assertEquals("2012-06-30T23:59:58.00", d.toString(2, utc));
        // 2) date = 2012-06-30T23:59:59.9999900
        d = new AbsoluteDate(d2, -1.00001);
        Assert.assertEquals("2012-06-30T23:59:59.9999900", d.toString(7, utc));
        Assert.assertEquals("2012-06-30T23:59:59.99999", d.toString(5, utc));
        Assert.assertEquals("2012-06-30T23:59:60.0000", d.toString(4, utc));
        Assert.assertEquals("2012-06-30T23:59:60.00", d.toString(2, utc));
        // 3) date = 2012-06-30T23:59:60.9999900 (UTC)
        d = new AbsoluteDate(d2, -0.00001);
        Assert.assertEquals("2012-06-30T23:59:60.9999900", d.toString(7, utc));
        Assert.assertEquals("2012-06-30T23:59:60.99999", d.toString(5, utc));
        Assert.assertEquals("2012-07-01T00:00:00.0000", d.toString(4, utc));
        Assert.assertEquals("2012-07-01T00:00:00.00", d.toString(2, utc));
        // 4) date = 2012-07-01T00:00:00.9999900
        d = new AbsoluteDate(d2, 0.99999);
        Assert.assertEquals("2012-07-01T00:00:00.9999900", d.toString(7, utc));
        Assert.assertEquals("2012-07-01T00:00:00.99999", d.toString(5, utc));
        Assert.assertEquals("2012-07-01T00:00:01.0000", d.toString(4, utc));
        Assert.assertEquals("2012-07-01T00:00:01.00", d.toString(2, utc));
        AbsoluteDate d3 = new AbsoluteDate("2012-07-01T00:00:00.00000", tai);
        // 5) date = 2012-06-30T23:59:60.9999900 (TAI)
        d3 = new AbsoluteDate(d3, -0.00001);
        Assert.assertEquals("2012-06-30T23:59:59.9999900", d3.toString(7, tai));
        Assert.assertEquals("2012-06-30T23:59:59.99999", d3.toString(5, tai));
        Assert.assertEquals("2012-07-01T00:00:00.0000", d3.toString(4, tai));
        Assert.assertEquals("2012-07-01T00:00:00.00", d3.toString(2, tai));
    }

    @Test
    public void testMonth() {
        Assert.assertEquals(new DateTimeComponents(2011, 2, 23),
            new DateTimeComponents(2011, Month.FEBRUARY, 23));
        Assert.assertEquals(new DateTimeComponents(2011, 2, 23, 1, 2, 3.4),
            new DateTimeComponents(2011, Month.FEBRUARY, 23, 1, 2, 3.4));
    }

    @Test
    public void testParse() {
        final String s = "2000-01-02T03:04:05.000";
        final String s2 = "2000-01-02T00:00:00.000";
        Assert.assertEquals(s, DateTimeComponents.parseDateTime(s).toString());
        Assert.assertEquals(s2, DateTimeComponents.parseDateTime("2000-01-02").toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadDay() {
        DateTimeComponents.parseDateTime("2000-02-30T03:04:05.000+00:00");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadZone() {
        DateTimeComponents.parseDateTime("2000-02-29T03:04:05.000+00:01");
    }

    @Test
    public void testSerialization() {

        // build several DateTimeComponents
        final DateTimeComponents[] dates = {
            new DateTimeComponents(2003, 1, 1, 7, 15, 33),
            new DateTimeComponents(2004, 2, 2, 8, 16, 34),
            new DateTimeComponents(DateComponents.J2000_EPOCH, TimeComponents.H12),
            DateTimeComponents.parseDateTime("2000-01-02T03:04:05.000"),
            DateTimeComponents.parseDateTime("2010-08-02T03:59:59.000")
        };

        for (final DateTimeComponents date : dates) {
            TestUtils.checkSerializedEquality(date);
        }
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }
}
