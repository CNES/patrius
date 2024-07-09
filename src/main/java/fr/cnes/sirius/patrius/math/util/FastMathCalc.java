/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 *
 * Copyright 2011-2017 CNES
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
 */
package fr.cnes.sirius.patrius.math.util;

import java.io.PrintStream;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;

//CHECKSTYLE: stop CommentRatio check
//CHECKSTYLE: stop MagicNumber check
//Reason: model - Commons-Math code

/**
 * Class used to compute the classical functions tables.
 * 
 * @version $Id: FastMathCalc.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.0
 */
final class FastMathCalc {

    /**
     * 0x40000000 - used to split a double into two parts, both with the low order bits cleared.
     * Equivalent to 2^30.
     */
    // 1073741824L
    private static final long HEX_40000000 = 0x40000000L;

    /** Factorial table, for Taylor series expansions. 0!, 1!, 2!, ... 19! */
    private static final double[] FACT = new double[] {
        +1.0d,
        +1.0d,
        +2.0d,
        +6.0d,
        +24.0d,
        +120.0d,
        +720.0d,
        +5040.0d,
        +40320.0d,
        +362880.0d,
        +3628800.0d,
        +39916800.0d,
        +479001600.0d,
        +6227020800.0d,
        +87178291200.0d,
        +1307674368000.0d,
        +20922789888000.0d,
        +355687428096000.0d,
        +6402373705728000.0d,
        +121645100408832000.0d,
    };

    /** Coefficients for slowLog. */
    private static final double[][] LN_SPLIT_COEF = {
        { 2.0, 0.0 },
        { 0.6666666269302368, 3.9736429850260626E-8 },
        { 0.3999999761581421, 2.3841857910019882E-8 },
        { 0.2857142686843872, 1.7029898543501842E-8 },
        { 0.2222222089767456, 1.3245471311735498E-8 },
        { 0.1818181574344635, 2.4384203044354907E-8 },
        { 0.1538461446762085, 9.140260083262505E-9 },
        { 0.13333332538604736, 9.220590270857665E-9 },
        { 0.11764700710773468, 1.2393345855018391E-8 },
        { 0.10526403784751892, 8.251545029714408E-9 },
        { 0.0952233225107193, 1.2675934823758863E-8 },
        { 0.08713622391223907, 1.1430250008909141E-8 },
        { 0.07842259109020233, 2.404307984052299E-9 },
        { 0.08371849358081818, 1.176342548272881E-8 },
        { 0.030589580535888672, 1.2958646899018938E-9 },
        { 0.14982303977012634, 1.225743062930824E-8 },
    };

    /** Table start declaration. */
    private static final String TABLE_START_DECL = "    {";

    /** Table end declaration. */
    private static final String TABLE_END_DECL = "    };";

    /**
     * Private Constructor.
     */
    private FastMathCalc() {
    }

    /**
     * For x between 0 and 1, returns exp(x), uses extended precision
     * 
     * @param x
     *        argument of exponential
     * @param result
     *        placeholder where to place exp(x) split in two terms
     *        for extra precision (i.e. exp(x) = result[0] + result[1]
     * @return exp(x)
     */
    public static double slowexp(final double x, final double[] result) {
        final double[] xs = new double[2];
        final double[] ys = new double[2];
        final double[] facts = new double[2];
        final double[] as = new double[2];
        split(x, xs);
        ys[0] = 0.0;
        ys[1] = 0.0;

        for (int i = FACT.length - 1; i >= 0; i--) {
            splitMult(xs, ys, as);
            ys[0] = as[0];
            ys[1] = as[1];

            split(FACT[i], as);
            splitReciprocal(as, facts);

            splitAdd(ys, facts, as);
            ys[0] = as[0];
            ys[1] = as[1];
        }

        if (result != null) {
            result[0] = ys[0];
            result[1] = ys[1];
        }

        return ys[0] + ys[1];
    }

    /**
     * Compute split[0], split[1] such that their sum is equal to d,
     * and split[0] has its 30 least significant bits as zero.
     * 
     * @param d
     *        number to split
     * @param split
     *        placeholder where to place the result
     */
    private static void split(final double d, final double[] split) {
        if (d < 8e298 && d > -8e298) {
            final double a = d * HEX_40000000;
            split[0] = (d + a) - a;
            split[1] = d - split[0];
        } else {
            final double a = d * 9.31322574615478515625E-10;
            split[0] = (d + a - d) * HEX_40000000;
            split[1] = d - split[0];
        }
    }

    /**
     * Recompute a split.
     * 
     * @param a
     *        input/out array containing the split, changed
     *        on output
     */
    private static void resplit(final double[] a) {
        final double c = a[0] + a[1];
        final double d = -(c - a[0] - a[1]);

        if (c < 8e298 && c > -8e298) {
            final double z = c * HEX_40000000;
            a[0] = (c + z) - z;
            a[1] = c - a[0] + d;
        } else {
            final double z = c * 9.31322574615478515625E-10;
            a[0] = (c + z - c) * HEX_40000000;
            a[1] = c - a[0] + d;
        }
    }

    /**
     * Multiply two numbers in split form.
     * 
     * @param a
     *        first term of multiplication
     * @param b
     *        second term of multiplication
     * @param ans
     *        placeholder where to put the result
     */
    private static void splitMult(final double[] a, final double[] b, final double[] ans) {
        ans[0] = a[0] * b[0];
        ans[1] = a[0] * b[1] + a[1] * b[0] + a[1] * b[1];

        /* Resplit */
        resplit(ans);
    }

    /**
     * Add two numbers in split form.
     * 
     * @param a
     *        first term of addition
     * @param b
     *        second term of addition
     * @param ans
     *        placeholder where to put the result
     */
    private static void splitAdd(final double[] a, final double[] b, final double[] ans) {
        ans[0] = a[0] + b[0];
        ans[1] = a[1] + b[1];

        resplit(ans);
    }

    /**
     * Compute the reciprocal of in. Use the following algorithm.
     * in = c + d.
     * want to find x + y such that x+y = 1/(c+d) and x is much
     * larger than y and x has several zero bits on the right.
     * 
     * Set b = 1/(2^22), a = 1 - b. Thus (a+b) = 1.
     * Use following identity to compute (a+b)/(c+d)
     * 
     * (a+b)/(c+d) = a/c + (bc - ad) / (c^2 + cd)
     * set x = a/c and y = (bc - ad) / (c^2 + cd)
     * This will be close to the right answer, but there will be
     * some rounding in the calculation of X. So by carefully
     * computing 1 - (c+d)(x+y) we can compute an error and
     * add that back in. This is done carefully so that terms
     * of similar size are subtracted first.
     * 
     * @param v
     *        initial number, in split form
     * @param result
     *        placeholder where to put the result
     */
    public static void splitReciprocal(final double[] v, final double[] result) {
        final double b = 1.0 / 4194304.0;
        final double a = 1.0 - b;

        if (v[0] == 0.0) {
            v[0] = v[1];
            v[1] = 0.0;
        }

        result[0] = a / v[0];
        result[1] = (b * v[0] - a * v[1]) / (v[0] * v[0] + v[0] * v[1]);

        if (result[1] != result[1]) {
            // can happen if result[1] is NAN
            result[1] = 0.0;
        }

        /* Resplit */
        resplit(result);

        for (int i = 0; i < 2; i++) {
            /* this may be overkill, probably once is enough */
            double err = 1.0 - result[0] * v[0] - result[0] * v[1] -
                result[1] * v[0] - result[1] * v[1];
            /* err = 1.0 - err; */
            err = err * (result[0] + result[1]);
            /* printf("err = %16e\n", err); */
            result[1] += err;
        }
    }

    /**
     * Compute (a[0] + a[1]) * (b[0] + b[1]) in extended precision.
     * 
     * @param a
     *        first term of the multiplication
     * @param b
     *        second term of the multiplication
     * @param result
     *        placeholder where to put the result
     */
    private static void quadMult(final double[] a, final double[] b, final double[] result) {
        final double[] xs = new double[2];
        final double[] ys = new double[2];
        final double[] zs = new double[2];

        /* a[0] * b[0] */
        split(a[0], xs);
        split(b[0], ys);
        splitMult(xs, ys, zs);

        result[0] = zs[0];
        result[1] = zs[1];

        /* a[0] * b[1] */
        split(b[1], ys);
        splitMult(xs, ys, zs);

        double tmp = result[0] + zs[0];
        result[1] = result[1] - (tmp - result[0] - zs[0]);
        result[0] = tmp;
        tmp = result[0] + zs[1];
        result[1] = result[1] - (tmp - result[0] - zs[1]);
        result[0] = tmp;

        /* a[1] * b[0] */
        split(a[1], xs);
        split(b[0], ys);
        splitMult(xs, ys, zs);

        tmp = result[0] + zs[0];
        result[1] = result[1] - (tmp - result[0] - zs[0]);
        result[0] = tmp;
        tmp = result[0] + zs[1];
        result[1] = result[1] - (tmp - result[0] - zs[1]);
        result[0] = tmp;

        /* a[1] * b[0] */
        split(a[1], xs);
        split(b[1], ys);
        splitMult(xs, ys, zs);

        tmp = result[0] + zs[0];
        result[1] = result[1] - (tmp - result[0] - zs[0]);
        result[0] = tmp;
        tmp = result[0] + zs[1];
        result[1] = result[1] - (tmp - result[0] - zs[1]);
        result[0] = tmp;
    }

    /**
     * Compute exp(p) for a integer p in extended precision.
     * 
     * @param pIn
     *        integer whose exponential is requested
     * @param result
     *        placeholder where to put the result in extended precision
     * @return exp(p) in standard precision (equal to result[0] + result[1])
     */
    public static double expint(final int pIn, final double[] result) {

        int p = pIn;
        // double x = M_E;
        final double[] xs = new double[2];
        final double[] as = new double[2];
        final double[] ys = new double[2];
        // split(x, xs);
        // xs[1] = (double)(2.7182818284590452353602874713526625L - xs[0]);
        // xs[0] = 2.71827697753906250000;
        // xs[1] = 4.85091998273542816811e-06;
        // xs[0] = Double.longBitsToDouble(0x4005bf0800000000L);
        // xs[1] = Double.longBitsToDouble(0x3ed458a2bb4a9b00L);

        /* E */
        xs[0] = 2.718281828459045;
        xs[1] = 1.4456468917292502E-16;

        split(1.0, ys);

        while (p > 0) {
            if ((p & 1) != 0) {
                quadMult(ys, xs, as);
                ys[0] = as[0];
                ys[1] = as[1];
            }

            quadMult(xs, xs, as);
            xs[0] = as[0];
            xs[1] = as[1];

            p >>= 1;
        }

        if (result != null) {
            result[0] = ys[0];
            result[1] = ys[1];

            resplit(result);
        }

        return ys[0] + ys[1];
    }

    /**
     * xi in the range of [1, 2].
     * 3 5 7
     * x+1 / x x x \
     * ln ----- = 2 * | x + ---- + ---- + ---- + ... |
     * 1-x \ 3 5 7 /
     * 
     * So, compute a Remez approximation of the following function
     * 
     * ln ((sqrt(x)+1)/(1-sqrt(x))) / x
     * 
     * This will be an even function with only positive coefficents.
     * x is in the range [0 - 1/3].
     * 
     * Transform xi for input to the above function by setting
     * x = (xi-1)/(xi+1). Input to the polynomial is x^2, then
     * the result is multiplied by x.
     * 
     * @param xi
     *        number from which log is requested
     * @return log(xi)
     */
    public static double[] slowLog(final double xi) {
        final double[] x = new double[2];
        final double[] x2 = new double[2];
        final double[] y = new double[2];
        final double[] a = new double[2];

        split(xi, x);

        /* Set X = (x-1)/(x+1) */
        x[0] += 1.0;
        resplit(x);
        splitReciprocal(x, a);
        x[0] -= 2.0;
        resplit(x);
        splitMult(x, a, y);
        x[0] = y[0];
        x[1] = y[1];

        /* Square X -> X2 */
        splitMult(x, x, x2);

        // x[0] -= 1.0;
        // resplit(x);

        y[0] = LN_SPLIT_COEF[LN_SPLIT_COEF.length - 1][0];
        y[1] = LN_SPLIT_COEF[LN_SPLIT_COEF.length - 1][1];

        for (int i = LN_SPLIT_COEF.length - 2; i >= 0; i--) {
            splitMult(y, x2, a);
            y[0] = a[0];
            y[1] = a[1];
            splitAdd(y, LN_SPLIT_COEF[i], a);
            y[0] = a[0];
            y[1] = a[1];
        }

        splitMult(y, x, a);
        y[0] = a[0];
        y[1] = a[1];

        return y;
    }

    /**
     * Print an array.
     * 
     * @param ps
     *        text output stream where output should be printed
     * @param name
     *        array name
     * @param expectedLen
     *        expected length of the array
     * @param array2d
     *        array data
     */
    public static void printarray(final PrintStream ps, final String name, final int expectedLen,
                                  final double[][] array2d) {
        ps.println(name);
        checkLen(expectedLen, array2d.length);
        ps.println(TABLE_START_DECL + " ");
        int i = 0;
        for (final double[] array : array2d) {
            // "double array[]" causes PMD parsing error
            ps.print("        {");
            for (final double d : array) {
                // assume inner array has very few entries
                // multiple entries per line
                ps.printf("%-25.25s", format(d));
            }
            ps.println("}, // " + i++);
        }
        ps.println(TABLE_END_DECL);
    }

    /**
     * Format a double.
     * 
     * @param d
     *        double number to format
     * @return formatted number
     */
    public static String format(final double d) {
        if (d == d) {
            return ((d >= 0) ? "+" : "") + Double.toString(d) + "d,";
        } else {
            return "Double.NaN,";
        }
    }

    /**
     * Check two lengths are equal.
     * 
     * @param expectedLen
     *        expected length
     * @param actual
     *        actual length
     * @exception DimensionMismatchException
     *            if the two lengths are not equal
     */
    private static void checkLen(final int expectedLen, final int actual) {
        if (expectedLen != actual) {
            throw new DimensionMismatchException(actual, expectedLen);
        }
    }

    // CHECKSTYLE: resume CommentRatio check
    // CHECKSTYLE: resume MagicNumber check
}
