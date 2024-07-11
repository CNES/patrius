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
package fr.cnes.sirius.patrius.math.optim.nonlinear.scalar;

import fr.cnes.sirius.patrius.math.analysis.MultivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.MultivariateVectorFunction;
import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;

/**
 * This class converts {@link MultivariateVectorFunction vectorial objective functions} to {@link MultivariateFunction
 * scalar objective functions} when the goal is to minimize them. <br/>
 * This class is mostly used when the vectorial objective function represents
 * a theoretical result computed from a point set applied to a model and
 * the models point must be adjusted to fit the theoretical result to some
 * reference observations. The observations may be obtained for example from
 * physical measurements whether the model is built from theoretical
 * considerations. <br/>
 * This class computes a possibly weighted squared sum of the residuals, which is
 * a scalar value. The residuals are the difference between the theoretical model
 * (i.e. the output of the vectorial objective function) and the observations. The
 * class implements the {@link MultivariateFunction} interface and can therefore be
 * minimized by any optimizer supporting scalar objectives functions.This is one way
 * to perform a least square estimation. There are other ways to do this without using
 * this converter, as some optimization algorithms directly support vectorial objective
 * functions. <br/>
 * This class support combination of residuals with or without weights and correlations.
 * 
 * @see MultivariateFunction
 * @see MultivariateVectorFunction
 * @version $Id: LeastSquaresConverter.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 2.0
 */
@SuppressWarnings("PMD.NullAssignment")
public class LeastSquaresConverter implements MultivariateFunction {
    /** Underlying vectorial function. */
    private final MultivariateVectorFunction function;
    /** Observations to be compared to objective function to compute residuals. */
    private final double[] observations;
    /** Optional weights for the residuals. */
    private final double[] weights;
    /** Optional scaling matrix (weight and correlations) for the residuals. */
    private final RealMatrix scale;

    /**
     * Builds a simple converter for uncorrelated residuals with identical
     * weights.
     * 
     * @param functionIn
     *        vectorial residuals function to wrap
     * @param observationsIn
     *        observations to be compared to objective function to compute residuals
     */
    public LeastSquaresConverter(final MultivariateVectorFunction functionIn,
        final double[] observationsIn) {
        this.function = functionIn;
        this.observations = observationsIn.clone();
        this.weights = null;
        this.scale = null;
    }

    /**
     * Builds a simple converter for uncorrelated residuals with the
     * specified weights.
     * <p>
     * The scalar objective function value is computed as:
     * 
     * <pre>
     * objective = &sum;weight<sub>i</sub>(observation<sub>i</sub>-objective<sub>i</sub>)<sup>2</sup>
     * </pre>
     * 
     * </p>
     * <p>
     * Weights can be used for example to combine residuals with different standard deviations. As an example, consider
     * a residuals array in which even elements are angular measurements in degrees with a 0.01&deg; standard deviation
     * and odd elements are distance measurements in meters with a 15m standard deviation. In this case, the weights
     * array should be initialized with value 1.0/(0.01<sup>2</sup>) in the even elements and 1.0/(15.0<sup>2</sup>) in
     * the odd elements (i.e. reciprocals of variances).
     * </p>
     * <p>
     * The array computed by the objective function, the observations array and the weights array must have consistent
     * sizes or a {@link DimensionMismatchException} will be triggered while computing the scalar objective.
     * </p>
     * 
     * @param functionIn
     *        vectorial residuals function to wrap
     * @param observationsIn
     *        observations to be compared to objective function to compute residuals
     * @param weightsIn
     *        weights to apply to the residuals
     * @throws DimensionMismatchException
     *         if the observations vector and the weights
     *         vector dimensions do not match (objective function dimension is checked only when
     *         the {@link #value(double[])} method is called)
     */
    public LeastSquaresConverter(final MultivariateVectorFunction functionIn,
        final double[] observationsIn,
        final double[] weightsIn) {
        if (observationsIn.length != weightsIn.length) {
            throw new DimensionMismatchException(observationsIn.length, weightsIn.length);
        }
        this.function = functionIn;
        this.observations = observationsIn.clone();
        this.weights = weightsIn.clone();
        this.scale = null;
    }

    /**
     * Builds a simple converter for correlated residuals with the
     * specified weights.
     * <p>
     * The scalar objective function value is computed as:
     * 
     * <pre>
     * objective = y<sup>T</sup>y with y = scale&times;(observation-objective)
     * </pre>
     * 
     * </p>
     * <p>
     * The array computed by the objective function, the observations array and the the scaling matrix must have
     * consistent sizes or a {@link DimensionMismatchException} will be triggered while computing the scalar objective.
     * </p>
     * 
     * @param functionIn
     *        vectorial residuals function to wrap
     * @param observationsIn
     *        observations to be compared to objective function to compute residuals
     * @param scaleIn
     *        scaling matrix
     * @throws DimensionMismatchException
     *         if the observations vector and the scale
     *         matrix dimensions do not match (objective function dimension is checked only when
     *         the {@link #value(double[])} method is called)
     */
    public LeastSquaresConverter(final MultivariateVectorFunction functionIn,
        final double[] observationsIn,
        final RealMatrix scaleIn) {
        if (observationsIn.length != scaleIn.getColumnDimension()) {
            throw new DimensionMismatchException(observationsIn.length, scaleIn.getColumnDimension());
        }
        this.function = functionIn;
        this.observations = observationsIn.clone();
        this.weights = null;
        this.scale = scaleIn.copy();
    }

    /** {@inheritDoc} */
    @Override
    public double value(final double[] point) {
        // compute residuals
        final double[] residuals = this.function.value(point);
        if (residuals.length != this.observations.length) {
            throw new DimensionMismatchException(residuals.length, this.observations.length);
        }
        for (int i = 0; i < residuals.length; ++i) {
            residuals[i] -= this.observations[i];
        }

        // compute sum of squares
        double sumSquares = 0;
        if (this.weights != null) {
            // Include weights
            for (int i = 0; i < residuals.length; ++i) {
                final double ri = residuals[i];
                sumSquares += this.weights[i] * ri * ri;
            }
        } else if (this.scale != null) {
            for (final double yi : this.scale.operate(residuals)) {
                sumSquares += yi * yi;
            }
        } else {
            for (final double ri : residuals) {
                sumSquares += ri * ri;
            }
        }

        // Return result
        //
        return sumSquares;
    }
}
