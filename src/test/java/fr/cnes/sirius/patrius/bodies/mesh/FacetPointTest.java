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
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies.mesh;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.bodies.EllipsoidPointTest;
import fr.cnes.sirius.patrius.bodies.LLHCoordinates;
import fr.cnes.sirius.patrius.bodies.LLHCoordinatesSystem;
import fr.cnes.sirius.patrius.bodies.mesh.FacetBodyShape.EllipsoidType;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class for class {@link FacetPoint}.
 */
public class FacetPointTest {

    /** Body radius (m). */
    private final double bodyRadius = 10000.;

    /** Facet celestial body used for tests. */
    private FacetBodyShape body;

    /**
     * Builds a theoretical spherical celestial whose poles are aligned with GCRF and whose
     * PVCoordinates are (0, 0, 0)in GCRF frame.
     */
    @Before
    public void setUp() throws PatriusException, IOException {

        // Build body file
        final String spherBodyObjPath = "src" + File.separator + "test" + File.separator
                + "resources" + File.separator + "mnt" + File.separator + "SphericalBody.obj";
        final String modelFile = System.getProperty("user.dir") + File.separator + spherBodyObjPath;
        writeBodyFile(modelFile, 51, 100, this.bodyRadius / 1E3, 0.);

        this.body = new FacetBodyShape("My body", FramesFactory.getGCRF(), new ObjMeshLoader(modelFile));
    }

    /**
     * Build spherical model and write it in file
     * 
     * @param modelFile
     *        output model file name
     * @param latitudeNumber
     *        number of latitude points (should be odd)
     * @param longitudeNumber
     *        number longitude points
     * @param radius
     *        body radius (km)
     */
    private static void writeBodyFile(final String modelFile, final int latitudeNumber, final int longitudeNumber,
                                      final double radius, final double flattening) throws IOException {
        // Initialization, open resources
        final FileOutputStream fileOutputStream = new FileOutputStream(modelFile);
        final OutputStreamWriter fileWriter = new OutputStreamWriter(fileOutputStream, Charset.forName("UTF-8")
            .newEncoder());
        final PrintWriter printWriter = new PrintWriter(fileWriter);

        // Build body
        final int latitudeNumber2 = (latitudeNumber - 1) / 2 - 1;
        final int numberPoints = (2 * latitudeNumber2 + 1) * longitudeNumber + 2;

        // Points

        // South pole
        printWriter.println(String.format(Locale.US, "v %20.15f%20.15f%20.15f", 0., 0., -radius * (1 - flattening)));

        // Regular points excluding poles
        for (int i = -latitudeNumber2; i <= latitudeNumber2; i++) {
            final double latitude = (double) i / (latitudeNumber2 + 1) * MathLib.PI / 2.;
            for (int j = 0; j < longitudeNumber; j++) {
                final double longitude = (double) j / longitudeNumber * 2. * MathLib.PI;
                final double coslat = MathLib.cos(latitude);
                final double sinlat = MathLib.sin(latitude);
                final double coslon = MathLib.cos(longitude);
                final double sinlon = MathLib.sin(longitude);
                final Vector3D pv = new Vector3D(coslat * coslon, coslat * sinlon, sinlat * (1 - flattening))
                    .scalarMultiply(radius);
                printWriter
                    .println(String.format(Locale.US, "v %20.15f%20.15f%20.15f", pv.getX(), pv.getY(), pv.getZ()));
            }
        }

        // North pole
        printWriter.println(String.format(Locale.US, "v %20.15f%20.15f%20.15f", 0., 0., radius * (1 - flattening)));

        // Triangles

        // South pole
        for (int j = 0; j < longitudeNumber - 1; j++) {
            printWriter.println(String.format(Locale.US, "f %10d%10d%10d", 1, j + 3, j + 2));
        }
        printWriter.println(String.format(Locale.US, "f %10d%10d%10d", 1, 2, longitudeNumber + 1));

        // Regular points excluding poles
        for (int i = 0; i < latitudeNumber - 3; i++) {
            for (int j = 0; j < longitudeNumber - 1; j++) {
                printWriter.println(String.format(Locale.US, "f %10d%10d%10d", i * longitudeNumber + 2 + j, i
                        * longitudeNumber + 2 + j + 1, (i + 1) * longitudeNumber + 2 + j));

                printWriter.println(String.format(Locale.US, "f %10d%10d%10d", i * longitudeNumber + 2 + j + 1, (i + 1)
                        * longitudeNumber + 2 + j + 1, (i + 1) * longitudeNumber + 2 + j));
            }
            printWriter.println(String.format(Locale.US, "f %10d%10d%10d", i * longitudeNumber + 2 + longitudeNumber
                    - 1, i * longitudeNumber + 2, (i + 1) * longitudeNumber + 2 + longitudeNumber - 1));

            printWriter.println(String.format(Locale.US, "f %10d%10d%10d", i * longitudeNumber + 2, (i + 1)
                    * longitudeNumber + 2, (i + 1) * longitudeNumber + 2 + longitudeNumber - 1));
        }

        // North pole
        for (int j = 0; j < longitudeNumber - 1; j++) {
            printWriter.println(String.format(Locale.US, "f %10d%10d%10d", numberPoints, numberPoints - j - 2,
                numberPoints - j - 1));
        }
        printWriter.println(String.format(Locale.US, "f %10d%10d%10d", numberPoints, numberPoints - 1, numberPoints
                - longitudeNumber));

        // Close resources
        printWriter.close();
        fileWriter.close();
        fileOutputStream.close();
    }

    /**
     * @testType UT
     * 
     * @description Test the getters of a class in case of spherical-simulated FacetBodyShape body
     * 
     * @testPassCriteria the getters returns the expected values (simple getters, results computed mathematically based
     *                   on sphere results) thresholds are not always 0 since reference results are from a sphere and
     *                   actual results from a FacetBodyShape representing a sphere with finite number of facets
     * 
     * @referenceVersion 4.12
     * 
     * @nonRegressionVersion 4.12
     */
    @Test
    public void testGetters() throws PatriusException {

        // Build FacetPoint
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Vector3D posGCRF = new Vector3D(10000, 20000, 30000);
        final Vector3D posOnShape = this.body.closestPointTo(posGCRF, body.getBodyFrame(), date).getPosition();

        final Vector3D posOnShapeEME2000 = FramesFactory.getGCRF().getTransformTo(FramesFactory.getEME2000(), date)
            .transformPosition(posOnShape);

        final FacetPoint point = new FacetPoint(this.body, posGCRF, "OverSurface");
        final FacetPoint point2 = new FacetPoint(this.body, posOnShapeEME2000, FramesFactory.getEME2000(), date,
            "OnSurface");
        final FacetPoint point3 = new FacetPoint(this.body, posOnShape.scalarMultiply(1. / 10.), "UnderSurface");

        // Method from BodyPoint
        Assert.assertFalse(point.isInsideShape());
        Assert.assertTrue(point3.isInsideShape());

        // Checks methods from FacetPoint
        // Distance to shape is only exact on vertices
        Assert.assertEquals(posGCRF.getNorm() - this.bodyRadius, point.getClosestPointOnShape().distance(point), 3.);
        // Distance to shape is only exact on vertices
        Assert.assertEquals(posGCRF.getNorm() - this.bodyRadius, point.getRadialProjectionOnShape().distance(point),
            5.);
        // Shape closest point is only exact on vertices
        Assert.assertEquals(
            0.,
            Vector3D.distance(point.getClosestPointOnShape().getPosition(),
                point.getPosition().scalarMultiply(this.bodyRadius / point.getPosition().getNorm())), 180.);

        Assert.assertTrue(point2.getNormalHeight() < 1e-12);
        Assert.assertTrue(point3.getNormalHeight() < 0);
        Assert.assertEquals(this.body, point.getBodyShape());
        Assert.assertEquals("FacetPoint: name='OverSurface', surface bodycentric coord={lat=0.9425140946216841, "
                + "long=1.1284423519899403}rad, normal height=27418.946465556313m, closest facets={7736, 7935}, "
                + "body='My body'", point.toString());

        // Methods from AbstractBodyPoint
        Assert.assertEquals("OverSurface", point.getName());
        Assert.assertEquals(posGCRF, point.getPosition());
    }

    /**
     * @testType UT
     * 
     * @description Test the computation of closest triangles in some various configurations: regular, on edge, on
     *              vertex
     * 
     * @testPassCriteria the getters returns the expected values (simple getters, results computed mathematically based
     *                   on sphere results) thresholds are not always 0 since reference results are from a sphere and
     *                   actual results from a FacetBodyShape representing a sphere with finite number of facets
     * 
     * @referenceVersion 4.12
     * 
     * @nonRegressionVersion 4.12
     */
    @Test
    public void testClosestTriangles() {
        // Regular
        final Triangle t = this.body.getTriangles()[1234];
        final FacetPoint point1 = new FacetPoint(this.body, t.getCenter().add(t.getNormal()), "");
        Assert.assertEquals(1, point1.getClosestTriangles().size());
        // On edge (on the equator)
        final FacetPoint point2 = new FacetPoint(this.body, new Vector3D(20000, 20000, 0), "");

        Assert.assertEquals(2, point2.getClosestTriangles().size());
        // On vertex (north pole) - 100 triangles around north pole
        final FacetPoint point3 = new FacetPoint(this.body, new Vector3D(0, 0, this.bodyRadius + 1000), "");
        Assert.assertEquals(100, point3.getClosestTriangles().size());
    }

    /**
     * @testType UT
     *
     * @description Try to build or use facet points with non-supported coordinates systems.
     *
     * @testPassCriteria The exceptions are thrown as expected
     *
     * @referenceVersion 4.12
     */
    @Test
    public void testCoordinatesSystemError() {

        // Try to build a facet point with a FacetBodyShape and LLHCoordinatesSystem.ELLIPSODETIC (shouldn't be
        // allowed as this coordinates system should be associated to an EllipsoidBodyShape)
        try {
            new FacetPoint(this.body, new LLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC, 1., 1.5, 100.), "");
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // Expected
            Assert.assertTrue(true);
        }

        // Build a "normal" facet point with a FacetBodyShape
        final FacetPoint point = new FacetPoint(this.body,
            new LLHCoordinates(LLHCoordinatesSystem.BODYCENTRIC_NORMAL, 1., 1.5, 100.), "");

        // Try to compute the coordinates of the facet point in the LLHCoordinatesSystem.ELLIPSODETIC system (shouldn't
        // be allowed as this coordinates system should be associated to an EllipsoidBodyShape)
        try {
            point.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // Expected
            Assert.assertTrue(true);
        }

        // Try to compute the position of the facet point in the default LLHCoordinatesSystem.BODYCENTRIC_NORMAL system
        // (shouldn't fail)
        try {
            point.getPosition();
            Assert.assertTrue(true);
        } catch (final IllegalArgumentException e) {
            // Not expected
            Assert.fail();
        }
    }

    /**
     * @description Evaluate the facet point serialization / deserialization process.
     *
     * @testPassCriteria The facet point can be serialized with all its parameters and deserialized.
     */
    @Test
    public void testSerialization() {

        // Random test
        final Random r = new Random();
        for (int i = 0; i < 100; ++i) {
            final double lat = r.nextDouble();
            final double longi = r.nextDouble();
            final double alti = r.nextDouble();
            final String name = "point" + i;
            final FacetPoint point1 = new FacetPoint(this.body, this.body.getLLHCoordinatesSystem(),
                MathLib.toRadians(lat), MathLib.toRadians(longi), alti, name);
            final FacetPoint point2 = TestUtils.serializeAndRecover(point1);
            EllipsoidPointTest.assertEqualsBodyPoint(point1, point2);
            EllipsoidPointTest.assertEqualsBodyPoint(point1.getClosestPointOnShape(), point2.getClosestPointOnShape());
            EllipsoidPointTest.assertEqualsBodyPoint(point1.getRadialProjectionOnShape(),
                point2.getRadialProjectionOnShape());
        }

        // Test on shape surface
        final FacetPoint point1 = new FacetPoint(this.body, this.body.getLLHCoordinatesSystem(),
            MathLib.toRadians(0.3), MathLib.toRadians(1.8), 0., "point");
        final FacetPoint point2 = TestUtils.serializeAndRecover(point1);
        EllipsoidPointTest.assertEqualsBodyPoint(point1, point2);
        EllipsoidPointTest.assertEqualsBodyPoint(point1.getClosestPointOnShape(), point2.getClosestPointOnShape());
        EllipsoidPointTest.assertEqualsBodyPoint(point1.getRadialProjectionOnShape(),
            point2.getRadialProjectionOnShape());

        // Check the closest triangles are well recomputed (transient attribute)
        final List<Triangle> closestTrianglesList1 = point1.getClosestTriangles();
        final List<Triangle> closestTrianglesList2 = point2.getClosestTriangles();

        // Triangle doesn't override equals() so we extrat their ID to compare them
        // Store them in Sets so we can use the containsAll method to evaluate them
        final Set<Integer> closestTriangleIDsSet1 = new HashSet<>(closestTrianglesList1.size());
        for (final Triangle closestTriangles1 : closestTrianglesList1) {
            closestTriangleIDsSet1.add(closestTriangles1.getID());
        }
        final Set<Integer> closestTriangleIDsSet2 = new HashSet<>(closestTrianglesList2.size());
        for (final Triangle closestTriangles2 : closestTrianglesList2) {
            closestTriangleIDsSet2.add(closestTriangles2.getID());
        }

        Assert.assertTrue(closestTriangleIDsSet1.size() > 0); // Check some elements are initialized
        Assert.assertEquals(closestTriangleIDsSet1.size(), closestTriangleIDsSet2.size());
        Assert.assertTrue(closestTriangleIDsSet1.containsAll(closestTriangleIDsSet2)); // Compare set by bijection
        Assert.assertTrue(closestTriangleIDsSet2.containsAll(closestTriangleIDsSet1));
    }
}
