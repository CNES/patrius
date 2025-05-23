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
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
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

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.bodies.EllipsoidBodyShape;
import fr.cnes.sirius.patrius.bodies.EllipsoidPoint;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.math.geometry.euclidean.twod.Vector2D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class for {@link Mercator}.
 * 
 * @author Emmanuel Bignon
 * @version $Id$
 * 
 */
public class MercatorTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Validate the Mercator projection
         * 
         * @featureDescription Validate the Mercator projection
         * 
         * @coveredRequirements DV-CARTO_20
         */
        MERCATOR_PROJECTION
    }

    /** Projection used in the tests (simple classic projection). */
    private static Mercator projection1;

    /** Projection used in the tests (centered, no series and azimuth != 0). */
    private static Mercator projection2;

    /** Point used in the tests (Toulouse). */
    private static EllipsoidPoint point1;

    /** Point used in the tests (North pole). */
    private static EllipsoidPoint point2;

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(MercatorTest.class.getSimpleName(), "Mercator projection");

        // Projections
        final EllipsoidBodyShape ellipsoid = new OneAxisEllipsoid(6378137.0,
            1. / 298.257223563, null,
            "default earth");
        projection1 = new Mercator(0., ellipsoid);
        projection2 = new Mercator(
            new EllipsoidPoint(ellipsoid, ellipsoid.getLLHCoordinatesSystem(), 0.1, 0.2, 0.3, ""), 0.5, true, false);

        // Point 1 (Toulouse)
        point1 = new EllipsoidPoint(ellipsoid, ellipsoid.getLLHCoordinatesSystem(), 0.758011794744558,
            0.0261405281982074, 256., "");
        // Point 2 (North pole)
        point2 = new EllipsoidPoint(ellipsoid, ellipsoid.getLLHCoordinatesSystem(), FastMath.PI / 2.,
            0.0261405281982074, 256., "");
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#MERCATOR_PROJECTION}
     * 
     * @testedMethod {@link Mercator#canMap(EllipsoidPoint)}
     * 
     * @description Check the Mercator projection can map provided points
     * 
     * @input ellipsoid, projection
     * 
     * @output boolean
     * 
     * @testPassCriteria Toulouse: true, North pole: false (reference : LibKernel library 10.0.0)
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testCanMap() {
        // Toulouse
        Assert.assertTrue(projection1.canMap(point1));
        // North pole
        Assert.assertFalse(projection1.canMap(point2));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#MERCATOR_PROJECTION}
     * 
     * @testedMethod {@link Mercator#applyTo(EllipsoidPoint)}
     * @testedMethod {@link Mercator#applyTo(double, double)}
     * 
     * @description test application of the projection (Toulouse, North pole, 2 different projections)
     * 
     * @input ellipsoid, projection
     * 
     * @output projected point
     * 
     * @testPassCriteria result is identical to reference (reference : LibKernel library 10.0.0, tolerance: 0)
     *                   result has slightly changed since 4.12 due to numerical conversion with the new coordinates
     *                   system
     * 
     * @referenceVersion 4.12
     * 
     * @nonRegressionVersion 4.12
     */
    @Test
    public void testApplyTo() throws PatriusException {
        Report.printMethodHeader("testApplyTo", "Apply projection", "LibKernel 10.0.0", 0., ComparisonType.RELATIVE);

        // Main algorithm (Toulouse)
        final Vector2D actual = projection1.applyTo(point1);
        final Vector2D actualbis = projection1.applyTo(point1.getLLHCoordinates().getLatitude(), point1
            .getLLHCoordinates().getLongitude());

        final double eps = 0.;
        final double expectedX = 166727.87010053065;
        final double expectedY = 5348402.564373465;

        Assert.assertEquals(expectedX, actual.getX(), eps);
        Assert.assertEquals(expectedY, actual.getY(), eps);

        Assert.assertEquals(expectedX, actualbis.getX(), eps);
        Assert.assertEquals(expectedY, actualbis.getY(), eps);
        Report.printToReport("Latitude (Toulouse, classic proj.)", expectedX, actualbis.getX());
        Report.printToReport("Longitude (Toulouse, classic proj.)", expectedY, actualbis.getY());

        // Other cases (North pole)
        final Vector2D actual2 = projection1.applyTo(point2.getLLHCoordinates().getLatitude(), point2
            .getLLHCoordinates().getLongitude());
        final double expectedX2 = 166727.8701005306;
        final double expectedY2 = 7.425695006522633E7;

        Assert.assertEquals(expectedX2, actual2.getX(), eps);
        Assert.assertEquals(expectedY2, actual2.getY(), eps);
        Report.printToReport("Latitude (North pole, classic proj.)", expectedX2, actual2.getX());
        Report.printToReport("Longitude (North pole, classic proj.)", expectedY2, actual2.getY());

        // Specific projection (Toulouse)
        final Vector2D actual3 = projection2.applyTo(point1.getLLHCoordinates().getLatitude(), point1
            .getLLHCoordinates().getLongitude());
        final double expectedX3 = -3217015.7348985383;
        final double expectedY3 = 3587210.7558377897;

        Assert.assertEquals(expectedX3, actual3.getX(), eps);
        Assert.assertEquals(expectedY3, actual3.getY(), eps);
        Report.printToReport("Latitude (Toulouse, specific proj.)", expectedX3, actual3.getX());
        Report.printToReport("Longitude (Toulouse, specific proj.)", expectedY3, actual3.getY());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#MERCATOR_PROJECTION}
     * 
     * @testedMethod {@link Mercator#applyInverseTo(EllipsoidPoint)}
     * @testedMethod {@link Mercator#applyInverseTo(double, double)}
     * 
     * @description test inverse application of the projection (Toulouse, 2 different projections)
     * 
     * @input ellipsoid, projection
     * 
     * @output geodetic coordinates
     * 
     * @testPassCriteria result is identical to reference (reference : LibKernel library 10.0.0, tolerance: 1E-15)
     *                   result has slightly changed since 4.12 due to numerical conversion with the new coordinates
     *                   system
     * 
     * @referenceVersion 4.12
     * 
     * @nonRegressionVersion 4.12
     */
    @Test
    public void testApplyInverseTo() throws PatriusException {
        Report.printMethodHeader("testApplyInverseTo", "Apply inverse projection", "LibKernel 10.0.0", 0.,
            ComparisonType.RELATIVE);

        // Main algorithm (Toulouse)
        final EllipsoidPoint actual = projection1.applyInverseTo(166727.87010052995, 5348402.564373465, point1
            .getLLHCoordinates().getHeight());
        final EllipsoidPoint actualbis = projection1.applyInverseTo(166727.87010052995, 5348402.564373465);
        final EllipsoidPoint actualter = projection1.applyInverseTo(166727.87010052995, 1.5 * 6378000);

        final double eps = 1E-15;
        final double eps2 = 1E-10;
        final EllipsoidPoint expected1 = new EllipsoidPoint(actual.getBodyShape(), actual.getBodyShape()
            .getLLHCoordinatesSystem(), MathLib.toRadians(43.43087666002356), MathLib.toRadians(1.4977419400000025),
            256., "");
        final EllipsoidPoint expectedbis = new EllipsoidPoint(actualbis.getBodyShape(),
            actualbis.getBodyShape().getLLHCoordinatesSystem(), MathLib.toRadians(43.43087666002356),
            MathLib.toRadians(1.4977419400000025), 0., "");
        final EllipsoidPoint expectedter = new EllipsoidPoint(actualter.getBodyShape(),
            actualter.getBodyShape().getLLHCoordinatesSystem(), MathLib.toRadians(64.9901036924673),
            MathLib.toRadians(1.4977419400000025), 0., "");

        Assert.assertEquals(expected1.getLLHCoordinates().getLatitude(), actual.getLLHCoordinates().getLatitude(), eps);
        Assert.assertEquals(expected1.getLLHCoordinates().getLongitude(), actual.getLLHCoordinates().getLongitude(),
            eps);
        Assert.assertEquals(expected1.getLLHCoordinates().getHeight(), actual.getLLHCoordinates().getHeight(), eps2);

        Assert.assertEquals(expectedbis.getLLHCoordinates().getLatitude(), actualbis.getLLHCoordinates().getLatitude(),
            eps);
        Assert.assertEquals(expectedbis.getLLHCoordinates().getLongitude(), actualbis.getLLHCoordinates()
            .getLongitude(), eps);
        Assert
            .assertEquals(expectedbis.getLLHCoordinates().getHeight(), actualbis.getLLHCoordinates().getHeight(), eps);
        Report.printToReport("Latitude (Toulouse, classic proj.)", expectedbis.getLLHCoordinates().getLatitude(),
            actualbis.getLLHCoordinates().getLatitude());
        Report.printToReport("Longitude (Toulouse, classic proj.)", expectedbis.getLLHCoordinates().getLongitude(),
            actualbis.getLLHCoordinates().getLongitude());
        Report.printToReport("Altitude (Toulouse, classic proj.)", expectedbis.getLLHCoordinates().getHeight(),
            actualbis.getLLHCoordinates().getHeight());

        Assert.assertEquals(expectedter.getLLHCoordinates().getLatitude(), actualter.getLLHCoordinates().getLatitude(),
            eps);
        Assert.assertEquals(expectedter.getLLHCoordinates().getLongitude(), actualter.getLLHCoordinates()
            .getLongitude(), eps);
        Assert
            .assertEquals(expectedter.getLLHCoordinates().getHeight(), actualter.getLLHCoordinates().getHeight(), eps);

        // Specific projection (Toulouse)
        final EllipsoidPoint actual2 = projection2.applyInverseTo(-3217015.7348985393, 3587210.75583779);
        final EllipsoidPoint expected2 = new EllipsoidPoint(actual2.getBodyShape(), actual2.getBodyShape()
            .getLLHCoordinatesSystem(), MathLib.toRadians(43.43087666000002), MathLib.toRadians(1.4977419400000034),
            0., "");

        Assert
            .assertEquals(expected2.getLLHCoordinates().getLatitude(), actual2.getLLHCoordinates().getLatitude(), eps);
        Assert.assertEquals(expected2.getLLHCoordinates().getLongitude(), actual2.getLLHCoordinates().getLongitude(),
            eps);
        Assert.assertEquals(expected2.getLLHCoordinates().getHeight(), actual2.getLLHCoordinates().getHeight(), eps);
        Report.printToReport("Latitude (Toulouse, specific proj.)", expected2.getLLHCoordinates().getLatitude(),
            actual2.getLLHCoordinates().getLatitude());
        Report.printToReport("Longitude (Toulouse, specific proj.)", expected2.getLLHCoordinates().getLongitude(),
            actual2.getLLHCoordinates().getLongitude());
        Report.printToReport("Altitude (Toulouse, specific proj.)", expected2.getLLHCoordinates().getHeight(), actual2
            .getLLHCoordinates().getHeight());

        // Test exception
        projection1.applyInverseTo(166727.87010052995, 7.425695006522633E7 - 1, point1.getLLHCoordinates().getHeight());
        Assert.assertTrue(true);
        try {
            projection1.applyInverseTo(166727.87010052995, 7.425695006522633E7 + 1, point1.getLLHCoordinates()
                .getHeight());
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }

        projection1.applyInverseTo(2.0037508342789244E7 - 1, 5348402.564373465, point1.getLLHCoordinates().getHeight());
        Assert.assertTrue(true);
        try {
            projection1.applyInverseTo(2.0037508342789244E7 + 1, 5348402.564373465, point1.getLLHCoordinates()
                .getHeight());
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#MERCATOR_PROJECTION}
     * 
     * @testedMethod {@link Mercator#isConformal()}
     * 
     * @description Check the Mercator projection is conformal
     * 
     * @input ellipsoid, projection
     * 
     * @output boolean
     * 
     * @testPassCriteria true (reference : LibKernel library 10.0.0)
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testIsConformal() {
        Assert.assertTrue(projection1.isConformal());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#MERCATOR_PROJECTION}
     * 
     * @testedMethod {@link Mercator#isEquivalent()}
     * 
     * @description Check the Mercator projection is not equivalent
     * 
     * @input ellipsoid, projection
     * 
     * @output boolean
     * 
     * @testPassCriteria false (reference : LibKernel library 10.0.0)
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testIsEquivalent() {
        Assert.assertFalse(projection1.isEquivalent());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#MERCATOR_PROJECTION}
     * 
     * @testedMethod {@link Mercator#getLineProperty()}
     * 
     * @description Check the projection line property is straight rhumb line
     * 
     * @input ellipsoid, projection
     * 
     * @output line property
     * 
     * @testPassCriteria line property is straight rhumb line (reference : LibKernel library 10.0.0)
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testLineProperty() {
        Assert.assertEquals(projection1.getLineProperty(), EnumLineProperty.STRAIGHT_RHUMB_LINE);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#MERCATOR_PROJECTION}
     * 
     * @testedMethod {@link Mercator#getMaximumLatitude()}
     * 
     * @description Check the Mercator projection maximum latitude
     * 
     * @input ellipsoid, projection
     * 
     * @output maximum latitude
     * 
     * @testPassCriteria result is identical to reference (reference : LibKernel library 10.0.0, tolerance: 0)
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testMaximumLatitude() {
        Report.printMethodHeader("testMaximumLatitude", "Maximum latitude", "LibKernel 10.0.0", 0.,
            ComparisonType.RELATIVE);
        Assert.assertEquals(1.5707788735023767, projection1.getMaximumLatitude(), 0.);
        Report.printToReport("Latitude", 1.5707788735023767, projection1.getMaximumLatitude());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#MERCATOR_PROJECTION}
     * 
     * @testedMethod {@link Mercator#getMaximumEastingValue()}
     * 
     * @description Check the Mercator projection maximum easting value
     * 
     * @input ellipsoid, projection
     * 
     * @output maximum easting value
     * 
     * @testPassCriteria result is identical to reference (reference : LibKernel library 10.0.0, tolerance: 0)
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testMaximumEastingValue() {
        Report.printMethodHeader("testMaximumEastingValue", "Maximum easting value", "LibKernel 10.0.0", 0.,
            ComparisonType.RELATIVE);
        Assert.assertEquals(2.0037508342789244E7, projection1.getMaximumEastingValue(), 0.);
        Report.printToReport("Easting value", 2.0037508342789244E7, projection1.getMaximumEastingValue());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#MERCATOR_PROJECTION}
     * 
     * @testedMethod {@link Mercator#getAzimuth()}
     * 
     * @description Check the Mercator projection azimuth value
     * 
     * @input ellipsoid, projection
     * 
     * @output azimuth value
     * 
     * @testPassCriteria result is identical to reference (reference : LibKernel library 10.0.0, tolerance: 0)
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testAzimuth() {
        Report.printMethodHeader("testAzimuth", "Azimuth", "LibKernel 10.0.0", 0., ComparisonType.RELATIVE);
        Assert.assertEquals(MathLib.toRadians(28.64788975654116), projection2.getAzimuth(), 0.);
        Report.printToReport("Azimuth", MathLib.toRadians(28.64788975654116), projection2.getAzimuth());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#MERCATOR_PROJECTION}
     * 
     * @testedMethod {@link Mercator#getMaximumNorthingValue()}
     * 
     * @description Check the Mercator projection maximum northing value
     * 
     * @input ellipsoid, projection
     * 
     * @output maximum northing value
     * 
     * @testPassCriteria result is identical to reference (reference : LibKernel library 10.0.0, tolerance: 0)
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testMaximumNorthingValue() {
        Report.printMethodHeader("testMaximumNorthingValue", "Maximum northing value", "LibKernel 10.0.0", 0.,
            ComparisonType.RELATIVE);
        Assert.assertEquals(7.425695006522633E7, projection1.getMaximumNorthingValue(), 0.);
        Report.printToReport("Northing value", 7.425695006522633E7, projection1.getMaximumNorthingValue());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#MERCATOR_PROJECTION}
     * 
     * @testedMethod {@link Mercator#getScaleFactor(double)}
     * 
     * @description Check the Mercator scale factor
     * 
     * @input ellipsoid, projection
     * 
     * @output scale factor
     * 
     * @testPassCriteria result is identical to reference (reference : LibKernel library 10.0.0, tolerance: 0)
     * 
     * @referenceVersion 4.12
     * 
     * @nonRegressionVersion 4.12
     */
    @Test
    public void testScaleFactor() {
        Report.printMethodHeader("testScaleFactor", "Scale factor", "LibKernel 10.0.0", 0., ComparisonType.RELATIVE);
        final double expected = 0.7273558637126116;
        final double actual = projection1.getScaleFactor(point1.getLLHCoordinates().getLatitude());
        Assert.assertEquals(expected, actual, 0.);
        Report.printToReport("Scale factor", expected, actual);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#MERCATOR_PROJECTION}
     * 
     * @testedMethod {@link Mercator#getDistortionFactor(double)}
     * 
     * @description Check the Mercator distorsion factor
     * 
     * @input ellipsoid, projection
     * 
     * @output distorsion factor
     * 
     * @testPassCriteria result is identical to reference (reference : LibKernel library 10.0.0, tolerance: 0)
     * 
     * @referenceVersion 4.12
     * 
     * @nonRegressionVersion 4.12
     */
    @Test
    public void testDistorsionFactor() {
        Report.printMethodHeader("testDistorsionFactor", "Distorsion factor", "LibKernel 10.0.0", 0.,
            ComparisonType.RELATIVE);
        final double expected = 1.3748428381339257;
        final double actual = projection1.getDistortionFactor(point1.getLLHCoordinates().getLatitude());
        Assert.assertEquals(expected, actual, 0.);
        Report.printToReport("Distorsion factor", expected, actual);
    }
}
