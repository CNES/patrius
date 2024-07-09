/**
 * 
 * Copyright 2011-2017 CNES
 *
 * HISTORY
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
 * @history creation 22/03/2012
 */
package fr.cnes.sirius.patrius.assembly.properties;

import fr.cnes.sirius.patrius.assembly.IPartProperty;
import fr.cnes.sirius.patrius.assembly.PropertyType;
import fr.cnes.sirius.patrius.assembly.properties.features.Facet;

/**
 * This class is a part property for the PATRIUS assembly. It allows the radiative model to use a part with
 * this property.
 * 
 * @concurrency immutable
 * 
 * @author Gerald Mercadier
 * 
 * @version $Id$
 * 
 * @since 1.1
 * 
 */
public final class RadiativeFacetProperty implements IPartProperty {

    /** Serial UID. */
    private static final long serialVersionUID = -3262766288304093255L;

    /**
     * Facet.
     */
    private final Facet facet;

    /**
     * Constructor of this property.
     * 
     * @param inFacet
     *        The facet.
     */
    public RadiativeFacetProperty(final Facet inFacet) {
        this.facet = inFacet;
    }

    /**
     * Get the facet.
     * 
     * @return the facet
     */
    public Facet getFacet() {
        return this.facet;
    }

    /** {@inheritDoc} */
    @Override
    public PropertyType getType() {
        return PropertyType.RADIATIVE_FACET;
    }
}
