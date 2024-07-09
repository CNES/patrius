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
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:611:02/08/2016:Creation of the class
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.events;

import java.io.Serializable;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;

/**
 * Interface to represent radius providers.
 */
public interface LocalRadiusProvider extends Serializable {

    /**
     * Calculate the apparent radius.
     * 
     * @param position
     *        spacecraft position
     * @param frame
     *        frame in which position is expressed
     * @param date
     *        date of position
     * @param occultedBodyIn
     *        body occulted by this
     * @return apparent radius
     */
    double getLocalRadius(final Vector3D position, final Frame frame, final AbsoluteDate date,
                          final PVCoordinatesProvider occultedBodyIn);

}
