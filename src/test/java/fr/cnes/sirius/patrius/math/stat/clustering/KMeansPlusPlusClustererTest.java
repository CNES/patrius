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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.stat.clustering;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;

public class KMeansPlusPlusClustererTest {

    @Test
    public void dimension2() {
        final KMeansPlusPlusClusterer<EuclideanIntegerPoint> transformer =
            new KMeansPlusPlusClusterer<>(new Random(1746432956321l));
        final EuclideanIntegerPoint[] points = new EuclideanIntegerPoint[] {

            // first expected cluster
            new EuclideanIntegerPoint(new int[] { -15, 3 }),
            new EuclideanIntegerPoint(new int[] { -15, 4 }),
            new EuclideanIntegerPoint(new int[] { -15, 5 }),
            new EuclideanIntegerPoint(new int[] { -14, 3 }),
            new EuclideanIntegerPoint(new int[] { -14, 5 }),
            new EuclideanIntegerPoint(new int[] { -13, 3 }),
            new EuclideanIntegerPoint(new int[] { -13, 4 }),
            new EuclideanIntegerPoint(new int[] { -13, 5 }),

            // second expected cluster
            new EuclideanIntegerPoint(new int[] { -1, 0 }),
            new EuclideanIntegerPoint(new int[] { -1, -1 }),
            new EuclideanIntegerPoint(new int[] { 0, -1 }),
            new EuclideanIntegerPoint(new int[] { 1, -1 }),
            new EuclideanIntegerPoint(new int[] { 1, -2 }),

            // third expected cluster
            new EuclideanIntegerPoint(new int[] { 13, 3 }),
            new EuclideanIntegerPoint(new int[] { 13, 4 }),
            new EuclideanIntegerPoint(new int[] { 14, 4 }),
            new EuclideanIntegerPoint(new int[] { 14, 7 }),
            new EuclideanIntegerPoint(new int[] { 16, 5 }),
            new EuclideanIntegerPoint(new int[] { 16, 6 }),
            new EuclideanIntegerPoint(new int[] { 17, 4 }),
            new EuclideanIntegerPoint(new int[] { 17, 7 })

        };
        final List<Cluster<EuclideanIntegerPoint>> clusters =
            transformer.cluster(Arrays.asList(points), 3, 5, 10);

        Assert.assertEquals(3, clusters.size());
        boolean cluster1Found = false;
        boolean cluster2Found = false;
        boolean cluster3Found = false;
        for (final Cluster<EuclideanIntegerPoint> cluster : clusters) {
            final int[] center = cluster.getCenter().getPoint();
            if (center[0] < 0) {
                cluster1Found = true;
                Assert.assertEquals(8, cluster.getPoints().size());
                Assert.assertEquals(-14, center[0]);
                Assert.assertEquals(4, center[1]);
            } else if (center[1] < 0) {
                cluster2Found = true;
                Assert.assertEquals(5, cluster.getPoints().size());
                Assert.assertEquals(0, center[0]);
                Assert.assertEquals(-1, center[1]);
            } else {
                cluster3Found = true;
                Assert.assertEquals(8, cluster.getPoints().size());
                Assert.assertEquals(15, center[0]);
                Assert.assertEquals(5, center[1]);
            }
        }
        Assert.assertTrue(cluster1Found);
        Assert.assertTrue(cluster2Found);
        Assert.assertTrue(cluster3Found);

    }

    /**
     * JIRA: MATH-305
     * 
     * Two points, one cluster, one iteration
     */
    @Test
    public void testPerformClusterAnalysisDegenerate() {
        final KMeansPlusPlusClusterer<EuclideanIntegerPoint> transformer = new KMeansPlusPlusClusterer<>(
                new Random(1746432956321l));
        final EuclideanIntegerPoint[] points = new EuclideanIntegerPoint[] {
            new EuclideanIntegerPoint(new int[] { 1959, 325100 }),
            new EuclideanIntegerPoint(new int[] { 1960, 373200 }), };
        final List<Cluster<EuclideanIntegerPoint>> clusters = transformer.cluster(Arrays.asList(points), 1, 1);
        Assert.assertEquals(1, clusters.size());
        Assert.assertEquals(2, (clusters.get(0).getPoints().size()));
        final EuclideanIntegerPoint pt1 = new EuclideanIntegerPoint(new int[] { 1959, 325100 });
        final EuclideanIntegerPoint pt2 = new EuclideanIntegerPoint(new int[] { 1960, 373200 });
        Assert.assertTrue(clusters.get(0).getPoints().contains(pt1));
        Assert.assertTrue(clusters.get(0).getPoints().contains(pt2));

    }

    @Test
    public void testCertainSpace() {
        final KMeansPlusPlusClusterer.EmptyClusterStrategy[] strategies = {
            KMeansPlusPlusClusterer.EmptyClusterStrategy.LARGEST_VARIANCE,
            KMeansPlusPlusClusterer.EmptyClusterStrategy.LARGEST_POINTS_NUMBER,
            KMeansPlusPlusClusterer.EmptyClusterStrategy.FARTHEST_POINT
        };
        for (final KMeansPlusPlusClusterer.EmptyClusterStrategy strategy : strategies) {
            final KMeansPlusPlusClusterer<EuclideanIntegerPoint> transformer =
                new KMeansPlusPlusClusterer<>(new Random(1746432956321l), strategy);
            final int numberOfVariables = 27;
            // initialise testvalues
            int position1 = 1;
            int position2 = position1 + numberOfVariables;
            int position3 = position2 + numberOfVariables;
            int position4 = position3 + numberOfVariables;
            // testvalues will be multiplied
            final int multiplier = 1000000;

            final EuclideanIntegerPoint[] breakingPoints = new EuclideanIntegerPoint[numberOfVariables];
            // define the space which will break the cluster algorithm
            for (int i = 0; i < numberOfVariables; i++) {
                final int points[] = { position1, position2, position3, position4 };
                // multiply the values
                for (int j = 0; j < points.length; j++) {
                    points[j] = points[j] * multiplier;
                }
                final EuclideanIntegerPoint euclideanIntegerPoint = new EuclideanIntegerPoint(points);
                breakingPoints[i] = euclideanIntegerPoint;
                position1 = position1 + numberOfVariables;
                position2 = position2 + numberOfVariables;
                position3 = position3 + numberOfVariables;
                position4 = position4 + numberOfVariables;
            }

            for (int n = 2; n < 27; ++n) {
                final List<Cluster<EuclideanIntegerPoint>> clusters =
                    transformer.cluster(Arrays.asList(breakingPoints), n, 100);
                Assert.assertEquals(n, clusters.size());
                int sum = 0;
                for (final Cluster<EuclideanIntegerPoint> cluster : clusters) {
                    sum += cluster.getPoints().size();
                }
                Assert.assertEquals(numberOfVariables, sum);
            }
        }

    }

    /**
     * A helper class for testSmallDistances(). This class is similar to EuclideanIntegerPoint, but
     * it defines a different distanceFrom() method that tends to return distances less than 1.
     */
    private class CloseIntegerPoint implements Clusterable<CloseIntegerPoint> {
        public CloseIntegerPoint(final EuclideanIntegerPoint point) {
            this.euclideanPoint = point;
        }

        @Override
        public double distanceFrom(final CloseIntegerPoint p) {
            return this.euclideanPoint.distanceFrom(p.euclideanPoint) * 0.001;
        }

        @Override
        public CloseIntegerPoint centroidOf(final Collection<CloseIntegerPoint> p) {
            final Collection<EuclideanIntegerPoint> euclideanPoints =
                new ArrayList<>();
            for (final CloseIntegerPoint point : p) {
                euclideanPoints.add(point.euclideanPoint);
            }
            return new CloseIntegerPoint(this.euclideanPoint.centroidOf(euclideanPoints));
        }

        @Override
        public boolean equals(final Object o) {
            if (!(o instanceof CloseIntegerPoint)) {
                return false;
            }
            final CloseIntegerPoint p = (CloseIntegerPoint) o;

            return this.euclideanPoint.equals(p.euclideanPoint);
        }

        @Override
        public int hashCode() {
            return this.euclideanPoint.hashCode();
        }

        private final EuclideanIntegerPoint euclideanPoint;
    }

    /**
     * Test points that are very close together. See issue MATH-546.
     */
    @Test
    public void testSmallDistances() {
        // Create a bunch of CloseIntegerPoints. Most are identical, but one is different by a
        // small distance.
        final int[] repeatedArray = { 0 };
        final int[] uniqueArray = { 1 };
        final CloseIntegerPoint repeatedPoint =
            new CloseIntegerPoint(new EuclideanIntegerPoint(repeatedArray));
        final CloseIntegerPoint uniquePoint =
            new CloseIntegerPoint(new EuclideanIntegerPoint(uniqueArray));

        final Collection<CloseIntegerPoint> points = new ArrayList<>();
        final int NUM_REPEATED_POINTS = 10 * 1000;
        for (int i = 0; i < NUM_REPEATED_POINTS; ++i) {
            points.add(repeatedPoint);
        }
        points.add(uniquePoint);

        // Ask a KMeansPlusPlusClusterer to run zero iterations (i.e., to simply choose initial
        // cluster centers).
        final long RANDOM_SEED = 0;
        final int NUM_CLUSTERS = 2;
        final int NUM_ITERATIONS = 0;
        final KMeansPlusPlusClusterer<CloseIntegerPoint> clusterer =
            new KMeansPlusPlusClusterer<>(new Random(RANDOM_SEED));
        final List<Cluster<CloseIntegerPoint>> clusters =
            clusterer.cluster(points, NUM_CLUSTERS, NUM_ITERATIONS);

        // Check that one of the chosen centers is the unique point.
        boolean uniquePointIsCenter = false;
        for (final Cluster<CloseIntegerPoint> cluster : clusters) {
            if (cluster.getCenter().equals(uniquePoint)) {
                uniquePointIsCenter = true;
            }
        }
        Assert.assertTrue(uniquePointIsCenter);
    }

    /**
     * 2 variables cannot be clustered into 3 clusters. See issue MATH-436.
     */
    @Test(expected = NumberIsTooSmallException.class)
    public void testPerformClusterAnalysisToManyClusters() {
        final KMeansPlusPlusClusterer<EuclideanIntegerPoint> transformer =
            new KMeansPlusPlusClusterer<>(
                new Random(1746432956321l));

        final EuclideanIntegerPoint[] points = new EuclideanIntegerPoint[] {
            new EuclideanIntegerPoint(new int[] {
                1959, 325100
            }), new EuclideanIntegerPoint(new int[] {
                1960, 373200
            })
        };

        transformer.cluster(Arrays.asList(points), 3, 1);
    }
}
