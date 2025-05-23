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
 * VERSION:4.13:DM:DM-68:08/12/2023:[PATRIUS] Ajout du repere G50 CNES
 * VERSION:4.13:DM:DM-3:08/12/2023:[PATRIUS] Distinction entre corps celestes et barycentres
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3166:10/05/2022:[PATRIUS] Definir l'ICRF comme repere racine 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
/*
 * 
 */
package fr.cnes.sirius.patrius.frames;

import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.CelestialPoint;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Predefined frames provided by {@link FramesFactory}.
 * 
 * @author Luc Maisonobe
 */
public enum Predefined {

    /** GCRF frame. */
    GCRF("GCRF"),

    /** Earth-Moon barycenter frame. */
    EMB("Earth-Moon barycenter ICRF frame"),

    /** ICRF frame. */
    ICRF("ICRF"),

    /** EME2000 frame. */
    EME2000("EME2000"),

    /** ITRF. */
    ITRF("ITRF"),

    /** Equinox-based ITRF. */
    ITRF_EQUINOX("Equinox-based ITRF"),

    /** TIRF. */
    TIRF("TIRF"),

    /** CIRF frame. */
    CIRF("CIRF"),

    /** Veis 1950 with tidal effects. */
    VEIS_1950("VEIS1950"),

    /** G50 (= Gamma 50). */
    G50("G50"),

    /** GTOD without EOP corrections. */
    GTOD_WITHOUT_EOP_CORRECTIONS("GTOD without EOP"),

    /** GTOD with EOP corrections. */
    GTOD_WITH_EOP_CORRECTIONS("GTOD with EOP"),

    /** TOD without EOP corrections. */
    TOD_WITHOUT_EOP_CORRECTIONS("TOD without EOP"),

    /** TOD with EOP corrections. */
    TOD_WITH_EOP_CORRECTIONS("TOD with EOP"),

    /** MOD without EOP corrections. */
    MOD_WITHOUT_EOP_CORRECTIONS("MOD without EOP"),

    /** MOD with EOP corrections. */
    MOD_WITH_EOP_CORRECTIONS("MOD with EOP"),

    /** TEME frame. */
    TEME("TEME"),

    /** ECLIPTIC_MOD without EOP corrections. */
    ECLIPTIC_MOD_WITHOUT_EOP_CORRECTIONS("ECLIPTIC_MOD without EOP"),

    /** ECLIPTIC_MOD with EOP corrections. */
    ECLIPTIC_MOD_WITH_EOP_CORRECTIONS("ECLIPTIC_MOD with EOP"),

    /** ECLIPTIC J2000 */
    ECLIPTIC_J2000("Ecliptic J2000");

    /** Name fo the frame. */
    private final String name;

    /**
     * Constructor.
     * 
     * @param name
     *        name of the frame
     */
    private Predefined(final String name) {
        this.name = name;
    }

    /**
     * Get the name of the frame.
     * 
     * @return name of the frame
     */
    public String getName() {
        return this.name;
    }
    
    /**
     * Returns the celestial point associated to Predefined frame key.
     * 
     * @return the celestial point associated to Predefined frame key
     * @throws PatriusException thrown if celestial point could not be built
     */
    public CelestialPoint getCelestialPoint() throws PatriusException {
        // Celestial point cannot be an attribute of Predefined, since it needs to be built on the fly
        switch (this) {
            case ICRF:
                // ICRF
                return CelestialBodyFactory.getSolarSystemBarycenter();
            case EMB:
                // EMB
                return CelestialBodyFactory.getEarthMoonBarycenter();
            default:
                // Earth case for all other frames
                return CelestialBodyFactory.getEarth();
        }
    }
}
