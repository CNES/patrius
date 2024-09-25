/**
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
 * VERSION:4.11:DM:DM-3259:22/05/2023:[PATRIUS] Creer une interface StarConvexBodyShape
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies;


/**
 * <p>
 * Extended interface for star-convex bodies shapes : extends the {@link BodyShape} interface by adding a method to get
 * a {@link GeodeticPoint} of the shape from given latitude,longitude and altitude.
 * </p>
 *
 * @see BodyShape
 *
 * @since 4.11
 */
public interface StarConvexBodyShape extends BodyShape {
    // Empty class for now
}
