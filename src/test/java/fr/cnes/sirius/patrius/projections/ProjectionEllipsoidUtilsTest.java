/**
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
 * HISTORY
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:FA:FA-2998:15/11/2021:[PATRIUS] Discontinuite methode computeBearing de la classe ProjectionEllipsoidUtils
 * VERSION:4.5:DM:DM-2473:27/05/2020:methode computeCenterPointAlongLoxodrome dans la classe ProjectionEllipsoidUtils
 * VERSION:4.4:FA:FA-2246:04/10/2019:[PATRIUS] Bug dans le cache de ProjectionEllipsoidUtils
 * VERSION:4.3:DM:DM-2102:15/05/2019:[PATRIUS] Refactoring du paquet fr.cnes.sirius.patrius.bodies
 * VERSION:4.3:DM:DM-2090:15/05/2019:[PATRIUS] ajout de fonctionnalites aux bibliotheques mathematiques
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.projections;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.bodies.EllipsoidBodyShape;
import fr.cnes.sirius.patrius.bodies.EllipsoidPoint;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class for {@link ProjectionEllipsoidUtils}.
 * 
 * @author Thomas Galpin
 * @version $Id$
 * 
 */
public class ProjectionEllipsoidUtilsTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Validate the utility methods of {@link ProjectionEllipsoid} class
         * 
         * @featureDescription Validate the the utility methods of {@link ProjectionEllipsoid} class
         * 
         * @coveredRequirements DV-CARTO_20
         */
        PROJECTION_ELLIPSOID
    }

    /** Projection ellipsoid used in the tests. */
    private static EllipsoidBodyShape ellipsoid;

    /** Point used in the tests (Toulouse). */
    private static EllipsoidPoint point1;

    /** Point used in the tests (somewhere else). */
    private static EllipsoidPoint point2;

    /** Point used in the tests (somewhere else). */
    private static EllipsoidPoint point3;

    /** Point used in the tests (North pole). */
    private static EllipsoidPoint point4;

    /** Point used in the tests (longitude = -Pi). */
    private static EllipsoidPoint point5;

    /** Point used in the tests (longitude = Pi). */
    private static EllipsoidPoint point6;

    /** List of points. */
    private static List<EllipsoidPoint> listPoints;

    /** Other projection ellipsoid used in the tests. */
    private static EllipsoidBodyShape ellipsoidOther;

    /** Point used in the tests (associated to an other ellipsoid). */
    private static EllipsoidPoint pointOther;

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(ProjectionEllipsoidUtilsTest.class.getSimpleName(),
            "Projection ellipsoid");

        // Projections
        ellipsoid = new OneAxisEllipsoid(6378137.0, 1. / 298.257223563, null, "default earth");

        // Point 1 (Toulouse)
        point1 = new EllipsoidPoint(ellipsoid, ellipsoid.getLLHCoordinatesSystem(), 0.758011794744558,
            0.0261405281982074, 256., "");

        // Point 2 (somewhere else)
        point2 = new EllipsoidPoint(ellipsoid, ellipsoid.getLLHCoordinatesSystem(), 0.2, 0.4, 1234., "");

        // Point 3 (somewhere else)
        point3 = new EllipsoidPoint(ellipsoid, ellipsoid.getLLHCoordinatesSystem(), 0.5, 0.2, 345., "");

        // Point 4 (North pole)
        point4 = new EllipsoidPoint(ellipsoid, ellipsoid.getLLHCoordinatesSystem(), FastMath.PI / 2., 0., 0., "");

        // Point 5 (longitude = -Pi)
        point5 = new EllipsoidPoint(ellipsoid, ellipsoid.getLLHCoordinatesSystem(), 0, -MathLib.PI, 0, "");

        // Point 6 (longitude = Pi)
        point6 = new EllipsoidPoint(ellipsoid, ellipsoid.getLLHCoordinatesSystem(), 0.1, MathLib.PI - 0.1, 0, "");

        // Point associated to an other ellipsoid
        ellipsoidOther = new OneAxisEllipsoid(6378137.0, 1. / 298.257223563, null, "other earth");
        pointOther = new EllipsoidPoint(ellipsoidOther, ellipsoidOther.getLLHCoordinatesSystem(), 0., 0., 0., "");

        // List of points and Vector2D
        listPoints = new ArrayList<>();
        listPoints.add(point1);
        listPoints.add(point2);
    }

    /**
     * @throws PatriusException
     *         if an error occurs
     * @testType UT
     * 
     * @testedFeature {@link features#PROJECTION_ELLIPSOID}
     * 
     * @testedMethod {@link ProjectionEllipsoid#computeBearing(EllipsoidPoint, EllipsoidPoint)}
     * 
     * @description test computation of bearing
     * 
     * @input ellipsoid, two geodetic points
     * 
     * @output bearing between the two geodetic points
     * 
     * @testPassCriteria result is identical to reference (reference : LibKernel library 10.0.0, tolerance: 0)
     * 
     * @referenceVersion 4.8
     * 
     * @nonRegressionVersion 4.8
     */
    @Test
    public void testComputeBearing() throws PatriusException {
        final double eps = 0.;
        Report.printMethodHeader("testComputeBearing", "Compute bearing", "LibKernel 10.0.0", eps,
            ComparisonType.RELATIVE);

        final double actual1 = ProjectionEllipsoidUtils.computeBearing(point1, point2);
        final double actual2 = ProjectionEllipsoidUtils.computeBearing(point2, point1);
        final double actual3 = ProjectionEllipsoidUtils.computeBearing(point1, point3);
        final double actual4 = ProjectionEllipsoidUtils.computeBearing(point3, point1);
        final double actual5 = ProjectionEllipsoidUtils.computeBearing(point2, point3);
        final double actual6 = ProjectionEllipsoidUtils.computeBearing(point3, point2);
        final double actual11 = ProjectionEllipsoidUtils.computeBearing(point5, point6);

        final double expeted1 = 2.6119068833311587;
        final double expeted2 = 5.753499536920952;
        final double expeted3 = 2.6432768067025294;
        final double expeted4 = 5.784869460292322;
        final double expeted5 = 5.723192810516977;
        final double expeted6 = 2.581600156927184;
        final double expeted11 = 5.495274586201312;

        Assert.assertEquals(actual1, expeted1, eps);
        Report.printToReport("Bearing between point1 and point2", expeted1, actual1);
        Assert.assertEquals(actual2, expeted2, eps);
        Report.printToReport("Bearing between point2 and point1", expeted2, actual2);
        Assert.assertEquals(actual3, expeted3, eps);
        Report.printToReport("Bearing between point1 and point3", expeted3, actual3);
        Assert.assertEquals(actual4, expeted4, eps);
        Report.printToReport("Bearing between point3 and point1", expeted4, actual4);
        Assert.assertEquals(actual5, expeted5, eps);
        Report.printToReport("Bearing between point2 and point3", expeted5, actual5);
        Assert.assertEquals(actual6, expeted6, eps);
        Report.printToReport("Bearing between point3 and point2", expeted6, actual6);
        Assert.assertEquals(actual11, expeted11, eps);
        Report.printToReport("Bearing between point5 and point6", expeted11, actual11);

        // test with two points along a meridian
        final EllipsoidPoint point5 = new EllipsoidPoint(ellipsoid, ellipsoid.getLLHCoordinatesSystem(), 0.8,
            0.0261405281982074, 256., "");
        final EllipsoidPoint point6 = new EllipsoidPoint(ellipsoid, ellipsoid.getLLHCoordinatesSystem(), 0.7,
            0.0261405281982074, 256., "");
        final double actual7 = ProjectionEllipsoidUtils.computeBearing(point1, point5);
        final double actual8 = ProjectionEllipsoidUtils.computeBearing(point1, point6);
        final double expeted7 = 6.283185307179586; // = 2PI, the value before normalization on [0;2PI] is -5e-17
        final double expeted8 = 3.141592653589793;
        Assert.assertEquals(actual7, expeted7, eps);
        Assert.assertEquals(actual8, expeted8, eps);
        Report.printToReport("Bearing between point1 and point5", expeted7, actual7);
        Report.printToReport("Bearing between point1 and point6", expeted8, actual8);

        // test with two points same latitude
        final EllipsoidPoint point7 = new EllipsoidPoint(ellipsoid, ellipsoid.getLLHCoordinatesSystem(),
            0.758011794744558, 0.02, 256., "");
        final EllipsoidPoint point8 = new EllipsoidPoint(ellipsoid, ellipsoid.getLLHCoordinatesSystem(),
            0.758011794744558, 0.03, 256., "");
        final double actual9 = ProjectionEllipsoidUtils.computeBearing(point1, point7);
        final double actual10 = ProjectionEllipsoidUtils.computeBearing(point1, point8);
        final double expeted9 = 4.71238898038469;
        final double expeted10 = 1.5707963267948966;
        Assert.assertEquals(actual9, expeted9, eps);
        Assert.assertEquals(actual10, expeted10, eps);
        Report.printToReport("Bearing between point1 and point7", expeted9, actual9);
        Report.printToReport("Bearing between point1 and point8", expeted10, actual10);

        // Check exception when points are associated to different body shapes
        try {
            ProjectionEllipsoidUtils.computeBearing(point1, pointOther);
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }

        // Check exception when points are too close
        try {
            ProjectionEllipsoidUtils.computeBearing(point1, point1);
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }
    }

    /**
     * @throws PatriusException
     *         if an error occurs
     * @testType UT
     * 
     * @testedFeature {@link features#PROJECTION_ELLIPSOID}
     * 
     * @testedMethod {@link ProjectionEllipsoid#computeSphericalAzimuth(EllipsoidPoint, EllipsoidPoint)}
     * 
     * @description test computation of spherical azimuth between two geodetic points
     * 
     * @input ellipsoid, two geodetic points
     * 
     * @output computation of spherical azimuth between the two geodetic points
     * 
     * @testPassCriteria result is identical to reference (reference : LibKernel library 10.0.0, tolerance: 0)
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testComputeSphericalAzimuth() throws PatriusException {
        final double eps = 0.;
        Report.printMethodHeader("testComputeSphericalAzimuth", "Compute spherical azimuth",
            "LibKernel 10.0.0", eps, ComparisonType.RELATIVE);

        final double actual1 = ProjectionEllipsoidUtils.computeSphericalAzimuth(point1, point2);
        final double actual2 = ProjectionEllipsoidUtils.computeSphericalAzimuth(point2, point1);
        final double actual3 = ProjectionEllipsoidUtils.computeSphericalAzimuth(point1, point3);
        final double actual4 = ProjectionEllipsoidUtils.computeSphericalAzimuth(point3, point1);
        final double actual5 = ProjectionEllipsoidUtils.computeSphericalAzimuth(point2, point3);
        final double actual6 = ProjectionEllipsoidUtils.computeSphericalAzimuth(point3, point2);

        final double expeted1 = 2.5037987586494235;
        final double expeted2 = 5.826256202343607;
        final double expeted3 = 2.588803690128547;
        final double expeted4 = 5.833710733093144;
        final double expeted5 = 5.75527394858528;
        final double expeted6 = 2.544118861542366;

        Assert.assertEquals(actual1, expeted1, eps);
        Report.printToReport("Spherical azimuth", expeted1, actual1);
        Assert.assertEquals(actual2, expeted2, eps);
        Assert.assertEquals(actual3, expeted3, eps);
        Assert.assertEquals(actual4, expeted4, eps);
        Assert.assertEquals(actual5, expeted5, eps);
        Assert.assertEquals(actual6, expeted6, eps);

        // Check exception when points are associated to different body shapes
        try {
            ProjectionEllipsoidUtils.computeSphericalAzimuth(point1, pointOther);
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#PROJECTION_ELLIPSOID}
     * 
     * @testedMethod {@link ProjectionEllipsoid#computeMercatorLatitude(double)}
     * 
     * @description test computation Mercator Latitude
     * 
     * @input ellipsoid, geodetic latitude
     * 
     * @output Mercator latitude
     * 
     * @testPassCriteria result is identical to reference (reference : LibKernel library 10.0.0, tolerance: 0)
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testComputeMercatorLatitude() {
        Report.printMethodHeader("testComputeMercatorLatitude", "Compute Mercator latitude",
            "LibKernel 10.0.0", 0, ComparisonType.RELATIVE);

        final double[] refMercatorLatitude = {
            -1.1102230246251565E-16
            , 0.10421074576230559
            , 0.2095933545574732
            , 0.3173854932677744
            , 0.42896580509513527
            , 0.5459504012025171
            , 0.670328990535307
            , 0.804672411915078
            , 0.952471956903105
            , 1.118735643260533
            , 1.3111274301053137
            , 1.5423886733563723
            , 1.8363052496450072
            , 2.2461678493890607
            , 2.9418719621825664
            , 11.642420045994898
            , 11.642420046045785
            , 11.642420046045785 };

        final double step = Mercator.MAX_LATITUDE * MathUtils.RAD_TO_DEG / 15;
        int countRef = 0;
        // loop for input latitude 0 <lat < Mercator.MAX_LATITUDE
        for (double i = 0; i < Mercator.MAX_LATITUDE * MathUtils.RAD_TO_DEG; i += step) {
            // assert + ptint report
            Assert.assertEquals(refMercatorLatitude[countRef],
                ProjectionEllipsoidUtils.computeMercatorLatitude(i * MathUtils.DEG_TO_RAD,
                    ellipsoid), 0.);
            if (i == step) {
                Report.printToReport("Mercator lat (" + MathLib.rint(i) + " deg)",
                    refMercatorLatitude[countRef],
                    ProjectionEllipsoidUtils.computeMercatorLatitude(i * MathUtils.DEG_TO_RAD,
                        ellipsoid));
            }
            countRef++;
        }

        // input latitude = Mercator.MAX_LATITUDE
        Assert.assertEquals(refMercatorLatitude[16],
            ProjectionEllipsoidUtils.computeMercatorLatitude(Mercator.MAX_LATITUDE, ellipsoid), 0.);
        Report.printToReport("Mercator lat (" + Mercator.MAX_LATITUDE * MathUtils.RAD_TO_DEG
                + " deg)", refMercatorLatitude[16],
            ProjectionEllipsoidUtils.computeMercatorLatitude(Mercator.MAX_LATITUDE, ellipsoid));

        // input latitude = Mercator.MAX_LATITUDE + PI/360
        Assert.assertEquals(refMercatorLatitude[17],
            ProjectionEllipsoidUtils.computeMercatorLatitude(
                Mercator.MAX_LATITUDE + FastMath.PI / 360, ellipsoid), 0.);
        Report.printToReport("Mercator lat (" + (Mercator.MAX_LATITUDE + FastMath.PI / 360)
                * MathUtils.RAD_TO_DEG + " deg)", refMercatorLatitude[17], ProjectionEllipsoidUtils
            .computeMercatorLatitude(Mercator.MAX_LATITUDE + FastMath.PI / 360, ellipsoid));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#PROJECTION_ELLIPSOID}
     * 
     * @testedMethod {@link ProjectionEllipsoid#computeRadiusEastWest(double)}
     * 
     * @description test computation of radius east/west
     * 
     * @input ellipsoid, geodetic latitude
     * 
     * @output radius east/west
     * 
     * @testPassCriteria result is identical to reference (reference : LibKernel library 10.0.0, tolerance: 0)
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testComputeRadiusEastWest() {
        Report.printMethodHeader("testComputeRadiusEastWest", "Compute radius east/west",
            "LibKernel 10.0.0", 0, ComparisonType.RELATIVE);

        final double[] refRadius = {
            6399593.625758493
            , 6399065.957420465
            , 6397535.342997051
            , 6395153.541201456
            , 6392156.08040758
            , 6388838.290121148
            , 6385525.660683886
            , 6382541.71120694
            , 6380176.6103943195
            , 6378659.507412074
            , 6378137.0
            , 6378659.507412074
            , 6380176.6103943195
            , 6382541.71120694
            , 6385525.660683886
            , 6388838.290121148
            , 6392156.08040758
            , 6395153.541201456
            , 6397535.342997051
            , 6399065.957420465
            , 6399593.625758493 };

        final double step = FastMath.PI / 20;
        int countRef = 0;
        // loop for input latitude - PI/2 <lat < PI/2 (20 values)
        for (double i = -FastMath.PI / 2; i < FastMath.PI / 2; i += step) {
            // assert + ptint report
            Assert.assertEquals(refRadius[countRef],
                ProjectionEllipsoidUtils.computeRadiusEastWest(i, ellipsoid), 0.);
            if (i == step) {
                Report.printToReport("Radius E/W", refRadius[countRef],
                    ProjectionEllipsoidUtils.computeRadiusEastWest(i, ellipsoid));
            }
            countRef++;
        }
    }

    /**
     * @throws PatriusException
     *         if an error occurs
     * @testType UT
     * 
     * @testedFeature {@link features#PROJECTION_ELLIPSOID}
     * 
     * @testedMethod {@link ProjectionEllipsoid#computeLoxodromicDistance(EllipsoidPoint, EllipsoidPoint)}
     * 
     * @description test computation loxodromic distance between two points
     * 
     * @input two geodetic points
     * 
     * @output a loxodromic distance
     * 
     * @testPassCriteria loxodromic distance is identical to reference (reference : LibKernel library 10.0.0, tolerance:
     *                   5E-16)
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testComputeLoxodromicDistance() throws PatriusException {
        final double eps = 5E-16;
        Report.printMethodHeader("testComputeLoxodromicDistance", "Compute loxodromic distance",
            "LibKernel 10.0.0", eps, ComparisonType.RELATIVE);

        // test data
        final EllipsoidPoint point5 = new EllipsoidPoint(ellipsoid, ellipsoid.getLLHCoordinatesSystem(), 0.8,
            0.0261405281982074, 256., "");
        final EllipsoidPoint point6 = new EllipsoidPoint(ellipsoid, ellipsoid.getLLHCoordinatesSystem(), 0.7,
            0.0261405281982074, 256., "");
        final EllipsoidPoint point7 = new EllipsoidPoint(ellipsoid, ellipsoid.getLLHCoordinatesSystem(),
            0.758011794744558, 0.02, 256., "");
        final EllipsoidPoint point8 = new EllipsoidPoint(ellipsoid, ellipsoid.getLLHCoordinatesSystem(),
            0.758011794744558, 0.03, 256., "");
        final EllipsoidPoint badPoint = new EllipsoidPoint(ellipsoid, ellipsoid.getLLHCoordinatesSystem(),
            MathUtils.DEG_TO_RAD * 90, 0.03, 256., "");

        // actual
        final double[] lox = {
            ProjectionEllipsoidUtils.computeLoxodromicDistance(point1, point2),
            ProjectionEllipsoidUtils.computeLoxodromicDistance(point2, point1),
            ProjectionEllipsoidUtils.computeLoxodromicDistance(point1, point3),
            ProjectionEllipsoidUtils.computeLoxodromicDistance(point3, point1),
            ProjectionEllipsoidUtils.computeLoxodromicDistance(point2, point3),
            ProjectionEllipsoidUtils.computeLoxodromicDistance(point3, point2),
            ProjectionEllipsoidUtils.computeLoxodromicDistance(point5, point6),
            ProjectionEllipsoidUtils.computeLoxodromicDistance(point7, point8) };

        // ref
        final double[] loxRef = { 4105994.0401493986
            , 4105994.0401493986
            , 1867447.6851144924
            , 1867447.6851144924
            , 2246051.708688264
            , 2246051.7086882642
            , 636511.770303472
            , 46391.75346512364 };

        // assert + ptint report
        for (int i = 0; i < lox.length; i++) {
            Assert.assertEquals(0., (loxRef[i] - lox[i]) / loxRef[i], eps);
            Report.printToReport("Loxodromic distance " + i, loxRef[i], lox[i]);
        }

        // Check exception when one of the latitude point is over 90°
        try {
            ProjectionEllipsoidUtils.computeLoxodromicDistance(point1, badPoint);
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }

        // Check exception when points are associated to different body shapes
        try {
            ProjectionEllipsoidUtils.computeLoxodromicDistance(point1, pointOther);
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#PROJECTION_ELLIPSOID}
     * 
     * @testedMethod {@link ProjectionEllipsoid#computeMeridionalDistance(double)}
     * 
     * @description test computation meridional distance
     * 
     * @input ellipsoid, latitude
     * 
     * @output meridional distance
     * 
     * @testPassCriteria result is identical to reference (reference : LibKernel library 10.0.0, tolerance: 0)
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testComputeMeridionalDistance() {
        Report.printMethodHeader("testComputeMeridionalDistance", "Compute meridional distance",
            "LibKernel 10.0.0", 0, ComparisonType.RELATIVE);

        // ref
        final double[] refMeridionalDist = {
            -1.0001965729528552E7
            , -8996803.073579784
            , -7992129.355847894
            , -6988384.569753045
            , -5985916.028950047
            , -4984944.378231887
            , -3985542.6704830388
            , -2987630.294689997
            , -1990981.9348791367
            , -995250.277395647
            , 0.0
            , 995250.277395647
            , 1990981.9348791367
            , 2987630.294689997
            , 3985542.6704830388
            , 4984944.378231887
            , 5985916.028950047
            , 6988384.569753045
            , 7992129.355847894
            , 8996803.073579784
            , 1.0001965729528552E7 };

        final double step = FastMath.PI / 20;
        int countRef = 0;
        // loop for input latitude - PI/2 <lat < PI/2 (20 values)
        for (double i = -FastMath.PI / 2; i < FastMath.PI / 2; i += step) {
            // assert + ptint report
            Assert.assertEquals(refMeridionalDist[countRef],
                ProjectionEllipsoidUtils.computeMeridionalDistance(i, ellipsoid), 0.);
            if (i == step) {
                Report.printToReport("Meridional distance", refMeridionalDist[countRef],
                    ProjectionEllipsoidUtils.computeMeridionalDistance(i, ellipsoid));
            }
            countRef++;
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#PROJECTION_ELLIPSOID}
     * 
     * @testedMethod {@link ProjectionEllipsoid#computeInverseMeridionalDistance(double)}
     * 
     * @description test computation inverse meridional distance at a latitude
     * 
     * @input ellipsoid, latitude
     * 
     * @output inverse meridional distance
     * 
     * @testPassCriteria result is identical to reference (reference : LibKernel library 10.0.0, tolerance: 0)
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testComputeInverseMeridionalDistance() {
        Report.printMethodHeader("testComputeInverseMeridionalDistance", "Compute inverse meridional distance",
            "LibKernel 10.0.0", 0, ComparisonType.RELATIVE);

        // ref
        final double[] refInverseMeridionalDist = {
            -0.1578291544856476
            , -0.14204846915147412
            , -0.1262670828409429
            , -0.11048507162655401
            , -0.09470251221843526
            , -0.07891948188621338
            , -0.06313605838025765
            , -0.04735231985238414
            , -0.03156834477610906
            , -0.015784211866540734
            , 0.0
            , 0.015784211866540734
            , 0.03156834477610906
            , 0.04735231985238414
            , 0.06313605838025765
            , 0.07891948188621338
            , 0.09470251221843526
            , 0.11048507162655401
            , 0.1262670828409429
            , 0.14204846915147412
            , 0.1578291544856476
        };

        final double step = 2e6 / 20;
        int countRef = 0;
        // loop for input distance - 1e6 <lat < 1e6 (20 values)
        for (double i = -1e6; i < 1e6; i += step) {
            // assert + ptint report
            Assert.assertEquals(refInverseMeridionalDist[countRef],
                ProjectionEllipsoidUtils.computeInverseMeridionalDistance(i, ellipsoid), 0.);
            if (i == step) {
                Report.printToReport("Inverse meridional distance",
                    refInverseMeridionalDist[countRef],
                    ProjectionEllipsoidUtils.computeInverseMeridionalDistance(i, ellipsoid));
            }
            countRef++;
        }
    }

    /**
     * @throws PatriusException
     *         if an error occurs
     * @testType UT
     * 
     * @testedFeature {@link features#PROJECTION_ELLIPSOID}
     * 
     * @testedMethod {@link ProjectionEllipsoid#computePointAlongLoxodrome(EllipsoidPoint, double, double)}
     * 
     * @description test compute point along a loxodrome
     * 
     * @input ellipsoid, initial point, distance along the rhumb line, azimuth
     * 
     * @output geodetic point
     * 
     * @testPassCriteria result is identical to reference (reference : LibKernel library 10.0.0, tolerance: 1E-15)
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testComputePointAlongLoxodrome() throws PatriusException {
        final double eps = 1E-15;
        Report.printMethodHeader("testComputePointAlongLoxodrome", "Compute point along loxodrome",
            "LibKernel 10.0.0", eps, ComparisonType.RELATIVE);

        // ref
        final List<EllipsoidPoint> refPoints = new ArrayList<>();
        refPoints
            .add(new EllipsoidPoint(ellipsoid, ellipsoid.getLLHCoordinatesSystem(), 0.7892352562618954, 0.0, 0, ""));
        refPoints.add(new EllipsoidPoint(ellipsoid, ellipsoid.getLLHCoordinatesSystem(), 0.5585544800165291,
            0.5865408288158058, 0, ""));
        refPoints
            .add(new EllipsoidPoint(ellipsoid, ellipsoid.getLLHCoordinatesSystem(), 0.0, 0.7853981633974484, 0, ""));
        refPoints.add(new EllipsoidPoint(ellipsoid, ellipsoid.getLLHCoordinatesSystem(), -0.558554480016529,
            0.5865408288158056, 0, ""));
        refPoints.add(new EllipsoidPoint(ellipsoid, ellipsoid.getLLHCoordinatesSystem(), -0.7892352562618954, 0.0, 0,
            ""));
        refPoints.add(new EllipsoidPoint(ellipsoid, ellipsoid.getLLHCoordinatesSystem(), -0.558554480016529,
            -0.5865408288158056, 0, ""));
        refPoints.add(new EllipsoidPoint(ellipsoid, ellipsoid.getLLHCoordinatesSystem(), 0.0, -0.7853981633974484, 0,
            ""));
        refPoints.add(new EllipsoidPoint(ellipsoid, ellipsoid.getLLHCoordinatesSystem(), 0.5585544800165291,
            -0.5865408288158058, 0, ""));
        refPoints
            .add(new EllipsoidPoint(ellipsoid, ellipsoid.getLLHCoordinatesSystem(), 0.7892352562618954, 0.0, 0, ""));

        // ref
        final List<EllipsoidPoint> actualPoints = new ArrayList<>();
        final EllipsoidPoint p0 = new EllipsoidPoint(ellipsoid, ellipsoid.getLLHCoordinatesSystem(), 0, 0, 0, "");
        final double d1 = 0.5 * ellipsoid.getEquatorialRadius() * FastMath.PI / 2;
        // loop over 10 points :
        for (int i = 0; i <= 360; i += 360 / 8) {
            final double azimuth = MathUtils.DEG_TO_RAD * i;
            actualPoints.add(ProjectionEllipsoidUtils.computePointAlongLoxodrome(p0, d1, azimuth));
        }
        // check
        checkListGeodeticPoint(refPoints, actualPoints, eps);

        // exceptions :
        try {
            ProjectionEllipsoidUtils.computePointAlongLoxodrome(new EllipsoidPoint(ellipsoid,
                ellipsoid.getLLHCoordinatesSystem(), MathUtils.DEG_TO_RAD * 90, 0.03, 256., ""), d1,
                0.1);
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }
        try {
            ProjectionEllipsoidUtils.computePointAlongLoxodrome(new EllipsoidPoint(ellipsoid,
                ellipsoid.getLLHCoordinatesSystem(), MathUtils.DEG_TO_RAD * 89, 0.03, 256., ""), d1,
                0.1);
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#PROJECTION_ELLIPSOID}
     * 
     * @testedMethod {@link ProjectionEllipsoid#computeInverseRectifyingLatitude(double)}
     * 
     * @description test computation inverse rectifying latitude
     * 
     * @input ellipsoid, latitude
     * 
     * @output inverse rectifying latitude
     * 
     * @testPassCriteria result is identical to reference (reference : LibKernel library 10.0.0, tolerance: 0)
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testComputeInverseRectifyingLatitude() {
        Report.printMethodHeader("testComputeInverseRectifyingLatitude", "Compute inverse rectifying latitude",
            "LibKernel 10.0.0", 0, ComparisonType.RELATIVE);

        // ref
        final double[] refInverseRectifyingLat = {
            -3.141592653589793
            , -2.7208380951196665
            , -2.3013303609906366
            , -1.8834785757729071
            , -1.466598764222742
            , -1.0493757139052782
            , -0.6307162481238062
            , -0.21046676675010437
            , 0.2104667667501043
            , 0.6307162481238062
            , 1.0493757139052782
            , 1.466598764222742
            , 1.8834785757729071
            , 2.3013303609906366
            , 2.7208380951196665
            , 3.141592653589793
        };

        final double step = 2 * FastMath.PI / 15;
        int countRef = 0;
        // loop for input distance - 1e6 <lat < 1e6 (20 values)
        for (double i = -FastMath.PI; i < FastMath.PI; i += step) {
            // assert + ptint report
            Assert.assertEquals(refInverseRectifyingLat[countRef],
                ProjectionEllipsoidUtils.computeInverseRectifyingLatitude(i, ellipsoid), 0.);
            if (i == step) {
                Report.printToReport("Inverse rectifying latitude",
                    refInverseRectifyingLat[countRef],
                    ProjectionEllipsoidUtils.computeInverseRectifyingLatitude(i, ellipsoid));
            }
            countRef++;
        }
    }

    /**
     * @throws PatriusException
     *         if an error occurs
     * @testType UT
     * 
     * @testedFeature {@link features#PROJECTION_ELLIPSOID}
     * 
     * @testedMethod {@link ProjectionEllipsoid#computeOrthodromicDistance(EllipsoidPoint, EllipsoidPoint)}
     * @testedMethod {@link ProjectionEllipsoid#computeOrthodromicDistance(double, double, double, double)}
     * 
     * @description test the computation of orthodromic distance between two geodetic points
     * 
     * @input ellipsoid
     * 
     * @output orthodromic distance
     * 
     * @testPassCriteria result is identical to reference (reference : LibKernel library 10.0.0, tolerance: 3E-14)
     *                   specials cases are considered : the two points have same latitude or same longitude, same
     *                   latitude and same longitude
     * 
     * @referenceVersion 3.3
     * 
     * @nonRegressionVersion 3.3
     */
    @Test
    public void testComputeOrthodromicDistance() throws PatriusException {
        final double eps = 3E-14;
        Report.printMethodHeader("testComputeOrthodromicDistance", "Compute orthodromic distance",
            "LibKernel 10.0.0", eps, ComparisonType.RELATIVE);

        // Toulouse
        final double actual1 = ProjectionEllipsoidUtils.computeOrthodromicDistance(point1, point2);
        final double actual2 = ProjectionEllipsoidUtils.computeOrthodromicDistance(point1, point2);

        final double expected = 4100536.2596218484;

        Assert.assertEquals(0., (expected - actual1) / expected, eps);
        Assert.assertEquals(0., (expected - actual2) / expected, eps);
        Report.printToReport("Orthodromic distance", expected, actual1);

        // North pole
        final double actual4 = ProjectionEllipsoidUtils.computeOrthodromicDistance(point1, point4);
        final double expected4 = 5191376.768899236;
        Assert.assertEquals(0., (expected4 - actual4) / expected4, eps);
        Report.printToReport("Orthodromic distance (North pole)", expected4, actual4);

        // Special case (delta-longitude > Pi)
        final EllipsoidPoint point5 = new EllipsoidPoint(ellipsoid, ellipsoid.getLLHCoordinatesSystem(), 0.2, -3.14,
            1234., "");
        final double actual5 = ProjectionEllipsoidUtils.computeOrthodromicDistance(point1, point5);
        final double expected5 = 1.3924419603948575E7;
        Assert.assertEquals(0., (expected5 - actual5) / expected5, eps);
        Report.printToReport("Orthodromic distance (dlon > PI)", expected5, actual5);

        // Special case : input points having same latitude in one hand OR same longitude on the other :
        // computed distance must not be 0
        final EllipsoidPoint point6 = new EllipsoidPoint(ellipsoid, ellipsoid.getLLHCoordinatesSystem(), 0.2, 0.5,
            1234., "");
        final EllipsoidPoint point7 = new EllipsoidPoint(ellipsoid, ellipsoid.getLLHCoordinatesSystem(), 0.1, 0.4,
            1234., "");
        final double actual6 = ProjectionEllipsoidUtils.computeOrthodromicDistance(point2, point6);
        final double actual7 = ProjectionEllipsoidUtils.computeOrthodromicDistance(point2, point7);
        Assert.assertFalse(actual6 == 0.);
        Assert.assertFalse(actual7 == 0.);

        // Finally, null distance is expected if points have same latitude AND same longitude
        final EllipsoidPoint point8 = new EllipsoidPoint(ellipsoid, ellipsoid.getLLHCoordinatesSystem(), 0.2, 0.4,
            1234., "");
        final double actual8 = ProjectionEllipsoidUtils.computeOrthodromicDistance(point2, point8);
        Assert.assertEquals(actual8, 0., 0.);

        // Check exception when points are associated to different body shapes
        try {
            ProjectionEllipsoidUtils.computeOrthodromicDistance(point1, pointOther);
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#PROJECTION_ELLIPSOID}
     * 
     * @testedMethod {@link ProjectionEllipsoid#computePointAlongOrthodrome(EllipsoidPoint, double, double)}
     * 
     * @description test the computation of point along an orthodrome
     * 
     * @input ellipsoid
     * 
     * @output geodetic coordinates
     * 
     * @testPassCriteria result is identical to reference (reference : LibKernel library 10.0.0, tolerance: 1E-14)
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testComputePointAlongOrthodrome() {
        final double eps = 1E-14;
        Report.printMethodHeader("testComputePointAlongOrthodrome", "Compute point along orthodrome",
            "LibKernel 10.0.0", eps, ComparisonType.RELATIVE);

        final EllipsoidPoint actual = ProjectionEllipsoidUtils.computePointAlongOrthodrome(point1, 12345., 0.5);

        final EllipsoidPoint expected = new EllipsoidPoint(ellipsoid, ellipsoid.getLLHCoordinatesSystem(),
            MathLib.toRadians(43.528364978062925), MathLib.toRadians(1.5709556155153825), 0., "");

        Assert.assertEquals(0., (expected.getLLHCoordinates().getLatitude() - actual.getLLHCoordinates().getLatitude())
                / expected.getLLHCoordinates().getLatitude(), eps);
        Assert.assertEquals(0., (expected.getLLHCoordinates().getLongitude() - actual.getLLHCoordinates()
            .getLongitude()) / expected.getLLHCoordinates().getLongitude(), eps);
        Assert.assertEquals(expected.getLLHCoordinates().getHeight(), actual.getLLHCoordinates().getHeight(), eps);
        Report.printToReport("Latitude", expected.getLLHCoordinates().getLatitude(), actual.getLLHCoordinates()
            .getLatitude());
        Report.printToReport("Longitude", expected.getLLHCoordinates().getLongitude(), actual.getLLHCoordinates()
            .getLongitude());
        Report.printToReport("Altitude", expected.getLLHCoordinates().getHeight(), actual.getLLHCoordinates()
            .getHeight());
    }

    /**
     * @throws PatriusException
     *         if an error occurs
     * @testType UT
     * 
     * @testedFeature {@link features#PROJECTION_ELLIPSOID}
     * 
     * @testedMethod {@link ProjectionEllipsoid#discretizeGreatCircle(EllipsoidPoint, EllipsoidPoint, double)}
     * 
     * @description test the discretization along a Great Circle
     * 
     * @input ellipsoid
     * 
     * @output geodetic coordinates
     * 
     * @testPassCriteria result is identical to reference (reference : LibKernel library 10.0.0, tolerance: 3E-13)
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testDiscretizationGreatCircle() throws PatriusException {
        final double eps = 3E-13;
        Report.printMethodHeader("testDiscretizationGreatCircle", "Discretization along Great Circle",
            "LibKernel 10.0.0", eps, ComparisonType.RELATIVE);

        // 3 maxlength cases
        final List<EllipsoidPoint> actual1 = ProjectionEllipsoidUtils.discretizeGreatCircle(point1, point2, 1000000);
        final List<EllipsoidPoint> actual2 = ProjectionEllipsoidUtils.discretizeGreatCircle(point1, point2, 0);
        final List<EllipsoidPoint> actual3 = ProjectionEllipsoidUtils.discretizeGreatCircle(point1, point2, 4000000);

        final List<EllipsoidPoint> expected1 = new ArrayList<>();
        expected1.add(new EllipsoidPoint(ellipsoid, ellipsoid.getLLHCoordinatesSystem(), MathLib
            .toRadians(43.43087666000001), MathLib.toRadians(1.4977419400000025), 256., ""));
        expected1.add(new EllipsoidPoint(ellipsoid, ellipsoid.getLLHCoordinatesSystem(), MathLib
            .toRadians(35.80077182694041), MathLib.toRadians(8.237088262669358), 0., ""));
        expected1.add(new EllipsoidPoint(ellipsoid, ellipsoid.getLLHCoordinatesSystem(), MathLib
            .toRadians(27.852456068881388), MathLib.toRadians(13.777774346708954), 0., ""));
        expected1.add(new EllipsoidPoint(ellipsoid, ellipsoid.getLLHCoordinatesSystem(), MathLib
            .toRadians(19.70725940634673), MathLib.toRadians(18.56131861964875), 0., ""));

        final List<EllipsoidPoint> expected2 = new ArrayList<>();
        expected2.add(new EllipsoidPoint(ellipsoid, ellipsoid.getLLHCoordinatesSystem(), MathLib
            .toRadians(43.43087666000001), MathLib.toRadians(1.4977419400000025), 256., ""));
        expected2.add(new EllipsoidPoint(ellipsoid, ellipsoid.getLLHCoordinatesSystem(), MathLib
            .toRadians(11.459155902616466), MathLib.toRadians(22.918311805232932), 1234., ""));

        final List<EllipsoidPoint> expected3 = new ArrayList<>();
        expected3.add(new EllipsoidPoint(ellipsoid, ellipsoid.getLLHCoordinatesSystem(), MathLib
            .toRadians(43.43087666000001), MathLib.toRadians(1.4977419400000025), 256., ""));
        expected3.add(new EllipsoidPoint(ellipsoid, ellipsoid.getLLHCoordinatesSystem(), MathLib
            .toRadians(11.459155902616466), MathLib.toRadians(22.918311805232932), 1234., ""));

        checkListGeodeticPoint(expected1, actual1, eps);
        checkListGeodeticPoint(expected2, actual2, eps);
        checkListGeodeticPoint(expected3, actual3, eps);
    }

    /**
     * @throws PatriusException
     *         if an error occurs
     * @testType UT
     * 
     * @testedFeature {@link features#PROJECTION_ELLIPSOID}
     * 
     * @testedMethod {@link ProjectionEllipsoid#discretizeRhumbLine(EllipsoidPoint, EllipsoidPoint, double)}
     * 
     * @description test the discretization along a Rhumb line
     * 
     * @input ellipsoid
     * 
     * @output geodetic coordinates
     * 
     * @testPassCriteria result is identical to reference (reference : LibKernel library 10.0.0, tolerance: 3E-13)
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testDiscretizationRhumbLine() throws PatriusException {
        final double eps = 3E-13;
        Report.printMethodHeader("testDiscretizationRhumbLine", "Discretization along Rhumb line",
            "LibKernel 10.0.0", eps, ComparisonType.RELATIVE);

        // 3 maxlength cases
        final List<EllipsoidPoint> actual1 = ProjectionEllipsoidUtils.discretizeRhumbLine(point1, point2, 1000000);
        final List<EllipsoidPoint> actual2 = ProjectionEllipsoidUtils.discretizeRhumbLine(point1, point2, 0);
        final List<EllipsoidPoint> actual3 = ProjectionEllipsoidUtils.discretizeRhumbLine(point1, point2, 4000000);

        final List<EllipsoidPoint> expected1 = new ArrayList<>();
        expected1.add(new EllipsoidPoint(ellipsoid, ellipsoid.getLLHCoordinatesSystem(), MathLib
            .toRadians(43.43087666000001), MathLib.toRadians(1.4977419400000025), 256., ""));
        expected1.add(new EllipsoidPoint(ellipsoid, ellipsoid.getLLHCoordinatesSystem(), MathLib
            .toRadians(35.45214288961152), MathLib.toRadians(7.534123017198017), 0., ""));
        expected1.add(new EllipsoidPoint(ellipsoid, ellipsoid.getLLHCoordinatesSystem(), MathLib
            .toRadians(27.46289676019212), MathLib.toRadians(12.998775866019459), 0., ""));
        expected1.add(new EllipsoidPoint(ellipsoid, ellipsoid.getLLHCoordinatesSystem(), MathLib
            .toRadians(19.464531632933117), MathLib.toRadians(18.080863380100528), 0., ""));

        final List<EllipsoidPoint> expected2 = new ArrayList<>();
        expected2.add(new EllipsoidPoint(ellipsoid, ellipsoid.getLLHCoordinatesSystem(), MathLib
            .toRadians(43.43087666000001), MathLib.toRadians(1.4977419400000025), 256., ""));

        final List<EllipsoidPoint> expected3 = new ArrayList<>();
        expected3.add(new EllipsoidPoint(ellipsoid, ellipsoid.getLLHCoordinatesSystem(), MathLib
            .toRadians(43.43087666000001), MathLib.toRadians(1.4977419400000025), 256., ""));
        expected3.add(new EllipsoidPoint(ellipsoid, ellipsoid.getLLHCoordinatesSystem(), MathLib
            .toRadians(11.459155902616466), MathLib.toRadians(22.918311805232932), 1234., ""));

        checkListGeodeticPoint(expected1, actual1, eps);
        checkListGeodeticPoint(expected2, actual2, eps);
        checkListGeodeticPoint(expected3, actual3, eps);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#PROJECTION_ELLIPSOID}
     * 
     * @testedMethod {@link ProjectionEllipsoid#getEccentricity()}
     * 
     * @description Check the ellipsoid eccentricity computation
     * 
     * @input ellipsoid
     * 
     * @output ellipsoid eccentricity
     * 
     * @testPassCriteria result is identical to reference (reference : LibKernel library 10.0.0, tolerance: 0)
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testEccentricity() {
        Report.printMethodHeader("testEccentricity", "Ellipsoid eccentricity value",
            "LibKernel 10.0.0", 0, ComparisonType.RELATIVE);

        Assert.assertEquals(0.08181919084262149,
            ProjectionEllipsoidUtils.getEccentricity(ellipsoid), 0.);
        Report.printToReport("Eccentricity", 0.08181919084262149,
            ProjectionEllipsoidUtils.getEccentricity(ellipsoid));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#PROJECTION_ELLIPSOID}
     * 
     * @testedMethod {@link ProjectionEllipsoid#getSeries()}
     * 
     * @description Check the ellipsoid series computation
     * 
     * @input ellipsoid
     * 
     * @output series
     * 
     * @testPassCriteria result is identical to reference (reference : LibKernel library 10.0.0, tolerance: 2E-16)
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testSeries() {
        final double eps = 2E-16;
        Report.printMethodHeader("testSeries", "Ellipsoid series", "LibKernel 10.0.0", eps, ComparisonType.RELATIVE);

        final double[] expected = { 0.9983242984527952, -0.002514607060518705,
            2.6390465943376213E-6,
            -3.418046086595787E-9, 0.0, 0.002518826584390675, 3.700949035620495E-6,
            7.447813767503831E-9,
            1.703599323859595E-11 };
        final double[] actual = ProjectionEllipsoidUtils.getSeries(ellipsoid);
        for (int i = 0; i < expected.length; i++) {
            if (expected[i] != 0) {
                Assert.assertEquals(0., (expected[i] - actual[i]) / expected[i], eps);
            } else {
                Assert.assertEquals(expected[i], actual[i], eps);
            }
        }
        Report.printToReport("Series", expected, actual);

        // Test cache update
        final OneAxisEllipsoid ellipsoid2 = new OneAxisEllipsoid(6300000.0, 1. / 200, null,
            "earth 2");
        final double[] actual2 = ProjectionEllipsoidUtils.getSeries(ellipsoid2);
        for (int j = 0; j < actual2.length; j++) {
            if (actual[j] != 0) {
                Assert.assertFalse(actual[j] == actual2[j]);
            }
        }
    }

    /**
     * Check list of EllipsoidPoint.
     * 
     * @param expected
     *        expected list
     * @param actual
     *        actual list
     * @param eps
     *        comparison epsilon (relative)
     */
    private static void checkListGeodeticPoint(final List<EllipsoidPoint> expected, final List<EllipsoidPoint> actual,
                                               final double eps) {
        Assert.assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            if (expected.get(i).getLLHCoordinates().getLatitude() != 0) {
                Assert.assertEquals(0., (expected.get(i).getLLHCoordinates().getLatitude() - actual.get(i)
                    .getLLHCoordinates().getLatitude())
                        / expected.get(i).getLLHCoordinates().getLatitude(), eps);
            } else {
                Assert.assertEquals(0., actual.get(i).getLLHCoordinates().getLatitude(), eps);
            }
            if (expected.get(i).getLLHCoordinates().getLongitude() != 0) {
                Assert.assertEquals(0., (expected.get(i).getLLHCoordinates().getLongitude() - actual.get(i)
                    .getLLHCoordinates().getLongitude())
                        / expected.get(i).getLLHCoordinates().getLongitude(), eps);
            } else {
                Assert.assertEquals(0., actual.get(i).getLLHCoordinates().getLongitude(), eps);
            }
            Assert.assertEquals(expected.get(i).getLLHCoordinates().getHeight(), actual.get(i).getLLHCoordinates()
                .getHeight(), eps);
            Report.printToReport("Point " + i + " (latitude)", expected.get(i).getLLHCoordinates().getLatitude(),
                actual.get(i).getLLHCoordinates().getLatitude());
            Report.printToReport("Point " + i + " (longitude)", expected.get(i).getLLHCoordinates().getLongitude(),
                actual.get(i).getLLHCoordinates().getLongitude());
            Report.printToReport("Point " + i + " (altitude)", expected.get(i).getLLHCoordinates().getHeight(), actual
                .get(i).getLLHCoordinates().getHeight());
        }
    }

    /**
     * @throws PatriusException
     *         if an error occurs
     * @testType UT
     * 
     * @testedFeature {@link features#PROJECTION_ELLIPSOID}
     * 
     * @testedMethod {@link ProjectionEllipsoidUtils#computeCenterPointAlongLoxodrome(EllipsoidPoint, EllipsoidPoint, EllipsoidBodyShape)}
     * 
     * @description test computation center between two points along a loxodrome
     * 
     * @input ellipsoid, two geodetic points
     * 
     * @output Geodetic point
     * 
     * @testPassCriteria result is identical to reference (reference : Math, tolerance: 0)
     * 
     * @referenceVersion 4.5
     * 
     * @nonRegressionVersion 4.5
     */
    @Test
    public void testComputeCenterAlongLoxodrome() throws PatriusException {
        Report.printMethodHeader("testComputeCenterAlongLoxodrome", "Compute center along loxodrome", "Math", 0,
            ComparisonType.RELATIVE);

        // Ellipsoid with no flattening for Mathematical computation
        final OneAxisEllipsoid ellipsoid = new OneAxisEllipsoid(6378137.0, 0, null, "");

        // On the equator
        final EllipsoidPoint p1 = new EllipsoidPoint(ellipsoid, ellipsoid.getLLHCoordinatesSystem(), 0, 1., 0, "");
        final EllipsoidPoint p2 = new EllipsoidPoint(ellipsoid, ellipsoid.getLLHCoordinatesSystem(), 0, 2., 0, "");
        final EllipsoidPoint actual1 = ProjectionEllipsoidUtils.computeCenterPointAlongLoxodrome(p1, p2);
        final EllipsoidPoint expected1 = new EllipsoidPoint(ellipsoid, ellipsoid.getLLHCoordinatesSystem(), 0, 1.5, 0,
            "");

        Assert.assertEquals(expected1.getLLHCoordinates().getLatitude(), actual1
            .getLLHCoordinates().getLatitude(), 0.);
        Assert.assertEquals(expected1.getLLHCoordinates().getLongitude(), actual1
            .getLLHCoordinates().getLongitude(), 0.);

        // On a meridian
        final EllipsoidPoint p3 = new EllipsoidPoint(ellipsoid, ellipsoid.getLLHCoordinatesSystem(), 1., 0., 0, "");
        final EllipsoidPoint p4 = new EllipsoidPoint(ellipsoid, ellipsoid.getLLHCoordinatesSystem(), 2., 0., 0, "");
        final EllipsoidPoint actual2 = ProjectionEllipsoidUtils.computeCenterPointAlongLoxodrome(p3, p4);
        final EllipsoidPoint expected2 = new EllipsoidPoint(ellipsoid, ellipsoid.getLLHCoordinatesSystem(), 1.5, 0., 0,
            "");

        Assert.assertEquals(expected2.getLLHCoordinates().getLatitude(), actual2.getLLHCoordinates().getLatitude(), 0.);
        Assert.assertEquals(expected2.getLLHCoordinates().getLongitude(), actual2.getLLHCoordinates().getLongitude(),
            0.);

        // Same points
        final EllipsoidPoint p5 = new EllipsoidPoint(ellipsoid, ellipsoid.getLLHCoordinatesSystem(), 0., 0., 0, "");
        final EllipsoidPoint p6 = new EllipsoidPoint(ellipsoid, ellipsoid.getLLHCoordinatesSystem(), 0., 0., 0, "");
        final EllipsoidPoint actual3 = ProjectionEllipsoidUtils.computeCenterPointAlongLoxodrome(p5, p6);
        final EllipsoidPoint expected3 = new EllipsoidPoint(ellipsoid, ellipsoid.getLLHCoordinatesSystem(), 0., 0., 0,
            "");

        Assert.assertEquals(expected3.getLLHCoordinates().getLatitude(), actual3.getLLHCoordinates().getLatitude(), 0.);
        Assert.assertEquals(expected3.getLLHCoordinates().getLongitude(), actual3.getLLHCoordinates().getLongitude(),
            0.);

        // Check exception when points are associated to different body shapes
        try {
            ProjectionEllipsoidUtils.computeCenterPointAlongLoxodrome(point1, pointOther);
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }
    }
}
