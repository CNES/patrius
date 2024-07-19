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
 * @history creation 8/03/2012
 *
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:373:12/01/2015: proper handling of mass event detection
 * VERSION::DM:834:04/04/2017:create vehicle object
 * VERSION::FA:1177:06/09/2017:add Cook model validation test
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.properties.features;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.CrossSectionProvider;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * This class is a cross section provider.
 * <p>
 * Note that the use of this class implies a constant area which may not be suited for some application such as reentry.
 * </p>
 * 
 * @concurrency immutable
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
public final class Facet implements CrossSectionProvider, Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = -2370962318770584068L;

    /** Unit Normal vector */
    private final Vector3D normal;

    /** Area */
    private final double area;

    /**
     * Simple constructor.
     * 
     * @param inNormal
     *        vector normal to the facet. Facet should be oriented such that facet normal vector is outside
     *        the spacecraft.
     * @param inArea
     *        facet area
     */
    public Facet(final Vector3D inNormal, final double inArea) {
        this.normal = inNormal.normalize();
        this.area = inArea;
    }

    /**
     * @return unit normal vector
     */
    public Vector3D getNormal() {
        return new Vector3D(1.0, this.normal);
    }

    /**
     * @return facet area
     */
    public double getArea() {
        return this.area;
    }

    /** {@inheritDoc} */
    @Override
    public double getCrossSection(final Vector3D direction) {
        // the cross section is not null only if the dot product of the normal vector and the
        // input direction is positive
        final double absCosAngle = MathLib.max(0.0, -Vector3D.dotProduct(direction.normalize(), this.normal));
        return absCosAngle * this.area;
    }
}
