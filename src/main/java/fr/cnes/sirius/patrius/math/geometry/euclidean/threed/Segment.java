/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 *
 * Copyright 2011-2017 CNES
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
 */
package fr.cnes.sirius.patrius.math.geometry.euclidean.threed;

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

}