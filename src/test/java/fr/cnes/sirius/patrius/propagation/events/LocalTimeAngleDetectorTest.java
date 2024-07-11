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
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
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
import fr.cnes.sirius.patrius.bodies.MeeusSun;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.nonstiff.AdaptiveStepsizeIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.CircularOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.propagation.events.EventDetector.Action;
import fr.cnes.sirius.patrius.propagation.events.EventsLogger.LoggedEvent;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusFixedStepHandler;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

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

        orbitRetro = new CircularOrbit(8000000, 0, 0, MathLib.toRadians(98.0), 0, FastMath.PI / 2,
            PositionAngle.TRUE, FramesFactory.getGCRF(), iniDate, mu);

        orbitPro = new CircularOrbit(8000000, 0, 0, MathLib.toRadians(60.0), 0, FastMath.PI / 2,
            PositionAngle.TRUE, FramesFactory.getGCRF(), iniDate, mu);

        state = new SpacecraftState(orbitRetro);
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
    public final void testLocalTimeAngleDetectorCtor1() throws PatriusException {
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
    public final void testLocalTimeAngleDetectorCtor2() throws PatriusException {
        final LocalTimeAngleDetector detector = new LocalTimeAngleDetector(-FastMath.PI, 500, 0.001);
        // The constructor did not crash...
        Assert.assertNotNull(detector);
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
    public final void testEventOccurred() throws PatriusException {
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
    public final void testRetrogradeOrbit() throws PatriusException {

        final double period = orbitRetro.getKeplerianPeriod();

        final Propagator propagator = new KeplerianPropagator(orbitRetro);

        // Step handler to track longitude evolution during propagation
        final MyStepHandler angleTracking = new MyStepHandler(iniDate, FramesFactory.getTIRF());
        propagator.setMasterMode(10, angleTracking);

        // detects the position angle = 0:
        final LocalTimeAngleDetector detector0 = new LocalTimeAngleDetector(FastMath.PI * 0){

            private static final long serialVersionUID = 4529781719692037999L;

            @Override
            public
                    Action
                    eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                           throws PatriusException {
                return Action.CONTINUE;
            }
        };
        // detects the position angle = 0.4 * PI:
        final LocalTimeAngleDetector detector04 = new LocalTimeAngleDetector(FastMath.PI * 0.4){

            private static final long serialVersionUID = 4529781719692037999L;

            @Override
            public
                    Action
                    eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                           throws PatriusException {
                return Action.CONTINUE;
            }
        };
        // detects the position angle = PI:
        final LocalTimeAngleDetector detectorPI = new LocalTimeAngleDetector(-FastMath.PI){

            private static final long serialVersionUID = 4529781719692037999L;

            @Override
            public
                    Action
                    eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                           throws PatriusException {
                return Action.CONTINUE;
            }
        };
        // detects the position angle = - PI * 0.2:
        final LocalTimeAngleDetector detector18 = new LocalTimeAngleDetector(-FastMath.PI * 0.2){

            private static final long serialVersionUID = 4529781719692037999L;

            @Override
            public
                    Action
                    eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                           throws PatriusException {
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
            final Vector3D sun = CelestialBodyFactory.getSun().getPVCoordinates(sstate.getDate(),
                FramesFactory.getTIRF()).getPosition();
            final Vector3D satellite = sstate.getPVCoordinates(FramesFactory.getTIRF()).getPosition();
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
    public final void testProgradeOrbit() throws PatriusException {
        final double period = orbitPro.getKeplerianPeriod();
        final Propagator propagator = new KeplerianPropagator(orbitPro);

        // Step handler to track longitude evolution during propagation
        final MyStepHandler angleTracking = new MyStepHandler(iniDate, FramesFactory.getTIRF());
        propagator.setMasterMode(10, angleTracking);

        // detects the position angle = 0:
        final LocalTimeAngleDetector detector0 = new LocalTimeAngleDetector(FastMath.PI * 0){

            private static final long serialVersionUID = 4529781719692037999L;

            @Override
            public
                    Action
                    eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                           throws PatriusException {
                return Action.CONTINUE;
            }
        };
        // detects the position angle = 0.4 * PI:
        final LocalTimeAngleDetector detector04 = new LocalTimeAngleDetector(FastMath.PI * 0.4){

            private static final long serialVersionUID = 4529781719692037999L;

            @Override
            public
                    Action
                    eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                           throws PatriusException {
                return Action.CONTINUE;
            }
        };
        // detects the position angle = PI:
        final LocalTimeAngleDetector detectorPI = new LocalTimeAngleDetector(-FastMath.PI){

            private static final long serialVersionUID = 4529781719692037999L;

            @Override
            public
                    Action
                    eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                           throws PatriusException {
                return Action.CONTINUE;
            }
        };
        // detects the position angle = - PI * 0.2:
        final LocalTimeAngleDetector detector18 = new LocalTimeAngleDetector(-FastMath.PI * 0.2){

            private static final long serialVersionUID = 4529781719692037999L;

            @Override
            public
                    Action
                    eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                           throws PatriusException {
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
            final Vector3D sun = CelestialBodyFactory.getSun().getPVCoordinates(sstate.getDate(),
                FramesFactory.getTIRF()).getPosition();
            final Vector3D satellite = sstate.getPVCoordinates(FramesFactory.getTIRF()).getPosition();
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
    public final void testNumericalPropagator() throws PatriusException {
        final double period = orbitRetro.getKeplerianPeriod();
        final double[] absTOL = { 1e-5, 1e-5, 1e-5, 1e-8, 1e-8, 1e-8 };
        final double[] relTOL = { 1e-10, 1e-10, 1e-10, 1e-10, 1e-10, 1e-10 };
        final AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(0.1, 60., absTOL, relTOL);
        final Propagator propagator = new NumericalPropagator(integrator);
        propagator.resetInitialState(new SpacecraftState(orbitRetro));
        // detects the position angle = 3 * PI / 2:
        final LocalTimeAngleDetector detector = new LocalTimeAngleDetector(MathLib.toRadians(-1.568));
        propagator.addEventDetector(detector);
        final SpacecraftState curState = propagator.propagate(iniDate.shiftedBy(period));
        // re-implement the g-function:
        final Vector3D sun = CelestialBodyFactory.getSun().getPVCoordinates(curState.getDate(),
            FramesFactory.getTIRF()).getPosition();
        final Vector3D satellite = curState.getPVCoordinates(FramesFactory.getTIRF()).getPosition();
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
    public final void testOutOfRangeLocalTime() {
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
     * @testedMethod {@link LocalTimeAngleDetector#LocalTimeAngleDetector(double, double, double, Action, boolean, CelestialBody)}
     * 
     * @description checks user Sun model is properly taken into account
     * 
     * @input constructor parameters : Sun model
     * 
     * @output g value
     * 
     * @testPassCriteria g value is the same when using default Sun and CelestialBodyFactory.getSun() and different when using MeeusSun
     * 
     * @referenceVersion 4.6
     * 
     * @nonRegressionVersion 4.6
     */
    @Test
    public final void sunConstructorTest() throws PatriusException {
        // Same detectors
        final EventDetector detector2 = new LocalTimeAngleDetector(-0.2, 0, 0, Action.CONTINUE, false);
        final EventDetector detector3 = new LocalTimeAngleDetector(-0.2, 0, 0, Action.CONTINUE, false, CelestialBodyFactory.getSun());
        // Different detector
        final EventDetector detector1 = new LocalTimeAngleDetector(-0.2, 0, 0, Action.CONTINUE, false, new MeeusSun());
        // Checks
        Assert.assertFalse(detector1.g(state) == detector2.g(state));
        Assert.assertTrue(detector2.g(state) == detector3.g(state));
    }

    /**
     * 
     * Implementation of a step handler to track the evolution
     * of the physical value to test
     * 
     */
    class MyStepHandler implements PatriusFixedStepHandler {

        /** serial UID */
        private static final long serialVersionUID = 5643736397373186L;

        /** results */
        public ArrayList<double[]> results;

        /** initial date */
        private final AbsoluteDate fromDate;

        /** initial date */
        private final Frame bodyFrame;

        /** The Sun. */
        private final CelestialBody sun;

        /**
         * simple constructor
         * 
         * @param date
         *        initialDate of propagation
         * @throws PatriusException
         */
        public MyStepHandler(final AbsoluteDate date, final Frame frame) throws PatriusException {
            this.results = new ArrayList<double[]>();
            this.fromDate = date;
            this.bodyFrame = frame;
            this.sun = CelestialBodyFactory.getSun();
        }

        @Override
        public void init(final SpacecraftState s0, final AbsoluteDate t) {

        }

        @Override
        public void handleStep(final SpacecraftState s, final boolean isLast) throws PropagationException {

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
                final double[] currentResult =
                {
                    s.getDate().durationFrom(this.fromDate),
                    angle
                };

                this.results.add(currentResult);
            } catch (final PatriusException e) {
                e.printStackTrace();
            }
        }
    }
}
