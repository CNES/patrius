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
 * HISTORY
* VERSION:4.6:FA:FA-2499:27/01/2021:[PATRIUS] Anomalie dans la gestion des panneaux solaires de la classe Vehicle 
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:93:31/03/2014:changed API for partial derivatives
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:284:06/10/2014:New architecture for parameterizable Parameters
 * VERSION::DM:534:10/02/2016:Parametrization of force models
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.models.utils;

import java.util.ArrayList;
import java.util.Map;

import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.AssemblyBuilder;
import fr.cnes.sirius.patrius.assembly.PropertyType;
import fr.cnes.sirius.patrius.assembly.models.AeroModel;
import fr.cnes.sirius.patrius.assembly.models.DirectRadiativeModel;
import fr.cnes.sirius.patrius.assembly.properties.AeroSphereProperty;
import fr.cnes.sirius.patrius.assembly.properties.MassProperty;
import fr.cnes.sirius.patrius.assembly.properties.RadiativeProperty;
import fr.cnes.sirius.patrius.assembly.properties.RadiativeSphereProperty;
import fr.cnes.sirius.patrius.forces.drag.DragSensitive;
import fr.cnes.sirius.patrius.forces.radiation.RadiationSensitive;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Mimicks partially the behaviour of Orekit's SphericalSpacecraft class,
 * with an underlying Assembly instance using the AeroModel and RadiativeModel.<br>
 * This is for validation test purposes.
 * The output of this class is meant to be compared to the SphericalSpacecraft's,
 * to show what differences the AeroModel introduces compared to the existing Orekit model.
 * 
 * @author cardosop
 * 
 * @version $Id$
 * 
 * @since 1.2
 * 
 */
public class AssemblySphericalSpacecraft implements DragSensitive, RadiationSensitive {

    /**
     * Serial UID.
     */
    private static final long serialVersionUID = -1656254220312265782L;
    /** Main body string. */
    private static final String MAIN_BODY = "Main body";
    /** Sphere radius. */
    final double sphereRadius;
    /** Drag coefficient. */
    final double dragCoefficient;
    /** Absorption coefficient. */
    private final double absorptionCoeff;
    /** Specular reflection coefficient. */
    private final double specularReflectionCoeff;
    /** Diffuse reflection coefficient. */
    private final double diffuseReflectionCoeff;
    /** Underlying assembly. */
    final Assembly sphereAssembly;
    /** Underlying aeromodel. */
    final AeroModel sphereAeroModel;
    /** Underlying radiativemodel. */
    final DirectRadiativeModel sphereRadiativeModel;

    /**
     * Constructor.
     * 
     * @param crossSection
     *        area of the cross section of the sphere.
     * @param dragCoef
     *        drag coefficient
     * @param absorptionCoeff
     *        absorption coefficient between 0.0 an 1.0
     *        (used only for radiation pressure)
     * @param reflectionCoeff
     *        specular reflection coefficient between 0.0 an 1.0
     *        (used only for radiation pressure)
     * @param diffuseCoeff
     *        diffuse reflection coefficient between 0.0 an 1.0
     *        (used only for radiation pressure)
     * @throws PatriusException
     */
    public AssemblySphericalSpacecraft(final double crossSection, final double dragCoef,
        final double absorptionCoeff,
        final double reflectionCoeff,
        final double diffuseCoeff) throws PatriusException {
        // Compute the sphere radius
        this.sphereRadius = MathLib.sqrt(crossSection / FastMath.PI);
        this.dragCoefficient = dragCoef;
        this.absorptionCoeff = absorptionCoeff;
        this.specularReflectionCoeff = reflectionCoeff;
        this.diffuseReflectionCoeff = diffuseCoeff;

        // Build the assembly
        this.sphereAssembly = this.buildAssembly();
        // Build the model
        this.sphereAeroModel = new AeroModel(this.sphereAssembly);
        this.sphereRadiativeModel = new DirectRadiativeModel(this.sphereAssembly);
    }

    /**
     * Builds the underlying assembly instance.
     * 
     * @return the built assembly
     * @throws PatriusException
     */
    private Assembly buildAssembly() throws PatriusException {
        // The assembly has a single underlying aero sphere
        final AssemblyBuilder abul = new AssemblyBuilder();
        abul.addMainPart(MAIN_BODY);
        // Mass property (zero for now)
        final MassProperty mp = new MassProperty(1000.);
        abul.addProperty(mp, MAIN_BODY);
        // Aero property
        final AeroSphereProperty ap = new AeroSphereProperty(this.sphereRadius, this.dragCoefficient);
        abul.addProperty(ap, MAIN_BODY);
        // Radiative property
        final RadiativeProperty rp = new RadiativeProperty(this.absorptionCoeff, this.specularReflectionCoeff,
            this.diffuseReflectionCoeff);
        abul.addProperty(rp, MAIN_BODY);
        // radiative sphere property
        final RadiativeSphereProperty rsp = new RadiativeSphereProperty(this.sphereRadius);
        abul.addProperty(rsp, MAIN_BODY);
        // Build the assembly
        return abul.returnAssembly();
    }

    @Override
    public
            Vector3D
            dragAcceleration(final SpacecraftState state, final double density, final Vector3D relativeVelocity)
                                                                                                                throws PatriusException {
        // Update the assembly mass with the state's mass
        final MassProperty m = (MassProperty) this.sphereAssembly.getMainPart().getProperty(PropertyType.MASS);
        final Map<String, double[]> massStates = state.getAdditionalStatesMass();
        final String mainPartName = this.sphereAssembly.getMainPart().getName();
        if (massStates.containsKey("MASS_" + mainPartName)) {
            m.updateMass(massStates.get("MASS_" + mainPartName)[0]);
        }
        // Call the underlying model
        return this.sphereAeroModel.dragAcceleration(state, density, relativeVelocity);
    }

    @Override
    public void addDDragAccDParam(final SpacecraftState s, final Parameter param, final double density,
                                  final Vector3D relativeVelocity, final double[] dAccdParam)
                                                                                             throws PatriusException {
        // does nothing
    }

    @Override
    public
            void
            addDDragAccDState(final SpacecraftState s, final double[][] dAccdPos, final double[][] dAccdVel,
                              final double density, final Vector3D acceleration, final Vector3D relativeVelocity,
                              final boolean computeGradientPosition, final boolean computeGradientVelocity)
                                                                                                           throws PatriusException {
        // does nothing
    }

    @Override
    public Vector3D
            radiationPressureAcceleration(final SpacecraftState state, final Vector3D flux)
                                                                                           throws PatriusException {
        // Update the assembly mass with the state's mass
        final MassProperty m = (MassProperty) this.sphereAssembly.getMainPart().getProperty(PropertyType.MASS);
        final Map<String, double[]> massStates = state.getAdditionalStatesMass();
        final String mainPartName = this.sphereAssembly.getMainPart().getName();
        if (massStates.containsKey(mainPartName)) {
            m.updateMass(massStates.get(mainPartName)[0]);
        }
        return this.sphereRadiativeModel.radiationPressureAcceleration(state, flux);
    }

    @Override
    public void addDSRPAccDParam(final SpacecraftState s, final Parameter param, final double[] dAccdParam,
                                 final Vector3D satSunVector)
                                                             throws PatriusException {
        // does nothing
    }

    @Override
    public void addDSRPAccDState(final SpacecraftState s, final double[][] dAccdPos, final double[][] dAccdVel,
                                 final Vector3D satSunVector) throws PatriusException {
        // does nothing
    }

    public Assembly getAssembly() {
        return this.sphereAssembly;
    }

    /** {@inheritDoc} */
    @Override
    public ArrayList<Parameter> getJacobianParameters() {
        final ArrayList<Parameter> list = new ArrayList<Parameter>();

        if (this.sphereAeroModel != null) {
            list.addAll(this.sphereAeroModel.getJacobianParameters());
        }
        if (this.sphereRadiativeModel != null) {
            list.addAll(this.sphereRadiativeModel.getJacobianParameters());
        }
        return list;
    }

    /** {@inheritDoc} */
    @Override
    public DragSensitive copy(final Assembly assembly) {
        // Unused
        return null;
    }
}
