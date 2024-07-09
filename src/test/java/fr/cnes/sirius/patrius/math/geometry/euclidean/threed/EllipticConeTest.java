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

import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;

/**
 * @description <p>
 *              Validation tests for the object EllipticCone.
 *              </p>
 * 
 * @author Thomas Trapier
 * 
 * @version $Id: EllipticConeTest.java 17909 2017-09-11 11:57:36Z bignon $
 * 
 * @since 1.0
 * 
 */
public class EllipticConeTest {
    /** Features description. */
    public enum features {

        /**
         * @featureTitle Oblique circular cone shape
         * 
         * @featureDescription Creation of an elliptic cone shape, computation of distances and intersections
         *                     with lines and points.
         * 
         * @coveredRequirements DV-GEOMETRIE_50, DV-GEOMETRIE_60, DV-GEOMETRIE_90, DV-GEOMETRIE_120, DV-GEOMETRIE_130,
         *                      DV-GEOMETRIE_140
         */
        ELLIPTIC_CONE_SHAPE
    }

    /** Epsilon for double comparison. */
    private final double comparisonEpsilon = Precision.DOUBLE_COMPARISON_EPSILON;

    /** Epsilon taking into account the machine error. */
    private final double machineEpsilon = Precision.EPSILON;

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ELLIPTIC_CONE_SHAPE}
     * 
     * @testedMethod {@link EllipticCone#EllipticCone(Vector3D, Vector3D, Vector3D, double, double, double)}
     * 
     * @description Instantiation of an elliptic cone from its origin, axis, approximative u vector, height and
     *              two angles.
     * 
     * @input A vector center, a normal vector, u vector, heights and radiuses (positive, null, negative).
     * 
     * @output EllipticCone
     * 
     * @testPassCriteria The cone can be created only if the angles and height are strictly positive, and the direction
     *                   of the axis not null, if the input u vector is not null and not parallel to the direction. We
     *                   check the returned elements (origin, direction, input vector u) with the ones given at the
     *                   construction with an epsilon of 1e-16 which takes into account the machine error only. An
     *                   exception is thrown otherwise.
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
        double angleU = 0.84;
        double angleV = 0.74;
        double height = 5.0;

        EllipticCone cone = new EllipticCone(origin, direction, inputUvector, angleU, angleV, height);

        final Vector3D originOut = cone.getOrigin();
        Assert.assertEquals(0.0, originOut.getX(), this.machineEpsilon);
        Assert.assertEquals(1.0, originOut.getY(), this.machineEpsilon);
        Assert.assertEquals(1.0, originOut.getZ(), this.machineEpsilon);

        final Vector3D directionOut = cone.getDirection();
        Assert.assertEquals(1.0, directionOut.getX(), this.machineEpsilon);
        Assert.assertEquals(0.0, directionOut.getY(), this.machineEpsilon);
        Assert.assertEquals(0.0, directionOut.getZ(), this.machineEpsilon);

        Assert.assertEquals(angleU, cone.getAngleU(), this.machineEpsilon);
        Assert.assertEquals(angleV, cone.getAngleV(), this.machineEpsilon);
        Assert.assertEquals(height, cone.getHeight(), this.machineEpsilon);

        final Vector3D uOut = cone.getU();
        Assert.assertEquals(0.0, uOut.getX(), this.machineEpsilon);
        Assert.assertEquals(1.0, uOut.getY(), this.machineEpsilon);
        Assert.assertEquals(0.0, uOut.getZ(), this.machineEpsilon);

        final Vector3D vOut = cone.getV();
        Assert.assertEquals(0.0, vOut.getX(), this.machineEpsilon);
        Assert.assertEquals(0.0, vOut.getY(), this.machineEpsilon);
        Assert.assertEquals(1.0, vOut.getZ(), this.machineEpsilon);

        // test with wrong angles
        angleU = 0.0;
        try {
            cone = new EllipticCone(origin, direction, inputUvector, angleU, angleV, height);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
        angleU = -1.0;
        try {
            cone = new EllipticCone(origin, direction, inputUvector, angleU, angleV, height);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
        angleU = 2.0;
        try {
            cone = new EllipticCone(origin, direction, inputUvector, angleU, angleV, height);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
        angleU = 0.84;
        angleV = 0.0;
        try {
            cone = new EllipticCone(origin, direction, inputUvector, angleU, angleV, height);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
        angleV = -1.0;
        try {
            cone = new EllipticCone(origin, direction, inputUvector, angleU, angleV, height);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
        angleV = 2.0;
        try {
            cone = new EllipticCone(origin, direction, inputUvector, angleU, angleV, height);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
        // test with wrong direction
        angleV = 0.84;
        direction = new Vector3D(0.0, 0.0, 0.0);
        try {
            cone = new EllipticCone(origin, direction, inputUvector, angleU, angleV, height);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }

        // tests with wrong u vector
        direction = new Vector3D(2.0, 0.0, 0.0);
        inputUvector = new Vector3D(0.0, 0.0, 0.0);
        try {
            cone = new EllipticCone(origin, direction, inputUvector, angleU, angleV, height);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
        inputUvector = new Vector3D(3.0, 0.0, 0.0);
        try {
            cone = new EllipticCone(origin, direction, inputUvector, angleU, angleV, height);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }

        // test with wrong height
        inputUvector = new Vector3D(0.0, 1.0, 0.0);
        height = 0.0;
        try {
            cone = new EllipticCone(origin, direction, inputUvector, angleU, angleV, height);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
        height = -1.0;
        try {
            cone = new EllipticCone(origin, direction, inputUvector, angleU, angleV, height);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ELLIPTIC_CONE_SHAPE}
     * 
     * @testedMethod {@link EllipticCone#distanceTo(Line)}
     * 
     * @description Compute the shortest distance between the surface of the cone and a Line.
     * 
     * @input Lines of space
     * 
     * @output doubles : the distances
     * 
     * @testPassCriteria The output doubles must be the right distance, positive if the line does not intersect the
     *                   surface and zero otherwise with an epsilon of 1e-14 due to the computation errors.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testDistanceToLine() {

        // cone creation
        final Vector3D origin = new Vector3D(0.0, 1.0, 1.0);
        final Vector3D direction = new Vector3D(2.0, 0.0, 0.0);
        final Vector3D inputUvector = new Vector3D(1.0, 1.0, 0.0);
        final double angleU = FastMath.PI / 4.0;
        final double angleV = FastMath.PI / 8.0;
        final double height = 5.0;
        final EllipticCone cone = new EllipticCone(origin, direction, inputUvector, angleU, angleV,
            height);

        // test with a line closest to the finite cone's side
        Vector3D originLine = new Vector3D(1.0, 4.0, 0.0);
        Vector3D directionLine = new Vector3D(0.0, 0.0, 1.0);
        Line line = new Line(originLine, originLine.add(directionLine));
        double distance = cone.distanceTo(line);
        Assert.assertEquals(MathLib.sqrt(2.0), distance, this.comparisonEpsilon);

        // test with a line closest to the finite cone's apex
        originLine = new Vector3D(-2.0, 2.0, 0.0);
        directionLine = new Vector3D(0.0, 0.0, 1.0);
        line = new Line(originLine, originLine.add(directionLine));
        distance = cone.distanceTo(line);
        Assert.assertEquals(MathLib.sqrt(5.0), distance, this.comparisonEpsilon);

        // test with a line intersecting the infinite cone but not the finite cone
        originLine = new Vector3D(6.0, 2.0, 0.0);
        directionLine = new Vector3D(0.0, 0.0, 1.0);
        line = new Line(originLine, originLine.add(directionLine));
        distance = cone.distanceTo(line);
        Assert.assertEquals(1.0, distance, this.comparisonEpsilon);

        originLine = new Vector3D(5.0, 7.0, 1.0);
        directionLine = new Vector3D(1.0, -1.0, 0.0);
        line = new Line(originLine, originLine.add(directionLine));
        distance = cone.distanceTo(line);
        Assert.assertEquals(MathLib.sqrt(2.0) / 2.0, distance, this.comparisonEpsilon);

        // test with a line intersecting the finite cone
        originLine = new Vector3D(4.0, 2.0, 0.0);
        directionLine = new Vector3D(0.0, 0.0, 1.0);
        line = new Line(originLine, originLine.add(directionLine));
        distance = cone.distanceTo(line);
        Assert.assertEquals(0.0, distance, this.comparisonEpsilon);

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ELLIPTIC_CONE_SHAPE}
     * 
     * @testedMethod {@link EllipticCone#intersects(Line)}
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
        // cone creation
        final Vector3D origin = new Vector3D(0.0, 1.0, 1.0);
        final Vector3D direction = new Vector3D(2.0, 0.0, 0.0);
        final Vector3D inputUvector = new Vector3D(1.0, 1.0, 0.0);
        final double angleU = FastMath.PI / 4.0;
        final double angleV = FastMath.PI / 8.0;
        final double height = 5.0;
        final EllipticCone cone = new EllipticCone(origin, direction, inputUvector, angleU, angleV,
            height);

        // test with a line closest to the finite cone's side
        Vector3D originLine = new Vector3D(1.0, 4.0, 0.0);
        Vector3D directionLine = new Vector3D(0.0, 0.0, 1.0);
        Line line = new Line(originLine, originLine.add(directionLine));
        boolean intersects = cone.intersects(line);
        Assert.assertTrue(!intersects);

        // test with a line closest to the finite cone's apex,
        // intersecting the symmetric cone
        originLine = new Vector3D(-2.0, 2.0, 0.0);
        directionLine = new Vector3D(0.0, 0.0, 1.0);
        line = new Line(originLine, originLine.add(directionLine));
        intersects = cone.intersects(line);
        Assert.assertTrue(!intersects);

        // test with a line intersecting the infinite cone but not the finite cone
        originLine = new Vector3D(6.0, 2.0, 0.0);
        directionLine = new Vector3D(0.0, 0.0, 1.0);
        line = new Line(originLine, originLine.add(directionLine));
        intersects = cone.intersects(line);
        Assert.assertTrue(!intersects);

        originLine = new Vector3D(5.0, 1.0, -2.0);
        directionLine = new Vector3D(1.0, 0.0, 1.0);
        line = new Line(originLine, originLine.add(directionLine));
        intersects = cone.intersects(line);
        Assert.assertTrue(!intersects);

        // test with a line intersecting the finite cone
        originLine = new Vector3D(4.0, 2.0, 0.0);
        directionLine = new Vector3D(0.0, 0.0, 1.0);
        line = new Line(originLine, originLine.add(directionLine));
        intersects = cone.intersects(line);
        Assert.assertTrue(intersects);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ELLIPTIC_CONE_SHAPE}
     * 
     * @testedMethod {@link EllipticCone#getIntersectionPoints(Line)}
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

        // cone creation
        final Vector3D origin = new Vector3D(0.0, 1.0, 1.0);
        final Vector3D direction = new Vector3D(2.0, 0.0, 0.0);
        final Vector3D inputUvector = new Vector3D(1.0, 1.0, 0.0);
        final double angleU = FastMath.PI / 4.0;
        final double angleV = FastMath.PI / 8.0;
        final double height = 5.0;
        final EllipticCone cone = new EllipticCone(origin, direction, inputUvector, angleU, angleV,
            height);

        // test with a line closest to the finite cone's side :
        // no intersection
        Vector3D originLine = new Vector3D(1.0, 4.0, 0.0);
        Vector3D directionLine = new Vector3D(0.0, 0.0, 1.0);
        Line line = new Line(originLine, originLine.add(directionLine));
        Vector3D[] intersections = cone.getIntersectionPoints(line);
        Assert.assertEquals(0, intersections.length);

        // test with a line closest to the finite cone's apex,
        // intersecting the symmetric cone
        originLine = new Vector3D(-2.0, 2.0, 0.0);
        directionLine = new Vector3D(0.0, 0.0, 1.0);
        line = new Line(originLine, originLine.add(directionLine));
        intersections = cone.getIntersectionPoints(line);
        Assert.assertEquals(0, intersections.length);

        // test with a line intersecting the infinite cone but not the finite cone
        originLine = new Vector3D(6.0, 2.0, 0.0);
        directionLine = new Vector3D(0.0, 0.0, 1.0);
        line = new Line(originLine, originLine.add(directionLine));
        intersections = cone.getIntersectionPoints(line);
        Assert.assertEquals(0, intersections.length);

        originLine = new Vector3D(5.0, 7.0, 1.0);
        directionLine = new Vector3D(1.0, -1.0, 0.0);
        line = new Line(originLine, originLine.add(directionLine));
        intersections = cone.getIntersectionPoints(line);
        Assert.assertEquals(0, intersections.length);

        // test with a line intersecting twice the finite cone
        originLine = new Vector3D(2.0, 5.0, 1.0);
        directionLine = new Vector3D(0.0, 1.0, 0.0);
        line = new Line(originLine, originLine.add(directionLine));
        intersections = cone.getIntersectionPoints(line);
        Assert.assertEquals(2, intersections.length);

        Vector3D intersect1 = intersections[0];
        Vector3D intersect2 = intersections[1];

        Assert.assertEquals(2.0, intersect1.getX(), this.comparisonEpsilon);
        Assert.assertEquals(3.0, intersect1.getY(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, intersect1.getZ(), this.comparisonEpsilon);

        Assert.assertEquals(2.0, intersect2.getX(), this.comparisonEpsilon);
        Assert.assertEquals(-1.0, intersect2.getY(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, intersect2.getZ(), this.comparisonEpsilon);

        // test with a line intersecting once the finite cone
        // and the ending ellipse
        originLine = new Vector3D(2.0, 3.0, 1.0);
        directionLine = new Vector3D(1.0, 0.0, 0.0);
        line = new Line(originLine, originLine.add(directionLine));
        intersections = cone.getIntersectionPoints(line);
        Assert.assertEquals(2, intersections.length);

        intersect1 = intersections[0];
        intersect2 = intersections[1];

        Assert.assertEquals(2.0, intersect1.getX(), this.comparisonEpsilon);
        Assert.assertEquals(3.0, intersect1.getY(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, intersect1.getZ(), this.comparisonEpsilon);

        Assert.assertEquals(5.0, intersect2.getX(), this.comparisonEpsilon);
        Assert.assertEquals(3.0, intersect2.getY(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, intersect2.getZ(), this.comparisonEpsilon);

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ELLIPTIC_CONE_SHAPE}
     * 
     * @testedMethod {@link EllipticCone#closestPointTo(Line)}
     * 
     * @description Compute the point of the elliptic realizing the shortest distance to a line of space, and the
     *              associated point of the line.
     * 
     * @input Points of space (Vector3D)
     * 
     * @output Vector3D[]
     * 
     * @testPassCriteria The output vector must be the one of the elliptic shape and the one of the line realizing the
     *                   shortest distance with an epsilon of 1e-14 due to the computation errors.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testClosestPointToLine() {

        // cone creation
        final Vector3D origin = new Vector3D(0.0, 1.0, 1.0);
        final Vector3D direction = new Vector3D(2.0, 0.0, 0.0);
        final Vector3D inputUvector = new Vector3D(1.0, 1.0, 0.0);
        final double angleU = FastMath.PI / 4.0;
        final double angleV = FastMath.PI / 8.0;
        final double height = 3.0;
        final EllipticCone cone = new EllipticCone(origin, direction, inputUvector, angleU, angleV,
            height);

        // test with a line closest to the finite cone's side
        Vector3D originLine = new Vector3D(1.0, 3.0, 0.0);
        Vector3D directionLine = new Vector3D(0.0, 0.0, 1.0);
        Line line = new Line(originLine, originLine.add(directionLine));

        Vector3D[] closestPoints = cone.closestPointTo(line);

        Vector3D point1 = closestPoints[0];
        Vector3D point2 = closestPoints[1];

        Assert.assertEquals(1.0, point1.getX(), this.comparisonEpsilon);
        Assert.assertEquals(3.0, point1.getY(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point1.getZ(), this.comparisonEpsilon);
        Assert.assertEquals(1.5, point2.getX(), this.comparisonEpsilon);
        Assert.assertEquals(2.5, point2.getY(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point2.getZ(), this.comparisonEpsilon);

        // test with a line intersecting the cone
        originLine = new Vector3D(1.0, 3.0, 1.0);
        directionLine = new Vector3D(1.0, 0.0, 0.0);
        line = new Line(originLine, originLine.add(directionLine));

        closestPoints = cone.closestPointTo(line);

        point1 = closestPoints[0];
        point2 = closestPoints[1];

        Assert.assertEquals(2.0, point1.getX(), this.comparisonEpsilon);
        Assert.assertEquals(3.0, point1.getY(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point1.getZ(), this.comparisonEpsilon);
        Assert.assertEquals(2.0, point2.getX(), this.comparisonEpsilon);
        Assert.assertEquals(3.0, point2.getY(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point2.getZ(), this.comparisonEpsilon);

        // test with a line closest to the apex (origin)
        originLine = new Vector3D(-1.0, 2.0, 7.0);
        directionLine = new Vector3D(0.0, 0.0, 2.0);
        line = new Line(originLine, originLine.add(directionLine));

        closestPoints = cone.closestPointTo(line);

        point1 = closestPoints[0];
        point2 = closestPoints[1];

        Assert.assertEquals(-1.0, point1.getX(), this.comparisonEpsilon);
        Assert.assertEquals(2.0, point1.getY(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point1.getZ(), this.comparisonEpsilon);
        Assert.assertEquals(0.0, point2.getX(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point2.getY(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point2.getZ(), this.comparisonEpsilon);

        // test with a line closest to the ending ellipse
        originLine = new Vector3D(2.0, 6.0, 1.0);
        directionLine = new Vector3D(1.0, -1.0, 0.0);
        line = new Line(originLine, originLine.add(directionLine));

        closestPoints = cone.closestPointTo(line);

        point1 = closestPoints[0];
        point2 = closestPoints[1];

        Assert.assertEquals(3.5, point1.getX(), this.comparisonEpsilon);
        Assert.assertEquals(4.5, point1.getY(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point1.getZ(), this.comparisonEpsilon);
        Assert.assertEquals(3.0, point2.getX(), this.comparisonEpsilon);
        Assert.assertEquals(4.0, point2.getY(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point2.getZ(), this.comparisonEpsilon);

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ELLIPTIC_CONE_SHAPE}
     * 
     * @testedMethod {@link EllipticCone#toString()}
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

        // cone creation
        final Vector3D origin = new Vector3D(0.0, 1.0, 1.0);
        final Vector3D direction = new Vector3D(2.0, 0.0, 0.0);
        final Vector3D inputUvector = new Vector3D(1.0, 1.0, 0.0);
        final double angleU = FastMath.PI / 4.0;
        final double angleV = FastMath.PI / 8.0;
        final double height = 3.0;
        final EllipticCone cone = new EllipticCone(origin, direction, inputUvector, angleU, angleV, height);

        // string creation
        final String result = cone.toString();

        final String expected =
            "EllipticCone{Origin{0; 1; 1},Direction{1; 0; 0},U vector{0; 1; 0},Angle on U{0.7853981633974483},Angle on V{0.39269908169872414},Height{3.0}}";
        Assert.assertEquals(expected, result);
    }

}
