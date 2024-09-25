/**
 * 
 * Copyright 2011-2022 CNES
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
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.properties;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Matrix3D;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;

/**
 * This class is a simple inertia property that can be added to a part.
 * The mass center and inertia matrix are simply given by the user in
 * the constructor. They also can be set later.
 * 
 * @concurrency not thread-safe
 * 
 * @concurrency.comment the use of frames makes this class not thread-safe
 * 
 * @see IInertiaProperty
 * 
 * @author Thomas Trapier
 * 
 * @version $Id$
 * 
 * @since 1.2
 * 
 */
public class InertiaSimpleProperty extends AbstractInertiaProperty {

    /** Serializable UID. */
    private static final long serialVersionUID = -7547375010586060849L;

    /**
     * Constructor for the simple inertia property
     * 
     * @param massCenter
     *        the mass center
     * @param inertiaMatrix
     *        the inertia matrix expressed at the mass center of the part.
     * @param mass
     *        the mass property associated to this part
     */
    public InertiaSimpleProperty(final Vector3D massCenter, final Matrix3D inertiaMatrix,
        final MassProperty mass) {
        super(massCenter, inertiaMatrix, mass);
    }

    /**
     * Constructor for the simple inertia property; the inertia matrix is expressed with respect to a point
     * that can be different from the mass center.
     * 
     * @param massCenter
     *        the mass center
     * @param inertiaMatrix
     *        the inertia matrix of the part.
     * @param inertiaReferencePoint
     *        the point with respect to the inertia matrix is expressed (in the part frame)
     * @param mass
     *        the mass property associated to this part
     */
    public InertiaSimpleProperty(final Vector3D massCenter, final Matrix3D inertiaMatrix,
        final Vector3D inertiaReferencePoint, final MassProperty mass) {
        super(massCenter, inertiaMatrix, inertiaReferencePoint, mass);
    }
}
