/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 *
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
 * VERSION:4.13:DM:DM-5:08/12/2023:[PATRIUS] Orientation d'un corps celeste sous forme de quaternions
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:FA:FA-2398:27/05/2020:incoherence de seuils de norme entre AngularCoordinates et Rotation 
 * VERSION:4.5:DM:DM-2456:27/05/2020:optimisation des rotations dans le cas identite
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:134:11/09/2013:Methods deprecated
 * VERSION::DM:84:08/11/2013:slerp method has been modified in order to manage interpolation
 * on the borders
 * VERSION::DM:134:16/12/2013:Methods deprecated (previous commit forgot getQ3())
 * VERSION::FA:210:12/03/2014:Modified slerp algorithm to take shortest path
 * VERSION::FA:183:14/03/2014:Completed javadoc to explain the difference between CLASSICAL and CCSDS
 * VERSION::DM:319:05/03/2015:Corrected Rotation class (Step1 + Step2)
 * VERSION::DM:342:05/03/2015:No exceptions thrown for singular Euler or Cardan angles
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::FA:422:29/04/2015:Exception thrown if quaternion norm is 0
 * VERSION::FA:509:15/10/2015:Protection of inverted trigonometric function call
 * VERSION::FA:608:29/07/2016: NaN produced by slerp() method
 * VERSION::FA:765:02/01/2017:Bad angles retrieved by getAngles()
 * VERSION::FA:1474:19/03/2018: Unnecessary computation in the constructor
 * VERSION::FA:1970:03/01/2019:quality corrections
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.math.geometry.euclidean.threed;

import java.io.Serializable;
import java.util.Objects;

import fr.cnes.sirius.patrius.math.complex.Quaternion;
import fr.cnes.sirius.patrius.math.exception.MathArithmeticException;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathArrays;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

//CHECKSTYLE: stop MagicNumber check
//CHECKSTYLE: stop NestedBlockDepth check
//CHECKSTYLE: stop CommentRatio check
//Reason: model - Commons-Math code kept as such

/**
 * This class implements rotations in a three-dimensional space.
 * 
 * <p>
 * Rotations can be represented by several different mathematical entities (matrices, axis and angle, Cardan or Euler
 * angles, quaternions). This class presents an higher level abstraction, more user-oriented and hiding this
 * implementation details. Well, for the curious, we use normalized rotation quaternions for the internal
 * representation. The user can build a rotation from any of these representations, and any of these representations can
 * be retrieved from a <code>Rotation</code> instance (see the various constructors and getters). In addition, a
 * rotation can also be built implicitly from a set of vectors and their image.
 * </p>
 * <p>
 * This implies that this class can be used to convert from one representation to another one. For example, converting a
 * rotation matrix into a set of Cardan angles can be done using the following single line of code:
 * </p>
 * 
 * <pre>
 * double[] angles = new Rotation(matrix, 1.0e-10).getAngles(RotationOrder.XYZ);
 * </pre>
 * <p>
 * A rotation is an <em>operator</em> which basically rotates three dimensional {@link Vector3D
 * vectors} into other three dimensional {@link Vector3D vectors} using {@link #applyTo(Vector3D)
 * applyTo(Vector3D)}.
 * </p>
 * <p>
 * For example, the image of vector A using the rotation r could be obtained by : B = r.applyTo(A).
 * </p>
 * 
 * <p>
 * Since a rotation is basically a vectorial operator, several rotations can be composed together and the composite
 * operation <code>r = r<sub>1</sub> o
 * r<sub>2</sub></code> (which means that for each vector <code>u</code>,
 * <code>r(u) = r<sub>1</sub>(r<sub>2</sub>(u))</code>) is also a rotation. Hence we can consider that in addition to
 * vectors, a rotation can be applied to other rotations as well (or to itself). We can apply <code>r<sub>1</sub></code>
 * to <code>r<sub>2</sub></code> and the result we get is <code>r = r<sub>1</sub> o r<sub>2</sub></code>. This rotation
 * could be applied to vector A like this : B = r.applyTo(A).
 * </p>
 * 
 * <p>
 * The rotation can be used to change the basis of a vector using {@link #applyInverseTo(Vector3D)
 * applyInverseTo(Vector3D)}. For example, r12 represents the orientation of frame R2 with respect to frame R1: The
 * image of a vector A expressed in frame R2 is : B = r12.applyInverseTo(A).
 * <p>
 * A rotation can be considered as a change of basis from R1 to R2 as follow : r_r2 = r12.applyTo(r_R1.revert()).
 * </p>
 * <p>
 * Rotations are guaranteed to be immutable objects.
 * </p>
 * 
 * @version $Id: Rotation.java 18108 2017-10-04 06:45:27Z bignon $
 * @see Vector3D
 * @see RotationOrder
 * @since 1.2
 */

public class Rotation implements Serializable {

    /** Identity rotation. */
    public static final Rotation IDENTITY = new Rotation(false, 1.0, 0.0, 0.0, 0.0);

     /** Serializable UID. */
    private static final long serialVersionUID = -2153622329907944313L;

    /** -0. */
    private static final double MINUS_ZERO = -0;

    /** 0.5. */
    private static final double HALF = 0.5;

    /** eps * eps. */
    private static final double EPSILON2 = Precision.EPSILON * Precision.EPSILON;

    /** Scalar coordinate of the quaternion. */
    private final double q0;

    /** First coordinate of the vectorial part of the quaternion. */
    private final double q1;

    /** Second coordinate of the vectorial part of the quaternion. */
    private final double q2;

    /** Third coordinate of the vectorial part of the quaternion. */
    private final double q3;

    /**
     * Build a rotation from the quaternion coordinates.
     * <p>
     * A rotation can be built from a <em>normalized</em> quaternion, i.e. a quaternion for which
     * q<sub>0</sub><sup>2</sup> + q<sub>1</sub><sup>2</sup> + q<sub>2</sub><sup>2</sup> + q<sub>3</sub><sup>2</sup> =
     * 1. If the quaternion is not normalized, the constructor can normalize it in a preprocessing step.
     * </p>
     * <p>
     * Note that some conventions put the scalar part of the quaternion as the 4<sup>th</sup> component and the vector
     * part as the first three components. This is <em>not</em> our convention. We put the scalar part as the first
     * component. As a consequence, the normalized quaternion coordinates are defined as :
     * </p>
     * <p>
     * q<sub>0</sub> = cos(&theta;/2)
     * </p>
     * <p>
     * q<sub>1</sub> = x * sin(&theta;/2)
     * </p>
     * <p>
     * q<sub>2</sub> = y * sin(&theta;/2)
     * </p>
     * <p>
     * q<sub>3</sub> = z * sin(&theta;/2) }
     * </p>
     * <p>
     * &theta; beeing the oriented rotation angle around the axis (x, y, z).
     * </p>
     * 
     * @param needsNormalization if true, the coordinates are considered not to be normalized, a
     *        normalization preprocessing step is performed before using them
     * @param q0In scalar part of the quaternion
     * @param q1In first coordinate of the vectorial part of the quaternion
     * @param q2In second coordinate of the vectorial part of the quaternion
     * @param q3In third coordinate of the vectorial part of the quaternion
     * @exception MathIllegalArgumentException thrown if norm of the quaternion is zero
     * @since 3.0
     */
    public Rotation(final boolean needsNormalization, final double q0In, final double q1In,
                    final double q2In, final double q3In) {

        double q0Tmp = q0In;
        double q1Tmp = q1In;
        double q2Tmp = q2In;
        double q3Tmp = q3In;

        if (needsNormalization) {
            // norm computation
            final double norm2 = q0In * q0In + q1In * q1In + q2In * q2In + q3In * q3In;
            if (norm2 < EPSILON2) {
                throw new MathIllegalArgumentException(PatriusMessages.ZERO_NORM);
            }

            if (norm2 != 1) {
                // normalization preprocessing
                final double inv = 1.0 / MathLib.sqrt(norm2);
                q0Tmp *= inv;
                q1Tmp *= inv;
                q2Tmp *= inv;
                q3Tmp *= inv;
            }
        }

        this.q0 = q0Tmp;
        this.q1 = q1Tmp;
        this.q2 = q2Tmp;
        this.q3 = q3Tmp;
    }

    /**
     * Build a rotation from the quaternion.
     * <p>
     * A rotation can be built from a <em>normalized</em> quaternion, i.e. a quaternion for which
     * q<sub>0</sub><sup>2</sup> + q<sub>1</sub><sup>2</sup> + q<sub>2</sub><sup>2</sup> + q<sub>3</sub><sup>2</sup> =
     * 1. If the quaternion is not normalized, the constructor can normalize it in a preprocessing step.
     * </p>
     * The normalized coordinates of this quaternion are defined as :</p>
     * <p>
     * q<sub>0</sub> = cos(&theta;/2)
     * </p>
     * <p>
     * q<sub>1</sub> = x * sin(&theta;/2)
     * </p>
     * <p>
     * q<sub>2</sub> = y * sin(&theta;/2)
     * </p>
     * <p>
     * q<sub>3</sub> = z * sin(&theta;/2) }
     * </p>
     * <p>
     * &theta; beeing the oriented rotation angle around the axis (x, y, z).
     * </p>
     * 
     * @param needsNormalization if true, the coordinates are considered not to be normalized, a
     *        normalization preprocessing step is performed before using them
     * @param quaternion the quaternion of rotation
     * @exception MathIllegalArgumentException thrown if norm of the quaternion is zero
     * @see Quaternion
     * @since 3.0
     */
    public Rotation(final boolean needsNormalization, final Quaternion quaternion) {
        this(needsNormalization, quaternion.getQ0(), quaternion.getQ1(), quaternion.getQ2(),
                quaternion.getQ3());
    }

    /**
     * Build a rotation from the quaternion coordinates.
     * <p>
     * A rotation can be built from a <em>normalized</em> quaternion, i.e. a quaternion for which
     * q<sub>0</sub><sup>2</sup> + q<sub>1</sub><sup>2</sup> + q<sub>2</sub><sup>2</sup> + q<sub>3</sub><sup>2</sup> =
     * 1. If the quaternion is not normalized, the constructor can normalize it in a preprocessing step.
     * </p>
     * <p>
     * Note that some conventions put the scalar part of the quaternion as the 4<sup>th</sup> component and the vector
     * part as the first three components. This is <em>not</em> our convention. We put the scalar part as the first
     * component. As a consequence, the normalized coordinates {q<sub>0</sub>, q<sub>1</sub>, q<sub>2</sub>,
     * q<sub>3</sub>} are defined as :
     * </p>
     * <p>
     * q<sub>0</sub> = cos(&theta;/2)
     * </p>
     * <p>
     * q<sub>1</sub> = x * sin(&theta;/2)
     * </p>
     * <p>
     * q<sub>2</sub> = y * sin(&theta;/2)
     * </p>
     * <p>
     * q<sub>3</sub> = z * sin(&theta;/2) }
     * </p>
     * <p>
     * &theta; beeing the oriented rotation angle around the axis (x, y, z).
     * </p>
     * 
     * @param needsNormalization if true, the coordinates are considered not to be normalized, a
     *        normalization preprocessing step is performed before using them
     * @param q the quaternion of rotation
     * @exception MathIllegalArgumentException thrown if norm of the quaternion is zero
     * @since 3.0
     */
    public Rotation(final boolean needsNormalization, final double[] q) {
        this(needsNormalization, q[0], q[1], q[2], q[3]);
    }

    /**
     * Build a rotation from an axis and an angle.
     * <p>
     * We use the convention that angles are oriented according to the effect of the rotation on vectors around the
     * axis. That means that if (i, j, k) is a direct frame and if we first provide +k as the axis and &pi;/2 as the
     * angle to this constructor, and then {@link #applyTo(Vector3D) apply} the instance to +i, we will get +j.
     * </p>
     * <p>
     * Another way to represent our convention is to say that a rotation of angle &theta; about the unit vector (x, y,
     * z) is the same as the rotation build from quaternion components { cos(&theta;/2), x * sin(&theta;/2), y *
     * sin(&theta;/2), z * sin(&theta;/2) }. No minus sign on the angle!
     * </p>
     * 
     * @param axis axis around which to rotate. Warning: if norm of axis is zero, Identity rotation is built
     * @param angle rotation angle.
     * @exception MathIllegalArgumentException if the axis norm is zero
     */
    public Rotation(final Vector3D axis, final double angle) {

        final double norm = axis.getNorm();
        if (norm == 0) {
            // Null norm: rotation is considered to be the identity
            this.q0 = 1.0;
            this.q1 = 0.0;
            this.q2 = 0.0;
            this.q3 = 0.0;
        } else {
            // Standard case
            final double halfAngle = 0.5 * angle;
            final double coeff = MathLib.sin(halfAngle) / norm;

            this.q0 = MathLib.cos(halfAngle);
            this.q1 = coeff * axis.getX();
            this.q2 = coeff * axis.getY();
            this.q3 = coeff * axis.getZ();
        }
    }

    /**
     * Build a rotation from a 3X3 matrix.
     * 
     * <p>
     * Rotation matrices are orthogonal matrices, i.e. unit matrices (which are matrices for which m.m<sup>T</sup> = I)
     * with real coefficients. The module of the determinant of unit matrices is 1, among the orthogonal 3X3 matrices,
     * only the ones having a positive determinant (+1) are rotation matrices.
     * </p>
     * 
     * <p>
     * When a rotation is defined by a matrix with truncated values (typically when it is extracted from a technical
     * sheet where only four to five significant digits are available), the matrix is not orthogonal anymore. This
     * constructor handles this case transparently by using a copy of the given matrix and applying a correction to the
     * copy in order to perfect its orthogonality. If the Frobenius norm of the correction needed is above the given
     * threshold, then the matrix is considered to be too far from a true rotation matrix and an exception is thrown.
     * <p>
     * 
     * @param m rotation matrix
     * @param threshold convergence threshold for the iterative orthogonality correction
     *        (convergence is reached when the difference between two steps of the Frobenius norm of
     *        the correction is below this threshold)
     * 
     * @exception NotARotationMatrixException if the matrix is not a 3X3 matrix, or if it cannot be
     *            transformed into an orthogonal matrix with the given threshold, or if the
     *            determinant of the resulting orthogonal matrix is negative
     */
    public Rotation(final double[][] m, final double threshold) {

        // dimension check
        if ((m.length != 3) || (m[0].length != 3) || (m[1].length != 3) || (m[2].length != 3)) {
            throw new NotARotationMatrixException(PatriusMessages.ROTATION_MATRIX_DIMENSIONS,
                m.length, m[0].length);
        }

        // compute a "close" orthogonal matrix
        final double[][] ort = orthogonalizeMatrix(m, threshold);

        // check the sign of the determinant
        final double det =
            ort[0][0] * (ort[1][1] * ort[2][2] - ort[2][1] * ort[1][2]) - ort[1][0]
                    * (ort[0][1] * ort[2][2] - ort[2][1] * ort[0][2]) + ort[2][0]
                            * (ort[0][1] * ort[1][2] - ort[1][1] * ort[0][2]);
        if (det < 0.0) {
            throw new NotARotationMatrixException(
                PatriusMessages.CLOSEST_ORTHOGONAL_MATRIX_HAS_NEGATIVE_DETERMINANT, det);
        }

        final double[] quat = mat2quat(ort);
        this.q0 = quat[0];
        this.q1 = quat[1];
        this.q2 = quat[2];
        this.q3 = quat[3];
    }

    /**
     * Build the rotation that transforms a pair of vector into another pair.
     * 
     * <p>
     * Except for possible scale factors, if the instance were applied to the pair (u<sub>1</sub>, u<sub>2</sub>) it
     * will produce the pair (v<sub>1</sub>, v<sub>2</sub>).
     * </p>
     * 
     * <p>
     * If the angular separation between u<sub>1</sub> and u<sub>2</sub> is not the same as the angular separation
     * between v<sub>1</sub> and v<sub>2</sub>, then a corrected v'<sub>2</sub> will be used rather than v<sub>2</sub>,
     * the corrected vector will be in the (v<sub>1</sub>, v<sub>2</sub>) plane.
     * </p>
     * 
     * @param u1In first vector of the origin pair
     * @param u2In second vector of the origin pair
     * @param v1In desired image of u1 by the rotation
     * @param v2In desired image of u2 by the rotation
     * @exception MathArithmeticException if the norm of one of the vectors is zero, or if one of
     *            the pair is degenerated (i.e. the vectors of the pair are colinear)
     */
    public Rotation(final Vector3D u1In, final Vector3D u2In, final Vector3D v1In,
            final Vector3D v2In) {

        Vector3D u1 = u1In;
        Vector3D u2 = u2In;
        Vector3D v1 = v1In;
        Vector3D v2 = v2In;

        // build orthonormalized base from u1, u2
        // this fails when vectors are null or colinear, which is forbidden to
        // define a rotation
        final Vector3D u3 = u1.crossProduct(u2).normalize();
        u2 = u3.crossProduct(u1).normalize();
        u1 = u1.normalize();

        // build an orthonormalized base from v1, v2
        // this fails when vectors are null or colinear, which is forbidden to
        // define a rotation
        final Vector3D v3 = v1.crossProduct(v2).normalize();
        v2 = v3.crossProduct(v1).normalize();
        v1 = v1.normalize();

        // buid a matrix transforming the first base into the second one
        final double[][] m =
            new double[][] {
                {
                    MathArrays.linearCombination(u1.getX(), v1.getX(), u2.getX(), v2.getX(),
                        u3.getX(), v3.getX()),
                    MathArrays.linearCombination(u1.getY(), v1.getX(), u2.getY(), v2.getX(),
                        u3.getY(), v3.getX()),
                    MathArrays.linearCombination(u1.getZ(), v1.getX(), u2.getZ(), v2.getX(),
                        u3.getZ(), v3.getX()) },
                {
                    MathArrays.linearCombination(u1.getX(), v1.getY(), u2.getX(), v2.getY(),
                        u3.getX(), v3.getY()),
                    MathArrays.linearCombination(u1.getY(), v1.getY(), u2.getY(), v2.getY(),
                        u3.getY(), v3.getY()),
                    MathArrays.linearCombination(u1.getZ(), v1.getY(), u2.getZ(), v2.getY(),
                        u3.getZ(), v3.getY()) },
                {
                    MathArrays.linearCombination(u1.getX(), v1.getZ(), u2.getX(), v2.getZ(),
                        u3.getX(), v3.getZ()),
                    MathArrays.linearCombination(u1.getY(), v1.getZ(), u2.getY(), v2.getZ(),
                        u3.getY(), v3.getZ()),
                    MathArrays.linearCombination(u1.getZ(), v1.getZ(), u2.getZ(), v2.getZ(),
                        u3.getZ(), v3.getZ()) } };

        final double[] quat = mat2quat(m);
        this.q0 = quat[0];
        this.q1 = quat[1];
        this.q2 = quat[2];
        this.q3 = quat[3];
    }

    /**
     * Build one of the rotations that transform one vector into another one.
     * 
     * <p>
     * Except for a possible scale factor, if the instance were applied to the vector u it will produce the vector v.
     * There is an infinite number of such rotations, this constructor choose the one with the smallest associated angle
     * (i.e. the one whose axis is orthogonal to the (u, v) plane). If u and v are colinear, an arbitrary rotation axis
     * is chosen.
     * </p>
     * 
     * @param u origin vector
     * @param v desired image of u by the rotation
     * @exception MathArithmeticException if the norm of one of the vectors is zero
     */
    public Rotation(final Vector3D u, final Vector3D v) {

        final double normProduct = u.getNorm() * v.getNorm();
        if (normProduct < Precision.EPSILON) {
            throw new MathArithmeticException(
                PatriusMessages.ZERO_NORM_FOR_ROTATION_DEFINING_VECTOR);
        }

        final double dot = u.dotProduct(v);

        if (dot < ((2.0e-15 - 1.0) * normProduct)) {
            // special case u = -v: we select a PI angle rotation around
            // an arbitrary vector orthogonal to u
            final Vector3D w = u.orthogonal();
            this.q0 = 0.0;
            this.q1 = w.getX();
            this.q2 = w.getY();
            this.q3 = w.getZ();
        } else {
            // general case: (u, v) defines a plane, we select
            // the shortest possible rotation: axis orthogonal to this plane
            this.q0 = MathLib.sqrt(HALF * (1.0 + dot / normProduct));
            final double coeff = 1.0 / (2.0 * this.q0 * normProduct);
            final Vector3D q = u.crossProduct(v);
            this.q1 = coeff * q.getX();
            this.q2 = coeff * q.getY();
            this.q3 = coeff * q.getZ();
        }
    }

    /**
     * Build a rotation from three Cardan or Euler elementary rotations.
     * 
     * <p>
     * Cardan rotations are three successive rotations around the canonical axes X, Y and Z, each axis being used once.
     * There are 6 such sets of rotations (XYZ, XZY, YXZ, YZX, ZXY and ZYX). Euler rotations are three successive
     * rotations around the canonical axes X, Y and Z, the first and last rotations being around the same axis. There
     * are 6 such sets of rotations (XYX, XZX, YXY, YZY, ZXZ and ZYZ), the most popular one being ZXZ.
     * </p>
     * <p>
     * Beware that many people routinely use the term Euler angles even for what really are Cardan angles (this
     * confusion is especially widespread in the aerospace business where Roll, Pitch and Yaw angles are often wrongly
     * tagged as Euler angles).
     * </p>
     * 
     * @param order order of rotations to use
     * @param alpha1 angle of the first elementary rotation
     * @param alpha2 angle of the second elementary rotation
     * @param alpha3 angle of the third elementary rotation
     */
    public Rotation(final RotationOrder order, final double alpha1, final double alpha2,
            final double alpha3) {
        final Rotation r1 = new Rotation(order.getA1(), alpha1);
        final Rotation r2 = new Rotation(order.getA2(), alpha2);
        final Rotation r3 = new Rotation(order.getA3(), alpha3);
        final Rotation composed = r1.applyTo(r2.applyTo(r3));
        this.q0 = composed.q0;
        this.q1 = composed.q1;
        this.q2 = composed.q2;
        this.q3 = composed.q3;
    }

    /**
     * Convert an orthogonal rotation matrix to a quaternion.
     * 
     * @param ort orthogonal rotation matrix
     * @return quaternion corresponding to the matrix
     */
    private static double[] mat2quat(final double[][] ort) {

        final double[] quat = new double[4];

        // There are different ways to compute the quaternions elements
        // from the matrix. They all involve computing one element from
        // the diagonal of the matrix, and computing the three other ones
        // using a formula involving a division by the first element,
        // which unfortunately can be zero. Since the norm of the
        // quaternion is 1, we know at least one element has an absolute
        // value greater or equal to 0.5, so it is always possible to
        // select the right formula and avoid division by zero and even
        // numerical inaccuracy. Checking the elements in turn and using
        // the first one greater than 0.45 is safe (this leads to a simple
        // test since qi = 0.45 implies 4 qi^2 - 1 = -0.19)
        double s = ort[0][0] + ort[1][1] + ort[2][2];
        if (s > -0.19) {
            // compute q0 and deduce q1, q2 and q3
            quat[0] = HALF * MathLib.sqrt(s + 1.0);
            final double inv = 0.25 / quat[0];
            quat[1] = inv * (ort[2][1] - ort[1][2]);
            quat[2] = inv * (ort[0][2] - ort[2][0]);
            quat[3] = inv * (ort[1][0] - ort[0][1]);
        } else {
            s = ort[0][0] - ort[1][1] - ort[2][2];
            if (s > -0.19) {
                // compute q1 and deduce q0, q2 and q3
                quat[1] = HALF * MathLib.sqrt(s + 1.0);
                final double inv = 0.25 / quat[1];
                quat[0] = inv * (ort[2][1] - ort[1][2]);
                quat[2] = inv * (ort[0][1] + ort[1][0]);
                quat[3] = inv * (ort[0][2] + ort[2][0]);
            } else {
                s = ort[1][1] - ort[0][0] - ort[2][2];
                if (s > -0.19) {
                    // compute q2 and deduce q0, q1 and q3
                    quat[2] = HALF * MathLib.sqrt(s + 1.0);
                    final double inv = 0.25 / quat[2];
                    quat[0] = inv * (ort[0][2] - ort[2][0]);
                    quat[1] = inv * (ort[0][1] + ort[1][0]);
                    quat[3] = inv * (ort[2][1] + ort[1][2]);
                } else {
                    // compute q3 and deduce q0, q1 and q2
                    s = ort[2][2] - ort[0][0] - ort[1][1];
                    quat[3] = HALF * MathLib.sqrt(s + 1.0);
                    final double inv = 0.25 / quat[3];
                    quat[0] = inv * (ort[1][0] - ort[0][1]);
                    quat[1] = inv * (ort[0][2] + ort[2][0]);
                    quat[2] = inv * (ort[2][1] + ort[1][2]);
                }
            }
        }

        return quat;
    }

    /**
     * Revert a rotation. Build a rotation which reverse the effect of another rotation. This means
     * that if r(u) = v, then r.revert(v) = u. The instance is not changed.
     * 
     * @return a new rotation whose effect is the reverse of the effect of the instance
     */
    public Rotation revert() {
        if (isIdentity()) {
            return IDENTITY;
        }
        return new Rotation(false, this.q0, -this.q1, -this.q2, -this.q3);
    }

    /**
     * Get the normalized axis of the rotation.
     * 
     * @return normalized axis of the rotation
     * @see #Rotation(Vector3D, double)
     */
    public Vector3D getAxis() {
        if (isIdentity()) {
            return Vector3D.PLUS_I;
        }

        // Initialization
        final Vector3D res;
        final double squaredSine = this.q1 * this.q1 + this.q2 * this.q2 + this.q3 * this.q3;

        if (squaredSine < Precision.EPSILON) {
            res = new Vector3D(1, 0, 0);
        } else if (this.q0 < 0) {
            final double inverse = -1 / MathLib.sqrt(squaredSine);
            res = new Vector3D(this.q1 * inverse, this.q2 * inverse, this.q3 * inverse);
        } else {
            // Generic case
            final double inverse = 1 / MathLib.sqrt(squaredSine);
            res = new Vector3D(this.q1 * inverse, this.q2 * inverse, this.q3 * inverse);
        }
        return res;
    }

    /**
     * Get the normalized quaternion.
     * <p>
     * The coordinates of this quaternion are defined as :
     * </p>
     * <p>
     * q<sub>0</sub> = cos(&theta;/2)
     * </p>
     * <p>
     * q<sub>1</sub> = x * sin(&theta;/2)
     * </p>
     * <p>
     * q<sub>2</sub> = y * sin(&theta;/2)
     * </p>
     * <p>
     * q<sub>3</sub> = z * sin(&theta;/2) }
     * </p>
     * <p>
     * &theta; beeing the oriented rotation angle around the axis (x, y, z).
     * </p>
     * 
     * @return the quaternion.
     * 
     * @since 3.0
     */
    public Quaternion getQuaternion() {
        if (isIdentity()) {
            return Quaternion.IDENTITY;
        }
        return new Quaternion(this.q0, this.q1, this.q2, this.q3);
    }

    /**
     * Get the normalized quaternion in double[] type : {q<sub>0</sub>, q<sub>1</sub>,
     * q<sub>2</sub>, q<sub>3</sub>}.
     * <p>
     * Those coordinates are defined as :
     * </p>
     * <p>
     * q<sub>0</sub> = cos(&theta;/2)
     * </p>
     * <p>
     * q<sub>1</sub> = x * sin(&theta;/2)
     * </p>
     * <p>
     * q<sub>2</sub> = y * sin(&theta;/2)
     * </p>
     * <p>
     * q<sub>3</sub> = z * sin(&theta;/2) }
     * </p>
     * <p>
     * &theta; beeing the oriented rotation angle around the axis (x, y, z).
     * </p>
     * 
     * @return the quaternion.
     * 
     * @since 3.0
     */
    public double[] getQi() {
        if (isIdentity()) {
            return new double[] { 1., 0., 0., 0. };
        }
        return new double[] { this.q0, this.q1, this.q2, this.q3 };
    }

    /**
     * Get the angle of the rotation. inverse trigo function are protected because for a normalize
     * quaternion it is not possible to have asin(x > 1) when q0 is different from 0 else acos is
     * called with a value between -0.1 and 0.1.
     * 
     * @return angle of the rotation (between 0 and &pi;)
     * @see #Rotation(Vector3D, double)
     */
    public double getAngle() {
        if (isIdentity()) {
            return 0.;
        }
        final double res;
        if ((this.q0 < -0.1) || (this.q0 > 0.1)) {
            res = 2 * MathLib.asin(MathLib.sqrt(this.q1 * this.q1 + this.q2 * this.q2 + this.q3 * this.q3));
        } else if (this.q0 < 0) {
            res = 2 * MathLib.acos(-this.q0);
        } else {
            res = 2 * MathLib.acos(this.q0);
        }
        return res;
    }

    /**
     * Get the Cardan or Euler angles corresponding to the instance.
     * 
     * <p>
     * The equations show that each rotation can be defined by two different values of the Cardan or Euler angles set.
     * For example if Cardan angles are used, the rotation defined by the angles a<sub>1</sub>, a<sub>2</sub> and
     * a<sub>3</sub> is the same as the rotation defined by the angles &pi; + a<sub>1</sub>, &pi; - a<sub>2</sub> and
     * &pi; + a<sub>3</sub>. This method implements the following arbitrary choices:
     * </p>
     * <ul>
     * <li>for Cardan angles, the chosen set is the one for which the second angle is between -&pi;/2 and &pi;/2 (i.e
     * its cosine is positive),</li>
     * <li>for Euler angles, the chosen set is the one for which the second angle is between 0 and &pi; (i.e its sine is
     * positive).</li>
     * </ul>
     * 
     * <p>
     * Cardan and Euler angle have a very disappointing drawback: all of them have singularities. For Cardan angles,
     * this is often called gimbal lock. There is <em>nothing</em> to do to prevent this, it is an intrinsic problem
     * with Cardan and Euler representation (but not a problem with the rotation itself, which is perfectly well
     * defined). For Cardan angles, singularities occur when the second angle is close to -&pi;/2 or +&pi;/2, for Euler
     * angle singularities occur when the second angle is close to 0 or &pi;, this implies that the identity rotation is
     * always singular for Euler angles!
     * </p>
     * 
     * @param order rotation order to use
     * @return an array of three angles, in the order specified by the set
     */
    // CHECKSTYLE: stop MethodLength check
    // CHECKSTYLE: stop CyclomaticComplexity check
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Commons-Math code kept as such
    public double[] getAngles(final RotationOrder order) {
        // CHECKSTYLE: resume MethodLength check
        // CHECKSTYLE: resume CyclomaticComplexity check
        // CHECKSTYLE: resume ReturnCount check

        if (isIdentity()) {
            return new double[] { 0., 0., 0. };
        }
        final double[] result = new double[3];

        if (order == RotationOrder.XYZ) {

            // r (Vector3D.plusK) coordinates are :
            // sin (theta), -cos (theta) sin (phi), cos (theta) cos (phi)
            // (-r) (Vector3D.plusI) coordinates are :
            // cos (psi) cos (theta), -sin (psi) cos (theta), sin (theta)
            // and we can choose to have theta in the interval [-PI/2 ; +PI/2]
            final Vector3D v1 = this.applyTo(Vector3D.PLUS_K);
            final Vector3D v2 = this.applyInverseTo(Vector3D.PLUS_I);

            if (((v1.getY() == 0) || (v1.getY() == MINUS_ZERO))
                    && ((v1.getZ() == 0) || (v1.getZ() == MINUS_ZERO))) {
                result[0] = 0;
            } else {
                result[0] = MathLib.atan2(-(v1.getY()), v1.getZ());
            }
            if (v2.getZ() > 1) {
                result[1] = FastMath.PI / 2;
            } else if (v2.getZ() < -1) {
                result[1] = -FastMath.PI / 2;
            } else {
                result[1] = MathLib.asin(v2.getZ());
            }
            if (((v2.getY() == 0) || (v2.getY() == MINUS_ZERO))
                    && ((v2.getX() == 0) || (v2.getX() == MINUS_ZERO))) {
                result[2] = 0;
            } else {
                result[2] = MathLib.atan2(-(v2.getY()), v2.getX());
            }
            return result;

        } else if (order == RotationOrder.XZY) {

            // r (Vector3D.plusJ) coordinates are :
            // -sin (psi), cos (psi) cos (phi), cos (psi) sin (phi)
            // (-r) (Vector3D.plusI) coordinates are :
            // cos (theta) cos (psi), -sin (psi), sin (theta) cos (psi)
            // and we can choose to have psi in the interval [-PI/2 ; +PI/2]
            final Vector3D v1 = this.applyTo(Vector3D.PLUS_J);
            final Vector3D v2 = this.applyInverseTo(Vector3D.PLUS_I);

            if (((v1.getY() == 0) || (v1.getY() == MINUS_ZERO))
                    && ((v1.getZ() == 0) || (v1.getZ() == MINUS_ZERO))) {
                result[0] = 0;
            } else {
                result[0] = MathLib.atan2(v1.getZ(), v1.getY());
            }
            if (v2.getY() > 1) {
                result[1] = -FastMath.PI / 2;
            } else if (v2.getY() < -1) {
                result[1] = FastMath.PI / 2;
            } else {
                result[1] = -MathLib.asin(v2.getY());
            }
            if (((v2.getZ() == 0) || (v2.getZ() == MINUS_ZERO))
                    && ((v2.getX() == 0) || (v2.getX() == MINUS_ZERO))) {
                result[2] = 0;
            } else {
                result[2] = MathLib.atan2(v2.getZ(), v2.getX());
            }

            return result;

        } else if (order == RotationOrder.YXZ) {

            // r (Vector3D.plusK) coordinates are :
            // cos (phi) sin (theta), -sin (phi), cos (phi) cos (theta)
            // (-r) (Vector3D.plusJ) coordinates are :
            // sin (psi) cos (phi), cos (psi) cos (phi), -sin (phi)
            // and we can choose to have phi in the interval [-PI/2 ; +PI/2]
            final Vector3D v1 = this.applyTo(Vector3D.PLUS_K);
            final Vector3D v2 = this.applyInverseTo(Vector3D.PLUS_J);

            if (((v1.getX() == 0) || (v1.getX() == MINUS_ZERO))
                    && ((v1.getZ() == 0) || (v1.getZ() == MINUS_ZERO))) {
                result[0] = 0;
            } else {
                result[0] = MathLib.atan2(v1.getX(), v1.getZ());
            }
            if (v2.getZ() > 1) {
                result[1] = -FastMath.PI / 2;
            } else if (v2.getZ() < -1) {
                result[1] = FastMath.PI / 2;
            } else {
                result[1] = -MathLib.asin(v2.getZ());
            }
            if (((v2.getY() == 0) || (v2.getY() == MINUS_ZERO))
                    && ((v2.getX() == 0) || (v2.getX() == MINUS_ZERO))) {
                result[2] = 0;
            } else {
                result[2] = MathLib.atan2(v2.getX(), v2.getY());
            }

            return result;

        } else if (order == RotationOrder.YZX) {

            // r (Vector3D.plusI) coordinates are :
            // cos (psi) cos (theta), sin (psi), -cos (psi) sin (theta)
            // (-r) (Vector3D.plusJ) coordinates are :
            // sin (psi), cos (phi) cos (psi), -sin (phi) cos (psi)
            // and we can choose to have psi in the interval [-PI/2 ; +PI/2]
            final Vector3D v1 = this.applyTo(Vector3D.PLUS_I);
            final Vector3D v2 = this.applyInverseTo(Vector3D.PLUS_J);

            if (((v1.getX() == 0) || (v1.getX() == MINUS_ZERO))
                    && ((v1.getZ() == 0) || (v1.getZ() == MINUS_ZERO))) {
                result[0] = 0;
            } else {
                result[0] = MathLib.atan2(-(v1.getZ()), v1.getX());
            }
            if (v2.getX() > 1) {
                result[1] = FastMath.PI / 2;
            } else if (v2.getX() < -1) {
                result[1] = -FastMath.PI / 2;
            } else {
                result[1] = MathLib.asin(v2.getX());
            }
            if (((v2.getY() == 0) || (v2.getY() == MINUS_ZERO))
                    && ((v2.getZ() == 0) || (v2.getZ() == MINUS_ZERO))) {
                result[2] = 0;
            } else {
                result[2] = MathLib.atan2(-(v2.getZ()), v2.getY());
            }

            return result;

        } else if (order == RotationOrder.ZXY) {

            // r (Vector3D.plusJ) coordinates are :
            // -cos (phi) sin (psi), cos (phi) cos (psi), sin (phi)
            // (-r) (Vector3D.plusK) coordinates are :
            // -sin (theta) cos (phi), sin (phi), cos (theta) cos (phi)
            // and we can choose to have phi in the interval [-PI/2 ; +PI/2]
            final Vector3D v1 = this.applyTo(Vector3D.PLUS_J);
            final Vector3D v2 = this.applyInverseTo(Vector3D.PLUS_K);

            if (((v1.getY() == 0) || (v1.getY() == MINUS_ZERO))
                    && ((v1.getX() == 0) || (v1.getX() == MINUS_ZERO))) {
                result[0] = 0;
            } else {
                result[0] = MathLib.atan2(-(v1.getX()), v1.getY());
            }
            if (v2.getY() > 1) {
                result[1] = FastMath.PI / 2;
            } else if (v2.getY() < -1) {
                result[1] = -FastMath.PI / 2;
            } else {
                result[1] = MathLib.asin(v2.getY());
            }
            if (((v2.getZ() == 0) || (v2.getZ() == MINUS_ZERO))
                    && ((v2.getX() == 0) || (v2.getX() == MINUS_ZERO))) {
                result[2] = 0;
            } else {
                result[2] = MathLib.atan2(-(v2.getX()), v2.getZ());
            }

            return result;

        } else if (order == RotationOrder.ZYX) {

            // r (Vector3D.plusI) coordinates are :
            // cos (theta) cos (psi), cos (theta) sin (psi), -sin (theta)
            // (-r) (Vector3D.plusK) coordinates are :
            // -sin (theta), sin (phi) cos (theta), cos (phi) cos (theta)
            // and we can choose to have theta in the interval [-PI/2 ; +PI/2]
            final Vector3D v1 = this.applyTo(Vector3D.PLUS_I);
            final Vector3D v2 = this.applyInverseTo(Vector3D.PLUS_K);

            if (((v1.getY() == 0) || (v1.getY() == MINUS_ZERO))
                    && ((v1.getX() == 0) || (v1.getX() == MINUS_ZERO))) {
                result[0] = 0;
            } else {
                result[0] = MathLib.atan2(v1.getY(), v1.getX());
            }
            if (v2.getX() > 1) {
                result[1] = -FastMath.PI / 2;
            } else if (v2.getX() < -1) {
                result[1] = FastMath.PI / 2;
            } else {
                result[1] = -MathLib.asin(v2.getX());
            }
            if (((v2.getY() == 0) || (v2.getY() == MINUS_ZERO))
                    && ((v2.getZ() == 0) || (v2.getZ() == MINUS_ZERO))) {
                result[2] = 0;
            } else {
                result[2] = MathLib.atan2(v2.getY(), v2.getZ());
            }

            return result;

        } else if (order == RotationOrder.XYX) {

            // r (Vector3D.plusI) coordinates are :
            // cos (theta), sin (phi1) sin (theta), -cos (phi1) sin (theta)
            // (-r) (Vector3D.plusI) coordinates are :
            // cos (theta), sin (theta) sin (phi2), sin (theta) cos (phi2)
            // and we can choose to have theta in the interval [0 ; PI]
            final Vector3D v1 = this.applyTo(Vector3D.PLUS_I);
            if (v1.distance(Vector3D.PLUS_I) < Precision.DOUBLE_COMPARISON_EPSILON) {
                result[0] = this.getAngle();
                return result;
            }
            final Vector3D v2 = this.applyInverseTo(Vector3D.PLUS_I);

            if (((v1.getY() == 0) || (v1.getY() == MINUS_ZERO))
                    && ((v1.getZ() == 0) || (v1.getZ() == MINUS_ZERO))) {
                result[0] = 0;
            } else {
                result[0] = MathLib.atan2(v1.getY(), -v1.getZ());
            }
            if (v2.getX() > 1) {
                result[1] = 0;
            } else if (v2.getX() < -1) {
                result[1] = FastMath.PI;
            } else {
                result[1] = MathLib.acos(v2.getX());
            }
            if (((v2.getY() == 0) || (v2.getY() == MINUS_ZERO))
                    && ((v2.getZ() == 0) || (v2.getZ() == MINUS_ZERO))) {
                result[2] = 0;
            } else {
                result[2] = MathLib.atan2(v2.getY(), v2.getZ());
            }

            return result;

        } else if (order == RotationOrder.XZX) {

            // r (Vector3D.plusI) coordinates are :
            // cos (psi), cos (phi1) sin (psi), sin (phi1) sin (psi)
            // (-r) (Vector3D.plusI) coordinates are :
            // cos (psi), -sin (psi) cos (phi2), sin (psi) sin (phi2)
            // and we can choose to have psi in the interval [0 ; PI]
            final Vector3D v1 = this.applyTo(Vector3D.PLUS_I);
            if (v1.distance(Vector3D.PLUS_I) < Precision.DOUBLE_COMPARISON_EPSILON) {
                result[0] = this.getAngle();
                return result;
            }
            final Vector3D v2 = this.applyInverseTo(Vector3D.PLUS_I);

            if (((v1.getY() == 0) || (v1.getY() == MINUS_ZERO))
                    && ((v1.getZ() == 0) || (v1.getZ() == MINUS_ZERO))) {
                result[0] = 0;
            } else {
                result[0] = MathLib.atan2(v1.getZ(), v1.getY());
            }
            if (v2.getX() > 1) {
                result[1] = 0;
            } else if (v2.getX() < -1) {
                result[1] = FastMath.PI;
            } else {
                result[1] = MathLib.acos(v2.getX());
            }
            if (((v2.getY() == 0) || (v2.getY() == MINUS_ZERO))
                    && ((v2.getZ() == 0) || (v2.getZ() == MINUS_ZERO))) {
                result[2] = 0;
            } else {
                result[2] = MathLib.atan2(v2.getZ(), -v2.getY());
            }

            return result;

        } else if (order == RotationOrder.YXY) {

            // r (Vector3D.plusJ) coordinates are :
            // sin (theta1) sin (phi), cos (phi), cos (theta1) sin (phi)
            // (-r) (Vector3D.plusJ) coordinates are :
            // sin (phi) sin (theta2), cos (phi), -sin (phi) cos (theta2)
            // and we can choose to have phi in the interval [0 ; PI]
            final Vector3D v1 = this.applyTo(Vector3D.PLUS_J);
            if (v1.distance(Vector3D.PLUS_J) < Precision.DOUBLE_COMPARISON_EPSILON) {
                result[0] = this.getAngle();
                return result;
            }
            final Vector3D v2 = this.applyInverseTo(Vector3D.PLUS_J);

            if (((v1.getX() == 0) || (v1.getX() == MINUS_ZERO))
                    && ((v1.getZ() == 0) || (v1.getZ() == MINUS_ZERO))) {
                result[0] = 0;
            } else {
                result[0] = MathLib.atan2(v1.getX(), v1.getZ());
            }
            if (v2.getY() > 1) {
                result[1] = 0;
            } else if (v2.getY() < -1) {
                result[1] = FastMath.PI;
            } else {
                result[1] = MathLib.acos(v2.getY());
            }
            if (((v2.getZ() == 0) || (v2.getZ() == MINUS_ZERO))
                    && ((v2.getX() == 0) || (v2.getX() == MINUS_ZERO))) {
                result[2] = 0;
            } else {
                result[2] = MathLib.atan2(v2.getX(), -v2.getZ());
            }

            return result;

        } else if (order == RotationOrder.YZY) {

            // r (Vector3D.plusJ) coordinates are :
            // -cos (theta1) sin (psi), cos (psi), sin (theta1) sin (psi)
            // (-r) (Vector3D.plusJ) coordinates are :
            // sin (psi) cos (theta2), cos (psi), sin (psi) sin (theta2)
            // and we can choose to have psi in the interval [0 ; PI]
            final Vector3D v1 = this.applyTo(Vector3D.PLUS_J);
            if (v1.distance(Vector3D.PLUS_J) < Precision.DOUBLE_COMPARISON_EPSILON) {
                result[0] = this.getAngle();
                return result;
            }
            final Vector3D v2 = this.applyInverseTo(Vector3D.PLUS_J);

            if (((v1.getX() == 0) || (v1.getX() == MINUS_ZERO))
                    && ((v1.getZ() == 0) || (v1.getZ() == MINUS_ZERO))) {
                result[0] = 0;
            } else {
                result[0] = MathLib.atan2(v1.getZ(), -v1.getX());
            }
            if (v2.getY() > 1) {
                result[1] = 0;
            } else if (v2.getY() < -1) {
                result[1] = FastMath.PI;
            } else {
                result[1] = MathLib.acos(v2.getY());
            }
            if (((v2.getZ() == 0) || (v2.getZ() == MINUS_ZERO))
                    && ((v2.getX() == 0) || (v2.getX() == MINUS_ZERO))) {
                result[2] = 0;
            } else {
                result[2] = MathLib.atan2(v2.getZ(), v2.getX());
            }

            return result;

        } else if (order == RotationOrder.ZXZ) {

            // r (Vector3D.plusK) coordinates are :
            // sin (psi1) sin (phi), -cos (psi1) sin (phi), cos (phi)
            // (-r) (Vector3D.plusK) coordinates are :
            // sin (phi) sin (psi2), sin (phi) cos (psi2), cos (phi)
            // and we can choose to have phi in the interval [0 ; PI]
            final Vector3D v1 = this.applyTo(Vector3D.PLUS_K);
            if (v1.distance(Vector3D.PLUS_K) < Precision.DOUBLE_COMPARISON_EPSILON) {
                result[0] = this.getAngle();
                return result;
            }
            final Vector3D v2 = this.applyInverseTo(Vector3D.PLUS_K);

            if (((v1.getY() == 0) || (v1.getY() == MINUS_ZERO))
                    && ((v1.getX() == 0) || (v1.getX() == MINUS_ZERO))) {
                result[0] = 0;
            } else {
                result[0] = MathLib.atan2(v1.getX(), -v1.getY());
            }
            if (v2.getZ() > 1) {
                result[1] = 0;
            } else if (v2.getZ() < -1) {
                result[1] = FastMath.PI;
            } else {
                result[1] = MathLib.acos(v2.getZ());
            }
            if (((v2.getY() == 0) || (v2.getY() == MINUS_ZERO))
                    && ((v2.getX() == 0) || (v2.getX() == MINUS_ZERO))) {
                result[2] = 0;
            } else {
                result[2] = MathLib.atan2(v2.getX(), v2.getY());
            }

            return result;

        } else {
            // last possibility is ZYZ

            // r (Vector3D.plusK) coordinates are :
            // cos (psi1) sin (theta), sin (psi1) sin (theta), cos (theta)
            // (-r) (Vector3D.plusK) coordinates are :
            // -sin (theta) cos (psi2), sin (theta) sin (psi2), cos (theta)
            // and we can choose to have theta in the interval [0 ; PI]
            final Vector3D v1 = this.applyTo(Vector3D.PLUS_K);
            if (v1.distance(Vector3D.PLUS_K) < Precision.DOUBLE_COMPARISON_EPSILON) {
                result[0] = this.getAngle();
                return result;
            }
            final Vector3D v2 = this.applyInverseTo(Vector3D.PLUS_K);

            if (((v1.getY() == 0) || (v1.getY() == MINUS_ZERO))
                    && ((v1.getX() == 0) || (v1.getX() == MINUS_ZERO))) {
                result[0] = 0;
            } else {
                result[0] = MathLib.atan2(v1.getY(), v1.getX());
            }
            if (v2.getZ() > 1) {
                result[1] = 0;
            } else if (v2.getZ() < -1) {
                result[1] = FastMath.PI;
            } else {
                result[1] = MathLib.acos(v2.getZ());
            }
            if (((v2.getY() == 0) || (v2.getY() == MINUS_ZERO))
                    && ((v2.getX() == 0) || (v2.getX() == MINUS_ZERO))) {
                result[2] = 0;
            } else {
                result[2] = MathLib.atan2(v2.getY(), -v2.getX());
            }

            return result;
        }
    }

    /**
     * Get the 3X3 rotation matrix corresponding to the instance.
     * 
     * @return the rotation matrix corresponding to the instance
     */
    public double[][] getMatrix() {

        if (isIdentity()) {
            return new double[][] { { 1., 0., 0. }, { 0., 1., 0. }, { 0., 0., 1. } };
        }

        // products
        final double q0q0 = this.q0 * this.q0;
        final double q0q1 = this.q0 * this.q1;
        final double q0q2 = this.q0 * this.q2;
        final double q0q3 = this.q0 * this.q3;
        final double q1q1 = this.q1 * this.q1;
        final double q1q2 = this.q1 * this.q2;
        final double q1q3 = this.q1 * this.q3;
        final double q2q2 = this.q2 * this.q2;
        final double q2q3 = this.q2 * this.q3;
        final double q3q3 = this.q3 * this.q3;

        // create the matrix
        final double[][] m = new double[3][];
        m[0] = new double[3];
        m[1] = new double[3];
        m[2] = new double[3];

        m[0][0] = 2.0 * (q0q0 + q1q1) - 1.0;
        m[1][0] = 2.0 * (q1q2 + q0q3);
        m[2][0] = 2.0 * (q1q3 - q0q2);

        m[0][1] = 2.0 * (q1q2 - q0q3);
        m[1][1] = 2.0 * (q0q0 + q2q2) - 1.0;
        m[2][1] = 2.0 * (q2q3 + q0q1);

        m[0][2] = 2.0 * (q1q3 + q0q2);
        m[1][2] = 2.0 * (q2q3 - q0q1);
        m[2][2] = 2.0 * (q0q0 + q3q3) - 1.0;

        return m;
    }

    /**
     * Apply the rotation to a vector. The image v' of a vector v is v' = Q.v.Q'.
     * 
     * @param u vector to apply the rotation to
     * @return a new vector which is the image of u by the rotation
     */
    public Vector3D applyTo(final Vector3D u) {

        if (isIdentity()) {
            return u;
        }

        final double x = u.getX();
        final double y = u.getY();
        final double z = u.getZ();

        final double s = this.q1 * x + this.q2 * y + this.q3 * z;

        return new Vector3D(2 * (this.q0 * (x * this.q0 + (this.q2 * z - this.q3 * y)) + s * this.q1) - x, 2
                * (this.q0 * (y * this.q0 + (this.q3 * x - this.q1 * z)) + s * this.q2) - y, 2
                * (this.q0 * (z * this.q0 + (this.q1 * y - this.q2 * x)) + s * this.q3) - z);
    }

    /**
     * Apply the rotation to a vector stored in an array. The image v' of a vector v is v' = Q.v.Q'.
     * 
     * @param vIn an array with three items which stores vector to rotate
     * @param vOut an array with three items to put result to (it can be the same array as in)
     */
    public void applyTo(final double[] vIn, final double[] vOut) {

        final double x = vIn[0];
        final double y = vIn[1];
        final double z = vIn[2];

        final double s = this.q1 * x + this.q2 * y + this.q3 * z;

        vOut[0] = 2 * (this.q0 * (x * this.q0 + (this.q2 * z - this.q3 * y)) + s * this.q1) - x;
        vOut[1] = 2 * (this.q0 * (y * this.q0 + (this.q3 * x - this.q1 * z)) + s * this.q2) - y;
        vOut[2] = 2 * (this.q0 * (z * this.q0 + (this.q1 * y - this.q2 * x)) + s * this.q3) - z;
    }

    /**
     * Apply the inverse of the rotation to a vector. The image v' of a vector v applying the
     * inverse of the rotation is v' = Q'.v.Q.
     * 
     * @param u vector to apply the inverse of the rotation to
     * @return a new vector which such that u is its image by the rotation
     */
    public Vector3D applyInverseTo(final Vector3D u) {

        if (isIdentity()) {
            return u;
        }

        final double x = u.getX();
        final double y = u.getY();
        final double z = u.getZ();

        final double s = this.q1 * x + this.q2 * y + this.q3 * z;
        final double m0 = -this.q0;

        return new Vector3D(2 * (m0 * (x * m0 + (this.q2 * z - this.q3 * y)) + s * this.q1) - x, 2
                * (m0 * (y * m0 + (this.q3 * x - this.q1 * z)) + s * this.q2) - y, 2
                * (m0 * (z * m0 + (this.q1 * y - this.q2 * x)) + s * this.q3) - z);
    }

    /**
     * Apply the inverse of the rotation to a vector stored in an array. The image v' of a vector v
     * applying the inverse of the rotation is v' = Q'.v.Q.
     * 
     * @param vIn an array with three items which stores vector to rotate
     * @param vOut an array with three items to put result to (it can be the same array as in)
     */
    public void applyInverseTo(final double[] vIn, final double[] vOut) {
        if (isIdentity()) {
            vOut[0] = vIn[0];
            vOut[1] = vIn[1];
            vOut[2] = vIn[2];
        } else {
            final double x = vIn[0];
            final double y = vIn[1];
            final double z = vIn[2];

            final double s = this.q1 * x + this.q2 * y + this.q3 * z;
            final double m0 = -this.q0;

            vOut[0] = 2 * (m0 * (x * m0 + (this.q2 * z - this.q3 * y)) + s * this.q1) - x;
            vOut[1] = 2 * (m0 * (y * m0 + (this.q3 * x - this.q1 * z)) + s * this.q2) - y;
            vOut[2] = 2 * (m0 * (z * m0 + (this.q1 * y - this.q2 * x)) + s * this.q3) - z;
        }
    }

    /**
     * Apply the instance to another rotation.
     * <p>
     * Applying the instance to a rotation is creating a composed rotation in the following order :
     * </p>
     * <p>
     * R3 = R2.applyTo(R1) is equivalent to R3 = R2 o R1
     * </p>
     * <p>
     * Example :
     * </p>
     * <p>
     * The vector u being transformed into v by R1 : v = R1(u)
     * </p>
     * <p>
     * The vector v being transformed into w by R2 : w = R2(v)
     * </p>
     * <p>
     * w = R2(v) = R2(R1(u)) = R2 o R1(u) = R3(u)
     * </p>
     * 
     * @param r rotation to apply the rotation to
     * @return a new rotation which is the composition of r by the instance
     */
    public Rotation applyTo(final Rotation r) {
        if (isIdentity()) {
            return r;
        }

        return new Rotation(false, r.q0 * this.q0 - (r.q1 * this.q1 + r.q2 * this.q2 + r.q3 * this.q3), r.q1
                * this.q0
                + r.q0 * this.q1 - (r.q2 * this.q3 - r.q3 * this.q2), r.q2 * this.q0 + r.q0 * this.q2
                - (r.q3 * this.q1 - r.q1 * this.q3), r.q3 * this.q0 + r.q0 * this.q3
                - (r.q1 * this.q2 - r.q2 * this.q1));
    }

    /**
     * Apply the inverse of the instance to another rotation.
     * <p>
     * Applying the inverse of the instance to a rotation is creating a composed rotation in the following order :
     * </p>
     * <p>
     * R3 = R2.applyInverseTo(R1) is equivalent to R3 = R2<sup>-1</sup> o R1
     * </p>
     * <p>
     * Example :
     * </p>
     * <p>
     * The vector u being transformed into v by R1 : v = R1(u)
     * </p>
     * <p>
     * The vector v being transformed into w by R2<sup>-1</sup> : w = R2<sup>-1</sup>(v)
     * </p>
     * <p>
     * w = R2<sup>-1</sup>(v) = R2<sup>-1</sup>(R1(u)) = R2<sup>-1</sup> o R1(u) = R3(u)
     * </p>
     * 
     * @param r rotation to apply the rotation to
     * @return a new rotation which is the composition of r by the inverse of the instance
     */
    public Rotation applyInverseTo(final Rotation r) {
        if (isIdentity()) {
            return r;
        }

        return new Rotation(false, r.q0 * this.q0 + (r.q1 * this.q1 + r.q2 * this.q2 + r.q3 * this.q3), r.q1
                * this.q0
                - r.q0 * this.q1 + (r.q2 * this.q3 - r.q3 * this.q2), r.q2 * this.q0 - r.q0 * this.q2
                + (r.q3 * this.q1 - r.q1 * this.q3), r.q3 * this.q0 - r.q0 * this.q3
                + (r.q1 * this.q2 - r.q2 * this.q1));
    }

    /**
     * Perfect orthogonality on a 3X3 matrix.
     * 
     * @param m initial matrix (not exactly orthogonal)
     * @param threshold convergence threshold for the iterative orthogonality correction
     *        (convergence is reached when the difference between two steps of the Frobenius norm of
     *        the correction is below this threshold)
     * @return an orthogonal matrix close to m
     * @exception NotARotationMatrixException if the matrix cannot be orthogonalized with the given
     *            threshold after 10 iterations
     */
    private static double[][] orthogonalizeMatrix(final double[][] m, final double threshold) {
        final double[] m0 = m[0];
        final double[] m1 = m[1];
        final double[] m2 = m[2];
        double x00 = m0[0];
        double x01 = m0[1];
        double x02 = m0[2];
        double x10 = m1[0];
        double x11 = m1[1];
        double x12 = m1[2];
        double x20 = m2[0];
        double x21 = m2[1];
        double x22 = m2[2];
        double fn = 0;
        double fn1;

        final double[][] o = new double[3][3];
        final double[] o0 = o[0];
        final double[] o1 = o[1];
        final double[] o2 = o[2];

        // iterative correction: Xn+1 = Xn - 0.5 * (Xn.Mt.Xn - M)
        int i = 1;
        while (i < 11) {

            // Mt.Xn
            final double mx00 = m0[0] * x00 + m1[0] * x10 + m2[0] * x20;
            final double mx10 = m0[1] * x00 + m1[1] * x10 + m2[1] * x20;
            final double mx20 = m0[2] * x00 + m1[2] * x10 + m2[2] * x20;
            final double mx01 = m0[0] * x01 + m1[0] * x11 + m2[0] * x21;
            final double mx11 = m0[1] * x01 + m1[1] * x11 + m2[1] * x21;
            final double mx21 = m0[2] * x01 + m1[2] * x11 + m2[2] * x21;
            final double mx02 = m0[0] * x02 + m1[0] * x12 + m2[0] * x22;
            final double mx12 = m0[1] * x02 + m1[1] * x12 + m2[1] * x22;
            final double mx22 = m0[2] * x02 + m1[2] * x12 + m2[2] * x22;

            // Xn+1
            o0[0] = x00 - HALF * (x00 * mx00 + x01 * mx10 + x02 * mx20 - m0[0]);
            o0[1] = x01 - HALF * (x00 * mx01 + x01 * mx11 + x02 * mx21 - m0[1]);
            o0[2] = x02 - HALF * (x00 * mx02 + x01 * mx12 + x02 * mx22 - m0[2]);
            o1[0] = x10 - HALF * (x10 * mx00 + x11 * mx10 + x12 * mx20 - m1[0]);
            o1[1] = x11 - HALF * (x10 * mx01 + x11 * mx11 + x12 * mx21 - m1[1]);
            o1[2] = x12 - HALF * (x10 * mx02 + x11 * mx12 + x12 * mx22 - m1[2]);
            o2[0] = x20 - HALF * (x20 * mx00 + x21 * mx10 + x22 * mx20 - m2[0]);
            o2[1] = x21 - HALF * (x20 * mx01 + x21 * mx11 + x22 * mx21 - m2[1]);
            o2[2] = x22 - HALF * (x20 * mx02 + x21 * mx12 + x22 * mx22 - m2[2]);

            // correction on each elements
            final double corr00 = o0[0] - m0[0];
            final double corr01 = o0[1] - m0[1];
            final double corr02 = o0[2] - m0[2];
            final double corr10 = o1[0] - m1[0];
            final double corr11 = o1[1] - m1[1];
            final double corr12 = o1[2] - m1[2];
            final double corr20 = o2[0] - m2[0];
            final double corr21 = o2[1] - m2[1];
            final double corr22 = o2[2] - m2[2];

            // Frobenius norm of the correction
            fn1 =
                corr00 * corr00 + corr01 * corr01 + corr02 * corr02 + corr10 * corr10 + corr11
                    * corr11 + corr12 * corr12 + corr20 * corr20 + corr21 * corr21 + corr22
                    * corr22;

            // convergence test
            if (MathLib.abs(fn1 - fn) <= threshold) {
                return o;
            }

            // prepare next iteration
            x00 = o0[0];
            x01 = o0[1];
            x02 = o0[2];
            x10 = o1[0];
            x11 = o1[1];
            x12 = o1[2];
            x20 = o2[0];
            x21 = o2[1];
            x22 = o2[2];
            fn = fn1;

            // Update loop variable
            i++;
        }

        // the algorithm did not converge after 10 iterations
        throw new NotARotationMatrixException(PatriusMessages.UNABLE_TO_ORTHOGONOLIZE_MATRIX, i - 1);
    }

    /**
     * Compute the <i>distance</i> between two rotations.
     * <p>
     * The <i>distance</i> is intended here as a way to check if two rotations are almost similar (i.e. they transform
     * vectors the same way) or very different. It is mathematically defined as the angle of the rotation r that
     * prepended to one of the rotations gives the other one:
     * </p>
     * 
     * <pre>
     *        r<sub>1</sub>(r) = r<sub>2</sub>
     * </pre>
     * <p>
     * This distance is an angle between 0 and &pi;. Its value is the smallest possible upper bound of the angle in
     * radians between r<sub>1</sub>(v) and r<sub>2</sub>(v) for all possible vectors v. This upper bound is reached for
     * some v. The distance is equal to 0 if and only if the two rotations are identical.
     * </p>
     * <p>
     * Comparing two rotations should always be done using this value rather than for example comparing the components
     * of the quaternions. It is much more stable, and has a geometric meaning. Also comparing quaternions components is
     * error prone since for example quaternions (0.36, 0.48, -0.48, -0.64) and (-0.36, -0.48, 0.48, 0.64) represent
     * exactly the same rotation despite their components are different (they are exact opposites).
     * </p>
     * 
     * @param r1 first rotation
     * @param r2 second rotation
     * @return <i>distance</i> between r1 and r2
     */
    public static double distance(final Rotation r1, final Rotation r2) {
        return r1.applyInverseTo(r2).getAngle();
    }

    /**
     * 
     * Compare two rotations with respect to their axis and angle
     * 
     * @param rotation : the rotation with which one want s to compare this rotation
     * @param angleThreshold : threshold below which one the angle between the rotations is
     *        neglectable
     * @param axisThreshold : threshold below which one the angle between the axis of the rotations
     *        is neglectable
     * @return true if the rotation can be considered similar according to the given thresholds,
     *         false otherwise
     * 
     * @since 1.0
     */
    public boolean isEqualTo(final Rotation rotation, final double angleThreshold,
            final double axisThreshold) {
        final double angleBetweenAxis = Vector3D.angle(this.getAxis(), rotation.getAxis());
        final double deltaAngle = MathLib.abs(this.getAngle() - rotation.getAngle());
        return (angleBetweenAxis < axisThreshold && deltaAngle < angleThreshold);
    }

    /**
     * 
     * Compare two rotations with respect to the distance between them (see
     * {@link Rotation#distance(Rotation, Rotation)}
     * 
     * @param rotation : the rotation with which one want s to compare this rotation
     * @return true if the rotation can be considered similar, false otherwise
     * 
     * @since 1.0
     */
    public boolean isEqualTo(final Rotation rotation) {
        return Precision.equals(Rotation.distance(this, rotation), 0.0d,
            Precision.DOUBLE_COMPARISON_EPSILON);
    }


    /**
     * Compare the rotation with respect to the identity rotation (see {@link Rotation#IDENTITY}).
     * 
     * @return true if the rotation is identity, false otherwise
     * 
     * @since 4.5
     */
    public boolean isIdentity() {
        return this.q0 == 1. && this.q1 == 0. && this.q2 == 0. && this.q3 == 0.;
    }

    /**
     * Returns linear interpolated rotation.
     * 
     * h parameter must be in [0;1] range.
     * 
     * @param r0 rotation at interpolation parameter h=0
     * @param r1 rotation at interpolation parameter h=1
     * @param h interpolation parameter, must be in [0;1] range
     * @return linear interpolated rotation at interpolation parameter h
     * 
     * @since 1.0
     */
    public static Rotation lerp(final Rotation r0, final Rotation r1, final double h) {
        if ((h < 0.) || (h > 1.)) {
            throw new OutOfRangeException(h, 0, 1);
        }
        final Quaternion q0 = r0.getQuaternion();
        final Quaternion q1 = r1.getQuaternion();
        final double w = q0.getQ0() + h * (q1.getQ0() - q0.getQ0());
        final double x = q0.getQ1() + h * (q1.getQ1() - q0.getQ1());
        final double y = q0.getQ2() + h * (q1.getQ2() - q0.getQ2());
        final double z = q0.getQ3() + h * (q1.getQ3() - q0.getQ3());
        // computed quaternion may be not normalized so we have to normalize it
        // for rotation definition
        return new Rotation(true, w, x, y, z);
    }

    /**
     * Returns spherical linear interpolated rotation. The cosinus angle cos(&lambda;) computed
     * between the input rotations could be 1. or greater than 1. due to troncation or numerical
     * precision problem. In that case, a quick return of the first input rotation is made since it
     * means input rotations are very close.
     * 
     * h interpolation parameter must be in [0;1] range.
     * 
     * @param r0 rotation at interpolation parameter h=0
     * @param r1 rotation at interpolation parameter h=1
     * @param h interpolation parameter, must be in [0;1] range
     * @return spherical linear interpolated rotation at interpolation parameter t
     * 
     * @since 1.0
     */
    public static Rotation slerp(final Rotation r0, final Rotation r1, final double h) {
        if ((h < 0.) || (h > 1.)) {
            throw new OutOfRangeException(h, 0, 1);
        }
        final Rotation rez;
        // get the quaternions from the rotations
        final Quaternion q0 = r0.getQuaternion();
        Quaternion q1 = r1.getQuaternion();

        if (q0.equals(q1, Precision.EPSILON)) {
            // r0 = r1, returns r0
            rez = r0;
        } else if (h == 0) {
            // h=0, returns r0
            rez = r0;
        } else if (h == 1) {
            // h=1, returns r1
            rez = r1;
        } else {

            // If the dotproduct is negative, use -q1 instead
            // to go along the shortest path
            if (Quaternion.dotProduct(q0, q1) < 0) {
                q1 = q1.multiply(-1);
            }

            // compute lambda, its cosinus and sinus
            final double cosLambda = Quaternion.dotProduct(q0, q1);

            // Quick return of r0 in both case where cos_lambda >= 1
            // (numerical quality issue if > 1) : it means lambda = 0, so r0 =
            // r1
            if (cosLambda >= 1) {
                return r0;
            }

            final double lambda = MathLib.acos(cosLambda);
            final double sinLambda = MathLib.sin(lambda);

            // compute the numerator of each of the terms
            final double num1 = MathLib.sin((1. - h) * lambda);
            final double num2 = MathLib.sin(h * lambda);

            // compute each term that compose the result
            final Quaternion qTerm0 = q0.multiply(num1 / sinLambda);
            final Quaternion qTerm1 = q1.multiply(num2 / sinLambda);

            // add the terms and return a rotation based on this quaternion
            final Quaternion q = Quaternion.add(qTerm0, qTerm1).normalize();
            rez = new Rotation(false, q);
        }
        return rez;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object object) {
        boolean isEqual = false;

        if (object == this) {
            // Identity
            isEqual = true;
        } else if ((object != null) && (object.getClass() == this.getClass())) {
            // Same object type: check all attributes
            final Rotation other = (Rotation) object;

            // Evaluate the attitudes components
            isEqual = Double.doubleToLongBits(this.q0) == Double.doubleToLongBits(other.q0)
                    && Double.doubleToLongBits(this.q1) == Double.doubleToLongBits(other.q1)
                    && Double.doubleToLongBits(this.q2) == Double.doubleToLongBits(other.q2)
                    && Double.doubleToLongBits(this.q3) == Double.doubleToLongBits(other.q3);
        }

        return isEqual;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hash(this.q0, this.q1, this.q2, this.q3);
    }

    /**
     * Get a string representation for the rotation.
     * 
     * @return a string representation for this rotation
     */
    @Override
    public String toString() {
        final StringBuffer res = new StringBuffer();
        final String fullClassName = this.getClass().getName();
        final String shortClassName = fullClassName.substring(fullClassName.lastIndexOf('.') + 1);
        final String open = "{";
        final String close = "}";
        final String comma = ",";
        res.append(shortClassName).append(open);
        res.append(this.q0 + comma + this.q1 + comma + this.q2 + comma + this.q3);
        res.append(close);
        return res.toString();
    }

    // CHECKSTYLE: resume MagicNumber check
    // CHECKSTYLE: resume NestedBlockDepth check
    // CHECKSTYLE: resume CommentRatio check
}
