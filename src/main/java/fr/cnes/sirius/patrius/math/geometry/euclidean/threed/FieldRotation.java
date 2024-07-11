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
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:489:06/10/2015:Refactoring of rotation acceleration by the AttitudeProvider
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.math.geometry.euclidean.threed;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.Field;
import fr.cnes.sirius.patrius.math.RealFieldElement;
import fr.cnes.sirius.patrius.math.exception.MathArithmeticException;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.util.MathArrays;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

//CHECKSTYLE: stop MagicNumber check
//CHECKSTYLE: stop CommentRatio check
//CHECKSTYLE: stop NestedBlockDepth check
//Reason: model - Commons-Math code kept as such

/**
 * This class is a re-implementation of {@link Rotation} using {@link RealFieldElement}.
 * <p>
 * Instance of this class are guaranteed to be immutable.
 * </p>
 * 
 * @param <T>
 *        the type of the field elements
 * @see FieldVector3D
 * @see RotationOrder
 * @version $Id: FieldRotation.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.1
 */

public class FieldRotation<T extends RealFieldElement<T>> implements Serializable {

    /** Serializable version identifier */
    private static final long serialVersionUID = 20130224L;

    /** 0.5. */
    private static final double HALF = 0.5;

    /** Scalar coordinate of the quaternion. */
    private final T q0;

    /** First coordinate of the vectorial part of the quaternion. */
    private final T q1;

    /** Second coordinate of the vectorial part of the quaternion. */
    private final T q2;

    /** Third coordinate of the vectorial part of the quaternion. */
    private final T q3;

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
     * @param needsNormalization
     *        if true, the coordinates are considered
     *        not to be normalized, a normalization preprocessing step is performed
     *        before using them
     * @param q0In
     *        scalar part of the quaternion
     * @param q1In
     *        first coordinate of the vectorial part of the quaternion
     * @param q2In
     *        second coordinate of the vectorial part of the quaternion
     * @param q3In
     *        third coordinate of the vectorial part of the quaternion
     * @exception MathIllegalArgumentException
     *            thrown if norm of the quaternion is zero
     * @since 3.0
     */
    public FieldRotation(final T q0In, final T q1In, final T q2In, final T q3In, final boolean needsNormalization) {

        final T norm = q0In.multiply(q0In).add(q1In.multiply(q1In))
            .add(q2In.multiply(q2In)).add(q3In.multiply(q3In)).sqrt();
        if (norm.getReal() < Precision.EPSILON) {
            throw new MathIllegalArgumentException(PatriusMessages.ZERO_NORM);
        }

        T q0Tmp = q0In;
        T q1Tmp = q1In;
        T q2Tmp = q2In;
        T q3Tmp = q3In;

        if (needsNormalization) {
            // normalization preprocessing
            final T inv = norm.reciprocal();
            q0Tmp = inv.multiply(q0In);
            q1Tmp = inv.multiply(q1In);
            q2Tmp = inv.multiply(q2In);
            q3Tmp = inv.multiply(q3In);
        }
        this.q0 = q0Tmp;
        this.q1 = q1Tmp;
        this.q2 = q2Tmp;
        this.q3 = q3Tmp;
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
     * @param axis
     *        axis around which to rotate
     * @param angle
     *        rotation angle.
     * @exception MathIllegalArgumentException
     *            if the axis norm is zero
     */
    public FieldRotation(final FieldVector3D<T> axis, final T angle) {

        final T norm = axis.getNorm();
        if (norm.getReal() < Precision.EPSILON) {
            throw new MathIllegalArgumentException(PatriusMessages.ZERO_NORM_FOR_ROTATION_AXIS);
        }
        final T halfAngle = angle.multiply(0.5);
        final T coeff = halfAngle.sin().divide(norm);

        this.q0 = halfAngle.cos();
        this.q1 = coeff.multiply(axis.getX());
        this.q2 = coeff.multiply(axis.getY());
        this.q3 = coeff.multiply(axis.getZ());
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
     * @param m
     *        rotation matrix
     * @param threshold
     *        convergence threshold for the iterative
     *        orthogonality correction (convergence is reached when the
     *        difference between two steps of the Frobenius norm of the
     *        correction is below this threshold)
     * 
     * @exception NotARotationMatrixException
     *            if the matrix is not a 3X3
     *            matrix, or if it cannot be transformed into an orthogonal matrix
     *            with the given threshold, or if the determinant of the resulting
     *            orthogonal matrix is negative
     */
    public FieldRotation(final T[][] m, final double threshold) {

        // dimension check
        if ((m.length != 3) || (m[0].length != 3) ||
            (m[1].length != 3) || (m[2].length != 3)) {
            throw new NotARotationMatrixException(
                PatriusMessages.ROTATION_MATRIX_DIMENSIONS,
                m.length, m[0].length);
        }

        // compute a "close" orthogonal matrix
        final T[][] ort = this.orthogonalizeMatrix(m, threshold);

        // check the sign of the determinant
        final T d0 = ort[1][1].multiply(ort[2][2]).subtract(ort[2][1].multiply(ort[1][2]));
        final T d1 = ort[0][1].multiply(ort[2][2]).subtract(ort[2][1].multiply(ort[0][2]));
        final T d2 = ort[0][1].multiply(ort[1][2]).subtract(ort[1][1].multiply(ort[0][2]));
        final T det =
            ort[0][0].multiply(d0).subtract(ort[1][0].multiply(d1)).add(ort[2][0].multiply(d2));
        if (det.getReal() < 0.0) {
            throw new NotARotationMatrixException(
                PatriusMessages.CLOSEST_ORTHOGONAL_MATRIX_HAS_NEGATIVE_DETERMINANT,
                det);
        }

        final T[] quat = this.mat2quat(ort);
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
     * @param u1In
     *        first vector of the origin pair
     * @param u2In
     *        second vector of the origin pair
     * @param v1In
     *        desired image of u1 by the rotation
     * @param v2In
     *        desired image of u2 by the rotation
     * @exception MathArithmeticException
     *            if the norm of one of the vectors is zero,
     *            or if one of the pair is degenerated (i.e. the vectors of the pair are colinear)
     */
    public FieldRotation(final FieldVector3D<T> u1In, final FieldVector3D<T> u2In, final FieldVector3D<T> v1In,
        final FieldVector3D<T> v2In) {

        // Initialization
        FieldVector3D<T> u1 = u1In;
        FieldVector3D<T> u2 = u2In;
        FieldVector3D<T> v1 = v1In;
        FieldVector3D<T> v2 = v2In;

        // build orthonormalized base from u1, u2
        // this fails when vectors are null or colinear, which is forbidden to define a rotation
        final FieldVector3D<T> u3 = FieldVector3D.crossProduct(u1, u2).normalize();
        u2 = FieldVector3D.crossProduct(u3, u1).normalize();
        u1 = u1.normalize();

        // build an orthonormalized base from v1, v2
        // this fails when vectors are null or colinear, which is forbidden to define a rotation
        final FieldVector3D<T> v3 = FieldVector3D.crossProduct(v1, v2).normalize();
        v2 = FieldVector3D.crossProduct(v3, v1).normalize();
        v1 = v1.normalize();

        // buid a matrix transforming the first base into the second one
        final T[][] array = MathArrays.buildArray(u1.getX().getField(), 3, 3);
        array[0][0] = u1.getX().multiply(v1.getX()).add(u2.getX().multiply(v2.getX()))
            .add(u3.getX().multiply(v3.getX()));
        array[0][1] = u1.getY().multiply(v1.getX()).add(u2.getY().multiply(v2.getX()))
            .add(u3.getY().multiply(v3.getX()));
        array[0][2] = u1.getZ().multiply(v1.getX()).add(u2.getZ().multiply(v2.getX()))
            .add(u3.getZ().multiply(v3.getX()));
        array[1][0] = u1.getX().multiply(v1.getY()).add(u2.getX().multiply(v2.getY()))
            .add(u3.getX().multiply(v3.getY()));
        array[1][1] = u1.getY().multiply(v1.getY()).add(u2.getY().multiply(v2.getY()))
            .add(u3.getY().multiply(v3.getY()));
        array[1][2] = u1.getZ().multiply(v1.getY()).add(u2.getZ().multiply(v2.getY()))
            .add(u3.getZ().multiply(v3.getY()));
        array[2][0] = u1.getX().multiply(v1.getZ()).add(u2.getX().multiply(v2.getZ()))
            .add(u3.getX().multiply(v3.getZ()));
        array[2][1] = u1.getY().multiply(v1.getZ()).add(u2.getY().multiply(v2.getZ()))
            .add(u3.getY().multiply(v3.getZ()));
        array[2][2] = u1.getZ().multiply(v1.getZ()).add(u2.getZ().multiply(v2.getZ()))
            .add(u3.getZ().multiply(v3.getZ()));

        final T[] quat = this.mat2quat(array);
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
     * @param u
     *        origin vector
     * @param v
     *        desired image of u by the rotation
     * @exception MathArithmeticException
     *            if the norm of one of the vectors is zero
     */
    public FieldRotation(final FieldVector3D<T> u, final FieldVector3D<T> v) {

        final T normProduct = u.getNorm().multiply(v.getNorm());
        if (normProduct.getReal() == 0) {
            throw new MathArithmeticException(PatriusMessages.ZERO_NORM_FOR_ROTATION_DEFINING_VECTOR);
        }

        final T dot = FieldVector3D.dotProduct(u, v);

        if (dot.getReal() < ((2.0e-15 - 1.0) * normProduct.getReal())) {
            // special case u = -v: we select a PI angle rotation around
            // an arbitrary vector orthogonal to u
            final FieldVector3D<T> w = u.orthogonal();
            this.q0 = normProduct.getField().getZero();
            this.q1 = w.getX();
            this.q2 = w.getY();
            this.q3 = w.getZ();
        } else {
            // general case: (u, v) defines a plane, we select
            // the shortest possible rotation: axis orthogonal to this plane
            this.q0 = dot.divide(normProduct).add(1.0).multiply(0.5).sqrt();
            final T coeff = this.q0.multiply(normProduct).multiply(2.0).reciprocal();

            final FieldVector3D<T> q = FieldVector3D.crossProduct(u, v);
            this.q1 = coeff.multiply(q.getX());
            this.q2 = coeff.multiply(q.getY());
            this.q3 = coeff.multiply(q.getZ());
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
     * @param order
     *        order of rotations to use
     * @param alpha1
     *        angle of the first elementary rotation
     * @param alpha2
     *        angle of the second elementary rotation
     * @param alpha3
     *        angle of the third elementary rotation
     */
    public FieldRotation(final RotationOrder order, final T alpha1, final T alpha2, final T alpha3) {
        final T one = alpha1.getField().getOne();
        final FieldRotation<T> r1 = new FieldRotation<T>(new FieldVector3D<T>(one, order.getA1()), alpha1);
        final FieldRotation<T> r2 = new FieldRotation<T>(new FieldVector3D<T>(one, order.getA2()), alpha2);
        final FieldRotation<T> r3 = new FieldRotation<T>(new FieldVector3D<T>(one, order.getA3()), alpha3);
        final FieldRotation<T> composed = r1.applyTo(r2.applyTo(r3));
        this.q0 = composed.q0;
        this.q1 = composed.q1;
        this.q2 = composed.q2;
        this.q3 = composed.q3;
    }

    /**
     * Convert an orthogonal rotation matrix to a quaternion.
     * 
     * @param ort
     *        orthogonal rotation matrix
     * @return quaternion corresponding to the matrix
     */
    private T[] mat2quat(final T[][] ort) {

        final T[] quat = MathArrays.buildArray(ort[0][0].getField(), 4);

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
        T s = ort[0][0].add(ort[1][1]).add(ort[2][2]);
        if (s.getReal() > -0.19) {
            // compute q0 and deduce q1, q2 and q3
            quat[0] = s.add(1.0).sqrt().multiply(0.5);
            final T inv = quat[0].reciprocal().multiply(0.25);
            quat[1] = inv.multiply(ort[2][1].subtract(ort[1][2]));
            quat[2] = inv.multiply(ort[0][2].subtract(ort[2][0]));
            quat[3] = inv.multiply(ort[1][0].subtract(ort[0][1]));
        } else {
            s = ort[0][0].subtract(ort[1][1]).subtract(ort[2][2]);
            if (s.getReal() > -0.19) {
                // compute q1 and deduce q0, q2 and q3
                quat[1] = s.add(1.0).sqrt().multiply(0.5);
                final T inv = quat[1].reciprocal().multiply(0.25);
                quat[0] = inv.multiply(ort[2][1].subtract(ort[1][2]));
                quat[2] = inv.multiply(ort[0][1].add(ort[1][0]));
                quat[3] = inv.multiply(ort[0][2].add(ort[2][0]));
            } else {
                s = ort[1][1].subtract(ort[0][0]).subtract(ort[2][2]);
                if (s.getReal() > -0.19) {
                    // compute q2 and deduce q0, q1 and q3
                    quat[2] = s.add(1.0).sqrt().multiply(0.5);
                    final T inv = quat[2].reciprocal().multiply(0.25);
                    quat[0] = inv.multiply(ort[0][2].subtract(ort[2][0]));
                    quat[1] = inv.multiply(ort[0][1].add(ort[1][0]));
                    quat[3] = inv.multiply(ort[2][1].add(ort[1][2]));
                } else {
                    // compute q3 and deduce q0, q1 and q2
                    s = ort[2][2].subtract(ort[0][0]).subtract(ort[1][1]);
                    quat[3] = s.add(1.0).sqrt().multiply(0.5);
                    final T inv = quat[3].reciprocal().multiply(0.25);
                    quat[0] = inv.multiply(ort[1][0].subtract(ort[0][1]));
                    quat[1] = inv.multiply(ort[0][2].add(ort[2][0]));
                    quat[2] = inv.multiply(ort[2][1].add(ort[1][2]));
                }
            }
        }

        return quat;

    }

    /**
     * Revert a rotation.
     * Build a rotation which reverse the effect of another
     * rotation. This means that if r(u) = v, then r.revert(v) = u. The
     * instance is not changed.
     * 
     * @return a new rotation whose effect is the reverse of the effect
     *         of the instance
     */
    public FieldRotation<T> revert() {
        return new FieldRotation<T>(this.q0, this.q1.negate(), this.q2.negate(), this.q3.negate(), false);
    }

    /**
     * Get the scalar coordinate of the quaternion.
     * 
     * @return scalar coordinate of the quaternion
     */
    public T getQ0() {
        return this.q0;
    }

    /**
     * Get the first coordinate of the vectorial part of the quaternion.
     * 
     * @return first coordinate of the vectorial part of the quaternion
     */
    public T getQ1() {
        return this.q1;
    }

    /**
     * Get the second coordinate of the vectorial part of the quaternion.
     * 
     * @return second coordinate of the vectorial part of the quaternion
     */
    public T getQ2() {
        return this.q2;
    }

    /**
     * Get the third coordinate of the vectorial part of the quaternion.
     * 
     * @return third coordinate of the vectorial part of the quaternion
     */
    public T getQ3() {
        return this.q3;
    }

    /**
     * Get the normalized axis of the rotation.
     * 
     * @return normalized axis of the rotation
     * @see #FieldRotation(FieldVector3D, RealFieldElement)
     */
    public FieldVector3D<T> getAxis() {
        final T squaredSine = this.q1.multiply(this.q1).add(this.q2.multiply(this.q2)).add(this.q3.multiply(this.q3));
        final FieldVector3D<T> res;
        if (squaredSine.getReal() < Precision.EPSILON) {
            final Field<T> field = squaredSine.getField();
            res = new FieldVector3D<T>(field.getOne(), field.getZero(), field.getZero());
        } else if (this.q0.getReal() < 0) {
            final T inverse = squaredSine.sqrt().reciprocal().negate();
            res = new FieldVector3D<T>(this.q1.multiply(inverse), this.q2.multiply(inverse), this.q3.multiply(inverse));
        } else {
            final T inverse = squaredSine.sqrt().reciprocal();
            res = new FieldVector3D<T>(this.q1.multiply(inverse), this.q2.multiply(inverse), this.q3.multiply(inverse));
        }
        return res;
    }

    /**
     * Get the angle of the rotation.
     * 
     * @return angle of the rotation (between 0 and &pi;)
     * @see #FieldRotation(FieldVector3D, RealFieldElement)
     */
    public T getAngle() {
        final T res;
        if ((this.q0.getReal() < -0.1) || (this.q0.getReal() > 0.1)) {
            res =
                this.q1.multiply(this.q1).add(this.q2.multiply(this.q2)).add(this.q3.multiply(this.q3)).sqrt().asin()
                    .multiply(2);
        } else if (this.q0.getReal() < 0) {
            res = this.q0.negate().acos().multiply(2);
        } else {
            res = this.q0.acos().multiply(2);
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
     * @param order
     *        rotation order to use
     * @return an array of three angles, in the order specified by the set
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Commons-Math code kept as such
    public T[] getAngles(final RotationOrder order) {
        // CHECKSTYLE: resume CyclomaticComplexity check
        // CHECKSTYLE: resume ReturnCount check
        if (order == RotationOrder.XYZ) {

            // r (+K) coordinates are :
            // sin (theta), -cos (theta) sin (phi), cos (theta) cos (phi)
            // (-r) (+I) coordinates are :
            // cos (psi) cos (theta), -sin (psi) cos (theta), sin (theta)
            // and we can choose to have theta in the interval [-PI/2 ; +PI/2]
            final FieldVector3D<T> v1 = this.applyTo(this.vector(0, 0, 1));
            final FieldVector3D<T> v2 = this.applyInverseTo(this.vector(1, 0, 0));
            return this.buildArray(v1.getY().negate().atan2(v1.getZ()),
                v2.getZ().asin(),
                v2.getY().negate().atan2(v2.getX()));

        } else if (order == RotationOrder.XZY) {

            // r (+J) coordinates are :
            // -sin (psi), cos (psi) cos (phi), cos (psi) sin (phi)
            // (-r) (+I) coordinates are :
            // cos (theta) cos (psi), -sin (psi), sin (theta) cos (psi)
            // and we can choose to have psi in the interval [-PI/2 ; +PI/2]
            final FieldVector3D<T> v1 = this.applyTo(this.vector(0, 1, 0));
            final FieldVector3D<T> v2 = this.applyInverseTo(this.vector(1, 0, 0));
            return this.buildArray(v1.getZ().atan2(v1.getY()),
                v2.getY().asin().negate(),
                v2.getZ().atan2(v2.getX()));

        } else if (order == RotationOrder.YXZ) {

            // r (+K) coordinates are :
            // cos (phi) sin (theta), -sin (phi), cos (phi) cos (theta)
            // (-r) (+J) coordinates are :
            // sin (psi) cos (phi), cos (psi) cos (phi), -sin (phi)
            // and we can choose to have phi in the interval [-PI/2 ; +PI/2]
            final FieldVector3D<T> v1 = this.applyTo(this.vector(0, 0, 1));
            final FieldVector3D<T> v2 = this.applyInverseTo(this.vector(0, 1, 0));
            return this.buildArray(v1.getX().atan2(v1.getZ()),
                v2.getZ().asin().negate(),
                v2.getX().atan2(v2.getY()));

        } else if (order == RotationOrder.YZX) {

            // r (+I) coordinates are :
            // cos (psi) cos (theta), sin (psi), -cos (psi) sin (theta)
            // (-r) (+J) coordinates are :
            // sin (psi), cos (phi) cos (psi), -sin (phi) cos (psi)
            // and we can choose to have psi in the interval [-PI/2 ; +PI/2]
            final FieldVector3D<T> v1 = this.applyTo(this.vector(1, 0, 0));
            final FieldVector3D<T> v2 = this.applyInverseTo(this.vector(0, 1, 0));
            return this.buildArray(v1.getZ().negate().atan2(v1.getX()),
                v2.getX().asin(),
                v2.getZ().negate().atan2(v2.getY()));

        } else if (order == RotationOrder.ZXY) {

            // r (+J) coordinates are :
            // -cos (phi) sin (psi), cos (phi) cos (psi), sin (phi)
            // (-r) (+K) coordinates are :
            // -sin (theta) cos (phi), sin (phi), cos (theta) cos (phi)
            // and we can choose to have phi in the interval [-PI/2 ; +PI/2]
            final FieldVector3D<T> v1 = this.applyTo(this.vector(0, 1, 0));
            final FieldVector3D<T> v2 = this.applyInverseTo(this.vector(0, 0, 1));
            return this.buildArray(v1.getX().negate().atan2(v1.getY()),
                v2.getY().asin(),
                v2.getX().negate().atan2(v2.getZ()));

        } else if (order == RotationOrder.ZYX) {

            // r (+I) coordinates are :
            // cos (theta) cos (psi), cos (theta) sin (psi), -sin (theta)
            // (-r) (+K) coordinates are :
            // -sin (theta), sin (phi) cos (theta), cos (phi) cos (theta)
            // and we can choose to have theta in the interval [-PI/2 ; +PI/2]
            final FieldVector3D<T> v1 = this.applyTo(this.vector(1, 0, 0));
            final FieldVector3D<T> v2 = this.applyInverseTo(this.vector(0, 0, 1));
            return this.buildArray(v1.getY().atan2(v1.getX()),
                v2.getX().asin().negate(),
                v2.getY().atan2(v2.getZ()));

        } else if (order == RotationOrder.XYX) {

            // r (+I) coordinates are :
            // cos (theta), sin (phi1) sin (theta), -cos (phi1) sin (theta)
            // (-r) (+I) coordinates are :
            // cos (theta), sin (theta) sin (phi2), sin (theta) cos (phi2)
            // and we can choose to have theta in the interval [0 ; PI]
            final FieldVector3D<T> v1 = this.applyTo(this.vector(1, 0, 0));
            final FieldVector3D<T> v2 = this.applyInverseTo(this.vector(1, 0, 0));
            return this.buildArray(v1.getY().atan2(v1.getZ().negate()),
                v2.getX().acos(),
                v2.getY().atan2(v2.getZ()));

        } else if (order == RotationOrder.XZX) {

            // r (+I) coordinates are :
            // cos (psi), cos (phi1) sin (psi), sin (phi1) sin (psi)
            // (-r) (+I) coordinates are :
            // cos (psi), -sin (psi) cos (phi2), sin (psi) sin (phi2)
            // and we can choose to have psi in the interval [0 ; PI]
            final FieldVector3D<T> v1 = this.applyTo(this.vector(1, 0, 0));
            final FieldVector3D<T> v2 = this.applyInverseTo(this.vector(1, 0, 0));
            return this.buildArray(v1.getZ().atan2(v1.getY()),
                v2.getX().acos(),
                v2.getZ().atan2(v2.getY().negate()));

        } else if (order == RotationOrder.YXY) {

            // r (+J) coordinates are :
            // sin (theta1) sin (phi), cos (phi), cos (theta1) sin (phi)
            // (-r) (+J) coordinates are :
            // sin (phi) sin (theta2), cos (phi), -sin (phi) cos (theta2)
            // and we can choose to have phi in the interval [0 ; PI]
            final FieldVector3D<T> v1 = this.applyTo(this.vector(0, 1, 0));
            final FieldVector3D<T> v2 = this.applyInverseTo(this.vector(0, 1, 0));
            return this.buildArray(v1.getX().atan2(v1.getZ()),
                v2.getY().acos(),
                v2.getX().atan2(v2.getZ().negate()));

        } else if (order == RotationOrder.YZY) {

            // r (+J) coordinates are :
            // -cos (theta1) sin (psi), cos (psi), sin (theta1) sin (psi)
            // (-r) (+J) coordinates are :
            // sin (psi) cos (theta2), cos (psi), sin (psi) sin (theta2)
            // and we can choose to have psi in the interval [0 ; PI]
            final FieldVector3D<T> v1 = this.applyTo(this.vector(0, 1, 0));
            final FieldVector3D<T> v2 = this.applyInverseTo(this.vector(0, 1, 0));
            return this.buildArray(v1.getZ().atan2(v1.getX().negate()),
                v2.getY().acos(),
                v2.getZ().atan2(v2.getX()));

        } else if (order == RotationOrder.ZXZ) {

            // r (+K) coordinates are :
            // sin (psi1) sin (phi), -cos (psi1) sin (phi), cos (phi)
            // (-r) (+K) coordinates are :
            // sin (phi) sin (psi2), sin (phi) cos (psi2), cos (phi)
            // and we can choose to have phi in the interval [0 ; PI]
            final FieldVector3D<T> v1 = this.applyTo(this.vector(0, 0, 1));
            final FieldVector3D<T> v2 = this.applyInverseTo(this.vector(0, 0, 1));
            return this.buildArray(v1.getX().atan2(v1.getY().negate()),
                v2.getZ().acos(),
                v2.getX().atan2(v2.getY()));

        } else {
            // last possibility is ZYZ

            // r (+K) coordinates are :
            // cos (psi1) sin (theta), sin (psi1) sin (theta), cos (theta)
            // (-r) (+K) coordinates are :
            // -sin (theta) cos (psi2), sin (theta) sin (psi2), cos (theta)
            // and we can choose to have theta in the interval [0 ; PI]
            final FieldVector3D<T> v1 = this.applyTo(this.vector(0, 0, 1));
            final FieldVector3D<T> v2 = this.applyInverseTo(this.vector(0, 0, 1));
            return this.buildArray(v1.getY().atan2(v1.getX()),
                v2.getZ().acos(),
                v2.getY().atan2(v2.getX().negate()));

        }

    }

    /**
     * Create a dimension 3 array.
     * 
     * @param a0
     *        first array element
     * @param a1
     *        second array element
     * @param a2
     *        third array element
     * @return new array
     */
    private T[] buildArray(final T a0, final T a1, final T a2) {
        final T[] array = MathArrays.buildArray(a0.getField(), 3);
        array[0] = a0;
        array[1] = a1;
        array[2] = a2;
        return array;
    }

    /**
     * Create a constant vector.
     * 
     * @param x
     *        abscissa
     * @param y
     *        ordinate
     * @param z
     *        height
     * @return a constant vector
     */
    private FieldVector3D<T> vector(final double x, final double y, final double z) {
        final T zero = this.q0.getField().getZero();
        return new FieldVector3D<T>(zero.add(x), zero.add(y), zero.add(z));
    }

    /**
     * Get the 3X3 matrix corresponding to the instance
     * 
     * @return the matrix corresponding to the instance
     */
    public T[][] getMatrix() {

        // products
        final T q0q0 = this.q0.multiply(this.q0);
        final T q0q1 = this.q0.multiply(this.q1);
        final T q0q2 = this.q0.multiply(this.q2);
        final T q0q3 = this.q0.multiply(this.q3);
        final T q1q1 = this.q1.multiply(this.q1);
        final T q1q2 = this.q1.multiply(this.q2);
        final T q1q3 = this.q1.multiply(this.q3);
        final T q2q2 = this.q2.multiply(this.q2);
        final T q2q3 = this.q2.multiply(this.q3);
        final T q3q3 = this.q3.multiply(this.q3);

        // create the matrix
        final T[][] m = MathArrays.buildArray(this.q0.getField(), 3, 3);

        m[0][0] = q0q0.add(q1q1).multiply(2).subtract(1);
        m[1][0] = q1q2.add(q0q3).multiply(2);
        m[2][0] = q1q3.subtract(q0q2).multiply(2);

        m[0][1] = q1q2.subtract(q0q3).multiply(2);
        m[1][1] = q0q0.add(q2q2).multiply(2).subtract(1);
        m[2][1] = q2q3.add(q0q1).multiply(2);

        m[0][2] = q1q3.add(q0q2).multiply(2);
        m[1][2] = q2q3.subtract(q0q1).multiply(2);
        m[2][2] = q0q0.add(q3q3).multiply(2).subtract(1);

        return m;

    }

    /**
     * Convert to a constant vector without derivatives.
     * 
     * @return a constant vector
     */
    public Rotation toRotation() {
        return new Rotation(false, this.q0.getReal(), this.q1.getReal(), this.q2.getReal(), this.q3.getReal());
    }

    /**
     * Apply the rotation to a vector.
     * The image v' of a vector v is v' = Q.v.Q'.
     * 
     * @param u
     *        vector to apply the rotation to
     * @return a new vector which is the image of u by the rotation
     */
    public FieldVector3D<T> applyTo(final FieldVector3D<T> u) {

        final T x = u.getX();
        final T y = u.getY();
        final T z = u.getZ();

        final T s = this.q1.multiply(x).add(this.q2.multiply(y)).add(this.q3.multiply(z));

        return new FieldVector3D<T>(
            this.q0.multiply(x.multiply(this.q0).add(this.q2.multiply(z).subtract(this.q3.multiply(y))))
                .add(s.multiply(this.q1))
                .multiply(2).subtract(x),
            this.q0.multiply(y.multiply(this.q0).add(this.q3.multiply(x).subtract(this.q1.multiply(z))))
                .add(s.multiply(this.q2))
                .multiply(2).subtract(y),
            this.q0.multiply(z.multiply(this.q0).add(this.q1.multiply(y).subtract(this.q2.multiply(x))))
                .add(s.multiply(this.q3))
                .multiply(2).subtract(z));
    }

    /**
     * Apply the rotation to a vector.
     * The image v' of a vector v is v' = Q.v.Q'.
     * 
     * @param u
     *        vector to apply the rotation to
     * @return a new vector which is the image of u by the rotation
     */
    public FieldVector3D<T> applyTo(final Vector3D u) {

        final double x = u.getX();
        final double y = u.getY();
        final double z = u.getZ();

        final T s = this.q1.multiply(x).add(this.q2.multiply(y)).add(this.q3.multiply(z));

        return new FieldVector3D<T>(this.q0
            .multiply(this.q0.multiply(x).add(this.q2.multiply(z).subtract(this.q3.multiply(y))))
            .add(s.multiply(this.q1)).multiply(2).subtract(x),
            this.q0.multiply(this.q0.multiply(y).add(this.q3.multiply(x).subtract(this.q1.multiply(z))))
                .add(s.multiply(this.q2))
                .multiply(2).subtract(y),
            this.q0.multiply(this.q0.multiply(z).add(this.q1.multiply(y).subtract(this.q2.multiply(x))))
                .add(s.multiply(this.q3))
                .multiply(2).subtract(z));

    }

    /**
     * Apply the rotation to a vector stored in an array.
     * The image v' of a vector v is v' = Q.v.Q'.
     * 
     * @param vIn
     *        an array with three items which stores vector to rotate
     * @param vOut
     *        an array with three items to put result to (it can be the same
     *        array as in)
     */
    public void applyTo(final T[] vIn, final T[] vOut) {

        final T x = vIn[0];
        final T y = vIn[1];
        final T z = vIn[2];

        final T s = this.q1.multiply(x).add(this.q2.multiply(y)).add(this.q3.multiply(z));

        vOut[0] =
            this.q0.multiply(x.multiply(this.q0).add(this.q2.multiply(z).subtract(this.q3.multiply(y))))
                .add(s.multiply(this.q1))
                .multiply(2).subtract(x);
        vOut[1] =
            this.q0.multiply(y.multiply(this.q0).add(this.q3.multiply(x).subtract(this.q1.multiply(z))))
                .add(s.multiply(this.q2))
                .multiply(2).subtract(y);
        vOut[2] =
            this.q0.multiply(z.multiply(this.q0).add(this.q1.multiply(y).subtract(this.q2.multiply(x))))
                .add(s.multiply(this.q3))
                .multiply(2).subtract(z);

    }

    /**
     * Apply the rotation to a vector stored in an array.
     * The image v' of a vector v is v' = Q.v.Q'.
     * 
     * @param vIn
     *        an array with three items which stores vector to rotate
     * @param vOut
     *        an array with three items to put result to
     */
    public void applyTo(final double[] vIn, final T[] vOut) {

        final double x = vIn[0];
        final double y = vIn[1];
        final double z = vIn[2];

        final T s = this.q1.multiply(x).add(this.q2.multiply(y)).add(this.q3.multiply(z));

        vOut[0] =
            this.q0.multiply(this.q0.multiply(x).add(this.q2.multiply(z).subtract(this.q3.multiply(y))))
                .add(s.multiply(this.q1))
                .multiply(2).subtract(x);
        vOut[1] =
            this.q0.multiply(this.q0.multiply(y).add(this.q3.multiply(x).subtract(this.q1.multiply(z))))
                .add(s.multiply(this.q2))
                .multiply(2).subtract(y);
        vOut[2] =
            this.q0.multiply(this.q0.multiply(z).add(this.q1.multiply(y).subtract(this.q2.multiply(x))))
                .add(s.multiply(this.q3))
                .multiply(2).subtract(z);

    }

    /**
     * Apply a rotation to a vector.
     * 
     * @param r
     *        rotation to apply
     * @param u
     *        vector to apply the rotation to
     * @param <T>
     *        the type of the field elements
     * @return a new vector which is the image of u by the rotation
     */
    public static <T extends RealFieldElement<T>> FieldVector3D<T> applyTo(final Rotation r, final FieldVector3D<T> u) {

        final T x = u.getX();
        final T y = u.getY();
        final T z = u.getZ();

        final T s = x.multiply(r.getQuaternion().getQ1()).add(y.multiply(r.getQuaternion().getQ2()))
            .add(z.multiply(r.getQuaternion().getQ3()));

        return new FieldVector3D<T>(
            x.multiply(r.getQuaternion().getQ0())
                .add(z.multiply(r.getQuaternion().getQ2()).subtract(y.multiply(r.getQuaternion().getQ3())))
                .multiply(r.getQuaternion().getQ0()).add(s.multiply(r.getQuaternion().getQ1())).multiply(2)
                .subtract(x),
            y.multiply(r.getQuaternion().getQ0())
                .add(x.multiply(r.getQuaternion().getQ3()).subtract(z.multiply(r.getQuaternion().getQ1())))
                .multiply(r.getQuaternion().getQ0()).add(s.multiply(r.getQuaternion().getQ2())).multiply(2)
                .subtract(y),
            z.multiply(r.getQuaternion().getQ0())
                .add(y.multiply(r.getQuaternion().getQ1()).subtract(x.multiply(r.getQuaternion().getQ2())))
                .multiply(r.getQuaternion().getQ0()).add(s.multiply(r.getQuaternion().getQ3())).multiply(2)
                .subtract(z));
    }

    /**
     * Apply the inverse of the rotation to a vector.
     * The image v' of a vector v applying the inverse of the rotation is v' = Q'.v.Q.
     * 
     * @param u
     *        vector to apply the inverse of the rotation to
     * @return a new vector which such that u is its image by the rotation
     */
    public FieldVector3D<T> applyInverseTo(final FieldVector3D<T> u) {

        final T x = u.getX();
        final T y = u.getY();
        final T z = u.getZ();

        final T s = this.q1.multiply(x).add(this.q2.multiply(y)).add(this.q3.multiply(z));
        final T m0 = this.q0.negate();

        return new FieldVector3D<T>(m0.multiply(x.multiply(m0).add(this.q2.multiply(z).subtract(this.q3.multiply(y))))
            .add(s.multiply(this.q1)).multiply(2).subtract(x),
            m0.multiply(y.multiply(m0).add(this.q3.multiply(x).subtract(this.q1.multiply(z)))).add(s.multiply(this.q2))
                .multiply(2).subtract(y),
            m0.multiply(z.multiply(m0).add(this.q1.multiply(y).subtract(this.q2.multiply(x)))).add(s.multiply(this.q3))
                .multiply(2).subtract(z));

    }

    /**
     * Apply the inverse of the rotation to a vector.
     * The image v' of a vector v applying the inverse of the rotation is v' = Q'.v.Q.
     * 
     * @param u
     *        vector to apply the inverse of the rotation to
     * @return a new vector which such that u is its image by the rotation
     */
    public FieldVector3D<T> applyInverseTo(final Vector3D u) {

        final double x = u.getX();
        final double y = u.getY();
        final double z = u.getZ();

        final T s = this.q1.multiply(x).add(this.q2.multiply(y)).add(this.q3.multiply(z));
        final T m0 = this.q0.negate();

        return new FieldVector3D<T>(m0.multiply(m0.multiply(x).add(this.q2.multiply(z).subtract(this.q3.multiply(y))))
            .add(s.multiply(this.q1)).multiply(2).subtract(x),
            m0.multiply(m0.multiply(y).add(this.q3.multiply(x).subtract(this.q1.multiply(z)))).add(s.multiply(this.q2))
                .multiply(2).subtract(y),
            m0.multiply(m0.multiply(z).add(this.q1.multiply(y).subtract(this.q2.multiply(x)))).add(s.multiply(this.q3))
                .multiply(2).subtract(z));

    }

    /**
     * Apply the inverse of the rotation to a vector stored in an array.
     * The image v' of a vector v applying the inverse of the rotation is v' = Q'.v.Q.
     * 
     * @param vIn
     *        an array with three items which stores vector to rotate
     * @param vOut
     *        an array with three items to put result to (it can be the same
     *        array as in)
     */
    public void applyInverseTo(final T[] vIn, final T[] vOut) {

        final T x = vIn[0];
        final T y = vIn[1];
        final T z = vIn[2];

        final T s = this.q1.multiply(x).add(this.q2.multiply(y)).add(this.q3.multiply(z));
        final T m0 = this.q0.negate();

        vOut[0] =
            m0.multiply(x.multiply(m0).add(this.q2.multiply(z).subtract(this.q3.multiply(y)))).add(s.multiply(this.q1))
                .multiply(2).subtract(x);
        vOut[1] =
            m0.multiply(y.multiply(m0).add(this.q3.multiply(x).subtract(this.q1.multiply(z)))).add(s.multiply(this.q2))
                .multiply(2).subtract(y);
        vOut[2] =
            m0.multiply(z.multiply(m0).add(this.q1.multiply(y).subtract(this.q2.multiply(x)))).add(s.multiply(this.q3))
                .multiply(2).subtract(z);

    }

    /**
     * Apply the inverse of the rotation to a vector stored in an array.
     * 
     * @param vIn
     *        an array with three items which stores vector to rotate
     * @param vOut
     *        an array with three items to put result to
     */
    public void applyInverseTo(final double[] vIn, final T[] vOut) {

        final double x = vIn[0];
        final double y = vIn[1];
        final double z = vIn[2];

        final T s = this.q1.multiply(x).add(this.q2.multiply(y)).add(this.q3.multiply(z));
        final T m0 = this.q0.negate();

        vOut[0] =
            m0.multiply(m0.multiply(x).add(this.q2.multiply(z).subtract(this.q3.multiply(y)))).add(s.multiply(this.q1))
                .multiply(2).subtract(x);
        vOut[1] =
            m0.multiply(m0.multiply(y).add(this.q3.multiply(x).subtract(this.q1.multiply(z)))).add(s.multiply(this.q2))
                .multiply(2).subtract(y);
        vOut[2] =
            m0.multiply(m0.multiply(z).add(this.q1.multiply(y).subtract(this.q2.multiply(x)))).add(s.multiply(this.q3))
                .multiply(2).subtract(z);

    }

    /**
     * Apply the inverse of a rotation to a vector.
     * 
     * @param r
     *        rotation to apply
     * @param u
     *        vector to apply the inverse of the rotation to
     * @param <T>
     *        the type of the field elements
     * @return a new vector which such that u is its image by the rotation
     */
    public static <T extends RealFieldElement<T>> FieldVector3D<T> applyInverseTo(final Rotation r,
                                                                                  final FieldVector3D<T> u) {

        final T x = u.getX();
        final T y = u.getY();
        final T z = u.getZ();

        final T s = x.multiply(r.getQuaternion().getQ1()).add(y.multiply(r.getQuaternion().getQ2()))
            .add(z.multiply(r.getQuaternion().getQ3()));
        final double m0 = -r.getQuaternion().getQ0();

        return new FieldVector3D<T>(x.multiply(m0)
            .add(z.multiply(r.getQuaternion().getQ2()).subtract(y.multiply(r.getQuaternion().getQ3())))
            .multiply(m0).add(s.multiply(r.getQuaternion().getQ1())).multiply(2).subtract(x),
            y.multiply(m0)
                .add(x.multiply(r.getQuaternion().getQ3()).subtract(z.multiply(r.getQuaternion().getQ1())))
                .multiply(m0).add(s.multiply(r.getQuaternion().getQ2())).multiply(2).subtract(y),
            z.multiply(m0)
                .add(y.multiply(r.getQuaternion().getQ1()).subtract(x.multiply(r.getQuaternion().getQ2())))
                .multiply(m0).add(s.multiply(r.getQuaternion().getQ3())).multiply(2).subtract(z));
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
     * @param r
     *        rotation to apply the rotation to
     * @return a new rotation which is the composition of r by the instance
     */
    public FieldRotation<T> applyTo(final FieldRotation<T> r) {
        return new FieldRotation<T>(r.q0.multiply(this.q0).subtract(
            r.q1.multiply(this.q1).add(r.q2.multiply(this.q2)).add(r.q3.multiply(this.q3))),
            r.q1.multiply(this.q0).add(r.q0.multiply(this.q1))
                .subtract(r.q2.multiply(this.q3).subtract(r.q3.multiply(this.q2))),
            r.q2.multiply(this.q0).add(r.q0.multiply(this.q2))
                .subtract(r.q3.multiply(this.q1).subtract(r.q1.multiply(this.q3))),
            r.q3.multiply(this.q0).add(r.q0.multiply(this.q3))
                .subtract(r.q1.multiply(this.q2).subtract(r.q2.multiply(this.q1))),
            false);
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
     * @param r
     *        rotation to apply the rotation to
     * @return a new rotation which is the composition of r by the instance
     */
    public FieldRotation<T> applyTo(final Rotation r) {
        final double q0d = r.getQuaternion().getQ0();
        final double q1d = r.getQuaternion().getQ1();
        final double q2d = r.getQuaternion().getQ2();
        final double q3d = r.getQuaternion().getQ3();
        return new FieldRotation<T>(
            this.q0.multiply(q0d).subtract(this.q1.multiply(q1d).add(this.q2.multiply(q2d)).add(this.q3.multiply(q3d))),
            this.q0.multiply(r.getQuaternion().getQ1())
                .add(this.q1.multiply(r.getQuaternion().getQ0()))
                .subtract(
                    this.q3.multiply(r.getQuaternion().getQ2())
                        .subtract(this.q2.multiply(r.getQuaternion().getQ3()))),
            this.q0.multiply(r.getQuaternion().getQ2())
                .add(this.q2.multiply(r.getQuaternion().getQ0()))
                .subtract(
                    this.q1.multiply(r.getQuaternion().getQ3())
                        .subtract(this.q3.multiply(r.getQuaternion().getQ1()))),
            this.q0.multiply(r.getQuaternion().getQ3())
                .add(this.q3.multiply(r.getQuaternion().getQ0()))
                .subtract(
                    this.q2.multiply(r.getQuaternion().getQ1())
                        .subtract(this.q1.multiply(r.getQuaternion().getQ2()))),
            false);
    }

    /**
     * Apply a rotation to another rotation.
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
     * @param r1
     *        rotation to apply
     * @param rInner
     *        rotation to apply the rotation to
     * @param <T>
     *        the type of the field elements
     * @return a new rotation which is the composition of r by the instance
     */
    public static <T extends RealFieldElement<T>> FieldRotation<T> applyTo(final Rotation r1,
                                                                           final FieldRotation<T> rInner) {
        return new FieldRotation<T>(rInner.q0.multiply(r1.getQuaternion().getQ0()).subtract(
            rInner.q1.multiply(r1.getQuaternion().getQ1()).add(rInner.q2.multiply(r1.getQuaternion().getQ2()))
                .add(rInner.q3.multiply(r1.getQuaternion().getQ3()))),
            rInner.q1
                .multiply(r1.getQuaternion().getQ0())
                .add(rInner.q0.multiply(r1.getQuaternion().getQ1()))
                .subtract(
                    rInner.q2.multiply(r1.getQuaternion().getQ3()).subtract(
                        rInner.q3.multiply(r1.getQuaternion().getQ2()))),
            rInner.q2
                .multiply(r1.getQuaternion().getQ0())
                .add(rInner.q0.multiply(r1.getQuaternion().getQ2()))
                .subtract(
                    rInner.q3.multiply(r1.getQuaternion().getQ1()).subtract(
                        rInner.q1.multiply(r1.getQuaternion().getQ3()))),
            rInner.q3
                .multiply(r1.getQuaternion().getQ0())
                .add(rInner.q0.multiply(r1.getQuaternion().getQ3()))
                .subtract(
                    rInner.q1.multiply(r1.getQuaternion().getQ2()).subtract(
                        rInner.q2.multiply(r1.getQuaternion().getQ1()))),
            false);
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
     * @param r
     *        rotation to apply the rotation to
     * @return a new rotation which is the composition of r by the inverse
     *         of the instance
     */
    public FieldRotation<T> applyInverseTo(final FieldRotation<T> r) {
        return new FieldRotation<T>(r.q0.multiply(this.q0).add(
            r.q1.multiply(this.q1).add(r.q2.multiply(this.q2)).add(r.q3.multiply(this.q3))),
            r.q0.multiply(this.q1).negate().add(r.q2.multiply(this.q3).subtract(r.q3.multiply(this.q2)))
                .add(r.q1.multiply(this.q0)),
            r.q0.multiply(this.q2).negate().add(r.q3.multiply(this.q1).subtract(r.q1.multiply(this.q3)))
                .add(r.q2.multiply(this.q0)),
            r.q0.multiply(this.q3).negate().add(r.q1.multiply(this.q2).subtract(r.q2.multiply(this.q1)))
                .add(r.q3.multiply(this.q0)),
            false);
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
     * @param r
     *        rotation to apply the rotation to
     * @return a new rotation which is the composition of r by the inverse
     *         of the instance
     */
    public FieldRotation<T> applyInverseTo(final Rotation r) {
        return new FieldRotation<T>(this.q0.multiply(r.getQuaternion().getQ0()).add(
            this.q1.multiply(r.getQuaternion().getQ1()).add(this.q2.multiply(r.getQuaternion().getQ2()))
                .add(this.q3.multiply(r.getQuaternion().getQ3()))),
            this.q1.negate().multiply(r.getQuaternion().getQ0())
                .add(this.q3.multiply(r.getQuaternion().getQ2()).subtract(this.q2.multiply(r.getQuaternion().getQ3())))
                .add(this.q0.multiply(r.getQuaternion().getQ1())),
            this.q2.negate().multiply(r.getQuaternion().getQ0())
                .add(this.q1.multiply(r.getQuaternion().getQ3()).subtract(this.q3.multiply(r.getQuaternion().getQ1())))
                .add(this.q0.multiply(r.getQuaternion().getQ2())),
            this.q3.negate().multiply(r.getQuaternion().getQ0())
                .add(this.q2.multiply(r.getQuaternion().getQ1()).subtract(this.q1.multiply(r.getQuaternion().getQ2())))
                .add(this.q0.multiply(r.getQuaternion().getQ3())),
            false);
    }

    /**
     * Apply the inverse of the a rotation to another rotation.
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
     * @param rOuter
     *        rotation to apply the rotation to
     * @param rInner
     *        rotation to apply the rotation to
     * @param <T>
     *        the type of the field elements
     * @return a new rotation which is the composition of r by the inverse
     *         of the instance
     */
    public static <T extends RealFieldElement<T>> FieldRotation<T> applyInverseTo(final Rotation rOuter,
                                                                                  final FieldRotation<T> rInner) {
        return new FieldRotation<T>(rInner.q0.multiply(rOuter.getQuaternion().getQ0()).add(
            rInner.q1.multiply(rOuter.getQuaternion().getQ1())
                .add(rInner.q2.multiply(rOuter.getQuaternion().getQ2()))
                .add(rInner.q3.multiply(rOuter.getQuaternion().getQ3()))),
            rInner.q0
                .negate()
                .multiply(rOuter.getQuaternion().getQ1())
                .add(rInner.q2.multiply(rOuter.getQuaternion().getQ3()).subtract(
                    rInner.q3.multiply(rOuter.getQuaternion().getQ2())))
                .add(rInner.q1.multiply(rOuter.getQuaternion().getQ0())),
            rInner.q0
                .negate()
                .multiply(rOuter.getQuaternion().getQ2())
                .add(rInner.q3.multiply(rOuter.getQuaternion().getQ1()).subtract(
                    rInner.q1.multiply(rOuter.getQuaternion().getQ3())))
                .add(rInner.q2.multiply(rOuter.getQuaternion().getQ0())),
            rInner.q0
                .negate()
                .multiply(rOuter.getQuaternion().getQ3())
                .add(rInner.q1.multiply(rOuter.getQuaternion().getQ2()).subtract(
                    rInner.q2.multiply(rOuter.getQuaternion().getQ1())))
                .add(rInner.q3.multiply(rOuter.getQuaternion().getQ0())),
            false);
    }

    /**
     * Perfect orthogonality on a 3X3 matrix.
     * 
     * @param m
     *        initial matrix (not exactly orthogonal)
     * @param threshold
     *        convergence threshold for the iterative
     *        orthogonality correction (convergence is reached when the
     *        difference between two steps of the Frobenius norm of the
     *        correction is below this threshold)
     * @return an orthogonal matrix close to m
     * @exception NotARotationMatrixException
     *            if the matrix cannot be
     *            orthogonalized with the given threshold after 10 iterations
     */
    private T[][] orthogonalizeMatrix(final T[][] m, final double threshold) {

        T x00 = m[0][0];
        T x01 = m[0][1];
        T x02 = m[0][2];
        T x10 = m[1][0];
        T x11 = m[1][1];
        T x12 = m[1][2];
        T x20 = m[2][0];
        T x21 = m[2][1];
        T x22 = m[2][2];
        double fn = 0;
        double fn1;

        final T[][] o = MathArrays.buildArray(m[0][0].getField(), 3, 3);

        // iterative correction: Xn+1 = Xn - 0.5 * (Xn.Mt.Xn - M)
        int i = 1;
        while (i < 11) {

            // Mt.Xn
            final T mx00 = m[0][0].multiply(x00).add(m[1][0].multiply(x10)).add(m[2][0].multiply(x20));
            final T mx10 = m[0][1].multiply(x00).add(m[1][1].multiply(x10)).add(m[2][1].multiply(x20));
            final T mx20 = m[0][2].multiply(x00).add(m[1][2].multiply(x10)).add(m[2][2].multiply(x20));
            final T mx01 = m[0][0].multiply(x01).add(m[1][0].multiply(x11)).add(m[2][0].multiply(x21));
            final T mx11 = m[0][1].multiply(x01).add(m[1][1].multiply(x11)).add(m[2][1].multiply(x21));
            final T mx21 = m[0][2].multiply(x01).add(m[1][2].multiply(x11)).add(m[2][2].multiply(x21));
            final T mx02 = m[0][0].multiply(x02).add(m[1][0].multiply(x12)).add(m[2][0].multiply(x22));
            final T mx12 = m[0][1].multiply(x02).add(m[1][1].multiply(x12)).add(m[2][1].multiply(x22));
            final T mx22 = m[0][2].multiply(x02).add(m[1][2].multiply(x12)).add(m[2][2].multiply(x22));

            // Xn+1
            o[0][0] = x00.subtract(x00.multiply(mx00).add(x01.multiply(mx10)).add(x02.multiply(mx20)).subtract(m[0][0])
                .multiply(HALF));
            o[0][1] = x01.subtract(x00.multiply(mx01).add(x01.multiply(mx11)).add(x02.multiply(mx21)).subtract(m[0][1])
                .multiply(HALF));
            o[0][2] = x02.subtract(x00.multiply(mx02).add(x01.multiply(mx12)).add(x02.multiply(mx22)).subtract(m[0][2])
                .multiply(HALF));
            o[1][0] = x10.subtract(x10.multiply(mx00).add(x11.multiply(mx10)).add(x12.multiply(mx20)).subtract(m[1][0])
                .multiply(HALF));
            o[1][1] = x11.subtract(x10.multiply(mx01).add(x11.multiply(mx11)).add(x12.multiply(mx21)).subtract(m[1][1])
                .multiply(HALF));
            o[1][2] = x12.subtract(x10.multiply(mx02).add(x11.multiply(mx12)).add(x12.multiply(mx22)).subtract(m[1][2])
                .multiply(HALF));
            o[2][0] = x20.subtract(x20.multiply(mx00).add(x21.multiply(mx10)).add(x22.multiply(mx20)).subtract(m[2][0])
                .multiply(HALF));
            o[2][1] = x21.subtract(x20.multiply(mx01).add(x21.multiply(mx11)).add(x22.multiply(mx21)).subtract(m[2][1])
                .multiply(HALF));
            o[2][2] = x22.subtract(x20.multiply(mx02).add(x21.multiply(mx12)).add(x22.multiply(mx22)).subtract(m[2][2])
                .multiply(HALF));

            // correction on each elements
            final double corr00 = o[0][0].getReal() - m[0][0].getReal();
            final double corr01 = o[0][1].getReal() - m[0][1].getReal();
            final double corr02 = o[0][2].getReal() - m[0][2].getReal();
            final double corr10 = o[1][0].getReal() - m[1][0].getReal();
            final double corr11 = o[1][1].getReal() - m[1][1].getReal();
            final double corr12 = o[1][2].getReal() - m[1][2].getReal();
            final double corr20 = o[2][0].getReal() - m[2][0].getReal();
            final double corr21 = o[2][1].getReal() - m[2][1].getReal();
            final double corr22 = o[2][2].getReal() - m[2][2].getReal();

            // Frobenius norm of the correction
            fn1 = corr00 * corr00 + corr01 * corr01 + corr02 * corr02 +
                corr10 * corr10 + corr11 * corr11 + corr12 * corr12 +
                corr20 * corr20 + corr21 * corr21 + corr22 * corr22;

            // convergence test
            if (MathLib.abs(fn1 - fn) <= threshold) {
                return o;
            }

            // prepare next iteration
            x00 = o[0][0];
            x01 = o[0][1];
            x02 = o[0][2];
            x10 = o[1][0];
            x11 = o[1][1];
            x12 = o[1][2];
            x20 = o[2][0];
            x21 = o[2][1];
            x22 = o[2][2];
            fn = fn1;

            // Update loop variable
            i++;
        }

        // the algorithm did not converge after 10 iterations
        throw new NotARotationMatrixException(PatriusMessages.UNABLE_TO_ORTHOGONOLIZE_MATRIX,
            i - 1);

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
     * @param r1
     *        first rotation
     * @param r2
     *        second rotation
     * @param <T>
     *        the type of the field elements
     * @return <i>distance</i> between r1 and r2
     */
    public static <T extends RealFieldElement<T>> T distance(final FieldRotation<T> r1, final FieldRotation<T> r2) {
        return r1.applyInverseTo(r2).getAngle();
    }

    // CHECKSTYLE: resume MagicNumber check
    // CHECKSTYLE: resume NestedBlockDepth check
    // CHECKSTYLE: resume CommentRatio check
}
