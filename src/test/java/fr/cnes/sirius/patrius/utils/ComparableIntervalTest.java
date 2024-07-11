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
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:1323:13/11/2017:change log message
 * VERSION::DM:1936:19/11/2018: add new methods to intervals classes
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import junit.framework.Assert;

import org.junit.Test;

import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.interval.ComparableInterval;
import fr.cnes.sirius.patrius.math.interval.IntervalEndpointType;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Unit tests for <code>ComparableInterval</code>.<br>
 * All tests will be made with the "Double" type.
 * 
 * @author cardosop
 * 
 * @version $Id: ComparableIntervalTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.0
 */
public class ComparableIntervalTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle COMPARABLE_INTERVAL
         * 
         * @featureDescription implementation of a comparable interval
         * 
         * @coveredRequirements DV-DATES_150, DV-DATES_160
         * 
         */
        COMPARABLE_INTERVAL,
        /**
         * @featureTitle INTERVAL_OVERLAPS
         * 
         * @featureDescription implementation of a method probing interval overlaps
         * 
         * @coveredRequirements DV-DATES_210
         */
        INTERVAL_OVERLAPS,
        /**
         * @featureTitle INTERVAL_INCLUDES
         * 
         * @featureDescription implementation of a method probing interval inclusion
         * 
         * @coveredRequirements DV-DATES_210
         */
        INTERVAL_INCLUDES,
        /**
         * @featureTitle INTERVAL_CONNECTED
         * 
         * @featureDescription implementation of a method probing interval connection
         * 
         * @coveredRequirements DV-DATES_210
         */
        INTERVAL_CONNECTED
    }

    /** The OPEN end point type. */
    private final IntervalEndpointType open = IntervalEndpointType.OPEN;

    /** The CLOSED end point type. */
    private final IntervalEndpointType closed = IntervalEndpointType.CLOSED;

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#COMPARABLE_INTERVAL}
     * 
     * @testedMethod {@link ComparableInterval#ComparableInterval(IntervalEndpointType, Comparable, Comparable, IntervalEndpointType)}
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
    public final void testComparableInterval() {
        final Double lowEnd = new Double(-0.24);
        final Double upEnd = new Double(5.235);
        final ComparableInterval<Double> doubInterval =
            new ComparableInterval<Double>(this.closed, lowEnd, upEnd, this.open);
        assertNotNull(doubInterval);
        // Tests for the getters
        final Double gLowEnd = doubInterval.getLowerData();
        final Double gUpEnd = doubInterval.getUpperData();
        final IntervalEndpointType gLowInt = doubInterval.getLowerEndpoint();
        final IntervalEndpointType gUpInt = doubInterval.getUpperEndpoint();
        assertEquals(lowEnd.doubleValue(), gLowEnd.doubleValue(), 0.);
        assertEquals(upEnd.doubleValue(), gUpEnd.doubleValue(), 0.);
        assertEquals(this.closed, gLowInt);
        assertEquals(this.open, gUpInt);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#COMPARABLE_INTERVAL}
     * 
     * @testedMethod {@link ComparableInterval#ComparableInterval(IntervalEndpointType, Comparable, Comparable, IntervalEndpointType)}
     * 
     * @description unit test for invalid constructor parameters
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
    public final void testInvalidIntervals() {
        // Note the order of values is off
        final Double lowValue = new Double(-0.23e11);
        final Double upValue = new Double(-6.5432e34);
        // Wrong order
        boolean allRight = false;
        try {
            new ComparableInterval<Double>(this.closed, lowValue, upValue, this.open);
        } catch (final MathIllegalArgumentException e) {
            Assert.assertEquals(PatriusMessages.INCORRECT_INTERVAL.getSourceString(), e.getMessage());
            allRight = true;
        }
        assertTrue(allRight);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#COMPARABLE_INTERVAL}
     * 
     * @testedMethod {@link ComparableInterval#hashCode()}
     * 
     * @description tests {@link ComparableInterval#hashCode()}
     * 
     * @input two identical comparable intervals
     * 
     * @output the hash codes of the two intervals
     * 
     * @testPassCriteria hashCode returns the integer for the two intervals
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public final void testHashCode() {
        final Double d1 = new Double(-100.56);
        final Double d2 = new Double(89.36);
        // The ComparableInterval are created:
        final ComparableInterval<Double> interval1 = new ComparableInterval<Double>(this.open, d1, d2, this.closed);
        final ComparableInterval<Double> interval2 = new ComparableInterval<Double>(this.open, d1, d2, this.closed);
        Assert.assertEquals(interval1.hashCode(), interval2.hashCode());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#COMPARABLE_INTERVAL}
     * 
     * @testedMethod {@link ComparableInterval#equals(Object)}
     * 
     * @description tests {@link ComparableInterval#equals(Object)}
     * 
     * @input four comparable intervals
     * 
     * @output boolean values from calls to <code>equals</code>
     * 
     * @testPassCriteria <code>equals</code> returns the expected booleans
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public final void testEquals() {
        final Double d1 = new Double(-100.635);
        final Double d2 = new Double(89.826);
        // The ComparableInterval are created:
        final ComparableInterval<Double> interval1 = new ComparableInterval<Double>(this.open, d1, d2,
            this.closed);
        final ComparableInterval<Double> interval2 = new ComparableInterval<Double>(this.open, d1, d2,
            this.closed);
        Assert.assertTrue(interval1.equals(interval1));
        Assert.assertTrue(interval1.equals(interval2));
        Assert.assertFalse(interval1.equals(new Double(0.2356)));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#COMPARABLE_INTERVAL}
     * 
     * @testedMethod {@link ComparableInterval#contains(Comparable)}
     * 
     * @description unit test for the <code>contains</code> method
     * 
     * @input a <code>ComparableInterval</code> and several <code>Double</code> instances
     * 
     * @output boolean values from calls to <code>contains</code>
     * 
     * @testPassCriteria the boolean values are as expected
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public final void testContains() {
        final Double d1 = new Double(-4.44);
        final Double d2 = new Double(2.22);
        final ComparableInterval<Double> ti1 = new ComparableInterval<Double>(this.closed, d1, d2, this.open);
        final Double inside1 = new Double(-4.44);
        final Double inside2 = new Double(-2.24);
        final Double inside3 = new Double(0);
        final Double inside4 = new Double(2.21);
        final Double outside1 = new Double(-0.2313E96);
        final Double outside2 = new Double(-4.45);
        final Double outside3 = new Double(2.22);
        final Double outside4 = new Double(0.15E150);
        // Tests for ti1
        assertTrue(ti1.contains(inside1));
        assertTrue(ti1.contains(inside2));
        assertTrue(ti1.contains(inside3));
        assertTrue(ti1.contains(inside4));
        assertTrue(!ti1.contains(outside1));
        assertTrue(!ti1.contains(outside2));
        assertTrue(!ti1.contains(outside3));
        assertTrue(!ti1.contains(outside4));
        //
        final Double d3 = new Double(234.5675);
        final Double d4 = new Double(522.23535);
        final ComparableInterval<Double> ti2 = new ComparableInterval<Double>(this.open, d3, d4, this.closed);
        final Double inside21 = new Double(235);
        final Double inside22 = new Double(400);
        final Double inside23 = new Double(500);
        final Double inside24 = new Double(522.23535);
        final Double outside21 = new Double(-0.73883E112);
        final Double outside22 = new Double(234.5675);
        final Double outside23 = new Double(523);
        final Double outside24 = new Double(2.334E66);
        // Tests for ti2
        assertTrue(ti2.contains(inside21));
        assertTrue(ti2.contains(inside22));
        assertTrue(ti2.contains(inside23));
        assertTrue(ti2.contains(inside24));
        assertTrue(!ti2.contains(outside21));
        assertTrue(!ti2.contains(outside22));
        assertTrue(!ti2.contains(outside23));
        assertTrue(!ti2.contains(outside24));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#INTERVAL_OVERLAPS}
     * 
     * @testedMethod {@link ComparableInterval#overlaps(ComparableInterval)}
     * 
     * @description unit test for the <code>overlaps</code> method
     * 
     * @input several <code>ComparableInterval</code> instances
     * 
     * @output boolean values from calls to <code>overlaps</code>
     * 
     * @testPassCriteria the boolean values are as expected
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public final void testOverlaps() {
        final Double d1 = new Double(-234234.46);
        final Double d2 = new Double(0.);
        final Double d3 = new Double(14785.720);
        final Double d4 = new Double(234495.96830);
        // Test : two non-overlapping intervals, closed
        // [ d1 ; d2 ]...[ d3 ; d4 ]
        final ComparableInterval<Double> nonOv1 = new ComparableInterval<Double>(d1, d2); // [CLOSED, CLOSED]
        final ComparableInterval<Double> nonOv2 = new ComparableInterval<Double>(this.closed, d3, d4, this.closed);
        assertTrue(!nonOv1.overlaps(nonOv2));
        assertTrue(!nonOv2.overlaps(nonOv1));
        // Test : two non-overlapping intervals, open
        // ] d1 ; d2 [...] d3 ; d4 [
        final ComparableInterval<Double> nonOvo1 = new ComparableInterval<Double>(this.open, d1, d2, this.open);
        final ComparableInterval<Double> nonOvo2 = new ComparableInterval<Double>(this.open, d3, d4, this.open);
        assertTrue(!nonOvo1.overlaps(nonOvo2));
        assertTrue(!nonOvo2.overlaps(nonOvo1));
        // Test : two non-overlapping intervals, with common endpoint
        // ] d1 ; d2 [...[ d2 ; d4 [
        final ComparableInterval<Double> nonOvc1 = new ComparableInterval<Double>(this.open, d1, d2, this.open);
        final ComparableInterval<Double> nonOvc2 = new ComparableInterval<Double>(this.closed, d2, d4, this.open);
        assertTrue(!nonOvc1.overlaps(nonOvc2));
        assertTrue(!nonOvc2.overlaps(nonOvc1));
        // Test : two other non-overlapping intervals, with common endpoint
        // ] d1 ; d3 ]...] d3 ; d4 [
        final ComparableInterval<Double> nonOovc1 = new ComparableInterval<Double>(this.open, d1, d3, this.closed);
        final ComparableInterval<Double> nonOovc2 = new ComparableInterval<Double>(this.open, d3, d4, this.open);
        assertTrue(!nonOovc1.overlaps(nonOovc2));
        assertTrue(!nonOovc2.overlaps(nonOovc1));
        // Test : two overlapping intervals, with common endpoint
        // ] d1 ; d2 ]...[ d2 ; d4 [
        final ComparableInterval<Double> ovc1 = new ComparableInterval<Double>(this.open, d1, d2, this.closed);
        final ComparableInterval<Double> ovc2 = new ComparableInterval<Double>(this.closed, d2, d4, this.open);
        assertTrue(ovc1.overlaps(ovc2));
        assertTrue(ovc2.overlaps(ovc1));
        // Test : two open overlapping intervals
        // ] d1 ; d3 [
        // ...] d2 ; d4 [
        final ComparableInterval<Double> ovo1 = new ComparableInterval<Double>(this.open, d1, d3, this.open);
        final ComparableInterval<Double> ovo2 = new ComparableInterval<Double>(this.open, d2, d4, this.open);
        assertTrue(ovo1.overlaps(ovo2));
        assertTrue(ovo2.overlaps(ovo1));
        // Test : two open overlapping intervals, one inside another
        // ] d1 ...;... d4 [
        // ...] d2 ; d3 [
        final ComparableInterval<Double> ovoi1 = new ComparableInterval<Double>(this.open, d1, d4, this.open);
        final ComparableInterval<Double> ovoi2 = new ComparableInterval<Double>(this.open, d2, d3, this.open);
        assertTrue(ovoi1.overlaps(ovoi2));
        assertTrue(ovoi2.overlaps(ovoi1));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#INTERVAL_INCLUDES}
     * 
     * @testedMethod {@link ComparableInterval#includes(ComparableInterval)}
     * 
     * @description unit test for the <code>includes</code> method
     * 
     * @input several <code>ComparableInterval</code> instances
     * 
     * @output boolean values from calls to <code>includes</code>
     * 
     * @testPassCriteria the boolean values are as expected
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public final void testIncludes() {
        final Double d1 = new Double(-20.2128);
        final Double d2 = new Double(6.55957);
        final Double d3 = new Double(200.482);
        final Double d4 = new Double(12345.67890);
        // Test : two non-overlapping intervals, closed
        // [ d1 ; d2 ]...[ d3 ; d4 ]
        final ComparableInterval<Double> nonOv1 = new ComparableInterval<Double>(this.closed, d1, d2, this.closed);
        final ComparableInterval<Double> nonOv2 = new ComparableInterval<Double>(this.closed, d3, d4, this.closed);
        assertTrue(!nonOv1.includes(nonOv2));
        assertTrue(!nonOv2.includes(nonOv1));
        // Test : two closed intervals, first includes second
        // [ d1 .....;..... d4 ]
        // .....[ d2 ; d3 ]
        final ComparableInterval<Double> clos1 = new ComparableInterval<Double>(this.closed, d1, d4, this.closed);
        final ComparableInterval<Double> clos2 = new ComparableInterval<Double>(this.closed, d2, d3, this.closed);
        assertTrue(clos1.includes(clos2));
        assertTrue(!clos2.includes(clos1));
        // Test : two open intervals, first includes second
        // ] d1 .....;..... d4 [
        // .....] d2 ; d3 [
        final ComparableInterval<Double> open1 = new ComparableInterval<Double>(this.open, d1, d4, this.open);
        final ComparableInterval<Double> open2 = new ComparableInterval<Double>(this.open, d2, d3, this.open);
        assertTrue(open1.includes(open2));
        assertTrue(!open2.includes(open1));
        // Test : an open and a closed interval, first includes second
        // ] d1 .....;..... d4 [
        // .....[ d2 ; d3 ]
        final ComparableInterval<Double> clop1 = new ComparableInterval<Double>(this.open, d1, d4, this.open);
        final ComparableInterval<Double> clop2 = new ComparableInterval<Double>(this.closed, d2, d3, this.closed);
        assertTrue(clop1.includes(clop2));
        assertTrue(!clop2.includes(clop1));
        // Test : two identical open intervals
        // ] d2 ; d4 [
        // ] d2 ; d4 [
        final ComparableInterval<Double> samo1 = new ComparableInterval<Double>(this.open, d2, d4, this.open);
        final ComparableInterval<Double> samo2 = new ComparableInterval<Double>(this.open, d2, d4, this.open);
        assertTrue(samo1.includes(samo2));
        assertTrue(samo2.includes(samo1));
        // Test : two identical closed intervals
        // [ d2 ; d4 ]
        // [ d2 ; d4 ]
        final ComparableInterval<Double> samc1 = new ComparableInterval<Double>(this.closed, d2, d4, this.closed);
        final ComparableInterval<Double> samc2 = new ComparableInterval<Double>(this.closed, d2, d4, this.closed);
        assertTrue(samc1.includes(samc2));
        assertTrue(samc2.includes(samc1));
        // Test : two intervals with same endpoints, one closed, one open
        // [ d2 ; d4 ]
        // ] d2 ; d4 [
        assertTrue(samc1.includes(samo2));
        assertTrue(!samo2.includes(samc1));
        // Test : half-closed interval + open interval , first includes second
        // [ d1 ...;... d3 [
        // ] d1 ; d2 [
        final ComparableInterval<Double> hci1 = new ComparableInterval<Double>(this.closed, d1, d3, this.open);
        final ComparableInterval<Double> hci2 = new ComparableInterval<Double>(this.open, d1, d2, this.open);
        assertTrue(hci1.includes(hci2));
        assertTrue(!hci2.includes(hci1));
        // Test : half-closed interval + open interval , neither includes neither
        // [ d1 ...;... d3 [
        // ] d1 .......;....... d4 [
        final ComparableInterval<Double> hci3 = new ComparableInterval<Double>(this.open, d1, d4, this.open);
        assertTrue(!hci1.includes(hci3));
        assertTrue(!hci3.includes(hci1));
        // Test : two open overlapping intervals
        // ] d1 ; d3 [
        // ...] d2 ; d4 [
        final ComparableInterval<Double> ovo1 = new ComparableInterval<Double>(this.open, d1, d3, this.open);
        final ComparableInterval<Double> ovo2 = new ComparableInterval<Double>(this.open, d2, d4, this.open);
        assertTrue(!ovo1.includes(ovo2));
        assertTrue(!ovo2.includes(ovo1));
        // Test : open interval and one-valued interval
        // ] d1 ..;.. d3 [
        // [ d1 ; d1 [
        final ComparableInterval<Double> onev1 = new ComparableInterval<Double>(this.closed, d1, d1, this.open);
        assertTrue(!ovo1.includes(onev1));
        assertTrue(!onev1.includes(ovo1));
        // Test : half-closed interval and one-valued interval
        // [ d1 ..;.. d3 [
        // [ d1 ; d1 [
        assertTrue(hci1.includes(onev1));
        assertTrue(!onev1.includes(hci1));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#INTERVAL_CONNECTED}
     * 
     * @testedMethod {@link ComparableInterval#isConnectedTo(ComparableInterval)}
     * 
     * @description unit test for the <code>isConnectedTo</code> method
     * 
     * @input several <code>ComparableInterval</code> instances
     * 
     * @output boolean values from calls to <code>isConnectedTo</code>
     * 
     * @testPassCriteria the boolean values are as expected
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public final void testIsConnectedTo() {
        final Double d1 = new Double(-50.23);
        final Double d2 = new Double(-0.1);
        final Double d3 = new Double(20);
        final Double d4 = new Double(12345.67890);
        // Test : two different intervals
        // ] d1 ; d2 [
        // [ d3 ; d4 ]
        final ComparableInterval<Double> i11 = new ComparableInterval<Double>(this.open, d1, d2, this.open);
        final ComparableInterval<Double> i12 = new ComparableInterval<Double>(this.closed, d3, d4, this.closed);
        assertTrue(!i11.isConnectedTo(i12));
        assertTrue(!i12.isConnectedTo(i11));
        // Test : two identical intervals, one open + one closed
        // [ d2 ; d4 ]
        // ] d2 ; d4 [
        final ComparableInterval<Double> i21 = new ComparableInterval<Double>(this.closed, d2, d4, this.closed);
        final ComparableInterval<Double> i22 = new ComparableInterval<Double>(this.open, d2, d4, this.open);
        assertTrue(!i21.isConnectedTo(i22));
        assertTrue(!i22.isConnectedTo(i21));
        // Test : two different intervals, same end point (OPEN - OPEN)
        // [ d1 ; d3 [
        // ] d3 ; d4 [
        final ComparableInterval<Double> i31 = new ComparableInterval<Double>(this.closed, d1, d3, this.open);
        final ComparableInterval<Double> i32 = new ComparableInterval<Double>(this.open, d3, d4, this.open);
        assertTrue(!i31.isConnectedTo(i32));
        assertTrue(!i32.isConnectedTo(i31));
        // Test : two different intervals, same end point (OPEN - CLOSED)
        // [ d1 ; d3 [
        // [ d3 ; d4 [
        final ComparableInterval<Double> i41 = new ComparableInterval<Double>(this.closed, d1, d3, this.open);
        final ComparableInterval<Double> i42 = new ComparableInterval<Double>(this.closed, d3, d4, this.open);
        assertTrue(!i41.isConnectedTo(i42));
        assertTrue(i42.isConnectedTo(i41));
        // Test : two different intervals, one included in the other, same end point
        // [ d1 ; d4 [
        // [ d3 ; d4 ]
        final ComparableInterval<Double> i51 = new ComparableInterval<Double>(this.closed, d1, d4, this.open);
        final ComparableInterval<Double> i52 = new ComparableInterval<Double>(this.closed, d3, d4, this.closed);
        assertTrue(!i51.isConnectedTo(i52));
        assertTrue(!i52.isConnectedTo(i51));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#COMPARABLE_INTERVAL}
     * 
     * @testedMethod {@link ComparableInterval#compareLowerEndTo(ComparableInterval)}
     * 
     * @description unit test for the <code>compareLowerEndTo</code> method
     * 
     * @input two <code>ComparableInterval</code>
     * 
     * @output Integer values from calls to <code>compareLowerEndTo</code>
     * 
     * @testPassCriteria the Integer values are as expected
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public final void testCompareLowerEndTo() {
        final Double d1 = new Double(-25.77);
        final Double d2 = new Double(-3.56);
        final Double d3 = new Double(400);

        // Test : two different intervals
        // ] d1 ; d2 [
        // [ d2 ; d3 ]
        ComparableInterval<Double> i1 = new ComparableInterval<Double>(this.open, d1, d2, this.open);
        ComparableInterval<Double> i2 = new ComparableInterval<Double>(this.closed, d2, d3, this.closed);
        assertEquals(-1, i1.compareLowerEndTo(i2));
        assertEquals(1, i2.compareLowerEndTo(i1));
        // Test : two different intervals, common lower end point (CLOSED + CLOSED)
        // [ d1 ; d2 [
        // [ d1 ; d3 [
        i1 = new ComparableInterval<Double>(this.closed, d1, d2, this.open);
        i2 = new ComparableInterval<Double>(this.closed, d1, d3, this.open);
        assertEquals(0, i1.compareLowerEndTo(i2));
        assertEquals(0, i2.compareLowerEndTo(i1));
        // Test : two different intervals, common lower end point (CLOSED + OPEN)
        // [ d1 ; d2 [
        // ] d1 ; d3 [
        i1 = new ComparableInterval<Double>(this.closed, d1, d2, this.open);
        i2 = new ComparableInterval<Double>(this.open, d1, d3, this.open);
        assertEquals(-1, i1.compareLowerEndTo(i2));
        assertEquals(1, i2.compareLowerEndTo(i1));
        // Test : two different intervals, common upper end point (OPEN + OPEN)
        // [ d2 ; d3 [
        // ] d1 ; d3 [
        i1 = new ComparableInterval<Double>(this.closed, d2, d3, this.open);
        i2 = new ComparableInterval<Double>(this.open, d1, d3, this.open);
        assertEquals(1, i1.compareLowerEndTo(i2));
        assertEquals(-1, i2.compareLowerEndTo(i1));
        // Test : two identical intervals
        // ] d1 ; d3 ]
        // ] d1 ; d3 ]
        i1 = new ComparableInterval<Double>(this.open, d1, d3, this.closed);
        i2 = new ComparableInterval<Double>(this.open, d1, d3, this.closed);
        assertEquals(0, i1.compareLowerEndTo(i2));
        assertEquals(0, i2.compareLowerEndTo(i1));
        // Test : two identical intervals, but lower end point OPEN + CLOSED
        // ] d1 ; d3 ]
        // [ d1 ; d3 ]
        i1 = new ComparableInterval<Double>(this.open, d1, d3, this.closed);
        i2 = new ComparableInterval<Double>(this.closed, d1, d3, this.closed);
        assertEquals(1, i1.compareLowerEndTo(i2));
        assertEquals(-1, i2.compareLowerEndTo(i1));
        // Test : two identical intervals, but upper end point OPEN + CLOSED
        // [ d1 ; d3 [
        // [ d1 ; d3 ]
        i1 = new ComparableInterval<Double>(this.closed, d1, d3, this.open);
        i2 = new ComparableInterval<Double>(this.closed, d1, d3, this.closed);
        assertEquals(0, i1.compareLowerEndTo(i2));
        assertEquals(0, i2.compareLowerEndTo(i1));
        // Test : two identical intervals, but lower end point OPEN + CLOSED and upper end point CLOSED + OPEN
        // ] d1 ; d3 ]
        // [ d1 ; d3 [
        i1 = new ComparableInterval<Double>(this.open, d1, d3, this.closed);
        i2 = new ComparableInterval<Double>(this.closed, d1, d3, this.open);
        assertEquals(1, i1.compareLowerEndTo(i2));
        assertEquals(-1, i2.compareLowerEndTo(i1));
        // Test : one interval
        i1 = new ComparableInterval<Double>(this.closed, d1, d2, this.closed);
        assertEquals(0, i1.compareLowerEndTo(i1));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#COMPARABLE_INTERVAL}
     * 
     * @testedMethod {@link ComparableInterval#compareUpperEndTo(ComparableInterval)}
     * 
     * @description unit test for the <code>compareUpperEndTo</code> method
     * 
     * @input two <code>ComparableInterval</code>
     * 
     * @output Integer values from calls to <code>compareUpperEndTo</code>
     * 
     * @testPassCriteria the Integer values are as expected
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public final void testCompareUpperEndTo() {
        final Double d1 = new Double(-25.77);
        final Double d2 = new Double(-3.56);
        final Double d3 = new Double(400);

        // Test : two different intervals
        // ] d1 ; d2 [
        // [ d2 ; d3 ]
        ComparableInterval<Double> i1 = new ComparableInterval<Double>(this.open, d1, d2, this.open);
        ComparableInterval<Double> i2 = new ComparableInterval<Double>(this.closed, d2, d3, this.closed);
        assertEquals(-1, i1.compareUpperEndTo(i2));
        assertEquals(1, i2.compareUpperEndTo(i1));
        // Test : two different intervals, common lower end point (CLOSED + CLOSED)
        // [ d1 ; d2 [
        // [ d1 ; d3 [
        i1 = new ComparableInterval<Double>(this.closed, d1, d2, this.open);
        i2 = new ComparableInterval<Double>(this.closed, d1, d3, this.open);
        assertEquals(-1, i1.compareUpperEndTo(i2));
        assertEquals(1, i2.compareUpperEndTo(i1));
        // Test : two different intervals, common upper end point (CLOSED + OPEN)
        // [ d2 ; d3 ]
        // ] d1 ; d3 [
        i1 = new ComparableInterval<Double>(this.closed, d2, d3, this.closed);
        i2 = new ComparableInterval<Double>(this.open, d1, d3, this.open);
        assertEquals(1, i1.compareUpperEndTo(i2));
        assertEquals(-1, i2.compareUpperEndTo(i1));
        // Test : two different intervals, common upper end point (OPEN + OPEN)
        // [ d2 ; d3 [
        // ] d1 ; d3 [
        i1 = new ComparableInterval<Double>(this.closed, d2, d3, this.open);
        i2 = new ComparableInterval<Double>(this.open, d1, d3, this.open);
        assertEquals(0, i1.compareUpperEndTo(i2));
        assertEquals(0, i2.compareUpperEndTo(i1));
        // Test : two identical intervals
        // ] d1 ; d3 ]
        // ] d1 ; d3 ]
        i1 = new ComparableInterval<Double>(this.open, d1, d3, this.closed);
        i2 = new ComparableInterval<Double>(this.open, d1, d3, this.closed);
        assertEquals(0, i1.compareUpperEndTo(i2));
        assertEquals(0, i2.compareUpperEndTo(i1));
        // Test : two identical intervals, but upper end point OPEN + CLOSED
        // ] d1 ; d3 [
        // [ d1 ; d3 ]
        i1 = new ComparableInterval<Double>(this.open, d1, d3, this.open);
        i2 = new ComparableInterval<Double>(this.closed, d1, d3, this.closed);
        assertEquals(-1, i1.compareUpperEndTo(i2));
        // Test : two identical intervals, but lower end point OPEN + CLOSED and upper end point CLOSED + OPEN
        // ] d1 ; d3 ]
        // [ d1 ; d3 [
        i1 = new ComparableInterval<Double>(this.open, d1, d3, this.closed);
        i2 = new ComparableInterval<Double>(this.closed, d1, d3, this.open);
        assertEquals(1, i1.compareUpperEndTo(i2));
        assertEquals(-1, i2.compareUpperEndTo(i1));
        // Test : one interval
        i1 = new ComparableInterval<Double>(this.closed, d1, d2, this.closed);
        assertEquals(0, i1.compareUpperEndTo(i1));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#COMPARABLE_INTERVAL}
     * 
     * @testedMethod {@link ComparableInterval#compareTo(ComparableInterval)}
     * 
     * @description unit test for the <code>compareTo</code> method
     * 
     * @input two <code>ComparableInterval</code>
     * 
     * @output Integer values from calls to <code>compareTo</code>
     * 
     * @testPassCriteria the Integer values are as expected
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public final void testCompareTo() {
        final Double d1 = new Double(-50.23);
        final Double d2 = new Double(-0.1);
        final Double d3 = new Double(20);
        final Double d4 = new Double(12345.67890);

        // Test : two different disjoint intervals
        // ] d1 ; d2 [
        // [ d3 ; d4 ]
        ComparableInterval<Double> i1 = new ComparableInterval<Double>(this.open, d1, d2, this.open);
        ComparableInterval<Double> i2 = new ComparableInterval<Double>(this.closed, d3, d4, this.closed);
        assertEquals(-1, i1.compareTo(i2));
        assertEquals(1, i2.compareTo(i1));
        // Test : two different intervals, common lower end point (CLOSED + CLOSED)
        // [ d1 ; d2 [
        // [ d1 ; d3 [
        i1 = new ComparableInterval<Double>(this.closed, d1, d2, this.open);
        i2 = new ComparableInterval<Double>(this.closed, d1, d3, this.open);
        assertEquals(-1, i1.compareTo(i2));
        assertEquals(1, i2.compareTo(i1));
        // Test : two different intervals, common lower end point (CLOSED + OPEN)
        // [ d1 ; d2 [
        // ] d1 ; d3 [
        i1 = new ComparableInterval<Double>(this.closed, d1, d2, this.open);
        i2 = new ComparableInterval<Double>(this.open, d1, d3, this.open);
        assertEquals(-1, i1.compareTo(i2));
        assertEquals(1, i2.compareTo(i1));
        // Test : two different intervals, common upper end point (OPEN + OPEN)
        // [ d2 ; d3 [
        // ] d1 ; d3 [
        i1 = new ComparableInterval<Double>(this.closed, d2, d3, this.open);
        i2 = new ComparableInterval<Double>(this.open, d1, d3, this.open);
        assertEquals(1, i1.compareTo(i2));
        assertEquals(-1, i2.compareTo(i1));
        // Test : two overlapping intervals
        // [ d1 ; d3 [
        // ] d2 ; d4 [
        i1 = new ComparableInterval<Double>(this.closed, d1, d3, this.open);
        i2 = new ComparableInterval<Double>(this.open, d2, d4, this.open);
        assertEquals(-1, i1.compareTo(i2));
        assertEquals(1, i2.compareTo(i1));
        // Test : two intervals, one included in the other:
        // [ d1 ; d4 [
        // ] d2 ; d3 [
        i1 = new ComparableInterval<Double>(this.closed, d1, d4, this.open);
        i2 = new ComparableInterval<Double>(this.open, d2, d3, this.open);
        assertEquals(-1, i1.compareTo(i2));
        assertEquals(1, i2.compareTo(i1));
        // Test : two identical intervals
        // ] d1 ; d3 ]
        // ] d1 ; d3 ]
        i1 = new ComparableInterval<Double>(this.open, d1, d3, this.closed);
        i2 = new ComparableInterval<Double>(this.open, d1, d3, this.closed);
        assertEquals(0, i1.compareTo(i2));
        assertEquals(0, i2.compareTo(i1));
        // Test : two identical intervals, but lower end point OPEN + CLOSED
        // ] d1 ; d3 ]
        // [ d1 ; d3 ]
        i1 = new ComparableInterval<Double>(this.open, d1, d3, this.closed);
        i2 = new ComparableInterval<Double>(this.closed, d1, d3, this.closed);
        assertEquals(1, i1.compareTo(i2));
        assertEquals(-1, i2.compareTo(i1));
        // Test : two identical intervals, but upper end point OPEN + CLOSED
        // [ d1 ; d3 [
        // [ d1 ; d3 ]
        i1 = new ComparableInterval<Double>(this.closed, d1, d3, this.open);
        i2 = new ComparableInterval<Double>(this.closed, d1, d3, this.closed);
        assertEquals(-1, i1.compareTo(i2));
        assertEquals(1, i2.compareTo(i1));
        // Test : two identical intervals, but lower end point OPEN + CLOSED and upper end point CLOSED + OPEN
        // ] d1 ; d3 ]
        // [ d1 ; d3 [
        i1 = new ComparableInterval<Double>(this.open, d1, d3, this.closed);
        i2 = new ComparableInterval<Double>(this.closed, d1, d3, this.open);
        assertEquals(1, i1.compareTo(i2));
        assertEquals(-1, i2.compareTo(i1));
        // Test : one interval
        i1 = new ComparableInterval<Double>(this.closed, d1, d2, this.closed);
        assertEquals(0, i1.compareTo(i1));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#COMPARABLE_INTERVAL}
     * 
     * @testedMethod {@link ComparableInterval#extendTo(Comparable)}
     * 
     * @description unit test for the <code>extendTo</code> method
     * 
     * @input one <code>ComparableInterval</code> and one <code>Comparable</code>
     * 
     * @output extended <code>ComparableInterval</code>
     * 
     * @testPassCriteria the extended interval is as expected
     * 
     * @referenceVersion 4.2
     * 
     * @nonRegressionVersion 4.2
     */
    @Test
    public final void testExtendTo() {
        final Double d1 = new Double(-50.2);
        final Double d2 = new Double(-0.1);
        final ComparableInterval<Double> interval = new ComparableInterval<Double>(this.open, d1, d2, this.closed);
        // Left extension
        final ComparableInterval<Double> actual1 = interval.extendTo(-60.);
        Assert.assertEquals(0, actual1.compareTo(new ComparableInterval<Double>(this.open, -60., -0.1, this.closed)));
        // Right extension
        final ComparableInterval<Double> actual2 = interval.extendTo(60.);
        Assert.assertEquals(0, actual2.compareTo(new ComparableInterval<Double>(this.open, -50.2, 60., this.closed)));
        // No extension
        final ComparableInterval<Double> actual3 = interval.extendTo(-10.);
        Assert.assertEquals(0, actual3.compareTo(new ComparableInterval<Double>(this.open, -50.2, -0.1, this.closed)));
    }

}
