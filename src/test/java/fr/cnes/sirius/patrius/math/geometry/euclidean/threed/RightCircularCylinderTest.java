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
 * 
 * @history creation 17/10/2011
 *
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:834:28/03/2017:add object Vehicule
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::FA:1796:03/10/2018:Correction vehicle class
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
 *              Validation tests for the object RightCircularCylinder.
 *              </p>
 * 
 * @author Thomas Trapier
 * 
 * @version $Id: RightCircularCylinderTest.java 17909 2017-09-11 11:57:36Z bignon $
 * 
 * @since 1.0
 * 
 */
public class RightCircularCylinderTest {
    /** Features description. */
    public enum features {

        /**
         * @featureTitle Right circular cylinder shape
         * 
         * @featureDescription Creation of a right circular cylinder shape, computation of distances and intersections
         *                     with lines and points.
         * 
         * @coveredRequirements DV-GEOMETRIE_50, DV-GEOMETRIE_60, DV-GEOMETRIE_90, DV-GEOMETRIE_120, DV-GEOMETRIE_130,
         *                      DV-GEOMETRIE_140
         */
        RIGHT_CIRCULAR_CYLINDER_SHAPE
    }

    /** Epsilon for double comparison. */
    private final double comparisonEpsilon = Precision.DOUBLE_COMPARISON_EPSILON;

    /** Epsilon taking into account the machine error. */
    private final double machineEpsilon = Precision.EPSILON;

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#RIGHT_CIRCULAR_CYLINDER_SHAPE}
     * 
     * @testedMethod {@link RightCircularCylinder#RightCircularCylinder(Vector3D, Vector3D, double, double)}
     * 
     * @description Instantiation of a an right circular cylinder from its axis, height and radius.
     * 
     * @input a point origin, a vector direction, several doubles as radiuses and height (positive, negative, zero)
     * 
     * @output RightCircularCylinder
     * 
     * @testPassCriteria The cylinder can be created only if the radius and height are strictly positive, and the
     *                   direction of the axis not null. We check the returned elements with the ones given at the
     *                   construction with an epsilon of 1e-16 which takes into account the machine error only. An
     *                   exception is thrown otherwise.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testConstructor() {

        final Vector3D origin = new Vector3D(0.0, 1.0, 1.0);

        // test with corrects inputs
        Vector3D direction = new Vector3D(2.0, 0.0, 0.0);
        double radius = 2.0;
        double height = 5.0;

        RightCircularCylinder cylinder1 = new RightCircularCylinder(origin, direction, radius, height);

        final Line axis = new Line(origin, origin.add(direction));
        final RightCircularCylinder cylinder2 = new RightCircularCylinder(axis, radius, height);

        final Vector3D origin1 = cylinder1.getOrigin();
        final Vector3D origin2 = cylinder2.getOrigin();
        Assert.assertEquals(origin2.getX(), origin1.getX(), this.machineEpsilon);
        Assert.assertEquals(origin2.getY(), origin1.getY(), this.machineEpsilon);
        Assert.assertEquals(origin2.getZ(), origin1.getZ(), this.machineEpsilon);

        final Vector3D direction2 = cylinder2.getDirection();
        Assert.assertEquals(1.0, direction2.getX(), this.machineEpsilon);
        Assert.assertEquals(0.0, direction2.getY(), this.machineEpsilon);
        Assert.assertEquals(0.0, direction2.getZ(), this.machineEpsilon);

        final Vector3D direction1 = cylinder1.getDirection();
        Assert.assertEquals(1.0, direction1.getX(), this.machineEpsilon);
        Assert.assertEquals(0.0, direction1.getY(), this.machineEpsilon);
        Assert.assertEquals(0.0, direction1.getZ(), this.machineEpsilon);

        Assert.assertEquals(radius, cylinder1.getRadius(), this.machineEpsilon);
        Assert.assertEquals(height, cylinder1.getLength(), this.machineEpsilon);

        Assert.assertEquals(FastMath.PI * radius * radius, cylinder1.getBaseSurface(), 0.);
        Assert.assertEquals(2. * radius * height, cylinder1.getTransversalSurf(), 0.);
        Assert.assertEquals(height,
            RightCircularSurfaceCylinder.getLengthFromTSurfaceAndRadius(2. * radius * height, radius), 0.);
        Assert.assertEquals((MathLib.sqrt(2.) + 2.) / 2. * radius * height, cylinder1.getEquivalentTransversalSurf(),
            0.);

        final double xSurf = radius * radius * FastMath.PI;
        final double tSurf = 2. * radius * height;
        final RightCircularSurfaceCylinder cylinder3 = new RightCircularSurfaceCylinder(xSurf, tSurf);
        Assert.assertEquals(height, cylinder3.getLength(), 0.);
        Assert.assertEquals(radius, cylinder3.getRadius(), 0.);
        Assert.assertEquals(xSurf, cylinder3.getSurfX(), 0);
        Assert.assertEquals(tSurf, cylinder3.getTransversalSurf());
        Assert.assertEquals((MathLib.sqrt(2) + 2) / 4.0 * tSurf, cylinder3.getEquivalentTransversalSurf(), 0);
        Assert.assertEquals(tSurf, RightCircularSurfaceCylinder.getTSurfaceFromRadiusAndLength(radius, height), 0);

        final RightCircularSurfaceCylinder cylinderClone = new RightCircularSurfaceCylinder(xSurf, tSurf);
        Assert.assertEquals(true, cylinder3.equals(cylinderClone));

        // test with wrong radiuses
        radius = 0.0;
        try {
            cylinder1 = new RightCircularCylinder(origin, direction, radius, height);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
        radius = -1.0;
        try {
            cylinder1 = new RightCircularCylinder(origin, direction, radius, height);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }

        // test with wrong direction
        radius = 1.0;
        direction = new Vector3D(0.0, 0.0, 0.0);
        try {
            cylinder1 = new RightCircularCylinder(origin, direction, radius, height);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }

        // test with wrong height
        direction = new Vector3D(2.0, 0.0, 0.0);
        height = 0.0;
        try {
            cylinder1 = new RightCircularCylinder(origin, direction, radius, height);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
        height = -1.0;
        try {
            cylinder1 = new RightCircularCylinder(origin, direction, radius, height);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#RIGHT_CIRCULAR_CYLINDER_SHAPE}
     * 
     * @testedMethod {@link RightCircularCylinder#distanceTo(Line)}
     * 
     * @description Compute the shortest distance between the surface of the cylinder and a Line.
     * 
     * @input Lines of space
     * 
     * @output doubles : the distances
     * 
     * @testPassCriteria The output doubles must be the right distance, positive if the line does not intersects the
     *                   surface and zero otherwise with an epsilon of 1e-14 due to the computation errors.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testDistanceToLine() {

        // cylinder creation
        final Vector3D origin = new Vector3D(0.0, 1.0, 1.0);
        final Vector3D direction = new Vector3D(2.0, 0.0, 0.0);
        final double radius = 2.0;
        final double height = 5.0;

        final RightCircularCylinder cylinder = new RightCircularCylinder(origin, direction, radius, height);

        // test with a line intersecting the side
        Vector3D originLine = new Vector3D(2.0, 1.0, 0.0);
        Vector3D dirLine = new Vector3D(0.0, 0.0, 1.0);
        Line line = new Line(originLine, originLine.add(dirLine));

        double distance = cylinder.distanceTo(line);
        Assert.assertEquals(0.0, distance, this.comparisonEpsilon);

        // test with a line intersecting the disks, parallel to the axis
        originLine = new Vector3D(0.0, 2.0, 1.0);
        dirLine = new Vector3D(1.0, 0.0, 0.0);
        line = new Line(originLine, originLine.add(dirLine));
        distance = cylinder.distanceTo(line);
        Assert.assertEquals(0.0, distance, this.comparisonEpsilon);

        // test with a line intersecting the disks, not parallel to the axis
        originLine = new Vector3D(0.0, 1.0, 1.0);
        dirLine = new Vector3D(1.0, 0.2, 0.0);
        line = new Line(originLine, originLine.add(dirLine));
        distance = cylinder.distanceTo(line);
        Assert.assertEquals(0.0, distance, this.comparisonEpsilon);

        // test with a tangent line to the side
        dirLine = new Vector3D(0.0, 0.0, 1.0);
        originLine = new Vector3D(0.0, 3.0, 0.0);
        line = new Line(originLine, originLine.add(dirLine));
        distance = cylinder.distanceTo(line);
        Assert.assertEquals(0.0, distance, this.comparisonEpsilon);

        // test with a line closest to a side, non parallel
        originLine = new Vector3D(0.0, 4.0, 0.0);
        dirLine = new Vector3D(1.0, 0.0, 1.0);
        line = new Line(originLine, originLine.add(dirLine));
        distance = cylinder.distanceTo(line);
        Assert.assertEquals(1.0, distance, this.comparisonEpsilon);

        // test with a line closest to a side, parallel to the axis
        originLine = new Vector3D(0.0, 4.0, 1.0);
        dirLine = new Vector3D(1.0, 0.0, 0.0);
        line = new Line(originLine, originLine.add(dirLine));
        distance = cylinder.distanceTo(line);
        Assert.assertEquals(1.0, distance, this.comparisonEpsilon);

        // test with a line closest to the up disk
        originLine = new Vector3D(2.5, 4.0, 1.0);
        dirLine = new Vector3D(-1.0, 1.0, 0.0);
        line = new Line(originLine, originLine.add(dirLine));
        distance = cylinder.distanceTo(line);
        Assert.assertEquals(MathLib.sqrt(2.0) / 2.0, distance, this.comparisonEpsilon);

        // test with a line closest to the bottom disk
        originLine = new Vector3D(-2.5, 4.0, 1.0);
        dirLine = new Vector3D(1.0, 1.0, 0.0);
        line = new Line(originLine, originLine.add(dirLine));
        distance = cylinder.distanceTo(line);
        Assert.assertEquals(MathLib.sqrt(2.0) / 2.0, distance, this.comparisonEpsilon);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#RIGHT_CIRCULAR_CYLINDER_SHAPE}
     * 
     * @testedMethod {@link RightCircularCylinder#intersects(Line)}
     * 
     * @description Test the intersection between the cylinder and a line.
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

        // cylinder creation
        final Vector3D origin = new Vector3D(0.0, 1.0, 1.0);
        final Vector3D direction = new Vector3D(2.0, 0.0, 0.0);
        final double radius = 2.0;
        final double height = 5.0;

        final RightCircularCylinder cylinder = new RightCircularCylinder(origin, direction, radius, height);

        // test with a line intersecting the side
        Vector3D originLine = new Vector3D(2.0, 1.0, 0.0);
        Vector3D dirLine = new Vector3D(0.0, 0.0, 1.0);
        Line line = new Line(originLine, originLine.add(dirLine));
        boolean intersects = cylinder.intersects(line);
        Assert.assertTrue(intersects);

        // test with a line intersecting the disks, parallel to the axis
        originLine = new Vector3D(0.0, 2.0, 1.0);
        dirLine = new Vector3D(1.0, 0.0, 0.0);
        line = new Line(originLine, originLine.add(dirLine));
        intersects = cylinder.intersects(line);
        Assert.assertTrue(intersects);

        // test with a line intersecting the disks, not parallel to the axis
        originLine = new Vector3D(0.0, 1.0, 1.0);
        dirLine = new Vector3D(1.0, 0.2, 0.0);
        line = new Line(originLine, originLine.add(dirLine));
        intersects = cylinder.intersects(line);
        Assert.assertTrue(intersects);

        // test with a tangent line to the side
        dirLine = new Vector3D(0.0, 0.0, 1.0);
        originLine = new Vector3D(0.0, 3.0, 0.0);
        line = new Line(originLine, originLine.add(dirLine));
        intersects = cylinder.intersects(line);
        Assert.assertTrue(intersects);

        // test without intersection with the associated
        // infinite cone, not parallel to the axis
        originLine = new Vector3D(0.0, 4.0, 0.0);
        dirLine = new Vector3D(1.0, 0.0, 1.0);
        line = new Line(originLine, originLine.add(dirLine));
        intersects = cylinder.intersects(line);
        Assert.assertTrue(!intersects);

        // tests with intersection with the associated infinite cone
        // but not with the shape
        originLine = new Vector3D(-2.5, 4.0, 1.0);
        dirLine = new Vector3D(1.0, 1.0, 0.0);
        line = new Line(originLine, originLine.add(dirLine));
        intersects = cylinder.intersects(line);
        Assert.assertTrue(!intersects);

        originLine = new Vector3D(2.5, 4.0, 1.0);
        dirLine = new Vector3D(-1.0, 1.0, 0.0);
        line = new Line(originLine, originLine.add(dirLine));
        intersects = cylinder.intersects(line);
        Assert.assertTrue(!intersects);

        // test with a line parallel to the axis, out of the shape
        originLine = new Vector3D(0.0, 4.0, 1.0);
        dirLine = new Vector3D(1.0, 0.0, 0.0);
        line = new Line(originLine, originLine.add(dirLine));
        intersects = cylinder.intersects(line);
        Assert.assertTrue(!intersects);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#RIGHT_CIRCULAR_CYLINDER_SHAPE}
     * 
     * @testedMethod {@link RightCircularCylinder#getIntersectionPoints(Line)}
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

        // cylinder creation
        final Vector3D origin = new Vector3D(0.0, 1.0, 1.0);
        final Vector3D direction = new Vector3D(2.0, 0.0, 0.0);
        final double radius = 2.0;
        final double height = 5.0;

        final RightCircularCylinder cylinder = new RightCircularCylinder(origin, direction, radius, height);

        // test with a line intersecting the side
        Vector3D originLine = new Vector3D(0.0, 1.0, 1.0);
        Vector3D dirLine = new Vector3D(0.0, 1.0, 1.0);
        Line line = new Line(originLine, originLine.add(dirLine));

        Vector3D[] intersections = cylinder.getIntersectionPoints(line);
        Assert.assertEquals(2, intersections.length);

        Vector3D intersect1 = intersections[0];
        Vector3D intersect2 = intersections[1];

        Assert.assertEquals(0.0, intersect1.getX(), this.comparisonEpsilon);
        Assert.assertEquals(-radius / MathLib.sqrt(2.0) + 1, intersect1.getY(), this.comparisonEpsilon);
        Assert.assertEquals(-radius / MathLib.sqrt(2.0) + 1, intersect1.getZ(), this.comparisonEpsilon);

        Assert.assertEquals(0.0, intersect2.getX(), this.comparisonEpsilon);
        Assert.assertEquals(radius / MathLib.sqrt(2.0) + 1, intersect2.getY(), this.comparisonEpsilon);
        Assert.assertEquals(radius / MathLib.sqrt(2.0) + 1, intersect2.getZ(), this.comparisonEpsilon);

        // test with a line intersecting the disks, not parallel to the axis
        originLine = new Vector3D(0.0, 1.0, 1.0);
        dirLine = new Vector3D(1.0, 0.0, 0.0);
        line = new Line(originLine, originLine.add(dirLine));
        intersections = cylinder.getIntersectionPoints(line);
        Assert.assertEquals(2, intersections.length);

        intersect1 = intersections[0];
        intersect2 = intersections[1];

        Assert.assertEquals(2.5, intersect1.getX(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, intersect2.getY(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, intersect2.getZ(), this.comparisonEpsilon);

        Assert.assertEquals(-2.5, intersect2.getX(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, intersect2.getY(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, intersect2.getZ(), this.comparisonEpsilon);

        // test with a tangent line to the side
        dirLine = new Vector3D(0.0, 0.0, 1.0);
        originLine = new Vector3D(0.0, 3.0, 0.0);
        line = new Line(originLine, originLine.add(dirLine));
        intersections = cylinder.getIntersectionPoints(line);
        Assert.assertEquals(1, intersections.length);

        final Vector3D intersect = intersections[0];
        Assert.assertEquals(0.0, intersect.getX(), this.comparisonEpsilon);
        Assert.assertEquals(3.0, intersect.getY(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, intersect.getZ(), this.comparisonEpsilon);

        // test without intersection with the associated
        // infinite cone, not parallel to the axis
        originLine = new Vector3D(0.0, 4.0, 0.0);
        dirLine = new Vector3D(1.0, 0.0, 1.0);
        line = new Line(originLine, originLine.add(dirLine));
        intersections = cylinder.getIntersectionPoints(line);
        Assert.assertEquals(0, intersections.length);

        // tests with intersection with the associated infinite cone
        // but not with the shape
        originLine = new Vector3D(-2.5, 4.0, 1.0);
        dirLine = new Vector3D(1.0, 1.0, 0.0);
        line = new Line(originLine, originLine.add(dirLine));
        intersections = cylinder.getIntersectionPoints(line);
        Assert.assertEquals(0, intersections.length);

        originLine = new Vector3D(2.5, 4.0, 1.0);
        dirLine = new Vector3D(-1.0, 1.0, 0.0);
        line = new Line(originLine, originLine.add(dirLine));
        intersections = cylinder.getIntersectionPoints(line);
        Assert.assertEquals(0, intersections.length);

        // test with a line parallel to the axis, out of the shape
        originLine = new Vector3D(0.0, 4.0, 1.0);
        dirLine = new Vector3D(1.0, 0.0, 0.0);
        line = new Line(originLine, originLine.add(dirLine));
        intersections = cylinder.getIntersectionPoints(line);
        Assert.assertEquals(0, intersections.length);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#RIGHT_CIRCULAR_CYLINDER_SHAPE}
     * 
     * @testedMethod {@link RightCircularCylinder#closestPointTo(Line)}
     * 
     * @description Compute the point of the cylinder realizing the shortest distance to a line of space, and the
     *              associated point of the line.
     * 
     * @input Points of space (Vector3D)
     * 
     * @output Vector3D[]
     * 
     * @testPassCriteria The output vector must be the one of the shape and the one of the line realizing the shortest
     *                   distance with an epsilon of 1e-14 due to the computation errors.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testClosestPointToLine() {

        // cylinder creation
        final Vector3D origin = new Vector3D(0.0, 1.0, 1.0);
        final Vector3D direction = new Vector3D(2.0, 0.0, 0.0);
        final double radius = 2.0;
        final double height = 6.0;

        final RightCircularCylinder cylinder = new RightCircularCylinder(origin, direction, radius, height);

        // test with an intersecting line
        Vector3D originLine = new Vector3D(2.0, 1.0, 1.0);
        Vector3D dirLine = new Vector3D(1.0, 0.0, 1.0);
        Line line = new Line(originLine, originLine.add(dirLine));

        Vector3D[] closestPoints = cylinder.closestPointTo(line);

        Vector3D point1 = closestPoints[0];
        Vector3D point2 = closestPoints[1];

        Assert.assertEquals(0.0, point1.getX(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point1.getY(), this.comparisonEpsilon);
        Assert.assertEquals(-1.0, point1.getZ(), this.comparisonEpsilon);
        Assert.assertEquals(0.0, point2.getX(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point2.getY(), this.comparisonEpsilon);
        Assert.assertEquals(-1.0, point2.getZ(), this.comparisonEpsilon);

        // test with a non parallel line out of the cylinder
        // closest to the side
        originLine = new Vector3D(5.0, 4.0, 5.0);
        dirLine = new Vector3D(1.0, 0.0, 1.0);
        line = new Line(originLine, originLine.add(dirLine));

        closestPoints = cylinder.closestPointTo(line);

        point1 = closestPoints[0];
        point2 = closestPoints[1];

        Assert.assertEquals(1.0, point1.getX(), this.comparisonEpsilon);
        Assert.assertEquals(4.0, point1.getY(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point1.getZ(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point2.getX(), this.comparisonEpsilon);
        Assert.assertEquals(3.0, point2.getY(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point2.getZ(), this.comparisonEpsilon);

        // test with a line closest to the up disk
        originLine = new Vector3D(3.0, 1.0, 5.0);
        dirLine = new Vector3D(1.0, 0.0, -1.0);
        line = new Line(originLine, originLine.add(dirLine));

        closestPoints = cylinder.closestPointTo(line);

        point1 = closestPoints[0];
        point2 = closestPoints[1];

        Assert.assertEquals(4.0, point1.getX(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point1.getY(), this.comparisonEpsilon);
        Assert.assertEquals(4.0, point1.getZ(), this.comparisonEpsilon);
        Assert.assertEquals(3.0, point2.getX(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point2.getY(), this.comparisonEpsilon);
        Assert.assertEquals(3.0, point2.getZ(), this.comparisonEpsilon);

        // test with a line closest to the bottom disk
        originLine = new Vector3D(-3.0, 1.0, 5.0);
        dirLine = new Vector3D(1.0, 0.0, 1.0);
        line = new Line(originLine, originLine.add(dirLine));

        closestPoints = cylinder.closestPointTo(line);

        point1 = closestPoints[0];
        point2 = closestPoints[1];

        Assert.assertEquals(-4.0, point1.getX(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point1.getY(), this.comparisonEpsilon);
        Assert.assertEquals(4.0, point1.getZ(), this.comparisonEpsilon);
        Assert.assertEquals(-3.0, point2.getX(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point2.getY(), this.comparisonEpsilon);
        Assert.assertEquals(3.0, point2.getZ(), this.comparisonEpsilon);

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#RIGHT_CIRCULAR_CYLINDER_SHAPE}
     * 
     * @testedMethod {@link RightCircularCylinder#toString()}
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

        // cylinder creation
        final Vector3D origin = new Vector3D(0.0, 1.0, 1.0);
        final Vector3D direction = new Vector3D(2.0, 0.0, 0.0);
        final double radius = 2.0;
        final double height = 6.0;

        final RightCircularCylinder cylinder = new RightCircularCylinder(origin, direction, radius, height);

        // string creation
        final String result = cylinder.toString();

        final String expected = "RightCircularCylinder{Origin{0; 1; 1},Direction{1; 0; 0},Radius{2.0},Height{6.0}}";
        Assert.assertEquals(expected, result);

        final RightCircularSurfaceCylinder cylinder2 = new RightCircularSurfaceCylinder(4, 10);
        // string creation
        final String result2 = cylinder2.toString();

        final String expected2 = "RightCircularSurfaceCylinder:[transversalSurface=10.0, baseSurface=4.0]";
        Assert.assertEquals(expected2, result2);

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#RIGHT_CIRCULAR_CYLINDER_SHAPE}
     * 
     * @testedMethod {@link RightCircularCylinder#getCrossSection(Vector3D)}
     * 
     * @description Test various cylinder cross sections computation (using rays with various orientations):
     *              - Ray from bottom
     *              - Ray from top
     *              - Rays from sides
     *              - Ray from 45° inclination
     * 
     * @input cylinder, rays
     * 
     * @output cross section
     * 
     * @testPassCriteria cross sections are as expected (reference: math, STELA in random case)
     * 
     * @referenceVersion 3.4
     * 
     * @nonRegressionVersion 3.4
     */
    @Test
    public void testCrossSection() {
        // Initialization
        final RightCircularCylinder cylinder = new RightCircularCylinder(new Vector3D(10, 20, 30), Vector3D.PLUS_K, 2,
            10.);

        // Check various configuration

        // Cylinder from bottom/top
        Assert.assertEquals(FastMath.PI * 2. * 2., cylinder.getCrossSection(Vector3D.PLUS_K), 0.);
        Assert.assertEquals(FastMath.PI * 2. * 2., cylinder.getCrossSection(Vector3D.MINUS_K), 0.);

        // Cylinder from sides
        Assert.assertEquals(2. * 2. * 10., cylinder.getCrossSection(Vector3D.PLUS_I), 0.);
        Assert.assertEquals(2. * 2. * 10., cylinder.getCrossSection(Vector3D.PLUS_J), 0.);

        // Cylinder from 45° inclination
        final double expected = FastMath.PI * 2. * 2. * MathLib.sqrt(2.) / 2. + 2. * 2. * 10. * MathLib.sqrt(2.) / 2.;
        Assert.assertEquals(expected, cylinder.getCrossSection(Vector3D.PLUS_K.add(Vector3D.PLUS_I)), 0.);

        // Cylinder with random inclination (reference: STELA, 100M planes)
        Assert.assertEquals(33.98, cylinder.getCrossSection(new Vector3D(1, -2, 3)), 1E-4);

        // Right circular surface cylinder
        final RightCircularSurfaceCylinder cylinder2 =
            new RightCircularSurfaceCylinder(2 * 2 * FastMath.PI, 2 * 2 * 10);
        Assert.assertEquals(FastMath.PI * 2. * 2., cylinder2.getCrossSection(Vector3D.PLUS_I), 0.);

    }

}
