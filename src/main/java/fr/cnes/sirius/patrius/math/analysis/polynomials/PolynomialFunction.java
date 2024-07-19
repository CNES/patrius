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
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11.1:FA:FA-86:30/06/2023:[PATRIUS] Retours JE Alice
 * VERSION:4.11.1:DM:DM-88:30/06/2023:[PATRIUS] Complement FT 3319
 * VERSION:4.11:DM:DM-3310:22/05/2023:[PATRIUS] Ajout de methode pour integrer les fonctions polynomiales
 * VERSION:4.11:DM:DM-3319:22/05/2023:[PATRIUS] Rendre QuaternionPolynomialSegment plus generique et coherent
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.polynomials;

import java.util.Arrays;

import fr.cnes.sirius.patrius.math.analysis.ParametricUnivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.differentiation.DerivativeStructure;
import fr.cnes.sirius.patrius.math.analysis.differentiation.UnivariateDifferentiableFunction;
import fr.cnes.sirius.patrius.math.exception.NoDataException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Immutable representation of a real polynomial function with real coefficients.
 * <p>
 * <a href="http://mathworld.wolfram.com/HornersMethod.html">Horner's Method</a> is used to evaluate
 * the function.
 * </p>
 *
 * @version $Id: PolynomialFunction.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class PolynomialFunction implements UnivariateDifferentiableFunction, PolynomialFunctionInterface {

    /** Serializable UID. */
    private static final long serialVersionUID = -7726511984200295583L;

    /**
     * The coefficients of the polynomial, ordered by degree -- i.e.,
     * coefficients[0] is the constant term and coefficients[n] is the
     * coefficient of x^n where n is the degree of the polynomial.
     */
    private final double[] coefficients;

    /**
     * Construct a polynomial with the given coefficients. The first element
     * of the coefficients array is the constant term. Higher degree
     * coefficients follow in sequence. The degree of the resulting polynomial
     * is the index of the last non-null element of the array, or 0 if all elements
     * are null.
     * <p>
     * The constructor makes a copy of the input array and assigns the copy to the coefficients
     * property.
     * </p>
     *
     * @param c
     *        Polynomial coefficients.
     * @throws NullArgumentException
     *         if {@code c} is {@code null}.
     * @throws NoDataException
     *         if {@code c} is empty.
     */
    public PolynomialFunction(final double[] c) {
        super();
        MathUtils.checkNotNull(c);
        int n = c.length;
        if (n == 0) {
            throw new NoDataException(PatriusMessages.EMPTY_POLYNOMIALS_COEFFICIENTS_ARRAY);
        }
        while ((n > 1) && (c[n - 1] == 0)) {
            --n;
        }
        this.coefficients = new double[n];
        System.arraycopy(c, 0, this.coefficients, 0, n);
    }

    /**
     * Compute the value of the function for the given argument.
     * <p>
     * The value returned is <br/>
     * <code>coefficients[n] * x^n + ... + coefficients[1] * x  + coefficients[0]</code>
     * </p>
     *
     * @param x
     *        Argument for which the function value should be computed.
     * @return the value of the polynomial at the given point.
     * @see UnivariateFunction#value(double)
     */
    @Override
    public double value(final double x) {
        return evaluate(this.coefficients, x);
    }

    /**
     * Returns the degree of the polynomial.
     *
     * @return the degree of the polynomial.
     */
    @Override
    public int getDegree() {
        return this.coefficients.length - 1;
    }

    /**
     * Returns a copy of the coefficients array.
     * <p>
     * Changes made to the returned copy will not affect the coefficients of the polynomial.
     * </p>
     *
     * @return a fresh copy of the coefficients array.
     */
    @Override
    public double[] getCoefficients() {
        return this.coefficients.clone();
    }

    /**
     * Uses Horner's Method to evaluate the polynomial with the given coefficients at
     * the argument.
     *
     * @param coefficients
     *        Coefficients of the polynomial to evaluate.
     * @param argument
     *        Input value.
     * @return the value of the polynomial.
     * @throws NoDataException
     *         if {@code coefficients} is empty.
     * @throws NullArgumentException
     *         if {@code coefficients} is {@code null}.
     */
    protected static double evaluate(final double[] coefficients, final double argument) {
        MathUtils.checkNotNull(coefficients);
        final int n = coefficients.length;
        if (n == 0) {
            throw new NoDataException(PatriusMessages.EMPTY_POLYNOMIALS_COEFFICIENTS_ARRAY);
        }
        double result = coefficients[n - 1];
        for (int j = n - 2; j >= 0; j--) {
            result = argument * result + coefficients[j];
        }
        return result;
    }

    /**
     * {@inheritDoc}
     *
     * @since 3.1
     * @throws NoDataException
     *         if {@code coefficients} is empty.
     * @throws NullArgumentException
     *         if {@code coefficients} is {@code null}.
     */
    @Override
    public DerivativeStructure value(final DerivativeStructure t) {
        MathUtils.checkNotNull(this.coefficients);
        final int n = this.coefficients.length;
        if (n == 0) {
            throw new NoDataException(PatriusMessages.EMPTY_POLYNOMIALS_COEFFICIENTS_ARRAY);
        }
        DerivativeStructure result = new DerivativeStructure(t.getFreeParameters(), t.getOrder(),
                this.coefficients[n - 1]);
        for (int j = n - 2; j >= 0; j--) {
            result = result.multiply(t).add(this.coefficients[j]);
        }
        return result;
    }

    /**
     * Add a polynomial to the instance.
     *
     * @param p
     *        Polynomial to add.
     * @return a new polynomial which is the sum of the instance and {@code p}.
     */
    public PolynomialFunction add(final PolynomialFunction p) {
        // identify the lowest degree polynomial
        final int lowLength = MathLib.min(this.coefficients.length, p.coefficients.length);
        final int highLength = MathLib.max(this.coefficients.length, p.coefficients.length);

        // build the coefficients array
        final double[] newCoefficients = new double[highLength];
        for (int i = 0; i < lowLength; ++i) {
            newCoefficients[i] = this.coefficients[i] + p.coefficients[i];
        }
        System.arraycopy((this.coefficients.length < p.coefficients.length) ? p.coefficients : this.coefficients,
                lowLength, newCoefficients, lowLength, highLength - lowLength);

        return new PolynomialFunction(newCoefficients);
    }

    /**
     * Subtract a polynomial from the instance.
     *
     * @param p
     *        Polynomial to subtract.
     * @return a new polynomial which is the difference the instance minus {@code p}.
     */
    public PolynomialFunction subtract(final PolynomialFunction p) {
        // identify the lowest degree polynomial
        final int lowLength = MathLib.min(this.coefficients.length, p.coefficients.length);
        final int highLength = MathLib.max(this.coefficients.length, p.coefficients.length);

        // build the coefficients array
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
            System.arraycopy(this.coefficients, lowLength, newCoefficients, lowLength, highLength - lowLength);
        }

        return new PolynomialFunction(newCoefficients);
    }

    /**
     * Negate the instance.
     *
     * @return a new polynomial.
     */
    public PolynomialFunction negate() {
        final double[] newCoefficients = new double[this.coefficients.length];
        for (int i = 0; i < this.coefficients.length; ++i) {
            newCoefficients[i] = -this.coefficients[i];
        }
        return new PolynomialFunction(newCoefficients);
    }

    /**
     * Multiply the instance by a polynomial.
     *
     * @param p
     *        Polynomial to multiply by.
     * @return a new polynomial.
     */
    public PolynomialFunction multiply(final PolynomialFunction p) {
        // build the coefficients array
        final double[] newCoefficients = new double[this.coefficients.length + p.coefficients.length - 1];

        // compute the new coefficients
        for (int i = 0; i < newCoefficients.length; ++i) {
            newCoefficients[i] = 0.0;
            final int min = MathLib.max(0, i + 1 - p.coefficients.length);
            for (int j = min; j < MathLib.min(this.coefficients.length, i + 1); ++j) {
                newCoefficients[i] += this.coefficients[j] * p.coefficients[i - j];
            }
        }

        // return the result of the multiplication of the instance by polynomial p
        return new PolynomialFunction(newCoefficients);
    }

    /**
     * Returns the coefficients of the derivative of the polynomial with the given coefficients.
     *
     * @param coefficients
     *        Coefficients of the polynomial to differentiate.
     * @return the coefficients of the derivative or {@code null} if coefficients has length 1.
     * @throws NoDataException
     *         if {@code coefficients} is empty.
     * @throws NullArgumentException
     *         if {@code coefficients} is {@code null}.
     */
    protected static double[] differentiate(final double[] coefficients) {
        // Sanity check
        MathUtils.checkNotNull(coefficients);
        // get the number of coefficients of the polynomial to differentiate
        final int n = coefficients.length;
        if (n == 0) {
            // Empty polynomial exception
            throw new NoDataException(PatriusMessages.EMPTY_POLYNOMIALS_COEFFICIENTS_ARRAY);
        }
        if (n == 1) {
            // return zero order null polynomial
            return new double[] { 0 };
        }
        final double[] result = new double[n - 1];
        // compute coefficients of the derivative
        for (int i = n - 1; i > 0; i--) {
            result[i - 1] = i * coefficients[i];
        }
        return result;
    }

    /**
     * Returns the derivative as a {@link PolynomialFunction}.
     *
     * @return the derivative polynomial.
     */
    @Override
    public PolynomialFunction derivative() {
        return new PolynomialFunction(differentiate(this.coefficients));
    }

    /**
     * Returns the derivative as a {@link UnivariateFunction}.
     *
     * @return the derivative function.
     */
    public UnivariateFunction univariateDerivative() {
        return this.derivative();
    }

    /**
     * Returns the primitive polynomial. The constant term is computed so the primitive polynomial
     * is matching the given (x0,y0) point.
     * @param x0
     *        the absicssa at which the polynomial value is known
     * @param y0
     *        the value of the polynomial at x0
     * @return the primitive polynomial
     */
    @Override
    public PolynomialFunction primitive(final double x0, final double y0) {
        // Get the number of coefficients of the polynomial to integrate
        final int n = this.coefficients.length;
        // Coefficients of the primitive of the polynomial
        final double[] resultCoeff = new double[n + 1];

        // Compute the primitive coefficients (general case)
        for (int i = n; i > 0; i--) {
            resultCoeff[i] = this.coefficients[i - 1] / i;
        }

        // The first coefficient of the polynomial is found by using the known value of the
        // polynomial as input : (x0,y0)

        // All the known coefficients are from index 1 to n+1. For now, the first coefficient is set
        // to 0. So at this step, "result" is the result polynomial without the first coefficient.
        final PolynomialFunction knownPartOfPolynomial = new PolynomialFunction(resultCoeff);
        // if a0 is the first coefficient: a0 = y0 - (a1*x0 + a2*x0² +...)
        resultCoeff[0] = y0 - knownPartOfPolynomial.value(x0);

        // create and return the complete primitive polynomial
        return new PolynomialFunction(resultCoeff);
    }

    /**
     * Returns a string representation of the polynomial.
     *
     * <p>
     * The representation is user oriented. Terms are displayed lowest degrees first. The
     * multiplications signs, coefficients equals to one and null terms are not displayed (except if
     * the polynomial is 0, in which case the 0 constant term is displayed). Addition of terms with
     * negative coefficients are replaced by subtraction of terms with positive coefficients except
     * for the first displayed term (i.e. we display <code>-3</code> for a constant negative
     * polynomial, but <code>1 - 3 x + x^2</code> if the negative coefficient is not the first one
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
        if (this.coefficients[0] == 0.0) {
            if (this.coefficients.length == 1) {
                return "0";
            }
        } else {
            s.append(toString(this.coefficients[0]));
        }

        // Loop on coefficients
        for (int i = 1; i < this.coefficients.length; ++i) {
            // Add + or -
            if (this.coefficients[i] != 0) {
                if (s.length() > 0) {
                    if (this.coefficients[i] < 0) {
                        s.append(" - ");
                    } else {
                        s.append(" + ");
                    }
                } else {
                    if (this.coefficients[i] < 0) {
                        s.append("-");
                    }
                }

                final double absAi = MathLib.abs(this.coefficients[i]);
                if ((absAi - 1) != 0) {
                    s.append(toString(absAi));
                    s.append(' ');
                }

                s.append("x");
                // Add power
                if (i > 1) {
                    s.append('^');
                    s.append(Integer.toString(i));
                }
            }
        }

        // Return string
        return s.toString();
    }

    /**
     * Creates a string representing a coefficient, removing ".0" endings.
     *
     * @param coeff
     *        Coefficient.
     * @return a string representation of {@code coeff}.
     */
    private static String toString(final double coeff) {
        final String c = Double.toString(coeff);
        if (c.endsWith(".0")) {
            return c.substring(0, c.length() - 2);
        } else {
            return c;
        }
    }

    /** {@inheritDoc} */
    @Override
    public PolynomialType getPolynomialType() {
        return PolynomialType.CLASSICAL;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(this.coefficients);
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PolynomialFunction)) {
            return false;
        }
        final PolynomialFunction other = (PolynomialFunction) obj;
        return Arrays.equals(this.coefficients, other.coefficients);
    }

    /**
     * Dedicated parametric polynomial class.
     *
     * @since 3.0
     */
    public static class Parametric implements ParametricUnivariateFunction {
        /** {@inheritDoc} */
        @Override
        public double[] gradient(final double x, final double... parameters) {
            final double[] gradient = new double[parameters.length];
            double xn = 1.0;
            for (int i = 0; i < parameters.length; ++i) {
                gradient[i] = xn;
                xn *= x;
            }
            return gradient;
        }

        /** {@inheritDoc} */
        @Override
        public double value(final double x, final double... parameters) {
            return PolynomialFunction.evaluate(parameters, x);
        }
    }
}
