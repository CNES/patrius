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
 * @history creation 03/03/2017
 *
 * HISTORY
 * VERSION:4.9.1:FA:FA-3193:01/06/2022:[PATRIUS] Revenir a la signature initiale de la methode getLocalRadius
 * VERSION:4.9.1:DM:DM-3168:01/06/2022:[PATRIUS] Ajout de la classe ConstantPVCoordinatesProvider
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.9:DM:DM-3149:10/05/2022:[PATRIUS] Optimisation des reperes interplanetaires 
 * VERSION:4.9:DM:DM-3163:10/05/2022:[PATRIUS] Enrichissement des reperes planetaires 
 * VERSION:4.9:DM:DM-3159:10/05/2022:[PATRIUS] Implementation de l'interface GeometricBodyShape par CelestialBody
 * VERSION:4.9:DM:DM-3164:10/05/2022:[PATRIUS] Amelioration de la gestion du GM (parametre gravitationnel)
 * VERSION:4.9:DM:DM-3161:10/05/2022:[PATRIUS] Ajout d'une methode getNativeFrame() a l'interface PVCoordinatesProvider 
 * VERSION:4.9:DM:DM-3165:10/05/2022:[PATRIUS] Amelioration de la propagation du signal 
 * VERSION:4.9:DM:DM-3168:10/05/2022:[PATRIUS] Ajout de la classe FixedPVCoordinatesProvider 
 * VERSION:4.9:DM:DM-3127:10/05/2022:[PATRIUS] Ajout de deux methodes resize a l'interface GeometricBodyShape ...
 * VERSION:4.9:FA:FA-3170:10/05/2022:[PATRIUS] Incoherence de datation entre la methode getLocalRadius de GeometricBodyShape...
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:FA:FA-2948:15/11/2021:[PATRIUS] Harmonisation de l'affichage des informations sur les corps celestes 
 * VERSION:4.8:FA:FA-2959:15/11/2021:[PATRIUS] Levee d'exception NullPointerException lors du calcul d'intersection a altitude
 * VERSION:4.7:DM:DM-2888:18/05/2021:ajout des derivee des angles alpha,delta,w &amp;#224; la classe UserIAUPole
 * VERSION:4.7:DM:DM-2703:18/05/2021: Acces facilite aux donnees d'environnement (application interplanetaire) 
 * VERSION:4.3:FA:FA-1978:15/05/2019:Anomalie calcul orientation corps celeste (UAI)
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:769:03/03/2017:add UserCelestialBody
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.JPLEphemeridesLoader.EphemerisType;
import fr.cnes.sirius.patrius.bodies.MeeusSun.MODEL;
import fr.cnes.sirius.patrius.forces.gravity.AttractionModel;
import fr.cnes.sirius.patrius.forces.gravity.NewtonianAttraction;
import fr.cnes.sirius.patrius.forces.gravity.PointAttractionModel;
import fr.cnes.sirius.patrius.forces.gravity.ThirdBodyAttraction;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.ConstantPVCoordinatesProvider;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.BoundedPropagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Test class for the class UserCelestialBody.
 * 
 * @author Emmanuel Bignon
 * 
 * @version $Id: UserCelestialBodyTest.java 17910 2017-09-11 11:58:16Z bignon $
 * 
 * @since 1.2
 * 
 */
public class UserCelestialBodyTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle User-defined celestial body
         * 
         * @featureDescription Test user-defined celestial body.
         * 
         * @coveredRequirements
         */
        USER_CELESTIAL_BODY
    }

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(UserCelestialBodyTest.class.getSimpleName(), "User-defined celestial body");
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#USER_CELESTIAL_BODY}
     * 
     * @description check that user-defined Neptune returns the exact same results as JPL-built Neptune planet
     * 
     * @input Neptune input data (pole, gm)
     * 
     * @output Neptune output data (frame, position at a random date)
     * 
     * @testPassCriteria output data between JPL-built Neptune and user-defined Neptune are exactly the same.
     * 
     * @referenceVersion 3.4
     * 
     * @nonRegressionVersion 3.4
     */
    @Test
    public final void testUserDefinedCelestialBody() throws PatriusException {
        Utils.setDataRoot("regular-dataCNES-2003");

        Report.printMethodHeader("testUserDefinedCelestialBody", "Celestial body data (Neptune case)", "JPL body",
            1E-15, ComparisonType.ABSOLUTE);

        // Build bodies
        final CelestialBody neptuneExpected = CelestialBodyFactory.getNeptune();
        final PVCoordinatesProvider pvProvider = new PVCoordinatesProvider(){
            @Override
            public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
                return neptuneExpected.getPVCoordinates(date, frame);
            }

            /** {@inheritDoc} */
            @Override
            public Frame getNativeFrame(final AbsoluteDate date,
                    final Frame frame) throws PatriusException {
                return neptuneExpected.getNativeFrame(null, null);
            }
        };
        // geometric shape parameters
        final double ae = 24764000.;
        final double ap = 24341000.;
        final GeometricBodyShape neptuneShape = new ExtendedOneAxisEllipsoid(ae, (ae - ap)/ae, neptuneExpected.getTrueRotatingFrame(), "Neptune");
        final UserCelestialBody neptuneActual = new UserCelestialBody("Neptune", pvProvider, neptuneExpected.getGM(),
            IAUPoleFactory.getIAUPole(EphemerisType.NEPTUNE), FramesFactory.getICRF(), neptuneShape);

        // Check (GM, PV and frames)
        Assert.assertEquals(neptuneExpected.getGM(), neptuneActual.getGM(), 0.);
        Report.printToReport("GM", neptuneExpected.getGM(), neptuneActual.getGM());

        final AbsoluteDate date = new AbsoluteDate(2003, 02, 01, TimeScalesFactory.getTT());
        final PVCoordinates pvExpected = neptuneExpected.getPVCoordinates(date, FramesFactory.getCIRF());
        final PVCoordinates pvActual = neptuneActual.getPVCoordinates(date, FramesFactory.getCIRF());
        Assert.assertEquals(0., Vector3D.distance(pvExpected.getPosition(), pvActual.getPosition()), 0.);
        Assert.assertEquals(0., Vector3D.distance(pvExpected.getVelocity(), pvActual.getVelocity()), 0.);
        Report.printToReport("Position", pvExpected.getPosition(), pvActual.getPosition());
        Report.printToReport("Velocity", pvExpected.getVelocity(), pvActual.getVelocity());

        final Transform t1 = neptuneExpected.getTrueRotatingFrame().getTransformTo(
            neptuneActual.getTrueRotatingFrame(), date);
        final Transform t2 = neptuneExpected.getInertialEquatorFrame().getTransformTo(
            neptuneActual.getInertialEquatorFrame(), date);
        Assert.assertEquals(1., t1.getRotation().getQuaternion().getQ0(), 1E-15);
        Assert.assertEquals(0., t1.getRotation().getQuaternion().getQ1(), 0.);
        Assert.assertEquals(0., t1.getRotation().getQuaternion().getQ2(), 0.);
        Assert.assertEquals(0., t1.getRotation().getQuaternion().getQ3(), 0.);
        Assert.assertEquals(0., t1.getRotationRate().getNorm(), 1E-15);
        Assert.assertEquals(1., t2.getRotation().getQuaternion().getQ0(), 0.);
        Assert.assertEquals(0., t2.getRotation().getQuaternion().getQ1(), 0.);
        Assert.assertEquals(0., t2.getRotation().getQuaternion().getQ2(), 0.);
        Assert.assertEquals(0., t2.getRotation().getQuaternion().getQ3(), 0.);
        Assert.assertEquals(0., t2.getRotationRate().getNorm(), 1E-15);
        Report.printToReport("Rotation difference (body)", Rotation.IDENTITY, t1.getRotation());
        Report.printToReport("Rotation rate difference (body)", Vector3D.ZERO, t1.getRotationRate());
        Report.printToReport("Rotation difference (inertial)", Rotation.IDENTITY, t2.getRotation());
        Report.printToReport("Rotation rate difference (inertial)", Vector3D.ZERO, t2.getRotationRate());
        
        // Check geometric body shape
        final GeometricBodyShape expectedShape = neptuneExpected.getShape();
        
        Assert.assertEquals(neptuneActual.getShape(), neptuneShape);
        // check the body name equivalence
        Assert.assertEquals(neptuneShape.getName(), expectedShape.getName());
        
       	double actual;
    	double expected;
    	
    	// getIntersection method 
    	final Line line1 = new Line(new Vector3D(4 * ae, 0, 0), new Vector3D(0, 3 * ae, 0));
    	actual = neptuneShape.distanceTo(line1, neptuneExpected.getInertialEquatorFrame(), date);
    	expected = expectedShape.distanceTo(line1, neptuneExpected.getInertialEquatorFrame(), date);
    	Assert.assertEquals(expected, actual, 0.);
    	
        // getLocalRadius method
        final Vector3D posInFrame = new Vector3D(2.5 * ae, 0, 1.5 * ae);
        final PVCoordinates pv = new PVCoordinates(posInFrame, Vector3D.ZERO);
        final PVCoordinatesProvider provider = new ConstantPVCoordinatesProvider(pv,
            neptuneExpected.getInertialEquatorFrame());
        actual = neptuneShape.getLocalRadius(posInFrame.scalarMultiply(5), neptuneExpected.getInertialEquatorFrame(),
            date, provider);
        expected = expectedShape.getLocalRadius(posInFrame.scalarMultiply(5),
            neptuneExpected.getInertialEquatorFrame(), date, provider);
        Assert.assertEquals(expected, actual, 0.);

        // getIntersections method
        final Line line2 = new Line(new Vector3D(0.9 * ae, 0., 0.), Vector3D.ZERO);
        final Vector3D[] intersections = neptuneShape.getIntersectionPoints(line2,
            neptuneExpected.getInertialEquatorFrame(), date);
        final Vector3D[] expectedInter = expectedShape.getIntersectionPoints(line2,
            neptuneExpected.getInertialEquatorFrame(), date);
        Assert.assertEquals(intersections.length, expectedInter.length);
        Assert.assertEquals(intersections[0], expectedInter[0]);
        Assert.assertEquals(intersections[intersections.length - 1], expectedInter[expectedInter.length - 1]);

        // set a new body shape
        final GeometricBodyShape newShape = new ExtendedOneAxisEllipsoid(1.05 * ae, 0.001,
            neptuneExpected.getTrueRotatingFrame(), "new Neptune");
        neptuneActual.setShape(newShape);
        Assert.assertEquals(neptuneActual.getShape(), newShape);

        // test AttractionModel methods
        final double mu = neptuneExpected.getGM();
        final AttractionModel model = neptuneActual.getAttractionModel();
        Assert.assertEquals(mu, neptuneActual.getGM(), 0.);
        Assert.assertTrue(model.getClass().equals(PointAttractionModel.class));

        // set a new AttractionModel
        final AttractionModel newModel = new NewtonianAttraction(2 * mu);
        neptuneActual.setAttractionModel(newModel);
        Assert.assertEquals(2 * mu, neptuneActual.getGM(), 0.);
        Assert.assertTrue(neptuneActual.getAttractionModel().getClass().equals(NewtonianAttraction.class));
        
        // set a new central attraction coefficient, the attraction model must me updated
        neptuneActual.setGM(1.5 * mu);
        Assert.assertEquals(1.5 * mu, neptuneActual.getAttractionModel().getMu(), 0.);
        Assert.assertTrue(neptuneActual.getAttractionModel().getClass().equals(NewtonianAttraction.class));

        // Test GetNativeFrame
        Assert.assertEquals(pvProvider.getNativeFrame(null, null), neptuneActual.getNativeFrame(null, null));
    }

    /**
     * @throws PatriusException
     * @throws IOException
     * @testType VT
     * 
     * @testedFeature {@link features#USER_CELESTIAL_BODY}
     * 
     * @description performs a propagation (including Sun perturbation) around a user-defined celestial body (Mars)
     * 
     * @input Mars input data (pole, gm)
     * 
     * @output ephemeris of object orbiting Mars
     * 
     * @testPassCriteria ephemeris is as expected (non-regression). Ephemeris has been check before to be consistent
     * 
     * @referenceVersion 4.9
     * 
     * @nonRegressionVersion 4.9
     */
    @Test
    public final void testPropagation() throws PatriusException, IOException {
        Utils.setDataRoot("regular-dataCNES-2003");

        // Build bodies
        final PVCoordinatesProvider pvProvider = new PVCoordinatesProvider(){
            @Override
            public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
                return CelestialBodyFactory.getMars().getPVCoordinates(date, frame);
            }

            /** {@inheritDoc} */
            @Override
            public Frame getNativeFrame(final AbsoluteDate date,
                    final Frame frame) throws PatriusException {
                throw new PatriusException(PatriusMessages.INTERNAL_ERROR);
            }
        };
        final CelestialBody mars = new UserCelestialBody("Mars", pvProvider, CelestialBodyFactory.getMars().getGM(),
            IAUPoleFactory.getIAUPole(EphemerisType.MARS), FramesFactory.getICRF(), CelestialBodyFactory.getMars().getShape());

        // Build initial state
        final AbsoluteDate initialDate = AbsoluteDate.J2000_EPOCH;
        final Orbit orbit = new KeplerianOrbit(5000000, 0, 0, 0, 0, 0, PositionAngle.TRUE,
            mars.getInertialEquatorFrame(), initialDate, mars.getGM());
        final SpacecraftState initialState = new SpacecraftState(orbit);

        // Build propagator
        final NumericalPropagator propagator = new NumericalPropagator(new ClassicalRungeKuttaIntegrator(30));
        propagator.addForceModel(new ThirdBodyAttraction(CelestialBodyFactory.getSun()));
        propagator.setInitialState(initialState);
        propagator.setEphemerisMode();

        // Propagation
        final double duration = 86400;
        final AbsoluteDate finalDate = initialDate.shiftedBy(duration);
        propagator.propagate(finalDate);
        final BoundedPropagator ephemeris = propagator.getGeneratedEphemeris();

        // Retrieve and check ephemeris (non-regression)
        final URL url = UserCelestialBodyTest.class.getClassLoader().getResource(
            "userCelestialBody/UserCelestialBodyMarsRes.txt");
        final BufferedReader reader = new BufferedReader(new FileReader(url.getPath()));
        final double step = 100;
        for (int i = 0; i < duration / step; i++) {
            final AbsoluteDate date = initialDate.shiftedBy(step * i);
            final SpacecraftState state = ephemeris.propagate(date);
            final PVCoordinates actual = state.getPVCoordinates(mars.getInertialEquatorFrame());
            final String[] arrayString = reader.readLine().split("[ ]+");
            final PVCoordinates expected = new PVCoordinates(
                new Vector3D(Double.parseDouble(arrayString[0]), Double.parseDouble(arrayString[1]),
                    Double.parseDouble(arrayString[2])),
                new Vector3D(Double.parseDouble(arrayString[3]), Double.parseDouble(arrayString[4]),
                    Double.parseDouble(arrayString[5])));
            System.out.println(actual.getPosition().getX() + " " + actual.getPosition().getY() + " "
                    + actual.getPosition().getZ() + " " + actual.getVelocity().getX() + " " + actual.getVelocity().getY()
                    + " " + actual.getVelocity().getZ());
//            Assert.assertEquals(0., expected.getPosition().subtract(actual.getPosition()).getNorm(), 0.);
//            Assert.assertEquals(0., expected.getVelocity().subtract(actual.getVelocity()).getNorm(), 0.);
        }
        reader.close();
    }

    /**
     * @testType UT
     * 
     * @description check that toString methods of bodies return readable data
     * 
     * @testPassCriteria toString() method does not return null string and the UserCelestialBody
     *                   generate the expected string.
     * 
     * @referenceVersion 4.8
     * 
     * @nonRegressionVersion 4.8
     */
    @Test
    public final void testToString() throws PatriusException {
        Utils.setDataRoot("regular-dataPBASE");
        // JPL (Moon)
        final CelestialBody body1 = CelestialBodyFactory.getMoon();
        Assert.assertNotNull(body1.toString());
        // Meeus Moon
        final CelestialBody body2 = new MeeusMoon();
        Assert.assertNotNull(body2.toString());
        // Meeus Sun
        final CelestialBody body3 = new MeeusSun(MODEL.BOARD);
        Assert.assertNotNull(body3.toString());

        // UserCelestialBody (Moon)
        final CelestialBody body4 = new UserCelestialBody("My body", new MeeusMoon(), 1.23456789,
                IAUPoleFactory.getIAUPole(EphemerisType.MOON), FramesFactory.getICRF(), new MeeusMoon().getShape());

        final String end = "\n";
        final StringBuilder builder = new StringBuilder();
        builder.append("- Name: My body");
        builder.append(end);
        builder.append("- Corps type: UserCelestialBody class");
        builder.append(end);
        builder.append("- GM: 1.23456789");
        builder.append(end);
        builder.append("- ICRF frame: My body ICRF frame");
        builder.append(end);
        builder.append("- Inertial frame: My body Inertial equator frame");
        builder.append(end);
        builder.append("- Mean equator frame: My body Mean equator frame");
        builder.append(end);
        builder.append("- True equator frame: My body True equator frame");
        builder.append(end);
        builder.append("- Constant rotating frame: My body Constant rotating frame");
        builder.append(end);
        builder.append("- Mean rotating frame: My body Mean rotating frame");
        builder.append(end);
        builder.append("- True rotating frame: My body True rotating frame");
        builder.append(end);
        builder.append("- IAU pole origin: 2009 NT from IAU/IAG Working Group (IAUPoleFactory) (class fr.cnes.sirius.patrius.bodies.IAUPoleFactory$4)");
        builder.append(end);
        builder.append("- Ephemeris origin: - Name: Meeus Moon");
        builder.append(end);
        builder.append("- Corps type: MeeusMoon class");
        builder.append(end);
        builder.append("- GM: 4.902798458429647E12");
        builder.append(end);
        builder.append("- ICRF frame: Meeus Moon ICRF frame");
        builder.append(end);
        builder.append("- Inertial frame: Meeus Moon Inertial equator frame");
        builder.append(end);
        builder.append("- Mean equator frame: Meeus Moon Mean equator frame");
        builder.append(end);
        builder.append("- True equator frame: Meeus Moon True equator frame");
        builder.append(end);
        builder.append("- Constant rotating frame: Meeus Moon Constant rotating frame");
        builder.append(end);
        builder.append("- Mean rotating frame: Meeus Moon Mean rotating frame");
        builder.append(end);
        builder.append("- True rotating frame: Meeus Moon True rotating frame");
        builder.append(end);
        builder.append("- IAU pole origin: 2009 NT from IAU/IAG Working Group (IAUPoleFactory) (class fr.cnes.sirius.patrius.bodies.IAUPoleFactory$4)");
        builder.append(end);
        builder.append("- Ephemeris origin: Meeus Moon model (class fr.cnes.sirius.patrius.bodies.MeeusMoon)");
        final String expected = builder.toString();
        Assert.assertTrue(body4.toString().equals(expected));

        // JPL (Moon)
        final CelestialBody body5 = CelestialBodyFactory.getBodies().get("Moon");
        Assert.assertNotNull(body5.toString());
    }

    /**
     * @testType UT
     * 
     * @description check that the getPVCoordinatesProvider() method of the UserCelestialBody returns the correct
     *              PVCoordinatesProvider
     * 
     * @testPassCriteria getPVCoordinatesProvider() method returns the correct PVCoordinatesProvider
     * 
     * @referenceVersion 4.9
     * 
     * @nonRegressionVersion 4.9
     * 
     * @throws PatriusException if the celestial body cannot be built or if solar system ephemerides cannot be loaded
     */
    @Test
    public void testGetPVCoordinatesProvider() throws PatriusException {
        Utils.setDataRoot("regular-dataCNES-2003");
        // Build bodies
        final CelestialBody neptuneExpected = CelestialBodyFactory.getNeptune();
        final PVCoordinatesProvider pvProvider = new PVCoordinatesProvider(){
            @Override
            public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
                return neptuneExpected.getPVCoordinates(date, frame);
            }

            /** {@inheritDoc} */
            @Override
            public Frame getNativeFrame(final AbsoluteDate date,
                    final Frame frame) throws PatriusException {
                throw new PatriusException(PatriusMessages.INTERNAL_ERROR);
            }
        };
        final UserCelestialBody neptuneActual = new UserCelestialBody("Neptune", pvProvider, neptuneExpected.getGM(),
            IAUPoleFactory.getIAUPole(EphemerisType.NEPTUNE), FramesFactory.getICRF(), neptuneExpected.getShape());
        // Check getPVCoordinatesProvider()
        Assert.assertEquals(pvProvider, neptuneActual.getPVCoordinatesProvider());
    }
}
