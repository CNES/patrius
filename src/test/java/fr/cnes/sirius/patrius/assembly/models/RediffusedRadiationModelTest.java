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
 * @history creation 12/03/2012
 *
 * HISTORY
 * VERSION:4.13:DM:DM-5:08/12/2023:[PATRIUS] Orientation d'un corps celeste sous forme de quaternions
 * VERSION:4.13:DM:DM-3:08/12/2023:[PATRIUS] Distinction entre corps celestes et barycentres
 * VERSION:4.13:DM:DM-132:08/12/2023:[PATRIUS] Suppression de la possibilite
 * de convertir les sorties de VacuumSignalPropagation
 * VERSION:4.13:FA:FA-144:08/12/2023:[PATRIUS] la methode BodyShape.getBodyFrame devrait
 * retourner un CelestialBodyFrame
 * VERSION:4.11:DM:DM-3311:22/05/2023:[PATRIUS] Evolutions mineures sur CelestialBody, shape et reperes
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration de la gestion des attractions gravitationnelles dans le propagateur
 * VERSION:4.11:FA:FA-3314:22/05/2023:[PATRIUS] Anomalie lors de l'evaluation d'un ForceModel lorsque le SpacecraftState est en ITRF
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3149:10/05/2022:[PATRIUS] Optimisation des reperes interplanetaires 
 * VERSION:4.9:DM:DM-3163:10/05/2022:[PATRIUS] Enrichissement des reperes planetaires 
 * VERSION:4.9:DM:DM-3161:10/05/2022:[PATRIUS] Ajout d'une methode getNativeFrame() a l'interface PVCoordinatesProvider 
 * VERSION:4.9:DM:DM-3165:10/05/2022:[PATRIUS] Amelioration de la propagation du signal 
 * VERSION:4.9:DM:DM-3164:10/05/2022:[PATRIUS] Amelioration de la gestion du GM (parametre gravitationnel)
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.7:FA:FA-2857:18/05/2021:Test unitaire manquant pour validation de la FA 2466 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:106:16/07/2013:Made sure parts with no radiative properties but that have a mass are taken into account
 * VERSION::FA:93:01/04/2014:Changed partial derivatives API
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::FA:270:05/09/2014:Add test with maneuver
 * VERSION::FA:271:05/09/2014:Definitions anomalies LVLH and VVLH
 * VERSION::DM:284:06/10/2014:New architecture for parameterizable Parameters
 * VERSION::DM:200:28/08/2014: dealing with a negative mass in the propagator
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::FA:461:11/06/2015:Corrected partial derivatives computation for rediffused PRS
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * VERSION::FA:673:12/09/2016: add getTotalMass(state)
 * VERSION::DM:834:04/04/2017:create vehicle object
 * VERSION::FA:1796:07/09/2018:Correction vehicle class
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.models;

import java.util.ArrayList;

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
import fr.cnes.sirius.patrius.assembly.properties.RadiativeIRProperty;
import fr.cnes.sirius.patrius.assembly.properties.RadiativeProperty;
import fr.cnes.sirius.patrius.assembly.properties.RadiativeSphereProperty;
import fr.cnes.sirius.patrius.assembly.properties.features.Facet;
import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.BodyCenterPointing;
import fr.cnes.sirius.patrius.attitudes.LofOffset;
import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyEphemeris;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.CelestialBodyIAUOrientation;
import fr.cnes.sirius.patrius.bodies.CelestialBodyOrientation;
import fr.cnes.sirius.patrius.bodies.CelestialPoint;
import fr.cnes.sirius.patrius.bodies.IAUPoleModelType;
import fr.cnes.sirius.patrius.forces.gravity.GravityModel;
import fr.cnes.sirius.patrius.forces.radiation.ElementaryFlux;
import fr.cnes.sirius.patrius.forces.radiation.IEmissivityModel;
import fr.cnes.sirius.patrius.forces.radiation.KnockeRiesModel;
import fr.cnes.sirius.patrius.forces.radiation.RediffusedRadiationPressure;
import fr.cnes.sirius.patrius.frames.CelestialBodyFrame;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Parallelepiped;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.RightCircularCylinder;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.RotationOrder;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Sphere;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.CircularOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * @description <p>
 *              Test class for the radiation pressure model.
 *              </p>
 * 
 * @author Denis Claude, Gerald Mercadier
 * 
 * @version $Id$
 * 
 * @since 1.1
 * 
 */
public class RediffusedRadiationModelTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Radiative model.
         * 
         * @featureDescription Computation of the rediffused radiation pressure acceleration.
         * 
         * @coveredRequirements DV-VEHICULE_410, DV-VEHICULE_430, DV-VEHICULE_431, DV-MOD_280
         */
        REDIFFUSED_RADIATIVE_MODEL
    }

    /**
     * A vehicle for the tests. An Assembly is created from it.
     */
    private Vehicle vehicle;

    /**
     * Main part's name
     */
    private final String mainBody = "mainBody";
    /**
     * other part's name
     */
    private final String part2 = "part2";
    /**
     * other part's name
     */
    private final String part3 = "part3";
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

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#REDIFFUSED_RADIATIVE_MODEL}
     * 
     * @testedMethod {@link RediffusedRadiativeModel #rediffusedRadiationPressureAcceleration(SpacecraftState, ElementaryFlux)}
     * @testedMethod {@link RediffusedRadiativeModel #addDAccDParamRediffusedRadiativePressure(SpacecraftState, Parameter, double[])}
     * @testedMethod {@link RediffusedRadiativeModel #addDAccDStateRediffusedRadiativePressure(SpacecraftState, double[][], double[][])}
     * 
     * @description Creation of an assembly and testing the rediffused radiation pressure
     *              acceleration. Test the computation of the derivatives with respect to state
     *              parameters and the derivatives with respect to parameter.
     * 
     * @input Assembly with radiative properties.
     * 
     * @output the rediffused radiation pressure acceleration and derivatives with respect to state
     *         parameters and to model parameters .
     * 
     * @testPassCriteria the computed acceleration and the computed derivatives are the expected
     *                   one.
     * 
     * @referenceVersion 1.1
     * @nonRegressionVersion 3.0.1
     */
    @Test
    public void radiationPressureAccelerationTest() throws PatriusException {

        Report.printMethodHeader("radiationPressureAccelerationTest",
                "Acceleration and partial derivatives computation", "Math", Precision.DOUBLE_COMPARISON_EPSILON,
                ComparisonType.ABSOLUTE);

        /**
         * Test on a model with a sphere and one facet
         */

        final AssemblyBuilder builder = new AssemblyBuilder();

        try {
            // add main part (one sphere) and part2 (one facet)
            builder.addMainPart(this.mainBody);
            builder.addPart(this.part2, this.mainBody, Transform.IDENTITY);
            builder.addPart(this.part3, this.mainBody, Transform.IDENTITY);

            // one facet
            final Vector3D normal = new Vector3D(-1.0, 1.0, -2.0);
            final Facet facet = new Facet(normal, 25 * FastMath.PI);

            // Main shape : one sphere
            // Add a Radiative cross section property with sphere
            final Sphere sphere = new Sphere(Vector3D.ZERO, 5.);
            final IPartProperty radCrossSphereProp = new RadiativeCrossSectionProperty(sphere);
            builder.addProperty(radCrossSphereProp, this.mainBody);
            final IPartProperty radFacetProp = new RadiativeFacetProperty(facet);
            builder.addProperty(radFacetProp, this.part2);

            // adding radiative properties
            final Parameter kaMain = new Parameter("kaMain", 1);
            final Parameter ksMain = new Parameter("ksMain", 0);
            final Parameter kdMain = new Parameter("kdMain", 0);
            final IPartProperty radMainProp = new RadiativeProperty(kaMain, ksMain, kdMain);
            builder.addProperty(radMainProp, this.mainBody);
            final Parameter kaPart = new Parameter("kaPart", 1);
            final Parameter ksPart = new Parameter("ksPart", 0);
            final Parameter kdPart = new Parameter("kdPart", 0);
            final IPartProperty radPartProp = new RadiativeProperty(kaPart, ksPart, kdPart);
            builder.addProperty(radPartProp, this.part2);
            final Parameter kaIRMain = new Parameter("kaIRMain", 1);
            final Parameter ksIRMain = new Parameter("ksIRMain", 1);
            final Parameter kdIRMain = new Parameter("kdIRMain", 1);
            final IPartProperty radIRMainProp = new RadiativeIRProperty(kaIRMain, ksIRMain, kdIRMain);
            builder.addProperty(radIRMainProp, this.mainBody);
            final Parameter kaIRPart = new Parameter("kaIRPart", 1);
            final Parameter ksIRPart = new Parameter("ksIRPart", 1);
            final Parameter kdIRPart = new Parameter("kdIRPart", 1);
            final IPartProperty radIRPartProp = new RadiativeIRProperty(kaIRPart, ksIRPart, kdIRPart);
            builder.addProperty(radIRPartProp, this.part2);

            // adding mass property
            final IPartProperty massMainProp = new MassProperty(1000.);
            builder.addProperty(massMainProp, this.mainBody);
            // mass of part 2 transfered to part 3. part 2 left without mass
            final IPartProperty part3Mass = new MassProperty(1000.);
            builder.addProperty(part3Mass, this.part3);

            // assembly creation
            final Assembly assembly = builder.returnAssembly();

            // rediffused radiative model
            final Parameter k0Al = new Parameter("k0Al", 0.568);
            final Parameter k0Ir = new Parameter("k0Ir", 1.857);
            RediffusedRadiativeModel radiativeModel = new RediffusedRadiativeModel(true, true, k0Al, k0Ir, assembly);

            Assert.assertEquals(true, radiativeModel.getFlagAlbedo());
            Assert.assertEquals(true, radiativeModel.getFlagIr());

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

            // compute radiation pressure acceleration
            final ElementaryFlux[] flux = new ElementaryFlux[] { new ElementaryFlux(Vector3D.PLUS_I, 1, 2) };
            assembly.initMainPartFrame(spacecraftState);
            final Vector3D computedAcc = radiativeModel.rediffusedRadiationPressureAcceleration(spacecraftState, flux);

            // Comparison
            final Vector3D expectedAcc = new Vector3D(0.39721897145366364, -0.0360528407265222206, 0.07210568145304441);
            Assert.assertEquals(expectedAcc.getX(), computedAcc.getX(), Precision.DOUBLE_COMPARISON_EPSILON);
            Assert.assertEquals(expectedAcc.getY(), computedAcc.getY(), Precision.DOUBLE_COMPARISON_EPSILON);
            Assert.assertEquals(expectedAcc.getZ(), computedAcc.getZ(), Precision.DOUBLE_COMPARISON_EPSILON);

            Report.printToReport("Acceleration", expectedAcc, computedAcc);

            // derivative
            final double[] dAccdParam = new double[3];
            radiativeModel.addDAccDParamRediffusedRadiativePressure(spacecraftState, k0Al, dAccdParam);
            Assert.assertEquals(0.05530178104689572, dAccdParam[0], Precision.DOUBLE_COMPARISON_EPSILON);
            Assert.assertEquals(0., dAccdParam[1], Precision.DOUBLE_COMPARISON_EPSILON);
            Assert.assertEquals(0., dAccdParam[2], Precision.DOUBLE_COMPARISON_EPSILON);

            Report.printToReport("Partial derivatives / albedo k0", new double[] { 0.05530178104689572, 0., 0. },
                    dAccdParam);

            radiativeModel.addDAccDParamRediffusedRadiativePressure(spacecraftState, k0Ir, dAccdParam);
            Assert.assertEquals(0.25229023544594087, dAccdParam[0], Precision.DOUBLE_COMPARISON_EPSILON);
            Assert.assertEquals(-0.01941456151132052, dAccdParam[1], Precision.DOUBLE_COMPARISON_EPSILON);
            Assert.assertEquals(0.03882912302264104, dAccdParam[2], Precision.DOUBLE_COMPARISON_EPSILON);

            Report.printToReport("Partial derivatives / IR k0", new double[] { 0.25229023544594087,
                    -0.01941456151132052, 0.03882912302264104 }, dAccdParam);

            radiativeModel.addDAccDParamRediffusedRadiativePressure(spacecraftState, kaPart, dAccdParam);
            radiativeModel.addDAccDParamRediffusedRadiativePressure(spacecraftState, kaIRPart, dAccdParam);
            Assert.assertEquals(0.2719251895308771, dAccdParam[0], Precision.DOUBLE_COMPARISON_EPSILON);
            Assert.assertEquals(-0.03904951559625673, dAccdParam[1], Precision.DOUBLE_COMPARISON_EPSILON);
            Assert.assertEquals(0.07809903119251346, dAccdParam[2], Precision.DOUBLE_COMPARISON_EPSILON);

            Report.printToReport("Partial derivatives / ka", new double[] { 0.2719251895308771, -0.03904951559625673,
                    0.07809903119251346 }, dAccdParam);

            radiativeModel.addDAccDParamRediffusedRadiativePressure(spacecraftState, kdPart, dAccdParam);
            radiativeModel.addDAccDParamRediffusedRadiativePressure(spacecraftState, kdIRPart, dAccdParam);
            radiativeModel.addDAccDParamRediffusedRadiativePressure(spacecraftState, kdMain, dAccdParam);
            radiativeModel.addDAccDParamRediffusedRadiativePressure(spacecraftState, kdIRMain, dAccdParam);
            Assert.assertEquals(-0.17195036823004045, dAccdParam[0], Precision.DOUBLE_COMPARISON_EPSILON);
            Assert.assertEquals(0.5052815383555607, dAccdParam[1], Precision.DOUBLE_COMPARISON_EPSILON);
            Assert.assertEquals(-1.0105630767111213, dAccdParam[2], Precision.DOUBLE_COMPARISON_EPSILON);

            Report.printToReport("Partial derivatives / kd", new double[] { -0.17195036823004045, 0.5052815383555607,
                    -1.0105630767111213 }, dAccdParam);

            radiativeModel.addDAccDParamRediffusedRadiativePressure(spacecraftState, ksPart, dAccdParam);
            radiativeModel.addDAccDParamRediffusedRadiativePressure(spacecraftState, ksIRPart, dAccdParam);
            Assert.assertEquals(-0.15591849535301713, dAccdParam[0], Precision.DOUBLE_COMPARISON_EPSILON);
            Assert.assertEquals(0.4892496654785374, dAccdParam[1], Precision.DOUBLE_COMPARISON_EPSILON);
            Assert.assertEquals(-0.9784993309570748, dAccdParam[2], Precision.DOUBLE_COMPARISON_EPSILON);

            Report.printToReport("Partial derivatives / ks", new double[] { -0.15591849535301713, 0.4892496654785374,
                    -0.9784993309570748 }, dAccdParam);

            // derivatives with respect to state parameters
            double[][] dAccdPos = new double[3][3];
            double[][] dAccdVel = new double[3][3];
            radiativeModel.addDAccDStateRediffusedRadiativePressure(spacecraftState, dAccdPos, dAccdVel);
            for (int i = 0; i < dAccdVel.length; i++) {
                for (int j = 0; j < dAccdVel.length; j++) {
                    Assert.assertEquals(0., dAccdPos[i][j], Precision.DOUBLE_COMPARISON_EPSILON);
                    Assert.assertEquals(0., dAccdVel[i][j], Precision.DOUBLE_COMPARISON_EPSILON);
                }
            }

            /**
             * FA461 Same test of derivatives as before : High Level test : Use of
             * RediffusedRatiationPressure
             */
            final IEmissivityModel model = new KnockeRiesModel();
            final CelestialBodyFrame itrfFrame = FramesFactory.getITRF();
            final CelestialPoint sun = CelestialBodyFactory.getSun();
            radiativeModel = new RediffusedRadiativeModel(true, true, k0Al, k0Ir, assembly);
            final RediffusedRadiationPressure force = new RediffusedRadiationPressure(sun, itrfFrame, 15, 5, model,
                    radiativeModel);

            // derivatives with respect to state parameters
            dAccdPos = new double[3][3];
            dAccdVel = new double[3][3];
            force.addDAccDState(spacecraftState, dAccdPos, dAccdVel);
            for (int i = 0; i < dAccdVel.length; i++) {
                for (int j = 0; j < dAccdVel.length; j++) {
                    Assert.assertEquals(0., dAccdPos[i][j], Precision.DOUBLE_COMPARISON_EPSILON);
                    Assert.assertEquals(0., dAccdVel[i][j], Precision.DOUBLE_COMPARISON_EPSILON);
                }
            }
        } catch (final PatriusException e) {
            Assert.fail();
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#REDIFFUSED_RADIATIVE_MODEL}
     * 
     * @testedMethod {@link RediffusedRadiativeModel#rediffusedRadiationPressureAcceleration(SpacecraftState, ElementaryFlux)}
     * 
     * @description Check that assembly orientation is properly taken into account when spacecraft state change.
     *              For that:
     *              - Sun is fixed along X axis
     *              - Emissivity model is constant
     *              - Orbit is along Z axis with two bulletin 1/2 keplerian period apart
     *              - Attitude law is body-center pointing
     * 
     * @testPassCriteria rediffused radiation pressure between the two bulletins is as expected (X, 0, Z) and (X, 0, -Z)
     * 
     * @referenceVersion 4.7
     * 
     * @nonRegressionVersion 4.7
     */
    @Test
    public void radiationPressureAssemblyTest() throws PatriusException {

        // Build assembly
        final AssemblyBuilder builder = new AssemblyBuilder();
        builder.addMainPart("Main");
        builder.addPart("Part", "Main", Transform.IDENTITY);
        // Adding radiative properties
        final Facet facet1 = new Facet(Vector3D.PLUS_K, 10);
        builder.addProperty(new RadiativeFacetProperty(facet1), "Main");
        final Parameter kaMain = new Parameter("kaMain", 1);
        final Parameter ksMain = new Parameter("ksMain", 1);
        final Parameter kdMain = new Parameter("kdMain", 1);
        final IPartProperty radMainProp = new RadiativeProperty(kaMain, ksMain, kdMain);
        builder.addProperty(radMainProp, "Main");
        builder.addProperty(radMainProp, "Part");
        // Adding mass property
        final IPartProperty massMainProp = new MassProperty(1000.);
        builder.addProperty(massMainProp, "Main");
        // Assembly creation
        final Assembly assembly = builder.returnAssembly();

        // Fixed Sun position on x-axis
        final CelestialBody sun  = new CelestialBody() {
            
            /** Serializable UID. */
            private static final long serialVersionUID = 7739138720191804961L;

            @Override
            public CelestialBodyEphemeris getEphemeris() {
                return null;
            }
            
            @Override
            public void setEphemeris(final CelestialBodyEphemeris ephemerisIn) {
                // nothing to do
            }

            @Override
            public PVCoordinates getPVCoordinates(final AbsoluteDate date,
                    final Frame frame) throws PatriusException {
                return new PVCoordinates(Vector3D.PLUS_I.scalarMultiply(1E9), Vector3D.ZERO);
            }

            @Override
            public String getName() {
                return null;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public CelestialBodyFrame getInertialFrame(final IAUPoleModelType iauPole) throws PatriusException {
                return null;
            }

            @Override
            public double getGM() {
                return 0;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public CelestialBodyFrame getRotatingFrame(final IAUPoleModelType iauPole) throws PatriusException {
                return null;
            }

            @Override
            public CelestialBodyFrame getICRF() throws PatriusException {
                return null;
            }

            @Override
            public CelestialBodyFrame getEME2000() {
                return null;
            }

            @Override
            public BodyShape getShape() {
                return null;
            }

            @Override
            public void setShape(final BodyShape shapeIn) {
                // nothing to do
            }

            @Override
            public void setGravityModel(final GravityModel gravityModelIn) {
                // nothing to do
            }

            @Override
            public GravityModel getGravityModel() {
                return null;
            }

            /** {@inheritDoc} */
            @Override
            public Frame getNativeFrame(final AbsoluteDate date) throws PatriusException {
                return null;
            }

            @Override
            public CelestialBodyIAUOrientation getOrientation() {
                return null;
            }

            @Override
            public void setOrientation(final CelestialBodyOrientation celestialBodyOrientation) {
                // nothing to do
            }

            @Override
            public void setGM(final double gmIn) {
                // nothing to do
            }

            @Override
            public CelestialBodyFrame getEclipticJ2000() throws PatriusException {
             // nothing to do
                return null;
            }
        };

        // rediffused radiative model
        final Parameter k0Al = new Parameter("k0Al", 0.568);
        final RediffusedRadiativeModel radiativeModel = new RediffusedRadiativeModel(true, false, k0Al, null, assembly);
        // Constant emissivity model
        final IEmissivityModel emissivityModel = new IEmissivityModel() {
            /**
             * 
             */
            private static final long serialVersionUID = -2879575925819764898L;

            @Override
            public double[] getEmissivity(final AbsoluteDate cdate,
                    final double latitude,
                    final double longitude) {
                return new double[] { 1., 1. };
            }
        };
        final RediffusedRadiationPressure rediffusedSRP = new RediffusedRadiationPressure(sun, FramesFactory.getGCRF(),
                100, 100, emissivityModel, radiativeModel);

        // Attitude law: body center pointing
        final AttitudeProvider attitudeProvider = new BodyCenterPointing();

        // State 1: pos = (0, 0, 7000km)
        final Orbit orbit1 = new KeplerianOrbit(7000000, 0, FastMath.PI / 2., 0, 0, FastMath.PI / 2.,
                PositionAngle.TRUE, FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH, Constants.WGS84_EARTH_MU);
        final SpacecraftState state1 = new SpacecraftState(orbit1, attitudeProvider.getAttitude(orbit1));
        assembly.initMainPartFrame(state1);

        // Compute radiation pressure acceleration
        final Vector3D acceleration1 = rediffusedSRP.computeAcceleration(state1);

        // State 2: pos = (0, 0, -7000km)
        final Orbit orbit2 = orbit1.shiftedBy(orbit1.getKeplerianPeriod() / 2.);
        final SpacecraftState state2 = new SpacecraftState(orbit2, attitudeProvider.getAttitude(orbit2));

        // Compute radiation pressure acceleration
        final Vector3D acceleration2 = rediffusedSRP.computeAcceleration(state2);

        // Check that X values are the same and Z values are opposed (which means assembly has been properly updated)
        // Since orbits are on Z axis and Sun on X axis
        // Accuracy limited due to tesselation of Earth model
        Assert.assertEquals(acceleration1.getX(), acceleration2.getX(), 1E-14);
        Assert.assertEquals(acceleration1.getZ(), -acceleration2.getZ(), 1E-7);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#REDIFFUSED_RADIATIVE_MODEL}
     * 
     * @testedMethod {@link RediffusedRadiativeModel#addDAccDParamRediffusedRadiativePressure(SpacecraftState, Parameter, double[])}
     * 
     * @description Creation of a sphere and testing the derivatives with respect to model
     *              parameters.
     * 
     * @input Sphere with radiative properties.
     * 
     * @output error when trying to compute the derivatives when trying to compute the derivatives
     *         with respect to an unknown parameter.
     * 
     * @testPassCriteria catch an error.
     * 
     * @referenceVersion 1.2
     * @nonRegressionVersion 3.0.1
     */
    @Test
    public void derivativesErrorTest() {

        // Test on a model with one sphere: error when trying to compute derivatives with respect to
        // an unknown
        // parameter.

        final AssemblyBuilder builder2 = new AssemblyBuilder();

        try {

            // add main part (one sphere)
            builder2.addMainPart(this.mainBody);

            // Main shape : one sphere
            final IPartProperty radSphereProp = new RadiativeSphereProperty(5.);
            builder2.addProperty(radSphereProp, this.mainBody);

            // adding radiative properties
            final IPartProperty radMainProp = new RadiativeProperty(1, 0, 0);
            builder2.addProperty(radMainProp, this.mainBody);

            // adding mass property
            final IPartProperty massMainProp = new MassProperty(1000.);
            builder2.addProperty(massMainProp, this.mainBody);

            // assembly creation
            final Assembly assembly = builder2.returnAssembly();

            // rediffused radiative model
            final double k0Al = 0.568;
            final double k0Ir = 1.857;
            final RediffusedRadiativeModel radiativeModel = new RediffusedRadiativeModel(false, false, k0Al, k0Ir,
                    assembly);

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

            // derivatives with respect to model parameters
            final double[] dAccdParam = new double[3];
            final Parameter param = new Parameter("NONE", 0.);
            radiativeModel.addDAccDParamRediffusedRadiativePressure(spacecraftState, param, dAccdParam);

            Assert.fail();

        } catch (final PatriusException e) {
            // expected !
        }

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#REDIFFUSED_RADIATIVE_MODEL}
     * 
     * @testedMethod {@link RediffusedRadiativeModel#rediffusedRadiationPressureAcceleration}
     * 
     * @description Creation of an assembly and testing the rediffused radiation pressure
     *              acceleration.
     * 
     * @input Assembly with rediffused radiative properties.
     * 
     * @output the rediffused radiation pressure acceleration.
     * 
     * @testPassCriteria the computed acceleration is the expected one.
     * 
     * @referenceVersion 1.2
     * @nonRegressionVersion 1.2
     */
    @Test
    public void sphereTest() {

        Report.printMethodHeader("sphereTest", "Acceleration computation (sphere)", "Orekit",
                Precision.DOUBLE_COMPARISON_EPSILON, ComparisonType.ABSOLUTE);

        /**
         * Test on a model with a sphere and compare with the SphericalSpacecraft model
         */

        final AssemblyBuilder builder = new AssemblyBuilder();

        try {
            // add main part: one sphere
            builder.addMainPart(this.mainBody);

            // Main shape : one sphere
            // Add a Radiative cross section property with sphere
            final Sphere sphere = new Sphere(Vector3D.ZERO, 5.);
            final IPartProperty radCrossSphereProp = new RadiativeCrossSectionProperty(sphere);
            builder.addProperty(radCrossSphereProp, this.mainBody);

            // adding radiative properties
            final double mainPartAbsCoef = 0.5;
            final double mainPartSpeCoef = 0.2;
            final double mainPartDifCoef = 0.3;
            final IPartProperty radMainProp = new RadiativeProperty(mainPartAbsCoef, mainPartSpeCoef, mainPartDifCoef);
            builder.addProperty(radMainProp, this.mainBody);
            final IPartProperty radIRMainProp = new RadiativeIRProperty(mainPartAbsCoef, mainPartSpeCoef,
                    mainPartDifCoef);
            builder.addProperty(radIRMainProp, this.mainBody);
            // adding mass property
            final double mainPartMass = 100.;
            final IPartProperty massMainProp = new MassProperty(mainPartMass);
            builder.addProperty(massMainProp, this.mainBody);

            // assembly creation
            final Assembly assembly = builder.returnAssembly();

            // spacecraft
            final AbsoluteDate date = new AbsoluteDate();
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
                    Rotation.IDENTITY, Vector3D.ZERO), new MassModel(assembly));

            // radiative model
            final RediffusedRadiativeModel radiativeModel = new RediffusedRadiativeModel(true, false, 1, 1, assembly);
            // compute radiation pressure acceleration
            final ElementaryFlux[] flux = new ElementaryFlux[] { new ElementaryFlux(Vector3D.PLUS_I, 10, 20) };
            assembly.initMainPartFrame(spacecraftState);
            final Vector3D computedAcc = radiativeModel.rediffusedRadiationPressureAcceleration(spacecraftState, flux);

            // Comparison
            Assert.assertEquals(8.901179185171081, computedAcc.getX(), Precision.DOUBLE_COMPARISON_EPSILON);
            Assert.assertEquals(0., computedAcc.getY(), Precision.DOUBLE_COMPARISON_EPSILON);
            Assert.assertEquals(0., computedAcc.getZ(), Precision.DOUBLE_COMPARISON_EPSILON);

            Report.printToReport("Acceleration", new Vector3D(8.901179185171081, 0., 0.), computedAcc);

        } catch (final PatriusException e) {
            Assert.fail();
        }

    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#REDIFFUSED_RADIATIVE_MODEL}
     * 
     * @testedMethod {@link RediffusedRadiativeModel#rediffusedRadiationPressureAcceleration(SpacecraftState, ElementaryFlux)}
     * 
     * @description Creation of an assembly and testing the rediffused radiation pressure
     *              acceleration for the cylinder case.
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
    public void rediffusedRadiationPressureAccCylinderTest() throws PatriusException {

        Report.printMethodHeader("rediffusedRadiationPressureAccCylinderTest", "Acceleration computation", "Math",
                Precision.DOUBLE_COMPARISON_EPSILON, ComparisonType.ABSOLUTE);

        // --------------------------------- Assembly with cylinder only ---------------------- //

        // Test values
        final double mass = 1000.;
        final double albPress = 10.;
        final double irPress = 20.;
        final double albFact = 2.;
        final double irFact = 2.;
        final double radius = 2.;
        final double heigh = 10.;
        final double area = 10.;
        final double kd = 0.25;
        final double ka = 0.5;
        final double ks = 0.25;
        final double kdIr = 0.25;
        final double kaIr = 0.5;
        final double ksIr = 0.25;

        // Create the Assembly
        final Assembly testAssembly = this.createTestAssemblyVehicleCylinderOnly(radius, heigh, mass, ka, kd, ks, kaIr,
                kdIr, ksIr);

        // Rediffused radiative model with albedo only
        final RediffusedRadiativeModel model = new RediffusedRadiativeModel(true, false, albFact, 0, testAssembly);

        // orbit
        final double mu = Constants.EGM96_EARTH_MU;
        final AbsoluteDate date = new AbsoluteDate(2012, 4, 2, 15, 3, 0, TimeScalesFactory.getTAI());
        final double a = 10000000;
        final Orbit testOrbit = new KeplerianOrbit(a, 0.93, MathLib.toRadians(75), 0, 0, 0, PositionAngle.MEAN,
                FramesFactory.getGCRF(), date, mu);
        // spacecraft
        final Attitude attitude = new LofOffset(testOrbit.getFrame(), LOFType.LVLH).getAttitude(testOrbit,
                testOrbit.getDate(), testOrbit.getFrame());
        final SpacecraftState bogusState = new SpacecraftState(testOrbit, attitude, new MassModel(testAssembly));
        final SpacecraftState shiftedBogusState = bogusState.shiftedBy(10.);

        // rotation from the reference frame to the spacecraft frame
        final Rotation toSatRotation = shiftedBogusState.getAttitude().getRotation();

        // Test case : flux is PLUS_I
        Vector3D flux = Vector3D.PLUS_I;
        flux = toSatRotation.applyTo(flux);
        Vector3D fluxSat = flux.negate();

        // Elementary flux
        ElementaryFlux[] elemFlux = new ElementaryFlux[] { new ElementaryFlux(flux, albPress, irPress) };

        // Compute radiation pressure acceleration
        Vector3D radAcc = model.rediffusedRadiationPressureAcceleration(shiftedBogusState, elemFlux);

        // Acceleration is as expected : along flux with expected norm
        double crossSection = radius * radius * heigh;
        // crossSection * - (1 + 4 kd / 9) * albedo_pressure
        double cylFactor = -crossSection * (1. + ((4. / 9.) * kd)) * (albPress / mass);

        // Expected acc is opposite to flux, with total factor 2 * cylFactor because albedo factor
        // is 2.
        Vector3D expectedAcc = fluxSat.scalarMultiply(2. * cylFactor);

        // Comparison
        Assert.assertEquals(expectedAcc.distance(radAcc), 0., Precision.DOUBLE_COMPARISON_EPSILON);

        // Test case : relative velocity is PLUS_I + PLUS_K
        // (cylinder from 45° inclination)

        // Call the dragAcceleration method
        flux = Vector3D.PLUS_I.add(Vector3D.PLUS_K);
        flux = toSatRotation.applyTo(flux);
        fluxSat = flux.negate();
        elemFlux = new ElementaryFlux[] { new ElementaryFlux(flux, albPress, irPress) };

        // Compute radiation pressure acceleration
        radAcc = model.rediffusedRadiationPressureAcceleration(shiftedBogusState, elemFlux);

        // Acceleration is as expected : along flux with expected norm
        crossSection = (FastMath.PI * radius * radius * MathLib.sqrt(2.) / 2. + radius * radius * heigh
                * MathLib.sqrt(2.) / 2.);
        // crossSection * - (1 + 4 kd / 9) * albedo_pressure
        cylFactor = -crossSection * (1. + ((4. / 9.) * kd)) * (albPress / mass);
        expectedAcc = fluxSat.scalarMultiply(albFact * cylFactor);

        // Comparison
        Assert.assertEquals(expectedAcc.distance(radAcc), 0., Precision.DOUBLE_COMPARISON_EPSILON);

        // --------------------------------- Assembly with panels ---------------------- //

        // Create the Assembly
        final Assembly assemblyPanels = this.createTestAssemblyVehicleCylinderWithPanels(radius, heigh, mass, area, ka,
                kd, ks);

        // Rediffused radiative model with albedo and IR
        final RediffusedRadiativeModel modelPanels = new RediffusedRadiativeModel(true, true, albFact, irFact,
                assemblyPanels);

        // Test case : relative velocity is MINUS_I + PLUS_K (norm sqrt(2))
        // in the satellite frame.
        // the only radiation force to be computed for facet is for the one having normal
        // I + K (negative orientation wrt velocity).
        // However, this force is (0., 0., 0.) because facet normal and velocity are orthogonal.
        flux = Vector3D.MINUS_I.add(Vector3D.PLUS_K);
        flux = toSatRotation.applyTo(flux);
        elemFlux = new ElementaryFlux[] { new ElementaryFlux(flux, albPress, irPress) };

        // Compute radiation pressure acceleration
        radAcc = modelPanels.rediffusedRadiationPressureAcceleration(shiftedBogusState, elemFlux);

        // Expected acceleration : aligned with velocity since force on cylinder and force on facet
        // are both aligned with velocity
        Assert.assertEquals(0., Vector3D.angle(flux, radAcc), Precision.DOUBLE_COMPARISON_EPSILON);

        // Test case : relative velocity is PLUS_I
        // the only force to be computed for facet is for the one having normal
        // -I + K (negative orientation wrt velocity)
        flux = Vector3D.PLUS_I;
        flux = toSatRotation.applyTo(flux);
        fluxSat = flux.negate();
        elemFlux = new ElementaryFlux[] { new ElementaryFlux(flux, albPress, irPress) };

        // Expected : normal and tangent as computed and along the right components
        // Remember : facet area is 10; spacecraft mass is 1000
        // Albedo contribution + IR contribution computed and added to obtain final acceleration

        // //////////////// Albedo part /////////////////////

        // Cylinder contribution
        crossSection = radius * radius * heigh;
        cylFactor = -crossSection * (1. + ((4. / 9.) * kd)) * (albPress / mass);

        // Facet normal computed in spacecraft's frame from part's frame :
        // part is solar panel with normal -I + K as expected
        Vector3D n = Vector3D.MINUS_I.add(Vector3D.PLUS_K);
        final Transform t = assemblyPanels.getPart("Solar panel0").getFrame()
                .getTransformTo(shiftedBogusState.getFrame(), date);
        n = t.transformVector(n).normalize();

        // Facet contribution : compute normal and tangential radiation for the facet
        // compute angle between normal facet in state's frame and flux sat
        final double cosAngle = n.dotProduct(fluxSat);
        final double valueForceAlbN = 2. * (-cosAngle * area * (albPress / mass)) * (ks * cosAngle + (kd / 3.));
        final double valueForceAlbT = (-cosAngle * area * (albPress / mass)) * (kd + ka);

        // Compute radiation pressure acceleration
        radAcc = modelPanels.rediffusedRadiationPressureAcceleration(shiftedBogusState, elemFlux);

        // Add normal and tang components, with albedo multiplicative coeff = 2.
        final Vector3D normalAlbVect = new Vector3D(valueForceAlbN, n);
        final Vector3D tangAlbVect = flux.negate().scalarMultiply(valueForceAlbT);
        Vector3D valueFacet = normalAlbVect.add(tangAlbVect).scalarMultiply(albFact);

        // Total albedo acc : use albedo coeff = 2.
        final Vector3D expectedAlbedoAcc = valueFacet.add(albFact * cylFactor, fluxSat);

        // //////////////// IR part /////////////////////
        cylFactor = -crossSection * (1. + ((4. / 9.) * kd)) * (irPress / mass);
        final double valueForceIrN = 2. * (-cosAngle * area * (irPress / mass)) * (ks * cosAngle + (kd / 3.));
        final double valueForceIrT = (-cosAngle * area * (irPress / mass)) * (kd + ka);

        // Add normal and tang components, with IR multiplicative coeff = 2.
        final Vector3D normalIrVect = new Vector3D(valueForceIrN, n);
        final Vector3D tangIrVect = flux.negate().scalarMultiply(valueForceIrT);
        valueFacet = normalIrVect.add(tangIrVect).scalarMultiply(irFact);

        // Total IR acc : use K0 IR coeff = 2.
        final Vector3D expectedIrAcc = valueFacet.add(irFact * cylFactor, fluxSat);

        // Comparisons : expected an acceleration composed of facet contribution along facet's
        // normal
        // and the sum of facet and cylinder contributions on flux direction (+I)
        // The total acceleration is albedo + IR
        expectedAcc = expectedAlbedoAcc.add(expectedIrAcc);

        Assert.assertEquals(radAcc.distance(expectedAcc), 0., Precision.DOUBLE_COMPARISON_EPSILON);
        Report.printToReport("Acceleration norm", expectedAcc.getNorm(), radAcc.getNorm());
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#REDIFFUSED_RADIATIVE_MODEL}
     * 
     * @testedMethod {@link RediffusedRadiativeModel#rediffusedRadiationPressureAcceleration(SpacecraftState, ElementaryFlux)}
     * 
     * @description Creation of an assembly and testing the rediffused radiation pressure
     *              acceleration for the parallelepiped case.
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
    public void rediffusedRadiationPressureAccParallTest() throws PatriusException {

        Report.printMethodHeader("rediffusedRadiationPressureAccParallTest", "Acceleration computation", "Math",
                Precision.DOUBLE_COMPARISON_EPSILON, ComparisonType.ABSOLUTE);

        // --------------------------------- Assembly with parallelepiped only
        // ---------------------- //

        // Test values
        final double mass = 1000.;
        final double albPress = 10.;
        final double irPress = 20.;
        final double albFact = 2.;
        final double irFact = 2.;
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
        // Create the Assembly
        final Assembly testAssembly = this.createTestAssemblyVehicleParal(length, width, heigh, mass, ka, kd, ks, kaIr,
                kdIr, ksIr);

        // Rediffused radiative model with albedo only
        final RediffusedRadiativeModel model = new RediffusedRadiativeModel(true, false, albFact, 0, testAssembly);

        // orbit
        final double mu = Constants.EGM96_EARTH_MU;
        final AbsoluteDate date = new AbsoluteDate(2012, 4, 2, 15, 3, 0, TimeScalesFactory.getTAI());
        final double a = 10000000;
        final Orbit testOrbit = new KeplerianOrbit(a, 0.93, MathLib.toRadians(75), 0, 0, 0, PositionAngle.MEAN,
                FramesFactory.getGCRF(), date, mu);
        // spacecraft
        final Attitude attitude = new LofOffset(testOrbit.getFrame(), LOFType.LVLH).getAttitude(testOrbit,
                testOrbit.getDate(), testOrbit.getFrame());
        final SpacecraftState bogusState = new SpacecraftState(testOrbit, attitude, new MassModel(testAssembly));
        final SpacecraftState shiftedBogusState = bogusState.shiftedBy(10.);

        // rotation from the reference frame to the spacecraft frame
        final Rotation toSatRotation = shiftedBogusState.getAttitude().getRotation();

        // Test case : flux is PLUS_I
        Vector3D flux = Vector3D.PLUS_I;
        flux = toSatRotation.applyTo(flux);
        Vector3D fluxSat = flux.negate();

        // Elementary flux
        ElementaryFlux[] elemFlux = new ElementaryFlux[] { new ElementaryFlux(flux, albPress, irPress) };

        // Compute radiation pressure acceleration
        Vector3D radAcc = model.rediffusedRadiationPressureAcceleration(shiftedBogusState, elemFlux);

        // Acceleration is as expected : along flux with expected norm
        double crossSection = length * heigh;
        // crossSection * - (1 + 4 kd / 9) * albedo_pressure
        double paralFactor = -crossSection * (1. + ((4. / 9.) * kd)) * (albPress / mass);

        // Expected acc is opposite to flux, with total factor 2 * cylFactor because albedo factor
        // is 2.
        Vector3D expectedAcc = fluxSat.scalarMultiply(2. * paralFactor);

        // Comparison
        Assert.assertEquals(expectedAcc.distance(radAcc), 0., Precision.DOUBLE_COMPARISON_EPSILON);

        // Test case : random direction is Vector3D(2.0, -2.0, 2.0)
        // in the satellite frame.
        flux = new Vector3D(2.0, -2.0, 2.0);
        flux = toSatRotation.applyTo(flux);
        fluxSat = flux.negate();
        elemFlux = new ElementaryFlux[] { new ElementaryFlux(flux, albPress, irPress) };

        // Compute radiation pressure acceleration
        radAcc = model.rediffusedRadiationPressureAcceleration(shiftedBogusState, elemFlux);

        // Acceleration is as expected : along flux with expected norm
        crossSection = 28. / MathLib.sqrt(3.);
        // crossSection * - (1 + 4 kd / 9) * albedo_pressure
        paralFactor = -crossSection * (1. + ((4. / 9.) * kd)) * (albPress / mass);
        expectedAcc = fluxSat.scalarMultiply(albFact * paralFactor);

        // Comparison
        Assert.assertEquals(expectedAcc.distance(radAcc), 0., Precision.DOUBLE_COMPARISON_EPSILON);

        // --------------------------------- Assembly with panels ---------------------- //

        // Create the Assembly
        final Assembly assemblyPanels = this.createTestAssemblyVehicleParalWithPanels(length, width, heigh, mass, area,
                ka, kd, ks);

        // Rediffused radiative model with albedo and IR
        final RediffusedRadiativeModel modelPanels = new RediffusedRadiativeModel(true, true, albFact, irFact,
                assemblyPanels);

        // Test case : relative velocity is MINUS_I + PLUS_K (norm sqrt(2))
        // in the satellite frame.
        // the only radiation force to be computed for facet is for the one having normal
        // I + K (negative orientation wrt velocity).
        // However, this force is (0., 0., 0.) because facet normal and velocity are orthogonal.
        flux = Vector3D.MINUS_I.add(Vector3D.PLUS_K);
        flux = toSatRotation.applyTo(flux);
        elemFlux = new ElementaryFlux[] { new ElementaryFlux(flux, albPress, irPress) };

        // Compute radiation pressure acceleration
        radAcc = modelPanels.rediffusedRadiationPressureAcceleration(shiftedBogusState, elemFlux);

        // Expected acceleration : aligned with velocity since force on cylinder and force on facet
        // are both aligned with velocity
        Assert.assertEquals(0., Vector3D.angle(flux, radAcc), Precision.DOUBLE_COMPARISON_EPSILON);

        // Test case : relative velocity is PLUS_I
        // the only force to be computed for facet is for the one having normal
        // -I + K (negative orientation wrt velocity)
        flux = Vector3D.PLUS_I;
        flux = toSatRotation.applyTo(flux);
        fluxSat = flux.negate();
        elemFlux = new ElementaryFlux[] { new ElementaryFlux(flux, albPress, irPress) };

        // Expected : normal and tangent as computed and along the right components
        // Remember : facet area is 10; spacecraft mass is 1000
        // Albedo contribution + IR contribution computed and added to obtain final acceleration

        // //////////////// Albedo part /////////////////////

        // Parallelepiped contribution
        crossSection = length * heigh;
        paralFactor = -crossSection * (1. + ((4. / 9.) * kd)) * (albPress / mass);

        // Facet normal computed in spacecraft's frame from part's frame :
        // part is solar panel with normal -I + K as expected
        Vector3D n = Vector3D.MINUS_I.add(Vector3D.PLUS_K);
        final Transform t = assemblyPanels.getPart("Solar panel0").getFrame()
                .getTransformTo(shiftedBogusState.getFrame(), date);
        n = t.transformVector(n).normalize();

        // Facet contribution : compute normal and tangential radiation for the facet
        // Compute angle between facet in state's frame and flux sate
        final double cosAngle = n.dotProduct(fluxSat);
        final double valueForceAlbN = 2. * (-cosAngle * area * (albPress / mass)) * (ks * cosAngle + (kd / 3.));
        final double valueForceAlbT = (-cosAngle * area * (albPress / mass)) * (kd + ka);

        // Compute radiation pressure acceleration
        radAcc = modelPanels.rediffusedRadiationPressureAcceleration(shiftedBogusState, elemFlux);

        // Add normal and tang components, with albedo multiplicative coeff = 2.
        final Vector3D normalAlbVect = new Vector3D(valueForceAlbN, n);
        final Vector3D tangAlbVect = flux.negate().scalarMultiply(valueForceAlbT);
        Vector3D valueFacet = normalAlbVect.add(tangAlbVect).scalarMultiply(albFact);

        // Total albedo acc : use albedo coeff = 2.
        final Vector3D expectedAlbedoAcc = valueFacet.add(albFact * paralFactor, fluxSat);

        // //////////////// IR part /////////////////////
        paralFactor = -crossSection * (1. + ((4. / 9.) * kd)) * (irPress / mass);
        final double valueForceIrN = 2. * (-cosAngle * area * (irPress / mass)) * (ks * cosAngle + (kd / 3.));
        final double valueForceIrT = (-cosAngle * area * (irPress / mass)) * (kd + ka);

        // Add normal and tang components, with IR multiplicative coeff = 2.
        final Vector3D normalIrVect = new Vector3D(valueForceIrN, n);
        final Vector3D tangIrVect = flux.negate().scalarMultiply(valueForceIrT);
        valueFacet = normalIrVect.add(tangIrVect).scalarMultiply(irFact);

        // Total IR acc : use K0 IR coeff = 2.
        final Vector3D expectedIrAcc = valueFacet.add(irFact * paralFactor, fluxSat);

        // Comparisons : expected an acceleration composed of facet contribution along facet's
        // normal
        // and the sum of facet and parallelepiped contributions on flux direction (+I)
        // The total acceleration is albedo + IR
        expectedAcc = expectedAlbedoAcc.add(expectedIrAcc);

        Assert.assertEquals(radAcc.distance(expectedAcc), 0., Precision.DOUBLE_COMPARISON_EPSILON);
        Report.printToReport("Acceleration norm", expectedAcc.getNorm(), radAcc.getNorm());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#REDIFFUSED_RADIATIVE_MODEL}
     * 
     * @testedMethod {@link DirectRadiativeModel#radiationPressureAcceleration(SpacecraftState, Vector3D)}
     * 
     * @description Creation of an assembly and testing the rediffused radiation pressure
     *              acceleration.
     * 
     * @input Assembly with rediffused radiative properties.
     * 
     * @output the rediffused radiation pressure acceleration.
     * 
     * @testPassCriteria the computed acceleration is the expected one.
     * 
     * @referenceVersion 1.2
     * @nonRegressionVersion 1.2
     */
    @Test
    public void boxTest() {

        Report.printMethodHeader("boxTest", "Acceleration computation (box and array)", "Orekit",
                Precision.DOUBLE_COMPARISON_EPSILON, ComparisonType.ABSOLUTE);

        /**
         * Test on a model with a box and compare with the BoxAndSolarArraySpacecraft model
         */

        final AssemblyBuilder builder = new AssemblyBuilder();

        try {
            // add main part
            builder.addMainPart(this.mainBody);

            // add faces of the box
            builder.addPart(this.facePlusX, this.mainBody, Transform.IDENTITY);
            builder.addPart(this.faceMinusX, this.mainBody, Transform.IDENTITY);
            builder.addPart(this.facePlusY, this.mainBody, Transform.IDENTITY);
            builder.addPart(this.faceMinusY, this.mainBody, Transform.IDENTITY);
            builder.addPart(this.facePlusZ, this.mainBody, Transform.IDENTITY);
            builder.addPart(this.faceMinusZ, this.mainBody, Transform.IDENTITY);

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
            final double mainPartAbsCoef = 0.5;
            final double mainPartSpeCoef = 0.3;
            final double mainPartDifCoef = 0.2;
            final IPartProperty radMainProp = new RadiativeProperty(mainPartAbsCoef, mainPartSpeCoef, mainPartDifCoef);
            final IPartProperty radIRMainProp = new RadiativeIRProperty(mainPartAbsCoef, mainPartSpeCoef,
                    mainPartDifCoef);

            builder.addProperty(radMainProp, this.facePlusX);
            builder.addProperty(radMainProp, this.faceMinusX);
            builder.addProperty(radMainProp, this.facePlusY);
            builder.addProperty(radMainProp, this.faceMinusY);
            builder.addProperty(radMainProp, this.facePlusZ);
            builder.addProperty(radMainProp, this.faceMinusZ);
            builder.addProperty(radIRMainProp, this.facePlusX);
            builder.addProperty(radIRMainProp, this.faceMinusX);
            builder.addProperty(radIRMainProp, this.facePlusY);
            builder.addProperty(radIRMainProp, this.faceMinusY);
            builder.addProperty(radIRMainProp, this.facePlusZ);
            builder.addProperty(radIRMainProp, this.faceMinusZ);
            // add mass property
            final double totalMass = 100.;
            final double faceMass = totalMass / 6;
            final IPartProperty massFaceProp = new MassProperty(faceMass);

            // builder.addProperty(new MassProperty(0.), mainBody);
            builder.addProperty(massFaceProp, this.facePlusX);
            builder.addProperty(massFaceProp, this.faceMinusX);
            builder.addProperty(massFaceProp, this.facePlusY);
            builder.addProperty(massFaceProp, this.faceMinusY);
            builder.addProperty(massFaceProp, this.facePlusZ);
            builder.addProperty(massFaceProp, this.faceMinusZ);

            // assembly creation
            final Assembly assembly = builder.returnAssembly();
            // spacecraft
            final AbsoluteDate date = new AbsoluteDate();
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
            // creation of the spacecraft
            final SpacecraftState spacecraftState = new SpacecraftState(orbit, new Attitude(date, referenceFrame,
                    new Rotation(Vector3D.PLUS_K, -FastMath.PI / 2), Vector3D.ZERO), new MassModel(assembly));

            // radiative model
            assembly.initMainPartFrame(spacecraftState);
            final RediffusedRadiativeModel radiativeModel = new RediffusedRadiativeModel(true, true, 1, 1, assembly);

            // compute radiation pressure acceleration
            final ElementaryFlux[] flux = new ElementaryFlux[] { new ElementaryFlux(new Vector3D(0., 0., -1.), 10, 20) };
            final Vector3D computedAcc = radiativeModel.rediffusedRadiationPressureAcceleration(spacecraftState, flux);

            // Comparison
            Assert.assertEquals(0., computedAcc.getX(), Precision.DOUBLE_COMPARISON_EPSILON);
            Assert.assertEquals(0., computedAcc.getY(), Precision.DOUBLE_COMPARISON_EPSILON);
            Assert.assertEquals(-1.7199999999999998, computedAcc.getZ(), Precision.DOUBLE_COMPARISON_EPSILON);

            Report.printToReport("Acceleration", new Vector3D(0, 0, -1.7199999999999998), computedAcc);

        } catch (final PatriusException e) {
            Assert.fail();
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#REDIFFUSED_RADIATIVE_MODEL}
     * 
     * @testedMethod {@link RediffusedRadiativeModel#RediffusedRadiativeModel(boolean, boolean, double, double, Assembly)}
     * 
     * @description Creation of an assembly and testing the exception.
     * 
     * @input Assembly with radiative properties.
     * 
     * @output test an error.
     * 
     * @testPassCriteria a RuntimeException is thrown.
     * 
     * @referenceVersion 1.2
     * @nonRegressionVersion 1.2
     */
    @Test
    public void error1Test() {

        final AssemblyBuilder builder = new AssemblyBuilder();

        try {
            // add main part: one sphere
            builder.addMainPart(this.mainBody);

            final IPartProperty radSphereProp = new RadiativeSphereProperty(5.);
            builder.addProperty(radSphereProp, this.mainBody);

            // adding radiative properties
            final double mainPartAbsCoef = 0.5;
            final double mainPartSpeCoef = 0.2;
            final double mainPartDifCoef = 0.3;
            final IPartProperty radMainProp = new RadiativeProperty(mainPartAbsCoef, mainPartSpeCoef, mainPartDifCoef);
            builder.addProperty(radMainProp, this.mainBody);
            final IPartProperty radIRMainProp = new RadiativeIRProperty(mainPartAbsCoef, mainPartSpeCoef,
                    mainPartDifCoef);
            builder.addProperty(radIRMainProp, this.mainBody);
            // adding mass property
            // mass
            final double mainPartMass = 0.;
            final IPartProperty massMainProp = new MassProperty(mainPartMass);
            builder.addProperty(massMainProp, this.mainBody);

            // assembly creation
            final Assembly assembly = builder.returnAssembly();

            new RediffusedRadiativeModel(true, true, 0, 0, assembly);
            Assert.fail();

        } catch (final RuntimeException e) {
            // expected !
        } catch (final PatriusException e) {
            Assert.fail();
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#REDIFFUSED_RADIATIVE_MODEL}
     * 
     * @testedMethod {@link RediffusedRadiativeModel #RediffusedRadiativeModel(boolean, boolean, double, double, Assembly)}
     * 
     * @description Creation of an assembly and testing the exception.
     * 
     * @input Assembly with radiative properties.
     * 
     * @output test an error.
     * 
     * @testPassCriteria a RuntimeException is thrown.
     * 
     * @referenceVersion 1.2
     * @nonRegressionVersion 1.2
     */
    @Test
    public void error2Test() {

        final AssemblyBuilder builder = new AssemblyBuilder();

        try {
            // add main part: one sphere
            builder.addMainPart(this.mainBody);

            final IPartProperty radSphereProp = new RadiativeSphereProperty(5.);

            builder.addProperty(radSphereProp, this.mainBody);
            final Vector3D normal = new Vector3D(0.0, 0.0, -2.0);
            final Facet facet = new Facet(normal, 25 * FastMath.PI);

            final IPartProperty radFacetProp = new RadiativeFacetProperty(facet);
            builder.addProperty(radFacetProp, this.mainBody);

            // assembly creation
            final Assembly assembly = builder.returnAssembly();

            new RediffusedRadiativeModel(true, true, 1, 1, assembly);
            Assert.fail();

        } catch (final RuntimeException e) {
            // expected !
        }
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#REDIFFUSED_RADIATIVE_MODEL}
     * 
     * @testedMethod {@link RediffusedRadiativeModel#RediffusedRadiativeModel(boolean, boolean, double, double, Assembly)}
     * @testedMethod {@link RediffusedRadiativeModel#getJacobianParameters()}
     * 
     * @description Test method getJacobianParameters
     * 
     * @input RediffusedRadiativeModel
     * 
     * @output parameters
     * 
     * @testPassCriteria the jacobian parameters should be as expected
     * 
     * @comments Test for coverage purpose
     * 
     * @referenceVersion 2.3
     * 
     * @nonRegressionVersion 3.0.1
     */
    @Test
    public void testGetJacobianParameters() throws PatriusException {

        final AssemblyBuilder builder = new AssemblyBuilder();
        builder.addMainPart(this.mainBody);
        final IPartProperty radSphereProp = new RadiativeSphereProperty(5.);
        builder.addProperty(radSphereProp, this.mainBody);
        // adding radiative properties
        final IPartProperty radMainProp = new RadiativeProperty(1, 2, 3);
        builder.addProperty(radMainProp, this.mainBody);
        final Parameter kaIR = new Parameter("kaIR", 4.);
        final Parameter ksIR = new Parameter("ksIR", 5.);
        final Parameter kdIR = new Parameter("kdIR", 6.);
        final IPartProperty radIRMainProp = new RadiativeIRProperty(kaIR, ksIR, kdIR);
        builder.addProperty(radIRMainProp, this.mainBody);
        // adding mass property
        final IPartProperty massMainProp = new MassProperty(1000.);
        builder.addProperty(massMainProp, this.mainBody);

        // assembly creation
        final Assembly assembly = builder.returnAssembly();

        // rediffused radiative model
        final double k0Al = 0.568;
        final double k0Ir = 1.857;
        final RediffusedRadiativeModel radiativeModel = new RediffusedRadiativeModel(false, false, k0Al, k0Ir, assembly);
        final ArrayList<Parameter> parameters = radiativeModel.getJacobianParameters();
        final int size = parameters.size();
        Assert.assertEquals(8, size);
        /** Parameter name for K0 albedo global coefficient. */
        final String K0ALBEDO_COEFFICIENT = "K0 albedo coefficient";
        /** Parameter name for K0 infrared global coefficient. */
        final String K0IR_COEFFICIENT = "K0 infrared coefficient";
        /** Parameter name for absorption coefficient. */
        final String ABSORPTION_COEFFICIENT = "absorption coefficient";
        /** Parameter name for specular coefficient. */
        final String SPECULAR_COEFFICIENT = "specular reflection coefficient";
        /** Parameter name for diffusion coefficient. */
        final String DIFFUSION_COEFFICIENT = "diffusion reflection coefficient";

        for (int i = 0; i < size; i++) {
            final Parameter param = parameters.get(i);
            if (param.getName().equals(K0ALBEDO_COEFFICIENT)) {
                Assert.assertEquals(k0Al, param.getValue(), Precision.DOUBLE_COMPARISON_EPSILON);
            } else if (param.getName().equals(K0IR_COEFFICIENT)) {
                Assert.assertEquals(k0Ir, param.getValue(), Precision.DOUBLE_COMPARISON_EPSILON);
            } else if (param.getName().equals(ABSORPTION_COEFFICIENT)) {
                Assert.assertEquals(1., param.getValue(), Precision.DOUBLE_COMPARISON_EPSILON);
            } else if (param.getName().equals(SPECULAR_COEFFICIENT)) {
                Assert.assertEquals(2., param.getValue(), Precision.DOUBLE_COMPARISON_EPSILON);
            } else if (param.getName().equals(DIFFUSION_COEFFICIENT)) {
                Assert.assertEquals(3., param.getValue(), Precision.DOUBLE_COMPARISON_EPSILON);
            } else if (param.getName().equals(kaIR.getName())) {
                Assert.assertEquals(4., param.getValue(), Precision.DOUBLE_COMPARISON_EPSILON);
            } else if (param.getName().equals(ksIR.getName())) {
                Assert.assertEquals(5., param.getValue(), Precision.DOUBLE_COMPARISON_EPSILON);
            } else if (param.getName().equals(kdIR.getName())) {
                Assert.assertEquals(6., param.getValue(), Precision.DOUBLE_COMPARISON_EPSILON);
            } else {
                Assert.assertTrue(false);
            }
        }
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
     * @param ksIr ks infrared coeff
     * @param kdIr kd infrared coeff
     * @param kaIr ka infrared coeff
     * @throws PatriusException
     */
    private Assembly createTestAssemblyVehicleCylinderOnly(final double radius,
            final double heigh,
            final double mass,
            final double ka,
            final double kd,
            final double ks,
            final double kaIr,
            final double kdIr,
            final double ksIr) throws PatriusException {

        // Creates the vehicle
        this.vehicle = new Vehicle();

        // cylinder property
        final RightCircularCylinder cylinder = new RightCircularCylinder(new Vector3D(10, 20, 30), Vector3D.PLUS_K,
                radius, heigh);

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
     * @param radius cylinder radius
     * @param heigh cylinder heigh
     * @param mass cylinder mass
     * @param area panels area
     * @param ka ka coeff
     * @param kd kd coeff
     * @param ks ks coeff
     * @throws PatriusException
     */
    private Assembly createTestAssemblyVehicleCylinderWithPanels(final double radius,
            final double heigh,
            final double mass,
            final double area,
            final double ka,
            final double kd,
            final double ks) throws PatriusException {
        
        // Creates the vehicle
        this.vehicle = new Vehicle();

        // cylinder property
        final RightCircularCylinder cylinder = new RightCircularCylinder(new Vector3D(10, 20, 30), Vector3D.PLUS_K,
                radius, heigh);

        this.vehicle.setMainShape(cylinder);
        this.vehicle.setDryMass(mass);

        // add solar panels
        this.vehicle.addSolarPanel(Vector3D.PLUS_I.add(Vector3D.PLUS_K), area);
        this.vehicle.addSolarPanel(Vector3D.MINUS_I.add(Vector3D.PLUS_K), area);

        // Radiative properties : visible + IR
        this.vehicle.setRadiativeProperties(ka, ks, kd, ka, ks, kd);

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
     * @param ksIr ks infrared coeff
     * @param kdIr kd infrared coeff
     * @param kaIr ka infrared coeff
     * @throws PatriusException
     */
    private Assembly createTestAssemblyVehicleParal(final double lenght,
            final double width,
            final double heigh,
            final double mass,
            final double ka,
            final double kd,
            final double ks,
            final double kaIr,
            final double kdIr,
            final double ksIr) throws PatriusException {

        // Creates the vehicle
        this.vehicle = new Vehicle();

        // Creation of a parallelepiped
        final Vector3D center = Vector3D.ZERO;
        final Vector3D uVector = Vector3D.PLUS_I;
        final Vector3D inputvVector = Vector3D.PLUS_J;
        final Parallelepiped parallelepiped = new Parallelepiped(center, uVector, inputvVector, lenght, width, heigh);

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
     * @throws PatriusException
     */
    private Assembly createTestAssemblyVehicleParalWithPanels(final double lenght,
            final double width,
            final double heigh,
            final double mass,
            final double area,
            final double ka,
            final double kd,
            final double ks) throws PatriusException {
       
        // Creates the vehicle
        this.vehicle = new Vehicle();

        // Creation of a parallelepiped
        final Vector3D center = Vector3D.ZERO;
        final Vector3D uVector = Vector3D.PLUS_I;
        final Vector3D inputvVector = Vector3D.PLUS_J;
        final Parallelepiped parallelepiped = new Parallelepiped(center, uVector, inputvVector, lenght, width, heigh);

        this.vehicle.setMainShape(parallelepiped);
        this.vehicle.setDryMass(mass);

        // add solar panels
        this.vehicle.addSolarPanel(Vector3D.PLUS_I.add(Vector3D.PLUS_K), area);
        this.vehicle.addSolarPanel(Vector3D.MINUS_I.add(Vector3D.PLUS_K), area);

        // Radiative properties
        this.vehicle.setRadiativeProperties(ka, ks, kd, ka, ks, kd);

        // Build the Assembly
        return this.vehicle.createAssembly(FramesFactory.getGCRF());
    }

    /**
     * General set up method.
     * 
     * @throws PatriusException if an Orekit error occurs
     */
    @BeforeClass
    public static void setUpBeforeClass() throws PatriusException {
        Report.printClassHeader(RediffusedRadiationModelTest.class.getSimpleName(), "Rediffused radiation model");
        // Orekit data initialization
        fr.cnes.sirius.patrius.Utils.setDataRoot("regular-dataPBASE");
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#REDIFFUSED_RADIATIVE_MODEL}
     * 
     * @testedMethod {@link RediffusedRadiativeModel #rediffusedRadiationPressureAcceleration(SpacecraftState, ElementaryFlux)}
     * 
     * @description Creation of two assembly ; apply a mass change in one of the two ; test the
     *              rediffused radiation pressure acceleration change due to the mass decrease.
     * 
     * @input Assembly with radiative properties.
     * 
     * @output the rediffused radiation pressure acceleration.
     * 
     * @testPassCriteria the computed accelerations are changing depending on the maneuver.
     * 
     * @referenceVersion 2.3
     * @nonRegressionVersion 2.3
     */
    @Test
    public void maneuverMassVariationTest() throws PatriusException {

        // assemblies creation
        final Assembly assembly = getAssembly(this.mainBody, this.part2, this.part3);
        final Assembly assemblySM = getAssembly("mainBodySM", "part2SM", "part3SM");

        // rediffused radiative model
        final double k0Al = 0.568;
        final double k0Ir = 1.857;
        final RediffusedRadiativeModel radiativeModel = new RediffusedRadiativeModel(true, true, k0Al, k0Ir, assembly);
        final RediffusedRadiativeModel radiativeModelSM = new RediffusedRadiativeModel(true, true, k0Al, k0Ir, assemblySM);

        // set up spacecraftState
        final Orbit orbit = new CircularOrbit(7200000.0, -1.0e-5, 2.0e-4, MathLib.toRadians(98.0),
                MathLib.toRadians(123.456), 0.0, PositionAngle.MEAN, FramesFactory.getEME2000(), new AbsoluteDate(
                        new DateComponents(2004, 01, 01), new TimeComponents(23, 30, 00.000),
                        TimeScalesFactory.getUTC()), Constants.EIGEN5C_EARTH_MU);
        final AttitudeProvider law = new LofOffset(orbit.getFrame(), LOFType.LVLH, RotationOrder.XZY,
                FastMath.PI / 2.0, -FastMath.PI / 2.0, 0.0);
        final SpacecraftState state = new SpacecraftState(orbit, law.getAttitude(orbit, orbit.getDate(),
                orbit.getFrame()));
        assembly.initMainPartFrame(state);
        assemblySM.initMainPartFrame(state);

        // apply a mass change on the first assembly
        final MassModel model = new MassModel(assembly);
        final double newMass = 800.;
        model.updateMass(this.part3, newMass);

        // compute radiation pressure acceleration
        final ElementaryFlux[] flux = new ElementaryFlux[] { new ElementaryFlux(Vector3D.PLUS_I, 1, 2) };
        final Vector3D computedAcc = radiativeModel.rediffusedRadiationPressureAcceleration(state, flux);
        final Vector3D computedAccSM = radiativeModelSM.rediffusedRadiationPressureAcceleration(state, flux);

        // Comparison
        Assert.assertFalse(computedAccSM.getX() == computedAcc.getX());
        Assert.assertFalse(computedAccSM.getY() == computedAcc.getY());
        Assert.assertFalse(computedAccSM.getZ() == computedAcc.getZ());
    }

    private static Assembly getAssembly(final String nameMain,
            final String namePart2,
            final String namePart3) throws PatriusException {
        final AssemblyBuilder builder = new AssemblyBuilder();

        // add main part (one sphere) and part2 (one facet)
        builder.addMainPart(nameMain);
        builder.addPart(namePart2, nameMain, Transform.IDENTITY);
        builder.addPart(namePart3, nameMain, Transform.IDENTITY);

        // one facet
        final Vector3D normal = new Vector3D(-1.0, 1.0, -2.0);
        final Facet facet = new Facet(normal, 25 * FastMath.PI);

        final IPartProperty radSphereProp = new RadiativeSphereProperty(5.);
        builder.addProperty(radSphereProp, nameMain);
        final IPartProperty radFacetProp = new RadiativeFacetProperty(facet);
        builder.addProperty(radFacetProp, namePart2);

        // adding radiative properties
        final IPartProperty radMainProp = new RadiativeProperty(1, 0, 0);
        builder.addProperty(radMainProp, nameMain);
        final IPartProperty radPartProp = new RadiativeProperty(1, 0, 0);
        builder.addProperty(radPartProp, namePart2);
        final IPartProperty radIRMainProp = new RadiativeIRProperty(1, 1, 1);
        builder.addProperty(radIRMainProp, nameMain);
        builder.addProperty(radIRMainProp, namePart2);

        // adding mass property
        final IPartProperty massMainProp = new MassProperty(1000.);
        builder.addProperty(massMainProp, nameMain);
        // mass of part 2 transfered to part 3. part 2 left without mass
        final IPartProperty part3Mass = new MassProperty(1000.);
        builder.addProperty(part3Mass, namePart3);

        return builder.returnAssembly();
    }
}
