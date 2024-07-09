/**
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
 * 
 * @history 29/04/2015
 * HISTORY
* VERSION:4.7:FA:FA-2862:18/05/2021:Bug lors de la propagation d'une orbite analytique, en multithread 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:266:29/04/2015:add various centered analytical models
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.analytical.twod;

import fr.cnes.sirius.patrius.time.AbsoluteDate;

/**
 * This class represents a polynomial function of date.
 * 
 * @concurrency thread-safe
 * 
 * @author Emmanuel Bignon
 * 
 * @version $Id: DatePolynomialFunction.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 3.0
 */
public class DatePolynomialFunction implements UnivariateDateFunction {

    /** Serial UID. */
    private static final long serialVersionUID = 6399329112266195043L;

    /** Origin date for the polynomial development. */
    private final AbsoluteDate originDate;

    /** Polynomial development coefficients. */
    private final double[] polynomialCoefficients;

    /**
     * Constructor.
     * 
     * @param origin
     *        origin date for the polynomial development
     * @param polynomialCoefs
     *        polynomial development coefficients
     */
    public DatePolynomialFunction(final AbsoluteDate origin, final double[] polynomialCoefs) {
        this.originDate = origin;
        this.polynomialCoefficients = polynomialCoefs;

        if (polynomialCoefs.length == 0) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Clone constructor.
     * 
     * @param function
     *        function to copy
     */
    public DatePolynomialFunction(final DatePolynomialFunction function) {
        // Date copy
        this.originDate = new AbsoluteDate(function.getT0(), 0.);
        // Polynomial coefficients copy
        this.polynomialCoefficients = function.getCoefPoly();
    }

    /**
     * Returns the model origin date.
     * 
     * @return the model origin date
     */
    public AbsoluteDate getT0() {
        return this.originDate;
    }

    /**
     * Returns a copy of polynomial development coefficients.
     * 
     * @return the polynomial development coefficients
     */
    public double[] getCoefPoly() {
        return this.polynomialCoefficients.clone();
    }

    /**
     * Returns polynomial order.
     * 
     * @return polynomial order
     */
    public int getPolyOrder() {
        return this.polynomialCoefficients.length - 1;
    }

    /** {@inheritDoc} */
    @Override
    public double value(final AbsoluteDate date) {

        // Compute powers of dt
        final double[] dtp = this.computeDtPowers(date, this.originDate, this.polynomialCoefficients.length - 1);

        // Compute value
        double res = 0;
        for (int i = 0; i < this.polynomialCoefficients.length; i++) {
            res += this.polynomialCoefficients[i] * dtp[i];
        }

        return res;
    }

    /**
     * Builds an array of powers of dt = date.durationFrom(origin).
     * 
     * @param date
     *        date
     * @param origin
     *        origin of dates
     * @param order
     *        highest power required
     * @return array of consecutive powered dt
     */
    private double[] computeDtPowers(final AbsoluteDate date, final AbsoluteDate origin, final int order) {

        final double dt = date.durationFrom(origin);

        // Fast dt^i array computation
        final double[] dtp = new double[order + 1];
        dtp[0] = 1.;
        for (int i = 1; i < order + 1; i++) {
            dtp[i] = dtp[i - 1] * dt;
        }

        return dtp;
    }
}
