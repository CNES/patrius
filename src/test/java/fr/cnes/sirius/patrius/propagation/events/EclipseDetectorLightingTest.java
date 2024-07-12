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
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration de la gestion des attractions gravitationnelles dans le propagateur
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.9:DM:DM-3161:10/05/2022:[PATRIUS] Ajout d'une methode getNativeFrame() a l'interface PVCoordinatesProvider 
 * VERSION:4.9:DM:DM-3165:10/05/2022:[PATRIUS] Amelioration de la propagation du signal 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:227:02/10/2014:Merged eclipse detectors and added eclipse detector by lighting ratio
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.events;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.forces.gravity.DirectBodyAttraction;
import fr.cnes.sirius.patrius.forces.gravity.NewtonianGravityModel;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.nonstiff.AdaptiveStepsizeIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Unit tests for {@link EclipseDetector}, with regard to the eclipse detection by lighting ratio.
 * 
 * @author sabatinit
 * 
 * @version $Id: EclipseDetectorLightingTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 2.3
 * 
 */
public class EclipseDetectorLightingTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Validate the eclipse detector when a lighting ratio is provided
         * 
         * @featureDescription Validate the eclipse detector when a lighting ratio is provided
         * 
         * @coveredRequirements
         */
        VALIDATE_ECLIPSE_BY_LIGHTING_RATIO
    }

    /** Mu. */
    private double mu;
    /** Date. */
    private AbsoluteDate date;
    /** Propagator. */
    private NumericalPropagator propagator;
    /** Sun. */
    private PVCoordinatesProvider sun;
    /** Moon. */
    private PVCoordinatesProvider moon;
    /** Sun radius. */
    private double aeS;
    /** Moon radius. */
    private double aeM;
    /** lighting string. */
    private final String lighting = "LIGHTING";
    /** umbra string. */
    private final String umbra = "UMBRA";
    /** penumbra string. */
    private final String penumbra = "PENUMBRA";

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedMethod {@link VALIDATE_ECLIPSE_BY_LIGHTING_RATIO}
     * 
     * @description test the EclipseDetector during propagation for different values of the lighting ratio.
     *              The occulted body is the Sun and the occulting body is the Moon; the occulting body
     *              apparent radius is smaller than the occulted body one (low orbit). Sun and Moon are ideally located
     *              on the x-axis of the inertial frame, the satellite orbit is equatorial.
     * 
     * @input a low equatorial orbit, the Sun and Moon are fixed and ideally located on the x-axis
     *        of the inertial frame.
     * 
     * @output for every propagation, the spacecraft states corresponding to the following detected events:
     *         umbra, penumbra and eclipse based on lighting ratio.
     * 
     * @testPassCriteria coherence among the detected events
     * 
     * @referenceVersion 2.3
     * 
     * @nonRegressionVersion 2.3
     */
    @Test
    public void testBiggerOccultedBodyRadius() throws PatriusException {

        // Test : apparent occulted radius > apparent occulting radius
        // Low equatorial orbit:
        final KeplerianOrbit orbit = new KeplerianOrbit(6775e3, .0, .0, 0,
            0, -FastMath.PI / 2.0, PositionAngle.TRUE, FramesFactory.getGCRF(), this.date, this.mu);
        final double t = 0.5 * orbit.getKeplerianPeriod();
        // Test 1: the lighting ratio is 0, we look for total eclipse events:
        double ratio = 0.0;
        Map<String, SpacecraftState> events = this.propagate(this.sun, this.aeS, this.moon, this.aeM, orbit, ratio, t);
        // The lighting ratio is 0, and the eclipse can not be total (R_ed>R_ing): no event is detected
        boolean testOK = false;
        try {
            events.get(this.lighting + "IN").getA();
        } catch (final NullPointerException e) {
            testOK = true;
        }
        Assert.assertTrue(testOK);
        testOK = false;
        try {
            events.get(this.lighting + "OUT").getA();
        } catch (final NullPointerException e) {
            testOK = true;
        }
        Assert.assertTrue(testOK);

        // Test 2: the lighting ratio is 1:
        ratio = 1.0;
        events = this.propagate(this.sun, this.aeS, this.moon, this.aeM, orbit, ratio, t);
        // The lighting ratio is 1, and there is an eclipse : two lighting eclipse events and
        // two penumbra are detected
        Assert.assertEquals(4, events.size());
        final SpacecraftState lighIn = events.get(this.lighting + "IN");
        final SpacecraftState lighOut = events.get(this.lighting + "OUT");
        final SpacecraftState penumbraIn = events.get(this.penumbra + "IN");
        final SpacecraftState penumbraOut = events.get(this.penumbra + "OUT");
        // these lighting events must correspond to the penumbra events:
        Assert.assertEquals(0.0, lighIn.getDate().durationFrom(penumbraIn.getDate()), 1E-4);
        Assert.assertEquals(0.0, lighOut.getDate().durationFrom(penumbraOut.getDate()), 1E-4);

        // Test 3: lighting ratio is 0.9 and 0.8:
        ratio = 0.9;
        final Map<String, SpacecraftState> events1 =
            this.propagate(this.sun, this.aeS, this.moon, this.aeM, orbit, ratio, t);
        // two lighting eclipse events and two penumbra are detected, the lighting events must be "contained"
        // in the interval defined by the two penumbra events.
        Assert.assertEquals(4, events.size());
        final SpacecraftState lighIn1 = events1.get(this.lighting + "IN");
        final SpacecraftState lighOut1 = events1.get(this.lighting + "OUT");
        Assert.assertTrue(lighIn1.getDate().durationFrom(penumbraIn.getDate()) > 0.0);
        Assert.assertTrue(lighOut1.getDate().durationFrom(penumbraOut.getDate()) < 0.0);

        ratio = 0.8;
        final Map<String, SpacecraftState> events2 =
            this.propagate(this.sun, this.aeS, this.moon, this.aeM, orbit, ratio, t);
        // two lighting eclipse events and two penumbra are detected, the lighting events must be "contained"
        // in the interval defined by the two previous lighting events with a bigger lighting ratio.
        Assert.assertEquals(4, events.size());
        final SpacecraftState lighIn2 = events2.get(this.lighting + "IN");
        final SpacecraftState lighOut2 = events2.get(this.lighting + "OUT");
        // these lighting events must correspond to the penumbra events:
        Assert.assertTrue(lighIn2.getDate().durationFrom(lighIn1.getDate()) > 0.0);
        Assert.assertTrue(lighOut2.getDate().durationFrom(lighOut1.getDate()) < 0.0);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedMethod {@link VALIDATE_ECLIPSE_BY_LIGHTING_RATIO}
     * 
     * @description test the EclipseDetector during propagation for different values of the lighting ratio.
     *              The occulted body is the Sun and the occulting body is the Moon; the occulting body
     *              apparent radius is bigger than the occulted body one (the orbit is geostationary).
     *              Sun and Moon are ideally located on the x-axis of the inertial frame, the satellite orbit is
     *              equatorial.
     * 
     * @input a geostationary orbit, the Sun and Moon are fixed and ideally located on the x-axis
     *        of the inertial frame.
     * 
     * @output for every propagation, the spacecraft states corresponding to the following detected events:
     *         umbra, penumbra and eclipse based on lighting ratio.
     * 
     * @testPassCriteria coherence among the detected events
     * 
     * @referenceVersion 2.3
     * 
     * @nonRegressionVersion 2.3
     */
    @Test
    public void testBiggerOccultingBodyRadius() throws PatriusException {

        // Test : apparent occulted radius < apparent occulting radius
        // Geostationary orbit:
        final KeplerianOrbit orbit = new KeplerianOrbit(35784e3, .0, .0, 0,
            0, -FastMath.PI / 2.0, PositionAngle.TRUE, FramesFactory.getGCRF(), this.date, this.mu);
        final double t = 0.5 * orbit.getKeplerianPeriod();
        // Test 1: the lighting ratio is 0, we look for total eclipse events:
        double ratio = 0.0;
        Map<String, SpacecraftState> events = this.propagate(this.sun, this.aeS, this.moon, this.aeM, orbit, ratio, t);
        // The lighting ratio is 0, and the eclipse is total (R_ed<R_ing):
        Assert.assertEquals(6, events.size());
        SpacecraftState lighIn = events.get(this.lighting + "IN");
        SpacecraftState lighOut = events.get(this.lighting + "OUT");
        final SpacecraftState umbraIn = events.get(this.umbra + "IN");
        final SpacecraftState umbraOut = events.get(this.umbra + "OUT");
        // these lighting events must correspond to the umbra events:
        Assert.assertEquals(0.0, lighIn.getDate().durationFrom(umbraIn.getDate()), 1E-4);
        Assert.assertEquals(0.0, lighOut.getDate().durationFrom(umbraOut.getDate()), 1E-4);

        // Test 2: the lighting ratio is 1:
        ratio = 1.0;
        events = this.propagate(this.sun, this.aeS, this.moon, this.aeM, orbit, ratio, t);
        // The lighting ratio is 1, and there is an eclipse : two lighting eclipse events and
        // two penumbra are detected
        Assert.assertEquals(6, events.size());
        lighIn = events.get(this.lighting + "IN");
        lighOut = events.get(this.lighting + "OUT");
        final SpacecraftState penumbraIn = events.get(this.penumbra + "IN");
        final SpacecraftState penumbraOut = events.get(this.penumbra + "OUT");
        // these lighting events must correspond to the penumbra events:
        Assert.assertEquals(0.0, lighIn.getDate().durationFrom(penumbraIn.getDate()), 1E-3);
        Assert.assertEquals(0.0, lighOut.getDate().durationFrom(penumbraOut.getDate()), 1E-3);

        // Test 3: lighting ratio is 0.9 and 0.8:
        ratio = 0.9;
        final Map<String, SpacecraftState> events1 =
            this.propagate(this.sun, this.aeS, this.moon, this.aeM, orbit, ratio, t);
        // two lighting eclipse events, two penumbra and two umbra are detected, the lighting events
        // must be "contained" in the interval defined by the two penumbra events, and the umbra events must
        // be inside both two intervals:
        Assert.assertEquals(6, events.size());
        final SpacecraftState lighIn1 = events1.get(this.lighting + "IN");
        final SpacecraftState lighOut1 = events1.get(this.lighting + "OUT");
        Assert.assertTrue(lighIn1.getDate().durationFrom(penumbraIn.getDate()) > 0.0);
        Assert.assertTrue(lighOut1.getDate().durationFrom(penumbraOut.getDate()) < 0.0);
        Assert.assertTrue(lighIn1.getDate().durationFrom(umbraIn.getDate()) < 0.0);
        Assert.assertTrue(lighOut1.getDate().durationFrom(umbraOut.getDate()) > 0.0);
        ratio = 0.8;
        final Map<String, SpacecraftState> events2 =
            this.propagate(this.sun, this.aeS, this.moon, this.aeM, orbit, ratio, t);
        // two lighting eclipse events and two penumbra and two umbra are detected, the lighting events
        // must be containdes in the interval defined by the two previous lighting events with a bigger lighting ratio.
        Assert.assertEquals(6, events.size());
        final SpacecraftState lighIn2 = events2.get(this.lighting + "IN");
        final SpacecraftState lighOut2 = events2.get(this.lighting + "OUT");
        // these lighting events must correspond to the penumbra events:
        Assert.assertTrue(lighIn2.getDate().durationFrom(lighIn1.getDate()) > 0.0);
        Assert.assertTrue(lighOut2.getDate().durationFrom(lighOut1.getDate()) < 0.0);
        Assert.assertTrue(lighIn2.getDate().durationFrom(umbraIn.getDate()) < 0.0);
        Assert.assertTrue(lighOut2.getDate().durationFrom(umbraOut.getDate()) > 0.0);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedMethod {@link VALIDATE_ECLIPSE_BY_LIGHTING_RATIO}
     * 
     * @description test that when the occulting body is behind the occulted body, from the satellite
     *              point of view, no eclipse is detected even if the two bodies are aligned
     * 
     * @input a low equatorial orbit, the Moon and a fictitious celestial body ideally located behind the Moon,
     *        from the satellite point of view.
     * 
     * @output for different values of the lighting ratio, the eclipse events should never been detected
     * 
     * @testPassCriteria no eclipse event is detected
     * 
     * @referenceVersion 2.3
     * 
     * @nonRegressionVersion 2.3
     */
    @Test
    public void testOccultingBodyBehindOcculted() throws PatriusException {
        //
        final KeplerianOrbit orbit = new KeplerianOrbit(8750e3, .0, .0, 0,
            0, -FastMath.PI / 2.0, PositionAngle.TRUE, FramesFactory.getGCRF(), this.date, this.mu);
        final double t = 2.0 * orbit.getKeplerianPeriod();

        // create a fictitious body "behind" the moon from the satellite point of view, whose radius is 2*moon radius:
        final PVCoordinatesProvider occulting = new PVCoordinatesProvider(){
            /** Serializable UID. */
            private static final long serialVersionUID = -5831213996768043416L;

            @Override
            public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
                return new PVCoordinates(new Vector3D(500000000.0, 0.0, 0.0), Vector3D.ZERO);
            }

            @Override
            public Frame getNativeFrame(final AbsoluteDate date,
                    final Frame frame) throws PatriusException {
                return null;
            }
        };
        final double occultingRadius = 2.0 * this.aeM;
        // Test the propagation for different values of the lighting ratio:
        Map<String, SpacecraftState> events =
            this.propagate(this.moon, this.aeM, occulting, occultingRadius, orbit, 0.0, t);
        Assert.assertEquals(0, events.size());
        events = this.propagate(this.moon, this.aeM, occulting, occultingRadius, orbit, 0.5, t);
        Assert.assertEquals(0, events.size());
        events = this.propagate(this.moon, this.aeM, occulting, occultingRadius, orbit, 1.0, t);
        Assert.assertEquals(0, events.size());
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedMethod {@link VALIDATE_ECLIPSE_BY_LIGHTING_RATIO}
     * 
     * @description test the EclipseDetector during propagation for different values of the lighting ratio.
     *              The occulted body is a fictitious Sun and the occulting body is a fictitious Moon, whose coordinates
     *              are:
     *              Moon coordinates : [4.5x, 0, 0], radius Moon = 1/4x
     *              Sun coordinates : [8x, -3/4x, 1/4x], radius Sun = 1/2x
     *              x is the semi-major axis of the equatorial and circular orbit.
     * 
     * @input an equatorial orbit, a fictitious Sun and Moon
     * 
     * @output for every propagation, the spacecraft states corresponding to the following detected events:
     *         umbra, penumbra and eclipse based on lighting ratio.
     * 
     * @testPassCriteria coherence among the detected events
     * 
     * @referenceVersion 2.3
     * 
     * @nonRegressionVersion 2.3
     */
    @Test
    public void testSolarEclipseWithUmbra() throws PatriusException {
        final double x = 7780e3;
        final CartesianOrbit orbit = new CartesianOrbit(new PVCoordinates(new Vector3D(0.0, -x, 0.0),
            new Vector3D(7157.792507, 0.0, 0.0)), FramesFactory.getGCRF(), this.date, this.mu);
        final double t = orbit.getKeplerianPeriod();

        // Fictitious Sun:
        final PVCoordinatesProvider testSun = new PVCoordinatesProvider(){
            /** Serializable UID. */
            private static final long serialVersionUID = 8223810320253815954L;

            @Override
            public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
                return new PVCoordinates(new Vector3D(8 * x, -0.75 * x, 0.25 * x), Vector3D.ZERO);
            }

            @Override
            public Frame getNativeFrame(final AbsoluteDate date,
                    final Frame frame) throws PatriusException {
                return null;
            }
        };
        // Fictitious Moon:
        final PVCoordinatesProvider testMoon = new PVCoordinatesProvider(){
            /** Serializable UID. */
            private static final long serialVersionUID = 7951384611400886564L;

            @Override
            public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
                return new PVCoordinates(new Vector3D(4.5 * x, 0.0, 0.0), Vector3D.ZERO);
            }

            @Override
            public Frame getNativeFrame(final AbsoluteDate date,
                    final Frame frame) throws PatriusException {
                return null;
            }
        };
        final double radiusSun = 0.25 * x;
        final double radiusMoon = 0.5 * x;

        // Test 1: the lighting ratio is 0, we look for total eclipse events:
        double ratio = 0.0;
        Map<String, SpacecraftState> events = this.propagate(testSun, radiusSun, testMoon, radiusMoon, orbit, ratio, t);
        Assert.assertEquals(6, events.size());
        SpacecraftState lighIn = events.get(this.lighting + "IN");
        SpacecraftState lighOut = events.get(this.lighting + "OUT");
        final SpacecraftState umbraIn = events.get(this.umbra + "IN");
        final SpacecraftState umbraOut = events.get(this.umbra + "OUT");
        SpacecraftState penumbraIn = events.get(this.penumbra + "IN");
        SpacecraftState penumbraOut = events.get(this.penumbra + "OUT");
        // The dates of lighting events and umbra events should be identical:
        Assert.assertEquals(0.0, lighIn.getDate().durationFrom(umbraIn.getDate()), 1E-10);
        Assert.assertEquals(0.0, lighOut.getDate().durationFrom(umbraOut.getDate()), 1E-10);
        // Lighting events should be "included" in the penumbra events interval:
        Assert.assertTrue(lighIn.getDate().durationFrom(penumbraIn.getDate()) > 0.0);
        Assert.assertTrue(lighOut.getDate().durationFrom(penumbraOut.getDate()) < 0.0);

        // Test 2: the lighting ratio is 1:
        ratio = 1.0;
        events = this.propagate(testSun, radiusSun, testMoon, radiusMoon, orbit, ratio, t);
        // The lighting ratio is 1, and there is an eclipse : two lighting eclipse events and
        // two penumbra are detected
        Assert.assertEquals(6, events.size());
        lighIn = events.get(this.lighting + "IN");
        lighOut = events.get(this.lighting + "OUT");
        penumbraIn = events.get(this.penumbra + "IN");
        penumbraOut = events.get(this.penumbra + "OUT");
        // these lighting events must correspond to the penumbra events:
        Assert.assertEquals(0.0, lighIn.getDate().durationFrom(penumbraIn.getDate()), 1E-3);
        Assert.assertEquals(0.0, lighOut.getDate().durationFrom(penumbraOut.getDate()), 1E-3);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedMethod {@link VALIDATE_ECLIPSE_BY_LIGHTING_RATIO}
     * 
     * @description test the EclipseDetector during propagation for different values of the lighting ratio.
     *              The occulted body is a fictitious Sun and the occulting body is a fictitious Moon, whose coordinates
     *              are:
     *              Moon coordinates : [4.5x, 0, 0], radius Moon = 1/2x
     *              Sun coordinates : [7x, -x, x], radius Sun = x
     *              x is the semi-major axis of the equatorial and circular orbit.
     * 
     * @input an equatorial orbit, a fictitious Sun and Moon
     * 
     * @output for every propagation, the spacecraft states corresponding to the following detected events:
     *         umbra, penumbra and eclipse based on lighting ratio.
     * 
     * @testPassCriteria coherence among the detected events
     * 
     * @referenceVersion 2.3
     * 
     * @nonRegressionVersion 2.3
     */
    @Test
    public void testSolarEclipseWithPenumbra() throws PatriusException {
        final double x = 7780e3;
        final CartesianOrbit orbit = new CartesianOrbit(new PVCoordinates(new Vector3D(0.0, -x, 0.0),
            new Vector3D(7157.792507, 0.0, 0.0)), FramesFactory.getGCRF(), this.date, this.mu);
        final double t = orbit.getKeplerianPeriod();

        // Fictitious Sun:
        final PVCoordinatesProvider testSun = new PVCoordinatesProvider(){
            /** Serializable UID. */
            private static final long serialVersionUID = 1595286785027311410L;

            @Override
            public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
                return new PVCoordinates(new Vector3D(7 * x, -x, x), Vector3D.ZERO);
            }

            @Override
            public Frame getNativeFrame(final AbsoluteDate date,
                    final Frame frame) throws PatriusException {
                return null;
            }
        };
        // Fictitious Moon:
        final PVCoordinatesProvider testMoon = new PVCoordinatesProvider(){
            /** Serializable UID. */
            private static final long serialVersionUID = 2436196487963794172L;

            @Override
            public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
                return new PVCoordinates(new Vector3D(4.5 * x, 0.0, 0.0), Vector3D.ZERO);
            }

            @Override
            public Frame getNativeFrame(final AbsoluteDate date,
                    final Frame frame) throws PatriusException {
                return null;
            }
        };
        final double radiusSun = x;
        final double radiusMoon = 0.5 * x;

        // Test 1: the lighting ratio is 0, we look for partial eclipse events:
        double ratio = 0.0;
        Map<String, SpacecraftState> events = this.propagate(testSun, radiusSun, testMoon, radiusMoon, orbit, ratio, t);
        Assert.assertEquals(2, events.size());
        SpacecraftState penumbraIn = events.get(this.penumbra + "IN");
        SpacecraftState penumbraOut = events.get(this.penumbra + "OUT");

        this.propagate(testSun, radiusSun, testMoon, radiusMoon, orbit, ratio, t);

        // The lighting ratio is 0, and the eclipse can not be total (R_ed>R_ing): no event is detected
        boolean testOK = false;
        try {
            events.get(this.lighting + "IN").getA();
        } catch (final NullPointerException e) {
            testOK = true;
        }
        Assert.assertTrue(testOK);
        testOK = false;
        try {
            events.get(this.lighting + "OUT").getA();
        } catch (final NullPointerException e) {
            testOK = true;
        }
        Assert.assertTrue(testOK);

        // Test 2: the lighting ratio is 1:
        ratio = 1.0;
        events = this.propagate(testSun, radiusSun, testMoon, radiusMoon, orbit, ratio, t);
        // The lighting ratio is 1, and there is an eclipse : two lighting eclipse events and
        // two penumbra are detected
        Assert.assertEquals(4, events.size());
        final SpacecraftState lighIn = events.get(this.lighting + "IN");
        final SpacecraftState lighOut = events.get(this.lighting + "OUT");
        penumbraIn = events.get(this.penumbra + "IN");
        penumbraOut = events.get(this.penumbra + "OUT");
        // these lighting events must correspond to the penumbra events:
        Assert.assertEquals(0.0, lighIn.getDate().durationFrom(penumbraIn.getDate()), 1E-4);
        Assert.assertEquals(0.0, lighOut.getDate().durationFrom(penumbraOut.getDate()), 1E-4);

    }

    /**
     * Perform a propagation with an umbra, penumbra and eclipse on lighting ratio detectors.
     * 
     * @param occulted
     *        the occulted body
     * @param occultedRadius
     *        the occulted body radius
     * @param occulting
     *        the occulting body
     * @param occultingRadius
     *        the occulting body radius
     * @param orbit
     *        the satellite orbit
     * @param ratio
     *        the lighting ratio
     * @param propagationDuration
     *        the propagation duration
     * @return the map containing all the umbra, penumbra and eclipse on lighting ratio detected events.
     * @throws PatriusException
     */
    private Map<String, SpacecraftState> propagate(final PVCoordinatesProvider occulted,
                                                   final double occultedRadius, final PVCoordinatesProvider occulting,
                                                   final double occultingRadius, final Orbit orbit, final double ratio,
                                                   final double propagationDuration) throws PatriusException {

        final double[] absTolerance = { 0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6 };
        final double[] relTolerance = { 1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7 };
        final AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(
            10, 1, absTolerance, relTolerance);
        integrator.setInitialStepSize(10);
        this.propagator = new NumericalPropagator(integrator);

        final Map<String, SpacecraftState> events = new HashMap<>();
        final EclipseDetector detectorLighting = new EclipseDetector(occulted, occultedRadius,
            occulting, occultingRadius, ratio, 100, 1E-12){
            /** Serializable UID. */
            private static final long serialVersionUID = 5334262898117182432L;

            @Override
            public
                    fr.cnes.sirius.patrius.propagation.events.EventDetector.Action
                    eventOccurred(
                                  final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                           throws PatriusException {
                if (!increasing) {
                    events.put(EclipseDetectorLightingTest.this.lighting + "IN", s);
                } else {
                    events.put(EclipseDetectorLightingTest.this.lighting + "OUT", s);
                }
                return Action.CONTINUE;
            }
        };

        final EclipseDetector detectorUmbra = new EclipseDetector(occulted, occultedRadius,
            occulting, occultingRadius, 0, 100, 1E-12){
            /** Serializable UID. */
            private static final long serialVersionUID = 5334262898117182432L;

            @Override
            public
                    fr.cnes.sirius.patrius.propagation.events.EventDetector.Action
                    eventOccurred(
                                  final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                           throws PatriusException {
                if (!increasing) {
                    events.put(EclipseDetectorLightingTest.this.umbra + "IN", s);
                } else {
                    events.put(EclipseDetectorLightingTest.this.umbra + "OUT", s);
                }
                return Action.CONTINUE;
            }
        };

        final EclipseDetector detectorPenumbra = new EclipseDetector(occulted, occultedRadius,
            occulting, occultingRadius, 1, 100, 1E-12){
            /** Serializable UID. */
            private static final long serialVersionUID = 5334262898117182432L;

            @Override
            public
                    fr.cnes.sirius.patrius.propagation.events.EventDetector.Action
                    eventOccurred(
                                  final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                           throws PatriusException {
                if (!increasing) {
                    events.put(EclipseDetectorLightingTest.this.penumbra + "IN", s);
                } else {
                    events.put(EclipseDetectorLightingTest.this.penumbra + "OUT", s);
                }
                return Action.CONTINUE;
            }
        };

        this.propagator.addEventDetector(detectorLighting);
        this.propagator.addEventDetector(detectorUmbra);
        this.propagator.addEventDetector(detectorPenumbra);
        final SpacecraftState initialState = new SpacecraftState(orbit);
        this.propagator.resetInitialState(initialState);
        this.propagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(initialState.getMu())));
        // Propagate over a half orbit period :
        this.propagator.propagate(this.date.shiftedBy(propagationDuration));
        return events;
    }

    @Before
    public void setUp() {
        this.date = new AbsoluteDate("2013-03-20T11:00:00.000", TimeScalesFactory.getTAI());
        this.mu = 3.9860047e14;
        this.sun = new PVCoordinatesProvider(){
            /** Serializable UID. */
            private static final long serialVersionUID = -643762341702801608L;

            @Override
            public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
                return new PVCoordinates(new Vector3D(149600000000.0, 0.0, 0.0), Vector3D.ZERO);
            }

            @Override
            public Frame getNativeFrame(final AbsoluteDate date,
                    final Frame frame) throws PatriusException {
                return null;
            }
        };
        this.moon = new PVCoordinatesProvider(){
            /** Serializable UID. */
            private static final long serialVersionUID = 7430057463038806968L;

            @Override
            public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
                return new PVCoordinates(new Vector3D(384400000.0, 0.0, 0.0), Vector3D.ZERO);
            }

            @Override
            public Frame getNativeFrame(final AbsoluteDate date,
                    final Frame frame) throws PatriusException {
                return null;
            }
        };
        this.aeS = Constants.SUN_RADIUS;
        this.aeM = Constants.MOON_EQUATORIAL_RADIUS;
    }
}
