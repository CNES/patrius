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
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.optim.nonlinear.scalar.noderiv;

import java.util.Comparator;

import fr.cnes.sirius.patrius.math.analysis.MultivariateFunction;
import fr.cnes.sirius.patrius.math.optim.PointValuePair;

/**
 * This class implements the multi-directional direct search method.
 * 
 * @version $Id: MultiDirectionalSimplex.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.0
 */
public class MultiDirectionalSimplex extends AbstractSimplex {
    /** Default value for {@link #khi}: {@value} . */
    private static final double DEFAULT_KHI = 2;
    /** Default value for {@link #gamma}: {@value} . */
    private static final double DEFAULT_GAMMA = 0.5;
    /** Expansion coefficient. */
    private final double khi;
    /** Contraction coefficient. */
    private final double gamma;

    /**
     * Build a multi-directional simplex with default coefficients.
     * The default values are 2.0 for khi and 0.5 for gamma.
     * 
     * @param n
     *        Dimension of the simplex.
     */
    public MultiDirectionalSimplex(final int n) {
        this(n, 1d);
    }

    /**
     * Build a multi-directional simplex with default coefficients.
     * The default values are 2.0 for khi and 0.5 for gamma.
     * 
     * @param n
     *        Dimension of the simplex.
     * @param sideLength
     *        Length of the sides of the default (hypercube)
     *        simplex. See {@link AbstractSimplex#AbstractSimplex(int,double)}.
     */
    public MultiDirectionalSimplex(final int n, final double sideLength) {
        this(n, sideLength, DEFAULT_KHI, DEFAULT_GAMMA);
    }

    /**
     * Build a multi-directional simplex with specified coefficients.
     * 
     * @param n
     *        Dimension of the simplex. See {@link AbstractSimplex#AbstractSimplex(int,double)}.
     * @param khiIn
     *        Expansion coefficient.
     * @param gammaIn
     *        Contraction coefficient.
     */
    public MultiDirectionalSimplex(final int n,
        final double khiIn, final double gammaIn) {
        this(n, 1d, khiIn, gammaIn);
    }

    /**
     * Build a multi-directional simplex with specified coefficients.
     * 
     * @param n
     *        Dimension of the simplex. See {@link AbstractSimplex#AbstractSimplex(int,double)}.
     * @param sideLength
     *        Length of the sides of the default (hypercube)
     *        simplex. See {@link AbstractSimplex#AbstractSimplex(int,double)}.
     * @param khiIn
     *        Expansion coefficient.
     * @param gammaIn
     *        Contraction coefficient.
     */
    public MultiDirectionalSimplex(final int n, final double sideLength,
        final double khiIn, final double gammaIn) {
        super(n, sideLength);

        this.khi = khiIn;
        this.gamma = gammaIn;
    }

    /**
     * Build a multi-directional simplex with default coefficients.
     * The default values are 2.0 for khi and 0.5 for gamma.
     * 
     * @param steps
     *        Steps along the canonical axes representing box edges.
     *        They may be negative but not zero. See
     */
    public MultiDirectionalSimplex(final double[] steps) {
        this(steps, DEFAULT_KHI, DEFAULT_GAMMA);
    }

    /**
     * Build a multi-directional simplex with specified coefficients.
     * 
     * @param steps
     *        Steps along the canonical axes representing box edges.
     *        They may be negative but not zero. See {@link AbstractSimplex#AbstractSimplex(double[])}.
     * @param khiIn
     *        Expansion coefficient.
     * @param gammaIn
     *        Contraction coefficient.
     */
    public MultiDirectionalSimplex(final double[] steps,
        final double khiIn, final double gammaIn) {
        super(steps);

        this.khi = khiIn;
        this.gamma = gammaIn;
    }

    /**
     * Build a multi-directional simplex with default coefficients.
     * The default values are 2.0 for khi and 0.5 for gamma.
     * 
     * @param referenceSimplex
     *        Reference simplex. See {@link AbstractSimplex#AbstractSimplex(double[][])}.
     */
    public MultiDirectionalSimplex(final double[][] referenceSimplex) {
        this(referenceSimplex, DEFAULT_KHI, DEFAULT_GAMMA);
    }

    /**
     * Build a multi-directional simplex with specified coefficients.
     * 
     * @param referenceSimplex
     *        Reference simplex. See {@link AbstractSimplex#AbstractSimplex(double[][])}.
     * @param khiIn
     *        Expansion coefficient.
     * @param gammaIn
     *        Contraction coefficient.
     * @throws fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException
     *         if the reference simplex does not contain at least one point.
     * @throws fr.cnes.sirius.patrius.math.exception.DimensionMismatchException
     *         if there is a dimension mismatch in the reference simplex.
     */
    public MultiDirectionalSimplex(final double[][] referenceSimplex,
        final double khiIn, final double gammaIn) {
        super(referenceSimplex);

        this.khi = khiIn;
        this.gamma = gammaIn;
    }

    /** {@inheritDoc} */
    @Override
    public void iterate(final MultivariateFunction evaluationFunction,
                        final Comparator<PointValuePair> comparator) {
        // Save the original simplex.
        final PointValuePair[] original = this.getPoints();
        final PointValuePair best = original[0];

        // Perform a reflection step.
        final PointValuePair reflected = this.evaluateNewSimplex(evaluationFunction,
            original, 1, comparator);
        if (comparator.compare(reflected, best) < 0) {
            // Compute the expanded simplex.
            final PointValuePair[] reflectedSimplex = this.getPoints();
            final PointValuePair expanded = this.evaluateNewSimplex(evaluationFunction,
                original, this.khi, comparator);
            if (comparator.compare(reflected, expanded) <= 0) {
                // Keep the reflected simplex.
                this.setPoints(reflectedSimplex);
            }
            // Keep the expanded simplex.
            return;
        }

        // Compute the contracted simplex.
        this.evaluateNewSimplex(evaluationFunction, original, this.gamma, comparator);

    }

    /**
     * Compute and evaluate a new simplex.
     * 
     * @param evaluationFunction
     *        Evaluation function.
     * @param original
     *        Original simplex (to be preserved).
     * @param coeff
     *        Linear coefficient.
     * @param comparator
     *        Comparator to use to sort simplex vertices from best
     *        to poorest.
     * @return the best point in the transformed simplex.
     * @throws fr.cnes.sirius.patrius.math.exception.TooManyEvaluationsException
     *         if the maximal number of evaluations is exceeded.
     */
    private PointValuePair evaluateNewSimplex(final MultivariateFunction evaluationFunction,
                                              final PointValuePair[] original,
                                              final double coeff,
                                              final Comparator<PointValuePair> comparator) {
        final double[] xSmallest = original[0].getPointRef();
        // Perform a linear transformation on all the simplex points,
        // except the first one.
        this.setPoint(0, original[0]);
        final int dim = this.getDimension();
        for (int i = 1; i < this.getSize(); i++) {
            final double[] xOriginal = original[i].getPointRef();
            final double[] xTransformed = new double[dim];
            for (int j = 0; j < dim; j++) {
                xTransformed[j] = xSmallest[j] + coeff * (xSmallest[j] - xOriginal[j]);
            }
            this.setPoint(i, new PointValuePair(xTransformed, Double.NaN, false));
        }

        // Evaluate the simplex.
        this.evaluate(evaluationFunction, comparator);

        return this.getPoint(0);
    }
}
