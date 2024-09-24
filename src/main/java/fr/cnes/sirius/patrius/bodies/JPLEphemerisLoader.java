/**
 * Copyright 2011-2022 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * HISTORY
 * VERSION:4.11.1:DM:DM-49:30/06/2023:[PATRIUS] Extraction arbre des reperes SPICE et link avec CelestialBodyFactory
 * VERSION:4.10.1:FA:FA-3267:02/12/2022:[PATRIUS] Anomalie dans gestion acceleration null du PVCoordinates (suite)
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.10:FA:FA-3202:03/11/2022:[PATRIUS] Renommage dans UserCelestialBody
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies;

import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Interface for JPL ephemeris loaders.
 *
 * @author Emmanuel Bignon
 *
 * @since 4.11
 */
public interface JPLEphemerisLoader extends CelestialBodyEphemerisLoader {
    
    /**
     * Get the gravitational coefficient of a body.
     *
     * @param body
     *        body for which the gravitational coefficient is requested
     * @return gravitational coefficient in m<sup>3</sup>/s<sup>2</sup>
     * @exception PatriusException
     *            if constants cannot be loaded
     */
    double getLoadedGravitationalCoefficient(final EphemerisType body) throws PatriusException;
}
