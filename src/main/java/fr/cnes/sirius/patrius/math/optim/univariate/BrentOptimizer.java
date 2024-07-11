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
package fr.cnes.sirius.patrius.math.optim.univariate;

import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;
import fr.cnes.sirius.patrius.math.optim.ConvergenceChecker;
import fr.cnes.sirius.patrius.math.optim.nonlinear.scalar.GoalType;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;

//CHECKSTYLE: stop CommentRatio check
//Reason: Commons-Math model kept as such

/**
 * For a function defined on some interval {@code (lo, hi)}, this class
 * finds an approximation {@code x} to the point at which the function
 * attains its minimum.
 * It implements Richard Brent's algorithm (from his book "Algorithms for
 * Minimization without Derivatives", p. 79) for finding minima of real
 * univariate functions. <br/>
 * This code is an adaptation, partly based on the Python code from SciPy
 * (module "optimize.py" v0.5); the original algorithm is also modified
 * <ul>
 * <li>to use an initial guess provided by the user,</li>
 * <li>to ensure that the best point encountered is the one returned.</li>
 * </ul>
 * 
 * @version $Id: BrentOptimizer.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 2.0
 */
public class BrentOptimizer extends UnivariateOptimizer {
    /**
     * Golden section.
     */
    private static final double GOLDEN_SECTION = 0.5 * (3 - MathLib.sqrt(5));
    /**
     * Minimum relative tolerance.
     */
    private static final double MIN_RELATIVE_TOLERANCE = 2 * MathLib.ulp(1d);

    /** 0.5. */
    private static final double HALF = 0.5;
    /**
     * Relative threshold.
     */
    private final double relativeThreshold;
    /**
     * Absolute threshold.
     */
    private final double absoluteThreshold;

    /**
     * The arguments are used implement the original stopping criterion
     * of Brent's algorithm. {@code abs} and {@code rel} define a tolerance {@code tol = rel |x| + abs}. {@code rel}
     * should be no smaller than <em>2 macheps</em> and preferably not much less than <em>sqrt(macheps)</em>,
     * where <em>macheps</em> is the relative machine precision. {@code abs} must
     * be positive.
     * 
     * @param rel
     *        Relative threshold.
     * @param abs
     *        Absolute threshold.
     * @param checker
     *        Additional, user-defined, convergence checking
     *        procedure.
     * @throws NotStrictlyPositiveException
     *         if {@code abs <= 0}.
     * @throws NumberIsTooSmallException
     *         if {@code rel < 2 * Math.ulp(1d)}.
     */
    public BrentOptimizer(final double rel,
        final double abs,
        final ConvergenceChecker<UnivariatePointValuePair> checker) {
        super(checker);

        if (rel < MIN_RELATIVE_TOLERANCE) {
            throw new NumberIsTooSmallException(rel, MIN_RELATIVE_TOLERANCE, true);
        }
        if (abs <= 0) {
            throw new NotStrictlyPositiveException(abs);
        }

        this.relativeThreshold = rel;
        this.absoluteThreshold = abs;
    }

    /**
     * The arguments are used for implementing the original stopping criterion
     * of Brent's algorithm. {@code abs} and {@code rel} define a tolerance {@code tol = rel |x| + abs}. {@code rel}
     * should be no smaller than <em>2 macheps</em> and preferably not much less than <em>sqrt(macheps)</em>,
     * where <em>macheps</em> is the relative machine precision. {@code abs} must
     * be positive.
     * 
     * @param rel
     *        Relative threshold.
     * @param abs
     *        Absolute threshold.
     * @throws NotStrictlyPositiveException
     *         if {@code abs <= 0}.
     * @throws NumberIsTooSmallException
     *         if {@code rel < 2 * Math.ulp(1d)}.
     */
    public BrentOptimizer(final double rel,
        final double abs) {
        this(rel, abs, null);
    }

    /** {@inheritDoc} */
    // CHECKSTYLE: stop MethodLength check
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Commons-Math code kept as such
    @Override
    protected UnivariatePointValuePair doOptimize() {
        // CHECKSTYLE: resume MethodLength check
        // CHECKSTYLE: resume CyclomaticComplexity check

        final boolean isMinim = this.getGoalType() == GoalType.MINIMIZE;
        final double lo = this.getMin();
        final double mid = this.getStartValue();
        final double hi = this.getMax();

        // Optional additional convergence criteria.
        final ConvergenceChecker<UnivariatePointValuePair> checker = this.getConvergenceChecker();

        double a;
        double b;
        if (lo < hi) {
            a = lo;
            b = hi;
        } else {
            a = hi;
            b = lo;
        }

        double x = mid;
        double v = x;
        double w = x;
        double d = 0;
        double e = 0;
        double fx = this.computeObjectiveValue(x);
        if (!isMinim) {
            fx = -fx;
        }
        double fv = fx;
        double fw = fx;

        UnivariatePointValuePair previous = null;
        UnivariatePointValuePair current = new UnivariatePointValuePair(x, isMinim ? fx : -fx);
        // Best point encountered so far (which is the initial guess).
        UnivariatePointValuePair best = current;

        int iter = 0;
        while (true) {
            final double m = 0.5 * (a + b);
            final double tol1 = this.relativeThreshold * MathLib.abs(x) + this.absoluteThreshold;
            final double tol2 = 2 * tol1;

            // Default stopping criterion.
            final boolean stop = MathLib.abs(x - m) <= tol2 - HALF * (b - a);
            if (stop) {
                // Default termination (Brent's criterion).
                return this.best(best,
                    this.best(previous,
                        current,
                        isMinim),
                    isMinim);
            } else {
                double p = 0;
                double q = 0;
                double r = 0;
                double u = 0;

                if (MathLib.abs(e) > tol1) {
                    // Fit parabola.
                    r = (x - w) * (fx - fv);
                    q = (x - v) * (fx - fw);
                    p = (x - v) * q - (x - w) * r;
                    q = 2 * (q - r);

                    if (q > 0) {
                        p = -p;
                    } else {
                        q = -q;
                    }

                    r = e;
                    e = d;

                    if (p > q * (a - x) &&
                        p < q * (b - x) &&
                        MathLib.abs(p) < MathLib.abs(HALF * q * r)) {
                        // Parabolic interpolation step.
                        d = p / q;
                        u = x + d;

                        // f must not be evaluated too close to a or b.
                        if (u - a < tol2 || b - u < tol2) {
                            if (x <= m) {
                                d = tol1;
                            } else {
                                d = -tol1;
                            }
                        }
                    } else {
                        // Golden section step.
                        if (x < m) {
                            e = b - x;
                        } else {
                            e = a - x;
                        }
                        d = GOLDEN_SECTION * e;
                    }
                } else {
                    // Golden section step.
                    if (x < m) {
                        e = b - x;
                    } else {
                        e = a - x;
                    }
                    d = GOLDEN_SECTION * e;
                }

                // Update by at least "tol1".
                if (MathLib.abs(d) < tol1) {
                    if (d >= 0) {
                        u = x + tol1;
                    } else {
                        u = x - tol1;
                    }
                } else {
                    u = x + d;
                }

                double fu = this.computeObjectiveValue(u);
                if (!isMinim) {
                    fu = -fu;
                }

                // User-defined convergence checker.
                previous = current;
                current = new UnivariatePointValuePair(u, isMinim ? fu : -fu);
                best = this.best(best,
                    this.best(previous,
                        current,
                        isMinim),
                    isMinim);

                if (checker != null) {
                    if (checker.converged(iter, previous, current)) {
                        return best;
                    }
                }

                // Update a, b, v, w and x.
                if (fu <= fx) {
                    if (u < x) {
                        b = x;
                    } else {
                        a = x;
                    }
                    v = w;
                    fv = fw;
                    w = x;
                    fw = fx;
                    x = u;
                    fx = fu;
                } else {
                    if (u < x) {
                        a = u;
                    } else {
                        b = u;
                    }
                    if (fu <= fw ||
                        Precision.equals(w, x)) {
                        v = w;
                        fv = fw;
                        w = u;
                        fw = fu;
                    } else if (fu <= fv ||
                        Precision.equals(v, x) ||
                        Precision.equals(v, w)) {
                        v = u;
                        fv = fu;
                    }
                }
            }
            ++iter;
        }
    }

    /**
     * Selects the best of two points.
     * 
     * @param a
     *        Point and value.
     * @param b
     *        Point and value.
     * @param isMinim
     *        {@code true} if the selected point must be the one with
     *        the lowest value.
     * @return the best point, or {@code null} if {@code a} and {@code b} are
     *         both {@code null}. When {@code a} and {@code b} have the same function
     *         value, {@code a} is returned.
     */
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Commons-Math code kept as such
    private UnivariatePointValuePair best(final UnivariatePointValuePair a,
                                          final UnivariatePointValuePair b,
                                          final boolean isMinim) {
        // CHECKSTYLE: resume ReturnCount check
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }

        if (isMinim) {
            return a.getValue() <= b.getValue() ? a : b;
        } else {
            return a.getValue() >= b.getValue() ? a : b;
        }
    }

    // CHECKSTYLE: resume CommentRatio check
}
