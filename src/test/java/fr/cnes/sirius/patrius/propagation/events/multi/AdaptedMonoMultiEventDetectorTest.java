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
 * VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
 * VERSION:4.13:FA:FA-79:08/12/2023:[PATRIUS] Probleme dans la fonction g de LocalTimeAngleDetector
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2089:15/05/2019:[PATRIUS] passage a Java 8
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:300:18/03/2015:Creation multi propagator
 * VERSION::DM:454:24/11/2015:Unimplemented method shouldBeRemoved()
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1872:10/12/2018:Substitution of AttitudeProvider with MultiAttitudeProvider
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.events.multi;

import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.NadirPointing;
import fr.cnes.sirius.patrius.attitudes.YawSteering;
import fr.cnes.sirius.patrius.attitudes.multi.MultiAttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.multi.MultiAttitudeProviderWrapper;
import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.EllipsoidBodyShape;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.events.AbstractDetector;
import fr.cnes.sirius.patrius.events.EventDetector;
import fr.cnes.sirius.patrius.events.EventDetector.Action;
import fr.cnes.sirius.patrius.events.MultiAbstractDetector;
import fr.cnes.sirius.patrius.events.MultiEventDetector;
import fr.cnes.sirius.patrius.events.utils.AdaptedMonoEventDetector;
import fr.cnes.sirius.patrius.events.utils.AdaptedMultiEventDetector;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.numerical.multi.MultiStateVectorInfo;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusExceptionWrapper;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * <p>
 * Unit tests for {@link AdaptedMonoEventDetector} and {@link AdaptedMultiEventDetector}.<br>
 * Note : unit test also written for code coverage, including AbstractDetector.
 * </p>
 * 
 * @author maggioranic
 * 
 * @version $Id$
 * 
 * @since 3.0
 * 
 */
public class AdaptedMonoMultiEventDetectorTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Validate the adapted mono event detector
         * 
         * @featureDescription Validate the adapted mono event detector
         */
        VALIDATE_ADAPTED_MONO_EVENT_DETECTOR,

        /**
         * @featureTitle Validate the adapted multi event detector
         * 
         * @featureDescription Validate the adapted multi event detector
         */
        VALIDATE_ADAPTED_MULTI_EVENT_DETECTOR,

        /**
         * @featureTitle Test coverage of MultiAbstractDetector
         * 
         * @featureDescription Test coverage of MultiAbstractDetector
         */
        VALIDATE_MULTI_ABSTRACT_DETECTOR
    }

    /**
     * Class implementing EventDetector that always crashes where possible - for test purposes of course.
     */
    public final class CrashingEventDetector implements EventDetector {

        /** Serializable UID. */
        private static final long serialVersionUID = 5701566433635269225L;

        /**
         * Always throws an {@link PatriusException}. {@inheritDoc}
         */
        @Override
        public double g(final SpacecraftState s) throws PatriusException {
            throw new PatriusException(PatriusMessages.USER_EXCEPTION);
        }

        /**
         * Always throws an {@link PatriusException}. {@inheritDoc}
         */
        @Override
        public Action
            eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                throws PatriusException {
            throw new PatriusException(PatriusMessages.USER_EXCEPTION);
        }

        @Override
        public boolean shouldBeRemoved() {
            return false;
        }

        /**
         * Always throws an {@link PatriusException}. {@inheritDoc}
         */
        @Override
        public SpacecraftState resetState(final SpacecraftState oldState) throws PatriusException {
            throw new PatriusException(PatriusMessages.USER_EXCEPTION);
        }

        /**
         * Returns 0. {@inheritDoc}
         */
        @Override
        public double getThreshold() {
            return 0;
        }

        /**
         * Returns 0. {@inheritDoc}
         */
        @Override
        public double getMaxCheckInterval() {
            return 0;
        }

        /**
         * Returns 0. {@inheritDoc}
         */
        @Override
        public int getMaxIterationCount() {
            return 0;
        }

        @Override
        public void init(final SpacecraftState s0, final AbsoluteDate t) {
            // handled by AdaptedEventDetector
            throw new PatriusExceptionWrapper(new PatriusException(PatriusMessages.USER_EXCEPTION));
        }

        @Override
        public int getSlopeSelection() {
            return 2;
        }

        @Override
        public EventDetector copy() {
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public boolean filterEvent(final SpacecraftState state,
                final boolean increasing,
                final boolean forward) throws PatriusException {
            return false;
        }
    }

    /**
     * Class implementing MultiEventDetector that always crashes where possible - for test purposes of course.
     */
    public final class CrashingMultiEventDetector implements MultiEventDetector {

        /**
         * Always throws an {@link PatriusException}. {@inheritDoc}
         */
        @Override
        public double g(final Map<String, SpacecraftState> s) throws PatriusException {
            throw new PatriusException(PatriusMessages.USER_EXCEPTION);
        }

        /**
         * Always throws an {@link PatriusException}. {@inheritDoc}
         */
        @Override
        public Action eventOccurred(final Map<String, SpacecraftState> s, final boolean increasing,
                                    final boolean forward) throws PatriusException {
            throw new PatriusException(PatriusMessages.USER_EXCEPTION);
        }

        @Override
        public boolean shouldBeRemoved() {
            return false;
        }

        /**
         * Always throws an {@link PatriusException}. {@inheritDoc}
         */
        @Override
        public Map<String, SpacecraftState>
            resetStates(final Map<String, SpacecraftState> oldState)
                throws PatriusException {
            throw new PatriusException(PatriusMessages.USER_EXCEPTION);
        }

        /**
         * Returns 0. {@inheritDoc}
         */
        @Override
        public double getThreshold() {
            return 0;
        }

        /**
         * Returns 0. {@inheritDoc}
         */
        @Override
        public double getMaxCheckInterval() {
            return 0;
        }

        /**
         * Returns 0. {@inheritDoc}
         */
        @Override
        public int getMaxIterationCount() {
            return 0;
        }

        @Override
        public void init(final Map<String, SpacecraftState> s0, final AbsoluteDate t) {
            // handled by AdaptedEventDetector
            throw new PatriusExceptionWrapper(new PatriusException(PatriusMessages.USER_EXCEPTION));
        }

        @Override
        public int getSlopeSelection() {
            return 2;
        }

        /** {@inheritDoc} */
        @Override
        public boolean filterEvent(final Map<String, SpacecraftState> states,
                final boolean increasing,
                final boolean forward) throws PatriusException {
            return false;
        }
    }

    /**
     * Class implementing EventDetector with specific resetState method - for test purposes of course.
     */
    public final class MyEventDetector extends AbstractDetector {
        protected MyEventDetector() {
            super(AbstractDetector.DEFAULT_MAXCHECK, AbstractDetector.DEFAULT_THRESHOLD);
        }

        private static final long serialVersionUID = 5652048601135881892L;

        @Override
        public double g(final SpacecraftState s) throws PatriusException {
            return 0.;
        }

        @Override
        public Action
            eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                throws PatriusException {
            return null;
        }

        @Override
        public boolean shouldBeRemoved() {
            return false;
        }

        @Override
        public SpacecraftState resetState(final SpacecraftState oldState) throws PatriusException {
            final Vector3D issPos = new Vector3D(0., 0., 0.);
            final Vector3D issVit = new Vector3D(0., 0., 0.);
            final PVCoordinates pvCoordinates = new PVCoordinates(issPos, issVit);
            return new SpacecraftState(new CartesianOrbit(pvCoordinates, FramesFactory.getEME2000(),
                AbsoluteDate.J2000_EPOCH, mu));
        }

        @Override
        public EventDetector copy() {
            return null;
        }
    }

    private static final String STATE1 = "state1";

    /**
     * A Cartesian orbit used for the tests.
     */
    private static CartesianOrbit tISSOrbit;

    /**
     * A reference date.
     */
    private static AbsoluteDate refDate = new AbsoluteDate("2011-11-09T12:00:00Z", TimeScalesFactory.getTT());

    /**
     * Earth's bodyshape
     */
    private static EllipsoidBodyShape earthBodyShape;

    /**
     * Mu.
     */
    private static double mu;
    private static Map<String, Double> muMap;

    /**
     * Frame.
     */
    private static Map<String, Frame> frameMap;

    /**
     * An AttitudeProvider.
     */
    private static AttitudeProvider attitudeLaw;
    private static Map<String, MultiAttitudeProvider> attitudeLawMap;

    /**
     * A map of states.
     */
    private static Map<String, SpacecraftState> states;

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
    public void setUp() throws PatriusException {

        // Orekit initialization
        Utils.setDataRoot("regular-dataPBASE");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));

        // Some orbit data for the tests
        // (Real ISS data!)
        final double ix = 2156444.05;
        final double iy = 3611777.68;
        final double iz = -5316875.46;
        final double ivx = -6579.446110;
        final double ivy = 3916.478783;
        final double ivz = 8.876119;

        mu = CelestialBodyFactory.getEarth().getGM();
        muMap = new HashMap<>();
        muMap.put(STATE1, mu);

        final Vector3D issPos = new Vector3D(ix, iy, iz);
        final Vector3D issVit = new Vector3D(ivx, ivy, ivz);
        final PVCoordinates pvCoordinates = new PVCoordinates(issPos, issVit);
        tISSOrbit = new CartesianOrbit(pvCoordinates, FramesFactory.getEME2000(), refDate, mu);

        // Earth body shape as found in many Orekit unit tests
        earthBodyShape =
            new OneAxisEllipsoid(6378137.0, 1.0 / 298.257222101, FramesFactory.getITRF());

        attitudeLaw =
            new YawSteering(new NadirPointing(earthBodyShape), CelestialBodyFactory.getSun(), Vector3D.MINUS_J);

        states = new HashMap<>();
        states.put(STATE1, new SpacecraftState(tISSOrbit));

        frameMap = new HashMap<>();
        frameMap.put(STATE1, FramesFactory.getEME2000());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_ADAPTED_MONO_EVENT_DETECTOR}
     * 
     * @testedMethod {@link AdaptedMonoEventDetector#g}
     * 
     * @description test for a call to g with error
     * 
     * @input constructor parameters
     * 
     * @output an AdaptedMonoEventDetector
     * 
     * @testPassCriteria the call to g raises an OrekitExceptionWrapper caused by an OrekitException
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     * 
     * @throws PatriusExceptionWrapper
     *         is expected
     */
    @Test(expected = PatriusExceptionWrapper.class)
    public void testAdaptedMonoEventDetectorGError() {
        final MultiStateVectorInfo stateVectorInfo = new MultiStateVectorInfo(states, muMap, frameMap);
        final CrashingEventDetector crashEd = new CrashingEventDetector();
        final MultiAttitudeProviderWrapper attitudeLawWrapper = new MultiAttitudeProviderWrapper(attitudeLaw, STATE1);
        final AdaptedMonoEventDetector detector = new AdaptedMonoEventDetector(crashEd,
            tISSOrbit.getType(), PositionAngle.MEAN, attitudeLawWrapper,
            attitudeLawWrapper, refDate, stateVectorInfo, STATE1);

        final double[] bogusY = { 0.1, 0.2, 0.3, 0.7, 0.4, 0.1, 0.6 };
        try {
            detector.g(0.1, bogusY);
        } catch (final PatriusExceptionWrapper e) {
            final Throwable cause = e.getCause();
            Assert.assertEquals(cause.getClass(), PatriusException.class);
            throw e;
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_ADAPTED_MULTI_EVENT_DETECTOR}
     * 
     * @testedMethod {@link AdaptedMultiEventDetector#g}
     * 
     * @description test for a call to g with error
     * 
     * @input constructor parameters
     * 
     * @output an AdaptedMultiEventDetector
     * 
     * @testPassCriteria the call to g raises an OrekitExceptionWrapper caused by an OrekitException
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     * 
     * @throws PatriusExceptionWrapper
     *         is expected
     */
    @Test(expected = PatriusExceptionWrapper.class)
    public void testAdaptedMultiEventDetectorGError() {
        final MultiStateVectorInfo stateVectorInfo = new MultiStateVectorInfo(states, muMap, frameMap);
        final CrashingMultiEventDetector crashEdMulti = new CrashingMultiEventDetector();
        final AdaptedMultiEventDetector multiDetector = new AdaptedMultiEventDetector(crashEdMulti,
            tISSOrbit.getType(), PositionAngle.MEAN, attitudeLawMap,
            attitudeLawMap, refDate, muMap, frameMap, stateVectorInfo);
        final double[] bogusY = { 0.1, 0.2, 0.3, 0.7, 0.4, 0.1, 0.6 };
        try {
            multiDetector.g(0.1, bogusY);
        } catch (final PatriusExceptionWrapper e) {
            final Throwable cause = e.getCause();
            Assert.assertEquals(cause.getClass(), PatriusException.class);
            throw e;
        }

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_ADAPTED_MONO_EVENT_DETECTOR}
     * 
     * @testedMethod {@link AdaptedMonoEventDetector#eventOccurred(double, double[], boolean, boolean)}
     * 
     * @description test for a call to eventOccured with error
     * 
     * @input constructor parameters
     * 
     * @output an AdaptedMonoEventDetector
     * 
     * @testPassCriteria the call raises an OrekitExceptionWrapper caused by an OrekitException
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     * 
     * @throws PatriusExceptionWrapper
     *         is expected
     */
    @Test(expected = PatriusExceptionWrapper.class)
    public void testAdaptedMonoEventDetectorEventOccuredError() {

        final CrashingEventDetector crashEd = new CrashingEventDetector();
        final MultiStateVectorInfo stateVectorInfo = new MultiStateVectorInfo(states, muMap, frameMap);
        final MultiAttitudeProviderWrapper attitudeLawWrapper = new MultiAttitudeProviderWrapper(attitudeLaw, STATE1);
        final AdaptedMonoEventDetector detector = new AdaptedMonoEventDetector(crashEd,
            tISSOrbit.getType(), PositionAngle.MEAN, attitudeLawWrapper,
            attitudeLawWrapper, refDate, stateVectorInfo, STATE1);

        final double[] bogusY = { 0.1, 0.2, 0.3, 0.7, 0.4, 0.1, 0.6 };
        try {
            detector.eventOccurred(0.1, bogusY, true, true);
        } catch (final PatriusExceptionWrapper e) {
            final Throwable cause = e.getException();
            Assert.assertEquals(cause.getClass(), PatriusException.class);
            throw e;
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_ADAPTED_MULTI_EVENT_DETECTOR}
     * 
     * @testedMethod {@link AdaptedMultiEventDetector#eventOccurred(double, double[], boolean, boolean)}
     * 
     * @description test for a call to eventOccured with error
     * 
     * @input constructor parameters
     * 
     * @output an AdaptedMultiEventDetector
     * 
     * @testPassCriteria the call raises an OrekitExceptionWrapper caused by an OrekitException
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     * 
     * @throws PatriusExceptionWrapper
     *         is expected
     */
    @Test(expected = PatriusExceptionWrapper.class)
    public void testAdaptedMultiEventDetectorEventOccuredError() {
        final MultiStateVectorInfo stateVectorInfo = new MultiStateVectorInfo(states, muMap, frameMap);
        final CrashingMultiEventDetector crashEdMulti = new CrashingMultiEventDetector();
        final AdaptedMultiEventDetector multiDetector = new AdaptedMultiEventDetector(crashEdMulti,
            tISSOrbit.getType(), PositionAngle.MEAN, attitudeLawMap,
            attitudeLawMap, refDate, muMap, frameMap, stateVectorInfo);
        final double[] bogusY = { 0.1, 0.2, 0.3, 0.7, 0.4, 0.1, 0.6 };
        try {
            multiDetector.eventOccurred(0.1, bogusY, true, true);
        } catch (final PatriusExceptionWrapper e) {
            final Throwable cause = e.getException();
            Assert.assertEquals(cause.getClass(), PatriusException.class);
            throw e;
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_ADAPTED_MONO_EVENT_DETECTOR}
     * 
     * @testedMethod {@link AdaptedMonoEventDetector#resetState(double, double[])}
     * 
     * @description test for a call to resetState with error
     * 
     * @input constructor parameters
     * 
     * @output an AdaptedMonoEventDetector
     * 
     * @testPassCriteria the call raises an OrekitExceptionWrapper caused by an OrekitException
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     * 
     * @throws PatriusExceptionWrapper
     *         is expected
     */
    @Test(expected = PatriusExceptionWrapper.class)
    public void testAdaptedMonoEventDetectorResetStateError() {

        final CrashingEventDetector crashEd = new CrashingEventDetector();
        final MultiStateVectorInfo stateVectorInfo = new MultiStateVectorInfo(states, muMap, frameMap);
        final MultiAttitudeProviderWrapper attitudeLawWrapper = new MultiAttitudeProviderWrapper(attitudeLaw, STATE1);
        final AdaptedMonoEventDetector detector = new AdaptedMonoEventDetector(crashEd,
            tISSOrbit.getType(), PositionAngle.MEAN, attitudeLawWrapper,
            attitudeLawWrapper, refDate, stateVectorInfo, STATE1);

        final double[] bogusY = { 0.1, 0.2, 0.3, 0.7, 0.4, 0.1, 0.6 };
        try {
            detector.resetState(0.1, bogusY);
        } catch (final PatriusExceptionWrapper e) {
            final Throwable cause = e.getException();
            Assert.assertEquals(cause.getClass(), PatriusException.class);
            throw e;
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_ADAPTED_MULTI_EVENT_DETECTOR}
     * 
     * @testedMethod {@link AdaptedMultiEventDetector#resetState(double, double[])}
     * 
     * @description test for a call to resetState with error
     * 
     * @input constructor parameters
     * 
     * @output an AdaptedMultiEventDetector
     * 
     * @testPassCriteria the call raises an OrekitExceptionWrapper caused by an OrekitException
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     * 
     * @throws PatriusExceptionWrapper
     *         is expected
     */
    @Test(expected = PatriusExceptionWrapper.class)
    public void testAdaptedMultiEventDetectorResetStateError() {
        final MultiStateVectorInfo stateVectorInfo = new MultiStateVectorInfo(states, muMap, frameMap);
        final double[] bogusY = { 0.1, 0.2, 0.3, 0.7, 0.4, 0.1, 0.6 };
        final CrashingMultiEventDetector crashEdMulti = new CrashingMultiEventDetector();
        final AdaptedMultiEventDetector multiDetector = new AdaptedMultiEventDetector(crashEdMulti,
            tISSOrbit.getType(), PositionAngle.MEAN, attitudeLawMap,
            attitudeLawMap, refDate, muMap, frameMap, stateVectorInfo);
        try {
            multiDetector.resetState(0.1, bogusY);
        } catch (final PatriusExceptionWrapper e) {
            final Throwable cause = e.getException();
            Assert.assertEquals(cause.getClass(), PatriusException.class);
            throw e;
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_ADAPTED_MONO_EVENT_DETECTOR}
     * 
     * @testedMethod {@link AdaptedMonoEventDetector#init(double, double[], double)}
     * @description test for a call to init with error
     * 
     * @input constructor parameters
     * 
     * @output an AdaptedMonoEventDetector
     * 
     * @testPassCriteria the call raises an OrekitExceptionWrapper caused by an OrekitException
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     * 
     * @throws PatriusExceptionWrapper
     *         is expected
     */
    @Test(expected = PatriusExceptionWrapper.class)
    public void testAdaptedMonoEventDetectorInitError() {

        final CrashingEventDetector crashEd = new CrashingEventDetector();
        final MultiStateVectorInfo stateVectorInfo = new MultiStateVectorInfo(states, muMap, frameMap);
        final MultiAttitudeProviderWrapper attitudeLawWrapper = new MultiAttitudeProviderWrapper(attitudeLaw, STATE1);
        final AdaptedMonoEventDetector detector = new AdaptedMonoEventDetector(crashEd,
            tISSOrbit.getType(), PositionAngle.MEAN, attitudeLawWrapper,
            attitudeLawWrapper, refDate, stateVectorInfo, STATE1);
        final double[] bogusY = { 0.1, 0.2, 0.3, 0.7, 0.4, 0.1, 0.6 };
        try {
            detector.init(0., bogusY, 0.);
        } catch (final PatriusExceptionWrapper e) {
            final Throwable cause = e.getException();
            Assert.assertEquals(cause.getClass(), PatriusException.class);
            throw e;
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_ADAPTED_MULTI_EVENT_DETECTOR}
     * 
     * @testedMethod {@link AdaptedMultiEventDetector#init(double, double[], double)}
     * @description test for a call to init with error
     * 
     * @input constructor parameters
     * 
     * @output an AdaptedMultiEventDetector
     * 
     * @testPassCriteria the call raises an OrekitExceptionWrapper caused by an OrekitException
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     * 
     * @throws PatriusExceptionWrapper
     *         is expected
     */
    @Test(expected = PatriusExceptionWrapper.class)
    public void testAdaptedMultiEventDetectorInitError() {

        final MultiStateVectorInfo stateVectorInfo = new MultiStateVectorInfo(states, muMap, frameMap);
        final double[] bogusY = { 0.1, 0.2, 0.3, 0.7, 0.4, 0.1, 0.6 };
        final CrashingMultiEventDetector crashEdMulti = new CrashingMultiEventDetector();
        final AdaptedMultiEventDetector multiDetector = new AdaptedMultiEventDetector(crashEdMulti,
            tISSOrbit.getType(), PositionAngle.MEAN, attitudeLawMap,
            attitudeLawMap, refDate, muMap, frameMap, stateVectorInfo);
        try {
            multiDetector.init(0., bogusY, 0.);
        } catch (final PatriusExceptionWrapper e) {
            final Throwable cause = e.getException();
            Assert.assertEquals(cause.getClass(), PatriusException.class);
            throw e;
        }

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_ADAPTED_MONO_EVENT_DETECTOR}
     * 
     * @testedMethod {@link AdaptedMonoEventDetector#resetState(double, double[])}
     * @description test for a call to resetState
     * 
     * @input constructor parameters
     * 
     * @output an AdaptedMonoEventDetector
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     * 
     * @throws PatriusExceptionWrapper
     *         is expected
     */
    @Test
    public void testAdaptedMonoEventDetectorResetState() {
        states.put("state2", new SpacecraftState(tISSOrbit));
        final MultiStateVectorInfo stateVectorInfo = new MultiStateVectorInfo(states, muMap, frameMap);
        final MyEventDetector detector0 = new MyEventDetector();
        final MultiAttitudeProviderWrapper attitudeLawWrapper = new MultiAttitudeProviderWrapper(attitudeLaw, STATE1);
        final AdaptedMonoEventDetector detector = new AdaptedMonoEventDetector(detector0,
            tISSOrbit.getType(), PositionAngle.MEAN, attitudeLawWrapper,
            attitudeLawWrapper, refDate, stateVectorInfo, STATE1);
        final double[] y = new double[12];
        stateVectorInfo.mapStatesToArray(states, tISSOrbit.getType(), PositionAngle.MEAN, y);
        detector.resetState(10, y);
        for (int i = 0; i < 6; i++) {
            Assert.assertEquals(0., y[stateVectorInfo.getSatRank(STATE1) + i]);
        }
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_MULTI_ABSTRACT_DETECTOR}
     * 
     * @testedMethod {@link MultiAbstractDetector#MultiAbstractDetector(double, double)}
     * @testedMethod {@link MultiAbstractDetector#init(Map, AbsoluteDate)}
     * @testedMethod {@link MultiAbstractDetector#resetStates(Map)}
     * 
     * @description Test for coverage purpose
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testMultiAbstractDetector() throws PatriusException {
        final MultiAbstractDetector detector = new MultiAbstractDetector(0, 0){
            @Override
            public double g(final Map<String, SpacecraftState> s) throws PatriusException {
                return 0;
            }

            @Override
            public Action eventOccurred(final Map<String, SpacecraftState> s, final boolean increasing,
                                        final boolean forward)
                throws PatriusException {
                return null;
            }

            @Override
            public boolean shouldBeRemoved() {
                return false;
            }

            /** {@inheritDoc} */
            @Override
            public boolean filterEvent(final Map<String, SpacecraftState> states,
                    final boolean increasing,
                    final boolean forward) throws PatriusException {
                return false;
            }
        };
        detector.init(states, refDate);
        Assert.assertEquals(states.hashCode(), detector.resetStates(states).hashCode());

        boolean testOk = false;
        try {
            new MultiAbstractDetector(10, 0, 0){
                @Override
                public double g(final Map<String, SpacecraftState> s) throws PatriusException {
                    return 0;
                }

                @Override
                public Action eventOccurred(final Map<String, SpacecraftState> s, final boolean increasing,
                                            final boolean forward)
                    throws PatriusException {
                    return null;
                }

                @Override
                public boolean shouldBeRemoved() {
                    return false;
                }

                /** {@inheritDoc} */
                @Override
                public boolean filterEvent(final Map<String, SpacecraftState> states,
                        final boolean increasing,
                        final boolean forward) throws PatriusException {
                    return false;
                }
            };
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            testOk = true;
            Assert.assertEquals(PatriusMessages.UNSUPPORTED_SLOPE_SELECTION_TYPE.getSourceString(), e.getMessage());
        }
        Assert.assertTrue(testOk);
    }
}
