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
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.polynomials;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.differentiation.DerivativeStructure;
import fr.cnes.sirius.patrius.math.analysis.differentiation.UnivariateDifferentiableFunction;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.exception.MathRuntimeException;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This class represents a trigonometric polynomial primitive. Such a function is defined
 * as being the sum of a {link TrigonometricPolynomialFunction} and a {@link PolynomialFunction} :
 * <code>P(x) = a0 + a1 x + sum( bk cos(kt) + ck sin(kt) )</code>
 * 
 * @author Rami Houdroge
 * 
 * @concurrency immutable
 * 
 * @since 1.1
 * @version $Id: TrigonometricPolynomialPrimitive.java 17603 2017-05-18 08:28:32Z bignon $
 */
public final class TrigonometricPolynomialPrimitive implements UnivariateDifferentiableFunction, Serializable {

    /**
     * Serial UID
     */
    private static final long serialVersionUID = -199931044622539474L;
    /**
     * Trigonometric Polynomial part
     */
    private final TrigonometricPolynomialFunction triPolynomial;
    /**
     * Linear Polynomial part
     */
    private final PolynomialFunction linPolynomial;

    /**
     * Constructor for the {@link TrigonometricPolynomialPrimitive} class
     * 
     * @param linearPolynomial
     *        linear part
     * @param trigonometricPolynomial
     *        trigonometric part
     */
    public TrigonometricPolynomialPrimitive(final PolynomialFunction linearPolynomial,
        final TrigonometricPolynomialFunction trigonometricPolynomial) {
        this.linPolynomial = linearPolynomial.add(new PolynomialFunction(
            new double[] { trigonometricPolynomial.getA0() }));
        this.triPolynomial = new TrigonometricPolynomialFunction(0,
            trigonometricPolynomial.getA(), trigonometricPolynomial.getB());
    }

    /**
     * Add two {@link TrigonometricPolynomialPrimitive}
     * 
     * @param poly
     *        to add
     * @return resulting {@link TrigonometricPolynomialPrimitive}
     */
    public TrigonometricPolynomialPrimitive add(final TrigonometricPolynomialPrimitive poly) {
        return new TrigonometricPolynomialPrimitive(
            this.getLinearPolynomial().add(poly.getLinearPolynomial()),
            this.getTrigonometricPolynomial().add(poly.getTrigonometricPolynomial()));
    }

    /**
     * Add a {@link PolynomialFunction}
     * 
     * @param poly
     *        to add
     * @return resulting {@link TrigonometricPolynomialPrimitive}
     */
    public TrigonometricPolynomialPrimitive add(final PolynomialFunction poly) {
        return new TrigonometricPolynomialPrimitive(this.getLinearPolynomial().add(poly),
            this.getTrigonometricPolynomial());
    }

    /**
     * Add a {@link TrigonometricPolynomialFunction}
     * 
     * @param poly
     *        to add
     * @return resulting {@link TrigonometricPolynomialPrimitive}
     */
    public TrigonometricPolynomialPrimitive add(final TrigonometricPolynomialFunction poly) {
        return new TrigonometricPolynomialPrimitive(this.getLinearPolynomial(), this.getTrigonometricPolynomial().add(
            poly));
    }

    /**
     * Get first order derivative
     * 
     * @return derivative as a {@link UnivariateFunction}
     */
    public UnivariateFunction derivative() {
        return this.polynomialDerivative();
    }

    /**
     * Get n<sup>th</sup> order derivative
     * 
     * @param n
     *        order of derivative
     * @return derivative as a {@link UnivariateFunction}
     */
    public UnivariateFunction derivative(final int n) {
        return this.polynomialDerivative(n);
    }

    /**
     * Static evaluation method
     * 
     * @param polynomial
     *        to evaluate
     * @param x
     *        abscissa
     * @return value of polynomial at abscissa
     */
    private static double evaluate(final TrigonometricPolynomialPrimitive polynomial, final double x) {
        return polynomial.getTrigonometricPolynomial().value(x) + polynomial.getLinearPolynomial().value(x);
    }

    /**
     * Get the Linear Polynomial Part
     * 
     * @return the polynomial as a {@link PolynomialFunction}
     */
    public PolynomialFunction getLinearPolynomial() {
        return this.linPolynomial;
    }

    /**
     * Get the Trigonometric Polynomial Part
     * 
     * @return the polynomial as a {@link TrigonometricPolynomialFunction}
     */
    public TrigonometricPolynomialFunction getTrigonometricPolynomial() {
        return this.triPolynomial;
    }

    /**
     * Get opposite of current polynomial
     * 
     * @return opposite
     */
    public TrigonometricPolynomialPrimitive negate() {
        return new TrigonometricPolynomialPrimitive(this.linPolynomial.negate(), this.triPolynomial.negate());
    }

    /**
     * Get first order derivative
     * 
     * @return derivative as an {@link TrigonometricPolynomialPrimitive}
     */
    public TrigonometricPolynomialPrimitive polynomialDerivative() {
        return polynomialDerivative(this);
    }

    /**
     * Get first order derivative
     * 
     * @param poly
     *        polynomial to differentiate
     * @return first order polynomial derivative as a {@link TrigonometricPolynomialFunction}
     */
    private static TrigonometricPolynomialPrimitive polynomialDerivative(
            final TrigonometricPolynomialPrimitive poly) {
        return new TrigonometricPolynomialPrimitive(poly.getLinearPolynomial().polynomialDerivative(),
            poly.getTrigonometricPolynomial().polynomialDerivative());
    }

    /**
     * Get n<sup>th</sup> order derivative
     * 
     * @param n
     *        order of derivative (n > 0)
     * @return n<sup>th</sup> order polynomial derivative as a {@link TrigonometricPolynomialFunction}
     */
    public TrigonometricPolynomialPrimitive polynomialDerivative(final int n) {
        if (n == 1) {
            return this.polynomialDerivative();
        } else if (n > 1) {
            return this.polynomialDerivative().polynomialDerivative(n - 1);
        } else {
            throw new MathIllegalArgumentException(PatriusMessages.NUMBER_TOO_SMALL, n);
        }
    }

    /**
     * Add a scalar to a {@link TrigonometricPolynomialPrimitive}
     * 
     * @param scalar
     *        for addition
     * @return resulting {@link TrigonometricPolynomialPrimitive}
     */
    public TrigonometricPolynomialPrimitive scalarAdd(final double scalar) {
        return new TrigonometricPolynomialPrimitive(this.linPolynomial, this.triPolynomial.scalarAdd(scalar));
    }

    /**
     * Divide by a scalar
     * 
     * @param scalar
     *        to divide polynomial by
     * @return polynomial divide by scalar
     */
    public TrigonometricPolynomialPrimitive scalarDivide(final double scalar) {
        if (Precision.equals(scalar, 0, 1)) {
            throw new MathIllegalArgumentException(PatriusMessages.ZERO_NOT_ALLOWED, scalar);
        } else {
            return this.scalarMultiply(MathLib.divide(1., scalar));
        }
    }

    /**
     * Multiply {@link TrigonometricPolynomialPrimitive} by a scalar
     * 
     * @param scalar
     *        for multiplication
     * @return resulting {@link TrigonometricPolynomialPrimitive}
     */
    public TrigonometricPolynomialPrimitive scalarMultiply(final double scalar) {
        return new TrigonometricPolynomialPrimitive(this.linPolynomial.multiply(
            new PolynomialFunction(new double[] { scalar })), this.triPolynomial.scalarMultiply(scalar));
    }

    /**
     * Subtract a scalar
     * 
     * @param scalar
     *        to subtract
     * @return new {@link TrigonometricPolynomialFunction}
     */
    public TrigonometricPolynomialPrimitive scalarSubtract(final double scalar) {
        return this.scalarAdd(-scalar);
    }

    /**
     * Subtract two {@link TrigonometricPolynomialPrimitive}
     * 
     * @param poly
     *        to Subtract
     * @return resulting {@link TrigonometricPolynomialPrimitive}
     */
    public TrigonometricPolynomialPrimitive subtract(final TrigonometricPolynomialPrimitive poly) {
        return this.add(poly.negate());
    }

    /**
     * Subtract a {@link PolynomialFunction}
     * 
     * @param poly
     *        to Subtract
     * @return resulting {@link TrigonometricPolynomialPrimitive}
     */
    public TrigonometricPolynomialPrimitive subtract(final PolynomialFunction poly) {
        return this.add(poly.negate());
    }

    /**
     * Subtract a {@link TrigonometricPolynomialFunction}
     * 
     * @param poly
     *        to Subtract
     * @return resulting {@link TrigonometricPolynomialPrimitive}
     */
    public TrigonometricPolynomialPrimitive subtract(final TrigonometricPolynomialFunction poly) {
        return this.add(poly.negate());
    }

    /**
     * Get String representation of polynomial
     * 
     * @return string
     */
    @Override
    public String toString() {
        final StringBuffer buf = new StringBuffer();
        buf.append(this.linPolynomial.toString());
        buf.append(this.triPolynomial.toString());
        return buf.toString();
    }

    /**
     * Get value at given abscissa
     * 
     * @param x
     *        abscissa
     * @return value a given abscissa
     */
    @Override
    public double value(final double x) {
        return evaluate(this, x);
    }

    /**
     * Get value of derivative
     * 
     * @param n
     *        order of derivative
     * @param x
     *        abscissa
     * @return value of derivative at abscissa
     */
    public double value(final int n, final double x) {
        return this.polynomialDerivative(n).value(x);
    }

    /** {@inheritDoc} */
    @Override
    public DerivativeStructure value(final DerivativeStructure t) {
        final int freeParams = t.getFreeParameters();
        if (freeParams != 1) {
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
