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
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.fitting;

import java.util.Arrays;
import java.util.Comparator;

import fr.cnes.sirius.patrius.math.analysis.function.Gaussian;
import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;
import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;
import fr.cnes.sirius.patrius.math.exception.ZeroException;
import fr.cnes.sirius.patrius.math.optim.nonlinear.vector.MultivariateVectorOptimizer;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Fits points to a {@link fr.cnes.sirius.patrius.math.analysis.function.Gaussian.Parametric Gaussian} function.
 * <p>
 * Usage example:
 * 
 * <pre>
 * GaussianFitter fitter = new GaussianFitter(
 *     new LevenbergMarquardtOptimizer());
 * fitter.addObservedPoint(4.0254623, 531026.0);
 * fitter.addObservedPoint(4.03128248, 984167.0);
 * fitter.addObservedPoint(4.03839603, 1887233.0);
 * fitter.addObservedPoint(4.04421621, 2687152.0);
 * fitter.addObservedPoint(4.05132976, 3461228.0);
 * fitter.addObservedPoint(4.05326982, 3580526.0);
 * fitter.addObservedPoint(4.05779662, 3439750.0);
 * fitter.addObservedPoint(4.0636168, 2877648.0);
 * fitter.addObservedPoint(4.06943698, 2175960.0);
 * fitter.addObservedPoint(4.07525716, 1447024.0);
 * fitter.addObservedPoint(4.08237071, 717104.0);
 * fitter.addObservedPoint(4.08366408, 620014.0);
 * double[] parameters = fitter.fit();
 * </pre>
 * 
 * @since 2.2
 * @version $Id: GaussianFitter.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class GaussianFitter extends CurveFitter<Gaussian.Parametric> {
    /**
     * Constructs an instance using the specified optimizer.
     * 
     * @param optimizer
     *        Optimizer to use for the fitting.
     */
    public GaussianFitter(final MultivariateVectorOptimizer optimizer) {
        super(optimizer);
    }

    /**
     * Fits a Gaussian function to the observed points.
     * 
     * @param initialGuess
     *        First guess values in the following order:
     *        <ul>
     *        <li>Norm</li>
     *        <li>Mean</li>
     *        <li>Sigma</li>
     *        </ul>
     * @return the parameters of the Gaussian function that best fits the
     *         observed points (in the same order as above).
     * @since 3.0
     */
    @SuppressWarnings("PMD.EmptyCatchBlock")
    public double[] fit(final double[] initialGuess) {
        final Gaussian.Parametric f = new Gaussian.Parametric(){
            /** {@inheritDoc} */
            @Override
            public double value(final double x, final double... p) {
                double v = Double.POSITIVE_INFINITY;
                try {
                    v = super.value(x, p);
                } catch (final NotStrictlyPositiveException e) {
                    // Do nothing
                }
                return v;
            }

            /** {@inheritDoc} */
            @Override
            public double[] gradient(final double x, final double... p) {
                double[] v = { Double.POSITIVE_INFINITY,
                    Double.POSITIVE_INFINITY,
                    Double.POSITIVE_INFINITY };
                try {
                    v = super.gradient(x, p);
                } catch (final NotStrictlyPositiveException e) {
                    // NOPMD
                    // Do nothing.
                }
                return v;
            }
        };

        return this.fit(f, initialGuess);
    }

    /**
     * Fits a Gaussian function to the observed points.
     * 
     * @return the parameters of the Gaussian function that best fits the
     *         observed points (in the same order as above).
     */
    public double[] fit() {
        final double[] guess = (new ParameterGuesser(this.getObservations())).guess();
        return this.fit(guess);
    }

    /**
     * Guesses the parameters {@code norm}, {@code mean}, and {@code sigma} of a
     * {@link fr.cnes.sirius.patrius.math.analysis.function.Gaussian.Parametric} based on the specified observed points.
     */
    public static class ParameterGuesser {
        /** Normalization factor. */
        private final double norm;
        /** Mean. */
        private final double mean;
        /** Standard deviation. */
        private final double sigma;

        /**
         * Constructs instance with the specified observed points.
         * 
         * @param observations
         *        Observed points from which to guess the
         *        parameters of the Gaussian.
         * @throws NullArgumentException
         *         if {@code observations} is {@code null}.
         * @throws NumberIsTooSmallException
         *         if there are less than 3
         *         observations.
         */
        public ParameterGuesser(final WeightedObservedPoint[] observations) {
            if (observations == null) {
                throw new NullArgumentException(PatriusMessages.INPUT_ARRAY);
            }
            if (observations.length < 3) {
                throw new NumberIsTooSmallException(observations.length, 3, true);
            }

            final WeightedObservedPoint[] sorted = sortObservations(observations);
            final double[] params = basicGuess(sorted);

            this.norm = params[0];
            this.mean = params[1];
            this.sigma = params[2];
        }

        /**
         * Gets an estimation of the parameters.
         * 
         * @return the guessed parameters, in the following order:
         *         <ul>
         *         <li>Normalization factor</li>
         *         <li>Mean</li>
         *         <li>Standard deviation</li>
         *         </ul>
         */
        public double[] guess() {
            return new double[] { this.norm, this.mean, this.sigma };
        }

        /**
         * Sort the observations.
         * 
         * @param unsorted
         *        Input observations.
         * @return the input observations, sorted.
         */
        private static WeightedObservedPoint[] sortObservations(final WeightedObservedPoint[] unsorted) {
            final WeightedObservedPoint[] observations = unsorted.clone();
            final Comparator<WeightedObservedPoint> cmp = new Comparator<WeightedObservedPoint>(){
                // CHECKSTYLE: stop CyclomaticComplexity check
                // CHECKSTYLE: stop ReturnCount check
                // Reason: Commons-Math code kept as such
                /** {@inheritDoc} */
                @Override
                public int compare(final WeightedObservedPoint p1,
                                   final WeightedObservedPoint p2) {
                    // CHECKSTYLE: resume CyclomaticComplexity check
                    // CHECKSTYLE: resume ReturnCount check

                    // Many specific cases
                    if (p1 == null && p2 == null) {
                        return 0;
                    }
                    if (p1 == null) {
                        return -1;
                    }
                    if (p2 == null) {
                        return 1;
                    }
                    if (p1.getX() < p2.getX()) {
                        return -1;
                    }
                    if (p1.getX() > p2.getX()) {
                        return 1;
                    }
                    if (p1.getY() < p2.getY()) {
                        return -1;
                    }
                    if (p1.getY() > p2.getY()) {
                        return 1;
                    }
                    if (p1.getWeight() < p2.getWeight()) {
                        return -1;
                    }
                    if (p1.getWeight() > p2.getWeight()) {
                        return 1;
                    }
                    // "General" case
                    //
                    return 0;
                }
            };

            Arrays.sort(observations, cmp);
            return observations;
        }

        /**
         * Guesses the parameters based on the specified observed points.
         * 
         * @param points
         *        Observed points, sorted.
         * @return the guessed parameters (normalization factor, mean and
         *         sigma).
         */
        private static double[] basicGuess(final WeightedObservedPoint[] points) {
            // Get Max xy
            final int maxYIdx = findMaxY(points);
            final double n = points[maxYIdx].getY();
            final double m = points[maxYIdx].getX();

            double fwhmApprox;
            try {
                final double halfY = n + ((m - n) / 2);
                final double fwhmX1 = interpolateXAtY(points, maxYIdx, -1, halfY);
                final double fwhmX2 = interpolateXAtY(points, maxYIdx, 1, halfY);
                fwhmApprox = fwhmX2 - fwhmX1;
            } catch (final OutOfRangeException e) {
                fwhmApprox = points[points.length - 1].getX() - points[0].getX();
            }
            // COmpute s
            final double s = fwhmApprox / (2 * MathLib.sqrt(2 * MathLib.log(2)));

            // Return result
            return new double[] { n, m, s };
        }

        /**
         * Finds index of point in specified points with the largest Y.
         * 
         * @param points
         *        Points to search.
         * @return the index in specified points array.
         */
        private static int findMaxY(final WeightedObservedPoint[] points) {
            int maxYIdx = 0;
            for (int i = 1; i < points.length; i++) {
                if (points[i].getY() > points[maxYIdx].getY()) {
                    maxYIdx = i;
                }
            }
            return maxYIdx;
        }

        /**
         * Interpolates using the specified points to determine X at the
         * specified Y.
         * 
         * @param points
         *        Points to use for interpolation.
         * @param startIdx
         *        Index within points from which to start the search for
         *        interpolation bounds points.
         * @param idxStep
         *        Index step for searching interpolation bounds points.
         * @param y
         *        Y value for which X should be determined.
         * @return the value of X for the specified Y.
         * @throws ZeroException
         *         if {@code idxStep} is 0.
         * @throws OutOfRangeException
         *         if specified {@code y} is not within the
         *         range of the specified {@code points}.
         */
        private static double interpolateXAtY(final WeightedObservedPoint[] points,
                                       final int startIdx,
                                       final int idxStep,
                                       final double y) {
            if (idxStep == 0) {
                // Null step index exception
                throw new ZeroException();
            }
            // Get interpolation points for startIdx, idxStep and y
            final WeightedObservedPoint[] twoPoints = getInterpolationPointsForY(points, startIdx, idxStep, y);
            // Separate weighed observed points
            final WeightedObservedPoint p1 = twoPoints[0];
            final WeightedObservedPoint p2 = twoPoints[1];
            final double res;
            if (p1.getY() == y) {
                res = p1.getX();
            } else if (p2.getY() == y) {
                res = p2.getX();
            } else {
                // Compute the value of X for y
                res = p1.getX() + (((y - p1.getY()) * (p2.getX() - p1.getX())) /
                    (p2.getY() - p1.getY()));
            }
            return res;
        }

        /**
         * Gets the two bounding interpolation points from the specified points
         * suitable for determining X at the specified Y.
         * 
         * @param points
         *        Points to use for interpolation.
         * @param startIdx
         *        Index within points from which to start search for
         *        interpolation bounds points.
         * @param idxStep
         *        Index step for search for interpolation bounds points.
         * @param y
         *        Y value for which X should be determined.
         * @return the array containing two points suitable for determining X at
         *         the specified Y.
         * @throws ZeroException
         *         if {@code idxStep} is 0.
         * @throws OutOfRangeException
         *         if specified {@code y} is not within the
         *         range of the specified {@code points}.
         */
        private static WeightedObservedPoint[] getInterpolationPointsForY(final WeightedObservedPoint[] points,
                                                                   final int startIdx,
                                                                   final int idxStep,
                                                                   final double y) {
            if (idxStep == 0) {
                // Immediate return
                throw new ZeroException();
            }
            for (int i = startIdx; idxStep < 0 ? i + idxStep >= 0 : i + idxStep < points.length; i += idxStep) {
                final WeightedObservedPoint p1 = points[i];
                final WeightedObservedPoint p2 = points[i + idxStep];
                if (isBetween(y, p1.getY(), p2.getY())) {
                    // Return weighted observed points in right order
                    if (idxStep < 0) {
                        return new WeightedObservedPoint[] { p2, p1 };
                    }
                    return new WeightedObservedPoint[] { p1, p2 };
                }
            }

            // Boundaries are replaced by dummy values because the raised
            // exception is caught and the message never displayed.
            throw new OutOfRangeException(y,
                Double.NEGATIVE_INFINITY,
                Double.POSITIVE_INFINITY);
        }

        /**
         * Determines whether a value is between two other values.
         * 
         * @param value
         *        Value to test whether it is between {@code boundary1} and {@code boundary2}.
         * @param boundary1
         *        One end of the range.
         * @param boundary2
         *        Other end of the range.
         * @return {@code true} if {@code value} is between {@code boundary1} and {@code boundary2} (inclusive),
         *         {@code false} otherwise.
         */
        private static boolean isBetween(final double value,
                                  final double boundary1,
                                  final double boundary2) {
            return (value >= boundary1 && value <= boundary2) ||
                (value >= boundary2 && value <= boundary1);
        }
    }
}
