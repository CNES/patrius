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
 * VERSION:4.13:DM:DM-3:08/12/2023:[PATRIUS] Distinction entre corps celestes et barycentres
 * VERSION:4.13:FA:FA-118:08/12/2023:[PATRIUS] Calcul d'union de PyramidalField invalide
 * VERSION:4.13:FA:FA-111:08/12/2023:[PATRIUS] Problemes lies à  l'utilisation des bsp
 * VERSION:4.11.1:DM:DM-49:30/06/2023:[PATRIUS] Extraction arbre des reperes SPICE et link avec CelestialBodyFactory
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies;



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

    /** JPL/PATRIUS name. */
    private final String name;

    /**
     * Constructor.
     * 
     * @param name JL name
     */
    private EphemerisType(final String name) {
        this.name = name;
    }

    /**
     * Returns the JPL/PATRIUS name.
     * 
     * @return the JPL/PATRIUS name
     */
    public String getName() {
        return name;
    }

    /**
     * Get ephemeris type from JPL/PATRIUS name.
     * 
     * @param name a name
     * @return ephemeris type from JPL/PATRIUS name, null if unknown
     */
    public static EphemerisType getEphemerisType(final String name) {
        for (final EphemerisType type : EphemerisType.values()) {
            if (type.getName().equals(name)) {
                return type;
            }
        }
        
        // Non-existent type
        return null;
    }
}
