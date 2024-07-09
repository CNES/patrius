/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 *
 * Copyright 2002-2012 CS Systèmes d'Information
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
 *
 */
/*
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:473:24/09/2015: reduce the computation time on Hermite interpolation
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.interpolation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import fr.cnes.sirius.patrius.math.analysis.differentiation.DerivativeStructure;
import fr.cnes.sirius.patrius.math.analysis.differentiation.UnivariateDifferentiableVectorFunction;
import fr.cnes.sirius.patrius.math.analysis.polynomials.PolynomialFunction;
import fr.cnes.sirius.patrius.math.exception.NoDataException;
import fr.cnes.sirius.patrius.math.util.ArithmeticUtils;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Polynomial interpolator using both sample values and sample derivatives.
 * <p>
 * WARNING: this class is <em>not</em> expected to remain in Orekit. It is provided by version 3.1 of Apache Commons
 * Math. However, since as of writing (June 2012) this version is not released yet, Orekit depends on the latest
 * official version 3.0 which does not provides this class. So despite it is implemented as a public class in Orekit so
 * it can be used in from any package, it does not belong to Orekit public API and should not be used at application
 * level. Once version 3.1 of Apache Commons Math is released, this class will be removed from Orekit.
 * </p>
 * <p>
 * The interpolation polynomials match all sample points, including both values and provided derivatives. There is one
 * polynomial for each component of the values vector. All polynomial have the same degree. The degree of the
 * polynomials depends on the number of points and number of derivatives at each point. For example the interpolation
 * polynomials for n sample points without any derivatives all have degree n-1. The interpolation polynomials for n
 * sample points with the two extreme points having value and first derivative and the remaining points having value
 * only all have degree n+1. The interpolation polynomial for n sample points with value, first and second derivative
 * for all points all have degree 3n-1.
 * </p>
 * 
 * @author Luc Maisonobe
 */
public class HermiteInterpolator implements UnivariateDifferentiableVectorFunction {

    /** Sample abscissae. */
    private final List<Double> abscissae;

    /** Top diagonal of the divided differences array. */
    private final List<double[]> topDiagonal;

    /** Bottom diagonal of the divided differences array. */
    private final List<double[]> bottomDiagonal;

    /**
     * Create an empty interpolator.
     */
    public HermiteInterpolator() {
        this.abscissae = new ArrayList<Double>();
        this.topDiagonal = new ArrayList<double[]>();
        this.bottomDiagonal = new ArrayList<double[]>();
    }

    /**
     * Add a sample point.
     * <p>
     * This method must be called once for each sample point. It is allowed to mix some calls with values only with
     * calls with values and first derivatives.
     * </p>
     * <p>
     * The point abscissae for all calls <em>must</em> be different.
     * </p>
     * 
     * @param x
     *        abscissa of the sample point
     * @param value
     *        value and derivatives of the sample point
     *        (if only one row is passed, it is the value, if two rows are
     *        passed the first one is the value and the second the derivative
     *        and so on)
     * @exception IllegalArgumentException
     *            if the abscissa is equals to a previously
     *            added sample point
     */
    public void addSamplePoint(final double x, final double[]... value) {

        // loop on the value length
        for (int i = 0; i < value.length; ++i) {
            // Loop on all values
            final double[] y = new double[value[i].length];
            if (i > 1) {
                final double inv = 1.0 / ArithmeticUtils.factorial(i);
                for (int j = 0; j < y.length; ++j) {
                    // fill y values with the inverse of values array
                    y[j] = value[i][j] * inv;
                }
            } else {
                for (int j = 0; j < y.length; ++j) {
                    // fill y values with values array
                    y[j] = value[i][j];
                }
            }

            // update the bottom diagonal of the divided differences array
            final int n = this.abscissae.size();
            this.bottomDiagonal.add(n - i, y);
            // initialize bottom0 with y
            double[] bottom0 = y;
            for (int j = i; j < n; ++j) {
                final double inv = 1.0 / (x - this.abscissae.get(n - (j + 1)));
                if (Double.isInfinite(inv)) {
                    // Exception
                    throw PatriusException.createIllegalArgumentException(PatriusMessages.DUPLICATED_ABSCISSA, x);
                }
                final double[] bottom1 = this.bottomDiagonal.get(n - (j + 1));
                for (int k = 0; k < y.length; ++k) {
                    bottom1[k] = inv * (bottom0[k] - bottom1[k]);
                }
                bottom0 = bottom1;
            }

            // update the top diagonal of the divided differences array
            //
            final double[] bottomTemp = new double[bottom0.length];
            System.arraycopy(bottom0, 0, bottomTemp, 0, bottom0.length);
            this.topDiagonal.add(bottomTemp);

            // update the abscissae array
            this.abscissae.add(x);

        }

    }

    /**
     * Compute the interpolation polynomials.
     * 
     * @return interpolation polynomials array
     * @exception IllegalStateException
     *            if sample is empty
     */
    public PolynomialFunction[] getPolynomials() {

        // safety check
        this.checkInterpolation();

        // iteration initialization
        // Create a polynomial from 0
        final PolynomialFunction zero = this.polynomial(0);
        final PolynomialFunction[] polynomials = new PolynomialFunction[this.topDiagonal.get(0).length];
        for (int i = 0; i < polynomials.length; ++i) {
            polynomials[i] = zero;
        }
        // Create a polynomial from 1
        PolynomialFunction coeff = this.polynomial(1);

        // build the polynomials by iterating on the top diagonal of the divided differences array
        for (int i = 0; i < this.topDiagonal.size(); ++i) {
            final double[] tdi = this.topDiagonal.get(i);
            for (int k = 0; k < polynomials.length; ++k) {
                polynomials[k] = polynomials[k].add(coeff.multiply(this.polynomial(tdi[k])));
            }
            coeff = coeff.multiply(this.polynomial(-this.abscissae.get(i), 1.0));
        }

        return polynomials;

    }

    /**
     * Interpolate value at a specified abscissa.
     * <p>
     * Calling this method is equivalent to call the {@link PolynomialFunction#value(double)
     * value} methods of all polynomials returned by {@link #getPolynomials() getPolynomials}, except it does not build
     * the intermediate polynomials, so this method is faster and numerically more stable.
     * </p>
     * 
     * @param x
     *        interpolation abscissa
     * @return interpolated value
     * @exception IllegalStateException
     *            if sample is empty
     */
    @Override
    public double[] value(final double x) {

        // safety check
        this.checkInterpolation();
        // initialize value
        final double[] value = new double[this.topDiagonal.get(0).length];
        double valueCoeff = 1;
        for (int i = 0; i < this.topDiagonal.size(); ++i) {
            final double[] dividedDifference = this.topDiagonal.get(i);
            for (int k = 0; k < value.length; ++k) {
                // evaluate value[k]
                value[k] += dividedDifference[k] * valueCoeff;
            }
            // evaluate deltaX with
            final double deltaX = x - this.abscissae.get(i);
            valueCoeff *= deltaX;
        }

        return value;

    }

    /**
     * Interpolate first derivative at a specified abscissa.
     * <p>
     * Calling this method is equivalent to call the {@link PolynomialFunction#value(double)
     * value} methods of the derivatives of all polynomials returned by {@link #getPolynomials() getPolynomials}, except
     * it builds neither the intermediate polynomials nor their derivatives, so this method is faster and numerically
     * more stable.
     * </p>
     * 
     * @param x
     *        interpolation abscissa
     * @return interpolated derivative
     * @exception IllegalStateException
     *            if sample is empty
     */
    public double[] derivative(final double x) {

        // safety check
        this.checkInterpolation();

        // Computation
        final double[] derivative = new double[this.topDiagonal.get(0).length];
        // initialize valueCoeff
        double valueCoeff = 1;
        double derivativeCoeff = 0;
        for (int i = 0; i < this.topDiagonal.size(); ++i) {
            final double[] dividedDifference = this.topDiagonal.get(i);
            for (int k = 0; k < derivative.length; ++k) {
                derivative[k] += dividedDifference[k] * derivativeCoeff;
            }
            final double deltaX = x - this.abscissae.get(i);
            derivativeCoeff = valueCoeff + derivativeCoeff * deltaX;
            valueCoeff *= deltaX;
        }

        // Return result
        return derivative;

    }

    /**
     * Interpolate value at a specified abscissa.
     * <p>
     * Calling this method is equivalent to call the {@link PolynomialFunction#value(DerivativeStructure) value} methods
     * of all polynomials returned by {@link #getPolynomials() getPolynomials}, except it does not build the
     * intermediate polynomials, so this method is faster and numerically more stable.
     * </p>
     * 
     * @param x
     *        interpolation abscissa
     * @return interpolated value
     * @exception NoDataException
     *            if sample is empty
     */
    @Override
    public DerivativeStructure[] value(final DerivativeStructure x) {

        // safety check
        this.checkInterpolation();

        // new instance of DerivativeStructure
        final DerivativeStructure[] value = new DerivativeStructure[this.topDiagonal.get(0).length];
        // fill value with x fields
        Arrays.fill(value, x.getField().getZero());
        DerivativeStructure valueCoeff = x.getField().getOne();
        // loop on the top diagonal length
        for (int i = 0; i < this.topDiagonal.size(); ++i) {
            final double[] dividedDifference = this.topDiagonal.get(i);
            for (int k = 0; k < value.length; ++k) {
                value[k] = value[k].add(valueCoeff.multiply(dividedDifference[k]));
            }
            final DerivativeStructure deltaX = x.subtract(this.abscissae.get(i));
            valueCoeff = valueCoeff.multiply(deltaX);
        }

        return value;
    }

    /**
     * Check interpolation can be performed.
     * 
     * @exception IllegalStateException
     *            if interpolation cannot be performed
     *            because sample is empty
     */
    private void checkInterpolation() {
        if (this.abscissae.isEmpty()) {
            throw PatriusException.createIllegalStateException(PatriusMessages.EMPTY_INTERPOLATION_SAMPLE);
        }
    }

    /**
     * Create a polynomial from its coefficients.
     * 
     * @param c
     *        polynomials coefficients
     * @return polynomial
     */
    private PolynomialFunction polynomial(final double... c) {
        return new PolynomialFunction(c);
    }

}
