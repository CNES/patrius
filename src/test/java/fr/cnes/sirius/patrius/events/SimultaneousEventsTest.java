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
 * @history created 23/04/12
 *
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3143:10/05/2022:[PATRIUS] Nouvelle interface OrbitEventDetector et nouvelles classes
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::FA:558:25/02/2016:Correction of algorithm for simultaneous events detection
 * VERSION::FA:612:21/07/2016:Bug in same date events with Action.STOP
 * VERSION::FA:673:12/09/2016: add getTotalMass(state)
 * VERSION::DM:1173:26/06/2017:add propulsive and tank properties
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.forces.maneuvers.ImpulseManeuver;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.AbstractIntegrator;
import fr.cnes.sirius.patrius.math.ode.FirstOrderDifferentialEquations;
import fr.cnes.sirius.patrius.math.ode.nonstiff.AdaptiveStepsizeIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.AbstractPropagator;
import fr.cnes.sirius.patrius.propagation.MassProvider;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SimpleMassModel;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.propagation.events.AbstractDetector;
import fr.cnes.sirius.patrius.propagation.events.DateDetector;
import fr.cnes.sirius.patrius.propagation.events.DistanceDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector.Action;
import fr.cnes.sirius.patrius.propagation.events.EventsLogger;
import fr.cnes.sirius.patrius.propagation.events.NodeDetector;
import fr.cnes.sirius.patrius.propagation.events.NthOccurrenceDetector;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This test illustrates the fact that event handling in the orekit propagator can detect two events
 * occuring at the same time.
 * 
 * @author cardosop
 * 
 * @version $Id$
 * 
 * @since 1.2
 * 
 */
public class SimultaneousEventsTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Validate the detection of simultaneous events
         * 
         * @featureDescription Validate the detection of simultaneous events
         * 
         * @coveredRequirements
         */
        VALIDATE_SIMULTANEOUS_EVENTS
    }

    /**
     * A Cartesian orbit used for the tests.
     */
    private static CartesianOrbit tISSOrbit;

    /** The Sun. */
    private static CelestialBody theSun;

    /** Initial propagator date. */
    private static AbsoluteDate iniDate;

    /** Counter for node detection. */
    private static int count;

    /** First distance detector. */
    private NDistanceDetector detector;

    /** Second distance detector. */
    private NDistanceDetector detector2;

    /**
     * Custom distance detector.
     */
    final class NDistanceDetector extends DistanceDetector {

        /** Serializable UID. */
        private static final long serialVersionUID = -4821124789984523302L;

        /**
         * The date of the event.
         */
        private AbsoluteDate eventDate = null;

        /**
         * Constructor.
         * 
         * @param pb
         *        the PV coordinates provider
         * @param dist
         *        the distance that triggers an event
         */
        public NDistanceDetector(final PVCoordinatesProvider pb, final double dist) {
            super(pb, dist);
        }

        @Override
        public Action
            eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                throws PatriusException {
            if (this.eventDate == null) {
                this.eventDate = s.getDate();
            }
            return Action.CONTINUE;
        }

        public AbsoluteDate getFirstEventDate() {
            return this.eventDate;
        }
    }

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
        Utils.setDataRoot("regular-dataPBASE");

        // Some orbit data for the tests
        // (Real ISS data!)
        final double ix = 2156444.05;
        final double iy = 3611777.68;
        final double iz = -5316875.46;
        final double ivx = -6579.446110;
        final double ivy = 3916.478783;
        final double ivz = 8.876119;

        iniDate = new AbsoluteDate("2011-11-09T12:00:00Z", TimeScalesFactory.getTT());
        final double mu = CelestialBodyFactory.getEarth().getGM();
        theSun = CelestialBodyFactory.getSun();

        final Vector3D issPos = new Vector3D(ix, iy, iz);
        final Vector3D issVit = new Vector3D(ivx, ivy, ivz);
        final PVCoordinates pvCoordinates = new PVCoordinates(issPos, issVit);
        tISSOrbit = new CartesianOrbit(pvCoordinates, FramesFactory.getEME2000(), iniDate, mu);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_SIMULTANEOUS_EVENTS}
     * 
     * @testedMethod {@link AbstractPropagator#propagate(AbsoluteDate, AbsoluteDate)}
     * 
     * @description tests that two simultaneous events are detected and logged by an Orekit
     *              Keplerian propagator.
     * 
     * @input an orbit, two distance detectors, an events logger
     * 
     * @output the events logged during the propagation using an events logger
     * 
     * @testPassCriteria the dates of two simultaneous events are equal
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public void testSimultaneousDistanceKeplerianPropagator() throws PatriusException {
        final double propagShift = 100000.;

        final double distance = 1.4818E11;

        // Propagator
        final KeplerianPropagator propagator = new KeplerianPropagator(tISSOrbit);
        final EventsLogger logger = new EventsLogger();
        // Detector for the given distance to the sun
        this.detector = new NDistanceDetector(theSun, distance);
        propagator.addEventDetector(logger.monitorDetector(this.detector));

        // Another detector for the very same event
        this.detector2 = new NDistanceDetector(theSun, distance);
        propagator.addEventDetector(logger.monitorDetector(this.detector2));

        propagator.propagate(iniDate.shiftedBy(propagShift));

        for (int i = 0; i < logger.getLoggedEvents().size() / 2; i += 2) {
            // checks the dates of two consecutive events are equal:
            Assert.assertEquals(logger.getLoggedEvents().get(i).getState().getDate(),
                logger.getLoggedEvents().get(i + 1).getState().getDate());
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_SIMULTANEOUS_EVENTS}
     * 
     * @testedMethod {@link AbstractIntegrator#integrate(FirstOrderDifferentialEquations, double, double[], double, double[])}
     * 
     * @description tests that two simultaneous events are detected and logged by a CM
     *              numerical propagator.
     * 
     * @input an orbit, two distance detectors, an events logger
     * 
     * @output the events logged during the propagation using an events logger
     * 
     * @testPassCriteria the dates of two simultaneous events are equal
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public void testSimultaneousDistanceNumericalPropagator() throws PatriusException {
        final double propagShift = 100000.;
        final double distance = 1.4818E11;

        // Propagator
        // sets the integrator:
        final double[] absTolerance = {
            0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6
        };
        final double[] relTolerance = {
            1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7
        };
        final AdaptiveStepsizeIntegrator integrator =
            new DormandPrince853Integrator(0.001, 1000, absTolerance, relTolerance);
        final NumericalPropagator propagator = new NumericalPropagator(integrator);
        propagator.setInitialState(new SpacecraftState(tISSOrbit));
        final EventsLogger logger = new EventsLogger();
        // Detector for the given distance to the sun
        this.detector = new NDistanceDetector(theSun, distance);
        propagator.addEventDetector(logger.monitorDetector(this.detector));

        // Another detector for the very same event
        this.detector2 = new NDistanceDetector(theSun, distance);
        propagator.addEventDetector(logger.monitorDetector(this.detector2));

        propagator.propagate(iniDate.shiftedBy(propagShift));

        for (int i = 0; i < logger.getLoggedEvents().size() / 2; i += 2) {
            // checks the dates of two consecutive events are equal:
            Assert.assertEquals(logger.getLoggedEvents().get(i).getState().getDate(),
                logger.getLoggedEvents().get(i + 1).getState().getDate());
        }
    }

    /**
     * FA-558
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_SIMULTANEOUS_EVENTS}
     * 
     * @testedMethod {@link AbstractIntegrator#integrate(FirstOrderDifferentialEquations, double, double[], double, double[])}
     * 
     * @description tests that three simultaneous events are detected and logged by an Orekit Keplerian propagator.
     * 
     * @input an orbit, two node detectors, one being wrapped by an nth occurrence detector.
     * 
     * @output the events logged during the propagation using an events logger
     * 
     * @testPassCriteria nodes are all properly detected
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testSimultaneousNodesKeplerianPropagator() throws PatriusException {

        // Initialization
        Utils.setDataRoot("regular-dataPBASE");
        count = 0;
        final AbsoluteDate date = new AbsoluteDate(2003, 1, 1, TimeScalesFactory.getUTC());
        final Orbit orbit = new KeplerianOrbit(7000000, 0.01, 0, 0, 0, 0, PositionAngle.TRUE, FramesFactory.getGCRF(),
            date, Constants.EGM96_EARTH_MU);

        // Node Detector
        final NodeDetector nd = new NodeDetector(FramesFactory.getITRF(), orbit.getKeplerianPeriod() / 30., 0.0001,
            Action.CONTINUE, Action.CONTINUE, false, false){
            /** Serializable UID. */
            private static final long serialVersionUID = 6892849024296442080L;

            @Override
            public double g(final SpacecraftState state)
                throws PatriusException {
                return super.g(state);
            }

            @Override
            public Action
                eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                    throws PatriusException {
                System.out.println("Node : " + s.getDate() + " ; " + s.getDate().durationFrom(date));
                count++;
                return super.eventOccurred(s, increasing, forward);
            }
        };

        // NthOccurrence Detector
        final NodeDetector and = new NodeDetector(FramesFactory.getITRF(), NodeDetector.ASCENDING,
            orbit.getKeplerianPeriod() / 30., 0.0001, Action.CONTINUE, false){
            /** Serializable UID. */
            private static final long serialVersionUID = -9065922349343249850L;

            @Override
            public double g(final SpacecraftState state)
                throws PatriusException {
                return super.g(state);
            }

            @Override
            public Action
                eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                    throws PatriusException {
                System.out.println("Node ASCENDING: " + s.getDate() + " ; " + s.getDate().durationFrom(date));
                return super.eventOccurred(s, increasing, forward);
            }
        };

        final NthOccurrenceDetector nthOccAND = new NthOccurrenceDetector(and, 6, Action.RESET_STATE, false){
            /** Serializable UID. */
            private static final long serialVersionUID = -334719553114843415L;

            @Override
            public double g(final SpacecraftState s) throws PatriusException {
                return super.g(s);
            }

            @Override
            public Action
                eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                    throws PatriusException {
                this.getEvent().eventOccurred(s, increasing, forward);
                return super.eventOccurred(s, increasing, forward);
            }

            @Override
            public SpacecraftState resetState(final SpacecraftState oldState) throws PatriusException {
                System.out.println("Node (Nth occ) : " + oldState.getDate() + " ; "
                        + oldState.getDate().durationFrom(date));
                return oldState;
            }
        };

        // Propagation (bug is reproduced with duration < 44800)
        final double duration = 44700;
        final KeplerianPropagator p = new KeplerianPropagator(orbit);
        p.addEventDetector(nd);
        p.addEventDetector(nthOccAND);
        p.propagate(orbit.getDate(), orbit.getDate().shiftedBy(duration));

        // Check number of detected nodes is as expected
        Assert.assertEquals(15, count);
    }

    /**
     * FA-612.
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_SIMULTANEOUS_EVENTS}
     * 
     * @description tests that two simultaneous events are detected, second event stops propagation
     *              (analytical and numerical propagators).
     * 
     * @testPassCriteria Propagation stopped at event date
     * 
     * @referenceVersion 3.3
     * 
     * @nonRegressionVersion 3.3
     */
    @Test
    public void testSameDateStop() throws PatriusException {

        // Initialization
        final AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH;
        final Orbit initialOrbit = new KeplerianOrbit(7000000., 0, MathLib.toRadians(50.), 0., 0., 0.,
            PositionAngle.TRUE, FramesFactory.getEME2000(), initDate, Constants.EGM96_EARTH_MU);
        final MassProvider massModel = new SimpleMassModel(5000., "body");

        // Analytical propagator
        final KeplerianPropagator propagator1 = new KeplerianPropagator(initialOrbit, null, Constants.EGM96_EARTH_MU,
            massModel);
        sameDateStopTest(propagator1);

        // Numerical propagator
        final NumericalPropagator propagator2 = new NumericalPropagator(new ClassicalRungeKuttaIntegrator(30.));
        propagator2.setInitialState(new SpacecraftState(initialOrbit, massModel));
        propagator2.setMassProviderEquation(massModel);
        sameDateStopTest(propagator2);
    }

    /**
     * FA-612.
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_SIMULTANEOUS_EVENTS}
     * 
     * @description tests that two simultaneous events are detected, second event detector is removed, hence no further
     *              detection occurs
     *              (analytical and numerical propagators).
     * 
     * @testPassCriteria 2nd detector detects only one event
     * 
     * @referenceVersion 3.3
     * 
     * @nonRegressionVersion 3.3
     */
    @Test
    public void testSameDateRemoveDetector() throws PatriusException {

        // Initialization
        final AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH;
        final Orbit initialOrbit = new KeplerianOrbit(7000000., 0, MathLib.toRadians(50.), 0., 0., 0.,
            PositionAngle.TRUE, FramesFactory.getEME2000(), initDate, Constants.EGM96_EARTH_MU);
        final MassProvider massModel = new SimpleMassModel(5000., "body");

        // Analytical propagator
        final KeplerianPropagator propagator1 = new KeplerianPropagator(initialOrbit, null, Constants.EGM96_EARTH_MU,
            massModel);
        sameDateRemoveDetectorTest(propagator1);

        // Numerical propagator
        final NumericalPropagator propagator2 = new NumericalPropagator(new ClassicalRungeKuttaIntegrator(30.));
        propagator2.setInitialState(new SpacecraftState(initialOrbit, massModel));
        propagator2.setMassProviderEquation(massModel);
        sameDateRemoveDetectorTest(propagator2);
    }

    /**
     * Performs propagation with two detectors at same date, second detector performs an Action.STOP.
     * 
     * @param propagator
     *        a propagator.
     * @throws PatriusException
     */
    private static void sameDateStopTest(final Propagator propagator) throws PatriusException {

        // Initialization
        final AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH;
        final Orbit initialOrbit = new KeplerianOrbit(7000000., 0, MathLib.toRadians(50.), 0., 0., 0.,
            PositionAngle.TRUE, FramesFactory.getEME2000(), initDate, Constants.EGM96_EARTH_MU);
        final MassProvider massModel = new SimpleMassModel(5000., "body");

        // Maneuver
        final AbsoluteDate manDate = initDate.shiftedBy(86400);
        final DateDetector manDateDetector = new DateDetector(manDate, AbstractDetector.DEFAULT_MAXCHECK,
            AbstractDetector.DEFAULT_THRESHOLD);
        final ImpulseManeuver impManeuver = new ImpulseManeuver(manDateDetector, new Vector3D(100, 0, 0), 300,
            massModel, "body", LOFType.TNW);
        propagator.addEventDetector(impManeuver);

        // Stop propagation detector
        final DateDetector endPropagationDetector = new DateDetector(manDate.shiftedBy(0.0),
            AbstractDetector.DEFAULT_MAXCHECK, AbstractDetector.DEFAULT_THRESHOLD);
        propagator.addEventDetector(endPropagationDetector);

        // Propagation with maneuver
        final AbsoluteDate maxDate = initialOrbit.getDate().shiftedBy(31 * 86400);
        final SpacecraftState finalSc = propagator.propagate(maxDate);

        // Check
        Assert.assertEquals(0., finalSc.getDate().durationFrom(manDate), 0.);
    }

    /**
     * Performs propagation with two detectors at same date, second detector performs a remove detector.
     * 
     * @param propagator
     *        a propagator.
     * @throws PatriusException
     */
    private static void sameDateRemoveDetectorTest(final Propagator propagator) throws PatriusException {

        // Initialization
        final AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH;
        final Orbit initialOrbit = new KeplerianOrbit(7000000., 0, MathLib.toRadians(50.), 0., 0., 0.,
            PositionAngle.TRUE, FramesFactory.getEME2000(), initDate, Constants.EGM96_EARTH_MU);
        final MassProvider massModel = new SimpleMassModel(5000., "body");

        initDate.shiftedBy(86400);
        final NodeDetector manDateDetector = new NodeDetector(FramesFactory.getGCRF(), 0,
            AbstractDetector.DEFAULT_MAXCHECK, AbstractDetector.DEFAULT_THRESHOLD);
        final ImpulseManeuver impManeuver = new ImpulseManeuver(manDateDetector, new Vector3D(100, 0, 0), 300,
            massModel, "body", LOFType.TNW);
        propagator.addEventDetector(impManeuver);

        // Remove action detector
        count = 0;
        final NodeDetector removeActionDetector = new NodeDetector(FramesFactory.getGCRF(), 2,
            AbstractDetector.DEFAULT_MAXCHECK, AbstractDetector.DEFAULT_THRESHOLD, Action.CONTINUE, true){
            /** Serializable UID. */
            private static final long serialVersionUID = -3906809989400993924L;

            @Override
            public Action
                eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                    throws PatriusException {
                count++;
                return Action.CONTINUE;
            }
        };
        propagator.addEventDetector(removeActionDetector);

        // Propagation with maneuver
        final AbsoluteDate maxDate = initialOrbit.getDate().shiftedBy(10 * 86400);
        propagator.propagate(maxDate);

        // Check
        Assert.assertEquals(1, count);
    }
}
