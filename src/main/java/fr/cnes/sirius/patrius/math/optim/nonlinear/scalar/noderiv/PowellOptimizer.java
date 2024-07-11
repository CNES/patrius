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
package fr.cnes.sirius.patrius.math.optim.nonlinear.scalar.noderiv;

import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;
import fr.cnes.sirius.patrius.math.optim.ConvergenceChecker;
import fr.cnes.sirius.patrius.math.optim.MaxEval;
import fr.cnes.sirius.patrius.math.optim.PointValuePair;
import fr.cnes.sirius.patrius.math.optim.nonlinear.scalar.GoalType;
import fr.cnes.sirius.patrius.math.optim.nonlinear.scalar.MultivariateOptimizer;
import fr.cnes.sirius.patrius.math.optim.univariate.BracketFinder;
import fr.cnes.sirius.patrius.math.optim.univariate.BrentOptimizer;
import fr.cnes.sirius.patrius.math.optim.univariate.SearchInterval;
import fr.cnes.sirius.patrius.math.optim.univariate.SimpleUnivariateValueChecker;
import fr.cnes.sirius.patrius.math.optim.univariate.UnivariateObjectiveFunction;
import fr.cnes.sirius.patrius.math.optim.univariate.UnivariatePointValuePair;
import fr.cnes.sirius.patrius.math.util.MathArrays;
import fr.cnes.sirius.patrius.math.util.MathLib;

//CHECKSTYLE: stop CommentRatio check
//Reason: Commons-Math model kept as such
/**
 * Powell algorithm.
 * This code is translated and adapted from the Python version of this
 * algorithm (as implemented in module {@code optimize.py} v0.5 of <em>SciPy</em>). <br/>
 * The default stopping criterion is based on the differences of the
 * function value between two successive iterations. It is however possible
 * to define a custom convergence checker that might terminate the algorithm
 * earlier. <br/>
 * The internal line search optimizer is a {@link BrentOptimizer} with a
 * convergence checker set to {@link SimpleUnivariateValueChecker}.
 * 
 * @version $Id: PowellOptimizer.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 2.2
 */
public class PowellOptimizer
    extends MultivariateOptimizer {
    /**
     * Minimum relative tolerance.
     */
    private static final double MIN_RELATIVE_TOLERANCE = 2 * MathLib.ulp(1d);
    /**
     * Relative threshold.
     */
    private final double relativeThreshold;
    /**
     * Absolute threshold.
     */
    private final double absoluteThreshold;
    /**
     * Line search.
     */
    private final LineSearch line;

    /**
     * This constructor allows to specify a user-defined convergence checker,
     * in addition to the parameters that control the default convergence
     * checking procedure. <br/>
     * The internal line search tolerances are set to the square-root of their
     * corresponding value in the multivariate optimizer.
     * 
     * @param rel
     *        Relative threshold.
     * @param abs
     *        Absolute threshold.
     * @param checker
     *        Convergence checker.
     * @throws NotStrictlyPositiveException
     *         if {@code abs <= 0}.
     * @throws NumberIsTooSmallException
     *         if {@code rel < 2 * Math.ulp(1d)}.
     */
    public PowellOptimizer(final double rel,
        final double abs,
        final ConvergenceChecker<PointValuePair> checker) {
        this(rel, abs, MathLib.sqrt(rel), MathLib.sqrt(abs), checker);
    }

    /**
     * This constructor allows to specify a user-defined convergence checker,
     * in addition to the parameters that control the default convergence
     * checking procedure and the line search tolerances.
     * 
     * @param rel
     *        Relative threshold for this optimizer.
     * @param abs
     *        Absolute threshold for this optimizer.
     * @param lineRel
     *        Relative threshold for the internal line search optimizer.
     * @param lineAbs
     *        Absolute threshold for the internal line search optimizer.
     * @param checker
     *        Convergence checker.
     * @throws NotStrictlyPositiveException
     *         if {@code abs <= 0}.
     * @throws NumberIsTooSmallException
     *         if {@code rel < 2 * Math.ulp(1d)}.
     */
    public PowellOptimizer(final double rel,
        final double abs,
        final double lineRel,
        final double lineAbs,
        final ConvergenceChecker<PointValuePair> checker) {
        super(checker);

        if (rel < MIN_RELATIVE_TOLERANCE) {
            throw new NumberIsTooSmallException(rel, MIN_RELATIVE_TOLERANCE, true);
        }
        if (abs <= 0) {
            throw new NotStrictlyPositiveException(abs);
        }
        this.relativeThreshold = rel;
        this.absoluteThreshold = abs;

        // Create the line search optimizer.
        this.line = new LineSearch(lineRel,
            lineAbs);
    }

    /**
     * The parameters control the default convergence checking procedure. <br/>
     * The internal line search tolerances are set to the square-root of their
     * corresponding value in the multivariate optimizer.
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
    public PowellOptimizer(final double rel,
        final double abs) {
        this(rel, abs, null);
    }

    /**
     * Builds an instance with the default convergence checking procedure.
     * 
     * @param rel
     *        Relative threshold.
     * @param abs
     *        Absolute threshold.
     * @param lineRel
     *        Relative threshold for the internal line search optimizer.
     * @param lineAbs
     *        Absolute threshold for the internal line search optimizer.
     * @throws NotStrictlyPositiveException
     *         if {@code abs <= 0}.
     * @throws NumberIsTooSmallException
     *         if {@code rel < 2 * Math.ulp(1d)}.
     */
    public PowellOptimizer(final double rel,
        final double abs,
        final double lineRel,
        final double lineAbs) {
        this(rel, abs, lineRel, lineAbs, null);
    }

    /** {@inheritDoc} */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Commons-Math code kept as such
    @Override
    protected PointValuePair doOptimize() {
        // CHECKSTYLE: resume CyclomaticComplexity check
        final GoalType goal = this.getGoalType();
        final double[] guess = this.getStartPoint();
        final int n = guess.length;

        final double[][] direc = new double[n][n];
        for (int i = 0; i < n; i++) {
            direc[i][i] = 1;
        }

        final ConvergenceChecker<PointValuePair> checker = this.getConvergenceChecker();

        double[] x = guess;
        double fVal = this.computeObjectiveValue(x);
        double[] x1 = x.clone();
        int iter = 0;
        while (true) {
            ++iter;

            final double fX = fVal;
            double fX2 = 0;
            double delta = 0;
            int bigInd = 0;
            double alphaMin = 0;

            for (int i = 0; i < n; i++) {
                final double[] d = MathArrays.copyOf(direc[i]);

                fX2 = fVal;

                final UnivariatePointValuePair optimum = this.line.search(x, d);
                fVal = optimum.getValue();
                alphaMin = optimum.getPoint();
                final double[][] result = this.newPointAndDirection(x, d, alphaMin);
                x = result[0];

                if ((fX2 - fVal) > delta) {
                    delta = fX2 - fVal;
                    bigInd = i;
                }
            }

            // Default convergence check.
            boolean stop = 2 * (fX - fVal) <=
                (this.relativeThreshold * (MathLib.abs(fX) + MathLib.abs(fVal)) +
                this.absoluteThreshold);

            final PointValuePair previous = new PointValuePair(x1, fX);
            final PointValuePair current = new PointValuePair(x, fVal);
            if (!stop) {
                // User-defined stopping criteria.
                if (checker != null) {
                    stop = checker.converged(iter, previous, current);
                }
            }
            if (stop) {
                if (goal == GoalType.MINIMIZE) {
                    return (fVal < fX) ? current : previous;
                } else {
                    return (fVal > fX) ? current : previous;
                }
            }

            final double[] d = new double[n];
            final double[] x2 = new double[n];
            for (int i = 0; i < n; i++) {
                d[i] = x[i] - x1[i];
                x2[i] = 2 * x[i] - x1[i];
            }

            x1 = x.clone();
            fX2 = this.computeObjectiveValue(x2);

            if (fX > fX2) {
                double t = 2 * (fX + fX2 - 2 * fVal);
                double temp = fX - fVal - delta;
                t *= temp * temp;
                temp = fX - fX2;
                t -= delta * temp * temp;

                if (t < 0.0) {
                    final UnivariatePointValuePair optimum = this.line.search(x, d);
                    fVal = optimum.getValue();
                    alphaMin = optimum.getPoint();
                    final double[][] result = this.newPointAndDirection(x, d, alphaMin);
                    x = result[0];

                    final int lastInd = n - 1;
                    direc[bigInd] = direc[lastInd];
                    direc[lastInd] = result[1];
                }
            }
        }
    }

    /**
     * Compute a new point (in the original space) and a new direction
     * vector, resulting from the line search.
     * 
     * @param p
     *        Point used in the line search.
     * @param d
     *        Direction used in the line search.
     * @param optimum
     *        Optimum found by the line search.
     * @return a 2-element array containing the new point (at index 0) and
     *         the new direction (at index 1).
     */
    private double[][] newPointAndDirection(final double[] p,
                                            final double[] d,
                                            final double optimum) {
        final int n = p.length;
        final double[] nP = new double[n];
        final double[] nD = new double[n];
        for (int i = 0; i < n; i++) {
            nD[i] = d[i] * optimum;
            nP[i] = p[i] + nD[i];
        }

        final double[][] result = new double[2][];
        result[0] = nP;
        result[1] = nD;

        return result;
    }

    /**
     * Class for finding the minimum of the objective function along a given
     * direction.
     */
    private class LineSearch extends BrentOptimizer {
        /**
         * Value that will pass the precondition check for {@link BrentOptimizer} but will not pass the convergence
         * check, so that the custom checker
         * will always decide when to stop the line search.
         */
        private static final double REL_TOL_UNUSED = 1e-15;
        /**
         * Value that will pass the precondition check for {@link BrentOptimizer} but will not pass the convergence
         * check, so that the custom checker
         * will always decide when to stop the line search.
         */
        private static final double ABS_TOL_UNUSED = Double.MIN_VALUE;
        /**
         * Automatic bracketing.
         */
        private final BracketFinder bracket = new BracketFinder();

        /**
         * The "BrentOptimizer" default stopping criterion uses the tolerances
         * to check the domain (point) values, not the function values.
         * We thus create a custom checker to use function values.
         * 
         * @param rel
         *        Relative threshold.
         * @param abs
         *        Absolute threshold.
         */
        LineSearch(final double rel,
            final double abs) {
            super(REL_TOL_UNUSED,
                ABS_TOL_UNUSED,
                new SimpleUnivariateValueChecker(rel, abs));
        }

        /**
         * Find the minimum of the function {@code f(p + alpha * d)}.
         * 
         * @param p
         *        Starting point.
         * @param d
         *        Search direction.
         * @return the optimum.
         * @throws fr.cnes.sirius.patrius.math.exception.TooManyEvaluationsException
         *         if the number of evaluations is exceeded.
         */
        public UnivariatePointValuePair search(final double[] p, final double[] d) {
            final int n = p.length;
            final UnivariateFunction f = new UnivariateFunction(){
                /** {@inheritDoc} */
                @Override
                public double value(final double alpha) {
                    final double[] x = new double[n];
                    for (int i = 0; i < n; i++) {
                        x[i] = p[i] + alpha * d[i];
                    }
                    return PowellOptimizer.this.computeObjectiveValue(x);
                }
            };

            final GoalType goal = PowellOptimizer.this.getGoalType();
            this.bracket.search(f, goal, 0, 1);
            // Passing "MAX_VALUE" as a dummy value because it is the enclosing
            // class that counts the number of evaluations (and will eventually
            // generate the exception).
            return this.optimize(new MaxEval(Integer.MAX_VALUE),
                new UnivariateObjectiveFunction(f),
                goal,
                new SearchInterval(this.bracket.getLo(),
                    this.bracket.getHi(),
                    this.bracket.getMid()));
        }
    }

    // CHECKSTYLE: resume CommentRatio check
}
