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
 * @history creation 18/04/2012
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.fieldsofview;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;

/**
 * This field of view contains an existing field and inverts it.
 * 
 * @concurrency conditionally thread-safe
 * 
 * @concurrency.comment the used field must be thread-safe itself.
 *                      All available fields are immutable.
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
public final class InvertField implements IFieldOfView {

    /** Serial UID. */
    private static final long serialVersionUID = -4732002247010892567L;

    /** the name of this field */
    private final String inName;

    /** first field */
    private final IFieldOfView filed1;

    /**
     * Constructor of the invert field of a given one.
     * 
     * @param name
     *        the name of the new field
     * @param invertedField
     *        the field to be inverted
     */
    public InvertField(final String name, final IFieldOfView invertedField) {
        this.filed1 = invertedField;
        this.inName = name;
    }

    /**
     * Computes the angular distance between a vector and the border of the field.
     * The result is positive if the direction is in the field, negative otherwise.
     * This "invert" field simply invert the value given by the origin field.
     * 
     * @param direction
     *        the direction vector (expressed in the tropocentric coordinate system of the object)
     * @return the angular distance
     */
    @Override
    public double getAngularDistance(final Vector3D direction) {
        return -this.filed1.getAngularDistance(direction);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isInTheField(final Vector3D direction) {
        return !this.filed1.isInTheField(direction);
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return this.inName;
    }

}
