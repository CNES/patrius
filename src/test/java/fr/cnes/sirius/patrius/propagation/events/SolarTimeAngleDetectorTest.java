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
 *
 * HISTORY
 * VERSION:4.13.1:FA:FA-177:17/01/2024:[PATRIUS] Reliquat OPENFD
 * VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
 * VERSION:4.13:DM:DM-3:08/12/2023:[PATRIUS] Distinction entre corps celestes et barycentres
 * VERSION:4.13:FA:FA-118:08/12/2023:[PATRIUS] Calcul d'union de PyramidalField invalide
 * VERSION:4.13:DM:DM-37:08/12/2023:[PATRIUS] Date d'evenement et propagation du signal
 * VERSION:4.11.1:FA:FA-78:30/06/2023:[PATRIUS] Reliquat DM 3258 sur les TU des détecteurs SolarTime et LocalTime
 * VERSION:4.11:DM:DM-3258:22/05/2023:[PATRIUS] Adaptation des detecteurs SolarTime et LocalTime pour l'interplanetaire
 * VERSION:4.11:DM:DM-3311:22/05/2023:[PATRIUS] Evolutions mineures sur CelestialBody, shape et reperes
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration de la gestion des attractions gravitationnelles dans le propagateur
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2414:27/05/2020:Choix des ephemeris solaires dans certains detecteurs 
 * VERSION:4.3:DM:DM-2001:15/05/2019:[Patrius] Donner le Soleil en entree de SolarTimeAngleDetector
 * VERSION:4.3:DM:DM-2090:15/05/2019:[PATRIUS] ajout de fonctionnalites aux bibliotheques mathematiques
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:394:20/02/2015: Added possibility to define the action performed when event occurs
 * VERSION::DM:710:22/03/2016:local time angle computation in [-PI, PI[
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * VERSION::FA:1777:04/10/2018:correct ICRF parent frame
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.events;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.CelestialPoint;
import fr.cnes.sirius.patrius.bodies.IAUPoleFactory;
import fr.cnes.sirius.patrius.bodies.IAUPoleModelType;
import fr.cnes.sirius.patrius.bodies.MeeusSun;
import fr.cnes.sirius.patrius.bodies.UserCelestialBody;
import fr.cnes.sirius.patrius.events.EventDetector.Action;
import fr.cnes.sirius.patrius.events.detectors.AbstractSignalPropagationDetector.DatationChoice;
import fr.cnes.sirius.patrius.events.detectors.AbstractSignalPropagationDetector.PropagationDelayType;
import fr.cnes.sirius.patrius.events.detectors.SolarTimeAngleDetector;
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
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.CircularOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Unit tests for {@link SolarTimeAngleDetector}.
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id: SolarTimeAngleDetectorTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.2
 * 
 */
public class SolarTimeAngleDetectorTest {

    /** Comparison epsilon for angles. */
    private static final double ANGEPS = 1E-09;

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Validate the satellite solar time angle detector
         * 
         * @featureDescription Validate the satellite solar time angle detector
         * 
         * @coveredRequirements DV-EVT_120
         */
        VALIDATE_SOLAR_TIME_ANGLE_DETECTOR
    }

    /** A Circular orbit used for the tests. */
    private static CircularOrbit orbit;

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
        CelestialBodyFactory.clearCelestialBodyLoaders();
        Utils.setDataRoot("regular-dataCNES-2003");
        FramesFactory.setConfiguration(fr.cnes.sirius.patrius.Utils.getIERS2003Configuration(true));

        iniDate = new AbsoluteDate("2010-10-10T12:00:00Z", TimeScalesFactory.getTT());
        final double mu = CelestialBodyFactory.getEarth().getGM();

        orbit = new CircularOrbit(9000000, 0, 0, 0.3, 0, FastMath.PI / 2,
            PositionAngle.TRUE, FramesFactory.getGCRF(), iniDate, mu);
        state = new SpacecraftState(orbit);
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
        final CelestialPoint sun = new MeeusSun();
        final SolarTimeAngleDetector eventDetector1 = new SolarTimeAngleDetector(0, sun, 100, 0.001, Action.CONTINUE,
            false);
        final SolarTimeAngleDetector eventDetector2 = (SolarTimeAngleDetector) eventDetector1.copy();
        eventDetector2.setPropagationDelayType(PropagationDelayType.LIGHT_SPEED, FramesFactory.getGCRF());

        // Wrap these event detectors
        final SignalPropagationWrapperDetector wrapper1 = new SignalPropagationWrapperDetector(eventDetector1);
        final SignalPropagationWrapperDetector wrapper2 = new SignalPropagationWrapperDetector(eventDetector2);

        // Add them in the propagator, then propagate
        final Propagator propagator = new KeplerianPropagator(orbit);
        propagator.addEventDetector(wrapper1);
        propagator.addEventDetector(wrapper2);
        final SpacecraftState finalState = propagator.propagate(iniDate.shiftedBy(4 * 3600.));

        // Evaluate the first event detector wrapper (INSTANTANEOUS) (emitter dates should be equal to receiver dates)
        Assert.assertEquals(2, wrapper1.getNBOccurredEvents());
        Assert.assertTrue(wrapper1.getEmitterDatesList().get(0)
            .equals(new AbsoluteDate("2010-10-10T12:41:30.164"), 1e-3));
        Assert.assertTrue(wrapper1.getReceiverDatesList().get(0)
            .equals(new AbsoluteDate("2010-10-10T12:41:30.164"), 1e-3));
        Assert.assertTrue(wrapper1.getEmitterDatesList().get(1)
            .equals(new AbsoluteDate("2010-10-10T15:03:09.626"), 1e-3));
        Assert.assertTrue(wrapper1.getReceiverDatesList().get(1)
            .equals(new AbsoluteDate("2010-10-10T15:03:09.626"), 1e-3));

        // Evaluate the second event detector wrapper (LIGHT_SPEED) (emitter dates should be before receiver dates)
        Assert.assertEquals(2, wrapper2.getNBOccurredEvents());
        Assert.assertTrue(wrapper2.getEmitterDatesList().get(0)
            .equals(new AbsoluteDate("2010-10-10T12:33:11.772"), 1e-3));
        Assert.assertTrue(wrapper2.getReceiverDatesList().get(0)
            .equals(new AbsoluteDate("2010-10-10T12:41:30.030"), 1e-3));
        Assert.assertTrue(wrapper2.getEmitterDatesList().get(1)
            .equals(new AbsoluteDate("2010-10-10T14:54:51.248"), 1e-3));
        Assert.assertTrue(wrapper2.getReceiverDatesList().get(1)
            .equals(new AbsoluteDate("2010-10-10T15:03:09.492"), 1e-3));

        // Evaluate the AbstractSignalPropagationDetector's abstract methods implementation
        Assert.assertEquals(sun, eventDetector1.getEmitter(null));
        Assert.assertEquals(finalState.getOrbit(), eventDetector1.getReceiver(finalState));
        Assert.assertEquals(DatationChoice.RECEIVER, eventDetector1.getDatationChoice());
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_SOLAR_TIME_ANGLE_DETECTOR}
     * 
     * @testedMethod {@link SolarTimeAngleDetector#SolarTimeAngleDetector(double) }
     * 
     * @description simple constructor test
     * 
     * @input constructor parameters: the solar time angle
     * 
     * @output a {@link SolarTimeAngleDetector}
     * 
     * @testPassCriteria the {@link SolarTimeAngleDetector} is successfully created
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public void testSolarTimeAngleDetectorCtor1() throws PatriusException {
        final SolarTimeAngleDetector detector = new SolarTimeAngleDetector(-FastMath.PI);
        // The constructor did not crash...
        Assert.assertNotNull(detector);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_SOLAR_TIME_ANGLE_DETECTOR}
     * 
     * @testedMethod {@link SolarTimeAngleDetector#SolarTimeAngleDetector(double, double, double) }
     * 
     * @description simple constructor test
     * 
     * @input constructor parameters: the solar time angle, the max check
     *        value and the threshold value.
     * 
     * @output a {@link SolarTimeAngleDetector}
     * 
     * @testPassCriteria the {@link SolarTimeAngleDetector} is successfully created
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void testSolarTimeAngleDetectorCtor2() throws PatriusException {
        final SolarTimeAngleDetector detector = new SolarTimeAngleDetector(-FastMath.PI, 500, 0.001);
        // The constructor did not crash...
        Assert.assertNotNull(detector);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_SOLAR_TIME_ANGLE_DETECTOR}
     * 
     * @testedMethod {@link SolarTimeAngleDetector#SolarTimeAngleDetector(double, double, double, Action) }
     * 
     * @description simple constructor test
     * 
     * @input constructor parameters: the solar time angle, the max check
     *        value and the threshold value et STOP action.
     * 
     * @output a {@link SolarTimeAngleDetector}
     * 
     * @testPassCriteria the {@link SolarTimeAngleDetector} is successfully created
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testSolarTimeAngleDetectorCtor3() throws PatriusException {
        final SolarTimeAngleDetector detector = new SolarTimeAngleDetector(-FastMath.PI, 500, 0.001, Action.STOP);
        final SolarTimeAngleDetector detector2 = (SolarTimeAngleDetector) detector.copy();
        // Test getters
        Assert.assertEquals(-FastMath.PI, detector2.getTime(), Utils.epsilonTest);

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_SOLAR_TIME_ANGLE_DETECTOR}
     * 
     * @testedMethod {@link SolarTimeAngleDetector#SolarTimeAngleDetector(double, double, double, 
     *                fr.cnes.sirius.patrius.frames.CelestialBodyFrame) }
     * 
     * @description simple constructor test
     * 
     * @input constructor parameters: the solar time angle, the max check
     *        value, the threshold value and the celestial body frame.
     * 
     * @output a {@link SolarTimeAngleDetector}
     * 
     * @testPassCriteria the {@link SolarTimeAngleDetector} is successfully created
     * 
     * @throws PatriusException If an error occurs at the detector or at the CelestialBody creation
     * 
     * @referenceVersion 4.11
     * 
     * @nonRegressionVersion 4.11
     */
    @Test
    public void testSolarTimeAngleDetectorCelestialBodyFrame() throws PatriusException {
        final CelestialPoint earth = CelestialBodyFactory.getEarth();
        final CelestialBodyFrame frame = new CelestialBodyFrame(earth.getICRF(), Transform.IDENTITY, "Frame", earth);
        final SolarTimeAngleDetector detectorCelestialBody = new SolarTimeAngleDetector(-FastMath.PI, 
            600, 1.e-6, frame);
        // The constructor did not crash...
        Assert.assertNotNull(detectorCelestialBody);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_SOLAR_TIME_ANGLE_DETECTOR}
     * 
     * @testedMethod {@link SolarTimeAngleDetector#SolarTimeAngleDetector(double, fr.cnes.sirius.patrius.bodies.CelestialPoint, double, double, Action, boolean) }
     * 
     * @description check user provided Sun model is properly taken into account by checking g function is exactly 0
     *              with sun aligned with spacecraft and Earth for an angle of 0
     * 
     * @input sun model
     * 
     * @output g value
     * 
     * @testPassCriteria the {@link SolarTimeAngleDetector} is successfully created
     * 
     * @referenceVersion 4.3
     * 
     * @nonRegressionVersion 4.3
     */
    @Test
    public void testSolarTimeAngleDetectorSun() throws PatriusException {
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Frame frame = FramesFactory.getGCRF();

        // Detector with Meeus model
        final CelestialPoint sun = new MeeusSun();
        final SolarTimeAngleDetector detector = new SolarTimeAngleDetector(0, sun, 100, 0.001, Action.STOP, false);

        // Test with Meeus model
        final PVCoordinates sunPV = sun.getPVCoordinates(date, frame);
        final PVCoordinates orbitPV =
            new PVCoordinates(sunPV.getPosition().scalarMultiply(0.5), new Vector3D(7000, 0, 0));
        final Orbit orbit = new CartesianOrbit(orbitPV, frame, date, Constants.GRS80_EARTH_MU);
        final SpacecraftState state = new SpacecraftState(orbit);
        Assert.assertEquals(0, detector.g(state), 1E-15);

        // Test with other model (JPL) - Difference expected
        final CelestialPoint sun2 = CelestialBodyFactory.getSun();
        final PVCoordinates sunPV2 = sun2.getPVCoordinates(date, frame);
        final PVCoordinates orbitPV2 =
            new PVCoordinates(sunPV2.getPosition().scalarMultiply(0.5), new Vector3D(7000, 0, 0));
        final Orbit orbit2 = new CartesianOrbit(orbitPV2, frame, date, Constants.GRS80_EARTH_MU);
        final SpacecraftState state2 = new SpacecraftState(orbit2);
        Assert.assertFalse(MathLib.abs(detector.g(state2)) < 1E-10);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_SOLAR_TIME_ANGLE_DETECTOR}
     * 
     * @testedMethod {@link SolarTimeAngleDetector#SolarTimeAngleDetector(double,
     *               fr.cnes.sirius.patrius.bodies.CelestialBody, double, double,
     *               fr.cnes.sirius.patrius.frames.CelestialBodyFrame Action, boolean) }
     * 
     * @description check user provided Sun model and CelestialBodyFrame is properly taken into account by checking g
     *              function is exactly 0
     *              with sun aligned with spacecraft and Earth for an angle of 0
     * 
     * @input sun model
     * 
     * @output g value
     * 
     * @testPassCriteria the {@link SolarTimeAngleDetector} is successfully created
     * 
     * @throws PatriusException If an error occurs in the detector or the CelestialBody creation
     * 
     */
    @Test
    public void testSolarTimeAngleCelestialBodyFrameDetectorSun() throws PatriusException {
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;

        // Detector with Meeus model
        final CelestialPoint sun = new MeeusSun();
        final CelestialPoint earth = CelestialBodyFactory.getEarth();
        final CelestialBodyFrame frame = new CelestialBodyFrame(earth.getICRF(), Transform.IDENTITY, "Frame", earth);
        final SolarTimeAngleDetector detector = new SolarTimeAngleDetector(0, sun, 100, 0.001, frame, Action.STOP,
            false);

        // Test with Meeus model
        final PVCoordinates sunPV = sun.getPVCoordinates(date, frame);
        final PVCoordinates orbitPV =
            new PVCoordinates(sunPV.getPosition().scalarMultiply(0.5), new Vector3D(7000, 0, 0));
        final Orbit orbit = new CartesianOrbit(orbitPV, frame, date, Constants.GRS80_EARTH_MU);
        final SpacecraftState state = new SpacecraftState(orbit);
        Assert.assertEquals(0, detector.g(state), 1E-15);

        // Test with other model (JPL) - Difference expected
        final CelestialPoint sun2 = CelestialBodyFactory.getSun();
        final PVCoordinates sunPV2 = sun2.getPVCoordinates(date, frame);
        final PVCoordinates orbitPV2 =
            new PVCoordinates(sunPV2.getPosition().scalarMultiply(0.5), new Vector3D(7000, 0, 0));
        final Orbit orbit2 = new CartesianOrbit(orbitPV2, frame, date, Constants.GRS80_EARTH_MU);
        final SpacecraftState state2 = new SpacecraftState(orbit2);
        Assert.assertFalse(MathLib.abs(detector.g(state2)) < 1E-10);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_SOLAR_TIME_ANGLE_DETECTOR}
     * 
     * @testedMethod {@link SolarTimeAngleDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * 
     * @description tests {@link SolarTimeAngleDetector#eventOccurred(SpacecraftState, boolean, boolean)}
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
        final SolarTimeAngleDetector detector = new SolarTimeAngleDetector(2.5634);
        // a g decreasing event is detected: do not continue
        Action rez = detector.eventOccurred(state, false, true);
        Assert.assertEquals(Action.STOP, rez);
        // a g increasing event is detected: do not continue
        rez = detector.eventOccurred(state, true, true);
        Assert.assertEquals(Action.STOP, rez);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_SOLAR_TIME_ANGLE_DETECTOR}
     * 
     * @testedMethod {@link SolarTimeAngleDetector#g(SpacecraftState)}
     * @testedMethod {@link SolarTimeAngleDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * 
     * @description propagates an orbit adding multiple SolarTimeDetector and using an events logger:
     *              an event is detected when the angle between the satellite and earth projections on the orbital
     *              plane is equal to one of the predetermined values.
     * 
     * @input constructor parameters, a propagator and an event logger
     * 
     * @output the solar time events logged during the propagation
     * 
     * @testPassCriteria check that when an event is logged during the propagation, the angle between
     *                   the two vector projections is equal to the predetermined value.
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @Test
    public void testOrbit() throws PatriusException {
        final double period = orbit.getKeplerianPeriod();
        final Propagator propagator = new KeplerianPropagator(orbit);
        // detects the solar angle = 0:
        final SolarTimeAngleDetector detector0 = new SolarTimeAngleDetector(FastMath.PI * 0){

            private static final long serialVersionUID = 7773544228003299956L;

            @Override
            public Action eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward) {
                return Action.CONTINUE;
            }
        };
        // detects the solar angle = 0.4 * PI:
        final SolarTimeAngleDetector detector04 = new SolarTimeAngleDetector(FastMath.PI * 0.4){

            private static final long serialVersionUID = 4129999659502008758L;

            @Override
            public Action eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward) {
                return Action.CONTINUE;
            }
        };
        // detects the solar angle = PI:
        final SolarTimeAngleDetector detectorPI = new SolarTimeAngleDetector(-FastMath.PI){

            private static final long serialVersionUID = 7858561671574682046L;

            @Override
            public Action eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward) {
                return Action.CONTINUE;
            }
        };
        // detects the solar angle = - PI * 0.2:
        final SolarTimeAngleDetector detector18 = new SolarTimeAngleDetector(-FastMath.PI * 0.2){

            private static final long serialVersionUID = -1255736652138424166L;

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
        // re-implement the g function for every detected event:
        Assert.assertEquals(12, logger.getLoggedEvents().size());
        for (final LoggedEvent event : logger.getLoggedEvents()) {
            final SpacecraftState sstate = event.getState();
            final Vector3D sat = sstate.getPVCoordinates(sstate.getFrame()).getPosition();
            final Vector3D sun = CelestialBodyFactory.getSun().getPVCoordinates(sstate.getDate(),
                sstate.getFrame()).getPosition();
            final Vector3D n = sstate.getPVCoordinates().getMomentum().normalize();
            final Vector3D sunPj = sun.subtract(n.scalarMultiply(Vector3D.dotProduct(sun, n)));
            final double angle = Vector3D.angle(sunPj, sat);
            final double time = ((SolarTimeAngleDetector) event.getEventDetector()).getTime();
            if (time == 0) {
                Assert.assertEquals(0, angle, ANGEPS);
            } else if (time == 0.4 * FastMath.PI) {
                Assert.assertEquals(0.4 * FastMath.PI, angle, ANGEPS);
            } else if (time == FastMath.PI) {
                Assert.assertEquals(FastMath.PI, angle, ANGEPS);
            } else if (time == -0.2 * FastMath.PI) {
                Assert.assertEquals(1.8 * FastMath.PI, 2 * FastMath.PI - angle, ANGEPS);
            }
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_SOLAR_TIME_ANGLE_DETECTOR}
     * 
     * @testedMethod {@link SolarTimeAngleDetector#g(SpacecraftState)}
     * 
     * @description tests g(SpacecraftState) propagating an elliptic
     *              orbit during one period with a numerical propagator.
     * 
     * @input constructor parameters and a numerical propagator
     * 
     * @output the spacecraft state when the event is detected
     * 
     * @testPassCriteria the solar time event is properly detected.
     * 
     * @referenceVersion 4.2
     * 
     * @nonRegressionVersion 4.2
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @Test
    public void testNumericalPropagator() throws PatriusException {
        final double period = orbit.getKeplerianPeriod();
        final double[] absTOL = { 1e-5, 1e-5, 1e-5, 1e-8, 1e-8, 1e-8 };
        final double[] relTOL = { 1e-10, 1e-10, 1e-10, 1e-10, 1e-10, 1e-10 };
        final AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(0.1, 60., absTOL, relTOL);
        final NumericalPropagator propagator = new NumericalPropagator(integrator);
        propagator.resetInitialState(new SpacecraftState(orbit));
        propagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(propagator
            .getInitialState().getMu())));
        // detects the position angle = 3 * PI / 2:
        final SolarTimeAngleDetector detector = new SolarTimeAngleDetector(MathLib.toRadians(133.42));
        propagator.addEventDetector(detector);
        final SpacecraftState curState = propagator.propagate(iniDate.shiftedBy(period));
        // re-implement the g-function:
        final Vector3D sat = curState.getPVCoordinates(curState.getFrame()).getPosition();
        final Vector3D sunP = CelestialBodyFactory.getSun().getPVCoordinates(curState.getDate(), curState.getFrame())
            .getPosition();
        final Vector3D n = curState.getPVCoordinates().getMomentum().normalize();
        final Vector3D sunPj = sunP.subtract(n.scalarMultiply(Vector3D.dotProduct(sunP, n)));
        // Computing the angle between the satellite and the sun projection over the orbital plane:
        final double angle = Vector3D.angle(sunPj, sat);
        Assert.assertEquals(MathLib.toRadians(133.42), angle, 1E-11);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_SOLAR_TIME_ANGLE_DETECTOR}
     * 
     * @testedMethod {@link SolarTimeAngleDetector#LocalTimeAngleDetector(double) }
     * 
     * @description Check exception properly thrown if provided angle is out of range [-Pi; Pi[
     * 
     * @input out of range solar time
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
    public void testOutOfRangeSolarTime() {
        try {
            new SolarTimeAngleDetector(-FastMath.PI - 1E-14);
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }
        try {
            new SolarTimeAngleDetector(-FastMath.PI);
            Assert.assertTrue(true);
        } catch (final PatriusException e) {
            Assert.fail();
        }
        try {
            new SolarTimeAngleDetector(FastMath.PI);
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }
        try {
            new SolarTimeAngleDetector(FastMath.PI - 1E-14);
            Assert.assertTrue(true);
        } catch (final PatriusException e1) {
            Assert.fail();
        }
    }
    
    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_SOLAR_TIME_ANGLE_DETECTOR}
     * 
     * @testedMethod {@link SolarTimeAngleDetector#g(fr.cnes.sirius.patrius.propagation.SpacecraftState) }
     * 
     * @description When the central body is not the reference frame:
     *              With Mars as central body, check that g function is exactly 0 with sun aligned with spacecraft and a
     *              moon of Mars for an angle of 0.
     *              and check that g function is not close to 0 with sun aligned with spacecraft and Mars but not with a
     *              moon of Mars.
     * 
     * @input sun model
     * 
     * @output g value
     * 
     * @testPassCriteria 0 when moon, sun and spacecraft are aligned, not 0 otherwise.
     * 
     * @throws PatriusException
     *         if an error occurs in the detector or the CelestialBody creation
     * 
     * @referenceVersion 4.11
     * 
     * @nonRegressionVersion 4.11
     */
    @Test
    public void testSolarTimeAngleDetectorMarsMoon() throws PatriusException {
        // Patrius data set
        Utils.setDataRoot("regular-dataPBASE");
        // Get sun
        final CelestialPoint sun = CelestialBodyFactory.getSun();

        // Initialize a date
        final AbsoluteDate date = new AbsoluteDate(2022, 12, 12, 0, 0, 0);

        // Choose Mars frame
        final Frame marsFrame = CelestialBodyFactory.getMars().getICRF();

        // Get the PV of the sun at the date
        final PVCoordinates sunPV = sun.getPVCoordinates(date, marsFrame);

        // Create Mars fake moon orbit
        final PVCoordinates moonPV =
            new PVCoordinates(sunPV.getPosition().scalarMultiply(0.08), new Vector3D(7000, 0, 0));
        final Orbit moonOrbit = new CartesianOrbit(moonPV, marsFrame, date, Constants.JPL_SSD_MARS_SYSTEM_GM);

        // Creation of fakeMoon celestialBody
        final UserCelestialBody moonCelestialBody = new UserCelestialBody("", moonOrbit, 0,
            IAUPoleFactory.getIAUPole(null),
            FramesFactory.getEME2000(), null);
        // Creation of the SolarTimeAngleDetector with the Mars moon as reference
        final SolarTimeAngleDetector detectorCelestialBody = new SolarTimeAngleDetector(-FastMath.PI, 600, 1.e-6,
            moonCelestialBody.getRotatingFrame(IAUPoleModelType.TRUE));

        // Create a spacecraft orbit
        final PVCoordinates spacecraftPV =
            new PVCoordinates(sunPV.getPosition().scalarMultiply(0.01), new Vector3D(7000, 0, 0));
        final Orbit spacecraftOrbit = new CartesianOrbit(spacecraftPV, marsFrame, date,
            Constants.JPL_SSD_MARS_SYSTEM_GM);
        final SpacecraftState state = new SpacecraftState(spacecraftOrbit);

        Assert.assertEquals(0, detectorCelestialBody.g(state), 1E-15);

        // Create a not aligned moon
        final PVCoordinates sunPVShifted = sun.getPVCoordinates(date.shiftedBy(60), marsFrame);
        final PVCoordinates moonPVShifted =
            new PVCoordinates(sunPVShifted.getPosition().scalarMultiply(0.08), new Vector3D(7000, 0, 0));
        final Orbit moonOrbitShifted = new CartesianOrbit(moonPVShifted, marsFrame, date,
            Constants.JPL_SSD_MARS_SYSTEM_GM);
        final UserCelestialBody moonShiftedCelestialBody = new UserCelestialBody("", moonOrbitShifted, 0,
            IAUPoleFactory.getIAUPole(null),
            FramesFactory.getEME2000(), null);

        // Creation of the SolarTimeAngleDetector with the shifted Mars moon as reference
        final SolarTimeAngleDetector detectorShiftedCelestialBody = new SolarTimeAngleDetector(-FastMath.PI, 600,
            1.e-6, moonShiftedCelestialBody.getRotatingFrame(IAUPoleModelType.TRUE));
        Assert.assertFalse(MathLib.abs(detectorShiftedCelestialBody.g(state)) < 1E-15);
    }
}
