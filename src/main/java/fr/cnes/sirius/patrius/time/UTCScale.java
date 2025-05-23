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
* VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
* VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:202:19/03/2014:Corrected last leap second computation methods
 * VERSION::FA:255:13/10/2014:header correction
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.time;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.PatriusConfiguration;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.TimeStampedCacheException;

//CHECKSTYLE: stop MagicNumber check
// Reason: constants - Orekit code

/**
 * Coordinated Universal Time.
 * <p>
 * UTC is related to TAI using step adjustments from time to time according to IERS (International Earth Rotation
 * Service) rules. Before 1972, these adjustments were piecewise linear offsets. Since 1972, these adjustments are
 * piecewise constant offsets, which require introduction of leap seconds.
 * </p>
 * <p>
 * Leap seconds are always inserted as additional seconds at the last minute of the day, pushing the next day forward.
 * Such minutes are therefore more than 60 seconds long. In theory, there may be seconds removal instead of seconds
 * insertion, but up to now (2010) it has never been used. As an example, when a one second leap was introduced at the
 * end of 2005, the UTC time sequence was 2005-12-31T23:59:59 UTC, followed by 2005-12-31T23:59:60 UTC, followed by
 * 2006-01-01T00:00:00 UTC.
 * </p>
 * <p>
 * The OREKIT library retrieves the post-1972 constant time steps data thanks to the
 * {@link fr.cnes.sirius.patrius.data.DataProvidersManager DataProvidersManager} class. The linear models used between
 * 1961 and 1972 are built-in in the class itself.
 * </p>
 * <p>
 * This is intended to be accessed thanks to the {@link TimeScalesFactory} class, so there is no public constructor.
 * Every call to {@link TimeScalesFactory#getUTC()} will create a new {@link UTCScale} instance, sharing the UTC-TAI
 * offset table between all instances.
 * </p>
 * 
 * @author Luc Maisonobe
 * @see AbsoluteDate
 */
@SuppressWarnings("PMD.NullAssignment")
public class UTCScale implements TimeScale {

    /** Serial UID. */
    private static final long serialVersionUID = -8824177435275758263L;

    /** 1000. */
    private static final double ONE_THOUSAND = 1000;

    /** Time steps. */
    private final TimeStampedCache<UTCTAIOffset> cache;

    /**
     * Package private constructor for the factory.
     * Used to create the prototype instance of this class that is used to
     * clone all subsequent instances of {@link UTCScale}. Initializes the offset
     * table that is shared among all instances.
     * 
     * @param entries
     *        user supplied entries
     * @exception PatriusException
     *            if cache cannot be set up
     */
    UTCScale(final SortedMap<DateComponents, Integer> entries) throws PatriusException {

        // create cache
        this.cache = new TimeStampedCache<>(2,
            PatriusConfiguration.getCacheSlotsNumber(), Double.POSITIVE_INFINITY,
            ONE_THOUSAND * Constants.JULIAN_YEAR, new Generator(entries), UTCTAIOffset.class);

        // ensure the cache is populated right from the beginning
        // (this allows calling getEarliest() or getLatest() even before computing first offset)
        this.cache.getNeighbors(AbsoluteDate.J2000_EPOCH);
    }

    /** {@inheritDoc} */
    @Override
    public double offsetFromTAI(final AbsoluteDate date) {
        final double res;
        if (this.cache.getEarliest().getDate().compareTo(date) > 0) {
            // the date is before the first known leap
            res = 0;
        } else if (this.cache.getLatest().getDate().compareTo(date) <= 0) {
            // the date is after the last known leap
            res = -this.cache.getLatest().getOffset(date);
        } else {
            // the date is nominally bracketed by two leaps
            try {
                res = -this.cache.getNeighbors(date)[0].getOffset(date);
            } catch (final TimeStampedCacheException tce) {
                // this should never happen as boundaries have been handled in the previous statements
                throw PatriusException.createInternalError(tce);
            }
        }
        return res;
    }

    /** {@inheritDoc} */
    @Override
    public double offsetToTAI(final DateComponents date,
                              final TimeComponents time) {

        final double res;
        if (this.cache.getEarliest().getMJD() > date.getMJD()) {
            // the date is before the first known leap
            res = 0;
        } else if (this.cache.getLatest().getMJD() <= date.getMJD()) {
            // the date is after the last known leap
            res = this.cache.getLatest().getOffset(date, time);
        } else {
            // the date is nominally bracketed by two leaps
            try {
                // find close neighbors, assuming date in TAI, i.e a date earlier than real UTC date
                final UTCTAIOffset[] neighbors =
                    this.cache.getNeighbors(new AbsoluteDate(date, time, TimeScalesFactory.getTAI()));
                if (neighbors[1].getMJD() <= date.getMJD()) {
                    // the date is in fact just after a leap second!
                    res = neighbors[1].getOffset(date, time);
                } else {
                    res = neighbors[0].getOffset(date, time);
                }
            } catch (final TimeStampedCacheException tce) {
                // this should never happen as boundaries have been handled in the previous statements
                throw PatriusException.createInternalError(tce);
            }
        }
        return res;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return "UTC";
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return this.getName();
    }

    /**
     * Get the date of the first known leap second.
     * 
     * @return date of the first known leap second
     */
    public AbsoluteDate getFirstKnownLeapSecond() {
        return this.cache.getEarliest().getDate();
    }

    /**
     * Get the date of the last known leap second.
     * 
     * @return date of the last known leap second
     */
    public AbsoluteDate getLastKnownLeapSecond() {
        return this.cache.getLatest().getDate();
    }

    /**
     * Check if date is within a leap second introduction.
     * 
     * @param date
     *        date to check
     * @return true if time is within a leap second introduction
     */
    public boolean insideLeap(final AbsoluteDate date) {
        final boolean res;
        if (this.cache.getEarliest().getDate().compareTo(date) > 0) {
            // the date is before the first known leap
            res = false;
        } else if (this.cache.getLatest().getDate().compareTo(date) <= 0) {
            // the date is after the last known leap
            res = date.compareTo(this.cache.getLatest().getValidityStart()) < 0;
        } else {
            // the date is nominally bracketed by two leaps
            try {
                res = date.compareTo(this.cache.getNeighbors(date)[0].getValidityStart()) < 0;
            } catch (final TimeStampedCacheException tce) {
                // this should never happen as boundaries have been handled in the previous statements
                throw PatriusException.createInternalError(tce);
            }
        }
        return res;
    }

    /**
     * Get the value of the previous leap.
     * 
     * @param date
     *        date to check
     * @return value of the previous leap
     */
    public double getLeap(final AbsoluteDate date) {
        final double res;
        if (this.cache.getEarliest().getDate().compareTo(date) > 0) {
            res = 0;
        } else if (this.cache.getLatest().getDate().compareTo(date) <= 0) {
            // the date is after the last known leap
            res = this.cache.getLatest().getLeap();
        } else {
            // the date is nominally bracketed by two leaps
            try {
                res = this.cache.getNeighbors(date)[0].getLeap();
            } catch (final TimeStampedCacheException tce) {
                // this should never happen as boundaries have been handled in the previous statements
                throw PatriusException.createInternalError(tce);
            }
        }
        return res;
    }


    /** Generator for leap seconds entries. */
    private static class Generator implements TimeStampedGenerator<UTCTAIOffset> {

        /** Serializable UID. */
        private static final long serialVersionUID = 4872444368668180483L;
        /** List of {@link UTCTAIOffset} entries. */
        private final List<UTCTAIOffset> offsets;

        /**
         * Simple constructor.
         * 
         * @param entries
         *        user supplied entries
         */
        public Generator(final SortedMap<DateComponents, Integer> entries) {

            this.offsets = new ArrayList<>();

            // set up the linear offsets used between 1961-01-01 and 1971-12-31
            // excerpt from UTC-TAI.history file:
            // 1961 Jan. 1 - 1961 Aug. 1 1.422 818 0s + (MJD - 37 300) x 0.001 296s
            // Aug. 1 - 1962 Jan. 1 1.372 818 0s + ""
            // 1962 Jan. 1 - 1963 Nov. 1 1.845 858 0s + (MJD - 37 665) x 0.001 123 2s
            // 1963 Nov. 1 - 1964 Jan. 1 1.945 858 0s + ""
            // 1964 Jan. 1 - April 1 3.240 130 0s + (MJD - 38 761) x 0.001 296s
            // April 1 - Sept. 1 3.340 130 0s + ""
            // Sept. 1 - 1965 Jan. 1 3.440 130 0s + ""
            // 1965 Jan. 1 - March 1 3.540 130 0s + ""
            // March 1 - Jul. 1 3.640 130 0s + ""
            // Jul. 1 - Sept. 1 3.740 130 0s + ""
            // Sept. 1 - 1966 Jan. 1 3.840 130 0s + ""
            // 1966 Jan. 1 - 1968 Feb. 1 4.313 170 0s + (MJD - 39 126) x 0.002 592s
            // 1968 Feb. 1 - 1972 Jan. 1 4.213 170 0s + ""
            this.addOffsetModel(new DateComponents(1961, 1, 1), 37300, 1.4228180, 0.0012960);
            this.addOffsetModel(new DateComponents(1961, 8, 1), 37300, 1.3728180, 0.0012960);
            this.addOffsetModel(new DateComponents(1962, 1, 1), 37665, 1.8458580, 0.0011232);
            this.addOffsetModel(new DateComponents(1963, 11, 1), 37665, 1.9458580, 0.0011232);
            this.addOffsetModel(new DateComponents(1964, 1, 1), 38761, 3.2401300, 0.0012960);
            this.addOffsetModel(new DateComponents(1964, 4, 1), 38761, 3.3401300, 0.0012960);
            this.addOffsetModel(new DateComponents(1964, 9, 1), 38761, 3.4401300, 0.0012960);
            this.addOffsetModel(new DateComponents(1965, 1, 1), 38761, 3.5401300, 0.0012960);
            this.addOffsetModel(new DateComponents(1965, 3, 1), 38761, 3.6401300, 0.0012960);
            this.addOffsetModel(new DateComponents(1965, 7, 1), 38761, 3.7401300, 0.0012960);
            this.addOffsetModel(new DateComponents(1965, 9, 1), 38761, 3.8401300, 0.0012960);
            this.addOffsetModel(new DateComponents(1966, 1, 1), 39126, 4.3131700, 0.0025920);
            this.addOffsetModel(new DateComponents(1968, 2, 1), 39126, 4.2131700, 0.0025920);

            // add leap second entries in chronological order
            for (final Map.Entry<DateComponents, Integer> entry : entries.entrySet()) {
                this.addOffsetModel(entry.getKey(), 0, entry.getValue(), 0);
            }

        }

        /** {@inheritDoc} */
        @Override
        public List<UTCTAIOffset> generate(final UTCTAIOffset existing,
                                           final AbsoluteDate date) throws TimeStampedCacheException {
            if (existing != null) {
                // short cut to avoid going through all cache updating as nothing new is available
                throw new TimeStampedCacheException(PatriusMessages.UNABLE_TO_GENERATE_NEW_DATA_AFTER,
                    this.offsets.get(this.offsets.size() - 1).getDate());
            }
            return this.offsets;
        }

        /**
         * Add an offset model.
         * <p>
         * This method <em>must</em> be called in chronological order.
         * </p>
         * 
         * @param date
         *        date of the constant offset model start
         * @param mjdRef
         *        reference date of the linear model as a modified julian day
         * @param offset
         *        offset at reference date in seconds (TAI minus UTC)
         * @param slope
         *        offset slope in seconds per UTC day (TAI minus UTC / dUTC)
         */
        private void addOffsetModel(final DateComponents date, final int mjdRef,
                                    final double offset, final double slope) {

            final TimeScale tai = TimeScalesFactory.getTAI();

            // start of the leap
            final UTCTAIOffset previous = this.offsets.isEmpty() ? null : this.offsets.get(this.offsets.size() - 1);
            final double previousOffset = (previous == null) ? 0.0 : previous.getOffset(date, TimeComponents.H00);
            final AbsoluteDate leapStart = new AbsoluteDate(date, tai).shiftedBy(previousOffset);

            // end of the leap
            final double startOffset = offset + slope * (date.getMJD() - mjdRef);
            final AbsoluteDate leapEnd = new AbsoluteDate(date, tai).shiftedBy(startOffset);

            // leap computed at leap start and in UTC scale
            final double normalizedSlope = slope / Constants.JULIAN_DAY;
            final double leap = leapEnd.durationFrom(leapStart) / (1 + normalizedSlope);

            if (previous != null) {
                previous.setValidityEnd(leapStart);
            }
            this.offsets.add(new UTCTAIOffset(leapStart, date.getMJD(), leap, offset, mjdRef, normalizedSlope));
        }
    }

    // CHECKSTYLE: resume MagicNumber check
}
