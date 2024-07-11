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
package fr.cnes.sirius.patrius.math.linear;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.MaxCountExceededException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.util.IterationManager;
import fr.cnes.sirius.patrius.math.util.MathUtils;

/**
 * This abstract class defines an iterative solver for the linear system A
 * &middot; x = b. In what follows, the <em>residual</em> r is defined as r = b
 * - A &middot; x, where A is the linear operator of the linear system, b is the
 * right-hand side vector, and x the current estimate of the solution.
 * 
 * @version $Id: IterativeLinearSolver.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.0
 */
//CHECKSTYLE: stop AbstractClassName check
@SuppressWarnings("PMD.AbstractNaming")
public abstract class IterativeLinearSolver {
    // CHECKSTYLE: resume AbstractClassName check

    /** The object in charge of managing the iterations. */
    private final IterationManager manager;

    /**
     * Creates a new instance of this class, with default iteration manager.
     * 
     * @param maxIterations
     *        the maximum number of iterations
     */
    public IterativeLinearSolver(final int maxIterations) {
        this.manager = new IterationManager(maxIterations);
    }

    /**
     * Creates a new instance of this class, with custom iteration manager.
     * 
     * @param managerIn
     *        the custom iteration manager
     * @throws NullArgumentException
     *         if {@code manager} is {@code null}
     */
    public IterativeLinearSolver(final IterationManager managerIn) {
        MathUtils.checkNotNull(managerIn);
        this.manager = managerIn;
    }

    /**
     * Performs all dimension checks on the parameters of {@link #solve(RealLinearOperator, RealVector, RealVector)
     * solve} and {@link #solveInPlace(RealLinearOperator, RealVector, RealVector) solveInPlace},
     * and throws an exception if one of the checks fails.
     * 
     * @param a
     *        the linear operator A of the system
     * @param b
     *        the right-hand side vector
     * @param x0
     *        the initial guess of the solution
     * @throws NullArgumentException
     *         if one of the parameters is {@code null}
     * @throws NonSquareOperatorException
     *         if {@code a} is not square
     * @throws DimensionMismatchException
     *         if {@code b} or {@code x0} have
     *         dimensions inconsistent with {@code a}
     */
    protected static void checkParameters(final RealLinearOperator a,
                                          final RealVector b, final RealVector x0) {
        MathUtils.checkNotNull(a);
        MathUtils.checkNotNull(b);
        MathUtils.checkNotNull(x0);
        if (a.getRowDimension() != a.getColumnDimension()) {
            throw new NonSquareOperatorException(a.getRowDimension(),
                a.getColumnDimension());
        }
        if (b.getDimension() != a.getRowDimension()) {
            throw new DimensionMismatchException(b.getDimension(),
                a.getRowDimension());
        }
        if (x0.getDimension() != a.getColumnDimension()) {
            throw new DimensionMismatchException(x0.getDimension(),
                a.getColumnDimension());
        }
    }

    /**
     * Returns the iteration manager attached to this solver.
     * 
     * @return the manager
     */
    public IterationManager getIterationManager() {
        return this.manager;
    }

    /**
     * Returns an estimate of the solution to the linear system A &middot; x =
     * b.
     * 
     * @param a
     *        the linear operator A of the system
     * @param b
     *        the right-hand side vector
     * @return a new vector containing the solution
     * @throws NullArgumentException
     *         if one of the parameters is {@code null}
     * @throws NonSquareOperatorException
     *         if {@code a} is not square
     * @throws DimensionMismatchException
     *         if {@code b} has dimensions
     *         inconsistent with {@code a}
     * @throws MaxCountExceededException
     *         at exhaustion of the iteration count,
     *         unless a custom {@link fr.cnes.sirius.patrius.math.util.Incrementor.MaxCountExceededCallback
     *         callback} has been set at construction of the {@link IterationManager}
     */
    public RealVector solve(final RealLinearOperator a, final RealVector b) {
        MathUtils.checkNotNull(a);
        final RealVector x = new ArrayRealVector(a.getColumnDimension());
        x.set(0.);
        return this.solveInPlace(a, b, x);
    }

    /**
     * Returns an estimate of the solution to the linear system A &middot; x =
     * b.
     * 
     * @param a
     *        the linear operator A of the system
     * @param b
     *        the right-hand side vector
     * @param x0
     *        the initial guess of the solution
     * @return a new vector containing the solution
     * @throws NullArgumentException
     *         if one of the parameters is {@code null}
     * @throws NonSquareOperatorException
     *         if {@code a} is not square
     * @throws DimensionMismatchException
     *         if {@code b} or {@code x0} have
     *         dimensions inconsistent with {@code a}
     * @throws MaxCountExceededException
     *         at exhaustion of the iteration count,
     *         unless a custom {@link fr.cnes.sirius.patrius.math.util.Incrementor.MaxCountExceededCallback
     *         callback} has been set at construction of the {@link IterationManager}
     */
    public RealVector solve(final RealLinearOperator a, final RealVector b, final RealVector x0) {
        MathUtils.checkNotNull(x0);
        return this.solveInPlace(a, b, x0.copy());
    }

    /**
     * Returns an estimate of the solution to the linear system A &middot; x =
     * b. The solution is computed in-place (initial guess is modified).
     * 
     * @param a
     *        the linear operator A of the system
     * @param b
     *        the right-hand side vector
     * @param x0
     *        initial guess of the solution
     * @return a reference to {@code x0} (shallow copy) updated with the
     *         solution
     * @throws NullArgumentException
     *         if one of the parameters is {@code null}
     * @throws NonSquareOperatorException
     *         if {@code a} is not square
     * @throws DimensionMismatchException
     *         if {@code b} or {@code x0} have
     *         dimensions inconsistent with {@code a}
     * @throws MaxCountExceededException
     *         at exhaustion of the iteration count,
     *         unless a custom {@link fr.cnes.sirius.patrius.math.util.Incrementor.MaxCountExceededCallback
     *         callback} has been set at construction of the {@link IterationManager}
     */
    public abstract RealVector solveInPlace(RealLinearOperator a, RealVector b,
                                            RealVector x0);
}
