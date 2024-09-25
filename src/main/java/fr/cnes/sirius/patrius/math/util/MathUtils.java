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
 * VERSION:4.11:DM:DM-3197:22/05/2023:[PATRIUS] Deplacement dans PATRIUS de classes façade ALGO DV SIRUS 
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:2109:23/04/2019:correction of quality issue with angle normalization
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.util;

import java.util.Arrays;

import fr.cnes.sirius.patrius.math.exception.MathArithmeticException;
import fr.cnes.sirius.patrius.math.exception.NotFiniteNumberException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.exception.util.Localizable;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.PatriusRuntimeException;

/**
 * Miscellaneous utility functions.
 *
 * @see ArithmeticUtils
 * @see Precision
 * @see MathArrays
 *
 * @version $Id: MathUtils.java 18108 2017-10-04 06:45:27Z bignon $
 */
public final class MathUtils {

    /**
     * 2 &pi;.
     *
     * @since 2.1
     */
    public static final double TWO_PI = 2 * FastMath.PI;

    /**
     * &pi;/2.
     */
    public static final double HALF_PI = FastMath.PI / 2.0;

    /** 180. */
    public static final double HALF_CIRCLE = 180.;

    /**
     * &pi;/180.
     */
    public static final double DEG_TO_RAD = FastMath.PI / HALF_CIRCLE;

    /**
     * 180/&pi;.
     */
    public static final double RAD_TO_DEG = HALF_CIRCLE / FastMath.PI;

    /**
     * Class contains only static methods.
     */
    private MathUtils() {
    }

    /**
     * Returns an integer hash code representing the given double value.
     *
     * @param value
     *        the value to be hashed
     * @return the hash code
     */
    public static int hash(final double value) {
        return new Double(value).hashCode();
    }

    /**
     * Returns an integer hash code representing the given double array.
     *
     * @param value
     *        the value to be hashed (may be null)
     * @return the hash code
     * @since 1.2
     */
    public static int hash(final double[] value) {
        return Arrays.hashCode(value);
    }

    /**
     * Normalize an angle in a 2&pi wide interval around a center value.
     * <p>
     * This method has three main uses:
     * </p>
     * <ul>
     * <li>normalize an angle between 0 and 2&pi;:<br/>
     * {@code a = MathUtils.normalizeAngle(a, FastMath.PI);}</li>
     * <li>normalize an angle between -&pi; and +&pi;<br/>
     * {@code a = MathUtils.normalizeAngle(a, 0.0);}</li>
     * <li>compute the angle between two defining angular positions:<br>
     * {@code angle = MathUtils.normalizeAngle(end, start) - start;}</li>
     * </ul>
     * <p>
     * Note that due to numerical accuracy and since &pi; cannot be represented exactly, the result interval is
     * <em>closed</em>, it cannot be half-closed as would be more satisfactory in a purely mathematical view.
     * </p>
     *
     * @param a
     *        angle to normalize
     * @param center
     *        center of the desired 2&pi; interval for the result
     * @return a-2k&pi; with integer k and center-&pi; &lt;= a-2k&pi; &lt;= center+&pi;
     * @since 1.2
     */
    public static double normalizeAngle(final double a, final double center) {
        // Mathematical result
        double res = a - TWO_PI * MathLib.floor((a + (FastMath.PI - center)) / TWO_PI);

        // Handle specific case due to roundoff errors (example with a = -1E-324 to set in [0, 2Pi])
        if (res < center - FastMath.PI) {
            res += TWO_PI;
        } else if (res > center + FastMath.PI) {
            res -= TWO_PI;
        }
        return res;
    }

    /**
     * <p>
     * Reduce {@code |a - offset|} to the primary interval {@code [0, |period|)}.
     * </p>
     *
     * <p>
     * Specifically, the value returned is <br/>
     * {@code a - |period| * floor((a - offset) / |period|) - offset}.
     * </p>
     *
     * <p>
     * If any of the parameters are {@code NaN} or infinite, the result is {@code NaN}.
     * </p>
     *
     * @param a
     *        Value to reduce.
     * @param period
     *        Period.
     * @param offset
     *        Value that will be mapped to {@code 0}.
     * @return the value, within the interval {@code [0 |period|)},
     *         that corresponds to {@code a}.
     */
    public static double reduce(final double a,
                                final double period,
                                final double offset) {
        final double p = MathLib.abs(period);
        return a - p * MathLib.floor((a - offset) / p) - offset;
    }

    /**
     * Returns the first argument with the sign of the second argument.
     *
     * @param magnitude
     *        Magnitude of the returned value.
     * @param sign
     *        Sign of the returned value.
     * @return a value with magnitude equal to {@code magnitude} and with the
     *         same sign as the {@code sign} argument.
     * @throws MathArithmeticException
     *         if {@code magnitude == Byte.MIN_VALUE} and {@code sign >= 0}.
     */
    public static byte copySign(final byte magnitude, final byte sign) {
        if ((magnitude >= 0 && sign >= 0) ||
            (magnitude < 0 && sign < 0)) {
            // Sign is OK.
            return magnitude;
        } else if (sign >= 0 &&
            magnitude == Byte.MIN_VALUE) {
            throw new MathArithmeticException(PatriusMessages.OVERFLOW);
        } else {
            // Flip sign.
            return (byte) -magnitude;
        }
    }

    /**
     * Returns the first argument with the sign of the second argument.
     *
     * @param magnitude
     *        Magnitude of the returned value.
     * @param sign
     *        Sign of the returned value.
     * @return a value with magnitude equal to {@code magnitude} and with the
     *         same sign as the {@code sign} argument.
     * @throws MathArithmeticException
     *         if {@code magnitude == Integer.MIN_VALUE} and {@code sign >= 0}.
     */
    public static int copySign(final int magnitude, final int sign) {
        if ((magnitude >= 0 && sign >= 0) ||
            (magnitude < 0 && sign < 0)) {
            // Sign is OK.
            return magnitude;
        } else if (sign >= 0 &&
            magnitude == Integer.MIN_VALUE) {
            throw new MathArithmeticException(PatriusMessages.OVERFLOW);
        } else {
            // Flip sign.
            return -magnitude;
        }
    }

    /**
     * Returns the first argument with the sign of the second argument.
     *
     * @param magnitude
     *        Magnitude of the returned value.
     * @param sign
     *        Sign of the returned value.
     * @return a value with magnitude equal to {@code magnitude} and with the
     *         same sign as the {@code sign} argument.
     * @throws MathArithmeticException
     *         if {@code magnitude == Long.MIN_VALUE} and {@code sign >= 0}.
     */
    public static long copySign(final long magnitude, final long sign) {
        if ((magnitude >= 0 && sign >= 0) ||
            (magnitude < 0 && sign < 0)) {
            // Sign is OK.
            return magnitude;
        } else if (sign >= 0 &&
            magnitude == Long.MIN_VALUE) {
            throw new MathArithmeticException(PatriusMessages.OVERFLOW);
        } else {
            // Flip sign.
            return -magnitude;
        }
    }

    /**
     * Check that the argument is a real number.
     *
     * @param x
     *        Argument.
     * @throws NotFiniteNumberException
     *         if {@code x} is not a
     *         finite real number.
     */
    public static void checkFinite(final double x) {
        if (Double.isInfinite(x) || Double.isNaN(x)) {
            throw new NotFiniteNumberException(x);
        }
    }

    /**
     * Check that all the elements are real numbers.
     *
     * @param val
     *        Arguments.
     * @throws NotFiniteNumberException
     *         if any values of the array is not a
     *         finite real number.
     */
    public static void checkFinite(final double[] val) {
        for (int i = 0; i < val.length; i++) {
            final double x = val[i];
            if (Double.isInfinite(x) || Double.isNaN(x)) {
                throw new NotFiniteNumberException(PatriusMessages.ARRAY_ELEMENT, x, i);
            }
        }
    }

    /**
     * Checks that an object is not null.
     *
     * @param o
     *        Object to be checked.
     * @param pattern
     *        Message pattern.
     * @param args
     *        Arguments to replace the placeholders in {@code pattern}.
     * @throws NullArgumentException
     *         if {@code o} is {@code null}.
     */
    public static void checkNotNull(final Object o,
                                    final Localizable pattern,
                                    final Object... args) {
        if (o == null) {
            throw new NullArgumentException(pattern, args);
        }
    }

    /**
     * Checks that an object is not null.
     *
     * @param o
     *        Object to be checked.
     * @throws NullArgumentException
     *         if {@code o} is {@code null}.
     */
    public static void checkNotNull(final Object o) {
        if (o == null) {
            throw new NullArgumentException();
        }
    }

    /**
     * Calculation of a quadratic equation (ax^2 + bx + c) and return the positive root
     *
     * @param aCoefficient
     *        - a Coefficient
     * @param bCoefficient
     *        - b Coefficient
     * @param cCoefficient
     *        - c Coefficient
     * @return the calculate roots (-c/b if a==0, exception if a==b==0)
     */
    public static double[] solveQuadraticEquation(final double aCoefficient, final double bCoefficient,
            final double cCoefficient) {

        // Declare outputs
        final double root1;
        final double root2;

        // If the quadratic coefficient is zero, this is a linear equation
        if (aCoefficient == 0.0) {

            // The linear coefficient may also be zero --> Throw, since the equation is not well
            // conditioned
            if (bCoefficient == 0.0) {
                final PatriusMessages errMsg = PatriusMessages.WRONG_QUADRATIC_EQUATION;
                throw new PatriusRuntimeException(errMsg, null);

            } else {
                // Otherwise, return the root
                root1 = -cCoefficient / bCoefficient;
                root2 = root1;
            }

        } else {

            // Nominal case, we find the roots via the analytic formulation
            final double discriminant = MathLib.sqrt(MathLib.pow(bCoefficient, 2) - 4 * aCoefficient * cCoefficient);
            root1 = (-bCoefficient + discriminant) / (2 * aCoefficient);
            root2 = (-bCoefficient - discriminant) / (2 * aCoefficient);
        }

        return new double[] { root1, root2 };
    }

    /**
     * Calculation of a quadratic equation (ax^2 + bx + c) and return the positive root
     *
     * @param aCoefficient
     *        - a Coefficient
     * @param bCoefficient
     *        - b Coefficient
     * @param cCoefficient
     *        - c Coefficient
     * @return the max value of the two calculated roots
     */
    public static double solveQuadraticEquationMaxRoot(final double aCoefficient, final double bCoefficient,
            final double cCoefficient) {
        final double[] roots = solveQuadraticEquation(aCoefficient, bCoefficient, cCoefficient);
        return MathLib.max(roots[0], roots[1]);
    }

    /**
     * Calculation of a quadratic equation (ax^2 + bx + c) and return the positive root
     *
     * @param aCoefficient
     *        - a Coefficient
     * @param bCoefficient
     *        - b Coefficient
     * @param cCoefficient
     *        - c Coefficient
     * @return the min value of the two calculated roots
     */
    public static double solveQuadraticEquationMinRoot(final double aCoefficient, final double bCoefficient,
            final double cCoefficient) {
        final double[] roots = solveQuadraticEquation(aCoefficient, bCoefficient, cCoefficient);
        return MathLib.min(roots[0], roots[1]);
    }

    /**
     * Among 2 values, determines the smallest relative distance to a given number. In case of
     * equality, distance to a is returned.
     *
     * @param nb
     *        the given number
     * @param a
     *        the first value
     * @param b
     *        the second value
     * @return the nearest value to the given number
     */
    public static double findSmallestOffset(final double nb, final Double a, final Double b) {
        final double nbToA = MathLib.abs(nb - a);
        final double nbToB = MathLib.abs(nb - b);
        double offset = 0.0;
        if (nbToA <= nbToB) {
            offset = nb - a;
        } else {
            offset = nb - b;
        }
        return offset;
    }

    /**
     * Evaluate if the given values describe at least one {@link Double#NaN}.
     * 
     * <p>
     * This method should be used when multiple {@link Double#NaN} evaluations are performed to reduce the complexity.
     * </p>
     *
     * @param values
     *        Values to evaluate
     * @return {@code true} if at least one value is {@link Double#NaN}, {@code false} if all values aren't
     *         {@link Double#NaN}
     */
    public static boolean containsNaN(final double... values) {
        for (final double value : values) {
            if (Double.isNaN(value)) {
                return true;
            }
        }
        return false;
    }
}
