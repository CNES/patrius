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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.polynomials;

import java.util.Arrays;

import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.differentiation.DerivativeStructure;
import fr.cnes.sirius.patrius.math.analysis.differentiation.UnivariateDifferentiableFunction;
import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;
import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;
import fr.cnes.sirius.patrius.math.util.MathArrays;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Represents a polynomial spline function.
 * <p>
 * A <strong>polynomial spline function</strong> consists of a set of <i>interpolating polynomials</i> and an ascending
 * array of domain <i>knot points</i>, determining the intervals over which the spline function is defined by the
 * constituent polynomials. The polynomials are assumed to have been computed to match the values of another function at
 * the knot points. The value consistency constraints are not currently enforced by
 * <code>PolynomialSplineFunction</code> itself, but are assumed to hold among the polynomials and knot points passed to
 * the constructor.
 * </p>
 * <p>
 * N.B.: The polynomials in the <code>polynomials</code> property must be centered on the knot points to compute the
 * spline function values. See below.
 * </p>
 * <p>
 * The domain of the polynomial spline function is <code>[smallest knot, largest knot]</code>. Attempts to evaluate the
 * function at values outside of this range generate IllegalArgumentExceptions.
 * </p>
 * <p>
 * The value of the polynomial spline function for an argument <code>x</code> is computed as follows:
 * <ol>
 * <li>The knot array is searched to find the segment to which <code>x</code> belongs. If <code>x</code> is less than
 * the smallest knot point or greater than the largest one, an <code>IllegalArgumentException</code> is thrown.</li>
 * <li>Let <code>j</code> be the index of the largest knot point that is less than or equal to <code>x</code>. The value
 * returned is <br>
 * <code>polynomials[j](x - knot[j])</code></li>
 * </ol>
 * </p>
 * 
 * @version $Id: PolynomialSplineFunction.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class PolynomialSplineFunction implements UnivariateDifferentiableFunction {

    /** Serializable UID. */
    private static final long serialVersionUID = 7055079862533339822L;

    /**
     * Spline segment interval delimiters (knots).
     * Size is n + 1 for n segments.
     */
    private final double[] knots;

    /**
     * The polynomial functions that make up the spline. The first element
     * determines the value of the spline over the first subinterval, the
     * second over the second, etc. Spline function values are determined by
     * evaluating these functions at {@code (x - knot[i])} where i is the
     * knot segment to which x belongs.
     */
    private final PolynomialFunction[] polynomials;

    /**
     * Number of spline segments. It is equal to the number of polynomials and
     * to the number of partition points - 1.
     */
    private final int n;

    /**
     * Construct a polynomial spline function with the given segment delimiters
     * and interpolating polynomials.
     * The constructor copies both arrays and assigns the copies to the knots
     * and polynomials properties, respectively.
     * 
     * @param knotsIn
     *        Spline segment interval delimiters.
     * @param polynomialsIn
     *        Polynomial functions that make up the spline.
     * @throws NullArgumentException
     *         if either of the input arrays is {@code null}.
     * @throws NumberIsTooSmallException
     *         if knots has length less than 2.
     * @throws DimensionMismatchException
     *         if {@code polynomials.length != knots.length - 1}.
     * @throws fr.cnes.sirius.patrius.math.exception.NonMonotonicSequenceException
     *         if
     *         the {@code knots} array is not strictly increasing.
     * 
     */
    public PolynomialSplineFunction(final double[] knotsIn, final PolynomialFunction[] polynomialsIn) {
        if (knotsIn == null ||
            polynomialsIn == null) {
            throw new NullArgumentException();
        }
        if (knotsIn.length < 2) {
            throw new NumberIsTooSmallException(PatriusMessages.NOT_ENOUGH_POINTS_IN_SPLINE_PARTITION,
                2, knotsIn.length, false);
        }
        if (knotsIn.length - 1 != polynomialsIn.length) {
            throw new DimensionMismatchException(polynomialsIn.length, knotsIn.length);
        }
        MathArrays.checkOrder(knotsIn);

        this.n = knotsIn.length - 1;
        this.knots = new double[this.n + 1];
        System.arraycopy(knotsIn, 0, this.knots, 0, this.n + 1);
        this.polynomials = new PolynomialFunction[this.n];
        System.arraycopy(polynomialsIn, 0, this.polynomials, 0, this.n);
    }

    /**
     * Compute the value for the function.
     * See {@link PolynomialSplineFunction} for details on the algorithm for
     * computing the value of the function.
     * 
     * @param v
     *        Point for which the function value should be computed.
     * @return the value.
     * @throws OutOfRangeException
     *         if {@code v} is outside of the domain of the
     *         spline function (smaller than the smallest knot point or larger than the
     *         largest knot point).
     */
    @Override
    public double value(final double v) {
        if (v < this.knots[0] || v > this.knots[this.n]) {
            throw new OutOfRangeException(v, this.knots[0], this.knots[this.n]);
        }
        int i = Arrays.binarySearch(this.knots, v);
        if (i < 0) {
            i = -i - 2;
        }
        // This will handle the case where v is the last knot value
        // There are only n-1 polynomials, so if v is the last knot
        // then we will use the last polynomial to calculate the value.
        if (i >= this.polynomials.length) {
            i--;
        }
        return this.polynomials[i].value(v - this.knots[i]);
    }

    /**
     * Get the derivative of the polynomial spline function.
     * 
     * @return the derivative function.
     */
    public UnivariateFunction derivative() {
        return this.polynomialSplineDerivative();
    }

    /**
     * Get the derivative of the polynomial spline function.
     * 
     * @return the derivative function.
     */
    public PolynomialSplineFunction polynomialSplineDerivative() {
        final PolynomialFunction[] derivativePolynomials = new PolynomialFunction[this.n];
        for (int i = 0; i < this.n; i++) {
            derivativePolynomials[i] = this.polynomials[i].polynomialDerivative();
        }
        return new PolynomialSplineFunction(this.knots, derivativePolynomials);
    }

    /**
     * {@inheritDoc}
     * 
     * @since 3.1
     */
    @Override
    public DerivativeStructure value(final DerivativeStructure t) {
        final double t0 = t.getValue();
        if (t0 < this.knots[0] || t0 > this.knots[this.n]) {
            throw new OutOfRangeException(t0, this.knots[0], this.knots[this.n]);
        }
        int i = Arrays.binarySearch(this.knots, t0);
        if (i < 0) {
            i = -i - 2;
        }
        // This will handle the case where t is the last knot value
        // There are only n-1 polynomials, so if t is the last knot
        // then we will use the last polynomial to calculate the value.
        if (i >= this.polynomials.length) {
            i--;
        }
        return this.polynomials[i].value(t.subtract(this.knots[i]));
    }

    /**
     * Get the number of spline segments.
     * It is also the number of polynomials and the number of knot points - 1.
     * 
     * @return the number of spline segments.
     */
    public int getN() {
        return this.n;
    }

    /**
     * Get a copy of the interpolating polynomials array.
     * It returns a fresh copy of the array. Changes made to the copy will
     * not affect the polynomials property.
     * 
     * @return the interpolating polynomials.
     */
    public PolynomialFunction[] getPolynomials() {
        final PolynomialFunction[] p = new PolynomialFunction[this.n];
        System.arraycopy(this.polynomials, 0, p, 0, this.n);
        return p;
    }

    /**
     * Get an array copy of the knot points.
     * It returns a fresh copy of the array. Changes made to the copy
     * will not affect the knots property.
     * 
     * @return the knot points.
     */
    public double[] getKnots() {
        final double[] out = new double[this.n + 1];
        System.arraycopy(this.knots, 0, out, 0, this.n + 1);
        return out;
    }
}
