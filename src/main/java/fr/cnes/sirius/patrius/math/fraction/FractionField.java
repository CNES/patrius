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
package fr.cnes.sirius.patrius.math.fraction;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.Field;
import fr.cnes.sirius.patrius.math.FieldElement;

/**
 * Representation of the fractional numbers field.
 * <p>
 * This class is a singleton.
 * </p>
 * 
 * @see Fraction
 * @version $Id: FractionField.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 2.0
 */
public final class FractionField implements Field<Fraction>, Serializable {

    /** Serializable version identifier */
    private static final long serialVersionUID = -1257768487499119313L;

    /**
     * Private constructor for the singleton.
     */
    private FractionField() {
    }

    /**
     * Get the unique instance.
     * 
     * @return the unique instance
     */
    public static FractionField getInstance() {
        return LazyHolder.INSTANCE;
    }

    /** {@inheritDoc} */
    @Override
    public Fraction getOne() {
        return Fraction.ONE;
    }

    /** {@inheritDoc} */
    @Override
    public Fraction getZero() {
        return Fraction.ZERO;
    }

    /** {@inheritDoc} */
    @Override
    public Class<? extends FieldElement<Fraction>> getRuntimeClass() {
        return Fraction.class;
    }

    /**
     * Handle deserialization of the singleton.
     * 
     * @return the singleton instance
     */
    private Object readResolve() {
        // return the singleton instance
        return LazyHolder.INSTANCE;
    }

    /**
     * Holder for the instance.
     * <p>
     * We use here the Initialization On Demand Holder Idiom.
     * </p>
     */
    private static class LazyHolder {
        /** Cached field instance. */
        private static final FractionField INSTANCE = new FractionField();
    }

}
