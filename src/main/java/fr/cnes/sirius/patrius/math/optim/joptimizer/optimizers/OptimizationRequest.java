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
 * VERSION:4.6:DM:DM-2591:27/01/2021:[PATRIUS] Intigration et validation JOptimizer
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.optim.joptimizer.optimizers;

import fr.cnes.sirius.patrius.math.linear.ArrayRealVector;
import fr.cnes.sirius.patrius.math.linear.BlockRealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealVector;
import fr.cnes.sirius.patrius.math.optim.joptimizer.functions.ConvexMultivariateRealFunction;

/**
 * Optimization problem.
 * Setting the field's values you define an optimization problem.
 * 
 * @see "S.Boyd and L.Vandenberghe, Convex Optimization"
 * @author alberto trivellato (alberto.trivellato@gmail.com)
 * 
 * @since 4.6
 */
public class OptimizationRequest {

    /**
     * Maximum number of iteration in the search algorithm.
     * Not mandatory, default is provided.
     */
    private int maxIteration = JOptimizer.DEFAULT_MAX_ITERATION;

    /**
     * Tolerance for the minimum value.
     * Not mandatory, default is provided.
     * NOTE: as a golden rule, do not ask for more accuracy than you really need.
     * @see "Convex Optimization, p. 11.7.3"
     */
    private double tolerance = JOptimizer.DEFAULT_TOLERANCE;

    /**
     * Tolerance for the constraints satisfaction.
     * Not mandatory, default is provided.
     * NOTE: as a golden rule, do not ask for more accuracy than you really need.
     * @see "Convex Optimization, p. 11.7.3"
     */
    private double toleranceFeas = JOptimizer.DEFAULT_FEASIBILITY_TOLERANCE;

    /**
     * Tolerance for inner iterations in the barrier-method.
     * NB: it makes sense only for barrier method.
     * Not mandatory, default is provided.
     * NOTE: as a golden rule, do not ask for more accuracy than you really need.
     * @see "Convex Optimization, p. 11.7.3"
     */
    private double toleranceInnerStep = JOptimizer.DEFAULT_TOLERANCE_INNER_STEP;

    /**
     * Calibration parameter for line search.
     * Not mandatory, default is provided.
     * @see "Convex Optimization, p. 11.7.3"
     */
    private double alpha = JOptimizer.DEFAULT_ALPHA;

    /**
     * Calibration parameter for line search.
     * Not mandatory, default is provided.
     * @see "Convex Optimization, p. 11.7.3"
     */
    private double beta = JOptimizer.DEFAULT_BETA;

    /**
     * Calibration parameter for line search.
     * Not mandatory, default is provided.
     * @see "Convex Optimization, p. 11.7.3"
     */
    private double mu = JOptimizer.DEFAULT_MU;

    /**
     * Activate progress condition check during iterations.
     * If true, a progress in the relevant algorithm norms is required during iterations,
     * otherwise the iteration will be exited with a warning (and solution
     * must be manually checked against the desired accuracy).
     * Not mandatory, default is provided.
     * @see "Convex Optimization, p. 11.7.3"
     */
    private boolean checkProgressConditions = false;

    /**
     * Check the accuracy of the solution of KKT system during iterations.
     * If true, every inversion of the system must have an accuracy that satisfy
     * the given toleranceKKT.
     * Not mandatory, default is provided.
     * @see "Convex Optimization, p. 11.7.3"
     */
    private boolean checkKKTSolutionAccuracy = false;

    /**
     * Acceptable tolerance for KKT system resolution.
     * Not mandatory, default is provided.
     */
    private double toleranceKKT = JOptimizer.DEFAULT_KKT_TOLERANCE;

    /**
     * Should matrix rescaling be disabled?
     * Rescaling is involved in LP presolving and in the solution of the KKT systems associated with
     * the problem.
     * It is an heuristic process, in some situations it could be useful to turn off this feature.
     */
    private boolean rescalingDisabled = false;

    /**
     * The objective function to minimize.
     * Mandatory.
     */
    private ConvexMultivariateRealFunction f0;

    /**
     * Feasible starting point for the minimum search.
     * It must be feasible.
     * Not mandatory.
     */
    private RealVector initialPoint = null;

    /**
     * Not-feasible starting point for the minimum search.
     * It does not have to be feasible. This provide the possibility to give the algorithm
     * a starting point even if it does not satisfies inequality constraints. The algorithm
     * will search a feasible point starting from here.
     * Not mandatory.
     */
    private RealVector notFeasibleInitialPoint = null;

    /**
     * Starting point for the Lagrangian multipliers.
     * Must have the same dimension of the inequalities constraints array.
     * Not mandatory, but very useful in some case.
     */
    private RealVector initialLagrangian = null;

    /**
     * Equalities constraints matrix.
     * Must be rank(A) < dimension of the variable.
     * Not mandatory.
     * @see "Convex Optimization, 11.1"
     */
    private RealMatrix matA = null;

    /**
     * Equalities constraints vector.
     * Not mandatory.
     * @see "Convex Optimization, 11.1"
     */
    private RealVector b = null;

    /**
     * Inequalities constraints array.
     * Not mandatory.
     * @see "Convex Optimization, 11.1"
     */
    private ConvexMultivariateRealFunction[] fi;

    /**
     * The chosen interior-point method.
     * Must be barrier-method or primal-dual method.
     */
    private String interiorPointMethod = JOptimizer.PRIMAL_DUAL_METHOD;

    /**
     * Get the maximum number of iteration in the search algorithm.
     * 
     * @return maximum number of iteration
     */
    public int getMaxIteration() {
        return maxIteration;
    }

    /**
     * Set the maximum number of iteration in the search algorithm.
     * 
     * @param maxIterations maximum number of iteration
     */
    public void setMaxIteration(final int maxIterations) {
        this.maxIteration = maxIterations;
    }

    /**
     * Get the tolerance for the minimum value.
     * 
     * @return tolerance
     */
    public double getTolerance() {
        return tolerance;
    }

    /**
     * Set the tolerance for the minimum value.
     * 
     * @param toleranceMV tolerance
     */
    public void setTolerance(final double toleranceMV) {
        this.tolerance = toleranceMV;
    }

    /**
     * Get the tolerance for the constraints satisfaction.
     * 
     * @return tolerance
     */
    public double getToleranceFeas() {
        return toleranceFeas;
    }

    /**
     * Set the tolerance for the constraints satisfaction.
     * 
     * @param toleranceF tolerance
     */
    public void setToleranceFeas(final double toleranceF) {
        this.toleranceFeas = toleranceF;
    }

    /**
     * Get the tolerance for inner iterations in the barrier-method.
     * 
     * @return tolerance
     */
    public double getToleranceInnerStep() {
        return toleranceInnerStep;
    }

    /**
     * Set the tolerance for inner iterations in the barrier-method.
     * 
     * @param toleranceIS tolerance
     */
    public void setToleranceInnerStep(final double toleranceIS) {
        this.toleranceInnerStep = toleranceIS;
    }

    /**
     * Get the calibration parameter for line search
     * 
     * @return alpha
     */
    public double getAlpha() {
        return alpha;
    }

    /**
     * Set the calibration parameter for line search
     * 
     * @param a calibration parameter
     */
    public void setAlpha(final double a) {
        this.alpha = a;
    }

    /**
     * Get the calibration parameter for line search
     * 
     * @return beta
     */
    public double getBeta() {
        return beta;
    }

    /**
     * Set the calibration parameter for line search
     * 
     * @param be calibration parameter
     */
    public void setBeta(final double be) {
        this.beta = be;
    }

    /**
     * Get the calibration parameter for line search
     * 
     * @return mu
     */
    public double getMu() {
        return mu;
    }

    /**
     * Set the calibration parameter for line search
     * 
     * @param m calibration parameter
     */
    public void setMu(final double m) {
        this.mu = m;
    }

    /**
     * If true, a progress in the relevant algorithm norms is
     * required during iterations, otherwise the iteration will
     * be exited with a warning
     * 
     * @return true/false
     */
    public boolean isCheckProgressConditions() {
        return checkProgressConditions;
    }

    /**
     * Set true if a progress in the relevant algorithm norms is
     * required during iterations, or false if the iteration will
     * be exited with a warning
     * 
     * @param checkProgressCondition true/false
     */
    public void setCheckProgressConditions(final boolean checkProgressCondition) {
        this.checkProgressConditions = checkProgressCondition;
    }

    /**
     * Check the accuracy of the solution of KKT system during iterations.
     * If true, every inversion of the system must have an accuracy that satisfy
     * the given toleranceKKT
     * 
     * @return true/false
     */
    public boolean isCheckKKTSolutionAccuracy() {
        return checkKKTSolutionAccuracy;
    }

    /**
     * Set true if every inversion of the system must have an accuracy that satisfy
     * the given toleranceKKT, false otherwise.
     * 
     * @param checkKKTSolutionAcc true/false
     */
    public void setCheckKKTSolutionAccuracy(final boolean checkKKTSolutionAcc) {
        this.checkKKTSolutionAccuracy = checkKKTSolutionAcc;
    }

    /**
     * Get the acceptable tolerance for KKT system resolution
     * 
     * @return tolerance
     */
    public double getToleranceKKT() {
        return toleranceKKT;
    }

    /**
     * Set the acceptable tolerance for KKT system resolution
     * 
     * @param toleranceK tolerance
     */
    public void setToleranceKKT(final double toleranceK) {
        this.toleranceKKT = toleranceK;
    }

    /**
     * Get the objective function to minimize
     * 
     * @return objective function
     */
    public ConvexMultivariateRealFunction getF0() {
        return f0;
    }

    /**
     * Set the objective function to minimize
     * 
     * @param f objective function
     */
    public void setF0(final ConvexMultivariateRealFunction f) {
        this.f0 = f;
    }

    /**
     * Get the feasible starting point for the minimum search
     * 
     * @return initial point
     */
    public RealVector getInitialPoint() {
        return initialPoint;
    }

    /**
     * Set the feasible starting point for the minimum search
     * 
     * @param initialP feasible starting point
     */
    public void setInitialPoint(final double[] initialP) {
        this.initialPoint = new ArrayRealVector(initialP);
    }

    /**
     * Get a not-feasible starting point for the minimum search
     * 
     * @return not-feasible initial point
     */
    public RealVector getNotFeasibleInitialPoint() {
        return notFeasibleInitialPoint;
    }

    /**
     * Set a not-feasible starting point for the minimum search
     * 
     * @param notFeasibleInitialP not-feasible initial point
     */
    public void setNotFeasibleInitialPoint(final double[] notFeasibleInitialP) {
        this.notFeasibleInitialPoint = new ArrayRealVector(notFeasibleInitialP);
    }

    /**
     * Get a starting point for the Lagrangian multipliers
     * 
     * @return initial point
     */
    public RealVector getInitialLagrangian() {
        return initialLagrangian;
    }

    /**
     * Set a starting point for the Lagrangian multipliers
     * 
     * @param initialL initial point
     */
    public void setInitialLagrangian(final double[] initialL) {
        this.initialLagrangian = new ArrayRealVector(initialL);
    }

    /**
     * Get the equalities constraints matrix
     * 
     * @return equalities constraints matrix
     */
    public RealMatrix getA() {
        return matA;
    }

    /**
     * Set the equalities constraints matrix
     * 
     * @param a equalities constraints double[][]
     */
    public void setA(final double[][] a) {
        if (a != null) {
            matA = new BlockRealMatrix(a);
        }
    }

    /**
     * Set the equalities constraints matrix
     * 
     * @param a equalities constraints matrix
     */
    public void setA(final RealMatrix a) {
        this.matA = a;
    }

    /**
     * Get the equalities constraints vector
     * 
     * @return equalities constraints vector
     */
    public RealVector getB() {
        return b;
    }

    /**
     * Set the equalities constraints vector
     * 
     * @param vecB equalities constraints double[]
     */
    public void setB(final double[] vecB) {
        if (vecB != null) {
            this.b = new ArrayRealVector(vecB);
        }
    }

    /**
     * Set the equalities constraints vector
     * 
     * @param vecB equalities constraints vector
     */
    public void setB(final RealVector vecB) {
        this.b = vecB;
    }

    /**
     * Get the inequalities constraints array
     * 
     * @return inequalities constraints array
     */
    public ConvexMultivariateRealFunction[] getFi() {
        if (fi == null) {
            return null;
        } else {
            return (ConvexMultivariateRealFunction[]) fi.clone();
        }
    }

    /**
     * Set the inequalities constraints array
     * 
     * @param f inequalities constraints array
     */
    public void setFi(final ConvexMultivariateRealFunction[] f) {
        this.fi = f.clone();

    }

    /**
     * Get the chosen interior-point method
     * 
     * @return chosen interior-point method
     */
    public String getInteriorPointMethod() {
        return interiorPointMethod;
    }

    /**
     * Set the chosen interior-point method
     * 
     * @param interiorPM chosen interior-point method (string)
     */
    public void setInteriorPointMethod(final String interiorPM) {
        this.interiorPointMethod = interiorPM;
    }

    /**
     * Set if the matrix rescaling should be disabled (true) or not (false)
     * Rescaling is involved in LP presolving and in the solution of the KKT
     * systems associated with the problem.
     * 
     * @param rescalingDis true/false
     */
    public void setRescalingDisabled(final boolean rescalingDis) {
        this.rescalingDisabled = rescalingDis;
    }

    /**
     * Is the matrix rescaling disabled?
     * 
     * @return true/false
     */
    public boolean isRescalingDisabled() {
        return rescalingDisabled;
    }
}
