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
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:207:27/03/2014:Added type of AOL to detect as well as reference equator
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:226:12/09/2014: problem with event detections.
 * VERSION::DM:394:20/02/2015: Added possibility to define the action performed when event occurs
 * VERSION::DM:1489:18/05/2018:Repatriation of custom class from Genopus
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.events;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.ode.nonstiff.AdaptiveStepsizeIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.CircularOrbit;
import fr.cnes.sirius.patrius.orbits.EquinoctialOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.propagation.events.EventDetector.Action;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScale;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Unit tests for {@link AOLDetector}.
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id: AOLDetectorTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.1
 * 
 */
public class AOLDetectorTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Validate the Argument of Latitude detector
         * 
         * @featureDescription Validate the Argument of Latitude detector
         * 
         * @coveredRequirements DV-EVT_120, DV-EVT_50
         */
        VALIDATE_AOL_DETECTOR
    }

    /**
     * An elliptic orbit used for the tests.
     */
    private static KeplerianOrbit orbit;

    /**
     * A circular (non equatorial) orbit used for the tests.
     */
    private static CircularOrbit circularNonEquatOrbit;

    /**
     * A circular (equatorial) orbit used for the tests.
     */
    private static CircularOrbit circularEquatOrbit;

    /**
     * An equinoctial (non equatorial) orbit used for the tests.
     */
    private static EquinoctialOrbit equinoctialNonEquatOrbit;

    /**
     * An equinoctial (equatorial) orbit used for the tests.
     */
    private static EquinoctialOrbit equinoctialEquatOrbit;

    /**
     * Initial propagator date.
     */
    private static AbsoluteDate iniDate;

    /**
     * gcrf frame.
     */
    private static Frame gcrf = FramesFactory.getGCRF();

    /**
     * Shortcut.
     */
    private static PositionAngle E = PositionAngle.ECCENTRIC;

    /**
     * Shortcut.
     */
    private static PositionAngle V = PositionAngle.TRUE;

    /**
     * Shortcut.
     */
    private static PositionAngle M = PositionAngle.MEAN;

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
        FramesFactory.setConfiguration(fr.cnes.sirius.patrius.Utils.getIERS2003Configuration(true));

        iniDate = new AbsoluteDate("2011-11-09T12:00:00Z", TimeScalesFactory.getTT());
        final double mu = CelestialBodyFactory.getEarth().getGM();

        orbit = new KeplerianOrbit(9000000, 0.02, 0.8, 1.2, 2.5, 0.02,
            PositionAngle.TRUE, gcrf, iniDate, mu);
        circularNonEquatOrbit = new CircularOrbit(8000000, 0, 0, MathLib.toRadians(18), 0, 0,
            PositionAngle.TRUE, gcrf, iniDate, mu);
        circularEquatOrbit = new CircularOrbit(8000000, 0.02, 0.1, 0, 0, MathLib.toRadians(5),
            PositionAngle.TRUE, gcrf, iniDate, mu);
        equinoctialNonEquatOrbit = new EquinoctialOrbit(8000000, 0, 0, MathLib.toRadians(9.6), MathLib.toRadians(67),
            MathLib.toRadians(1),
            PositionAngle.TRUE, gcrf, iniDate, mu);
        equinoctialEquatOrbit = new EquinoctialOrbit(8000000, 0.2, 0.05, 0, 0, MathLib.toRadians(10),
            PositionAngle.TRUE, gcrf, iniDate, mu);
    }

    /**
     * @throws PatriusException
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_AOL_DETECTOR}
     * 
     * @testedMethod {@link AOLDetector#AOLDetector(double, PositionAngle, Frame)}
     * 
     * @description Test detection of AOL in ITRF with orbit in GCRF
     * 
     * @input an orbit and the three dates corresponding to the three types of AOL
     * 
     * @output the recomputed respective AOLs from the states at detection time
     * 
     * @testPassCriteria the three values are equal to 1.15e-13 rad, which is the angular accuracy
     *                   corresponding to the date detection accuracy (1e-10 s) for this orbit
     * 
     * @referenceVersion 2.2
     * 
     * @nonRegressionVersion 2.2
     */
    @Test
    public void testAOLValues() throws PatriusException {

        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));

        /*
         * Reference
         */

        // constants
        final double ae = Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS;
        final double mu = Constants.GRIM5C1_EARTH_MU;

        // parameters
        final double a = ae + 400e3;
        final double e = .002;
        final double i = .59;
        final double pa = .5;
        final double raan = .2;
        final double w = .2;

        // date
        final AbsoluteDate j2000 = AbsoluteDate.J2000_EPOCH.shiftedBy(43200);

        // frames
        final Frame gcrf = FramesFactory.getGCRF();

        // orbit in frozen frame
        final Orbit orbit = new KeplerianOrbit(a, e, i, pa, raan, w, PositionAngle.TRUE, gcrf, j2000, mu);

        // end date
        final AbsoluteDate end = j2000.shiftedBy(orbit.getKeplerianPeriod());

        // keplerian
        final KeplerianPropagator propagator = new KeplerianPropagator(orbit);

        // detectors
        final OverriddenAOLDetector aolV = new OverriddenAOLDetector(.2, V, gcrf);
        final OverriddenAOLDetector aolM = new OverriddenAOLDetector(.2, M, gcrf);
        final OverriddenAOLDetector aolE = new OverriddenAOLDetector(.2, E, gcrf);

        propagator.addEventDetector(aolE);
        propagator.addEventDetector(aolM);
        propagator.addEventDetector(aolV);

        propagator.propagate(end);

        // states
        final SpacecraftState se = aolE.getDetectionState();
        final SpacecraftState sm = aolM.getDetectionState();
        final SpacecraftState sv = aolV.getDetectionState();

        // orbits at dates
        final CircularOrbit ce = (CircularOrbit) OrbitType.CIRCULAR.convertType(
            new CartesianOrbit(se.getPVCoordinates(gcrf), gcrf, se.getDate(), se.getMu()));
        final CircularOrbit cm = (CircularOrbit) OrbitType.CIRCULAR.convertType(
            new CartesianOrbit(sm.getPVCoordinates(gcrf), gcrf, sm.getDate(), sm.getMu()));
        final CircularOrbit cv = (CircularOrbit) OrbitType.CIRCULAR.convertType(
            new CartesianOrbit(sv.getPVCoordinates(gcrf), gcrf, sv.getDate(), sv.getMu()));

        // angular error wrt date detection error
        final double v = orbit.getPVCoordinates().getVelocity().getNorm() * 1.2;
        final double p = orbit.getPVCoordinates().getPosition().getNorm() * 1.2;
        final double err = v * this.detectionAccuracy / p;
        System.out.println(err);

        // values of aol
        Assert.assertEquals(0, ce.getAlphaE() - .2, err);
        Assert.assertEquals(0, cm.getAlphaM() - .2, err);
        Assert.assertEquals(0, cv.getAlphaV() - .2, err);
    }

    /**
     * @throws PatriusException
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_AOL_DETECTOR}
     * 
     * @testedMethod {@link AOLDetector#AOLDetector(double, PositionAngle, Frame)}
     * 
     * @description Test detection of AOL in ITRF with orbit in GCRF
     * 
     * @input circular orbit
     * 
     * @output the three types of AOL
     * 
     * @testPassCriteria the three values are equal to 1e-14 rad
     * 
     * @referenceVersion 2.2
     * 
     * @nonRegressionVersion 2.2
     */
    @Test
    public void testCircularOrbit() throws PatriusException {

        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));

        /*
         * Reference
         */

        // constants
        final double ae = Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS;
        final double mu = Constants.GRIM5C1_EARTH_MU;

        // parameters
        final double a = ae + 400e3;
        final double e = .000;
        final double i = .59;
        final double pa = .5;
        final double raan = .2;
        final double w = .2;

        // date
        final AbsoluteDate j2000 = AbsoluteDate.J2000_EPOCH.shiftedBy(43200);

        // frames
        final Frame gcrf = FramesFactory.getGCRF();
        FramesFactory.getITRF();

        // orbit in frozen frame
        final Orbit orbit = new KeplerianOrbit(a, e, i, pa, raan, w, PositionAngle.TRUE, gcrf, j2000, mu);

        // end date
        final AbsoluteDate end = j2000.shiftedBy(orbit.getKeplerianPeriod());

        // keplerian
        final KeplerianPropagator propagator = new KeplerianPropagator(orbit);

        // detectors
        final OverriddenAOLDetector aolV = new OverriddenAOLDetector(.2, V, gcrf);
        final OverriddenAOLDetector aolM = new OverriddenAOLDetector(.2, M, gcrf);
        final OverriddenAOLDetector aolE = new OverriddenAOLDetector(.2, E, gcrf);

        propagator.addEventDetector(aolE);
        propagator.addEventDetector(aolM);
        propagator.addEventDetector(aolV);

        propagator.propagate(end);

        // dates
        final AbsoluteDate de = aolE.getDetectionDate();
        final AbsoluteDate dm = aolM.getDetectionDate();
        final AbsoluteDate dv = aolV.getDetectionDate();

        final TimeScale tai = TimeScalesFactory.getTAI();

        Assert.assertEquals(0, de.offsetFrom(dv, tai), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(0, dm.offsetFrom(dv, tai), Precision.DOUBLE_COMPARISON_EPSILON);

    }

    /**
     * @throws PatriusException
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_AOL_DETECTOR}
     * 
     * @testedMethod {@link AOLDetector#AOLDetector(double, PositionAngle, Frame)}
     * 
     * @description Test detection of AOL in ITRF with orbit in GCRF
     * 
     * @input orbital parameters in frozen ITRF and detection date in ITRF
     * 
     * @output detection date in frozen ITRF at input detection date
     * 
     * @testPassCriteria the detection dates are the same less than 1e-10 s of error (event detection accuracy)
     * 
     * @referenceVersion 2.2
     * 
     * @nonRegressionVersion 2.2
     */
    @Test
    public void testITRFAndThenFreeze() throws PatriusException {

        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));

        /*
         * Reference
         */

        // constants
        final double ae = Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS;
        final double mu = Constants.GRIM5C1_EARTH_MU;

        // parameters
        final double a = ae + 400e3;
        final double e = .001;
        final double i = .58;
        final double pa = .5;
        final double raan = .2;
        final double w = .2;

        // date
        final AbsoluteDate j2000 = AbsoluteDate.J2000_EPOCH.shiftedBy(43200);

        // frames
        final Frame gcrf = FramesFactory.getGCRF();
        final Frame itrf = FramesFactory.getITRF();

        // orbit in frozen frame
        final Orbit orbit = new KeplerianOrbit(a, e, i, pa, raan, w, PositionAngle.TRUE, gcrf, j2000, mu);

        // end date
        final AbsoluteDate end = j2000.shiftedBy(orbit.getKeplerianPeriod());

        // keplerian
        final KeplerianPropagator propagator = new KeplerianPropagator(orbit);

        // detectors
        final OverriddenAOLDetector aol = new OverriddenAOLDetector(3.14 / 4, V, itrf);
        propagator.addEventDetector(aol);

        // propagation
        propagator.propagate(end);

        // frozen ITRF at detected date
        final AbsoluteDate ref = aol.getDetectionDate();
        final Frame frozenITRF = itrf.getFrozenFrame(gcrf, ref, "frozen ITRF");

        // keplerian
        final KeplerianPropagator propagator2 = new KeplerianPropagator(orbit);

        // detectors
        final OverriddenAOLDetector aol2 = new OverriddenAOLDetector(3.14 / 4, V, frozenITRF);
        propagator2.addEventDetector(aol2);

        // propagation
        propagator2.propagate(end);

        // actual date
        final AbsoluteDate actual = aol2.getDetectionDate();

        Assert.assertEquals(0, actual.offsetFrom(ref, TimeScalesFactory.getTAI()), this.detectionAccuracy);
    }

    /**
     * @throws PatriusException
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_AOL_DETECTOR}
     * 
     * @testedMethod {@link AOLDetector#AOLDetector(double, PositionAngle, Frame)}
     * 
     * @description Test detection of AOL in ITRF with orbit in GCRF
     * 
     * @input orbital parameters in frozen ITRF
     * 
     * @output 0° AOL detection in frozen ITRF and ascending node in frozen ITRF detection
     * 
     * @testPassCriteria the detection dates are the ssame less than 1e-10 s of error (event detection accuracy)
     * 
     * @referenceVersion 2.2
     * 
     * @nonRegressionVersion 2.2
     */
    @Test
    public void testITRFAndRAAN() throws PatriusException {

        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));

        /*
         * Reference
         */

        // constants
        final double ae = Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS;
        final double mu = Constants.GRIM5C1_EARTH_MU;

        // parameters
        final double a = ae + 400e3;
        final double e = .0001;
        final double i = .58;
        final double pa = .5;
        final double raan = .2;
        final double w = .2;

        // date
        final AbsoluteDate j2000 = AbsoluteDate.J2000_EPOCH.shiftedBy(43200);

        // frames
        final Frame gcrf = FramesFactory.getGCRF();
        final Frame frozenITRF = FramesFactory.getITRF().getFrozenFrame(gcrf, j2000, "ITRF frozen");

        // orbit in frozen frame
        final Orbit orbit = new KeplerianOrbit(a, e, i, pa, raan, w, PositionAngle.TRUE, frozenITRF, j2000, mu);

        // end date
        final AbsoluteDate end = j2000.shiftedBy(orbit.getKeplerianPeriod());

        // keplerian
        final KeplerianPropagator propagator = new KeplerianPropagator(orbit);

        // detectors
        final OverriddenAOLDetector aol = new OverriddenAOLDetector(0., V, frozenITRF);
        final OverriddenNodeDetector node = new OverriddenNodeDetector(orbit, frozenITRF);
        propagator.addEventDetector(aol);
        propagator.addEventDetector(node);

        // propagation
        propagator.propagate(end);
        Assert.assertEquals(0, aol.getDetectionDate().offsetFrom(node.getDetectionDate(), TimeScalesFactory.getTAI()),
            this.detectionAccuracy);
    }

    private final double detectionAccuracy = 1e-10;

    /**
     * @throws PatriusException
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_AOL_DETECTOR}
     * 
     * @testedMethod {@link AOLDetector#AOLDetector(double, PositionAngle, Frame)}
     * 
     * @description Test detection of AOL in ITRF with orbit in GCRF
     * 
     * @input orbital parameters in frozen ITRF / reference detection date
     * 
     * @output AOL detection in ITRF and AOL detection in GCRF
     * 
     * @testPassCriteria the detection date in ITRF is the same as the reference with 1e-10 s (event detection
     *                   accuracy). the detection
     *                   date in GCRF differs with at least 1e-10 s
     * 
     * @referenceVersion 2.2
     * 
     * @nonRegressionVersion 2.2
     */
    @Test
    public void testFramesPart1() throws PatriusException {

        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));

        /*
         * Reference
         */

        // constants
        final double ae = Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS;
        final double mu = Constants.GRIM5C1_EARTH_MU;

        // parameters
        final double a = ae + 400e3;
        final double e = .001;
        final double i = .58;
        final double pa = .5;
        final double raan = .2;
        final double w = .2;

        // date
        final AbsoluteDate j2000 = AbsoluteDate.J2000_EPOCH.shiftedBy(43200);

        // frames
        final Frame gcrf = FramesFactory.getGCRF();
        final Frame frozenITRF = FramesFactory.getITRF().getFrozenFrame(gcrf, j2000, "ITRF frozen");

        // orbit in frozen frame
        final Orbit orbit = new KeplerianOrbit(a, e, i, pa, raan, w, PositionAngle.TRUE, frozenITRF, j2000, mu);

        // end date
        final AbsoluteDate end = j2000.shiftedBy(orbit.getKeplerianPeriod());

        // keplerian
        final KeplerianPropagator propagator = new KeplerianPropagator(orbit);

        // detector
        final OverriddenAOLDetector detector = new OverriddenAOLDetector(.2, V, frozenITRF);
        propagator.addEventDetector(detector);

        // propagation
        propagator.propagate(end);

        // REFERENCE DATE
        final AbsoluteDate reference = detector.getDetectionDate();

        /*
         * SAME ORBIT IN A DIFFERENT FRAME
         */
        // pvs
        final PVCoordinates pvInGcrf = orbit.getPVCoordinates(gcrf);

        // orbit
        final Orbit orbitInGcrf = new CartesianOrbit(pvInGcrf, gcrf, j2000, mu);

        // keplerian
        final KeplerianPropagator propagator2 = new KeplerianPropagator(orbitInGcrf);

        // detector
        propagator2.addEventDetector(detector);

        // propagation
        propagator2.propagate(end);

        // get the detected date
        final AbsoluteDate actual = detector.getDetectionDate();

        // check is 1e-6 s
        Assert.assertEquals(0, actual.offsetFrom(reference, TimeScalesFactory.getTAI()), this.detectionAccuracy);

        /*
         * SAME ORBIT / AOL detection in gcrf
         */
        // keplerian
        final KeplerianPropagator propagator3 = new KeplerianPropagator(orbitInGcrf);

        // detector
        final OverriddenAOLDetector detector3 = new OverriddenAOLDetector(.2, V, gcrf);
        propagator3.addEventDetector(detector3);

        // propagation
        propagator3.propagate(end);

        // get the detected date
        final AbsoluteDate actual2 = detector3.getDetectionDate();

        // check that the dates are different with at least 1e-6 sec
        Assert
            .assertTrue(MathLib.abs(actual2.offsetFrom(reference, TimeScalesFactory.getTAI())) > this.detectionAccuracy);

    }

    /**
     * Overrides the eventoccured method to get detection date
     * 
     * @author houdroger
     * 
     */
    class OverriddenNodeDetector extends NodeDetector {

        public OverriddenNodeDetector(final Orbit orbit, final Frame frame) {
            super(orbit, frame, 0, AOLDetectorTest.this.detectionAccuracy);
        }

        AbsoluteDate detectionDate;

        public AbsoluteDate getDetectionDate() {
            return this.detectionDate;
        }

        @Override
        public
                Action
                eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                       throws PatriusException {
            this.detectionDate = s.getDate();
            System.out.println(s.getDate() + " : Target Node " + increasing);
            return Action.CONTINUE;
        }
    }

    /**
     * Overrides the eventoccured method to get detection date
     * 
     * @author houdroger
     * 
     */
    class OverriddenAOLDetector extends AOLDetector {

        AbsoluteDate detectionDate;

        public AbsoluteDate getDetectionDate() {
            return this.detectionDate;
        }

        SpacecraftState detectionState;

        public SpacecraftState getDetectionState() {
            return this.detectionState;
        }

        public OverriddenAOLDetector(final double angle, final PositionAngle type,
            final Frame equator) {
            super(angle, type, equator, 60, AOLDetectorTest.this.detectionAccuracy);
        }

        @Override
        public
                Action
                eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                       throws PatriusException {
            this.detectionDate = s.getDate();
            this.detectionState = s;
            System.out.println(s.getDate() + " : Target AOL");
            return Action.CONTINUE;
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_AOL_DETECTOR}
     * 
     * @testedMethod {@link AOLDetector#AOLDetector(double, PositionAngle, Frame)}
     * 
     * @description simple constructor test
     * 
     * @input constructor parameters : position angle AOL
     * 
     * @output an AOLDetector
     * 
     * @testPassCriteria the {@link AOLDetector} is successfully created
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public final void testAOLDetectorCtor1() {
        final AOLDetector detector = new AOLDetector(MathLib.toRadians(10), V, gcrf);
        // The constructor did not crash...
        Assert.assertNotNull(detector);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_AOL_DETECTOR}
     * 
     * @testedMethod {@link AOLDetector#AOLDetector(double, PositionAngle, Frame, double, double)}
     * 
     * @description simple constructor test
     * 
     * @input constructor parameters : angle AOL, threshold and max check value
     * 
     * @output an AOLDetector
     * 
     * @testPassCriteria the {@link AOLDetector} is successfully created
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public final void testAOLDetectorCtor2() {
        final AOLDetector detector = new AOLDetector(MathLib.toRadians(10), V, gcrf, 2, 0.01);
        // The constructor did not crash...
        Assert.assertNotNull(detector);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_AOL_DETECTOR}
     * 
     * @testedMethod {@link AOLDetector#AOLDetector(double, PositionAngle, Frame, double, double, Action)}
     * 
     * @description simple constructor test
     * 
     * @input constructor parameters : angle AOL, threshold and max check value and STOP Action
     * 
     * @output an AOLDetector
     * 
     * @testPassCriteria the {@link AOLDetector} is successfully created
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public final void testAOLDetectorCtor3() {
        final AOLDetector detector = new AOLDetector(MathLib.toRadians(10), V, gcrf, 2, 0.01, Action.STOP);
        // Test getter
        Assert.assertEquals(MathLib.toRadians(10), detector.getAOL());
        Assert.assertEquals(V, detector.getAOLType());
        Assert.assertEquals(gcrf, detector.getAOLFrame());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_AOL_DETECTOR}
     * 
     * @testedMethod {@link AOLDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * 
     * @description tests {@link AOLDetector#eventOccurred(SpacecraftState, boolean, boolean)}
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
        final AOLDetector detector = new AOLDetector(2.5, V, gcrf);
        final SpacecraftState state = new SpacecraftState(orbit);
        // eventOccurred() is called:
        final Action rez = detector.eventOccurred(state, false, true);
        Assert.assertEquals(Action.STOP, rez);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_AOL_DETECTOR}
     * 
     * @testedMethod {@link AOLDetector#g(SpacecraftState)}
     * 
     * @description tests {@link AOLDetector#g(SpacecraftState)} propagating an elliptical
     *              orbit during one period.
     * 
     * @input constructor and g parameters
     * 
     * @output g output
     * 
     * @testPassCriteria g is zero when the AOL is equal to a predetermined value
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

        // detects the position angle:
        final AOLDetector detector = new AOLDetector(2 * FastMath.PI, V, gcrf);
        propagator.addEventDetector(detector);
        final SpacecraftState curState = propagator.propagate(iniDate.shiftedBy(period));
        final CircularOrbit outputOrbit = new CircularOrbit(curState.getOrbit());
        // bad precision:
        Assert.assertEquals(2 * FastMath.PI,
            outputOrbit.getAlphaV(), 1E-9);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_AOL_DETECTOR}
     * 
     * @testedMethod {@link AOLDetector#g(SpacecraftState)}
     * 
     * @description tests {@link AOLDetector#g(SpacecraftState)} propagating an elliptical
     *              orbit during one period.
     * 
     * @input constructor and g parameters
     * 
     * @output g output
     * 
     * @testPassCriteria g is zero when the AOL is equal to a predetermined value
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

        // detects the position angle:
        final AOLDetector detector = new AOLDetector(FastMath.PI * 1.5, V, gcrf);
        propagator.addEventDetector(detector);
        final SpacecraftState curState = propagator.propagate(iniDate.shiftedBy(period));
        final CircularOrbit outputOrbit = new CircularOrbit(curState.getOrbit());
        Assert.assertEquals(FastMath.PI * 1.5,
            outputOrbit.getAlphaV(), Utils.epsilonTest);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_AOL_DETECTOR}
     * 
     * @testedMethod {@link AOLDetector#g(SpacecraftState)}
     * 
     * @description tests {@link AOLDetector#g(SpacecraftState)} propagating an non-equatorial
     *              orbit (CircularOrbit type) during one period.
     * 
     * @input a CircularOrbit instance and a propagator
     * 
     * @output propagation outputs
     * 
     * @testPassCriteria g is zero when the AOL is equal to a predetermined value
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @Test
    public final void testCircularNonEquatorialOrbit() throws PatriusException {
        final double period = circularNonEquatOrbit.getKeplerianPeriod();
        final Propagator propagator = new KeplerianPropagator(circularNonEquatOrbit);

        // detects the position angle:
        final AOLDetector detector = new AOLDetector(MathLib.toRadians(119.5), V, gcrf);
        propagator.addEventDetector(detector);
        final SpacecraftState curState = propagator.propagate(iniDate.shiftedBy(period));
        final CircularOrbit outputOrbit = new CircularOrbit(curState.getOrbit());
        Assert.assertEquals(MathLib.toRadians(119.5),
            outputOrbit.getAlphaV(), Utils.epsilonTest);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_AOL_DETECTOR}
     * 
     * @testedMethod {@link AOLDetector#g(SpacecraftState)}
     * 
     * @description tests {@link AOLDetector#g(SpacecraftState)} propagating an equatorial
     *              orbit (CircularOrbit type) during one period.
     * 
     * @input a CircularOrbit instance and a propagator
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
    public final void testCircularEquatorialOrbit() throws PatriusException {
        final double period = circularEquatOrbit.getKeplerianPeriod();
        final Propagator propagator = new KeplerianPropagator(circularEquatOrbit);

        // detects the position angle:
        final AOLDetector detector = new AOLDetector(MathLib.toRadians(19.5), V, gcrf);
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
     * @testedFeature {@link features#VALIDATE_AOL_DETECTOR}
     * 
     * @testedMethod {@link AOLDetector#g(SpacecraftState)}
     * 
     * @description tests {@link AOLDetector#g(SpacecraftState)} propagating an non-equatorial
     *              orbit (EquinoctialOrbit type) during one period.
     * 
     * @input a EquinoctialOrbit instance and a propagator
     * 
     * @output propagation outputs
     * 
     * @testPassCriteria g is zero when the AOL is equal to a predetermined value
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @Test
    public final void testEquinoctialNonEquatorialOrbit() throws PatriusException {
        final double period = equinoctialNonEquatOrbit.getKeplerianPeriod();
        final Propagator propagator = new KeplerianPropagator(equinoctialNonEquatOrbit);

        // detects the position angle:
        final AOLDetector detector = new AOLDetector(MathLib.toRadians(253.6), V, gcrf);
        propagator.addEventDetector(detector);
        final SpacecraftState curState = propagator.propagate(iniDate.shiftedBy(period));
        final CircularOrbit outputOrbit = new CircularOrbit(curState.getOrbit());
        Assert.assertEquals(MathLib.toRadians(253.6),
            outputOrbit.getAlphaV(), Utils.epsilonTest);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_AOL_DETECTOR}
     * 
     * @testedMethod {@link AOLDetector#g(SpacecraftState)}
     * 
     * @description tests {@link AOLDetector#g(SpacecraftState)} propagating an equatorial
     *              orbit (EquinoctialOrbit type) during one period.
     * 
     * @input a EquinoctialOrbit instance and a propagator
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
    public final void testEquinoctialEquatorialOrbit() throws PatriusException {
        final double period = equinoctialEquatOrbit.getKeplerianPeriod();
        final Propagator propagator = new KeplerianPropagator(equinoctialEquatOrbit);

        // detects the position angle:
        final AOLDetector detector = new AOLDetector(MathLib.toRadians(53.6), V, gcrf);
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
     * @testedFeature {@link features#VALIDATE_AOL_DETECTOR}
     * 
     * @testedMethod {@link AOLDetector#g(SpacecraftState)}
     * 
     * @description tests g(SpacecraftState) propagating an elliptic
     *              orbit during one period with a numerical propagator.
     * 
     * @input constructor parameters and a numerical propagator
     * 
     * @output the spacecraft state when the event is detected
     * 
     * @testPassCriteria the AOL event is properly detected.
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
        final AOLDetector detector = new AOLDetector(MathLib.toRadians(117.38), V, gcrf);
        propagator.addEventDetector(detector);
        final SpacecraftState curState = propagator.propagate(iniDate.shiftedBy(period));
        final CircularOrbit finalOrbit = new CircularOrbit(curState.getOrbit());
        Assert.assertEquals(MathLib.toRadians(117.38), finalOrbit.getAlphaV(), Utils.epsilonTest);
    }

    /**
     * @testType UT
     * 
     * 
     * @description Test the getters of a class.
     * 
     * @input the class parameters
     * 
     * @output the class parameters
     * 
     * @testPassCriteria the parameters of the class are the same in input and
     *                   output
     * 
     * @referenceVersion 4.1
     * 
     * @nonRegressionVersion 4.1
     */
    @Test
    public void testGetters() {
        final double angle = MathLib.toRadians(20);
        final PositionAngle pos = PositionAngle.ECCENTRIC;
        final Frame frame = FramesFactory.getGCRF();
        final AOLDetector detector = new AOLDetector(angle, pos, frame);
        final AOLDetector detector2 = (AOLDetector) detector.copy();
        Assert.assertEquals(angle, detector2.getAOL(), 0);
        Assert.assertEquals(pos, detector2.getAOLType());
        Assert.assertEquals(frame.getName(), detector2.getAOLFrame().getName());
        Assert.assertEquals(Action.STOP, detector2.getAction());
    }
}
