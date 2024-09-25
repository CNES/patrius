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
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.solver;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.analysis.polynomials.PolynomialFunction;
import fr.cnes.sirius.patrius.math.complex.Complex;
import fr.cnes.sirius.patrius.math.complex.ComplexUtils;
import fr.cnes.sirius.patrius.math.exception.NoBracketingException;
import fr.cnes.sirius.patrius.math.exception.NoDataException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.exception.TooManyEvaluationsException;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

//CHECKSTYLE: stop CommentRatio check
//Reason: model - Commons-Math code kept as such

/**
 * Implements the <a href="http://mathworld.wolfram.com/LaguerresMethod.html">
 * Laguerre's Method</a> for root finding of real coefficient polynomials.
 * For reference, see
 * <quote>
 * <b>A First Course in Numerical Analysis</b>
 * ISBN 048641454X, chapter 8.
 * </quote>
 * Laguerre's method is global in the sense that it can start with any initial
 * approximation and be able to solve all roots from that point.
 * The algorithm requires a bracketing condition.
 * 
 * @version $Id: LaguerreSolver.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 1.2
 */
public class LaguerreSolver extends AbstractPolynomialSolver {

     /** Serializable UID. */
    private static final long serialVersionUID = -8938148528590182210L;

    /** Default absolute accuracy. */
    private static final double DEFAULT_ABSOLUTE_ACCURACY = 1e-6;
    /** Complex solver. */
    private final ComplexSolver complexSolver = new ComplexSolver();

    /**
     * Construct a solver with default accuracy (1e-6).
     */
    public LaguerreSolver() {
        this(DEFAULT_ABSOLUTE_ACCURACY);
    }

    /**
     * Construct a solver.
     * 
     * @param absoluteAccuracy
     *        Absolute accuracy.
     */
    public LaguerreSolver(final double absoluteAccuracy) {
        super(absoluteAccuracy);
    }

    /**
     * Construct a solver.
     * 
     * @param relativeAccuracy
     *        Relative accuracy.
     * @param absoluteAccuracy
     *        Absolute accuracy.
     */
    public LaguerreSolver(final double relativeAccuracy,
        final double absoluteAccuracy) {
        super(relativeAccuracy, absoluteAccuracy);
    }

    /**
     * Construct a solver.
     * 
     * @param relativeAccuracy
     *        Relative accuracy.
     * @param absoluteAccuracy
     *        Absolute accuracy.
     * @param functionValueAccuracy
     *        Function value accuracy.
     */
    public LaguerreSolver(final double relativeAccuracy,
        final double absoluteAccuracy,
        final double functionValueAccuracy) {
        super(relativeAccuracy, absoluteAccuracy, functionValueAccuracy);
    }

    /**
     * {@inheritDoc}
     */
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Commons-Math code kept as such
    @Override
    public double doSolve() {
        // CHECKSTYLE: resume ReturnCount check
        final double min = this.getMin();
        final double max = this.getMax();
        final double initial = this.getStartValue();
        final double functionValueAccuracy = this.getFunctionValueAccuracy();

        this.verifySequence(min, initial, max);

        // Return the initial guess if it is good enough.
        final double yInitial = this.computeObjectiveValue(initial);
        if (MathLib.abs(yInitial) <= functionValueAccuracy) {
            return initial;
        }

        // Return the first endpoint if it is good enough.
        final double yMin = this.computeObjectiveValue(min);
        if (MathLib.abs(yMin) <= functionValueAccuracy) {
            return min;
        }

        // Reduce interval if min and initial bracket the root.
        if (yInitial * yMin < 0) {
            return this.laguerre(min, initial, yMin, yInitial);
        }

        // Return the second endpoint if it is good enough.
        final double yMax = this.computeObjectiveValue(max);
        if (MathLib.abs(yMax) <= functionValueAccuracy) {
            return max;
        }

        // Reduce interval if initial and max bracket the root.
        if (yInitial * yMax < 0) {
            return this.laguerre(initial, max, yInitial, yMax);
        }

        throw new NoBracketingException(min, max, yMin, yMax);
    }

    /**
     * Find a real root in the given interval.
     * 
     * Despite the bracketing condition, the root returned by
     * {@link LaguerreSolver.ComplexSolver#solve(Complex[],Complex)} may
     * not be a real zero inside {@code [min, max]}.
     * For example, <code>p(x) = x<sup>3</sup> + 1,</code> with {@code min = -2}, {@code max = 2}, {@code initial = 0}.
     * When it occurs, this code calls {@link LaguerreSolver.ComplexSolver#solveAll(Complex[],Complex)} in order to
     * obtain all roots and picks up one real root.
     * 
     * @param lo
     *        Lower bound of the search interval.
     * @param hi
     *        Higher bound of the search interval.
     * @param fLo
     *        Function value at the lower bound of the search interval.
     * @param fHi
     *        Function value at the higher bound of the search interval.
     * @return the point at which the function value is zero.
     */
    private double laguerre(final double lo, final double hi,
                            final double fLo, final double fHi) {
        final Complex[] c = ComplexUtils.convertToComplex(this.getCoefficients());

        final Complex initial = new Complex(0.5 * (lo + hi), 0);
        final Complex z = this.complexSolver.solve(c, initial);
        if (this.complexSolver.isRoot(lo, hi, z)) {
            return z.getReal();
        } else {
            double r = Double.NaN;
            // Solve all roots and select the one we are seeking.
            final Complex[] root = this.complexSolver.solveAll(c, initial);
            for (final Complex element : root) {
                if (this.complexSolver.isRoot(lo, hi, element)) {
                    r = element.getReal();
                    break;
                }
            }
            return r;
        }
    }

    /**
     * Find all complex roots for the polynomial with the given
     * coefficients, starting from the given initial value. <br/>
     * Note: This method is not part of the API of {@link BaseUnivariateSolver}.
     * 
     * @param coefficients
     *        Polynomial coefficients.
     * @param initial
     *        Start value.
     * @return the point at which the function value is zero.
     * @throws fr.cnes.sirius.patrius.math.exception.TooManyEvaluationsException
     *         if the maximum number of evaluations is exceeded.
     * @throws NullArgumentException
     *         if the {@code coefficients} is {@code null}.
     * @throws NoDataException
     *         if the {@code coefficients} array is empty.
     * @throws TooManyEvaluationsException
     *         if too many evaluations are required.
     * @since 3.1
     */
    public Complex[] solveAllComplex(final double[] coefficients,
                                     final double initial) {
        this.setup(Integer.MAX_VALUE,
            new PolynomialFunction(coefficients),
            Double.NEGATIVE_INFINITY,
            Double.POSITIVE_INFINITY,
            initial);
        return this.complexSolver.solveAll(ComplexUtils.convertToComplex(coefficients),
            new Complex(initial, 0d));
    }

    /**
     * Find a complex root for the polynomial with the given coefficients,
     * starting from the given initial value. <br/>
     * Note: This method is not part of the API of {@link BaseUnivariateSolver}.
     * 
     * @param coefficients
     *        Polynomial coefficients.
     * @param initial
     *        Start value.
     * @return the point at which the function value is zero.
     * @throws fr.cnes.sirius.patrius.math.exception.TooManyEvaluationsException
     *         if the maximum number of evaluations is exceeded.
     * @throws NullArgumentException
     *         if the {@code coefficients} is {@code null}.
     * @throws NoDataException
     *         if the {@code coefficients} array is empty.
     * @throws TooManyEvaluationsException
     *         if too many evaluations are required.
     * @since 3.1
     */
    public Complex solveComplex(final double[] coefficients,
                                final double initial) {
        this.setup(Integer.MAX_VALUE,
            new PolynomialFunction(coefficients),
            Double.NEGATIVE_INFINITY,
            Double.POSITIVE_INFINITY,
            initial);
        return this.complexSolver.solve(ComplexUtils.convertToComplex(coefficients),
            new Complex(initial, 0d));
    }

    /**
     * Class for searching all (complex) roots.
     */
    private class ComplexSolver implements Serializable {
        
         /** Serializable UID. */
        private static final long serialVersionUID = 5101660065190637154L;

        /**
         * Check whether the given complex root is actually a real zero
         * in the given interval, within the solver tolerance level.
         * 
         * @param min
         *        Lower bound for the interval.
         * @param max
         *        Upper bound for the interval.
         * @param z
         *        Complex root.
         * @return {@code true} if z is a real zero.
         */
        public boolean isRoot(final double min, final double max, final Complex z) {
            if (LaguerreSolver.this.isSequence(min, z.getReal(), max)) {
                final double tolerance =
                    MathLib.max(LaguerreSolver.this.getRelativeAccuracy() * z.abs(),
                        LaguerreSolver.this.getAbsoluteAccuracy());
                return (MathLib.abs(z.getImaginary()) <= tolerance) ||
                    (z.abs() <= LaguerreSolver.this.getFunctionValueAccuracy());
            }
            return false;
        }

        /**
         * Find all complex roots for the polynomial with the given
         * coefficients, starting from the given initial value.
         * 
         * @param coefficients
         *        Polynomial coefficients.
         * @param initial
         *        Start value.
         * @return the point at which the function value is zero.
         * @throws fr.cnes.sirius.patrius.math.exception.TooManyEvaluationsException
         *         if the maximum number of evaluations is exceeded.
         * @throws NullArgumentException
         *         if the {@code coefficients} is {@code null}.
         * @throws NoDataException
         *         if the {@code coefficients} array is empty.
         * @throws TooManyEvaluationsException
         *         if too many evaluations are required.
         */
        public Complex[] solveAll(final Complex[] coefficients, final Complex initial) {
            if (coefficients == null) {
                throw new NullArgumentException();
            }
            final int n = coefficients.length - 1;
            if (n == 0) {
                throw new NoDataException(PatriusMessages.POLYNOMIAL);
            }
            // Coefficients for deflated polynomial.
            final Complex[] c = new Complex[n + 1];
            System.arraycopy(coefficients, 0, c, 0, c.length);

            // Solve individual roots successively.
            final Complex[] root = new Complex[n];
            for (int i = 0; i < n; i++) {
                final Complex[] subarray = new Complex[n - i + 1];
                System.arraycopy(c, 0, subarray, 0, subarray.length);
                root[i] = this.solve(subarray, initial);
                // Polynomial deflation using synthetic division.
                Complex newc = c[n - i];
                Complex oldc = null;
                for (int j = n - i - 1; j >= 0; j--) {
                    oldc = c[j];
                    c[j] = newc;
                    newc = oldc.add(newc.multiply(root[i]));
                }
            }

            return root;
        }

        /**
         * Find a complex root for the polynomial with the given coefficients,
         * starting from the given initial value.
         * 
         * @param coefficients
         *        Polynomial coefficients.
         * @param initial
         *        Start value.
         * @return the point at which the function value is zero.
         * @throws fr.cnes.sirius.patrius.math.exception.TooManyEvaluationsException
         *         if the maximum number of evaluations is exceeded.
         * @throws NullArgumentException
         *         if the {@code coefficients} is {@code null}.
         * @throws NoDataException
         *         if the {@code coefficients} array is empty.
         * @throws TooManyEvaluationsException
         *         if too many evaluations are required.
         */
        public Complex solve(final Complex[] coefficients, final Complex initial) {
            if (coefficients == null) {
                throw new NullArgumentException();
            }

            final int n = coefficients.length - 1;
            if (n == 0) {
                throw new NoDataException(PatriusMessages.POLYNOMIAL);
            }

            final double absoluteAccuracy = LaguerreSolver.this.getAbsoluteAccuracy();
            final double relativeAccuracy = LaguerreSolver.this.getRelativeAccuracy();
            final double functionValueAccuracy = LaguerreSolver.this.getFunctionValueAccuracy();

            final Complex nC = new Complex(n, 0);
            final Complex n1C = new Complex(n - 1, 0);

            Complex z = initial;
            Complex oldz = new Complex(Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY);
            while (true) {
                // Compute pv (polynomial value), dv (derivative value), and
                // d2v (second derivative value) simultaneously.
                Complex pv = coefficients[n];
                Complex dv = Complex.ZERO;
                Complex d2v = Complex.ZERO;
                for (int j = n - 1; j >= 0; j--) {
                    d2v = dv.add(z.multiply(d2v));
                    dv = pv.add(z.multiply(dv));
                    pv = coefficients[j].add(z.multiply(pv));
                }
                d2v = d2v.multiply(new Complex(2.0, 0.0));

                // Check for convergence.
                final double tolerance = MathLib.max(relativeAccuracy * z.abs(),
                    absoluteAccuracy);
                if ((z.subtract(oldz)).abs() <= tolerance) {
                    return z;
                }
                if (pv.abs() <= functionValueAccuracy) {
                    return z;
                }

                // Now pv != 0, calculate the new approximation.
                final Complex g = dv.divide(pv);
                final Complex g2 = g.multiply(g);
                final Complex h = g2.subtract(d2v.divide(pv));
                final Complex delta = n1C.multiply((nC.multiply(h)).subtract(g2));
                // Choose a denominator larger in magnitude.
                final Complex deltaSqrt = delta.sqrt();
                final Complex dplus = g.add(deltaSqrt);
                final Complex dminus = g.subtract(deltaSqrt);
                final Complex denominator = dplus.abs() > dminus.abs() ? dplus : dminus;
                // Perturb z if denominator is zero, for instance,
                // p(x) = x^3 + 1, z = 0.
                if (denominator.equals(new Complex(0.0, 0.0))) {
                    z = z.add(new Complex(absoluteAccuracy, absoluteAccuracy));
                    oldz = new Complex(Double.POSITIVE_INFINITY,
                        Double.POSITIVE_INFINITY);
                } else {
                    oldz = z;
                    z = z.subtract(nC.divide(denominator));
                }
                LaguerreSolver.this.incrementEvaluationCount();
            }
        }
    }

    // CHECKSTYLE: resume CommentRatio check
}
