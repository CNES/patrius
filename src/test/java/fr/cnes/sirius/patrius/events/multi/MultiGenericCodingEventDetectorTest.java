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
 * @history created 18/03/2015
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:300:18/03/2015:Creation multi propagator
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events.multi;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.LofOffset;
import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.events.CodedEvent;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.frames.TopocentricFrame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.CircularFieldOfViewDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector.Action;
import fr.cnes.sirius.patrius.propagation.events.multi.MultiEventDetector;
import fr.cnes.sirius.patrius.propagation.events.multi.OneSatEventDetectorWrapper;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * Unit tests for {@link MultiGenericCodingEventDetector}.<br>
 * </p>
 * 
 * @author maggioranic
 * 
 * @version $Id$
 * 
 * @since 3.0
 * 
 */
public class MultiGenericCodingEventDetectorTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Validate the multi generic coding event detector
         * 
         * @featureDescription Validate the multi generic coding event detector
         */
        VALIDATE_MULTI_GENERIC_CODING_EVENT_DETECTOR
    }

    /**
     * Sat Id.
     */
    private static final String STATE1 = "state1";

    /**
     * A visibility detector.
     */
    private static MultiEventDetector visi;

    /**
     * A spacecraft state.
     */
    private static Map<String, SpacecraftState> state;

    /**
     * A date.
     */
    private static AbsoluteDate date;

    /**
     * Setup for all unit tests in the class.
     * Provides two strings, a {@link AbsoluteDate} and a boolean.
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @Before
    public void setUp() throws PatriusException {
        // Orekit initialization
        Utils.setDataRoot("regular-dataPBASE");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));

        // Set up the visibility event:
        // Circular field of view detector
        final GeodeticPoint coord = new GeodeticPoint(
            MathLib.toRadians(5), MathLib.toRadians(120), 0);
        final Frame frame = FramesFactory.getITRF();
        final OneAxisEllipsoid body = new OneAxisEllipsoid(6378136.460, 1 / 298.257222101, frame);
        final TopocentricFrame station = new TopocentricFrame(body, coord, "Station1");
        final Vector3D center = Vector3D.PLUS_I;
        final double aperture = MathLib.toRadians(35);
        visi = new OneSatEventDetectorWrapper(new CircularFieldOfViewDetector(station, center, aperture, 0.1), STATE1);

        // Set up the spacecraft state:
        date = new AbsoluteDate(2000, 1, 1, TimeScalesFactory.getTT());
        final Orbit orbit = new KeplerianOrbit(1e07, 0.1, 0.1, 0, 0,
            0, PositionAngle.MEAN, FramesFactory.getGCRF(), date,
            Constants.EGM96_EARTH_MU);
        final Attitude attitude = new LofOffset(orbit.getFrame(), LOFType.LVLH).getAttitude(orbit, orbit.getDate(),
            orbit.getFrame());
        state = new HashMap<>();
        state.put(STATE1, new SpacecraftState(orbit, attitude));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_MULTI_GENERIC_CODING_EVENT_DETECTOR}
     * 
     * @testedMethod {@link MultiGenericCodingEventDetector#MultiGenericCodingEventDetector(MultiEventDetector, String, String, boolean, String)}
     * 
     * @description simple constructor test
     * 
     * @input constructor parameters : {@link MultiEventDetector}, code "increasing",
     *        code "decreasing", boolean, code "phenomenon"
     * 
     * @output an {@link MultiGenericCodingEventDetector}
     * 
     * @testPassCriteria the {@link MultiGenericCodingEventDetector} is successfully
     *                   created
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testMultiGenericCodingEventPhen() {
        // The MultiCodingEventDetector is created:
        final MultiGenericCodingEventDetector detector =
            new MultiGenericCodingEventDetector(visi, "ENTER", "EXIT", true, "Visibility");
        // Check the constructor did not crash:
        Assert.assertNotNull(detector);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_MULTI_GENERIC_CODING_EVENT_DETECTOR}
     * 
     * @testedMethod {@link MultiGenericCodingEventDetector#MultiGenericCodingEventDetector(MultiEventDetector, String, String)}
     * 
     * @description simple constructor test
     * 
     * @input constructor parameters : {@link MultiEventDetector}, code "increasing",
     *        code "decreasing"
     * 
     * @output an {@link MultiGenericCodingEventDetector}
     * 
     * @testPassCriteria the {@link MultiGenericCodingEventDetector} is successfully
     *                   created
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testMultiGenericCodingEventNoPhen() {

        // The CodingEventDetector is created:
        final MultiGenericCodingEventDetector detector =
            new MultiGenericCodingEventDetector(visi, "ENTER", "EXIT");
        // Check the constructor did not crash:
        Assert.assertNotNull(detector);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_MUTLI_GENERIC_CODING_EVENT_DETECTOR}
     * 
     * @testedMethod {@link MultiGenericCodingEventDetector#MultiGenericCodingEventDetector(MultiEventDetector, String, String, boolean, String, double, int)}
     * @testedMethod {@link MultiGenericCodingEventDetector#MultiGenericCodingEventDetector(MultiEventDetector, String, String, double, int)}
     * 
     * @description simple constructor test
     * 
     * @input constructor parameters
     * 
     * @output some {@link MultiGenericCodingEventDetector}
     * 
     * @testPassCriteria the {@link MultiGenericCodingEventDetector} are successfully
     *                   created
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testMultiGenericCodingEventDelayOcc() {
        // The MultiCodingEventDetector is created:
        final MultiGenericCodingEventDetector detector1 =
            new MultiGenericCodingEventDetector(visi, "ENTER", "EXIT", 0, 0);
        // Check the constructor did not crash:
        Assert.assertNotNull(detector1);
        // The MultiCodingEventDetector is created:
        final MultiGenericCodingEventDetector detector2 =
            new MultiGenericCodingEventDetector(visi, "ENTER", "EXIT", 756.32, 0);
        // Check the constructor did not crash:
        Assert.assertNotNull(detector2);
        // The MultiCodingEventDetector is created:
        final MultiGenericCodingEventDetector detector3 =
            new MultiGenericCodingEventDetector(visi, "ENTER", "EXIT", true, "VISI", 0, 0);
        // Check the constructor did not crash:
        Assert.assertNotNull(detector3);
    }

    /**
     * @throws PatriusException
     *         from the wrapped event detector.
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_MULTI_GENERIC_CODING_EVENT_DETECTOR}
     * 
     * @testedMethod {@link MultiGenericCodingEventDetector#eventOccurred(java.util.Map, boolean, boolean)}
     * @testedMethod {@link MultiGenericCodingEventDetector#g(java.util.Map)}
     * @testedMethod {@link MultiGenericCodingEventDetector#resetState(SpacecraftState)}
     * @testedMethod {@link MultiGenericCodingEventDetector#getThreshold()}
     * @testedMethod {@link MultiGenericCodingEventDetector#getMaxCheckInterval()}
     * @testedMethod {@link MultiGenericCodingEventDetector#getMaxIterationCount()}
     * 
     * @description tests all the overridden methods of {@link MultiEventDetector} using the
     *              {@link CircularFieldOfViewDetector}
     * 
     * @input {@link MultiGenericCodingEventDetector} constructor parameters,
     *        and a {@link SpacecraftState}
     * 
     * @output a {@link MultiGenericCodingEventDetector}
     * 
     * @testPassCriteria all the methods return the expected values
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testOverriddenMethods() throws PatriusException {
        // The MultiCodingEventDetector is created:
        final MultiGenericCodingEventDetector detector =
            new MultiGenericCodingEventDetector(visi, "ENTER", "EXIT");

        // Test eventOccurred method
        Action actual = detector.eventOccurred(state, true, true);
        Assert.assertEquals(Action.CONTINUE, actual);
        actual = detector.eventOccurred(state, false, true);
        Assert.assertEquals(Action.STOP, actual);
        actual = detector.eventOccurred(state, true, false);
        Assert.assertEquals(Action.CONTINUE, actual);
        actual = detector.eventOccurred(state, false, false);
        Assert.assertEquals(Action.STOP, actual);

        // Test g method
        final double expectedG = visi.g(state);
        Assert.assertEquals(expectedG, detector.g(state), 0.);

        // Test resetState method
        final AbsoluteDate date0 = new AbsoluteDate(2000, 1, 15, TimeScalesFactory.getTT());
        final Orbit orbit0 = new KeplerianOrbit(1e07, 0.1, 0.1, 0, 0,
            FastMath.PI, PositionAngle.MEAN, FramesFactory.getGCRF(), date0,
            Constants.EGM96_EARTH_MU);
        final SpacecraftState state0 = new SpacecraftState(orbit0);
        final Map<String, SpacecraftState> states0 = new HashMap<>();
        states0.put(STATE1, state0);
        final Map<String, SpacecraftState> expectedState = visi.resetStates(states0);
        Assert.assertEquals(expectedState, detector.resetStates(states0));

        // Test getThreshold
        final double expectedThreshold = visi.getThreshold();
        Assert.assertEquals(expectedThreshold, detector.getThreshold(), 0.);

        // Test getMaxCheckInterval
        final double maxCheck = visi.getMaxCheckInterval();
        Assert.assertEquals(maxCheck, detector.getMaxCheckInterval(), 0.);

        // Test getMaxIterationCount
        final double maxIteration = visi.getMaxIterationCount();
        Assert.assertEquals(maxIteration, detector.getMaxIterationCount(), 0.);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_MULTI_GENERIC_CODING_EVENT_DETECTOR}
     * 
     * @testedMethod {@link MultiGenericCodingEventDetector#buildCodedEvent(Map, boolean)}
     * 
     * @description tests {@link MultiGenericCodingEventDetector#buildCodedEvent(Map, boolean)} using the
     *              {@link CircularFieldOfViewDetector}
     * 
     * @input {@link MultiGenericCodingEventDetector} and {@link CodedEvent} constructor
     *        parameters
     * 
     * @output two {@link CodedEvent} (one when g is increasing and one when is
     *         decreasing)
     * 
     * @testPassCriteria the method returns the expected CodedEvent when g is
     *                   increasing and when g is decreasing
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testBuildCodedEvent() {
        // The CodingEventDetector is created:
        final MultiGenericCodingEventDetector detector =
            new MultiGenericCodingEventDetector(visi, "ENTER", "EXIT", true,
                "Visibility");
        // Expected CodedEvent when increasing
        final CodedEvent expected1 = new CodedEvent(
            "ENTER", "generic event", date, true);
        // Expected CodedEvent when decreasing
        final CodedEvent expected2 = new CodedEvent(
            "EXIT", "generic event", date, false);

        Assert.assertEquals(expected1, detector.buildCodedEvent(state, true));
        Assert.assertEquals(expected2, detector.buildCodedEvent(state, false));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_MULTI_GENERIC_CODING_EVENT_DETECTOR}
     * 
     * @testedMethod {@link MultiGenericCodingEventDetector#positiveSignMeansActive()}
     * 
     * @description tests {@link MultiGenericCodingEventDetector#positiveSignMeansActive()} using the
     *              {@link CircularFieldOfViewDetector}
     * 
     * @input {@link MultiGenericCodingEventDetector} constructor parameters
     * 
     * @output the flag increasingIsStart
     * 
     * @testPassCriteria the method returns the expected boolean
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testPositiveSignMeansActive() {
        // The CodingEventDetector is created:
        final MultiGenericCodingEventDetector detector =
            new MultiGenericCodingEventDetector(visi, "ENTER", "EXIT", false,
                "Visibility");

        Assert.assertEquals(false, detector.positiveSignMeansActive());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_MULTI_GENERIC_CODING_EVENT_DETECTOR}
     * 
     * @testedMethod {@link MultiGenericCodingEventDetector#getPhenomenonCode()}
     * 
     * @description tests {@link MultiGenericCodingEventDetector#getPhenomenonCode()} using the
     *              {@link CircularFieldOfViewDetector}
     * 
     * @input {@link MultiGenericCodingEventDetector} constructor parameters
     * 
     * @output the phenomenon code, null when the {@link MultiGenericCodingEventDetector} does not support phenomena
     * 
     * @testPassCriteria the method returns the expected code (or null)
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testGetPhenomenonCode() {
        // The CodingEventDetector is created:
        final MultiGenericCodingEventDetector detector1 =
            new MultiGenericCodingEventDetector(visi, "ENTER", "EXIT", true,
                "Visibility");
        final MultiGenericCodingEventDetector detector2 =
            new MultiGenericCodingEventDetector(visi, "ENTER", "EXIT");
        Assert.assertEquals("Visibility", detector1.getPhenomenonCode());
        Assert.assertNull(detector2.getPhenomenonCode());
    }

    /**
     * @throws PatriusException
     *         from the wrapped event detector.
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_MULTI_GENERIC_CODING_EVENT_DETECTOR}
     * 
     * @testedMethod {@link MultiGenericCodingEventDetector#isStateActive(Map)}
     * 
     * @description tests {@link MultiGenericCodingEventDetector#isStateActive(Map)}
     * 
     * @input {@link MultiGenericCodingEventDetector} constructor parameters
     * 
     * @output if the state is active or not
     * 
     * @testPassCriteria the method returns the expected state
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testIsStateActive() throws PatriusException {
        // The CodingEventDetector is created:
        final MultiGenericCodingEventDetector detector1 =
            new MultiGenericCodingEventDetector(visi, "ENTER", "EXIT", true,
                "Visibility");
        Assert.assertEquals(false, detector1.isStateActive(state));
    }
}
