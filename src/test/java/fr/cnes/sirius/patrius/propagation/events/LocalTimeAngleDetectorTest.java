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
 * HISTORY
 * VERSION:4.13.1:FA:FA-177:17/01/2024:[PATRIUS] Reliquat OPENFD
 * VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
 * VERSION:4.13:FA:FA-79:08/12/2023:[PATRIUS] Probleme dans la fonction g de LocalTimeAngleDetector
 * VERSION:4.13:DM:DM-3:08/12/2023:[PATRIUS] Distinction entre corps celestes et barycentres
 * VERSION:4.13:FA:FA-118:08/12/2023:[PATRIUS] Calcul d'union de PyramidalField invalide
 * VERSION:4.13:DM:DM-37:08/12/2023:[PATRIUS] Date d'evenement et propagation du signal
 * VERSION:4.11.1:FA:FA-78:30/06/2023:[PATRIUS] Reliquat DM 3258 sur les TU des détecteurs SolarTime et LocalTime
 * VERSION:4.11.1:FA:FA-89:30/06/2023:[PATRIUS] Problème dans la fonction g de LocalTimeAngleDetector - Retour en arrière
 * VERSION:4.11:DM:DM-3258:22/05/2023:[PATRIUS] Adaptation des detecteurs SolarTime et LocalTime pour l'interplanetaire
 * VERSION:4.11:DM:DM-3311:22/05/2023:[PATRIUS] Evolutions mineures sur CelestialBody, shape et reperes
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration de la gestion des attractions gravitationnelles dans le propagateur
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.6:FA:FA-2502:27/01/2021:[PATRIUS] Choix des ephemerides solaires dans certains detecteurs - manque de cas de test 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:394:20/02/2015: Added possibility to define the action performed when event occurs
 * VERSION::FA:680:27/09/2016:correction local time computation
 * VERSION::FA:902:13/12/2016:corrected anomaly on local time computation
 * VERSION::DM:710:22/03/2016:local time angle computation in [-PI, PI[
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.events;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.CelestialPoint;
import fr.cnes.sirius.patrius.bodies.IAUPoleFactory;
import fr.cnes.sirius.patrius.bodies.IAUPoleModelType;
import fr.cnes.sirius.patrius.bodies.MeeusSun;
import fr.cnes.sirius.patrius.bodies.UserCelestialBody;
import fr.cnes.sirius.patrius.events.EventDetector;
import fr.cnes.sirius.patrius.events.EventDetector.Action;
import fr.cnes.sirius.patrius.events.detectors.AbstractSignalPropagationDetector.DatationChoice;
import fr.cnes.sirius.patrius.events.detectors.AbstractSignalPropagationDetector.PropagationDelayType;
import fr.cnes.sirius.patrius.events.detectors.LocalTimeAngleDetector;
import fr.cnes.sirius.patrius.events.postprocessing.EventsLogger;
import fr.cnes.sirius.patrius.events.postprocessing.EventsLogger.LoggedEvent;
import fr.cnes.sirius.patrius.events.utils.SignalPropagationWrapperDetector;
import fr.cnes.sirius.patrius.forces.gravity.DirectBodyAttraction;
import fr.cnes.sirius.patrius.forces.gravity.NewtonianGravityModel;
import fr.cnes.sirius.patrius.frames.CelestialBodyFrame;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.nonstiff.AdaptiveStepsizeIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.CircularOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusFixedStepHandler;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Unit tests for {@link LocalTimeAngleDetector}.
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id: LocalTimeAngleDetectorTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.1
 * 
 */
public class LocalTimeAngleDetectorTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Validate the satellite local time angle detector
         * 
         * @featureDescription Validate the satellite local time angle detector
         * 
         * @coveredRequirements DV-EVT_120, DV-EVT_50
         */
        VALIDATE_LOCAL_TIME_ANGLE_DETECTOR
    }

    /** A Circular retrograde orbit used for the tests. */
    private static CircularOrbit orbitRetro;

    /** A Circular prograde orbit used for the tests. */
    private static CircularOrbit orbitPro;

    /** A state used for the tests. */
    private static SpacecraftState state;

    /** Initial propagator date. */
    private static AbsoluteDate iniDate;

    /**
     * Setup for all unit tests in the class.
     * Provides an {@link Orbit}.
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @BeforeClass
    public static void setUpBeforeClass() throws PatriusException {
        // Orekit initialization
        Utils.setDataRoot("regular-dataCNES-2003");
        FramesFactory.setConfiguration(fr.cnes.sirius.patrius.Utils.getIERS2003ConfigurationWOEOP(true));

        iniDate = new AbsoluteDate("2011-11-09T12:00:00Z", TimeScalesFactory.getTT());
        final double mu = CelestialBodyFactory.getEarth().getGM();

        orbitRetro = new CircularOrbit(8000000, 0, 0, MathLib.toRadians(98.0), 0, FastMath.PI / 2, PositionAngle.TRUE,
                FramesFactory.getGCRF(), iniDate, mu);

        orbitPro = new CircularOrbit(8000000, 0, 0, MathLib.toRadians(60.0), 0, FastMath.PI / 2, PositionAngle.TRUE,
                FramesFactory.getGCRF(), iniDate, mu);

        state = new SpacecraftState(orbitRetro);
    }

    /**
     * @description Test this event detector wrap feature in {@link SignalPropagationWrapperDetector}
     * 
     * @input this event detector in INSTANTANEOUS & LIGHT_SPEED
     * 
     * @output the emitter & receiver dates
     * 
     * @testPassCriteria The results containers as expected (non regression)
     * 
     * @referenceVersion 4.13
     * 
     * @nonRegressionVersion 4.13
     */
    @Test
    public void testSignalPropagationWrapperDetector() throws PatriusException {

        // Build two identical event detectors (the first in INSTANTANEOUS, the second in LIGHT_SPEED)
        final CelestialBody sun = CelestialBodyFactory.getSun();
        final LocalTimeAngleDetector eventDetector1 = new LocalTimeAngleDetector(FastMath.PI * 0, 600, 1e-6, null,
            Action.CONTINUE, false, sun);
        final LocalTimeAngleDetector eventDetector2 = (LocalTimeAngleDetector) eventDetector1.copy();
        eventDetector2.setPropagationDelayType(PropagationDelayType.LIGHT_SPEED, FramesFactory.getGCRF());

        // Wrap these event detectors
        final SignalPropagationWrapperDetector wrapper1 = new SignalPropagationWrapperDetector(eventDetector1);
        final SignalPropagationWrapperDetector wrapper2 = new SignalPropagationWrapperDetector(eventDetector2);

        // Add them in the propagator, then propagate
        final Propagator propagator = new KeplerianPropagator(orbitPro);
        propagator.addEventDetector(wrapper1);
        propagator.addEventDetector(wrapper2);
        final SpacecraftState finalState = propagator.propagate(iniDate.shiftedBy(3 * 3600.));

        // Evaluate the first event detector wrapper (INSTANTANEOUS) (emitter dates should be equal to receiver dates)
        Assert.assertEquals(2, wrapper1.getNBOccurredEvents());
        Assert.assertTrue(wrapper1.getEmitterDatesList().get(0)
            .equals(new AbsoluteDate("2011-11-09T12:49:49.088"), 1e-3));
        Assert.assertTrue(wrapper1.getReceiverDatesList().get(0)
            .equals(new AbsoluteDate("2011-11-09T12:49:49.088"), 1e-3));
        Assert.assertTrue(wrapper1.getEmitterDatesList().get(1)
            .equals(new AbsoluteDate("2011-11-09T14:48:31.504"), 1e-3));
        Assert.assertTrue(wrapper1.getReceiverDatesList().get(1)
            .equals(new AbsoluteDate("2011-11-09T14:48:31.504"), 1e-3));

        // Evaluate the second event detector wrapper (LIGHT_SPEED) (emitter dates should be before receiver dates)
        Assert.assertEquals(2, wrapper2.getNBOccurredEvents());
        Assert.assertTrue(wrapper2.getEmitterDatesList().get(0)
            .equals(new AbsoluteDate("2011-11-09T12:41:34.737"), 1e-3));
        Assert.assertTrue(wrapper2.getReceiverDatesList().get(0)
            .equals(new AbsoluteDate("2011-11-09T12:49:48.995"), 1e-3));
        Assert.assertTrue(wrapper2.getEmitterDatesList().get(1)
            .equals(new AbsoluteDate("2011-11-09T14:40:17.163"), 1e-3));
        Assert.assertTrue(wrapper2.getReceiverDatesList().get(1)
            .equals(new AbsoluteDate("2011-11-09T14:48:31.411"), 1e-3));

        // Evaluate the AbstractSignalPropagationDetector's abstract methods implementation
        Assert.assertEquals(sun, eventDetector1.getEmitter(null));
        Assert.assertEquals(finalState.getOrbit(), eventDetector1.getReceiver(finalState));
        Assert.assertEquals(DatationChoice.RECEIVER, eventDetector1.getDatationChoice());
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#VVALIDATE_LOCAL_TIME_ANGLE_DETECTOR}
     * 
     * @testedMethod {@link LocalTimeAngleDetector#LocalTimeAngleDetector(double, double, double, fr.cnes.sirius.patrius.frames.CelestialBodyFrame, fr.cnes.sirius.patrius.propagation.events.Action, boolean, fr.cnes.sirius.patrius.bodies.CelestialBody, int) }
     * 
     * @description test of the slopeSelection parameter
     * 
     * @input constructor parameters: the local time angle, the max check
     *        value, the threshold value, the celestial body frame, the action,
     *        the remove boolean, the sun body and the slopeSelection.
     * 
     * @output a {@link LocalTimeDetector}
     * 
     * @testPassCriteria the {@link LocalTimeDetector} when initiated with increasing parameter only detect increasing
     *                   event.
     * 
     * @throws PatriusException If error occurs in the propagation or in the detector creation
     * 
     * @referenceVersion 4.11
     * 
     * @nonRegressionVersion 4.11
     */
    @Test
    public void testLocalTimeAngleDetectorIncreasingDecreasing() throws PatriusException {
        // Detector in increasing mode
        final LocalTimeAngleDetector detectorIncrease = new LocalTimeAngleDetector(FastMath.PI * 0, 600, 1e-6, null,
                Action.CONTINUE, false, CelestialBodyFactory.getSun(), 0);
        // Detector in decreasing mode
        final LocalTimeAngleDetector detectorDecrease = new LocalTimeAngleDetector(FastMath.PI * 0, 600, 1e-6, null,
                Action.CONTINUE, false, CelestialBodyFactory.getSun(), 1);
        // Detector in increasing & decreasing mode
        final LocalTimeAngleDetector detector = new LocalTimeAngleDetector(FastMath.PI * 0, 600, 1e-6, null,
                Action.CONTINUE, false, CelestialBodyFactory.getSun());

        final double period = orbitPro.getKeplerianPeriod();
        final Propagator propagator = new KeplerianPropagator(orbitPro);

        // Step handler to track longitude evolution during propagation
        final MyStepHandler angleTracking = new MyStepHandler(iniDate, orbitPro.getFrame());
        propagator.setMasterMode(10, angleTracking);

        // Create one logger for each detector
        final EventsLogger loggerIncrease = new EventsLogger();
        final EventsLogger loggerDecrease = new EventsLogger();
        final EventsLogger logger = new EventsLogger();

        // Add the three different loggers to the propagator
        propagator.addEventDetector(loggerIncrease.monitorDetector(detectorIncrease));
        propagator.addEventDetector(loggerDecrease.monitorDetector(detectorDecrease));
        propagator.addEventDetector(logger.monitorDetector(detector));

        // Propagate
        propagator.propagate(iniDate.shiftedBy(5 * period));
        // Asserts
        for (int i = 0; i < loggerIncrease.getLoggedEvents().size(); i++) {
            Assert.assertTrue(loggerIncrease.getLoggedEvents().get(i).isIncreasing());
        }
        for (int j = 0; j < loggerDecrease.getLoggedEvents().size(); j++) {
            Assert.assertFalse(loggerDecrease.getLoggedEvents().get(j).isIncreasing());
        }
        Assert.assertTrue(loggerIncrease.getLoggedEvents().size() + loggerDecrease.getLoggedEvents().size() == logger
                .getLoggedEvents().size());
        Assert.assertEquals(5, loggerIncrease.getLoggedEvents().size());
        Assert.assertEquals(0, loggerDecrease.getLoggedEvents().size());
        Assert.assertEquals(5, logger.getLoggedEvents().size());
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#VVALIDATE_LOCAL_TIME_ANGLE_DETECTOR}
     * 
     * @testedMethod {@link LocalTimeAngleDetector#LocalTimeAngleDetector(double, double, double, fr.cnes.sirius.patrius.frames.CelestialBodyFrame, fr.cnes.sirius.patrius.propagation.events.Action, boolean, fr.cnes.sirius.patrius.bodies.CelestialBody, int) }
     * 
     * @description test of the slopeSelection parameter
     * 
     * @input constructor parameters: the local time angle, the max check
     *        value, the threshold value, the celestial body frame, the action,
     *        the remove boolean, the sun body and the slopeSelection.
     * 
     * @output a {@link LocalTimeDetector}
     * 
     * @testPassCriteria the {@link LocalTimeDetector} when initiated with increasing parameter only detect increasing
     *                   event.
     * 
     * @throws PatriusException If error occurs in the propagation or in the detector creation
     * 
     * @referenceVersion 4.13
     * 
     * @nonRegressionVersion 4.13
     */
    @Test
    public void testLocalTimeAngleDetectorIncreasingDecreasingBackward() throws PatriusException {
        // Detector in increasing mode
        final LocalTimeAngleDetector detectorIncrease = new LocalTimeAngleDetector(FastMath.PI * 0, 600, 1e-6, null,
                Action.CONTINUE, false, CelestialBodyFactory.getSun(), 0);
        // Detector in decreasing mode
        final LocalTimeAngleDetector detectorDecrease = new LocalTimeAngleDetector(FastMath.PI * 0, 600, 1e-6, null,
                Action.CONTINUE, false, CelestialBodyFactory.getSun(), 1);
        // Detector in increasing & decreasing mode
        final LocalTimeAngleDetector detector = new LocalTimeAngleDetector(FastMath.PI * 0, 600, 1e-6, null,
                Action.CONTINUE, false, CelestialBodyFactory.getSun());

        final double period = orbitPro.getKeplerianPeriod();
        final Propagator propagator = new KeplerianPropagator(orbitPro);
        propagator.propagate(iniDate.shiftedBy(5 * period));

        // Step handler to track longitude evolution during propagation
        final MyStepHandler angleTracking = new MyStepHandler(iniDate, orbitPro.getFrame());
        propagator.setMasterMode(10, angleTracking);

        // Create one logger for each detector
        final EventsLogger loggerIncrease = new EventsLogger();
        final EventsLogger loggerDecrease = new EventsLogger();
        final EventsLogger logger = new EventsLogger();

        // Add the three different loggers to the propagator
        propagator.addEventDetector(loggerIncrease.monitorDetector(detectorIncrease));
        propagator.addEventDetector(loggerDecrease.monitorDetector(detectorDecrease));
        propagator.addEventDetector(logger.monitorDetector(detector));

        // Propagate backward
        propagator.propagate(iniDate);

        // Asserts
        Assert.assertEquals(5, loggerIncrease.getLoggedEvents().size());
        Assert.assertEquals(0, loggerDecrease.getLoggedEvents().size());
        Assert.assertEquals(5, logger.getLoggedEvents().size());
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_LOCAL_TIME_ANGLE_DETECTOR}
     * 
     * @testedMethod {@link LocalTimeAngleDetector#LocalTimeAngleDetector(double) }
     * 
     * @description simple constructor test
     * 
     * @input constructor parameters: the local time angle
     * 
     * @output a {@link LocalTimeAngleDetector}
     * 
     * @testPassCriteria the {@link LocalTimeAngleDetector} is successfully created
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void testLocalTimeAngleDetectorCtor1() throws PatriusException {
        final LocalTimeAngleDetector detector = new LocalTimeAngleDetector(-FastMath.PI);
        // The constructor did not crash...
        Assert.assertNotNull(detector);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#VVALIDATE_LOCAL_TIME_ANGLE_DETECTOR}
     * 
     * @testedMethod {@link LocalTimeAngleDetector#LocalTimeAngleDetector(double, double, double) }
     * 
     * @description simple constructor test
     * 
     * @input constructor parameters: the local time angle, the max check
     *        value and the threshold value.
     * 
     * @output a {@link LocalTimeDetector}
     * 
     * @testPassCriteria the {@link LocalTimeDetector} is successfully created
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void testLocalTimeAngleDetectorCtor2() throws PatriusException {
        final LocalTimeAngleDetector detector = new LocalTimeAngleDetector(-FastMath.PI, 500, 0.001);
        // The constructor did not crash...
        Assert.assertNotNull(detector);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VVALIDATE_LOCAL_TIME_ANGLE_DETECTOR}
     * 
     * @testedMethod {@link LocalTimeAngleDetector#LocalTimeAngleDetector(double, double, double, fr.cnes.sirius.patrius.frames.CelestialBodyFrame) }
     * 
     * @description simple constructor test
     * 
     * @input constructor parameters: the local time angle, the max check
     *        value, the threshold value and a celestial body frame.
     * 
     * @output a {@link LocalTimeDetector}
     * 
     * @testPassCriteria the {@link LocalTimeDetector} is successfully created
     * 
     * @throws PatriusException If an error occurs in the detector creation
     * 
     * @referenceVersion 4.11
     * 
     * @nonRegressionVersion 4.11
     */
    @Test
    public void testLocalTimeAngleDetectorCelestialBodyFrame() throws PatriusException {
        final CelestialPoint earth = CelestialBodyFactory.getEarth();
        final CelestialBodyFrame frame = new CelestialBodyFrame(FramesFactory.getGCRF(), Transform.IDENTITY, "Frame",
                true, earth);
        final LocalTimeAngleDetector detectorCelestialBody = new LocalTimeAngleDetector(-FastMath.PI, 600, 1.e-6, frame);
        // The constructor did not crash...
        Assert.assertNotNull(detectorCelestialBody);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_LOCAL_TIME_ANGLE_DETECTOR}
     * 
     * @testedMethod {@link LocalTimeAngleDetector#LocalTimeAngleDetector(double, double, double, fr.cnes.sirius.patrius.frames.CelestialBodyFrame)}
     * 
     * @description tests
     *              {@link LocalTimeAngleDetector#LocalTimeAngleDetector(double, double, double, fr.cnes.sirius.patrius.frames.CelestialBodyFrame)}
     * 
     * @input constructor
     * 
     * @output constructor outputs
     * 
     * @testPassCriteria the constructor return the right Patrius exception
     * 
     * @referenceVersion 4.11
     * 
     * @throws PatriusException
     *         if the frame is not pseudo inertial
     */
    @Test
    public void testExeptionNotPseudoInertialFrame() throws PatriusException {
        final CelestialPoint earth = CelestialBodyFactory.getEarth();
        final CelestialBodyFrame frame = new CelestialBodyFrame(earth.getEME2000(), Transform.IDENTITY, "Frame", earth);
        try {
            new LocalTimeAngleDetector(-FastMath.PI, 600, 1.e-6, frame);
        } catch (final PatriusException frameError) {
            Assert.assertTrue(frameError.getMessage().contains("The frame must be pseudo inertial"));
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_LOCAL_TIME_ANGLE_DETECTOR}
     * 
     * @testedMethod {@link LocalTimeAngleDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * 
     * @description tests {@link LocalTimeAngleDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * 
     * @input constructor and eventOccured parameters
     * 
     * @output eventOccured outputs
     * 
     * @testPassCriteria eventOccured returns the expected value (stop the propagation)
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @Test
    public void testEventOccurred() throws PatriusException {
        final LocalTimeAngleDetector detector = new LocalTimeAngleDetector(3.25 - 2. * FastMath.PI);
        final LocalTimeAngleDetector detector2 = (LocalTimeAngleDetector) detector.copy();
        // the integration direction is forward
        // a g decreasing event is detected: do not continue
        Action rez = detector2.eventOccurred(state, false, true);
        Assert.assertEquals(Action.STOP, rez);
        // a g increasing event is detected: do not continue
        rez = detector2.eventOccurred(state, true, true);
        Assert.assertEquals(Action.STOP, rez);

        // the integration direction is backward
        // a g decreasing event is detected: do not continue
        rez = detector2.eventOccurred(state, false, false);
        Assert.assertEquals(Action.STOP, rez);
        // a g increasing event is detected: do not continue
        rez = detector2.eventOccurred(state, true, false);
        Assert.assertEquals(Action.STOP, rez);

        Assert.assertEquals(Action.STOP, detector2.getAction());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_LOCAL_TIME_ANGLE_DETECTOR}
     * 
     * @testedMethod {@link LocalTimeAngleDetector#g(SpacecraftState)}
     * @testedMethod {@link LocalTimeAngleDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * 
     * @description propagates a retrograde orbit adding multiple LocalTimeAngleDetector and using an events logger:
     *              an event is detected when the angle between the satellite and earth projections on the equatorial
     *              plane is equal to one of the predetermined values.
     * 
     * @input constructor parameters, a propagator and an event logger
     * 
     * @output the local time events logged during the propagation
     * 
     * @testPassCriteria check that when an event is logged during the propagation, the angle between
     *                   the two vector projections is equal to the predetermined value.
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @Test
    public void testRetrogradeOrbit() throws PatriusException {

        final double period = orbitRetro.getKeplerianPeriod();

        final Propagator propagator = new KeplerianPropagator(orbitRetro);

        // Step handler to track longitude evolution during propagation
        final MyStepHandler angleTracking = new MyStepHandler(iniDate, FramesFactory.getTIRF());
        propagator.setMasterMode(10, angleTracking);

        // detects the position angle = 0:
        final LocalTimeAngleDetector detector0 = new LocalTimeAngleDetector(FastMath.PI * 0) {

            private static final long serialVersionUID = 4529781719692037999L;

            @Override
            public Action eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward) {
                return Action.CONTINUE;
            }
        };
        // detects the position angle = 0.4 * PI:
        final LocalTimeAngleDetector detector04 = new LocalTimeAngleDetector(FastMath.PI * 0.4) {

            private static final long serialVersionUID = 4529781719692037999L;

            @Override
            public Action eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward) {
                return Action.CONTINUE;
            }
        };
        // detects the position angle = PI:
        final LocalTimeAngleDetector detectorPI = new LocalTimeAngleDetector(-FastMath.PI) {

            private static final long serialVersionUID = 4529781719692037999L;

            @Override
            public Action eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward) {
                return Action.CONTINUE;
            }
        };
        // detects the position angle = - PI * 0.2:
        final LocalTimeAngleDetector detector18 = new LocalTimeAngleDetector(-FastMath.PI * 0.2) {

            private static final long serialVersionUID = 4529781719692037999L;

            @Override
            public Action eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward) {
                return Action.CONTINUE;
            }
        };
        final EventsLogger logger = new EventsLogger();

        propagator.addEventDetector(logger.monitorDetector(detector0));
        propagator.addEventDetector(logger.monitorDetector(detector04));
        propagator.addEventDetector(logger.monitorDetector(detectorPI));
        propagator.addEventDetector(logger.monitorDetector(detector18));

        propagator.propagate(iniDate.shiftedBy(3 * period));
        // re-implement the g function for the first 0*PI detected event:
        Assert.assertEquals(12, logger.getLoggedEvents().size());
        for (final LoggedEvent event : logger.getLoggedEvents()) {
            final SpacecraftState sstate = event.getState();
            final Vector3D sun = CelestialBodyFactory.getSun().getPVCoordinates(sstate.getDate(), sstate.getFrame())
                    .getPosition();
            final Vector3D satellite = sstate.getPVCoordinates(sstate.getFrame()).getPosition();
            final Vector3D sun2 = new Vector3D(sun.getX(), sun.getY(), 0).normalize();
            final Vector3D satellite2 = new Vector3D(satellite.getX(), satellite.getY(), 0).normalize();
            final double angle = Vector3D.angle(sun2, satellite2);

            final double time = ((LocalTimeAngleDetector) event.getEventDetector()).getTime();
            if (time == 0) {
                Assert.assertEquals(0, angle, 1E-09);
            } else if (time == 0.4 * FastMath.PI) {
                Assert.assertEquals(0.4 * FastMath.PI, angle, 1E-08);
            } else if (time == FastMath.PI) {
                Assert.assertEquals(FastMath.PI, angle, 1E-08);
            } else if (time == -0.2 * FastMath.PI) {
                Assert.assertEquals(0.2 * FastMath.PI, angle, 1E-09);
            }
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_LOCAL_TIME_ANGLE_DETECTOR}
     * 
     * @testedMethod {@link LocalTimeAngleDetector#g(SpacecraftState)}
     * @testedMethod {@link LocalTimeAngleDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * 
     * @description propagates a prograde orbit adding multiple LocalTimeDetector and using an events logger:
     *              an event is detected when the angle between the satellite and earth projections on the equatorial
     *              plane is equal to one of the predetermined values.
     * 
     * @input constructor parameters, a propagator and an event logger
     * 
     * @output the local time events logged during the propagation
     * 
     * @testPassCriteria check that when an event is logged during the propagation, the angle between
     *                   the two vector projections is equal to the predetermined value.
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @Test
    public void testProgradeOrbit() throws PatriusException {
        final double period = orbitPro.getKeplerianPeriod();
        final Propagator propagator = new KeplerianPropagator(orbitPro);

        // Step handler to track longitude evolution during propagation
        final MyStepHandler angleTracking = new MyStepHandler(iniDate, FramesFactory.getTIRF());
        propagator.setMasterMode(10, angleTracking);

        // detects the position angle = 0:
        final LocalTimeAngleDetector detector0 = new LocalTimeAngleDetector(FastMath.PI * 0) {

            private static final long serialVersionUID = 4529781719692037999L;

            @Override
            public Action eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward) {
                return Action.CONTINUE;
            }
        };
        // detects the position angle = 0.4 * PI:
        final LocalTimeAngleDetector detector04 = new LocalTimeAngleDetector(FastMath.PI * 0.4) {

            private static final long serialVersionUID = 4529781719692037999L;

            @Override
            public Action eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward) {
                return Action.CONTINUE;
            }
        };
        // detects the position angle = PI:
        final LocalTimeAngleDetector detectorPI = new LocalTimeAngleDetector(-FastMath.PI) {

            private static final long serialVersionUID = 4529781719692037999L;

            @Override
            public Action eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward) {
                return Action.CONTINUE;
            }
        };
        // detects the position angle = - PI * 0.2:
        final LocalTimeAngleDetector detector18 = new LocalTimeAngleDetector(-FastMath.PI * 0.2) {

            private static final long serialVersionUID = 4529781719692037999L;

            @Override
            public Action eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward) {
                return Action.CONTINUE;
            }
        };
        final EventsLogger logger = new EventsLogger();

        propagator.addEventDetector(logger.monitorDetector(detector0));
        propagator.addEventDetector(logger.monitorDetector(detector04));
        propagator.addEventDetector(logger.monitorDetector(detectorPI));
        propagator.addEventDetector(logger.monitorDetector(detector18));

        propagator.propagate(iniDate.shiftedBy(3 * period));

        // re-implement the g function for the first 0*PI detected event:
        Assert.assertEquals(12, logger.getLoggedEvents().size());
        for (final LoggedEvent event : logger.getLoggedEvents()) {
            final SpacecraftState sstate = event.getState();
            final Vector3D sun = CelestialBodyFactory.getSun().getPVCoordinates(sstate.getDate(), sstate.getFrame())
                    .getPosition();
            final Vector3D satellite = sstate.getPVCoordinates().getPosition();
            final Vector3D sun2 = new Vector3D(sun.getX(), sun.getY(), 0).normalize();
            final Vector3D satellite2 = new Vector3D(satellite.getX(), satellite.getY(), 0).normalize();
            final double angle = Vector3D.angle(sun2, satellite2);
            final double time = ((LocalTimeAngleDetector) event.getEventDetector()).getTime();
            if (time == 0) {
                Assert.assertEquals(0, angle, 1E-09);
            } else if (time == 0.4 * FastMath.PI) {
                Assert.assertEquals(0.4 * FastMath.PI, angle, 1E-08);
            } else if (time == FastMath.PI) {
                Assert.assertEquals(0, angle, 1E-08);
            } else if (time == -0.2 * FastMath.PI) {
                Assert.assertEquals(0.2 * FastMath.PI, angle, 1E-09);
            }
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_LOCAL_TIME_ANGLE_DETECTOR}
     * 
     * @testedMethod {@link LocalTimeAngleDetector#g(SpacecraftState)}
     * 
     * @description tests g(SpacecraftState) propagating an elliptic
     *              orbit during one period with a numerical propagator.
     * 
     * @input constructor parameters and a numerical propagator
     * 
     * @output the spacecraft state when the event is detected
     * 
     * @testPassCriteria the local time event is properly detected.
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @Test
    public void testNumericalPropagator() throws PatriusException {
        final double period = orbitRetro.getKeplerianPeriod();
        final double[] absTOL = { 1e-5, 1e-5, 1e-5, 1e-8, 1e-8, 1e-8 };
        final double[] relTOL = { 1e-10, 1e-10, 1e-10, 1e-10, 1e-10, 1e-10 };
        final AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(0.1, 60., absTOL, relTOL);
        final NumericalPropagator propagator = new NumericalPropagator(integrator);
        propagator.resetInitialState(new SpacecraftState(orbitRetro));
        propagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(propagator.getInitialState()
                .getMu())));
        // detects the position angle = 3 * PI / 2:
        final LocalTimeAngleDetector detector = new LocalTimeAngleDetector(MathLib.toRadians(-1.568));
        propagator.addEventDetector(detector);
        final SpacecraftState curState = propagator.propagate(iniDate.shiftedBy(period));
        // re-implement the g-function:
        final Vector3D sun = CelestialBodyFactory.getSun().getPVCoordinates(curState.getDate(), curState.getFrame())
                .getPosition();
        final Vector3D satellite = curState.getPVCoordinates(curState.getFrame()).getPosition();
        final Vector3D sun2 = new Vector3D(sun.getX(), sun.getY(), 0).normalize();
        final Vector3D satellite2 = new Vector3D(satellite.getX(), satellite.getY(), 0).normalize();
        final double angle = Vector3D.angle(sun2, satellite2);
        Assert.assertEquals(MathLib.toRadians(1.568), angle, Utils.epsilonTest);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_LOCAL_TIME_ANGLE_DETECTOR}
     * 
     * @testedMethod {@link LocalTimeAngleDetector#LocalTimeAngleDetector(double) }
     * 
     * @description Check exception properly thrown if provided angle is out of range [-Pi; Pi[
     * 
     * @input out of range local time
     * 
     * @output exception
     * 
     * @testPassCriteria exception properly thrown
     * 
     * @referenceVersion 3.4
     * 
     * @nonRegressionVersion 3.4
     */
    @Test
    public void testOutOfRangeLocalTime() {
        try {
            new LocalTimeAngleDetector(-FastMath.PI - 1E-14);
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }
        try {
            new LocalTimeAngleDetector(-FastMath.PI);
            Assert.assertTrue(true);
        } catch (final PatriusException e) {
            Assert.fail();
        }
        try {
            new LocalTimeAngleDetector(FastMath.PI);
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }
        try {
            new LocalTimeAngleDetector(FastMath.PI - 1E-14);
            Assert.assertTrue(true);
        } catch (final PatriusException e1) {
            Assert.fail();
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_LOCAL_TIME_ANGLE_DETECTOR}
     * 
     * @testedMethod {@link LocalTimeAngleDetector#LocalTimeAngleDetector(double, double, double, Action, boolean, CelestialPoint)}
     * 
     * @description checks user Sun model is properly taken into account
     * 
     * @input constructor parameters : Sun model
     * 
     * @output g value
     * 
     * @testPassCriteria g value is the same when using default Sun and CelestialBodyFactory.getSun() and different when
     *                   using MeeusSun
     * 
     * @referenceVersion 4.6
     * 
     * @nonRegressionVersion 4.6
     */
    @Test
    public void sunConstructorTest() throws PatriusException {
        // Same detectors
        final EventDetector detector2 = new LocalTimeAngleDetector(-0.2, 0, 0, Action.CONTINUE, false);
        final EventDetector detector3 = new LocalTimeAngleDetector(-0.2, 0, 0, Action.CONTINUE, false,
                CelestialBodyFactory.getSun());
        // Different detector
        final EventDetector detector1 = new LocalTimeAngleDetector(-0.2, 0, 0, Action.CONTINUE, false, new MeeusSun());
        // Checks
        Assert.assertTrue(detector2.g(state) == detector3.g(state));
        Assert.assertFalse(detector1.g(state) == detector2.g(state));

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_LOCAL_TIME_ANGLE_DETECTOR}
     * 
     * @testedMethod {@link LocalTimeAngleDetector#g(SpacecraftState)}
     * @testedMethod {@link LocalTimeAngleDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * 
     * @description propagates a spacecraft in mars orbit adding multiple LocalTimeDetector and using an events logger:
     *              an event is detected when the angle between the satellite and mars moon projections on the
     *              equatorial
     *              plane is equal to one of the predetermined values.
     * 
     * @input constructor parameters, a propagator and an event logger
     * 
     * @output the local time events logged during the propagation
     * 
     * @testPassCriteria check that when an event is logged during the propagation, the angle between
     *                   the two vector projections is equal to the predetermined value.
     * 
     * @referenceVersion 4.11
     * 
     * @nonRegressionVersion 4.11
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @Test
    public void testLocalTimeAngleDetectorMarsMoon() throws PatriusException {
        final CelestialBodyFrame marsFrame = CelestialBodyFactory.getMars().getInertialFrame(IAUPoleModelType.TRUE);
        // Mars moon orbit
        final Orbit moonOrbit = new KeplerianOrbit(9377.1E+03, 0.0151, FastMath.toRadians(1.075), 0.0, 0.0, 0.0,
                PositionAngle.TRUE, marsFrame, iniDate, Constants.JPL_SSD_MARS_SYSTEM_GM);
        // Mars moon body
        final UserCelestialBody moonCelestialBody = new UserCelestialBody("", moonOrbit, 0,
                IAUPoleFactory.getIAUPole(null), FramesFactory.getEME2000(), null);

        final Orbit spacecraftOrbit = new KeplerianOrbit(6000.0E+03, 0.009, FastMath.toRadians(2), 0.0, 0.0, 0.0,
                PositionAngle.TRUE, marsFrame, iniDate, Constants.JPL_SSD_MARS_SYSTEM_GM);
        final double period = spacecraftOrbit.getKeplerianPeriod();
        final Propagator propagator = new KeplerianPropagator(spacecraftOrbit);

        // step handler to track longitude evolution during propagation
        final MyStepHandler angleTracking = new MyStepHandler(iniDate,
                moonCelestialBody.getInertialFrame(IAUPoleModelType.TRUE));
        propagator.setMasterMode(10, angleTracking);

        // detects the position angle = 0:
        final LocalTimeAngleDetector detector0 = new LocalTimeAngleDetector(FastMath.PI * 0, 600, 1.e-6,
                moonCelestialBody.getInertialFrame(IAUPoleModelType.TRUE), Action.CONTINUE);

        // detects the position angle = 0.4 * PI:
        final LocalTimeAngleDetector detector04 = new LocalTimeAngleDetector(FastMath.PI * 0.4, 600, 1.e-6,
                moonCelestialBody.getInertialFrame(IAUPoleModelType.TRUE), Action.CONTINUE);

        // detects the position angle = PI:
        final LocalTimeAngleDetector detectorPI = new LocalTimeAngleDetector(-FastMath.PI, 600, 1.e-6,
                moonCelestialBody.getInertialFrame(IAUPoleModelType.TRUE), Action.CONTINUE);

        // creates the logger
        final EventsLogger logger = new EventsLogger();
        // adds the logger to the propagator
        propagator.addEventDetector(logger.monitorDetector(detector0));
        propagator.addEventDetector(logger.monitorDetector(detector04));
        propagator.addEventDetector(logger.monitorDetector(detectorPI));
        // propagate
        propagator.propagate(iniDate.shiftedBy(4 * period));

        for (final LoggedEvent event : logger.getLoggedEvents()) {
            // recreate the g function
            final SpacecraftState sstate = event.getState();
            final Vector3D sun = CelestialBodyFactory.getSun()
                    .getPVCoordinates(sstate.getDate(), moonCelestialBody.getInertialFrame(IAUPoleModelType.TRUE))
                    .getPosition();
            final Vector3D satellite = sstate.getPVCoordinates(
                    moonCelestialBody.getInertialFrame(IAUPoleModelType.TRUE)).getPosition();
            final Vector3D sun2 = new Vector3D(sun.getX(), sun.getY(), 0).normalize();
            final Vector3D satellite2 = new Vector3D(satellite.getX(), satellite.getY(), 0).normalize();

            // Check that time and angle are equal
            final double angle = Vector3D.angle(sun2, satellite2);
            final double time = ((LocalTimeAngleDetector) event.getEventDetector()).getTime();
            if (time == 0) {
                Assert.assertEquals(0, angle, 1E-09);
            } else if (time == 0.4 * FastMath.PI) {
                Assert.assertEquals(0.4 * FastMath.PI, angle, 1E-08);
            } else if (time == -FastMath.PI) {
                Assert.assertEquals(FastMath.PI, angle, 1E-08);
            }
        }
    }

    /**
     * 
     * Implementation of a step handler to track the evolution
     * of the physical value to test
     * 
     */
    class MyStepHandler implements PatriusFixedStepHandler {

        /** Serializable UID. */
        private static final long serialVersionUID = 5643736397373186L;

        /** results */
        public ArrayList<double[]> results;

        /** initial date */
        private final AbsoluteDate fromDate;

        /** initial date */
        private final Frame bodyFrame;

        /** The Sun. */
        private final CelestialPoint sun;

        /**
         * simple constructor
         * 
         * @param date
         *        initialDate of propagation
         * @throws PatriusException
         */
        public MyStepHandler(final AbsoluteDate date,
                final Frame frame) throws PatriusException {
            this.results = new ArrayList<>();
            this.fromDate = date;
            this.bodyFrame = frame;
            this.sun = CelestialBodyFactory.getSun();
        }

        @Override
        public void init(final SpacecraftState s0,
                final AbsoluteDate t) {
            // nothing to do
        }

        @Override
        public void handleStep(final SpacecraftState s, final boolean isLast) {

            try {
                // Getting the position of the satellite:
                final Vector3D sPV = s.getPVCoordinates(this.bodyFrame).getPosition();
                final Vector3D sPVproj = new Vector3D(sPV.getX(), sPV.getY(), .0);
                // Getting the position of the sun:
                final Vector3D sunPV = this.sun.getPVCoordinates(s.getDate(), this.bodyFrame).getPosition();
                final Vector3D sunPVproj = new Vector3D(sunPV.getX(), sunPV.getY(), .0);
                // Computing the angle between the sun and satellite projections over the equatorial plane:
                double angle = Vector3D.angle(sunPVproj, sPVproj);
                if (Vector3D.crossProduct(sunPVproj, sPVproj).getZ() < 0) {
                    // The "angle" function returns a value between 0 and PI, while we are working with angle between
                    // -PI and PI
                    // : when z-component of the cross product between the two vectors is negative, returning -1 *
                    // angle:
                    angle = -angle;
                }
                final double[] currentResult = { s.getDate().durationFrom(this.fromDate), angle };

                this.results.add(currentResult);
            } catch (final PatriusException e) {
                e.printStackTrace();
            }
        }
    }
}
