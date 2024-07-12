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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies;

import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * List of supported ephemerides types (for use in JPL loaders classes).
 *
 * @author Emmanuel Bignon
 * 
 * @since 4.10
 */
public enum EphemerisType {

    /** Constant for solar system barycenter. */
    SOLAR_SYSTEM_BARYCENTER(CelestialBodyFactory.SOLAR_SYSTEM_BARYCENTER),

    /** Constant for the Sun. */
    SUN(CelestialBodyFactory.SUN),

    /** Constant for Mercury. */
    MERCURY(CelestialBodyFactory.MERCURY),

    /** Constant for Venus. */
    VENUS(CelestialBodyFactory.VENUS),

    /** Constant for the Earth-Moon barycenter. */
    EARTH_MOON(CelestialBodyFactory.EARTH_MOON),

    /** Constant for the Earth. */
    EARTH(CelestialBodyFactory.EARTH),

    /** Constant for the Moon. */
    MOON(CelestialBodyFactory.MOON),

    /** Constant for Mars. */
    MARS(CelestialBodyFactory.MARS),

    /** Constant for Jupiter. */
    JUPITER(CelestialBodyFactory.JUPITER),

    /** Constant for Saturn. */
    SATURN(CelestialBodyFactory.SATURN),

    /** Constant for Uranus. */
    URANUS(CelestialBodyFactory.URANUS),

    /** Constant for Neptune. */
    NEPTUNE(CelestialBodyFactory.NEPTUNE),

    /** Constant for Pluto. */
    PLUTO(CelestialBodyFactory.PLUTO);

    /** JPL name. */
    private final String name;

    /**
     * Constructor.
     * @param name JL name
     */
    private EphemerisType(final String name) {
        this.name = name;
    }

    /**
     * Returns the JPL name.
     * @return the JPL name
     */
    public String getName() {
        return name;
    }

    /**
     * Get ephemeris type from name.
     * @param name a name
     * @return ephemeris type from name
     * @throws PatriusException thrown if body name is unknown
     */
    public static EphemerisType getEphemerisType(final String name) throws PatriusException {
        for (final EphemerisType type : EphemerisType.values()) {
            if (type.getName().equals(name)) {
                return type;
            }
        }
        // Current workaround for EMB name which is different in JPL and BSP ephemeris files
        if (name.equals(CelestialBodyFactory.EARTH_MOON_BSP)) {
            return EphemerisType.EARTH_MOON;
        }
        throw new PatriusException(PatriusMessages.UNKNOWN_BODY, name);
    }
}
