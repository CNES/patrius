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
 *              Validation tests for the object EllipticCylinder.
 *              </p>
 * 
 * @author Thomas Trapier
 * 
 * @version $Id: EllipticCylinderTest.java 17909 2017-09-11 11:57:36Z bignon $
 * 
 * @since 1.0
 * 
 */
public class EllipticCylinderTest {

    /** Features description. */
    public enum features {

        /**
         * @featureTitle Elliptic cylinder shape
         * 
         * @featureDescription Creation of an elliptic finite cylinder shape, computation of distances and intersections
         *                     with lines and points.
         * 
         * @coveredRequirements DV-GEOMETRIE_50, DV-GEOMETRIE_60, DV-GEOMETRIE_90, DV-GEOMETRIE_120, DV-GEOMETRIE_130,
         *                      DV-GEOMETRIE_140
         */
        ELLIPTIC_CYLINDER_SHAPE
    }

    /** Epsilon for double comparison. */
    private final double comparisonEpsilon = Precision.DOUBLE_COMPARISON_EPSILON;

    /** Epsilon taking into account the machine error. */
    private final double machineEpsilon = Precision.EPSILON;

    /** Epsilon taking into account the machine error. */
    private final double zeroEpsilon = 0.0;

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ELLIPTIC_CYLINDER_SHAPE}
     * 
     * @testedMethod {@link EllipticCylinder#EllipticCylinder(Vector3D, Vector3D, Vector3D, double, double, double)}
     * 
     * @description Instantiation an elliptic finite cylinder from its origin, axis, approximative u vector, height and
     *              two radiuses.
     * 
     * @input A vector center, a normal vector, u vector, heights and radiuses (positive, null, negative).
     * 
     * @output EllipticCylinder
     * 
     * @testPassCriteria The cylinder can be created only if the radiuses and height are strictly positive, and the
     *                   direction of the axis not null, if the input u vector is not null and not parallel to the
     *                   direction. An exception is thrown otherwise. We check the returned elements (origin, direction,
     *                   u and a radius) with the ones given at the construction with an epsilon of 1e-16 which takes
     *                   into account the machine error only.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testConstructor() {

        // test with corrects inputs
        final Vector3D origin = new Vector3D(0.0, 1.0, 1.0);
        Vector3D direction = new Vector3D(2.0, 0.0, 0.0);
        Vector3D inputUvector = new Vector3D(1.0, 1.0, 0.0);
        double radiusA = 4.0;
        double radiusB = 2.0;
        double height = 5.0;

        EllipticCylinder cylinder = new EllipticCylinder(origin, direction, inputUvector, radiusA, radiusB, height);

        final Vector3D originOut = cylinder.getOrigin();
        Assert.assertEquals(0.0, originOut.getX(), this.machineEpsilon);
        Assert.assertEquals(1.0, originOut.getY(), this.machineEpsilon);
        Assert.assertEquals(1.0, originOut.getZ(), this.machineEpsilon);

        final Vector3D directionOut = cylinder.getDirection();
        Assert.assertEquals(1.0, directionOut.getX(), this.machineEpsilon);
        Assert.assertEquals(0.0, directionOut.getY(), this.machineEpsilon);
        Assert.assertEquals(0.0, directionOut.getZ(), this.machineEpsilon);

        Assert.assertEquals(radiusA, cylinder.getRadiusA(), this.machineEpsilon);
        Assert.assertEquals(radiusB, cylinder.getRadiusB(), this.machineEpsilon);
        Assert.assertEquals(height, cylinder.getHeight(), this.machineEpsilon);

        final Vector3D uOut = cylinder.getU();
        Assert.assertEquals(0.0, uOut.getX(), this.machineEpsilon);
        Assert.assertEquals(1.0, uOut.getY(), this.machineEpsilon);
        Assert.assertEquals(0.0, uOut.getZ(), this.machineEpsilon);

        final Vector3D vOut = cylinder.getV();
        Assert.assertEquals(0.0, vOut.getX(), this.machineEpsilon);
        Assert.assertEquals(0.0, vOut.getY(), this.machineEpsilon);
        Assert.assertEquals(1.0, vOut.getZ(), this.machineEpsilon);

        // test with wrong radiuses
        radiusA = 0.0;
        try {
            cylinder = new EllipticCylinder(origin, direction, inputUvector, radiusA, radiusB, height);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
        radiusA = -1.0;
        try {
            cylinder = new EllipticCylinder(origin, direction, inputUvector, radiusA, radiusB, height);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
        radiusA = 0.84;
        radiusB = 0.0;
        try {
            cylinder = new EllipticCylinder(origin, direction, inputUvector, radiusA, radiusB, height);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
        radiusB = -1.0;
        try {
            cylinder = new EllipticCylinder(origin, direction, inputUvector, radiusA, radiusB, height);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
        // test with wrong direction
        radiusB = 0.84;
        direction = new Vector3D(0.0, 0.0, 0.0);
        try {
            cylinder = new EllipticCylinder(origin, direction, inputUvector, radiusA, radiusB, height);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }

        // tests with wrong u vector
        direction = new Vector3D(2.0, 0.0, 0.0);
        inputUvector = new Vector3D(0.0, 0.0, 0.0);
        try {
            cylinder = new EllipticCylinder(origin, direction, inputUvector, radiusA, radiusB, height);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
        inputUvector = new Vector3D(3.0, 0.0, 0.0);
        try {
            cylinder = new EllipticCylinder(origin, direction, inputUvector, radiusA, radiusB, height);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }

        // test with wrong height
        inputUvector = new Vector3D(0.0, 1.0, 0.0);
        height = 0.0;
        try {
            cylinder = new EllipticCylinder(origin, direction, inputUvector, radiusA, radiusB, height);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
        height = -1.0;
        try {
            cylinder = new EllipticCylinder(origin, direction, inputUvector, radiusA, radiusB, height);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ELLIPTIC_CYLINDER_SHAPE}
     * 
     * @testedMethod {@link EllipticCylinder#distanceTo(Line)}
     * 
     * @description Compute the shortest distance between the surface of the cylinder and a Line.
     * 
     * @input Lines of space
     * 
     * @output doubles : the distances
     * 
     * @testPassCriteria The output doubles must be the right distance (with an epsilon of 1e-14 due to the computation
     *                   errors), positive if the line does not intersect the surface and zero otherwise.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testDistanceToLine() {

        // Cylinder creation
        final Vector3D origin = new Vector3D(0.0, 1.0, 1.0);
        final Vector3D direction = new Vector3D(2.0, 0.0, 0.0);
        final Vector3D inputUvector = new Vector3D(1.0, 1.0, 0.0);
        final double radiusA = 4.0;
        final double radiusB = 2.0;
        final double height = 5.0;

        final EllipticCylinder cylinder = new EllipticCylinder(origin, direction, inputUvector, radiusA, radiusB,
            height);

        // test with a line intersecting the side
        Vector3D origLine = new Vector3D(0.0, 1.0, 1.0);
        Vector3D dirLine = new Vector3D(0.0, 4.0, -2.0);
        Line line = new Line(origLine, origLine.add(dirLine));

        double distance = cylinder.distanceTo(line);
        Assert.assertEquals(0.0, distance, this.zeroEpsilon);

        // test with a line intersecting the ending ellipses
        origLine = new Vector3D(0.0, 3.0, 2.0);
        dirLine = new Vector3D(4.0, 0.0, 0.0);
        line = new Line(origLine, origLine.add(dirLine));

        distance = cylinder.distanceTo(line);
        Assert.assertEquals(0.0, distance, this.zeroEpsilon);

        // test with a line closest to the side
        origLine = new Vector3D(0.0, 6.0, 1.0);
        dirLine = new Vector3D(0.0, 0.0, 1.0);
        line = new Line(origLine, origLine.add(dirLine));

        distance = cylinder.distanceTo(line);
        Assert.assertEquals(1.0, distance, this.comparisonEpsilon);

        // test with a line intersecting the infinite cylinder but not the finite one :
        // closest to the up ending ellipse
        origLine = new Vector3D(2.5, 1.0, 4.0);
        dirLine = new Vector3D(-1.0, 0.0, 1.0);
        line = new Line(origLine, origLine.add(dirLine));

        distance = cylinder.distanceTo(line);
        Assert.assertEquals(MathLib.sqrt(2.0) / 2.0, distance, this.comparisonEpsilon);

        // test with a line intersecting the infinite cylinder but not the finite one :
        // closest to the bottom ending ellipse
        origLine = new Vector3D(-2.5, 1.0, 4.0);
        dirLine = new Vector3D(1.0, 0.0, 1.0);
        line = new Line(origLine, origLine.add(dirLine));

        distance = cylinder.distanceTo(line);
        Assert.assertEquals(MathLib.sqrt(2.0) / 2.0, distance, this.comparisonEpsilon);

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ELLIPTIC_CYLINDER_SHAPE}
     * 
     * @testedMethod {@link EllipticCylinder#intersects(Line)}
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
        final Vector3D inputUvector = new Vector3D(1.0, 1.0, 0.0);
        final double radiusA = 4.0;
        final double radiusB = 2.0;
        final double height = 5.0;

        final EllipticCylinder cylinder = new EllipticCylinder(origin, direction, inputUvector, radiusA, radiusB,
            height);

        // test with a line intersecting the side
        Vector3D origLine = new Vector3D(0.0, 1.0, 1.0);
        Vector3D dirLine = new Vector3D(0.0, 4.0, -2.0);
        Line line = new Line(origLine, origLine.add(dirLine));

        boolean intersects = cylinder.intersects(line);
        Assert.assertTrue(intersects);

        // test with a line intersecting the ending ellipses
        origLine = new Vector3D(0.0, 3.0, 2.0);
        dirLine = new Vector3D(4.0, 0.0, 0.0);
        line = new Line(origLine, origLine.add(dirLine));

        intersects = cylinder.intersects(line);
        Assert.assertTrue(intersects);

        // test with a line closest to the side, intersecting nothing
        origLine = new Vector3D(0.0, 6.0, 1.0);
        dirLine = new Vector3D(0.0, 0.0, 1.0);
        line = new Line(origLine, origLine.add(dirLine));

        intersects = cylinder.intersects(line);
        Assert.assertTrue(!intersects);

        // test with a line intersecting the infinite cylinder but not the finite one :
        // closest to the up ending ellipse
        origLine = new Vector3D(2.5, 1.0, 4.0);
        dirLine = new Vector3D(-1.0, 0.0, 1.0);
        line = new Line(origLine, origLine.add(dirLine));

        intersects = cylinder.intersects(line);
        Assert.assertTrue(!intersects);

        // test with a line intersecting the infinite cylinder but not the finite one :
        // closest to the bottom ending ellipse
        origLine = new Vector3D(-2.5, 1.0, 4.0);
        dirLine = new Vector3D(1.0, 0.0, 1.0);
        line = new Line(origLine, origLine.add(dirLine));

        intersects = cylinder.intersects(line);
        Assert.assertTrue(!intersects);

        // test with a line intersecting nothing
        origLine = new Vector3D(-2.5, 6.0, 4.0);
        dirLine = new Vector3D(1.0, 0.0, 1.0);
        line = new Line(origLine, origLine.add(dirLine));

        intersects = cylinder.intersects(line);
        Assert.assertTrue(!intersects);

        // test with a line intersecting the side and an ending ellipse
        origLine = new Vector3D(2, 1.0, 1.0);
        dirLine = new Vector3D(1.0, 0.0, 1.0);
        line = new Line(origLine, origLine.add(dirLine));

        intersects = cylinder.intersects(line);
        Assert.assertTrue(intersects);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ELLIPTIC_CYLINDER_SHAPE}
     * 
     * @testedMethod {@link EllipticCylinder#getIntersectionPoints(Line)}
     * 
     * @description Compute the intersection points with a line.
     * 
     * @input Lines of space
     * 
     * @output Vector3D[]
     * 
     * @testPassCriteria The result array is empty if there is no intersection point. The points have the expected
     *                   coordinates otherwise (with an epsilon of 1e-14 due to the computation errors).
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
        final Vector3D inputUvector = new Vector3D(1.0, 1.0, 0.0);
        final double radiusA = 4.0;
        final double radiusB = 2.0;
        final double height = 6.0;

        final EllipticCylinder cylinder = new EllipticCylinder(origin, direction, inputUvector, radiusA, radiusB,
            height);

        // test with a line intersecting the side
        Vector3D origLine = new Vector3D(0.0, 1.0, 1.0);
        Vector3D dirLine = new Vector3D(0.0, 4.0, 0.0);
        Line line = new Line(origLine, origLine.add(dirLine));

        Vector3D[] intersections = cylinder.getIntersectionPoints(line);
        Assert.assertEquals(2, intersections.length);

        Vector3D intersect1 = intersections[0];
        Vector3D intersect2 = intersections[1];

        Assert.assertEquals(0.0, intersect1.getX(), this.comparisonEpsilon);
        Assert.assertEquals(5.0, intersect1.getY(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, intersect1.getZ(), this.comparisonEpsilon);

        Assert.assertEquals(0.0, intersect2.getX(), this.comparisonEpsilon);
        Assert.assertEquals(-3.0, intersect2.getY(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, intersect2.getZ(), this.comparisonEpsilon);

        // test with a line intersecting the ending ellipses
        origLine = new Vector3D(0.0, 3.0, 2.0);
        dirLine = new Vector3D(4.0, 0.0, 0.0);
        line = new Line(origLine, origLine.add(dirLine));

        intersections = cylinder.getIntersectionPoints(line);
        Assert.assertEquals(2, intersections.length);

        intersect1 = intersections[0];
        intersect2 = intersections[1];

        Assert.assertEquals(3.0, intersect1.getX(), this.comparisonEpsilon);
        Assert.assertEquals(3.0, intersect1.getY(), this.comparisonEpsilon);
        Assert.assertEquals(2.0, intersect1.getZ(), this.comparisonEpsilon);

        Assert.assertEquals(-3.0, intersect2.getX(), this.comparisonEpsilon);
        Assert.assertEquals(3.0, intersect2.getY(), this.comparisonEpsilon);
        Assert.assertEquals(2.0, intersect2.getZ(), this.comparisonEpsilon);

        // test with a line closest to the side, intersecting nothing
        origLine = new Vector3D(0.0, 6.0, 1.0);
        dirLine = new Vector3D(0.0, 0.0, 1.0);
        line = new Line(origLine, origLine.add(dirLine));

        intersections = cylinder.getIntersectionPoints(line);
        Assert.assertEquals(0, intersections.length);

        // test with a line intersecting the infinite cylinder but not the finite one :
        // closest to the up ending ellipse
        origLine = new Vector3D(2.5, 1.0, 4.0);
        dirLine = new Vector3D(-1.0, 0.0, 1.0);
        line = new Line(origLine, origLine.add(dirLine));

        intersections = cylinder.getIntersectionPoints(line);
        Assert.assertEquals(0, intersections.length);

        // test with a line intersecting the side and an ending ellipse
        origLine = new Vector3D(2.0, 1.0, 1.0);
        dirLine = new Vector3D(1.0, 0.0, 1.0);
        line = new Line(origLine, origLine.add(dirLine));

        intersections = cylinder.getIntersectionPoints(line);
        Assert.assertEquals(2, intersections.length);

        intersect1 = intersections[0];
        intersect2 = intersections[1];

        Assert.assertEquals(0.0, intersect1.getX(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, intersect1.getY(), this.comparisonEpsilon);
        Assert.assertEquals(-1.0, intersect1.getZ(), this.comparisonEpsilon);

        Assert.assertEquals(3.0, intersect2.getX(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, intersect2.getY(), this.comparisonEpsilon);
        Assert.assertEquals(2.0, intersect2.getZ(), this.comparisonEpsilon);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ELLIPTIC_CYLINDER_SHAPE}
     * 
     * @testedMethod {@link EllipticCylinder#closestPointTo(Line)}
     * 
     * @description Compute the point of the elliptic cylinder realizing the shortest distance to a line of space, and
     *              the associated point of the line.
     * 
     * @input Points of space (Vector3D)
     * 
     * @output Vector3D[]
     * 
     * @testPassCriteria The output vector must be the one of the shape and the one of the line realizing the shortest
     *                   distance (with an epsilon of 1e-14 due to the computation errors).
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
        final Vector3D inputUvector = new Vector3D(1.0, 1.0, 0.0);
        final double radiusA = 4.0;
        final double radiusB = 2.0;
        final double height = 6.0;

        final EllipticCylinder cylinder = new EllipticCylinder(origin, direction, inputUvector, radiusA, radiusB,
            height);

        // test with a line intersecting the side
        Vector3D origLine = new Vector3D(0.0, 1.0, 1.0);
        Vector3D dirLine = new Vector3D(0.0, 4.0, 0.0);
        Line line = new Line(origLine, origLine.add(dirLine));

        Vector3D[] closestPoints = cylinder.closestPointTo(line);

        Vector3D point1 = closestPoints[0];
        Vector3D point2 = closestPoints[1];

        Assert.assertEquals(0.0, point1.getX(), this.comparisonEpsilon);
        Assert.assertEquals(5.0, point1.getY(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point1.getZ(), this.comparisonEpsilon);
        Assert.assertEquals(0.0, point2.getX(), this.comparisonEpsilon);
        Assert.assertEquals(5.0, point2.getY(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point2.getZ(), this.comparisonEpsilon);

        // test with a line intersecting the infinite
        // cylinder but not the finite one
        // closest to up ellipse
        origLine = new Vector3D(4.0, 1.0, 4.0);
        dirLine = new Vector3D(1.0, 0.0, -1.0);
        line = new Line(origLine, origLine.add(dirLine));

        closestPoints = cylinder.closestPointTo(line);

        point1 = closestPoints[0];
        point2 = closestPoints[1];

        Assert.assertEquals(4.0, point1.getX(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point1.getY(), this.comparisonEpsilon);
        Assert.assertEquals(4.0, point1.getZ(), this.comparisonEpsilon);
        Assert.assertEquals(3.0, point2.getX(), this.comparisonEpsilon);
        Assert.assertEquals(3.0, point2.getZ(), this.comparisonEpsilon);

        // test with a line intersecting nothing
        origLine = new Vector3D(2.0, 1.0, 4.0);
        dirLine = new Vector3D(0.0, 1.0, 0.0);
        line = new Line(origLine, origLine.add(dirLine));

        closestPoints = cylinder.closestPointTo(line);

        point1 = closestPoints[0];
        point2 = closestPoints[1];

        Assert.assertEquals(2.0, point1.getX(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point1.getY(), this.comparisonEpsilon);
        Assert.assertEquals(4.0, point1.getZ(), this.comparisonEpsilon);
        Assert.assertEquals(2.0, point2.getX(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point2.getY(), this.comparisonEpsilon);
        Assert.assertEquals(3.0, point2.getZ(), this.comparisonEpsilon);

        // test with a line intersecting the infinite
        // cylinder but not the finite one
        // closest to bottom ellipse
        origLine = new Vector3D(-4.0, 1.0, 4.0);
        dirLine = new Vector3D(1.0, 0.0, 1.0);
        line = new Line(origLine, origLine.add(dirLine));

        closestPoints = cylinder.closestPointTo(line);

        point1 = closestPoints[0];
        point2 = closestPoints[1];

        Assert.assertEquals(-4.0, point1.getX(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point1.getY(), this.comparisonEpsilon);
        Assert.assertEquals(4.0, point1.getZ(), this.comparisonEpsilon);
        Assert.assertEquals(-3.0, point2.getX(), this.comparisonEpsilon);
        Assert.assertEquals(3.0, point2.getZ(), this.comparisonEpsilon);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ELLIPTIC_CYLINDER_SHAPE}
     * 
     * @testedMethod {@link EllipticCylinder#toString()}
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
        final Vector3D inputUvector = new Vector3D(1.0, 1.0, 0.0);
        final double radiusA = 4.0;
        final double radiusB = 2.0;
        final double height = 6.0;

        final EllipticCylinder cylinder = new EllipticCylinder(origin, direction, inputUvector, radiusA, radiusB,
            height);

        // string creation
        final String result = cylinder.toString();

        final String expected =
            "EllipticCylinder{Origin{0; 1; 1},Direction{1; 0; 0},U vector{0; 1; 0},Radius A{4.0},Radius B{2.0},Height{6.0}}";
        Assert.assertEquals(expected, result);
    }
}
