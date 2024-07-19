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
 * @history creation 21/05/2012
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.6:FA:FA-2499:27/01/2021:[PATRIUS] Anomalie dans la gestion des panneaux solaires de la classe Vehicle 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:93:31/03/2014:changed API for partial derivatives
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:284:06/10/2014:New architecture for parameterizable Parameters
 * VERSION::DM:200:28/08/2014: dealing with a negative mass in the propagator
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:534:10/02/2016:Parametrization of force models
 * VERSION::FA:662:26/08/2016:Computation times speed-up
 * VERSION::FA:673:12/09/2016: add getTotalMass(state)
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.models.utils;

import java.util.ArrayList;
import java.util.Map;

import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.AssemblyBuilder;
import fr.cnes.sirius.patrius.assembly.Part;
import fr.cnes.sirius.patrius.assembly.PropertyType;
import fr.cnes.sirius.patrius.assembly.models.AeroModel;
import fr.cnes.sirius.patrius.assembly.models.DirectRadiativeModel;
import fr.cnes.sirius.patrius.assembly.properties.AeroFacetProperty;
import fr.cnes.sirius.patrius.assembly.properties.MassProperty;
import fr.cnes.sirius.patrius.assembly.properties.RadiativeFacetProperty;
import fr.cnes.sirius.patrius.assembly.properties.RadiativeProperty;
import fr.cnes.sirius.patrius.assembly.properties.features.Facet;
import fr.cnes.sirius.patrius.forces.drag.DragSensitive;
import fr.cnes.sirius.patrius.forces.radiation.RadiationSensitive;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Mimicks partially the behaviour of Orekit's BoxAndSolarArraySpacecraft class,
 * with an underlying Assembly instance using the AeroModel and RadiativeModel.<br>
 * This is for validation test purposes.
 * The output of this class is meant to be compared to the BoxAndSolarArraySpacecraft's,
 * to show what differences the AeroModel introduces compared to the existing Orekit model.
 * 
 * @author Thomas Trapier
 * 
 * @version $Id$
 * 
 * @since 1.2
 * 
 */
public class AssemblyBoxAndSolarArraySpacecraft implements DragSensitive, RadiationSensitive {

     /** Serializable UID. */
    private static final long serialVersionUID = 8696093257453702541L;
    /** Part1 string. */
    private static final String FACET1 = "Facet1";
    /** Part2 string. */
    private static final String FACET2 = "Facet2";
    /** Part3 string. */
    private static final String FACET3 = "Facet3";
    /** Part4 string. */
    private static final String FACET4 = "Facet4";
    /** Part5 string. */
    private static final String FACET5 = "Facet5";
    /** Part6 string. */
    private static final String FACET6 = "Facet6";
    /** Solar array1 string */
    private static final String SOLAR_ARRAY1 = "Solar array1";
    /** Solar array2 string */
    private static final String SOLAR_ARRAY2 = "Solar array2";
    /** Drag coefficient. */
    final double dragCoefficient;
    /** Absorption coefficient. */
    private double absorptionCoeff;
    /** Specular reflection coefficient. */
    private double specularReflectionCoeff;
    /** Diffuse reflection coefficient. */
    private double diffuseReflectionCoeff;
    /** Underlying assembly. */
    final Assembly boxAndsolarArrayAssembly;
    /** Underlying aeromodel. */
    final AeroModel boxAndSolarAeroModel;
    /** Underlying aeromodel. */
    final DirectRadiativeModel boxAndSolarRadiativeModel;
    /** Sun model. */
    private final PVCoordinatesProvider inSun;
    /** Solar array rotation axis in spacecraft frame. */
    private final Vector3D saZ;
    /** Solar array area (m<sup>2</sup>). */
    private final double inSolarArrayArea;

    /**
     * Builds the spacecraft model.
     * 
     * @param mass
     *        main mass
     * @param xLength
     *        length of the body along its X axis (m)
     * @param yLength
     *        length of the body along its Y axis (m)
     * @param zLength
     *        length of the body along its Z axis (m)
     * @param sun
     *        sun model
     * @param solarArrayArea
     *        area of the solar array (m<sup>2</sup>)
     * @param solarArrayAxis
     *        solar array rotation axis in satellite frame
     * @param dragCoeff
     *        drag coefficient (used only for drag)
     * @param absorptionCoeff
     *        absorption coefficient between 0.0 an 1.0
     *        (used only for radiation pressure)
     * @param specularReflectionCoeff
     *        specular reflection coefficient between 0.0 an 1.0
     *        (used only for radiation pressure)
     * @param diffuseReflectionCoeff
     *        diffuse reflection coefficient between 0.0 an 1.0
     *        (used only for radiation pressure
     * @throws PatriusException
     */
    public AssemblyBoxAndSolarArraySpacecraft(final double xLength, final double yLength,
        final double zLength, final PVCoordinatesProvider sun,
        final double solarArrayArea,
        final Vector3D solarArrayAxis,
        final double dragCoeff, final double absorptionCoeff,
        final double specularReflectionCoeff,
        final double diffuseReflectionCoeff) throws PatriusException {
        this.dragCoefficient = dragCoeff;
        this.saZ = solarArrayAxis;
        this.inSolarArrayArea = solarArrayArea;
        this.boxAndsolarArrayAssembly = this.buildAssembly(xLength, yLength, zLength, solarArrayArea, solarArrayAxis);
        this.boxAndSolarAeroModel = new AeroModel(this.boxAndsolarArrayAssembly);
        this.boxAndSolarRadiativeModel = new DirectRadiativeModel(this.boxAndsolarArrayAssembly);
        this.inSun = sun;
    }

    /**
     * Builds the underlying assembly instance.
     * 
     * @param xLength
     *        length of the body along its X axis (m)
     * @param yLength
     *        length of the body along its Y axis (m)
     * @param zLength
     *        length of the body along its Z axis (m)
     * @param solarArrayArea
     *        area of the solar array (m<sup>2</sup>)
     * @param solarArrayAxis
     *        solar array rotation axis in satellite frame
     * @return the built assembly
     * @throws PatriusException
     */
    private Assembly buildAssembly(final double xLength, final double yLength,
                                   final double zLength,
                                   final double solarArrayArea,
                                   final Vector3D solarArrayAxis) throws PatriusException {
        final AssemblyBuilder abul = new AssemblyBuilder();

        // facets
        abul.addMainPart(FACET1);
        abul.addPart(FACET2, FACET1, Transform.IDENTITY);
        abul.addPart(FACET3, FACET1, Transform.IDENTITY);
        abul.addPart(FACET4, FACET1, Transform.IDENTITY);
        abul.addPart(FACET5, FACET1, Transform.IDENTITY);
        abul.addPart(FACET6, FACET1, Transform.IDENTITY);
        abul.addPart(SOLAR_ARRAY1, FACET1, Transform.IDENTITY);
        abul.addPart(SOLAR_ARRAY2, FACET1, Transform.IDENTITY);

        // Mass property (mass for main and zero for others)
        final MassProperty mainMass = new MassProperty(1000.);
        final MassProperty zeroMass = new MassProperty(0.);
        abul.addProperty(mainMass, FACET1);
        abul.addProperty(zeroMass, FACET2);
        abul.addProperty(zeroMass, FACET3);
        abul.addProperty(zeroMass, FACET4);
        abul.addProperty(zeroMass, FACET5);
        abul.addProperty(zeroMass, FACET6);

        // facet properties
        final Facet facet1 = new Facet(Vector3D.PLUS_K, xLength * yLength);

        final AeroFacetProperty facetProp10 = new AeroFacetProperty(facet1, this.dragCoefficient, this.dragCoefficient);
        final RadiativeFacetProperty facetProp11 = new RadiativeFacetProperty(facet1);

        abul.addProperty(facetProp10, FACET1);
        final RadiativeProperty rp = new RadiativeProperty(this.absorptionCoeff, this.specularReflectionCoeff,
            this.diffuseReflectionCoeff);
        abul.addProperty(facetProp11, FACET1);
        abul.addProperty(rp, FACET1);

        final Facet facet2 = new Facet(Vector3D.MINUS_K, xLength * yLength);
        final AeroFacetProperty facetProp20 = new AeroFacetProperty(facet2, this.dragCoefficient, this.dragCoefficient);
        final RadiativeFacetProperty facetProp21 = new RadiativeFacetProperty(facet2);
        abul.addProperty(facetProp20, FACET2);
        abul.addProperty(facetProp21, FACET2);
        abul.addProperty(rp, FACET2);

        final Facet facet3 = new Facet(Vector3D.PLUS_J, xLength * zLength);
        final AeroFacetProperty facetProp30 = new AeroFacetProperty(facet3, this.dragCoefficient, this.dragCoefficient);
        final RadiativeFacetProperty facetProp31 = new RadiativeFacetProperty(facet3);
        abul.addProperty(facetProp30, FACET3);
        abul.addProperty(facetProp31, FACET3);
        abul.addProperty(rp, FACET3);

        final Facet facet4 = new Facet(Vector3D.MINUS_J, xLength * zLength);
        final AeroFacetProperty facetProp40 = new AeroFacetProperty(facet4, this.dragCoefficient, this.dragCoefficient);
        final RadiativeFacetProperty facetProp41 = new RadiativeFacetProperty(facet4);
        abul.addProperty(facetProp40, FACET4);
        abul.addProperty(facetProp41, FACET4);
        abul.addProperty(rp, FACET4);

        final Facet facet5 = new Facet(Vector3D.PLUS_I, yLength * zLength);
        final AeroFacetProperty facetProp50 = new AeroFacetProperty(facet5, this.dragCoefficient, this.dragCoefficient);
        final RadiativeFacetProperty facetProp51 = new RadiativeFacetProperty(facet5);
        abul.addProperty(facetProp50, FACET5);
        abul.addProperty(facetProp51, FACET5);
        abul.addProperty(rp, FACET5);

        final Facet facet6 = new Facet(Vector3D.MINUS_I, yLength * zLength);
        final AeroFacetProperty facetProp60 = new AeroFacetProperty(facet6, this.dragCoefficient, this.dragCoefficient);
        final RadiativeFacetProperty facetProp61 = new RadiativeFacetProperty(facet5);
        abul.addProperty(facetProp60, FACET6);
        abul.addProperty(facetProp61, FACET6);
        abul.addProperty(rp, FACET6);

        return abul.returnAssembly();
    }

    /**
     * Updates the assembly by redefining its solar panel
     * 
     * @param state
     *        current state information: date, kinematics, attitude
     * @exception PatriusException
     *            if sun direction cannot be computed in best lightning
     *            configuration
     */
    private void updateSolarPanel(final SpacecraftState state)
                                                              throws PatriusException {

        final AbsoluteDate date = state.getDate();

        // compute orientation for best lightning
        final Frame frame = state.getFrame();
        final Vector3D sunInert = this.inSun.getPVCoordinates(date, frame).getPosition().normalize();
        final Vector3D sunSpacecraft = state.getAttitude().getRotation().applyInverseTo(sunInert);
        final double d = Vector3D.dotProduct(sunSpacecraft, this.saZ);
        final double f = 1 - d * d;

        final double s = 1.0 / MathLib.sqrt(f);
        final Vector3D normal = new Vector3D(s, sunSpacecraft, -s * d, this.saZ);

        final AeroFacetProperty aeroProp =
            new AeroFacetProperty(new Facet(normal, this.inSolarArrayArea), this.dragCoefficient,
                this.dragCoefficient);
        this.boxAndsolarArrayAssembly.removePart(SOLAR_ARRAY1);
        final Part solArr = new Part(SOLAR_ARRAY1, this.boxAndsolarArrayAssembly.getPart(FACET1), Transform.IDENTITY);
        solArr.addProperty(aeroProp);
        this.boxAndsolarArrayAssembly.addPart(solArr);

        this.boxAndsolarArrayAssembly.removePart(SOLAR_ARRAY2);
        final Part solArr2 = new Part(SOLAR_ARRAY2, this.boxAndsolarArrayAssembly.getPart(FACET1), Transform.IDENTITY);
        final AeroFacetProperty facetAeroProp2 =
            new AeroFacetProperty(new Facet(normal.negate(), this.inSolarArrayArea),
                this.dragCoefficient, this.dragCoefficient);
        solArr2.addProperty(facetAeroProp2);
        this.boxAndsolarArrayAssembly.addPart(solArr2);
    }

    @Override
    public Vector3D dragAcceleration(final SpacecraftState state, final double density,
                                     final Vector3D relativeVelocity)
                                                                     throws PatriusException {
        this.updateSolarPanel(state);

        // Update the assembly mass with the state's mass
        final MassProperty m =
            (MassProperty) this.boxAndsolarArrayAssembly.getMainPart().getProperty(PropertyType.MASS);
        final Map<String, double[]> massStates = state.getAdditionalStatesMass();
        final String mainPartName = this.boxAndsolarArrayAssembly.getMainPart().getName();
        if (massStates.containsKey("MASS_" + mainPartName)) {
            m.updateMass(massStates.get("MASS_" + mainPartName)[0]);
        }
        return new AeroModel(this.boxAndsolarArrayAssembly).dragAcceleration(state, density, relativeVelocity);
    }

    @Override
    public void addDDragAccDParam(final SpacecraftState s, final Parameter param, final double density,
                                  final Vector3D relativeVelocity, final double[] dAccdParam)
                                                                                             throws PatriusException {
        // does nothing
    }

    @Override
    public void addDDragAccDState(final SpacecraftState s, final double[][] dAccdPos, final double[][] dAccdVel,
                                  final double density,
                                  final Vector3D acceleration, final Vector3D relativeVelocity,
                                  final boolean computeGradientPosition,
                                  final boolean computeGradientVelocity) throws PatriusException {
        // does nothing
    }

    @Override
    public Vector3D
            radiationPressureAcceleration(final SpacecraftState state, final Vector3D flux)
                                                                                           throws PatriusException {
        this.updateSolarPanel(state);

        // Update the assembly mass with the state's mass
        final MassProperty m =
            (MassProperty) this.boxAndsolarArrayAssembly.getMainPart().getProperty(PropertyType.MASS);
        final Map<String, double[]> massStates = state.getAdditionalStatesMass();
        final String mainPartName = this.boxAndsolarArrayAssembly.getMainPart().getName();
        if (massStates.containsKey(mainPartName)) {
            m.updateMass(massStates.get(mainPartName)[0]);
        }
        return this.boxAndSolarRadiativeModel.radiationPressureAcceleration(state, flux);
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

    /** {@inheritDoc} */
    @Override
    public ArrayList<Parameter> getJacobianParameters() {
        // this model does not support partial derivatives computation yet:
        throw PatriusException.createInternalError(null);
    }

    public Assembly getAssembly() {
        return this.boxAndsolarArrayAssembly;
    }

    /** {@inheritDoc} */
    @Override
    public DragSensitive copy(final Assembly assembly) {
        // Unused
        return null;
    }
}
