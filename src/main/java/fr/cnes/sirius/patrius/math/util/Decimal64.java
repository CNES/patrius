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
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.util;

import fr.cnes.sirius.patrius.math.Field;
import fr.cnes.sirius.patrius.math.FieldElement;

/**
 * This class wraps a {@code double} value in an object. It is similar to the
 * standard class {@link Double}, while also implementing the {@link FieldElement} interface.
 * 
 * @since 3.1
 * @version $Id: Decimal64.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class Decimal64 extends Number implements FieldElement<Decimal64>,
    Comparable<Decimal64> {

    /** The constant value of {@code 0d} as a {@code Decimal64}. */
    public static final Decimal64 ZERO;

    /** The constant value of {@code 1d} as a {@code Decimal64}. */
    public static final Decimal64 ONE;

    /**
     * The constant value of {@link Double#NEGATIVE_INFINITY} as a {@code Decimal64}.
     */
    public static final Decimal64 NEGATIVE_INFINITY;

    /**
     * The constant value of {@link Double#POSITIVE_INFINITY} as a {@code Decimal64}.
     */
    public static final Decimal64 POSITIVE_INFINITY;

    /** The constant value of {@link Double#NaN} as a {@code Decimal64}. */
    public static final Decimal64 NAN;

    /** Hashmap generator. */
    private static final int HASHMAP_GENERATOR = 32;

    /** UID. */
    private static final long serialVersionUID = 20120227L;

    static {
        ZERO = new Decimal64(0d);
        ONE = new Decimal64(1d);
        NEGATIVE_INFINITY = new Decimal64(Double.NEGATIVE_INFINITY);
        POSITIVE_INFINITY = new Decimal64(Double.POSITIVE_INFINITY);
        NAN = new Decimal64(Double.NaN);
    }

    /** The primitive {@code double} value of this object. */
    private final double value;

    /**
     * Creates a new instance of this class.
     * 
     * @param x
     *        the primitive {@code double} value of the object to be created
     */
    public Decimal64(final double x) {
        super();
        this.value = x;
    }

    /*
     * Methods from the FieldElement interface.
     */

    /** {@inheritDoc} */
    @Override
    public Field<Decimal64> getField() {
        return Decimal64Field.getInstance();
    }

    /**
     * {@inheritDoc}
     * 
     * The current implementation strictly enforces {@code this.add(a).equals(new Decimal64(this.doubleValue()
     * + a.doubleValue()))}.
     */
    @Override
    public Decimal64 add(final Decimal64 a) {
        return new Decimal64(this.value + a.value);
    }

    /**
     * {@inheritDoc}
     * 
     * The current implementation strictly enforces {@code this.subtract(a).equals(new Decimal64(this.doubleValue()
     * - a.doubleValue()))}.
     */
    @Override
    public Decimal64 subtract(final Decimal64 a) {
        return new Decimal64(this.value - a.value);
    }

    /**
     * {@inheritDoc}
     * 
     * The current implementation strictly enforces {@code this.negate().equals(new Decimal64(-this.doubleValue()))}.
     */
    @Override
    public Decimal64 negate() {
        return new Decimal64(-this.value);
    }

    /**
     * {@inheritDoc}
     * 
     * The current implementation strictly enforces {@code this.multiply(a).equals(new Decimal64(this.doubleValue()
     * * a.doubleValue()))}.
     */
    @Override
    public Decimal64 multiply(final Decimal64 a) {
        return new Decimal64(this.value * a.value);
    }

    /**
     * {@inheritDoc}
     * 
     * The current implementation strictly enforces
     * {@code this.multiply(n).equals(new Decimal64(n * this.doubleValue()))}.
     */
    @Override
    public Decimal64 multiply(final int n) {
        return new Decimal64(n * this.value);
    }

    /**
     * {@inheritDoc}
     * 
     * The current implementation strictly enforces {@code this.divide(a).equals(new Decimal64(this.doubleValue()
     * / a.doubleValue()))}.
     * 
     */
    @Override
    public Decimal64 divide(final Decimal64 a) {
        return new Decimal64(this.value / a.value);
    }

    /**
     * {@inheritDoc}
     * 
     * The current implementation strictly enforces {@code this.reciprocal().equals(new Decimal64(1.0
     * / this.doubleValue()))}.
     */
    @Override
    public Decimal64 reciprocal() {
        return new Decimal64(1.0 / this.value);
    }

    /*
     * Methods from the Number abstract class
     */

    /**
     * {@inheritDoc}
     * 
     * The current implementation performs casting to a {@code byte}.
     */
    @Override
    public byte byteValue() {
        return (byte) this.value;
    }

    /**
     * {@inheritDoc}
     * 
     * The current implementation performs casting to a {@code int}.
     */
    @Override
    public int intValue() {
        return (int) this.value;
    }

    /**
     * {@inheritDoc}
     * 
     * The current implementation performs casting to a {@code long}.
     */
    @Override
    public long longValue() {
        return (long) this.value;
    }

    /**
     * {@inheritDoc}
     * 
     * The current implementation performs casting to a {@code float}.
     */
    @Override
    public float floatValue() {
        return (float) this.value;
    }

    /** {@inheritDoc} */
    @Override
    public double doubleValue() {
        return this.value;
    }

    /*
     * Methods from the Comparable interface.
     */

    /**
     * {@inheritDoc}
     * 
     * The current implementation returns the same value as
     * <center> {@code new Double(this.doubleValue()).compareTo(new
     * Double(o.doubleValue()))} </center>
     * 
     * @see Double#compareTo(Double)
     */
    @Override
    public int compareTo(final Decimal64 o) {
        return Double.compare(this.value, o.value);
    }

    /*
     * Methods from the Object abstract class.
     */

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof Decimal64) {
            final Decimal64 that = (Decimal64) obj;
            return Double.doubleToLongBits(this.value) == Double
                .doubleToLongBits(that.value);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * The current implementation returns the same value as {@code new Double(this.doubleValue()).hashCode()}
     * 
     * @see Double#hashCode()
     */
    @Override
    public int hashCode() {
        final long v = Double.doubleToLongBits(this.value);
        return (int) (v ^ (v >>> HASHMAP_GENERATOR));
    }

    /**
     * {@inheritDoc}
     * 
     * The returned {@code String} is equal to {@code Double.toString(this.doubleValue())}
     * 
     * @see Double#toString(double)
     */
    @Override
    public String toString() {
        return Double.toString(this.value);
    }

    /*
     * Methods inspired by the Double class.
     */

    /**
     * Returns {@code true} if {@code this} double precision number is infinite
     * ({@link Double#POSITIVE_INFINITY} or {@link Double#NEGATIVE_INFINITY}).
     * 
     * @return {@code true} if {@code this} number is infinite
     */
    public boolean isInfinite() {
        return Double.isInfinite(this.value);
    }

    /**
     * Returns {@code true} if {@code this} double precision number is
     * Not-a-Number ({@code NaN}), false otherwise.
     * 
     * @return {@code true} if {@code this} is {@code NaN}
     */
    public boolean isNaN() {
        return Double.isNaN(this.value);
    }
}
