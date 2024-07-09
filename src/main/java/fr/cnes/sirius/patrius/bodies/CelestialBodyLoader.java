/**
 * Copyright 2002-2012 CS Systèmes d'Information
 * Copyright 2011-2017 CNES
 *
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
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
/*
 * 
 */
package fr.cnes.sirius.patrius.bodies;

import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Interface for loading celestial bodies.
 * 
 * @author Luc Maisonobe
 */
public interface CelestialBodyLoader {

    /**
     * Load celestial body.
     * 
     * @param name
     *        name of the celestial body
     * @return loaded celestial body
     * @throws PatriusException
     *         if the body cannot be loaded
     */
    CelestialBody loadCelestialBody(String name) throws PatriusException;

}
