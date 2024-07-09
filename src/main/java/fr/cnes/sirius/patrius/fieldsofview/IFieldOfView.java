/**
 * 
 * Copyright 2011-2017 CNES
 *
 * HISTORY
* VERSION:4.7:DM:DM-2767:18/05/2021:Evolutions et corrections diverses 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
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
 * @history Creation 16/04/2012
 */
package fr.cnes.sirius.patrius.fieldsofview;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;

/**
 * This is the interface for all the field of view (circular, elliptic...). All of them
 * can compute the angular distance to a given direction (Vector3D).
 * 
 * @author Thomas Trapier
 * 
 * @version $Id$
 * 
 * @since 1.2
 * 
 */
public interface IFieldOfView extends Serializable {

    /**
     * Computes the angular distance between a vector and the border of the field.
     * The result is positive if the direction is in the field, negative otherwise.
     * For some of the fields (ComplexField), that value can be approximative : see
     * particular javadoc of each class.
     * 
     * @param direction
     *        the direction vector (expressed in the topocentric coordinate system of the object) 
     * @return the angular distance
     */
    double getAngularDistance(final Vector3D direction);

    /**
     * @param direction
     *        a direction vector (expressed in the topocentric coordinate system of the object)
     * @return true if the direction is in the field
     */
    boolean isInTheField(final Vector3D direction);

    /**
     * @return the name of the field
     */
    String getName();

}
