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
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration de la gestion des attractions gravitationnelles dans le propagateur
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.EllipsoidBodyShape;
import fr.cnes.sirius.patrius.bodies.EllipsoidPoint;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.forces.gravity.DirectBodyAttraction;
import fr.cnes.sirius.patrius.forces.gravity.NewtonianGravityModel;
import fr.cnes.sirius.patrius.frames.FramesFactory;
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
import fr.cnes.sirius.patrius.propagation.events.EventDetector.Action;
import fr.cnes.sirius.patrius.propagation.events.EventsLogger.LoggedEvent;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Unit tests for {@link ExtremaLongitudeDetector}.
 * 
 * @author chabaudp
 * 
 * @version $Id: ExtremaLongitudeDetectorTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.3
 * 
 */
public class ExtremaLongitudeDetectorTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Validate the satellite extrema longitude detector
         * 
         * @featureDescription Validate the satellite extrema longitude detector
         * 
         * @coveredRequirements DV-EVT_121
         */
        VALIDATE_EXTREMA_LONGITUDE_DETECTOR
    }

    /** earth shape */
    private EllipsoidBodyShape earth;

    /** initial date */
    private final AbsoluteDate initdate = new AbsoluteDate("2011-11-09T12:00:00Z", TimeScalesFactory.getTT());

    /** Epsilon longitude comparison */
    private static final double EPSILON_LONGITUDE = 0.5;

    /** Epsilon date comparison */
    private static final double EPSILON_DATE = 1;

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
     * @testedFeature {@link features#VALIDATE_EXTREMA_LONGITUDE_DETECTOR}
     * 
     * @testedMethod {@link ExtremaLongitudeDetector#g(SpacecraftState) }
     * 
     * @description test of the maximum longitude detection for keplerian propagator with a GTO orbit
     * 
     * @input keplerian propagator, GTO orbit a = 40 000 km, e=0.657, i=10 deg, pa=199 deg
     *        which present maximum longitude at about 72, -144, 0 deg on the period
     *        from 2011-11-09T12:00:00 during three orbital period
     * 
     * @output all the detected events
     * 
     * @throws PatriusException
     *         Should not happen
     * 
     * @testPassCriteria difference between longitude of the spacecraftstate when event occurred and
     *                   longitude to detect less than {@link ExtremaLongitudeDetectorTest#EPSILON_LONGITUDE}
     *                   Justification : unitary test need not precision in detection a larger EPSILON
     *                   will be robust to modification that have not to be tested in this unitary test
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion none
     */
    @Test
    public void testGTOOrbitMax() throws PatriusException {

        final KeplerianOrbit orbit = new KeplerianOrbit(30000e3, 0.657, MathLib.toRadians(10.0),
            MathLib.toRadians(199.0), 0.0, 0.0, PositionAngle.TRUE, FramesFactory.getGCRF(), this.initdate,
            Constants.EGM96_EARTH_MU);

        final double period = orbit.getKeplerianPeriod();

        final Propagator propagator = new KeplerianPropagator(orbit);

        final double[] longitudes = { 72.0, -144.0, 0.0 };

        this.computeVerification(propagator, this.initdate.shiftedBy(3 * period), longitudes,
            ExtremaLongitudeDetector.MAX);

    }

    /**
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_EXTREMA_LONGITUDE_DETECTOR}
     * 
     * @testedMethod {@link ExtremaLongitudeDetector#g(SpacecraftState) }
     * 
     * @description test of the minimum longitude detection for keplerian propagator with a GTO orbit
     * 
     * @input keplerian propagator, GTO orbit a = 40 000 km, e=0.657, i=10 deg, pa=199 deg
     *        which present minimum longitude at about 14, 158, -58 deg for the period
     *        from 2011-11-09T12:00:00 during three orbital period
     * 
     * @output all the detected events
     * 
     * @throws PatriusException
     *         should not happen
     * 
     * @testPassCriteria difference between longitude of the spacecraftstate when event occurred and
     *                   longitude to detect less than {@link ExtremaLongitudeDetectorTest#EPSILON_LONGITUDE}
     *                   Justification : unitary test need not precision in detection a larger EPSILON
     *                   will be robust to modification that have not to be tested in this unitary test
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion none
     */
    @Test
    public void testGTOOrbitMin() throws PatriusException {

        final KeplerianOrbit orbit = new KeplerianOrbit(30000e3, 0.657, MathLib.toRadians(10.0),
            MathLib.toRadians(199.0), 0.0, 0.0, PositionAngle.TRUE, FramesFactory.getGCRF(), this.initdate,
            Constants.EGM96_EARTH_MU);

        final double period = orbit.getKeplerianPeriod();

        final Propagator propagator = new KeplerianPropagator(orbit);

        final double[] longitudes = { 14.0, 158.0, -58.0 };

        this.computeVerification(propagator, this.initdate.shiftedBy(3 * period), longitudes,
            ExtremaLongitudeDetector.MIN);

    }

    /**
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_EXTREMA_LONGITUDE_DETECTOR}
     * 
     * @testedMethod {@link ExtremaLongitudeDetector#g(SpacecraftState) }
     * 
     * @description test of the min and max longitude detection for keplerian propagator with a GTO orbit
     * 
     * @input keplerian propagator, GTO orbit a = 40 000 km, e=0.657, i=10 deg, pa=199 deg
     *        which present extrema longitude at about 72 (max), 14 (min), -144 (max),
     *        158 (min), 0 (max), -58 (min) for period from 2011-11-09T12:00:00 during three orbital period
     * 
     * @output all the detected events
     * 
     * @throws PatriusException
     *         Should not happen
     * 
     * @testPassCriteria difference between longitude of the spacecraftstate when event occurred and
     *                   longitude to detect less than {@link ExtremaLongitudeDetectorTest#EPSILON_LONGITUDE}
     *                   Justification : unitary test need not precision in detection a larger EPSILON
     *                   will be robust to modification that have not to be tested in this unitary test
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion none
     */
    @Test
    public void testGTOOrbitMinMax() throws PatriusException {

        final KeplerianOrbit orbit = new KeplerianOrbit(30000e3, 0.657, MathLib.toRadians(10.0),
            MathLib.toRadians(199.0), 0.0, 0.0, PositionAngle.TRUE, FramesFactory.getGCRF(), this.initdate,
            Constants.EGM96_EARTH_MU);

        final double period = orbit.getKeplerianPeriod();

        final Propagator propagator = new KeplerianPropagator(orbit);

        final double[] longitudes = { 72.0, 14.0, -144.0, 158.0, 0.0, -58.0 };

        this.computeVerification(propagator, this.initdate.shiftedBy(3 * period), longitudes,
            ExtremaLongitudeDetector.MIN_MAX);

    }

    /**
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_EXTREMA_LONGITUDE_DETECTOR}
     * 
     * @testedMethod {@link ExtremaLongitudeDetector#ExtremaLongitudeDetector(final int extremumType, final Frame
     *               bodyFrame)}
     * @testedMethod {@link ExtremaLongitudeDetector#eventOccurred(final SpacecraftState s, final boolean increasing)}
     * 
     * @description test of the first min longitude detected for numerical propagator with a GTO orbit
     * 
     * @input numerical propagator, GTO orbit a = 40 000 km, e=0.657, i=10 deg, pa=199 deg
     *        which present a first local minimum longitude at about 14 deg on 2011-11-09T23:47:28.617
     *        in the period from 2011-11-09T12:00:00 during three orbital period
     * 
     * @output the spacecraftstate when propagation stopped on the first detected event
     * 
     * @throws PatriusException
     *         Should not happen
     * 
     * @testPassCriteria difference between longitude of the spacecraftstate when propagation stopped and 14 deg
     *                   less than {@link ExtremaLongitudeDetectorTest#EPSILON_LONGITUDE}
     * @testPassCriteria difference between date of the spacacrafstate when propagation stopped and
     *                   2011-11-09T23:47:28.617
     *                   less than {@link ExtremaLongitudeDetectorTest#EPSILON_DATE} Justification : unitary test need
     *                   not precision in detection a larger EPSILON
     *                   will be robust to modification that have not to be tested in this unitary test
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion none
     */
    @Test
    public void testConstructorAndOccured() throws PatriusException {

        final KeplerianOrbit orbit = new KeplerianOrbit(30000e3, 0.657, MathLib.toRadians(10.0),
            MathLib.toRadians(199.0), 0.0, 0.0, PositionAngle.TRUE, FramesFactory.getGCRF(), this.initdate,
            Constants.EGM96_EARTH_MU);

        final double period = orbit.getKeplerianPeriod();

        final double[] absTOL = { 1e-5, 1e-5, 1e-5, 1e-8, 1e-8, 1e-8 };
        final double[] relTOL = { 1e-10, 1e-10, 1e-10, 1e-10, 1e-10, 1e-10 };
        final AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(0.1, 60., absTOL, relTOL);
        final NumericalPropagator propagator = new NumericalPropagator(integrator);

        propagator.resetInitialState(new SpacecraftState(orbit));

        final ExtremaLongitudeDetector curentDetector = new ExtremaLongitudeDetector(ExtremaLongitudeDetector.MIN,
            this.earth.getBodyFrame());

        propagator.addEventDetector(curentDetector);
        propagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(propagator
            .getInitialState().getMu())));
        final SpacecraftState sstate = propagator.propagate(this.initdate.shiftedBy(3 * period));

        final AbsoluteDate expectedDate = new AbsoluteDate(2011, 11, 9, 23, 47, 28, TimeScalesFactory.getUTC());
        final AbsoluteDate propagationDate = sstate.getDate();
        final double delta = MathLib.abs(propagationDate.durationFrom(expectedDate));
        Assert.assertTrue("delta is " + delta, delta < EPSILON_DATE);

        final EllipsoidPoint propagationElPoint = this.earth.buildPoint(sstate.getPVCoordinates().getPosition(),
            sstate.getFrame(), propagationDate, "");
        Assert.assertEquals(MathLib.toRadians(14.0), propagationElPoint.getLLHCoordinates().getLongitude(),
            EPSILON_LONGITUDE);
    }

    /**
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_EXTREMA_LONGITUDE_DETECTOR}
     * 
     * @testedMethod {@link ExtremaLongitudeDetector#g(SpacecraftState) }
     * 
     * @description test of the extrema longitude detection for keplerian propagator with a low orbit
     *              without any extrema
     * 
     * @input keplerian propagator, with a standard circular orbit
     * 
     * @output spacecraftstate at final date to propagate
     * 
     * @throws PatriusException
     *         Should not happen
     * 
     * @testPassCriteria propagator never stop because no event are detected :
     *                   the spacecraftstate date is equal to the date to propagate
     *                   Justification : unitary test need not precision in detection a larger EPSILON
     *                   will be robust to modification that have not to be tested in this unitary test
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion none
     */
    @Test
    public void testLowOrbitMinMax() throws PatriusException {

        // Create Orbit
        final CircularOrbit orbit = new CircularOrbit(8000000, 0, 0, MathLib.toRadians(60.0), 0, FastMath.PI / 2,
            PositionAngle.TRUE, FramesFactory.getGCRF(), this.initdate, Constants.EGM96_EARTH_MU);

        // Create propagator, detector and add the detector
        final Propagator propagator = new KeplerianPropagator(orbit);
        final ExtremaLongitudeDetector curentDetector = new ExtremaLongitudeDetector(ExtremaLongitudeDetector.MIN_MAX,
            this.earth.getBodyFrame());
        propagator.addEventDetector(curentDetector);

        // Propagate until three orbital period
        final double period = orbit.getKeplerianPeriod();
        final AbsoluteDate endDate = this.initdate.shiftedBy(3 * period);
        final SpacecraftState sstate = propagator.propagate(endDate);

        // Validate the propagator never stop until the endDate
        final AbsoluteDate propagationDate = sstate.getDate();
        Assert.assertTrue(propagationDate.compareTo(endDate) == 0);

    }

    /**
     * 
     * Main method to valid the detection :
     * creates an extrema longitude detector and add it to the propagator,
     * compare each detected longitude to the reference list of extrema
     * 
     * @precondition none
     * 
     * @param propagator
     *        : the propagator to validate with the orbit type
     * @param end
     *        : final date to propagate to in AbsoluteDate
     * @param longitudes
     *        : list of reference extrema longitude in degree
     * @param upOrDown
     *        : the way to test the detector
     *        ExtremaLongitudeDetector.MIN to detect when the ground track goes from a negative to a positive
     *        longitude evolution
     *        ExtremaLongitudeDetector.MAX to detect when the ground track goes from a positive to a negative
     *        longitude evolution
     *        ExtremaLongitudeDetector.MIN_MAX to detect both
     * @throws PatriusException
     *         should not happen
     * 
     */
    public void computeVerification(final Propagator propagator, final AbsoluteDate end,
                                    final double[] longitudes, final int upOrDown)
        throws PatriusException {

        final EventsLogger logger = new EventsLogger();

        final ExtremaLongitudeDetector curentDetector =
            new ExtremaLongitudeDetector(upOrDown, this.earth.getBodyFrame(),
                180.0, 1e-6){
                private static final long serialVersionUID = 4529781719692037999L;

                @Override
                public Action
                    eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                        throws PatriusException {
                    return Action.CONTINUE;
                }
            };

        propagator.addEventDetector(logger.monitorDetector(curentDetector));

        propagator.propagate(end);

        int i = 0;
        for (final LoggedEvent event : logger.getLoggedEvents()) {
            final SpacecraftState sstate = event.getState();
            final EllipsoidPoint elPoint = this.earth.buildPoint(sstate.getPVCoordinates().getPosition(),
                sstate.getFrame(), sstate.getDate(), "");
            final double detectedLongitude = MathLib.toDegrees(elPoint.getLLHCoordinates().getLongitude());
            Assert.assertEquals(longitudes[i], detectedLongitude, EPSILON_LONGITUDE);
            // System.out.println("Date : " + sstate.getDate().toString()
            // + ", Longitude :" + detectedLongitude + ", Altitude :" + geoPoint.getAltitude()
            // + ", Vitesse :" + sstate.getPVCoordinates(earth.getBodyFrame()).getVelocity().toString());
            i++;
        }
    }

    /**
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_EXTREMA_LONGITUDE_DETECTOR}
     * 
     * @testedMethod {@link ExtremaLongitudeDetector#ExtremaLongitudeDetector(final int extremumType, final Frame
     *               bodyFrame, double, double, Action)}
     * 
     * @description simple constructor test
     * 
     * @input constructor parameters: extremumType, bodyFrame the max check
     *        value and the threshold value and the STOP Action.
     * 
     * @output a {@link ExtremaLongitudeDetector}
     * 
     * 
     * @testPassCriteria the {@link ExtremaLongitudeDetector} is successfully created
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion none
     */
    @Test
    public void testConstructor2() {
        final ExtremaLongitudeDetector detector = new ExtremaLongitudeDetector(ExtremaLongitudeDetector.MIN,
            this.earth.getBodyFrame(), 10, 0.1, Action.STOP);
        final ExtremaLongitudeDetector detector2 = (ExtremaLongitudeDetector) detector.copy();
        // Test getter
        Assert.assertEquals(this.earth.getBodyFrame(), detector2.getBodyFrame());
    }
}
