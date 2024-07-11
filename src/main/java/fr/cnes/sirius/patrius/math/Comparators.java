/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 *
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
 * @history Creation 22/07/11
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.4:FA:FA-2137:04/10/2019:FA mineure Patrius V4.3
 * VERSION:4.3:FA:FA-2113:15/05/2019:Usage des exceptions pour gerer un if/else "nominal" dans
 * Ellipsoid.getPointLocation()
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:128:26/09/2013:Modified some methods for enhanced accuracy
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math;

import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.utils.UtilsPatrius;

/**
 * <p>
 * - Static comparison methods for real numbers
 * </p>
 * <p>
 * - Classical methods to compare doubles using an epsilon, as an input or with a default value
 * </p>
 * See DV-MATHS_30.
 * 
 * @useSample <p>
 *            Those methods can be used this way : if (greaterOrEqual(x, y)) {...}
 *            </p>
 *            <p>
 *            The condition is "true" if x is greater or equal to y (with a relative comparison using the default
 *            epsilon)
 *            </p>
 * 
 * @concurrency unconditionally thread-safe
 * 
 * @author Thomas TRAPIER
 * 
 * @version $Id: Comparators.java 17584 2017-05-10 13:26:39Z bignon $
 * 
 * @since 1.0
 * 
 */
public final class Comparators {

    /** The epsilon used for doubles relative comparison */
    public static final double DOUBLE_COMPARISON_EPSILON = UtilsPatrius.DOUBLE_COMPARISON_EPSILON;

    /**
     * Constructor<br>
     * Private constructor to avoid the user to create a Comparators object
     */
    private Comparators() {
    }

    /**
     * Tests the equality between doubles with a relative comparison using a default epsilon.
     * 
     * @param x
     *        first double to be compared
     * @param y
     *        second double to be compared
     * @return a boolean : "true" if the doubles are found equals.
     *         <p>
     *         The value "Nan" as input always imply the return "false"
     *         </p>
     * 
     * @since 1.0
     */
    public static boolean equals(final double x, final double y) {

        return equalsWithRelativeTolerance(x, y, DOUBLE_COMPARISON_EPSILON);
    }

    /**
     * Tests the equality between doubles with a relative comparison using an input epsilon.
     * 
     * @param x
     *        first double to be compared
     * @param y
     *        second double to be compared
     * @param eps
     *        epsilon used in the relative comparison
     * 
     * @return a boolean : "true" if the doubles are found equals.
     *         <p>
     *         The value "Nan" as input always imply the return "false"
     *         </p>
     * 
     * @since 1.0
     */
    public static boolean equals(final double x, final double y, final double eps) {
        // calls the method added in MathUtils
        return equalsWithRelativeTolerance(x, y, eps);
    }

    /**
     * Tests if a double is lower or equal to another with a relative comparison using a default epsilon.
     * 
     * @param x
     *        first double to be compared
     * @param y
     *        second double to be compared
     * @return a boolean : "true" if x is found lower or equal to y.
     *         <p>
     *         The value "Nan" as input always imply the return "false"
     *         </p>
     * 
     * @since 1.0
     */
    public static boolean lowerOrEqual(final double x, final double y) {

        return lowerOrEqual(x, y, DOUBLE_COMPARISON_EPSILON);
    }

    /**
     * Tests if a double is lower or equal to another with a relative comparison using an input epsilon.
     * 
     * @param x
     *        first double to be compared
     * @param y
     *        second double to be compared
     * @param eps
     *        epsilon used in the relative comparison
     * 
     * @return a boolean : "true" if x is found lower or equal to y.
     *         <p>
     *         The value "Nan" as input always imply the return "false"
     *         </p>
     * 
     * @since 1.0
     */
    public static boolean lowerOrEqual(final double x, final double y, final double eps) {

        // Initialisation
        boolean isLower = false;

        // test
        if (x < y || equalsWithRelativeTolerance(x, y, eps)) {
            isLower = true;
        }

        return isLower;
    }

    /**
     * Tests if a double is greater or equal to another with a relative comparison using a default epsilon.
     * 
     * @param x
     *        first double to be compared
     * @param y
     *        second double to be compared
     * @return a boolean : "true" if x is found greater or equal to y.
     *         <p>
     *         The value "Nan" as input always imply the return "false"
     *         </p>
     * 
     * @since 1.0
     */
    public static boolean greaterOrEqual(final double x, final double y) {

        return greaterOrEqual(x, y, DOUBLE_COMPARISON_EPSILON);
    }

    /**
     * Tests if a double is greater or equal to another with a relative comparison using an input epsilon.
     * 
     * @param x
     *        first double to be compared
     * @param y
     *        second double to be compared
     * @param eps
     *        epsilon used in the relative comparison
     * 
     * @return a boolean : "true" if x is found greater or equal to y.
     *         <p>
     *         The value "Nan" as input always imply the return "false"
     *         </p>
     * 
     * @since 1.0
     */
    public static boolean greaterOrEqual(final double x, final double y, final double eps) {
        // Initialisation
        boolean isGreater = false;

        // test
        if (y < x || equalsWithRelativeTolerance(x, y, eps)) {
            isGreater = true;
        }
        return isGreater;
    }

    /**
     * Tests if a double is strictly lower than another with a relative comparison using a default epsilon.
     * 
     * @param x
     *        first double to be compared
     * @param y
     *        second double to be compared
     * @return a boolean : "true" if x is found lower than y.
     *         <p>
     *         The value "Nan" as input always imply the return "false"
     *         </p>
     * 
     * @since 1.0
     */
    public static boolean lowerStrict(final double x, final double y) {

        return lowerStrict(x, y, DOUBLE_COMPARISON_EPSILON);
    }

    /**
     * Tests if a double is strictly lower than another with a relative comparison using an input epsilon.
     * 
     * @param x
     *        first double to be compared
     * @param y
     *        second double to be compared
     * @param eps
     *        epsilon used in the relative comparison
     * 
     * @return a boolean : "true" if x is found lower than y.
     *         <p>
     *         The value "Nan" as input always imply the return "false"
     *         </p>
     * 
     * @since 1.0
     */
    public static boolean lowerStrict(final double x, final double y, final double eps) {
        // Initialisation
        boolean isLower = false;

        // test
        if (x < y && !equalsWithRelativeTolerance(x, y, eps)) {
            isLower = true;
        }
        return isLower;
    }

    /**
     * Copied from commons math {@link Precision#equalsWithRelativeTolerance(double, double, double)}.
     * 
     * Returns {@code true} if the difference between the number is smaller or equal
     * to the given tolerance.
     * 
     * The difference is the call to the {@link Precision#equals(double, double, int)}. The ulp is 0 instead of 1. This
     * means that two adjacent numbers are not considered equal.
     * 
     * @param x
     *        First value.
     * @param y
     *        Second value.
     * @param eps
     *        Amount of allowed relative error.
     * @return {@code true} if the values are two adjacent floating point
     *         numbers or they are within range of each other.
     * @since 2.1
     */
    public static boolean equalsWithRelativeTolerance(final double x, final double y, final double eps) {

        // return true if x=y
        if (Precision.equals(x, y, 0)) {
            return true;
        }

        final double relativeDifference;
        // compute the relative difference
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
        // return true if the difference between the number is smaller or equal to the given tolerance
        return relativeDifference <= eps;
    }

    /**
     * Tests if a double is strictly greater than another with a relative comparison using a default epsilon.
     * 
     * @param x
     *        first double to be compared
     * @param y
     *        second double to be compared
     * @return a boolean : "true" if x is found greater than y.
     *         <p>
     *         The value "Nan" as input always imply the return "false"
     *         </p>
     * 
     * @since 1.0
     */
    public static boolean greaterStrict(final double x, final double y) {

        return greaterStrict(x, y, DOUBLE_COMPARISON_EPSILON);
    }

    /**
     * Tests if a double is strictly greater than another with a relative comparison using an input epsilon.
     * 
     * @param x
     *        first double to be compared
     * @param y
     *        second double to be compared
     * @param eps
     *        epsilon used in the relative comparison
     * 
     * @return a boolean : "true" if x is found greater than y.
     *         <p>
     *         The value "Nan" as input always imply the return "false"
     *         </p>
     * 
     * @since 1.0
     */
    public static boolean greaterStrict(final double x, final double y, final double eps) {
        // Initialisation
        boolean isGreater = false;

        // test
        if (y < x && !equalsWithRelativeTolerance(x, y, eps)) {
            isGreater = true;
        }
        return isGreater;
    }

}
