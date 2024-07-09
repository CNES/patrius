/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * HISTORY
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

import java.util.ArrayList;
import java.util.List;

import fr.cnes.sirius.patrius.math.exception.TooManyIterationsException;
import fr.cnes.sirius.patrius.math.optim.PointValuePair;
import fr.cnes.sirius.patrius.math.util.Precision;

/**
 * Solves a linear problem using the "Two-Phase Simplex" method.
 * 
 * @version $Id: SimplexSolver.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 2.0
 */
public class SimplexSolver extends LinearOptimizer {

    /** Default amount of error to accept for algorithm convergence. */
    private static final double DEFAULT_EPSILON = 1.0e-6;

    /** Default amount of error to accept in floating point comparisons (as ulps). */
    private static final int DEFAULT_ULPS = 10;

    /** Amount of error to accept for algorithm convergence. */
    private final double epsilon;

    /** Amount of error to accept in floating point comparisons (as ulps). */
    private final int maxUlps;

    /**
     * Builds a simplex solver with default settings.
     */
    public SimplexSolver() {
        this(DEFAULT_EPSILON, DEFAULT_ULPS);
    }

    /**
     * Builds a simplex solver with a specified accepted amount of error.
     * 
     * @param epsilonIn
     *        Amount of error to accept for algorithm convergence.
     * @param maxUlpsIn
     *        Amount of error to accept in floating point comparisons.
     */
    public SimplexSolver(final double epsilonIn,
                         final int maxUlpsIn) {
        super();
        this.epsilon = epsilonIn;
        this.maxUlps = maxUlpsIn;
    }

    /**
     * Returns the column with the most negative coefficient in the objective function row.
     * 
     * @param tableau
     *        Simple tableau for the problem.
     * @return the column with the most negative coefficient.
     */
    private Integer getPivotColumn(final SimplexTableau tableau) {
        double minValue = 0;
        Integer minPos = null;
        for (int i = tableau.getNumObjectiveFunctions(); i < tableau.getWidth() - 1; i++) {
            final double entry = tableau.getEntry(0, i);
            // check if the entry is strictly smaller than the current minimum
            // do not use a ulp/epsilon check
            if (entry < minValue) {
                minValue = entry;
                minPos = i;
            }
        }
        return minPos;
    }

    /**
     * Returns the row with the minimum ratio as given by the minimum ratio test (MRT).
     * 
     * @param tableau
     *        Simple tableau for the problem.
     * @param col
     *        Column to test the ratio of (see {@link #getPivotColumn(SimplexTableau)}).
     * @return the row with the minimum ratio.
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // CHECKSTYLE: stop ReturnCount check
    // CHECKSTYLE: stop NestedBlockDepth check
    // Reason: Commons-Math code kept as such
    private Integer getPivotRow(final SimplexTableau tableau, final int col) {
        // CHECKSTYLE: resume CyclomaticComplexity check
        // CHECKSTYLE: resume ReturnCount check
        // create a list of all the rows that tie for the lowest score in the minimum ratio test
        List<Integer> minRatioPositions = new ArrayList<Integer>();
        double minRatio = Double.MAX_VALUE;
        for (int i = tableau.getNumObjectiveFunctions(); i < tableau.getHeight(); i++) {
            final double rhs = tableau.getEntry(i, tableau.getWidth() - 1);
            final double entry = tableau.getEntry(i, col);

            if (Precision.compareTo(entry, 0d, this.maxUlps) > 0) {
                final double ratio = rhs / entry;
                // check if the entry is strictly equal to the current min ratio
                // do not use a ulp/epsilon check
                final int cmp = Double.compare(ratio, minRatio);
                if (cmp == 0) {
                    minRatioPositions.add(i);
                } else if (cmp < 0) {
                    minRatio = ratio;
                    minRatioPositions = new ArrayList<Integer>();
                    minRatioPositions.add(i);
                }
            }
        }

        if (minRatioPositions.isEmpty()) {
            return null;
        } else if (!minRatioPositions.isEmpty()) {
            // there's a degeneracy as indicated by a tie in the minimum ratio test

            // 1. check if there's an artificial variable that can be forced out of the basis
            if (tableau.getNumArtificialVariables() > 0) {
                for (final Integer row : minRatioPositions) {
                    for (int i = 0; i < tableau.getNumArtificialVariables(); i++) {
                        final int column = i + tableau.getArtificialVariableOffset();
                        final double entry = tableau.getEntry(row, column);
                        if (Precision.equals(entry, 1d, this.maxUlps) && row.equals(tableau.getBasicRow(column))) {
                            return row;
                        }
                    }
                }
            }

            // 2. apply Bland's rule to prevent cycling:
            // take the row for which the corresponding basic variable has the smallest index
            //
            // see http://www.stanford.edu/class/msande310/blandrule.pdf
            // see http://en.wikipedia.org/wiki/Bland%27s_rule (not equivalent to the above paper)
            //
            // Additional heuristic: if we did not get a solution after half of maxIterations
            // revert to the simple case of just returning the top-most row
            // This heuristic is based on empirical data gathered while investigating MATH-828.
            if (this.getEvaluations() < this.getMaxEvaluations() / 2) {
                Integer minRow = null;
                int minIndex = tableau.getWidth();
                final int varStart = tableau.getNumObjectiveFunctions();
                final int varEnd = tableau.getWidth() - 1;
                for (final Integer row : minRatioPositions) {
                    for (int i = varStart; i < varEnd && !row.equals(minRow); i++) {
                        final Integer basicRow = tableau.getBasicRow(i);
                        if (basicRow != null && basicRow.equals(row)) {
                            if (i < minIndex) {
                                minIndex = i;
                                minRow = row;
                            }
                        }
                    }
                }
                return minRow;
            }
        }
        return minRatioPositions.get(0);
    }

    // CHECKSTYLE: resume NestedBlockDepth check

    /**
     * Runs one iteration of the Simplex method on the given model.
     * 
     * @param tableau
     *        Simple tableau for the problem.
     * @throws TooManyIterationsException
     *         if the allowed number of iterations has been exhausted.
     * @throws UnboundedSolutionException
     *         if the model is found not to have a bounded solution.
     */
    protected void doIteration(final SimplexTableau tableau) {

        // increment the iteration count
        this.incrementIterationCount();

        final Integer pivotCol = this.getPivotColumn(tableau);
        final Integer pivotRow = this.getPivotRow(tableau, pivotCol);
        // raise an exception if the row is null
        if (pivotRow == null) {
            throw new UnboundedSolutionException();
        }

        // set the pivot element to 1
        final double pivotVal = tableau.getEntry(pivotRow, pivotCol);
        tableau.divideRow(pivotRow, pivotVal);

        // set the rest of the pivot column to 0
        for (int i = 0; i < tableau.getHeight(); i++) {
            if (i != pivotRow) {
                final double multiplier = tableau.getEntry(i, pivotCol);
                tableau.subtractRow(i, pivotRow, multiplier);
            }
        }
    }

    /**
     * Solves Phase 1 of the Simplex method.
     * 
     * @param tableau
     *        Simple tableau for the problem.
     * @throws TooManyIterationsException
     *         if the allowed number of iterations has been exhausted.
     * @throws UnboundedSolutionException
     *         if the model is found not to have a bounded solution.
     * @throws NoFeasibleSolutionException
     *         if there is no feasible solution?
     */
    protected void solvePhase1(final SimplexTableau tableau) {

        // make sure we're in Phase 1
        if (tableau.getNumArtificialVariables() == 0) {
            return;
        }

        while (!tableau.isOptimal()) {
            this.doIteration(tableau);
        }

        // if W is not zero then we have no feasible solution
        if (!Precision.equals(tableau.getEntry(0, tableau.getRhsOffset()), 0d, this.epsilon)) {
            throw new NoFeasibleSolutionException();
        }
    }

    /** {@inheritDoc} */
    @Override
    public PointValuePair doOptimize() {
        final SimplexTableau tableau =
            new SimplexTableau(this.getFunction(),
                this.getConstraints(),
                this.getGoalType(),
                this.isRestrictedToNonNegative(),
                this.epsilon,
                this.maxUlps);

        this.solvePhase1(tableau);
        tableau.dropPhase1Objective();

        while (!tableau.isOptimal()) {
            this.doIteration(tableau);
        }
        return tableau.getSolution();
    }
}
