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
 * @history creation 12/03/2012
 * HISTORY
* VERSION:4.8:DM:DM-2898:15/11/2021:[PATRIUS] Hypothese geocentrique a supprimer pour la SRP 
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:106:16/07/2013:Changed mass of a radiative part to 0 and added a part with a mass
 * and no radiative properties in radiationPressureAccelerationTest().
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:284:06/10/2014:New architecture for parameterizable Parameters
 * VERSION::DM:200:28/08/2014: dealing with a negative mass in the propagator
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::FA:439:12/06/2015:Corrected partial derivatives computation for PRS
 * VERSION::FA:566:02/03/2016:Corrected partial derivatives computation for PRS wrt k0
 * VERSION::DM:596:12/04/2016:Improve test coherence
 * VERSION::DM:834:04/04/2017:create vehicle object
 * VERSION::FA:1192:30/08/2017:update parts frame
 * VERSION::FA:1796:07/09/2018:Correction vehicle class
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * VERSION::FA:1777:04/10/2018:correct ICRF parent frame
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.models;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.AssemblyBuilder;
import fr.cnes.sirius.patrius.assembly.IPartProperty;
import fr.cnes.sirius.patrius.assembly.Vehicle;
import fr.cnes.sirius.patrius.assembly.properties.MassProperty;
import fr.cnes.sirius.patrius.assembly.properties.RadiativeCrossSectionProperty;
import fr.cnes.sirius.patrius.assembly.properties.RadiativeFacetProperty;
import fr.cnes.sirius.patrius.assembly.properties.RadiativeProperty;
import fr.cnes.sirius.patrius.assembly.properties.RadiativeSphereProperty;
import fr.cnes.sirius.patrius.assembly.properties.features.Facet;
import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.LofOffset;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.forces.radiation.SolarRadiationPressureCircular;
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
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * @description <p>
 *              Test class for the radiation pressure model.
 *              </p>
 * 
 * @author Gerald Mercadier, Denis Claude
 * 
 * @version $Id$
 * 
 * @since 1.1
 * 
 */
public class RadiationModelTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Radiative model.
         * 
         * @featureDescription Computation of the radiation pressure acceleration and partial
         *                     derivatives.
         * 
         * @coveredRequirements DV-VEHICULE_410, DV-VEHICULE_430, DV-VEHICULE_431, DV-MOD_280
         */
        RADIATIVE_MODEL
    }

    /**
     * A vehicle for the tests. An Assembly is created from it.
     */
    private Vehicle vehicle;

    /**
     * sun model
     */
    private static CelestialBody sun;
    /**
     * spacecraft state
     */
    private static SpacecraftState scs;
    /**
     * Main part's name
     */
    private final String mainBody = "mainBody";
    /**
     * other part's name
     */
    private final String part2 = "part2";
    /**
     * +X oriented face
     */
    private final String facePlusX = "face+X";
    /**
     * -X oriented face
     */
    private final String faceMinusX = "face-X";
    /**
     * +Y oriented face
     */
    private final String facePlusY = "face+Y";
    /**
     * -Y oriented face
     */
    private final String faceMinusY = "face-Y";
    /**
     * +Z oriented face
     */
    private final String facePlusZ = "face+Z";
    /**
     * -Z oriented face
     */
    private final String faceMinusZ = "face-Z";

    /** eps for finite differences */
    private final double epsFD = 1E-10;

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(RadiationModelTest.class.getSimpleName(), "Radiation model");
    }

    /**
     * Creates a new Assembly with a massless radiative cross section property (sphere as main
     * shape) and with masspart without radiative props.
     * 
     * @param mass the mass of the spacecraft.
     * @param ka the absorption coefficient.
     * @param ks the specular coefficient.
     * @param kd the diffusion coefficient.
     * @return an assembly
     * @throws PatriusException
     */
    private Assembly createAssemblySphereOnly(final double mass, final double ka, final double ks,
                                              final double kd) throws PatriusException {
        final AssemblyBuilder builder = new AssemblyBuilder();

        // add main part: one sphere
        builder.addMainPart(this.mainBody);
        builder.addPart(this.part2, this.mainBody, Transform.IDENTITY);

        final Sphere sphere = new Sphere(Vector3D.ZERO, 5.);
        final IPartProperty radCrossSphereProp = new RadiativeCrossSectionProperty(sphere);
        builder.addProperty(radCrossSphereProp, this.mainBody);

        // adding radiative properties
        final IPartProperty radMainProp = new RadiativeProperty(ka, ks, kd);
        builder.addProperty(radMainProp, this.mainBody);

        // adding mass property
        final IPartProperty mass2Prop = new MassProperty(mass);
        builder.addProperty(mass2Prop, this.part2);

        // assembly creation
        return builder.returnAssembly();
    }

    /**
     * Creates a new Assembly with a massless radiative cross section property (sphere as main
     * shape) and with masspart without radiative props.
     * 
     * @param mass the mass of the spacecraft.
     * @param ka the absorption coefficient parameter.
     * @param ks the specular coefficient parameter.
     * @param kd the diffusion coefficient parameter.
     * @return an assembly
     * @throws PatriusException
     */
    private Assembly createAssemblySphereOnly(final double mass, final Parameter ka,
                                              final Parameter ks, final Parameter kd) throws PatriusException {
        final AssemblyBuilder builder = new AssemblyBuilder();

        // add main part: one sphere
        builder.addMainPart(this.mainBody);
        builder.addPart(this.part2, this.mainBody, Transform.IDENTITY);

        final Sphere sphere = new Sphere(Vector3D.ZERO, 5.);
        final IPartProperty radCrossSphereProp = new RadiativeCrossSectionProperty(sphere);
        builder.addProperty(radCrossSphereProp, this.mainBody);

        // adding radiative properties
        final IPartProperty radMainProp = new RadiativeProperty(ka, ks, kd);
        builder.addProperty(radMainProp, this.mainBody);

        // adding mass property
        final IPartProperty mass2Prop = new MassProperty(mass);
        builder.addProperty(mass2Prop, this.part2);

        // assembly creation
        return builder.returnAssembly();
    }

    /**
     * Creates a new Assembly with a cylinder as main and sets testAssembly with it.
     * 
     * @param radius cylinder radius
     * @param heigh cylinder heigh
     * @param mass cylinder mass
     * @param ka ka coeff
     * @param kd kd coeff
     * @param ks ks coeff
     * @param kaIr ka infrared coeff
     * @param kdIr kd infrared coeff
     * @param ksIr ks infrared coeff
     * @throws PatriusException
     */
    private Assembly createTestAssemblyVehicleCylinderOnly(final double radius, final double heigh,
                                                           final double mass, final double ka, final double kd,
                                                           final double ks, final double kaIr,
                                                           final double ksIr, final double kdIr)
                                                                                                throws PatriusException {

        // Creates the vehicle
        this.vehicle = new Vehicle();

        // cylinder property
        final RightCircularCylinder cylinder = new RightCircularCylinder(new Vector3D(10, 20, 30),
            Vector3D.PLUS_K, radius, heigh);

        this.vehicle.setMainShape(cylinder);
        this.vehicle.setDryMass(mass);

        // Radiative properties
        this.vehicle.setRadiativeProperties(ka, ks, kd, kaIr, ksIr, kdIr);

        // Build the Assembly
        return this.vehicle.createAssembly(FramesFactory.getGCRF());
    }

    /**
     * Creates a new Assembly with a cylinder as main and sets testAssembly with it.
     * 
     * @throws PatriusException
     */
    private Assembly createTestAssemblyVehicleCylinderWithPanels() throws PatriusException {

        // Creates the vehicle
        this.vehicle = new Vehicle();

        // cylinder property
        final RightCircularCylinder cylinder = new RightCircularCylinder(new Vector3D(10, 20, 30),
            Vector3D.PLUS_K, 2, 10.);

        this.vehicle.setMainShape(cylinder);
        this.vehicle.setDryMass(1000.);

        // add solar panels
        final double area = 10.;
        this.vehicle.addSolarPanel(Vector3D.PLUS_I.add(Vector3D.PLUS_K), area);
        this.vehicle.addSolarPanel(Vector3D.MINUS_I.add(Vector3D.PLUS_K), area);

        // Radiative properties
        this.vehicle.setRadiativeProperties(0.5, 0.25, 0.25, 0.5, 0.25, 0.25);

        // Build the Assembly
        return this.vehicle.createAssembly(FramesFactory.getGCRF());
    }

    /**
     * Creates a new Assembly with a parallelepiped as main and a solar panels and sets testAssembly
     * with it.
     * 
     * @param lenght parall lenght
     * @param width parall width
     * @param heigh parall heigh
     * @param mass parall mass
     * @param ka ka coeff
     * @param kd kd coeff
     * @param ks ks coeff
     * @param kaIr ka infrared coeff
     * @param kdIr kd infrared coeff
     * @param ksIr ks infrared coeff
     * @throws PatriusException
     */
    private Assembly createTestAssemblyVehicleParal(final double lenght, final double width,
                                                    final double heigh, final double mass, final double ka,
                                                    final double kd,
                                                    final double ks, final double kaIr, final double ksIr,
                                                    final double kdIr) throws PatriusException {

        // Creates the vehicle
        this.vehicle = new Vehicle();

        // Creation of a parallelepiped
        final Vector3D center = Vector3D.ZERO;
        final Vector3D uVector = Vector3D.PLUS_I;
        final Vector3D inputvVector = Vector3D.PLUS_J;
        final Parallelepiped parallelepiped = new Parallelepiped(center, uVector, inputvVector,
            lenght, width, heigh);

        this.vehicle.setMainShape(parallelepiped);
        this.vehicle.setDryMass(mass);

        // Radiative properties
        this.vehicle.setRadiativeProperties(ka, ks, kd, kaIr, ksIr, kdIr);

        // Build the Assembly
        return this.vehicle.createAssembly(FramesFactory.getGCRF());
    }

    /**
     * Creates a new Assembly with a parallelepiped as main and a solar panels and sets testAssembly
     * with it.
     * 
     * @param lenght parall lenght
     * @param width parall width
     * @param heigh parall heigh
     * @param mass parall mass
     * @param area panels area
     * @param ka ka coeff
     * @param kd kd coeff
     * @param ks ks coeff
     * @param kaIr ka infrared coeff
     * @param kdIr kd infrared coeff
     * @param ksIr ks infrared coeff
     * @throws PatriusException
     */
    private Assembly
            createTestAssemblyVehicleParalWithPanels(final double lenght,
                                                     final double width, final double heigh, final double mass,
                                                     final double area,
                                                     final double ka, final double kd, final double ks,
                                                     final double kaIr,
                                                     final double kdIr, final double ksIr) throws PatriusException {

        // Creates the vehicle
        this.vehicle = new Vehicle();

        // Creation of a parallelepiped
        final Vector3D center = Vector3D.ZERO;
        final Vector3D uVector = Vector3D.PLUS_I;
        final Vector3D inputvVector = Vector3D.PLUS_J;
        final Parallelepiped parallelepiped = new Parallelepiped(center, uVector, inputvVector,
            lenght, width, heigh);

        this.vehicle.setMainShape(parallelepiped);
        this.vehicle.setDryMass(mass);

        // add solar panels
        this.vehicle.addSolarPanel(Vector3D.PLUS_I.add(Vector3D.PLUS_K), area);
        this.vehicle.addSolarPanel(Vector3D.MINUS_I.add(Vector3D.PLUS_K), area);

        // Radiative properties
        this.vehicle.setRadiativeProperties(ka, ks, kd, kaIr, ksIr, kdIr);

        // Build the Assembly
        return this.vehicle.createAssembly(FramesFactory.getGCRF());
    }

    /**
     * Creates a new Assembly with a massless radiative facet and a with masspart without radiative
     * props.
     * 
     * @param mass the mass of the spacecraft.
     * @param ka the absorption coefficient.
     * @param ks the specular coefficient.
     * @param kd the diffusion coefficient.
     * @return an assembly
     * @throws PatriusException
     */
    private Assembly createAssemblyFacetOnly(final double mass, final double ka, final double ks,
                                             final double kd) throws PatriusException {
        final AssemblyBuilder builder = new AssemblyBuilder();

        // Assembly:
        final String mainPart = "satellite";
        builder.addMainPart(mainPart);
        final UpdatableFrame mainpartFrame = new UpdatableFrame(FramesFactory.getGCRF(),
            Transform.IDENTITY, "strewberry field");
        builder.initMainPartFrame(mainpartFrame);
        // Add the facet:
        final Facet faceArray = new Facet(Vector3D.MINUS_K, 2.);
        final IPartProperty radArrayProp = new RadiativeFacetProperty(faceArray);
        builder.addProperty(radArrayProp, mainPart);
        builder.addProperty(new RadiativeProperty(ka, ks, kd), mainPart);

        // adding mass property
        final IPartProperty mass2Prop = new MassProperty(mass);
        builder.addPart(this.part2, mainPart, Transform.IDENTITY);
        builder.addProperty(mass2Prop, this.part2);

        // Assembly creation:
        return builder.returnAssembly();
    }

    /**
     * Creates a new Assembly with a massless radiative facet and a with masspart without radiative
     * props.
     * 
     * @param mass the mass of the spacecraft.
     * @param ka the absorption coefficient parameter.
     * @param ks the specular coefficient parameter.
     * @param kd the diffusion coefficient parameter.
     * @return an assembly
     * @throws PatriusException
     */
    private Assembly createAssemblyFacetOnly(final double mass, final Parameter ka,
                                             final Parameter ks, final Parameter kd) throws PatriusException {
        final AssemblyBuilder builder = new AssemblyBuilder();

        // Assembly:
        final String mainPart = "satellite";
        builder.addMainPart(mainPart);
        final UpdatableFrame mainpartFrame = new UpdatableFrame(FramesFactory.getGCRF(),
            Transform.IDENTITY, "strewberry field");
        builder.initMainPartFrame(mainpartFrame);
        // Add the facet:
        final Facet faceArray = new Facet(Vector3D.MINUS_K, 2.);
        final IPartProperty radArrayProp = new RadiativeFacetProperty(faceArray);
        builder.addProperty(radArrayProp, mainPart);
        builder.addProperty(new RadiativeProperty(ka, ks, kd), mainPart);

        // adding mass property
        final IPartProperty mass2Prop = new MassProperty(mass);
        builder.addPart(this.part2, mainPart, Transform.IDENTITY);
        builder.addProperty(mass2Prop, this.part2);

        // Assembly creation:
        return builder.returnAssembly();
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#RADIATIVE_MODEL}
     * 
     * @testedMethod {@link DirectRadiativeModel#DirectRadiativeModel(CelestialBody, Assembly)}
     * 
     * @description Creation of an illegal assembly
     * 
     * @input Assembly without radiative properties.
     * 
     * @output RadiativeModel.
     * 
     * @testPassCriteria an IllegalArgumentException is thrown when building an instance of
     *                   RadiativeModel .
     * 
     * @referenceVersion 2.0
     */
    @Test
    public final void illegalAssemblyTest() throws PatriusException {
        final AssemblyBuilder builder = new AssemblyBuilder();

        builder.addMainPart(this.mainBody);
        builder.addProperty(new MassProperty(100), this.mainBody);

        try {
            new DirectRadiativeModel(builder.returnAssembly());
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected !
        }
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#RADIATIVE_MODEL}
     * 
     * @testedMethod {@link DirectRadiativeModel#DirectRadiativeModel(CelestialBody, Assembly)}
     * 
     * @description Creation of an assembly and testing the radiative properties.
     * 
     * @input 1/ Assembly with radiative properties and without mass properties. 2/ Assembly a part
     *        with redundant radiative properties
     * 
     * @output RadiativeModel.
     * 
     * @testPassCriteria an IllegalArgumentException is thrown when building an instance of
     *                   RadiativeModel .
     * 
     * @referenceVersion 1.1
     */
    @Test
    public final void assemblyTest() {

        /**
         * Test on a model with a sphere and a facet with no mass property.
         */

        final AssemblyBuilder builder = new AssemblyBuilder();

        // add main part (one sphere) and part2 (one facet)
        builder.addMainPart(this.mainBody);
        builder.addPart(this.part2, this.mainBody, Transform.IDENTITY);

        // one facet
        final Vector3D normal = new Vector3D(0.0, 0.0, -2.0);
        final Facet facet = new Facet(normal, 25 * FastMath.PI);

        final IPartProperty radSphereProp = new RadiativeSphereProperty(5.);
        builder.addProperty(radSphereProp, this.mainBody);
        final IPartProperty radFacetProp = new RadiativeFacetProperty(facet);
        builder.addProperty(radFacetProp, this.part2);

        // adding radiative properties
        final IPartProperty radProp = new RadiativeProperty(0.6, 0.4, 0.);
        builder.addProperty(radProp, this.mainBody);
        builder.addProperty(radProp, this.part2);

        // no mass property !

        // assembly creation
        final Assembly assembly = builder.returnAssembly();

        // radiative model
        try {
            new DirectRadiativeModel(assembly);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected !
        }

        /**
         * Test on a model with a part with redundant radiative properties.
         */
        final AssemblyBuilder builder2 = new AssemblyBuilder();

        // add main part (one sphere) and part2 (one facet)
        builder2.addMainPart(this.mainBody);
        builder2.addPart(this.part2, this.mainBody, Transform.IDENTITY);

        // one facet
        final Facet facet2 = new Facet(new Vector3D(0.0, 0.0, -2.0), 25 * FastMath.PI);

        final IPartProperty radSphereProp2 = new RadiativeSphereProperty(5.);
        builder2.addProperty(radSphereProp2, this.mainBody);
        final IPartProperty radFacetProp2 = new RadiativeFacetProperty(facet2);
        builder2.addProperty(radFacetProp2, this.part2);
        // redundant radiative property !
        builder2.addProperty(radSphereProp2, this.part2);

        // adding radiative properties
        final IPartProperty radMainProp2 = new RadiativeProperty(0.6, 0.4, 1.);
        builder2.addProperty(radMainProp2, this.mainBody);
        builder2.addProperty(radMainProp2, this.part2);

        // assembly creation
        final Assembly assembly2 = builder2.returnAssembly();

        // radiative model
        try {
            new DirectRadiativeModel(assembly2);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected !
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#RADIATIVE_MODEL}
     * 
     * @testedMethod {@link DirectRadiativeModel#radiationPressureAcceleration(SpacecraftState, Vector3D)}
     * 
     * @description Creation of an assembly and testing the radiation pressure acceleration.
     * 
     * @input Assembly with radiative properties : radiative property and radiative cross section
     *        property for the main shape (a sphere) and a radiative facet property for the facet
     *        attached to the main shape.
     * 
     * @output the radiation pressure acceleration and the derivatives.
     * 
     * @testPassCriteria the computed acceleration are the expected ones.
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 2.3
     */
    @Test
    public final void radiationPressureAccelerationTest() {

        Report.printMethodHeader("radiationPressureAccelerationTest", "Acceleration computation",
            "Math", Precision.DOUBLE_COMPARISON_EPSILON, ComparisonType.ABSOLUTE);

        /**
         * Test on a model with a sphere and one facet
         */

        final AssemblyBuilder builder = new AssemblyBuilder();

        try {
            // add main part (one sphere) and part2 (one facet)
            builder.addMainPart(this.mainBody);
            builder.addPart(this.part2, this.mainBody, Transform.IDENTITY);

            // one facet
            final Vector3D normal = new Vector3D(0.0, 0.0, -2.0);
            final Facet facet = new Facet(normal, 25 * FastMath.PI);

            // Main shape : sphere
            final Sphere sphere = new Sphere(Vector3D.ZERO, 5.);
            final IPartProperty radCrossSphereProp = new RadiativeCrossSectionProperty(sphere);
            builder.addProperty(radCrossSphereProp, this.mainBody);
            final IPartProperty radFacetProp = new RadiativeFacetProperty(facet);
            builder.addProperty(radFacetProp, this.part2);

            // adding radiative properties
            final IPartProperty radMainProp = new RadiativeProperty(1., 0., 0.);
            builder.addProperty(radMainProp, this.mainBody);
            final IPartProperty radPartProp = new RadiativeProperty(1., 0., 0.);
            builder.addProperty(radPartProp, this.part2);

            // adding mass property
            final double mainPartMass = 0.;
            final IPartProperty massMainProp = new MassProperty(mainPartMass);
            builder.addProperty(massMainProp, this.mainBody);
            final double part2Mass = 1000.;
            final IPartProperty massFacetProp = new MassProperty(part2Mass);
            builder.addProperty(massFacetProp, this.part2);

            // add a mass only part
            final double massyPartMass = 1500;
            final String massy = "massy part";
            builder.addPart(massy, this.mainBody, Transform.IDENTITY);
            builder.addProperty(new MassProperty(massyPartMass), massy);

            builder.initMainPartFrame(new UpdatableFrame(FramesFactory.getGCRF(),
                Transform.IDENTITY, "mainFrame"));

            // assembly creation
            final Assembly assembly = builder.returnAssembly();

            // incoming flux
            final Vector3D flux = new Vector3D(0., 0., 3.);

            // radiative model
            final DirectRadiativeModel radiativeModel = new DirectRadiativeModel(assembly);

            // spacecraft
            final AbsoluteDate date = new AbsoluteDate(2005, 3, 5, 0, 24, 0.0,
                TimeScalesFactory.getTAI());
            // mu from grim4s4_gr model
            final double mu = 0.39860043770442e+15;
            // GCRF reference frame
            final Frame referenceFrame = FramesFactory.getGCRF();
            // pos-vel
            final Vector3D pos = new Vector3D(4.05228560172917172e+07, -1.17844795966431592e+07,
                -6.58338151580381091e+05);
            final Vector3D vel = new Vector3D(8.57448611492193891e+02, 2.94919910671677371e+03,
                -4.06888496702080431e+01);
            final PVCoordinates pvCoordinates = new PVCoordinates(pos, vel);
            final Orbit orbit = new CartesianOrbit(pvCoordinates, referenceFrame, date, mu);
            // creation of the spacecraft with no attitude
            final SpacecraftState spacecraftState = new SpacecraftState(orbit, new Attitude(date,
                referenceFrame, Rotation.IDENTITY, Vector3D.ZERO));

            // compute radiation pressure acceleration
            final Vector3D computedAcc = radiativeModel.radiationPressureAcceleration(
                spacecraftState, flux);

            // expected force on the sphere
            // mass

            // radiative coefficients
            final double mainPartAbsCoef = 1;
            final double mainPartSpeCoef = 0;
            // kP = S*(1+4*(1-absCoef)*(1-speCoef)/9)
            final double kP = 25 * FastMath.PI
                * (1 + 4 * (1 - mainPartAbsCoef) * (1 - mainPartSpeCoef) / 9);

            final Vector3D sphereExpectedForce = new Vector3D(kP, flux);

            // expected force on the facet
            // mass
            // radiative coefficients
            final double part2AbsCoef = 1;
            final double part2SpeCoef = 0;
            // orientation of the facet
            final double orientation = Vector3D.dotProduct(flux.normalize(), normal.normalize());
            // computedFlux = -orientation*S*||flux||/mass
            final double computedFlux = -orientation * 25 * FastMath.PI * flux.getNorm();
            // acceleration along the flux direction
            // cF = computedFlux*(k_diff+k_abs)
            final double cF = computedFlux * part2AbsCoef;
            // acceleration along the normal direction
            // cN = 2*computedFlux*(k_spe*orientation-k_diff/3)
            final double cN = 2 * computedFlux * part2SpeCoef * orientation;

            final Vector3D facetExpectedForce = new Vector3D(cF, flux.normalize(), cN,
                normal.normalize());

            // expected radiation pressure acceleration on the assembly
            final Vector3D expectedAcc = new Vector3D(
                1.0 / (mainPartMass + part2Mass + massyPartMass),
                sphereExpectedForce.add(facetExpectedForce));

            // Orekit model = (1 - Ka)(1 - Ks)
            // PBD SIRIUS model = Kd
            Assert.assertEquals(0.0, computedAcc.subtract(expectedAcc).getNorm(),
                Precision.DOUBLE_COMPARISON_EPSILON);

            Report.printToReport("Acceleration", expectedAcc, computedAcc);

            // radiative model with k0 <> 1
            final double k0_value = 2.0;
            final DirectRadiativeModel radiativeModelK0 = new DirectRadiativeModel(assembly,
                k0_value);

            // compute radiation pressure acceleration
            final Vector3D computedAcck0 = radiativeModelK0.radiationPressureAcceleration(
                spacecraftState, flux);
            Assert.assertEquals(computedAcck0, computedAcc.scalarMultiply(k0_value));
        } catch (final PatriusException e) {
            Assert.fail();
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#RADIATIVE_MODEL}
     * 
     * @testedMethod {@link DirectRadiativeModel#radiationPressureAcceleration(SpacecraftState, Vector3D)}
     * @testedMethod {@link SolarRadiationPressureCircular#computeAcceleration(SpacecraftState)}
     * 
     * @description Creation of an assembly and testing the radiation pressure acceleration using
     *              the SphericalSpacecraft class as a reference.
     * 
     * @input Assembly with radiative properties : {@link RadiativeCrossSectionProperty} and {@link RadiativeProperty}.
     * 
     * @output the radiation pressure acceleration.
     * 
     * @testPassCriteria the computed acceleration is the expected one.
     * 
     * @referenceVersion 1.1
     */
    @Test
    public final void sphereTest() {

        Report.printMethodHeader("sphereTest", "Acceleration computation (sphere)", "Orekit",
            Precision.DOUBLE_COMPARISON_EPSILON, ComparisonType.ABSOLUTE);

        /**
         * Test on a model with a sphere and compare with the SphericalSpacecraft model
         */

        final AssemblyBuilder builder = new AssemblyBuilder();

        try {
            // add main part: one sphere
            builder.addMainPart(this.mainBody);

            final Sphere sphere = new Sphere(Vector3D.ZERO, 5.);
            final IPartProperty radCrossSphereProp = new RadiativeCrossSectionProperty(sphere);
            builder.addProperty(radCrossSphereProp, this.mainBody);

            // adding radiative properties
            final IPartProperty radMainProp = new RadiativeProperty(0.6, 0.1, 0.3);
            builder.addProperty(radMainProp, this.mainBody);

            // adding mass property
            // mass
            final double mainPartMass = 1000.;
            final IPartProperty massMainProp = new MassProperty(mainPartMass);
            builder.addProperty(massMainProp, this.mainBody);

            builder.initMainPartFrame(new UpdatableFrame(FramesFactory.getGCRF(),
                Transform.IDENTITY, "mainFrame"));

            // assembly creation
            final Assembly assembly = builder.returnAssembly();

            // radiative model
            final DirectRadiativeModel radiativeModel = new DirectRadiativeModel(assembly);

            final Vector3D position = new Vector3D(1.5698127651877177E+06, -4.0571199222644530E+06,
                5.1323187886121357E+06);
            final Vector3D velocity = new Vector3D(2.6780951868939924E+03, -5.2461908201159367E+03,
                -4.9566405945152592E+03);
            final PVCoordinates pvs = new PVCoordinates(position, velocity);
            final double mu = 0.39860043770442e+15;
            final Orbit orbit = new CartesianOrbit(pvs, FramesFactory.getGCRF(),
                new AbsoluteDate(), mu);
            final Attitude attitude = new Attitude(new AbsoluteDate(), FramesFactory.getGCRF(),
                Rotation.IDENTITY, Vector3D.ZERO);
            final SolarRadiationPressureCircular prs1 = new SolarRadiationPressureCircular(sun,
                Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS, radiativeModel);
            final SpacecraftState sstate = new SpacecraftState(orbit, attitude);
            // compute radiation pressure acceleration
            final Vector3D computedAcc = prs1.computeAcceleration(sstate);

            // Comparison with SphericalSpacecraft model
            // final SphericalSpacecraft sphereSC = new SphericalSpacecraft(25. * FastMath.PI, 0.,
            // 0.6, 0.25, 0.0);
            // final SolarRadiationPressure prs2 = new SolarRadiationPressure(sun,
            // Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS, sphereSC);
            // compute radiation pressure acceleration
            // final Vector3D expectedAcc = prs2.computeAcceleration(sstate);
            final Vector3D expectedAcc = new Vector3D(-7.561597648255493E-8, 3.788391461579627E-7,
                1.6426463922070193E-7);

            Assert.assertEquals(0.0, computedAcc.subtract(expectedAcc).getNorm(),
                2. * Precision.DOUBLE_COMPARISON_EPSILON);

            Report.printToReport("Acceleration", expectedAcc, computedAcc);

        } catch (final PatriusException e) {
            Assert.fail();
        }
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#RADIATIVE_MODEL}
     * 
     * @testedMethod {@link DirectRadiativeModel#radiationPressureAcceleration(SpacecraftState, Vector3D)}
     * 
     * @description Creation of an assembly and testing the radiation pressure acceleration for the
     *              cylinder case.
     * 
     * @input Assembly with radiative properties : radiative property and radiative cross section
     *        property for the main shape (a cylinder) being alone for first case, second case
     *        include a radiative facet property for the facet attached to the main shape.
     * 
     * @output the radiation pressure acceleration.
     * 
     * @testPassCriteria the computed acceleration must be the one expected.
     * 
     * @referenceVersion 3.4
     * 
     * @nonRegressionVersion 3.4
     */
    @Test
    public final void radiationPressureAccCylinderTest() throws PatriusException {

        Report.printMethodHeader("radiationPressureAccCylinderTest", "Acceleration computation",
            "Math", Precision.DOUBLE_COMPARISON_EPSILON, ComparisonType.ABSOLUTE);

        // Test values
        final double mass = 1000.;
        final double radius = 2.;
        final double heigh = 10.;
        final double area = 10.;
        final double kd = 0.25;
        final double ka = 0.5;
        final double ks = 0.25;
        final double kdIr = 0.25;
        final double kaIr = 0.5;
        final double ksIr = 0.25;

        // --------------------------------- Assembly with cylinder only ---------------------- //

        // Create the Assembly
        final Assembly testAssembly = this.createTestAssemblyVehicleCylinderOnly(radius, heigh, mass,
            ka, ks, kd, kaIr, ksIr, kdIr);

        // Radiative model
        final DirectRadiativeModel radiativeModel = new DirectRadiativeModel(testAssembly);

        // orbit
        final double mu = Constants.EGM96_EARTH_MU;
        final AbsoluteDate date = new AbsoluteDate(2012, 4, 2, 15, 3, 0, TimeScalesFactory.getTAI());
        final double a = 10000000;
        final Orbit testOrbit = new KeplerianOrbit(a, 0.93, MathLib.toRadians(75), 0, 0, 0,
            PositionAngle.MEAN, FramesFactory.getGCRF(), date, mu);
        // spacecraft
        final Attitude attitude = new LofOffset(testOrbit.getFrame(), LOFType.LVLH).getAttitude(
            testOrbit, testOrbit.getDate(), testOrbit.getFrame());
        final SpacecraftState bogusState = new SpacecraftState(testOrbit, attitude, new MassModel(
            testAssembly));
        final SpacecraftState shiftedBogusState = bogusState.shiftedBy(10.);

        // rotation from the reference frame to the spacecraft frame
        final Rotation toSatRotation = shiftedBogusState.getAttitude().getRotation();

        // Test case : flux is PLUS_I
        Vector3D flux = Vector3D.PLUS_I;
        flux = toSatRotation.applyTo(flux);

        // Compute radiation pressure acceleration
        testAssembly.updateMainPartFrame(shiftedBogusState);
        Vector3D radAcc = radiativeModel.radiationPressureAcceleration(shiftedBogusState, flux);

        // Acceleration is as expected : along flux with expected norm
        double crossSection = radius * radius * heigh;
        // crossSection * (1 + 4 kd / 9)
        double cylFactor = crossSection * (1 + (4. * kd / 9.));
        Vector3D expectedAcc = flux.scalarMultiply(cylFactor / mass);

        // Comparison
        Assert.assertEquals(expectedAcc.distance(radAcc), 0., Precision.EPSILON);

        // Test case : relative velocity is PLUS_I + PLUS_K
        // (cylinder from 45° inclination)
        flux = Vector3D.PLUS_I.add(Vector3D.PLUS_K);
        flux = toSatRotation.applyTo(flux);

        // Compute radiation pressure acceleration
        radAcc = radiativeModel.radiationPressureAcceleration(shiftedBogusState, flux);

        // Acceleration is as expected : along flux with expected norm
        crossSection = (FastMath.PI * radius * radius * MathLib.sqrt(2.) / 2. + radius * radius
            * heigh * MathLib.sqrt(2.) / 2.);
        // crossSection * (1 + 4 kd / 9)
        cylFactor = crossSection * (1 + (4. * kd / 9.));
        expectedAcc = flux.scalarMultiply(cylFactor / mass);

        // Comparison
        Assert.assertEquals(expectedAcc.distance(radAcc), 0., Precision.EPSILON);

        // --------------------------------- Assembly with panels ---------------------- //

        // create the assembly : cylinder with panels
        final Assembly assemblyPanels = this.createTestAssemblyVehicleCylinderWithPanels();

        // Radiative model
        final DirectRadiativeModel radiativeModelPanels = new DirectRadiativeModel(assemblyPanels);

        // Test case : relative velocity is MINUS_I + PLUS_K (norm sqrt(2))
        // in the satellite frame.
        // the only radiation force to be computed for facet is for the one having normal
        // I + K (negative orientation wrt velocity).
        // However, this force is (0., 0., 0.) because facet normal and velocity are orthogonal.
        flux = Vector3D.MINUS_I.add(Vector3D.PLUS_K);
        flux = toSatRotation.applyTo(flux);

        // Compute radiation pressure acceleration
        assemblyPanels.updateMainPartFrame(shiftedBogusState);
        radAcc = radiativeModelPanels.radiationPressureAcceleration(shiftedBogusState, flux);

        // Expected acceleration : aligned with velocity since force on cylinder and force on facet
        // are both aligned with velocity
        Assert.assertEquals(0., Vector3D.angle(flux, radAcc), Precision.DOUBLE_COMPARISON_EPSILON);

        // Test case : relative velocity is PLUS_I
        // the only force to be computed for facet is for the one having normal
        // -I + K (negative orientation wrt velocity)
        flux = Vector3D.PLUS_I;
        final double fluxNorm = flux.getNorm();
        flux = toSatRotation.applyTo(flux);

        // Expected : normal and tangent as computed and along the right components
        // Remember : facet area is 10; spacecraft mass is 1000

        // Cylinder contribution
        crossSection = radius * radius * heigh;
        cylFactor = crossSection * (1 + (4. * kd / 9.));

        // Facet contribution : compute normal and tangential radiation for the facet
        // angle is 3 * PI/4 between +I and -I + K
        final double cosAngle = MathLib.cos(3. * FastMath.PI / 4.);
        final double valueForceN = 2. * (-cosAngle * area * fluxNorm) * (ks * cosAngle - (kd / 3.));
        final double valueForceT = (-cosAngle * area * fluxNorm) * (kd + ka);

        // Compute radiation pressure acceleration
        radAcc = radiativeModelPanels.radiationPressureAcceleration(shiftedBogusState, flux);
        final Vector3D testAccSat = toSatRotation.applyInverseTo(radAcc);

        // Comparisons : expected an acceleration composed of facet contribution along facet's
        // normal
        // and the sum of facet and cylinder contributions on flux direction (+I)
        final Vector3D normalVect = new Vector3D(valueForceN, new Vector3D(-1. / MathLib.sqrt(2.),
            0., 1. / MathLib.sqrt(2.)));
        final Vector3D tangVect = Vector3D.PLUS_I.scalarMultiply(valueForceT + cylFactor);
        expectedAcc = normalVect.add(tangVect).scalarMultiply(1. / mass);
        Assert.assertEquals(testAccSat.distance(expectedAcc), 0.,
            Precision.DOUBLE_COMPARISON_EPSILON);

        Report.printToReport("Acceleration norm", expectedAcc.getNorm(), testAccSat.getNorm());
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#RADIATIVE_MODEL}
     * 
     * @testedMethod {@link DirectRadiativeModel#radiationPressureAcceleration(SpacecraftState, Vector3D)}
     * 
     * @description Creation of an assembly and testing the radiation pressure acceleration for the
     *              parallelepiped case.
     * 
     * @input Assembly with radiative properties : radiative property and radiative cross section
     *        property for the main shape (a parallelepiped) being alone for first case, second case
     *        include a radiative facet property for the facet attached to the main shape.
     * 
     * @output the radiation pressure acceleration.
     * 
     * @testPassCriteria the computed acceleration must be the one expected.
     * 
     * @referenceVersion 3.4
     * 
     * @nonRegressionVersion 3.4
     */
    @Test
    public final void radiationPressureAccParalTest() throws PatriusException {

        Report.printMethodHeader("radiationPressureAccParalTest", "Acceleration computation",
            "Math", Precision.DOUBLE_COMPARISON_EPSILON, ComparisonType.ABSOLUTE);

        // Test values
        final double mass = 1000.;
        final double length = 2.;
        final double width = 2.;
        final double heigh = 6.;
        final double area = 10.;
        final double kd = 0.25;
        final double ka = 0.5;
        final double ks = 0.25;
        final double kdIr = 0.25;
        final double kaIr = 0.5;
        final double ksIr = 0.25;

        // --------------------------------- Assembly with parallelepiped only
        // ---------------------- //

        // create the assembly : parallelepiped only
        final Assembly testAssembly = this.createTestAssemblyVehicleParal(length, width, heigh, mass,
            ka, kd, ks, kaIr, kdIr, ksIr);

        // create an AeroModel for the assembly
        final DirectRadiativeModel radModel = new DirectRadiativeModel(testAssembly);

        // orbit
        final double mu = Constants.EGM96_EARTH_MU;
        final AbsoluteDate date = new AbsoluteDate(2012, 4, 2, 15, 3, 0, TimeScalesFactory.getTAI());
        final double a = 10000000;
        final Orbit testOrbit = new KeplerianOrbit(a, 0.93, MathLib.toRadians(75), 0, 0, 0,
            PositionAngle.MEAN, FramesFactory.getGCRF(), date, mu);
        // spacecraft
        final Attitude attitude = new LofOffset(testOrbit.getFrame(), LOFType.LVLH).getAttitude(
            testOrbit, testOrbit.getDate(), testOrbit.getFrame());
        final SpacecraftState bogusState = new SpacecraftState(testOrbit, attitude, new MassModel(
            testAssembly));
        final SpacecraftState shiftedBogusState = bogusState.shiftedBy(10.);

        // rotation from the reference frame to the spacecraft frame
        final Rotation toSatRotation = shiftedBogusState.getAttitude().getRotation();

        // Test case : relative velocity is PLUS_I
        // in the satellite frame.
        // Parallelepiped cross section only involve one plate of area 2. * 6. (the bigger plate
        // with normal +I)
        Vector3D flux = Vector3D.PLUS_I;
        flux = toSatRotation.applyTo(flux);

        // Compute radiation pressure acceleration
        testAssembly.updateMainPartFrame(shiftedBogusState);
        Vector3D radAcc = radModel.radiationPressureAcceleration(shiftedBogusState, flux);

        // Acceleration is as expected : along flux with expected norm
        double crossSection = 2. * 6.;
        // crossSection * (1 + 4 kd / 9)
        double paralFactor = crossSection * (1 + (4. * 0.25 / 9.));
        Vector3D expectedAcc = flux.scalarMultiply(paralFactor / mass);

        // Expected acceleration : aligned with velocity and as expected
        Vector3D forceParal = flux.scalarMultiply(paralFactor / mass);
        Assert.assertEquals(radAcc.distance(forceParal), 0., Precision.DOUBLE_COMPARISON_EPSILON);

        // Test case : random direction is Vector3D(2.0, -2.0, 2.0)
        // in the satellite frame.
        flux = new Vector3D(2.0, -2.0, 2.0);
        flux = toSatRotation.applyTo(flux);

        // Compute radiation pressure acceleration
        radAcc = radModel.radiationPressureAcceleration(shiftedBogusState, flux);

        // Expected acceleration : aligned with velocity and as expected
        // Cross section is manually computed at 28. / FastMath.sqrt(3.)
        crossSection = 28. / MathLib.sqrt(3.);
        paralFactor = crossSection * (1 + (4. * 0.25 / 9.));
        forceParal = flux.scalarMultiply(paralFactor / mass);
        Assert.assertEquals(radAcc.distance(forceParal), 0., Precision.DOUBLE_COMPARISON_EPSILON);

        // --------------------------------- Assembly with panels ---------------------- //

        // create the assembly : cylinder with panels
        final Assembly assemblyPanels = this.createTestAssemblyVehicleParalWithPanels(length, width,
            heigh, mass, area, ka, kd, ks, kaIr, kdIr, ksIr);

        // Radiative model
        final DirectRadiativeModel radiativeModelPanels = new DirectRadiativeModel(assemblyPanels);

        // Test case : relative velocity is MINUS_I + PLUS_K (norm sqrt(2))
        // in the satellite frame.
        // the only radiation force to be computed for facet is for the one having normal
        // I + K (negative orientation wrt velocity)
        flux = Vector3D.MINUS_I.add(Vector3D.PLUS_K);
        flux = toSatRotation.applyTo(flux);

        // Compute radiation pressure acceleration
        assemblyPanels.updateMainPartFrame(shiftedBogusState);
        radAcc = radiativeModelPanels.radiationPressureAcceleration(shiftedBogusState, flux);

        // Expected acceleration : aligned with velocity since force on parallelepiped and force on
        // facet
        // are both aligned with velocity
        Assert.assertEquals(0., Vector3D.angle(flux, radAcc), Precision.DOUBLE_COMPARISON_EPSILON);

        // Test case : relative velocity is PLUS_I
        // the only force to be computed for facet is for the one having normal
        // -I + K (negative orientation wrt velocity)
        flux = Vector3D.PLUS_I;
        final double fluxNorm = flux.getNorm();
        flux = toSatRotation.applyTo(flux);

        // Expected : normal and tangent as computed and along the right components
        // Remember : facet area is 10; spacecraft mass is 1000

        // Parallelepiped contribution
        crossSection = 2. * 6.;
        paralFactor = crossSection * (1 + (4. * 0.25 / 9.));

        // Facet contribution : compute normal and tangential radiation for the facet
        final double cosAngle = MathLib.cos(3. * FastMath.PI / 4.);
        final double valueForceN = 2. * (-cosAngle * 10. * fluxNorm)
            * (0.25 * cosAngle - (0.25 / 3.));
        final double valueForceT = (-cosAngle * 10. * fluxNorm) * (0.25 + 0.5);

        // Call the dragAcceleration method
        radAcc = radiativeModelPanels.radiationPressureAcceleration(shiftedBogusState, flux);
        final Vector3D testAccSat = toSatRotation.applyInverseTo(radAcc);

        // Comparisons : expected an acceleration composed of facet contribution along facet's
        // normal
        // and the sum of facet and parallelepiped contributions on flux direction (+I)
        final Vector3D normalVect = new Vector3D(valueForceN, new Vector3D(-1. / MathLib.sqrt(2.),
            0., 1. / MathLib.sqrt(2.)));
        final Vector3D tangVect = Vector3D.PLUS_I.scalarMultiply(valueForceT + paralFactor);
        expectedAcc = normalVect.add(tangVect).scalarMultiply(1. / mass);
        Assert.assertEquals(testAccSat.distance(expectedAcc), 0.,
            Precision.DOUBLE_COMPARISON_EPSILON);

        Report.printToReport("Acceleration norm", expectedAcc.getNorm(), testAccSat.getNorm());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#RADIATIVE_MODEL}
     * 
     * @testedMethod {@link DirectRadiativeModel#radiationPressureAcceleration(SpacecraftState, Vector3D)}
     * @testedMethod {@link SolarRadiationPressureCircular#computeAcceleration(SpacecraftState)}
     * 
     * @description Creation of an assembly and testing the radiation pressure acceleration using
     *              the BoxAndSolarArraySpacecraft class as a reference.
     * 
     * @input Assembly with radiative properties.
     * 
     * @output the radiation pressure acceleration.
     * 
     * @testPassCriteria the computed acceleration is the expected one.
     * 
     * @referenceVersion 1.1
     */
    @Test
    public final void boxTest() {

        Report.printMethodHeader("boxTest", "Acceleration computation (box and array)", "Orekit",
            Precision.DOUBLE_COMPARISON_EPSILON, ComparisonType.ABSOLUTE);

        /**
         * Test on a model with a box and compare with the BoxAndSolarArraySpacecraft model
         */

        final AssemblyBuilder builder = new AssemblyBuilder();

        try {

            // add main part
            builder.addMainPart(this.mainBody);
            final UpdatableFrame mainpartFrame = new UpdatableFrame(FramesFactory.getGCRF(),
                Transform.IDENTITY, "warlus");
            builder.initMainPartFrame(mainpartFrame);
            final Rotation rotation = new Rotation(Vector3D.PLUS_J, MathLib.toRadians(0.));
            final Transform transform = new Transform(new AbsoluteDate(), rotation);
            // add faces of the box
            builder.addPart(this.facePlusX, this.mainBody, transform);
            builder.addPart(this.faceMinusX, this.mainBody, transform);
            builder.addPart(this.facePlusY, this.mainBody, transform);
            builder.addPart(this.faceMinusY, this.mainBody, transform);
            builder.addPart(this.facePlusZ, this.mainBody, transform);
            builder.addPart(this.faceMinusZ, this.mainBody, transform);

            // create the facet
            final double facetArea = 4;
            final Facet facetPlusX = new Facet(Vector3D.PLUS_I, facetArea);
            final Facet facetMinusX = new Facet(Vector3D.MINUS_I, facetArea);
            final Facet facetPlusY = new Facet(Vector3D.PLUS_J, facetArea);
            final Facet facetMinusY = new Facet(Vector3D.MINUS_J, facetArea);
            final Facet facetPlusZ = new Facet(Vector3D.PLUS_K, facetArea);
            final Facet facetMinusZ = new Facet(Vector3D.MINUS_K, facetArea);

            // add properties RadiativeFacetProperty
            final IPartProperty radFacetPlusXProp = new RadiativeFacetProperty(facetPlusX);
            final IPartProperty radFacetMinusXProp = new RadiativeFacetProperty(facetMinusX);
            final IPartProperty radFacetPlusYProp = new RadiativeFacetProperty(facetPlusY);
            final IPartProperty radFacetMinusYProp = new RadiativeFacetProperty(facetMinusY);
            final IPartProperty radFacetPlusZProp = new RadiativeFacetProperty(facetPlusZ);
            final IPartProperty radFacetMinusZProp = new RadiativeFacetProperty(facetMinusZ);

            builder.addProperty(radFacetPlusXProp, this.facePlusX);
            builder.addProperty(radFacetMinusXProp, this.faceMinusX);
            builder.addProperty(radFacetPlusYProp, this.facePlusY);
            builder.addProperty(radFacetMinusYProp, this.faceMinusY);
            builder.addProperty(radFacetPlusZProp, this.facePlusZ);
            builder.addProperty(radFacetMinusZProp, this.faceMinusZ);

            // add thermo-optical coefficients
            // sum = 1 because BoxAndSolarArraySpacecraft model works with a sum = 1
            final IPartProperty radMainProp = new RadiativeProperty(1., 0.0, 0.0);

            builder.addProperty(radMainProp, this.facePlusX);
            builder.addProperty(radMainProp, this.faceMinusX);
            builder.addProperty(radMainProp, this.facePlusY);
            builder.addProperty(radMainProp, this.faceMinusY);
            builder.addProperty(radMainProp, this.facePlusZ);
            builder.addProperty(radMainProp, this.faceMinusZ);

            // add mass property
            final double totalMass = 1000.;
            final double faceMass = totalMass / 6;
            final IPartProperty massFaceProp = new MassProperty(faceMass);

            builder.addProperty(massFaceProp, this.facePlusX);
            builder.addProperty(massFaceProp, this.faceMinusX);
            builder.addProperty(massFaceProp, this.facePlusY);
            builder.addProperty(massFaceProp, this.faceMinusY);
            builder.addProperty(massFaceProp, this.facePlusZ);
            builder.addProperty(massFaceProp, this.faceMinusZ);

            // assembly creation
            final Assembly assembly = builder.returnAssembly();

            // radiative model
            final DirectRadiativeModel radiativeModel = new DirectRadiativeModel(assembly);

            final Vector3D position = new Vector3D(1.5698127651877177E+06, -4.0571199222644530E+06,
                5.1323187886121357E+06);
            final Vector3D velocity = new Vector3D(2.6780951868939924E+03, -5.2461908201159367E+03,
                -4.9566405945152592E+03);
            final PVCoordinates pvs = new PVCoordinates(position, velocity);
            final double mu = 0.39860043770442e+15;
            final Orbit orbit = new CartesianOrbit(pvs, FramesFactory.getGCRF(),
                new AbsoluteDate(), mu);
            final Attitude attitude = new LofOffset(orbit.getFrame(), LOFType.LVLH).getAttitude(
                orbit, orbit.getDate(), orbit.getFrame());
            final SolarRadiationPressureCircular prs1 = new SolarRadiationPressureCircular(sun,
                Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS, radiativeModel);
            final SpacecraftState sstate = new SpacecraftState(orbit, attitude);

            // compute radiation pressure acceleration
            final Vector3D computedAcc = prs1.computeAcceleration(sstate);

            // Comparison with BoxAndSolarArraySpacecraft model
            // final BoxAndSolarArraySpacecraft boxSC = new BoxAndSolarArraySpacecraft(2., 2., 2.,
            // sun, 0.,
            // Vector3D.PLUS_J, new AbsoluteDate(), transform.transformVector(Vector3D.PLUS_I), 0.,
            // 0., 1., 0., 0.);
            // final SolarRadiationPressure prs2 = new SolarRadiationPressure(sun,
            // Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS, boxSC);

            // compute radiation pressure acceleration
            // final Vector3D expectedAcc = prs2.computeAcceleration(sstate);
            final Vector3D expectedAcc = new Vector3D(-4.9218188889308174E-9, 2.465851467059814E-8,
                1.0691931013895968E-8);

            Assert.assertEquals(0.0, computedAcc.subtract(expectedAcc).getNorm(),
                Precision.DOUBLE_COMPARISON_EPSILON);

            Report.printToReport("Acceleration", expectedAcc, computedAcc);

        } catch (final PatriusException e) {
            Assert.fail();
        }
    }

    /**
     * @throws PatriusException if ephemeris fail
     * @testType UT
     * 
     * @testedFeature {@link features#RADIATIVE_MODEL}
     * 
     * @testedMethod {@link DirectRadiativeModel#addDSRPAccDParam(SpacecraftState, String, double[])}
     * @testedMethod {@link SolarRadiationPressureCircular#addDAccDParam(SpacecraftState, String, double[])}
     * @testedMethod {@link DirectRadiativeModel#addDSRPAccDState(SpacecraftState, double[][], double[][], Vector3D)}
     * @testedMethod {@link SolarRadiationPressureCircular#addDAccDState(SpacecraftState, double[][], double[][])}
     * 
     * @description test the computation of partial derivatives with respect to thermo-optical
     *              parameters, k0 parameter and state for an assembly composed by a sphere.
     * 
     * @input SpacecraftState, arrays
     * 
     * @output partial derivatives with respect to the thermo-optical parameters
     * 
     * @testPassCriteria the test is successful if the computed partial derivatives wrt
     *                   thermo-optical coefficients k0 parameter and state are equal to the
     *                   analytical ones (1e-14 threshold on a relative scale) and to the finite
     *                   differences one (1e-10 on a relative scale).
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 3.0.1
     */
    @Test
    public void testSpherePDerivatives() throws PatriusException {

        final Parameter ka = new Parameter("ka", 0.4);
        final Parameter ks = new Parameter("ks", 0.3);
        final Parameter kd0 = new Parameter("kd0", 0.3);

        // containers for partial derivatives
        double[] dAccdParam = new double[3];

        final double mass = 1000.;
        final Assembly assembly = this.createAssemblySphereOnly(mass, ka, ks, kd0);
        assembly.initMainPartFrame(scs);
        final Parameter k0 = new Parameter("k0", 2.);
        final DirectRadiativeModel sc = new DirectRadiativeModel(assembly, k0);

        // complete coverage
        try {
            sc.addDSRPAccDParam(scs, new Parameter("toto", 0.), dAccdParam, Vector3D.PLUS_I);
            Assert.fail();
        } catch (final PatriusException e) {
            // expected
            Assert.assertTrue(true);
        }

        // coefficient difference:
        final double diff = .00001;

        // DIFFUSE COEFFICIENT
        // Assembly 1
        Assembly assembly1 = this.createAssemblySphereOnly(mass, 0.4, 0.3, 0.3 - diff / 2.);
        assembly1.initMainPartFrame(scs);
        DirectRadiativeModel sc1 = new DirectRadiativeModel(assembly1);
        // Assembly 2
        Assembly assembly2 = this.createAssemblySphereOnly(mass, 0.4, 0.3, 0.3 + diff / 2.);
        assembly2.initMainPartFrame(scs);
        DirectRadiativeModel sc2 = new DirectRadiativeModel(assembly2);

        // SRP
        final double rEquat = Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS;
        SolarRadiationPressureCircular srp = new SolarRadiationPressureCircular(sun, rEquat, sc);
        SolarRadiationPressureCircular srp1 = new SolarRadiationPressureCircular(sun, rEquat, sc1);
        SolarRadiationPressureCircular srp2 = new SolarRadiationPressureCircular(sun, rEquat, sc2);

        dAccdParam = new double[3];
        srp.addDAccDParam(scs, kd0, dAccdParam);

        // finite differences:
        Vector3D acc1 = srp1.computeAcceleration(scs);
        Vector3D acc2 = srp2.computeAcceleration(scs);
        final double[] damFdiff = { (acc2.getX() - acc1.getX()) / diff,
            (acc2.getY() - acc1.getY()) / diff, (acc2.getZ() - acc1.getZ()) / diff };

        // partial derivatives wrt diffuse coefficient:
        Assert.assertArrayEquals(damFdiff, dAccdParam, this.epsFD);

        // K0
        // Assembly 1
        assembly1 = this.createAssemblySphereOnly(mass, 0.4, 0.3, 0.3);
        assembly1.initMainPartFrame(scs);
        final Parameter k01 = new Parameter("k01", 2. - diff / 2.);
        sc1 = new DirectRadiativeModel(assembly1, k01);
        // Assembly 2
        assembly2 = this.createAssemblySphereOnly(mass, 0.4, 0.3, 0.3);
        assembly2.initMainPartFrame(scs);
        final Parameter k02 = new Parameter("k02", 2. + diff / 2.);
        sc2 = new DirectRadiativeModel(assembly2, k02);

        // SRP
        srp = new SolarRadiationPressureCircular(sun, rEquat, sc);
        srp1 = new SolarRadiationPressureCircular(sun, rEquat, sc1);
        srp2 = new SolarRadiationPressureCircular(sun, rEquat, sc2);

        dAccdParam = new double[3];
        srp.addDAccDParam(scs, k0, dAccdParam);

        // finite differences:
        acc1 = srp1.computeAcceleration(scs);
        acc2 = srp2.computeAcceleration(scs);
        final double[] damFK0 = { (acc2.getX() - acc1.getX()) / diff,
            (acc2.getY() - acc1.getY()) / diff, (acc2.getZ() - acc1.getZ()) / diff };

        // partial derivatives wrt diffuse coefficient:
        Assert.assertArrayEquals(damFK0, dAccdParam, this.epsFD);

        // complete coverage
        try {
            srp1.addDAccDParam(scs, new Parameter("toto", 0.), dAccdParam);
            Assert.fail();
        } catch (final PatriusException e) {
            // expected
            Assert.assertTrue(true);
        }

        /*
         * Partial derivatives with respect to the state
         */
        final double[][] dAccdPos = new double[3][3];
        final double[][] dAccdVel = new double[3][3];
        srp.addDAccDState(scs, dAccdPos, dAccdVel);
        for (int i = 0; i < dAccdPos.length; i++) {
            for (int j = 0; j < dAccdPos.length; j++) {
                Assert.assertEquals(0., dAccdPos[i][j], Precision.DOUBLE_COMPARISON_EPSILON);
                Assert.assertEquals(0., dAccdVel[i][j], Precision.DOUBLE_COMPARISON_EPSILON);
            }
        }
    }

    /**
     * @throws PatriusException if ephemeris fail
     * @testType UT
     * 
     * @testedFeature {@link features#RADIATIVE_MODEL}
     * 
     * @testedMethod {@link DirectRadiativeModel#addDSRPAccDParam(SpacecraftState, String, double[])}
     * @testedMethod {@link SolarRadiationPressureCircular#addDAccDParam(SpacecraftState, String, double[])}
     * 
     * @description test the computation of partial derivatives with respect to thermo-optical
     *              parameters, k0 parameter and for an assembly composed by a facet.
     * 
     * @input SpacecraftState, arrays
     * 
     * @output partial derivatives with respect to the thermo-optical parameters
     * 
     * @testPassCriteria the test is successful if the computed partial derivatives wrt
     *                   thermo-optical coefficients, k0 parameters and state are equal to the
     *                   analytical ones (1e-14 threshold on a relative scale) and to the finite
     *                   differences one (1e-10 on a relative scale).
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 3.0.1
     */
    @Test
    public void testFacetPDerivatives() throws PatriusException {
        // containers for partial derivatives
        double[] dAccdParam = new double[3];

        final Parameter ka = new Parameter("ka", 0.6);
        final Parameter ks = new Parameter("ks", 0.1);
        final Parameter kd = new Parameter("kd", 0.3);

        final double mass = 1000.;
        final Assembly assembly = this.createAssemblyFacetOnly(mass, ka, ks, kd);
        final Parameter k0 = new Parameter("k0", 1.);
        final DirectRadiativeModel sc = new DirectRadiativeModel(assembly, k0);

        // coefficient difference:
        final double diff = .00001;

        Assembly assembly1 = this.createAssemblyFacetOnly(mass, 0.6 - diff / 2., 0.1, 0.3);
        DirectRadiativeModel sc1 = new DirectRadiativeModel(assembly1);
        // Assembly 2
        Assembly assembly2 = this.createAssemblyFacetOnly(mass, 0.6 + diff / 2., 0.1, 0.3);
        DirectRadiativeModel sc2 = new DirectRadiativeModel(assembly2);

        // SRP
        // Earth with GRIM5C1 flattening
        final double rEquat = Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS;
        SolarRadiationPressureCircular srp = new SolarRadiationPressureCircular(sun, rEquat, sc);
        SolarRadiationPressureCircular srp1 = new SolarRadiationPressureCircular(sun, rEquat, sc1);
        SolarRadiationPressureCircular srp2 = new SolarRadiationPressureCircular(sun, rEquat, sc2);

        srp.addDAccDParam(scs, ka, dAccdParam);

        // finite differences:
        Vector3D acc1 = srp1.computeAcceleration(scs);
        Vector3D acc2 = srp2.computeAcceleration(scs);
        final double[] damFabs = { (acc2.getX() - acc1.getX()) / diff,
            (acc2.getY() - acc1.getY()) / diff, (acc2.getZ() - acc1.getZ()) / diff };
        // partial derivatives wrt absorption coefficient:
        Assert.assertArrayEquals(damFabs, dAccdParam, this.epsFD);

        // SPECULAR COEFFICIENT
        // Assembly 1
        assembly1 = this.createAssemblyFacetOnly(mass, 0.6, 0.1 - diff / 2., 0.3);
        sc1 = new DirectRadiativeModel(assembly1);
        // Assembly 2
        assembly2 = this.createAssemblyFacetOnly(mass, 0.6, 0.1 + diff / 2., 0.3);
        sc2 = new DirectRadiativeModel(assembly2);

        // SRP
        srp = new SolarRadiationPressureCircular(sun, rEquat, sc);
        srp1 = new SolarRadiationPressureCircular(sun, rEquat, sc1);
        srp2 = new SolarRadiationPressureCircular(sun, rEquat, sc2);

        dAccdParam = new double[3];
        srp.addDAccDParam(scs, ks, dAccdParam);

        // finite differences:
        acc1 = srp1.computeAcceleration(scs);
        acc2 = srp2.computeAcceleration(scs);
        final double[] damFspec = { (acc2.getX() - acc1.getX()) / diff,
            (acc2.getY() - acc1.getY()) / diff, (acc2.getZ() - acc1.getZ()) / diff };

        // partial derivatives wrt specular coefficient:
        Assert.assertArrayEquals(damFspec, dAccdParam, this.epsFD);

        // DIFFUSE COEFFICIENT
        // Assembly 1
        assembly1 = this.createAssemblyFacetOnly(mass, 0.6, 0.1, 0.3 - diff / 2.);
        sc1 = new DirectRadiativeModel(assembly1);
        // Assembly 2
        assembly2 = this.createAssemblyFacetOnly(mass, 0.6, 0.1, 0.3 + diff / 2.);
        sc2 = new DirectRadiativeModel(assembly2);

        // SRP
        srp = new SolarRadiationPressureCircular(sun, rEquat, sc);
        srp1 = new SolarRadiationPressureCircular(sun, rEquat, sc1);
        srp2 = new SolarRadiationPressureCircular(sun, rEquat, sc2);

        dAccdParam = new double[3];
        srp.addDAccDParam(scs, kd, dAccdParam);

        // finite differences:
        acc1 = srp1.computeAcceleration(scs);
        acc2 = srp2.computeAcceleration(scs);
        final double[] damFdiff = { (acc2.getX() - acc1.getX()) / diff,
            (acc2.getY() - acc1.getY()) / diff, (acc2.getZ() - acc1.getZ()) / diff };

        // partial derivatives wrt diffuse coefficient:
        Assert.assertArrayEquals(damFdiff, dAccdParam, this.epsFD);

        // complete coverage
        try {
            srp1.addDAccDParam(scs, new Parameter("toto", 0.), dAccdParam);
            Assert.fail();
        } catch (final PatriusException e) {
            // expected
            Assert.assertTrue(true);
        }

        // K0
        // Assembly 1
        assembly1 = this.createAssemblyFacetOnly(mass, 0.6, 0.1, 0.3);
        final Parameter k01 = new Parameter("k01", 1. - diff / 2.);
        sc1 = new DirectRadiativeModel(assembly1, k01);
        // Assembly 2
        assembly2 = this.createAssemblyFacetOnly(mass, 0.6, 0.1, 0.3);
        final Parameter k02 = new Parameter("k02", 1. + diff / 2.);
        sc2 = new DirectRadiativeModel(assembly2, k02);

        // SRP
        srp = new SolarRadiationPressureCircular(sun, rEquat, sc);
        srp1 = new SolarRadiationPressureCircular(sun, rEquat, sc1);
        srp2 = new SolarRadiationPressureCircular(sun, rEquat, sc2);

        dAccdParam = new double[3];
        srp.addDAccDParam(scs, k0, dAccdParam);

        // finite differences:
        acc1 = srp1.computeAcceleration(scs);
        acc2 = srp2.computeAcceleration(scs);
        final double[] damFk0 = { (acc2.getX() - acc1.getX()) / diff,
            (acc2.getY() - acc1.getY()) / diff, (acc2.getZ() - acc1.getZ()) / diff };

        // partial derivatives wrt diffuse coefficient:
        Assert.assertArrayEquals(damFk0, dAccdParam, this.epsFD);
    }

    /**
     * General set up method.
     * 
     * @throws PatriusException if an Orekit error occurs
     */
    @BeforeClass
    public static void setUp() throws PatriusException {
        // Orekit data initialization
        fr.cnes.sirius.patrius.Utils.setDataRoot("regular-dataPBASE");
        sun = CelestialBodyFactory.getSun();

        // params and computation
        final AbsoluteDate date = new AbsoluteDate(2000, 3, 20, 8, 10, 43.93000000000029,
            TimeScalesFactory.getTAI());
        final Vector3D pos = new Vector3D(1.5698127651877177E+06, -4.0571199222644530E+06,
            5.1323187886121357E+06);
        final Vector3D vel = new Vector3D(2.6780951868939924E+03, -5.2461908201159367E+03,
            -4.9566405945152592E+03);
        /*
         * final Vector3D pos = new Vector3D(2000000+06, 0., 0.); final Vector3D vel = new Vector3D(
         * 7.E+03, 0., 7.E+03);
         */
        final PVCoordinates pv = new PVCoordinates(pos, vel);
        final double mu = 3.9860043770442000E+14;
        final Orbit orbit = new CartesianOrbit(pv, FramesFactory.getGCRF(), date, mu);
        final Attitude attitude = new LofOffset(orbit.getFrame(), LOFType.LVLH).getAttitude(orbit,
            orbit.getDate(), orbit.getFrame());
        scs = new SpacecraftState(orbit, attitude);
    }
}
