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
package fr.cnes.sirius.patrius.math.fraction;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.Field;
import fr.cnes.sirius.patrius.math.FieldElement;

/**
 * Representation of the fractional numbers without any overflow field.
 * <p>
 * This class is a singleton.
 * </p>
 * 
 * @see Fraction
 * @version $Id: BigFractionField.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 2.0
 */
public final class BigFractionField implements Field<BigFraction>, Serializable {

     /** Serializable UID. */
    private static final long serialVersionUID = -1699294557189741703L;

    /**
     * Private constructor for the singleton.
     */
    private BigFractionField() {
    }

    /**
     * Get the unique instance.
     * 
     * @return the unique instance
     */
    public static BigFractionField getInstance() {
        return LazyHolder.INSTANCE;
    }

    /** {@inheritDoc} */
    @Override
    public BigFraction getOne() {
        return BigFraction.ONE;
    }

    /** {@inheritDoc} */
    @Override
    public BigFraction getZero() {
        return BigFraction.ZERO;
    }

    /** {@inheritDoc} */
    @Override
    public Class<? extends FieldElement<BigFraction>> getRuntimeClass() {
        return BigFraction.class;
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
        private static final BigFractionField INSTANCE = new BigFractionField();
    }

}
