/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
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
 */
package fr.cnes.sirius.patrius.math.util;

/**
 * Generic pair. <br/>
 * Although the instances of this class are immutable, it is impossible
 * to ensure that the references passed to the constructor will not be
 * modified by the caller.
 * 
 * @param <K>
 *        Key type.
 * @param <V>
 *        Value type.
 * 
 * @since 3.0
 * @version $Id: Pair.java 18108 2017-10-04 06:45:27Z bignon $
 */
@SuppressWarnings({"PMD.ShortClassName", "PMD.NullAssignment"})
public class Pair<K, V> {
    /** Key. */
    private final K key;
    /** Value. */
    private final V value;

    /**
     * Empty constructor.
     */
    public Pair() {
        this.key = null;
        this.value = null;
    }

    /**
     * Create an entry representing a mapping from the specified key to the
     * specified value.
     * 
     * @param k
     *        Key (first element of the pair).
     * @param v
     *        Value (second element of the pair).
     */
    public Pair(final K k, final V v) {
        this.key = k;
        this.value = v;
    }

    /**
     * Create an entry representing the same mapping as the specified entry.
     * 
     * @param entry
     *        Entry to copy.
     */
    public Pair(final Pair<? extends K, ? extends V> entry) {
        this(entry.getKey(), entry.getValue());
    }

    /**
     * Get the key.
     * 
     * @return the key (first element of the pair).
     */
    public K getKey() {
        return this.key;
    }

    /**
     * Get the value.
     * 
     * @return the value (second element of the pair).
     */
    public V getValue() {
        return this.value;
    }

    /**
     * Get the first element of the pair.
     * 
     * @return the first element of the pair.
     * @since 3.1
     */
    public K getFirst() {
        return this.key;
    }

    /**
     * Get the second element of the pair.
     * 
     * @return the second element of the pair.
     * @since 3.1
     */
    public V getSecond() {
        return this.value;
    }

    /**
     * Compare the specified object with this entry for equality.
     * 
     * @param o
     *        Object.
     * @return {@code true} if the given object is also a map entry and
     *         the two entries represent the same mapping.
     */
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof Pair) {
            final Pair<?, ?> oP = (Pair<?, ?>) o;
            return (this.key == null ?
                oP.key == null :
                this.key.equals(oP.key)) &&
                (this.value == null ?
                    oP.value == null :
                    this.value.equals(oP.value));
        } else {
            return false;
        }
    }

    // CHECKSTYLE: stop MagicNumber check
    // Reason: model - Commons-Math code

    /**
     * Compute a hash code.
     * 
     * @return the hash code value.
     */
    @Override
    public int hashCode() {
        int result = this.key == null ? 0 : this.key.hashCode();

        final int h = this.value == null ? 0 : this.value.hashCode();
        result = 37 * result + h ^ (h >>> 16);

        return result;
    }

    // CHECKSTYLE: resume MagicNumber check
}
