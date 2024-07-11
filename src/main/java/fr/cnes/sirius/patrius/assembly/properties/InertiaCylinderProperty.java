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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
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
 * Inertia property for a cylinder part. The (0, 0, 0) point of the given frame
 * is the center of a basis of the cylinder. Its axis is Z, and it is oriented
 * on the positive values.
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
public final class InertiaCylinderProperty extends AbstractInertiaProperty {

    /** Serializable UID. */
    private static final long serialVersionUID = -2793876565171400403L;

    /** the radius of the sphere */
    private final double inRadius;

    /** the height : dimension on the Z axis of the frame */
    private final double inHeight;

    /**
     * Constructor for the cylinder inertia property.
     * 
     * @param radius
     *        the radius of the basis (must be positive)
     * @param height
     *        the heihgt of the cylinder (must be positive)
     * @param mass
     *        the mass property associated to this part
     */
    public InertiaCylinderProperty(final double radius, final double height,
        final MassProperty mass) {
        super(new Vector3D(0.0, 0.0, height / 2.0), null, mass);

        // test of the radius
        if (radius < 0.0 || Precision.equals(radius, 0.0)) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.PDB_VALUE_SHOULD_BE_POSITIVE);
        }
        // test of the height
        if (height < 0.0 || Precision.equals(height, 0.0)) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.PDB_VALUE_SHOULD_BE_POSITIVE);
        }

        this.inRadius = radius;
        this.inHeight = height;
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
        final double[][] dataIn =
        {
            {
                1.0 / 4.0 * this.getMass() * this.inRadius * this.inRadius + 1.0 / 3.0 * this.getMass()
                    * this.inHeight * this.inHeight, 0.0, 0.0 },
            {
                0.0,
                1.0 / 4.0 * this.getMass() * this.inRadius * this.inRadius + 1.0 / 3.0 * this.getMass()
                    * this.inHeight * this.inHeight, 0.0 },
            { 0.0, 0.0, 1.0 / 2.0 * this.getMass() * this.inRadius * this.inRadius } };
        this.setInertiaMatrix(new Matrix3D(dataIn));
    }
}
