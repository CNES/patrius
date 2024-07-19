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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.geometry.euclidean.threed;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.exception.MathArithmeticException;
import fr.cnes.sirius.patrius.math.geometry.Vector;
import fr.cnes.sirius.patrius.math.geometry.euclidean.oned.Vector1D;
import fr.cnes.sirius.patrius.math.geometry.euclidean.twod.Euclidean2D;
import fr.cnes.sirius.patrius.math.geometry.euclidean.twod.PolygonsSet;
import fr.cnes.sirius.patrius.math.geometry.euclidean.twod.Vector2D;
import fr.cnes.sirius.patrius.math.geometry.partitioning.Embedding;
import fr.cnes.sirius.patrius.math.geometry.partitioning.Hyperplane;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

//CHECKSTYLE: stop IllegalType check
//Reason: Commons-Math code kept as such

/**
 * The class represent planes in a three dimensional space.
 * 
 * @version $Id: Plane.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.0
 */
public class Plane implements Hyperplane<Euclidean3D>, Embedding<Euclidean3D, Euclidean2D>, InfiniteShape,
    Serializable {

     /** Serializable UID. */
    private static final long serialVersionUID = -6611519133283486360L;

    /** Threshold. */
    private static final double THRESHOLD = 1.0e-10;

    /** Offset of the origin with respect to the plane. */
    private double originOffset;

    /** Origin of the plane frame. */
    private Vector3D origin;

    /** First vector of the plane frame (in plane). */
    private Vector3D u;

    /** Second vector of the plane frame (in plane). */
    private Vector3D v;

    /** Third vector of the plane frame (plane normal). */
    private Vector3D w;

    /**
     * Build a plane normal to a given direction and containing the origin.
     * 
     * @param normal
     *        normal direction to the plane
     * @exception MathArithmeticException
     *            if the normal norm is too small
     */
    public Plane(final Vector3D normal) {
        this.setNormal(normal);
        this.originOffset = 0;
        this.setFrame();
    }

    /**
     * Build a plane from a point and a normal.
     * 
     * @param p
     *        point belonging to the plane
     * @param normal
     *        normal direction to the plane
     * @exception MathArithmeticException
     *            if the normal norm is too small
     */
    public Plane(final Vector3D p, final Vector3D normal) {
        this.setNormal(normal);
        this.originOffset = -p.dotProduct(this.w);
        this.setFrame();
    }

    /**
     * Build a plane from three points.
     * <p>
     * The plane is oriented in the direction of {@code (p2-p1) ^ (p3-p1)}
     * </p>
     * 
     * @param p1
     *        first point belonging to the plane
     * @param p2
     *        second point belonging to the plane
     * @param p3
     *        third point belonging to the plane
     * @exception MathArithmeticException
     *            if the points do not constitute a plane
     */
    public Plane(final Vector3D p1, final Vector3D p2, final Vector3D p3) {
        this(p1, p2.subtract(p1).crossProduct(p3.subtract(p1)));
    }

    /**
     * Build a plane from a point and two vectors.
     * <p>
     * The plane is oriented in the direction of {@code (v1) ^ (v2)}
     * </p>
     * 
     * @param p
     *        first point belonging to the plane
     * @param v1
     *        second point belonging to the plane
     * @param v2
     *        third point belonging to the plane
     * @param isFrame
     *        boolean to choose if the given point and vectors constitute the local frame
     *        (they may be not orthogonal)
     * @exception IllegalArgumentException
     *            if the point and vectors do not constitute a plane
     */
    public Plane(final Vector3D p, final Vector3D v1, final Vector3D v2, final boolean isFrame) {
        this.setNormal(Vector3D.crossProduct(v1, v2));
        this.originOffset = -Vector3D.dotProduct(p, this.w);
        if (isFrame) {
            // the given points and vectors constitute the local frame
            // but may not be orthogonal.
            this.origin = p;
            this.u = v1;
            this.v = v2;
        } else {
            // the frame is created automatically
            this.setFrame();
        }
    }

    /**
     * Build a plane from a line and a vector.
     * <p>
     * The plane is created to contain the line and be directed by the vector
     * </p>
     * 
     * @param line
     *        a line belonging to the plane
     * @param vector
     *        a vector belonging to the plane
     * @exception IllegalArgumentException
     *            if the line and vector do not constitute a plane
     */
    public Plane(final Line line, final Vector3D vector) {
        this(line.getOrigin(), line.getOrigin().add(line.getDirection()), line.getOrigin().add(vector));
    }

    /**
     * Build a plane from a line and a point out of the line.
     * <p>
     * The plane is created to contain the line and the point
     * </p>
     * 
     * @param point
     *        a point belonging to the plane
     * @param line
     *        a line belonging to the plane
     * @exception IllegalArgumentException
     *            if the line and point do not constitute a plane
     */
    public Plane(final Vector3D point, final Line line) {
        this(line.getOrigin(), line.getOrigin().add(line.getDirection()), point);
    }

    /**
     * Copy constructor.
     * <p>
     * The instance created is completely independant of the original one. A deep copy is used, none of the underlying
     * object are shared.
     * </p>
     * 
     * @param plane
     *        plane to copy
     */
    public Plane(final Plane plane) {
        this.originOffset = plane.originOffset;
        this.origin = plane.origin;
        this.u = plane.u;
        this.v = plane.v;
        this.w = plane.w;
    }

    /**
     * Copy the instance.
     * <p>
     * The instance created is completely independant of the original one. A deep copy is used, none of the underlying
     * objects are shared (except for immutable objects).
     * </p>
     * 
     * @return a new hyperplane, copy of the instance
     */
    @Override
    public Plane copySelf() {
        return new Plane(this);
    }

    /**
     * Reset the instance as if built from a point and a normal.
     * 
     * @param p
     *        point belonging to the plane
     * @param normal
     *        normal direction to the plane
     * @exception MathArithmeticException
     *            if the normal norm is too small
     */
    public void reset(final Vector3D p, final Vector3D normal) {
        this.setNormal(normal);
        this.originOffset = -p.dotProduct(this.w);
        this.setFrame();
    }

    /**
     * Reset the instance from another one.
     * <p>
     * The updated instance is completely independant of the original one. A deep reset is used none of the underlying
     * object is shared.
     * </p>
     * 
     * @param original
     *        plane to reset from
     */
    public void reset(final Plane original) {
        this.originOffset = original.originOffset;
        this.origin = original.origin;
        this.u = original.u;
        this.v = original.v;
        this.w = original.w;
    }

    /**
     * Set the normal vactor.
     * 
     * @param normal
     *        normal direction to the plane (will be copied)
     * @exception MathArithmeticException
     *            if the normal norm is too small
     */
    private void setNormal(final Vector3D normal) {
        final double norm = normal.getNorm();
        if (norm < THRESHOLD) {
            throw new MathArithmeticException(PatriusMessages.ZERO_NORM);
        }
        this.w = new Vector3D(1.0 / norm, normal);
    }

    /**
     * Reset the plane frame.
     */
    private void setFrame() {
        this.origin = new Vector3D(-this.originOffset, this.w);
        this.u = this.w.orthogonal();
        this.v = Vector3D.crossProduct(this.w, this.u);
    }

    /**
     * Get the origin point of the plane frame.
     * <p>
     * The point returned is the orthogonal projection of the 3D-space origin in the plane.
     * </p>
     * 
     * @return the origin point of the plane frame (point closest to the
     *         3D-space origin)
     */
    public Vector3D getOrigin() {
        return this.origin;
    }

    /**
     * Get the normalized normal vector.
     * <p>
     * The frame defined by ({@link #getU getU}, {@link #getV getV}, {@link #getNormal getNormal}) is a rigth-handed
     * orthonormalized frame).
     * </p>
     * 
     * @return normalized normal vector
     * @see #getU
     * @see #getV
     */
    public Vector3D getNormal() {
        return this.w;
    }

    /**
     * Get the plane first canonical vector.
     * <p>
     * The frame defined by ({@link #getU getU}, {@link #getV getV}, {@link #getNormal getNormal}) is a rigth-handed
     * orthonormalized frame).
     * </p>
     * 
     * @return normalized first canonical vector
     * @see #getV
     * @see #getNormal
     */
    public Vector3D getU() {
        return this.u;
    }

    /**
     * Get the plane second canonical vector.
     * <p>
     * The frame defined by ({@link #getU getU}, {@link #getV getV}, {@link #getNormal getNormal}) is a rigth-handed
     * orthonormalized frame).
     * </p>
     * 
     * @return normalized second canonical vector
     * @see #getU
     * @see #getNormal
     */
    public Vector3D getV() {
        return this.v;
    }

    /**
     * Revert the plane.
     * <p>
     * Replace the instance by a similar plane with opposite orientation.
     * </p>
     * <p>
     * The new plane frame is chosen in such a way that a 3D point that had {@code (x, y)} in-plane coordinates and
     * {@code z} offset with respect to the plane and is unaffected by the change will have {@code (y, x)} in-plane
     * coordinates and {@code -z} offset with respect to the new plane. This means that the {@code u} and {@code v}
     * vectors returned by the {@link #getU} and {@link #getV} methods are exchanged, and the {@code w} vector returned
     * by the {@link #getNormal} method is reversed.
     * </p>
     */
    public void revertSelf() {
        final Vector3D tmp = this.u;
        this.u = this.v;
        this.v = tmp;
        this.w = this.w.negate();
        this.originOffset = -this.originOffset;
    }

    /**
     * Transform a 3D space point into an in-plane point.
     * 
     * @param point
     *        point of the space (must be a {@link Vector3D
     *        Vector3D} instance)
     * @return in-plane point (really a {@link fr.cnes.sirius.patrius.math.geometry.euclidean.twod.Vector2D Vector2D}
     *         instance)
     * @see #toSpace
     */
    @Override
    public Vector2D toSubSpace(final Vector<Euclidean3D> point) {
        return new Vector2D(point.dotProduct(this.u), point.dotProduct(this.v));
    }

    /**
     * Transform an in-plane point into a 3D space point.
     * 
     * @param point
     *        in-plane point (must be a {@link fr.cnes.sirius.patrius.math.geometry.euclidean.twod.Vector2D
     *        Vector2D} instance)
     * @return 3D space point (really a {@link Vector3D Vector3D} instance)
     * @see #toSubSpace
     */
    @Override
    public Vector3D toSpace(final Vector<Euclidean2D> point) {
        final Vector2D p2D = (Vector2D) point;
        return new Vector3D(p2D.getX(), this.u, p2D.getY(), this.v, -this.originOffset, this.w);
    }

    /**
     * Get one point from the 3D-space.
     * 
     * @param plane
     *        desired in-plane coordinates for the point in the
     *        plane
     * @param offset
     *        desired offset for the point
     * @return one point in the 3D-space, with given coordinates and offset
     *         relative to the plane
     */
    public Vector3D getPointAt(final Vector2D plane, final double offset) {
        return new Vector3D(plane.getX(), this.u, plane.getY(), this.v, offset - this.originOffset, this.w);
    }

    /**
     * Check if the instance is similar to another plane.
     * <p>
     * Planes are considered similar if they contain the same points. This does not mean they are equal since they can
     * have opposite normals.
     * </p>
     * 
     * @param plane
     *        plane to which the instance is compared
     * @return true if the planes are similar
     */
    public boolean isSimilarTo(final Plane plane) {
        final double angle = Vector3D.angle(this.w, plane.w);
        return ((angle < THRESHOLD) && (MathLib.abs(this.originOffset - plane.originOffset) < THRESHOLD))
                || ((angle > (FastMath.PI - THRESHOLD))
                && (MathLib.abs(this.originOffset + plane.originOffset) < THRESHOLD));
    }

    /**
     * Rotate the plane around the specified point.
     * <p>
     * The instance is not modified, a new instance is created.
     * </p>
     * 
     * @param center
     *        rotation center
     * @param rotation
     *        vectorial rotation operator
     * @return a new plane
     */
    public Plane rotate(final Vector3D center, final Rotation rotation) {

        final Vector3D delta = this.origin.subtract(center);
        final Plane plane = new Plane(center.add(rotation.applyTo(delta)),
            rotation.applyTo(this.w));

        // make sure the frame is transformed as desired
        plane.u = rotation.applyTo(this.u);
        plane.v = rotation.applyTo(this.v);

        return plane;

    }

    /**
     * Translate the plane by the specified amount.
     * <p>
     * The instance is not modified, a new instance is created.
     * </p>
     * 
     * @param translation
     *        translation to apply
     * @return a new plane
     */
    public Plane translate(final Vector3D translation) {

        final Plane plane = new Plane(this.origin.add(translation), this.w);

        // make sure the frame is transformed as desired
        plane.u = this.u;
        plane.v = this.v;

        return plane;

    }

    /**
     * Get the intersection of a line with the instance.
     * 
     * @param line
     *        line intersecting the instance
     * @return intersection point between between the line and the
     *         instance (null if the line is parallel to the instance)
     */
    public Vector3D intersection(final Line line) {
        final Vector3D direction = line.getDirection();
        final double dot = this.w.dotProduct(direction);
        if (MathLib.abs(dot) < THRESHOLD) {
            return null;
        }
        final Vector3D point = line.toSpace(Vector1D.ZERO);
        final double k = -(this.originOffset + this.w.dotProduct(point)) / dot;
        return new Vector3D(1.0, point, k, direction);
    }

    /**
     * Build the line shared by the instance and another plane.
     * 
     * @param other
     *        other plane
     * @return line at the intersection of the instance and the
     *         other plane (really a {@link Line Line} instance)
     */
    public Line intersection(final Plane other) {
        final Vector3D direction = Vector3D.crossProduct(this.w, other.w);
        if (direction.getNorm() < THRESHOLD) {
            return null;
        }
        final Vector3D point = intersection(this, other, new Plane(direction));
        return new Line(point, point.add(direction));
    }

    /**
     * Get the intersection point of three planes.
     * 
     * @param plane1
     *        first plane1
     * @param plane2
     *        second plane2
     * @param plane3
     *        third plane2
     * @return intersection point of three planes, null if some planes are parallel
     */
    public static Vector3D intersection(final Plane plane1, final Plane plane2, final Plane plane3) {

        // coefficients of the three planes linear equations
        final double a1 = plane1.w.getX();
        final double b1 = plane1.w.getY();
        final double c1 = plane1.w.getZ();

        final double a2 = plane2.w.getX();
        final double b2 = plane2.w.getY();
        final double c2 = plane2.w.getZ();

        final double a3 = plane3.w.getX();
        final double b3 = plane3.w.getY();
        final double c3 = plane3.w.getZ();

        // direct Cramer resolution of the linear system
        // (this is still feasible for a 3x3 system)
        final double a23 = b2 * c3 - b3 * c2;
        final double b23 = c2 * a3 - c3 * a2;
        final double c23 = a2 * b3 - a3 * b2;
        // Compute determinant
        final double determinant = a1 * a23 + b1 * b23 + c1 * c23;
        if (MathLib.abs(determinant) < THRESHOLD) {
            return null;
        }

        // Offset
        final double d1 = plane1.originOffset;
        final double d2 = plane2.originOffset;
        final double d3 = plane3.originOffset;

        // Solution
        final double r = 1.0 / determinant;
        return new Vector3D(
            (-a23 * d1 - (c1 * b3 - c3 * b1) * d2 - (c2 * b1 - c1 * b2) * d3) * r,
            (-b23 * d1 - (c3 * a1 - c1 * a3) * d2 - (c1 * a2 - c2 * a1) * d3) * r,
            (-c23 * d1 - (b1 * a3 - b3 * a1) * d2 - (b2 * a1 - b1 * a2) * d3) * r);
    }

    /**
     * Build a region covering the whole hyperplane.
     * 
     * @return a region covering the whole hyperplane
     */
    @Override
    public SubPlane wholeHyperplane() {
        return new SubPlane(this, new PolygonsSet());
    }

    /**
     * Build a region covering the whole space.
     * 
     * @return a region containing the instance (really a {@link PolyhedronsSet PolyhedronsSet} instance)
     */
    @Override
    public PolyhedronsSet wholeSpace() {
        return new PolyhedronsSet();
    }

    /**
     * Check if the instance contains a point.
     * 
     * @param p
     *        point to check
     * @return true if p belongs to the plane
     */
    public boolean contains(final Vector3D p) {
        return MathLib.abs(this.getOffset(p)) < THRESHOLD;
    }

    /**
     * Get the offset (oriented distance) of a parallel plane.
     * <p>
     * This method should be called only for parallel planes otherwise the result is not meaningful.
     * </p>
     * <p>
     * The offset is 0 if both planes are the same, it is positive if the plane is on the plus side of the instance and
     * negative if it is on the minus side, according to its natural orientation.
     * </p>
     * 
     * @param plane
     *        plane to check
     * @return offset of the plane
     */
    public double getOffset(final Plane plane) {
        return this.originOffset + (this.sameOrientationAs(plane) ? -plane.originOffset : plane.originOffset);
    }

    /**
     * Get the offset (oriented distance) of a point.
     * <p>
     * The offset is 0 if the point is on the underlying hyperplane, it is positive if the point is on one particular
     * side of the hyperplane, and it is negative if the point is on the other side, according to the hyperplane natural
     * orientation.
     * </p>
     * 
     * @param point
     *        point to check
     * @return offset of the point
     */
    @Override
    public double getOffset(final Vector<Euclidean3D> point) {
        return point.dotProduct(this.w) + this.originOffset;
    }

    /**
     * Check if the instance has the same orientation as another hyperplane.
     * 
     * @param other
     *        other hyperplane to check against the instance
     * @return true if the instance and the other hyperplane have
     *         the same orientation
     */
    @Override
    public boolean sameOrientationAs(final Hyperplane<Euclidean3D> other) {
        return (((Plane) other).w).dotProduct(this.w) > 0.0;
    }

    /**
     * Computes the distance between this plane and a point of space.
     * 
     * @param point
     *        any point of space
     * @return the distance between this and the given point
     */
    public double distanceTo(final Vector3D point) {
        // distance between the projected point and the input point
        return MathLib.abs(this.getOffset(point));
    }

    /** {@inheritDoc} */
    @Override
    public boolean intersects(final Line line) {
        // test of the angle between w and the line
        final Vector3D direction = line.getDirection();
        final double dot = Vector3D.dotProduct(this.w, direction);

        return !(MathLib.abs(dot) < THRESHOLD);
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D[] getIntersectionPoints(final Line line) {
        final Vector3D[] intersection;

        // There is a solution only if the line isn't parallel to
        // the plane.
        if (this.intersects(line)) {
            intersection = new Vector3D[1];
            intersection[0] = this.intersection(line);

        } else {
            intersection = new Vector3D[0];
        }
        return intersection;
    }

    /**
     * Computes the distance between this plane and a line.
     * 
     * @param line
     *        a line of space
     * @return the distance between this plane and the line : zero if the line
     *         intersects this plane
     */
    @Override
    public double distanceTo(final Line line) {
        final Vector3D orig = line.getOrigin();
        double distance = 0.0;

        // The distance is not zero only if the line doesn't intersect the plane
        if (!this.intersects(line)) {
            distance = this.distanceTo(orig);
        }

        return distance;
    }

    /** {@inheritDoc} */
    @Override
    public final Vector3D[] closestPointTo(final Line line) {

        // initialisation
        final Vector3D[] points = new Vector3D[2];

        // search of intersections with the line
        final Vector3D[] intersections = this.getIntersectionPoints(line);

        // if no intersection is found, the closest point of the line
        // the its origin, and the point of the plane its projection
        // if the origin abscissa is too low, then return the min abscissa point
        if (intersections.length == 0) {
            points[0] = line.getAbscissa(line.getOrigin()) < line.getMinAbscissa() ? line
                .pointAt(line.getMinAbscissa()) : line.getOrigin();
            points[1] = this.toSpace(this.toSubSpace(points[0]));
        } else {
            // if an intersection is found, the two points are identical
            points[0] = intersections[0];
            points[1] = intersections[0];
        }

        return points;
    }
    // CHECKSTYLE: resume IllegalType check
}
