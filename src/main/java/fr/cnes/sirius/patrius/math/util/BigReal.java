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
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.util;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;

import fr.cnes.sirius.patrius.math.Field;
import fr.cnes.sirius.patrius.math.FieldElement;
import fr.cnes.sirius.patrius.math.exception.MathArithmeticException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Arbitrary precision decimal number.
 * <p>
 * This class is a simple wrapper around the standard <code>BigDecimal</code> in order to implement the
 * {@link FieldElement} interface.
 * </p>
 * 
 * @since 2.0
 * @version $Id: BigReal.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class BigReal implements FieldElement<BigReal>, Comparable<BigReal>, Serializable {

    /** A big real representing 0. */
    public static final BigReal ZERO = new BigReal(BigDecimal.ZERO);

    /** A big real representing 1. */
    public static final BigReal ONE = new BigReal(BigDecimal.ONE);

    /** Default scale. */
    private static final int DEFAULT_SCALE = 64;

    /** Serializable version identifier. */
    private static final long serialVersionUID = 4984534880991310382L;

    /** Underlying BigDecimal. */
    private final BigDecimal d;

    /** Rounding mode for divisions. **/
    private RoundingMode roundingMode = RoundingMode.HALF_UP;

    /*** BigDecimal scale ***/
    private int scale = DEFAULT_SCALE;

    /**
     * Build an instance from a BigDecimal.
     * 
     * @param val
     *        value of the instance
     */
    public BigReal(final BigDecimal val) {
        this.d = val;
    }

    /**
     * Build an instance from a BigInteger.
     * 
     * @param val
     *        value of the instance
     */
    public BigReal(final BigInteger val) {
        this.d = new BigDecimal(val);
    }

    /**
     * Build an instance from an unscaled BigInteger.
     * 
     * @param unscaledVal
     *        unscaled value
     * @param scaleIn
     *        scale to use
     */
    public BigReal(final BigInteger unscaledVal, final int scaleIn) {
        this.d = new BigDecimal(unscaledVal, scaleIn);
    }

    /**
     * Build an instance from an unscaled BigInteger.
     * 
     * @param unscaledVal
     *        unscaled value
     * @param scaleIn
     *        scale to use
     * @param mc
     *        to used
     */
    public BigReal(final BigInteger unscaledVal, final int scaleIn, final MathContext mc) {
        this.d = new BigDecimal(unscaledVal, scaleIn, mc);
    }

    /**
     * Build an instance from a BigInteger.
     * 
     * @param val
     *        value of the instance
     * @param mc
     *        context to use
     */
    public BigReal(final BigInteger val, final MathContext mc) {
        this.d = new BigDecimal(val, mc);
    }

    /**
     * Build an instance from a characters representation.
     * 
     * @param in
     *        character representation of the value
     */
    public BigReal(final char[] in) {
        this.d = new BigDecimal(in);
    }

    /**
     * Build an instance from a characters representation.
     * 
     * @param in
     *        character representation of the value
     * @param offset
     *        offset of the first character to analyze
     * @param len
     *        length of the array slice to analyze
     */
    public BigReal(final char[] in, final int offset, final int len) {
        this.d = new BigDecimal(in, offset, len);
    }

    /**
     * Build an instance from a characters representation.
     * 
     * @param in
     *        character representation of the value
     * @param offset
     *        offset of the first character to analyze
     * @param len
     *        length of the array slice to analyze
     * @param mc
     *        context to use
     */
    public BigReal(final char[] in, final int offset, final int len, final MathContext mc) {
        this.d = new BigDecimal(in, offset, len, mc);
    }

    /**
     * Build an instance from a characters representation.
     * 
     * @param in
     *        character representation of the value
     * @param mc
     *        context to use
     */
    public BigReal(final char[] in, final MathContext mc) {
        this.d = new BigDecimal(in, mc);
    }

    /**
     * Build an instance from a double.
     * 
     * @param val
     *        value of the instance
     */
    public BigReal(final double val) {
        this.d = new BigDecimal(val);
    }

    /**
     * Build an instance from a double.
     * 
     * @param val
     *        value of the instance
     * @param mc
     *        context to use
     */
    public BigReal(final double val, final MathContext mc) {
        this.d = new BigDecimal(val, mc);
    }

    /**
     * Build an instance from an int.
     * 
     * @param val
     *        value of the instance
     */
    public BigReal(final int val) {
        this.d = new BigDecimal(val);
    }

    /**
     * Build an instance from an int.
     * 
     * @param val
     *        value of the instance
     * @param mc
     *        context to use
     */
    public BigReal(final int val, final MathContext mc) {
        this.d = new BigDecimal(val, mc);
    }

    /**
     * Build an instance from a long.
     * 
     * @param val
     *        value of the instance
     */
    public BigReal(final long val) {
        this.d = new BigDecimal(val);
    }

    /**
     * Build an instance from a long.
     * 
     * @param val
     *        value of the instance
     * @param mc
     *        context to use
     */
    public BigReal(final long val, final MathContext mc) {
        this.d = new BigDecimal(val, mc);
    }

    /**
     * Build an instance from a String representation.
     * 
     * @param val
     *        character representation of the value
     */
    public BigReal(final String val) {
        this.d = new BigDecimal(val);
    }

    /**
     * Build an instance from a String representation.
     * 
     * @param val
     *        character representation of the value
     * @param mc
     *        context to use
     */
    public BigReal(final String val, final MathContext mc) {
        this.d = new BigDecimal(val, mc);
    }

    /***
     * Gets the rounding mode for division operations
     * The default is {@code RoundingMode.HALF_UP}
     * 
     * @return the rounding mode.
     * @since 2.1
     */
    public RoundingMode getRoundingMode() {
        return this.roundingMode;
    }

    /***
     * Sets the rounding mode for decimal divisions.
     * 
     * @param roundingModeIn
     *        rounding mode for decimal divisions
     * @since 2.1
     */
    public void setRoundingMode(final RoundingMode roundingModeIn) {
        this.roundingMode = roundingModeIn;
    }

    /***
     * Sets the scale for division operations.
     * The default is 64
     * 
     * @return the scale
     * @since 2.1
     */
    public int getScale() {
        return this.scale;
    }

    /***
     * Sets the scale for division operations.
     * 
     * @param scaleIn
     *        scale for division operations
     * @since 2.1
     */
    public void setScale(final int scaleIn) {
        this.scale = scaleIn;
    }

    /** {@inheritDoc} */
    @Override
    public BigReal add(final BigReal a) {
        return new BigReal(this.d.add(a.d));
    }

    /** {@inheritDoc} */
    @Override
    public BigReal subtract(final BigReal a) {
        return new BigReal(this.d.subtract(a.d));
    }

    /** {@inheritDoc} */
    @Override
    public BigReal negate() {
        return new BigReal(this.d.negate());
    }

    /**
     * {@inheritDoc}
     * 
     * @throws MathArithmeticException
     *         if {@code a} is zero
     */
    @Override
    @SuppressWarnings("PMD.PreserveStackTrace")
    public BigReal divide(final BigReal a) {
        try {
            return new BigReal(this.d.divide(a.d, this.scale, this.roundingMode));
        } catch (final ArithmeticException e) {
            // Division by zero has occurred
            throw new MathArithmeticException(PatriusMessages.ZERO_NOT_ALLOWED);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @throws MathArithmeticException
     *         if {@code this} is zero
     */
    @Override
    @SuppressWarnings("PMD.PreserveStackTrace")
    public BigReal reciprocal() {
        try {
            return new BigReal(BigDecimal.ONE.divide(this.d, this.scale, this.roundingMode));
        } catch (final ArithmeticException e) {
            // Division by zero has occurred
            throw new MathArithmeticException(PatriusMessages.ZERO_NOT_ALLOWED);
        }
    }

    /** {@inheritDoc} */
    @Override
    public BigReal multiply(final BigReal a) {
        return new BigReal(this.d.multiply(a.d));
    }

    /** {@inheritDoc} */
    @Override
    public BigReal multiply(final int n) {
        return new BigReal(this.d.multiply(new BigDecimal(n)));
    }

    /** {@inheritDoc} */
    @Override
    public int compareTo(final BigReal a) {
        return this.d.compareTo(a.d);
    }

    /**
     * Get the double value corresponding to the instance.
     * 
     * @return double value corresponding to the instance
     */
    public double doubleValue() {
        return this.d.doubleValue();
    }

    /**
     * Get the BigDecimal value corresponding to the instance.
     * 
     * @return BigDecimal value corresponding to the instance
     */
    public BigDecimal bigDecimalValue() {
        return this.d;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }

        if (other instanceof BigReal) {
            return this.d.equals(((BigReal) other).d);
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return this.d.hashCode();
    }

    /** {@inheritDoc} */
    @Override
    public Field<BigReal> getField() {
        return BigRealField.getInstance();
    }
}
