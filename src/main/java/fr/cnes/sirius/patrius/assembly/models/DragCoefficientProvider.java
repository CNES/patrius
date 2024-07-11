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
 * @history creation 13/09/2016
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:599:13/09/2016: new tabulated aero model
 * VERSION::FA:705:07/12/2016: corrected anomaly in dragAcceleration()
 * VERSION::DM:711:07/12/2016: change signature of method getCoefficients()
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.models;

import java.io.Serializable;

import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.forces.atmospheres.AtmosphereData;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;

/**
 * Drag coefficient (x surface) provider.
 * 
 * @concurrency not thread-safe
 * 
 * @author Emmanuel Bignon
 * 
 * @version $Id$
 * 
 * @since 3.3
 */
public interface DragCoefficientProvider extends Serializable {

    /**
     * Provides drag coefficient (x surface).
     * 
     * @param relativeVelocity
     *        relative velocity of atmosphere with respect to spacecraft in satellite frame
     * @param atmoData
     *        atmosphere data
     * @param assembly
     *        assembly
     * @return drag coefficient (x surface) in satellite frame
     */
    DragCoefficient getCoefficients(final Vector3D relativeVelocity, final AtmosphereData atmoData,
                                    final Assembly assembly);
}
