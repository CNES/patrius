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
 * VERSION:4.11.1:DM:DM-88:30/06/2023:[PATRIUS] Complement FT 3319
 * VERSION:4.11:DM:DM-3319:22/05/2023:[PATRIUS] QuaternionPolynomialSegment plus generique et coherent
 * VERSION:4.10.2:FA:FA-3290:31/01/2023:[PATRIUS] Erreur getChebyshevAbscissas
 * VERSION:4.10.1:FA:FA-3263:02/12/2022:[PATRIUS] Implementation incorrecte de la classe DatePolynomialChebyshevFunction
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.10:DM:DM-3204:03/11/2022:[PATRIUS] Evolutions autour des polynômes de Chebyshev
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.polynomials;

import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;

/**
 * This class represents a Chebyshev polynomial function of date.
 * <p>
 * The real time (unreduced time) is used.
 * </p>
 *
 * @concurrency thread-safe
 *
 * @author Alice Latourte
 *
 * @since 4.10
 */
public class DatePolynomialChebyshevFunction implements DatePolynomialFunctionInterface {

    /** Serializable UID. */
    private static final long serialVersionUID = 6399329112266195043L;

    /** Origin date for the polynomial development. */
    private final AbsoluteDate originDate;

    /** Underlying polynomial Chebyshev function. */
    private final PolynomialChebyshevFunction fct;

    /**
     * Constructor.
     *
     * @param originDate
     *        the polynomial origin date
     * @param tStart
     *        the Chebyshev polynomial range start date
     * @param tEnd
     *        the Chebyshev polynomial range end date
     * @param polynomialCoefs
     *        the polynomial development coefficients
     */
    public DatePolynomialChebyshevFunction(final AbsoluteDate originDate, final AbsoluteDate tStart,
                                           final AbsoluteDate tEnd, final double[] polynomialCoefs) {
        // Compute the attributes of this class
        this(originDate, new PolynomialChebyshevFunction(tStart.durationFrom(originDate),
            tEnd.durationFrom(originDate), polynomialCoefs));
    }

    /**
     * Constructor.
     *
     * @param originDate
     *        the polynomial origin date
     * @param polyFunction
     *        the polynomial function
     */
    public DatePolynomialChebyshevFunction(final AbsoluteDate originDate,
                                           final PolynomialChebyshevFunction polyFunction) {
        // Compute the attributes of this class
        this.originDate = originDate;
        this.fct = polyFunction;
    }

    /**
     * Copies this function and returns a new one identical to this.
     *
     * @return new DatePolynomialChebyshevFunction identical to this
     */
    public DatePolynomialChebyshevFunction copy() {
        return new DatePolynomialChebyshevFunction(this.getT0(), this.getStart(), this.getEnd(),
            this.getCoefficients());
    }

    /**
     * Getter for the Chebyshev polynomial origin date.
     * 
     * @return the Chebyshev polynomial origin date
     */
    @Override
    public AbsoluteDate getT0() {
        return this.originDate;
    }

    /**
     * Getter for the Chebyshev polynomial range start date.
     * 
     * @return the Chebyshev polynomial range start date
     */
    public AbsoluteDate getStart() {
        return this.originDate.shiftedBy(this.fct.getStart());
    }

    /**
     * Getter for the Chebyshev polynomial range end date.
     * 
     * @return the Chebyshev polynomial range end date
     */
    public AbsoluteDate getEnd() {
        return this.originDate.shiftedBy(this.fct.getEnd());
    }

    /**
     * Getter for the Chebyshev polynomial range.
     * 
     * @return the Chebyshev polynomial range
     */
    public AbsoluteDateInterval getRange() {
        return new AbsoluteDateInterval(this.originDate.shiftedBy(this.fct.getStart()),
            this.originDate.shiftedBy(this.fct.getEnd()));
    }

    /**
     * Getter for polynomial order.
     *
     * @return the polynomial order
     */
    public int getOrder() {
        return getCoefficients().length - 1;
    }

    /**
     * Compute the N Chebyshev abscissas on the range [start ; end] in a chronological (increasing) order.
     * 
     * @param n
     *        Number of points to evaluate
     * @return the N Chebyshev abscissas
     * @throws NotStrictlyPositiveException
     *         if {@code n <= 0}
     */
    public AbsoluteDate[] getChebyshevAbscissas(final int n) {
        // Check input consistency
        if (n <= 0) {
            throw new NotStrictlyPositiveException(n);
        }

        // Compute the array of abscissas
        final double[] abscissas = PolynomialsUtils.getChebyshevAbscissas(this.fct.getStart(), this.fct.getEnd(), n);

        // Evaluate the function at the n points required
        final AbsoluteDate[] dateAbscissas = new AbsoluteDate[n];
        for (int i = 0; i < n; i++) {
            // Fill in the abscissas array in a chronological (increasing) order
            dateAbscissas[i] = new AbsoluteDate(this.getT0(), abscissas[i]);
        }

        // Return result
        return dateAbscissas;
    }

    /** {@inheritDoc} */
    @Override
    public double value(final AbsoluteDate date) {
        // Return value
        return this.fct.value(date.durationFrom(this.originDate));
    }

    /** {@inheritDoc} */
    @Override
    public int getDegree() {
        // Return degree
        return this.fct.getDegree();
    }

    /** {@inheritDoc} */
    @Override
    public double[] getCoefficients() {
        // Return coefficients
        return this.fct.getCoefficients();
    }

    /** {@inheritDoc} */
    @Override
    public DatePolynomialChebyshevFunction derivative() {
        // Return polynomial derivative
        return new DatePolynomialChebyshevFunction(this.originDate, this.fct.derivative());
    }

    /** {@inheritDoc} */
    @Override
    public DatePolynomialChebyshevFunction primitive(final AbsoluteDate date0, final double value0) {
        // Return primitive
        return new DatePolynomialChebyshevFunction(this.originDate, this.fct.primitive(dateToDouble(date0), value0));
    }

    /** {@inheritDoc} */
    @Override
    public final double dateToDouble(final AbsoluteDate date) {
        // Return double
        return date.durationFrom(getT0());
    }

    /** {@inheritDoc} */
    @Override
    public final AbsoluteDate doubleToDate(final double time) {
        // Return date
        return getT0().shiftedBy(time);
    }

    /** {@inheritDoc} */
    @Override
    public Double getTimeFactor() {
        // Return null because the real time is used
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public PolynomialType getPolynomialType() {
        return PolynomialType.CHEBYSHEV;
    }

    /**
     * Getter for the start range of the underlying polynomial Chebyshev function.
     * 
     * @return the start range of the underlying polynomial Chebyshev function
     */
    public double getStartAsDouble() {
        return this.fct.getStart();
    }

    /**
     * Getter for the end range of the underlying polynomial Chebyshev function.
     * 
     * @return the end range of the underlying polynomial Chebyshev function
     */
    public double getEndAsDouble() {
        return this.fct.getEnd();
    }
}
