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
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.solver;

import fr.cnes.sirius.patrius.math.analysis.differentiation.DerivativeStructure;
import fr.cnes.sirius.patrius.math.analysis.differentiation.UnivariateDifferentiableFunction;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Implements <a href="http://mathworld.wolfram.com/NewtonsMethod.html">
 * Newton's Method</a> for finding zeros of real univariate differentiable
 * functions.
 * 
 * @since 3.1
 * @version $Id: NewtonRaphsonSolver.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class NewtonRaphsonSolver extends AbstractUnivariateDifferentiableSolver {
    /** Default absolute accuracy. */
    private static final double DEFAULT_ABSOLUTE_ACCURACY = 1e-6;

    /**
     * Construct a solver.
     */
    public NewtonRaphsonSolver() {
        this(DEFAULT_ABSOLUTE_ACCURACY);
    }

    /**
     * Construct a solver.
     * 
     * @param absoluteAccuracy
     *        Absolute accuracy.
     */
    public NewtonRaphsonSolver(final double absoluteAccuracy) {
        super(absoluteAccuracy);
    }

    /**
     * Find a zero near the midpoint of {@code min} and {@code max}.
     * 
     * @param f
     *        Function to solve.
     * @param min
     *        Lower bound for the interval.
     * @param max
     *        Upper bound for the interval.
     * @param maxEval
     *        Maximum number of evaluations.
     * @return the value where the function is zero.
     * @throws fr.cnes.sirius.patrius.math.exception.TooManyEvaluationsException
     *         if the maximum evaluation count is exceeded.
     * @throws fr.cnes.sirius.patrius.math.exception.NumberIsTooLargeException
     *         if {@code min >= max}.
     */
    @Override
    public double solve(final int maxEval, final UnivariateDifferentiableFunction f,
                        final double min, final double max) {
        return super.solve(maxEval, f, UnivariateSolverUtils.midpoint(min, max));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected double doSolve() {
        final double startValue = this.getStartValue();
        final double absoluteAccuracy = this.getAbsoluteAccuracy();

        double x0 = startValue;
        double x1;
        while (true) {
            final DerivativeStructure y0 = this.computeObjectiveValueAndDerivative(x0);
            x1 = x0 - (y0.getValue() / y0.getPartialDerivative(1));
            if (MathLib.abs(x1 - x0) <= absoluteAccuracy) {
                return x1;
            }

            x0 = x1;
        }
    }
}
