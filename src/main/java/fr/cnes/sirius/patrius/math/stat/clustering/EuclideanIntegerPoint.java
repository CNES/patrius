/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 *
 * Copyright 2011-2022 CNES
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
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.stat.clustering;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;

import fr.cnes.sirius.patrius.math.util.MathArrays;

/**
 * A simple implementation of {@link Clusterable} for points with integer coordinates.
 * 
 * @version $Id: EuclideanIntegerPoint.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 2.0
 */
public class EuclideanIntegerPoint implements Clusterable<EuclideanIntegerPoint>, Serializable {

    /** Serializable version identifier. */
    private static final long serialVersionUID = 3946024775784901369L;

    /** Point coordinates. */
    private final int[] point;

    /**
     * Build an instance wrapping an integer array.
     * <p>
     * The wrapped array is referenced, it is <em>not</em> copied.
     * </p>
     * 
     * @param pointIn
     *        the n-dimensional point in integer space
     */
    public EuclideanIntegerPoint(final int[] pointIn) {
        this.point = pointIn;
    }

    /**
     * Get the n-dimensional point in integer space.
     * 
     * @return a reference (not a copy!) to the wrapped array
     */
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    public int[] getPoint() {
        return this.point;
    }

    /** {@inheritDoc} */
    @Override
    public double distanceFrom(final EuclideanIntegerPoint p) {
        return MathArrays.distance(this.point, p.getPoint());
    }

    /** {@inheritDoc} */
    @Override
    public EuclideanIntegerPoint centroidOf(final Collection<EuclideanIntegerPoint> points) {
        // initialize array
        final int[] centroid = new int[this.getPoint().length];
        // loop on the points
        for (final EuclideanIntegerPoint p : points) {
            for (int i = 0; i < centroid.length; i++) {
                centroid[i] += p.getPoint()[i];
            }
        }
        for (int i = 0; i < centroid.length; i++) {
            centroid[i] /= points.size();
        }
        // return new instance of EuclideanIntegerPoint
        return new EuclideanIntegerPoint(centroid);
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof EuclideanIntegerPoint)) {
            return false;
        }
        return Arrays.equals(this.point, ((EuclideanIntegerPoint) other).point);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Arrays.hashCode(this.point);
    }

    /**
     * {@inheritDoc}
     * 
     * @since 2.1
     */
    @Override
    public String toString() {
        return Arrays.toString(this.point);
    }

}
