/**
 * 
 * Copyright 2011-2017 CNES
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
* VERSION:4.7:DM:DM-2870:18/05/2021:Complements pour la manipulation des coordonnees cart.
* VERSION:4.7:FA:FA-2821:18/05/2021:Refus injustifié de calcul de l incidence solaire lorsqu elle dépasse 90 degres 
* VERSION:4.6:DM:DM-2544:27/01/2021:Ajouter la definition d'un corps celeste a partir d'un modele de forme 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies.mesh;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
import fr.cnes.sirius.patrius.bodies.MeeusSun;
import fr.cnes.sirius.patrius.fieldsofview.CircularField;
import fr.cnes.sirius.patrius.fieldsofview.IFieldOfView;
import fr.cnes.sirius.patrius.fieldsofview.PyramidalField;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Validation tests for {@link FacetCelestialBody} class.
 * 
 * @author Emmanuel Bignon
 * 
 * @since 4.6
 */
public class FacetCelestialBodyValTest {

    /** Epsilon for double comparison. */
    private static final double EPS = 1E-14;

    /** Facet celestial body used for tests: Phobos mesh in Moon position. */
    private static FacetCelestialBody body1;

    /** Facet celestial body used for tests: Phobos mesh in Moon position. */
    private static FacetCelestialBody body2;

    /**
     * Load mesh.
     */
    @BeforeClass
    public static void setUpBeforeClass() throws PatriusException, IOException, URISyntaxException {
        // Patrius data set
        Utils.setDataRoot("regular-dataPBASE");

        // Load .obj mesh
        final String modelFile1 = "mnt" + File.separator + "Phobos_Ernst_HD.obj";
        final String fullName1 = FacetCelestialBody.class.getClassLoader().getResource(modelFile1).toURI().getPath();
        body1 = new FacetCelestialBody("", CelestialBodyFactory.getMoon(), 0, IAUPoleFactory.getIAUPole(null),
                new ObjMeshLoader(fullName1));

        // Load geodetic mesh and validate .obj file writing
        final String modelFile2 = "mnt" + File.separator + "m1phobos.tab";
        final String fullName2 = FacetCelestialBody.class.getClassLoader().getResource(modelFile2).toURI().getPath();
        final GeodeticMeshLoader loader2 = new GeodeticMeshLoader(fullName2);
        loader2.toObjFile("m1phobos.obj");
        final ObjMeshLoader loader3 = new ObjMeshLoader("m1phobos.obj");
        body2 = new FacetCelestialBody("", CelestialBodyFactory.getMoon(), 0, IAUPoleFactory.getIAUPole(null),
                loader3);
        Assert.assertEquals(loader2.getVertices().size(), loader3.getVertices().size()); 
        Assert.assertEquals(loader2.getTriangles().length, loader3.getTriangles().length); 
    }

    /**
     * @testType VT
     * 
     * @description check that body surface is as expected.
     * 
     * @testPassCriteria body surface is equal between the two mesh (threshold: 1E-2, limited due to different number of vertices)
     * 
     * @referenceVersion 4.6
     * 
     * @nonRegressionVersion 4.6
     */
    @Test
    public final void surfaceTest() {
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
     * @testPassCriteria distance from line to body is properly computed (reference: math, threshold: 0)
     * 
     * @referenceVersion 4.6
     * 
     * @nonRegressionVersion 4.6
     */
    @Test
    public final void distanceToTest() throws PatriusException {
        // Initialization
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Frame frame = FramesFactory.getEME2000();
        
        // Compute several distance to lines
        // Too many takes to much time
        for (int i = 680; i < 720; i++) {
            // Earth - Phobos line of sight with changing shift (20 first lines cross body, 20 last lines do not)
            final Vector3D position1 = Vector3D.ZERO;
            final Vector3D position2 = body1.getPVCoordinates(date, frame).getPosition().add(new Vector3D(20 * i, 20 * i, 20 * i));
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
            
            // Check - Threshold is "high" since algorithm is not exact for not-convex bodies
            //System.out.println(i + " " + minDistanceAct + " " + minDistanceRef + " " + (minDistanceAct - minDistanceRef));
            Assert.assertEquals(minDistanceRef, minDistanceAct, 80.);
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
    public final void intersectionTest() throws PatriusException {
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
            
            // Checks
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
            Assert.assertTrue(Vector3D.dotProduct(body1.getPVCoordinates(date, frame).getPosition().subtract(actual1.getPoint()), position1.subtract(actual1.getPoint())) < 0);
            Assert.assertTrue(Vector3D.dotProduct(body2.getPVCoordinates(date, frame).getPosition().subtract(actual2.getPoint()), position1.subtract(actual2.getPoint())) < 0);
            
            // Intersection point is similar between .obj and geodetic files (threshold: 131m, limited due to different models)
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
    public final void neighborsTest() throws PatriusException {
        // Random generator
        final Random random = new Random(12345);

        for (int i = 0; i < 1000; i++) {
            // Initialization (triangle + max distance)
            final Triangle triangle = body1.getTriangles()[i];
            final double maxDistance = random.nextDouble() * 5 * i;

            // getNeighbors(Triangle)
            final List<Triangle> actual1 = body1.getNeighbors(triangle, maxDistance);
            checkNeighborsTriangles(actual1, triangle, maxDistance);

            // getNeighbors(GeodeticPoint)
            final GeodeticPoint point2 = new GeodeticPoint(random.nextDouble() * MathLib.PI - MathLib.PI / 2., random.nextDouble() * 2. * MathLib.PI, random.nextDouble() * 10000);
            final List<Triangle> actual2 = body1.getNeighbors(point2, maxDistance);
            checkNeighborsTriangles(actual2, point2, body1, maxDistance);

            // getNeighbors(Vector3D)
            final Vector3D point3 = new Vector3D(random.nextDouble() * 20000 - 10000, random.nextDouble() * 20000 - 10000, random.nextDouble() * 20000 - 10000);
            final List<Triangle> actual3 = body1.getNeighbors(point3, maxDistance);
            checkNeighborsTriangles(actual3, point3, body1, maxDistance);
        }
    }

    /**
     * @testType VT
     * 
     * @description check that surface pointed data are properly computed in different cases:
     *              <ul>
     *              <li>Line of sight intersects with body and Sun is visible from the target triangle</li>
     *              <li>Line of sight intersects with body and Sun is not visible from the target triangle</li>
     *              <li>No intersection: exception thrown</li>
     *              </ul>
     * 
     * @testPassCriteria returned field data are as expected (reference: math, threshold 1E-4, limited due to large distance
     *                   [300 000km] between point and body))
     * 
     * @referenceVersion 4.6
     * 
     * @nonRegressionVersion 4.6
     */
    @Test
    public final void surfacePointedDataTest() throws PatriusException {
        // Random generator
        final Random random = new Random(12345);

        // Initialization
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Frame frame = FramesFactory.getEME2000();
        final Transform transform = frame.getTransformTo(body1.getBodyFrame(), date);
        final double pixelFOV = 0.1;

        // Attitude law: body center pointing
        final AttitudeProvider attitudeProvider = new BodyCenterGroundPointing(body1);

        // Compute several surface data
        for (int i = 0; i < 1000; i++) {
            final Vector3D shift = new Vector3D(-0.5 + random.nextDouble(), -0.5 + random.nextDouble(), -0.5 + random.nextDouble()).normalize().scalarMultiply(15000);
            final Vector3D bodyPosition = body1.getPVCoordinates(date, frame).getPosition();
            final Vector3D position = bodyPosition.add(shift);
            final Orbit orbit = new CartesianOrbit(new PVCoordinates(position, Vector3D.PLUS_I), frame, date, Constants.EGM96_EARTH_MU);
            final Attitude attitude = attitudeProvider.getAttitude(orbit);
            final SpacecraftState state = new SpacecraftState(orbit, attitude);
            final Transform transform2 = new Transform(state.getDate(), state.getAttitude(frame).getRotation());
            final List<SpacecraftState> states = new ArrayList<SpacecraftState>();
            states.add(state);

            // Compute surface data
            final List<SurfacePointedData> actualEphemeris = body1.getSurfacePointedDataEphemeris(states, Vector3D.PLUS_K, new MeeusSun(), pixelFOV);
            
            // Checks
            Assert.assertEquals(states.size(), actualEphemeris.size());
            final SurfacePointedData actual = actualEphemeris.get(0);
            
            // Check intersection point (accuracy limited due to large distance between spacecraft and body)
            final Vector3D dirInPosFrame = transform2.getInverse().transformVector(Vector3D.PLUS_K);
            final Line lineOfSight = new Line(position, position.add(dirInPosFrame));
            final Intersection intersection = body1.getIntersection(lineOfSight, position, frame, date);
            Assert.assertEquals(intersection.getTriangle(), actual.getIntersection().getTriangle());
            Assert.assertEquals(0., transform.transformPosition(intersection.getPoint()).distance(actual.getIntersection().getPoint()), 3E-4);

            // Check distance to point
            final double expectedDistance = intersection.getPoint().subtract(position).getNorm();
            Assert.assertEquals(0., expectedDistance - actual.getDistance(), 2E-4);
            
            // Check incidence (should get same results in body and EME2000 frames)
            final double expectedIncidence = Vector3D.angle(intersection.getTriangle().getNormal().negate(), dirInPosFrame);
            Assert.assertEquals(0., (expectedIncidence - actual.getIncidence()) / expectedIncidence, 1E-5);

            // Check solar incidence (should get same results in body and EME2000 frames)
            final Vector3D sunPos = new MeeusSun().getPVCoordinates(date, frame).getPosition();
            final Vector3D sunVectorInPosFrame = intersection.getPoint().subtract(sunPos);
            final double expectedSolarIncidence = Vector3D.angle(intersection.getTriangle().getNormal().negate(), sunVectorInPosFrame);
            Assert.assertEquals(0., (expectedSolarIncidence - actual.getSolarIncidence()) / expectedSolarIncidence, 1E-5);
            
            // Check phase angle (should get same results in body and EME2000 frames)
            final double expectedPhaseAngle = Vector3D.angle(dirInPosFrame, sunVectorInPosFrame);
            if (expectedSolarIncidence > MathLib.PI / 2.) {
                Assert.assertTrue(Double.isNaN(actual.getPhaseAngle()));
            } else {
                Assert.assertEquals(0., (expectedPhaseAngle - actual.getPhaseAngle()) / expectedPhaseAngle, 1E-5);
            }
            
            // Check resolution
            final double expectedResolution = actual.getDistance() * MathLib.cos(actual.getIncidence())
                    * (MathLib.tan(actual.getIncidence() + pixelFOV / 2) - MathLib.tan(actual.getIncidence() - pixelFOV / 2));
            Assert.assertEquals(expectedResolution, actual.getResolution());
        }
    }

    /**
     * @testType VT
     * 
     * @description check that field data are properly computed for fast and standard methods with a non-circular field (here pyramidal):
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
    public final void fieldDataTest() throws PatriusException {
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
            final Vector3D shift = new Vector3D(-0.5 + random.nextDouble(), -0.5 + random.nextDouble(), -0.5 + random.nextDouble()).normalize().scalarMultiply(15000);
            final Vector3D bodyPosition = body1.getPVCoordinates(date, frame).getPosition();
            final Vector3D position = bodyPosition.add(shift);
            final Orbit orbit = new CartesianOrbit(new PVCoordinates(position, Vector3D.PLUS_I), frame, date, Constants.EGM96_EARTH_MU);
            final Attitude attitude = attitudeProvider.getAttitude(orbit);
            final SpacecraftState state = new SpacecraftState(orbit, attitude);
            final Transform transform2 = new Transform(state.getDate(), state.getAttitude(body1.getBodyFrame()).getRotation());

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
                    final Vector3D vertexDirInBodyFrame = v.getPosition().subtract(transform.transformPosition(position));
                    final Vector3D vertexDirInSensorFrame = transform2.transformVector(vertexDirInBodyFrame);
                    Assert.assertTrue(fieldOfView.isInTheField(vertexDirInSensorFrame));
                }

                // Check masking
                boolean isMasked = true;
                for (final Vertex v : t.getVertices()) {
                    final Vector3D posInBodyFrame = transform.transformPosition(position);
                    final Line line = new Line(posInBodyFrame, v.getPosition());
                    final Vector3D[] intersections = body1.getIntersectionPoints(line, body1.getBodyFrame(), null);
                    boolean res = false;
                    for (int j = 0; j < intersections.length; j++) {
                        // Discard current vertex which is an obvious intersection point
                        if (intersections[j].distance(v.getPosition()) > 1E-6) {
                            // Vertex is hidden if intersection point is between vertex and position
                            final Vector3D v1 = v.getPosition().subtract(intersections[j]);
                            final Vector3D v2 = posInBodyFrame.subtract(intersections[j]);
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
                Assert.assertEquals(0, (actual1.getVisibleSurface() - actual2.getVisibleSurface()) / actual1.getVisibleSurface(), 1E-14);
            }
            double expectedSurface = 0;
            for (final Triangle t : actual1.getVisibleTriangles()) {
                expectedSurface += t.getSurface();
            }
            Assert.assertEquals(expectedSurface, actual1.getVisibleSurface(), 0.);
            
            // Check contour
            // Check contour is in the field
            // Check contour is close to field of view sides (0.04 rad, accuracy limited due to number of vertices)
            final List<GeodeticPoint> contour1 = actual1.getContour();
            final List<GeodeticPoint> contour2 = actual2.getContour();
            Assert.assertEquals(contour1.size(), contour2.size());
            for (final GeodeticPoint point : contour1) {
                final Vector3D p3D = body1.transform(point);
                final Vector3D vertexDirInBodyFrame = p3D.subtract(transform.transformPosition(position));
                final Vector3D vertexDirInSensorFrame = transform2.transformVector(vertexDirInBodyFrame);
                Assert.assertTrue(fieldOfView.isInTheField(vertexDirInSensorFrame));
                Assert.assertEquals(0., fieldOfView.getAngularDistance(vertexDirInSensorFrame), 5E-2);
            }
            for (final GeodeticPoint point : contour2) {
                final Vector3D p3D = body1.transform(point);
                final Vector3D vertexDirInBodyFrame = p3D.subtract(transform.transformPosition(position));
                final Vector3D vertexDirInSensorFrame = transform2.transformVector(vertexDirInBodyFrame);
                Assert.assertTrue(fieldOfView.isInTheField(vertexDirInSensorFrame));
                Assert.assertEquals(0., fieldOfView.getAngularDistance(vertexDirInSensorFrame), 5E-2);
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
    public final void eclipseTest() throws PatriusException {
        // Random generator
        final Random random = new Random(12345);

        // Initialization
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Frame frame = FramesFactory.getEME2000();
        
        // Compute several eclipse position
        for (int i = 1; i < 1000; i++) {
            // Random position: make sure position is outside body
            final Vector3D shift = new Vector3D(-0.5 + random.nextDouble(), -0.5 + random.nextDouble(), -0.5 + random.nextDouble()).normalize().scalarMultiply(10000);
            final Vector3D bodyPosition = body1.getPVCoordinates(date, frame).getPosition();
            final Vector3D sunPosition = new MeeusSun().getPVCoordinates(date, frame).getPosition();
            final Vector3D position = bodyPosition.add(shift);

            // Get eclipse result
            final boolean actual = body1.isInEclipse(date, position, frame, new MeeusSun());
            
            // Satellite - Sun line of sight
            final Line lineOfSight = new Line(position, sunPosition);

            // Compute intersection
            final Intersection intersection = body1.getIntersection(lineOfSight, position, frame, date);

            // Compute orientation of satellite - Intersection point of body - Sun
            boolean properlyOriented = false;
            if (intersection != null) {
                properlyOriented = Vector3D.dotProduct(position.subtract(intersection.getPoint()), sunPosition.subtract(intersection.getPoint())) < 0;
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
    public final void localAltitudeTest() throws PatriusException {
        // Method with arguments (latitude, longitude)
        // Equator
        Assert.assertEquals(12.3668E3, body2.getLocalAltitude(0, 0) + body2.getMinNorm(), 1.);
        // North pole
        Assert.assertEquals(9.5983E3, body2.getLocalAltitude(MathLib.PI / 2., 0) + body2.getMinNorm(), 1.);

        // Method with arguments (direction) - Should return exactly same results as with arguments (latitude, longitude)
        // North pole
        Assert.assertEquals(body2.getLocalAltitude(MathLib.PI / 2., 0), body2.getLocalAltitude(Vector3D.PLUS_K), 1E-5);
        // Equator
        Assert.assertEquals(body2.getLocalAltitude(0, 0), body2.getLocalAltitude(Vector3D.PLUS_I), 1E-5);
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
    public final void minmaxNormTest() throws PatriusException {
        final double expectedMin = 8.08439663366414E3;
        final double expectedMax = 13.9663150079038E3;
        Assert.assertEquals(0, (expectedMin - body1.getMinNorm()) / expectedMin, EPS);
        Assert.assertEquals(0, (expectedMax - body1.getMaxNorm()) / expectedMax, EPS);
    }

    /**
     * Checks provided point belongs to provided triangle.
     * Limited accuracy due to far stretch line of sight (about 300 000km).
     * @param point a point
     * @param triangle a triangle
     * @param epsilon epsilon
     */
    private void checkPointInTriangle(final Vector3D point, final Triangle triangle, final double epsilon) {
        final Vector3D v1 = point.subtract(triangle.getCenter());
        final double angle = MathLib.abs(Vector3D.angle(v1, triangle.getNormal()) - MathLib.PI / 2.);
        Assert.assertEquals(0, angle, 1E-7);
    }

    /**
     *  Check that provided list of triangles is within provided distance of provided triangle.
     * @param triangles triangles list to check
     * @param triangle triangle
     * @param maxDistance max distance
     */
    private void checkNeighborsTriangles(final List<Triangle> triangles, final Triangle triangle, final double maxDistance) {
        for (final Triangle t : triangles) {
            Assert.assertTrue(t.getCenter().distance(triangle.getCenter()) <= maxDistance);
        }
    }

    /**
     *  Check that provided list of triangles is within provided distance of provided geodetic point.
     * @param triangles triangles list to check
     * @param point geodetic point
     * @param body body
     * @param maxDistance max distance
     */
    private void checkNeighborsTriangles(final List<Triangle> triangles, final GeodeticPoint point, final FacetCelestialBody body, final double maxDistance) {
        final Vector3D position = body.transform(point);
        for (final Triangle t : triangles) {
            Assert.assertTrue(t.getCenter().distance(position) <= maxDistance);
        }
    }

    /**
     *  Check that provided list of triangles is within provided distance of provided 3D point.
     * @param triangles triangles list to check
     * @param point 3D point
     * @param body body
     * @param maxDistance max distance
     */
    private void checkNeighborsTriangles(final List<Triangle> triangles, final Vector3D point, final FacetCelestialBody body, final double maxDistance) {
        for (final Triangle t : triangles) {
            Assert.assertTrue(t.getCenter().distance(point) <= maxDistance);
        }
    }
}
