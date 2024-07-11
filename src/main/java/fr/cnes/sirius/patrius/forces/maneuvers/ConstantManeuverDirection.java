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
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:976:16/11/2017:Merge continuous maneuvers classes
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.maneuvers;

import fr.cnes.sirius.patrius.math.analysis.IDependentVectorVariable;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;

/**
 * Constant direction (class used for maneuvers building only).
 * 
 * @author Emmanuel Bignon
 * 
 * @version $Id: ConstantDirection.java 18195 2017-10-13 11:49:12Z bignon $
 * 
 * @since 4.0
 */
class ConstantManeuverDirection implements IDependentVectorVariable<SpacecraftState> {

    /** Serial UID. */
    private static final long serialVersionUID = -6543263721048900293L;

    /** The thrust direction. */
    private final Vector3D direction;

    /**
     * Constructor.
     * 
     * @param constantDirection
     *        the constant direction;
     */
    public ConstantManeuverDirection(final Vector3D constantDirection) {
        this.direction = constantDirection;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D value(final SpacecraftState s) {
        return this.direction;
    }
}
