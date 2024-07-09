/**
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
/* Copyright 2002-2011 CS Communication & Systèmes
 *
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:410:16/04/2015: Anomalies in the Patrius Javadoc
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.time;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.TimeStampedCacheException;

/**
 * Generic thread-safe cache for {@link TimeStamped time-stamped} data.
 * 
 * @param <T>
 *        Type of the cached data.
 * 
 * @author Luc Maisonobe
 */
@SuppressWarnings("PMD.ShortClassName")
public class TimeStampedCache<T extends TimeStamped> implements Serializable {

    /** Default number of independent cached time slots. */
    public static final int DEFAULT_CACHED_SLOTS_NUMBER = 10;

    /** Serial UID. */
    private static final long serialVersionUID = -53837067695666545L;

    /** Quantum step. */
    private static final double QUANTUM_STEP = 1.0e-6;

    /** Reference date for indexing. */
    private final AtomicReference<AbsoluteDate> reference;

    /** Maximum number of independent cached time slots. */
    private final int maxSlots;

    /** Maximum duration span in seconds of one slot. */
    private final double maxSpan;

    /** Quantum gap above which a new slot is created instead of extending an existing one. */
    private final long newSlotQuantumGap;

    /** Class of the cached entries. */
    private final Class<T> entriesClass;

    /** Generator to use for yet non-cached data. */
    private final TimeStampedGenerator<T> generator;

    /** Number of entries in a neighbors array. */
    private final int neighborsSize;

    /** Independent time slots cached. */
    private final List<Slot> slots;

    /** Number of calls to the generate method. */
    private final AtomicInteger calls;

    /** Number of evictions. */
    private final AtomicInteger evictions;

    /** Global lock. */
    private final ReadWriteLock lock;

    /**
     * Simple constructor.
     * 
     * @param neighborsSizeIn
     *        fixed size of the arrays to be returned by {@link #getNeighbors(AbsoluteDate)}, must be at least 2
     * @param maxSlotsIn
     *        maximum number of independent cached time slots
     * @param maxSpanIn
     *        maximum duration span in seconds of one slot
     *        (can be set to {@code Double.POSITIVE_INFINITY} if desired)
     * @param newSlotInterval
     *        time interval above which a new slot is created
     *        instead of extending an existing one
     * @param generatorIn
     *        generator to use for yet non-existent data
     * @param entriesClassIn
     *        class of the cached entries
     */
    public TimeStampedCache(final int neighborsSizeIn, final int maxSlotsIn, final double maxSpanIn,
        final double newSlotInterval, final TimeStampedGenerator<T> generatorIn,
        final Class<T> entriesClassIn) {

        // safety check
        if (maxSlotsIn < 1) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.NUMBER_TOO_SMALL, maxSlotsIn, 1);
        }
        if (neighborsSizeIn < 2) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.NOT_ENOUGH_CACHED_NEIGHBORS,
                neighborsSizeIn, 2);
        }

        this.reference = new AtomicReference<AbsoluteDate>();
        this.maxSlots = maxSlotsIn;
        this.maxSpan = maxSpanIn;
        this.newSlotQuantumGap = MathLib.round(newSlotInterval / QUANTUM_STEP);
        this.entriesClass = entriesClassIn;
        this.generator = generatorIn;
        this.neighborsSize = neighborsSizeIn;
        this.slots = new ArrayList<Slot>(maxSlotsIn);
        this.calls = new AtomicInteger(0);
        this.evictions = new AtomicInteger(0);
        this.lock = new ReentrantReadWriteLock();

    }

    /**
     * Get the generator.
     * 
     * @return generator
     */
    public TimeStampedGenerator<T> getGenerator() {
        return this.generator;
    }

    /**
     * Get the maximum number of independent cached time slots.
     * 
     * @return maximum number of independent cached time slots
     */
    public int getMaxSlots() {
        return this.maxSlots;
    }

    /**
     * Get the maximum duration span in seconds of one slot.
     * 
     * @return maximum duration span in seconds of one slot
     */
    public double getMaxSpan() {
        return this.maxSpan;
    }

    /**
     * Get quantum gap above which a new slot is created instead of extending an existing one.
     * <p>
     * The quantum gap is the {@code newSlotInterval} value provided at construction rounded to the nearest quantum step
     * used internally by the cache.
     * </p>
     * 
     * @return quantum gap in seconds
     */
    public double getNewSlotQuantumGap() {
        return this.newSlotQuantumGap * QUANTUM_STEP;
    }

    /**
     * Get the number of calls to the generate method.
     * <p>
     * This number of calls is related to the number of cache misses and may be used to tune the cache configuration.
     * Each cache miss implies at least one call is performed, but may require several calls if the new date is far
     * offset from the existing cache, depending on the number of elements and step between elements in the arrays
     * returned by the generator.
     * </p>
     * 
     * @return number of calls to the generate method
     */
    public int getGenerateCalls() {
        return this.calls.get();
    }

    /**
     * Get the number of slots evictions.
     * <p>
     * This number should remain small when the max number of slots is sufficient with respect to the number of
     * concurrent requests to the cache. If it increases too much, then the cache configuration is probably bad and
     * cache does not really improve things (in this case, the {@link #getGenerateCalls()
     * number of calls to the generate method} will probably increase too.
     * </p>
     * 
     * @return number of slots evictions
     */
    public int getSlotsEvictions() {
        return this.evictions.get();
    }

    /**
     * Get the number of slots in use.
     * 
     * @return number of slots in use
     */
    public int getSlots() {

        this.lock.readLock().lock();
        try {
            return this.slots.size();
        } finally {
            this.lock.readLock().unlock();
        }

    }

    /**
     * Get the total number of entries cached.
     * 
     * @return total number of entries cached
     */
    public int getEntries() {

        this.lock.readLock().lock();
        try {
            int entries = 0;
            for (final Slot slot : this.slots) {
                entries += slot.getEntries();
            }
            return entries;
        } finally {
            this.lock.readLock().unlock();
        }

    }

    /**
     * Get the earliest cached entry.
     * 
     * @return earliest cached entry
     * @exception IllegalStateException
     *            if the cache has no slots at all
     * @see #getSlots()
     */
    public T getEarliest() {

        this.lock.readLock().lock();
        try {
            if (this.slots.isEmpty()) {
                throw PatriusException.createIllegalStateException(PatriusMessages.NO_CACHED_ENTRIES);
            }
            return this.slots.get(0).getEarliest();
        } finally {
            this.lock.readLock().unlock();
        }

    }

    /**
     * Get the latest cached entry.
     * 
     * @return latest cached entry
     * @exception IllegalStateException
     *            if the cache has no slots at all
     * @see #getSlots()
     */
    public T getLatest() {

        this.lock.readLock().lock();
        try {
            if (this.slots.isEmpty()) {
                throw PatriusException.createIllegalStateException(PatriusMessages.NO_CACHED_ENTRIES);
            }
            return this.slots.get(this.slots.size() - 1).getLatest();
        } finally {
            this.lock.readLock().unlock();
        }

    }

    /**
     * Get the fixed size of the arrays to be returned by {@link #getNeighbors(AbsoluteDate)}.
     * 
     * @return size of the array
     */
    public int getNeighborsSize() {
        return this.neighborsSize;
    }

    /**
     * Get the entries surrounding a central date.
     * <p>
     * If the central date is well within covered range, the returned array will be balanced with half the points before
     * central date and half the points after it (depending on n parity, of course). If the central date is near the
     * generator range boundary, then the returned array will be unbalanced and will contain only the n earliest (or
     * latest) generated (and cached) entries. A typical example of the later case is leap seconds cache, since the
     * number of leap seconds cannot be arbitrarily increased.
     * </p>
     * 
     * @param central
     *        central date
     * @return array of cached entries surrounding specified date (the size
     *         of the array is fixed to the one specified in the
     *         {@link #TimeStampedCache(int, int, double, double, TimeStampedGenerator, Class) constructor})
     * @exception TimeStampedCacheException
     *            if entries are not chronologically
     *            sorted or if new data cannot be generated
     * @see #getEarliest()
     * @see #getLatest()
     */
    public T[] getNeighbors(final AbsoluteDate central) throws TimeStampedCacheException {

        this.lock.readLock().lock();
        try {
            final long dateQuantum = this.quantum(central);
            return this.selectSlot(central, dateQuantum).getNeighbors(central, dateQuantum);
        } finally {
            this.lock.readLock().unlock();
        }

    }

    /**
     * Convert a date to a rough global quantum.
     * <p>
     * We own a global read lock while calling this method.
     * </p>
     * 
     * @param date
     *        date to convert
     * @return quantum corresponding to the date
     */
    private long quantum(final AbsoluteDate date) {
        this.reference.compareAndSet(null, date);
        return MathLib.round(date.durationFrom(this.reference.get()) / QUANTUM_STEP);
    }

    /**
     * Select a slot containing a date.
     * <p>
     * We own a global read lock while calling this method.
     * </p>
     * 
     * @param date
     *        target date
     * @param dateQuantum
     *        global quantum of the date
     * @return slot covering the date
     * @exception TimeStampedCacheException
     *            if entries are not chronologically
     *            sorted or if new data cannot be generated
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Orekit code kept as such
    private Slot selectSlot(final AbsoluteDate date, final long dateQuantum) throws TimeStampedCacheException {
        // CHECKSTYLE: resume CyclomaticComplexity check

        Slot selected = null;

        int index = this.slots.isEmpty() ? 0 : this.slotIndex(dateQuantum);
        if (this.slots.isEmpty() ||
            this.slots.get(index).getEarliestQuantum() > dateQuantum + this.newSlotQuantumGap ||
            this.slots.get(index).getLatestQuantum() < dateQuantum - this.newSlotQuantumGap) {
            // no existing slot is suitable

            // upgrade the read lock to a write lock so we can change the list of available slots
            this.lock.readLock().unlock();
            this.lock.writeLock().lock();

            try {
                // check slots again as another thread may have changed
                // the list while we were waiting for the write lock
                index = this.slots.isEmpty() ? 0 : this.slotIndex(dateQuantum);
                if (this.slots.isEmpty() ||
                    this.slots.get(index).getEarliestQuantum() > dateQuantum + this.newSlotQuantumGap ||
                    this.slots.get(index).getLatestQuantum() < dateQuantum - this.newSlotQuantumGap) {

                    // we really need to create a new slot in the current thread
                    // (no other threads have created it while we were waiting for the lock)
                    if ((!this.slots.isEmpty()) &&
                        this.slots.get(index).getLatestQuantum() < dateQuantum - this.newSlotQuantumGap) {
                        ++index;
                    }

                    if (this.slots.size() >= this.maxSlots) {
                        // we must prevent exceeding allowed max

                        // select the oldest accessed slot for eviction
                        int evict = 0;
                        for (int i = 0; i < this.slots.size(); ++i) {
                            if (this.slots.get(i).getLastAccess() < this.slots.get(evict).getLastAccess()) {
                                evict = i;
                            }
                        }

                        // evict the selected slot
                        this.evictions.incrementAndGet();
                        this.slots.remove(evict);

                        if (evict < index) {
                            // adjust index of created slot as it was shifted by the eviction
                            index--;
                        }
                    }

                    this.slots.add(index, new Slot(date));

                }

            } finally {
                // downgrade back to a read lock
                this.lock.readLock().lock();
                this.lock.writeLock().unlock();
            }
        }

        selected = this.slots.get(index);

        return selected;

    }

    /**
     * Get the index of the slot in which a date could be cached.
     * <p>
     * We own a global read lock while calling this method.
     * </p>
     * 
     * @param dateQuantum
     *        quantum of the date to search for
     * @return the slot in which the date could be cached
     */
    private int slotIndex(final long dateQuantum) {

        // Initialization
        int iInf = 0;
        final long qInf = this.slots.get(iInf).getEarliestQuantum();
        int iSup = this.slots.size() - 1;
        final long qSup = this.slots.get(iSup).getLatestQuantum();
        while (iSup - iInf > 0) {
            // Loop
            final int iInterp = (int) ((iInf * (qSup - dateQuantum) + iSup * (dateQuantum - qInf)) / (qSup - qInf));
            final int iMed = MathLib.max(iInf, MathLib.min(iInterp, iSup));
            final Slot slot = this.slots.get(iMed);
            if (dateQuantum < slot.getEarliestQuantum()) {
                iSup = iMed - 1;
            } else if (dateQuantum > slot.getLatestQuantum()) {
                iInf = MathLib.min(iSup, iMed + 1);
            } else {
                // Direct return
                return iMed;
            }
        }

        // Return result
        return iInf;

    }

    /** Time slot. */
    private final class Slot implements Serializable {

        /** Serial UID. */
        private static final long serialVersionUID = -5108552978209563473L;

        /** Cached time-stamped entries. */
        private final List<Entry> cache;

        /** Earliest quantum. */
        private final AtomicLong earliestQuantum;

        /** Latest quantum. */
        private final AtomicLong latestQuantum;

        /** Index from a previous recent call. */
        private final AtomicInteger guessedIndex;

        /** Last access time. */
        private final AtomicLong lastAccess;

        /**
         * Simple constructor.
         * 
         * @param date
         *        central date for initial entries to insert in the slot
         * @exception TimeStampedCacheException
         *            if entries are not chronologically
         *            sorted or if new data cannot be generated
         */
        public Slot(final AbsoluteDate date) throws TimeStampedCacheException {

            // allocate cache
            this.cache = new ArrayList<Entry>();

            // set up first entries
            AbsoluteDate generationDate = date;

            TimeStampedCache.this.calls.incrementAndGet();
            for (final T entry : this.generateAndCheck(null, generationDate)) {
                this.cache.add(new Entry(entry, TimeStampedCache.this.quantum(entry.getDate())));
            }
            this.earliestQuantum = new AtomicLong(this.cache.get(0).getQuantum());
            this.latestQuantum = new AtomicLong(this.cache.get(this.cache.size() - 1).getQuantum());

            while (this.cache.size() < TimeStampedCache.this.neighborsSize) {
                // we need to generate more entries

                final T entry0 = this.cache.get(0).getData();
                final T entryN = this.cache.get(this.cache.size() - 1).getData();
                TimeStampedCache.this.calls.incrementAndGet();

                final T existing;
                if (entryN.getDate().durationFrom(date) <= date.durationFrom(entry0.getDate())) {
                    // generate additional point at the end of the slot
                    existing = entryN;
                    generationDate =
                        entryN.getDate().shiftedBy(
                            this.getMeanStep() * (TimeStampedCache.this.neighborsSize - this.cache.size()));
                    this.appendAtEnd(this.generateAndCheck(existing, generationDate));
                } else {
                    // generate additional point at the start of the slot
                    existing = entry0;
                    generationDate =
                        entry0.getDate().shiftedBy(
                            -this.getMeanStep() * (TimeStampedCache.this.neighborsSize - this.cache.size()));
                    this.insertAtStart(this.generateAndCheck(existing, generationDate));
                }

            }

            this.guessedIndex = new AtomicInteger(this.cache.size() / 2);
            this.lastAccess = new AtomicLong(System.currentTimeMillis());

        }

        /**
         * Get the earliest entry contained in the slot.
         * 
         * @return earliest entry contained in the slot
         */
        public T getEarliest() {
            return this.cache.get(0).getData();
        }

        /**
         * Get the quantum of the earliest date contained in the slot.
         * 
         * @return quantum of the earliest date contained in the slot
         */
        public long getEarliestQuantum() {
            return this.earliestQuantum.get();
        }

        /**
         * Get the latest entry contained in the slot.
         * 
         * @return latest entry contained in the slot
         */
        public T getLatest() {
            return this.cache.get(this.cache.size() - 1).getData();
        }

        /**
         * Get the quantum of the latest date contained in the slot.
         * 
         * @return quantum of the latest date contained in the slot
         */
        public long getLatestQuantum() {
            return this.latestQuantum.get();
        }

        /**
         * Get the number of entries contained din the slot.
         * 
         * @return number of entries contained din the slot
         */
        public int getEntries() {
            return this.cache.size();
        }

        /**
         * Get the mean step between entries.
         * 
         * @return mean step between entries (or an arbitrary non-null value
         *         if there are fewer than 2 entries)
         */
        private double getMeanStep() {
            if (this.cache.size() < 2) {
                return 1.0;
            } else {
                final AbsoluteDate t0 = this.cache.get(0).getData().getDate();
                final AbsoluteDate tn = this.cache.get(this.cache.size() - 1).getData().getDate();
                return tn.durationFrom(t0) / (this.cache.size() - 1);
            }
        }

        /**
         * Get last access time of slot.
         * 
         * @return last known access time
         */
        public long getLastAccess() {
            return this.lastAccess.get();
        }

        /**
         * Get the entries surrounding a central date.
         * <p>
         * If the central date is well within covered slot, the returned array will be balanced with half the points
         * before central date and half the points after it (depending on n parity, of course). If the central date is
         * near slot boundary and the underlying {@link TimeStampedGenerator
         * generator} cannot extend it (i.e. it returns null), then the returned array will be unbalanced and will
         * contain only the n earliest (or latest) cached entries. A typical example of the later case is leap seconds
         * cache, since the number of leap seconds cannot be arbitrarily increased.
         * </p>
         * 
         * @param central
         *        central date
         * @param dateQuantum
         *        global quantum of the date
         * @return a new array containing date neighbors
         * @exception TimeStampedCacheException
         *            if entries are not chronologically
         *            sorted or if new data cannot be generated
         * @see #getBefore(AbsoluteDate)
         * @see #getAfter(AbsoluteDate)
         */
        // CHECKSTYLE: stop CyclomaticComplexity check
        // Reason: Orekit code kept as such
        public T[] getNeighbors(final AbsoluteDate central, final long dateQuantum) throws TimeStampedCacheException {
            // CHECKSTYLE: resume CyclomaticComplexity check

            int index = this.entryIndex(central, dateQuantum);
            int firstNeighbor = index - (TimeStampedCache.this.neighborsSize - 1) / 2;

            if (firstNeighbor < 0 || firstNeighbor + TimeStampedCache.this.neighborsSize > this.cache.size()) {
                // the cache is not balanced around the desired date, we can try to generate new data

                // upgrade the read lock to a write lock so we can change the list of available slots
                TimeStampedCache.this.lock.readLock().unlock();
                TimeStampedCache.this.lock.writeLock().lock();

                try {
                    // check entries again as another thread may have changed
                    // the list while we were waiting for the write lock
                    boolean loop = true;
                    while (loop) {
                        index = this.entryIndex(central, dateQuantum);
                        firstNeighbor = index - (TimeStampedCache.this.neighborsSize - 1) / 2;
                        if (firstNeighbor < 0
                            || firstNeighbor + TimeStampedCache.this.neighborsSize > this.cache.size()) {

                            // estimate which data we need to be generated
                            final double step = this.getMeanStep();
                            final T existing;
                            final AbsoluteDate generationDate;
                            final boolean simplyRebalance;
                            if (firstNeighbor < 0) {
                                existing = this.cache.get(0).getData();
                                generationDate = existing.getDate().shiftedBy(step * firstNeighbor);
                                simplyRebalance = existing.getDate().compareTo(central) <= 0;
                            } else {
                                existing = this.cache.get(this.cache.size() - 1).getData();
                                generationDate = existing.getDate().shiftedBy(
                                    step * (firstNeighbor + TimeStampedCache.this.neighborsSize - this.cache.size()));
                                simplyRebalance = existing.getDate().compareTo(central) >= 0;
                            }
                            TimeStampedCache.this.calls.incrementAndGet();

                            // generated data and add it to the slot
                            try {
                                if (firstNeighbor < 0) {
                                    this.insertAtStart(this.generateAndCheck(existing, generationDate));
                                } else {
                                    this.appendAtEnd(this.generateAndCheck(existing, generationDate));
                                }
                            } catch (final TimeStampedCacheException tce) {
                                if (simplyRebalance) {
                                    // we were simply trying to rebalance an unbalanced interval near slot end
                                    // we failed, but the central date is already covered by the existing (unbalanced)
                                    // data
                                    // so we ignore the exception and stop the loop, we will continue with what we have
                                    loop = false;
                                } else {
                                    throw tce;
                                }
                            }

                        } else {
                            loop = false;
                        }
                    }
                } finally {
                    // downgrade back to a read lock
                    TimeStampedCache.this.lock.readLock().lock();
                    TimeStampedCache.this.lock.writeLock().unlock();
                }

            }

            @SuppressWarnings("unchecked")
            final T[] array =
                (T[]) Array.newInstance(TimeStampedCache.this.entriesClass, TimeStampedCache.this.neighborsSize);
            if (firstNeighbor + TimeStampedCache.this.neighborsSize > this.cache.size()) {
                // we end up with a non-balanced neighborhood,
                // adjust the start point to fit within the cache
                firstNeighbor = this.cache.size() - TimeStampedCache.this.neighborsSize;
            }
            if (firstNeighbor < 0) {
                firstNeighbor = 0;
            }
            for (int i = 0; i < TimeStampedCache.this.neighborsSize; ++i) {
                array[i] = this.cache.get(firstNeighbor + i).getData();
            }

            return array;

        }

        /**
         * Get the index of the entry corresponding to a date.
         * <p>
         * We own a local read lock while calling this method.
         * </p>
         * 
         * @param date
         *        date
         * @param dateQuantum
         *        global quantum of the date
         * @return index in the array such that entry[index] is before
         *         date and entry[index + 1] is after date (or they are at array boundaries)
         */
        // CHECKSTYLE: stop CyclomaticComplexity check
        // CHECKSTYLE: stop ReturnCount check
        // Reason: Orekit code kept as such
        private int entryIndex(final AbsoluteDate date, final long dateQuantum) {
            // CHECKSTYLE: resume CyclomaticComplexity check
            // CHECKSTYLE: resume ReturnCount check

            // first quick guesses, assuming a recent search was close enough
            final int guess = this.guessedIndex.get();
            if (guess > 0 && guess < this.cache.size()) {
                if (this.cache.get(guess).getQuantum() <= dateQuantum) {
                    if (guess + 1 < this.cache.size() && this.cache.get(guess + 1).getQuantum() > dateQuantum) {
                        // good guess!
                        return guess;
                    } else {
                        // perhaps we have simply shifted just one point forward ?
                        if (guess + 2 < this.cache.size() && this.cache.get(guess + 2).getQuantum() > dateQuantum) {
                            this.guessedIndex.set(guess + 1);
                            return guess + 1;
                        }
                    }
                } else {
                    // perhaps we have simply shifted just one point backward ?
                    if (guess > 1 && this.cache.get(guess - 1).getQuantum() <= dateQuantum) {
                        this.guessedIndex.set(guess - 1);
                        return guess - 1;
                    }
                }
            }

            // quick guesses have failed, we need to perform a full blown search
            if (dateQuantum < this.getEarliestQuantum()) {
                // date if before the first entry
                return -1;
            } else if (dateQuantum > this.getLatestQuantum()) {
                // date is after the last entry
                return this.cache.size();
            } else {

                // try to get an existing entry
                int iInf = 0;
                final long qInf = this.cache.get(iInf).getQuantum();
                int iSup = this.cache.size() - 1;
                final long qSup = this.cache.get(iSup).getQuantum();
                while (iSup - iInf > 0) {
                    // within a continuous slot, entries are expected to be roughly linear
                    final int iInterp = (int) ((iInf * (qSup - dateQuantum)
                        + iSup * (dateQuantum - qInf)) / (qSup - qInf));
                    final int iMed = MathLib.max(iInf + 1, MathLib.min(iInterp, iSup));
                    final Entry entry = this.cache.get(iMed);
                    if (dateQuantum < entry.getQuantum()) {
                        iSup = iMed - 1;
                    } else if (dateQuantum > entry.getQuantum()) {
                        iInf = iMed;
                    } else {
                        this.guessedIndex.set(iMed);
                        return iMed;
                    }
                }

                this.guessedIndex.set(iInf);
                return iInf;

            }

        }

        /**
         * Insert data at slot start.
         * 
         * @param data
         *        data to insert
         * @exception TimeStampedCacheException
         *            if new data cannot be generated
         */
        private void insertAtStart(final List<T> data) throws TimeStampedCacheException {

            // insert data at start
            boolean inserted = false;
            final long q0 = this.earliestQuantum.get();
            for (int i = 0; i < data.size(); ++i) {
                final long quantum = TimeStampedCache.this.quantum(data.get(i).getDate());
                if (quantum < q0) {
                    // data quantum is before earliest quantum, it can be inserted at the start
                    this.cache.add(i, new Entry(data.get(i), quantum));
                    inserted = true;
                } else {
                    break;
                }
            }

            if (!inserted) {
                // Insertion exception
                throw new TimeStampedCacheException(PatriusMessages.UNABLE_TO_GENERATE_NEW_DATA_BEFORE,
                    this.cache.get(0).getData().getDate());
            }

            // evict excess data at end
            final AbsoluteDate t0 = this.cache.get(0).getData().getDate();
            while (this.cache.size() > TimeStampedCache.this.neighborsSize
                && this.cache.get(this.cache.size() - 1).getData().getDate().durationFrom(t0) > 
                TimeStampedCache.this.maxSpan) {
                this.cache.remove(this.cache.size() - 1);
            }

            // update boundaries
            this.earliestQuantum.set(this.cache.get(0).getQuantum());
            this.latestQuantum.set(this.cache.get(this.cache.size() - 1).getQuantum());

        }

        /**
         * Append data at slot end.
         * 
         * @param data
         *        data to append
         * @exception TimeStampedCacheException
         *            if new data cannot be generated
         */
        private void appendAtEnd(final List<T> data) throws TimeStampedCacheException {

            // append data at end
            boolean appended = false;
            final long qn = this.latestQuantum.get();
            final int n = this.cache.size();
            for (int i = data.size() - 1; i >= 0; --i) {
                final long quantum = TimeStampedCache.this.quantum(data.get(i).getDate());
                if (quantum > qn) {
                    // data quantum is after latest quantum, it can be inserted at the end
                    this.cache.add(n, new Entry(data.get(i), quantum));
                    appended = true;
                } else {
                    break;
                }
            }

            if (!appended) {
                // Insertion exception
                throw new TimeStampedCacheException(PatriusMessages.UNABLE_TO_GENERATE_NEW_DATA_AFTER,
                    this.cache.get(this.cache.size() - 1).getData().getDate());
            }

            // evict excess data at start
            final AbsoluteDate tn = this.cache.get(this.cache.size() - 1).getData().getDate();
            while (this.cache.size() > TimeStampedCache.this.neighborsSize &&
                tn.durationFrom(this.cache.get(0).getData().getDate()) > TimeStampedCache.this.maxSpan) {
                this.cache.remove(0);
            }

            // update boundaries
            this.earliestQuantum.set(this.cache.get(0).getQuantum());
            this.latestQuantum.set(this.cache.get(this.cache.size() - 1).getQuantum());

        }

        /**
         * Generate entries and check ordering.
         * 
         * @param existing
         *        closest already existing entry (may be null)
         * @param date
         *        date that must be covered by the range of the generated array
         *        (guaranteed to lie between {@link #getEarliest()} and {@link #getLatest()})
         * @return chronologically sorted list of generated entries
         * @exception TimeStampedCacheException
         *            if if entries are not chronologically
         *            sorted or if new data cannot be generated
         */
        private List<T> generateAndCheck(final T existing, final AbsoluteDate date) throws TimeStampedCacheException {
            final List<T> entries = TimeStampedCache.this.generator.generate(existing, date);
            if (entries.isEmpty()) {
                throw new TimeStampedCacheException(PatriusMessages.NO_DATA_GENERATED, date);
            }
            for (int i = 1; i < entries.size(); ++i) {
                if (entries.get(i).getDate().compareTo(entries.get(i - 1).getDate()) < 0) {
                    throw new TimeStampedCacheException(PatriusMessages.NON_CHRONOLOGICALLY_SORTED_ENTRIES,
                        entries.get(i - 1).getDate(),
                        entries.get(i).getDate());
                }
            }
            return entries;
        }

        /** Container for entries. */
        private class Entry implements Serializable {

            /** Serial UID. */
            private static final long serialVersionUID = 6785993636980995351L;

            /** Entry data. */
            private final T data;

            /** Global quantum of the entry. */
            private final long quantum;

            /**
             * Simple constructor.
             * 
             * @param dataIn
             *        entry data
             * @param quantumIn
             *        entry quantum
             */
            public Entry(final T dataIn, final long quantumIn) {
                this.quantum = quantumIn;
                this.data = dataIn;
            }

            /**
             * Get the quantum.
             * 
             * @return quantum
             */
            public long getQuantum() {
                return this.quantum;
            }

            /**
             * Get the data.
             * 
             * @return data
             */
            public T getData() {
                return this.data;
            }

        }
    }

}
