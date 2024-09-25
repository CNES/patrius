/**
 * Copyright 2023-2023 CNES
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
 * VERSION:4.13:DM:DM-108:08/12/2023:[PATRIUS] Modele d'obliquite et de precession de la Terre
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.configuration.modprecession;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.time.AbsoluteDate;

/**
 * This interface provides methods used to compute the GCRF/EME2000 to MOD and MOD to Ecliptic MOD transformations.
 *
 * @author Emmanuel Bignon
 *
 * @since 4.13
 */
public interface MODPrecessionModel extends Serializable {

    /**
     * Getter for the Earth obliquity at provided date used in MOD to Ecliptic MOD transformation.
     *
     * @param date date
     * @return the Earth obliquity at provided date used in MOD to Ecliptic MOD transformation
     */
    double getEarthObliquity(AbsoluteDate date);

    /**
     * Getter for the MOD precession transformation from GCRF/EME2000 to MOD at provided date.
     *
     * @param date date
     * @return the MOD precession rotation from GCRF/EME2000 to MOD at provided date
     */
    Rotation getMODPrecession(AbsoluteDate date);
}
