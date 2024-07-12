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
 * @history creation 25/04/2012
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:86:22/10/2013:New mass management system
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.properties;

import fr.cnes.sirius.patrius.assembly.IPartProperty;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Matrix3D;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;

/**
 * This is the interface for all inertia properties : those properties
 * can provide the inertia matrix, mass and mass center of the part, and the frame
 * in which the vector and the matrix are expressed.
 * The inertia matrix is the one at the mass center of the part.
 * 
 * @see IPartProperty
 * 
 * @author Thomas Trapier
 * 
 * @version $Id$
 * 
 * @since 1.2
 * 
 */
public interface IInertiaProperty extends IPartProperty {

    /**
     * @return the position of the mass center in the reference frame
     */
    Vector3D getMassCenter();

    /**
     * @return the inertia matrix at the mass center of the part.
     */
    Matrix3D getInertiaMatrix();

    /**
     * @return the mass of the considered part.
     */
    double getMass();

    /**
     * @return the mass property
     */
    MassProperty getMassProperty();
}
