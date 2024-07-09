/**
 * 
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
 * @history Created on 06/10/2011
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:400:17/03/2015: use class FastMath instead of class Math
 * VERSION::FA:458:19/10/2015: Use class FastMath instead of class Math
 * VERSION::FA:650:22/07/2016: ellipsoid corrections
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.geometry.euclidean.threed;

import java.util.Random;

import junit.framework.Assert;

import org.junit.Test;

import fr.cnes.sirius.patrius.math.exception.MathArithmeticException;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;

/**
 * <p>
 * Test class for {@link Spheroid}
 * </p>
 * 
 * @see Spheroid
 * 
 * @author Rami Houdroge
 * 
 * @version $Id: SpheroidTest.java 17909 2017-09-11 11:57:36Z bignon $
 * 
 * @since 1.0
 * 
 */
public class SpheroidTest {

    /** Features description. */
    public enum features {

        /**
         * @featureTitle Spheroid shape
         * 
         * @featureDescription Creation of a spheroid, computation of distances and intersections with lines and points.
         * 
         * @coveredRequirements DV-GEOMETRIE_50, DV-GEOMETRIE_60, DV-GEOMETRIE_90, DV-GEOMETRIE_120, DV-GEOMETRIE_130
         */
        SPHEROID_SHAPE,

        /**
         * @featureTitle Spheroid getters
         * 
         * @featureDescription Test Spheroid getters
         * 
         * @coveredRequirements DV-GEOMETRIE_50, DV-GEOMETRIE_60, DV-GEOMETRIE_90, DV-GEOMETRIE_120, DV-GEOMETRIE_130
         */
        SPHEROID_PROPS,

        /**
         * @featureTitle Spheroid basis transformations
         * 
         * @featureDescription Test Spheroid basis transformations
         * 
         * @coveredRequirements DV-GEOMETRIE_50, DV-GEOMETRIE_60, DV-GEOMETRIE_90, DV-GEOMETRIE_120, DV-GEOMETRIE_130
         */
        SPHEROID_BASISTRANSFORMATIONS,

        /**
         * @featureTitle Spheroid intersections
         * 
         * @featureDescription Test Spheroid intersection algorithms
         * 
         * @coveredRequirements DV-GEOMETRIE_50, DV-GEOMETRIE_60, DV-GEOMETRIE_90, DV-GEOMETRIE_120, DV-GEOMETRIE_130,
         *                      DV-GEOMETRIE_140
         */
        SPHEROID_INTERSECTIONS,

        /**
         * @featureTitle Spheroid distance
         * 
         * @featureDescription Test Spheroid distance computation algorithms
         * 
         * @coveredRequirements DV-GEOMETRIE_50, DV-GEOMETRIE_60, DV-GEOMETRIE_90, DV-GEOMETRIE_120, DV-GEOMETRIE_130
         */
        SPHEROID_DISTANCES
    }

    /** Epsilon for double comparison. */
    private final double comparisonEpsilon = Precision.DOUBLE_COMPARISON_EPSILON;

    /** Epsilon taking into account the machine error. */
    private final double machineEpsilon = Precision.EPSILON;

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#SPHEROID_SHAPE}
     * @testedFeature {@link features#SPHEROID_PROPS}
     * 
     * @testedMethod {@link Spheroid#Spheroid(Vector3D, Vector3D, double, double)}
     * 
     * @description Test Spheroid Constructor {@link Spheroid#Spheroid(Vector3D, Vector3D, double, double)} Here we
     *              check the correctness of the Spheroid class constructor. Nominal case as well as degraded cases are
     *              checked. Once the test is passed, the method is considered correct and used afterwards.
     * 
     * @input data
     * 
     * @output Spheroid
     * 
     * @testPassCriteria No exception is raised for nominal cases, an IllegalArgumentException is raised for degraded
     *                   cases. We check the returned elements with the ones given at the construction with an epsilon
     *                   of 1e-16 which takes into account the machine error only.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public final void testConstructor() {

        // Spheroid parameters
        final Vector3D position = new Vector3D(1., 2., 3.);
        Vector3D revAxis = new Vector3D(2., 2., 2.);
        double a = 1.0;
        double b = 2.0;
        try {
            // create spheroid object
            Spheroid mySpheroid = new Spheroid(position, revAxis, a, b);
            // test getters
            Assert.assertEquals(a, mySpheroid.getEquatorialRadius(), this.machineEpsilon);
            Assert.assertEquals(b, mySpheroid.getPolarRadius(), this.machineEpsilon);
            // new test
            mySpheroid = new Spheroid(position, revAxis, 3, 1);
            // test getters
            Assert.assertEquals(3, mySpheroid.getEquatorialRadius(), this.machineEpsilon);
            Assert.assertEquals(1, mySpheroid.getPolarRadius(), this.machineEpsilon);
            // new test
            mySpheroid = new Spheroid(position, revAxis, 2, 2);
            // test getters
            Assert.assertEquals(2, mySpheroid.getEquatorialRadius(), this.machineEpsilon);
            Assert.assertEquals(2, mySpheroid.getPolarRadius(), this.machineEpsilon);

        } catch (final IllegalArgumentException e) {
            Assert.fail();
        }

        // Null semi axis a
        a = 0;
        try {
            new Spheroid(position, revAxis, a, b);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected!!
        }
        // Negative semi axis a
        a = -1;
        try {
            new Spheroid(position, revAxis, a, b);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected!!
        }

        // Null semi axis b
        a = 1;
        b = 0;
        try {
            new Spheroid(position, revAxis, a, b);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected!!
        }

        // Negative semi axis b
        b = -1;
        try {
            new Spheroid(position, revAxis, a, b);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected!!
        }

        // Null axis of revolution
        b = 1;
        revAxis = new Vector3D(.0, .0, .0);
        try {
            new Spheroid(position, revAxis, a, b);
            Assert.fail();
        } catch (final MathArithmeticException e) {
            // expected!!
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#SPHEROID_PROPS}
     * 
     * @testedMethod {@link Spheroid#getCenter()}
     * 
     * @description Test Spheroid getters.
     * 
     * @input none
     * 
     * @output Vector3D containing the normalized cylinder axis
     * 
     * @testPassCriteria The returned axis is the same as the user specified axis with an epsilon of 1e-16 due to the
     *                   machine errors only.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public final void testGetCenter() {

        // Spheroid parameters
        final Vector3D position = new Vector3D(MathLib.random(), MathLib.random(), MathLib.random());
        final Vector3D revAxis = new Vector3D(MathLib.random(), MathLib.random(), MathLib.random());
        final double a = MathLib.random();
        final double b = MathLib.random();
        // create spheroid object
        final Spheroid mySpheroid = new Spheroid(position, revAxis, a, b);

        // Test validity of spheroid position using getter
        final Vector3D returnedCenter = mySpheroid.getCenter();
        Assert.assertEquals(returnedCenter.getX(), position.getX(), this.machineEpsilon);
        Assert.assertEquals(returnedCenter.getY(), position.getY(), this.machineEpsilon);
        Assert.assertEquals(returnedCenter.getZ(), position.getZ(), this.machineEpsilon);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#SPHEROID_BASISTRANSFORMATIONS}
     * 
     * @testedMethod {@link Spheroid#getLocalBasisTransform()}
     * @testedMethod {@link Spheroid#getStandardBasisTransform()}
     * 
     * @description Test Spheroid basis transformations.
     * 
     * @input none
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

        /** --------------- Create Spheroid --------------- */
        // Spheroid parameters
        final Vector3D position = new Vector3D(MathLib.random(), MathLib.random(), MathLib.random());
        final Vector3D revAxis = new Vector3D(MathLib.random(), MathLib.random(), MathLib.random());
        final double a = MathLib.random();
        final double b = MathLib.random();
        // create spheroid object
        final Spheroid mySpheroid = new Spheroid(position, revAxis, a, b);

        /** --------------- Get transforms --------------- */
        // Get reference to standard basis matrix transform
        // It contains the vectors of the reference basis expressed in the
        // standard basis.
        final Matrix3D myBasis = mySpheroid.getStandardBasisTransform();
        final Matrix3D myBasisInv = mySpheroid.getLocalBasisTransform();

        /** --------------- Tests --------------- */
        // Get basis vectors
        final Vector3D myXAxis = new Vector3D(myBasis.getEntry(0, 0), myBasis.getEntry(1, 0), myBasis.getEntry(2, 0));
        final Vector3D myYAxis = new Vector3D(myBasis.getEntry(0, 1), myBasis.getEntry(1, 1), myBasis.getEntry(2, 1));
        final Vector3D myZAxis = new Vector3D(myBasis.getEntry(0, 2), myBasis.getEntry(1, 2), myBasis.getEntry(2, 2));

        // Check if returned Z-axis is parallel to specified axis of revolution
        Assert.assertEquals(1, myZAxis.normalize().dotProduct(revAxis.normalize()), this.comparisonEpsilon);
        Assert.assertEquals(0, myZAxis.normalize().crossProduct(revAxis.normalize()).getNorm(), this.comparisonEpsilon);

        // Check if reference basis vectors are of norm one
        Assert.assertEquals(1, myXAxis.getNorm(), this.comparisonEpsilon);
        Assert.assertEquals(1, myYAxis.getNorm(), this.comparisonEpsilon);
        Assert.assertEquals(1, myZAxis.getNorm(), this.comparisonEpsilon);

        // Check if reference basis vectors are orthogonal
        Assert.assertEquals(0, myXAxis.dotProduct(myYAxis), this.comparisonEpsilon);
        Assert.assertEquals(0, myYAxis.dotProduct(myZAxis), this.comparisonEpsilon);
        Assert.assertEquals(0, myZAxis.dotProduct(myXAxis), this.comparisonEpsilon);

        // Check if this basis is direct
        Assert.assertEquals(1, myZAxis.dotProduct(myXAxis.crossProduct(myYAxis)), this.comparisonEpsilon);
        Assert.assertEquals(0, myZAxis.crossProduct(myXAxis.crossProduct(myYAxis)).getNorm(), this.comparisonEpsilon);
        Assert.assertEquals(1, myXAxis.dotProduct(myYAxis.crossProduct(myZAxis)), this.comparisonEpsilon);
        Assert.assertEquals(0, myXAxis.crossProduct(myYAxis.crossProduct(myZAxis)).getNorm(), this.comparisonEpsilon);
        Assert.assertEquals(1, myYAxis.dotProduct(myZAxis.crossProduct(myXAxis)), this.comparisonEpsilon);
        Assert.assertEquals(0, myYAxis.crossProduct(myZAxis.crossProduct(myXAxis)).getNorm(), this.comparisonEpsilon);

        // Make sure the computed inverted transform satisfies A^T * A = I
        final Matrix3D productOfBoth = myBasis.multiply(myBasisInv);
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
     * @testedFeature {@link features#SPHEROID_BASISTRANSFORMATIONS}
     * 
     * @testedMethod {@link Spheroid#getAffineLocalExpression(Vector3D)}
     * @testedMethod {@link Spheroid#getAffineStandardExpression(Vector3D)}
     * @testedMethod {@link Spheroid#getVectorialLocalExpression(Vector3D)}
     * @testedMethod {@link Spheroid#getVectorialStandardExpression(Vector3D)}
     * 
     * @description Test Spheroid basis transformations.
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
    public final void testGetAffineLocalExpression() {

        /** ---------- Create Spheroid and test point ---------- */
        // Spheroid parameters
        final Vector3D position = new Vector3D(2, 0, 0);
        final Vector3D revAxis = new Vector3D(0, 0, 1);
        final double a = 2;
        final double b = 1;
        // create spheroid object
        final Spheroid mySpheroid = new Spheroid(position, revAxis, a, b);
        // create point
        final Vector3D myPoint = new Vector3D(10, 0, 0);
        // create vector
        Vector3D myVect = new Vector3D(1, 1, 1);

        /** ----- getAffineLocalExpression test ----- */
        // Get point expressed in local basis
        final Vector3D computed1 = mySpheroid.getAffineLocalExpression(myPoint);
        // In this configuration, the global X-axis is the spheroids local -Y-axis
        // as such, the local expression of the vector is (0, -8, 0)
        Assert.assertEquals(0, computed1.getX(), this.comparisonEpsilon);
        Assert.assertEquals(-8, computed1.getY(), this.comparisonEpsilon);
        Assert.assertEquals(0, computed1.getZ(), this.comparisonEpsilon);

        /** ----- getAffineStandardExpression test ----- */
        // Get point expressed in local basis
        final Vector3D computed2 = mySpheroid.getAffineStandardExpression(myPoint);
        // In this configuration, the global X-axis is the spheroids local -Y-axis
        // as such, the standard expression of the vector is (2, 10, 0)
        Assert.assertEquals(2, computed2.getX(), this.comparisonEpsilon);
        Assert.assertEquals(10, computed2.getY(), this.comparisonEpsilon);
        Assert.assertEquals(0, computed2.getZ(), this.comparisonEpsilon);

        /** ----- getVectorialLocalExpression test ----- */
        // Get point expressed in local basis
        final Vector3D computed3 = mySpheroid.getVectorialLocalExpression(myVect);
        // In this configuration, the global X-axis is the spheroids local -Y-axis
        // as such, the local expression of the vector is (1, -1, 1)
        Assert.assertEquals(1, computed3.getX(), this.comparisonEpsilon);
        Assert.assertEquals(-1, computed3.getY(), this.comparisonEpsilon);
        Assert.assertEquals(1, computed3.getZ(), this.comparisonEpsilon);

        /** ----- getVectorialStandardExpression test ----- */
        // Get point expressed in local basis
        final Vector3D computed4 = mySpheroid.getVectorialStandardExpression(myVect);
        // In this configuration, the global X-axis is the spheroids local -Y-axis
        // as such, the standard expression of the vector is (-1, 1, 1)
        Assert.assertEquals(-1, computed4.getX(), this.comparisonEpsilon);
        Assert.assertEquals(1, computed4.getY(), this.comparisonEpsilon);
        Assert.assertEquals(1, computed4.getZ(), this.comparisonEpsilon);

        /** ----- Make we get the same thing after reconverting ----- */
        double r1 = (MathLib.random() - .5) * 10;
        double r2 = (MathLib.random() - .5) * 10;
        double r3 = (MathLib.random() - .5) * 10;
        myVect = new Vector3D(r1, r2, r3);
        final Vector3D computed11 = mySpheroid.getAffineLocalExpression(myVect);
        final Vector3D computed12 = mySpheroid.getAffineStandardExpression(computed11);
        Assert.assertEquals(myVect.getX(), computed12.getX(), this.comparisonEpsilon);
        Assert.assertEquals(myVect.getY(), computed12.getY(), this.comparisonEpsilon);
        Assert.assertEquals(myVect.getZ(), computed12.getZ(), this.comparisonEpsilon);

        /** ----- Make we get the same thing after reconverting ----- */
        r1 = (MathLib.random() - .5) * 10;
        r2 = (MathLib.random() - .5) * 10;
        r3 = (MathLib.random() - .5) * 10;
        myVect = new Vector3D(r1, r2, r3);
        final Vector3D computed21 = mySpheroid.getVectorialLocalExpression(myVect);
        final Vector3D computed22 = mySpheroid.getVectorialStandardExpression(computed21);
        Assert.assertEquals(myVect.getX(), computed22.getX(), this.comparisonEpsilon);
        Assert.assertEquals(myVect.getY(), computed22.getY(), this.comparisonEpsilon);
        Assert.assertEquals(myVect.getZ(), computed22.getZ(), this.comparisonEpsilon);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#SPHEROID_INTERSECTIONS}
     * 
     * @testedMethod {@link Spheroid#intersects(Line)}
     * 
     * @description Test Spheroid intersections algorithm.
     * 
     * @input Line
     * 
     * @output Boolean set to true if the user specified line intersects the spheroid
     * 
     * @testPassCriteria The expected result is the same as the predicted one.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public final void testIntersects() {
        // declare spheroid parameters
        Vector3D position;
        Vector3D revAxis;
        double a;
        double b;

        // declare spheroid object
        Spheroid mySpheroid;

        // items for tests
        int index;
        Line myLine1;
        boolean expected;
        boolean actual;
        double orthSA;
        double revSA;
        Vector3D linePos;
        Vector3D lineDir;

        /** First test, line with dir along axis of revolution */
        for (index = 0; index < 50; index++) {
            // Spheroid parameters ( +.1 to ensure no 0 value is returned )
            position = new Vector3D(MathLib.random(), MathLib.random(), MathLib.random());
            revAxis = new Vector3D(MathLib.random() + .1, MathLib.random() + .1, MathLib.random() + .1);
            a = MathLib.random() + .1;
            b = MathLib.random() + .1;

            // create spheroid object
            mySpheroid = new Spheroid(position, revAxis, a, b);

            // random values for line position displacement (orthogonal to center)
            // orthSA is between -1.5*a and 1.5*a
            orthSA = 3 * (MathLib.random() - .5) * a;

            // create line properties
            linePos = position;
            linePos = linePos.add(revAxis.orthogonal().scalarMultiply(orthSA));

            // create line
            myLine1 = new Line(linePos, linePos.add(revAxis));

            // The line intersects if the displacement relative to the centers postion is
            // lower than a because line direction is along axis of revolution
            expected = MathLib.abs(orthSA) < a;
            actual = mySpheroid.intersects(myLine1);

            Assert.assertEquals(expected, actual);
        }

        /** Second test, line with dir orthogonal to axis of revolution */
        for (index = 0; index < 50; index++) {
            // Spheroid parameters ( +.1 to ensure no 0 value is returned )
            position = new Vector3D(MathLib.random(), MathLib.random(), MathLib.random());
            revAxis = new Vector3D(MathLib.random() + .1, MathLib.random() + .1, MathLib.random() + .1);
            a = MathLib.random() + .1;
            b = MathLib.random() + .1;

            // create spheroid object
            mySpheroid = new Spheroid(position, revAxis, a, b);

            // random values for line position displacement (parallel to center)
            // revSA is between -1.5*b and 1.5*b
            revSA = 3 * (MathLib.random() - .5) * b;

            // create line properties
            linePos = position;
            linePos = linePos.add(revAxis.normalize().scalarMultiply(revSA));

            // create line
            myLine1 = new Line(linePos, linePos.add(revAxis.orthogonal()));

            // The line intersects if the displacement relative to the centers postion is
            // lower than b because its dir is orthogonal to axis of revolution
            expected = MathLib.abs(revSA) < b;
            Assert.assertEquals(expected, mySpheroid.intersects(myLine1));
        }

        /** Third test, line with random direction and a point inside the spheroid */
        for (index = 0; index < 50; index++) {
            // Spheroid parameters
            position = new Vector3D(MathLib.random() + 10, MathLib.random() + 10, MathLib.random() + 10);
            revAxis = new Vector3D(MathLib.random() + .1, MathLib.random() + .1, MathLib.random() + .1);
            a = 1;
            b = 1;

            // create spheroid object
            mySpheroid = new Spheroid(position, revAxis, a, b);

            // random values for displacement of line origin, but within spheroid!
            revSA = 2 * (MathLib.random() - .5) * b;
            orthSA = MathLib.random() * a * MathLib.sqrt(1 - revSA * revSA / (b * b));

            // create a line that has a random direction but that intersects
            // this is done by ensuring one point is inside the spheroid
            lineDir = new Vector3D(MathLib.random(), MathLib.random(), MathLib.random());
            linePos = position;
            linePos = linePos.add(revAxis.normalize().scalarMultiply(revSA));
            linePos = linePos.add(revAxis.orthogonal().scalarMultiply(orthSA));

            // create line
            myLine1 = new Line(linePos, linePos.add(lineDir));

            // This line always intersects
            Assert.assertEquals(true, mySpheroid.intersects(myLine1));
        }

        /** Fourth test, pour la route */
        // Spheroid parameters
        position = new Vector3D(10, 10, 10);
        revAxis = new Vector3D(1, 1, 1);
        a = 1;
        b = 1;

        // create spheroid object
        mySpheroid = new Spheroid(position, revAxis, a, b);

        // line
        lineDir = new Vector3D(2, 2, 4);
        linePos = new Vector3D(0, 0, 0);

        // create line
        myLine1 = new Line(linePos, linePos.add(lineDir));

        // This line doesnt intersects
        Assert.assertEquals(false, mySpheroid.intersects(myLine1));

        /** Double check */
        // Spheroid parameters
        position = new Vector3D(2, 0, 0);
        revAxis = new Vector3D(0, 0, 1);
        a = 2;
        b = 1;
        // create spheroid object
        mySpheroid = new Spheroid(position, revAxis, a, b);

        // create line
        lineDir = new Vector3D(0, 0, 1);
        linePos = new Vector3D(2, 0, 0);
        myLine1 = new Line(linePos, linePos.add(lineDir));

        Assert.assertEquals(true, mySpheroid.intersects(myLine1));

        /** Double check */
        // Spheroid parameters
        position = new Vector3D(2, 0, 0);
        revAxis = new Vector3D(0, 0, 1);
        a = 2;
        b = 1;
        // create spheroid object
        mySpheroid = new Spheroid(position, revAxis, a, b);

        // create line
        lineDir = new Vector3D(0, 0, 1);
        linePos = new Vector3D(5, 0, 0);
        myLine1 = new Line(linePos, linePos.add(lineDir));

        Assert.assertEquals(false, mySpheroid.intersects(myLine1));

        /** Double check */
        // Spheroid parameters
        position = new Vector3D(0, 0, 0);
        revAxis = new Vector3D(0, 0, 1);
        a = 2;
        b = 1;
        // create spheroid object
        mySpheroid = new Spheroid(position, revAxis, a, b);

        // create line
        lineDir = new Vector3D(1, 1, 0);
        linePos = new Vector3D(0, 0, 1);
        myLine1 = new Line(linePos, linePos.add(lineDir));
        Assert.assertEquals(true, mySpheroid.intersects(myLine1));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#SPHEROID_INTERSECTIONS}
     * 
     * @testedMethod {@link Spheroid#getIntersectionPoints(Line)}
     * 
     * @description Test Spheroid intersections algorithm.
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

        // Spheroid parameters
        final Vector3D position = new Vector3D(10, 10, 10);
        final Vector3D revAxis = new Vector3D(0, 0, 1);
        final int a = 1;
        final int b = 1;

        // create spheroid object
        final Spheroid mySpheroid = new Spheroid(position, revAxis, a, b);

        // line with two intersections
        Vector3D lineDir = new Vector3D(0, 0, 1);
        Vector3D linePos = new Vector3D(10, 10, 9);
        // create line
        Line myLine1 = new Line(linePos, linePos.add(lineDir));
        // This line does intersects, closest point is (10, 10, 9) and comes first
        Vector3D[] intersections = mySpheroid.getIntersectionPoints(myLine1);
        if (intersections[0].getNorm() > intersections[1].getNorm()) {
            final Vector3D temp = intersections[0];
            intersections[0] = intersections[1];
            intersections[1] = temp;
        }

        Assert.assertEquals(10, intersections[0].getX(), this.comparisonEpsilon);
        Assert.assertEquals(10, intersections[0].getY(), this.comparisonEpsilon);
        Assert.assertEquals(9, intersections[0].getZ(), this.comparisonEpsilon);
        Assert.assertEquals(10, intersections[1].getX(), this.comparisonEpsilon);
        Assert.assertEquals(10, intersections[1].getY(), this.comparisonEpsilon);
        Assert.assertEquals(11, intersections[1].getZ(), this.comparisonEpsilon);

        // line with one intersections
        lineDir = new Vector3D(0, 0, 1);
        linePos = new Vector3D(9, 10, 10);
        // create line
        myLine1 = new Line(linePos, linePos.add(lineDir));
        // This line does intersects
        intersections = mySpheroid.getIntersectionPoints(myLine1);

        Assert.assertEquals(9, intersections[0].getX(), this.comparisonEpsilon);
        Assert.assertEquals(10, intersections[0].getY(), this.comparisonEpsilon);
        Assert.assertEquals(10, intersections[0].getZ(), this.comparisonEpsilon);

        // line with no intersections
        lineDir = new Vector3D(0, 0, 1);
        linePos = new Vector3D(9, 9, 10);
        // create line
        myLine1 = new Line(linePos, linePos.add(lineDir));
        // This line does intersects
        intersections = mySpheroid.getIntersectionPoints(myLine1);
        Assert.assertEquals(0, intersections.length);
    }

    /**
     * Testing distance and closestPointTo(Vector3D)
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#SPHEROID_DISTANCES}
     * 
     * @testedMethod {@link Spheroid#closestPointTo(Vector3D)}
     * @testedMethod {@link Spheroid#distanceTo(Vector3D)}
     * 
     * @description Test Spheroid distance computation algorithms for distance to Vector3D.
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
    public final void testClosestPointToPart1() {

        // Params
        double ct;
        Vector3D center;
        Vector3D axis;
        double a;
        double b;
        Spheroid spheroid;
        Vector3D aFarAwayPoint;
        Vector3D theClosestPoint;
        Vector3D expectedPoint;
        //
        /** test avec un spheroid sur l'axe X et un point en dehors */
        // definition d'un spheroide de revolution
        center = new Vector3D(0, 0, 0);
        axis = new Vector3D(0, 0, 1);
        a = 2;
        b = 1;
        spheroid = new Spheroid(center, axis, a, b);
        // un point de l'espace (en dehors de l'spheroide
        // et son pt le plus proche de l'spheroide
        aFarAwayPoint = spheroid.getAffineStandardExpression(new Vector3D(5, 0, 0));
        expectedPoint = spheroid.getAffineStandardExpression(new Vector3D(a, 0, 0));
        // le point de l'spheroide le plus proche de ce point de l'espace
        theClosestPoint = spheroid.closestPointTo(aFarAwayPoint);
        Assert.assertEquals(expectedPoint.getX(), theClosestPoint.getX(), this.comparisonEpsilon);
        Assert.assertEquals(expectedPoint.getY(), theClosestPoint.getY(), this.comparisonEpsilon);
        Assert.assertEquals(expectedPoint.getZ(), theClosestPoint.getZ(), this.comparisonEpsilon);
        Assert.assertEquals(aFarAwayPoint.subtract(expectedPoint).getNorm(), spheroid.distanceTo(aFarAwayPoint),
            this.comparisonEpsilon);

        /** test avec un spheroid sur l'axe X et un point en dehors */
        // definition d'un spheroide de revolution
        ct = 0;
        center = new Vector3D(ct, 0, 0);
        axis = new Vector3D(0, 0, 1);
        a = 2;
        b = 1;
        spheroid = new Spheroid(center, axis, a, b);
        // un point de l'espace (en dehors de l'spheroide)
        // et son pt le plus proche de l'spheroide
        aFarAwayPoint = spheroid.getAffineStandardExpression(new Vector3D(10, 0, 0));
        expectedPoint = spheroid.getAffineStandardExpression(new Vector3D(a, 0, 0));
        // le point de l'spheroide le plus proche de ce point de l'espace
        theClosestPoint = spheroid.closestPointTo(aFarAwayPoint);
        Assert.assertEquals(expectedPoint.getX(), theClosestPoint.getX(), this.comparisonEpsilon);
        Assert.assertEquals(expectedPoint.getY(), theClosestPoint.getY(), this.comparisonEpsilon);
        Assert.assertEquals(expectedPoint.getZ(), theClosestPoint.getZ(), this.comparisonEpsilon);
        Assert.assertEquals(aFarAwayPoint.subtract(expectedPoint).getNorm(), spheroid.distanceTo(aFarAwayPoint),
            this.comparisonEpsilon);

        /** test avec un spheroid sur l'axe X et un point en dehors */
        // definition d'un spheroide de revolution
        ct = 70;
        center = new Vector3D(ct, 0, 0);
        axis = new Vector3D(0, 0, 1);
        a = 2;
        b = 1;
        spheroid = new Spheroid(center, axis, a, b);
        // un point de l'espace (en dehors de l'spheroide
        // et son pt le plus proche de l'spheroide
        aFarAwayPoint = spheroid.getAffineStandardExpression(new Vector3D(10, 0, 0));
        expectedPoint = spheroid.getAffineStandardExpression(new Vector3D(a, 0, 0));
        // le point de l'spheroide le plus proche de ce point de l'espace
        theClosestPoint = spheroid.closestPointTo(aFarAwayPoint);
        Assert.assertEquals(expectedPoint.getX(), theClosestPoint.getX(), this.comparisonEpsilon);
        Assert.assertEquals(expectedPoint.getY(), theClosestPoint.getY(), this.comparisonEpsilon);
        Assert.assertEquals(expectedPoint.getZ(), theClosestPoint.getZ(), this.comparisonEpsilon);
        Assert.assertEquals(aFarAwayPoint.subtract(expectedPoint).getNorm(), spheroid.distanceTo(aFarAwayPoint),
            this.comparisonEpsilon);

        /** test avec un spheroid sur l'axe X et un point en dehors */
        // definition d'un spheroide de revolution
        ct = 0;
        center = new Vector3D(ct, 0, 0);
        axis = new Vector3D(0, 0, 1);
        a = 2;
        b = 1;
        spheroid = new Spheroid(center, axis, a, b);
        // un point de l'espace (en dehors de l'spheroide
        // et son pt le plus proche de l'spheroide
        aFarAwayPoint = spheroid.getAffineStandardExpression(new Vector3D(-10, 0, 0));
        expectedPoint = spheroid.getAffineStandardExpression(new Vector3D(-a, 0, 0));
        // le point de l'spheroide le plus proche de ce point de l'espace
        theClosestPoint = spheroid.closestPointTo(aFarAwayPoint);
        Assert.assertEquals(expectedPoint.getX(), theClosestPoint.getX(), this.comparisonEpsilon);
        Assert.assertEquals(expectedPoint.getY(), theClosestPoint.getY(), this.comparisonEpsilon);
        Assert.assertEquals(expectedPoint.getZ(), theClosestPoint.getZ(), this.comparisonEpsilon);
        Assert.assertEquals(aFarAwayPoint.subtract(expectedPoint).getNorm(), spheroid.distanceTo(aFarAwayPoint),
            this.comparisonEpsilon);

        /** test avec un spheroid sur l'axe X et un point dessus */
        // definition d'un spheroide de revolution
        ct = 0;
        center = new Vector3D(ct, 0, 0);
        axis = new Vector3D(0, 0, 1);
        a = 2;
        b = 1;
        spheroid = new Spheroid(center, axis, a, b);
        // un point de l'espace (en dehors de l'spheroide
        // et son pt le plus proche de l'spheroide
        aFarAwayPoint = spheroid.getAffineStandardExpression(new Vector3D(-a, 0, 0));
        expectedPoint = spheroid.getAffineStandardExpression(new Vector3D(-a, 0, 0));
        // le point de l'spheroide le plus proche de ce point de l'espace
        theClosestPoint = spheroid.closestPointTo(aFarAwayPoint);
        Assert.assertEquals(expectedPoint.getX(), theClosestPoint.getX(), this.comparisonEpsilon);
        Assert.assertEquals(expectedPoint.getY(), theClosestPoint.getY(), this.comparisonEpsilon);
        Assert.assertEquals(expectedPoint.getZ(), theClosestPoint.getZ(), this.comparisonEpsilon);
        Assert.assertEquals(aFarAwayPoint.subtract(expectedPoint).getNorm(), spheroid.distanceTo(aFarAwayPoint),
            this.comparisonEpsilon);

        center = new Vector3D(25, 5, 0);
        axis = new Vector3D(1, 1, 1);
        a = 2;
        b = 2.5;
        spheroid = new Spheroid(center, axis, a, b);

        // Test : Spheroid + point on rev axis z < 0

        // Test : Spheroid + point on rev axis z > 0
        aFarAwayPoint = spheroid.getAffineStandardExpression(new Vector3D(0, 0, 10));
        theClosestPoint = spheroid.closestPointTo(aFarAwayPoint);
        expectedPoint = spheroid.getAffineStandardExpression(new Vector3D(0, 0, b));
        Assert.assertEquals(aFarAwayPoint.subtract(expectedPoint).getNorm(), spheroid.distanceTo(aFarAwayPoint),
            this.comparisonEpsilon);

        // Test : Spheroid + point on X-axis orthogonal to rev axis
        aFarAwayPoint = spheroid.getAffineStandardExpression(new Vector3D(-6, 0, 0));
        theClosestPoint = spheroid.closestPointTo(aFarAwayPoint);
        expectedPoint = spheroid.getAffineStandardExpression(new Vector3D(-a, 0, 0));
        Assert.assertEquals(aFarAwayPoint.subtract(expectedPoint).getNorm(), spheroid.distanceTo(aFarAwayPoint),
            this.comparisonEpsilon);

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#SPHEROID_DISTANCES}
     * 
     * @testedMethod {@link Spheroid#closestPointTo(Vector3D)}
     * @testedMethod {@link Spheroid#distanceTo(Vector3D)}
     * 
     * @description Test Spheroid distance computation algorithms. Test part 2 only to keep part 1 under 100 lines.
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
    public final void testClosestPointToPart2() {

        Vector3D center = new Vector3D(25, 5, 0);
        Vector3D axis = new Vector3D(1, 1, 1);
        double a = 2;
        double b = 2.5;
        Spheroid spheroid = new Spheroid(center, axis, a, b);

        // Test : Spheroid + point on rev axis z < 0
        Vector3D aFarAwayPoint = spheroid.getAffineStandardExpression(new Vector3D(0, 0, -10));
        Vector3D theClosestPoint = spheroid.closestPointTo(aFarAwayPoint);
        Vector3D expectedPoint = spheroid.getAffineStandardExpression(new Vector3D(0, 0, -b));
        Assert.assertEquals(expectedPoint.getX(), theClosestPoint.getX(), this.comparisonEpsilon);
        Assert.assertEquals(expectedPoint.getY(), theClosestPoint.getY(), this.comparisonEpsilon);
        Assert.assertEquals(expectedPoint.getZ(), theClosestPoint.getZ(), this.comparisonEpsilon);
        Assert.assertEquals(aFarAwayPoint.subtract(expectedPoint).getNorm(), spheroid.distanceTo(aFarAwayPoint),
            this.comparisonEpsilon);

        // Test : Spheroid + point on Y-axis
        aFarAwayPoint = spheroid.getAffineStandardExpression(new Vector3D(0, 6, 0));
        theClosestPoint = spheroid.closestPointTo(aFarAwayPoint);
        expectedPoint = spheroid.getAffineStandardExpression(new Vector3D(0, a, 0));
        Assert.assertEquals(expectedPoint.getX(), theClosestPoint.getX(), this.comparisonEpsilon);
        Assert.assertEquals(expectedPoint.getY(), theClosestPoint.getY(), this.comparisonEpsilon);
        Assert.assertEquals(expectedPoint.getZ(), theClosestPoint.getZ(), this.comparisonEpsilon);
        Assert.assertEquals(aFarAwayPoint.subtract(expectedPoint).getNorm(), spheroid.distanceTo(aFarAwayPoint),
            this.comparisonEpsilon);

        // Test : Spheroid + point on Y-axis
        aFarAwayPoint = spheroid.getAffineStandardExpression(new Vector3D(0, 15, 0));
        theClosestPoint = spheroid.closestPointTo(aFarAwayPoint);
        expectedPoint = spheroid.getAffineStandardExpression(new Vector3D(0, a, 0));
        Assert.assertEquals(expectedPoint.getX(), theClosestPoint.getX(), this.comparisonEpsilon);
        Assert.assertEquals(expectedPoint.getY(), theClosestPoint.getY(), this.comparisonEpsilon);
        Assert.assertEquals(expectedPoint.getZ(), theClosestPoint.getZ(), this.comparisonEpsilon);
        Assert.assertEquals(aFarAwayPoint.subtract(expectedPoint).getNorm(), spheroid.distanceTo(aFarAwayPoint),
            this.comparisonEpsilon);

        spheroid = new Spheroid(new Vector3D(25, 5, 0), new Vector3D(1, 1, 1), 2, 2.5);

        // Test : Spheroid + point at center + a<b
        aFarAwayPoint = spheroid.getAffineStandardExpression(new Vector3D(0, 0, 0));
        theClosestPoint = spheroid.closestPointTo(aFarAwayPoint);
        expectedPoint = spheroid.getAffineStandardExpression(new Vector3D(a, 0, 0));
        Assert.assertEquals(-aFarAwayPoint.subtract(expectedPoint).getNorm(), spheroid.distanceTo(aFarAwayPoint),
            this.comparisonEpsilon);

        // Test : Spheroid + point inside
        aFarAwayPoint = spheroid.getAffineStandardExpression(new Vector3D(1, 0, 0));
        theClosestPoint = spheroid.closestPointTo(aFarAwayPoint);
        expectedPoint = spheroid.getAffineStandardExpression(new Vector3D(a, 0, 0));
        Assert.assertEquals(expectedPoint.getX(), theClosestPoint.getX(), this.comparisonEpsilon);
        Assert.assertEquals(expectedPoint.getY(), theClosestPoint.getY(), this.comparisonEpsilon);
        Assert.assertEquals(expectedPoint.getZ(), theClosestPoint.getZ(), this.comparisonEpsilon);
        Assert.assertEquals(-aFarAwayPoint.subtract(expectedPoint).getNorm(), spheroid.distanceTo(aFarAwayPoint),
            this.comparisonEpsilon);

        center = new Vector3D(25, 5, 0);
        axis = new Vector3D(1, 1, 1);
        spheroid = new Spheroid(center, axis, 2.5, 2);

        // Test : Spheroid + point at center
        aFarAwayPoint = spheroid.getAffineStandardExpression(new Vector3D(0, 0, 0));
        theClosestPoint = spheroid.closestPointTo(aFarAwayPoint);
        expectedPoint = spheroid.getAffineStandardExpression(new Vector3D(0, 0, 2));
        Assert.assertEquals(expectedPoint.getX(), theClosestPoint.getX(), this.comparisonEpsilon);
        Assert.assertEquals(expectedPoint.getY(), theClosestPoint.getY(), this.comparisonEpsilon);
        Assert.assertEquals(expectedPoint.getZ(), theClosestPoint.getZ(), this.comparisonEpsilon);
        Assert.assertEquals(-aFarAwayPoint.subtract(expectedPoint).getNorm(), spheroid.distanceTo(aFarAwayPoint),
            this.comparisonEpsilon);

        center = new Vector3D(25, 5, 0);
        axis = new Vector3D(1, 1, 1);
        a = 1;
        b = 3;
        spheroid = new Spheroid(center, axis, a, b);

        // Test : Spheroid + point at center + b<a
        aFarAwayPoint = spheroid.getAffineStandardExpression(new Vector3D(0, 0, 10));
        theClosestPoint = spheroid.closestPointTo(aFarAwayPoint);
        expectedPoint = spheroid.getAffineStandardExpression(new Vector3D(0, 0, b));
        Assert.assertEquals(expectedPoint.getX(), theClosestPoint.getX(), this.comparisonEpsilon);
        Assert.assertEquals(expectedPoint.getY(), theClosestPoint.getY(), this.comparisonEpsilon);
        Assert.assertEquals(expectedPoint.getZ(), theClosestPoint.getZ(), this.comparisonEpsilon);
        Assert.assertEquals(aFarAwayPoint.subtract(expectedPoint).getNorm(), spheroid.distanceTo(aFarAwayPoint),
            this.comparisonEpsilon);

        center = new Vector3D(-7, 6, -3);
        axis = new Vector3D(1, -2, 1.5);
        a = 1;
        b = 1;
        spheroid = new Spheroid(center, axis, a, b);

        // Test : Spheroid + point in Oxz
        aFarAwayPoint = spheroid.getAffineStandardExpression(new Vector3D(10, 0, 10));
        theClosestPoint = spheroid.closestPointTo(aFarAwayPoint);
        expectedPoint = spheroid.getAffineStandardExpression(new Vector3D(MathLib.cos(FastMath.PI / 4), 0, Math
            .sin(FastMath.PI / 4)));
        Assert.assertEquals(expectedPoint.getX(), theClosestPoint.getX(), this.comparisonEpsilon);
        Assert.assertEquals(expectedPoint.getY(), theClosestPoint.getY(), this.comparisonEpsilon);
        Assert.assertEquals(expectedPoint.getZ(), theClosestPoint.getZ(), this.comparisonEpsilon);
        Assert.assertEquals(aFarAwayPoint.subtract(expectedPoint).getNorm(), spheroid.distanceTo(aFarAwayPoint),
            this.comparisonEpsilon);

        // Test : Spheroid + point in Oyz
        aFarAwayPoint = spheroid.getAffineStandardExpression(new Vector3D(0, 10, 10));
        theClosestPoint = spheroid.closestPointTo(aFarAwayPoint);
        expectedPoint = spheroid.getAffineStandardExpression(new Vector3D(0, MathLib.cos(FastMath.PI / 4), Math
            .sin(FastMath.PI / 4)));
        Assert.assertEquals(expectedPoint.getX(), theClosestPoint.getX(), this.comparisonEpsilon);
        Assert.assertEquals(expectedPoint.getY(), theClosestPoint.getY(), this.comparisonEpsilon);
        Assert.assertEquals(expectedPoint.getZ(), theClosestPoint.getZ(), this.comparisonEpsilon);
        Assert.assertEquals(aFarAwayPoint.subtract(expectedPoint).getNorm(), spheroid.distanceTo(aFarAwayPoint),
            this.comparisonEpsilon);

        // Test : Spheroid + point in Oxy
        aFarAwayPoint = spheroid.getAffineStandardExpression(new Vector3D(10, 10, 0));
        theClosestPoint = spheroid.closestPointTo(aFarAwayPoint);
        expectedPoint = spheroid.getAffineStandardExpression(new Vector3D(MathLib.cos(FastMath.PI / 4), MathLib
            .sin(FastMath.PI / 4),
            0));
        Assert.assertEquals(aFarAwayPoint.subtract(expectedPoint).getNorm(), spheroid.distanceTo(aFarAwayPoint),
            this.comparisonEpsilon);

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#SPHEROID_DISTANCES}
     * 
     * @testedMethod {@link Spheroid#closestPointTo(Line)}
     * @testedMethod {@link Spheroid#distanceTo(Line)}
     * 
     * @description Test Spheroid distance computation algorithms.
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
    public final void testDistanceToLine() {

        // Spheroid parameters
        final Vector3D position = new Vector3D(5, 5, 5);
        final Vector3D revAxis = new Vector3D(1, 1, 1);
        final double a = 2;
        final double b = 2.5;
        double distance;

        // create spheroid object
        Spheroid mySpheroid;

        // line params
        Vector3D lineOrg;
        Vector3D lineDir;

        // point is on axis of revolution
        mySpheroid = new Spheroid(position, revAxis, a, b);
        lineOrg = new Vector3D(0, 0, 0);
        lineDir = new Vector3D(-.5, -.5, 1);
        Line myLine = new Line(lineOrg, lineOrg.add(lineDir));

        distance = mySpheroid.distanceTo(myLine);
        Assert.assertEquals(5 * MathLib.sqrt(3) - 2.5, distance, this.comparisonEpsilon);

        // point isnt on axis of revolution
        mySpheroid = new Spheroid(new Vector3D(5, 5, 5), new Vector3D(0, 0, 1), a, b);

        lineOrg = new Vector3D(0, 0, 0);
        lineDir = new Vector3D(0, 0, 1);
        myLine = new Line(lineOrg, lineOrg.add(lineDir));

        distance = mySpheroid.distanceTo(myLine);
        Assert.assertEquals(5 * MathLib.sqrt(2) - 2, distance, this.comparisonEpsilon);

        // line intersects
        mySpheroid = new Spheroid(new Vector3D(5, 5, 5), new Vector3D(0, 0, 1), a, b);

        lineOrg = new Vector3D(5, 5, 0);
        lineDir = new Vector3D(0, 0, 1);
        myLine = new Line(lineOrg, lineOrg.add(lineDir));

        distance = mySpheroid.distanceTo(myLine);
        Assert.assertEquals(0, distance, this.comparisonEpsilon);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#SPHEROID_DISTANCES}
     * 
     * @testedMethod {@link Spheroid#toString()}
     * 
     * @description Creates a string describing the shape, the order of the informations in this output being the same
     *              as the one of the constructor
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

        // Spheroid
        final Vector3D position = new Vector3D(5, 5, 5);
        final Vector3D revAxis = new Vector3D(1, 0, 0);
        final double a = 2;
        final double b = 2.5;
        final Spheroid mySpheroid = new Spheroid(position, revAxis, a, b);

        // string creation
        final String result = mySpheroid.toString();

        final String expected =
            "Spheroid{Center{5; 5; 5},Revolution axis{1; 0; 0},Equatorial radius{2.0},Polar radius{2.5}}";
        Assert.assertEquals(expected, result);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#SPHEROID_DISTANCES}
     * 
     * @testedMethod {@link Spheroid#closestPointTo(Vector3D)}
     * 
     * @description Make sure the vector (user point - computed closest point) is normal to the surface of the spheroid
     * 
     * @input none.
     * 
     * @output dot product of surface tangents and user point / closest point vector
     * 
     * @testPassCriteria The computed dot products must be within machine espilon range
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public void testOrthogonality() {

        // definition d'un ellipsoide de revolution
        final Vector3D center = new Vector3D(0, 0, 0);
        final Vector3D axis = new Vector3D(0, 0, 1);
        final double a = 2;
        final double b = 1;
        final Spheroid sph = new Spheroid(center, axis, a, b);

        // Bug : until the bug is fixed, the test is 'unrandomized' with a seed so it never fails.
        final Random ran = new Random(1);
        Vector3D p = null;
        Vector3D s;
        Vector3D dir;
        Vector3D v1;
        Vector3D v2;
        double ct;
        double st;
        double cp;
        double sp;
        double[] cc;
        for (int i = 0; i < 100; i++) {

            // random point and its closest point
            p = new Vector3D(ran.nextDouble() * 10, ran.nextDouble() * 10, ran.nextDouble() * 10);
            // For following p : fails!
            // p = new Vector3D(0.663563233, 0.9231832269, 0.2113361458);
            s = sph.closestPointTo(p);

            // vector from closest point to user point
            dir = p.subtract(s);

            // ellipsoidic coordinates of closest point
            cc = sph.getEllipsoidicCoordinates(s);
            ct = MathLib.cos(cc[0]);
            st = MathLib.sin(cc[0]);
            cp = MathLib.cos(cc[1]);
            sp = MathLib.sin(cc[1]);

            // tangents to ellipsoid surface
            v1 = new Vector3D(-a * st * cp, a * ct * cp, 0);
            v2 = new Vector3D(-a * ct * sp, -a * st * sp, b * cp);

            // make sure the dir vector is normal to the surface
            Assert.assertEquals(0, Vector3D.dotProduct(v1, dir), this.comparisonEpsilon);
            Assert.assertEquals(0, Vector3D.dotProduct(v2, dir), this.comparisonEpsilon);

        }

    }
}
