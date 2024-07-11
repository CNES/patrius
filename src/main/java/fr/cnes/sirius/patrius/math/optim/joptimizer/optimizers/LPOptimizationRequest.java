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
 * Linear optimization problem.
 * The general form is:
 * 
 * min(c) s.t. <br>
 * G.x < h <br>
 * A.x = b <br>
 * lb <= x <= ub
 * 
 * Lower and upper bounds can be stated for a more user friendly usage.
 * 
 * @see "S.Boyd and L.Vandenberghe, Convex Optimization"
 * @author alberto trivellato (alberto.trivellato@gmail.com)
 * 
 * @since 4.6
 */
public class LPOptimizationRequest extends OptimizationRequest {

    /** Message */
    private static final String MATRIX_FORM = "Use the matrix formulation for this linear problem";
    /**
     * Linear objective function.
     */
    private RealVector c;

    /**
     * Linear inequalities constraints matrix.
     */
    private RealMatrix g;

    /**
     * Linear inequalities constraints coefficients.
     */
    private RealVector h;

    /**
     * Lower bounds.
     */
    private RealVector lb;

    /**
     * Upper bounds.
     */
    private RealVector ub;

    /**
     * Lagrangian lower bounds for linear constraints (A rows).
     */
    private RealVector ylb;

    /**
     * Lagrangian upper bounds for linear constraints (A rows).
     */
    private RealVector yub;

    /**
     * Lagrangian lower bounds for lb constraints.
     */
    private RealVector zlb;

    /**
     * Lagrangian upper bounds for ub constraints.
     */
    private RealVector zub;

    /**
     * Should LP presolving be disabled?
     */
    private boolean presolvingDisabled = false;

    /**
     * If true, no method for making normal equations sparser will be applied during the presolving
     * phase.
     * @see Jacek Gondzio
     *      "Presolve analysis of linear programs prior to applying an interior point method", 3
     */
    private boolean avoidPresolvingIncreaseSparsity = false;

    /**
     * If true, no methods that cause fill-in in the original matrices will be called during the
     * presolving phase.
     */
    private boolean avoidPresolvingFillIn = false;

    /**
     * Check if the bound conditions on the optimal equality constraints Lagrangian coefficients are
     * respected.
     */
    private boolean checkOptimalLagrangianBounds = false;
    
       
    /**
     * Get the linear objective function
     * 
     * @return linear objective function
     **/
    public RealVector getC() {
        return c;
    }

    /**
     * Set the linear objective function
     * 
     * @param valueC linear objective function
     **/
    public void setC(final double[] valueC) {
        if (valueC != null) {
            setC(new ArrayRealVector(valueC));
        }
    }

    /**
     * Set the linear objective function
     * 
     * @param vecC linear objective function
     **/
    public void setC(final RealVector vecC) {
        this.c = vecC;
    }

    /**
     * Get the linear inequalities constraints matrix
     * 
     * @return linear inequalities constraints matrix
     **/
    public RealMatrix getG() {
        return g;
    }

    /**
     * Set the linear inequalities constraints matrix
     * 
     * @param valueG linear inequalities constraints matrix
     **/
    public void setG(final double[][] valueG) {
        if (valueG != null) {
            setG(new BlockRealMatrix(valueG));
        }
    }

    /**
     * Set the linear inequalities constraints matrix
     * 
     * @param matG linear inequalities constraints matrix
     **/
    public void setG(final RealMatrix matG) {
        this.g = matG;
    }

    /**
     * Get the linear inequalities constraints coefficients
     * 
     * @return linear inequalities constraints coefficients
     **/
    public RealVector getH() {
        return h;
    }

    /**
     * Set the linear inequalities constraints coefficients
     * 
     * @param valueH linear inequalities constraints coefficients
     **/
    public void setH(final double[] valueH) {
        if (valueH != null) {
            setH(new ArrayRealVector(valueH));
        }
    }

    /**
     * Set the linear inequalities constraints coefficients
     * 
     * @param vecH linear inequalities constraints coefficients
     **/
    public void setH(final RealVector vecH) {
        this.h = vecH;
    }

    /**
     * Get the lower bounds
     * 
     * @return lower bounds 
     **/
    public RealVector getLb() {
        return this.lb;
    }

    /**
     * Set the lower bounds
     * 
     * @param lB lower bounds 
     **/
    public void setLb(final double[] lB) {
        if (lB != null) {
            setLb(new ArrayRealVector(lB));
        }
    }

    /**
     * Set the lower bounds
     * 
     * @param lbVector lower bounds 
     **/
    public void setLb(final RealVector lbVector) {
        for (int i = 0; i < lbVector.getDimension(); i++) {
            final double lbi = lbVector.getEntry(i);
            if (Double.isNaN(lbi) || Double.isInfinite(lbi)) {
                throw new IllegalArgumentException("The lower bounds can not be set to Double.NaN or Double.INFINITY");
            }
        }
        this.lb = lbVector;
    }

    /**
     * Get the upper bounds
     * 
     * @return upper bounds 
     **/
    public RealVector getUb() {
        return this.ub;
    }

    /**
     * Set the upper bounds
     * 
     * @param uB upper bounds 
     **/
    public void setUb(final double[] uB) {
        if (uB != null) {
            setUb(new ArrayRealVector(uB));
        }
    }

    /**
     * Set the upper bounds
     * 
     * @param uB upper bounds 
     **/
    public void setUb(final RealVector uB) {
        for (int i = 0; i < uB.getDimension(); i++) {
            final double ubi = uB.getEntry(i);
            if (Double.isNaN(ubi) || Double.isInfinite(ubi)) {
                throw new IllegalArgumentException("The upper bounds can not be set to Double.NaN or Double.INFINITY");
            }
        }
        this.ub = uB;
    }

    /**
     * Get the Lagrangian lower bounds for linear constraints (A rows)
     * 
     * @return Lagrangian lower bounds for linear constraints 
     **/
    public RealVector getYlb() {
        return this.ylb;
    }

    /**
     * Set the Lagrangian lower bounds for linear constraints (A rows)
     * 
     * @param ylbVector Lagrangian lower bounds for linear constraints 
     **/
    public void setYlb(final RealVector ylbVector) {
        this.ylb = ylbVector;
    }

    /**
     * Get the Lagrangian upper bounds for linear constraints (A rows)
     * 
     * @return Lagrangian upper bounds for linear constraints 
     **/
    public RealVector getYub() {
        return this.yub;
    }


    /**
     * Set the Lagrangian upper bounds for linear constraints (A rows)
     * 
     * @param yubVector Lagrangian upper bounds for linear constraints 
     **/
    public void setYub(final RealVector yubVector) {
        this.yub = yubVector;
    }

    /**
     * Get the Lagrangian upper bounds for linear bounds
     * 
     * @return Lagrangian upper bounds for linear bounds
     **/
    public RealVector getZlb() {
        return this.zlb;
    }

    /**
     * Set the Lagrangian upper bounds for linear bounds
     * 
     * @param zlbVector Lagrangian upper bounds for linear bounds
     **/
    public void setZlb(final RealVector zlbVector) {
        this.zlb = zlbVector;
    }

    /**
     * Get the Lagrangian upper bounds for upper bounds
     * 
     * @return Lagrangian upper bounds for upper bounds
     **/
    public RealVector getZub() {
        return this.zub;
    }

    /**
     * Set the Lagrangian upper bounds for upper bounds
     * 
     * @param zubVector Lagrangian upper bounds for upper bounds
     **/
    public void setZub(final RealVector zubVector) {
        this.zub = zubVector;
    }

    /**
     * If true, no method for making normal equations sparser will be applied during the presolving phase.
     * 
     * @return true/false 
     **/
    public boolean isAvoidPresolvingIncreaseSparsity() {
        return avoidPresolvingIncreaseSparsity;
    }

    /**
     * Set true if no method for making normal equations sparser will be applied during the presolving phase
     * or false otherwise
     * 
     * @param avoidPIS true/false
     **/
    public void setAvoidPresolvingIncreaseSparsity(final boolean avoidPIS) {
        this.avoidPresolvingIncreaseSparsity = avoidPIS;
    }

    /**
     * If true, no methods that cause fill-in in the original matrices will be called during the presolving phase.
     * 
     * @return true/false 
     **/  
    public boolean isAvoidPresolvingFillIn() {
        return avoidPresolvingFillIn;
    }

    /**
     * Set true if no methods that cause fill-in in the original matrices will be called during the presolving phase
     * or false otherwise
     * 
     * @param avoidPFI true/false
     **/  
    public void setAvoidPresolvingFillIn(final boolean avoidPFI) {
        this.avoidPresolvingFillIn = avoidPFI;
    }

    /**
     * Should LP presolving be disabled?
     * 
     * @return true/false
     **/ 
    public boolean isPresolvingDisabled() {
        return this.presolvingDisabled;
    }

    /**
     * Set if LP presolving should be disabled
     * 
     * @param presolvingDis true/false
     **/ 
    public void setPresolvingDisabled(final boolean presolvingDis) {
        this.presolvingDisabled = presolvingDis;
    }
    
    /**
     * Check if the bound conditions on the optimal equality constraints Lagrangian coefficients are respected.
     * 
     * @return true/false
     **/ 
    public boolean isCheckOptimalLagrangianBounds() {
        return this.checkOptimalLagrangianBounds;
    }

    /**
     * Set if the bound conditions on the optimal equality constraints Lagrangian coefficients are respected.
     * 
     * @param checkOLB true/false
     **/ 
    public void setCheckOptimalLagrangianBounds(final boolean checkOLB) {
        this.checkOptimalLagrangianBounds = checkOLB;
    }

    /**
     * Set the objective function to minimize
     */
    @Override
    public void setF0(final ConvexMultivariateRealFunction f0) {
        throw new UnsupportedOperationException(MATRIX_FORM);
    }

    /**
     * Set the inequalities constraints array
     */
    @Override
    public void setFi(final ConvexMultivariateRealFunction[] fi) {
        throw new UnsupportedOperationException(MATRIX_FORM);
    }

    /**
     * Convert into string
     */
    //CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: complex JOptimizer code kept as such
    @Override
    public String toString() {
        //CHECKSTYLE: resume CyclomaticComplexity check
        //try {
        final StringBuffer sb = new StringBuffer();
        // saves into sb the problem description 
        sb.append(this.getClass().getName() + ": ");
        // look for all the possible variables in the problem
        // first write the problem description with letters
        sb.append("\nmin(c) s.t.");
        if (getG() != null && getG().getRowDimension() > 0) {
            sb.append("\nG.x < h");
        }
        if (getA() != null && getA().getRowDimension() > 0) {
            sb.append("\nA.x = b");
        }
        if (getLb() != null && getUb() != null) {
            sb.append("\nlb <= x <= ub");
        } else if (getLb() != null) {
            sb.append("\nlb <= x");
        } else if (getUb() != null) {
            sb.append("\nx <= ub");
        }
        // then write the variables values
        sb.append("\nc: " + getC().toString());
        if (g != null) {
            // convert G and h values into string
            sb.append("\nG: " + g.toString());
            sb.append("\nh: " + h.toString());
        }
        if (getA() != null) {
            sb.append("\nA: " + getA().toString());
            sb.append("\nb: " + getB().toString());
        }
        if (getLb() != null) {
            sb.append("\nlb: " + getLb().toString());
        }
        if (getUb() != null) {
            sb.append("\nub: " + getUb().toString());
        }
        if (getYlb() != null) {
            sb.append("\nylb: " + getYlb().toString());
        }
        if (getYub() != null) {
            sb.append("\nyub: " + getYub().toString());
        }
        if (getZlb() != null) {
            sb.append("\nzlb: " + getZlb().toString());
        }
        if (getZub() != null) {
            sb.append("\nzub: " + getZub().toString());
        }

        return sb.toString(); // Returns a string representation of the object
    }

    /**
     * Clone a linear problem request
     * @return cloned linear problem request
     */
    public LPOptimizationRequest cloneMe() {
        // creates the cloned request
        final LPOptimizationRequest clonedLPRequest = new LPOptimizationRequest();
        // sets the new request with all the information
        clonedLPRequest.setToleranceFeas(getToleranceFeas());
        clonedLPRequest.setPresolvingDisabled(isPresolvingDisabled());
        clonedLPRequest.setRescalingDisabled(isRescalingDisabled());
        clonedLPRequest.setAvoidPresolvingFillIn(isAvoidPresolvingFillIn());
        clonedLPRequest.setAvoidPresolvingIncreaseSparsity(isAvoidPresolvingIncreaseSparsity());
        clonedLPRequest.setCheckOptimalLagrangianBounds(isCheckOptimalLagrangianBounds());
        clonedLPRequest.setAlpha(getAlpha());
        clonedLPRequest.setBeta(getBeta());
        clonedLPRequest.setCheckKKTSolutionAccuracy(isCheckKKTSolutionAccuracy());
        clonedLPRequest.setToleranceKKT(getToleranceKKT());
        clonedLPRequest.setCheckProgressConditions(isCheckProgressConditions());
        clonedLPRequest.setMaxIteration(getMaxIteration());
        clonedLPRequest.setMu(getMu());
        clonedLPRequest.setTolerance(getTolerance());
        // returns the request cloned
        return clonedLPRequest; //cloned request
    }
}
