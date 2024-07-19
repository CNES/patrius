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
 * VERSION:4.11:DM:DM-3232:22/05/2023:[PATRIUS] Detection d'extrema dans la classe ExtremaGenericDetector
 * VERSION:4.10.1:FA:FA-3275:02/12/2022:[PATRIUS] Mauvais calcul d'intersection entre 2 segments 2D
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.geometry.euclidean.threed;

import fr.cnes.sirius.patrius.math.util.Precision;

/**
 * Simple container for a two-points segment.
 * 
 * @version $Id: Segment.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.0
 */
public class Segment {

    /** Start point of the segment. */
    private final Vector3D start;

    /** End point of the segments. */
    private final Vector3D end;

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
    public Segment(final Vector3D startIn, final Vector3D endIn, final Line lineIn) {
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
    public Segment(final Vector3D startIn, final Vector3D endIn) {
        this(startIn, endIn, new Line(startIn, endIn));
    }

    /**
     * Get the start point of the segment.
     * 
     * @return start point of the segment
     */
    public Vector3D getStart() {
        return this.start;
    }

    /**
     * Get the end point of the segment.
     * 
     * @return end point of the segment
     */
    public Vector3D getEnd() {
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
     * Indicates whether or not the given segments intersect each other.
     * 
     * @param segments array of segments
     * @return {@code true} if the given segments intersect each other
     */
    public static boolean isIntersectingSegments(final Segment[] segments) {
        // Initialization
        boolean intersectingSegments = false;
        final int numberOfSegments = segments.length;
        Line line1;
        Line line2;
        Vector3D intersectionPoint;
        
        // Loop on all possible pairs of segments
        for (int i = 0; i < numberOfSegments - 1; i++) {
            for (int j = i + 1; j < numberOfSegments; j++) {
                line1 = segments[i].getLine();
                line2 = segments[j].getLine();
                // Check if an intersection point exists between the two lines given by the two segments
                intersectionPoint = line1.intersection(line2);
                // Check if the intersection point is contained within both segments
                if (null != intersectionPoint) {
                    if ((line1.getAbscissa(intersectionPoint) - line1.getAbscissa(segments[i].getStart()))
                            > Precision.DOUBLE_COMPARISON_EPSILON
                            && (line1.getAbscissa(segments[i].getEnd()) - line1.getAbscissa(intersectionPoint))
                            > Precision.DOUBLE_COMPARISON_EPSILON
                            && (line2.getAbscissa(intersectionPoint) - line2.getAbscissa(segments[j].getStart()))
                            > Precision.DOUBLE_COMPARISON_EPSILON
                            && (line2.getAbscissa(segments[j].getEnd()) - line2.getAbscissa(intersectionPoint))
                            > Precision.DOUBLE_COMPARISON_EPSILON) {
                        intersectingSegments = true;
                        break;
                    }
                }
            }
        }

        return intersectingSegments;
    }

}
