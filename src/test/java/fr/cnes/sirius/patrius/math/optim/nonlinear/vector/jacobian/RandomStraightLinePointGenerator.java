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
package fr.cnes.sirius.patrius.math.optim.nonlinear.vector.jacobian;

import java.awt.geom.Point2D;

import fr.cnes.sirius.patrius.math.distribution.NormalDistribution;
import fr.cnes.sirius.patrius.math.distribution.RealDistribution;
import fr.cnes.sirius.patrius.math.distribution.UniformRealDistribution;
import fr.cnes.sirius.patrius.math.random.RandomGenerator;
import fr.cnes.sirius.patrius.math.random.Well44497b;

/**
 * Factory for generating a cloud of points that approximate a straight line.
 */
public class RandomStraightLinePointGenerator {
    /** Slope. */
    private final double slope;
    /** Intercept. */
    private final double intercept;
    /** RNG for the x-coordinate. */
    private final RealDistribution x;
    /** RNG for the error on the y-coordinate. */
    private final RealDistribution error;

    /**
     * The generator will create a cloud of points whose x-coordinates
     * will be randomly sampled between {@code xLo} and {@code xHi}, and
     * the corresponding y-coordinates will be computed as
     * 
     * <pre>
     * <code>
     *  y = a x + b + N(0, error)
     * </code>
     * </pre>
     * 
     * where {@code N(mean, sigma)} is a Gaussian distribution with the
     * given mean and standard deviation.
     * 
     * @param a
     *        Slope.
     * @param b
     *        Intercept.
     * @param sigma
     *        Standard deviation on the y-coordinate of the point.
     * @param lo
     *        Lowest value of the x-coordinate.
     * @param hi
     *        Highest value of the x-coordinate.
     * @param seed
     *        RNG seed.
     */
    public RandomStraightLinePointGenerator(final double a,
        final double b,
        final double sigma,
        final double lo,
        final double hi,
        final long seed) {
        final RandomGenerator rng = new Well44497b(seed);
        this.slope = a;
        this.intercept = b;
        this.error = new NormalDistribution(rng, 0, sigma,
            NormalDistribution.DEFAULT_INVERSE_ABSOLUTE_ACCURACY);
        this.x = new UniformRealDistribution(rng, lo, hi,
            UniformRealDistribution.DEFAULT_INVERSE_ABSOLUTE_ACCURACY);
    }

    /**
     * Point generator.
     * 
     * @param n
     *        Number of points to create.
     * @return the cloud of {@code n} points.
     */
    public Point2D.Double[] generate(final int n) {
        final Point2D.Double[] cloud = new Point2D.Double[n];
        for (int i = 0; i < n; i++) {
            cloud[i] = this.create();
        }
        return cloud;
    }

    /**
     * Create one point.
     * 
     * @return a point.
     */
    private Point2D.Double create() {
        final double abscissa = this.x.sample();
        final double yModel = this.slope * abscissa + this.intercept;
        final double ordinate = yModel + this.error.sample();

        return new Point2D.Double(abscissa, ordinate);
    }
}
