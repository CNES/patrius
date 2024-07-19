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
 * @history creation 16/06/2016
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:600:16/06/2016:add Cook (Cn, Ct) models
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.models.cook;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;

/**
 * Wall gas temperature provider.
 * 
 * @concurrency not thread-safe
 * 
 * @author Emmanuel Bignon
 * @since 3.3
 * @version $Id$
 */
public interface WallGasTemperatureProvider extends Serializable {

    /**
     * Compute wall gas temperature.
     * 
     * @param state
     *        spacecraft state
     * @param relativeVelocity
     *        relative velocity with respect to gas
     * @param theta
     *        angle between facet and relative velocity (atmosphere / satellite)
     * @return atmosphericTemperature atmospheric temperature
     */
    double getWallGasTemperature(final SpacecraftState state, final Vector3D relativeVelocity, final double theta);
}
