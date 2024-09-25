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
 * @history Created 27/04/2012
 *
 * HISTORY
 * VERSION:4.13:DM:DM-5:08/12/2023:[PATRIUS] Orientation d'un corps celeste sous forme de quaternions
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.11:FA:FA-3320:22/05/2023:[PATRIUS] Mauvaise implementation de la methode hashCode de Vector3D
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.10:DM:DM-3194:03/11/2022:[PATRIUS] Fusion des interfaces GeometricBodyShape et BodyShape 
 * VERSION:4.9.1:DM:DM-3168:01/06/2022:[PATRIUS] Ajout de la classe ConstantPVCoordinatesProvider
 * VERSION:4.9:DM:DM-3168:10/05/2022:[PATRIUS] Ajout de la classe FixedPVCoordinatesProvider 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-3040:15/11/2021:[PATRIUS]Reversement des evolutions de la branche patrius-for-lotus 
 * VERSION:4.5:DM:DM-2445:27/05/2020:optimisation de SolarActivityReader 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:106:16/07/2013:Account of massless parts with aero props. Account of parts with mass and without aero properties.
 * VERSION::FA:93:01/04/2014:Changed partial derivatives API
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:284:06/10/2014:New architecture for parameterizable Parameters
 * VERSION::DM:200:28/08/2014: dealing with a negative mass in the propagator
 * VERSION::DM:268:30/04/2015:drag and lift implementation
 * VERSION::FA:412:04/05/2015:surface to mass ratio
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::FA:495:10/08/2015:correction on the partial derivatives wrt cx
 * VERSION::DM:490:17/11/2015:Mise à jour testJacobianList() au vu de la nouvelle implémentation de DragForce
 * VERSION::DM:534:10/02/2016:Parametrization of force models
 * VERSION::DM:596:12/04/2016:Improve test coherence
 * VERSION::FA:673:12/09/2016: add getTotalMass(state)
 * VERSION::DM:834:04/04/2017:create vehicle object
 * VERSION::DM:571:12/04/2017:Add density partial derivative contribution in partial derivatives
 * VERSION::FA:1192:30/08/2017:update parts frame
 * VERSION::FA:1177:06/09/2017:add Cook model validation test
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.models;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.AssemblyBuilder;
import fr.cnes.sirius.patrius.assembly.IPartProperty;
import fr.cnes.sirius.patrius.assembly.PropertyType;
import fr.cnes.sirius.patrius.assembly.Vehicle;
import fr.cnes.sirius.patrius.assembly.models.utils.AssemblyBoxAndSolarArraySpacecraft;
import fr.cnes.sirius.patrius.assembly.models.utils.AssemblySphericalSpacecraft;
import fr.cnes.sirius.patrius.assembly.properties.AeroCrossSectionProperty;
import fr.cnes.sirius.patrius.assembly.properties.AeroFacetProperty;
import fr.cnes.sirius.patrius.assembly.properties.AeroSphereProperty;
import fr.cnes.sirius.patrius.assembly.properties.MassProperty;
import fr.cnes.sirius.patrius.assembly.properties.features.Facet;
import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.LofOffset;
import fr.cnes.sirius.patrius.bodies.EllipsoidBodyShape;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.forces.atmospheres.Atmosphere;
import fr.cnes.sirius.patrius.forces.atmospheres.SimpleExponentialAtmosphere;
import fr.cnes.sirius.patrius.forces.drag.DragForce;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.frames.UpdatableFrame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Parallelepiped;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.RightCircularCylinder;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Sphere;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.parameter.ConstantFunction;
import fr.cnes.sirius.patrius.math.parameter.IParamDiffFunction;
import fr.cnes.sirius.patrius.math.parameter.LinearFunction;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.parameter.PiecewiseFunction;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.ConstantPVCoordinatesProvider;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.MassProvider;
import fr.cnes.sirius.patrius.propagation.SimpleMassModel;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Unit tests for the AeroModel class.
 * 
 * @author cardosop
 * 
 * @version $Id$
 * 
 * @since 1.2
 * 
 */
public class AeroModelTest {

    /** Body name. */
    private static final String MAIN_BODY = "mainBody";
    /** Part name. */
    private static final String PART1 = "part1";
    /** Default mass model. */
    private static MassProvider massModel = new SimpleMassModel(1000., "default");

    /** Features description. */
    public enum features {

        /**
         * @featureTitle Aero model
         * 
         * @featureDescription Aerodynamic model for an assembly
         * 
         * @coveredRequirements DV-VEHICULE_400, DV-VEHICULE_410, DV-VEHICULE_420, DV-VEHICULE_450, DV-VEHICULE_460
         */
        AERO_MODEL,

        /**
         * @featureTitle Drag acceleration partial derivatives
         * 
         * @featureDescription Partial derivatives for the drag acceleration
         * 
         * @coveredRequirements DV-MOD_160
         */
        DRAG_PARTIAL_DERIVATIVES
    }

    /**
     * A vehicle for the tests.
     * An Assembly is created from it.
     */
    private Vehicle vehicle;

    /** An Assembly for the tests. */
    private Assembly testAssembly;

    /** Epsilon for double comparisons. */
    private final double comparisonEpsilon = Precision.DOUBLE_COMPARISON_EPSILON;

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(AeroModelTest.class.getSimpleName(), "Aero model");
    }

    /**
     * FA 93 : added test to ensure the list of jacobian parameters is correct
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#AERO_MODEL}
     * 
     * @testedMethod {@link DragForce#getParameters()}
     * 
     * @description Test for the AeroModel parameters
     * 
     * @input nothing
     * 
     * @output parameter list
     * 
     * @testPassCriteria two elements
     * 
     * @referenceVersion 2.2
     * 
     * @nonRegressionVersion 2.3
     */
    @Test
    public void testJacobianList() throws PatriusException {

        // Configure data management accordingly
        Utils.setDataRoot("potentialPartialDerivatives");

        FramesFactory.getITRF();

        // create the assembly
        final Parameter cn = new Parameter("C_N", 1.7);
        final Parameter ct = new Parameter("C_T", 2.3);
        this.createTestAssemblyFacetOnly(Vector3D.PLUS_I, cn, ct);

        // create an AeroModel for the assembly
        final AeroModel aeroModel = new AeroModel(this.testAssembly);
        final DragForce att = new DragForce(null, aeroModel);

        Assert.assertEquals(3, att.getParameters().size());

        final String n = "C_N";
        final String t = "C_T";

        Assert.assertNotNull(att.getParameters().get(0).getName().equals(n));
        Assert.assertNotNull(att.getParameters().get(1).getName().equals(t));
    }

    /**
     * Creates a new Assembly and sets testAssembly with it.
     * 
     * @throws PatriusException
     */
    private void createValidTestAssembly() throws PatriusException {
        final AssemblyBuilder builder = new AssemblyBuilder();

        // add main part (one sphere) and part2 (one facet)
        builder.addMainPart(MAIN_BODY);
        builder.addPart(PART1, MAIN_BODY, Transform.IDENTITY);

        // one facet
        final Vector3D normal = new Vector3D(0.0, 0.0, -2.0);
        final Facet facet = new Facet(normal, 25 * FastMath.PI);

        // adding aero properties
        final IPartProperty aeroFacetProp = new AeroFacetProperty(facet);
        builder.addProperty(aeroFacetProp, PART1);
        // sphere property
        final double radius = 10.;
        final IPartProperty aeroProp = new AeroSphereProperty(radius);
        builder.addProperty(aeroProp, MAIN_BODY);
        builder.addProperty(aeroProp, PART1);

        // adding mass properties
        final IPartProperty massMainProp = new MassProperty(100.);
        builder.addProperty(massMainProp, MAIN_BODY);
        final IPartProperty part2Prop = new MassProperty(10.);
        builder.addProperty(part2Prop, PART1);

        // assembly creation
        this.testAssembly = builder.returnAssembly();
    }

    /**
     * Creates a new Assembly with a sphere as main and a solar panels
     * and sets testAssembly with it.
     * 
     * @throws PatriusException
     */
    private void createTestAssemblyVehicleSphereWithPanels() throws PatriusException {

        // Creates the vehicle
        this.vehicle = new Vehicle();

        // sphere property
        final double radius = 10.;
        final Sphere sphere = new Sphere(Vector3D.ZERO, radius);
        this.vehicle.setMainShape(sphere);
        this.vehicle.setDryMass(1000.);

        // add solar panels
        final Parameter cn = new Parameter("C_N", 1.7);
        final Parameter ct = new Parameter("C_T", 0.);
        final double area = 10.;
        this.vehicle.setAerodynamicsProperties(cn.getValue(), ct.getValue());
        this.vehicle.addSolarPanel(Vector3D.PLUS_I, area);
        this.vehicle.addSolarPanel(Vector3D.MINUS_I, area);

        // Build the Assembly
        this.testAssembly = this.vehicle.createAssembly(FramesFactory.getGCRF());
    }

    /**
     * Creates a new Assembly with a cylinder as main and sets testAssembly with it.
     * 
     * @throws PatriusException
     */
    private void createTestAssemblyVehicleCylinderOnly() throws PatriusException {

        // Creates the vehicle
        this.vehicle = new Vehicle();

        // cylinder property
        final RightCircularCylinder cylinder = new RightCircularCylinder(new Vector3D(10, 20, 30), Vector3D.PLUS_K, 2,
            10.);

        this.vehicle.setMainShape(cylinder);
        this.vehicle.setDryMass(1000.);
        final Parameter cn = new Parameter("C_N", 1.7);
        final Parameter ct = new Parameter("C_T", 2.3);
        this.vehicle.setAerodynamicsProperties(cn.getValue(), ct.getValue());

        // Build the Assembly
        this.testAssembly = this.vehicle.createAssembly(FramesFactory.getGCRF());
    }

    /**
     * Creates a new Assembly with a cylinder as main and sets testAssembly with it.
     * 
     * @throws PatriusException
     */
    private void createTestAssemblyVehicleCylinderWithPanels() throws PatriusException {

        // Creates the vehicle
        this.vehicle = new Vehicle();

        // cylinder property
        final RightCircularCylinder cylinder = new RightCircularCylinder(new Vector3D(10, 20, 30), Vector3D.PLUS_K, 2,
            10.);

        this.vehicle.setMainShape(cylinder);
        this.vehicle.setDryMass(1000.);

        // add solar panels
        final Parameter cn = new Parameter("C_N", 1.7);
        final Parameter ct = new Parameter("C_T", 0.);
        final double area = 10.;
        this.vehicle.setAerodynamicsProperties(cn.getValue(), ct.getValue());
        this.vehicle.addSolarPanel(Vector3D.PLUS_I.add(Vector3D.PLUS_K), area);
        this.vehicle.addSolarPanel(Vector3D.MINUS_I.add(Vector3D.PLUS_K), area);

        // Build the Assembly
        this.testAssembly = this.vehicle.createAssembly(FramesFactory.getGCRF());
    }

    /**
     * Creates a new Assembly with a parallelepiped as main and a solar panels
     * and sets testAssembly with it.
     * 
     * @throws PatriusException
     */
    private void createTestAssemblyVehicleParal() throws PatriusException {

        // Creates the vehicle
        this.vehicle = new Vehicle();

        // Creation of a parallelepiped
        final Vector3D center = Vector3D.ZERO;
        final Vector3D uVector = Vector3D.PLUS_I;
        final Vector3D inputvVector = Vector3D.PLUS_J;
        final Parallelepiped parallelepiped = new Parallelepiped(center, uVector, inputvVector, 2.0, 2.0, 6.0);

        this.vehicle.setMainShape(parallelepiped);
        this.vehicle.setDryMass(100.);
        final Parameter cn = new Parameter("C_N", 1.7);
        final Parameter ct = new Parameter("C_T", 2.3);
        this.vehicle.setAerodynamicsProperties(cn.getValue(), ct.getValue());

        // Build the Assembly
        this.testAssembly = this.vehicle.createAssembly(FramesFactory.getGCRF());
    }

    /**
     * Creates a new Assembly with a parallelepiped as main and a solar panels
     * and sets testAssembly with it.
     * 
     * @throws PatriusException
     */
    private void createTestAssemblyVehicleParalWithPanels() throws PatriusException {

        // Creates the vehicle
        this.vehicle = new Vehicle();

        // Creation of a parallelepiped
        final Vector3D center = Vector3D.ZERO;
        final Vector3D uVector = Vector3D.PLUS_I;
        final Vector3D inputvVector = Vector3D.PLUS_J;
        final Parallelepiped parallelepiped = new Parallelepiped(center, uVector, inputvVector, 2.0, 2.0, 6.0);

        this.vehicle.setMainShape(parallelepiped);
        this.vehicle.setDryMass(100.);

        // add solar panels
        final Parameter cn = new Parameter("C_N", 1.7);
        final Parameter ct = new Parameter("C_T", 0.);
        final double area = 10.;
        this.vehicle.setAerodynamicsProperties(cn.getValue(), ct.getValue());
        this.vehicle.addSolarPanel(Vector3D.PLUS_I.add(Vector3D.PLUS_K), area);
        this.vehicle.addSolarPanel(Vector3D.MINUS_I.add(Vector3D.PLUS_K), area);

        // Build the Assembly
        this.testAssembly = this.vehicle.createAssembly(FramesFactory.getGCRF());
    }

    /**
     * Creates a new Assembly with invalid aero properties and sets testAssembly with it.
     * 
     * @throws PatriusException
     */
    private void createInvalidTestAssembly() throws PatriusException {
        final AssemblyBuilder builder = new AssemblyBuilder();

        // add main part (one sphere) and two facets
        builder.addMainPart(MAIN_BODY);

        // mass
        final IPartProperty massMainProp = new MassProperty(100.);
        builder.addProperty(massMainProp, MAIN_BODY);

        builder.initMainPartFrame(new UpdatableFrame(FramesFactory.getGCRF(), Transform.IDENTITY, "MainFrame"));

        // no aero
        this.testAssembly = builder.returnAssembly();
    }

    /**
     * Creates a new Assembly with invalid aero properties and sets testAssembly with it.
     * 
     * @throws PatriusException
     */
    private void createInvalidTestAssembly2() throws PatriusException {
        final AssemblyBuilder builder = new AssemblyBuilder();

        // add main part
        builder.addMainPart(MAIN_BODY);

        // facet
        final Vector3D normal = new Vector3D(0.0, 0.0, -2.0);
        final Facet facet = new Facet(normal, 25 * FastMath.PI);

        final IPartProperty aeroFacetProp = new AeroFacetProperty(facet);
        builder.addProperty(aeroFacetProp, MAIN_BODY);

        // adding mass properties
        final IPartProperty massMainProp = new MassProperty(100.);
        builder.addProperty(massMainProp, MAIN_BODY);

        // sphere property
        final double radius = 10.;
        final AeroSphereProperty asp = new AeroSphereProperty(radius);
        builder.addProperty(asp, MAIN_BODY);

        // The main body has both a facet and a sphere property!

        // adding aero properties
        final IPartProperty aeroProp = new AeroFacetProperty(facet);
        builder.addProperty(aeroProp, MAIN_BODY);

        builder.initMainPartFrame(new UpdatableFrame(FramesFactory.getGCRF(), Transform.IDENTITY, "MainFrame"));

        // assembly creation
        this.testAssembly = builder.returnAssembly();
    }

    /**
     * Creates a new Assembly with only a sphere as main,
     * and sets testAssembly with it.
     * 
     * @throws PatriusException
     */
    private void createTestAssemblySphereOnly() throws PatriusException {
        final AssemblyBuilder builder = new AssemblyBuilder();

        // add main part (one sphere)
        final String facet = "facet";
        builder.addMainPart(MAIN_BODY);
        builder.addPart(facet, MAIN_BODY, Transform.IDENTITY);

        // sphere property
        final double radius = 10.;
        final AeroSphereProperty asp = new AeroSphereProperty(radius);
        builder.addProperty(asp, MAIN_BODY);

        // adding mass properties
        final IPartProperty massMainProp = new MassProperty(100.);
        builder.addProperty(massMainProp, MAIN_BODY);

        builder.initMainPartFrame(new UpdatableFrame(FramesFactory.getGCRF(), Transform.IDENTITY, "MainFrame"));

        // assembly creation
        this.testAssembly = builder.returnAssembly();
    }

    /**
     * Creates a new Assembly with only a sphere as main, and specifying the Cx coefficient,
     * and sets testAssembly with it.
     * 
     * @param cx
     *        drag force coefficient parameter
     * @throws PatriusException
     */
    private void createTestAssemblySphereOnlyWithCx(final Parameter cx) throws PatriusException {
        final AssemblyBuilder builder = new AssemblyBuilder();

        // add main part (one sphere)
        builder.addMainPart(MAIN_BODY);

        // sphere property
        final double radius = 10.;
        final Sphere sphere = new Sphere(Vector3D.ZERO, radius);
        final AeroCrossSectionProperty asp = new AeroCrossSectionProperty(sphere, cx);
        builder.addProperty(asp, MAIN_BODY);

        // adding mass properties
        final String massPart = "mass";
        builder.addPart(massPart, MAIN_BODY, Transform.IDENTITY);
        final IPartProperty massMainProp = new MassProperty(100.);
        builder.addProperty(massMainProp, massPart);

        builder.initMainPartFrame(new UpdatableFrame(FramesFactory.getGCRF(), Transform.IDENTITY, "MainFrame"));

        // assembly creation
        this.testAssembly = builder.returnAssembly();
    }

    /**
     * Creates a new Assembly with only a facet as main,
     * and sets testAssembly with it.
     * 
     * @param facetNormal
     *        the normal of the facet in the spacecraft frame.
     * @param cn
     *        normal force coefficient parameter
     * @param ct
     *        tangential force coefficient parameter
     * @throws PatriusException
     */
    private void createTestAssemblyFacetOnly(final Vector3D facetNormal,
                                             final Parameter cn,
                                             final Parameter ct) throws PatriusException {
        final AssemblyBuilder builder = new AssemblyBuilder();

        // add main part (one facet)
        final String facet = "facet";
        builder.addMainPart(MAIN_BODY);
        builder.addPart(facet, MAIN_BODY, Transform.IDENTITY);

        // Facet
        final Vector3D normal = facetNormal;
        final double area = 10.;
        final Facet inFacet = new Facet(normal, area);

        // facet property
        final AeroFacetProperty afp = new AeroFacetProperty(inFacet, cn, ct);
        builder.addProperty(afp, facet);

        // adding mass properties
        final IPartProperty massMainProp = new MassProperty(100.);
        builder.addProperty(massMainProp, MAIN_BODY);

        builder.initMainPartFrame(new UpdatableFrame(FramesFactory.getGCRF(), Transform.IDENTITY, "MainFrame"));

        // assembly creation
        this.testAssembly = builder.returnAssembly();
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#AERO_MODEL}
     * 
     * @testedMethod {@link AeroModel#AeroModel(Assembly)}
     * 
     * @description Test for the AeroModel constructor
     * 
     * @input an Assembly
     * 
     * @output an AeroModel instance
     * 
     * @testPassCriteria the instance creation succeeds
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 2.3
     */
    @Test
    public void testAeroModel() throws PatriusException {
        // create the assembly
        this.createValidTestAssembly();

        // create an AeroModel for the assembly
        final AeroModel aeroModel = new AeroModel(this.testAssembly);

        // Should never fail
        Assert.assertNotNull(aeroModel);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#AERO_MODEL}
     * 
     * @testedMethod {@link AeroModel#AeroModel(Assembly)}
     * 
     * @description Error test for the AeroModel constructor : missing properties for a valid aero model
     * 
     * @input an Assembly
     * 
     * @output none
     * 
     * @testPassCriteria an IllegalArgumentException is raised
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test(expected = IllegalArgumentException.class)
    public void testAeroModelError() throws PatriusException {
        // create the assembly
        this.createInvalidTestAssembly();

        // create an AeroModel for the assembly
        final AeroModel aeroModel = new AeroModel(this.testAssembly);

        // Should never reach here
        Assert.fail(aeroModel.toString() + " is valid but should not be");
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#AERO_MODEL}
     * 
     * @testedMethod {@link AeroModel#AeroModel(Assembly)}
     * 
     * @description Second error test for the AeroModel constructor : excess of properties for an aero model
     * 
     * @input an Assembly
     * 
     * @output none
     * 
     * @testPassCriteria an IllegalArgumentException is raised
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test(expected = IllegalArgumentException.class)
    public void testAeroModelError2() throws PatriusException {
        // create the assembly
        this.createInvalidTestAssembly2();

        // create an AeroModel for the assembly
        final AeroModel aeroModel = new AeroModel(this.testAssembly);

        // Should never reach here
        Assert.fail(aeroModel.toString() + " is valid but should not be");
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#AERO_MODEL}
     * 
     * @testedMethod {@link AeroModel#dragAcceleration(SpacecraftState, double, Vector3D)}
     * 
     * @description Test for the dragAcceleration method (model with a sphere only)
     * 
     * @input an Assembly
     * 
     * @output an AeroModel instance and an acceleration vector
     * 
     * @testPassCriteria the acceleration vector is as expected
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public void testDragAccelerationSphereOnly() throws PatriusException {

        Report.printMethodHeader("testDragAccelerationSphereOnly", "Acceleration computation (sphere)", "Math",
            Precision.DOUBLE_COMPARISON_EPSILON, ComparisonType.ABSOLUTE);

        // create the assembly
        this.createTestAssemblySphereOnly();

        // create an AeroModel for the assembly
        final AeroModel aeroModel = new AeroModel(this.testAssembly);

        // orbit
        final double mu = Constants.EGM96_EARTH_MU;
        final AbsoluteDate date = new AbsoluteDate(2012, 4, 2, 15, 3, 0, TimeScalesFactory.getTAI());
        final double a = 10000000;
        final Orbit testOrbit = new KeplerianOrbit(a, 0.93, MathLib.toRadians(75), 0, 0, 0, PositionAngle.MEAN,
            FramesFactory.getGCRF(), date, mu);
        final Attitude attitude = new LofOffset(testOrbit.getFrame(), LOFType.LVLH).getAttitude(testOrbit,
            testOrbit.getDate(), testOrbit.getFrame());
        final SpacecraftState bogusState = new SpacecraftState(testOrbit, attitude, new MassModel(this.testAssembly));
        final SpacecraftState shiftedBogusState = bogusState.shiftedBy(10.);
        final double testDensity = 0.1234;
        // rotation from the reference frame to the spacecraft frame
        final Rotation toSatRotation = shiftedBogusState.getAttitude().getRotation();

        // Test case : relative velocity is ZERO.
        Vector3D testVelocity = Vector3D.ZERO;
        // Call the dragAcceleration method
        Vector3D testAcc = aeroModel.dragAcceleration(shiftedBogusState, testDensity, testVelocity);
        // Expected : acceleration is ZERO
        Assert.assertEquals(Vector3D.ZERO, testAcc);

        // Test case : relative velocity is a multiplied unit vector
        // in the satellite frame.
        testVelocity = toSatRotation.applyInverseTo(Vector3D.PLUS_I.scalarMultiply(3.));
        // Call the dragAcceleration method
        testAcc = aeroModel.dragAcceleration(shiftedBogusState, testDensity, testVelocity);
        // Expected : acceleration is along the velocity
        Assert.assertEquals(0., Vector3D.angle(testVelocity, testAcc), Precision.DOUBLE_COMPARISON_EPSILON);
        // Expected : in the satellite frame, acceleration is along the same unit vector
        Vector3D testAccSat = toSatRotation.applyTo(testAcc);
        Assert.assertEquals(0., Vector3D.angle(Vector3D.PLUS_I, testAccSat), Precision.DOUBLE_COMPARISON_EPSILON);
        // Expected : norm as computed
        // Remember : sphere radius is 10; spacecraft mass is 100 ; velocity is 3
        double expectedNorm = (0.5 * testDensity * 3. * 3. * AeroFacetProperty.DEFAULT_C_N * FastMath.PI * 10. * 10.) / 100.;
        Assert.assertEquals(expectedNorm, testAccSat.getNorm(), Precision.DOUBLE_COMPARISON_EPSILON);

        // Test case : relative velocity is a random normalized vector
        // in the satellite frame.
        final Vector3D randomVector = (new Vector3D(MathLib.random(), MathLib.random(), MathLib.random())).normalize();
        testVelocity = toSatRotation.applyInverseTo(randomVector);
        // Call the dragAcceleration method
        testAcc = aeroModel.dragAcceleration(shiftedBogusState, testDensity, testVelocity);
        // Expected : acceleration is along the velocity
        Assert.assertEquals(0., Vector3D.angle(testVelocity, testAcc), Precision.DOUBLE_COMPARISON_EPSILON);
        // Expected : in the satellite frame, acceleration is along the random vector
        testAccSat = toSatRotation.applyTo(testAcc);
        Assert.assertEquals(0., Vector3D.angle(randomVector, testAccSat), Precision.DOUBLE_COMPARISON_EPSILON);
        // Expected : norm as computed
        // Remember : sphere radius is 10; spacecraft mass is 100 ; velocity is 1
        expectedNorm = (0.5 * testDensity * AeroFacetProperty.DEFAULT_C_N * FastMath.PI * 10. * 10.) / 100.;
        Assert.assertEquals(expectedNorm, testAccSat.getNorm(), Precision.DOUBLE_COMPARISON_EPSILON);

        Report.printToReport("Acceleration norm", expectedNorm, testAccSat.getNorm());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#AERO_MODEL}
     * 
     * @testedMethod {@link AeroModel#dragAcceleration(SpacecraftState, double, Vector3D)}
     * 
     * @description Test for the dragAcceleration method : model with a sphere and two solar panels such that
     *              first panel has +I for normal vector, the second has -I
     * 
     * @input an Assembly
     * 
     * @output an AeroModel instance and an acceleration vector
     * 
     * @testPassCriteria the acceleration vector is as expected
     * 
     * @referenceVersion 3.4
     * 
     * @nonRegressionVersion 3.4
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public void testDragAccelerationSphereWithPanels() throws PatriusException {

        Report.printMethodHeader("testDragAccelerationSphereWithPanels",
            "Acceleration computation (sphere with panels)", "Math", Precision.DOUBLE_COMPARISON_EPSILON,
            ComparisonType.ABSOLUTE);

        // create the assembly
        this.createTestAssemblyVehicleSphereWithPanels();

        // create an AeroModel for the assembly
        final AeroModel aeroModel = new AeroModel(this.testAssembly);

        // orbit
        final double mu = Constants.EGM96_EARTH_MU;
        final AbsoluteDate date = new AbsoluteDate(2012, 4, 2, 15, 3, 0, TimeScalesFactory.getTAI());
        final double a = 10000000;
        final Orbit testOrbit = new KeplerianOrbit(a, 0.93, MathLib.toRadians(75), 0, 0, 0, PositionAngle.MEAN,
            FramesFactory.getGCRF(), date, mu);
        // spacecraft
        final Attitude attitude = new LofOffset(testOrbit.getFrame(), LOFType.LVLH).getAttitude(testOrbit,
            testOrbit.getDate(), testOrbit.getFrame());
        final SpacecraftState bogusState = new SpacecraftState(testOrbit, attitude, new MassModel(this.testAssembly));
        final SpacecraftState shiftedBogusState = bogusState.shiftedBy(10.);
        final double testDensity = 0.1234;
        // rotation from the reference frame to the spacecraft frame
        final Rotation toSatRotation = shiftedBogusState.getAttitude().getRotation();

        // Cylinder mass
        final double mass = ((MassProperty) this.testAssembly.getMainPart().getProperty(PropertyType.MASS)).getMass();

        // Test case : relative velocity is ZERO.
        Vector3D testVelocity = Vector3D.ZERO;
        // Call the dragAcceleration method
        Vector3D testAcc = aeroModel.dragAcceleration(shiftedBogusState, testDensity, testVelocity);
        // Expected : acceleration is ZERO
        Assert.assertEquals(Vector3D.ZERO, testAcc);

        // Test case : relative velocity in the backfront of the first facet
        // and front of the second : the only force to be computed for facet
        // is for the one having normal +I (negative orientation wrt velocity)
        testVelocity = toSatRotation.applyTo(Vector3D.MINUS_I);

        // Call the dragAcceleration method
        testAcc = aeroModel.dragAcceleration(shiftedBogusState, testDensity, testVelocity);

        // Expected acceleration : aligned with velocity since force on sphere and force on facet
        // are both aligned with velocity
        Assert.assertEquals(0., Vector3D.angle(testVelocity, testAcc), Precision.DOUBLE_COMPARISON_EPSILON);

        // Test case : relative velocity is PLUS_I + PLUS_K (norm sqrt(2))
        // in the satellite frame.
        // the only force to be computed for facet is for the one having normal
        // -I (negative orientation wrt velocity)
        testVelocity = Vector3D.PLUS_I.add(Vector3D.MINUS_K);
        final double velSatNorm = testVelocity.getNorm();
        testVelocity = toSatRotation.applyTo(testVelocity);

        // Expected : normal and tangent as computed and along the right components
        // Remember : facet area is 10; spacecraft mass is 100
        // Obtain drag components by projection
        final double forceFact = 0.5 * testDensity * velSatNorm * velSatNorm * AeroFacetProperty.DEFAULT_C_N * 100
                * FastMath.PI / velSatNorm;

        // Angle between facet and velocity is 3 PI / 4
        final double expectedNorm = (0.5 * testDensity * velSatNorm * velSatNorm * AeroFacetProperty.DEFAULT_C_N * 10.
                * MathLib.cos(FastMath.PI / 4.) * MathLib.cos(FastMath.PI / 4.) + forceFact)
                / mass;

        // Remember that coeff Ct = 0 for the facet
        final double expectedTang = forceFact / mass;

        // Call the dragAcceleration method
        testAcc = aeroModel.dragAcceleration(shiftedBogusState, testDensity, testVelocity);
        final Vector3D testAccSat = toSatRotation.applyInverseTo(testAcc);
        Assert.assertEquals(expectedNorm, testAccSat.getX(), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(0., testAccSat.getY(), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(-expectedTang, testAccSat.getZ(), Precision.DOUBLE_COMPARISON_EPSILON);

        Report.printToReport("Acceleration norm",
            MathLib.sqrt(expectedNorm * expectedNorm + expectedTang * expectedTang), testAccSat.getNorm());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#AERO_MODEL}
     * 
     * @testedMethod {@link AeroModel#dragAcceleration(SpacecraftState, double, Vector3D)}
     * 
     * @description Test for the dragAcceleration method (model cylinder only, then cylinder with panels).
     *              Solar panels are such that first panel has I + K for normal vector, the second has -I + K for normal
     *              (45° wrt main shape).
     * 
     * @input an Assembly
     * 
     * @output an AeroModel instance and an acceleration vector
     * 
     * @testPassCriteria the acceleration vector is as expected
     * 
     * @referenceVersion 3.4
     * 
     * @nonRegressionVersion 3.4
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public void testDragAccelerationCylinder() throws PatriusException {

        Report.printMethodHeader("testDragAccelerationCylinder", "Acceleration computation (cylinder)", "Math",
            Precision.DOUBLE_COMPARISON_EPSILON, ComparisonType.ABSOLUTE);

        // --------------------------------- Assembly with cylinder only ---------------------- //

        // create the assembly : cylinder only
        this.createTestAssemblyVehicleCylinderOnly();

        // orbit
        final double mu = Constants.EGM96_EARTH_MU;
        final AbsoluteDate date = new AbsoluteDate(2012, 4, 2, 15, 3, 0, TimeScalesFactory.getTAI());
        final double a = 10000000;
        final Orbit testOrbit = new KeplerianOrbit(a, 0.93, MathLib.toRadians(75), 0, 0, 0, PositionAngle.MEAN,
            FramesFactory.getGCRF(), date, mu);
        // spacecraft
        final Attitude attitude = new LofOffset(testOrbit.getFrame(), LOFType.LVLH).getAttitude(testOrbit,
            testOrbit.getDate(), testOrbit.getFrame());
        final SpacecraftState bogusState = new SpacecraftState(testOrbit, attitude, new MassModel(this.testAssembly));
        final SpacecraftState shiftedBogusState = bogusState.shiftedBy(10.);
        final double testDensity = 0.1234;
        // rotation from the reference frame to the spacecraft frame
        final Rotation toSatRotation = shiftedBogusState.getAttitude().getRotation();

        // create an AeroModel for the assembly
        AeroModel aeroModel = new AeroModel(this.testAssembly);

        // Cylinder mass
        final double mass = ((MassProperty) this.testAssembly.getMainPart().getProperty(PropertyType.MASS)).getMass();

        // Test case : relative velocity is PLUS_I
        Vector3D testVelocity = Vector3D.PLUS_I;
        double velSatNorm = testVelocity.getNorm();
        testVelocity = toSatRotation.applyTo(testVelocity);

        // Call the dragAcceleration method
        this.testAssembly.updateMainPartFrame(shiftedBogusState);
        Vector3D testAcc = aeroModel.dragAcceleration(shiftedBogusState, testDensity, testVelocity);

        // Expected acceleration
        double forceFactor = 0.5 * testDensity * velSatNorm * velSatNorm * AeroFacetProperty.DEFAULT_C_N * 2. * 2.
                * 10.;
        Vector3D forceCyl = testVelocity.scalarMultiply(forceFactor / mass);
        Assert.assertEquals(testAcc.distance(forceCyl), 0., Precision.DOUBLE_COMPARISON_EPSILON);

        // Test case : relative velocity is PLUS_I + PLUS_K
        // (cylinder from 45° inclination)

        // Call the dragAcceleration method
        testVelocity = Vector3D.PLUS_I.add(Vector3D.PLUS_K);
        velSatNorm = testVelocity.getNorm();
        testVelocity = toSatRotation.applyTo(testVelocity);
        testAcc = aeroModel.dragAcceleration(shiftedBogusState, testDensity, testVelocity);

        // Expected acceleration
        forceFactor = (0.5 * testDensity * velSatNorm * velSatNorm * AeroFacetProperty.DEFAULT_C_N * (FastMath.PI * 2.
                * 2. * MathLib.sqrt(2.) / 2. + 2. * 2. * 10. * MathLib.sqrt(2.) / 2.))
                / velSatNorm;

        forceCyl = testVelocity.scalarMultiply(forceFactor / mass);
        Assert.assertEquals(testAcc.distance(forceCyl), 0., Precision.DOUBLE_COMPARISON_EPSILON);

        // --------------------------------- Assembly with panels ---------------------- //
        
        // create the assembly : cylinder with panels
        this.createTestAssemblyVehicleCylinderWithPanels();

        // create an AeroModel for the assembly
        aeroModel = new AeroModel(this.testAssembly);

        // Test case : relative velocity is MINUS_I + PLUS_K (norm sqrt(2))
        // in the satellite frame.
        // the only force to be computed for facet is for the one having normal
        // I + K (negative orientation wrt velocity).
        // This force is zero since velocity and facet normal and orthogonal.
        testVelocity = Vector3D.MINUS_I.add(Vector3D.PLUS_K);
        testVelocity = toSatRotation.applyTo(testVelocity);

        // Call the dragAcceleration method
        testAcc = aeroModel.dragAcceleration(shiftedBogusState, testDensity, testVelocity);

        // Expected acceleration : aligned with velocity since force on cylinder and force on facet
        // are both aligned with velocity
        Assert.assertEquals(0., Vector3D.angle(testVelocity, testAcc), Precision.DOUBLE_COMPARISON_EPSILON);

        // Test case : relative velocity is PLUS_I
        // the only force to be computed for facet is for the one having normal
        // -I + K (negative orientation wrt velocity)
        testVelocity = Vector3D.PLUS_I;
        velSatNorm = testVelocity.getNorm();
        testVelocity = toSatRotation.applyTo(testVelocity);

        // Expected : normal and tangent as computed and along the right components
        // Remember : facet area is 10; spacecraft mass is 1000

        // Cylinder contribution
        final double cylinderFact = 0.5 * testDensity * velSatNorm * velSatNorm * AeroFacetProperty.DEFAULT_C_N * 2.
                * 2. * 10.;

        // Facet contribution : compute drag force projection into [-I, +K]
        // Remember that coeff Ct = 0 for the facet
        final double valueForceN = 0.5 * testDensity * velSatNorm * velSatNorm * AeroFacetProperty.DEFAULT_C_N * 10.
                * MathLib.cos(FastMath.PI / 4.) * MathLib.cos(FastMath.PI / 4.);
        final double valueForceT = 0.;

        // X drag component
        final double compIN = valueForceN * MathLib.cos(FastMath.PI / 4.);
        final double compIT = valueForceT * MathLib.sin(FastMath.PI / 4.);
        final double dragX = (compIN + compIT + cylinderFact) / mass;

        // Z drag component
        final double compKN = -valueForceN * MathLib.sin(FastMath.PI / 4.);
        final double compKT = valueForceT * MathLib.cos(FastMath.PI / 4.);
        final double dragZ = (compKN + compKT) / mass;

        // Call the dragAcceleration method
        this.testAssembly.updateMainPartFrame(shiftedBogusState);
        testAcc = aeroModel.dragAcceleration(shiftedBogusState, testDensity, testVelocity);
        final Vector3D testAccSat = toSatRotation.applyInverseTo(testAcc);
        Assert.assertEquals(dragX, testAccSat.getX(), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(0., testAccSat.getY(), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(dragZ, testAccSat.getZ(), Precision.DOUBLE_COMPARISON_EPSILON);

        Report.printToReport("Acceleration norm", MathLib.sqrt(dragX * dragX + dragZ * dragZ), testAccSat.getNorm());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#AERO_MODEL}
     * 
     * @testedMethod {@link AeroModel#dragAcceleration(SpacecraftState, double, Vector3D)}
     * 
     * @description Test for the dragAcceleration method (model with parallelepiped only, then parallelepiped with
     *              panels).
     *              Solar panels are such that first panel has I + K for normal vector, the second has -I + K for normal
     *              (45° wrt main shape).
     * 
     * @input an Assembly
     * 
     * @output an AeroModel instance and an acceleration vector
     * 
     * @testPassCriteria the acceleration vector is as expected
     * 
     * @referenceVersion 3.4
     * 
     * @nonRegressionVersion 3.4
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public void testDragAccelerationParallelepiped() throws PatriusException {

        Report.printMethodHeader("testDragAccelerationParallelepiped", "Acceleration computation (parallelepiped)",
            "Math", Precision.DOUBLE_COMPARISON_EPSILON, ComparisonType.ABSOLUTE);

        // --------------------------------- Assembly with parallelepiped only ---------------------- //

        // create the assembly : parallelepiped only
        this.createTestAssemblyVehicleParal();

        // Parallelepiped mass
        final double mass = ((MassProperty) this.testAssembly.getMainPart().getProperty(PropertyType.MASS)).getMass();

        // create an AeroModel for the assembly
        AeroModel aeroModel = new AeroModel(this.testAssembly);

        // orbit
        final double mu = Constants.EGM96_EARTH_MU;
        final AbsoluteDate date = new AbsoluteDate(2012, 4, 2, 15, 3, 0, TimeScalesFactory.getTAI());
        final double a = 10000000;
        final Orbit testOrbit = new KeplerianOrbit(a, 0.93, MathLib.toRadians(75), 0, 0, 0, PositionAngle.MEAN,
            FramesFactory.getGCRF(), date, mu);
        // spacecraft
        final Attitude attitude = new LofOffset(testOrbit.getFrame(), LOFType.LVLH).getAttitude(testOrbit,
            testOrbit.getDate(), testOrbit.getFrame());
        final SpacecraftState bogusState = new SpacecraftState(testOrbit, attitude, new MassModel(this.testAssembly));
        final SpacecraftState shiftedBogusState = bogusState.shiftedBy(10.);
        final double testDensity = 0.1234;
        // rotation from the reference frame to the spacecraft frame
        final Rotation toSatRotation = shiftedBogusState.getAttitude().getRotation();

        // Test case : relative velocity is PLUS_I
        // in the satellite frame.
        // Parallelepiped cross section only involve one plate of area 2. * 6. (the bigger plate with normal +I)
        Vector3D testVelocity = Vector3D.PLUS_I;
        testVelocity = toSatRotation.applyTo(testVelocity);
        double velSatNorm = testVelocity.getNorm();

        // Call the dragAcceleration method
        this.testAssembly.updateMainPartFrame(shiftedBogusState);
        Vector3D testAcc = aeroModel.dragAcceleration(shiftedBogusState, testDensity, testVelocity);

        // Expected acceleration : aligned with velocity and as expected
        double forceFactor = 0.5 * testDensity * velSatNorm * velSatNorm * AeroFacetProperty.DEFAULT_C_N * 12.;
        Vector3D forceParal = testVelocity.scalarMultiply(forceFactor / mass);
        Assert.assertEquals(testAcc.distance(forceParal), 0., Precision.DOUBLE_COMPARISON_EPSILON);

        // Test case : random direction is Vector3D(2.0, -2.0, 2.0)
        // in the satellite frame.
        testVelocity = new Vector3D(2.0, -2.0, 2.0);
        testVelocity = toSatRotation.applyTo(testVelocity);
        velSatNorm = testVelocity.getNorm();

        // Call the dragAcceleration method
        this.testAssembly.updateMainPartFrame(shiftedBogusState);
        testAcc = aeroModel.dragAcceleration(shiftedBogusState, testDensity, testVelocity);

        // Expected acceleration : aligned with velocity and as expected
        // Cross section is manually computed at 28. / FastMath.sqrt(3.)
        forceFactor = (0.5 * testDensity * velSatNorm * velSatNorm * AeroFacetProperty.DEFAULT_C_N * (28. / MathLib
            .sqrt(3.))) / velSatNorm;
        forceParal = testVelocity.scalarMultiply(forceFactor / mass);
        Assert.assertEquals(testAcc.distance(forceParal), 0., Precision.DOUBLE_COMPARISON_EPSILON);

        // --------------------------------- Assembly with panels ---------------------- //

        // create the assembly
        this.createTestAssemblyVehicleParalWithPanels();

        // create an AeroModel for the assembly
        aeroModel = new AeroModel(this.testAssembly);

        // Test case : relative velocity is MINUS_I + PLUS_K (norm sqrt(2))
        // in the satellite frame.
        // the only force to be computed for facet is for the one having normal
        // -I + K (negative orientation wrt velocity)
        testVelocity = Vector3D.MINUS_I.add(Vector3D.PLUS_K);
        testVelocity = toSatRotation.applyTo(testVelocity);

        // Call the dragAcceleration method
        testAcc = aeroModel.dragAcceleration(shiftedBogusState, testDensity, testVelocity);

        // Expected acceleration : aligned with velocity since force on parallelepiped and force on facet
        // are both aligned with velocity
        Assert.assertEquals(0., Vector3D.angle(testVelocity, testAcc), Precision.DOUBLE_COMPARISON_EPSILON);

        // Test case : relative velocity is PLUS_I
        testVelocity = Vector3D.PLUS_I;
        velSatNorm = testVelocity.getNorm();
        testVelocity = toSatRotation.applyTo(testVelocity);

        // Expected : normal and tangent as computed and along the right components
        // Remember : facet area is 10; spacecraft mass is 1000
        final double paralFact = 0.5 * testDensity * velSatNorm * velSatNorm * AeroFacetProperty.DEFAULT_C_N * 12.;

        // Facet contribution : compute drag force projection into [-I, +K]
        // Remember that coeff Ct = 0 for the facet
        final double valueForceN = 0.5 * testDensity * velSatNorm * velSatNorm * AeroFacetProperty.DEFAULT_C_N * 10.
                * MathLib.cos(FastMath.PI / 4.) * MathLib.cos(FastMath.PI / 4.);
        final double valueForceT = 0.;

        // X drag component
        final double compIN = valueForceN * MathLib.cos(FastMath.PI / 4.);
        final double compIT = valueForceT * MathLib.sin(FastMath.PI / 4.);
        final double dragX = (compIN + compIT + paralFact) / mass;

        // Z drag component
        final double compKN = -valueForceN * MathLib.sin(FastMath.PI / 4.);
        final double compKT = valueForceT * MathLib.cos(FastMath.PI / 4.);
        final double dragZ = (compKN + compKT) / mass;

        // Call the dragAcceleration method
        this.testAssembly.updateMainPartFrame(shiftedBogusState);
        testAcc = aeroModel.dragAcceleration(shiftedBogusState, testDensity, testVelocity);
        final Vector3D testAccSat = toSatRotation.applyInverseTo(testAcc);
        Assert.assertEquals(dragX, testAccSat.getX(), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(0., testAccSat.getY(), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(dragZ, testAccSat.getZ(), Precision.DOUBLE_COMPARISON_EPSILON);

        Report.printToReport("Acceleration norm", MathLib.sqrt(dragX * dragX + dragZ * dragZ), testAccSat.getNorm());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#AERO_MODEL}
     * 
     * @testedMethod {@link AeroModel#dragAcceleration(SpacecraftState, double, Vector3D)}
     * 
     * @description Test for the dragAcceleration method (model with a facet only)
     * 
     * @input an Assembly
     * 
     * @output an AeroModel instance and acceleration vectors
     * 
     * @testPassCriteria the acceleration vectors are as expected
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 2.3
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public void testDragAccelerationFacetOnly() throws PatriusException {

        Report.printMethodHeader("testDragAccelerationFacetOnly", "Acceleration computation (facet)", "Math",
            Precision.DOUBLE_COMPARISON_EPSILON, ComparisonType.ABSOLUTE);

        // create the assemblyv
        final Parameter cn = new Parameter("C_N", 1.7);
        final Parameter ct = new Parameter("C_T", 2.3);
        this.createTestAssemblyFacetOnly(Vector3D.PLUS_I, cn, ct);

        // create an AeroModel for the assembly
        final AeroModel aeroModel = new AeroModel(this.testAssembly);

        // orbit
        final double mu = Constants.EGM96_EARTH_MU;
        final AbsoluteDate date = new AbsoluteDate(2012, 4, 2, 15, 3, 0, TimeScalesFactory.getTAI());
        final double a = 10000000;
        final Orbit testOrbit = new KeplerianOrbit(a, 0.93, MathLib.toRadians(75), 0, 0, 0, PositionAngle.MEAN,
            FramesFactory.getGCRF(), date, mu);
        final Attitude attitude = new LofOffset(testOrbit.getFrame(), LOFType.LVLH).getAttitude(testOrbit,
            testOrbit.getDate(), testOrbit.getFrame());

        // spacecraft
        final SpacecraftState bogusState = new SpacecraftState(testOrbit, attitude, new MassModel(this.testAssembly));
        final SpacecraftState shiftedBogusState = bogusState.shiftedBy(10.);
        final double testDensity = 0.1234;
        // rotation from the reference frame to the spacecraft frame
        final Rotation toSatRotation = shiftedBogusState.getAttitude().getRotation();

        // Test case : relative velocity is ZERO.
        Vector3D testVelocity = Vector3D.ZERO;
        // Call the dragAcceleration method
        Vector3D testAcc = aeroModel.dragAcceleration(shiftedBogusState, testDensity, testVelocity);
        // Expected : acceleration is ZERO
        Assert.assertEquals(Vector3D.ZERO, testAcc);

        // Test case : relative velocity is a multiplied unit vector
        // in the satellite frame, right on the "front" of the facet
        // (normal component only for the resulting force)
        testVelocity = toSatRotation.applyTo(Vector3D.MINUS_I.scalarMultiply(3.));
        // Call the dragAcceleration method
        testAcc = aeroModel.dragAcceleration(shiftedBogusState, testDensity, testVelocity);
        // Expected : acceleration is along the velocity
        Assert.assertEquals(0., Vector3D.angle(testVelocity, testAcc), Precision.DOUBLE_COMPARISON_EPSILON);
        // Expected : in the satellite frame, acceleration is along the same unit vector
        // as the relative velocity
        Vector3D testAccSat = toSatRotation.applyInverseTo(testAcc);
        Assert.assertEquals(0., Vector3D.angle(Vector3D.MINUS_I, testAccSat), Precision.DOUBLE_COMPARISON_EPSILON);
        // Expected : norm as computed
        // Remember : facet area is 10; spacecraft mass is 100 ; velocity is 3
        double expectedNorm = (0.5 * testDensity * 3. * 3. * AeroFacetProperty.DEFAULT_C_N * 10.) / 100.;
        Assert.assertEquals(expectedNorm, testAccSat.getNorm(), Precision.DOUBLE_COMPARISON_EPSILON);

        Report.printToReport("Acceleration norm", expectedNorm, testAccSat.getNorm());

        // Test case : relative velocity is MINUS_I + MINUS_K (norm sqrt(2))
        // in the satellite frame.
        Vector3D testVelocitySat = Vector3D.MINUS_I.add(Vector3D.MINUS_K);
        double velSatNorm = testVelocitySat.getNorm();
        testVelocity = toSatRotation.applyTo(testVelocitySat);
        // Expected : norm and tangent as computed and along the right components
        // Remember : facet area is 10; spacecraft mass is 100 ; velocity is 3
        expectedNorm = (0.5 * testDensity * velSatNorm * velSatNorm * AeroFacetProperty.DEFAULT_C_N * 10.
                * MathLib.cos(FastMath.PI / 4.) * MathLib.cos(FastMath.PI / 4.)) / 100.;
        double expectedTang = (0.5 * testDensity * velSatNorm * velSatNorm * AeroFacetProperty.DEFAULT_C_T * 10.
                * MathLib.sin(FastMath.PI / 4.) * MathLib.cos(FastMath.PI / 4.)) / 100.;
        // Call the dragAcceleration method
        testAcc = aeroModel.dragAcceleration(shiftedBogusState, testDensity, testVelocity);
        testAccSat = toSatRotation.applyInverseTo(testAcc);
        Assert.assertEquals(-expectedNorm, testAccSat.getX(), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(-expectedTang, testAccSat.getZ(), Precision.DOUBLE_COMPARISON_EPSILON);

        // Test case : relative velocity is MINUS_I + MINUS_J + MINUS_K (norm sqrt(3))
        // in the satellite frame.
        testVelocitySat = Vector3D.MINUS_I.add(Vector3D.MINUS_J).add(Vector3D.MINUS_K);
        velSatNorm = testVelocitySat.getNorm();
        testVelocity = toSatRotation.applyTo(testVelocitySat);
        // Expected : norm and tangent as computed and along the right components
        // Remember : facet area is 10; spacecraft mass is 100 ; velocity is 3
        final double tangAng = Vector3D.angle(Vector3D.PLUS_I, testVelocitySat);
        expectedNorm = (0.5 * testDensity * velSatNorm * velSatNorm * AeroFacetProperty.DEFAULT_C_N * 10.
                * MathLib.cos(tangAng) * MathLib.cos(tangAng)) / 100.;
        expectedTang = (0.5 * testDensity * velSatNorm * velSatNorm * AeroFacetProperty.DEFAULT_C_T * 10.
                * MathLib.sin(tangAng) * MathLib.cos(tangAng)) / 100.;
        expectedTang = MathLib.abs(expectedTang);
        // Call the dragAcceleration method
        testAcc = aeroModel.dragAcceleration(shiftedBogusState, testDensity, testVelocity);
        testAccSat = toSatRotation.applyInverseTo(testAcc);
        final Vector3D accSatYZ = new Vector3D(0., testAccSat.getY(), testAccSat.getZ());
        Assert.assertEquals(-expectedNorm, testAccSat.getX(), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(expectedTang, accSatYZ.getNorm(), Precision.DOUBLE_COMPARISON_EPSILON);

        // Test case : relative velocity is MINUS_I + PLUS_K (norm sqrt(2))
        // in the satellite frame.
        testVelocitySat = Vector3D.MINUS_I.add(Vector3D.PLUS_K);
        velSatNorm = testVelocitySat.getNorm();
        testVelocity = toSatRotation.applyTo(testVelocitySat);
        // Expected : normal and tangent as computed and along the right components
        // Remember : facet area is 10; spacecraft mass is 100 ; velocity is 3
        expectedNorm = (0.5 * testDensity * velSatNorm * velSatNorm * AeroFacetProperty.DEFAULT_C_N * 10.
                * MathLib.cos(FastMath.PI / 4.) * MathLib.cos(FastMath.PI / 4.)) / 100.;
        expectedTang = (0.5 * testDensity * velSatNorm * velSatNorm * AeroFacetProperty.DEFAULT_C_T * 10.
                * MathLib.sin(FastMath.PI / 4.) * MathLib.cos(FastMath.PI / 4.)) / 100.;
        // Call the dragAcceleration method
        testAcc = aeroModel.dragAcceleration(shiftedBogusState, testDensity, testVelocity);
        testAccSat = toSatRotation.applyInverseTo(testAcc);
        Assert.assertEquals(-expectedNorm, testAccSat.getX(), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(expectedTang, testAccSat.getZ(), Precision.DOUBLE_COMPARISON_EPSILON);

        // Test case : relative velocity is PLUS_K
        // in the satellite frame.
        testVelocitySat = Vector3D.PLUS_K;
        velSatNorm = testVelocitySat.getNorm();
        testVelocity = toSatRotation.applyTo(testVelocitySat);
        // Expected : acceleration is exactly Vector3D.ZERO, given the angle with the normal (Pi/2)
        // Call the dragAcceleration method
        testAcc = aeroModel.dragAcceleration(shiftedBogusState, testDensity, testVelocity);
        testAccSat = toSatRotation.applyInverseTo(testAcc);
        Assert.assertEquals(Vector3D.ZERO, testAcc);
        Assert.assertEquals(testAccSat.getX(), 0.0, Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(testAccSat.getY(), 0.0, Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(testAccSat.getZ(), 0.0, Precision.DOUBLE_COMPARISON_EPSILON);

        // Test case : relative velocity is PLUS_I
        // in the satellite frame.
        testVelocitySat = Vector3D.PLUS_I;
        velSatNorm = testVelocitySat.getNorm();
        testVelocity = toSatRotation.applyTo(testVelocitySat);
        // Expected : acceleration is exactly Vector3D.ZERO, given the angle with the normal (0)
        // which makes a velocity from the "inside".
        // Call the dragAcceleration method
        testAcc = aeroModel.dragAcceleration(shiftedBogusState, testDensity, testVelocity);
        testAccSat = toSatRotation.applyInverseTo(testAcc);
        Assert.assertEquals(Vector3D.ZERO, testAcc);
        Assert.assertEquals(testAccSat.getX(), 0.0, Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(testAccSat.getY(), 0.0, Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(testAccSat.getZ(), 0.0, Precision.DOUBLE_COMPARISON_EPSILON);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#AERO_MODEL}
     * 
     * @testedMethod {@link AeroModel#dragAcceleration(SpacecraftState, double, Vector3D)}
     * 
     * @description Test for the dragAcceleration method (model with a spacecraft defined the same way as
     *              the orekit's SphericalSpacecraft)
     * 
     * @input an Assembly with a spherical body
     * 
     * @output an AeroModel instance and acceleration vectors
     * 
     * @testPassCriteria the acceleration vectors are as expected
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 2.3
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public void comparisonToSphericalSpacecraft() throws PatriusException {

        Report.printMethodHeader("comparisonToSphericalSpacecraft", "Acceleration computation (sphere)", "Orekit",
            this.comparisonEpsilon, ComparisonType.ABSOLUTE);

        // parameters
        final double crossSection = 10.0;
        final double dragCoeff = 0.1056;
        final double density = 1.2e-7;
        final Vector3D relativeVelocity = new Vector3D(100.0, 2300.0, -20.0);

        // model
        final AssemblySphericalSpacecraft siriusModel = new AssemblySphericalSpacecraft(crossSection, dragCoeff, 0.5,
            0.5, 0., "Main");

        // orbit
        final double mu = Constants.EGM96_EARTH_MU;
        final AbsoluteDate date = new AbsoluteDate(2012, 4, 2, 15, 3, 0, TimeScalesFactory.getTAI());
        final double a = 10000000;
        final Orbit testOrbit = new KeplerianOrbit(a, 0.93, MathLib.toRadians(75), 0, 0, 0, PositionAngle.MEAN,
            FramesFactory.getGCRF(), date, mu);
        final Attitude attitude = new LofOffset(testOrbit.getFrame(), LOFType.LVLH).getAttitude(testOrbit,
            testOrbit.getDate(), testOrbit.getFrame());
        massModel = new MassModel(siriusModel.getAssembly());
        final SpacecraftState state = new SpacecraftState(testOrbit, attitude, massModel);

        siriusModel.getAssembly().initMainPartFrame(state);

        final Vector3D siriusAcc = siriusModel.dragAcceleration(state, density, relativeVelocity);

        // Expected orekit results
        // final DragSensitive orekitModel = new SphericalSpacecraft(crossSection, dragCoeff, 0.5, 0.5, 0.);
        // final Vector3D orekitAcc = orekitModel.dragAcceleration(state, density, relativeVelocity);
        Assert.assertEquals(1.4587117835898905E-5, siriusAcc.getX(), this.comparisonEpsilon);
        Assert.assertEquals(3.3550371022567484E-4, siriusAcc.getY(), this.comparisonEpsilon);
        Assert.assertEquals(-2.9174235671797813E-6, siriusAcc.getZ(), this.comparisonEpsilon);

        Report.printToReport("Acceleration", new Vector3D(1.4587117835898905E-5, 3.3550371022567484E-4,
            -2.9174235671797813E-6), siriusAcc);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#AERO_MODEL}
     * 
     * @testedMethod {@link AeroModel#dragAcceleration(SpacecraftState, double, Vector3D)}
     * 
     * @description Test for the dragAcceleration method (model with a spacecraft defined the same way as
     *              the orekit's BoxAndSolarArraySpacecraft)
     * 
     * @input an Assembly with a "box and solar array" body
     * 
     * @output an AeroModel instance and acceleration vectors
     * 
     * @testPassCriteria the acceleration vectors are as expected
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 2.3
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public void comparisonToBoxAndSASpacecraft() throws PatriusException {

        Report.printMethodHeader("comparisonToBoxAndSASpacecraft", "Acceleration computation (box and array)",
            "Orekit", this.comparisonEpsilon, ComparisonType.ABSOLUTE);

        // parameters
        final double xLength = 0.8;
        final double yLength = 2.1;
        final double zLength = 1.2;
        final double solarArrayArea = 8.0;
        final Vector3D solarArrayAxis = Vector3D.PLUS_J;
        final double dragCoeff = 0.1056;
        final double density = 1.2e-7;
        final PVCoordinatesProvider sun = new ConstantPVCoordinatesProvider(new PVCoordinates(new Vector3D(1.0e+11,
            0.7e+11, 0.), Vector3D.ZERO), FramesFactory.getGCRF());

        // models
        // final DragSensitive orekitModel =
        // new BoxAndSolarArraySpacecraft(xLength, yLength, zLength,
        // sun, solarArrayArea, solarArrayAxis, dragCoeff, 0.5, 0.5, 0.);
        final AssemblyBoxAndSolarArraySpacecraft siriusModel = new AssemblyBoxAndSolarArraySpacecraft(xLength, yLength,
            zLength, sun, solarArrayArea, solarArrayAxis, dragCoeff, 0.5, 0.5, 0.);

        // orbit
        final double mu = Constants.EGM96_EARTH_MU;
        final double a = 10000000;
        final AbsoluteDate date = new AbsoluteDate(2012, 4, 2, 15, 3, 0, TimeScalesFactory.getTAI());
        final Orbit testOrbit = new KeplerianOrbit(a, 0.93, MathLib.toRadians(75), 0, 0, 0, PositionAngle.MEAN,
            FramesFactory.getGCRF(), date, mu);
        final Attitude attitude = new LofOffset(testOrbit.getFrame(), LOFType.LVLH).getAttitude(testOrbit,
            testOrbit.getDate(), testOrbit.getFrame());
        final SpacecraftState state = new SpacecraftState(testOrbit, attitude, new MassModel(siriusModel.getAssembly()));

        siriusModel.getAssembly().initMainPartFrame(state);

        // tests with different relative velocities
        // 1
        Vector3D relativeVelocity = new Vector3D(100.0, 2300.0, -20.0);

        Vector3D siriusAcc = siriusModel.dragAcceleration(state, density, relativeVelocity);
        // Vector3D orekitAcc = orekitModel.dragAcceleration(state, density, relativeVelocity);
        Assert.assertEquals(3.3997552390203635E-6, siriusAcc.getX(), this.comparisonEpsilon);
        Assert.assertEquals(7.819437049746836E-5, siriusAcc.getY(), this.comparisonEpsilon);
        Assert.assertEquals(-6.799510478040727E-7, siriusAcc.getZ(), this.comparisonEpsilon);

        Report.printToReport("Acceleration", new Vector3D(3.3997552390203635E-6, 7.819437049746836E-5,
            -6.799510478040727E-7), siriusAcc);

        // 2
        relativeVelocity = new Vector3D(-50.0, 0.0, 285550.0);

        siriusAcc = siriusModel.dragAcceleration(state, density, relativeVelocity);
        // orekitAcc = orekitModel.dragAcceleration(state, density, relativeVelocity);
        Assert.assertEquals(-3.671942941363731E-4, siriusAcc.getX(), this.comparisonEpsilon);
        Assert.assertEquals(0.0, siriusAcc.getY(), this.comparisonEpsilon);
        Assert.assertEquals(2.0970466138128265, siriusAcc.getZ(), this.comparisonEpsilon);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#AERO_MODEL}
     * @testedFeature {@link features#DRAG_PARTIAL_DERIVATIVES}
     * 
     * @testedMethod {@link AeroModel#addDDragAccDParam(SpacecraftState, double[], double)}
     * @testedMethod {@link DragForce#addDAccDParam(SpacecraftState, String, double[])}
     * 
     * @description Test for the addDDragAccDParam method for the sphere
     * 
     * @input an Assembly (a sphere)
     * 
     * @output the partial derivatives with respect to the ballistic coefficient
     * 
     * @testPassCriteria the partial derivatives are the expected one
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.1
     */
    @Test
    public void testDDragAccDParamSphere() throws PatriusException {
        // create the assembly
        final Parameter cx = new Parameter("C_X", AeroCrossSectionProperty.DEFAULT_C_X);
        this.createTestAssemblySphereOnlyWithCx(cx);

        // create an AeroModel for the assembly
        final AeroModel aeroModel = new AeroModel(this.testAssembly);

        // this class represents an atmosphere with constant density:
        class ConstantAtmosphere implements Atmosphere {
            /** Serializable UID. */
            private static final long serialVersionUID = -3368953919646428640L;

            @Override
            public double getDensity(final AbsoluteDate date, final Vector3D position, final Frame frame) {
                return 1E-10;
            }

            @Override
            public Vector3D getVelocity(final AbsoluteDate date, final Vector3D position, final Frame frame) {
                return Vector3D.MINUS_I;
            }

            @Override
            public double getSpeedOfSound(final AbsoluteDate date, final Vector3D position, final Frame frame) {
                return 0;
            }

            @Override
            public Atmosphere copy() {
                return null;
            }

            @Override
            public void checkSolarActivityData(final AbsoluteDate start, final AbsoluteDate end) {
                // Nothing to do
            }
        }

        // spacecraft
        final AbsoluteDate date = new AbsoluteDate();
        // mu from grim4s4_gr model
        final double mu = 0.39860043770442e+15;
        // GCRF reference frame
        final Frame referenceFrame = FramesFactory.getGCRF();
        // pos-vel
        final Vector3D pos = new Vector3D(4.05e+07, -1.18e+07, -6.59e+05);
        final Vector3D vel = new Vector3D(8.57e+02, 2.95e+03, -4.07e+01);
        final PVCoordinates pvCoordinates = new PVCoordinates(pos, vel);
        final Orbit orbit = new CartesianOrbit(pvCoordinates, referenceFrame, date, mu);
        final Attitude attitude = new LofOffset(orbit.getFrame(), LOFType.LVLH).getAttitude(orbit, orbit.getDate(),
            orbit.getFrame());
        final SpacecraftState state = new SpacecraftState(orbit, attitude);
        this.testAssembly.initMainPartFrame(state);
        final double[] dAccdBc = new double[3];

        cx.setValue(12.0);

        aeroModel.addDDragAccDParam(state, cx, 1E-10, Vector3D.MINUS_I, dAccdBc);
        // Compute acc
        Vector3D acc = aeroModel.dragAcceleration(state, 1E-10, Vector3D.MINUS_I);
        Assert.assertEquals(acc.getX() / cx.getValue(), dAccdBc[0], 0.0);
        Assert.assertEquals(acc.getY() / cx.getValue(), dAccdBc[1], 0.0);
        Assert.assertEquals(acc.getZ() / cx.getValue(), dAccdBc[2], 0.0);

        // test the DragForce method:
        final ConstantAtmosphere atm = new ConstantAtmosphere();
        final DragForce drag = new DragForce(atm, aeroModel);
        final double[] dAccdParam = new double[3];
        drag.addDAccDParam(state, cx, dAccdParam);
        // Compute acc
        acc = drag.computeAcceleration(state);
        Assert.assertEquals(acc.getX() / cx.getValue(), dAccdParam[0], 0.0);
        Assert.assertEquals(acc.getY() / cx.getValue(), dAccdParam[1], 0.0);
        Assert.assertEquals(acc.getZ() / cx.getValue(), dAccdParam[2], 1e-14);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#AERO_MODEL}
     * @testedFeature {@link features#DRAG_PARTIAL_DERIVATIVES}
     * 
     * @testedMethod {@link AeroModel#addDDragAccDParam(SpacecraftState, double[], double)}
     * @testedMethod {@link DragForce#addDAccDParam(SpacecraftState, String, double[])}
     * 
     * @description Test for the addDDragAccDParam method for the assembly with facets
     * 
     * @input an Assembly with facets
     * 
     * @output the partial derivatives with respect to the ballistic coefficient
     * 
     * @testPassCriteria the partial derivatives are the expected one
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testDDragAccDParamFacet() throws PatriusException {
        // create the assembly
        final Parameter cn = new Parameter("C_N", 1.7);
        final Parameter ct = new Parameter("C_T", 2.3);
        this.createTestAssemblyFacetOnly(Vector3D.PLUS_J, cn, ct);

        // create an AeroModel for the assembly
        final AeroModel aeroModel = new AeroModel(this.testAssembly);

        // this class represents an atmosphere with constant density:
        class ConstantAtmosphere implements Atmosphere {
            /** Serializable UID. */
            private static final long serialVersionUID = 5075225392373123156L;

            @Override
            public double getDensity(final AbsoluteDate date, final Vector3D position, final Frame frame) {
                return 1E-10;
            }

            @Override
            public Vector3D getVelocity(final AbsoluteDate date, final Vector3D position, final Frame frame) {
                return Vector3D.PLUS_I.scalarMultiply(1000.);
            }

            @Override
            public double getSpeedOfSound(final AbsoluteDate date, final Vector3D position, final Frame frame) {
                return 0;
            }

            @Override
            public Atmosphere copy() {
                return null;
            }

            @Override
            public void checkSolarActivityData(final AbsoluteDate start, final AbsoluteDate end) {
                // Nothing to do
            }
        }

        // spacecraft
        final AbsoluteDate date = new AbsoluteDate();
        // mu from grim4s4_gr model
        final double mu = 0.39860043770442e+15;
        // GCRF reference frame
        final Frame referenceFrame = FramesFactory.getGCRF();
        // pos-vel
        final Vector3D pos = new Vector3D(4.05e+07, -1.18e+07, -6.59e+05);
        final Vector3D vel = new Vector3D(8.57e+02, 2.95e+03, -4.07e+01);
        final PVCoordinates pvCoordinates = new PVCoordinates(pos, vel);
        final Orbit orbit = new CartesianOrbit(pvCoordinates, referenceFrame, date, mu);
        final Attitude attitude = new LofOffset(orbit.getFrame(), LOFType.LVLH).getAttitude(orbit, orbit.getDate(),
            orbit.getFrame());
        final SpacecraftState state = new SpacecraftState(orbit, attitude);
        this.testAssembly.initMainPartFrame(state);

        final ConstantAtmosphere atm = new ConstantAtmosphere();
        final double density = atm.getDensity(date, state.getPVCoordinates().getPosition(), state.getFrame());
        final Vector3D vAtm = atm
            .getVelocity(state.getDate(), state.getPVCoordinates().getPosition(), state.getFrame());
        final Vector3D relativeVelocity = vAtm.subtract(state.getPVCoordinates().getVelocity());

        // ZOOM reference
        final double[] dAccdBc = new double[3];
        aeroModel.addDDragAccDParam(state, cn, density, relativeVelocity, dAccdBc);
        Assert.assertEquals(3.271678626988056E-11, dAccdBc[0], Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(1.4623329641762308E-11, dAccdBc[1], Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(1.748823299176588E-9, dAccdBc[2], Precision.DOUBLE_COMPARISON_EPSILON);

        Arrays.fill(dAccdBc, 0.0);
        aeroModel.addDDragAccDParam(state, ct, density, relativeVelocity, dAccdBc);
        Assert.assertEquals(1.334061397127886E-8, dAccdBc[0], Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(-2.7589802007627713E-7, dAccdBc[1], Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(2.057432377971944E-9, dAccdBc[2], Precision.DOUBLE_COMPARISON_EPSILON);

        // test the DragForce method:
        final DragForce drag = new DragForce(atm, aeroModel);
        final double[] dAccdParam = new double[3];
        drag.addDAccDParam(state, cn, dAccdParam);
        Assert.assertEquals(3.271678626988056E-11, dAccdParam[0], Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(1.4623329641762308E-11, dAccdParam[1], Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(1.748823299176588E-9, dAccdParam[2], Precision.DOUBLE_COMPARISON_EPSILON);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#AERO_MODEL}
     * @testedFeature {@link features#DRAG_PARTIAL_DERIVATIVES}
     * 
     * @testedMethod {@link AeroModel#addDDragAccDParam(SpacecraftState, double[], double)}
     * @testedMethod {@link DragForce#addDAccDParam(SpacecraftState, String, double[])}
     * 
     * @description Test for the addDDragAccDParam method
     * 
     * @input an Assembly with one body (sphere) and one facet
     * 
     * @output the partial derivatives with respect of jacobians parameters
     * 
     * @testPassCriteria the partial derivatives are the expected one
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.1
     */
    @Test
    public void testDDragAccDParam() throws PatriusException {
        // create the assembly
        final AssemblyBuilder builder = new AssemblyBuilder();

        // add main part (one sphere) and part2 (one facet)
        builder.addMainPart(MAIN_BODY);
        builder.addPart(PART1, MAIN_BODY, Transform.IDENTITY);

        // one facet
        final Vector3D normal = Vector3D.PLUS_J;
        final double area = 10.;
        final Facet facet = new Facet(normal, area);

        // IParamDiffFucntion for C_N and C_T
        final Parameter an = new Parameter("an", -1);
        final Parameter bn = new Parameter("C_N", 1);
        final Parameter bt = new Parameter("C_T", -2);

        final AbsoluteDate t0 = new AbsoluteDate().shiftedBy(-0.125);

        final LinearFunction cn = new LinearFunction(t0, bn, an);
        final LinearFunction ct = new LinearFunction(t0, bt, an);

        final IPartProperty aeroFacetProp = new AeroFacetProperty(facet, cn, ct);
        // adding aero properties
        builder.addProperty(aeroFacetProp, PART1);

        // sphere property
        final double radius = 10.;

        // linear piecewize Function for C_X
        final Parameter b1 = new Parameter("C_X", 1);
        final Parameter a2 = new Parameter("a2", -2);
        final Parameter a3 = new Parameter("a3", -3);
        final Parameter b3 = new Parameter("b3", 3);

        // f1 = a1.t + b1
        final AbsoluteDate t1 = new AbsoluteDate().shiftedBy(+0.125);
        final LinearFunction f1 = new LinearFunction(t1, b1, an);

        // f2 = a2
        final AbsoluteDate t2 = t1.shiftedBy(10.0);
        final ConstantFunction f2 = new ConstantFunction(a2);

        // f3 = a3.t + b3
        final AbsoluteDate t3 = t1.shiftedBy(15.0);
        final LinearFunction f3 = new LinearFunction(t3, b3, a3);

        final ArrayList<AbsoluteDate> listDate = new ArrayList<>();
        listDate.add(t1);
        listDate.add(t2);

        final ArrayList<IParamDiffFunction> listFct = new ArrayList<>();
        listFct.add(f1);
        listFct.add(f2);
        listFct.add(f3);

        final PiecewiseFunction pwf = new PiecewiseFunction(listFct, listDate);

        final IPartProperty spherePropDiff = new AeroSphereProperty(radius, pwf);
        builder.addProperty(spherePropDiff, MAIN_BODY);

        // adding mass properties
        final IPartProperty massMainProp = new MassProperty(100.);
        builder.addProperty(massMainProp, MAIN_BODY);
        final IPartProperty part2Prop = new MassProperty(10.);
        builder.addProperty(part2Prop, PART1);

        // assembly creation
        this.testAssembly = builder.returnAssembly();

        // create an AeroModel for the assembly
        final AeroModel aeroModel = new AeroModel(this.testAssembly);

        // this class represents an atmosphere with constant density:
        class ConstantAtmosphere implements Atmosphere {
            /** Serializable UID. */
            private static final long serialVersionUID = 6448735329222575200L;

            @Override
            public double getDensity(final AbsoluteDate date, final Vector3D position, final Frame frame) {
                return 1E-10;
            }

            @Override
            public Vector3D getVelocity(final AbsoluteDate date, final Vector3D position, final Frame frame) {
                return Vector3D.PLUS_I.scalarMultiply(1000.);
            }

            @Override
            public double getSpeedOfSound(final AbsoluteDate date, final Vector3D position, final Frame frame) {
                return 0;
            }

            @Override
            public Atmosphere copy() {
                return null;
            }

            @Override
            public void checkSolarActivityData(final AbsoluteDate start, final AbsoluteDate end) {
                // Nothing to do
            }
        }

        // spacecraft
        final AbsoluteDate date = new AbsoluteDate();
        // mu from grim4s4_gr model
        final double mu = 0.39860043770442e+15;
        // GCRF reference frame
        final Frame referenceFrame = FramesFactory.getGCRF();
        // pos-vel
        final Vector3D pos = new Vector3D(4.05e+07, -1.18e+07, -6.59e+05);
        final Vector3D vel = new Vector3D(8.57e+02, 2.95e+03, -4.07e+01);
        final PVCoordinates pvCoordinates = new PVCoordinates(pos, vel);
        final Orbit orbit = new CartesianOrbit(pvCoordinates, referenceFrame, date, mu);
        final Attitude attitude = new LofOffset(orbit.getFrame(), LOFType.LVLH).getAttitude(orbit, orbit.getDate(),
            orbit.getFrame());
        final SpacecraftState state = new SpacecraftState(orbit, attitude);

        this.testAssembly.initMainPartFrame(state);

        final ConstantAtmosphere atm = new ConstantAtmosphere();
        final double density = atm.getDensity(date, state.getPVCoordinates().getPosition(), state.getFrame());
        final Vector3D vAtm = atm
            .getVelocity(state.getDate(), state.getPVCoordinates().getPosition(), state.getFrame());
        final Vector3D relativeVelocity = vAtm.subtract(state.getPVCoordinates().getVelocity());

        // test the DragForce method:
        final DragForce drag = new DragForce(atm, aeroModel);
        final double[] dAccdParam = new double[3];

        // Getting all the parameter
        final ArrayList<Parameter> listParams = aeroModel.getParameters();
        for (final Parameter p : listParams) {
            drag.addDAccDParam(state, p, dAccdParam);
        }
        // Non regression results
        Assert.assertEquals(5.278909826967003E-5, dAccdParam[0], Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(-0.0010890057950373316, dAccdParam[1], Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(1.502478717929402E-5, dAccdParam[2], Precision.DOUBLE_COMPARISON_EPSILON);

        // Restoring default value
        b1.setValue(AeroCrossSectionProperty.DEFAULT_C_X);
        bn.setValue(AeroFacetProperty.DEFAULT_C_N);
        bt.setValue(AeroFacetProperty.DEFAULT_C_T);

        an.setValue(0.);
        b3.setValue(0.);
        a2.setValue(0.);
        a3.setValue(0.);

        // diff with results of testDDragAccDParamFacet
        final double[] dAccdBc = new double[3];
        aeroModel.addDDragAccDParam(state, bn, density, relativeVelocity, dAccdBc);
        Assert.assertEquals(2.974253297261864E-11, dAccdBc[0], Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(1.3293936037965791E-11, dAccdBc[1], Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(1.589839362887807E-9, dAccdBc[2], Precision.DOUBLE_COMPARISON_EPSILON);

        Arrays.fill(dAccdBc, 0.0);
        aeroModel.addDDragAccDParam(state, bt, density, relativeVelocity, dAccdBc);
        Assert.assertEquals(1.2127830882980741E-8, dAccdBc[0], Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(-2.5081638188752464E-7, dAccdBc[1], Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(1.870393070883594E-9, dAccdBc[2], Precision.DOUBLE_COMPARISON_EPSILON);

        // diff with the results of testDDragAccDParamSphere (f1 function (of Cx as piecewize function) is concerned)
        Arrays.fill(dAccdParam, 0.0);
        bn.setValue(0.);
        bt.setValue(0.);
        drag.addDAccDParam(state, b1, dAccdParam);

        // Compute acc
        final Vector3D acc = drag.computeAcceleration(state);
        final Vector3D expectedDP = acc.scalarMultiply(1. / b1.getValue());
        Assert.assertEquals(expectedDP.getX(), dAccdParam[0], Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(expectedDP.getY(), dAccdParam[1], Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(expectedDP.getZ(), dAccdParam[2], Precision.DOUBLE_COMPARISON_EPSILON);

        try {
            drag.addDAccDParam(state, new Parameter("toto", 0.), dAccdParam);
            Assert.fail();
        } catch (final PatriusException e) {
            // expected
            Assert.assertTrue(true);
        }

        try {
            aeroModel.addDDragAccDParam(state, new Parameter("toto", 0.), density, relativeVelocity, dAccdBc);
            Assert.fail();
        } catch (final PatriusException e) {
            // expected
            Assert.assertTrue(true);
        }
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#AERO_MODEL}
     * @testedFeature {@link features#DRAG_PARTIAL_DERIVATIVES}
     * 
     * @testedMethod {@link AeroModel#addDDragAccDState(SpacecraftState, double[][], double[][], double, Vector3D)}
     * @testedMethod {@link DragForce#addDAccDState(SpacecraftState, double[][], double[][], double[])}
     * 
     * @description Test for the addDDragAccDState method for the sphere.
     *              In PATRIUS V3.4, a new implementation of the derivatives wrt position is available : it includes
     *              partial derivatives
     *              of the density wrt position.
     * 
     * @input an Assembly (a sphere)
     * 
     * @output the partial derivatives with respect to spacecraft position and velocity
     * 
     * @testPassCriteria the partial derivatives are the expected one
     * 
     * @referenceVersion 3.4
     * 
     * @nonRegressionVersion 3.4
     */
    @Test
    public void testDDragAccDState() throws PatriusException {

        Report.printMethodHeader("testDDragAccDState", "Partial derivatives / vel computation", "Math",
            this.comparisonEpsilon, ComparisonType.ABSOLUTE);

        // create the assembly :
        this.createTestAssemblySphereOnly();

        // Build an AeroModel with atmosphere model and OneAxisEllipsoid :
        // new partial derivatives computation
        final double ae = Utils.ae;
        final double f = 1.0 / 298.257222101;

        // Build an atmosphere with exponential model
        final OneAxisEllipsoid earthShape = new OneAxisEllipsoid(ae, f, FramesFactory.getITRF());
        final Atmosphere atmosphere = new SimpleExponentialAtmosphere(earthShape, 0.0004, 42000.0, 7500.0);
        final AeroModel aeroModel = new AeroModel(this.testAssembly, atmosphere, earthShape);

        // spacecraft
        final AbsoluteDate date = new AbsoluteDate(2003, 1, 1, TimeScalesFactory.getTT());
        // mu from grim4s4_gr model
        final double mu = 0.39860043770442e+15;
        // GCRF reference frame
        final Frame referenceFrame = FramesFactory.getGCRF();
        // pos-vel
        final Vector3D pos = new Vector3D(4862.610e+03, 2807.429e+03, 3220.373e+03);
        final Vector3D vel = new Vector3D(6414.7, -2006., -3180.);
        final PVCoordinates pvCoordinates = new PVCoordinates(pos, vel);
        final Orbit orbit = new CartesianOrbit(pvCoordinates, referenceFrame, date, mu);
        final Attitude attitude = new LofOffset(orbit.getFrame(), LOFType.LVLH).getAttitude(orbit, orbit.getDate(),
            orbit.getFrame());
        final SpacecraftState state = new SpacecraftState(orbit, attitude);
        this.testAssembly.initMainPartFrame(state);
        final double[][] dAccdPos = new double[3][3];
        final double[][] dAccdVit = new double[3][3];
        // compute the relative velocity:
        final Frame frame = state.getFrame();
        final Vector3D position = state.getPVCoordinates().getPosition();

        final double rho = atmosphere.getDensity(state.getDate(), position, state.getFrame());
        final Vector3D vAtm = atmosphere.getVelocity(date, position, frame);
        final Vector3D relativeVelocity = vAtm.subtract(state.getPVCoordinates().getVelocity());
        final Vector3D acceleration = aeroModel.dragAcceleration(state, rho, relativeVelocity);
        aeroModel.addDDragAccDState(state, dAccdPos, dAccdVit, rho, acceleration, relativeVelocity, true, true);

        // Computed expected derivatives wrt to position using full finite differences for drho/dr
        final double[][] dRhodRContrib = new double[3][3];

        // Default step for finite differences is 10 m
        final double defaultStep = 10.;
        final double sphereRad = 10.;
        final double mass = 100.;
        // Factor (-0.5 * V^2 * S * Cx / m) used in computation
        final double factor = -0.5 * AeroCrossSectionProperty.DEFAULT_C_X * (FastMath.PI * sphereRad * sphereRad)
                / mass;

        final double omega = Constants.WGS84_EARTH_ANGULAR_VELOCITY;
        final Vector3D wEarth = new Vector3D(0., 0., omega);
        final Vector3D vrel = vel.subtract(wEarth.crossProduct(pos));
        final double norm = vrel.getNorm();

        // dRho/dr : finite differences with default step = 10m
        final double[] dRho = new double[3];
        final Vector3D posX = new Vector3D(pos.getX() + defaultStep, pos.getY(), pos.getZ());
        final Vector3D posY = new Vector3D(pos.getX(), pos.getY() + defaultStep, pos.getZ());
        final Vector3D posZ = new Vector3D(pos.getX(), pos.getY(), pos.getZ() + defaultStep);

        final double densityDx = atmosphere.getDensity(date, posX, frame);
        final double densityDy = atmosphere.getDensity(date, posY, frame);
        final double densityDz = atmosphere.getDensity(date, posZ, frame);
        dRho[0] = MathLib.divide((densityDx - rho), defaultStep);
        dRho[1] = MathLib.divide((densityDy - rho), defaultStep);
        dRho[2] = MathLib.divide((densityDz - rho), defaultStep);

        // Expected density contribution
        dRhodRContrib[0][0] = factor * norm * (vrel.getX() * dRho[0]);
        dRhodRContrib[0][1] = factor * norm * (vrel.getX() * dRho[1]);
        dRhodRContrib[0][2] = factor * norm * (vrel.getX() * dRho[2]);
        dRhodRContrib[1][0] = factor * norm * (vrel.getY() * dRho[0]);
        dRhodRContrib[1][1] = factor * norm * (vrel.getY() * dRho[1]);
        dRhodRContrib[1][2] = factor * norm * (vrel.getY() * dRho[2]);
        dRhodRContrib[2][0] = factor * norm * (vrel.getZ() * dRho[0]);
        dRhodRContrib[2][1] = factor * norm * (vrel.getZ() * dRho[1]);
        dRhodRContrib[2][2] = factor * norm * (vrel.getZ() * dRho[2]);

        // Compute expected derivatives wrt velocity "manually" (simple formulae)
        final double[][] expectedDAccDv = new double[3][3];
        final double vrelX = vrel.getX();
        final double vrelY = vrel.getY();
        final double vrelZ = vrel.getZ();
        // Intermediate factor : (-0.5 * rho * V^2 * Cx / m) used in computation
        final double factTimesRho = factor * rho;
        final double factRhoOnNorm = factTimesRho / norm;

        expectedDAccDv[0][0] = factRhoOnNorm * vrelX * vrelX + factTimesRho * norm;
        expectedDAccDv[0][1] = factRhoOnNorm * vrelX * vrelY;
        expectedDAccDv[0][2] = factRhoOnNorm * vrelX * vrelZ;
        expectedDAccDv[1][0] = factRhoOnNorm * vrelX * vrelY;
        expectedDAccDv[1][1] = factRhoOnNorm * vrelY * vrelY + factTimesRho * norm;
        expectedDAccDv[1][2] = factRhoOnNorm * vrelY * vrelZ;
        expectedDAccDv[2][0] = factRhoOnNorm * vrelX * vrelZ;
        expectedDAccDv[2][1] = factRhoOnNorm * vrelY * vrelZ;
        expectedDAccDv[2][2] = factRhoOnNorm * vrelZ * vrelZ + factTimesRho * norm;

        // Expected dAcc/dr : sum of density contribution and computated derivatives wrt velocity
        final double[][] expectedDAccDr = new double[3][3];
        expectedDAccDr[0][0] = dRhodRContrib[0][0] - omega * expectedDAccDv[0][1];
        expectedDAccDr[0][1] = dRhodRContrib[0][1] + omega * expectedDAccDv[0][0];
        expectedDAccDr[0][2] = dRhodRContrib[0][2];
        expectedDAccDr[1][0] = dRhodRContrib[1][0] - omega * expectedDAccDv[1][1];
        expectedDAccDr[1][1] = dRhodRContrib[1][1] + omega * expectedDAccDv[1][0];
        expectedDAccDr[1][2] = dRhodRContrib[1][2];
        expectedDAccDr[2][0] = dRhodRContrib[2][0] - omega * expectedDAccDv[2][1];
        expectedDAccDr[2][1] = dRhodRContrib[2][1] + omega * expectedDAccDv[2][0];
        expectedDAccDr[2][2] = dRhodRContrib[2][2];

        // Comparisons
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                // Epsilon comparison for derivatives wrt position is 1.0e-6 because
                // density derivatives are computed using full finite differences while
                // AeroModel algorithm performs a computation based on altitude variation
                Assert.assertEquals(expectedDAccDr[i][j], dAccdPos[i][j], 1.0E-6);
                Assert.assertEquals(expectedDAccDv[i][j], dAccdVit[i][j], 1E-15);
            }
        }

        // This class represents an atmosphere with constant density and atmospheric velocity:
        class ConstantAtmosphere implements Atmosphere {
            /** Serializable UID. */
            private static final long serialVersionUID = -3860832499878875961L;

            @Override
            public double getDensity(final AbsoluteDate date, final Vector3D position, final Frame frame) {
                return rho;
            }

            @Override
            public Vector3D getVelocity(final AbsoluteDate date, final Vector3D position, final Frame frame) {
                return vAtm;
            }

            @Override
            public double getSpeedOfSound(final AbsoluteDate date, final Vector3D position, final Frame frame) {
                return 0;
            }

            @Override
            public Atmosphere copy() {
                return null;
            }

            @Override
            public void checkSolarActivityData(final AbsoluteDate start, final AbsoluteDate end) {
                // Nothing to do
            }
        }

        // test the DragForce method:
        // Using an AeroModel with provided step for rho partial derivatives computation by
        // finite differences on altitude (default step = 10 m)
        final AeroModel aeroModel2 = new AeroModel(this.testAssembly, atmosphere, earthShape, 10.);
        final ConstantAtmosphere atm = new ConstantAtmosphere();
        final DragForce drag = new DragForce(atm, aeroModel2);
        final double[][] dAccdPosDrag = new double[3][3];
        final double[][] dAccdVitDrag = new double[3][3];
        drag.addDAccDState(state, dAccdPosDrag, dAccdVitDrag);

        // Same results expected as previous case since drag model use a constant atmosphere
        // with same rho, relative velocity than previous case
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                Assert.assertEquals(expectedDAccDr[i][j], dAccdPosDrag[i][j], 1.0E-6);
                Assert.assertEquals(expectedDAccDv[i][j], dAccdVitDrag[i][j], 1E-15);
            }
        }

        Report.printToReport("Partial derivatives", expectedDAccDv, dAccdVitDrag);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#AERO_MODEL}
     * @testedFeature {@link features#DRAG_PARTIAL_DERIVATIVES}
     * 
     * @testedMethod {@link AeroModel#addDDragAccDState(SpacecraftState, double[][], double[][], double, Vector3D)}
     * 
     * @description "Bound" test for the addDDragAccDState method for the sphere.
     *              In PATRIUS V3.4, a new implementation of the derivatives wrt position is available : it includes
     *              partial derivatives
     *              of the density wrt position. As this computation need derivatives wrt velocity, an exception is
     *              thrown in the case where
     *              the computation wrt velocity is deactivated (by dedicated boolean).
     * 
     * @input an Assembly (a sphere)
     * 
     * @output exception
     * 
     * @testPassCriteria the exception must be risen as expected since derivatives wrt velocity are not activated.
     * 
     * @referenceVersion 3.4
     * 
     * @nonRegressionVersion 3.4
     */
    @Test
    public void testExceptionDDragAccDState() throws PatriusException {

        // create the assembly
        this.createTestAssemblySphereOnly();

        // Build an AeroModel with atmosphere model and OneAxisEllipsoid :
        // new partial derivatives computation
        final double ae = Utils.ae;
        final double f = 1.0 / 298.257222101;

        // Build an atmosphere with exponential model
        final OneAxisEllipsoid earthShape = new OneAxisEllipsoid(ae, f, FramesFactory.getITRF());
        final Atmosphere atmosphere = new SimpleExponentialAtmosphere(earthShape, 0.0004, 42000.0, 7500.0);
        final AeroModel aeroModel = new AeroModel(this.testAssembly, atmosphere, earthShape);

        // spacecraft
        final AbsoluteDate date = new AbsoluteDate();
        // mu from grim4s4_gr model
        final double mu = 0.39860043770442e+15;
        // GCRF reference frame
        final Frame referenceFrame = FramesFactory.getGCRF();
        // pos-vel
        final Vector3D pos = new Vector3D(3.05e+07, -2.75e+07, -6.58e+05);
        final Vector3D vel = new Vector3D(8.34e+02, 2.94e+03, -4.07e+01);
        final PVCoordinates pvCoordinates = new PVCoordinates(pos, vel);
        final Orbit orbit = new CartesianOrbit(pvCoordinates, referenceFrame, date, mu);
        final Attitude attitude = new LofOffset(orbit.getFrame(), LOFType.LVLH).getAttitude(orbit, orbit.getDate(),
            orbit.getFrame());
        final SpacecraftState state = new SpacecraftState(orbit, attitude);
        this.testAssembly.initMainPartFrame(state);
        final double[][] dAccdPos = new double[3][3];
        final double[][] dAccdVit = new double[3][3];
        // compute the relative velocity:
        final Frame frame = state.getFrame();
        final Vector3D position = state.getPVCoordinates().getPosition();

        final double rho = 1E-10;
        final Vector3D vAtm = atmosphere.getVelocity(date, position, frame);
        final Vector3D relativeVelocity = vAtm.subtract(state.getPVCoordinates().getVelocity());
        final Vector3D acceleration = aeroModel.dragAcceleration(state, rho, relativeVelocity);

        // Try to computed derivatives wrt position but not wrt velocity !
        try {
            aeroModel.addDDragAccDState(state, dAccdPos, dAccdVit, rho, acceleration, relativeVelocity, true, false);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // Expected
            Assert.assertTrue(true);
        }
    }

    /**
     * @testType UT
     * 
     * 
     * @description Test the getters of a class.
     * 
     * @input the class parameters
     * 
     * @output the class parameters
     * 
     * @testPassCriteria the parameters of the class are the same in input and
     *                   output
     * 
     * @referenceVersion 4.1
     * 
     * @nonRegressionVersion 4.1
     */
    @Test
    public void testGetters() {
        final EllipsoidBodyShape earthBody = new OneAxisEllipsoid(Constants.CNES_STELA_AE,
            Constants.GRIM5C1_EARTH_FLATTENING, FramesFactory.getCIRF());
        final SimpleExponentialAtmosphere atm = new SimpleExponentialAtmosphere(earthBody, 1e-16, 150000, 140000);

        Assert.assertTrue(earthBody.equals(atm.getShape()));
        Assert.assertEquals(1e-16, atm.getRho0(), 0);
        Assert.assertEquals(150000, atm.getH0(), 0);
        Assert.assertEquals(140000, atm.getHscale(), 0);

    }

    /** General set up method. */
    @Before
    public void setUp() {
        // Orekit data initialization
        Utils.setDataRoot("regular-dataPBASE");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));
    }
}
