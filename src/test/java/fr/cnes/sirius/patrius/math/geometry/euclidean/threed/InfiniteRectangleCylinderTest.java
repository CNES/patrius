/**
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
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.geometry.euclidean.threed;

import junit.framework.Assert;

import org.junit.Test;

import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;

/**
 * @description <p>
 *              Validation tests for the object InfiniteRectangleCylinder.
 *              </p>
 * 
 * @author Thomas Trapier
 * 
 * @version $Id: InfiniteRectangleCylinderTest.java 17909 2017-09-11 11:57:36Z bignon $
 * 
 * @since 1.0
 * 
 */
public class InfiniteRectangleCylinderTest {

    /** Features description. */
    public enum features {

        /**
         * @featureTitle Infinite rectangle cylinder shape
         * 
         * @featureDescription Creation of an infinite rectangle cylinder shape, computation of distances and
         *                     intersections with lines and points.
         * 
         * @coveredRequirements DV-GEOMETRIE_40, DV-GEOMETRIE_50, DV-GEOMETRIE_60, DV-GEOMETRIE_90, DV-GEOMETRIE_120,
         *                      DV-GEOMETRIE_130, DV-GEOMETRIE_140
         */
        INFINITE_RECTANGLE_CYLINDER_SHAPE
    }

    /** Epsilon for double comparison. */
    private final double comparisonEpsilon = Precision.DOUBLE_COMPARISON_EPSILON;

    /** Epsilon taking into account the machine error. */
    private final double machineEpsilon = Precision.EPSILON;

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#INFINITE_RECTANGLE_CYLINDER_SHAPE}
     * 
     * @testedMethod {@link InfiniteRectangleCylinder#InfiniteRectangleCylinder(Vector3D, Vector3D, Vector3D, double, double)}
     * 
     * @description Instantiation of a an infinite rectangle cylinder from its origin, axis, orientation length and
     *              width.
     * 
     * @input A point origin, a vector axis, a vector U, several doubles as dimensions (correct, negative, ...)
     * 
     * @output InfiniteRectangleCylinder
     * 
     * @testPassCriteria The cylinder is created only if the dimensions are strictly positive, the axis and the input u
     *                   vector not null and not parallel (with an epsilon of 1e-16 due to the machine errors only : we
     *                   check that the elements are indeed the ones given at the construction). An exception is thrown
     *                   otherwise.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testConstructor() {

        // creation of the cylinder
        final Vector3D origin = new Vector3D(0.0, 1.0, 1.0);
        Vector3D direction = new Vector3D(2.0, 0.0, 0.0);
        Vector3D inputVectorU = new Vector3D(1.0, 1.0, 0.0);
        InfiniteRectangleCylinder cylinder1 = new InfiniteRectangleCylinder(origin, direction, inputVectorU, 2.0, 4.0);

        final Line axis = new Line(origin, origin.add(direction));
        final InfiniteRectangleCylinder cylinder2 = new InfiniteRectangleCylinder(axis, inputVectorU, 2.0, 4.0);

        // getters test
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

        final Vector3D uVector1 = cylinder1.getU();
        final Vector3D uVector2 = cylinder2.getU();
        Assert.assertEquals(0.0, uVector1.getX(), this.machineEpsilon);
        Assert.assertEquals(1.0, uVector1.getY(), this.machineEpsilon);
        Assert.assertEquals(0.0, uVector1.getZ(), this.machineEpsilon);
        Assert.assertEquals(0.0, uVector2.getX(), this.machineEpsilon);
        Assert.assertEquals(1.0, uVector2.getY(), this.machineEpsilon);
        Assert.assertEquals(0.0, uVector2.getZ(), this.machineEpsilon);

        final Vector3D vVector1 = cylinder1.getV();
        final Vector3D vVector2 = cylinder2.getV();
        Assert.assertEquals(0.0, vVector1.getX(), this.machineEpsilon);
        Assert.assertEquals(0.0, vVector1.getY(), this.machineEpsilon);
        Assert.assertEquals(1.0, vVector1.getZ(), this.machineEpsilon);
        Assert.assertEquals(0.0, vVector2.getX(), this.machineEpsilon);
        Assert.assertEquals(0.0, vVector2.getY(), this.machineEpsilon);
        Assert.assertEquals(1.0, vVector2.getZ(), this.machineEpsilon);

        final double length1 = cylinder1.getLength();
        final double length2 = cylinder2.getLength();
        Assert.assertEquals(2.0, length1, this.machineEpsilon);
        Assert.assertEquals(2.0, length2, this.machineEpsilon);

        final double width1 = cylinder1.getWidth();
        final double width2 = cylinder2.getWidth();
        Assert.assertEquals(4.0, width1, this.machineEpsilon);
        Assert.assertEquals(4.0, width2, this.machineEpsilon);

        // test with wrong direction
        direction = new Vector3D(0.0, 0.0, 0.0);
        try {
            cylinder1 = new InfiniteRectangleCylinder(origin, direction, inputVectorU, 2.0, 4.0);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
        // test with U parallel to the axis
        direction = new Vector3D(1.0, 0.0, 0.0);
        inputVectorU = new Vector3D(2.0, 0.0, 0.0);
        try {
            cylinder1 = new InfiniteRectangleCylinder(origin, direction, inputVectorU, 2.0, 4.0);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }

        // test with wrong dimensions
        inputVectorU = new Vector3D(0.0, 1.0, 0.0);
        try {
            cylinder1 = new InfiniteRectangleCylinder(origin, direction, inputVectorU, 0.0, 4.0);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
        try {
            cylinder1 = new InfiniteRectangleCylinder(origin, direction, inputVectorU, -1.0, 4.0);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
        try {
            cylinder1 = new InfiniteRectangleCylinder(origin, direction, inputVectorU, 2.0, -4.0);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
        try {
            cylinder1 = new InfiniteRectangleCylinder(origin, direction, inputVectorU, 2.0, 0.0);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#INFINITE_RECTANGLE_CYLINDER_SHAPE}
     * 
     * @testedMethod {@link InfiniteRectangleCylinder#distanceTo(Line)}
     * 
     * @description Compute the shortest distance between the surface of the cylinder and a Line.
     * 
     * @input Lines of space
     * 
     * @output doubles : the distances
     * 
     * @testPassCriteria The output doubles must be the right distance, positive if the line does not intersect the
     *                   surface and is out of the cylinder, negative if it is inside, zero if an intersection is found
     *                   with an epsilon of 1e-14 due to the computation errors.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testDistanceToLine() {

        // creation of the cylinder
        final Vector3D origin = new Vector3D(0.0, 1.0, 1.0);
        final Vector3D direction = new Vector3D(2.0, 0.0, 0.0);
        final Vector3D inputVectorU = new Vector3D(1.0, 1.0, 0.0);
        final InfiniteRectangleCylinder cylinder = new InfiniteRectangleCylinder(origin, direction, inputVectorU, 2.0,
            4.0);

        // test with a line parallel to a side, out of the cylinder
        Vector3D originLine = new Vector3D(1.0, 3.0, 2.0);
        Vector3D directionLine = new Vector3D(0.0, 0.0, 1.0);
        Line line = new Line(originLine, originLine.add(directionLine));

        double distance = cylinder.distanceTo(line);
        Assert.assertEquals(1.0, distance, this.comparisonEpsilon);

        // test with a line parallel to the axis, out of the cylinder 1
        directionLine = new Vector3D(1.0, 0.0, 0.0);
        line = new Line(originLine, originLine.add(directionLine));
        distance = cylinder.distanceTo(line);
        Assert.assertEquals(1.0, distance, this.comparisonEpsilon);

        // test with a line parallel to the axis, out of the cylinder 2
        originLine = new Vector3D(1.0, 0.5, 4.5);
        line = new Line(originLine, originLine.add(directionLine));
        distance = cylinder.distanceTo(line);
        Assert.assertEquals(1.5, distance, this.comparisonEpsilon);

        // test with a line parallel to the axis, out of the cylinder 3
        originLine = new Vector3D(1.0, 3.0, 4.0);
        line = new Line(originLine, originLine.add(directionLine));
        distance = cylinder.distanceTo(line);
        Assert.assertEquals(MathLib.sqrt(2.0), distance, this.comparisonEpsilon);

        // test with a line parallel to the axis, in the cylinder
        originLine = new Vector3D(0.0, 1.5, 1.5);
        directionLine = new Vector3D(1.0, 0.0, 0.0);
        line = new Line(originLine, originLine.add(directionLine));

        distance = cylinder.distanceTo(line);
        Assert.assertEquals(-0.5, distance, this.comparisonEpsilon);

        // test with a line parallel to nothing with no intersection...
        originLine = new Vector3D(0.0, 2.0, 4.0);
        directionLine = new Vector3D(1.0, -1.0, 1.0);
        line = new Line(originLine, originLine.add(directionLine));

        distance = cylinder.distanceTo(line);
        Assert.assertEquals(MathLib.sqrt(2.0) / 2.0, distance, this.comparisonEpsilon);

        // test with a line intersecting the cylinder
        originLine = new Vector3D(0.0, 3.0, 1.0);
        line = new Line(originLine, originLine.add(directionLine));

        distance = cylinder.distanceTo(line);
        Assert.assertEquals(0.0, distance, this.comparisonEpsilon);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#INFINITE_RECTANGLE_CYLINDER_SHAPE}
     * 
     * @testedMethod {@link InfiniteRectangleCylinder#intersects(Line)}
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

        // creation of the cylinder
        final Vector3D origin = new Vector3D(0.0, 1.0, 1.0);
        final Vector3D direction = new Vector3D(2.0, 0.0, 0.0);
        final Vector3D inputVectorU = new Vector3D(1.0, 1.0, 0.0);
        final InfiniteRectangleCylinder cylinder = new InfiniteRectangleCylinder(origin, direction, inputVectorU, 2.0,
            4.0);

        // test with a line intersecting the cylinder
        Vector3D originLine = new Vector3D(0.0, 3.0, 1.0);
        Vector3D directionLine = new Vector3D(1.0, -1.0, 1.0);
        Line line = new Line(originLine, originLine.add(directionLine));

        boolean intersects = cylinder.intersects(line);
        Assert.assertTrue(intersects);

        // test with a line parallel to the axis, out of the cylinder 1
        directionLine = new Vector3D(1.0, 0.0, 0.0);
        line = new Line(originLine, originLine.add(directionLine));

        intersects = cylinder.intersects(line);
        Assert.assertTrue(!intersects);

        // test with a line parallel to the axis, in the cylinder
        originLine = new Vector3D(0.0, 1.5, 1.5);
        directionLine = new Vector3D(1.0, 0.0, 0.0);
        line = new Line(originLine, originLine.add(directionLine));

        intersects = cylinder.intersects(line);
        Assert.assertTrue(!intersects);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#INFINITE_RECTANGLE_CYLINDER_SHAPE}
     * 
     * @testedMethod {@link InfiniteRectangleCylinder#getIntersectionPoints(Line)}
     * 
     * @description Compute the intersection points with a line.
     * 
     * @input Lines of space
     * 
     * @output Vector3D[]
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

        // creation of the cylinder
        final Vector3D origin = new Vector3D(0.0, 1.0, 1.0);
        final Vector3D direction = new Vector3D(2.0, 0.0, 0.0);
        final Vector3D inputVectorU = new Vector3D(1.0, 1.0, 0.0);
        final InfiniteRectangleCylinder cylinder = new InfiniteRectangleCylinder(origin, direction, inputVectorU, 2.0,
            4.0);

        // test with a line intersecting the cylinder
        Vector3D originLine = new Vector3D(0.0, 3.0, 1.0);
        Vector3D directionLine = new Vector3D(1.0, -1.0, 1.0);
        Line line = new Line(originLine, originLine.add(directionLine));

        Vector3D[] intersections = cylinder.getIntersectionPoints(line);
        Assert.assertEquals(2, intersections.length);

        final Vector3D intersect1 = intersections[0];
        final Vector3D intersect2 = intersections[1];

        Assert.assertEquals(1.0, intersect1.getX(), this.comparisonEpsilon);
        Assert.assertEquals(2.0, intersect1.getY(), this.comparisonEpsilon);
        Assert.assertEquals(2.0, intersect1.getZ(), this.comparisonEpsilon);

        Assert.assertEquals(2.0, intersect2.getX(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, intersect2.getY(), this.comparisonEpsilon);
        Assert.assertEquals(3.0, intersect2.getZ(), this.comparisonEpsilon);

        // test with a line with no intersection
        originLine = new Vector3D(1.0, 3.0, 3.0);
        directionLine = new Vector3D(1.0, -1.0, 1.0);
        line = new Line(originLine, originLine.add(directionLine));

        intersections = cylinder.getIntersectionPoints(line);
        Assert.assertEquals(0, intersections.length);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#INFINITE_RECTANGLE_CYLINDER_SHAPE}
     * 
     * @testedMethod {@link InfiniteRectangleCylinder#closestPointTo(Line)}
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

        // creation of the cylinder
        final Vector3D origin = new Vector3D(0.0, 1.0, 1.0);
        final Vector3D direction = new Vector3D(2.0, 0.0, 0.0);
        final Vector3D inputVectorU = new Vector3D(1.0, 1.0, 0.0);
        final InfiniteRectangleCylinder cylinder = new InfiniteRectangleCylinder(origin, direction, inputVectorU, 2.0,
            4.0);

        // test with a line parallel to a side, out of the cylinder
        Vector3D originLine = new Vector3D(1.0, 3.0, 2.0);
        Vector3D directionLine = new Vector3D(0.0, 0.0, 1.0);
        Line line = new Line(originLine, originLine.add(directionLine));

        Vector3D[] closestPoints = cylinder.closestPointTo(line);

        Vector3D point1 = closestPoints[0];
        Vector3D point2 = closestPoints[1];

        Assert.assertEquals(1.0, point1.getX(), this.comparisonEpsilon);
        Assert.assertEquals(3.0, point1.getY(), this.comparisonEpsilon);
        Assert.assertEquals(3.0, point1.getZ(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point2.getX(), this.comparisonEpsilon);
        Assert.assertEquals(2.0, point2.getY(), this.comparisonEpsilon);
        Assert.assertEquals(3.0, point2.getZ(), this.comparisonEpsilon);

        // test with a line parallel to the axis, out of the cylinder 1
        directionLine = new Vector3D(1.0, 0.0, 0.0);
        line = new Line(originLine, originLine.add(directionLine));

        closestPoints = cylinder.closestPointTo(line);

        point1 = closestPoints[0];
        point2 = closestPoints[1];

        Assert.assertEquals(0.0, point1.getX(), this.comparisonEpsilon);
        Assert.assertEquals(3.0, point1.getY(), this.comparisonEpsilon);
        Assert.assertEquals(2.0, point1.getZ(), this.comparisonEpsilon);
        Assert.assertEquals(0.0, point2.getX(), this.comparisonEpsilon);
        Assert.assertEquals(2.0, point2.getY(), this.comparisonEpsilon);
        Assert.assertEquals(2.0, point2.getZ(), this.comparisonEpsilon);

        // test with a line parallel to the axis, out of the cylinder 2
        originLine = new Vector3D(1.0, 0.5, 4.5);
        line = new Line(originLine, originLine.add(directionLine));

        closestPoints = cylinder.closestPointTo(line);

        point1 = closestPoints[0];
        point2 = closestPoints[1];

        Assert.assertEquals(0.0, point1.getX(), this.comparisonEpsilon);
        Assert.assertEquals(0.5, point1.getY(), this.comparisonEpsilon);
        Assert.assertEquals(4.5, point1.getZ(), this.comparisonEpsilon);
        Assert.assertEquals(0.0, point2.getX(), this.comparisonEpsilon);
        Assert.assertEquals(0.5, point2.getY(), this.comparisonEpsilon);
        Assert.assertEquals(3.0, point2.getZ(), this.comparisonEpsilon);

        // test with a line parallel to the axis, out of the cylinder 3
        originLine = new Vector3D(1.0, 3.0, 4.0);
        line = new Line(originLine, originLine.add(directionLine));

        closestPoints = cylinder.closestPointTo(line);

        point1 = closestPoints[0];
        point2 = closestPoints[1];

        Assert.assertEquals(0.0, point1.getX(), this.comparisonEpsilon);
        Assert.assertEquals(3.0, point1.getY(), this.comparisonEpsilon);
        Assert.assertEquals(4.0, point1.getZ(), this.comparisonEpsilon);
        Assert.assertEquals(0.0, point2.getX(), this.comparisonEpsilon);
        Assert.assertEquals(2.0, point2.getY(), this.comparisonEpsilon);
        Assert.assertEquals(3.0, point2.getZ(), this.comparisonEpsilon);

        // test with a line parallel to the axis, in the cylinder
        originLine = new Vector3D(0.0, 1.5, 1.5);
        directionLine = new Vector3D(1.0, 0.0, 0.0);
        line = new Line(originLine, originLine.add(directionLine));

        closestPoints = cylinder.closestPointTo(line);

        point1 = closestPoints[0];
        point2 = closestPoints[1];

        Assert.assertEquals(0.0, point1.getX(), this.comparisonEpsilon);
        Assert.assertEquals(1.5, point1.getY(), this.comparisonEpsilon);
        Assert.assertEquals(1.5, point1.getZ(), this.comparisonEpsilon);
        Assert.assertEquals(0.0, point2.getX(), this.comparisonEpsilon);
        Assert.assertEquals(2.0, point2.getY(), this.comparisonEpsilon);
        Assert.assertEquals(1.5, point2.getZ(), this.comparisonEpsilon);

        // test with a line parallel to nothing with no intersection...
        originLine = new Vector3D(0.0, 2.0, 4.0);
        directionLine = new Vector3D(1.0, -1.0, 1.0);
        line = new Line(originLine, originLine.add(directionLine));

        closestPoints = cylinder.closestPointTo(line);

        point1 = closestPoints[0];
        point2 = closestPoints[1];

        Assert.assertEquals(-0.5, point1.getX(), this.comparisonEpsilon);
        Assert.assertEquals(2.5, point1.getY(), this.comparisonEpsilon);
        Assert.assertEquals(3.5, point1.getZ(), this.comparisonEpsilon);
        Assert.assertEquals(-0.5, point2.getX(), this.comparisonEpsilon);
        Assert.assertEquals(2.0, point2.getY(), this.comparisonEpsilon);
        Assert.assertEquals(3.0, point2.getZ(), this.comparisonEpsilon);

        // test with a line intersecting the cylinder
        originLine = new Vector3D(0.0, 3.0, 1.0);
        line = new Line(originLine, originLine.add(directionLine));

        closestPoints = cylinder.closestPointTo(line);

        point1 = closestPoints[0];
        point2 = closestPoints[1];

        Assert.assertEquals(1.0, point1.getX(), this.comparisonEpsilon);
        Assert.assertEquals(2.0, point1.getY(), this.comparisonEpsilon);
        Assert.assertEquals(2.0, point1.getZ(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point2.getX(), this.comparisonEpsilon);
        Assert.assertEquals(2.0, point2.getY(), this.comparisonEpsilon);
        Assert.assertEquals(2.0, point2.getZ(), this.comparisonEpsilon);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#INFINITE_RECTANGLE_CYLINDER_SHAPE}
     * 
     * @testedMethod {@link InfiniteRectangleCylinder#toString()}
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

        // creation of the cylinder
        final Vector3D origin = new Vector3D(0.0, 1.0, 1.0);
        final Vector3D direction = new Vector3D(2.0, 0.0, 0.0);
        final Vector3D inputVectorU = new Vector3D(1.0, 1.0, 0.0);
        final InfiniteRectangleCylinder cylinder = new InfiniteRectangleCylinder(origin, direction, inputVectorU, 2.0,
            4.0);

        // string creation
        final String result = cylinder.toString();

        final String expected =
            "InfiniteRectangleCylinder{Origin{0; 1; 1},Direction{1; 0; 0},U vector{0; 1; 0},Length{2.0},Width{4.0}}";
        Assert.assertEquals(expected, result);
    }

}
