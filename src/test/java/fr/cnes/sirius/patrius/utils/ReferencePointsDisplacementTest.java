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
 * @history creation 27/07/2012
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-2922:15/11/2021:[PATRIUS] suppression de l'utilisation de la reflexion Java dans patrius 
 * VERSION:4.7:FA:FA-2855:18/05/2021:Erreur calcul colatitude DM 2622 
 * VERSION:4.6:DM:DM-2622:27/01/2021:Modelisation de la maree polaire dans Patrius 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:306:12/11/2014:coverage
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.utils;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.JPLEphemeridesLoader;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.forces.gravity.tides.ReferencePointsDisplacement;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class for the displacement of reference points.
 * 
 * @author Denis Claude
 * 
 * @version $Id: ReferencePointsDisplacementTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.2
 * 
 */
public class ReferencePointsDisplacementTest {

    /** itrf */
    private static Frame itrf;

    /** Smallest positive number such that 1 - EPSILON is not numerically equal to 1. */
    private final double nonRegEps = Precision.DOUBLE_COMPARISON_EPSILON;

    /** Features description. */
    public enum features {
        /**
         * @featureTitle point correction
         * 
         * @featureDescription Point correction test
         * 
         * @coveredRequirements DV-MOD_50, DV-MOD_60, DV-MOD_70, DV-MOD_100, DV-MOD_110
         * 
         */
        POINT_CORRECTION
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#POINT_CORRECTION}
     * 
     * @testedMethod {@link ReferencePointsDisplacement#solidEarthTidesCorrections(fr.cnes.sirius.patrius.time.AbsoluteDate, fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D, fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D, fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D)}
     * 
     * @description test the main method and compare the results to those given by the IERS (2 examples given, see the
     *              following link : ftp://tai.bipm.org/iers/convupdt/chapter7/dehanttideinel/DEHANTTIDEINEL.F). This
     *              test enables the detection of potential regression.
     * 
     * @input date : 13/04/2009</p> moon position : (-179996231.920342, -312468450.131567, -169288918.592160)</p> sun
     *        position : (137859926952.015, 54228127881.4350, 23509422341.6960)</p> station location : (4075578.385,
     *        931852.890, 4801570.154)</p> date : 13/07/2012</p> moon position : (300396716.912, 243238281.451,
     *        120548075.939)</p> sun position : (-54537460436.2357, 130244288385.279, 56463429031.5996)</p> station
     *        location : (1112189.660, -4842955.026, 3985352.284)</p>
     * 
     * @output displacement of point
     * 
     * @testPassCriteria the difference with respect to the reference (from IERS) on each component of the station
     *                   displacement should be lower than a relative epsilon of 1e-14 due to machine errors only. The
     *                   expected results are the following ones :</p> date 13/04/2009</p> station displacement :
     *                   (0.07700420357108125891, 0.06304056321824967613, 0.05516568152597246810)</p> date
     *                   13/07/2012</p> station displacement : (-0.02036831479592075833, 0.05658254776225972449,
     *                   -0.07597679676871742227)</p>
     * 
     * @throws PatriusException
     *         if an error occurs
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public final void pointCorrectionTest() throws PatriusException {

        AbsoluteDate date;
        Vector3D sunPos;
        Vector3D moonPos;
        Vector3D point;

        // test : ftp://tai.bipm.org/iers/convupdt/chapter7/dehanttideinel/DEHANTTIDEINEL.F

        // date : 13/04/2009
        date = new AbsoluteDate(2009, 4, 13, 0, 0, 0., TimeScalesFactory.getUTC());
        // entries : moon position, sun position, station location
        moonPos = new Vector3D(-179996231.920342, -312468450.131567, -169288918.592160);
        sunPos = new Vector3D(137859926952.015, 54228127881.4350, 23509422341.6960);
        point = new Vector3D(4075578.385, 931852.890, 4801570.154);
        final Vector3D dep = ReferencePointsDisplacement.solidEarthTidesCorrections(date, point, sunPos, moonPos);

        // comparison to the reference
        Assert.assertEquals("deviation on dx :", 0.,
            MathLib.abs((0.07700420357108125891 - dep.getX()) / MathLib.max(0.07700420357108125891, dep.getX())),
            this.nonRegEps);
        Assert.assertEquals("deviation on dy :", 0.,
            MathLib.abs((0.06304056321824967613 - dep.getY()) / MathLib.max(0.06304056321824967613, dep.getY())),
            this.nonRegEps);
        Assert.assertEquals("deviation on dz :", 0.,
            MathLib.abs((0.05516568152597246810 - dep.getZ()) / MathLib.max(0.05516568152597246810, dep.getZ())),
            this.nonRegEps);

        // test : ftp://tai.bipm.org/iers/convupdt/chapter7/dehanttideinel/DEHANTTIDEINEL.F

        // date : 13/07/2012
        date = new AbsoluteDate(2012, 7, 13, 0, 0, 0., TimeScalesFactory.getUTC());
        // entries : moon position, sun position, station location
        moonPos = new Vector3D(300396716.912, 243238281.451, 120548075.939);
        sunPos = new Vector3D(-54537460436.2357, 130244288385.279, 56463429031.5996);
        point = new Vector3D(1112189.660, -4842955.026, 3985352.284);
        final Vector3D dep1 = ReferencePointsDisplacement.solidEarthTidesCorrections(date, point, sunPos, moonPos);

        // comparison to the reference
        Assert.assertEquals(
            "deviation on dx :",
            0.,
            MathLib.abs((-0.02036831479592075833 - dep1.getX())
                / MathLib.max(0.02036831479592075833, -dep1.getX())), this.nonRegEps);
        Assert.assertEquals("deviation on dy :", 0., MathLib.abs((0.05658254776225972449 - dep1.getY())
            / MathLib.max(0.05658254776225972449, dep1.getY())), this.nonRegEps);
        Assert.assertEquals(
            "deviation on dz :",
            0.,
            MathLib.abs((-0.07597679676871742227 - dep1.getZ())
                / MathLib.max(0.07597679676871742227, -dep1.getZ())), this.nonRegEps);
    }

    /**
     * @testType UT
     * 
     * @testedMethod {@link ReferencePointsDisplacement#poleTidesCorrections(AbsoluteDate, Vector3D)}
     * 
     * @description check the value of an Earth point displacement due to polar tide
     * 
     * @testPassCriteria the value is as expected (reference: ZOOM, threshold: AD).
     * 
     * @referenceVersion 4.6
     * 
     * @nonRegressionVersion 4.6
     */
    @Test
    public final void testPoleTidesCorrection() throws PatriusException {

        // Computation
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Vector3D point = new Vector3D(6378000, 0, 0);
        final Vector3D actual = ReferencePointsDisplacement.poleTidesCorrections(date, point);

        // Check
        System.out.println(actual);
    }

    /**
     * General set up method.
     * 
     * @throws PatriusException
     *         if an Orekit error occurs
     */
    @BeforeClass
    public static void setUp() throws PatriusException {

        fr.cnes.sirius.patrius.Utils.setDataRoot("regular-dataCNES-2003");
        FramesFactory.setConfiguration(fr.cnes.sirius.patrius.Utils.getIERS2003Configuration(true));
        itrf = FramesFactory.getITRF();
        new OneAxisEllipsoid(Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS, Constants.GRIM5C1_EARTH_FLATTENING,
            itrf);
        // Sun creation
        CelestialBodyFactory.clearCelestialBodyLoaders();
        final String fileName = "unxp2000.405";

        final JPLEphemeridesLoader loaderEMB = new JPLEphemeridesLoader(fileName,
            JPLEphemeridesLoader.EphemerisType.EARTH_MOON);
        final JPLEphemeridesLoader loaderSSB = new JPLEphemeridesLoader(fileName,
            JPLEphemeridesLoader.EphemerisType.SOLAR_SYSTEM_BARYCENTER);

        final JPLEphemeridesLoader loaderSun = new JPLEphemeridesLoader(fileName,
            JPLEphemeridesLoader.EphemerisType.SUN);

        final JPLEphemeridesLoader loaderMoon = new JPLEphemeridesLoader(fileName,
            JPLEphemeridesLoader.EphemerisType.MOON);

        CelestialBodyFactory.addCelestialBodyLoader(CelestialBodyFactory.EARTH_MOON, loaderEMB);
        CelestialBodyFactory.addCelestialBodyLoader(CelestialBodyFactory.SOLAR_SYSTEM_BARYCENTER, loaderSSB);
        CelestialBodyFactory.addCelestialBodyLoader(CelestialBodyFactory.SUN, loaderSun);
        CelestialBodyFactory.addCelestialBodyLoader(CelestialBodyFactory.MOON, loaderMoon);
    }
}
