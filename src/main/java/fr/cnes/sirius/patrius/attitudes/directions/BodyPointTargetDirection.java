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
 * HISTORY
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.11:DM:DM-3268:22/05/2023:[PATRIUS] Creation d'une classe GeodeticTargetDirection
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes.directions;

import fr.cnes.sirius.patrius.bodies.BodyPoint;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.ConstantPVCoordinatesProvider;

/**
 * This class extends the {@link GenericTargetDirection} to create a direction with a target which is a
 * {@link BodyPoint}.
 * 
 * @author Pierre Préault
 */
public class BodyPointTargetDirection extends GenericTargetDirection {

    /** Serializable UID. */
    private static final long serialVersionUID = 8950906884858285070L;

    /**
     * Simple constructor
     * 
     * @param target
     *        the target point
     */
    public BodyPointTargetDirection(final BodyPoint target) {
        // Initialization with super constructor
        super(new ConstantPVCoordinatesProvider(target.getPosition(), target.getBodyShape().getBodyFrame()));
    }
}
