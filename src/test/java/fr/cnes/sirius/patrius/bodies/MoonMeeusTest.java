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
 * @history creation 01/08/2012
 * 
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:591:05/04/2016:add IAU pole data
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class for the coverage of the class MeeusMoon.
 * 
 * @author Julie Anton
 * 
 * @version $Id: MoonMeeusTest.java 17910 2017-09-11 11:58:16Z bignon $
 * 
 * @since 1.2
 * 
 */
public class MoonMeeusTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Meeus Moon test class for coverage purposes
         * 
         * @featureDescription perform simple tests for coverage, the validation tests are located in pbase-tools.
         * 
         * @coveredRequirements DV-MOD_470
         */
        MEEUS_MOON_COVERAGE
    }

    /** Moon */
    private CelestialBody moon;

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(MoonMeeusTest.class.getSimpleName(), "Moon Meeus model");
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#MEEUS_MOON_COVERAGE}
     * 
     * @description check that IAU data have been initialised and body frame have been defined (through parent method)
     * 
     * @input None
     * 
     * @output transformation
     * 
     * @testPassCriteria returned transformation is not null.
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public final void testTransformation() throws PatriusException {
        Assert.assertNotNull(this.moon.getInertiallyOrientedFrame().getTransformTo(FramesFactory.getEME2000(),
            AbsoluteDate.J2000_EPOCH));
        Assert.assertNotNull(this.moon.getBodyOrientedFrame().getTransformTo(FramesFactory.getEME2000(),
            AbsoluteDate.J2000_EPOCH));
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#MEEUS_MOON_COVERAGE}
     * 
     * @testedMethod {@link MeeusMoon#getPVCoordinates(AbsoluteDate, fr.cnes.sirius.patrius.frames.Frame)}
     * 
     * @description test on one point the results of Meeus algorithm with respect to DE405 ephemerides (JPL)
     * 
     * @input DE405 ephemerides
     * 
     * @output position of the Moon
     * 
     * @testPassCriteria the difference in position should be lower than 26km, the angular difference between both
     *                   results should be lower 15.2''
     * 
     * @comments the thresholds are taken from the following document : Modèles d'éphémérides luni-solaires, DCT/SB/MS,
     *           14/03/2011.
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public final void testGetPVCoordinates() throws PatriusException {

        Report.printMethodHeader("testGetPVCoordinates", "PV coordinates computation", "DE405 ephemeris", 26E3,
            ComparisonType.ABSOLUTE);

        Utils.setDataRoot("regular-dataCNES-2003");
        FramesFactory.setConfiguration(Utils.getIERS2003Configuration(true));
        // start date
        final AbsoluteDate date = new AbsoluteDate(2012, 8, 1, TimeScalesFactory.getTT());
        final Frame gcrf = FramesFactory.getGCRF();

        final JPLEphemeridesLoader loader = new JPLEphemeridesLoader("unxp2000.405",
            JPLEphemeridesLoader.EphemerisType.MOON);

        final JPLEphemeridesLoader loaderEMB = new JPLEphemeridesLoader("unxp2000.405",
            JPLEphemeridesLoader.EphemerisType.EARTH_MOON);

        CelestialBodyFactory.addCelestialBodyLoader(CelestialBodyFactory.EARTH_MOON, loaderEMB);
        CelestialBodyFactory.addCelestialBodyLoader(CelestialBodyFactory.MOON, loader);

        final CelestialBody moonDE = CelestialBodyFactory.getMoon();

        final Vector3D pos1 = this.moon.getPVCoordinates(date, gcrf).getPosition();

        final Vector3D pos2 = moonDE.getPVCoordinates(date, gcrf).getPosition();

        // difference of the distance from Earth to Sun between DE405 and Meeus results
        Assert.assertTrue(Vector3D.distance(pos1, pos2) < 26e3);

        Report.printToReport("Position", pos2, pos1);

        // angular distance between DE405 and Meeus results
        Assert.assertTrue(Vector3D.angle(pos1, pos2) < 15.2 * Constants.ARC_SECONDS_TO_RADIANS);

    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#MEEUS_MOON_COVERAGE}
     * 
     * @testedMethod {@link MeeusMoon#getPVCoordinates(AbsoluteDate, fr.cnes.sirius.patrius.frames.Frame)}
     * 
     * @description test on one point the results of Meeus algorithm (with different level of precision) with respect to
     *              DE405 ephemerides (JPL)
     * 
     * @input DE405 ephemerides
     * 
     * @output position of the Moon
     * 
     * @testPassCriteria the angular difference between DE405 and Meeus 62*66*46 should be lower than 0.005°, the
     *                   angular difference between DE405 and Meeus 26*13*13 should be lower than 0.035°, the angular
     *                   difference between DE405 and Meeus 9*4*3 should be lower than 0.25°.
     * 
     * @comments the thresholds are taken from the following document : Modèles Méeus pour éphémérides Lune-Soleil:
     *           compléments sur le nombre d'opérations, DCT/SB/MS
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public final void testGetPVCoordinates2() throws PatriusException {
        // start date
        final AbsoluteDate date = new AbsoluteDate(2012, 8, 1, TimeScalesFactory.getTT());
        final Frame gcrf = FramesFactory.getGCRF();

        final JPLEphemeridesLoader loader = new JPLEphemeridesLoader("unxp2000.405",
            JPLEphemeridesLoader.EphemerisType.MOON);

        final JPLEphemeridesLoader loaderEMB = new JPLEphemeridesLoader("unxp2000.405",
            JPLEphemeridesLoader.EphemerisType.EARTH_MOON);

        CelestialBodyFactory.addCelestialBodyLoader(CelestialBodyFactory.EARTH_MOON, loaderEMB);
        CelestialBodyFactory.addCelestialBodyLoader(CelestialBodyFactory.MOON, loader);

        final CelestialBody moonDE = CelestialBodyFactory.getMoon();
        // Meeus moon (high precision)
        final CelestialBody moonMeeus0005 = new MeeusMoon(62, 66, 46);
        // Meeus moon (medium precision)
        final CelestialBody moonMeeus0035 = new MeeusMoon(26, 13, 13);
        // Meeus moon (low precision)
        final CelestialBody moonMeeus025 = new MeeusMoon(9, 4, 3);

        // reference
        final Vector3D posREF = moonDE.getPVCoordinates(date, gcrf).getPosition();

        // results
        final Vector3D pos0005 = moonMeeus0005.getPVCoordinates(date, gcrf).getPosition();
        final Vector3D pos0035 = moonMeeus0035.getPVCoordinates(date, gcrf).getPosition();
        final Vector3D pos025 = moonMeeus025.getPVCoordinates(date, gcrf).getPosition();

        // angular distance between DE405 and Meeus results (high precision)
        Assert.assertTrue(Vector3D.angle(posREF, pos0005) < 0.005 * MathUtils.DEG_TO_RAD);
        // angular distance between DE405 and Meeus results (medium precision)
        Assert.assertTrue(Vector3D.angle(posREF, pos0035) < 0.035 * MathUtils.DEG_TO_RAD);
        // angular distance between DE405 and Meeus results (low precision)
        Assert.assertTrue(Vector3D.angle(posREF, pos025) < 0.25 * MathUtils.DEG_TO_RAD);
    }

    /**
     * Set up
     * 
     * @throws PatriusException
     */
    @Before
    public void setUp() throws PatriusException {
        Utils.setDataRoot("regular-dataCNES-2003");
        FramesFactory.setConfiguration(Utils.getIERS2003Configuration(true));
        this.moon = new MeeusMoon();
    }

}
