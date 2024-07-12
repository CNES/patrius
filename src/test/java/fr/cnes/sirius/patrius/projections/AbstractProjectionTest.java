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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2102:15/05/2019:[Patrius] Refactoring du paquet fr.cnes.sirius.patrius.bodies
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:525:22/04/2016: add new functionalities existing in LibKernel
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
import fr.cnes.sirius.patrius.bodies.EllipsoidBodyShape;
import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.math.geometry.euclidean.twod.Vector2D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class for {@link AbstractProjection}.
 * Test are performed using a Mercator projection.
 * 
 * @author Emmanuel Bignon
 * @version $Id$
 * 
 */
public class AbstractProjectionTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Validate the common projection methods
         * 
         * @featureDescription Validate the common projection methods
         * 
         * @coveredRequirements DV-CARTO_20
         */
        PROJECTION
    }

    /** Projection used in the tests (Mercator projection). */
    private static Mercator projection;

    /** Projection used in the tests (Flamsteed-Samson projection). */
    private static GeneralizedFlamsteedSamson projection2;

    /** Geodetic point used in the tests (Toulouse). */
    private static GeodeticPoint point1;

    /** Geodetic point used in the tests (somewhere else). */
    private static GeodeticPoint point2;

    /** Geodetic point used in the tests (somewhere else). */
    private static GeodeticPoint point3;

    /** Projection of first geodetic point used in the tests (Toulouse). */
    private static Vector2D proj1;

    /** Projection of second geodetic point used in the tests (somewhere else). */
    private static Vector2D proj2;

    /** Projection of third geodetic point used in the tests (somewhere else). */
    private static Vector2D proj3;

    /** List of geodetic points. */
    private static List<GeodeticPoint> listGeodeticPoints;

    /** List of Vector2D. */
    private static List<Vector2D> listVector2D;

    @BeforeClass
    public static void setUpBeforeClass() throws PatriusException {
        Report.printClassHeader(AbstractProjectionTest.class.getSimpleName(), "Projection");

        // Projections
        final EllipsoidBodyShape ellipsoid = new OneAxisEllipsoid(6378137.0,
            1. / 298.257223563, null,
            "default earth");
        projection = new Mercator(new GeodeticPoint(0.1, 0.2, 0.3), ellipsoid, 0.5, true, false);
        projection2 = new GeneralizedFlamsteedSamson(new GeodeticPoint(0.1, 0.2, 0.3), ellipsoid, 0.5);

        // Point 1 (Toulouse)
        point1 = new GeodeticPoint(0.758011794744558, 0.0261405281982074, 256.);
        proj1 = projection.applyTo(point1);
        // Point 2 (somewhere else)
        point2 = new GeodeticPoint(0.2, 0.4, 1234.);
        proj2 = projection.applyTo(point2);
        // Point 3 (somewhere else)
        point3 = new GeodeticPoint(0.5, 0.2, 345.);
        proj3 = projection.applyTo(point3);

        // List of geodetic points and Vector2D
        listGeodeticPoints = new ArrayList<>();
        listGeodeticPoints.add(point1);
        listGeodeticPoints.add(point2);
        listGeodeticPoints.add(point3);
        listVector2D = new ArrayList<>();
        listVector2D.add(proj1);
        listVector2D.add(proj2);
        listVector2D.add(proj3);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#PROJECTION}
     * 
     * @testedMethod {@link AbstractProjection#discretize(Vector2D, Vector2D, double, boolean)}
     * 
     * @description test discretization of the projection
     * 
     * @input ellipsoid, projection
     * 
     * @output discretized points
     * 
     * @testPassCriteria result is identical to reference (reference : LibKernel library 10.0.0, tolerance: 2E-13)
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testDiscretize() {
        Report.printMethodHeader("testDiscretize", "Discretize", "LibKernel 10.0.0", 2E-13, ComparisonType.RELATIVE);

        final List<Vector2D> actual = projection.discretize(proj1, proj2, 1000000., true);

        final double eps = 2E-13;
        final List<Vector2D> expected = new ArrayList<>();
        expected.add(new Vector2D(-3217015.7348985393, 3587210.75583779));
        expected.add(new Vector2D(-2411998.5702685374, 3103443.558572025));
        expected.add(new Vector2D(-1606981.4056385353, 2619676.3613062603));
        expected.add(new Vector2D(-801964.2410085332, 2135909.1640404956));
        expected.add(new Vector2D(3052.9236214687116, 1652141.9667747305));
        expected.add(new Vector2D(808070.0882514704, 1168374.7695089655));

        checkListVector2D(expected, actual, eps);

        // Check last point has been discarded
        Assert.assertEquals(expected.size() - 1, projection.discretize(proj1, proj2, 1000000., false).size());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#PROJECTION}
     * 
     * @testedMethod {@link AbstractProjection#discretize(Vector2D, Vector2D, double, boolean)}
     * @testedMethod {@link AbstractProjection#discretizeCircleAndApplyTo(List, double)}
     * @testedMethod {@link AbstractProjection#discretizeRhumbAndApplyTo(List, double)}
     * 
     * @description test discretization followed by application of the projection (circle, rhumb, straight and none)
     * 
     * @input ellipsoid, projection
     * 
     * @output discretized and projected points, exception in case of EnumLineProperty = NONE
     * 
     * @testPassCriteria result is identical to reference (reference : LibKernel library 10.0.0, tolerance: 2E-12)
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testDiscretizeAndApplyTo() throws PatriusException {
        Report.printMethodHeader("testDiscretizeAndApplyTo", "Discretize and apply projection (circle and rhumb)",
            "LibKernel 10.0.0", 2E-12, ComparisonType.RELATIVE);

        final List<Vector2D> actual1 = projection.discretizeAndApplyTo(listGeodeticPoints,
            EnumLineProperty.GREAT_CIRCLE, 1000000.);
        final List<Vector2D> actual2 = projection.discretizeAndApplyTo(listGeodeticPoints, EnumLineProperty.STRAIGHT,
            1000000.);
        final List<Vector2D> actual3 = projection.discretizeAndApplyTo(listGeodeticPoints,
            EnumLineProperty.STRAIGHT_RHUMB_LINE, 1000000.);

        final List<Vector2D> actual4 = projection.discretizeAndApplyTo(listGeodeticPoints,
            EnumLineProperty.GREAT_CIRCLE, 0.);
        final List<Vector2D> actual5 = projection.discretizeAndApplyTo(listGeodeticPoints,
            EnumLineProperty.STRAIGHT_RHUMB_LINE, 0.);

        final List<Vector2D> actual6 = projection2.discretizeAndApplyTo(listGeodeticPoints,
            EnumLineProperty.STRAIGHT_RHUMB_LINE, 1000000.);
        final List<Vector2D> actual7 = projection2.discretizeAndApplyTo(listGeodeticPoints,
            EnumLineProperty.STRAIGHT_RHUMB_LINE, 0.);

        final double eps = 2E-12;
        final List<Vector2D> expected1 = new ArrayList<>();
        expected1.add(new Vector2D(-3217015.7348985393, 3587210.75583779));
        expected1.add(new Vector2D(-2037077.1187211685, 2984414.2761594327));
        expected1.add(new Vector2D(-1003394.2132250291, 2372397.177764573));
        expected1.add(new Vector2D(-67819.26724412781, 1765034.541158774));
        expected1.add(new Vector2D(808070.0882514704, 1168374.7695089655));
        expected1.add(new Vector2D(-193944.5142212914, 1757169.6418341447));

        final List<Vector2D> expected2 = new ArrayList<>();

        final List<Vector2D> expected3 = new ArrayList<>();
        expected3.add(new Vector2D(-3217015.7348985393, 3587210.75583779));
        expected3.add(new Vector2D(-2411998.5702685374, 3103443.558572025));
        expected3.add(new Vector2D(-1606981.4056385353, 2619676.3613062603));
        expected3.add(new Vector2D(-801964.2410085332, 2135909.1640404956));
        expected3.add(new Vector2D(3052.9236214687116, 1652141.9667747305));
        expected3.add(new Vector2D(808070.0882514704, 1168374.7695089655));
        expected3.add(new Vector2D(113218.49731743801, 1557779.6936806152));
        expected3.add(new Vector2D(-581633.0936165943, 1947184.617852265));
        expected3.add(new Vector2D(-1276484.684550627, 2336589.542023915));
        expected3.add(new Vector2D(-1923328.3679999309, 2753463.2799618733));
        expected3.add(new Vector2D(-2570172.051449235, 3170337.0178998318));

        final List<Vector2D> expected4 = new ArrayList<>();
        expected4.add(new Vector2D(-3217015.7348985393, 3587210.75583779));
        expected4.add(new Vector2D(808070.0882514704, 1168374.7695089655));
        expected4.add(new Vector2D(-1276484.684550627, 2336589.542023915));

        final List<Vector2D> expected5 = new ArrayList<>();
        expected5.add(new Vector2D(-3217015.7348985393, 3587210.75583779));
        expected5.add(new Vector2D(808070.0882514704, 1168374.7695089655));
        expected5.add(new Vector2D(-1276484.684550627, 2336589.542023915));

        final List<Vector2D> expected6 = new ArrayList<>();
        expected6.add(new Vector2D(-2544416.4212971386, 3369672.300353526));
        expected6.add(new Vector2D(-1782046.869623942, 2776755.1421492966));
        expected6.add(new Vector2D(-958958.1192068739, 2217009.0333577353));
        expected6.add(new Vector2D(-95785.75317010291, 1679160.7035868398));
        expected6.add(new Vector2D(790739.8394354179, 1154070.299807879));
        expected6.add(new Vector2D(-194738.08323646756, 1699922.7446641875));
        expected6.add(new Vector2D(-1151693.812343351, 2261356.9349844963));
        expected6.add(new Vector2D(-2544416.4212971386, 3369672.300353526));

        final List<Vector2D> expected7 = new ArrayList<>();
        expected7.add(new Vector2D(-2544416.4212971386, 3369672.300353526));
        expected7.add(new Vector2D(790739.8394354179, 1154070.299807879));
        expected7.add(new Vector2D(-1151693.812343351, 2261356.9349844963));

        checkListVector2D(expected1, actual1, eps);
        checkListVector2D(expected2, actual2, eps);
        checkListVector2D(expected3, actual3, eps);
        checkListVector2D(expected4, actual4, eps);
        checkListVector2D(expected5, actual5, eps);
        checkListVector2D(expected6, actual6, eps);
        checkListVector2D(expected7, actual7, eps);

        // Check exception is thrown with EnumLineProperty = none
        try {
            projection.discretizeAndApplyTo(listGeodeticPoints, EnumLineProperty.NONE, 1000000.);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#PROJECTION}
     * 
     * @testedMethod {@link AbstractProjection#applyToAndDiscretize(GeodeticPoint, GeodeticPoint, double, boolean)}
     * 
     * @description test application of the projection followed by discretization
     * 
     * @input ellipsoid, projection
     * 
     * @output discretized projected points
     * 
     * @testPassCriteria result is identical to reference (reference : LibKernel library 10.0.0, tolerance: 2E-13)
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testApplyToAndDiscretize() throws PatriusException {
        Report.printMethodHeader("testApplyToAndDiscretize", "Apply projection and discretize", "LibKernel 10.0.0",
            2E-13, ComparisonType.RELATIVE);

        final List<Vector2D> actual1 = projection.applyToAndDiscretize(point1, point2, 1000000., true);
        final List<Vector2D> actual2 = projection.applyToAndDiscretize(point1, point2, 0., true);

        final double eps = 2E-13;
        final List<Vector2D> expected1 = new ArrayList<>();
        expected1.add(new Vector2D(-3217015.7348985393, 3587210.75583779));
        expected1.add(new Vector2D(-2411998.5702685374, 3103443.558572025));
        expected1.add(new Vector2D(-1606981.4056385353, 2619676.3613062603));
        expected1.add(new Vector2D(-801964.2410085332, 2135909.1640404956));
        expected1.add(new Vector2D(3052.9236214687116, 1652141.9667747305));
        expected1.add(new Vector2D(808070.0882514704, 1168374.7695089655));

        final List<Vector2D> expected2 = new ArrayList<>();
        expected2.add(new Vector2D(-3217015.7348985393, 3587210.75583779));
        expected2.add(new Vector2D(808070.0882514704, 1168374.7695089655));

        checkListVector2D(expected1, actual1, eps);
        checkListVector2D(expected2, actual2, eps);

        // Check last point has been discarded
        Assert.assertEquals(expected2.size() - 1, projection.applyToAndDiscretize(point1, point2, 0., false).size());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#PROJECTION}
     * 
     * @testedMethod {@link AbstractProjection#applyTo(List)}
     * 
     * @description test application of the projection
     * 
     * @input ellipsoid, projection
     * 
     * @output projected points
     * 
     * @testPassCriteria result is identical to reference (reference : LibKernel library 10.0.0, tolerance: 1E-15)
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testApplyTo() throws PatriusException {
        Report.printMethodHeader("testApplyTo", "Apply projection", "LibKernel 10.0.0", 1E-15, ComparisonType.RELATIVE);

        final List<Vector2D> actual = projection.applyTo(listGeodeticPoints);

        final double eps = 2E-13;
        final List<Vector2D> expected = new ArrayList<>();
        expected.add(new Vector2D(-3217015.7348985393, 3587210.75583779));
        expected.add(new Vector2D(808070.0882514704, 1168374.7695089655));
        expected.add(new Vector2D(-1276484.684550627, 2336589.542023915));

        checkListVector2D(expected, actual, eps);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#PROJECTION}
     * 
     * @testedMethod {@link AbstractProjection#applyInverseTo(List)}
     * @testedMethod {@link AbstractProjection#applyInverseTo(double[], double[])}
     * 
     * @description test inverse application of the projection
     * 
     * @input ellipsoid, projection
     * 
     * @output geodetic coordinates
     * 
     * @testPassCriteria result is identical to reference (reference : LibKernel library 10.0.0, tolerance: 1E-15)
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testApplyInverseTo() throws PatriusException {
        Report.printMethodHeader("testApplyInverseTo", "Apply inverse projection", "LibKernel 10.0.0", 1E-15,
            ComparisonType.RELATIVE);

        final List<GeodeticPoint> actual1 = projection.applyInverseTo(listVector2D);

        final double[] x = new double[listVector2D.size()];
        final double[] y = new double[listVector2D.size()];
        final double[] y2 = new double[listVector2D.size() + 1];
        for (int i = 0; i < listVector2D.size(); i++) {
            x[i] = listVector2D.get(i).getX();
            y[i] = listVector2D.get(i).getY();
            y2[i] = listVector2D.get(i).getY();
        }
        final List<GeodeticPoint> actual2 = projection.applyInverseTo(x, y);

        final double eps = 1E-15;
        final List<GeodeticPoint> expected = new ArrayList<>();
        expected.add(new GeodeticPoint(MathLib.toRadians(43.43087666000002), MathLib.toRadians(1.4977419400000034),
            0.));
        expected.add(new GeodeticPoint(MathLib.toRadians(11.45915590261646), MathLib.toRadians(22.918311805232932),
            0.));
        expected.add(new GeodeticPoint(MathLib.toRadians(28.64788975654117), MathLib.toRadians(11.459155902616464),
            0.));

        checkListGeodeticPoint(expected, actual1, eps);
        checkListGeodeticPoint(expected, actual2, eps);

        // Check exception thrown if x.length != y.length
        try {
            projection.applyInverseTo(x, y2);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
    }

    /**
     * Check list of Vector2D.
     * 
     * @param expected
     *        expected list
     * @param actual
     *        actual list
     * @param eps
     *        comparison epsilon (relative)
     */
    private static void checkListVector2D(final List<Vector2D> expected, final List<Vector2D> actual, final double eps) {
        Assert.assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            Assert.assertEquals(0., (expected.get(i).getX() - actual.get(i).getX()) / expected.get(i).getX(), eps);
            Assert.assertEquals(0., (expected.get(i).getY() - actual.get(i).getY()) / expected.get(i).getY(), eps);
            Report.printToReport("Point " + i, expected.get(i), actual.get(i));
        }
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
    private static void checkListGeodeticPoint(final List<GeodeticPoint> expected, final List<GeodeticPoint> actual,
                                        final double eps) {
        Assert.assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            Assert.assertEquals(0., (expected.get(i).getLatitude() - actual.get(i).getLatitude())
                / expected.get(i).getLatitude(), eps);
            Assert.assertEquals(0., (expected.get(i).getLongitude() - actual.get(i).getLongitude())
                / expected.get(i).getLongitude(), eps);
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
