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
 * @history Created 23/07/2013
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:85:23/07/2013:Created drag application point property
 * VERSION::FA:410:16/04/2015: Anomalies in the Patrius Javadoc
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.properties;

import fr.cnes.sirius.patrius.assembly.IPartProperty;
import fr.cnes.sirius.patrius.assembly.PropertyType;
import fr.cnes.sirius.patrius.assembly.models.AeroWrenchModel;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.wrenches.DragWrench;

/**
 * This class represents a drag application point property
 * 
 * @concurrency immutable
 * 
 * @see AeroWrenchModel
 * @see DragWrench
 * 
 * @author Rami Houdroge
 * @since 2.1
 * @version $Id $
 * 
 */
public class AeroApplicationPoint implements IPartProperty {

    /** Serial UID. */
    private static final long serialVersionUID = 5870045380256748644L;

    /** Application point of the drag forces */
    private final Vector3D appPoint;

    /**
     * Create a drag force application point property.
     * 
     * @param applicationPoint
     *        Application point of the drag forces
     */
    public AeroApplicationPoint(final Vector3D applicationPoint) {
        this.appPoint = applicationPoint;
    }

    /**
     * Get the application point in the given frame at the given date.
     * 
     * @return the application point of drag forces
     */
    public Vector3D getApplicationPoint() {
        return this.appPoint;
    }

    /** {@inheritDoc} */
    @Override
    public PropertyType getType() {
        return PropertyType.AERO_APPLICATION_POINT;
    }

}
