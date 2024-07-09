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
 * @history creation 17/04/2012
 *
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.fieldsofview;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * This class describes a boolean field of view that combines two existing fields
 * with a "AND" or "OR" boolean combination.
 * 
 * @concurrency conditionally thread-safe
 * 
 * @concurrency.comment the used fields must be thread-safe themselves.
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
public final class BooleanField implements IFieldOfView {

    /** Serial UID. */
    private static final long serialVersionUID = 771065829374619466L;

    /** the name of this field */
    private final String inName;

    /** first field */
    private final IFieldOfView filed1;

    /** second field */
    private final IFieldOfView filed2;

    /** the combination to be applied */
    private final BooleanCombination combination;

    /**
     * The boolean combination to be applied
     */
    public enum BooleanCombination {

        /**
         * AND
         */
        AND,

        /**
         * OR
         */
        OR;
    }

    /**
     * Constructor for the "AND" or "OR" combined field of view
     * 
     * @param name
     *        the name of this field
     * @param firstField
     *        the first field to be combined
     * @param secondField
     *        the second field to be combined
     * @param booleanCombination
     *        combination to be applied : "AND" or "OR"
     */
    public BooleanField(final String name, final IFieldOfView firstField,
        final IFieldOfView secondField, final BooleanCombination booleanCombination) {

        // initialisations
        this.filed1 = firstField;
        this.filed2 = secondField;
        this.inName = name;
        this.combination = booleanCombination;
    }

    /**
     * Computes the angular distance between a vector and the border of the field.
     * The result is positive if the direction is in the field, negative otherwise.
     * In some cases, this value can be approximative : in the "OR" case, inside of the field,
     * and in the "AND" case ouside of it.
     * 
     * @param direction
     *        the direction vector (expressed in the tropocentric coordinate system of the object)
     * @return the angular distance
     */
    @Override
    public double getAngularDistance(final Vector3D direction) {
        if (this.combination == BooleanCombination.AND) {
            // AND combination : min of the values
            return MathLib.min(this.filed1.getAngularDistance(direction), this.filed2.getAngularDistance(direction));
        } else {
            // OR combination : max of the values
            return MathLib.max(this.filed1.getAngularDistance(direction), this.filed2.getAngularDistance(direction));
        }

    }

    /** {@inheritDoc} */
    @Override
    public boolean isInTheField(final Vector3D direction) {
        if (this.combination == BooleanCombination.AND) {
            // AND combination
            return this.filed1.isInTheField(direction) && this.filed2.isInTheField(direction);
        } else {
            // OR combination
            return this.filed1.isInTheField(direction) || this.filed2.isInTheField(direction);
        }

    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return this.inName;
    }

}
