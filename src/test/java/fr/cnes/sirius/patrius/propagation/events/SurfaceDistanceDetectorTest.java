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
 * HISTORY
 * VERSION:4.11:DM:DM-17:22/05/2023:[PATRIUS] Detecteur de distance a la surface d'un corps celeste
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.events;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.EventDetector.Action;
import fr.cnes.sirius.patrius.propagation.events.SurfaceDistanceDetector.BodyDistanceType;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Unit tests for {@link SurfaceDistanceDetector}.
 * 
 * @since 4.11
 * 
 */
public class SurfaceDistanceDetectorTest {

    /** A Cartesian orbit used for the tests. */
    private static CartesianOrbit tISSOrbit;

    /** A SpacecraftState used for the tests. */
    private static SpacecraftState tISSSpState;

    /** The Sun. */
    private static CelestialBody theSun;

    /** The Earth. */
    private static CelestialBody theEarth;

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
        FramesFactory.setConfiguration(fr.cnes.sirius.patrius.Utils.getIERS2003Configuration(true));

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
        tISSSpState = new SpacecraftState(tISSOrbit);

        theEarth = CelestialBodyFactory.getEarth();

    }

    /**
     * @testType UT
     * 
     * @testedMethod {@link SurfaceDistanceDetector#SurfaceDistanceDetector (PVCoordinatesProvider, int, BodyDistanceType) }
     * 
     * @description simple constructor test
     * 
     * @input constructor parameters : reference body, distance
     * 
     * @output a {@link SurfaceDistanceDetector}
     * 
     * @testPassCriteria the {@link SurfaceDistanceDetector} is successfully created
     * 
     * @referenceVersion 4.11
     * 
     * @nonRegressionVersion 4.11
     */
    @Test
    public void testSurfaceDistanceDetectorCtor() {
        final SurfaceDistanceDetector detector =
            new SurfaceDistanceDetector(theSun, 12345., BodyDistanceType.CLOSEST);
        // The constructor did not crash...
        Assert.assertNotNull(detector);
    }

    /**
     * @testType UT
     * 
     * @testedMethod {@link SurfaceDistanceDetector#SurfaceDistanceDetector (PVCoordinatesProvider, int, BodyDistanceType, double, double)}
     * 
     * @description constructor test
     * 
     * @input constructor parameters : reference body, distance, max check, threshold
     * 
     * @output a {@link SurfaceDistanceDetector}
     * 
     * @testPassCriteria the {@link SurfaceDistanceDetector} is successfully created
     * 
     * @referenceVersion 4.11
     * 
     * @nonRegressionVersion 4.11
     */
    @Test
    public void testSurfaceDistanceDetectorCtor2() {
        final SurfaceDistanceDetector detector =
            new SurfaceDistanceDetector(theSun, 54321., BodyDistanceType.CLOSEST, 10, 0.1);
        // The constructor did not crash...
        Assert.assertNotNull(detector);
    }

    /**
     * @testType UT
     * 
     * @testedMethod {@link SurfaceDistanceDetector#SurfaceDistanceDetector (PVCoordinatesProvider, int, BodyDistanceType, double, double, Action)}
     * 
     * @description constructor test
     * 
     * @input constructor parameters : reference body, distance, max check, threshold and STOP.Action
     * 
     * @output a {@link SurfaceDistanceDetector}
     * 
     * @testPassCriteria the {@link SurfaceDistanceDetector} is successfully created
     * 
     * @referenceVersion 4.11
     * 
     * @nonRegressionVersion 4.11
     */
    @Test
    public void testSurfaceDistanceDetectorCtor3() {
        final SurfaceDistanceDetector detector =
            new SurfaceDistanceDetector(theSun, 54321., BodyDistanceType.CLOSEST, 10, 0.1, Action.STOP, Action.STOP);
        final SurfaceDistanceDetector detector2 = (SurfaceDistanceDetector) detector.copy();
        // Test getter
        Assert.assertEquals(54321., detector2.getDistance());
        Assert.assertEquals(theSun, detector2.getBody());
        Assert.assertEquals(theSun.getShape(), detector2.getBodyShape());
        Assert.assertEquals(theSun.getShape().getBodyFrame(), detector2.getBodyFixedFrame());
        Assert.assertEquals(BodyDistanceType.CLOSEST, detector2.getBodyDistanceType());
    }

    /**
     * @testType UT
     * 
     * @testedMethod {@link SurfaceDistanceDetector#SurfaceDistanceDetector (PVCoordinatesProvider, int, BodyDistanceType) }
     * 
     * @description constructor test without error
     * 
     * @input constructor parameters : reference body, negative distance
     * 
     * @output none
     * 
     * @testPassCriteria the {@link SurfaceDistanceDetector} should be created
     * 
     * @referenceVersion 4.11
     * 
     * @nonRegressionVersion 4.11
     */
    @Test
    public void testSurfaceDistanceDetectorCtorNoError() {
        try {
            // Negative distance in input
            new SurfaceDistanceDetector(theSun, -1., BodyDistanceType.CLOSEST);
            // No exception should be thrown, so we should reach the next line
            Assert.assertTrue(true);
        } catch (final IllegalArgumentException e) {
            Assert.fail();
        }
    }

    /**
     * @testType UT
     * 
     * @testedMethod {@link SurfaceDistanceDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * 
     * @description tests {@link SurfaceDistanceDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * 
     * @input constructor and eventOccured parameters
     * 
     * @output eventOccured outputs
     * 
     * @testPassCriteria eventOccured returns the expected values for true and false as second parameters
     * 
     * @referenceVersion 4.11
     * 
     * @nonRegressionVersion 4.11
     * 
     * @throws PatriusException should not happen here
     */
    @Test
    public void testEventOccurred() throws PatriusException {
        final SurfaceDistanceDetector detector1 =
            new SurfaceDistanceDetector(theSun, 2121., BodyDistanceType.CLOSEST);
        final SurfaceDistanceDetector detector2 =
            new SurfaceDistanceDetector(theSun, 2121., BodyDistanceType.RADIAL);
        // Distance is decreasing
        Action rez1 = detector1.eventOccurred(tISSSpState, false, true);
        Assert.assertEquals(Action.STOP, rez1);
        Action rez2 = detector2.eventOccurred(tISSSpState, false, true);
        Assert.assertEquals(Action.STOP, rez2);
        // Distance is increasing
        rez1 = detector1.eventOccurred(tISSSpState, true, true);
        Assert.assertEquals(Action.STOP, rez1);
        rez2 = detector2.eventOccurred(tISSSpState, true, true);
        Assert.assertEquals(Action.STOP, rez2);
    }

    /**
     * @testType UT
     * 
     * @testedMethod {@link SurfaceDistanceDetector#g(SpacecraftState)}
     * 
     * @description tests {@link SurfaceDistanceDetector#g(SpacecraftState)}
     * 
     * @input constructor and g parameters
     * 
     * @output g output
     * 
     * @testPassCriteria g returns the expected value
     * 
     * @referenceVersion 4.11
     * 
     * @nonRegressionVersion 4.11
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @Test
    public void testG() throws PatriusException {
        // Orekit initialization specific for this test
        Utils.setDataRoot("regular-dataCNES-2003");
        FramesFactory.setConfiguration(fr.cnes.sirius.patrius.Utils.getIERS2003ConfigurationWOEOP(true));
        theEarth.setShape(new OneAxisEllipsoid(6378137.0, 1.0 / 298.257222101, FramesFactory.getITRF()));

        // 400 km
        final double targetDistance = 400000.;
        final Frame bodyFrame = theEarth.getShape().getBodyFrame();
        final Vector3D posBody = tISSSpState.getPVCoordinates(bodyFrame).getPosition();

        // CLOSEST case

        final SurfaceDistanceDetector detector1 =
            new SurfaceDistanceDetector(theEarth, targetDistance, BodyDistanceType.CLOSEST,
                0.05 * tISSOrbit.getKeplerianPeriod(), AbstractDetector.DEFAULT_THRESHOLD);
        final GeodeticPoint point = theEarth.getShape().transform(posBody,
            bodyFrame, tISSSpState.getDate());
        // We basically reimplement g...
        final double expectedAd1 = point.getAltitude() - targetDistance;
        Assert.assertEquals(expectedAd1, detector1.g(tISSSpState), 0.);

        // RADIAL case

        final SurfaceDistanceDetector detector2 =
            new SurfaceDistanceDetector(theEarth, targetDistance, BodyDistanceType.RADIAL,
                0.05 * tISSOrbit.getKeplerianPeriod(), AbstractDetector.DEFAULT_THRESHOLD);
        // We basically reimplement g...
        final Vector3D intersectionPoint = theEarth.getShape().transform(
            theEarth.getShape().getIntersectionPoint(new Line(Vector3D.ZERO, posBody), posBody,
                bodyFrame, tISSSpState.getDate()));
        final double expectedAd2 = intersectionPoint.distance(posBody) - targetDistance;
        Assert.assertEquals(expectedAd2, detector2.g(tISSSpState), 0.);

        // Reinitialization specific for all the other tests
        Utils.setDataRoot("regular-dataCNES-2003/de406-ephemerides");
        FramesFactory.setConfiguration(Utils.getIERS2003Configuration(true));
        theEarth = CelestialBodyFactory.getEarth();
    }

    /**
     * @testType UT
     * 
     * @testedMethod {@link SurfaceDistanceDetector#getDistance()}
     * 
     * @description tests getDistance()
     * 
     * @input constructor parameters
     * 
     * @output getDistance output
     * 
     * @testPassCriteria getDistance returns the expected value
     * 
     * @referenceVersion 4.11
     * 
     * @nonRegressionVersion 4.11
     * 
     * @throws PatriusException should not happen here
     */
    @Test
    public void testGetDistance() throws PatriusException {
        final double targetDistance1 = 400000.;
        final SurfaceDistanceDetector detector1 =
            new SurfaceDistanceDetector(theEarth, targetDistance1, BodyDistanceType.CLOSEST,
                0.05 * tISSOrbit.getKeplerianPeriod(), AbstractDetector.DEFAULT_THRESHOLD);
        Assert.assertEquals(targetDistance1, detector1.getDistance(), 0.);

        final double targetDistance2 = 400000.;
        final SurfaceDistanceDetector detector2 =
            new SurfaceDistanceDetector(theEarth, targetDistance2, BodyDistanceType.RADIAL,
                0.05 * tISSOrbit.getKeplerianPeriod(), AbstractDetector.DEFAULT_THRESHOLD);
        Assert.assertEquals(targetDistance2, detector2.getDistance(), 0.);
    }

    /**
     * @testType UT
     * 
     * @testedMethod {@link SurfaceDistanceDetector#getBodyShape()}
     * 
     * @description tests getBodyShape()
     * 
     * @input constructor parameters
     * 
     * @output getBodyShape output
     * 
     * @testPassCriteria getBodyShape returns the expected value
     * 
     * @referenceVersion 4.11
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void testGetBodyShape() {
        final double targetDistance = 400000.;
        final SurfaceDistanceDetector detector =
            new SurfaceDistanceDetector(theEarth, targetDistance, BodyDistanceType.CLOSEST,
                0.05 * tISSOrbit.getKeplerianPeriod(), AbstractDetector.DEFAULT_THRESHOLD);
        Assert.assertEquals(theEarth.getShape(), detector.getBodyShape());
    }

    /**
     * @testType UT
     * 
     * @testedMethod {@link SurfaceDistanceDetector#getBody()}
     * 
     * @description tests getBody()
     * 
     * @input constructor parameters
     * 
     * @output getBody output
     * 
     * @testPassCriteria getBody returns the expected value
     * 
     * @referenceVersion 4.11
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void testGetBody() {
        final double targetDistance = 400000.;
        final SurfaceDistanceDetector detector =
            new SurfaceDistanceDetector(theEarth, targetDistance, BodyDistanceType.CLOSEST,
                0.05 * tISSOrbit.getKeplerianPeriod(), AbstractDetector.DEFAULT_THRESHOLD);
        Assert.assertEquals(theEarth, detector.getBody());
    }
}
