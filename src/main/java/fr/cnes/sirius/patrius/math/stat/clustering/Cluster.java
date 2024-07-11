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
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.stat.clustering;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Cluster holding a set of {@link Clusterable} points.
 * 
 * @param <T>
 *        the type of points that can be clustered
 * @version $Id: Cluster.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 2.0
 */
public class Cluster<T extends Clusterable<T>> implements Serializable {

    /** Serializable version identifier. */
    private static final long serialVersionUID = -3442297081515880464L;

    /** The points contained in this cluster. */
    private final List<T> points;

    /** Center of the cluster. */
    private final T center;

    /**
     * Build a cluster centered at a specified point.
     * 
     * @param centerIn
     *        the point which is to be the center of this cluster
     */
    public Cluster(final T centerIn) {
        this.center = centerIn;
        this.points = new ArrayList<T>();
    }

    /**
     * Add a point to this cluster.
     * 
     * @param point
     *        point to add
     */
    public void addPoint(final T point) {
        this.points.add(point);
    }

    /**
     * Get the points contained in the cluster.
     * 
     * @return points contained in the cluster
     */
    public List<T> getPoints() {
        return this.points;
    }

    /**
     * Get the point chosen to be the center of this cluster.
     * 
     * @return chosen cluster center
     */
    public T getCenter() {
        return this.center;
    }

}
