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
 * VERSION:4.4:DM:DM-2153:04/10/2019:[PATRIUS] PVCoordinatePropagator
 * VERSION:4.3.1:FA:FA-2136:11/07/2019:[PATRIUS] Exception NumberIsTooLarge lors de la propagation
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.solver;

import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.exception.NoBracketingException;
import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooLargeException;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Utility routines for {@link UnivariateSolver} objects.
 * 
 * @version $Id: UnivariateSolverUtils.java 18108 2017-10-04 06:45:27Z bignon $
 */
public final class UnivariateSolverUtils {

    /** 0.5. */
    private static final double HALF = 0.5;

    /**
     * Class contains only static methods.
     */
    private UnivariateSolverUtils() {
    }

    /**
     * Convenience method to find a zero of a univariate real function. A default
     * solver is used.
     * 
     * @param function
     *        Function.
     * @param x0
     *        Lower bound for the interval.
     * @param x1
     *        Upper bound for the interval.
     * @return a value where the function is zero.
     * @throws NoBracketingException
     *         if the function has the same sign at the
     *         endpoints.
     * @throws NullArgumentException
     *         if {@code function} is {@code null}.
     */
    public static double solve(final UnivariateFunction function, final double x0, final double x1) {
        if (function == null) {
            throw new NullArgumentException(PatriusMessages.FUNCTION);
        }
        final UnivariateSolver solver = new BrentSolver();
        return solver.solve(Integer.MAX_VALUE, function, x0, x1);
    }

    /**
     * Convenience method to find a zero of a univariate real function. A default
     * solver is used.
     * 
     * @param function
     *        Function.
     * @param x0
     *        Lower bound for the interval.
     * @param x1
     *        Upper bound for the interval.
     * @param absoluteAccuracy
     *        Accuracy to be used by the solver.
     * @return a value where the function is zero.
     * @throws NoBracketingException
     *         if the function has the same sign at the
     *         endpoints.
     * @throws NullArgumentException
     *         if {@code function} is {@code null}.
     */
    public static double solve(final UnivariateFunction function,
                               final double x0, final double x1,
                               final double absoluteAccuracy) {
        if (function == null) {
            throw new NullArgumentException(PatriusMessages.FUNCTION);
        }
        final UnivariateSolver solver = new BrentSolver(absoluteAccuracy);
        return solver.solve(Integer.MAX_VALUE, function, x0, x1);
    }

    /**
     * Force a root found by a non-bracketing solver to lie on a specified side,
     * as if the solver was a bracketing one.
     * 
     * @param maxEval
     *        maximal number of new evaluations of the function
     *        (evaluations already done for finding the root should have already been subtracted
     *        from this number)
     * @param f
     *        function to solve
     * @param bracketing
     *        bracketing solver to use for shifting the root
     * @param baseRoot
     *        original root found by a previous non-bracketing solver
     * @param min
     *        minimal bound of the search interval
     * @param max
     *        maximal bound of the search interval
     * @param allowedSolution
     *        the kind of solutions that the root-finding algorithm may
     *        accept as solutions.
     * @return a root approximation, on the specified side of the exact root
     * @throws NoBracketingException
     *         if the function has the same sign at the
     *         endpoints.
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Commons-Math code kept as such
    public static double forceSide(final int maxEval, final UnivariateFunction f,
                                   final BracketedUnivariateSolver<UnivariateFunction> bracketing,
                                   final double baseRoot, final double min, final double max,
                                   final AllowedSolution allowedSolution) {
        // CHECKSTYLE: resume CyclomaticComplexity check

        if (allowedSolution == AllowedSolution.ANY_SIDE) {
            // no further bracketing required
            return baseRoot;
        }

        // find a very small interval bracketing the root
        final double step = MathLib.max(bracketing.getAbsoluteAccuracy(),
            MathLib.abs(baseRoot * bracketing.getRelativeAccuracy()));
        double xLo = MathLib.max(min, baseRoot - step);
        double fLo = f.value(xLo);
        double xHi = MathLib.min(max, baseRoot + step);
        double fHi = f.value(xHi);
        int remainingEval = maxEval - 2;
        while (remainingEval > 0) {

            if ((fLo >= 0 && fHi <= 0) || (fLo <= 0 && fHi >= 0)) {
                // compute the root on the selected side
                return bracketing.solve(remainingEval, f, xLo, xHi, baseRoot, allowedSolution);
            }

            // try increasing the interval
            boolean changeLo = false;
            boolean changeHi = false;
            if (fLo < fHi) {
                // increasing function
                if (fLo >= 0) {
                    changeLo = true;
                } else {
                    changeHi = true;
                }
            } else if (fLo > fHi) {
                // decreasing function
                if (fLo <= 0) {
                    changeLo = true;
                } else {
                    changeHi = true;
                }
            } else {
                // unknown variation
                changeLo = true;
                changeHi = true;
            }

            // update the lower bound
            if (changeLo) {
                xLo = MathLib.max(min, xLo - step);
                fLo = f.value(xLo);
                remainingEval--;
            }

            // update the higher bound
            if (changeHi) {
                xHi = MathLib.min(max, xHi + step);
                fHi = f.value(xHi);
                remainingEval--;
            }

        }

        throw new NoBracketingException(PatriusMessages.FAILED_BRACKETING,
            xLo, xHi, fLo, fHi,
            maxEval - remainingEval, maxEval, baseRoot,
            min, max);

    }

    /**
     * This method attempts to find two values a and b satisfying
     * <ul>
     * <li> <code> lowerBound <= a < initial < b <= upperBound</code></li>
     * <li> <code> f(a) * f(b) < 0 </code></li>
     * </ul>
     * If f is continuous on <code>[a,b],</code> this means that <code>a</code> and <code>b</code> bracket a root of f.
     * <p>
     * The algorithm starts by setting <code>a := initial -1; b := initial +1,</code> examines the value of the function
     * at <code>a</code> and <code>b</code> and keeps moving the endpoints out by one unit each time through a loop that
     * terminates when one of the following happens:
     * <ul>
     * <li> <code> f(a) * f(b) < 0 </code> -- success!</li>
     * <li> <code> a = lower </code> and <code> b = upper</code> -- NoBracketingException</li>
     * <li> <code> Integer.MAX_VALUE</code> iterations elapse -- NoBracketingException</li>
     * </ul>
     * </p>
     * <p>
     * <strong>Note: </strong> this method can take <code>Integer.MAX_VALUE</code> iterations to throw a
     * <code>ConvergenceException.</code> Unless you are confident that there is a root between <code>lowerBound</code>
     * and <code>upperBound</code> near <code>initial,</code> it is better to use
     * {@link #bracket(UnivariateFunction, double, double, double, int)}, explicitly specifying the maximum number of
     * iterations.
     * </p>
     * 
     * @param function
     *        Function.
     * @param initial
     *        Initial midpoint of interval being expanded to
     *        bracket a root.
     * @param lowerBound
     *        Lower bound (a is never lower than this value)
     * @param upperBound
     *        Upper bound (b never is greater than this
     *        value).
     * @return a two-element array holding a and b.
     * @throws NoBracketingException
     *         if a root cannot be bracketted.
     * @throws NotStrictlyPositiveException
     *         if {@code maximumIterations <= 0}.
     * @throws NullArgumentException
     *         if {@code function} is {@code null}.
     */
    public static double[] bracket(final UnivariateFunction function,
                                   final double initial,
                                   final double lowerBound, final double upperBound) {
        return bracket(function, initial, lowerBound, upperBound, Integer.MAX_VALUE);
    }

    /**
     * This method attempts to find two values a and b satisfying
     * <ul>
     * <li> <code> lowerBound <= a < initial < b <= upperBound</code></li>
     * <li> <code> f(a) * f(b) <= 0 </code></li>
     * </ul>
     * If f is continuous on <code>[a,b],</code> this means that <code>a</code> and <code>b</code> bracket a root of f.
     * <p>
     * The algorithm starts by setting <code>a := initial -1; b := initial +1,</code> examines the value of the function
     * at <code>a</code> and <code>b</code> and keeps moving the endpoints out by one unit each time through a loop that
     * terminates when one of the following happens:
     * <ul>
     * <li> <code> f(a) * f(b) <= 0 </code> -- success!</li>
     * <li> <code> a = lower </code> and <code> b = upper</code> -- NoBracketingException</li>
     * <li> <code> maximumIterations</code> iterations elapse -- NoBracketingException</li>
     * </ul>
     * </p>
     * 
     * @param function
     *        Function.
     * @param initial
     *        Initial midpoint of interval being expanded to
     *        bracket a root.
     * @param lowerBound
     *        Lower bound (a is never lower than this value).
     * @param upperBound
     *        Upper bound (b never is greater than this
     *        value).
     * @param maximumIterations
     *        Maximum number of iterations to perform
     * @return a two element array holding a and b.
     * @throws NoBracketingException
     *         if the algorithm fails to find a and b
     *         satisfying the desired conditions.
     * @throws NotStrictlyPositiveException
     *         if {@code maximumIterations <= 0}.
     * @throws NullArgumentException
     *         if {@code function} is {@code null}.
     */
    public static double[] bracket(final UnivariateFunction function,
                                   final double initial,
                                   final double lowerBound, final double upperBound,
                                   final int maximumIterations) {
        if (function == null) {
            // Exception
            throw new NullArgumentException(PatriusMessages.FUNCTION);
        }
        if (maximumIterations <= 0) {
            // Exception
            throw new NotStrictlyPositiveException(PatriusMessages.INVALID_MAX_ITERATIONS, maximumIterations);
        }

        // Check sequence order
        verifySequence(lowerBound, initial, upperBound);

        // Initialization
        double a = initial;
        double b = initial;
        double fa;
        double fb;
        int numIterations = 0;

        do {
            // Loop until convergence
            a = MathLib.max(a - 1.0, lowerBound);
            b = MathLib.min(b + 1.0, upperBound);
            fa = function.value(a);

            fb = function.value(b);
            ++numIterations;
        } while ((fa * fb > 0.0) && (numIterations < maximumIterations) &&
            ((a > lowerBound) || (b < upperBound)));

        if (fa * fb > 0.0) {
            throw new NoBracketingException(PatriusMessages.FAILED_BRACKETING,
                a, b, fa, fb,
                numIterations, maximumIterations, initial,
                lowerBound, upperBound);
        }

        // Return result
        return new double[] { a, b };
    }

    /**
     * Compute the midpoint of two values.
     * 
     * @param a
     *        first value.
     * @param b
     *        second value.
     * @return the midpoint.
     */
    public static double midpoint(final double a, final double b) {
        return (a + b) * HALF;
    }

    /**
     * Check whether the interval bounds bracket a root. That is, if the
     * values at the endpoints are not equal to zero, then the function takes
     * opposite signs at the endpoints.
     * 
     * @param function
     *        Function.
     * @param lower
     *        Lower endpoint.
     * @param upper
     *        Upper endpoint.
     * @return {@code true} if the function values have opposite signs at the
     *         given points.
     * @throws NullArgumentException
     *         if {@code function} is {@code null}.
     */
    public static boolean isBracketing(final UnivariateFunction function,
                                       final double lower,
                                       final double upper) {
        if (function == null) {
            throw new NullArgumentException(PatriusMessages.FUNCTION);
        }
        final double fLo = function.value(lower);
        final double fHi = function.value(upper);
        return (fLo >= 0 && fHi <= 0) || (fLo <= 0 && fHi >= 0);
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
    public static boolean isSequence(final double start,
                                     final double mid,
                                     final double end) {
        return (start < mid) && (mid < end);
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
    public static void verifyInterval(final double lower,
                                      final double upper) {
        if (lower >= upper) {
            throw new NumberIsTooLargeException(PatriusMessages.ENDPOINTS_NOT_AN_INTERVAL,
                lower, upper, false);
        }
    }
    
    /**
     * Check that the endpoints specify an interval.
     * 
     * @param lower
     *            Lower endpoint.
     * @param upper
     *            Upper endpoint.
     * @throws NumberIsTooLargeException
     *             if {@code lower > upper}.
     */
    public static void verifyIntervalStrict(final double lower,
            final double upper) {
        if (lower > upper) {
            throw new NumberIsTooLargeException(PatriusMessages.ENDPOINTS_NOT_AN_INTERVAL,
                    lower, upper, false);
        }
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
    public static void verifySequence(final double lower,
                                      final double initial,
                                      final double upper) {
        verifyInterval(lower, initial);
        verifyInterval(initial, upper);
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
    public static void verifySequenceStrict(final double lower,
            final double initial,
            final double upper) {
        verifyInterval(lower, upper);
        verifyIntervalStrict(lower, initial);
        verifyIntervalStrict(initial, upper);
    }

    /**
     * Check that the endpoints specify an interval and the end points
     * bracket a root.
     * 
     * @param function
     *        Function.
     * @param lower
     *        Lower endpoint.
     * @param upper
     *        Upper endpoint.
     * @throws NoBracketingException
     *         if the function has the same sign at the
     *         endpoints.
     * @throws NullArgumentException
     *         if {@code function} is {@code null}.
     */
    public static void verifyBracketing(final UnivariateFunction function,
                                        final double lower,
                                        final double upper) {
        if (function == null) {
            throw new NullArgumentException(PatriusMessages.FUNCTION);
        }
        verifyInterval(lower, upper);
        if (!isBracketing(function, lower, upper)) {
            throw new NoBracketingException(lower, upper,
                function.value(lower),
                function.value(upper));
        }
    }
}
