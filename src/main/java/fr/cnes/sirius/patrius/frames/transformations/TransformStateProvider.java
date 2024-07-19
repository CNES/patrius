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
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.4:DM:DM-2148:04/10/2019:[PATRIUS] Creations de parties mobiles dans un Assembly
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.transformations;

import java.io.Serializable;

import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Interface for Transform providers.
 * <p>
 * The transform provider interface is mainly used to define the transform between a frame and its parent frame.
 * </p>
 * <p>This class extends the concept of {@link TransformProvider} by considering state-dependant transforms</p>
 * 
 * @author Emmanuel Bignon
 *
 * @since 4.4
 */
public interface TransformStateProvider extends Serializable {

    /**
     * Get the {@link Transform} corresponding to specified state.
     * 
     * @param state state
     * @return transform with specified state
     * @exception PatriusException thrown if transform cannot be computed
     */
    Transform getTransform(final SpacecraftState state) throws PatriusException;
}
