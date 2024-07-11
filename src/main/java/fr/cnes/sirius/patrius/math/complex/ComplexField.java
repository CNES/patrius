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
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.complex;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.Field;
import fr.cnes.sirius.patrius.math.FieldElement;

/**
 * Representation of the complex numbers field.
 * <p>
 * This class is a singleton.
 * </p>
 * 
 * @see Complex
 * @version $Id: ComplexField.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 2.0
 */
public final class ComplexField implements Field<Complex>, Serializable {

    /** Serializable version identifier. */
    private static final long serialVersionUID = -6130362688700788798L;

    /**
     * Private constructor for the singleton.
     */
    private ComplexField() {
    }

    /**
     * Get the unique instance.
     * 
     * @return the unique instance
     */
    public static ComplexField getInstance() {
        return LazyHolder.INSTANCE;
    }

    /** {@inheritDoc} */
    @Override
    public Complex getOne() {
        return Complex.ONE;
    }

    /** {@inheritDoc} */
    @Override
    public Complex getZero() {
        return Complex.ZERO;
    }

    /** {@inheritDoc} */
    @Override
    public Class<? extends FieldElement<Complex>> getRuntimeClass() {
        return Complex.class;
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
        private static final ComplexField INSTANCE = new ComplexField();
    }

}
