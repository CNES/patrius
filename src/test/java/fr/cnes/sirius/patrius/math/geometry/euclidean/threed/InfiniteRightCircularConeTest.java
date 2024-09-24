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
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.math.util.Precision;

/**
 * @description <p>
 *              Validation tests for the object InfiniteRightCircularCone.
 *              </p>
 * 
 * @author Thomas Trapier
 * 
 * @version $Id: InfiniteRightCircularConeTest.java 17909 2017-09-11 11:57:36Z bignon $
 * 
 * @since 1.0
 * 
 */
public class InfiniteRightCircularConeTest {

    /** Features description. */
    public enum features {

        /**
         * @featureTitle Infinite right circular cone shape
         * 
         * @featureDescription Creation of an infinite right circular cone shape, computation of distances and
         *                     intersections with lines and points.
         * 
         * @coveredRequirements DV-GEOMETRIE_40, DV-GEOMETRIE_50, DV-GEOMETRIE_60, DV-GEOMETRIE_90, DV-GEOMETRIE_120,
         *                      DV-GEOMETRIE_130, DV-GEOMETRIE_140
         */
        INFINITE_ERIGHT_CIRCULAR_CONE_SHAPE
    }

    /** Epsilon for double comparison. */
    private final double comparisonEpsilon = Precision.DOUBLE_COMPARISON_EPSILON;

    /** Epsilon taking into account the machine error. */
    private final double machineEpsilon = Precision.EPSILON;

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#INFINITE_ERIGHT_CIRCULAR_CONE_SHAPE}
     * 
     * @testedMethod {@link InfiniteRightCircularCone#InfiniteRightCircularCone(Vector3D, Vector3D, double)}
     * 
     * @description Instantiation of a an infinite right circular cone from its origin, axis and angle.
     * 
     * @input A point origin, a vector axis several doubles as angles (correct, negative, greater than PI/2)
     * 
     * @output InfiniteRightCircularCone
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
    public void testConstructor() {

        final Vector3D origin = new Vector3D(1.0, 1.0, 1.0);

        // test with corrects inputs
        Vector3D axis = new Vector3D(2.0, 0.0, 0.0);
        double angle = 0.87954;
        InfiniteRightCircularCone cone = new InfiniteRightCircularCone(origin, axis, angle);

        final Vector3D realOrigin = cone.getOrigin();
        Assert.assertEquals(origin.getX(), realOrigin.getX(), this.machineEpsilon);
        Assert.assertEquals(origin.getY(), realOrigin.getY(), this.machineEpsilon);
        Assert.assertEquals(origin.getZ(), realOrigin.getZ(), this.machineEpsilon);

        final Vector3D realAxis = cone.getAxis();
        Assert.assertEquals(1.0, realAxis.getX(), this.machineEpsilon);
        Assert.assertEquals(0.0, realAxis.getY(), this.machineEpsilon);
        Assert.assertEquals(0.0, realAxis.getZ(), this.machineEpsilon);

        Assert.assertEquals(angle, cone.getAngle(), this.machineEpsilon);

        // test with wrong angles
        try {
            angle = -0.1;
            cone = new InfiniteRightCircularCone(origin, axis, angle);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
        try {
            angle = MathUtils.HALF_PI + 0.1;
            cone = new InfiniteRightCircularCone(origin, axis, angle);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }

        // test with wrong axis
        try {
            axis = new Vector3D(0.0, 0.0, 0.0);
            angle = 0.64574;
            cone = new InfiniteRightCircularCone(origin, axis, angle);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#INFINITE_ERIGHT_CIRCULAR_CONE_SHAPE}
     * 
     * @testedMethod {@link InfiniteRightCircularCone#distanceTo(Line)}
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
        final Vector3D originCone = new Vector3D(1.0, 1.0, 1.0);
        final Vector3D axis = new Vector3D(2.0, 0.0, 0.0);
        final double angle = FastMath.PI / 4.0;
        final InfiniteRightCircularCone cone = new InfiniteRightCircularCone(originCone, axis, angle);

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

        // test with a line closest to the cone's side
        originLine = new Vector3D(1.0, 0.0, 1.0);
        dirLine = new Vector3D(0.0, 0.0, 1.0);
        line = new Line(originLine, originLine.add(dirLine));

        distance = cone.distanceTo(line);
        Assert.assertEquals(MathLib.sqrt(2.0) / 2.0, distance, this.comparisonEpsilon);

        // test with a line parallel to the cone's side
        originLine = new Vector3D(1.0, 0.0, 1.0);
        dirLine = new Vector3D(1.0, -1.0, 0.0);
        line = new Line(originLine, originLine.add(dirLine));

        distance = cone.distanceTo(line);
        Assert.assertEquals(MathLib.sqrt(2.0) / 2.0, distance, this.comparisonEpsilon);

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#INFINITE_ERIGHT_CIRCULAR_CONE_SHAPE}
     * 
     * @testedMethod {@link InfiniteRightCircularCone#intersects(Line)}
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
        final Vector3D originCone = new Vector3D(1.0, 1.0, 1.0);
        final Vector3D axis = new Vector3D(2.0, 0.0, 0.0);
        final double angle = FastMath.PI / 4.0;
        final InfiniteRightCircularCone cone = new InfiniteRightCircularCone(originCone, axis, angle);

        // test with an intersecting line
        Vector3D originLine = new Vector3D(2.0, 1.0, 0.0);
        Vector3D dirLine = new Vector3D(0.0, 0.0, 1.0);
        Line line = new Line(originLine, originLine.add(dirLine));
        boolean intersects = cone.intersects(line);
        Assert.assertTrue(intersects);

        // test with a non intersecting line
        originLine = new Vector3D(0.0, 1.0, 0.0);
        dirLine = new Vector3D(0.0, 0.0, 1.0);
        line = new Line(originLine, originLine.add(dirLine));
        intersects = cone.intersects(line);
        Assert.assertTrue(!intersects);

        // test with a non intersecting line parallel to the cone
        originLine = new Vector3D(0.0, 1.0, 1.0);
        dirLine = new Vector3D(1.0, 1.0, 0.0);
        line = new Line(originLine, originLine.add(dirLine));
        intersects = cone.intersects(line);
        Assert.assertTrue(!intersects);

        // test with an intersecting line parallel to the cone
        originLine = new Vector3D(-3.0, -4.0, 1.0);
        dirLine = new Vector3D(1.0, 1.0, 0.0);
        line = new Line(originLine, originLine.add(dirLine));
        intersects = cone.intersects(line);
        Assert.assertTrue(intersects);

        // two tests with an intersecting line parallel to the axis
        originLine = new Vector3D(-3.0, -4.0, 1.0);
        dirLine = new Vector3D(1.0, 0.0, 0.0);
        line = new Line(originLine, originLine.add(dirLine));
        intersects = cone.intersects(line);
        Assert.assertTrue(intersects);

        originLine = new Vector3D(-3.0, -4.0, 1.0);
        dirLine = new Vector3D(-1.0, 0.0, 0.0);
        line = new Line(originLine, originLine.add(dirLine));
        intersects = cone.intersects(line);
        Assert.assertTrue(intersects);

        // test with a tangent line
        originLine = new Vector3D(2.0, 2.0, 0.0);
        dirLine = new Vector3D(0.0, 0.0, 1.0);
        line = new Line(originLine, originLine.add(dirLine));
        intersects = cone.intersects(line);
        Assert.assertTrue(intersects);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#INFINITE_ERIGHT_CIRCULAR_CONE_SHAPE}
     * 
     * @testedMethod {@link InfiniteRightCircularCone#getIntersectionPoints(Line)}
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
        final Vector3D axis = new Vector3D(2.0, 0.0, 0.0);
        final double angle = FastMath.PI / 4.0;
        final InfiniteRightCircularCone cone = new InfiniteRightCircularCone(originCone, axis, angle);

        // test with an intersecting line
        Vector3D originLine = new Vector3D(2.0, 1.0, 0.0);
        Vector3D dirLine = new Vector3D(0.0, 0.0, 1.0);
        Line line = new Line(originLine, originLine.add(dirLine));
        Vector3D[] intersections = cone.getIntersectionPoints(line);
        Assert.assertEquals(2, intersections.length);
        final Vector3D intersect1 = intersections[0];
        final Vector3D intersect2 = intersections[1];

        Assert.assertEquals(2.0, intersect1.getX(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, intersect1.getY(), this.comparisonEpsilon);
        Assert.assertEquals(2.0, intersect1.getZ(), this.comparisonEpsilon);

        Assert.assertEquals(2.0, intersect2.getX(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, intersect2.getY(), this.comparisonEpsilon);
        Assert.assertEquals(0.0, intersect2.getZ(), this.comparisonEpsilon);

        // test with a non intersecting line
        originLine = new Vector3D(0.0, 1.0, 0.0);
        dirLine = new Vector3D(0.0, 0.0, 1.0);
        line = new Line(originLine, originLine.add(dirLine));
        intersections = cone.getIntersectionPoints(line);
        Assert.assertEquals(0, intersections.length);

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#INFINITE_ERIGHT_CIRCULAR_CONE_SHAPE}
     * 
     * @testedMethod {@link InfiniteRightCircularCone#closestPointTo(Line)}
     * 
     * @description Compute the point of the cone realizing the shortest distance to a line of space, and the associated
     *              point of the line.
     * 
     * @input Points of space (Vector3D)
     * 
     * @output Vector3D[]
     * 
     * @testPassCriteria The output vector must be the one of the shape and the one of the line realizing the shortest
     *                   distance with an epsilon of 1e-14 due to the computation errors..
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testClosestPointToLine() {

        // creation of the cone
        final Vector3D originCone = new Vector3D(1.0, 1.0, 1.0);
        final Vector3D axis = new Vector3D(2.0, 0.0, 0.0);
        final double angle = FastMath.PI / 4.0;
        final InfiniteRightCircularCone cone = new InfiniteRightCircularCone(originCone, axis, angle);

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

        // test with a non intersecting line
        // closest to the side
        originLine = new Vector3D(1.0, 1.0, 2.0);
        dirLine = new Vector3D(0.0, 1.0, 0.0);
        line = new Line(originLine, originLine.add(dirLine));

        closestPoints = cone.closestPointTo(line);

        point1 = closestPoints[0];
        point2 = closestPoints[1];

        Assert.assertEquals(1.0, point1.getX(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point1.getY(), this.comparisonEpsilon);
        Assert.assertEquals(2.0, point1.getZ(), this.comparisonEpsilon);
        Assert.assertEquals(1.5, point2.getX(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point2.getY(), this.comparisonEpsilon);
        Assert.assertEquals(1.5, point2.getZ(), this.comparisonEpsilon);

        // test with a non intersecting line
        // closest to the apex
        originLine = new Vector3D(-1.0, 1.0, 2.0);
        dirLine = new Vector3D(0.0, 1.0, 0.0);
        line = new Line(originLine, originLine.add(dirLine));

        closestPoints = cone.closestPointTo(line);

        point1 = closestPoints[0];
        point2 = closestPoints[1];

        Assert.assertEquals(-1.0, point1.getX(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point1.getY(), this.comparisonEpsilon);
        Assert.assertEquals(2.0, point1.getZ(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point2.getX(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point2.getY(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point2.getZ(), this.comparisonEpsilon);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#INFINITE_ERIGHT_CIRCULAR_CONE_SHAPE}
     * 
     * @testedMethod {@link InfiniteRightCircularCone#toString()}
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
        final double angle = FastMath.PI / 4.0;
        final InfiniteRightCircularCone cone = new InfiniteRightCircularCone(originCone, axis, angle);

        // string creation
        final String result = cone.toString();

        final String expected =
            "InfiniteRightCircularCone{Origin{1; 1; 1},Direction{1; 0; 0},Angle{0.7853981633974483}}";
        Assert.assertEquals(expected, result);
    }
}
