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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:319:05/03/2015:Corrected Rotation class (Step1)
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.validation.geometry.euclidean.threed;

import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.complex.Quaternion;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Matrix3D;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.NotARotationMatrixException;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.math.util.Precision;

/**
 * @description Validation tests for the object Rotation.
 *              Since there is no proper reference data, the Validate class is not needed.
 * 
 * @author Julie Anton
 * 
 * @version $Id: RotationValidationTest.java 17909 2017-09-11 11:57:36Z bignon $
 * 
 * @since 1.0
 * 
 */
public class RotationValTest {

    /** String constant. */
    private static final String MS2 = "ms";
    /** String constant. */
    private static final String S = "s:";
    /** String constant. */
    private static final String MN2 = "mn:";
    /** String constant. */
    private static final String H2 = "h:";

    /** Features description. */
    public enum features {
        /**
         * @featureTitle validation test
         * 
         * @featureDescription Test the robustness of the rotation.
         * 
         * @coveredRequirements DV-MATHS_360
         */
        ROBUSTNESS,
        /**
         * @featureTitle validation test
         * 
         * @featureDescription Test the performances of the rotation.
         * 
         * @coveredRequirements DV-MATHS_420
         */
        PERFORMANCES,
        /**
         * @featureTitle validation test
         * 
         * @featureDescription Test the algebraic rotation.
         * 
         * @coveredRequirements DV-MATHS_390
         */
        MATH_ROTATION
    }

    /** Used epsilon for double comparison */
    private final double comparisonEpsilon = Precision.DOUBLE_COMPARISON_EPSILON;

    /** Used epsilon for double comparison */
    private final double machineEpsilon = Precision.EPSILON;

    /**
     * @testType TVT
     * 
     * @testedFeature {@link features#ROBUSTNESS}
     * 
     * @testedMethod {@link Rotation#applyTo(Vector3D)}
     * @testedMethod {@link Rotation#applyInverseTo(Vector3D)}
     * 
     * @description Check if the composition of 360 * n (n is an integer) rotations of 1° angle and of axis z = (0,0,1)
     *              is equivalent to the identity.
     * 
     * @input Points : (0,0,1) ; (0,0,-1) and 1000 random points
     * 
     * @output Image points of the previous ones by 360 * n rotations of 1° angle and around z axis
     * 
     * @testPassCriteria The image points should coincide with the initial points with an epsilon of n times 1e-14 on
     *                   each component due to computation errors.
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testApplyTo() {

        // number of trials
        final int n = 1000 * 360;

        // angle of the elementary rotation
        final double angle = MathUtils.DEG_TO_RAD;
        // axis of the elementary rotation
        final Vector3D axis = Vector3D.PLUS_K;
        // rotation
        final Rotation rotation = new Rotation(axis, angle);

        // initial point
        Vector3D initialPoint;
        // image point by the rotations
        Vector3D point;

        // CASE 1 : the initial point is on the axis of the rotation
        initialPoint = new Vector3D(0, 0, 8.2);
        point = initialPoint;
        for (int i = 0; i < n - 10; i++) {
            point = rotation.applyTo(point);
        }
        // the initial point and the image point have the same components (absolute comparison
        // term by term with an epsilon of 10E-14 : epsilon for double comparison of the MathUtils class)
        Assert.assertTrue(this.compareVectors(initialPoint, point, this.comparisonEpsilon));

        // CASE 2 : the initial points are random points
        final Random rand = new Random(54863);

        for (int j = 0; j < 1000; j++) {

            initialPoint = new Vector3D(rand.nextDouble(), rand.nextDouble(), rand.nextDouble());
            point = initialPoint;
            for (int i = 0; i < n; i++) {
                point = rotation.applyTo(point);
            }

            // the initial point and the image point are not considered to be the same, they do not have
            // the same components (absolute comparison term by term) if the allowed epsilon is 1E-14 ie the
            // epsilon for double comparison of the MathUtils class
            Assert.assertFalse(this.compareVectors(initialPoint, point, this.comparisonEpsilon));
            // the initial point and the image point are considered to be the same, they have
            // the same components (absolute comparison term by term) if the allowed epsilon is n times
            // the epsilon for double comparison of the MathUtils class (below that value the points cannot
            // be considered to be the same)
            Assert.assertTrue(this.compareVectors(initialPoint, point, n * this.comparisonEpsilon));
        }

    }

    /**
     * @testType TVT
     * 
     * @testedFeature {@link features#ROBUSTNESS}
     * 
     * @testedMethod {@link Rotation#applyTo(Rotation)}
     * @testedMethod {@link Rotation#applyTo(Vector3D)}
     * 
     * @description Check if the composition of 1000 random rotations give the same result as these 1000 random
     *              rotations successively applied on one point.
     * 
     * @input The test is done on 10000 random points.
     * 
     * @output Image points by the rotations
     * 
     * @testPassCriteria The image point given by the composition of 1000 random rotations should be the same as the one
     *                   given by these 1000 random rotations applied successively with an epsilon of the number of
     *                   compositions times 1e-14 which is due to the computation errors.
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testComposition() {
        // number of rotations
        final int numberOfRotations = 1000;
        // array of random rotations
        final Rotation[] table = new Rotation[numberOfRotations];

        final Random randRotation = new Random(54863);
        // final rotation : result of the composition of the previous random rotations
        Rotation finalRotation;

        // filling of the array of random rotations
        Quaternion quat = new Quaternion(randRotation.nextDouble(), randRotation.nextDouble(),
            randRotation.nextDouble(), randRotation.nextDouble());
        quat = quat.normalize();
        table[0] = new Rotation(false, quat);
        finalRotation = table[0];
        for (int i = 1; i < numberOfRotations; i++) {
            quat = new Quaternion(randRotation.nextDouble(), randRotation.nextDouble(), randRotation.nextDouble(),
                randRotation.nextDouble());
            quat = quat.normalize();
            table[i] = new Rotation(false, quat);
            // creation of the rotation resulting from the composition of the random rotations
            finalRotation = table[i].applyTo(finalRotation);
        }

        Vector3D initialPoint;
        Vector3D point;
        // trial on 10000 random points
        final Random randVector = new Random(563);
        for (int j = 0; j < 10000; j++) {
            initialPoint = new Vector3D(randVector.nextDouble(), randVector.nextDouble(), randVector.nextDouble());
            point = initialPoint;
            // computation of the image point by the random rotations applied successively
            for (int i = 0; i < numberOfRotations; i++) {
                point = table[i].applyTo(point);
            }
            // the image point previously obtained should be the same as the image point obtained with
            // the rotation which results from the composition of the random rotations
            // Note that the image point are considered to be the same (absolute comparison term by term)
            // if the allowed epsilon is 10 times the epsilon for double comparison of the MathUtils class
            Assert.assertTrue(this.compareVectors(point, finalRotation.applyTo(initialPoint), numberOfRotations
                * this.comparisonEpsilon));
        }
    }

    /**
     * @testType PT
     * 
     * @testedFeature {@link features#PERFORMANCES}
     * 
     * @testedMethod {@link Rotation#applyTo(Vector3D)}
     * 
     * @description Compare the execution time when the rotation is implemented as a matrix with the object Matrix3D
     *              {@link Matrix3D} with the execution time when the rotation is implemented through the object
     *              Rotation {@link Rotation}.
     * 
     * @input The test is done with the rotation of axis i=(1,0,0) and angle PI/3, the initial point of which the image
     *        by the previous rotation will be computed.
     * 
     * @output Vector3D
     * 
     * @testPassCriteria The image point given by the composition of 500000000 rotations implemented with the object
     *                   Rotation {@link Rotation} should be the same as the one given by 500000000 rotations
     *                   implemented with the object Matrix3D {@link Matrix3D} with an epsilon of the number of
     *                   compositions times 1e-14 which is due to computation errors.
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testPerformances() {
        // rotation matrix : rotation of angle PI/3 and axis i=(1,0,0)
        final double[][] table = { { 1, 0, 0 }, { 0, MathLib.cos(FastMath.PI / 3), -MathLib.sin(FastMath.PI / 3) },
            { 0, MathLib.sin(FastMath.PI / 3), MathLib.cos(FastMath.PI / 3) } };

        final double numberOfCompositions = 500000000;

        // initial point
        final Vector3D initialPoint = new Vector3D(1.4, -0.05, 4.2);

        // case 1 : implementation of the rotation as a matrix with the object RealMatrix

        // rotation matrix
        final Matrix3D matrix = new Matrix3D(table);

        // point of which the image will be computed by the rotation
        Vector3D pointA = initialPoint;

        // tic
        long start = System.currentTimeMillis();
        // successive computations of the image point of the initial point described above by the rotation
        for (int i = 0; i < numberOfCompositions; i++) {
            // image point by the previous rotation
            pointA = matrix.multiply(pointA);
        }
        // toc
        long duree = System.currentTimeMillis() - start;
        // display
        long h = duree / 3600000;
        long mn = (duree % 3600000) / 60000;
        long sec = (duree % 60000) / 1000;
        long ms = (duree % 1000);
        // execution time
        System.out.println("execution time (matrix implementation)= " + h + H2 + mn + MN2 + sec + S + ms + MS2);

        // case 2 : implementation of the rotation with the object Rotation

        // vector of which the image will be computed by the rotation
        Vector3D pointB = initialPoint;
        try {
            // rotation with the object Rotation
            final Rotation rotation = new Rotation(table, 1E-10);

            start = System.currentTimeMillis();

            // successive computations of the image point of the initial point described above by the rotation
            for (int i = 0; i < numberOfCompositions; i++) {
                // image point by the previous rotation
                pointB = rotation.applyTo(pointB);
            }
            // toc
            duree = System.currentTimeMillis() - start;
            // display
            h = duree / 3600000;
            mn = (duree % 3600000) / 60000;
            sec = (duree % 60000) / 1000;
            ms = (duree % 1000);
            // execution time
            System.out.println("execution time (rotation implementation)= " + h + H2 + mn + MN2 + sec + S + ms
                + MS2);

        } catch (final NotARotationMatrixException ex) {
            Assert.fail("not a rotation matrix");
        }
        Assert.assertTrue(this.compareVectors(pointA, pointB, numberOfCompositions * this.comparisonEpsilon));
    }

    /**
     * @testType TVT
     * 
     * @testedFeature {@link features#MATH_ROTATION}
     * 
     * @testedMethod {@link Rotation#applyTo(Vector3D)}
     * @testedMethod {@link Rotation#applyInverseTo(Vector3D)}
     * 
     * @description The notion of convention has been deleted : an algebraic rotation is performed.
     * 
     * @input Quaternion of rotation Q such as the rotation is the rotation of axis k (0,0,1) and angle 30°
     * 
     * @output <p>
     *         Pur quaternion v' image of the vector v by the rotation previously described through the method
     *         {@link Rotation#applyTo(Vector3D)},
     *         </p>
     *         <p>
     *         Pur quaternion v' image of the vector v by the rotation previously described through the method
     *         {@link Rotation#applyInverseTo(Vector3D)},
     *         </p>
     *         <p>
     *         Pur quaternion v' image of v given by the formula : v' = qvq-1
     *         </p>
     * 
     * @testPassCriteria <p>
     *                   The pur quaternion v' image of v given by the rotation through the method
     *                   {@link Rotation#applyTo(Vector3D)} should be the same as the one given by the formula : v' =
     *                   qvq-1 with an epsilon of 1e-16 on the scalar part due to machine errors only and an epsilon of
     *                   1e-14 on the vector part components.
     *                   </p>
     *                   <p>
     *                   The pur quaternion v' image of v given by the rotation through the method
     *                   {@link Rotation#applyInverseTo(Vector3D)} should be the same as the one given by the formula :
     *                   v' = q-1vq with an epsilon of 1e-16 on the scalar part due to machine errors only and an
     *                   epsilon of 1e-14 on the vector part components.
     *                   </p>
     * 
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testConvention() {

        // number of trials
        final int numberOfTrials = 1000;

        // rotation
        final Rotation rotation = new Rotation(Vector3D.PLUS_K, FastMath.PI / 6);
        // quaternion of rotation
        final Quaternion q = rotation.getQuaternion();

        // inverse quaternion of the previous one
        final Quaternion qInv = q.getConjugate();

        // vector of which the image by the rotation will be computed
        Vector3D v;

        // product : q v qInv
        Quaternion productQQinv;

        // product : qInv v q
        Quaternion productQinvQ;

        // result of the product q v qInv
        Vector3D resultQQinv;

        // result of the product qInv v q
        Vector3D resultQinvQ;

        // the test is performed on 1000 random vectors
        final Random r = new Random(12847);

        for (int i = 0; i < numberOfTrials; i++) {

            // vector of which the image by the rotation will be computed
            v = new Vector3D(r.nextDouble(), r.nextDouble(), r.nextDouble());

            // computation of the product : q v qInv
            productQQinv = Quaternion.multiply(q, Quaternion.multiply(
                new Quaternion(0, v.getX(), v.getY(), v.getZ()),
                qInv));
            resultQQinv = new Vector3D(productQQinv.getVectorPart());

            // computation of the product : qInv v q
            productQinvQ = Quaternion.multiply(qInv, Quaternion.multiply(
                new Quaternion(0, v.getX(), v.getY(), v.getZ()),
                q));
            resultQinvQ = new Vector3D(productQinvQ.getVectorPart());

            // the quaternion resulting from the previous product should be a pure quaternion
            Assert.assertEquals(0, productQQinv.getScalarPart(), this.machineEpsilon);
            Assert.assertEquals(0, productQinvQ.getScalarPart(), this.machineEpsilon);

            // the test should be true : q v qInv is equivalent to applyTo()
            Assert.assertTrue(this.compareVectors(resultQQinv, rotation.applyTo(v), this.comparisonEpsilon));
            // the test should be true : qInv v q is equivalent to applyInverseTo()
            Assert.assertTrue(this.compareVectors(resultQinvQ, rotation.applyInverseTo(v), this.comparisonEpsilon));
        }
    }

    /**
     * @description compare two points term by term with an absolute threshold
     * 
     * @param vec1
     *        first point
     * @param vec2
     *        second point
     * @param eps
     *        threshold
     * @return true if the difference between the components of the two points is below the threshold, false otherwise
     * 
     * @since 1.0
     */
    private boolean compareVectors(final Vector3D vec1, final Vector3D vec2, final double eps) {
        boolean result;
        // difference between the first components
        if (!Precision.equals(vec1.getX(), vec2.getX(), eps)) {
            result = false;
        }
        // difference between the second components
        else if (!Precision.equals(vec1.getY(), vec2.getY(), eps)) {
            result = false;
        }
        // difference between the third components
        else if (!Precision.equals(vec1.getZ(), vec2.getZ(), eps)) {
            result = false;
        } else {
            result = true;
        }
        return result;
    }
}
