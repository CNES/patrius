/**
 * HISTORY
 * VERSION:4.10.2:FA:FA-3290:31/01/2023:[PATRIUS] Erreur dans la methode getChebyshevAbscissas dans DatePolynomialChebyshevFunction
 * VERSION:4.10.1:FA:FA-3263:02/12/2022:[PATRIUS] Implementation incorrecte de la classe DatePolynomialChebyshevFunction
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.10:DM:DM-3204:03/11/2022:[PATRIUS] Evolutions autour des polynômes de Chebyshev
 * END-HISTORY
 */
/*
 */
package fr.cnes.sirius.patrius.propagation.analytical.twod;

import fr.cnes.sirius.patrius.math.analysis.polynomials.PolynomialChebyshevFunction;
import fr.cnes.sirius.patrius.math.analysis.polynomials.PolynomialsUtils;
import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;

/**
 * This class represents a Chebyshev polynomial function of date.
 *
 * @concurrency thread-safe
 *
 * @author Alice Latourte
 *
 * @since 4.10
 */
public class DatePolynomialChebyshevFunction implements UnivariateDateFunction {

     /** Serializable UID. */
    private static final long serialVersionUID = 6399329112266195043L;
    /** Origin date for the polynomial development. */
    private final AbsoluteDate originDate;
    /** Underlying polynomial Chebyshev function. */
    private final PolynomialChebyshevFunction fct;

    /**
     * Constructor.
     *
     * @param originDate the polynomial origin date
     * @param tStart the Chebyshev polynomial range start date
     * @param tEnd the Chebyshev polynomial range end date
     * @param polynomialCoefs the polynomial development coefficients
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
     * @param originDate the polynomial origin date
     * @param polyFunction the polynomial function
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
        return new DatePolynomialChebyshevFunction(this.getT0(), this.getStart(), this.getEnd(), this.getCoefPoly());
    }

    /**
     * Returns the Chebyshev polynomial origin date
     * 
     * @return the Chebyshev polynomial origin date
     */
    public AbsoluteDate getT0() {
        return this.originDate;
    }

    /**
     * Returns the Chebyshev polynomial range start date
     * 
     * @return the Chebyshev polynomial range start date
     */
    public AbsoluteDate getStart() {
        return this.originDate.shiftedBy(this.fct.getStart());
    }

    /**
     * Returns the Chebyshev polynomial range end date
     * 
     * @return the Chebyshev polynomial range end date
     */
    public AbsoluteDate getEnd() {
        return this.originDate.shiftedBy(this.fct.getEnd());
    }

    /**
     * Returns the Chebyshev polynomial range
     * 
     * @return the Chebyshev polynomial range
     */
    public AbsoluteDateInterval getRange() {
        return new AbsoluteDateInterval(this.originDate.shiftedBy(this.fct.getStart()),
            this.originDate.shiftedBy(this.fct.getEnd()));
    }

    /**
     * Returns a copy of polynomial development coefficients.
     *
     * @return the polynomial development coefficients
     */
    public double[] getCoefPoly() {
        return this.fct.getCoefficients();
    }

    /**
     * Returns polynomial order.
     *
     * @return the polynomial order
     */
    public int getPolyOrder() {
        return getCoefPoly().length - 1;
    }

    /**
     * Compute the N Chebyshev abscissas on the range [start ; end] in a chronological (increasing) order.
     * 
     * @param n Number of points to evaluate
     * @return the N Chebyshev abscissas
     * @throws NotStrictlyPositiveException if {@code n <= 0}
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

    /**
     * {@inheritDoc}
     *
     */
    @Override
    public double value(final AbsoluteDate date) {
        return this.fct.value(date.durationFrom(this.originDate));
    }

}
