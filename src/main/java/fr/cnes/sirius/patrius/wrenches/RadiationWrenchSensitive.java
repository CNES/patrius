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
 * @history Created on 18/07/2013
 *
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:85:18/07/2013:Created the RadiationWrenchSensitive interface
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.wrenches;

import java.io.Serializable;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Interface to represent solar radiation wrench sensitive vehicles
 * 
 * @author Rami Houdroge
 * @version $Id $
 * @since 2.1
 */
public interface RadiationWrenchSensitive extends Serializable {

    /**
     * Compute the torque due to radiation pressire.
     * <p>
     * The computation includes all spacecraft specific characteristics like shape, area and coefficients.
     * </p>
     * 
     * @param state
     *        current state information: date, kinematics, attitude
     * @param flux
     *        radiation flux in the same inertial frame as spacecraft orbit
     * @return spacecraft torque expressed at the mass center in the frame of the
     *         main part of the assembly (accounts for attitude)
     * @throws PatriusException
     *         if torque cannot be computed
     */
    Wrench radiationWrench(final SpacecraftState state, final Vector3D flux) throws PatriusException;

    /**
     * Compute the torque due to radiation pressire.
     * <p>
     * The computation includes all spacecraft specific characteristics like shape, area and coefficients.
     * </p>
     * 
     * @param state
     *        current state information: date, kinematics, attitude
     * @param flux
     *        radiation flux in the same inertial frame as spacecraft orbit
     * @param origin
     *        point in which to express torque
     * @param frame
     *        the reference frame
     * @return spacecraft torque expressed at origin in frame
     * @throws PatriusException
     *         if torque cannot be computed
     */
    Wrench radiationWrench(final SpacecraftState state, final Vector3D flux, final Vector3D origin,
                           final Frame frame) throws PatriusException;
}
