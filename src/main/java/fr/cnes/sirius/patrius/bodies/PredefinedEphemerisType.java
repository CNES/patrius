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
 * VERSION:4.14:OPENFD-258:22/08/2024:[PATRIUS] Ephemerides des barycentres planetaires
 * dans les fichiers JPL historiques
 * VERSION:4.14:OPENFD-172:22/08/2024:[PATRIUS] Harmonisation de la gestion
 * des reperes predefinis et des corps predefinis
 * VERSION:4.14:OPENFD-311:22/08/2024: [PATRIUS] getInputCoord sur EllipsoidPoint
 * VERSION:4.14:OPENFD-253:22/08/2024: [PATRIUS] Problemes e l'utilisation des bsp planetaires
 * VERSION:4.13:DM:DM-3:08/12/2023:[PATRIUS] Distinction entre corps celestes et barycentres
 * VERSION:4.13:FA:FA-118:08/12/2023:[PATRIUS] Calcul d'union de PyramidalField invalide
 * VERSION:4.13:FA:FA-111:08/12/2023:[PATRIUS] Problemes lies à  l'utilisation des bsp
 * VERSION:4.11.1:DM:DM-49:30/06/2023:[PATRIUS] Extraction arbre des reperes SPICE et link avec CelestialBodyFactory
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies;



/**
 * List of predefined ephemerides types (for use in JPL loaders classes).
 *
 * @author Emmanuel Bignon
 *
 * @since 4.10
 */
public enum PredefinedEphemerisType {

    /** Constant for solar system barycenter. */
    SOLAR_SYSTEM_BARYCENTER(CelestialBodyFactory.SOLAR_SYSTEM_BARYCENTER, true),

    /** Constant for the Sun. */
    SUN(CelestialBodyFactory.SUN, false),

    /** Constant for Mercury. */
    MERCURY(CelestialBodyFactory.MERCURY, false),

    /** Constant for Venus. */
    VENUS(CelestialBodyFactory.VENUS, false),

    /** Constant for the Earth-Moon barycenter. */
    EARTH_MOON(CelestialBodyFactory.EARTH_MOON, true),

    /** Constant for the Earth. */
    EARTH(CelestialBodyFactory.EARTH, false),

    /** Constant for the Moon. */
    MOON(CelestialBodyFactory.MOON, false),

    /** Constant for Mars. */
    MARS(CelestialBodyFactory.MARS, false),

    /** Constant for Mars barycenter. */
    MARS_BARY(CelestialBodyFactory.MARS_BARY, true),

    /** Constant for Jupiter. */
    JUPITER(CelestialBodyFactory.JUPITER, false),

    /** Constant for Jupiter barycenter. */
    JUPITER_BARY(CelestialBodyFactory.JUPITER_BARY, true),

    /** Constant for Saturn. */
    SATURN(CelestialBodyFactory.SATURN, false),

    /** Constant for Saturn barycenter. */
    SATURN_BARY(CelestialBodyFactory.SATURN_BARY, true),

    /** Constant for Uranus. */
    URANUS(CelestialBodyFactory.URANUS, false),

    /** Constant for Uranus barycenter. */
    URANUS_BARY(CelestialBodyFactory.URANUS_BARY, true),

    /** Constant for Neptune. */
    NEPTUNE(CelestialBodyFactory.NEPTUNE, false),

    /** Constant for Neptune barycenter. */
    NEPTUNE_BARY(CelestialBodyFactory.NEPTUNE_BARY, true),

    /** Constant for Pluto. */
    PLUTO(CelestialBodyFactory.PLUTO, false),

    /** Constant for Pluto barycenter. */
    PLUTO_BARY(CelestialBodyFactory.PLUTO_BARY, true);

    /** JPL/PATRIUS name. */
    private final String name;

    /** Boolean to identify barycenters. */
    private final boolean isBarycenter;

    /**
     * Constructor.
     *
     * @param name
     *        JL name
     * @param isBarycenter
     *        true if the PredefinedEphemerisType is a barycenter
     */
    private PredefinedEphemerisType(final String name, final boolean isBarycenter) {
        this.name = name;
        this.isBarycenter = isBarycenter;
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
     * @return true if the PredefinedEphemerisType is a barycenter.
     */
    public boolean isBarycenter() {
        return isBarycenter;
    }

    /**
     * Get ephemeris type from JPL/PATRIUS name.
     *
     * @param name a name
     * @return ephemeris type from JPL/PATRIUS name, null if unknown
     */
    public static PredefinedEphemerisType getEphemerisType(final String name) {

        // Initialise the type to be returned
        PredefinedEphemerisType typeToReturn = null;

        // Go through the different existing types
        for (final PredefinedEphemerisType type : PredefinedEphemerisType.values()) {
            if (type.getName().equals(name)) {
                typeToReturn = type;
                break;
            }
        }
        // null if type is not existing
        return typeToReturn;
    }
}
