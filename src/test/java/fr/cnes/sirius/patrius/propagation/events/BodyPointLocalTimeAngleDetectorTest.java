package fr.cnes.sirius.patrius.propagation.events;

/** HISTORY
 * VERSION:4.16:OPENFD-489:25/04/2025:[PATRIUS] Adaptation de l'evenement LocalTime pour une direction zenithale
 * VERSION:4.16:OPENFD-468:25/04/2025:[PATRIUS] Renommer toutes les mentions du GeodeticPoint
 * END-HISTORY
 */
import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.bodies.BodyPoint;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.IAUPoleCoefficients;
import fr.cnes.sirius.patrius.bodies.IAUPoleCoefficients1D;
import fr.cnes.sirius.patrius.bodies.IAUPoleFunction;
import fr.cnes.sirius.patrius.bodies.IAUPoleFunction.IAUTimeDependency;
import fr.cnes.sirius.patrius.bodies.IAUPoleFunctionType;
import fr.cnes.sirius.patrius.bodies.IAUPoleModelType;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.bodies.UserIAUCelestialBody;
import fr.cnes.sirius.patrius.bodies.UserIAUPole;
import fr.cnes.sirius.patrius.events.EventDetector.Action;
import fr.cnes.sirius.patrius.events.detectors.BodyPointLocalTimeAngleDetector;
import fr.cnes.sirius.patrius.events.detectors.LocalTimeAngleDetector;
import fr.cnes.sirius.patrius.events.postprocessing.EventsLogger;
import fr.cnes.sirius.patrius.events.postprocessing.EventsLogger.LoggedEvent;
import fr.cnes.sirius.patrius.frames.CelestialBodyFrame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.analysis.differentiation.UnivariateDifferentiableFunction;
import fr.cnes.sirius.patrius.math.analysis.polynomials.PolynomialFunction;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class BodyPointLocalTimeAngleDetectorTest extends LocalTimeAngleDetectorTest {

    /**
     * @throws PatriusException
     * @testType UT
     *
     * @testedFeature The correct construction of {@link BodyPointLocalTimeAngleDetector} and the
     *                basis functionality (getters, copy...)
     *
     * @testedMethod {@link BodyPointLocalTimeAngleDetector#BodyPointLocalTimeAngleDetector(double, BodyPoint, double, double, CelestialBodyFrame, Action, boolean, fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider, int) }
     * @testedMethod {@link BodyPointLocalTimeAngleDetector#getBodyPoint() }
     * @testedMethod {@link BodyPointLocalTimeAngleDetector#copy() }
     *
     * @description simple constructor test and getters
     *
     * @input constructor parameters:
     *
     * @output a {@link BodyPointLocalTimeAngleDetector}
     *
     * @testPassCriteria the {@link BodyPointLocalTimeAngleDetector} is successfully created and the
     *                   getters return the original values
     *
     * @referenceVersion 4.16
     *
     * @nonRegressionVersion 4.16
     */
    @Test
    public void testBodyPointLocalTimeAngleDetectorCtor() throws PatriusException {

        // Configure detector
        final CelestialBody sun = CelestialBodyFactory.getSun();
        final CelestialBodyFrame marsFrame = CelestialBodyFactory.getMars().getInertialFrame();

        // Mars moon orbit
        final Orbit moonOrbit =
            new KeplerianOrbit(9377.1E+03, 0.0151, FastMath.toRadians(1.075), 0.0, 0.0, 0.0,
                PositionAngle.TRUE, marsFrame, iniDate, Constants.JPL_SSD_MARS_SYSTEM_GM);
        final double period = moonOrbit.getKeplerianPeriod();

        // Mars moon orientation
        final IAUPoleCoefficients1D alpha0Coeffs = new IAUPoleCoefficients1D(null);
        final IAUPoleCoefficients1D delta0Coeffs = new IAUPoleCoefficients1D(null);
        final UnivariateDifferentiableFunction function =
            new PolynomialFunction(new double[] { 0., 2. * MathLib.PI * 86400. / period });
        final IAUPoleFunction moonRotation =
            new IAUPoleFunction(IAUPoleFunctionType.SECULAR, function, IAUTimeDependency.DAYS);
        final IAUPoleCoefficients1D wCoeffs =
            new IAUPoleCoefficients1D(Collections.singletonList(moonRotation));
        final IAUPoleCoefficients poleCoeffs =
            new IAUPoleCoefficients(alpha0Coeffs, delta0Coeffs, wCoeffs);

        // Mars moon body
        final UserIAUCelestialBody moonCelestialBody = new UserIAUCelestialBody("", moonOrbit, 0,
            new UserIAUPole(poleCoeffs), FramesFactory.getEME2000(), null);
        final CelestialBodyFrame moonInertialFrame =
            moonCelestialBody.getInertialFrame(IAUPoleModelType.CONSTANT);
        final CelestialBodyFrame moonRotatingFrame =
            moonCelestialBody.getRotatingFrame(IAUPoleModelType.MEAN);
        moonCelestialBody.setShape(new OneAxisEllipsoid(1.E4, 0., moonRotatingFrame));

        final BodyPoint bodyPoint1 = moonCelestialBody.getShape()
            .buildPoint(new Vector3D(-1000., 1000., 1000.), "bodyPoint1");

        // creates the detector
        final BodyPointLocalTimeAngleDetector detector =
            new BodyPointLocalTimeAngleDetector(FastMath.PI * 0.5, bodyPoint1, 600, 1.e-6,
                moonInertialFrame, Action.CONTINUE, false, sun, 2);

        // CHECKS:

        // The constructor did not crash... so the object was constructed correctly
        Assert.assertNotNull(detector);

        // Verify that the body point is successfuly returned
        Assert.assertTrue(bodyPoint1.equals(detector.getBodyPoint()));

        // Verify that the copy method works
        final BodyPointLocalTimeAngleDetector detectorCopy = detector.copy();
        Assert.assertEquals(detector.getTime(), detectorCopy.getTime(), Double.MIN_VALUE);
        Assert.assertTrue(detector.getBodyPoint().equals(detectorCopy.getBodyPoint()));
        Assert.assertEquals(detector.getMaxCheckInterval(), detectorCopy.getMaxCheckInterval(),
            Double.MIN_VALUE);
        Assert.assertEquals(detector.getThreshold(), detectorCopy.getThreshold(), Double.MIN_VALUE);
        Assert.assertTrue(detector.getFrame().equals(detectorCopy.getFrame()));
        Assert.assertTrue(detector.getAction().equals(detectorCopy.getAction()));
        Assert.assertTrue(detector.getSun().equals(detectorCopy.getSun()));
        Assert.assertEquals(detector.getSlopeSelection(), detectorCopy.getSlopeSelection());
    }

    /**
     * @testType UT
     *
     * @testedFeature {@link features#VALIDATE_LOCAL_TIME_ANGLE_DETECTOR}
     *
     * @testedMethod {@link BodyPointLocalTimeAngleDetector#g(SpacecraftState)}
     * @testedMethod {@link BodyPointLocalTimeAngleDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     *
     * @description propagates a spacecraft in mars orbit adding one BodyPointLocalTimeAngleDetector
     *              and using an events logger:
     *              an event is detected when the angle between the satellite point and mars moon
     *              projections on the
     *              equatorial plane is equal to one of the predetermined values.
     *
     * @input constructor parameters, a propagator and an event logger
     *
     * @output the local time events logged during the propagation
     *
     * @testPassCriteria check that when an event is logged during the propagation, the angle
     *                   between
     *                   the two vector projections is equal to the predetermined value.
     *
     * @referenceVersion 4.16
     *
     * @nonRegressionVersion 4.16
     *
     * @throws PatriusException
     *         should not happen here
     */
    @Test
    public void testLocalTimeAngleDetectorZenith() throws PatriusException {

        final CelestialBody sun = CelestialBodyFactory.getSun();
        final CelestialBodyFrame marsFrame = CelestialBodyFactory.getMars().getInertialFrame();

        // Mars moon orbit
        final Orbit moonOrbit =
            new KeplerianOrbit(9377.1E+03, 0.0151, FastMath.toRadians(1.075), 0.0, 0.0, 0.0,
                PositionAngle.TRUE, marsFrame, iniDate, Constants.JPL_SSD_MARS_SYSTEM_GM);
        final double period = moonOrbit.getKeplerianPeriod();

        // Mars moon orientation
        final IAUPoleCoefficients1D alpha0Coeffs = new IAUPoleCoefficients1D(null);
        final IAUPoleCoefficients1D delta0Coeffs = new IAUPoleCoefficients1D(null);
        final UnivariateDifferentiableFunction function =
            new PolynomialFunction(new double[] { 0., 2. * MathLib.PI * 86400. / period });
        final IAUPoleFunction moonRotation =
            new IAUPoleFunction(IAUPoleFunctionType.SECULAR, function, IAUTimeDependency.DAYS);
        final IAUPoleCoefficients1D wCoeffs =
            new IAUPoleCoefficients1D(Collections.singletonList(moonRotation));
        final IAUPoleCoefficients poleCoeffs =
            new IAUPoleCoefficients(alpha0Coeffs, delta0Coeffs, wCoeffs);

        // Mars moon body
        final UserIAUCelestialBody moonCelestialBody = new UserIAUCelestialBody("", moonOrbit, 0,
            new UserIAUPole(poleCoeffs), FramesFactory.getEME2000(), null);
        final CelestialBodyFrame moonInertialFrame =
            moonCelestialBody.getInertialFrame(IAUPoleModelType.CONSTANT);
        final CelestialBodyFrame moonRotatingFrame =
            moonCelestialBody.getRotatingFrame(IAUPoleModelType.MEAN);
        moonCelestialBody.setShape(new OneAxisEllipsoid(1.E4, 0., moonRotatingFrame));

        // Spacecraft orbit (non used for the event detection, but necessary for the propagator)
        final Orbit spacecraftOrbit =
            new KeplerianOrbit(6000.0E+03, 0.009, FastMath.toRadians(2), 0.0, 0.0, 0.0,
                PositionAngle.TRUE, marsFrame, iniDate, Constants.JPL_SSD_MARS_SYSTEM_GM);
        final Propagator propagator = new KeplerianPropagator(spacecraftOrbit);

        // step handler to track local time evolution during propagation
        final MyStepHandler angleTracking = new MyStepHandler(iniDate, moonInertialFrame);
        propagator.setMasterMode(10, angleTracking);

        final BodyPoint bodyPoint1 = moonCelestialBody.getShape()
            .buildPoint(new Vector3D(-1000., 1000., 1000.), "bodyPoint1");

        // creates the detector
        final BodyPointLocalTimeAngleDetector detector0 =
            new BodyPointLocalTimeAngleDetector(FastMath.PI * 0.5, bodyPoint1, 600, 1.e-6,
                moonInertialFrame, Action.CONTINUE, false, sun, 2);

        // creates the logger
        final EventsLogger logger = new EventsLogger();
        // adds the logger to the propagator
        propagator.addEventDetector(logger.monitorDetector(detector0));
        // propagate
        propagator.propagate(iniDate.shiftedBy(4 * period));

        for (final LoggedEvent event : logger.getLoggedEvents()) {
            // recreate the g function
            final SpacecraftState sstate = event.getState();
            final Vector3D sunPos =
                sun.getPVCoordinates(sstate.getDate(), moonInertialFrame).getPosition();
            final Vector3D zenithDir = bodyPoint1.getNormal(sstate.getDate(), moonInertialFrame);
            final Vector3D sunProj = new Vector3D(sunPos.getX(), sunPos.getY(), 0).normalize();
            final Vector3D zenithDirProj =
                new Vector3D(zenithDir.getX(), zenithDir.getY(), 0).normalize();

            // Check that time and angle are equal
            final double angle = Vector3D.angle(sunProj, zenithDirProj);
            final double time = ((LocalTimeAngleDetector) event.getEventDetector()).getTime();
            System.out.println(sstate.getDate() + " " + angle);
            Assert.assertEquals(time, angle, 1E-09);
        }
    }

    /**
     * @testType UT
     *
     * @testedFeature Check that an error is raised when the frame is null
     *
     * @description propagates a spacecraft in mars orbit adding one BodyPointLocalTimeAngleDetector
     *              and using an events logger:
     *              An error is raised because the Frame of hthe BodyPointLocalTimeAngleDetector is null
     *
     * @input constructor parameters (with null frame) and a propagator
     *
     * @output The expected error
     *
     * @testPassCriteria The expected error is raised
     *
     * @referenceVersion 4.16
     *
     * @nonRegressionVersion 4.16
     *
     * @throws PatriusException
     *         should not happen here
     */
    @Test
    public void testNullFrameError() throws PatriusException {

        final CelestialBody sun = CelestialBodyFactory.getSun();
        final CelestialBodyFrame marsFrame = CelestialBodyFactory.getMars().getInertialFrame();

        // Mars moon orbit
        final Orbit moonOrbit =
            new KeplerianOrbit(9377.1E+03, 0.0151, FastMath.toRadians(1.075), 0.0, 0.0, 0.0,
                PositionAngle.TRUE, marsFrame, iniDate, Constants.JPL_SSD_MARS_SYSTEM_GM);
        final double period = moonOrbit.getKeplerianPeriod();

        // Mars moon orientation
        final IAUPoleCoefficients1D alpha0Coeffs = new IAUPoleCoefficients1D(null);
        final IAUPoleCoefficients1D delta0Coeffs = new IAUPoleCoefficients1D(null);
        final UnivariateDifferentiableFunction function =
            new PolynomialFunction(new double[] { 0., 2. * MathLib.PI * 86400. / period });
        final IAUPoleFunction moonRotation =
            new IAUPoleFunction(IAUPoleFunctionType.SECULAR, function, IAUTimeDependency.DAYS);
        final IAUPoleCoefficients1D wCoeffs =
            new IAUPoleCoefficients1D(Collections.singletonList(moonRotation));
        final IAUPoleCoefficients poleCoeffs =
            new IAUPoleCoefficients(alpha0Coeffs, delta0Coeffs, wCoeffs);

        // Mars moon body
        final UserIAUCelestialBody moonCelestialBody = new UserIAUCelestialBody("", moonOrbit, 0,
            new UserIAUPole(poleCoeffs), FramesFactory.getEME2000(), null);
        final CelestialBodyFrame moonInertialFrame =
            moonCelestialBody.getInertialFrame(IAUPoleModelType.CONSTANT);
        final CelestialBodyFrame moonRotatingFrame =
            moonCelestialBody.getRotatingFrame(IAUPoleModelType.MEAN);
        moonCelestialBody.setShape(new OneAxisEllipsoid(1.E4, 0., moonRotatingFrame));

        // Spacecraft orbit (non used for the event detection, but necessary for the propagator)
        final Orbit spacecraftOrbit =
            new KeplerianOrbit(6000.0E+03, 0.009, FastMath.toRadians(2), 0.0, 0.0, 0.0,
                PositionAngle.TRUE, marsFrame, iniDate, Constants.JPL_SSD_MARS_SYSTEM_GM);
        final Propagator propagator = new KeplerianPropagator(spacecraftOrbit);

        // step handler to track local time evolution during propagation
        final MyStepHandler angleTracking = new MyStepHandler(iniDate, moonInertialFrame);
        propagator.setMasterMode(10, angleTracking);

        final BodyPoint bodyPoint1 = moonCelestialBody.getShape()
            .buildPoint(new Vector3D(-1000., 1000., 1000.), "bodyPoint1");

        // creates the detector
        final BodyPointLocalTimeAngleDetector detector0 =
            new BodyPointLocalTimeAngleDetector(FastMath.PI * 0.5, bodyPoint1, 600, 1.e-6,
                null, Action.CONTINUE, false, sun, 2);

        // creates the logger
        final EventsLogger logger = new EventsLogger();

        // adds the logger to the propagator
        propagator.addEventDetector(logger.monitorDetector(detector0));

        // propagate and fail because null frame
        try {
            propagator.propagate(iniDate.shiftedBy(4 * period));
            // If this line is reached, it means no error is raised -> FAIL test
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertEquals("Frame is mandatory", e.getMessage());
        }
    }

}
