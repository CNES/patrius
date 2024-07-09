/**
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
 * HISTORY
* VERSION:4.4:DM:DM-2143:04/10/2019:[PATRIUS] Ajout des methodes overlaps() et includes() a AbsoluteDateIntervalsList
* VERSION:4.4:FA:FA-2133:04/10/2019:[PATRIUS] Probleme de robustesse dans AbsoluteDateIntervalsList
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1936:23/10/2018: add new methods to intervals classes
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.utils;

import static org.junit.Assert.*;

import java.util.NoSuchElementException;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.math.interval.IntervalEndpointType;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.time.AbsoluteDateIntervalsList;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * Test class for {@link AbsoluteDateIntervalsList}
 * </p>
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id: AbsoluteDateIntervalsListTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.1
 * 
 */
public class AbsoluteDateIntervalsListTest {

    /** Date 1. **/
    private static AbsoluteDate date1;
    /** Date 2. **/
    private static AbsoluteDate date2;
    /** Date 3. **/
    private static AbsoluteDate date3;
    /** Date 4. **/
    private static AbsoluteDate date4;
    /** Date 5. **/
    private static AbsoluteDate date5;
    /** Date 6. **/
    private static AbsoluteDate date6;

    /** The OPEN end point type. */
    private final IntervalEndpointType open = IntervalEndpointType.OPEN;
    /** The CLOSED end point type. */
    private final IntervalEndpointType closed = IntervalEndpointType.CLOSED;

    /** Features description. */
    public enum features {
        /**
         * @featureTitle validation of the list of time intervals
         * 
         * @featureDescription validation of the list of time intervals
         * 
         * @coveredRequirements DV-DATES_190, DV-DATES_191, DV-DATES_192
         * 
         */
        VALIDATION_TIME_INTERVALS_LIST,
        /**
         * @featureTitle validation of the methods to process intervals lists
         * 
         * @featureDescription validation of the methods to process intervals lists
         * 
         * @coveredRequirements DV-DATES_200
         * 
         */
        VALIDATION_INTERVALS_LIST_OPERATIONS
    }

    /**
     * Setup for all unit tests in the class.
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @BeforeClass
    public static void setUpBeforeClass() throws PatriusException {
        // Orekit initialization
        Utils.setDataRoot("regular-dataCNES-2003");
        date1 = new AbsoluteDate("2010-08-20T10:00:00Z", TimeScalesFactory.getTT());
        date2 = new AbsoluteDate("2010-08-20T10:30:00Z", TimeScalesFactory.getTT());
        date3 = new AbsoluteDate("2010-08-20T10:40:00Z", TimeScalesFactory.getTT());
        date4 = new AbsoluteDate("2010-08-20T12:00:00Z", TimeScalesFactory.getTT());
        date5 = new AbsoluteDate("2010-08-20T12:30:00Z", TimeScalesFactory.getTT());
        date6 = new AbsoluteDate("2010-08-20T18:00:00Z", TimeScalesFactory.getTT());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATION_TIME_INTERVALS_LIST}
     * 
     * @testedMethod {@link AbsoluteDateIntervalsList#AbsoluteDateIntervalsList()}
     * 
     * @description simple constructor test
     * 
     * @input no inputs
     * 
     * @output an {@link AbsoluteDateIntervalsList}
     * 
     * @testPassCriteria the {@link AbsoluteDateIntervalsList} is successfully
     *                   created
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public final void testAbsoluteDateIntervalsList() {
        // The AbsoluteDateIntervalsList is created:
        final AbsoluteDateIntervalsList list = new AbsoluteDateIntervalsList();
        // Check the constructor did not crash:
        Assert.assertNotNull(list);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATION_TIME_INTERVALS_LIST}
     * 
     * @testedMethod {@link AbsoluteDateIntervalsList#getIntervalsContainingDate(AbsoluteDate)}
     * 
     * @description tests {@link AbsoluteDateIntervalsList#getIntervalsContainingDate(AbsoluteDate)}
     * 
     * @input six {@link AbsoluteDateInterval}
     * 
     * @output the size of the list before and after the method computation
     * 
     * @testPassCriteria the time intervals containing the specified date are successfully found
     *                   in the list: the size of the list is the expected one.
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @Test
    public final void testGetIntervalsContainingDate() throws PatriusException {
        // The AbsoluteDateIntervalsList is created:
        final AbsoluteDateIntervalsList list = new AbsoluteDateIntervalsList();
        // set up the time intervals to add:
        final AbsoluteDateInterval i1 = new AbsoluteDateInterval(this.open, date1, date2, this.closed);
        final AbsoluteDateInterval i2 = new AbsoluteDateInterval(this.closed, date1, date3, this.open);
        final AbsoluteDateInterval i3 = new AbsoluteDateInterval(this.closed, date2, date5, this.closed);
        final AbsoluteDateInterval i4 = new AbsoluteDateInterval(this.closed, date1, date5, this.closed);
        final AbsoluteDateInterval i5 = new AbsoluteDateInterval(this.open, date4, date5, this.closed);
        final AbsoluteDateInterval i6 = new AbsoluteDateInterval(this.closed, date2, date3, this.open);
        list.add(i6);
        list.add(i5);
        list.add(i3);
        list.add(i4);
        list.add(i2);
        list.add(i1);

        // Check the size of the list:
        Assert.assertEquals(6, list.size(), 0.0);
        final AbsoluteDate dateIn = new AbsoluteDate("2010-08-20T10:35:00Z",
            TimeScalesFactory.getTT());
        // Check the size of the list of intervals containing the date dateIn:
        Assert.assertEquals(4, list.getIntervalsContainingDate(dateIn).size(), 0.0);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATION_TIME_INTERVALS_LIST}
     * 
     * @testedMethod {@link AbsoluteDateIntervalsList#add(AbsoluteDateInterval)}
     * 
     * @description tests if the intervals randomly added to the list are properly ordered
     *              by the TreeSet functionalities
     * 
     * @input an {@link AbsoluteDateIntervalsList} and six {@link AbsoluteDateInterval}
     * 
     * @output list.toArray() output
     * 
     * @testPassCriteria list.toArray() returns the expected intervals at the expected positions
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @Test
    public final void testOrderIntervalsInTheList() throws PatriusException {
        // set up the time intervals to add; the intervals are instantiated following
        // the order we expect the list to have:
        final AbsoluteDateInterval i1 = new AbsoluteDateInterval(this.closed, date1, date2, this.closed);
        final AbsoluteDateInterval i2 = new AbsoluteDateInterval(this.closed, date1, date3, this.open);
        final AbsoluteDateInterval i3 = new AbsoluteDateInterval(this.closed, date1, date3, this.closed);
        final AbsoluteDateInterval i4 = new AbsoluteDateInterval(this.closed, date1, date3, this.closed);
        final AbsoluteDateInterval i5 = new AbsoluteDateInterval(this.open, date1, date2, this.closed);
        final AbsoluteDateInterval i6 = new AbsoluteDateInterval(this.closed, date2, date3, this.closed);

        // randomly add the intervals to the list:
        final AbsoluteDateIntervalsList list = new AbsoluteDateIntervalsList();
        list.add(i3);
        list.add(i5);
        list.add(i1);
        list.add(i2);
        list.add(i6);
        list.add(i4);
        // check the size of the list is 5 (two intervals are identical, only one of them has been added):
        Assert.assertEquals(5, list.size());
        // check the intervals are at the right index in the list:
        Assert.assertEquals(i1, list.toArray()[0]);
        Assert.assertEquals(i2, list.toArray()[1]);
        Assert.assertEquals(i3, list.toArray()[2]);
        Assert.assertEquals(i5, list.toArray()[3]);
        Assert.assertEquals(i6, list.toArray()[4]);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATION_INTERVALS_LIST_OPERATIONS}
     * 
     * @testedMethod {@link AbsoluteDateIntervalsList#getInclusiveInterval()}
     * 
     * @description tests the computation of the inclusive interval given a list of intervals
     * 
     * @input some {@link AbsoluteDateIntervalsList} and {@link AbsoluteDateInterval}
     * 
     * @output list.getInclusiveInterval() output
     * 
     * @testPassCriteria list.getInclusiveInterval() returns the expected inclusive interval
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @Test
    public final void testGetInclusiveInterval() throws PatriusException {
        // set up the dates:
        // set up the time intervals to add;
        final AbsoluteDateInterval i1 = new AbsoluteDateInterval(this.closed, date1, date2, this.closed);
        final AbsoluteDateInterval i2 = new AbsoluteDateInterval(this.open, date2, date3, this.open);
        final AbsoluteDateInterval i3 = new AbsoluteDateInterval(this.closed, date3, date4, this.closed);
        final AbsoluteDateInterval i4 = new AbsoluteDateInterval(this.closed, date4, date5, this.closed);
        final AbsoluteDateInterval i5 = new AbsoluteDateInterval(this.open, date1, date3, this.closed);
        final AbsoluteDateInterval i6 = new AbsoluteDateInterval(this.closed, date3, date5, this.open);

        // Test: list = interval 2
        // d1 d2 d3 d4 d5
        // ]----[
        final AbsoluteDateIntervalsList list1 = new AbsoluteDateIntervalsList();
        list1.add(i2);
        Assert.assertEquals(new AbsoluteDateInterval(this.open, date2, date3, this.open), list1.getInclusiveInterval());
        // Test: list = intervals 1 + 5
        // d1 d2 d3 d4 d5
        // [-----]
        // ]-----------]
        final AbsoluteDateIntervalsList list2 = new AbsoluteDateIntervalsList();
        list2.add(i5);
        list2.add(i1);
        Assert.assertEquals(new AbsoluteDateInterval(this.closed, date1, date3, this.closed),
            list2.getInclusiveInterval());
        // Test: list = intervals 1 + 2 + 4
        // d1 d2 d3 d4 d5
        // [-----]
        // ]-----[
        // [-----]
        final AbsoluteDateIntervalsList list3 = new AbsoluteDateIntervalsList();
        list3.add(i4);
        list3.add(i2);
        list3.add(i1);
        Assert.assertEquals(new AbsoluteDateInterval(this.closed, date1, date5, this.closed),
            list3.getInclusiveInterval());
        // Test: list = intervals 3 + 6
        // d1 d2 d3 d4 d5
        // [-----]
        // [-----------[
        final AbsoluteDateIntervalsList list4 = new AbsoluteDateIntervalsList();
        list4.add(i3);
        list4.add(i6);
        Assert.assertEquals(new AbsoluteDateInterval(this.closed, date3, date5, this.open),
            list4.getInclusiveInterval());
        // Test: list = intervals 1 + 6 + 4
        // d1 d2 d3 d4 d5
        // [-----]
        // [-----------[
        // [-----]
        final AbsoluteDateIntervalsList list5 = new AbsoluteDateIntervalsList();
        list5.add(i1);
        list5.add(i6);
        list5.add(i4);
        Assert.assertEquals(new AbsoluteDateInterval(this.closed, date1, date5, this.closed),
            list5.getInclusiveInterval());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATION_INTERVALS_LIST_OPERATIONS}
     * 
     * @testedMethod {@link AbsoluteDateIntervalsList#getComplementaryIntervals()}
     * 
     * @description tests the computation of the complementary intervals given a list of intervals
     * 
     * @input some {@link AbsoluteDateIntervalsList} and {@link AbsoluteDateInterval}
     * 
     * @output list.getComplementaryIntervals() output
     * 
     * @testPassCriteria list.getComplementaryIntervals() returns the expected complementary interval
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @Test
    public final void testGetComplementaryIntervals() throws PatriusException {
        // set up the dates:
        // set up the time intervals to add;
        final AbsoluteDateInterval i1 = new AbsoluteDateInterval(this.closed, date1, date2, this.closed);
        final AbsoluteDateInterval i2 =
            new AbsoluteDateInterval(this.open, AbsoluteDate.PAST_INFINITY, date2, this.open);
        final AbsoluteDateInterval i3 =
            new AbsoluteDateInterval(this.closed, date4, AbsoluteDate.FUTURE_INFINITY, this.open);
        final AbsoluteDateInterval i4 = new AbsoluteDateInterval(this.open, date1, date3, this.closed);
        final AbsoluteDateInterval i5 = new AbsoluteDateInterval(this.open, date2, date4, this.closed);
        final AbsoluteDateInterval i6 = new AbsoluteDateInterval(this.closed, date2, date4, this.closed);
        final AbsoluteDateInterval i7 =
            new AbsoluteDateInterval(this.closed, date3, AbsoluteDate.FUTURE_INFINITY, this.open);
        final AbsoluteDateInterval i8 = new AbsoluteDateInterval(this.closed, date3, date4, this.open);
        final AbsoluteDateInterval i9 = new AbsoluteDateInterval(this.open, date5, date6, this.open);
        final AbsoluteDateInterval i10 =
            new AbsoluteDateInterval(this.open, date6, AbsoluteDate.FUTURE_INFINITY, this.open);

        // Test: list = interval [d1 ; d2]
        // -inf d1 d2 d3 d4 +inf
        // [----]
        final AbsoluteDateIntervalsList list1 = new AbsoluteDateIntervalsList();
        list1.add(i1);
        AbsoluteDateInterval expected1 = new AbsoluteDateInterval(IntervalEndpointType.OPEN,
            AbsoluteDate.PAST_INFINITY, date1, this.open);
        AbsoluteDateInterval expected2 = new AbsoluteDateInterval(this.open, date2, AbsoluteDate.FUTURE_INFINITY,
            IntervalEndpointType.OPEN);
        final AbsoluteDateIntervalsList expectedList1 = new AbsoluteDateIntervalsList();
        expectedList1.add(expected1);
        expectedList1.add(expected2);
        Assert.assertEquals(expectedList1, list1.getComplementaryIntervals());
        // Test: list = interval ]-inf ; d2[
        // -inf d1 d2 d3 d4 +inf
        // ]--------------[
        final AbsoluteDateIntervalsList list2 = new AbsoluteDateIntervalsList();
        list2.add(i2);
        expected1 = new AbsoluteDateInterval(this.closed, date2,
            AbsoluteDate.FUTURE_INFINITY, IntervalEndpointType.OPEN);
        final AbsoluteDateIntervalsList expectedList2 = new AbsoluteDateIntervalsList();
        expectedList2.add(expected1);
        Assert.assertEquals(expectedList2, list2.getComplementaryIntervals());
        // Test: list = interval [d4 ; +inf[
        // -inf d1 d2 d3 d4 +inf
        // [-----[
        final AbsoluteDateIntervalsList list3 = new AbsoluteDateIntervalsList();
        list3.add(i3);
        expected1 = new AbsoluteDateInterval(IntervalEndpointType.OPEN,
            AbsoluteDate.PAST_INFINITY, date4, this.open);
        final AbsoluteDateIntervalsList expectedList3 = new AbsoluteDateIntervalsList();
        expectedList3.add(expected1);
        Assert.assertEquals(expectedList3, list3.getComplementaryIntervals());
        // Test: list = intervals ]-inf ; d2[ , [d4 ; +inf[
        // -inf d1 d2 d3 d4 +inf
        // ]--------------[
        // [-----[
        final AbsoluteDateIntervalsList list4 = new AbsoluteDateIntervalsList();
        list4.add(i2);
        list4.add(i3);
        expected1 = new AbsoluteDateInterval(this.closed, date2, date4, this.open);
        final AbsoluteDateIntervalsList expectedList4 = new AbsoluteDateIntervalsList();
        expectedList4.add(expected1);
        Assert.assertEquals(expectedList4, list4.getComplementaryIntervals());
        // Test: list = intervals ]d1 ; d3] , ]d2 ; d4]
        // -inf d1 d2 d3 d4 +inf
        // ]-----------]
        // ]-----------]
        final AbsoluteDateIntervalsList list5 = new AbsoluteDateIntervalsList();
        list5.add(i4);
        list5.add(i5);
        expected1 = new AbsoluteDateInterval(IntervalEndpointType.OPEN,
            AbsoluteDate.PAST_INFINITY, date1, this.closed);
        expected2 = new AbsoluteDateInterval(this.open, date4, AbsoluteDate.FUTURE_INFINITY,
            IntervalEndpointType.OPEN);
        final AbsoluteDateIntervalsList expectedList5 = new AbsoluteDateIntervalsList();
        expectedList5.add(expected1);
        expectedList5.add(expected2);
        Assert.assertEquals(expectedList5, list5.getComplementaryIntervals());
        // Test: list = intervals ]-inf ; d2[ , ]d1 ; d3]
        // -inf d1 d2 d3 d4 +inf
        // ]--------------[
        // ]-----------]
        final AbsoluteDateIntervalsList list6 = new AbsoluteDateIntervalsList();
        list6.add(i2);
        list6.add(i4);
        expected1 = new AbsoluteDateInterval(this.open, date3, AbsoluteDate.FUTURE_INFINITY,
            IntervalEndpointType.OPEN);
        final AbsoluteDateIntervalsList expectedList6 = new AbsoluteDateIntervalsList();
        expectedList6.add(expected1);
        Assert.assertEquals(expectedList6, list6.getComplementaryIntervals());
        // Test: list = intervals ]-inf ; d2[ , ]d2 ; d4]
        // -inf d1 d2 d3 d4 +inf
        // ]--------------[
        // ]-----------]
        final AbsoluteDateIntervalsList list7 = new AbsoluteDateIntervalsList();
        list7.add(i2);
        list7.add(i5);
        expected1 = new AbsoluteDateInterval(this.closed, date2, date2, this.closed);
        expected2 = new AbsoluteDateInterval(this.open, date4, AbsoluteDate.FUTURE_INFINITY,
            IntervalEndpointType.OPEN);
        final AbsoluteDateIntervalsList expectedList7 = new AbsoluteDateIntervalsList();
        expectedList7.add(expected1);
        expectedList7.add(expected2);
        Assert.assertEquals(expectedList7, list7.getComplementaryIntervals());
        // Test: list = intervals ]-inf ; d2[ , [d2 ; d4]
        // -inf d1 d2 d3 d4 +inf
        // ]--------------[
        // [-----------]
        final AbsoluteDateIntervalsList list8 = new AbsoluteDateIntervalsList();
        list8.add(i2);
        list8.add(i6);
        expected1 = new AbsoluteDateInterval(this.open, date4, AbsoluteDate.FUTURE_INFINITY,
            IntervalEndpointType.OPEN);
        final AbsoluteDateIntervalsList expectedList8 = new AbsoluteDateIntervalsList();
        expectedList8.add(expected1);
        Assert.assertEquals(expectedList8, list8.getComplementaryIntervals());
        // Test: list = intervals [d3 ; +inf[ , [d4 ; +inf[
        // -inf d1 d2 d3 d4 +inf
        // [------------[
        // [-----[
        final AbsoluteDateIntervalsList list9 = new AbsoluteDateIntervalsList();
        list9.add(i3);
        list9.add(i7);
        expected1 = new AbsoluteDateInterval(IntervalEndpointType.OPEN, AbsoluteDate.PAST_INFINITY, date3, this.open);
        final AbsoluteDateIntervalsList expectedList9 = new AbsoluteDateIntervalsList();
        expectedList9.add(expected1);
        Assert.assertEquals(expectedList9, list9.getComplementaryIntervals());
        // Test: list = intervals [d1 ; d2] , [d3 ; d4[ , ]d5 ; d6[
        // -inf d1 d2 d3 d4 d5 d6 +inf
        // [----]
        // [-----[
        // ]-----[
        final AbsoluteDateIntervalsList list10 = new AbsoluteDateIntervalsList();
        list10.add(i1);
        list10.add(i8);
        list10.add(i9);
        expected1 = new AbsoluteDateInterval(IntervalEndpointType.OPEN, AbsoluteDate.PAST_INFINITY, date1, this.open);
        expected2 = new AbsoluteDateInterval(this.open, date2, date3, this.open);
        AbsoluteDateInterval expected3 = new AbsoluteDateInterval(this.closed, date4, date5, this.closed);
        final AbsoluteDateInterval expected4 =
            new AbsoluteDateInterval(this.closed, date6, AbsoluteDate.FUTURE_INFINITY,
                IntervalEndpointType.OPEN);
        final AbsoluteDateIntervalsList expectedList10 = new AbsoluteDateIntervalsList();
        expectedList10.add(expected1);
        expectedList10.add(expected2);
        expectedList10.add(expected3);
        expectedList10.add(expected4);
        Assert.assertEquals(expectedList10, list10.getComplementaryIntervals());
        // Test: list = intervals [-inf ; d2[ , [d3 ; d4[ , ]d6 ; +inf[
        // -inf d1 d2 d3 d4 d5 d6 +inf
        // ]--------------[
        // [-----[
        // ]------[
        final AbsoluteDateIntervalsList list11 = new AbsoluteDateIntervalsList();
        list11.add(i2);
        list11.add(i8);
        list11.add(i10);
        expected1 = new AbsoluteDateInterval(this.closed, date2, date3, this.open);
        expected2 = new AbsoluteDateInterval(this.closed, date4, date6, this.closed);
        final AbsoluteDateIntervalsList expectedList11 = new AbsoluteDateIntervalsList();
        expectedList11.add(expected1);
        expectedList11.add(expected2);
        Assert.assertEquals(expectedList11, list11.getComplementaryIntervals());
        // Test: list = intervals [-inf ; d2[ , [d3 ; d4[ , ]d5 ; d6[
        // -inf d1 d2 d3 d4 d5 d6 +inf
        // ]-------------[
        // [-----[
        // ]-----[
        final AbsoluteDateIntervalsList list12 = new AbsoluteDateIntervalsList();
        list12.add(i2);
        list12.add(i8);
        list12.add(i9);
        expected1 = new AbsoluteDateInterval(this.closed, date2, date3, this.open);
        expected2 = new AbsoluteDateInterval(this.closed, date4, date5, this.closed);
        expected3 = new AbsoluteDateInterval(this.closed, date6, AbsoluteDate.FUTURE_INFINITY,
            IntervalEndpointType.OPEN);
        final AbsoluteDateIntervalsList expectedList12 = new AbsoluteDateIntervalsList();
        expectedList12.add(expected1);
        expectedList12.add(expected2);
        expectedList12.add(expected3);
        Assert.assertEquals(expectedList12, list12.getComplementaryIntervals());
        // Test: list = intervals [-inf ; d2[ , [d1 ; d2]
        // -inf d1 d2 d3 d4 d5 d6 +inf
        // ]-------------[
        // [-----]
        final AbsoluteDateIntervalsList list13 = new AbsoluteDateIntervalsList();
        list13.add(i1);
        list13.add(i2);
        expected1 = new AbsoluteDateInterval(this.open, date2, AbsoluteDate.FUTURE_INFINITY,
            IntervalEndpointType.OPEN);
        final AbsoluteDateIntervalsList expectedList13 = new AbsoluteDateIntervalsList();
        expectedList13.add(expected1);
        Assert.assertEquals(expectedList13, list13.getComplementaryIntervals());
        
        // Check the specific case when getComplementaryIntervals is called when the AbsoluteDateIntervalsList object is
        // empty
        final AbsoluteDateIntervalsList list14 = new AbsoluteDateIntervalsList();
        expected1 = new AbsoluteDateInterval(IntervalEndpointType.OPEN, AbsoluteDate.PAST_INFINITY,
            AbsoluteDate.FUTURE_INFINITY, IntervalEndpointType.OPEN);
        final AbsoluteDateIntervalsList expectedList14 = new AbsoluteDateIntervalsList();
        expectedList14.add(expected1);
        try {
            Assert.assertEquals(expectedList14, list14.getComplementaryIntervals());
        } catch (NoSuchElementException e) {
            // Error case should be avoid
            Assert.fail();
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATION_INTERVALS_LIST_OPERATIONS}
     * 
     * @testedMethod {@link AbsoluteDateIntervalsList#getMergedIntervals()}
     * 
     * @description tests merge function of the date intervals list
     * 
     * @input some {@link AbsoluteDateIntervalsList} and {@link AbsoluteDateInterval}
     * 
     * @output a {@link AbsoluteDateIntervalsList}
     * 
     * @testPassCriteria result is as expected
     * 
     * @referenceVersion 4.2
     * 
     * @nonRegressionVersion 4.2
     */
    @Test
    public final void testGetMergedIntervals() throws PatriusException {
        // Build list
        final AbsoluteDate d1 = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate d2 = AbsoluteDate.J2000_EPOCH.shiftedBy(10);
        final AbsoluteDate d3 = AbsoluteDate.J2000_EPOCH.shiftedBy(20);
        final AbsoluteDate d4 = AbsoluteDate.J2000_EPOCH.shiftedBy(30);
        final AbsoluteDate d5 = AbsoluteDate.J2000_EPOCH.shiftedBy(40);
        final AbsoluteDate d6 = AbsoluteDate.FUTURE_INFINITY;

        final AbsoluteDateInterval i1 = new AbsoluteDateInterval(this.closed, d1, d3, this.open);
        final AbsoluteDateInterval i2 = new AbsoluteDateInterval(this.closed, d2, d4, this.closed);
        final AbsoluteDateInterval i3 = new AbsoluteDateInterval(this.closed, d5, d6, this.open);
        final AbsoluteDateIntervalsList list = new AbsoluteDateIntervalsList();
        list.add(i1);
        list.add(i2);
        list.add(i3);

        // Compute and check
        final AbsoluteDateIntervalsList actual = list.getMergedIntervals();
        final AbsoluteDateIntervalsList expected = new AbsoluteDateIntervalsList();
        expected.add(new AbsoluteDateInterval(this.closed, d1, d4, this.closed));
        expected.add(new AbsoluteDateInterval(this.closed, d5, d6, this.open));
        Assert.assertEquals(expected, actual);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATION_INTERVALS_LIST_OPERATIONS}
     * 
     * @testedMethod {@link AbsoluteDateIntervalsList#getIntersectionWith(AbsoluteDateInterval)}
     * 
     * @description tests intersection function of the date intervals list
     * 
     * @input some {@link AbsoluteDateIntervalsList} and {@link AbsoluteDateInterval}
     * 
     * @output a {@link AbsoluteDateIntervalsList}
     * 
     * @testPassCriteria result is as expected
     * 
     * @referenceVersion 4.2
     * 
     * @nonRegressionVersion 4.2
     */
    @Test
    public final void testGetIntersectionWith() throws PatriusException {
        // Build list
        final AbsoluteDate d1 = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate d2 = AbsoluteDate.J2000_EPOCH.shiftedBy(10);
        final AbsoluteDate d3 = AbsoluteDate.J2000_EPOCH.shiftedBy(20);
        final AbsoluteDate d4 = AbsoluteDate.J2000_EPOCH.shiftedBy(30);
        final AbsoluteDate d5 = AbsoluteDate.J2000_EPOCH.shiftedBy(40);
        final AbsoluteDate d6 = AbsoluteDate.FUTURE_INFINITY;

        final AbsoluteDateInterval i1 = new AbsoluteDateInterval(this.closed, d1, d3, this.open);
        final AbsoluteDateInterval i2 = new AbsoluteDateInterval(this.closed, d2, d4, this.closed);
        final AbsoluteDateInterval i3 = new AbsoluteDateInterval(this.closed, d5, d6, this.open);
        final AbsoluteDateIntervalsList list = new AbsoluteDateIntervalsList();
        list.add(i1);
        list.add(i2);
        list.add(i3);

        // Build outside intervals to check intersection with
        final AbsoluteDate dj1 = AbsoluteDate.J2000_EPOCH.shiftedBy(-20);
        final AbsoluteDate dj2 = AbsoluteDate.J2000_EPOCH.shiftedBy(-10);
        final AbsoluteDate dj3 = AbsoluteDate.J2000_EPOCH.shiftedBy(25);
        final AbsoluteDate dj4 = AbsoluteDate.J2000_EPOCH.shiftedBy(50);
        final AbsoluteDateInterval j1 = new AbsoluteDateInterval(this.closed, dj1, dj2, this.open);
        final AbsoluteDateInterval j2 = new AbsoluteDateInterval(this.closed, dj2, dj3, this.open);
        final AbsoluteDateInterval j3 = new AbsoluteDateInterval(this.closed, dj1, dj4, this.open);

        // Compute and check
        // No intersection
        final AbsoluteDateIntervalsList actual1 = list.getIntersectionWith(j1);
        final AbsoluteDateIntervalsList expected1 = new AbsoluteDateIntervalsList();
        Assert.assertEquals(expected1, actual1);

        // Only one intersection
        final AbsoluteDateIntervalsList actual2 = list.getIntersectionWith(j2);
        final AbsoluteDateIntervalsList expected2 = new AbsoluteDateIntervalsList();
        expected2.add(new AbsoluteDateInterval(this.closed, d1, dj3, this.open));
        Assert.assertEquals(expected2, actual2);

        // Whole intersection
        final AbsoluteDateIntervalsList actual3 = list.getIntersectionWith(j3);
        final AbsoluteDateIntervalsList expected3 = new AbsoluteDateIntervalsList();
        expected3.add(new AbsoluteDateInterval(this.closed, d1, d4, this.closed));
        expected3.add(new AbsoluteDateInterval(this.closed, d5, dj4, this.open));
        Assert.assertEquals(expected3, actual3);
    }
    
    /**
     * Tests the method {@link AbsoluteDateIntervalsList#overlaps(AbsoluteDateInterval)}.
     */
    @Test
    public void testOverlaps() {
        
        final AbsoluteDate date1  = new AbsoluteDate("2010-08-20T10:00:00Z", TimeScalesFactory.getTT());
        final AbsoluteDate date2  = new AbsoluteDate("2010-08-20T10:30:00Z", TimeScalesFactory.getTT());
        final AbsoluteDate date3  = new AbsoluteDate("2010-08-20T10:40:00Z", TimeScalesFactory.getTT());
        final AbsoluteDate date4  = new AbsoluteDate("2010-08-20T12:00:00Z", TimeScalesFactory.getTT());
        final AbsoluteDate date5  = new AbsoluteDate("2010-08-20T12:30:00Z", TimeScalesFactory.getTT());
        final AbsoluteDate date6  = new AbsoluteDate("2010-08-20T18:00:00Z", TimeScalesFactory.getTT());
        final AbsoluteDate date7  = new AbsoluteDate("2010-08-20T18:10:00Z", TimeScalesFactory.getTT());
        final AbsoluteDate date8  = new AbsoluteDate("2010-08-20T18:30:00Z", TimeScalesFactory.getTT());
        final AbsoluteDate date9  = new AbsoluteDate("2010-08-20T18:40:00Z", TimeScalesFactory.getTT());
        final AbsoluteDate date10 = new AbsoluteDate("2010-08-20T19:00:00Z", TimeScalesFactory.getTT());
        
        // The AbsoluteDateIntervalsList is created:
        final AbsoluteDateIntervalsList list = new AbsoluteDateIntervalsList();
        // set up the time intervals to add:
        list.add(new AbsoluteDateInterval(this.open, date2, date3, this.open));
        list.add(new AbsoluteDateInterval(this.closed, date6, date7, this.closed));
        
        AbsoluteDateInterval interval;
        // Interval is included
        interval = new AbsoluteDateInterval(this.open, date6, date7, this.closed);
        assertTrue(list.overlaps(interval));

        // Interval is overlapping (on the upper bound side)
        interval = new AbsoluteDateInterval(this.open, date5, date7, this.closed);
        assertTrue(list.overlaps(interval));

        // Interval is overlapping (on the lower bound side)
        interval = new AbsoluteDateInterval(this.closed, date6, date9, this.closed);
        assertTrue(list.overlaps(interval));

        // Interval is not overlapping
        interval = new AbsoluteDateInterval(this.open, date8, date10, this.closed);
        assertFalse(list.overlaps(interval));

        // Interval is not overlapping, but is connected to another interval
        interval = new AbsoluteDateInterval(this.open, date4, date6, this.closed);
        assertTrue(list.overlaps(interval));

        // Interval is almost connected, but its endpoint is open
        interval = new AbsoluteDateInterval(this.open, date1, date2, this.open);
        assertFalse(list.overlaps(interval));
    }

    /**
     * Tests the method {@link AbsoluteDateIntervalsList#includes(AbsoluteDateInterval)}.
     */
    @Test
    public void testIncludes() {
        
        final AbsoluteDate date1 = new AbsoluteDate("2010-08-20T10:00:00Z", TimeScalesFactory.getTT());
        final AbsoluteDate date2 = new AbsoluteDate("2010-08-20T10:30:00Z", TimeScalesFactory.getTT());
        final AbsoluteDate date3 = new AbsoluteDate("2010-08-20T10:40:00Z", TimeScalesFactory.getTT());
        final AbsoluteDate date4 = new AbsoluteDate("2010-08-20T12:00:00Z", TimeScalesFactory.getTT());
        final AbsoluteDate date5 = new AbsoluteDate("2010-08-20T12:30:00Z", TimeScalesFactory.getTT());
        final AbsoluteDate date6 = new AbsoluteDate("2010-08-20T18:10:00Z", TimeScalesFactory.getTT());
        final AbsoluteDate date7 = new AbsoluteDate("2010-08-20T18:30:00Z", TimeScalesFactory.getTT());
        final AbsoluteDate date8 = new AbsoluteDate("2010-08-20T18:40:00Z", TimeScalesFactory.getTT());
        final AbsoluteDate date9 = new AbsoluteDate("2010-08-20T19:00:00Z", TimeScalesFactory.getTT());
        
        // The AbsoluteDateIntervalsList is created:
        final AbsoluteDateIntervalsList list = new AbsoluteDateIntervalsList();
        // set up the time intervals to add:
        list.add(new AbsoluteDateInterval(this.open, date2, date5, this.open));
        list.add(new AbsoluteDateInterval(this.closed, date6, date7, this.closed));

        AbsoluteDateInterval interval;
        // Interval is entirely included
        interval = new AbsoluteDateInterval(this.open, date3, date4, this.closed);
        assertTrue(list.includes(interval));

        // Interval is not included and does not overlaps
        interval = new AbsoluteDateInterval(this.open, date8, date9, this.closed);
        assertFalse(list.includes(interval));

        // Interval is not included, but overlaps
        interval = new AbsoluteDateInterval(this.closed, date1, date3, this.closed);
        assertFalse(list.includes(interval));

        // Interval is not included, but is connected
        interval = new AbsoluteDateInterval(this.closed, date7, date8, this.closed);
        assertFalse(list.includes(interval));

        // Interval is not included, is almost connected, but endpoint is open
        interval = new AbsoluteDateInterval(this.open, date1, date2, this.closed);
        assertFalse(list.includes(interval));
    }
}
