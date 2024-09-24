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
 *              Validation tests for the object InfiniteRightCircularCylinder.
 *              </p>
 * 
 * @author Thomas Trapier
 * 
 * @version $Id: InfiniteRightCircularCylinderTest.java 17909 2017-09-11 11:57:36Z bignon $
 * 
 * @since 1.0
 * 
 */
public class InfiniteRightCircularCylinderTest {

    /** Features description. */
    public enum features {

        /**
         * @featureTitle Infinite right circular cylinder shape
         * 
         * @featureDescription Creation of an infinite right circular cylinder shape, computation of distances and
         *                     intersections with lines and points.
         * 
         * @coveredRequirements DV-GEOMETRIE_40, DV-GEOMETRIE_50, DV-GEOMETRIE_60, DV-GEOMETRIE_90, DV-GEOMETRIE_120,
         *                      DV-GEOMETRIE_130, DV-GEOMETRIE_140
         */
        INFINITE_RIGHT_CIRCULAR_CYLINDER_SHAPE
    }

    /** Epsilon for double comparison. */
    private final double comparisonEpsilon = Precision.DOUBLE_COMPARISON_EPSILON;

    /** Epsilon taking into account the machine error. */
    private final double machineEpsilon = Precision.EPSILON;

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#INFINITE_RIGHT_CIRCULAR_CYLINDER_SHAPE}
     * 
     * @testedMethod {@link InfiniteRightCircularCylinder#InfiniteRightCircularCylinder(Vector3D, Vector3D, double)}
     * 
     * @description Instantiation of a an infinite right circular cylinder from its axis and radius.
     * 
     * @input A point origin, a vector direction, several doubles as radiuses (positive, negative, zero)
     * 
     * @output InfiniteRightCircularCylinder
     * 
     * @testPassCriteria The cylinder can be created only if the radius is strictly positive, and the direction of the
     *                   axis not null (with an epsilon of 1e-16 due to the machine errors only : we check that the
     *                   elements are indeed the ones given at the construction). An exception is thrown otherwise.
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
        InfiniteRightCircularCylinder cylinder1 = new InfiniteRightCircularCylinder(origin, direction, radius);

        final Line axis = new Line(origin, origin.add(direction));
        final InfiniteRightCircularCylinder cylinder2 = new InfiniteRightCircularCylinder(axis, radius);

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

        // test with wrong radiuses
        radius = 0.0;
        try {
            cylinder1 = new InfiniteRightCircularCylinder(origin, direction, radius);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
        radius = -1.0;
        try {
            cylinder1 = new InfiniteRightCircularCylinder(origin, direction, radius);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }

        // test with wrong direction
        radius = 1.0;
        direction = new Vector3D(0.0, 0.0, 0.0);
        try {
            cylinder1 = new InfiniteRightCircularCylinder(origin, direction, radius);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#INFINITE_RIGHT_CIRCULAR_CYLINDER_SHAPE}
     * 
     * @testedMethod {@link InfiniteRightCircularCylinder#distanceTo(Line)}
     * 
     * @description Compute the shortest distance between the surface of the cylinder and a Line.
     * 
     * @input Lines of space
     * 
     * @output doubles : the distances
     * 
     * @testPassCriteria The output doubles must be the right distance with an epsilon of 1e-14 due to the computation
     *                   errors, positive if the line does not intersect the surface and is out of the cylinder,
     *                   negative if the line does not intersect the surface and is inside the cylinder zero otherwise.
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
        final InfiniteRightCircularCylinder cylinder = new InfiniteRightCircularCylinder(origin, direction, radius);

        // test with an intersecting line
        Vector3D originLine = new Vector3D(2.0, 1.0, 0.0);
        Vector3D dirLine = new Vector3D(0.0, 0.0, 1.0);
        Line line = new Line(originLine, originLine.add(dirLine));

        double distance = cylinder.distanceTo(line);
        Assert.assertEquals(0.0, distance, this.comparisonEpsilon);

        // test with a tangent line
        originLine = new Vector3D(0.0, 3.0, 0.0);
        line = new Line(originLine, originLine.add(dirLine));
        distance = cylinder.distanceTo(line);
        Assert.assertEquals(0.0, distance, this.comparisonEpsilon);

        // test with a non intersecting line
        originLine = new Vector3D(0.0, 6.0, 0.0);
        line = new Line(originLine, originLine.add(dirLine));
        distance = cylinder.distanceTo(line);
        Assert.assertEquals(3.0, distance, this.comparisonEpsilon);

        // test with a parallel line out of the cylinder
        originLine = new Vector3D(0.0, 6.0, 1.0);
        dirLine = new Vector3D(1.0, 0.0, 0.0);
        line = new Line(originLine, originLine.add(dirLine));
        distance = cylinder.distanceTo(line);
        Assert.assertEquals(3.0, distance, this.comparisonEpsilon);

        // test with a parallel line in the cylinder
        originLine = new Vector3D(0.0, 2.0, 1.0);
        line = new Line(originLine, originLine.add(dirLine));
        distance = cylinder.distanceTo(line);
        Assert.assertEquals(-1.0, distance, this.comparisonEpsilon);

        // test with a parallel line on the cylinder
        originLine = new Vector3D(0.0, 3.0, 1.0);
        line = new Line(originLine, originLine.add(dirLine));
        distance = cylinder.distanceTo(line);
        Assert.assertEquals(0.0, distance, this.comparisonEpsilon);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#INFINITE_RIGHT_CIRCULAR_CYLINDER_SHAPE}
     * 
     * @testedMethod {@link InfiniteRightCircularCylinder#intersects(Line)}
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
        final InfiniteRightCircularCylinder cylinder = new InfiniteRightCircularCylinder(origin, direction, radius);

        // test with an intersecting line
        Vector3D originLine = new Vector3D(2.0, 1.0, 0.0);
        Vector3D dirLine = new Vector3D(0.0, 0.0, 1.0);
        Line line = new Line(originLine, originLine.add(dirLine));

        boolean intersects = cylinder.intersects(line);
        Assert.assertTrue(intersects);

        // test with a non intersecting line
        originLine = new Vector3D(0.0, 6.0, 0.0);
        line = new Line(originLine, originLine.add(dirLine));

        intersects = cylinder.intersects(line);
        Assert.assertTrue(!intersects);

        // test with a parallel line out of the cylinder
        originLine = new Vector3D(0.0, 6.0, 1.0);
        dirLine = new Vector3D(1.0, 0.0, 0.0);
        line = new Line(originLine, originLine.add(dirLine));

        intersects = cylinder.intersects(line);
        Assert.assertTrue(!intersects);

        // test with a parallel line in the cylinder
        originLine = new Vector3D(0.0, 2.0, 1.0);
        line = new Line(originLine, originLine.add(dirLine));

        intersects = cylinder.intersects(line);
        Assert.assertTrue(!intersects);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#INFINITE_RIGHT_CIRCULAR_CYLINDER_SHAPE}
     * 
     * @testedMethod {@link InfiniteRightCircularCylinder#getIntersectionPoints(Line)}
     * 
     * @description Compute the intersection points with a line.
     * 
     * @input Lines of space
     * 
     * @output Vector3D
     * 
     * @testPassCriteria The result array is empty if no intersection point. The points have the expected coordinates
     *                   otherwise with an epsilon of 1e-14 due to the computation errors.
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
        final InfiniteRightCircularCylinder cylinder = new InfiniteRightCircularCylinder(origin, direction, radius);

        // test with a non intersecting line
        Vector3D originLine = new Vector3D(0.0, 6.0, 0.0);
        Vector3D dirLine = new Vector3D(0.0, 0.0, 1.0);
        Line line = new Line(originLine, originLine.add(dirLine));

        Vector3D[] intersections = cylinder.getIntersectionPoints(line);
        Assert.assertEquals(0, intersections.length);

        // test with an intersecting line
        originLine = new Vector3D(0.0, 1.0, 1.0);
        dirLine = new Vector3D(0.0, 1.0, 1.0);
        line = new Line(originLine, originLine.add(dirLine));

        intersections = cylinder.getIntersectionPoints(line);
        Assert.assertEquals(2, intersections.length);

        final Vector3D intersect1 = intersections[0];
        final Vector3D intersect2 = intersections[1];

        Assert.assertEquals(0.0, intersect1.getX(), this.comparisonEpsilon);
        Assert.assertEquals(-radius / MathLib.sqrt(2.0) + 1, intersect1.getY(), this.comparisonEpsilon);
        Assert.assertEquals(-radius / MathLib.sqrt(2.0) + 1, intersect1.getZ(), this.comparisonEpsilon);

        Assert.assertEquals(0.0, intersect2.getX(), this.comparisonEpsilon);
        Assert.assertEquals(radius / MathLib.sqrt(2.0) + 1, intersect2.getY(), this.comparisonEpsilon);
        Assert.assertEquals(radius / MathLib.sqrt(2.0) + 1, intersect2.getZ(), this.comparisonEpsilon);

        // test with a tangent line
        originLine = new Vector3D(0.0, 3.0, 0.0);
        dirLine = new Vector3D(0.0, 0.0, 1.0);
        line = new Line(originLine, originLine.add(dirLine));

        intersections = cylinder.getIntersectionPoints(line);
        Assert.assertEquals(1, intersections.length);

        final Vector3D intersect = intersections[0];
        Assert.assertEquals(0.0, intersect.getX(), this.comparisonEpsilon);
        Assert.assertEquals(3.0, intersect.getY(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, intersect.getZ(), this.comparisonEpsilon);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#INFINITE_RIGHT_CIRCULAR_CYLINDER_SHAPE}
     * 
     * @testedMethod {@link InfiniteRightCircularCylinder#closestPointTo(Line)}
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
        final InfiniteRightCircularCylinder cylinder = new InfiniteRightCircularCylinder(origin, direction, radius);

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

        // test with a parallel line identical to the axis
        originLine = new Vector3D(5.0, 1.0, 1.0);
        dirLine = new Vector3D(1.0, 0.0, 0.0);
        line = new Line(originLine, originLine.add(dirLine));

        closestPoints = cylinder.closestPointTo(line);

        point1 = closestPoints[0];
        point2 = closestPoints[1];

        Assert.assertEquals(0.0, point1.getX(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point1.getY(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point1.getZ(), this.comparisonEpsilon);
        Assert.assertEquals(0.0, point2.getX(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point2.getY(), this.comparisonEpsilon);
        Assert.assertEquals(3.0, point2.getZ(), this.comparisonEpsilon);

        // test with a parallel line inside the cylinder
        originLine = new Vector3D(5.0, 1.0, 2.0);
        dirLine = new Vector3D(1.0, 0.0, 0.0);
        line = new Line(originLine, originLine.add(dirLine));

        closestPoints = cylinder.closestPointTo(line);

        point1 = closestPoints[0];
        point2 = closestPoints[1];

        Assert.assertEquals(0.0, point1.getX(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point1.getY(), this.comparisonEpsilon);
        Assert.assertEquals(2.0, point1.getZ(), this.comparisonEpsilon);
        Assert.assertEquals(0.0, point2.getX(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point2.getY(), this.comparisonEpsilon);
        Assert.assertEquals(3.0, point2.getZ(), this.comparisonEpsilon);

        // test with a parallel line out of the cylinder
        originLine = new Vector3D(5.0, 1.0, 5.0);
        dirLine = new Vector3D(1.0, 0.0, 0.0);
        line = new Line(originLine, originLine.add(dirLine));

        closestPoints = cylinder.closestPointTo(line);

        point1 = closestPoints[0];
        point2 = closestPoints[1];

        Assert.assertEquals(0.0, point1.getX(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point1.getY(), this.comparisonEpsilon);
        Assert.assertEquals(5.0, point1.getZ(), this.comparisonEpsilon);
        Assert.assertEquals(0.0, point2.getX(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point2.getY(), this.comparisonEpsilon);
        Assert.assertEquals(3.0, point2.getZ(), this.comparisonEpsilon);

        // test with a non parallel line out of the cylinder
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
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#INFINITE_RIGHT_CIRCULAR_CYLINDER_SHAPE}
     * 
     * @testedMethod {@link InfiniteRightCircularCylinder#toString()}
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
        final InfiniteRightCircularCylinder cylinder = new InfiniteRightCircularCylinder(origin, direction, radius);

        // string creation
        final String result = cylinder.toString();

        final String expected = "InfiniteRightCircularCylinder{Origin{0; 1; 1},Direction{1; 0; 0},Radius{2.0}}";
        Assert.assertEquals(expected, result);
    }
}
