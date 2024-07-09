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
package fr.cnes.sirius.patrius.math.util;

import fr.cnes.sirius.patrius.math.exception.ConvergenceException;
import fr.cnes.sirius.patrius.math.exception.MaxCountExceededException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

//CHECKSTYLE: stop CommentRatio check
//Reason: model - Commons-Math code kept as such

/**
 * Provides a generic means to evaluate continued fractions. Subclasses simply
 * provided the a and b coefficients to evaluate the continued fraction.
 * 
 * <p>
 * References:
 * <ul>
 * <li><a href="http://mathworld.wolfram.com/ContinuedFraction.html"> Continued Fraction</a></li>
 * </ul>
 * </p>
 * 
 * @version $Id: ContinuedFraction.java 18108 2017-10-04 06:45:27Z bignon $
 */
//CHECKSTYLE: stop AbstractClassName check
@SuppressWarnings("PMD.AbstractNaming")
public abstract class ContinuedFraction {
    // CHECKSTYLE: resume AbstractClassName check

    /** Maximum allowed numerical error. */
    private static final double DEFAULT_EPSILON = 10e-9;

    /**
     * Default constructor.
     */
    protected ContinuedFraction() {
        super();
    }

    /**
     * Access the n-th a coefficient of the continued fraction. Since a can be
     * a function of the evaluation point, x, that is passed in as well.
     * 
     * @param n
     *        the coefficient index to retrieve.
     * @param x
     *        the evaluation point.
     * @return the n-th a coefficient.
     */
    protected abstract double getA(int n, double x);

    /**
     * Access the n-th b coefficient of the continued fraction. Since b can be
     * a function of the evaluation point, x, that is passed in as well.
     * 
     * @param n
     *        the coefficient index to retrieve.
     * @param x
     *        the evaluation point.
     * @return the n-th b coefficient.
     */
    protected abstract double getB(int n, double x);

    /**
     * Evaluates the continued fraction at the value x.
     * 
     * @param x
     *        the evaluation point.
     * @return the value of the continued fraction evaluated at x.
     * @throws ConvergenceException
     *         if the algorithm fails to converge.
     */
    public double evaluate(final double x) {
        return this.evaluate(x, DEFAULT_EPSILON, Integer.MAX_VALUE);
    }

    /**
     * Evaluates the continued fraction at the value x.
     * 
     * @param x
     *        the evaluation point.
     * @param epsilon
     *        maximum error allowed.
     * @return the value of the continued fraction evaluated at x.
     * @throws ConvergenceException
     *         if the algorithm fails to converge.
     */
    public double evaluate(final double x, final double epsilon) {
        return this.evaluate(x, epsilon, Integer.MAX_VALUE);
    }

    /**
     * Evaluates the continued fraction at the value x.
     * 
     * @param x
     *        the evaluation point.
     * @param maxIterations
     *        maximum number of convergents
     * @return the value of the continued fraction evaluated at x.
     * @throws ConvergenceException
     *         if the algorithm fails to converge.
     * @throws MaxCountExceededException
     *         if maximal number of iterations is reached
     */
    public double evaluate(final double x, final int maxIterations) {
        return this.evaluate(x, DEFAULT_EPSILON, maxIterations);
    }

    /**
     * Evaluates the continued fraction at the value x.
     * <p>
     * The implementation of this method is based on the modified Lentz algorithm as described on page 18 ff. in:
     * <ul>
     * <li>
     * I. J. Thompson, A. R. Barnett. "Coulomb and Bessel Functions of Complex Arguments and Order." <a target="_blank"
     * href="http://www.fresco.org.uk/papers/Thompson-JCP64p490.pdf">
     * http://www.fresco.org.uk/papers/Thompson-JCP64p490.pdf</a></li>
     * </ul>
     * <b>Note:</b> the implementation uses the terms a<sub>i</sub> and b<sub>i</sub> as defined in <a
     * href="http://mathworld.wolfram.com/ContinuedFraction.html">Continued Fraction @ MathWorld</a>.
     * </p>
     * 
     * @param x
     *        the evaluation point.
     * @param epsilon
     *        maximum error allowed.
     * @param maxIterations
     *        maximum number of convergents
     * @return the value of the continued fraction evaluated at x.
     * @throws ConvergenceException
     *         if the algorithm fails to converge.
     * @throws MaxCountExceededException
     *         if maximal number of iterations is reached
     */
    public double evaluate(final double x, final double epsilon, final int maxIterations) {
        final double small = 1e-50;
        double hPrev = this.getA(0, x);

        // use the value of small as epsilon criteria for zero checks
        if (Precision.equals(hPrev, 0.0, small)) {
            hPrev = small;
        }

        int n = 1;
        double dPrev = 0.0;
        double cPrev = hPrev;
        double hN = hPrev;

        while (n < maxIterations) {
            final double a = this.getA(n, x);
            final double b = this.getB(n, x);

            double dN = a + b * dPrev;
            if (Precision.equals(dN, 0.0, small)) {
                dN = small;
            }
            double cN = a + b / cPrev;
            if (Precision.equals(cN, 0.0, small)) {
                cN = small;
            }

            dN = 1 / dN;
            final double deltaN = cN * dN;
            hN = hPrev * deltaN;

            if (Double.isInfinite(hN)) {
                throw new ConvergenceException(PatriusMessages.CONTINUED_FRACTION_INFINITY_DIVERGENCE,
                    x);
            }
            if (Double.isNaN(hN)) {
                throw new ConvergenceException(PatriusMessages.CONTINUED_FRACTION_NAN_DIVERGENCE,
                    x);
            }

            if (MathLib.abs(deltaN - 1.0) < epsilon) {
                break;
            }

            dPrev = dN;
            cPrev = cN;
            hPrev = hN;
            n++;
        }

        if (n >= maxIterations) {
            throw new MaxCountExceededException(PatriusMessages.NON_CONVERGENT_CONTINUED_FRACTION,
                maxIterations, x);
        }

        return hN;
    }

    // CHECKSTYLE: resume CommentRatio check
}
