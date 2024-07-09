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
package fr.cnes.sirius.patrius.math.analysis.integration;

import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.solver.UnivariateSolverUtils;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.exception.MaxCountExceededException;
import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;
import fr.cnes.sirius.patrius.math.exception.TooManyEvaluationsException;
import fr.cnes.sirius.patrius.math.util.Incrementor;
import fr.cnes.sirius.patrius.math.util.MathUtils;

/**
 * Provide a default implementation for several generic functions.
 * 
 * @version $Id: BaseAbstractUnivariateIntegrator.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 1.2
 */
//CHECKSTYLE: stop AbstractClassName check
@SuppressWarnings("PMD.AbstractNaming")
public abstract class BaseAbstractUnivariateIntegrator implements UnivariateIntegrator {
    // CHECKSTYLE: resume AbstractClassName check

    /** Default absolute accuracy. */
    public static final double DEFAULT_ABSOLUTE_ACCURACY = 1.0e-15;

    /** Default relative accuracy. */
    public static final double DEFAULT_RELATIVE_ACCURACY = 1.0e-6;

    /** Default minimal iteration count. */
    public static final int DEFAULT_MIN_ITERATIONS_COUNT = 3;

    /** Default maximal iteration count. */
    public static final int DEFAULT_MAX_ITERATIONS_COUNT = Integer.MAX_VALUE;

    /** Serial UID. */
    private static final long serialVersionUID = 5198557347678587098L;

    /** The iteration count. */
    protected final Incrementor iterations;

    /** Maximum absolute error. */
    private final double absoluteAccuracy;

    /** Maximum relative error. */
    private final double relativeAccuracy;

    /** minimum number of iterations */
    private final int minimalIterationCount;

    /** The functions evaluation count. */
    private final Incrementor evaluations;

    /** Function to integrate. */
    private UnivariateFunction function;

    /** Lower bound for the interval. */
    private double min;

    /** Upper bound for the interval. */
    private double max;

    /**
     * Construct an integrator with given accuracies and iteration counts.
     * <p>
     * The meanings of the various parameters are:
     * <ul>
     * <li>relative accuracy: this is used to stop iterations if the absolute accuracy can't be achieved due to large
     * values or short mantissa length. If this should be the primary criterion for convergence rather then a safety
     * measure, set the absolute accuracy to a ridiculously small value, like
     * {@link fr.cnes.sirius.patrius.math.util.Precision#SAFE_MIN Precision.SAFE_MIN}.</li>
     * <li>absolute accuracy: The default is usually chosen so that results in the interval -10..-0.1 and +0.1..+10 can
     * be found with a reasonable accuracy. If the expected absolute value of your results is of much smaller magnitude,
     * set this to a smaller value.</li>
     * <li>minimum number of iterations: minimal iteration is needed to avoid false early convergence, e.g. the sample
     * points happen to be zeroes of the function. Users can use the default value or choose one that they see as
     * appropriate.</li>
     * <li>maximum number of iterations: usually a high iteration count indicates convergence problems. However, the
     * "reasonable value" varies widely for different algorithms. Users are advised to use the default value supplied by
     * the algorithm.</li>
     * </ul>
     * </p>
     * 
     * @param relativeAccuracyIn
     *        relative accuracy of the result
     * @param absoluteAccuracyIn
     *        absolute accuracy of the result
     * @param minimalIterationCountIn
     *        minimum number of iterations
     * @param maximalIterationCount
     *        maximum number of iterations
     * @exception NotStrictlyPositiveException
     *            if minimal number of iterations
     *            is not strictly positive
     * @exception NumberIsTooSmallException
     *            if maximal number of iterations
     *            is lesser than or equal to the minimal number of iterations
     */
    protected BaseAbstractUnivariateIntegrator(final double relativeAccuracyIn,
        final double absoluteAccuracyIn,
        final int minimalIterationCountIn,
        final int maximalIterationCount) {

        // accuracy settings
        this.relativeAccuracy = relativeAccuracyIn;
        this.absoluteAccuracy = absoluteAccuracyIn;

        // iterations count settings
        if (minimalIterationCountIn <= 0) {
            throw new NotStrictlyPositiveException(minimalIterationCountIn);
        }
        if (maximalIterationCount <= minimalIterationCountIn) {
            throw new NumberIsTooSmallException(maximalIterationCount, minimalIterationCountIn, false);
        }
        this.minimalIterationCount = minimalIterationCountIn;
        this.iterations = new Incrementor();
        this.iterations.setMaximalCount(maximalIterationCount);

        // prepare evaluations counter, but do not set it yet
        this.evaluations = new Incrementor();

    }

    /**
     * Construct an integrator with given accuracies.
     * 
     * @param relativeAccuracyIn
     *        relative accuracy of the result
     * @param absoluteAccuracyIn
     *        absolute accuracy of the result
     */
    protected BaseAbstractUnivariateIntegrator(final double relativeAccuracyIn,
        final double absoluteAccuracyIn) {
        this(relativeAccuracyIn, absoluteAccuracyIn,
            DEFAULT_MIN_ITERATIONS_COUNT, DEFAULT_MAX_ITERATIONS_COUNT);
    }

    /**
     * Construct an integrator with given iteration counts.
     * 
     * @param minimalIterationCountIn
     *        minimum number of iterations
     * @param maximalIterationCount
     *        maximum number of iterations
     * @exception NotStrictlyPositiveException
     *            if minimal number of iterations
     *            is not strictly positive
     * @exception NumberIsTooSmallException
     *            if maximal number of iterations
     *            is lesser than or equal to the minimal number of iterations
     */
    protected BaseAbstractUnivariateIntegrator(final int minimalIterationCountIn,
        final int maximalIterationCount) {
        this(DEFAULT_RELATIVE_ACCURACY, DEFAULT_ABSOLUTE_ACCURACY,
            minimalIterationCountIn, maximalIterationCount);
    }

    /** {@inheritDoc} */
    @Override
    public double getRelativeAccuracy() {
        return this.relativeAccuracy;
    }

    /** {@inheritDoc} */
    @Override
    public double getAbsoluteAccuracy() {
        return this.absoluteAccuracy;
    }

    /** {@inheritDoc} */
    @Override
    public int getMinimalIterationCount() {
        return this.minimalIterationCount;
    }

    /** {@inheritDoc} */
    @Override
    public int getMaximalIterationCount() {
        return this.iterations.getMaximalCount();
    }

    /** {@inheritDoc} */
    @Override
    public int getEvaluations() {
        return this.evaluations.getCount();
    }

    /** {@inheritDoc} */
    @Override
    public int getIterations() {
        return this.iterations.getCount();
    }

    /**
     * @return the lower bound.
     */
    protected double getMin() {
        return this.min;
    }

    /**
     * @return the upper bound.
     */
    protected double getMax() {
        return this.max;
    }

    /**
     * Compute the objective function value.
     * 
     * @param point
     *        Point at which the objective function must be evaluated.
     * @return the objective function value at specified point.
     * @throws TooManyEvaluationsException
     *         if the maximal number of function
     *         evaluations is exceeded.
     */
    @SuppressWarnings("PMD.PreserveStackTrace")
    protected double computeObjectiveValue(final double point) {
        try {
            this.evaluations.incrementCount();
        } catch (final MaxCountExceededException e) {
            throw new TooManyEvaluationsException(e.getMax());
        }
        return this.function.value(point);
    }

    /**
     * Prepare for computation.
     * Subclasses must call this method if they override any of the {@code solve} methods.
     * 
     * @param maxEval
     *        Maximum number of evaluations.
     * @param f
     *        the integrand function
     * @param lower
     *        the min bound for the interval
     * @param upper
     *        the upper bound for the interval
     * @throws NullArgumentException
     *         if {@code f} is {@code null}.
     * @throws MathIllegalArgumentException
     *         if {@code min >= max}.
     */
    protected void setup(final int maxEval,
                         final UnivariateFunction f,
                         final double lower, final double upper) {

        // Checks.
        MathUtils.checkNotNull(f);
        UnivariateSolverUtils.verifyInterval(lower, upper);

        // Reset.
        this.min = lower;
        this.max = upper;
        this.function = f;
        this.evaluations.setMaximalCount(maxEval);
        this.evaluations.resetCount();
        this.iterations.resetCount();

    }

    /** {@inheritDoc} */
    @Override
    public double integrate(final int maxEval, final UnivariateFunction f,
                            final double lower, final double upper) {

        // Initialization.
        this.setup(maxEval, f, lower, upper);

        // Perform computation.
        return this.doIntegrate();

    }

    /**
     * Method for implementing actual integration algorithms in derived
     * classes.
     * 
     * @return the root.
     * @throws TooManyEvaluationsException
     *         if the maximal number of evaluations
     *         is exceeded.
     * @throws MaxCountExceededException
     *         if the maximum iteration count is exceeded
     *         or the integrator detects convergence problems otherwise
     */
    protected abstract double doIntegrate();

}
