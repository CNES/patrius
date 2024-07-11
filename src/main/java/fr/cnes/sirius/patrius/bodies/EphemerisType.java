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
    SOLAR_SYSTEM_BARYCENTER,

    /** Constant for the Sun. */
    SUN,

    /** Constant for Mercury. */
    MERCURY,

    /** Constant for Venus. */
    VENUS,

    /** Constant for the Earth-Moon barycenter. */
    EARTH_MOON,

    /** Constant for the Earth. */
    EARTH,

    /** Constant for the Moon. */
    MOON,

    /** Constant for Mars. */
    MARS,

    /** Constant for Jupiter. */
    JUPITER,

    /** Constant for Saturn. */
    SATURN,

    /** Constant for Uranus. */
    URANUS,

    /** Constant for Neptune. */
    NEPTUNE,

    /** Constant for Pluto. */
    PLUTO

}
