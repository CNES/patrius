/**
 * 
 * Copyright 2011-2017 CNES
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
 * @history Created 22/07/2013
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:85:22/07/2013:Created radiative application point property
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.properties;

import fr.cnes.sirius.patrius.assembly.IPartProperty;
import fr.cnes.sirius.patrius.assembly.PropertyType;
import fr.cnes.sirius.patrius.assembly.models.DirectRadiativeWrenchModel;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.wrenches.SolarRadiationWrench;

/**
 * This class represents a radiative application point property
 * 
 * @concurrency immutable
 * 
 * @see DirectRadiativeWrenchModel
 * @see SolarRadiationWrench
 * 
 * @author Rami Houdroge
 * @since 2.1
 * @version $Id $
 * 
 */
public class RadiativeApplicationPoint implements IPartProperty {

    /** Serial UID. */
    private static final long serialVersionUID = -5700674669145711181L;

    /** Application point of the radiative forces */
    private final Vector3D appPoint;

    /**
     * Create a radiative force application point property.
     * 
     * @param applicationPoint
     *        Application point of the radiative forces in the part frame
     */
    public RadiativeApplicationPoint(final Vector3D applicationPoint) {
        this.appPoint = applicationPoint;
    }

    /**
     * Get the application point in the part frame
     * 
     * @return the application point of radiative forces
     */
    public Vector3D getApplicationPoint() {
        return this.appPoint;
    }

    /** {@inheritDoc} */
    @Override
    public PropertyType getType() {
        return PropertyType.RADIATION_APPLICATION_POINT;
    }

}
