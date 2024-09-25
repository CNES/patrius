/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 * VERSION:4.13:DM:DM-3:08/12/2023:[PATRIUS] Distinction entre corps celestes et barycentres
 * VERSION:4.13:FA:FA-111:08/12/2023:[PATRIUS] Problemes lies à  l'utilisation des bsp
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
/*
 * 
 */
package fr.cnes.sirius.patrius.bodies;

import java.io.Serializable;

import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Interface for loading celestial bodies/points.
 * <p>
 * A point {@link CelestialPoint} is simpler than a {@link CelestialBody}: it does not contains a gravity field, a shape
 * or IAU data.
 * </p>
 * <p>
 * The two methods {@link #loadCelestialPoint(String)} and {@link #loadCelestialBody(String)} returns the same data but
 * built in different objects type ({@link CelestialPoint} or {@link CelestialBody}).
 * </p>
 * 
 * @author Luc Maisonobe
 */
public interface CelestialBodyLoader extends Serializable {

    /**
     * Load celestial point.
     * 
     * @param name
     *        name of the celestial point
     * @return loaded celestial point
     * @throws PatriusException
     *         if the point cannot be loaded
     */
    CelestialPoint loadCelestialPoint(String name) throws PatriusException;

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

    /**
     * Returns name of body as known by the loader corresponding to PATRIUS body name.
     * @param patriusName PATRIUS body name
     * @return name of body as known by the loader corresponding to PATRIUS body name
     */
    String getName(final String patriusName);
}
