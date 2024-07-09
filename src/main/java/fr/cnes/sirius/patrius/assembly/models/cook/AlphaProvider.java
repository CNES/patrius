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
 * @history creation 16/06/2016
 *
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:599:01/08/2016:add wall property
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.models.cook;

import java.io.Serializable;

import fr.cnes.sirius.patrius.propagation.SpacecraftState;

/**
 * Interface for alpha (energy accomodation coefficient).
 * 
 * @concurrency not thread-safe
 * 
 * @author Emmanuel Bignon
 * @since 3.3
 * @version $Id$
 */
public interface AlphaProvider extends Serializable {

    /**
     * Return alpha (energy accomodation coefficient) value.
     * 
     * @param state
     *        spacecraft state
     * @return alpha value
     */
    double getAlpha(final SpacecraftState state);
}
