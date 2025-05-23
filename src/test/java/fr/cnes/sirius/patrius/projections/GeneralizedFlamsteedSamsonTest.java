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
 * Test class for {@link GeneralizedFlamsteedSamson}.
 * 
 * @author Emmanuel Bignon
 * @version $Id$
 * 
 */
public class GeneralizedFlamsteedSamsonTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Validate the Generalized Flamsteed-Samson projection
         * 
         * @featureDescription Validate the Generalized Flamsteed-Samson projection
         * 
         * @coveredRequirements DV-CARTO_20
         */
        GENERALIZED_FLAMSTEEDSAMSON_PROJECTION
    }

    /** Projection used in the tests (simple classic projection). */
    private static GeneralizedFlamsteedSamson projection1;

    /** Projection used in the tests (centered, no series and azimuth != 0). */
    private static GeneralizedFlamsteedSamson projection2;

    /** Ellipsoid point used in the tests (Toulouse). */
    private static EllipsoidPoint point1;

    /** Ellipsoid point used in the tests (North pole). */
    private static EllipsoidPoint point2;

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(GeneralizedFlamsteedSamsonTest.class.getSimpleName(),
            "Generalized Flamsteed-Samson projection");

        // Projections
        final EllipsoidBodyShape ellipsoid = new OneAxisEllipsoid(6378137.0, 1. / 298.257223563, null, "default earth");
        projection1 = new GeneralizedFlamsteedSamson(new EllipsoidPoint(ellipsoid, ellipsoid.getLLHCoordinatesSystem(),
            0., 0., 0., ""), 0.);
        projection2 = new GeneralizedFlamsteedSamson(new EllipsoidPoint(ellipsoid, ellipsoid.getLLHCoordinatesSystem(),
            0.1, 0.2, 0.3, ""), 0.5);

        // Point 1 (Toulouse)
        point1 = new EllipsoidPoint(ellipsoid, ellipsoid.getLLHCoordinatesSystem(), 0.758011794744558,
            0.0261405281982074, 256., "");
        // Point 2 (North pole)
        point2 = new EllipsoidPoint(ellipsoid, ellipsoid.getLLHCoordinatesSystem(), FastMath.PI / 2., 0.0261405281982074,
            256., "");
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#GENERALIZED_FLAMSTEEDSAMSON_PROJECTION}
     * 
     * @testedMethod {@link GeneralizedFlamsteedSamson#canMap(EllipsoidPoint)}
     * 
     * @description Check the Generalized Flamsteed-Samson projection can map provided point
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
     * @testedFeature {@link features#GENERALIZED_FLAMSTEEDSAMSON_PROJECTION}
     * 
     * @testedMethod {@link GeneralizedFlamsteedSamson#applyTo(EllipsoidPoint)}
     * @testedMethod {@link GeneralizedFlamsteedSamson#applyTo(double, double)}
     * 
     * @description test application of the projection (Toulouse, 2 different projections)
     * 
     * @input ellipsoid, projection
     * 
     * @output projected point
     * 
     * @testPassCriteria result is identical to reference (reference : LibKernel library 10.0.0, tolerance: 3E-15)
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testApplyTo() throws PatriusException {
        Report.printMethodHeader("testApplyTo", "Apply projection", "LibKernel 10.0.0", 0., ComparisonType.RELATIVE);

        // Main algorithm (Toulouse)
        final Vector2D actual = projection1.applyTo(point1);
        final Vector2D actualbis = projection1.applyTo(point1.getLLHCoordinates().getLatitude(), point1
            .getLLHCoordinates().getLongitude());

        final double eps = 3E-15;
        final double expectedX = 121270.4939619356;
        final double expectedY = 4810588.960625679;

        Assert.assertEquals(expectedX, actual.getX(), eps);
        Assert.assertEquals(expectedY, actual.getY(), eps);

        Assert.assertEquals(expectedX, actualbis.getX(), eps);
        Assert.assertEquals(expectedY, actualbis.getY(), eps);
        Report.printToReport("Latitude (Toulouse, classic proj.)", expectedX, actualbis.getX());
        Report.printToReport("Longitude (Toulouse, classic proj.)", expectedY, actualbis.getY());

        // Specific projection (Toulouse)
        final Vector2D actual3 = projection2.applyTo(point1.getLLHCoordinates().getLatitude(), point1
            .getLLHCoordinates().getLongitude());
        final double expectedX3 = -2544416.4212971386;
        final double expectedY3 = 3369672.300353526;

        Assert.assertEquals(0., (expectedX3 - actual3.getX()) / expectedX3, eps);
        Assert.assertEquals(0., (expectedY3 - actual3.getY()) / expectedY3, eps);
        Report.printToReport("Latitude (Toulouse, specific proj.)", expectedX3, actual3.getX());
        Report.printToReport("Longitude (Toulouse, specific proj.)", expectedY3, actual3.getY());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#GENERALIZED_FLAMSTEEDSAMSON_PROJECTION}
     * 
     * @testedMethod {@link GeneralizedFlamsteedSamson#applyInverseTo(EllipsoidPoint)}
     * @testedMethod {@link GeneralizedFlamsteedSamson#applyInverseTo(double, double)}
     * 
     * @description test inverse application of the projection (Toulouse, 2 different projections)
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
        Report.printMethodHeader("testApplyInverseTo", "Apply inverse projection", "LibKernel 10.0.0", 0.,
            ComparisonType.RELATIVE);

        // Main algorithm (Toulouse)
        final EllipsoidPoint actual = projection1.applyInverseTo(121270.4939619351, 4810588.960625679,
            point1.getLLHCoordinates().getHeight());
        final EllipsoidPoint actualbis = projection1.applyInverseTo(121270.4939619351, 4810588.960625679);

        final double eps = 1E-15;
        final EllipsoidPoint expected1 = new EllipsoidPoint(actual.getBodyShape(), actual.getBodyShape()
            .getLLHCoordinatesSystem(), MathLib.toRadians(43.430876660986925), MathLib.toRadians(1.497741940024339),
            0., "");
        final EllipsoidPoint expectedbis = new EllipsoidPoint(actualbis.getBodyShape(),
            actual.getBodyShape().getLLHCoordinatesSystem(), MathLib.toRadians(43.430876660986925),
            MathLib.toRadians(1.497741940024339), 0., "");

        Assert.assertEquals(expected1.getLLHCoordinates().getLatitude(), actual.getLLHCoordinates().getLatitude(), eps);
        Assert.assertEquals(expected1.getLLHCoordinates().getLongitude(), actual.getLLHCoordinates().getLongitude(),
            eps);
        Assert.assertEquals(expected1.getLLHCoordinates().getHeight(), actual.getLLHCoordinates().getHeight(), eps);

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

        // Specific projection (Toulouse)
        final EllipsoidPoint actual2 = projection2.applyInverseTo(-2544416.4212971386, 3369672.300353526);
        final EllipsoidPoint expected2 = new EllipsoidPoint(actual2.getBodyShape(), actual.getBodyShape()
            .getLLHCoordinatesSystem(), MathLib.toRadians(43.430876660681406), MathLib.toRadians(1.4977419374305494),
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
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#GENERALIZED_FLAMSTEEDSAMSON_PROJECTION}
     * 
     * @testedMethod {@link GeneralizedFlamsteedSamson#isConformal()}
     * 
     * @description Check the Generalized Flamsteed-Samson projection is not conformal
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
    public void testIsConformal() {
        Assert.assertFalse(projection1.isConformal());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#GENERALIZED_FLAMSTEEDSAMSON_PROJECTION}
     * 
     * @testedMethod {@link GeneralizedFlamsteedSamson#isEquivalent()}
     * 
     * @description Check the Generalized Flamsteed-Samson projection is equivalent
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
    public void testIsEquivalent() {
        Assert.assertTrue(projection1.isEquivalent());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#GENERALIZED_FLAMSTEEDSAMSON_PROJECTION}
     * 
     * @testedMethod {@link GeneralizedFlamsteedSamson#getLineProperty()}
     * 
     * @description Check the projection line property is none
     * 
     * @input ellipsoid, projection
     * 
     * @output line property
     * 
     * @testPassCriteria line property is none (reference : LibKernel library 10.0.0)
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testLineProperty() {
        Assert.assertEquals(projection1.getLineProperty(), EnumLineProperty.NONE);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#GENERALIZED_FLAMSTEEDSAMSON_PROJECTION}
     * 
     * @testedMethod {@link GeneralizedFlamsteedSamson#getMaximumLatitude()}
     * 
     * @description Check the Generalized Flamsteed-Samson projection maximum latitude
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
     * @testedFeature {@link features#GENERALIZED_FLAMSTEEDSAMSON_PROJECTION}
     * 
     * @testedMethod {@link GeneralizedFlamsteedSamson#getMaximumEastingValue()}
     * 
     * @description Check the Generalized Flamsteed-Samson projection maximum easting value
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
     * @testedFeature {@link features#GENERALIZED_FLAMSTEEDSAMSON_PROJECTION}
     * 
     * @testedMethod {@link GeneralizedFlamsteedSamson#getAzimuth()}
     * 
     * @description Check the Generalized Flamsteed-Samson projection azimuth value
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
     * @testedFeature {@link features#GENERALIZED_FLAMSTEEDSAMSON_PROJECTION}
     * 
     * @testedMethod {@link GeneralizedFlamsteedSamson#getMaximumNorthingValue()}
     * 
     * @description Check the Generalized Flamsteed-Samson projection maximum northing value
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
     * @testedFeature {@link features#GENERALIZED_FLAMSTEEDSAMSON_PROJECTION}
     * 
     * @testedMethod {@link GeneralizedFlamsteedSamson#getScaleFactor(double)}
     * 
     * @description Check the Generalized Flamsteed-Samson scale factor
     * 
     * @input ellipsoid, projection
     * 
     * @output scale factor
     * 
     * @testPassCriteria result is identical to reference (reference : LibKernel library 10.0.0, tolerance: 0)
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
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
     * @testedFeature {@link features#GENERALIZED_FLAMSTEEDSAMSON_PROJECTION}
     * 
     * @testedMethod {@link GeneralizedFlamsteedSamson#getDistortionFactor(double)}
     * 
     * @description Check the Generalized Flamsteed-Samson distorsion factor
     * 
     * @input ellipsoid, projection
     * 
     * @output distorsion factor
     * 
     * @testPassCriteria result is identical to reference (reference : LibKernel library 10.0.0, tolerance: 0)
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
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
