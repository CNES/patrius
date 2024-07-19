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
 * VERSION:4.11:DM:DM-3311:22/05/2023:[PATRIUS] Evolutions mineures sur CelestialBody, shape et reperes
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3163:10/05/2022:[PATRIUS] Enrichissement des reperes planetaires 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies;

/**
 * IAU pole type: this enumeration lists all possible IAU pole effects (constant, secular, harmonics)
 * used to convert from ICRF body centered frame to true equator/rotating body centered frame.
 * Note that when used in {@link UserIAUPole}, effects are cumulative. One cannot only get the harmonic effects alone
 * for instance.
 */
public enum IAUPoleModelType {

    /** Constant part only (transformation between ICRF and inertially-oriented frame). */
    CONSTANT {
        /** {@inheritDoc} */
        @Override
        boolean accept(final IAUPoleFunctionType type) {
            return type.equals(IAUPoleFunctionType.CONSTANT);
        }
    },

    /** Constant + secular effects (transformation between inertially-oriented and mean equator/rotating frame). */
    MEAN {
        /** {@inheritDoc} */
        @Override
        boolean accept(final IAUPoleFunctionType type) {
            return type.equals(IAUPoleFunctionType.CONSTANT) || type.equals(IAUPoleFunctionType.SECULAR);
        }
    },

    /**
     * Constant + secular + harmonic effects (transformation between inertially-oriented and true equator/rotating
     * frame).
     */
    TRUE {
        /** {@inheritDoc} */
        @Override
        boolean accept(final IAUPoleFunctionType type) {
            return true;
        }
    };

    /**
     * Returns true if enum takes into account the provided type, false otherwise.
     * @param type IAU pole type
     * @return true if enum takes into account the provided type, false otherwise
     */
    abstract boolean accept(final IAUPoleFunctionType type);
}
