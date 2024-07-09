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
 * 
 * @history creation 10/06/2012
 *
 * HISTORY
* VERSION:4.8:DM:DM-2922:15/11/2021:[PATRIUS] suppression de l'utilisation de la reflexion Java dans patrius 
* VERSION:4.3:DM:DM-2102:15/05/2019:[Patrius] Refactoring du paquet fr.cnes.sirius.patrius.bodies
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:227:09/04/2014:Merged eclipse detectors
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 *                             (added forward parameter to eventOccurred signature)
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::FA:382:09/12/2014:Eclipse detector corrections
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:457:09/11/2015: Move extendedOneAxisEllipsoid from patrius to orekit addons
 * VERSION::DM:611:04/08/2016:New implementation using radii provider for visibility of main/inhibition targets
 * VERSION::FA:673:12/09/2016: add getTotalMass(state)
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.assembly.BasicPVCoordinatesProvider;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.EllipsoidBodyShape;
import fr.cnes.sirius.patrius.bodies.ExtendedOneAxisEllipsoid;
import fr.cnes.sirius.patrius.frames.FactoryManagedFrame;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.nonstiff.AdaptiveStepsizeIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.CircularOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.SimpleMassModel;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.AbstractDetector;
import fr.cnes.sirius.patrius.propagation.events.EclipseDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector.Action;
import fr.cnes.sirius.patrius.propagation.events.EventsLogger;
import fr.cnes.sirius.patrius.propagation.events.EventsLogger.LoggedEvent;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class for {@link EclipseDetector}
 * 
 * @see EclipseDetector
 * 
 * @author Rami Houdroge
 * 
 * @version $Id$
 * 
 * @since 1.2
 */
public class EllipsoidEclipseDetectorTest {

    /** eps */
    private static final double EPS = Precision.DOUBLE_COMPARISON_EPSILON;
    /** eps date */
    private static final double EPS_DATE = 1E-10;
    /** one ms */
    private static final double ONEMS = 1E-3;
    /** one µs */
    private static final double ONE_MS = 1E-6;
    /** semi major axis of orbit */
    private static final int A = 7000000;
    /** mu */
    private final double mu = 3.9860047e14;
    /** date */
    private AbsoluteDate initialDate;
    /** container */
    private NumericalPropagator propagator;
    /** container */
    private Frame frame;
    /** container */
    private TreeMap<AbsoluteDate, Double[]> dataSpheroid;
    /** container */
    private TreeMap<AbsoluteDate, Double[]> dataSpherical;
    /** container */
    private KeplerianOrbit orbit;
    /** container */
    private CelestialBody sun;
    /** container */
    private double sunR;
    /** container */
    private double ae;
    /** container */
    private double f;
    /** container */
    private FactoryManagedFrame bodyFrame;
    /** container */
    private CelestialBody earthC;
    /** container */
    private double propagationDuration;
    /** container */
    private SpacecraftState initialState;
    /** container */
    private SimpleMassModel massModel;
    /** name */
    private final String name = "earth";
    /** umbra string. */
    private final String umbra = "UMBRA";
    /** penumbra string. */
    private final String penumbra = "PENUMBRA";

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Ellipsoid eclipse detector
         * 
         * @featureDescription Test the detector of eclipse from ellipsoids
         * 
         * @coveredRequirements DV-MOD_310
         */
        ELLIPSOID_ECLIPSE
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ELLIPSOID_ECLIPSE}
     * 
     * @testedMethod {@link EclipseDetector#g(SpacecraftState)}
     * 
     * @description Test the robustness of the detection algorithm with a very thin Earth
     * 
     * @input a spheroid celestial body shape (occulting body), some points of space (propagated satellite) and the sun
     *        (occulted body)
     * 
     * @output eclipse event times
     * 
     * @testPassCriteria the total duration of the penumbra must be equal to 33 s ± 10 s
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.3
     */
    @Test
    public void testVeryThinEllipsoid() {

        try {

            // Earth with flattening and equatorial radius adjusted so that the conjugate radius is equal to the
            // spheridcal radius
            final EllipsoidBodyShape earth = new ExtendedOneAxisEllipsoid(this.ae, .99, this.bodyFrame, this.name);

            // Eclipse detectors
            final MySpheroidEclipseDetector detectorSpheroid =
                new MySpheroidEclipseDetector(this.sun, this.sunR, earth, false);

            // propagators
            this.propagator.clearEventsDetectors();
            this.propagator.setInitialState(this.initialState);
            this.propagator.addEventDetector(detectorSpheroid);
            this.propagator.propagate(this.initialDate.shiftedBy(this.propagationDuration));

            this.dataSpheroid = detectorSpheroid.getData();
            final TreeMap<AbsoluteDate, Double[]> dataSpheroidPenumbra = detectorSpheroid.getData();

            // retrieve dates of events
            final AbsoluteDate[] spheroidEclipsePenumbra = dataSpheroidPenumbra.keySet().toArray(
                new AbsoluteDate[dataSpheroidPenumbra.keySet().size()]);

            Assert.assertEquals(33, spheroidEclipsePenumbra[1].durationFrom(spheroidEclipsePenumbra[0]), 10);

        } catch (final PatriusException oe) {
            Assert.fail(oe.getLocalizedMessage());
        }

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ELLIPSOID_ECLIPSE}
     * 
     * @testedMethod {@link EclipseDetector#g(SpacecraftState)}
     * 
     * @description Test the correctness of the detection algorithm when the ellipsoid is such that the polar radius is
     *              equal to the spherical equatorial radius
     * 
     * @input a spheroid celestial body shape (occulting body), some points of space (propagated satellite) and the sun
     *        (occulted body)
     * 
     * @output eclipse event times
     * 
     * @testPassCriteria the event times must be the same as that of the sphere eclipse events. Tolerance is set to 1
     *                   ms.
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.3
     */
    @Test
    public void testWithAugmentedEllipsoid() {

        try {

            // Earth with flattening and equatorial radius adjusted so that the conjugate radius is equal to the
            // spheridcal radius
            final EllipsoidBodyShape earth = new ExtendedOneAxisEllipsoid(this.ae / (1 - this.f), this.f,
                this.bodyFrame, this.name);

            // check dimension of adjusted Earth model
            Assert.assertEquals(this.ae, earth.getConjugateRadius(), 1e-10);

            // Eclipse detectors
            final MySpheroidEclipseDetector detectorSpheroid =
                new MySpheroidEclipseDetector(this.sun, this.sunR, earth, true);
            final MySphericalEclipseDetector detectorSpherical =
                new MySphericalEclipseDetector(this.sun, this.sunR, this.earthC, this.ae,
                    true);

            // propagators
            this.propagator.clearEventsDetectors();
            this.propagator.setInitialState(this.initialState);
            this.propagator.addEventDetector(detectorSpheroid);
            this.propagator.addEventDetector(detectorSpherical);
            this.propagator.propagate(this.initialDate.shiftedBy(this.propagationDuration));

            this.dataSpheroid = detectorSpheroid.getData();
            this.dataSpherical = detectorSpherical.getData();

            // check that we have the same number of "enter / exit eclipse zone" events
            Assert.assertEquals(this.dataSpherical.keySet().size(), this.dataSpheroid.keySet().size());

            // retrieve dates of events
            final AbsoluteDate[] sphericalEclipse = this.dataSpherical.keySet().toArray(
                new AbsoluteDate[this.dataSpherical.keySet().size()]);
            final AbsoluteDate[] spheroidEclipse = this.dataSpheroid.keySet().toArray(
                new AbsoluteDate[this.dataSpherical.keySet().size()]);

            for (int i = 0; i < sphericalEclipse.length; i++) {
                Assert.assertEquals(0, spheroidEclipse[i].durationFrom(sphericalEclipse[i]), ONEMS);
            }

        } catch (final PatriusException oe) {
            Assert.fail(oe.getLocalizedMessage());
        }

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ELLIPSOID_ECLIPSE}
     * 
     * @testedMethod {@link EclipseDetector#g(SpacecraftState)}
     * 
     * @description Test the correctness of the detection algorithm with a standard ellipsoid (GRIM5C1 equatorial radius
     *              and earth flattening)
     * 
     * @input a spheroid celestial body shape (occulting body), some points of space (propagated satellite) and the sun
     *        (occulted body)
     * 
     * @output eclipse event times
     * 
     * @testPassCriteria the event times must be 6.822 s from the sphere eclipse events. Tolerance is set to 1 ms.
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.3
     */
    @Test
    public void testWithEllipsoid() {

        try {

            // Earth with no flattening
            final EllipsoidBodyShape earth = new ExtendedOneAxisEllipsoid(this.ae, this.f, this.bodyFrame, this.name);

            // Eclipse detectors
            final MySpheroidEclipseDetector detectorSpheroid =
                new MySpheroidEclipseDetector(this.sun, this.sunR, earth, true);
            final MySphericalEclipseDetector detectorSpherical =
                new MySphericalEclipseDetector(this.sun, this.sunR, this.earthC, this.ae,
                    true);

            // propagators
            this.propagator.clearEventsDetectors();
            this.propagator.setInitialState(this.initialState);
            this.propagator.addEventDetector(detectorSpheroid);
            this.propagator.addEventDetector(detectorSpherical);
            this.propagator.propagate(this.initialDate.shiftedBy(this.propagationDuration));

            this.dataSpheroid = detectorSpheroid.getData();
            this.dataSpherical = detectorSpherical.getData();

            // eclipse time difference approximation
            final double daol = MathLib.acos(earth.getConjugateRadius() / A)
                - MathLib.acos(earth.getTransverseRadius() / A);
            final double dt = MathLib.abs(this.orbit.getKeplerianPeriod() * daol / 2 / FastMath.PI);

            // check that we have the same number of "enter / exit eclipse zone" events
            Assert.assertEquals(this.dataSpherical.keySet().size(), this.dataSpheroid.keySet().size());

            // retrieve dates of events
            final AbsoluteDate[] sphericalEclipse = this.dataSpherical.keySet().toArray(
                new AbsoluteDate[this.dataSpherical.keySet().size()]);
            final AbsoluteDate[] spheroidEclipse = this.dataSpheroid.keySet().toArray(
                new AbsoluteDate[this.dataSpherical.keySet().size()]);

            // in the case of a spheroid, the eclipse will last approximately dt seconds less than in the case of a
            // sphere thus, we check that entering and existing differ by at most dt, with a 1 msec accuracy
            for (int i = 0; i < spheroidEclipse.length; i++) {
                Assert.assertEquals(0, MathLib.abs(spheroidEclipse[i].durationFrom(sphericalEclipse[i])) - dt, ONEMS);
            }

        } catch (final PatriusException oe) {
            Assert.fail(oe.getLocalizedMessage());
        }

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ELLIPSOID_ECLIPSE}
     * 
     * @testedMethod {@link EclipseDetector#g(SpacecraftState)}
     * 
     * @description Test the correctness of the detection algorithm with a standard ellipsoid (GRIM5C1 equatorial radius
     *              and 0 flattening)
     * 
     * @input a spheroid celestial body shape (occulting body), some points of space (propagated satellite) and the sun
     *        (occulted body)
     * 
     * @output eclipse event times
     * 
     * @testPassCriteria the event times must be the same as that of the sphere eclipse events. Tolerance is set to 1
     *                   µs.
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.3
     */
    @Test
    public void testWithSphericalEarth() {
        try {

            // Earth with no flattening
            final EllipsoidBodyShape earth = new ExtendedOneAxisEllipsoid(this.ae, 0, this.bodyFrame, this.name);

            // Eclipse detectors
            final MySpheroidEclipseDetector detectorSpheroid =
                new MySpheroidEclipseDetector(this.sun, this.sunR, earth, true);
            final MySphericalEclipseDetector detectorSpherical =
                new MySphericalEclipseDetector(this.sun, this.sunR, this.earthC, this.ae,
                    true);

            // propagators
            this.propagator.clearEventsDetectors();
            this.propagator.setInitialState(this.initialState);
            this.propagator.addEventDetector(detectorSpheroid);
            this.propagator.addEventDetector(detectorSpherical);
            this.propagator.propagate(this.initialDate.shiftedBy(this.propagationDuration));

            this.dataSpheroid = detectorSpheroid.getData();
            this.dataSpherical = detectorSpherical.getData();

            // check that we have the same number of "enter / exit eclipse zone" events
            Assert.assertEquals(this.dataSpherical.keySet().size(), this.dataSpheroid.keySet().size());

            // retrieve dates of events
            final AbsoluteDate[] sphericalEclipse = this.dataSpherical.keySet().toArray(
                new AbsoluteDate[this.dataSpherical.keySet().size()]);
            final AbsoluteDate[] spheroidEclipse = this.dataSpheroid.keySet().toArray(
                new AbsoluteDate[this.dataSpherical.keySet().size()]);

            // the spheroid has a flattening of zero. Therefore, the eclipse enter and exit times must be equal
            for (int i = 0; i < sphericalEclipse.length; i++) {
                Assert.assertEquals(0, spheroidEclipse[i].durationFrom(sphericalEclipse[i]), ONE_MS);
            }

        } catch (final PatriusException oe) {
            Assert.fail(oe.getLocalizedMessage());
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ELLIPSOID_ECLIPSE}
     * 
     * @testedMethod {@link EclipseDetector#g(SpacecraftState)}
     * 
     * @description Test the correctness of the detection algorithm with a standard ellipsoid (GRIM5C1 equatorial radius
     *              and 0 flattening) when the occulting body is behind the occulted body. To do this the central body
     *              is the occulting one,
     *              and the occulted body is a small sphere fixed between vehicle and central body.
     * 
     * @input a spheroid celestial body shape (occulting body), an equinoxial orbit (the vehicle) and an occulted body
     *        fixed to a distance between the vehicle and the occulting body
     * 
     * @output the same number of eclipses are detected with two different EclipseDetectos
     * 
     * @testPassCriteria
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.2
     */
    @Test
    public void testDistanceEffect() {

        // a = 14000000m, e=0, i=Pi/2, AoP=0, RAAN=0, M=0 @ 2000-01-01T00:00:00.0
        final AbsoluteDate iniDate = AbsoluteDate.J2000_EPOCH;
        final CircularOrbit circOrbit = new CircularOrbit(14000e3, 0.0, 0.0, MathUtils.HALF_PI, 0.0, 0.0,
            PositionAngle.TRUE, this.frame, iniDate, this.mu);

        // An occulted body at distance of 7000 km from earth center on GCRF x-axis
        final Vector3D occultedPos = new Vector3D(7000e3, 0.0, 0.0);
        final Vector3D occultedVel = Vector3D.ZERO;
        final double occultedRadius = 100;
        final PVCoordinates occultedPV = new PVCoordinates(occultedPos, occultedVel);
        final PVCoordinatesProvider occultedBody = new BasicPVCoordinatesProvider(occultedPV, this.frame);
        final DormandPrince853Integrator integ = new DormandPrince853Integrator(1, 60, new double[] { 1e-6, 1e-6, 1e-6,
            1e-9, 1e-9, 1e-9 }, new double[] { 1e-10, 1e-10, 1e-10, 1e-10, 1e-10, 1e-10 });
        final NumericalPropagator numProp = new NumericalPropagator(integ);

        try {

            // Central Body : earth with no flattening
            final EllipsoidBodyShape earth = new ExtendedOneAxisEllipsoid(this.ae, 0.0,
                FramesFactory.getTIRF(), this.name);

            // EllipsoidEclipse detectors and Orekit EclipseDetector
            final EclipseDetector detectorSpheroid = new EclipseDetector(occultedBody,
                occultedRadius, earth, 0, AbstractDetector.DEFAULT_MAXCHECK, AbstractDetector.DEFAULT_THRESHOLD){
                /** serial UID */
                private static final long serialVersionUID = 1L;

                @Override
                public
                        Action
                        eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                               throws PatriusException {
                    return Action.CONTINUE;
                }
            };
            final EclipseDetector orekitEclipseDector =
                new EclipseDetector(occultedBody, occultedRadius, this.earthC, this.ae,
                    0, AbstractDetector.DEFAULT_MAXCHECK, AbstractDetector.DEFAULT_THRESHOLD){
                    /** serial UID */
                    private static final long serialVersionUID = 1L;

                    @Override
                    public
                            Action
                            eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                                   throws PatriusException {
                        return Action.CONTINUE;
                    }
                };

            // Log orekit detector events
            final EventsLogger loggerOrekit = new EventsLogger();
            final EventsLogger loggerEllipsoid = new EventsLogger();

            // A numerical propagator
            final SpacecraftState iniState = new SpacecraftState(circOrbit);
            numProp.clearEventsDetectors();
            numProp.setInitialState(iniState);
            numProp.addEventDetector(loggerOrekit.monitorDetector(orekitEclipseDector));
            numProp.addEventDetector(loggerEllipsoid.monitorDetector(detectorSpheroid));

            // Propagate during three orbital period
            final double period = circOrbit.getKeplerianPeriod();
            numProp.propagate(iniDate.shiftedBy(3 * period));

            // Orekit detector should detect two times more events than ellipsoid one :
            final List<LoggedEvent> orekitEvents = loggerOrekit.getLoggedEvents();
            final List<LoggedEvent> ellipsoidEvents = loggerEllipsoid.getLoggedEvents();

            final int numberOfOrekitEv = orekitEvents.size();
            final int numberOfEllipsoidEv = ellipsoidEvents.size();
            Assert.assertTrue(numberOfOrekitEv == numberOfEllipsoidEv);

            // Date of ellipsoid logger event should be equals to second and third,
            // sixth and seventh, and tenth and eleventh orekit logger
            Assert.assertEquals(orekitEvents.get(0).getState().getDate().durationFrom(iniDate), ellipsoidEvents.get(0)
                .getState().getDate().durationFrom(iniDate), EPS_DATE);
            Assert.assertEquals(orekitEvents.get(1).getState().getDate().durationFrom(iniDate), ellipsoidEvents.get(1)
                .getState().getDate().durationFrom(iniDate), EPS_DATE);
            Assert.assertEquals(orekitEvents.get(2).getState().getDate().durationFrom(iniDate), ellipsoidEvents.get(2)
                .getState().getDate().durationFrom(iniDate), EPS_DATE);
            Assert.assertEquals(orekitEvents.get(3).getState().getDate().durationFrom(iniDate), ellipsoidEvents.get(3)
                .getState().getDate().durationFrom(iniDate), EPS_DATE);
            Assert.assertEquals(orekitEvents.get(4).getState().getDate().durationFrom(iniDate), ellipsoidEvents.get(4)
                .getState().getDate().durationFrom(iniDate), EPS_DATE);
            Assert.assertEquals(orekitEvents.get(5).getState().getDate().durationFrom(iniDate), ellipsoidEvents.get(5)
                .getState().getDate().durationFrom(iniDate), EPS_DATE);

        } catch (final PatriusException oe) {
            Assert.fail(oe.getLocalizedMessage());
        }

    }

    /**
     * @throws PatriusException
     *         if fail
     * @testType UT
     * 
     * @testedFeature {@link features#ELLIPSOID_ECLIPSE}
     * 
     * @testedMethod {@link EclipseDetector#getOcculted()}
     * @testedMethod {@link EclipseDetector#getOccultedRadius()}
     * @testedMethod {@link EclipseDetector#getOcculting()}
     * 
     * @description Test the correctness of the getters
     * 
     * @input a spheroid celestial body shape (occulting body), some points of space (propagated satellite) and the sun
     *        (occulted body)
     * 
     * @output eclipse event times
     * 
     * @testPassCriteria the event times must be the same as that of the sphere eclipse events. Tolerance is set to 1
     *                   µs.
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.3
     */
    @Test
    public void testGetters() throws PatriusException {
        // Earth with no flattening
        final EllipsoidBodyShape earth = new ExtendedOneAxisEllipsoid(this.ae, 0, this.bodyFrame, this.name);

        // Eclipse detectors
        final EclipseDetector d = new EclipseDetector(this.sun, this.sunR, earth, 0, AbstractDetector.DEFAULT_MAXCHECK,
            AbstractDetector.DEFAULT_THRESHOLD);

        Assert.assertEquals(this.sunR, d.getOccultedRadius(), EPS);
        Assert.assertEquals(earth, d.getOcculting());
        Assert.assertEquals(this.sun, d.getOcculted());
        Assert.assertTrue(d.isTotalEclipse());
        Assert.assertTrue(d.eventOccurred(null, true, true) == Action.STOP);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedMethod {@link ELLIPSOID_ECLIPSE}
     * 
     * @description test the EclipseDetector during propagation for a spheroidal occulting body.
     *              This test does not use numerical references to validate the detector, it only qualitatively shows
     *              that the detector of umbra/penumbra does detect events when using a spheroidal occulting body
     *              and a lighting ratio.
     * 
     * @input an equatorial orbit, a fictitious Sun and Moon
     * 
     * @output the events detected during a half-orbit propagation
     * 
     * @testPassCriteria four events should be detected: two umbra and two penumbra events.
     * 
     * @referenceVersion 2.3.1
     * 
     * @nonRegressionVersion 2.3.1
     */
    @Test
    public void testLightingRatioSpheroid() throws PatriusException {
        final AbsoluteDate date = new AbsoluteDate("2013-03-20T11:00:00.000", TimeScalesFactory.getTAI());

        // The occulted spherical body:
        final double r = 6378137.0;
        final PVCoordinatesProvider occulted = new PVCoordinatesProvider(){
            @Override
            public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
                return new PVCoordinates(new Vector3D(10 * r, 0.0, 0.0), Vector3D.ZERO);
            }
        };
        final Transform t = new Transform(date, new Vector3D(3 * r, 0, 0));
        final Frame frame = new Frame(FramesFactory.getGCRF(), t, "frame");

        // Occulting body is a spheroid:
        final EllipsoidBodyShape occulting =
            new ExtendedOneAxisEllipsoid(0.3 * r, 0.1, frame,
                "Spheroid");
        final Map<String, SpacecraftState> events = new HashMap<String, SpacecraftState>();
        final EclipseDetector detectorUmbra = new EclipseDetector(occulted, r,
            occulting, 1e-11, 60, 1E-3){
            /**  */
            private static final long serialVersionUID = 5334262898117182432L;

            @Override
            public
                    fr.cnes.sirius.patrius.propagation.events.EventDetector.Action
                    eventOccurred(
                                  final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                           throws PatriusException {
                if (!increasing) {
                    events.put(EllipsoidEclipseDetectorTest.this.umbra + "IN", s);
                } else {
                    events.put(EllipsoidEclipseDetectorTest.this.umbra + "OUT", s);
                }
                return Action.CONTINUE;
            };
        };
        final EclipseDetector detectorPenumbra = new EclipseDetector(occulted, r,
            occulting, 1.0 - 1e-11, 60, 1E-3){
            /**  */
            private static final long serialVersionUID = 5334262898117182432L;

            @Override
            public
                    fr.cnes.sirius.patrius.propagation.events.EventDetector.Action
                    eventOccurred(
                                  final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                           throws PatriusException {
                if (!increasing) {
                    events.put(EllipsoidEclipseDetectorTest.this.penumbra + "IN", s);
                } else {
                    events.put(EllipsoidEclipseDetectorTest.this.penumbra + "OUT", s);
                }
                return Action.CONTINUE;
            };
        };
        final KeplerianOrbit orbit = new KeplerianOrbit(6775e3, .0, .0, 0,
            0, -FastMath.PI / 2.0, PositionAngle.TRUE, FramesFactory.getGCRF(), date, this.mu);

        final double[] absTolerance = { 0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6 };
        final double[] relTolerance = { 1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7 };
        final AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(
            10, 1, absTolerance, relTolerance);
        integrator.setInitialStepSize(10);
        this.propagator = new NumericalPropagator(integrator);
        this.propagator.addEventDetector(detectorUmbra);
        this.propagator.addEventDetector(detectorPenumbra);
        final SpacecraftState initialState = new SpacecraftState(orbit);
        this.propagator.resetInitialState(initialState);
        // Propagate over a half orbit period :
        final double time = 0.5 * orbit.getKeplerianPeriod();
        this.propagator.propagate(date.shiftedBy(time));
        Assert.assertEquals(4, events.size());
    }

    /**
     * Setup class
     * 
     * @throws PatriusException
     *         if failure
     */
    @Before
    public void setup() throws PatriusException {
        Utils.setDataRoot("regular-dataPBASE");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));

        this.frame = FramesFactory.getGCRF();

        // a = 7000000m, e=0, i=Pi/2, AoP=0, RAAN=0, M=0 @ 2000-03-20T07:25:26.92866534
        // At this time, the sun is exactly in the xOz plane of the GCRF, as well as the orbit
        // sun is 10E-05° above xOy plane
        this.propagationDuration = 86400 * .3;
        this.initialDate = new AbsoluteDate(2000, 3, 20, 7, 25, 26.92866534, TimeScalesFactory.getUTC())
            .shiftedBy(-this.propagationDuration / 2);
        this.orbit =
            new KeplerianOrbit(A, 0, MathUtils.HALF_PI, 0, 0, 0, PositionAngle.TRUE, this.frame, this.initialDate,
                this.mu);

        this.sun = CelestialBodyFactory.getSun();
        this.sunR = Constants.SUN_RADIUS;

        this.ae = Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS;
        this.f = Constants.GRIM5C1_EARTH_FLATTENING;

        this.bodyFrame = FramesFactory.getITRF();

        this.earthC = CelestialBodyFactory.getEarth();

        final DormandPrince853Integrator integ = new DormandPrince853Integrator(1, 60, new double[] { 1e-6, 1e-6, 1e-6,
            1e-9, 1e-9, 1e-9, 1e-10 }, new double[] { 1e-10, 1e-10, 1e-10, 1e-10, 1e-10, 1e-10, 1e-10 });
        this.propagator = new NumericalPropagator(integ);
        this.massModel = new SimpleMassModel(4000, "default");
        this.initialState = new SpacecraftState(this.orbit, this.massModel);
        this.propagator.setMassProviderEquation(this.massModel);
    }

    /**
     * Overriden detector, added logger to eventOccured method
     */
    public class MySpheroidEclipseDetector extends EclipseDetector {

        /** Generated serial-UID */
        private static final long serialVersionUID = -2155364246472748457L;
        /** Logs event data */
        private final TreeMap<AbsoluteDate, Double[]> mapNew = new TreeMap<AbsoluteDate, Double[]>();

        /**
         * new detector
         * 
         * @param occulted
         *        body
         * @param occultedRadius
         *        body radius
         * @param occulting
         *        body
         * @param totalEclipse
         *        flag
         */
        public MySpheroidEclipseDetector(final PVCoordinatesProvider occulted, final double occultedRadius,
            final EllipsoidBodyShape occulting, final boolean totalEclipse) {
            super(occulted, occultedRadius, occulting, totalEclipse ? 0 : 1, AbstractDetector.DEFAULT_MAXCHECK,
                AbstractDetector.DEFAULT_THRESHOLD);
        }

        @Override
        public Action eventOccurred(final SpacecraftState s,
                final boolean increasing,
                final boolean forward) throws PatriusException {

            this.mapNew.put(
                    s.getDate(),
                    new Double[] {
                            this.getOccultingRadiusProvider().getLocalRadius(s.getPVCoordinates().getPosition(),
                                    s.getFrame(), s.getDate(), EllipsoidEclipseDetectorTest.this.sun), s.getLv(),
                            increasing ? 0. : 1. });
            return Action.CONTINUE;
        }

        /**
         * Returns logged data
         * 
         * @return data map
         */
        public TreeMap<AbsoluteDate, Double[]> getData() {
            return this.mapNew;
        }
    }

    /**
     * Overriden detector, added logger to eventOccured method
     */
    public class MySphericalEclipseDetector extends EclipseDetector {

        /** Generated serial-UID */
        private static final long serialVersionUID = -8788058868258865109L;
        /** Logs event data */
        private final TreeMap<AbsoluteDate, Double[]> mapNewOld = new TreeMap<AbsoluteDate, Double[]>();

        /**
         * new detector
         * 
         * @param occulted
         *        body
         * @param occultedRadius
         *        body radius
         * @param occulting
         *        body
         * @param occultingRadius
         *        radius
         * @param total
         *        flag
         */
        public MySphericalEclipseDetector(final PVCoordinatesProvider occulted, final double occultedRadius,
            final PVCoordinatesProvider occulting, final double occultingRadius, final boolean total) {
            super(occulted, occultedRadius, occulting, occultingRadius, total ? 0 : 1,
                AbstractDetector.DEFAULT_MAXCHECK, AbstractDetector.DEFAULT_THRESHOLD);
        }

        @Override
        public
                Action
                eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                       throws PatriusException {
            this.mapNewOld.put(s.getDate(), new Double[] { (double) A, s.getLv(), increasing ? 0. : 1. });
            return Action.CONTINUE;
        }

        /**
         * Returns logged data
         * 
         * @return data map
         */
        public TreeMap<AbsoluteDate, Double[]> getData() {
            return this.mapNewOld;
        }
    }
}
