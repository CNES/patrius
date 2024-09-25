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
 * VERSION:4.13:DM:DM-132:08/12/2023:[PATRIUS] Suppression de la possibilite
 * de convertir les sorties de VacuumSignalPropagation
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:524:10/03/2016:serialization test
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies;

import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.SphericalCoordinates;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class for class {@link EllipsoidPoint}.
 */
public class EllipsoidPointTest {

    private static final double TOLERANCE_GEODETIC_POINT = 1E-8;
    private static final double TOLERANCE_ANGLE = 1E-12;

    @Test
    public void test_EllipsoidBodyPoint() {

        final EllipsoidBodyShape earthShape = new OneAxisEllipsoid(Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS, 0.,
            FramesFactory.getGCRF());

        // geodetic coordinates
        final double longitude = 2;
        final double latitude = MathLib.toRadians(65);
        final double altitude = 455;

        // Build a point (ELLIPSODETIC convention)
        final EllipsoidPoint point0 = new EllipsoidPoint(earthShape, earthShape.getLLHCoordinatesSystem(), latitude,
            longitude, altitude, "");

        // Assertions
        Assert.assertEquals(latitude, point0.getLLHCoordinates().getLatitude(), 1e-12);
        Assert.assertEquals(longitude, point0.getLLHCoordinates().getLongitude(), 0.);
        Assert.assertEquals(altitude, point0.getLLHCoordinates().getHeight(), 1.2e-9);

        /* Test the ELLIPSODETIC convention */

        final Vector3D position0 = point0.getPosition();

        // build an ellipsoid body point using this position
        final EllipsoidPoint point1 = new EllipsoidPoint(earthShape, position0, false, "point1");

        // compute its LLH coordinates with the GEODETIC convention
        LLHCoordinatesSystem convention = LLHCoordinatesSystem.ELLIPSODETIC;

        // Assertions
        Assert.assertEquals(latitude, point1.getLLHCoordinates(convention).getLatitude(), TOLERANCE_ANGLE);
        Assert.assertEquals(longitude, point1.getLLHCoordinates(convention).getLongitude(), 0.);
        Assert.assertEquals(altitude, point1.getLLHCoordinates(convention).getHeight(), TOLERANCE_GEODETIC_POINT);
        Assert.assertEquals(position0.getX(), point1.getPosition().getX(), TOLERANCE_GEODETIC_POINT);
        Assert.assertEquals(position0.getY(), point1.getPosition().getY(), TOLERANCE_GEODETIC_POINT);
        Assert.assertEquals(position0.getZ(), point1.getPosition().getZ(), TOLERANCE_GEODETIC_POINT);
        Assert.assertEquals(point0.getNormal().getX(), point1.getNormal().getX(), TOLERANCE_GEODETIC_POINT);
        Assert.assertEquals(point0.getNormal().getY(), point1.getNormal().getY(), TOLERANCE_GEODETIC_POINT);
        Assert.assertEquals(point0.getNormal().getZ(), point1.getNormal().getZ(), TOLERANCE_GEODETIC_POINT);

        /* Test the BODYCENTRIC_RADIAL convention */

        // compute its LLH coordinates with the BODYCENTRIC_RADIAL_CONVENTION
        convention = LLHCoordinatesSystem.BODYCENTRIC_RADIAL;
        final LLHCoordinates coord2 = point0.getLLHCoordinates(convention);

        // build an ellipsoid body point with these coordinates
        final EllipsoidPoint point2 = new EllipsoidPoint(earthShape, coord2, "point2");

        // Assertions
        Assert.assertEquals(latitude, point2.getLLHCoordinates(convention).getLatitude(), TOLERANCE_ANGLE);
        Assert.assertEquals(longitude, point2.getLLHCoordinates(convention).getLongitude(), 0.);
        Assert.assertEquals(altitude, point2.getLLHCoordinates(convention).getHeight(), TOLERANCE_GEODETIC_POINT);
        Assert.assertEquals(position0.getX(), point2.getPosition().getX(), TOLERANCE_GEODETIC_POINT);
        Assert.assertEquals(position0.getY(), point2.getPosition().getY(), TOLERANCE_GEODETIC_POINT);
        Assert.assertEquals(position0.getZ(), point2.getPosition().getZ(), TOLERANCE_GEODETIC_POINT);
        Assert.assertEquals(point0.getNormal().getX(), point2.getNormal().getX(), TOLERANCE_GEODETIC_POINT);
        Assert.assertEquals(point0.getNormal().getY(), point2.getNormal().getY(), TOLERANCE_GEODETIC_POINT);
        Assert.assertEquals(point0.getNormal().getZ(), point2.getNormal().getZ(), TOLERANCE_GEODETIC_POINT);

        /* Test the BODYCENTRIC_NORMAL convention */

        // compute its LLH coordinates with the BODYCENTRIC_RADIAL_CONVENTION
        convention = LLHCoordinatesSystem.BODYCENTRIC_NORMAL;
        final LLHCoordinates coord3 = point0.getLLHCoordinates(convention);

        // build an ellipsoid body point with these coordinates
        final EllipsoidPoint point3 = new EllipsoidPoint(earthShape, coord3, "point3");

        // Assertions
        Assert.assertEquals(latitude, point3.getLLHCoordinates(convention).getLatitude(), TOLERANCE_ANGLE);
        Assert.assertEquals(longitude, point3.getLLHCoordinates(convention).getLongitude(), 0.);
        Assert.assertEquals(altitude, point3.getLLHCoordinates(convention).getHeight(), TOLERANCE_GEODETIC_POINT);
        Assert.assertEquals(position0.getX(), point3.getPosition().getX(), TOLERANCE_GEODETIC_POINT);
        Assert.assertEquals(position0.getY(), point3.getPosition().getY(), TOLERANCE_GEODETIC_POINT);
        Assert.assertEquals(position0.getZ(), point3.getPosition().getZ(), TOLERANCE_GEODETIC_POINT);
        Assert.assertEquals(point0.getNormal().getX(), point3.getNormal().getX(), TOLERANCE_GEODETIC_POINT);
        Assert.assertEquals(point0.getNormal().getY(), point3.getNormal().getY(), TOLERANCE_GEODETIC_POINT);
        Assert.assertEquals(point0.getNormal().getZ(), point3.getNormal().getZ(), TOLERANCE_GEODETIC_POINT);

        /* Compare the ellipsoid points with each other */

        // Assertions
        Assert.assertEquals(0., point1.getPosition().subtract(position0).getX(), TOLERANCE_GEODETIC_POINT);
        Assert.assertEquals(0., point1.getPosition().subtract(position0).getY(), TOLERANCE_GEODETIC_POINT);
        Assert.assertEquals(0., point1.getPosition().subtract(position0).getZ(), TOLERANCE_GEODETIC_POINT);
        Assert.assertEquals(0., point2.getPosition().subtract(position0).getX(), TOLERANCE_GEODETIC_POINT);
        Assert.assertEquals(0., point2.getPosition().subtract(position0).getY(), TOLERANCE_GEODETIC_POINT);
        Assert.assertEquals(0., point2.getPosition().subtract(position0).getZ(), TOLERANCE_GEODETIC_POINT);
        Assert.assertEquals(0., point3.getPosition().subtract(position0).getX(), TOLERANCE_GEODETIC_POINT);
        Assert.assertEquals(0., point3.getPosition().subtract(position0).getY(), TOLERANCE_GEODETIC_POINT);
        Assert.assertEquals(0., point3.getPosition().subtract(position0).getZ(), TOLERANCE_GEODETIC_POINT);
        /* Check all their points on shape have null height */

        // Assertions
        Assert.assertEquals(0., point0.getClosestPointOnShape().getLLHCoordinates().getHeight(),
            TOLERANCE_GEODETIC_POINT);
        Assert.assertEquals(0., point0.getRadialProjectionOnShape().getLLHCoordinates().getHeight(),
            TOLERANCE_GEODETIC_POINT);
        Assert.assertEquals(0., point1.getClosestPointOnShape().getLLHCoordinates().getHeight(),
            TOLERANCE_GEODETIC_POINT);
        Assert.assertEquals(0., point1.getRadialProjectionOnShape().getLLHCoordinates().getHeight(),
            TOLERANCE_GEODETIC_POINT);
        Assert.assertEquals(0., point2.getClosestPointOnShape().getLLHCoordinates().getHeight(),
            TOLERANCE_GEODETIC_POINT);
        Assert.assertEquals(0., point2.getRadialProjectionOnShape().getLLHCoordinates().getHeight(),
            TOLERANCE_GEODETIC_POINT);
        Assert.assertEquals(0., point3.getClosestPointOnShape().getLLHCoordinates().getHeight(),
            TOLERANCE_GEODETIC_POINT);
        Assert.assertEquals(0., point3.getRadialProjectionOnShape().getLLHCoordinates().getHeight(),
            TOLERANCE_GEODETIC_POINT);
    }

    /**
     * @testType UT
     *
     * @description Test the getters of a class in case of spherical body.
     *
     * @testPassCriteria the getters returns the expected values (simple getters, results computed mathematically)
     *                   thresholds are not always
     *                   0 since some computations lead to slight round-off errors
     *
     * @referenceVersion 4.11
     *
     * @nonRegressionVersion 4.11
     */
    @Test
    public void testGettersSpherical() {

        // Build GeodeticPoint
        final double latitude = MathLib.toRadians(42.5);
        final double longitude = MathLib.toRadians(12.4);
        final double altitude = 1234;
        final EllipsoidBodyShape bodyShape = new OneAxisEllipsoid(Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS, 0.,
            FramesFactory.getGCRF());
        final String name = "EllipsoidPoint";
        final EllipsoidPoint point = new EllipsoidPoint(bodyShape, bodyShape.getLLHCoordinatesSystem(), latitude,
            longitude, altitude, name);
        final EllipsoidPoint point2 = new EllipsoidPoint(bodyShape, bodyShape.getLLHCoordinatesSystem(), latitude,
            longitude, -altitude, name);

        // Checks
        // Methods from EllipsoidPoint
        Assert.assertEquals(latitude, point.getLLHCoordinates().getLatitude(), 0.);
        Assert.assertEquals(longitude, point.getLLHCoordinates().getLongitude(), 0.);
        Assert.assertEquals(altitude, point.getLLHCoordinates().getHeight(), 1e-10);
        Assert.assertEquals(altitude, point.getRadialProjectionOnShape().getPosition().subtract(point.getPosition())
            .getNorm(), 1E-9);
        Assert.assertEquals(altitude, point2.getRadialProjectionOnShape().getPosition().subtract(point2.getPosition())
            .getNorm(), 1E-9);
        Assert.assertEquals(latitude, point.getClosestPointOnShape().getLLHCoordinates().getLatitude(), 0.);
        Assert.assertEquals(longitude, point.getClosestPointOnShape().getLLHCoordinates().getLongitude(), 0.);
        Assert.assertEquals(0., point.getClosestPointOnShape().getLLHCoordinates().getHeight(), 0.);
        Assert.assertFalse(point.isInsideShape());
        Assert.assertTrue(point.getClosestPointOnShape().isInsideShape());
        Assert.assertEquals(bodyShape, point.getBodyShape());
        Assert.assertNotNull(point.toString());
        Assert.assertNotNull(point.toString());
        Assert.assertNotNull(point.toString(LLHCoordinatesSystem.BODYCENTRIC_NORMAL));
        Assert.assertNotNull(point.toString(LLHCoordinatesSystem.BODYCENTRIC_RADIAL));

        // Methods from AbstractBodyPoint
        Assert.assertEquals(name, point.getName());
        Assert.assertEquals(0., Vector3D.distance(new SphericalCoordinates(point.getLLHCoordinates().getLatitude(),
            point.getLLHCoordinates().getLongitude(), bodyShape.getARadius() + altitude)
            .getCartesianCoordinates(), point.getPosition()), 1E-9);
        Assert.assertEquals(0., Vector3D.distance(point.getPosition().normalize(), point.getNormal()), 1E-15);

        // Method from BodyPoint
        Assert.assertFalse(point.isOnShapeSurface());
    }

    /**
     * @testType UT
     *
     * @description Test the getters of a class in case of ellipsoid body.
     *
     * @testPassCriteria the getters returns the expected values (simple getters, results computed either mathematically
     *                   or non-regression
     *                   only after thematical validation)
     *
     * @referenceVersion 4.11
     *
     * @nonRegressionVersion 4.11
     */
    @Test
    public void testGettersEllipsoid() {

        // Build GeodeticPoint
        final double latitude = MathLib.toRadians(42.5);
        final double longitude = MathLib.toRadians(12.4);
        final double altitude = 1234;
        final String name = "EllipsoidPoint";
        final EllipsoidBodyShape bodyShape = new OneAxisEllipsoid(Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS,
            Constants.GRIM5C1_EARTH_FLATTENING, FramesFactory.getGCRF());
        final EllipsoidPoint point = new EllipsoidPoint(bodyShape, bodyShape.getLLHCoordinatesSystem(), latitude,
            longitude, altitude, name);

        // Checks
        // Methods from EllipsoidPoint
        Assert.assertEquals(latitude, point.getLLHCoordinates().getLatitude(), 0.);
        Assert.assertEquals(longitude, point.getLLHCoordinates().getLongitude(), 1e-12);
        Assert.assertEquals(altitude, point.getLLHCoordinates().getHeight(), 1e-9);
        Assert.assertEquals(1234.0069009013573,
            point.getRadialProjectionOnShape().getPosition().subtract(point.getPosition()).getNorm(), 0.);
        // Expected to be slightly larger than altitude

        Assert.assertEquals(0.7417649320975901, point.getClosestPointOnShape().getLLHCoordinates().getLatitude(), 0.);
        // Expected to be slightly different from latitude

        Assert.assertEquals(0.21642082724729678, point.getClosestPointOnShape().getLLHCoordinates().getLongitude(), 0.);
        // Expected to be slightly different from longitude

        Assert.assertEquals(0., point.getClosestPointOnShape().getLLHCoordinates().getHeight(), 1E-9);
        Assert.assertFalse(point.isInsideShape());
        Assert.assertEquals(bodyShape, point.getBodyShape());
        Assert.assertNotNull(point.toString());
        Assert.assertNotNull(point.toString(LLHCoordinatesSystem.ELLIPSODETIC));
        Assert.assertNotNull(point.toString(LLHCoordinatesSystem.BODYCENTRIC_NORMAL));
        Assert.assertNotNull(point.toString(LLHCoordinatesSystem.BODYCENTRIC_RADIAL));

        // Methods from AbstractBodyPoint
        Assert.assertEquals(name, point.getName());
        final LLHCoordinates bodycentricCoord = point.getLLHCoordinates(LLHCoordinatesSystem.BODYCENTRIC_RADIAL);
        Assert.assertEquals(9715.90721085705, Vector3D.distance(
            new SphericalCoordinates(bodycentricCoord.getLatitude(), bodycentricCoord.getLongitude(), bodyShape
                .getARadius() + altitude).getCartesianCoordinates(), point.getPosition()), 1E-9);
        // Expected to be slightly different from position vector

        Assert.assertEquals(0.003344005668915436,
            Vector3D.distance(point.getPosition().normalize(), point.getNormal()), 1E-6);
        // Expected to be slightly different from position vector

        // Method from BodyPoint
        Assert.assertFalse(point.isOnShapeSurface());
    }

    /**
     * @description Evaluate the ellipsoid point serialization / deserialization process.
     *
     * @testPassCriteria The ellipsoid point can be serialized with all its parameters and deserialized.
     */
    @Test
    public void testSerialization() {

        // Random test
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(1000, 0.1, FramesFactory.getGCRF());
        final Random r = new Random();
        for (int i = 0; i < 100; ++i) {
            final double lat = r.nextDouble();
            final double longi = r.nextDouble();
            final double alti = r.nextDouble();
            final String name = "point" + i;
            final EllipsoidPoint point1 = new EllipsoidPoint(earth, earth.getLLHCoordinatesSystem(),
                MathLib.toRadians(lat), MathLib.toRadians(longi), alti, name);
            final EllipsoidPoint point2 = TestUtils.serializeAndRecover(point1);
            assertEqualsBodyPoint(point1, point2);
            assertEqualsBodyPoint(point1.getClosestPointOnShape(), point2.getClosestPointOnShape());
            assertEqualsBodyPoint(point1.getRadialProjectionOnShape(), point2.getRadialProjectionOnShape());
        }

        // Test on shape surface
        final EllipsoidPoint point1 = new EllipsoidPoint(earth, earth.getLLHCoordinatesSystem(),
            MathLib.toRadians(0.3), MathLib.toRadians(1.8), 0., "point");
        final EllipsoidPoint point2 = TestUtils.serializeAndRecover(point1);
        assertEqualsBodyPoint(point1, point2);
        assertEqualsBodyPoint(point1.getClosestPointOnShape(), point2.getClosestPointOnShape());
        assertEqualsBodyPoint(point1.getRadialProjectionOnShape(), point2.getRadialProjectionOnShape());
    }

    /**
     * @testType UT
     * 
     * @description Test all the {@link BodyPoint} methods
     * 
     * @testPassCriteria the methods returns the expected results (reference: math, threshold: 0)
     * 
     * @referenceVersion 4.12
     * 
     * @nonRegressionVersion 4.12
     */
    @Test
    public void testBodyPoint() throws PatriusException {

        // Build BodyPoint (here, a simple EllipsoidPoint)
        final OneAxisEllipsoid shape = new OneAxisEllipsoid(6378000, 0, FramesFactory.getGCRF());
        final Vector3D position = new Vector3D(7000E3, 0, 0);
        final EllipsoidPoint point = new EllipsoidPoint(shape, position, "");

        Assert.assertTrue(point.getPVCoordinates(AbsoluteDate.J2000_EPOCH, FramesFactory.getGCRF()).getPosition().equals(position));
        Assert.assertTrue(point.getNormal().equals(new Vector3D(1, 0, 0)));
        Assert.assertEquals(point.angularSeparation(point), 0, 0);
        Assert.assertEquals(point.getNativeFrame(null), FramesFactory.getGCRF());
    }

    /**
     * Evaluate two {@link BodyPoint} with their attributes.
     * 
     * @param point1
     *        First point
     * @param point2
     *        Second point
     */
    public static void assertEqualsBodyPoint(final BodyPoint point1, final BodyPoint point2) {
        Assert.assertEquals(point1.getName(), point2.getName());
        Assert.assertEquals(point1.isOnShapeSurface(), point2.isOnShapeSurface());
        Assert.assertEquals(point1.getPosition(), point2.getPosition());

        Assert.assertEquals(point1.getNormalHeight(), point2.getNormalHeight(), 0.);
        Assert.assertEquals(point1.getNormal(), point2.getNormal());

        Assert.assertEquals(point1.getLLHCoordinates().getLatitude(), point2.getLLHCoordinates().getLatitude(), 0.);
        Assert.assertEquals(point1.getLLHCoordinates().getLongitude(), point2.getLLHCoordinates().getLongitude(), 0.);
        Assert.assertEquals(point1.getLLHCoordinates().getHeight(), point2.getLLHCoordinates().getHeight(), 0.);

        Assert.assertEquals(point1.toString(), point2.toString());
    }
}
