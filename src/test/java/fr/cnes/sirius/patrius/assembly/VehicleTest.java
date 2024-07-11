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
 * @history creation 04/04/2017
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-2898:15/11/2021:[PATRIUS] Hypothese geocentrique a supprimer pour la SRP 
 * VERSION:4.6:FA:FA-2499:27/01/2021:[PATRIUS] Anomalie dans la gestion des panneaux solaires de la classe Vehicle 
 * VERSION:4.6:FA:FA-2741:27/01/2021:[PATRIUS] Chaine de transformation de repere non optimale dans MSIS2000
 * VERSION:4.3:DM:DM-2102:15/05/2019:[Patrius] Refactoring du paquet fr.cnes.sirius.patrius.bodies
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:834:04/04/2017:create vehicle object
 * VERSION::DM:570:05/04/2017:add PropulsiveProperty and TankProperty
 * VERSION::DM:976:16/11/2017:Merge continuous maneuvers classes
 * VERSION::FA:1449:15/03/2018:remove PropulsiveProperty name attribute
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::FA:1796:07/09/2018:Correction vehicle class
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * VERSION::FA:1777:04/10/2018:correct ICRF parent frame
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.assembly.models.AeroModel;
import fr.cnes.sirius.patrius.assembly.models.DirectRadiativeModel;
import fr.cnes.sirius.patrius.assembly.models.DragLiftModel;
import fr.cnes.sirius.patrius.assembly.models.MassModel;
import fr.cnes.sirius.patrius.assembly.models.RediffusedRadiativeModel;
import fr.cnes.sirius.patrius.assembly.models.aerocoeffs.AeroCoeffConstant;
import fr.cnes.sirius.patrius.assembly.models.aerocoeffs.AerodynamicCoefficient;
import fr.cnes.sirius.patrius.assembly.properties.MassProperty;
import fr.cnes.sirius.patrius.assembly.properties.PropulsiveProperty;
import fr.cnes.sirius.patrius.assembly.properties.RadiativeProperty;
import fr.cnes.sirius.patrius.assembly.properties.TankProperty;
import fr.cnes.sirius.patrius.assembly.properties.features.Facet;
import fr.cnes.sirius.patrius.assembly.vehicle.AerodynamicProperties;
import fr.cnes.sirius.patrius.assembly.vehicle.RadiativeProperties;
import fr.cnes.sirius.patrius.assembly.vehicle.VehicleSurfaceModel;
import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.AttitudeLaw;
import fr.cnes.sirius.patrius.attitudes.BodyCenterPointing;
import fr.cnes.sirius.patrius.attitudes.LofOffset;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.EllipsoidBodyShape;
import fr.cnes.sirius.patrius.bodies.ExtendedOneAxisEllipsoid;
import fr.cnes.sirius.patrius.bodies.JPLEphemeridesLoader;
import fr.cnes.sirius.patrius.bodies.MeeusSun;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.forces.ForceModel;
import fr.cnes.sirius.patrius.forces.atmospheres.Atmosphere;
import fr.cnes.sirius.patrius.forces.atmospheres.ExtendedAtmosphere;
import fr.cnes.sirius.patrius.forces.atmospheres.MSISE2000;
import fr.cnes.sirius.patrius.forces.atmospheres.MSISE2000InputParameters;
import fr.cnes.sirius.patrius.forces.atmospheres.US76;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.ACSOLFormatReader;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.SolarActivityDataFactory;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.specialized.ContinuousMSISE2000SolarData;
import fr.cnes.sirius.patrius.forces.drag.DragForce;
import fr.cnes.sirius.patrius.forces.maneuvers.ContinuousThrustManeuver;
import fr.cnes.sirius.patrius.forces.radiation.KnockeRiesModel;
import fr.cnes.sirius.patrius.forces.radiation.RadiationSensitive;
import fr.cnes.sirius.patrius.forces.radiation.RediffusedRadiationPressure;
import fr.cnes.sirius.patrius.forces.radiation.SolarRadiationPressureCircular;
import fr.cnes.sirius.patrius.forces.radiation.SolarRadiationPressureEllipsoid;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfigurationFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.RightCircularCylinder;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Sphere;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.FirstOrderIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.parameter.IParamDiffFunction;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.KeplerianParameters;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.MassProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class for {@link Vehicle}.
 * 
 * @author rodriguest
 * 
 * @version $Id$
 * 
 * @since 3.4
 * 
 */
public class VehicleTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Vehicle serialization
         * 
         * @featureDescription Creation of a Vehicle and try to serialize it.
         */
        VEHICULE_SERIALIZATION,
        /**
         * @featureTitle Assembly creation
         * 
         * @featureDescription Creation of a Vehicle with properties which return an Assembly.
         */
        ASSEMBLY_CREATION,
        /**
         * @featureTitle Vehicle propagation
         * 
         * @featureDescription Creation of a Vehicle and propagate it.
         */
        VEHICULE_PROPAGATION,
        /**
         * @featureTitle Assembly exception
         * 
         * @featureDescription Handle exception cases at Vehicle creation
         */
        ASSEMBLY_EXCEPTION

    }

    /** Main shape. */
    private static final String MAIN_SHAPE = "Main shape";

    /** Solar panel. */
    private static final String SOLAR_PANEL = "Solar panel";

    /** Sun. */
    private static CelestialBody sun;

    /** Earth. */
    private static EllipsoidBodyShape earth;

    /** Model for atmosphere. */
    private static ExtendedAtmosphere atm;

    /**
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#VEHICULE_SERIALIZATION}
     * 
     * @testedMethod {@link Vehicle#Vehicle()}
     * 
     * @description Create an instance of Vehicle with some properties and try to serialize then
     *              deserialize it. Initial object's attributes and retrieved deserialized object's
     *              attributes are compared.
     * 
     * @input a Vehicle
     * 
     * @output a deserialized Vehicle
     * 
     * @testPassCriteria The serialization must occurred successfully, attributes of the two
     *                   instances must be the same.
     * 
     * @referenceVersion 3.4
     * 
     * @nonRegressionVersion 3.4
     */
    @Test
    public final void testSerialization() throws IOException, ClassNotFoundException,
                                         PatriusException {

        // Create a vehicle
        final Vehicle vehicle = new Vehicle();

        // Main shape
        vehicle.setMainShape(new Sphere(Vector3D.ZERO, 1.0));
        // Define some properties
        vehicle.setDryMass(1000.);
        vehicle.addTank("Tank1", new TankProperty(300));
        vehicle.addTank("Tank2", new TankProperty(200));
        vehicle.addSolarPanel(Vector3D.PLUS_K, 1.0);
        vehicle.addSolarPanel(Vector3D.PLUS_J, 1.0);
        vehicle.setRadiativeProperties(1.0, 0.0, 0.0, 1.0, 0.0, 0.0);
        vehicle.setAerodynamicsProperties(2.2, 3.2);

        // Serialize the object
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final ObjectOutputStream so = new ObjectOutputStream(bos);
        so.writeObject(vehicle);

        // Deserialize the object
        final ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        final ObjectInputStream si = new ObjectInputStream(bis);
        final Vehicle vehicle2 = (Vehicle) si.readObject();

        // Check the two instances attributes are the same
        // Masses
        Assert.assertEquals(vehicle.getTotalMass(), vehicle2.getTotalMass(), 0.);
        Assert.assertEquals(vehicle.getDryMass(), vehicle2.getDryMass(), 0.);
        Assert.assertEquals(vehicle.getErgolsMass(), vehicle2.getErgolsMass(), 0.);

        // Tanks properties
        final List<TankProperty> tanks1 = vehicle.getTanksList();
        final List<TankProperty> tanks2 = vehicle2.getTanksList();
        final List<Facet> panel1 = vehicle.getSolarPanelsList();
        final List<Facet> panel2 = vehicle2.getSolarPanelsList();
        Assert.assertEquals(tanks1.size(), tanks2.size(), 0.);
        for (int i = 0; i < tanks1.size(); i++) {
            Assert.assertEquals(tanks1.get(i).getPartName(), tanks2.get(i).getPartName());
            Assert.assertEquals(tanks1.get(i).getMass(), tanks2.get(i).getMass(), 0.);
        }

        // Solar panels properties
        Assert.assertEquals(panel1.size(), panel2.size(), 0.);
        for (int i = 0; i < panel1.size(); i++) {
            Assert.assertEquals(panel1.get(i).getArea(), panel2.get(i).getArea(), 0.);
            Assert.assertEquals(panel1.get(i).getNormal().distance(panel2.get(i).getNormal()), 0.,
                0.);
        }
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#}
     * 
     * @testedMethod {@link Vehicle#Vehicle()}
     * @testedMethod {@link Vehicle#addEngine(fr.cnes.sirius.patrius.assembly.properties.PropulsiveProperty)}
     * @testedMethod {@link Vehicle#addSolarPanel(Vector3D, double)}
     * @testedMethod {@link Vehicle#addTank(TankProperty)}
     * @testedMethod {@link Vehicle#setAerodynamicsProperties(double, double)}
     * @testedMethod {@link Vehicle#setAerodynamicsProperties(fr.cnes.sirius.patrius.math.parameter.IParamDiffFunction, fr.cnes.sirius.patrius.math.parameter.IParamDiffFunction)}
     * @testedMethod {@link Vehicle#setDryMass(double)}
     * @testedMethod {@link Vehicle#setMainShape(fr.cnes.sirius.patrius.math.geometry.euclidean.threed.CrossSectionProvider)}
     * @testedMethod {@link Vehicle#setRadiativeProperties(double, double, double, double, double, double)}
     * @testedMethod {@link Vehicle#getAerodynamicsPropertiesFunction()}
     * @testedMethod {@link Vehicle#getDryMass()}
     * @testedMethod {@link Vehicle#getEnginesList()}
     * @testedMethod {@link Vehicle#getSolarPanelsList()}
     * @testedMethod {@link Vehicle#getTanksList()}
     * @testedMethod {@link Vehicle#getTotalMass()}
     * @testedMethod {@link Vehicle#getErgolsMass()}
     * 
     * @description Create an instance of Vehicle and test its functionality : add/set properties
     *              and get these properties.
     * 
     * @input a Vehicle
     * 
     * @output
     * 
     * @testPassCriteria The properties must be added successfully and retrieved as expected.
     * 
     * @referenceVersion 3.4
     * 
     * @nonRegressionVersion 3.4
     */
    @Test
    public final void testVehicleProperties() throws PatriusException {

        // Create a vehicle
        final Vehicle vehicle = new Vehicle();

        // Set the main shape : cylinder
        vehicle.setMainShape(new RightCircularCylinder(Vector3D.ZERO, Vector3D.PLUS_K, 1.0, 4.0));

        Assert.assertTrue(vehicle.getMainShape() instanceof RightCircularCylinder);

        // Aerodynamic properties
        final double cx = 1.7;
        final double cz = 2.;
        vehicle.setAerodynamicsProperties(cx, cz);

        final IParamDiffFunction[] aeroCoeff = vehicle.getAerodynamicsPropertiesFunction();
        final KeplerianParameters param = new KeplerianParameters(10000E3, 0.1, 0.2, 0.3, 0.4, 0.5,
            PositionAngle.TRUE, Constants.EIGEN5C_EARTH_MU);
        final KeplerianOrbit orbit = new KeplerianOrbit(param, FramesFactory.getEME2000(),
            AbsoluteDate.J2000_EPOCH);
        final SpacecraftState state = new SpacecraftState(orbit);

        Assert.assertEquals(aeroCoeff[0].value(state), 1.7, 0.);
        Assert.assertEquals(aeroCoeff[1].value(state), 2.0, 0.);

        // Reflectivity properties
        final double ka = 1.0;
        final double ks = 0.0;
        final double kd = 0.0;
        final double kaIr = 1.0;
        final double ksIr = 0.0;
        final double kdIr = 0.0;
        vehicle.setRadiativeProperties(ka, ks, kd, kaIr, ksIr, kdIr);

        final double[] radCoeffs = vehicle.getRadiativePropertiesTab();
        Assert.assertEquals(ka, radCoeffs[0], 0.);
        Assert.assertEquals(ks, radCoeffs[1], 0.);
        Assert.assertEquals(kd, radCoeffs[2], 0.);
        Assert.assertEquals(kaIr, radCoeffs[3], 0.);
        Assert.assertEquals(ksIr, radCoeffs[4], 0.);
        Assert.assertEquals(kdIr, radCoeffs[5], 0.);

        // Dry mass of the vehicle : no tanks so dry mass = total mass
        vehicle.setDryMass(1000.);
        Assert.assertEquals(vehicle.getDryMass(), vehicle.getTotalMass(), 0.);

        // Add tanks and engines : total mass has grown up
        vehicle.addTank("Tank1", new TankProperty(300.));
        vehicle.addTank("Tank2", new TankProperty(200.));
        vehicle.addEngine("Engine", new PropulsiveProperty(400., 300.));
        Assert.assertEquals(vehicle.getErgolsMass(), 500., 0.);
        Assert.assertEquals(vehicle.getTotalMass(), 1500., 0.);

        // Check that we have 1 engine and 2 tanks
        Assert.assertEquals(vehicle.getEnginesList().size(), 1);
        Assert.assertEquals(vehicle.getTanksList().size(), 2);

        // Add solar panels
        vehicle.addSolarPanel(Vector3D.PLUS_K, 1.0);
        vehicle.addSolarPanel(Vector3D.PLUS_J, 1.0);
        Assert.assertEquals(vehicle.getSolarPanelsList().size(), 2);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#ASSEMBLY_CREATION}
     * 
     * @testedMethod {@link Vehicle#Vehicle()}
     * @testedMethod {@link Vehicle#createAssembly(org.orekit.frames.Frame)}
     * 
     * @description Create an instance of Vehicle with some properties and build an {@link Assembly} from it.
     * 
     * @input a Vehicle
     * 
     * @output an Assembly
     * 
     * @testPassCriteria The Assembly must have the expected parts and properties
     * 
     * @referenceVersion 3.4
     * 
     * @nonRegressionVersion 3.4
     */
    @Test
    public final void testCreationAssembly() throws PatriusException {

        // Create a vehicle
        final Vehicle vehicle = new Vehicle();

        // Add some properties
        vehicle.setMainShape(new Sphere(Vector3D.ZERO, 1.0));
        vehicle.setDryMass(1000.);
        vehicle.addSolarPanel(Vector3D.PLUS_K, 1.0);
        vehicle.addSolarPanel(Vector3D.PLUS_J, 1.0);
        vehicle.addTank("Tank1", new TankProperty(300.));
        vehicle.addTank("Tank2", new TankProperty(200.));
        vehicle.addEngine("Engine", new PropulsiveProperty(400., 300.));
        vehicle.setAerodynamicsProperties(1.7, 0.);
        vehicle.setRadiativeProperties(1.0, 0., 0., 1.0, 0., 0.);

        // Create the assembly with default multiplicative factors on mass,
        // drag/SPR area equals to 1.
        final Assembly assembly = vehicle.createAssembly(FramesFactory.getITRF());

        // Added properties must be visible in the assembly, attached to main
        // part or parts
        final MainPart mainPart = assembly.getMainPart();
        final PropertyType[] expectedMainProp = new PropertyType[] { PropertyType.MASS,
            PropertyType.AERO_CROSS_SECTION, PropertyType.RADIATIVE_CROSS_SECTION,
            PropertyType.RADIATIVE, PropertyType.RADIATIVEIR, PropertyType.AERO_GLOBAL };

        final PropertyType[] expectedPartsProp = new PropertyType[] { PropertyType.PROPULSIVE,
            PropertyType.TANK, PropertyType.AERO_FACET, PropertyType.RADIATIVE_FACET };

        for (final PropertyType prop : expectedMainProp) {
            Assert.assertTrue(mainPart.hasProperty(prop));
        }

        // Check available parts : get all names
        // 6 parts expected including main part
        final Map<String, IPart> parts = assembly.getParts();
        Assert.assertEquals(parts.size(), 6);

        final IPart part1 = parts.get(MAIN_SHAPE);
        final IPart part2 = parts.get("Engine");
        final IPart part3 = parts.get("Tank1");
        final IPart part4 = parts.get("Tank2");
        final IPart part5 = parts.get(SOLAR_PANEL + "0");
        final IPart part6 = parts.get(SOLAR_PANEL + "1");

        // Parts must have the expected properties
        // Remain that solar panels have RADIATIVE/RADIATIVEIR properties as
        // well as main part
        Assert.assertTrue(part2.hasProperty(expectedPartsProp[0]));
        Assert.assertTrue(part3.hasProperty(expectedPartsProp[1]));
        Assert.assertTrue(part4.hasProperty(expectedPartsProp[1]));
        Assert.assertTrue(part5.hasProperty(expectedMainProp[3])
            && part5.hasProperty(expectedMainProp[4])
            && part5.hasProperty(expectedPartsProp[2])
            && part5.hasProperty(expectedPartsProp[3]));
        Assert.assertTrue(part6.hasProperty(expectedMainProp[3])
            && part6.hasProperty(expectedMainProp[4])
            && part6.hasProperty(expectedPartsProp[2])
            && part6.hasProperty(expectedPartsProp[3]));

        // Parts must have the expected names
        Assert.assertEquals(part1.getName(), MAIN_SHAPE);
        Assert.assertEquals(part2.getName(), "Engine");
        Assert.assertEquals(part3.getName(), "Tank1");
        Assert.assertEquals(part4.getName(), "Tank2");
        Assert.assertEquals(part5.getName(), SOLAR_PANEL + "0");
        Assert.assertEquals(part6.getName(), SOLAR_PANEL + "1");

        final Set<String> names = assembly.getAllPartsNames();
        Assert.assertEquals(names.size(), 6);
        Assert.assertTrue(names.contains(MAIN_SHAPE));
        Assert.assertTrue(names.contains("Engine"));
        Assert.assertTrue(names.contains("Tank1"));
        Assert.assertTrue(names.contains("Tank2"));
        Assert.assertTrue(names.contains(SOLAR_PANEL + "0"));
        Assert.assertTrue(names.contains(SOLAR_PANEL + "1"));

        // Check specific case of sphere without solar panels : check
        // aerodynamic properties have been properly defined
        final Vehicle vehicle2 = new Vehicle();
        vehicle2.setMainShape(new Sphere(Vector3D.ZERO, 1.));
        vehicle2.setAerodynamicsProperties(new AeroCoeffConstant(new Parameter("coeff1", 1)),
            new AeroCoeffConstant(new Parameter("coeff2", 2)));

        final SpacecraftState state = new SpacecraftState(new CartesianOrbit(new PVCoordinates(),
            FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH.shiftedBy(1.), 0));
        Assert.assertEquals(1., vehicle2.getAerodynamicsPropertiesFunction()[0].value(state), 0.);
        Assert.assertEquals(2., vehicle2.getAerodynamicsPropertiesFunction()[1].value(state), 0.);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#ASSEMBLY_CREATION}
     * 
     * @testedMethod {@link Vehicle#createAssembly(org.orekit.frames.Frame, double, double, double)}
     * 
     * @description Create an instance of Vehicle with mass, drag and SRP factors. Compute drag and
     *              SRP (direct and rediffused) and check that factors have properly been applied
     * 
     * @input a Vehicle
     * 
     * @output drag and SRP accelerations
     * 
     * @testPassCriteria The drag and SRP (direct and rediffused) must be properly scaled with
     *                   respect to defined multiplicative factors
     * 
     * @referenceVersion 3.4
     * 
     * @nonRegressionVersion 3.4
     */
    @Test
    public final void testCreationAssemblyWithFactors() throws PatriusException {

        // Create a vehicle
        final Vehicle vehicle = new Vehicle();

        // Add some properties
        vehicle.setMainShape(new Sphere(Vector3D.ZERO, 1.0));
        vehicle.setDryMass(1000.);
        vehicle.addSolarPanel(Vector3D.PLUS_K, 1.0);
        vehicle.addSolarPanel(Vector3D.PLUS_J, 1.0);
        vehicle.addTank("Tank1", new TankProperty(300.));
        vehicle.addTank("Tank2", new TankProperty(200.));
        vehicle.addEngine("Engine", new PropulsiveProperty(400., 300.));
        vehicle.setAerodynamicsProperties(1.7, 0.);
        vehicle.setRadiativeProperties(1.0, 0., 0., 1.0, 0., 0.);

        // Create the assembly with multiplicative factors on mass, drag / SRP.
        final Assembly assembly = vehicle.createAssembly(FramesFactory.getGCRF());
        final Assembly assemblyMass = vehicle.createAssembly(FramesFactory.getGCRF(), 2, 1, 1);
        final Assembly assemblyDrag = vehicle.createAssembly(FramesFactory.getGCRF(), 1, 3, 1);
        final Assembly assemblySRP = vehicle.createAssembly(FramesFactory.getGCRF(), 1, 1, 4);

        // State
        final Orbit orbit = new KeplerianOrbit(6700000, 0.001, MathLib.toRadians(20.), 0., 0., 0.,
            PositionAngle.MEAN, FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH,
            Constants.EGM96_EARTH_MU);
        final Attitude attitude = new BodyCenterPointing().getAttitude(orbit);
        final SpacecraftState state = new SpacecraftState(orbit, attitude, new MassModel(assembly));
        final SpacecraftState stateMass = new SpacecraftState(orbit, attitude, new MassModel(
            assemblyMass));

        // Check drag acceleration is properly scaled-up
        final Atmosphere atmosphere = new US76(new OneAxisEllipsoid(
            Constants.EGM96_EARTH_EQUATORIAL_RADIUS, Constants.GRIM5C1_EARTH_FLATTENING,
            FramesFactory.getGCRF()));

        final DragForce dragForce = new DragForce(atmosphere, new AeroModel(assembly));
        final DragForce dragForceMass = new DragForce(atmosphere, new AeroModel(assemblyMass));
        final DragForce dragForceDrag = new DragForce(atmosphere, new AeroModel(assemblyDrag));

        Assert.assertEquals(2, dragForce.computeAcceleration(state).getNorm()
            / dragForceMass.computeAcceleration(stateMass).getNorm(), 0.);
        Assert.assertEquals(3, dragForceDrag.computeAcceleration(state).getNorm()
            / dragForce.computeAcceleration(state).getNorm(), 1E-14);

        // Check SRP acceleration is properly scaled-up
        final SolarRadiationPressureCircular srpForce = new SolarRadiationPressureCircular(
            new MeeusSun(), 6378000, new DirectRadiativeModel(assembly));
        final SolarRadiationPressureCircular srpForceMass = new SolarRadiationPressureCircular(
            new MeeusSun(), 6378000, new DirectRadiativeModel(assemblyMass));
        final SolarRadiationPressureCircular srpForceSRP = new SolarRadiationPressureCircular(
            new MeeusSun(), 6378000, new DirectRadiativeModel(assemblySRP));

        Assert.assertEquals(2, srpForce.computeAcceleration(state).getNorm()
            / srpForceMass.computeAcceleration(stateMass).getNorm(), 0.);
        Assert.assertEquals(4, srpForceSRP.computeAcceleration(state).getNorm()
            / srpForce.computeAcceleration(state).getNorm(), 0.);

        // Check rediffused SRP acceleration is properly scaled-up
        final RediffusedRadiationPressure rsrpForce = new RediffusedRadiationPressure(
            new MeeusSun(), FramesFactory.getGCRF(), 5, 5, new KnockeRiesModel(),
            new RediffusedRadiativeModel(true, true, 0.5, 0.5, assembly));
        final RediffusedRadiationPressure rsrpForceMass = new RediffusedRadiationPressure(
            new MeeusSun(), FramesFactory.getGCRF(), 5, 5, new KnockeRiesModel(),
            new RediffusedRadiativeModel(true, true, 0.5, 0.5, assemblyMass));
        final RediffusedRadiationPressure rsrpForceSRP = new RediffusedRadiationPressure(
            new MeeusSun(), FramesFactory.getGCRF(), 5, 5, new KnockeRiesModel(),
            new RediffusedRadiativeModel(true, true, 0.5, 0.5, assemblySRP));

        Assert.assertEquals(2, rsrpForce.computeAcceleration(state).getNorm()
            / rsrpForceMass.computeAcceleration(stateMass).getNorm(), 0.);
        Assert.assertEquals(4, rsrpForceSRP.computeAcceleration(state).getNorm()
            / rsrpForce.computeAcceleration(state).getNorm(), 0.);
    }

    /**
     * @throws PatriusException
     * @testType VT
     * 
     * @testedFeature {@link features# VEHICULE_PROPAGATION}
     * 
     * @testedMethod {@link Vehicle#Vehicle()}
     * @testedMethod {@link Vehicle#createAssembly(org.orekit.frames.Frame, double, double, double)}
     * 
     * @description Create an instance of Vehicle and build an Assembly from it with some
     *              properties. Build aero model (DragForce) and radiative model (PRS + rediffused
     *              PRS). Add this models to a numerical propagator and propagate. This a non
     *              regression test for later versions, input are random.
     * 
     * @input a Vehicle
     * 
     * @output propagation results
     * 
     * @testPassCriteria Results is the same as reference (PATRIUS v4.6, threshold: 0)
     * 
     * @referenceVersion 4.6
     * 
     * @nonRegressionVersion 4.6
     */
    @Test
    public final void testPropagation() throws PatriusException {

        // Create a vehicle
        final Vehicle vehicle = new Vehicle();

        // Add some properties to it
        final double radius = 4.;
        final double heigh = 10.;
        final RightCircularCylinder cylinder = new RightCircularCylinder(Vector3D.ZERO,
            Vector3D.PLUS_K, radius, heigh);
        vehicle.setMainShape(cylinder);

        // Mass property : dry mass
        final double dryMass = 1000.;
        vehicle.setDryMass(dryMass);

        // Solar panels : 45° from main part
        final double panelArea = 10.;
        vehicle.addSolarPanel(Vector3D.PLUS_I.add(Vector3D.PLUS_K), panelArea);
        vehicle.addSolarPanel(Vector3D.MINUS_I.add(Vector3D.PLUS_K), panelArea);

        // Two engines
        final double thrust1 = 1000.;
        final double isp1 = 320.;
        final double thrust2 = 270.;
        final double isp2 = 150.;
        vehicle.addEngine("Engine1", new PropulsiveProperty(thrust1, isp1));
        vehicle.addEngine("Engine2", new PropulsiveProperty(thrust2, isp2));

        // One tank
        vehicle.addTank("Tank", new TankProperty(500.));

        // Aerodynamic properties
        final double cx = 1.7;
        final double cz = 0.;
        vehicle.setAerodynamicsProperties(cx, cz);

        // Reflectivity property
        final double ka = 1.;
        final double ks = 0.;
        final double kd = 0.;
        final double absorptionCoef = 1.0;
        final double specularCoef = 0.0;
        final double diffuseCoef = 0.0;
        vehicle.setRadiativeProperties(ka, ks, kd, absorptionCoef, specularCoef, diffuseCoef);

        // Create the assembly with default multiplicative factors on mass,
        // drag/SPR area equals to 1.
        final Assembly assembly = vehicle.createAssembly(FramesFactory.getGCRF());

        // Build the aero force model : Dragforce
        final AeroModel model = new AeroModel(assembly);
        final DragForce force = new DragForce(atm, model, false, false);

        // Build the radiative force model : PRS and rediffused PRS
        final RadiationSensitive radiativeModel = new DirectRadiativeModel(assembly);
        final SolarRadiationPressureEllipsoid prs = new SolarRadiationPressureEllipsoid(sun, earth,
            radiativeModel, false);
        final RediffusedRadiativeModel rediffRadiativeModel = new RediffusedRadiativeModel(true,
            true, 0.5, 0.5, assembly);
        final RediffusedRadiationPressure rediffusedPrs = new RediffusedRadiationPressure(sun,
            FramesFactory.getITRF(), 4, 4, new KnockeRiesModel(), rediffRadiativeModel, false);

        // Build the numerical propagator

        // Integrator
        final double[] absTol = new double[] { 1e-4, 1e-4, 1e-4, 1e-7, 1e-7, 1e-7 };
        final double[] relTol = new double[] { 1e-12, 1e-12, 1e-12, 1e-12, 1e-12, 1e-12 };
        final FirstOrderIntegrator integrator = new DormandPrince853Integrator(1E-6, 1000, absTol,
            relTol);

        // Propagator
        final NumericalPropagator propagator = new NumericalPropagator(integrator);
        propagator.setOrbitType(OrbitType.CARTESIAN);

        // Initial date for propagation
        final AbsoluteDate date = new AbsoluteDate("2012-01-01T00:00:00.000",
            TimeScalesFactory.getTAI());

        // Build the mass model from the Assembly
        final MassProvider massProvider = new MassModel(assembly);
        propagator.setMassProviderEquation(massProvider);

        // Attitude law (simple law, compulsory for drag computation)
        propagator.setAttitudeProvider(new BodyCenterPointing());

        // Add the forces model : aero + radiation forces and a constant thrust
        // maneuver
        propagator.addForceModel(force);
        propagator.addForceModel(prs);
        propagator.addForceModel(rediffusedPrs);
        final AbsoluteDate manStart = date.shiftedBy(300.);
        final double manDuration = 100.;
        final ContinuousThrustManeuver maneuver = new ContinuousThrustManeuver(manStart,
            manDuration, new PropulsiveProperty(thrust1, isp1), Vector3D.PLUS_I, massProvider,
            vehicle.getTanksList().get(0));
        propagator.addForceModel(maneuver);

        // Define an initial orbit : simple keplerian orbit with altitude at 400
        // km
        final Orbit initialOrb = new KeplerianOrbit(6700000, 0.001, MathLib.toRadians(20.), 0.,
            0., 0., PositionAngle.MEAN, FramesFactory.getGCRF(), date, Constants.EGM96_EARTH_MU);
        final SpacecraftState state = new SpacecraftState(initialOrb, massProvider);

        // Propagate over one day
        propagator.setInitialState(state);
        final SpacecraftState finalState = propagator.propagate(date.shiftedBy(86400.));

        // Expected tank mass after maneuver
        final double expectedTankMass = 468.13386834443986;

        // Comparisons on orbital elements and additional state (mass) -
        // Reference PATRIUS V4.6
        final CartesianOrbit finalOrbit = (CartesianOrbit) finalState.getOrbit();

        Assert.assertEquals(-6132894.431769333, finalOrbit.getPVCoordinates().getPosition().getX(),
            0.);
        Assert.assertEquals(2967003.856999945, finalOrbit.getPVCoordinates().getPosition().getY(),
            0.);
        Assert.assertEquals(1079892.967961955, finalOrbit.getPVCoordinates().getPosition().getZ(),
            0.);
        Assert.assertEquals(-3552.8220610004814,
            finalOrbit.getPVCoordinates().getVelocity().getX(), 0.);
        Assert.assertEquals(-6267.477704918801, finalOrbit.getPVCoordinates().getVelocity().getY(),
            0.);
        Assert.assertEquals(-2281.1629523867737, finalOrbit.getPVCoordinates().getVelocity().getZ(),
            0.);

        // Check the mass in the SpacecraftState as well as in the Assembly
        Assert.assertEquals(expectedTankMass, finalState.getMass("Tank"), 0.);
        Assert.assertEquals(expectedTankMass, massProvider.getMass("Tank"), 0.);
        Assert.assertEquals(expectedTankMass + dryMass,
            finalState.getMass("Tank") + finalState.getMass(MAIN_SHAPE), 0.);
        Assert.assertEquals(expectedTankMass + dryMass, massProvider.getTotalMass(finalState), 0.);
    }

    /**
     * @throws PatriusException
     * @testType VT
     * 
     * @testedFeature {@link features# VEHICULE_PROPAGATION}
     * 
     * @testedMethod {@link Vehicle#Vehicle()}
     * @testedMethod {@link Vehicle#createAssembly(org.orekit.frames.Frame, double, double, double)}
     * 
     * @description Create an instance of Vehicle and build an Assembly from it with some properties
     *              : a {@link DragLiftModel} model is used with coefficient Cx, Cz. Add this models
     *              to a numerical propagator and propagate. This a non regression test for later
     *              versions, input are random.
     * 
     * @input a Vehicle
     * 
     * @output propagation results
     * 
     * @testPassCriteria Results is the same as reference (PATRIUS v4.6, threshold: 0)
     * 
     * @referenceVersion 4.6
     * 
     * @nonRegressionVersion 4.6
     */
    @Test
    public final void testDragLiftPropagation() throws PatriusException {

        // Create a vehicle and add some properties to it
        final Vehicle veh = new Vehicle();
        veh.setDryMass(100.);
        veh.setMainShape(new Sphere(Vector3D.ZERO, 10.));

        final double cd = 2.0;
        final double cl = 0.2;
        veh.setAerodynamicsProperties(cd, cl);

        final Assembly assembly = veh.createAssembly(FramesFactory.getGCRF());
        final MassModel massProvider = new MassModel(assembly);

        // Adding atmospheric forces using US76 model
        final DragLiftModel dragLiftModel = new DragLiftModel(assembly);
        final ForceModel force = new DragForce(atm, dragLiftModel);

        // Build the numerical propagator

        // Integrator
        final double[] absTol = new double[] { 1e-4, 1e-4, 1e-4, 1e-7, 1e-7, 1e-7 };
        final double[] relTol = new double[] { 1e-12, 1e-12, 1e-12, 1e-12, 1e-12, 1e-12 };
        final FirstOrderIntegrator integrator = new DormandPrince853Integrator(1E-6, 1000, absTol,
            relTol);

        // Propagator
        final NumericalPropagator propagator = new NumericalPropagator(integrator);
        propagator.setOrbitType(OrbitType.CARTESIAN);
        propagator.addForceModel(force);

        propagator.setMassProviderEquation(massProvider);

        // Attitude law (simple law, compulsory for drag computation)
        propagator.setAttitudeProvider(new BodyCenterPointing());

        // Initial date for propagation
        final AbsoluteDate date = new AbsoluteDate("2012-01-01T00:00:00.000",
            TimeScalesFactory.getTAI());

        // Define an initial orbit : simple keplerian orbit with altitude at 400
        // km
        final Orbit initialOrb = new KeplerianOrbit(6700000, 0.001, MathLib.toRadians(20.), 0.,
            0., 0., PositionAngle.MEAN, FramesFactory.getGCRF(), date, Constants.EGM96_EARTH_MU);
        final SpacecraftState state = new SpacecraftState(initialOrb, massProvider);

        // Propagate over one day
        propagator.setInitialState(state);
        final SpacecraftState finalState = propagator.propagate(date.shiftedBy(3600.));

        // Comparisons on orbital elements and additional state (mass) -
        // Reference PATRIUS V4.6
        final CartesianOrbit finalOrbit = (CartesianOrbit) finalState.getOrbit();
        final PVCoordinates pv = finalOrbit.getPVCoordinates();

        Assert.assertEquals(-3579575.7086644927, pv.getPosition().getX(), 0.);
        Assert.assertEquals(-5308963.758949568, pv.getPosition().getY(), 0.);
        Assert.assertEquals(-1932236.8270395, pv.getPosition().getZ(), 0.);
        Assert.assertEquals(6526.323735180116, pv.getVelocity().getX(), 0.);
        Assert.assertEquals(-3873.6854655992797, pv.getVelocity().getY(), 0.);
        Assert.assertEquals(-1409.827119061159, pv.getVelocity().getZ(), 0.);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features# ASSEMBLY_EXCEPTION}
     * 
     * @testedMethod {@link Vehicle#Vehicle()}
     * @testedMethod {@link Vehicle#createAssembly(org.orekit.frames.Frame)}
     * @testedMethod {@link Vehicle#createAssembly(org.orekit.frames.Frame, double, double, double)}
     * 
     * @description Create an instance of Vehicle and try to add properties leading to exception
     *              rising : the "bounds" cases are tested.
     * 
     * @input a Vehicle
     * 
     * @output exceptions
     * 
     * @testPassCriteria The exceptions must be raised as expected
     * 
     * @referenceVersion 3.4
     * 
     * @nonRegressionVersion 3.4
     */
    @Test
    public final void testVehicleException() throws PatriusException {

        // Create a vehicle
        final Vehicle vehicle = new Vehicle();

        // Try to build a vehicle without a Main shape
        try {
            vehicle.createAssembly(FramesFactory.getGCRF());
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // This must happen
            Assert.assertTrue(true);
        } catch (final PatriusException e) {
            Assert.fail();
        }

        // Try to set tabulated Cx, Cz (non constant functions) with shape other
        // than a sphere
        vehicle.setMainShape(new RightCircularCylinder(Vector3D.ZERO, Vector3D.PLUS_K, 1.0, 4.0));

        final AerodynamicCoefficient cx = new AeroCoeffConstant(new Parameter("cx", 1.7));
        final AerodynamicCoefficient cz = new AeroCoeffConstant(new Parameter("cz", 0.));

        try {
            vehicle.setAerodynamicsProperties(cx, cz);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // This must happen
            Assert.assertTrue(true);
        }

        // Same behavior with a sphere having solar panels
        vehicle.setMainShape(new Sphere(Vector3D.ZERO, 1.0));
        vehicle.addSolarPanel(Vector3D.PLUS_K, 1.0);

        try {
            vehicle.setAerodynamicsProperties(cx, cz);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // This must happen
            Assert.assertTrue(true);
        }
    }

    /**
     * General set up method.
     * 
     * @throws PatriusException if an Orekit error occurs
     * @throws IOException should not happen
     */
    @BeforeClass
    public static void setUp() throws PatriusException, IOException {

        // Data location
        Utils.setDataRoot("regular-dataPBASE");

        // Frame configuration
        FramesFactory.setConfiguration(FramesConfigurationFactory.getIERS2010Configuration());

        // Build a MSISE atmosphere model
        final JPLEphemeridesLoader loaderSun = new JPLEphemeridesLoader("unxp2000.405",
            JPLEphemeridesLoader.EphemerisType.SUN);
        sun = loaderSun.loadCelestialBody(CelestialBodyFactory.SUN);
        earth = new ExtendedOneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
            Constants.WGS84_EARTH_FLATTENING, FramesFactory.getITRF(), "Earth");
        SolarActivityDataFactory.addSolarActivityDataReader(new ACSOLFormatReader("ACSOL.act"));
        final MSISE2000InputParameters data = new ContinuousMSISE2000SolarData(
            SolarActivityDataFactory.getSolarActivityDataProvider());
        atm = new MSISE2000(data, earth, sun);
    }

    /**
     * @throws PatriusException problem during vehicle construction
     * @testType UT
     * 
     * @testedFeature {@link features#VEHICULE_PROPAGATION}
     * 
     * @description Test the getters of a class.
     * 
     * @input the class parameters
     * 
     * @output the class parameters
     * 
     * @testPassCriteria the parameters of the class are the same in input and output
     * 
     * @referenceVersion 4.1
     * 
     * @nonRegressionVersion 4.1
     */
    @Test
    public void testGetters() throws PatriusException {

        // Create a vehicle
        final Vehicle vehicle = new Vehicle();

        // Add some properties
        vehicle.setMainShape(new Sphere(Vector3D.ZERO, 1.0));
        vehicle.setDryMass(1000.);
        vehicle.addTank("Tank1", new TankProperty(300.));
        vehicle.addTank("Tank2", new TankProperty(200.));
        vehicle.addEngine("Engine", new PropulsiveProperty(400., 300.));
        vehicle.setAerodynamicsProperties(0.1, 0);
        vehicle.setRadiativeProperties(0.2, 0.4, 0.4, 0.1, 0.5, 0.4);

        Assert.assertEquals(200, vehicle.getTank("Tank2").getMass(), 0);
        Assert.assertEquals(300, vehicle.getEngine("Engine").getIspParam().getValue(), 0);
        Assert.assertEquals(1000, vehicle.getMassProperty().getMass(), 0);
        Assert.assertEquals(0.1, vehicle.getAerodynamicProperties().getConstantDragCoef(), 0);
        Assert.assertEquals(0.4, vehicle.getRadiativeProperties().getRadiativeProperty()
            .getDiffuseReflectionRatio().getValue(), 0);

        // New vehicle

        final List<PropulsiveProperty> enginesList = new ArrayList<>();
        final PropulsiveProperty prop = new PropulsiveProperty(400., 300.);
        prop.setPartName("PART");
        enginesList.add(prop);
        final List<TankProperty> tankList = new ArrayList<>();
        tankList.add(new TankProperty(300));
        final Vehicle vehicle2 = new Vehicle(new Sphere(Vector3D.ZERO, 1.0), null,
            new MassProperty(200), new AerodynamicProperties(new Sphere(3000), 0.3),
            new RadiativeProperties(new RadiativeProperty(0.2, 0.3, 0.5), null,
                new VehicleSurfaceModel(new Sphere(300))), enginesList, tankList);

        Assert.assertEquals(0.3, vehicle2.getAerodynamicProperties().getConstantDragCoef(), 0);
        Assert.assertEquals(0.2, vehicle2.getRadiativeProperties().getRadiativeProperty()
            .getAbsorptionRatio().getValue(), 0);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VEHICULE_PROPAGATION}
     * 
     * @description Check that solar panels are properly taken into account. This test is extracted from PSIMU tutorials. 
     * 
     * @input propagation parameters, vehicle with and without solar panels
     * 
     * @output final semi-major axis
     * 
     * @testPassCriteria finak semi-major axis should be lower with added solar panels
     * 
     * @referenceVersion 4.6
     * 
     * @nonRegressionVersion 4.6
     */
    @Test
    public void testSolarPanels() throws PatriusException, IOException, ParseException {
        // Patrius Dataset initialization
        Utils.setDataRoot("regular-dataPBASE");

        final SpacecraftState reference = propagate(false);
        final SpacecraftState actual = propagate(true);
        Assert.assertTrue(reference.getOrbit().getA() > actual.getOrbit().getA());
    }

    /**
     * Propagate a spacecraft state taking into account solar panels or not.
     * @param addSolarPanels true if solar panels are added to assembly
     * @return propagated spacecraft state
     */
    public SpacecraftState propagate(final boolean addSolarPanels) throws PatriusException, IOException, ParseException {
            
            // ORBIT
            final AbsoluteDate date = new AbsoluteDate("2010-01-01T12:00:00.000", TimeScalesFactory.getUTC());
            final KeplerianOrbit iniOrbit = new KeplerianOrbit(Constants.WGS84_EARTH_EQUATORIAL_RADIUS + 250.e3, 0, FastMath.toRadians(51.6), 0, 0, 0, PositionAngle.MEAN, FramesFactory.getGCRF(), date, Constants.WGS84_EARTH_MU);
            
            // VEHICLE
            
            final Vehicle vehicle = new Vehicle();
            // Dry mass
            vehicle.setDryMass(1000.);
            // Shape
            vehicle.setMainShape(new Sphere(5.));
            
            // !!! Adding SP !!!
            if (addSolarPanels) {
                vehicle.addSolarPanel(new Vector3D(1.,  0.,  0.), 10.);
            }

            // Aerodynamic properties
            vehicle.setAerodynamicsProperties(2.,  0.);
            
            final Assembly assembly = vehicle.createAssembly(FramesFactory.getGCRF());
            final MassProvider mm = new MassModel(assembly);
            
            // FORCES (aero)
            final ExtendedOneAxisEllipsoid EARTH = new ExtendedOneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_FLATTENING, FramesFactory.getITRF(), "EARTH");
            final DragForce dragForce = new DragForce(1., new US76(EARTH), new AeroModel(assembly));
            
            // Mimick PSIMU behavior by copying drag force
            final DragForce dragForce2 = new DragForce(dragForce, assembly);
            
            // ATTITUDE
            final AttitudeLaw attitudeLaw = new LofOffset(LOFType.TNW);
            final Attitude iniAttitude = attitudeLaw.getAttitude(iniOrbit);
            
            // Propagator initialization
            final NumericalPropagator propagator = new NumericalPropagator(new ClassicalRungeKuttaIntegrator(30.));
            propagator.setAttitudeProvider(attitudeLaw);
            propagator.setInitialState(new SpacecraftState(iniOrbit, iniAttitude, mm));
            propagator.setMassProviderEquation(mm);
            propagator.addForceModel(dragForce2);

            // PROPAGATION
            return propagator.propagate(date.shiftedBy(86400.));
        }
}
