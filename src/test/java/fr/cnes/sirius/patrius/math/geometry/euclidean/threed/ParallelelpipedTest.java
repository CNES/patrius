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
 *              Validation tests for the object parallelepiped.
 *              </p>
 * 
 * @author Thomas Trapier
 * 
 * @version $Id: ParallelelpipedTest.java 17909 2017-09-11 11:57:36Z bignon $
 * 
 * @since 1.0
 * 
 */
public class ParallelelpipedTest {
    /** Features description. */
    public enum features {

        /**
         * @featureTitle Parallelepiped shape
         * 
         * @featureDescription Creation of a rectangle plate shape, computation of distances and intersections with
         *                     lines and points.
         * 
         * @coveredRequirements DV-GEOMETRIE_50, DV-GEOMETRIE_60, DV-GEOMETRIE_90, DV-GEOMETRIE_120, DV-GEOMETRIE_130,
         *                      DV-GEOMETRIE_140
         */
        PARALLELEPIPED_SHAPE
    }

    /** Epsilon for double comparison. */
    private final double comparisonEpsilon = Precision.DOUBLE_COMPARISON_EPSILON;

    /** Epsilon taking into account the machine error. */
    private final double machineEpsilon = Precision.EPSILON;

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#PARALLELEPIPED_SHAPE}
     * 
     * @testedMethod {@link Parallelepiped#Parallelepiped(Vector3D, Vector3D, Vector3D, double, double, double)}
     * 
     * @description Instantiation of a rectangle parallelepiped from its center, two vectors and three dimensions.
     * 
     * @input A point center, two vectors to describe its orientation and dimensions.
     * 
     * @output Parallelepiped
     * 
     * @testPassCriteria The shape can be created only if the two vectors are not parallel and the dimensions strictly
     *                   positive, an exception is thrown otherwise. We check the returned elements with the ones given
     *                   at the construction with an epsilon of 1e-16 which takes into account the machine error only.
     *                   We check the computed corners with the expected ones with an epsilon of 1e-14 due to the
     *                   computation errors.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testConstructor() {

        // Creation of a plate with no orthogonal input vectors and test
        // of the corrected Y vector of the local frame
        final Vector3D center = new Vector3D(1.0, 1.0, 1.0);
        final Vector3D uVector = new Vector3D(1.0, 1.0, 0.0);
        Vector3D inputvVector = new Vector3D(-5.0, 1.0, 0.0);

        Parallelepiped parallelepiped = new Parallelepiped(center, uVector, inputvVector, 4.0, 2.0, 6.0);

        final Vector3D realvVector = parallelepiped.getV();
        Assert.assertEquals(realvVector.getX(), -MathLib.sqrt(2.0) / 2.0, this.machineEpsilon);
        Assert.assertEquals(realvVector.getY(), MathLib.sqrt(2.0) / 2.0, this.machineEpsilon);
        Assert.assertEquals(realvVector.getZ(), 0.0, this.machineEpsilon);

        final Vector3D realuVector = parallelepiped.getU();
        Assert.assertEquals(realuVector.getX(), MathLib.sqrt(2.0) / 2.0, this.machineEpsilon);
        Assert.assertEquals(realuVector.getY(), MathLib.sqrt(2.0) / 2.0, this.machineEpsilon);
        Assert.assertEquals(realuVector.getZ(), 0.0, this.machineEpsilon);

        final Vector3D realwVector = parallelepiped.getW();
        Assert.assertEquals(realwVector.getX(), 0.0, this.machineEpsilon);
        Assert.assertEquals(realwVector.getY(), 0.0, this.machineEpsilon);
        Assert.assertEquals(realwVector.getZ(), 1.0, this.machineEpsilon);

        final Vector3D realCenter = parallelepiped.getCenter();
        Assert.assertEquals(realCenter.getX(), 1.0, this.machineEpsilon);
        Assert.assertEquals(realCenter.getY(), 1.0, this.machineEpsilon);
        Assert.assertEquals(realCenter.getZ(), 1.0, this.machineEpsilon);

        Assert.assertEquals(parallelepiped.getLength(), 4.0, this.machineEpsilon);
        Assert.assertEquals(parallelepiped.getWidth(), 2.0, this.machineEpsilon);
        Assert.assertEquals(parallelepiped.getHeight(), 6.0, this.machineEpsilon);

        // Test of the corner points coordinates
        final Vector3D[] corners = parallelepiped.getCorners();
        Vector3D corner = corners[0];
        Assert.assertEquals(corner.getX(), 1.0 + MathLib.sqrt(2.0) / 2.0, this.comparisonEpsilon);
        Assert.assertEquals(corner.getY(), 1.0 + 3.0 * MathLib.sqrt(2.0) / 2.0, this.comparisonEpsilon);
        Assert.assertEquals(corner.getZ(), 4.0, this.comparisonEpsilon);

        corner = corners[1];
        Assert.assertEquals(corner.getX(), 1.0 - 3.0 * MathLib.sqrt(2.0) / 2.0, this.comparisonEpsilon);
        Assert.assertEquals(corner.getY(), 1.0 - MathLib.sqrt(2.0) / 2.0, this.comparisonEpsilon);
        Assert.assertEquals(corner.getZ(), 4.0, this.comparisonEpsilon);

        corner = corners[2];
        Assert.assertEquals(corner.getX(), 1.0 - MathLib.sqrt(2.0) / 2.0, this.comparisonEpsilon);
        Assert.assertEquals(corner.getY(), 1.0 - 3.0 * MathLib.sqrt(2.0) / 2.0, this.comparisonEpsilon);
        Assert.assertEquals(corner.getZ(), 4.0, this.comparisonEpsilon);

        corner = corners[3];
        Assert.assertEquals(corner.getX(), 1.0 + 3.0 * MathLib.sqrt(2.0) / 2.0, this.comparisonEpsilon);
        Assert.assertEquals(corner.getY(), 1.0 + MathLib.sqrt(2.0) / 2.0, this.comparisonEpsilon);
        Assert.assertEquals(corner.getZ(), 4.0, this.comparisonEpsilon);

        corner = corners[4];
        Assert.assertEquals(corner.getX(), 1.0 + MathLib.sqrt(2.0) / 2.0, this.comparisonEpsilon);
        Assert.assertEquals(corner.getY(), 1.0 + 3.0 * MathLib.sqrt(2.0) / 2.0, this.comparisonEpsilon);
        Assert.assertEquals(corner.getZ(), -2.0, this.comparisonEpsilon);

        corner = corners[5];
        Assert.assertEquals(corner.getX(), 1.0 - 3.0 * MathLib.sqrt(2.0) / 2.0, this.comparisonEpsilon);
        Assert.assertEquals(corner.getY(), 1.0 - MathLib.sqrt(2.0) / 2.0, this.comparisonEpsilon);
        Assert.assertEquals(corner.getZ(), -2.0, this.comparisonEpsilon);

        corner = corners[6];
        Assert.assertEquals(corner.getX(), 1.0 - MathLib.sqrt(2.0) / 2.0, this.comparisonEpsilon);
        Assert.assertEquals(corner.getY(), 1.0 - 3.0 * MathLib.sqrt(2.0) / 2.0, this.comparisonEpsilon);
        Assert.assertEquals(corner.getZ(), -2.0, this.comparisonEpsilon);

        corner = corners[7];
        Assert.assertEquals(corner.getX(), 1.0 + 3.0 * MathLib.sqrt(2.0) / 2.0, this.comparisonEpsilon);
        Assert.assertEquals(corner.getY(), 1.0 + MathLib.sqrt(2.0) / 2.0, this.comparisonEpsilon);
        Assert.assertEquals(corner.getZ(), -2.0, this.comparisonEpsilon);

        final Plate[] faces = parallelepiped.getFaces();
        corner = faces[4].getC4();
        Assert.assertEquals(corner.getX(), 1.0 + 3.0 * MathLib.sqrt(2.0) / 2.0, this.comparisonEpsilon);
        Assert.assertEquals(corner.getY(), 1.0 + MathLib.sqrt(2.0) / 2.0, this.comparisonEpsilon);
        Assert.assertEquals(corner.getZ(), 4.0, this.comparisonEpsilon);

        // Test with parallel input vectors
        inputvVector = new Vector3D(1.8, 1.8, 0.0);
        try {
            parallelepiped = new Parallelepiped(center, uVector, inputvVector, 4.0, 2.0, 6.0);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected !
        }

        // Test with wrong dimensions
        inputvVector = new Vector3D(1.0, -1.0, 0.0);
        try {
            parallelepiped = new Parallelepiped(center, uVector, inputvVector, 0.0, 2.0, 6.0);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected !
        }
        try {
            parallelepiped = new Parallelepiped(center, uVector, inputvVector, 4.0, 0.0, 6.0);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected !
        }
        try {
            parallelepiped = new Parallelepiped(center, uVector, inputvVector, 4.0, 2.0, 0.0);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected !
        }
        try {
            parallelepiped = new Parallelepiped(center, uVector, inputvVector, -4.0, 2.0, 6.0);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected !
        }
        try {
            parallelepiped = new Parallelepiped(center, uVector, inputvVector, 4.0, -2.0, 6.0);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected !
        }
        try {
            parallelepiped = new Parallelepiped(center, uVector, inputvVector, 4.0, 2.0, -6.0);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected !
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#PARALLELEPIPED_SHAPE}
     * 
     * @testedMethod {@link Parallelepiped#distanceTo(Line)}
     * 
     * @description Compute the shortest distance between the surface of the shape and a line of space.
     * 
     * @input Points of space
     * 
     * @output doubles : the distances
     * 
     * @testPassCriteria The output doubles must be the right distance, zero if the line passes through the surface with
     *                   an epsilon of 1e-14 due to the computation errors.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testDistanceToLine() {

        // Creation of a parallelepiped
        final Vector3D center = new Vector3D(1.0, 1.0, 1.0);
        final Vector3D uVector = new Vector3D(1.0, 0.0, 0.0);
        final Vector3D inputvVector = new Vector3D(0.0, 1.0, 0.0);

        final Parallelepiped parallelepiped = new Parallelepiped(center, uVector, inputvVector, 2.0, 2.0, 6.0);

        // test with a line touching a side
        Vector3D origin = new Vector3D(0.0, 0.0, 4.0);
        Vector3D direction = new Vector3D(0.0, 1.0, -1.0);
        Line line = new Line(origin, origin.add(direction));
        double distance = parallelepiped.distanceTo(line);
        Assert.assertEquals(distance, 0.0, this.comparisonEpsilon);

        // test with a line with no intersection
        origin = new Vector3D(0.0, 0.0, 7.0);
        direction = new Vector3D(0.0, 1.0, -1.0);
        line = new Line(origin, origin.add(direction));
        distance = parallelepiped.distanceTo(line);
        Assert.assertEquals(distance, MathLib.sqrt(2.0) / 2.0, this.comparisonEpsilon);

        // test with a line intersecting the shape
        origin = new Vector3D(0.5, 0.5, 2.0);
        direction = new Vector3D(0.0, 0.0, -1.0);
        line = new Line(origin, origin.add(direction));
        distance = parallelepiped.distanceTo(line);
        Assert.assertEquals(distance, 0.0, this.comparisonEpsilon);

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#PARALLELEPIPED_SHAPE}
     * 
     * @testedMethod {@link Parallelepiped#getIntersectionPoints(Line)}
     * 
     * @description Test the computation of intersection points between the shape and a line.
     * 
     * @input Lines of space
     * 
     * @output Vector3D[]
     * 
     * @testPassCriteria The output must be an empty array if there is no intersection or if the line belongs to the
     *                   plane defined by the plate, and the right coordinates otherwise with an epsilon of 1e-14 due to
     *                   the computation errors.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testIntersectionsLine() {

        // Creation of a parallelepiped
        final Vector3D center = new Vector3D(1.0, 1.0, 1.0);
        final Vector3D uVector = new Vector3D(1.0, 0.0, 0.0);
        final Vector3D inputvVector = new Vector3D(0.0, 1.0, 0.0);

        final Parallelepiped parallelepiped = new Parallelepiped(center, uVector, inputvVector, 2.0, 2.0, 6.0);

        // test with a line with no intersection
        Vector3D origin = new Vector3D(0.0, 0.0, 7.0);
        Vector3D direction = new Vector3D(0.0, 1.0, -1.0);
        Line line = new Line(origin, origin.add(direction));
        Vector3D[] intersections = parallelepiped.getIntersectionPoints(line);
        Assert.assertEquals(0, intersections.length);

        // test with a line intersecting the shape
        origin = new Vector3D(0.5, 0.5, 2.0);
        direction = new Vector3D(0.0, 0.0, -1.0);
        line = new Line(origin, origin.add(direction));
        intersections = parallelepiped.getIntersectionPoints(line);
        Assert.assertEquals(2, intersections.length);

        Vector3D intersection = intersections[0];
        Assert.assertEquals(0.5, intersection.getX(), this.comparisonEpsilon);
        Assert.assertEquals(0.5, intersection.getY(), this.comparisonEpsilon);
        Assert.assertEquals(4.0, intersection.getZ(), this.comparisonEpsilon);

        intersection = intersections[1];
        Assert.assertEquals(0.5, intersection.getX(), this.comparisonEpsilon);
        Assert.assertEquals(0.5, intersection.getY(), this.comparisonEpsilon);
        Assert.assertEquals(-2.0, intersection.getZ(), this.comparisonEpsilon);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#PARALLELEPIPED_SHAPE}
     * 
     * @testedMethod {@link Parallelepiped#intersects(Line)}
     * 
     * @description Test the intersection between the shape and a line.
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
        // Creation of a parallelepiped
        final Vector3D center = new Vector3D(1.0, 1.0, 1.0);
        final Vector3D uVector = new Vector3D(1.0, 0.0, 0.0);
        final Vector3D inputvVector = new Vector3D(0.0, 1.0, 0.0);

        final Parallelepiped parallelepiped = new Parallelepiped(center, uVector, inputvVector, 2.0, 2.0, 6.0);

        // test with a line with no intersection
        Vector3D origin = new Vector3D(0.0, 0.0, 7.0);
        Vector3D direction = new Vector3D(0.0, 1.0, -1.0);
        Line line = new Line(origin, origin.add(direction));
        boolean intersects = parallelepiped.intersects(line);
        Assert.assertTrue(!intersects);

        // test with a line intersecting the shape
        origin = new Vector3D(0.5, 0.5, 2.0);
        direction = new Vector3D(0.0, 0.0, -1.0);
        line = new Line(origin, origin.add(direction));
        intersects = parallelepiped.intersects(line);
        Assert.assertTrue(intersects);

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#PARALLELEPIPED_SHAPE}
     * 
     * @testedMethod {@link Parallelepiped#closestPointTo(Line)}
     * 
     * @description Compute the point of the parallelepiped realizing the shortest distance to a line of space, and the
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

        // Creation of a parallelepiped
        final Vector3D center = new Vector3D(0.0, 0.0, 0.0);
        final Vector3D uVector = new Vector3D(1.0, 0.0, 0.0);
        final Vector3D inputvVector = new Vector3D(0.0, 1.0, 0.0);

        final Parallelepiped parallelepiped = new Parallelepiped(center, uVector, inputvVector, 2.0, 2.0, 6.0);

        // test with a line with no intersection
        Vector3D origin = new Vector3D(2.0, 0.0, 0.0);
        Vector3D direction = new Vector3D(1.0, 0.0, -1.0);
        Line line = new Line(origin, origin.add(direction));

        Vector3D[] closestPoints = parallelepiped.closestPointTo(line);

        Vector3D point1 = closestPoints[0];
        Vector3D point2 = closestPoints[1];

        Assert.assertEquals(-1.0, point1.getX(), this.comparisonEpsilon);
        Assert.assertEquals(0.0, point1.getY(), this.comparisonEpsilon);
        Assert.assertEquals(3.0, point1.getZ(), this.comparisonEpsilon);
        Assert.assertEquals(-1.0, point2.getX(), this.comparisonEpsilon);
        Assert.assertEquals(0.0, point2.getY(), this.comparisonEpsilon);
        Assert.assertEquals(3.0, point2.getZ(), this.comparisonEpsilon);

        // test with a line intersecting the shape
        origin = new Vector3D(3.0, 0.0, 0.0);
        direction = new Vector3D(-1.0, 1.0, 0.0);
        line = new Line(origin, origin.add(direction));

        closestPoints = parallelepiped.closestPointTo(line);

        point1 = closestPoints[0];
        point2 = closestPoints[1];

        Assert.assertEquals(1.5, point1.getX(), this.comparisonEpsilon);
        Assert.assertEquals(1.5, point1.getY(), this.comparisonEpsilon);
        Assert.assertEquals(0.0, point1.getZ(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point2.getX(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point2.getY(), this.comparisonEpsilon);
        Assert.assertEquals(0.0, point2.getZ(), this.comparisonEpsilon);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#PARALLELEPIPED_SHAPE}
     * 
     * @testedMethod {@link Parallelepiped#toString()}
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

        // Creation of a parallelepiped
        final Vector3D center = new Vector3D(0.0, 0.0, 0.0);
        final Vector3D uVector = new Vector3D(1.0, 0.0, 0.0);
        final Vector3D inputvVector = new Vector3D(0.0, 1.0, 0.0);

        final Parallelepiped parallelepiped = new Parallelepiped(center, uVector, inputvVector, 2.0, 2.0, 6.0);

        // string creation
        final String result = parallelepiped.toString();

        final String expected =
            "Parallelepiped{Center{0; 0; 0},U vector{1; 0; 0},V vector{0; 1; 0},Length{2.0},Width{2.0},Height{6.0}}";
        Assert.assertEquals(expected, result);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#PARALLELEPIPED_SHAPE}
     * 
     * @testedMethod {@link Parallelepiped#getCrossSection(Vector3D)}
     * 
     * @description Creates a parallelepiped, and gets the cross section.
     * 
     * @input a parallelepiped.
     * 
     * @output a double : the cross section
     * 
     * @testPassCriteria The cross section must be the one expected.
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void testCrossSection() {
        final double eps = Precision.DOUBLE_COMPARISON_EPSILON;

        // Creation of a parallelepiped
        final Vector3D center = new Vector3D(0.0, 0.0, 0.0);
        final Vector3D uVector = new Vector3D(1.0, 0.0, 0.0);
        final Vector3D inputvVector = new Vector3D(0.0, 1.0, 0.0);

        final Parallelepiped parallelepiped = new Parallelepiped(center, uVector, inputvVector, 2.0, 2.0, 6.0);

        // three directions tested

        // cross sections computations
        final Vector3D direction1 = new Vector3D(2.0, -2.0, 2.0);
        final Vector3D direction2 = new Vector3D(-5.0, 0.0, 5.0);
        final double crossSection1 = parallelepiped.getCrossSection(direction1);
        final double crossSection2 = parallelepiped.getCrossSection(Vector3D.PLUS_K);
        final double crossSection3 = parallelepiped.getCrossSection(direction2);

        // tests of the values
        final double expectedFirstSection = 28.0 / MathLib.sqrt(3.0);
        Assert.assertEquals(expectedFirstSection, crossSection1, eps);
        final double expectedFirstSection2 = 16.0 / MathLib.sqrt(2.0);
        Assert.assertEquals(expectedFirstSection2, crossSection3, eps);
        Assert.assertEquals(4.0, crossSection2, eps);
    }
}
