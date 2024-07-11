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
 * @history Created 02/01/2012
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:345:30/10/2014:modified comments ratio
 * VERSION::FA:400:17/03/2015: use class FastMath instead of class Math
 * VERSION::FA:458:19/10/2015: Use class FastMath instead of class Math
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.polynomials;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.analysis.DifferentiableIntegrableUnivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.differentiation.DerivativeStructure;
import fr.cnes.sirius.patrius.math.analysis.polynomials.ElementaryMultiplicationTypes.ElementaryType;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.exception.MathRuntimeException;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This class is the Trigonometric Polynomial Function class. Given a constant <code>a0</code>, and two arrays of same
 * lengths <code>a</code> and <code>b</code>,
 * the corresponding trigonometric polynomial function <code>p</code> is given
 * by the following expression : <br>
 * {@code   p(x) = a0 + sum( a(k) * cos(k*x) + b(k) * sin(k*x) , k, 1, n )} <br>
 * where <code>a(k)</code> (resp . <code>b(k)</code>) is the k<sup>th</sup> coefficient of the array <code>a</code>
 * (resp. <code>b</code>).
 * 
 * @concurrency immutable
 * 
 * @author Rami Houdroge
 * @since 1.1
 * @version $Id: TrigonometricPolynomialFunction.java 17603 2017-05-18 08:28:32Z bignon $
 */
public final class TrigonometricPolynomialFunction implements DifferentiableIntegrableUnivariateFunction, Serializable {

    /**
     * Serial UID
     */
    private static final long serialVersionUID = -7735389592574470960L;

    /**
     * a0 coefficient
     */
    private final double a0;

    /**
     * a coefficients
     */
    private final double[] a;

    /**
     * b coefficients
     */
    private final double[] b;

    /**
     * degree of polynomial
     */
    private final int degree;

    /**
     * Constructor for the {@link TrigonometricPolynomialFunction}. This constructor creates the following trigonometric
     * polynomial : <br>
     * {@code P(X) = a0 + a(k) * cos(kX) + b(k) * sin(kX)}
     * 
     * @param coefA0
     *        a0 coefficient
     * @param coefA
     *        cosine coefficients array
     * @param coefB
     *        sine coefficients array
     * @throws MathIllegalArgumentException
     *         if arrays a and b are of different lengths
     */
    public TrigonometricPolynomialFunction(final double coefA0, final double[] coefA, final double[] coefB) {
        // Check if same lengths for cosine and sine coefficients
        if (coefA.length != coefB.length) {
            throw new MathIllegalArgumentException(PatriusMessages.VECTOR_LENGTH_MISMATCH, coefA);
        }

        // Compute real polynomial degree
        int n = coefA.length;
        while ((n > 0) && Precision.equals(coefA[n - 1], 0, 1) && Precision.equals(coefB[n - 1], 0, 1)) {
            n = n - 1;
        }

        // Store values
        this.a0 = coefA0;
        this.degree = n;
        this.a = new double[n];
        this.b = new double[n];
        System.arraycopy(coefA, 0, this.a, 0, n);
        System.arraycopy(coefB, 0, this.b, 0, n);
    }

    /**
     * Add two {@link TrigonometricPolynomialFunction}
     * 
     * @param newPol
     *        polynomial to add to current
     * @return resulting TrigonometricPolynomialFunction
     */
    public TrigonometricPolynomialFunction add(final TrigonometricPolynomialFunction newPol) {

        final boolean isThisBigger = this.degree > newPol.getDegree();
        // max polynomial degree:
        final int degreeSum = MathLib.max(newPol.getDegree(), this.degree);
        // min polynomial degree:
        final int stopIdx = MathLib.min(newPol.getDegree(), this.degree);

        final double[] newA = new double[degreeSum];
        final double[] newB = new double[degreeSum];
        final double a0n = this.a0 + newPol.getA0();

        for (int i = 0; i < degreeSum; i++) {
            if (i < stopIdx) {
                // sum the two polynomials:
                newA[i] = this.a[i] + newPol.getA()[i];
                newB[i] = this.b[i] + newPol.getB()[i];
            } else {
                // do not sum the two polynomials:
                if (isThisBigger) {
                    // use the first polynomial:
                    newA[i] += this.a[i];
                    newB[i] += this.b[i];
                } else {
                    // use the second polynomial:
                    newA[i] += newPol.getA()[i];
                    newB[i] += newPol.getB()[i];
                }
            }
        }
        return new TrigonometricPolynomialFunction(a0n, newA, newB);
    }

    /**
     * Compute and return derivative of polynomial
     * 
     * @return derivative of polynomial function
     */
    public UnivariateFunction derivative() {
        return this.polynomialDerivative(1);
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
     * Multiply {@link TrigonometricPolynomialFunction} by an {@link ElementaryMultiplicationTypes}
     * 
     * @param poly
     *        polynomial
     * @param type
     *        of elementary polynomial
     * @param j
     *        order of elementary polynomial
     * @return resulting polynomial
     */
    private static TrigonometricPolynomialFunction elementaryMultiplication(
            final TrigonometricPolynomialFunction poly, final ElementaryType type, final int j) {

        // Polynomial degree
        final int oldDegree = poly.getDegree();

        // Old polynomial coefficient : order zero coefficient, cosine coefficients
        //          and sine coefficients
        final double oldA0 = poly.getA0();
        final double[] oldA = poly.getA();
        final double[] oldB = poly.getB();

        // Initialize new polynomial coefficient
        final double[] newA0 = new double[1];
        final double[] newA = new double[oldDegree + j];
        final double[] newB = new double[oldDegree + j];

        // Compute new coefficients depending on the type of elementary polynomial
        switch (type) {
            case COS:
                caseCosine(oldA, newA, oldB, newB, oldA0, newA0, j, oldDegree);
                break;
            case SIN:
                caseSine(oldA, newA, oldB, newB, oldA0, newA0, j, oldDegree);
                break;
            default:
                throw new RuntimeException("This should never happen");
        }

        return new TrigonometricPolynomialFunction(newA0[0], newA, newB);
    }

    /**
     * Calculate new coefficients arrays after multiplication by cos(jt)
     * 
     * @param oldA
     *        old cosine coefficients.
     * @param newA
     *        new cosine coefficients.
     * @param oldB
     *        old sine coefficients.
     * @param newB
     *        new sine coefficients.
     * @param oldA0
     *        old constant.
     * @param newA0
     *        new constant.
     * @param j
     *        order of elementary polynomial.
     * @param oldDegree
     *        degree of polynomial.
     * 
     * @since 1.1
     */
    private static void caseCosine(final double[] oldA, final double[] newA, final double[] oldB, final double[] newB,
                                   final double oldA0, final double[] newA0, final int j, final int oldDegree) {

        int sign = 1;

        // a0 * cos(jt)
        newA[j - 1] = oldA0;

        for (int k = 0; k < oldDegree; k++) {
            /*
             * WARNING
             * k is the index into the a (or b) array, it represents a[k] * cos((k+1)t)
             * j is the order of the elementary polynomial cos(jt)
             * Hence :
             * a[k] cos((k+1)t) * cos (j*t) = a[k] / 2 * ( cos( (k+1+j) * t) + cos( (k+1-j) * t)
             * Thus newA[ k+j ] = oldA[k]/2 and newA[|k+1-j|-1] = oldA[k]/2
             */

            /*
             * ak cos((k+1)t) * cos(jt)
             */
            // = ak cos((k+1+j)t) / 2
            newA[k + j] += oldA[k] / 2;
            // + ak cos((k+1-j)t)
            if (k + 1 == j) {
                newA0[0] = oldA[k] / 2;
            } else {
                newA[MathLib.abs(k + 1 - j) - 1] += oldA[k] / 2;
            }

            /*
             * bk sin(kt) * cos(jt)
             */
            // = bk sin((k+j)t) / 2
            newB[k + j] += oldB[k] / 2;
            // + bk sin((k-j)t) /2
            if (k + 1 != j) {
                if (k + 1 - j < 0) {
                    sign = -1;
                } else {
                    sign = 1;
                }
                newB[sign * (k + 1 - j) - 1] += sign * oldB[k] / 2;
            }
        }
    }

    /**
     * Calculate new coefficients arrays after multiplication by sin(jt)
     * 
     * @param oldA
     *        old cosine coefficients
     * @param newA
     *        new cosine coefficients
     * @param oldB
     *        old sine coefficients
     * @param newB
     *        new sine coefficients
     * @param oldA0
     *        old constant
     * @param newA0
     *        new constant
     * @param j
     *        order of elementary polynomial
     * @param oldDegree
     *        degree of polynomial
     * 
     * @since 1.1
     */
    private static void caseSine(final double[] oldA, final double[] newA, final double[] oldB, final double[] newB,
                                 final double oldA0, final double[] newA0, final int j, final int oldDegree) {

        int sign = 1;

        // a0 * sin(jt)
        newB[j - 1] = oldA0;
        for (int k1 = 0; k1 < oldDegree; k1++) {

            /*
             * ak cos(kt) * sin(jt)
             */
            // = ak sin((k+j)t) / 2
            newB[k1 + j] += oldA[k1] / 2;
            // - ak sin((k-j)t) / 2
            if (k1 + 1 != j) {
                if (k1 + 1 - j < 0) {
                    sign = -1;
                } else {
                    sign = 1;
                }
                newB[sign * (k1 + 1 - j) - 1] -= sign * oldA[k1] / 2;
            }

            /*
             * bk sin(kt) * sin(jt)
             */
            // = - bk cos((k+j)t) / 2
            newA[k1 + j] -= oldB[k1] / 2;
            // + bk cos((k-j)t) / 2
            if (k1 + 1 == j) {
                newA0[0] = oldB[k1] / 2;
            } else {
                newA[MathLib.abs(k1 + 1 - j) - 1] += oldB[k1] / 2;
            }
        }
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
    private static double evaluate(final TrigonometricPolynomialFunction poly, final double x) {
        double result = poly.getA0();
        for (int k = 0; k < poly.getDegree(); k++) {
            final double[] sincos = MathLib.sinAndCos((k + 1) * x);
            final double sin = sincos[0];
            final double cos = sincos[1];
            result += poly.getA()[k] * cos + poly.getB()[k] * sin;
        }

        return result;
    }

    /**
     * Get array of cosine coefficients
     * 
     * @return a
     */
    public double[] getA() {
        return this.a.clone();
    }

    /**
     * Get value of order zero coefficient
     * 
     * @return a0
     */
    public double getA0() {
        return this.a0;
    }

    /**
     * Get array of sine coefficients
     * 
     * @return b
     */
    public double[] getB() {
        return this.b.clone();
    }

    /**
     * Get polynomial degree
     * 
     * @return n
     */
    public int getDegree() {
        return this.degree;
    }

    /**
     * Multiply this polynomial by another polynomial
     * 
     * @param polynomial
     *        polynomial to multiply instance by
     * @return multiplied polynomials in a new instance
     */
    public TrigonometricPolynomialFunction multiply(final TrigonometricPolynomialFunction polynomial) {

        // Polynomial coefficients
        final double a0n = polynomial.getA0();
        final double[] polA = polynomial.getA();
        final double[] polB = polynomial.getB();

        // scalar multiply by a0
        TrigonometricPolynomialFunction result = this.scalarMultiply(a0n);

        // perform elementary multiplications
        for (int i = 0; i < polynomial.getDegree(); i++) {
            if (!Precision.equals(polA[i], 0, 1)) {
                result = result.add(elementaryMultiplication(this, ElementaryType.COS, i + 1).scalarMultiply(polA[i]));
            }
            if (!Precision.equals(polB[i], 0, 1)) {
                result = result.add(elementaryMultiplication(this, ElementaryType.SIN, i + 1).scalarMultiply(polB[i]));
            }
        }

        return result;
    }

    /**
     * Negate polynomial
     * 
     * @return negated polynomial
     */
    public TrigonometricPolynomialFunction negate() {
        final double[] newA = new double[this.degree];
        final double[] newB = new double[this.degree];
        for (int i = 0; i < this.degree; i++) {
            newA[i] = -this.a[i];
            newB[i] = -this.b[i];
        }
        return new TrigonometricPolynomialFunction(-this.a0, newA, newB);
    }

    /**
     * Returns the first order derivative as a {@link TrigonometricPolynomialFunction}.
     * 
     * @return the derivative polynomial.
     */
    public TrigonometricPolynomialFunction polynomialDerivative() {

        return this.polynomialDerivative(1);
    }

    /**
     * Returns the n<sup>th</sup> order derivative as a {@link TrigonometricPolynomialFunction}.
     * 
     * @param order
     *        order of derivative (must be > 0)
     * @return the derivative polynomial.
     */
    public TrigonometricPolynomialFunction polynomialDerivative(final int order) {

        TrigonometricPolynomialFunction result = this;

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
    private static TrigonometricPolynomialFunction polynomialDerivative(final TrigonometricPolynomialFunction poly) {

        final int degree = poly.getDegree();

        final double[] aD = new double[degree];
        final double[] bD = new double[degree];

        for (int k = 0; k < degree; k++) {
            bD[k] = -poly.getA()[k] * (k + 1);
            aD[k] = poly.getB()[k] * (k + 1);
        }

        return new TrigonometricPolynomialFunction(0, aD, bD);
    }

    /**
     * Get primitive of {@link TrigonometricPolynomialFunction}
     * 
     * @param constant
     *        integration constant
     * @return the primitive as a {@link TrigonometricPolynomialPrimitive}
     */
    public TrigonometricPolynomialPrimitive polynomialPrimitive(final double constant) {
        return polynomialPrimitive(this, constant);
    }

    /**
     * Get primitive of {@link TrigonometricPolynomialFunction}
     * 
     * @param poly
     *        polynomial for primitive computation
     * @param constant
     *        integration constant
     * @return the primitive as a {@link TrigonometricPolynomialPrimitive}
     */
    private static TrigonometricPolynomialPrimitive polynomialPrimitive(final TrigonometricPolynomialFunction poly,
                                                                          final double constant) {

        // Calculate and store primitive
        final double[] a = new double[poly.getDegree()];
        final double[] b = new double[poly.getDegree()];

        for (int i = 0; i < poly.getDegree(); i++) {
            a[i] = -poly.getB()[i] / (i + 1);
            b[i] = poly.getA()[i] / (i + 1);
        }

        // Create new trigonometric polynomial from primitives
        final TrigonometricPolynomialFunction trigonometricPolynomial = new TrigonometricPolynomialFunction(0, a, b);
        // Create new polynomial function from input trigonometric polynomial
        final PolynomialFunction linearPolynomial = new PolynomialFunction(new double[] { constant, poly.getA0() });

        return new TrigonometricPolynomialPrimitive(linearPolynomial, trigonometricPolynomial);
    }

    /**
     * Get primitive of {@link TrigonometricPolynomialFunction}
     * 
     * @return the primitive as a {@link UnivariateFunction}
     */
    @Override
    public UnivariateFunction primitive() {
        return polynomialPrimitive(this, 0);
    }

    /**
     * Add a scalar
     * 
     * @param scalar
     *        to add
     * @return new {@link TrigonometricPolynomialFunction}
     */
    public TrigonometricPolynomialFunction scalarAdd(final double scalar) {
        return new TrigonometricPolynomialFunction(this.getA0() + scalar, this.getA(), this.getB());
    }

    /**
     * Multiply by a scalar
     * 
     * @param scalar
     *        to multiply polynomial by
     * @return polynomial multiplied by scalar
     */
    public TrigonometricPolynomialFunction scalarMultiply(final double scalar) {
        final double[] newA = new double[this.degree];
        final double[] newB = new double[this.degree];
        for (int i = 0; i < this.degree; i++) {
            newA[i] = this.a[i] * scalar;
            newB[i] = this.b[i] * scalar;
        }
        return new TrigonometricPolynomialFunction(scalar * this.a0, newA, newB);
    }

    /**
     * Subtract a scalar
     * 
     * @param scalar
     *        to subtract
     * @return new {@link TrigonometricPolynomialFunction}
     */
    public TrigonometricPolynomialFunction scalarSubtract(final double scalar) {
        return this.scalarAdd(-scalar);
    }

    /**
     * Divide by a scalar
     * 
     * @param scalar
     *        to divide polynomial by
     * @return polynomial divide by scalar
     */
    public TrigonometricPolynomialFunction scalarDivide(final double scalar) {
        if (Precision.equals(scalar, 0, 1)) {
            throw new MathIllegalArgumentException(PatriusMessages.ZERO_NOT_ALLOWED, scalar);
        } else {
            return this.scalarMultiply(MathLib.divide(1., scalar));
        }
    }

    /**
     * Subtract a polynomial to the current polynomial :
     * 
     * @param polynomial
     *        to perform subtraction
     * @return {@code this - polynomial}
     */
    public TrigonometricPolynomialFunction subtract(final TrigonometricPolynomialFunction polynomial) {
        return this.add(polynomial.negate());
    }

    /**
     * Get String representation of polynomial
     * 
     * @return string
     */
    @Override
    public String toString() {

        // Initialize variable and output
        final String x = " x)";
        final StringBuffer buf = new StringBuffer();

        // Append order zero coefficient
        if (this.getA0() != 0) {
            buf.append(this.addNumber(this.getA0()));
        }
        // Append arrays of sine and cosine coefficients
        for (int i = 0; i < this.getDegree(); i++) {
            if (this.getA()[i] != 0) {
                buf.append(this.addNumber(this.getA()[i]) + " cos(" + (i + 1) + x);
            }
            if (this.getB()[i] != 0) {
                buf.append(this.addNumber(this.getB()[i]) + " sin(" + (i + 1) + x);
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
    private String addNumber(final double coeff) {

        final String sign;
        if (coeff < 0) {
            sign = " - ";
        } else {
            sign = " + ";
        }

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
        } else {
            return c;
        }
    }

    /**
     * Return value at x of polynomial
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
    public double value(final int n, final double x) {
        return this.polynomialDerivative(n).value(x);
    }

    /** {@inheritDoc} */
    @Override
    public DerivativeStructure value(final DerivativeStructure t) {
        final int freePars = t.getFreeParameters();
        if (freePars != 1) {
            // Limitation : only one free parameter allowed
            throw new MathRuntimeException(PatriusMessages.UNSUPPORTED_OPERATION);
        }
        final double value = t.getValue();
        final int order = t.getOrder();

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
