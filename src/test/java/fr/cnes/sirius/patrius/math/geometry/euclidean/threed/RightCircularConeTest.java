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

import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;

/**
 * @description <p>
 *              Validation tests for the object {@link RightCircularCone}.
 *              </p>
 * 
 * @author Thomas Trapier
 * 
 * @version $Id: RightCircularConeTest.java 17909 2017-09-11 11:57:36Z bignon $
 * 
 * @since 1.0
 * 
 */
public class RightCircularConeTest {

    /** Features description. */
    public enum features {

        /**
         * @featureTitle Right circular cone shape
         * 
         * @featureDescription Creation of an right circular cone shape, computation of distances and intersections with
         *                     lines and points.
         * 
         * @coveredRequirements DV-GEOMETRIE_50, DV-GEOMETRIE_60, DV-GEOMETRIE_90, DV-GEOMETRIE_120, DV-GEOMETRIE_130,
         *                      DV-GEOMETRIE_140
         */
        RIGHT_CIRCULAR_CONE_SHAPE
    }

    /** Epsilon for double comparison. */
    private final double comparisonEpsilon = Precision.DOUBLE_COMPARISON_EPSILON;

    /** Epsilon taking into account the machine error. */
    private final double machineEpsilon = Precision.EPSILON;

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#RIGHT_CIRCULAR_CONE_SHAPE}
     * 
     * @testedMethod {@link RightCircularCone#RightCircularCone(Vector3D, Vector3D, double, double)}
     * 
     * @description Instantiation of a right circular cone from its axis, height and angle.
     * 
     * @input A point origin, a vector direction, several doubles as angles and height (positive, negative, zero)
     * 
     * @output RightCircularCone
     * 
     * @testPassCriteria The cone can be created only if the angle and height are strictly positive, and the direction
     *                   of the axis not null. We check the returned elements with the ones given at the construction
     *                   with an epsilon of 1e-16 which takes into account the machine error only. An exception is
     *                   thrown otherwise.
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
        double angle = 0.84;
        double height = 5.0;

        RightCircularCone cone = new RightCircularCone(origin, direction, angle, height);

        final Vector3D originOut = cone.getOrigin();
        Assert.assertEquals(0.0, originOut.getX(), this.machineEpsilon);
        Assert.assertEquals(1.0, originOut.getY(), this.machineEpsilon);
        Assert.assertEquals(1.0, originOut.getZ(), this.machineEpsilon);

        final Vector3D directionOut = cone.getDirection();
        Assert.assertEquals(1.0, directionOut.getX(), this.machineEpsilon);
        Assert.assertEquals(0.0, directionOut.getY(), this.machineEpsilon);
        Assert.assertEquals(0.0, directionOut.getZ(), this.machineEpsilon);

        Assert.assertEquals(angle, cone.getAngle(), this.machineEpsilon);
        Assert.assertEquals(height, cone.getLength(), this.machineEpsilon);

        // test with wrong radiuses
        angle = 0.0;
        try {
            cone = new RightCircularCone(origin, direction, angle, height);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
        angle = -1.0;
        try {
            cone = new RightCircularCone(origin, direction, angle, height);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }

        // test with wrong direction
        angle = 0.84;
        direction = new Vector3D(0.0, 0.0, 0.0);
        try {
            cone = new RightCircularCone(origin, direction, angle, height);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }

        // test with wrong height
        direction = new Vector3D(2.0, 0.0, 0.0);
        height = 0.0;
        try {
            cone = new RightCircularCone(origin, direction, angle, height);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
        height = -1.0;
        try {
            cone = new RightCircularCone(origin, direction, angle, height);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#RIGHT_CIRCULAR_CONE_SHAPE}
     * 
     * @testedMethod {@link RightCircularCone#distanceTo(Line)}
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

        // test with corrects inputs
        final Vector3D origin = new Vector3D(0.0, 1.0, 1.0);
        final Vector3D direction = new Vector3D(2.0, 0.0, 0.0);
        final double angle = FastMath.PI / 4.0;
        final double height = 5.0;

        final RightCircularCone cone = new RightCircularCone(origin, direction, angle, height);

        // test with a line intersecting the finite cone
        Vector3D originLine = new Vector3D(2.0, 1.0, 0.0);
        Vector3D dirLine = new Vector3D(0.0, 0.0, 1.0);
        Line line = new Line(originLine, originLine.add(dirLine));
        double distance = cone.distanceTo(line);
        Assert.assertEquals(0.0, distance, this.comparisonEpsilon);

        // test with a line closest to the side
        originLine = new Vector3D(0.0, 3.0, 0.0);
        dirLine = new Vector3D(0.0, 0.0, 1.0);
        line = new Line(originLine, originLine.add(dirLine));
        distance = cone.distanceTo(line);
        Assert.assertEquals(MathLib.sqrt(2.0), distance, this.comparisonEpsilon);

        // test with a line closest to the apex
        originLine = new Vector3D(-2.0, 2.0, 0.0);
        dirLine = new Vector3D(0.0, 0.0, 1.0);
        line = new Line(originLine, originLine.add(dirLine));
        distance = cone.distanceTo(line);
        Assert.assertEquals(MathLib.sqrt(5.0), distance, this.comparisonEpsilon);

        // test with a line intersecting the infinite cone,
        // closest to the disk
        originLine = new Vector3D(1.0, 5.0, 1.0);
        dirLine = new Vector3D(2.0, 1.0, 0.0);
        line = new Line(originLine, originLine.add(dirLine));
        distance = cone.distanceTo(line);
        Assert.assertEquals(2.0 / MathLib.sqrt(5.0), distance, this.comparisonEpsilon);

        // test with a line parallel to the side
        originLine = new Vector3D(-1.0, 1.0, 1.0);
        dirLine = new Vector3D(1.0, 1.0, 0.0);
        line = new Line(originLine, originLine.add(dirLine));
        distance = cone.distanceTo(line);
        Assert.assertEquals(MathLib.sqrt(2.0) / 2.0, distance, this.comparisonEpsilon);

        // test with a line with no intersection with the infinite cone,
        // closest to the disk
        originLine = new Vector3D(6.0, 8.0, 1.0);
        dirLine = new Vector3D(0.0, 0.0, 1.0);
        line = new Line(originLine, originLine.add(dirLine));
        distance = cone.distanceTo(line);
        Assert.assertEquals(MathLib.sqrt(5.0), distance, this.comparisonEpsilon);

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#RIGHT_CIRCULAR_CONE_SHAPE}
     * 
     * @testedMethod {@link RightCircularCone#intersects(Line)}
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

        // test with corrects inputs
        final Vector3D origin = new Vector3D(0.0, 1.0, 1.0);
        final Vector3D direction = new Vector3D(2.0, 0.0, 0.0);
        final double angle = FastMath.PI / 4.0;
        final double height = 5.0;

        final RightCircularCone cone = new RightCircularCone(origin, direction, angle, height);

        // test with a line intersecting the finite cone
        Vector3D originLine = new Vector3D(2.0, 1.0, 0.0);
        Vector3D dirLine = new Vector3D(0.0, 0.0, 1.0);
        Line line = new Line(originLine, originLine.add(dirLine));
        boolean intersects = cone.intersects(line);
        Assert.assertTrue(intersects);

        // two tests with a line intersecting the infinite cone
        // but not the finite cone
        originLine = new Vector3D(6.0, 1.0, 0.0);
        line = new Line(originLine, originLine.add(dirLine));
        intersects = cone.intersects(line);
        Assert.assertTrue(!intersects);

        originLine = new Vector3D(0.0, 6.1, 1.0);
        dirLine = new Vector3D(1.0, 0.0, 0.0);
        line = new Line(originLine, originLine.add(dirLine));
        intersects = cone.intersects(line);
        Assert.assertTrue(!intersects);

        // test with no intersection with the infinite cone
        originLine = new Vector3D(0.0, 2.0, 1.0);
        dirLine = new Vector3D(0.0, 1.0, 1.0);
        line = new Line(originLine, originLine.add(dirLine));
        intersects = cone.intersects(line);
        Assert.assertTrue(!intersects);

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#RIGHT_CIRCULAR_CONE_SHAPE}
     * 
     * @testedMethod {@link RightCircularCone#getIntersectionPoints(Line)}
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

        // test with corrects inputs
        final Vector3D origin = new Vector3D(0.0, 1.0, 1.0);
        final Vector3D direction = new Vector3D(2.0, 0.0, 0.0);
        final double angle = FastMath.PI / 4.0;
        final double height = 5.0;

        final RightCircularCone cone = new RightCircularCone(origin, direction, angle, height);

        // test with a line intersecting only the finite cone
        Vector3D originLine = new Vector3D(2.0, 1.0, 0.0);
        Vector3D dirLine = new Vector3D(0.0, 0.0, 1.0);
        Line line = new Line(originLine, originLine.add(dirLine));
        Vector3D[] intersections = cone.getIntersectionPoints(line);
        Assert.assertEquals(2, intersections.length);
        Vector3D intersect1 = intersections[0];
        Vector3D intersect2 = intersections[1];

        Assert.assertEquals(2.0, intersect1.getX(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, intersect1.getY(), this.comparisonEpsilon);
        Assert.assertEquals(3.0, intersect1.getZ(), this.comparisonEpsilon);

        Assert.assertEquals(2.0, intersect2.getX(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, intersect2.getY(), this.comparisonEpsilon);
        Assert.assertEquals(-1.0, intersect2.getZ(), this.comparisonEpsilon);

        // test with a line intersecting the infinite cone
        // but not the finite cone
        originLine = new Vector3D(6.0, 1.0, 0.0);
        line = new Line(originLine, originLine.add(dirLine));
        intersections = cone.getIntersectionPoints(line);
        Assert.assertEquals(0, intersections.length);

        // test with a line intersecting the finite cone and the disk
        originLine = new Vector3D(0.0, 5.0, 1.0);
        dirLine = new Vector3D(1.0, 0.0, 0.0);
        line = new Line(originLine, originLine.add(dirLine));
        intersections = cone.getIntersectionPoints(line);
        Assert.assertEquals(2, intersections.length);
        intersect1 = intersections[0];
        intersect2 = intersections[1];

        Assert.assertEquals(4.0, intersect1.getX(), this.comparisonEpsilon);
        Assert.assertEquals(5.0, intersect1.getY(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, intersect1.getZ(), this.comparisonEpsilon);

        Assert.assertEquals(5.0, intersect2.getX(), this.comparisonEpsilon);
        Assert.assertEquals(5.0, intersect2.getY(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, intersect2.getZ(), this.comparisonEpsilon);

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#RIGHT_CIRCULAR_CONE_SHAPE}
     * 
     * @testedMethod {@link RightCircularCone#closestPointTo(Line)}
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

        // test with corrects inputs
        final Vector3D origin = new Vector3D(1.0, 1.0, 1.0);
        final Vector3D direction = new Vector3D(2.0, 0.0, 0.0);
        final double angle = FastMath.PI / 4.0;
        final double height = 4.0;

        final RightCircularCone cone = new RightCircularCone(origin, direction, angle, height);

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
        Assert.assertEquals(1.0, point1.getY(), this.comparisonEpsilon);
        Assert.assertEquals(3.0, point1.getZ(), this.comparisonEpsilon);
        Assert.assertEquals(2.0, point2.getX(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point2.getY(), this.comparisonEpsilon);
        Assert.assertEquals(2.0, point2.getZ(), this.comparisonEpsilon);

        // test with a non intersecting line closest to the ending disk
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
     * @testedFeature {@link features#RIGHT_CIRCULAR_CONE_SHAPE}
     * 
     * @testedMethod {@link RightCircularCone#toString()}
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
        final Vector3D origin = new Vector3D(1.0, 1.0, 1.0);
        final Vector3D direction = new Vector3D(2.0, 0.0, 0.0);
        final double angle = FastMath.PI / 4.0;
        final double height = 4.0;

        final RightCircularCone cone = new RightCircularCone(origin, direction, angle, height);

        // string creation
        final String result = cone.toString();

        final String expected =
            "RightCircularCone{Origin{1; 1; 1},Direction{1; 0; 0},Angle{0.7853981633974483},Height{4.0}}";
        Assert.assertEquals(expected, result);
    }

}
