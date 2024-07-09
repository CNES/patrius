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
 * HISTORY
* VERSION:4.6:FA:FA-2499:27/01/2021:[PATRIUS] Anomalie dans la gestion des panneaux solaires de la classe Vehicle 
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:93:31/03/2014:changed API for partial derivatives
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:200:28/08/2014: dealing with a negative mass in the propagator
 * VERSION::DM:284:06/10/2014:New architecture for parameterizable Parameters
 * VERSION::DM:534:10/02/2016:Parametrization of force models
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.models.utils;

import java.util.ArrayList;

import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.forces.BoxAndSolarArraySpacecraft;
import fr.cnes.sirius.patrius.forces.drag.DragSensitive;
import fr.cnes.sirius.patrius.forces.radiation.RadiationSensitive;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This class represents the features of a simplified spacecraft.
 * <p>
 * The model of this spacecraft is a simple spherical model, this means that all coefficients are constant and do not
 * depend of the direction.
 * </p>
 * <p>
 * Instances of this class are guaranteed to be immutable.
 * </p>
 * 
 * @see BoxAndSolarArraySpacecraft
 * @author &Eacute;douard Delente
 * @author Fabien Maussion
 * @author Pascal Parraud
 */
public class PatriusSphericalSpacecraft implements RadiationSensitive, DragSensitive {

    /** Serializable UID. */
    private static final long serialVersionUID = -1596721390500187750L;

    /** Cross section (m<sup>2</sup>). */
    private final double crossSection;

    /** Drag coefficient. */
    private double dragCoeff;

    /** Absorption coefficient. */
    private double absorptionCoeff;

    /** Specular reflection coefficient. */
    private double specularReflectionCoeff;

    /** Diffuse reflection coefficient. */
    private double diffuseReflectionCoeff;

    /** Composite drag coefficient (S.Cd/2). */
    private double kD;

    /** Composite radiation pressure coefficient. */
    private double kP;

    /** Part name concerned. */
    private final String partName;

    /**
     * Simple constructor.
     * 
     * @param crossSection2
     *        Surface (m<sup>2</sup>)
     * @param dragCoeff2
     *        drag coefficient (used only for drag)
     * @param absorptionCoeff2
     *        absorption coefficient between 0.0 an 1.0
     *        (used only for radiation pressure)
     * @param reflectionCoeff
     *        specular reflection coefficient between 0.0 an 1.0
     *        (used only for radiation pressure)
     * @param diffuseCoeff
     *        diffuse reflection coefficient between 0.0 an 1.0
     *        (used only for radiation pressure)
     * @param partName2
     *        concerned
     */
    public PatriusSphericalSpacecraft(final double crossSection2,
        final double dragCoeff2,
        final double absorptionCoeff2,
        final double reflectionCoeff,
        final double diffuseCoeff,
        final String partName2) {

        this.crossSection = crossSection2;
        this.dragCoeff = dragCoeff2;
        this.absorptionCoeff = absorptionCoeff2;
        this.specularReflectionCoeff = reflectionCoeff;
        this.diffuseReflectionCoeff = diffuseCoeff;
        this.partName = partName2;

        this.setKD();
        this.setKP();
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D dragAcceleration(final SpacecraftState state, final double density,
                                     final Vector3D relativeVelocity) throws PatriusException {
        final double massValue = state.getMass(this.partName);
        if (massValue <= 0.0) {
            throw new PatriusException(PatriusMessages.NOT_POSITIVE_MASS, massValue);
        }
        return new Vector3D(density * relativeVelocity.getNorm() * this.kD / massValue, relativeVelocity);

    }

    /** {@inheritDoc} */
    @Override
    public Vector3D radiationPressureAcceleration(final SpacecraftState state,
                                                  final Vector3D flux) throws PatriusException {
        final double massValue = state.getMass(this.partName);
        if (massValue <= 0.0) {
            throw new PatriusException(PatriusMessages.NOT_POSITIVE_MASS, massValue);
        }
        return new Vector3D(this.kP / massValue, flux);
    }

    /**
     * <p>
     * Method to compute the acceleration. This method has been implemented in order to validate the force model only.
     * The reason is that for the validation context, we do not want to set up an instance of the SpacecraftState object
     * to avoid the inertial frame of the spacecraft orbit.
     * </p>
     * 
     * <p>
     * (see Story #V86 and Feature #34 on https://www.orekit.org/forge/issues/34)
     * </p>
     * 
     * <p>
     * Out of the validation context, one must use the method Vector3D radiationPressureAcceleration(final
     * SpacecraftState state, final Vector3D flux)
     * </p>
     * 
     * @param mass
     *        mass of the spacecraft
     * @param flux
     *        radiation flux
     * @return acceleration vector
     * 
     */
    public Vector3D radiationPressureAcceleration(final double mass, final Vector3D flux) {
        return new Vector3D(this.kP / mass, flux);
    }

    /**
     * Set the drag coefficient.
     * 
     * @param value
     *        the drag coefficient to set
     */
    public void setDragCoefficient(final double value) {
        this.dragCoeff = value;
        this.setKD();
    }

    /**
     * Get the drag coefficient.
     * 
     * @return the drag coefficient
     */
    public double getDragCoefficient() {
        return this.dragCoeff;
    }

    /**
     * Set the absorption coefficient.
     * 
     * @param value
     *        the absorption coefficient
     */
    public void setAbsorptionCoefficient(final double value) {
        this.absorptionCoeff = value;
        this.setKP();
    }

    /**
     * Get the absorption coefficient.
     * 
     * @return the absorption coefficient
     */
    public double getAbsorptionCoefficient() {
        return this.absorptionCoeff;
    }

    /**
     * Set the reflection coefficient.
     * 
     * @param value
     *        the reflection coefficient
     */
    public void setReflectionCoefficient(final double value) {
        this.specularReflectionCoeff = value;
        this.setKP();
    }

    /**
     * Get the reflection coefficient.
     * 
     * @return the reflection coefficient
     */
    public double getReflectionCoefficient() {
        return this.specularReflectionCoeff;
    }

    /**
     * Set the diffusion coefficient.
     * 
     * @param value
     *        the diffusion coefficient
     */
    public void setDiffusionCoefficient(final double value) {
        this.diffuseReflectionCoeff = value;
    }

    /**
     * Get the diffusion coefficient.
     * 
     * @return the diffusion coefficient
     */
    public double getDiffusionCoefficient() {
        return this.diffuseReflectionCoeff;
    }

    /** Set kD value. */
    private void setKD() {
        this.kD = this.dragCoeff * this.crossSection / 2;
    }

    /** Set kP value. */
    private void setKP() {
        this.kP =
            this.crossSection * (1 + 4 * (1.0 - this.absorptionCoeff) * (1.0 - this.specularReflectionCoeff) / 9.0);
    }

    /** {@inheritDoc} */
    @Override
    public
            void
            addDDragAccDState(final SpacecraftState s, final double[][] dAccdPos, final double[][] dAccdVel,
                              final double density, final Vector3D acceleration, final Vector3D relativeVelocity,
                              final boolean computeGradientPosition, final boolean computeGradientVelocity)
                                                                                                           throws PatriusException {
        // this model does not support partial derivatives computation yet:
        throw PatriusException.createInternalError(null);
    }

    /** {@inheritDoc} */
    @Override
    public void addDDragAccDParam(final SpacecraftState s, final Parameter param, final double density,
                                  final Vector3D relativeVelocity, final double[] dAccdParam) throws PatriusException {
        // this model does not support partial derivatives computation yet:
        throw PatriusException.createInternalError(null);
    }

    /** {@inheritDoc} */
    @Override
    public void addDSRPAccDParam(final SpacecraftState s, final Parameter param, final double[] dAccdParam,
                                 final Vector3D satSunVector) throws PatriusException {
        throw PatriusException.createInternalError(null);
    }

    /** {@inheritDoc} */
    @Override
    public void addDSRPAccDState(final SpacecraftState s, final double[][] dAccdPos, final double[][] dAccdVel,
                                 final Vector3D satSunVector) throws PatriusException {
        throw PatriusException.createInternalError(null);
    }

    /** {@inheritDoc} */
    @Override
    public ArrayList<Parameter> getJacobianParameters() {
        // no parameters handle
        return new ArrayList<Parameter>();
    }

    /** {@inheritDoc} */
    @Override
    public DragSensitive copy(final Assembly assembly) {
        // Unused
        return null;
    }
}
