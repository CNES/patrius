/**
 * Copyright 2011-2022 CNES
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
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies;

import java.io.Serializable;

import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Interface for loading celestial bodies ephemeris.
 * 
 * @author Emmanuel Bignon
 * 
 * @since 4.10
 */
public interface CelestialBodyEphemerisLoader extends Serializable {

    /**
     * Load celestial body ephemeris.
     * 
     * @param name
     *        name of the celestial body
     * @return loaded celestial body ephemeris
     * @throws PatriusException
     *         if the body ephemeris cannot be loaded
     */
    CelestialBodyEphemeris loadCelestialBodyEphemeris(String name) throws PatriusException;

}