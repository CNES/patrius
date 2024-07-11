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
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::DM:227:02/10/2014:Merged eclipse detectors and added eclipse detector by lighting ratio
 * VERSION::FA:261:13/10/2014:JE V2.2 corrections (move IDirection in package attitudes.directions)
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.events;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.directions.ConstantVectorDirection;
import fr.cnes.sirius.patrius.attitudes.directions.IDirection;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.propagation.events.EventDetector.Action;
import fr.cnes.sirius.patrius.propagation.events.EventsLogger.LoggedEvent;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Unit tests for {@link EclipseDetector}.
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id: GenericEclipseDetectorTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.2
 * 
 */
public class GenericEclipseDetectorTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Validate the generic eclipse detector
         * 
         * @featureDescription Validate the generic eclipse detector
         * 
         * @coveredRequirements DV-EVT_140
         */
        VALIDATE_GENERIC_ECLIPSE_DETECTOR
    }

    /** The Sun. */
    private static CelestialBody sun;

    /** The Earth. */
    private static CelestialBody earth;

    /** The Moon. */
    private static CelestialBody moon;

    /** A generic orbit. */
    private static KeplerianOrbit orbit;

    /** Initial propagation date. */
    private static AbsoluteDate date;

    /**
     * Setup for all unit tests in the class.
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @BeforeClass
    public static void setUpBeforeClass() throws PatriusException {
        // Orekit initialisation
        Utils.setDataRoot("regular-dataCNES-2003");
        FramesFactory.setConfiguration(fr.cnes.sirius.patrius.Utils.getIERS2003Configuration(true));
        // Celestial bodies initialisation:
        sun = CelestialBodyFactory.getSun();
        earth = CelestialBodyFactory.getEarth();
        moon = CelestialBodyFactory.getMoon();
        // Orbit initialisation:
        date = new AbsoluteDate("2012-04-01T12:00:00Z", TimeScalesFactory.getTT());
        final double mu = earth.getGM();
        orbit = new KeplerianOrbit(12500000, 0.01, MathLib.toRadians(8.5), 0, 0,
            FastMath.PI / 2, PositionAngle.TRUE, FramesFactory.getGCRF(), date, mu);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_GENERIC_ECLIPSE_DETECTOR}
     * 
     * @testedMethod {@link EclipseDetector#GenericEclipseDetector(PVCoordinatesProvider, double, PVCoordinatesProvider, double, boolean, double, double)}
     * 
     * @description Simple constructor test.
     * 
     * @input Constructor parameters: threshold and max check value, two {@link PVCoordinatesProvider},
     *        the radius of the two celestial bodies and the total/partial eclipse flag.
     * 
     * @output a {@link EclipseDetector} instance
     * 
     * @testPassCriteria the instance is successfully created
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 2.3
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public final void testGenericEclipseDetectorCtor1() throws PatriusException {
        final EclipseDetector detector = new EclipseDetector(sun, Constants.SUN_RADIUS,
            earth, Constants.GRS80_EARTH_EQUATORIAL_RADIUS, 1, 215.3, 1E-3);
        // The constructor did not crash...
        Assert.assertNotNull(detector);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_GENERIC_ECLIPSE_DETECTOR}
     * 
     * @testedMethod {@link EclipseDetector#GenericEclipseDetector(IDirection, PVCoordinatesProvider, double, double, double)}
     * 
     * @description Simple constructor test.
     * 
     * @input Constructor parameters: threshold and max check value, one {@link IDirection},
     *        one {@link PVCoordinatesProvider} and the radius of the celestial body.
     * 
     * @output a {@link EclipseDetector} instance
     * 
     * @testPassCriteria the instance is successfully created
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 2.3
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public final void testGenericEclipseDetectorCtor2() throws PatriusException {
        final ConstantVectorDirection direction = new ConstantVectorDirection(new Vector3D(1, 0, 0),
            FramesFactory.getGCRF());
        final EclipseDetector detector = new EclipseDetector(direction, earth,
            Constants.GRS80_EARTH_EQUATORIAL_RADIUS, 215.3, 1E-3);
        // The constructor did not crash...
        Assert.assertNotNull(detector);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_GENERIC_ECLIPSE_DETECTOR}
     * 
     * @testedMethod {@link EclipseDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * 
     * @description tests {@link EclipseDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * 
     * @input constructor and eventOccured parameters
     * 
     * @output eventOccured outputs
     * 
     * @testPassCriteria eventOccured returns the expected values for true and false as second parameters
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 2.3
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @Test
    public final void testEventOccurred() throws PatriusException {
        final EclipseDetector detector = new EclipseDetector(sun, Constants.SUN_RADIUS,
            earth, Constants.GRS80_EARTH_EQUATORIAL_RADIUS, 1, 215.3, 1E-3);
        final SpacecraftState state = new SpacecraftState(orbit);
        // Entering the eclipse:
        Action rez = detector.eventOccurred(state, false, true);
        Assert.assertEquals(Action.CONTINUE, rez);
        // Exiting the eclipse:
        rez = detector.eventOccurred(state, true, true);
        Assert.assertEquals(Action.STOP, rez);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_GENERIC_ECLIPSE_DETECTOR}
     * 
     * @testedMethod {@link EclipseDetector#GenericEclipseDetector(PVCoordinatesProvider, double, PVCoordinatesProvider, double, boolean, double, double)}
     * @testedMethod {@link EclipseDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * @testedMethod {@link EclipseDetector#g(SpacecraftState)}
     * 
     * @description Tests the detection of satellite eclipse events when the Moon is the occulted body
     *              and the Earth is the occulting body.
     * 
     * @input The eclipse detectors constructor parameters, a propagator and an events logger.
     * 
     * @output a list of {@link LoggedEvent} logged during the propagation
     * 
     * @testPassCriteria the number of detected events should be the expected number and for every
     *                   detected event the re-implemented g function should be zero.
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 2.3
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public final void testMoonEarthEclipse() throws PatriusException {
        final double period = orbit.getKeplerianPeriod();
        final Propagator propagator = new KeplerianPropagator(orbit);
        // creates the eclipse detectors overriding the eventOccurred function (the propagation does not stop when an
        // event is detected):
        final EclipseDetector detectorUmbra = new EclipseDetector(moon, Constants.MOON_EQUATORIAL_RADIUS,
            earth, Constants.GRS80_EARTH_EQUATORIAL_RADIUS, 0, 300, 1.E-9){

            private static final long serialVersionUID = -8866653005160082402L;

            @Override
            public
                    Action
                    eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                           throws PatriusException {
                return Action.CONTINUE;
            }
        };
        final EclipseDetector detectorPenumbra = new EclipseDetector(moon, Constants.MOON_EQUATORIAL_RADIUS,
            earth, Constants.GRS80_EARTH_EQUATORIAL_RADIUS, 1, 300, 1.E-9){

            private static final long serialVersionUID = 5222045269157847237L;

            @Override
            public
                    Action
                    eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                           throws PatriusException {
                return Action.CONTINUE;
            }
        };
        // propagates the orbit using an events logger:
        final EventsLogger logger = new EventsLogger();
        propagator.addEventDetector(logger.monitorDetector(detectorUmbra));
        propagator.addEventDetector(logger.monitorDetector(detectorPenumbra));
        propagator.propagate(date.shiftedBy(5 * period));
        // there should be 20 umbra/penumbra events occurring during 5 periods:
        Assert.assertEquals(20, logger.getLoggedEvents().size());
        // re-implement the g function for every detected eclipse event:
        for (final LoggedEvent event : logger.getLoggedEvents()) {
            // computes the satellite and celestial bodies coordinates:
            final SpacecraftState state = event.getState();
            final Vector3D satPV = state.getPVCoordinates(state.getFrame()).getPosition();
            final Vector3D earthPV = earth.getPVCoordinates(state.getDate(), state.getFrame()).getPosition();
            final Vector3D moonPV = moon.getPVCoordinates(state.getDate(), state.getFrame()).getPosition();
            final Vector3D satMoon = moonPV.subtract(satPV);
            final Vector3D satEarth = earthPV.subtract(satPV);
            final double angle = Vector3D.angle(satMoon, satEarth);
            final double moonTangent = MathLib.asin(Constants.MOON_EQUATORIAL_RADIUS / satMoon.getNorm());
            final double earthTangent = MathLib.asin(Constants.GRS80_EARTH_EQUATORIAL_RADIUS / satEarth.getNorm());
            final boolean isTotalEclipse = ((EclipseDetector) event.getEventDetector()).isTotalEclipse();
            if (isTotalEclipse) {
                // umbra events:
                Assert.assertEquals(0, angle + moonTangent - earthTangent, Utils.epsilonTest);
            } else {
                // penumbra events:
                Assert.assertEquals(0, angle - moonTangent - earthTangent, Utils.epsilonTest);
            }
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_GENERIC_ECLIPSE_DETECTOR}
     * 
     * @testedMethod {@link EclipseDetector#GenericEclipseDetector(PVCoordinatesProvider, double, PVCoordinatesProvider, double, boolean, double, double)}
     * @testedMethod {@link EclipseDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * @testedMethod {@link EclipseDetector#g(SpacecraftState)}
     * 
     * @description Tests the detection of satellite eclipse events when the Sun is the occulted body
     *              and the Earth is the occulting body.
     * 
     * @input The eclipse detectors constructor parameters, a propagator and an events logger.
     * 
     * @output a list of {@link LoggedEvent} logged during the propagation
     * 
     * @testPassCriteria the number of detected events should be the expected number and for every
     *                   detected event the re-implemented g function should be zero.
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 2.3
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public final void testSunEarthEclipse() throws PatriusException {
        final double period = orbit.getKeplerianPeriod();
        final Propagator propagator = new KeplerianPropagator(orbit);
        // creates the eclipse detectors overriding the eventOccurred function (the propagation does not stop when an
        // event is detected):
        final EclipseDetector detectorUmbra = new EclipseDetector(sun, Constants.SUN_RADIUS,
            earth, Constants.GRS80_EARTH_EQUATORIAL_RADIUS, 0, 300, 1.E-9){

            private static final long serialVersionUID = 5331315185138132654L;

            @Override
            public
                    Action
                    eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                           throws PatriusException {
                return Action.CONTINUE;
            }
        };
        final EclipseDetector detectorPenumbra = new EclipseDetector(sun, Constants.SUN_RADIUS,
            earth, Constants.GRS80_EARTH_EQUATORIAL_RADIUS, 1, 300, 1.E-9){

            private static final long serialVersionUID = 3669843886542581566L;

            @Override
            public
                    Action
                    eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                           throws PatriusException {
                return Action.CONTINUE;
            }
        };
        // propagates the orbit using an events logger:
        final EventsLogger logger = new EventsLogger();
        propagator.addEventDetector(logger.monitorDetector(detectorUmbra));
        propagator.addEventDetector(logger.monitorDetector(detectorPenumbra));
        propagator.propagate(date.shiftedBy(4 * period));
        // there should be 16 umbra/penumbra events occurring during 4 periods:
        Assert.assertEquals(16, logger.getLoggedEvents().size());
        // re-implement the g function for every detected eclipse event:
        for (final LoggedEvent event : logger.getLoggedEvents()) {
            // computes the satellite and celestial bodies coordinates:
            final SpacecraftState state = event.getState();
            final Vector3D satPV = state.getPVCoordinates(state.getFrame()).getPosition();
            final Vector3D earthPV = earth.getPVCoordinates(state.getDate(), state.getFrame()).getPosition();
            final Vector3D sunPV = sun.getPVCoordinates(state.getDate(), state.getFrame()).getPosition();
            final Vector3D satSun = sunPV.subtract(satPV);
            final Vector3D satEarth = earthPV.subtract(satPV);
            final double angle = Vector3D.angle(satSun, satEarth);
            final double sunTangent = MathLib.asin(Constants.SUN_RADIUS / satSun.getNorm());
            final double earthTangent = MathLib.asin(Constants.GRS80_EARTH_EQUATORIAL_RADIUS / satEarth.getNorm());
            final boolean isTotalEclipse = ((EclipseDetector) event.getEventDetector()).isTotalEclipse();
            if (isTotalEclipse) {
                // umbra events:
                Assert.assertEquals(0, angle + sunTangent - earthTangent, Utils.epsilonTest);
            } else {
                // penumbra events:
                Assert.assertEquals(0, angle - sunTangent - earthTangent, Utils.epsilonTest);
            }
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_GENERIC_ECLIPSE_DETECTOR}
     * 
     * @testedMethod {@link EclipseDetector#GenericEclipseDetector(PVCoordinatesProvider, double, PVCoordinatesProvider, double, boolean, double, double)}
     * @testedMethod {@link EclipseDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * @testedMethod {@link EclipseDetector#g(SpacecraftState)}
     * 
     * @description Tests the detection of satellite eclipse events when Venus is the occulted body
     *              and the Earth is the occulting body.
     * 
     * @input The eclipse detectors constructor parameters, a propagator and an events logger.
     * 
     * @output a list of {@link LoggedEvent} logged during the propagation
     * 
     * @testPassCriteria the number of detected events should be the expected number and for every
     *                   detected event the re-implemented g function should be zero.
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 2.3
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public final void testVenusEarthEclipse() throws PatriusException {
        final double period = orbit.getKeplerianPeriod();
        final Propagator propagator = new KeplerianPropagator(orbit);
        // creates the eclipse detectors overriding the eventOccurred function (the propagation does not stop when an
        // event is detected):
        final EclipseDetector detectorUmbra = new EclipseDetector(CelestialBodyFactory.getVenus(), 6.0518E6,
            earth, Constants.GRS80_EARTH_EQUATORIAL_RADIUS, 0, 300, 1.E-9){

            private static final long serialVersionUID = 594287606153419817L;

            @Override
            public
                    Action
                    eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                           throws PatriusException {
                return Action.CONTINUE;
            }
        };
        final EclipseDetector detectorPenumbra = new EclipseDetector(CelestialBodyFactory.getVenus(), 6.0518E6,
            earth, Constants.GRS80_EARTH_EQUATORIAL_RADIUS, 1, 300, 1.E-9){

            private static final long serialVersionUID = 3149611452596544030L;

            @Override
            public
                    Action
                    eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                           throws PatriusException {
                return Action.CONTINUE;
            }
        };
        // propagates the orbit using an events logger:
        final EventsLogger logger = new EventsLogger();
        propagator.addEventDetector(logger.monitorDetector(detectorUmbra));
        propagator.addEventDetector(logger.monitorDetector(detectorPenumbra));
        propagator.propagate(date.shiftedBy(4 * period));
        // there should be 16 umbra/penumbra events occurring during 4 periods:
        Assert.assertEquals(16, logger.getLoggedEvents().size());
        // re-implement the g function for every detected eclipse event:
        for (final LoggedEvent event : logger.getLoggedEvents()) {
            // computes the satellite and celestial bodies coordinates:
            final SpacecraftState state = event.getState();
            final Vector3D satPV = state.getPVCoordinates(state.getFrame()).getPosition();
            final Vector3D earthPV = earth.getPVCoordinates(state.getDate(), state.getFrame()).getPosition();
            final Vector3D venusPV = CelestialBodyFactory.getVenus()
                .getPVCoordinates(state.getDate(), state.getFrame()).getPosition();
            final Vector3D satVenus = venusPV.subtract(satPV);
            final Vector3D satEarth = earthPV.subtract(satPV);
            final double angle = Vector3D.angle(satVenus, satEarth);
            final double venusTangent = MathLib.asin(6.0518E6 / satVenus.getNorm());
            final double earthTangent = MathLib.asin(Constants.GRS80_EARTH_EQUATORIAL_RADIUS / satEarth.getNorm());
            final boolean isTotalEclipse = ((EclipseDetector) event.getEventDetector()).isTotalEclipse();
            if (isTotalEclipse) {
                // umbra events:
                Assert.assertEquals(0, angle + venusTangent - earthTangent, Utils.epsilonTest);
            } else {
                // penumbra events:
                Assert.assertEquals(0, angle - venusTangent - earthTangent, Utils.epsilonTest);
            }
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_GENERIC_ECLIPSE_DETECTOR}
     * 
     * @testedMethod {@link EclipseDetector#GenericEclipseDetector(PVCoordinatesProvider, double, PVCoordinatesProvider, double, boolean, double, double)}
     * @testedMethod {@link EclipseDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * @testedMethod {@link EclipseDetector#g(SpacecraftState)}
     * 
     * @description Tests the detection of satellite eclipse events when another satellite (with an
     *              approximated circular shape) is the occulted body and the Earth is the occulting body. In particular
     *              it checks that the event is not triggered when the occulted spacecraft is between the first
     *              satellite and the Earth, but
     *              only when the Earth is between the two satellites.
     * 
     * @input The eclipse detector constructor parameters, a propagator and an events logger.
     * 
     * @output a list of {@link LoggedEvent} logged during the propagation
     * 
     * @testPassCriteria the number of detected events should be the expected number and for every
     *                   detected event the re-implemented g function should be zero; moreover, no event should be
     *                   detected
     *                   if the distance between the satellite and the Earth is bigger than the distance between the
     *                   satellite
     *                   and the occulted spacecraft.
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 2.3
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public final void testTwoSatellites() throws PatriusException {
        final double period = orbit.getKeplerianPeriod();
        // computes the semi-major axis of the occulted satellite orbit in order to set its period equal to the
        // half period of the first satellite orbit:
        final double occultedA = MathLib.pow(MathLib.pow(period / (4 * FastMath.PI), 2) * earth.getGM(), 1. / 3.);
        final Propagator propagator = new KeplerianPropagator(orbit);
        // creates the eclipse detectors overriding the eventOccurred function (the propagation does not stop when an
        // event is detected):
        final KeplerianOrbit occultedSat = new KeplerianOrbit(occultedA, 0.01, MathLib.toRadians(8.5), 0, 0,
            0, PositionAngle.TRUE, FramesFactory.getGCRF(), date, earth.getGM());
        final EclipseDetector detectorUmbra = new EclipseDetector(occultedSat, 5.0,
            earth, Constants.GRS80_EARTH_EQUATORIAL_RADIUS, 0, 300, 1.E-9){

            private static final long serialVersionUID = 5331315185138132654L;

            @Override
            public
                    Action
                    eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                           throws PatriusException {
                return Action.CONTINUE;
            }
        };
        final EclipseDetector detectorPenumbra = new EclipseDetector(occultedSat, 5.0,
            earth, Constants.GRS80_EARTH_EQUATORIAL_RADIUS, 1, 300, 1.E-9){

            private static final long serialVersionUID = 3669843886542581566L;

            @Override
            public
                    Action
                    eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                           throws PatriusException {
                return Action.CONTINUE;
            }
        };
        // propagates the orbit using an events logger:
        final EventsLogger logger = new EventsLogger();
        propagator.addEventDetector(logger.monitorDetector(detectorUmbra));
        propagator.addEventDetector(logger.monitorDetector(detectorPenumbra));
        propagator.propagate(date.shiftedBy(2 * period));
        // there should be 8 umbra/penumbra events occurring during 2 periods:
        Assert.assertEquals(8, logger.getLoggedEvents().size());
        // re-implement the g function for every detected eclipse event:
        for (final LoggedEvent event : logger.getLoggedEvents()) {
            // computes the satellite and celestial bodies coordinates:
            final SpacecraftState state = event.getState();
            final Vector3D satPV = state.getPVCoordinates(state.getFrame()).getPosition();
            final Vector3D earthPV = earth.getPVCoordinates(state.getDate(), state.getFrame()).getPosition();
            final Vector3D occultedSatPV = occultedSat.getPVCoordinates(state.getDate(), state.getFrame())
                .getPosition();
            final Vector3D satOccultedSat = occultedSatPV.subtract(satPV);
            final Vector3D satEarth = earthPV.subtract(satPV);
            final double angle = Vector3D.angle(satOccultedSat, satEarth);
            final double occultedSatTangent = MathLib.asin(5.0 / satOccultedSat.getNorm());
            final double earthTangent = MathLib.asin(Constants.GRS80_EARTH_EQUATORIAL_RADIUS / satEarth.getNorm());
            final boolean isTotalEclipse = ((EclipseDetector) event.getEventDetector()).isTotalEclipse();
            if (isTotalEclipse) {
                // no umbra event is detected when the satellite is in between the Earth and the satellite whose orbit
                // is propagated.
                Assert.assertTrue(satOccultedSat.getNorm() > satEarth.getNorm());
                // umbra events:
                Assert.assertEquals(0, angle + occultedSatTangent - earthTangent, Utils.epsilonTest);
            } else {
                // no penumbra event is detected when the satellite is in between the Earth and the satellite whose
                // orbit is propagated.
                Assert.assertTrue(satOccultedSat.getNorm() > satEarth.getNorm());
                // penumbra events:
                Assert.assertEquals(0, angle - occultedSatTangent - earthTangent, Utils.epsilonTest);
            }
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_GENERIC_ECLIPSE_DETECTOR}
     * 
     * @testedMethod {@link EclipseDetector#GenericEclipseDetector(IDirection, PVCoordinatesProvider, double, double, double)}
     * @testedMethod {@link EclipseDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * @testedMethod {@link EclipseDetector#g(SpacecraftState)}
     * 
     * @description Tests the detection of satellite eclipse events when the occulted object is an inertial
     *              direction (representing for instance a star) and the occulting body is the Earth. The satellite
     *              orbit
     *              is equatorial and two eclipse detectors are added to the propagator: one associated to the direction
     *              X in the GCRF frame and the other to the direction Z.
     * 
     * @input The eclipse detector constructor parameters, a propagator and an events logger.
     * 
     * @output a list of {@link LoggedEvent} logged during the propagation
     * 
     * @testPassCriteria the number of detected events should be the expected number: two events for every
     *                   orbital period for the X-direction detector and zero events for the Z-direction detector (the
     *                   orbit
     *                   is equatorial).
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 2.3
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public final void testInertialDirection() throws PatriusException {
        final KeplerianOrbit eqOrbit = new KeplerianOrbit(7500000, 0.01, 0, 0, 0,
            FastMath.PI / 2, PositionAngle.TRUE, FramesFactory.getGCRF(), date, earth.getGM());

        final double period = eqOrbit.getKeplerianPeriod();
        final Propagator propagator = new KeplerianPropagator(eqOrbit);
        // creates the eclipse detectors overriding the eventOccurred function (the propagation does not stop when an
        // event is detected):
        final ConstantVectorDirection directionX = new ConstantVectorDirection(new Vector3D(1, 0, 0),
            FramesFactory.getGCRF());
        final EclipseDetector detectorX = new EclipseDetector(directionX, earth,
            Constants.GRS80_EARTH_EQUATORIAL_RADIUS, 300, 1.E-9){

            private static final long serialVersionUID = -8029365056528545444L;

            @Override
            public
                    Action
                    eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                           throws PatriusException {
                return Action.CONTINUE;
            }
        };
        final ConstantVectorDirection directionZ = new ConstantVectorDirection(new Vector3D(0, 0, 1),
            FramesFactory.getGCRF());
        final EclipseDetector detectorZ = new EclipseDetector(directionZ, earth,
            Constants.GRS80_EARTH_EQUATORIAL_RADIUS, 300, 1.E-9){

            private static final long serialVersionUID = -2615472964127261385L;

            @Override
            public
                    Action
                    eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                           throws PatriusException {
                return Action.CONTINUE;
            }
        };
        // propagates the orbit using an events logger:
        final EventsLogger logger = new EventsLogger();
        propagator.addEventDetector(logger.monitorDetector(detectorX));
        propagator.addEventDetector(logger.monitorDetector(detectorZ));
        propagator.propagate(date.shiftedBy(3 * period));
        // there should be 6 X events occurring during 2 periods (0 Z events):
        Assert.assertEquals(6, logger.getLoggedEvents().size());
        // re-implement the g function for every detected eclipse event:
        for (final LoggedEvent event : logger.getLoggedEvents()) {
            // computes the satellite and celestial bodies coordinates:
            final SpacecraftState state = event.getState();
            final Vector3D satPV = state.getPVCoordinates(state.getFrame()).getPosition();
            final Vector3D vectorXPV = directionX.getVector(null, state.getDate(), state.getFrame());
            final Vector3D earthPV = earth.getPVCoordinates(state.getDate(), state.getFrame()).getPosition();
            final Vector3D satX = vectorXPV;
            final Vector3D satEarth = earthPV.subtract(satPV);
            final double angle = Vector3D.angle(satX, satEarth);
            final double earthTangent = MathLib.asin(Constants.GRS80_EARTH_EQUATORIAL_RADIUS / satEarth.getNorm());
            // penumbra events:
            Assert.assertEquals(0, angle - earthTangent, Utils.epsilonTest);
        }
    }
}
