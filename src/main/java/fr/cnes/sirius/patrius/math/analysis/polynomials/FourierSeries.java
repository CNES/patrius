/**
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
 * @history Created 23/05/2012
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:FA:FA-2325:27/05/2020:FourierSeries realise des clones abusifs 
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:410:16/04/2015: Anomalies in the Patrius Javadoc
 * VERSION::FA:458:19/10/2015: Use class FastMath instead of class Math
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.polynomials;

import fr.cnes.sirius.patrius.math.analysis.DifferentiableIntegrableUnivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.differentiation.DerivativeStructure;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.exception.MathRuntimeException;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This class represents a finite Fourier Series
 * 
 * @concurrency immutable
 * 
 * @see FourierDecompositionEngine
 * 
 * @author Rami Houdroge
 * 
 * @version $Id: FourierSeries.java 17603 2017-05-18 08:28:32Z bignon $
 * 
 * @since 1.2
 * 
 */
public final class FourierSeries implements DifferentiableIntegrableUnivariateFunction {

    /** Serializable UID. */
    private static final long serialVersionUID = -669438626664714658L;

    /**
     * angular frequency
     */
    private final double angularFrequency;

    /**
     * a0 coefficient
     */
    private final double constant;

    /**
     * a coefficients
     */
    private final double[] cosArray;

    /**
     * b coefficients
     */
    private final double[] sinArray;

    /**
     * degree of polynomial
     */
    private final int degree;

    /**
     * Constructor for the {@link FourierSeries}. This constructor creates the following trigonometric
     * polynomial : <br>
     * {@code P(X) = a0 + a(k) * cos(k * omega * X) + b(k) * sin(k * omega * X)}<br>
     * where <code>omega = 2 * pi / period</code>
     * 
     * @param omega
     *        angular frequency
     * @param a0
     *        a0 coefficient
     * @param a
     *        cosine coefficients array
     * @param b
     *        sine coefficients array
     * 
     * @throws MathIllegalArgumentException
     *         if arrays a and b are of different lengths
     */
    public FourierSeries(final double omega, final double a0, final double[] a, final double[] b) {
        // Check if same lengths for cosine and sine coefficients
        if (a.length != b.length) {
            throw new MathIllegalArgumentException(PatriusMessages.VECTOR_LENGTH_MISMATCH, a);
        }

        // Compute real polynomial degree
        int n = a.length;
        while ((n > 0) && Precision.equals(a[n - 1], 0, 1) && Precision.equals(b[n - 1], 0, 1)) {
            n = n - 1;
        }

        // Store values
        this.angularFrequency = omega;
        this.constant = a0;
        this.degree = n;
        this.cosArray = new double[n];
        this.sinArray = new double[n];
        System.arraycopy(a, 0, this.cosArray, 0, n);
        System.arraycopy(b, 0, this.sinArray, 0, n);
    }

    /**
     * Add a scalar
     * 
     * @param scalar
     *        to add
     * @return new {@link TrigonometricPolynomialFunction}
     */
    public FourierSeries scalarAdd(final double scalar) {

        return new FourierSeries(this.getAngularFrequency(), this.getConstant() + scalar, this.getCosArray(),
            this.getSinArray());
    }

    /**
     * Multiply by a scalar
     * 
     * @param scalar
     *        to multiply polynomial by
     * @return polynomial multiplied by scalar
     */
    public FourierSeries scalarMultiply(final double scalar) {

        final double[] newA = new double[this.degree];
        final double[] newB = new double[this.degree];
        for (int i = 0; i < this.degree; i++) {
            newA[i] = this.cosArray[i] * scalar;
            newB[i] = this.sinArray[i] * scalar;
        }
        return new FourierSeries(this.getAngularFrequency(), scalar * this.getConstant(), newA, newB);
    }

    /**
     * Subtract a scalar
     * 
     * @param scalar
     *        to subtract
     * @return new {@link TrigonometricPolynomialFunction}
     */
    public FourierSeries scalarSubtract(final double scalar) {
        return this.scalarAdd(-scalar);
    }

    /**
     * Divide by a scalar
     * 
     * @param scalar
     *        to divide polynomial by
     * @return polynomial divide by scalar
     */
    public FourierSeries scalarDivide(final double scalar) {
        if (Precision.equals(scalar, 0, 1)) {
            throw new MathIllegalArgumentException(PatriusMessages.ZERO_NOT_ALLOWED, scalar);
        }

        return this.scalarMultiply(MathLib.divide(1., scalar));
    }

    /**
     * Negate polynomial
     * 
     * @return negated polynomial
     */
    public FourierSeries negate() {

        final double[] newA = new double[this.degree];
        final double[] newB = new double[this.degree];
        for (int i = 0; i < this.degree; i++) {
            newA[i] = -this.cosArray[i];
            newB[i] = -this.sinArray[i];
        }
        return new FourierSeries(this.getAngularFrequency(), -this.constant, newA, newB);
    }

    /**
     * Returns the first order derivative as a {@link TrigonometricPolynomialFunction}.
     * 
     * @return the derivative polynomial.
     */
    public FourierSeries polynomialDerivative() {

        return this.polynomialDerivative(1);
    }

    /**
     * Returns the n<sup>th</sup> order derivative as a {@link TrigonometricPolynomialFunction}.
     * 
     * @param order
     *        order of derivative (must be > 0)
     * @return the derivative polynomial.
     */
    public FourierSeries polynomialDerivative(final int order) {

        FourierSeries result = this;

        if (order >= 1) {
            for (int dv = 0; dv < order; dv++) {
                result = polynomialDerivative(result);
            }
        } else {
            throw new MathIllegalArgumentException(PatriusMessages.NUMBER_TOO_SMALL, order);
        }

        return result;
    }

    /**
     * Returns the derivative as a {@link TrigonometricPolynomialFunction}.
     * 
     * @param poly
     *        polynomial to derivate
     * @return the derivative polynomial.
     */
    private static FourierSeries polynomialDerivative(final FourierSeries poly) {

        final int degree = poly.getOrder() - 1;

        final double[] aD = new double[degree];
        final double[] bD = new double[degree];

        for (int k = 0; k < degree; k++) {
            bD[k] = -poly.cosArray[k] * poly.getAngularFrequency() * (k + 1);
            aD[k] = poly.sinArray[k] * poly.getAngularFrequency() * (k + 1);
        }

        return new FourierSeries(poly.getAngularFrequency(), 0, aD, bD);
    }

    /**
     * Get primitive of {@link FourierSeries}. a<inf>0</inf> constant is considered equal to 0.
     * 
     * @return the primitive as a {@link FourierSeries}
     */
    public FourierSeries polynomialPrimitive() {
        return polynomialPrimitive(this);
    }

    /**
     * Get primitive of {@link FourierSeries}. a<inf>0</inf> constant is considered equal to 0.
     * 
     * @param poly
     *        polynomial for primitive computation
     * @return the primitive as a {@link FourierSeries}
     */
    private static FourierSeries polynomialPrimitive(final FourierSeries poly) {

        // Calculate and store primitive
        final double[] a = new double[poly.getOrder() - 1];
        final double[] b = new double[poly.getOrder() - 1];

        for (int i = 0; i < poly.getOrder() - 1; i++) {
            a[i] = -poly.sinArray[i] / ((i + 1) * poly.getAngularFrequency());
            b[i] = poly.cosArray[i] / ((i + 1) * poly.getAngularFrequency());
        }

        return new FourierSeries(poly.getAngularFrequency(), 0, a, b);
    }

    /**
     * Return value at x of polynomial.
     * 
     * @param x
     *        desired abscissa
     * @return value of polynomial function
     */
    @Override
    public double value(final double x) {
        return evaluate(this, x);
    }

    /**
     * Return value at x of n<sup>th</sup> order derivative
     * 
     * @param n
     *        order of derivative
     * @param x
     *        desired abscissa
     * @return value of derivative
     */
    public double derivativeValue(final int n, final double x) {
        return this.polynomialDerivative(n).value(x);
    }

    /**
     * Return value at x of the function primitive
     * 
     * @param x
     *        desired abscissa
     * @return value of derivative
     */
    public double primitiveValue(final double x) {
        return this.polynomialPrimitive().value(x);
    }

    /**
     * Compute value at x of trigonometric polynomial P : <br>
     * {@code P(x) = a0 + a(k) * cos(kx) + b(k) * sin(kx)}
     * 
     * @param poly
     *        polynomial to evaluate
     * @param x
     *        abscissa of desired value
     * @return value of poly at x
     */
    private static double evaluate(final FourierSeries poly, final double x) {
        double result = poly.getConstant();
        for (int k = 0; k < poly.getOrder() - 1; k++) {
            final double[] sincos = MathLib.sinAndCos((k + 1) * x * poly.getAngularFrequency());
            final double sin = sincos[0];
            final double cos = sincos[1];
            result += poly.cosArray[k] * cos + poly.sinArray[k] * sin;
        }

        return result;
    }

    /**
     * Get primitive of {@link TrigonometricPolynomialFunction}
     * 
     * @return the primitive as a {@link UnivariateFunction}
     */
    @Override
    public UnivariateFunction primitive() {
        return polynomialPrimitive(this);
    }

    /**
     * Compute and return derivative of polynomial
     * 
     * @return derivative of polynomial function
     */
    public UnivariateFunction derivative() {
        return polynomialDerivative(this);
    }

    /**
     * Compute and return n<sup>th</sup> derivative of polynomial
     * 
     * @param n
     *        order of derivative
     * @return n<sup>th</sup> derivative of polynomial function
     */
    public UnivariateFunction derivative(final int n) {
        return this.polynomialDerivative(n);
    }

    /**
     * @return the angularFrequency
     */
    public double getAngularFrequency() {
        return this.angularFrequency;
    }

    /**
     * @return the period
     */
    public double getPeriod() {
        return MathLib.divide(2 * FastMath.PI, this.angularFrequency);
    }

    /**
     * @return the constant
     */
    public double getConstant() {
        return this.constant;
    }

    /**
     * @return the cosArray
     */
    public double[] getCosArray() {
        return this.cosArray.clone();
    }

    /**
     * @return the sinArray
     */
    public double[] getSinArray() {
        return this.sinArray.clone();
    }

    /**
     * @return the order of the Fourier Decomposition
     */
    public int getOrder() {
        return this.degree + 1;
    }

    /**
     * Get String representation of polynomial
     * 
     * @return string
     */
    @Override
    public String toString() {

        // Initialize buffer
        final String x = " x )";
        final StringBuffer buf = new StringBuffer();

        // Append zero order coefficient
        if (this.getConstant() != 0) {
            buf.append(addNumber(this.getConstant()));
        }
        // Append arrays for sine and cosine coeffcicients
        for (int i = 0; i < this.getOrder() - 1; i++) {
            if (this.getCosArray()[i] != 0) {
                buf.append(addNumber(this.getCosArray()[i]) + " cos("
                        + addNumber((i + 1) * this.getAngularFrequency()) + x);
            }
            if (this.getSinArray()[i] != 0) {
                buf.append(addNumber(this.getSinArray()[i]) + " sin("
                        + addNumber((i + 1) * this.getAngularFrequency()) + x);
            }
        }

        return buf.toString();
    }

    /**
     * Formats a number
     * 
     * @param coeff
     *        number to format
     * @return formatted number as a string
     * 
     * @since 1.1
     */
    private static String addNumber(final double coeff) {
        final String sign = coeff < 0 ? " - " : " + ";
        final StringBuffer buf = new StringBuffer();
        buf.append(sign);
        buf.append(toString(MathLib.abs(coeff)));
        return buf.toString();
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
        }
        return c;
    }

    /** {@inheritDoc} */
    @Override
    public DerivativeStructure value(final DerivativeStructure t) {
        final int freePars = t.getFreeParameters();
        if (freePars != 1) {
            // Limitation : only one free parameter allowed
            throw new MathRuntimeException(PatriusMessages.UNSUPPORTED_OPERATION);
        }
        final int order = t.getOrder();
        final double value = t.getValue();

        // Compute all values to the order "order"
        final double[] arrez = new double[order + 1];
        // Order 0 value...
        arrez[0] = this.value(value);
        for (int i = 1; i <= order; i++) {
            arrez[i] = this.derivative(i).value(value);
        }
        // Inserts those values in a DerivativeStructure instance
        return new DerivativeStructure(1, order, arrez);
    }
}
