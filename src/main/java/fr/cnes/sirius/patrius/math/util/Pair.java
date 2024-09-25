/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
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
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11:DM:DM-3286:22/05/2023:[PATRIUS] Ajout d'une methode toString() a la classe Pair
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration gestion attractions gravitationnelles
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.util;

import java.io.Serializable;
import java.util.Objects;

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
@SuppressWarnings("PMD.ShortClassName")
public class Pair<K, V> implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 2705992726451588143L;

    /** Key. */
    private final K key;

    /** Value. */
    private final V value;


    /**
     * Create an entry representing a mapping from the specified key to the
     * specified value.
     *
     * @param k
     *        Key (first element of the pair)
     * @param v
     *        Value (second element of the pair)
     */
    public Pair(final K k, final V v) {
        this.key = k;
        this.value = v;
    }

    /**
     * Create an entry representing the same mapping as the specified entry.
     *
     * @param entry
     *        Entry to copy
     */
    public Pair(final Pair<? extends K, ? extends V> entry) {
        this(entry.getKey(), entry.getValue());
    }

    /**
     * Getter for the key.
     *
     * @return the key (first element of the pair).
     */
    public K getKey() {
        return this.key;
    }

    /**
     * Getter for the value.
     *
     * @return the value (second element of the pair).
     */
    public V getValue() {
        return this.value;
    }

    /**
     * Getter for the first element of the pair.
     *
     * @return the first element of the pair.
     * @since 3.1
     */
    public K getFirst() {
        return getKey();
    }

    /**
     * Getter for the second element of the pair.
     *
     * @return the second element of the pair.
     * @since 3.1
     */
    public V getSecond() {
        return getValue();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object o) {
        boolean isEqual = false;

        if (this == o) {
            isEqual = true;
        } else if (o instanceof Pair) {
            final Pair<?, ?> oP = (Pair<?, ?>) o;
            isEqual = Objects.equals(this.key, oP.key) && Objects.equals(this.value, oP.value);
        }
        return isEqual;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hash(this.key, this.value);
    }

    /**
     * Display the key and the value of the pair.
     *
     * @return String : the key and the value (k,v) contained in the pair
     */
    @Override
    public String toString() {
        return "{" + this.key.toString() + ", " + this.value.toString() + "}";
    }
}
