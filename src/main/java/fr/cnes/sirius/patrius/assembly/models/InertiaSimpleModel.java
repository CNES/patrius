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
 * @history creation 26/04/2012
 *
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:86:22/10/2013:New mass management system
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:200:28/08/2014: dealing with a negative mass in the propagator
 * VERSION::FA:373:12/01/2015: proper handling of mass event detection
 * VERSION::FA:410:16/04/2015: Anomalies in the Patrius Javadoc
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::FA:673:12/09/2016: add getTotalMass(state)
 * VERSION::FA:706:06/01/2017: synchronisation problem with the Assemby mass
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.models;

import java.util.ArrayList;
import java.util.List;

import fr.cnes.sirius.patrius.assembly.properties.MassEquation;
import fr.cnes.sirius.patrius.assembly.properties.MassProperty;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Matrix3D;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.numerical.AdditionalEquations;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusExceptionWrapper;

/**
 * Simple inertia model : the mass, mass center and inertia matrix are directly
 * given by the user.
 * <p>
 * Note : when using this model within a propagation, it is necessary to feed the additional equations to the
 * propagator. This has to be done prior to any propagation, to allow this model to account mass variations (i.e. due to
 * maneuvers), using the method NumericalPropagator.setMassProviderEquation() which will register the additional
 * equation and initialize the initial additional state.
 * </p>
 * 
 * @concurrency not thread-safe
 * 
 * @concurrency.comment the frame and setters makes this class not thread-safe
 * 
 * @see IInertiaModel
 * 
 * @author Thomas Trapier
 * 
 * @version $Id$
 * 
 * @since 1.2
 * 
 */
public final class InertiaSimpleModel implements IInertiaModel {

    /** Serial UID. */
    private static final long serialVersionUID = 987674997665201049L;

    /** The global mass. */
    private final MassProperty inMass;

    /** The mass center (in the reference frame). */
    private Vector3D center;

    /** The inertia matrix (in the reference frame) with respect to the mass center. */
    private Matrix3D matrix;

    /** The reference frame for the mass center and the inertia matrix. */
    private final Frame refFrame;

    /** Mass equation */
    private final MassEquation eq;

    /** Part name */
    private final String name;

    /**
     * Constructor for a simple inertia model.
     * 
     * @param mass
     *        the global mass
     * @param massCenter
     *        the mass center
     * @param inertiaMatrix
     *        the inertia matrix, expressed with respect to the mass center
     * @param frame
     *        the expression frame of the mass center vector and inertia matrix
     * @param partName
     *        name of the part
     * @throws PatriusException
     *         if the mass is negative (PatriusMessages.MASS_ARGUMENT_IS_NEGATIVE)
     */
    public InertiaSimpleModel(final double mass, final Vector3D massCenter,
        final Matrix3D inertiaMatrix, final Frame frame, final String partName) throws PatriusException {
        this.inMass = new MassProperty(mass);
        this.eq = new MassEquation(partName);
        this.center = new Vector3D(1.0, massCenter);
        this.matrix = inertiaMatrix.multiply(1.0);
        this.refFrame = frame;
        this.name = partName;

    }

    /**
     * Constructor for a simple inertia model; the inertia matrix is expressed with respect to a point
     * that can be different from the mass center.
     * 
     * @param mass
     *        the global mass
     * @param massCenter
     *        the mass center
     * @param inertiaMatrix
     *        the inertia matrix
     * @param inertiaReferencePoint
     *        the point with respect to the inertia matrix is expressed (in the reference frame)
     * @param frame
     *        the expression frame of the mass center vector and inertia matrix
     * @param partName
     *        name of the part
     * @throws PatriusException
     *         if the mass is negative (PatriusMessages.MASS_ARGUMENT_IS_NEGATIVE)
     */
    public InertiaSimpleModel(final double mass, final Vector3D massCenter,
        final Matrix3D inertiaMatrix, final Vector3D inertiaReferencePoint, final Frame frame,
        final String partName) throws PatriusException {
        this.inMass = new MassProperty(mass);
        this.eq = new MassEquation(partName);
        this.center = new Vector3D(1.0, massCenter);
        final Matrix3D crossVectorMatrix = new Matrix3D(inertiaReferencePoint.subtract(massCenter));
        this.matrix = inertiaMatrix.add(crossVectorMatrix.multiply(crossVectorMatrix).multiply(mass));
        this.refFrame = frame;
        this.name = partName;
    }

    /** {@inheritDoc} */
    @Override
    public double getTotalMass() {
        return this.inMass.getMass();
    }

    /** {@inheritDoc} */
    @Override
    public double getTotalMass(final SpacecraftState state) {

        try {
            double mass = 0.;
            // Try to retrieve mass from state vector
            if (state.getAdditionalStatesMass().size() == 0) {
                // Mass is not in state vector, retrieve mass from mass provider
                mass = this.getTotalMass();
            } else {
                mass = state.getMass(this.name);
            }

            return mass;

        } catch (final PatriusException e) {
            // It cannot happen since a check on mass existence has been performed before
            throw new PatriusExceptionWrapper(e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public double getMass(final String partName) {
        this.checkProperty(partName);
        return this.getTotalMass();
    }

    /**
     * Make sure the given part has a mass property
     * 
     * @param partName
     *        name of part subject to mass flow variation
     */
    private void checkProperty(final String partName) {
        if (!this.name.contentEquals(partName)) {
            throw new IllegalArgumentException();
        }
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getMassCenter(final Frame frame, final AbsoluteDate date) throws PatriusException {

        // if the frame is different to the reference frame, vector computation in the new frame
        if (frame.equals(this.refFrame)) {
            return new Vector3D(1.0, this.center);
        } else {
            // else, the known vector is simply returned
            final Transform trans = this.refFrame.getTransformTo(frame, date);
            return trans.transformPosition(this.center);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Matrix3D getInertiaMatrix(final Frame frame, final AbsoluteDate date) throws PatriusException {

        // if the frame is different to the reference frame, matrix computation in the new frame
        if (frame.equals(this.refFrame)) {
            return this.matrix.multiply(1.0);
        } else {
            // else, the known matrix is simply returned

            // inertia matrix computation in this frame
            // frames transformations matrix
            final Transform toFrame = this.refFrame.getTransformTo(frame, date);

            // frames transformations matrix
            final Rotation rot = toFrame.getRotation();
            final Matrix3D toRefFrameMatrix = new Matrix3D(rot.revert().getMatrix());
            final Matrix3D toFrameMatrix = new Matrix3D(rot.getMatrix());

            return toRefFrameMatrix.multiply(this.matrix.multiply(toFrameMatrix));
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @param part
     *        ignore this parameter
     * @throws PatriusException
     *         if the mass is negative (PatriusMessages.MASS_ARGUMENT_IS_NEGATIVE)
     */
    @Override
    public void updateMass(final String part, final double mass) throws PatriusException {
        this.inMass.updateMass(mass);
    }

    /**
     * Updates the mass center.
     * 
     * @param massCenter
     *        the new mass center
     */
    public void updateMassCenter(final Vector3D massCenter) {
        this.center = new Vector3D(1.0, massCenter);
    }

    /**
     * Updates the inertia matrix.
     * 
     * @param inertiaMatrix
     *        the new inertia matrix.
     */
    public void updateIntertiaMatrix(final Matrix3D inertiaMatrix) {
        this.matrix = inertiaMatrix.multiply(1.0);
    }

    /** {@inheritDoc} */
    @Override
    public Matrix3D getInertiaMatrix(final Frame frame, final AbsoluteDate date,
                                     final Vector3D inertiaReferencePoint) throws PatriusException {

        final Matrix3D crossVectorMatrix = new Matrix3D(inertiaReferencePoint.subtract(this.center));
        return this.matrix.subtract(crossVectorMatrix.multiply(crossVectorMatrix)
            .multiply(this.getTotalMass()));
    }

    /** {@inheritDoc} */
    @Override
    public void addMassDerivative(final String partName, final double flowRate) {
        this.checkProperty(partName);
        this.eq.addMassDerivative(flowRate);
    }

    /** {@inheritDoc} */
    @Override
    public void setMassDerivativeZero(final String partName) {
        this.checkProperty(partName);
        this.eq.setMassDerivativeZero();
    }

    /** {@inheritDoc} */
    @Override
    public AdditionalEquations getAdditionalEquation(final String partName) {
        this.checkProperty(partName);
        return this.eq;
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getAllPartsNames() {
        final List<String> list = new ArrayList<String>();
        list.add(this.name);
        return list;
    }
}
