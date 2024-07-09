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
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:229:24/03/2014:Corrected digital quality absorption problem
 * VERSION::DM:256:01/08/2014:Changed AbsoluteDate.toString to take into account seconds precision
 * VERSION::FA:363:03/12/2014:Solved toString round date bug
 * VERSION::FA:383:08/12/2014:Corrected digital quality absorption problem
 * VERSION::DM:524:10/03/2016:serialization test
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.time;

import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.TestUtils;

public class TimeComponentsTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle TimeComponentsTest
         * 
         * @featureDescription Tests the org.orekit.time.TimeComponents class
         * 
         * @coveredRequirements
         */
        TIME_COMPONENTS_TEST,

    }

    /**
     * This test was added as part of FT229 to ensure there are no
     * absorption problems.
     * Before this correction, the date used to be displayed as
     * 2019-01-03T16:55:-00.000
     * 
     * @since 2.2
     * @version 2.3.1
     */
    @Test
    public void testDisplay() {
        final AbsoluteDate date = new AbsoluteDate(599806499, 0.999999999999);
        Assert.assertTrue("2019-01-03T16:55:00.000".contentEquals(date.toString()));
        final AbsoluteDate date2 = new AbsoluteDate(599799599, 0.999999999999);
        Assert.assertTrue("2019-01-03T15:00:00.000".contentEquals(date2.toString()));

        // test added for coverage purpose of TimeComponents constructor
        final TimeComponents time = new TimeComponents(60 * 59 + 59, 59.999);
        Assert.assertTrue("01:00:58.999".contentEquals(time.toString()));
    }

    /**
     * This test tests the verification : hours have to be positive.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testOutOfRangeA() {
        new TimeComponents(-1, 10, 10);
    }

    /**
     * This test tests the verification : hours is between 0 and 23.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testOutOfRangeB() {
        new TimeComponents(24, 10, 10);
    }

    /**
     * This test tests the verification : hours have to be positive.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testOutOfRangeC() {
        new TimeComponents(10, -1, 10);
    }

    /**
     * This test tests the verification : minutes is between 0 and 59.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testOutOfRangeD() {
        new TimeComponents(10, 60, 10);
    }

    /**
     * This test tests the verification : minutes have to be positive.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testOutOfRangeE() {
        new TimeComponents(10, 10, -1);
    }

    /**
     * This test tests the verification : seconds is between 0 and 59.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testOutOfRangeF() {
        new TimeComponents(10, 10, 61);
    }

    /**
     * This test tests the verification : seconds have to be positive.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testOutOfRangeG() {
        new TimeComponents(12, -40.0);
    }

    /**
     * This test tests the verification : seconds have to be less than
     * double org.orekit.utils.Constants.JULIAN_DAY = 86400.0 s.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testOutOfRangeH() {
        new TimeComponents(0, 86500.0);
    }

    /**
     * This test tests the verification if inconsistent arguments are given.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testNegativePrecision() {
        new TimeComponents(0, 86500.0).toString(-1, false);
    }

    /**
     * testInRange.
     */
    @Test
    public void testInRange() {

        TimeComponents time = new TimeComponents(10, 10, 10);
        Assert.assertEquals(10, time.getHour());
        Assert.assertEquals(10, time.getMinute());
        Assert.assertEquals(10.0, time.getSecond(), 1.0e-10);

        time = new TimeComponents(0.0);
        Assert.assertEquals(0.0, time.getSecondsInDay(), 1.0e-10);

        time = new TimeComponents(10, 10, 60.999);
        Assert.assertEquals(10, time.getHour());
        Assert.assertEquals(10, time.getMinute());
        Assert.assertEquals(60.999, time.getSecond(), 1.0e-10);

        time = new TimeComponents(43200.0);
        Assert.assertEquals(43200.0, time.getSecondsInDay(), 1.0e-10);

        time = new TimeComponents(86399.999);
        Assert.assertEquals(86399.999, time.getSecondsInDay(), 1.0e-10);

    }

    /**
     * tests getSecondsInDay.
     */
    @Test
    public void testValues() {
        Assert.assertEquals(0.0, new TimeComponents(0, 0, 0).getSecondsInDay(), 1.0e-10);
        Assert.assertEquals(21600.0, new TimeComponents(6, 0, 0).getSecondsInDay(), 1.0e-10);
        Assert.assertEquals(43200.0, new TimeComponents(12, 0, 0).getSecondsInDay(), 1.0e-10);
        Assert.assertEquals(64800.0, new TimeComponents(18, 0, 0).getSecondsInDay(), 1.0e-10);
        Assert.assertEquals(86399.9, new TimeComponents(23, 59, 59.9).getSecondsInDay(), 1.0e-10);
    }

    /**
     * tests toString.
     */
    @Test
    public void testString() {
        Assert.assertEquals("00:00:00.000", new TimeComponents(0).toString());
        Assert.assertEquals("06:00:00.000", new TimeComponents(21600).toString());
        Assert.assertEquals("12:00:00.000", new TimeComponents(43200).toString());
        Assert.assertEquals("18:00:00.000", new TimeComponents(64800).toString());
        Assert.assertEquals("23:59:59.900", new TimeComponents(86399.9).toString());
    }

    /**
     * tests toString with a negative precision.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testStringNegativePrecision() {
        final int precision = -1;
        new TimeComponents(0).toString(precision, false);
    }

    /**
     * tests parseTime.
     */
    @Test
    public void testParse() {
        Assert.assertEquals(86399.9, TimeComponents.parseTime("235959.900").getSecondsInDay(), 1.0e-10);
        Assert.assertEquals(86399.9, TimeComponents.parseTime("23:59:59.900").getSecondsInDay(), 1.0e-10);
        Assert.assertEquals(86399.9, TimeComponents.parseTime("23:59:59,900").getSecondsInDay(), 1.0e-10);
        Assert.assertEquals(86399.9, TimeComponents.parseTime("235959.900Z").getSecondsInDay(), 1.0e-10);
        Assert.assertEquals(86399.9, TimeComponents.parseTime("23:59:59.900Z").getSecondsInDay(), 1.0e-10);
        Assert.assertEquals(86399.9, TimeComponents.parseTime("235959.900+00").getSecondsInDay(), 1.0e-10);
        Assert.assertEquals(86399.9, TimeComponents.parseTime("23:59:59.900+00").getSecondsInDay(), 1.0e-10);
        Assert.assertEquals(86399.9, TimeComponents.parseTime("235959.900+00:00").getSecondsInDay(), 1.0e-10);
        Assert.assertEquals(86399.9, TimeComponents.parseTime("23:59:59.900+00:00").getSecondsInDay(), 1.0e-10);
    }

    /**
     * tests bad format for parseTime.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testBadFormat() {
        TimeComponents.parseTime("23h59m59s");
    }

    /**
     * tests bad zone for parseTime.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testBadZone() {
        TimeComponents.parseTime("23:59:59+01:00");
    }

    /**
     * testComparisons.
     */
    @Test
    public void testComparisons() {
        final TimeComponents[] times = { new TimeComponents(0, 0, 0.0), new TimeComponents(0, 0, 1.0e-15),
            new TimeComponents(0, 12, 3.0), new TimeComponents(15, 9, 3.0), new TimeComponents(23, 59, 59.0),
            new TimeComponents(23, 59, 60.0 - 1.0e-12) };
        for (int i = 0; i < times.length; ++i) {
            for (int j = 0; j < times.length; ++j) {
                if (times[i].compareTo(times[j]) < 0) {
                    Assert.assertTrue(times[j].compareTo(times[i]) > 0);
                    Assert.assertFalse(times[i].equals(times[j]));
                    Assert.assertFalse(times[j].equals(times[i]));
                    Assert.assertTrue(times[i].hashCode() != times[j].hashCode());
                    Assert.assertTrue(i < j);
                } else if (times[i].compareTo(times[j]) > 0) {
                    Assert.assertTrue(times[j].compareTo(times[i]) < 0);
                    Assert.assertFalse(times[i].equals(times[j]));
                    Assert.assertFalse(times[j].equals(times[i]));
                    Assert.assertTrue(times[i].hashCode() != times[j].hashCode());
                    Assert.assertTrue(i > j);
                } else {
                    Assert.assertTrue(times[j].compareTo(times[i]) == 0);
                    Assert.assertTrue(times[i].equals(times[j]));
                    Assert.assertTrue(times[j].equals(times[i]));
                    Assert.assertTrue(times[i].hashCode() == times[j].hashCode());
                    Assert.assertTrue(i == j);
                }
            }
        }
        Assert.assertFalse(times[0].equals(this));
    }

    @Test
    public void testSerialization() {

        // Random test
        final Random r = new Random();
        for (int i = 0; i < 1000; ++i) {
            // nextInt/Double exclusive of the top value, add one if needed
            final int h = r.nextInt((23 - 0) + 1) + 0;
            final int m = r.nextInt((59 - 0) + 1) + 0;
            final double s = 0.0 + (61.0 - 0.0) * r.nextDouble();
            TestUtils.checkSerializedEquality(new TimeComponents(h, m, s));
        }

        // Test constants
        TestUtils.checkSerializedEquality(TimeComponents.H00);
        TestUtils.checkSerializedEquality(TimeComponents.H12);
    }
}
