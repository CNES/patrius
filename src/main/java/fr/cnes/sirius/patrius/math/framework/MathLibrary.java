/**
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
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.3:DM:DM-2089:15/05/2019:[PATRIUS] passage a Java 8
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.framework;


/**
 * Interface for low-level math libraries.
 *
 * @author Emmanuel Bignon
 *
 * @since 4.2
 */
public interface MathLibrary {

    /**
     * Compute the square root of a number.
     * 
     * @param a number on which evaluation is done
     * @return square root of a
     */
    double sqrt(final double a);

    /**
     * Compute the hyperbolic cosine of a number.
     * 
     * @param x number on which evaluation is done
     * @return hyperbolic cosine of x
     */
    double cosh(final double x);

    /**
     * Compute the hyperbolic sine of a number.
     * 
     * @param x number on which evaluation is done
     * @return hyperbolic sine of x
     */
    double sinh(final double x);

    /**
     * Compute the hyperbolic tangent of a number.
     * 
     * @param x number on which evaluation is done
     * @return hyperbolic tangent of x
     */
    double tanh(final double x);

    /**
     * Compute the inverse hyperbolic cosine of a number.
     * 
     * @param a number on which evaluation is done
     * @return inverse hyperbolic cosine of a
     */
    double acosh(final double a);

    /**
     * Compute the inverse hyperbolic sine of a number.
     * 
     * @param a number on which evaluation is done
     * @return inverse hyperbolic sine of a
     */
    double asinh(final double a);

    /**
     * Compute the inverse hyperbolic tangent of a number.
     * 
     * @param a number on which evaluation is done
     * @return inverse hyperbolic tangent of a
     */
    double atanh(final double a);

    /**
     * Compute the signum of a number. The signum is -1 for negative numbers, +1 for positive
     * numbers and 0 otherwise
     * 
     * @param a number on which evaluation is done
     * @return -1.0, -0.0, +0.0, +1.0 or NaN depending on sign of a
     */
    double signum(final double a);

    /**
     * Compute the signum of a number. The signum is -1 for negative numbers, +1 for positive
     * numbers and 0 otherwise
     * 
     * @param a number on which evaluation is done
     * @return -1.0, -0.0, +0.0, +1.0 or NaN depending on sign of a
     */
    float signum(final float a);

    /**
     * Compute next number towards positive infinity.
     * 
     * @param a number to which neighbor should be computed
     * @return neighbor of a towards positive infinity
     */
    double nextUp(final double a);

    /**
     * Compute next number towards positive infinity.
     * 
     * @param a number to which neighbor should be computed
     * @return neighbor of a towards positive infinity
     */
    float nextUp(final float a);

    /**
     * Returns a pseudo-random number between 0.0 and 1.0.
     * 
     * @return a random number between 0.0 and 1.0
     */
    double random();

    /**
     * Exponential function.
     * 
     * Computes exp(x), function result is nearly rounded. It will be correctly rounded to the
     * theoretical value for 99.9% of input values, otherwise it will have a 1 UPL error.
     * 
     * Method: Lookup intVal = exp(int(x)) Lookup fracVal = exp(int(x-int(x) / 1024.0) * 1024.0 );
     * Compute z as the exponential of the remaining bits by a polynomial minus one exp(x) = intVal
     * * fracVal * (1 + z)
     * 
     * Accuracy: Calculation is done with 63 bits of precision, so result should be correctly
     * rounded for 99.9% of input values, with less than 1 ULP error otherwise.
     * 
     * @param x a double
     * @return double e<sup>x</sup>
     */
    double exp(final double x);

    /**
     * Compute exp(x) - 1
     * 
     * @param x number to compute shifted exponential
     * @return exp(x) - 1
     */
    double expm1(final double x);

    /**
     * Natural logarithm.
     * 
     * @param x a double
     * @return log(x)
     */
    double log(final double x);

    /**
     * Computes log(1 + x).
     * 
     * @param x Number.
     * @return {@code log(1 + x)}.
     */
    double log1p(final double x);

    /**
     * Compute the base 10 logarithm.
     * 
     * @param x a number
     * @return log10(x)
     */
    double log10(final double x);

    /**
     * Power function. Compute x^y.
     * 
     * @param x a double
     * @param y a double
     * @return double
     */
    double pow(final double x, final double y);

    /**
     * Raise a double to an int power.
     * 
     * @param d Number to raise.
     * @param e Exponent.
     * @return d<sup>e</sup>
     * @since 3.1
     */
    double pow(final double d, final int e);

    /**
     * Sine function.
     * 
     * @param x Argument.
     * @return sin(x)
     */
    double sin(final double x);

    /**
     * Cosine function.
     * 
     * @param x Argument.
     * @return cos(x)
     */
    double cos(final double x);

    /**
     * Tangent function.
     * 
     * @param x Argument.
     * @return tan(x)
     */
    double tan(final double x);

    /**
     * Arctangent function
     * 
     * @param x a number
     * @return atan(x)
     */
    double atan(final double x);

    /**
     * Two arguments arctangent function
     * 
     * @param y ordinate
     * @param x abscissa
     * @return phase angle of point (x,y) between {@code -PI} and {@code PI}
     */
    double atan2(final double y, final double x);

    /**
     * Compute the arc sine of a number.
     * 
     * @param x number on which evaluation is done
     * @return arc sine of x
     */
    double asin(final double x);

    /**
     * Compute the arc cosine of a number.
     * 
     * @param x number on which evaluation is done
     * @return arc cosine of x
     */
    double acos(final double x);

    /**
     * Compute the cubic root of a number.
     * 
     * @param x number on which evaluation is done
     * @return cubic root of x
     */
    double cbrt(final double x);

    /**
     * Convert degrees to radians, with error of less than 0.5 ULP
     * 
     * @param x angle in degrees
     * @return x converted into radians
     */
    double toRadians(final double x);

    /**
     * Convert radians to degrees, with error of less than 0.5 ULP
     * 
     * @param x angle in radians
     * @return x converted into degrees
     */
    double toDegrees(final double x);

    /**
     * Absolute value.
     * 
     * @param x number from which absolute value is requested
     * @return abs(x)
     */
    int abs(final int x);

    /**
     * Absolute value.
     * 
     * @param x number from which absolute value is requested
     * @return abs(x)
     */
    long abs(final long x);

    /**
     * Absolute value.
     * 
     * @param x number from which absolute value is requested
     * @return abs(x)
     */
    float abs(final float x);

    /**
     * Absolute value.
     * 
     * @param x number from which absolute value is requested
     * @return abs(x)
     */
    double abs(final double x);

    /**
     * Compute least significant bit (Unit in Last Position) for a number.
     * 
     * @param x number from which ulp is requested
     * @return ulp(x)
     */
    double ulp(final double x);

    /**
     * Compute least significant bit (Unit in Last Position) for a number.
     * 
     * @param x number from which ulp is requested
     * @return ulp(x)
     */
    float ulp(final float x);

    /**
     * Multiply a double number by a power of 2.
     * 
     * @param d number to multiply
     * @param n power of 2
     * @return d &times; 2<sup>n</sup>
     */
    double scalb(final double d, final int n);

    /**
     * Multiply a float number by a power of 2.
     * 
     * @param f number to multiply
     * @param n power of 2
     * @return f &times; 2<sup>n</sup>
     */
    float scalb(final float f, final int n);

    /**
     * Get the next machine representable number after a number, moving in the direction of another
     * number.
     * <p>
     * The ordering is as follows (increasing):
     * <ul>
     * <li>-INFINITY</li>
     * <li>-MAX_VALUE</li>
     * <li>-MIN_VALUE</li>
     * <li>-0.0</li>
     * <li>+0.0</li>
     * <li>+MIN_VALUE</li>
     * <li>+MAX_VALUE</li>
     * <li>+INFINITY</li>
     * <li></li>
     * <p>
     * If arguments compare equal, then the second argument is returned.
     * <p>
     * If {@code direction} is greater than {@code d}, the smallest machine representable number strictly greater than
     * {@code d} is returned; if less, then the largest representable number strictly less than {@code d} is returned.
     * </p>
     * <p>
     * If {@code d} is infinite and direction does not bring it back to finite numbers, it is returned unchanged.
     * </p>
     * 
     * @param d base number
     * @param direction (the only important thing is whether {@code direction} is greater or smaller than {@code d})
     * @return the next machine representable number in the specified direction
     */
    double nextAfter(final double d, final double direction);

    /**
     * Get the next machine representable number after a number, moving in the direction of another
     * number.
     * <p>
     * The ordering is as follows (increasing):
     * <ul>
     * <li>-INFINITY</li>
     * <li>-MAX_VALUE</li>
     * <li>-MIN_VALUE</li>
     * <li>-0.0</li>
     * <li>+0.0</li>
     * <li>+MIN_VALUE</li>
     * <li>+MAX_VALUE</li>
     * <li>+INFINITY</li>
     * <li></li>
     * <p>
     * If arguments compare equal, then the second argument is returned.
     * <p>
     * If {@code direction} is greater than {@code f}, the smallest machine representable number strictly greater than
     * {@code f} is returned; if less, then the largest representable number strictly less than {@code f} is returned.
     * </p>
     * <p>
     * If {@code f} is infinite and direction does not bring it back to finite numbers, it is returned unchanged.
     * </p>
     * 
     * @param f base number
     * @param direction (the only important thing is whether {@code direction} is greater or smaller than {@code f})
     * @return the next machine representable number in the specified direction
     */
    float nextAfter(final float f, final double direction);

    /**
     * Get the largest whole number smaller than x.
     * 
     * @param x number from which floor is requested
     * @return a double number f such that f is an integer f <= x < f + 1.0
     */
    double floor(final double x);

    /**
     * Get the smallest whole number larger than x.
     * 
     * @param x number from which ceil is requested
     * @return a double number c such that c is an integer c - 1.0 < x <= c
     */
    double ceil(final double x);

    /**
     * Get the whole number that is the nearest to x, or the even one if x is exactly half way
     * between two integers.
     * 
     * @param x number from which nearest whole number is requested
     * @return a double number r such that r is an integer r - 0.5 <= x <= r + 0.5
     */
    double rint(final double x);

    /**
     * Get the closest long to x.
     * 
     * @param x number from which closest long is requested
     * @return closest long to x
     */
    long round(final double x);

    /**
     * Get the closest int to x.
     * 
     * @param x number from which closest int is requested
     * @return closest int to x
     */
    int round(final float x);

    /**
     * Compute the minimum of two values
     * 
     * @param a first value
     * @param b second value
     * @return a if a is lesser or equal to b, b otherwise
     */
    int min(final int a, final int b);

    /**
     * Compute the minimum of two values
     * 
     * @param a first value
     * @param b second value
     * @return a if a is lesser or equal to b, b otherwise
     */
    long min(final long a, final long b);

    /**
     * Compute the minimum of two values
     * 
     * @param a first value
     * @param b second value
     * @return a if a is lesser or equal to b, b otherwise
     */
    float min(final float a, final float b);

    /**
     * Compute the minimum of two values
     * 
     * @param a first value
     * @param b second value
     * @return a if a is lesser or equal to b, b otherwise
     */
    double min(final double a, final double b);

    /**
     * Compute the maximum of two values
     * 
     * @param a first value
     * @param b second value
     * @return b if a is lesser or equal to b, a otherwise
     */
    int max(final int a, final int b);

    /**
     * Compute the maximum of two values
     * 
     * @param a first value
     * @param b second value
     * @return b if a is lesser or equal to b, a otherwise
     */
    long max(final long a, final long b);

    /**
     * Compute the maximum of two values
     * 
     * @param a first value
     * @param b second value
     * @return b if a is lesser or equal to b, a otherwise
     */
    float max(final float a, final float b);

    /**
     * Compute the maximum of two values
     * 
     * @param a first value
     * @param b second value
     * @return b if a is lesser or equal to b, a otherwise
     */
    double max(final double a, final double b);

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
     * @param x a value
     * @param y a value
     * @return sqrt(<i>x</i><sup>2</sup>&nbsp;+<i>y</i><sup>2</sup>)
     */
    double hypot(final double x, final double y);

    /**
     * Computes the remainder as prescribed by the IEEE 754 standard. The remainder value is
     * mathematically equal to {@code x - y*n} where {@code n} is the mathematical integer closest
     * to the exact mathematical value of the quotient {@code x/y}. If two mathematical integers are
     * equally close to {@code x/y} then {@code n} is the integer that is even.
     * <ul>
     * <li>If either operand is NaN, the result is NaN.</li>
     * <li>If the result is not NaN, the sign of the result equals the sign of the dividend.</li>
     * <li>If the dividend is an infinity, or the divisor is a zero, or both, the result is NaN.</li>
     * <li>If the dividend is finite and the divisor is an infinity, the result equals the dividend.</li>
     * <li>If the dividend is a zero and the divisor is finite, the result equals the dividend.</li>
     * </ul>
     * 
     * @param dividend the number to be divided
     * @param divisor the number by which to divide
     * @return the remainder, rounded
     * @throws ArithmeticException thrown if input cannot allow to compute remainder
     */
    // CHECKSTYLE: stop MethodName
    // Reason: name kept as such
    double IEEEremainder(final double dividend, final double divisor);

    /**
     * Returns the first argument with the sign of the second argument. A NaN {@code sign} argument
     * is treated as positive.
     * 
     * @param magnitude the value to return
     * @param sign the sign for the returned value
     * @return the magnitude with the same sign as the {@code sign} argument
     */
    double copySign(final double magnitude, final double sign);

    /**
     * Returns the first argument with the sign of the second argument. A NaN {@code sign} argument
     * is treated as positive.
     * 
     * @param magnitude the value to return
     * @param sign the sign for the returned value
     * @return the magnitude with the same sign as the {@code sign} argument
     */
    float copySign(final float magnitude, final float sign);

    /**
     * Return the exponent of a double number, removing the bias.
     * <p>
     * For double numbers of the form 2<sup>x</sup>, the unbiased exponent is exactly x.
     * </p>
     * 
     * @param d number from which exponent is requested
     * @return exponent for d in IEEE754 representation, without bias
     */
    int getExponent(final double d);

    /**
     * Return the exponent of a float number, removing the bias.
     * <p>
     * For float numbers of the form 2<sup>x</sup>, the unbiased exponent is exactly x.
     * </p>
     * 
     * @param f number from which exponent is requested
     * @return exponent for d in IEEE754 representation, without bias
     */
    int getExponent(final float f);

    /**
     * Computes sine and cosine of an angle together.
     * @param x angle in radians
     * @return [angle sine, angle cosine]
     */
    double[] sinAndCos(final double x);

    /**
     * Computes sine and cosine of an angle together.
     * Values are returned in the array provided [angle sine, angle cosine]. This array should be of size 2.
     * Warning: array size is not checked for performances reasons.
     * @param x angle in radians
     * @param sincos array of size 2. Array is filled is values: [angle sine, angle cosine]
     */
    void sinAndCos(final double x, final double[] sincos);

    /**
     * Computes hyperbolic sine and hyperbolic cosine together.
     * @param x value
     * @return [hyperbolic sine, hyperbolic cosine]
     */
    double[] sinhAndCosh(final double x);

    /**
     * Computes hyperbolic sine and hyperbolic cosine together.
     * Values are returned in the array provided [hyperbolic sine, hyperbolic cosine]. This array should be of size 2.
     * Warning: array size is not checked for performances reasons.
     * @param x value
     * @param sinhcosh array of size 2. Array is filled is values: [hyperbolic sine, hyperbolic cosine]
     */
    void sinhAndCosh(final double x, final double[] sinhcosh);
}
