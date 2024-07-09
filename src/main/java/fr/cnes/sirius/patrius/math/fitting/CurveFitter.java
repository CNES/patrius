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
package fr.cnes.sirius.patrius.math.fitting;

import java.util.ArrayList;
import java.util.List;

import fr.cnes.sirius.patrius.math.analysis.MultivariateMatrixFunction;
import fr.cnes.sirius.patrius.math.analysis.MultivariateVectorFunction;
import fr.cnes.sirius.patrius.math.analysis.ParametricUnivariateFunction;
import fr.cnes.sirius.patrius.math.optim.InitialGuess;
import fr.cnes.sirius.patrius.math.optim.MaxEval;
import fr.cnes.sirius.patrius.math.optim.PointVectorValuePair;
import fr.cnes.sirius.patrius.math.optim.nonlinear.vector.ModelFunction;
import fr.cnes.sirius.patrius.math.optim.nonlinear.vector.ModelFunctionJacobian;
import fr.cnes.sirius.patrius.math.optim.nonlinear.vector.MultivariateVectorOptimizer;
import fr.cnes.sirius.patrius.math.optim.nonlinear.vector.Target;
import fr.cnes.sirius.patrius.math.optim.nonlinear.vector.Weight;

/**
 * Fitter for parametric univariate real functions y = f(x). <br/>
 * When a univariate real function y = f(x) does depend on some
 * unknown parameters p<sub>0</sub>, p<sub>1</sub> ... p<sub>n-1</sub>,
 * this class can be used to find these parameters. It does this
 * by <em>fitting</em> the curve so it remains very close to a set of
 * observed points (x<sub>0</sub>, y<sub>0</sub>), (x<sub>1</sub>,
 * y<sub>1</sub>) ... (x<sub>k-1</sub>, y<sub>k-1</sub>). This fitting
 * is done by finding the parameters values that minimizes the objective
 * function &sum;(y<sub>i</sub>-f(x<sub>i</sub>))<sup>2</sup>. This is
 * really a least squares problem.
 * 
 * @param <T>
 *        Function to use for the fit.
 * 
 * @version $Id: CurveFitter.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 2.0
 */
public class CurveFitter<T extends ParametricUnivariateFunction> {
    /** Optimizer to use for the fitting. */
    private final MultivariateVectorOptimizer optimizer;
    /** Observed points. */
    private final List<WeightedObservedPoint> observations;

    /**
     * Simple constructor.
     * 
     * @param optimizerIn
     *        Optimizer to use for the fitting.
     * @since 3.1
     */
    public CurveFitter(final MultivariateVectorOptimizer optimizerIn) {
        this.optimizer = optimizerIn;
        this.observations = new ArrayList<WeightedObservedPoint>();
    }

    /**
     * Add an observed (x,y) point to the sample with unit weight.
     * <p>
     * Calling this method is equivalent to call {@code addObservedPoint(1.0, x, y)}.
     * </p>
     * 
     * @param x
     *        abscissa of the point
     * @param y
     *        observed value of the point at x, after fitting we should
     *        have f(x) as close as possible to this value
     * @see #addObservedPoint(double, double, double)
     * @see #addObservedPoint(WeightedObservedPoint)
     * @see #getObservations()
     */
    public void addObservedPoint(final double x, final double y) {
        this.addObservedPoint(1.0, x, y);
    }

    /**
     * Add an observed weighted (x,y) point to the sample.
     * 
     * @param weight
     *        weight of the observed point in the fit
     * @param x
     *        abscissa of the point
     * @param y
     *        observed value of the point at x, after fitting we should
     *        have f(x) as close as possible to this value
     * @see #addObservedPoint(double, double)
     * @see #addObservedPoint(WeightedObservedPoint)
     * @see #getObservations()
     */
    public void addObservedPoint(final double weight, final double x, final double y) {
        this.observations.add(new WeightedObservedPoint(weight, x, y));
    }

    /**
     * Add an observed weighted (x,y) point to the sample.
     * 
     * @param observed
     *        observed point to add
     * @see #addObservedPoint(double, double)
     * @see #addObservedPoint(double, double, double)
     * @see #getObservations()
     */
    public void addObservedPoint(final WeightedObservedPoint observed) {
        this.observations.add(observed);
    }

    /**
     * Get the observed points.
     * 
     * @return observed points
     * @see #addObservedPoint(double, double)
     * @see #addObservedPoint(double, double, double)
     * @see #addObservedPoint(WeightedObservedPoint)
     */
    public WeightedObservedPoint[] getObservations() {
        return this.observations.toArray(new WeightedObservedPoint[this.observations.size()]);
    }

    /**
     * Remove all observations.
     */
    public void clearObservations() {
        this.observations.clear();
    }

    /**
     * Fit a curve.
     * This method compute the coefficients of the curve that best
     * fit the sample of observed points previously given through calls
     * to the {@link #addObservedPoint(WeightedObservedPoint)
     * addObservedPoint} method.
     * 
     * @param f
     *        parametric function to fit.
     * @param initialGuess
     *        first guess of the function parameters.
     * @return the fitted parameters.
     * @throws fr.cnes.sirius.patrius.math.exception.DimensionMismatchException
     *         if the start point dimension is wrong.
     */
    public double[] fit(final T f, final double[] initialGuess) {
        return this.fit(Integer.MAX_VALUE, f, initialGuess);
    }

    /**
     * Fit a curve.
     * This method compute the coefficients of the curve that best
     * fit the sample of observed points previously given through calls
     * to the {@link #addObservedPoint(WeightedObservedPoint)
     * addObservedPoint} method.
     * 
     * @param f
     *        parametric function to fit.
     * @param initialGuess
     *        first guess of the function parameters.
     * @param maxEval
     *        Maximum number of function evaluations.
     * @return the fitted parameters.
     * @throws fr.cnes.sirius.patrius.math.exception.TooManyEvaluationsException
     *         if the number of allowed evaluations is exceeded.
     * @throws fr.cnes.sirius.patrius.math.exception.DimensionMismatchException
     *         if the start point dimension is wrong.
     * @since 3.0
     */
    public double[] fit(final int maxEval, final T f,
                        final double[] initialGuess) {
        // Prepare least squares problem.
        final double[] target = new double[this.observations.size()];
        final double[] weights = new double[this.observations.size()];
        int i = 0;
        for (final WeightedObservedPoint point : this.observations) {
            target[i] = point.getY();
            weights[i] = point.getWeight();
            ++i;
        }

        // Input to the optimizer: the model and its Jacobian.
        final TheoreticalValuesFunction model = new TheoreticalValuesFunction(f);

        // Perform the fit.
        final PointVectorValuePair optimum = this.optimizer.optimize(new MaxEval(maxEval),
            model.getModelFunction(),
            model.getModelFunctionJacobian(),
            new Target(target),
            new Weight(weights),
            new InitialGuess(initialGuess));
        // Extract the coefficients.
        return optimum.getPointRef();
    }

    /** Vectorial function computing function theoretical values. */
    private class TheoreticalValuesFunction {
        /** Function to fit. */
        private final ParametricUnivariateFunction f;

        /**
         * @param fIn
         *        function to fit.
         */
        public TheoreticalValuesFunction(final ParametricUnivariateFunction fIn) {
            this.f = fIn;
        }

        /**
         * @return the model function values.
         */
        public ModelFunction getModelFunction() {
            return new ModelFunction(new MultivariateVectorFunction(){
                /** {@inheritDoc} */
                @Override
                public double[] value(final double[] point) {
                    // compute the residuals
                    final double[] values = new double[CurveFitter.this.observations.size()];
                    int i = 0;
                    for (final WeightedObservedPoint observed : CurveFitter.this.observations) {
                        values[i++] = TheoreticalValuesFunction.this.f.value(observed.getX(), point);
                    }

                    return values;
                }
            });
        }

        /**
         * @return the model function Jacobian.
         */
        public ModelFunctionJacobian getModelFunctionJacobian() {
            return new ModelFunctionJacobian(new MultivariateMatrixFunction(){
                /** {@inheritDoc} */
                @Override
                public double[][] value(final double[] point) {
                    final double[][] jacobian = new double[CurveFitter.this.observations.size()][];
                    int i = 0;
                    for (final WeightedObservedPoint observed : CurveFitter.this.observations) {
                        jacobian[i++] = TheoreticalValuesFunction.this.f.gradient(observed.getX(), point);
                    }
                    return jacobian;
                }
            });
        }
    }
}
