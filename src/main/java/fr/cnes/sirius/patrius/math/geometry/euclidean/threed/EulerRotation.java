/**
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
 * @history creation 30/05/2018
 *
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1489:30/05/2018:add GENOPUS Custom classes
 * VERSION::FA:1970:03/01/2019:quality corrections
 * END-HISTORY
 **/

package fr.cnes.sirius.patrius.math.geometry.euclidean.threed;

/**
 * Rotation focusing on Euler angles. This is a specific class extending {@link Rotation}. It does
 * not contain algorithmic code but allow to store an Euler rotation order.
 *
 * @author Emmanuel Bignon
 *
 * @version $Id$
 *
 * @since 4.1
 */
public class EulerRotation extends Rotation {

    /** Serial UID. */
    private static final long serialVersionUID = -4863402941914521143L;

    /** Rotation order. */
    private final RotationOrder mOrder;

    /** 1st component of rotation. */
    private final double mAlpha1;

    /** 2nd component of rotation. */
    private final double mAlpha2;

    /** 3rd component of rotation. */
    private final double mAlpha3;

    /**
     * Constructor.
     * 
     * @see Rotation#Rotation(RotationOrder, double, double, double)
     * @param order order of rotations to use
     * @param alpha1 angle of the first elementary rotation
     * @param alpha2 angle of the second elementary rotation
     * @param alpha3 angle of the third elementary rotation
     */
    public EulerRotation(final RotationOrder order, final double alpha1, final double alpha2, final double alpha3) {
        super(order, alpha1, alpha2, alpha3);
        this.mOrder = order;
        this.mAlpha1 = alpha1;
        this.mAlpha2 = alpha2;
        this.mAlpha3 = alpha3;
    }

    /** {@inheritDoc} */
    @Override
    public double[] getAngles(final RotationOrder order) {
        final double[] angles;
        if (this.mOrder.equals(order)) {
            angles = new double[] { this.mAlpha1, this.mAlpha2, this.mAlpha3 };
        } else {
            angles = super.getAngles(order);
        }
        return angles;
    }

    /**
     * Get the Cardan or Euler angles corresponding to the instance in the initial rotation order.
     * 
     * @see Rotation#getAngles(RotationOrder)
     * @return an array of three angles, in the order specified by the set
     */
    public double[] getAngles() {
        return this.getAngles(this.mOrder);
    }

    /**
     * Getter for the order of rotations to use for (alpha1, alpha2, alpha3) composition.
     * 
     * @return the order of rotations to use for (alpha1, alpha2, alpha3) composition
     */
    public RotationOrder getRotationOrder() {
        return this.mOrder;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return String.format("EulerRotation: order=%s, alpha1=%s, alpha2=%s, alpha3=%s",
            this.mOrder.toString(), this.mAlpha1, this.mAlpha2, this.mAlpha3);
    }
}