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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.geometry.euclidean.threed;

import junit.framework.Assert;

import org.junit.Test;

import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;

/**
 * @description <p>
 *              Validation tests for the object Sphere.
 *              </p>
 * 
 * @author Thomas Trapier
 * 
 * @version $Id: SphereTest.java 17909 2017-09-11 11:57:36Z bignon $
 * 
 * @since 1.0
 * 
 */
public class SphereTest {

    /** Features description. */
    public enum features {

        /**
         * @featureTitle Sphere shape
         * 
         * @featureDescription Creation of a spherical shape, computation of distances and intersections with lines and
         *                     points.
         * 
         * @coveredRequirements DV-GEOMETRIE_50, DV-GEOMETRIE_60, DV-GEOMETRIE_90, DV-GEOMETRIE_120, DV-GEOMETRIE_130,
         *                      DV-GEOMETRIE_140
         */
        SPHERE_SHAPE
    }

    /** Epsilon for double comparison. */
    private final double comparisonEpsilon = Precision.DOUBLE_COMPARISON_EPSILON;

    /** Epsilon taking into account the machine error. */
    private final double machineEpsilon = Precision.EPSILON;

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#SPHERE_SHAPE}
     * 
     * @testedMethod {@link Sphere#Sphere(Vector3D, double)}
     * 
     * @description instantiation of a sphere from its center and radius.
     * 
     * @input a point center, several double as radiuses (positive, negative, null)
     * 
     * @output Sphere
     * 
     * @testPassCriteria The sphere can be created only if the radius is positive or null, an exception is thrown
     *                   otherwise. We check the returned elements with the ones given at the construction with an
     *                   epsilon of 1e-16 which takes into account the machine error only.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testConstructor() {

        final Vector3D center = new Vector3D(1.0, 1.0, 1.0);

        // positive radius
        final Sphere sphere1 = new Sphere(center, 1.0);
        Assert.assertEquals(sphere1.getRadius(), 1.0, this.machineEpsilon);
        final Vector3D centerOut = sphere1.getCenter();
        Assert.assertEquals(centerOut.getX(), 1.0, this.machineEpsilon);
        Assert.assertEquals(centerOut.getY(), 1.0, this.machineEpsilon);
        Assert.assertEquals(centerOut.getZ(), 1.0, this.machineEpsilon);

        Assert.assertEquals(sphere1.getSemiA(), 1.0, this.machineEpsilon);
        Assert.assertEquals(sphere1.getSemiB(), 1.0, this.machineEpsilon);
        Assert.assertEquals(sphere1.getSemiC(), 1.0, this.machineEpsilon);

        final Vector3D point = new Vector3D(1.0, 4.0, 1.0);
        final Vector3D normal = sphere1.getNormal(point);
        Assert.assertEquals(normal.getX(), 0.0, this.machineEpsilon);
        Assert.assertEquals(normal.getY(), 1.0, this.machineEpsilon);
        Assert.assertEquals(normal.getZ(), 0.0, this.machineEpsilon);

        final Vector3D clostetPoint = sphere1.closestPointTo(point);
        Assert.assertEquals(clostetPoint.getX(), 1.0, this.machineEpsilon);
        Assert.assertEquals(clostetPoint.getY(), 2.0, this.machineEpsilon);
        Assert.assertEquals(clostetPoint.getZ(), 1.0, this.machineEpsilon);

        // null radius
        try {
            new Sphere(center, 0.0);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected !
        }

        // negative radius
        try {
            new Sphere(center, -1.0);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected !
        }

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#SPHERE_SHAPE}
     * 
     * @testedMethod {@link Sphere#distanceTo(Vector3D)}
     * 
     * @description Compute the shortest distance between the surface of the sphere and any point of space.
     * 
     * @input Points of space
     * 
     * @output doubles : the distances
     * 
     * @testPassCriteria The output doubles must be the right distance with an epsilon of 1e-14 due to the computation
     *                   errors, positive if the point is outside the sphere, zero if the point belongs to the surface,
     *                   negative if the point is inside the sphere.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testDistanceToPoint() {

        final Vector3D center = new Vector3D(1.0, 1.0, 1.0);
        final Sphere sphere = new Sphere(center, 1.0);

        // test with a point on the surface
        final Vector3D point1 = new Vector3D(0.0, 1.0, 1.0);
        double distance = sphere.distanceTo(point1);
        Assert.assertEquals(distance, 0.0, this.comparisonEpsilon);

        // test with a point out of the sphere
        final Vector3D point2 = new Vector3D(-1.0, 1.0, 1.0);
        distance = sphere.distanceTo(point2);
        Assert.assertEquals(distance, 1.0, this.comparisonEpsilon);

        // test with a point in the sphere
        final Vector3D point3 = new Vector3D(0.5, 1.0, 1.0);
        distance = sphere.distanceTo(point3);
        Assert.assertEquals(distance, -0.5, this.comparisonEpsilon);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#SPHERE_SHAPE}
     * 
     * @testedMethod {@link Sphere#distanceTo(Line)}
     * 
     * @description Compute the shortest distance between the surface of the sphere and a Line.
     * 
     * @input Lines of space
     * 
     * @output doubles : the distances
     * 
     * @testPassCriteria The output doubles must be the right distance with an epsilon of 1e-14 due to the computation
     *                   errors, positive if the line does not intersect the surface, zero otherwise.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testDistanceToLine() {

        final Vector3D center = new Vector3D(1.0, 1.0, 1.0);
        final Sphere sphere = new Sphere(center, 1.0);

        // test with a line tangent to the surface : distance = 0.0
        Vector3D lineOrig = new Vector3D(2.0, 0.0, 1.0);
        Vector3D lineDir = new Vector3D(0.5, 0.0, 0.0);
        Line line = new Line(lineOrig, lineOrig.add(lineDir));
        double distance = sphere.distanceTo(line);
        Assert.assertEquals(distance, 0.0, this.comparisonEpsilon);

        // test with a line passing through the center : distance = -radius
        lineDir = new Vector3D(0.5, -0.5, 0.0);
        line = new Line(lineOrig, lineOrig.add(lineDir));
        distance = sphere.distanceTo(line);
        Assert.assertEquals(distance, 0.0, this.comparisonEpsilon);

        // test with a line passing inside the sphere, half the radius
        lineOrig = new Vector3D(2.5, 0.5, 1.0);
        lineDir = new Vector3D(0.5, 0.0, 0.0);
        line = new Line(lineOrig, lineOrig.add(lineDir));
        distance = sphere.distanceTo(line);
        Assert.assertEquals(distance, 0.0, this.comparisonEpsilon);

        // test with a line with no intersection with the sphere
        lineOrig = new Vector3D(2.0, 0.0, 1.0);
        lineDir = new Vector3D(0.5, 0.5, 0.0);
        line = new Line(lineOrig, lineOrig.add(lineDir));
        distance = sphere.distanceTo(line);
        final double res = MathLib.sqrt(2.0) - 1.0;
        Assert.assertEquals(distance, res, this.comparisonEpsilon);

        // test with a semi-finite line that does not intersect the sphere
        lineOrig = new Vector3D(1.5, 5.0, 1.0);
        lineDir = new Vector3D(0.0, -1.0, 0.0);
        final Vector3D minAbsP = new Vector3D(1.5, -5.0, 1.0);
        line = new Line(lineOrig, lineOrig.add(lineDir), minAbsP);
        distance = sphere.distanceTo(line);

        final Vector3D pointOnSphere = center.add(minAbsP.subtract(center).normalize());
        Assert.assertEquals(distance, pointOnSphere.distance(minAbsP));

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#SPHERE_SHAPE}
     * 
     * @testedMethod {@link Sphere#intersects(Line)}
     * 
     * @description Test the intersection between the sphere and a line.
     * 
     * @input Lines of space
     * 
     * @output booleans
     * 
     * @testPassCriteria The output boolean must be true if the line intersects the surface, false otherwise.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testIntersectsLine() {
        final Vector3D center = new Vector3D(1.0, 1.0, 1.0);
        final Sphere sphere = new Sphere(center, 1.0);

        // test with a line passing inside the sphere, half the radius
        Vector3D lineOrig = new Vector3D(2.5, 0.5, 1.0);
        Vector3D lineDir = new Vector3D(0.5, 0.0, 0.0);
        Line line = new Line(lineOrig, lineOrig.add(lineDir));
        boolean intersects = sphere.intersects(line);
        Assert.assertTrue(intersects);
        Assert.assertEquals(2, sphere.getIntersectionPoints(line).length);

        // redo previous test with semi-finite line and min abscissa point "after" intersections
        line = new Line(lineOrig, lineOrig.add(lineDir), lineOrig);
        intersects = sphere.intersects(line);
        Assert.assertFalse(intersects);
        Assert.assertEquals(0, sphere.getIntersectionPoints(line).length);

        // redo previous test with semi-finite line and min abscissa point within the sphere
        line = new Line(lineOrig, lineOrig.add(lineDir), new Vector3D(1, 0.5, 1));
        intersects = sphere.intersects(line);
        Assert.assertTrue(intersects);
        Assert.assertEquals(1, sphere.getIntersectionPoints(line).length);

        // test with a line with no intersection with the sphere
        lineOrig = new Vector3D(2.0, 0.0, 1.0);
        lineDir = new Vector3D(0.5, 0.5, 0.0);
        line = new Line(lineOrig, lineOrig.add(lineDir));
        intersects = sphere.intersects(line);
        Assert.assertFalse(intersects);

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#SPHERE_SHAPE}
     * 
     * @testedMethod {@link Sphere#getIntersectionPoints(Line)}
     * 
     * @description Compute the intersection points with a line.
     * 
     * @input Lines of space
     * 
     * @output Vector3D
     * 
     * @testPassCriteria The result array is empty if there is no intersection point. The points have the expected
     *                   coordinates otherwise with an epsilon of 1e-14 due to the computation errors.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testIntersectionsLine() {

        final Vector3D center = new Vector3D(1.0, 1.0, 1.0);
        final Sphere sphere = new Sphere(center, 1.0);

        // test with a line passing inside the sphere, half the radius
        // two intersection points
        Vector3D lineOrig = new Vector3D(1.5, 5.0, 1.0);
        Vector3D lineDir = new Vector3D(0.0, 1.0, 0.0);
        Line line = new Line(lineOrig, lineOrig.add(lineDir));
        Vector3D[] res = sphere.getIntersectionPoints(line);

        Assert.assertEquals(res[1].getX(), 1.5, this.comparisonEpsilon);
        Assert.assertEquals(res[1].getY(), 1.0 - MathLib.sin(FastMath.PI / 3.0), this.comparisonEpsilon);
        Assert.assertEquals(res[1].getZ(), 1.0, this.comparisonEpsilon);

        Assert.assertEquals(res[0].getX(), 1.5, this.comparisonEpsilon);
        Assert.assertEquals(res[0].getY(), 1.0 + MathLib.sin(FastMath.PI / 3.0), this.comparisonEpsilon);
        Assert.assertEquals(res[0].getZ(), 1.0, this.comparisonEpsilon);

        // test with a line passing inside the sphere
        // the origin point is exactly half distance of the two intersections
        lineOrig = new Vector3D(1.5, 1.0, 1.0);
        lineDir = new Vector3D(0.0, 1.0, 0.0);
        line = new Line(lineOrig, lineOrig.add(lineDir));

        res = sphere.getIntersectionPoints(line);

        Assert.assertEquals(res[1].getX(), 1.5, this.comparisonEpsilon);
        Assert.assertEquals(res[1].getY(), 1.0 - MathLib.sin(FastMath.PI / 3.0), this.comparisonEpsilon);
        Assert.assertEquals(res[1].getZ(), 1.0, this.comparisonEpsilon);

        Assert.assertEquals(res[0].getX(), 1.5, this.comparisonEpsilon);
        Assert.assertEquals(res[0].getY(), 1.0 + MathLib.sin(FastMath.PI / 3.0), this.comparisonEpsilon);
        Assert.assertEquals(res[0].getZ(), 1.0, this.comparisonEpsilon);

        // test with no intersection points
        lineOrig = new Vector3D(3.0, 5.0, 1.0);
        lineDir = new Vector3D(0.0, 1.0, 0.0);
        line = new Line(lineOrig, lineOrig.add(lineDir));
        res = sphere.getIntersectionPoints(line);
        Assert.assertEquals(0, res.length);

        // test with a line tangent to the sphere
        // one intersection point
        lineOrig = new Vector3D(2.0, 5.0, 1.0);
        lineDir = new Vector3D(0.0, 1.0, 0.0);
        line = new Line(lineOrig, lineOrig.add(lineDir));
        res = sphere.getIntersectionPoints(line);

        Assert.assertEquals(res.length, 1);
        Assert.assertEquals(res[0].getX(), 2.0, this.comparisonEpsilon);
        Assert.assertEquals(res[0].getY(), 1.0, this.comparisonEpsilon);
        Assert.assertEquals(res[0].getZ(), 1.0, this.comparisonEpsilon);

        // test with a line intersecting the sphere at half the radius along Z-axis
        // two intersection points
        lineOrig = new Vector3D(1.0, 1.0, 1.0);
        lineDir = new Vector3D(0.0, 0.0, 1.0);
        line = new Line(lineOrig, lineOrig.add(lineDir));
        res = sphere.getIntersectionPoints(line);
        Assert.assertEquals(2, res.length);

        Assert.assertEquals(res[0].getX(), 1.0, this.comparisonEpsilon);
        Assert.assertEquals(res[0].getY(), 1.0, this.comparisonEpsilon);
        Assert.assertEquals(res[0].getZ(), 0.0, this.comparisonEpsilon);

        Assert.assertEquals(res[1].getX(), 1.0, this.comparisonEpsilon);
        Assert.assertEquals(res[1].getY(), 1.0, this.comparisonEpsilon);
        Assert.assertEquals(res[1].getZ(), 2.0, this.comparisonEpsilon);

        /*
         * Redo some tests with semi finite lines
         */
        // Semi-finite line intersects in two points
        lineOrig = new Vector3D(1.5, 5.0, 1.0);
        lineDir = new Vector3D(0.0, -1.0, 0.0);
        line = new Line(lineOrig, lineOrig.add(lineDir), lineOrig);
        res = sphere.getIntersectionPoints(line);

        Assert.assertEquals(res.length, 2);
        Assert.assertEquals(res[0].getX(), 1.5, this.comparisonEpsilon);
        Assert.assertEquals(res[0].getY(), 1.0 - MathLib.sin(FastMath.PI / 3.0), this.comparisonEpsilon);
        Assert.assertEquals(res[0].getZ(), 1.0, this.comparisonEpsilon);

        Assert.assertEquals(res[1].getX(), 1.5, this.comparisonEpsilon);
        Assert.assertEquals(res[1].getY(), 1.0 + MathLib.sin(FastMath.PI / 3.0), this.comparisonEpsilon);
        Assert.assertEquals(res[1].getZ(), 1.0, this.comparisonEpsilon);

        // Semi-finite line intersects in only one point: min abscissa point is within the sphere
        lineOrig = new Vector3D(1.5, 5.0, 1.0);
        lineDir = new Vector3D(0.0, -1.0, 0.0);
        line = new Line(lineOrig, lineOrig.add(lineDir), sphere.getCenter());
        res = sphere.getIntersectionPoints(line);

        Assert.assertEquals(res.length, 1);
        Assert.assertEquals(res[0].getX(), 1.5, this.comparisonEpsilon);
        Assert.assertEquals(res[0].getY(), 1.0 - MathLib.sin(FastMath.PI / 3.0), this.comparisonEpsilon);
        Assert.assertEquals(res[0].getZ(), 1.0, this.comparisonEpsilon);

        // Semi-finite line does not intersect
        lineOrig = new Vector3D(1.5, 5.0, 1.0);
        lineDir = new Vector3D(0.0, -1.0, 0.0);
        final Vector3D minAbsP = new Vector3D(1.5, -5.0, 1.0);
        line = new Line(lineOrig, lineOrig.add(lineDir), minAbsP);
        res = sphere.getIntersectionPoints(line);

        Assert.assertEquals(res.length, 0);

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#SPHERE_SHAPE}
     * 
     * @testedMethod {@link Sphere#getIntersectionPoints(Line)}
     * 
     * @description Special case of intersection points computation : line origin too close from the center of the
     *              sphere.
     * 
     * @input Lines of space
     * 
     * @output Vector3D
     * 
     * @testPassCriteria Correct intersection points with an epsilon of 1e-14 due to the computation errors.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testCloseOrigin() {
        // Commons-math moves the origin of the line the closest
        // to the origin of the 3D space,
        // which here is also the center of the sphere...
        // This used to cause a loss of precision
        // on the intersection point's positions.
        final Vector3D yPlusZ = Vector3D.PLUS_J.add(Vector3D.PLUS_K);
        final Line yPlusZLine = new Line(Vector3D.MINUS_K, Vector3D.MINUS_K.add(yPlusZ));
        final Sphere originSphere = new Sphere(Vector3D.ZERO, 1.);
        final Vector3D[] origInts = originSphere.getIntersectionPoints(yPlusZLine);
        final Vector3D exp01 = new Vector3D(0., 0., -1.);
        final Vector3D exp02 = new Vector3D(0., 1., 0.);
        // There was a loss of precision that forced the epsilon at 1.5e-8
        // but now it is at Precision.DOUBLE_COMPARISON_EPSILON;

        Assert.assertEquals(0., exp01.distance(origInts[1]), this.comparisonEpsilon);
        Assert.assertEquals(0., exp02.distance(origInts[0]), this.comparisonEpsilon);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#SPHERE_SHAPE}
     * 
     * @testedMethod {@link Sphere#closestPointTo(Line)}
     * 
     * @description Compute the point of the elliptic realizing the shortest distance to a line of space, and the
     *              associated point of the line.
     * 
     * @input Points of space (Vector3D)
     * 
     * @output Vector3D[]
     * 
     * @testPassCriteria The output vector must be the one of the elliptic shape and the one of the line realizing the
     *                   shortest distance.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testClosestPointToLine() {

        final double eps = Precision.DOUBLE_COMPARISON_EPSILON;

        final Vector3D center = new Vector3D(1.0, 1.0, 1.0);
        final Sphere sphere = new Sphere(center, 1.0);

        /*
         * Infinite line case
         */
        // test with an intersecting line
        Vector3D lineOrig = new Vector3D(1.5, 5.0, 1.0);
        Vector3D lineDir = new Vector3D(0.0, 1.0, 0.0);
        Line line = new Line(lineOrig, lineOrig.add(lineDir));

        Vector3D[] closestPoints = sphere.closestPointTo(line);

        Vector3D point1 = closestPoints[0];
        Vector3D point2 = closestPoints[1];

        Assert.assertEquals(1.5, point1.getX(), eps);
        Assert.assertEquals(1.0 - MathLib.sin(FastMath.PI / 3.0), point1.getY(), eps);
        Assert.assertEquals(1.0, point1.getZ(), eps);
        Assert.assertEquals(1.5, point2.getX(), eps);
        Assert.assertEquals(1.0 - MathLib.sin(FastMath.PI / 3.0), point2.getY(), eps);
        Assert.assertEquals(1.0, point2.getZ(), eps);

        // test with a non intersecting line
        lineOrig = new Vector3D(1.0, 3.0, 5.0);
        lineDir = new Vector3D(0.0, 0.0, 1.0);
        line = new Line(lineOrig, lineOrig.add(lineDir));

        closestPoints = sphere.closestPointTo(line);

        point1 = closestPoints[0];
        point2 = closestPoints[1];

        Assert.assertEquals(1.0, point1.getX(), eps);
        Assert.assertEquals(3.0, point1.getY(), eps);
        Assert.assertEquals(1.0, point1.getZ(), eps);
        Assert.assertEquals(1.0, point2.getX(), eps);
        Assert.assertEquals(2.0, point2.getY(), eps);
        Assert.assertEquals(1.0, point2.getZ(), eps);

        /*
         * Semi-finite line case
         */
        // Semi-finite line intersects: return point with min abscissa
        lineOrig = new Vector3D(1.5, 5.0, 1.0);
        lineDir = new Vector3D(0.0, -1.0, 0.0);
        line = new Line(lineOrig, lineOrig.add(lineDir), lineOrig);
        closestPoints = sphere.closestPointTo(line);

        Assert.assertEquals(closestPoints[0].getX(), 1.5, this.comparisonEpsilon);
        Assert.assertEquals(closestPoints[0].getY(), 1.0 + MathLib.sin(FastMath.PI / 3.0), this.comparisonEpsilon);
        Assert.assertEquals(closestPoints[0].getZ(), 1.0, this.comparisonEpsilon);

        this.assertEq(closestPoints[1], closestPoints[0]);

        // Semi-finite line intersects in only one point: line's min abscissa point is within the sphere
        lineOrig = new Vector3D(1.5, 5.0, 1.0);
        lineDir = new Vector3D(0.0, -1.0, 0.0);
        line = new Line(lineOrig, lineOrig.add(lineDir), sphere.getCenter());
        closestPoints = sphere.closestPointTo(line);

        Assert.assertEquals(closestPoints[0].getX(), 1.5, this.comparisonEpsilon);
        Assert.assertEquals(closestPoints[0].getY(), 1.0 - MathLib.sin(FastMath.PI / 3.0), this.comparisonEpsilon);
        Assert.assertEquals(closestPoints[0].getZ(), 1.0, this.comparisonEpsilon);

        this.assertEq(closestPoints[1], closestPoints[0]);

        // Semi-finite line does not intersect
        lineOrig = new Vector3D(1.5, 5.0, 1.0);
        lineDir = new Vector3D(0.0, -1.0, 0.0);
        Vector3D minAbsP = new Vector3D(1.5, -5.0, 1.0);
        line = new Line(lineOrig, lineOrig.add(lineDir), minAbsP);
        closestPoints = sphere.closestPointTo(line);

        this.assertEq(minAbsP, closestPoints[0]);

        Vector3D closestPointOnSphere = center.add(minAbsP.subtract(center).normalize());
        this.assertEq(closestPointOnSphere, closestPoints[1]);

        // New test case with semi-finite line
        // Sphere is centered on zero
        // Semi-finite line does not intersect but its infinite version crosses sphere centre
        // Semi-finite line origin is also vector zero
        final Vector3D center2 = Vector3D.ZERO;
        final Sphere sphere2 = new Sphere(center2, 1.0);

        minAbsP = new Vector3D(3, 4, 5);
        line = new Line(sphere2.getCenter(), minAbsP, minAbsP);
        closestPoints = sphere2.closestPointTo(line);

        this.assertEq(Vector3D.ZERO, line.getOrigin());
        this.assertEq(minAbsP, closestPoints[0]);
        closestPointOnSphere = sphere2.getCenter().add(minAbsP.subtract(sphere2.getCenter()).normalize());
        this.assertEq(closestPointOnSphere, closestPoints[1]);
        
        

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#SPHERE_SHAPE}
     * 
     * @testedMethod {@link Sphere#toString()}
     * 
     * @description Creates a string describing the shape, the order of the informations
     *              in this output being the same as the one of the constructor
     * 
     * @input none.
     * 
     * @output String
     * 
     * @testPassCriteria The output string must contain the right information.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testToString() {

        final Vector3D center = new Vector3D(1.0, 1.0, 1.0);
        final Sphere sphere = new Sphere(center, 1.0);

        // string creation
        final String result = sphere.toString();

        final String expected = "Sphere{Center{1; 1; 1},Radius{1.0}}";
        Assert.assertEquals(expected, result);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#SPHERE_SHAPE}
     * 
     * @testedMethod {@link Sphere#getCrossSection(Vector3D)}
     * 
     * @description Creates a sphere, and gets the cross section.
     * 
     * @input a sphere.
     * 
     * @output a double : the cross section
     * 
     * @testPassCriteria The cross section must be equal to PI * R * R, whatever the direction vector is.
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void testCrossSection() {
        final double eps = Precision.DOUBLE_COMPARISON_EPSILON;

        final Vector3D center = new Vector3D(1.0, 1.0, 1.0);
        final Sphere sphere = new Sphere(center, 1.0);

        final Vector3D direction = new Vector3D(5.0, 4.0, 2.0);

        final double crossSection1 = sphere.getCrossSection(direction);
        final double crossSection2 = sphere.getCrossSection(Vector3D.PLUS_K);

        Assert.assertEquals(FastMath.PI, crossSection1, eps);
        Assert.assertEquals(FastMath.PI, crossSection2, eps);
    }

    /**
     * @testType UT
     * 
     * 
     * @description Test the getters of a class.
     * 
     * @input the class parameters
     * 
     * @output the class parameters
     * 
     * @testPassCriteria the parameters of the class are the same in input and
     *                   output
     * 
     * @referenceVersion 4.1
     * 
     * @nonRegressionVersion 4.1
     */
    @Test
    public void testGetters() {

        final Vector3D center = new Vector3D(1.0, 1.0, 1.0);
        // positive radius
        final Sphere sphere = new Sphere(center, 1.0);
        Assert.assertEquals(12.5663706143591722, Sphere.getSurfaceFromRadius(2), 0);
        Assert.assertEquals(2, Sphere.getRadiusFromSurface(12.566370614359172), 0);
        Assert.assertEquals(FastMath.PI, sphere.getSurface(), 0);
    }

    /**
     * Test equality of vectors
     * 
     * @param v1
     *        expected
     * @param v2
     *        actual
     */
    private void assertEq(final Vector3D v1, final Vector3D v2) {

        Assert.assertEquals(v1.getX(), v2.getX(), this.comparisonEpsilon);
        Assert.assertEquals(v1.getY(), v2.getY(), this.comparisonEpsilon);
        Assert.assertEquals(v1.getZ(), v2.getZ(), this.comparisonEpsilon);

    }

}