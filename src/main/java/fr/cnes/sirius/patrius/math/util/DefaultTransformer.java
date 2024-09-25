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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.util;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * A Default NumberTransformer for java.lang.Numbers and Numeric Strings. This
 * provides some simple conversion capabilities to turn any java.lang.Number
 * into a primitive double or to turn a String representation of a Number into
 * a double.
 * 
 * @version $Id: DefaultTransformer.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class DefaultTransformer implements NumberTransformer, Serializable {

     /** Serializable UID. */
    private static final long serialVersionUID = 4019938025047800455L;

    /** Hashmap generator. */
    private static final int HASHMAP_GENERATOR = 401993047;

    /**
     * @param o
     *        the object that gets transformed.
     * @return a double primitive representation of the Object o.
     * @throws NullArgumentException
     *         if Object <code>o</code> is {@code null}.
     * @throws MathIllegalArgumentException
     *         if Object <code>o</code> cannot successfully be transformed
     * @see <a
     *      href="http://commons.apache.org/collections/api-release/org/apache/
     *      commons/collections/Transformer.html">Commons
     *      Collections Transformer</a>
     */
    @Override
    @SuppressWarnings("PMD.PreserveStackTrace")
    public double transform(final Object o) {

        if (o == null) {
            throw new NullArgumentException(PatriusMessages.OBJECT_TRANSFORMATION);
        }

        if (o instanceof Number) {
            return ((Number) o).doubleValue();
        }

        try {
            return Double.valueOf(o.toString());
        } catch (final NumberFormatException e) {
            throw new MathIllegalArgumentException(PatriusMessages.CANNOT_TRANSFORM_TO_DOUBLE,
                o.toString());
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof DefaultTransformer;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        // some arbitrary number ...
        return HASHMAP_GENERATOR;
    }

}
