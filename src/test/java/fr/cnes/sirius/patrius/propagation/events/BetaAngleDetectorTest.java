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
 * @history created 27/02/12
 *
 *
 * HISTORY
 * VERSION:4.13.1:FA:FA-177:17/01/2024:[PATRIUS] Reliquat OPENFD
 * VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
 * VERSION:4.13:DM:DM-3:08/12/2023:[PATRIUS] Distinction entre corps celestes et barycentres
 * VERSION:4.13:FA:FA-118:08/12/2023:[PATRIUS] Calcul d'union de PyramidalField invalide
 * VERSION:4.13:DM:DM-108:08/12/2023:[PATRIUS] Modele d'obliquite et de precession de la Terre
 * VERSION:4.13:DM:DM-37:08/12/2023:[PATRIUS] Date d'evenement et propagation du signal
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3166:10/05/2022:[PATRIUS] Definir l'ICRF comme repere racine 
 * VERSION:4.9:DM:DM-3169:10/05/2022:[PATRIUS] Precision de l'hypothese de propagation instantanee de la lumiere
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.6:FA:FA-2502:27/01/2021:[PATRIUS] Choix des ephemerides solaires dans certains detecteurs - manque de cas de test 
 * VERSION:4.5:DM:DM-2414:27/05/2020:Choix des ephemeris solaires dans certains detecteurs 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::DM:394:20/02/2015: Added possibility to define the action performed when event occurs
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
import fr.cnes.sirius.patrius.bodies.MeeusSun;
import fr.cnes.sirius.patrius.events.EventDetector;
import fr.cnes.sirius.patrius.events.EventDetector.Action;
import fr.cnes.sirius.patrius.events.detectors.AbstractSignalPropagationDetector.DatationChoice;
import fr.cnes.sirius.patrius.events.detectors.AbstractSignalPropagationDetector.PropagationDelayType;
import fr.cnes.sirius.patrius.events.detectors.BetaAngleDetector;
import fr.cnes.sirius.patrius.events.utils.SignalPropagationWrapperDetector;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Unit tests for {@link BetaAngleDetector}.
 * 
 * @author cardosop
 * 
 * @version $Id: BetaAngleDetectorTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.1
 * 
 */
public class BetaAngleDetectorTest {

    /**
     * EXISTS_BUT_SHOULD_NOT string.
     */
    private static final String EXISTS_BUT_SHOULD_NOT = " exists but should not...";

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Validate the distance detector
         * 
         * @featureDescription Validate the distance detector
         * 
         * @coveredRequirements DV-EVT_120, DV-EVT_50
         */
        VALIDATE_BETA_ANGLE_DETECTOR
    }

    /**
     * A Cartesian orbit used for the tests.
     */
    private static CartesianOrbit tISSOrbit;

    /**
     * A SpacecraftState used for the tests.
     */
    private static SpacecraftState tISSSpState;

    /**
     * A Cartesian orbit used for the tests.
     */
    private static CartesianOrbit tISSOrbit2;

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
        Utils.setDataRoot("regular-dataCNES-2003/de406-ephemerides");
        FramesFactory.clear();

        // Some orbit data for the tests
        // (Real ISS data!)
        double ix = 2156444.05;
        double iy = 3611777.68;
        double iz = -5316875.46;
        double ivx = -6579.446110;
        double ivy = 3916.478783;
        double ivz = 8.876119;

        iniDate = new AbsoluteDate("2011-11-09T12:00:00Z", TimeScalesFactory.getTT());
        final double mu = CelestialBodyFactory.getEarth().getGM();

        Vector3D issPos = new Vector3D(ix, iy, iz);
        Vector3D issVit = new Vector3D(ivx, ivy, ivz);
        PVCoordinates pvCoordinates = new PVCoordinates(issPos, issVit);
        tISSOrbit = new CartesianOrbit(pvCoordinates, FramesFactory.getEME2000(), iniDate, mu);
        tISSSpState = new SpacecraftState(tISSOrbit);

        // Another orbit
        ix = 2156444.05;
        iy = 3611777.68;
        iz = -5316875.46;
        ivz = -6579.446110;
        ivx = 3916.478783;
        ivy = 8.876119;
        issPos = new Vector3D(ix, iy, iz);
        issVit = new Vector3D(ivx, ivy, ivz);
        pvCoordinates = new PVCoordinates(issPos, issVit);
        tISSOrbit2 = new CartesianOrbit(pvCoordinates, FramesFactory.getEME2000(), iniDate, mu);

    }

    /**
     * @throws PatriusException 
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_BETA_ANGLE_DETECTOR}
     * 
     * @testedMethod {@link BetaAngleDetector#BetaAngleDetector(double) }
     * 
     * @description simple constructor test
     * 
     * @input constructor parameters : angle
     * 
     * @output a {@link BetaAngleDetector}
     * 
     * @testPassCriteria the {@link BetaAngleDetector} is successfully created
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void testBetaAngleDetectorCtor() throws PatriusException {
        final EventDetector detector =
            new BetaAngleDetector(-MathUtils.HALF_PI);
        // The constructor did not crash...
        Assert.assertNotNull(detector);
    }

    /**
     * @throws PatriusException 
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_BETA_ANGLE_DETECTOR}
     * 
     * @testedMethod {@link BetaAngleDetector#BetaAngleDetector(double, double, double)}
     * 
     * @description constructor test
     * 
     * @input constructor parameters : angle, max check, threshold
     * 
     * @output an {@link BetaAngleDetector}
     * 
     * @testPassCriteria the {@link BetaAngleDetector} is successfully created
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void testBetaAngleDetectorCtor2() throws PatriusException {
        final EventDetector detector =
            new BetaAngleDetector(MathUtils.HALF_PI, 10, 0.1);
        // The constructor did not crash...
        Assert.assertNotNull(detector);
    }

    /**
     * @throws PatriusException 
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_BETA_ANGLE_DETECTOR}
     * 
     * @testedMethod {@link BetaAngleDetector#BetaAngleDetector(double, double, double, Action)}
     * 
     * @description constructor test
     * 
     * @input constructor parameters : angle, max check, threshold and STOP Action
     * 
     * @output an {@link BetaAngleDetector}
     * 
     * @testPassCriteria the {@link BetaAngleDetector} is successfully created
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testBetaAngleDetectorCtor3() throws PatriusException {
        final BetaAngleDetector detector =
            new BetaAngleDetector(MathUtils.HALF_PI, 10, 0.1, Action.STOP);
        final BetaAngleDetector detector2 = (BetaAngleDetector) detector.copy();
        // Test getter
        Assert.assertEquals(MathUtils.HALF_PI, detector2.getAngle(), 0.);
    }

    /**
     * @throws PatriusException 
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_BETA_ANGLE_DETECTOR}
     * 
     * @testedMethod {@link BetaAngleDetector#BetaAngleDetector(double) }
     * 
     * @description error constructor test
     * 
     * @input constructor parameters : reference body, out-of-range angle
     * 
     * @output none
     * 
     * @testPassCriteria the {@link BetaAngleDetector} cannot be created (IllegalArgumentException)
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test(expected = IllegalArgumentException.class)
    public void testBetaAngleDetectorCtorError() throws PatriusException {
        // Illegal angle as parameter
        final EventDetector detector =
            new BetaAngleDetector(MathLib.nextAfter(MathUtils.HALF_PI, 9.));
        // We should never reach the next line...
        Assert.fail(detector.toString() + EXISTS_BUT_SHOULD_NOT);
    }

    /**
     * @throws PatriusException 
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_BETA_ANGLE_DETECTOR}
     * 
     * @testedMethod {@link BetaAngleDetector#BetaAngleDetector(double) }
     * 
     * @description error constructor test
     * 
     * @input constructor parameters : reference body, out-of-range angle
     * 
     * @output none
     * 
     * @testPassCriteria the {@link BetaAngleDetector} cannot be created (IllegalArgumentException)
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test(expected = IllegalArgumentException.class)
    public void testBetaAngleDetectorCtorError2() throws PatriusException {
        // Illegal angle as parameter
        final EventDetector detector =
            new BetaAngleDetector(MathLib.nextAfter(-MathUtils.HALF_PI, -9.));
        // We should never reach the next line...
        Assert.fail(detector.toString() + EXISTS_BUT_SHOULD_NOT);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_BETA_ANGLE_DETECTOR}
     * 
     * @testedMethod {@link BetaAngleDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * 
     * @description tests {@link BetaAngleDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * 
     * @input constructor and eventOccured parameters
     * 
     * @output eventOccured outputs
     * 
     * @testPassCriteria eventOccured returns the expected values for true and false as second parameters,
     *                   and for true and false as third parameters
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
        final EventDetector detector =
            new BetaAngleDetector(0.2121);
        // Distance is decreasing and integration direction is forward
        Action rez = detector.eventOccurred(tISSSpState, false, true);
        Assert.assertEquals(Action.STOP, rez);
        // Distance is increasing and integration direction is forward
        rez = detector.eventOccurred(tISSSpState, true, true);
        Assert.assertEquals(Action.STOP, rez);
        // Distance is decreasing and integration direction is backward
        rez = detector.eventOccurred(tISSSpState, false, false);
        Assert.assertEquals(Action.STOP, rez);
        // Distance is increasing and integration direction is backward
        rez = detector.eventOccurred(tISSSpState, true, false);
        Assert.assertEquals(Action.STOP, rez);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_BETA_ANGLE_DETECTOR}
     * 
     * @testedMethod {@link BetaAngleDetector#g(SpacecraftState)}
     * 
     * @description tests {@link BetaAngleDetector#g(SpacecraftState)}
     * 
     * @input constructor and g parameters
     * 
     * @output g output
     * 
     * @testPassCriteria g returns the expected value
     * 
     * @referenceVersion 4.2
     * 
     * @nonRegressionVersion 4.2
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @Test
    public void testG() throws PatriusException {
        final EventDetector detector = new BetaAngleDetector(-0.333);
        final double expectedDot = -0.7964035436316703;
        Assert.assertEquals(expectedDot, detector.g(tISSSpState), Utils.epsilonTest);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_BETA_ANGLE_DETECTOR}
     * 
     * @testedMethod {@link BetaAngleDetector#g(SpacecraftState)}
     * @testedMethod {@link BetaAngleDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * 
     * @description Propagation test 01 : detecting an angle
     * 
     * @input propagator, BetaAngleDetector input parameters
     * 
     * @output resulting SpacecraftState from the propagation
     * 
     * @testPassCriteria resulting SpacecraftState at the expected date
     * 
     * @referenceVersion 4.2
     * 
     * @nonRegressionVersion 4.2
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @Test
    public void testPropagation01() throws PatriusException {
        final double propagShift = 100000.;
        // Note : result changed for PATRIUS 4.2
        final double expectedShift = 4152.139988849637;
        final double angle = -1.13;

        // Propagator
        final KeplerianPropagator propagator = new KeplerianPropagator(tISSOrbit);

        // Detector for the given distance to the sun
        final EventDetector detector = new BetaAngleDetector(angle);
        propagator.addEventDetector(detector);

        final SpacecraftState curState = propagator.propagate(iniDate.shiftedBy(propagShift));

        Assert.assertEquals(expectedShift, curState.getDate().durationFrom(iniDate), Utils.epsilonTest);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_BETA_ANGLE_DETECTOR}
     * 
     * @testedMethod {@link BetaAngleDetector#g(SpacecraftState)}
     * @testedMethod {@link BetaAngleDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * 
     * @description Propagation test 02 : detecting another angle
     * 
     * @input propagator, BetaAngleDetector input parameters
     * 
     * @output resulting SpacecraftState from the propagation
     * 
     * @testPassCriteria resulting SpacecraftState at the expected date
     * 
     * @referenceVersion 4.2
     * 
     * @nonRegressionVersion 4.2
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @Test
    public void testPropagation02() throws PatriusException {
        final double propagShift = 100000.;
        // Note : result changed for PATRIUS 4.2
        final double expectedShift = 93862.60893850481;
        final double angle = 1.05;

        // Propagator
        final KeplerianPropagator propagator = new KeplerianPropagator(tISSOrbit2);

        // Detector for the given distance to the sun
        final EventDetector detector = new BetaAngleDetector(angle, 600, 1e-12);
        propagator.addEventDetector(detector);

        final SpacecraftState curState = propagator.propagate(iniDate.shiftedBy(propagShift));

        Assert.assertEquals(expectedShift, curState.getDate().durationFrom(iniDate), Utils.epsilonTest);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_BETA_ANGLE_DETECTOR}
     * 
     * @testedMethod {@link BetaAngleDetector#BetaAngleDetector(double, double, double, Action, boolean, fr.cnes.sirius.patrius.bodies.CelestialBody)}
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
    public void sunConstructorTest() throws PatriusException {
        // Same detectors
        final EventDetector detector2 = new BetaAngleDetector(-0.2, 0, 0, Action.CONTINUE, false);
        final EventDetector detector3 = new BetaAngleDetector(-0.2, 0, 0, Action.CONTINUE, false, CelestialBodyFactory.getSun());
        // Different detector
        final EventDetector detector1 = new BetaAngleDetector(-0.2, 0, 0, Action.CONTINUE, false, new MeeusSun());
        // Checks
        Assert.assertFalse(detector1.g(tISSSpState) == detector2.g(tISSSpState));
        Assert.assertTrue(detector2.g(tISSSpState) == detector3.g(tISSSpState));
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
        final BetaAngleDetector eventDetector1 = new BetaAngleDetector(-1.13, 10, 0.1, Action.CONTINUE,
            false, sun);
        final BetaAngleDetector eventDetector2 = (BetaAngleDetector) eventDetector1.copy();
        eventDetector2.setPropagationDelayType(PropagationDelayType.LIGHT_SPEED, FramesFactory.getGCRF());

        // Wrap these event detectors
        final SignalPropagationWrapperDetector wrapper1 = new SignalPropagationWrapperDetector(eventDetector1);
        final SignalPropagationWrapperDetector wrapper2 = new SignalPropagationWrapperDetector(eventDetector2);

        // Add them in the propagator, then propagate
        final KeplerianPropagator propagator = new KeplerianPropagator(tISSOrbit);
        propagator.addEventDetector(wrapper1);
        propagator.addEventDetector(wrapper2);
        final SpacecraftState finalState = propagator.propagate(iniDate.shiftedBy(5000));

        // Evaluate the first event detector wrapper (INSTANTANEOUS) (emitter dates should be equal to receiver dates)
        Assert.assertEquals(1, wrapper1.getNBOccurredEvents());
        Assert.assertTrue(wrapper1.getEmitterDatesList().get(0)
            .equals(new AbsoluteDate("2011-11-09T13:03:26.583"), 1e-3));
        Assert.assertTrue(wrapper1.getReceiverDatesList().get(0)
            .equals(new AbsoluteDate("2011-11-09T13:03:26.583"), 1e-3));

        // Evaluate the second event detector wrapper (LIGHT_SPEED) (emitter dates should be before receiver dates)
        Assert.assertEquals(1, wrapper2.getNBOccurredEvents());
        Assert.assertTrue(wrapper2.getEmitterDatesList().get(0)
            .equals(new AbsoluteDate("2011-11-09T13:03:26.583"), 1e-3));
        Assert.assertTrue(wrapper2.getReceiverDatesList().get(0)
            .equals(new AbsoluteDate("2011-11-09T13:11:40.882"), 1e-3));

        // Evaluate the AbstractSignalPropagationDetector's abstract methods implementation
        Assert.assertEquals(sun, eventDetector1.getEmitter(null));
        Assert.assertEquals(finalState.getOrbit(), eventDetector1.getReceiver(finalState));
        Assert.assertEquals(DatationChoice.RECEIVER, eventDetector1.getDatationChoice());
    }
}
