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

import java.util.Arrays;
import java.util.Comparator;

import fr.cnes.sirius.patrius.math.analysis.MultivariateFunction;
import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;
import fr.cnes.sirius.patrius.math.exception.ZeroException;
import fr.cnes.sirius.patrius.math.optim.OptimizationData;
import fr.cnes.sirius.patrius.math.optim.PointValuePair;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This class implements the simplex concept.
 * It is intended to be used in conjunction with {@link SimplexOptimizer}. <br/>
 * The initial configuration of the simplex is set by the constructors {@link #AbstractSimplex(double[])} or
 * {@link #AbstractSimplex(double[][])}.
 * The other {@link #AbstractSimplex(int) constructor} will set all steps
 * to 1, thus building a default configuration from a unit hypercube. <br/>
 * Users <em>must</em> call the {@link #build(double[]) build} method in order
 * to create the data structure that will be acted on by the other methods of
 * this class.
 * 
 * @see SimplexOptimizer
 * @version $Id: AbstractSimplex.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.0
 */
public abstract class AbstractSimplex implements OptimizationData {
    /** Simplex. */
    private PointValuePair[] simplex;
    /** Start simplex configuration. */
    private final double[][] startConfiguration;
    /** Simplex dimension (must be equal to {@code simplex.length - 1}). */
    private final int dimension;

    /**
     * Build a unit hypercube simplex.
     * 
     * @param n
     *        Dimension of the simplex.
     */
    protected AbstractSimplex(final int n) {
        this(n, 1d);
    }

    /**
     * Build a hypercube simplex with the given side length.
     * 
     * @param n
     *        Dimension of the simplex.
     * @param sideLength
     *        Length of the sides of the hypercube.
     */
    protected AbstractSimplex(final int n,
        final double sideLength) {
        this(createHypercubeSteps(n, sideLength));
    }

    /**
     * The start configuration for simplex is built from a box parallel to
     * the canonical axes of the space. The simplex is the subset of vertices
     * of a box parallel to the canonical axes. It is built as the path followed
     * while traveling from one vertex of the box to the diagonally opposite
     * vertex moving only along the box edges. The first vertex of the box will
     * be located at the start point of the optimization.
     * As an example, in dimension 3 a simplex has 4 vertices. Setting the
     * steps to (1, 10, 2) and the start point to (1, 1, 1) would imply the
     * start simplex would be: { (1, 1, 1), (2, 1, 1), (2, 11, 1), (2, 11, 3) }.
     * The first vertex would be set to the start point at (1, 1, 1) and the
     * last vertex would be set to the diagonally opposite vertex at (2, 11, 3).
     * 
     * @param steps
     *        Steps along the canonical axes representing box edges. They
     *        may be negative but not zero.
     * @throws NullArgumentException
     *         if {@code steps} is {@code null}.
     * @throws ZeroException
     *         if one of the steps is zero.
     */
    protected AbstractSimplex(final double[] steps) {
        if (steps == null) {
            throw new NullArgumentException();
        }
        if (steps.length == 0) {
            throw new ZeroException();
        }
        this.dimension = steps.length;

        // Only the relative position of the n final vertices with respect
        // to the first one are stored.
        this.startConfiguration = new double[this.dimension][this.dimension];
        for (int i = 0; i < this.dimension; i++) {
            final double[] vertexI = this.startConfiguration[i];
            for (int j = 0; j < i + 1; j++) {
                if (steps[j] == 0) {
                    throw new ZeroException(PatriusMessages.EQUAL_VERTICES_IN_SIMPLEX);
                }
                System.arraycopy(steps, 0, vertexI, 0, j + 1);
            }
        }
    }

    /**
     * The real initial simplex will be set up by moving the reference
     * simplex such that its first point is located at the start point of the
     * optimization.
     * 
     * @param referenceSimplex
     *        Reference simplex.
     * @throws NotStrictlyPositiveException
     *         if the reference simplex does not
     *         contain at least one point.
     * @throws DimensionMismatchException
     *         if there is a dimension mismatch
     *         in the reference simplex.
     * @throws IllegalArgumentException
     *         if one of its vertices is duplicated.
     */
    protected AbstractSimplex(final double[][] referenceSimplex) {
        if (referenceSimplex.length <= 0) {
            throw new NotStrictlyPositiveException(PatriusMessages.SIMPLEX_NEED_ONE_POINT,
                referenceSimplex.length);
        }
        this.dimension = referenceSimplex.length - 1;

        // Only the relative position of the n final vertices with respect
        // to the first one are stored.
        this.startConfiguration = new double[this.dimension][this.dimension];
        final double[] ref0 = referenceSimplex[0];

        // Loop over vertices.
        for (int i = 0; i < referenceSimplex.length; i++) {
            final double[] refI = referenceSimplex[i];

            // Safety checks.
            if (refI.length != this.dimension) {
                throw new DimensionMismatchException(refI.length, this.dimension);
            }
            for (int j = 0; j < i; j++) {
                final double[] refJ = referenceSimplex[j];
                boolean allEquals = true;
                for (int k = 0; k < this.dimension; k++) {
                    if (refI[k] != refJ[k]) {
                        allEquals = false;
                        break;
                    }
                }
                if (allEquals) {
                    throw new MathIllegalArgumentException(PatriusMessages.EQUAL_VERTICES_IN_SIMPLEX,
                        i, j);
                }
            }

            // Store vertex i position relative to vertex 0 position.
            if (i > 0) {
                final double[] confI = this.startConfiguration[i - 1];
                for (int k = 0; k < this.dimension; k++) {
                    confI[k] = refI[k] - ref0[k];
                }
            }
        }
    }

    /**
     * Get simplex dimension.
     * 
     * @return the dimension of the simplex.
     */
    public int getDimension() {
        return this.dimension;
    }

    /**
     * Get simplex size.
     * After calling the {@link #build(double[]) build} method, this method will
     * will be equivalent to {@code getDimension() + 1}.
     * 
     * @return the size of the simplex.
     */
    public int getSize() {
        return this.simplex.length;
    }

    /**
     * Compute the next simplex of the algorithm.
     * 
     * @param evaluationFunction
     *        Evaluation function.
     * @param comparator
     *        Comparator to use to sort simplex vertices from best
     *        to worst.
     * @throws fr.cnes.sirius.patrius.math.exception.TooManyEvaluationsException
     *         if the algorithm fails to converge.
     */
    public abstract void iterate(final MultivariateFunction evaluationFunction,
                                 final Comparator<PointValuePair> comparator);

    /**
     * Build an initial simplex.
     * 
     * @param startPoint
     *        First point of the simplex.
     * @throws DimensionMismatchException
     *         if the start point does not match
     *         simplex dimension.
     */
    public void build(final double[] startPoint) {
        if (this.dimension != startPoint.length) {
            // Exception : the dimensions don't match
            throw new DimensionMismatchException(this.dimension, startPoint.length);
        }

        // Initialize simplex
        this.simplex = new PointValuePair[this.dimension + 1];
        // Set first vertex.
        this.simplex[0] = new PointValuePair(startPoint, Double.NaN);

        // Set remaining vertices.
        for (int i = 0; i < this.dimension; i++) {
            final double[] confI = this.startConfiguration[i];
            final double[] vertexI = new double[this.dimension];
            for (int k = 0; k < this.dimension; k++) {
                vertexI[k] = startPoint[k] + confI[k];
            }
            this.simplex[i + 1] = new PointValuePair(vertexI, Double.NaN);
        }
    }

    /**
     * Evaluate all the non-evaluated points of the simplex.
     * 
     * @param evaluationFunction
     *        Evaluation function.
     * @param comparator
     *        Comparator to use to sort simplex vertices from best to worst.
     * @throws fr.cnes.sirius.patrius.math.exception.TooManyEvaluationsException
     *         if the maximal number of evaluations is exceeded.
     */
    public void evaluate(final MultivariateFunction evaluationFunction,
                         final Comparator<PointValuePair> comparator) {
        // Evaluate the objective function at all non-evaluated simplex points.
        for (int i = 0; i < this.simplex.length; i++) {
            final PointValuePair vertex = this.simplex[i];
            final double[] point = vertex.getPointRef();
            if (Double.isNaN(vertex.getValue())) {
                this.simplex[i] = new PointValuePair(point, evaluationFunction.value(point), false);
            }
        }

        // Sort the simplex from best to worst.
        Arrays.sort(this.simplex, comparator);
    }

    /**
     * Replace the worst point of the simplex by a new point.
     * 
     * @param pointValuePair
     *        Point to insert.
     * @param comparator
     *        Comparator to use for sorting the simplex vertices
     *        from best to worst.
     * @return modified point-value pair
     */
    protected PointValuePair replaceWorstPoint(final PointValuePair pointValuePair,
                                               final Comparator<PointValuePair> comparator) {
        PointValuePair res = pointValuePair;
        for (int i = 0; i < this.dimension; i++) {
            if (comparator.compare(this.simplex[i], res) > 0) {
                final PointValuePair tmp = this.simplex[i];
                this.simplex[i] = res;
                res = tmp;
            }
        }
        this.simplex[this.dimension] = res;
        return res;
    }

    /**
     * Get the points of the simplex.
     * 
     * @return all the simplex points.
     */
    public PointValuePair[] getPoints() {
        final PointValuePair[] copy = new PointValuePair[this.simplex.length];
        System.arraycopy(this.simplex, 0, copy, 0, this.simplex.length);
        return copy;
    }

    /**
     * Get the simplex point stored at the requested {@code index}.
     * 
     * @param index
     *        Location.
     * @return the point at location {@code index}.
     */
    public PointValuePair getPoint(final int index) {
        if (index < 0 ||
            index >= this.simplex.length) {
            throw new OutOfRangeException(index, 0, this.simplex.length - 1);
        }
        return this.simplex[index];
    }

    /**
     * Store a new point at location {@code index}.
     * Note that no deep-copy of {@code point} is performed.
     * 
     * @param index
     *        Location.
     * @param point
     *        New value.
     */
    protected void setPoint(final int index, final PointValuePair point) {
        if (index < 0 ||
            index >= this.simplex.length) {
            throw new OutOfRangeException(index, 0, this.simplex.length - 1);
        }
        this.simplex[index] = point;
    }

    /**
     * Replace all points.
     * Note that no deep-copy of {@code points} is performed.
     * 
     * @param points
     *        New Points.
     */
    protected void setPoints(final PointValuePair[] points) {
        if (points.length != this.simplex.length) {
            throw new DimensionMismatchException(points.length, this.simplex.length);
        }
        this.simplex = points;
    }

    /**
     * Create steps for a unit hypercube.
     * 
     * @param n
     *        Dimension of the hypercube.
     * @param sideLength
     *        Length of the sides of the hypercube.
     * @return the steps.
     */
    private static double[] createHypercubeSteps(final int n,
                                                 final double sideLength) {
        final double[] steps = new double[n];
        for (int i = 0; i < n; i++) {
            steps[i] = sideLength;
        }
        return steps;
    }
}
