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
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.solver;

import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;

/**
 * Interface for (univariate real) rootfinding algorithms.
 * Implementations will search for only one zero in the given interval.
 * 
 * This class is not intended for use outside of the Apache Commons Math
 * library, regular user should rely on more specific interfaces like {@link UnivariateSolver}, {@link PolynomialSolver}
 * or {@link UnivariateDifferentiableSolver}.
 * 
 * @param <Func>
 *        Type of function to solve.
 * 
 * @since 3.0
 * @version $Id: BaseUnivariateSolver.java 18108 2017-10-04 06:45:27Z bignon $
 * @see UnivariateSolver
 * @see PolynomialSolver
 * @see UnivariateDifferentiableSolver
 */
public interface BaseUnivariateSolver<Func extends UnivariateFunction> {
    /**
     * Get the maximum number of function evaluations.
     * 
     * @return the maximum number of function evaluations.
     */
    int getMaxEvaluations();

    /**
     * Get the number of evaluations of the objective function.
     * The number of evaluations corresponds to the last call to the {@code optimize} method. It is 0 if the method has
     * not been
     * called yet.
     * 
     * @return the number of evaluations of the objective function.
     */
    int getEvaluations();

    /**
     * Get the absolute accuracy of the solver. Solutions returned by the
     * solver should be accurate to this tolerance, i.e., if &epsilon; is the
     * absolute accuracy of the solver and {@code v} is a value returned by
     * one of the {@code solve} methods, then a root of the function should
     * exist somewhere in the interval ({@code v} - &epsilon;, {@code v} + &epsilon;).
     * 
     * @return the absolute accuracy.
     */
    double getAbsoluteAccuracy();

    /**
     * Get the relative accuracy of the solver. The contract for relative
     * accuracy is the same as {@link #getAbsoluteAccuracy()}, but using
     * relative, rather than absolute error. If &rho; is the relative accuracy
     * configured for a solver and {@code v} is a value returned, then a root
     * of the function should exist somewhere in the interval
     * ({@code v} - &rho; {@code v}, {@code v} + &rho; {@code v}).
     * 
     * @return the relative accuracy.
     */
    double getRelativeAccuracy();

    /**
     * Get the function value accuracy of the solver. If {@code v} is
     * a value returned by the solver for a function {@code f},
     * then by contract, {@code |f(v)|} should be less than or equal to
     * the function value accuracy configured for the solver.
     * 
     * @return the function value accuracy.
     */
    double getFunctionValueAccuracy();

    /**
     * Solve for a zero root in the given interval.
     * A solver may require that the interval brackets a single zero root.
     * Solvers that do require bracketing should be able to handle the case
     * where one of the endpoints is itself a root.
     * 
     * @param maxEval
     *        Maximum number of evaluations.
     * @param f
     *        Function to solve.
     * @param min
     *        Lower bound for the interval.
     * @param max
     *        Upper bound for the interval.
     * @return a value where the function is zero.
     * @throws fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException
     *         if the arguments do not satisfy the requirements specified by the solver.
     * @throws fr.cnes.sirius.patrius.math.exception.TooManyEvaluationsException
     *         if
     *         the allowed number of evaluations is exceeded.
     */
    double solve(int maxEval, Func f, double min, double max);

    /**
     * Solve for a zero in the given interval, start at {@code startValue}.
     * A solver may require that the interval brackets a single zero root.
     * Solvers that do require bracketing should be able to handle the case
     * where one of the endpoints is itself a root.
     * 
     * @param maxEval
     *        Maximum number of evaluations.
     * @param f
     *        Function to solve.
     * @param min
     *        Lower bound for the interval.
     * @param max
     *        Upper bound for the interval.
     * @param startValue
     *        Start value to use.
     * @return a value where the function is zero.
     * @throws fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException
     *         if the arguments do not satisfy the requirements specified by the solver.
     * @throws fr.cnes.sirius.patrius.math.exception.TooManyEvaluationsException
     *         if
     *         the allowed number of evaluations is exceeded.
     */
    double solve(int maxEval, Func f, double min, double max, double startValue);

    /**
     * Solve for a zero in the vicinity of {@code startValue}.
     * 
     * @param f
     *        Function to solve.
     * @param startValue
     *        Start value to use.
     * @return a value where the function is zero.
     * @param maxEval
     *        Maximum number of evaluations.
     * @throws fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException
     *         if the arguments do not satisfy the requirements specified by the solver.
     * @throws fr.cnes.sirius.patrius.math.exception.TooManyEvaluationsException
     *         if
     *         the allowed number of evaluations is exceeded.
     */
    double solve(int maxEval, Func f, double startValue);
}
