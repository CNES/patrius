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
 * VERSION:4.5:DM:DM-2300:27/05/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.optim.nonlinear.vector.jacobian;

import fr.cnes.sirius.patrius.math.exception.ConvergenceException;
import fr.cnes.sirius.patrius.math.exception.MathInternalError;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.linear.ArrayRealVector;
import fr.cnes.sirius.patrius.math.linear.BlockRealMatrix;
import fr.cnes.sirius.patrius.math.linear.DecompositionSolver;
import fr.cnes.sirius.patrius.math.linear.LUDecomposition;
import fr.cnes.sirius.patrius.math.linear.QRDecomposition;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.linear.SingularMatrixException;
import fr.cnes.sirius.patrius.math.optim.ConvergenceChecker;
import fr.cnes.sirius.patrius.math.optim.PointVectorValuePair;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

//CHECKSTYLE: stop ModifiedControlVariable check
//Reason: Commons-Math code kept as such

/**
 * Gauss-Newton least-squares solver.
 * <p>
 * This class solve a least-square problem by solving the normal equations of the linearized problem at each iteration.
 * Either LU decomposition or QR decomposition can be used to solve the normal equations. LU decomposition is faster but
 * QR decomposition is more robust for difficult problems.
 * </p>
 * 
 * @version $Id: GaussNewtonOptimizer.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 2.0
 * 
 */
public class GaussNewtonOptimizer extends AbstractLeastSquaresOptimizer {
    /** Indicator for using LU decomposition. */
    private final boolean useLU;

    /**
     * Simple constructor with default settings.
     * The normal equations will be solved using LU decomposition.
     * 
     * @param checker
     *        Convergence checker.
     */
    public GaussNewtonOptimizer(final ConvergenceChecker<PointVectorValuePair> checker) {
        this(true, checker);
    }

    /**
     * @param useLUIn
     *        If {@code true}, the normal equations will be solved
     *        using LU decomposition, otherwise they will be solved using QR
     *        decomposition.
     * @param checker
     *        Convergence checker.
     */
    public GaussNewtonOptimizer(final boolean useLUIn,
                                final ConvergenceChecker<PointVectorValuePair> checker) {
        super(checker);
        this.useLU = useLUIn;
    }

    /** {@inheritDoc} */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Commons-Math code kept as such
    @Override
    @SuppressWarnings({ "PMD.PrematureDeclaration", "PMD.PreserveStackTrace" })
    public PointVectorValuePair doOptimize() {
        // CHECKSTYLE: resume CyclomaticComplexity check
        final ConvergenceChecker<PointVectorValuePair> checker = this.getConvergenceChecker();

        // Computation will be useless without a checker (see "for-loop").
        if (checker == null) {
            throw new NullArgumentException();
        }

        final double[] targetValues = this.getTarget();
        // Number of observed data.
        final int nR = targetValues.length;

        final RealMatrix weightMatrix = this.getWeight();
        // Diagonal of the weight matrix.
        final double[] residualsWeights = new double[nR];
        for (int i = 0; i < nR; i++) {
            residualsWeights[i] = weightMatrix.getEntry(i, i);
        }

        final double[] currentPoint = this.getStartPoint();
        final int nC = currentPoint.length;

        // iterate until convergence is reached
        PointVectorValuePair current = null;
        int iter = 0;
        for (boolean converged = false; !converged;) {
            ++iter;

            // evaluate the objective function and its jacobian
            final PointVectorValuePair previous = current;
            // Value of the objective function at "currentPoint".
            final double[] currentObjective = this.computeObjectiveValue(currentPoint);
            final double[] currentResiduals = this.computeResiduals(currentObjective);
            final RealMatrix weightedJacobian = this.computeWeightedJacobian(currentPoint);
            current = new PointVectorValuePair(currentPoint, currentObjective);

            // build the linear problem
            final double[] b = new double[nC];
            final double[][] a = new double[nC][nC];
            for (int i = 0; i < nR; ++i) {

                final double[] grad = weightedJacobian.getRow(i);
                final double weight = residualsWeights[i];
                final double residual = currentResiduals[i];

                // compute the normal equation
                final double wr = weight * residual;
                for (int j = 0; j < nC; ++j) {
                    b[j] += wr * grad[j];
                }

                // build the contribution matrix for measurement i
                for (int k = 0; k < nC; ++k) {
                    final double[] ak = a[k];
                    final double wgk = weight * grad[k];
                    for (int l = 0; l < nC; ++l) {
                        ak[l] += wgk * grad[l];
                    }
                }
            }

            try {
                // solve the linearized least squares problem
                final RealMatrix mA = new BlockRealMatrix(a);
                final DecompositionSolver solver = this.useLU ?
                    new LUDecomposition(mA).getSolver() :
                    new QRDecomposition(mA).getSolver();
                final double[] dX = solver.solve(new ArrayRealVector(b, false)).toArray();
                // update the estimated parameters
                for (int i = 0; i < nC; ++i) {
                    currentPoint[i] += dX[i];
                }
            } catch (final SingularMatrixException e) {
                throw new ConvergenceException(PatriusMessages.UNABLE_TO_SOLVE_SINGULAR_PROBLEM);
            }

            // Check convergence.
            if (previous != null) {
                converged = checker.converged(iter, previous, current);
                if (converged) {
                    this.setCost(this.computeCost(currentResiduals));
                    return current;
                }
            }
        }
        // Must never happen.
        throw new MathInternalError();
    }

    // CHECKSTYLE: resume ModifiedControlVariable check
}
