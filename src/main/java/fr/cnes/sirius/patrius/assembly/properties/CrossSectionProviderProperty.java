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
 * @history creation 8/03/2012
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.properties;

import fr.cnes.sirius.patrius.assembly.IPartProperty;
import fr.cnes.sirius.patrius.assembly.PropertyType;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.CrossSectionProvider;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;

/**
 * This class is a part property for the PATRIUS assembly. It is the geometric
 * cross section provider of a part.
 * 
 * @concurrency conditionally thread-safe
 * 
 * @concurrency.comment the CrossSectionProvider object must be immutable. In this case, this
 *                      class is immutable itself and thus, is not subject to thread safety tests.
 * 
 * @see CrossSectionProvider
 * 
 * @author Thomas Trapier
 * 
 * @version $Id$
 * 
 * @since 1.1
 * 
 */
public final class CrossSectionProviderProperty implements IPartProperty {

    /** Serial UID. */
    private static final long serialVersionUID = -7350462642089412716L;

    /**
     * The cross section providing geometry of the part.
     */
    private final CrossSectionProvider geometryProv;

    /**
     * Constructor of this property.
     * 
     * @param geometry
     *        The cross section providing geometry of the part.
     */
    public CrossSectionProviderProperty(final CrossSectionProvider geometry) {
        this.geometryProv = geometry;
    }

    /**
     * Computes the cross section of the geometry from a direction
     * defined by a Vector3D.
     * 
     * @param direction
     *        the direction vector
     * @return the cross section
     */
    public double getCrossSection(final Vector3D direction) {
        return this.geometryProv.getCrossSection(direction);
    }

    /** {@inheritDoc} */
    @Override
    public PropertyType getType() {
        return PropertyType.CROSS_SECTION;
    }
}
