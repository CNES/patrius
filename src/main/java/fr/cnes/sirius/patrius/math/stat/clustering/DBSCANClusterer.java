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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.stat.clustering;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import fr.cnes.sirius.patrius.math.exception.NotPositiveException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.util.MathUtils;

/**
 * DBSCAN (density-based spatial clustering of applications with noise) algorithm.
 * <p>
 * The DBSCAN algorithm forms clusters based on the idea of density connectivity, i.e. a point p is density connected to
 * another point q, if there exists a chain of points p<sub>i</sub>, with i = 1 .. n and p<sub>1</sub> = p and
 * p<sub>n</sub> = q, such that each pair &lt;p<sub>i</sub>, p<sub>i+1</sub>&gt; is directly density-reachable. A point
 * q is directly density-reachable from point p if it is in the &epsilon;-neighborhood of this point.
 * <p>
 * Any point that is not density-reachable from a formed cluster is treated as noise, and will thus not be present in
 * the result.
 * <p>
 * The algorithm requires two parameters:
 * <ul>
 * <li>eps: the distance that defines the &epsilon;-neighborhood of a point
 * <li>minPoints: the minimum number of density-connected points required to form a cluster
 * </ul>
 * <p>
 * <b>Note:</b> as DBSCAN is not a centroid-based clustering algorithm, the resulting {@link Cluster} objects will have
 * no defined center, i.e. {@link Cluster#getCenter()} will return {@code null}.
 * 
 * @param <T>
 *        type of the points to cluster
 * @see <a href="http://en.wikipedia.org/wiki/DBSCAN">DBSCAN (wikipedia)</a>
 * @see <a href="http://www.dbs.ifi.lmu.de/Publikationen/Papers/KDD-96.final.frame.pdf"> A Density-Based Algorithm for
 *      Discovering Clusters in Large Spatial Databases with Noise</a>
 * @version $Id: DBSCANClusterer.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.1
 */
public class DBSCANClusterer<T extends Clusterable<T>> {

    /** Maximum radius of the neighborhood to be considered. */
    private final double eps;

    /** Minimum number of points needed for a cluster. */
    private final int minPts;

    /** Status of a point during the clustering process. */
    private enum PointStatus {
        /** The point has is considered to be noise. */
        NOISE,
        /** The point is already part of a cluster. */
        PART_OF_CLUSTER
    }

    /**
     * Creates a new instance of a DBSCANClusterer.
     * 
     * @param epsIn
     *        maximum radius of the neighborhood to be considered
     * @param minPtsIn
     *        minimum number of points needed for a cluster
     * @throws NotPositiveException
     *         if {@code eps < 0.0} or {@code minPts < 0}
     */
    public DBSCANClusterer(final double epsIn, final int minPtsIn) {
        if (epsIn < 0.0d) {
            throw new NotPositiveException(epsIn);
        }
        if (minPtsIn < 0) {
            throw new NotPositiveException(minPtsIn);
        }
        this.eps = epsIn;
        this.minPts = minPtsIn;
    }

    /**
     * Returns the maximum radius of the neighborhood to be considered.
     * 
     * @return maximum radius of the neighborhood
     */
    public double getEps() {
        return this.eps;
    }

    /**
     * Returns the minimum number of points needed for a cluster.
     * 
     * @return minimum number of points needed for a cluster
     */
    public int getMinPts() {
        return this.minPts;
    }

    /**
     * Performs DBSCAN cluster analysis.
     * <p>
     * <b>Note:</b> as DBSCAN is not a centroid-based clustering algorithm, the resulting {@link Cluster} objects will
     * have no defined center, i.e. {@link Cluster#getCenter()} will return {@code null}.
     * 
     * @param points
     *        the points to cluster
     * @return the list of clusters
     * @throws NullArgumentException
     *         if the data points are null
     */
    public List<Cluster<T>> cluster(final Collection<T> points) {

        // sanity checks
        MathUtils.checkNotNull(points);

        // initialize clusters with an array list
        final List<Cluster<T>> clusters = new ArrayList<>();
        // new instance of a HashMap
        final Map<Clusterable<T>, PointStatus> visited = new ConcurrentHashMap<>();

        for (final T point : points) {
            if (visited.get(point) != null) {
                continue;
            }
            final List<T> neighbors = this.getNeighbors(point, points);
            if (neighbors.size() >= this.minPts) {
                // DBSCAN does not care about center points
                final Cluster<T> cluster = new Cluster<>(null);
                clusters.add(this.expandCluster(cluster, point, neighbors, points, visited));
            } else {
                visited.put(point, PointStatus.NOISE);
            }
        }

        return clusters;
    }

    /**
     * Expands the cluster to include density-reachable items.
     * 
     * @param cluster
     *        Cluster to expand
     * @param point
     *        Point to add to cluster
     * @param neighbors
     *        List of neighbors
     * @param points
     *        the data set
     * @param visited
     *        the set of already visited points
     * @return the expanded cluster
     */
    private Cluster<T> expandCluster(final Cluster<T> cluster,
                                     final T point,
                                     final List<T> neighbors,
                                     final Collection<T> points,
                                     final Map<Clusterable<T>, PointStatus> visited) {
        // Initialization
        cluster.addPoint(point);
        visited.put(point, PointStatus.PART_OF_CLUSTER);

        List<T> seeds = new ArrayList<>(neighbors);
        int index = 0;
        while (index < seeds.size()) {
            // Loop
            final T current = seeds.get(index);
            final PointStatus pStatus = visited.get(current);
            // only check non-visited points
            if (pStatus == null) {
                final List<T> currentNeighbors = this.getNeighbors(current, points);
                if (currentNeighbors.size() >= this.minPts) {
                    seeds = this.merge(seeds, currentNeighbors);
                }
            }

            if (pStatus != PointStatus.PART_OF_CLUSTER) {
                visited.put(current, PointStatus.PART_OF_CLUSTER);
                cluster.addPoint(current);
            }

            index++;
        }
        // Return result
        return cluster;
    }

    /**
     * Returns a list of density-reachable neighbors of a {@code point}.
     * 
     * @param point
     *        the point to look for
     * @param points
     *        possible neighbors
     * @return the List of neighbors
     */
    private List<T> getNeighbors(final T point, final Collection<T> points) {
        final List<T> neighbors = new ArrayList<>();
        for (final T neighbor : points) {
            if (!point.equals(neighbor) && neighbor.distanceFrom(point) <= this.eps) {
                neighbors.add(neighbor);
            }
        }
        return neighbors;
    }

    /**
     * Merges two lists together.
     * 
     * @param one
     *        first list
     * @param two
     *        second list
     * @return merged lists
     */
    private List<T> merge(final List<T> one, final List<T> two) {
        final Set<T> oneSet = new HashSet<>(one);
        for (final T item : two) {
            if (!oneSet.contains(item)) {
                one.add(item);
            }
        }
        return one;
    }
}
