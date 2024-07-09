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
package fr.cnes.sirius.patrius.frames;

/**
 * Predefined frames provided by {@link FramesFactory}.
 * 
 * @author Luc Maisonobe
 */
public enum Predefined {

    /** GCRF frame. */
    GCRF(Frame.getRoot().getName()),

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

    /** EOD without EOP corrections. */
    EOD_WITHOUT_EOP_CORRECTIONS("EOD without EOP"),

    /** EOD with EOP corrections. */
    EOD_WITH_EOP_CORRECTIONS("EOD with EOP");

    /** Name fo the frame. */
    private final String name;

    /**
     * Simple constructor.
     * 
     * @param nameIn
     *        name of the frame
     */
    private Predefined(final String nameIn) {
        this.name = nameIn;
    }

    /**
     * Get the name of the frame.
     * 
     * @return name of the frame
     */
    public String getName() {
        return this.name;
    }

}
