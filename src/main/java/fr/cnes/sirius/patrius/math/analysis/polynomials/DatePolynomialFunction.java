/**
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
 * 
 * @history 29/04/2015
 *
 * HISTORY
 * VERSION:4.13:DM:DM-119:08/12/2023:[PATRIUS] Ajout d'une methode copy(AbsoluteDate)
 * à  l'interface DatePolynomialFunctionInterface
 * VERSION:4.13:DM:DM-103:08/12/2023:[PATRIUS] Optimisation du CIRFProvider
 * VERSION:4.11.1:DM:DM-88:30/06/2023:[PATRIUS] Complement FT 3319
 * VERSION:4.11:DM:DM-3319:22/05/2023:[PATRIUS] QuaternionPolynomialSegment plus generique et coherent
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.7:FA:FA-2862:18/05/2021:Bug lors de la propagation d'une orbite analytique, en multithread 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:266:29/04/2015:add various centered analytical models
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.polynomials;

import fr.cnes.sirius.patrius.math.util.ArithmeticUtils;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

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
public class DatePolynomialFunction implements DatePolynomialFunctionInterface {

    /** Serializable UID. */
    private static final long serialVersionUID = 6399329112266195043L;

    /** Origin date for the polynomial development. */
    private final AbsoluteDate originDate;

    /** Underlying polynomial function */
    private final PolynomialFunction fct;

    /**
     * Time factor used to transform a time as double into an absoluteDate and vice-versa:
     * <ul>
     * <li>The absolute date corresponding to a given time as double is equal to the origin date shifted by the time
     * multiplied by this time factor.</li>
     * <li>The time as double corresponding to a given absolute date is equal to the duration between originDate and the
     * date, divided by this time factor.</li>
     * </ul>
     * If <code>null</code>, a unit time factor (value 1.0) is used.
     */
    private final Double timeFactor;

    /**
     * Constructor using real time (unreduced time).
     *
     * @param origin the origin date for the polynomial development
     * @param polyFunction the polynomial function
     */
    public DatePolynomialFunction(final AbsoluteDate origin, final PolynomialFunction polyFunction) {
        // Call the main constructor with a null time factor
        this(origin, null, polyFunction);
    }

    /**
     * Main constructor.
     *
     * @param origin the origin date for the polynomial development
     * @param timeFactorIn the time Factor used to transform a time as double into an absoluteDate and vice-versa:
     *        <ul>
     *        <li>The absolute date corresponding to a given time as double is equal to the origin date shifted by the
     *        time multiplied by this time factor.</li>
     *        <li>The time as double corresponding to a given absolute date is equal to the duration between originDate
     *        and the date, divided by this time factor.</li>
     *        </ul>
     *        If <code>null</code>, a unit time factor (value 1.0) is used.
     * @param polyFunction the polynomial function
     */
    public DatePolynomialFunction(final AbsoluteDate origin, final Double timeFactorIn,
                                  final PolynomialFunction polyFunction) {
        // Set the origin date
        this.originDate = origin;
        // Set the polynomial function
        this.fct = polyFunction;
        // Set the time factor
        this.timeFactor = timeFactorIn;

    }

    /**
     * Returns the model origin date.
     * 
     * @return the model origin date
     */
    @Override
    public AbsoluteDate getT0() {
        // Return the model origin date
        return this.originDate;
    }

    /** {@inheritDoc} */
    @Override
    public double value(final AbsoluteDate date) {
        // Return the value
        return this.fct.value(dateToDouble(date));
    }

    /** {@inheritDoc} */
    @Override
    public DatePolynomialFunction derivative() {
        // Call temporary coefficients
        final double[] derivativeCoef = this.fct.derivative().getCoefficients();
        if (this.timeFactor != null) {
            // Modify coefficients due to use of reduced time
            for (int iDegree = 0; iDegree < derivativeCoef.length; iDegree++) {
                // apply multiplying factor (unreducedDuration) due to use of reduced times
                derivativeCoef[iDegree] /= this.timeFactor;
            }
        }

        // Return the date polynomial function
        return new DatePolynomialFunction(this.originDate, this.timeFactor, new PolynomialFunction(derivativeCoef));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getDegree() {
        // Return the degree
        return this.fct.getDegree();
    }

    /** {@inheritDoc} */
    @Override
    public double[] getCoefficients() {
        // Return the coefficients
        return this.fct.getCoefficients();
    }

    /** {@inheritDoc} */
    @Override
    public Double getTimeFactor() {
        // Return the time factor
        return this.timeFactor;
    }

    /** {@inheritDoc} */
    @Override
    public DatePolynomialFunction primitive(final AbsoluteDate date0, final double value0) {
        // Call primitive function
        final PolynomialFunction primitive = this.fct.primitive(dateToDouble(date0), value0);
        final double[] primitiveCoeff = primitive.getCoefficients();
        if (this.timeFactor != null) {
            // Modify coefficients due to use of reduced time
            double powerT0 = 1.0;
            primitiveCoeff[0] = value0;
            for (int iDegree = 1; iDegree < primitiveCoeff.length; iDegree++) {
                // Apply multiplying factor (unreducedDuration) due to use of reduced times
                primitiveCoeff[iDegree] *= this.timeFactor;
                // Deal with constant final term
                powerT0 = powerT0 * dateToDouble(date0);
                primitiveCoeff[0] = primitiveCoeff[0] - (primitiveCoeff[iDegree] * powerT0);
            }
        }

        // Return the primitive
        return new DatePolynomialFunction(this.originDate, this.timeFactor, new PolynomialFunction(primitiveCoeff));
    }

    /**
     * {@inheritDoc}
     * 
     * @throws IllegalArgumentException
     *         if the time factor is enabled and the new origin date is not strictly anterior to the current origin date
     *         shifted by the timeFactor
     */
    @Override
    public DatePolynomialFunction copy(final AbsoluteDate newOriginDate) {

        // Check if, when the time factor is enabled, the new origin date is strictly anterior to the origin date of
        // this shifted by the timeFactor, otherwise raise an exception
        if ((this.timeFactor != null)
                && (newOriginDate.durationFrom(this.originDate.shiftedBy(this.timeFactor)) >= 0.)) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.DATE_POLYNOMIAL_COPY_INVALID_DATE);
        }

        // Time shift of the origin date
        final double dt = newOriginDate.durationFrom(getT0());

        // Time factor for the cloned function
        Double newTimeFactor = null;

        // Extract the origin coefficients
        final double[] coeff = getCoefficients();

        // Declare the coefficients for cloned function
        final double[] newCoeff;

        if (Double.compare(dt, 0.) == 0.) {
            // The origin date shift is not significant
            // Output timeFactor and coefficients are equal to those of this
            newTimeFactor = this.timeFactor;
            newCoeff = coeff;

        } else if (this.timeFactor == null) {
            // Real time is used: newTimeFactor remains null

            // The coefficients can simply be shifted
            newCoeff = PolynomialsUtils.shift(coeff, dt);

        } else {
            // Reduced time is used
            newTimeFactor = this.timeFactor - dt;

            newCoeff = new double[coeff.length];
            // Loop over all coefficients to shift them while incorporating the original and new time factors
            for (int k = 0; k < coeff.length; k++) {

                // Coefficient initialized to 0
                newCoeff[k] = 0.;
                for (int i = k; i < newCoeff.length; i++) {
                    newCoeff[k] += ((((coeff[i] / MathLib.pow(this.timeFactor, i)) * ArithmeticUtils.factorial(i))
                            / ArithmeticUtils.factorial(k) / ArithmeticUtils.factorial(i - k)) * MathLib.pow(dt, i - k))
                            * MathLib.pow(newTimeFactor, k);
                }
            }
        }

        return new DatePolynomialFunction(newOriginDate, newTimeFactor, new PolynomialFunction(newCoeff));
    }

    /** {@inheritDoc} */
    @Override
    public PolynomialType getPolynomialType() {
        return PolynomialType.CLASSICAL;
    }
}
