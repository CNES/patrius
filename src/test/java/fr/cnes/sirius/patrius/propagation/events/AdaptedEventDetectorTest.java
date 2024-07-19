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
 * @history created 17/11/11
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::FA:273:20/10/2013:Minor code problems
 * VERSION::DM:454:24/11/2015:Unimplemented method shouldBeRemoved()
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.events;

import java.util.HashMap;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.NadirPointing;
import fr.cnes.sirius.patrius.attitudes.YawSteering;
import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.EllipsoidBodyShape;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.numerical.AdditionalStateInfo;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusExceptionWrapper;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Unit tests for {@link AdaptedEventDetector}.<br>
 * Note : unit test also written for code coverage, including AbstractDetector.
 * 
 * @author cardosop
 * 
 * @version $Id: AdaptedEventDetectorTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.1
 * 
 */
public class AdaptedEventDetectorTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Validate the adapted event detector
         * 
         * @featureDescription Validate the adapted event detector
         * 
         * @coveredRequirements DV-INTEG_70
         */
        VALIDATE_ADAPTED_EVENT_DETECTOR
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
    }

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

    /**
     * An AttitudeProvider.
     */
    private static AttitudeProvider attitudeLaw;

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

        mu = CelestialBodyFactory.getEarth().getGM();

        final Vector3D issPos = new Vector3D(ix, iy, iz);
        final Vector3D issVit = new Vector3D(ivx, ivy, ivz);
        final PVCoordinates pvCoordinates = new PVCoordinates(issPos, issVit);
        tISSOrbit = new CartesianOrbit(pvCoordinates, FramesFactory.getEME2000(), refDate, mu);

        // Earth body shape as found in many Orekit unit tests
        earthBodyShape =
            new OneAxisEllipsoid(6378137.0, 1.0 / 298.257222101, FramesFactory.getITRF());

        attitudeLaw =
            new YawSteering(new NadirPointing(earthBodyShape), CelestialBodyFactory.getSun(), Vector3D.MINUS_J);

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_ADAPTED_EVENT_DETECTOR}
     * 
     * @testedMethod {@link AdaptedEventDetector#g}
     * 
     * @description test for a call to g with error
     * 
     * @input constructor parameters
     * 
     * @output an AdaptedEventDetector
     * 
     * @testPassCriteria the call to g raises an OrekitExceptionWrapper caused by an OrekitException
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 2.3
     * 
     * @throws PatriusExceptionWrapper
     *         is expected
     */
    @Test(expected = PatriusExceptionWrapper.class)
    public void testAdaptedEventDetectorGError() {

        final CrashingEventDetector crashEd = new CrashingEventDetector();
        final AdaptedEventDetector detector = new AdaptedEventDetector(crashEd,
            new HashMap<String, AdditionalStateInfo>(), tISSOrbit.getType(), PositionAngle.MEAN, attitudeLaw,
            attitudeLaw, refDate, mu, FramesFactory.getEME2000());
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
     * @testedFeature {@link features#VALIDATE_ADAPTED_EVENT_DETECTOR}
     * 
     * @testedMethod {@link AdaptedEventDetector#eventOccurred(double, double[], boolean, boolean)}
     * 
     * @description test for a call to eventOccured with error
     * 
     * @input constructor parameters
     * 
     * @output an AdaptedEventDetector
     * 
     * @testPassCriteria the call raises an OrekitExceptionWrapper caused by an OrekitException
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 2.3
     * 
     * @throws PatriusExceptionWrapper
     *         is expected
     */
    @Test(expected = PatriusExceptionWrapper.class)
    public void testAdaptedEventDetectorEventOccuredError() {

        final CrashingEventDetector crashEd = new CrashingEventDetector();
        final AdaptedEventDetector detector = new AdaptedEventDetector(crashEd,
            new HashMap<String, AdditionalStateInfo>(), tISSOrbit.getType(), PositionAngle.MEAN, attitudeLaw,
            attitudeLaw, refDate, mu, FramesFactory.getEME2000());
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
     * @testedFeature {@link features#VALIDATE_ADAPTED_EVENT_DETECTOR}
     * 
     * @testedMethod {@link AdaptedEventDetector#resetState(double, double[])}
     * 
     * @description test for a call to resetState with error
     * 
     * @input constructor parameters
     * 
     * @output an AdaptedEventDetector
     * 
     * @testPassCriteria the call raises an OrekitExceptionWrapper caused by an OrekitException
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 2.3
     * 
     * @throws PatriusExceptionWrapper
     *         is expected
     */
    @Test(expected = PatriusExceptionWrapper.class)
    public void testAdaptedEventDetectorResetStateError() {

        final CrashingEventDetector crashEd = new CrashingEventDetector();
        final AdaptedEventDetector detector = new AdaptedEventDetector(crashEd,
            new HashMap<String, AdditionalStateInfo>(), tISSOrbit.getType(), PositionAngle.MEAN, attitudeLaw,
            attitudeLaw, refDate, mu, FramesFactory.getEME2000());
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
     * @testedFeature {@link features#VALIDATE_ADAPTED_EVENT_DETECTOR}
     * 
     * @testedMethod {@link AdaptedEventDetector#init(double, double[], double)}
     * 
     * @description test for a call to init with error
     * 
     * @input constructor parameters
     * 
     * @output an AdaptedEventDetector
     * 
     * @testPassCriteria the call raises an OrekitExceptionWrapper caused by an OrekitException
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 2.3
     * 
     * @throws PatriusExceptionWrapper
     *         is expected
     */
    @Test(expected = PatriusExceptionWrapper.class)
    public void testAdaptedEventDetectorInitError() {

        final CrashingEventDetector crashEd = new CrashingEventDetector();
        final AdaptedEventDetector detector = new AdaptedEventDetector(crashEd,
            new HashMap<String, AdditionalStateInfo>(), tISSOrbit.getType(), PositionAngle.MEAN, attitudeLaw,
            attitudeLaw, refDate, mu, FramesFactory.getEME2000());
        final double[] bogusY = { 0.1, 0.2, 0.3, 0.7, 0.4, 0.1, 0.6 };
        try {
            detector.init(0., bogusY, 0.);
        } catch (final PatriusExceptionWrapper e) {
            final Throwable cause = e.getException();
            Assert.assertEquals(cause.getClass(), PatriusException.class);
            throw e;
        }
    }

}
