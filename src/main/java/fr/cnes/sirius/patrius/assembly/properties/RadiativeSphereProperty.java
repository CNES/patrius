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
 * @history creation 22/03/2012
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:358:09/03/2015:proper handling of vehicle negative surface
 * VERSION::DM:834:04/04/2017:create vehicle object
 * VERSION::DM:1489:23/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.properties;

import fr.cnes.sirius.patrius.assembly.PropertyType;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This class is a part property for the PATRIUS assembly. It allows the radiative model to use the part
 * with this property.
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
public final class RadiativeSphereProperty extends RadiativeCrossSectionProperty {

    /** Serializable UID. */
    private static final long serialVersionUID = 4838854700590811701L;

    /**
     * Sphere radius (m).
     */
    private final double sphereRadius;

    /**
     * Sphere area (m<sup>2</sup>).
     */
    private final double sphereArea;

    /**
     * Constructor with radius.
     * 
     * @param inSphereRadius
     *        the sphere radius (m)
     */
    public RadiativeSphereProperty(final double inSphereRadius) {
        super(null);
        this.sphereRadius = inSphereRadius;
        this.sphereArea = FastMath.PI * this.sphereRadius * this.sphereRadius;
    }

    /**
     * Constructor with area.
     * 
     * @param inSphereArea
     *        the sphere area (m<sup>2</sup>)
     */
    public RadiativeSphereProperty(final Parameter inSphereArea) {
        super(null);
        this.sphereArea = inSphereArea.getValue();

        // Radius is defined only if area is positive
        if (this.sphereArea >= 0) {
            this.sphereRadius = MathLib.sqrt(this.sphereArea / FastMath.PI);
        } else {
            this.sphereRadius = Double.NaN;
        }
    }

    /**
     * Get the sphere radius.
     * 
     * @return the sphere radius (m)
     * @throws PatriusException
     *         thrown if radius is undefined (negative area)
     */
    public double getSphereRadius() throws PatriusException {
        if (Double.isNaN(this.sphereRadius)) {
            throw new PatriusException(PatriusMessages.UNDEFINED_RADIUS);
        }
        return this.sphereRadius;
    }

    /**
     * Get the sphere area.
     * 
     * @return the sphere area (m<sup>2</sup>)
     */
    public double getSphereArea() {
        return this.sphereArea;
    }

    /** {@inheritDoc} */
    @Override
    public double getCrossSection(final SpacecraftState state, final Vector3D flux, final Frame partFrame)
        throws PatriusException {
        return this.sphereArea;
    }

    /** {@inheritDoc} */
    @Override
    public PropertyType getType() {
        return PropertyType.RADIATIVE_CROSS_SECTION;
    }
}
