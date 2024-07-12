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
 * VERSION:4.10.1:FA:FA-3275:02/12/2022:[PATRIUS] Mauvais calcul d'intersection entre 2 segments 2D
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-2979:15/11/2021:[PATRIUS] Ajout d'une methode public getIntersection(Segment) dans la classe
 * Segment 2D 
 * VERSION:4.8:FA:FA-2956:15/11/2021:[PATRIUS] Temps de propagation non implemente pour certains evenements 
 * VERSION:4.7:DM:DM-2909:18/05/2021:Ajout des methodes getIntersectionPoint, getClosestPoint(Vector2D) et getAlpha()
 * VERSION:4.7:DM:DM-2653:18/05/2021:generalisation des sequences et correction/refonte des sequences de segments 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.geometry.euclidean.twod;


/**
 * Simple container for a two-points segment.
 * 
 * @version $Id: Segment.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.0
 */
public class Segment {

    /** Start point of the segment. */
    private final Vector2D start;

    /** End point of the segments. */
    private final Vector2D end;

    /** Line containing the segment. */
    private final Line line;

    /**
     * Build a segment.
     * 
     * @param startIn
     *        start point of the segment
     * @param endIn
     *        end point of the segment
     * @param lineIn
     *        line containing the segment
     */
    public Segment(final Vector2D startIn, final Vector2D endIn, final Line lineIn) {
        this.start = startIn;
        this.end = endIn;
        this.line = lineIn;
    }

    /**
     * Build a segment.
     * 
     * @param startIn
     *        start point of the segment
     * @param endIn
     *        end point of the segment
     */
    public Segment(final Vector2D startIn, final Vector2D endIn) {
        this(startIn, endIn, new Line(startIn, endIn));
    }

    /**
     * Get the start point of the segment.
     * 
     * @return start point of the segment
     */
    public Vector2D getStart() {
        return this.start;
    }

    /**
     * Get the end point of the segment.
     * 
     * @return end point of the segment
     */
    public Vector2D getEnd() {
        return this.end;
    }

    /**
     * Get the line containing the segment.
     * 
     * @return line containing the segment
     */
    public Line getLine() {
        return this.line;
    }

    /**
     * Calculates the shortest distance from a point to this line segment.
     * <p>
     * If the perpendicular extension from the point to the line does not cross in the bounds of the line segment, the
     * shortest distance to the two end points will be returned.
     * </p>
     * 
     * Algorithm adapted from:
     * <a href="http://www.codeguru.com/forum/printthread.php?s=cc8cf0596231f9a7dba4da6e77c29db3&t=194400&pp=15&page=1">
     * Thread @ Codeguru</a>
     * 
     * @param p
     *        to check
     * @return distance between the instance and the point
     * @since 3.1
     */
    public double distance(final Vector2D p) {
        // closest point on the segment 
        final Vector2D closestP = getClosestPoint(p); 
        return closestP.distance(p); 
    }
    
    /**
     * Returns closest point of provided point belonging to segment.
     * @param p a point
     * @return closest point of provided point belonging to segment
     */
    public Vector2D getClosestPoint(final Vector2D p) {
        final double deltaX = this.end.getX() - this.start.getX();
        final double deltaY = this.end.getY() - this.start.getY();

        final double r = ((p.getX() - this.start.getX()) * deltaX + (p.getY() - this.start.getY()) * deltaY) /
            (deltaX * deltaX + deltaY * deltaY);

        // r == 0 => P = startPt
        // r == 1 => P = endPt
        // r < 0 => P is on the backward extension of the segment
        // r > 1 => P is on the forward extension of the segment
        // 0 < r < 1 => P is on the segment

        // if point isn't on the line segment, just return the closest point among start and end
        if (r < 0) {
            return this.start;
        } else if (r > 1) {
            return this.end;
        } else {
            // find point on line and see if it is in the line segment
            final double px = this.start.getX() + r * deltaX;
            final double py = this.start.getY() + r * deltaY;
            return new Vector2D(px, py);
        }
    }
    
    /**
     * Returns the intersection between two segments, null if there is no intersection.
     * @param segment a segment
     * @return the intersection between two segments, null if there is no intersection
     */
    @SuppressWarnings("PMD.NullAssignment")
    // Reason: null value expected (no intersection)
    public Vector2D getIntersection(final Segment segment) {
        // Slope
        final double l1x = this.end.getX() - this.start.getX();
        final double l2x = segment.end.getX() - segment.start.getX();
        final double l1y = this.end.getY() - this.start.getY();
        final double l2y = segment.end.getY() - segment.start.getY();
        final double a1 = l1y / l1x;
        final double a2 = l2y / l2x;
        
        final Vector2D res;
        if (a1 == a2) {
            // Lines are parallel (overlapping or not)
            res = null;
        } else {
            // The two lines intersect
            // Compute intersection (t1, t2) parameters such that start1 + t1 * lenght1 = start2 + t2* lenght2
            final double det = -l1x * l2y + l1y * l2x;
            final double dett1 = (segment.start.getX() - this.start.getX()) * (-l2y)
                    + (segment.start.getY() - this.start.getY()) * l2x;
            final double dett2 = (segment.start.getY() - this.start.getY()) * l1x - l1y
                    * (segment.start.getX() - this.start.getX());
            final double t1 = dett1 / det;
            final double t2 = dett2 / det;
            // Check if intersection is on both segments
            if (t1 < 0 || t2 > 1) {
                // Intersection out of segments
                res = null;
            } else {
                // Intersection
                res = new Vector2D(this.start.getX() + l1x * t1, this.start.getY() + l1y * t1);
            }
        }

        return res;
    }
}
