/**
 * Copyright 2002-2012 CS Systèmes d'Information
 * Copyright 2011-2022 CNES
 *
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
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies;

/**
 * This enumerate define the handled LLH (Latitude/Longitude/Height) coordinates systems.<br>
 * Each coordinates system is defined by:
 * <ul>
 * <li>A lat/long coordinates system</li>
 * <li>An height coordinate system</li>
 * </ul>
 *
 * @author Alice Latourte
 */
public enum LLHCoordinatesSystem {
    
    //CHECKSTYLE: stop MultipleStringLiterals check
    //Reason: cannot define String constant in enum class

    /** Ellipsodetic latitude/longitude and normal height: applicable to ellipsoid shapes only. */
    ELLIPSODETIC("surface ellipsodetic coord", "normal height"),

    /** Bodycentric latitude/longitude, and radial height. */
    BODYCENTRIC_RADIAL("surface bodycentric coord", "radial height"),

    /** Bodycentric latitude/longitude, and normal height. */
    BODYCENTRIC_NORMAL("surface bodycentric coord", "normal height");

    /** Label for the managed lat/long system. */
    private final String latLongSystemLabel;

    /** Label for the managed height system. */
    private final String heightSystemLabel;

    /**
     * Private constructor.
     *
     * @param latLongSystemLabel
     *        label for the managed lat/long coordinates system
     * @param heightSystemLabel
     *        label for the managed height coordinate system
     */
    private LLHCoordinatesSystem(final String latLongSystemLabel, final String heightSystemLabel) {
        this.latLongSystemLabel = latLongSystemLabel;
        this.heightSystemLabel = heightSystemLabel;
    }

    /**
     * Getter for the label for the managed lat/long coordinates system.
     * 
     * @return the label for the managed lat/long coordinates system
     */
    public final String getLatLongSystemLabel() {
        return this.latLongSystemLabel;
    }

    /**
     * Getter for the label for the managed height coordinate system.
     * 
     * @return the label for the managed height coordinate system
     */
    public final String getHeightSystemLabel() {
        return this.heightSystemLabel;
    }

    //CHECKSTYLE: resume MultipleStringLiterals check
}
