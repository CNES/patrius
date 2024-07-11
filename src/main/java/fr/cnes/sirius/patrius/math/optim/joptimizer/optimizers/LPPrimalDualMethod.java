/**
 * Copyright 2019-2020 CNES
 * Copyright 2011-2014 JOptimizer
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 * HISTORY
 * VERSION:4.8:FA:FA-2954:15/11/2021:[PATRIUS] Problemes lors de l'integration de JOptimizer dans Patrius 
 * VERSION:4.7:FA:FA-2762:18/05/2021:Probleme lors des controles qualite via la PIC 
 * VERSION:4.6:DM:DM-2591:27/01/2021:[PATRIUS] Intigration et validation JOptimizer
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.optim.joptimizer.optimizers;

import fr.cnes.sirius.patrius.math.linear.ArrayRealVector;
import fr.cnes.sirius.patrius.math.linear.BlockRealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealVector;
import fr.cnes.sirius.patrius.math.optim.joptimizer.algebra.AlgebraUtils;
import fr.cnes.sirius.patrius.math.optim.joptimizer.solvers.AbstractKKTSolver;
import fr.cnes.sirius.patrius.math.optim.joptimizer.solvers.UpperDiagonalHKKTSolver;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.PatriusRuntimeException;

/**
 * Primal-dual interior-point method for LP problems in the form (1):
 * 
 * <br>
 * min(c) s.t. <br>
 * G.x < h <br>
 * A.x = b <br>
 * lb <= x <= ub
 * 
 * <br>
 * If lower and/or upper bounds are not passed in, a default value is assigned on them, that is: <br>
 * -)if the vector lb is not passed in, all the lower bounds are assumed to be equal to the value of
 * the field
 * <i>minLBValue</i> <br>
 * -)if the vector ub is not passed in, all the upper bounds are assumed to be equal to the value of
 * the field
 * <i>maxUBValue</i>
 * 
 * <br>
 * The problem is first transformed in the standard form, then presolved and finally solved.
 * 
 * <br>
 * Note 1: avoid to set minLBValue or maxUBValue to fake unbounded values.
 * 
 * @see "S.Boyd and L.Vandenberghe, Convex Optimization, p. 609"
 * @author alberto trivellato (alberto.trivellato@gmail.com)
 * 
 * @since 4.6
 */
public class LPPrimalDualMethod extends AbstractLPOptimizationRequestHandler {

    /** Default minimum lower bound */
    public static final double DEFAULT_MIN_LOWER_BOUND = -99999;
    /** Default maximum upper bound */
    public static final double DEFAULT_MAX_UPPER_BOUND = +99999;
    /** Minimum lower bound */
    private double minLBValue = DEFAULT_MIN_LOWER_BOUND;
    /** Maximum upper bound */
    private double maxUBValue = DEFAULT_MAX_UPPER_BOUND;
    /**
     * Lower bounds with min value limited to the value of the field <i>minLBValue</i>.
     */
    private RealVector limitedLb;

    /**
     * Upper bounds with max value limited to the value of the field <i>maxUBValue</i>.
     */
    private RealVector limitedUb;

    /**
     * KKT solver
     */
    private AbstractKKTSolver kktSolver;

    /**
     * Constructor It sets the default minimum lower bound and maximum upper bound
     */
    public LPPrimalDualMethod() {
        this(DEFAULT_MIN_LOWER_BOUND, DEFAULT_MAX_UPPER_BOUND);
    }

    /**
     * Constructor
     * 
     * @param minLBVal
     *            minimum lower bound
     * @param maxUBVal
     *            maximum upper bound
     */
    public LPPrimalDualMethod(final double minLBVal, final double maxUBVal) {
        super();
        if (Double.isNaN(minLBVal) || Double.isInfinite(minLBVal)) {
            throw new IllegalArgumentException(
                    "The field minLBValue must not be set to Double.NaN or Double.NEGATIVE_INFINITY");
        }
        if (Double.isNaN(maxUBVal) || Double.isInfinite(maxUBVal)) {
            throw new IllegalArgumentException(
                    "The field maxUBValue must not be set to Double.NaN or Double.POSITIVE_INFINITY");
        }
        this.minLBValue = minLBVal;
        this.maxUBValue = maxUBVal;
    }

    /**
     * Solves an LP in the form of: min(c) s.t. A.x = b G.x < h lb <= x <= ub
     * 
     */
    @Override
    public int optimize() throws PatriusException {

        final LPOptimizationRequest lpRequest = getLPOptimizationRequest();

        // standard form conversion
        final LPStandardConverter lpConverter = new LPStandardConverter();// the slack variables
                                                                          // will have
        // default
        // unboundedUBValue
        lpConverter.toStandardForm(getC(), getG(), getH(), getA(), getB(), getLb(), getUb());
        final int nOfSlackVariables = lpConverter.getStandardS();
        final RealVector standardC = lpConverter.getStandardC();
        final RealMatrix standardA = lpConverter.getStandardA();
        final RealVector standardB = lpConverter.getStandardB();
        final RealVector standardLb = lpConverter.getStandardLB();
        final RealVector standardUb = lpConverter.getStandardUB();

        // solve the standard form problem
        final LPOptimizationRequest standardLPRequest = lpRequest.cloneMe();
        standardLPRequest.setC(standardC);
        standardLPRequest.setA(standardA);
        standardLPRequest.setB(standardB);
        // substitute not-double numbers
        standardLPRequest.setLb(AlgebraUtils.replaceValues(standardLb, lpConverter.getUnboundedLBValue(), minLBValue));
        // substitute not-double numbers
        standardLPRequest.setUb(AlgebraUtils.replaceValues(standardUb, lpConverter.getUnboundedUBValue(), maxUBValue));
        if (getInitialPoint() != null) {
            standardLPRequest.setInitialPoint(lpConverter.getStandardComponents(getInitialPoint().toArray()));
        }
        if (getNotFeasibleInitialPoint() != null) {
            standardLPRequest.setNotFeasibleInitialPoint(
                lpConverter.getStandardComponents(getNotFeasibleInitialPoint().toArray()));
        }

        // optimization
        final LPPrimalDualMethod opt = new LPPrimalDualMethod(minLBValue, maxUBValue);
        opt.setLPOptimizationRequest(standardLPRequest);
        if (opt.optimizeStandardLP(nOfSlackVariables) == OptimizationResponse.FAILED) {
            return OptimizationResponse.FAILED;
        }

        // back to original form
        final LPOptimizationResponse lpResponse = opt.getLPOptimizationResponse();
        final double[] standardSolution = lpResponse.getSolution();
        final double[] originalSol = lpConverter.postConvert(standardSolution);
        lpResponse.setSolution(originalSol);
        setLPOptimizationResponse(lpResponse);
        return lpResponse.getReturnCode();
    }

    /**
     * Solves a standard form LP problem in the form of min(c) s.t. A.x = b lb <= x <= ub
     * 
     * @param nOfSlackVariables
     *            number of slack variables
     * @return result
     * @throws PatriusException
     *             if an error occurs
     */
    protected int optimizeStandardLP(final int nOfSlackVariables) throws PatriusException {

        final LPOptimizationRequest lpRequest = getLPOptimizationRequest();

        final LPOptimizationResponse lpResponse;
        if (lpRequest.isPresolvingDisabled()) {
            // optimization
            final LPPrimalDualMethod opt = new LPPrimalDualMethod(minLBValue, maxUBValue);
            opt.setLPOptimizationRequest(lpRequest);
            if (opt.optimizePresolvedStandardLP() == OptimizationResponse.FAILED) {
                return OptimizationResponse.FAILED; // optimization failed
            }
            lpResponse = opt.getLPOptimizationResponse();
            setLPOptimizationResponse(lpResponse);
        } else {
            // presolving
            final LPPresolver lpPresolver = new LPPresolver();
            lpPresolver.setAvoidScaling(lpRequest.isRescalingDisabled());
            lpPresolver.setAvoidFillIn(lpRequest.isAvoidPresolvingFillIn());
            lpPresolver.setAvoidIncreaseSparsity(lpRequest.isAvoidPresolvingIncreaseSparsity());
            lpPresolver.setNOfSlackVariables((short) nOfSlackVariables);
            lpPresolver.presolve(getC(), getA(), getB(), getLb(), getUb());
            final int presolvedDim = lpPresolver.getPresolvedN();

            if (presolvedDim == 0) {
                // deterministic problem
                lpResponse = new LPOptimizationResponse();
                lpResponse.setReturnCode(OptimizationResponse.SUCCESS); // optimization succes
                lpResponse.setSolution(new double[]{});
            } else {
                // solving the presolved problem
                final RealVector presolvedC = lpPresolver.getPresolvedC();
                final RealMatrix presolvedA = lpPresolver.getPresolvedA();
                final RealVector presolvedB = lpPresolver.getPresolvedB();

                // new LP problem (the presolved problem)
                final LPOptimizationRequest presolvedLPRequest = lpRequest.cloneMe();
                presolvedLPRequest.setC(presolvedC);
                presolvedLPRequest.setA(presolvedA);
                presolvedLPRequest.setB(presolvedB);
                presolvedLPRequest.setLb(lpPresolver.getPresolvedLB());
                presolvedLPRequest.setUb(lpPresolver.getPresolvedUB());
                presolvedLPRequest.setYlb(lpPresolver.getPresolvedYlb());
                presolvedLPRequest.setYub(lpPresolver.getPresolvedYub());
                presolvedLPRequest.setZlb(lpPresolver.getPresolvedZlb());
                presolvedLPRequest.setZub(lpPresolver.getPresolvedZub());
                if (getInitialPoint() != null) {
                    presolvedLPRequest.setInitialPoint(lpPresolver.presolve(getInitialPoint().toArray()));
                }
                if (getNotFeasibleInitialPoint() != null) {
                    presolvedLPRequest
                            .setNotFeasibleInitialPoint(lpPresolver.presolve(getNotFeasibleInitialPoint().toArray()));
                }

                // optimization
                // NB: because of rescaling during the presolving phase, minLB and maxUB could have
                // been rescaled
                final double rescaledMinLBValue;
                if (Double.isNaN(lpPresolver.getMinRescaledLB())) {
                    rescaledMinLBValue = this.minLBValue;
                } else {
                    rescaledMinLBValue = lpPresolver.getMinRescaledLB();
                }
                final double rescaledMaxUBValue;
                if (Double.isNaN(lpPresolver.getMaxRescaledUB())) {
                    rescaledMaxUBValue = this.maxUBValue;
                } else {
                    rescaledMaxUBValue = lpPresolver.getMaxRescaledUB();
                }
                final LPPrimalDualMethod opt = new LPPrimalDualMethod(rescaledMinLBValue, rescaledMaxUBValue);
                opt.setLPOptimizationRequest(presolvedLPRequest);
                if (opt.optimizePresolvedStandardLP() == OptimizationResponse.FAILED) {
                    return OptimizationResponse.FAILED; // optimization failed
                }
                lpResponse = opt.getLPOptimizationResponse();
            }

            // postsolving
            final double[] postsolvedSolution = lpPresolver.postsolve(lpResponse.getSolution());
            lpResponse.setSolution(postsolvedSolution);
            setLPOptimizationResponse(lpResponse);
        }

        return lpResponse.getReturnCode();
    }

    /**
     * Solves a presolved standard form LP problem in the form of min(c) s.t. A.x = b lb <= x <= ub
     * 
     * @return result
     * @throws PatriusException
     *             if an error occurs
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // CHECKSTYLE: stop MethodLength check
    // CHECKSTYLE: stop CommentRatio check
    // Reason: complex JOptimizer code kept as such
    protected int optimizePresolvedStandardLP() throws PatriusException {
        // CHECKSTYLE: resume CommentRatio check
        // CHECKSTYLE: resume CyclomaticComplexity check
        // CHECKSTYLE: resume MethodLength check

        if (this.dim <= -1) {
            if (getLb().getDimension() != getUb().getDimension()) {
                throw new IllegalArgumentException("Lower and upper bounds must have the same dimension");
            }
            this.dim = getLb().getDimension();
            double minDeltaBoundsValue = Double.MAX_VALUE;
            for (int i = 0; i < getDim(); i++) {
                final double deltai = getUb().getEntry(i) - getLb().getEntry(i);
                if (deltai < minDeltaBoundsValue) {
                    minDeltaBoundsValue = deltai;
                }
            }
        }

        if (this.meq <= -1) {
            if (getA() != null) {
                this.meq = getA().getRowDimension();
            } else {
                this.meq = 0;
            }
        }
        if (this.mieq <= -1) {
            this.mieq = 2 * getDim();
        }

        RealVector x0 = getInitialPoint();
        if (x0 == null) {
            final RealVector x0NF = getNotFeasibleInitialPoint();
            if (x0NF != null) {
                final double rPriX0NFNorm = (rPri(x0NF)).getNorm();
                final RealVector fiX0NF = getFi(x0NF);
                final int maxIndex = fiX0NF.getMaxIndex();
                final double maxValue = fiX0NF.getEntry(maxIndex);
                if (maxValue < 0 && rPriX0NFNorm <= getToleranceFeas()) {
                    // the provided not-feasible starting point is already feasible
                    x0 = x0NF;
                }
            }
            if (x0 == null) {
                final BasicPhaseILPPDM bf1 = new BasicPhaseILPPDM(this);
                x0 = bf1.findFeasibleInitialPoint();
            }
        }

        // check X0 feasibility
        final RealVector fiX0 = getFi(x0);
        final int maxIndex = fiX0.getMaxIndex();
        final double maxValue = fiX0.getEntry(maxIndex);
        final double rPriX0Norm = (rPri(x0)).getNorm();
        if (maxValue >= 0. || rPriX0Norm > getToleranceFeas()) {// must be fi STRICTLY < 0
            // the point must be INTERNAL, fi are used as denominators
            throw new PatriusException(PatriusMessages.INITIAL_POINT_NOT_FEASIBLE);
        }

        final RealVector v0 = new ArrayRealVector(getMeq());
        if (getYlb() != null && getYub() != null) {
            // NB: the Lagrangian multipliers for eq. constraints used in this interior point method
            // (v)
            // are the opposite of the Lagrangian multipliers for eq. constraints used in the
            // presolver (y)
            // and so Ylb<=y<=Yub becomes -Yub<=v<=-Ylb
            for (int i = 0; i < getMeq(); i++) {
                double v0i = 0;
                if (!isLbUnbounded(getYlb().getEntry(i))) {
                    if (!isUbUnbounded(getYub().getEntry(i))) {
                        v0i = -(getYub().getEntry(i) + getYlb().getEntry(i)) / 2;
                    } else {
                        v0i = -getYlb().getEntry(i);
                    }
                } else {
                    if (!isUbUnbounded(getYub().getEntry(i))) {
                        v0i = -getYub().getEntry(i);
                    } else {
                        v0i = 0;
                    }
                }
                v0.setEntry(i, v0i);
            }
        }

        RealVector l0 = getInitialLagrangian();
        if (l0 != null) {
            for (int j = 0; j < l0.getDimension(); j++) {
                // must be >0
                if (l0.getEntry(j) <= 0) {
                    throw new IllegalArgumentException("initial lagrangian must be strictly > 0");
                }
            }
        } else {
            l0 = new ArrayRealVector(getMieq(), 1.);// must be >0 strictly
            if (getZlb() != null && getZub() != null) {
                // Zlb<= L <=Zub, meaning that:
                // zlb[i] and zub[i] are the bounds on the Lagrangian of the constraint associated
                // with lb[i]<x[i]<ub[i]
                // note that zlb.size = zub.size = lb.size = ub.size (and = n of variables of the
                // problem (= getDim())
                // and that L.size = nOfBoundedLb + nOfBoundedUb (and in general < 2*getDim())
                int cntLB = 0;
                int cntUB = 0;
                for (int i = 0; i < getDim(); i++) {
                    final double zlbi; // L
                    if (isLbUnbounded(getZlb().getEntry(i))) {
                        zlbi = 0;
                    } else {
                        zlbi = getZlb().getEntry(i);
                    }
                    // must
                    // be
                    // >
                    // 0
                    final double zubi;
                    if (isUbUnbounded(getZub().getEntry(i))) {
                        zubi = 1;
                    } else {
                        zubi = getZub().getEntry(i);
                    }
                    l0.setEntry(cntLB, (zubi - zlbi) / 2);
                    cntLB++;
                    l0.setEntry(getDim() + cntUB, (zubi - zlbi) / 2);
                    cntUB++;
                }
            }
        }

        final LPOptimizationResponse lpResponse = new LPOptimizationResponse();
        RealVector x = x0;
        RealVector v = v0;
        RealVector l = l0;
        double previousRPriXNorm = Double.NaN;
        double previousRDualXLVNorm = Double.NaN;
        double previousSurrDG = Double.NaN;
        double t;
        int iteration = 0;
        while (true) {

            iteration++;
            // iteration limit condition
            if (iteration == getMaxIteration() + 1) {
                lpResponse.setReturnCode(OptimizationResponse.FAILED);
                break;
            }

            // determine functions evaluations
            final RealVector gradF0X = getGradF0(x);
            final RealVector fiX = getFi(x);

            // determine t
            final double surrDG = getSurrogateDualityGap(fiX, l);
            t = getMu() * getMieq() / surrDG;

            // determine residuals
            final RealVector rPriX = rPri(x);
            final RealVector rDualXLV = rDual(gradF0X, l, v);
            final double rPriXNorm = rPriX.getNorm();
            final double rDualXLVNorm = rDualXLV.getNorm();

            // custom exit condition
            if (checkCustomExitConditions(x)) {
                lpResponse.setReturnCode(OptimizationResponse.SUCCESS);
                break;
            }

            // exit condition
            if (rPriXNorm <= getToleranceFeas() && rDualXLVNorm <= getToleranceFeas() && surrDG <= getTolerance()) {
                lpResponse.setReturnCode(OptimizationResponse.SUCCESS);
                break;
            }
            // progress conditions
            if (isCheckProgressConditions()) {
                if (!Double.isNaN(previousRPriXNorm) && !Double.isNaN(previousRDualXLVNorm)
                    && !Double.isNaN(previousSurrDG)) {
                    if ((previousRPriXNorm <= rPriXNorm && rPriXNorm >= getToleranceFeas())
                        || (previousRDualXLVNorm <= rDualXLVNorm && rDualXLVNorm >= getToleranceFeas())) {
                        lpResponse.setReturnCode(OptimizationResponse.FAILED);
                        break;
                    }
                }
                previousRPriXNorm = rPriXNorm;
                previousRDualXLVNorm = rDualXLVNorm;
                previousSurrDG = surrDG;
            }

            // compute primal-dual search direction
            // a) prepare 11.55 system
            final RealMatrix hpd = gradLSum(l, fiX);
            final RealVector gradSum = gradSum(t, fiX);
            RealVector g = null;
            if (getA() == null) {
                g = gradF0X.add(gradSum);
            } else {
                g = gradF0X.add(gradSum).add(AlgebraUtils.zMultTranspose(getA(), v, new ArrayRealVector(getDim()), 0));
            }

            // b) solving 11.55 system
            final LPOptimizationRequest lpRequest = getLPOptimizationRequest();
            if (this.kktSolver == null) {
                this.kktSolver = new UpperDiagonalHKKTSolver(getDim(), lpRequest.isRescalingDisabled());
            }
            if (isCheckKKTSolutionAccuracy()) {
                kktSolver.setCheckKKTSolutionAccuracy(true);
                kktSolver.setToleranceKKT(getToleranceKKT());
            }
            kktSolver.setHMatrix(hpd);
            kktSolver.setGVector(g);
            if (getA() != null) {
                kktSolver.setAMatrix(getA());
                kktSolver.setHVector(rPriX);
            }
            final RealVector[] sol = kktSolver.solve();
            final RealVector stepX = sol[0];
            final RealVector stepV;
            if (sol[1] != null) {
                stepV = sol[1];
            } else {
                stepV = new ArrayRealVector(0);
            }

            // c) solving for L
            final RealVector stepL = new ArrayRealVector(getMieq());
            final RealVector gradFiStepX = gradFiStepX(stepX);
            final RealVector rCentXLt = rCent(fiX, l, t);
            for (int i = 0; i < getMieq(); i++) {
                stepL.setEntry(i, (-l.getEntry(i) * gradFiStepX.getEntry(i) + rCentXLt.getEntry(i)) / fiX.getEntry(i));
            }

            // line search and update
            // a) sMax computation
            double sMax = Double.MAX_VALUE;
            for (int j = 0; j < getMieq(); j++) {
                if (stepL.getEntry(j) < 0) {
                    sMax = MathLib.min(-l.getEntry(j) / stepL.getEntry(j), sMax);
                }
            }
            sMax = MathLib.min(1, sMax);
            double s = SCALAR * sMax;
            // b) backtracking with f
            RealVector x1 = new ArrayRealVector(x.getDimension());
            int cnt = 0;
            boolean areAllNegative = true;
            while (cnt < MAX_ITERATIONS) {
                cnt++;
                // X1 = X + s*stepX
                x1 = stepX.mapMultiply(s).add(x);
                final RealVector ineqValueX1 = getFi(x1);
                areAllNegative = true;
                for (int j = 0; areAllNegative && j < getMieq(); j++) {
                    areAllNegative = (Double.compare(ineqValueX1.getEntry(j), 0.) < 0);
                }
                if (areAllNegative) {
                    break;
                }
                s = getBeta() * s;
            }

            if (!areAllNegative) {
                // exited from the feasible region
                throw new PatriusException(PatriusMessages.OPTIMIZATION_FAILED);
            }

            // c) backtracking with norm
            final double rCentXLtNorm = rCentXLt.getNorm();
            final double normRXLVt =
                MathLib.sqrt(MathLib.pow(rPriXNorm, 2) + MathLib.pow(rCentXLtNorm, 2) + MathLib.pow(rDualXLVNorm, 2));
            double previousNormRX1L1V1t = Double.NaN;
            RealVector l1 = new ArrayRealVector(l.getDimension());
            RealVector v1 = new ArrayRealVector(v.getDimension());
            RealVector fiX1 = null;
            RealVector gradF0X1 = null;
            RealVector rPriX1 = null;
            RealVector rCentX1L1t = null;
            RealVector rDualX1L1V1 = null;
            cnt = 0;
            while (cnt < MAX_ITERATIONS) {
                cnt++;
                x1 = AlgebraUtils.add(x, stepX, s);
                l1 = AlgebraUtils.add(l, stepL, s);
                v1 = AlgebraUtils.add(v, stepV, s);

                if (isInDomainF0(x1)) {
                    fiX1 = getFi(x1);
                    gradF0X1 = getGradF0(x1);

                    rPriX1 = rPri(x1);
                    rCentX1L1t = rCent(fiX1, l1, t);
                    rDualX1L1V1 = rDual(gradF0X1, l1, v1);

                    final double normRX1L1V1t = MathLib.sqrt(MathLib.pow(rPriX1.getNorm(), 2)
                        + MathLib.pow(rCentX1L1t.getNorm(), 2) + MathLib.pow(rDualX1L1V1.getNorm(), 2));

                    if (normRX1L1V1t <= (1 - getAlpha() * s) * normRXLVt) {
                        break;
                    }

                    if (!Double.isNaN(previousNormRX1L1V1t)) {
                        if (previousNormRX1L1V1t <= normRX1L1V1t) {
                            break;
                        }
                    }
                    previousNormRX1L1V1t = normRX1L1V1t;
                }

                s = getBeta() * s;
            }

            // update
            x = x1;
            v = v1;
            l = l1;
        }

        lpResponse.setSolution(x.toArray());
        setLPOptimizationResponse(lpResponse);
        return lpResponse.getReturnCode();
    }

    /**
     * Calculates the second term of the first row of (11.55) "Convex Optimization".
     * 
     * @see "Convex Optimization, 11.55"
     * 
     * @param t
     *            value
     * @param fiX
     *            vector
     * @return result
     */
    protected RealVector gradSum(final double t, final RealVector fiX) {
        final RealVector gradSum = new ArrayRealVector(getDim());
        for (int i = 0; i < dim; i++) {
            double d = 0;
            d += 1. / (t * fiX.getEntry(i));
            d += -1. / (t * fiX.getEntry(getDim() + i));
            gradSum.setEntry(i, d);
        }
        return gradSum;
    }

    /**
     * Return the H matrix (that is diagonal). This is the third addendum of (11.56) of "Convex Optimization".
     * 
     * @see "Convex Optimization, 11.56"
     * 
     * @param l
     *            vector
     * @param fiX
     *            vector
     * @return H matrix
     */
    protected RealMatrix gradLSum(final RealVector l, final RealVector fiX) {
        // SparseDoubleMatrix2D GradLSum = new SparseDoubleMatrix2D(getDim(), getDim(), getDim(),
        // 0.001, 0.01);
        final BlockRealMatrix gradLSum = new BlockRealMatrix(getDim(), getDim());
        for (int i = 0; i < getDim(); i++) {
            double d = 0;
            d -= l.getEntry(i) / fiX.getEntry(i);
            d -= l.getEntry(getDim() + i) / fiX.getEntry(getDim() + i);
            gradLSum.setEntry(i, i, d);
        }

        return gradLSum;
    }

    /**
     * Computes the term Grad[fi].stepX
     * 
     * @param stepX
     *            vector
     * @return result
     */
    protected RealVector gradFiStepX(final RealVector stepX) {

        final RealVector ret = new ArrayRealVector(getMieq());
        for (int i = 0; i < getDim(); i++) {
            ret.setEntry(i, -stepX.getEntry(i));
            ret.setEntry(getDim() + i, stepX.getEntry(i));
        }

        return ret;
    }

    /**
     * Surrogate duality gap.
     * 
     * @see "Convex Optimization, 11.59"
     * @param fiX
     *            vector
     * @param l
     *            vector
     * @return result
     */
    private double getSurrogateDualityGap(final RealVector fiX, final RealVector l) {
        return -fiX.dotProduct(l);
    }

    /**
     * @see "Convex Optimization, p. 610"
     * @param gradF0X
     *            vector
     * @param l
     *            vector
     * @param v
     *            vector
     * @return result
     */
    protected RealVector rDual(final RealVector gradF0X, final RealVector l, final RealVector v) {
        final RealVector m1 = new ArrayRealVector(getDim());
        for (int i = 0; i < getDim(); i++) { // set values to m1
            double m = 0;
            m += -l.getEntry(i);
            m += l.getEntry(getDim() + i);
            m1.setEntry(i, m + gradF0X.getEntry(i));
        }
        if (getMeq() == 0) { // if number of equalities = 0 return vector of zeros
            return m1;
        }
        return AlgebraUtils.zMultTranspose(getA(), v, m1, 1.); // return A[T].v + b1*m1
    }

    /**
     * @see "Convex Optimization, p. 610"
     * @param fiX
     *            vector
     * @param l
     *            vector
     * @param t
     *            value
     * @return result
     */
    private RealVector rCent(final RealVector fiX, final RealVector l, final double t) {
        final RealVector ret = new ArrayRealVector(l.getDimension());
        for (int i = 0; i < ret.getDimension(); i++) {
            ret.setEntry(i, -l.getEntry(i) * fiX.getEntry(i) - 1. / t);
        }
        return ret;
    }

    /**
     * Set the KTT solver
     * 
     * @param kktSol
     *            ktt solver
     */
    public void setKKTSolver(final AbstractKKTSolver kktSol) {
        this.kktSolver = kktSol;
    }

    /**
     * Objective function value at X. This is C.X
     * 
     * @param x
     *            value
     * @return objective function
     */
    @Override
    protected double getF0(final RealVector x) {
        return getC().dotProduct(x);
    }

    /**
     * Objective function gradient at X. This is C itself.
     * 
     * @param x
     *            vector
     * @return gradient
     */
    @Override
    protected RealVector getGradF0(final RealVector x) {
        return getC();
    }

    /**
     * Objective function hessian at X. Hessians are null for linear problems
     * 
     * @param x
     *            vector
     */
    @Override
    protected RealMatrix getHessF0(final RealVector x) {
        throw new PatriusRuntimeException(PatriusMessages.HESSIAN_NULL_LP, null);
    }

    /**
     * Inequality functions values at X. This is (-x+lb) for all bounded lb and (x-ub) for all bounded ub.
     * 
     * @param x
     *            vector
     * @return inequality functions values
     */
    @Override
    protected RealVector getFi(final RealVector x) {
        final double[] ret = new double[getMieq()];
        for (int i = 0; i < getDim(); i++) {
            ret[i] = -x.getEntry(i) + getLb().getEntry(i);
            ret[getDim() + i] = x.getEntry(i) - getUb().getEntry(i);
        }
        return new ArrayRealVector(ret);
    }

    /**
     * Inequality functions gradients values at X. This is -1 for all bounded lb and 1 for all bounded ub, and it is
     * returned in a 2-rows compressed format.
     * 
     * GradFi are not used for LP
     * 
     * @param x
     *            vector
     * @return a 2xdim matrix, 1 row for the lower bounds and 1 row for the upper bounds gradients
     */
    @Override
    protected RealMatrix getGradFi(final RealVector x) {
        throw new PatriusRuntimeException(PatriusMessages.GRADIENT_NULL_LP, null);
    }

    /**
     * Inequality functions hessians values at X. Hessians are null for linear problems
     * 
     * @param x
     *            vector
     */
    @Override
    public RealMatrix[] getHessFi(final RealVector x) {
        throw new PatriusRuntimeException(PatriusMessages.HESSIAN_NULL_LP, null);
    }

    /**
     * rPri := Ax - b
     * 
     * @param x
     *            vector
     * @return result
     */
    @Override
    protected RealVector rPri(final RealVector x) {
        if (getMeq() == 0) {
            return new ArrayRealVector(0);
        }
        return AlgebraUtils.zMult(getA(), x, getB(), -1);
    }

    /**
     * Objective function domain.
     * 
     * @param x
     *            vector
     * @return true
     */
    @Override
    protected boolean isInDomainF0(final RealVector x) {
        return true;
    }

    /**
     * Return the lower bounds for the problem. If the original lower bounds are null, the they are set to the value of
     * <i>minLBValue</i>. Otherwise, any lower bound is limited to the value of <i>minLBValue</i>
     */
    @Override
    protected RealVector getLb() {
        if (this.limitedLb == null) {
            if (super.getLb() == null) {
                // The original lower bounds are null, the they are set to minLBValue
                this.limitedLb = new ArrayRealVector(getC().getDimension(), minLBValue);
            } else {
                this.limitedLb = new ArrayRealVector(super.getLb().getDimension());
                for (int i = 0; i < super.getLb().getDimension(); i++) {
                    final double lbi = super.getLb().getEntry(i);
                    if (lbi < minLBValue) {
                        // the "i"-th lower bound was limited from "lbi" to minLBValue
                        limitedLb.setEntry(i, minLBValue);
                    } else {
                        limitedLb.setEntry(i, lbi);
                    }
                }
            }
        }
        return limitedLb; // Return lower bounds for the problem
    }

    /**
     * Return the upper bounds for the problem. If the original upper bounds are null, the they are set to the value of
     * <i>maxUBValue</i>. Otherwise, any upper bound is limited to the value of <i>maxUBValue</i>
     */
    @Override
    protected RealVector getUb() {
        if (this.limitedUb == null) {
            if (super.getUb() == null) {
                // The original upper bounds are null, the they are set to maxUBValue
                this.limitedUb = new ArrayRealVector(getC().getDimension(), maxUBValue);
            } else {
                this.limitedUb = new ArrayRealVector(super.getUb().getDimension());
                for (int i = 0; i < super.getUb().getDimension(); i++) {
                    final double ubi = super.getUb().getEntry(i);
                    if (maxUBValue < ubi) {
                        // the "i"-th upper bound was limited form "ubi" to maxUBValue
                        limitedUb.setEntry(i, maxUBValue);
                    } else {
                        limitedUb.setEntry(i, ubi);
                    }
                }
            }
        }
        return limitedUb; // Return upper bounds for the problem
    }

    /**
     * Is the lower bound unbounded?
     * 
     * @param lb
     *            lower bound
     * @return true/false
     */
    protected boolean isLbUnbounded(final Double lb) {
        return Double.isNaN(lb);
    }

    /**
     * Is the upper bound unbounded?
     * 
     * @param ub
     *            upper bound
     * @return true/false
     */
    protected boolean isUbUnbounded(final Double ub) {
        return Double.isNaN(ub);
    }
}
