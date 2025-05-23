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
 * VERSION:4.3:DM:DM-2089:15/05/2019:[PATRIUS] passage a Java 8
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.interpolation;

import java.io.Serializable;
import java.util.Arrays;

import fr.cnes.sirius.patrius.math.analysis.polynomials.PolynomialSplineFunction;
import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.NoDataException;
import fr.cnes.sirius.patrius.math.exception.NonMonotonicSequenceException;
import fr.cnes.sirius.patrius.math.exception.NotFiniteNumberException;
import fr.cnes.sirius.patrius.math.exception.NotPositiveException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;
import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;
import fr.cnes.sirius.patrius.math.util.MathArrays;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Implements the <a href="http://en.wikipedia.org/wiki/Local_regression">
 * Local Regression Algorithm</a> (also Loess, Lowess) for interpolation of
 * real univariate functions.
 * <p/>
 * For reference, see <a href="http://www.math.tau.ac.il/~yekutiel/MA seminar/Cleveland 1979.pdf"> William S. Cleveland
 * - Robust Locally Weighted Regression and Smoothing Scatterplots</a>
 * <p/>
 * This class implements both the loess method and serves as an interpolation adapter to it, allowing one to build a
 * spline on the obtained loess fit.
 * 
 * @version $Id: LoessInterpolator.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 2.0
 */
public class LoessInterpolator
    implements UnivariateInterpolator, Serializable {
    /** Default value of the bandwidth parameter. */
    public static final double DEFAULT_BANDWIDTH = 0.3;
    /** Default value of the number of robustness iterations. */
    public static final int DEFAULT_ROBUSTNESS_ITERS = 2;
    /**
     * Default value for accuracy.
     * 
     * @since 2.1
     */
    public static final double DEFAULT_ACCURACY = 1e-12;
     /** Serializable UID. */
    private static final long serialVersionUID = 5204927143605193821L;
    /**
     * The bandwidth parameter: when computing the loess fit at
     * a particular point, this fraction of source points closest
     * to the current point is taken into account for computing
     * a least-squares regression.
     * <p/>
     * A sensible value is usually 0.25 to 0.5.
     */
    private final double bandwidth;
    /**
     * The number of robustness iterations parameter: this many
     * robustness iterations are done.
     * <p/>
     * A sensible value is usually 0 (just the initial fit without any robustness iterations) to 4.
     */
    private final int robustnessIters;
    /**
     * If the median residual at a certain robustness iteration
     * is less than this amount, no more iterations are done.
     */
    private final double accuracy;

    /**
     * Constructs a new {@link LoessInterpolator} with a bandwidth of {@link #DEFAULT_BANDWIDTH},
     * {@link #DEFAULT_ROBUSTNESS_ITERS} robustness iterations
     * and an accuracy of {#link #DEFAULT_ACCURACY}.
     * See {@link #LoessInterpolator(double, int, double)} for an explanation of
     * the parameters.
     */
    public LoessInterpolator() {
        this.bandwidth = DEFAULT_BANDWIDTH;
        this.robustnessIters = DEFAULT_ROBUSTNESS_ITERS;
        this.accuracy = DEFAULT_ACCURACY;
    }

    /**
     * Construct a new {@link LoessInterpolator} with given bandwidth and number of robustness iterations.
     * <p>
     * Calling this constructor is equivalent to calling {link {@link #LoessInterpolator(double, int, double)
     * LoessInterpolator(bandwidth,
     * robustnessIters, LoessInterpolator.DEFAULT_ACCURACY)}
     * </p>
     * 
     * @param bandwidthIn
     *        when computing the loess fit at
     *        a particular point, this fraction of source points closest
     *        to the current point is taken into account for computing
     *        a least-squares regression.<br/>
     *        A sensible value is usually 0.25 to 0.5, the default value is {@link #DEFAULT_BANDWIDTH}.
     * @param robustnessItersIn
     *        This many robustness iterations are done.<br/>
     *        A sensible value is usually 0 (just the initial fit without any
     *        robustness iterations) to 4, the default value is {@link #DEFAULT_ROBUSTNESS_ITERS}.
     * 
     * @see #LoessInterpolator(double, int, double)
     */
    public LoessInterpolator(final double bandwidthIn, final int robustnessItersIn) {
        this(bandwidthIn, robustnessItersIn, DEFAULT_ACCURACY);
    }

    /**
     * Construct a new {@link LoessInterpolator} with given bandwidth, number of robustness iterations and accuracy.
     * 
     * @param bandwidthIn
     *        when computing the loess fit at
     *        a particular point, this fraction of source points closest
     *        to the current point is taken into account for computing
     *        a least-squares regression.<br/>
     *        A sensible value is usually 0.25 to 0.5, the default value is {@link #DEFAULT_BANDWIDTH}.
     * @param robustnessItersIn
     *        This many robustness iterations are done.<br/>
     *        A sensible value is usually 0 (just the initial fit without any
     *        robustness iterations) to 4, the default value is {@link #DEFAULT_ROBUSTNESS_ITERS}.
     * @param accuracyIn
     *        If the median residual at a certain robustness iteration
     *        is less than this amount, no more iterations are done.
     * @throws OutOfRangeException
     *         if bandwidth does not lie in the interval [0,1].
     * @throws NotPositiveException
     *         if {@code robustnessIters} is negative.
     * @see #LoessInterpolator(double, int)
     * @since 2.1
     */
    public LoessInterpolator(final double bandwidthIn, final int robustnessItersIn, final double accuracyIn) {
        if (bandwidthIn < 0 ||
            bandwidthIn > 1) {
            throw new OutOfRangeException(PatriusMessages.BANDWIDTH, bandwidthIn, 0, 1);
        }
        this.bandwidth = bandwidthIn;
        if (robustnessItersIn < 0) {
            throw new NotPositiveException(PatriusMessages.ROBUSTNESS_ITERATIONS, robustnessItersIn);
        }
        this.robustnessIters = robustnessItersIn;
        this.accuracy = accuracyIn;
    }

    /**
     * Compute an interpolating function by performing a loess fit
     * on the data at the original abscissae and then building a cubic spline
     * with a {@link fr.cnes.sirius.patrius.math.analysis.interpolation.SplineInterpolator} on the resulting fit.
     * 
     * @param xval
     *        the arguments for the interpolation points
     * @param yval
     *        the values for the interpolation points
     * @return A cubic spline built upon a loess fit to the data at the original abscissae
     * @throws NonMonotonicSequenceException
     *         if {@code xval} not sorted in
     *         strictly increasing order.
     * @throws DimensionMismatchException
     *         if {@code xval} and {@code yval} have
     *         different sizes.
     * @throws NoDataException
     *         if {@code xval} or {@code yval} has zero size.
     * @throws NotFiniteNumberException
     *         if any of the arguments and values are
     *         not finite real numbers.
     * @throws NumberIsTooSmallException
     *         if the bandwidth is too small to
     *         accomodate the size of the input data (i.e. the bandwidth must be
     *         larger than 2/n).
     */
    @Override
    public final PolynomialSplineFunction interpolate(final double[] xval,
                                                      final double[] yval) {
        return new SplineInterpolator().interpolate(xval, this.smooth(xval, yval));
    }

    /**
     * Compute a weighted loess fit on the data at the original abscissae.
     * 
     * @param xval
     *        Arguments for the interpolation points.
     * @param yval
     *        Values for the interpolation points.
     * @param weights
     *        point weights: coefficients by which the robustness weight
     *        of a point is multiplied.
     * @return the values of the loess fit at corresponding original abscissae.
     * @throws NonMonotonicSequenceException
     *         if {@code xval} not sorted in
     *         strictly increasing order.
     * @throws DimensionMismatchException
     *         if {@code xval} and {@code yval} have
     *         different sizes.
     * @throws NoDataException
     *         if {@code xval} or {@code yval} has zero size.
     * @throws NotFiniteNumberException
     *         if any of the arguments and values are
     *         not finite real numbers.
     * @throws NumberIsTooSmallException
     *         if the bandwidth is too small to
     *         accomodate the size of the input data (i.e. the bandwidth must be
     *         larger than 2/n).
     * @since 2.1
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // CHECKSTYLE: stop ReturnCount check
    // CHECKSTYLE: stop MethodLength check
    // Reason: Commons-Math code kept as such
    public final double[] smooth(final double[] xval, final double[] yval,
                                 final double[] weights) {
        // CHECKSTYLE: resume MethodLength check
        // CHECKSTYLE: resume ReturnCount check
        // CHECKSTYLE: resume CyclomaticComplexity check
        if (xval.length != yval.length) {
            throw new DimensionMismatchException(xval.length, yval.length);
        }

        final int n = xval.length;

        if (n == 0) {
            throw new NoDataException();
        }

        checkAllFiniteReal(xval);
        checkAllFiniteReal(yval);
        checkAllFiniteReal(weights);

        MathArrays.checkOrder(xval);

        if (n == 1) {
            return new double[] { yval[0] };
        }

        if (n == 2) {
            return new double[] { yval[0], yval[1] };
        }

        final int bandwidthInPoints = (int) (this.bandwidth * n);

        if (bandwidthInPoints < 2) {
            throw new NumberIsTooSmallException(PatriusMessages.BANDWIDTH,
                bandwidthInPoints, 2, true);
        }

        final double[] res = new double[n];

        final double[] residuals = new double[n];
        final double[] sortedResiduals = new double[n];

        final double[] robustnessWeights = new double[n];

        // Do an initial fit and 'robustnessIters' robustness iterations.
        // This is equivalent to doing 'robustnessIters+1' robustness iterations
        // starting with all robustness weights set to 1.
        Arrays.fill(robustnessWeights, 1);

        for (int iter = 0; iter <= this.robustnessIters; ++iter) {
            final int[] bandwidthInterval = { 0, bandwidthInPoints - 1 };
            // At each x, compute a local weighted linear regression
            for (int i = 0; i < n; ++i) {
                final double x = xval[i];

                // Find out the interval of source points on which
                // a regression is to be made.
                if (i > 0) {
                    updateBandwidthInterval(xval, weights, i, bandwidthInterval);
                }

                final int ileft = bandwidthInterval[0];
                final int iright = bandwidthInterval[1];

                // Compute the point of the bandwidth interval that is
                // farthest from x
                final int edge;
                if (xval[i] - xval[ileft] > xval[iright] - xval[i]) {
                    edge = ileft;
                } else {
                    edge = iright;
                }

                // Compute a least-squares linear fit weighted by
                // the product of robustness weights and the tricube
                // weight function.
                // See http://en.wikipedia.org/wiki/Linear_regression
                // (section "Univariate linear case")
                // and http://en.wikipedia.org/wiki/Weighted_least_squares
                // (section "Weighted least squares")
                double sumWeights = 0;
                double sumX = 0;
                double sumXSquared = 0;
                double sumY = 0;
                double sumXY = 0;
                final double denom = MathLib.abs(1.0 / (xval[edge] - x));
                for (int k = ileft; k <= iright; ++k) {
                    final double xk = xval[k];
                    final double yk = yval[k];
                    final double dist = (k < i) ? x - xk : xk - x;
                    final double w = tricube(dist * denom) * robustnessWeights[k] * weights[k];
                    final double xkw = xk * w;
                    sumWeights += w;
                    sumX += xkw;
                    sumXSquared += xk * xkw;
                    sumY += yk * w;
                    sumXY += yk * xkw;
                }

                final double meanX = sumX / sumWeights;
                final double meanY = sumY / sumWeights;
                final double meanXY = sumXY / sumWeights;
                final double meanXSquared = sumXSquared / sumWeights;

                final double beta;
                if (MathLib.sqrt(MathLib.abs(meanXSquared - meanX * meanX)) < this.accuracy) {
                    beta = 0;
                } else {
                    beta = (meanXY - meanX * meanY) / (meanXSquared - meanX * meanX);
                }

                final double alpha = meanY - beta * meanX;

                res[i] = beta * x + alpha;
                residuals[i] = MathLib.abs(yval[i] - res[i]);
            }

            // No need to recompute the robustness weights at the last
            // iteration, they won't be needed anymore
            if (iter == this.robustnessIters) {
                break;
            }

            // Recompute the robustness weights.

            // Find the median residual.
            // An arraycopy and a sort are completely tractable here,
            // because the preceding loop is a lot more expensive
            System.arraycopy(residuals, 0, sortedResiduals, 0, n);
            Arrays.sort(sortedResiduals);
            final double medianResidual = sortedResiduals[n / 2];

            if (MathLib.abs(medianResidual) < this.accuracy) {
                break;
            }

            for (int i = 0; i < n; ++i) {
                final double arg = residuals[i] / (6 * medianResidual);
                if (arg >= 1) {
                    robustnessWeights[i] = 0;
                } else {
                    final double w = 1 - arg * arg;
                    robustnessWeights[i] = w * w;
                }
            }
        }

        return res;
    }

    /**
     * Compute a loess fit on the data at the original abscissae.
     * 
     * @param xval
     *        the arguments for the interpolation points
     * @param yval
     *        the values for the interpolation points
     * @return values of the loess fit at corresponding original abscissae
     * @throws NonMonotonicSequenceException
     *         if {@code xval} not sorted in
     *         strictly increasing order.
     * @throws DimensionMismatchException
     *         if {@code xval} and {@code yval} have
     *         different sizes.
     * @throws NoDataException
     *         if {@code xval} or {@code yval} has zero size.
     * @throws NotFiniteNumberException
     *         if any of the arguments and values are
     *         not finite real numbers.
     * @throws NumberIsTooSmallException
     *         if the bandwidth is too small to
     *         accomodate the size of the input data (i.e. the bandwidth must be
     *         larger than 2/n).
     */
    public final double[] smooth(final double[] xval, final double[] yval) {
        if (xval.length != yval.length) {
            throw new DimensionMismatchException(xval.length, yval.length);
        }

        final double[] unitWeights = new double[xval.length];
        Arrays.fill(unitWeights, 1.0);

        return this.smooth(xval, yval, unitWeights);
    }

    /**
     * Given an index interval into xval that embraces a certain number of
     * points closest to {@code xval[i-1]}, update the interval so that it
     * embraces the same number of points closest to {@code xval[i]},
     * ignoring zero weights.
     * 
     * @param xval
     *        Arguments array.
     * @param weights
     *        Weights array.
     * @param i
     *        Index around which the new interval should be computed.
     * @param bandwidthInterval
     *        a two-element array {left, right} such that:
     *        {@code (left==0 or xval[i] - xval[left-1] > xval[right] - xval[i])} and
     *        {@code (right==xval.length-1 or xval[right+1] - xval[i] > xval[i] - xval[left])}.
     *        The array will be updated.
     */
    private static void updateBandwidthInterval(final double[] xval, final double[] weights,
                                                final int i,
                                                final int[] bandwidthInterval) {
        final int left = bandwidthInterval[0];
        final int right = bandwidthInterval[1];

        // The right edge should be adjusted if the next point to the right
        // is closer to xval[i] than the leftmost point of the current interval
        final int nextRight = nextNonzero(weights, right);
        if (nextRight < xval.length && xval[nextRight] - xval[i] < xval[i] - xval[left]) {
            final int nextLeft = nextNonzero(weights, bandwidthInterval[0]);
            bandwidthInterval[0] = nextLeft;
            bandwidthInterval[1] = nextRight;
        }
    }

    /**
     * Return the smallest index {@code j} such that {@code j > i && (j == weights.length || weights[j] != 0)}.
     * 
     * @param weights
     *        Weights array.
     * @param i
     *        Index from which to start search.
     * @return the smallest compliant index.
     */
    private static int nextNonzero(final double[] weights, final int i) {
        int j = i + 1;
        while (j < weights.length && weights[j] == 0) {
            ++j;
        }
        return j;
    }

    /**
     * Compute the
     * <a href="http://en.wikipedia.org/wiki/Local_regression#Weight_function">tricube</a>
     * weight function
     * 
     * @param x
     *        Argument.
     * @return <code>(1 - |x|<sup>3</sup>)<sup>3</sup></code> for |x| &lt; 1, 0 otherwise.
     */
    private static double tricube(final double x) {
        final double absX = MathLib.abs(x);
        if (absX >= 1.0) {
            return 0.0;
        }
        final double tmp = 1 - absX * absX * absX;
        return tmp * tmp * tmp;
    }

    /**
     * Check that all elements of an array are finite real numbers.
     * 
     * @param values
     *        Values array.
     * @throws fr.cnes.sirius.patrius.math.exception.NotFiniteNumberException
     *         if one of the values is not a finite real number.
     */
    private static void checkAllFiniteReal(final double[] values) {
        for (final double value : values) {
            MathUtils.checkFinite(value);
        }
    }
}
