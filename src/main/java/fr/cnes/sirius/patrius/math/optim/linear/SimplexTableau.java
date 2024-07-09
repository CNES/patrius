/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * HISTORY
* VERSION:4.8:DM:DM-2922:15/11/2021:[PATRIUS] suppression de l'utilisation de la reflexion Java dans patrius 
* VERSION:4.7:DM:DM-2766:18/05/2021:Evol. et corr. dans le package fr.cnes.sirius.patrius.math.linear (suite DM 2300) 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 *
 * Copyright 2011-2017 CNES
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
 */
package fr.cnes.sirius.patrius.math.optim.linear;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import fr.cnes.sirius.patrius.math.linear.Array2DRowRealMatrix;
import fr.cnes.sirius.patrius.math.linear.MatrixUtils;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealVector;
import fr.cnes.sirius.patrius.math.optim.PointValuePair;
import fr.cnes.sirius.patrius.math.optim.nonlinear.scalar.GoalType;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;

/**
 * A tableau for use in the Simplex method.
 * 
 * <p>
 * Example:
 * 
 * <pre>
 *   W |  Z |  x1 |  x2 |  x- | s1 |  s2 |  a1 |  RHS
 * ---------------------------------------------------
 *  -1    0    0     0     0     0     0     1     0   &lt;= phase 1 objective
 *   0    1   -15   -10    0     0     0     0     0   &lt;= phase 2 objective
 *   0    0    1     0     0     1     0     0     2   &lt;= constraint 1
 *   0    0    0     1     0     0     1     0     3   &lt;= constraint 2
 *   0    0    1     1     0     0     0     1     4   &lt;= constraint 3
 * </pre>
 * 
 * W: Phase 1 objective function</br> Z: Phase 2 objective function</br> x1 &amp; x2: Decision variables</br> x-: Extra
 * decision variable to allow for negative values</br> s1 &amp; s2: Slack/Surplus variables</br> a1: Artificial
 * variable</br> RHS: Right hand side</br>
 * </p>
 * 
 * @version $Id: SimplexTableau.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 2.0
 */
@SuppressWarnings({"PMD.NullAssignment", "PMD.ConstructorCallsOverridableMethod"})
class SimplexTableau implements Serializable {

    /** Column label for negative vars. */
    private static final String NEGATIVE_VAR_COLUMN_LABEL = "x-";

    /** X. */
    private static final String X = "x";

    /** Default amount of error to accept in floating point comparisons (as ulps). */
    private static final int DEFAULT_ULPS = 10;

    /** The cut-off threshold to zero-out entries. */
    private static final double CUTOFF_THRESHOLD = 1e-12;

    /** Serializable version identifier. */
    private static final long serialVersionUID = -1369660067587938365L;

    /** Linear objective function. */
    private final LinearObjectiveFunction f;

    /** Linear constraints. */
    private final List<LinearConstraint> constraints;

    /** Whether to restrict the variables to non-negative values. */
    private final boolean restrictToNonNegative;

    /** The variables each column represents */
    private final List<String> columnLabels = new ArrayList<String>();

    /** Simple tableau. */
    private transient RealMatrix tableau;

    /** Number of decision variables. */
    private final int numDecisionVariables;

    /** Number of slack variables. */
    private final int numSlackVariables;

    /** Number of artificial variables. */
    private int numArtificialVariables;

    /** Amount of error to accept when checking for optimality. */
    private final double epsilon;

    /** Amount of error to accept in floating point comparisons. */
    private final int maxUlps;

    /**
     * Builds a tableau for a linear problem.
     * 
     * @param fIn
     *        Linear objective function.
     * @param constraintsIn
     *        Linear constraints.
     * @param goalType
     *        Optimization goal: either {@link GoalType#MAXIMIZE} or {@link GoalType#MINIMIZE}.
     * @param restrictToNonNegativeIn
     *        Whether to restrict the variables to non-negative values.
     * @param epsilonIn
     *        Amount of error to accept when checking for optimality.
     */
    SimplexTableau(final LinearObjectiveFunction fIn,
                   final Collection<LinearConstraint> constraintsIn,
                   final GoalType goalType,
                   final boolean restrictToNonNegativeIn,
                   final double epsilonIn) {
        this(fIn, constraintsIn, goalType, restrictToNonNegativeIn, epsilonIn, DEFAULT_ULPS);
    }

    /**
     * Build a tableau for a linear problem.
     * 
     * @param fIn
     *        linear objective function
     * @param constraintsIn
     *        linear constraints
     * @param goalType
     *        type of optimization goal: either {@link GoalType#MAXIMIZE} or {@link GoalType#MINIMIZE}
     * @param restrictToNonNegativeIn
     *        whether to restrict the variables to non-negative values
     * @param epsilonIn
     *        amount of error to accept when checking for optimality
     * @param maxUlpsIn
     *        amount of error to accept in floating point comparisons
     */
    SimplexTableau(final LinearObjectiveFunction fIn,
                   final Collection<LinearConstraint> constraintsIn,
                   final GoalType goalType,
                   final boolean restrictToNonNegativeIn,
                   final double epsilonIn,
                   final int maxUlpsIn) {
        this.f = fIn;
        this.constraints = this.normalizeConstraints(constraintsIn);
        this.restrictToNonNegative = restrictToNonNegativeIn;
        this.epsilon = epsilonIn;
        this.maxUlps = maxUlpsIn;
        this.numDecisionVariables = fIn.getCoefficients().getDimension() +
                (restrictToNonNegativeIn ? 0 : 1);
        this.numSlackVariables = this.getConstraintTypeCounts(Relationship.LEQ) +
                this.getConstraintTypeCounts(Relationship.GEQ);
        this.numArtificialVariables = this.getConstraintTypeCounts(Relationship.EQ) +
                this.getConstraintTypeCounts(Relationship.GEQ);
        this.tableau = this.createTableau(goalType == GoalType.MAXIMIZE);
        this.initializeColumnLabels();
    }

    /**
     * Initialize the labels for the columns.
     */
    protected void initializeColumnLabels() {
        if (this.getNumObjectiveFunctions() == 2) {
            // add W if Phase 1
            this.columnLabels.add("W");
        }
        // add Z in the columns labels
        this.columnLabels.add("Z");
        for (int i = 0; i < this.getOriginalNumDecisionVariables(); i++) {
            this.columnLabels.add(X + i);
        }
        if (!this.restrictToNonNegative) {
            this.columnLabels.add(NEGATIVE_VAR_COLUMN_LABEL);
        }
        // loop on the number of slack variables
        for (int i = 0; i < this.getNumSlackVariables(); i++) {
            this.columnLabels.add("s" + i);
        }
        // loop on the number of artificial variables
        for (int i = 0; i < this.getNumArtificialVariables(); i++) {
            this.columnLabels.add("a" + i);
        }
        // add "RHS
        this.columnLabels.add("RHS");
    }

    /**
     * Create the tableau by itself.
     * 
     * @param maximize
     *        if true, goal is to maximize the objective function
     * @return created tableau
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Commons-Math code kept as such
    protected RealMatrix createTableau(final boolean maximize) {
        // CHECKSTYLE: resume CyclomaticComplexity check

        // create a matrix of the correct size
        // + 1 is for RHS
        final int width = this.numDecisionVariables + this.numSlackVariables +
                this.numArtificialVariables + this.getNumObjectiveFunctions() + 1;
        final int height = this.constraints.size() + this.getNumObjectiveFunctions();
        final Array2DRowRealMatrix matrix = new Array2DRowRealMatrix(height, width);

        // initialize the objective function rows
        if (this.getNumObjectiveFunctions() == 2) {
            matrix.setEntry(0, 0, -1);
        }
        final int zIndex = (this.getNumObjectiveFunctions() == 1) ? 0 : 1;
        matrix.setEntry(zIndex, zIndex, maximize ? 1 : -1);
        final RealVector objectiveCoefficients =
            maximize ? this.f.getCoefficients().mapMultiply(-1) : this.f.getCoefficients();
        this.copyArray(objectiveCoefficients.toArray(), matrix.getDataRef()[zIndex]);
        matrix.setEntry(zIndex, width - 1,
            maximize ? this.f.getConstantTerm() : -1 * this.f.getConstantTerm());

        if (!this.restrictToNonNegative) {
            matrix.setEntry(zIndex, this.getSlackVariableOffset() - 1,
                getInvertedCoefficientSum(objectiveCoefficients));
        }

        // initialize the constraint rows
        int slackVar = 0;
        int artificialVar = 0;
        for (int i = 0; i < this.constraints.size(); i++) {
            final LinearConstraint constraint = this.constraints.get(i);
            final int row = this.getNumObjectiveFunctions() + i;

            // decision variable coefficients
            this.copyArray(constraint.getCoefficients().toArray(), matrix.getDataRef()[row]);

            // x-
            if (!this.restrictToNonNegative) {
                matrix.setEntry(row, this.getSlackVariableOffset() - 1,
                    getInvertedCoefficientSum(constraint.getCoefficients()));
            }

            // RHS
            matrix.setEntry(row, width - 1, constraint.getValue());

            // slack variables
            if (constraint.getRelationship() == Relationship.LEQ) {
                // slack
                matrix.setEntry(row, this.getSlackVariableOffset() + slackVar++, 1);
            } else if (constraint.getRelationship() == Relationship.GEQ) {
                // excess
                matrix.setEntry(row, this.getSlackVariableOffset() + slackVar++, -1);
            }

            // artificial variables
            if ((constraint.getRelationship() == Relationship.EQ) ||
                    (constraint.getRelationship() == Relationship.GEQ)) {
                matrix.setEntry(0, this.getArtificialVariableOffset() + artificialVar, 1);
                matrix.setEntry(row, this.getArtificialVariableOffset() + artificialVar++, 1);
                matrix.setRowVector(0, matrix.getRowVector(0).subtract(matrix.getRowVector(row)));
            }
        }

        return matrix;
    }

    /**
     * Get new versions of the constraints which have positive right hand sides.
     * 
     * @param originalConstraints
     *        original (not normalized) constraints
     * @return new versions of the constraints
     */
    public List<LinearConstraint> normalizeConstraints(final Collection<LinearConstraint> originalConstraints) {
        final List<LinearConstraint> normalized = new ArrayList<LinearConstraint>();
        for (final LinearConstraint constraint : originalConstraints) {
            normalized.add(this.normalize(constraint));
        }
        return normalized;
    }

    /**
     * Get a new equation equivalent to this one with a positive right hand side.
     * 
     * @param constraint
     *        reference constraint
     * @return new equation
     */
    private LinearConstraint normalize(final LinearConstraint constraint) {
        if (constraint.getValue() < 0) {
            return new LinearConstraint(constraint.getCoefficients().mapMultiply(-1),
                constraint.getRelationship().oppositeRelationship(),
                -1 * constraint.getValue());
        }
        return new LinearConstraint(constraint.getCoefficients(),
            constraint.getRelationship(), constraint.getValue());
    }

    /**
     * Get the number of objective functions in this tableau.
     * 
     * @return 2 for Phase 1. 1 for Phase 2.
     */
    protected final int getNumObjectiveFunctions() {
        return this.numArtificialVariables > 0 ? 2 : 1;
    }

    /**
     * Get a count of constraints corresponding to a specified relationship.
     * 
     * @param relationship
     *        relationship to count
     * @return number of constraint with the specified relationship
     */
    private int getConstraintTypeCounts(final Relationship relationship) {
        int count = 0;
        for (final LinearConstraint constraint : this.constraints) {
            if (constraint.getRelationship() == relationship) {
                ++count;
            }
        }
        return count;
    }

    /**
     * Get the -1 times the sum of all coefficients in the given array.
     * 
     * @param coefficients
     *        coefficients to sum
     * @return the -1 times the sum of all coefficients in the given array.
     */
    protected static double getInvertedCoefficientSum(final RealVector coefficients) {
        double sum = 0;
        for (final double coefficient : coefficients.toArray()) {
            sum -= coefficient;
        }
        return sum;
    }

    /**
     * Checks whether the given column is basic.
     * 
     * @param col
     *        index of the column to check
     * @return the row that the variable is basic in. null if the column is not basic
     */
    protected Integer getBasicRow(final int col) {
        Integer row = null;
        for (int i = 0; i < this.getHeight(); i++) {
            final double entry = this.getEntry(i, col);
            if (Precision.equals(entry, 1d, this.maxUlps) && (row == null)) {
                row = i;
            } else if (!Precision.equals(entry, 0d, this.maxUlps)) {
                return null;
            }
        }
        return row;
    }

    /**
     * Removes the phase 1 objective function, positive cost non-artificial variables,
     * and the non-basic artificial variables from this tableau.
     */
    protected void dropPhase1Objective() {
        // if Phase 2 return
        if (this.getNumObjectiveFunctions() == 1) {
            // Immediate return
            return;
        }

        // initialize columns TreeSet
        final Set<Integer> columnsToDrop = new TreeSet<Integer>();
        // first term is equal to zero
        columnsToDrop.add(0);

        // positive cost non-artificial variables
        for (int i = this.getNumObjectiveFunctions(); i < this.getArtificialVariableOffset(); i++) {
            final double entry = this.tableau.getEntry(0, i);
            if (Precision.compareTo(entry, 0d, this.epsilon) > 0) {
                columnsToDrop.add(i);
            }
        }

        // non-basic artificial variables
        for (int i = 0; i < this.getNumArtificialVariables(); i++) {
            final int col = i + this.getArtificialVariableOffset();
            if (this.getBasicRow(col) == null) {
                columnsToDrop.add(col);
            }
        }

        // fill the matrix with the tableau entries
        final double[][] matrix = new double[this.getHeight() - 1][this.getWidth() - columnsToDrop.size()];
        for (int i = 1; i < this.getHeight(); i++) {
            int col = 0;
            for (int j = 0; j < this.getWidth(); j++) {
                if (!columnsToDrop.contains(j)) {
                    matrix[i - 1][col++] = this.tableau.getEntry(i, j);
                }
            }
        }

        // remove the columns in reverse order so the indices are correct
        final Integer[] drop = columnsToDrop.toArray(new Integer[columnsToDrop.size()]);
        for (int i = drop.length - 1; i >= 0; i--) {
            this.columnLabels.remove((int) drop[i]);
        }

        // Update variables
        this.tableau = new Array2DRowRealMatrix(matrix);
        this.numArtificialVariables = 0;
    }

    /**
     * @param src
     *        the source array
     * @param dest
     *        the destination array
     */
    private void copyArray(final double[] src, final double[] dest) {
        System.arraycopy(src, 0, dest, this.getNumObjectiveFunctions(), src.length);
    }

    /**
     * Returns whether the problem is at an optimal state.
     * 
     * @return whether the model has been solved
     */
    public boolean isOptimal() {
        for (int i = this.getNumObjectiveFunctions(); i < this.getWidth() - 1; i++) {
            final double entry = this.tableau.getEntry(0, i);
            if (Precision.compareTo(entry, 0d, this.epsilon) < 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get the current solution.
     * 
     * @return current solution
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Commons-Math code kept as such
    protected PointValuePair getSolution() {
        // CHECKSTYLE: resume CyclomaticComplexity check
        final int negativeVarColumn = this.columnLabels.indexOf(NEGATIVE_VAR_COLUMN_LABEL);
        final Integer negativeVarBasicRow = negativeVarColumn > 0 ? this.getBasicRow(negativeVarColumn) : null;
        final double mostNegative =
            negativeVarBasicRow == null ? 0 : this.getEntry(negativeVarBasicRow, this.getRhsOffset());

        final Set<Integer> basicRows = new HashSet<Integer>();
        final double[] coefficients = new double[this.getOriginalNumDecisionVariables()];
        for (int i = 0; i < coefficients.length; i++) {
            final int colIndex = this.columnLabels.indexOf(X + i);
            if (colIndex < 0) {
                coefficients[i] = 0;
                continue;
            }
            final Integer basicRow = this.getBasicRow(colIndex);
            if (basicRow != null && basicRow == 0) {
                // if the basic row is found to be the objective function row
                // set the coefficient to 0 -> this case handles unconstrained
                // variables that are still part of the objective function
                coefficients[i] = 0;
            } else if (basicRows.contains(basicRow)) {
                // if multiple variables can take a given value
                // then we choose the first and set the rest equal to 0
                coefficients[i] = 0 - (this.restrictToNonNegative ? 0 : mostNegative);
            } else {
                basicRows.add(basicRow);
                coefficients[i] =
                    (basicRow == null ? 0 : this.getEntry(basicRow, this.getRhsOffset())) -
                            (this.restrictToNonNegative ? 0 : mostNegative);
            }
        }
        return new PointValuePair(coefficients, this.f.value(coefficients));
    }

    /**
     * Subtracts a multiple of one row from another.
     * <p>
     * After application of this operation, the following will hold:
     * 
     * <pre>
     * minuendRow = minuendRow - multiple * subtrahendRow
     * </pre>
     * 
     * @param dividendRow
     *        index of the row
     * @param divisor
     *        value of the divisor
     */
    protected void divideRow(final int dividendRow, final double divisor) {
        for (int j = 0; j < this.getWidth(); j++) {
            this.tableau.setEntry(dividendRow, j, this.tableau.getEntry(dividendRow, j) / divisor);
        }
    }

    /**
     * Subtracts a multiple of one row from another.
     * <p>
     * After application of this operation, the following will hold:
     * 
     * <pre>
     * minuendRow = minuendRow - multiple * subtrahendRow
     * </pre>
     * 
     * @param minuendRow
     *        row index
     * @param subtrahendRow
     *        row index
     * @param multiple
     *        multiplication factor
     */
    protected void subtractRow(final int minuendRow, final int subtrahendRow,
                               final double multiple) {
        for (int i = 0; i < this.getWidth(); i++) {
            double result = this.tableau.getEntry(minuendRow, i) - this.tableau.getEntry(subtrahendRow, i) * multiple;
            // cut-off values smaller than the CUTOFF_THRESHOLD, otherwise may lead to numerical instabilities
            if (MathLib.abs(result) < CUTOFF_THRESHOLD) {
                result = 0.0;
            }
            this.tableau.setEntry(minuendRow, i, result);
        }
    }

    /**
     * Get the width of the tableau.
     * 
     * @return width of the tableau
     */
    protected final int getWidth() {
        return this.tableau.getColumnDimension();
    }

    /**
     * Get the height of the tableau.
     * 
     * @return height of the tableau
     */
    protected final int getHeight() {
        return this.tableau.getRowDimension();
    }

    /**
     * Get an entry of the tableau.
     * 
     * @param row
     *        row index
     * @param column
     *        column index
     * @return entry at (row, column)
     */
    protected final double getEntry(final int row, final int column) {
        return this.tableau.getEntry(row, column);
    }

    /**
     * Set an entry of the tableau.
     * 
     * @param row
     *        row index
     * @param column
     *        column index
     * @param value
     *        for the entry
     */
    protected final void setEntry(final int row, final int column,
                                  final double value) {
        this.tableau.setEntry(row, column, value);
    }

    /**
     * Get the offset of the first slack variable.
     * 
     * @return offset of the first slack variable
     */
    protected final int getSlackVariableOffset() {
        return this.getNumObjectiveFunctions() + this.numDecisionVariables;
    }

    /**
     * Get the offset of the first artificial variable.
     * 
     * @return offset of the first artificial variable
     */
    protected final int getArtificialVariableOffset() {
        return this.getNumObjectiveFunctions() + this.numDecisionVariables + this.numSlackVariables;
    }

    /**
     * Get the offset of the right hand side.
     * 
     * @return offset of the right hand side
     */
    protected final int getRhsOffset() {
        return this.getWidth() - 1;
    }

    /**
     * Get the number of decision variables.
     * <p>
     * If variables are not restricted to positive values, this will include 1 extra decision variable to represent the
     * absolute value of the most negative variable.
     * 
     * @return number of decision variables
     * @see #getOriginalNumDecisionVariables()
     */
    protected final int getNumDecisionVariables() {
        return this.numDecisionVariables;
    }

    /**
     * Get the original number of decision variables.
     * 
     * @return original number of decision variables
     * @see #getNumDecisionVariables()
     */
    protected final int getOriginalNumDecisionVariables() {
        return this.f.getCoefficients().getDimension();
    }

    /**
     * Get the number of slack variables.
     * 
     * @return number of slack variables
     */
    protected final int getNumSlackVariables() {
        return this.numSlackVariables;
    }

    /**
     * Get the number of artificial variables.
     * 
     * @return number of artificial variables
     */
    protected final int getNumArtificialVariables() {
        return this.numArtificialVariables;
    }

    /**
     * Get the tableau data.
     * 
     * @return tableau data
     */
    protected final double[][] getData() {
        return this.tableau.getData();
    }

    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Commons-Math code kept as such
    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object other) {
        // CHECKSTYLE: resume CyclomaticComplexity check

        if (this == other) {
            return true;
        }

        if (other instanceof SimplexTableau) {
            final SimplexTableau rhs = (SimplexTableau) other;
            return (this.restrictToNonNegative == rhs.restrictToNonNegative) &&
                    (this.numDecisionVariables == rhs.numDecisionVariables) &&
                    (this.numSlackVariables == rhs.numSlackVariables) &&
                    (this.numArtificialVariables == rhs.numArtificialVariables) &&
                    (this.epsilon == rhs.epsilon) &&
                    (this.maxUlps == rhs.maxUlps) &&
                    this.f.equals(rhs.f) &&
                    this.constraints.equals(rhs.constraints) &&
                    this.tableau.equals(rhs.tableau);
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int cond1 = this.numDecisionVariables ^ this.numSlackVariables ^ this.numArtificialVariables;
        final int cond2 = Double.valueOf(this.epsilon).hashCode() ^ this.maxUlps ^ this.f.hashCode();
        final int cond3 = this.constraints.hashCode() ^ this.tableau.hashCode();
        return Boolean.valueOf(this.restrictToNonNegative).hashCode() ^ cond1 ^ cond2 ^ cond3;
    }

    /**
     * Serialize the instance.
     * 
     * @param oos
     *        stream where object should be written
     * @throws IOException
     *         if object cannot be written to stream
     */
    private void writeObject(final ObjectOutputStream oos) throws IOException {
        oos.defaultWriteObject();
        MatrixUtils.serializeRealMatrix(this.tableau, oos);
    }

    /**
     * Deserialize the instance.
     * 
     * @param ois
     *        stream from which the object should be read
     * @throws ClassNotFoundException
     *         if a class in the stream cannot be found
     * @throws IOException
     *         if object cannot be read from the stream
     */
    private void readObject(final ObjectInputStream ois) throws ClassNotFoundException, IOException {
        ois.defaultReadObject();
        tableau = MatrixUtils.deserializeRealMatrix(ois);
    }
}
