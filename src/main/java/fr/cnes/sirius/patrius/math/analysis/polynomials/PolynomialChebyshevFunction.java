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
 * VERSION:4.10:DM:DM-3204:03/11/2022:[PATRIUS] Evolutions autour des polynômes de Chebyshev
 * VERSION:4.9:DM:DM-3171:10/05/2022:[PATRIUS] Clarification de la convention utilisee pour evaluer ... 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-2994:15/11/2021:[PATRIUS] Polynômes de Chebyshev pour l'interpolation et l'approximation
 * de fonctions 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.polynomials;

import java.util.Arrays;
import java.util.Objects;

import fr.cnes.sirius.patrius.math.analysis.ParametricUnivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.exception.NoDataException;
import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Immutable representation of a Chebyshev polynomial with real Chebyshev coefficients.
 * <p>
 * The following methods and algorithms are based on the article <a
 * href="http://numerical.recipes/book/book.html">[NUMERICAL RECIPES]</a>, &sect;5.8
 * (<cite>Chebyshev Approximation</cite>) & &sect;5.9 (<cite>Derivatives or Integrals of a
 * Chebyshev</cite>).
 * </p>
 * 
 * <p>
 * <b>Important notice</b>: two conventions exist for defining Chebyshev polynomials:<br/>
 * <ul>
 * <li>Defining the first coefficient similarly to other coefficients.
 * Chebyshev Polynomial is then &Sigma;c<sub>i</sub>T<sub>i</sub> (for i in [0, n])</li>
 * <li>Adding a 0.5 multiplier to first coefficient.
 * Chebyshev Polynomial is then c<sub>0</sub>/2 + &Sigma;c<sub>i</sub>T<sub>i</sub>  (for i in [1, n])</li>
 * </ul>
 * PATRIUS uses the <b>first</b> convention (convention used in SPICE library).
 * </p>
 * 
 * @author bonitt
 */
public class PolynomialChebyshevFunction implements UnivariateFunction,
        ParametricUnivariateFunction {

    /** Serializable UID. */
    private static final long serialVersionUID = 3585139305036880724L;

    /** Start range. */
    private final double start;

    /** End range. */
    private final double end;

    /**
     * The coefficients of the Chebyshev polynomial, ordered by degree
     * -- i.e., coefficients[0] is the coefficient of T<sub>0</sub> and coefficients[n]
     * is the coefficient of T<sub>n</sub> where n is the degree of the polynomial.
     */
    private final double[] coefficients;

    /**
     * Construct a Chebyshev polynomial with the given coefficients. The first element of the
     * coefficients array is the
     * constant term. Higher degree coefficients follow in sequence. The degree of the resulting
     * polynomial is the index
     * of the last non-null element of the array, or 0 if all elements are null.
     * <p>
     * The constructor makes a copy of the input array and assigns the copy to the coefficients
     * property.
     * </p>
     * <p>
     * The Chebyshev polynomial is an approximation defined on the range [start ; end].
     * </p>
     * 
     * @param start
     *        Start range
     * @param end
     *        End range
     * @param c
     *        Chebyshev polynomial coefficients
     * @throws NullArgumentException
     *         if {@code c} is {@code null}
     * @throws NoDataException
     *         if {@code c} is empty
     * @throws MathIllegalArgumentException
     *         if {@code start >= end}
     */
    public PolynomialChebyshevFunction(final double start, final double end, final double[] c) {
        super();
        // Check input consistency
        MathUtils.checkNotNull(c);
        final int n = c.length;
        if (n == 0) {
            throw new NoDataException(PatriusMessages.EMPTY_POLYNOMIALS_COEFFICIENTS_ARRAY);
        }
        if (start >= end) {
            throw new MathIllegalArgumentException(PatriusMessages.INCORRECT_INTERVAL);
        }

        // Store the data
        this.start = start;
        this.end = end;
        this.coefficients = new double[n];
        System.arraycopy(c, 0, this.coefficients, 0, n);
    }

    /**
     * Getter for the start range.
     * 
     * @return the start range
     */
    public double getStart() {
        return this.start;
    }

    /**
     * Getter for the end range.
     * 
     * @return the end range
     */
    public double getEnd() {
        return this.end;
    }

    /**
     * Returns the degree of the Chebyshev polynomial.
     * 
     * @return the degree of the Chebyshev polynomial
     */
    public int degree() {
        return this.coefficients.length - 1;
    }

    /**
     * Returns a copy of the coefficients array.
     * <p>
     * Changes made to the returned copy will not affect the coefficients of the Chebyshev
     * polynomial.
     * </p>
     * 
     * @return a fresh copy of the coefficients array
     */
    public double[] getCoefficients() {
        return this.coefficients.clone();
    }

    /**
     * Compute the N Chebyshev abscissas on the range [start ; end] in a chronological (increasing) order..
     * 
     * @param n Number of points to evaluate
     * @return the N Chebyshev abscissas
     * @throws NotStrictlyPositiveException
     *         if {@code n <= 0}
     * @throws MathIllegalArgumentException
     *         if {@code start >= end}
     */
    public double[] getChebyshevAbscissas(final int n) {
        // Compute and return the Chebyshev abscissas in a chronological (increasing) order
        return PolynomialsUtils.getChebyshevAbscissas(this.start, this.end, n);
    }

    /**
     * Compute the value of the function for the given argument.
     * <p>
     * The value returned is <br/>
     * <code>coefficients[0] + coefficients[1] * T1 +  ... + coefficients[n] * Tn</code>
     * </p>
     * <p>
     * Note: the argument must be in the Chebyshev polynomial range [{@link #start}; {@link #end}].
     * </p>
     * 
     * @param x
     *        Argument for which the function value should be computed
     * @return the value of the Chebyshev polynomial at the given point
     * @see UnivariateFunction#value(double)
     * @throws IllegalArgumentException
     *         if the argument isn't in the Chebyshev polynomial range
     */
    @Override
    public double value(final double x) {
        return evaluate(this.start, this.end, this.coefficients, x);
    }

    /**
     * The Chebyshev polynomial is evaluated at a point <math>y = [x - (b + a) / 2] / [(b - a) /
     * 2]</math> and the
     * result is returned as the function value.
     * 
     * @param start
     *        Start range
     * @param end
     *        End range
     * @param coefficients
     *        Coefficients of the polynomial to evaluate.
     * @param argument
     *        Input value
     * @return the value of the Chebyshev polynomial
     * @throws NoDataException
     *         if {@code coefficients} is empty
     * @throws NullArgumentException
     *         if {@code coefficients} is {@code null}
     * @throws MathIllegalArgumentException
     *         if {@code start >= end}
     * @throws IllegalArgumentException
     *         if the argument isn't in the Chebyshev polynomial range
     */
    protected static double evaluate(final double start, final double end,
            final double[] coefficients, final double argument) {

        // Check input consistency
        MathUtils.checkNotNull(coefficients);
        final int n = coefficients.length;
        if (n == 0) {
            throw new NoDataException(PatriusMessages.EMPTY_POLYNOMIALS_COEFFICIENTS_ARRAY);
        }
        if (start >= end) {
            throw new MathIllegalArgumentException(PatriusMessages.INCORRECT_INTERVAL);
        }

        final int degree = n - 1;

        // Change of variable
        final double y = (2. * argument - start - end) / (end - start);
        final double y2 = 2. * y; // For optimization

        // Clenshaw’s recurrence
        double d = 0.;
        double dd = 0.;
        double sv;
        for (int i = degree; i > 0; i--) {
            sv = d;
            d = y2 * d - dd + coefficients[i];
            dd = sv;
        }

        // Last step is different
        return y * d - dd + coefficients[0];
    }

    /**
     * Add a Chebyshev polynomial to the instance.
     * <p>
     * Note: the Chebyshev polynomial to add must be defined on the same range as the instance.
     * </p>
     * 
     * @param p
     *        Chebyshev polynomial to add
     * @return a new Chebyshev polynomial which is the sum of the instance and {@code p}
     * @throws IllegalArgumentException if the given Chebyshev polynomial hasn't the same range as
     *         the instance
     */
    public PolynomialChebyshevFunction add(final PolynomialChebyshevFunction p) {
        // Check input function range consistency
        this.checkSameRange(p);

        // Identify the lowest degree polynomial
        final int lowLength = MathLib.min(this.coefficients.length, p.coefficients.length);
        final int highLength = MathLib.max(this.coefficients.length, p.coefficients.length);

        // Build the coefficients array
        final double[] newCoefficients = new double[highLength];
        for (int i = 0; i < lowLength; ++i) {
            newCoefficients[i] = this.coefficients[i] + p.coefficients[i];
        }
        System.arraycopy((this.coefficients.length < p.coefficients.length) ? p.coefficients
                : this.coefficients, lowLength, newCoefficients, lowLength, highLength - lowLength);

        return new PolynomialChebyshevFunction(this.start, this.end, newCoefficients);
    }

    /**
     * Subtract a Chebyshev polynomial from the instance.
     * <p>
     * Note: the Chebyshev polynomial to subtract must be defined on the same range as the instance.
     * </p>
     * 
     * @param p
     *        Chebyshev polynomial to subtract
     * @return a new Chebyshev polynomial which is the difference of the instance minus {@code p}
     * @throws IllegalArgumentException if the given Chebyshev polynomial hasn't the same range as
     *         the instance
     */
    public PolynomialChebyshevFunction subtract(final PolynomialChebyshevFunction p) {
        // Check input function range consistency
        this.checkSameRange(p);

        // Identify the lowest degree polynomial
        final int lowLength = MathLib.min(this.coefficients.length, p.coefficients.length);
        final int highLength = MathLib.max(this.coefficients.length, p.coefficients.length);

        // Build the coefficients array
        final double[] newCoefficients = new double[highLength];
        for (int i = 0; i < lowLength; ++i) {
            newCoefficients[i] = this.coefficients[i] - p.coefficients[i];
        }
        if (this.coefficients.length < p.coefficients.length) {
            // Add opposite of additional elements from the polynomial to subtract
            for (int i = lowLength; i < highLength; ++i) {
                newCoefficients[i] = -p.coefficients[i];
            }
        } else {
            // Add copy of additional elements from the instance
            System.arraycopy(this.coefficients, lowLength, newCoefficients, lowLength, highLength
                    - lowLength);
        }

        return new PolynomialChebyshevFunction(this.start, this.end, newCoefficients);
    }

    /**
     * Negate the instance coefficients.
     * 
     * @return a new Chebyshev polynomial
     */
    public PolynomialChebyshevFunction negate() {
        final double[] newCoefficients = new double[this.coefficients.length];
        for (int i = 0; i < this.coefficients.length; ++i) {
            newCoefficients[i] = -this.coefficients[i];
        }
        return new PolynomialChebyshevFunction(this.start, this.end, newCoefficients);
    }

    /**
     * Return a new {@link PolynomialChebyshevFunction} object that approximates the derivative of
     * the existing function
     * over the same range [{@link #start}; {@link #end}].
     * 
     * @return a new Chebyshev derivative polynomial
     */
    public PolynomialChebyshevFunction polynomialDerivative() {

        final int n = this.coefficients.length;
        final double[] derivativeCoefficients;

        if (n > 1) {
            derivativeCoefficients = new double[n - 1];
            final int degree = n - 2; // For optimization
            // "degree" and "degree - 1" are special cases
            derivativeCoefficients[degree] = 2. * (degree + 1) * this.coefficients[degree + 1];
            if (degree > 0) {
                derivativeCoefficients[degree - 1] = 2. * degree * this.coefficients[degree];

                for (int i = degree - 1; i > 0; i--) {
                    derivativeCoefficients[i - 1] = derivativeCoefficients[i + 1] + 2 * i
                            * this.coefficients[i];
                }
            }

            // Normalize to the interval b-a
            double k;
            for (int i = 0; i <= degree; i++) {
                if (i == 0) {
                    k = 1. / (this.end - this.start);
                } else {
                    k = 2. / (this.end - this.start);
                }
                derivativeCoefficients[i] *= k;
            }
        } else {
            // if n == 1, then derivativeCoefficients has only one coefficient, whose value is zero:
            // derivativeCoefficients = { 0. }
            derivativeCoefficients = new double[n];
            derivativeCoefficients[0] = 0.;
        }

        return new PolynomialChebyshevFunction(this.start, this.end, derivativeCoefficients);
    }

    /**
     * Return a new Chebyshev polynomial object as a {@link UnivariateFunction} that approximates
     * the derivative of the
     * existing function over the same range [{@link #start}; {@link #end}].
     * 
     * @return a new Chebyshev derivative polynomial
     */
    public UnivariateFunction derivative() {
        return this.polynomialDerivative();
    }

    /** {@inheritDoc} */
    @Override
    public double[] gradient(final double x, final double... parameters) {
        // Parameters are ci coefficients
        final double[] gradient = new double[parameters.length];
        for (int i = 0; i < parameters.length; ++i) {
            // Evaluate Ti (Chebychev coefficient for ith coefficient)
            final double[] coefs = new double[i + 1];
            coefs[i] = 1.;
            gradient[i] = PolynomialChebyshevFunction.evaluate(this.start, this.end, coefs, x);
        }
        return gradient;
    }

    /** {@inheritDoc} */
    @Override
    public double value(final double x, final double... parameters) {
        return PolynomialChebyshevFunction.evaluate(this.start, this.end, parameters, x);
    }

    /**
     * Returns a string representation of the Chebyshev polynomial.
     * 
     * <p>
     * The representation is user oriented. Terms are displayed lowest degrees first. The
     * multiplications signs, coefficients equals to one and null terms are not displayed (except if
     * the polynomial is 0, in which case the 0 constant term is displayed). Addition of terms with
     * negative coefficients are replaced by subtraction of terms with positive coefficients except
     * for the first displayed term (i.e. we display <code>-3</code> for a constant negative
     * polynomial, but <code>1 - 3 T1 + T2</code> if the negative coefficient is not the first one
     * displayed).
     * </p>
     * 
     * @return a string representation of the polynomial.
     */
    @Override
    public String toString() {
        // Initialization
        final StringBuilder s = new StringBuilder();

        // Add 0 only if necessary
        if (this.coefficients[0] == 0.) {
            if (this.coefficients.length == 1) {
                return "0";
            }
        } else {
            s.append(toString(this.coefficients[0]));
        }

        // Loop on coefficients
        for (int i = 1; i < this.coefficients.length; ++i) {
            // Add + or -
            if (this.coefficients[i] != 0.) {
                if (s.length() > 0) {
                    if (this.coefficients[i] < 0.) {
                        s.append(" - ");
                    } else {
                        s.append(" + ");
                    }
                } else {
                    if (this.coefficients[i] < 0.) {
                        s.append("-");
                    }
                }

                final double absCi = MathLib.abs(this.coefficients[i]);
                if ((absCi - 1.) != 0.) {
                    s.append(toString(absCi));
                    s.append(' ');
                }

                s.append("T");
                s.append(Integer.toString(i)); // Add degree
            }
        }

        // Return string
        return s.toString();
    }

    /**
     * Creates a string representing a coefficient, removing ".0" endings.
     * 
     * @param coeff
     *        Coefficient
     * @return a string representation of {@code coeff}.
     */
    private static String toString(final double coeff) {
        String c = Double.toString(coeff);
        if (c.endsWith(".0")) {
            c = c.substring(0, c.length() - 2);
        }
        return c;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = Objects.hash(new Double(this.start), new Double(this.end));
        result = prime * result + Arrays.hashCode(this.coefficients);
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object obj) {
        boolean isEqual;
        if (obj == this) {
            // Identity
            isEqual = true;
        } else if ((obj != null) && (obj.getClass() == this.getClass())) {
            // Same object type: check all attributes
            final PolynomialChebyshevFunction other = (PolynomialChebyshevFunction) obj;
            isEqual = true;
            isEqual &= Objects.equals(new Double(this.start), new Double(other.start));
            isEqual &= Objects.equals(new Double(this.end), new Double(other.end));
            isEqual &= Arrays.equals(this.coefficients, other.coefficients);
        } else {
            // Different object type
            isEqual = false;
        }
        return isEqual;
    }

    /**
     * Check if the given Chebyshev polynomial has the same range as the instance.
     * 
     * @param p
     *        Chebyshev polynomial to check against the instance
     * @throws IllegalArgumentException if it isn't the case
     */
    private void checkSameRange(final PolynomialChebyshevFunction p) {
        if (this.start != p.start || this.end != p.end) {
            throw PatriusException.createIllegalArgumentException(
                    PatriusMessages.CHEBYCHEV_POLYNOMIALS_NOT_SAME_RANGE, this.start, this.end,
                    p.start, p.end);
        }
    }
}
