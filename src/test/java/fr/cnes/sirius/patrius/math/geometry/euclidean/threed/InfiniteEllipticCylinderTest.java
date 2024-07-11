/**
 * 
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
 * @history Created on 20/10/2011
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:400:17/03/2015: use class FastMath instead of class Math
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.geometry.euclidean.threed;

import junit.framework.Assert;

import org.junit.Test;

import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;

/**
 * <p>
 * Test class for {@link InfiniteEllipticCylinder}
 * </p>
 * 
 * @author Rami Houdroge
 * 
 * @version $Id: InfiniteEllipticCylinderTest.java 17909 2017-09-11 11:57:36Z bignon $
 * 
 * @since 1.0
 * 
 */
public class InfiniteEllipticCylinderTest {

    /** Root of 2 */
    private static final double SQRT2 = MathLib.sqrt(2);

    /** Epsilon for double comparison. */
    private final double comparisonEpsilon = Precision.DOUBLE_COMPARISON_EPSILON;

    /** Epsilon taking into account the machine error. */
    private final double machineEpsilon = Precision.EPSILON;

    /** Epsilon taking into account the machine error. */
    private final double zeroEpsilon = 0.0;

    /** Features description. */
    public enum features {

        /**
         * @featureTitle Infinite Elliptic Cylinder
         * 
         * @featureDescription Creation of an infinite elliptic cylinder. Test constructors in nominal and degraded
         *                     cases.
         * 
         * @coveredRequirements DV-GEOMETRIE_50, DV-GEOMETRIE_60, DV-GEOMETRIE_90, DV-GEOMETRIE_120, DV-GEOMETRIE_130
         */
        INFINITEELLIPTICCYLINDER_SHAPE,

        /**
         * @featureTitle Infinite Elliptic Cylinder
         * 
         * @featureDescription Test InfiniteEllipticCylinder getters.
         * 
         * @coveredRequirements DV-GEOMETRIE_50, DV-GEOMETRIE_60, DV-GEOMETRIE_90, DV-GEOMETRIE_120, DV-GEOMETRIE_130
         */
        INFINITEELLIPTICCYLINDER_PROPS,

        /**
         * @featureTitle Infinite Elliptic Cylinder
         * 
         * @featureDescription Test InfiniteEllipticCylinder basis transformations3.
         * 
         * @coveredRequirements DV-GEOMETRIE_50, DV-GEOMETRIE_60, DV-GEOMETRIE_90, DV-GEOMETRIE_120, DV-GEOMETRIE_130
         */
        INFINITEELLIPTICCYLINDER_BASISTRANSFORMATIONS,

        /**
         * @featureTitle Infinite Elliptic Cylinder
         * 
         * @featureDescription Test InfiniteEllipticCylinder intersection algorithms.
         * 
         * @coveredRequirements DV-GEOMETRIE_50, DV-GEOMETRIE_60, DV-GEOMETRIE_90, DV-GEOMETRIE_120, DV-GEOMETRIE_130,
         *                      DV-GEOMETRIE_140
         */
        INFINITEELLIPTICCYLINDER_INTERSECTIONS,

        /**
         * @featureTitle Infinite Elliptic Cylinder
         * 
         * @featureDescription Test InfiniteEllipticCylinder distance computation algorithms.
         * 
         * @coveredRequirements DV-GEOMETRIE_50, DV-GEOMETRIE_60, DV-GEOMETRIE_90, DV-GEOMETRIE_120, DV-GEOMETRIE_130
         */
        INFINITEELLIPTICCYLINDER_DISTANCES,

        /**
         * @featureTitle Infinite Elliptic Cylinder
         * 
         * @featureDescription Test InfiniteEllipticCylinder toString method.
         * 
         * @coveredRequirements DV-GEOMETRIE_50, DV-GEOMETRIE_60, DV-GEOMETRIE_90, DV-GEOMETRIE_120, DV-GEOMETRIE_130
         */
        INFINITEELLIPTICCYLINDER_STRING
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#INFINITEELLIPTICCYLINDER_SHAPE}
     * 
     * @testedMethod {@link InfiniteEllipticCylinder#InfiniteEllipticCylinder(Vector3D, Vector3D, Vector3D, double, double)}
     * 
     * @description Instantiation of an infinite elliptic cylinder shape in 3D space.
     * 
     * @input A position vector, an axis vector, an orthogonal axis vector and two semi axes
     * 
     * @output InfiniteEllipticCylinder
     * 
     * @testPassCriteria The cylinder shape can be created only if they are strictly positive. The axis and orthogonal
     *                   axis must not be parallel, an exception is thrown otherwise.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public final void testInfiniteEllipticCylinder() {

        final Vector3D origin = new Vector3D(2, 0, 6);
        Vector3D axis = new Vector3D(0, 0, 1);
        Vector3D axisX = new Vector3D(1, 0, 0);
        double a = 2;
        double b = 1;
        try {
            new InfiniteEllipticCylinder(origin, axis, axisX, a, b);
        } catch (final IllegalArgumentException e) {
            Assert.fail();
        }

        a = -1;
        try {
            new InfiniteEllipticCylinder(origin, axis, axisX, a, b);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected!
        }

        a = 1;
        b = -1;
        try {
            new InfiniteEllipticCylinder(origin, axis, axisX, a, b);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected!
        }

        b = 1;
        axisX = new Vector3D(0, 0, 1);
        try {
            new InfiniteEllipticCylinder(origin, axis, axisX, a, b);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected!
        }

        axisX = new Vector3D(0, 0, 0);
        try {
            new InfiniteEllipticCylinder(origin, axis, axisX, a, b);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected!
        }

        axisX = new Vector3D(1, 0, 0);
        axis = new Vector3D(0, 0, 0);
        try {
            new InfiniteEllipticCylinder(origin, axis, axisX, a, b);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected!
        }

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#INFINITEELLIPTICCYLINDER_PROPS}
     * 
     * @testedMethod {@link InfiniteEllipticCylinder#getDirection()}
     * 
     * @description Test InfiniteEllipticCylinder getters.
     * 
     * @input none
     * 
     * @output Vector3D containing the normalized cylinder axis
     * 
     * @testPassCriteria The returned axis is the same as the user specified axis (with an epsilon of 1e-16 due to the
     *                   machine errors only : we check that the axis is indeed the one given at the construction
     *                   normalized).
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public final void testGetAxis() {

        final Vector3D origin = new Vector3D(2, -5, 6);
        final Vector3D axis = new Vector3D(5, 3, 1);
        final Vector3D axisX = new Vector3D(1, -2, .8);
        final double a = 2;
        final double b = 1;
        final InfiniteEllipticCylinder myCl = new InfiniteEllipticCylinder(origin, axis, axisX, a, b);

        Assert.assertEquals(axis.normalize().getX(), myCl.getDirection().getX(), this.machineEpsilon);
        Assert.assertEquals(axis.normalize().getY(), myCl.getDirection().getY(), this.machineEpsilon);
        Assert.assertEquals(axis.normalize().getZ(), myCl.getDirection().getZ(), this.machineEpsilon);

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#INFINITEELLIPTICCYLINDER_PROPS}
     * 
     * @testedMethod {@link InfiniteEllipticCylinder#getOrigin()}
     * 
     * @description Test InfiniteEllipticCylinder getters.
     * 
     * @input none
     * 
     * @output Vector3D containing the cylinder position
     * 
     * @testPassCriteria The returned position is the same as the user specified position (with an epsilon of 1e-16 due
     *                   to the machine errors : we check if the origin is indeed the one given at the construction).
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public final void testGetOrigin() {

        final Vector3D origin = new Vector3D(2, -5, 6);
        final Vector3D axis = new Vector3D(5, 3, 1);
        final Vector3D axisX = new Vector3D(1, -2, .8);
        final double a = 2;
        final double b = 1;
        final InfiniteEllipticCylinder myCl = new InfiniteEllipticCylinder(origin, axis, axisX, a, b);

        Assert.assertEquals(origin.getX(), myCl.getOrigin().getX(), this.machineEpsilon);
        Assert.assertEquals(origin.getY(), myCl.getOrigin().getY(), this.machineEpsilon);
        Assert.assertEquals(origin.getZ(), myCl.getOrigin().getZ(), this.machineEpsilon);

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#INFINITEELLIPTICCYLINDER_PROPS}
     * 
     * @testedMethod {@link InfiniteEllipticCylinder#getSemiAxisA()}
     * 
     * @description Test InfiniteEllipticCylinder getters.
     * 
     * @input none
     * 
     * @output double containing semi axis a
     * 
     * @testPassCriteria The returned semi axis is the same as the user specified semi axis (with an epsilon of 1e-16
     *                   due to the machine errors : we check if the semi axis is indeed the one given at the
     *                   construction).
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public final void testGetSemiAxisA() {

        final Vector3D origin = new Vector3D(2, -5, 6);
        final Vector3D axis = new Vector3D(5, 3, 1);
        final Vector3D axisX = new Vector3D(1, -2, .8);
        final double a = 2;
        final double b = 1;
        final InfiniteEllipticCylinder myCl = new InfiniteEllipticCylinder(origin, axis, axisX, a, b);

        Assert.assertEquals(a, myCl.getSemiAxisA(), this.machineEpsilon);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#INFINITEELLIPTICCYLINDER_PROPS}
     * 
     * @testedMethod {@link InfiniteEllipticCylinder#getSemiAxisB()}
     * 
     * @description Test InfiniteEllipticCylinder getters.
     * 
     * @input none
     * 
     * @output double containing semi axis b
     * 
     * @testPassCriteria The returned semi axis is the same as the user specified semi axis (with an epsilon of 1e-16
     *                   due to the machine errors : we check if the semi axis is indeed the one given at the
     *                   construction).
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public final void testGetSemiAxisB() {

        final Vector3D origin = new Vector3D(2, -5, 6);
        final Vector3D axis = new Vector3D(5, 3, 1);
        final Vector3D axisX = new Vector3D(1, -2, .8);
        final double a = 2;
        final double b = 1;
        final InfiniteEllipticCylinder myCl = new InfiniteEllipticCylinder(origin, axis, axisX, a, b);

        Assert.assertEquals(b, myCl.getSemiAxisB(), this.machineEpsilon);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#INFINITEELLIPTICCYLINDER_BASISTRANSFORMATIONS}
     * 
     * @testedMethod {@link InfiniteEllipticCylinder#getLocalBasisTransform()},
     *               {@link InfiniteEllipticCylinder#getStandardBasisTransform()}
     * 
     * @description Test InfiniteEllipticCylinder getters.
     * 
     * @input Origin = (2, -5, 6)
     * @input Axis = (5, 3, 1)
     * @input AxisX = (1, -2, .8)
     * @input a=2 and b=1
     * 
     * @output Matrix3D containing coordinates of target basis expressed in current basis
     * 
     * @testPassCriteria The local basis transform matrix contains the coordinates of the standard basis vectors
     *                   expressed in the local basis. The product of both matrices must equal the identity matrix. All
     *                   with an epsilon of 1e-14 due to the computation errors.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public final void testGetLocalBasisTransform() {

        final Vector3D origin = new Vector3D(2, -5, 6);
        final Vector3D axis = new Vector3D(5, 3, 1);
        final Vector3D axisX = new Vector3D(1, -2, .8);
        final double a = 2;
        final double b = 1;
        final InfiniteEllipticCylinder myCl = new InfiniteEllipticCylinder(origin, axis, axisX, a, b);

        // Get frame
        final Matrix3D myFrame = myCl.getStandardBasisTransform();

        // Get frame vectors
        final Vector3D myXAxis = new Vector3D(myFrame.getEntry(0, 0), myFrame.getEntry(1, 0), myFrame.getEntry(2, 0));
        final Vector3D myYAxis = new Vector3D(myFrame.getEntry(0, 1), myFrame.getEntry(1, 1), myFrame.getEntry(2, 1));
        final Vector3D myZAxis = new Vector3D(myFrame.getEntry(0, 2), myFrame.getEntry(1, 2), myFrame.getEntry(2, 2));

        // Check if returned Z-axis is parallel to specified axis of revolution
        Assert.assertEquals(1, myZAxis.normalize().dotProduct(axis.normalize()), this.comparisonEpsilon);
        Assert.assertEquals(0, myZAxis.normalize().crossProduct(axis.normalize()).getNorm(), this.comparisonEpsilon);

        // Check if reference frame vectors are of norm one
        Assert.assertEquals(1, myXAxis.getNorm(), this.comparisonEpsilon);
        Assert.assertEquals(1, myYAxis.getNorm(), this.comparisonEpsilon);
        Assert.assertEquals(1, myZAxis.getNorm(), this.comparisonEpsilon);

        // Check if reference frame vectors are orthogonal
        Assert.assertEquals(0, myXAxis.dotProduct(myYAxis), this.comparisonEpsilon);
        Assert.assertEquals(0, myYAxis.dotProduct(myZAxis), this.comparisonEpsilon);
        Assert.assertEquals(0, myZAxis.dotProduct(myXAxis), this.comparisonEpsilon);

        // Check if this frame is direct
        Assert.assertEquals(1, myZAxis.dotProduct(myXAxis.crossProduct(myYAxis)), this.comparisonEpsilon);
        Assert.assertEquals(0, myZAxis.crossProduct(myXAxis.crossProduct(myYAxis)).getNorm(), this.comparisonEpsilon);
        Assert.assertEquals(1, myXAxis.dotProduct(myYAxis.crossProduct(myZAxis)), this.comparisonEpsilon);
        Assert.assertEquals(0, myXAxis.crossProduct(myYAxis.crossProduct(myZAxis)).getNorm(), this.comparisonEpsilon);
        Assert.assertEquals(1, myYAxis.dotProduct(myZAxis.crossProduct(myXAxis)), this.comparisonEpsilon);
        Assert.assertEquals(0, myYAxis.crossProduct(myZAxis.crossProduct(myXAxis)).getNorm(), this.comparisonEpsilon);

        // Make sure the computed inverted transform satisfies A^T * A = I
        final Matrix3D myFrameInv = myCl.getLocalBasisTransform();
        final Matrix3D productOfBoth = myFrame.multiply(myFrameInv);
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                final double expected = i == j ? 1 : 0;
                Assert.assertEquals(expected, productOfBoth.getEntry(i, j), this.comparisonEpsilon);
            }
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#INFINITEELLIPTICCYLINDER_BASISTRANSFORMATIONS}
     * 
     * @testedMethod {@link InfiniteEllipticCylinder#getAffineLocalExpression(Vector3D)},
     *               {@link InfiniteEllipticCylinder#getAffineStandardExpression(Vector3D)}
     * 
     * @description Test InfiniteEllipticCylinder basis transformations.
     * 
     * @input Vector3D
     * 
     * @output Vector3D expressed in taget basis
     * 
     * @testPassCriteria For a vector expressed in the standard basis, the vector expressed in the local basis must be
     *                   the same as the predicted one, and the transformation into the standard basis must yield the
     *                   same result with an epsilon of 1e-14 due to the computation errors.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public final void testGetAffineLocalExpression() {

        final Vector3D origin = new Vector3D(2, 0, 0);
        final Vector3D axis = new Vector3D(1, 0, 0);
        final Vector3D axisX = new Vector3D(0, 1, 0);
        final double a = 2;
        final double b = 1;
        final InfiniteEllipticCylinder myCl = new InfiniteEllipticCylinder(origin, axis, axisX, a, b);

        // Point
        final Vector3D myPoint = new Vector3D(1, 0, 0);
        Vector3D newPoint = myCl.getAffineLocalExpression(myPoint);
        Assert.assertEquals(0, newPoint.getX(), this.comparisonEpsilon);
        Assert.assertEquals(0, newPoint.getY(), this.comparisonEpsilon);
        Assert.assertEquals(-1, newPoint.getZ(), this.comparisonEpsilon);

        // reverse transformation
        newPoint = myCl.getAffineStandardExpression(newPoint);
        Assert.assertEquals(myPoint.getX(), newPoint.getX(), this.comparisonEpsilon);
        Assert.assertEquals(myPoint.getY(), newPoint.getY(), this.comparisonEpsilon);
        Assert.assertEquals(myPoint.getZ(), newPoint.getZ(), this.comparisonEpsilon);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#INFINITEELLIPTICCYLINDER_BASISTRANSFORMATIONS}
     * 
     * @testedMethod {@link InfiniteEllipticCylinder#getVectorialLocalExpression(Vector3D)},
     *               {@link InfiniteEllipticCylinder#getVectorialStandardExpression(Vector3D)}
     * 
     * @description Test InfiniteEllipticCylinder basis transformations.
     * 
     * @input Vector3D
     * 
     * @output Vector3D expressed in taget basis
     * 
     * @testPassCriteria For a vector expressed in the standard basis, the vector expressed in the local basis must be
     *                   the same as the predicted one, and the transformation into the standard basis must yield the
     *                   same result with an epsilon of 1e-14 due to the computation errors.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public final void testGetVectorialLocalExpression() {

        final Vector3D origin = new Vector3D(2, 0, 0);
        final Vector3D axis = new Vector3D(1, 0, 0);
        final Vector3D axisX = new Vector3D(0, 1, 0);
        final double a = 2;
        final double b = 1;
        final InfiniteEllipticCylinder myCl = new InfiniteEllipticCylinder(origin, axis, axisX, a, b);

        // Point
        final Vector3D myPoint = new Vector3D(1, 0, 0);
        Vector3D newPoint = myCl.getVectorialLocalExpression(myPoint);
        Assert.assertEquals(0, newPoint.getX(), this.comparisonEpsilon);
        Assert.assertEquals(0, newPoint.getY(), this.comparisonEpsilon);
        Assert.assertEquals(1, newPoint.getZ(), this.comparisonEpsilon);

        // reverse transformation
        newPoint = myCl.getVectorialStandardExpression(newPoint);
        Assert.assertEquals(myPoint.getX(), newPoint.getX(), this.comparisonEpsilon);
        Assert.assertEquals(myPoint.getY(), newPoint.getY(), this.comparisonEpsilon);
        Assert.assertEquals(myPoint.getZ(), newPoint.getZ(), this.comparisonEpsilon);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#INFINITEELLIPTICCYLINDER_INTERSECTIONS}
     * 
     * @testedMethod {@link InfiniteEllipticCylinder#intersects(Line)}
     * 
     * @description Test InfiniteEllipticCylinder intersections algorithm.
     * 
     * @input Line
     * 
     * @output Boolean set to true if the user specified line intersects the cylinder
     * 
     * @testPassCriteria The expected result is the same as the predicted one.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public final void testIntersects() {
        final Vector3D origin = new Vector3D(2, 0, 0);
        final Vector3D axis = new Vector3D(1, 0, 0);
        final Vector3D axisX = new Vector3D(0, 1, 0);
        final double a = 2;
        final double b = 1;
        final InfiniteEllipticCylinder myCl = new InfiniteEllipticCylinder(origin, axis, axisX, a, b);

        Vector3D lineOrg = new Vector3D(0, 0, 0);
        Vector3D lineDir = new Vector3D(0, 0, 1);
        Line myLine = new Line(lineOrg, lineOrg.add(lineDir));
        Assert.assertTrue(myCl.intersects(myLine));

        lineOrg = new Vector3D(2, 0, 0);
        lineDir = new Vector3D(0, 1, 0);
        myLine = new Line(lineOrg, lineOrg.add(lineDir));
        Assert.assertTrue(myCl.intersects(myLine));

        lineOrg = new Vector3D(0, 0, 2);
        lineDir = new Vector3D(1, 0, 0);
        myLine = new Line(lineOrg, lineOrg.add(lineDir));
        Assert.assertFalse(myCl.intersects(myLine));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#INFINITEELLIPTICCYLINDER_INTERSECTIONS}
     * 
     * @testedMethod {@link InfiniteEllipticCylinder#getIntersectionPoints(Line)}
     * 
     * @description Test InfiniteEllipticCylinder intersections algorithm.
     * 
     * @input Line
     * 
     * @output Vector3D[] containing the intersection points, or empty
     * 
     * @testPassCriteria The expected result is the same as the predicted one with an epsilon of 1e-14 due to the
     *                   computation errors.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public final void testGetIntersectionPoints() {

        final Vector3D origin = new Vector3D(2, 0, 0);
        final Vector3D axis = new Vector3D(1, 0, 0);
        final Vector3D axisX = new Vector3D(0, 1, 0);
        final double a = 2;
        final double b = 1;
        final InfiniteEllipticCylinder myCl = new InfiniteEllipticCylinder(origin, axis, axisX, a, b);

        // doesnt intersect
        Vector3D lineOrg = new Vector3D(0, 0, 2);
        Vector3D lineDir = new Vector3D(1, 0, 0);
        Line myLine = new Line(lineOrg, lineOrg.add(lineDir));
        Assert.assertEquals(myCl.getIntersectionPoints(myLine).length, 0);

        // one point
        lineOrg = new Vector3D(0, 0, 1);
        lineDir = new Vector3D(1, 0, 0);
        myLine = new Line(lineOrg, lineOrg.add(lineDir));
        Vector3D[] iscs = myCl.getIntersectionPoints(myLine);
        Assert.assertEquals(iscs.length, 1);
        Assert.assertEquals(0, iscs[0].getX(), this.comparisonEpsilon);
        Assert.assertEquals(0, iscs[0].getY(), this.comparisonEpsilon);
        Assert.assertEquals(1, iscs[0].getZ(), this.comparisonEpsilon);

        // two points
        lineOrg = new Vector3D(2, 0, 0);
        lineDir = new Vector3D(0, 1, 0);
        myLine = new Line(lineOrg, lineOrg.add(lineDir));
        iscs = myCl.getIntersectionPoints(myLine);
        Assert.assertEquals(iscs.length, 2);
        Assert.assertEquals(2, iscs[0].getX(), this.comparisonEpsilon);
        Assert.assertEquals(a, iscs[0].getY(), this.comparisonEpsilon);
        Assert.assertEquals(0, iscs[0].getZ(), this.comparisonEpsilon);
        Assert.assertEquals(2, iscs[1].getX(), this.comparisonEpsilon);
        Assert.assertEquals(-a, iscs[1].getY(), this.comparisonEpsilon);
        Assert.assertEquals(0, iscs[1].getZ(), this.comparisonEpsilon);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#INFINITEELLIPTICCYLINDER_DISTANCES}
     * 
     * @testedMethod {@link InfiniteEllipticCylinder#closestPointTo(Vector3D)},
     *               {@link InfiniteEllipticCylinder#distanceTo(Vector3D)}
     * 
     * @description Test InfiniteEllipticCylinder distance computation algorithms, test part1.
     * 
     * @input Vector3D
     * 
     * @output Vector3D containing the closest computed point
     * 
     * @testPassCriteria The expected result is the same as the predicted one with an epsilon of 1e-14 due to the
     *                   computation errors.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public final void testClosestPointToVector3DPart1() {

        /* Normally oriented */
        Vector3D origin = new Vector3D(0, 0, 0);
        Vector3D axis = new Vector3D(0, 0, 1);
        Vector3D axisX = new Vector3D(1, 0, 0);
        double a = 2;
        double b = 1;

        InfiniteEllipticCylinder myCl = new InfiniteEllipticCylinder(origin, axis, axisX, a, b);

        // point on x axis
        Vector3D testPoint = new Vector3D(4, 0, 0);
        Vector3D expected = new Vector3D(a, 0, 0);
        Vector3D computed = myCl.closestPointTo(testPoint);

        Assert.assertEquals(expected.getX(), computed.getX(), this.comparisonEpsilon);
        Assert.assertEquals(expected.getY(), computed.getY(), this.comparisonEpsilon);
        Assert.assertEquals(expected.getZ(), computed.getZ(), this.comparisonEpsilon);

        Assert.assertEquals(4 - a, myCl.distanceTo(testPoint), this.comparisonEpsilon);

        // point on y axis
        testPoint = new Vector3D(0, 4, 0);
        expected = new Vector3D(0, b, 0);
        computed = myCl.closestPointTo(testPoint);

        Assert.assertEquals(expected.getX(), computed.getX(), this.comparisonEpsilon);
        Assert.assertEquals(expected.getY(), computed.getY(), this.comparisonEpsilon);
        Assert.assertEquals(expected.getZ(), computed.getZ(), this.comparisonEpsilon);

        Assert.assertEquals(4 - b, myCl.distanceTo(testPoint), this.comparisonEpsilon);

        // point on y axis
        testPoint = new Vector3D(0, 0, 1);
        expected = new Vector3D(0, b, 1);
        computed = myCl.closestPointTo(testPoint);

        Assert.assertEquals(expected.getX(), computed.getX(), this.comparisonEpsilon);
        Assert.assertEquals(expected.getY(), computed.getY(), this.comparisonEpsilon);
        Assert.assertEquals(expected.getZ(), computed.getZ(), this.comparisonEpsilon);

        Assert.assertEquals(-b, myCl.distanceTo(testPoint), this.comparisonEpsilon);

        /* Randomly oriented */
        // point at center
        a = 1;
        b = 2;
        testPoint = new Vector3D(0, 0, 80);
        expected = new Vector3D(a, 0, 80);
        myCl = new InfiniteEllipticCylinder(origin, axis, axisX, a, b);
        computed = myCl.closestPointTo(testPoint);
        Assert.assertEquals(expected.getX(), computed.getX(), this.comparisonEpsilon);
        Assert.assertEquals(expected.getY(), computed.getY(), this.comparisonEpsilon);
        Assert.assertEquals(expected.getZ(), computed.getZ(), this.comparisonEpsilon);

        Assert.assertEquals(-a, myCl.distanceTo(testPoint), this.comparisonEpsilon);

        // point on local x axis
        a = 6;
        b = 2;
        origin = new Vector3D(0, 0, 0);
        axis = new Vector3D(1, 0, 0);
        axisX = new Vector3D(1, 1, 0);
        myCl = new InfiniteEllipticCylinder(origin, axis, axisX, a, b);
        testPoint = new Vector3D(0, 10, 0);
        expected = new Vector3D(0, a, 0);
        computed = myCl.closestPointTo(testPoint);
        Assert.assertEquals(expected.getX(), computed.getX(), this.comparisonEpsilon);
        Assert.assertEquals(expected.getY(), computed.getY(), this.comparisonEpsilon);
        Assert.assertEquals(expected.getZ(), computed.getZ(), this.comparisonEpsilon);

        Assert.assertEquals(10 - a, myCl.distanceTo(testPoint), this.comparisonEpsilon);

        // point on local x axis
        a = 6;
        b = 2;
        origin = new Vector3D(2, 3, 5);
        axis = new Vector3D(3, -2, 4);
        axisX = new Vector3D(1.8, 3, -8);
        myCl = new InfiniteEllipticCylinder(origin, axis, axisX, a, b);

        testPoint = myCl.getAffineStandardExpression(new Vector3D(10, 0, 0));
        expected = myCl.getAffineStandardExpression(new Vector3D(a, 0, 0));
        computed = myCl.closestPointTo(testPoint);
        Assert.assertEquals(expected.getX(), computed.getX(), this.comparisonEpsilon);
        Assert.assertEquals(expected.getY(), computed.getY(), this.comparisonEpsilon);
        Assert.assertEquals(expected.getZ(), computed.getZ(), this.comparisonEpsilon);

        Assert.assertEquals(10 - a, myCl.distanceTo(testPoint), this.comparisonEpsilon);

        // point on local y axis
        testPoint = myCl.getAffineStandardExpression(new Vector3D(0, 4, 0));
        expected = myCl.getAffineStandardExpression(new Vector3D(0, b, 0));
        computed = myCl.closestPointTo(testPoint);
        Assert.assertEquals(expected.getX(), computed.getX(), this.comparisonEpsilon);
        Assert.assertEquals(expected.getY(), computed.getY(), this.comparisonEpsilon);
        Assert.assertEquals(expected.getZ(), computed.getZ(), this.comparisonEpsilon);

        Assert.assertEquals(4 - b, myCl.distanceTo(testPoint), this.comparisonEpsilon);

        // point at center
        testPoint = myCl.getAffineStandardExpression(new Vector3D(0, 0, 0));
        expected = myCl.getAffineStandardExpression(new Vector3D(0, b, 0));
        computed = myCl.closestPointTo(testPoint);
        Assert.assertEquals(expected.getX(), computed.getX(), this.comparisonEpsilon);
        Assert.assertEquals(expected.getY(), computed.getY(), this.comparisonEpsilon);
        Assert.assertEquals(expected.getZ(), computed.getZ(), this.comparisonEpsilon);
        Assert.assertEquals(-b, myCl.distanceTo(testPoint), this.comparisonEpsilon);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#INFINITEELLIPTICCYLINDER_DISTANCES}
     * 
     * @testedMethod {@link InfiniteEllipticCylinder#closestPointTo(Vector3D)},
     *               {@link InfiniteEllipticCylinder#distanceTo(Vector3D)}
     * 
     * @description Test InfiniteEllipticCylinder distance computation algorithms, test part 2 only to keep part 1 under
     *              100 lines.
     * 
     * @input Vector3D
     * 
     * @output Vector3D containing the closest computed point.
     * 
     * @testPassCriteria The expected result is the same as the predicted one with an epsilon of 1e-14 due to the
     *                   computation errors.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public final void testClosestPointToVector3DPart2() {

        // point outside
        double a = 2;
        double b = 2;
        Vector3D origin = new Vector3D(3, 5, -2);
        Vector3D axis = new Vector3D(4, -5, 1);
        Vector3D axisX = new Vector3D(1, 3, -2);
        InfiniteEllipticCylinder myCl = new InfiniteEllipticCylinder(origin, axis, axisX, a, b);
        Vector3D testPoint = myCl.getAffineStandardExpression(new Vector3D(4, 4, 0));
        Vector3D expected = myCl.getAffineStandardExpression(new Vector3D(SQRT2, SQRT2, 0));
        Vector3D computed = myCl.closestPointTo(testPoint);
        Assert.assertEquals(expected.getX(), computed.getX(), this.comparisonEpsilon);
        Assert.assertEquals(expected.getY(), computed.getY(), this.comparisonEpsilon);
        Assert.assertEquals(expected.getZ(), computed.getZ(), this.comparisonEpsilon);

        Assert.assertEquals((4 - SQRT2) * SQRT2, myCl.distanceTo(testPoint), this.comparisonEpsilon);

        // point on cylinder
        testPoint = myCl.getAffineStandardExpression(new Vector3D(SQRT2, SQRT2, 0));
        expected = myCl.getAffineStandardExpression(new Vector3D(SQRT2, SQRT2, 0));
        computed = myCl.closestPointTo(testPoint);
        Assert.assertEquals(expected.getX(), computed.getX(), this.comparisonEpsilon);
        Assert.assertEquals(expected.getY(), computed.getY(), this.comparisonEpsilon);
        Assert.assertEquals(expected.getZ(), computed.getZ(), this.comparisonEpsilon);

        Assert.assertEquals(0, myCl.distanceTo(testPoint), this.comparisonEpsilon);

        // pb with distance to line -- resolved
        origin = new Vector3D(0, 0, 0);
        axis = new Vector3D(0, 0, 1);
        axisX = new Vector3D(1, 0, 0);
        a = 2;
        b = 3;
        myCl = new InfiniteEllipticCylinder(origin, axis, axisX, a, b);

        testPoint = myCl.getAffineStandardExpression(new Vector3D(3, 0, 0));
        computed = myCl.closestPointTo(testPoint);
        expected = myCl.getAffineStandardExpression(new Vector3D(a, 0, 0));

        Assert.assertEquals(expected.getX(), computed.getX(), this.comparisonEpsilon);
        Assert.assertEquals(expected.getY(), computed.getY(), this.comparisonEpsilon);
        Assert.assertEquals(expected.getZ(), computed.getZ(), this.comparisonEpsilon);

        Assert.assertEquals(3 - a, myCl.distanceTo(testPoint), this.comparisonEpsilon);

        // point inside
        origin = new Vector3D(0, 0, 0);
        axis = new Vector3D(0, 0, 1);
        axisX = new Vector3D(1, 0, 0);
        a = 1;
        b = 1;
        myCl = new InfiniteEllipticCylinder(origin, axis, axisX, a, b);

        testPoint = myCl.getAffineStandardExpression(new Vector3D(.5, .5, 0));
        computed = myCl.closestPointTo(testPoint);
        expected = myCl.getAffineStandardExpression(new Vector3D(SQRT2 / 2, SQRT2 / 2, 0));

        Assert.assertEquals(expected.getX(), computed.getX(), this.comparisonEpsilon);
        Assert.assertEquals(expected.getY(), computed.getY(), this.comparisonEpsilon);
        Assert.assertEquals(expected.getZ(), computed.getZ(), this.comparisonEpsilon);

        Assert.assertEquals((.5 - SQRT2 / 2) * SQRT2, myCl.distanceTo(testPoint), this.comparisonEpsilon);

    }

    /**
     * @testType UT
     * 
     * 
     * @testedFeature {@link features#INFINITEELLIPTICCYLINDER_DISTANCES}
     * 
     * @testedMethod {@link InfiniteEllipticCylinder#closestPointTo(Line)},
     *               {@link InfiniteEllipticCylinder#distanceTo(Line)}
     * 
     * @description Test InfiniteEllipticCylinder distance computation algorithms.
     * 
     * @input Line
     * 
     * @output Vector3D[] containing the closest computed points
     * 
     * @testPassCriteria The expected result is the same as the predicted one with an epsilon of 1e-14 due to the
     *                   computation errors.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public final void testGetClosestPointToLinePart1() {

        // line iscs

        Vector3D origin = new Vector3D(2, 0, 0);
        Vector3D axis = new Vector3D(1, 0, 0);
        Vector3D axisX = new Vector3D(0, 1, 0);
        double a = 2;
        double b = 1;
        InfiniteEllipticCylinder myCl = new InfiniteEllipticCylinder(origin, axis, axisX, a, b);

        Vector3D lineOrg = myCl.getAffineStandardExpression(new Vector3D(5, 0, 0));
        Vector3D lineDir = myCl.getVectorialStandardExpression(new Vector3D(-1, 0, 0));
        Line myLine = new Line(lineOrg, lineOrg.add(lineDir));
        Assert.assertEquals(true, myCl.intersects(myLine));
        Assert.assertEquals(2, myCl.getIntersectionPoints(myLine).length);

        double dist = myCl.distanceTo(myLine);
        Assert.assertEquals(0, dist, this.zeroEpsilon);
        Vector3D[] pts = myCl.closestPointTo(myLine);
        pts[0] = myCl.getAffineLocalExpression(pts[0]);
        pts[1] = myCl.getAffineLocalExpression(pts[1]);
        Assert.assertEquals(pts[0].getX(), pts[1].getX(), this.comparisonEpsilon);
        Assert.assertEquals(pts[0].getY(), pts[1].getY(), this.comparisonEpsilon);
        Assert.assertEquals(pts[0].getZ(), pts[1].getZ(), this.comparisonEpsilon);

        // line iscs in one point
        origin = new Vector3D(2, 0, 0);
        axis = new Vector3D(1, 0, 0);
        axisX = new Vector3D(0, 1, 0);
        a = 2;
        b = 1;
        myCl = new InfiniteEllipticCylinder(origin, axis, axisX, a, b);

        lineOrg = myCl.getAffineStandardExpression(new Vector3D(2, 0, 0));
        lineDir = myCl.getVectorialStandardExpression(new Vector3D(0, 1, 0));
        myLine = new Line(lineOrg, lineOrg.add(lineDir));
        Assert.assertEquals(true, myCl.intersects(myLine));
        Assert.assertEquals(1, myCl.getIntersectionPoints(myLine).length);

        dist = myCl.distanceTo(myLine);
        Assert.assertEquals(0, dist, this.zeroEpsilon);
        pts = myCl.closestPointTo(myLine);
        pts[0] = myCl.getAffineLocalExpression(pts[0]);
        pts[1] = myCl.getAffineLocalExpression(pts[1]);
        Assert.assertEquals(pts[0].getX(), pts[1].getX(), this.comparisonEpsilon);
        Assert.assertEquals(pts[0].getY(), pts[1].getY(), this.comparisonEpsilon);
        Assert.assertEquals(pts[0].getZ(), pts[1].getZ(), this.comparisonEpsilon);

        // line doesnt intersect
        origin = new Vector3D(0, 0, 0);
        axis = new Vector3D(0, 0, 1);
        axisX = new Vector3D(1, 0, 0);
        a = 2;
        b = 3;
        myCl = new InfiniteEllipticCylinder(origin, axis, axisX, a, b);

        lineOrg = myCl.getAffineStandardExpression(new Vector3D(3, 0, 0));
        lineDir = myCl.getVectorialStandardExpression(new Vector3D(0, 1, 0));
        myLine = new Line(lineOrg, lineOrg.add(lineDir));
        Assert.assertEquals(false, myCl.intersects(myLine));

        pts = myCl.closestPointTo(myLine);
        pts[0] = myCl.getAffineLocalExpression(pts[0]);
        pts[1] = myCl.getAffineLocalExpression(pts[1]);
        Assert.assertEquals(3, pts[0].getX(), this.comparisonEpsilon);
        Assert.assertEquals(0, pts[0].getY(), this.comparisonEpsilon);
        Assert.assertEquals(0, pts[0].getZ(), this.comparisonEpsilon);
        Assert.assertEquals(a, pts[1].getX(), this.comparisonEpsilon);
        Assert.assertEquals(0, pts[1].getY(), this.comparisonEpsilon);
        Assert.assertEquals(0, pts[1].getZ(), this.comparisonEpsilon);

        dist = myCl.distanceTo(myLine);
        Assert.assertEquals(1, dist, this.comparisonEpsilon);

        /** Random orientation */
        origin = new Vector3D(2, 5, -8);
        axis = new Vector3D(3, -5, 3.5);
        axisX = new Vector3D(-6, 1, 3.5);
        a = 2.5;
        b = 3.5;
        myCl = new InfiniteEllipticCylinder(origin, axis, axisX, a, b);

        // line along local Y axis. no Z axis component
        lineOrg = myCl.getAffineStandardExpression(new Vector3D(0, 4, 0));
        lineDir = myCl.getVectorialStandardExpression(new Vector3D(0, 0, 1));
        myLine = new Line(lineOrg, lineOrg.add(lineDir));
        Assert.assertEquals(false, myCl.intersects(myLine));

        pts = myCl.closestPointTo(myLine);
        pts[0] = myCl.getAffineLocalExpression(pts[0]);
        pts[1] = myCl.getAffineLocalExpression(pts[1]);
        Assert.assertEquals(0, pts[0].getX(), this.comparisonEpsilon);
        Assert.assertEquals(4, pts[0].getY(), this.comparisonEpsilon);
        Assert.assertEquals(0, pts[0].getZ(), this.comparisonEpsilon);
        Assert.assertEquals(0, pts[1].getX(), this.comparisonEpsilon);
        Assert.assertEquals(b, pts[1].getY(), this.comparisonEpsilon);
        Assert.assertEquals(0, pts[1].getZ(), this.comparisonEpsilon);

        Assert.assertEquals(4 - b, myCl.distanceTo(myLine), this.comparisonEpsilon);

        // line along local X axis. no Z axis component
        lineOrg = myCl.getAffineStandardExpression(new Vector3D(4, 0, 0));
        lineDir = myCl.getVectorialStandardExpression(new Vector3D(0, 0, 1));
        myLine = new Line(lineOrg, lineOrg.add(lineDir));
        Assert.assertEquals(false, myCl.intersects(myLine));

        pts = myCl.closestPointTo(myLine);
        pts[0] = myCl.getAffineLocalExpression(pts[0]);
        pts[1] = myCl.getAffineLocalExpression(pts[1]);
        Assert.assertEquals(4, pts[0].getX(), this.comparisonEpsilon);
        Assert.assertEquals(0, pts[0].getY(), this.comparisonEpsilon);
        Assert.assertEquals(0, pts[0].getZ(), this.comparisonEpsilon);
        Assert.assertEquals(a, pts[1].getX(), this.comparisonEpsilon);
        Assert.assertEquals(0, pts[1].getY(), this.comparisonEpsilon);
        Assert.assertEquals(0, pts[1].getZ(), this.comparisonEpsilon);

        Assert.assertEquals(4 - a, myCl.distanceTo(myLine), this.comparisonEpsilon);

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#INFINITEELLIPTICCYLINDER_DISTANCES}
     * 
     * @testedMethod {@link InfiniteEllipticCylinder#closestPointTo(Line)},
     *               {@link InfiniteEllipticCylinder#distanceTo(Line)}
     * 
     * @description Test InfiniteEllipticCylinder distance computation algorithms. Test part 2 only to keep part 1 under
     *              100 lines.
     * 
     * @input Line
     * 
     * @output Vector3D[] containing the closest computed points
     * 
     * @testPassCriteria The expected result is the same as the predicted one with an epsilon of 1e-14 due to the
     *                   computation errors.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public final void testGetClosestPointToLinePart2() {

        /** Random orientation */
        final Vector3D origin = new Vector3D(2, 5, -8);
        final Vector3D axis = new Vector3D(3, -5, 3.5);
        final Vector3D axisX = new Vector3D(-6, 1, 3.5);
        final double a = 2.5;
        final double b = 3.5;
        final InfiniteEllipticCylinder myCl = new InfiniteEllipticCylinder(origin, axis, axisX, a, b);

        // line along local X axis
        Vector3D lineOrg = myCl.getAffineStandardExpression(new Vector3D(4, 0, 0));
        Vector3D lineDir = myCl.getVectorialStandardExpression(new Vector3D(0, 1, 1));
        Line myLine = new Line(lineOrg, lineOrg.add(lineDir));
        Assert.assertEquals(false, myCl.intersects(myLine));

        Vector3D[] pts = myCl.closestPointTo(myLine);
        pts[0] = myCl.getAffineLocalExpression(pts[0]);
        pts[1] = myCl.getAffineLocalExpression(pts[1]);
        Assert.assertEquals(4, pts[0].getX(), this.comparisonEpsilon);
        Assert.assertEquals(0, pts[0].getY(), this.comparisonEpsilon);
        Assert.assertEquals(0, pts[0].getZ(), this.comparisonEpsilon);
        Assert.assertEquals(a, pts[1].getX(), this.comparisonEpsilon);
        Assert.assertEquals(0, pts[1].getY(), this.comparisonEpsilon);
        Assert.assertEquals(0, pts[1].getZ(), this.comparisonEpsilon);

        Assert.assertEquals(4 - a, myCl.distanceTo(myLine), this.comparisonEpsilon);

        // line along local Y axis
        lineOrg = myCl.getAffineStandardExpression(new Vector3D(0, 4, 0));
        lineDir = myCl.getVectorialStandardExpression(new Vector3D(1, 0, 1));
        myLine = new Line(lineOrg, lineOrg.add(lineDir));
        Assert.assertEquals(false, myCl.intersects(myLine));

        pts = myCl.closestPointTo(myLine);
        pts[0] = myCl.getAffineLocalExpression(pts[0]);
        pts[1] = myCl.getAffineLocalExpression(pts[1]);
        Assert.assertEquals(0, pts[0].getX(), this.comparisonEpsilon);
        Assert.assertEquals(4, pts[0].getY(), this.comparisonEpsilon);
        Assert.assertEquals(0, pts[0].getZ(), this.comparisonEpsilon);
        Assert.assertEquals(0, pts[1].getX(), this.comparisonEpsilon);
        Assert.assertEquals(b, pts[1].getY(), this.comparisonEpsilon);
        Assert.assertEquals(0, pts[1].getZ(), this.comparisonEpsilon);

        Assert.assertEquals(4 - b, myCl.distanceTo(myLine), this.comparisonEpsilon);

        // line along local Z axis.
        lineOrg = myCl.getAffineStandardExpression(new Vector3D(0, 0, 0));
        lineDir = myCl.getVectorialStandardExpression(new Vector3D(0, 0, 1));
        myLine = new Line(lineOrg, lineOrg.add(lineDir));
        Assert.assertEquals(false, myCl.intersects(myLine));

        pts = myCl.closestPointTo(myLine);
        pts[0] = myCl.getAffineLocalExpression(pts[0]);
        pts[1] = myCl.getAffineLocalExpression(pts[1]);
        Assert.assertEquals(0, pts[0].getX(), this.comparisonEpsilon);
        Assert.assertEquals(0, pts[0].getY(), this.comparisonEpsilon);
        Assert.assertEquals(0, pts[0].getZ(), this.comparisonEpsilon);
        // NOTE : modified because of story T-336 (commons-math and orekit update)
        // Two possible results , a or -a, depending on rounding
        Assert.assertEquals(a, MathLib.abs(pts[1].getX()), this.comparisonEpsilon);
        Assert.assertEquals(0, pts[1].getY(), this.comparisonEpsilon);
        Assert.assertEquals(0, pts[1].getZ(), this.comparisonEpsilon);

        Assert.assertEquals(a, myCl.distanceTo(myLine), this.comparisonEpsilon);

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#INFINITEELLIPTICCYLINDER_STRING}
     * 
     * @testedMethod {@link InfiniteEllipticCylinder#toString()}
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

        // Creation of the cylinder
        final Vector3D origin = new Vector3D(2, 5, -8);
        final Vector3D axis = new Vector3D(3, 0, 0);
        final Vector3D axisX = new Vector3D(0, 1, 0);
        final double a = 2.5;
        final double b = 3.5;
        final InfiniteEllipticCylinder myCl = new InfiniteEllipticCylinder(origin, axis, axisX, a, b);

        // string creation
        final String result = myCl.toString();

        final String expected =
            "InfiniteEllipticCylinder{Origin{2; 5; -8},Direction{1; 0; 0},U vector{0; 1; 0},Radius A{2.5},Radius B{3.5}}";
        Assert.assertEquals(expected, result);
    }

}
