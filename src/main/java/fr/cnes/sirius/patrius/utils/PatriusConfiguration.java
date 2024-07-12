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

/**
 * Utility class for setting global configuration parameters.
 * 
 * @author Luc Maisonobe
 */
public final class PatriusConfiguration {

    /** Number of slots to use in caches. */
    private static int cacheSlotNumber;

    /** Default cache slot number. */
    private static final int DEFAULT_CACHE_SLOT_NUMBER = 100;

    static {
        cacheSlotNumber = DEFAULT_CACHE_SLOT_NUMBER;
    }

    /**
     * Private constructor.
     * <p>
     * This class is a utility class, it should neither have a public nor a default constructor. This private
     * constructor prevents the compiler from generating one automatically.
     * </p>
     */
    private PatriusConfiguration() {
    }

    /**
     * Set the number of slots to use in caches.
     * 
     * @param slotsNumber
     *        number of slots to use in caches
     */
    public static void setCacheSlotsNumber(final int slotsNumber) {
        PatriusConfiguration.cacheSlotNumber = slotsNumber;
    }

    /**
     * Get the number of slots to use in caches.
     * 
     * @return number of slots to use in caches
     */
    public static int getCacheSlotsNumber() {
        return cacheSlotNumber;
    }

}
