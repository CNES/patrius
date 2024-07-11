/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 *
 * Copyright 2011-2022 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.util;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;

/**
 * This TansformerMap automates the transformation of mixed object types.
 * It provides a means to set NumberTransformers that will be selected
 * based on the Class of the object handed to the Maps <code>double transform(Object o)</code> method.
 * 
 * @version $Id: TransformerMap.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class TransformerMap implements NumberTransformer, Serializable {

    /** Hashcode generator. */
    private static final int THIRTY_ONE = 31;

    /** Serializable version identifier */
    private static final long serialVersionUID = 4605318041528645258L;

    /**
     * A default Number Transformer for Numbers and numeric Strings.
     */
    private final NumberTransformer defaultTransformer;

    /**
     * The internal Map.
     */
    private final Map<Class<?>, NumberTransformer> map;

    /**
     * Build a map containing only the default transformer.
     */
    public TransformerMap() {
        this.map = new HashMap<Class<?>, NumberTransformer>();
        this.defaultTransformer = new DefaultTransformer();
    }

    /**
     * Tests if a Class is present in the TransformerMap.
     * 
     * @param key
     *        Class to check
     * @return true|false
     */
    public boolean containsClass(final Class<?> key) {
        return this.map.containsKey(key);
    }

    /**
     * Tests if a NumberTransformer is present in the TransformerMap.
     * 
     * @param value
     *        NumberTransformer to check
     * @return true|false
     */
    public boolean containsTransformer(final NumberTransformer value) {
        return this.map.containsValue(value);
    }

    /**
     * Returns the Transformer that is mapped to a class
     * if mapping is not present, this returns null.
     * 
     * @param key
     *        The Class of the object
     * @return the mapped NumberTransformer or null.
     */
    public NumberTransformer getTransformer(final Class<?> key) {
        return this.map.get(key);
    }

    /**
     * Sets a Class to Transformer Mapping in the Map. If
     * the Class is already present, this overwrites that
     * mapping.
     * 
     * @param key
     *        The Class
     * @param transformer
     *        The NumberTransformer
     * @return the replaced transformer if one is present
     */
    public NumberTransformer putTransformer(final Class<?> key, final NumberTransformer transformer) {
        return this.map.put(key, transformer);
    }

    /**
     * Removes a Class to Transformer Mapping in the Map.
     * 
     * @param key
     *        The Class
     * @return the removed transformer if one is present or
     *         null if none was present.
     */
    public NumberTransformer removeTransformer(final Class<?> key) {
        return this.map.remove(key);
    }

    /**
     * Clears all the Class to Transformer mappings.
     */
    public void clear() {
        this.map.clear();
    }

    /**
     * Returns the Set of Classes used as keys in the map.
     * 
     * @return Set of Classes
     */
    public Set<Class<?>> classes() {
        return this.map.keySet();
    }

    /**
     * Returns the Set of NumberTransformers used as values
     * in the map.
     * 
     * @return Set of NumberTransformers
     */
    public Collection<NumberTransformer> transformers() {
        return this.map.values();
    }

    /**
     * Attempts to transform the Object against the map of
     * NumberTransformers. Otherwise it returns Double.NaN.
     * 
     * @param o
     *        the Object to be transformed.
     * @return the double value of the Object.
     * @throws MathIllegalArgumentException
     *         if the Object can not be
     *         transformed into a Double.
     * @see fr.cnes.sirius.patrius.math.util.NumberTransformer#transform(java.lang.Object)
     */
    @Override
    public double transform(final Object o) {
        double value = Double.NaN;

        if (o instanceof Number || o instanceof String) {
            value = this.defaultTransformer.transform(o);
        } else {
            final NumberTransformer trans = this.getTransformer(o.getClass());
            if (trans != null) {
                value = trans.transform(o);
            }
        }

        return value;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object other) {
        // return true if the object are exactyle the same
        if (this == other) {
            return true;
        }
        if (other instanceof TransformerMap) {
            // cast in TransformerMap
            final TransformerMap rhs = (TransformerMap) other;
            if (!this.defaultTransformer.equals(rhs.defaultTransformer)) {
                return false;
            }
            // return false if the size are different
            if (this.map.size() != rhs.map.size()) {
                return false;
            }
            // checks each entry and return false if one of this is different
            for (final Map.Entry<Class<?>, NumberTransformer> entry : this.map.entrySet()) {
                if (!entry.getValue().equals(rhs.map.get(entry.getKey()))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        int hash = this.defaultTransformer.hashCode();
        for (final NumberTransformer t : this.map.values()) {
            hash = hash * THIRTY_ONE + t.hashCode();
        }
        return hash;
    }

}
