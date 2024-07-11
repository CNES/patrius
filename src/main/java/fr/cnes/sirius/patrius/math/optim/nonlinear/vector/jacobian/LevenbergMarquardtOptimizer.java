/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 *
 * Copyright 2011-2022 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.7:DM:DM-2766:18/05/2021:Evol. et corr. dans le package fr.cnes.sirius.patrius.math.linear (suite DM 2300) 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.optim.nonlinear.vector.jacobian;

import java.util.Arrays;

import fr.cnes.sirius.patrius.math.exception.ConvergenceException;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.optim.ConvergenceChecker;
import fr.cnes.sirius.patrius.math.optim.PointVectorValuePair;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

// CHECKSTYLE: stop MagicNumber check
// CHECKSTYLE: stop ModifiedControlVariable check
// Reason: Commons-Math code kept as such

/**
 * This class solves a least-squares problem using the Levenberg-Marquardt algorithm.
 * 
 * <p>
 * This implementation <em>should</em> work even for over-determined systems (i.e. systems having more point than
 * equations). Over-determined systems are solved by ignoring the point which have the smallest impact according to
 * their jacobian column norm. Only the rank of the matrix and some loop bounds are changed to implement this.
 * </p>
 * 
 * <p>
 * The resolution engine is a simple translation of the MINPACK <a
 * href="http://www.netlib.org/minpack/lmder.f">lmder</a> routine with minor changes. The changes include the
 * over-determined resolution, the use of inherited convergence checker and the Q.R. decomposition which has been
 * rewritten following the algorithm described in the P. Lascaux and R. Theodor book <i>Analyse num&eacute;rique
 * matricielle appliqu&eacute;e &agrave; l'art de l'ing&eacute;nieur</i>, Masson 1986.
 * </p>
 * <p>
 * The authors of the original fortran version are:
 * <ul>
 * <li>Argonne National Laboratory. MINPACK project. March 1980</li>
 * <li>Burton S. Garbow</li>
 * <li>Kenneth E. Hillstrom</li>
 * <li>Jorge J. More</li>
 * </ul>
 * The redistribution policy for MINPACK is available <a href="http://www.netlib.org/minpack/disclaimer">here</a>, for
 * convenience, it is reproduced below.
 * </p>
 * 
 * <table border="0" width="80%" cellpadding="10" align="center" bgcolor="#E0E0E0">
 * <tr>
 * <td>
 * Minpack Copyright Notice (1999) University of Chicago. All rights reserved</td>
 * </tr>
 * <tr>
 * <td>
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 * <ol>
 * <li>Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 * disclaimer.</li>
 * <li>Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 * following disclaimer in the documentation and/or other materials provided with the distribution.</li>
 * <li>The end-user documentation included with the redistribution, if any, must include the following acknowledgment:
 * <code>This product includes software developed by the University of
 *           Chicago, as Operator of Argonne National Laboratory.</code> Alternately, this acknowledgment may appear in
 * the software itself, if and wherever such third-party acknowledgments normally appear.</li>
 * <li><strong>WARRANTY DISCLAIMER. THE SOFTWARE IS SUPPLIED "AS IS" WITHOUT WARRANTY OF ANY KIND. THE COPYRIGHT HOLDER,
 * THE UNITED STATES, THE UNITED STATES DEPARTMENT OF ENERGY, AND THEIR EMPLOYEES: (1) DISCLAIM ANY WARRANTIES, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO ANY IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE,
 * TITLE OR NON-INFRINGEMENT, (2) DO NOT ASSUME ANY LEGAL LIABILITY OR RESPONSIBILITY FOR THE ACCURACY, COMPLETENESS, OR
 * USEFULNESS OF THE SOFTWARE, (3) DO NOT REPRESENT THAT USE OF THE SOFTWARE WOULD NOT INFRINGE PRIVATELY OWNED RIGHTS,
 * (4) DO NOT WARRANT THAT THE SOFTWARE WILL FUNCTION UNINTERRUPTED, THAT IT IS ERROR-FREE OR THAT ANY ERRORS WILL BE
 * CORRECTED.</strong></li>
 * <li><strong>LIMITATION OF LIABILITY. IN NO EVENT WILL THE COPYRIGHT HOLDER, THE UNITED STATES, THE UNITED STATES
 * DEPARTMENT OF ENERGY, OR THEIR EMPLOYEES: BE LIABLE FOR ANY INDIRECT, INCIDENTAL, CONSEQUENTIAL, SPECIAL OR PUNITIVE
 * DAMAGES OF ANY KIND OR NATURE, INCLUDING BUT NOT LIMITED TO LOSS OF PROFITS OR LOSS OF DATA, FOR ANY REASON
 * WHATSOEVER, WHETHER SUCH LIABILITY IS ASSERTED ON THE BASIS OF CONTRACT, TORT (INCLUDING NEGLIGENCE OR STRICT
 * LIABILITY), OR OTHERWISE, EVEN IF ANY OF SAID PARTIES HAS BEEN WARNED OF THE POSSIBILITY OF SUCH LOSS OR
 * DAMAGES.</strong></li>
 * <ol></td>
 * </tr>
 * </table>
 * 
 * @version $Id: LevenbergMarquardtOptimizer.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 2.0
 */
public class LevenbergMarquardtOptimizer
    extends AbstractLeastSquaresOptimizer {
    /** Number of solved point. */
    private int solvedCols;
    /** Diagonal elements of the R matrix in the Q.R. decomposition. */
    private double[] diagR;
    /** Norms of the columns of the jacobian matrix. */
    private double[] jacNorm;
    /** Coefficients of the Householder transforms vectors. */
    private double[] beta;
    /** Columns permutation array. */
    private int[] permutation;
    /** Rank of the jacobian matrix. */
    private int rank;
    /** Levenberg-Marquardt parameter. */
    private double lmPar;
    /** Parameters evolution direction associated with lmPar. */
    private double[] lmDir;
    /** Positive input variable used in determining the initial step bound. */
    private final double initialStepBoundFactor;
    /** Desired relative error in the sum of squares. */
    private final double costRelativeTolerance;
    /** Desired relative error in the approximate solution parameters. */
    private final double parRelativeTolerance;
    /**
     * Desired max cosine on the orthogonality between the function vector
     * and the columns of the jacobian.
     */
    private final double orthoTolerance;
    /** Threshold for QR ranking. */
    private final double qrRankingThreshold;
    /** Weighted Jacobian. */
    private double[][] weightedJacobian;

    /**
     * Build an optimizer for least squares problems with default values
     * for all the tuning parameters (see the {@link #LevenbergMarquardtOptimizer(double,double,double,double,double)
     * other contructor}.
     * The default values for the algorithm settings are:
     * <ul>
     * <li>Initial step bound factor: 100</li>
     * <li>Cost relative tolerance: 1e-10</li>
     * <li>Parameters relative tolerance: 1e-10</li>
     * <li>Orthogonality tolerance: 1e-10</li>
     * <li>QR ranking threshold: {@link Precision#SAFE_MIN}</li>
     * </ul>
     */
    public LevenbergMarquardtOptimizer() {
        this(100, 1e-10, 1e-10, 1e-10, Precision.SAFE_MIN);
    }

    /**
     * Constructor that allows the specification of a custom convergence
     * checker.
     * Note that all the usual convergence checks will be <em>disabled</em>.
     * The default values for the algorithm settings are:
     * <ul>
     * <li>Initial step bound factor: 100</li>
     * <li>Cost relative tolerance: 1e-10</li>
     * <li>Parameters relative tolerance: 1e-10</li>
     * <li>Orthogonality tolerance: 1e-10</li>
     * <li>QR ranking threshold: {@link Precision#SAFE_MIN}</li>
     * </ul>
     * 
     * @param checker
     *        Convergence checker.
     */
    public LevenbergMarquardtOptimizer(final ConvergenceChecker<PointVectorValuePair> checker) {
        this(100, checker, 1e-10, 1e-10, 1e-10, Precision.SAFE_MIN);
    }

    /**
     * Constructor that allows the specification of a custom convergence
     * checker, in addition to the standard ones.
     * 
     * @param initialStepBoundFactorIn
     *        Positive input variable used in
     *        determining the initial step bound. This bound is set to the
     *        product of initialStepBoundFactor and the euclidean norm of {@code diag * x} if non-zero, or else to
     *        {@code initialStepBoundFactor} itself. In most cases factor should lie in the interval
     *        {@code (0.1, 100.0)}. {@code 100} is a generally recommended value.
     * @param checker
     *        Convergence checker.
     * @param costRelativeToleranceIn
     *        Desired relative error in the sum of
     *        squares.
     * @param parRelativeToleranceIn
     *        Desired relative error in the approximate
     *        solution parameters.
     * @param orthoToleranceIn
     *        Desired max cosine on the orthogonality between
     *        the function vector and the columns of the Jacobian.
     * @param threshold
     *        Desired threshold for QR ranking. If the squared norm
     *        of a column vector is smaller or equal to this threshold during QR
     *        decomposition, it is considered to be a zero vector and hence the rank
     *        of the matrix is reduced.
     */
    public LevenbergMarquardtOptimizer(final double initialStepBoundFactorIn,
                                       final ConvergenceChecker<PointVectorValuePair> checker,
                                       final double costRelativeToleranceIn,
                                       final double parRelativeToleranceIn,
                                       final double orthoToleranceIn,
                                       final double threshold) {
        super(checker);
        this.initialStepBoundFactor = initialStepBoundFactorIn;
        this.costRelativeTolerance = costRelativeToleranceIn;
        this.parRelativeTolerance = parRelativeToleranceIn;
        this.orthoTolerance = orthoToleranceIn;
        this.qrRankingThreshold = threshold;
    }

    /**
     * Build an optimizer for least squares problems with default values
     * for some of the tuning parameters (see the
     * {@link #LevenbergMarquardtOptimizer(double,double,double,double,double)
     * other contructor}.
     * The default values for the algorithm settings are:
     * <ul>
     * <li>Initial step bound factor}: 100</li>
     * <li>QR ranking threshold}: {@link Precision#SAFE_MIN}</li>
     * </ul>
     * 
     * @param costRelativeToleranceIn
     *        Desired relative error in the sum of
     *        squares.
     * @param parRelativeToleranceIn
     *        Desired relative error in the approximate
     *        solution parameters.
     * @param orthoToleranceIn
     *        Desired max cosine on the orthogonality between
     *        the function vector and the columns of the Jacobian.
     */
    public LevenbergMarquardtOptimizer(final double costRelativeToleranceIn,
                                       final double parRelativeToleranceIn,
                                       final double orthoToleranceIn) {
        this(100,
                costRelativeToleranceIn, parRelativeToleranceIn, orthoToleranceIn,
                Precision.SAFE_MIN);
    }

    /**
     * The arguments control the behaviour of the default convergence checking
     * procedure.
     * Additional criteria can defined through the setting of a {@link ConvergenceChecker}.
     * 
     * @param initialStepBoundFactorIn
     *        Positive input variable used in
     *        determining the initial step bound. This bound is set to the
     *        product of initialStepBoundFactor and the euclidean norm of {@code diag * x} if non-zero, or else to
     *        {@code initialStepBoundFactor} itself. In most cases factor should lie in the interval
     *        {@code (0.1, 100.0)}. {@code 100} is a generally recommended value.
     * @param costRelativeToleranceIn
     *        Desired relative error in the sum of
     *        squares.
     * @param parRelativeToleranceIn
     *        Desired relative error in the approximate
     *        solution parameters.
     * @param orthoToleranceIn
     *        Desired max cosine on the orthogonality between
     *        the function vector and the columns of the Jacobian.
     * @param threshold
     *        Desired threshold for QR ranking. If the squared norm
     *        of a column vector is smaller or equal to this threshold during QR
     *        decomposition, it is considered to be a zero vector and hence the rank
     *        of the matrix is reduced.
     */
    public LevenbergMarquardtOptimizer(final double initialStepBoundFactorIn,
                                       final double costRelativeToleranceIn,
                                       final double parRelativeToleranceIn,
                                       final double orthoToleranceIn,
                                       final double threshold) {
        // No custom convergence criterion.
        super(null);
        this.initialStepBoundFactor = initialStepBoundFactorIn;
        this.costRelativeTolerance = costRelativeToleranceIn;
        this.parRelativeTolerance = parRelativeToleranceIn;
        this.orthoTolerance = orthoToleranceIn;
        this.qrRankingThreshold = threshold;
    }

    /** {@inheritDoc} */
    // CHECKSTYLE: stop MethodLength check
    // CHECKSTYLE: stop CyclomaticComplexity check
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Commons-Math code kept as such
    @Override
    @SuppressWarnings({"PMD.PrematureDeclaration", "PMD.AvoidArrayLoops"})
    protected PointVectorValuePair doOptimize() {
        // CHECKSTYLE: resume ReturnCount check
        // CHECKSTYLE: resume MethodLength check
        // CHECKSTYLE: resume CyclomaticComplexity check

        // Number of observed data.
        final int nR = this.getTarget().length;
        final double[] currentPoint = this.getStartPoint();
        // Number of parameters.
        final int nC = currentPoint.length;

        // arrays shared with the other private methods
        this.solvedCols = MathLib.min(nR, nC);
        this.diagR = new double[nC];
        this.jacNorm = new double[nC];
        this.beta = new double[nC];
        this.permutation = new int[nC];
        this.lmDir = new double[nC];

        // local point
        double delta = 0;
        double xNorm = 0;
        final double[] diag = new double[nC];
        final double[] oldX = new double[nC];
        double[] oldRes = new double[nR];
        double[] oldObj = new double[nR];
        final double[] qtf = new double[nR];
        final double[] work1 = new double[nC];
        final double[] work2 = new double[nC];
        final double[] work3 = new double[nC];

        final RealMatrix weightMatrixSqrt = this.getWeightSquareRoot();

        // Evaluate the function at the starting point and calculate its norm.
        double[] currentObjective = this.computeObjectiveValue(currentPoint);
        double[] currentResiduals = this.computeResiduals(currentObjective);
        PointVectorValuePair current = new PointVectorValuePair(currentPoint, currentObjective);
        double currentCost = this.computeCost(currentResiduals);

        // Outer loop.
        this.lmPar = 0;
        boolean firstIteration = true;
        int iter = 0;
        final ConvergenceChecker<PointVectorValuePair> checker = this.getConvergenceChecker();
        while (true) {
            ++iter;
            final PointVectorValuePair previous = current;

            // QR decomposition of the jacobian matrix
            this.qrDecomposition(this.computeWeightedJacobian(currentPoint));

            double[] weightedResidual = weightMatrixSqrt.operate(currentResiduals);
            System.arraycopy(weightedResidual, 0, qtf, 0, nR);

            // compute Qt.res
            this.qTy(qtf);

            // now we don't need Q anymore,
            // so let jacobian contain the R matrix with its diagonal elements
            for (int k = 0; k < this.solvedCols; ++k) {
                final int pk = this.permutation[k];
                this.weightedJacobian[k][pk] = this.diagR[pk];
            }

            if (firstIteration) {
                // scale the point according to the norms of the columns
                // of the initial jacobian
                xNorm = 0;
                for (int k = 0; k < nC; ++k) {
                    double dk = this.jacNorm[k];
                    if (dk == 0) {
                        dk = 1.0;
                    }
                    final double xk = dk * currentPoint[k];
                    xNorm += xk * xk;
                    diag[k] = dk;
                }
                xNorm = MathLib.sqrt(xNorm);

                // initialize the step bound delta
                delta = (xNorm == 0) ? this.initialStepBoundFactor : (this.initialStepBoundFactor * xNorm);
            }

            // check orthogonality between function vector and jacobian columns
            double maxCosine = 0;
            if (currentCost != 0) {
                for (int j = 0; j < this.solvedCols; ++j) {
                    final int pj = this.permutation[j];
                    final double s = this.jacNorm[pj];
                    if (s != 0) {
                        double sum = 0;
                        for (int i = 0; i <= j; ++i) {
                            sum += this.weightedJacobian[i][pj] * qtf[i];
                        }
                        maxCosine = MathLib.max(maxCosine, MathLib.abs(sum) / (s * currentCost));
                    }
                }
            }
            if (maxCosine <= this.orthoTolerance) {
                // Convergence has been reached.
                this.setCost(currentCost);
                return current;
            }

            // rescale if necessary
            for (int j = 0; j < nC; ++j) {
                diag[j] = MathLib.max(diag[j], this.jacNorm[j]);
            }

            // Inner loop.
            for (double ratio = 0; ratio < 1.0e-4;) {

                // save the state
                for (int j = 0; j < this.solvedCols; ++j) {
                    final int pj = this.permutation[j];
                    oldX[pj] = currentPoint[pj];
                }
                final double previousCost = currentCost;
                double[] tmpVec = weightedResidual;
                weightedResidual = oldRes;
                oldRes = tmpVec;
                tmpVec = currentObjective;
                currentObjective = oldObj;
                oldObj = tmpVec;

                // determine the Levenberg-Marquardt parameter
                this.determineLMParameter(qtf, delta, diag, work1, work2, work3);

                // compute the new point and the norm of the evolution direction
                double lmNorm = 0;
                for (int j = 0; j < this.solvedCols; ++j) {
                    final int pj = this.permutation[j];
                    this.lmDir[pj] = -this.lmDir[pj];
                    currentPoint[pj] = oldX[pj] + this.lmDir[pj];
                    final double s = diag[pj] * this.lmDir[pj];
                    lmNorm += s * s;
                }
                lmNorm = MathLib.sqrt(lmNorm);
                // on the first iteration, adjust the initial step bound.
                if (firstIteration) {
                    delta = MathLib.min(delta, lmNorm);
                }

                // Evaluate the function at x + p and calculate its norm.
                currentObjective = this.computeObjectiveValue(currentPoint);
                currentResiduals = this.computeResiduals(currentObjective);
                current = new PointVectorValuePair(currentPoint, currentObjective);
                currentCost = this.computeCost(currentResiduals);

                // compute the scaled actual reduction
                double actRed = -1.0;
                if (0.1 * currentCost < previousCost) {
                    final double r = currentCost / previousCost;
                    actRed = 1.0 - r * r;
                }

                // compute the scaled predicted reduction
                // and the scaled directional derivative
                for (int j = 0; j < this.solvedCols; ++j) {
                    final int pj = this.permutation[j];
                    final double dirJ = this.lmDir[pj];
                    work1[j] = 0;
                    for (int i = 0; i <= j; ++i) {
                        work1[i] += this.weightedJacobian[i][pj] * dirJ;
                    }
                }
                double coeff1 = 0;
                for (int j = 0; j < this.solvedCols; ++j) {
                    coeff1 += work1[j] * work1[j];
                }
                final double pc2 = previousCost * previousCost;
                coeff1 = coeff1 / pc2;
                final double coeff2 = this.lmPar * lmNorm * lmNorm / pc2;
                final double preRed = coeff1 + 2 * coeff2;
                final double dirDer = -(coeff1 + coeff2);

                // ratio of the actual to the predicted reduction
                ratio = (preRed == 0) ? 0 : (actRed / preRed);

                // update the step bound
                if (ratio <= 0.25) {
                    double tmp =
                        (actRed < 0) ? (0.5 * dirDer / (dirDer + 0.5 * actRed)) : 0.5;
                    if ((0.1 * currentCost >= previousCost) || (tmp < 0.1)) {
                        tmp = 0.1;
                    }
                    delta = tmp * MathLib.min(delta, 10.0 * lmNorm);
                    this.lmPar /= tmp;
                } else if ((this.lmPar == 0) || (ratio >= 0.75)) {
                    delta = 2 * lmNorm;
                    this.lmPar *= 0.5;
                }

                // test for successful iteration.
                if (ratio >= 1.0e-4) {
                    // successful iteration, update the norm
                    firstIteration = false;
                    xNorm = 0;
                    for (int k = 0; k < nC; ++k) {
                        final double xK = diag[k] * currentPoint[k];
                        xNorm += xK * xK;
                    }
                    xNorm = MathLib.sqrt(xNorm);

                    // tests for convergence.
                    if (checker != null) {
                        // we use the vectorial convergence checker
                        if (checker.converged(iter, previous, current)) {
                            this.setCost(currentCost);
                            return current;
                        }
                    }
                } else {
                    // failed iteration, reset the previous values
                    currentCost = previousCost;
                    for (int j = 0; j < this.solvedCols; ++j) {
                        final int pj = this.permutation[j];
                        currentPoint[pj] = oldX[pj];
                    }
                    tmpVec = weightedResidual;
                    weightedResidual = oldRes;
                    oldRes = tmpVec;
                    tmpVec = currentObjective;
                    currentObjective = oldObj;
                    oldObj = tmpVec;
                    // Reset "current" to previous values.
                    current = new PointVectorValuePair(currentPoint, currentObjective);
                }

                // Default convergence criteria.
                if ((MathLib.abs(actRed) <= this.costRelativeTolerance &&
                        preRed <= this.costRelativeTolerance &&
                        ratio <= 2.0) ||
                        delta <= this.parRelativeTolerance * xNorm) {
                    this.setCost(currentCost);
                    return current;
                }

                // tests for termination and stringent tolerances
                // (2.2204e-16 is the machine epsilon for IEEE754)
                if ((MathLib.abs(actRed) <= 2.2204e-16) && (preRed <= 2.2204e-16) && (ratio <= 2.0)) {
                    throw new ConvergenceException(PatriusMessages.TOO_SMALL_COST_RELATIVE_TOLERANCE,
                        this.costRelativeTolerance);
                } else if (delta <= 2.2204e-16 * xNorm) {
                    throw new ConvergenceException(PatriusMessages.TOO_SMALL_PARAMETERS_RELATIVE_TOLERANCE,
                        this.parRelativeTolerance);
                } else if (maxCosine <= 2.2204e-16) {
                    throw new ConvergenceException(PatriusMessages.TOO_SMALL_ORTHOGONALITY_TOLERANCE,
                        this.orthoTolerance);
                }
            }
        }
    }

    /**
     * Determine the Levenberg-Marquardt parameter.
     * <p>
     * This implementation is a translation in Java of the MINPACK <a
     * href="http://www.netlib.org/minpack/lmpar.f">lmpar</a> routine.
     * </p>
     * <p>
     * This method sets the lmPar and lmDir attributes.
     * </p>
     * <p>
     * The authors of the original fortran function are:
     * </p>
     * <ul>
     * <li>Argonne National Laboratory. MINPACK project. March 1980</li>
     * <li>Burton S. Garbow</li>
     * <li>Kenneth E. Hillstrom</li>
     * <li>Jorge J. More</li>
     * </ul>
     * <p>
     * Luc Maisonobe did the Java translation.
     * </p>
     * 
     * @param qy
     *        array containing qTy
     * @param delta
     *        upper bound on the euclidean norm of diagR * lmDir
     * @param diag
     *        diagonal matrix
     * @param work1
     *        work array
     * @param work2
     *        work array
     * @param work3
     *        work array
     */
    // CHECKSTYLE: stop MethodLength check
    // CHECKSTYLE: stop CyclomaticComplexity check
    // CHECKSTYLE: stop CommentRatio check
    // Reason: Commons-Math code kept as such
    private void determineLMParameter(final double[] qy, final double delta, final double[] diag,
                                      final double[] work1, final double[] work2, final double[] work3) {
        // CHECKSTYLE: resume CommentRatio check
        // CHECKSTYLE: resume MethodLength check
        // CHECKSTYLE: resume CyclomaticComplexity check

        final int nC = this.weightedJacobian[0].length;

        // compute and store in x the gauss-newton direction, if the
        // jacobian is rank-deficient, obtain a least squares solution
        for (int j = 0; j < this.rank; ++j) {
            this.lmDir[this.permutation[j]] = qy[j];
        }
        for (int j = this.rank; j < nC; ++j) {
            this.lmDir[this.permutation[j]] = 0;
        }
        for (int k = this.rank - 1; k >= 0; --k) {
            final int pk = this.permutation[k];
            final double ypk = this.lmDir[pk] / this.diagR[pk];
            for (int i = 0; i < k; ++i) {
                this.lmDir[this.permutation[i]] -= ypk * this.weightedJacobian[i][pk];
            }
            this.lmDir[pk] = ypk;
        }

        // evaluate the function at the origin, and test
        // for acceptance of the Gauss-Newton direction
        double dxNorm = 0;
        for (int j = 0; j < this.solvedCols; ++j) {
            final int pj = this.permutation[j];
            final double s = diag[pj] * this.lmDir[pj];
            work1[pj] = s;
            dxNorm += s * s;
        }
        dxNorm = MathLib.sqrt(dxNorm);
        double fp = dxNorm - delta;
        if (fp <= 0.1 * delta) {
            this.lmPar = 0;
            return;
        }

        // if the jacobian is not rank deficient, the Newton step provides
        // a lower bound, parl, for the zero of the function,
        // otherwise set this bound to zero
        double sum2;
        double parl = 0;
        if (this.rank == this.solvedCols) {
            for (int j = 0; j < this.solvedCols; ++j) {
                final int pj = this.permutation[j];
                work1[pj] *= diag[pj] / dxNorm;
            }
            sum2 = 0;
            for (int j = 0; j < this.solvedCols; ++j) {
                final int pj = this.permutation[j];
                double sum = 0;
                for (int i = 0; i < j; ++i) {
                    sum += this.weightedJacobian[i][pj] * work1[this.permutation[i]];
                }
                final double s = (work1[pj] - sum) / this.diagR[pj];
                work1[pj] = s;
                sum2 += s * s;
            }
            parl = fp / (delta * sum2);
        }

        // calculate an upper bound, paru, for the zero of the function
        sum2 = 0;
        for (int j = 0; j < this.solvedCols; ++j) {
            final int pj = this.permutation[j];
            double sum = 0;
            for (int i = 0; i <= j; ++i) {
                sum += this.weightedJacobian[i][pj] * qy[i];
            }
            sum /= diag[pj];
            sum2 += sum * sum;
        }
        final double gNorm = MathLib.sqrt(sum2);
        double paru = gNorm / delta;
        if (paru == 0) {
            // 2.2251e-308 is the smallest positive real for IEE754
            paru = 2.2251e-308 / MathLib.min(delta, 0.1);
        }

        // if the input par lies outside of the interval (parl,paru),
        // set par to the closer endpoint
        this.lmPar = MathLib.min(paru, MathLib.max(this.lmPar, parl));
        if (this.lmPar == 0) {
            this.lmPar = gNorm / dxNorm;
        }

        for (int countdown = 10; countdown >= 0; --countdown) {

            // evaluate the function at the current value of lmPar
            if (this.lmPar == 0) {
                this.lmPar = MathLib.max(2.2251e-308, 0.001 * paru);
            }
            final double sPar = MathLib.sqrt(this.lmPar);
            for (int j = 0; j < this.solvedCols; ++j) {
                final int pj = this.permutation[j];
                work1[pj] = sPar * diag[pj];
            }
            this.determineLMDirection(qy, work1, work2, work3);

            dxNorm = 0;
            for (int j = 0; j < this.solvedCols; ++j) {
                final int pj = this.permutation[j];
                final double s = diag[pj] * this.lmDir[pj];
                work3[pj] = s;
                dxNorm += s * s;
            }
            dxNorm = MathLib.sqrt(dxNorm);
            final double previousFP = fp;
            fp = dxNorm - delta;

            // if the function is small enough, accept the current value
            // of lmPar, also test for the exceptional cases where parl is zero
            if ((MathLib.abs(fp) <= 0.1 * delta) ||
                    ((parl == 0) && (fp <= previousFP) && (previousFP < 0))) {
                return;
            }

            // compute the Newton correction
            for (int j = 0; j < this.solvedCols; ++j) {
                final int pj = this.permutation[j];
                work1[pj] = work3[pj] * diag[pj] / dxNorm;
            }
            for (int j = 0; j < this.solvedCols; ++j) {
                final int pj = this.permutation[j];
                work1[pj] /= work2[j];
                final double tmp = work1[pj];
                for (int i = j + 1; i < this.solvedCols; ++i) {
                    work1[this.permutation[i]] -= this.weightedJacobian[i][pj] * tmp;
                }
            }
            sum2 = 0;
            for (int j = 0; j < this.solvedCols; ++j) {
                final double s = work1[this.permutation[j]];
                sum2 += s * s;
            }
            final double correction = fp / (delta * sum2);

            // depending on the sign of the function, update parl or paru.
            if (fp > 0) {
                parl = MathLib.max(parl, this.lmPar);
            } else if (fp < 0) {
                paru = MathLib.min(paru, this.lmPar);
            }

            // compute an improved estimate for lmPar
            this.lmPar = MathLib.max(parl, this.lmPar + correction);

        }
    }

    /**
     * Solve a*x = b and d*x = 0 in the least squares sense.
     * <p>
     * This implementation is a translation in Java of the MINPACK <a
     * href="http://www.netlib.org/minpack/qrsolv.f">qrsolv</a> routine.
     * </p>
     * <p>
     * This method sets the lmDir and lmDiag attributes.
     * </p>
     * <p>
     * The authors of the original fortran function are:
     * </p>
     * <ul>
     * <li>Argonne National Laboratory. MINPACK project. March 1980</li>
     * <li>Burton S. Garbow</li>
     * <li>Kenneth E. Hillstrom</li>
     * <li>Jorge J. More</li>
     * </ul>
     * <p>
     * Luc Maisonobe did the Java translation.
     * </p>
     * 
     * @param qy
     *        array containing qTy
     * @param diag
     *        diagonal matrix
     * @param lmDiag
     *        diagonal elements associated with lmDir
     * @param work
     *        work array
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Commons-Math code kept as such
    private void determineLMDirection(final double[] qy, final double[] diag,
                                      final double[] lmDiag, final double[] work) {
        // CHECKSTYLE: resume CyclomaticComplexity check

        // copy R and Qty to preserve input and initialize s
        // in particular, save the diagonal elements of R in lmDir
        for (int j = 0; j < this.solvedCols; ++j) {
            final int pj = this.permutation[j];
            for (int i = j + 1; i < this.solvedCols; ++i) {
                this.weightedJacobian[i][pj] = this.weightedJacobian[j][this.permutation[i]];
            }
            this.lmDir[j] = this.diagR[pj];
            work[j] = qy[j];
        }

        // eliminate the diagonal matrix d using a Givens rotation
        for (int j = 0; j < this.solvedCols; ++j) {

            // prepare the row of d to be eliminated, locating the
            // diagonal element using p from the Q.R. factorization
            final int pj = this.permutation[j];
            final double dpj = diag[pj];
            if (dpj != 0) {
                Arrays.fill(lmDiag, j + 1, lmDiag.length, 0);
            }
            lmDiag[j] = dpj;

            // the transformations to eliminate the row of d
            // modify only a single element of Qty
            // beyond the first n, which is initially zero.
            double qtbpj = 0;
            for (int k = j; k < this.solvedCols; ++k) {
                final int pk = this.permutation[k];

                // determine a Givens rotation which eliminates the
                // appropriate element in the current row of d
                if (lmDiag[k] != 0) {

                    final double sin;
                    final double cos;
                    final double rkk = this.weightedJacobian[k][pk];
                    if (MathLib.abs(rkk) < MathLib.abs(lmDiag[k])) {
                        final double cotan = rkk / lmDiag[k];
                        sin = 1.0 / MathLib.sqrt(1.0 + cotan * cotan);
                        cos = sin * cotan;
                    } else {
                        final double tan = lmDiag[k] / rkk;
                        cos = 1.0 / MathLib.sqrt(1.0 + tan * tan);
                        sin = cos * tan;
                    }

                    // compute the modified diagonal element of R and
                    // the modified element of (Qty,0)
                    this.weightedJacobian[k][pk] = cos * rkk + sin * lmDiag[k];
                    final double temp = cos * work[k] + sin * qtbpj;
                    qtbpj = -sin * work[k] + cos * qtbpj;
                    work[k] = temp;

                    // accumulate the tranformation in the row of s
                    for (int i = k + 1; i < this.solvedCols; ++i) {
                        final double rik = this.weightedJacobian[i][pk];
                        final double temp2 = cos * rik + sin * lmDiag[i];
                        lmDiag[i] = -sin * rik + cos * lmDiag[i];
                        this.weightedJacobian[i][pk] = temp2;
                    }
                }
            }

            // store the diagonal element of s and restore
            // the corresponding diagonal element of R
            lmDiag[j] = this.weightedJacobian[j][this.permutation[j]];
            this.weightedJacobian[j][this.permutation[j]] = this.lmDir[j];
        }

        // solve the triangular system for z, if the system is
        // singular, then obtain a least squares solution
        int nSing = this.solvedCols;
        for (int j = 0; j < this.solvedCols; ++j) {
            if ((lmDiag[j] == 0) && (nSing == this.solvedCols)) {
                nSing = j;
            }
            if (nSing < this.solvedCols) {
                work[j] = 0;
            }
        }
        if (nSing > 0) {
            for (int j = nSing - 1; j >= 0; --j) {
                final int pj = this.permutation[j];
                double sum = 0;
                for (int i = j + 1; i < nSing; ++i) {
                    sum += this.weightedJacobian[i][pj] * work[i];
                }
                work[j] = (work[j] - sum) / lmDiag[j];
            }
        }

        // permute the components of z back to components of lmDir
        for (int j = 0; j < this.lmDir.length; ++j) {
            this.lmDir[this.permutation[j]] = work[j];
        }
    }

    /**
     * Decompose a matrix A as A.P = Q.R using Householder transforms.
     * <p>
     * As suggested in the P. Lascaux and R. Theodor book <i>Analyse num&eacute;rique matricielle appliqu&eacute;e
     * &agrave; l'art de l'ing&eacute;nieur</i> (Masson, 1986), instead of representing the Householder transforms with
     * u<sub>k</sub> unit vectors such that:
     * 
     * <pre>
     * H<sub>k</sub> = I - 2u<sub>k</sub>.u<sub>k</sub><sup>t</sup>
     * </pre>
     * 
     * we use <sub>k</sub> non-unit vectors such that:
     * 
     * <pre>
     * H<sub>k</sub> = I - beta<sub>k</sub>v<sub>k</sub>.v<sub>k</sub><sup>t</sup>
     * </pre>
     * 
     * where v<sub>k</sub> = a<sub>k</sub> - alpha<sub>k</sub> e<sub>k</sub>. The beta<sub>k</sub> coefficients are
     * provided upon exit as recomputing them from the v<sub>k</sub> vectors would be costly.
     * </p>
     * <p>
     * This decomposition handles rank deficient cases since the tranformations are performed in non-increasing columns
     * norms order thanks to columns pivoting. The diagonal elements of the R matrix are therefore also in
     * non-increasing absolute values order.
     * </p>
     * 
     * @param jacobian
     *        Weighted Jacobian matrix at the current point.
     * @exception ConvergenceException
     *            if the decomposition cannot be performed
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Commons-Math code kept as such
    private void qrDecomposition(final RealMatrix jacobian) {
        // CHECKSTYLE: resume CyclomaticComplexity check

        // Code in this class assumes that the weighted Jacobian is -(W^(1/2) J),
        // hence the multiplication by -1.
        this.weightedJacobian = jacobian.scalarMultiply(-1).getData(false);

        // number of rows
        final int nR = this.weightedJacobian.length;
        // number of columns
        final int nC = this.weightedJacobian[0].length;

        // initializations
        for (int k = 0; k < nC; ++k) {
            this.permutation[k] = k;
            double norm2 = 0;
            for (int i = 0; i < nR; ++i) {
                final double akk = this.weightedJacobian[i][k];
                // evaluate square norm
                norm2 += akk * akk;
            }
            // fill the norms of the columns of the jacobian matrix
            this.jacNorm[k] = MathLib.sqrt(norm2);
        }

        // transform the matrix column after column
        for (int k = 0; k < nC; ++k) {

            // select the column with the greatest norm on active components
            int nextColumn = -1;
            double ak2 = Double.NEGATIVE_INFINITY;
            for (int i = k; i < nC; ++i) {
                double norm2 = 0;
                for (int j = k; j < nR; ++j) {
                    final double aki = this.weightedJacobian[j][this.permutation[i]];
                    norm2 += aki * aki;
                }
                if (Double.isInfinite(norm2) || Double.isNaN(norm2)) {
                    throw new ConvergenceException(PatriusMessages.UNABLE_TO_PERFORM_QR_DECOMPOSITION_ON_JACOBIAN,
                        nR, nC);
                }
                if (norm2 > ak2) {
                    nextColumn = i;
                    ak2 = norm2;
                }
            }
            // update the rank && return if ak2 is lower than the QR ranking
            if (ak2 <= this.qrRankingThreshold) {
                this.rank = k;
                return;
            }
            final int pk = this.permutation[nextColumn];
            this.permutation[nextColumn] = this.permutation[k];
            this.permutation[k] = pk;

            // choose alpha such that Hk.u = alpha ek
            final double akk = this.weightedJacobian[k][pk];
            final double alpha = (akk > 0) ? -MathLib.sqrt(ak2) : MathLib.sqrt(ak2);
            final double betak = 1.0 / (ak2 - akk * alpha);
            this.beta[pk] = betak;

            // transform the current column
            this.diagR[pk] = alpha;
            this.weightedJacobian[k][pk] -= alpha;

            // transform the remaining columns
            for (int dk = nC - 1 - k; dk > 0; --dk) {
                double gamma = 0;
                for (int j = k; j < nR; ++j) {
                    gamma += this.weightedJacobian[j][pk] * this.weightedJacobian[j][this.permutation[k + dk]];
                }
                gamma *= betak;
                for (int j = k; j < nR; ++j) {
                    this.weightedJacobian[j][this.permutation[k + dk]] -= gamma * this.weightedJacobian[j][pk];
                }
            }
        }
        this.rank = this.solvedCols;
    }

    /**
     * Compute the product Qt.y for some Q.R. decomposition.
     * 
     * @param y
     *        vector to multiply (will be overwritten with the result)
     */
    private void qTy(final double[] y) {
        // number of rows
        final int nR = this.weightedJacobian.length;
        // number of columns
        final int nC = this.weightedJacobian[0].length;

        // loop on the columns
        for (int k = 0; k < nC; ++k) {
            final int pk = this.permutation[k];
            double gamma = 0;
            for (int i = k; i < nR; ++i) {
                gamma += this.weightedJacobian[i][pk] * y[i];
            }
            gamma *= this.beta[pk];
            for (int i = k; i < nR; ++i) {
                // update y values
                y[i] -= gamma * this.weightedJacobian[i][pk];
            }
        }
    }

    // CHECKSTYLE: resume MagicNumber check
    // CHECKSTYLE: resume ModifiedControlVariable check
}
