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
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 *                             (added forward parameter to eventOccurred signature)
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:226:12/09/2014: problem with event detections.
 * VERSION::DM:396:16/03/2015:new architecture for orbital parameters
 * VERSION::DM:394:20/02/2015: Added possibility to define the action performed when event occurs
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 *
 */
package fr.cnes.sirius.patrius.propagation.events;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.ode.nonstiff.AdaptiveStepsizeIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.CircularOrbit;
import fr.cnes.sirius.patrius.orbits.EquinoctialOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.propagation.events.EventDetector.Action;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Unit tests for {@link AnomalyDetector}.
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id: AnomalyDetectorTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.1
 * 
 */
public class AnomalyDetectorTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Validate the anomaly detector
         * 
         * @featureDescription Validate the anomaly detector
         * 
         * @coveredRequirements DV-EVT_120, DV-EVT_50
         */
        VALIDATE_ANOMALY_DETECTOR
    }

    /**
     * An elliptic orbit used for the tests.
     */
    private static KeplerianOrbit orbit;

    /**
     * A true circular orbit used for the tests.
     */
    private static CircularOrbit trueCircularOrbit;

    /**
     * A false circular orbit used for the tests.
     */
    private static CircularOrbit falseCircularOrbit;

    /**
     * An equinoctial (circular) orbit used for the tests.
     */
    private static EquinoctialOrbit equinoctialCircularOrbit;

    /**
     * An equinoctial (non circular) orbit used for the tests.
     */
    private static EquinoctialOrbit equinoctialNonCircularOrbit;

    /**
     * Initial propagator date.
     */
    private static AbsoluteDate iniDate;

    /**
     * Setup for all unit tests in the class.
     * Provides two {@link Orbit}.
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @BeforeClass
    public static void setUpBeforeClass() throws PatriusException {
        // Orekit initialization
        Utils.setDataRoot("regular-dataCNES-2003");

        iniDate = new AbsoluteDate("2011-11-09T12:00:00Z", TimeScalesFactory.getTT());
        final double mu = CelestialBodyFactory.getEarth().getGM();

        orbit = new KeplerianOrbit(9000000, 0.02, 0.8, 0.2, 0.05, 0,
            PositionAngle.TRUE, FramesFactory.getGCRF(), iniDate, mu);
        trueCircularOrbit = new CircularOrbit(8000000, 0, 0, MathLib.toRadians(18), 0, 0,
            PositionAngle.TRUE, FramesFactory.getGCRF(), iniDate, mu);
        falseCircularOrbit = new CircularOrbit(8000000, 0.02, 0.1, MathLib.toRadians(32), 0, MathLib.toRadians(5),
            PositionAngle.TRUE, FramesFactory.getGCRF(), iniDate, mu);
        equinoctialCircularOrbit = new EquinoctialOrbit(8000000, 0, 0, MathLib.toRadians(9.6), MathLib.toRadians(67),
            MathLib.toRadians(1),
            PositionAngle.TRUE, FramesFactory.getGCRF(), iniDate, mu);
        equinoctialNonCircularOrbit = new EquinoctialOrbit(8000000, 0.2, 0.05, MathLib.toRadians(9.6),
            MathLib.toRadians(67), MathLib.toRadians(1),
            PositionAngle.TRUE, FramesFactory.getGCRF(), iniDate, mu);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_ANOMALY_DETECTOR}
     * 
     * @testedMethod {@link AnomalyDetector#AnomalyDetector(PositionAngle, double)}
     * 
     * @description simple constructor test
     * 
     * @input constructor parameters : anomaly type, anomaly
     * 
     * @output an AnomalyDetector
     * 
     * @testPassCriteria the AnomalyDetector is successfully created
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public final void testAnomalyDetectorCtor1() {
        final AnomalyDetector detector = new AnomalyDetector(PositionAngle.TRUE, 10);
        // The constructor did not crash...
        Assert.assertNotNull(detector);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_ANOMALY_DETECTOR}
     * 
     * @testedMethod {@link AnomalyDetector#AnomalyDetector(PositionAngle, double, double, double)}
     * 
     * @description simple constructor test
     * 
     * @input constructor parameters : anomaly type, anomaly
     * 
     * @output an AnomalyDetector
     * 
     * @testPassCriteria the AnomalyDetector is successfully created
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public final void testAnomalyDetectorCtor2() {
        final AnomalyDetector detector = new AnomalyDetector(PositionAngle.TRUE, 10, 2, 0.01);
        // The constructor did not crash...
        Assert.assertNotNull(detector);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_ANOMALY_DETECTOR}
     * 
     * @testedMethod {@link AnomalyDetector#AnomalyDetector(PositionAngle, double, double, double, Action)}
     * 
     * @description simple constructor test
     * 
     * @input constructor parameters : anomaly type, anomaly
     * 
     * @output an AnomalyDetector
     * 
     * @testPassCriteria the AnomalyDetector is successfully created
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public final void testAnomalyDetectorCtor3() {
        final AnomalyDetector detector = new AnomalyDetector(PositionAngle.TRUE, 10, 2, 0.01, Action.STOP);
        final AnomalyDetector detector2 = (AnomalyDetector) detector.copy();
        // Test getter
        Assert.assertEquals(10.0, detector2.getAnomaly());
        Assert.assertEquals(PositionAngle.TRUE, detector2.getAnomalyType());
        Assert.assertEquals(Action.STOP, detector2.getAction());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_ANOMALY_DETECTOR}
     * 
     * @testedMethod {@link AnomalyDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * 
     * @description tests eventOccurred(SpacecraftState, boolean)
     * 
     * @input constructor and eventOccured parameters
     * 
     * @output eventOccured outputs
     * 
     * @testPassCriteria eventOccured returns the expected values (stop propagation)
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
        final AnomalyDetector detector = new AnomalyDetector(PositionAngle.MEAN, 2.5);
        final SpacecraftState state = new SpacecraftState(orbit);
        // eventOccurred() is called:
        final Action rez = detector.eventOccurred(state, false, true);
        Assert.assertEquals(Action.STOP, rez);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_ANOMALY_DETECTOR}
     * 
     * @testedMethod {@link AnomalyDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * @testedMethod {@link AnomalyDetector#g(SpacecraftState)}
     * 
     * @description tests the detector detects an event when the orbit is a
     *              circular orbit
     * 
     * @input a circular orbit and a propagator
     * 
     * @output propagation outputs
     * 
     * @testPassCriteria eventOccured is never called
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @Test
    public final void testTrueCircularOrbit() throws PatriusException {
        final double period = trueCircularOrbit.getKeplerianPeriod();
        // circular orbit:
        final Propagator propagator = new KeplerianPropagator(trueCircularOrbit);
        // anomaly detector:
        final AnomalyDetector detector = new AnomalyDetector(PositionAngle.TRUE, MathLib.toRadians(20));
        propagator.addEventDetector(detector);
        final SpacecraftState curState = propagator.propagate(iniDate.shiftedBy(2 * period));
        // An event should be detected: curState date should not be equal to the propagation end date:
        final boolean equals = MathLib.abs(2 * period - curState.getDate().durationFrom(iniDate)) < Utils.epsilonTest;
        Assert.assertFalse(equals);
        // Assert.assertEquals(2 * period, curState.getDate().durationFrom(iniDate), Utils.epsilonTest);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_ANOMALY_DETECTOR}
     * 
     * @testedMethod {@link AnomalyDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * @testedMethod {@link AnomalyDetector#g(SpacecraftState)}
     * 
     * @description tests the detector the detector detects an anomaly event if the orbit is not a circular
     *              orbit (even if it has been created using the CircularOrbit class)
     * 
     * @input a circular orbit (Equinoctial orbit) and a propagator
     * 
     * @output propagation outputs
     * 
     * @testPassCriteria g is zero when the anomaly is equal to a predetermined value
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @Test
    public final void testFalseCircularOrbit() throws PatriusException {
        final double period = falseCircularOrbit.getKeplerianPeriod();
        // circular orbit:
        final Propagator propagator = new KeplerianPropagator(falseCircularOrbit);
        // anomaly detector:
        final AnomalyDetector detector = new AnomalyDetector(PositionAngle.TRUE, MathLib.toRadians(15.63), 100, 1E-10);
        propagator.addEventDetector(detector);
        final SpacecraftState curState = propagator.propagate(iniDate.shiftedBy(2 * period));
        // as the orbit is not really a circular orbit, an event should be detected:
        final KeplerianOrbit newOrbit = new KeplerianOrbit(curState.getOrbit());
        Assert.assertEquals(MathLib.toRadians(15.63), newOrbit.getTrueAnomaly(), Utils.epsilonTest);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_ANOMALY_DETECTOR}
     * 
     * @testedMethod {@link AnomalyDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * @testedMethod {@link AnomalyDetector#g(SpacecraftState)}
     * 
     * @description tests the detector detects an event when the orbit is a
     *              circular orbit
     * 
     * @input a circular orbit (Equinoctial orbit) and a propagator
     * 
     * @output propagation outputs
     * 
     * @testPassCriteria eventOccured is never called
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @Test
    public final void testEquinoctialCircularOrbit() throws PatriusException {
        final double period = equinoctialCircularOrbit.getKeplerianPeriod();
        // circular orbit:
        final Propagator propagator = new KeplerianPropagator(equinoctialCircularOrbit);
        // anomaly detector:
        final AnomalyDetector detector = new AnomalyDetector(PositionAngle.TRUE, MathLib.toRadians(196.3));
        propagator.addEventDetector(detector);
        final SpacecraftState curState = propagator.propagate(iniDate.shiftedBy(2 * period));
        // An event should be detected: curState date should not be equal to the propagation end date:
        final boolean equals = MathLib.abs(2 * period - curState.getDate().durationFrom(iniDate)) < Utils.epsilonTest;
        Assert.assertFalse(equals);
        // Assert.assertEquals(2 * period, curState.getDate().durationFrom(iniDate), Utils.epsilonTest);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_ANOMALY_DETECTOR}
     * 
     * @testedMethod {@link AnomalyDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * @testedMethod {@link AnomalyDetector#g(SpacecraftState)}
     * 
     * @description tests the detector detects an anomaly event if the orbit is not a circular
     *              orbit (even if it has been created using the EquinoctialOrbit class)
     * 
     * @input a circular orbit (Equinoctial orbit) and a propagator
     * 
     * @output propagation outputs
     * 
     * @testPassCriteria eventOccured is never called
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @Test
    public final void testEquinoctialNonCircularOrbit() throws PatriusException {
        final double period = equinoctialNonCircularOrbit.getKeplerianPeriod();
        // non-circular orbit:
        final Propagator propagator = new KeplerianPropagator(equinoctialNonCircularOrbit);
        // anomaly detector:
        final AnomalyDetector detector = new AnomalyDetector(PositionAngle.ECCENTRIC, MathLib.toRadians(115.7));
        propagator.addEventDetector(detector);
        final SpacecraftState curState = propagator.propagate(iniDate.shiftedBy(2 * period));
        // as the orbit is not a circular orbit, an event should be detected:
        final KeplerianOrbit newOrbit = new KeplerianOrbit(curState.getOrbit());
        Assert.assertEquals(MathLib.toRadians(115.7), newOrbit.getEccentricAnomaly(), Utils.epsilonTest);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_ANOMALY_DETECTOR}
     * 
     * @testedMethod {@link AnomalyDetector#g(SpacecraftState)}
     * 
     * @description tests g(SpacecraftState) propagating an elliptic
     *              orbit during one period.
     * 
     * @input constructor and g parameters
     * 
     * @output g output
     * 
     * @testPassCriteria g is zero when the anomaly is equal to 3*PI/2
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @Test
    public final void testEllipticOrbit1() throws PatriusException {
        final double period = orbit.getKeplerianPeriod();
        final Propagator propagator = new KeplerianPropagator(orbit);

        // detects the position angle = 3 * PI / 2:
        final AnomalyDetector detector = new AnomalyDetector(PositionAngle.TRUE, 3 * FastMath.PI / 2);
        propagator.addEventDetector(detector);
        final SpacecraftState curState = propagator.propagate(iniDate.shiftedBy(period));
        Assert.assertEquals(3 * FastMath.PI / 2, ((KeplerianOrbit) curState.getOrbit()).getTrueAnomaly(),
            Utils.epsilonTest);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_ANOMALY_DETECTOR}
     * 
     * @testedMethod {@link AnomalyDetector#g(SpacecraftState)}
     * 
     * @description tests g(SpacecraftState) propagating an elliptic
     *              orbit during one period and detecting when the angle position is equal to 3 / 2 PI.
     * 
     * @input constructor and g parameters
     * 
     * @output g output
     * 
     * @testPassCriteria g is zero when the anomaly is equal to 2*PI
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @Test
    public final void testEllipticOrbit2() throws PatriusException {
        final double period = orbit.getKeplerianPeriod();
        final Propagator propagator = new KeplerianPropagator(orbit);
        // detects the position angle = PI * 2:
        final AnomalyDetector detectorMin = new AnomalyDetector(PositionAngle.TRUE, FastMath.PI * 2);
        propagator.addEventDetector(detectorMin);
        final SpacecraftState curState = propagator.propagate(iniDate.shiftedBy(period));
        Assert
            .assertEquals(FastMath.PI * 2, ((KeplerianOrbit) curState.getOrbit()).getTrueAnomaly(), Utils.epsilonTest);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_ANOMALY_DETECTOR}
     * 
     * @testedMethod {@link AnomalyDetector#g(SpacecraftState)}
     * 
     * @description tests g(SpacecraftState) propagating an
     *              orbit during one period:
     * 
     * @input constructor and g parameters
     * 
     * @output g output
     * 
     * @testPassCriteria g is zero when the true anomaly is equal to the given value
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @Test
    public final void testOrbitTrueAnomaly() throws PatriusException {
        final double period = orbit.getKeplerianPeriod();
        final Propagator propagator = new KeplerianPropagator(orbit);

        // detects the position angle = 115°:
        final AnomalyDetector detector = new AnomalyDetector(PositionAngle.TRUE, MathLib.toRadians(115));
        propagator.addEventDetector(detector);
        final SpacecraftState curState = propagator.propagate(iniDate.shiftedBy(period));
        Assert.assertEquals(MathLib.toRadians(115), ((KeplerianOrbit) curState.getOrbit()).getTrueAnomaly(),
            Utils.epsilonTest);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_ANOMALY_DETECTOR}
     * 
     * @testedMethod {@link AnomalyDetector#g(SpacecraftState)}
     * 
     * @description tests g(SpacecraftState) propagating an
     *              orbit during one period:
     * 
     * @input constructor and g parameters
     * 
     * @output g output
     * 
     * @testPassCriteria g is zero when the mean anomaly is equal to the given value
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @Test
    public final void testOrbitMeanAnomaly() throws PatriusException {
        final double period = orbit.getKeplerianPeriod();
        final Propagator propagator = new KeplerianPropagator(orbit);

        // detects the position angle = PI * 1.5:
        final AnomalyDetector detector = new AnomalyDetector(PositionAngle.MEAN, FastMath.PI * 1.5);
        propagator.addEventDetector(detector);
        final SpacecraftState curState = propagator.propagate(iniDate.shiftedBy(period));
        Assert.assertEquals(FastMath.PI * 1.5, ((KeplerianOrbit) curState.getOrbit()).getMeanAnomaly(),
            Utils.epsilonTest);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_ANOMALY_DETECTOR}
     * 
     * @testedMethod {@link AnomalyDetector#g(SpacecraftState)}
     * 
     * @description tests g(SpacecraftState) propagating an orbit
     *              during one period:
     * 
     * @input constructor and g parameters
     * 
     * @output g output
     * 
     * @testPassCriteria g is zero when the eccentric anomaly is equal to the given value
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @Test
    public final void testOrbitEccentricAnomaly() throws PatriusException {
        final double period = orbit.getKeplerianPeriod();
        final Propagator propagator = new KeplerianPropagator(orbit);

        // detects the position angle = PI / 7:
        final AnomalyDetector detector = new AnomalyDetector(PositionAngle.ECCENTRIC, FastMath.PI / 7);
        propagator.addEventDetector(detector);
        final SpacecraftState curState = propagator.propagate(iniDate.shiftedBy(period));
        Assert.assertEquals(FastMath.PI / 7, ((KeplerianOrbit) curState.getOrbit()).getEccentricAnomaly(),
            Utils.epsilonTest);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_ANOMALY_DETECTOR}
     * 
     * @testedMethod {@link AnomalyDetector#g(SpacecraftState)}
     * 
     * @description tests g(SpacecraftState) propagating an elliptic
     *              orbit during one period with a numerical propagator.
     * 
     * @input constructor parameters and a numerical propagator
     * 
     * @output the spacecraft state when the event is detected
     * 
     * @testPassCriteria the anomaly event is properly detected.
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
        final double period = orbit.getKeplerianPeriod();
        final double[] absTOL = { 1e-5, 1e-5, 1e-5, 1e-8, 1e-8, 1e-8 };
        final double[] relTOL = { 1e-10, 1e-10, 1e-10, 1e-10, 1e-10, 1e-10 };
        final AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(0.1, 60., absTOL, relTOL);
        final Propagator propagator = new NumericalPropagator(integrator);
        propagator.resetInitialState(new SpacecraftState(orbit));
        // detects the position angle = 3 * PI / 2:
        final AnomalyDetector detector = new AnomalyDetector(PositionAngle.TRUE, MathLib.toRadians(-95.625));
        propagator.addEventDetector(detector);
        final SpacecraftState curState = propagator.propagate(iniDate.shiftedBy(period));
        final KeplerianOrbit finalOrbit = new KeplerianOrbit(curState.getOrbit());
        Assert.assertEquals(MathLib.toRadians(-95.625) + 2. * FastMath.PI, finalOrbit.getTrueAnomaly(),
            Utils.epsilonTest);
    }
}
