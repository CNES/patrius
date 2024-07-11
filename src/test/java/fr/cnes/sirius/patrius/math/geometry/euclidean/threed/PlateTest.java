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
 *              Validation tests for the object Plate.
 *              </p>
 * 
 * @author Thomas Trapier
 * 
 * @version $Id: PlateTest.java 17909 2017-09-11 11:57:36Z bignon $
 * 
 * @since 1.0
 * 
 */
public class PlateTest {

    /** Features description. */
    public enum features {

        /**
         * @featureTitle Plate shape
         * 
         * @featureDescription Creation of a rectangle plate shape, computation of distances and intersections with
         *                     lines and points.
         * 
         * @coveredRequirements DV-GEOMETRIE_50, DV-GEOMETRIE_60, DV-GEOMETRIE_90, DV-GEOMETRIE_120, DV-GEOMETRIE_130,
         *                      DV-GEOMETRIE_140
         */
        PLATE_SHAPE
    }

    /** Epsilon for double comparison. */
    private final double comparisonEpsilon = Precision.DOUBLE_COMPARISON_EPSILON;

    /** Epsilon taking into account the machine error. */
    private final double machineEpsilon = Precision.EPSILON;

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#PLATE_SHAPE}
     * 
     * @testedMethod {@link Plate#Plate(Vector3D, Vector3D, Vector3D, double, double)}
     * 
     * @description Instantiation of a rectangle plate from its center, two vectors and two dimensions.
     * 
     * @input A point center, two vectors to describe its orientation and dimensions.
     * 
     * @output Plate
     * 
     * @testPassCriteria the shape can be created only if the two vectors are not parallel and the dimensions strictly
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
        Plate plate1 = new Plate(center, uVector, inputvVector, 4.0, 2.0);

        final Vector3D realvVector = plate1.getV();
        Assert.assertEquals(realvVector.getX(), -MathLib.sqrt(2.0) / 2.0, this.machineEpsilon);
        Assert.assertEquals(realvVector.getY(), MathLib.sqrt(2.0) / 2.0, this.machineEpsilon);
        Assert.assertEquals(realvVector.getZ(), 0.0, this.machineEpsilon);

        final Vector3D realuVector = plate1.getU();
        Assert.assertEquals(realuVector.getX(), MathLib.sqrt(2.0) / 2.0, this.machineEpsilon);
        Assert.assertEquals(realuVector.getY(), MathLib.sqrt(2.0) / 2.0, this.machineEpsilon);
        Assert.assertEquals(realuVector.getZ(), 0.0, this.machineEpsilon);

        final Vector3D realCenter = plate1.getCenter();
        Assert.assertEquals(realCenter.getX(), 1.0, this.machineEpsilon);
        Assert.assertEquals(realCenter.getY(), 1.0, this.machineEpsilon);
        Assert.assertEquals(realCenter.getZ(), 1.0, this.machineEpsilon);

        Assert.assertEquals(plate1.getLength(), 4.0, this.machineEpsilon);
        Assert.assertEquals(plate1.getWidth(), 2.0, this.machineEpsilon);

        // Test of the corner points coordinates
        Vector3D corner = plate1.getC1();
        Assert.assertEquals(corner.getX(), 1.0 + MathLib.sqrt(2.0) / 2.0, this.comparisonEpsilon);
        Assert.assertEquals(corner.getY(), 1.0 + 3.0 * MathLib.sqrt(2.0) / 2.0, this.comparisonEpsilon);
        Assert.assertEquals(corner.getZ(), 1.0, this.comparisonEpsilon);

        corner = plate1.getC2();
        Assert.assertEquals(corner.getX(), 1.0 - 3.0 * MathLib.sqrt(2.0) / 2.0, this.comparisonEpsilon);
        Assert.assertEquals(corner.getY(), 1.0 - MathLib.sqrt(2.0) / 2.0, this.comparisonEpsilon);
        Assert.assertEquals(corner.getZ(), 1.0, this.comparisonEpsilon);

        corner = plate1.getC3();
        Assert.assertEquals(corner.getX(), 1.0 - MathLib.sqrt(2.0) / 2.0, this.comparisonEpsilon);
        Assert.assertEquals(corner.getY(), 1.0 - 3.0 * MathLib.sqrt(2.0) / 2.0, this.comparisonEpsilon);
        Assert.assertEquals(corner.getZ(), 1.0, this.comparisonEpsilon);

        corner = plate1.getC4();
        Assert.assertEquals(corner.getX(), 1.0 + 3.0 * MathLib.sqrt(2.0) / 2.0, this.comparisonEpsilon);
        Assert.assertEquals(corner.getY(), 1.0 + MathLib.sqrt(2.0) / 2.0, this.comparisonEpsilon);
        Assert.assertEquals(corner.getZ(), 1.0, this.comparisonEpsilon);

        // test edges
        final LineSegment[] edges = plate1.getEdges();

        Assert.assertEquals(plate1.getC1().getX(), edges[0].getOrigin().getX(), this.comparisonEpsilon);
        Assert.assertEquals(plate1.getC1().getY(), edges[0].getOrigin().getY(), this.comparisonEpsilon);
        Assert.assertEquals(plate1.getC1().getZ(), edges[0].getOrigin().getZ(), this.comparisonEpsilon);

        Assert.assertEquals(plate1.getC2().getX(), edges[0].getEnd().getX(), this.comparisonEpsilon);
        Assert.assertEquals(plate1.getC2().getY(), edges[0].getEnd().getY(), this.comparisonEpsilon);
        Assert.assertEquals(plate1.getC2().getZ(), edges[0].getEnd().getZ(), this.comparisonEpsilon);

        Assert.assertEquals(plate1.getC2().getX(), edges[1].getOrigin().getX(), this.comparisonEpsilon);
        Assert.assertEquals(plate1.getC2().getY(), edges[1].getOrigin().getY(), this.comparisonEpsilon);
        Assert.assertEquals(plate1.getC2().getZ(), edges[1].getOrigin().getZ(), this.comparisonEpsilon);

        Assert.assertEquals(plate1.getC3().getX(), edges[1].getEnd().getX(), this.comparisonEpsilon);
        Assert.assertEquals(plate1.getC3().getY(), edges[1].getEnd().getY(), this.comparisonEpsilon);
        Assert.assertEquals(plate1.getC3().getZ(), edges[1].getEnd().getZ(), this.comparisonEpsilon);

        Assert.assertEquals(plate1.getC3().getX(), edges[2].getOrigin().getX(), this.comparisonEpsilon);
        Assert.assertEquals(plate1.getC3().getY(), edges[2].getOrigin().getY(), this.comparisonEpsilon);
        Assert.assertEquals(plate1.getC3().getZ(), edges[2].getOrigin().getZ(), this.comparisonEpsilon);

        Assert.assertEquals(plate1.getC4().getX(), edges[2].getEnd().getX(), this.comparisonEpsilon);
        Assert.assertEquals(plate1.getC4().getY(), edges[2].getEnd().getY(), this.comparisonEpsilon);
        Assert.assertEquals(plate1.getC4().getZ(), edges[2].getEnd().getZ(), this.comparisonEpsilon);

        Assert.assertEquals(plate1.getC4().getX(), edges[3].getOrigin().getX(), this.comparisonEpsilon);
        Assert.assertEquals(plate1.getC4().getY(), edges[3].getOrigin().getY(), this.comparisonEpsilon);
        Assert.assertEquals(plate1.getC4().getZ(), edges[3].getOrigin().getZ(), this.comparisonEpsilon);

        Assert.assertEquals(plate1.getC1().getX(), edges[3].getEnd().getX(), this.comparisonEpsilon);
        Assert.assertEquals(plate1.getC1().getY(), edges[3].getEnd().getY(), this.comparisonEpsilon);
        Assert.assertEquals(plate1.getC1().getZ(), edges[3].getEnd().getZ(), this.comparisonEpsilon);

        // Test with parallel input vectors
        inputvVector = new Vector3D(1.8, 1.8, 0.0);
        try {
            plate1 = new Plate(center, uVector, inputvVector, 4.0, 3.0);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected !
        }

        // Test with wrong dimensions
        inputvVector = new Vector3D(1.0, -1.0, 0.0);
        try {
            plate1 = new Plate(center, uVector, inputvVector, 0.0, 3.0);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected !
        }
        try {
            plate1 = new Plate(center, uVector, inputvVector, -0.1, 3.0);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected !
        }
        try {
            plate1 = new Plate(center, uVector, inputvVector, 1.0, 0.0);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected !
        }
        try {
            plate1 = new Plate(center, uVector, inputvVector, 0.0, -1.0);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected !
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#PLATE_SHAPE}
     * 
     * @testedMethod {@link Plate#distanceTo(Line)}
     * 
     * @description Compute the shortest distance between the surface of the shape and a line of space.
     * 
     * @input Lines of space
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

        // Creation of a plate
        final Vector3D center = new Vector3D(0.0, 0.0, 0.0);
        final Vector3D uVector = new Vector3D(1.0, 0.0, 0.0);
        final Vector3D inputvVector = new Vector3D(0.0, 1.0, 0.0);
        final Plate plate1 = new Plate(center, uVector, inputvVector, 2.0, 2.0);

        // test with a line touching a side
        Vector3D origin = new Vector3D(0.0, 0.0, 1.0);
        Vector3D direction = new Vector3D(0.0, 1.0, -1.0);
        Line line = new Line(origin, origin.add(direction));
        double distance = plate1.distanceTo(line);
        Assert.assertEquals(distance, 0.0, this.comparisonEpsilon);

        // test with a closest point belonging to a side
        origin = new Vector3D(0.0, 0.0, 2.0);
        direction = new Vector3D(0.0, 1.0, -1.0);
        line = new Line(origin, origin.add(direction));
        distance = plate1.distanceTo(line);
        Assert.assertEquals(distance, MathLib.sqrt(2.0) / 2.0, this.comparisonEpsilon);

        origin = new Vector3D(0.5, 0.0, 2.0);
        direction = new Vector3D(0.0, -1.0, 1.0);
        line = new Line(origin, origin.add(direction));
        distance = plate1.distanceTo(line);
        Assert.assertEquals(distance, MathLib.sqrt(2.0) / 2.0, this.comparisonEpsilon);

        // test with a closest point equals to a corner
        origin = new Vector3D(2.0, 2.0, 2.0);
        direction = new Vector3D(0.0, 0.0, -1.0);
        line = new Line(origin, origin.add(direction));
        distance = plate1.distanceTo(line);
        Assert.assertEquals(distance, MathLib.sqrt(2.0), this.comparisonEpsilon);

        // test with a line parallel to a side
        origin = new Vector3D(2.0, 2.0, 0.0);
        direction = new Vector3D(1.0, 0.0, 0.0);
        line = new Line(origin, origin.add(direction));
        distance = plate1.distanceTo(line);
        Assert.assertEquals(distance, 1.0, this.comparisonEpsilon);

        // test with a line intersecting the plate
        origin = new Vector3D(0.5, 0.5, 2.0);
        direction = new Vector3D(0.0, 0.0, -1.0);
        line = new Line(origin, origin.add(direction));
        distance = plate1.distanceTo(line);
        Assert.assertEquals(distance, 0.0, this.comparisonEpsilon);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#PLATE_SHAPE}
     * 
     * @testedMethod {@link Plate#intersects(Line)}
     * 
     * @description Test the computation of intersection points between the shape and a line.
     * 
     * @input Lines of space
     * 
     * @output Vector3D[]
     * 
     * @testPassCriteria The output must be null if there is no intersection or if the line belongs to the plane defined
     *                   by the plate, and the right coordinates otherwise with an epsilon of 1e-14 due to the
     *                   computation errors.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testIntersectionsLine() {

        // Creation of a plate
        final Vector3D center = new Vector3D(0.0, 0.0, 0.0);
        final Vector3D uVector = new Vector3D(1.0, 0.0, 0.0);
        final Vector3D inputvVector = new Vector3D(0.0, 1.0, 0.0);
        final Plate plate1 = new Plate(center, uVector, inputvVector, 2.0, 2.0);

        // test with no intersection and a non parallel vector
        Vector3D origin = new Vector3D(0.0, 0.0, 1.0);
        Vector3D direction = new Vector3D(0.0, 2.0, -1.0);
        Line line = new Line(origin, origin.add(direction));
        Vector3D[] res = plate1.getIntersectionPoints(line);
        Assert.assertEquals(0, res.length);

        // test with a vector parallel to the plate
        origin = new Vector3D(0.5, 0.5, 0.0);
        direction = new Vector3D(1.0, 1.0, 0.0);
        line = new Line(origin, origin.add(direction));
        res = plate1.getIntersectionPoints(line);
        Assert.assertEquals(0, res.length);

        // test with an intersection
        direction = new Vector3D(0.0, 0.0, -1.0);
        line = new Line(origin, origin.add(direction));
        res = plate1.getIntersectionPoints(line);
        Assert.assertEquals(1, res.length);
        Assert.assertEquals(0.5, res[0].getX(), this.comparisonEpsilon);
        Assert.assertEquals(0.5, res[0].getY(), this.comparisonEpsilon);
        Assert.assertEquals(0.0, res[0].getZ(), this.comparisonEpsilon);

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#PLATE_SHAPE}
     * 
     * @testedMethod {@link Plate#intersects(Line)}
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
        // Creation of a plate
        final Vector3D center = new Vector3D(1.0, 1.0, 1.0);
        final Vector3D uVector = new Vector3D(1.0, 1.0, 0.0);
        final Vector3D inputvVector = new Vector3D(-5.0, 1.0, 0.0);
        final Plate plate1 = new Plate(center, uVector, inputvVector, 4.0, 2.0);

        // test with no intersection and a non parallel vector
        Vector3D origin = new Vector3D(0.0, 0.0, 8.0);
        Vector3D direction = new Vector3D(0.0, 2.0, -1.0);
        Line line = new Line(origin, origin.add(direction));
        boolean intersects = plate1.intersects(line);
        Assert.assertTrue(!intersects);

        // test with a parallel vector
        origin = new Vector3D(0.0, 0.0, 1.0);
        direction = new Vector3D(1.0, 2.0, 0.0);
        line = new Line(origin, origin.add(direction));
        intersects = plate1.intersects(line);
        Assert.assertTrue(!intersects);

        // test with an intersection
        origin = new Vector3D(1.0, 1.0, 2.0);
        direction = new Vector3D(1.0, 1.0, 1.0);
        line = new Line(origin, origin.add(direction));
        intersects = plate1.intersects(line);
        Assert.assertTrue(intersects);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#PLATE_SHAPE}
     * 
     * @testedMethod {@link Plate#closestPointTo(Line)}
     * 
     * @description Compute the point of the plate realizing the shortest distance to a line of space, and the
     *              associated point of the line.
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

        // Creation of a plate
        final Vector3D center = new Vector3D(1.0, 1.0, 1.0);
        final Vector3D uVector = new Vector3D(1.0, 0.0, 0.0);
        final Vector3D inputvVector = new Vector3D(0.0, 1.0, 0.0);
        final Plate plate = new Plate(center, uVector, inputvVector, 4.0, 2.0);

        // test with an intersecting line
        Vector3D origLine = new Vector3D(2.0, 1.0, 2.0);
        Vector3D dirLine = new Vector3D(0.0, 0.0, -1.0);
        Line line = new Line(origLine, origLine.add(dirLine));

        Vector3D[] closestPoints = plate.closestPointTo(line);

        Vector3D point1 = closestPoints[0];
        Vector3D point2 = closestPoints[1];

        Assert.assertEquals(2.0, point1.getX(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point1.getY(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point1.getZ(), this.comparisonEpsilon);
        Assert.assertEquals(2.0, point2.getX(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point2.getY(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point2.getZ(), this.comparisonEpsilon);

        // test with a line closest to a corner
        origLine = new Vector3D(4.0, 3.0, 2.0);
        dirLine = new Vector3D(0.0, 0.0, -1.0);
        line = new Line(origLine, origLine.add(dirLine));

        closestPoints = plate.closestPointTo(line);

        point1 = closestPoints[0];
        point2 = closestPoints[1];

        Assert.assertEquals(4.0, point1.getX(), this.comparisonEpsilon);
        Assert.assertEquals(3.0, point1.getY(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point1.getZ(), this.comparisonEpsilon);
        Assert.assertEquals(3.0, point2.getX(), this.comparisonEpsilon);
        Assert.assertEquals(2.0, point2.getY(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point2.getZ(), this.comparisonEpsilon);

        // test with a line closest to an edge
        origLine = new Vector3D(2.0, 2.0, 2.0);
        dirLine = new Vector3D(0.0, 1.0, -1.0);
        line = new Line(origLine, origLine.add(dirLine));

        closestPoints = plate.closestPointTo(line);

        point1 = closestPoints[0];
        point2 = closestPoints[1];

        Assert.assertEquals(2.0, point1.getX(), this.comparisonEpsilon);
        Assert.assertEquals(2.5, point1.getY(), this.comparisonEpsilon);
        Assert.assertEquals(1.5, point1.getZ(), this.comparisonEpsilon);
        Assert.assertEquals(2.0, point2.getX(), this.comparisonEpsilon);
        Assert.assertEquals(2.0, point2.getY(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point2.getZ(), this.comparisonEpsilon);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#PLATE_SHAPE}
     * 
     * @testedMethod {@link Plate#toString()}
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

        // Creation of a plate
        final Vector3D center = new Vector3D(1.0, 1.0, 1.0);
        final Vector3D uVector = new Vector3D(1.0, 0.0, 0.0);
        final Vector3D inputvVector = new Vector3D(0.0, 1.0, 0.0);
        final Plate plate = new Plate(center, uVector, inputvVector, 4.0, 2.0);

        // string creation
        final String result = plate.toString();

        final String expected = "Plate{Center{1; 1; 1},U vector{1; 0; 0},V vector{0; 1; 0},Length{4.0},Width{2.0}}";
        Assert.assertEquals(expected, result);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#PLATE_SHAPE}
     * 
     * @testedMethod {@link Sphere#getCrossSection(Vector3D)}
     * 
     * @description Creates a sphere, and gets the cross section.
     * 
     * @input a sphere.
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

        // Creation of a plate
        final Vector3D center = new Vector3D(1.0, 1.0, 1.0);
        final Vector3D uVector = new Vector3D(1.0, 0.0, 0.0);
        final Vector3D inputvVector = new Vector3D(0.0, 1.0, 0.0);
        final Plate plate = new Plate(center, uVector, inputvVector, 4.0, 2.0);

        final Vector3D direction = new Vector3D(0.0, 2.0, 2.0);

        final double crossSection1 = plate.getCrossSection(direction);
        final double crossSection2 = plate.getCrossSection(Vector3D.PLUS_K);
        final double crossSection3 = plate.getCrossSection(Vector3D.PLUS_I);

        Assert.assertEquals(8.0 / MathLib.sqrt(2.0), crossSection1, eps);
        Assert.assertEquals(8.0, crossSection2, eps);
        Assert.assertEquals(0.0, crossSection3, eps);
    }
}
