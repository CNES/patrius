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
 * <p>This PATRIUS 4.9, the user can define a "min abscissa" in order to define a sub-line: only points after this 
 * abscissa are considered to be part of the sub-line.</p>
 * 
 * @version $Id: Line.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.0
 */
@SuppressWarnings({"PMD.ShortClassName", "PMD.NullAssignment", "PMD.ConstructorCallsOverridableMethod"})
public class Line implements Embedding<Euclidean3D, Euclidean1D>, Serializable {

    /** Serial UID. */
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
     * Get a line with reversed direction.
     * 
     * @return a new instance, with reversed direction
     */
    public Line revert() {
        return new Line(this.zero, this.zero.subtract(this.direction), this.pointAt(this.minAbscissa));
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
        double ascissa = point.subtract(this.zero).dotProduct(this.direction);
        // Check if ascissa is NaN
        if (Double.isNaN(ascissa)) {
            ascissa = Double.NEGATIVE_INFINITY;
        }

        return ascissa;
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
     * Lines are considered similar if they contain the same points. This does not mean they are equal since they can
     * have opposite directions.
     * </p>
     * 
     * @param line
     *        line to which instance should be compared
     * @return true if the lines are similar
     */
    public boolean isSimilarTo(final Line line) {
        final double angle = Vector3D.angle(this.direction, line.direction);
        return ((angle < THRESHOLD10) || (angle > (FastMath.PI - THRESHOLD10))) && this.contains(line.zero);
    }

    /**
     * Check if the instance contains a point.
     * 
     * @param p
     *        point to check
     * @return true if p belongs to the line
     */
    public boolean contains(final Vector3D p) {
        return this.distance(p) < THRESHOLD10;
    }

    /**
     * Compute the distance between the instance and a point.
     * 
     * @param p
     *        to check
     * @return distance between the instance and the point
     */
    public double distance(final Vector3D p) {
        final Vector3D d = p.subtract(this.zero);
        final Vector3D n = new Vector3D(1.0, d, -d.dotProduct(this.direction), this.direction);
        return n.getNorm();
    }

    /**
     * Compute the shortest distance between the instance and another line.
     * 
     * @param line
     *        line to check against the instance
     * @return shortest distance between the instance and the line
     */
    public double distance(final Line line) {

        final Vector3D normal = Vector3D.crossProduct(this.direction, line.direction);
        final double n = normal.getNorm();
        if (n < Precision.SAFE_MIN) {
            // lines are parallel
            return this.distance(line.zero);
        }

        // signed separation of the two parallel planes that contains the lines
        final double offset = line.zero.subtract(this.zero).dotProduct(normal) / n;

        return MathLib.abs(offset);

    }

    /**
     * Compute the point of the instance closest to another line.
     * 
     * @param line
     *        line to check against the instance
     * @return point of the instance closest to another line
     */
    public Vector3D closestPoint(final Line line) {

        final double cos = this.direction.dotProduct(line.direction);
        final double n = 1 - cos * cos;
        if (n < Precision.DOUBLE_COMPARISON_EPSILON) {
            // the lines are parallel
            return this.zero;
        }

        final Vector3D delta0 = line.zero.subtract(this.zero);
        final double a = delta0.dotProduct(this.direction);
        final double b = delta0.dotProduct(line.direction);

        return new Vector3D(1, this.zero, (a - b * cos) / n, this.direction);

    }

    /**
     * Get the intersection point of the instance and another line.
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
        // Check if the line given in input contains the closest point and if the intersection point is on the right
        // side of the line by comparing its abscissa with the minimum abscissa
        if (line.contains(closest) && this.getAbscissa(closest) > this.minAbscissa) {
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
     * Compute the intersection points with another line if it exists.
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

            // the list is filled with the zero point if the lines are
            // identical
            if (directionCrossOToO.getNorm() < vectorsComparisonEps) {
                intersection = new Vector3D[1];
                intersection[0] = this.zero;
            }

        } else {
            // Vector zeros of the lines
            final Vector3D origLineToOrigSegment = line.zero.subtract(this.zero);

            // projection of this vector on the normed normal vector
            normal = normal.normalize();
            final double projectionOnNormal = Vector3D.dotProduct(origLineToOrigSegment, normal);

            // if the projection on normal is null, the lines intersect
            if (MathLib.abs(projectionOnNormal) < vectorsComparisonEps) {

                // intersection point computation
                intersection = new Vector3D[1];
                // check if the intersection point is on the right side of the line by comparing its abscissa with the
                // minimum abscissa
                if (this.getAbscissa(this.closestPointTo(line)[0]) > this.minAbscissa) {
                    intersection[0] = this.closestPointTo(line)[0];
                }
            }
        }

        return intersection;
    }

    /**
     * Computes the points of this and another line realizing the shortest distance.
     * If lines are parallel, the zero point of the other line is used.
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
            // the first point is the origin of the given line,
            // the second its projection
            points[0] = line.getOrigin();
            points[1] = this.toSpace(this.toSubSpace(points[0]));
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

            // Computation of the distances of these points to the input line
            final double distOrig = line.distance(translatedOrig);
            final double distOrigPLusDir = line.distance(translatedOrigPlusDir);

            // Thales theorem : computation of the point of the segment
            // witch is the closest to the input line
            // The following value can't be null because the lines are'nt parallel
            final double alpha = distOrig / (distOrig - distOrigPLusDir);

            // computation of the two points
            points[1] = this.zero.add(alpha, this.direction);
            points[0] = points[1].add(normal);
        }
        return points;
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

    // CHECKSTYLE: resume IllegalType check
}
