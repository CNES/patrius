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

import fr.cnes.sirius.patrius.math.exception.NullArgumentException;

/**
 * Immutable class representing an entry of the cache {@link FIFOThreadSafeCache}.
 * 
 * @param <K>
 *        The key, usually used to identify if the computation has already been performed
 * @param <V>
 *        The value, usually representing the result of the computation associated to the key
 * @author veuillh
 */
public class CacheEntry<K, V> {

    /** The key of the entry. */
    private final K key;

    /** The value of the entry. */
    private final V value;

    /**
     * Standard constructor.
     * 
     * @param key
     *        The key
     * @param value
     *        The value
     * @throws NullArgumentException
     *         if {@code key} or {@code value} is null
     */
    public CacheEntry(final K key, final V value) {
        // Check inputs
        if (key == null || value == null) {
            throw new NullArgumentException();
        }

        this.key = key;
        this.value = value;
    }

    /**
     * Returns a string representation of the cache entry.
     *
     * @return a string representation of the cache entry
     */
    @Override
    public String toString() {
        return this.key.toString() + " : " + this.value.toString();
    }

    /**
     * Getter for the key of the entry.
     * 
     * @return the key of the entry
     */
    public K getKey() {
        return this.key;
    }

    /**
     * Getter for the value of the entry.
     * 
     * @return the value of the entry
     */
    public V getValue() {
        return this.value;
    }
}
