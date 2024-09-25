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
 * VERSION:4.13:DM:DM-120:08/12/2023:[PATRIUS] Merge de la branche patrius-for-lotus dans Patrius
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.time;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.tools.cache.CacheEntry;
import fr.cnes.sirius.patrius.tools.cache.FIFOThreadSafeCache;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This class associates objects to {@link AbsoluteDateInterval}. It allows to get efficiently the object corresponding
 * to a given date.
 *
 * @param <T>
 *        The type of the objects
 *
 * @author veuillh
 * 
 * @since 4.13
 */
public class IntervalMapSearcher<T> implements Iterable<CacheEntry<AbsoluteDateInterval, T>>, Serializable {

    /** Default cache size. */
    public static final int DEFAULT_CACHE_SIZE = 3;

    /** Serializable UID. */
    private static final long serialVersionUID = -2582777893214898463L;

    /** Samples associating intervals to its data. */
    private final CacheEntry<AbsoluteDateInterval, T>[] samples;

    /** Cache value for the first interval. */
    private final AbsoluteDateInterval firstInterval;

    /** Cache value for the last interval. */
    private final AbsoluteDateInterval lastInterval;

    /** Cache storing the last intervals data to avoid repeating computations. */
    private final FIFOThreadSafeCache<AbsoluteDateInterval, T> cache;

    /**
     * Simple constructor with a collection of {@link AbsoluteDateInterval intervals} and a collection of objects.
     *
     * <p>
     * Both collections should be the same size and their iterator should coherently go through the data.<br>
     * The intervals must not overlap with each other.
     * </p>
     *
     * @param intervalsCollection
     *        Collection of intervals
     * @param objectsCollection
     *        Collection of objects
     * @throws DimensionMismatchException
     *         if {@code intervalsCollection.size() != objectsCollection.size()}
     * @throws IllegalArgumentException
     *         if some intervals from the specified collection overlap with each other
     *         if one of the objects of the objects collection is {@code null}
     */
    public IntervalMapSearcher(final Collection<AbsoluteDateInterval> intervalsCollection,
                               final Collection<T> objectsCollection) {
        this(intervalsCollection, objectsCollection, DEFAULT_CACHE_SIZE);
    }

    /**
     * Simple constructor with a collection of {@link AbsoluteDateInterval intervals} and a collection of objects.
     *
     * <p>
     * Both collections should be the same size and their iterator should coherently go through the data.<br>
     * The intervals must not overlap with each other.
     * </p>
     *
     * @param intervalsCollection
     *        Collection of intervals
     * @param objectsCollection
     *        Collection of objects
     * @param cacheSize
     *        Size of the cache
     * @throws DimensionMismatchException
     *         if {@code intervalsCollection.size() != objectsCollection.size()}
     * @throws IllegalArgumentException
     *         if some intervals from the specified collection overlap with each other
     *         if one of the objects of the objects collection is {@code null}
     */
    public IntervalMapSearcher(final Collection<AbsoluteDateInterval> intervalsCollection,
                               final Collection<T> objectsCollection, final int cacheSize) {
        this(buildMap(intervalsCollection, objectsCollection), cacheSize);
    }

    /**
     * Main constructor.
     *
     * @param map
     *        Map of intervals and functions. Note that the map is not duplicated internally.
     * @throws IllegalArgumentException
     *         if some intervals from the specified map overlap with each other
     *         if one of the objects of the map is {@code null}
     * @throws NullArgumentException
     *         if {@code map} is {@code null}
     */
    public IntervalMapSearcher(final Map<AbsoluteDateInterval, T> map) {
        this(map, DEFAULT_CACHE_SIZE);
    }

    /**
     * Main constructor.
     *
     * @param map
     *        Map of intervals and functions. Note that the map is not duplicated internally.
     * @param cacheSize
     *        Size of the cache
     * @throws IllegalArgumentException
     *         if some intervals from the specified map overlap with each other
     *         if one of the objects of the map is {@code null}
     * @throws NullArgumentException
     *         if {@code map} is {@code null}
     */
    @SuppressWarnings("unchecked")
    public IntervalMapSearcher(final Map<AbsoluteDateInterval, T> map, final int cacheSize) {
        // Check for null inputs
        if (map == null) {
            throw new NullArgumentException(PatriusMessages.NULL_NOT_ALLOWED_DESCRIPTION, "map");
        }

        // Use a TreeMap to sort the intervals in chronological order
        final TreeMap<AbsoluteDateInterval, T> treeMap;
        if (map instanceof TreeMap) {
            treeMap = (TreeMap<AbsoluteDateInterval, T>) map;
        } else {
            treeMap = new TreeMap<>(map);
        }

        // Extract the first and the last intervals
        this.firstInterval = treeMap.firstKey();
        this.lastInterval = treeMap.lastKey();

        // Build the samples (check the data consistency at the same time)
        this.samples = new CacheEntry[treeMap.size()];
        int i = 0;
        AbsoluteDateInterval previousInterval = this.firstInterval;
        for (final Entry<AbsoluteDateInterval, T> entry : treeMap.entrySet()) {
            final AbsoluteDateInterval interval = entry.getKey();
            // Check the entry
            if (i != 0 && interval.overlaps(previousInterval)) {
                throw PatriusException
                    .createIllegalArgumentException(PatriusMessages.INTERVALS_OVERLAPPING_NOT_ALLOWED);
            }
            if (entry.getValue() == null) {
                throw PatriusException
                    .createIllegalArgumentException(PatriusMessages.NULL_OBJECT_AT_INTERVAL, interval);
            }
            // Store the sample
            this.samples[i] = new CacheEntry<>(interval, entry.getValue());
            previousInterval = interval;
            i++;
        }

        // Initialize the cache mechanism
        this.cache = new FIFOThreadSafeCache<>(cacheSize);
    }

    /**
     * Check if the provided date belongs to any available interval.
     *
     * @param date
     *        Date to check
     * @return {@code true} if the date belongs to an available interval
     */
    public boolean containsData(final AbsoluteDate date) {
        return getData(date, false) != null;
    }

    /**
     * Getter for the first interval.
     *
     * @return the first interval
     */
    public AbsoluteDateInterval getFirstInterval() {
        return this.firstInterval;
    }

    /**
     * Getter for the last interval.
     *
     * @return the last interval
     */
    public AbsoluteDateInterval getLastInterval() {
        return this.lastInterval;
    }

    /**
     * Getter for the available intervals.
     *
     * @return the available intervals
     */
    public AbsoluteDateIntervalsList getIntervals() {
        final AbsoluteDateIntervalsList intervals = new AbsoluteDateIntervalsList();
        for (final CacheEntry<AbsoluteDateInterval, T> entry : this.samples) {
            intervals.add(entry.getKey());
        }
        return intervals;
    }

    /**
     * Getter for the data associated to the {@link AbsoluteDateInterval intervals}.
     *
     * @return the data
     */
    public List<T> getData() {
        final List<T> data = new ArrayList<>(this.samples.length);
        for (final CacheEntry<?, T> entry : this.samples) {
            data.add(entry.getValue());
        }
        return data;
    }

    /**
     * Getter for the association between {@link AbsoluteDateInterval intervals} and data.
     *
     * @return the interval/data association
     */
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    public Map<AbsoluteDateInterval, T> getIntervalDataAssociation() {
        final Map<AbsoluteDateInterval, T> dataAssociation = new TreeMap<>();
        for (final CacheEntry<AbsoluteDateInterval, T> entry : this.samples) {
            dataAssociation.put(entry.getKey(), entry.getValue());
        }
        return dataAssociation;
    }

    /**
     * Getter for the object associated to the provided date.
     *
     * @param date
     *        Date associated to the object
     * @return the corresponding object
     * @throws IllegalStateException
     *         if the provided date does not belong to any of the intervals
     */
    public T getData(final AbsoluteDate date) {
        return getData(date, true);
    }

    /**
     * Getter for the object associated to the provided date.
     *
     * @param date
     *        Date associated to the object
     * @param throwException
     *        Indicate if the method should throw an exception if the provided date does not belong to any of the
     *        intervals
     * @return the corresponding object
     * @throws IllegalStateException
     *         if {@code throwException == true} and the provided date does not belong to any of the intervals
     */
    public T getData(final AbsoluteDate date, final boolean throwException) {
        final CacheEntry<AbsoluteDateInterval, T> matchingCacheEntry =
            this.cache.computeIf(entry -> entry.getKey().contains(date), () -> {
                //
                // Search sample matching the provided date
                //

                // Initialize variables to the bounds of the samples
                int indexInf = 0;
                int indexSup = this.samples.length - 1;

                AbsoluteDateInterval intervalInf = this.firstInterval;
                AbsoluteDateInterval intervalSup = this.lastInterval;

                // Check bounds
                final int intervalInfComp = intervalInf.compare(date);
                final int intervalSupComp = intervalSup.compare(date);

                // Check if the date is outside the supported interval [dateInf, dateSup]
                if (intervalInfComp < 0 || intervalSupComp > 0) {
                    // Raise an exception if asked, otherwise only return null
                    if (throwException) {
                        throw PatriusException
                            .createIllegalStateException(PatriusMessages.DATE_OUTSIDE_INTERVALS, date);
                    }
                    return null;
                }

                if (intervalInfComp == 0) {
                    // The provided date matches the inferior sample
                    return this.samples[indexInf];
                } else if (intervalSupComp == 0) {
                    // The provided date matches the superior sample
                    return this.samples[indexSup];
                }

                // The provided date is within the two extreme samples
                // Search while indexInf + 1 < indexSup
                while (indexSup - indexInf > 1) {
                    // Compute the mid sample
                    final int indexMid = midIndex(indexInf, indexSup);

                    final AbsoluteDateInterval intervalMid = this.samples[indexMid].getKey();
                    final int intervalMidComp = intervalMid.compare(date);

                    if (intervalMidComp == 0) {
                        return this.samples[indexMid];
                    }

                    if (intervalMidComp > 0) {
                        // The provided date is above the mid sample: update the inferior sample
                        indexInf = indexMid;
                        intervalInf = intervalMid;
                    } else {
                        // The provided date is below the mid sample: update the superior sample
                        indexSup = indexMid;
                        intervalSup = intervalMid;
                    }
                }

                // The provided date does not belong to any of the intervals
                // Raise an exception if asked, otherwise only return null
                if (throwException) {
                    throw PatriusException.createIllegalStateException(PatriusMessages.DATE_IN_NO_INTERVALS, date);
                }
                return null;
            });

        final T output;
        if (matchingCacheEntry == null) {
            output = null;
        } else {
            output = matchingCacheEntry.getValue();
        }

        return output;
    }

    /**
     * Return an iterator over entries associating an object and an interval.
     * 
     * @return the iterator
     */
    @Override
    public Iterator<CacheEntry<AbsoluteDateInterval, T>> iterator() {
        return Arrays.stream(this.samples).iterator();
    }

    /**
     * Return the number of elements.
     *
     * @return the number of elements
     */
    public int size() {
        return this.samples.length;
    }

    /**
     * Transform into an array of entries.
     *
     * @param copy
     *        Copy the internal array
     * @return the array of data
     */
    public CacheEntry<AbsoluteDateInterval, T>[] toArray(final boolean copy) {
        return copy ? this.samples.clone() : this.samples;
    }

    /**
     * Transform into an array of entries.
     *
     * @return the array of data
     */
    public CacheEntry<AbsoluteDateInterval, T>[] toArray() {
        return toArray(true);
    }

    /**
     * Compute the middle index.
     *
     * @param indexInf
     *        Inferior index
     * @param indexSup
     *        Superior index
     * @return the middle index
     */
    private static int midIndex(final int indexInf, final int indexSup) {
        return (indexInf + indexSup) >>> 1;
    }

    /**
     * Build a map from a collection of intervals and a collection of objects.
     *
     * @param <T>
     *        The type of the objects
     * @param intervalsCollection
     *        Collection of intervals
     * @param objectsCollection
     *        Collection of objects
     * @return the resulting map
     * @throws DimensionMismatchException
     *         if {@code intervalsCollection.size() != objectsCollection.size()}
     */
    private static
        <T>
            Map<AbsoluteDateInterval, T>
        buildMap(final Collection<AbsoluteDateInterval> intervalsCollection,
                 final Collection<T> objectsCollection) {
        final int intervalsListSize = intervalsCollection.size();

        // Check for size consistency
        if (intervalsListSize != objectsCollection.size()) {
            throw new DimensionMismatchException(intervalsListSize, objectsCollection.size());
        }

        // Build the map
        final TreeMap<AbsoluteDateInterval, T> map = new TreeMap<>();
        final Iterator<AbsoluteDateInterval> intervalsIterator = intervalsCollection.iterator();
        final Iterator<T> objectsIterator = objectsCollection.iterator();

        while (intervalsIterator.hasNext()) {
            map.put(intervalsIterator.next(), objectsIterator.next());
        }
        return map;
    }
}
