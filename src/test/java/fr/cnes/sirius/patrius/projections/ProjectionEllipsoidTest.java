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
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2102:15/05/2019:[Patrius] Refactoring du paquet fr.cnes.sirius.patrius.bodies
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:525:22/04/2016: add new functionalities existing in LibKernel
 * VERSION::FA:679:27/09/2016:Corrected orthodromic distance computation
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
/**
 */
package fr.cnes.sirius.patrius.projections;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class for {@link ProjectionEllipsoid}.
 * 
 * @author Thomas Galpin
 * @version $Id$
 * 
 */
@Deprecated
public class ProjectionEllipsoidTest {

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
    private static ProjectionEllipsoid ellipsoid;

    /** Geodetic point used in the tests (Toulouse). */
    private static GeodeticPoint point1;

    /** Geodetic point used in the tests (somewhere else). */
    private static GeodeticPoint point2;

    /** Geodetic point used in the tests (somewhere else). */
    private static GeodeticPoint point3;

    /** Geodetic point used in the tests (North pole). */
    private static GeodeticPoint point4;

    /** List of geodetic points. */
    private static List<GeodeticPoint> listGeodeticPoints;

    @BeforeClass
    public static void setUpBeforeClass() throws PatriusException {
        Report.printClassHeader(ProjectionEllipsoidTest.class.getSimpleName(), "Projection ellipsoid");

        // Projections
        ellipsoid = new ProjectionEllipsoid(6378137.0, 1. / 298.257223563, null, "default earth");

        // Point 1 (Toulouse)
        point1 = new GeodeticPoint(0.758011794744558, 0.0261405281982074, 256.);

        // Point 2 (somewhere else)
        point2 = new GeodeticPoint(0.2, 0.4, 1234.);

        // Point 3 (somewhere else)
        point3 = new GeodeticPoint(0.5, 0.2, 345.);

        // Point 4 (North pole)
        point4 = new GeodeticPoint(FastMath.PI / 2., 0., 0.);

        // List of geodetic points and Vector2D
        listGeodeticPoints = new ArrayList<GeodeticPoint>();
        listGeodeticPoints.add(point1);
        listGeodeticPoints.add(point2);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#PROJECTION_ELLIPSOID}
     * 
     * @testedMethod {@link ProjectionEllipsoid#computeBearing(GeodeticPoint, GeodeticPoint)}
     * 
     * @description test computation of bearing
     * 
     * @input ellipsoid, two geodetic points
     * 
     * @output bearing between the two geodetic points
     * 
     * @testPassCriteria result is identical to reference (reference : LibKernel library 10.0.0, tolerance: 0)
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public final void testComputeBearing() throws PatriusException {
        Report.printMethodHeader("testComputeBearing", "Compute bearing", "LibKernel 10.0.0", 0,
            ComparisonType.RELATIVE);

        final double actual1 = ellipsoid.computeBearing(point1, point2);
        final double actual2 = ellipsoid.computeBearing(point2, point1);
        final double actual3 = ellipsoid.computeBearing(point1, point3);
        final double actual4 = ellipsoid.computeBearing(point3, point1);
        final double actual5 = ellipsoid.computeBearing(point2, point3);
        final double actual6 = ellipsoid.computeBearing(point3, point2);

        final double eps = 0;
        final double expeted1 = 2.6119068833311587;
        final double expeted2 = 5.753499536920952;
        final double expeted3 = 2.643276806702529;
        final double expeted4 = 5.784869460292322;
        final double expeted5 = 5.723192810516977;
        final double expeted6 = 2.5816001569271845;

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

        // test with two points along a meridian
        final GeodeticPoint point5 = new GeodeticPoint(0.8, 0.0261405281982074, 256.);
        final GeodeticPoint point6 = new GeodeticPoint(0.7, 0.0261405281982074, 256.);
        final double actual7 = ellipsoid.computeBearing(point1, point5);
        final double actual8 = ellipsoid.computeBearing(point1, point6);
        final double expeted7 = 0.;
        final double expeted8 = 3.141592653589793;
        Assert.assertEquals(actual7, expeted7, eps);
        Assert.assertEquals(actual8, expeted8, eps);
        Report.printToReport("Bearing between point1 and point5", expeted7, actual7);
        Report.printToReport("Bearing between point1 and point6", expeted8, actual8);

        // test with two points same latitude
        final GeodeticPoint point7 = new GeodeticPoint(0.758011794744558, 0.02, 256.);
        final GeodeticPoint point8 = new GeodeticPoint(0.758011794744558, 0.03, 256.);
        final double actual9 = ellipsoid.computeBearing(point1, point7);
        final double actual10 = ellipsoid.computeBearing(point1, point8);
        final double expeted9 = 4.71238898038469;
        final double expeted10 = 1.5707963267948966;
        Assert.assertEquals(actual9, expeted9, eps);
        Assert.assertEquals(actual10, expeted10, eps);
        Report.printToReport("Bearing between point1 and point7", expeted9, actual9);
        Report.printToReport("Bearing between point1 and point8", expeted10, actual10);

        // Check exception when points are too close
        try {
            ellipsoid.computeBearing(point1, point1);
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
     * @testedMethod {@link ProjectionEllipsoid#computeSphericalAzimuth(GeodeticPoint, GeodeticPoint)}
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
    public final void testComputeSphericalAzimuth() {
        Report.printMethodHeader("testComputeSphericalAzimuth", "Compute spherical azimuth", "LibKernel 10.0.0", 0,
            ComparisonType.RELATIVE);

        final double actual1 = ellipsoid.computeSphericalAzimuth(point1, point2);
        final double actual2 = ellipsoid.computeSphericalAzimuth(point2, point1);
        final double actual3 = ellipsoid.computeSphericalAzimuth(point1, point3);
        final double actual4 = ellipsoid.computeSphericalAzimuth(point3, point1);
        final double actual5 = ellipsoid.computeSphericalAzimuth(point2, point3);
        final double actual6 = ellipsoid.computeSphericalAzimuth(point3, point2);

        final double eps = 0;
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
    public final void testComputeMercatorLatitude() {
        Report.printMethodHeader("testComputeMercatorLatitude", "Compute Mercator latitude", "LibKernel 10.0.0", 0,
            ComparisonType.RELATIVE);

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
                ellipsoid.computeMercatorLatitude(i * MathUtils.DEG_TO_RAD));
            if (i == step) {
                Report.printToReport("Mercator lat (" + MathLib.rint(i) + " deg)",
                    refMercatorLatitude[countRef], ellipsoid.computeMercatorLatitude(i * MathUtils.DEG_TO_RAD));
            }
            countRef++;
        }

        // input latitude = Mercator.MAX_LATITUDE
        Assert.assertEquals(refMercatorLatitude[16], ellipsoid.computeMercatorLatitude(Mercator.MAX_LATITUDE));
        Report.printToReport("Mercator lat (" + Mercator.MAX_LATITUDE * MathUtils.RAD_TO_DEG + " deg)",
            refMercatorLatitude[16], ellipsoid.computeMercatorLatitude(Mercator.MAX_LATITUDE));

        // input latitude = Mercator.MAX_LATITUDE + PI/360
        Assert.assertEquals(refMercatorLatitude[17],
            ellipsoid.computeMercatorLatitude(Mercator.MAX_LATITUDE + FastMath.PI / 360));
        Report.printToReport("Mercator lat (" + (Mercator.MAX_LATITUDE + FastMath.PI / 360) * MathUtils.RAD_TO_DEG
            + " deg)",
            refMercatorLatitude[17], ellipsoid.computeMercatorLatitude(Mercator.MAX_LATITUDE + FastMath.PI / 360));
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
    public final void testComputeRadiusEastWest() {
        Report.printMethodHeader("testComputeRadiusEastWest", "Compute radius east/west", "LibKernel 10.0.0", 0,
            ComparisonType.RELATIVE);

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
            Assert.assertEquals(refRadius[countRef], ellipsoid.computeRadiusEastWest(i));
            if (i == step) {
                Report.printToReport("Radius E/W", refRadius[countRef], ellipsoid.computeRadiusEastWest(i));
            }
            countRef++;
        }
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#PROJECTION_ELLIPSOID}
     * 
     * @testedMethod {@link ProjectionEllipsoid#computeLoxodromicDistance(GeodeticPoint, GeodeticPoint)}
     * 
     * @description test computation loxodromic distance between two points
     * 
     * @input two geodetic points
     * 
     * @output a loxodromic distance
     * 
     * @testPassCriteria loxodromic distance is identical to reference (reference : LibKernel library 10.0.0, tolerance:
     *                   3E-16)
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public final void testComputeLoxodromicDistance() throws PatriusException {
        Report.printMethodHeader("testComputeLoxodromicDistance", "Compute loxodromic distance", "LibKernel 10.0.0",
            3E-16, ComparisonType.RELATIVE);

        // test data
        final GeodeticPoint point5 = new GeodeticPoint(0.8, 0.0261405281982074, 256.);
        final GeodeticPoint point6 = new GeodeticPoint(0.7, 0.0261405281982074, 256.);
        final GeodeticPoint point7 = new GeodeticPoint(0.758011794744558, 0.02, 256.);
        final GeodeticPoint point8 = new GeodeticPoint(0.758011794744558, 0.03, 256.);
        final GeodeticPoint badPoint = new GeodeticPoint(MathUtils.DEG_TO_RAD * 90, 0.03, 256.);

        // actual
        final double[] lox = { ellipsoid.computeLoxodromicDistance(point1, point2),
            ellipsoid.computeLoxodromicDistance(point2, point1),
            ellipsoid.computeLoxodromicDistance(point1, point3),
            ellipsoid.computeLoxodromicDistance(point3, point1),
            ellipsoid.computeLoxodromicDistance(point2, point3),
            ellipsoid.computeLoxodromicDistance(point3, point2),
            ellipsoid.computeLoxodromicDistance(point5, point6),
            ellipsoid.computeLoxodromicDistance(point7, point8) };

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
            Assert.assertEquals(0., (loxRef[i] - lox[i]) / loxRef[i], 3E-16);
            Report.printToReport("Loxodromic distance " + i, loxRef[i], lox[i]);
        }

        // Check exception when one of the latitude point is over 90°
        try {
            ellipsoid.computeLoxodromicDistance(point1, badPoint);
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }
    }

    /**
     * @throws PatriusException
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
    public final void testComputeMeridionalDistance() throws PatriusException {
        Report.printMethodHeader("testComputeMeridionalDistance", "Compute meridional distance", "LibKernel 10.0.0", 0,
            ComparisonType.RELATIVE);

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
            Assert.assertEquals(refMeridionalDist[countRef], ellipsoid.computeMeridionalDistance(i));
            if (i == step) {
                Report.printToReport("Meridional distance", refMeridionalDist[countRef],
                    ellipsoid.computeMeridionalDistance(i));
            }
            countRef++;
        }

    }

    /**
     * @throws PatriusException
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
    public final void testComputeInverseMeridionalDistance() throws PatriusException {
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
            Assert.assertEquals(refInverseMeridionalDist[countRef], ellipsoid.computeInverseMeridionalDistance(i));
            if (i == step) {
                Report.printToReport("Inverse meridional distance", refInverseMeridionalDist[countRef],
                    ellipsoid.computeInverseMeridionalDistance(i));
            }
            countRef++;
        }
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#PROJECTION_ELLIPSOID}
     * 
     * @testedMethod {@link ProjectionEllipsoid#computePointAlongLoxodrome(GeodeticPoint, double, double)}
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
    public final void testComputePointAlongLoxodrome() throws PatriusException {
        Report.printMethodHeader("testComputePointAlongLoxodrome", "Compute point along loxodrome", "LibKernel 10.0.0",
            1E-15, ComparisonType.RELATIVE);

        final double eps = 1E-15;

        // ref
        final List<GeodeticPoint> refPoints = new ArrayList<GeodeticPoint>();
        refPoints.add(new GeodeticPoint(0.7892352562618954, 0.0, 0));
        refPoints.add(new GeodeticPoint(0.5585544800165291, 0.5865408288158058, 0));
        refPoints.add(new GeodeticPoint(0.0, 0.7853981633974484, 0));
        refPoints.add(new GeodeticPoint(-0.558554480016529, 0.5865408288158056, 0));
        refPoints.add(new GeodeticPoint(-0.7892352562618954, 0.0, 0));
        refPoints.add(new GeodeticPoint(-0.558554480016529, -0.5865408288158056, 0));
        refPoints.add(new GeodeticPoint(0.0, -0.7853981633974484, 0));
        refPoints.add(new GeodeticPoint(0.5585544800165291, -0.5865408288158058, 0));
        refPoints.add(new GeodeticPoint(0.7892352562618954, 0.0, 0));

        // ref
        final List<GeodeticPoint> actualPoints = new ArrayList<GeodeticPoint>();
        final GeodeticPoint p0 = new GeodeticPoint(0, 0, 0);
        final double d1 = 0.5 * ellipsoid.getEquatorialRadius() * FastMath.PI / 2;
        // loop over 10 points :
        for (int i = 0; i <= 360; i += 360 / 8) {
            final double azimuth = MathUtils.DEG_TO_RAD * i;
            actualPoints.add(ellipsoid.computePointAlongLoxodrome(p0, d1, azimuth));
        }
        // check
        this.checkListGeodeticPoint(refPoints, actualPoints, eps);

        // exceptions :
        try {
            ellipsoid.computePointAlongLoxodrome(new GeodeticPoint(MathUtils.DEG_TO_RAD * 90, 0.03, 256.), d1, 0.1);
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }
        try {
            ellipsoid.computePointAlongLoxodrome(new GeodeticPoint(MathUtils.DEG_TO_RAD * 89, 0.03, 256.), d1, 0.1);
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }
    }

    /**
     * @throws PatriusException
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
    public final void testComputeInverseRectifyingLatitude() throws PatriusException {
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
            Assert.assertEquals(refInverseRectifyingLat[countRef], ellipsoid.computeInverseRectifyingLatitude(i));
            if (i == step) {
                Report.printToReport("Inverse rectifying latitude",
                    refInverseRectifyingLat[countRef], ellipsoid.computeInverseRectifyingLatitude(i));
            }
            countRef++;
        }

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#PROJECTION_ELLIPSOID}
     * 
     * @testedMethod {@link ProjectionEllipsoid#computeOrthodromicDistance(GeodeticPoint, GeodeticPoint)}
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
    public final void testComputeOrthodromicDistance() throws PatriusException {
        Report.printMethodHeader("testComputeOrthodromicDistance", "Compute orthodromic distance", "LibKernel 10.0.0",
            3E-14, ComparisonType.RELATIVE);

        // Toulouse
        final double actual1 = ellipsoid.computeOrthodromicDistance(point1, point2);
        final double actual2 = ellipsoid.computeOrthodromicDistance(point1, point2);

        final double eps = 3E-14;
        final double expected = 4100536.2596218484;

        Assert.assertEquals(0., (expected - actual1) / expected, eps);
        Assert.assertEquals(0., (expected - actual2) / expected, eps);
        Report.printToReport("Orthodromic distance", expected, actual1);

        // North pole
        final double actual4 = ellipsoid.computeOrthodromicDistance(point1, point4);
        final double expected4 = 5191376.768899236;
        Assert.assertEquals(0., (expected4 - actual4) / expected4, eps);
        Report.printToReport("Orthodromic distance (North pole)", expected4, actual4);

        // Special case (delta-longitude > Pi)
        final GeodeticPoint point5 = new GeodeticPoint(0.2, -3.14, 1234.);
        final double actual5 = ellipsoid.computeOrthodromicDistance(point1, point5);
        final double expected5 = 1.3924419603948575E7;
        Assert.assertEquals(0., (expected5 - actual5) / expected5, eps);
        Report.printToReport("Orthodromic distance (dlon > PI)", expected5, actual5);

        // Special case : input points having same latitude in one hand OR same longitude on the other :
        // computed distance must not be 0
        final GeodeticPoint point6 = new GeodeticPoint(0.2, 0.5, 1234.);
        final GeodeticPoint point7 = new GeodeticPoint(0.1, 0.4, 1234.);
        final double actual6 = ellipsoid.computeOrthodromicDistance(point2, point6);
        final double actual7 = ellipsoid.computeOrthodromicDistance(point2, point7);
        Assert.assertFalse(actual6 == 0.);
        Assert.assertFalse(actual7 == 0.);

        // Finally, null distance is expected if points have same latitude AND same longitude
        final GeodeticPoint point8 = new GeodeticPoint(0.2, 0.4, 1234.);
        final double actual8 = ellipsoid.computeOrthodromicDistance(point2, point8);
        Assert.assertEquals(actual8, 0., 0.);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#PROJECTION_ELLIPSOID}
     * 
     * @testedMethod {@link ProjectionEllipsoid#computePointAlongOrthodrome(GeodeticPoint, double, double)}
     * 
     * @description test the computation of point along an orthodrome
     * 
     * @input ellipsoid
     * 
     * @output geodetic coordinates
     * 
     * @testPassCriteria result is identical to reference (reference : LibKernel library 10.0.0, tolerance: 0)
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public final void testComputePointAlongOrthodrome() throws PatriusException {
        Report.printMethodHeader("testComputePointAlongOrthodrome", "Compute point along orthodrome",
            "LibKernel 10.0.0", 0, ComparisonType.RELATIVE);

        final GeodeticPoint actual = ellipsoid.computePointAlongOrthodrome(point1, 12345., 0.5);

        final double eps = 0;
        final GeodeticPoint expected = new GeodeticPoint(MathLib.toRadians(43.528364978062925),
            MathLib.toRadians(1.5709556155153825), 0.);

        Assert.assertEquals(0., (expected.getLatitude() - actual.getLatitude()) / expected.getLatitude(), eps);
        Assert.assertEquals(0., (expected.getLongitude() - actual.getLongitude()) / expected.getLongitude(), eps);
        Assert.assertEquals(expected.getAltitude(), actual.getAltitude(), eps);
        Report.printToReport("Latitude", expected.getLatitude(), actual.getLatitude());
        Report.printToReport("Longitude", expected.getLongitude(), actual.getLongitude());
        Report.printToReport("Altitude", expected.getAltitude(), actual.getAltitude());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#PROJECTION_ELLIPSOID}
     * 
     * @testedMethod {@link ProjectionEllipsoid#discretizeGreatCircle(GeodeticPoint, GeodeticPoint, double)}
     * 
     * @description test the discretization along a Great Circle
     * 
     * @input ellipsoid
     * 
     * @output geodetic coordinates
     * 
     * @testPassCriteria result is identical to reference (reference : LibKernel library 10.0.0, tolerance: 3E-14)
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public final void testDiscretizationGreatCircle() throws PatriusException {
        Report.printMethodHeader("testDiscretizationGreatCircle", "Discretization along Great Circle",
            "LibKernel 10.0.0", 3E-14, ComparisonType.RELATIVE);

        // 3 maxlength cases
        final List<GeodeticPoint> actual1 = ellipsoid.discretizeGreatCircle(point1, point2, 1000000);
        final List<GeodeticPoint> actual2 = ellipsoid.discretizeGreatCircle(point1, point2, 0);
        final List<GeodeticPoint> actual3 = ellipsoid.discretizeGreatCircle(point1, point2, 4000000);

        final double eps = 3E-14;
        final List<GeodeticPoint> expected1 = new ArrayList<GeodeticPoint>();
        expected1.add(new GeodeticPoint(MathLib.toRadians(43.43087666000001), MathLib.toRadians(1.4977419400000025),
            256.));
        expected1.add(new GeodeticPoint(MathLib.toRadians(35.80077182694041), MathLib.toRadians(8.237088262669358),
            0.));
        expected1.add(new GeodeticPoint(MathLib.toRadians(27.852456068881388), MathLib.toRadians(13.777774346708954),
            0.));
        expected1.add(new GeodeticPoint(MathLib.toRadians(19.70725940634673), MathLib.toRadians(18.56131861964875),
            0.));

        final List<GeodeticPoint> expected2 = new ArrayList<GeodeticPoint>();
        expected2.add(new GeodeticPoint(MathLib.toRadians(43.43087666000001), MathLib.toRadians(1.4977419400000025),
            256.));
        expected2.add(new GeodeticPoint(MathLib.toRadians(11.459155902616466), MathLib.toRadians(22.918311805232932),
            1234.));

        final List<GeodeticPoint> expected3 = new ArrayList<GeodeticPoint>();
        expected3.add(new GeodeticPoint(MathLib.toRadians(43.43087666000001), MathLib.toRadians(1.4977419400000025),
            256.));
        expected3.add(new GeodeticPoint(MathLib.toRadians(11.459155902616466), MathLib.toRadians(22.918311805232932),
            1234.));

        this.checkListGeodeticPoint(expected1, actual1, eps);
        this.checkListGeodeticPoint(expected2, actual2, eps);
        this.checkListGeodeticPoint(expected3, actual3, eps);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#PROJECTION_ELLIPSOID}
     * 
     * @testedMethod {@link ProjectionEllipsoid#discretizeRhumbLine(GeodeticPoint, GeodeticPoint, double)}
     * 
     * @description test the discretization along a Rhumb line
     * 
     * @input ellipsoid
     * 
     * @output geodetic coordinates
     * 
     * @testPassCriteria result is identical to reference (reference : LibKernel library 10.0.0, tolerance: 2E-16)
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public final void testDiscretizationRhumbLine() throws PatriusException {
        Report.printMethodHeader("testDiscretizationRhumbLine", "Discretization along Rhumb line", "LibKernel 10.0.0",
            2E-16, ComparisonType.RELATIVE);

        // 3 maxlength cases
        final List<GeodeticPoint> actual1 = ellipsoid.discretizeRhumbLine(point1, point2, 1000000);
        final List<GeodeticPoint> actual2 = ellipsoid.discretizeRhumbLine(point1, point2, 0);
        final List<GeodeticPoint> actual3 = ellipsoid.discretizeRhumbLine(point1, point2, 4000000);

        final double eps = 2E-16;
        final List<GeodeticPoint> expected1 = new ArrayList<GeodeticPoint>();
        expected1.add(new GeodeticPoint(MathLib.toRadians(43.43087666000001), MathLib.toRadians(1.4977419400000025),
            256.));
        expected1.add(new GeodeticPoint(MathLib.toRadians(35.45214288961152), MathLib.toRadians(7.534123017198017),
            0.));
        expected1.add(new GeodeticPoint(MathLib.toRadians(27.46289676019212), MathLib.toRadians(12.998775866019459),
            0.));
        expected1.add(new GeodeticPoint(MathLib.toRadians(19.464531632933117), MathLib.toRadians(18.080863380100528),
            0.));

        final List<GeodeticPoint> expected2 = new ArrayList<GeodeticPoint>();
        expected2.add(new GeodeticPoint(MathLib.toRadians(43.43087666000001), MathLib.toRadians(1.4977419400000025),
            256.));

        final List<GeodeticPoint> expected3 = new ArrayList<GeodeticPoint>();
        expected3.add(new GeodeticPoint(MathLib.toRadians(43.43087666000001), MathLib.toRadians(1.4977419400000025),
            256.));
        expected3.add(new GeodeticPoint(MathLib.toRadians(11.459155902616466), MathLib.toRadians(22.918311805232932),
            1234.));

        this.checkListGeodeticPoint(expected1, actual1, eps);
        this.checkListGeodeticPoint(expected2, actual2, eps);
        this.checkListGeodeticPoint(expected3, actual3, eps);
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
    public final void testEccentricity() throws PatriusException {
        Report.printMethodHeader("testEccentricity", "Ellipsoid eccentricity value", "LibKernel 10.0.0", 0,
            ComparisonType.RELATIVE);

        Assert.assertEquals(0.08181919084262149, ellipsoid.getEccentricity(), 0.);
        Report.printToReport("Eccentricity", 0.08181919084262149, ellipsoid.getEccentricity());
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
    public final void testSeries() throws PatriusException {
        Report.printMethodHeader("testSeries", "Ellipsoid series", "LibKernel 10.0.0", 2E-16, ComparisonType.RELATIVE);

        final double eps = 2E-16;
        final double[] expected = { 0.9983242984527952, -0.002514607060518705, 2.6390465943376213E-6,
            -3.418046086595787E-9, 0.0, 0.002518826584390675, 3.700949035620495E-6, 7.447813767503831E-9,
            1.703599323859595E-11 };
        final double[] actual = ellipsoid.getSeries();
        for (int i = 0; i < expected.length; i++) {
            if (expected[i] != 0) {
                Assert.assertEquals(0., (expected[i] - actual[i]) / expected[i], eps);
            } else {
                Assert.assertEquals(expected[i], actual[i], eps);
            }
        }
        Report.printToReport("Series", expected, actual);
    }

    /**
     * Check list of GeodeticPoint.
     * 
     * @param expected
     *        expected list
     * @param actual
     *        actual list
     * @param eps
     *        comparison epsilon (relative)
     */
    private void checkListGeodeticPoint(final List<GeodeticPoint> expected, final List<GeodeticPoint> actual,
                                        final double eps) {
        Assert.assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            if (expected.get(i).getLatitude() != 0) {
                Assert.assertEquals(0., (expected.get(i).getLatitude() - actual.get(i).getLatitude())
                    / expected.get(i).getLatitude(), eps);
            } else {
                Assert.assertEquals(0., actual.get(i).getLatitude(), eps);
            }
            if (expected.get(i).getLongitude() != 0) {
                Assert.assertEquals(0.,
                    (expected.get(i).getLongitude() - actual.get(i).getLongitude())
                        / expected.get(i).getLongitude(), eps);
            } else {
                Assert.assertEquals(0., actual.get(i).getLongitude(), eps);
            }
            Assert.assertEquals(expected.get(i).getAltitude(), actual.get(i).getAltitude(), eps);
            Report.printToReport("Point " + i + " (latitude)", expected.get(i).getLatitude(), actual.get(i)
                .getLatitude());
            Report.printToReport("Point " + i + " (longitude)", expected.get(i).getLongitude(), actual.get(i)
                .getLongitude());
            Report.printToReport("Point " + i + " (altitude)", expected.get(i).getAltitude(), actual.get(i)
                .getAltitude());
        }
    }
}
