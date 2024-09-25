/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 *
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
 *
 * @history 23/10/2018
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1936:23/10/2018: add new methods to intervals classes
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.interval;

import java.util.Iterator;
import java.util.TreeSet;

/**
 * This class represents a list of objects {@link ComparableInterval}.
 * <p>
 * It extends a TreeSet of {@link ComparableInterval} instances. Since the objects of the list implement the
 * {@link ComparableInterval} class, the list is an ordered collection.
 * </p>
 *
 * <p>
 * The generic class must implement <code>java.lang.Comparable</code><br>
 * It is HIGHLY recommended this class be immutable!
 * </p>
 *
 * @param <T>
 *        the nature of ending points
 *
 * @concurrency not thread-safe (TreeSet is not thread-safe)
 *
 * @see ComparableInterval
 * @see TreeSet
 *
 * @author GMV
 */
public class ComparableIntervalsList<T extends Comparable<T>> extends
    TreeSet<ComparableInterval<T>> {

     /** Serializable UID. */
    private static final long serialVersionUID = -8887645816871832831L;

    /**
     * Returns true if the provided interval overlaps one of the intervals of the list.
     *
     * @param interval
     *        the interval
     *
     * @return true if the interval overlaps one of the interval of the list
     */
    public boolean overlaps(final ComparableInterval<T> interval) {
        boolean overlaps = false;

        for (final ComparableInterval<T> entry : this) {
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
    public boolean includes(final ComparableInterval<T> interval) {
        boolean includes = false;

        for (final ComparableInterval<T> entry : this) {
            if (entry.includes(interval)) {
                includes = true;
            }
        }

        return includes;
    }

    /**
     * Returns the intervals of the list that contain the provided entry.
     *
     * <p>
     * The list returned can be empty if the entry is not contained in any interval.
     * </p>
     *
     * @param entry
     *        the entry
     *
     * @return the intervals of the list that contain the entry
     */
    public ComparableIntervalsList<T> getIntervalsContaining(final T entry) {
        final ComparableIntervalsList<T> newList = new ComparableIntervalsList<>();

        for (final ComparableInterval<T> interval : this) {
            if (interval.contains(entry)) {
                newList.add(interval);
            }
        }

        return newList;
    }

    /**
     * Returns the shortest interval that contains all the intervals of the list.
     *
     * <p>
     * While an entry included in at least one of the listed intervals must be contained in this global interval, the
     * opposite is not guaranteed (the inclusive interval can contain entries that do not belong to any listed
     * interval).
     * </p>
     *
     * @return the shortest interval that contains all the intervals of the list
     */
    public ComparableInterval<T> getInclusiveInterval() {
        // Inclusive interval
        ComparableInterval<T> inclusiveInterval = null;

        // Interval list iterator
        final Iterator<ComparableInterval<T>> iterator = this.iterator();

        // Sets the inclusive interval equal to the first interval in the list
        if (iterator.hasNext()) {
            inclusiveInterval = iterator.next();
        }

        // Iterates over the remaining intervals in the list
        while (iterator.hasNext()) {
            // Next interval in the list
            final ComparableInterval<T> interval = iterator.next();

            // Try to merge the current interval to the total interval
            final ComparableInterval<T> mergedInterval = interval.mergeTo(inclusiveInterval);

            // Merging has been successful
            if (mergedInterval == null) {
                // Merging has failed, because intervals are disjoint
                inclusiveInterval = new ComparableInterval<>(inclusiveInterval.getLowerEndpoint(),
                    inclusiveInterval.getLowerData(), interval.getUpperData(),
                    interval.getUpperEndpoint());
            } else {
                inclusiveInterval = mergedInterval;
            }
        }

        return inclusiveInterval;
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
    public ComparableIntervalsList<T> getMergedIntervals() {
        final ComparableIntervalsList<T> mergedIntervals = new ComparableIntervalsList<>();

        // Interval currently merging
        ComparableInterval<T> merging = null;

        // Iterates over the intervals in the list
        for (final ComparableInterval<T> interval : this) {
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
    public ComparableIntervalsList<T> getIntersectionWith(final ComparableInterval<T> interval) {
        final ComparableIntervalsList<T> intersections = new ComparableIntervalsList<>();

        for (final ComparableInterval<T> entry : this) {
            final ComparableInterval<T> intersection = interval.getIntersectionWith(entry);

            if (intersection != null) {
                intersections.add(intersection);
            }
        }

        return intersections.getMergedIntervals();
    }
}
