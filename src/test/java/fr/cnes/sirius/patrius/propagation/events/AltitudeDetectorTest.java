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
 * @history created 14/11/11
 * 
 * HISTORY
 * VERSION:4.5:DM:DM-2460:27/05/2020:Prise en compte des temps de propagation dans les calculs evenements
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 *                             (added forward parameter to eventOccurred signature)
 * VERSION::DM:394:20/02/2015: Added possibility to define the action performed when event occurs
 * VERSION::DM:454:24/11/2015:Unimplemented method shouldBeRemoved()
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * END-HISTORY
 * 
 */
package fr.cnes.sirius.patrius.propagation.events;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.EventDetector.Action;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Unit tests for {@link AltitudeDetector}.<br>
 * Note : unit test also written for code coverage, including AbstractDetector.
 * 
 * @author cardosop
 * 
 * @version $Id$
 * 
 * @since 1.1
 * 
 */
public class AltitudeDetectorTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Validate the altitude detector
         * 
         * @featureDescription Validate the altitude detector
         * 
         * @coveredRequirements DV-EVT_120, DV-EVT_50
         */
        VALIDATE_ALTITUDE_DETECTOR
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
     * Earth's bodyshape.
     */
    private static BodyShape earthBodyShape;

    @After
    public void tearDown() throws PatriusException {
        FramesFactory.setConfiguration(Utils.getIERS2003Configuration(true));
    }

    /**
     * Setup for all unit tests in the class.
     * Provides an {@link Orbit}, a {@link SpacecraftState} and a {@link BodyShape} for Earth.
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @Before
    public void setUpBeforeClass() throws PatriusException {

        // Orekit initialization
        Utils.setDataRoot("regular-dataCNES-2003");
        FramesFactory.setConfiguration(fr.cnes.sirius.patrius.Utils.getIERS2003ConfigurationWOEOP(true));

        // Some orbit data for the tests
        // (Real ISS data!)
        final double ix = 2156444.05;
        final double iy = 3611777.68;
        final double iz = -5316875.46;
        final double ivx = -6579.446110;
        final double ivy = 3916.478783;
        final double ivz = 8.876119;
        final AbsoluteDate date = new AbsoluteDate("2011-11-09T12:00:00Z", TimeScalesFactory.getTT());

        final double mu = CelestialBodyFactory.getEarth().getGM();

        final Vector3D issPos = new Vector3D(ix, iy, iz);
        final Vector3D issVit = new Vector3D(ivx, ivy, ivz);
        final PVCoordinates pvCoordinates = new PVCoordinates(issPos, issVit);
        tISSOrbit = new CartesianOrbit(pvCoordinates, FramesFactory.getEME2000(), date, mu);

        tISSSpState = new SpacecraftState(tISSOrbit);

        // Earth body shape as found in many Orekit unit tests
        earthBodyShape =
            new OneAxisEllipsoid(6378137.0, 1.0 / 298.257222101, FramesFactory.getITRF());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_ALTITUDE_DETECTOR}
     * 
     * @testedMethod {@link AltitudeDetector#AltitudeDetector(double, BodyShape)}
     * 
     * @description simple constructor test
     * 
     * @input constructor parameters : altitude, BodyShape
     * 
     * @output an AltitudeDetector
     * 
     * @testPassCriteria the AltitudeDetector is successfully created
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public final void testAltitudeDetectorDoubleBodyShape() {
        final double targetAlt = 400000.;
        final AltitudeDetector detector =
            new AltitudeDetector(
                targetAlt, earthBodyShape);
        // The constructor did not crash...
        Assert.assertNotNull(detector);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_ALTITUDE_DETECTOR}
     * 
     * @testedMethod {@link AltitudeDetector#AltitudeDetector(double, BodyShape, double)}
     * 
     * @description simple constructor test
     * 
     * @input constructor parameters : max check, altitude, BodyShape
     * 
     * @output an AltitudeDetector
     * 
     * @testPassCriteria the AltitudeDetector is successfully created
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    public final void testAltitudeDetectorDoubleDoubleBodyShape() {
        final double targetAlt = 400000.;
        final AltitudeDetector detector =
            new AltitudeDetector(targetAlt,
                earthBodyShape, 0.05 * tISSOrbit.getKeplerianPeriod());
        // The constructor did not crash...
        Assert.assertNotNull(detector);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_ALTITUDE_DETECTOR}
     * 
     * @testedMethod {@link AltitudeDetector#AltitudeDetector(double, BodyShape, double, double)}
     * 
     * @description simple constructor test
     * 
     * @input constructor parameters : max check, threshold, altitude, BodyShape
     * 
     * @output an AltitudeDetector
     * 
     * @testPassCriteria the AltitudeDetector is successfully created
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public final void testAltitudeDetectorDoubleDoubleDoubleBodyShape() {
        final double targetAlt = 400000.;
        final AltitudeDetector detector =
            new AltitudeDetector(targetAlt, earthBodyShape,
                AbstractDetector.DEFAULT_MAXCHECK, 0.05 * tISSOrbit.getKeplerianPeriod());
        // The constructor did not crash...
        Assert.assertNotNull(detector);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_ALTITUDE_DETECTOR}
     * 
     * @testedMethod {@link AltitudeDetector#AltitudeDetector(double, BodyShape, double, double, Action, Action)}
     * 
     * @description simple constructor test
     * 
     * @input constructor parameters : max check, threshold, altitude, BodyShape, action, action
     * 
     * @output an AltitudeDetector
     * 
     * @testPassCriteria the AltitudeDetector is successfully created
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public final void testAltitudeDetectorDoubleDoubleDoubleBodyShapeActionAction() throws PatriusException {
        final double targetAlt = 400000.;
        final AltitudeDetector detector =
            new AltitudeDetector(targetAlt, earthBodyShape,
                AbstractDetector.DEFAULT_MAXCHECK, 0.05 * tISSOrbit.getKeplerianPeriod(), Action.CONTINUE,
                Action.STOP);
        final AltitudeDetector detector2 = (AltitudeDetector) detector.copy();
        // The constructor did not crash...
        Assert.assertNotNull(detector);
        // Check actions
        Assert.assertEquals(Action.CONTINUE, detector2.eventOccurred(tISSSpState, true, true));
        Assert.assertEquals(Action.STOP, detector2.eventOccurred(tISSSpState, false, true));

        final AltitudeDetector detector3 = (AltitudeDetector)
            new AltitudeDetector(targetAlt, earthBodyShape,
                0, AbstractDetector.DEFAULT_MAXCHECK, AbstractDetector.DEFAULT_THRESHOLD, Action.RESET_DERIVATIVES,
                false).copy();
        final AltitudeDetector detector4 = (AltitudeDetector)
            new AltitudeDetector(targetAlt, earthBodyShape,
                1, AbstractDetector.DEFAULT_MAXCHECK, AbstractDetector.DEFAULT_THRESHOLD, Action.STOP,
                false).copy();
        final AltitudeDetector detector5 = (AltitudeDetector)
                new AltitudeDetector(targetAlt, earthBodyShape,
                    2, AbstractDetector.DEFAULT_MAXCHECK, AbstractDetector.DEFAULT_THRESHOLD, Action.STOP,
                    false).copy();
        Assert.assertEquals(Action.RESET_DERIVATIVES, detector3.eventOccurred(tISSSpState, true, true));
        Assert.assertEquals(Action.STOP, detector4.eventOccurred(tISSSpState, false, true));
        Assert.assertEquals(Action.STOP, detector5.eventOccurred(tISSSpState, false, true));
        Assert.assertEquals(Action.STOP, detector5.eventOccurred(tISSSpState, true, true));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_ALTITUDE_DETECTOR}
     * 
     * @testedMethod {@link AltitudeDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * 
     * @description tests eventOccurred(SpacecraftState, boolean)
     * 
     * @input constructor and eventOccured parameters
     * 
     * @output eventOccured outputs
     * 
     * @testPassCriteria eventOccured returns the expected values for true and false as second parameters
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
        final AltitudeDetector detector =
            new AltitudeDetector(400000,
                earthBodyShape, 0.05 * tISSOrbit.getKeplerianPeriod());
        Action rez = detector.eventOccurred(tISSSpState, true, true);
        Assert.assertEquals(Action.CONTINUE, rez);
        rez = detector.eventOccurred(tISSSpState, false, true);
        Assert.assertEquals(Action.STOP, rez);

        Assert.assertEquals(Action.CONTINUE, detector.getActionAtAscending());
        Assert.assertEquals(Action.STOP, detector.getActionAtDescending());
        Assert.assertEquals(false, detector.removeAtAscending());
        Assert.assertEquals(false, detector.removeAtDescending());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_ALTITUDE_DETECTOR}
     * 
     * @testedMethod {@link AltitudeDetector#g(SpacecraftState)}
     * 
     * @description tests g(SpacecraftState)
     * 
     * @input constructor and g parameters
     * 
     * @output g output
     * 
     * @testPassCriteria g returns the expected value
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @Test
    public final void testG() throws PatriusException {
        // 400 km
        final double targetAlt = 400000.;
        final AltitudeDetector detector =
            new AltitudeDetector(targetAlt,
                earthBodyShape, 0.05 * tISSOrbit.getKeplerianPeriod());
        // We basically reimplement g...
        final Frame bodyFrame = earthBodyShape.getBodyFrame();
        final PVCoordinates pvBody = tISSSpState.getPVCoordinates(bodyFrame);
        final GeodeticPoint point = earthBodyShape.transform(pvBody.getPosition(),
            bodyFrame, tISSSpState.getDate());
        final double expectedAd = point.getAltitude() - targetAlt;
        Assert.assertEquals(expectedAd, detector.g(tISSSpState), 0.);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_ALTITUDE_DETECTOR}
     * 
     * @testedMethod {@link AltitudeDetector#getAltitude()}
     * 
     * @description tests getAltitude()
     * 
     * @input constructor parameters
     * 
     * @output getAltitude output
     * 
     * @testPassCriteria getAltitude returns the expected value
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @Test
    public final void testGetAltitude() throws PatriusException {
        final double targetAlt = 400000.;
        final AltitudeDetector detector =
            new AltitudeDetector(targetAlt,
                earthBodyShape, 0.05 * tISSOrbit.getKeplerianPeriod());
        Assert.assertEquals(targetAlt, detector.getAltitude(), 0.);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_ALTITUDE_DETECTOR}
     * 
     * @testedMethod {@link AltitudeDetector#getBodyShape()}
     * 
     * @description tests getBodyShape()
     * 
     * @input constructor parameters
     * 
     * @output getBodyShape output
     * 
     * @testPassCriteria getBodyShape returns the expected value
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public final void testGetBodyShape() {
        final double targetAlt = 400000.;
        final AltitudeDetector detector =
            new AltitudeDetector(targetAlt,
                earthBodyShape, 0.05 * tISSOrbit.getKeplerianPeriod());
        Assert.assertEquals(earthBodyShape, detector.getBodyShape());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_ALTITUDE_DETECTOR}
     * 
     * @testedMethod {@link AltitudeDetector#getMaxCheckInterval()}
     * 
     * @description tests getMaxCheckInterval()
     * 
     * @input constructor parameters
     * 
     * @output getMaxCheckInterval output
     * 
     * @testPassCriteria getMaxCheckInterval returns the expected value
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public final void testGetMaxCheckInterval() {
        final double targetAlt = 400000.;
        // expected check
        final double expectedChk = 655.957;
        final AltitudeDetector detector =
            new AltitudeDetector(targetAlt, earthBodyShape,
                expectedChk, 0.05 * tISSOrbit.getKeplerianPeriod());
        Assert.assertEquals(expectedChk, detector.getMaxCheckInterval());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_ALTITUDE_DETECTOR}
     * 
     * @testedMethod {@link AltitudeDetector#getMaxIterationCount()}
     * 
     * @description tests getMaxIterationCount()
     * 
     * @input constructor parameters
     * 
     * @output getMaxIterationCount output
     * 
     * @testPassCriteria getMaxIterationCount returns the expected value
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public final void testGetMaxIterationCount() {
        final int expectedIc = 100;
        final double targetAlt = 400000.;
        final AltitudeDetector detector =
            new AltitudeDetector(targetAlt,
                earthBodyShape, 0.05 * tISSOrbit.getKeplerianPeriod());
        Assert.assertEquals(expectedIc, detector.getMaxIterationCount());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_ALTITUDE_DETECTOR}
     * 
     * @testedMethod {@link AltitudeDetector#getThreshold()}
     * 
     * @description tests getThreshold()
     * 
     * @input constructor parameters
     * 
     * @output getThreshold output
     * 
     * @testPassCriteria getThreshold returns the expected value
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public final void testGetThreshold() {
        final double targetAlt = 400000.;
        final double expectedThr = 0.000123;
        final AltitudeDetector detector =
            new AltitudeDetector(targetAlt,
                earthBodyShape, 0.05 * tISSOrbit.getKeplerianPeriod(), expectedThr);
        Assert.assertEquals(expectedThr, detector.getThreshold());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_ALTITUDE_DETECTOR}
     * 
     * @testedMethod {@link AltitudeDetector#resetState(SpacecraftState)}
     * 
     * @description tests resetState(SpacecraftState)
     * 
     * @input constructor and resetState parameters
     * 
     * @output resetState output
     * 
     * @testPassCriteria resetState returns the expected value according to the input
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @Test
    public final void testResetState() throws PatriusException {
        // The current resetState implementation
        // does nothing useful, it just returns
        // the input parameter
        final SpacecraftState otherISSState = new SpacecraftState(tISSOrbit);
        final double targetAlt = 400000.;
        final AltitudeDetector detector =
            new AltitudeDetector(targetAlt,
                earthBodyShape, 0.05 * tISSOrbit.getKeplerianPeriod());
        SpacecraftState rezState = detector.resetState(tISSSpState);
        Assert.assertEquals(tISSSpState, rezState);
        rezState = detector.resetState(otherISSState);
        Assert.assertEquals(otherISSState, rezState);
    }

}