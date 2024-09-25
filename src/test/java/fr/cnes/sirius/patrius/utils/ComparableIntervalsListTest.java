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
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1936:23/10/2018: add new methods to intervals classes
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import org.junit.Test;

import fr.cnes.sirius.patrius.math.interval.ComparableInterval;
import fr.cnes.sirius.patrius.math.interval.ComparableIntervalsList;
import fr.cnes.sirius.patrius.math.interval.IntervalEndpointType;

/**
 * JUnit tests for {@link ComparableIntervalsList}.
 *
 * @author GMV
 */
public class ComparableIntervalsListTest {
    // ————— Constants

    /** Positive infinity. */
    private static final Double posinf = Double.POSITIVE_INFINITY;
    /** Negative infinity. */
    private static final Double neginf = Double.NEGATIVE_INFINITY;

    /** Closed interval bound type. */
    private static final IntervalEndpointType CLS = IntervalEndpointType.CLOSED;
    /** Open interval bound type. */
    private static final IntervalEndpointType OPN = IntervalEndpointType.OPEN;

    // ————— Intervals

    /** Intervals. */
    private static final ComparableInterval<Double> interval00;
    private static final ComparableInterval<Double> interval01;
    private static final ComparableInterval<Double> interval02;
    private static final ComparableInterval<Double> interval03;
    private static final ComparableInterval<Double> interval04;
    private static final ComparableInterval<Double> interval05;
    private static final ComparableInterval<Double> interval06;
    private static final ComparableInterval<Double> interval07;
    private static final ComparableInterval<Double> interval08;

    static {
        interval00 = new ComparableInterval<>(OPN, -100.0, +100.0, OPN);
        interval01 = new ComparableInterval<>(OPN, neginf, -900.0, OPN);
        interval02 = new ComparableInterval<>(OPN, +900.0, posinf, OPN);
        interval03 = new ComparableInterval<>(CLS, -700.0, -200.0, CLS);
        interval04 = new ComparableInterval<>(CLS, +200.0, +700.0, CLS);
        interval05 = new ComparableInterval<>(OPN, -900.0, -500.0, OPN);
        interval06 = new ComparableInterval<>(OPN, +500.0, +900.0, OPN);
        interval07 = new ComparableInterval<>(OPN, -050.0, +000.0, OPN);
        interval08 = new ComparableInterval<>(OPN, +000.0, +050.0, OPN);
    }

    private static final ComparableIntervalsList<Double> intervals = new ComparableIntervalsList<>();

    static {
        intervals.add(interval00);
        intervals.add(interval01);
        intervals.add(interval02);
        intervals.add(interval03);
        intervals.add(interval04);
        intervals.add(interval05);
        intervals.add(interval06);
        intervals.add(interval07);
        intervals.add(interval08);
    }

    /**
     * Tests the equality between two lists.
     * <p>
     * The list containing all the intervals is compared to a list where all the intervals have been added in the
     * reverse order, twice. This ensure that duplicates are actually removed and that the insertion order does not
     * matter.
     * </p>
     */
    @Test
    public void testEquality() {
        final ComparableIntervalsList<Double> list = new ComparableIntervalsList<>();

        for (int i = 0; i < 2; i++) {
            list.add(interval08);
            list.add(interval07);
            list.add(interval06);
            list.add(interval05);
            list.add(interval04);
            list.add(interval03);
            list.add(interval02);
            list.add(interval01);
            list.add(interval00);
        }

        assertEquals(intervals, list);
    }

    /**
     * Tests the list ordering.
     * <p>
     * The intervals must be sorted according to the lower bound first and then according to the upper bound. This test
     * really only ensures that the class is indeed ordered.
     * </p>
     */
    @Test
    public void testOrdering() {
        // Interval list iterator
        final Iterator<ComparableInterval<Double>> iterator = intervals.iterator();

        // Set the previous interval as the first interval
        ComparableInterval<Double> previous = iterator.next();

        // Ensure that the following interval are ordered
        while (iterator.hasNext()) {
            final ComparableInterval<Double> interval = iterator.next();
            assert (interval.compareTo(previous) >= 0);
            previous = interval;
        }
    }

    /**
     * Tests the method {@link ComparableIntervalsList#getInclusiveInterval()}.
     */
    @Test
    public void testInclusiveInterval() {
        ComparableInterval<Double> computed;
        ComparableInterval<Double> expected;

        final ComparableIntervalsList<Double> initial = new ComparableIntervalsList<>();

        // - Intervals have infinite bounds
        // --------------------------------

        initial.clear();
        initial.add(interval01);
        initial.add(interval02);

        computed = initial.getInclusiveInterval();
        expected = new ComparableInterval<>(OPN, neginf, posinf, OPN);

        assertEquals(expected, computed);

        // - Intervals are not overlapping
        // -------------------------------

        initial.clear();
        initial.add(interval00);
        initial.add(interval03);

        computed = initial.getInclusiveInterval();
        expected = new ComparableInterval<>(CLS, -700.0, +100.0, OPN);

        assertEquals(expected, computed);

        // - Intervals are overlapping
        // ---------------------------

        initial.clear();
        initial.add(interval03);
        initial.add(interval05);

        computed = initial.getInclusiveInterval();
        expected = new ComparableInterval<>(OPN, -900.0, -200.0, CLS);

        assertEquals(expected, computed);

        // - Intervals are included
        // ------------------------

        initial.clear();
        initial.add(interval00);
        initial.add(interval07);
        initial.add(interval08);

        computed = initial.getInclusiveInterval();
        expected = new ComparableInterval<>(OPN, -100.0, +100.0, OPN);

        assertEquals(expected, computed);
    }

    /**
     * Tests the method {@link ComparableIntervalsList#overlaps(ComparableInterval)}.
     */
    @Test
    public void testOverlaps() {
        ComparableInterval<Double> interval;

        // Interval is included
        interval = new ComparableInterval<>(CLS, -40.0, +40.0, CLS);
        assertTrue(intervals.overlaps(interval));

        // Interval is overlapping (on the upper bound side)
        interval = new ComparableInterval<>(CLS, -150.0, -50.0, CLS);
        assertTrue(intervals.overlaps(interval));

        // Interval is overlapping (on the lower bound side)
        interval = new ComparableInterval<>(CLS, +50.0, +150.0, CLS);
        assertTrue(intervals.overlaps(interval));

        // Interval is not overlapping
        interval = new ComparableInterval<>(CLS, -180.0, -120.0, CLS);
        assertTrue(!intervals.overlaps(interval));

        // Interval is not overlapping, but is connected to another interval
        interval = new ComparableInterval<>(CLS, -200.0, -100.0, CLS);
        assertTrue(intervals.overlaps(interval));

        // Interval is almost connected, but its endpoint is open
        interval = new ComparableInterval<>(OPN, -200.0, -100.0, OPN);
        assertTrue(!intervals.overlaps(interval));
    }

    /**
     * Tests the method {@link ComparableIntervalsList#includes(ComparableInterval)}.
     */
    @Test
    public void testIncludes() {
        ComparableInterval<Double> interval;

        // Interval is entirely included
        interval = new ComparableInterval<>(CLS, +20.0, +40.0, CLS);
        assertTrue(intervals.includes(interval));

        // Interval is not included and does not overlaps
        interval = new ComparableInterval<>(CLS, -180.0, -120.0, CLS);
        assertTrue(!intervals.includes(interval));

        // Interval is not included, but overlaps
        interval = new ComparableInterval<>(CLS, -120.0, +0.0, CLS);
        assertTrue(!intervals.includes(interval));

        // Interval is not included, but is connected
        interval = new ComparableInterval<>(CLS, -200.0, -120.0, CLS);
        assertTrue(!intervals.includes(interval));

        // Interval is not included, is almost connected, but endpoint is open
        interval = new ComparableInterval<>(OPN, -200.0, -120.0, CLS);
        assertTrue(!intervals.includes(interval));
    }

    /**
     * Tests the method {@link ComparableIntervalsList#getMergedIntervals()}.
     */
    @Test
    public void testMerging() {
        ComparableIntervalsList<Double> computed;

        final ComparableIntervalsList<Double> initial = new ComparableIntervalsList<>();
        final ComparableIntervalsList<Double> expected = new ComparableIntervalsList<>();

        // - Merge all the intervals
        // -------------------------

        initial.clear();
        initial.addAll(intervals);

        computed = initial.getMergedIntervals();

        expected.clear();
        expected.add(new ComparableInterval<>(OPN, neginf, -900.0, OPN));
        expected.add(new ComparableInterval<>(OPN, -900.0, -200.0, CLS));
        expected.add(new ComparableInterval<>(OPN, -100.0, +100.0, OPN));
        expected.add(new ComparableInterval<>(CLS, +200.0, +900.0, OPN));
        expected.add(new ComparableInterval<>(OPN, +900.0, posinf, OPN));

        assertEquals(expected, computed);

        // - Merge intervals that do not overlap
        // -------------------------------------

        initial.clear();
        initial.add(interval00);
        initial.add(interval01);
        initial.add(interval02);

        computed = initial.getMergedIntervals();

        expected.clear();
        expected.addAll(initial);

        assertEquals(expected, computed);
    }

    /**
     * Tests the method {@link ComparableIntervalsList#getIntervalsContaining(Comparable)}.
     */
    @Test
    public void testContaining() {
        ComparableIntervalsList<Double> computed;

        final ComparableIntervalsList<Double> expected = new ComparableIntervalsList<>();

        // - Value is infinite
        // -------------------

        computed = intervals.getIntervalsContaining(posinf);

        expected.clear();

        assertEquals(expected, computed);

        // - Value is in multiple intervals
        // --------------------------------

        computed = intervals.getIntervalsContaining(-650.0);

        expected.clear();
        expected.add(interval03);
        expected.add(interval05);

        assertEquals(expected, computed);

        // - Value is on an open bound
        // ---------------------------

        computed = intervals.getIntervalsContaining(-500.0);

        expected.clear();
        expected.add(interval03);

        assertEquals(expected, computed);

        // - Value is not in any interval
        // ---------------------------

        computed = intervals.getIntervalsContaining(-150.0);

        expected.clear();

        assertEquals(expected, computed);
    }

    /**
     * Tests the method {@link ComparableIntervalsList#getIntersectionWith(ComparableInterval)}.
     */
    @Test
    public void testIntersection() {
        ComparableInterval<Double> interval;
        ComparableIntervalsList<Double> computed;

        final ComparableIntervalsList<Double> expected = new ComparableIntervalsList<>();

        // - Interval is included in one interval
        // --------------------------------------

        interval = new ComparableInterval<>(CLS, -90.0, -80.0, CLS);
        computed = intervals.getIntersectionWith(interval);

        expected.clear();
        expected.add(interval);

        assertEquals(expected, computed);

        // - Interval is overlapping one interval
        // --------------------------------------

        interval = new ComparableInterval<>(CLS, -150.0, -70.0, CLS);
        computed = intervals.getIntersectionWith(interval);

        expected.clear();
        expected.add(new ComparableInterval<>(OPN, -100.0, -70.0, CLS));

        assertEquals(expected, computed);

        // - Interval is not overlapping any interval
        // ------------------------------------------

        interval = new ComparableInterval<>(CLS, -180.0, -120.0, CLS);
        computed = intervals.getIntersectionWith(interval);

        expected.clear();

        assertEquals(expected, computed);

        // - Interval is connected but not overlapping any interval
        // --------------------------------------------------------

        interval = new ComparableInterval<>(CLS, -200.0, -100.0, CLS);
        computed = intervals.getIntersectionWith(interval);

        expected.clear();
        expected.add(new ComparableInterval<>(CLS, -200.0, -200.0, CLS));

        assertEquals(expected, computed);

        // - Interval is overlapping with multiple intervals
        // -------------------------------------------------

        interval = new ComparableInterval<>(CLS, -20.0, +20.0, CLS);
        computed = intervals.getIntersectionWith(interval);

        expected.clear();
        expected.add(new ComparableInterval<>(CLS, -20.0, +20.0, CLS));

        assertEquals(expected, computed);
    }
}
