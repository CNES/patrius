/**
 * 
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
 * @history creation 25/04/2012
 * 
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:86:22/10/2013:New mass management system
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.properties;

import fr.cnes.sirius.patrius.assembly.PropertyType;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Matrix3D;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;

/**
 * This class is the abstract class for all inertia properties : those properties
 * can provide the inertia matrix and mass center of the part. All of them
 * shall extend it to assure they have the same "PropertyType"
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
public abstract class AbstractInertiaProperty implements IInertiaProperty {

    /** Serial UID. */
    private static final long serialVersionUID = 5570419284105270544L;

    /** the mass center */
    private Vector3D center;

    /** the inertia matrix */
    private Matrix3D matrix;

    /** the mass property of the part */
    private final MassProperty massProp;

    /**
     * Constructor for the abstract inertia property.
     * 
     * @param massCenter
     *        the mass center
     * @param inertiaMatrix
     *        the inertia matrix
     * @param mass
     *        the mass property associated to this part
     */
    protected AbstractInertiaProperty(final Vector3D massCenter, final Matrix3D inertiaMatrix,
        final MassProperty mass) {
        this.center = new Vector3D(1.0, massCenter);
        if (inertiaMatrix != null) {
            this.matrix = inertiaMatrix.multiply(1.0);
        }
        this.massProp = mass;
    }

    /**
     * Constructor for the abstract inertia property. The inertia matrix is expressed with respect to a point
     * that can be different from the mass center.
     * 
     * @param massCenter
     *        the mass center
     * @param inertiaMatrix
     *        the inertia matrix
     * @param inertiaReferencePoint
     *        the point with respect to the inertia matrix is expressed (in the part frame)
     * @param mass
     *        the mass property associated to this part
     */
    protected AbstractInertiaProperty(final Vector3D massCenter, final Matrix3D inertiaMatrix,
        final Vector3D inertiaReferencePoint, final MassProperty mass) {
        this.center = new Vector3D(1.0, massCenter);
        this.massProp = mass;
        final Matrix3D crossVectorMatrix = new Matrix3D(inertiaReferencePoint.subtract(massCenter));
        this.matrix =
            inertiaMatrix.add(crossVectorMatrix.multiply(crossVectorMatrix).multiply(this.massProp.getMass()))
                .multiply(1.0);

    }

    /** {@inheritDoc} */
    @Override
    public PropertyType getType() {
        return PropertyType.INERTIA;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getMassCenter() {
        return this.center;
    }

    /** {@inheritDoc} */
    @Override
    public double getMass() {
        return this.massProp.getMass();
    }

    /** {@inheritDoc} */
    @Override
    public Matrix3D getInertiaMatrix() {
        return this.matrix;
    }

    /**
     * Sets the mass center.
     * 
     * @param massCenter
     *        the new mass center
     */
    public void setMassCenter(final Vector3D massCenter) {
        this.center = new Vector3D(1.0, massCenter);
    }

    /**
     * Sets the inertia matrix
     * 
     * @param inertiaMatrix
     *        the new inetria matrix
     */
    public void setInertiaMatrix(final Matrix3D inertiaMatrix) {
        this.matrix = inertiaMatrix.multiply(1.0);
    }

    /** {@inheritDoc} */
    @Override
    public MassProperty getMassProperty() {
        return this.massProp;
    }
}
