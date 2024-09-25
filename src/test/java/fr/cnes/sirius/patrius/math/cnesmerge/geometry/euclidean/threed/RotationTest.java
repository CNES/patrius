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
 * HISTORY
 * VERSION:4.13:DM:DM-3:08/12/2023:[PATRIUS] Distinction entre corps celestes et barycentres
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:FA:FA-2398:27/05/2020:incoherence de seuils de norme entre AngularCoordinates et Rotation 
 * VERSION:4.5:DM:DM-2456:27/05/2020:optimisation des rotations dans le cas identite
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:319:05/03/2015:Corrected Rotation class (Step1)
 * VERSION::DM:342:05/03/2015:No exceptions thrown for singular Euler or Cardan angles: testSingularities() replaced
 * VERSION::FA:509:15/10/2015:Protection of inverted trigonometric function call
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.cnesmerge.geometry.euclidean.threed;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import junit.framework.Assert;

import org.junit.Test;

import fr.cnes.sirius.patrius.math.complex.Quaternion;
import fr.cnes.sirius.patrius.math.exception.MathArithmeticException;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.NotARotationMatrixException;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.RotationOrder;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.linear.BlockRealMatrix;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.math.util.Precision;

/**
 * @description <p>
 *              test class for Rotation
 *              </p>
 * 
 * @author Julie Anton
 * 
 * @version $Id: RotationTest.java 17909 2017-09-11 11:57:36Z bignon $
 * 
 * @since 1.0
 * 
 */
public class RotationTest {

    /** Used epsilon for double comparison */
    private final double comparisonEpsilon = Precision.DOUBLE_COMPARISON_EPSILON;

    /** Epsilon below which one an angle is negligible. */
    private final double angleEpsilon = 1e-10;

    /** Epsilon below which one a distance is negligible. */
    private final double geometryEpsilon = 1e-10;

    /** Features description. */
    public enum features {
        /**
         * @featureTitle rotation constructors
         * 
         * @featureDescription There are several way to build a rotation.
         * 
         * @coveredRequirements DV-MATHS_360
         */
        CONSTRUCTION,

        /**
         * @featureTitle comparison
         * 
         * @featureDescription Two rotations can be compared either strictly or approximately.
         * 
         * @coveredRequirements DV-MATHS_410
         */
        COMPARISON,

        /**
         * @featureTitle interpolation
         * 
         * @featureDescription We can calculate the spherical interpolation and the linear interpolation of rotations.
         * 
         * @coveredRequirements DV-MATHS_430, DV-MATHS_440
         */
        INTERPOLATION,

        /**
         * @featureTitle interpolation
         * 
         * @featureDescription We can calculate the spherical interpolation and the linear interpolation of rotations.
         * 
         * @coveredRequirements DV-MATHS_360
         */
        ROTATION_CM_COVERAGE
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#CONSTRUCTION}
     * 
     * @testedMethod {@link Rotation#Rotation(boolean, Quaternion)}
     * @testedMethod {@link Rotation#Rotation(boolean, double, double, double, double)}
     * 
     * @description Rotation constructors from quaternion are tested, one of the constructors asks for 4 doubles
     *              (quaternion components) and a boolean (needs to be normalized or not), the other one asks for an
     *              object Quaternion {@link Quaternion}.
     * 
     * @input Rotation sr1 = (true, 1,2,3,4)
     *        <p>
     *        Rotation sr2 = (false, cos(PI/3), sin(PI/3), 0, 0)
     *        </p>
     *        <p>
     *        Rotation sr3 = (false, Quaternion(1,2,3,4))
     *        </p>
     * 
     * @output Rotation
     * 
     * @testPassCriteria The Rotation should be implemented except for the third case, with this constructor (from the
     *                   object Quaternion), when the quaternion given in parameter is not nomalized, the constructor
     *                   throws an exception.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    @SuppressWarnings("unused")
    public void testConstructors() {
        
        final Rotation r = new Rotation(false, 0, 1, 0, 0);
        System.out.println(r.getAngles(RotationOrder.YZY));
        System.out.println(r.getAngle());
        System.out.println(r.getAxis());
        
        final double d1 = MathLib.cos(FastMath.PI / 3.);
        final double d2 = MathLib.sin(FastMath.PI / 3.);
        final Quaternion q = new Quaternion(d1, d2, 0., 0.);
        // test constructor from (int, double, double, double, double)
        final Rotation sr1 = new Rotation(true, 1., 2., 3., 4.);
        Assert.assertNotNull(sr1);

        final Rotation sr3 = new Rotation(false, d1, d2, 0., 0.);
        Assert.assertNotNull(sr3);

        Rotation sr2 = new Rotation(false, q);
        Assert.assertNotNull(sr2);
        sr2 = new Rotation(false, q);
        Assert.assertNotNull(sr2);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#COMPARISON}
     * 
     * @testedMethod {@link Rotation#isEqualTo(Rotation)}
     * @testedMethod {@link Rotation#isEqualTo(Rotation, double, double)}
     * 
     * @description The methods of comparison between two rotations are tested. One of them is a strict comparison which
     *              evaluates the angle between the two rotations (this angle has to be lower than 1E-14
     *              {@link MathUtils#DOUBLE_COMPARISON_EPSILON}), the other one allows thresholds and compares the
     *              angles of the rotations and the angle between their axis of rotation.
     * 
     * @input Rotation rotationA = ((0,0,1), PI/6) : rotation of axis k and angle 30°
     * @input Rotation rotationB = ((0,0,1), PI/6 + 0.001) : rotation of axis k and angle slightly larger than 30°
     * @input Rotation rotationC = ((0,0.001,1), PI/6) : rotation of axis close to k and angle 30°
     * @input Rotation rotationD = (0,0,-1), PI/6) : rotation of axis -k and angle 30°
     * 
     * @output boolean : true if the rotations can be considered equal, false otherwise
     * 
     * @testPassCriteria We compare strictly rotationA with rotationA : they should be equal,
     *                   <p>
     *                   we compare rotationA and rotationB with a threshold of 0.01 rad for the angles and 0.001 rad
     *                   for the axis : they should be considered equal,
     *                   </p>
     *                   <p>
     *                   we compare rotationA and rotationB with a threshold of 0.001 rad for the angles and 0.001 rad
     *                   for the axis : they should NOT be considered equal,
     *                   </p>
     *                   <p>
     *                   we compare rotationA and rotationC with a threshold of 0.001 rad for the angles and 0.001 rad
     *                   for the axis : they should be considered equal,
     *                   </p>
     *                   <p>
     *                   we compare rotationA and rotationC with a threshold of 0.001 rad for the angles and 0.00001 rad
     *                   for the axis : they should NOT be considered equal,
     *                   </p>
     *                   <p>
     *                   we compare rotationA and rotationD with a threshold of 0.001 rad for the angles and 0.001 rad
     *                   for the axis : they should NOT be considered equal because they are opposed.
     *                   </p>
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testComparison() {

        final Rotation rotationA = new Rotation(Vector3D.PLUS_K, FastMath.PI / 6);
        final Rotation rotationB = new Rotation(Vector3D.PLUS_K, FastMath.PI / 6 + 0.001);
        final Rotation rotationC = new Rotation(new Vector3D(0, 0.001, 1), FastMath.PI / 6);
        final Rotation rotationD = new Rotation(Vector3D.MINUS_K, FastMath.PI / 6);

        // strict comparison : the angle between both rotations is lower than 1E-14
        Assert.assertTrue(rotationA.isEqualTo(rotationA));

        // approximate comparisons

        // case : same axis, close angles
        Assert.assertTrue(rotationA.isEqualTo(rotationB, 0.01, 0.001));
        Assert.assertFalse(rotationA.isEqualTo(rotationB, 0.001, 0.001));
        // case : same angles, close axis
        Assert.assertTrue(rotationA.isEqualTo(rotationC, 0.001, 0.001));
        Assert.assertFalse(rotationA.isEqualTo(rotationC, 0.001, 0.00001));

        // case : opposed rotations
        Assert.assertFalse(rotationA.isEqualTo(rotationD, 0.001, 0.001));

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#CONSTRUCTION}
     * 
     * @testedMethod {@link Rotation#Rotation(Vector3D, Vector3D)}
     * 
     * @description The construction of a rotation from two vectors (one vector u and its image v by the rotation) is
     *              tested. This test checks if the axis of this rotation is equal to the normalized cross product of u
     *              and v. This test is a complement to the testVectorOnePair().
     * 
     * @input Vector3D u = (3,2,1)
     *        <p>
     *        Vector3D v = (-4,2,2)
     *        </p>
     * 
     * @output The axis of the rotation which transforms the vector u in vector v and its angle
     * 
     * @testPassCriteria The axis of the rotation should be equal to the normalized cross product of u and v ; there are
     *                   considered to be equal if the length of the vector resulting from their difference is below
     *                   1e-10. The angle of the rotation should be equal to the expected one taken into account an
     *                   epsilon of 1e-10 (epsilon for angle comparisons).
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testVectorOnePair2() {

        final Vector3D u = new Vector3D(3, 2, 1);
        final Vector3D v = new Vector3D(-4, 2, 2);

        final Vector3D cross = Vector3D.crossProduct(u, v);
        final Rotation rotation = new Rotation(u, v);
        final double dot = Vector3D.dotProduct(u, v);
        if (dot <= 0) {
            Assert.assertEquals(FastMath.PI - MathLib.asin(cross.getNorm() / u.getNorm() / v.getNorm()),
                rotation.getAngle(), this.angleEpsilon);
        } else {
            Assert.assertEquals(MathLib.asin(cross.getNorm() / u.getNorm() / v.getNorm()), rotation.getAngle(),
                this.angleEpsilon);
        }
        checkVector(cross.normalize(), rotation.getAxis());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#INTERPOLATION}
     * 
     * @testedMethod {@link Rotation#slerp(Rotation, Rotation, double)}
     * 
     * @description The linear interpolation of rotations is tested.
     * 
     * @input The rotations r1 and r2 have the same axis and different angles : 32.12° for the rotation r1 and 63.672°
     *        for the rotation r2.
     *        <p>
     *        the rotations r3 and r4 have the same axis but different angles : 45° for the rotation r3 and 225° for the
     *        rotation r4
     *        </p>
     * 
     * @output Interpolated rotation
     * 
     * @testPassCriteria Interpolation between rotation r1 and rotation r2:
     *                   <p>
     *                   if the interpolation parameter is 3, an exception OutOfRangeException is raised.
     *                   </p>
     *                   <p>
     *                   if the interpolation parameter is -3, an exception OutOfRangeException is raised.
     *                   </p>
     *                   <p>
     *                   if the interpolation parameter is 0.5, the interpolated rotation is the rotation of angle
     *                   0.83594289853520409 rad and with the same axis that the rotations r1 and r2 with an epsilon of
     *                   1e-10 on the angle and an epsilon of 1e-14 on the axis components.
     *                   </p>
     *                   <p>
     *                   interpolation between rotation r3 and rotation r4 :
     *                   </p>
     *                   <p>
     *                   if the interpolation parameter is 0.5, the interpolated rotation is the rotation of angle
     *                   2.3561944901923453 rad and with the same axis that the rotations r3 and r4 with an epsilon of
     *                   1e-10 on the angle and an epsilon of 1e-14 on the axis components.
     *                   </p>
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @SuppressWarnings("unused")
    @Test
    public void testLERP() {
        final Vector3D axis = new Vector3D(1, 1, 1);
        final Rotation r1 = new Rotation(axis, (32.12 * MathUtils.DEG_TO_RAD));
        final Rotation r2 = new Rotation(axis, (63.672 * MathUtils.DEG_TO_RAD));

        // Case #1: parameter is out of range [0;1]
        try {
            Rotation.lerp(r1, r2, 3);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            // do nothing as it is expected
        }

        try {
            Rotation.lerp(r1, r2, -3);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            // do nothing as it is expected
        }

        // Case #2: parameter is in range [0;1]
        final Rotation r = Rotation.lerp(r1, r2, 0.5);

        Assert.assertEquals(0.83594289853520409, r.getAngle(), this.angleEpsilon);
        Assert.assertEquals(axis.getX() / axis.getNorm(), r.getAxis().getX(), this.comparisonEpsilon);
        Assert.assertEquals(axis.getY() / axis.getNorm(), r.getAxis().getY(), this.comparisonEpsilon);
        Assert.assertEquals(axis.getZ() / axis.getNorm(), r.getAxis().getZ(), this.comparisonEpsilon);

        // Case #3: rotations around same axis, one with angle &theta;, other one with angle (&theta;+&pi;)
        final Rotation r3 = new Rotation(axis, (FastMath.PI / 4.));
        final Rotation r4 = new Rotation(axis, (5. * FastMath.PI / 4.));
        final Rotation r5 = Rotation.lerp(r3, r4, 0.5);
        Assert.assertEquals(2.3561944901923453, r5.getAngle(), this.angleEpsilon);
        Assert.assertEquals(axis.getX() / axis.getNorm(), r5.getAxis().getX(), this.comparisonEpsilon);
        Assert.assertEquals(axis.getY() / axis.getNorm(), r5.getAxis().getY(), this.comparisonEpsilon);
        Assert.assertEquals(axis.getZ() / axis.getNorm(), r5.getAxis().getZ(), this.comparisonEpsilon);

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#INTERPOLATION}
     * 
     * @testedMethod {@link Rotation#slerp(Rotation, Rotation, double)}
     * 
     * @description The spherical interpolation of rotations is tested
     * 
     * @input The rotations r1 and r2 have the same axis and different angles : 25.33° for the rotation r1 and 47.99°
     *        for the rotation r2.
     *        <p>
     *        The rotations r3 and r4 have the same axis but different angles : 45° for the rotation r3 and 225° for the
     *        rotation r4.
     *        </p>
     * 
     * @output Interpolated rotation
     * 
     * @testPassCriteria Interpolation between rotation r1 and rotation r2:
     *                   <p>
     *                   If the interpolation parameter is 3, an exception OutOfRangeException is raised.
     *                   </p>
     *                   <p>
     *                   If the interpolation parameter is -3, an exception OutOfRangeException is raised.
     *                   </p>
     *                   <p>
     *                   If the interpolation parameter is 0.3, the interpolated rotation is the rotation of angle
     *                   0.56073938208073704 rad and with the same axis that the rotations r1 and r2 with an epsilon of
     *                   1e-10 on the angle and an epsilon of 1e-14 on the axis components.
     *                   </p>
     *                   <p>
     *                   Interpolation between rotation r3 and rotation r4 :
     *                   </p>
     *                   <p>
     *                   If the interpolation parameter is 0.5, the interpolated rotation is the rotation of angle
     *                   2.3561944901923453 rad and with the same axis that the rotations r3 and r4 with an epsilon of
     *                   1e-10 on the angle and an epsilon of 1e-14 on the axis components.
     *                   </p>
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @SuppressWarnings("unused")
    @Test
    public void testSLERP() {
        final Vector3D axis = new Vector3D(1, 1, 1);
        final Rotation r1 = new Rotation(axis, (25.33 * MathUtils.DEG_TO_RAD));
        final Rotation r2 = new Rotation(axis, (47.99 * MathUtils.DEG_TO_RAD));

        // Case #1: parameter is out of range [0;1]
        try {
            Rotation.slerp(r1, r2, 3);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            // do nothing as it is expected
        }

        try {
            Rotation.slerp(r1, r2, -3);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            // do nothing as it is expected
        }

        // Case #2: parameter is in range [0;1]
        final Rotation r = Rotation.slerp(r1, r2, 0.3);

        Assert.assertEquals(0.56073938208073704, r.getAngle(), this.angleEpsilon);
        Assert.assertEquals(axis.getX() / axis.getNorm(), r.getAxis().getX(), this.comparisonEpsilon);
        Assert.assertEquals(axis.getY() / axis.getNorm(), r.getAxis().getY(), this.comparisonEpsilon);
        Assert.assertEquals(axis.getZ() / axis.getNorm(), r.getAxis().getZ(), this.comparisonEpsilon);

        // Case #3: rotations around same axis, one with angle &theta;, other one with angle (&theta;+&pi;)
        final Rotation r3 = new Rotation(axis, (FastMath.PI / 4.));
        final Rotation r4 = new Rotation(axis, (5. * FastMath.PI / 4.));
        final Rotation r5 = Rotation.slerp(r3, r4, 0.5);
        Assert.assertEquals(2.3561944901923453, r5.getAngle(), this.angleEpsilon);
        Assert.assertEquals(axis.getX() / axis.getNorm(), r5.getAxis().getX(), this.comparisonEpsilon);
        Assert.assertEquals(axis.getY() / axis.getNorm(), r5.getAxis().getY(), this.comparisonEpsilon);
        Assert.assertEquals(axis.getZ() / axis.getNorm(), r5.getAxis().getZ(), this.comparisonEpsilon);

    }

    /**
     * @description Compare two vectors, if the norm of the vector which results from the difference of the two vectors
     *              that have to be compared id below 1e-10 the vectors are considered to be the same.
     * 
     * @param v1
     *        first vector
     * @param v2
     *        second vector
     * 
     * @since 1.0
     */
    private void checkVector(final Vector3D v1, final Vector3D v2) {
        Assert.assertTrue(v1.subtract(v2).getNorm() < this.geometryEpsilon);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#CONVENTION}
     * 
     * @testedMethod {@link Rotation#toString()}
     * 
     * @description Check the output of the method toString().
     * 
     * @input rotation built with the quaternion (0.5,0.5,0.5,0.5)
     * 
     * @output String
     * 
     * @testPassCriteria The expected output is "Rotation{0.5,0.5,0.5,0.5}".
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testToString() {
        final Rotation rotation = new Rotation(false, 0.5, 0.5, 0.5, 0.5);
        Assert.assertEquals("Rotation{0.5,0.5,0.5,0.5}", rotation.toString());
    }

    /*
     * BELOW : Commons math legacy tests, for coverage.
     */

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ROTATION_CM_COVERAGE}
     * 
     * @description Legacy test.
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void testIdentity() {

        Rotation r = new Rotation(false, 1, 0, 0, 0);

        Assert.assertTrue(r.isIdentity());
        Assert.assertTrue(r.revert().isIdentity());
        Assert.assertTrue(r.getAxis().equals(Vector3D.PLUS_I));
        Assert.assertTrue(r.getQuaternion().equals(Quaternion.IDENTITY));
        final double[] rQi = r.getQi();
        Assert.assertEquals(4, rQi.length, 0);
        Assert.assertEquals(1., rQi[0], 0.);
        Assert.assertEquals(0., rQi[1], 0.);
        Assert.assertEquals(0., rQi[2], 0.);
        Assert.assertEquals(0., rQi[3], 0.);
        Assert.assertEquals(0., r.getAngle(), 0.);
        final double[] rAngles = r.getAngles(RotationOrder.XYZ);
        Assert.assertEquals(3, rAngles.length, 0);
        Assert.assertEquals(0., rAngles[0], 0.);
        Assert.assertEquals(0., rAngles[1], 0.);
        Assert.assertEquals(0., rAngles[2], 0.);
        final double[][] IDENTITY_MATRIX = new double[][] { { 1., 0., 0. }, { 0., 1., 0. }, { 0., 0., 1. } };
        final double[][] resIdentity = r.getMatrix();
        Assert.assertTrue(new BlockRealMatrix(r.getMatrix()).equals(new BlockRealMatrix(IDENTITY_MATRIX)));
        Assert.assertTrue(r.applyTo(Vector3D.PLUS_I).equals(Vector3D.PLUS_I));
        Assert.assertTrue(r.applyInverseTo(Vector3D.PLUS_I).equals(Vector3D.PLUS_I));
        final Rotation r2 = new Rotation(new Vector3D(10, 10, 10), 2 * FastMath.PI / 3);
        Assert.assertTrue(r.applyTo(r2).equals(r2));
        Assert.assertTrue(r.applyInverseTo(r2).equals(r2));

        checkVector(r.applyTo(Vector3D.PLUS_I), Vector3D.PLUS_I);
        checkVector(r.applyTo(Vector3D.PLUS_J), Vector3D.PLUS_J);
        checkVector(r.applyTo(Vector3D.PLUS_K), Vector3D.PLUS_K);
        checkAngle(r.getAngle(), 0);

        double[] angles;
        angles = r.getAngles(RotationOrder.XYZ);
        for (int i = 0; i < 3; i++) {
            checkAngle(angles[i], 0);
        }
        angles = r.getAngles(RotationOrder.XYX);
        for (int i = 0; i < 3; i++) {
            checkAngle(angles[i], 0);
        }
        angles = r.getAngles(RotationOrder.ZYZ);
        for (int i = 0; i < 3; i++) {
            checkAngle(angles[i], 0);
        }
        angles = r.getAngles(RotationOrder.ZYX);
        for (int i = 0; i < 3; i++) {
            checkAngle(angles[i], 0);
        }
        angles = r.getAngles(RotationOrder.YXZ);
        for (int i = 0; i < 3; i++) {
            checkAngle(angles[i], 0);
        }
        angles = r.getAngles(RotationOrder.YXY);
        for (int i = 0; i < 3; i++) {
            checkAngle(angles[i], 0);
        }
        angles = r.getAngles(RotationOrder.ZXZ);
        for (int i = 0; i < 3; i++) {
            checkAngle(angles[i], 0);
        }
        angles = r.getAngles(RotationOrder.ZXY);
        for (int i = 0; i < 3; i++) {
            checkAngle(angles[i], 0);
        }
        angles = r.getAngles(RotationOrder.XZY);
        for (int i = 0; i < 3; i++) {
            checkAngle(angles[i], 0);
        }
        angles = r.getAngles(RotationOrder.YZY);
        for (int i = 0; i < 3; i++) {
            checkAngle(angles[i], 0);
        }
        angles = r.getAngles(RotationOrder.XZX);
        for (int i = 0; i < 3; i++) {
            checkAngle(angles[i], 0);
        }
        angles = r.getAngles(RotationOrder.YZX);
        for (int i = 0; i < 3; i++) {
            checkAngle(angles[i], 0);
        }

        r = new Rotation(false, -1, 0, 0, 0);
        checkVector(r.applyTo(Vector3D.PLUS_I), Vector3D.PLUS_I);
        checkVector(r.applyTo(Vector3D.PLUS_J), Vector3D.PLUS_J);
        checkVector(r.applyTo(Vector3D.PLUS_K), Vector3D.PLUS_K);
        checkAngle(r.getAngle(), 0);

        r = new Rotation(true, 42, 0, 0, 0);
        checkVector(r.applyTo(Vector3D.PLUS_I), Vector3D.PLUS_I);
        checkVector(r.applyTo(Vector3D.PLUS_J), Vector3D.PLUS_J);
        checkVector(r.applyTo(Vector3D.PLUS_K), Vector3D.PLUS_K);
        checkAngle(r.getAngle(), 0);

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ROTATION_CM_COVERAGE}
     * 
     * @description Legacy test.
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void testAxisAngle() {

        Rotation r = new Rotation(new Vector3D(10, 10, 10), 2 * FastMath.PI / 3);
        checkVector(r.applyTo(Vector3D.PLUS_I), Vector3D.PLUS_J);
        checkVector(r.applyTo(Vector3D.PLUS_J), Vector3D.PLUS_K);
        checkVector(r.applyTo(Vector3D.PLUS_K), Vector3D.PLUS_I);
        final double s = 1 / MathLib.sqrt(3);
        checkVector(r.getAxis(), new Vector3D(s, s, s));
        checkAngle(r.getAngle(), 2 * FastMath.PI / 3);

        try {
            final Rotation rot = new Rotation(new Vector3D(0, 0, 0), 2 * FastMath.PI / 3);
            Assert.assertEquals(Rotation.distance(rot, Rotation.IDENTITY), 0, 0);
        } catch (final MathIllegalArgumentException e) {
            Assert.fail("an exception should not have been thrown");
        }

        r = new Rotation(Vector3D.PLUS_K, 1.5 * FastMath.PI);
        checkVector(r.getAxis(), new Vector3D(0, 0, -1));
        checkAngle(r.getAngle(), 0.5 * FastMath.PI);

        r = new Rotation(Vector3D.PLUS_J, FastMath.PI);
        checkVector(r.getAxis(), Vector3D.PLUS_J);
        checkAngle(r.getAngle(), FastMath.PI);

        checkVector(Rotation.IDENTITY.getAxis(), Vector3D.PLUS_I);

        r = new Rotation(Vector3D.MINUS_J, -FastMath.PI / 3);
        checkVector(r.getAxis(), Vector3D.PLUS_J);
        checkAngle(r.getAngle(), FastMath.PI / 3);

        r = new Rotation(Vector3D.PLUS_J, 2 * FastMath.PI + FastMath.PI / 3);
        checkVector(r.getAxis(), Vector3D.PLUS_J);
        checkAngle(r.getAngle(), FastMath.PI / 3);

        r = new Rotation(Vector3D.PLUS_J, 0);
        checkVector(r.getAxis(), Vector3D.PLUS_I);
        checkAngle(r.getAngle(), 0);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ROTATION_CM_COVERAGE}
     * 
     * @description Legacy test.
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void testRevert() {
        final Rotation r = new Rotation(true, 0.001, 0.36, 0.48, 0.8);
        final Rotation reverted = r.revert();
        checkRotation(r.applyTo(reverted), 1, 0, 0, 0);
        checkRotation(reverted.applyTo(r), 1, 0, 0, 0);
        Assert.assertEquals(r.getAngle(), reverted.getAngle(), 1.0e-12);
        Assert.assertEquals(-1, Vector3D.dotProduct(r.getAxis(), reverted.getAxis()), 1.0e-12);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ROTATION_CM_COVERAGE}
     * 
     * @description Legacy test.
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void testVectorOnePair() {

        final Vector3D u = new Vector3D(3, 2, 1);
        final Vector3D v = new Vector3D(-4, 2, 2);
        final Rotation r = new Rotation(u, v);
        checkVector(r.applyTo(u.scalarMultiply(v.getNorm())), v.scalarMultiply(u.getNorm()));

        checkAngle(new Rotation(u, u.negate()).getAngle(), FastMath.PI);

        try {
            new Rotation(u, Vector3D.ZERO);
            Assert.fail("an exception should have been thrown");
        } catch (final MathArithmeticException e) {
            // expected behavior
        }

        checkRotation(new Rotation(u, u), 1, 0, 0, 0);

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ROTATION_CM_COVERAGE}
     * 
     * @description Legacy test.
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void testVectorTwoPairs() {

        final Vector3D u1 = new Vector3D(3, 0, 0);
        final Vector3D u2 = new Vector3D(0, 5, 0);
        final Vector3D v1 = new Vector3D(0, 0, 2);
        final Vector3D v2 = new Vector3D(-2, 0, 2);
        Rotation r = new Rotation(u1, u2, v1, v2);
        checkVector(r.applyTo(Vector3D.PLUS_I), Vector3D.PLUS_K);
        checkVector(r.applyTo(Vector3D.PLUS_J), Vector3D.MINUS_I);

        r = new Rotation(u1, u2, u1.negate(), u2.negate());
        final Vector3D axis = r.getAxis();
        if (Vector3D.dotProduct(axis, Vector3D.PLUS_K) > 0) {
            checkVector(axis, Vector3D.PLUS_K);
        } else {
            checkVector(axis, Vector3D.MINUS_K);
        }
        checkAngle(r.getAngle(), FastMath.PI);

        final double sqrt = MathLib.sqrt(2) / 2;
        r = new Rotation(Vector3D.PLUS_I, Vector3D.PLUS_J,
            new Vector3D(0.5, 0.5, sqrt),
            new Vector3D(0.5, 0.5, -sqrt));
        checkRotation(r, sqrt, -0.5, -0.5, 0);

        r = new Rotation(u1, u2, u1, Vector3D.crossProduct(u1, u2));
        checkRotation(r, sqrt, sqrt, 0, 0);

        checkRotation(new Rotation(u1, u2, u1, u2), 1, 0, 0, 0);

        try {
            new Rotation(u1, u2, Vector3D.ZERO, v2);
            Assert.fail("an exception should have been thrown");
        } catch (final MathArithmeticException e) {
            // expected behavior
        }

        try {
            new Rotation(Vector3D.ZERO, u2, v1, v2);
            Assert.fail("an exception should have been thrown");
        } catch (final MathArithmeticException e) {
            // expected behavior
        }

        try {
            new Rotation(u1, Vector3D.ZERO, v1, v2);
            Assert.fail("an exception should have been thrown");
        } catch (final MathArithmeticException e) {
            // expected behavior
        }

        try {
            new Rotation(u1, u2, v1, Vector3D.ZERO);
            Assert.fail("an exception should have been thrown");
        } catch (final MathArithmeticException e) {
            // expected behavior
        }

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ROTATION_CM_COVERAGE}
     * 
     * @description Legacy test.
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void testMatrix() {

        try {
            new Rotation(new double[][] { { 0.0, 1.0, 0.0 }, { 1.0, 0.0, 0.0 } }, 1.0e-7);
            Assert.fail("Expecting NotARotationMatrixException");
        } catch (final NotARotationMatrixException nrme) {
            // expected behavior
        }

        try {
            new Rotation(new double[][] { { 0.0, 1.0 }, { 1.0, 0.0, 0.0 },
                { 0.0, 0.0, 1.0 } }, 1.0e-7);
            Assert.fail("Expecting NotARotationMatrixException");
        } catch (final NotARotationMatrixException nrme) {
            // expected behavior
        }

        try {
            new Rotation(new double[][] { { 0.0, 1.0, 0.0 }, { 1.0, 0.0 },
                { 0.0, 0.0, 1.0 } }, 1.0e-7);
            Assert.fail("Expecting NotARotationMatrixException");
        } catch (final NotARotationMatrixException nrme) {
            // expected behavior
        }

        try {
            new Rotation(new double[][] { { 0.0, 1.0, 0.0 }, { 1.0, 0.0, 0.0 },
                { 0.0, 0.0 } }, 1.0e-7);
            Assert.fail("Expecting NotARotationMatrixException");
        } catch (final NotARotationMatrixException nrme) {
            // expected behavior
        }

        try {
            new Rotation(new double[][] { { 0.445888, 0.797184, -0.407040 }, { 0.821760, -0.184320, 0.539200 },
                { -0.354816, 0.574912, 0.737280 }
            }, 1.0e-7);
            Assert.fail("Expecting NotARotationMatrixException");
        } catch (final NotARotationMatrixException nrme) {
            // expected behavior
        }

        try {
            new Rotation(new double[][] {
                { 0.4, 0.8, -0.4 },
                { -0.4, 0.6, 0.7 },
                { 0.8, -0.2, 0.5 }
            }, 1.0e-15);
            Assert.fail("Expecting NotARotationMatrixException");
        } catch (final NotARotationMatrixException nrme) {
            // expected behavior
        }

        checkRotation(new Rotation(new double[][] {
            { 0.445888, 0.797184, -0.407040 },
            { -0.354816, 0.574912, 0.737280 },
            { 0.821760, -0.184320, 0.539200 }
        }, 1.0e-10),
            0.8, -0.288, -0.384, -0.36);

        checkRotation(new Rotation(new double[][] {
            { 0.539200, 0.737280, 0.407040 },
            { 0.184320, -0.574912, 0.797184 },
            { 0.821760, -0.354816, -0.445888 }
        }, 1.0e-10),
            0.36, -0.8, -0.288, -0.384);

        checkRotation(new Rotation(new double[][] {
            { -0.445888, 0.797184, -0.407040 },
            { 0.354816, 0.574912, 0.737280 },
            { 0.821760, 0.184320, -0.539200 }
        }, 1.0e-10),
            0.384, -0.36, -0.8, -0.288);

        checkRotation(new Rotation(new double[][] {
            { -0.539200, 0.737280, 0.407040 },
            { -0.184320, -0.574912, 0.797184 },
            { 0.821760, 0.354816, 0.445888 }
        }, 1.0e-10),
            0.288, -0.384, -0.36, -0.8);

        final double[][] m1 = { { 0.0, 1.0, 0.0 },
            { 0.0, 0.0, 1.0 },
            { 1.0, 0.0, 0.0 } };
        Rotation r = new Rotation(m1, 1.0e-7);
        checkVector(r.applyTo(Vector3D.PLUS_I), Vector3D.PLUS_K);
        checkVector(r.applyTo(Vector3D.PLUS_J), Vector3D.PLUS_I);
        checkVector(r.applyTo(Vector3D.PLUS_K), Vector3D.PLUS_J);

        final double[][] m2 = { { 0.83203, -0.55012, -0.07139 },
            { 0.48293, 0.78164, -0.39474 },
            { 0.27296, 0.29396, 0.91602 } };
        r = new Rotation(m2, 1.0e-12);

        final double[][] m3 = r.getMatrix();
        final double d00 = m2[0][0] - m3[0][0];
        final double d01 = m2[0][1] - m3[0][1];
        final double d02 = m2[0][2] - m3[0][2];
        final double d10 = m2[1][0] - m3[1][0];
        final double d11 = m2[1][1] - m3[1][1];
        final double d12 = m2[1][2] - m3[1][2];
        final double d20 = m2[2][0] - m3[2][0];
        final double d21 = m2[2][1] - m3[2][1];
        final double d22 = m2[2][2] - m3[2][2];

        Assert.assertTrue(MathLib.abs(d00) < 6.0e-6);
        Assert.assertTrue(MathLib.abs(d01) < 6.0e-6);
        Assert.assertTrue(MathLib.abs(d02) < 6.0e-6);
        Assert.assertTrue(MathLib.abs(d10) < 6.0e-6);
        Assert.assertTrue(MathLib.abs(d11) < 6.0e-6);
        Assert.assertTrue(MathLib.abs(d12) < 6.0e-6);
        Assert.assertTrue(MathLib.abs(d20) < 6.0e-6);
        Assert.assertTrue(MathLib.abs(d21) < 6.0e-6);
        Assert.assertTrue(MathLib.abs(d22) < 6.0e-6);

        Assert.assertTrue(MathLib.abs(d00) > 4.0e-7);
        Assert.assertTrue(MathLib.abs(d01) > 4.0e-7);
        Assert.assertTrue(MathLib.abs(d02) > 4.0e-7);
        Assert.assertTrue(MathLib.abs(d10) > 4.0e-7);
        Assert.assertTrue(MathLib.abs(d11) > 4.0e-7);
        Assert.assertTrue(MathLib.abs(d12) > 4.0e-7);
        Assert.assertTrue(MathLib.abs(d20) > 4.0e-7);
        Assert.assertTrue(MathLib.abs(d21) > 4.0e-7);
        Assert.assertTrue(MathLib.abs(d22) > 4.0e-7);

        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 3; ++j) {
                final double m3tm3 = m3[i][0] * m3[j][0]
                    + m3[i][1] * m3[j][1]
                    + m3[i][2] * m3[j][2];
                if (i == j) {
                    Assert.assertTrue(MathLib.abs(m3tm3 - 1.0) < 1.0e-10);
                } else {
                    Assert.assertTrue(MathLib.abs(m3tm3) < 1.0e-10);
                }
            }
        }

        checkVector(r.applyTo(Vector3D.PLUS_I),
            new Vector3D(m3[0][0], m3[1][0], m3[2][0]));
        checkVector(r.applyTo(Vector3D.PLUS_J),
            new Vector3D(m3[0][1], m3[1][1], m3[2][1]));
        checkVector(r.applyTo(Vector3D.PLUS_K),
            new Vector3D(m3[0][2], m3[1][2], m3[2][2]));

        final double[][] m4 = { { 1.0, 0.0, 0.0 },
            { 0.0, -1.0, 0.0 },
            { 0.0, 0.0, -1.0 } };
        r = new Rotation(m4, 1.0e-7);
        checkAngle(r.getAngle(), FastMath.PI);

        try {
            final double[][] m5 = { { 0.0, 0.0, 1.0 },
                { 0.0, 1.0, 0.0 },
                { 1.0, 0.0, 0.0 } };
            r = new Rotation(m5, 1.0e-7);
            Assert.fail("got " + r + ", should have caught an exception");
        } catch (final NotARotationMatrixException e) {
            // expected
        }

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ROTATION_CM_COVERAGE}
     * 
     * @description Legacy test.
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void testAngles() {

        final RotationOrder[] CardanOrders = {
            RotationOrder.XYZ, RotationOrder.XZY, RotationOrder.YXZ,
            RotationOrder.YZX, RotationOrder.ZXY, RotationOrder.ZYX
        };

        for (final RotationOrder cardanOrder : CardanOrders) {
            for (double alpha1 = 0.1; alpha1 < 6.2; alpha1 += 0.3) {
                for (double alpha2 = -1.55; alpha2 < 1.55; alpha2 += 0.3) {
                    for (double alpha3 = 0.1; alpha3 < 6.2; alpha3 += 0.3) {
                        final Rotation r = new Rotation(cardanOrder, alpha1, alpha2, alpha3);
                        final double[] angles = r.getAngles(cardanOrder);
                        checkAngle(angles[0], alpha1);
                        checkAngle(angles[1], alpha2);
                        checkAngle(angles[2], alpha3);
                    }
                }
            }
        }

        final RotationOrder[] EulerOrders = {
            RotationOrder.XYX, RotationOrder.XZX, RotationOrder.YXY,
            RotationOrder.YZY, RotationOrder.ZXZ, RotationOrder.ZYZ
        };

        for (final RotationOrder eulerOrder : EulerOrders) {
            for (double alpha1 = 0.1; alpha1 < 6.2; alpha1 += 0.3) {
                for (double alpha2 = 0.05; alpha2 < 3.1; alpha2 += 0.3) {
                    for (double alpha3 = 0.1; alpha3 < 6.2; alpha3 += 0.3) {
                        final Rotation r = new Rotation(eulerOrder,
                            alpha1, alpha2, alpha3);
                        final double[] angles = r.getAngles(eulerOrder);
                        checkAngle(angles[0], alpha1);
                        checkAngle(angles[1], alpha2);
                        checkAngle(angles[2], alpha3);
                    }
                }
            }
        }

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ROTATION_CM_COVERAGE}
     * 
     * @description Legacy test.Check that no exception is thrown when the angles are singular
     *              This test is meant to work only if no exceptions are thrown.
     *              It also checks that values are coherent when using rotation build from axis and angles,
     *              and build from exact quaternion
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void testSingularities() {

        final Map<RotationOrder, Rotation> m = new LinkedHashMap<>();
        final Map<RotationOrder, Rotation> quat = new LinkedHashMap<>();
        final double PI = FastMath.PI;
        final double PIS2 = 0.5 * FastMath.PI;

        // Rotation corresponding to singularities in Euler or Cardan Angles
        // build from a vector and an angle
        m.put(RotationOrder.XYZ, new Rotation(Vector3D.PLUS_J, PIS2));
        m.put(RotationOrder.XYX, new Rotation(Vector3D.PLUS_J, PI));
        m.put(RotationOrder.ZYZ, new Rotation(Vector3D.PLUS_J, PI));
        m.put(RotationOrder.ZYX, new Rotation(Vector3D.PLUS_J, PIS2));
        m.put(RotationOrder.YXZ, new Rotation(Vector3D.PLUS_I, PIS2));
        m.put(RotationOrder.YXY, new Rotation(Vector3D.PLUS_I, PI));
        m.put(RotationOrder.ZXZ, new Rotation(Vector3D.PLUS_I, PI));
        m.put(RotationOrder.ZXY, new Rotation(Vector3D.PLUS_I, PIS2));
        m.put(RotationOrder.XZY, new Rotation(Vector3D.PLUS_K, PIS2));
        m.put(RotationOrder.YZY, new Rotation(Vector3D.PLUS_K, PI));
        m.put(RotationOrder.XZX, new Rotation(Vector3D.PLUS_K, PI));
        m.put(RotationOrder.YZX, new Rotation(Vector3D.PLUS_K, PIS2));

        // build from exact quaternion
        quat.put(RotationOrder.XYZ, new Rotation(false, MathLib.sqrt(2) / 2, 0, MathLib.sqrt(2) / 2, 0));
        quat.put(RotationOrder.XYX, new Rotation(false, 0, 0, 1, 0));
        quat.put(RotationOrder.ZYZ, new Rotation(false, 0, 0, 1, 0));
        quat.put(RotationOrder.ZYX, new Rotation(false, MathLib.sqrt(2) / 2, 0, MathLib.sqrt(2) / 2, 0));
        quat.put(RotationOrder.YXZ, new Rotation(false, MathLib.sqrt(2) / 2, MathLib.sqrt(2) / 2, 0, 0));
        quat.put(RotationOrder.YXY, new Rotation(false, 0, 1, 0, 0));
        quat.put(RotationOrder.ZXZ, new Rotation(false, 0, 1, 0, 0));
        quat.put(RotationOrder.ZXY, new Rotation(false, MathLib.sqrt(2) / 2, MathLib.sqrt(2) / 2, 0, 0));
        quat.put(RotationOrder.XZY, new Rotation(false, MathLib.sqrt(2) / 2, 0, 0, MathLib.sqrt(2) / 2));
        quat.put(RotationOrder.YZY, new Rotation(false, 0, 0, 0, 1));
        quat.put(RotationOrder.XZX, new Rotation(false, 0, 0, 0, 1));
        quat.put(RotationOrder.YZX, new Rotation(false, MathLib.sqrt(2) / 2, 0, 0, MathLib.sqrt(2) / 2));

        double[] angles;
        double[] anglesFromQuat;
        RotationOrder ro;
        Rotation rCurrent;
        Rotation rExpected;

        for (final Entry<RotationOrder, Rotation> entry : m.entrySet()) {

            ro = entry.getKey();
            rExpected = entry.getValue();

            // Transformation Rotation ==> angles
            angles = rExpected.getAngles(ro);
            // Verification of inverse transformation (angles ==> Rotation)
            rCurrent = new Rotation(ro, angles[0], angles[1], angles[2]);

            // Check differences
            Assert.assertEquals(rExpected.applyInverseTo(rCurrent).getAngle(), 0, 1e-30);

            // Compare the angles from the rotation build with axis and angle
            // to the angles from the rotation build with quaternion
            anglesFromQuat = quat.get(ro).getAngles(ro);
            Assert.assertEquals(anglesFromQuat[0], angles[0], 1e-30);
            Assert.assertEquals(anglesFromQuat[1], angles[1], 1e-30);
            Assert.assertEquals(anglesFromQuat[2], angles[2], 1e-30);

            // Transformation of inverse rotation Rotation ==> angles
            angles = rExpected.revert().getAngles(ro);
            // Verification of inverse transformation (angles ==> Rotation)
            rCurrent = new Rotation(ro, angles[0], angles[1], angles[2]);

            // Check differences
            Assert.assertEquals(rExpected.revert().applyInverseTo(rCurrent).getAngle(), 0, 1e-30);
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ROTATION_CM_COVERAGE}
     * 
     * @description Legacy test.Check that no exception is thrown when the angles are singular
     *              This test is meant to work only if no exceptions are thrown.
     *              It also checks that values are coherent when using rotation build from axis and angles,
     *              and build from exact quaternion
     * 
     * @referenceVersion 3.1
     * 
     * @nonRegressionVersion 3.1
     */
    @Test
    public void testOtherSingularities() {

        final Map<RotationOrder, Rotation> m = new LinkedHashMap<>();
        final Map<RotationOrder, Rotation> quat = new LinkedHashMap<>();
        final double PI = FastMath.PI;
        final double PIS2 = 0.5 * FastMath.PI;

        // Rotation corresponding to singularities in Euler or Cardan Angles
        // build from a vector and an angle
        m.put(RotationOrder.XYZ, new Rotation(Vector3D.PLUS_J, -PIS2));
        m.put(RotationOrder.ZYX, new Rotation(Vector3D.PLUS_J, -PIS2));
        m.put(RotationOrder.XZY, new Rotation(Vector3D.PLUS_K, -PIS2));
        m.put(RotationOrder.YZX, new Rotation(Vector3D.PLUS_K, -PIS2));
        m.put(RotationOrder.YXZ, new Rotation(Vector3D.PLUS_I, -PIS2));
        m.put(RotationOrder.ZXY, new Rotation(Vector3D.PLUS_I, -PIS2));

        m.put(RotationOrder.XYX, new Rotation(Vector3D.PLUS_J, PI));
        m.put(RotationOrder.ZYZ, new Rotation(Vector3D.PLUS_J, PI));
        m.put(RotationOrder.XZX, new Rotation(Vector3D.PLUS_K, PI));
        m.put(RotationOrder.YZY, new Rotation(Vector3D.PLUS_K, PI));
        m.put(RotationOrder.YXY, new Rotation(Vector3D.PLUS_I, PI));
        m.put(RotationOrder.ZXZ, new Rotation(Vector3D.PLUS_I, PI));

        // build from exact quaternion
        quat.put(RotationOrder.XYZ, new Rotation(false, MathLib.sqrt(2) / 2, 0, -MathLib.sqrt(2) / 2, 0));
        quat.put(RotationOrder.ZYX, new Rotation(false, MathLib.sqrt(2) / 2, 0, -MathLib.sqrt(2) / 2, 0));
        quat.put(RotationOrder.XZY, new Rotation(false, MathLib.sqrt(2) / 2, 0, 0, -MathLib.sqrt(2) / 2));
        quat.put(RotationOrder.YZX, new Rotation(false, MathLib.sqrt(2) / 2, 0, 0, -MathLib.sqrt(2) / 2));
        quat.put(RotationOrder.YXZ, new Rotation(false, MathLib.sqrt(2) / 2, -MathLib.sqrt(2) / 2, 0, 0));
        quat.put(RotationOrder.ZXY, new Rotation(false, MathLib.sqrt(2) / 2, -MathLib.sqrt(2) / 2, 0, 0));

        quat.put(RotationOrder.XYX, new Rotation(false, 0, 0, 1, 0));
        quat.put(RotationOrder.ZYZ, new Rotation(false, 0, 0, 1, 0));
        quat.put(RotationOrder.XZX, new Rotation(false, 0, 0, 0, 1));
        quat.put(RotationOrder.YZY, new Rotation(false, 0, 0, 0, 1));
        quat.put(RotationOrder.YXY, new Rotation(false, 0, 1, 0, 0));
        quat.put(RotationOrder.ZXZ, new Rotation(false, 0, 1, 0, 0));

        double[] angles;
        double[] anglesFromQuat;
        RotationOrder ro;
        Rotation rCurrent;
        Rotation rExpected;

        for (final Entry<RotationOrder, Rotation> entry : m.entrySet()) {

            ro = entry.getKey();
            rExpected = entry.getValue();

            // Transformation Rotation ==> angles
            angles = rExpected.getAngles(ro);
            // Verification of inverse transformation (angles ==> Rotation)
            rCurrent = new Rotation(ro, angles[0], angles[1], angles[2]);

            // Check differences
            Assert.assertEquals(rExpected.applyInverseTo(rCurrent).getAngle(), 0, 1e-30);

            // Compare the angles from the rotation build with axis and angle
            // to the angles from the rotation build with quaternion
            anglesFromQuat = quat.get(ro).getAngles(ro);
            Assert.assertEquals(anglesFromQuat[0], angles[0], 1e-30);
            Assert.assertEquals(anglesFromQuat[1], angles[1], 1e-30);
            Assert.assertEquals(anglesFromQuat[2], angles[2], 1e-30);

            // Transformation of inverse rotation Rotation ==> angles
            angles = rExpected.revert().getAngles(ro);
            // Verification of inverse transformation (angles ==> Rotation)
            rCurrent = new Rotation(ro, angles[0], angles[1], angles[2]);

            // Check differences
            Assert.assertEquals(rExpected.revert().applyInverseTo(rCurrent).getAngle(), 0, 1e-30);
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ROTATION_CM_COVERAGE}
     * 
     * @description Legacy test.
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void testQuaternion() {

        Rotation r1 = new Rotation(new Vector3D(2, -3, 5), 1.7);
        final double n = 23.5;
        final Rotation r2 = new Rotation(true, n * r1.getQuaternion().getQ0(),
            n * r1.getQuaternion().getQ1(), n * r1.getQuaternion().getQ2(),
            n * r1.getQuaternion().getQ3());
        for (double x = -0.9; x < 0.9; x += 0.2) {
            for (double y = -0.9; y < 0.9; y += 0.2) {
                for (double z = -0.9; z < 0.9; z += 0.2) {
                    final Vector3D u = new Vector3D(x, y, z);
                    checkVector(r2.applyTo(u), r1.applyTo(u));
                }
            }
        }

        r1 = new Rotation(false, 0.288, 0.384, 0.36, 0.8);
        checkRotation(r1, -r1.getQuaternion().getQ0(), -r1.getQuaternion().getQ1(), -r1
            .getQuaternion().getQ2(), -r1.getQuaternion().getQ3());

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ROTATION_CM_COVERAGE}
     * 
     * @description Legacy test.
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void testCompose() {

        final Rotation r1 = new Rotation(new Vector3D(2, -3, 5), 1.7);
        final Rotation r2 = new Rotation(new Vector3D(-1, 3, 2), 0.3);
        final Rotation r3 = r2.applyTo(r1);

        for (double x = -0.9; x < 0.9; x += 0.2) {
            for (double y = -0.9; y < 0.9; y += 0.2) {
                for (double z = -0.9; z < 0.9; z += 0.2) {
                    final Vector3D u = new Vector3D(x, y, z);
                    checkVector(r2.applyTo(r1.applyTo(u)), r3.applyTo(u));
                }
            }
        }

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ROTATION_CM_COVERAGE}
     * 
     * @description Legacy test.
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void testComposeInverse() {

        final Rotation r1 = new Rotation(new Vector3D(2, -3, 5), 1.7);
        final Rotation r2 = new Rotation(new Vector3D(-1, 3, 2), 0.3);
        final Rotation r3 = r2.applyInverseTo(r1);

        for (double x = -0.9; x < 0.9; x += 0.2) {
            for (double y = -0.9; y < 0.9; y += 0.2) {
                for (double z = -0.9; z < 0.9; z += 0.2) {
                    final Vector3D u = new Vector3D(x, y, z);
                    checkVector(r2.applyInverseTo(r1.applyTo(u)), r3.applyTo(u));
                }
            }
        }

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ROTATION_CM_COVERAGE}
     * 
     * @description Legacy test.
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void testApplyInverseTo() {

        Rotation r = new Rotation(new Vector3D(2, -3, 5), 1.7);
        for (double lambda = 0; lambda < 6.2; lambda += 0.2) {
            for (double phi = -1.55; phi < 1.55; phi += 0.2) {
                final Vector3D u = new Vector3D(MathLib.cos(lambda) * MathLib.cos(phi),
                    MathLib.sin(lambda) * MathLib.cos(phi),
                    MathLib.sin(phi));

                checkVector(u, r.applyInverseTo(r.applyTo(u)));
                checkVector(u, r.applyTo(r.applyInverseTo(u)));
            }
        }

        r = Rotation.IDENTITY;
        for (double lambda = 0; lambda < 6.2; lambda += 0.2) {
            for (double phi = -1.55; phi < 1.55; phi += 0.2) {
                final Vector3D u = new Vector3D(MathLib.cos(lambda) * MathLib.cos(phi),
                    MathLib.sin(lambda) * MathLib.cos(phi),
                    MathLib.sin(phi));
                checkVector(u, r.applyInverseTo(r.applyTo(u)));
                checkVector(u, r.applyTo(r.applyInverseTo(u)));
            }
        }

        r = new Rotation(Vector3D.PLUS_K, FastMath.PI);
        for (double lambda = 0; lambda < 6.2; lambda += 0.2) {
            for (double phi = -1.55; phi < 1.55; phi += 0.2) {
                final Vector3D u = new Vector3D(MathLib.cos(lambda) * MathLib.cos(phi),
                    MathLib.sin(lambda) * MathLib.cos(phi),
                    MathLib.sin(phi));
                checkVector(u, r.applyInverseTo(r.applyTo(u)));
                checkVector(u, r.applyTo(r.applyInverseTo(u)));
            }
        }
    }

    /**
     * Calls assertEquals on angles after normalizing them.
     * 
     * @param a1
     *        expected angle.
     * @param a2
     *        actual angle.
     */
    private static void checkAngle(final double a1, final double a2) {
        Assert.assertEquals(a1, MathUtils.normalizeAngle(a2, a1), 1.0e-10);
    }

    /**
     * Calls assertEquals for rotations.
     * 
     * @param r
     *        expected rotation.
     * @param q0
     *        Q0 for the actual rotation.
     * @param q1
     *        Q1 for the actual rotation.
     * @param q2
     *        Q2 for the actual rotation.
     * @param q3
     *        Q3 for the actual rotation.
     */
    private static void checkRotation(final Rotation r, final double q0, final double q1, final double q2,
                                      final double q3) {
        Assert.assertEquals(0, Rotation.distance(r, new Rotation(false, q0, q1, q2, q3)), 1.0e-12);
    }
}
