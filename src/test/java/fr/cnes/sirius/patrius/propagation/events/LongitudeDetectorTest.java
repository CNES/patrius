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
 * @history created 10/07/12
 *
 *
 * HISTORY
 * VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
 * VERSION:4.13:DM:DM-126:08/12/2023:[PATRIUS] Distinction increasing/decreasing dans LongitudeDetector
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration de la gestion des attractions gravitationnelles dans le propagateur
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.10:DM:DM-3194:03/11/2022:[PATRIUS] Fusion des interfaces GeometricBodyShape et BodyShape 
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:394:20/02/2015: Added possibility to define the action performed when event occurs
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.events;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.EllipsoidBodyShape;
import fr.cnes.sirius.patrius.bodies.EllipsoidPoint;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.events.EventDetector.Action;
import fr.cnes.sirius.patrius.events.detectors.LongitudeDetector;
import fr.cnes.sirius.patrius.events.postprocessing.EventsLogger;
import fr.cnes.sirius.patrius.events.postprocessing.EventsLogger.LoggedEvent;
import fr.cnes.sirius.patrius.forces.gravity.DirectBodyAttraction;
import fr.cnes.sirius.patrius.forces.gravity.NewtonianGravityModel;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.nonstiff.AdaptiveStepsizeIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.CircularOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusFixedStepHandler;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Unit tests for {@link LongitudeDetector}.
 * 
 * @author Philippe CHABAUD
 * 
 * @version $Id: LongitudeDetectorTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.3
 * 
 */
public class LongitudeDetectorTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Validate the satellite longitude detector
         * 
         * @featureDescription Validate the satellite longitude detector
         * 
         * @coveredRequirements DV-EVT_121
         */
        VALIDATE_LONGITUDE_DETECTOR
    }

    /** earth shape */
    private EllipsoidBodyShape earth;

    /** Epsilon longitude comparison */
    private static final double EPSILON_LONGITUDE = 1E-8;

    /**
     * Setup for all unit tests in the class.
     * Configure frames and set earth model
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @Before
    public void setUp() throws PatriusException {
        Utils.setDataRoot("frame-validation");
        FramesFactory.setConfiguration(fr.cnes.sirius.patrius.Utils.getIERS2003ConfigurationWOEOP(true));
        this.earth = new OneAxisEllipsoid(6000000.0, 0.0, FramesFactory.getTIRF());
    }

    /**
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_LONGITUDE_DETECTOR}
     * 
     * @testedMethod {@link LongitudeDetector#g(SpacecraftState) }
     * 
     * @description test of the longitude detection for keplerian propagator with a simple circular orbit
     * 
     * @input keplerian propagator, simple circular orbit a = 8000 km, e=0, i=60 deg, list of longitude to detect
     * 
     * @output all the detected events
     * 
     * @throws PatriusException
     *         : Should not happen
     * 
     * @testPassCriteria difference between longitude of the spacecraftstate when event occured and
     *                   longitude to detect less than {@link LongitudeDetectorTest#EPSILON_LONGITUDE} Justification :
     *                   covering RCOV_500 for angle
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testStandardOrbit() throws PatriusException {

        final AbsoluteDate iniDate = new AbsoluteDate("2011-11-09T12:00:00Z", TimeScalesFactory.getTT());

        final CircularOrbit orbit = new CircularOrbit(8000000, 0, 0, MathLib.toRadians(60.0), 0, FastMath.PI / 2,
            PositionAngle.TRUE, FramesFactory.getGCRF(), iniDate, Constants.EGM96_EARTH_MU);

        final double period = orbit.getKeplerianPeriod();
        final Propagator propagator = new KeplerianPropagator(orbit);

        final double[] longitudes = { 0.0, 90.0, 180.0 };
        this.computeVerification(propagator, iniDate.shiftedBy(3 * period), longitudes, "pro");
    }

    /**
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_LONGITUDE_DETECTOR}
     * 
     * @testedMethod {@link LongitudeDetector#g(SpacecraftState) }
     * 
     * @description test of the longitude detection for keplerian propagator with a GTO orbit.
     *              Test if the detector is robust to the changing of way of the longitude
     * 
     * @input keplerian propagator, GTO orbit a = 40 000 km, e=0.86, i=10 deg, list of longitude to detect.<br>
     *        On this orbit we detect extrema longitude around 50 deg, -170 deg, and -30 deg of longitude.<br>
     * 
     * 
     * @output all the detected events
     * 
     * @throws PatriusException
     *         : Should not happen
     * 
     * @testPassCriteria difference between longitude of the spacecraftstate when event occured and
     *                   longitude to detect less than {@link LongitudeDetectorTest#EPSILON_LONGITUDE} Justification :
     *                   covering RCOV_500 for angle
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testGTOOrbit() throws PatriusException {

        final AbsoluteDate iniDate = new AbsoluteDate("2011-11-09T12:00:00Z", TimeScalesFactory.getTT());

        final double a = 30000e3;
        final double b = 6200e3;
        final double e = (a - b) / (a + b);

        final KeplerianOrbit orbit = new KeplerianOrbit(a, e, MathLib.toRadians(10.0), MathLib.toRadians(199.0), 0.0,
            0.0,
            PositionAngle.TRUE, FramesFactory.getGCRF(), iniDate, Constants.EGM96_EARTH_MU);

        final double period = orbit.getKeplerianPeriod();
        final Propagator propagator = new KeplerianPropagator(orbit);

        final double[] longitudes = { 50, -170, -30 };
        this.computeVerification(propagator, iniDate.shiftedBy(3 * period), longitudes, "gto");

    }

    /**
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_LONGITUDE_DETECTOR}
     * 
     * @testedMethod {@link LongitudeDetector#g(SpacecraftState) }
     * 
     * @description test of the longitude detection for numerical propagator with a retrograde circular orbit
     * 
     * @input numerical propagator, retrograde orbit a = 8000 km, e=0, i=98 deg, list of longitude to dectect
     * 
     * @output all detected events
     * 
     * @throws PatriusException
     *         : Should not happen
     * 
     * @testPassCriteria difference between longitude of the spacecraftstate when event occured and
     *                   longitude to detect less than {@link LongitudeDetectorTest#EPSILON_LONGITUDE} Justification :
     *                   covering RCOV_500 for angle
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testNumericalPropagator() throws PatriusException {

        final AbsoluteDate iniDate = new AbsoluteDate("2011-11-09T12:00:00Z", TimeScalesFactory.getTT());

        final CircularOrbit orbit = new CircularOrbit(8000000, 0, 0, MathLib.toRadians(98.0), 0, FastMath.PI / 2,
            PositionAngle.TRUE, FramesFactory.getGCRF(), iniDate, Constants.EGM96_EARTH_MU);

        final double period = orbit.getKeplerianPeriod();

        final double[] absTOL = { 1e-5, 1e-5, 1e-5, 1e-8, 1e-8, 1e-8 };
        final double[] relTOL = { 1e-10, 1e-10, 1e-10, 1e-10, 1e-10, 1e-10 };
        final AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(0.1, 60., absTOL, relTOL);
        final NumericalPropagator propagator = new NumericalPropagator(integrator);
        propagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(orbit.getMu())));

        propagator.resetInitialState(new SpacecraftState(orbit));

        final double[] longitudes = { -179.0, -90.0, 0.0, 90.0 };
        this.computeVerification(propagator, iniDate.shiftedBy(3 * period), longitudes, "retro");

    }

    /**
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_LONGITUDE_DETECTOR}
     * 
     * @testedMethod {@link LongitudeDetector#LongitudeDetector(final double longitudeToDetect, final BodyShape
     *               earth)}
     * @testedMethod {@link LongitudeDetector#eventOccurred(final SpacecraftState s, final boolean increasing)}
     * 
     * @description test of the first longitude to 40 deg detected for numerical propagator with a GTO orbit
     * 
     * @input numerical propagator, GTO orbit a = 40 000 km, e=0.657, i=10 deg, pa=199 deg
     * 
     * @output the spacecraftstate when propagation stopped on the first detected event
     * 
     * @throws PatriusException
     *         Should not happen
     * 
     * @testPassCriteria difference between longitude of the spacecraftstate when propagation stopped and 40 deg
     *                   less than {@link LongitudeDetector#EPSILON_LONGITUDE} Justification : covering RCOV_500 for
     *                   angle
     * @testPassCriteria difference between date of the spacacrafstate when propagation stopped is before the date
     *                   until the propagation should go if there is no event detected
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion none
     */
    @Test
    public void testConstructorAndOccured() throws PatriusException {

        final AbsoluteDate iniDate = new AbsoluteDate("2011-11-09T12:00:00Z", TimeScalesFactory.getTT());

        final KeplerianOrbit orbit = new KeplerianOrbit(30000e3, 0.657, MathLib.toRadians(10.0),
            MathLib.toRadians(199.0), 0.0, 0.0, PositionAngle.TRUE, FramesFactory.getGCRF(), iniDate,
            Constants.EGM96_EARTH_MU);

        final double period = orbit.getKeplerianPeriod();

        final double[] absTOL = { 1e-5, 1e-5, 1e-5, 1e-8, 1e-8, 1e-8 };
        final double[] relTOL = { 1e-10, 1e-10, 1e-10, 1e-10, 1e-10, 1e-10 };
        final AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(0.1, 60., absTOL, relTOL);
        final NumericalPropagator propagator = new NumericalPropagator(integrator);
        propagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(orbit.getMu())));

        propagator.resetInitialState(new SpacecraftState(orbit));

        final LongitudeDetector curentDetector =
            new LongitudeDetector(MathLib.toRadians(40), this.earth.getBodyFrame());

        propagator.addEventDetector(curentDetector);
        final SpacecraftState sstate = propagator.propagate(iniDate.shiftedBy(3 * period));

        final AbsoluteDate propagationDate = sstate.getDate();
        Assert.assertTrue(sstate.getDate().compareTo(iniDate.shiftedBy(3 * period)) < 0);

        final EllipsoidPoint propagationGeoPoint = this.earth.buildPoint(sstate.getPVCoordinates().getPosition(),
            sstate.getFrame(), propagationDate, "");
        Assert.assertEquals(MathLib.toRadians(40.0), propagationGeoPoint.getLLHCoordinates().getLongitude(),
            EPSILON_LONGITUDE);
    }

    /**
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_LONGITUDE_DETECTOR}
     * 
     * @testedMethod {@link LongitudeDetector#LongitudeDetector(final double longitudeToDetect, final BodyShape
     *               earth,
     *               double, double, Action)}
     * 
     * @description simple constructor test
     * 
     * @input constructor parameters : longitudeToDetect, bodyFrame max check, threshold and STOP.Action
     * 
     * @output a {@link LongitudeDetector}
     * 
     * @testPassCriteria the {@link LongitudeDetector} is successfully created
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion none
     */
    @Test
    public void testConstructor() {

        final LongitudeDetector detector =
            new LongitudeDetector(MathLib.toRadians(40), this.earth.getBodyFrame(), 10, 0.1,
                Action.STOP);
        final LongitudeDetector detector2 = (LongitudeDetector) detector.copy();
        // Test getter
        Assert.assertEquals(this.earth.getBodyFrame(), detector2.getBodyFrame());
        Assert.assertEquals(MathLib.toRadians(40), detector2.getLongitudeToDetect(), Utils.epsilonTest);
    }

    /**
     * @throws PatriusException
     *         if error occurs in the propagation or in the detector creation
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_LONGITUDE_DETECTOR}
     * 
     * @testedMethod {@link LongitudeDetector#LongitudeDetector(double, Frame, double, double, Action, boolean, int)}
     * 
     * @description test of the slopeSelection parameter
     * 
     * @input constructor parameters: the longitude angle, the max check
     *        value, the threshold value, the celestial body frame, the action,
     *        the remove boolean, the sun body and the slopeSelection.
     * 
     * @output a {@link LongitudeDetector}
     * 
     * @testPassCriteria the {@link LongitudeDetector} when initiated with increasing parameter only detect increasing
     *                   event.
     * 
     * @referenceVersion 4.13
     * 
     * @nonRegressionVersion 4.13
     */
    @Test
    public void testLongitudeDetectorIncreasingDecreasing() throws PatriusException {
        // Detector in increasing mode
        final LongitudeDetector detectorIncrease = new LongitudeDetector(FastMath.PI * 0, this.earth.getBodyFrame(),
            600, 1e-6, Action.CONTINUE, false, 0);
        // Detector in decreasing mode
        final LongitudeDetector detectorDecrease = new LongitudeDetector(FastMath.PI * 0, this.earth.getBodyFrame(),
            600, 1e-6, Action.CONTINUE, false, 1);
        // Detector in increasing & decreasing mode
        final LongitudeDetector detector = new LongitudeDetector(FastMath.PI * 0, this.earth.getBodyFrame(), 600, 1e-6,
            Action.CONTINUE, false);

        final AbsoluteDate iniDate = new AbsoluteDate("2011-11-09T12:00:00Z", TimeScalesFactory.getTT());

        final KeplerianOrbit orbit = new KeplerianOrbit(30000e3, 0.657, MathLib.toRadians(10.0),
            MathLib.toRadians(199.0), 0.0, 0.0, PositionAngle.TRUE, FramesFactory.getGCRF(), iniDate,
            Constants.EGM96_EARTH_MU);

        final double period = orbit.getKeplerianPeriod();
        final Propagator propagator = new KeplerianPropagator(orbit);

        // Step handler to track longitude evolution during propagation
        final MyStepHandler angleTracking = new MyStepHandler(iniDate, orbit.getFrame());
        propagator.setMasterMode(10, angleTracking);

        // Create one logger for each detector
        final EventsLogger loggerIncrease = new EventsLogger();
        final EventsLogger loggerDecrease = new EventsLogger();
        final EventsLogger logger = new EventsLogger();

        // Add the three different loggers to the propagator
        propagator.addEventDetector(loggerIncrease.monitorDetector(detectorIncrease));
        propagator.addEventDetector(loggerDecrease.monitorDetector(detectorDecrease));
        propagator.addEventDetector(logger.monitorDetector(detector));

        // Propagate
        propagator.propagate(iniDate.shiftedBy(5 * period));
        // Asserts
        for (int i = 0; i < loggerIncrease.getLoggedEvents().size(); i++) {
            Assert.assertTrue(loggerIncrease.getLoggedEvents().get(i).isIncreasing());
        }
        for (int j = 0; j < loggerDecrease.getLoggedEvents().size(); j++) {
            Assert.assertFalse(loggerDecrease.getLoggedEvents().get(j).isIncreasing());
        }
        Assert.assertEquals(loggerIncrease.getLoggedEvents().size() + loggerDecrease.getLoggedEvents().size(),
            logger.getLoggedEvents().size());
        Assert.assertEquals(2, loggerIncrease.getLoggedEvents().size());
        Assert.assertEquals(0, loggerDecrease.getLoggedEvents().size());
        Assert.assertEquals(2, logger.getLoggedEvents().size());
    }

    /**
     * @throws PatriusException
     *         if error occurs in the propagation or in the detector creation
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_LONGITUDE_DETECTOR}
     * 
     * @testedMethod {@link LongitudeDetector#LongitudeDetector(double, Frame, double, double, Action, boolean, int)}
     * 
     * @description test of the slopeSelection parameter
     * 
     * @input constructor parameters: the longitude angle, the max check
     *        value, the threshold value, the celestial body frame, the action,
     *        the remove boolean, the sun body and the slopeSelection.
     * 
     * @output a {@link LongitudeDetector}
     * 
     * @testPassCriteria the {@link LongitudeDetector} when initiated with increasing parameter only detect increasing
     *                   event.
     * 
     * 
     * @referenceVersion 4.13
     * 
     * @nonRegressionVersion 4.13
     */
    @Test
    public void testLongitudeDetectorIncreasingDecreasingBackward() throws PatriusException {
        // Detector in increasing mode
        final LongitudeDetector detectorIncrease = new LongitudeDetector(FastMath.PI * 0, this.earth.getBodyFrame(),
            600, 1e-6, Action.CONTINUE, false, 0);
        // Detector in decreasing mode
        final LongitudeDetector detectorDecrease = new LongitudeDetector(FastMath.PI * 0, this.earth.getBodyFrame(),
            600, 1e-6, Action.CONTINUE, false, 1);
        // Detector in increasing & decreasing mode
        final LongitudeDetector detector = new LongitudeDetector(FastMath.PI * 0, this.earth.getBodyFrame(), 600, 1e-6,
            Action.CONTINUE, false);

        final AbsoluteDate iniDate = new AbsoluteDate("2011-11-09T12:00:00Z", TimeScalesFactory.getTT());

        final KeplerianOrbit orbit = new KeplerianOrbit(30000e3, 0.657, MathLib.toRadians(10.0),
            MathLib.toRadians(199.0), 0.0, 0.0, PositionAngle.TRUE, FramesFactory.getGCRF(), iniDate,
            Constants.EGM96_EARTH_MU);

        final double period = orbit.getKeplerianPeriod();
        final Propagator propagator = new KeplerianPropagator(orbit);
        propagator.propagate(iniDate.shiftedBy(5 * period));

        // Step handler to track longitude evolution during propagation
        final MyStepHandler angleTracking = new MyStepHandler(iniDate, orbit.getFrame());
        propagator.setMasterMode(10, angleTracking);

        // Create one logger for each detector
        final EventsLogger loggerIncrease = new EventsLogger();
        final EventsLogger loggerDecrease = new EventsLogger();
        final EventsLogger logger = new EventsLogger();

        // Add the three different loggers to the propagator
        propagator.addEventDetector(loggerIncrease.monitorDetector(detectorIncrease));
        propagator.addEventDetector(loggerDecrease.monitorDetector(detectorDecrease));
        propagator.addEventDetector(logger.monitorDetector(detector));

        // Propagate backward
        propagator.propagate(iniDate);

        // Asserts
        Assert.assertEquals(2, loggerIncrease.getLoggedEvents().size());
        Assert.assertEquals(0, loggerDecrease.getLoggedEvents().size());
        Assert.assertEquals(2, logger.getLoggedEvents().size());
    }

    /**
     * 
     * Main method to valid the detection :
     * creates detector for each longitude, add the detectors to the propagator,
     * compare each detected longitude to the longitude to detect
     * 
     * @precondition none
     * 
     * @param propagator
     *        : the propagator to validate with the orbit type
     * @param end
     *        : final date to propagate to
     * @param longitudes
     *        : List of longitudes to detect. Unit : deg
     * @throws PatriusException
     *         should not happen
     * 
     */
    public void computeVerification(final Propagator propagator, final AbsoluteDate end,
                                    final double[] longitudes, final String testCase)
        throws PatriusException {

        // Step handler to track longitude evolution during propagation
        final MyStepHandler angleTracking = new MyStepHandler(propagator.getInitialState().getDate(),
            FramesFactory.getTIRF());
        propagator.setMasterMode(10, angleTracking);

        // list of detector to get results
        final List<LongitudeDetector> longiDetectList = new ArrayList<>();

        // Log event to test detected events
        final EventsLogger logger = new EventsLogger();

        for (final double longitude : longitudes) {
            final LongitudeDetector curentDetector = new LongitudeDetector(MathLib.toRadians(longitude),
                this.earth.getBodyFrame())
            {
                /** Serializable UID. */
                private static final long serialVersionUID = 6050388605586127764L;

                /** Overrided method eventOccured to continue */
                @Override
                public Action eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward) {
                    return Action.CONTINUE;
                }
            };
            propagator.addEventDetector(logger.monitorDetector(curentDetector));
            longiDetectList.add(curentDetector);
        }

        propagator.propagate(end);

        for (final LoggedEvent event : logger.getLoggedEvents()) {
            final SpacecraftState sstate = event.getState();
            sstate.getPVCoordinates().getPosition();
            final EllipsoidPoint elPoint = this.earth.buildPoint(sstate.getPVCoordinates().getPosition(),
                sstate.getFrame(), sstate.getDate(), "");

            final double longitudeToDetect = ((LongitudeDetector) event.getEventDetector()).getLongitudeToDetect();
            final double detectedLongitude = elPoint.getLLHCoordinates().getLongitude();

            Assert.assertEquals(longitudeToDetect, detectedLongitude, EPSILON_LONGITUDE);
        }

        // Write results from step handler and detector and logger to plot in scilab for SVS
        // ResultsFileWriter.writeResultsToPlot("longitude", testCase+"LongitudeTracking.txt", angleTracking.results);
        // int i=0;
        // for (LongitudeDetector curentLD : longiDetectList){
        // String filename = testCase+"detector_"+i+".txt";
        // i++;
        // ResultsFileWriter.writeResultsToPlot("longitude", filename, curentLD.results);
        // }
    }

    /**
     * 
     * Implementation of a step handler to track the evolution
     * of the physical value to test
     * 
     */
    class MyStepHandler implements PatriusFixedStepHandler {

        /** Serializable UID. */
        private static final long serialVersionUID = 5643736397373186L;

        /** results */
        public ArrayList<double[]> results;

        /** initial date */
        private final AbsoluteDate fromDate;

        /** initial date */
        private final Frame bodyFrame;

        /**
         * simple constructor
         * 
         * @param date
         *        initialDate of propagation
         * @throws PatriusException
         */
        public MyStepHandler(final AbsoluteDate date, final Frame frame) {
            this.results = new ArrayList<>();
            this.fromDate = date;
            this.bodyFrame = frame;
        }

        @Override
        public void init(final SpacecraftState s0, final AbsoluteDate t) {
            // nothing to do
        }

        @Override
        public void handleStep(final SpacecraftState s, final boolean isLast) {

            try {

                // spacecraft position projected in central body frame (O,x,y) plane
                final Vector3D pInCBodyFrame = s.getPVCoordinates(this.bodyFrame).getPosition();
                final Vector3D projPosition = new Vector3D(pInCBodyFrame.getX(), pInCBodyFrame.getY(), 0.0);

                // longitude is the angle between x axis
                // and the projection of the position in the central body frame (O,x,y) plane.
                final Vector3D xAxis = Vector3D.PLUS_I;
                double longitude = Vector3D.angle(xAxis, projPosition);

                // The "angle" function returns a value between 0 and PI, while we are working with angle between -PI
                // and PI
                // : when z-component of the cross product between the two vectors is negative, returning -1 * angle:
                if (Vector3D.crossProduct(xAxis, projPosition).getZ() < 0) {
                    // The "angle" function returns a value between 0 and PI, while we are working with angle between
                    // -PI and PI
                    // : when z-component of the cross product between the two vectors is negative, returning -1 *
                    // angle:
                    longitude = -1 * longitude;

                }

                final double[] currentResult =
                {
                    s.getDate().durationFrom(this.fromDate),
                    longitude
                };

                this.results.add(currentResult);
            } catch (final PatriusException e) {
                e.printStackTrace();
            }
        }
    }
}
