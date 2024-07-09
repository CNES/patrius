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
 */
package fr.cnes.sirius.patrius.math.optim.joptimizer.optimizers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import fr.cnes.sirius.patrius.math.linear.ArrayRealVector;
import fr.cnes.sirius.patrius.math.linear.BlockRealMatrix;
import fr.cnes.sirius.patrius.math.linear.MatrixUtils;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealVector;
import fr.cnes.sirius.patrius.math.optim.joptimizer.algebra.AlgebraUtils;
import fr.cnes.sirius.patrius.math.optim.joptimizer.algebra.Matrix1NornRescaler;
import fr.cnes.sirius.patrius.math.optim.joptimizer.algebra.MatrixRescaler;
import fr.cnes.sirius.patrius.math.optim.joptimizer.util.ArrayUtils;
import fr.cnes.sirius.patrius.math.optim.joptimizer.util.Utils;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.PatriusRuntimeException;

//CHECKSTYLE: stop MethodLength check
//CHECKSTYLE: stop NestedBlockDepth check
//CHECKSTYLE: stop CyclomaticComplexity check
//CHECKSTYLE: stop ModifiedControlVariable check
//Reason: JOptimizer complex code kept as such

/**
 * Presolver for a linear problem in the form of:
 * 
 * min(c) s.t.
 * A.x = b
 * lb <= x <= ub
 * 
 * <br>
 * Note 1: unboundedLBValue is the distinctive value of an unbounded lower bound. It must be one of
 * the values:
 * <ol>
 * <li>Double.NaN (the default)</li>
 * <li>Double.NEGATIVE_INFINITY</li>
 * </ol>
 * Note 2: unboundedUBValue is the distinctive value of an unbounded upper bound. It must be one of
 * the values:
 * <ol>
 * <li>Double.NaN (the default)</li>
 * <li>Double.POSITIVE_INFINITY</li>
 * </ol>
 * Note 3: if lb is null, each variable lower bound will be assigned the value of
 * <i>unboundedLBValue</i> <br>
 * Note 4: if ub is null, each variable upper bound will be assigned the value of
 * <i>unboundedUBValue</i>
 * 
 * @author alberto trivellato (alberto.trivellato@gmail.com)
* HISTORY
* VERSION:4.8:FA:FA-2954:15/11/2021:[PATRIUS] Problemes lors de l'integration de JOptimizer dans Patrius 
* VERSION:4.8:FA:FA-2956:15/11/2021:[PATRIUS] Temps de propagation non implemente pour certains evenements 
* VERSION:4.7:DM:DM-2766:18/05/2021:Evol. et corr. dans le package fr.cnes.sirius.patrius.math.linear (suite DM 2300) 
* VERSION:4.6:DM:DM-2591:27/01/2021:[PATRIUS] Intigration et validation JOptimizer
* END-HISTORY
 * @see E.D. Andersen, K.D. Andersen, "Presolving in linear programming"
 * @see Jacek Gondzio
 *      "Presolve analysis of linear programs prior to applying an interior point method"
 * @see Xin Huang, "Preprocessing and Postprocessing in Linear Optimization"
 * @since 4.6
 */
public class LPPresolver {

    /** Default unbounded lower bound */
    public static final double DEFAULT_UNBOUNDED_LOWER_BOUND = Double.NaN;
    /** Default unbounded upper bound */
    public static final double DEFAULT_UNBOUNDED_UPPER_BOUND = Double.NaN;

    /** Scalar */
    private static final double SCALAR1 = 1.e-7;
    /** Scalar */
    private static final double SCALAR2 = 1.01;
    /** Message */
    private static final String NOTEMPTY = " but was not empty";
    /** Message */
    private static final String EMPTYCOL = "Expected empty column ";
    /** Message */
    private static final String BRACKET1 = "]";
    /** Message */
    private static final String BRACKET3 = "]=";
    /** Message */
    private static final String BRACKET4 = "*x[";
    /** Message */
    private static final String BRACKET6 = "x[";
    /** Message */
    private static final String PLUS = "+";

    /** Epsilon number */
    private double eps = Utils.getDoubleMachineEpsilon();
    /**
     * If true, no method for making normal equations sparser will be applied.
     * 
     * @see Jacek Gondzio "Presolve analysis of linear programs prior to applying an interior point method", 3
     */
    private boolean avoidIncreaseSparsity = false;
    /**
     * If true, no methods that cause fill-in in the original matrices will be called.
     */
    private boolean avoidFillIn = false;
    /**
     * If true, no scaling on constraints matrices will be applied.
     */
    private boolean avoidScaling = false;
    /** Unbounded lower bound */
    private double unboundedLBValue = DEFAULT_UNBOUNDED_LOWER_BOUND;
    /** Unbounded upper bound */
    private double unboundedUBValue = DEFAULT_UNBOUNDED_UPPER_BOUND;
    /** Original number of variables */
    private int originalN;
    /** Original number of equalities */
    private int originalMeq;
    /**
     * If the problem is in standard form, this is the number of slack variables (expected to be the first variables of
     * the problem).
     */
    private int nOfSlackVariables = -1;

    // after presolving fields;
    /** Presolved number of variables */
    private int presolvedN = -1;
    /** Presolved number of equalities */
    private int presolvedMeq = -1;
    /** Are the variables independent? */
    private boolean[] indipendentVariables;
    /** Presolved array X */
    private int[] presolvedX;
    /** Presolved vector C */
    private RealVector presolvedC = null;
    /** Presolved matrix A */
    private RealMatrix presolvedA = null;
    /** Presolved vector B */
    private RealVector presolvedB = null;
    /** Presolved lower bounds vector */
    private RealVector presolvedLB = null;
    /** Presolved upper bounds vector */
    private RealVector presolvedUB = null;
    /** Presolved Y lower bounds vector */
    private RealVector presolvedYlb = null;
    /** Presolved Y upper bounds vector */
    private RealVector presolvedYub = null;
    /** Presolved Z lower bounds vector */
    private RealVector presolvedZlb = null;
    /** Presolved Z upper bounds vector */
    private RealVector presolvedZub = null;

    /**
     * Row by row non-zeroes entries of A.
     */
    private int[][] vRowPositions;
    /**
     * Column by column non-zeroes entries of A.
     */
    private int[][] vColPositions;

    /** min constraints values (g[i] <= A[i,j].x) */
    private double[] g;
    /** max constraints values (h[i] >= A[i,j].x) */
    private double[] h;
    /** Row length map */
    private int[][] vRowLengthMap;
    /** Column length map */
    private int[][] vColLengthMap;

    /** Is some reduction done? */
    private boolean someReductionDone = true;
    /** Vector used for rescaling */
    private RealVector t = null;
    /** Vector used for rescaling */
    private RealVector r = null;
    /** Minimum rescaled lower bounds */
    private double minRescaledLB = Double.NaN;// used for rescaling
    /** Minimum rescaled upper bounds */
    private double maxRescaledUB = Double.NaN;// used for rescaling
    /** Presolving stack */
    private final List<AbstractPresolvingStackElement> presolvingStack =
        new ArrayList<LPPresolver.AbstractPresolvingStackElement>();
    /** Expected solution */
    private double[] expectedSolution;// for testing purpose
    /** Expected tolerance */
    private double expectedTolerance = Double.NaN;// for testing purpose

    /**
     * Default constructor
     */
    public LPPresolver() {
        this(DEFAULT_UNBOUNDED_LOWER_BOUND, DEFAULT_UNBOUNDED_UPPER_BOUND);
    }

    /**
     * Constructor
     * 
     * @param unboundedLBVal
     *            unbounded lower bound
     * @param unboundedUBVal
     *            unbounded upper bound
     */
    public LPPresolver(final double unboundedLBVal, final double unboundedUBVal) {
        if (!Double.isNaN(unboundedLBVal) && !Double.isInfinite(unboundedLBVal)) {
            throw new IllegalArgumentException(
                    "The field unboundedLBValue must be set to Double.NaN or Double.NEGATIVE_INFINITY");
        }
        if (!Double.isNaN(unboundedUBVal) && !Double.isInfinite(unboundedUBVal)) {
            throw new IllegalArgumentException(
                    "The field unboundedUBValue must be set to Double.NaN or Double.POSITIVE_INFINITY");
        }
        // this.unspecifiedLBValue = unspecifiedLBValue;
        // this.unspecifiedUBValue = unspecifiedUBValue;
        this.unboundedLBValue = unboundedLBVal;
        this.unboundedUBValue = unboundedUBVal;
    }

    /**
     * If true, no method for making normal equations sparser will be applied.
     * 
     * @return true/false
     **/
    public boolean isAvoidIncreaseSparsity() {
        return avoidIncreaseSparsity;
    }

    /**
     * Set true if no method for making normal equations sparser will be applied or false otherwise
     * 
     * @param avoidIS
     *            true/false
     **/
    public void setAvoidIncreaseSparsity(final boolean avoidIS) {
        this.avoidIncreaseSparsity = avoidIS;
    }

    /**
     * Set true if no methods that cause fill-in in the original matrices will be called or false otherwise
     * 
     * @param avoidFI
     *            true/false
     **/
    public void setAvoidFillIn(final boolean avoidFI) {
        this.avoidFillIn = avoidFI;
    }

    /**
     * Set if the matrix scaling should be disabled (true) or not (false)
     * 
     * @param avoidS
     *            true/false
     */
    public void setAvoidScaling(final boolean avoidS) {
        this.avoidScaling = avoidS;
    }

    /**
     * Presolve
     * 
     * @param originalC
     *            original C vector
     * @param originalA
     *            original A matrix
     * @param originalB
     *            original B vector
     * @param originalLB
     *            original LB vector
     * @param originalUB
     *            original UB vector
     */
    public void presolve(final double[] originalC, final double[][] originalA, final double[] originalB,
        final double[] originalLB, final double[] originalUB) {
        RealMatrix aMatrix = null;
        RealVector bVector = null;
        if (originalA != null) {
            aMatrix = new BlockRealMatrix(originalA);
            bVector = new ArrayRealVector(originalB);
        }
        if (originalLB != null && originalUB != null) {
            if (originalLB.length != originalUB.length) {
                // dimension mismatch between upper and lower bounds
                throw new IllegalArgumentException("lower and upper bounds have different lenght");
            }
        }
        final RealVector lbVector;
        // initialize lower bounds vector
        if (originalLB != null) {
            lbVector = new ArrayRealVector(originalLB);
        } else {
            lbVector = null;
        }
        // initialize upper bounds vector
        final RealVector ubVector;
        if (originalUB != null) {
            ubVector = new ArrayRealVector(originalUB);
        } else {
            ubVector = null;
        }
        // calls the presolve method for realmatrix and realvector
        presolve(new ArrayRealVector(originalC), aMatrix, bVector, lbVector, ubVector);
    }

    /**
     * Presolve
     * 
     * @param originalC
     *            original C vector
     * @param originalA
     *            original A matrix
     * @param originalB
     *            original B vector
     * @param originalLB
     *            original LB vector
     * @param originalUB
     *            original UB vector
     */
    public void presolve(final RealVector originalC, final RealMatrix originalA, final RealVector originalB,
        final RealVector originalLB, final RealVector originalUB) {

        // working entities definition
        this.originalN = (originalA != null) ? originalA.getColumnDimension() : originalLB.getDimension();
        this.originalMeq = (originalA != null) ? originalA.getRowDimension() : 0;
        this.indipendentVariables = new boolean[originalN];
        Arrays.fill(indipendentVariables, true);

        this.g = new double[originalN];
        this.h = new double[originalN];

        RealVector originalLBCopy = null;
        RealVector originalUBCopy = null;
        if (originalLB == null) {
            originalLBCopy = new ArrayRealVector(originalN, unboundedLBValue);
        } else {
            originalLBCopy = originalLB.copy();
        }
        if (originalUB == null) {
            originalUBCopy = new ArrayRealVector(originalN, unboundedUBValue);
        } else {
            originalUBCopy = originalUB.copy();
        }
        for (int i = 0; i < originalN; i++) {
            if (originalUBCopy.getEntry(i) < originalLBCopy.getEntry(i)) {
                throw new PatriusRuntimeException(PatriusMessages.INFEASIBLE_PROBLEM, null);
            }
        }

        final RealMatrix a;
        if (originalA != null) {
            a = new BlockRealMatrix(this.originalMeq, this.originalN);
        } else {
            a = null;
        }

        vRowLengthMap = new int[originalN + 1][];// the first position is for 0-length rows
        vColLengthMap = new int[originalMeq + 1][];// the first position is for 0-length columns
        final int[] vColCounter = new int[originalN];// counter of non-zero values in each column
        vRowPositions = new int[originalMeq][0];
        for (int i = 0; i < originalMeq; i++) {
            int[] vRowPositionsI = new int[]{};
            for (int j = 0; j < originalN; j++) {
                final double originalAIJ = originalA.getEntry(i, j);
                if (!isZero(originalAIJ)) {
                    vRowPositionsI = ArrayUtils.add(vRowPositionsI, vRowPositionsI.length, j);
                    vColCounter[j]++;
                    a.setEntry(i, j, originalAIJ);
                }
            }
            // check empty row
            if (vRowPositionsI.length < 1) {
                if (!isZero(originalB.getEntry(i))) {
                    throw new PatriusRuntimeException(PatriusMessages.INFEASIBLE_PROBLEM, null);
                }
            }
            vRowPositions[i] = vRowPositionsI;
            if (this.vRowLengthMap[vRowPositionsI.length] == null) {
                vRowLengthMap[vRowPositionsI.length] = new int[]{i};
            } else {
                vRowLengthMap[vRowPositionsI.length] = addToSortedArray(vRowLengthMap[vRowPositionsI.length], i);
            }
        }

        // primal variables lower bounds
        final RealVector lb = originalLBCopy.copy();// this will change during the process
        // primal variables upper bounds
        final RealVector ub = originalUBCopy.copy();// this will change during the process
        // check empty columns
        for (int j = 0; j < vColCounter.length; j++) {
            if (vColCounter[j] == 0) {
                // empty column
                if (originalC.getEntry(j) > 0) {
                    if (isLBUnbounded(lb.getEntry(j))) {
                        throw new PatriusRuntimeException(PatriusMessages.UNBOUNDED_PROBLEM, null);
                    } else {
                        // variable j fixed at its lower bound
                        ub.setEntry(j, lb.getEntry(j));
                    }
                } else if (originalC.getEntry(j) < 0) {
                    if (isUBUnbounded(ub.getEntry(j))) {
                        throw new PatriusRuntimeException(PatriusMessages.UNBOUNDED_PROBLEM, null);
                    } else {
                        // variable j fixed at its upper bound
                        lb.setEntry(j, ub.getEntry(j));
                    }
                }
            }
        }

        // fill not-zero columns holders
        this.vColPositions = new int[originalN][];
        for (int j = 0; j < originalN; j++) {
            final int length = vColCounter[j];
            this.vColPositions[j] = new int[length];
            if (this.vColLengthMap[length] == null) {
                vColLengthMap[length] = new int[]{j};
            } else {
                vColLengthMap[length] = addToSortedArray(vColLengthMap[length], j);
            }
        }
        for (int i = 0; i < vRowPositions.length; i++) {
            final int[] vRowPositionsI = vRowPositions[i];
            for (int j = 0; j < vRowPositionsI.length; j++) {
                final int col = vRowPositionsI[j];
                this.vColPositions[col][vColPositions[col].length - vColCounter[col]] = i;
                vColCounter[vRowPositionsI[j]]--;
            }
        }

        final RealVector c = originalC.copy();
        final RealVector b;// this will change during the process

        if (originalB != null) {
            b = originalB.copy();
        } else {
            b = null;
        }

        // lagrangian lower bounds for linear constraints (A rows)
        final RealVector ylb = new ArrayRealVector(originalMeq, unboundedLBValue);
        // lagrangian upper bounds for linear constraints (A rows)
        final RealVector yub = new ArrayRealVector(originalMeq, unboundedUBValue);
        // lagrangian lower bounds for lb constraints
        final RealVector zlb = new ArrayRealVector(originalN, unboundedLBValue);
        // lagrangian upper bounds for ub constraints
        final RealVector zub = new ArrayRealVector(originalN, unboundedUBValue);

        // pre-presolving check
        checkProgress(c, a, b, lb, ub, ylb, yub, zlb, zub);

        // remove all fixed variables
        removeFixedVariables(c, a, b, lb, ub, ylb, yub, zlb, zub);

        // repeat
        int iteration = 0;
        while (someReductionDone) {
            iteration++;
            someReductionDone = false;// reset
            checkProgress(c, a, b, lb, ub, ylb, yub, zlb, zub);
            // Check rows
            // Remove fixed variables
            removeFixedVariables(c, a, b, lb, ub, ylb, yub, zlb, zub);
            checkProgress(c, a, b, lb, ub, ylb, yub, zlb, zub);
            // Remove all row singletons
            removeSingletonRows(c, a, b, lb, ub, ylb, yub, zlb, zub);
            checkProgress(c, a, b, lb, ub, ylb, yub, zlb, zub);
            // Remove all forcing constraints
            removeForcingConstraints(c, a, b, lb, ub, ylb, yub, zlb, zub);
            checkProgress(c, a, b, lb, ub, ylb, yub, zlb, zub);
            // tight the bounds
            if (iteration < 5) {
                // the higher the iteration, the less useful it is
                compareBounds(c, a, b, lb, ub, ylb, yub, zlb, zub);
                checkProgress(c, a, b, lb, ub, ylb, yub, zlb, zub);
            }
            // Dominated constraints
            // Remove all dominated constraints
            removeDominatedConstraints(c, a, b, lb, ub, ylb, yub, zlb, zub);
            checkProgress(c, a, b, lb, ub, ylb, yub, zlb, zub);
            // Check columns
            // Remove all free, implied free column singletons and
            // all column singletons in combination with a doubleton equation
            checkColumnSingletons(c, a, b, lb, ub, ylb, yub, zlb, zub);
            checkProgress(c, a, b, lb, ub, ylb, yub, zlb, zub);
            // Dominated columns
            removeDominatedColumns(c, a, b, lb, ub, ylb, yub, zlb, zub);
            checkProgress(c, a, b, lb, ub, ylb, yub, zlb, zub);
            if (!avoidIncreaseSparsity) {
                // Duplicate rows
                removeDuplicateRow(c, a, b, lb, ub, ylb, yub, zlb, zub);
                checkProgress(c, a, b, lb, ub, ylb, yub, zlb, zub);
                // Duplicate columns
                removeDuplicateColumn(c, a, b, lb, ub, ylb, yub, zlb, zub);
                checkProgress(c, a, b, lb, ub, ylb, yub, zlb, zub);
            }
            // Remove row doubleton
            if (!avoidFillIn) {
                removeDoubletonRow(c, a, b, lb, ub, ylb, yub, zlb, zub);
                checkProgress(c, a, b, lb, ub, ylb, yub, zlb, zub);
            }

            // Remove empty rows from the indexed list
            // removeEmptyRows();
        }
        removeAllEmptyRowsAndColumns(c, a, b, lb, ub, ylb, yub, zlb, zub);

        presolvedN = 0;
        presolvedX = new int[originalN];// longer than it needs
        Arrays.fill(presolvedX, -1);
        final int[] presolvedPositions = new int[originalN];
        Arrays.fill(presolvedPositions, -1);
        for (int j = 0; j < indipendentVariables.length; j++) {
            if (indipendentVariables[j]) {
                presolvedX[presolvedN] = j;
                presolvedPositions[j] = presolvedN;
                presolvedN++;
            }
        }

        presolvedMeq = 0;
        for (int i = 0; i < vRowPositions.length; i++) {
            if (vRowPositions[i].length > 0) {
                presolvedMeq++;
            }
        }

        if (presolvedMeq > 0) {
            presolvedA = new BlockRealMatrix(presolvedMeq, presolvedN);
            presolvedB = new ArrayRealVector(presolvedMeq);
            presolvedYlb = new ArrayRealVector(presolvedMeq);
            presolvedYub = new ArrayRealVector(presolvedMeq);
        }
        if (presolvedN > 0) {
            presolvedC = new ArrayRealVector(presolvedN);
            presolvedLB = new ArrayRealVector(presolvedN);
            presolvedUB = new ArrayRealVector(presolvedN);
            presolvedZlb = new ArrayRealVector(presolvedN);
            presolvedZub = new ArrayRealVector(presolvedN);
        }
        int cntR = 0;
        for (int i = 0; presolvedA != null && i < vRowPositions.length; i++) {
            final int[] vRowPositionsI = vRowPositions[i];
            if (vRowPositionsI.length > 0) {
                for (int j = 0; j < vRowPositionsI.length; j++) {
                    final int jnz = vRowPositionsI[j];
                    final int col = presolvedPositions[jnz];
                    presolvedA.setEntry(cntR, col, a.getEntry(i, jnz));
                    presolvedB.setEntry(cntR, b.getEntry(i));
                }
                cntR++;
            }
        }
        cntR = 0;
        for (int i = 0; i < vRowPositions.length; i++) {
            if (vRowPositions[i].length > 0) {
                presolvedYlb.setEntry(cntR, ylb.getEntry(i));
                presolvedYub.setEntry(cntR, yub.getEntry(i));
                cntR++;
            }
        }
        for (int j = 0; j < presolvedN; j++) {
            final int col = presolvedX[j];
            presolvedC.setEntry(j, c.getEntry(col));
            presolvedLB.setEntry(j, lb.getEntry(col));
            presolvedUB.setEntry(j, ub.getEntry(col));
            presolvedZlb.setEntry(j, zlb.getEntry(col));
            presolvedZub.setEntry(j, zub.getEntry(col));
        }

        objectiveFunctionNormalization();

        if (!avoidScaling) {
            scaling();
        }
    }

    /**
     * Objective function normalization.
     */
    private void objectiveFunctionNormalization() {
        if (this.presolvedC != null && this.presolvedC.getDimension() > 0) {
            final double normC = presolvedC.getNorm();
            if (normC > 0) {
                this.presolvedC.mapMultiplyToSelf(1 / normC);
            }
        }
    }

    /**
     * From the full x, gives back its presolved elements.
     * 
     * @param x
     *            full x
     * @return presolved elements
     */
    public double[] presolve(final double[] x) {
        if (x.length != originalN) {
            // dimension mismatch
            throw new IllegalArgumentException("wrong array dimension: " + x.length);
        }
        // create a new array to save presolved results
        final double[] presolX = Arrays.copyOf(x, x.length);
        for (int i = 0; i < presolvingStack.size(); i++) {
            presolvingStack.get(i).preSolve(presolX);
        }
        final double[] ret = new double[presolvedN];
        int cntPosition = 0;
        for (int i = 0; i < presolX.length; i++) {
            if (indipendentVariables[i]) {
                ret[cntPosition] = presolX[i];
                cntPosition++;
            }
        }
        if (this.t != null) {
            // rescaling has been done:
            // x = T.x1
            for (int i = 0; i < ret.length; i++) {
                ret[i] = ret[i] / t.getEntry(i);
            }
        }
        return ret; // return presolved elements
    }

    /**
     * From the full x, gives back its postsolved elements.
     * 
     * @param x
     *            full x
     * @return postsolved elements
     */
    public double[] postsolve(final double[] x) {

        final double[] postsolvedX = new double[originalN];

        if (this.t != null) {
            // rescaling has been done: x = T.x1
            for (int i = 0; i < x.length; i++) {
                x[i] = t.getEntry(i) * x[i];
            }
        }

        // copy x element into postsolved array
        for (int i = 0; i < x.length; i++) {
            postsolvedX[presolvedX[i]] = x[i];
        }
        // compute post solved elements
        for (int i = presolvingStack.size() - 1; i > -1; i--) {
            presolvingStack.get(i).postSolve(postsolvedX);
        }
        return postsolvedX;
    }

    /**
     * Remove fixed variables A variable corresponding to a column of the coefficient matrix A is said to be a fixed
     * variable if the lower bound on the variable equals the upper bound on that variable
     * 
     * @param c
     *            original C vector
     * @param a
     *            original A matrix
     * @param b
     *            original B vector
     * @param lb
     *            original lower bounds vector
     * @param ub
     *            original upper bounds vector
     * @param ylb
     *            original Y lower bounds vector
     * @param yub
     *            original Y upper bounds vector
     * @param zlb
     *            original Y lower bounds vector
     * @param zub
     *            original Y upper bounds vector
     */
    private void removeFixedVariables(final RealVector c, final RealMatrix a, final RealVector b, final RealVector lb,
        final RealVector ub, final RealVector ylb, final RealVector yub, final RealVector zlb, final RealVector zub) {
        for (int j = 0; j < indipendentVariables.length; j++) {
            if (indipendentVariables[j]) {
                // this is an active variable
                if (!isLBUnbounded(lb.getEntry(j)) && isZero(lb.getEntry(j) - ub.getEntry(j))) {
                    // variable x is fixed
                    final double v = lb.getEntry(j);
                    addToPresolvingStack(new LinearDependency(j, null, null, v));

                    // substitution into objective function @TODO

                    // substitution
                    for (int k = 0; k < this.vRowPositions.length; k++) {
                        final int[] vRowPositionsK = vRowPositions[k];
                        for (int i = 0; i < vRowPositionsK.length; i++) {
                            if (vRowPositionsK[i] == j) {
                                if (vRowPositionsK.length == 1) {
                                    // this row contains only xj
                                    if (!isZero(v - b.getEntry(k) / a.getEntry(k, j))) {
                                        // Infeasible problem
                                        throw new PatriusRuntimeException(PatriusMessages.INFEASIBLE_PROBLEM, null);
                                    }
                                }
                                b.setEntry(k, b.getEntry(k) - a.getEntry(k, j) * v);
                                vRowPositions[k] = removeElementFromSortedArray(vRowPositionsK, j);
                                changeRowsLengthPosition(k, vRowPositions[k].length + 1, vRowPositions[k].length);
                                a.setEntry(k, j, 0);
                                break;
                            }
                            if (vRowPositionsK[i] > j) {
                                break;// the array is sorted
                            }
                        }
                    }
                    changeColumnsLengthPosition(j, vColPositions[j].length, 0);
                    vColPositions[j] = new int[]{};
                    this.someReductionDone = true;
                }
            }
        }
    }

    /**
     * Remove singleton rows A row of the matrix A is said to be a singleton row if only one element in that row is
     * non-zero
     * 
     * @param c
     *            original C vector
     * @param a
     *            original A matrix
     * @param b
     *            original B vector
     * @param lb
     *            original lower bounds vector
     * @param ub
     *            original upper bounds vector
     * @param ylb
     *            original Y lower bounds vector
     * @param yub
     *            original Y upper bounds vector
     * @param zlb
     *            original Y lower bounds vector
     * @param zub
     *            original Y upper bounds vector
     */
    private void removeSingletonRows(final RealVector c, final RealMatrix a, final RealVector b, final RealVector lb,
        final RealVector ub, final RealVector ylb, final RealVector yub, final RealVector zlb, final RealVector zub) {
        int i = 0;
        while (i < vRowPositions.length) {
            final int[] vRowPositionsI = vRowPositions[i];
            if (vRowPositionsI.length == 1) {
                // singleton found: A(i,j).x(j) = b(i)
                final int j = vRowPositionsI[0];
                final double aIJ = a.getEntry(i, j);
                final double xj = b.getEntry(i) / aIJ;
                addToPresolvingStack(new LinearDependency(j, null, null, xj));

                // substitution into the other equations
                for (int k = 0; k < this.vRowPositions.length; k++) {
                    if (k != i) {
                        final int[] vRowPositionsK = vRowPositions[k];
                        for (int nz = 0; nz < vRowPositionsK.length; nz++) {
                            if (vRowPositionsK[nz] == j) {
                                // this row contains xj at position nz
                                if (vRowPositionsK.length == 1) {
                                    if (!isZero(xj - b.getEntry(k) / a.getEntry(k, j))) {
                                        // infeasible problem
                                        throw new PatriusRuntimeException(PatriusMessages.INFEASIBLE_PROBLEM, null);
                                    }
                                }
                                b.setEntry(k, b.getEntry(k) - a.getEntry(k, j) * xj);
                                a.setEntry(k, j, 0.);
                                vRowPositions[k] = (int[]) ArrayUtils.remove(vRowPositionsK, nz);

                                changeRowsLengthPosition(k, vRowPositions[k].length + 1, vRowPositions[k].length);
                                break;
                            } else if (vRowPositionsK[nz] > j) {
                                break;
                            }
                        }
                    }
                }
                a.setEntry(i, j, 0.);
                b.setEntry(i, 0.);
                changeColumnsLengthPosition(j, vColPositions[j].length, 0);
                vColPositions[j] = new int[]{};
                vRowPositions[i] = (int[]) ArrayUtils.remove(vRowPositionsI, 0);// this row has
                                                                                // only this
                                                                                // nz-entry
                changeRowsLengthPosition(i, vRowPositions[i].length + 1, vRowPositions[i].length);
                this.someReductionDone = true;
                i = -1;// restart
            }
            i++;
        }
    }

    /**
     * Remove forcing constraints A row i of the matrix A is said to have a forcing constraint if the values of g(i) and
     * h(i) are such that either h(i) is equal to b(i) or b(i) is equal to g(i)
     * 
     * @param c
     *            original C vector
     * @param a
     *            original A matrix
     * @param b
     *            original B vector
     * @param lb
     *            original lower bounds vector
     * @param ub
     *            original upper bounds vector
     * @param ylb
     *            original Y lower bounds vector
     * @param yub
     *            original Y upper bounds vector
     * @param zlb
     *            original Y lower bounds vector
     * @param zub
     *            original Y upper bounds vector
     */
    private void removeForcingConstraints(final RealVector c, final RealMatrix a, final RealVector b,
        final RealVector lb, final RealVector ub, final RealVector ylb, final RealVector yub, final RealVector zlb,
        final RealVector zub) {
        for (int i = 0; i < vRowPositions.length; i++) {
            final int[] vRowPositionsI = vRowPositions[i];
            if (vRowPositionsI.length > 0) {
                g[i] = 0.;
                h[i] = 0.;
                boolean allPositive = true;
                boolean allNegative = true;
                boolean allLbPositive = true;
                boolean allUbFinite = true;
                for (int nz = 0; nz < vRowPositionsI.length; nz++) {
                    final int j = vRowPositionsI[nz];
                    final double aij = a.getEntry(i, j);
                    if (aij > 0) {
                        // j in P
                        g[i] += aij * lb.getEntry(j);
                        h[i] += aij * ub.getEntry(j);
                        allNegative = false;
                    } else if (aij < 0) {
                        // j in M
                        g[i] += aij * ub.getEntry(j);
                        h[i] += aij * lb.getEntry(j);
                        allPositive = false;
                    }
                    allLbPositive = allLbPositive && lb.getEntry(j) >= 0;
                    allUbFinite = allUbFinite && !isUBUnbounded(ub.getEntry(j));
                }

                if (h[i] < b.getEntry(i) || b.getEntry(i) < g[i]) {
                    throw new PatriusRuntimeException(PatriusMessages.INFEASIBLE_PROBLEM, null);
                }

                int[] forcedVariablesI = new int[]{};
                if (isZero(g[i] - b.getEntry(i))) {
                    // forcing constraint
                    // the only feasible value of xj is l[j] (u[j]) if A(i,j) > 0 (A(i,j) < 0).
                    // Therefore, we can fix all variables in the ith constraint.
                    for (int nz = 0; nz < vRowPositionsI.length; nz++) {
                        final int j = vRowPositionsI[nz];
                        final double aij = a.getEntry(i, j);
                        if (aij > 0) {
                            ub.setEntry(j, lb.getEntry(j));
                        } else {
                            lb.setEntry(j, ub.getEntry(j));
                        }
                        forcedVariablesI = ArrayUtils.add(forcedVariablesI, j);
                        addToPresolvingStack(new LinearDependency(j, null, null, lb.getEntry(j)));
                    }
                } else if (isZero(h[i] - b.getEntry(i))) {
                    // forcing constraint
                    // the only feasible value of xj is u[j] (l[j]) if A(i,j) > 0 (A(i,j) < 0).
                    // Therefore, we can fix all variables in the ith constraint.
                    for (int nz = 0; nz < vRowPositionsI.length; nz++) {
                        final int j = vRowPositionsI[nz];
                        final double aij = a.getEntry(i, j);
                        if (aij > 0) {
                            lb.setEntry(j, ub.getEntry(j));
                        } else {
                            ub.setEntry(j, lb.getEntry(j));
                        }
                        forcedVariablesI = ArrayUtils.add(forcedVariablesI, j);
                        addToPresolvingStack(new LinearDependency(j, null, null, lb.getEntry(j)));
                    }
                }
                if (forcedVariablesI.length > 0) {
                    // there are forced variables to substitute
                    for (int fv = 0; fv < forcedVariablesI.length; fv++) {
                        final int j = forcedVariablesI[fv];
                        final double xj = lb.getEntry(j);
                        // substitution into the other equations
                        for (int k = 0; k < this.vRowPositions.length; k++) {
                            if (k != i) {
                                final int[] vRowPositionsK = vRowPositions[k];
                                for (int nz = 0; nz < vRowPositionsK.length; nz++) {
                                    if (vRowPositionsK[nz] == j) {
                                        // this row contains x[j]
                                        if (vRowPositionsK.length == 1) {
                                            if (!isZero(xj - b.getEntry(k) / a.getEntry(k, j))) {
                                                throw new PatriusRuntimeException(PatriusMessages.INFEASIBLE_PROBLEM,
                                                        null);
                                            }
                                        }
                                        b.setEntry(k, b.getEntry(k) - a.getEntry(k, j) * xj);
                                        a.setEntry(k, j, 0.);
                                        vRowPositions[k] = (int[]) ArrayUtils.remove(vRowPositionsK, nz);
                                        changeRowsLengthPosition(k, vRowPositions[k].length + 1,
                                            vRowPositions[k].length);
                                        changeColumnsLengthPosition(j, vColPositions[j].length,
                                            vColPositions[j].length - 1);
                                        vColPositions[j] = (int[]) ArrayUtils.remove(vColPositions[j], 0);// ordered
                                                                                                          // row loop
                                        break;
                                    } else if (vRowPositionsK[nz] > j) {
                                        break;
                                    }
                                }
                            }
                        }
                        a.setEntry(i, j, 0.);
                        b.setEntry(i, 0.);
                        vRowPositions[i] = removeElementFromSortedArray(vRowPositions[i], j);// this
                                                                                             // row
                                                                                             // has
                                                                                             // only
                                                                                             // this
                                                                                             // nz-entry
                        changeRowsLengthPosition(i, vRowPositions[i].length + 1, vRowPositions[i].length);
                        if (vColPositions[j].length != 1 && vColPositions[j][0] != j) {
                            throw new IllegalStateException(EMPTYCOL + j + NOTEMPTY);
                        }
                        changeColumnsLengthPosition(j, vColPositions[j].length, 0);
                        vColPositions[j] = new int[]{};
                        this.someReductionDone = true;
                    }
                    // cancel the row
                    if (vRowPositions[i].length > 0) {
                        throw new IllegalStateException("Expected empty row " + i + NOTEMPTY);
                    }
                    vRowPositions[i] = new int[]{};
                    b.setEntry(i, 0);
                    continue;
                }

                // check if we can tight upper bounds. leveraging the fact that, typically,
                // the problem has lb => 0 for most variables.
                // if coefficients are all positive or all negative the bounds can be limited
                final boolean aa = g[i] >= 0 && b.getEntry(i) >= 0;
                final boolean bb = h[i] <= 0 && b.getEntry(i) <= 0;
                final boolean t1 = aa && allPositive;
                final boolean t2 = bb && allNegative;// same as above with a sign change
                if (t1 || t2) {
                    boolean sameSignReductionDone = false;
                    for (int nz = 0; nz < vRowPositionsI.length; nz++) {
                        final int nzj = vRowPositionsI[nz];
                        final double d = b.getEntry(i) / a.getEntry(i, nzj);
                        // we can limit upper bound to d
                        if (isUBUnbounded(ub.getEntry(nzj)) || ub.getEntry(nzj) > d) {
                            ub.setEntry(nzj, d);
                            sameSignReductionDone = true;
                        }
                    }
                    if (sameSignReductionDone) {
                        this.someReductionDone = true;
                    }
                }
            }
        }
    }

    /**
     * Compare bounds
     * 
     * @param c
     *            original C vector
     * @param a
     *            original A matrix
     * @param b
     *            original B vector
     * @param lb
     *            original lower bounds vector
     * @param ub
     *            original upper bounds vector
     * @param ylb
     *            original Y lower bounds vector
     * @param yub
     *            original Y upper bounds vector
     * @param zlb
     *            original Y lower bounds vector
     * @param zub
     *            original Y upper bounds vector
     */
    private void compareBounds(final RealVector c, final RealMatrix a, final RealVector b, final RealVector lb,
        final RealVector ub, final RealVector ylb, final RealVector yub, final RealVector zlb, final RealVector zub) {
        final int treshold;
        // if avoidFillIn=true
        // doubleton are not managed
        if (avoidFillIn) {
            treshold = 2;
        } else {
            treshold = 3;
        }
        for (int i = 0; i < vRowPositions.length; i++) {
            final int[] vRowPositionsI = vRowPositions[i];
            if (vRowPositionsI.length >= treshold) {

                // define SP(x) = Sum[Aij * xj], Aij>=0
                // define SM(x) = Sum[-Aij * xj], Aij<0
                // we have SP(x) = b[i] + SM(x)

                boolean allLbPositive = true;
                int cntSP = 0;
                int cntSM = 0;
                boolean allSPLbFinite = true;
                boolean allSPUbFinite = true;
                boolean allSMLbFinite = true;
                boolean allSMUbFinite = true;
                for (int nz = 0; nz < vRowPositionsI.length; nz++) {
                    final int j = vRowPositionsI[nz];
                    allLbPositive = allLbPositive && lb.getEntry(j) >= 0;
                    final double aij = a.getEntry(i, j);
                    if (aij >= 0) {
                        cntSP++;
                        allSPLbFinite = allSPLbFinite && !isLBUnbounded(lb.getEntry(j));
                        allSPUbFinite = allSPUbFinite && !isUBUnbounded(ub.getEntry(j));
                    } else {
                        cntSM++;
                        allSMLbFinite = allSMLbFinite && !isLBUnbounded(lb.getEntry(j));
                        allSMUbFinite = allSMUbFinite && !isUBUnbounded(ub.getEntry(j));
                    }
                    // break conditions
                    if (!(allLbPositive || allSPLbFinite || allSPUbFinite)) {
                        break;
                    }
                    if (!(allSMLbFinite || allSMUbFinite)) {
                        break;
                    }
                }

                if (allLbPositive) { // all entries of lower bounds vector are positive
                    if (allSPUbFinite) {
                        // we have SM < SP(ub) - b[i]
                        // so ub[j] < (SP(ub) - b[i]) / -Aij, for each j in P
                        double spub = 0;
                        for (int nz = 0; nz < vRowPositionsI.length; nz++) {
                            final int j = vRowPositionsI[nz];
                            final double aij = a.getEntry(i, j);
                            if (aij >= 0) {
                                spub += aij * ub.getEntry(j); // Sum[Aij * xj]
                            }
                        }
                        for (int nz = 0; nz < vRowPositionsI.length; nz++) {
                            final int j = vRowPositionsI[nz];
                            final double aij = a.getEntry(i, j);
                            if (aij < 0) {
                                if (isUBUnbounded(ub.getEntry(j)) || ub.getEntry(j) > -(spub - b.getEntry(i)) / aij) {
                                    ub.setEntry(j, -(spub - b.getEntry(i)) / aij); // (SP(ub) - b[i]) / -Aij
                                    this.someReductionDone = true;
                                }
                            }
                        }
                    }
                    if (allSMUbFinite) {
                        // we have SP < b[i] + SM(ub)
                        // so ub[j] < (b[i] + SM(ub)) / Aij, for each j in SP
                        double smub = 0;
                        for (int nz = 0; nz < vRowPositionsI.length; nz++) {
                            final int j = vRowPositionsI[nz];
                            final double aij = a.getEntry(i, j);
                            if (aij <= 0) {
                                smub -= aij * ub.getEntry(j); // Sum[-Aij * xj]
                            }
                        }
                        for (int nz = 0; nz < vRowPositionsI.length; nz++) {
                            final int j = vRowPositionsI[nz];
                            final double aij = a.getEntry(i, j);
                            if (aij > 0) {
                                if (isUBUnbounded(ub.getEntry(j)) || ub.getEntry(j) > (b.getEntry(i) + smub) / aij) {
                                    ub.setEntry(j, (b.getEntry(i) + smub) / aij); // (b[i] + SM(ub)) / Aij
                                    this.someReductionDone = true;
                                }
                            }
                        }
                    }
                    if (cntSM == 1 && allSPLbFinite) {
                        // we have SM > -b[i] + SP(lb)
                        // so lb[m] > (-b[i] + SM(lb)) / Aim
                        double splb = 0;
                        int m = -1;
                        for (int nz = 0; nz < vRowPositionsI.length; nz++) {
                            final int j = vRowPositionsI[nz];
                            final double aij = a.getEntry(i, j);
                            if (aij >= 0) {
                                splb += aij * lb.getEntry(j);
                            } else {
                                m = j;
                            }
                        }
                        final double aim = -a.getEntry(i, m);
                        if (isLBUnbounded(lb.getEntry(m)) || lb.getEntry(m) < (-b.getEntry(i) + splb) / aim) {
                            lb.setEntry(m, (-b.getEntry(i) + splb) / aim); // (-b[i] + SM(lb)) / Aim
                            this.someReductionDone = true;
                        }
                    }
                    if (cntSP == 1 && allSMLbFinite) {
                        // we have SP > b[i] + SM(lb)
                        // so lb[p] > (b[i] + SM(lb)) / Aip
                        double smlb = 0;
                        int p = -1;
                        for (int nz = 0; nz < vRowPositionsI.length; nz++) {
                            final int j = vRowPositionsI[nz];
                            final double aij = a.getEntry(i, j);
                            if (aij <= 0) {
                                smlb -= aij * lb.getEntry(j);
                            } else {
                                p = j;
                            }
                        }
                        final double aip = a.getEntry(i, p);
                        if (isLBUnbounded(lb.getEntry(p)) || lb.getEntry(p) < (b.getEntry(i) + smlb) / aip) {
                            lb.setEntry(p, (b.getEntry(i) + smlb) / aip); // (b[i] + SM(lb)) / Aip
                            this.someReductionDone = true;
                        }
                    }
                }
            }
        }
    }

    /**
     * Remove dominated constraints
     * 
     * @param c
     *            original C vector
     * @param a
     *            original A matrix
     * @param b
     *            original B vector
     * @param lb
     *            original lower bounds vector
     * @param ub
     *            original upper bounds vector
     * @param ylb
     *            original Y lower bounds vector
     * @param yub
     *            original Y upper bounds vector
     * @param zlb
     *            original Y lower bounds vector
     * @param zub
     *            original Y upper bounds vector
     */
    private void removeDominatedConstraints(final RealVector c, final RealMatrix a, final RealVector b,
        final RealVector lb, final RealVector ub, final RealVector ylb, final RealVector yub, final RealVector zlb,
        final RealVector zub) {
        // intentionally left empty
    }

    /**
     * Manages: -)free column singletons -)doubleton equations combined with a column singleton -)implied free column
     * singletons
     * 
     * @param c
     *            original C vector
     * @param a
     *            original A matrix
     * @param b
     *            original B vector
     * @param lb
     *            original lower bounds vector
     * @param ub
     *            original upper bounds vector
     * @param ylb
     *            original Y lower bounds vector
     * @param yub
     *            original Y upper bounds vector
     * @param zlb
     *            original Y lower bounds vector
     * @param zub
     *            original Y upper bounds vector
     */
    private void checkColumnSingletons(final RealVector c, final RealMatrix a, final RealVector b, final RealVector lb,
        final RealVector ub, final RealVector ylb, final RealVector yub, final RealVector zlb, final RealVector zub) {
        for (int col = 0; col < this.vColPositions.length; col++) {
            if (vColPositions[col].length == 1) {
                final int row = vColPositions[col][0];
                int[] vRowPositionsRow = vRowPositions[row];
                if (vRowPositionsRow.length < 2) {
                    continue;// this is a fixed variable
                }

                final double arCol = a.getEntry(row, col);
                final double cCol = c.getEntry(col);
                final boolean isCColNz = !isZero(cCol);
                double lbCol = lb.getEntry(col);
                double ubCol = ub.getEntry(col);
                final boolean isLBUnbounded = isLBUnbounded(lbCol);
                final boolean isUBUnbounded = isUBUnbounded(ubCol);

                if (isLBUnbounded || isUBUnbounded) {
                    // bound on one of the optimal Lagrange multipliers.
                    if (isLBUnbounded) {
                        if (isUBUnbounded) {
                            // table 2, row 1
                            zlb.setEntry(col, 0);
                            zub.setEntry(col, 0);
                            ylb.setEntry(row, cCol / arCol);
                            yub.setEntry(row, cCol / arCol);
                        } else {
                            if (arCol > 0) {
                                // table 2, row 4
                                zub.setEntry(col, 0);
                                ylb.setEntry(row, cCol / arCol);
                            } else {
                                // table 2, row 5
                                zub.setEntry(col, 0);
                                yub.setEntry(row, cCol / arCol);
                            }
                        }
                    } else {
                        if (isUBUnbounded) {
                            if (arCol > 0) {
                                // table 2, row 2
                                zlb.setEntry(col, 0);
                                yub.setEntry(row, cCol / arCol);
                            } else {
                                // table 2, row 3
                                zlb.setEntry(col, 0);
                                ylb.setEntry(row, cCol / arCol);
                            }
                        }
                    }

                    if (isLBUnbounded && isUBUnbounded) {
                        // free column singleton: one constraint and one variable
                        // is removed from the problem without generating any fill-ins in A,
                        // although the objective function is modified
                        // substitution into the objective function
                        final int[] xi = new int[vRowPositionsRow.length - 1];
                        final double[] mi = new double[vRowPositionsRow.length - 1];
                        int cntXi = 0;
                        for (int j = 0; j < vRowPositionsRow.length; j++) {
                            final int nzJ = vRowPositionsRow[j];
                            if (nzJ != col) {
                                xi[cntXi] = nzJ;
                                mi[cntXi] = -a.getEntry(row, nzJ) / arCol;
                                cntXi++;
                                if (isCColNz) {
                                    c.setEntry(nzJ, c.getEntry(nzJ) - cCol * a.getEntry(row, nzJ) / arCol);
                                }
                            }
                        }
                        // see Andersen & Andersen, eq (10) [that is incorrect!]
                        addToPresolvingStack(new LinearDependency(col, xi, mi, b.getEntry(row)));
                        for (int j = 0; j < vRowPositionsRow.length; j++) {
                            final int column = vRowPositionsRow[j];// the nz column index
                            if (column != col && vColPositions[column].length == 1) {
                                // this is also a column singleton, we do not want an empty final
                                // column
                                // so we fix the value of the variable
                                // @TODO: fix this for unbounded bounds
                                if (c.getEntry(column) < 0) {
                                    lb.setEntry(column, ub.getEntry(column));
                                } else {
                                    ub.setEntry(column, lb.getEntry(column));
                                }
                                addToPresolvingStack(new LinearDependency(column, null, null, lb.getEntry(column)));
                                pruneFixedVariable(column, c, a, b, lb, ub, ylb, yub, zlb, zub);
                            }
                            changeColumnsLengthPosition(column, vColPositions[column].length,
                                vColPositions[column].length - 1);
                            vColPositions[column] = removeElementFromSortedArray(vColPositions[column], row);
                            a.setEntry(row, column, 0.);
                        }
                        changeRowsLengthPosition(row, vRowPositions[row].length, 0);
                        vRowPositions[row] = new int[]{};
                        vColPositions[col] = new int[]{};
                        ylb.setEntry(row, cCol / arCol);// ok, but jet stated above
                        yub.setEntry(row, cCol / arCol);// ok, but jet stated above
                        b.setEntry(row, 0);
                        lb.setEntry(col, this.unboundedLBValue);
                        ub.setEntry(col, this.unboundedUBValue);
                        c.setEntry(col, 0);
                        this.someReductionDone = true;
                        continue;
                    }
                }

                final double impliedL;
                final double impliedU;
                if (arCol > 0) {
                    impliedL = (b.getEntry(row) - h[row]) / arCol + ubCol;
                    impliedU = (b.getEntry(row) - g[row]) / arCol + lbCol;
                } else {
                    impliedL = (b.getEntry(row) - g[row]) / arCol + ubCol;
                    impliedU = (b.getEntry(row) - h[row]) / arCol + lbCol;
                }
                final boolean ifl = impliedL > lbCol;// do not use =, it will cause a loop
                final boolean ifu = impliedU < ubCol;// do not use =, it will cause a loop
                if (ifl) {
                    lb.setEntry(col, impliedL);// tighten the bounds
                    lbCol = impliedL;
                    this.someReductionDone = true;
                }
                if (ifu) {
                    ub.setEntry(col, impliedU);// tighten the bounds
                    ubCol = impliedU;
                    this.someReductionDone = true;
                }
                final boolean isImpliedFree = (ifl && ifu) || (isZero(impliedL - lbCol) && isZero(impliedU - ubCol));

                if (vRowPositionsRow.length == 2 || isImpliedFree) {
                    // substitution
                    int y = -1;
                    double q = 0.;
                    double m = 0.;
                    final int[] xi = new int[vRowPositionsRow.length - 1];
                    final double[] mi = new double[vRowPositionsRow.length - 1];
                    final StringBuffer sb = new StringBuffer(BRACKET6 + col + BRACKET3);
                    q = b.getEntry(row) / arCol;
                    sb.append(q);
                    int cntXi = 0;
                    for (int j = 0; j < vRowPositionsRow.length; j++) {
                        final int nzJ = vRowPositionsRow[j];
                        if (nzJ != col) {
                            final double aRnzJ = a.getEntry(row, nzJ);
                            m = -aRnzJ / arCol;
                            xi[cntXi] = nzJ;
                            mi[cntXi] = m;
                            cntXi++;
                            sb.append(" + " + m + BRACKET4 + nzJ + BRACKET1);
                            if (isCColNz) {
                                // the objective function is modified
                                final double cc = c.getEntry(col) * aRnzJ / arCol;
                                c.setEntry(nzJ, c.getEntry(nzJ) - cc);
                            }
                            y = nzJ;
                        }
                    }
                    addToPresolvingStack(new LinearDependency(col, xi, mi, q));

                    if (vRowPositionsRow.length == 2) {
                        // NOTE: the row and the column are removed
                        // x = m*y + q, x column singleton
                        // addToDoubletonMap(col, y, m, q);
                        // the bounds on the variable y are modified so that the feasible region is
                        // unchanged even if
                        // the bounds on x are removed
                        // y = x/m - q/m
                        final double lbY = lb.getEntry(y);
                        final double ubY = ub.getEntry(y);
                        final boolean isLBYUnbounded = isLBUnbounded(lbY);
                        final boolean isUBYUnbounded = isLBUnbounded(ubY);
                        if (m > 0) {
                            if (!isLBUnbounded) {
                                final double l = lbCol / m - q / m;
                                if (isLBYUnbounded) {
                                    lb.setEntry(y, l);
                                } else {
                                    lb.setEntry(y, MathLib.max(lbY, l));
                                }
                            }
                            if (!isUBUnbounded) {
                                final double u = ubCol / m - q / m;
                                if (isUBYUnbounded) {
                                    ub.setEntry(y, u);
                                } else {
                                    ub.setEntry(y, MathLib.min(ubY, u));
                                }
                            }
                        } else {
                            if (!isUBUnbounded) {
                                final double u = ubCol / m - q / m;
                                if (isLBYUnbounded) {
                                    lb.setEntry(y, u);
                                } else {
                                    lb.setEntry(y, MathLib.max(lbY, u));
                                }
                            }
                            if (!isLBUnbounded) {
                                final double l = lbCol / m - q / m;
                                if (isUBYUnbounded) {
                                    ub.setEntry(y, l);
                                } else {
                                    ub.setEntry(y, MathLib.min(ubY, l));
                                }
                            }
                        }
                        if (vColPositions[y].length == 1) {
                            // this is also a column singleton, we do not want an empty final column
                            // so we fix the value of the variable
                            if (c.getEntry(y) < 0) {
                                lb.setEntry(y, ub.getEntry(y));
                            } else if (c.getEntry(y) > 0) {
                                ub.setEntry(y, lb.getEntry(y));
                            } else {
                                // any value is good
                                if (!isLBUnbounded(lb.getEntry(y)) && !isUBUnbounded(ub.getEntry(y))) {
                                    final double d = (ub.getEntry(y) + lb.getEntry(y)) / 2;
                                    lb.setEntry(y, d);
                                    ub.setEntry(y, d);
                                } else if (!isLBUnbounded(lb.getEntry(y))) {
                                    ub.setEntry(y, lb.getEntry(y));
                                } else {
                                    lb.setEntry(y, ub.getEntry(y));
                                }
                            }
                        }
                        // remove the bounds on col
                        lb.setEntry(col, this.unboundedLBValue);
                        ub.setEntry(col, this.unboundedUBValue);
                        // remove the variable
                        a.setEntry(row, col, 0.);
                        a.setEntry(row, y, 0.);
                        b.setEntry(row, 0.);
                        changeColumnsLengthPosition(col, vColPositions[col].length, 0);
                        vColPositions[col] = new int[]{};
                        vRowPositionsRow = removeElementFromSortedArray(vRowPositionsRow, col);// just
                                                                                               // to
                                                                                               // have
                                                                                               // vRowPositionsRow[0]
                        changeRowsLengthPosition(row, vRowPositionsRow.length + 1, vRowPositionsRow.length);
                        changeColumnsLengthPosition(vRowPositionsRow[0], vColPositions[vRowPositionsRow[0]].length,
                            vColPositions[vRowPositionsRow[0]].length - 1);
                        vColPositions[vRowPositionsRow[0]] =
                            removeElementFromSortedArray(vColPositions[vRowPositionsRow[0]], row);
                        vRowPositions[row] = new int[]{};
                        this.someReductionDone = true;
                        continue;
                    } else {
                        // NOTE: one constraint and one variable is removed from the problem
                        // without generating any fill-ins in A,
                        // although the objective function is modified
                        ylb.setEntry(row, c.getEntry(col) / a.getEntry(row, col));// ok, but already
                                                                                  // stated above
                        yub.setEntry(row, c.getEntry(col) / a.getEntry(row, col));// ok, but already
                                                                                  // stated above
                        for (int cc = 0; cc < vRowPositions[row].length; cc++) {
                            final int column = vRowPositions[row][cc];
                            if (column == col) {
                                continue;
                            }
                            if (vColPositions[column].length == 1) {
                                // this is also a column singleton, we do not want an empty final
                                // column
                                // so we fix the value of the variable
                                if (c.getEntry(column) < 0) {
                                    // no problem of unbounded bound, this is an implied free column
                                    if (isUBUnbounded(ub.getEntry(column))) {
                                        throw new PatriusRuntimeException(PatriusMessages.UNBOUNDED_PROBLEM, null);
                                    }
                                    lb.setEntry(column, ub.getEntry(column));
                                } else if (c.getEntry(column) > 0) {
                                    // no problem of unbounded bound, this is an implied free column
                                    if (isLBUnbounded(lb.getEntry(column))) {
                                        throw new PatriusRuntimeException(PatriusMessages.UNBOUNDED_PROBLEM, null);
                                    }
                                    ub.setEntry(column, lb.getEntry(column));
                                } else {
                                    // no problem of unbounded bound, this is an implied free column
                                    if (isLBUnbounded(lb.getEntry(column)) || isUBUnbounded(ub.getEntry(column))) {
                                        throw new PatriusRuntimeException(PatriusMessages.UNBOUNDED_PROBLEM, null);
                                    }
                                    final double d = (ub.getEntry(y) - lb.getEntry(y)) / 2;
                                    lb.setEntry(y, d);
                                    ub.setEntry(y, d);
                                }
                                addToPresolvingStack(new LinearDependency(column, null, null, lb.getEntry(column)));
                                pruneFixedVariable(column, c, a, b, lb, ub, ylb, yub, zlb, zub);
                            }
                            changeColumnsLengthPosition(column, vColPositions[column].length,
                                vColPositions[column].length - 1);
                            vColPositions[column] = removeElementFromSortedArray(vColPositions[column], row);
                            a.setEntry(row, column, 0.);
                        }
                        a.setEntry(row, col, 0.);
                        b.setEntry(row, 0);
                        lb.setEntry(col, this.unboundedLBValue);
                        ub.setEntry(col, this.unboundedUBValue);
                        c.setEntry(col, 0);
                        changeColumnsLengthPosition(col, vColPositions[col].length, 0);
                        vColPositions[col] = new int[]{};
                        changeRowsLengthPosition(row, vRowPositions[row].length, 0);
                        vRowPositions[row] = new int[]{};
                        this.someReductionDone = true;
                        // checkProgress(c, A, b, lb, ub, ylb, yub, zlb, zub);
                        continue;
                    }
                }
            }
        }
    }

    /**
     * Remove dominated columns NOTE: this presolving technique needs no corresponding postsolving.
     * 
     * A column j of the matrix A is said to be a dominated column if the values of e(j) and d(j) are such that either
     * c(j)– d(j) is greater than zero or c(j)–e(j) is less than zero.
     * 
     * @param c
     *            original C vector
     * @param a
     *            original A matrix
     * @param b
     *            original B vector
     * @param lb
     *            original lower bounds vector
     * @param ub
     *            original upper bounds vector
     * @param ylb
     *            original Y lower bounds vector
     * @param yub
     *            original Y upper bounds vector
     * @param zlb
     *            original Y lower bounds vector
     * @param zub
     *            original Y upper bounds vector
     */
    private void removeDominatedColumns(final RealVector c, final RealMatrix a, final RealVector b, final RealVector lb,
        final RealVector ub, final RealVector ylb, final RealVector yub, final RealVector zlb, final RealVector zub) {
        for (int col = 0; col < this.vColPositions.length; col++) {
            final int[] vColPositionsCol = vColPositions[col];
            if (vColPositionsCol == null || vColPositionsCol.length == 0) {
                continue;
            }
            double e = 0.;
            double d = 0.;
            // it will be e <= d
            for (int i = 0; i < vColPositionsCol.length; i++) {
                final int row = vColPositionsCol[i];
                final double aij = a.getEntry(row, col);
                if (aij > 0) {
                    e += aij * ylb.getEntry(row);
                    d += aij * yub.getEntry(row);
                } else {
                    e += aij * yub.getEntry(row);
                    d += aij * ylb.getEntry(row);
                }
            }

            final double cmd = c.getEntry(col) - d;
            final double cme = c.getEntry(col) - e;
            final boolean isCmdPositive = cmd > 0 && !isZero(cmd);// strictly > 0
            final boolean isCmeNegative = cme < 0 && !isZero(cme);// strictly < 0
            final boolean isLBColUnbounded = isLBUnbounded(lb.getEntry(col));
            final boolean isUBColUnbounded = isUBUnbounded(ub.getEntry(col));

            if (isCmdPositive || isCmeNegative) {
                // dominated column
                if (isCmdPositive) {
                    zlb.setEntry(col, 0);
                    if (isLBColUnbounded) {
                        // unbounded problem
                        throw new PatriusRuntimeException(PatriusMessages.UNBOUNDED_PROBLEM, null);
                    }
                    ub.setEntry(col, lb.getEntry(col));
                    addToPresolvingStack(new LinearDependency(col, null, null, lb.getEntry(col)));
                    pruneFixedVariable(col, c, a, b, lb, ub, ylb, yub, zlb, zub);
                } else if (isCmeNegative) {
                    zub.setEntry(col, 0);
                    if (isUBColUnbounded) {
                        // unbounded problem
                        throw new PatriusRuntimeException(PatriusMessages.UNBOUNDED_PROBLEM, null);
                    }
                    lb.setEntry(col, ub.getEntry(col));
                    addToPresolvingStack(new LinearDependency(col, null, null, ub.getEntry(col)));
                    pruneFixedVariable(col, c, a, b, lb, ub, ylb, yub, zlb, zub);
                }
                continue;
            }

            // here we have cmd<=0 and cme>=0 (can even be unbounded)

            if (vColPositionsCol.length > 1) {// the column singletons are used to generate the
                                              // bounds d and e and
                                              // therefore they cannot be dropped with this test
                if (!isLBColUnbounded && isZero(cmd)) {
                    // weakly dominated column, see A. & A. (27), (28)
                    ub.setEntry(col, lb.getEntry(col));
                    addToPresolvingStack(new LinearDependency(col, null, null, lb.getEntry(col)));
                    pruneFixedVariable(col, c, a, b, lb, ub, ylb, yub, zlb, zub);
                    continue;
                }
                if (!isUBColUnbounded && isZero(cme)) {
                    // weakly dominated column, see A. & A. (27), (28)
                    lb.setEntry(col, ub.getEntry(col));
                    addToPresolvingStack(new LinearDependency(col, null, null, ub.getEntry(col)));
                    pruneFixedVariable(col, c, a, b, lb, ub, ylb, yub, zlb, zub);
                    continue;
                }
            }

            if (!isLBColUnbounded && isUBColUnbounded) {
                // new bounds on the optimal Lagrange multipliers y
                for (int i = 0; i < vColPositionsCol.length; i++) {
                    final int row = vColPositionsCol[i];
                    final double aij = a.getEntry(row, col);
                    if (aij > 0) {
                        if (!isUBUnbounded(cme / aij + ylb.getEntry(row))) {
                            // set new bounds on the optimal Lagrange multipliers:
                            yub.setEntry(row, Utils.min(yub.getEntry(row), cme / aij + ylb.getEntry(row)));
                        }
                    } else {
                        if (!isLBUnbounded(cme / aij + yub.getEntry(row))) {
                            ylb.setEntry(row, Utils.max(ylb.getEntry(row), cme / aij + yub.getEntry(row)));
                        }
                    }
                }
            }
            if (isLBColUnbounded && !isUBColUnbounded) {
                // new bounds on the optimal Lagrange multipliers y
                for (int i = 0; i < vColPositionsCol.length; i++) {
                    final int row = vColPositionsCol[i];
                    final double aij = a.getEntry(row, col);
                    if (aij > 0) {
                        if (!isLBUnbounded(cmd / aij + yub.getEntry(row))) {
                            // set new bounds on the optimal Lagrange multipliers:
                            ylb.setEntry(row, MathLib.max(ylb.getEntry(row), cmd / aij + yub.getEntry(row)));
                        }
                    } else {
                        if (!isUBUnbounded(cmd / aij + ylb.getEntry(row))) {
                            yub.setEntry(row, MathLib.min(yub.getEntry(row), cmd / aij + ylb.getEntry(row)));
                        }
                    }
                }
            }
        }
    }

    /**
     * Remove Duplicated rows NB: for the rows of A that contain the slack variables, there cannot be the same sparsity
     * pattern (A is diagonal in its right-upper part)
     * 
     * Two rows k and i are said to be duplicate rows if each element in rowi is a multiple v times the corresponding
     * element in row k in all the columns which are not singletons
     * 
     * @param c
     *            original C vector
     * @param a
     *            original A matrix
     * @param b
     *            original B vector
     * @param lb
     *            original lower bounds vector
     * @param ub
     *            original upper bounds vector
     * @param ylb
     *            original Y lower bounds vector
     * @param yub
     *            original Y upper bounds vector
     * @param zlb
     *            original Y lower bounds vector
     * @param zub
     *            original Y upper bounds vector
     */
    private void removeDuplicateRow(final RealVector c, final RealMatrix a, final RealVector b, final RealVector lb,
        final RealVector ub, final RealVector ylb, final RealVector yub, final RealVector zlb, final RealVector zub) {
        // the position 0 is for empty rows, 1 is for row singleton and 2 for row doubleton
        final int startingLength = 3;
        for (int i = startingLength; i < vRowLengthMap.length; i++) {
            final int[] vRowLengthMapI = vRowLengthMap[i];
            if (vRowLengthMapI == null || vRowLengthMapI.length < 1) {
                // no rows has this number of nz
                continue;
            }

            boolean stop = false;
            for (int j = 0; !stop && j < vRowLengthMapI.length; j++) {
                final int prow = vRowLengthMapI[j];// the row of A that has this number of nz
                if (vRowPositions[prow].length == 0 || prow < nOfSlackVariables) {
                    // the upper left part of A is diagonal if there are the slack variables:
                    // there is no sparsity superset possible
                    continue;
                }
                final int[] vRowPositionsProw = vRowPositions[prow];
                for (int si = i; !stop && si < vRowLengthMap.length; si++) {
                    // look into rows with superset sparsity pattern
                    final int[] vRowLengthMapSI = vRowLengthMap[si];
                    if (vRowLengthMapSI == null || vRowLengthMapSI.length < 1) {
                        continue;
                    }
                    for (int sj = 0; sj < vRowLengthMapSI.length; sj++) {
                        if (si == i && sj <= j) {
                            continue;// look forward, not behind
                        }
                        final int srow = vRowLengthMapSI[sj];
                        final int[] vRowPositionsSrow = vRowPositions[srow];
                        // same sparsity pattern?
                        if (isSubsetSparsityPattern(vRowPositionsProw, vRowPositionsSrow)) {
                            // found superset sparsity pattern
                            // look for the higher number of coefficients that can be deleted
                            final Map<Double, List<Integer>> coeffRatiosMap =
                                new ConcurrentHashMap<Double, List<Integer>>();
                            for (int k = 0; k < vRowPositionsProw.length; k++) {
                                final int col = vRowPositionsProw[k];
                                final double aprl = a.getEntry(prow, col);
                                final double asrl = a.getEntry(srow, col);
                                final double ratio = -asrl / aprl;
                                // put the ratio and the column index in the map
                                boolean added = false;
                                for (final Entry<Double, List<Integer>> entry : coeffRatiosMap.entrySet()) {
                                    final Double keyRatio = entry.getKey();
                                    if (isZero(ratio - keyRatio)) {
                                        coeffRatiosMap.get(keyRatio).add(col);
                                        added = true;
                                        break;
                                    }
                                }
                                if (!added) {
                                    final List<Integer> newList = new ArrayList<Integer>();
                                    newList.add(col);
                                    coeffRatiosMap.put(ratio, newList);
                                }
                            }
                            // take the ratio(s) with the higher number of column indexes
                            int maxNumberOfColumn = -1;
                            List<Integer> candidatedColumns = null;
                            for (final Entry<Double, List<Integer>> entry : coeffRatiosMap.entrySet()) {
                                final Double keyRatio = entry.getKey();
                                final int size = coeffRatiosMap.get(keyRatio).size();
                                if (size > maxNumberOfColumn) {
                                    maxNumberOfColumn = size;
                                    candidatedColumns = coeffRatiosMap.get(keyRatio);
                                } else if (size == maxNumberOfColumn) {
                                    candidatedColumns.addAll(coeffRatiosMap.get(keyRatio));
                                }
                            }

                            // look for the position with less column fill in
                            int lessFilledColumn = -1;// cannot be greater
                            int lessFilledColumnLength = this.originalMeq + 1;// cannot be greater
                            for (int k = 0; k < candidatedColumns.size(); k++) {
                                final int col = candidatedColumns.get(k).intValue();
                                if (vColPositions[col].length > 1
                                    && vColPositions[col].length < lessFilledColumnLength) {
                                    lessFilledColumn = col;
                                    lessFilledColumnLength = vColPositions[col].length;
                                }
                            }

                            final double aprl = a.getEntry(prow, lessFilledColumn);
                            final double asrl = a.getEntry(srow, lessFilledColumn);
                            final double alpha = -asrl / aprl;

                            b.setEntry(srow, b.getEntry(srow) + alpha * b.getEntry(prow));
                            // substitute A[prow] with A[prow] * alpha*A[row] for every nz entry of
                            // A[row]
                            for (int ti = 0; ti < vRowPositionsProw.length; ti++) {
                                final int cc = vRowPositionsProw[ti];
                                double nv = 0.;
                                if (cc != lessFilledColumn) {
                                    nv = a.getEntry(srow, cc) + alpha * a.getEntry(prow, cc);
                                }
                                a.setEntry(srow, cc, nv);
                                if (isZero(nv)) {
                                    vRowPositions[srow] = removeElementFromSortedArray(vRowPositions[srow], cc);
                                    changeColumnsLengthPosition(cc, vColPositions[cc].length,
                                        vColPositions[cc].length - 1);
                                    vColPositions[cc] = removeElementFromSortedArray(vColPositions[cc], srow);
                                    changeRowsLengthPosition(srow, vRowPositions[srow].length + 1,
                                        vRowPositions[srow].length);
                                    a.setEntry(srow, cc, 0.);
                                }
                            }
                            this.someReductionDone = true;
                            stop = true;
                            i = startingLength - 1;// restart, ++ comes from the for loop
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * Remove duplicated column
     * 
     * Two columns j and k are said to be duplicate columns if each element in column j is a multiple v times the
     * corresponding element in column k
     * 
     * @param c
     *            original C vector
     * @param a
     *            original A matrix
     * @param b
     *            original B vector
     * @param lb
     *            original lower bounds vector
     * @param ub
     *            original upper bounds vector
     * @param ylb
     *            original Y lower bounds vector
     * @param yub
     *            original Y upper bounds vector
     * @param zlb
     *            original Y lower bounds vector
     * @param zub
     *            original Y upper bounds vector
     */
    private void removeDuplicateColumn(final RealVector c, final RealMatrix a, final RealVector b, final RealVector lb,
        final RealVector ub, final RealVector ylb, final RealVector yub, final RealVector zlb, final RealVector zub) {
        // the position 0 is for empty columns, 1 is for column singleton
        final int startingLength = 2;
        for (int i = startingLength; i < vColLengthMap.length; i++) {
            final int[] vColLengthMapI = vColLengthMap[i];
            if (vColLengthMapI == null || vColLengthMapI.length < 1) {
                // no column has this number of nz
                continue;
            }

            final boolean stop = false;
            for (int j = 0; !stop && j < vColLengthMapI.length; j++) {
                final int pcol = vColLengthMapI[j];// the column of A that has this number of nz
                final int[] vColPositionsPcol = vColPositions[pcol];
                if (pcol < nOfSlackVariables) {
                    // the upper left part of A is diagonal if there are the slack variables:
                    // the sparsity pattern can not be the same
                    continue;
                }
                // look into the next columns with the same sparsity pattern
                for (int sj = j + 1; !stop && sj < vColLengthMapI.length; sj++) {
                    final int scol = vColLengthMapI[sj];
                    final int[] vColPositionsScol = vColPositions[scol];
                    if (isSameSparsityPattern(vColPositionsPcol, vColPositionsScol)) {
                        // found the same sparsity pattern
                        // check if pcol = alfa * srow
                        boolean isDuplicated = true;
                        final double v =
                            a.getEntry(vColPositionsPcol[0], pcol) / a.getEntry(vColPositionsScol[0], scol);
                        for (int k = 1; k < i; k++) {// "i" is the number of nz of pcol and scol
                            isDuplicated = isZero(
                                v - a.getEntry(vColPositionsPcol[k], pcol) / a.getEntry(vColPositionsScol[k], scol));
                            if (!isDuplicated) {
                                break;
                            }
                        }
                        if (!isDuplicated) {
                            continue;
                        } else {
                            // here we have pcol = alfa * scol
                            // NB: for table 3 and 4, j=pcol and k=scol
                            final double cAlfaC = c.getEntry(pcol) - v * c.getEntry(scol);
                            final boolean isLBPUnbounded = isLBUnbounded(lb.getEntry(pcol));
                            final boolean isUBPUnbounded = isUBUnbounded(ub.getEntry(pcol));
                            if (!isZero(cAlfaC)) {
                                // Fixing a duplicate column
                                // see table 3 of A. & A.
                                if (isUBUnbounded(ub.getEntry(scol)) && zlb.getEntry(scol) >= 0) {
                                    if (v >= 0 && cAlfaC > 0) {
                                        zlb.setEntry(pcol, 0);// table 3, row 1 (zj > 0)
                                        if (!isLBPUnbounded) {// check table 1, row 1
                                            if (isUBPUnbounded) {
                                                ub.setEntry(pcol, lb.getEntry(pcol));
                                                addToPresolvingStack(
                                                    new LinearDependency(pcol, null, null, lb.getEntry(pcol)));
                                                pruneFixedVariable(pcol, c, a, b, lb, ub, ylb, yub, zlb, zub);
                                            }
                                        } else {// check table 1, row 3
                                            throw new PatriusRuntimeException(PatriusMessages.UNBOUNDED_PROBLEM, null);
                                        }
                                        this.someReductionDone = true;
                                    } else if (v <= 0 && cAlfaC < 0) {
                                        zub.setEntry(pcol, 0);// table 3, row 2 (zj < 0)
                                        if (isLBPUnbounded) {// check table 1, row 2
                                            if (!isUBPUnbounded) {
                                                lb.setEntry(pcol, ub.getEntry(pcol));
                                                addToPresolvingStack(
                                                    new LinearDependency(pcol, null, null, ub.getEntry(pcol)));
                                                pruneFixedVariable(pcol, c, a, b, lb, ub, ylb, yub, zlb, zub);
                                            }
                                        } else {// check table 1, row 4
                                            throw new PatriusRuntimeException(PatriusMessages.UNBOUNDED_PROBLEM, null);
                                        }
                                        this.someReductionDone = true;
                                    }
                                }
                                if (isLBUnbounded(lb.getEntry(scol)) && zlb.getEntry(scol) <= 0) {
                                    if (v >= 0 && cAlfaC < 0) {
                                        zlb.setEntry(pcol, 0);// table 3, row 3 (zj < 0)
                                        if (isLBPUnbounded) {// check table 1, row 2
                                            if (!isUBPUnbounded) {
                                                lb.setEntry(pcol, ub.getEntry(pcol));
                                                addToPresolvingStack(
                                                    new LinearDependency(pcol, null, null, ub.getEntry(pcol)));
                                                pruneFixedVariable(pcol, c, a, b, lb, ub, ylb, yub, zlb, zub);
                                            }
                                        } else {// check table 1, row 4
                                            throw new PatriusRuntimeException(PatriusMessages.UNBOUNDED_PROBLEM, null);
                                        }
                                        this.someReductionDone = true;
                                    } else if (v <= 0 && cAlfaC > 0) {
                                        zub.setEntry(pcol, 0);// table 3, row 4 (zj > 0)
                                        if (!isLBPUnbounded) {// check table 1, row 1
                                            if (isUBPUnbounded) {
                                                ub.setEntry(pcol, lb.getEntry(pcol));
                                                addToPresolvingStack(
                                                    new LinearDependency(pcol, null, null, lb.getEntry(pcol)));
                                                pruneFixedVariable(pcol, c, a, b, lb, ub, ylb, yub, zlb, zub);
                                            }
                                        } else {// check table 1, row 3
                                            throw new PatriusRuntimeException(PatriusMessages.UNBOUNDED_PROBLEM, null);
                                        }
                                        this.someReductionDone = true;
                                    }
                                }
                            } else {// see A. & A. (46)
                                    // c[j] -v*c[k] = 0 (j=pcol and k=scol)
                                    // Replacing two duplicate columns by one:
                                    // modifies the bounds on variable scol according to Table 4
                                    // and removes variable pcol from the problem.
                                    // that is: the variable xj and the corresponding column j is
                                    // removed and
                                    // the new lower and upper bounds lb[k] and ub[k] on xk are
                                    // calculated as
                                    // given in that table
                                final boolean vp = v > 0;
                                final boolean vm = v < 0;
                                if (vp || vm) {
                                    // remove the variable pcol(i.e. j)
                                    for (int rr = 0; rr < vColPositionsPcol.length; rr++) {
                                        final int row = vColPositionsPcol[rr];
                                        vRowPositions[row] = removeElementFromSortedArray(vRowPositions[row], pcol);
                                        a.setEntry(row, pcol, 0.);
                                        changeRowsLengthPosition(row, vRowPositions[row].length + 1,
                                            vRowPositions[row].length);
                                    }
                                    changeColumnsLengthPosition(pcol, vColPositions[pcol].length, 0);
                                    vColPositions[pcol] = new int[]{};
                                    final DuplicatedColumn dc = new DuplicatedColumn(pcol, scol, scol, v,
                                            lb.getEntry(pcol), ub.getEntry(pcol), lb.getEntry(scol), ub.getEntry(scol));
                                    addToPresolvingStack(dc);
                                    if (vp) {
                                        lb.setEntry(scol, lb.getEntry(scol) + v * lb.getEntry(pcol));
                                        ub.setEntry(scol, ub.getEntry(scol) + v * ub.getEntry(pcol));
                                    } else if (vm) {
                                        lb.setEntry(scol, lb.getEntry(scol) + v * ub.getEntry(pcol));
                                        ub.setEntry(scol, ub.getEntry(scol) + v * lb.getEntry(pcol));
                                    }
                                    this.someReductionDone = true;

                                    // this is just for testing purpose
                                    if (expectedSolution != null) {
                                        // xk = -v*xj + xkPrime with
                                        // xj=pcol
                                        // xk=scol
                                        // xkPrime=scol
                                        // expectedSolution[scol] =
                                        // expectedSolution[scol]+v*expectedSolution[pcol];
                                        dc.preSolve(expectedSolution);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Remove doubleton rows NB: keep this method AFTER any other method that changes lb, ub, ylb, yub, zlb, zub: these
     * will be recalculated at the nex iteration. This method causes fill-in.
     * 
     * @param c
     *            original C vector
     * @param a
     *            original A matrix
     * @param b
     *            original B vector
     * @param lb
     *            original lower bounds vector
     * @param ub
     *            original upper bounds vector
     * @param ylb
     *            original Y lower bounds vector
     * @param yub
     *            original Y upper bounds vector
     * @param zlb
     *            original Y lower bounds vector
     * @param zub
     *            original Y upper bounds vector
     */
    private void removeDoubletonRow(final RealVector c, final RealMatrix a, final RealVector b, final RealVector lb,
        final RealVector ub, final RealVector ylb, final RealVector yub, final RealVector zlb, final RealVector zub) {
        for (int i = 0; i < this.vRowPositions.length; i++) {
            final int[] vRowPositionsI = vRowPositions[i];
            if (vRowPositionsI.length == 2) {
                final int x = vRowPositionsI[0];
                final int y = vRowPositionsI[1];
                // rx + sy = t;
                // x = - sy/r + t/r = my + q
                final double rVal = a.getEntry(i, x);
                final double s = a.getEntry(i, y);
                final double tVal = b.getEntry(i);
                final double m = -s / rVal;
                final double q = tVal / rVal;
                // found doubleton row
                addToPresolvingStack(new LinearDependency(x, new int[]{y}, new double[]{m}, q));
                // the bounds on the variable y are modified so that the feasible region is
                // unchanged even if the bounds
                // on x are removed
                // y = x/m - q/m
                final double lbX = lb.getEntry(x); // get x values
                final double ubX = ub.getEntry(x);
                final double lbY = lb.getEntry(y); // get y values
                final double ubY = ub.getEntry(y);
                // check if the bounds are unbounded
                final boolean isLBXUnbounded = isLBUnbounded(lbX);
                final boolean isUBXUnbounded = isLBUnbounded(ubX);
                final boolean isLBYUnbounded = isLBUnbounded(lbY);
                final boolean isUBYUnbounded = isLBUnbounded(ubY);
                // fill the bounds
                if (m > 0) {
                    if (!isLBXUnbounded) {
                        final double l = lbX / m - q / m;
                        if (isLBYUnbounded) {
                            lb.setEntry(y, l);
                        } else {
                            lb.setEntry(y, MathLib.max(lbY, l));
                        }
                    }
                    if (!isUBXUnbounded) {
                        final double u = ubX / m - q / m;
                        if (isUBYUnbounded) {
                            ub.setEntry(y, u);
                        } else {
                            ub.setEntry(y, MathLib.min(ubY, u));
                        }
                    }
                } else {
                    if (!isUBXUnbounded) {
                        final double u = ubX / m - q / m;
                        if (isLBYUnbounded) {
                            lb.setEntry(y, u);
                        } else {
                            lb.setEntry(y, MathLib.max(lbY, u));
                        }
                    }
                    if (!isLBXUnbounded) {
                        final double l = lbX / m - q / m;
                        if (isUBYUnbounded) {
                            ub.setEntry(y, l);
                        } else {
                            ub.setEntry(y, MathLib.min(ubY, l));
                        }
                    }
                }

                // substitution into objective function
                final double cc = c.getEntry(x) * s / rVal;
                c.setEntry(y, c.getEntry(y) - cc);

                // substitution: this can cause fill-in
                for (int k = 0; k < this.vRowPositions.length; k++) {
                    if (k != i) {
                        final int[] vRowPositionsK = vRowPositions[k];
                        for (int j = 0; j < vRowPositionsK.length; j++) {
                            if (vRowPositionsK[j] == x) {
                                final double akX = a.getEntry(k, x);
                                final double akY = a.getEntry(k, y);
                                final double akYnew = akY + akX * m;// this can be 0
                                if (!isZero(akYnew)) {
                                    // fill in
                                    a.setEntry(k, y, akYnew);
                                    if (!ArrayUtils.contains(vRowPositionsK, y)) {
                                        vRowPositions[k] = addToSortedArray(vRowPositionsK, y);
                                        changeRowsLengthPosition(k, vRowPositions[k].length - 1,
                                            vRowPositions[k].length);
                                        changeColumnsLengthPosition(y, vColPositions[y].length,
                                            vColPositions[y].length + 1);
                                        vColPositions[y] = addToSortedArray(vColPositions[y], k);
                                    }
                                } else {
                                    // remove
                                    vRowPositions[k] = removeElementFromSortedArray(vRowPositionsK, y);
                                    changeRowsLengthPosition(k, vRowPositions[k].length + 1, vRowPositions[k].length);
                                    changeColumnsLengthPosition(y, vColPositions[y].length,
                                        vColPositions[y].length - 1);
                                    vColPositions[y] = removeElementFromSortedArray(vColPositions[y], k);
                                    a.setEntry(k, y, 0.);
                                }
                                b.setEntry(k, b.getEntry(k) - akX * q);
                                a.setEntry(k, x, 0.);
                                vRowPositions[k] = removeElementFromSortedArray(vRowPositions[k], x);
                                changeRowsLengthPosition(k, vRowPositions[k].length + 1, vRowPositions[k].length);
                                changeColumnsLengthPosition(x, vColPositions[x].length, vColPositions[x].length - 1);
                                vColPositions[x] = removeElementFromSortedArray(vColPositions[x], k);
                                break;
                            } else if (vRowPositionsK[j] > x) {
                                break;// the array is sorted
                            }
                        }
                    }
                }

                // remove the row and the two columns
                vRowPositions[i] = new int[]{};
                if (vColPositions[x].length != 1 && vColPositions[x][0] != i) {
                    // Expected empty column, but was not empty
                    throw new IllegalStateException(EMPTYCOL + x + NOTEMPTY);
                }
                changeColumnsLengthPosition(x, vColPositions[x].length, 0);
                vColPositions[x] = new int[]{};
                changeColumnsLengthPosition(y, vColPositions[y].length, vColPositions[y].length - 1);
                vColPositions[y] = removeElementFromSortedArray(vColPositions[y], i);
                a.setEntry(i, x, 0.);
                a.setEntry(i, y, 0.);
                b.setEntry(i, 0);
                this.someReductionDone = true; // reduction done
            }
        }
    }

    /**
     * Given R the row scaling factor and T the column scaling factor, if x is the solution of the problem before
     * scaling and x1 is the solution of the problem after scaling, we have: <br>
     * R.A.T.x1 = Rb and x = T.x1
     * 
     * Every scaling needs the adjustment of the other data vectors of the LP problem. After scaling, the vectors c, lb,
     * ub become <br>
     * c -> T.c <br>
     * lb -> InvT.lb <br>
     * ub -> InvT.ub
     * 
     * The objective value is the same.
     * 
     * @see Xin Huang, "Preprocessing and Postprocessing in Linear Optimization" 2.8
     */
    private void scaling() {
        if (presolvedA != null) {
            final MatrixRescaler rescaler = new Matrix1NornRescaler();
            final RealVector[] uV = rescaler.getMatrixScalingFactors(presolvedA);
            this.r = uV[0];
            this.t = uV[1];
            // to check rescaling:
            // rescaler.checkScaling(presolvedA, R, T);

            // scaling A -> R.A.T
            presolvedA = AlgebraUtils.diagonalMatrixMult(r, presolvedA, t);
            for (int i = 0; i < r.getDimension(); i++) {
                final double ri = r.getEntry(i);
                presolvedB.setEntry(i, presolvedB.getEntry(i) * ri);
            }
            // Assign min and max rescaled bounds
            this.minRescaledLB = Double.MAX_VALUE;
            this.maxRescaledUB = -Double.MAX_VALUE;
            for (int i = 0; i < t.getDimension(); i++) {
                final double ti = t.getEntry(i);

                presolvedC.setEntry(i, presolvedC.getEntry(i) * ti); // c -> T.c

                final double lbi = presolvedLB.getEntry(i) / ti;
                presolvedLB.setEntry(i, lbi);
                this.minRescaledLB = MathLib.min(this.minRescaledLB, lbi);

                final double ubi = presolvedUB.getEntry(i) / ti;
                presolvedUB.setEntry(i, ubi);
                this.maxRescaledUB = Utils.max(this.maxRescaledUB, ubi);
            }
        }
    }

    /**
     * Remove all empty rows and columns
     * 
     * @param c
     *            original C vector
     * @param a
     *            original A matrix
     * @param b
     *            original B vector
     * @param lb
     *            original lower bounds vector
     * @param ub
     *            original upper bounds vector
     * @param ylb
     *            original Y lower bounds vector
     * @param yub
     *            original Y upper bounds vector
     * @param zlb
     *            original Y lower bounds vector
     * @param zub
     *            original Y upper bounds vector
     */
    private void removeAllEmptyRowsAndColumns(final RealVector c, final RealMatrix a, final RealVector b,
        final RealVector lb, final RealVector ub, final RealVector ylb, final RealVector yub, final RealVector zlb,
        final RealVector zub) {

        // intentionally left empty
    }

    /**
     * Prune fixed variable
     * 
     * @param x
     *            variable
     * @param c
     *            original C vector
     * @param a
     *            original A matrix
     * @param b
     *            original B vector
     * @param lb
     *            original lower bounds vector
     * @param ub
     *            original upper bounds vector
     * @param ylb
     *            original Y lower bounds vector
     * @param yub
     *            original Y upper bounds vector
     * @param zlb
     *            original Y lower bounds vector
     * @param zub
     *            original Y upper bounds vector
     */
    private void pruneFixedVariable(final int x, final RealVector c, final RealMatrix a, final RealVector b,
        final RealVector lb, final RealVector ub, final RealVector ylb, final RealVector yub, final RealVector zlb,
        final RealVector zub) {
        final double v = lb.getEntry(x);
        for (int i = 0; i < this.vRowPositions.length; i++) {
            if (ArrayUtils.contains(vRowPositions[i], x)) {
                // remove elements
                vRowPositions[i] = removeElementFromSortedArray(this.vRowPositions[i], x);
                changeRowsLengthPosition(i, vRowPositions[i].length + 1, vRowPositions[i].length);
                if (vRowPositions[i] == null || vRowPositions[i].length == 0) {
                    // this row contains only x
                    if (!isZero(v - b.getEntry(i) / a.getEntry(i, x))) {
                        // infeasible problem
                        throw new PatriusRuntimeException(PatriusMessages.INFEASIBLE_PROBLEM, null);
                    }
                    a.setEntry(i, x, 0.);
                    b.setEntry(i, 0);
                } else {
                    b.setEntry(i, b.getEntry(i) - a.getEntry(i, x) * v); // b*a*v
                    a.setEntry(i, x, 0.);
                }
            }
        }
        changeColumnsLengthPosition(x, vColPositions[x].length, 0);
        vColPositions[x] = new int[]{};
        this.someReductionDone = true;
    }

    /**
     * Removes the first occurrence of the specified element from the specified array. All subsequent elements are
     * shifted to the left.
     * 
     * @param array
     *            specified array
     * @param element
     *            element to remove
     * @return array with the element removed
     */
    private static int[] removeElementFromSortedArray(final int[] array, final int element) {
        if (array.length < 2) {
            return new int[]{};
        }
        return ArrayUtils.removeElements(array, element);
    }

    /**
     * Add an element into a sorted array
     * 
     * @param array
     *            specified array
     * @param element
     *            element to add
     * @return array with the element added
     */
    private static int[] addToSortedArray(final int[] array, final int element) {
        final int[] ret = new int[array.length + 1];
        int cnt = 0;
        boolean goStraight = false;
        for (int i = 0; i < array.length; i++) {
            final int s = array[i];
            if (goStraight) {
                ret[cnt] = s;
                cnt++;
            } else {
                if (s < element) {
                    // element bigger than value in array, so continue in the loop
                    ret[cnt] = s;
                    cnt++;
                    continue;
                }
                if (s == element) {
                    return array; // the element is already contained, so return the original array
                }
                if (s > element) {
                    // element smaller than value in array, so it is directly added
                    ret[cnt] = element;
                    cnt++;
                    ret[cnt] = s;
                    cnt++;
                    goStraight = true; // set true, element already added, now fill with original values
                }
            }
        }
        if (cnt < ret.length) {
            // to be added at the last position
            ret[cnt] = element;
        }
        return ret; // return sorted array with new element
    }

    /**
     * Is the susbet following a sparsity pattern?
     * 
     * @param subset
     *            subset
     * @param superset
     *            superset
     * @return true/false
     */
    private static boolean isSubsetSparsityPattern(final int[] subset, final int[] superset) {
        int position = 0;
        for (int i = 0; i < subset.length; i++) {
            final int s = subset[i];
            boolean found = false;
            for (int j = position; j < superset.length; j++) {
                if (superset[j] == s) { // superset[j] = subset[i]
                    found = true;
                    position = j;
                    break; // break, value already found
                }
            }
            if (!found) {
                return false; // any element matched, so return false
            }
        }
        return true; // return true, element found
    }

    /**
     * Are both arrays following the same sparsity pattern?
     * 
     * @param sp1
     *            array 1
     * @param sp2
     *            array 2
     * @return true/false
     */
    private static boolean isSameSparsityPattern(final int[] sp1, final int[] sp2) {
        if (sp1.length == sp2.length) {
            for (int k = 0; k < sp1.length; k++) {
                if (sp1[k] != sp2[k]) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Is the lower bound unbounded?
     * 
     * @param lb
     *            lower bound
     * @return true/false
     */
    public boolean isLBUnbounded(final double lb) {
        return Double.compare(unboundedLBValue, lb) == 0;
    }

    /**
     * Is the upper bound unbounded?
     * 
     * @param ub
     *            upper bound
     * @return true/false
     */
    public boolean isUBUnbounded(final double ub) {
        return Double.compare(unboundedUBValue, ub) == 0;
    }

    /**
     * Get the original number of variables
     * 
     * @return original number of variables
     */
    public int getOriginalN() {
        return this.originalN;
    }

    /**
     * Get the original number of variables
     * 
     * @return original number of variables
     */
    public int getOriginalMeq() {
        return this.originalMeq;
    }

    /**
     * Get the presolved number of variables
     * 
     * @return presolved number of variables
     */
    public int getPresolvedN() {
        return this.presolvedN;
    }

    /**
     * Get the presolved number of variables
     * 
     * @return presolved number of variables
     */
    public int getPresolvedMeq() {
        return this.presolvedMeq;
    }

    /**
     * Get the presolved vector C
     * 
     * @return presolved vector C
     */
    public RealVector getPresolvedC() {
        return this.presolvedC;
    }

    /**
     * Get the presolved matrix A
     * 
     * @return presolved matrix A
     */
    public RealMatrix getPresolvedA() {
        return this.presolvedA;
    }

    /**
     * Get the presolved vector B
     * 
     * @return presolved vector B
     */
    public RealVector getPresolvedB() {
        return this.presolvedB;
    }

    /**
     * Get the presolved lower bounds vector
     * 
     * @return presolved lower bounds vector
     */
    public RealVector getPresolvedLB() {
        return this.presolvedLB;
    }

    /**
     * Get the presolved upper bounds vector
     * 
     * @return presolved ipper bounds vector
     */
    public RealVector getPresolvedUB() {
        return this.presolvedUB;
    }

    /**
     * Get the presolved Y lower bounds vector
     * 
     * @return presolved Y lower bounds vector
     */
    public RealVector getPresolvedYlb() {
        return this.presolvedYlb;
    }

    /**
     * Get the presolved Y upper bounds vector
     * 
     * @return presolved Y uppwer bounds vector
     */
    public RealVector getPresolvedYub() {
        return this.presolvedYub;
    }

    /**
     * Get the presolved Z lower bounds vector
     * 
     * @return presolved Z lower bounds vector
     */
    public RealVector getPresolvedZlb() {
        return this.presolvedZlb;
    }

    /**
     * Get the presolved Z upper bounds vector
     * 
     * @return presolved Z uppwer bounds vector
     */
    public RealVector getPresolvedZub() {
        return this.presolvedZub;
    }

    /**
     * Return true if the element is smaller than epsilon
     * 
     * @param d
     *            value to compare
     * @return true/false
     */
    private boolean isZero(final double d) {
        if (Double.isNaN(d)) {
            return false;
        }
        return MathLib.abs(d) < eps;
    }

    /**
     * Set the number of slack variables
     * 
     * @param nOfSlackVar
     *            number
     */
    public void setNOfSlackVariables(final int nOfSlackVar) {
        this.nOfSlackVariables = nOfSlackVar;
    }

    /**
     * Add linear dependency to presolving stack
     * 
     * @param linearDependency
     *            to add
     */
    private void addToPresolvingStack(final LinearDependency linearDependency) {
        this.indipendentVariables[linearDependency.x] = false;
        presolvingStack.add(presolvingStack.size(), linearDependency);
    }

    /**
     * Add duplicated column to presolving stack
     * 
     * @param duplicatedColumn
     *            to add
     */
    private void addToPresolvingStack(final DuplicatedColumn duplicatedColumn) {
        this.indipendentVariables[duplicatedColumn.xj] = false;
        presolvingStack.add(presolvingStack.size(), duplicatedColumn);
    }

    /**
     * This method is just for testing scope.
     * 
     * @param c
     *            original C vector
     * @param a
     *            original A matrix
     * @param b
     *            original B vector
     * @param lb
     *            original lower bounds vector
     * @param ub
     *            original upper bounds vector
     * @param ylb
     *            original Y lower bounds vector
     * @param yub
     *            original Y upper bounds vector
     * @param zlb
     *            original Y lower bounds vector
     * @param zub
     *            original Y upper bounds vector
     */
    private void checkProgress(final RealVector c, final RealMatrix a, final RealVector b, final RealVector lb,
        final RealVector ub, final RealVector ylb, final RealVector yub, final RealVector zlb, final RealVector zub) {

        if (this.expectedSolution == null) {
            return;
        }

        if (Double.isNaN(this.expectedTolerance)) {
            // for this to work properly, this method must be called at least one time before
            // presolving operations
            // start
            final RealVector x = MatrixUtils.createRealVector(expectedSolution);
            final RealMatrix aMatrix = MatrixUtils.createRealMatrix(a.getData());
            final RealVector bVector = MatrixUtils.createRealVector(b.toArray());
            final RealVector aXb = aMatrix.operate(x).subtract(bVector);
            final double norm = MathLib.pow(aXb.getNorm(), 2);
            this.expectedTolerance = MathLib.max(SCALAR1, SCALAR2 * norm);
        }

        // A.x = b
        final double tolerance = this.expectedTolerance;
        final RealVector x = MatrixUtils.createRealVector(expectedSolution);
        final RealMatrix aMatrix = MatrixUtils.createRealMatrix(a.getData());
        final RealVector bvector = MatrixUtils.createRealVector(b.toArray());
        final RealVector aXb = aMatrix.operate(x).subtract(bvector);
        final double norm = MathLib.pow(aXb.getNorm(), 2);
        if (norm > tolerance) {
            // where is the error?
            for (int i = 0; i < aXb.getDimension(); i++) {
                if (MathLib.abs(aXb.getEntry(i)) > tolerance) {
                    throw new IllegalStateException();
                }
            }
            throw new IllegalStateException();
        }

        // upper e lower
        for (int i = 0; i < x.getDimension(); i++) {
            if (x.getEntry(i) + tolerance < lb.getEntry(i)) {
                throw new IllegalStateException();
            }
            if (x.getEntry(i) > ub.getEntry(i) + tolerance) {
                throw new IllegalStateException();
            }
        }
    }

    /**
     * Change rows length position.
     * 
     * @param rowIndex
     *            index of the row to change
     * @param lengthIndexFrom
     *            start index
     * @param lengthIndexTo
     *            stop index
     */
    private void changeRowsLengthPosition(final int rowIndex, final int lengthIndexFrom, final int lengthIndexTo) {
        if (lengthIndexFrom == 0) {
            return;
        }
        if (vRowLengthMap[lengthIndexTo] == null) {
            vRowLengthMap[lengthIndexTo] = new int[]{};
        }
        vRowLengthMap[lengthIndexTo] = addToSortedArray(vRowLengthMap[lengthIndexTo], rowIndex);
        vRowLengthMap[lengthIndexFrom] = removeElementFromSortedArray(vRowLengthMap[lengthIndexFrom], rowIndex);
    }

    /**
     * Change columns length position.
     * 
     * @param colIndex
     *            index of the column to change
     * @param lengthIndexFrom
     *            start index
     * @param lengthIndexTo
     *            stop index
     */
    private void changeColumnsLengthPosition(final int colIndex, final int lengthIndexFrom, final int lengthIndexTo) {
        if (lengthIndexFrom == 0) {
            return;
        }
        if (vColLengthMap[lengthIndexTo] == null) {
            vColLengthMap[lengthIndexTo] = new int[]{};
        }
        vColLengthMap[lengthIndexTo] = addToSortedArray(vColLengthMap[lengthIndexTo], colIndex);
        vColLengthMap[lengthIndexFrom] = removeElementFromSortedArray(vColLengthMap[lengthIndexFrom], colIndex);
    }

    /**
     * Just for testing porpose
     * 
     * @param sol
     *            solution
     */
    public void setExpectedSolution(final double[] sol) {
        this.expectedSolution = Arrays.copyOf(sol, sol.length);
    }

    /**
     * Get the minimum rescaled lower bound
     * 
     * @return minimum rescaled lower bound
     */
    public double getMinRescaledLB() {
        return minRescaledLB;
    }

    /**
     * Get the maximum rescaled lower bound
     * 
     * @return maximum rescaled lower bound
     */
    public double getMaxRescaledUB() {
        return maxRescaledUB;
    }

    /**
     * Set the value for zero-comparison: <br>
     * if |a - b| < eps then a - b = 0. <br>
     * Default is the <i>double epsilon machine<i> value.
     * 
     * @param epsilon
     *            value
     */
    public void setZeroTolerance(final double epsilon) {
        this.eps = epsilon;
    }

    /**
     * Abstract class Presolving stack element
     */
    private abstract class AbstractPresolvingStackElement {
        /**
         * Post solver
         * 
         * @param x
         *            values
         */
        protected abstract void postSolve(double[] x);

        /**
         * Pre solver
         * 
         * @param x
         *            values
         */
        protected abstract void preSolve(double[] x);
    }

    /**
     * x = q + Sum_i[mi * xi]
     */
    private class LinearDependency extends AbstractPresolvingStackElement {
        /** X value */
        private final int x;
        /** X value at position i */
        private final int[] xi;
        /** M value at position i */
        private final double[] mi;
        /** Q value */
        private final double q;

        /**
         * Constructor
         * 
         * @param xValue
         *            value
         * @param xiValue
         *            array
         * @param miValue
         *            array
         * @param qValue
         *            value
         */
        LinearDependency(final int xValue, final int[] xiValue, final double[] miValue, final double qValue) {
            super();
            this.x = xValue;
            this.xi = xiValue;
            this.mi = miValue;
            this.q = qValue;
        }

        /**
         * Post solver
         */
        @Override
        protected void postSolve(final double[] postsolvedX) {
            for (int k = 0; this.xi != null && k < this.xi.length; k++) {
                postsolvedX[this.x] += this.mi[k] * postsolvedX[this.xi[k]];
            }
            postsolvedX[this.x] += this.q;
        }

        /**
         * Pre solver
         */
        @Override
        protected void preSolve(final double[] v) {
            // Intentionally left empty
        }

        /**
         * Converts into string
         */
        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer();
            sb.append(BRACKET6 + x + BRACKET3);
            for (int i = 0; xi != null && i < xi.length; i++) {
                sb.append(PLUS + mi[i] + BRACKET4 + xi[i] + BRACKET1);
            }
            sb.append(PLUS + q);
            return sb.toString();
        }
    }

    /**
     * The presolving stack element relative to the substitution: xk = -v*xj + xkPrime
     */
    private class DuplicatedColumn extends AbstractPresolvingStackElement {
        /** Element xj */
        private final int xj;
        /** Element xk */
        private final int xk;
        /** Element xkPrime */
        private final int xkPrime;
        /** Element v */
        private final double v;
        /** The lower bound of the presolved variables xj */
        private double lbj;
        /** the upper bound of the presolved variables xj */
        private double ubj;
        /** the lower bound of the variables xk */
        private double lbk;
        /** the upper bound of the variables xk */
        private double ubk;

        /**
         * Duplicated column
         * 
         * @param xjValue
         *            value
         * @param xkValue
         *            value
         * @param xkPrimeValue
         *            value
         * @param vValue
         *            value
         * @param lbjValue
         *            value
         * @param ubjValue
         *            value
         * @param lbkValue
         *            value
         * @param ubkValue
         *            value
         */
        DuplicatedColumn(final int xjValue, final int xkValue, final int xkPrimeValue, final double vValue,
                final double lbjValue, final double ubjValue, final double lbkValue, final double ubkValue) {
            super();
            this.xj = xjValue;
            this.xk = xkValue;
            this.xkPrime = xkPrimeValue;
            this.v = vValue;
            this.lbj = lbjValue;
            this.ubj = ubjValue;
            this.lbk = lbkValue;
            this.ubk = ubkValue;
        }

        /**
         * Post solver
         */
        @Override
        protected void postSolve(final double[] postsolvedX) {
            // getting back the original variables, the original bounds must be respected
            // NB: remember that xj is a dependent variables (taken out from the problem by the
            // presolver)

            if (isLBUnbounded(this.lbk)) {
                this.lbk = -Double.MAX_VALUE;
            }
            if (isLBUnbounded(this.lbj)) {
                this.lbj = -Double.MAX_VALUE;
            }
            if (isLBUnbounded(this.ubk)) {
                this.ubk = Double.MAX_VALUE;
            }
            if (isUBUnbounded(this.ubj)) {
                this.ubj = Double.MAX_VALUE;
            }

            if (v > 0) {
                // we must have:
                // lbk < xk < ubk
                // but
                // xk = xkPrime -v*xj
                // and so (-v>0):
                // xkPrime-v*ubj < xk = xkPrime -v*xj < xkPrime-v*lbj
                // then:
                // MathLib.max(lbk, xkPrime-v*ubj) < xk < MathLib.min(ubk, xkPrime-v*lbj);
                final double p = postsolvedX[xkPrime];
                postsolvedX[xk] = MathLib.max(lbk, p - v * ubj);
                postsolvedX[xj] = (p - postsolvedX[xk]) / v;
            } else if (v < 0) {
                // we must have:
                // lbk < xk < ubk
                // but
                // xk = xkPrime -v*xj
                // and so (-v<0):
                // xkPrime-v*lbj < xk = xkPrime -v*xj < xkPrime-v*ubj
                // then:
                // MathLib.max(lbk, xkPrime-v*lbj) < xk < MathLib.min(ubk, xkPrime-v*ubj);
                final double p = postsolvedX[xkPrime];
                postsolvedX[xk] = MathLib.max(lbk, p - v * lbj);
                postsolvedX[xj] = (p - postsolvedX[xk]) / v;
            } else {
                throw new IllegalStateException("coefficient v must be >0 or <0");
            }
        }

        /**
         * Pre solver
         */
        @Override
        protected void preSolve(final double[] x) {
            // es x[2]=-1.0*x[0] + xPrime[2]
            x[this.xkPrime] = x[this.xk] + this.v * x[this.xj];
        }

        /**
         * Converts into string
         */
        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer();
            sb.append(BRACKET6 + xk + "]=-" + v + BRACKET4 + xj + "] + xPrime[" + xkPrime + BRACKET1);
            return sb.toString();
        }
    }
}

//CHECKSTYLE: resume MethodLength check
//CHECKSTYLE: resume NestedBlockDepth check
//CHECKSTYLE: resume CyclomaticComplexity check
//CHECKSTYLE: resume ModifiedControlVariable check
