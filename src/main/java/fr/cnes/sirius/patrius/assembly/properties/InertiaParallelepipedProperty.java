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
 * Inertia property for a parallelepipedic part. The center of the parallelepiped is the (0, 0, 0) point
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
public final class InertiaParallelepipedProperty extends AbstractInertiaProperty {

    /** Serial UID. */
    private static final long serialVersionUID = -4109361009795718787L;

    /** the dimension on the X axis of the frame */
    private final double inLength;

    /** the dimension on the Y axis of the frame */
    private final double inWidth;

    /** the dimension on the Z axis of the frame */
    private final double inHeight;

    /**
     * Constructor for the parallelepiped inertia property. The center of the parallelepiped
     * is the (0, 0, 0) point in the given frame.
     * 
     * @param length
     *        length the dimension on the X axis of the frame.
     * @param width
     *        the dimension on the Y axis of the given frame.
     * @param height
     *        the dimension on the Z axis of the given frame.
     * @param mass
     *        the mass property associated to this part
     */
    public InertiaParallelepipedProperty(final double length, final double width, final double height,
        final MassProperty mass) {
        super(Vector3D.ZERO, null, mass);
        // test of the length
        if (length < 0.0 || Precision.equals(length, 0.0)) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.PDB_VALUE_SHOULD_BE_POSITIVE);
        }
        // test of the width
        if (width < 0.0 || Precision.equals(width, 0.0)) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.PDB_VALUE_SHOULD_BE_POSITIVE);
        }
        // test of the height
        if (height < 0.0 || Precision.equals(height, 0.0)) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.PDB_VALUE_SHOULD_BE_POSITIVE);
        }
        this.inLength = length;
        this.inWidth = width;
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
        final double twelve = 12.0;
        final double[][] dataIn =
        {
            { 1.0 / twelve * this.getMass() * (this.inWidth * this.inWidth + this.inHeight * this.inHeight), 0.0,
                0.0 },
            { 0.0, 1.0 / twelve * this.getMass() * (this.inLength * this.inLength + this.inHeight * this.inHeight),
                0.0 },
            { 0.0, 0.0,
                1.0 / twelve * this.getMass() * (this.inLength * this.inLength + this.inWidth * this.inWidth) } };
        this.setInertiaMatrix(new Matrix3D(dataIn));
    }
}
