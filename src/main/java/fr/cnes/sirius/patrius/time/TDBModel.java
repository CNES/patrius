/**
 * Copyright 2021-2021 CNES
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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.7:DM:DM-2682:18/05/2021: Echelle de temps TDB (diff. PATRIUS - SPICE) 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.time;


/**
 * Barycentric Dynamic Time model.
 * <p>
 * TDB is time used to take account of time dilation when calculating orbits of planets, asteroids, comets and
 * interplanetary spacecraft in the Solar system. It was based on a Dynamical time scale but was not well defined and
 * not rigorously correct as a relativistic time scale. It was subsequently deprecated in favour of Barycentric
 * Coordinate Time (TCB), but at the 2006 General Assembly of the International Astronomical Union TDB was rehabilitated
 * by making it a specific fixed linear transformation of TCB.
 * </p>
 * 
 * @author Emmanuel Bignon
 * 
 * @since 4.7
 */
public interface TDBModel {

    /**
     * Get the offset to convert locations from {@link TAIScale} to {@link TDBScale}.
     * 
     * @param date
     *        conversion date
     * @return offset in seconds to add to a location in <em>{@link TAIScale}
     * time scale</em> to get a location in <em>instance time scale</em>
     */
    double offsetFromTAI(final AbsoluteDate date);
}
