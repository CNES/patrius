/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
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
 * VERSION:4.11.1:FA:FA-74:30/06/2023:[PATRIUS] Reliquat OGM3320 hash code de Vector3D
 * VERSION:4.11:FA:FA-3320:22/05/2023:[PATRIUS] Mauvaise implementation de la methode hashCode de Vector3D
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-2922:15/11/2021:[PATRIUS] suppression de l'utilisation de la reflexion Java dans patrius 
 * VERSION:4.7:DM:DM-2909:18/05/2021:Ajout des methodes getIntersectionPoint, getClosestPoint(Vector2D) et getAlpha()
 * VERSION:4.7:DM:DM-2758:18/05/2021:Conversion de coordonnees cartesiennes en Coordonnees spheriques
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.geometry.euclidean.threed;

import java.text.NumberFormat;
import java.util.Objects;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.MathArithmeticException;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooLargeException;
import fr.cnes.sirius.patrius.math.geometry.Space;
import fr.cnes.sirius.patrius.math.geometry.Vector;
import fr.cnes.sirius.patrius.math.linear.ArrayRealVector;
import fr.cnes.sirius.patrius.math.linear.DecompositionSolver;
import fr.cnes.sirius.patrius.math.linear.MatrixUtils;
import fr.cnes.sirius.patrius.math.linear.QRDecomposition;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealVector;
import fr.cnes.sirius.patrius.math.linear.SingularMatrixException;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathArrays;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

//CHECKSTYLE: stop IllegalType check
//Reason: Commons-Math code kept as such

/**
 * This class implements vectors in a three-dimensional space.
 * <p>
 * Instance of this class are guaranteed to be immutable.
 * </p>
 * 
 * @version $Id: Vector3D.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 1.2
 */
public class Vector3D implements Vector<Euclidean3D> {

    /** Null vector (coordinates: 0, 0, 0). */
    public static final Vector3D ZERO = new Vector3D(0, 0, 0);

    /** First canonical vector (coordinates: 1, 0, 0). */
    public static final Vector3D PLUS_I = new Vector3D(1, 0, 0);

    /** Opposite of the first canonical vector (coordinates: -1, 0, 0). */
    public static final Vector3D MINUS_I = new Vector3D(-1, 0, 0);

    /** Second canonical vector (coordinates: 0, 1, 0). */
    public static final Vector3D PLUS_J = new Vector3D(0, 1, 0);

    /** Opposite of the second canonical vector (coordinates: 0, -1, 0). */
    public static final Vector3D MINUS_J = new Vector3D(0, -1, 0);

    /** Third canonical vector (coordinates: 0, 0, 1). */
    public static final Vector3D PLUS_K = new Vector3D(0, 0, 1);

    /** Opposite of the third canonical vector (coordinates: 0, 0, -1). */
    public static final Vector3D MINUS_K = new Vector3D(0, 0, -1);

    // CHECKSTYLE: stop ConstantName
    /** A vector with all coordinates set to NaN. */
    @SuppressWarnings("PMD.VariableNamingConventions")
    public static final Vector3D NaN = new Vector3D(Double.NaN, Double.NaN, Double.NaN);
    // CHECKSTYLE: resume ConstantName

    /** A vector with all coordinates set to positive infinity. */
    public static final Vector3D POSITIVE_INFINITY =
        new Vector3D(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);

    /** A vector with all coordinates set to negative infinity. */
    public static final Vector3D NEGATIVE_INFINITY =
        new Vector3D(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);

    /** Serializable UID. */
    private static final long serialVersionUID = 1313493323784566947L;

    /** Abscissa. */
    private final double x;

    /** Ordinate. */
    private final double y;

    /** Height. */
    private final double z;

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
    public Vector3D(final double xIn, final double yIn, final double zIn) {
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
    public Vector3D(final double[] v) {
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
     *        angle (&alpha;) between projection on XY-plane and X-axis counted in counter-clockwise around Z
     *        (0 is +X, &pi;/2 is +Y, &pi; is -X and 3&pi;/2 is -Y)
     * @param delta
     *        elevation (&delta;) above (XY) plane, from -&pi;/2 to +&pi;/2
     * @see #getAlpha()
     * @see #getDelta()
     */
    public Vector3D(final double alpha, final double delta) {
        final double[] sincosAlpha = MathLib.sinAndCos(alpha);
        final double sinAlpha = sincosAlpha[0];
        final double cosAlpha = sincosAlpha[1];
        final double[] sincosDelta = MathLib.sinAndCos(delta);
        final double sinDelta = sincosDelta[0];
        final double cosDelta = sincosDelta[1];
        
        this.x = cosAlpha * cosDelta;
        this.y = sinAlpha * cosDelta;
        this.z = sinDelta;
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
    public Vector3D(final double a, final Vector3D u) {
        this.x = a * u.x;
        this.y = a * u.y;
        this.z = a * u.z;
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
    public Vector3D(final double a1, final Vector3D u1, final double a2, final Vector3D u2) {
        this.x = MathArrays.linearCombination(a1, u1.x, a2, u2.x);
        this.y = MathArrays.linearCombination(a1, u1.y, a2, u2.y);
        this.z = MathArrays.linearCombination(a1, u1.z, a2, u2.z);
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
    public Vector3D(final double a1, final Vector3D u1, final double a2, final Vector3D u2,
        final double a3, final Vector3D u3) {
        this.x = MathArrays.linearCombination(a1, u1.x, a2, u2.x, a3, u3.x);
        this.y = MathArrays.linearCombination(a1, u1.y, a2, u2.y, a3, u3.y);
        this.z = MathArrays.linearCombination(a1, u1.z, a2, u2.z, a3, u3.z);
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
    public Vector3D(final double a1, final Vector3D u1, final double a2, final Vector3D u2,
        final double a3, final Vector3D u3, final double a4, final Vector3D u4) {
        this.x = MathArrays.linearCombination(a1, u1.x, a2, u2.x, a3, u3.x, a4, u4.x);
        this.y = MathArrays.linearCombination(a1, u1.y, a2, u2.y, a3, u3.y, a4, u4.y);
        this.z = MathArrays.linearCombination(a1, u1.z, a2, u2.z, a3, u3.z, a4, u4.z);
    }

    /**
     * From a RealVector constructor
     * Build a vector from a RealVector object. The input RealVector
     * dimension must be 3.
     * 
     * @param vector
     *        The RealVector
     */
    public Vector3D(final RealVector vector) {
        if (vector.getDimension() == 3) {
            this.x = vector.getEntry(0);
            this.y = vector.getEntry(1);
            this.z = vector.getEntry(2);
        } else {
            throw new MathIllegalArgumentException(PatriusMessages.DIMENSIONS_MISMATCH);
        }
    }

    /**
     * From a {@link SphericalCoordinates} constructor.
     * 
     * @param coord
     *        The spherical coordinates
     */
    public Vector3D(final SphericalCoordinates coord) {
        final Vector3D res = coord.getCartesianCoordinates();
        this.x = res.x;
        this.y = res.y;
        this.z = res.z;
    }

    /**
     * Get the abscissa of the vector.
     * 
     * @return abscissa of the vector
     * @see #Vector3D(double, double, double)
     */
    public double getX() {
        return this.x;
    }

    /**
     * Get the ordinate of the vector.
     * 
     * @return ordinate of the vector
     * @see #Vector3D(double, double, double)
     */
    public double getY() {
        return this.y;
    }

    /**
     * Get the height of the vector.
     * 
     * @return height of the vector
     * @see #Vector3D(double, double, double)
     */
    public double getZ() {
        return this.z;
    }

    /**
     * Get a RealVector with identical data.
     * 
     * @return the RealVector
     * @see RealVector
     */
    @Override
    public RealVector getRealVector() {
        final double[] data = { this.x, this.y, this.z };
        return new ArrayRealVector(data);
    }

    /**
     * Get the vector coordinates as a dimension 3 array.
     * 
     * @return vector coordinates
     * @see #Vector3D(double[])
     */
    public double[] toArray() {
        return new double[] { this.x, this.y, this.z };
    }

    /** {@inheritDoc} */
    @Override
    public Space getSpace() {
        return Euclidean3D.getInstance();
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getZero() {
        return ZERO;
    }

    /**
     * Indicates if this vector has all its components to 0.
     * 
     * @return
     *         true if all the components are 0.
     */
    public boolean isZero() {
        return this.x == 0 && this.y == 0 && this.z == 0;
    }

    /** {@inheritDoc} */
    @Override
    public double getNorm1() {
        return MathLib.abs(this.x) + MathLib.abs(this.y) + MathLib.abs(this.z);
    }

    /** {@inheritDoc} */
    @Override
    public double getNorm() {
        // there are no cancellation problems here, so we use the straightforward formula
        return MathLib.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
    }

    /** {@inheritDoc} */
    @Override
    public double getNormSq() {
        // there are no cancellation problems here, so we use the straightforward formula
        return this.x * this.x + this.y * this.y + this.z * this.z;
    }

    /** {@inheritDoc} */
    @Override
    public double getNormInf() {
        return MathLib.max(MathLib.max(MathLib.abs(this.x), MathLib.abs(this.y)), MathLib.abs(this.z));
    }

    /**
     * For a given vector, get the angle between projection on XY-plane and X-axis counted in counter-clockwise
     * direction: 0 corresponds to Vector3D(1, 0, ...), and increasing values are counter-clockwise.
     * 
     * @return the angle between projection on XY-plane and X-axis counted in counter-clockwise direction (&alpha;) of
     *         the vector, between -&pi; and +&pi;
     * @see #Vector3D(double, double)
     */
    public double getAlpha() {
        return MathLib.atan2(this.y, this.x);
    }

    /**
     * Get the elevation of the vector.
     * 
     * @return elevation (&delta;) of the vector, between -&pi;/2 and +&pi;/2
     * @see #Vector3D(double, double)
     */
    public double getDelta() {
        return MathLib.asin(this.z / this.getNorm());
    }

    /**
     * Returns the spherical coordinates.
     * @return the spherical coordinates (&delta;, &alpha;, norm) / (latitude, longitude, altitude)
     */
    public SphericalCoordinates getSphericalCoordinates() {
        return new SphericalCoordinates(getDelta(), getAlpha(), getNorm());
    }
    
    /** {@inheritDoc} */
    @Override
    public Vector3D add(final Vector<Euclidean3D> v) {
        final Vector3D v3 = (Vector3D) v;
        return new Vector3D(this.x + v3.x, this.y + v3.y, this.z + v3.z);
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D add(final double factor, final Vector<Euclidean3D> v) {
        return new Vector3D(1, this, factor, (Vector3D) v);
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D subtract(final Vector<Euclidean3D> v) {
        final Vector3D v3 = (Vector3D) v;
        return new Vector3D(this.x - v3.x, this.y - v3.y, this.z - v3.z);
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D subtract(final double factor, final Vector<Euclidean3D> v) {
        return new Vector3D(1, this, -factor, (Vector3D) v);
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D normalize() {
        final double s = this.getNorm();
        if (s == 0) {
            throw new MathArithmeticException(PatriusMessages.CANNOT_NORMALIZE_A_ZERO_NORM_VECTOR);
        }
        return this.scalarMultiply(1 / s);
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
    public Vector3D orthogonal() {

        final double threshold = 0.6 * this.getNorm();
        if (threshold == 0) {
            // Null norm
            throw new MathArithmeticException(PatriusMessages.ZERO_NORM);
        }

        final Vector3D res;
        if ((this.x >= -threshold) && (this.x <= threshold)) {
            final double inverse = 1 / MathLib.sqrt(this.y * this.y + this.z * this.z);
            res = new Vector3D(0, inverse * this.z, -inverse * this.y);
        } else if ((this.y >= -threshold) && (this.y <= threshold)) {
            final double inverse = 1 / MathLib.sqrt(this.x * this.x + this.z * this.z);
            res = new Vector3D(-inverse * this.z, 0, inverse * this.x);
        } else {
            // Normal case
            final double inverse = 1 / MathLib.sqrt(this.x * this.x + this.y * this.y);
            res = new Vector3D(inverse * this.y, -inverse * this.x, 0);
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
     * @return angular separation between v1 and v2
     * @exception MathArithmeticException
     *            if either vector has a null norm
     */
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Commons-Math code kept as such
    public static double angle(final Vector3D v1, final Vector3D v2) {
        // CHECKSTYLE: resume ReturnCount check

        final double normProduct = v1.getNorm() * v2.getNorm();
        if (normProduct == 0) {
            throw new MathArithmeticException(PatriusMessages.ZERO_NORM);
        }

        final double dot = v1.dotProduct(v2);
        final double threshold = normProduct * 0.9999;
        if ((dot < -threshold) || (dot > threshold)) {
            // the vectors are almost aligned, compute using the sine
            final Vector3D v3 = crossProduct(v1, v2);
            if (dot >= 0) {
                return MathLib.asin(v3.getNorm() / normProduct);
            }
            return FastMath.PI - MathLib.asin(v3.getNorm() / normProduct);
        }

        // the vectors are sufficiently separated to use the cosine
        return MathLib.acos(dot / normProduct);

    }

    /** {@inheritDoc} */
    @Override
    public Vector3D negate() {
        return new Vector3D(-this.x, -this.y, -this.z);
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D scalarMultiply(final double a) {
        return new Vector3D(a * this.x, a * this.y, a * this.z);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isNaN() {
        return Double.isNaN(this.x) || Double.isNaN(this.y) || Double.isNaN(this.z);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isInfinite() {
        return !this.isNaN() && (Double.isInfinite(this.x) || Double.isInfinite(this.y) || Double.isInfinite(this.z));
    }

    /**
     * Test for the equality of two 3D vectors.
     * <p>
     * If all coordinates of two 3D vectors are exactly the same, and none are <code>Double.NaN</code>, the two 3D
     * vectors are considered to be equal.
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
        boolean isEqual = false;
        // Check if the two objects are equal
        if (other == this) {
            // Identity
            isEqual = true;
        } else if ((other != null) && (other.getClass() == this.getClass())) {
            // Same object type: check all attributes
            final Vector3D vec = (Vector3D) other;
            isEqual = Double.doubleToLongBits(this.x) == Double.doubleToLongBits(vec.x)
                    && Double.doubleToLongBits(this.y) == Double.doubleToLongBits(vec.y)
                    && Double.doubleToLongBits(this.z) == Double.doubleToLongBits(vec.z);
        }
        
        // Return a boolean saying whether the two objects are equal or not
        return isEqual;
    }

    // CHECKSTYLE: stop MagicNumber check
    // Reason: model - Commons-Math code

    /**
     * Get a hashCode for the 3D vector.
     * 
     * @return a hash code value for this object
     */
    @Override
    public int hashCode() {
        // Compute and return the hash code of this Vector3D
        return Objects.hash(this.x, this.y, this.z);
    }

    // CHECKSTYLE: resume MagicNumber check

    /**
     * {@inheritDoc}
     * <p>
     * The implementation uses specific multiplication and addition algorithms to preserve accuracy and reduce
     * cancellation effects. It should be very accurate even for nearly orthogonal vectors.
     * </p>
     * 
     * @see MathArrays#linearCombination(double, double, double, double, double, double)
     */
    @Override
    public double dotProduct(final Vector<Euclidean3D> v) {
        final Vector3D v3 = (Vector3D) v;
        return MathArrays.linearCombination(this.x, v3.x, this.y, v3.y, this.z, v3.z);
    }

    /**
     * Compute the cross-product of the instance with another vector.
     * 
     * @param v
     *        other vector
     * @return the cross product this ^ v as a new Vector3D
     */
    @SuppressWarnings("PMD.LooseCoupling")
    public Vector3D crossProduct(final Vector<Euclidean3D> v) {
        final Vector3D v3 = (Vector3D) v;
        return new Vector3D(MathArrays.linearCombination(this.y, v3.z, -this.z, v3.y),
            MathArrays.linearCombination(this.z, v3.x, -this.x, v3.z),
            MathArrays.linearCombination(this.x, v3.y, -this.y, v3.x));
    }

    /** {@inheritDoc} */
    @Override
    public double distance1(final Vector<Euclidean3D> v) {
        final Vector3D v3 = (Vector3D) v;
        final double dx = MathLib.abs(v3.x - this.x);
        final double dy = MathLib.abs(v3.y - this.y);
        final double dz = MathLib.abs(v3.z - this.z);
        return dx + dy + dz;
    }

    /** {@inheritDoc} */
    @Override
    public double distance(final Vector<Euclidean3D> v) {
        final Vector3D v3 = (Vector3D) v;
        final double dx = v3.x - this.x;
        final double dy = v3.y - this.y;
        final double dz = v3.z - this.z;
        return MathLib.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /** {@inheritDoc} */
    @Override
    public double distanceInf(final Vector<Euclidean3D> v) {
        final Vector3D v3 = (Vector3D) v;
        final double dx = MathLib.abs(v3.x - this.x);
        final double dy = MathLib.abs(v3.y - this.y);
        final double dz = MathLib.abs(v3.z - this.z);
        return MathLib.max(MathLib.max(dx, dy), dz);
    }

    /** {@inheritDoc} */
    @Override
    public double distanceSq(final Vector<Euclidean3D> v) {
        final Vector3D v3 = (Vector3D) v;
        final double dx = v3.x - this.x;
        final double dy = v3.y - this.y;
        final double dz = v3.z - this.z;
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Compute the dot-product of two vectors.
     * 
     * @param v1
     *        first vector
     * @param v2
     *        second vector
     * @return the dot product v1.v2
     */
    public static double dotProduct(final Vector3D v1, final Vector3D v2) {
        return v1.dotProduct(v2);
    }

    /**
     * Compute the cross-product of two vectors.
     * 
     * @param v1
     *        first vector
     * @param v2
     *        second vector
     * @return the cross product v1 ^ v2 as a new Vector
     */
    public static Vector3D crossProduct(final Vector3D v1, final Vector3D v2) {
        return v1.crossProduct(v2);
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
     * @return the distance between v1 and v2 according to the L<sub>1</sub> norm
     */
    public static double distance1(final Vector3D v1, final Vector3D v2) {
        return v1.distance1(v2);
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
     * @return the distance between v1 and v2 according to the L<sub>2</sub> norm
     */
    public static double distance(final Vector3D v1, final Vector3D v2) {
        return v1.distance(v2);
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
     * @return the distance between v1 and v2 according to the L<sub>&infin;</sub> norm
     */
    public static double distanceInf(final Vector3D v1, final Vector3D v2) {
        return v1.distanceInf(v2);
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
     * @return the square of the distance between v1 and v2
     */
    public static double distanceSq(final Vector3D v1, final Vector3D v2) {
        return v1.distanceSq(v2);
    }

    /**
     * Get a string representation of this vector.
     * 
     * @return a string representation of this vector
     */
    @Override
    public String toString() {
        return Vector3DFormat.getInstance().format(this);
    }

    /** {@inheritDoc} */
    @Override
    public String toString(final NumberFormat format) {
        return new Vector3DFormat(format).format(this);
    }


    /**
     * Find a vector from two known cross products.
     * <p>
     * We want to find Ω such that: Ω ⨯ v₁ = c₁ and Ω ⨯ v₂ = c₂
     * </p>
     * <p>
     * The first equation (Ω ⨯ v₁ = c₁) will always be fulfilled exactly, and the second one will be fulfilled if
     * possible.
     * </p>
     * 
     * @param v1
     *        vector forming the first known cross product
     * @param c1
     *        know vector for cross product Ω ⨯ v₁
     * @param v2
     *        vector forming the second known cross product
     * @param c2
     *        know vector for cross product Ω ⨯ v₂
     * @param tolerance
     *        relative tolerance factor used to check singularities
     * @return vector Ω such that: Ω ⨯ v₁ = c₁ and Ω ⨯ v₂ = c₂
     * @exception MathIllegalArgumentException
     *            if vectors are inconsistent and
     *            no solution can be found
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Commons-Math code kept as such
    @SuppressWarnings("PMD.PreserveStackTrace")
    public static Vector3D inverseCrossProducts(final Vector3D v1, final Vector3D c1,
                                                 final Vector3D v2, final Vector3D c2,
                                                 final double tolerance) {
        // CHECKSTYLE: resume CyclomaticComplexity check
        final double v12 = v1.getNormSq();
        final double v1n = MathLib.sqrt(v12);
        final double v22 = v2.getNormSq();
        final double v2n = MathLib.sqrt(v22);
        final double threshold = tolerance * MathLib.max(v1n, v2n);

        Vector3D omega;

        try {
            // create the over-determined linear system representing the two cross products
            final RealMatrix m = MatrixUtils.createRealMatrix(6, 3);
            m.setEntry(0, 1, v1.getZ());
            m.setEntry(0, 2, -v1.getY());
            m.setEntry(1, 0, -v1.getZ());
            m.setEntry(1, 2, v1.getX());
            m.setEntry(2, 0, v1.getY());
            m.setEntry(2, 1, -v1.getX());
            m.setEntry(3, 1, v2.getZ());
            m.setEntry(3, 2, -v2.getY());
            m.setEntry(4, 0, -v2.getZ());
            m.setEntry(4, 2, v2.getX());
            m.setEntry(5, 0, v2.getY());
            m.setEntry(5, 1, -v2.getX());

            final RealVector rhs = MatrixUtils.createRealVector(new double[] {
                c1.getX(), c1.getY(), c1.getZ(),
                c2.getX(), c2.getY(), c2.getZ()
            });

            // find the best solution we can
            final DecompositionSolver solver = new QRDecomposition(m, threshold).getSolver();
            final RealVector v = solver.solve(rhs);
            omega = new Vector3D(v.getEntry(0), v.getEntry(1), v.getEntry(2));

        } catch (final SingularMatrixException sme) {

            // handle some special cases for which we can compute a solution
            final double c12 = c1.getNormSq();
            final double c1n = MathLib.sqrt(c12);
            final double c22 = c2.getNormSq();
            final double c2n = MathLib.sqrt(c22);

            if (c1n <= threshold && c2n <= threshold) {
                // simple special case, velocities are cancelled
                return Vector3D.ZERO;
            } else if (v1n <= threshold && c1n >= threshold) {
                // this is inconsistent, if v₁ is zero, c₁ must be 0 too
                throw new NumberIsTooLargeException(c1n, 0, true);
            } else if (v2n <= threshold && c2n >= threshold) {
                // this is inconsistent, if v₂ is zero, c₂ must be 0 too
                throw new NumberIsTooLargeException(c2n, 0, true);
            } else if (Vector3D.crossProduct(v1, v2).getNorm() <= threshold && v12 > threshold) {
                // simple special case, v₂ is redundant with v₁, we just ignore it
                // use the simplest Ω: orthogonal to both v₁ and c₁
                omega = new Vector3D(1.0 / v12, Vector3D.crossProduct(v1, c1));
            } else {
                throw sme;
            }

        }

        // check results
        final double d1 = Vector3D.distance(Vector3D.crossProduct(omega, v1), c1);
        if (d1 > threshold) {
            throw new NumberIsTooLargeException(d1, 0, true);
        }

        final double d2 = Vector3D.distance(Vector3D.crossProduct(omega, v2), c2);
        if (d2 > threshold) {
            throw new NumberIsTooLargeException(d2, 0, true);
        }

        return omega;
    }

    // CHECKSTYLE: resume IllegalType check
}
