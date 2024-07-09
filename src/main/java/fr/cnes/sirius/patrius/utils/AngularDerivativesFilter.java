/**
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
 *
 */
/* Copyright 2002-2015 CS Systèmes d'Information
 *
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:489:06/10/2015:Refactoring of rotation acceleration by the AttitudeProvider
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.utils;

import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Enumerate for selecting which derivatives to use in {@link TimeStampedAngularCoordinates} interpolation.
 * 
 * @see CartesianDerivativesFilter
 * @author Luc Maisonobe
 * @since 3.1
 */
public enum AngularDerivativesFilter {

    /** Use only rotations, ignoring rotation rates. */
    USE_R(0),

    /** Use rotations and rotation rates. */
    USE_RR(1),

    /** Use rotations, rotation rates and acceleration. */
    USE_RRA(2);

    /** Maximum derivation order. */
    private final int maxOrder;

    /**
     * Simple constructor.
     * 
     * @param maxOrderIn
     *        maximum derivation order
     */
    private AngularDerivativesFilter(final int maxOrderIn) {
        this.maxOrder = maxOrderIn;
    }

    /**
     * Get the maximum derivation order.
     * 
     * @return maximum derivation order
     */
    public int getMaxOrder() {
        return this.maxOrder;
    }

    /**
     * Get the filter corresponding to a maximum derivation order.
     * 
     * @param order
     *        maximum derivation order
     * @return the month corresponding to the string
     * @exception IllegalArgumentException
     *            if the order is out of range
     */
    public static AngularDerivativesFilter getFilter(final int order) {
        for (final AngularDerivativesFilter filter : values()) {
            if (filter.getMaxOrder() == order) {
                return filter;
            }
        }
        throw PatriusException.createIllegalArgumentException(PatriusMessages.OUT_OF_RANGE_DERIVATION_ORDER, order);
    }

}
