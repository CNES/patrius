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
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.geometry.euclidean.twod;

import java.awt.geom.AffineTransform;

import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.geometry.Vector;
import fr.cnes.sirius.patrius.math.geometry.euclidean.oned.Euclidean1D;
import fr.cnes.sirius.patrius.math.geometry.euclidean.oned.IntervalsSet;
import fr.cnes.sirius.patrius.math.geometry.euclidean.oned.OrientedPoint;
import fr.cnes.sirius.patrius.math.geometry.euclidean.oned.Vector1D;
import fr.cnes.sirius.patrius.math.geometry.partitioning.Embedding;
import fr.cnes.sirius.patrius.math.geometry.partitioning.Hyperplane;
import fr.cnes.sirius.patrius.math.geometry.partitioning.SubHyperplane;
import fr.cnes.sirius.patrius.math.geometry.partitioning.Transform;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

//CHECKSTYLE: stop IllegalType check
//Reason: Commons-Math code kept as such

/**
 * This class represents an oriented line in the 2D plane.
 * 
 * <p>
 * An oriented line can be defined either by prolongating a line segment between two points past these points, or by one
 * point and an angular direction (in trigonometric orientation).
 * </p>
 * 
 * <p>
 * Since it is oriented the two half planes at its two sides are unambiguously identified as a left half plane and a
 * right half plane. This can be used to identify the interior and the exterior in a simple way by local properties only
 * when part of a line is used to define part of a polygon boundary.
 * </p>
 * 
 * <p>
 * A line can also be used to completely define a reference frame in the plane. It is sufficient to select one specific
 * point in the line (the orthogonal projection of the original reference frame on the line) and to use the unit vector
 * in the line direction and the orthogonal vector oriented from left half plane to right half plane. We define two
 * coordinates by the process, the <em>abscissa</em> along the line, and the <em>offset</em> across the line. All points
 * of the plane are uniquely identified by these two coordinates. The line is the set of points at zero offset, the left
 * half plane is the set of points with negative offsets and the right half plane is the set of points with positive
 * offsets.
 * </p>
 * 
 * @version $Id: Line.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.0
 */
@SuppressWarnings({"PMD.ShortClassName", "PMD.ConstructorCallsOverridableMethod"})
public class Line implements Hyperplane<Euclidean2D>, Embedding<Euclidean2D, Euclidean1D> {

    /** Threshold 1E-10. */
    private static final double THRESHOLD10 = 1.0e-10;

    /** Threshold 1E-20. */
    private static final double THRESHOLD20 = 1.0e-20;

    /** Angle with respect to the abscissa axis. */
    private double angle;

    /** Cosine of the line angle. */
    private double cos;

    /** Sine of the line angle. */
    private double sin;

    /** Offset of the frame origin. */
    private double originOffset;

    /**
     * Build a line from two points.
     * <p>
     * The line is oriented from p1 to p2
     * </p>
     * 
     * @param p1
     *        first point
     * @param p2
     *        second point
     */
    public Line(final Vector2D p1, final Vector2D p2) {
        this.reset(p1, p2);
    }

    /**
     * Build a line from a point and an angle.
     * 
     * @param p
     *        point belonging to the line
     * @param angleIn
     *        angle of the line with respect to abscissa axis
     */
    public Line(final Vector2D p, final double angleIn) {
        this.reset(p, angleIn);
    }

    /**
     * Build a line from its internal characteristics.
     * 
     * @param angleIn
     *        angle of the line with respect to abscissa axis
     * @param cosIn
     *        cosine of the angle
     * @param sinIn
     *        sine of the angle
     * @param originOffsetIn
     *        offset of the origin
     */
    private Line(final double angleIn, final double cosIn, final double sinIn, final double originOffsetIn) {
        this.angle = angleIn;
        this.cos = cosIn;
        this.sin = sinIn;
        this.originOffset = originOffsetIn;
    }

    /**
     * Copy constructor.
     * <p>
     * The created instance is completely independent from the original instance, it is a deep copy.
     * </p>
     * 
     * @param line
     *        line to copy
     */
    public Line(final Line line) {
        this.angle = MathUtils.normalizeAngle(line.angle, FastMath.PI);
        final double[] sincos = MathLib.sinAndCos(this.angle);
        this.sin = sincos[0];
        this.cos = sincos[1];
        this.originOffset = line.originOffset;
    }

    /** {@inheritDoc} */
    @Override
    public Line copySelf() {
        return new Line(this);
    }

    /**
     * Reset the instance as if built from two points.
     * <p>
     * The line is oriented from p1 to p2
     * </p>
     * 
     * @param p1
     *        first point
     * @param p2
     *        second point
     */
    public void reset(final Vector2D p1, final Vector2D p2) {
        final double dx = p2.getX() - p1.getX();
        final double dy = p2.getY() - p1.getY();
        final double d = MathLib.hypot(dx, dy);
        if (d == 0.0) {
            // Null distance
            // Convention is used
            this.angle = 0.0;
            this.cos = 1.0;
            this.sin = 0.0;
            this.originOffset = p1.getY();
        } else {
            // General case
            this.angle = FastMath.PI + MathLib.atan2(-dy, -dx);
            final double[] sincos = MathLib.sinAndCos(this.angle);
            this.sin = sincos[0];
            this.cos = sincos[1];
            this.originOffset = (p2.getX() * p1.getY() - p1.getX() * p2.getY()) / d;
        }
    }

    /**
     * Reset the instance as if built from a line and an angle.
     * 
     * @param p
     *        point belonging to the line
     * @param alpha
     *        angle of the line with respect to abscissa axis
     */
    public void reset(final Vector2D p, final double alpha) {
        this.angle = MathUtils.normalizeAngle(alpha, FastMath.PI);
        final double[] sincos = MathLib.sinAndCos(this.angle);
        this.sin = sincos[0];
        this.cos = sincos[1];
        this.originOffset = this.cos * p.getY() - this.sin * p.getX();
    }

    /**
     * Revert the instance.
     */
    public void revertSelf() {
        if (this.angle < FastMath.PI) {
            this.angle += FastMath.PI;
        } else {
            this.angle -= FastMath.PI;
        }
        this.cos = -this.cos;
        this.sin = -this.sin;
        this.originOffset = -this.originOffset;
    }

    /**
     * Get the reverse of the instance.
     * <p>
     * Get a line with reversed orientation with respect to the instance. A new object is built, the instance is
     * untouched.
     * </p>
     * 
     * @return a new line, with orientation opposite to the instance orientation
     */
    public Line getReverse() {
        return new Line((this.angle < FastMath.PI) ? (this.angle + FastMath.PI) : (this.angle - FastMath.PI),
            -this.cos, -this.sin, -this.originOffset);
    }

    /** {@inheritDoc} */
    @Override
    public Vector1D toSubSpace(final Vector<Euclidean2D> point) {
        final Vector2D p2 = (Vector2D) point;
        return new Vector1D(this.cos * p2.getX() + this.sin * p2.getY());
    }

    /** {@inheritDoc} */
    @Override
    public Vector2D toSpace(final Vector<Euclidean1D> point) {
        final double abscissa = ((Vector1D) point).getX();
        return new Vector2D(abscissa * this.cos - this.originOffset * this.sin,
            abscissa * this.sin + this.originOffset * this.cos);
    }

    /**
     * Get the intersection point of the instance and another line.
     * 
     * @param other
     *        other line
     * @return intersection point of the instance and the other line
     *         or null if there are no intersection points
     */
    public Vector2D intersection(final Line other) {
        final double d = this.sin * other.cos - other.sin * this.cos;
        if (MathLib.abs(d) < THRESHOLD10) {
            return null;
        }
        return new Vector2D((this.cos * other.originOffset - other.cos * this.originOffset) / d,
            (this.sin * other.originOffset - other.sin * this.originOffset) / d);
    }

    /** {@inheritDoc} */
    @Override
    public SubLine wholeHyperplane() {
        return new SubLine(this, new IntervalsSet());
    }

    /**
     * Build a region covering the whole space.
     * 
     * @return a region containing the instance (really a {@link PolygonsSet PolygonsSet} instance)
     */
    @Override
    public PolygonsSet wholeSpace() {
        return new PolygonsSet();
    }

    /**
     * Get the offset (oriented distance) of a parallel line.
     * <p>
     * This method should be called only for parallel lines otherwise the result is not meaningful.
     * </p>
     * <p>
     * The offset is 0 if both lines are the same, it is positive if the line is on the right side of the instance and
     * negative if it is on the left side, according to its natural orientation.
     * </p>
     * 
     * @param line
     *        line to check
     * @return offset of the line
     */
    public double getOffset(final Line line) {
        return this.originOffset +
            ((this.cos * line.cos + this.sin * line.sin > 0) ? -line.originOffset : line.originOffset);
    }

    /** {@inheritDoc} */
    @Override
    public double getOffset(final Vector<Euclidean2D> point) {
        final Vector2D p2 = (Vector2D) point;
        return this.sin * p2.getX() - this.cos * p2.getY() + this.originOffset;
    }

    /** {@inheritDoc} */
    @Override
    public boolean sameOrientationAs(final Hyperplane<Euclidean2D> other) {
        final Line otherL = (Line) other;
        return (this.sin * otherL.sin + this.cos * otherL.cos) >= 0.0;
    }

    /**
     * Get one point from the plane.
     * 
     * @param abscissa
     *        desired abscissa for the point
     * @param offset
     *        desired offset for the point
     * @return one point in the plane, with given abscissa and offset
     *         relative to the line
     */
    public Vector2D getPointAt(final Vector1D abscissa, final double offset) {
        final double x = abscissa.getX();
        final double dOffset = offset - this.originOffset;
        return new Vector2D(x * this.cos + dOffset * this.sin, x * this.sin - dOffset * this.cos);
    }

    /**
     * Check if the line contains a point.
     * 
     * @param p
     *        point to check
     * @return true if p belongs to the line
     */
    public boolean contains(final Vector2D p) {
        return MathLib.abs(this.getOffset(p)) < THRESHOLD10;
    }

    /**
     * Compute the distance between the instance and a point.
     * <p>
     * This is a shortcut for invoking FastMath.abs(getOffset(p)), and provides consistency with what is in the
     * fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line class.
     * </p>
     * 
     * @param p
     *        to check
     * @return distance between the instance and the point
     * @since 3.1
     */
    public double distance(final Vector2D p) {
        return MathLib.abs(this.getOffset(p));
    }

    /**
     * Check the instance is parallel to another line.
     * 
     * @param line
     *        other line to check
     * @return true if the instance is parallel to the other line
     *         (they can have either the same or opposite orientations)
     */
    public boolean isParallelTo(final Line line) {
        return MathLib.abs(this.sin * line.cos - this.cos * line.sin) < THRESHOLD10;
    }

    /**
     * Translate the line to force it passing by a point.
     * 
     * @param p
     *        point by which the line should pass
     */
    public void translateToPoint(final Vector2D p) {
        this.originOffset = this.cos * p.getY() - this.sin * p.getX();
    }

    /**
     * Get the angle of the line.
     * 
     * @return the angle of the line with respect to the abscissa axis
     */
    public double getAngle() {
        return MathUtils.normalizeAngle(this.angle, FastMath.PI);
    }

    /**
     * Set the angle of the line.
     * 
     * @param angleIn
     *        new angle of the line with respect to the abscissa axis
     */
    public void setAngle(final double angleIn) {
        this.angle = MathUtils.normalizeAngle(angleIn, FastMath.PI);
        final double[] sincos = MathLib.sinAndCos(this.angle);
        this.sin = sincos[0];
        this.cos = sincos[1];
    }

    /**
     * Get the offset of the origin.
     * 
     * @return the offset of the origin
     */
    public double getOriginOffset() {
        return this.originOffset;
    }

    /**
     * Set the offset of the origin.
     * 
     * @param offset
     *        offset of the origin
     */
    public void setOriginOffset(final double offset) {
        this.originOffset = offset;
    }

    /**
     * Get a {@link fr.cnes.sirius.patrius.math.geometry.partitioning.Transform
     * Transform} embedding an affine transform.
     * 
     * @param transform
     *        affine transform to embed (must be inversible
     *        otherwise the {@link fr.cnes.sirius.patrius.math.geometry.partitioning.Transform#apply(Hyperplane)
     *        apply(Hyperplane)} method would work only for some lines, and
     *        fail for other ones)
     * @return a new transform that can be applied to either {@link Vector2D Vector2D}, {@link Line Line} or
     *         {@link fr.cnes.sirius.patrius.math.geometry.partitioning.SubHyperplane
     *         SubHyperplane} instances
     * @exception MathIllegalArgumentException
     *            if the transform is non invertible
     */
    public static Transform<Euclidean2D, Euclidean1D> getTransform(final AffineTransform transform) {
        return new LineTransform(transform);
    }

    /**
     * Class embedding an affine transform.
     * <p>
     * This class is used in order to apply an affine transform to a line. Using a specific object allow to perform some
     * computations on the transform only once even if the same transform is to be applied to a large number of lines
     * (for example to a large polygon)./
     * <p>
     */
    private static class LineTransform implements Transform<Euclidean2D, Euclidean1D> {

        /** Intermediate variable. */
        private final double cXX;
        /** Intermediate variable. */
        private final double cXY;
        /** Intermediate variable. */
        private final double cX1;
        /** Intermediate variable. */
        private final double cYX;
        /** Intermediate variable. */
        private final double cYY;
        /** Intermediate variable. */
        private final double cY1;

        /** Intermediate variable. */
        private final double c1Y;
        /** Intermediate variable. */
        private final double c1X;
        /** Intermediate variable. */
        private final double c11;

        /**
         * Build an affine line transform from a n {@code AffineTransform}.
         * 
         * @param transform
         *        transform to use (must be invertible otherwise
         *        the {@link LineTransform#apply(Hyperplane)} method would work
         *        only for some lines, and fail for other ones)
         * @exception MathIllegalArgumentException
         *            if the transform is non invertible
         */
        public LineTransform(final AffineTransform transform) {

            final double[] m = new double[6];
            transform.getMatrix(m);
            this.cXX = m[0];
            this.cXY = m[2];
            this.cX1 = m[4];
            this.cYX = m[1];
            this.cYY = m[3];
            this.cY1 = m[5];

            this.c1Y = this.cXY * this.cY1 - this.cYY * this.cX1;
            this.c1X = this.cXX * this.cY1 - this.cYX * this.cX1;
            this.c11 = this.cXX * this.cYY - this.cYX * this.cXY;

            if (MathLib.abs(this.c11) < THRESHOLD20) {
                throw new MathIllegalArgumentException(PatriusMessages.NON_INVERTIBLE_TRANSFORM);
            }

        }

        /** {@inheritDoc} */
        @Override
        public Vector2D apply(final Vector<Euclidean2D> point) {
            final Vector2D p2D = (Vector2D) point;
            final double x = p2D.getX();
            final double y = p2D.getY();
            return new Vector2D(this.cXX * x + this.cXY * y + this.cX1,
                this.cYX * x + this.cYY * y + this.cY1);
        }

        /** {@inheritDoc} */
        @Override
        public Line apply(final Hyperplane<Euclidean2D> hyperplane) {
            final Line line = (Line) hyperplane;
            final double rOffset = this.c1X * line.cos + this.c1Y * line.sin + this.c11 * line.originOffset;
            final double rCos = this.cXX * line.cos + this.cXY * line.sin;
            final double rSin = this.cYX * line.cos + this.cYY * line.sin;
            final double inv = 1.0 / MathLib.sqrt(rSin * rSin + rCos * rCos);
            return new Line(FastMath.PI + MathLib.atan2(-rSin, -rCos),
                inv * rCos, inv * rSin,
                inv * rOffset);
        }

        /** {@inheritDoc} */
        @Override
        public SubHyperplane<Euclidean1D> apply(final SubHyperplane<Euclidean1D> sub,
                                                final Hyperplane<Euclidean2D> original,
                                                final Hyperplane<Euclidean2D> transformed) {
            final OrientedPoint op = (OrientedPoint) sub.getHyperplane();
            final Line originalLine = (Line) original;
            final Line transformedLine = (Line) transformed;
            final Vector1D newLoc =
                transformedLine.toSubSpace(this.apply(originalLine.toSpace(op.getLocation())));
            return new OrientedPoint(newLoc, op.isDirect()).wholeHyperplane();
        }

    }

    // CHECKSTYLE: resume IllegalType check
}
