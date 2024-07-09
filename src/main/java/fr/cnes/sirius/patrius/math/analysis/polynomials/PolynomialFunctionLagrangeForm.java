/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 *
 *
 * Copyright 2011-2017 CNES
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
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:288:18/09/2014: ephemeris interpolation with variable steps
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.polynomials;

import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;
import fr.cnes.sirius.patrius.math.util.MathArrays;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Implements the representation of a real polynomial function in
 * <a href="http://mathworld.wolfram.com/LagrangeInterpolatingPolynomial.html">
 * Lagrange Form</a>. For reference, see <b>Introduction to Numerical
 * Analysis</b>, ISBN 038795452X, chapter 2.
 * <p>
 * The approximated function should be smooth enough for Lagrange polynomial to work well. Otherwise, consider using
 * splines instead.
 * </p>
 * 
 * @version $Id: PolynomialFunctionLagrangeForm.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 1.2
 */
@SuppressWarnings("PMD.NullAssignment")
public class PolynomialFunctionLagrangeForm implements UnivariateFunction {

    /** 0.5. */
    private static final double HALF = 0.5;

    /**
     * The coefficients of the polynomial, ordered by degree -- i.e.
     * coefficients[0] is the constant term and coefficients[n] is the
     * coefficient of x^n where n is the degree of the polynomial.
     */
    private double[] coefficients;
    /**
     * Interpolating points (abscissas).
     */
    private final double[] x;
    /**
     * Function values at interpolating points.
     */
    private final double[] y;
    /**
     * Function values at interpolating points.
     */
    private final double[][] yTab;
    /**
     * Whether the polynomial coefficients are available.
     */
    private boolean coefficientsComputed;

    /**
     * Construct a Lagrange polynomial with the given abscissas and function
     * values. The order of interpolating points are not important.
     * <p>
     * The constructor makes copy of the input arrays and assigns them.
     * </p>
     * 
     * @param xIn
     *        interpolating points
     * @param yIn
     *        function values at interpolating points
     * @throws DimensionMismatchException
     *         if the array lengths are different.
     * @throws NumberIsTooSmallException
     *         if the number of points is less than 2.
     * @throws fr.cnes.sirius.patrius.math.exception.NonMonotonicSequenceException
     *         if two abscissae have the same value.
     */
    public PolynomialFunctionLagrangeForm(final double[] xIn, final double[] yIn) {
        this.x = new double[xIn.length];
        this.y = new double[yIn.length];

        // just for the constructor not to yield, useless
        this.yTab = null;

        System.arraycopy(xIn, 0, this.x, 0, xIn.length);
        System.arraycopy(yIn, 0, this.y, 0, yIn.length);
        this.coefficientsComputed = false;

        if (!verifyInterpolationArray(xIn, yIn, false)) {
            MathArrays.sortInPlace(this.x, this.y);
            // Second check in case some abscissa is duplicated.
            verifyInterpolationArray(this.x, this.y, true);
        }
    }

    /**
     * Constructs a Lagrange polynomial with the given abscissas and an array of different function
     * values. The order of interpolating points are not important.
     * <p>
     * The constructor makes copy of the input arrays and assigns them.
     * </p>
     * 
     * @param x2
     *        interpolating points
     * @param ytab2
     *        function values at interpolating points
     * @throws DimensionMismatchException
     *         if the array lengths are different.
     * @throws NumberIsTooSmallException
     *         if the number of points is less than 2.
     * @throws fr.cnes.sirius.patrius.math.exception.NonMonotonicSequenceException
     *         if two abscissae have the same value.
     */
    public PolynomialFunctionLagrangeForm(final double[] x2, final double[][] ytab2) {
        this.x = x2.clone();
        final double[][] yTabTemp = ytab2.clone();
        this.coefficientsComputed = false;

        final int numberOfFunctionsToInterpolate = yTabTemp.length;
        final int numberOfPoints = this.x.length;

        final double[] indexTab = new double[numberOfPoints];

        for (int i = 0; i < numberOfPoints; i++) {
            indexTab[i] = i;
        }

        if (!verifyInterpolationArray(this.x, indexTab, false)) {
            MathArrays.sortInPlace(this.x, indexTab);
            // Second check in case some abscissa is duplicated.
            verifyInterpolationArray(this.x, indexTab, true);
        }

        final int finalNumberOfPoints = this.x.length;
        this.yTab = new double[numberOfFunctionsToInterpolate][finalNumberOfPoints];
        int index = 0;
        double yij = 0.0;
        for (int i = 0; i < numberOfFunctionsToInterpolate; i++) {
            for (int j = 0; j < finalNumberOfPoints; j++) {
                index = (int) indexTab[j];
                yij = yTabTemp[i][index];
                this.yTab[i][j] = yij;
            }
        }

        // just for the constructor not to yield, useless
        this.y = this.yTab[0];
    }

    /**
     * Calculate the function value at the given point.
     * 
     * @param z
     *        Point at which the function value is to be computed.
     * @return the function value.
     * @throws DimensionMismatchException
     *         if {@code x} and {@code y} have
     *         different lengths.
     * @throws fr.cnes.sirius.patrius.math.exception.NonMonotonicSequenceException
     *         if {@code x} is not sorted in strictly increasing order.
     * @throws NumberIsTooSmallException
     *         if the size of {@code x} is less
     *         than 2.
     */
    @Override
    public double value(final double z) {
        return evaluateInternal(this.x, this.y, z);
    }

    /**
     * Calculate the function value at the given point.
     * 
     * @param index
     *        : the function to be interpolated, ie btw 0 and yTab.length-1
     * @param z
     *        Point at which the function value is to be computed.
     * @return the function value.
     * @throws DimensionMismatchException
     *         if {@code x} and {@code y} have
     *         different lengths.
     * @throws fr.cnes.sirius.patrius.math.exception.NonMonotonicSequenceException
     *         if {@code x} is not sorted in strictly increasing order.
     * @throws NumberIsTooSmallException
     *         if the size of {@code x} is less
     *         than 2.
     */
    public double valueIndex(final int index, final double z) {

        if (index < 0 || index > (this.yTab.length - 1)) {
            throw new IllegalArgumentException(
                "PolynomialFunctionLagrangeForm : Index is not between 0 and yTab.length ");
        }
        final double[] y2 = this.yTab[index];
        return evaluateInternal(this.x, y2, z);
    }

    /**
     * Returns the degree of the polynomial.
     * 
     * @return the degree of the polynomial
     */
    public int degree() {
        return this.x.length - 1;
    }

    /**
     * Returns a copy of the interpolating points array.
     * <p>
     * Changes made to the returned copy will not affect the polynomial.
     * </p>
     * 
     * @return a fresh copy of the interpolating points array
     */
    public double[] getInterpolatingPoints() {
        final double[] out = new double[this.x.length];
        System.arraycopy(this.x, 0, out, 0, this.x.length);
        return out;
    }

    /**
     * Returns a copy of the interpolating values array.
     * <p>
     * Changes made to the returned copy will not affect the polynomial.
     * </p>
     * 
     * @return a fresh copy of the interpolating values array
     */
    public double[] getInterpolatingValues() {
        final double[] out = new double[this.y.length];
        System.arraycopy(this.y, 0, out, 0, this.y.length);
        return out;
    }

    /**
     * Returns a copy of the interpolating values matrix.
     * <p>
     * Changes made to the returned copy will not affect the polynomial.
     * </p>
     * 
     * @return a fresh copy of the interpolating values matrix
     */
    public double[][] getInterpolatingTabValues() {
        return this.yTab.clone();
    }

    /**
     * Returns a copy of the coefficients array.
     * <p>
     * Changes made to the returned copy will not affect the polynomial.
     * </p>
     * <p>
     * Note that coefficients computation can be ill-conditioned. Use with caution and only when it is necessary.
     * </p>
     * 
     * @return a fresh copy of the coefficients array
     */
    public double[] getCoefficients() {
        if (!this.coefficientsComputed) {
            this.computeCoefficients();
        }
        final double[] out = new double[this.coefficients.length];
        System.arraycopy(this.coefficients, 0, out, 0, this.coefficients.length);
        return out;
    }

    /**
     * Evaluate the Lagrange polynomial using
     * <a href="http://mathworld.wolfram.com/NevillesAlgorithm.html">
     * Neville's Algorithm</a>. It takes O(n^2) time.
     * 
     * @param x
     *        Interpolating points array.
     * @param y
     *        Interpolating values array.
     * @param z
     *        Point at which the function value is to be computed.
     * @return the function value.
     * @throws DimensionMismatchException
     *         if {@code x} and {@code y} have
     *         different lengths.
     * @throws fr.cnes.sirius.patrius.math.exception.NonMonotonicSequenceException
     *         if {@code x} is not sorted in strictly increasing order.
     * @throws NumberIsTooSmallException
     *         if the size of {@code x} is less
     *         than 2.
     */
    public static double evaluate(final double[] x, final double[] y, final double z) {
        if (verifyInterpolationArray(x, y, false)) {
            return evaluateInternal(x, y, z);
        }

        // Array is not sorted.
        final double[] xNew = new double[x.length];
        final double[] yNew = new double[y.length];
        System.arraycopy(x, 0, xNew, 0, x.length);
        System.arraycopy(y, 0, yNew, 0, y.length);

        MathArrays.sortInPlace(xNew, yNew);
        // Second check in case some abscissa is duplicated.
        verifyInterpolationArray(xNew, yNew, true);
        return evaluateInternal(xNew, yNew, z);
    }

    /**
     * Evaluate the Lagrange polynomial using
     * <a href="http://mathworld.wolfram.com/NevillesAlgorithm.html">
     * Neville's Algorithm</a>. It takes O(n^2) time.
     * 
     * @param x
     *        Interpolating points array.
     * @param y
     *        Interpolating values array.
     * @param z
     *        Point at which the function value is to be computed.
     * @return the function value.
     * @throws DimensionMismatchException
     *         if {@code x} and {@code y} have
     *         different lengths.
     * @throws fr.cnes.sirius.patrius.math.exception.NonMonotonicSequenceException
     *         if {@code x} is not sorted in strictly increasing order.
     * @throws NumberIsTooSmallException
     *         if the size of {@code x} is less
     *         than 2.
     */
    private static double evaluateInternal(final double[] x, final double[] y, final double z) {
        int nearest = 0;
        final int n = x.length;
        final double[] c = new double[n];
        final double[] d = new double[n];
        double minDist = Double.POSITIVE_INFINITY;
        for (int i = 0; i < n; i++) {
            // initialize the difference arrays
            c[i] = y[i];
            d[i] = y[i];
            // find out the abscissa closest to z
            final double dist = MathLib.abs(z - x[i]);
            if (dist < minDist) {
                nearest = i;
                minDist = dist;
            }
        }

        // initial approximation to the function value at z
        double value = y[nearest];

        for (int i = 1; i < n; i++) {
            for (int j = 0; j < n - i; j++) {
                final double tc = x[j] - z;
                final double td = x[i + j] - z;
                final double divider = x[j] - x[i + j];
                // update the difference arrays
                final double w = (c[j + 1] - d[j]) / divider;
                c[j] = tc * w;
                d[j] = td * w;
            }
            // sum up the difference terms to get the final value
            if (nearest < HALF * (n - i + 1)) {
                // fork down
                value += c[nearest];
            } else {
                nearest--;
                // fork up
                value += d[nearest];
            }
        }

        return value;
    }

    /**
     * Calculate the coefficients of Lagrange polynomial from the
     * interpolation data. It takes O(n^2) time.
     * Note that this computation can be ill-conditioned: Use with caution
     * and only when it is necessary.
     */
    protected void computeCoefficients() {
        // Get polynomial degree
        final int n = this.degree() + 1;
        this.coefficients = new double[n];
        // Initialize coefficients array
        for (int i = 0; i < n; i++) {
            this.coefficients[i] = 0.0;
        }

        // c[] are the coefficients of P(x) = (x-x[0])(x-x[1])...(x-x[n-1])
        final double[] c = new double[n + 1];
        c[0] = 1.0;
        for (int i = 0; i < n; i++) {
            for (int j = i; j > 0; j--) {
                c[j] = c[j - 1] - c[j] * this.x[i];
            }
            c[0] *= -this.x[i];
            c[i + 1] = 1;
        }

        final double[] tc = new double[n];
        // Loop on coefficients array to compute their value
        for (int i = 0; i < n; i++) {
            // d = (x[i]-x[0])...(x[i]-x[i-1])(x[i]-x[i+1])...(x[i]-x[n-1])
            double d = 1;
            for (int j = 0; j < n; j++) {
                if (i != j) {
                    d *= this.x[i] - this.x[j];
                }
            }
            final double t = this.y[i] / d;
            // Lagrange polynomial is the sum of n terms, each of which is a
            // polynomial of degree n-1. tc[] are the coefficients of the i-th
            // numerator Pi(x) = (x-x[0])...(x-x[i-1])(x-x[i+1])...(x-x[n-1]).
            // actually c[n] = 1
            tc[n - 1] = c[n];
            this.coefficients[n - 1] += t * tc[n - 1];
            for (int j = n - 2; j >= 0; j--) {
                tc[j] = c[j + 1] + tc[j + 1] * this.x[i];
                this.coefficients[j] += t * tc[j];
            }
        }

        this.coefficientsComputed = true;
    }

    /**
     * Check that the interpolation arrays are valid.
     * The arrays features checked by this method are that both arrays have the
     * same length and this length is at least 2.
     * 
     * @param x
     *        Interpolating points array.
     * @param y
     *        Interpolating values array.
     * @param abort
     *        Whether to throw an exception if {@code x} is not sorted.
     * @throws DimensionMismatchException
     *         if the array lengths are different.
     * @throws NumberIsTooSmallException
     *         if the number of points is less than 2.
     * @throws fr.cnes.sirius.patrius.math.exception.NonMonotonicSequenceException
     *         if {@code x} is not sorted in strictly increasing order and {@code abort} is {@code true}.
     * @return {@code false} if the {@code x} is not sorted in increasing order, {@code true} otherwise.
     * @see #evaluate(double[], double[], double)
     * @see #computeCoefficients()
     */
    public static boolean verifyInterpolationArray(final double[] x, final double[] y, final boolean abort) {
        if (x.length != y.length) {
            throw new DimensionMismatchException(x.length, y.length);
        }
        if (x.length < 2) {
            throw new NumberIsTooSmallException(PatriusMessages.WRONG_NUMBER_OF_POINTS, 2, x.length, true);
        }

        return MathArrays.checkOrder(x, MathArrays.OrderDirection.INCREASING, true, abort);
    }
}
