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
 * VERSION:4.13:DM:DM-37:08/12/2023:[PATRIUS] Date d'evenement et propagation du signal
 * VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
 * VERSION:4.13:DM:DM-32:08/12/2023:[PATRIUS] Ajout d'un ThreeAxisEllipsoid
 * VERSION:4.13:DM:DM-132:08/12/2023:[PATRIUS] Suppression de la possibilite
 * de convertir les sorties de VacuumSignalPropagation
 * VERSION:4.13:FA:FA-144:08/12/2023:[PATRIUS] la methode BodyShape.getBodyFrame devrait
 * retourner un CelestialBodyFrame
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.BodyShape.MarginType;
import fr.cnes.sirius.patrius.events.detectors.AbstractSignalPropagationDetector.PropagationDelayType;
import fr.cnes.sirius.patrius.frames.CelestialBodyFrame;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Ellipsoid;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.ConstantPVCoordinatesProvider;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.signalpropagation.VacuumSignalPropagationModel;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Unit test class for the {@link GroundAntenna} class.
 * 
 * @author Thibaut BONIT
 *
 * @version $Id$
 *
 * @since 4.13
 * 
 */
public class ThreeAxisEllipsoidTest {

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(ThreeAxisEllipsoidTest.class.getSimpleName(), "Three axis ellipsoid");
    }

    @Before
    public void setUp() throws PatriusException {
        Utils.setDataRoot("regular-dataPBASE");
        FramesFactory.setConfiguration(Utils.getIERS2003Configuration(true));
    }

    /**
     * @throws PatriusException
     *         if the precession-nutation model data embedded in the library cannot be read
     * @description Builds a new instance and tests the basic getters.
     *
     * @testedMethod {@link ThreeAxisEllipsoid#ThreeAxisEllipsoid(double, double, double, Frame)}
     * @testedMethod {@link ThreeAxisEllipsoid#ThreeAxisEllipsoid(double, double, double, Frame, String)}
     * @testedMethod {@link AbstractBodyShape#getName()}
     * @testedMethod {@link AbstractBodyShape#getBodyFrame()}
     * @testedMethod {@link AbstractBodyShape#getNativeFrame(AbsoluteDate)}
     * @testedMethod {@link AbstractBodyShape#getEpsilonSignalPropagation()}
     * @testedMethod {@link AbstractBodyShape#setEpsilonSignalPropagation(double)}
     * @testedMethod {@link AbstractEllipsoidBodyShape#getARadius()}
     * @testedMethod {@link AbstractEllipsoidBodyShape#getBRadius()}
     * @testedMethod {@link AbstractEllipsoidBodyShape#getCRadius()}
     * @testedMethod {@link AbstractEllipsoidBodyShape#getEncompassingSphereRadius()}
     * @testedMethod {@link AbstractEllipsoidBodyShape#getEllipsoid()}
     * @testedMethod {@link EllipsoidBodyShape#isSpherical()}
     * @testedMethod {@link AbstractEllipsoidBodyShape#computePositionFromEllipsodeticCoordinates(double, double, double)}
     * @testedMethod {@link BodyShape#getDistanceEpsilon()}
     * @testedMethod {@link BodyShape#setDistanceEpsilon(double)}
     * @testedMethod {@link AbstractEllipsoidBodyShape#getLLHCoordinatesSystem()}
     * @testedMethod {@link AbstractEllipsoidBodyShape#setLLHCoordinatesSystem(LLHCoordinatesSystem)}
     * @testedMethod {@link AbstractEllipsoidBodyShape#isDefaultLLHCoordinatesSystem()}
     * @testedMethod {@link AbstractEllipsoidBodyShape#setConvergenceThreshold(double)}
     * @testedMethod {@link AbstractEllipsoidBodyShape#getMaxIterSignalPropagation()}
     * @testedMethod {@link AbstractEllipsoidBodyShape#setMaxIterSignalPropagation(int)}
     * @testedMethod {@link ThreeAxisEllipsoid#DEFAULT_THREE_AXIS_ELLIPSOID_NAME}
     * @testedMethod {@link AbstractEllipsoidBodyShape#DEFAULT_LLH_COORD_SYSTEM}
     * @testedMethod {@link AbstractEllipsoidBodyShape#CLOSE_APPROACH_THRESHOLD}
     * @testedMethod {@link AbstractBodyShape#DEFAULT_EPSILON_SIGNAL_PROPAGATION}
     * @testedMethod {@link BodyShape#DEFAULT_DISTANCE_EPSILON}
     * @testedMethod {@link BodyShape#DIRECTION_FACTOR}
     * 
     * @testPassCriteria The instance is build without error and the basic getters return the expected data.
     */
    @Test
    public void testConstructor() throws PatriusException {

        // Initialization
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final CelestialBodyFrame frame = FramesFactory.getITRF();
        final String name = "elName";
        final double aRadius = Constants.WGS84_EARTH_EQUATORIAL_RADIUS;
        final double bRadius = aRadius + 50.;
        final double cRadius = aRadius - 100.;

        // Build an ellipsoid as a sphere
        ThreeAxisEllipsoid model = new ThreeAxisEllipsoid(aRadius, aRadius, aRadius, frame);
        Assert.assertEquals(ThreeAxisEllipsoid.DEFAULT_THREE_AXIS_ELLIPSOID_NAME, model.getName());
        Assert.assertTrue(model.isSpherical());
        Assert.assertEquals(aRadius, model.getARadius(), 0.);
        Assert.assertEquals(aRadius, model.getBRadius(), 0.);
        Assert.assertEquals(aRadius, model.getCRadius(), 0.);

        model = new ThreeAxisEllipsoid(aRadius, bRadius, cRadius, frame, name);

        Assert.assertEquals(name, model.getName());
        Assert.assertFalse(model.isSpherical());
        Assert.assertEquals(frame, model.getBodyFrame());
        Assert.assertEquals(frame, model.getNativeFrame(date));

        Assert.assertEquals(AbstractBodyShape.DEFAULT_EPSILON_SIGNAL_PROPAGATION, model.getEpsilonSignalPropagation(),
            0.);
        model.setEpsilonSignalPropagation(1e-12);
        Assert.assertEquals(1e-12, model.getEpsilonSignalPropagation(), 0.);

        Assert.assertEquals(aRadius, model.getARadius(), 0.);
        Assert.assertEquals(bRadius, model.getBRadius(), 0.);
        Assert.assertEquals(cRadius, model.getCRadius(), 0.);
        Assert.assertEquals(bRadius, model.getEncompassingSphereRadius(), 0.); // Largest radius

        Assert.assertTrue(model.getEllipsoid() instanceof Ellipsoid);

        Assert.assertEquals(new Vector3D(6219778.958580832, 1260831.381305696, 636732.1647730129),
            model.computePositionFromEllipsodeticCoordinates(0.1, 0.2, 10.2)); // Non regression (ref: 4.13)

        Assert.assertEquals(BodyShape.DEFAULT_DISTANCE_EPSILON, model.getDistanceEpsilon(), 0.);
        model.setDistanceEpsilon(1.2e-10);
        Assert.assertEquals(1.2e-10, model.getDistanceEpsilon(), 0.);

        Assert.assertEquals(LLHCoordinatesSystem.ELLIPSODETIC, model.getLLHCoordinatesSystem());
        Assert.assertTrue(model.isDefaultLLHCoordinatesSystem());
        model.setLLHCoordinatesSystem(LLHCoordinatesSystem.BODYCENTRIC_NORMAL);
        Assert.assertEquals(LLHCoordinatesSystem.BODYCENTRIC_NORMAL, model.getLLHCoordinatesSystem());
        Assert.assertFalse(model.isDefaultLLHCoordinatesSystem());

        model.setConvergenceThreshold(1e-12); // For coverage, we can't access the value in Ellipsoid to check

        Assert.assertEquals(VacuumSignalPropagationModel.DEFAULT_MAX_ITER, model.getMaxIterSignalPropagation(), 0.);
        model.setMaxIterSignalPropagation(12);
        Assert.assertEquals(12, model.getMaxIterSignalPropagation(), 0.);

        // Evaluate the static parameters values by non regression
        Assert.assertEquals("THREE_AXIS_ELLIPSOID", ThreeAxisEllipsoid.DEFAULT_THREE_AXIS_ELLIPSOID_NAME);
        Assert.assertEquals(LLHCoordinatesSystem.ELLIPSODETIC, AbstractEllipsoidBodyShape.DEFAULT_LLH_COORD_SYSTEM);
        Assert.assertEquals(1e-10, AbstractEllipsoidBodyShape.CLOSE_APPROACH_THRESHOLD, 0.);
        Assert.assertEquals(1e-14, AbstractBodyShape.DEFAULT_EPSILON_SIGNAL_PROPAGATION, 0.);
        Assert.assertEquals(1e-8, BodyShape.DEFAULT_DISTANCE_EPSILON, 0.);
        Assert.assertEquals(1e14, BodyShape.DIRECTION_FACTOR, 0.);
    }

    /**
     * @throws PatriusException
     *         if the precession-nutation model data embedded in the library cannot be read
     * @description Cover the deprecated methods.
     *
     * @testedMethod {@link ThreeAxisEllipsoid#getE2()}
     * @testedMethod {@link ThreeAxisEllipsoid#getG2()}
     * @testedMethod {@link ThreeAxisEllipsoid#getTransverseRadius()}
     * @testedMethod {@link ThreeAxisEllipsoid#getConjugateRadius()}
     * @testedMethod {@link ThreeAxisEllipsoid#getEquatorialRadius()}
     * @testedMethod {@link ThreeAxisEllipsoid#getFlattening()}
     * 
     * @testPassCriteria They should return an {@link UnsupportedOperationException}.
     * @deprecated since 4.13
     */
    @Test
    @Deprecated
    public void testDeprecated() throws PatriusException {

        // Initialization
        final CelestialBodyFrame frame = FramesFactory.getITRF();
        final double aRadius = Constants.WGS84_EARTH_EQUATORIAL_RADIUS;
        final double bRadius = aRadius + 50.;
        final double cRadius = aRadius - 100.;

        final ThreeAxisEllipsoid model = new ThreeAxisEllipsoid(aRadius, bRadius, cRadius, frame);

        // Unsupported methods (can be deleted once the deprecated methods are cleaned)
        try {
            model.getE2();
            Assert.fail();
        } catch (final UnsupportedOperationException e) {
            // Expected
            Assert.assertTrue(true);
        }
        try {
            model.getG2();
            Assert.fail();
        } catch (final UnsupportedOperationException e) {
            // Expected
            Assert.assertTrue(true);
        }
        try {
            model.getTransverseRadius();
            Assert.fail();
        } catch (final UnsupportedOperationException e) {
            // Expected
            Assert.assertTrue(true);
        }
        try {
            model.getConjugateRadius();
            Assert.fail();
        } catch (final UnsupportedOperationException e) {
            // Expected
            Assert.assertTrue(true);
        }
        try {
            model.getEquatorialRadius();
            Assert.fail();
        } catch (final UnsupportedOperationException e) {
            // Expected
            Assert.assertTrue(true);
        }
        try {
            model.getFlattening();
            Assert.fail();
        } catch (final UnsupportedOperationException e) {
            // Expected
            Assert.assertTrue(true);
        }
    }

    /**
     * @testType UT
     * 
     * @description Evaluate a one axis ellipsoid (reference) against a three axis ellipsoid with the same dimension.
     * 
     * @testPassCriteria the two ellipsoids produce the same results
     * @throws PatriusException
     *         if a frame problem occurs
     * 
     * @referenceVersion 4.13
     * 
     * @nonRegressionVersion 4.13
     */
    @Test
    public void threeAxisVsOneAxisEllipsoidTest() throws PatriusException {

        // Build the one axis ellipsoid (reference) and a three axis ellipsoid with the same dimension
        final OneAxisEllipsoid oneAxisEll = new OneAxisEllipsoid(6378137.0, 1. / 298.257222101,
            FramesFactory.getITRF(), "oneAxisEll");
        final ThreeAxisEllipsoid threeAxisEll = new ThreeAxisEllipsoid(oneAxisEll.getARadius(),
            oneAxisEll.getBRadius(), oneAxisEll.getCRadius(), FramesFactory.getITRF(), "threeAxisEll");

        // Environment initialization (data reused from others OneAxisEllipsoidTest class tests)
        final Frame frame = FramesFactory.getITRF();
        final AbsoluteDate origin = AbsoluteDate.J2000_EPOCH;

        final Vector3D point = new Vector3D(0., 93.7139699, 3.5930796);
        final Vector3D direction = new Vector3D(0., 1., 1.);
        final Line line = new Line(point, point.add(direction));

        final Vector3D point2 = new Vector3D(0., 1e8, 0.);
        final Vector3D direction2 = Vector3D.PLUS_I;
        final Line line2 = new Line(point2, point2.add(direction2));

        final double alt = 100.;

        final double transfertDuration = 10.;
        final double deltaxyz = 100.;
        final PVCoordinatesProvider occultedBody = new ConstantPVCoordinatesProvider(
            Vector3D.PLUS_J.scalarMultiply(deltaxyz), frame);
        final PVCoordinatesProvider observer = new PVCoordinatesProvider(){
            /** Serializable UID. */
            private static final long serialVersionUID = 3675470733301070434L;

            @Override
            public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) {
                // Duration since emission
                final double duration = date.durationFrom(origin.shiftedBy(-transfertDuration));
                return new PVCoordinates(new Vector3D(deltaxyz * (1 - duration / 10.), -transfertDuration
                        * Constants.SPEED_OF_LIGHT, deltaxyz * duration / 10.), Vector3D.ZERO);
            }

            @Override
            public Frame getNativeFrame(final AbsoluteDate date) {
                return FramesFactory.getGCRF();
            }
        };

        final double marginValue1 = 1E3;
        final double marginValue2 = 2.;

        // Evaluate the two ellipsoids one against the other (should provide the same results)
        // Evaluate the basic getters
        Assert.assertEquals(oneAxisEll.getARadius(), threeAxisEll.getARadius(), 0.);
        Assert.assertEquals(oneAxisEll.getBRadius(), threeAxisEll.getBRadius(), 0.);
        Assert.assertEquals(oneAxisEll.getCRadius(), threeAxisEll.getCRadius(), 0.);
        Assert.assertEquals(oneAxisEll.getEncompassingSphereRadius(), threeAxisEll.getEncompassingSphereRadius(), 0.);

        // Evaluate the intersection point methods
        EllipsoidPoint ep1 = oneAxisEll.getIntersectionPoint(line, point, frame, origin);
        EllipsoidPoint ep2 = threeAxisEll.getIntersectionPoint(line, point, frame, origin);
        Assert.assertEquals(ep1.getLLHCoordinates(), ep2.getLLHCoordinates());

        ep1 = oneAxisEll.getIntersectionPoint(line, point, frame, origin, alt);
        ep2 = threeAxisEll.getIntersectionPoint(line, point, frame, origin, alt);
        Assert.assertEquals(ep1.getLLHCoordinates(), ep2.getLLHCoordinates());

        final EllipsoidPoint[] ep1Bis = oneAxisEll.getIntersectionPoints(line, frame, origin);
        final EllipsoidPoint[] ep2Bis = threeAxisEll.getIntersectionPoints(line, frame, origin);
        Assert.assertTrue(ep1Bis.length > 0); // Make sure some points are evaluated
        Assert.assertEquals(ep1Bis.length, ep2Bis.length);
        for (int i = 0; i < ep1Bis.length; i++) {
            Assert.assertEquals(ep1Bis[i].getLLHCoordinates(), ep2Bis[i].getLLHCoordinates());
        }

        // Evaluate the apparent radius method
        final double apparentRadius1 = oneAxisEll.getApparentRadius(observer, origin, occultedBody,
            PropagationDelayType.INSTANTANEOUS);
        final double apparentRadius2 = threeAxisEll.getApparentRadius(observer, origin, occultedBody,
            PropagationDelayType.INSTANTANEOUS);
        Assert.assertEquals(apparentRadius1, apparentRadius2, 0.);

        // Evaluate the resize method
        OneAxisEllipsoid oneAxisEllResized = oneAxisEll.resize(MarginType.DISTANCE, marginValue1);
        ThreeAxisEllipsoid threeAxisEllResized = threeAxisEll.resize(MarginType.DISTANCE, marginValue1);

        Assert.assertEquals(oneAxisEllResized.getARadius(), threeAxisEllResized.getARadius(), 0.);
        Assert.assertEquals(oneAxisEllResized.getBRadius(), threeAxisEllResized.getBRadius(), 0.);
        Assert.assertEquals(oneAxisEllResized.getCRadius(), threeAxisEllResized.getCRadius(), 0.);

        oneAxisEllResized = oneAxisEll.resize(MarginType.SCALE_FACTOR, marginValue2);
        threeAxisEllResized = threeAxisEll.resize(MarginType.SCALE_FACTOR, marginValue2);

        Assert.assertEquals(oneAxisEllResized.getARadius(), threeAxisEllResized.getARadius(), 0.);
        Assert.assertEquals(oneAxisEllResized.getBRadius(), threeAxisEllResized.getBRadius(), 0.);
        Assert.assertEquals(oneAxisEllResized.getCRadius(), threeAxisEllResized.getCRadius(), 0.);

        // Evaluate the distanceTo method
        final double dist1 = oneAxisEll.distanceTo(line2, frame, origin);
        final double dist2 = threeAxisEll.distanceTo(line2, frame, origin);

        Assert.assertEquals(dist1, dist2, 0.);

        // Evaluate the closestPointTo methods
        final EllipsoidPoint closestPt1 = oneAxisEll.closestPointTo(point, frame, origin);
        final EllipsoidPoint closestPt2 = threeAxisEll.closestPointTo(point, frame, origin);

        Assert.assertEquals(closestPt1.getLLHCoordinates(), closestPt2.getLLHCoordinates());

        final EllipsoidPoint[] closestPt1Bis = oneAxisEll.closestPointTo(line, frame, origin);
        final EllipsoidPoint[] closestPt2Bis = threeAxisEll.closestPointTo(line, frame, origin);

        Assert.assertTrue(closestPt1Bis.length > 0); // Make sure some points are evaluated
        Assert.assertEquals(closestPt1Bis.length, closestPt2Bis.length);
        for (int i = 0; i < closestPt1Bis.length; i++) {
            Assert.assertEquals(closestPt1Bis[i].getLLHCoordinates(), closestPt2Bis[i].getLLHCoordinates());
        }

        // Evaluate the buildPoint methods
        EllipsoidPoint buildPt1 = oneAxisEll.buildPoint(point, frame, origin, "buildPt1");
        EllipsoidPoint buildPt2 = threeAxisEll.buildPoint(point, frame, origin, "buildPt2");

        Assert.assertEquals(buildPt1.getLLHCoordinates(), buildPt2.getLLHCoordinates());

        buildPt1 = oneAxisEll.buildPoint(LLHCoordinatesSystem.BODYCENTRIC_NORMAL, 0.2, 0.4, 12.6, "buildPt1");
        buildPt2 = threeAxisEll.buildPoint(LLHCoordinatesSystem.BODYCENTRIC_NORMAL, 0.2, 0.4, 12.6, "buildPt2");

        Assert.assertEquals(buildPt1.getLLHCoordinates().getLLHCoordinatesSystem(), buildPt2.getLLHCoordinates()
            .getLLHCoordinatesSystem());
        Assert.assertEquals(buildPt1.getLLHCoordinates().getLatitude(), buildPt2.getLLHCoordinates().getLatitude(),
            1e-10);
        Assert.assertEquals(buildPt1.getLLHCoordinates().getLongitude(), buildPt2.getLLHCoordinates().getLongitude(),
            1e-10);
        Assert.assertEquals(buildPt1.getLLHCoordinates().getHeight(), buildPt2.getLLHCoordinates().getHeight(), 1e-9);
    }

    /**
     * @testType UT
     * 
     * @testedMethod {@link ThreeAxisEllipsoid#getIntersectionPoint(Line, Vector3D, Frame, AbsoluteDate, double)}
     * 
     * @description Checks intersection between a line and an ellipsoid at a given altitude on two cases:
     *              <ul>
     *              <li>Intersection at altitude = 100m: altitude of computed points should be 100m (accuracy: 1E-3m)</li>
     *              <li>Intersection at altitude = 0m: altitude of computed points should be 0m (accuracy: 0m)</li>
     *              <li>Intersection at altitude = 1E-4m: altitude of computed points should be 0m (accuracy: 0m)</li>
     *              </ul>
     * 
     * @testPassCriteria altitude of computed points are as expected. Points are on the initial line.
     * 
     * @referenceVersion 4.13
     * 
     * @nonRegressionVersion 4.13
     */
    @Test
    public void intersectionPointsAltitudeTest() throws PatriusException {
        // Initialization
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final CelestialBodyFrame frame = FramesFactory.getITRF();
        final double aRadius = Constants.WGS84_EARTH_EQUATORIAL_RADIUS;
        final double bRadius = aRadius + 50.;
        final double cRadius = aRadius - 100.;
        final ThreeAxisEllipsoid model = new ThreeAxisEllipsoid(aRadius, bRadius, cRadius, frame);

        // Test with a random point and altitude = 100m
        final Vector3D point = new Vector3D(0., 93.7139699, 3.5930796);
        final Vector3D direction = new Vector3D(0., 1., 1.);
        final Line line = new Line(point, point.add(direction));
        final EllipsoidPoint ep = model.getIntersectionPoint(line, point, frame, date, 100.);
        Assert.assertEquals(ep.getLLHCoordinates().getHeight(), 100., 1.e-3);
        Assert.assertTrue(line.distance(ep.getPosition()) < 1e-8);

        // Test with a random point and altitude = 0m. Exact result is expected
        final Vector3D point2 = new Vector3D(0., 93.7139699, 3.5930796);
        final Vector3D direction2 = new Vector3D(0., 1., 1.);
        final Line line2 = new Line(point2, point2.add(direction2));
        final EllipsoidPoint ep2 = model.getIntersectionPoint(line2, point2, frame, date, 0.);
        Assert.assertEquals(ep2.getLLHCoordinates().getHeight(), 0., 0.);
        Assert.assertTrue(line2.distance(ep2.getPosition()) < 1e-8);

        // No intersection test
        final Vector3D point3 = new Vector3D(1e9, 5e9, 10e9);
        final Vector3D direction3 = new Vector3D(1., 1., 1.);
        final Line line3 = new Line(point3, point3.add(direction3));
        final EllipsoidPoint ep3 = model.getIntersectionPoint(line3, point3, frame, date, 0.1);
        Assert.assertNull(ep3);

        // Test with a random point and altitude = < eps. Exact result is expected (altitude should be 0)
        final Vector3D point4 = new Vector3D(0., 93.7139699, 3.5930796);
        final Vector3D direction4 = new Vector3D(0., 1., 1.);
        final Line line4 = new Line(point4, point4.add(direction4));
        final EllipsoidPoint ep4 = model.getIntersectionPoint(line4, point4, frame, date, 1e-15);
        Assert.assertEquals(ep4.getLLHCoordinates().getHeight(), 0., 1e-9);
        Assert.assertTrue(line4.distance(ep4.getPosition()) < 1e-8);
    }

    /**
     * @testType UT
     * 
     * @testedMethod {@link AbstractEllipsoidBodyShape#getApparentRadius(PVCoordinatesProvider, AbsoluteDate, PVCoordinatesProvider, PropagationDelayType)}
     * 
     * @description Check computation of apparent radius is correct depending on signal propagation. This test
     *              considers a nearly flat bodyshape with far observer and close occulted body. Occulted body is behind
     *              occulting body. Observer body moves from equatorial plane to
     *              polar plane in a time equal to signal propagation duration. For simplicity, case in in ICRF frame.
     * 
     * @testPassCriteria apparent radius is as expected (reference: math, absolute threshold: 0)
     * 
     * @referenceVersion 4.13
     * 
     * @nonRegressionVersion 4.13
     */
    @Test
    public void testGetApparentRadius() throws PatriusException {

        // Initialization
        final CelestialBodyFrame frame = FramesFactory.getICRF();
        final AbsoluteDate origin = AbsoluteDate.J2000_EPOCH;  // Reception date
        final double aRadius = Constants.WGS84_EARTH_EQUATORIAL_RADIUS;
        final double bRadius = aRadius + 50.;
        final double cRadius = aRadius - 100.;
        final double transfertDuration = 10.;
        final double deltaxyz = 100.;
        final ThreeAxisEllipsoid ellipsoid = new ThreeAxisEllipsoid(aRadius, bRadius, cRadius, frame);

        // Occulted body is behind (along J) occulting body
        final PVCoordinatesProvider occultedBody = new ConstantPVCoordinatesProvider(
            Vector3D.PLUS_J.scalarMultiply(deltaxyz), frame);
        final PVCoordinatesProvider occultedBody2 = new ConstantPVCoordinatesProvider(
            Vector3D.PLUS_J.scalarMultiply(transfertDuration * Constants.SPEED_OF_LIGHT), frame);

        // Observer is moving from equatorial to polar plane such that transfert time (10s) between this two planes is
        // equal to signal propagation duration between source and observer
        final PVCoordinatesProvider observer = new PVCoordinatesProvider(){
            /** Serializable UID. */
            private static final long serialVersionUID = 3675470733301070434L;

            @Override
            public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) {
                // Duration since emission
                final double duration = date.durationFrom(origin.shiftedBy(-transfertDuration));
                return new PVCoordinates(new Vector3D(deltaxyz * (1 - duration / 10.), -transfertDuration
                        * Constants.SPEED_OF_LIGHT, deltaxyz * duration / 10.), Vector3D.ZERO);
            }

            @Override
            public Frame getNativeFrame(final AbsoluteDate date) {
                return FramesFactory.getGCRF();
            }
        };

        final PVCoordinatesProvider observer2 = new PVCoordinatesProvider(){
            /** Serializable UID. */
            private static final long serialVersionUID = 2653776250918773014L;

            @Override
            public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) {
                // Duration since emission
                final double duration = date.durationFrom(origin.shiftedBy(-transfertDuration));
                return new PVCoordinates(new Vector3D(deltaxyz * (1 - duration / 10.), -deltaxyz, deltaxyz * duration
                        / 10.), Vector3D.ZERO);
            }

            @Override
            public Frame getNativeFrame(final AbsoluteDate date) {
                return FramesFactory.getGCRF();
            }
        };

        // No light speed taken into account (PropagationDelayType.INSTANTANEOUS)
        // At t = 0s, observer is along X axis of body, at t = 10s, observer is along Z axis of body
        final double radiusEquatorialInst = ellipsoid.getApparentRadius(observer, origin.shiftedBy(-transfertDuration),
            occultedBody, PropagationDelayType.INSTANTANEOUS);
        final double radiusPolarInst = ellipsoid.getApparentRadius(observer, origin, occultedBody,
            PropagationDelayType.INSTANTANEOUS);
        Assert.assertEquals(0., (aRadius - radiusEquatorialInst) / aRadius, 1e-9);
        Assert.assertEquals(0., (cRadius - radiusPolarInst) / cRadius, 1e-9);

        // Light speed is taken into account (PropagationDelayType.LIGHT_SPEED)
        // Observer at 10s-light from occulting body
        // Occulted body very close to occulting body
        // Received light passes over polar radius of occulting body
        // At signal reception, observer is along Z axis of body (polar radius = 500m)
        ellipsoid.setEpsilonSignalPropagation(1E-14);
        final double radiusLS1 = ellipsoid.getApparentRadius(observer, origin, occultedBody,
            PropagationDelayType.LIGHT_SPEED);
        Assert.assertEquals(0., (cRadius - radiusLS1) / cRadius, 1e-9);

        // Light speed is taken into account (PropagationDelayType.LIGHT_SPEED)
        // Observer very close to occulting body
        // Occulted body at 10s-light from occulting body
        // Received light passes over polar radius of occulting body
        // At signal reception, observer is along Z axis of body (polar radius = 500m)
        final double radiusLS2 = ellipsoid.getApparentRadius(observer2, origin, occultedBody2,
            PropagationDelayType.LIGHT_SPEED);
        Assert.assertEquals(0., (cRadius - radiusLS2) / cRadius, 1e-9);

        // For coverage (define the ellipsoid as a sphere)
        final ThreeAxisEllipsoid ellipsoidBis = new ThreeAxisEllipsoid(aRadius, aRadius, aRadius, frame);
        final double radiusBis = ellipsoidBis.getApparentRadius(observer, origin, occultedBody,
            PropagationDelayType.LIGHT_SPEED);
        Assert.assertEquals(aRadius, radiusBis, 0.);
    }

    /**
     * Test needed to validate the resize of a ThreeAxisEllipsoid.
     * 
     * @testedMethod {@link ThreeAxisEllipsoid#resize(MarginType, double)}
     * 
     * @throws PatriusException
     *         if the precession-nutation model data embedded in the library cannot be read or if the margin type is
     *         invalid
     */
    @Test
    public void testResize() throws PatriusException {

        // Initialization
        final CelestialBodyFrame frame = FramesFactory.getITRF();
        final double aRadius = Constants.WGS84_EARTH_EQUATORIAL_RADIUS;
        final double bRadius = aRadius + 50.;
        final double cRadius = aRadius - 100.;
        final ThreeAxisEllipsoid model = new ThreeAxisEllipsoid(aRadius, bRadius, cRadius, frame);
        final double tolerance = 1E-11;

        // Case with a zero margin distance
        final double marginValue2 = 0.;
        final ThreeAxisEllipsoid model2 = model.resize(MarginType.DISTANCE, marginValue2);
        Assert.assertEquals(aRadius, model2.getARadius(), tolerance);
        Assert.assertEquals(bRadius, model2.getBRadius(), tolerance);
        Assert.assertEquals(cRadius, model2.getCRadius(), tolerance);

        // Case with a positive margin distance
        final double marginValue3 = 1E3;
        final ThreeAxisEllipsoid model3 = model.resize(MarginType.DISTANCE, marginValue3);
        Assert.assertEquals(aRadius + marginValue3, model3.getARadius(), tolerance);
        Assert.assertEquals(bRadius + marginValue3, model3.getBRadius(), tolerance);
        Assert.assertEquals(cRadius + marginValue3, model3.getCRadius(), tolerance);

        // Case with a negative margin distance smaller than the opposite of the smallest radius (C)
        final double marginValue4 = -cRadius + 1E-3;
        final ThreeAxisEllipsoid model4 = model.resize(MarginType.DISTANCE, marginValue4);
        Assert.assertEquals(aRadius + marginValue4, model4.getARadius(), tolerance);
        Assert.assertEquals(bRadius + marginValue4, model4.getBRadius(), tolerance);
        Assert.assertEquals(cRadius + marginValue4, model4.getCRadius(), tolerance);

        // Case with a negative margin distance equal to the opposite of the smallest radius (C)
        final double marginValue5 = -cRadius;
        try {
            model.resize(MarginType.DISTANCE, marginValue5);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        // Case with a negative margin distance larger than the opposite of the smallest radius (C)
        final double marginValue6 = -cRadius - 1E-3;
        try {
            model.resize(MarginType.DISTANCE, marginValue6);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        // Case with a positive margin scale factor larger than 1
        final double marginValue7 = 2.;
        final ThreeAxisEllipsoid model7 = model.resize(MarginType.SCALE_FACTOR, marginValue7);
        Assert.assertEquals(aRadius * marginValue7, model7.getARadius(), tolerance);
        Assert.assertEquals(bRadius * marginValue7, model7.getBRadius(), tolerance);
        Assert.assertEquals(cRadius * marginValue7, model7.getCRadius(), tolerance);

        // Case with a positive margin scale factor equal to 1
        final double marginValue8 = 1.;
        final ThreeAxisEllipsoid model8 = model.resize(MarginType.SCALE_FACTOR, marginValue8);
        Assert.assertEquals(aRadius, model8.getARadius(), tolerance);
        Assert.assertEquals(bRadius, model8.getBRadius(), tolerance);
        Assert.assertEquals(cRadius, model8.getCRadius(), tolerance);

        // Case with a positive margin scale factor smaller than 1
        final double marginValue9 = 0.5;
        final ThreeAxisEllipsoid model9 = model.resize(MarginType.SCALE_FACTOR, marginValue9);
        Assert.assertEquals(aRadius * marginValue9, model9.getARadius(), tolerance);
        Assert.assertEquals(bRadius * marginValue9, model9.getBRadius(), tolerance);
        Assert.assertEquals(cRadius * marginValue9, model9.getCRadius(), tolerance);

        // Case with a margin scale factor equal to 0
        final double marginValue10 = 0.;
        try {
            model.resize(MarginType.SCALE_FACTOR, marginValue10);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        // Case with a negative margin scale factor
        final double marginValue11 = -0.5;
        try {
            model.resize(MarginType.SCALE_FACTOR, marginValue11);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
    }
}
