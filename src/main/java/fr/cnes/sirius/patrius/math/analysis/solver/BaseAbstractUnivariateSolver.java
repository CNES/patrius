/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
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
 * 
 * 
 * @history created 16/11/17
 * 
 * HISTORY
* VERSION:4.4:DM:DM-2153:04/10/2019:[PATRIUS] PVCoordinatePropagator
 * VERSION:4.3.1:FA:FA-2136:11/07/2019:[PATRIUS] Exception NumberIsTooLarge lors de la propagation
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1305:16/11/2017: Serializable interface implementation
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.solver;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.exception.MaxCountExceededException;
import fr.cnes.sirius.patrius.math.exception.NoBracketingException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooLargeException;
import fr.cnes.sirius.patrius.math.exception.TooManyEvaluationsException;
import fr.cnes.sirius.patrius.math.util.Incrementor;
import fr.cnes.sirius.patrius.math.util.MathUtils;

/**
 * Provide a default implementation for several functions useful to generic
 * solvers.
 * 
 * @param <F>
 *        Type of function to solve.
 * 
 * @since 2.0
 * @version $Id: BaseAbstractUnivariateSolver.java 18108 2017-10-04 06:45:27Z bignon $
 */
//CHECKSTYLE: stop AbstractClassName check
@SuppressWarnings("PMD.AbstractNaming")
public abstract class BaseAbstractUnivariateSolver<F extends UnivariateFunction>
    implements BaseUnivariateSolver<F>, Serializable {
    //CHECKSTYLE: resume AbstractClassName check

    /** Serializable UID. */
    private static final long serialVersionUID = 7789280485915572943L;
    /** Default relative accuracy. */
    private static final double DEFAULT_RELATIVE_ACCURACY = 1e-14;
    /** Default function value accuracy. */
    private static final double DEFAULT_FUNCTION_VALUE_ACCURACY = 1e-15;
    /** 0.5. */
    private static final double HALF = 0.5;
    /** Function value accuracy. */
    private final double functionValueAccuracy;
    /** Absolute accuracy. */
    private final double absoluteAccuracy;
    /** Relative accuracy. */
    private final double relativeAccuracy;
    /** Evaluations counter. */
    private final Incrementor evaluations = new Incrementor();
    /** Lower end of search interval. */
    private double searchMin;
    /** Higher end of search interval. */
    private double searchMax;
    /** Initial guess. */
    private double searchStart;
    /** Function to solve. */
    private F function;

    /**
     * Construct a solver with given absolute accuracy.
     * 
     * @param absoluteAccuracyIn
     *        Maximum absolute error.
     */
    protected BaseAbstractUnivariateSolver(final double absoluteAccuracyIn) {
        this(DEFAULT_RELATIVE_ACCURACY,
            absoluteAccuracyIn,
            DEFAULT_FUNCTION_VALUE_ACCURACY);
    }

    /**
     * Construct a solver with given accuracies.
     * 
     * @param relativeAccuracyIn
     *        Maximum relative error.
     * @param absoluteAccuracyIn
     *        Maximum absolute error.
     */
    protected BaseAbstractUnivariateSolver(final double relativeAccuracyIn,
        final double absoluteAccuracyIn) {
        this(relativeAccuracyIn,
            absoluteAccuracyIn,
            DEFAULT_FUNCTION_VALUE_ACCURACY);
    }

    /**
     * Construct a solver with given accuracies.
     * 
     * @param relativeAccuracyIn
     *        Maximum relative error.
     * @param absoluteAccuracyIn
     *        Maximum absolute error.
     * @param functionValueAccuracyIn
     *        Maximum function value error.
     */
    protected BaseAbstractUnivariateSolver(final double relativeAccuracyIn,
        final double absoluteAccuracyIn,
        final double functionValueAccuracyIn) {
        this.absoluteAccuracy = absoluteAccuracyIn;
        this.relativeAccuracy = relativeAccuracyIn;
        this.functionValueAccuracy = functionValueAccuracyIn;
    }

    /** {@inheritDoc} */
    @Override
    public int getMaxEvaluations() {
        return this.evaluations.getMaximalCount();
    }

    /** {@inheritDoc} */
    @Override
    public int getEvaluations() {
        return this.evaluations.getCount();
    }

    /**
     * @return the lower end of the search interval.
     */
    public double getMin() {
        return this.searchMin;
    }

    /**
     * @return the higher end of the search interval.
     */
    public double getMax() {
        return this.searchMax;
    }

    /**
     * @return the initial guess.
     */
    public double getStartValue() {
        return this.searchStart;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getAbsoluteAccuracy() {
        return this.absoluteAccuracy;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getRelativeAccuracy() {
        return this.relativeAccuracy;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getFunctionValueAccuracy() {
        return this.functionValueAccuracy;
    }

    /**
     * Compute the objective function value.
     * 
     * @param point
     *        Point at which the objective function must be evaluated.
     * @return the objective function value at specified point.
     * @throws TooManyEvaluationsException
     *         if the maximal number of evaluations
     *         is exceeded.
     */
    protected double computeObjectiveValue(final double point) {
        this.incrementEvaluationCount();
        return this.function.value(point);
    }

    /**
     * Prepare for computation.
     * Subclasses must call this method if they override any of the {@code solve} methods.
     * 
     * @param f
     *        Function to solve.
     * @param min
     *        Lower bound for the interval.
     * @param max
     *        Upper bound for the interval.
     * @param startValue
     *        Start value to use.
     * @param maxEval
     *        Maximum number of evaluations.
     */
    protected void setup(final int maxEval,
                         final F f,
                         final double min, final double max,
                         final double startValue) {
        // Checks.
        MathUtils.checkNotNull(f);

        // Reset.
        this.searchMin = min;
        this.searchMax = max;
        this.searchStart = startValue;
        this.function = f;
        this.evaluations.setMaximalCount(maxEval);
        this.evaluations.resetCount();
    }

    /** {@inheritDoc} */
    @Override
    public double solve(final int maxEval, final F f, final double min, final double max, final double startValue) {
        // Initialization.
        this.setup(maxEval, f, min, max, startValue);

        // Perform computation.
        return this.doSolve();
    }

    /** {@inheritDoc} */
    @Override
    public double solve(final int maxEval, final F f, final double min, final double max) {
        return this.solve(maxEval, f, min, max, min + HALF * (max - min));
    }

    /** {@inheritDoc} */
    @Override
    public double solve(final int maxEval, final F f, final double startValue) {
        return this.solve(maxEval, f, Double.NaN, Double.NaN, startValue);
    }

    /**
     * Method for implementing actual optimization algorithms in derived
     * classes.
     * 
     * @return the root.
     * @throws TooManyEvaluationsException
     *         if the maximal number of evaluations
     *         is exceeded.
     * @throws NoBracketingException
     *         if the initial search interval does not bracket
     *         a root and the solver requires it.
     */
    protected abstract double doSolve();

    /**
     * Check whether the function takes opposite signs at the endpoints.
     * 
     * @param lower
     *        Lower endpoint.
     * @param upper
     *        Upper endpoint.
     * @return {@code true} if the function values have opposite signs at the
     *         given points.
     */
    protected boolean isBracketing(final double lower,
                                   final double upper) {
        return UnivariateSolverUtils.isBracketing(this.function, lower, upper);
    }

    /**
     * Check whether the arguments form a (strictly) increasing sequence.
     * 
     * @param start
     *        First number.
     * @param mid
     *        Second number.
     * @param end
     *        Third number.
     * @return {@code true} if the arguments form an increasing sequence.
     */
    protected boolean isSequence(final double start,
                                 final double mid,
                                 final double end) {
        return UnivariateSolverUtils.isSequence(start, mid, end);
    }

    /**
     * Check that the endpoints specify an interval.
     * 
     * @param lower
     *        Lower endpoint.
     * @param upper
     *        Upper endpoint.
     * @throws NumberIsTooLargeException
     *         if {@code lower >= upper}.
     */
    protected void verifyInterval(final double lower,
                                  final double upper) {
        UnivariateSolverUtils.verifyInterval(lower, upper);
    }

    /**
     * Check that {@code lower < initial < upper}.
     * 
     * @param lower
     *        Lower endpoint.
     * @param initial
     *        Initial value.
     * @param upper
     *        Upper endpoint.
     * @throws NumberIsTooLargeException
     *         if {@code lower >= initial} or {@code initial >= upper}.
     */
    protected void verifySequence(final double lower,
                                  final double initial,
                                  final double upper) {
        UnivariateSolverUtils.verifySequence(lower, initial, upper);
    }
    
    /**
     * Check that {@code lower <= initial <= upper & lower < upper}.
     * 
     * @param lower
     *            Lower endpoint.
     * @param initial
     *            Initial value.
     * @param upper
     *            Upper endpoint.
     * @throws NumberIsTooLargeException
     *             if {@code lower > initial} or {@code initial > upper} or {@code lower >= upper}.
     */
    protected void verifySequenceStrict(final double lower,
            final double initial,
            final double upper) {
        UnivariateSolverUtils.verifySequenceStrict(lower, initial, upper);
    }

    /**
     * Check that the endpoints specify an interval and the function takes
     * opposite signs at the endpoints.
     * 
     * @param lower
     *        Lower endpoint.
     * @param upper
     *        Upper endpoint.
     * @throws NullArgumentException
     *         if the function has not been set.
     * @throws NoBracketingException
     *         if the function has the same sign at
     *         the endpoints.
     */
    protected void verifyBracketing(final double lower,
                                    final double upper) {
        UnivariateSolverUtils.verifyBracketing(this.function, lower, upper);
    }

    /**
     * Increment the evaluation count by one.
     * Method {@link #computeObjectiveValue(double)} calls this method internally.
     * It is provided for subclasses that do not exclusively use {@code computeObjectiveValue} to solve the function.
     * See e.g. {@link AbstractUnivariateDifferentiableSolver}.
     * 
     * @throws TooManyEvaluationsException
     *         when the allowed number of function
     *         evaluations has been exhausted.
     */
    @SuppressWarnings("PMD.PreserveStackTrace")
    protected void incrementEvaluationCount() {
        try {
            this.evaluations.incrementCount();
        } catch (final MaxCountExceededException e) {
            throw new TooManyEvaluationsException(e.getMax());
        }
    }
}
