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
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.math.util.Precision;

/**
 * @description <p>
 *              Validation tests for the object InfiniterectangleCone.
 *              </p>
 * 
 * @author Thomas Trapier
 * 
 * @version $Id: InfiniteRectangleConeTest.java 17909 2017-09-11 11:57:36Z bignon $
 * 
 * @since 1.0
 * 
 */
public class InfiniteRectangleConeTest {

    /** Features description. */
    public enum features {

        /**
         * @featureTitle Infinite rectangle cone shape
         * 
         * @featureDescription Creation of an infinite rectangle cone shape, computation of distances and intersections
         *                     with lines and points.
         * 
         * @coveredRequirements DV-GEOMETRIE_40, DV-GEOMETRIE_50, DV-GEOMETRIE_60, DV-GEOMETRIE_90, DV-GEOMETRIE_120,
         *                      DV-GEOMETRIE_130, DV-GEOMETRIE_140
         */
        INFINITE_RECTANGLE_CONE_SHAPE
    }

    /** Epsilon for double comparison. */
    private final double comparisonEpsilon = Precision.DOUBLE_COMPARISON_EPSILON;

    /** Epsilon taking into account the machine error. */
    private final double machineEpsilon = Precision.EPSILON;

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#INFINITE_RECTANGLE_CONE_SHAPE}
     * 
     * @testedMethod {@link InfiniteRectangleCone#InfiniteRectangleCone(Vector3D, Vector3D, Vector3D, double, double)}
     * 
     * @description Instantiation of a an infinite rectangle cone from its origin, direction and angles.
     * 
     * @input A point origin, a vector axis, a vector U, several doubles as angles (correct, negative, greater than
     *        PI/2)
     * 
     * @output InfiniteRectangleCone
     * 
     * @testPassCriteria The cone can be created only if the angle is between 0.0 and PI, and the axis not null (with an
     *                   epsilon of 1e-16 due to the machine errors only : we check that the elements are indeed the
     *                   ones given at the construction). An exception is thrown otherwise.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public final void testConstructor() {

        // creation of the cone
        final Vector3D originCone = new Vector3D(1.0, 1.0, 1.0);
        Vector3D axis = new Vector3D(2.0, 0.0, 0.0);
        Vector3D inputVectorU = new Vector3D(1.0, 1.0, 0.0);
        double angleU = FastMath.PI / 4.0;
        double angleV = FastMath.PI / 6.0;

        InfiniteRectangleCone cone = new InfiniteRectangleCone(originCone, axis, inputVectorU, angleU, angleV);

        final Vector3D realOrigin = cone.getOrigin();
        Assert.assertEquals(originCone.getX(), realOrigin.getX(), this.machineEpsilon);
        Assert.assertEquals(originCone.getY(), realOrigin.getY(), this.machineEpsilon);
        Assert.assertEquals(originCone.getZ(), realOrigin.getZ(), this.machineEpsilon);

        final Vector3D realAxis = cone.getAxis();
        Assert.assertEquals(1.0, realAxis.getX(), this.machineEpsilon);
        Assert.assertEquals(0.0, realAxis.getY(), this.machineEpsilon);
        Assert.assertEquals(0.0, realAxis.getZ(), this.machineEpsilon);

        final Vector3D realU = cone.getU();
        Assert.assertEquals(0.0, realU.getX(), this.machineEpsilon);
        Assert.assertEquals(1.0, realU.getY(), this.machineEpsilon);
        Assert.assertEquals(0.0, realU.getZ(), this.machineEpsilon);

        final Vector3D realV = cone.getV();
        Assert.assertEquals(0.0, realV.getX(), this.machineEpsilon);
        Assert.assertEquals(0.0, realV.getY(), this.machineEpsilon);
        Assert.assertEquals(1.0, realV.getZ(), this.machineEpsilon);

        Assert.assertEquals(angleU, cone.getAngleU(), this.machineEpsilon);
        Assert.assertEquals(angleV, cone.getAngleV(), this.machineEpsilon);

        // test with wrong angles
        try {
            angleU = -0.1;
            cone = new InfiniteRectangleCone(originCone, axis, inputVectorU, angleU, angleV);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
        try {
            angleU = MathUtils.HALF_PI + 0.1;
            cone = new InfiniteRectangleCone(originCone, axis, inputVectorU, angleU, angleV);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
        try {
            angleU = 0.64574;
            angleV = -0.1;
            cone = new InfiniteRectangleCone(originCone, axis, inputVectorU, angleU, angleV);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
        try {
            angleV = MathUtils.HALF_PI + 0.1;
            cone = new InfiniteRectangleCone(originCone, axis, inputVectorU, angleU, angleV);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }

        // test with wrong axis
        try {
            axis = new Vector3D(0.0, 0.0, 0.0);
            angleU = 0.64574;
            cone = new InfiniteRectangleCone(originCone, axis, inputVectorU, angleU, angleV);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
        // test with wrong U vector
        try {
            axis = new Vector3D(2.0, 0.0, 0.0);
            inputVectorU = new Vector3D(0.0, 0.0, 0.0);
            cone = new InfiniteRectangleCone(originCone, axis, inputVectorU, angleU, angleV);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
        try {
            inputVectorU = new Vector3D(1.0, 0.0, 0.0);
            cone = new InfiniteRectangleCone(originCone, axis, inputVectorU, angleU, angleV);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#INFINITE_RECTANGLE_CONE_SHAPE}
     * 
     * @testedMethod {@link InfiniteRectangleCone#distanceTo(Line)}
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
    public final void testDistanceToLine() {

        // creation of the cone
        final Vector3D originCone = new Vector3D(1.0, 1.0, 1.0);
        final Vector3D axis = new Vector3D(2.0, 0.0, 0.0);
        final Vector3D inputVectorU = new Vector3D(0.0, 1.0, 0.0);
        final double angleU = FastMath.PI / 4.0;
        final double angleV = FastMath.PI / 4.0;

        final InfiniteRectangleCone cone = new InfiniteRectangleCone(originCone, axis, inputVectorU, angleU, angleV);

        // test with an intersecting line
        Vector3D originLine = new Vector3D(2.0, 1.0, 0.0);
        Vector3D dirLine = new Vector3D(0.0, 0.0, 1.0);
        Line line = new Line(originLine, originLine.add(dirLine));

        double distance = cone.distanceTo(line);
        Assert.assertEquals(0.0, distance, this.comparisonEpsilon);

        // test with a line closest to the cone's origin
        originLine = new Vector3D(0.0, 1.0, 0.0);
        dirLine = new Vector3D(0.0, 0.0, 1.0);
        line = new Line(originLine, originLine.add(dirLine));

        distance = cone.distanceTo(line);
        Assert.assertEquals(1.0, distance, this.comparisonEpsilon);

        // test with a non intersecting line parallel to the cone's side
        originLine = new Vector3D(0.0, 1.0, 1.0);
        dirLine = new Vector3D(1.0, 0.0, 1.0);
        line = new Line(originLine, originLine.add(dirLine));

        distance = cone.distanceTo(line);
        Assert.assertEquals(MathLib.sqrt(2.0) / 2.0, distance, this.comparisonEpsilon);

        // test with a line closest to a side axis
        originLine = new Vector3D(1.0, 2.0, 0.0);
        dirLine = new Vector3D(0.0, 0.0, 1.0);
        line = new Line(originLine, originLine.add(dirLine));

        distance = cone.distanceTo(line);
        Assert.assertEquals(MathLib.sqrt(2.0) / 2.0, distance, this.comparisonEpsilon);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#INFINITE_RECTANGLE_CONE_SHAPE}
     * 
     * @testedMethod {@link InfiniteRectangleCone#intersects(Line)}
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
    public final void testIntersectsLine() {
        // creation of the cone
        final Vector3D originCone = new Vector3D(1.0, 1.0, 1.0);
        final Vector3D axis = new Vector3D(2.0, 0.0, 0.0);
        final Vector3D inputVectorU = new Vector3D(0.0, 1.0, 0.0);
        final double angleU = FastMath.PI / 4.0;
        final double angleV = FastMath.PI / 4.0;

        final InfiniteRectangleCone cone = new InfiniteRectangleCone(originCone, axis, inputVectorU, angleU, angleV);

        // test with an intersecting line
        Vector3D originLine = new Vector3D(2.0, 1.0, 0.0);
        Vector3D dirLine = new Vector3D(0.0, 0.0, 1.0);
        Line line = new Line(originLine, originLine.add(dirLine));
        boolean intersects = cone.intersects(line);
        Assert.assertTrue(intersects);

        // test with a non intersecting line
        originLine = new Vector3D(0.0, 1.0, 1.0);
        dirLine = new Vector3D(0.0, 0.0, 1.0);
        line = new Line(originLine, originLine.add(dirLine));
        intersects = cone.intersects(line);
        Assert.assertTrue(!intersects);

        // test with a non intersecting line parallel to the cone's side
        originLine = new Vector3D(0.0, 1.0, 1.0);
        dirLine = new Vector3D(1.0, 0.0, 1.0);
        line = new Line(originLine, originLine.add(dirLine));
        intersects = cone.intersects(line);
        Assert.assertTrue(!intersects);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#INFINITE_RECTANGLE_CONE_SHAPE}
     * 
     * @testedMethod {@link InfiniteRectangleCone#getIntersectionPoints(Line)}
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
    public final void testIntersectionsLine() {

        // creation of the cone
        Vector3D originCone = new Vector3D(1.0, 1.0, 1.0);
        Vector3D axis = new Vector3D(2.0, 0.0, 0.0);
        Vector3D inputVectorU = new Vector3D(0.0, 1.0, 0.0);
        double angleU = FastMath.PI / 4.0;
        double angleV = FastMath.PI / 4.0;

        InfiniteRectangleCone cone = new InfiniteRectangleCone(originCone, axis, inputVectorU, angleU, angleV);

        // test with a twice intersecting line
        Vector3D originLine = new Vector3D(2.0, 1.0, 0.0);
        Vector3D dirLine = new Vector3D(0.0, 0.0, 1.0);
        Line line = new Line(originLine, originLine.add(dirLine));
        Vector3D[] intersections = cone.getIntersectionPoints(line);
        Assert.assertEquals(intersections.length, 2);

        Vector3D intersect1 = intersections[0];
        final Vector3D intersect2 = intersections[1];

        Assert.assertEquals(2.0, intersect1.getX(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, intersect1.getY(), this.comparisonEpsilon);
        Assert.assertEquals(2.0, intersect1.getZ(), this.comparisonEpsilon);

        Assert.assertEquals(2.0, intersect2.getX(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, intersect2.getY(), this.comparisonEpsilon);
        Assert.assertEquals(0.0, intersect2.getZ(), this.comparisonEpsilon);

        // test with a once intersecting line
        originLine = new Vector3D(1.0, 1.0, 2.0);
        dirLine = new Vector3D(1.0, 0.0, 0.0);
        line = new Line(originLine, originLine.add(dirLine));
        intersections = cone.getIntersectionPoints(line);
        Assert.assertEquals(intersections.length, 1);

        intersect1 = intersections[0];

        Assert.assertEquals(2.0, intersect1.getX(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, intersect1.getY(), this.comparisonEpsilon);
        Assert.assertEquals(2.0, intersect1.getZ(), this.comparisonEpsilon);

        // test with a line passing exactly through the origin
        originLine = new Vector3D(1.0, 1.0, 1.0);
        dirLine = new Vector3D(1.0, 0.0, 0.0);
        line = new Line(originLine, originLine.add(dirLine));
        intersections = cone.getIntersectionPoints(line);
        Assert.assertEquals(intersections.length, 1);

        intersect1 = intersections[0];

        Assert.assertEquals(1.0, intersect1.getX(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, intersect1.getY(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, intersect1.getZ(), this.comparisonEpsilon);

        // test with a non intersecting line
        originLine = new Vector3D(0.0, 1.0, 1.0);
        dirLine = new Vector3D(0.0, 0.0, 1.0);
        line = new Line(originLine, originLine.add(dirLine));
        intersections = cone.getIntersectionPoints(line);
        Assert.assertEquals(intersections.length, 0);

        // limit test case
        originCone = new Vector3D(-2, 0, 1);
        axis = new Vector3D(5, 2, 2);
        inputVectorU = new Vector3D(5, 0, 1);
        angleU = FastMath.PI / 4;
        angleV = FastMath.PI / 4;

        cone = new InfiniteRectangleCone(originCone, axis, inputVectorU, angleU, angleV);
        line = new Line(new Vector3D(5, -2, 8), new Vector3D(5, -2, 8).add(new Vector3D(-1, 6, 1)));

        intersections = cone.getIntersectionPoints(line);
        Assert.assertEquals(intersections.length, 2);

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#INFINITE_RECTANGLE_CONE_SHAPE}
     * 
     * @testedMethod {@link InfiniteRectangleCone#closestPointTo(Line)}
     * 
     * @description Compute the point of the cone realizing the shortest distance to a line of space, and the associated
     *              point of the line.
     * 
     * @input Points of space (Vector3D)
     * 
     * @output Vector3D[]
     * 
     * @testPassCriteria The output vector must be the one of the cone and the one of the line realizing the shortest
     *                   distance with an epsilon of 1e-14 due to the computation errors.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public final void testClosestPointToLine() {

        // creation of the cone
        final Vector3D originCone = new Vector3D(1.0, 1.0, 1.0);
        final Vector3D axis = new Vector3D(2.0, 0.0, 0.0);
        final Vector3D inputVectorU = new Vector3D(0.0, 1.0, 0.0);
        final double angleU = FastMath.PI / 4.0;
        final double angleV = FastMath.PI / 4.0;

        final InfiniteRectangleCone cone = new InfiniteRectangleCone(originCone, axis, inputVectorU, angleU, angleV);

        // test with an intersecting line
        Vector3D originLine = new Vector3D(2.0, 1.0, 0.0);
        Vector3D dirLine = new Vector3D(0.0, 0.0, 1.0);
        Line line = new Line(originLine, originLine.add(dirLine));

        Vector3D[] closestPoints = cone.closestPointTo(line);

        Vector3D point1 = closestPoints[0];
        Vector3D point2 = closestPoints[1];

        Assert.assertEquals(2.0, point1.getX(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point1.getY(), this.comparisonEpsilon);
        Assert.assertEquals(2.0, point1.getZ(), this.comparisonEpsilon);
        Assert.assertEquals(2.0, point2.getX(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point2.getY(), this.comparisonEpsilon);
        Assert.assertEquals(2.0, point2.getZ(), this.comparisonEpsilon);

        // test with a line closest to the cone's origin
        originLine = new Vector3D(0.0, 1.0, 0.0);
        dirLine = new Vector3D(0.0, 0.0, 1.0);
        line = new Line(originLine, originLine.add(dirLine));

        closestPoints = cone.closestPointTo(line);

        point1 = closestPoints[0];
        point2 = closestPoints[1];

        Assert.assertEquals(0.0, point1.getX(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point1.getY(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point1.getZ(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point2.getX(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point2.getY(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point2.getZ(), this.comparisonEpsilon);

        // test with a non intersecting line parallel to the cone's side
        originLine = new Vector3D(0.0, 1.0, 1.0);
        dirLine = new Vector3D(1.0, 0.0, 1.0);
        line = new Line(originLine, originLine.add(dirLine));

        closestPoints = cone.closestPointTo(line);

        point1 = closestPoints[0];
        point2 = closestPoints[1];

        Assert.assertEquals(0.5, point1.getX(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point1.getY(), this.comparisonEpsilon);
        Assert.assertEquals(1.5, point1.getZ(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point2.getX(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point2.getY(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point2.getZ(), this.comparisonEpsilon);

        // test with a line closest to a side axis
        originLine = new Vector3D(1.0, 2.0, 0.0);
        dirLine = new Vector3D(0.0, 0.0, 1.0);
        line = new Line(originLine, originLine.add(dirLine));

        closestPoints = cone.closestPointTo(line);

        point1 = closestPoints[0];
        point2 = closestPoints[1];

        Assert.assertEquals(1.0, point1.getX(), this.comparisonEpsilon);
        Assert.assertEquals(2.0, point1.getY(), this.comparisonEpsilon);
        Assert.assertEquals(0.5, point1.getZ(), this.comparisonEpsilon);
        Assert.assertEquals(1.5, point2.getX(), this.comparisonEpsilon);
        Assert.assertEquals(1.5, point2.getY(), this.comparisonEpsilon);
        Assert.assertEquals(0.5, point2.getZ(), this.comparisonEpsilon);

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#INFINITE_RECTANGLE_CONE_SHAPE}
     * 
     * @testedMethod {@link InfiniteRectangleCone#toString()}
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
        final Vector3D axis = new Vector3D(2.0, 0.0, 0.0);
        final Vector3D inputVectorU = new Vector3D(0.0, 1.0, 0.0);
        final double angleU = FastMath.PI / 4.0;
        final double angleV = FastMath.PI / 4.0;

        final InfiniteRectangleCone cone = new InfiniteRectangleCone(originCone, axis, inputVectorU, angleU, angleV);

        // string creation
        final String result = cone.toString();

        final String expected =
            "InfiniteRectangleCone{Origin{1; 1; 1},Direction{1; 0; 0},U vector{0; 1; 0},Angle on U{0.7853981633974483},Angle on V{0.7853981633974483}}";
        Assert.assertEquals(expected, result);
    }
}
