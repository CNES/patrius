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
package fr.cnes.sirius.patrius.math.geometry.euclidean.oned;

import fr.cnes.sirius.patrius.math.geometry.partitioning.Region.Location;

/**
 * This class represents a 1D interval.
 * 
 * @see IntervalsSet
 * @version $Id: Interval.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.0
 */
public class Interval {

    /** 0.5. */
    private static final double HALF = 0.5;

    /** The lower bound of the interval. */
    private final double lower;

    /** The upper bound of the interval. */
    private final double upper;

    /**
     * Simple constructor.
     * 
     * @param lowerIn
     *        lower bound of the interval
     * @param upperIn
     *        upper bound of the interval
     */
    public Interval(final double lowerIn, final double upperIn) {
        this.lower = lowerIn;
        this.upper = upperIn;
    }

    /**
     * Get the lower bound of the interval.
     * 
     * @return lower bound of the interval
     * @since 3.1
     */
    public double getInf() {
        return this.lower;
    }

    /**
     * Get the upper bound of the interval.
     * 
     * @return upper bound of the interval
     * @since 3.1
     */
    public double getSup() {
        return this.upper;
    }

    /**
     * Get the size of the interval.
     * 
     * @return size of the interval
     * @since 3.1
     */
    public double getSize() {
        return this.upper - this.lower;
    }

    /**
     * Get the barycenter of the interval.
     * 
     * @return barycenter of the interval
     * @since 3.1
     */
    public double getBarycenter() {
        return HALF * (this.lower + this.upper);
    }

    /**
     * Check a point with respect to the interval.
     * 
     * @param point
     *        point to check
     * @param tolerance
     *        tolerance below which points are considered to
     *        belong to the boundary
     * @return a code representing the point status: either {@link Location#INSIDE}, {@link Location#OUTSIDE} or
     *         {@link Location#BOUNDARY}
     * @since 3.1
     */
    public Location checkPoint(final double point, final double tolerance) {
        final Location res;
        if (point < this.lower - tolerance || point > this.upper + tolerance) {
            res = Location.OUTSIDE;
        } else if (point > this.lower + tolerance && point < this.upper - tolerance) {
            res = Location.INSIDE;
        } else {
            res = Location.BOUNDARY;
        }
        return res;
    }

}
