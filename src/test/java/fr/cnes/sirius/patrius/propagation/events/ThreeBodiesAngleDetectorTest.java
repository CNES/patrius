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
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration de la gestion des attractions gravitationnelles dans le propagateur
 * VERSION:4.11:DM:DM-3256:22/05/2023:[PATRIUS] Suite 3246
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::DM:394:20/02/2015: Added possibility to define the action performed when event occurs
 * VERSION::DM:300:22/04/2015:Creation multi propagator
 * VERSION::DM:596:12/04/2016:Improve test coherence
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.events;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.EllipsoidPoint;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.forces.gravity.DirectBodyAttraction;
import fr.cnes.sirius.patrius.forces.gravity.NewtonianGravityModel;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.TopocentricFrame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.FirstOrderIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.propagation.events.EventDetector.Action;
import fr.cnes.sirius.patrius.propagation.events.ThreeBodiesAngleDetector.BodyOrder;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Unit tests for {@link ThreeBodiesAngleDetector}.
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id$
 * 
 * @since 1.1
 * 
 */
public class ThreeBodiesAngleDetectorTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Validate the three bodies angle detector
         * 
         * @featureDescription Validate the three bodies angle detector
         * 
         * @coveredRequirements DV-EVT_120, DV-EVT_50
         */
        VALIDATE_THREE_BODIES_ANGLE_DETECTOR,

        /**
         * @featureTitle Validate the three bodies angle detector
         * 
         * @featureDescription Validate the three bodies angle detector
         */
        VALIDATE_MULTI_THREE_BODIES_ANGLE_DETECTOR
    }

    /** A Cartesian orbit used for the tests. */
    private static CartesianOrbit orbit;

    /** A state used for the tests. */
    private static SpacecraftState state;

    /** Initial propagator date. */
    private static AbsoluteDate iniDate;

    /** Sun coordinates provider. */
    private static PVCoordinatesProvider sun;

    /** Sun coordinates provider. */
    private static PVCoordinatesProvider earth;

    /** Sun coordinates provider. */
    private static PVCoordinatesProvider moon;

    /**
     * Setup for all unit tests in the class. Provides an {@link Orbit}.
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @BeforeClass
    public static void setUpBeforeClass() throws PatriusException {
        // Orekit initialization
        Utils.setDataRoot("regular-dataCNES-2003");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));

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

        final Vector3D issPos = new Vector3D(ix, iy, iz);
        final Vector3D issVit = new Vector3D(ivx, ivy, ivz);
        final PVCoordinates pvCoordinates = new PVCoordinates(issPos, issVit);
        orbit = new CartesianOrbit(pvCoordinates, FramesFactory.getEME2000(), iniDate, mu);
        state = new SpacecraftState(orbit);

        sun = CelestialBodyFactory.getSun();
        earth = CelestialBodyFactory.getEarth();
        moon = CelestialBodyFactory.getMoon();
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_THREE_BODIES_ANGLE_DETECTOR}
     * 
     * @testedMethod {@link ThreeBodiesAngleDetector#ThreeBodiesAngleDetector(PVCoordinatesProvider, PVCoordinatesProvider, PVCoordinatesProvider, double) }
     * 
     * @description simple constructor test
     * 
     * @input constructor parameters: three PV coordinates providers, angle
     * 
     * @output a {@link ThreeBodiesAngleDetector}
     * 
     * @testPassCriteria the {@link ThreeBodiesAngleDetector} is successfully created
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void testThreeBodiesAngleDetectorCtor1() {
        final ThreeBodiesAngleDetector detector = new ThreeBodiesAngleDetector(sun, moon, earth, 2.0123);
        // The constructor did not crash...
        Assert.assertNotNull(detector);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_THREE_BODIES_ANGLE_DETECTOR}
     * 
     * @testedMethod {@link ThreeBodiesAngleDetector#ThreeBodiesAngleDetector(PVCoordinatesProvider, PVCoordinatesProvider, PVCoordinatesProvider, double, double, double) }
     * 
     * @description simple constructor test
     * 
     * @input constructor parameters : three PV coordinates providers, angle, maxcheck and threshold values
     * 
     * @output a {@link ThreeBodiesAngleDetector}
     * 
     * @testPassCriteria the {@link ThreeBodiesAngleDetector} is successfully created
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void testThreeBodiesAngleDetectorCtor2() {
        final ThreeBodiesAngleDetector detector = new ThreeBodiesAngleDetector(sun, moon, earth, 4.0123, 600, 0.001);
        // The constructor did not crash...
        Assert.assertNotNull(detector);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_THREE_BODIES_ANGLE_DETECTOR}
     * 
     * @testedMethod {@link ThreeBodiesAngleDetector#ThreeBodiesAngleDetector(PVCoordinatesProvider, PVCoordinatesProvider, PVCoordinatesProvider, double, double, double, Action) }
     * 
     * @description simple constructor test
     * 
     * @input constructor parameters : three PV coordinates providers, angle, maxcheck and threshold values and STOP
     *        Action
     * 
     * @output a {@link ThreeBodiesAngleDetector}
     * 
     * @testPassCriteria the {@link ThreeBodiesAngleDetector} is successfully created
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testThreeBodiesAngleDetectorCtor3() throws PatriusException {
        final ThreeBodiesAngleDetector detector = new ThreeBodiesAngleDetector(sun, moon, earth, 4.0123, 600, 0.001,
            Action.STOP);
        final ThreeBodiesAngleDetector detector3 = (ThreeBodiesAngleDetector) detector.copy();
        final double angle = 4.0123 - MathLib.floor(4.0123 / FastMath.PI) * FastMath.PI;
        // Test getters
        Assert.assertEquals(sun, detector3.getFirstBody());
        Assert.assertEquals(moon, detector3.getSecondBody());
        Assert.assertEquals(earth, detector3.getThirdBody());
        Assert.assertEquals(angle, detector3.getAlignmentAngle(), Utils.epsilonTest);
        // sets up the orbits:
        final double mu = CelestialBodyFactory.getEarth().getGM();
        // first orbit:
        final double a1 = 8000000;
        final Orbit orbit1 = new KeplerianOrbit(a1, 0.01, 0.2, 0, 0, FastMath.PI, PositionAngle.MEAN,
            FramesFactory.getGCRF(), iniDate, mu);
        // second orbit:
        final double a2 = 8200000;
        final Orbit orbit2 = new KeplerianOrbit(a2, 0.005, 0.3, 0, 0, 0, PositionAngle.MEAN, FramesFactory.getGCRF(),
            iniDate, mu);
        // detects the following angle between satellite 1 - Earth - satellite 2:
        final double angle2 = FastMath.PI / 2;
        final ThreeBodiesAngleDetector detector2 = new ThreeBodiesAngleDetector(orbit1,
            CelestialBodyFactory.getEarth(),
            orbit2, angle2);
        // Test getters
        Assert.assertEquals(orbit1, detector2.getFirstBody());
        Assert.assertEquals(CelestialBodyFactory.getEarth(), detector2.getSecondBody());
        Assert.assertEquals(orbit2, detector2.getThirdBody());
        Assert.assertEquals(angle2, detector2.getAlignmentAngle(), Utils.epsilonTest);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_THREE_BODIES_ANGLE_DETECTOR}
     * 
     * @testedMethod {@link ThreeBodiesAngleDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * 
     * @description tests {@link ThreeBodiesAngleDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * 
     * @input constructor and eventOccured parameters
     * 
     * @output eventOccured outputs
     * 
     * @testPassCriteria eventOccured returns the expected value
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
        final ThreeBodiesAngleDetector detector = new ThreeBodiesAngleDetector(sun, moon, earth, 0.25);
        // Distance is decreasing
        Action rez = detector.eventOccurred(state, false, true);
        Assert.assertEquals(Action.STOP, rez);
        // Distance is increasing
        rez = detector.eventOccurred(state, true, true);
        Assert.assertEquals(Action.STOP, rez);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_THREE_BODIES_ANGLE_DETECTOR}
     * 
     * @testedMethod {@link ThreeBodiesAngleDetector#g(SpacecraftState)}
     * @testedMethod {@link ThreeBodiesAngleDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * 
     * @description propagates an orbit and stops the propagation when the angle between the earth, the sun and the moon
     *              is equal to a predetermined value
     * 
     * @input constructor parameters and a propagator
     * 
     * @output resulting SpacecraftState from the propagation
     * 
     * @testPassCriteria check that when the propagation stops, the angle between the three celestial bodies is equal to
     *                   the given angle.
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @Test
    public void testThreeCelestialBodies() throws PatriusException {
        // sets up the propagator and the detector:
        final double expectedAngle = MathLib.toRadians(70);
        final ThreeBodiesAngleDetector detector = new ThreeBodiesAngleDetector(sun, earth, moon, expectedAngle);
        final KeplerianPropagator propagator = new KeplerianPropagator(orbit);
        propagator.addEventDetector(detector);
        final SpacecraftState finalState = propagator.propagate(iniDate.shiftedBy(1000000));

        // computes the bodies positions:
        final Vector3D sunPosition = sun.getPVCoordinates(finalState.getDate(), finalState.getFrame()).getPosition();
        final Vector3D earthPosition = earth.getPVCoordinates(finalState.getDate(), finalState.getFrame())
            .getPosition();
        final Vector3D moonPosition = moon.getPVCoordinates(finalState.getDate(), finalState.getFrame()).getPosition();
        // computes the vectors between the positions:
        final Vector3D earthSun = sunPosition.subtract(earthPosition);
        final Vector3D earthMoon = moonPosition.subtract(earthPosition);
        final double actualAngle = Vector3D.angle(earthSun, earthMoon);
        Assert.assertEquals(expectedAngle, actualAngle, Utils.epsilonTest);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_THREE_BODIES_ANGLE_DETECTOR}
     * 
     * @testedMethod {@link ThreeBodiesAngleDetector#g(SpacecraftState)}
     * @testedMethod {@link ThreeBodiesAngleDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * 
     * @description detects the angle between two orbits and the Earth
     * 
     * @input a propagator, two {@link KeplerianOrbit}
     * 
     * @output resulting SpacecraftState from the propagation
     * 
     * @testPassCriteria the angle between the position of satellite 1 - Earth - satellite 2 should be equal to PI/2
     *                   when the three bodies angle event is detected
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @Test
    public void testTwoOrbits() throws PatriusException {
        // sets up the orbits:
        final double mu = CelestialBodyFactory.getEarth().getGM();
        // first orbit:
        final double a1 = 8000000;
        final Orbit orbit1 = new KeplerianOrbit(a1, 0.01, 0.2, 0, 0, FastMath.PI, PositionAngle.MEAN,
            FramesFactory.getGCRF(), iniDate, mu);
        // second orbit:
        final double a2 = 8200000;
        final Orbit orbit2 = new KeplerianOrbit(a2, 0.005, 0.3, 0, 0, 0, PositionAngle.MEAN, FramesFactory.getGCRF(),
            iniDate, mu);

        // set up the propagator:
        final KeplerianPropagator propagator = new KeplerianPropagator(orbit1);
        // detects the following angle between satellite 1 - Earth - satellite 2:
        final double angle = FastMath.PI / 2;
        final ThreeBodiesAngleDetector detector = new ThreeBodiesAngleDetector(orbit1, CelestialBodyFactory.getEarth(),
            orbit2, angle);
        propagator.addEventDetector(detector);
        // the propagation should stop when the angle is reached:
        final SpacecraftState curState = propagator.propagate(iniDate.shiftedBy(100000));

        // computes the satellites positions:
        final Vector3D position1 = orbit1.getPVCoordinates(curState.getDate(), FramesFactory.getGCRF()).getPosition();
        final Vector3D position2 = orbit2.getPVCoordinates(curState.getDate(), FramesFactory.getGCRF()).getPosition();
        // computes the angle between the two satellites:
        final double actualAngle = Vector3D.angle(position1, position2);
        // verifies that this angle is = PI / 2:
        Assert.assertEquals(angle, actualAngle, Utils.epsilonTest);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_THREE_BODIES_ANGLE_DETECTOR}
     * 
     * @testedMethod {@link ThreeBodiesAngleDetector#g(SpacecraftState)}
     * @testedMethod {@link ThreeBodiesAngleDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * 
     * @description detects the angle between two satellites orbits and a station, the satellites orbits are propagated
     *              with numerical propagators, the three bodies angle detector is associated to one of them
     * 
     * @input two numerical propagators and a topocentric frame as the station pv coordinates provider
     * 
     * @output resulting SpacecraftState from the propagation
     * 
     * @testPassCriteria the angle between the position of satellite 1 - station - satellite 2 should be equal to PI/6
     *                   when the three bodies angle event is detected
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @Test
    public void testSatelliteStationSatellite() throws PatriusException {
        // sets up the orbits:
        final double mu = CelestialBodyFactory.getEarth().getGM();
        // first orbit:
        final double a1 = 8000000;
        final Orbit orbit1 = new KeplerianOrbit(a1, 0.01, 0.2, 0, 0, FastMath.PI, PositionAngle.MEAN,
            FramesFactory.getGCRF(), iniDate, mu);
        // second orbit:
        final double a2 = 8200000;
        final Orbit orbit2 = new KeplerianOrbit(a2, 0.005, 0.3, 0, 0, 0, PositionAngle.MEAN, FramesFactory.getGCRF(),
            iniDate, mu);
        // station :
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
            Constants.WGS84_EARTH_FLATTENING, FramesFactory.getITRF());
        final EllipsoidPoint point = new EllipsoidPoint(earth, earth.getLLHCoordinatesSystem(),
            MathLib.toRadians(60), MathLib.toRadians(20), 0, "");
        final TopocentricFrame topo = new TopocentricFrame(point, "station");

        // integrator
        final OrbitType type = OrbitType.CARTESIAN;
        final double dP = 1.0e-6;
        final double[][] tol = NumericalPropagator.tolerances(dP, orbit1, type);
        final FirstOrderIntegrator integrator1 = new DormandPrince853Integrator(0.1, 900., tol[0], tol[1]);
        final FirstOrderIntegrator integrator2 = new DormandPrince853Integrator(0.1, 900., tol[0], tol[1]);

        // set up the propagator2:
        final NumericalPropagator propagator1 = new NumericalPropagator(integrator1);
        propagator1.resetInitialState(new SpacecraftState(orbit1));
        propagator1.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(propagator1
            .getInitialState().getMu())));

        final SpacecraftState initialState2 = new SpacecraftState(orbit2);
        final NumericalPropagator propagator2 = new NumericalPropagator(integrator2, initialState2.getFrame(), type,
            PositionAngle.TRUE);
        propagator2.resetInitialState(initialState2);
        propagator2.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(propagator2
            .getInitialState().getMu())));

        // detects the following angle between satellite 1 - station - satellite 2:
        final double angle = FastMath.PI / 6;
        final ThreeBodiesAngleDetector detector1 = new ThreeBodiesAngleDetector(propagator1, topo, BodyOrder.THIRD,
            angle);

        // add two detectors to the propagator only to complete the code coverage!
        final ThreeBodiesAngleDetector detector2 = new ThreeBodiesAngleDetector(propagator1, topo, BodyOrder.FIRST,
            angle){
            private static final long serialVersionUID = 2855923874276621070L;

            @Override
            public Action eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward) {
                return Action.CONTINUE;
            }
        };
        final ThreeBodiesAngleDetector detector3 = new ThreeBodiesAngleDetector(propagator1, topo, BodyOrder.SECOND,
            angle){
            private static final long serialVersionUID = -2051060260822247565L;

            @Override
            public Action eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward) {
                return Action.CONTINUE;
            }
        };

        // Asserts on bodies
        // detector 1 :
        Assert.assertEquals(detector1.getFirstBody(), propagator1);
        Assert.assertEquals(detector1.getSecondBody(), topo);
        Assert.assertEquals(detector1.getThirdBody(), null);
        // detector 2 :
        Assert.assertEquals(detector2.getFirstBody(), null);
        Assert.assertEquals(detector2.getSecondBody(), propagator1);
        Assert.assertEquals(detector2.getThirdBody(), topo);
        // detector 3 :
        Assert.assertEquals(detector3.getFirstBody(), propagator1);
        Assert.assertEquals(detector3.getSecondBody(), null);
        Assert.assertEquals(detector3.getThirdBody(), topo);

        propagator2.addEventDetector(detector1);
        propagator2.addEventDetector(detector2);
        propagator2.addEventDetector(detector3);

        // the propagation should stop when the angle is reached:
        final SpacecraftState curState = propagator2.propagate(iniDate.shiftedBy(100000));

        // computes the satellites positions:
        final Vector3D position1 = propagator1.getPVCoordinates(curState.getDate(), FramesFactory.getGCRF())
            .getPosition()
            .subtract(topo.getPVCoordinates(curState.getDate(), FramesFactory.getGCRF()).getPosition());
        final Vector3D position2 = curState.getPVCoordinates().getPosition()
            .subtract(topo.getPVCoordinates(curState.getDate(), FramesFactory.getGCRF()).getPosition());
        // computes the angle between the two satellites:
        final double actualAngle = Vector3D.angle(position1, position2);
        // verifies that this angle is = PI / 6:
        Assert.assertEquals(angle, actualAngle, Utils.epsilonTest);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_MULTI_THREE_BODIES_ANGLE_DETECTOR}
     * 
     * @testedMethod {@link ThreeBodiesAngleDetector#g(Map)}
     * @testedMethod {@link ThreeBodiesAngleDetector#g(SpacecraftState)}
     * @testedMethod {@link ThreeBodiesAngleDetector#getInSpacecraftId1()}
     * @testedMethod {@link ThreeBodiesAngleDetector#getInSpacecraftId2()}
     * @testedMethod {@link ThreeBodiesAngleDetector#getInSpacecraftId3()}
     * 
     * @description Test exceptions raised by g() method. Test for coverage purposes
     * 
     * @testPassCriteria exception raised
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testExceptions() throws PatriusException {
        /** Sat id 1 */
        final String ID1 = "state1";
        /** Sat id 2 */
        final String ID2 = "state2";
        /** Sat id 3 */
        final String ID3 = "state3";

        final ThreeBodiesAngleDetector detector1 = new ThreeBodiesAngleDetector(orbit, orbit, orbit, 0.);

        final Map<String, SpacecraftState> states = new HashMap<>();
        states.put(ID1, state);
        states.put(ID2, state.shiftedBy(10.));
        states.put(ID3, state.shiftedBy(20.));

        boolean testOk = false;
        try {
            detector1.g(states);
            Assert.fail();
        } catch (final PatriusException e) {
            testOk = true;
            Assert.assertEquals(PatriusMessages.MONO_MULTI_DETECTOR.getSourceString(), e.getMessage());
        }
        Assert.assertTrue(testOk);

        final ThreeBodiesAngleDetector detector2 = new ThreeBodiesAngleDetector(ID1, ID2, ID3, 0.,
            AbstractDetector.DEFAULT_MAXCHECK, AbstractDetector.DEFAULT_THRESHOLD, Action.STOP);

        testOk = false;
        try {
            detector2.g(state);
            Assert.fail();
        } catch (final PatriusException e) {
            testOk = true;
            Assert.assertEquals(PatriusMessages.MONO_MULTI_DETECTOR.getSourceString(), e.getMessage());
        }
        Assert.assertTrue(testOk);

        // Coverage tests
        // ==============
        // init() method
        detector2.init(states, iniDate);

        // eventOccured method
        Assert.assertEquals(Action.STOP, detector2.eventOccurred(states, true, true));

        // reset states method
        Assert.assertEquals(states.hashCode(), detector2.resetStates(states).hashCode());

        // g() method
        Assert.assertNotNull(detector2.g(states));

        // Id getters
        Assert.assertEquals(ID1, detector2.getInSpacecraftId1());
        Assert.assertEquals(ID2, detector2.getInSpacecraftId2());
        Assert.assertEquals(ID3, detector2.getInSpacecraftId3());
    }
}
