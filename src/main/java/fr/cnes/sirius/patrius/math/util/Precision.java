/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 *
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
 * VERSION:4.6:FA:FA-2542:27/01/2021:[PATRIUS] Definition d'un champ de vue avec demi-angle de 180° 
 * VERSION:4.5.1:FA:FA-2540:04/08/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear
 * VERSION:4.5:DM:DM-2300:27/05/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear 
 * VERSION:4.4:FA:FA-2137:04/10/2019:FA mineure Patrius V4.3
 * VERSION:4.3:FA:FA-2113:15/05/2019:Usage des exceptions pour gerer un if/else dans Ellipsoid.getPointLocation()
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.math.util;

import java.math.BigDecimal;

import fr.cnes.sirius.patrius.math.exception.MathArithmeticException;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

//CHECKSTYLE: stop CommentRatio check
//CHECKSTYLE: stop MagicNumber check
//Reason: model - Commons-Math code

/**
 * Utilities for comparing numbers.
 * 
 * @since 3.0
 * @version $Id: Precision.java 18108 2017-10-04 06:45:27Z bignon $
 */
public final class Precision {
    /**
     * <p>
     * Largest double-precision floating-point number such that {@code 1 + EPSILON} is numerically equal to 1. This
     * value is an upper bound on the relative error due to rounding real numbers to double precision floating-point
     * numbers.
     * </p>
     * <p>
     * In IEEE 754 arithmetic, this is 2<sup>-53</sup>.
     * </p>
     * 
     * @see <a href="http://en.wikipedia.org/wiki/Machine_epsilon">Machine epsilon</a>
     */
    public static final double EPSILON;

    /** Epsilon used for doubles relative comparison. Value is {@value}. */
    public static final double DOUBLE_COMPARISON_EPSILON = 1.0e-14;

    /**
     * Safe minimum, such that {@code 1 / SAFE_MIN} does not overflow. <br/>
     * In IEEE 754 arithmetic, this is also the smallest normalized
     * number 2<sup>-1022</sup>.
     */
    public static final double SAFE_MIN;

    /** Exponent offset in IEEE754 representation. */
    private static final long EXPONENT_OFFSET = 1023L;

    /** Offset to order signed double numbers lexicographically. */
    private static final long SGN_MASK = 0x8000000000000000L;
    /** Offset to order signed double numbers lexicographically. */
    private static final int SGN_MASK_FLOAT = 0x80000000;

    /** 0.5. */
    private static final double HALF = 0.5;

    static {
        /*
         * This was previously expressed as = 0x1.0p-53;
         * However, OpenJDK (Sparc Solaris) cannot handle such small
         * constants: MATH-721
         */
        EPSILON = Double.longBitsToDouble((EXPONENT_OFFSET - 53L) << 52);

        /*
         * This was previously expressed as = 0x1.0p-1022;
         * However, OpenJDK (Sparc Solaris) cannot handle such small
         * constants: MATH-721
         */
        SAFE_MIN = Double.longBitsToDouble((EXPONENT_OFFSET - 1022L) << 52);
    }

    /**
     * Private constructor.
     */
    private Precision() {
    }

    /**
     * Compares two numbers given some amount of allowed error.
     * 
     * @param x
     *        the first number
     * @param y
     *        the second number
     * @param eps
     *        the amount of error to allow when checking for equality
     * @return <ul>
     *         <li>0 if {@link #equals(double, double, double) equals(x, y, eps)}</li>
     *         <li>&lt; 0 if !{@link #equals(double, double, double) equals(x, y, eps)} &amp;&amp; x &lt; y</li>
     *         <li>>0 if !{@link #equals(double, double, double) equals(x, y, eps)} &amp;&amp; x > y</li>
     *         </ul>
     */
    public static int compareTo(final double x, final double y, final double eps) {
        final int res;
        if (equals(x, y, eps)) {
            res = 0;
        } else if (x < y) {
            res = -1;
        } else {
            res = 1;
        }
        return res;
    }

    /**
     * Compares two numbers given some amount of allowed error.
     * Two float numbers are considered equal if there are {@code (maxUlps - 1)} (or fewer) floating point numbers
     * between them, i.e. two adjacent floating
     * point numbers are considered equal.
     * Adapted from <a
     * href="http://www.cygnus-software.com/papers/comparingfloats/comparingfloats.htm">
     * Bruce Dawson</a>
     * 
     * @param x
     *        first value
     * @param y
     *        second value
     * @param maxUlps
     *        {@code (maxUlps - 1)} is the number of floating point
     *        values between {@code x} and {@code y}.
     * @return <ul>
     *         <li>0 if {@link #equals(double, double, int) equals(x, y, maxUlps)}</li>
     *         <li>&lt; 0 if !{@link #equals(double, double, int) equals(x, y, maxUlps)} &amp;&amp; x &lt; y</li>
     *         <li>>0 if !{@link #equals(double, double, int) equals(x, y, maxUlps)} &amp;&amp; x > y</li>
     *         </ul>
     */
    public static int compareTo(final double x, final double y, final int maxUlps) {
        final int res;
        if (equals(x, y, maxUlps)) {
            res = 0;
        } else if (x < y) {
            res = -1;
        } else {
            res = 1;
        }
        return res;
    }

    /**
     * Returns true if they are equal as defined by {@link #equals(float,float,int) equals(x, y, 1)}.
     * 
     * @param x
     *        first value
     * @param y
     *        second value
     * @return {@code true} if the values are equal.
     */
    public static boolean equals(final float x, final float y) {
        return equals(x, y, 1);
    }

    /**
     * Returns true if both arguments are NaN or neither is NaN and they are
     * equal as defined by {@link #equals(float,float) equals(x, y, 1)}.
     * 
     * @param x
     *        first value
     * @param y
     *        second value
     * @return {@code true} if the values are equal or both are NaN.
     * @since 2.2
     */
    public static boolean equalsIncludingNaN(final float x, final float y) {
        return (Float.isNaN(x) && Float.isNaN(y)) || equals(x, y, 1);
    }

    /**
     * Returns true if both arguments are equal or within the range of allowed
     * error (inclusive).
     * 
     * @param x
     *        first value
     * @param y
     *        second value
     * @param eps
     *        the amount of absolute error to allow.
     * @return {@code true} if the values are equal or within range of each other.
     * @since 2.2
     */
    public static boolean equals(final float x, final float y, final float eps) {
        return equals(x, y, 1) || MathLib.abs(y - x) <= eps;
    }

    /**
     * Returns true if both arguments are NaN or are equal or within the range
     * of allowed error (inclusive).
     * 
     * @param x
     *        first value
     * @param y
     *        second value
     * @param eps
     *        the amount of absolute error to allow.
     * @return {@code true} if the values are equal or within range of each other,
     *         or both are NaN.
     * @since 2.2
     */
    public static boolean equalsIncludingNaN(final float x, final float y, final float eps) {
        return equalsIncludingNaN(x, y) || (MathLib.abs(y - x) <= eps);
    }

    /**
     * Returns true if both arguments are equal or within the range of allowed
     * error (inclusive).
     * Two float numbers are considered equal if there are {@code (maxUlps - 1)} (or fewer) floating point numbers
     * between them, i.e. two adjacent floating
     * point numbers are considered equal.
     * Adapted from <a
     * href="http://www.cygnus-software.com/papers/comparingfloats/comparingfloats.htm">
     * Bruce Dawson</a>
     * 
     * @param x
     *        first value
     * @param y
     *        second value
     * @param maxUlps
     *        {@code (maxUlps - 1)} is the number of floating point
     *        values between {@code x} and {@code y}.
     * @return {@code true} if there are fewer than {@code maxUlps} floating
     *         point values between {@code x} and {@code y}.
     * @since 2.2
     */
    public static boolean equals(final float x, final float y, final int maxUlps) {
        int xInt = Float.floatToIntBits(x);
        int yInt = Float.floatToIntBits(y);

        // Make lexicographically ordered as a two's-complement integer.
        if (xInt < 0) {
            xInt = SGN_MASK_FLOAT - xInt;
        }
        if (yInt < 0) {
            yInt = SGN_MASK_FLOAT - yInt;
        }

        final boolean isEqual = MathLib.abs(xInt - yInt) <= maxUlps;

        return isEqual && !Float.isNaN(x) && !Float.isNaN(y);
    }

    /**
     * Returns true if both arguments are NaN or if they are equal as defined
     * by {@link #equals(float,float,int) equals(x, y, maxUlps)}.
     * 
     * @param x
     *        first value
     * @param y
     *        second value
     * @param maxUlps
     *        {@code (maxUlps - 1)} is the number of floating point
     *        values between {@code x} and {@code y}.
     * @return {@code true} if both arguments are NaN or if there are less than {@code maxUlps} floating point values
     *         between {@code x} and {@code y}.
     * @since 2.2
     */
    public static boolean equalsIncludingNaN(final float x, final float y, final int maxUlps) {
        return (Float.isNaN(x) && Float.isNaN(y)) || equals(x, y, maxUlps);
    }

    /**
     * Returns true iff they are equal as defined by {@link #equals(double,double,int) equals(x, y, 1)}.
     * 
     * @param x
     *        first value
     * @param y
     *        second value
     * @return {@code true} if the values are equal.
     */
    public static boolean equals(final double x, final double y) {
        return equals(x, y, 1);
    }

    /**
     * Returns true if both arguments are NaN or neither is NaN and they are
     * equal as defined by {@link #equals(double,double) equals(x, y, 1)}.
     * 
     * @param x
     *        first value
     * @param y
     *        second value
     * @return {@code true} if the values are equal or both are NaN.
     * @since 2.2
     */
    public static boolean equalsIncludingNaN(final double x, final double y) {
        return (Double.isNaN(x) && Double.isNaN(y)) || equals(x, y, 1);
    }

    /**
     * Returns {@code true} if there is no double value strictly between the
     * arguments or the difference between them is within the range of allowed
     * error (inclusive).
     * 
     * @param x
     *        First value.
     * @param y
     *        Second value.
     * @param eps
     *        Amount of allowed absolute error.
     * @return {@code true} if the values are two adjacent floating point
     *         numbers or they are within range of each other.
     */
    public static boolean equals(final double x, final double y, final double eps) {
        final double gap;
        if (!Double.isNaN(x) && !Double.isNaN(y)) {
            gap = Math.abs(y - x);
        } else {
            gap = Double.NaN;
        }

        return equals(x, y, 1) || gap <= eps;
    }

    /**
     * Returns {@code true} if there is no double value strictly between the
     * arguments or the relative difference between them is smaller or equal
     * to the given tolerance.
     * 
     * @param x
     *        First value.
     * @param y
     *        Second value.
     * @param eps
     *        Amount of allowed relative error.
     * @return {@code true} if the values are two adjacent floating point
     *         numbers or they are within range of each other.
     * @since 3.1
     */
    public static boolean equalsWithRelativeTolerance(final double x, final double y, final double eps) {
        if (equals(x, y, 1)) {
            return true;
        }

        final double relativeDifference;
        if (!Double.isNaN(x) && !Double.isNaN(y) && !Double.isInfinite(x) && !Double.isInfinite(y)) {
            final double absoluteMax = MathLib.max(MathLib.abs(x), MathLib.abs(y));
            if (absoluteMax != 0.) {
                relativeDifference = MathLib.abs(MathLib.divide((x - y), absoluteMax));
            } else {
                relativeDifference = Double.NaN;
            }
        } else {
            relativeDifference = Double.NaN;
        }

        return relativeDifference <= eps;
    }

    /**
     * Returns true if both arguments are NaN or are equal or within the range
     * of allowed error (inclusive).
     * 
     * @param x
     *        first value
     * @param y
     *        second value
     * @param eps
     *        the amount of absolute error to allow.
     * @return {@code true} if the values are equal or within range of each other,
     *         or both are NaN.
     * @since 2.2
     */
    public static boolean equalsIncludingNaN(final double x, final double y, final double eps) {
        return equalsIncludingNaN(x, y) || (MathLib.abs(y - x) <= eps);
    }

    /**
     * Returns true if both arguments are equal or within the range of allowed
     * error (inclusive).
     * Two float numbers are considered equal if there are {@code (maxUlps - 1)} (or fewer) floating point numbers
     * between them, i.e. two adjacent floating
     * point numbers are considered equal.
     * Adapted from <a
     * href="http://www.cygnus-software.com/papers/comparingfloats/comparingfloats.htm">
     * Bruce Dawson</a>
     * 
     * @param x
     *        first value
     * @param y
     *        second value
     * @param maxUlps
     *        {@code (maxUlps - 1)} is the number of floating point
     *        values between {@code x} and {@code y}.
     * @return {@code true} if there are fewer than {@code maxUlps} floating
     *         point values between {@code x} and {@code y}.
     */
    public static boolean equals(final double x, final double y, final int maxUlps) {
        long xInt = Double.doubleToLongBits(x);
        long yInt = Double.doubleToLongBits(y);

        // Make lexicographically ordered as a two's-complement integer.
        if (xInt < 0) {
            xInt = SGN_MASK - xInt;
        }
        if (yInt < 0) {
            yInt = SGN_MASK - yInt;
        }

        final boolean isEqual = MathLib.abs(xInt - yInt) <= maxUlps;

        return isEqual && !Double.isNaN(x) && !Double.isNaN(y);
    }

    /**
     * Returns true if both arguments are NaN or if they are equal as defined
     * by {@link #equals(double,double,int) equals(x, y, maxUlps)}.
     * 
     * @param x
     *        first value
     * @param y
     *        second value
     * @param maxUlps
     *        {@code (maxUlps - 1)} is the number of floating point
     *        values between {@code x} and {@code y}.
     * @return {@code true} if both arguments are NaN or if there are less than {@code maxUlps} floating point values
     *         between {@code x} and {@code y}.
     * @since 2.2
     */
    public static boolean equalsIncludingNaN(final double x, final double y, final int maxUlps) {
        return (Double.isNaN(x) && Double.isNaN(y)) || equals(x, y, maxUlps);
    }

    /**
     * Returns {@code true} if both arguments are found equal
     * with a relative comparison using a default epsilon.
     * 
     * @param x
     *        first double to be compared
     * @param y
     *        second double to be compared
     * 
     * @return a boolean : "true" if the doubles are found equals.
     *         <p>
     *         The value "Nan" as input always imply the return "false"
     *         </p>
     * 
     */
    public static boolean equalsWithRelativeTolerance(final double x, final double y) {
        return equalsWithRelativeTolerance(x, y, DOUBLE_COMPARISON_EPSILON);
    }

    /**
     * Returns {@code true} if there is no double value strictly between the
     * arguments or the relative difference between them is smaller or equal
     * to the given tolerance AND if there is no double value strictly between the
     * arguments or the difference between them is within the range of allowed
     * error (inclusive).
     * 
     * @see {@link Precision#equals(double, double, double)}
     * @see {@link Precision#equalsWithRelativeTolerance(double, double, double)}
     * 
     * @param x
     *        First value.
     * @param y
     *        Second value.
     * @param relThreshold
     *        Amount of allowed relative error.
     * @param absThreshold
     *        Amount of allowed absolute error.
     * @return {@code true} if the values are two adjacent floating point
     *         numbers or they are within range of each other.
     * @since 4.5
     */
    public static boolean equalsWithAbsoluteAndRelativeTolerances(final double x, final double y,
            final double relThreshold, final double absThreshold) {
        return equalsWithRelativeTolerance(x, y, relThreshold) && equals(x, y, absThreshold);
    }

    /**
     * Returns {@code true} if there is no double value strictly between the
     * arguments or the relative difference between them is smaller or equal
     * to the given tolerance OR if there is no double value strictly between the
     * arguments or the difference between them is within the range of allowed
     * error (inclusive).
     * 
     * @see {@link Precision#equals(double, double, double)}
     * @see {@link Precision#equalsWithRelativeTolerance(double, double, double)}
     * 
     * @param x
     *        First value.
     * @param y
     *        Second value.
     * @param relThreshold
     *        Amount of allowed relative error.
     * @param absThreshold
     *        Amount of allowed absolute error.
     * @return {@code true} if the values are two adjacent floating point
     *         numbers or they are within range of each other.
     * @since 4.5
     */
    public static boolean equalsWithAbsoluteOrRelativeTolerances(final double x, final double y,
            final double relThreshold, final double absThreshold) {
        return equalsWithRelativeTolerance(x, y, relThreshold) || equals(x, y, absThreshold);
    }

    /**
     * Rounds the given value to the specified number of decimal places.
     * The value is rounded using the {@link BigDecimal#ROUND_HALF_UP} method.
     * 
     * @param x
     *        Value to round.
     * @param scale
     *        Number of digits to the right of the decimal point.
     * @return the rounded value.
     * @since 1.1 (previously in {@code MathUtils}, moved as of version 3.0)
     */
    public static double round(final double x, final int scale) {
        return round(x, scale, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * Rounds the given value to the specified number of decimal places.
     * The value is rounded using the given method which is any method defined
     * in {@link BigDecimal}.
     * If {@code x} is infinite or {@code NaN}, then the value of {@code x} is
     * returned unchanged, regardless of the other parameters.
     * 
     * @param x
     *        Value to round.
     * @param scale
     *        Number of digits to the right of the decimal point.
     * @param roundingMethod
     *        Rounding method as defined in {@link BigDecimal}.
     * @return the rounded value.
     * @throws ArithmeticException
     *         if {@code roundingMethod == ROUND_UNNECESSARY} and the specified scaling operation would require
     *         rounding.
     * @throws IllegalArgumentException
     *         if {@code roundingMethod} does not
     *         represent a valid rounding mode.
     * @since 1.1 (previously in {@code MathUtils}, moved as of version 3.0)
     */
    public static double round(final double x, final int scale, final int roundingMethod) {

        if (!Double.isNaN(x) && !Double.isInfinite(x)) {
            if (roundingMethod >= BigDecimal.ROUND_UP
                    && roundingMethod <= BigDecimal.ROUND_UNNECESSARY) {
                return (new BigDecimal(Double.toString(x)).setScale(scale, roundingMethod))
                    .doubleValue();
            } else {
                return Double.NaN;
            }
        } else {
            // Exception case
            return x;

        }
    }

    /**
     * Rounds the given value to the specified number of decimal places.
     * The value is rounded using the {@link BigDecimal#ROUND_HALF_UP} method.
     * 
     * @param x
     *        Value to round.
     * @param scale
     *        Number of digits to the right of the decimal point.
     * @return the rounded value.
     * @since 1.1 (previously in {@code MathUtils}, moved as of version 3.0)
     */
    public static float round(final float x, final int scale) {
        return round(x, scale, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * Rounds the given value to the specified number of decimal places.
     * The value is rounded using the given method which is any method defined
     * in {@link BigDecimal}.
     * 
     * @param x
     *        Value to round.
     * @param scale
     *        Number of digits to the right of the decimal point.
     * @param roundingMethod
     *        Rounding method as defined in {@link BigDecimal}.
     * @return the rounded value.
     * @since 1.1 (previously in {@code MathUtils}, moved as of version 3.0)
     * @throws MathArithmeticException
     *         if an exact operation is required but result is not exact
     * @throws MathIllegalArgumentException
     *         if {@code roundingMethod} is not a valid rounding method.
     */
    public static float round(final float x, final int scale, final int roundingMethod) {
        final float sign = MathLib.copySign(1f, x);
        final float factor = (float) MathLib.pow(10.0f, scale) * sign;
        return (float) roundUnscaled(x * factor, sign, roundingMethod) / factor;
    }

    /**
     * Rounds the given non-negative value to the "nearest" integer. Nearest is
     * determined by the rounding method specified. Rounding methods are defined
     * in {@link BigDecimal}.
     * 
     * @param unscaledIn
     *        Value to round.
     * @param sign
     *        Sign of the original, scaled value.
     * @param roundingMethod
     *        Rounding method, as defined in {@link BigDecimal}.
     * @return the rounded value.
     * @throws MathArithmeticException
     *         if an exact operation is required but result is not exact
     * @throws MathIllegalArgumentException
     *         if {@code roundingMethod} is not a valid rounding method.
     * @since 1.1 (previously in {@code MathUtils}, moved as of version 3.0)
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Commons-Math code kept as such
    private static double roundUnscaled(final double unscaledIn,
            final double sign,
            final int roundingMethod) {
        // CHECKSTYLE: resume CyclomaticComplexity check
        double unscaled = unscaledIn;
        switch (roundingMethod) {
            case BigDecimal.ROUND_CEILING:
                if (sign == -1) {
                    unscaled = MathLib.floor(MathLib.nextAfter(unscaled, Double.NEGATIVE_INFINITY));
                } else {
                    unscaled = MathLib.ceil(MathLib.nextAfter(unscaled, Double.POSITIVE_INFINITY));
                }
                break;
            case BigDecimal.ROUND_DOWN:
                unscaled = MathLib.floor(MathLib.nextAfter(unscaled, Double.NEGATIVE_INFINITY));
                break;
            case BigDecimal.ROUND_FLOOR:
                if (sign == -1) {
                    unscaled = MathLib.ceil(MathLib.nextAfter(unscaled, Double.POSITIVE_INFINITY));
                } else {
                    unscaled = MathLib.floor(MathLib.nextAfter(unscaled, Double.NEGATIVE_INFINITY));
                }
                break;
            case BigDecimal.ROUND_HALF_DOWN:
                unscaled = MathLib.nextAfter(unscaled, Double.NEGATIVE_INFINITY);
                final double fraction = unscaled - MathLib.floor(unscaled);
                if (fraction > HALF) {
                    unscaled = MathLib.ceil(unscaled);
                } else {
                    unscaled = MathLib.floor(unscaled);
                }
                break;
            case BigDecimal.ROUND_HALF_EVEN:
                final double fraction2 = unscaled - MathLib.floor(unscaled);
                if (fraction2 > HALF) {
                    unscaled = MathLib.ceil(unscaled);
                } else if (fraction2 < HALF) {
                    unscaled = MathLib.floor(unscaled);
                } else {
                    // The following equality test is intentional and needed for rounding purposes
                    if (MathLib.floor(unscaled) / 2.0 == MathLib.floor(Math
                        .floor(unscaled) / 2.0)) {
                        // even
                        unscaled = MathLib.floor(unscaled);
                    } else {
                        // odd
                        unscaled = MathLib.ceil(unscaled);
                    }
                }
                break;
            case BigDecimal.ROUND_HALF_UP:
                unscaled = MathLib.nextAfter(unscaled, Double.POSITIVE_INFINITY);
                final double fraction3 = unscaled - MathLib.floor(unscaled);
                if (fraction3 >= HALF) {
                    unscaled = MathLib.ceil(unscaled);
                } else {
                    unscaled = MathLib.floor(unscaled);
                }
                break;
            case BigDecimal.ROUND_UNNECESSARY:
                if (unscaled != MathLib.floor(unscaled)) {
                    throw new MathArithmeticException();
                }
                break;
            case BigDecimal.ROUND_UP:
                unscaled = MathLib.ceil(MathLib.nextAfter(unscaled, Double.POSITIVE_INFINITY));
                break;
            default:
                throw new MathIllegalArgumentException(PatriusMessages.INVALID_ROUNDING_METHOD,
                    roundingMethod,
                    "ROUND_CEILING", BigDecimal.ROUND_CEILING,
                    "ROUND_DOWN", BigDecimal.ROUND_DOWN,
                    "ROUND_FLOOR", BigDecimal.ROUND_FLOOR,
                    "ROUND_HALF_DOWN", BigDecimal.ROUND_HALF_DOWN,
                    "ROUND_HALF_EVEN", BigDecimal.ROUND_HALF_EVEN,
                    "ROUND_HALF_UP", BigDecimal.ROUND_HALF_UP,
                    "ROUND_UNNECESSARY", BigDecimal.ROUND_UNNECESSARY,
                    "ROUND_UP", BigDecimal.ROUND_UP);
        }
        return unscaled;
    }

    /**
     * Computes a number {@code delta} close to {@code originalDelta} with
     * the property that
     * 
     * <pre>
     * <code>
     *   x + delta - x
     * </code>
     * </pre>
     * 
     * is exactly machine-representable.
     * This is useful when computing numerical derivatives, in order to reduce
     * roundoff errors.
     * 
     * @param x
     *        Value.
     * @param originalDelta
     *        Offset value.
     * @return a number {@code delta} so that {@code x + delta} and {@code x} differ by a representable floating number.
     */
    public static double representableDelta(final double x,
            final double originalDelta) {
        return x + originalDelta - x;
    }

    // CHECKSTYLE: resume CommentRatio check
    // CHECKSTYLE: resume MagicNumber check
}
