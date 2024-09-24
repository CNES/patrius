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

/**
 * Test class for {@link IdentityProjection}.
 * 
 * @author Emmanuel Bignon
 * @version $Id$
 * 
 */
public class IdentityProjectionTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Validate the Identity projection
         * 
         * @featureDescription Validate the Identity projection
         * 
         */
        IDENTITY_PROJECTION
    }

    /** Projection used in the tests. */
    private static IdentityProjection projection;

    /** Point used in the tests. */
    private static EllipsoidPoint point;

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(IdentityProjectionTest.class.getSimpleName(), "Identity projection");

        // Projection
        final EllipsoidBodyShape ellipsoid = new OneAxisEllipsoid(6378137.0,
            1. / 298.257223563, null,
            "default earth");
        projection = new IdentityProjection(ellipsoid);

        // Point
        point = new EllipsoidPoint(ellipsoid, ellipsoid.getLLHCoordinatesSystem(), 0.758011794744558,
            0.0261405281982074, 256., "");
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#IDENTITY_PROJECTION}
     * 
     * @testedMethod {@link IdentityProjection#canMap(EllipsoidPoint)}
     * 
     * @description Check the identity projection can map any point
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
    public void testCanMap() {
        Assert.assertTrue(projection.canMap(point));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#IDENTITY_PROJECTION}
     * 
     * @testedMethod {@link IdentityProjection#applyTo(EllipsoidPoint)}
     * @testedMethod {@link IdentityProjection#applyTo(double, double)}
     * 
     * @description test application of the projection
     * 
     * @input ellipsoid, projection
     * 
     * @output projected point
     * 
     * @testPassCriteria result is identical to reference (reference : LibKernel library 10.0.0, tolerance: 0)
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testApplyTo() {
        Report.printMethodHeader("testApplyTo", "Apply projection", "LibKernel 10.0.0", 0., ComparisonType.RELATIVE);

        final Vector2D actual = projection.applyTo(point);
        final Vector2D actual2 = projection.applyTo(point.getLLHCoordinates().getLatitude(), point.getLLHCoordinates()
            .getLongitude());

        final double eps = 0.;
        final EllipsoidPoint expected = new EllipsoidPoint(point.getBodyShape(), point.getBodyShape()
            .getLLHCoordinatesSystem(), 0.758011794744558, 0.0261405281982074, 256., "");

        Assert.assertEquals(expected.getLLHCoordinates().getLatitude(), actual.getX(), eps);
        Assert.assertEquals(expected.getLLHCoordinates().getLongitude(), actual.getY(), eps);
        Report.printToReport("Latitude", expected.getLLHCoordinates().getLatitude(), actual.getX());
        Report.printToReport("Longitude", expected.getLLHCoordinates().getLongitude(), actual.getY());

        Assert.assertEquals(expected.getLLHCoordinates().getLatitude(), actual2.getX(), eps);
        Assert.assertEquals(expected.getLLHCoordinates().getLongitude(), actual2.getY(), eps);
        Report.printToReport("Latitude", expected.getLLHCoordinates().getLatitude(), actual2.getX());
        Report.printToReport("Longitude", expected.getLLHCoordinates().getLongitude(), actual2.getY());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#IDENTITY_PROJECTION}
     * 
     * @testedMethod {@link IdentityProjection#applyInverseTo(EllipsoidPoint)}
     * @testedMethod {@link IdentityProjection#applyInverseTo(double, double)}
     * 
     * @description test inverse application of the projection
     * 
     * @input ellipsoid, projection
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
    public void testApplyInverseTo() {
        Report.printMethodHeader("testApplyInverseTo", "Apply inverse projection", "LibKernel 10.0.0", 0.,
            ComparisonType.RELATIVE);

        final EllipsoidPoint actual = projection.applyInverseTo(point.getLLHCoordinates().getLatitude(), point
            .getLLHCoordinates().getLongitude(), point.getLLHCoordinates().getHeight());
        final EllipsoidPoint actual2 = projection.applyInverseTo(point.getLLHCoordinates().getLatitude(), point
            .getLLHCoordinates().getLongitude());

        final double eps = 1e-14;
        final double eps2 = 1e-9;
        final EllipsoidPoint expected1 = new EllipsoidPoint(actual.getBodyShape(), actual.getBodyShape()
            .getLLHCoordinatesSystem(), 0.758011794744558, 0.0261405281982074, 256., "");
        final EllipsoidPoint expected2 = new EllipsoidPoint(actual2.getBodyShape(), actual.getBodyShape()
            .getLLHCoordinatesSystem(), 0.758011794744558, 0.0261405281982074, 0., "");

        Assert.assertEquals(expected1.getLLHCoordinates().getLatitude(), actual.getLLHCoordinates().getLatitude(), eps);
        Assert.assertEquals(expected1.getLLHCoordinates().getLongitude(), actual.getLLHCoordinates().getLongitude(),
            eps);
        Assert.assertEquals(expected1.getLLHCoordinates().getHeight(), actual.getLLHCoordinates().getHeight(), eps2);
        Report.printToReport("Latitude", expected1.getLLHCoordinates().getLatitude(), actual.getLLHCoordinates()
            .getLatitude());
        Report.printToReport("Longitude", expected1.getLLHCoordinates().getLongitude(), actual.getLLHCoordinates()
            .getLongitude());
        Report.printToReport("Altitude", expected1.getLLHCoordinates().getHeight(), actual.getLLHCoordinates()
            .getHeight());

        Assert
            .assertEquals(expected2.getLLHCoordinates().getLatitude(), actual2.getLLHCoordinates().getLatitude(), eps);
        Assert.assertEquals(expected2.getLLHCoordinates().getLongitude(), actual2.getLLHCoordinates().getLongitude(),
            eps);
        Assert.assertEquals(expected2.getLLHCoordinates().getHeight(), actual2.getLLHCoordinates().getHeight(), eps);
        Report.printToReport("Latitude", expected2.getLLHCoordinates().getLatitude(), actual2.getLLHCoordinates()
            .getLatitude());
        Report.printToReport("Longitude", expected2.getLLHCoordinates().getLongitude(), actual2.getLLHCoordinates()
            .getLongitude());
        Report.printToReport("Altitude", expected2.getLLHCoordinates().getHeight(), actual2.getLLHCoordinates()
            .getHeight());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#IDENTITY_PROJECTION}
     * 
     * @testedMethod {@link IdentityProjection#isConformal()}
     * 
     * @description Check the identity projection is not conformal
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
        Assert.assertFalse(projection.isConformal());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#IDENTITY_PROJECTION}
     * 
     * @testedMethod {@link IdentityProjection#isEquivalent()}
     * 
     * @description Check the identity projection is not equivalent
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
        Assert.assertFalse(projection.isEquivalent());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#IDENTITY_PROJECTION}
     * 
     * @testedMethod {@link IdentityProjection#getLineProperty()}
     * 
     * @description Check the identity projection line property is straight
     * 
     * @input ellipsoid, projection
     * 
     * @output line property
     * 
     * @testPassCriteria line property is straight (reference : LibKernel library 10.0.0)
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testLineProperty() {
        Assert.assertEquals(projection.getLineProperty(), EnumLineProperty.STRAIGHT);
        Assert.assertEquals(projection.getLineProperty().getName(), EnumLineProperty.STRAIGHT.getName());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#IDENTITY_PROJECTION}
     * 
     * @testedMethod {@link IdentityProjection#getMaximumLatitude()}
     * 
     * @description Check the identity projection maximum latitude
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
        Assert.assertEquals(1.5707963267948966, projection.getMaximumLatitude(), 0.);
        Report.printToReport("Latitude", 1.5707963267948966, projection.getMaximumLatitude());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#IDENTITY_PROJECTION}
     * 
     * @testedMethod {@link IdentityProjection#getMaximumEastingValue()}
     * 
     * @description Check the identity projection maximum easting value
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
        Assert.assertEquals(0., projection.getMaximumEastingValue(), 0.);
        Report.printToReport("Easting value", 0., projection.getMaximumEastingValue());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#IDENTITY_PROJECTION}
     * 
     * @testedMethod {@link IdentityProjection#getDistortionFactor(double)}
     * 
     * @description Check the distorsion factor cannot be computed
     * 
     * @input ellipsoid, projection
     * 
     * @output exception
     * 
     * @testPassCriteria exception is thrown (reference : LibKernel library 10.0.0)
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test(expected = IllegalArgumentException.class)
    public void testDistorsionFactor() {
        projection.getDistortionFactor(point.getLLHCoordinates().getLatitude());
    }
}
