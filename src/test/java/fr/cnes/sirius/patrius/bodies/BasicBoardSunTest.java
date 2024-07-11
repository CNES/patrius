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
 * VERSION:4.9:DM:DM-3161:10/05/2022:[PATRIUS] Ajout d'une methode getNativeFrame() a l'interface PVCoordinatesProvider 
 * VERSION:4.9:DM:DM-3165:10/05/2022:[PATRIUS] Amelioration de la propagation du signal 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:219:03/04/2014:Changed API and corrected reference frame
 * VERSION::FA:602:18/10/2016:Correct timescale
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.assembly.models.RFLinkBudgetTest;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.time.UTCTAILoader;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * <p>
 * Test class for the BasicBoardSun model.
 * </p>
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id$
 * 
 * @since 1.3
 */
public class BasicBoardSunTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle basic board Sun model validation
         * 
         * @featureDescription test the Sun model for the SUP attitude law
         * 
         * @coveredRequirements DV-ATT_440
         */
        BASIC_BOARD_SUN_MODEL_VALIDATION
    }

    /** Sun longitude amplitude l1 default value (rad). */
    private static final double LONGITUDE_AMPLITUDE = 1.915 * MathUtils.DEG_TO_RAD;

    /** Equatorial plan / ecliptic plan inclination &epsilon; default value (rad). */
    private static final double EQUAT_ECLIPT_INCLINATION = 23.43 * MathUtils.DEG_TO_RAD;

    /** Default reference date */
    private static final AbsoluteDate DEFAULT_REF_DATE = AbsoluteDate.J2000_EPOCH.shiftedBy(10);

    /** Index for anonymous class. */
    private static int index;

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#BASIC_BOARD_SUN_MODEL_VALIDATION}
     * 
     * @testedMethod {@link BasicBoardSun#BasicBoardSun()}
     * @testedMethod {@link BasicBoardSun#getLine(fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider, AbsoluteDate, Frame)}
     * 
     * @description the basic board Sun model is validated using the Meeus Sun
     *              model
     * 
     * @input the basic board Sun and the Meeus Sun.
     * 
     * @output the Sun direction at a given date for the two models in ITRF
     * 
     * @testPassCriteria the direction should be the same with less than 120 arcseconds opening between the directions
     * 
     * @referenceVersion 2.2
     * 
     * @nonRegressionVersion 2.2
     * 
     * @throws PatriusException
     */
    @Test
    public final void testNewConstructor() throws PatriusException {
        Utils.setDataRoot("regular-dataPBASE");
        FramesFactory.setConfiguration(Utils
            .getIERS2003ConfigurationWOEOP(true));
        final Frame frame = FramesFactory.getITRF();
        final Orbit orbit = new KeplerianOrbit(7e6, .001, 1, 2, 3, 4,
            PositionAngle.ECCENTRIC, FramesFactory.getGCRF(),
            new AbsoluteDate(), Constants.GRIM5C1_EARTH_MU);

        double temp = 0;

        // comparison of MEEUS and CSO
        /** Sun mean longitude default constant &alpha;1 (rad). */
        final double MEAN_LONGITUDE_0 = 280.460 * MathUtils.DEG_TO_RAD;

        /** Sun mean longitude default constant &alpha;2 (rad/s). */
        final double MEAN_LONGITUDE_1 = 0.9856091 * MathUtils.DEG_TO_RAD / Constants.JULIAN_DAY;

        /** Sun mean anomaly default constant &nu;1 (rad). */
        final double MEAN_ANOMALY_0 = 357.528 * MathUtils.DEG_TO_RAD;

        /** Sun mean anomaly default constant &nu;2 (rad/s). */
        final double MEAN_ANOMALY_1 = 0.9856003 * MathUtils.DEG_TO_RAD / Constants.JULIAN_DAY;

        final BasicBoardSun sun = new BasicBoardSun(DEFAULT_REF_DATE, MEAN_LONGITUDE_0, MEAN_LONGITUDE_1,
            MEAN_ANOMALY_0, MEAN_ANOMALY_1, LONGITUDE_AMPLITUDE, EQUAT_ECLIPT_INCLINATION);
        final CelestialBody sunMeeus = CelestialBodyFactory.getSun();

        for (int i = -1000; i < 1000; i++) {
            // test data
            final AbsoluteDate date = AbsoluteDate.J2000_EPOCH
                .shiftedBy(i * 86400.);

            // Actual values
            final Vector3D actualDirection = sun.getVector(null, date, frame);
            final Vector3D actualDirection1 = sun.getLine(orbit, date, frame).getDirection();

            // pvActual = sun.getPVCoordinates(date, frame);
            final PVCoordinates expectedPv = sunMeeus.getPVCoordinates(date,
                frame);
            final Vector3D expectedDirection = expectedPv.getPosition()
                .normalize();

            // distance from one to another
            final double angularDistance = Vector3D.angle(actualDirection,
                expectedDirection) / Constants.ARC_SECONDS_TO_RADIANS;
            final double angularDistance1 = Vector3D.angle(actualDirection1,
                expectedDirection) / Constants.ARC_SECONDS_TO_RADIANS;

            // errors (m and asec)

            if (angularDistance > temp) {
                temp = angularDistance;
            }
            if (angularDistance1 > temp) {
                temp = angularDistance1;
            }
        }

        Assert.assertTrue(temp < 120);

        // Check results with the two constructors
        final BasicBoardSun board1 = new BasicBoardSun();
        final BasicBoardSun board2 = new BasicBoardSun(new AbsoluteDate(DateComponents.J2000_EPOCH,
            TimeComponents.H12, TimeScalesFactory.getTAI()),
            280.460 * MathUtils.DEG_TO_RAD, 0.9856091 * MathUtils.DEG_TO_RAD / 86400.,
            357.528 * MathUtils.DEG_TO_RAD, 0.9856003 * MathUtils.DEG_TO_RAD / 86400.,
            1.915 * MathUtils.DEG_TO_RAD, 23.43 * MathUtils.DEG_TO_RAD);
        final double actual = board1.getVector(null, AbsoluteDate.GALILEO_EPOCH, FramesFactory.getGCRF()).subtract(
            board2.getVector(null, AbsoluteDate.GALILEO_EPOCH, FramesFactory.getGCRF())).getNorm();
        Assert.assertEquals(0., actual, 0.);

    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#BASIC_BOARD_SUN_MODEL_VALIDATION}
     * 
     * @testedMethod {@link BasicBoardSun#BasicBoardSun()}
     * @testedMethod {@link BasicBoardSun#getLine(fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider, AbsoluteDate, Frame)}
     * 
     * @description check the basic board Sun model with respect to Gotlib over 26h
     * 
     * @input the basic board Sun.
     * 
     * @output the Sun direction
     * 
     * @testPassCriteria result is close to reference (GOTLIB, absolute threshold: 1E-14)
     * 
     * @referenceVersion 3.3
     * 
     * @nonRegressionVersion 3.3
     * 
     * @throws PatriusException
     * @throws IOException
     */
    @Test
    public final void testValidation() throws PatriusException, IOException {

        // Initialization
        Report.printMethodHeader("testValidation", "Basic board Sun direction computation", "GOTLIB", 1E-14,
            ComparisonType.ABSOLUTE);

        TimeScalesFactory.clearUTCTAILoaders();
        TimeScalesFactory.addUTCTAILoader(new UTCTAILoader(){

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
                for (int i = 2000; i < 2010; i++) {
                    entries.put(new DateComponents(i, 1, 1), -32);
                }
                return entries;
            }

            @Override
            public String getSupportedNames() {
                return "";
            }
        });

        // Load reference data
        final URL url = RFLinkBudgetTest.class.getClassLoader().getResource("basicboardsun/Ephemerides.txt");
        final BufferedReader reader = new BufferedReader(new FileReader(url.getPath()));
        final List<AbsoluteDate> dates = new ArrayList<AbsoluteDate>();
        final List<PVCoordinates> pvs = new ArrayList<PVCoordinates>();
        final List<Rotation> rots = new ArrayList<Rotation>();
        String line;
        while ((line = reader.readLine()) != null) {

            // Skip header
            if (line.startsWith("//")) {
                continue;
            }

            // Parse line
            final String[] array = line.split(" ");

            // Store date
            final AbsoluteDate date = new AbsoluteDate(array[0], TimeScalesFactory.getTAI());
            dates.add(date);

            // Store pv
            final Vector3D pos = new Vector3D(Double.parseDouble(array[1]), Double.parseDouble(array[2]),
                Double.parseDouble(array[3]));
            final Vector3D vel = new Vector3D(Double.parseDouble(array[4]), Double.parseDouble(array[5]),
                Double.parseDouble(array[6]));
            pvs.add(new PVCoordinates(pos, vel));

            // Store quaternion
            final Rotation rot = new Rotation(true, Double.parseDouble(array[7]), Double.parseDouble(array[8]),
                Double.parseDouble(array[9]), Double.parseDouble(array[10]));
            rots.add(rot);
        }
        reader.close();

        // Initialization
        final AbsoluteDate refDate = new AbsoluteDate(DateComponents.J2000_EPOCH, TimeComponents.H12,
            TimeScalesFactory.getTAI());
        final BasicBoardSun direction = new BasicBoardSun(refDate, MathLib.toRadians(280.46),
            MathLib.toRadians(0.9856474 / 86400.), MathLib.toRadians(357.528),
            MathLib.toRadians(0.9856003 / 86400.),
            MathLib.toRadians(1.915), MathLib.toRadians(23.439));

        // Check
        for (int i = 0; i < dates.size(); i++) {
            index = i;
            final Vector3D actual = direction.getVector(new PVCoordinatesProvider(){
                @Override
                public PVCoordinates
                        getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
                    return pvs.get(index);
                }

                /** {@inheritDoc} */
                @Override
                public Frame getNativeFrame(final AbsoluteDate date,
                        final Frame frame) throws PatriusException {
                    throw new PatriusException(PatriusMessages.INTERNAL_ERROR);
                }
            }, dates.get(i), FramesFactory.getGCRF());
            // Reference results: Sun directed toward -Z
            final Vector3D expected = rots.get(i).applyTo(Vector3D.MINUS_K);

            if (i == 0) {
                Report.printToReport("Direction", expected, actual);
            }

            Assert.assertEquals(0., expected.subtract(actual).getNorm(), 2E-14);
        }
    }

    /**
     * Setup for all unit tests in the class.
     */
    @BeforeClass
    public static void setUpBeforeClass() throws PatriusException, NumberFormatException, IOException {
        Report.printClassHeader(BasicBoardSunTest.class.getSimpleName(), "Basic board Sun");
    }

}
