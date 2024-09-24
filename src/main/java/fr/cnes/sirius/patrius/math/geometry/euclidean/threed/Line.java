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
 * VERSION:4.10.1:FA:FA-3265:02/12/2022:[PATRIUS] Calcul KO des points plus proches entre une Line et un FacetBodyShape
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3163:10/05/2022:[PATRIUS] Enrichissement des reperes planetaires 
 * VERSION:4.9:DM:DM-3135:10/05/2022:[PATRIUS] Calcul d'intersection sur BodyShape  
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.geometry.euclidean.threed;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.geometry.Vector;
import fr.cnes.sirius.patrius.math.geometry.euclidean.oned.Euclidean1D;
import fr.cnes.sirius.patrius.math.geometry.euclidean.oned.IntervalsSet;
import fr.cnes.sirius.patrius.math.geometry.euclidean.oned.Vector1D;
import fr.cnes.sirius.patrius.math.geometry.partitioning.Embedding;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

//CHECKSTYLE: stop IllegalType check
//Reason: Commons-Math code kept as such

/**
 * The class represent lines in a three dimensional space.
 * 
 * <p>
 * Each oriented line is intrinsically associated with an abscissa which is a coordinate on the line. The point at
 * abscissa 0 is the orthogonal projection of the origin on the line, another equivalent way to express this is to say
 * that it is the point of the line which is closest to the origin. Abscissa increases in the line direction.
 * </p>
 * <p>
 * This PATRIUS 4.9, the user can define a "min abscissa" in order to define a sub-line: only points after this abscissa
 * are considered to be part of the sub-line.
 * </p>
 * 
 * @version $Id: Line.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.0
 */
@SuppressWarnings({ "PMD.ShortClassName", "PMD.NullAssignment", "PMD.ConstructorCallsOverridableMethod" })
public class Line implements Embedding<Euclidean3D, Euclidean1D>, Serializable {

     /** Serializable UID. */
    private static final long serialVersionUID = 8033077703790111400L;

    /** Threshold 1E-10. */
    private static final double THRESHOLD10 = 1.0e-10;

    /** Line direction. */
    private Vector3D direction;

    /** Line point closest to the origin. */
    private Vector3D zero;

    /** Minimum abscissa. */
    private double minAbscissa;

    /**
     * Build a line from three points, one of which is the point of minimum abscissa.
     * 
     * @param p1
     *        first point belonging to the line (this can be any point)
     * @param p2
     *        second point belonging to the line (this can be any point, different from p1)
     * @param pMinAbscissa
     *        point of minimum abscissa (only points after this abscissa are considered to be part of the sub-line)
     * @exception MathIllegalArgumentException
     *            if the points are equal
     */
    public Line(final Vector3D p1, final Vector3D p2, final Vector3D pMinAbscissa) {
        this.reset(p1, p2);
        // Compute the minimum abscissa
        this.minAbscissa = this.getAbscissa(pMinAbscissa);
    }

    /**
     * Build a line from two points.
     * 
     * @param p1
     *        first point belonging to the line (this can be any point)
     * @param p2
     *        second point belonging to the line (this can be any point, different from p1)
     * @exception MathIllegalArgumentException
     *            if the points are equal
     */
    public Line(final Vector3D p1, final Vector3D p2) {
        this.reset(p1, p2);
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
        this.direction = line.direction;
        this.zero = line.zero;
        this.minAbscissa = line.minAbscissa;
    }

    /**
     * Reset the instance as if built from two points.
     * 
     * @param p1
     *        first point belonging to the line (this can be any point)
     * @param p2
     *        second point belonging to the line (this can be any point, different from p1)
     * @exception MathIllegalArgumentException
     *            if the points are equal
     */
    public void reset(final Vector3D p1, final Vector3D p2) {
        final Vector3D delta = p2.subtract(p1);
        final double norm2 = delta.getNormSq();
        if (norm2 == 0.0) {
            throw new MathIllegalArgumentException(PatriusMessages.ZERO_NORM);
        }
        this.direction = new Vector3D(1.0 / MathLib.sqrt(norm2), delta);
        this.zero = new Vector3D(1.0, p1, -p1.dotProduct(delta) / norm2, delta);
        // Reset the minimum abscissa to negative infinity
        this.minAbscissa = Double.NEGATIVE_INFINITY;
    }

    /**
     * Get a line with reversed direction.<br>
     * In the case of a semi-finite line, the min abscissa point is "frozen" so that
     * the min abscissa point of the new line is the same. As a consequence, the minimum abscissa value is equal to the
     * opposite of this.minAbscissa.<br>
     * Reverted infinite lines still have minAbscissa = Double.NEGATIVE_INFINITY
     * 
     * @return a new instance, with reversed direction
     */
    public Line revert() {
        if (this.minAbscissa == Double.NEGATIVE_INFINITY) {
            // Return infinite line with opposite direction
            return new Line(this.zero, this.zero.subtract(this.direction));
        } else {
            // Return semi-finite line with opposite direction and same minimum abscissa point
            return new Line(this.zero, this.zero.subtract(this.direction), this.pointAt(this.minAbscissa));
        }

    }

    /**
     * Get the normalized direction vector.
     * 
     * @return normalized direction vector
     */
    public Vector3D getDirection() {
        return this.direction;
    }

    /**
     * Get the line point closest to the origin.
     * 
     * @return line point closest to the origin
     */
    public Vector3D getOrigin() {
        return this.zero;
    }

    /**
     * Get the line minimum abscissa.
     * 
     * @return line minimum abscissa
     */
    public double getMinAbscissa() {
        return this.minAbscissa;
    }

    /**
     * Get the abscissa of a point with respect to the line.
     * <p>
     * The abscissa is 0 if the projection of the point and the projection of the frame origin on the line are the same
     * point.
     * </p>
     * 
     * @param point
     *        point to check
     * @return abscissa of the point
     */
    public double getAbscissa(final Vector3D point) {
        double abscissa = point.subtract(this.zero).dotProduct(this.direction);
        // Check if abscissa is NaN
        if (Double.isNaN(abscissa)) {
            abscissa = Double.NEGATIVE_INFINITY;
        }

        return abscissa;
    }

    /**
     * Get one point from the line.
     * 
     * @param abscissa
     *        desired abscissa for the point
     * @return one point belonging to the line, at specified abscissa
     */
    public Vector3D pointAt(final double abscissa) {
        return new Vector3D(1.0, this.zero, abscissa, this.direction);
    }

    /**
     * {@inheritDoc}
     * 
     * @see #getAbscissa(Vector3D)
     */
    @Override
    public Vector1D toSubSpace(final Vector<Euclidean3D> point) {
        return new Vector1D(this.getAbscissa((Vector3D) point));
    }

    /**
     * {@inheritDoc}
     * 
     * @see #pointAt(double)
     */
    @Override
    public Vector3D toSpace(final Vector<Euclidean1D> point) {
        return this.pointAt(((Vector1D) point).getX());
    }

    /**
     * Check if the instance is similar to another line.
     * <p>
     * Lines are considered similar if they contain the same points regardless abscissas and if they are both infinite
     * or semi-finite. This does not mean they are equal since they can also have opposite directions.
     * </p>
     * 
     * @param line
     *        line to which instance should be compared
     * @return true if the lines are similar
     */
    public boolean isSimilarTo(final Line line) {
        // Angle between directions
        final double angle = Vector3D.angle(this.direction, line.direction);

        // Line status: finite or semi-finite
        final boolean infinite1 = this.minAbscissa == Double.NEGATIVE_INFINITY;
        final boolean infinite2 = line.minAbscissa == Double.NEGATIVE_INFINITY;
        final boolean sameFiniteStatus = infinite1 == infinite2;

        if (sameFiniteStatus) {

            // Both lines are infinite or semi-finite
            if (infinite1) {
                // Two infinite lines
                // True if same direction or opposite and if the line contains the other line's zero (no offset)
                return ((angle < THRESHOLD10) || (angle > (FastMath.PI - THRESHOLD10))) && this.contains(line.zero);
            } else {
                // True if strictly same direction and if lines' min abscissa point is the same (no offset)
                final double deltaMinAbscissas = this.pointAt(this.minAbscissa)
                    .distance(line.pointAt(line.minAbscissa));
                return (angle < THRESHOLD10 && deltaMinAbscissas < THRESHOLD10);
            }

        } else {
            // An infinite line is not similar to a semi-finite line
            return false;
        }
    }

    /**
     * Check if the instance contains a point. Calculations take the minimum abscissa into account.
     * 
     * @param p
     *        point to check
     * @return true if p belongs to the line and its abscissa is greater than the minimum abscissa
     */
    public boolean contains(final Vector3D p) {
        return this.distance(p) < THRESHOLD10;
    }

    /**
     * Compute the distance between the instance and a point. Calculations take the minimum abscissa into account.
     * 
     * @param p
     *        to check
     * @return distance between the instance and the point
     */
    public double distance(final Vector3D p) {
        // Check point's abscissa
        if (this.getAbscissa(p) >= this.minAbscissa) {
            // Classical calculation: orthogonal distance
            final Vector3D d = p.subtract(this.zero);
            final Vector3D n = new Vector3D(1.0, d, -d.dotProduct(this.direction), this.direction);
            return n.getNorm();
        } else {
            // Distance from the point of minAbscissa (minAbscissa cannot be Double.NEGATIVE_INFINITY in this case)
            return Vector3D.distance(p, this.pointAt(this.minAbscissa));
        }
    }

    /**
     * Compute the shortest distance between the instance and another line. Calculations take the minimum abscissa into
     * account.
     * 
     * @param line
     *        line to check against the instance
     * @return shortest distance between the instance and the line
     */
    public double distance(final Line line) {

        if (this.minAbscissa == Double.NEGATIVE_INFINITY && line.minAbscissa == Double.NEGATIVE_INFINITY) {
            // Classic case with two infinite lines

            final Vector3D normal = Vector3D.crossProduct(this.direction, line.direction);
            final double n = normal.getNorm();
            if (n < Precision.SAFE_MIN) {
                // lines are parallel
                return this.distance(line.zero);
            }

            // Signed separation of the two parallel planes that contains the lines
            final double offset = line.zero.subtract(this.zero).dotProduct(normal) / n;

            return MathLib.abs(offset);

        } else {
            // Case with at least one semi-finite line: need to compute points so as to know if their abscissa is
            // allowed
            final Vector3D[] closestPoints = this.closestPointTo(line);

            return closestPoints[0].distance(closestPoints[1]);
        }

    }

    /**
     * Compute the point of the instance closest to another line. Calculations take the minimum abscissa into account.
     * 
     * @param line
     *        line to check against the instance
     * @return point of the instance closest to another line
     */
    public Vector3D closestPoint(final Line line) {

        if (line.getMinAbscissa() == Double.NEGATIVE_INFINITY) {
            // If the other line is infinite (this may be infinite or semi-finite)

            final double cos = this.direction.dotProduct(line.direction);
            final double n = 1 - cos * cos;
            if (n < Precision.DOUBLE_COMPARISON_EPSILON) {
                // the lines are parallel
                if (this.getAbscissa(this.zero) < this.minAbscissa) {
                    // Zero does not belong to the line: return min abscissa point
                    return this.pointAt(this.minAbscissa);
                } else {
                    // Check that the zero is contained by the line taking the min abscissa into account
                    return this.zero;
                }
            }

            final Vector3D delta0 = line.zero.subtract(this.zero);
            final double a = delta0.dotProduct(this.direction);
            final double b = delta0.dotProduct(line.direction);

            // Candidate result: need to check that this point belongs to the line
            final Vector3D candidatePoint = new Vector3D(1, this.zero, (a - b * cos) / n, this.direction);

            if (this.getAbscissa(candidatePoint) >= this.minAbscissa) {
                // The candidate point belongs to the line
                return candidatePoint;
            } else {
                // The candidate point does not belong to the line: return the min abscissa point
                return this.pointAt(this.minAbscissa);
            }

        } else {
            // If the other line is semi-finite, need to compute closest points
            // The code of the other clause above is efficient but can't handle input line's min abscissa
            return closestPointTo(line)[1];
        }

    }

    /**
     * Get the intersection point of the instance and another line. Calculations take the minimum abscissa into account.
     * 
     * @param line
     *        other line
     * @return intersection point of the instance and the other line
     *         or null if there are no intersection points
     */
    public Vector3D intersection(final Line line) {
        // Initialize to null the result to be returned
        Vector3D res = null;
        // Retrieve the point of this line which is the closest to the line given in input
        final Vector3D closest = this.closestPoint(line);
        // Check if the line given in input contains the closest point
        if (line.contains(closest)) {
            res = closest;
        }

        // Return the result
        return res;
    }

    /**
     * Build a sub-line covering the whole line.
     * 
     * @return a sub-line covering the whole line
     */
    public SubLine wholeLine() {
        return new SubLine(this, new IntervalsSet());
    }

    /**
     * Compute the intersection points with another line if it exists. Calculations take the minimum abscissa into
     * account.
     * 
     * @param line
     *        the second line
     * @return a Vector3D array containing the intersection point if it exists,
     *         empty otherwise
     */
    public Vector3D[] getIntersectionPoints(final Line line) {
        final double vectorsComparisonEps = 1.0e-10;

        Vector3D[] intersection = new Vector3D[0];

        Vector3D normal = Vector3D.crossProduct(this.direction, line.direction);

        // lines are parallel
        if (normal.getNorm() < vectorsComparisonEps) {

            final Vector3D directionCrossOToO = Vector3D.crossProduct(line.zero.subtract(this.zero), this.direction);

            // If the lines are identical then the list is filed either with the zero point or a minimum abscissa
            // point
            if (directionCrossOToO.getNorm() < vectorsComparisonEps) {
                final boolean thisInfinite = (this.minAbscissa == Double.NEGATIVE_INFINITY);
                final boolean lineInfinite = (line.minAbscissa == Double.NEGATIVE_INFINITY);
                intersection = new Vector3D[1];
                if (thisInfinite && lineInfinite) {
                    intersection[0] = this.zero;
                } else if (thisInfinite) {
                    intersection[0] = line.pointAt(line.minAbscissa);
                } else if (lineInfinite) {
                    intersection[0] = this.pointAt(this.minAbscissa);
                } else {
                    final Vector3D[] points = this.closestPointTo(line);
                    intersection[0] = points[0];

                    // Last check: two semi-finite lines may not intersect even if they belong to the same infinite line
                    if (points[0].distance(points[1]) > vectorsComparisonEps) {
                        return new Vector3D[0];
                    }
                }
            }

        } else {
            // Vector zeros of the lines
            final Vector3D origLineToOrigSegment = line.zero.subtract(this.zero);

            // projection of this vector on the normed normal vector
            normal = normal.normalize();
            final double projectionOnNormal = Vector3D.dotProduct(origLineToOrigSegment, normal);

            // if the projection on normal is null, the lines intersect
            if (MathLib.abs(projectionOnNormal) < vectorsComparisonEps) {

                // Check that lines' respective closest point is the same: if so, they intersect on allowed abscissas
                final Vector3D[] closestPoints = this.closestPointTo(line);

                if (closestPoints[0].distance(closestPoints[1]) < vectorsComparisonEps) {
                    // Intersection point computation
                    intersection = new Vector3D[1];
                    intersection[0] = closestPoints[0];
                }
            }
        }

        return intersection;
    }

    /**
     * Computes the points of this and another line realizing the shortest distance.
     * If lines are parallel, the zero point of the other line is used.
     * Calculations take the minimum abscissa into account.
     * 
     * @param line
     *        the line
     * @return the closest point of the other line, and the closest point of this.
     */
    public final Vector3D[] closestPointTo(final Line line) {
        final double vectorsComparisonEps = 1.0e-10;

        // Initialisations
        final Vector3D[] points = new Vector3D[2];
        final Vector3D originLine = line.getOrigin();
        final Vector3D directionLine = line.getDirection().normalize();

        // computation of the normal to both directions
        Vector3D normal = Vector3D.crossProduct(directionLine, this.direction);

        // lines are parallel
        if (normal.getNorm() < vectorsComparisonEps) {
            // the first point is the origin of the given line if the point belongs to it, the line's min abscissa point
            // otherwise
            points[0] = line.getAbscissa(originLine) < line.minAbscissa ? line.pointAt(line.getMinAbscissa())
                : originLine;
            // the second point is point's projection, or the min abscissa point of this
            final Vector3D candidatePoint = this.toSpace(this.toSubSpace(points[0]));
            points[1] = this.getAbscissa(candidatePoint) < this.minAbscissa ? this.pointAt(this.minAbscissa)
                : candidatePoint;
        } else {
            // Vector from the origin of this to the origin of the input line
            final Vector3D origLineToOrigSegment = originLine.subtract(this.zero);

            // projection of this vector on the normed normal vector
            normal = normal.normalize();
            final double projectionOnNormal = Vector3D.dotProduct(origLineToOrigSegment, normal);

            // creation of the shortest vector between the lines
            normal = normal.scalarMultiply(projectionOnNormal);

            // computation of the points origin and zero + direction
            // translated into the plane parallel to the separating middle plane
            // and containing the input line.
            final Vector3D translatedOrig = this.zero.add(normal);
            final Vector3D translatedOrigPlusDir = this.zero.add(this.direction).add(normal);

            // Computation of the distances of these points to the infinite version of the input line
            // If the input line is semi-finite, an infinite version is computed for calculations
            // Otherwise distance would take minAbscissa point into account and falsen the result
            Line lineComput = line;
            if (line.getMinAbscissa() != Double.NEGATIVE_INFINITY) {
                lineComput = createLine(line.zero, line.direction);
            }
            final double distOrig = lineComput.distance(translatedOrig);
            final double distOrigPLusDir = lineComput.distance(translatedOrigPlusDir);

            // Thales theorem : computation of the point of the segment
            // witch is the closest to the input line
            // The following value can't be null because the lines are'nt parallel
            final double alpha = distOrig / (distOrig - distOrigPLusDir);

            // computation of the two points
            points[1] = this.zero.add(alpha, this.direction);
            points[0] = points[1].add(normal);

            // Correct points if min abscissas are not respected
            if (line.getAbscissa(points[0]) < line.minAbscissa) {
                points[0] = line.pointAt(line.minAbscissa);
                final Vector3D candidatePoint = this.toSpace(this.toSubSpace(points[0]));
                points[1] = this.getAbscissa(candidatePoint) < this.minAbscissa ? this.pointAt(this.getMinAbscissa())
                    : candidatePoint;
            } else if (this.getAbscissa(points[1]) < this.minAbscissa) {
                points[1] = this.pointAt(this.minAbscissa);
                final Vector3D candidatePoint = line.toSpace(line.toSubSpace(points[1]));
                points[0] = line.getAbscissa(candidatePoint) < line.minAbscissa ? line.pointAt(line.getMinAbscissa())
                    : candidatePoint;
            }
        }
        return points;
    }

    /**
     * Get the point with the lowest abscissa from an array of points. Points are not filtered by the line's
     * minAbscissa, ie. this method may return a point with an abscissa lower than this.minAbscissa.<br>
     * In case several points have the same lowest abscissa, only the first one of the array is returned.<br>
     * <b><em>Warning: array must not be empty</em></b>
     * 
     * @param points
     *        array of points to assess, must not be empty
     * 
     * @return the point of the array which has the lowest abscissa
     */
    public final Vector3D pointOfMinAbscissa(final Vector3D[] points) {

        // Browse array and retrieve the point with the lowest abscissa
        Vector3D res = null;
        double minAbs = Double.POSITIVE_INFINITY;
        for (final Vector3D minAbsPoint : points) {
            final double abscissa = this.getAbscissa(minAbsPoint);
            if (abscissa < minAbs) {
                minAbs = abscissa;
                res = minAbsPoint;
            }
        }

        return res;
    }

    /**
     * Creates a Line object from a point of space and a direction vector.
     * 
     * @param point
     *        the origin point
     * @param direction
     *        the directing vector of the line
     * @return the new Line
     */
    public static final Line createLine(final Vector3D point, final Vector3D direction) {
        return new Line(point, point.add(direction));
    }

    /**
     * Creates a Line object from a point of space, a direction vector and the point of minimum abscissa.
     * 
     * @param point
     *        the origin point
     * @param direction
     *        the directing vector of the line
     * @param pointMinAbscissa
     *        point of minimum abscissa (only points after this abscissa are considered to be part of the sub-line)
     * @return the new Line
     */
    public static final Line
        createLine(final Vector3D point, final Vector3D direction, final Vector3D pointMinAbscissa) {
        return new Line(point, point.add(direction), pointMinAbscissa);
    }

    // CHECKSTYLE: resume IllegalType check
}
