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
 * @history creation 27/04/2012
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.properties;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Matrix3D;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Inertia property for a spherical part. The center of the part is the (0, 0, 0) point
 * in the given frame.
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
public final class InertiaSphereProperty extends AbstractInertiaProperty {

    /** Serial UID. */
    private static final long serialVersionUID = -6251515717619406375L;

    /** the radius of the sphere */
    private final double inRadius;

    /**
     * Constructor for the spherical inertia property.
     * 
     * @param radius
     *        the radius of the sphere
     * @param mass
     *        the mass property associated to this part
     */
    public InertiaSphereProperty(final double radius, final MassProperty mass) {
        super(Vector3D.ZERO, null, mass);

        // test of the radius
        if (radius < 0.0 || Precision.equals(radius, 0.0)) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.PDB_VALUE_SHOULD_BE_POSITIVE);
        }

        this.inRadius = radius;
    }

    /** {@inheritDoc} */
    @Override
    public Matrix3D getInertiaMatrix() {
        this.updateMatrixFromMass();
        return super.getInertiaMatrix();
    }

    /**
     * updates the inertia matrix entries from the mass.
     */
    private void updateMatrixFromMass() {
        final double value = 2.0 / 5.0 * this.getMass() * this.inRadius * this.inRadius;
        final double[][] dataIn = { { value, 0.0, 0.0 }, { 0.0, value, 0.0 }, { 0.0, 0.0, value } };
        this.setInertiaMatrix(new Matrix3D(dataIn));
    }
}
