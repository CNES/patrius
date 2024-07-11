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
 * @history creation 16/04/2012
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.fieldsofview;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;

/**
 * This class describes an omnidirectional field of view : any vector is in it,
 * the angular distance is always 1 (positive).
 * 
 * @concurrency immutable
 * 
 * @see IFieldOfView
 * 
 * @author Thomas Trapier
 * 
 * @version $Id$
 * 
 * @since 1.2
 * 
 */
public final class OmnidirectionalField implements IFieldOfView {

    /** Serial UID. */
    private static final long serialVersionUID = -2399659632905964457L;
    /** the name of this field */
    private final String inName;

    /**
     * Constructor of an omnidirectional field of view.
     * 
     * @param name
     *        the name of this field
     */
    public OmnidirectionalField(final String name) {
        this.inName = name;
    }

    /**
     * this method has no sense in the case of an omnidirectional field. The
     * convention for all other fields to return a positive value if then vector
     * is in the field : this method always return 1, all vectors being in it.
     * 
     * @param direction
     *        the direction vector (unused)
     * @return always 1.0
     */
    @Override
    public double getAngularDistance(final Vector3D direction) {
        return 1.0;
    }

    /**
     * Any vector being in the field, this method always return true
     * 
     * @param direction
     *        the direction vector (unused)
     * @return always true
     */
    @Override
    public boolean isInTheField(final Vector3D direction) {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return this.inName;
    }

}
