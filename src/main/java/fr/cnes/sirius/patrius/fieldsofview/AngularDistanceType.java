/**
 *
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
 *
 * @history creation 22/08/2024
 *
 * HISTORY
 * VERSION:4.14:OPENFD-173:22/08/2024: Ajout d'une nouvelle interface IGeometricaFieldOfView
 * VERSION:4.14:OPENFD-311:22/08/2024: [PATRIUS] getInputCoord sur EllipsoidPoint
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.fieldsofview;

/**
 * This enum defines different methods to compute the angular distance between a given direction in space and the border
 * of the FOV.
 */
public enum AngularDistanceType {
    /** Minimal distance between the input direction and all the borders of the FOV */
    MINIMAL,

    /**
     * Directional distance. It defined as the angle between the current direction d and the FOV border intersection
     * with the half-plane containing both d and the FOV's main direction.
     */
    DIRECTIONAL;
}
