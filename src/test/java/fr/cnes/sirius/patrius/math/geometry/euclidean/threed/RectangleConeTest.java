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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
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
 *              Validation tests for the object RectangleCone.
 *              </p>
 * 
 * @author Thomas Trapier
 * 
 * @version $Id: RectangleConeTest.java 17909 2017-09-11 11:57:36Z bignon $
 * 
 * @since 1.0
 * 
 */
public class RectangleConeTest {
    /** Features description. */
    public enum features {

        /**
         * @featureTitle Rectangle cone shape
         * 
         * @featureDescription Creation of an rectangle cone shape, computation of distances and intersections with
         *                     lines and points.
         * 
         * @coveredRequirements DV-GEOMETRIE_50, DV-GEOMETRIE_60, DV-GEOMETRIE_90, DV-GEOMETRIE_120, DV-GEOMETRIE_130,
         *                      DV-GEOMETRIE_140
         */
        RECTANGLE_CONE_SHAPE
    }

    /** Epsilon for double comparison. */
    private final double comparisonEpsilon = Precision.DOUBLE_COMPARISON_EPSILON;

    /** Epsilon taking into account the machine error. */
    private final double machineEpsilon = Precision.EPSILON;

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#RECTANGLE_CONE_SHAPE}
     * 
     * @testedMethod {@link RectangleCone#RectangleCone(Vector3D, Vector3D, Vector3D, double, double, double)}
     * 
     * @description Instantiation of a an infinite right circular cone from its origin, axis' direction, height and
     *              basis dimensions.
     * 
     * @input A point origin, a vector direction, a vector U, several doubles as dimensions (correct, negative, zero)
     * 
     * @output RectangleCone
     * 
     * @testPassCriteria The cone can be created only if the direction's norm, the U vector's norm and the dimensions
     *                   are strictly positive, and if the u vector and the direction are not parallel. We check the
     *                   returned elements with the ones given at the construction with an epsilon of 1e-16 which takes
     *                   into account the machine error only. An exception is thrown otherwise.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testConstructor() {

        // creation of the cone
        final Vector3D originCone = new Vector3D(1.0, 1.0, 1.0);
        Vector3D direction = new Vector3D(2.0, 0.0, 0.0);
        Vector3D inputVectorU = new Vector3D(1.0, 1.0, 0.0);
        double length = 4.0;
        double width = 2.0;
        double height = 5.0;

        RectangleCone cone = new RectangleCone(originCone, direction, inputVectorU, length, width, height);

        final Vector3D realOrigin = cone.getOrigin();
        Assert.assertEquals(originCone.getX(), realOrigin.getX(), this.machineEpsilon);
        Assert.assertEquals(originCone.getY(), realOrigin.getY(), this.machineEpsilon);
        Assert.assertEquals(originCone.getZ(), realOrigin.getZ(), this.machineEpsilon);

        final Vector3D realDirection = cone.getDirection();
        Assert.assertEquals(1.0, realDirection.getX(), this.machineEpsilon);
        Assert.assertEquals(0.0, realDirection.getY(), this.machineEpsilon);
        Assert.assertEquals(0.0, realDirection.getZ(), this.machineEpsilon);

        final Vector3D realU = cone.getU();
        Assert.assertEquals(0.0, realU.getX(), this.machineEpsilon);
        Assert.assertEquals(1.0, realU.getY(), this.machineEpsilon);
        Assert.assertEquals(0.0, realU.getZ(), this.machineEpsilon);

        final Vector3D realV = cone.getV();
        Assert.assertEquals(0.0, realV.getX(), this.machineEpsilon);
        Assert.assertEquals(0.0, realV.getY(), this.machineEpsilon);
        Assert.assertEquals(1.0, realV.getZ(), this.machineEpsilon);

        Assert.assertEquals(height, cone.getHeight(), this.machineEpsilon);
        Assert.assertEquals(length, cone.getLength(), this.machineEpsilon);
        Assert.assertEquals(width, cone.getWidth(), this.machineEpsilon);

        // test with wrong axis
        try {
            direction = new Vector3D(0.0, 0.0, 0.0);
            cone = new RectangleCone(originCone, direction, inputVectorU, length, width, height);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
        // test with wrong U vector
        try {
            direction = new Vector3D(2.0, 0.0, 0.0);
            inputVectorU = new Vector3D(0.0, 0.0, 0.0);
            cone = new RectangleCone(originCone, direction, inputVectorU, length, width, height);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
        try {
            inputVectorU = new Vector3D(1.0, 0.0, 0.0);
            cone = new RectangleCone(originCone, direction, inputVectorU, length, width, height);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }

        // test with wrong dimensions
        try {
            inputVectorU = new Vector3D(0.0, 1.0, 0.0);
            length = 0.0;
            cone = new RectangleCone(originCone, direction, inputVectorU, length, width, height);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
        try {
            inputVectorU = new Vector3D(0.0, 1.0, 0.0);
            length = -1.0;
            cone = new RectangleCone(originCone, direction, inputVectorU, length, width, height);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
        try {
            inputVectorU = new Vector3D(0.0, 1.0, 0.0);
            length = 1.0;
            width = 0.0;
            cone = new RectangleCone(originCone, direction, inputVectorU, length, width, height);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
        try {
            inputVectorU = new Vector3D(0.0, 1.0, 0.0);
            width = -1.0;
            cone = new RectangleCone(originCone, direction, inputVectorU, length, width, height);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
        try {
            inputVectorU = new Vector3D(0.0, 1.0, 0.0);
            height = 0.0;
            width = 1.0;
            cone = new RectangleCone(originCone, direction, inputVectorU, length, width, height);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
        try {
            inputVectorU = new Vector3D(0.0, 1.0, 0.0);
            height = -1.0;
            cone = new RectangleCone(originCone, direction, inputVectorU, length, width, height);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#RECTANGLE_CONE_SHAPE}
     * 
     * @testedMethod {@link RectangleCone#distanceTo(Line)}
     * 
     * @description Compute the shortest distance between the surface of the cone and a Line.
     * 
     * @input Lines of space
     * 
     * @output doubles : the distances
     * 
     * @testPassCriteria The output doubles must be the right distance, positive if the line does not intersect the
     *                   surface, zero otherwise with an epsilon of 1e-14 due to the computation errors.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testDistanceToLine() {

        // creation of the cone
        final Vector3D originCone = new Vector3D(0.0, 1.0, 1.0);
        final Vector3D direction = new Vector3D(2.0, 0.0, 0.0);
        final Vector3D inputVectorU = new Vector3D(1.0, 1.0, 0.0);
        final double length = 8.0;
        final double width = 2.0;
        final double height = 4.0;
        final RectangleCone cone = new RectangleCone(originCone, direction, inputVectorU, length, width, height);

        // test with a line intersecting the cone
        Vector3D lineDir = new Vector3D(0.0, 0.0, 1.0);
        Vector3D lineOrig = new Vector3D(3.0, 0.0, 0.0);
        Line line = new Line(lineOrig, lineOrig.add(lineDir));
        double distance = cone.distanceTo(line);
        Assert.assertEquals(0.0, distance, this.comparisonEpsilon);

        // test with a line intersecting the infinite cone
        // but not the finite cone, closest to the plate
        lineDir = new Vector3D(0.0, 0.0, 1.0);
        lineOrig = new Vector3D(5.0, 0.0, 0.0);
        line = new Line(lineOrig, lineOrig.add(lineDir));
        distance = cone.distanceTo(line);
        Assert.assertEquals(1.0, distance, this.comparisonEpsilon);

        // test with a line closest to an edge
        lineOrig = new Vector3D(0.0, 0.0, 0.0);
        lineDir = new Vector3D(0.0, 0.0, 1.0);
        line = new Line(lineOrig, lineOrig.add(lineDir));
        distance = cone.distanceTo(line);
        Assert.assertEquals(MathLib.sqrt(2.0) / 2.0, distance, this.comparisonEpsilon);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#RECTANGLE_CONE_SHAPE}
     * 
     * @testedMethod {@link RectangleCone#intersects(Line)}
     * 
     * @description Test the intersection between the cone and a line.
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
        // creation of the cone
        final Vector3D originCone = new Vector3D(0.0, 1.0, 1.0);
        final Vector3D direction = new Vector3D(2.0, 0.0, 0.0);
        final Vector3D inputVectorU = new Vector3D(1.0, 1.0, 0.0);
        final double length = 8.0;
        final double width = 8.0;
        final double height = 4.0;
        final RectangleCone cone = new RectangleCone(originCone, direction, inputVectorU, length, width, height);

        // test with a line intersecting the cone
        Vector3D lineDir = new Vector3D(0.0, 0.0, 1.0);
        Vector3D lineOrig = new Vector3D(3.0, 0.0, 0.0);
        Line line = new Line(lineOrig, lineOrig.add(lineDir));
        boolean intersects = cone.intersects(line);
        Assert.assertTrue(intersects);

        // test with a line intersecting the infinite cone
        // but not the finite cone
        lineDir = new Vector3D(0.0, 0.0, 1.0);
        lineOrig = new Vector3D(5.0, 0.0, 0.0);
        line = new Line(lineOrig, lineOrig.add(lineDir));
        intersects = cone.intersects(line);
        Assert.assertTrue(!intersects);

        // test with a line intersecting nothing
        lineDir = new Vector3D(0.0, 0.0, 1.0);
        lineOrig = new Vector3D(0.0, 2.0, 0.0);
        line = new Line(lineOrig, lineOrig.add(lineDir));
        intersects = cone.intersects(line);
        Assert.assertTrue(!intersects);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#RECTANGLE_CONE_SHAPE}
     * 
     * @testedMethod {@link RectangleCone#getIntersectionPoints(Line)}
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

        // creation of the cone
        final Vector3D originCone = new Vector3D(1.0, 1.0, 1.0);
        final Vector3D direction = new Vector3D(2.0, 0.0, 0.0);
        final Vector3D inputVectorU = new Vector3D(1.0, 1.0, 0.0);
        final double length = 8.0;
        final double width = 8.0;
        final double height = 4.0;
        RectangleCone cone = new RectangleCone(originCone, direction, inputVectorU, length, width, height);

        // test with a line twice intersecting the finite cone
        // but not the plate
        Vector3D originLine = new Vector3D(2.0, 1.0, 0.0);
        Vector3D dirLine = new Vector3D(0.0, 0.0, 1.0);
        Line line = new Line(originLine, originLine.add(dirLine));
        Vector3D[] intersections = cone.getIntersectionPoints(line);
        Assert.assertEquals(intersections.length, 2);

        Vector3D intersect1 = intersections[0];
        Vector3D intersect2 = intersections[1];

        Assert.assertEquals(2.0, intersect1.getX(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, intersect1.getY(), this.comparisonEpsilon);
        Assert.assertEquals(2.0, intersect1.getZ(), this.comparisonEpsilon);

        Assert.assertEquals(2.0, intersect2.getX(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, intersect2.getY(), this.comparisonEpsilon);
        Assert.assertEquals(0.0, intersect2.getZ(), this.comparisonEpsilon);

        // test with a line intersecting the finite cone
        // and the plate
        originLine = new Vector3D(1.0, 1.0, 2.0);
        dirLine = new Vector3D(1.0, 0.0, 0.0);
        line = new Line(originLine, originLine.add(dirLine));
        intersections = cone.getIntersectionPoints(line);
        Assert.assertEquals(intersections.length, 2);

        intersect1 = intersections[0];
        intersect2 = intersections[1];

        Assert.assertEquals(2.0, intersect1.getX(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, intersect1.getY(), this.comparisonEpsilon);
        Assert.assertEquals(2.0, intersect1.getZ(), this.comparisonEpsilon);

        Assert.assertEquals(5.0, intersect2.getX(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, intersect2.getY(), this.comparisonEpsilon);
        Assert.assertEquals(2.0, intersect2.getZ(), this.comparisonEpsilon);

        // test with a line intersecting the infinite cone
        // but not the finite cone
        dirLine = new Vector3D(0.0, 0.0, 1.0);
        originLine = new Vector3D(9.0, 0.0, 0.0);
        line = new Line(originLine, originLine.add(dirLine));
        intersections = cone.getIntersectionPoints(line);
        Assert.assertEquals(intersections.length, 0);

        // test with a line intersecting nothing
        dirLine = new Vector3D(0.0, 0.0, 1.0);
        originLine = new Vector3D(0.0, 2.0, 0.0);
        line = new Line(originLine, originLine.add(dirLine));
        intersections = cone.getIntersectionPoints(line);
        Assert.assertEquals(intersections.length, 0);

        // limit test case
        final Vector3D inOrigin = new Vector3D(0, 0, 10);
        final Vector3D inDirection = Vector3D.MINUS_K;
        final Vector3D inUvector = Vector3D.PLUS_J;
        final double inLength = 10;
        final double inWidth = 5;
        final double inHeight = 20;
        cone = new RectangleCone(inOrigin, inDirection, inUvector, inLength, inWidth, inHeight);

        final Vector3D p1 = new Vector3D(0.625, 1.25, 5);
        final Vector3D v1 = new Vector3D(1, 1, 0);
        final Line l1 = new Line(p1, p1.add(v1));

        intersections = cone.getIntersectionPoints(l1);
        Assert.assertEquals(intersections.length, 2);

        intersect1 = intersections[0];
        intersect2 = intersections[1];

        Assert.assertEquals(0.625, intersect1.getX(), this.comparisonEpsilon);
        Assert.assertEquals(1.25, intersect1.getY(), this.comparisonEpsilon);
        Assert.assertEquals(5.0, intersect1.getZ(), this.comparisonEpsilon);

        Assert.assertEquals(-0.625, intersect2.getX(), this.comparisonEpsilon);
        Assert.assertEquals(0.0, intersect2.getY(), this.comparisonEpsilon);
        Assert.assertEquals(5.0, intersect2.getZ(), this.comparisonEpsilon);

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#RECTANGLE_CONE_SHAPE}
     * 
     * @testedMethod {@link RectangleCone#closestPointTo(Line)}
     * 
     * @description Compute the point of the cone realizing the shortest distance to a line of space, and the associated
     *              point of the line.
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

        // creation of the cone
        final Vector3D originCone = new Vector3D(1.0, 1.0, 1.0);
        final Vector3D direction = new Vector3D(2.0, 0.0, 0.0);
        final Vector3D inputVectorU = new Vector3D(1.0, 1.0, 0.0);
        final double length = 8.0;
        final double width = 8.0;
        final double height = 4.0;
        final RectangleCone cone = new RectangleCone(originCone, direction, inputVectorU, length, width, height);

        // test with an intersecting line
        Vector3D originLine = new Vector3D(2.0, 1.0, 0.0);
        Vector3D dirLine = new Vector3D(0.0, 0.0, 1.0);
        Line line = new Line(originLine, originLine.add(dirLine));

        Vector3D[] closestPoints = cone.closestPointTo(line);

        Vector3D point1 = closestPoints[0];
        Vector3D point2 = closestPoints[1];

        Assert.assertEquals(2.0, point1.getX(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point1.getY(), this.comparisonEpsilon);
        Assert.assertEquals(0.0, point1.getZ(), this.comparisonEpsilon);
        Assert.assertEquals(2.0, point2.getX(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point2.getY(), this.comparisonEpsilon);
        Assert.assertEquals(0.0, point2.getZ(), this.comparisonEpsilon);

        // test with a non intersecting line closest to the
        // apex (origin)
        originLine = new Vector3D(-1.0, 5.0, 0.0);
        dirLine = new Vector3D(0.0, 1.0, 0.0);
        line = new Line(originLine, originLine.add(dirLine));

        closestPoints = cone.closestPointTo(line);

        point1 = closestPoints[0];
        point2 = closestPoints[1];

        Assert.assertEquals(-1.0, point1.getX(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point1.getY(), this.comparisonEpsilon);
        Assert.assertEquals(0.0, point1.getZ(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point2.getX(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point2.getY(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point2.getZ(), this.comparisonEpsilon);

        // test with a non intersecting line closest to a side
        originLine = new Vector3D(1.0, 5.0, 3.0);
        dirLine = new Vector3D(0.0, 1.0, 0.0);
        line = new Line(originLine, originLine.add(dirLine));

        closestPoints = cone.closestPointTo(line);

        point1 = closestPoints[0];
        point2 = closestPoints[1];

        Assert.assertEquals(1.0, point1.getX(), this.comparisonEpsilon);
        Assert.assertEquals(0.0, point1.getY(), this.comparisonEpsilon);
        Assert.assertEquals(3.0, point1.getZ(), this.comparisonEpsilon);
        Assert.assertEquals(2.0, point2.getX(), this.comparisonEpsilon);
        Assert.assertEquals(0.0, point2.getY(), this.comparisonEpsilon);
        Assert.assertEquals(2.0, point2.getZ(), this.comparisonEpsilon);

        // test with a non intersecting line closest to the ending plate
        originLine = new Vector3D(4.0, 1.0, 8.0);
        dirLine = new Vector3D(1.0, 0.0, -1.0);
        line = new Line(originLine, originLine.add(dirLine));

        closestPoints = cone.closestPointTo(line);

        point1 = closestPoints[0];
        point2 = closestPoints[1];

        Assert.assertEquals(6.0, point1.getX(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point1.getY(), this.comparisonEpsilon);
        Assert.assertEquals(6.0, point1.getZ(), this.comparisonEpsilon);
        Assert.assertEquals(5.0, point2.getX(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point2.getY(), this.comparisonEpsilon);
        Assert.assertEquals(5.0, point2.getZ(), this.comparisonEpsilon);

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#RECTANGLE_CONE_SHAPE}
     * 
     * @testedMethod {@link RectangleCone#toString()}
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

        // creation of the cone
        final Vector3D originCone = new Vector3D(1.0, 1.0, 1.0);
        final Vector3D direction = new Vector3D(2.0, 0.0, 0.0);
        final Vector3D inputVectorU = new Vector3D(1.0, 1.0, 0.0);
        final double length = 8.0;
        final double width = 8.0;
        final double height = 4.0;
        final RectangleCone cone = new RectangleCone(originCone, direction, inputVectorU, length, width, height);

        // string creation
        final String result = cone.toString();

        final String expected =
            "RectangleCone{Origin{1; 1; 1},Direction{1; 0; 0},U vector{0; 1; 0},Length{8.0},Width{8.0},Height{4.0}}";
        Assert.assertEquals(expected, result);
    }

}
