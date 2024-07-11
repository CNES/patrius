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
 * @history creation 23/04/2015
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2324:27/05/2020:Ajout de la conversion LTAN => RAAN dans LocalTimeAngle 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:346:23/04/2015:creation of a local time class
 * VERSION::DM:439:17/06/2016:correction of LocalTimeTest
 * VERSION::FA:680:27/09/2016:correction local time computation
 * VERSION::FA:902:13/12/2016:corrected anomaly on local time computation
 * VERSION::DM:710:22/03/2016:local time angle computation in [-PI, PI[
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.time;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.SortedMap;
import java.util.TreeMap;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.MeeusSun;
import fr.cnes.sirius.patrius.bodies.MeeusSun.MODEL;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.CircularOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.LocalTimeAngleDetector;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * @description <p>
 *              Test class for {@link LocalTimeAngle} class
 *              </p>
 * 
 * @see {@link LocalTimeAngle}
 * 
 * @author Emmanuel Bignon
 * 
 * @version $Id: LocalTimeAngleTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 3.0
 * 
 */
public class LocalTimeAngleTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle True local time computation
         * 
         * @featureDescription tests true local time angle computation
         */
        TRUE_LOCAL_TIME_ANGLE,

        /**
         * @featureTitle Mean local time computation
         * 
         * @featureDescription tests mean local time computation
         */
        MEAN_LOCAL_TIME
    }

    /** Sun. */
    private static PVCoordinatesProvider sun;

    /** Local time. */
    private static LocalTimeAngle localTime;
    
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
        Utils.setDataRoot("regular-dataCNES-2003");
        FramesFactory.setConfiguration(fr.cnes.sirius.patrius.Utils.getIERS2003ConfigurationWOEOP(true));
    }
    
    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#MEAN_LOCAL_TIME}
     * 
     * @testedMethod {@link LocalTimeAngle#computeMeanLocalTimeAngle(Orbit)}
     * @testedMethod {@link LocalTimeAngle#computeMeanLocalTimeAngle(AbsoluteDate, Vector3D, Frame)}
     * 
     * @description tests mean local time computation
     * 
     * @input date and position
     * 
     * @output mean local time
     * 
     * @testPassCriteria mean local time error with respect to reference lower than 6E-5 (relative difference)
     *                   tolerance is large since sun ephemeris models are different (Newcomb not available in PATRIUS)
     * 
     * @referenceVersion 3.3
     * 
     * @nonRegressionVersion 3.3
     */
    @Test
    public final void meanLocalTimeAngleTest() throws PatriusException {
        Utils.setDataRoot("regular-dataCNES-2003");
        sun = CelestialBodyFactory.getSun();
        localTime = new LocalTimeAngle(sun);

        try {
            final Vector3D pos = new Vector3D(7000E3, 1000E3, 0);

            // Test mean local time computation in TIRF
            final AbsoluteDate date = AbsoluteDate.FIFTIES_EPOCH_TT.shiftedBy(20000. * Constants.JULIAN_DAY + 85000);
            final Orbit orbit = new CartesianOrbit(new PVCoordinates(pos, new Vector3D(0, 1E3, 0)),
                FramesFactory.getGCRF(), date, Constants.EGM96_EARTH_MU);
            final double actual = localTime.computeMeanLocalTimeAngle(orbit);
            final double expected = localTime.computeMeanLocalTimeAngle(date, pos, FramesFactory.getGCRF());
            Assert.assertEquals(actual, expected, 0.);

            // Test mean local time computation in another frame (VEIS) - 2 cases
            // Since reference values have been generated with Newcomb model not available in PATRIUS, results are not
            // very accurate

            // First case
            final AbsoluteDate date1 = AbsoluteDate.FIFTIES_EPOCH_TT.shiftedBy(20200. * Constants.JULIAN_DAY + 50.);
            final double trueLocalTime1 = localTime.computeTrueLocalTimeAngle(date1, pos, FramesFactory.getVeis1950());
            final double actual1 = localTime.computeMeanLocalTimeAngle(date1, pos, FramesFactory.getVeis1950());
            final double expected1 = trueLocalTime1 - 84.07440307930665 / Constants.RADIANS_TO_SEC;
            Assert.assertEquals((actual1 - expected1) / expected1, 0, 6E-5);

            // Second case
            final AbsoluteDate date2 = AbsoluteDate.FIFTIES_EPOCH_TT.shiftedBy(20300. * Constants.JULIAN_DAY);
            final double trueLocalTime2 = localTime.computeTrueLocalTimeAngle(date2, pos, FramesFactory.getVeis1950());
            final double actual2 = localTime.computeMeanLocalTimeAngle(date2, pos, FramesFactory.getVeis1950());
            final double expected2 = trueLocalTime2 - -383.0924484584539 / Constants.RADIANS_TO_SEC;
            Assert.assertEquals((actual2 - expected2) / expected2, 0, 6E-5);

        } catch (final PatriusException e) {
            Assert.fail();
        }
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#TRUE_LOCAL_TIME_ANGLE}
     * 
     * @testedMethod {@link LocalTimeAngle#computeTrueLocalTimeAngle(Orbit)}
     * 
     * @description Test computation of true local time angle in default frame (TIRF)
     * 
     * @input date and position
     * 
     * @output true local time angle
     * 
     * @testPassCriteria Comparison OK between true local time angle computed with
     *                   {@link LocalTimeAngle#computeTrueLocalTimeAngle(Orbit)} and
     *                   {@link LocalTimeAngle#computeTrueLocalTimeAngle(AbsoluteDate, Vector3D, Frame)}
     * 
     * @comments Test for coverage purpose
     * 
     * @referenceVersion 3.3
     * 
     * @nonRegressionVersion 3.3
     */
    @Test
    public final void trueLocalTimeAngleTest() throws PatriusException {
        Utils.setDataRoot("regular-dataCNES-2003");
        sun = CelestialBodyFactory.getSun();
        localTime = new LocalTimeAngle(sun);

        final AbsoluteDate date = AbsoluteDate.FIFTIES_EPOCH_TT.shiftedBy(20000. * Constants.JULIAN_DAY + 85000);
        final Vector3D pos = new Vector3D(7.0e6, 1.0e6, 4.0e6);
        final Orbit orbit = new CartesianOrbit(new PVCoordinates(pos, new Vector3D(0, 1E3, 0)),
            FramesFactory.getGCRF(), date, Constants.EGM96_EARTH_MU);
        final double actual = localTime.computeTrueLocalTimeAngle(orbit);
        final double expected = localTime.computeTrueLocalTimeAngle(date, pos, FramesFactory.getGCRF());
        Assert.assertEquals(actual, expected, 0.);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#TRUE_LOCAL_TIME_ANGLE}
     * 
     * @testedMethod {@link LocalTimeAngleDetector#g(SpacecraftState)}
     * 
     * @description test computation of true local time.
     * 
     * @input an orbit
     * 
     * @output true local time
     * 
     * @testPassCriteria the local time angle is the expected one (reference: math).
     * 
     * @referenceVersion 3.4
     * 
     * @nonRegressionVersion 3.4
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @Test
    public final void trueLocalTimeAngleTest2() throws PatriusException {

        // Initialization
        sun = CelestialBodyFactory.getSun();
        localTime = new LocalTimeAngle(sun);

        final AbsoluteDate iniDate = new AbsoluteDate("2011-11-09T12:00:00Z", TimeScalesFactory.getTT());
        final double mu = CelestialBodyFactory.getEarth().getGM();

        final CircularOrbit orbitRetro = new CircularOrbit(8000000, 0, 0, MathLib.toRadians(98.0), 0, FastMath.PI / 2,
            PositionAngle.TRUE, FramesFactory.getGCRF(), iniDate, mu);
        final SpacecraftState state = new SpacecraftState(orbitRetro);

        // Actual value
        final double actual = localTime.computeTrueLocalTimeAngle(state.getDate(), state.getPVCoordinates()
            .getPosition(), FramesFactory.getGCRF());

        // Expected value
        final Vector3D sun = CelestialBodyFactory.getSun().getPVCoordinates(state.getDate(), FramesFactory.getTIRF())
            .getPosition();
        final Vector3D satellite = state.getPVCoordinates(FramesFactory.getTIRF()).getPosition();
        final Vector3D sunpos = new Vector3D(sun.getX(), sun.getY(), 0);
        final Vector3D satpos = new Vector3D(satellite.getX(), satellite.getY(), 0);
        final double expected = Vector3D.angle(sunpos, satpos);

        // Check
        Assert.assertEquals(expected, actual, Utils.epsilonTest);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#TRUE_LOCAL_TIME_ANGLE}
     * 
     * @testedMethod {@link LocalTimeAngle#computeRAANFromTrueLocalTime(AbsoluteDate, double, Frame)}
     * @testedMethod {@link LocalTimeAngle#computeRAANFromMeanLocalTime(AbsoluteDate, double)}
     * 
     * @description test computation of RAAN from local time.
     * 
     * @input date and local time
     * 
     * @output RAAN
     * 
     * @testPassCriteria the RAAN is as expected (reference: Celestlab, function CL_op_locTime, threshold: 1E-6/1E-12).
     * 
     * @referenceVersion 4.5
     * 
     * @nonRegressionVersion 4.5
     */
    @Test
    public final void testRAAN() throws PatriusException {
        // UTC - TAI = 37s
        TimeScalesFactory.clearUTCTAILoaders();
        TimeScalesFactory.addUTCTAILoader(new MyUTCTAILoader());

        // Initialization
        final LocalTimeAngle localTimeAngle = new LocalTimeAngle(new MeeusSun(MODEL.STANDARD));
        final AbsoluteDate date = new AbsoluteDate(2009, 9, 7, 10, 29, 30.5, TimeScalesFactory.getUTC());

        // RAAN from true local time
        // Accuracy limited by Scilab slightly different frame configuration (use of "fast conversion")
        final double trueLocalTime = 1.23;
        final double expected1 = 4.12743870309230590;
        final double actual1 = localTimeAngle.computeRAANFromTrueLocalTime(date, trueLocalTime, FramesFactory.getCIRF());
        Assert.assertEquals(0, MathLib.abs((expected1 - actual1) / expected1), 3E-7);

        // RAAN from mean local time
        // Accuracy limited by Scilab use of CNES julian date
        final double meanLocalTime = 1.23;
        final double expected2 = 4.13613628901215510;
        final double actual2 = localTimeAngle.computeRAANFromMeanLocalTime(date, meanLocalTime);
        Assert.assertEquals(0, MathLib.abs((expected2 - actual2) / expected2), 1E-12);
        
        // Exception
        try {
            localTimeAngle.computeRAANFromTrueLocalTime(date, trueLocalTime, FramesFactory.getTIRF());
            Assert.fail();
        } catch (final PatriusException e) {
            // Expected
            Assert.assertTrue(true);
        }
    }

    /**
     * Constant UTC-TAI loader.
     */
    private class MyUTCTAILoader implements UTCTAILoader {

        public MyUTCTAILoader() {
        }

        @Override
        public boolean stillAcceptsData() {
            return false;
        }

        @Override
        public void loadData(final InputStream input, final String name)
                                                                        throws IOException, ParseException,
                                                                        PatriusException {
        }

        @Override
        public SortedMap<DateComponents, Integer> loadTimeSteps() {
            final SortedMap<DateComponents, Integer> entries = new TreeMap<DateComponents, Integer>();
            for (int i = 1969; i < 2200; i++) {
                entries.put(new DateComponents(i, 1, 1), 37);
            }
            return entries;
        }

        @Override
        public String getSupportedNames() {
            return "";
        }
    }
}
