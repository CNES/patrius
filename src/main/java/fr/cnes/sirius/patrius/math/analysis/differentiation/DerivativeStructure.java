/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 *
 * Copyright 2011-2017 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * HISTORY
 * VERSION:4.3:DM:DM-2089:15/05/2019:[PATRIUS] passage a Java 8
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.differentiation;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.Field;
import fr.cnes.sirius.patrius.math.FieldElement;
import fr.cnes.sirius.patrius.math.RealFieldElement;
import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooLargeException;
import fr.cnes.sirius.patrius.math.util.MathArrays;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;

// CHECKSTYLE: stop MagicNumber check
// Reason: model - Commons-Math code

/**
 * Class representing both the value and the differentials of a function.
 * <p>
 * This class is the workhorse of the differentiation package.
 * </p>
 * <p>
 * This class is an implementation of the extension to Rall's numbers described in Dan Kalman's paper <a
 * href="http://www.math.american.edu/People/kalman/pdffiles/mmgautodiff.pdf">Doubly Recursive Multivariate Automatic
 * Differentiation</a>, Mathematics Magazine, vol. 75, no. 3, June 2002.
 * </p>
 * . Rall's numbers are an extension to the real numbers used
 * throughout mathematical expressions; they hold the derivative together with the
 * value of a function. Dan Kalman's derivative structures hold all partial derivatives
 * up to any specified order, with respect to any number of free parameters. Rall's
 * numbers therefore can be seen as derivative structures for order one derivative and
 * one free parameter, and real numbers can be seen as derivative structures with zero
 * order derivative and no free parameters.</p>
 * <p>
 * {@link DerivativeStructure} instances can be used directly thanks to the arithmetic operators to the mathematical
 * functions provided as static methods by this class (+, -, *, /, %, sin, cos ...).
 * </p>
 * <p>
 * Implementing complex expressions by hand using these classes is a tedious and error-prone task but has the advantage
 * of having no limitation on the derivation order despite no requiring users to compute the derivatives by themselves.
 * Implementing complex expression can also be done by developing computation code using standard primitive double
 * values and to use {@link UnivariateFunctionDifferentiator differentiators} to create the {@link DerivativeStructure}
 * -based instances. This method is simpler but may be limited in the accuracy and derivation orders and may be
 * computationally intensive (this is typically the case for {@link FiniteDifferencesDifferentiator finite differences
 * differentiator}.
 * </p>
 * <p>
 * Instances of this class are guaranteed to be immutable.
 * </p>
 * 
 * @see DSCompiler
 * @version $Id: DerivativeStructure.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.1
 */
public class DerivativeStructure implements RealFieldElement<DerivativeStructure>, Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 20120730L;

    /** Compiler for the current dimensions. */
    private transient DSCompiler compiler;

    /** Combined array holding all values. */
    private final double[] data;

    /**
     * Build an instance with all values and derivatives set to 0.
     * 
     * @param compilerIn
     *        compiler to use for computation
     */
    private DerivativeStructure(final DSCompiler compilerIn) {
        this.compiler = compilerIn;
        this.data = new double[compilerIn.getSize()];
    }

    /**
     * Build an instance with all values and derivatives set to 0.
     * 
     * @param parameters
     *        number of free parameters
     * @param order
     *        derivation order
     */
    public DerivativeStructure(final int parameters, final int order) {
        this(DSCompiler.getCompiler(parameters, order));
    }

    /**
     * Build an instance representing a constant value.
     * 
     * @param parameters
     *        number of free parameters
     * @param order
     *        derivation order
     * @param value
     *        value of the constant
     * @see #DerivativeStructure(int, int, int, double)
     */
    public DerivativeStructure(final int parameters, final int order, final double value) {
        this(parameters, order);
        this.data[0] = value;
    }

    /**
     * Build an instance representing a variable.
     * <p>
     * Instances built using this constructor are considered to be the free variables with respect to which
     * differentials are computed. As such, their differential with respect to themselves is +1.
     * </p>
     * 
     * @param parameters
     *        number of free parameters
     * @param order
     *        derivation order
     * @param index
     *        index of the variable (from 0 to {@code parameters - 1})
     * @param value
     *        value of the variable
     * @exception NumberIsTooLargeException
     *            if {@code index >= parameters}.
     * @see #DerivativeStructure(int, int, double)
     */
    public DerivativeStructure(final int parameters, final int order,
                               final int index, final double value) {
        this(parameters, order, value);

        if (index >= parameters) {
            throw new NumberIsTooLargeException(index, parameters, false);
        }

        if (order > 0) {
            // the derivative of the variable with respect to itself is 1.
            this.data[DSCompiler.getCompiler(index, order).getSize()] = 1.0;
        }

    }

    /**
     * Linear combination constructor.
     * The derivative structure built will be a1 * ds1 + a2 * ds2
     * 
     * @param a1
     *        first scale factor
     * @param ds1
     *        first base (unscaled) derivative structure
     * @param a2
     *        second scale factor
     * @param ds2
     *        second base (unscaled) derivative structure
     * @exception DimensionMismatchException
     *            if number of free parameters or orders are inconsistent
     */
    public DerivativeStructure(final double a1, final DerivativeStructure ds1,
                               final double a2, final DerivativeStructure ds2) {
        this(ds1.compiler);
        this.compiler.checkCompatibility(ds2.compiler);
        this.compiler.linearCombination(a1, ds1.data, 0, a2, ds2.data, 0, this.data, 0);
    }

    /**
     * Linear combination constructor.
     * The derivative structure built will be a1 * ds1 + a2 * ds2 + a3 * ds3
     * 
     * @param a1
     *        first scale factor
     * @param ds1
     *        first base (unscaled) derivative structure
     * @param a2
     *        second scale factor
     * @param ds2
     *        second base (unscaled) derivative structure
     * @param a3
     *        third scale factor
     * @param ds3
     *        third base (unscaled) derivative structure
     * @exception DimensionMismatchException
     *            if number of free parameters or orders are inconsistent
     */
    public DerivativeStructure(final double a1, final DerivativeStructure ds1,
                               final double a2, final DerivativeStructure ds2,
                               final double a3, final DerivativeStructure ds3) {
        this(ds1.compiler);
        this.compiler.checkCompatibility(ds2.compiler);
        this.compiler.checkCompatibility(ds3.compiler);
        this.compiler.linearCombination(a1, ds1.data, 0, a2, ds2.data, 0, a3, ds3.data, 0, this.data, 0);
    }

    /**
     * Linear combination constructor.
     * The derivative structure built will be a1 * ds1 + a2 * ds2 + a3 * ds3 + a4 * ds4
     * 
     * @param a1
     *        first scale factor
     * @param ds1
     *        first base (unscaled) derivative structure
     * @param a2
     *        second scale factor
     * @param ds2
     *        second base (unscaled) derivative structure
     * @param a3
     *        third scale factor
     * @param ds3
     *        third base (unscaled) derivative structure
     * @param a4
     *        fourth scale factor
     * @param ds4
     *        fourth base (unscaled) derivative structure
     * @exception DimensionMismatchException
     *            if number of free parameters or orders are inconsistent
     */
    public DerivativeStructure(final double a1, final DerivativeStructure ds1,
                               final double a2, final DerivativeStructure ds2,
                               final double a3, final DerivativeStructure ds3,
                               final double a4, final DerivativeStructure ds4) {
        this(ds1.compiler);
        this.compiler.checkCompatibility(ds2.compiler);
        this.compiler.checkCompatibility(ds3.compiler);
        this.compiler.checkCompatibility(ds4.compiler);
        this.compiler.linearCombination(a1, ds1.data, 0, a2, ds2.data, 0,
            a3, ds3.data, 0, a4, ds4.data, 0,
            this.data, 0);
    }

    /**
     * Build an instance from all its derivatives.
     * 
     * @param parameters
     *        number of free parameters
     * @param order
     *        derivation order
     * @param derivatives
     *        derivatives sorted according to {@link DSCompiler#getPartialDerivativeIndex(int...)}
     * @exception DimensionMismatchException
     *            if derivatives array does not match the {@link DSCompiler#getSize() size} expected by the compiler
     * @see #getAllDerivatives()
     */
    public DerivativeStructure(final int parameters, final int order, final double... derivatives) {
        this(parameters, order);
        if (derivatives.length != this.data.length) {
            throw new DimensionMismatchException(derivatives.length, this.data.length);
        }
        System.arraycopy(derivatives, 0, this.data, 0, this.data.length);
    }

    /**
     * Copy constructor.
     * 
     * @param ds
     *        instance to copy
     */
    private DerivativeStructure(final DerivativeStructure ds) {
        this.compiler = ds.compiler;
        this.data = ds.data.clone();
    }

    /**
     * Get the number of free parameters.
     * 
     * @return number of free parameters
     */
    public int getFreeParameters() {
        return this.compiler.getFreeParameters();
    }

    /**
     * Get the derivation order.
     * 
     * @return derivation order
     */
    public int getOrder() {
        return this.compiler.getOrder();
    }

    /**
     * Create a constant compatible with instance order and number of parameters.
     * <p>
     * This method is a convenience factory method, it simply calls
     * {@code new DerivativeStructure(getFreeParameters(), getOrder(), c)}
     * </p>
     * 
     * @param c
     *        value of the constant
     * @return a constant compatible with instance order and number of parameters
     * @see #DerivativeStructure(int, int, double)
     * @since 3.1
     */
    public DerivativeStructure createConstant(final double c) {
        return new DerivativeStructure(this.getFreeParameters(), this.getOrder(), c);
    }

    /**
     * {@inheritDoc}
     * 
     * @since 3.2
     */
    @Override
    public double getReal() {
        return this.data[0];
    }

    /**
     * Get the value part of the derivative structure.
     * 
     * @return value part of the derivative structure
     * @see #getPartialDerivative(int...)
     */
    public double getValue() {
        return this.data[0];
    }

    /**
     * Get a partial derivative.
     * 
     * @param orders
     *        derivation orders with respect to each variable (if all orders are 0,
     *        the value is returned)
     * @return partial derivative
     * @see #getValue()
     * @exception DimensionMismatchException
     *            if the numbers of variables does not
     *            match the instance
     * @exception NumberIsTooLargeException
     *            if sum of derivation orders is larger
     *            than the instance limits
     */
    public double getPartialDerivative(final int... orders) {
        return this.data[this.compiler.getPartialDerivativeIndex(orders)];
    }

    /**
     * Get all partial derivatives.
     * 
     * @return a fresh copy of partial derivatives, in an array sorted according to
     *         {@link DSCompiler#getPartialDerivativeIndex(int...)}
     */
    public double[] getAllDerivatives() {
        return this.data.clone();
    }

    /**
     * '+' operator.
     * 
     * @param a
     *        right hand side parameter of the operator
     * @return this+a
     */
    @Override
    public DerivativeStructure add(final double a) {
        final DerivativeStructure ds = new DerivativeStructure(this);
        ds.data[0] += a;
        return ds;
    }

    /**
     * '+' operator.
     * 
     * @param a
     *        right hand side parameter of the operator
     * @return this+a
     * @exception DimensionMismatchException
     *            if number of free parameters or orders are inconsistent
     */
    @Override
    public DerivativeStructure add(final DerivativeStructure a) {
        this.compiler.checkCompatibility(a.compiler);
        final DerivativeStructure ds = new DerivativeStructure(this);
        this.compiler.add(this.data, 0, a.data, 0, ds.data, 0);
        return ds;
    }

    /**
     * '-' operator.
     * 
     * @param a
     *        right hand side parameter of the operator
     * @return this-a
     */
    @Override
    public DerivativeStructure subtract(final double a) {
        return this.add(-a);
    }

    /**
     * '-' operator.
     * 
     * @param a
     *        right hand side parameter of the operator
     * @return this-a
     * @exception DimensionMismatchException
     *            if number of free parameters or orders are inconsistent
     */
    @Override
    public DerivativeStructure subtract(final DerivativeStructure a) {
        this.compiler.checkCompatibility(a.compiler);
        final DerivativeStructure ds = new DerivativeStructure(this);
        this.compiler.subtract(this.data, 0, a.data, 0, ds.data, 0);
        return ds;
    }

    /** {@inheritDoc} */
    @Override
    public DerivativeStructure multiply(final int n) {
        return this.multiply((double) n);
    }

    /**
     * '&times;' operator.
     * 
     * @param a
     *        right hand side parameter of the operator
     * @return this&times;a
     */
    @Override
    public DerivativeStructure multiply(final double a) {
        final DerivativeStructure ds = new DerivativeStructure(this);
        for (int i = 0; i < ds.data.length; ++i) {
            ds.data[i] *= a;
        }
        return ds;
    }

    /**
     * '&times;' operator.
     * 
     * @param a
     *        right hand side parameter of the operator
     * @return this&times;a
     * @exception DimensionMismatchException
     *            if number of free parameters or orders are inconsistent
     */
    @Override
    public DerivativeStructure multiply(final DerivativeStructure a) {
        this.compiler.checkCompatibility(a.compiler);
        final DerivativeStructure result = new DerivativeStructure(this.compiler);
        this.compiler.multiply(this.data, 0, a.data, 0, result.data, 0);
        return result;
    }

    /**
     * '/' operator.
     * 
     * @param a
     *        right hand side parameter of the operator
     * @return this/a
     */
    @Override
    public DerivativeStructure divide(final double a) {
        final DerivativeStructure ds = new DerivativeStructure(this);
        for (int i = 0; i < ds.data.length; ++i) {
            ds.data[i] /= a;
        }
        return ds;
    }

    /**
     * '/' operator.
     * 
     * @param a
     *        right hand side parameter of the operator
     * @return this/a
     * @exception DimensionMismatchException
     *            if number of free parameters or orders are inconsistent
     */
    @Override
    public DerivativeStructure divide(final DerivativeStructure a) {
        this.compiler.checkCompatibility(a.compiler);
        final DerivativeStructure result = new DerivativeStructure(this.compiler);
        this.compiler.divide(this.data, 0, a.data, 0, result.data, 0);
        return result;
    }

    /**
     * '%' operator.
     * 
     * @param a
     *        right hand side parameter of the operator
     * @return this%a
     */
    @Override
    public DerivativeStructure remainder(final double a) {
        final DerivativeStructure ds = new DerivativeStructure(this);
        ds.data[0] = ds.data[0] % a;
        return ds;
    }

    /**
     * '%' operator.
     * 
     * @param a
     *        right hand side parameter of the operator
     * @return this%a
     * @exception DimensionMismatchException
     *            if number of free parameters or orders are inconsistent
     */
    @Override
    public DerivativeStructure remainder(final DerivativeStructure a) {
        this.compiler.checkCompatibility(a.compiler);
        final DerivativeStructure result = new DerivativeStructure(this.compiler);
        this.compiler.remainder(this.data, 0, a.data, 0, result.data, 0);
        return result;
    }

    /**
     * unary '-' operator.
     * 
     * @return -this
     */
    @Override
    public DerivativeStructure negate() {
        final DerivativeStructure ds = new DerivativeStructure(this.compiler);
        for (int i = 0; i < ds.data.length; ++i) {
            ds.data[i] = -this.data[i];
        }
        return ds;
    }

    /**
     * absolute value.
     * 
     * @return abs(this)
     */
    @Override
    public DerivativeStructure abs() {
        if (Double.doubleToLongBits(this.data[0]) < 0) {
            // we use the bits representation to also handle -0.0
            return this.negate();
        } else {
            return this;
        }
    }

    /**
     * Get the smallest whole number larger than instance.
     * 
     * @return ceil(this)
     */
    @Override
    public DerivativeStructure ceil() {
        return new DerivativeStructure(this.compiler.getFreeParameters(),
            this.compiler.getOrder(),
            MathLib.ceil(this.data[0]));
    }

    /**
     * Get the largest whole number smaller than instance.
     * 
     * @return floor(this)
     */
    @Override
    public DerivativeStructure floor() {
        return new DerivativeStructure(this.compiler.getFreeParameters(),
            this.compiler.getOrder(),
            MathLib.floor(this.data[0]));
    }

    /**
     * Get the whole number that is the nearest to the instance, or the even one if x is exactly half way between two
     * integers.
     * 
     * @return a double number r such that r is an integer r - 0.5 <= this <= r + 0.5
     */
    @Override
    public DerivativeStructure rint() {
        return new DerivativeStructure(this.compiler.getFreeParameters(),
            this.compiler.getOrder(),
            MathLib.rint(this.data[0]));
    }

    /**
     * Get the closest long to instance value.
     * 
     * @return closest long to {@link #getValue()}
     */
    @Override
    public long round() {
        return MathLib.round(this.data[0]);
    }

    /**
     * Compute the signum of the instance.
     * The signum is -1 for negative numbers, +1 for positive numbers and 0 otherwise
     * 
     * @return -1.0, -0.0, +0.0, +1.0 or NaN depending on sign of a
     */
    @Override
    public DerivativeStructure signum() {
        return new DerivativeStructure(this.compiler.getFreeParameters(),
            this.compiler.getOrder(),
            MathLib.signum(this.data[0]));
    }

    /**
     * {@inheritDoc}
     * 
     * @since 3.1
     */
    @Override
    public DerivativeStructure copySign(final DerivativeStructure sign) {
        final long m = Double.doubleToLongBits(this.data[0]);
        final long s = Double.doubleToLongBits(sign.data[0]);
        if ((m >= 0 && s >= 0) || (m < 0 && s < 0)) {
            // Sign is currently OK
            return this;
        }
        // flip sign
        return this.negate();
    }

    /**
     * Returns the instance with the sign of the argument.
     * A NaN {@code sign} argument is treated as positive.
     * 
     * @param sign
     *        the sign for the returned value
     * @return the instance with the same sign as the {@code sign} argument
     */
    @Override
    public DerivativeStructure copySign(final double sign) {
        final long m = Double.doubleToLongBits(this.data[0]);
        final long s = Double.doubleToLongBits(sign);
        if ((m >= 0 && s >= 0) || (m < 0 && s < 0)) {
            // Sign is currently OK
            return this;
        }
        // flip sign
        return this.negate();
    }

    /**
     * Return the exponent of the instance value, removing the bias.
     * <p>
     * For double numbers of the form 2<sup>x</sup>, the unbiased exponent is exactly x.
     * </p>
     * 
     * @return exponent for instance in IEEE754 representation, without bias
     */
    public int getExponent() {
        return MathLib.getExponent(this.data[0]);
    }

    /**
     * Multiply the instance by a power of 2.
     * 
     * @param n
     *        power of 2
     * @return this &times; 2<sup>n</sup>
     */
    @Override
    public DerivativeStructure scalb(final int n) {
        final DerivativeStructure ds = new DerivativeStructure(this.compiler);
        for (int i = 0; i < ds.data.length; ++i) {
            ds.data[i] = MathLib.scalb(this.data[i], n);
        }
        return ds;
    }

    /**
     * {@inheritDoc}
     * 
     * @exception DimensionMismatchException
     *            if number of free parameters
     *            or orders do not match
     * @since 3.1
     */
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Commons-Math code kept as such
    @Override
    public DerivativeStructure hypot(final DerivativeStructure y) {
        // CHECKSTYLE: resume ReturnCount check

        this.compiler.checkCompatibility(y.compiler);

        if (Double.isInfinite(this.data[0]) || Double.isInfinite(y.data[0])) {
            return new DerivativeStructure(this.compiler.getFreeParameters(),
                this.compiler.getFreeParameters(),
                Double.POSITIVE_INFINITY);
        } else if (Double.isNaN(this.data[0]) || Double.isNaN(y.data[0])) {
            return new DerivativeStructure(this.compiler.getFreeParameters(),
                this.compiler.getFreeParameters(),
                Double.NaN);
        } else {

            final int expX = this.getExponent();
            final int expY = y.getExponent();
            if (expX > expY + 27) {
                // y is neglectible with respect to x
                return this.abs();
            } else if (expY > expX + 27) {
                // x is neglectible with respect to y
                return y.abs();
            } else {

                // find an intermediate scale to avoid both overflow and underflow
                final int middleExp = (expX + expY) / 2;

                // scale parameters without losing precision
                final DerivativeStructure scaledX = this.scalb(-middleExp);
                final DerivativeStructure scaledY = y.scalb(-middleExp);

                // compute scaled hypotenuse
                final DerivativeStructure scaledH =
                    scaledX.multiply(scaledX).add(scaledY.multiply(scaledY)).sqrt();

                // remove scaling
                return scaledH.scalb(middleExp);

            }

        }
    }

    /**
     * Returns the hypotenuse of a triangle with sides {@code x} and {@code y} -
     * sqrt(<i>x</i><sup>2</sup>&nbsp;+<i>y</i><sup>2</sup>)<br/>
     * avoiding intermediate overflow or underflow.
     * 
     * <ul>
     * <li>If either argument is infinite, then the result is positive infinity.</li>
     * <li>else, if either argument is NaN then the result is NaN.</li>
     * </ul>
     * 
     * @param x
     *        a value
     * @param y
     *        a value
     * @return sqrt(<i>x</i><sup>2</sup>&nbsp;+<i>y</i><sup>2</sup>)
     * @exception DimensionMismatchException
     *            if number of free parameters or orders are inconsistent
     */
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Commons-Math code kept as such
    public static DerivativeStructure hypot(final DerivativeStructure x, final DerivativeStructure y) {
        // CHECKSTYLE: resume ReturnCount check

        x.compiler.checkCompatibility(y.compiler);

        if (Double.isInfinite(x.data[0]) || Double.isInfinite(y.data[0])) {
            return new DerivativeStructure(x.compiler.getFreeParameters(),
                x.compiler.getFreeParameters(),
                Double.POSITIVE_INFINITY);
        } else if (Double.isNaN(x.data[0]) || Double.isNaN(y.data[0])) {
            return new DerivativeStructure(x.compiler.getFreeParameters(),
                x.compiler.getFreeParameters(),
                Double.NaN);
        } else {

            final int expX = x.getExponent();
            final int expY = y.getExponent();
            if (expX > expY + 27) {
                // y is neglectible with respect to x
                return x.abs();
            } else if (expY > expX + 27) {
                // x is neglectible with respect to y
                return y.abs();
            } else {

                // find an intermediate scale to avoid both overflow and underflow
                final int middleExp = (expX + expY) / 2;

                // scale parameters without losing precision
                final DerivativeStructure scaledX = x.scalb(-middleExp);
                final DerivativeStructure scaledY = y.scalb(-middleExp);

                // compute scaled hypotenuse
                final DerivativeStructure scaledH =
                    scaledX.multiply(scaledX).add(scaledY.multiply(scaledY)).sqrt();

                // remove scaling
                return scaledH.scalb(middleExp);

            }

        }
    }

    /**
     * Compute composition of the instance by a univariate function.
     * 
     * @param f
     *        array of value and derivatives of the function at
     *        the current point (i.e. [f({@link #getValue()}),
     *        f'({@link #getValue()}), f''({@link #getValue()})...]).
     * @return f(this)
     * @exception DimensionMismatchException
     *            if the number of derivatives
     *            in the array is not equal to {@link #getOrder() order} + 1
     */
    public DerivativeStructure compose(final double... f) {
        if (f.length != this.getOrder() + 1) {
            throw new DimensionMismatchException(f.length, this.getOrder() + 1);
        }
        final DerivativeStructure result = new DerivativeStructure(this.compiler);
        this.compiler.compose(this.data, 0, f, result.data, 0);
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public DerivativeStructure reciprocal() {
        final DerivativeStructure result = new DerivativeStructure(this.compiler);
        this.compiler.pow(this.data, 0, -1, result.data, 0);
        return result;
    }

    /**
     * Square root.
     * 
     * @return square root of the instance
     */
    @Override
    public DerivativeStructure sqrt() {
        return this.rootN(2);
    }

    /**
     * Cubic root.
     * 
     * @return cubic root of the instance
     */
    @Override
    public DerivativeStructure cbrt() {
        return this.rootN(3);
    }

    /**
     * N<sup>th</sup> root.
     * 
     * @param n
     *        order of the root
     * @return n<sup>th</sup> root of the instance
     */
    @Override
    public DerivativeStructure rootN(final int n) {
        final DerivativeStructure result = new DerivativeStructure(this.compiler);
        this.compiler.rootN(this.data, 0, n, result.data, 0);
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public Field<DerivativeStructure> getField() {
        return new Field<DerivativeStructure>(){

            /** {@inheritDoc} */
            @Override
            public DerivativeStructure getZero() {
                return new DerivativeStructure(DerivativeStructure.this.compiler.getFreeParameters(),
                    DerivativeStructure.this.compiler.getOrder(), 0.0);
            }

            /** {@inheritDoc} */
            @Override
            public DerivativeStructure getOne() {
                return new DerivativeStructure(DerivativeStructure.this.compiler.getFreeParameters(),
                    DerivativeStructure.this.compiler.getOrder(), 1.0);
            }

            /** {@inheritDoc} */
            @Override
            public Class<? extends FieldElement<DerivativeStructure>> getRuntimeClass() {
                return DerivativeStructure.class;
            }

        };
    }

    /**
     * Compute a<sup>x</sup> where a is a double and x a {@link DerivativeStructure}
     * 
     * @param a
     *        number to exponentiate
     * @param x
     *        power to apply
     * @return a<sup>x</sup>
     * @since 3.1
     */
    public static DerivativeStructure pow(final double a, final DerivativeStructure x) {
        final DerivativeStructure result = new DerivativeStructure(x.compiler);
        x.compiler.pow(a, x.data, 0, result.data, 0);
        return result;
    }

    /**
     * Power operation.
     * 
     * @param p
     *        power to apply
     * @return this<sup>p</sup>
     */
    @Override
    public DerivativeStructure pow(final double p) {
        final DerivativeStructure result = new DerivativeStructure(this.compiler);
        this.compiler.pow(this.data, 0, p, result.data, 0);
        return result;
    }

    /**
     * Integer power operation.
     * 
     * @param n
     *        power to apply
     * @return this<sup>n</sup>
     */
    @Override
    public DerivativeStructure pow(final int n) {
        final DerivativeStructure result = new DerivativeStructure(this.compiler);
        this.compiler.pow(this.data, 0, n, result.data, 0);
        return result;
    }

    /**
     * Power operation.
     * 
     * @param e
     *        exponent
     * @return this<sup>e</sup>
     * @exception DimensionMismatchException
     *            if number of free parameters or orders are inconsistent
     */
    @Override
    public DerivativeStructure pow(final DerivativeStructure e) {
        this.compiler.checkCompatibility(e.compiler);
        final DerivativeStructure result = new DerivativeStructure(this.compiler);
        this.compiler.pow(this.data, 0, e.data, 0, result.data, 0);
        return result;
    }

    /**
     * Exponential.
     * 
     * @return exponential of the instance
     */
    @Override
    public DerivativeStructure exp() {
        final DerivativeStructure result = new DerivativeStructure(this.compiler);
        this.compiler.exp(this.data, 0, result.data, 0);
        return result;
    }

    /**
     * Exponential minus 1.
     * 
     * @return exponential minus one of the instance
     */
    @Override
    public DerivativeStructure expm1() {
        final DerivativeStructure result = new DerivativeStructure(this.compiler);
        this.compiler.expm1(this.data, 0, result.data, 0);
        return result;
    }

    /**
     * Natural logarithm.
     * 
     * @return logarithm of the instance
     */
    @Override
    public DerivativeStructure log() {
        final DerivativeStructure result = new DerivativeStructure(this.compiler);
        this.compiler.log(this.data, 0, result.data, 0);
        return result;
    }

    /**
     * Shifted natural logarithm.
     * 
     * @return logarithm of one plus the instance
     */
    @Override
    public DerivativeStructure log1p() {
        final DerivativeStructure result = new DerivativeStructure(this.compiler);
        this.compiler.log1p(this.data, 0, result.data, 0);
        return result;
    }

    /**
     * Base 10 logarithm.
     * 
     * @return base 10 logarithm of the instance
     */
    public DerivativeStructure log10() {
        final DerivativeStructure result = new DerivativeStructure(this.compiler);
        this.compiler.log10(this.data, 0, result.data, 0);
        return result;
    }

    /**
     * Cosine operation.
     * 
     * @return cos(this)
     */
    @Override
    public DerivativeStructure cos() {
        final DerivativeStructure result = new DerivativeStructure(this.compiler);
        this.compiler.cos(this.data, 0, result.data, 0);
        return result;
    }

    /**
     * Sine operation.
     * 
     * @return sin(this)
     */
    @Override
    public DerivativeStructure sin() {
        final DerivativeStructure result = new DerivativeStructure(this.compiler);
        this.compiler.sin(this.data, 0, result.data, 0);
        return result;
    }

    /**
     * Tangent operation.
     * 
     * @return tan(this)
     */
    @Override
    public DerivativeStructure tan() {
        final DerivativeStructure result = new DerivativeStructure(this.compiler);
        this.compiler.tan(this.data, 0, result.data, 0);
        return result;
    }

    /**
     * Arc cosine operation.
     * 
     * @return acos(this)
     */
    @Override
    public DerivativeStructure acos() {
        final DerivativeStructure result = new DerivativeStructure(this.compiler);
        this.compiler.acos(this.data, 0, result.data, 0);
        return result;
    }

    /**
     * Arc sine operation.
     * 
     * @return asin(this)
     */
    @Override
    public DerivativeStructure asin() {
        final DerivativeStructure result = new DerivativeStructure(this.compiler);
        this.compiler.asin(this.data, 0, result.data, 0);
        return result;
    }

    /**
     * Arc tangent operation.
     * 
     * @return atan(this)
     */
    @Override
    public DerivativeStructure atan() {
        final DerivativeStructure result = new DerivativeStructure(this.compiler);
        this.compiler.atan(this.data, 0, result.data, 0);
        return result;
    }

    /**
     * {@inheritDoc}
     * 
     * @since 3.1
     */
    @Override
    public DerivativeStructure atan2(final DerivativeStructure x) {
        this.compiler.checkCompatibility(x.compiler);
        final DerivativeStructure result = new DerivativeStructure(this.compiler);
        this.compiler.atan2(this.data, 0, x.data, 0, result.data, 0);
        return result;
    }

    /**
     * Two arguments arc tangent operation.
     * 
     * @param y
     *        first argument of the arc tangent
     * @param x
     *        second argument of the arc tangent
     * @return atan2(y, x)
     * @exception DimensionMismatchException
     *            if number of free parameters or orders are inconsistent
     */
    public static DerivativeStructure atan2(final DerivativeStructure y, final DerivativeStructure x) {
        y.compiler.checkCompatibility(x.compiler);
        final DerivativeStructure result = new DerivativeStructure(y.compiler);
        y.compiler.atan2(y.data, 0, x.data, 0, result.data, 0);
        return result;
    }

    /**
     * Hyperbolic cosine operation.
     * 
     * @return cosh(this)
     */
    @Override
    public DerivativeStructure cosh() {
        final DerivativeStructure result = new DerivativeStructure(this.compiler);
        this.compiler.cosh(this.data, 0, result.data, 0);
        return result;
    }

    /**
     * Hyperbolic sine operation.
     * 
     * @return sinh(this)
     */
    @Override
    public DerivativeStructure sinh() {
        final DerivativeStructure result = new DerivativeStructure(this.compiler);
        this.compiler.sinh(this.data, 0, result.data, 0);
        return result;
    }

    /**
     * Hyperbolic tangent operation.
     * 
     * @return tanh(this)
     */
    @Override
    public DerivativeStructure tanh() {
        final DerivativeStructure result = new DerivativeStructure(this.compiler);
        this.compiler.tanh(this.data, 0, result.data, 0);
        return result;
    }

    /**
     * Inverse hyperbolic cosine operation.
     * 
     * @return acosh(this)
     */
    @Override
    public DerivativeStructure acosh() {
        final DerivativeStructure result = new DerivativeStructure(this.compiler);
        this.compiler.acosh(this.data, 0, result.data, 0);
        return result;
    }

    /**
     * Inverse hyperbolic sine operation.
     * 
     * @return asin(this)
     */
    @Override
    public DerivativeStructure asinh() {
        final DerivativeStructure result = new DerivativeStructure(this.compiler);
        this.compiler.asinh(this.data, 0, result.data, 0);
        return result;
    }

    /**
     * Inverse hyperbolic tangent operation.
     * 
     * @return atanh(this)
     */
    @Override
    public DerivativeStructure atanh() {
        final DerivativeStructure result = new DerivativeStructure(this.compiler);
        this.compiler.atanh(this.data, 0, result.data, 0);
        return result;
    }

    /**
     * Convert radians to degrees, with error of less than 0.5 ULP
     * 
     * @return instance converted into degrees
     */
    public DerivativeStructure toDegrees() {
        final DerivativeStructure ds = new DerivativeStructure(this.compiler);
        for (int i = 0; i < ds.data.length; ++i) {
            ds.data[i] = MathLib.toDegrees(this.data[i]);
        }
        return ds;
    }

    /**
     * Convert degrees to radians, with error of less than 0.5 ULP
     * 
     * @return instance converted into radians
     */
    public DerivativeStructure toRadians() {
        final DerivativeStructure ds = new DerivativeStructure(this.compiler);
        for (int i = 0; i < ds.data.length; ++i) {
            ds.data[i] = MathLib.toRadians(this.data[i]);
        }
        return ds;
    }

    /**
     * Evaluate Taylor expansion a derivative structure.
     * 
     * @param delta
     *        parameters offsets (&Delta;x, &Delta;y, ...)
     * @return value of the Taylor expansion at x + &Delta;x, y + &Delta;y, ...
     */
    public double taylor(final double... delta) {
        return this.compiler.taylor(this.data, 0, delta);
    }

    /**
     * {@inheritDoc}
     * 
     * @exception DimensionMismatchException
     *            if number of free parameters
     *            or orders do not match
     * @since 3.1
     */
    @Override
    public DerivativeStructure linearCombination(final DerivativeStructure[] a, final DerivativeStructure[] b) {

        // compute an accurate value, taking care of cancellations
        final double[] aDouble = new double[a.length];
        for (int i = 0; i < a.length; ++i) {
            // fill aDouble values from a array
            aDouble[i] = a[i].getValue();
        }
        final double[] bDouble = new double[b.length];
        for (int i = 0; i < b.length; ++i) {
            // fill bDouble values from b array
            bDouble[i] = b[i].getValue();
        }
        // Compute a linear combination with aDouble & bDouble
        final double accurateValue = MathArrays.linearCombination(aDouble, bDouble);

        // compute a simple value, with all partial derivatives
        DerivativeStructure simpleValue = a[0].getField().getZero();
        for (int i = 0; i < a.length; ++i) {
            simpleValue = simpleValue.add(a[i].multiply(b[i]));
        }

        // create a result with accurate value and all derivatives (not necessarily as accurate as the value)
        final double[] all = simpleValue.getAllDerivatives();
        all[0] = accurateValue;
        return new DerivativeStructure(simpleValue.getFreeParameters(), simpleValue.getOrder(), all);

    }

    /**
     * {@inheritDoc}
     * 
     * @exception DimensionMismatchException
     *            if number of free parameters
     *            or orders do not match
     * @since 3.1
     */
    @Override
    public DerivativeStructure linearCombination(final double[] a, final DerivativeStructure[] b) {

        // compute an accurate value, taking care of cancellations
        final double[] bDouble = new double[b.length];
        for (int i = 0; i < b.length; ++i) {
            bDouble[i] = b[i].getValue();
        }
        final double accurateValue = MathArrays.linearCombination(a, bDouble);

        // compute a simple value, with all partial derivatives
        DerivativeStructure simpleValue = b[0].getField().getZero();
        for (int i = 0; i < a.length; ++i) {
            simpleValue = simpleValue.add(b[i].multiply(a[i]));
        }

        // create a result with accurate value and all derivatives (not necessarily as accurate as the value)
        final double[] all = simpleValue.getAllDerivatives();
        all[0] = accurateValue;
        return new DerivativeStructure(simpleValue.getFreeParameters(), simpleValue.getOrder(), all);

    }

    /**
     * {@inheritDoc}
     * 
     * @exception DimensionMismatchException
     *            if number of free parameters
     *            or orders do not match
     * @since 3.1
     */
    @Override
    public DerivativeStructure linearCombination(final DerivativeStructure a1, final DerivativeStructure b1,
                                                 final DerivativeStructure a2, final DerivativeStructure b2) {

        // compute an accurate value, taking care of cancellations
        final double accurateValue = MathArrays.linearCombination(a1.getValue(), b1.getValue(),
            a2.getValue(), b2.getValue());

        // compute a simple value, with all partial derivatives
        final DerivativeStructure simpleValue = a1.multiply(b1).add(a2.multiply(b2));

        // create a result with accurate value and all derivatives (not necessarily as accurate as the value)
        final double[] all = simpleValue.getAllDerivatives();
        all[0] = accurateValue;
        return new DerivativeStructure(this.getFreeParameters(), this.getOrder(), all);

    }

    /**
     * {@inheritDoc}
     * 
     * @exception DimensionMismatchException
     *            if number of free parameters
     *            or orders do not match
     * @since 3.1
     */
    @Override
    public DerivativeStructure linearCombination(final double a1, final DerivativeStructure b1,
                                                 final double a2, final DerivativeStructure b2) {

        // compute an accurate value, taking care of cancellations
        final double accurateValue = MathArrays.linearCombination(a1, b1.getValue(),
            a2, b2.getValue());

        // compute a simple value, with all partial derivatives
        final DerivativeStructure simpleValue = b1.multiply(a1).add(b2.multiply(a2));

        // create a result with accurate value and all derivatives (not necessarily as accurate as the value)
        final double[] all = simpleValue.getAllDerivatives();
        all[0] = accurateValue;
        return new DerivativeStructure(this.getFreeParameters(), this.getOrder(), all);

    }

    /**
     * {@inheritDoc}
     * 
     * @exception DimensionMismatchException
     *            if number of free parameters
     *            or orders do not match
     * @since 3.1
     */
    @Override
    public DerivativeStructure linearCombination(final DerivativeStructure a1, final DerivativeStructure b1,
                                                 final DerivativeStructure a2, final DerivativeStructure b2,
                                                 final DerivativeStructure a3, final DerivativeStructure b3) {

        // compute an accurate value, taking care of cancellations
        final double accurateValue = MathArrays.linearCombination(a1.getValue(), b1.getValue(),
            a2.getValue(), b2.getValue(),
            a3.getValue(), b3.getValue());

        // compute a simple value, with all partial derivatives
        final DerivativeStructure simpleValue = a1.multiply(b1).add(a2.multiply(b2)).add(a3.multiply(b3));

        // create a result with accurate value and all derivatives (not necessarily as accurate as the value)
        final double[] all = simpleValue.getAllDerivatives();
        all[0] = accurateValue;
        return new DerivativeStructure(this.getFreeParameters(), this.getOrder(), all);

    }

    /**
     * {@inheritDoc}
     * 
     * @exception DimensionMismatchException
     *            if number of free parameters
     *            or orders do not match
     * @since 3.1
     */
    @Override
    public DerivativeStructure linearCombination(final double a1, final DerivativeStructure b1,
                                                 final double a2, final DerivativeStructure b2,
                                                 final double a3, final DerivativeStructure b3) {

        // compute an accurate value, taking care of cancellations
        final double accurateValue = MathArrays.linearCombination(a1, b1.getValue(),
            a2, b2.getValue(),
            a3, b3.getValue());

        // compute a simple value, with all partial derivatives
        final DerivativeStructure simpleValue = b1.multiply(a1).add(b2.multiply(a2)).add(b3.multiply(a3));

        // create a result with accurate value and all derivatives (not necessarily as accurate as the value)
        final double[] all = simpleValue.getAllDerivatives();
        all[0] = accurateValue;
        return new DerivativeStructure(this.getFreeParameters(), this.getOrder(), all);

    }

    /**
     * {@inheritDoc}
     * 
     * @exception DimensionMismatchException
     *            if number of free parameters
     *            or orders do not match
     * @since 3.1
     */
    @Override
    public DerivativeStructure linearCombination(final DerivativeStructure a1, final DerivativeStructure b1,
                                                 final DerivativeStructure a2, final DerivativeStructure b2,
                                                 final DerivativeStructure a3, final DerivativeStructure b3,
                                                 final DerivativeStructure a4, final DerivativeStructure b4) {

        // compute an accurate value, taking care of cancellations
        final double accurateValue = MathArrays.linearCombination(a1.getValue(), b1.getValue(),
            a2.getValue(), b2.getValue(),
            a3.getValue(), b3.getValue(),
            a4.getValue(), b4.getValue());

        // compute a simple value, with all partial derivatives
        final DerivativeStructure simpleValue = a1.multiply(b1).add(a2.multiply(b2)).add(a3.multiply(b3))
            .add(a4.multiply(b4));

        // create a result with accurate value and all derivatives (not necessarily as accurate as the value)
        final double[] all = simpleValue.getAllDerivatives();
        all[0] = accurateValue;
        return new DerivativeStructure(this.getFreeParameters(), this.getOrder(), all);

    }

    /**
     * {@inheritDoc}
     * 
     * @exception DimensionMismatchException
     *            if number of free parameters
     *            or orders do not match
     * @since 3.1
     */
    @Override
    public DerivativeStructure linearCombination(final double a1, final DerivativeStructure b1,
                                                 final double a2, final DerivativeStructure b2,
                                                 final double a3, final DerivativeStructure b3,
                                                 final double a4, final DerivativeStructure b4) {

        // compute an accurate value, taking care of cancellations
        final double accurateValue = MathArrays.linearCombination(a1, b1.getValue(),
            a2, b2.getValue(),
            a3, b3.getValue(),
            a4, b4.getValue());

        // compute a simple value, with all partial derivatives
        final DerivativeStructure simpleValue = b1.multiply(a1).add(b2.multiply(a2)).add(b3.multiply(a3))
            .add(b4.multiply(a4));

        // create a result with accurate value and all derivatives (not necessarily as accurate as the value)
        final double[] all = simpleValue.getAllDerivatives();
        all[0] = accurateValue;
        return new DerivativeStructure(this.getFreeParameters(), this.getOrder(), all);

    }

    /**
     * Test for the equality of two derivative structures.
     * <p>
     * Derivative structures are considered equal if they have the same number of free parameters, the same derivation
     * order, and the same derivatives.
     * </p>
     * 
     * @param other
     *        Object to test for equality to this
     * @return true if two derivative structures are equal
     * @since 3.1
     */
    @Override
    public boolean equals(final Object other) {

        if (this == other) {
            return true;
        }

        if (other instanceof DerivativeStructure) {
            final DerivativeStructure rhs = (DerivativeStructure) other;
            return (this.getFreeParameters() == rhs.getFreeParameters()) &&
                    (this.getOrder() == rhs.getOrder()) &&
                    MathArrays.equals(this.data, rhs.data);
        }

        return false;

    }

    /**
     * Get a hashCode for the derivative structure.
     * 
     * @return a hash code value for this object
     * @since 3.1
     */
    @Override
    public int hashCode() {
        return 227 + 229 * this.getFreeParameters() + 233 * this.getOrder() + 239 * MathUtils.hash(this.data);
    }

    /**
     * Replace the instance with a data transfer object for serialization.
     * 
     * @return data transfer object that will be serialized
     */
    private Object writeReplace() {
        return new DataTransferObject(this.compiler.getFreeParameters(), this.compiler.getOrder(), this.data);
    }

    /** Internal class used only for serialization. */
    private static class DataTransferObject implements Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = 20120730L;

        /**
         * Number of variables.
         * 
         * @serial
         */
        private final int variables;

        /**
         * Derivation order.
         * 
         * @serial
         */
        private final int order;

        /**
         * Partial derivatives.
         * 
         * @serial
         */
        private final double[] data;

        /**
         * Simple constructor.
         * 
         * @param variablesIn
         *        number of variables
         * @param orderIn
         *        derivation order
         * @param dataIn
         *        partial derivatives
         */
        public DataTransferObject(final int variablesIn, final int orderIn, final double[] dataIn) {
            this.variables = variablesIn;
            this.order = orderIn;
            this.data = dataIn;
        }

        /**
         * Replace the deserialized data transfer object with a {@link DerivativeStructure}.
         * 
         * @return replacement {@link DerivativeStructure}
         */
        private Object readResolve() {
            return new DerivativeStructure(this.variables, this.order, this.data);
        }

    }

    // CHECKSTYLE: resume MagicNumber check
}
