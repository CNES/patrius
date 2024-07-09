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
package fr.cnes.sirius.patrius.math.analysis.integration.gauss;

import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.NonMonotonicSequenceException;
import fr.cnes.sirius.patrius.math.util.MathArrays;
import fr.cnes.sirius.patrius.math.util.Pair;

/**
 * Class that implements the Gaussian rule for {@link #integrate(UnivariateFunction) integrating} a weighted
 * function.
 * 
 * @since 3.1
 * @version $Id: GaussIntegrator.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class GaussIntegrator {
    /** Nodes. */
    private final double[] points;
    /** Nodes weights. */
    private final double[] weights;

    /**
     * Creates an integrator from the given {@code points} and {@code weights}.
     * The integration interval is defined by the first and last value of {@code points} which must be sorted in
     * increasing order.
     * 
     * @param pointsIn
     *        Integration points.
     * @param weightsIn
     *        Weights of the corresponding integration nodes.
     * @throws NonMonotonicSequenceException
     *         if the {@code points} are not
     *         sorted in increasing order.
     */
    public GaussIntegrator(final double[] pointsIn,
        final double[] weightsIn) {
        if (pointsIn.length != weightsIn.length) {
            throw new DimensionMismatchException(pointsIn.length,
                weightsIn.length);
        }

        MathArrays.checkOrder(pointsIn, MathArrays.OrderDirection.INCREASING, true, true);

        this.points = pointsIn.clone();
        this.weights = weightsIn.clone();
    }

    /**
     * Creates an integrator from the given pair of points (first element of
     * the pair) and weights (second element of the pair.
     * 
     * @param pointsAndWeights
     *        Integration points and corresponding weights.
     * @throws NonMonotonicSequenceException
     *         if the {@code points} are not
     *         sorted in increasing order.
     * 
     * @see #GaussIntegrator(double[], double[])
     */
    public GaussIntegrator(final Pair<double[], double[]> pointsAndWeights) {
        this(pointsAndWeights.getFirst(), pointsAndWeights.getSecond());
    }

    /**
     * Returns an estimate of the integral of {@code f(x) * w(x)},
     * where {@code w} is a weight function that depends on the actual
     * flavor of the Gauss integration scheme.
     * The algorithm uses the points and associated weights, as passed
     * to the {@link #GaussIntegrator(double[],double[]) constructor}.
     * 
     * @param f
     *        Function to integrate.
     * @return the integral of the weighted function.
     */
    public double integrate(final UnivariateFunction f) {
        // Init variables
        double s = 0;
        double c = 0;
        // Loop on all points
        for (int i = 0; i < this.points.length; i++) {
            final double x = this.points[i];
            final double w = this.weights[i];
            final double y = w * f.value(x) - c;
            final double t = s + y;
            c = (t - s) - y;
            s = t;
        }
        // return the computed estimate of the integral of the weighted function
        return s;
    }

    /**
     * @return the order of the integration rule (the number of integration
     *         points).
     */
    public int getNumberOfPoints() {
        return this.points.length;
    }
}
