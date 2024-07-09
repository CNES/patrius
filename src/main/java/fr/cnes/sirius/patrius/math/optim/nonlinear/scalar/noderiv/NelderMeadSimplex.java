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
package fr.cnes.sirius.patrius.math.optim.nonlinear.scalar.noderiv;

import java.util.Comparator;

import fr.cnes.sirius.patrius.math.analysis.MultivariateFunction;
import fr.cnes.sirius.patrius.math.optim.PointValuePair;

/**
 * This class implements the Nelder-Mead simplex algorithm.
 * 
 * @version $Id: NelderMeadSimplex.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.0
 */
public class NelderMeadSimplex extends AbstractSimplex {
    /** Default value for {@link #rho}: {@value} . */
    private static final double DEFAULT_RHO = 1;
    /** Default value for {@link #khi}: {@value} . */
    private static final double DEFAULT_KHI = 2;
    /** Default value for {@link #gamma}: {@value} . */
    private static final double DEFAULT_GAMMA = 0.5;
    /** Default value for {@link #sigma}: {@value} . */
    private static final double DEFAULT_SIGMA = 0.5;
    /** Reflection coefficient. */
    private final double rho;
    /** Expansion coefficient. */
    private final double khi;
    /** Contraction coefficient. */
    private final double gamma;
    /** Shrinkage coefficient. */
    private final double sigma;

    /**
     * Build a Nelder-Mead simplex with default coefficients.
     * The default coefficients are 1.0 for rho, 2.0 for khi and 0.5
     * for both gamma and sigma.
     * 
     * @param n
     *        Dimension of the simplex.
     */
    public NelderMeadSimplex(final int n) {
        this(n, 1d);
    }

    /**
     * Build a Nelder-Mead simplex with default coefficients.
     * The default coefficients are 1.0 for rho, 2.0 for khi and 0.5
     * for both gamma and sigma.
     * 
     * @param n
     *        Dimension of the simplex.
     * @param sideLength
     *        Length of the sides of the default (hypercube)
     *        simplex. See {@link AbstractSimplex#AbstractSimplex(int,double)}.
     */
    public NelderMeadSimplex(final int n, final double sideLength) {
        this(n, sideLength,
            DEFAULT_RHO, DEFAULT_KHI, DEFAULT_GAMMA, DEFAULT_SIGMA);
    }

    /**
     * Build a Nelder-Mead simplex with specified coefficients.
     * 
     * @param n
     *        Dimension of the simplex. See {@link AbstractSimplex#AbstractSimplex(int,double)}.
     * @param sideLength
     *        Length of the sides of the default (hypercube)
     *        simplex. See {@link AbstractSimplex#AbstractSimplex(int,double)}.
     * @param rhoIn
     *        Reflection coefficient.
     * @param khiIn
     *        Expansion coefficient.
     * @param gammaIn
     *        Contraction coefficient.
     * @param sigmaIn
     *        Shrinkage coefficient.
     */
    public NelderMeadSimplex(final int n, final double sideLength,
        final double rhoIn, final double khiIn,
        final double gammaIn, final double sigmaIn) {
        super(n, sideLength);

        this.rho = rhoIn;
        this.khi = khiIn;
        this.gamma = gammaIn;
        this.sigma = sigmaIn;
    }

    /**
     * Build a Nelder-Mead simplex with specified coefficients.
     * 
     * @param n
     *        Dimension of the simplex. See {@link AbstractSimplex#AbstractSimplex(int)}.
     * @param rhoIn
     *        Reflection coefficient.
     * @param khiIn
     *        Expansion coefficient.
     * @param gammaIn
     *        Contraction coefficient.
     * @param sigmaIn
     *        Shrinkage coefficient.
     */
    public NelderMeadSimplex(final int n,
        final double rhoIn, final double khiIn,
        final double gammaIn, final double sigmaIn) {
        this(n, 1d, rhoIn, khiIn, gammaIn, sigmaIn);
    }

    /**
     * Build a Nelder-Mead simplex with default coefficients.
     * The default coefficients are 1.0 for rho, 2.0 for khi and 0.5
     * for both gamma and sigma.
     * 
     * @param steps
     *        Steps along the canonical axes representing box edges.
     *        They may be negative but not zero. See
     */
    public NelderMeadSimplex(final double[] steps) {
        this(steps, DEFAULT_RHO, DEFAULT_KHI, DEFAULT_GAMMA, DEFAULT_SIGMA);
    }

    /**
     * Build a Nelder-Mead simplex with specified coefficients.
     * 
     * @param steps
     *        Steps along the canonical axes representing box edges.
     *        They may be negative but not zero. See {@link AbstractSimplex#AbstractSimplex(double[])}.
     * @param rhoIn
     *        Reflection coefficient.
     * @param khiIn
     *        Expansion coefficient.
     * @param gammaIn
     *        Contraction coefficient.
     * @param sigmaIn
     *        Shrinkage coefficient.
     * @throws IllegalArgumentException
     *         if one of the steps is zero.
     */
    public NelderMeadSimplex(final double[] steps,
        final double rhoIn, final double khiIn,
        final double gammaIn, final double sigmaIn) {
        super(steps);

        this.rho = rhoIn;
        this.khi = khiIn;
        this.gamma = gammaIn;
        this.sigma = sigmaIn;
    }

    /**
     * Build a Nelder-Mead simplex with default coefficients.
     * The default coefficients are 1.0 for rho, 2.0 for khi and 0.5
     * for both gamma and sigma.
     * 
     * @param referenceSimplex
     *        Reference simplex. See {@link AbstractSimplex#AbstractSimplex(double[][])}.
     */
    public NelderMeadSimplex(final double[][] referenceSimplex) {
        this(referenceSimplex, DEFAULT_RHO, DEFAULT_KHI, DEFAULT_GAMMA, DEFAULT_SIGMA);
    }

    /**
     * Build a Nelder-Mead simplex with specified coefficients.
     * 
     * @param referenceSimplex
     *        Reference simplex. See {@link AbstractSimplex#AbstractSimplex(double[][])}.
     * @param rhoIn
     *        Reflection coefficient.
     * @param khiIn
     *        Expansion coefficient.
     * @param gammaIn
     *        Contraction coefficient.
     * @param sigmaIn
     *        Shrinkage coefficient.
     * @throws fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException
     *         if the reference simplex does not contain at least one point.
     * @throws fr.cnes.sirius.patrius.math.exception.DimensionMismatchException
     *         if there is a dimension mismatch in the reference simplex.
     */
    public NelderMeadSimplex(final double[][] referenceSimplex,
        final double rhoIn, final double khiIn,
        final double gammaIn, final double sigmaIn) {
        super(referenceSimplex);

        this.rho = rhoIn;
        this.khi = khiIn;
        this.gamma = gammaIn;
        this.sigma = sigmaIn;
    }

    /** {@inheritDoc} */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Commons-Math code kept as such
    @Override
    public void iterate(final MultivariateFunction evaluationFunction,
                        final Comparator<PointValuePair> comparator) {
        // CHECKSTYLE: resume CyclomaticComplexity check
        // The simplex has n + 1 points if dimension is n.
        final int n = this.getDimension();

        // Interesting values.
        final PointValuePair best = this.getPoint(0);
        final PointValuePair secondBest = this.getPoint(n - 1);
        final PointValuePair worst = this.getPoint(n);
        final double[] xWorst = worst.getPointRef();

        // Compute the centroid of the best vertices (dismissing the worst
        // point at index n).
        final double[] centroid = new double[n];
        for (int i = 0; i < n; i++) {
            final double[] x = this.getPoint(i).getPointRef();
            for (int j = 0; j < n; j++) {
                centroid[j] += x[j];
            }
        }
        final double scaling = 1.0 / n;
        for (int j = 0; j < n; j++) {
            centroid[j] *= scaling;
        }

        // compute the reflection point
        final double[] xR = new double[n];
        for (int j = 0; j < n; j++) {
            xR[j] = centroid[j] + this.rho * (centroid[j] - xWorst[j]);
        }
        PointValuePair reflected = new PointValuePair(xR, evaluationFunction.value(xR), false);

        if (comparator.compare(best, reflected) <= 0 &&
            comparator.compare(reflected, secondBest) < 0) {
            // Accept the reflected point.
            reflected = this.replaceWorstPoint(reflected, comparator);
        } else if (comparator.compare(reflected, best) < 0) {
            // Compute the expansion point.
            final double[] xE = new double[n];
            for (int j = 0; j < n; j++) {
                xE[j] = centroid[j] + this.khi * (xR[j] - centroid[j]);
            }
            PointValuePair expanded = new PointValuePair(xE, evaluationFunction.value(xE), false);

            if (comparator.compare(expanded, reflected) < 0) {
                // Accept the expansion point.
                expanded = this.replaceWorstPoint(expanded, comparator);
            } else {
                // Accept the reflected point.
                reflected = this.replaceWorstPoint(reflected, comparator);
            }
        } else {
            if (comparator.compare(reflected, worst) < 0) {
                // Perform an outside contraction.
                final double[] xC = new double[n];
                for (int j = 0; j < n; j++) {
                    xC[j] = centroid[j] + this.gamma * (xR[j] - centroid[j]);
                }
                PointValuePair outContracted = new PointValuePair(xC, evaluationFunction.value(xC), false);
                if (comparator.compare(outContracted, reflected) <= 0) {
                    // Accept the contraction point.
                    outContracted = this.replaceWorstPoint(outContracted, comparator);
                    return;
                }
            } else {
                // Perform an inside contraction.
                final double[] xC = new double[n];
                for (int j = 0; j < n; j++) {
                    xC[j] = centroid[j] - this.gamma * (centroid[j] - xWorst[j]);
                }
                PointValuePair inContracted = new PointValuePair(xC, evaluationFunction.value(xC), false);

                if (comparator.compare(inContracted, worst) < 0) {
                    // Accept the contraction point.
                    inContracted = this.replaceWorstPoint(inContracted, comparator);
                    return;
                }
            }

            // Perform a shrink.
            final double[] xSmallest = this.getPoint(0).getPointRef();
            for (int i = 1; i <= n; i++) {
                final double[] x = this.getPoint(i).getPoint();
                for (int j = 0; j < n; j++) {
                    x[j] = xSmallest[j] + this.sigma * (x[j] - xSmallest[j]);
                }
                this.setPoint(i, new PointValuePair(x, Double.NaN, false));
            }
            this.evaluate(evaluationFunction, comparator);
        }
    }
}
