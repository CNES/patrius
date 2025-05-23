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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:489:06/10/2015:Refactoring of rotation acceleration by the AttitudeProvider
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.math.geometry.euclidean.threed;

import java.io.Serializable;
import java.text.NumberFormat;

import fr.cnes.sirius.patrius.math.RealFieldElement;
import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.MathArithmeticException;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathArrays;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

//CHECKSTYLE: stop MagicNumber check
//Reason: model - Commons-Math code

/**
 * This class is a re-implementation of {@link Vector3D} using {@link RealFieldElement}.
 * <p>
 * Instance of this class are guaranteed to be immutable.
 * </p>
 * 
 * @param <T>
 *        the type of the field elements
 * @since 3.1
 * @version $Id: FieldVector3D.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class FieldVector3D<T extends RealFieldElement<T>> implements Serializable {

     /** Serializable UID. */
    private static final long serialVersionUID = 20130224L;

    /** Abscissa. */
    private final T x;

    /** Ordinate. */
    private final T y;

    /** Height. */
    private final T z;

    /**
     * Simple constructor.
     * Build a vector from its coordinates
     * 
     * @param xIn
     *        abscissa
     * @param yIn
     *        ordinate
     * @param zIn
     *        height
     * @see #getX()
     * @see #getY()
     * @see #getZ()
     */
    public FieldVector3D(final T xIn, final T yIn, final T zIn) {
        this.x = xIn;
        this.y = yIn;
        this.z = zIn;
    }

    /**
     * Simple constructor.
     * Build a vector from its coordinates
     * 
     * @param v
     *        coordinates array
     * @exception DimensionMismatchException
     *            if array does not have 3 elements
     * @see #toArray()
     */
    public FieldVector3D(final T[] v) {
        if (v.length != 3) {
            throw new DimensionMismatchException(v.length, 3);
        }

        this.x = v[0];
        this.y = v[1];
        this.z = v[2];
    }

    /**
     * Simple constructor.
     * Build a vector from its azimuthal coordinates
     * 
     * @param alpha
     *        azimuth (&alpha;) around Z
     *        (0 is +X, &pi;/2 is +Y, &pi; is -X and 3&pi;/2 is -Y)
     * @param delta
     *        elevation (&delta;) above (XY) plane, from -&pi;/2 to +&pi;/2
     * @see #getAlpha()
     * @see #getDelta()
     */
    public FieldVector3D(final T alpha, final T delta) {
        final T cosDelta = delta.cos();
        this.x = alpha.cos().multiply(cosDelta);
        this.y = alpha.sin().multiply(cosDelta);
        this.z = delta.sin();
    }

    /**
     * Multiplicative constructor
     * Build a vector from another one and a scale factor.
     * The vector built will be a * u
     * 
     * @param a
     *        scale factor
     * @param u
     *        base (unscaled) vector
     */
    public FieldVector3D(final T a, final FieldVector3D<T> u) {
        this.x = a.multiply(u.x);
        this.y = a.multiply(u.y);
        this.z = a.multiply(u.z);
    }

    /**
     * Multiplicative constructor
     * Build a vector from another one and a scale factor.
     * The vector built will be a * u
     * 
     * @param a
     *        scale factor
     * @param u
     *        base (unscaled) vector
     */
    public FieldVector3D(final T a, final Vector3D u) {
        this.x = a.multiply(u.getX());
        this.y = a.multiply(u.getY());
        this.z = a.multiply(u.getZ());
    }

    /**
     * Multiplicative constructor
     * Build a vector from another one and a scale factor.
     * The vector built will be a * u
     * 
     * @param a
     *        scale factor
     * @param u
     *        base (unscaled) vector
     */
    public FieldVector3D(final double a, final FieldVector3D<T> u) {
        this.x = u.x.multiply(a);
        this.y = u.y.multiply(a);
        this.z = u.z.multiply(a);
    }

    /**
     * Linear constructor
     * Build a vector from two other ones and corresponding scale factors.
     * The vector built will be a1 * u1 + a2 * u2
     * 
     * @param a1
     *        first scale factor
     * @param u1
     *        first base (unscaled) vector
     * @param a2
     *        second scale factor
     * @param u2
     *        second base (unscaled) vector
     */
    public FieldVector3D(final T a1, final FieldVector3D<T> u1,
        final T a2, final FieldVector3D<T> u2) {
        final T prototype = a1;
        this.x = prototype.linearCombination(a1, u1.getX(), a2, u2.getX());
        this.y = prototype.linearCombination(a1, u1.getY(), a2, u2.getY());
        this.z = prototype.linearCombination(a1, u1.getZ(), a2, u2.getZ());
    }

    /**
     * Linear constructor
     * Build a vector from two other ones and corresponding scale factors.
     * The vector built will be a1 * u1 + a2 * u2
     * 
     * @param a1
     *        first scale factor
     * @param u1
     *        first base (unscaled) vector
     * @param a2
     *        second scale factor
     * @param u2
     *        second base (unscaled) vector
     */
    public FieldVector3D(final T a1, final Vector3D u1,
        final T a2, final Vector3D u2) {
        final T prototype = a1;
        this.x = prototype.linearCombination(u1.getX(), a1, u2.getX(), a2);
        this.y = prototype.linearCombination(u1.getY(), a1, u2.getY(), a2);
        this.z = prototype.linearCombination(u1.getZ(), a1, u2.getZ(), a2);
    }

    /**
     * Linear constructor
     * Build a vector from two other ones and corresponding scale factors.
     * The vector built will be a1 * u1 + a2 * u2
     * 
     * @param a1
     *        first scale factor
     * @param u1
     *        first base (unscaled) vector
     * @param a2
     *        second scale factor
     * @param u2
     *        second base (unscaled) vector
     */
    public FieldVector3D(final double a1, final FieldVector3D<T> u1,
        final double a2, final FieldVector3D<T> u2) {
        final T prototype = u1.getX();
        this.x = prototype.linearCombination(a1, u1.getX(), a2, u2.getX());
        this.y = prototype.linearCombination(a1, u1.getY(), a2, u2.getY());
        this.z = prototype.linearCombination(a1, u1.getZ(), a2, u2.getZ());
    }

    /**
     * Linear constructor
     * Build a vector from three other ones and corresponding scale factors.
     * The vector built will be a1 * u1 + a2 * u2 + a3 * u3
     * 
     * @param a1
     *        first scale factor
     * @param u1
     *        first base (unscaled) vector
     * @param a2
     *        second scale factor
     * @param u2
     *        second base (unscaled) vector
     * @param a3
     *        third scale factor
     * @param u3
     *        third base (unscaled) vector
     */
    public FieldVector3D(final T a1, final FieldVector3D<T> u1,
        final T a2, final FieldVector3D<T> u2,
        final T a3, final FieldVector3D<T> u3) {
        final T prototype = a1;
        this.x = prototype.linearCombination(a1, u1.getX(), a2, u2.getX(), a3, u3.getX());
        this.y = prototype.linearCombination(a1, u1.getY(), a2, u2.getY(), a3, u3.getY());
        this.z = prototype.linearCombination(a1, u1.getZ(), a2, u2.getZ(), a3, u3.getZ());
    }

    /**
     * Linear constructor
     * Build a vector from three other ones and corresponding scale factors.
     * The vector built will be a1 * u1 + a2 * u2 + a3 * u3
     * 
     * @param a1
     *        first scale factor
     * @param u1
     *        first base (unscaled) vector
     * @param a2
     *        second scale factor
     * @param u2
     *        second base (unscaled) vector
     * @param a3
     *        third scale factor
     * @param u3
     *        third base (unscaled) vector
     */
    public FieldVector3D(final T a1, final Vector3D u1,
        final T a2, final Vector3D u2,
        final T a3, final Vector3D u3) {
        final T prototype = a1;
        this.x = prototype.linearCombination(u1.getX(), a1, u2.getX(), a2, u3.getX(), a3);
        this.y = prototype.linearCombination(u1.getY(), a1, u2.getY(), a2, u3.getY(), a3);
        this.z = prototype.linearCombination(u1.getZ(), a1, u2.getZ(), a2, u3.getZ(), a3);
    }

    /**
     * Linear constructor
     * Build a vector from three other ones and corresponding scale factors.
     * The vector built will be a1 * u1 + a2 * u2 + a3 * u3
     * 
     * @param a1
     *        first scale factor
     * @param u1
     *        first base (unscaled) vector
     * @param a2
     *        second scale factor
     * @param u2
     *        second base (unscaled) vector
     * @param a3
     *        third scale factor
     * @param u3
     *        third base (unscaled) vector
     */
    public FieldVector3D(final double a1, final FieldVector3D<T> u1,
        final double a2, final FieldVector3D<T> u2,
        final double a3, final FieldVector3D<T> u3) {
        final T prototype = u1.getX();
        this.x = prototype.linearCombination(a1, u1.getX(), a2, u2.getX(), a3, u3.getX());
        this.y = prototype.linearCombination(a1, u1.getY(), a2, u2.getY(), a3, u3.getY());
        this.z = prototype.linearCombination(a1, u1.getZ(), a2, u2.getZ(), a3, u3.getZ());
    }

    /**
     * Linear constructor
     * Build a vector from four other ones and corresponding scale factors.
     * The vector built will be a1 * u1 + a2 * u2 + a3 * u3 + a4 * u4
     * 
     * @param a1
     *        first scale factor
     * @param u1
     *        first base (unscaled) vector
     * @param a2
     *        second scale factor
     * @param u2
     *        second base (unscaled) vector
     * @param a3
     *        third scale factor
     * @param u3
     *        third base (unscaled) vector
     * @param a4
     *        fourth scale factor
     * @param u4
     *        fourth base (unscaled) vector
     */
    public FieldVector3D(final T a1, final FieldVector3D<T> u1,
        final T a2, final FieldVector3D<T> u2,
        final T a3, final FieldVector3D<T> u3,
        final T a4, final FieldVector3D<T> u4) {
        final T prototype = a1;
        this.x = prototype.linearCombination(a1, u1.getX(), a2, u2.getX(), a3, u3.getX(), a4, u4.getX());
        this.y = prototype.linearCombination(a1, u1.getY(), a2, u2.getY(), a3, u3.getY(), a4, u4.getY());
        this.z = prototype.linearCombination(a1, u1.getZ(), a2, u2.getZ(), a3, u3.getZ(), a4, u4.getZ());
    }

    /**
     * Linear constructor
     * Build a vector from four other ones and corresponding scale factors.
     * The vector built will be a1 * u1 + a2 * u2 + a3 * u3 + a4 * u4
     * 
     * @param a1
     *        first scale factor
     * @param u1
     *        first base (unscaled) vector
     * @param a2
     *        second scale factor
     * @param u2
     *        second base (unscaled) vector
     * @param a3
     *        third scale factor
     * @param u3
     *        third base (unscaled) vector
     * @param a4
     *        fourth scale factor
     * @param u4
     *        fourth base (unscaled) vector
     */
    public FieldVector3D(final T a1, final Vector3D u1,
        final T a2, final Vector3D u2,
        final T a3, final Vector3D u3,
        final T a4, final Vector3D u4) {
        final T prototype = a1;
        this.x = prototype.linearCombination(u1.getX(), a1, u2.getX(), a2, u3.getX(), a3, u4.getX(), a4);
        this.y = prototype.linearCombination(u1.getY(), a1, u2.getY(), a2, u3.getY(), a3, u4.getY(), a4);
        this.z = prototype.linearCombination(u1.getZ(), a1, u2.getZ(), a2, u3.getZ(), a3, u4.getZ(), a4);
    }

    /**
     * Linear constructor
     * Build a vector from four other ones and corresponding scale factors.
     * The vector built will be a1 * u1 + a2 * u2 + a3 * u3 + a4 * u4
     * 
     * @param a1
     *        first scale factor
     * @param u1
     *        first base (unscaled) vector
     * @param a2
     *        second scale factor
     * @param u2
     *        second base (unscaled) vector
     * @param a3
     *        third scale factor
     * @param u3
     *        third base (unscaled) vector
     * @param a4
     *        fourth scale factor
     * @param u4
     *        fourth base (unscaled) vector
     */
    public FieldVector3D(final double a1, final FieldVector3D<T> u1,
        final double a2, final FieldVector3D<T> u2,
        final double a3, final FieldVector3D<T> u3,
        final double a4, final FieldVector3D<T> u4) {
        final T prototype = u1.getX();
        this.x = prototype.linearCombination(a1, u1.getX(), a2, u2.getX(), a3, u3.getX(), a4, u4.getX());
        this.y = prototype.linearCombination(a1, u1.getY(), a2, u2.getY(), a3, u3.getY(), a4, u4.getY());
        this.z = prototype.linearCombination(a1, u1.getZ(), a2, u2.getZ(), a3, u3.getZ(), a4, u4.getZ());
    }

    /**
     * Get the abscissa of the vector.
     * 
     * @return abscissa of the vector
     * @see #FieldVector3D(RealFieldElement, RealFieldElement, RealFieldElement)
     */
    public T getX() {
        return this.x;
    }

    /**
     * Get the ordinate of the vector.
     * 
     * @return ordinate of the vector
     * @see #FieldVector3D(RealFieldElement, RealFieldElement, RealFieldElement)
     */
    public T getY() {
        return this.y;
    }

    /**
     * Get the height of the vector.
     * 
     * @return height of the vector
     * @see #FieldVector3D(RealFieldElement, RealFieldElement, RealFieldElement)
     */
    public T getZ() {
        return this.z;
    }

    /**
     * Get the vector coordinates as a dimension 3 array.
     * 
     * @return vector coordinates
     * @see #FieldVector3D(RealFieldElement[])
     */
    public T[] toArray() {
        final T[] array = MathArrays.buildArray(this.x.getField(), 3);
        array[0] = this.x;
        array[1] = this.y;
        array[2] = this.z;
        return array;
    }

    /**
     * Convert to a constant vector without derivatives.
     * 
     * @return a constant vector
     */
    public Vector3D toVector3D() {
        return new Vector3D(this.x.getReal(), this.y.getReal(), this.z.getReal());
    }

    /**
     * Get the L<sub>1</sub> norm for the vector.
     * 
     * @return L<sub>1</sub> norm for the vector
     */
    public T getNorm1() {
        return this.x.abs().add(this.y.abs()).add(this.z.abs());
    }

    /**
     * Get the L<sub>2</sub> norm for the vector.
     * 
     * @return Euclidean norm for the vector
     */
    public T getNorm() {
        // there are no cancellation problems here, so we use the straightforward formula
        return this.x.multiply(this.x).add(this.y.multiply(this.y)).add(this.z.multiply(this.z)).sqrt();
    }

    /**
     * Get the square of the norm for the vector.
     * 
     * @return square of the Euclidean norm for the vector
     */
    public T getNormSq() {
        // there are no cancellation problems here, so we use the straightforward formula
        return this.x.multiply(this.x).add(this.y.multiply(this.y)).add(this.z.multiply(this.z));
    }

    /**
     * Get the L<sub>&infin;</sub> norm for the vector.
     * 
     * @return L<sub>&infin;</sub> norm for the vector
     */
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Commons-Math code kept as such
    public T getNormInf() {
        // CHECKSTYLE: resume ReturnCount check
        // Absolute values
        final T xAbs = this.x.abs();
        final T yAbs = this.y.abs();
        final T zAbs = this.z.abs();
        // Case |x| <= |y|
        if (xAbs.getReal() <= yAbs.getReal()) {
            // return max between |y| and |z|
            if (yAbs.getReal() <= zAbs.getReal()) {
                return zAbs;
            }
            return yAbs;
        }

        // Case |x| > |y| return max between |x| and |z|
        if (xAbs.getReal() <= zAbs.getReal()) {
            return zAbs;
        }
        return xAbs;
    }

    /**
     * Get the azimuth of the vector.
     * 
     * @return azimuth (&alpha;) of the vector, between -&pi; and +&pi;
     * @see #FieldVector3D(RealFieldElement, RealFieldElement)
     */
    public T getAlpha() {
        return this.y.atan2(this.x);
    }

    /**
     * Get the elevation of the vector.
     * 
     * @return elevation (&delta;) of the vector, between -&pi;/2 and +&pi;/2
     * @see #FieldVector3D(RealFieldElement, RealFieldElement)
     */
    public T getDelta() {
        return this.z.divide(this.getNorm()).asin();
    }

    /**
     * Add a vector to the instance.
     * 
     * @param v
     *        vector to add
     * @return a new vector
     */
    public FieldVector3D<T> add(final FieldVector3D<T> v) {
        return new FieldVector3D<>(this.x.add(v.x), this.y.add(v.y), this.z.add(v.z));
    }

    /**
     * Add a vector to the instance.
     * 
     * @param v
     *        vector to add
     * @return a new vector
     */
    public FieldVector3D<T> add(final Vector3D v) {
        return new FieldVector3D<>(this.x.add(v.getX()), this.y.add(v.getY()), this.z.add(v.getZ()));
    }

    /**
     * Add a scaled vector to the instance.
     * 
     * @param factor
     *        scale factor to apply to v before adding it
     * @param v
     *        vector to add
     * @return a new vector
     */
    public FieldVector3D<T> add(final T factor, final FieldVector3D<T> v) {
        return new FieldVector3D<>(this.x.getField().getOne(), this, factor, v);
    }

    /**
     * Add a scaled vector to the instance.
     * 
     * @param factor
     *        scale factor to apply to v before adding it
     * @param v
     *        vector to add
     * @return a new vector
     */
    public FieldVector3D<T> add(final T factor, final Vector3D v) {
        return new FieldVector3D<>(this.x.add(factor.multiply(v.getX())),
            this.y.add(factor.multiply(v.getY())),
            this.z.add(factor.multiply(v.getZ())));
    }

    /**
     * Add a scaled vector to the instance.
     * 
     * @param factor
     *        scale factor to apply to v before adding it
     * @param v
     *        vector to add
     * @return a new vector
     */
    public FieldVector3D<T> add(final double factor, final FieldVector3D<T> v) {
        return new FieldVector3D<>(1.0, this, factor, v);
    }

    /**
     * Add a scaled vector to the instance.
     * 
     * @param factor
     *        scale factor to apply to v before adding it
     * @param v
     *        vector to add
     * @return a new vector
     */
    public FieldVector3D<T> add(final double factor, final Vector3D v) {
        return new FieldVector3D<>(this.x.add(factor * v.getX()),
            this.y.add(factor * v.getY()),
            this.z.add(factor * v.getZ()));
    }

    /**
     * Subtract a vector from the instance.
     * 
     * @param v
     *        vector to subtract
     * @return a new vector
     */
    public FieldVector3D<T> subtract(final FieldVector3D<T> v) {
        return new FieldVector3D<>(this.x.subtract(v.x), this.y.subtract(v.y), this.z.subtract(v.z));
    }

    /**
     * Subtract a vector from the instance.
     * 
     * @param v
     *        vector to subtract
     * @return a new vector
     */
    public FieldVector3D<T> subtract(final Vector3D v) {
        return new FieldVector3D<>(this.x.subtract(v.getX()), this.y.subtract(v.getY()), this.z.subtract(v.getZ()));
    }

    /**
     * Subtract a scaled vector from the instance.
     * 
     * @param factor
     *        scale factor to apply to v before subtracting it
     * @param v
     *        vector to subtract
     * @return a new vector
     */
    public FieldVector3D<T> subtract(final T factor, final FieldVector3D<T> v) {
        return new FieldVector3D<>(this.x.getField().getOne(), this, factor.negate(), v);
    }

    /**
     * Subtract a scaled vector from the instance.
     * 
     * @param factor
     *        scale factor to apply to v before subtracting it
     * @param v
     *        vector to subtract
     * @return a new vector
     */
    public FieldVector3D<T> subtract(final T factor, final Vector3D v) {
        return new FieldVector3D<>(this.x.subtract(factor.multiply(v.getX())),
            this.y.subtract(factor.multiply(v.getY())),
            this.z.subtract(factor.multiply(v.getZ())));
    }

    /**
     * Subtract a scaled vector from the instance.
     * 
     * @param factor
     *        scale factor to apply to v before subtracting it
     * @param v
     *        vector to subtract
     * @return a new vector
     */
    public FieldVector3D<T> subtract(final double factor, final FieldVector3D<T> v) {
        return new FieldVector3D<>(1.0, this, -factor, v);
    }

    /**
     * Subtract a scaled vector from the instance.
     * 
     * @param factor
     *        scale factor to apply to v before subtracting it
     * @param v
     *        vector to subtract
     * @return a new vector
     */
    public FieldVector3D<T> subtract(final double factor, final Vector3D v) {
        return new FieldVector3D<>(this.x.subtract(factor * v.getX()),
            this.y.subtract(factor * v.getY()),
            this.z.subtract(factor * v.getZ()));
    }

    /**
     * Get a normalized vector aligned with the instance.
     * 
     * @return a new normalized vector
     * @exception MathArithmeticException
     *            if the norm is zero
     */
    public FieldVector3D<T> normalize() {
        final T s = this.getNorm();
        if (s.getReal() == 0) {
            throw new MathArithmeticException(PatriusMessages.CANNOT_NORMALIZE_A_ZERO_NORM_VECTOR);
        }
        return this.scalarMultiply(s.reciprocal());
    }

    /**
     * Get a vector orthogonal to the instance.
     * <p>
     * There are an infinite number of normalized vectors orthogonal to the instance. This method picks up one of them
     * almost arbitrarily. It is useful when one needs to compute a reference frame with one of the axes in a predefined
     * direction. The following example shows how to build a frame having the k axis aligned with the known vector u :
     * 
     * <pre>
     * <code>
     *   Vector3D k = u.normalize();
     *   Vector3D i = k.orthogonal();
     *   Vector3D j = Vector3D.crossProduct(k, i);
     * </code>
     * </pre>
     * 
     * </p>
     * 
     * @return a new normalized vector orthogonal to the instance
     * @exception MathArithmeticException
     *            if the norm of the instance is null
     */
    public FieldVector3D<T> orthogonal() {

        final double threshold = 0.6 * this.getNorm().getReal();
        if (threshold == 0) {
            // Exception
            throw new MathArithmeticException(PatriusMessages.ZERO_NORM);
        }

        final FieldVector3D<T> res;
        if (MathLib.abs(this.x.getReal()) <= threshold) {
            final T inverse = this.y.multiply(this.y).add(this.z.multiply(this.z)).sqrt().reciprocal();
            res = new FieldVector3D<>(inverse.getField().getZero(),
                inverse.multiply(this.z), inverse.multiply(this.y).negate());
        } else if (MathLib.abs(this.y.getReal()) <= threshold) {
            final T inverse = this.x.multiply(this.x).add(this.z.multiply(this.z)).sqrt().reciprocal();
            res = new FieldVector3D<>(inverse.multiply(this.z).negate(),
                inverse.getField().getZero(), inverse.multiply(this.x));
        } else {
            // General case
            final T inverse = this.x.multiply(this.x).add(this.y.multiply(this.y)).sqrt().reciprocal();
            res = new FieldVector3D<>(inverse.multiply(this.y),
                inverse.multiply(this.x).negate(), inverse.getField().getZero());
        }

        // Return result
        return res;
    }

    /**
     * Compute the angular separation between two vectors.
     * <p>
     * This method computes the angular separation between two vectors using the dot product for well separated vectors
     * and the cross product for almost aligned vectors. This allows to have a good accuracy in all cases, even for
     * vectors very close to each other.
     * </p>
     * 
     * @param v1
     *        first vector
     * @param v2
     *        second vector
     * @param <T>
     *        the type of the field elements
     * @return angular separation between v1 and v2
     * @exception MathArithmeticException
     *            if either vector has a null norm
     */
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Commons-Math code kept as such
    public static <T extends RealFieldElement<T>> T angle(final FieldVector3D<T> v1, final FieldVector3D<T> v2) {
        // CHECKSTYLE: resume ReturnCount check

        final T normProduct = v1.getNorm().multiply(v2.getNorm());
        if (normProduct.getReal() == 0) {
            throw new MathArithmeticException(PatriusMessages.ZERO_NORM);
        }

        final T dot = dotProduct(v1, v2);
        final double threshold = normProduct.getReal() * 0.9999;
        if ((dot.getReal() < -threshold) || (dot.getReal() > threshold)) {
            // the vectors are almost aligned, compute using the sine
            final FieldVector3D<T> v3 = crossProduct(v1, v2);
            if (dot.getReal() >= 0) {
                return v3.getNorm().divide(normProduct).asin();
            }
            return v3.getNorm().divide(normProduct).asin().subtract(FastMath.PI).negate();
        }

        // the vectors are sufficiently separated to use the cosine
        return dot.divide(normProduct).acos();
    }

    /**
     * Compute the angular separation between two vectors.
     * <p>
     * This method computes the angular separation between two vectors using the dot product for well separated vectors
     * and the cross product for almost aligned vectors. This allows to have a good accuracy in all cases, even for
     * vectors very close to each other.
     * </p>
     * 
     * @param v1
     *        first vector
     * @param v2
     *        second vector
     * @param <T>
     *        the type of the field elements
     * @return angular separation between v1 and v2
     * @exception MathArithmeticException
     *            if either vector has a null norm
     */
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Commons-Math code kept as such
    public static <T extends RealFieldElement<T>> T angle(final FieldVector3D<T> v1, final Vector3D v2) {
        // CHECKSTYLE: resume ReturnCount check

        final T normProduct = v1.getNorm().multiply(v2.getNorm());
        if (normProduct.getReal() == 0) {
            throw new MathArithmeticException(PatriusMessages.ZERO_NORM);
        }

        final T dot = dotProduct(v1, v2);
        final double threshold = normProduct.getReal() * 0.9999;
        if ((dot.getReal() < -threshold) || (dot.getReal() > threshold)) {
            // the vectors are almost aligned, compute using the sine
            final FieldVector3D<T> v3 = crossProduct(v1, v2);
            if (dot.getReal() >= 0) {
                return v3.getNorm().divide(normProduct).asin();
            }
            return v3.getNorm().divide(normProduct).asin().subtract(FastMath.PI).negate();
        }

        // the vectors are sufficiently separated to use the cosine
        return dot.divide(normProduct).acos();
    }

    /**
     * Compute the angular separation between two vectors.
     * <p>
     * This method computes the angular separation between two vectors using the dot product for well separated vectors
     * and the cross product for almost aligned vectors. This allows to have a good accuracy in all cases, even for
     * vectors very close to each other.
     * </p>
     * 
     * @param v1
     *        first vector
     * @param v2
     *        second vector
     * @param <T>
     *        the type of the field elements
     * @return angular separation between v1 and v2
     * @exception MathArithmeticException
     *            if either vector has a null norm
     */
    public static <T extends RealFieldElement<T>> T angle(final Vector3D v1, final FieldVector3D<T> v2) {
        return angle(v2, v1);
    }

    /**
     * Get the opposite of the instance.
     * 
     * @return a new vector which is opposite to the instance
     */
    public FieldVector3D<T> negate() {
        return new FieldVector3D<>(this.x.negate(), this.y.negate(), this.z.negate());
    }

    /**
     * Multiply the instance by a scalar.
     * 
     * @param a
     *        scalar
     * @return a new vector
     */
    public FieldVector3D<T> scalarMultiply(final T a) {
        return new FieldVector3D<>(this.x.multiply(a), this.y.multiply(a), this.z.multiply(a));
    }

    /**
     * Multiply the instance by a scalar.
     * 
     * @param a
     *        scalar
     * @return a new vector
     */
    public FieldVector3D<T> scalarMultiply(final double a) {
        return new FieldVector3D<>(this.x.multiply(a), this.y.multiply(a), this.z.multiply(a));
    }

    /**
     * Returns true if any coordinate of this vector is NaN; false otherwise
     * 
     * @return true if any coordinate of this vector is NaN; false otherwise
     */
    public boolean isNaN() {
        return Double.isNaN(this.x.getReal()) || Double.isNaN(this.y.getReal()) || Double.isNaN(this.z.getReal());
    }

    /**
     * Returns true if any coordinate of this vector is infinite and none are NaN;
     * false otherwise
     * 
     * @return true if any coordinate of this vector is infinite and none are NaN;
     *         false otherwise
     */
    public boolean isInfinite() {
        return !this.isNaN()
            && (Double.isInfinite(this.x.getReal()) || Double.isInfinite(this.y.getReal()) || Double.isInfinite(this.z
                .getReal()));
    }

    /**
     * Test for the equality of two 3D vectors.
     * <p>
     * If all coordinates of two 3D vectors are exactly the same, and none of their {@link RealFieldElement#getReal()
     * real part} are <code>NaN</code>, the two 3D vectors are considered to be equal.
     * </p>
     * <p>
     * <code>NaN</code> coordinates are considered to affect globally the vector and be equals to each other - i.e, if
     * either (or all) real part of the coordinates of the 3D vector are <code>NaN</code>, the 3D vector is
     * <code>NaN</code>.
     * </p>
     * 
     * @param other
     *        Object to test for equality to this
     * @return true if two 3D vector objects are equal, false if
     *         object is null, not an instance of Vector3D, or
     *         not equal to this Vector3D instance
     * 
     */
    @Override
    public boolean equals(final Object other) {

        if (this == other) {
            return true;
        }

        if (other instanceof FieldVector3D) {
            @SuppressWarnings("unchecked")
            final FieldVector3D<T> rhs = (FieldVector3D<T>) other;
            if (rhs.isNaN()) {
                return this.isNaN();
            }

            return this.x.equals(rhs.x) && this.y.equals(rhs.y) && this.z.equals(rhs.z);

        }
        return false;
    }

    /**
     * Get a hashCode for the 3D vector.
     * <p>
     * All NaN values have the same hash code.
     * </p>
     * 
     * @return a hash code value for this object
     */
    @Override
    public int hashCode() {
        if (this.isNaN()) {
            return 409;
        }
        return 311 * (107 * this.x.hashCode() + 83 * this.y.hashCode() + this.z.hashCode());
    }

    /**
     * Compute the dot-product of the instance and another vector.
     * <p>
     * The implementation uses specific multiplication and addition algorithms to preserve accuracy and reduce
     * cancellation effects. It should be very accurate even for nearly orthogonal vectors.
     * </p>
     * 
     * @see MathArrays#linearCombination(double, double, double, double, double, double)
     * @param v
     *        second vector
     * @return the dot product this.v
     */
    public T dotProduct(final FieldVector3D<T> v) {
        return this.x.linearCombination(this.x, v.x, this.y, v.y, this.z, v.z);
    }

    /**
     * Compute the dot-product of the instance and another vector.
     * <p>
     * The implementation uses specific multiplication and addition algorithms to preserve accuracy and reduce
     * cancellation effects. It should be very accurate even for nearly orthogonal vectors.
     * </p>
     * 
     * @see MathArrays#linearCombination(double, double, double, double, double, double)
     * @param v
     *        second vector
     * @return the dot product this.v
     */
    public T dotProduct(final Vector3D v) {
        return this.x.linearCombination(v.getX(), this.x, v.getY(), this.y, v.getZ(), this.z);
    }

    /**
     * Compute the cross-product of the instance with another vector.
     * 
     * @param v
     *        other vector
     * @return the cross product this ^ v as a new Vector3D
     */
    public FieldVector3D<T> crossProduct(final FieldVector3D<T> v) {
        return new FieldVector3D<>(this.x.linearCombination(this.y, v.z, this.z.negate(), v.y),
            this.y.linearCombination(this.z, v.x, this.x.negate(), v.z),
            this.z.linearCombination(this.x, v.y, this.y.negate(), v.x));
    }

    /**
     * Compute the cross-product of the instance with another vector.
     * 
     * @param v
     *        other vector
     * @return the cross product this ^ v as a new Vector3D
     */
    public FieldVector3D<T> crossProduct(final Vector3D v) {
        return new FieldVector3D<>(this.x.linearCombination(v.getZ(), this.y, -v.getY(), this.z),
            this.y.linearCombination(v.getX(), this.z, -v.getZ(), this.x),
            this.z.linearCombination(v.getY(), this.x, -v.getX(), this.y));
    }

    /**
     * Compute the distance between the instance and another vector according to the L<sub>1</sub> norm.
     * <p>
     * Calling this method is equivalent to calling: <code>q.subtract(p).getNorm1()</code> except that no intermediate
     * vector is built
     * </p>
     * 
     * @param v
     *        second vector
     * @return the distance between the instance and p according to the L<sub>1</sub> norm
     */
    public T distance1(final FieldVector3D<T> v) {
        final T dx = v.x.subtract(this.x).abs();
        final T dy = v.y.subtract(this.y).abs();
        final T dz = v.z.subtract(this.z).abs();
        return dx.add(dy).add(dz);
    }

    /**
     * Compute the distance between the instance and another vector according to the L<sub>1</sub> norm.
     * <p>
     * Calling this method is equivalent to calling: <code>q.subtract(p).getNorm1()</code> except that no intermediate
     * vector is built
     * </p>
     * 
     * @param v
     *        second vector
     * @return the distance between the instance and p according to the L<sub>1</sub> norm
     */
    public T distance1(final Vector3D v) {
        final T dx = this.x.subtract(v.getX()).abs();
        final T dy = this.y.subtract(v.getY()).abs();
        final T dz = this.z.subtract(v.getZ()).abs();
        return dx.add(dy).add(dz);
    }

    /**
     * Compute the distance between the instance and another vector according to the L<sub>2</sub> norm.
     * <p>
     * Calling this method is equivalent to calling: <code>q.subtract(p).getNorm()</code> except that no intermediate
     * vector is built
     * </p>
     * 
     * @param v
     *        second vector
     * @return the distance between the instance and p according to the L<sub>2</sub> norm
     */
    public T distance(final FieldVector3D<T> v) {
        final T dx = v.x.subtract(this.x);
        final T dy = v.y.subtract(this.y);
        final T dz = v.z.subtract(this.z);
        return dx.multiply(dx).add(dy.multiply(dy)).add(dz.multiply(dz)).sqrt();
    }

    /**
     * Compute the distance between the instance and another vector according to the L<sub>2</sub> norm.
     * <p>
     * Calling this method is equivalent to calling: <code>q.subtract(p).getNorm()</code> except that no intermediate
     * vector is built
     * </p>
     * 
     * @param v
     *        second vector
     * @return the distance between the instance and p according to the L<sub>2</sub> norm
     */
    public T distance(final Vector3D v) {
        final T dx = this.x.subtract(v.getX());
        final T dy = this.y.subtract(v.getY());
        final T dz = this.z.subtract(v.getZ());
        return dx.multiply(dx).add(dy.multiply(dy)).add(dz.multiply(dz)).sqrt();
    }

    /**
     * Compute the distance between the instance and another vector according to the L<sub>&infin;</sub> norm.
     * <p>
     * Calling this method is equivalent to calling: <code>q.subtract(p).getNormInf()</code> except that no intermediate
     * vector is built
     * </p>
     * 
     * @param v
     *        second vector
     * @return the distance between the instance and p according to the L<sub>&infin;</sub> norm
     */
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Commons-Math code kept as such
    public T distanceInf(final FieldVector3D<T> v) {
        // CHECKSTYLE: resume ReturnCount check
        // Absolute values of distance between the instance and vector v
        // for each of the three coordinates
        final T dx = v.x.subtract(this.x).abs();
        final T dy = v.y.subtract(this.y).abs();
        final T dz = v.z.subtract(this.z).abs();
        if (dx.getReal() <= dy.getReal()) {
            // return greater distance between dy and dz
            if (dy.getReal() <= dz.getReal()) {
                return dz;
            }
            return dy;
        }
        // return greater distance between dx and dz
        if (dx.getReal() <= dz.getReal()) {
            return dz;
        }
        return dx;
    }

    /**
     * Compute the distance between the instance and another vector according to the L<sub>&infin;</sub> norm.
     * <p>
     * Calling this method is equivalent to calling: <code>q.subtract(p).getNormInf()</code> except that no intermediate
     * vector is built
     * </p>
     * 
     * @param v
     *        second vector
     * @return the distance between the instance and p according to the L<sub>&infin;</sub> norm
     */
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Commons-Math code kept as such
    public T distanceInf(final Vector3D v) {
        // CHECKSTYLE: resume ReturnCount check
        // Compute x distance
        final T dx = this.x.subtract(v.getX()).abs();
        // Compute y distance
        final T dy = this.y.subtract(v.getY()).abs();
        // Compute z distance
        final T dz = this.z.subtract(v.getZ()).abs();
        // Return maximum between the three distances
        if (dx.getReal() <= dy.getReal()) {
            if (dy.getReal() <= dz.getReal()) {
                return dz;
            }
            return dy;
        }
        if (dx.getReal() <= dz.getReal()) {
            return dz;
        }
        return dx;
    }

    /**
     * Compute the square of the distance between the instance and another vector.
     * <p>
     * Calling this method is equivalent to calling: <code>q.subtract(p).getNormSq()</code> except that no intermediate
     * vector is built
     * </p>
     * 
     * @param v
     *        second vector
     * @return the square of the distance between the instance and p
     */
    public T distanceSq(final FieldVector3D<T> v) {
        final T dx = v.x.subtract(this.x);
        final T dy = v.y.subtract(this.y);
        final T dz = v.z.subtract(this.z);
        return dx.multiply(dx).add(dy.multiply(dy)).add(dz.multiply(dz));
    }

    /**
     * Compute the square of the distance between the instance and another vector.
     * <p>
     * Calling this method is equivalent to calling: <code>q.subtract(p).getNormSq()</code> except that no intermediate
     * vector is built
     * </p>
     * 
     * @param v
     *        second vector
     * @return the square of the distance between the instance and p
     */
    public T distanceSq(final Vector3D v) {
        final T dx = this.x.subtract(v.getX());
        final T dy = this.y.subtract(v.getY());
        final T dz = this.z.subtract(v.getZ());
        return dx.multiply(dx).add(dy.multiply(dy)).add(dz.multiply(dz));
    }

    /**
     * Compute the dot-product of two vectors.
     * 
     * @param v1
     *        first vector
     * @param v2
     *        second vector
     * @param <T>
     *        the type of the field elements
     * @return the dot product v1.v2
     */
    public static <T extends RealFieldElement<T>> T dotProduct(final FieldVector3D<T> v1,
                                                               final FieldVector3D<T> v2) {
        return v1.dotProduct(v2);
    }

    /**
     * Compute the dot-product of two vectors.
     * 
     * @param v1
     *        first vector
     * @param v2
     *        second vector
     * @param <T>
     *        the type of the field elements
     * @return the dot product v1.v2
     */
    public static <T extends RealFieldElement<T>> T dotProduct(final FieldVector3D<T> v1,
                                                               final Vector3D v2) {
        return v1.dotProduct(v2);
    }

    /**
     * Compute the dot-product of two vectors.
     * 
     * @param v1
     *        first vector
     * @param v2
     *        second vector
     * @param <T>
     *        the type of the field elements
     * @return the dot product v1.v2
     */
    public static <T extends RealFieldElement<T>> T dotProduct(final Vector3D v1,
                                                               final FieldVector3D<T> v2) {
        return v2.dotProduct(v1);
    }

    /**
     * Compute the cross-product of two vectors.
     * 
     * @param v1
     *        first vector
     * @param v2
     *        second vector
     * @param <T>
     *        the type of the field elements
     * @return the cross product v1 ^ v2 as a new Vector
     */
    public static <T extends RealFieldElement<T>> FieldVector3D<T> crossProduct(final FieldVector3D<T> v1,
                                                                                final FieldVector3D<T> v2) {
        return v1.crossProduct(v2);
    }

    /**
     * Compute the cross-product of two vectors.
     * 
     * @param v1
     *        first vector
     * @param v2
     *        second vector
     * @param <T>
     *        the type of the field elements
     * @return the cross product v1 ^ v2 as a new Vector
     */
    public static <T extends RealFieldElement<T>> FieldVector3D<T> crossProduct(final FieldVector3D<T> v1,
                                                                                final Vector3D v2) {
        return v1.crossProduct(v2);
    }

    /**
     * Compute the cross-product of two vectors.
     * 
     * @param v1
     *        first vector
     * @param v2
     *        second vector
     * @param <T>
     *        the type of the field elements
     * @return the cross product v1 ^ v2 as a new Vector
     */
    public static <T extends RealFieldElement<T>> FieldVector3D<T> crossProduct(final Vector3D v1,
                                                                                final FieldVector3D<T> v2) {
        return new FieldVector3D<>(v2.x.linearCombination(v1.getY(), v2.z, -v1.getZ(), v2.y),
            v2.y.linearCombination(v1.getZ(), v2.x, -v1.getX(), v2.z),
            v2.z.linearCombination(v1.getX(), v2.y, -v1.getY(), v2.x));
    }

    /**
     * Compute the distance between two vectors according to the L<sub>1</sub> norm.
     * <p>
     * Calling this method is equivalent to calling: <code>v1.subtract(v2).getNorm1()</code> except that no intermediate
     * vector is built
     * </p>
     * 
     * @param v1
     *        first vector
     * @param v2
     *        second vector
     * @param <T>
     *        the type of the field elements
     * @return the distance between v1 and v2 according to the L<sub>1</sub> norm
     */
    public static <T extends RealFieldElement<T>> T distance1(final FieldVector3D<T> v1,
                                                              final FieldVector3D<T> v2) {
        return v1.distance1(v2);
    }

    /**
     * Compute the distance between two vectors according to the L<sub>1</sub> norm.
     * <p>
     * Calling this method is equivalent to calling: <code>v1.subtract(v2).getNorm1()</code> except that no intermediate
     * vector is built
     * </p>
     * 
     * @param v1
     *        first vector
     * @param v2
     *        second vector
     * @param <T>
     *        the type of the field elements
     * @return the distance between v1 and v2 according to the L<sub>1</sub> norm
     */
    public static <T extends RealFieldElement<T>> T distance1(final FieldVector3D<T> v1,
                                                              final Vector3D v2) {
        return v1.distance1(v2);
    }

    /**
     * Compute the distance between two vectors according to the L<sub>1</sub> norm.
     * <p>
     * Calling this method is equivalent to calling: <code>v1.subtract(v2).getNorm1()</code> except that no intermediate
     * vector is built
     * </p>
     * 
     * @param v1
     *        first vector
     * @param v2
     *        second vector
     * @param <T>
     *        the type of the field elements
     * @return the distance between v1 and v2 according to the L<sub>1</sub> norm
     */
    public static <T extends RealFieldElement<T>> T distance1(final Vector3D v1,
                                                              final FieldVector3D<T> v2) {
        return v2.distance1(v1);
    }

    /**
     * Compute the distance between two vectors according to the L<sub>2</sub> norm.
     * <p>
     * Calling this method is equivalent to calling: <code>v1.subtract(v2).getNorm()</code> except that no intermediate
     * vector is built
     * </p>
     * 
     * @param v1
     *        first vector
     * @param v2
     *        second vector
     * @param <T>
     *        the type of the field elements
     * @return the distance between v1 and v2 according to the L<sub>2</sub> norm
     */
    public static <T extends RealFieldElement<T>> T distance(final FieldVector3D<T> v1,
                                                             final FieldVector3D<T> v2) {
        return v1.distance(v2);
    }

    /**
     * Compute the distance between two vectors according to the L<sub>2</sub> norm.
     * <p>
     * Calling this method is equivalent to calling: <code>v1.subtract(v2).getNorm()</code> except that no intermediate
     * vector is built
     * </p>
     * 
     * @param v1
     *        first vector
     * @param v2
     *        second vector
     * @param <T>
     *        the type of the field elements
     * @return the distance between v1 and v2 according to the L<sub>2</sub> norm
     */
    public static <T extends RealFieldElement<T>> T distance(final FieldVector3D<T> v1,
                                                             final Vector3D v2) {
        return v1.distance(v2);
    }

    /**
     * Compute the distance between two vectors according to the L<sub>2</sub> norm.
     * <p>
     * Calling this method is equivalent to calling: <code>v1.subtract(v2).getNorm()</code> except that no intermediate
     * vector is built
     * </p>
     * 
     * @param v1
     *        first vector
     * @param v2
     *        second vector
     * @param <T>
     *        the type of the field elements
     * @return the distance between v1 and v2 according to the L<sub>2</sub> norm
     */
    public static <T extends RealFieldElement<T>> T distance(final Vector3D v1,
                                                             final FieldVector3D<T> v2) {
        return v2.distance(v1);
    }

    /**
     * Compute the distance between two vectors according to the L<sub>&infin;</sub> norm.
     * <p>
     * Calling this method is equivalent to calling: <code>v1.subtract(v2).getNormInf()</code> except that no
     * intermediate vector is built
     * </p>
     * 
     * @param v1
     *        first vector
     * @param v2
     *        second vector
     * @param <T>
     *        the type of the field elements
     * @return the distance between v1 and v2 according to the L<sub>&infin;</sub> norm
     */
    public static <T extends RealFieldElement<T>> T distanceInf(final FieldVector3D<T> v1,
                                                                final FieldVector3D<T> v2) {
        return v1.distanceInf(v2);
    }

    /**
     * Compute the distance between two vectors according to the L<sub>&infin;</sub> norm.
     * <p>
     * Calling this method is equivalent to calling: <code>v1.subtract(v2).getNormInf()</code> except that no
     * intermediate vector is built
     * </p>
     * 
     * @param v1
     *        first vector
     * @param v2
     *        second vector
     * @param <T>
     *        the type of the field elements
     * @return the distance between v1 and v2 according to the L<sub>&infin;</sub> norm
     */
    public static <T extends RealFieldElement<T>> T distanceInf(final FieldVector3D<T> v1,
                                                                final Vector3D v2) {
        return v1.distanceInf(v2);
    }

    /**
     * Compute the distance between two vectors according to the L<sub>&infin;</sub> norm.
     * <p>
     * Calling this method is equivalent to calling: <code>v1.subtract(v2).getNormInf()</code> except that no
     * intermediate vector is built
     * </p>
     * 
     * @param v1
     *        first vector
     * @param v2
     *        second vector
     * @param <T>
     *        the type of the field elements
     * @return the distance between v1 and v2 according to the L<sub>&infin;</sub> norm
     */
    public static <T extends RealFieldElement<T>> T distanceInf(final Vector3D v1,
                                                                final FieldVector3D<T> v2) {
        return v2.distanceInf(v1);
    }

    /**
     * Compute the square of the distance between two vectors.
     * <p>
     * Calling this method is equivalent to calling: <code>v1.subtract(v2).getNormSq()</code> except that no
     * intermediate vector is built
     * </p>
     * 
     * @param v1
     *        first vector
     * @param v2
     *        second vector
     * @param <T>
     *        the type of the field elements
     * @return the square of the distance between v1 and v2
     */
    public static <T extends RealFieldElement<T>> T distanceSq(final FieldVector3D<T> v1,
                                                               final FieldVector3D<T> v2) {
        return v1.distanceSq(v2);
    }

    /**
     * Compute the square of the distance between two vectors.
     * <p>
     * Calling this method is equivalent to calling: <code>v1.subtract(v2).getNormSq()</code> except that no
     * intermediate vector is built
     * </p>
     * 
     * @param v1
     *        first vector
     * @param v2
     *        second vector
     * @param <T>
     *        the type of the field elements
     * @return the square of the distance between v1 and v2
     */
    public static <T extends RealFieldElement<T>> T distanceSq(final FieldVector3D<T> v1,
                                                               final Vector3D v2) {
        return v1.distanceSq(v2);
    }

    /**
     * Compute the square of the distance between two vectors.
     * <p>
     * Calling this method is equivalent to calling: <code>v1.subtract(v2).getNormSq()</code> except that no
     * intermediate vector is built
     * </p>
     * 
     * @param v1
     *        first vector
     * @param v2
     *        second vector
     * @param <T>
     *        the type of the field elements
     * @return the square of the distance between v1 and v2
     */
    public static <T extends RealFieldElement<T>> T distanceSq(final Vector3D v1,
                                                               final FieldVector3D<T> v2) {
        return v2.distanceSq(v1);
    }

    /**
     * Get a string representation of this vector.
     * 
     * @return a string representation of this vector
     */
    @Override
    public String toString() {
        return Vector3DFormat.getInstance().format(this.toVector3D());
    }

    /**
     * Get a string representation of this vector.
     * 
     * @param format
     *        the custom format for components
     * @return a string representation of this vector
     */
    public String toString(final NumberFormat format) {
        return new Vector3DFormat(format).format(this.toVector3D());
    }

    // CHECKSTYLE: resume MagicNumber check
}
