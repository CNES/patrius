/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
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
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.cnes.sirius.patrius.math.analysis.polynomials;

import fr.cnes.sirius.patrius.math.analysis.differentiation.DerivativeStructure;
import fr.cnes.sirius.patrius.math.analysis.differentiation.UnivariateDifferentiableFunction;
import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.NoDataException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Implements the representation of a real polynomial function in
 * Newton Form. For reference, see <b>Elementary Numerical Analysis</b>,
 * ISBN 0070124477, chapter 2.
 * <p>
 * The formula of polynomial in Newton form is p(x) = a[0] + a[1](x-c[0]) + a[2](x-c[0])(x-c[1]) + ... +
 * a[n](x-c[0])(x-c[1])...(x-c[n-1]) Note that the length of a[] is one more than the length of c[]
 * </p>
 * 
 * @version $Id: PolynomialFunctionNewtonForm.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 1.2
 */
public class PolynomialFunctionNewtonForm implements UnivariateDifferentiableFunction {

    /**
     * The coefficients of the polynomial, ordered by degree -- i.e.
     * coefficients[0] is the constant term and coefficients[n] is the
     * coefficient of x^n where n is the degree of the polynomial.
     */
    private double[] coefficients;

    /**
     * Centers of the Newton polynomial.
     */
    private final double[] c;

    /**
     * When all c[i] = 0, a[] becomes normal polynomial coefficients,
     * i.e. a[i] = coefficients[i].
     */
    private final double[] a;

    /**
     * Whether the polynomial coefficients are available.
     */
    private boolean coefficientsComputed;

    /**
     * Construct a Newton polynomial with the given a[] and c[]. The order of
     * centers are important in that if c[] shuffle, then values of a[] would
     * completely change, not just a permutation of old a[].
     * <p>
     * The constructor makes copy of the input arrays and assigns them.
     * </p>
     * 
     * @param aIn
     *        Coefficients in Newton form formula.
     * @param cIn
     *        Centers.
     * @throws fr.cnes.sirius.patrius.math.exception.NullArgumentException
     *         if
     *         any argument is {@code null}.
     * @throws NoDataException
     *         if any array has zero length.
     * @throws DimensionMismatchException
     *         if the size difference between {@code a} and {@code c} is not equal to 1.
     */
    public PolynomialFunctionNewtonForm(final double[] aIn, final double[] cIn) {

        verifyInputArray(aIn, cIn);
        this.a = new double[aIn.length];
        this.c = new double[cIn.length];
        System.arraycopy(aIn, 0, this.a, 0, aIn.length);
        System.arraycopy(cIn, 0, this.c, 0, cIn.length);
        this.coefficientsComputed = false;
    }

    /**
     * Calculate the function value at the given point.
     * 
     * @param z
     *        Point at which the function value is to be computed.
     * @return the function value.
     */
    @Override
    public double value(final double z) {
        return evaluate(this.a, this.c, z);
    }

    /**
     * {@inheritDoc}
     * 
     * @since 3.1
     */
    @Override
    public DerivativeStructure value(final DerivativeStructure t) {
        verifyInputArray(this.a, this.c);

        final int n = this.c.length;
        DerivativeStructure value = new DerivativeStructure(t.getFreeParameters(), t.getOrder(), this.a[n]);
        for (int i = n - 1; i >= 0; i--) {
            value = t.subtract(this.c[i]).multiply(value).add(this.a[i]);
        }

        return value;

    }

    /**
     * Returns the degree of the polynomial.
     * 
     * @return the degree of the polynomial
     */
    public int degree() {
        return this.c.length;
    }

    /**
     * Returns a copy of coefficients in Newton form formula.
     * <p>
     * Changes made to the returned copy will not affect the polynomial.
     * </p>
     * 
     * @return a fresh copy of coefficients in Newton form formula
     */
    public double[] getNewtonCoefficients() {
        final double[] out = new double[this.a.length];
        System.arraycopy(this.a, 0, out, 0, this.a.length);
        return out;
    }

    /**
     * Returns a copy of the centers array.
     * <p>
     * Changes made to the returned copy will not affect the polynomial.
     * </p>
     * 
     * @return a fresh copy of the centers array.
     */
    public double[] getCenters() {
        final double[] out = new double[this.c.length];
        System.arraycopy(this.c, 0, out, 0, this.c.length);
        return out;
    }

    /**
     * Returns a copy of the coefficients array.
     * <p>
     * Changes made to the returned copy will not affect the polynomial.
     * </p>
     * 
     * @return a fresh copy of the coefficients array.
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
     * Evaluate the Newton polynomial using nested multiplication. It is
     * also called <a href="http://mathworld.wolfram.com/HornersRule.html">
     * Horner's Rule</a> and takes O(N) time.
     * 
     * @param a
     *        Coefficients in Newton form formula.
     * @param c
     *        Centers.
     * @param z
     *        Point at which the function value is to be computed.
     * @return the function value.
     * @throws fr.cnes.sirius.patrius.math.exception.NullArgumentException
     *         if
     *         any argument is {@code null}.
     * @throws NoDataException
     *         if any array has zero length.
     * @throws DimensionMismatchException
     *         if the size difference between {@code a} and {@code c} is not equal to 1.
     */
    public static double evaluate(final double[] a, final double[] c, final double z) {
        verifyInputArray(a, c);

        final int n = c.length;
        double value = a[n];
        for (int i = n - 1; i >= 0; i--) {
            value = a[i] + (z - c[i]) * value;
        }

        return value;
    }

    /**
     * Calculate the normal polynomial coefficients given the Newton form.
     * It also uses nested multiplication but takes O(N^2) time.
     */
    protected void computeCoefficients() {
        // Get polynomial degree
        final int n = this.degree();

        // Initialize coefficients array
        this.coefficients = new double[n + 1];
        for (int i = 0; i <= n; i++) {
            this.coefficients[i] = 0.0;
        }

        // Zero order coefficient
        this.coefficients[0] = this.a[n];
        // Compute coefficients
        for (int i = n - 1; i >= 0; i--) {
            for (int j = n - i; j > 0; j--) {
                this.coefficients[j] = this.coefficients[j - 1] - this.c[i] * this.coefficients[j];
            }
            this.coefficients[0] = this.a[i] - this.c[i] * this.coefficients[0];
        }

        this.coefficientsComputed = true;
    }

    /**
     * Verifies that the input arrays are valid.
     * <p>
     * The centers must be distinct for interpolation purposes, but not for general use. Thus it is not verified here.
     * </p>
     * 
     * @param a
     *        the coefficients in Newton form formula
     * @param c
     *        the centers
     * @throws fr.cnes.sirius.patrius.math.exception.NullArgumentException
     *         if
     *         any argument is {@code null}.
     * @throws NoDataException
     *         if any array has zero length.
     * @throws DimensionMismatchException
     *         if the size difference between {@code a} and {@code c} is not equal to 1.
     */
    protected static void verifyInputArray(final double[] a, final double[] c) {
        if (a.length == 0 ||
            c.length == 0) {
            throw new NoDataException(PatriusMessages.EMPTY_POLYNOMIALS_COEFFICIENTS_ARRAY);
        }
        if (a.length != c.length + 1) {
            throw new DimensionMismatchException(PatriusMessages.ARRAY_SIZES_SHOULD_HAVE_DIFFERENCE_1,
                a.length, c.length);
        }
    }

}
