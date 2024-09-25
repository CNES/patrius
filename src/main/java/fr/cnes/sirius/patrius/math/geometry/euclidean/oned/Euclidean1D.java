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
package fr.cnes.sirius.patrius.math.geometry.euclidean.oned;

import fr.cnes.sirius.patrius.math.exception.MathUnsupportedOperationException;
import fr.cnes.sirius.patrius.math.geometry.Space;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This class implements a one-dimensional space.
 * 
 * @version $Id: Euclidean1D.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.0
 */
public final class Euclidean1D implements Space {

     /** Serializable UID. */
    private static final long serialVersionUID = -1178039568877797126L;

    /**
     * Private constructor for the singleton.
     */
    private Euclidean1D() {
    }

    /**
     * Get the unique instance.
     * 
     * @return the unique instance
     */
    public static Euclidean1D getInstance() {
        return LazyHolder.INSTANCE;
    }

    /** {@inheritDoc} */
    @Override
    public int getDimension() {
        return 1;
    }

    /**
     * {@inheritDoc}
     * <p>
     * As the 1-dimension Euclidean space does not have proper sub-spaces, this method always throws a
     * {@link MathUnsupportedOperationException}
     * </p>
     * 
     * @return nothing
     * @throws MathUnsupportedOperationException
     *         in all cases
     */
    @Override
    public Space getSubSpace() {
        throw new MathUnsupportedOperationException(PatriusMessages.NOT_SUPPORTED_IN_DIMENSION_N, 1);
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
        private static final Euclidean1D INSTANCE = new Euclidean1D();
    }
}
