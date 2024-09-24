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
 * @history Created on 12/10/2011
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:400:17/03/2015: use class FastMath instead of class Math
 * VERSION::FA:458:19/10/2015: Use class FastMath instead of class Math
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
 * <p>
 * Test class for {@link InfiniteEllipticCone}
 * </p>
 * 
 * @see InfiniteEllipticCone
 * 
 * @author Rami Houdroge
 * 
 * @version $Id: InfiniteEllipticConeTest.java 17909 2017-09-11 11:57:36Z bignon $
 * 
 * @since 1.0
 * 
 */
public class InfiniteEllipticConeTest {

    /** Epsilon for double comparison. */
    private final double comparisonEpsilon = Precision.DOUBLE_COMPARISON_EPSILON;

    /** Epsilon taking into account the machine error. */
    private final double machineEpsilon = Precision.EPSILON;

    /** Features description. */
    public enum features {

        /**
         * @featureTitle InfiniteEllipticCone shape
         * 
         * @featureDescription Creation of a cone, computation of distances and intersections with lines and points.
         * 
         * @coveredRequirements DV-GEOMETRIE_40, DV-GEOMETRIE_50, DV-GEOMETRIE_60, DV-GEOMETRIE_90, DV-GEOMETRIE_120,
         *                      DV-GEOMETRIE_130
         */
        INFINITE_ELLIPTIC_CONE_SHAPE,

        /**
         * @featureTitle InfiniteEllipticCone getters
         * 
         * @featureDescription Test cone getters.
         * 
         * @coveredRequirements DV-GEOMETRIE_40, DV-GEOMETRIE_50, DV-GEOMETRIE_60, DV-GEOMETRIE_90, DV-GEOMETRIE_120,
         *                      DV-GEOMETRIE_130
         */
        INFINITE_ELLIPTIC_CONE_PROPS,

        /**
         * @featureTitle InfiniteEllipticCone transformations
         * 
         * @featureDescription Test cone basis transformations.
         * 
         * @coveredRequirements DV-GEOMETRIE_40, DV-GEOMETRIE_50, DV-GEOMETRIE_60, DV-GEOMETRIE_90, DV-GEOMETRIE_120,
         *                      DV-GEOMETRIE_130
         */
        INFINITE_ELLIPTIC_CONE_BASISTRANSFORMATIONS,

        /**
         * @featureTitle InfiniteEllipticCone intersections
         * 
         * @featureDescription Test cone intersection algorithms.
         * 
         * @coveredRequirements DV-GEOMETRIE_40, DV-GEOMETRIE_50, DV-GEOMETRIE_60, DV-GEOMETRIE_90, DV-GEOMETRIE_120,
         *                      DV-GEOMETRIE_130, DV-GEOMETRIE_140
         */
        INFINITE_ELLIPTIC_CONE_INTERSECTIONS,

        /**
         * @featureTitle InfiniteEllipticCone distance
         * 
         * @featureDescription Test cone distance computation algorithms.
         * 
         * @coveredRequirements DV-GEOMETRIE_40, DV-GEOMETRIE_50, DV-GEOMETRIE_60, DV-GEOMETRIE_90, DV-GEOMETRIE_120,
         *                      DV-GEOMETRIE_130
         */
        INFINITE_ELLIPTIC_CONE_DISTANCES,

        /**
         * @featureTitle InfiniteEllipticCone ToString
         * 
         * @featureDescription ToString method test.
         * 
         * @coveredRequirements NA
         */
        INFINITE_ELLIPTIC_CONE_TO_STRING
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#INFINITE_ELLIPTIC_CONE_SHAPE}
     * 
     * @testedMethod {@link InfiniteEllipticCone#InfiniteEllipticCone (Vector3D, Vector3D, Vector3D, double, double)}
     * 
     * @description Test InfiniteEllipticCone Constructor
     *              {@link InfiniteEllipticCone#InfiniteEllipticCone(Vector3D, Vector3D, Vector3D, double, double)} Here
     *              we check the correctness of the Spheroid class constructor. Nominal case as well as degraded
     *              cases are checked. Once the test is passed, the method is considered correct and used afterwards.
     * 
     * @input Nominal case
     *        <p>
     *        apex = (1,2,3)
     *        </p>
     *        <p>
     *        axis = (0,0,1)
     *        </p>
     *        <p>
     *        axisU = (1,0,0)
     *        </p>
     *        <p>
     *        alpha = PI/4 and beta = PI/5
     *        </p>
     * @input Degraded cases
     *        <p>
     *        incorrect alpha value
     *        </p>
     *        <p>
     *        Incorrect beta value
     *        </p>
     *        <p>
     *        Incorrect axis specified : Cone axis, Perpendicular axis
     *        </p>
     *        <p>
     *        Incorrect axis specified : Perpendicular axis
     *        </p>
     *        <p>
     *        Incorrect axis specified : Cone and perpendicular axis not perpendicular
     *        </p>
     * 
     * @output InfiniteEllipticCone
     * 
     * @testPassCriteria No exception is raised for the nominal cases and the returned elements are the same as the user
     *                   specified ones (with an epsilon of 1e-16 due to the machine errors only : we check that the
     *                   elements are indeed the ones given at the construction), an IllegalArgumentException is raised
     *                   for degraded cases.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testConstructor() {

        // Cone parameters
        Vector3D apex;
        Vector3D axis;
        Vector3D axisU;
        double alpha;
        double beta;
        InfiniteEllipticCone myCone;

        /** ------------ Nominal case ------------ */
        apex = new Vector3D(1, 2, 3);
        axis = new Vector3D(0, 0, 1);
        axisU = new Vector3D(1, 0, 0);
        alpha = FastMath.PI / 4;
        beta = FastMath.PI / 5;
        try {
            myCone = new InfiniteEllipticCone(apex, axis, axisU, alpha, beta);
        } catch (final IllegalArgumentException e) {
            Assert.fail();
        }

        /** ------------ Degraded cases ------------ */

        // incorrect alpha value
        alpha = 0;
        try {
            myCone = new InfiniteEllipticCone(apex, axis, axisU, alpha, beta);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
        alpha = FastMath.PI;
        try {
            myCone = new InfiniteEllipticCone(apex, axis, axisU, alpha, beta);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }

        // Incorrect beta value
        alpha = FastMath.PI / 4;
        beta = FastMath.PI;
        try {
            myCone = new InfiniteEllipticCone(apex, axis, axisU, alpha, beta);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }

        // Incorrect axis specified
        // - Cone axis
        beta = FastMath.PI / 4;
        axis = new Vector3D(0, 0, 5E-11);
        try {
            myCone = new InfiniteEllipticCone(apex, axis, axisU, alpha, beta);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
        // - Perpendicular axis
        axis = new Vector3D(0, 0, 1);
        axisU = new Vector3D(0, 5E-11, 0);
        try {
            myCone = new InfiniteEllipticCone(apex, axis, axisU, alpha, beta);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
        // - Cone and perpendicular axis not perpendicular
        axis = new Vector3D(0, 0, 5E-6);
        axisU = new Vector3D(0, 5E-6, 0);
        try {
            myCone = new InfiniteEllipticCone(apex, axis, axisU, alpha, beta);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }

        // Test getOrigin method
        apex = new Vector3D(1, 2, 3);
        axis = new Vector3D(0, 0, 1);
        axisU = new Vector3D(1, 0, 0);
        alpha = FastMath.PI / 4;
        beta = FastMath.PI / 5;
        myCone = new InfiniteEllipticCone(apex, axis, axisU, alpha, beta);
        final Vector3D origin = myCone.getOrigin();
        Assert.assertEquals(apex.getX(), origin.getX(), this.machineEpsilon);
        Assert.assertEquals(apex.getY(), origin.getY(), this.machineEpsilon);
        Assert.assertEquals(apex.getZ(), origin.getZ(), this.machineEpsilon);

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#INFINITE_ELLIPTIC_CONE_PROPS}
     * 
     * @testedMethod {@link InfiniteEllipticCone#getOrigin()}
     * 
     * @description Test cone getters.
     * 
     * @input apex = (1, 2, 3)
     * @input axis = (0, 0, 1)
     * @input axisU = (1, 0, 0)
     * @input alpha = PI/4 and beta = PI/5
     * 
     * @output Vector3D containing the cone apex
     * 
     * @testPassCriteria The returned origin is the same as the user specified origin (with an epsilon of 1e-16 due to
     *                   the machine errors : we check if the origin is indeed the one given at the construction).
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testGetOrigin() {

        // cone parameters
        final Vector3D apex = new Vector3D(1, 2, 3);
        final Vector3D axis = new Vector3D(0, 0, 1);
        final Vector3D axisU = new Vector3D(1, 0, 0);
        final double alpha = FastMath.PI / 4;
        final double beta = FastMath.PI / 5;
        // create cone
        final InfiniteEllipticCone myCone = new InfiniteEllipticCone(apex, axis, axisU, alpha, beta);

        // Test validity of spheroid position using getter
        final Vector3D returnedCenter = myCone.getOrigin();
        Assert.assertEquals(returnedCenter.getX(), apex.getX(), this.machineEpsilon);
        Assert.assertEquals(returnedCenter.getY(), apex.getY(), this.machineEpsilon);
        Assert.assertEquals(returnedCenter.getZ(), apex.getZ(), this.machineEpsilon);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#INFINITE_ELLIPTIC_CONE_PROPS}
     * 
     * @testedMethod {@link InfiniteEllipticCone#getAngleX()}
     * @testedMethod {@link InfiniteEllipticCone#getAngleY()}
     * @testedMethod {@link InfiniteEllipticCone#getApertureX()}
     * @testedMethod {@link InfiniteEllipticCone#getApertureY()}
     * @testedMethod {@link InfiniteEllipticCone#getSemiAxisX()}
     * @testedMethod {@link InfiniteEllipticCone#getSemiAxisY()}
     * 
     * @description Test InfiniteEllipticCone getters.
     * 
     * @input apex = (1, 2, 3)
     * @input axis = (0, 0, 1)
     * @input axisU = (1, 0, 0)
     * @input alpha = PI/4 and beta = PI/5
     * 
     * @output doubles
     * 
     * @testPassCriteria returned values are the same as the specified values (with an epsilon of 1e-16 due to the
     *                   machine errors : we check if the elements are indeed the ones given at the construction).
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testGetters() {

        // cone parameters
        final Vector3D apex = new Vector3D(1, 2, 3);
        final Vector3D axis = new Vector3D(0, 0, 1);
        final Vector3D axisU = new Vector3D(1, 0, 0);
        final double alpha = FastMath.PI / 4;
        final double beta = FastMath.PI / 5;
        // create cone
        final InfiniteEllipticCone myCone = new InfiniteEllipticCone(apex, axis, axisU, alpha, beta);

        Assert.assertEquals(alpha, myCone.getAngleX(), this.machineEpsilon);
        Assert.assertEquals(2 * alpha, myCone.getApertureX(), this.machineEpsilon);
        Assert.assertEquals(MathLib.tan(alpha), myCone.getSemiAxisX(), this.machineEpsilon);
        Assert.assertEquals(beta, myCone.getAngleY(), this.machineEpsilon);
        Assert.assertEquals(2 * beta, myCone.getApertureY(), this.machineEpsilon);
        Assert.assertEquals(MathLib.tan(beta), myCone.getSemiAxisY(), this.machineEpsilon);

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#INFINITE_ELLIPTIC_CONE_BASISTRANSFORMATIONS}
     * 
     * @testedMethod {@link InfiniteEllipticCone#getLocalBasisTransform()}
     * @testedMethod {@link InfiniteEllipticCone#getStandardBasisTransform()}
     * 
     * @description Test Cone basis transformations.
     * 
     * @input apex = (1, 2, 3)
     * @input axis = (4, 5, 6)
     * @input axisU = (1, 0, 0)
     * @input alpha = PI/4 and beta = PI/5
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
    public void testStandardBasisTransformations() {

        // cone parameters
        final Vector3D apex = new Vector3D(1, 2, 3);
        final Vector3D axis = new Vector3D(4, 5, 6);
        final Vector3D axisU = new Vector3D(1, 0, 0);
        final double alpha = FastMath.PI / 4;
        final double beta = FastMath.PI / 5;
        // create cone
        final InfiniteEllipticCone myCone = new InfiniteEllipticCone(apex, axis, axisU, alpha, beta);

        // Get frame
        final Matrix3D myFrame = myCone.getStandardBasisTransform();

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
        final Matrix3D myFrameInv = myCone.getLocalBasisTransform();
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
     * @testedFeature {@link features#INFINITE_ELLIPTIC_CONE_BASISTRANSFORMATIONS}
     * 
     * @testedMethod {@link InfiniteEllipticCone#getVectorialLocalExpression(Vector3D)}
     * @testedMethod {@link InfiniteEllipticCone#getVectorialStandardExpression(Vector3D)}
     * 
     * @description Test cone basis transformations.
     * 
     * @input Vector3D
     * 
     * @output Vector3D expressed in target basis
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
    public void testGetVectorialLocalExpression() {

        // Cone parameters
        Vector3D apex;
        Vector3D axis;
        Vector3D axisU;
        double alpha;
        double beta;
        InfiniteEllipticCone myCone;

        apex = new Vector3D(0, 0, 0);
        axis = new Vector3D(0, 0, 1);
        axisU = new Vector3D(0, 1, 0);
        alpha = FastMath.PI / 4;
        beta = FastMath.PI / 4;
        myCone = new InfiniteEllipticCone(apex, axis, axisU, alpha, beta);

        // Point
        final Vector3D myPoint = new Vector3D(1, 0, 0);
        Vector3D newPoint = myCone.getVectorialLocalExpression(myPoint);
        Assert.assertEquals(0, newPoint.getX(), this.comparisonEpsilon);
        Assert.assertEquals(-1, newPoint.getY(), this.comparisonEpsilon);
        Assert.assertEquals(0, newPoint.getZ(), this.comparisonEpsilon);

        // reverse transformation
        newPoint = myCone.getVectorialStandardExpression(newPoint);
        Assert.assertEquals(myPoint.getX(), newPoint.getX(), this.comparisonEpsilon);
        Assert.assertEquals(myPoint.getY(), newPoint.getY(), this.comparisonEpsilon);
        Assert.assertEquals(myPoint.getZ(), newPoint.getZ(), this.comparisonEpsilon);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#INFINITE_ELLIPTIC_CONE_BASISTRANSFORMATIONS}
     * 
     * @testedMethod {@link InfiniteEllipticCone#getAffineLocalExpression(Vector3D)}
     * @testedMethod {@link InfiniteEllipticCone#getAffineStandardExpression(Vector3D)}
     * 
     * @description Test cone basis transformations.
     * 
     * @input Vector3D
     * 
     * @output Vector3D expressed in target basis
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
    public void testGetAffineLocalExpression() {

        // Cone parameters
        Vector3D apex;
        Vector3D axis;
        Vector3D axisU;
        double alpha;
        double beta;
        InfiniteEllipticCone myCone;

        apex = new Vector3D(0, 0, 2);
        axis = new Vector3D(0, 0, 1);
        axisU = new Vector3D(0, 1, 0);
        alpha = FastMath.PI / 4;
        beta = FastMath.PI / 4;
        myCone = new InfiniteEllipticCone(apex, axis, axisU, alpha, beta);

        // Point
        final Vector3D myPoint = new Vector3D(1, 0, 0);
        Vector3D newPoint = myCone.getAffineLocalExpression(myPoint);
        Assert.assertEquals(0, newPoint.getX(), this.comparisonEpsilon);
        Assert.assertEquals(-1, newPoint.getY(), this.comparisonEpsilon);
        Assert.assertEquals(-2, newPoint.getZ(), this.comparisonEpsilon);

        // reverse transformation
        newPoint = myCone.getAffineStandardExpression(newPoint);
        Assert.assertEquals(myPoint.getX(), newPoint.getX(), this.comparisonEpsilon);
        Assert.assertEquals(myPoint.getY(), newPoint.getY(), this.comparisonEpsilon);
        Assert.assertEquals(myPoint.getZ(), newPoint.getZ(), this.comparisonEpsilon);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#INFINITE_ELLIPTIC_CONE_INTERSECTIONS}
     * 
     * @testedMethod {@link InfiniteEllipticCone#intersects(Line)}
     * 
     * @description Test cone intersections algorithm.
     * 
     * @input Line
     * 
     * @output Boolean set to true if the user specified line intersects the cone
     * 
     * @testPassCriteria The expected result is the same as the predicted one.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testIntersects() {

        // Cone parameters
        Vector3D apex;
        Vector3D axis;
        Vector3D axisU;
        double alpha;
        double beta;
        InfiniteEllipticCone myCone;

        apex = new Vector3D(0, 0, 2);
        axis = new Vector3D(0, 0, 1);
        axisU = new Vector3D(0, 1, 0);
        alpha = FastMath.PI / 4;
        beta = FastMath.PI / 4;
        myCone = new InfiniteEllipticCone(apex, axis, axisU, alpha, beta);

        // Line parameters
        Vector3D lineOrg = new Vector3D(0, 0, 0);
        Vector3D lineDir = new Vector3D(0, 1, 0);
        Line myLine = new Line(lineOrg, lineOrg.add(lineDir));

        boolean result = myCone.intersects(myLine);
        Assert.assertFalse(result);

        // Line parameters
        lineOrg = new Vector3D(0, 0, 4);
        lineDir = new Vector3D(0, 1, 0);
        myLine = new Line(lineOrg, lineOrg.add(lineDir));

        result = myCone.intersects(myLine);
        Assert.assertTrue(result);

        // Line parameters
        lineOrg = new Vector3D(0, 0, 4);
        lineDir = new Vector3D(1, 0, 0);
        myLine = new Line(lineOrg, lineOrg.add(lineDir));

        result = myCone.intersects(myLine);
        Assert.assertTrue(result);

        // Line parameters
        lineOrg = new Vector3D(0, 0, 2);
        lineDir = new Vector3D(1, 0, 0);
        myLine = new Line(lineOrg, lineOrg.add(lineDir));

        result = myCone.intersects(myLine);
        Assert.assertTrue(result);

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#INFINITE_ELLIPTIC_CONE_SHAPE}
     * 
     * @testedMethod {@link InfiniteEllipticCone#isStrictlyInside(Vector3D)}
     * 
     * @description Test if a point is strictly inside the cone.
     * 
     * @input Vector3D
     * 
     * @output boolean true if is strictly inside false otherwise
     * 
     * @testPassCriteria The expected result is the same as the predicted one.
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public void testIsStrictlyInside() {
        // Cone parameters
        Vector3D apex;
        Vector3D axis;
        Vector3D axisU;
        double alpha;
        double beta;
        InfiniteEllipticCone myCone;

        apex = new Vector3D(0, 0, 0);
        axis = new Vector3D(0, 0, 1);
        axisU = new Vector3D(1, 0, 0);
        alpha = FastMath.PI / 4;
        beta = FastMath.PI / 3;
        myCone = new InfiniteEllipticCone(apex, axis, axisU, alpha, beta);

        Vector3D point = new Vector3D(MathLib.cos(FastMath.PI / 4), 0, MathLib.sin(FastMath.PI / 4));
        Assert.assertFalse(myCone.isStrictlyInside(point));

        point = new Vector3D(MathLib.cos(FastMath.PI / 4), 0, MathLib.sin(FastMath.PI / 4) + .1);
        Assert.assertTrue(myCone.isStrictlyInside(point));

        point = new Vector3D(MathLib.cos(FastMath.PI / 4), 0, MathLib.sin(FastMath.PI / 4) - .1);
        Assert.assertFalse(myCone.isStrictlyInside(point));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#INFINITE_ELLIPTIC_CONE_INTERSECTIONS}
     * 
     * @testedMethod {@link InfiniteEllipticCone#getIntersectionPoints(Line)}
     * 
     * @description Test cone intersections algorithm.
     * 
     * @input Line
     * 
     * @output Vector3D[] array with intersections points. Empty if none.
     * 
     * @testPassCriteria The expected result is the same as the predicted one with an epsilon of 1e-14 due to the
     *                   computation errors.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testIntersections() {

        // Cone parameters
        Vector3D apex;
        Vector3D axis;
        Vector3D axisU;
        double alpha;
        double beta;
        InfiniteEllipticCone myCone;

        apex = new Vector3D(0, 0, 2);
        axis = new Vector3D(0, 0, 1);
        axisU = new Vector3D(0, 1, 0);
        alpha = FastMath.PI / 4;
        beta = FastMath.PI / 4;
        myCone = new InfiniteEllipticCone(apex, axis, axisU, alpha, beta);

        // Line parameters
        Vector3D lineOrg = new Vector3D(0, 1, 0);
        Vector3D lineDir = new Vector3D(0, 0, 1);
        Line myLine = new Line(lineOrg, lineOrg.add(lineDir));
        boolean result = myCone.intersects(myLine);
        Assert.assertEquals(result, true);
        Vector3D[] result1 = myCone.getIntersectionPoints(myLine);
        Assert.assertEquals(1, result1.length);
        Assert.assertEquals(0, result1[0].getX(), this.comparisonEpsilon);
        Assert.assertEquals(1, result1[0].getY(), this.comparisonEpsilon);
        Assert.assertEquals(3, result1[0].getZ(), this.comparisonEpsilon);

        // Line parameters
        lineOrg = new Vector3D(0, 0, 4);
        lineDir = new Vector3D(0, 1, 0);
        myLine = new Line(lineOrg, lineOrg.add(lineDir));
        result = myCone.intersects(myLine);
        Assert.assertEquals(result, true);
        result1 = myCone.getIntersectionPoints(myLine);

        Assert.assertEquals(2, result1.length);

        Assert.assertEquals(0, result1[0].getX(), this.comparisonEpsilon);
        Assert.assertEquals(2, result1[0].getY(), this.comparisonEpsilon);
        Assert.assertEquals(4, result1[0].getZ(), this.comparisonEpsilon);
        Assert.assertEquals(0, result1[1].getX(), this.comparisonEpsilon);
        Assert.assertEquals(-2, result1[1].getY(), this.comparisonEpsilon);
        Assert.assertEquals(4, result1[1].getZ(), this.comparisonEpsilon);

        apex = new Vector3D(0, 0, 0);
        axis = new Vector3D(0, 0, 1);
        axisU = new Vector3D(0, 1, 0);
        alpha = FastMath.PI / 8;
        beta = FastMath.PI / 8;
        myCone = new InfiniteEllipticCone(apex, axis, axisU, alpha, beta);
        // Line parameters
        lineOrg = new Vector3D(1, 1, 0);
        lineDir = new Vector3D(0, 1, 0);
        myLine = new Line(lineOrg, lineOrg.add(lineDir));
        result = myCone.intersects(myLine);
        Assert.assertFalse(result);
        result1 = myCone.getIntersectionPoints(myLine);
        Assert.assertEquals(0, result1.length);

        apex = new Vector3D(0, 0, 0);
        axis = new Vector3D(0, 0, 1);
        axisU = new Vector3D(0, 1, 0);
        alpha = FastMath.PI / 8;
        beta = FastMath.PI / 8;
        myCone = new InfiniteEllipticCone(apex, axis, axisU, alpha, beta);
        // Line parameters
        lineOrg = new Vector3D(0, 0.5, 1);
        lineDir = new Vector3D(-.5, 1, 0);
        myLine = new Line(lineOrg, lineOrg.add(lineDir));
        result = myCone.intersects(myLine);
        Assert.assertTrue(result);
        result1 = myCone.getIntersectionPoints(myLine);
        Assert.assertEquals(2, result1.length);

        // Line parameters
        lineOrg = new Vector3D(0, 0.5, 1);
        lineDir = new Vector3D(.5, 1, 0);
        myLine = new Line(lineOrg, lineOrg.add(lineDir));
        result = myCone.intersects(myLine);
        Assert.assertTrue(result);
        result1 = myCone.getIntersectionPoints(myLine);
        Assert.assertEquals(2, result1.length);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#INFINITE_ELLIPTIC_CONE_DISTANCES}
     * 
     * @testedMethod {@link InfiniteEllipticCone#closestPointTo(Vector3D)}
     * 
     * @description Test cone distance computation algorithms
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
    public void testClosestPointToPointPart1() {

        // Cone parameters
        Vector3D apex;
        Vector3D axis;
        Vector3D axisU;
        double alpha;
        double beta;
        InfiniteEllipticCone myCone;

        alpha = FastMath.PI / 4;
        beta = FastMath.PI / 6;
        myCone = new InfiniteEllipticCone(new Vector3D(0, 0, 0), new Vector3D(0, 0, 1), new Vector3D(1, 0, 0),
            alpha, beta);

        // Point on axis of cone (z<0)
        Vector3D myPoint = new Vector3D(0, 0, -2);
        Vector3D result = myCone.closestPointTo(myPoint);
        Vector3D exp = new Vector3D(0, 0, 0);
        Assert.assertEquals(exp.getX(), result.getX(), this.comparisonEpsilon);
        Assert.assertEquals(exp.getY(), result.getY(), this.comparisonEpsilon);
        Assert.assertEquals(exp.getZ(), result.getZ(), this.comparisonEpsilon);

        // Point on axis of cone (z>0)
        myPoint = new Vector3D(0, 0, 2);
        result = myCone.closestPointTo(myPoint);
        double h = 2 - 2 * MathLib.tan(beta) * MathLib.cos(beta) * MathLib.sin(beta);
        exp = new Vector3D(0, h * MathLib.tan(beta), h);
        Assert.assertEquals(exp.getX(), result.getX(), this.comparisonEpsilon);
        Assert.assertEquals(exp.getY(), result.getY(), this.comparisonEpsilon);
        Assert.assertEquals(exp.getZ(), result.getZ(), this.comparisonEpsilon);

        alpha = FastMath.PI / 6;
        beta = FastMath.PI / 4;
        myCone = new InfiniteEllipticCone(new Vector3D(0, 0, 0), new Vector3D(0, 0, 1), new Vector3D(1, 0, 0),
            alpha, beta);
        // Point on axis of cone (z>0)
        myPoint = new Vector3D(0, 0, 2);
        result = myCone.closestPointTo(myPoint);
        h = 2 - 2 * MathLib.tan(alpha) * MathLib.cos(alpha) * MathLib.sin(alpha);
        exp = new Vector3D(h * MathLib.tan(alpha), 0, h);
        Assert.assertEquals(exp.getX(), result.getX(), this.comparisonEpsilon);
        Assert.assertEquals(exp.getY(), result.getY(), this.comparisonEpsilon);
        Assert.assertEquals(exp.getZ(), result.getZ(), this.comparisonEpsilon);

        myCone = new InfiniteEllipticCone(new Vector3D(0, 0, 0), new Vector3D(0, 0, 1), new Vector3D(1, 0, 0),
            FastMath.PI / 4, FastMath.PI / 6);

        // Point on apex
        myPoint = new Vector3D(0, 0, 0);
        result = myCone.closestPointTo(myPoint);
        exp = new Vector3D(0, 0, 0);
        Assert.assertEquals(exp.getX(), result.getX(), this.comparisonEpsilon);
        Assert.assertEquals(exp.getY(), result.getY(), this.comparisonEpsilon);
        Assert.assertEquals(exp.getZ(), result.getZ(), this.comparisonEpsilon);

        // point on Oxz plane
        myPoint = new Vector3D(2, 0, 1);
        result = myCone.closestPointTo(myPoint);
        exp = new Vector3D(1 + MathLib.sin(FastMath.PI / 4) * MathLib.sin(FastMath.PI / 4), 0, 1
                + MathLib.sin(FastMath.PI / 4)
                * MathLib.cos(FastMath.PI / 4));
        Assert.assertEquals(exp.getX(), result.getX(), this.comparisonEpsilon);
        Assert.assertEquals(exp.getY(), result.getY(), this.comparisonEpsilon);
        Assert.assertEquals(exp.getZ(), result.getZ(), this.comparisonEpsilon);

        // point on Oyz plane
        myPoint = new Vector3D(0, 2, 1);
        result = myCone.closestPointTo(myPoint);
        exp = new Vector3D(0, 1 / MathLib.sqrt(3) + (2 - 1 / MathLib.sqrt(3)) / 2 * MathLib.sin(FastMath.PI / 6), 1
                + (2 - 1 / MathLib.sqrt(3)) / 2 * MathLib.cos(FastMath.PI / 6));
        Assert.assertEquals(exp.getX(), result.getX(), this.comparisonEpsilon);
        Assert.assertEquals(exp.getY(), result.getY(), this.comparisonEpsilon);
        Assert.assertEquals(exp.getZ(), result.getZ(), this.comparisonEpsilon);

        // point on Oxz plane
        myPoint = new Vector3D(1, 0, 0);
        result = myCone.closestPointTo(myPoint);
        exp = new Vector3D(MathLib.sin(FastMath.PI / 4) * MathLib.sin(FastMath.PI / 4), 0,
            MathLib.sin(FastMath.PI / 4)
                    * MathLib.cos(FastMath.PI / 4));
        Assert.assertEquals(exp.getX(), result.getX(), this.comparisonEpsilon);
        Assert.assertEquals(exp.getY(), result.getY(), this.comparisonEpsilon);
        Assert.assertEquals(exp.getZ(), result.getZ(), this.comparisonEpsilon);

        /** tests */
        // Point next to cone
        apex = new Vector3D(3, 0, 0);
        axis = new Vector3D(0, 0, 1);
        axisU = new Vector3D(1, 0, 0);
        alpha = FastMath.PI / 4;
        beta = FastMath.PI / 6;
        myCone = new InfiniteEllipticCone(apex, axis, axisU, alpha, beta);

        // point on Oxz plane
        myPoint = new Vector3D(15, 0, 0);
        result = myCone.closestPointTo(myPoint);
        exp = new Vector3D(9, 0, 6);
        Assert.assertEquals(exp.getX(), result.getX(), this.comparisonEpsilon);
        Assert.assertEquals(exp.getY(), result.getY(), this.comparisonEpsilon);
        Assert.assertEquals(exp.getZ(), result.getZ(), this.comparisonEpsilon);

        // point on Oxz plane
        myPoint = new Vector3D(-15, 0, 0);
        result = myCone.closestPointTo(myPoint);
        exp = new Vector3D(-6, 0, 9);
        Assert.assertEquals(exp.getX(), result.getX(), this.comparisonEpsilon);
        Assert.assertEquals(exp.getY(), result.getY(), this.comparisonEpsilon);
        Assert.assertEquals(exp.getZ(), result.getZ(), this.comparisonEpsilon);

        // point on cone
        myPoint = new Vector3D(-6, 0, 9);
        result = myCone.closestPointTo(myPoint);
        exp = new Vector3D(-6, 0, 9);
        Assert.assertEquals(exp.getX(), result.getX(), this.comparisonEpsilon);
        Assert.assertEquals(exp.getY(), result.getY(), this.comparisonEpsilon);
        Assert.assertEquals(exp.getZ(), result.getZ(), this.comparisonEpsilon);

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#INFINITE_ELLIPTIC_CONE_DISTANCES}
     * 
     * @testedMethod {@link InfiniteEllipticCone#closestPointTo(Vector3D)}
     * 
     * @description Test cone distance computation algorithms. Test part 2 only to keep part 1 under 100 lines.
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
    public void testClosestPointToPointPart2() {
        // cone randomly oriented
        final Vector3D apex = new Vector3D(15, 2, 6);
        final Vector3D axis = new Vector3D(-2, 5, 1);
        final Vector3D axisU = new Vector3D(1, .5, 15.88);
        final double alpha = FastMath.PI / 4;
        final double beta = FastMath.PI / 6;
        final InfiniteEllipticCone myCone = new InfiniteEllipticCone(apex, axis, axisU, alpha, beta);
        // point on Oyz plane
        final Vector3D myPoint = myCone.getAffineStandardExpression(new Vector3D(0, 2, 1));
        final Vector3D result = myCone.closestPointTo(myPoint);
        final Vector3D exp = myCone.getAffineStandardExpression(new Vector3D(0, 1 / MathLib.sqrt(3)
                + (2 - 1 / MathLib.sqrt(3)) / 2 * MathLib.sin(FastMath.PI / 6), 1 + (2 - 1 / MathLib.sqrt(3)) / 2
                * MathLib.cos(FastMath.PI / 6)));
        Assert.assertEquals(exp.getX(), result.getX(), this.comparisonEpsilon);
        Assert.assertEquals(exp.getY(), result.getY(), this.comparisonEpsilon);
        Assert.assertEquals(exp.getZ(), result.getZ(), this.comparisonEpsilon);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#INFINITE_ELLIPTIC_CONE_DISTANCES}
     * 
     * @testedMethod {@link InfiniteEllipticCone#distanceTo(Vector3D)}
     * 
     * @description Test cone distance computation algorithms : inside or outside of the cone?
     * 
     * @input Vector3D
     * 
     * @output boolean
     * 
     * @testPassCriteria The point is in the predicted zone (inside or outside).
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testDistanceToPoint() {

        // Cone parameters
        Vector3D apex;
        Vector3D axis;
        Vector3D axisU;
        double alpha;
        double beta;
        InfiniteEllipticCone myCone;

        apex = new Vector3D(0, 0, 0);
        axis = new Vector3D(0, 0, 1);
        axisU = new Vector3D(1, 0, 0);
        alpha = FastMath.PI / 4;
        beta = FastMath.PI / 6;
        myCone = new InfiniteEllipticCone(apex, axis, axisU, alpha, beta);

        // Point on axis of cone (z>0)
        Vector3D myPoint = new Vector3D(0, 0, 2);
        boolean result = myCone.distanceTo(myPoint) < 0;
        Assert.assertTrue(result);

        // Point on apex
        myPoint = new Vector3D(0, 0, 0);
        result = myCone.distanceTo(myPoint) < 0;
        Assert.assertFalse(result);

        // Point on axis of cone (z<0)
        myPoint = new Vector3D(0, 0, -2);
        result = myCone.distanceTo(myPoint) > 0;
        Assert.assertTrue(result);

        // Point on Oxz
        myPoint = new Vector3D(2, 0, 0);
        result = myCone.distanceTo(myPoint) > 0;
        Assert.assertTrue(result);

        // Point on Oxz very high
        myPoint = new Vector3D(2, 0, 20);
        result = myCone.distanceTo(myPoint) < 0;
        Assert.assertTrue(result);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#INFINITE_ELLIPTIC_CONE_DISTANCES}
     * 
     * @testedMethod {@link InfiniteEllipticCone#closestPointTo(Line)}
     * @testedMethod {@link InfiniteEllipticCone#distanceTo(Line)}
     * 
     * @description Test cone distance computation algorithms
     * 
     * @input Line
     * 
     * @output Vector3D[] containing the closest computed point
     * 
     * @testPassCriteria The expected result is the same as the predicted one with an epsilon of 1e-14 due to the
     *                   computation errors.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testClosestPointToLine() {

        // Cone
        Vector3D apex = new Vector3D(0, 0, 0);
        Vector3D axis = new Vector3D(0, 0, 1);
        Vector3D axisU = new Vector3D(1, 0, 0);
        double alpha = FastMath.PI / 4;
        double beta = FastMath.PI / 6;
        InfiniteEllipticCone myCone = new InfiniteEllipticCone(apex, axis, axisU, alpha, beta);

        // line doesnt intersect
        Vector3D myPoint = new Vector3D(2, 0, 1);
        Vector3D myDir = new Vector3D(0, 1, 0);
        Line myLine = new Line(myPoint, myPoint.add(myDir));
        Vector3D[] result = myCone.closestPointTo(myLine);
        Vector3D expLinePoint = myPoint;
        Vector3D exp = new Vector3D(1 + MathLib.sin(alpha) * MathLib.sin(alpha), 0, 1 + MathLib.sin(alpha)
                * MathLib.cos(alpha));
        Assert.assertEquals(expLinePoint.getX(), result[0].getX(), this.comparisonEpsilon);
        Assert.assertEquals(expLinePoint.getY(), result[0].getY(), this.comparisonEpsilon);
        Assert.assertEquals(expLinePoint.getZ(), result[0].getZ(), this.comparisonEpsilon);
        Assert.assertEquals(exp.getX(), result[1].getX(), this.comparisonEpsilon);
        Assert.assertEquals(exp.getY(), result[1].getY(), this.comparisonEpsilon);
        Assert.assertEquals(exp.getZ(), result[1].getZ(), this.comparisonEpsilon);
        double myDist = myPoint.subtract(exp).getNorm();
        Assert.assertEquals(myDist, myCone.distanceTo(myLine), this.comparisonEpsilon);

        // line does intersect
        myPoint = new Vector3D(0, 0, 0);
        myDir = new Vector3D(0, 1, 0);
        myLine = new Line(myPoint, myPoint.add(myDir));
        result = myCone.closestPointTo(myLine);
        expLinePoint = myPoint;
        exp = new Vector3D(0, 0, 0);
        Assert.assertEquals(expLinePoint.getX(), result[0].getX(), this.comparisonEpsilon);
        Assert.assertEquals(expLinePoint.getY(), result[0].getY(), this.comparisonEpsilon);
        Assert.assertEquals(expLinePoint.getZ(), result[0].getZ(), this.comparisonEpsilon);
        Assert.assertEquals(exp.getX(), result[1].getX(), this.comparisonEpsilon);
        Assert.assertEquals(exp.getY(), result[1].getY(), this.comparisonEpsilon);
        Assert.assertEquals(exp.getZ(), result[1].getZ(), this.comparisonEpsilon);
        myDist = myPoint.subtract(exp).getNorm();
        Assert.assertEquals(myDist, myCone.distanceTo(myLine), this.comparisonEpsilon);

        Vector3D expConePoint;

        // Cone randomly oriented
        apex = new Vector3D(1, -5, 3);
        axis = new Vector3D(-1.88, 2.5, -4);
        axisU = axis.orthogonal();
        alpha = FastMath.PI / 4;
        beta = FastMath.PI / 6;
        myCone = new InfiniteEllipticCone(apex, axis, axisU, alpha, beta);

        // line doesnt intersect
        myPoint = myCone.getAffineStandardExpression(new Vector3D(2, 0, 1));
        myDir = myCone.getVectorialStandardExpression(new Vector3D(0, 1, 0));
        myLine = new Line(myPoint, myPoint.add(myDir));

        // compute
        result = myCone.closestPointTo(myLine);

        // expectations
        expLinePoint = myPoint;
        expConePoint = myCone.getAffineStandardExpression(new Vector3D(1 + MathLib.sin(alpha) * MathLib.sin(alpha),
            0, 1
                    + MathLib.sin(alpha) * MathLib.cos(alpha)));

        // asserts
        Assert.assertEquals(expLinePoint.getX(), result[0].getX(), this.comparisonEpsilon);
        Assert.assertEquals(expLinePoint.getY(), result[0].getY(), this.comparisonEpsilon);
        Assert.assertEquals(expLinePoint.getZ(), result[0].getZ(), this.comparisonEpsilon);
        Assert.assertEquals(expConePoint.getX(), result[1].getX(), this.comparisonEpsilon);
        Assert.assertEquals(expConePoint.getY(), result[1].getY(), this.comparisonEpsilon);
        Assert.assertEquals(expConePoint.getZ(), result[1].getZ(), this.comparisonEpsilon);

        myDist = myPoint.subtract(expConePoint).getNorm();
        Assert.assertEquals(myDist, myCone.distanceTo(myLine), this.comparisonEpsilon);

        // FAILED TEST by Réjane - Bug corrected
        final Vector3D ConeOrig = new Vector3D(5, 2, 0);
        final Vector3D ConeDirection = new Vector3D(-1, 3, 5);
        final Vector3D UAxis = new Vector3D(2, 5, -1);
        final double angleA = FastMath.PI / 4;
        final double angleB = FastMath.PI / 8;

        final InfiniteEllipticCone monCone = new InfiniteEllipticCone(ConeOrig, ConeDirection, UAxis,
            angleA, angleB);
        final Line maDroite1 = new Line(new Vector3D(5, 1, 2), new Vector3D(5, 1, 2).add(new Vector3D(3, 2, 0)));

        // pas de points d'intersection
        final Vector3D[] PointsIntersection = monCone.getIntersectionPoints(maDroite1);
        Assert.assertEquals(PointsIntersection.length, 0);
        Assert.assertEquals(true, monCone.distanceTo(maDroite1) > 0);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#INFINITE_ELLIPTIC_CONE_TO_STRING}
     * 
     * @testedMethod {@link InfiniteEllipticCone#toString()}
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

        // Cone
        final Vector3D apex = new Vector3D(0, 0, 0);
        final Vector3D axis = new Vector3D(0, 0, 1);
        final Vector3D axisU = new Vector3D(1, 0, 0);
        final double alpha = FastMath.PI / 4;
        final double beta = FastMath.PI / 6;
        final InfiniteEllipticCone myCone = new InfiniteEllipticCone(apex, axis, axisU, alpha, beta);

        // string creation
        final String result = myCone.toString();

        final String expected =
            "InfiniteEllipticCone{Origin{0; 0; 0},Direction{0; 0; 1},U vector{1; 0; 0},Angle on U{0.7853981633974483},Angle on V{0.5235987755982988}}";
        Assert.assertEquals(expected, result);
    }
}
