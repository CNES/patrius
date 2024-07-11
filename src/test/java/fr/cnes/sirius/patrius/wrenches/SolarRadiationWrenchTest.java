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
 * @history creation 23/07/2013
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-2922:15/11/2021:[PATRIUS] suppression de l'utilisation de la reflexion Java dans patrius 
 * VERSION:4.3:DM:DM-2102:15/05/2019:[Patrius] Refactoring du paquet fr.cnes.sirius.patrius.bodies
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:85:22/07/2013:SolarRadiationWrench tests
 * VERSION::FA:183:17/03/2014:Added test details to javadoc
 * VERSION::DM:227:09/04/2014:Merged eclipse detectors
 * VERSION::DM:284:06/10/2014:New architecture for parameterizable Parameters
 * VERSION::DM:457:09/11/2015: Move extendedOneAxisEllipsoid from patrius to orekit addons
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * VERSION::FA:1777:04/10/2018:correct ICRF parent frame
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.wrenches;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.AssemblyBuilder;
import fr.cnes.sirius.patrius.assembly.IPartProperty;
import fr.cnes.sirius.patrius.assembly.models.DirectRadiativeWrenchModel;
import fr.cnes.sirius.patrius.assembly.properties.InertiaSimpleProperty;
import fr.cnes.sirius.patrius.assembly.properties.MassProperty;
import fr.cnes.sirius.patrius.assembly.properties.RadiativeApplicationPoint;
import fr.cnes.sirius.patrius.assembly.properties.RadiativeFacetProperty;
import fr.cnes.sirius.patrius.assembly.properties.RadiativeProperty;
import fr.cnes.sirius.patrius.assembly.properties.RadiativeSphereProperty;
import fr.cnes.sirius.patrius.assembly.properties.features.Facet;
import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.EllipsoidBodyShape;
import fr.cnes.sirius.patrius.bodies.ExtendedOneAxisEllipsoid;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Matrix3D;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class for the radiation pressure wrench model.
 * 
 * @author Rami Houdroge
 * 
 * @version $Id$
 * 
 * @since 2.1
 * 
 */
public class SolarRadiationWrenchTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Radiative model wrench
         * 
         * @featureDescription Computation of the radiation pressure wrench
         * 
         * @coveredRequirements DV-COUPLES_10, DV-COUPLES_20, DV-COUPLES_30, DV-COUPLES_50
         */
        RADIATIVE_MODEL
    }

    /**
     * sun model
     */
    private static CelestialBody sun;
    /**
     * earth
     */
    private static EllipsoidBodyShape earth;
    /**
     * Main part's name
     */
    private final String mainBody = "mainBody";
    /**
     * other part's name
     */
    private final String part2 = "part2";

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(SolarRadiationWrenchTest.class.getSimpleName(), "Solar radiation pressure wrench");
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#RADIATIVE_MODEL}
     * 
     * @testedMethod {@link SolarRadiationWrench#getParameter(String)}
     * @testedMethod {@link SolarRadiationWrench#setParameter(String, double)}
     * 
     * @description testing parameterization interface methods
     * 
     * @input different values with getters
     * 
     * @output same values with setters
     * 
     * @testPassCriteria the values obtained via the getters are the same as the ones given with the setters
     * 
     * @referenceVersion 2.1
     */
    @Test
    public final void testParametrizable() throws PatriusException {
        final double distance = Constants.SEIDELMANN_UA;
        final double pressure = Constants.CONST_SOL_N_M2;
        final double sunRadius = Constants.IERS92_SUN_EQUATORIAL_RADIUS;

        final Parameter distanceParam = new Parameter("distance", distance);
        final Parameter pressureParam = new Parameter("pressure", pressure);
        final Parameter sunRadiusParam = new Parameter("sunRadius", sunRadius);

        final SolarRadiationWrench model = new SolarRadiationWrench(distanceParam, pressureParam, sunRadiusParam, null,
            null, null);

        for (final Parameter p : model.getParameters()) {
            if (p.equals("distance")) {
                distanceParam.setValue(4);
                Assert.assertEquals(4, p.getValue(), Precision.EPSILON);
            }
            if (p.equals("pressure")) {
                pressureParam.setValue(3);
                Assert.assertEquals(3, p.getValue(), Precision.EPSILON);
            }
            if (p.equals("sunRadius")) {
                sunRadiusParam.setValue(5);
                Assert.assertEquals(5, p.getValue(), Precision.EPSILON);
            }
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#RADIATIVE_MODEL}
     * 
     * @testedMethod {@link SolarRadiationWrench#computeTorque(SpacecraftState)}
     * @testedMethod {@link SolarRadiationWrench#computeTorque(SpacecraftState, Vector3D, Frame)}
     * @testedMethod {@link SolarRadiationWrench#computeWrench(SpacecraftState)}
     * @testedMethod {@link SolarRadiationWrench#computeWrench(SpacecraftState, Vector3D, Frame)}
     * 
     * @description Creation of an assembly and testing the radiation pressure acceleration.
     * 
     * @input an Assembly made of two parts : a main body and another part.
     *        main body : sphere with a radius of 5m
     *        other part : facet with n = (0, 0, -2) and area of 25*pi
     *        application point : (-3, 0, 0) and (0, -2, 0) for the sphere and facet respectively
     *        inertia : mass center at (1, 0, 0) and I3 for inertia matrix. total mass = 1500 kg
     * 
     * @output the wrench (-8.277642288147849e-006, 1.594588736441943e-004,
     *         -4.425140233902411e-004) at mass center and the displaced (3, 0, 0) wrench (-8.277642288147849e-006,
     *         2.453965421824023e-004,
     *         -6.406757708847698e-004)
     * 
     * @testPassCriteria the computed acceleration and the derivatives are the expected ones.
     * 
     * @referenceVersion 4.2
     */
    @Test
    public final void radiationPressureAccelerationTest() {

        Report.printMethodHeader("radiationPressureAccelerationTest", "SRP wrench computation", "Unknown",
            Precision.DOUBLE_COMPARISON_EPSILON, ComparisonType.ABSOLUTE);

        /**
         * Test on a model with a sphere and one facet
         */

        final AssemblyBuilder builder = new AssemblyBuilder();

        try {

            // -------------------- ASSEMBLY RADIATIVE MODEL ---------------- //
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
            final IPartProperty radMainProp = new RadiativeProperty(1., 0., 0.);
            builder.addProperty(radMainProp, this.mainBody);
            final IPartProperty radPartProp = new RadiativeProperty(1., 0., 0.);
            builder.addProperty(radPartProp, this.part2);

            // application points
            final RadiativeApplicationPoint apS = new RadiativeApplicationPoint(Vector3D.MINUS_I.scalarMultiply(3));
            builder.addProperty(apS, this.mainBody);
            final RadiativeApplicationPoint apF = new RadiativeApplicationPoint(Vector3D.PLUS_J.scalarMultiply(-2));
            builder.addProperty(apF, this.part2);

            // inertia simple prop
            final MassProperty mp = new MassProperty(1500);
            final InertiaSimpleProperty prop = new InertiaSimpleProperty(Vector3D.PLUS_I, new Matrix3D(new double[][] {
                { 1, 0, 0 }, { 0, 1, 0 }, { 0, 0, 1 } }), mp);
            builder.addProperty(prop, this.mainBody);

            // assembly creation
            final Assembly assembly = builder.returnAssembly();

            // radiative model
            final DirectRadiativeWrenchModel radiativeModel = new DirectRadiativeWrenchModel(assembly);

            // -------------------- THE WRENCH MODEL --------------------------- //
            // Wrench model
            final SolarRadiationWrench model = new SolarRadiationWrench(sun, earth, radiativeModel);

            // -------------------- A SPACECRAFTSTATE -------------------------- //
            // spacecraft
            final AbsoluteDate date = new AbsoluteDate(2005, 3, 5, 0, 24, 0.0, TimeScalesFactory.getTAI());
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
            final SpacecraftState spacecraftState = new SpacecraftState(orbit, new Attitude(date, referenceFrame,
                Rotation.IDENTITY, Vector3D.ZERO));

            // --------------------COMPUTE WRENCH VALIDATE ACCELERATION -------------------------- //

            // compute radiation pressure acceleration
            assembly.initMainPartFrame(spacecraftState);
            final Wrench computedWrench = model.computeWrench(spacecraftState);

            // -------------------- VALIDATE ACCELERATION -------------------------- //
            final Vector3D computedAcc = computedWrench.getForce()
                .scalarMultiply(1 / 1500.);

            // we need to use the flux computed internally by SolarRadiationWrench
            final Vector3D satSunVector = model.getSatSunVector(spacecraftState);
            final double rawP = model.computeRawP(spacecraftState);
            final Vector3D flux = new Vector3D(-rawP / satSunVector.getNorm(), satSunVector);

            // radiative coefficients
            final double mainPartAbsCoef = 1;
            final double mainPartSpeCoef = 0;
            // kP = S*(1+4*(1-absCoef)*(1-speCoef)/9)
            final double kP = 25 * FastMath.PI * (1 + 4 * (1 - mainPartAbsCoef) * (1 - mainPartSpeCoef) / 9);

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

            final Vector3D facetExpectedForce = new Vector3D(cF, flux.normalize(), cN, normal.normalize());

            // expected radiation pressure acceleration on the assembly
            final Vector3D expectedAcc = new Vector3D(1.0 / 1500,
                sphereExpectedForce.add(facetExpectedForce));

            Assert.assertEquals(0.0, computedAcc.subtract(expectedAcc).getNorm(), Precision.DOUBLE_COMPARISON_EPSILON);

            // ------------------------ VALIDATE WRENCH -------------------- //

            final Vector3D expectedTorque = new Vector3D(-8.277642288147849e-006, 1.594588736441943e-004,
                -4.425140233902411e-004);
            Assert.assertEquals(0.0, model.computeTorque(spacecraftState).subtract(expectedTorque).getNorm(),
                2E-10);

            final Vector3D expectedDisplaceTorque = new Vector3D(-8.277642288147849e-006,
                2.453965421824023e-004,
                -6.406757708847698e-004);
            Vector3D actualDisplaced = model.computeWrench(spacecraftState, Vector3D.PLUS_I.scalarMultiply(3),
                assembly.getMainPart().getFrame()).getTorque();
            Assert.assertEquals(
                0.0,
                actualDisplaced.subtract(expectedDisplaceTorque)
                    .getNorm(),
                2E-10);

            Report.printToReport("Torque", expectedDisplaceTorque, actualDisplaced);

            actualDisplaced = model.computeTorque(spacecraftState, Vector3D.PLUS_I.scalarMultiply(3),
                assembly.getMainPart().getFrame());
            Assert.assertEquals(
                0.0,
                actualDisplaced.subtract(expectedDisplaceTorque)
                    .getNorm(),
                2E-10);

            // // derivatives with respect to state parameters
            // final double[][] dAccdPos = new double[3][3];
            // final double[][] dAccdVel = new double[3][3];
            // final double[] dAccdM = new double[3];
            // radiativeModel.addDSRPAccDState(spacecraftState, dAccdPos, dAccdVel, dAccdM);
            //
            // // comparison of dAccdM only (dAccdPos and dAccdVel are null)
            // // OREKIT results for coverage
            // Assert.assertEquals(-2.550590026399706E-16, dAccdM[0], Precision.DOUBLE_COMPARISON_EPSILON);
            // Assert.assertEquals(6.506656656765106E-17, dAccdM[1], Precision.DOUBLE_COMPARISON_EPSILON);
            // Assert.assertEquals(2.8217701455184404E-17, dAccdM[2], Precision.DOUBLE_COMPARISON_EPSILON);
            //
            // // derivatives with respect to diffuse reflection coefficient
            // final double[] dAccdParam = new double[3];
            // radiativeModel.addDSRPAccDParam(spacecraftState, SolarRadiationPressure.DIFFUSION_COEFFICIENT,
            // dAccdParam);
            //
            // // comparison of dAccdParam, diffuse reflection coefficient only
            // // derivatives with respect to absorption and specular coefficients are not allowed for the spherical
            // part
            // // OREKIT results for coverage
            // Assert.assertEquals(-2.4836713469254443E-24, dAccdParam[0], Precision.DOUBLE_COMPARISON_EPSILON);
            // Assert.assertEquals(6.335944442431802E-25, dAccdParam[1], Precision.DOUBLE_COMPARISON_EPSILON);
            // Assert.assertEquals(1.4645734339944201E-24, dAccdParam[2], Precision.DOUBLE_COMPARISON_EPSILON);

        } catch (final PatriusException e) {
            Assert.fail();
        }
    }

    /**
     * General set up method.
     * 
     * @throws PatriusException
     *         if an Orekit error occurs
     */
    @BeforeClass
    public static void setUp() throws PatriusException {
        // Orekit data initialization
        Utils.setDataRoot("regular-dataPBASE");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));

        sun = CelestialBodyFactory.getSun();
        earth = new ExtendedOneAxisEllipsoid(Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS,
            Constants.GRIM5C1_EARTH_FLATTENING,
            FramesFactory.getITRF(), "Earth");

        try {
            final AssemblyBuilder b = new AssemblyBuilder();
            b.addMainPart("hello");
            new DirectRadiativeWrenchModel(b.returnAssembly());
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected!
        }
    }
}
