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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.NotPositiveException;
import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;
import fr.cnes.sirius.patrius.math.exception.TooManyEvaluationsException;
import fr.cnes.sirius.patrius.math.linear.Array2DRowRealMatrix;
import fr.cnes.sirius.patrius.math.linear.EigenDecomposition;
import fr.cnes.sirius.patrius.math.linear.MatrixUtils;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.optim.ConvergenceChecker;
import fr.cnes.sirius.patrius.math.optim.OptimizationData;
import fr.cnes.sirius.patrius.math.optim.PointValuePair;
import fr.cnes.sirius.patrius.math.optim.nonlinear.scalar.GoalType;
import fr.cnes.sirius.patrius.math.optim.nonlinear.scalar.MultivariateOptimizer;
import fr.cnes.sirius.patrius.math.random.RandomGenerator;
import fr.cnes.sirius.patrius.math.util.MathArrays;

//CHECKSTYLE: stop MagicNumber check
//Reason: model - Commons-Math code

/**
 * <p>
 * An implementation of the active Covariance Matrix Adaptation Evolution Strategy (CMA-ES) for non-linear, non-convex,
 * non-smooth, global function minimization. The CMA-Evolution Strategy (CMA-ES) is a reliable stochastic optimization
 * method which should be applied if derivative-based methods, e.g. quasi-Newton BFGS or conjugate gradient, fail due to
 * a rugged search landscape (e.g. noise, local optima, outlier, etc.) of the objective function. Like a quasi-Newton
 * method, the CMA-ES learns and applies a variable metric on the underlying search space. Unlike a quasi-Newton method,
 * the CMA-ES neither estimates nor uses gradients, making it considerably more reliable in terms of finding a good, or
 * even close to optimal, solution.
 * </p>
 * 
 * <p>
 * In general, on smooth objective functions the CMA-ES is roughly ten times slower than BFGS (counting objective
 * function evaluations, no gradients provided). For up to <math>N=10</math> variables also the derivative-free simplex
 * direct search method (Nelder and Mead) can be faster, but it is far less reliable than CMA-ES.
 * </p>
 * 
 * <p>
 * The CMA-ES is particularly well suited for non-separable and/or badly conditioned problems. To observe the advantage
 * of CMA compared to a conventional evolution strategy, it will usually take about <math>30 N</math> function
 * evaluations. On difficult problems the complete optimization (a single run) is expected to take <em>roughly</em>
 * between <math>30 N</math> and <math>300 N<sup>2</sup></math> function evaluations.
 * </p>
 * 
 * <p>
 * This implementation is translated and adapted from the Matlab version of the CMA-ES algorithm as implemented in
 * module {@code cmaes.m} version 3.51.
 * </p>
 * 
 * For more information, please refer to the following links:
 * <ul>
 * <li><a href="http://www.lri.fr/~hansen/cmaes.m">Matlab code</a></li>
 * <li><a href="http://www.lri.fr/~hansen/cmaesintro.html">Introduction to CMA-ES</a></li>
 * <li><a href="http://en.wikipedia.org/wiki/CMA-ES">Wikipedia</a></li>
 * </ul>
 * 
 * @version $Id: CMAESOptimizer.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.0
 */
public class CMAESOptimizer
    extends MultivariateOptimizer {
    // global search parameters
    /**
     * Population size, offspring number. The primary strategy parameter to play
     * with, which can be increased from its default value. Increasing the
     * population size improves global search properties in exchange to speed.
     * Speed decreases, as a rule, at most linearly with increasing population
     * size. It is advisable to begin with the default small population size.
     */
    private int lambda;
    /**
     * Covariance update mechanism, default is active CMA. isActiveCMA = true
     * turns on "active CMA" with a negative update of the covariance matrix and
     * checks for positive definiteness. OPTS.CMA.active = 2 does not check for
     * pos. def. and is numerically faster. Active CMA usually speeds up the
     * adaptation.
     */
    private final boolean isActiveCMA;
    /**
     * Determines how often a new random offspring is generated in case it is
     * not feasible / beyond the defined limits, default is 0.
     */
    private final int checkFeasableCount;
    /**
     * @see Sigma
     */
    private double[] inputSigma;
    /** Number of objective variables/problem dimension */
    private int dimension;
    /**
     * Defines the number of initial iterations, where the covariance matrix
     * remains diagonal and the algorithm has internally linear time complexity.
     * diagonalOnly = 1 means keeping the covariance matrix always diagonal and
     * this setting also exhibits linear space complexity. This can be
     * particularly useful for dimension > 100.
     * 
     * @see <a href="http://hal.archives-ouvertes.fr/inria-00287367/en">A Simple Modification in CMA-ES</a>
     */
    private int diagonalOnly;
    /** Number of objective variables/problem dimension */
    private boolean isMinimize = true;
    /** Indicates whether statistic data is collected. */
    private final boolean generateStatistics;

    // termination criteria
    /** Maximal number of iterations allowed. */
    private final int maxIterations;
    /** Limit for fitness value. */
    private final double stopFitness;
    /** Stop if x-changes larger stopTolUpX. */
    private double stopTolUpX;
    /** Stop if x-change smaller stopTolX. */
    private double stopTolX;
    /** Stop if fun-changes smaller stopTolFun. */
    private double stopTolFun;
    /** Stop if back fun-changes smaller stopTolHistFun. */
    private double stopTolHistFun;

    // selection strategy parameters
    /** Number of parents/points for recombination. */
    private int mu;
    /** Array for weighted recombination. */
    private RealMatrix weights;
    /** Variance-effectiveness of sum w_i x_i. */
    private double mueff;

    // dynamic strategy parameters and constants
    /** Overall standard deviation - search volume. */
    private double sigma;
    /** Cumulation constant. */
    private double cc;
    /** Cumulation constant for step-size. */
    private double cs;
    /** Damping for step-size. */
    private double damps;
    /** Learning rate for rank-one update. */
    private double ccov1;
    /** Learning rate for rank-mu update' */
    private double ccovmu;
    /** Expectation of ||N(0,I)|| == norm(randn(N,1)). */
    private double chiN;
    /** Learning rate for rank-one update - diagonalOnly */
    private double ccov1Sep;
    /** Learning rate for rank-mu update - diagonalOnly */
    private double ccovmuSep;

    // CMA internal values - updated each generation
    /** Objective variables. */
    private RealMatrix xmean;
    /** Evolution path. */
    private RealMatrix pc;
    /** Evolution path for sigma. */
    private RealMatrix ps;
    /** Norm of ps, stored for efficiency. */
    private double normps;
    /** Coordinate system. */
    private RealMatrix b;
    /** Scaling. */
    private RealMatrix d;
    /** B*D, stored for efficiency. */
    private RealMatrix bd;
    /** Diagonal of sqrt(D), stored for efficiency. */
    private RealMatrix diagD;
    /** Covariance matrix. */
    private RealMatrix c;
    /** Diagonal of C, used for diagonalOnly. */
    private RealMatrix diagC;
    /** Number of iterations already performed. */
    private int iterations;

    /** History queue of best values. */
    private double[] fitnessHistory;

    /** Random generator. */
    private final RandomGenerator random;

    /** History of sigma values. */
    private final List<Double> statisticsSigmaHistory = new ArrayList<Double>();
    /** History of mean matrix. */
    private final List<RealMatrix> statisticsMeanHistory = new ArrayList<RealMatrix>();
    /** History of fitness values. */
    private final List<Double> statisticsFitnessHistory = new ArrayList<Double>();
    /** History of D matrix. */
    private final List<RealMatrix> statisticsDHistory = new ArrayList<RealMatrix>();

    /**
     * @param maxIterationsIn
     *        Maximal number of iterations.
     * @param stopFitnessIn
     *        Whether to stop if objective function value is smaller than {@code stopFitness}.
     * @param isActiveCMAIn
     *        Chooses the covariance matrix update method.
     * @param diagonalOnlyIn
     *        Number of initial iterations, where the covariance matrix
     *        remains diagonal.
     * @param checkFeasableCountIn
     *        Determines how often new random objective variables are
     *        generated in case they are out of bounds.
     * @param randomIn
     *        Random generator.
     * @param generateStatisticsIn
     *        Whether statistic data is collected.
     * @param checker
     *        Convergence checker.
     * 
     * @since 3.1
     */
    public CMAESOptimizer(final int maxIterationsIn,
        final double stopFitnessIn,
        final boolean isActiveCMAIn,
        final int diagonalOnlyIn,
        final int checkFeasableCountIn,
        final RandomGenerator randomIn,
        final boolean generateStatisticsIn,
        final ConvergenceChecker<PointValuePair> checker) {
        super(checker);
        this.maxIterations = maxIterationsIn;
        this.stopFitness = stopFitnessIn;
        this.isActiveCMA = isActiveCMAIn;
        this.diagonalOnly = diagonalOnlyIn;
        this.checkFeasableCount = checkFeasableCountIn;
        this.random = randomIn;
        this.generateStatistics = generateStatisticsIn;
    }

    /**
     * @return History of sigma values.
     */
    public List<Double> getStatisticsSigmaHistory() {
        return this.statisticsSigmaHistory;
    }

    /**
     * @return History of mean matrix.
     */
    public List<RealMatrix> getStatisticsMeanHistory() {
        return this.statisticsMeanHistory;
    }

    /**
     * @return History of fitness values.
     */
    public List<Double> getStatisticsFitnessHistory() {
        return this.statisticsFitnessHistory;
    }

    /**
     * @return History of D matrix.
     */
    public List<RealMatrix> getStatisticsDHistory() {
        return this.statisticsDHistory;
    }

    /**
     * {@inheritDoc}
     * 
     * @param optData
     *        Optimization data. The following data will be looked for:
     *        <ul>
     *        <li>{@link fr.cnes.sirius.patrius.math.optim.MaxEval}</li>
     *        <li>{@link fr.cnes.sirius.patrius.math.optim.InitialGuess}</li>
     *        <li>{@link fr.cnes.sirius.patrius.math.optim.SimpleBounds}</li>
     *        <li>{@link fr.cnes.sirius.patrius.math.optim.nonlinear.scalar.ObjectiveFunction}</li>
     *        <li>{@link Sigma}</li>
     *        <li>{@link PopulationSize}</li>
     *        </ul>
     * @return {@inheritDoc}
     * @throws TooManyEvaluationsException
     *         if the maximal number of
     *         evaluations is exceeded.
     * @throws DimensionMismatchException
     *         if the initial guess, target, and weight
     *         arguments have inconsistent dimensions.
     */
    @Override
    public PointValuePair optimize(final OptimizationData... optData) {
        // Retrieve settings.
        this.parseOptimizationData(optData);
        // Set up base class and perform computation.
        return super.optimize(optData);
    }

    /** {@inheritDoc} */
    // CHECKSTYLE: stop MethodLength check
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Commons-Math code kept as such
    @Override
    protected PointValuePair doOptimize() {
        // CHECKSTYLE: resume MethodLength check
        // CHECKSTYLE: resume CyclomaticComplexity check

        this.checkParameters();
        // -------------------- Initialization --------------------------------
        this.isMinimize = this.getGoalType().equals(GoalType.MINIMIZE);
        final FitnessFunction fitfun = new FitnessFunction();
        final double[] guess = this.getStartPoint();
        // number of objective variables/problem dimension
        this.dimension = guess.length;
        this.initializeCMA(guess);
        this.iterations = 0;
        double bestValue = fitfun.value(guess);
        push(this.fitnessHistory, bestValue);
        PointValuePair optimum = new PointValuePair(this.getStartPoint(),
            this.isMinimize ? bestValue : -bestValue);
        PointValuePair lastResult = null;

        // -------------------- Generation Loop --------------------------------

        generationLoop: for (this.iterations = 1; this.iterations <= this.maxIterations; this.iterations++) {
            // Generate and evaluate lambda offspring
            final RealMatrix arz = this.randn1(this.dimension, this.lambda);
            final RealMatrix arx = zeros(this.dimension, this.lambda);
            final double[] fitness = new double[this.lambda];
            // generate random offspring
            for (int k = 0; k < this.lambda; k++) {
                RealMatrix arxk = null;
                for (int i = 0; i < this.checkFeasableCount + 1; i++) {
                    if (this.diagonalOnly <= 0) {
                        // m + sig * Normal(0,C)
                        arxk = this.xmean.add(this.bd.multiply(arz.getColumnMatrix(k))
                            .scalarMultiply(this.sigma));
                    } else {
                        arxk = this.xmean.add(times(this.diagD, arz.getColumnMatrix(k))
                            .scalarMultiply(this.sigma));
                    }
                    if (i >= this.checkFeasableCount ||
                        fitfun.isFeasible(arxk.getColumn(0))) {
                        break;
                    }
                    // regenerate random arguments for row
                    arz.setColumn(k, this.randn(this.dimension));
                }
                copyColumn(arxk, 0, arx, k);
                try {
                    // compute fitness
                    fitness[k] = fitfun.value(arx.getColumn(k));
                } catch (final TooManyEvaluationsException e) {
                    break generationLoop;
                }
            }
            // Sort by fitness and compute weighted mean into xmean
            final int[] arindex = this.sortedIndices(fitness);
            // Calculate new xmean, this is selection and recombination
            // for speed up of Eq. (2) and (3)
            final RealMatrix xold = this.xmean;
            final RealMatrix bestArx = selectColumns(arx, MathArrays.copyOf(arindex, this.mu));
            this.xmean = bestArx.multiply(this.weights);
            final RealMatrix bestArz = selectColumns(arz, MathArrays.copyOf(arindex, this.mu));
            final RealMatrix zmean = bestArz.multiply(this.weights);
            final boolean hsig = this.updateEvolutionPaths(zmean, xold);
            if (this.diagonalOnly <= 0) {
                this.updateCovariance(hsig, bestArx, arz, arindex, xold);
            } else {
                this.updateCovarianceDiagonalOnly(hsig, bestArz);
            }
            // Adapt step size sigma - Eq. (5)
            this.sigma *= Math.exp(Math.min(1, (this.normps / this.chiN - 1) * this.cs / this.damps));
            final double bestFitness = fitness[arindex[0]];
            final double worstFitness = fitness[arindex[arindex.length - 1]];
            if (bestValue > bestFitness) {
                bestValue = bestFitness;
                lastResult = optimum;
                optimum = new PointValuePair(fitfun.repair(bestArx.getColumn(0)),
                    this.isMinimize ? bestFitness : -bestFitness);
                if (this.getConvergenceChecker() != null &&
                    lastResult != null) {
                    if (this.getConvergenceChecker().converged(this.iterations, optimum, lastResult)) {
                        break generationLoop;
                    }
                }
            }
            // handle termination criteria
            // Break, if fitness is good enough
            if (this.stopFitness != 0) {
                // only if stopFitness is defined
                if (bestFitness < (this.isMinimize ? this.stopFitness : -this.stopFitness)) {
                    break generationLoop;
                }
            }
            final double[] sqrtDiagC = sqrt(this.diagC).getColumn(0);
            final double[] pcCol = this.pc.getColumn(0);
            for (int i = 0; i < this.dimension; i++) {
                if (this.sigma * Math.max(Math.abs(pcCol[i]), sqrtDiagC[i]) > this.stopTolX) {
                    break;
                }
                if (i >= this.dimension - 1) {
                    break generationLoop;
                }
            }
            for (int i = 0; i < this.dimension; i++) {
                if (this.sigma * sqrtDiagC[i] > this.stopTolUpX) {
                    break generationLoop;
                }
            }
            final double historyBest = min(this.fitnessHistory);
            final double historyWorst = max(this.fitnessHistory);
            if (this.iterations > 2 &&
                Math.max(historyWorst, worstFitness) -
                    Math.min(historyBest, bestFitness) < this.stopTolFun) {
                break generationLoop;
            }
            if (this.iterations > this.fitnessHistory.length &&
                historyWorst - historyBest < this.stopTolHistFun) {
                break generationLoop;
            }
            // condition number of the covariance matrix exceeds 1e14
            if (max(this.diagD) / min(this.diagD) > 1e7) {
                break generationLoop;
            }
            // user defined termination
            if (this.getConvergenceChecker() != null) {
                final PointValuePair current = new PointValuePair(bestArx.getColumn(0),
                    this.isMinimize ? bestFitness : -bestFitness);
                if (lastResult != null &&
                    this.getConvergenceChecker().converged(this.iterations, current, lastResult)) {
                    break generationLoop;
                }
                lastResult = current;
            }
            // Adjust step size in case of equal function values (flat fitness)
            if (bestValue == fitness[arindex[(int) (0.1 + this.lambda / 4.)]]) {
                this.sigma = this.sigma * Math.exp(0.2 + this.cs / this.damps);
            }
            if (this.iterations > 2 && Math.max(historyWorst, bestFitness) -
                Math.min(historyBest, bestFitness) == 0) {
                this.sigma = this.sigma * Math.exp(0.2 + this.cs / this.damps);
            }
            // store best in history
            push(this.fitnessHistory, bestFitness);
            fitfun.setValueRange(worstFitness - bestFitness);
            if (this.generateStatistics) {
                this.statisticsSigmaHistory.add(this.sigma);
                this.statisticsFitnessHistory.add(bestFitness);
                this.statisticsMeanHistory.add(this.xmean.transpose());
                this.statisticsDHistory.add(this.diagD.transpose().scalarMultiply(1E5));
            }
        }
        return optimum;
    }

    /**
     * Scans the list of (required and optional) optimization data that
     * characterize the problem.
     * 
     * @param optData
     *        Optimization data. The following data will be looked for:
     *        <ul>
     *        <li>{@link Sigma}</li>
     *        <li>{@link PopulationSize}</li>
     *        </ul>
     */
    private void parseOptimizationData(final OptimizationData... optData) {
        // The existing values (as set by the previous call) are reused if
        // not provided in the argument list.
        for (final OptimizationData data : optData) {
            if (data instanceof Sigma) {
                this.inputSigma = ((Sigma) data).getSigma();
                continue;
            }
            if (data instanceof PopulationSize) {
                this.lambda = ((PopulationSize) data).getPopulationSize();
                continue;
            }
        }
    }

    /**
     * Checks dimensions and values of boundaries and inputSigma if defined.
     */
    private void checkParameters() {
        // Initialization : get initial guess, lower and upper bound
        final double[] init = this.getStartPoint();
        final double[] lB = this.getLowerBound();
        final double[] uB = this.getUpperBound();

        // Check if inputSigma is defined
        if (this.inputSigma != null) {
            if (this.inputSigma.length != init.length) {
                // Dimension mismatch
                throw new DimensionMismatchException(this.inputSigma.length, init.length);
            }
            for (int i = 0; i < init.length; i++) {
                if (this.inputSigma[i] > uB[i] - lB[i]) {
                    // inputSigma out of bounds
                    throw new OutOfRangeException(this.inputSigma[i], 0, uB[i] - lB[i]);
                }
            }
        }
    }

    /**
     * Initialization of the dynamic search parameters
     * 
     * @param guess
     *        Initial guess for the arguments of the fitness function.
     */
    private void initializeCMA(final double[] guess) {
        if (this.lambda <= 0) {
            throw new NotStrictlyPositiveException(this.lambda);
        }
        // initialize sigma
        final double[][] sigmaArray = new double[guess.length][1];
        for (int i = 0; i < guess.length; i++) {
            sigmaArray[i][0] = this.inputSigma[i];
        }
        final RealMatrix insigma = new Array2DRowRealMatrix(sigmaArray, false);
        // overall standard deviation
        this.sigma = max(insigma);

        // initialize termination criteria
        this.stopTolUpX = 1e3 * max(insigma);
        this.stopTolX = 1e-11 * max(insigma);
        this.stopTolFun = 1e-12;
        this.stopTolHistFun = 1e-13;

        // initialize selection strategy parameters
        // number of parents/points for recombination
        this.mu = this.lambda / 2;
        final double logMu2 = Math.log(this.mu + 0.5);
        this.weights = log(sequence(1, this.mu, 1)).scalarMultiply(-1).scalarAdd(logMu2);
        double sumw = 0;
        double sumwq = 0;
        for (int i = 0; i < this.mu; i++) {
            final double w = this.weights.getEntry(i, 0);
            sumw += w;
            sumwq += w * w;
        }
        this.weights = this.weights.scalarMultiply(1 / sumw);
        // variance-effectiveness of sum w_i x_i
        this.mueff = sumw * sumw / sumwq;

        // initialize dynamic strategy parameters and constants
        this.cc = (4 + this.mueff / this.dimension) /
            (this.dimension + 4 + 2 * this.mueff / this.dimension);
        this.cs = (this.mueff + 2) / (this.dimension + this.mueff + 3.);
        this.damps = (1 + 2 * Math.max(0, Math.sqrt((this.mueff - 1) /
            (this.dimension + 1)) - 1)) *
            Math.max(0.3,
                1 - this.dimension / (1e-6 + this.maxIterations)) + this.cs;
        this.ccov1 = 2 / ((this.dimension + 1.3) * (this.dimension + 1.3) + this.mueff);
        this.ccovmu = Math.min(1 - this.ccov1, 2 * (this.mueff - 2 + 1 / this.mueff) /
            ((this.dimension + 2) * (this.dimension + 2) + this.mueff));
        this.ccov1Sep = Math.min(1, this.ccov1 * (this.dimension + 1.5) / 3);
        this.ccovmuSep = Math.min(1 - this.ccov1, this.ccovmu * (this.dimension + 1.5) / 3);
        this.chiN = Math.sqrt(this.dimension) *
            (1 - 1 / ((double) 4 * this.dimension) + 1 / ((double) 21 * this.dimension * this.dimension));
        // intialize CMA internal values - updated each generation
        // objective variables
        this.xmean = MatrixUtils.createColumnRealMatrix(guess);
        this.diagD = insigma.scalarMultiply(1 / this.sigma);
        this.diagC = square(this.diagD);
        // evolution paths for C and sigma
        this.pc = zeros(this.dimension, 1);
        // B defines the coordinate system
        this.ps = zeros(this.dimension, 1);
        this.normps = this.ps.getFrobeniusNorm();

        this.b = eye(this.dimension, this.dimension);
        // diagonal D defines the scaling
        this.d = ones(this.dimension, 1);
        this.bd = times(this.b, repmat(this.diagD.transpose(), this.dimension, 1));
        // covariance
        this.c = this.b.multiply(diag(square(this.d)).multiply(this.b.transpose()));
        final int historySize = 10 + (int) (3 * 10 * this.dimension / (double) this.lambda);
        // history of fitness values
        this.fitnessHistory = new double[historySize];
        for (int i = 0; i < historySize; i++) {
            this.fitnessHistory[i] = Double.MAX_VALUE;
        }
    }

    /**
     * Update of the evolution paths ps and pc.
     * 
     * @param zmean
     *        Weighted row matrix of the gaussian random numbers generating
     *        the current offspring.
     * @param xold
     *        xmean matrix of the previous generation.
     * @return hsig flag indicating a small correction.
     */
    private boolean updateEvolutionPaths(final RealMatrix zmean, final RealMatrix xold) {
        this.ps = this.ps.scalarMultiply(1 - this.cs).add(
            this.b.multiply(zmean).scalarMultiply(
                Math.sqrt(this.cs * (2 - this.cs) * this.mueff)));
        this.normps = this.ps.getFrobeniusNorm();
        final boolean hsig = this.normps /
            Math.sqrt(1 - Math.pow(1 - this.cs, 2 * this.iterations)) /
            this.chiN < 1.4 + 2 / ((double) this.dimension + 1);
        this.pc = this.pc.scalarMultiply(1 - this.cc);
        if (hsig) {
            this.pc =
                this.pc.add(this.xmean.subtract(xold).scalarMultiply(
                    Math.sqrt(this.cc * (2 - this.cc) * this.mueff) / this.sigma));
        }
        return hsig;
    }

    /**
     * Update of the covariance matrix C for diagonalOnly > 0
     * 
     * @param hsig
     *        Flag indicating a small correction.
     * @param bestArz
     *        Fitness-sorted matrix of the gaussian random values of the
     *        current offspring.
     */
    private void updateCovarianceDiagonalOnly(final boolean hsig,
                                              final RealMatrix bestArz) {
        // minor correction if hsig==false
        double oldFac = hsig ? 0 : this.ccov1Sep * this.cc * (2 - this.cc);
        oldFac += 1 - this.ccov1Sep - this.ccovmuSep;
        // regard old matrix
        this.diagC = this.diagC.scalarMultiply(oldFac)
            // plus rank one update
            .add(square(this.pc).scalarMultiply(this.ccov1Sep))
            // plus rank mu update
            .add((times(this.diagC, square(bestArz).multiply(this.weights)))
                .scalarMultiply(this.ccovmuSep));
        // replaces eig(C)
        this.diagD = sqrt(this.diagC);
        if (this.diagonalOnly > 1 &&
            this.iterations > this.diagonalOnly) {
            // full covariance matrix from now on
            this.diagonalOnly = 0;
            this.b = eye(this.dimension, this.dimension);
            this.bd = diag(this.diagD);
            this.c = diag(this.diagC);
        }
    }

    /**
     * Update of the covariance matrix C.
     * 
     * @param hsig
     *        Flag indicating a small correction.
     * @param bestArx
     *        Fitness-sorted matrix of the argument vectors producing the
     *        current offspring.
     * @param arz
     *        Unsorted matrix containing the gaussian random values of the
     *        current offspring.
     * @param arindex
     *        Indices indicating the fitness-order of the current offspring.
     * @param xold
     *        xmean matrix of the previous generation.
     */
    private void updateCovariance(final boolean hsig, final RealMatrix bestArx,
                                  final RealMatrix arz, final int[] arindex,
                                  final RealMatrix xold) {
        double negccov = 0;
        if (this.ccov1 + this.ccovmu > 0) {
            final RealMatrix arpos = bestArx.subtract(repmat(xold, 1, this.mu))
                // mu difference vectors
                .scalarMultiply(1 / this.sigma);
            final RealMatrix roneu = this.pc.multiply(this.pc.transpose())
                // rank one update
                .scalarMultiply(this.ccov1);
            // minor correction if hsig==false
            double oldFac = hsig ? 0 : this.ccov1 * this.cc * (2 - this.cc);
            oldFac += 1 - this.ccov1 - this.ccovmu;
            if (this.isActiveCMA) {
                // Adapt covariance matrix C active CMA
                negccov = (1 - this.ccovmu) * 0.25 * this.mueff /
                    (Math.pow(this.dimension + 2, 1.5) + 2 * this.mueff);
                // keep at least 0.66 in all directions, small popsize are most
                // critical
                final double negminresidualvariance = 0.66;
                // where to make up for the variance loss
                final double negalphaold = 0.5;
                // prepare vectors, compute negative updating matrix Cneg
                final int[] arReverseIndex = reverse(arindex);
                RealMatrix arzneg = selectColumns(arz, MathArrays.copyOf(arReverseIndex, this.mu));
                RealMatrix arnorms = sqrt(sumRows(square(arzneg)));
                final int[] idxnorms = this.sortedIndices(arnorms.getRow(0));
                final RealMatrix arnormsSorted = selectColumns(arnorms, idxnorms);
                final int[] idxReverse = reverse(idxnorms);
                final RealMatrix arnormsReverse = selectColumns(arnorms, idxReverse);
                arnorms = divide(arnormsReverse, arnormsSorted);
                final int[] idxInv = inverse(idxnorms);
                final RealMatrix arnormsInv = selectColumns(arnorms, idxInv);
                // check and set learning rate negccov
                final double negcovMax = (1 - negminresidualvariance) /
                    square(arnormsInv).multiply(this.weights).getEntry(0, 0);
                if (negccov > negcovMax) {
                    negccov = negcovMax;
                }
                arzneg = times(arzneg, repmat(arnormsInv, this.dimension, 1));
                final RealMatrix artmp = this.bd.multiply(arzneg);
                final RealMatrix cneg = artmp.multiply(diag(this.weights)).multiply(artmp.transpose());
                oldFac += negalphaold * negccov;
                this.c = this.c.scalarMultiply(oldFac)
                    // regard old matrix
                    .add(roneu)
                    // plus rank one update
                    .add(arpos.scalarMultiply(
                        // plus rank mu update
                        this.ccovmu + (1 - negalphaold) * negccov)
                        .multiply(times(repmat(this.weights, 1, this.dimension),
                            arpos.transpose())))
                    .subtract(cneg.scalarMultiply(negccov));
            } else {
                // Adapt covariance matrix C - nonactive
                // regard old matrix
                this.c = this.c.scalarMultiply(oldFac)
                    // plus rank one update
                    .add(roneu)
                    // plus rank mu update
                    .add(arpos.scalarMultiply(this.ccovmu)
                        .multiply(times(repmat(this.weights, 1, this.dimension),
                            arpos.transpose())));
            }
        }
        this.updateBD(negccov);
    }

    /**
     * Update B and D from C.
     * 
     * @param negccov
     *        Negative covariance factor.
     */
    private void updateBD(final double negccov) {
        if (this.ccov1 + this.ccovmu + negccov > 0 &&
            (this.iterations % 1. / (this.ccov1 + this.ccovmu + negccov) / this.dimension / 10.) < 1) {
            // to achieve O(N^2)
            this.c = triu(this.c, 0).add(triu(this.c, 1).transpose());
            // enforce symmetry to prevent complex numbers
            final EigenDecomposition eig = new EigenDecomposition(this.c);
            // eigen decomposition, B==normalized eigenvectors
            this.b = eig.getV();
            this.d = eig.getD();
            this.diagD = diag(this.d);
            if (min(this.diagD) <= 0) {
                for (int i = 0; i < this.dimension; i++) {
                    if (this.diagD.getEntry(i, 0) < 0) {
                        this.diagD.setEntry(i, 0, 0);
                    }
                }
                final double tfac = max(this.diagD) / 1e14;
                this.c = this.c.add(eye(this.dimension, this.dimension).scalarMultiply(tfac));
                this.diagD = this.diagD.add(ones(this.dimension, 1).scalarMultiply(tfac));
            }
            if (max(this.diagD) > 1e14 * min(this.diagD)) {
                final double tfac = max(this.diagD) / 1e14 - min(this.diagD);
                this.c = this.c.add(eye(this.dimension, this.dimension).scalarMultiply(tfac));
                this.diagD = this.diagD.add(ones(this.dimension, 1).scalarMultiply(tfac));
            }
            this.diagC = diag(this.c);
            // D contains standard deviations now
            this.diagD = sqrt(this.diagD);
            // O(n^2)
            this.bd = times(this.b, repmat(this.diagD.transpose(), this.dimension, 1));
        }
    }

    /**
     * Pushes the current best fitness value in a history queue.
     * 
     * @param vals
     *        History queue.
     * @param val
     *        Current best fitness value.
     */
    @SuppressWarnings("PMD.AvoidArrayLoops")
    private static void push(final double[] vals, final double val) {
        for (int i = vals.length - 1; i > 0; i--) {
            vals[i] = vals[i - 1];
        }
        vals[0] = val;
    }

    /**
     * Sorts fitness values.
     * 
     * @param doubles
     *        Array of values to be sorted.
     * @return a sorted array of indices pointing into doubles.
     */
    private int[] sortedIndices(final double[] doubles) {
        // Initialize DoubleIndex array
        final DoubleIndex[] dis = new DoubleIndex[doubles.length];
        for (int i = 0; i < doubles.length; i++) {
            dis[i] = new DoubleIndex(doubles[i], i);
        }
        // Sort DoubleIndex array
        Arrays.sort(dis);
        // Get indices from sorted array
        final int[] indices = new int[doubles.length];
        for (int i = 0; i < doubles.length; i++) {
            indices[i] = dis[i].index;
        }
        return indices;
    }

    // -----Matrix utility functions similar to the Matlab build in functions------

    /**
     * @param m
     *        Input matrix
     * @return Matrix representing the element-wise logarithm of m.
     */
    private static RealMatrix log(final RealMatrix m) {
        final double[][] d = new double[m.getRowDimension()][m.getColumnDimension()];
        for (int r = 0; r < m.getRowDimension(); r++) {
            for (int c = 0; c < m.getColumnDimension(); c++) {
                d[r][c] = Math.log(m.getEntry(r, c));
            }
        }
        return new Array2DRowRealMatrix(d, false);
    }

    /**
     * @param m
     *        Input matrix.
     * @return Matrix representing the element-wise square root of m.
     */
    private static RealMatrix sqrt(final RealMatrix m) {
        final double[][] d = new double[m.getRowDimension()][m.getColumnDimension()];
        for (int r = 0; r < m.getRowDimension(); r++) {
            for (int c = 0; c < m.getColumnDimension(); c++) {
                d[r][c] = Math.sqrt(m.getEntry(r, c));
            }
        }
        return new Array2DRowRealMatrix(d, false);
    }

    /**
     * @param m
     *        Input matrix.
     * @return Matrix representing the element-wise square of m.
     */
    private static RealMatrix square(final RealMatrix m) {
        final double[][] d = new double[m.getRowDimension()][m.getColumnDimension()];
        for (int r = 0; r < m.getRowDimension(); r++) {
            for (int c = 0; c < m.getColumnDimension(); c++) {
                final double e = m.getEntry(r, c);
                d[r][c] = e * e;
            }
        }
        return new Array2DRowRealMatrix(d, false);
    }

    /**
     * @param m
     *        Input matrix 1.
     * @param n
     *        Input matrix 2.
     * @return the matrix where the elements of m and n are element-wise multiplied.
     */
    private static RealMatrix times(final RealMatrix m, final RealMatrix n) {
        final double[][] d = new double[m.getRowDimension()][m.getColumnDimension()];
        for (int r = 0; r < m.getRowDimension(); r++) {
            for (int c = 0; c < m.getColumnDimension(); c++) {
                d[r][c] = m.getEntry(r, c) * n.getEntry(r, c);
            }
        }
        return new Array2DRowRealMatrix(d, false);
    }

    /**
     * @param m
     *        Input matrix 1.
     * @param n
     *        Input matrix 2.
     * @return Matrix where the elements of m and n are element-wise divided.
     */
    private static RealMatrix divide(final RealMatrix m, final RealMatrix n) {
        final double[][] d = new double[m.getRowDimension()][m.getColumnDimension()];
        for (int r = 0; r < m.getRowDimension(); r++) {
            for (int c = 0; c < m.getColumnDimension(); c++) {
                d[r][c] = m.getEntry(r, c) / n.getEntry(r, c);
            }
        }
        return new Array2DRowRealMatrix(d, false);
    }

    /**
     * @param m
     *        Input matrix.
     * @param cols
     *        Columns to select.
     * @return Matrix representing the selected columns.
     */
    private static RealMatrix selectColumns(final RealMatrix m, final int[] cols) {
        final double[][] d = new double[m.getRowDimension()][cols.length];
        for (int r = 0; r < m.getRowDimension(); r++) {
            for (int c = 0; c < cols.length; c++) {
                d[r][c] = m.getEntry(r, cols[c]);
            }
        }
        return new Array2DRowRealMatrix(d, false);
    }

    /**
     * @param m
     *        Input matrix.
     * @param k
     *        Diagonal position.
     * @return Upper triangular part of matrix.
     */
    private static RealMatrix triu(final RealMatrix m, final int k) {
        final double[][] d = new double[m.getRowDimension()][m.getColumnDimension()];
        for (int r = 0; r < m.getRowDimension(); r++) {
            for (int c = 0; c < m.getColumnDimension(); c++) {
                d[r][c] = r <= c - k ? m.getEntry(r, c) : 0;
            }
        }
        return new Array2DRowRealMatrix(d, false);
    }

    /**
     * @param m
     *        Input matrix.
     * @return Row matrix representing the sums of the rows.
     */
    private static RealMatrix sumRows(final RealMatrix m) {
        // Initialize output data array
        final double[][] d = new double[1][m.getColumnDimension()];
        // Compute the sums of the rows
        for (int c = 0; c < m.getColumnDimension(); c++) {
            double sum = 0;
            for (int r = 0; r < m.getRowDimension(); r++) {
                sum += m.getEntry(r, c);
            }
            d[0][c] = sum;
        }
        // Convert data arrays into matrix
        return new Array2DRowRealMatrix(d, false);
    }

    /**
     * @param m
     *        Input matrix.
     * @return the diagonal n-by-n matrix if m is a column matrix or the column
     *         matrix representing the diagonal if m is a n-by-n matrix.
     */
    private static RealMatrix diag(final RealMatrix m) {
        if (m.getColumnDimension() == 1) {
            // m is a column matrix
            final double[][] d = new double[m.getRowDimension()][m.getRowDimension()];
            // Create output d as diagonal n-by-n matrix with data from m
            for (int i = 0; i < m.getRowDimension(); i++) {
                d[i][i] = m.getEntry(i, 0);
            }
            return new Array2DRowRealMatrix(d, false);
        } else {
            // m is a n-by-n matrix
            final double[][] d = new double[m.getRowDimension()][1];
            // Create output d as column matrix representing the diagonal of m
            for (int i = 0; i < m.getColumnDimension(); i++) {
                d[i][0] = m.getEntry(i, i);
            }
            return new Array2DRowRealMatrix(d, false);
        }
    }

    /**
     * Copies a column from m1 to m2.
     * 
     * @param m1
     *        Source matrix.
     * @param col1
     *        Source column.
     * @param m2
     *        Target matrix.
     * @param col2
     *        Target column.
     */
    private static void copyColumn(final RealMatrix m1, final int col1,
                                   final RealMatrix m2, final int col2) {
        for (int i = 0; i < m1.getRowDimension(); i++) {
            m2.setEntry(i, col2, m1.getEntry(i, col1));
        }
    }

    /**
     * @param n
     *        Number of rows.
     * @param m
     *        Number of columns.
     * @return n-by-m matrix filled with 1.
     */
    private static RealMatrix ones(final int n, final int m) {
        final double[][] d = new double[n][m];
        for (int r = 0; r < n; r++) {
            Arrays.fill(d[r], 1);
        }
        return new Array2DRowRealMatrix(d, false);
    }

    /**
     * @param n
     *        Number of rows.
     * @param m
     *        Number of columns.
     * @return n-by-m matrix of 0 values out of diagonal, and 1 values on
     *         the diagonal.
     */
    private static RealMatrix eye(final int n, final int m) {
        final double[][] d = new double[n][m];
        for (int r = 0; r < n; r++) {
            if (r < m) {
                d[r][r] = 1;
            }
        }
        return new Array2DRowRealMatrix(d, false);
    }

    /**
     * @param n
     *        Number of rows.
     * @param m
     *        Number of columns.
     * @return n-by-m matrix of zero values.
     */
    private static RealMatrix zeros(final int n, final int m) {
        return new Array2DRowRealMatrix(n, m);
    }

    /**
     * @param mat
     *        Input matrix.
     * @param n
     *        Number of row replicates.
     * @param m
     *        Number of column replicates.
     * @return a matrix which replicates the input matrix in both directions.
     */
    private static RealMatrix repmat(final RealMatrix mat, final int n, final int m) {
        // Get row dimension
        final int rd = mat.getRowDimension();
        // Get column dimension
        final int cd = mat.getColumnDimension();
        // Initialize and complete a matrix replicating the input matrix in both directions
        final double[][] d = new double[n * rd][m * cd];
        for (int r = 0; r < n * rd; r++) {
            for (int c = 0; c < m * cd; c++) {
                d[r][c] = mat.getEntry(r % rd, c % cd);
            }
        }
        return new Array2DRowRealMatrix(d, false);
    }

    /**
     * @param start
     *        Start value.
     * @param end
     *        End value.
     * @param step
     *        Step size.
     * @return a sequence as column matrix.
     */
    private static RealMatrix sequence(final double start, final double end, final double step) {
        // Compute size of column matrix
        final int size = (int) ((end - start) / step + 1);
        // Initialize column data 2D array
        final double[][] d = new double[size][1];
        double value = start;
        for (int r = 0; r < size; r++) {
            d[r][0] = value;
            value += step;
        }
        // Convert 2D array into column matrix
        return new Array2DRowRealMatrix(d, false);
    }

    /**
     * @param m
     *        Input matrix.
     * @return the maximum of the matrix element values.
     */
    private static double max(final RealMatrix m) {
        // Initialization
        double max = -Double.MAX_VALUE;
        // Loop on all rows and columns of m
        for (int r = 0; r < m.getRowDimension(); r++) {
            for (int c = 0; c < m.getColumnDimension(); c++) {
                final double e = m.getEntry(r, c);
                if (max < e) {
                    // e is the new maximum
                    max = e;
                }
            }
        }
        return max;
    }

    /**
     * @param m
     *        Input matrix.
     * @return the minimum of the matrix element values.
     */
    private static double min(final RealMatrix m) {
        // Initialization
        double min = Double.MAX_VALUE;
        // Loop to find min
        for (int r = 0; r < m.getRowDimension(); r++) {
            for (int c = 0; c < m.getColumnDimension(); c++) {
                final double e = m.getEntry(r, c);
                if (min > e) {
                    min = e;
                }
            }
        }
        // Return result
        return min;
    }

    /**
     * @param m
     *        Input array.
     * @return the maximum of the array values.
     */
    private static double max(final double[] m) {
        double max = -Double.MAX_VALUE;
        for (final double element : m) {
            if (max < element) {
                max = element;
            }
        }
        return max;
    }

    /**
     * @param m
     *        Input array.
     * @return the minimum of the array values.
     */
    private static double min(final double[] m) {
        double min = Double.MAX_VALUE;
        for (final double element : m) {
            if (min > element) {
                min = element;
            }
        }
        return min;
    }

    /**
     * @param indices
     *        Input index array.
     * @return the inverse of the mapping defined by indices.
     */
    private static int[] inverse(final int[] indices) {
        final int[] inverse = new int[indices.length];
        for (int i = 0; i < indices.length; i++) {
            inverse[indices[i]] = i;
        }
        return inverse;
    }

    /**
     * @param indices
     *        Input index array.
     * @return the indices in inverse order (last is first).
     */
    private static int[] reverse(final int[] indices) {
        final int[] reverse = new int[indices.length];
        for (int i = 0; i < indices.length; i++) {
            reverse[i] = indices[indices.length - i - 1];
        }
        return reverse;
    }

    /**
     * @param size
     *        Length of random array.
     * @return an array of Gaussian random numbers.
     */
    private double[] randn(final int size) {
        final double[] randn = new double[size];
        for (int i = 0; i < size; i++) {
            randn[i] = this.random.nextGaussian();
        }
        return randn;
    }

    /**
     * @param size
     *        Number of rows.
     * @param popSize
     *        Population size.
     * @return a 2-dimensional matrix of Gaussian random numbers.
     */
    private RealMatrix randn1(final int size, final int popSize) {
        final double[][] data = new double[size][popSize];
        for (int r = 0; r < size; r++) {
            for (int j = 0; j < popSize; j++) {
                data[r][j] = this.random.nextGaussian();
            }
        }
        return new Array2DRowRealMatrix(data, false);
    }


    /**
     * Input sigma values.
     * They define the initial coordinate-wise standard deviations for
     * sampling new search points around the initial guess.
     * It is suggested to set them to the estimated distance from the
     * initial to the desired optimum.
     * Small values induce the search to be more local (and very small
     * values are more likely to find a local optimum close to the initial
     * guess).
     * Too small values might however lead to early termination.
     */
    public static class Sigma implements OptimizationData {

        /** Sigma values. */
        private final double[] sigmas;

        /**
         * @param s
         *        Sigma values.
         * @throws NotPositiveException
         *         if any of the array entries is smaller
         *         than zero.
         */
        public Sigma(final double[] s) {
            for (final double element : s) {
                if (element < 0) {
                    throw new NotPositiveException(element);
                }
            }

            this.sigmas = s.clone();
        }

        /**
         * @return the sigma values.
         */
        public double[] getSigma() {
            return this.sigmas.clone();
        }
    }

    /**
     * Population size.
     * The number of offspring is the primary strategy parameter.
     * In the absence of better clues, a good default could be an
     * integer close to {@code 4 + 3 ln(n)}, where {@code n} is the
     * number of optimized parameters.
     * Increasing the population size improves global search properties
     * at the expense of speed (which in general decreases at most
     * linearly with increasing population size).
     */
    public static class PopulationSize implements OptimizationData {
        /** Population size. */
        private final int lambda;

        /**
         * @param size
         *        Population size.
         * @throws NotStrictlyPositiveException
         *         if {@code size <= 0}.
         */
        public PopulationSize(final int size) {
            if (size <= 0) {
                throw new NotStrictlyPositiveException(size);
            }
            this.lambda = size;
        }

        /**
         * @return the population size.
         */
        public int getPopulationSize() {
            return this.lambda;
        }
    }


    /**
     * Used to sort fitness values. Sorting is always in lower value first
     * order.
     */
    private static class DoubleIndex implements Comparable<DoubleIndex> {
        /** Value to compare. */
        private final double value;
        /** Index into sorted array. */
        private final int index;

        /**
         * @param valueIn
         *        Value to compare.
         * @param indexIn
         *        Index into sorted array.
         */
        DoubleIndex(final double valueIn, final int indexIn) {
            this.value = valueIn;
            this.index = indexIn;
        }

        /** {@inheritDoc} */
        @Override
        public int compareTo(final DoubleIndex o) {
            return Double.compare(this.value, o.value);
        }

        /** {@inheritDoc} */
        @Override
        public boolean equals(final Object other) {

            if (this == other) {
                return true;
            }

            if (other instanceof DoubleIndex) {
                return Double.compare(this.value, ((DoubleIndex) other).value) == 0;
            }

            return false;
        }

        /** {@inheritDoc} */
        @Override
        public int hashCode() {
            final long bits = Double.doubleToLongBits(this.value);
            return (int) ((1438542 ^ (bits >>> 32) ^ bits) & 0xffffffff);
        }
    }

    /**
     * Normalizes fitness values to the range [0,1]. Adds a penalty to the
     * fitness value if out of range. The penalty is adjusted by calling
     * setValueRange().
     */
    private class FitnessFunction {
        /** Determines the penalty for boundary violations */
        private double valueRange;
        /**
         * Flag indicating whether the objective variables are forced into their
         * bounds if defined
         */
        private final boolean isRepairMode;

        /**
         * Simple constructor.
         */
        public FitnessFunction() {
            this.valueRange = 1;
            this.isRepairMode = true;
        }

        /**
         * @param point
         *        Normalized objective variables.
         * @return the objective value + penalty for violated bounds.
         */
        public double value(final double[] point) {
            final double value;
            if (this.isRepairMode) {
                final double[] repaired = this.repair(point);
                value = CMAESOptimizer.this.computeObjectiveValue(repaired) +
                    this.penalty(point, repaired);
            } else {
                value = CMAESOptimizer.this.computeObjectiveValue(point);
            }
            return CMAESOptimizer.this.isMinimize ? value : -value;
        }

        /**
         * @param x
         *        Normalized objective variables.
         * @return {@code true} if in bounds.
         */
        // CHECKSTYLE: stop ReturnCount check
        // Reason: Commons-Math code kept as such
        public boolean isFeasible(final double[] x) {
            // CHECKSTYLE: resume ReturnCount check
            final double[] lB = CMAESOptimizer.this.getLowerBound();
            final double[] uB = CMAESOptimizer.this.getUpperBound();

            for (int i = 0; i < x.length; i++) {
                if (x[i] < lB[i]) {
                    return false;
                }
                if (x[i] > uB[i]) {
                    return false;
                }
            }
            return true;
        }

        /**
         * @param valueRangeIn
         *        Adjusts the penalty computation.
         */
        public void setValueRange(final double valueRangeIn) {
            this.valueRange = valueRangeIn;
        }

        /**
         * @param x
         *        Normalized objective variables.
         * @return the repaired (i.e. all in bounds) objective variables.
         */
        private double[] repair(final double[] x) {
            // Get lower bound
            final double[] lB = CMAESOptimizer.this.getLowerBound();
            // Get upper bound
            final double[] uB = CMAESOptimizer.this.getUpperBound();

            // Initialize repaired objective variable array
            final double[] repaired = new double[x.length];
            for (int i = 0; i < x.length; i++) {
                if (x[i] < lB[i]) {
                    // raise to lower bound
                    repaired[i] = lB[i];
                } else if (x[i] > uB[i]) {
                    // lower to upper bound
                    repaired[i] = uB[i];
                } else {
                    // keep value as it is
                    repaired[i] = x[i];
                }
            }
            return repaired;
        }

        /**
         * @param x
         *        Normalized objective variables.
         * @param repaired
         *        Repaired objective variables.
         * @return Penalty value according to the violation of the bounds.
         */
        private double penalty(final double[] x, final double[] repaired) {
            double penalty = 0;
            for (int i = 0; i < x.length; i++) {
                final double diff = Math.abs(x[i] - repaired[i]);
                penalty += diff * this.valueRange;
            }
            return CMAESOptimizer.this.isMinimize ? penalty : -penalty;
        }
    }
    // CHECKSTYLE: resume MagicNumber check
}
