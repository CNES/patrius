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
 * VERSION:4.13:DM:DM-37:08/12/2023:[PATRIUS] Date d'evenement et propagation du signal
 * VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
 * VERSION:4.13:DM:DM-3:08/12/2023:[PATRIUS] Distinction entre corps celestes et barycentres
 * VERSION:4.13:DM:DM-132:08/12/2023:[PATRIUS] Suppression de la possibilite
 * de convertir les sorties de VacuumSignalPropagation
 * VERSION:4.13:DM:DM-5:08/12/2023:[PATRIUS] Orientation d'un corps celeste sous forme de quaternions
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.11.1:FA:FA-69:30/06/2023:[PATRIUS] Amélioration de la gestion des attractions gravitationnelles dans le propagateur
 * VERSION:4.11:DM:DM-3303:22/05/2023:[PATRIUS] Modifications mineures dans UserCelestialBody 
 * VERSION:4.11:DM:DM-3311:22/05/2023:[PATRIUS] Evolutions mineures sur CelestialBody, shape et reperes
 * VERSION:4.11:DM:DM-3300:22/05/2023:[PATRIUS] Nouvelle approche pour le calcul de la position relative de 2 corps celestes 
 * VERSION:4.11:DM:DM-3256:22/05/2023:[PATRIUS] Suite 3246
 * VERSION:4.11:FA:FA-3314:22/05/2023:[PATRIUS] Anomalie lors de l'evaluation d'un ForceModel lorsque le SpacecraftState est en ITRF
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration de la gestion des attractions gravitationnelles dans le propagateur
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
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
 * VERSION:4.9:FA:FA-3170:10/05/2022:[PATRIUS] Incoherence de datation entre la methode getLocalRadius...
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
import fr.cnes.sirius.patrius.bodies.MeeusSun.MODEL;
import fr.cnes.sirius.patrius.events.detectors.AbstractSignalPropagationDetector.PropagationDelayType;
import fr.cnes.sirius.patrius.forces.gravity.AbstractGravityModel;
import fr.cnes.sirius.patrius.forces.gravity.AbstractHarmonicGravityModel;
import fr.cnes.sirius.patrius.forces.gravity.DirectBodyAttraction;
import fr.cnes.sirius.patrius.forces.gravity.GravityModel;
import fr.cnes.sirius.patrius.forces.gravity.NewtonianGravityModel;
import fr.cnes.sirius.patrius.forces.gravity.ThirdBodyAttraction;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.TestUtils;
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
        Utils.setDataRoot("regular-dataCNES-2003");
        Report.printClassHeader(UserCelestialBodyTest.class.getSimpleName(), "User-defined celestial body");
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#USER_CELESTIAL_BODY}
     * 
     * @description check that user-defined Neptune returns the exact same results as JPL-built
     *              Neptune planet
     * 
     * @input Neptune input data (pole, gm)
     * 
     * @output Neptune output data (frame, position at a random date)
     * 
     * @testPassCriteria output data between JPL-built Neptune and user-defined Neptune are exactly
     *                   the same.
     * 
     * @referenceVersion 3.4
     * 
     * @nonRegressionVersion 3.4
     */
    @Test
    public void testUserDefinedCelestialBody() throws PatriusException {
        Utils.setDataRoot("regular-dataCNES-2003");

        Report.printMethodHeader("testUserDefinedCelestialBody",
            "Celestial body data (Neptune case)", "JPL body", 1E-15, ComparisonType.ABSOLUTE);

        // Build bodies
        final CelestialBody neptuneExpected = CelestialBodyFactory.getNeptune();
        final PVCoordinatesProvider pvProvider = new PVCoordinatesProvider(){
            /** Serializable UID. */
            private static final long serialVersionUID = 3983409421506898178L;

            @Override
            public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame)
                throws PatriusException {
                return neptuneExpected.getPVCoordinates(date, frame);
            }

            /** {@inheritDoc} */
            @Override
            public Frame getNativeFrame(final AbsoluteDate date)
                throws PatriusException {
                return neptuneExpected.getNativeFrame(null);
            }
        };
        // geometric shape parameters
        final double ae = 24764000.;
        final double ap = 24341000.;
        final BodyShape neptuneShape = new OneAxisEllipsoid(ae, (ae - ap) / ae,
            neptuneExpected.getRotatingFrame(IAUPoleModelType.TRUE), "Neptune");
        // UserCelestialBody with GM
        final UserCelestialBody neptuneActual = new UserCelestialBody("Neptune", pvProvider,
            neptuneExpected.getGM(), IAUPoleFactory.getIAUPole(EphemerisType.NEPTUNE),
            FramesFactory.getICRF(), neptuneShape);
        // UserCelestialBody with attraction model
        final UserCelestialBody neptuneActualAttractionModel = new UserCelestialBody("Neptune", pvProvider,
            neptuneExpected.getGravityModel(), IAUPoleFactory.getIAUPole(EphemerisType.NEPTUNE),
            FramesFactory.getICRF(), neptuneShape);

        // Check (GM, PV and frames)
        Assert.assertEquals(neptuneExpected.getGM(), neptuneActual.getGM(), 0.);
        Report.printToReport("GM", neptuneExpected.getGM(), neptuneActual.getGM());
        Assert.assertEquals(neptuneExpected.getGM(), neptuneActualAttractionModel.getGM(), 0.);

        final AbsoluteDate date = new AbsoluteDate(2003, 02, 01, TimeScalesFactory.getTT());
        final PVCoordinates pvExpected = neptuneExpected.getPVCoordinates(date,
            FramesFactory.getCIRF());
        final PVCoordinates pvActual = neptuneActual
            .getPVCoordinates(date, FramesFactory.getCIRF());
        final PVCoordinates pvActualAttractionModel = neptuneActualAttractionModel
            .getPVCoordinates(date, FramesFactory.getCIRF());
        Assert.assertEquals(0.,
            Vector3D.distance(pvExpected.getPosition(), pvActual.getPosition()), 0.);
        Assert.assertEquals(0.,
            Vector3D.distance(pvExpected.getVelocity(), pvActual.getVelocity()), 0.);
        Assert.assertEquals(0.,
            Vector3D.distance(pvExpected.getPosition(), pvActualAttractionModel.getPosition()), 0.);
        Assert.assertEquals(0.,
            Vector3D.distance(pvExpected.getVelocity(), pvActualAttractionModel.getVelocity()), 0.);
        Report.printToReport("Position", pvExpected.getPosition(), pvActual.getPosition());
        Report.printToReport("Velocity", pvExpected.getVelocity(), pvActual.getVelocity());

        final Transform t1 = neptuneExpected.getRotatingFrame(IAUPoleModelType.TRUE).getTransformTo(
            neptuneActual.getRotatingFrame(IAUPoleModelType.TRUE), date);
        final Transform t2 = neptuneExpected.getInertialFrame(IAUPoleModelType.CONSTANT).getTransformTo(
            neptuneActual.getInertialFrame(IAUPoleModelType.CONSTANT), date);
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
        Report.printToReport("Rotation rate difference (inertial)", Vector3D.ZERO,
            t2.getRotationRate());

        // Check geometric body shape
        final BodyShape expectedShape = neptuneExpected.getShape();

        Assert.assertEquals(neptuneActual.getShape(), neptuneShape);
        Assert.assertEquals(neptuneActualAttractionModel.getShape(), neptuneShape);
        // check the body name equivalence
        Assert.assertEquals(neptuneShape.getName(), expectedShape.getName());

        double actual;
        double expected;

        // getIntersection method
        final Line line1 = new Line(new Vector3D(4 * ae, 0, 0), new Vector3D(0, 3 * ae, 0));
        actual = neptuneShape.distanceTo(line1, neptuneExpected.getInertialFrame(IAUPoleModelType.CONSTANT),
            date);
        expected = expectedShape.distanceTo(line1, neptuneExpected.getInertialFrame(IAUPoleModelType.CONSTANT),
            date);
        Assert.assertEquals(expected, actual, 0.);

        // getApparentRadius method
        final Vector3D posInFrame = new Vector3D(2.5 * ae, 0, 1.5 * ae);
        final PVCoordinates pv = new PVCoordinates(posInFrame, Vector3D.ZERO);
        final PVCoordinatesProvider provider = new ConstantPVCoordinatesProvider(pv,
            neptuneExpected.getInertialFrame(IAUPoleModelType.CONSTANT));
        actual = neptuneShape.getApparentRadius(new ConstantPVCoordinatesProvider(posInFrame.scalarMultiply(5),
            neptuneExpected.getInertialFrame(IAUPoleModelType.CONSTANT)),
            date, provider, PropagationDelayType.INSTANTANEOUS);
        expected = expectedShape.getApparentRadius(new ConstantPVCoordinatesProvider(posInFrame.scalarMultiply(5),
            neptuneExpected.getInertialFrame(IAUPoleModelType.CONSTANT)),
            date, provider, PropagationDelayType.INSTANTANEOUS);
        Assert.assertEquals(expected, actual, 0.);

        // getIntersections method
        final Line line2 = new Line(new Vector3D(0.9 * ae, 0., 0.), Vector3D.ZERO);
        final BodyPoint[] intersections = neptuneShape.getIntersectionPoints(line2,
            neptuneExpected.getInertialFrame(IAUPoleModelType.CONSTANT), date);
        final BodyPoint[] expectedInter = expectedShape.getIntersectionPoints(line2,
            neptuneExpected.getInertialFrame(IAUPoleModelType.CONSTANT), date);
        Assert.assertEquals(intersections.length, expectedInter.length);
        for (int i = 0; i < expectedInter.length; i++) {
            Assert.assertEquals(intersections[0].getLLHCoordinates().getLatitude(),
                expectedInter[0].getLLHCoordinates().getLatitude(), 1e-12);
            Assert.assertEquals(intersections[0].getLLHCoordinates().getLongitude(),
                expectedInter[0].getLLHCoordinates().getLongitude(), 1e-12);
            Assert.assertEquals(intersections[0].getLLHCoordinates().getHeight(),
                expectedInter[0].getLLHCoordinates().getHeight(), 1e-12);
        }

        // set a new body shape
        final BodyShape newShape = new OneAxisEllipsoid(1.05 * ae, 0.001,
            neptuneExpected.getRotatingFrame(IAUPoleModelType.TRUE), "new Neptune");
        neptuneActual.setShape(newShape);
        neptuneActualAttractionModel.setShape(newShape);
        Assert.assertEquals(neptuneActual.getShape(), newShape);
        Assert.assertEquals(neptuneActualAttractionModel.getShape(), newShape);

        // test AttractionModel methods
        final double mu = neptuneExpected.getGM();
        final GravityModel model = neptuneActual.getGravityModel();
        final GravityModel modelAttractionModel = neptuneActualAttractionModel.getGravityModel();
        Assert.assertEquals(mu, neptuneActual.getGM(), 0.);
        Assert.assertEquals(mu, neptuneActualAttractionModel.getGM(), 0.);
        Assert.assertTrue(model.getClass().equals(NewtonianGravityModel.class));
        Assert.assertTrue(modelAttractionModel.getClass().equals(NewtonianGravityModel.class));

        // set a new AttractionModel
        final AbstractGravityModel newModel = new NewtonianGravityModel(2 * mu);
        neptuneActual.setGravityModel(newModel);
        neptuneActualAttractionModel.setGravityModel(newModel);
        Assert.assertEquals(2 * mu, neptuneActual.getGM(), 0.);
        Assert.assertEquals(2 * mu, neptuneActualAttractionModel.getGM(), 0.);
        Assert.assertTrue(neptuneActual.getGravityModel().getClass()
            .equals(NewtonianGravityModel.class));
        Assert.assertTrue(neptuneActualAttractionModel.getGravityModel().getClass()
            .equals(NewtonianGravityModel.class));

        // set a new central attraction coefficient, the attraction model must me updated
        neptuneActual.setGM(1.5 * mu);
        neptuneActualAttractionModel.setGM(1.5 * mu);
        Assert.assertEquals(1.5 * mu, neptuneActual.getGravityModel().getMu(), 0.);
        Assert.assertEquals(1.5 * mu, neptuneActualAttractionModel.getGravityModel().getMu(), 0.);
        Assert.assertTrue(neptuneActual.getGravityModel().getClass()
            .equals(NewtonianGravityModel.class));
        Assert.assertTrue(neptuneActualAttractionModel.getGravityModel().getClass()
            .equals(NewtonianGravityModel.class));

        // Test GetNativeFrame
        Assert.assertEquals(pvProvider.getNativeFrame(null).getName(),
            neptuneActual.getNativeFrame(null).getName());
        Assert.assertEquals(pvProvider.getNativeFrame(null).getName(),
            neptuneActualAttractionModel.getNativeFrame(null).getName());
    }

    /**
     * @throws PatriusException
     * @throws IOException
     * @testType VT
     * 
     * @testedFeature {@link features#USER_CELESTIAL_BODY}
     * 
     * @description performs a propagation (including Sun perturbation) around a user-defined
     *              celestial body (Mars)
     * 
     * @input Mars input data (pole, gm)
     * 
     * @output ephemeris of object orbiting Mars
     * 
     * @testPassCriteria ephemeris is as expected (non-regression). Ephemeris has been check before
     *                   to be consistent
     * 
     * @referenceVersion 4.11.1
     * 
     * @nonRegressionVersion 4.11.1
     */
    @Test
    public void testPropagation() throws PatriusException, IOException {
        Utils.setDataRoot("regular-dataCNES-2003");

        // Build bodies
        final PVCoordinatesProvider pvProvider = new PVCoordinatesProvider(){
            /** Serializable UID. */
            private static final long serialVersionUID = 4082033533887647052L;

            @Override
            public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame)
                throws PatriusException {
                return CelestialBodyFactory.getMars().getPVCoordinates(date, frame);
            }

            /** {@inheritDoc} */
            @Override
            public Frame getNativeFrame(final AbsoluteDate date)
                throws PatriusException {
                throw new PatriusException(PatriusMessages.INTERNAL_ERROR);
            }
        };
        // UserCelestialBody with GM
        final CelestialBody mars = new UserCelestialBody("Mars", pvProvider, CelestialBodyFactory
            .getMars().getGM(), IAUPoleFactory.getIAUPole(EphemerisType.MARS),
            FramesFactory.getICRF(), CelestialBodyFactory.getMars().getShape());

        // Build initial state
        final AbsoluteDate initialDate = AbsoluteDate.J2000_EPOCH;
        final Orbit orbit = new KeplerianOrbit(5000000, 0, 0, 0, 0, 0, PositionAngle.TRUE,
            mars.getInertialFrame(IAUPoleModelType.CONSTANT), initialDate, mars.getGM());
        final SpacecraftState initialState = new SpacecraftState(orbit);

        // Build propagator
        final NumericalPropagator propagator = new NumericalPropagator(
            new ClassicalRungeKuttaIntegrator(30), initialState.getFrame());
        final AbstractHarmonicGravityModel gravityModel = (AbstractHarmonicGravityModel) CelestialBodyFactory.getSun()
            .getGravityModel();
        gravityModel.setCentralTermContribution(false);
        propagator.addForceModel(new ThirdBodyAttraction(gravityModel));
        propagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(initialState.getFrame(), mars
            .getGM())));
        propagator.setInitialState(initialState);
        propagator.setEphemerisMode();

        // Propagation
        final double duration = 86400;
        final AbsoluteDate finalDate = initialDate.shiftedBy(duration);
        propagator.propagate(finalDate);
        BoundedPropagator ephemeris = propagator.getGeneratedEphemeris();

        // Retrieve and check ephemeris (non-regression)
        final URL url = UserCelestialBodyTest.class.getClassLoader().getResource(
            "userCelestialBody/UserCelestialBodyMarsRes.txt");
        BufferedReader reader = new BufferedReader(new FileReader(url.getPath()));
        final double step = 100;
        for (int i = 0; i < duration / step; i++) {
            final AbsoluteDate date = initialDate.shiftedBy(step * i);
            final SpacecraftState state = ephemeris.propagate(date);
            final PVCoordinates actual = state.getPVCoordinates(mars
                .getInertialFrame(IAUPoleModelType.CONSTANT));
            final String[] arrayString = reader.readLine().split("[ ]+");
            final PVCoordinates expected = new PVCoordinates(new Vector3D(
                Double.parseDouble(arrayString[0]), Double.parseDouble(arrayString[1]),
                Double.parseDouble(arrayString[2])), new Vector3D(
                Double.parseDouble(arrayString[3]), Double.parseDouble(arrayString[4]),
                Double.parseDouble(arrayString[5])));
            Assert.assertEquals(0., expected.getPosition().subtract(actual.getPosition()).getNorm(), 0.);
            Assert.assertEquals(0., expected.getVelocity().subtract(actual.getVelocity()).getNorm(), 0.);
        }
        reader.close();

        // UserCelestialBody with attractionModel
        final CelestialBody marsAttractionModel = new UserCelestialBody("Mars", pvProvider, CelestialBodyFactory
            .getMars().getGravityModel(), IAUPoleFactory.getIAUPole(EphemerisType.MARS),
            FramesFactory.getICRF(), CelestialBodyFactory.getMars().getShape());

        // Set initial state for the propagator
        propagator.setInitialState(initialState);

        // Propagation
        propagator.propagate(finalDate);
        ephemeris = propagator.getGeneratedEphemeris();

        // Retrieve and check ephemeris (non-regression)
        reader = new BufferedReader(new FileReader(url.getPath()));
        for (int i = 0; i < duration / step; i++) {
            final AbsoluteDate date = initialDate.shiftedBy(step * i);
            final SpacecraftState state = ephemeris.propagate(date);
            final PVCoordinates actual = state.getPVCoordinates(marsAttractionModel
                .getInertialFrame(IAUPoleModelType.CONSTANT));
            final String[] arrayString = reader.readLine().split("[ ]+");
            final PVCoordinates expected = new PVCoordinates(new Vector3D(
                Double.parseDouble(arrayString[0]), Double.parseDouble(arrayString[1]),
                Double.parseDouble(arrayString[2])), new Vector3D(
                Double.parseDouble(arrayString[3]), Double.parseDouble(arrayString[4]),
                Double.parseDouble(arrayString[5])));
            Assert.assertEquals(0.,
                expected.getPosition().subtract(actual.getPosition()).getNorm(), 0.);
            Assert.assertEquals(0.,
                expected.getVelocity().subtract(actual.getVelocity()).getNorm(), 0.);
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
    public void testToString() throws PatriusException {
        Utils.setDataRoot("regular-dataPBASE");
        // JPL (Moon)
        final CelestialPoint body1 = CelestialBodyFactory.getMoon();
        Assert.assertNotNull(body1.toString());
        // Meeus Moon
        final CelestialPoint body2 = new MeeusMoon();
        Assert.assertNotNull(body2.toString());
        // Meeus Sun
        final CelestialPoint body3 = new MeeusSun(MODEL.BOARD);
        Assert.assertNotNull(body3.toString());

        // UserCelestialBody (Moon) with GM
        final CelestialPoint body4 = new UserCelestialBody("My body", new MeeusMoon(), 1.23456789,
            IAUPoleFactory.getIAUPole(EphemerisType.MOON), FramesFactory.getICRF(),
            new MeeusMoon().getShape());

        // UserCelestialBody (Moon) with attraction model
        final CelestialPoint body5 = new UserCelestialBody("My body", new MeeusMoon(), new NewtonianGravityModel(
            1.23456789), IAUPoleFactory.getIAUPole(EphemerisType.MOON), FramesFactory.getICRF(),
            new MeeusMoon().getShape());

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
        builder.append("- EME2000 frame: My body EME2000 frame");
        builder.append(end);
        builder.append("- Ecliptic J2000 frame: My body EclipticJ2000 frame");
        builder.append(end);
        builder.append("- Inertial frame: My body Inertial frame (constant model)");
        builder.append(end);
        builder.append("- Mean equator frame: My body Inertial frame (mean model)");
        builder.append(end);
        builder.append("- True equator frame: My body Inertial frame (true model)");
        builder.append(end);
        builder.append("- Constant rotating frame: My body Rotating frame (constant model)");
        builder.append(end);
        builder.append("- Mean rotating frame: My body Rotating frame (mean model)");
        builder.append(end);
        builder.append("- True rotating frame: My body Rotating frame (true model)");
        builder.append(end);
        builder
            .append("- orientation: 2009 NT from IAU/IAG Working Group (IAUPoleFactory) (class fr.cnes.sirius.patrius.bodies.IAUPoleFactory$4)");
        builder.append(end);
        builder.append("- Ephemeris origin: - Name: Meeus Moon");
        builder.append(end);
        builder.append("- Corps type: MeeusMoon class");
        builder.append(end);
        builder.append("- GM: 4.902798458429647E12");
        builder.append(end);
        builder.append("- ICRF frame: Meeus Moon ICRF frame");
        builder.append(end);
        builder.append("- EME2000 frame: Meeus Moon EME2000 frame");
        builder.append(end);
        builder.append("- Ecliptic J2000 frame: Meeus Moon EclipticJ2000 frame");
        builder.append(end);
        builder.append("- Inertial frame: Meeus Moon Inertial frame (constant model)");
        builder.append(end);
        builder.append("- Mean equator frame: Meeus Moon Inertial frame (mean model)");
        builder.append(end);
        builder.append("- True equator frame: Meeus Moon Inertial frame (true model)");
        builder.append(end);
        builder.append("- Constant rotating frame: Meeus Moon Rotating frame (constant model)");
        builder.append(end);
        builder.append("- Mean rotating frame: Meeus Moon Rotating frame (mean model)");
        builder.append(end);
        builder.append("- True rotating frame: Meeus Moon Rotating frame (true model)");
        builder.append(end);
        builder
            .append("- orientation: 2009 NT from IAU/IAG Working Group (IAUPoleFactory) (class fr.cnes.sirius.patrius.bodies.IAUPoleFactory$4)");
        builder.append(end);
        builder.append("- Ephemeris origin: Meeus Moon model (class fr.cnes.sirius.patrius.bodies.MeeusMoon)");
        final String expected = builder.toString();
        Assert.assertEquals(expected, body4.toString());
        Assert.assertEquals(expected, body5.toString());

        // JPL (Moon)
        final CelestialPoint body6 = CelestialBodyFactory.getBodies().get("Moon");
        Assert.assertNotNull(body6.toString());
    }

    /**
     * @throws PatriusException
     *         if an error occurs
     * @description Evaluate the user-defined celestial body serialization / deserialization
     *              process.
     *
     * @testPassCriteria The user-defined celestial body can be serialized and deserialized.
     */
    @Test
    public void testSerialization() throws PatriusException {

        Utils.setDataRoot("regular-dataCNES-2003");

        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Frame frame = FramesFactory.getGCRF();
        final PVCoordinatesProvider marsPV = CelestialBodyFactory.getMars();

        final UserCelestialBody mars = new UserCelestialBody("Mars", marsPV, CelestialBodyFactory
            .getMars().getGM(), IAUPoleFactory.getIAUPole(EphemerisType.MARS),
            FramesFactory.getICRF(), CelestialBodyFactory.getMars().getShape());
        final UserCelestialBody deserializedMars = TestUtils.serializeAndRecover(mars);

        // Check the body through PVCoordinates computation on several dates
        for (int i = 0; i < 10; i++) {
            final AbsoluteDate currentDate = date.shiftedBy(i);
            Assert.assertEquals(mars.getPVCoordinates(currentDate, frame),
                deserializedMars.getPVCoordinates(currentDate, frame));
        }
    }
}
