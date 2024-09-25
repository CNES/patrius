/**
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
package fr.cnes.sirius.patrius.utils;

import fr.cnes.sirius.patrius.utils.TimeStampedPVCoordinates;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Enumerate for selecting which derivatives to use in {@link TimeStampedPVCoordinates} and
 * interpolation.
 * 
 * @see AngularDerivativesFilter
 * @author Luc Maisonobe
 * @since 3.1
 */
public enum CartesianDerivativesFilter {

    /** Use only positions, ignoring velocities. */
    USE_P(0),

    /** Use positions and velocities. */
    USE_PV(1),

    /** Use positions, velocities and accelerations. */
    USE_PVA(2);

    /** Maximum derivation order. */
    private final int maxOrder;

    /**
     * Simple constructor.
     * 
     * @param maxOrderIn
     *        maximum derivation order
     */
    private CartesianDerivativesFilter(final int maxOrderIn) {
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
    public static CartesianDerivativesFilter getFilter(final int order) {
        for (final CartesianDerivativesFilter filter : values()) {
            if (filter.getMaxOrder() == order) {
                return filter;
            }
        }
        throw PatriusException.createIllegalArgumentException(PatriusMessages.OUT_OF_RANGE_DERIVATION_ORDER, order);
    }

}
