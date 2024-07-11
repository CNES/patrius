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
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.optim.nonlinear.vector.jacobian;

import fr.cnes.sirius.patrius.math.distribution.NormalDistribution;
import fr.cnes.sirius.patrius.math.distribution.RealDistribution;
import fr.cnes.sirius.patrius.math.distribution.UniformRealDistribution;
import fr.cnes.sirius.patrius.math.geometry.euclidean.twod.Vector2D;
import fr.cnes.sirius.patrius.math.random.RandomGenerator;
import fr.cnes.sirius.patrius.math.random.Well44497b;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;

/**
 * Factory for generating a cloud of points that approximate a circle.
 */
public class RandomCirclePointGenerator {
    /** RNG for the x-coordinate of the center. */
    private final RealDistribution cX;
    /** RNG for the y-coordinate of the center. */
    private final RealDistribution cY;
    /** RNG for the parametric position of the point. */
    private final RealDistribution tP;
    /** Radius of the circle. */
    private final double radius;

    /**
     * @param x
     *        Abscissa of the circle center.
     * @param y
     *        Ordinate of the circle center.
     * @param radius
     *        Radius of the circle.
     * @param xSigma
     *        Error on the x-coordinate of the circumference points.
     * @param ySigma
     *        Error on the y-coordinate of the circumference points.
     * @param seed
     *        RNG seed.
     */
    public RandomCirclePointGenerator(final double x,
        final double y,
        final double radius,
        final double xSigma,
        final double ySigma,
        final long seed) {
        final RandomGenerator rng = new Well44497b(seed);
        this.radius = radius;
        this.cX = new NormalDistribution(rng, x, xSigma,
            NormalDistribution.DEFAULT_INVERSE_ABSOLUTE_ACCURACY);
        this.cY = new NormalDistribution(rng, y, ySigma,
            NormalDistribution.DEFAULT_INVERSE_ABSOLUTE_ACCURACY);
        this.tP = new UniformRealDistribution(rng, 0, MathUtils.TWO_PI,
            UniformRealDistribution.DEFAULT_INVERSE_ABSOLUTE_ACCURACY);
    }

    /**
     * Point generator.
     * 
     * @param n
     *        Number of points to create.
     * @return the cloud of {@code n} points.
     */
    public Vector2D[] generate(final int n) {
        final Vector2D[] cloud = new Vector2D[n];
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
    private Vector2D create() {
        final double t = this.tP.sample();
        final double pX = this.cX.sample() + this.radius * MathLib.cos(t);
        final double pY = this.cY.sample() + this.radius * MathLib.sin(t);

        return new Vector2D(pX, pY);
    }
}
