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
 *              Validation tests for the object Ellipse.
 *              </p>
 * 
 * @author Thomas Trapier
 * 
 * @version $Id: EllipseTest.java 17909 2017-09-11 11:57:36Z bignon $
 * 
 * @since 1.0
 * 
 */
public class EllipseTest {

    /** Features description. */
    public enum features {

        /**
         * @featureTitle Line segment
         * 
         * @featureDescription Creation of an elliptic shape in 3D space, computation of distance to lines.
         * 
         * @coveredRequirements DV-GEOMETRIE_50, DV-GEOMETRIE_60, DV-GEOMETRIE_90, DV-GEOMETRIE_120, DV-GEOMETRIE_130,
         *                      DV-GEOMETRIE_140
         */
        ELLIPSE_SHAPE
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
     * @testedFeature {@link features#ELLIPSE_SHAPE}
     * 
     * @testedMethod {@link Ellipse#Ellipse(Vector3D, Vector3D, Vector3D, double, double)}
     * 
     * @description Instantiation of an elliptic shape in 3D space from its center, normal, approximative u vector and
     *              two radius.
     * 
     * @input A vector center, a normal vector, u vectors and radiuses (positive, null, negative).
     * 
     * @output Ellipse
     * 
     * @testPassCriteria The elliptic shape can be created only if the normal's and vector's norm and radiuses are
     *                   strictly positive, the normal and approximative u must not be parallel, an exception is thrown
     *                   otherwise. We check the returned elements (center, normal, u, v, a radius and b radius) with
     *                   the ones given at the construction with an epsilon of 1e-16 which takes into account the
     *                   machine error only.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testConstructor() {

        // Creation of a ellipse with not orthogonal input vectors and test
        // of the corrected U and V vectors of the local frame
        final Vector3D center = new Vector3D(1.0, 1.0, 1.0);
        Vector3D normal = new Vector3D(2.0, 0.0, 0.0);
        Vector3D inputuVector = new Vector3D(1.0, 1.0, 0.0);
        double radiusA = 4.0;
        double radiusB = 2.0;
        Ellipse ellipse = new Ellipse(center, normal, inputuVector, radiusA, radiusB);

        // getters tests
        final Vector3D centerOut = ellipse.getCenter();
        Assert.assertEquals(1.0, centerOut.getX(), this.machineEpsilon);
        Assert.assertEquals(1.0, centerOut.getY(), this.machineEpsilon);
        Assert.assertEquals(1.0, centerOut.getZ(), this.machineEpsilon);

        final Vector3D normalOut = ellipse.getNormal();
        Assert.assertEquals(1.0, normalOut.getX(), this.machineEpsilon);
        Assert.assertEquals(0.0, normalOut.getY(), this.machineEpsilon);
        Assert.assertEquals(0.0, normalOut.getZ(), this.machineEpsilon);

        final Vector3D uOut = ellipse.getU();
        Assert.assertEquals(0.0, uOut.getX(), this.machineEpsilon);
        Assert.assertEquals(1.0, uOut.getY(), this.machineEpsilon);
        Assert.assertEquals(0.0, uOut.getZ(), this.machineEpsilon);

        final Vector3D vOut = ellipse.getV();
        Assert.assertEquals(0.0, vOut.getX(), this.machineEpsilon);
        Assert.assertEquals(0.0, vOut.getY(), this.machineEpsilon);
        Assert.assertEquals(1.0, vOut.getZ(), this.machineEpsilon);

        final double radAOut = ellipse.getRadiusA();
        Assert.assertEquals(radiusA, radAOut, this.machineEpsilon);

        final double radBOut = ellipse.getRadiusB();
        Assert.assertEquals(radiusB, radBOut, this.machineEpsilon);

        // wrong radiuses
        try {
            radiusA = 0.0;
            ellipse = new Ellipse(center, normal, inputuVector, radiusA, radiusB);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected !
        }
        try {
            radiusA = -1.0;
            ellipse = new Ellipse(center, normal, inputuVector, radiusA, radiusB);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected !
        }
        try {
            radiusA = 1.0;
            radiusB = 0.0;
            ellipse = new Ellipse(center, normal, inputuVector, radiusA, radiusB);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected !
        }
        try {
            radiusB = -1.0;
            ellipse = new Ellipse(center, normal, inputuVector, radiusA, radiusB);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected !
        }

        // tests with wrong normal and input U vector
        try {
            radiusB = 2.0;
            normal = new Vector3D(0.0, 0.0, 0.0);
            ellipse = new Ellipse(center, normal, inputuVector, radiusA, radiusB);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected !
        }
        try {
            radiusB = 2.0;
            normal = new Vector3D(2.0, 0.0, 0.0);
            inputuVector = new Vector3D(0.0, 0.0, 0.0);
            ellipse = new Ellipse(center, normal, inputuVector, radiusA, radiusB);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected !
        }
        try {
            radiusB = 2.0;
            normal = new Vector3D(2.0, 0.0, 0.0);
            inputuVector = new Vector3D(1.0, 0.0, 0.0);
            ellipse = new Ellipse(center, normal, inputuVector, radiusA, radiusB);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected !
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ELLIPSE_SHAPE}
     * 
     * @testedMethod {@link Ellipse#distanceTo(Line)}
     * 
     * @description Compute the shortest distance between the elliptic shape and a line of space.
     * 
     * @input Lines of space
     * 
     * @output doubles : the distances
     * 
     * @testPassCriteria The output doubles must be the right distance (with an epsilon of 1e-14 due to the computation
     *                   errors), exactly zero if the line touches the elliptic shape.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testDistanceToLine() {

        // Creation of an ellipse
        Vector3D center = new Vector3D(1.0, 1.0, 1.0);
        Vector3D normal = new Vector3D(2.0, 0.0, 0.0);
        Vector3D inputuVector = new Vector3D(1.0, 1.0, 0.0);
        double radiusA = 3.0;
        double radiusB = 3.0;
        Ellipse ellipse = new Ellipse(center, normal, inputuVector, radiusA, radiusB);

        // test with an intersecting line
        Vector3D origLine = new Vector3D(0.0, 3.0, 2.0);
        Vector3D dirLine = new Vector3D(1.0, 0.0, 0.0);
        Line line = new Line(origLine, origLine.add(dirLine));

        double distance = ellipse.distanceTo(line);
        Assert.assertEquals(0.0, distance, this.zeroEpsilon);

        // test with a line parallel to the normal
        origLine = new Vector3D(0.0, 4.0, 3.0);
        dirLine = new Vector3D(1.0, 0.0, 0.0);
        line = new Line(origLine, origLine.add(dirLine));

        double realDistance = MathLib.sqrt(13) - 3;
        distance = ellipse.distanceTo(line);
        Assert.assertEquals(realDistance, distance, this.comparisonEpsilon);

        // test with a line not parallel to the normal
        // closest point is one of the first four tested
        origLine = new Vector3D(1.0, 1.0, 5.0);
        dirLine = new Vector3D(1.0, 0.0, -1.0);
        line = new Line(origLine, origLine.add(dirLine));

        realDistance = MathLib.sqrt(2.0) / 2.0;
        distance = ellipse.distanceTo(line);
        Assert.assertEquals(realDistance, distance, this.comparisonEpsilon);

        // test with a line with the same distance to two of the first points
        // and equal distance to the others
        origLine = new Vector3D(2.0, 1.0, 5.0);
        dirLine = new Vector3D(0.0, 0.0, 1.0);
        line = new Line(origLine, origLine.add(dirLine));

        realDistance = 1.0;
        distance = ellipse.distanceTo(line);
        Assert.assertEquals(realDistance, distance, this.comparisonEpsilon);

        // test with a line not parallel to the normal
        // closest point is not one of the first points tested
        origLine = new Vector3D(1.0, 4.0, 4.0);
        dirLine = new Vector3D(1.0, -1.0 / MathLib.sqrt(2.0), -1.0 / MathLib.sqrt(2.0));
        line = new Line(origLine, origLine.add(dirLine));

        realDistance = (3.0 * (MathLib.sqrt(2.0) - 1.0)) / MathLib.sqrt(2.0);
        distance = ellipse.distanceTo(line);
        Assert.assertEquals(realDistance, distance, this.comparisonEpsilon);

        center = new Vector3D(-1.0, -1.0, 0.0);
        normal = new Vector3D(1.0, 0.0, 1.0);
        inputuVector = new Vector3D(1.0, 0.0, -1.0);
        radiusA = MathLib.sqrt(2.0);
        radiusB = MathLib.sqrt(2.0);
        ellipse = new Ellipse(center, normal, inputuVector, radiusA, radiusB);

        origLine = new Vector3D(0.0, 0.0, 0.0);
        dirLine = new Vector3D(0.0, 1.0, 0.0);
        line = new Line(origLine, origLine.add(dirLine));

        realDistance = MathLib.sqrt(2.0) / 2.0;
        distance = ellipse.distanceTo(line);
        Assert.assertEquals(realDistance, distance, this.comparisonEpsilon);

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ELLIPSE_SHAPE}
     * 
     * @testedMethod {@link Ellipse#getIntersectionPoints(Line)}
     * 
     * @description Test the computation of intersection points between the shape and a line.
     * 
     * @input Lines of space
     * 
     * @output Vector3D[]
     * 
     * @testPassCriteria The output must be an empty array if there is no intersection or if the line belongs to the
     *                   plane defined by the plate, and the right coordinates otherwise (with an epsilon of 1e-14 due
     *                   to the computation errors).
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testIntersectionsLine() {

        // Creation of an ellipse

        final Vector3D center = new Vector3D(1.0, 1.0, 1.0);
        final Vector3D normal = new Vector3D(2.0, 0.0, 0.0);
        final Vector3D inputuVector = new Vector3D(1.0, 1.0, 0.0);
        final double radiusA = 4.0;
        final double radiusB = 2.0;
        final Ellipse ellipse = new Ellipse(center, normal, inputuVector, radiusA, radiusB);

        // test with an intersecting line
        Vector3D origLine = new Vector3D(0.0, 3.0, 2.0);
        Vector3D dirLine = new Vector3D(1.0, -1.0, -1.0);
        Line line = new Line(origLine, origLine.add(dirLine));
        Vector3D[] intersections = ellipse.getIntersectionPoints(line);
        Assert.assertEquals(1, intersections.length);

        Vector3D intersect1 = intersections[0];

        Assert.assertEquals(1.0, intersect1.getX(), this.comparisonEpsilon);
        Assert.assertEquals(2.0, intersect1.getY(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, intersect1.getZ(), this.comparisonEpsilon);

        // test with an intersecting line : intersecting the center
        origLine = new Vector3D(2.0, 2.0, 2.0);
        dirLine = new Vector3D(1.0, 1.0, 1.0);
        line = new Line(origLine, origLine.add(dirLine));
        intersections = ellipse.getIntersectionPoints(line);
        Assert.assertEquals(1, intersections.length);

        intersect1 = intersections[0];

        Assert.assertEquals(1.0, intersect1.getX(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, intersect1.getY(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, intersect1.getZ(), this.comparisonEpsilon);

        // test with a non intersecting line
        origLine = new Vector3D(3.0, 3.0, 2.0);
        dirLine = new Vector3D(-1.0, 0.5, 1.0);
        line = new Line(origLine, origLine.add(dirLine));

        intersections = ellipse.getIntersectionPoints(line);
        Assert.assertEquals(0, intersections.length);

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ELLIPSE_SHAPE}
     * 
     * @testedMethod {@link Ellipse#intersects(Line)}
     * 
     * @description Test the intersection between the shape and a line.
     * 
     * @input Lines of space
     * 
     * @output booleans
     * 
     * @testPassCriteria The output boolean must be true if the line does intersects the surface, false otherwise.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testIntersectsLine() {

        // Creation of an ellipse
        final Vector3D center = new Vector3D(1.0, 1.0, 1.0);
        final Vector3D normal = new Vector3D(2.0, 0.0, 0.0);
        final Vector3D inputuVector = new Vector3D(1.0, 1.0, 0.0);
        final double radiusA = 4.0;
        final double radiusB = 2.0;
        final Ellipse ellipse = new Ellipse(center, normal, inputuVector, radiusA, radiusB);

        // test with an intersecting line
        Vector3D origLine = new Vector3D(0.0, 3.0, 2.0);
        Vector3D dirLine = new Vector3D(1.0, -0.5, -1.0);
        Line line = new Line(origLine, origLine.add(dirLine));
        boolean intersects = ellipse.intersects(line);
        Assert.assertTrue(intersects);

        // test with a non intersecting line
        origLine = new Vector3D(3.0, 3.0, 2.0);
        dirLine = new Vector3D(-1.0, 0.5, 1.0);
        line = new Line(origLine, origLine.add(dirLine));
        intersects = ellipse.intersects(line);
        Assert.assertTrue(!intersects);

        // test with a line parallel to the ellipse's plane
        origLine = new Vector3D(3.0, 3.0, 2.0);
        dirLine = new Vector3D(0.0, 0.5, 1.0);
        line = new Line(origLine, origLine.add(dirLine));
        intersects = ellipse.intersects(line);
        Assert.assertTrue(!intersects);

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ELLIPSE_SHAPE}
     * 
     * @testedMethod {@link Ellipse#distanceTo(Vector3D)}
     * 
     * @description Compute the shortest distance between the elliptic shape and a point of space.
     * 
     * @input Points of space (Vector3D)
     * 
     * @output double
     * 
     * @testPassCriteria The output doubles must be the right distance, zero if the point belongs to the elliptic shape
     *                   (with an epsilon of 1e-14 due to the computation errors).
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testDistanceToPoint() {

        // Creation of an ellipse
        final Vector3D center = new Vector3D(1.0, 1.0, 1.0);
        final Vector3D normal = new Vector3D(2.0, 0.0, 0.0);
        final Vector3D inputuVector = new Vector3D(1.0, 1.0, 0.0);
        final double radiusA = 4.0;
        double radiusB = 2.0;
        Ellipse ellipse = new Ellipse(center, normal, inputuVector, radiusA, radiusB);

        // test with a point closest to the inside of the shape
        Vector3D point = new Vector3D(5.0, 3.0, 2.0);
        double distance = ellipse.distanceTo(point);
        Assert.assertEquals(4.0, distance, this.comparisonEpsilon);

        // test with a point closest to the ellipse
        point = new Vector3D(5.0, 1.0, 6.0);
        distance = ellipse.distanceTo(point);
        Assert.assertEquals(5.0, distance, this.comparisonEpsilon);

        // test with a point belonging to the shape
        point = new Vector3D(1.0, 2.0, 3.0);
        distance = ellipse.distanceTo(point);
        Assert.assertEquals(0.0, distance, this.comparisonEpsilon);

        // test with a point closest to the ellipse, not one of the first four tested
        radiusB = 4.0;
        ellipse = new Ellipse(center, normal, inputuVector, radiusA, radiusB);
        point = new Vector3D(5.0, 1.0 + 7.0 * MathLib.cos(0.2), 1.0 + 7.0 * MathLib.sin(0.2));
        distance = ellipse.distanceTo(point);
        Assert.assertEquals(5.0, distance, this.comparisonEpsilon);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ELLIPSE_SHAPE}
     * 
     * @testedMethod {@link Ellipse#closestPointTo(Vector3D)}
     * 
     * @description Compute the point of the elliptic realizing the shortest distance to a point of space.
     * 
     * @input Points of space (Vector3D)
     * 
     * @output Vector3D
     * 
     * @testPassCriteria The output vector must be the one of the elliptic shape realizing the shortest distance (with
     *                   an epsilon of 1e-14 due to the computation errors).
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testClosestPointToPoint() {

        // Creation of an ellipse
        final Vector3D center = new Vector3D(1.0, 1.0, 1.0);
        final Vector3D normal = new Vector3D(2.0, 0.0, 0.0);
        final Vector3D inputuVector = new Vector3D(1.0, 1.0, 0.0);
        final double radiusA = 4.0;
        final double radiusB = 2.0;
        final Ellipse ellipse = new Ellipse(center, normal, inputuVector, radiusA, radiusB);

        // test with a point closest to the inside of the shape
        Vector3D point = new Vector3D(5.0, 3.0, 2.0);
        Vector3D closestPoint = ellipse.closestPointTo(point);
        Assert.assertEquals(1.0, closestPoint.getX(), this.comparisonEpsilon);
        Assert.assertEquals(3.0, closestPoint.getY(), this.comparisonEpsilon);
        Assert.assertEquals(2.0, closestPoint.getZ(), this.comparisonEpsilon);

        // tests with a point closest to the ellipse
        point = new Vector3D(5.0, 1.0, 6.0);
        closestPoint = ellipse.closestPointTo(point);
        Assert.assertEquals(1.0, closestPoint.getX(), this.comparisonEpsilon);
        Assert.assertEquals(3.0, closestPoint.getZ(), this.comparisonEpsilon);

        // test with a point belonging to the shape
        point = new Vector3D(1.0, 3.0, 2.0);
        closestPoint = ellipse.closestPointTo(point);
        Assert.assertEquals(1.0, closestPoint.getX(), this.comparisonEpsilon);
        Assert.assertEquals(3.0, closestPoint.getY(), this.comparisonEpsilon);
        Assert.assertEquals(2.0, closestPoint.getZ(), this.comparisonEpsilon);

        // Creation of another ellipse
        final Vector3D center2 = new Vector3D(0.0, 0.0, 0.0);
        final Vector3D normal2 = new Vector3D(0.0, 0.0, 1.0);
        final Vector3D inputuVector2 = new Vector3D(1.0, 0.0, 0.0);
        final double radiusA2 = 4.0;
        final double radiusB2 = 4.0;
        final Ellipse ellipse2 = new Ellipse(center2, normal2, inputuVector2, radiusA2, radiusB2);

        // tests with a point closest to the ellipse
        point = new Vector3D(5.0, 0.0, 0.0);
        closestPoint = ellipse2.closestPointTo(point);
        Assert.assertEquals(4.0, closestPoint.getX(), this.comparisonEpsilon);
        Assert.assertEquals(0.0, closestPoint.getZ(), this.comparisonEpsilon);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ELLIPSE_SHAPE}
     * 
     * @testedMethod {@link Ellipse#closestPointTo(Line)}
     * 
     * @description Compute the point of the elliptic realizing the shortest distance to a line of space, and the
     *              associated point of the line.
     * 
     * @input Points of space (Vector3D)
     * 
     * @output Vector3D[]
     * 
     * @testPassCriteria The output vector must be the one of the elliptic shape and the one of the line realizing the
     *                   shortest distance (with an epsilon of 1e-14 due to the computation errors).
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testClosestPointToLine() {

        // Creation of an ellipse
        final Vector3D center = new Vector3D(1.0, 1.0, 1.0);
        final Vector3D normal = new Vector3D(2.0, 0.0, 0.0);
        final Vector3D inputuVector = new Vector3D(1.0, 1.0, 0.0);
        final double radiusA = 4.0;
        final double radiusB = 2.0;
        final Ellipse ellipse = new Ellipse(center, normal, inputuVector, radiusA, radiusB);

        // test with an intersecting line
        Vector3D origLine = new Vector3D(0.0, 3.0, 2.0);
        Vector3D dirLine = new Vector3D(1.0, -1.0, -1.0);
        Line line = new Line(origLine, origLine.add(dirLine));

        Vector3D[] closestPoints = ellipse.closestPointTo(line);

        Vector3D point1 = closestPoints[0];
        Vector3D point2 = closestPoints[1];

        Assert.assertEquals(1.0, point1.getX(), this.comparisonEpsilon);
        Assert.assertEquals(2.0, point1.getY(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point1.getZ(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point2.getX(), this.comparisonEpsilon);
        Assert.assertEquals(2.0, point2.getY(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point2.getZ(), this.comparisonEpsilon);

        // test with a line parallel to the ellipse
        origLine = new Vector3D(2.0, 1.0, 1.0);
        dirLine = new Vector3D(0.0, 1.0, 0.0);
        line = new Line(origLine, origLine.add(dirLine));

        closestPoints = ellipse.closestPointTo(line);

        point1 = closestPoints[0];
        point2 = closestPoints[1];

        Assert.assertEquals(2.0, point1.getX(), this.comparisonEpsilon);
        Assert.assertEquals(5.0, point1.getY(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point1.getZ(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point2.getX(), this.comparisonEpsilon);
        Assert.assertEquals(5.0, point2.getY(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point2.getZ(), this.comparisonEpsilon);

        // test with a line not parallel to the ellipse
        origLine = new Vector3D(1.0, 6.0, 1.0);
        dirLine = new Vector3D(1.0, 0.0, 0.0);
        line = new Line(origLine, origLine.add(dirLine));

        closestPoints = ellipse.closestPointTo(line);

        point1 = closestPoints[0];
        point2 = closestPoints[1];

        Assert.assertEquals(1.0, point1.getX(), this.comparisonEpsilon);
        Assert.assertEquals(6.0, point1.getY(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point1.getZ(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point2.getX(), this.comparisonEpsilon);
        Assert.assertEquals(5.0, point2.getY(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point2.getZ(), this.comparisonEpsilon);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ELLIPSE_SHAPE}
     * 
     * @testedMethod {@link Ellipse#toString()}
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

        // Creation of an ellipse
        final Vector3D center = new Vector3D(1.0, 1.0, 1.0);
        final Vector3D normal = new Vector3D(2.0, 0.0, 0.0);
        final Vector3D inputuVector = new Vector3D(1.0, 1.0, 0.0);
        final double radiusA = 4.0;
        final double radiusB = 2.0;
        final Ellipse ellipse = new Ellipse(center, normal, inputuVector, radiusA, radiusB);

        // string creation
        final String result = ellipse.toString();

        final String expected = "Ellipse{center{1; 1; 1},normal{1; 0; 0},radius A{4.0},radius B{2.0}}";
        Assert.assertEquals(expected, result);
    }
}
