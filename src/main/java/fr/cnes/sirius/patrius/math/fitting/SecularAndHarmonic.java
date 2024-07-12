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
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.fitting;

import fr.cnes.sirius.patrius.math.analysis.ParametricUnivariateFunction;
import fr.cnes.sirius.patrius.math.optim.nonlinear.vector.jacobian.LevenbergMarquardtOptimizer;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.time.AbsoluteDate;

/**
 * Class for fitting evolution of osculating orbital parameters.
 * <p>
 * This class allows conversion from osculating parameters to mean parameters.
 * </p>
 * 
 * @author Luc Maisonobe
 */
public class SecularAndHarmonic {

    /** Degree of polynomial secular part. */
    private final int secularDegree;

    /** Pulsations of harmonic part. */
    private final double[] pulsations;

    /** Curve fitting engine. */
    private CurveFitter fitter;

    /** Reference date for the model. */
    private AbsoluteDate reference;

    /** Fitted parameters. */
    private double[] fitted;

    /**
     * Simple constructor.
     * 
     * @param secularDegreeIn
     *        degree of polynomial secular part
     * @param pulsationsIn
     *        pulsations of harmonic part
     */
    public SecularAndHarmonic(final int secularDegreeIn, final double... pulsationsIn) {
        this.secularDegree = secularDegreeIn;
        this.pulsations = pulsationsIn.clone();
    }

    /**
     * Reset fitting.
     * 
     * @param date
     *        reference date
     * @param initialGuess
     *        initial guess for the parameters
     * @see #getReferenceDate()
     */
    public void resetFitting(final AbsoluteDate date, final double... initialGuess) {
        this.fitter = new CurveFitter(new LevenbergMarquardtOptimizer());
        this.reference = date;
        this.fitted = initialGuess.clone();
    }

    /**
     * Add a fitting point.
     * 
     * @param date
     *        date of the point
     * @param osculatingValue
     *        osculating value
     */
    public void addPoint(final AbsoluteDate date, final double osculatingValue) {
        this.fitter.addObservedPoint(date.durationFrom(this.reference), osculatingValue);
    }

    /**
     * Get the reference date.
     * 
     * @return reference date
     * @see #resetFitting(AbsoluteDate, double...)
     */
    public AbsoluteDate getReferenceDate() {
        return this.reference;
    }

    /**
     * Get an upper bound of the fitted harmonic amplitude.
     * 
     * @return upper bound of the fitted harmonic amplitude
     */
    public double getHarmonicAmplitude() {
        double amplitude = 0;
        for (int i = 0; i < this.pulsations.length; ++i) {
            amplitude += MathLib.hypot(this.fitted[this.secularDegree + 2 * i + 1],
                this.fitted[this.secularDegree + 2 * i + 2]);
        }
        return amplitude;
    }

    /**
     * Fit parameters.
     * 
     * @see #getFittedParameters()
     */
    public void fit() {

        this.fitted = this.fitter.fit(new ParametricUnivariateFunction(){

            /** {@inheritDoc} */
            @Override
            public double value(final double x, final double... parameters) {
                return SecularAndHarmonic.this.truncatedValue(SecularAndHarmonic.this.secularDegree,
                    SecularAndHarmonic.this.pulsations.length, x, parameters);
            }

            /** {@inheritDoc} */
            @Override
            public double[] gradient(final double x, final double... parameters) {
                // Initialize gradient
                final double[] gradient =
                    new double[SecularAndHarmonic.this.secularDegree + 1 + 2
                        * SecularAndHarmonic.this.pulsations.length];

                // secular part
                double xN = 1.0;
                for (int i = 0; i <= SecularAndHarmonic.this.secularDegree; ++i) {
                    gradient[i] = xN;
                    xN *= x;
                }

                // harmonic part
                for (int i = 0; i < SecularAndHarmonic.this.pulsations.length; ++i) {
                    gradient[SecularAndHarmonic.this.secularDegree + 2 * i + 1] =
                        MathLib.cos(SecularAndHarmonic.this.pulsations[i] * x);
                    gradient[SecularAndHarmonic.this.secularDegree + 2 * i + 2] =
                        MathLib.sin(SecularAndHarmonic.this.pulsations[i] * x);
                }

                // return computed gradient
                return gradient;
            }

        }, this.fitted);

    }

    /**
     * Get a copy of the last fitted parameters.
     * 
     * @return copy of the last fitted parameters.
     * @see #fit()
     */
    public double[] getFittedParameters() {
        return this.fitted.clone();
    }

    /**
     * Get fitted osculating value.
     * 
     * @param date
     *        current date
     * @return osculating value at current date
     */
    public double osculatingValue(final AbsoluteDate date) {
        return this.truncatedValue(this.secularDegree, this.pulsations.length,
            date.durationFrom(this.reference), this.fitted);
    }

    /**
     * Get fitted osculating derivative.
     * 
     * @param date
     *        current date
     * @return osculating derivative at current date
     */
    public double osculatingDerivative(final AbsoluteDate date) {
        return this.truncatedDerivative(this.secularDegree, this.pulsations.length,
            date.durationFrom(this.reference), this.fitted);
    }

    /**
     * Get fitted osculating second derivative.
     * 
     * @param date
     *        current date
     * @return osculating second derivative at current date
     */
    public double osculatingSecondDerivative(final AbsoluteDate date) {
        return this.truncatedSecondDerivative(this.secularDegree, this.pulsations.length,
            date.durationFrom(this.reference), this.fitted);
    }

    /**
     * Get mean value, truncated to first components.
     * 
     * @param date
     *        current date
     * @param degree
     *        degree of polynomial secular part to consider
     * @param harmonics
     *        number of harmonics terms to consider
     * @return mean value at current date
     */
    public double meanValue(final AbsoluteDate date, final int degree, final int harmonics) {
        return this.truncatedValue(degree, harmonics, date.durationFrom(this.reference), this.fitted);
    }

    /**
     * Get mean derivative, truncated to first components.
     * 
     * @param date
     *        current date
     * @param degree
     *        degree of polynomial secular part to consider
     * @param harmonics
     *        number of harmonics terms to consider
     * @return mean derivative at current date
     */
    public double meanDerivative(final AbsoluteDate date, final int degree, final int harmonics) {
        return this.truncatedDerivative(degree, harmonics, date.durationFrom(this.reference), this.fitted);
    }

    /**
     * Approximate an already fitted model to polynomial only terms.
     * <p>
     * This method is mainly used in order to combine the large amplitude long periods with the secular part as a new
     * approximate polynomial model over some time range. This should be used rather than simply extracting the
     * polynomial coefficients from {@link #getFittedParameters()} when some periodic terms amplitudes are large (for
     * example Sun resonance effects on local solar time in sun synchronous orbits). In theses cases, the pure
     * polynomial secular part in the coefficients may be far from the mean model.
     * </p>
     * 
     * @param combinedDegree
     *        desired degree for the combined polynomial
     * @param combinedReference
     *        desired reference date for the combined polynomial
     * @param meanDegree
     *        degree of polynomial secular part to consider
     * @param meanHarmonics
     *        number of harmonics terms to consider
     * @param start
     *        start date of the approximation time range
     * @param end
     *        end date of the approximation time range
     * @param step
     *        sampling step
     * @return coefficients of the approximate polynomial (in increasing degree order),
     *         using the user provided reference date
     */
    public double[] approximateAsPolynomialOnly(final int combinedDegree, final AbsoluteDate combinedReference,
                                                final int meanDegree, final int meanHarmonics,
                                                final AbsoluteDate start, final AbsoluteDate end,
                                                final double step) {
        final PolynomialFitter sfitter = new PolynomialFitter(new LevenbergMarquardtOptimizer());
        for (AbsoluteDate date = start; date.compareTo(end) < 0; date = date.shiftedBy(step)) {
            this.fitter.addObservedPoint(date.durationFrom(combinedReference),
                this.meanValue(date, meanDegree, meanHarmonics));
        }
        return sfitter.fit(this.fitted);
    }

    /**
     * Get mean second derivative, truncated to first components.
     * 
     * @param date
     *        current date
     * @param degree
     *        degree of polynomial secular part
     * @param harmonics
     *        number of harmonics terms to consider
     * @return mean second derivative at current date
     */
    public double meanSecondDerivative(final AbsoluteDate date, final int degree, final int harmonics) {
        return this.truncatedSecondDerivative(degree, harmonics, date.durationFrom(this.reference), this.fitted);
    }

    /**
     * Get value truncated to first components.
     * 
     * @param degree
     *        degree of polynomial secular part
     * @param harmonics
     *        number of harmonics terms to consider
     * @param time
     *        time parameter
     * @param parameters
     *        models parameters (must include all parameters,
     *        including the ones ignored due to model truncation)
     * @return truncated value
     */
    private double truncatedValue(final int degree, final int harmonics,
                                  final double time, final double... parameters) {

        // Initialize truncated value
        double value = 0;

        // secular part
        double tN = 1.0;
        for (int i = 0; i <= degree; ++i) {
            value += parameters[i] * tN;
            tN *= time;
        }

        // harmonic part
        for (int i = 0; i < harmonics; ++i) {
            final double[] sincos = MathLib.sinAndCos(this.pulsations[i] * time);
            final double sin = sincos[0];
            final double cos = sincos[1];
            value += parameters[this.secularDegree + 2 * i + 1] * cos +
                parameters[this.secularDegree + 2 * i + 2] * sin;
        }

        return value;

    }

    /**
     * Get derivative truncated to first components.
     * 
     * @param degree
     *        degree of polynomial secular part
     * @param harmonics
     *        number of harmonics terms to consider
     * @param time
     *        time parameter
     * @param parameters
     *        models parameters (must include all parameters,
     *        including the ones ignored due to model truncation)
     * @return truncated derivative
     */
    private double truncatedDerivative(final int degree, final int harmonics,
                                       final double time, final double... parameters) {

        // Initialize truncated derivative value
        double derivative = 0;

        // secular part
        double tN = 1.0;
        for (int i = 1; i <= degree; ++i) {
            derivative += i * parameters[i] * tN;
            tN *= time;
        }

        // harmonic part
        for (int i = 0; i < harmonics; ++i) {
            final double[] sincos = MathLib.sinAndCos(this.pulsations[i] * time);
            final double sin = sincos[0];
            final double cos = sincos[1];
            derivative +=
                this.pulsations[i]
                    * (-parameters[this.secularDegree + 2 * i + 1] * sin +
                    parameters[this.secularDegree + 2 * i + 2] * cos);
        }

        return derivative;

    }

    /**
     * Get second derivative truncated to first components.
     * 
     * @param degree
     *        degree of polynomial secular part
     * @param harmonics
     *        number of harmonics terms to consider
     * @param time
     *        time parameter
     * @param parameters
     *        models parameters (must include all parameters,
     *        including the ones ignored due to model truncation)
     * @return truncated second derivative
     */
    private double truncatedSecondDerivative(final int degree, final int harmonics,
                                             final double time, final double... parameters) {

        // Initialize truncated second derivative value
        double d2 = 0;

        // secular part
        double tN = 1.0;
        for (int i = 2; i <= degree; ++i) {
            d2 += (i - 1) * i * parameters[i] * tN;
            tN *= time;
        }

        // harmonic part
        for (int i = 0; i < harmonics; ++i) {
            final double[] sincos = MathLib.sinAndCos(this.pulsations[i] * time);
            final double sin = sincos[0];
            final double cos = sincos[1];
            d2 += -this.pulsations[i] * this.pulsations[i] *
                (parameters[this.secularDegree + 2 * i + 1] * cos +
                parameters[this.secularDegree + 2 * i + 2] * sin);
        }

        return d2;

    }

}
