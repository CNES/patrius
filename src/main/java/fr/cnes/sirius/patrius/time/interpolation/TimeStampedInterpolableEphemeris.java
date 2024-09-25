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
 * VERSION:4.13:DM:DM-103:08/12/2023:[PATRIUS] Optimisation du CIRFProvider
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.time.interpolation;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Locale;
import java.util.function.Function;

import fr.cnes.sirius.patrius.math.exception.NotPositiveException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.time.TimeStamped;
import fr.cnes.sirius.patrius.tools.cache.CacheEntry;
import fr.cnes.sirius.patrius.tools.cache.FIFOThreadSafeCache;

/**
 * Class representing an interpolable ephemeris for any time stamped data. It is thread-safe.
 *
 * <p>
 * This class makes a difference between 3 interval types:
 * <ul>
 * <li>The samples interval corresponds to the first and last date of the provided samples.</li>
 * <li>The optimal interval is related to the order of interpolation. Indeed, interpolation is of best quality when it
 * is performed between the 2 central points of the interpolation (if interpolation is of order 8, then interpolation
 * quality is best if there are 4 points before and 4 points after).</li>
 * <li>The usable interval corresponds, depending on {@link #isAcceptOutOfOptimalRange}, either to the samples interval
 * or to the optimal interval</li>
 * </ul>
 * </p>
 *
 * @param <IN>
 *        The type of the samples to be interpolated
 * @param <OUT>
 *        The type of the interpolation result.<br>
 *        For generality sake, it can be different from IN (for example, we can interpolate a sub-data of IN).
 * @author veuillh
 */
public class TimeStampedInterpolableEphemeris<IN extends TimeStamped, OUT> implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 6676921559649148183L;

    /** Mutualize start error message. */
    private static final String START_MESSAGE = "The provided date (";

    /** Mutualize end error message. */
    private static final String END_MESSAGE = ")";

    /** The samples of time stamped data. It must be sorted. */
    private final IN[] samples;

    /** Cache value for the first usable date. */
    private final AbsoluteDate firstUsableDate;

    /** Cache value for the last usable date. */
    private final AbsoluteDate lastUsableDate;

    /** Half the order of interpolation. */
    private final int halfOrder;

    /**
     * Indicates whether accept dates outside of the optimal interval which is a sub-interval from the full interval
     * interval required for interpolation with respect to the interpolation order.
     */
    private final boolean acceptOutOfOptimalRange;

    /** The interpolation function builder. */
    private final TimeStampedInterpolationFunctionBuilder<IN, OUT> interpolationFunctionBuilder;

    /** The cache storing the last interpolation functions to avoid repeating computations. */
    private final FIFOThreadSafeCache<AbsoluteDateInterval, Function<AbsoluteDate, ? extends OUT>> cache;

    /** Search method for the right interval of samples. */
    private SearchMethod searchMethod;

    /**
     * Simplified constructor.<br>
     * By default, the given samples are copied to be stored, plus they are checked to be strictly sorted (in increasing
     * order and no samples with the same date) and a cache size of {@link FIFOThreadSafeCache#DEFAULT_MAX_SIZE} is
     * used.
     * <p>
     * Note: The interpolation function builder has to implement {@link Serializable} if the interpolable ephemeris
     * should be serializable (not required).
     * </p>
     *
     * @param samples
     *        The array of samples
     * @param order
     *        Interpolation order (number of points to use for the interpolation). It must be even.
     * @param interpFctBuilder
     *        The function that can build an interpolation function
     * @param acceptOutOfOptimalRange
     *        Indicates whether accept dates outside of the optimal interval which is a sub-interval from the full
     *        interval interval
     *        required for interpolation with respect to the interpolation order
     * @throws NullArgumentException
     *         if {@code samples} is null
     * @throws IllegalArgumentException
     *         if the samples aren't sorted
     *         if the order isn't an even number or is lower than 2
     *         if the interpolable ephemeris length is lower than the order
     */
    public TimeStampedInterpolableEphemeris(final IN[] samples,
                                            final int order,
                                            final TimeStampedInterpolationFunctionBuilder<IN, OUT> interpFctBuilder,
                                            final boolean acceptOutOfOptimalRange) {
        this(samples, order, interpFctBuilder, acceptOutOfOptimalRange, true, true,
                FIFOThreadSafeCache.DEFAULT_MAX_SIZE);
    }

    /**
     * Standard constructor.
     * <p>
     * Note: The interpolation function builder has to implement {@link Serializable} if the interpolable ephemeris
     * should be serializable (not required).
     * </p>
     *
     * @param samples
     *        The array of samples
     * @param order
     *        Interpolation order (number of points to use for the interpolation). It must be even.
     * @param interpFctBuilder
     *        The function that can build an interpolation function
     * @param acceptOutOfOptimalRange
     *        Indicates whether accept dates outside of the optimal interval which is a sub-interval from the full
     *        interval interval required for interpolation with respect to the interpolation order
     * @param copySamples
     *        Indicates whether the given samples should be copied or stored directly
     * @param checkStrictlySorted
     *        Indicates whether the given samples should be checked to be strictly sorted (in increasing order and no
     *        samples with the same date) or if it's not necessary
     * @param cacheSize
     *        The size of the cache. 0 is a legitimate value emulating the absence of cache.
     * @throws NullArgumentException
     *         if {@code samples} or {@code interpolationFunctionBuilder} is null
     * @throws IllegalArgumentException
     *         if the samples should be sorted ({@code checkStrictlySorted = true}) and aren't
     *         if the order is an odd number or is lower than 2
     *         if the samples array length is lower than the order
     * @throws NotPositiveException
     *         if {@code cacheSize < 0}
     */
    public TimeStampedInterpolableEphemeris(final IN[] samples,
                                            final int order,
                                            final TimeStampedInterpolationFunctionBuilder<IN, OUT> interpFctBuilder,
                                            final boolean acceptOutOfOptimalRange, final boolean copySamples,
                                            final boolean checkStrictlySorted, final int cacheSize) {

        // Check inputs
        if (samples == null || interpFctBuilder == null) {
            throw new NullArgumentException();
        }
        if (checkStrictlySorted && !isStrictlySorted(samples)) {
            throw new IllegalArgumentException("The samples should be sorted");
        }
        if (order < 2 || order % 2 != 0) {
            throw new IllegalArgumentException("The order must be an even number greater or equal than 2");
        }
        if (samples.length < order) {
            throw new IllegalArgumentException(
                "The interpolable ephemeris length must be greater or equal than the order");
        }
        if (cacheSize < 0) {
            throw new NotPositiveException(cacheSize);
        }

        this.samples = copySamples ? samples.clone() : samples;
        this.halfOrder = order / 2;
        this.acceptOutOfOptimalRange = acceptOutOfOptimalRange;
        this.interpolationFunctionBuilder = interpFctBuilder;
        this.cache = new FIFOThreadSafeCache<>(cacheSize);
        this.searchMethod = SearchMethod.PROPORTIONAL;

        // Define the usable first/last dates depending on the acceptOutOfOptimalRange value
        if (acceptOutOfOptimalRange) {
            this.firstUsableDate = getFirstDate();
            this.lastUsableDate = getLastDate();
        } else {
            this.firstUsableDate = getFirstOptimalDate();
            this.lastUsableDate = getLastOptimalDate();
        }
    }

    /**
     * Private constructor.
     *
     * @param samples
     *        The array of samples
     * @param halfOrder
     *        Half the order of interpolation
     * @param acceptOutOfOptimalRange
     *        Indicates whether accept dates outside of the optimal interval which is a sub-interval from the full
     *        interval interval required for interpolation with respect to the interpolation order
     * @param interpFctBuilder
     *        The interpolation function builder
     * @param cache
     *        The cache storing the last interpolation functions to avoid repeating computations
     * @param searchMethod
     *        Search method for the right interval of samples
     */
    private TimeStampedInterpolableEphemeris(final IN[] samples,
                                             final int halfOrder,
                                             final boolean acceptOutOfOptimalRange,
                                             final TimeStampedInterpolationFunctionBuilder<IN, OUT> interpFctBuilder,
                                             final FIFOThreadSafeCache<AbsoluteDateInterval,
                                             Function<AbsoluteDate, ? extends OUT>> cache,
                                             final SearchMethod searchMethod) {
        this.samples = samples;
        this.halfOrder = halfOrder;
        this.acceptOutOfOptimalRange = acceptOutOfOptimalRange;
        this.interpolationFunctionBuilder = interpFctBuilder;
        this.cache = cache;
        this.searchMethod = searchMethod;

        // Define the usable first/last dates depending on the acceptOutOfOptimalRange value
        if (acceptOutOfOptimalRange) {
            this.firstUsableDate = getFirstDate();
            this.lastUsableDate = getLastDate();
        } else {
            this.firstUsableDate = getFirstOptimalDate();
            this.lastUsableDate = getLastOptimalDate();
        }
    }

    /**
     * Extend the current interpolable ephemeris with new samples while keeping the cache.
     *
     * <p>
     * Note: The current ephemeris should not be used anymore after being extended since it will share the same cache as
     * the returned extended ephemeris.
     * </p>
     *
     * @param extraSamples
     *        The extra sample to extend the ephemeris
     * @param addOnTheRight
     *        Indicate if the samples should be added on the right (after) or on the left (before) the existing samples
     * @param checkStrictlySorted
     *        Indicate if the extraSample should be checked to be strictly increasingly sorted.
     *        Should be deactivated only if it is guaranteed by the user.
     * @return a new interpolable ephemeris containing the concatenated samples and the cache
     * @throws IllegalArgumentException
     *         if the extra samples dates are not coherent with respect to the {@code addOnTheRight} boolean
     *         or if the extra samples are not strictly sorted (while {@code checkStrictlySorted} is activated)
     */
    public TimeStampedInterpolableEphemeris<IN, OUT> extendInterpolableEphemeris(final IN[] extraSamples,
                                                                                 final boolean addOnTheRight,
                                                                                 final boolean checkStrictlySorted) {
        // Check extraSamples are strictly sorted
        if (checkStrictlySorted && !isStrictlySorted(extraSamples)) {
            throw new IllegalArgumentException("The extraSamples should be sorted");
        }

        // Build the extendedSamples
        final IN[] extendedSamples;
        if (addOnTheRight) {
            // Add the extra samples on the right of the existing samples
            if (extraSamples[0].getDate().compareTo(this.getLastDate()) <= 0) {
                throw new IllegalArgumentException(
                    "The first date of the extra samples is not strictly after the last date of the existing samples.");
            }
            // Create the new extendedSample from the existing samples
            extendedSamples = Arrays.copyOf(this.samples, this.samples.length + extraSamples.length);
            // Add the extraSamples
            System.arraycopy(extraSamples, 0, extendedSamples, this.samples.length, extraSamples.length);
        } else {
            // Add the extra sample on the left of the existing samples
            if (extraSamples[extraSamples.length - 1].getDate().compareTo(this.getFirstDate()) >= 0) {
                throw new IllegalArgumentException("The last date of the extra samples is not strictly before "
                        + "the first date of the existing samples.");
            }
            // Create the new extendedSample from the extra samples
            extendedSamples = Arrays.copyOf(extraSamples, this.samples.length + extraSamples.length);
            // Add the existing samples
            System.arraycopy(this.samples, 0, extendedSamples, extraSamples.length, this.samples.length);
        }

        return new TimeStampedInterpolableEphemeris<>(extendedSamples, this.halfOrder, this.acceptOutOfOptimalRange,
            this.interpolationFunctionBuilder, this.cache, this.searchMethod);
    }

    /**
     * Returns an interpolated instance at the required date.
     *
     * @param date
     *        The date of the interpolation
     * @return the interpolated instance
     * @throws IllegalStateException
     *         if the date is outside the supported interval
     *         or if the instance has the setting {@code acceptOutOfRange = false} and the date is outside the optimal
     *         interval which is a sub-interval from the full interval interval required for interpolation with respect
     *         to the interpolation order
     */
    public OUT interpolate(final AbsoluteDate date) {
        final CacheEntry<AbsoluteDateInterval, Function<AbsoluteDate, ? extends OUT>> cacheEntry = this.cache
            .computeIf(entry -> entry.getKey().contains(date), () -> {

                //
                // Search samples surrounding the provided date
                //

                // Initialize variables to the bounds of the samples
                int indexInf;
                int indexSup;
                if (this.acceptOutOfOptimalRange) {
                    indexInf = 0;
                    indexSup = this.samples.length - 1;
                } else {
                    indexInf = this.halfOrder - 1;
                    indexSup = this.samples.length - this.halfOrder;
                }

                AbsoluteDate dateInf = this.firstUsableDate;
                AbsoluteDate dateSup = this.lastUsableDate;

                // Check bounds
                final int dateInfComp = date.compareTo(dateInf);
                final int dateSupComp = date.compareTo(dateSup);

                if (dateInfComp < 0 || dateSupComp > 0) {
                    // If the date is outside the usable interval [dateInf, dateSup], raise an exception
                    throw new IllegalStateException(START_MESSAGE + date + ") is outside the usable interval "
                            + new AbsoluteDateInterval(this.getFirstUsableDate(), this.getLastUsableDate()));
                }

                if (dateInfComp == 0) {
                    // The provided date matches the inferior sample
                    indexSup = indexInf + 1;
                    dateSup = this.samples[indexSup].getDate();
                } else if (dateSupComp == 0) {
                    // The provided date matches the superior sample
                    indexInf = indexSup - 1;
                    dateInf = this.samples[indexInf].getDate();
                } else {
                    // The provided date is within the two extreme samples
                    // Search while indexInf + 1 < indexSup
                    while (indexSup - indexInf > 1) {
                        // Compute the mid sample
                        final int indexMid = this.searchMethod.midPoint(indexInf, indexSup, dateInf, dateSup, date);
                        final AbsoluteDate dateMid = this.samples[indexMid].getDate();
                        final int dateMidComp = dateMid.compareTo(date);

                        if (dateMidComp == 0) {
                            indexInf = indexMid;
                            dateInf = dateMid;
                            indexSup = indexMid + 1;
                            dateSup = this.samples[indexSup].getDate();
                        } else if (dateMidComp < 0) {
                            // The provided date is above the mid sample: update the inferior sample
                            indexInf = indexMid;
                            dateInf = dateMid;
                        } else {
                            // The provided date is below the mid sample: update the superior sample
                            indexSup = indexMid;
                            dateSup = dateMid;
                        }
                    }
                }

                //
                // Build the interpolation function
                //
                int interpolationIndexInf = indexInf - this.halfOrder + 1;
                int interpolationIndexSup = indexSup + this.halfOrder - 1;

                // Otherwise, shift the interpolation index so that we remain in the bounds
                if (interpolationIndexInf < 0) {
                    // We are not in the optimal interval, but it was allowed by the user
                    // shift the interpolation index so that we remain in the bounds
                    interpolationIndexSup -= interpolationIndexInf;
                    interpolationIndexInf = 0;
                } else if (interpolationIndexSup > this.samples.length - 1) {
                    // We are not in the optimal interval, but it was allowed by the user
                    // shift the interpolation index so that we remain in the bounds
                    interpolationIndexInf += this.samples.length - 1 - interpolationIndexSup;
                    interpolationIndexSup = this.samples.length - 1;
                }

                final Function<AbsoluteDate, ? extends OUT> interpolationFunction =
                    this.interpolationFunctionBuilder.buildInterpolationFunction(this.samples, interpolationIndexInf,
                        interpolationIndexSup + 1);

                // Return the interpolation function incorporated in a cache entry for potential next matches
                return new CacheEntry<>(new AbsoluteDateInterval(dateInf, dateSup), interpolationFunction);
            });
        return cacheEntry.getValue().apply(date);
    }

    /**
     * Getter for the floor index for the given date.<br>
     * If the provided date is before the first sample, -1 is returned.
     *
     * @param date
     *        The date to look for
     * @return the floor index
     */
    public int getFloorIndex(final AbsoluteDate date) {
        // Initialize the superior variable to the bounds of the samples
        int indexSup = this.samples.length - 1;

        AbsoluteDate dateInf = this.getFirstDate();
        AbsoluteDate dateSup = this.getLastDate();

        // Check bounds
        final int dateInfComp = date.compareTo(dateInf);
        final int dateSupComp = date.compareTo(dateSup);

        // Quick escapes
        if (dateInfComp < 0) {
            return -1;
        } else if (dateInfComp == 0) {
            return 0;
        } else if (dateSupComp >= 0) {
            return indexSup;
        }

        // Initialize the inferior variable to the bounds of the samples
        int indexInf = 0;

        // The provided date is within the two extreme samples
        // Search while indexInf + 1 < indexSup
        while (indexSup - indexInf > 1) {
            // Compute the mid sample
            final int indexMid = this.searchMethod.midPoint(indexInf, indexSup, dateInf, dateSup, date);
            final AbsoluteDate dateMid = this.samples[indexMid].getDate();
            final int dateMidComp = dateMid.compareTo(date);

            if (dateMidComp == 0) {
                return indexMid;
            } else if (dateMidComp < 0) {
                // The provided date is above the mid sample: update the inferior sample
                indexInf = indexMid;
                dateInf = dateMid;
            } else {
                // The provided date is below the mid sample: update the superior sample
                indexSup = indexMid;
                dateSup = dateMid;
            }
        }
        return indexInf;
    }

    /**
     * Getter for the ceiling index for the given date.<br>
     * If the provided date is after the last sample, -1 is returned.
     *
     * @param date
     *        The date to look for
     * @return the ceiling index
     */
    public int getCeilingIndex(final AbsoluteDate date) {
        // Initialize the superior variable to the bounds of the samples
        int indexSup = this.samples.length - 1;

        AbsoluteDate dateInf = this.getFirstDate();
        AbsoluteDate dateSup = this.getLastDate();

        // Check bounds
        final int dateInfComp = date.compareTo(dateInf);
        final int dateSupComp = date.compareTo(dateSup);

        // Quick escapes
        if (dateSupComp > 0) {
            return -1;
        } else if (dateSupComp == 0) {
            return indexSup;
        } else if (dateInfComp <= 0) {
            return 0;
        }

        // Initialize the inferior variable to the bounds of the samples
        int indexInf = 0;

        // The provided date is within the two extreme samples
        // Search while indexInf + 1 < indexSup
        while (indexSup - indexInf > 1) {
            // Compute the mid sample
            final int indexMid = this.searchMethod.midPoint(indexInf, indexSup, dateInf, dateSup, date);
            final AbsoluteDate dateMid = this.samples[indexMid].getDate();
            final int dateMidComp = dateMid.compareTo(date);

            if (dateMidComp == 0) {
                return indexMid;
            } else if (dateMidComp < 0) {
                // The provided date is above the mid sample: update the inferior sample
                indexInf = indexMid;
                dateInf = dateMid;
            } else {
                // The provided date is below the mid sample: update the superior sample
                indexSup = indexMid;
                dateSup = dateMid;
            }
        }
        return indexSup;
    }

    /**
     * Getter for the floor sample for the given date.
     *
     * @param date
     *        The date to look for
     * @return the ceiling index
     * @throws IllegalStateException
     *         if the provided date is before the first sample
     */
    public IN getFloorSample(final AbsoluteDate date) {
        final int floorIndex = getFloorIndex(date);
        if (floorIndex < 0) {
            throw new IllegalStateException(START_MESSAGE + date + ") is before the first sample ("
                    + this.getFirstSample().getDate() + END_MESSAGE);
        }
        return this.samples[floorIndex];
    }

    /**
     * Getter for the ceiling sample for the given date.
     *
     * @param date
     *        The date to look for
     * @return the ceiling index
     * @throws IllegalStateException
     *         if the provided date is after the last sample
     */
    public IN getCeilingSample(final AbsoluteDate date) {
        final int ceilingIndex = getCeilingIndex(date);
        if (ceilingIndex < 0) {
            throw new IllegalStateException(START_MESSAGE + date + ") is after the last sample ("
                    + this.getLastSample().getDate() + END_MESSAGE);
        }
        return this.samples[ceilingIndex];
    }

    /**
     * Getter for the first sample.
     *
     * @return the first sample
     */
    // Implementation note: This method is declared final, so it can't be {@code Override} and it can be called safely
    // from the constructor
    public final IN getFirstSample() {
        return this.samples[0];
    }

    /**
     * Getter for the last sample.
     *
     * @return the last sample
     */
    // Implementation note: This method is declared final, so it can't be {@code Override} and it can be called safely
    // from the constructor
    public final IN getLastSample() {
        return this.samples[this.samples.length - 1];
    }

    /**
     * Getter for the first date.
     *
     * @return the first date
     */
    // Implementation note: This method is declared final, so it can't be {@code Override} and it can be called safely
    // from the constructor
    public final AbsoluteDate getFirstDate() {
        return getFirstSample().getDate();
    }

    /**
     * Getter for the last date.
     *
     * @return the last date
     */
    // Implementation note: This method is declared final, so it can't be {@code Override} and it can be called safely
    // from the constructor
    public final AbsoluteDate getLastDate() {
        return getLastSample().getDate();
    }

    /**
     * Getter for the first usable date.
     *
     * @return the first usable date
     */
    public AbsoluteDate getFirstUsableDate() {
        return this.firstUsableDate;
    }

    /**
     * Getter for the last usable date.
     *
     * @return the last usable date
     */
    public AbsoluteDate getLastUsableDate() {
        return this.lastUsableDate;
    }

    /**
     * Getter for the first optimal date.
     *
     * @return the first optimal date
     */
    // Implementation note: This method is declared final, so it can't be {@code Override} and it can be called safely
    // from the constructor
    public final AbsoluteDate getFirstOptimalDate() {
        return this.samples[this.halfOrder - 1].getDate();
    }

    /**
     * Getter for the last optimal date.
     *
     * @return the last date
     */
    // Implementation note: This method is declared final, so it can't be {@code Override} and it can be called safely
    // from the constructor
    public final AbsoluteDate getLastOptimalDate() {
        return this.samples[this.samples.length - this.halfOrder].getDate();
    }

    /**
     * Getter for the sample size.
     *
     * @return the sample size
     */
    public int getSampleSize() {
        return this.samples.length;
    }

    /**
     * Getter for the samples array.
     *
     * @param copy
     *        if {@code true} return a copy of the samples array, otherwise return the stored array
     * @return the samples array
     */
    public IN[] getSamples(final boolean copy) {
        final IN[] out;
        if (copy) {
            out = this.samples.clone();
        } else {
            out = this.samples;
        }
        return out;
    }

    /**
     * Provides the ratio of reusability of the internal cache. This method can help to chose the size of the cache.
     *
     * @return the reusability ratio (0 means no reusability at all, 0.5 means that the supplier is called only half
     *         time compared to computeIf method)
     */
    public double getCacheReusabilityRatio() {
        return this.cache.getReusabilityRatio();
    }

    /**
     * Indicates whether accept dates outside of the optimal interval which is a sub-interval from the full interval
     * interval required for interpolation with respect to the interpolation order.
     *
     * @return {@code true} if the dates outside of the optimal interval are accepted, {@code false} otherwise
     */
    public boolean isAcceptOutOfOptimalRange() {
        return this.acceptOutOfOptimalRange;
    }

    /**
     * Getter for the search method.
     *
     * @return the search method
     */
    public SearchMethod getSearchMethod() {
        return this.searchMethod;
    }

    /**
     * Setter for the search method.
     *
     * @param searchMethod
     *        the search method to set
     * @throws NullArgumentException
     *         if {@code searchMethod} is null
     */
    public void setSearchMethod(final SearchMethod searchMethod) {
        if (searchMethod == null) {
            throw new NullArgumentException();
        }
        this.searchMethod = searchMethod;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        final String txt = "TimeStampedInterpolableEphemeris{firstUsableDate=%s, lastUsableDate=%s, "
                + "interpolationOrder=%d, nbSamples=%d}";
        return String.format(Locale.US, txt, this.firstUsableDate.toString(), this.lastUsableDate.toString(),
            this.halfOrder * 2, this.samples.length);
    }

    /**
     * Checks whether the samples are strictly sorted (in increasing order and no samples with the same date).
     *
     * @param samples
     *        The samples of time stamped data to check
     * @return {@code true} if the samples are strictly sorted, {@code false} otherwise
     */
    private static boolean isStrictlySorted(final TimeStamped[] samples) {
        AbsoluteDate previousDate = samples[0].getDate();
        for (int i = 1; i < samples.length; i++) {
            final AbsoluteDate nextDate = samples[i].getDate();
            if (nextDate.compareTo(previousDate) <= 0) {
                return false;
            }
            previousDate = nextDate;
        }
        return true;
    }

    /** Enumerate to specify the search method used to find the correct samples interval given a certain date. */
    public enum SearchMethod {

        /**
         * This method only relies on indexes.<br>
         * It simply cuts the remaining interval in half at each step. The complexity is O(log(n)).
         */
        DICHOTOMY {
            /** {@inheritDoc} */
            @Override
            public int midPoint(final int indexInf, final int indexSup, final AbsoluteDate dateInf,
                                final AbsoluteDate dateSup, final AbsoluteDate date) {
                // (indexInf + indexSup) >>> 1 is equivalent to ((indexInf + indexSup) / 2) if indexInf and indexSup are
                // positive
                return (indexInf + indexSup) >>> 1;
            }
        },

        /**
         * This method tries to find the best guess for the next middle point by computing the position of the searched
         * date as if the samples were evenly distributed.<br>
         * If the samples are evenly distributed, the complexity is O(log(log(n))).<br>
         * If it is not the case, the complexity is can be higher, up to O(n) if the samples are exponentially
         * distributed for example.
         */
        PROPORTIONAL {
            /** {@inheritDoc} */
            @Override
            public int midPoint(final int indexInf, final int indexSup, final AbsoluteDate dateInf,
                                final AbsoluteDate dateSup, final AbsoluteDate date) {
                // Value within ]0, 1[ to measure the position of date in the interval
                final double datePosition = date.durationFrom(dateInf) / dateSup.durationFrom(dateInf);
                // Transform to position to an integer
                int indexShift = (int) (datePosition * (indexSup - indexInf));
                // Make sure the shift is strictly positive to avoid stagnation
                if (indexShift == 0) {
                    indexShift += 1;
                }
                return indexInf + indexShift;
            }
        };

        /**
         * Computes the middle point index in the range [indexInf ; indexSup].
         *
         * @param indexInf
         *        Inferior index
         * @param indexSup
         *        Superior index
         * @param dateInf
         *        Inferior date
         * @param dateSup
         *        Superior date
         * @param date
         *        The date to look for
         * @return the middle point index
         */
        public abstract int midPoint(final int indexInf, final int indexSup, final AbsoluteDate dateInf,
                                     final AbsoluteDate dateSup, final AbsoluteDate date);
    }
}
