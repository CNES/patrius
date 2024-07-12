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
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.tools.cache;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.function.Supplier;

import fr.cnes.sirius.patrius.math.exception.NotPositiveException;
import fr.cnes.sirius.patrius.utils.StringTablePrinter;

/**
 * This class implements a thread safe cache.<br>
 * It is based on a FirstInFirstOut (FIFO) structure. As soon as the structure reaches it's maximum size, adding a new
 * entry removes the oldest entry.
 * <p>
 * The tread-safety is handled by the use of the {@link ConcurrentLinkedDeque} implementation (which is a lock-free
 * implementation).
 * </p>
 * 
 * @param <K>
 *        The key, usually used to identify if the computation has already been performed
 * @param <V>
 *        The value, usually representing the result of the computation associated to the key
 * @author veuillh
 */
public class FIFOThreadSafeCache<K, V> implements Serializable {

    /**
     * Default max size for the cache: trade-off between the duration for look-up in the cache versus reuse of already
     * computed values.
     */
    public static final int DEFAULT_MAX_SIZE = 8;

    /** Percent conversion value. */
    public static final int PERCENT = 100;

    /** Serializable UID. */
    private static final long serialVersionUID = -788798667509530959L;

    /**
     * The max size of the structure. When this size is reached, the last elements of the structure start being removed.
     */
    private final int listMaxSize;

    /** The main structure of the cache. */
    private transient ConcurrentLinkedDeque<CacheEntry<K, V>> structure;

    /** Count the number of calls of the {@link #computeIf(Predicate, Supplier)} method. */
    private transient AtomicInteger nbCallComputeIf;

    /** Count the number of calls of the {@link #addEntry(CacheEntry)} method. */
    private transient AtomicInteger nbCallAddEntry;

    /** Indicates when the cache is full and needs to poll. */
    private transient volatile boolean needsPoll;

    /**
     * Constructor with default max size {@link #DEFAULT_MAX_SIZE}.
     */
    public FIFOThreadSafeCache() {
        this(DEFAULT_MAX_SIZE);
    }

    /**
     * Standard constructor.
     * 
     * @param listMaxSize
     *        The max size of the cache. 0 is a legitimate value emulating the absence of cache.
     * @throws NotPositiveException
     *         if {@code listMaxSize < 0}
     */
    public FIFOThreadSafeCache(final int listMaxSize) {
        // Check input
        if (listMaxSize < 0) {
            throw new NotPositiveException(listMaxSize);
        }

        this.listMaxSize = listMaxSize;
        this.structure = new ConcurrentLinkedDeque<>();
        this.nbCallComputeIf = new AtomicInteger(0);
        this.nbCallAddEntry = new AtomicInteger(0);
        this.needsPoll = false;
    }

    /**
     * Computes and add a new entry in the cache, if necessary.
     * 
     * @param cacheAvailabilityPredicate
     *        The predicate that test if one of the entries of the cache matches the requirement
     * @param entrySupplier
     *        The supplier that will build the entry if required
     * @return the entry computed
     */
    public CacheEntry<K, V> computeIf(final Predicate<CacheEntry<K, V>> cacheAvailabilityPredicate,
                                      final Supplier<CacheEntry<K, V>> entrySupplier) {

        // Increment the counter
        this.nbCallComputeIf.incrementAndGet();

        // Quick escape in case of no cache
        if (this.listMaxSize == 0) {
            return checkNotNull(entrySupplier.get());
        }

        // Look for an entry that matches the predicate in the internal structure
        CacheEntry<K, V> entry = getEntry(cacheAvailabilityPredicate);

        if (entry != null) {
            // The predicate matched an entry. It is directly returned
            return entry;
        }

        // The predicate did not match any entry. The entry needs to be computed.
        entry = checkNotNull(entrySupplier.get());

        // Note that in a multi-thread situation, the entry might be added while it has, meanwhile, been added in the
        // structure by another thread. Not a big deal...
        addEntry(entry);

        return entry;
    }

    /**
     * Computes and add a new entry in the cache, if necessary.
     * 
     * @param key
     *        The key to assess if the computation is required
     * @param entrySupplier
     *        The supplier that will build the entry if required
     * @return the entry computed
     */
    public CacheEntry<K, V> computeIfAbsent(final K key, final Supplier<CacheEntry<K, V>> entrySupplier) {
        return computeIf((cacheEntry) -> cacheEntry.getKey().equals(key), entrySupplier);
    }

    /**
     * Clears the cache.<br>
     * Note that this method might have non desired effects if it is called in concurrency with {@link #computeIf} (more
     * precisely with addEntry internal method).
     */
    public void clear() {
        // Implementation note: if an addEntry is called between one of theses commands, the counters might not be
        // representative anymore.
        this.structure.clear();
        this.nbCallComputeIf.set(0);
        this.nbCallAddEntry.set(0);
        this.needsPoll = false;
    }

    /**
     * Getter for the maximum size of the cache.
     * 
     * @return the maximum size of the cache
     */
    public int getMaxSize() {
        return this.listMaxSize;
    }

    /**
     * Provides the ratio of reusability of this cache.
     * <p>
     * This method can help to chose the size of the cache.
     * </p>
     * 
     * @return the reusability ratio (0 means no reusability at all, 0.5 means that the supplier is called only half
     *         time compared to the {@link #computeIf(Predicate, Supplier)} method)
     */
    public double getReusabilityRatio() {
        final int nbTotalCalls = this.nbCallComputeIf.get();
        final int nbAddEntryCalls = this.nbCallAddEntry.get();
        final double nbReusability = nbTotalCalls - nbAddEntryCalls;
        return nbReusability / nbTotalCalls;
    }

    /**
     * Returns a string representation of the cache structure.
     *
     * @return a string representation of the cache structure
     */
    @Override
    public String toString() {
        // Initialize the table with 2 columns
        final StringTablePrinter tablePrinter =
            new StringTablePrinter(this.getClass().getSimpleName(), new String[] { "Key ", "Value " });
        final String[] line = new String[2];

        // Loop over the structure to fill the table
        for (final CacheEntry<K, V> entry : this.structure) {
            line[0] = entry.getKey().toString();
            line[1] = entry.getValue().toString();
            tablePrinter.addLine(line);
        }
        // Add the final element and print it
        tablePrinter.addBoldLineSeparator();
        tablePrinter.addLine(new String[] { "Reusability ratio",
            String.format(Locale.US, "%.2f%%", getReusabilityRatio() * PERCENT) });
        tablePrinter.addBoldLineSeparator();
        return tablePrinter.toString();
    }

    /**
     * Internal method to look for an entry that matches the predicate. Returns null if no entry matched the predicate.
     * 
     * <p>
     * Note that the internal structure can be modified while this method is called. Thus, {@code null} can be returned
     * while the key was being added or a value can be returned while it was being suppressed, but it is not a big deal.
     * <br>
     * See {@link ConcurrentLinkedDeque#iterator()} for information about the consistency of the iterator.
     * </p>
     * 
     * @param cacheAvailabilityPredicate
     *        The predicate that needs to be matched to return an entry of the internal structure
     * @return the entry that matched the predicate. Might be null if no entry matched
     */
    private CacheEntry<K, V> getEntry(final Predicate<CacheEntry<K, V>> cacheAvailabilityPredicate) {
        for (final CacheEntry<K, V> entry : this.structure) {
            if (cacheAvailabilityPredicate.test(entry)) {
                return entry;
            }
        }
        return null;
    }

    /**
     * Add an entry in the structure. It will be added in the first position.
     * 
     * @param newFirstEntry
     *        The entry that should be added
     */
    private void addEntry(final CacheEntry<K, V> newFirstEntry) {

        // First step: add the entry in the first position of the structure
        this.structure.addFirst(newFirstEntry);

        // Increment the counter
        final int nbCall = this.nbCallAddEntry.incrementAndGet();

        // Second step: if the maximum size is reached switch the boolean value,
        // the next time the last element of the structure will be removed
        if (this.needsPoll) {
            this.structure.pollLast();
        } else if (nbCall >= this.listMaxSize) {
            this.needsPoll = true;
        }
    }

    /**
     * Internal method to check that the supplier does not provide null entry.
     * 
     * @param entry
     *        The entry to check
     * @return the entry
     * @throws IllegalStateException
     *         if {@code entry} is null (the entry supplier must generate a not null entry)
     */
    private CacheEntry<K, V> checkNotNull(final CacheEntry<K, V> entry) {
        if (entry == null) {
            throw new IllegalStateException("The entry supplier must generate a not null entry");
        }
        return entry;
    }

    /**
     * Private method for the serialization. It reinitializes the transient attributes.
     * 
     * @param aInputStream
     *        stream where the cache should be written
     * @throws ClassNotFoundException
     *         if a class in the stream cannot be found
     * @throws IOException
     *         if object cannot be read from the stream
     */
    private void readObject(final ObjectInputStream aInputStream) throws ClassNotFoundException, IOException {
        // Perform the default deserialization first
        aInputStream.defaultReadObject();

        // Handle transient attributes
        this.structure = new ConcurrentLinkedDeque<>();
        this.nbCallComputeIf = new AtomicInteger(0);
        this.nbCallAddEntry = new AtomicInteger(0);
        this.needsPoll = false;
    }
}
