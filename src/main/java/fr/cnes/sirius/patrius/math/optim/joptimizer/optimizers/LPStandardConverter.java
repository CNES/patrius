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
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.6:DM:DM-2591:27/01/2021:[PATRIUS] Intigration et validation JOptimizer
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.optim.joptimizer.optimizers;

import java.util.ArrayList;
import java.util.List;

import fr.cnes.sirius.patrius.math.linear.ArrayRealVector;
import fr.cnes.sirius.patrius.math.linear.BlockRealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealVector;
import fr.cnes.sirius.patrius.math.optim.joptimizer.algebra.AlgebraUtils;

/**
 * Converts a general LP problem stated in the form (1): <br>
 * min(c) s.t. <br>
 * G.x < h <br>
 * A.x = b <br>
 * lb <= x <= ub <br>
 * <br>
 * to the (strictly)standard form (2) <br>
 * min(c) s.t. <br>
 * A.x = b <br>
 * x >= 0 <br>
 * <br>
 * or to the (quasi)standard form (3) <br>
 * min(c) s.t. <br>
 * A.x = b <br>
 * lb <= x <= ub <br>
 * <br>
 * Setting the field <i>strictlyStandardForm</i> to true, the conversion is in the (strictly)
 * standard form (2).
 * 
 * <br>
 * Note 1: (3) it is not exactly the standard LP form (2) because of the more general lower and
 * upper bounds terms. <br>
 * Note 2: if the vector lb is not passed in, all the lower bounds are assumed to be equal to the
 * value of the field
 * <i>unboundedLBValue</i> <br>
 * Note 3: if the vector ub is not passed in, all the upper bounds are assumed to be equal to the
 * value of the field
 * <i>unboundedUBValue</i> <br>
 * Note 4: unboundedLBValue is the distinctive value of an unbounded lower bound. It must be one of
 * the values:
 * <ol>
 * <li>Double.NaN (the default)</li>
 * <li>Double.NEGATIVE_INFINITY</li>
 * </ol>
 * <br>
 * Note 5: unboundedUBValue is the distinctive value of an unbounded upper bound. It must be one of
 * the values:
 * <ol>
 * <li>Double.NaN (the default)</li>
 * <li>Double.POSITIVE_INFINITY</li>
 * </ol>
 * 
 * @see Converting LPs to standard form, "Convex Optimization", p 147
 * @author alberto trivellato (alberto.trivellato@gmail.com)
 * @since 4.6
 */
public class LPStandardConverter {

    /** Default unbounded lower bound */
    public static final double DEFAULT_UNBOUNDED_LOWER_BOUND = Double.NaN;// NaN because in the
                                                                          // tests files the
                                                                          // unbounded lb are
    /** Default unbounded upper bound */
    public static final double DEFAULT_UNBOUNDED_UPPER_BOUND = Double.NaN;// NaN because in the
                                                                          // tests files the
                                                                          // unbounded ub are
                                                                          // usually with this value
    /** Message */
    private static final String WRONGDIM = "wrong array dimension: ";
    /** original number of variables */
    private int originalN;
    /** final number of variables */
    private int standardN;
    /** final number of slack variables for inequalities constraints */
    private int standardS;
    /** final objective function */
    private RealVector standardC;
    /** final equalities constraints coefficients */
    private RealMatrix standardA;
    /** final equalities constraints limits */
    private RealVector standardB;
    /** final lower bounds */
    private RealVector standardLB;
    /** final upper bounds */
    private RealVector standardUB;
    /** original variables to split for having positive final variables */
    private final List<Integer> splittedVariablesList = new ArrayList<Integer>();
    /** Unbounded lower bound value */
    private double unboundedLBValue = DEFAULT_UNBOUNDED_LOWER_BOUND;
    /** Unbounded upper bound value */
    private double unboundedUBValue = DEFAULT_UNBOUNDED_UPPER_BOUND;
    /**
     * if true, convert the problem to the strictly standard form:
     * min(c) s.t.
     * A.x = b
     * x >=0
     * 
     * otherwise, convert the problem to the more general (quasi) standard form:
     * min(c) s.t.
     * A.x = b
     * lb <= x <= ub
     */
    private boolean strictlyStandardForm = false;

    /**
     * Constructor, set the strictlyStandardForm to false
     */
    public LPStandardConverter() {
        this(false);
    }

    /**
     * Constructor, set the default unbounded lower and upper bounds
     * 
     * @param strictlySF true/false
     */
    public LPStandardConverter(final boolean strictlySF) {
        this(strictlySF, DEFAULT_UNBOUNDED_LOWER_BOUND, DEFAULT_UNBOUNDED_UPPER_BOUND);
    }

    /**
     * Constructor
     * 
     * @param unboundedLBVal unbounded lower bound value
     * @param unboundedUBVal unbounded upper bound value
     */
    public LPStandardConverter(final double unboundedLBVal,
            final double unboundedUBVal) {
        this(false, unboundedLBVal, unboundedUBVal);
    }

    /**
     * Constructor
     * 
     * @param strictlySF true/false
     * @param unboundedLBVal unbounded lower bound value
     * @param unboundedUBVal unbounded upper bound value
     */
    public LPStandardConverter(final boolean strictlySF,
            final double unboundedLBVal,
            final double unboundedUBVal) {
        if (!Double.isNaN(unboundedLBVal) && !Double.isInfinite(unboundedLBVal)) {
            throw new IllegalArgumentException(
                    "The field unboundedLBValue must be set to Double.NaN or Double.NEGATIVE_INFINITY");
        }
        if (!Double.isNaN(unboundedUBVal) && !Double.isInfinite(unboundedUBVal)) {
            throw new IllegalArgumentException(
                    "The field unboundedUBValue must be set to Double.NaN or Double.POSITIVE_INFINITY");
        }
        this.strictlyStandardForm = strictlySF;
        this.unboundedLBValue = unboundedLBVal;
        this.unboundedUBValue = unboundedUBVal;
    }

    /**
     * Transforms the problem from a general form to the (quasi) standard LP form.
     * 
     * @param originalC original C array
     * @param originalG original G matrix
     * @param originalH original H array
     * @param originalA original A matrix
     * @param originalB original B array
     * @param originalLB if null, all lower bounds default to this.unspecifiedLBValue
     * @param originalUB if null, all upper bounds default to this.unspecifiedUBValue
     * @see Converting LPs to standard form, "Convex Optimization", p 147
     */
    public void toStandardForm(final double[] originalC,
            final double[][] originalG,
            final double[] originalH,
            final double[][] originalA,
            final double[] originalB,
            final double[] originalLB,
            final double[] originalUB) {

        RealMatrix gMatrix = null;
        RealVector hVector = null;
        if (originalG != null) {
            // GMatrix = (useSparsity)? new SparseDoubleMatrix2D(G) : new BlockRealMatrix(G);
            gMatrix = new BlockRealMatrix(originalG);
            hVector = new ArrayRealVector(originalH);
        }
        RealMatrix aMatrix = null;
        RealVector bVector = null;
        if (originalA != null) {
            // AMatrix = (useSparsity)? new SparseDoubleMatrix2D(A) : new BlockRealMatrix(A);
            aMatrix = new BlockRealMatrix(originalA);
            bVector = new ArrayRealVector(originalB);
        }
        if (originalLB != null && originalUB != null) {
            if (originalLB.length != originalUB.length) {
                // dimension mismatch
                throw new IllegalArgumentException("lower and upper bounds have different lenght");
            }
        }
        final RealVector lbVector;
        if (originalLB != null) {
            lbVector = new ArrayRealVector(originalLB);
        } else {
            lbVector = null; // null if the original lower bounds are null
        }
        final RealVector ubVector;
        if (originalUB != null) {
            ubVector = new ArrayRealVector(originalUB);
        } else {
            ubVector = null; // null if the original upper bounds are null
        }
        // transforms to standard form
        toStandardForm(new ArrayRealVector(originalC), gMatrix, hVector, aMatrix, bVector,
                lbVector, ubVector);
    }

    /**
     * Transforms the problem from a general form to the (quasi) standard LP form.
     * 
     * @param originalC original C array
     * @param originalG original G matrix
     * @param originalH original H array
     * @param originalA original A matrix
     * @param originalB original B array
     * @param originalLB if null, all lower bounds default to this.unspecifiedLBValue
     * @param originalUB if null, all upper bounds default to this.unspecifiedUBValue
     * @see Converting LPs to standard form, "Convex Optimization", p 147
     */
    //CHECKSTYLE: stop CyclomaticComplexity check
    //CHECKSTYLE: stop MethodLength check
    // Reason: complex JOptimizer code kept as such
    @SuppressWarnings("PMD.NullAssignment")
    public void toStandardForm(final RealVector originalC,
            final RealMatrix originalG,
            final RealVector originalH,
            final RealMatrix originalA,
            final RealVector originalB,
            final RealVector originalLB,
            final RealVector originalUB) {
        //CHECKSTYLE: resume CyclomaticComplexity check
        //CHECKSTYLE: resume MethodLength check

        this.originalN = originalC.getDimension();
        if (originalLB != null && originalUB != null) {
            if (originalLB.getDimension() != originalUB.getDimension()) {
                throw new IllegalArgumentException("lower and upper bounds have different size");
            }
        }
        RealVector originalLBCopy = null;
        RealVector originalUBCopy = null;
        if (originalLB == null) {
            // there are no lb, that is they are all unbounded
            originalLBCopy = new ArrayRealVector(originalN, unboundedLBValue);
        } else {
            originalLBCopy = originalLB.copy();
        }
        if (originalUB == null) {
            // there are no ub, that is they are all unbounded
            originalUBCopy = new ArrayRealVector(originalN, unboundedUBValue);
        } else {
            originalUBCopy = originalUB.copy();
        }

        if (originalG == null && !strictlyStandardForm) {
            // nothing to convert
            this.standardN = originalN;
            this.standardA = originalA;
            this.standardB = originalB;
            this.standardC = originalC;
            this.standardLB = originalLBCopy;
            this.standardUB = originalUBCopy;
            return;
        }

        // definition of the elements
        final int nOfSlackG; // number of slack
        if (originalG != null) {
            nOfSlackG = originalG.getRowDimension();
        } else {
            nOfSlackG = 0;
        }
        // variables given by
        // G
        int nOfSlackUB = 0;// number of slack variables given by the upper bounds
        int nOfSplittedVariables = 0;// number of variables to split, x = xPlus-xMinus with xPlus
                                     // and xMinus positive
        int nOfSlackLB = 0;// number of slack variables given by the lower bounds
        /** lower bound slack */
        final boolean[] lbSlack = new boolean[originalN];
        /** upper bound slack */
        final boolean[] ubSlack = new boolean[originalN];

        if (strictlyStandardForm) {
            // record the variables that need the split x = xPlus-xMinus
            for (int i = 0; i < originalN; i++) {
                final double lbi = originalLBCopy.getEntry(i);
                final double ubi = originalUBCopy.getEntry(i);
                if (isLbUnbounded(lbi)) {
                    // no slack (no row in the final A) but split the variable
                    // we have lb[i] = -oo, so must split this variable (because it is not forced to
                    // be non negative)
                    splittedVariablesList.add(splittedVariablesList.size(), i);
                } else {
                    final int lbCompare = Double.compare(lbi, 0.);
                    if (lbCompare < 0) {
                        // this inequality must become an equality
                        nOfSlackLB++;
                        lbSlack[i] = true;
                        // we have lb[i] < 0, so must split this variable (because it is not forced
                        // to be non negative)
                        splittedVariablesList.add(splittedVariablesList.size(), i);
                    } else if (lbCompare > 0) {
                        // we have lb[i] > 0, and this must become a row in the standardA matrix
                        // (standard lb limits are
                        // all = 0)
                        nOfSlackLB++;
                        lbSlack[i] = true;
                    }
                }
                if (!isUbUnbounded(ubi)) {
                    // this lb must become a row in the standardA matrix (there are no standard ub
                    // limits)
                    nOfSlackUB++;
                    ubSlack[i] = true;
                }
            }
            nOfSplittedVariables = splittedVariablesList.size();
        }

        // The first step: introduce slack variables s[i] for all the inequalities
        this.standardS = nOfSlackG + nOfSlackUB + nOfSlackLB;
        this.standardN = standardS + originalN + nOfSplittedVariables;
        // we must build a final A matrix that is different from the original

        if (originalA != null) {
            standardA = new BlockRealMatrix(standardS + originalA.getRowDimension(), standardN);
            standardB = new ArrayRealVector(standardS + originalB.getDimension());
        } else {
            standardA = new BlockRealMatrix(standardS, standardN);
            standardB = new ArrayRealVector(standardS);
        }

        // filling with original G values
        for (int i = 0; i < nOfSlackG; i++) {
            standardA.setEntry(i, i, 1);// slack variable position
            standardB.setEntry(i, originalH.getEntry(i));
        }
        for (int i = 0; i < nOfSlackG; i++) {
            for (int j = 0; j < originalN; j++) {
                standardA.setEntry(i, standardS + j, originalG.getEntry(i, j));
            }
        }

        // filling for the lower and upper bounds
        int cntSlackLB = 0;
        int cntSlackUB = 0;
        for (int i = 0; i < originalN; i++) {
            if (lbSlack[i]) {
                standardA.setEntry(nOfSlackG + cntSlackLB, nOfSlackG + cntSlackLB, 1);// slack
                                                                                      // variable
                                                                                      // position
                standardA.setEntry(nOfSlackG + cntSlackLB, standardS + i, -1);
                standardB.setEntry(nOfSlackG + cntSlackLB, -originalLBCopy.getEntry(i));
                cntSlackLB++;
            }
            if (ubSlack[i]) {
                standardA.setEntry(nOfSlackG + nOfSlackLB + cntSlackUB, nOfSlackG + nOfSlackLB
                        + cntSlackUB, 1);// slack
                // variable
                // position
                standardA.setEntry(nOfSlackG + nOfSlackLB + cntSlackUB, standardS + i, 1);
                standardB.setEntry(nOfSlackG + nOfSlackLB + cntSlackUB, originalUBCopy.getEntry(i));
                cntSlackUB++;
            }
        }

        // filling with original A values
        for (int i = 0; originalA != null && i < originalA.getRowDimension(); i++) {
            for (int j = 0; j < originalN; j++) {
                standardA.setEntry(standardS + i, standardS + j, originalA.getEntry(i, j));
            }
            standardB.setEntry(standardS + i, originalB.getEntry(i));
        }

        // filling for splitted variables
        cntSlackLB = 0;
        cntSlackUB = 0;
        int previousSplittedVariables = 0;
        for (int sv = 0; sv < nOfSplittedVariables; sv++) {
            final int i = splittedVariablesList.get(sv);
            // variable i was splitted
            for (int r = 0; r < nOfSlackG; r++) {// n of rows of G
                standardA.setEntry(r, standardS + originalN + sv, -originalG.getEntry(r, i));
            }

            if (lbSlack[i]) {
                for (int k = previousSplittedVariables; k < i; k++) {
                    if (lbSlack[k]) {
                        // for this lb we have a row (and a slack variable) in the standard A
                        cntSlackLB++;
                    }
                }
                standardA.setEntry(nOfSlackG + cntSlackLB, standardS + originalN + sv, 1);// lower
                                                                                          // bound
            }

            if (ubSlack[i]) {
                for (int k = previousSplittedVariables; k < i; k++) {
                    if (ubSlack[k]) {
                        // for this ub we have a row (and a slack variable) in the standard A
                        cntSlackUB++;
                    }
                }
                standardA.setEntry(nOfSlackG + nOfSlackLB + cntSlackUB, standardS + originalN + sv,
                        -1);// upper
                            // bound
            }

            previousSplittedVariables = i;

            for (int r = 0; originalA != null && r < originalA.getRowDimension(); r++) {
                standardA.setEntry(standardS + r, standardS + originalN + sv,
                        -originalA.getEntry(r, i));
            }
        }

        standardC = new ArrayRealVector(standardN);
        standardLB = new ArrayRealVector(standardN);
        standardUB = new ArrayRealVector(standardN, unboundedUBValue);// the slacks are upper
                                                                      // unbounded
        for (int i = 0; i < originalN; i++) {
            standardC.setEntry(standardS + i, originalC.getEntry(i));
        }
        for (int i = 0; i < standardS; i++) {
            standardLB.setEntry(i, 0.);
        }
        for (int i = 0; i < originalN; i++) {
            standardLB.setEntry(standardS + i, originalLBCopy.getEntry(i));
        }
        for (int i = 0; i < originalN; i++) {
            standardUB.setEntry(standardS + i, originalUBCopy.getEntry(i));
        }

        if (strictlyStandardForm) {
            standardLB = new ArrayRealVector(standardN, 0.);// brand new lb
            standardUB = null;// no ub for the strictly standard form
        }
    }

    /**
     * Get back the vector in the original components.
     * @param x vector in the standard variables
     * @return the original component
     */
    public double[] postConvert(final double[] x) {
        if (x.length != standardN) {
            // dimension mismatch
            throw new IllegalArgumentException(WRONGDIM + x.length);
        }
        final double[] ret = new double[originalN];
        int cntSplitted = 0;
        for (int i = standardS; i < standardN; i++) {
            if (splittedVariablesList.contains(i - standardS)) {
                // this variable was split: x = xPlus-xMinus
                ret[i - standardS] = x[i] - x[standardN + cntSplitted];
                cntSplitted++;
            } else {
                ret[i - standardS] = x[i];
            }
        }
        // return the element post converted
        return ret;
    }

    /**
     * Express a vector in the original variables in the final standard variable form
     * @param x vector in the original variables
     * @return vector in the standard variables
     */
    public double[] getStandardComponents(final double[] x) {
        if (x.length != originalN) {
            throw new IllegalArgumentException(WRONGDIM + x.length); // dimension mismatch
        }
        final double[] ret = new double[standardN];
        for (int i = 0; i < x.length; i++) {
            if (splittedVariablesList.contains(i)) {
                // this variable was splitted: x = xPlus-xMinus
                if (x[i] >= 0) {
                    // value for xPlus
                    ret[standardS + i] = x[i];
                } else {
                    int pos = -1;
                    for (int k = 0; k < splittedVariablesList.size(); k++) {
                        if (splittedVariablesList.get(k) == i) {
                            pos = k;
                            break; // break, position already found
                        }
                    }
                    // value for xMinus
                    ret[standardS + x.length + pos] = -x[i];
                }
            } else {
                ret[standardS + i] = x[i];
            }
        }
        if (standardS > 0) {
            final RealVector residuals = AlgebraUtils.zMult(standardA, new ArrayRealVector(ret),
                    standardB, -1);
            for (int i = 0; i < standardS; i++) {
                ret[i] = -residuals.getEntry(i) + ret[i]; // substract the residuals values
            }
        }
        return ret;
    }

    /**
     * Get the original number of variables
     * @return num variables
     */
    public int getOriginalN() {
        return originalN;
    }

    /**
     * Get the final number of variables
     * @return num variables
     */
    public int getStandardN() {
        return standardN;
    }

    /**
     * Get the final number of slack variables for inequalities constraints
     * @return num variables
     */
    public int getStandardS() {
        return standardS;
    }

    /**
     * Get the final objective function
     * @return objective function
     */
    public RealVector getStandardC() {
        return standardC;
    }

    /**
     * Get the final equalities constraints coefficients
     * @return coefficients
     */
    public RealMatrix getStandardA() {
        return standardA;
    }

    /**
     * Get the final equalities constraints limits
     * @return limits
     */
    public RealVector getStandardB() {
        return standardB;
    }

    /***
     * Get the final lower bounds
     * This makes sense only if strictlyStandardForm = false (otherwise all lb are 0).
     * 
     * @return lower bounds
     */
    public RealVector getStandardLB() {
        return standardLB;
    }

    /**
     * Get the final upper bounds
     * This makes sense only if strictlyStandardForm = false (otherwise all ub are unbounded).
     * 
     * @return upper bounds
     */
    public RealVector getStandardUB() {
        return standardUB;
    }

    /**
     * Is the lower bound unbounded?
     * 
     * @param lb lower bound
     * @return true/false
     */
    public boolean isLbUnbounded(final Double lb) {
        return Double.compare(unboundedLBValue, lb) == 0;
    }

    /**
     * Is the upper bound unbounded?
     * 
     * @param ub upper bound
     * @return true/false
     */
    public boolean isUbUnbounded(final Double ub) {
        return Double.compare(unboundedUBValue, ub) == 0;
    }

    /**
     * Get the unbounded lower bound value
     * 
     * @return value
     */
    public double getUnboundedLBValue() {
        return unboundedLBValue;
    }

    /**
     * Get the unbounded upper bound value
     * 
     * @return value
     */
    public double getUnboundedUBValue() {
        return unboundedUBValue;
    }
}
