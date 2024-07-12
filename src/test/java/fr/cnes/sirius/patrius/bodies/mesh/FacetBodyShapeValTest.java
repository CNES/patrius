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
 * HISTORY
 * VERSION:4.11.1:FA:FA-53:30/06/2023:[PATRIUS] Error in class FieldData
 * VERSION:4.11.1:FA:FA-81:30/06/2023:[PATRIUS] Reliquat DM 3299
 * VERSION:4.11.1:FA:FA-60:30/06/2023:[PATRIUS] Erreur dans les méthodes getNeighbors de FacetBodyShape
 * VERSION:4.11.1:FA:FA-50:30/06/2023:[PATRIUS] Calcul d'intersections sur un FacetBodyShape
 * VERSION:4.11:FA:FA-3277:22/05/2023:[PATRIUS] Ellipsoïde ajuste sur un FacetBodyShape
 * VERSION:4.11:DM:DM-3311:22/05/2023:[PATRIUS] Evolutions mineures sur CelestialBody, shape et reperes
 * VERSION:4.11:DM:DM-3300:22/05/2023:[PATRIUS] Nouvelle approche pour le calcul de la position relative de 2 corps celestes 
 * VERSION:4.11:DM:DM-3259:22/05/2023:[PATRIUS] Creer une interface StarConvexBodyShape
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration de la gestion des attractions gravitationnelles dans le propagateur
 * VERSION:4.10.1:FA:FA-3265:02/12/2022:[PATRIUS] Calcul KO des points plus proches entre une Line et un FacetBodyShape
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.10:DM:DM-3183:03/11/2022:[PATRIUS] Acces aux points les plus proches entre un GeometricBodyShape...
 * VERSION:4.10:FA:FA-3186:03/11/2022:[PATRIUS] Corriger la duplication entre getLocalRadius et getApparentRadius
 * VERSION:4.9.1:FA:FA-3193:01/06/2022:[PATRIUS] Revenir a la signature initiale de la methode getLocalRadius
 * VERSION:4.9.1:DM:DM-3168:01/06/2022:[PATRIUS] Ajout de la classe ConstantPVCoordinatesProvider
 * VERSION:4.9:DM:DM-3159:10/05/2022:[PATRIUS] Implementation de l'interface GeometricBodyShape par CelestialBody
 * VERSION:4.9:DM:DM-3135:10/05/2022:[PATRIUS] Calcul d'intersection sur BodyShape  
 * VERSION:4.9:DM:DM-3168:10/05/2022:[PATRIUS] Ajout de la classe FixedPVCoordinatesProvider 
 * VERSION:4.9:FA:FA-3170:10/05/2022:[PATRIUS] Incoherence de datation entre la methode getLocalRadius...
 * VERSION:4.9:DM:DM-3127:10/05/2022:[PATRIUS] Ajout de deux methodes resize a l'interface GeometricBodyShape ...
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.7:DM:DM-2870:18/05/2021:Complements pour la manipulation des coordonnees cart.
 * VERSION:4.7:FA:FA-2821:18/05/2021:Refus injustifié de calcul de l incidence solaire lorsqu elle dépasse 90 degres 
 * VERSION:4.6:DM:DM-2544:27/01/2021:Ajouter la definition d'un corps celeste a partir d'un modele de forme 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies.mesh;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.BodyCenterGroundPointing;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.bodies.IAUPoleFactory;
import fr.cnes.sirius.patrius.bodies.IAUPoleModelType;
import fr.cnes.sirius.patrius.bodies.MeeusSun;
import fr.cnes.sirius.patrius.bodies.UserCelestialBody;
import fr.cnes.sirius.patrius.bodies.mesh.FacetBodyShape.EllipsoidType;
import fr.cnes.sirius.patrius.fieldsofview.CircularField;
import fr.cnes.sirius.patrius.fieldsofview.IFieldOfView;
import fr.cnes.sirius.patrius.fieldsofview.PyramidalField;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.exception.MaxCountExceededException;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Plane;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.SphericalCoordinates;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.ConstantPVCoordinatesProvider;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.AbstractDetector.PropagationDelayType;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Validation tests for {@link FacetCelestialBody} class.
 *
 * @author Emmanuel Bignon
 *
 * @since 4.6
 */
public class FacetBodyShapeValTest {

    /** Epsilon for double comparison. */
    private static final double EPS = 1E-14;

    /** Expected minimum value for distance between vertices and center of the body. */
    private static final double EXPECTED_MIN = 8.08439663366414E3;

    /** Expected maximum value for distance between vertices and center of the body. */
    private static final double EXPECTED_MAX = 13.9663150079038E3;

    /** Expected inner ellipsoid equatorial radius. */
    private static final double EXPECTED_INNER_RADIUS = 10424.120296826843;

    /** Expected outer ellipsoid equatorial radius. */
    private static final double EXPECTED_OUTER_RADIUS = 14174.074751351569;

    /** User celestial body used for tests: Phobos mesh in Moon position. */
    private static UserCelestialBody celestialBody1;

    /** User celestial body used for tests: Phobos mesh in Moon position. */
    private static UserCelestialBody celestialBody2;

    /** Star convex Facet body shape used for tests: Phobos mesh in Moon position. */
    private static StarConvexFacetBodyShape body1;

    /** Star convex Facet body shape used for tests: Phobos mesh in Moon position. */
    private static FacetBodyShape body2;

    /** Facet body shape used for tests: 67P/Tchouri mesh in GCRF. */
    private static FacetBodyShape body67P;

    /**
     * Load mesh.
     */
    @BeforeClass
    public static void setUpBeforeClass() throws PatriusException, URISyntaxException {
        // Patrius data set
        Utils.setDataRoot("regular-dataPBASE");

        // Load .obj mesh
        final String modelFile1 = "mnt" + File.separator + "Phobos_Ernst_HD.obj";
        final String fullName1 = StarConvexFacetBodyShape.class.getClassLoader().getResource(modelFile1).toURI()
            .getPath();
        celestialBody1 = new UserCelestialBody("", CelestialBodyFactory.getMoon(), 0, IAUPoleFactory.getIAUPole(null),
            FramesFactory.getGCRF(), null);
        body1 = new StarConvexFacetBodyShape("Phobos HD", celestialBody1.getRotatingFrame(IAUPoleModelType.TRUE),
            EllipsoidType.INNER_SPHERE,
            new ObjMeshLoader(fullName1));
        // Load geodetic mesh and validate .obj file writing
        final String modelFile2 = "mnt" + File.separator + "m1phobos.tab";
        final String fullName2 = StarConvexFacetBodyShape.class.getClassLoader().getResource(modelFile2).toURI()
            .getPath();
        final GeodeticMeshLoader loader2 = new GeodeticMeshLoader(fullName2);
        final String m1phobosObjPath = "src" + File.separator + "test" + File.separator + "resources" + File.separator
                + "mnt" + File.separator + "m1phobos.obj";
        loader2.toObjFile(m1phobosObjPath);
        final ObjMeshLoader loader3 = new ObjMeshLoader(m1phobosObjPath);
        celestialBody2 = new UserCelestialBody("", CelestialBodyFactory.getMoon(), 0, IAUPoleFactory.getIAUPole(null),
            FramesFactory.getGCRF(), null);
        body2 = new FacetBodyShape("Phobos m1", celestialBody2.getRotatingFrame(IAUPoleModelType.TRUE),
            EllipsoidType.INNER_SPHERE, loader3);
        Assert.assertEquals(loader2.getVertices().size(), loader3.getVertices().size());
        Assert.assertEquals(loader2.getTriangles().length, loader3.getTriangles().length);

        // Load .obj mesh for Tchouri
        final String modelFile67P = "mnt" + File.separator + "67P_Tchouri.obj";
        final String fullName67P = FacetBodyShape.class.getClassLoader().getResource(modelFile67P).toURI().getPath();
        body67P = new FacetBodyShape("", FramesFactory.getGCRF(), EllipsoidType.INNER_SPHERE, new ObjMeshLoader(
            fullName67P));
    }

    /**
     * @testType" + File.separator + "VT
     *
     * @description check that body surface is as expected.
     *
     * @testPassCriteria body surface is equal between the two mesh (threshold: 1E-2, limited due to different number of
     *                   vertices)
     *
     * @referenceVersion 4.6
     *
     * @nonRegressionVersion 4.6
     */
    @Test
    public void surfaceTest() {
        double surface1 = 0;
        for (final Triangle triangle : body1.getTriangles()) {
            surface1 += triangle.getSurface();
        }
        double surface2 = 0;
        for (final Triangle triangle : body2.getTriangles()) {
            surface2 += triangle.getSurface();
        }
        Assert.assertEquals(0, (surface1 - surface2) / surface1, 1E-2);
    }

    /**
     * @testType VT
     *
     * @description check that distance from line to body is properly computed.
     *
     * @testPassCriteria distance from line to body is properly computed (reference: math, absolute threshold: 80m,
     *                   limited because method distanceTo() is not exact)
     *
     * @referenceVersion 4.6
     *
     * @nonRegressionVersion 4.6
     */
    @Test
    public void distanceToLineTest() throws PatriusException {
        // Initialization
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Frame frame = FramesFactory.getEME2000();

        // Compute several distance to lines
        // Too many take too much time
        for (int i = 690; i < 710; i++) {
            // Earth - Phobos line of sight with changing shift (10 first lines cross body, 10 last lines do not)
            final Vector3D position1 = Vector3D.ZERO;
            final Vector3D position2 = body1.getPVCoordinates(date, frame).getPosition()
                .add(new Vector3D(20 * i, 20 * i, 20 * i));
            final Line line = new Line(position1, position2);

            // Convert line to body frame
            final Transform t = frame.getTransformTo(body1.getBodyFrame(), date);
            final Line lineInBodyFrame = t.transformLine(line);

            // Actual distance
            final double minDistanceAct = body1.distanceTo(line, frame, date);

            // Reference: computed for each triangle
            double minDistanceRef = 0;
            if (body1.getIntersectionPoints(line, frame, date).length == 0) {
                minDistanceRef = Double.POSITIVE_INFINITY;
                for (final Triangle triangle : body1.getTriangles()) {
                    minDistanceRef = MathLib.min(minDistanceRef, triangle.distanceTo(lineInBodyFrame));
                }
            }

            // Check
            Assert.assertEquals(minDistanceRef, minDistanceAct, 0.);
        }
    }

    /**
     * @testType VT
     *
     * @description check that distance from sub-line to body is properly computed.
     *
     * @testPassCriteria distance from sub-line to body is properly computed (reference: math, absolute threshold: 550m,
     *                   limited because method distanceTo() is not exact)
     *
     * @referenceVersion 4.10
     *
     * @nonRegressionVersion 4.10
     */
    @Test
    public void distanceToSubLineTest() throws PatriusException {
        // Initialization
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;

        // Compute several distance to lines
        // Too many take too much time
        for (int i = 0; i < 10; i++) {
            // Earth - Phobos line of sight with changing shift (20 first lines cross body, 20 last lines do not)
            final Vector3D position1 = Vector3D.ZERO;
            final Vector3D position2 = new Vector3D(new SphericalCoordinates(i / 3, i % 3, 20000));
            final Line subLine = new Line(position1, position2, position2);

            // Actual distance
            final double minDistanceAct = body1.distanceTo(subLine, body1.getBodyFrame(), date);

            // Reference: computed for each triangle
            double minDistanceRef = Double.POSITIVE_INFINITY;
            for (final Triangle triangle : body1.getTriangles()) {
                minDistanceRef = MathLib.min(minDistanceRef, triangle.distanceTo(subLine));
            }

            // Check
            Assert.assertEquals(minDistanceRef, minDistanceAct, 0.);
        }
    }

    /**
     * @testType VT
     *
     * @description check that intersection between a line of sight and the body is properly computed:
     *              <ul>
     *              <li>Check triangle belongs to body</li>
     *              <li>Check found intersection point belongs to triangle</li>
     *              <li>Check intersection point lies on line of sight</li>
     *              <li>Check intersection point is between satellite body center</li>
     *              <li>Check intersection point is similar between .obj and geodetic files</li>
     *              </ul>
     *
     * @testPassCriteria intersection between a line of sight and the body is properly computed for various
     *                   configurations (reference: math, absolute threshold is limited due to large distance
     *                   [300 000km] between point and body)
     *
     * @referenceVersion 4.6
     *
     * @nonRegressionVersion 4.6
     */
    @Test
    public void intersectionTest() throws PatriusException {
        // Initialization
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Frame frame = FramesFactory.getEME2000();

        // Epsilon
        final double epsilon = 3E-7;

        // Compute several intersections
        for (int i = 0; i < 1000; i++) {
            // Earth - Phobos line of sight with changing shift
            final Vector3D position1 = Vector3D.ZERO;
            final Vector3D position2 = body1.getPVCoordinates(date, frame).getPosition().add(new Vector3D(i, i, i));
            final Line lineOfSight = new Line(position1, position2);

            // Compute intersection
            final Intersection actual1 = body1.getIntersection(lineOfSight, position1, frame, date);
            final Intersection actual2 = body2.getIntersection(lineOfSight, position1, frame, date);

            // Triangle belongs to body
            Assert.assertTrue(Arrays.asList(body1.getTriangles()).contains(actual1.getTriangle()));
            Assert.assertTrue(Arrays.asList(body2.getTriangles()).contains(actual2.getTriangle()));

            // Found intersection point belongs to triangle
            final Transform t = frame.getTransformTo(body1.getBodyFrame(), date);
            checkPointInTriangle(t.transformPosition(actual1.getPoint()), actual1.getTriangle(), epsilon);
            checkPointInTriangle(t.transformPosition(actual2.getPoint()), actual2.getTriangle(), epsilon);

            // Intersection point lies on line of sight
            Assert.assertTrue(lineOfSight.distance(actual1.getPoint()) < epsilon);
            Assert.assertTrue(lineOfSight.distance(actual2.getPoint()) < epsilon);

            // Intersection point is between satellite and body center
            Assert.assertTrue(Vector3D.dotProduct(
                body1.getPVCoordinates(date, frame).getPosition().subtract(actual1.getPoint()),
                position1.subtract(actual1.getPoint())) < 0);
            Assert.assertTrue(Vector3D.dotProduct(
                body2.getPVCoordinates(date, frame).getPosition().subtract(actual2.getPoint()),
                position1.subtract(actual2.getPoint())) < 0);

            // Intersection point is similar between .obj and geodetic files (threshold: 131m, limited due to different
            // models)
            Assert.assertEquals(0., actual1.getPoint().distance(actual2.getPoint()), 131);
        }
    }

    /**
     * @testType VT
     *
     * @description check that neighbors are properly computed (Triangle, Geodetic and cartesian points case):
     *              <ul>
     *              <li>Check that neighbors respect the distance criterion</li>
     *              <li>Check that other triangles do not respect the distance criterion</li>
     *              </ul>
     *
     * @testPassCriteria neighbors are properly computed for various configurations (reference: math, threshold: 0)
     *
     * @referenceVersion 4.6
     *
     * @nonRegressionVersion 4.6
     */
    @Test
    public void neighborsTest() throws PatriusException {
        // Random generator
        final Random random = new Random(12345);

        // Too many take too much time
        for (int i = 0; i < 100; i++) {
            // Initialization (triangle + max distance)
            final Triangle triangle = body1.getTriangles()[i];
            final double maxDistance = random.nextDouble() * 5 * i;

            // getNeighbors(Triangle)
            final List<Triangle> actual1 = body1.getNeighbors(triangle, maxDistance);
            checkNeighborsTriangles(actual1, triangle, maxDistance);

            // getNeighbors(GeodeticPoint)
            final GeodeticPoint point2 = new GeodeticPoint(random.nextDouble() * MathLib.PI - MathLib.PI / 2.,
                random.nextDouble() * 2. * MathLib.PI, random.nextDouble() * 10000);
            final List<Triangle> actual2 = body1.getNeighbors(point2, maxDistance);
            checkNeighborsTriangles(actual2, point2, body1, maxDistance);

            // getNeighbors(Vector3D)
            final Vector3D point3 = new Vector3D(random.nextDouble() * 20000 - 10000,
                random.nextDouble() * 20000 - 10000, random.nextDouble() * 20000 - 10000);
            final List<Triangle> actual3 = body1.getNeighbors(point3, maxDistance);
            checkNeighborsTriangles(actual3, point3, body1, maxDistance);
        }
    }

    /**
     * @testType VT
     *
     * @description check that field data are properly computed for fast and standard methods with a non-circular field
     *              (here pyramidal):
     *              <ul>
     *              <li>Check both methods return the same number of triangles and found triangles are the same</li>
     *              <li>Check found triangles belong to body</li>
     *              <li>Check found triangles are in the field of view</li>
     *              <li>Check surface is the sum of all seen triangles surfaces and is identical for both methods</li>
     *              <li>Check contour:</li>
     *              <li>Check points of contour lies strictly in field of view</li>
     *              <li>Check neighbors of contour which are further from field of view are outside field of view</li>
     *              </ul>
     *
     * @testPassCriteria field data are properly computed for various
     *                   configurations (reference: math, absolute threshold is limited due to large distance
     *                   [300 000km] between point and body)
     *
     * @referenceVersion 4.6
     *
     * @nonRegressionVersion 4.6
     */
    @Test
    public void fieldDataTest() throws PatriusException {
        // Random generator
        final Random random = new Random(12345);

        // Initialization
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Frame frame = FramesFactory.getEME2000();
        final Transform transform = frame.getTransformTo(body1.getBodyFrame(), date);

        // Attitude law: body center pointing
        final AttitudeProvider attitudeProvider = new BodyCenterGroundPointing(body1);

        // Compute several field data
        for (int i = 0; i < 10; i++) {
            final Vector3D shift = new Vector3D(-0.5 + random.nextDouble(), -0.5 + random.nextDouble(), -0.5
                    + random.nextDouble()).normalize().scalarMultiply(15000);
            final Vector3D bodyPosition = body1.getPVCoordinates(date, frame).getPosition();
            final Vector3D position = bodyPosition.add(shift);
            final Orbit orbit = new CartesianOrbit(new PVCoordinates(position, Vector3D.PLUS_I), frame, date,
                Constants.EGM96_EARTH_MU);
            final Attitude attitude = attitudeProvider.getAttitude(orbit);
            final SpacecraftState state = new SpacecraftState(orbit, attitude);
            final Transform transform2 = new Transform(state.getDate(), state.getAttitude(body1.getBodyFrame())
                .getRotation());

            // Various field of view
            final IFieldOfView fieldOfView;
            if (i < 2) {
                // "Complex" field of view (pyramidal)
                fieldOfView = new PyramidalField("", new Vector3D[] {
                    Vector3D.PLUS_K.add(Vector3D.PLUS_I.scalarMultiply(0.1)),
                    Vector3D.PLUS_K.add(Vector3D.PLUS_J.scalarMultiply(0.1)),
                    Vector3D.PLUS_K.add(Vector3D.MINUS_I.scalarMultiply(0.1)),
                    Vector3D.PLUS_K.add(Vector3D.MINUS_J).scalarMultiply(0.1) });
            } else {
                fieldOfView = new CircularField("", 0.5, Vector3D.PLUS_K);
            }

            // Compute field data (2 methods, should return the same result)
            final FieldData actual1 = body1.getFieldData(state, fieldOfView, null);
            final FieldData actual2 = body1.getFieldData(state, fieldOfView, Vector3D.PLUS_K);

            // Checks

            // Both methods return the same number of triangles and are the same
            // For too wide fields, fast methods tends to miss some triangles (because of masked neighbors)
            Assert.assertEquals(actual1.getVisibleTriangles().size(), actual2.getVisibleTriangles().size());
            for (final Triangle t : actual1.getVisibleTriangles()) {
                Assert.assertTrue(actual2.getVisibleTriangles().contains(t));
            }
            for (final Triangle t : actual2.getVisibleTriangles()) {
                Assert.assertTrue(actual1.getVisibleTriangles().contains(t));
            }

            // From now on, all triangles actual1 and actual2 are the same, only actual1 data are checked

            // All triangle belong to body
            for (final Triangle t : actual1.getVisibleTriangles()) {
                Assert.assertTrue(Arrays.asList(body1.getTriangles()).contains(t));
            }

            // Found triangles are in the field of view
            for (final Triangle t : actual1.getVisibleTriangles()) {
                // Check orientation
                Assert.assertTrue(t.isVisible(transform.transformPosition(position)));
                // Check vertices are in the field
                for (final Vertex v : t.getVertices()) {
                    final Vector3D vertexDirInBodyFrame = v.getPosition().subtract(
                        transform.transformPosition(position));
                    final Vector3D vertexDirInSensorFrame = transform2.transformVector(vertexDirInBodyFrame);
                    Assert.assertTrue(fieldOfView.isInTheField(vertexDirInSensorFrame));
                }

                // Check masking
                boolean isMasked = true;
                final Frame bodyFrame = body1.getBodyFrame();
                for (final Vertex v : t.getVertices()) {
                    final Vector3D posInBodyFrame = transform.transformPosition(position);
                    final Line line = new Line(posInBodyFrame, v.getPosition());
                    final Vector3D[] intersections = body1.getIntersectionPoints(line, bodyFrame, null);
                    boolean res = false;
                    for (final Vector3D intersection : intersections) {
                        // Discard current vertex which is an obvious intersection point
                        if (intersection.distance(v.getPosition()) > 1E-6) {
                            // Vertex is hidden if intersection point is between vertex and position
                            final Vector3D v1 = v.getPosition().subtract(intersection);
                            final Vector3D v2 = posInBodyFrame.subtract(intersection);
                            res |= Vector3D.dotProduct(v1, v2) < 0;
                        }
                    }
                    isMasked &= res;
                }
                Assert.assertFalse(isMasked);
            }

            // Check surface
            // May not be strictly equal due to different sum order
            if (actual1.getVisibleSurface() == 0) {
                Assert.assertEquals(actual1.getVisibleSurface(), actual2.getVisibleSurface(), 0.);
            } else {
                Assert.assertEquals(0,
                    (actual1.getVisibleSurface() - actual2.getVisibleSurface()) / actual1.getVisibleSurface(), 1E-14);
            }
            double expectedSurface = 0;
            for (final Triangle t : actual1.getVisibleTriangles()) {
                expectedSurface += t.getSurface();
            }
            Assert.assertEquals(expectedSurface, actual1.getVisibleSurface(), 0.);

            // Check contour
            // Check contour is in the field
            // Check contour is close to field of view sides (0.062 rad, accuracy limited due to number of vertices)
            final List<GeodeticPoint> contour1 = actual1.getContour();
            final List<GeodeticPoint> contour2 = actual2.getContour();
            Assert.assertEquals(contour1.size(), contour2.size());
            for (final GeodeticPoint point : contour1) {
                final Vector3D p3D = body1.transform(point);
                final Vector3D vertexDirInBodyFrame = p3D.subtract(transform.transformPosition(position));
                final Vector3D vertexDirInSensorFrame = transform2.transformVector(vertexDirInBodyFrame);
                Assert.assertTrue(fieldOfView.isInTheField(vertexDirInSensorFrame));
                Assert.assertEquals(0., fieldOfView.getAngularDistance(vertexDirInSensorFrame), 6.2E-2);
            }
            for (final GeodeticPoint point : contour2) {
                final Vector3D p3D = body1.transform(point);
                final Vector3D vertexDirInBodyFrame = p3D.subtract(transform.transformPosition(position));
                final Vector3D vertexDirInSensorFrame = transform2.transformVector(vertexDirInBodyFrame);
                Assert.assertTrue(fieldOfView.isInTheField(vertexDirInSensorFrame));
                Assert.assertEquals(0., fieldOfView.getAngularDistance(vertexDirInSensorFrame), 6.2E-2);
            }
        }
    }

    /**
     * @testType VT
     *
     * @description check that satellite is in eclipse or not through geometry check:
     *              <ul>
     *              <li>Check there is an intersection or not. If intersecting:</li>
     *              <li>Check intersection point is consistent with eclipse status</li>
     *              <li>Check intersecting body point is between satellite and Sun</li>
     *              </ul>
     *
     * @testPassCriteria satellite is in eclipse of not for various configurations (reference: math, threshold: 0)
     *
     * @referenceVersion 4.6
     *
     * @nonRegressionVersion 4.6
     */
    @Test
    public void eclipseTest() throws PatriusException {
        // Random generator
        final Random random = new Random(12345);

        // Initialization
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Frame frame = FramesFactory.getEME2000();

        // Compute several eclipse position
        for (int i = 1; i < 1000; i++) {
            // Random position: make sure position is outside body
            final Vector3D shift = new Vector3D(-0.5 + random.nextDouble(), -0.5 + random.nextDouble(), -0.5
                    + random.nextDouble()).normalize().scalarMultiply(10000);
            final Vector3D bodyPosition = body1.getPVCoordinates(date, frame).getPosition();
            final Vector3D sunPosition = new MeeusSun().getPVCoordinates(date, frame).getPosition();
            final Vector3D position = bodyPosition.add(shift);

            // Get eclipse result
            final boolean actual = body1.isInEclipse(date, position, frame, new MeeusSun());

            // Satellite - Sun line of sight
            final Line lineOfSight = new Line(position, sunPosition);

            // Compute intersection
            final Vector3D posInFrame = frame.getTransformTo(body1.getBodyFrame(), date).transformPosition(position);
            final Intersection intersection = body1.getIntersection(lineOfSight, posInFrame, frame, date);

            // Compute orientation of satellite - Intersection point of body - Sun
            boolean properlyOriented = false;
            if (intersection != null) {
                properlyOriented = Vector3D.dotProduct(position.subtract(intersection.getPoint()),
                    sunPosition.subtract(intersection.getPoint())) < 0;
            }

            // Checks intersection is consistent with eclipse status
            Assert.assertEquals(actual, intersection != null && properlyOriented);
        }
    }

    /**
     * @testType VT
     *
     * @description check that the local altitude is properly computed:
     *              <ul>
     *              <li>At the north pole</li>
     *              <li>At the equator</li>
     *              <li>Local altitude is the same between the two available methods</li>
     *              <li>Local altitude is the same between the two bodies</li>
     *              </ul>
     *
     * @testPassCriteria local altitude is as expected (reference: math)
     *
     * @referenceVersion 4.6
     *
     * @nonRegressionVersion 4.6
     */
    @Test
    public void localAltitudeTest() throws PatriusException {
        // Method with arguments (latitude, longitude)
        // Equator
        Assert.assertEquals(12.3668E3, body2.getLocalAltitude(0, 0) + body2.getMinNorm(), 1.);
        // North pole
        Assert.assertEquals(9.5983E3, body2.getLocalAltitude(MathLib.PI / 2., 0) + body2.getMinNorm(), 1.);

        // Method with arguments (direction) - Should return exactly same results as with arguments (latitude,
        // longitude)
        // North pole
        Assert.assertEquals(body2.getLocalAltitude(MathLib.PI / 2., 0), body2.getLocalAltitude(Vector3D.PLUS_K), 1E-5);
        // Equator
        Assert.assertEquals(body2.getLocalAltitude(0, 0), body2.getLocalAltitude(Vector3D.PLUS_I), 1E-5);
    }

    /**
     * @testType VT
     *
     * @description check that the apparent radius is properly computed at the farthest point (vertex) from the center
     *              of the occulting body
     *
     * @testPassCriteria the apparent radius is as expected (reference: math)
     *
     * @referenceVersion 4.9
     *
     * @nonRegressionVersion 4.9
     */
    @Test
    public void apparentRadiusTest() throws PatriusException {
        // Take the case where the farthest point corresponds to the tangential point, because the direction
        // observer-tangentialPoint is perpendicular to the direction occulting-TangentialPoint
        // Initialize the date
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        // Initialize the frame
        final Frame frame = body2.getBodyFrame();
        // Retrieve the PV coordinates provider of the occulted body
        final PVCoordinatesProvider sunPVCoordProvider = new MeeusSun();
        // Initialize the position of the farthest point (vertex) from the center of the occulting body
        final Vector3D farthestPoint = new Vector3D(12315.476693964, -6548.255113588, 975.348967185);
        // Retrieve the position of the center of the occulting body
        final Vector3D occulting = body2.getPVCoordinates(date, frame).getPosition();
        // Create the plane containing the farthest point, the center of the occulting body and the occulted body
        final Plane plane = new Plane(farthestPoint, occulting, sunPVCoordProvider.getPVCoordinates(date, frame)
            .getPosition());
        // Compute the direction going from the farthest point to the center of the occulting body
        final Vector3D farthestOcculting = occulting.subtract(farthestPoint);
        // Compute the direction of the line going from the farthest point to the observer as the direction
        // perpendicular to both the normal axis of the plane and the direction of the line going from the farthest
        // point to the center of the occulting body
        final Vector3D farthestObserver = ((plane.getNormal().crossProduct(farthestOcculting)));
        // Check that the direction of the line going from the farthest point to the observer is contained within the
        // plane
        Assert.assertTrue(plane.contains(farthestObserver));
        // Compute the apparent radius at the farthest point with the default convergence threshold value
        final double apparentRadiusFarthestPoint = body2.getApparentRadius(new ConstantPVCoordinatesProvider(
            farthestPoint, frame), date,
            sunPVCoordProvider, PropagationDelayType.INSTANTANEOUS);
        // Check that the computed apparent radius at the farthest point is equal to the distance between the center of
        // the occulting body and the vertex which is the farthest from it
        Assert.assertEquals(0., MathLib.abs((apparentRadiusFarthestPoint - body2.getMaxNorm()) / body2.getMaxNorm()),
            3E-3);
        // Check that the setThreshold(double) method actually modifies the convergence threshold of the apparent radius
        // computation method
        body2.setThreshold(1E3);
        // Compute the apparent radius at the farthest point with the a convergence threshold value different from the
        // default one
        final double apparentRadiusFarthestPoint2 = body2.getApparentRadius(new ConstantPVCoordinatesProvider(
            farthestPoint, frame), date,
            sunPVCoordProvider, PropagationDelayType.INSTANTANEOUS);
        // Check that the values of the apparent radii computed with different convergence threshold values are
        // different
        Assert.assertTrue(apparentRadiusFarthestPoint2 != apparentRadiusFarthestPoint);
        // Reset the convergence threshold value of the apparent radius computation method to the default one
        body2.setThreshold(1E-2);

        // Check convergence exception case
        final int maxNSteps = 10;
        body2.setMaxApparentRadiusSteps(maxNSteps);
        try {
            body2.getApparentRadius(new ConstantPVCoordinatesProvider(farthestPoint, frame), date, sunPVCoordProvider,
                PropagationDelayType.INSTANTANEOUS);
            Assert.fail();
        } catch (final MaxCountExceededException maxCountException) {
            Assert.assertEquals(
                new MaxCountExceededException(PatriusMessages.CONVERGENCE_FAILED, maxNSteps).getMessage(),
                maxCountException.getMessage());
        }
        // Reset the max number of steps
        body2.setMaxApparentRadiusSteps(100);

        // Light speed case
        final double apparentRadiusLS = body2.getApparentRadius(new ConstantPVCoordinatesProvider(
            farthestPoint, frame), date,
            sunPVCoordProvider, PropagationDelayType.LIGHT_SPEED);
        // Non-regression only
        Assert.assertEquals(13982.19484791212, apparentRadiusLS, 0.);
    }

    /**
     * @testType UT
     *
     * @description check that min/max norm of body is properly computed
     *
     * @testPassCriteria min/max norm is as expected (10000m) (reference: math)
     *
     * @referenceVersion 4.7
     *
     * @nonRegressionVersion 4.7
     */
    @Test
    public void minmaxNormTest() {
        Assert.assertEquals(0., (body1.getMinNorm() - EXPECTED_MIN) / EXPECTED_MIN, EPS);
        Assert.assertEquals(0., (body1.getMaxNorm() - EXPECTED_MAX) / EXPECTED_MAX, EPS);
    }

    /**
     * @testType UT
     *
     * @description check that the inner ellipsoid is properly computed
     *
     * @testPassCriteria the inner ellipsoid is as expected (reference: math)
     *
     * @referenceVersion 4.9
     *
     * @nonRegressionVersion 4.9
     */
    @Test
    public void innerEllipsoidTest() {
        // Check the non-regression of the equatorial radius of the inner ellipsoid.
        Assert.assertEquals(0.,
            (body1.getEllipsoid(EllipsoidType.INNER_ELLIPSOID).getEquatorialRadius() - EXPECTED_INNER_RADIUS)
                / EXPECTED_INNER_RADIUS, EPS);
        // Check that the flattening of the inner ellipsoid has the same flattening as the fitted ellipsoid
        Assert.assertEquals(0.,
            (body1.getEllipsoid(EllipsoidType.INNER_ELLIPSOID).getFlattening() - body1.getEllipsoid(
                EllipsoidType.FITTED_ELLIPSOID)
                .getFlattening()) / body1.getEllipsoid(EllipsoidType.FITTED_ELLIPSOID).getFlattening(), EPS);
        // Check that the inner ellipsoid is inscribed within all the vertices
        // Retrieve the map of vertices
        final Map<Integer, Vertex> verticesMap = body1.getMeshProvider().getVertices();
        // Loop on all the vertices
        int counter = 0;
        for (final Vertex vertex : verticesMap.values()) {
            // Check that the inner ellipsoid polar radius is smaller than the current vertex
            // distance to center
            Assert.assertTrue(body1.getEllipsoid(EllipsoidType.INNER_ELLIPSOID).getEquatorialRadius()
                    * (1 - body1.getEllipsoid(EllipsoidType.INNER_ELLIPSOID).getFlattening()) <= vertex.getPosition()
                .getNorm());
            // Increment the counter if the vertex distance to the origin is smaller than the
            // equatorial radius
            if (body1.getEllipsoid(EllipsoidType.OUTER_ELLIPSOID).getEquatorialRadius() >= vertex.getPosition()
                .getNorm()) {
                counter += 1;
            }
        }
        // Check that at least one vertex has a distance to the origin smaller than the equatorial
        // radius
        Assert.assertTrue(counter > 0);
    }

    /**
     * @testType UT
     *
     * @description check that the outer ellipsoid is properly computed
     *
     * @testPassCriteria the outer ellipsoid is as expected (reference: math)
     *
     * @referenceVersion 4.9
     *
     * @nonRegressionVersion 4.9
     */
    @Test
    public void outerEllipsoidTest() {
        // Check the non-regression of the equatorial radius of the outer ellipsoid.
        Assert.assertEquals(0.,
            (body1.getEllipsoid(EllipsoidType.OUTER_ELLIPSOID).getEquatorialRadius() - EXPECTED_OUTER_RADIUS)
                / EXPECTED_OUTER_RADIUS, EPS);
        // Check that the flattening of the outer ellipsoid has the same flattening as the fitted ellipsoid
        Assert.assertEquals(0.,
            (body1.getEllipsoid(EllipsoidType.OUTER_ELLIPSOID).getFlattening() - body1.getEllipsoid(
                EllipsoidType.FITTED_ELLIPSOID).getFlattening())
                    / body1.getEllipsoid(EllipsoidType.FITTED_ELLIPSOID).getFlattening(), EPS);
        // Check that the outer ellipsoid is englobing all the vertices
        // Retrieve the map of vertices
        final Map<Integer, Vertex> verticesMap = body1.getMeshProvider().getVertices();
        // Loop on all the vertices
        int counter = 0;
        for (final Vertex vertex : verticesMap.values()) {
            // Check that the outer ellipsoid equatorial radius is larger than the current vertex
            // distance to center
            Assert.assertTrue(body1.getEllipsoid(EllipsoidType.OUTER_ELLIPSOID).getEquatorialRadius() >= vertex
                .getPosition().getNorm());
            if (body1.getEllipsoid(EllipsoidType.OUTER_ELLIPSOID).getEquatorialRadius()
                    * (1 - body1.getEllipsoid(EllipsoidType.OUTER_ELLIPSOID).getFlattening()) <= vertex
                .getPosition().getNorm()) {
                // Increment the counter if the vertex distance to the origin is larger than the
                // polar radius
                counter += 1;
            }
        }
        // Check that at least one vertex has a larger norm than the polar radius
        Assert.assertTrue(counter > 0);
    }

    /**
     * @testType UT
     *
     * @description check that the inner sphere is properly computed
     *
     * @testPassCriteria the inner sphere is as expected (reference: math)
     *
     * @referenceVersion 4.9
     *
     * @nonRegressionVersion 4.9
     */
    @Test
    public void innerSphereTest() {
        // Check that the radius of the inner sphere coincides with the expected minimum value of the distance between
        // the vertices and the center of the body, i.e. with the norm of the position of the closest vertex
        Assert.assertEquals(0., (body1.getEllipsoid(EllipsoidType.INNER_SPHERE).getEquatorialRadius() - EXPECTED_MIN)
                / EXPECTED_MIN, EPS);
        // Check that the flattening of the inner sphere is zero
        Assert.assertEquals(0., body1.getEllipsoid(EllipsoidType.INNER_SPHERE).getFlattening(), EPS);
        // Check that the inner sphere is inscribed within all the vertices
        // Retrieve the map of vertices
        final Map<Integer, Vertex> verticesMap = body1.getMeshProvider().getVertices();
        // Loop on all the vertices
        for (final Vertex vertex : verticesMap.values()) {
            // Check that the inner sphere is inscribed within the current vertex
            Assert.assertTrue(body1.getEllipsoid(EllipsoidType.INNER_SPHERE).getEquatorialRadius() <= vertex
                .getPosition().getNorm());
        }
    }

    /**
     * @testType UT
     *
     * @description check that the outer sphere is properly computed
     *
     * @testPassCriteria the outer sphere is as expected (reference: math)
     *
     * @referenceVersion 4.9
     *
     * @nonRegressionVersion 4.9
     */
    @Test
    public void outerSphereTest() {
        // Check that the radius of the outer sphere coincides with the expected maximum value of the distance between
        // the vertices and the center of the body, i.e. with the norm of the position of the farthest vertex
        Assert.assertEquals(0., (body1.getEllipsoid(EllipsoidType.OUTER_SPHERE).getEquatorialRadius() - EXPECTED_MAX)
                / EXPECTED_MAX, EPS);
        // Check that the flattening of the outer sphere is zero
        Assert.assertEquals(0., body1.getEllipsoid(EllipsoidType.OUTER_SPHERE).getFlattening(), EPS);
        // Check that the outer sphere is englobing all the vertices
        // Retrieve the map of vertices
        final Map<Integer, Vertex> verticesMap = body1.getMeshProvider().getVertices();
        // Loop on all the vertices
        for (final Vertex vertex : verticesMap.values()) {
            // Check that the outer sphere is englobing the current vertex
            Assert.assertTrue(body1.getEllipsoid(EllipsoidType.OUTER_SPHERE).getEquatorialRadius() >= vertex
                .getPosition().getNorm());
        }
    }

    /**
     * @throws PatriusException
     * @throws URISyntaxException
     * @testType UT
     *
     * @description check that Tchouri is not accepted as a StarConvexBodyShape.
     *
     * @testPassCriteria the starConvexBodyShape object is not constructed and an exception is thrown
     *
     * @referenceVersion 4.11
     */
    @Test(expected = PatriusException.class)
    public void StarConvexFacetBodyShapeConstructorTchouriTest() throws PatriusException, URISyntaxException {
        // Load .obj mesh for Tchouri
        final String modelFile67P = "mnt" + File.separator + "67P_Tchouri.obj";
        final String fullName67P = FacetBodyShape.class.getClassLoader().getResource(modelFile67P).toURI().getPath();

        final StarConvexFacetBodyShape phobosStarConv = new StarConvexFacetBodyShape("tchouri", body67P.getBodyFrame(),
            EllipsoidType.FITTED_ELLIPSOID, new ObjMeshLoader(fullName67P));
    }

    /**
     * @testType UT
     *
     * @description check that 67P/Tchouri is not convex, but Phobos is.
     *
     * @testPassCriteria the maxSlopes returned by {@link FacetBodyShape#getMaxSlope()} and the list
     *                   returned by {@link FacetBodyShape#getOverPerpendicularSteepFacets()} are
     *                   coherent.
     *
     * @referenceVersion 4.11
     *
     * @nonRegressionVersion 4.11
     */
    @Test
    public void getOverPerpendicularSteepFacetsTest() {
        // Check the maxSlope values of 67P/Tchouri and Phobos
        // Phobos maxSlope must be under 90° since it has a star convex shape.
        Assert.assertEquals(1.0180478445890895, body1.getMaxSlope(), 0.);
        // Tchouri maxSlope must be over 90°.
        Assert.assertEquals(2.969726637562819, body67P.getMaxSlope(), 0.);
        // Check the lists of the facets that are over perpendicular steep (over 90° slope) of 67P/Tchouri and Phobos
        // Phobos should have no facet with a slope over 90° since it has a star convex shape.
        Assert.assertEquals(0, body1.getOverPerpendicularSteepFacets().size());
        // Tchouri must have facets over 90° steep.
        Assert.assertEquals(12444, body67P.getOverPerpendicularSteepFacets().size());
    }

    @Test
    public void problemPhobosM1Test() {
        final List<Triangle> list = body2.getOverPerpendicularSteepFacets();
        Assert.assertEquals(30, list.size());
    }

    /**
     * Numerical quality issue test on intersection computation.
     */
    @Test
    public void fa50Test() throws PatriusException {

        // File path to Phobos_Ernst_200K.obj
        final String filePath = "src" + File.separator + "test" + File.separator + "resources" + File.separator
                + "mnt" + File.separator + "Phobos_Ernst_200K.obj";
        final ObjMeshLoader loader = new ObjMeshLoader(filePath);
        final FacetBodyShape facetShape = new FacetBodyShape("facet body shape", FramesFactory.getITRF(),
                EllipsoidType.INNER_SPHERE, loader);

        // input planetocentric lat/long
        final double planetocentricLatitude = 0.04008717791700243;
        final double planetocentricLongitude = 1.3591551872957268;

        // longitude, cosine and sine
        final double[] sincosLon = MathLib.sinAndCos(planetocentricLongitude);
        final double sinLon = sincosLon[0];
        final double cosLon = sincosLon[1];

        // latitude, cosine and sine
        final double[] sincosLat = MathLib.sinAndCos(planetocentricLatitude);
        final double sinLat = sincosLat[0];
        final double cosLat = sincosLat[1];

        // define a position outside the shape with entered lat/long
        final Vector3D positionKO = new Vector3D(cosLat * cosLon, cosLat * sinLon, sinLat).scalarMultiply(facetShape.getMaxNorm() * 2);

        // half line from origin towards positionKO
        final Line halfLineKO = new Line(Vector3D.ZERO, positionKO, Vector3D.ZERO);

        // compute intersection with shape
        final GeodeticPoint intersectKO = facetShape.getIntersectionPoint(halfLineKO, positionKO, facetShape.getBodyFrame(), null);
        // => intersectKO is null

        // Assertion KO (should not be null)
        Assert.assertNotNull(intersectKO);
    }

    /**
     * Checks provided point belongs to provided triangle.
     * Limited accuracy due to far stretch line of sight (about 300 000km).
     *
     * @param point a point
     * @param triangle a triangle
     * @param epsilon epsilon
     */
    private static void checkPointInTriangle(final Vector3D point, final Triangle triangle, final double epsilon) {
        final Vector3D v1 = point.subtract(triangle.getCenter());
        final double angle = MathLib.abs(Vector3D.angle(v1, triangle.getNormal()) - MathLib.PI / 2.);
        Assert.assertEquals(0, angle, 1E-7);
    }

    /**
     * Check that provided list of triangles is within provided distance of provided triangle.
     *
     * @param triangles triangles list to check
     * @param triangle triangle
     * @param maxDistance max distance
     */
    private static void checkNeighborsTriangles(final List<Triangle> triangles, final Triangle triangle,
                                                final double maxDistance) {
        for (final Triangle t : triangles) {
            Assert.assertTrue(t.getCenter().distance(triangle.getCenter()) <= maxDistance);
        }
    }

    /**
     * Check that provided list of triangles is within provided distance of provided geodetic point.
     *
     * @param triangles triangles list to check
     * @param point geodetic point
     * @param body body
     * @param maxDistance max distance
     */
    private static void checkNeighborsTriangles(final List<Triangle> triangles, final GeodeticPoint point,
                                                final FacetBodyShape body, final double maxDistance) {
        final Vector3D position = body.transform(point);
        for (final Triangle t : triangles) {
            Assert.assertTrue(t.getCenter().distance(position) <= maxDistance);
        }
    }

    /**
     * Check that provided list of triangles is within provided distance of provided 3D point.
     *
     * @param triangles triangles list to check
     * @param point 3D point
     * @param body body
     * @param maxDistance max distance
     */
    private static void checkNeighborsTriangles(final List<Triangle> triangles, final Vector3D point,
                                                final FacetBodyShape body, final double maxDistance) {
        for (final Triangle t : triangles) {
            Assert.assertTrue(t.getCenter().distance(point) <= maxDistance);
        }
    }
}
