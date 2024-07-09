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
 *
 * @history created 12/03/12
 *
 * HISTORY
* VERSION:4.4:DM:DM-2143:04/10/2019:[PATRIUS] Ajout des methodes overlaps() et includes() a AbsoluteDateIntervalsList
* VERSION:4.4:FA:FA-2133:04/10/2019:[PATRIUS] Probleme de robustesse dans AbsoluteDateIntervalsList
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1936:23/10/2018: add new methods to intervals classes
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.time;

import java.util.Iterator;
import java.util.TreeSet;

import fr.cnes.sirius.patrius.math.interval.ComparableInterval;
import fr.cnes.sirius.patrius.math.interval.IntervalEndpointType;

/**
 * <p>
 * This class represents a list of objects {@link AbsoluteDateInterval}.<br>
 * It extends a TreeSet of {@link AbsoluteDateInterval} instances ; as the {@link AbsoluteDateInterval} objects
 * implement the {@link ComparableInterval} class, the list is an ordered collection of time intervals.
 * 
 * @concurrency not thread-safe
 * 
 * @concurrency.comment thread safety is not required for this class. And, TreeSet is not
 *                      thread-safe.
 * 
 * @see AbsoluteDateInterval
 * @see TreeSet
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id: AbsoluteDateIntervalsList.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 1.1
 * 
 */
public class AbsoluteDateIntervalsList extends TreeSet<AbsoluteDateInterval> {

    /** Generated serialVersionUID */
    private static final long serialVersionUID = -5112507046887326180L;

    /**
     * Gets the {@link AbsoluteDateIntervalsList} containing the specified date.
     * The list can contain zero, one or more {@link AbsoluteDateInterval}.
     * 
     * @param date
     *        the date included in the time intervals
     * 
     * @return a list of time intervals containing the input date.
     */
    public AbsoluteDateIntervalsList getIntervalsContainingDate(final AbsoluteDate date) {
        final AbsoluteDateIntervalsList newLst = new AbsoluteDateIntervalsList();
        for (final AbsoluteDateInterval current : this) {
            if (current.contains(date)) {
                newLst.add(current);
            }
        }
        return newLst;
    }

    /**
     * Gets the shortest interval containing all the intervals belonging to the list.<br>
     * While a date included in at least one of the listed intervals must be contained in
     * this global interval, the opposite is not guaranteed (the inclusive interval can contain
     * dates that do not belong to any listed interval).
     * 
     * @return an {@link AbsoluteDateInterval} including all the intervals of the list
     */
    public AbsoluteDateInterval getInclusiveInterval() {
        AbsoluteDateInterval totalInterval = null;
        final Iterator<AbsoluteDateInterval> i = this.iterator();
        if (i.hasNext()) {
            // sets the inclusive interval equal to the first interval in the list:
            totalInterval = i.next();
        }
        while (i.hasNext()) {
            // iterates over the intervals in the list:
            final AbsoluteDateInterval current = i.next();
            final AbsoluteDateInterval newInterval = current.mergeTo(totalInterval);
            if (newInterval == null) {
                // merging failed, because intervals are disjoint:
                totalInterval = new AbsoluteDateInterval(totalInterval.getLowerEndpoint(),
                    totalInterval.getLowerData(),
                    current.getUpperData(), current.getUpperEndpoint());
            } else {
                // merging has been successful:
                totalInterval = newInterval;
            }
        }
        return totalInterval;
    }

    /**
     * Merges the intervals of the list that overlap and returns the list of merged intervals.
     *
     * <p>
     * The list returned should not contain any overlapping intervals.
     * </p>
     *
     * @return the list of merged intervals
     */
    public AbsoluteDateIntervalsList getMergedIntervals() {
        final AbsoluteDateIntervalsList mergedIntervals = new AbsoluteDateIntervalsList();

        // Interval currently merging
        AbsoluteDateInterval merging = null;

        // Iterates over the intervals in the list
        for (final AbsoluteDateInterval interval : this) {
            // If no interval is being merged, set the merging
            // interval as the current interval
            if (merging == null) {
                merging = interval;
            } else if (merging.overlaps(interval)) {
                // If an interval is being merged and the current
                // interval overlaps it, merge it to
                merging = merging.mergeTo(interval);
            } else {
                // Otherwise, add the merged interval to the list returned
                // and set the interval being merged to null
                mergedIntervals.add(merging);
                merging = interval;
            }
        }

        // If an interval is still being merged after iterating,
        // add it to the returned list
        if (merging != null) {
            mergedIntervals.add(merging);
        }

        return mergedIntervals;
    }

    /**
     * Returns the intersection between an interval and all the intervals of the list.
     *
     * <p>
     * The list returned can be empty if the provided interval does not intersects any interval of the list.
     * </p>
     *
     * @param interval
     *        the interval
     *
     * @return the intersection between the interval and the list of intervals
     */
    public AbsoluteDateIntervalsList getIntersectionWith(final AbsoluteDateInterval interval) {
        final AbsoluteDateIntervalsList intersections = new AbsoluteDateIntervalsList();

        for (final AbsoluteDateInterval entry : this) {
            final AbsoluteDateInterval intersection = interval.getIntersectionWith(entry);

            if (intersection != null) {
                intersections.add(intersection);
            }
        }

        return intersections.getMergedIntervals();
    }

    /**
     * Gets the list of complementary intervals of the given list of intervals.<br>
     * 
     * @return an {@link AbsoluteDateIntervalsList} including all the complementary intervals
     */
    public AbsoluteDateIntervalsList getComplementaryIntervals() {
        final AbsoluteDateIntervalsList complementaryList = new AbsoluteDateIntervalsList();

        if (!this.isEmpty()) {
            if (this.first().getLowerData() != AbsoluteDate.PAST_INFINITY) {
                // creates the first complementary interval ]-inf ; lower point of the first interval]:
                final AbsoluteDateInterval firstComplementary =
                    new AbsoluteDateInterval(IntervalEndpointType.OPEN, AbsoluteDate.PAST_INFINITY,
                        this.first().getLowerData(), this.first().getLowerEndpoint().getOpposite());
                complementaryList.add(firstComplementary);
            }
            // iterates over the elements of the list:
            final Iterator<AbsoluteDateInterval> i = this.iterator();
            AbsoluteDateInterval merged;
            merged = this.first();
            while (i.hasNext() && this.size() != 1) {
                // initialisations:
                AbsoluteDate startDate = null;
                final AbsoluteDate endDate;
                IntervalEndpointType startType = null;
                final IntervalEndpointType endType;
                AbsoluteDateInterval next = null;
                // merges all the overlapping intervals:
                while (merged != null && i.hasNext()) {
                    startDate = merged.getUpperData();
                    startType = merged.getUpperEndpoint();
                    next = i.next();
                    merged = merged.mergeTo(next);
                }
                if (merged == null) {
                    // all the consecutive intervals have been merged, creates the new complementary
                    // interval:
                    endDate = next.getLowerData();
                    endType = next.getLowerEndpoint();
                    final AbsoluteDateInterval complementary = new AbsoluteDateInterval(
                        startType.getOpposite(), startDate, endDate, endType.getOpposite());
                    complementaryList.add(complementary);
                    merged = next;
                }
            }
            if (this.last().getUpperData() != AbsoluteDate.FUTURE_INFINITY) {
                // creates the last complementary interval [upper point of the last interval ; +inf[:
                final AbsoluteDateInterval lastComplementary = new AbsoluteDateInterval(this.last()
                    .getUpperEndpoint().getOpposite(), this.last().getUpperData(),
                    AbsoluteDate.FUTURE_INFINITY, IntervalEndpointType.OPEN);
                complementaryList.add(lastComplementary);
            }
        } else {
            final AbsoluteDateInterval complementary = new AbsoluteDateInterval(
                IntervalEndpointType.OPEN, AbsoluteDate.PAST_INFINITY,
                AbsoluteDate.FUTURE_INFINITY, IntervalEndpointType.OPEN);
            complementaryList.add(complementary);
        }

        return complementaryList;
    }

    /**
     * Returns true if the provided interval overlaps one of the intervals of the list.
     * 
     * @param interval
     *        the interval
     * 
     * @return true if the interval overlaps one of the interval of the list
     */
    public boolean overlaps(final AbsoluteDateInterval interval) {
        boolean overlaps = false;

        for (final AbsoluteDateInterval entry : this) {
            if (entry.overlaps(interval)) {
                overlaps = true;
            }
        }

        return overlaps;
    }

    /**
     * Returns true if the provided interval is included in one of the intervals of the list.
     * 
     * @param interval
     *        the interval
     * 
     * @return true if the interval is included in one of the intervals of the list
     */
    public boolean includes(final AbsoluteDateInterval interval) {
        boolean includes = false;

        for (final AbsoluteDateInterval entry : this) {
            if (entry.includes(interval)) {
                includes = true;
            }
        }

        return includes;
    }
}
