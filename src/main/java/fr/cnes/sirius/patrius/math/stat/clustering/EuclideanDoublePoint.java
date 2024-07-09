/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
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
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.cnes.sirius.patrius.math.stat.clustering;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;

import fr.cnes.sirius.patrius.math.util.MathArrays;

/**
 * A simple implementation of {@link Clusterable} for points with double coordinates.
 * 
 * @version $Id: EuclideanDoublePoint.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.1
 */
public class EuclideanDoublePoint implements Clusterable<EuclideanDoublePoint>, Serializable {

    /** Serializable version identifier. */
    private static final long serialVersionUID = 8026472786091227632L;

    /** Point coordinates. */
    private final double[] point;

    /**
     * Build an instance wrapping an integer array.
     * <p>
     * The wrapped array is referenced, it is <em>not</em> copied.
     * 
     * @param pointIn
     *        the n-dimensional point in integer space
     */
    public EuclideanDoublePoint(final double[] pointIn) {
        this.point = pointIn;
    }

    /** {@inheritDoc} */
    @Override
    public EuclideanDoublePoint centroidOf(final Collection<EuclideanDoublePoint> points) {
        // initialize array
        final double[] centroid = new double[this.getPoint().length];
        // loop on the Euclidean points
        for (final EuclideanDoublePoint p : points) {
            for (int i = 0; i < centroid.length; i++) {
                centroid[i] += p.getPoint()[i];
            }
        }
        for (int i = 0; i < centroid.length; i++) {
            centroid[i] /= points.size();
        }
        // return a new instance of EuclideanDoublePoint
        return new EuclideanDoublePoint(centroid);
    }

    /** {@inheritDoc} */
    @Override
    public double distanceFrom(final EuclideanDoublePoint p) {
        return MathArrays.distance(this.point, p.getPoint());
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof EuclideanDoublePoint)) {
            return false;
        }
        return Arrays.equals(this.point, ((EuclideanDoublePoint) other).point);
    }

    /**
     * Get the n-dimensional point in integer space.
     * 
     * @return a reference (not a copy!) to the wrapped array
     */
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    public double[] getPoint() {
        return this.point;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Arrays.hashCode(this.point);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return Arrays.toString(this.point);
    }

}
