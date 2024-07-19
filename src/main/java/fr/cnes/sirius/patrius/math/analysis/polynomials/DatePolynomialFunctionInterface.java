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
 * HISTORY
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11.1:DM:DM-88:30/06/2023:[PATRIUS] Complement FT 3319
 * VERSION:4.11:DM:DM-3319:22/05/2023:[PATRIUS] Rendre QuaternionPolynomialSegment plus generique et coherent
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.polynomials;

import fr.cnes.sirius.patrius.time.AbsoluteDate;

/**
 * Represents an interface for polynomial functions of date.
 *
 * @author Alice Latourte
 *
 * @version $Id$
 *
 * @since 4.11
 */
public interface DatePolynomialFunctionInterface extends UnivariateDateFunction {

    /**
     * Getter for the origin date.
     * 
     * @return the origin date
     */
    public AbsoluteDate getT0();

    /**
     * Getter for the time as double corresponding to the given {@link AbsoluteDate}.
     *
     * @param date
     *        the given {@link AbsoluteDate}
     * @return the corresponding time as double
     */
    default double dateToDouble(final AbsoluteDate date) {
        // Initialize the output
        final double dateOut;

        // Check if the time factor is null
        if (getTimeFactor() == null) {
            // Compute the output date
            dateOut = date.durationFrom(getT0());
        } else {
            // Compute the output date
            dateOut = date.durationFrom(getT0()) / getTimeFactor();
        }

        // Return the output date
        return dateOut;
    }

    /**
     * Getter for the {@link AbsoluteDate} corresponding to the given time as double.
     *
     * @param time
     *        the given time as double with respect to time origin
     * @return the corresponding {@link AbsoluteDate}
     */
    default AbsoluteDate doubleToDate(final double time) {

        // Initialize the output
        final AbsoluteDate dateOut;

        // Check if the time factor is null
        if (getTimeFactor() == null) {
            // Compute the output date
            dateOut = getT0().shiftedBy(time);
        } else {
            // Compute the output date
            dateOut = getT0().shiftedBy(time * getTimeFactor());
        }

        // Return the output date
        return dateOut;
    }

    /**
     * Getter for the time factor.
     * 
     * @return the time factor (a <code>null</code> value corresponds to a unit time factor)
     */
    public Double getTimeFactor();

    /**
     * Getter for the polynomial degree.
     * 
     * @return the polynomial degree
     */
    public int getDegree();

    /**
     * Getter for the polynomial coefficients.
     * 
     * @return the polynomial coefficients
     */
    public double[] getCoefficients();

    /**
     * Getter for the derivative date polynomial function.
     * 
     * @return the derivative date polynomial function
     */
    public DatePolynomialFunctionInterface derivative();

    /**
     * Getter for the primitive date polynomial function at the given date and for the given function value at
     * abscissa0.
     *
     * @param date0
     *        the date of interest
     * @param ordinate0
     *        the function value at abscissa0
     * @return the primitive date polynomial function at the given date and for the given function value at abscissa0
     */
    public DatePolynomialFunctionInterface primitive(final AbsoluteDate date0, final double ordinate0);

    /**
     * Getter for the type of this polynomial function.
     * 
     * @return the type of this polynomial function
     */
    public PolynomialType getPolynomialType();
}
