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
 * @history Created on 05/03/2012
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.properties;

import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.IPartProperty;
import fr.cnes.sirius.patrius.assembly.PropertyType;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.SolidShape;

/**
 * This class defines the Geometric Property to be used with {@link Assembly assemblies}.
 * 
 * @useSample
 * 
 *            <code>final SolidShape cone = new ObliqueCircularCone(Vector3D.ZERO, translation, 
 *            translation.orthogonal(),
 * MathUtils.DEG_TO_RAD * 30, MathUtils.DEG_TO_RAD * 20, 1.5);<br>
 * final IPartProperty shapeProp2 = new GeometricProperty(cone);<br>
 * builder.addProperty(shapeProp2, part2);</code>
 * 
 * @concurrency conditionally thread-safe
 * 
 * @concurrency.comment the SolidShape object must be immutable. In this case, this
 *                      class is immutable itself and thus, is not subject to thread safety tests.
 * 
 * @author houdroger
 * 
 * @version $Id$
 * 
 * @since 1.1
 * 
 */
public final class GeometricProperty implements IPartProperty {

    /** Serial UID. */
    private static final long serialVersionUID = 105245032294840352L;

    /** Geometric shape */
    private final SolidShape shape;

    /**
     * Constructor
     * 
     * @param solidShape
     *        shape of assembly part
     */
    public GeometricProperty(final SolidShape solidShape) {
        this.shape = solidShape;
    }

    /**
     * @return the shape
     */
    public SolidShape getShape() {
        return this.shape;
    }

    /** {@inheritDoc} */
    @Override
    public PropertyType getType() {
        return PropertyType.GEOMETRY;
    }

}
