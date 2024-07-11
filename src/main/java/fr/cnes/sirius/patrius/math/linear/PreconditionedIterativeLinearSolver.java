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
 * VERSION:4.5:DM:DM-2300:27/05/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear 
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
 * <p>
 * This abstract class defines preconditioned iterative solvers. When A is ill-conditioned, instead of solving system A
 * &middot; x = b directly, it is preferable to solve either <center> (M &middot; A) &middot; x = M &middot; b </center>
 * (left preconditioning), or <center> (A &middot; M) &middot; y = b, &nbsp;&nbsp;&nbsp;&nbsp;followed by M &middot; y =
 * x </center> (right preconditioning), where M approximates in some way A<sup>-1</sup>, while matrix-vector products of
 * the type M &middot; y remain comparatively easy to compute. In this library, M (not M<sup>-1</sup>!) is called the
 * <em>preconditionner</em>.
 * </p>
 * <p>
 * Concrete implementations of this abstract class must be provided with the preconditioner M, as a
 * {@link RealLinearOperator}.
 * </p>
 * 
 * @version $Id: PreconditionedIterativeLinearSolver.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.0
 */
//CHECKSTYLE: stop AbstractClassName check
@SuppressWarnings("PMD.AbstractNaming")
public abstract class PreconditionedIterativeLinearSolver
    extends IterativeLinearSolver {
    // CHECKSTYLE: resume AbstractClassName check

    /**
     * Creates a new instance of this class, with default iteration manager.
     * 
     * @param maxIterations
     *        the maximum number of iterations
     */
    public PreconditionedIterativeLinearSolver(final int maxIterations) {
        super(maxIterations);
    }

    /**
     * Creates a new instance of this class, with custom iteration manager.
     * 
     * @param manager
     *        the custom iteration manager
     * @throws NullArgumentException
     *         if {@code manager} is {@code null}
     */
    public PreconditionedIterativeLinearSolver(final IterationManager manager) {
        super(manager);
    }

    /**
     * Returns an estimate of the solution to the linear system A &middot; x =
     * b.
     * 
     * @param a
     *        the linear operator A of the system
     * @param m
     *        the preconditioner, M (can be {@code null})
     * @param b
     *        the right-hand side vector
     * @param x0
     *        the initial guess of the solution
     * @return a new vector containing the solution
     * @throws NullArgumentException
     *         if one of the parameters is {@code null}
     * @throws NonSquareOperatorException
     *         if {@code a} or {@code m} is not
     *         square
     * @throws DimensionMismatchException
     *         if {@code m}, {@code b} or {@code x0} have dimensions inconsistent with {@code a}
     * @throws MaxCountExceededException
     *         at exhaustion of the iteration count,
     *         unless a custom {@link fr.cnes.sirius.patrius.math.util.Incrementor.MaxCountExceededCallback
     *         callback} has been set at construction of the {@link IterationManager}
     */
    public RealVector solve(final RealLinearOperator a,
                            final RealLinearOperator m, final RealVector b, final RealVector x0) {
        MathUtils.checkNotNull(x0);
        return this.solveInPlace(a, m, b, x0.copy());
    }

    /** {@inheritDoc} */
    @Override
    public RealVector solve(final RealLinearOperator a, final RealVector b) {
        MathUtils.checkNotNull(a);
        final RealVector x = new ArrayRealVector(a.getColumnDimension());
        x.set(0.);
        return this.solveInPlace(a, null, b, x);
    }

    /** {@inheritDoc} */
    @Override
    public RealVector solve(final RealLinearOperator a, final RealVector b,
                            final RealVector x0) {
        MathUtils.checkNotNull(x0);
        return this.solveInPlace(a, null, b, x0.copy());
    }

    /**
     * Performs all dimension checks on the parameters of
     * {@link #solve(RealLinearOperator, RealLinearOperator, RealVector, RealVector) solve} and
     * {@link #solveInPlace(RealLinearOperator, RealLinearOperator, RealVector, RealVector) solveInPlace},
     * and throws an exception if one of the checks fails.
     * 
     * @param a
     *        the linear operator A of the system
     * @param m
     *        the preconditioner, M (can be {@code null})
     * @param b
     *        the right-hand side vector
     * @param x0
     *        the initial guess of the solution
     * @throws NullArgumentException
     *         if one of the parameters is {@code null}
     * @throws NonSquareOperatorException
     *         if {@code a} or {@code m} is not
     *         square
     * @throws DimensionMismatchException
     *         if {@code m}, {@code b} or {@code x0} have dimensions inconsistent with {@code a}
     */
    protected static void checkParameters(final RealLinearOperator a,
                                          final RealLinearOperator m, final RealVector b, final RealVector x0) {
        checkParameters(a, b, x0);
        if (m != null) {
            if (m.getColumnDimension() != m.getRowDimension()) {
                throw new NonSquareOperatorException(m.getColumnDimension(), m.getRowDimension());
            }
            if (m.getRowDimension() != a.getRowDimension()) {
                throw new DimensionMismatchException(m.getRowDimension(), a.getRowDimension());
            }
        }
    }

    /**
     * Returns an estimate of the solution to the linear system A &middot; x =
     * b.
     * 
     * @param a
     *        the linear operator A of the system
     * @param m
     *        the preconditioner, M (can be {@code null})
     * @param b
     *        the right-hand side vector
     * @return a new vector containing the solution
     * @throws NullArgumentException
     *         if one of the parameters is {@code null}
     * @throws NonSquareOperatorException
     *         if {@code a} or {@code m} is not
     *         square
     * @throws DimensionMismatchException
     *         if {@code m} or {@code b} have
     *         dimensions inconsistent with {@code a}
     * @throws MaxCountExceededException
     *         at exhaustion of the iteration count,
     *         unless a custom {@link fr.cnes.sirius.patrius.math.util.Incrementor.MaxCountExceededCallback
     *         callback} has been set at construction of the {@link IterationManager}
     */
    public RealVector solve(final RealLinearOperator a, final RealLinearOperator m,
                            final RealVector b) {
        MathUtils.checkNotNull(a);
        final RealVector x = new ArrayRealVector(a.getColumnDimension());
        return this.solveInPlace(a, m, b, x);
    }

    /**
     * Returns an estimate of the solution to the linear system A &middot; x =
     * b. The solution is computed in-place (initial guess is modified).
     * 
     * @param a
     *        the linear operator A of the system
     * @param m
     *        the preconditioner, M (can be {@code null})
     * @param b
     *        the right-hand side vector
     * @param x0
     *        the initial guess of the solution
     * @return a reference to {@code x0} (shallow copy) updated with the
     *         solution
     * @throws NullArgumentException
     *         if one of the parameters is {@code null}
     * @throws NonSquareOperatorException
     *         if {@code a} or {@code m} is not
     *         square
     * @throws DimensionMismatchException
     *         if {@code m}, {@code b} or {@code x0} have dimensions inconsistent with {@code a}
     * @throws MaxCountExceededException
     *         at exhaustion of the iteration count,
     *         unless a custom {@link fr.cnes.sirius.patrius.math.util.Incrementor.MaxCountExceededCallback
     *         callback} has been set at construction of the {@link IterationManager}
     */
    public abstract RealVector solveInPlace(RealLinearOperator a,
                                            RealLinearOperator m, RealVector b, RealVector x0);

    /** {@inheritDoc} */
    @Override
    public RealVector solveInPlace(final RealLinearOperator a,
                                   final RealVector b, final RealVector x0) {
        return this.solveInPlace(a, null, b, x0);
    }
}
