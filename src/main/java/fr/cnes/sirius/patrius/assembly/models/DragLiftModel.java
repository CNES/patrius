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
 * @history creation 12/11/2014
 */
/* 
 * HISTORY
* VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.6:FA:FA-2499:27/01/2021:[PATRIUS] Anomalie dans la gestion des panneaux solaires de la classe Vehicle 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:268:30/04/2015:drag and lift implementation
 * VERSION::DM:534:10/02/2016:Parametrization of force models
 * VERSION::DM:596:12/04/2016:Add exception for not implemented methods
 * VERSION::FA:673:12/09/2016: add getTotalMass(state)
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::DM:834:04/04/2017:create vehicle object
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.models;

import java.util.ArrayList;

import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.IPart;
import fr.cnes.sirius.patrius.assembly.PropertyType;
import fr.cnes.sirius.patrius.assembly.properties.AeroGlobalProperty;
import fr.cnes.sirius.patrius.forces.drag.DragSensitive;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.parameter.IParamDiffFunction;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.parameter.Parameterizable;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.propagation.MassProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Class that represents an drag and lift aero model, based on the vehicle.
 * 
 * @concurrency not thread-safe
 * 
 * @concurrency.comment The use of a frame (Assembly attribute) linked to the tree of frames in each of the parts makes
 *                      this class not thread-safe.
 * 
 * @author toussaintf
 * 
 * @version $Id$
 * 
 * @since 3.0
 * 
 */
public final class DragLiftModel extends Parameterizable implements DragSensitive {

    /**
     * Generated serial UID.
     */
    private static final long serialVersionUID = -8747054892764517686L;

    /** The considered vehicle on which the model is based on. */
    private final Assembly assembly;

    /** Mass model */
    private final MassProvider massModel;

    /**
     * Aero drag and lift model (the acceleration is computed from all the sub parts of the vehicle).
     * 
     * @param inAssembly
     *        The considered vehicle.
     */
    public DragLiftModel(final Assembly inAssembly) {
        super();
        // check if aero properties are specified and get them
        this.checkProperties(inAssembly);
        this.assembly = inAssembly;
        this.massModel = new MassModel(this.assembly);
    }

    /**
     * 
     * This method tests if the required property exist (AERO_GLOBAL). At least
     * one part with a mass is required.
     * 
     * @param assemblyIn
     *        The considered vehicle.
     */
    private void checkProperties(final Assembly assemblyIn) {

        boolean hasRadProp = false;
        boolean hasMassProp = false;

        for (final IPart part : assemblyIn.getParts().values()) {
            // checking aero properties
            if (part.hasProperty(PropertyType.AERO_GLOBAL)) {
                hasRadProp |= true;
                this.getCoefsParameters(part);
            }

            // checking mass property
            if (part.hasProperty(PropertyType.MASS)) {
                hasMassProp |= true;
            }
        }

        if (!hasRadProp || !hasMassProp) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.PDB_NO_AERO_GLOBAL_MASS_PROPERTIES);
        }
    }

    /**
     * Getting S, Cx and Cz coefficients parameters and store them in the local map (Parameterizable)
     * 
     * @param part
     *        : AeroGlobalProperty
     * 
     */
    private void getCoefsParameters(final IPart part) {
        final IParamDiffFunction aeroDragCoefFunction = ((AeroGlobalProperty) part.
            getProperty(PropertyType.AERO_GLOBAL)).getDragCoef();
        // Adding parameters to internal list
        this.addAllParameters(aeroDragCoefFunction.getParameters());

        final IParamDiffFunction aeroLiftCoefFunction = ((AeroGlobalProperty) part.
            getProperty(PropertyType.AERO_GLOBAL)).getLiftCoef();
        // Adding parameters to internal list
        this.addAllParameters(aeroLiftCoefFunction.getParameters());
    }

    /** {@inheritDoc} */
    @Override
    public ArrayList<Parameter> getJacobianParameters() {
        // return all parameters
        return this.getParameters();
    }

    /**
     * Method to compute the aero acceleration, based on the assembly.
     * 
     * @param state
     *        the current state of the spacecraft.
     * @param density
     *        the atmosphere density.
     * @param relativeVelocity
     *        the spacecraft velocity relative to the atmosphere in the spacecraftstate reference frame.
     * @return the acceleration applied on the assembly in the spacecraftstate frame.
     * @throws PatriusException
     *         if acceleration cannot be computed.
     * 
     */
    @Override
    public Vector3D dragAcceleration(final SpacecraftState state, final double density,
                                     final Vector3D relativeVelocity) throws PatriusException {

        Vector3D force = Vector3D.ZERO;

        this.assembly.updateMainPartFrame(state);

        for (final IPart part : this.assembly.getParts().values()) {

            Vector3D partComputedForce = Vector3D.ZERO;
            Vector3D partComputedDragForce = Vector3D.ZERO;
            Vector3D partComputedLiftForce = Vector3D.ZERO;

            if (part.hasProperty(PropertyType.AERO_GLOBAL)) {

                // get transform from spacecraftstate frame to part frame
                final Transform t = state.getFrame().getTransformTo(part.getFrame(), state.getDate());

                // relative velocity in part frame
                final Vector3D rvt = t.transformVector(relativeVelocity);

                // Area
                final AeroGlobalProperty aeroGlobalProp =
                    (AeroGlobalProperty) part.getProperty(PropertyType.AERO_GLOBAL);
                final double area = aeroGlobalProp.getCrossSection(rvt);

                // forces in part frame
                partComputedDragForce = dragForce(state, part, density, rvt, area);
                partComputedLiftForce = liftForce(state, part, density, rvt, area);

                // combined forces in part frame
                partComputedForce = partComputedDragForce.add(partComputedLiftForce);

                // global aero force in spacecraft state frame
                partComputedForce = t.getInverse().transformVector(partComputedForce);
            }

            // add the contribution to the total force
            force = force.add(partComputedForce);
        }

        // compute the acceleration
        return new Vector3D(MathLib.divide(1.0, this.massModel.getTotalMass(state)), force);
    }

    /**
     * Method to compute the drag force.
     * 
     * @param state
     *        the current state of the spacecraft
     * @param part
     *        the current part of the assembly
     * @param density
     *        the atmosphere density
     * @param relativeVelocity
     *        the spacecraft velocity relative to the atmosphere in the PART FRAME
     * @param area
     *        drag area
     * @return the drag force applied on the part in the PART FRAME
     * @throws PatriusException
     *         orekit frame exception
     */
    private static Vector3D dragForce(final SpacecraftState state, final IPart part, final double density,
                                      final Vector3D relativeVelocity, final double area) throws PatriusException {

        // get the AeroGlobalProperty
        final AeroGlobalProperty aeroGlobalProp =
            (AeroGlobalProperty) part.getProperty(PropertyType.AERO_GLOBAL);
        final double dragCoef = aeroGlobalProp.getDragCoef().value(state);

        // compute the drag force norm
        final double relativeVelocityNorm = relativeVelocity.getNorm();
        final double dragForceNorm = 0.5 * density * relativeVelocityNorm * relativeVelocityNorm * dragCoef * area;

        // the drag force is expressed along the relative velocity
        return new Vector3D(dragForceNorm, relativeVelocity.normalize());
    }

    /**
     * Method to compute the lift force.
     * 
     * @param state
     *        the current state of the spacecraft.
     * @param part
     *        the current part of the assembly.
     * @param density
     *        the atmosphere density.
     * @param relativeVelocity
     *        the spacecraft velocity relative to the atmosphere in the PART FRAME
     * @param area
     *        lift area
     * @return the lift force applied on the part in the PART FRAME.
     * @throws PatriusException
     *         orekit frame exception
     */
    private static Vector3D liftForce(final SpacecraftState state, final IPart part, final double density,
                                      final Vector3D relativeVelocity, final double area) throws PatriusException {

        // get the lift coefficient and spacecraft surface
        final AeroGlobalProperty aeroGlobalProp =
            (AeroGlobalProperty) part.getProperty(PropertyType.AERO_GLOBAL);
        final double liftCoef = aeroGlobalProp.getLiftCoef().value(state);

        // compute the lift force norm
        final double relativeVelocityNorm = relativeVelocity.getNorm();
        final double dragForceNorm = 0.5 * density * relativeVelocityNorm * relativeVelocityNorm * liftCoef * area;

        // Velocity of satellite wrt atmosphere
        // Negate is done since relativeVelocity is the velocity of the flow wrt to the part
        final Vector3D relVel = relativeVelocity.negate();

        // Compute the direction of the lift force (z axis of aerodynamic frame)
        // Take into account the rotation around the relative velocity vector
        final Vector3D z = Vector3D.crossProduct(Vector3D.PLUS_J, relVel).normalize();

        return new Vector3D(dragForceNorm, z);
    }

    /**
     * Compute acceleration derivatives with respect to ballistic coefficient.
     * 
     * @param s
     *        SpacecraftState
     * @param param
     *        name of parameter
     * @param density
     *        the atmosphere density.
     * @param relativeVelocity
     *        the spacecraft velocity relative to the atmosphere.
     * @param dAccdParam
     *        acceleration derivatives with respect to ballistic coefficient.
     * @exception PatriusException
     *            if derivatives cannot be computed
     */
    @Override
    public void addDDragAccDParam(final SpacecraftState s, final Parameter param, final double density,
                                  final Vector3D relativeVelocity,
                                  final double[] dAccdParam) throws PatriusException {
        // not implemented : throw exception
        throw PatriusException.createIllegalArgumentException(PatriusMessages.NOT_IMPLEMENTED);
    }

    /** {@inheritDoc} */
    @Override
    public void addDDragAccDState(final SpacecraftState s, final double[][] dAccdPos, final double[][] dAccdVel,
                                  final double density,
                                  final Vector3D acceleration, final Vector3D relativeVelocity,
                                  final boolean computeGradientPosition,
                                  final boolean computeGradientVelocity) throws PatriusException {
        // not implemented : throw exception
        throw PatriusException.createIllegalArgumentException(PatriusMessages.NOT_IMPLEMENTED);
    }

    /** {@inheritDoc} */
    @Override
    public DragSensitive copy(final Assembly newAssembly) {
        return new DragLiftModel(newAssembly);
    }
}
