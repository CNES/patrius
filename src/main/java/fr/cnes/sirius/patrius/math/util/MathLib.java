/**
 * 
 * Copyright 2011-2022 CNES
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
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:FA:FA-2447:27/05/2020:Mathlib.divide() incomplète 
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.3:DM:DM-2090:15/05/2019:[PATRIUS] ajout de fonctionnalites aux bibliotheques mathematiques
 * VERSION:4.3:DM:DM-2089:15/05/2019:[PATRIUS] passage a Java 8
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.util;

import fr.cnes.sirius.patrius.math.framework.FastMathWrapper;
import fr.cnes.sirius.patrius.math.framework.FastestMathLibWrapper;
import fr.cnes.sirius.patrius.math.framework.JafamaFastMathWrapper;
import fr.cnes.sirius.patrius.math.framework.JafamaStrictFastMathWrapper;
import fr.cnes.sirius.patrius.math.framework.MathLibrary;
import fr.cnes.sirius.patrius.math.framework.MathLibraryType;
import fr.cnes.sirius.patrius.math.framework.MathWrapper;
import fr.cnes.sirius.patrius.math.framework.StrictMathWrapper;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.PatriusRuntimeException;

/**
 * Math library. This static class contains all low-level Math function such as abs(), sin(), cos(),
 * exp(), etc. It simply encapsulate a {@link MathLibrary}. By default {@link FastMathWrapper} which
 * encapsulate {@link MathLib} is used.
 * <p>
 * The user can define its own Math library used in PATRIUS by implementing {@link MathLibrary} and defining it as the
 * Math library to use with method {@link #setMathLibrary(MathLibrary)}
 * </p>
 * <p>
 * Be aware that some {@link MathLib} of JAFAMA functions may not be included in Mathlib since only all commons
 * functions between FastMath and JAFAMA are listed in MathLib (e.g. {@link FastMath#log(double, double)}.
 * </p>
 *
 * @author Emmanuel Bignon
 *
 * @since 4.2
 */
public final class MathLib {

    static {
        // Set default Math library to FastMath
        setMathLibrary(MathLibraryType.FASTMATH);
    };

    /** Archimede's constant PI, ratio of circle circumference to diameter. */
    public static final double PI = 105414357.0 / 33554432.0 + 1.984187159361080883e-9;

    /** Napier's constant e, base of the natural logarithm. */
    public static final double E = 2850325.0 / 1048576.0 + 8.254840070411028747e-8;

    /** Math library used in PATRIUS. */
    private static MathLibrary mathLibrary;

    /**
     * Constructor.
     */
    private MathLib() {
    }

    /**
     * Set Math library.
     * <p>
     * This methods allows user to define its own Math library.
     * </p>
     * 
     * @param library library
     */
    public static void setMathLibrary(final MathLibrary library) {
        mathLibrary = library;
    }

    /**
     * Set Math library using predefined values.
     * 
     * @param libraryType library type
     */
    public static void setMathLibrary(final MathLibraryType libraryType) {
        switch (libraryType) {
            case MATH:
                // Math wrapper
                setMathLibrary(new MathWrapper());
                break;
            case STRICTMATH:
                // StrictMath wrapper
                setMathLibrary(new StrictMathWrapper());
                break;
            case FASTMATH:
                // FastMath wrapper
                setMathLibrary(new FastMathWrapper());
                break;
            case JAFAMA_FASTMATH:
                // Jafama Fatest FastMath wrapper
                setMathLibrary(new JafamaFastMathWrapper());
                break;
            case JAFAMA_STRICT_FASTMATH:
                // Jafama Strict FastMath wrapper
                setMathLibrary(new JafamaStrictFastMathWrapper());
                break;
            case FASTEST_MATHLIB:
                // Fatest FastMath wrapper
                setMathLibrary(new FastestMathLibWrapper());
                break;
            default:
                // Cannot happen
                throw new PatriusRuntimeException(PatriusMessages.UNKNOWN_PARAMETER, null);
        }
    }

    /**
     * Compute the square root of a number.
     * 
     * @param a number on which evaluation is done
     * @return square root of a
     * @throws ArithmeticException thrown if a < 0 or a is NaN
     */
    public static double sqrt(final double a) {
        if (Double.isNaN(a)) {
            throw new ArithmeticException("Input of sqrt(x) is NaN.");
        }
        if (a < 0) {
            throw new ArithmeticException("sqrt(x) with x < 0 is not defined.");
        }
        return mathLibrary.sqrt(a);
    }

    /**
     * Compute the hyperbolic cosine of a number.
     * 
     * @param x number on which evaluation is done
     * @return hyperbolic cosine of x
     * @throws ArithmeticException thrown if x is NaN
     */
    public static double cosh(final double x) {
        if (Double.isNaN(x)) {
            throw new ArithmeticException("Input of cosh(x) is NaN.");
        }
        return mathLibrary.cosh(x);
    }

    /**
     * Compute the hyperbolic sine of a number.
     * 
     * @param x number on which evaluation is done
     * @return hyperbolic sine of x
     * @throws ArithmeticException thrown if x is NaN
     */
    public static double sinh(final double x) {
        if (Double.isNaN(x)) {
            throw new ArithmeticException("Input of sinh(x) is NaN.");
        }
        return mathLibrary.sinh(x);
    }

    /**
     * Compute the hyperbolic tangent of a number.
     * 
     * @param x number on which evaluation is done
     * @return hyperbolic tangent of x
     * @throws ArithmeticException thrown if a is NaN
     */
    public static double tanh(final double x) {
        if (Double.isNaN(x)) {
            throw new ArithmeticException("Input of tanh(x) is NaN.");
        }
        return mathLibrary.tanh(x);
    }

    /**
     * Compute the inverse hyperbolic cosine of a number.
     * 
     * @param a number on which evaluation is done
     * @return inverse hyperbolic cosine of a
     * @throws ArithmeticException thrown if a < 1 or a is NaN
     */
    public static double acosh(final double a) {
        if (Double.isNaN(a)) {
            throw new ArithmeticException("Input of acosh(x) is NaN.");
        }
        if (a < 1) {
            throw new ArithmeticException("acosh(x) with x < 1 is not defined.");
        }
        return mathLibrary.acosh(a);
    }

    /**
     * Compute the inverse hyperbolic sine of a number.
     * 
     * @param a number on which evaluation is done
     * @return inverse hyperbolic sine of a
     * @throws ArithmeticException thrown if a is NaN
     */
    public static double asinh(final double a) {
        if (Double.isNaN(a)) {
            throw new ArithmeticException("Input of asinh(x) is NaN.");
        }
        return mathLibrary.asinh(a);
    }

    /**
     * Compute the inverse hyperbolic tangent of a number.
     * 
     * @param a number on which evaluation is done
     * @return inverse hyperbolic tangent of a
     * @throws ArithmeticException thrown if a > 1 or a < -1 or a is NaN
     */
    public static double atanh(final double a) {
        if (Double.isNaN(a)) {
            throw new ArithmeticException("Input of atanh(x) is NaN.");
        }
        if (a > 1.0 || a < -1.0) {
            throw new ArithmeticException("atan(x) with x > 1 or x < -1 is not defined.");
        }
        return mathLibrary.atanh(a);
    }

    /**
     * Compute the signum of a number. The signum is -1 for negative numbers, +1 for positive
     * numbers and 0 otherwise
     * 
     * @param a number on which evaluation is done
     * @return -1.0, -0.0, +0.0, +1.0 or NaN depending on sign of a
     * @throws ArithmeticException thrown if a is NaN
     */
    public static double signum(final double a) {
        if (Double.isNaN(a)) {
            throw new ArithmeticException("Input of signum(x) is NaN.");
        }
        return mathLibrary.signum(a);
    }

    /**
     * Compute the signum of a number. The signum is -1 for negative numbers, +1 for positive
     * numbers and 0 otherwise
     * 
     * @param a number on which evaluation is done
     * @return -1.0, -0.0, +0.0, +1.0 or NaN depending on sign of a
     * @throws ArithmeticException thrown if a is NaN
     */
    public static float signum(final float a) {
        if (Float.isNaN(a)) {
            throw new ArithmeticException("Input of signum(x [float]) is NaN.");
        }
        return mathLibrary.signum(a);
    }

    /**
     * Compute next number towards positive infinity.
     * 
     * @param a number to which neighbor should be computed
     * @return neighbor of a towards positive infinity
     * @throws ArithmeticException thrown if a is NaN
     */
    public static double nextUp(final double a) {
        if (Double.isNaN(a)) {
            throw new ArithmeticException("Input of nextUp(x) is NaN.");
        }
        return mathLibrary.nextUp(a);
    }

    /**
     * Compute next number towards positive infinity.
     * 
     * @param a number to which neighbor should be computed
     * @return neighbor of a towards positive infinity
     * @throws ArithmeticException thrown if a is NaN
     */
    public static float nextUp(final float a) {
        if (Float.isNaN(a)) {
            throw new ArithmeticException("Input of nextUp(x [float]) is NaN.");
        }
        return mathLibrary.nextUp(a);
    }

    /**
     * Returns a pseudo-random number between 0.0 and 1.0.
     * 
     * @return a random number between 0.0 and 1.0
     */
    public static double random() {
        return mathLibrary.random();
    }

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
     * @throws ArithmeticException thrown if x is NaN
     */
    public static double exp(final double x) {
        if (Double.isNaN(x)) {
            throw new ArithmeticException("Input of exp(x) is NaN.");
        }
        return mathLibrary.exp(x);
    }

    /**
     * Compute exp(x) - 1
     * 
     * @param x number to compute shifted exponential
     * @return exp(x) - 1
     * @throws ArithmeticException thrown if x is NaN
     */
    public static double expm1(final double x) {
        if (Double.isNaN(x)) {
            throw new ArithmeticException("Input of expm1(x) is NaN.");
        }
        return mathLibrary.expm1(x);
    }

    /**
     * Natural logarithm.
     * 
     * @param x a double
     * @return log(x)
     * @throws ArithmeticException thrown if x < 0 or x is NaN
     */
    public static double log(final double x) {
        if (Double.isNaN(x)) {
            throw new ArithmeticException("Input of log(x) is NaN.");
        }
        if (x < 0) {
            throw new ArithmeticException("log(x) with x < 0 is not defined.");
        }
        return mathLibrary.log(x);
    }

    /**
     * Computes log(1 + x).
     * 
     * @param x Number.
     * @return {@code log(1 + x)}.
     * @throws ArithmeticException thrown if x < 0 or x is NaN
     */
    public static double log1p(final double x) {
        if (Double.isNaN(x)) {
            throw new ArithmeticException("Input of log1p(x) is NaN.");
        }
        if (x < -1) {
            throw new ArithmeticException("log1p(x) with x < -1 is not defined.");
        }
        return mathLibrary.log1p(x);
    }

    /**
     * Compute the base 10 logarithm.
     * 
     * @param x a number
     * @return log10(x)
     * @throws ArithmeticException thrown if x < 0 or x is NaN
     */
    public static double log10(final double x) {
        if (Double.isNaN(x)) {
            throw new ArithmeticException("Input of log10(x) is NaN.");
        }
        if (x < 0) {
            throw new ArithmeticException("log10(x) with x < 0 is not defined.");
        }
        return mathLibrary.log10(x);
    }

    /**
     * Power function. Compute x^y.
     * 
     * @param x a double
     * @param y a double
     * @return double
     * @throws ArithmeticException thrown if y < 0 and y not an integer or x or y is NaN or x = -1
     */
    public static double pow(final double x, final double y) {
        // safety check
        if (Double.isNaN(x) || Double.isNaN(y)) {
            // raise an exception if the x or y is NaN
            throw new ArithmeticException("Input of pow(x, y) is NaN.");
        }
        if (Double.isInfinite(y) && x == -1.0) {
            throw new ArithmeticException("pow(-1, +/-Inf) is NaN.");
        }
        if (y == Double.POSITIVE_INFINITY && x * x == 1.0) {
            throw new ArithmeticException("pow(-1, +Inf) is NaN.");
        }
        if (y == Double.NEGATIVE_INFINITY && x == -1.0) {
            throw new ArithmeticException("pow(-1, -Inf) is NaN.");
        }
        if (y == Double.NEGATIVE_INFINITY && x == 1) {
            // Specific case: wrong FastMath/Jafama definition
            return 1.0;
        }
        // return x pows y
        return mathLibrary.pow(x, y);
    }

    /**
     * Raise a double to an int power.
     * 
     * @param d Number to raise.
     * @param e Exponent.
     * @return d<sup>e</sup>
     * @since 3.1
     * @throws ArithmeticException thrown if d is NaN
     */
    public static double pow(final double d, final int e) {
        if (Double.isNaN(d)) {
            throw new ArithmeticException("Input of pow(x, y [int]) is NaN.");
        }
        return mathLibrary.pow(d, e);
    }

    /**
     * Sine function.
     * 
     * @param x Argument.
     * @return sin(x)
     * @throws ArithmeticException thrown if x is NaN or Infinity
     */
    public static double sin(final double x) {
        if (Double.isNaN(x) || Double.isInfinite(x)) {
            throw new ArithmeticException("Input of sin(x) is NaN or Infinity.");
        }
        return mathLibrary.sin(x);
    }

    /**
     * Cosine function.
     * 
     * @param x Argument.
     * @return cos(x)
     * @throws ArithmeticException thrown if x is NaN or Infinity
     */
    public static double cos(final double x) {
        if (Double.isNaN(x) || Double.isInfinite(x)) {
            throw new ArithmeticException("Input of cos(x) is NaN or Infinity.");
        }
        return mathLibrary.cos(x);
    }

    /**
     * Tangent function.
     * 
     * @param x Argument.
     * @return tan(x)
     * @throws ArithmeticException thrown if x is NaN or Infinity
     */
    public static double tan(final double x) {
        if (Double.isNaN(x) || Double.isInfinite(x)) {
            throw new ArithmeticException("Input of tan(x) is NaN or Infinity.");
        }
        return mathLibrary.tan(x);
    }

    /**
     * Arctangent function
     * 
     * @param x a number
     * @return atan(x)
     * @throws ArithmeticException thrown if x is NaN
     */
    public static double atan(final double x) {
        if (Double.isNaN(x)) {
            throw new ArithmeticException("Input of atan(x) is NaN.");
        }
        return mathLibrary.atan(x);
    }

    /**
     * Two arguments arctangent function
     * 
     * @param y ordinate
     * @param x abscissa
     * @return phase angle of point (x,y) between {@code -PI} and {@code PI}
     * @throws ArithmeticException thrown if x or y is NaN
     */
    public static double atan2(final double y, final double x) {
        if (Double.isNaN(x) || Double.isNaN(y)) {
            throw new ArithmeticException("Input of atan(x, y) is NaN.");
        }
        return mathLibrary.atan2(y, x);
    }

    /**
     * Compute the arc sine of a number.
     * 
     * @param x number on which evaluation is done
     * @return arc sine of x
     * @throws ArithmeticException thrown if x < -1 or x > 1 or x is NaN
     */
    public static double asin(final double x) {
        if (Double.isNaN(x)) {
            throw new ArithmeticException("Input of asin(x) is NaN.");
        }
        if (x > 1.0 || x < -1.0) {
            throw new ArithmeticException("asin(x) with x > 1 or x < -1 is not defined.");
        }
        return mathLibrary.asin(x);
    }

    /**
     * Compute the arc cosine of a number.
     * 
     * @param x number on which evaluation is done
     * @return arc cosine of x
     * @throws ArithmeticException thrown if x < -1 or x > 1 or x is NaN
     */
    public static double acos(final double x) {
        if (Double.isNaN(x)) {
            throw new ArithmeticException("Input of acos(x) is NaN.");
        }
        if (x > 1.0 || x < -1.0) {
            throw new ArithmeticException("acos(x) with x > 1 or x < -1 is not defined.");
        }
        return mathLibrary.acos(x);
    }

    /**
     * Compute the cubic root of a number.
     * 
     * @param x number on which evaluation is done
     * @return cubic root of x
     * @throws ArithmeticException thrown if x is NaN
     */
    public static double cbrt(final double x) {
        if (Double.isNaN(x)) {
            throw new ArithmeticException("Input of cbrt(x) is NaN.");
        }
        return mathLibrary.cbrt(x);
    }

    /**
     * Convert degrees to radians, with error of less than 0.5 ULP
     * 
     * @param x angle in degrees
     * @return x converted into radians
     * @throws ArithmeticException thrown if x is NaN
     */
    public static double toRadians(final double x) {
        if (Double.isNaN(x)) {
            throw new ArithmeticException("Input of toRadians(x) is NaN.");
        }
        return mathLibrary.toRadians(x);
    }

    /**
     * Convert radians to degrees, with error of less than 0.5 ULP
     * 
     * @param x angle in radians
     * @return x converted into degrees
     * @throws ArithmeticException thrown if x is NaN
     */
    public static double toDegrees(final double x) {
        if (Double.isNaN(x)) {
            throw new ArithmeticException("Input of toDegrees(x) is NaN.");
        }
        return mathLibrary.toDegrees(x);
    }

    /**
     * Absolute value.
     * 
     * @param x number from which absolute value is requested
     * @return abs(x)
     */
    public static int abs(final int x) {
        return mathLibrary.abs(x);
    }

    /**
     * Absolute value.
     * 
     * @param x number from which absolute value is requested
     * @return abs(x)
     */
    public static long abs(final long x) {
        return mathLibrary.abs(x);
    }

    /**
     * Absolute value.
     * 
     * @param x number from which absolute value is requested
     * @return abs(x)
     * @throws ArithmeticException thrown if x is NaN
     */
    public static float abs(final float x) {
        if (Float.isNaN(x)) {
            throw new ArithmeticException("Input of abs(x [float]) is NaN.");
        }
        return mathLibrary.abs(x);
    }

    /**
     * Absolute value.
     * 
     * @param x number from which absolute value is requested
     * @return abs(x)
     * @throws ArithmeticException thrown if x is NaN
     */
    public static double abs(final double x) {
        if (Double.isNaN(x)) {
            throw new ArithmeticException("Input of abs(x) is NaN.");
        }
        return mathLibrary.abs(x);
    }

    /**
     * Compute least significant bit (Unit in Last Position) for a number.
     * 
     * @param x number from which ulp is requested
     * @return ulp(x)
     * @throws ArithmeticException thrown if x is NaN
     */
    public static double ulp(final double x) {
        if (Double.isNaN(x)) {
            throw new ArithmeticException("Input of ulp(x) is NaN.");
        }
        return mathLibrary.ulp(x);
    }

    /**
     * Compute least significant bit (Unit in Last Position) for a number.
     * 
     * @param x number from which ulp is requested
     * @return ulp(x)
     * @throws ArithmeticException thrown if x is NaN
     */
    public static float ulp(final float x) {
        if (Float.isNaN(x)) {
            throw new ArithmeticException("Input of ulp(x [float]) is NaN.");
        }
        return mathLibrary.ulp(x);
    }

    /**
     * Multiply a double number by a power of 2.
     * 
     * @param d number to multiply
     * @param n power of 2
     * @return d &times; 2<sup>n</sup>
     * @throws ArithmeticException thrown if d is NaN
     */
    public static double scalb(final double d, final int n) {
        if (Double.isNaN(d)) {
            throw new ArithmeticException("Input of scalb(x, y) is NaN.");
        }
        return mathLibrary.scalb(d, n);
    }

    /**
     * Multiply a float number by a power of 2.
     * 
     * @param f number to multiply
     * @param n power of 2
     * @return f &times; 2<sup>n</sup>
     * @throws ArithmeticException thrown if f is NaN
     */
    public static float scalb(final float f, final int n) {
        if (Float.isNaN(f)) {
            throw new ArithmeticException("Input of scalb(x [float], y) is NaN.");
        }
        return mathLibrary.scalb(f, n);
    }

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
     * @throws ArithmeticException thrown if d or direction is NaN
     */
    public static double nextAfter(final double d, final double direction) {
        if (Double.isNaN(d) || Double.isNaN(direction)) {
            throw new ArithmeticException("Input of nextAfter(x, direction) is NaN.");
        }
        return mathLibrary.nextAfter(d, direction);
    }

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
     * @throws ArithmeticException thrown if f or direction is NaN
     */
    public static float nextAfter(final float f, final double direction) {
        if (Double.isNaN(f) || Double.isNaN(direction)) {
            throw new ArithmeticException("Input of nextAfter(x [float], direction) is NaN.");
        }
        return mathLibrary.nextAfter(f, direction);
    }

    /**
     * Get the largest whole number smaller than x.
     * 
     * @param x number from which floor is requested
     * @return a double number f such that f is an integer f <= x < f + 1.0
     * @throws ArithmeticException thrown if x is NaN
     */
    public static double floor(final double x) {
        if (Double.isNaN(x)) {
            throw new ArithmeticException("Input of floor(x) is NaN.");
        }
        return mathLibrary.floor(x);
    }

    /**
     * Get the smallest whole number larger than x.
     * 
     * @param x number from which ceil is requested
     * @return a double number c such that c is an integer c - 1.0 < x <= c
     * @throws ArithmeticException thrown if x is NaN
     */
    public static double ceil(final double x) {
        if (Double.isNaN(x)) {
            throw new ArithmeticException("Input of ceil(x) is NaN.");
        }
        return mathLibrary.ceil(x);
    }

    /**
     * Get the whole number that is the nearest to x, or the even one if x is exactly half way
     * between two integers.
     * 
     * @param x number from which nearest whole number is requested
     * @return a double number r such that r is an integer r - 0.5 <= x <= r + 0.5
     * @throws ArithmeticException thrown if x is NaN
     */
    public static double rint(final double x) {
        if (Double.isNaN(x)) {
            throw new ArithmeticException("Input of rint(x) is NaN.");
        }
        return mathLibrary.rint(x);
    }

    /**
     * Get the closest long to x.
     * 
     * @param x number from which closest long is requested
     * @return closest long to x
     * @throws ArithmeticException thrown if x is NaN
     */
    public static long round(final double x) {
        if (Double.isNaN(x)) {
            throw new ArithmeticException("Input of round(x) is NaN.");
        }
        return mathLibrary.round(x);
    }

    /**
     * Get the closest int to x.
     * 
     * @param x number from which closest int is requested
     * @return closest int to x
     * @throws ArithmeticException thrown if x is NaN
     */
    public static int round(final float x) {
        if (Float.isNaN(x)) {
            throw new ArithmeticException("Input of round(x [float]) is NaN.");
        }
        return mathLibrary.round(x);
    }

    /**
     * Compute the minimum of two values
     * 
     * @param a first value
     * @param b second value
     * @return a if a is lesser or equal to b, b otherwise
     */
    public static int min(final int a, final int b) {
        return mathLibrary.min(a, b);
    }

    /**
     * Compute the minimum of two values
     * 
     * @param a first value
     * @param b second value
     * @return a if a is lesser or equal to b, b otherwise
     */
    public static long min(final long a, final long b) {
        return mathLibrary.min(a, b);
    }

    /**
     * Compute the minimum of two values
     * 
     * @param a first value
     * @param b second value
     * @return a if a is lesser or equal to b, b otherwise
     * @throws ArithmeticException thrown if a or b is NaN
     */
    public static float min(final float a, final float b) {
        if (Float.isNaN(a) || Float.isNaN(b)) {
            throw new ArithmeticException("Input of min(x [float], y [float]) is NaN.");
        }
        return mathLibrary.min(a, b);
    }

    /**
     * Compute the minimum of two values
     * 
     * @param a first value
     * @param b second value
     * @return a if a is lesser or equal to b, b otherwise
     * @throws ArithmeticException thrown if a or b is NaN
     */
    public static double min(final double a, final double b) {
        if (Double.isNaN(a) || Double.isNaN(b)) {
            throw new ArithmeticException("Input of min(x, y) is NaN.");
        }
        return mathLibrary.min(a, b);
    }

    /**
     * Compute the maximum of two values
     * 
     * @param a first value
     * @param b second value
     * @return b if a is lesser or equal to b, a otherwise
     */
    public static int max(final int a, final int b) {
        return mathLibrary.max(a, b);
    }

    /**
     * Compute the maximum of two values
     * 
     * @param a first value
     * @param b second value
     * @return b if a is lesser or equal to b, a otherwise
     */
    public static long max(final long a, final long b) {
        return mathLibrary.max(a, b);
    }

    /**
     * Compute the maximum of two values
     * 
     * @param a first value
     * @param b second value
     * @return b if a is lesser or equal to b, a otherwise
     * @throws ArithmeticException thrown if a or b is NaN
     */
    public static float max(final float a, final float b) {
        if (Float.isNaN(a) || Float.isNaN(b)) {
            throw new ArithmeticException("Input of max(x [float], y [float]) is NaN.");
        }
        return mathLibrary.max(a, b);
    }

    /**
     * Compute the maximum of two values
     * 
     * @param a first value
     * @param b second value
     * @return b if a is lesser or equal to b, a otherwise
     * @throws ArithmeticException thrown if a or b is NaN
     */
    public static double max(final double a, final double b) {
        if (Double.isNaN(a) || Double.isNaN(b)) {
            throw new ArithmeticException("Input of max(x, y) is NaN.");
        }
        return mathLibrary.max(a, b);
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
     * @param x a value
     * @param y a value
     * @return sqrt(<i>x</i><sup>2</sup>&nbsp;+<i>y</i><sup>2</sup>)
     * @throws ArithmeticException thrown if x or y is NaN
     */
    public static double hypot(final double x, final double y) {
        if (Double.isNaN(x) || Double.isNaN(y)) {
            throw new ArithmeticException("Input of hypot(x, y) is NaN.");
        }
        return mathLibrary.hypot(x, y);
    }

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
    public static double IEEEremainder(final double dividend, final double divisor) {
        final double res = mathLibrary.IEEEremainder(dividend, divisor);
        if (Double.isNaN(res)) {
            throw new ArithmeticException(
                "Input of IEEEremainder(x, y) is such that returned value is NaN.");
        }
        return res;
    }

    /**
     * Returns the first argument with the sign of the second argument. A NaN {@code sign} argument
     * is treated as positive.
     * 
     * @param magnitude the value to return
     * @param sign the sign for the returned value
     * @return the magnitude with the same sign as the {@code sign} argument
     * @throws ArithmeticException thrown if magnitude is NaN
     */
    public static double copySign(final double magnitude, final double sign) {
        if (Double.isNaN(magnitude) || Double.isNaN(sign)) {
            throw new ArithmeticException("Input of copySign(x, y) is NaN.");
        }
        return mathLibrary.copySign(magnitude, sign);
    }

    /**
     * Returns the first argument with the sign of the second argument. A NaN {@code sign} argument
     * is treated as positive.
     * 
     * @param magnitude the value to return
     * @param sign the sign for the returned value
     * @return the magnitude with the same sign as the {@code sign} argument
     * @throws ArithmeticException thrown if magnitude is NaN
     */
    public static float copySign(final float magnitude, final float sign) {
        if (Float.isNaN(magnitude) || Float.isNaN(sign)) {
            throw new ArithmeticException("Input of copySign(x [float], y [float]) is NaN.");
        }
        return mathLibrary.copySign(magnitude, sign);
    }

    /**
     * Return the exponent of a double number, removing the bias.
     * <p>
     * For double numbers of the form 2<sup>x</sup>, the unbiased exponent is exactly x.
     * </p>
     * 
     * @param d number from which exponent is requested
     * @return exponent for d in IEEE754 representation, without bias
     */
    public static int getExponent(final double d) {
        return mathLibrary.getExponent(d);
    }

    /**
     * Return the exponent of a float number, removing the bias.
     * <p>
     * For float numbers of the form 2<sup>x</sup>, the unbiased exponent is exactly x.
     * </p>
     * 
     * @param f number from which exponent is requested
     * @return exponent for d in IEEE754 representation, without bias
     */
    public static int getExponent(final float f) {
        return mathLibrary.getExponent(f);
    }

    /**
     * Computes x / y.
     * This method returns an exception if output is NaN or y = 0.
     * 
     * @param x numerator
     * @param y denominator
     * @return x / y
     * @throws ArithmeticException thrown if x / y = NaN or y = 0
     */
    public static double divide(final double x, final double y) {
        if (y == 0.) {
            throw new ArithmeticException("Unable to perform division by 0");
        }
        if (Double.isNaN(x) || Double.isNaN(y)) {
            throw new ArithmeticException("Input of divide(x, y) is NaN.");
        }

        return x / y;
    }

    /**
     * Computes sine and cosine of an angle together.
     * @param x angle in radians
     * @return [angle sine, angle cosine]
     */
    public static double[] sinAndCos(final double x) {
        if (Double.isNaN(x) || Double.isInfinite(x)) {
            throw new ArithmeticException("Input of sinAndCos(x) is NaN or Infinity.");
        }
        return mathLibrary.sinAndCos(x);
    }

    /**
     * Computes sine and cosine of an angle together.
     * Values are returned in the array provided [angle sine, angle cosine]. This array should be of size 2.
     * Warning: array size is not checked for performances reasons.
     * @param x angle in radians
     * @param sincos array of size 2. Array is filled is values: [angle sine, angle cosine]
     */
    public static void sinAndCos(final double x, final double[] sincos) {
        if (Double.isNaN(x) || Double.isInfinite(x)) {
            throw new ArithmeticException("Input of sinAndCos(x, sincos) is NaN or Infinity.");
        }
        mathLibrary.sinAndCos(x, sincos);
    }

    /**
     * Computes hyperbolic sine and hyperbolic cosine together.
     * @param x value
     * @return [hyperbolic sine, hyperbolic cosine]
     */
    public static double[] sinhAndCosh(final double x) {
        if (Double.isNaN(x)) {
            throw new ArithmeticException("Input of sinhAndCosh(x) is NaN.");
        }
        return mathLibrary.sinhAndCosh(x);
    }

    /**
     * Computes hyperbolic sine and hyperbolic cosine together.
     * Values are returned in the array provided [hyperbolic sine, hyperbolic cosine]. This array should be of size 2.
     * Warning: array size is not checked for performances reasons.
     * @param x value
     * @param sinhcosh array of size 2. Array is filled is values: [hyperbolic sine, hyperbolic cosine]
     */
    public static void sinhAndCosh(final double x, final double[] sinhcosh) {
        if (Double.isNaN(x)) {
            throw new ArithmeticException("Input of sinhAndCosh(x, sinhcosh) is NaN.");
        }
        mathLibrary.sinhAndCosh(x, sinhcosh);
    }
}
