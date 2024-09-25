/**
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
 * VERSION:4.13:DM:DM-120:08/12/2023:[PATRIUS] Merge de la branche patrius-for-lotus dans Patrius
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.time;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.testng.Assert;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.interval.IntervalEndpointType;

/**
 * Unit test class for the {@link IntervalMapSearcher} class.
 *
 * @author bonitt
 */
public class IntervalMapSearcherTest {

    @Test
    public void testFeatures() {

        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final double duration = 4 * 3600.0;

        final List<AbsoluteDateInterval> intervals = new ArrayList<>();
        intervals.add(new AbsoluteDateInterval(IntervalEndpointType.OPEN, date, date.shiftedBy(duration),
            IntervalEndpointType.OPEN));
        intervals.add(new AbsoluteDateInterval(IntervalEndpointType.CLOSED, date.shiftedBy(duration * 2),
            date.shiftedBy(duration * 3), IntervalEndpointType.OPEN));
        intervals.add(new AbsoluteDateInterval(date.shiftedBy(duration * 3), date.shiftedBy(duration * 4)));
        intervals.add(new AbsoluteDateInterval(IntervalEndpointType.OPEN, date.shiftedBy(duration * 4),
            date.shiftedBy(duration * 5), IntervalEndpointType.CLOSED));

        final List<Double> objects = Arrays.asList(1., 2., 3., 4.);

        final Map<AbsoluteDateInterval, Double> map = new LinkedHashMap<>();
        for (int i = 0; i < intervals.size(); i++) {
            map.put(intervals.get(i), objects.get(i));
        }

        // Build the searchers using the two constructors (which cover the four constructors)
        final IntervalMapSearcher<Double> searcher = new IntervalMapSearcher<>(intervals, objects);
        final IntervalMapSearcher<Double> searcher2 = new IntervalMapSearcher<>(map);

        // Evaluate the searchers on basic tasks (they should behave the same)
        Assert.assertTrue(searcher.containsData(date.shiftedBy(10.)));
        Assert.assertTrue(searcher2.containsData(date.shiftedBy(10.)));
        Assert.assertFalse(searcher.containsData(date.shiftedBy(-10.)));
        Assert.assertFalse(searcher2.containsData(date.shiftedBy(-10.)));

        Assert.assertEquals(intervals.get(0), searcher.getFirstInterval());
        Assert.assertEquals(intervals.get(0), searcher2.getFirstInterval());

        Assert.assertEquals(intervals.get(3), searcher.getLastInterval());
        Assert.assertEquals(intervals.get(3), searcher2.getLastInterval());

        Assert.assertEquals(intervals, searcher.getIntervals());
        Assert.assertEquals(intervals, searcher2.getIntervals());

        Assert.assertEquals(objects, searcher.getData());
        Assert.assertEquals(objects, searcher2.getData());

        Assert.assertEquals(map, searcher.getIntervalDataAssociation());
        Assert.assertEquals(map, searcher2.getIntervalDataAssociation());

        Assert.assertEquals(1., searcher.getData(date.shiftedBy(10.)));
        Assert.assertEquals(1., searcher2.getData(date.shiftedBy(10.)));

        Assert.assertEquals(2., searcher.getData(date.shiftedBy(duration * 2 + 10.)));
        Assert.assertEquals(2., searcher2.getData(date.shiftedBy(duration * 2 + 10.)));

        Assert.assertEquals(3., searcher.getData(date.shiftedBy(duration * 3 + 10.)));
        Assert.assertEquals(3., searcher2.getData(date.shiftedBy(duration * 3 + 10.)));

        Assert.assertEquals(4., searcher.getData(date.shiftedBy(duration * 4 + 10.)));
        Assert.assertEquals(4., searcher2.getData(date.shiftedBy(duration * 4 + 10.)));

        Assert.assertNull(searcher.getData(date.shiftedBy(-10.), false));
        Assert.assertNull(searcher2.getData(date.shiftedBy(duration * 5 + 10.), false));

        Assert.assertNull(searcher.getData(date.shiftedBy(duration + 10.), false));
        Assert.assertNull(searcher2.getData(date.shiftedBy(duration + 10.), false));

        Assert.assertEquals(4, searcher.size());
        Assert.assertEquals(4, searcher2.size());

        Assert.assertEquals(4, searcher.toArray().length);
        Assert.assertEquals(4, searcher2.toArray().length);

        Assert.assertEquals(intervals.get(0), searcher.iterator().next().getKey());
    }

    @Test
    public void testExceptions() {

        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final double duration = 4 * 3600.0;

        final List<AbsoluteDateInterval> intervals = new ArrayList<>();
        intervals.add(new AbsoluteDateInterval(IntervalEndpointType.OPEN, date, date.shiftedBy(duration),
            IntervalEndpointType.OPEN));
        intervals.add(new AbsoluteDateInterval(IntervalEndpointType.CLOSED, date.shiftedBy(duration * 2),
            date.shiftedBy(duration * 3), IntervalEndpointType.OPEN));
        intervals.add(new AbsoluteDateInterval(date.shiftedBy(duration * 3), date.shiftedBy(duration * 4)));
        intervals.add(new AbsoluteDateInterval(IntervalEndpointType.OPEN, date.shiftedBy(duration * 4),
            date.shiftedBy(duration * 5), IntervalEndpointType.CLOSED));

        final List<Double> objects = Arrays.asList(1., 2., 3., 4.);

        final IntervalMapSearcher<Double> searcher = new IntervalMapSearcher<>(intervals, objects);

        // Try to use an interval list with a different size of the object list (should fail)
        try {
            new IntervalMapSearcher<>(intervals, Arrays.asList(1., 2., 3.));
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            // Expected
            Assert.assertTrue(true);
        }

        // Try to use a null map (should fail)
        try {
            new IntervalMapSearcher<>(null);
            Assert.fail();
        } catch (final NullArgumentException e) {
            // Expected
            Assert.assertTrue(true);
        }

        // Try to use an object list with a null attribute (should fail)
        try {
            new IntervalMapSearcher<>(intervals, Arrays.asList(1., 2., null, 4.));
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // Expected
            Assert.assertTrue(true);
        }

        // Try to use an interval list with overlapping intervals (should fail)
        intervals.add(new AbsoluteDateInterval(IntervalEndpointType.OPEN, date.shiftedBy(duration * 4.2),
            date.shiftedBy(duration * 4.8), IntervalEndpointType.CLOSED));
        try {
            new IntervalMapSearcher<>(intervals, Arrays.asList(1., 2., 3., 4., 5.));
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // Expected
            Assert.assertTrue(true);
        }

        // Try to get a data at a date which doesn't belong to any of the intervals
        // (outside the supported interval [dateInf, dateSup]) (should fail)
        try {
            searcher.getData(date.shiftedBy(-10.));
            Assert.fail();
        } catch (final IllegalStateException e) {
            // Expected
            Assert.assertTrue(true);
        }

        // Try to get a data at a date which doesn't belong to any of the intervals
        // (inside the supported interval [dateInf, dateSup]) (should fail)
        try {
            searcher.getData(date.shiftedBy(duration + 10.));
            Assert.fail();
        } catch (final IllegalStateException e) {
            // Expected
            Assert.assertTrue(true);
        }
    }
}
