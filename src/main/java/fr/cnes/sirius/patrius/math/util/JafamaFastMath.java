/**
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
 * 
 * HISTORY
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.3:DM:DM-2090:15/05/2019:[PATRIUS] ajout de fonctionnalites aux bibliotheques mathematiques
 * VERSION:4.3:DM:DM-2089:15/05/2019:[PATRIUS] passage a Java 8
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.util;

import net.jafama.NumbersUtils;

// CHECKSTYLE: stop MagicNumber check
// CHECKSTYLE: stop FinalParameters check
// CHECKSTYLE: stop FinalLocalVariable check
//Reason: Jafama code kept as such

/**
 * This class sole purpose is to compute sin and cos in one single method.
 * Code is taken from Jafama FastMath class.
 * This class is not meant to be public. Public use is performed through {@link MathLib#sinAndCos(double)}.
 *
 * @author Emmanuel Bignon
 *
 * @since 4.5
 */
public final class JafamaFastMath {

    /** Constant. */
    private static final double PIO2_INV = Double.longBitsToDouble(0x3FE45F306DC9C883L); // 6.36619772367581382433e-01
                                                                                         // 53 bits of 2/pi
    /** Constant. */
    private static final double PIO2_HI = Double.longBitsToDouble(0x3FF921FB54400000L); // 1.57079632673412561417e+00
                                                                                        // first 33 bits of pi/2
    /** Constant. */
    private static final double PIO2_LO = Double.longBitsToDouble(0x3DD0B4611A626331L); // 6.07710050650619224932e-11
                                                                                        // pi/2 - PIO2_HI
    /** Constant. */
    private static final double TWOPI_HI = 4 * PIO2_HI;
    /** Constant. */
    private static final double TWOPI_LO = 4 * PIO2_LO;
    /** Constant. */
    private static final double ONE_DIV_F2 = 1 / 2.0;
    /** Constant. */
    private static final double ONE_DIV_F3 = 1 / 6.0;
    /** Constant. */
    private static final double ONE_DIV_F4 = 1 / 24.0;
    /** Constant. */
    private static final int SIN_COS_TABS_SIZE = (1 << 11) + 1;
    /** Constant. */
    private static final double SIN_COS_DELTA_HI = TWOPI_HI / (SIN_COS_TABS_SIZE - 1);
    /** Constant. */
    private static final double SIN_COS_DELTA_LO = TWOPI_LO / (SIN_COS_TABS_SIZE - 1);
    /** Constant. */
    private static final double SIN_COS_INDEXER = 1 / (SIN_COS_DELTA_HI + SIN_COS_DELTA_LO);
    /** Constant. */
    private static final double SIN_COS_MAX_VALUE_FOR_INT_MODULO = ((Integer.MAX_VALUE >> 9) / SIN_COS_INDEXER) * 0.99;
    /** Constant. */
    private static final double TWO_POW_24 = NumbersUtils.twoPow(24);
    /** Constant. */
    private static final double TWO_POW_N24 = NumbersUtils.twoPow(-24);
    /** Constant. */
    private static final int MAX_DOUBLE_EXPONENT = 1023;
    /** Constant. */
    private static final double SQRT_2 = StrictMath.sqrt(2.0);
    /** Constant. */
    private static final double TWO_POW_27 = NumbersUtils.twoPow(27);
    /** Constant. */
    private static final double LOG_TWO_POW_27 = StrictMath.log(TWO_POW_27);
    /** Constant. */
    private static final double LOG_DOUBLE_MAX_VALUE = StrictMath.log(Double.MAX_VALUE);
    /** Constant. */
    private static final double TWO_POW_N27 = NumbersUtils.twoPow(-27);
    /** Constant. */
    private static final double TWO_POW_N28 = NumbersUtils.twoPow(-28);

    /**
     * Bit = 0 where quadrant is encoded in remainder bits.
     */
    private static final long QUADRANT_BITS_0_MASK = 0xCFFFFFFFFFFFFFFFL;

    /**
     * Remainder bits where quadrant is encoded, 0 elsewhere.
     */
    private static final long QUADRANT_PLACE_BITS = 0x3000000000000000L;

    /**
     * fdlibm uses 2^19*PI/2 here.
     * With 2^18*PI/2 we would be more accurate, for example when normalizing
     * 822245.903631403, which is close to 2^19*PI/2, but we are still in
     * our accuracy tolerance with fdlibm's value (but not 2^20*PI/2) so we
     * stick to it, to help being faster than (Strict)Math for values in
     * [2^18*PI/2,2^19*PI/2].
     * 
     * For tests, can use a smaller value, for heavy remainder
     * not to only be used with huge values.
     */
    private static final double NORMALIZE_ANGLE_MAX_MEDIUM_DOUBLE_PIO2 = StrictMath.pow(2.0, 19.0) * (Math.PI / 2);

    /** Constants for PI/2. Only the 23 most significant bits of each mantissa are used. */
    private static final double PIO2_TAB0 = Double.longBitsToDouble(0x3FF921FB40000000L);
    /** Constants for PI/2. Only the 23 most significant bits of each mantissa are used. */
    private static final double PIO2_TAB1 = Double.longBitsToDouble(0x3E74442D00000000L);
    /** Constants for PI/2. Only the 23 most significant bits of each mantissa are used. */
    private static final double PIO2_TAB2 = Double.longBitsToDouble(0x3CF8469880000000L);
    /** Constants for PI/2. Only the 23 most significant bits of each mantissa are used. */
    private static final double PIO2_TAB3 = Double.longBitsToDouble(0x3B78CC5160000000L);
    /** Constants for PI/2. Only the 23 most significant bits of each mantissa are used. */
    private static final double PIO2_TAB4 = Double.longBitsToDouble(0x39F01B8380000000L);
    /** Constants for PI/2. Only the 23 most significant bits of each mantissa are used. */
    private static final double PIO2_TAB5 = Double.longBitsToDouble(0x387A252040000000L);

    /**
     * Table of constants for 1/(PI/2), 282 Hex digits (enough for normalizing doubles).
     * 1/(PI/2) approximation = sum of TWO_OVER_PI_TAB[i]*2^(-24*(i+1)).
     * 
     * double and not int, to avoid int-to-double cast during computations.
     */
    private static final double[] TWO_OVER_PI_TAB = {
        0xA2F983, 0x6E4E44, 0x1529FC, 0x2757D1, 0xF534DD, 0xC0DB62,
        0x95993C, 0x439041, 0xFE5163, 0xABDEBB, 0xC561B7, 0x246E3A, 0x424DD2, 0xe00649, 0x2EEA09, 0xD1921C,
        0xFE1DEB, 0x1CB129, 0xA73EE8, 0x8235F5, 0x2EBB44, 0x84E99C, 0x7026B4, 0x5F7E41, 0x3991d6, 0x398353,
        0x39F49C, 0x845F8B, 0xBDF928, 0x3B1FF8, 0x97FFDE, 0x05980F, 0xEF2F11, 0x8B5A0A, 0x6D1F6D, 0x367ECF,
        0x27CB09, 0xB74F46, 0x3F669E, 0x5FEA2D, 0x7527BA, 0xC7EBE5, 0xF17B3D, 0x0739F7, 0x8A5292, 0xEA6BFB,
        0x5FB11F, 0x8D5D08, 0x560330, 0x46FC7B, 0x6BABF0, 0xCFBC20, 0x9AF436, 0x1DA9E3, 0x91615E, 0xE61B08,
        0x659985, 0x5F14A0, 0x68408D, 0xFFD880, 0x4D7327, 0x310606, 0x1556CA, 0x73A8C9, 0x60E27B, 0xC08C6B };

    /**
     * Private constructor.
     */
    private JafamaFastMath() {
        // Private constructor
    }

    /**
     * Computes sine and cosine of an angle together.
     * Values are returned in the array provided [angle sine, angle cosine]. This array should be of size 2.
     * Warning: array size is not checked for performances reasons.
     * @param x angle in radians
     * @param sincos array of size 2. Array is filled is values: [angle sine, angle cosine]
     */
    public static void sinAndCos(final double x,
            final double[] sincos) {
        // Initialization
        double xi = x;

        // Using the same algorithm than sin(double) method,
        // and computing also cosine at the end.

        // Negative result
        boolean negateResult = false;
        if (xi < 0.0) {
            xi = -xi;
            negateResult = true;
        }
        if (xi > SIN_COS_MAX_VALUE_FOR_INT_MODULO) {
            // For values above 12000
            // Use standard sin and cos values
            final long remAndQuad = remainderPiO2(xi);
            xi = decodeRemainder(remAndQuad);
            final double sin;
            final int q = decodeQuadrant(remAndQuad);
            if (q == 0) {
                sin = MathLib.sin(xi);
                sincos[1] = MathLib.cos(xi);
            } else if (q == 1) {
                sin = MathLib.cos(xi);
                sincos[1] = -MathLib.sin(xi);
            } else if (q == 2) {
                sin = -MathLib.sin(xi);
                sincos[1] = -MathLib.cos(xi);
            } else {
                sin = -MathLib.cos(xi);
                sincos[1] = MathLib.sin(xi);
            }
            sincos[0] = (negateResult ? -sin : sin);
            return;
        }

        // Generic case
        int index = (int) (xi * SIN_COS_INDEXER + 0.5);
        final double delta = (xi - index * SIN_COS_DELTA_HI) - index * SIN_COS_DELTA_LO;
        index &= (SIN_COS_TABS_SIZE - 2); // index % (SIN_COS_TABS_SIZE-1)
        final double indexSin = MyTSinCos.SIN_TAB[index];
        final double indexCos = MyTSinCos.COS_TAB[index];
        // Could factor some multiplications (delta * factorials), but then is less accurate.
        sincos[1] = indexCos
                + delta
                * (-indexSin + delta
                        * (-indexCos * ONE_DIV_F2 + delta * (indexSin * ONE_DIV_F3 + delta * indexCos * ONE_DIV_F4)));
        final double result = indexSin
                + delta
                * (indexCos + delta
                        * (-indexSin * ONE_DIV_F2 + delta * (-indexCos * ONE_DIV_F3 + delta * indexSin * ONE_DIV_F4)));
        sincos[0] = negateResult ? -result : result;
    }

    /**
     * Computes hyperbolic sine and hyperbolic cosine together.
     * Values are returned in the array provided [hyperbolic sine, hyperbolic cosine]. This array should be of size 2.
     * Warning: array size is not checked for performances reasons.
     * @param x value
     * @param sinhcosh array of size 2. Array is filled is values: [hyperbolic sine, hyperbolic cosine]
     */
    public static void sinhAndCosh(final double x,
            final double[] sinhcosh) {

        // Initialization
        double xi = x;

        // Mixup of sinh and cosh treatments: if you modify them,
        // you might want to also modify this.
        final double h;
        if (xi < 0.0) {
            xi = -xi;
            h = -0.5;
        } else {
            h = 0.5;
        }
        final double hsine;
        // LOG_TWO_POW_27 = 18.714973875118524
        if (xi < LOG_TWO_POW_27) { // test from cosh
            // sinh
            if (xi < TWO_POW_N28) {
                hsine = (h < 0.0) ? -xi : xi;
            } else {
                final double t = MathLib.expm1(xi);
                hsine = h * (t + t / (t + 1.0));
            }
            // cosh
            if (xi < TWO_POW_N27) {
                sinhcosh[1] = 1;
            } else {
                final double t = MathLib.exp(xi);
                sinhcosh[1] = 0.5 * (t + 1 / t);
            }
        } else if (xi < 22.0) {
            // test from sinh
            // Here, value is in [18.714973875118524,22.0[.
            final double t = MathLib.expm1(xi);
            hsine = h * (t + t / (t + 1.0));
            sinhcosh[1] = 0.5 * (t + 1.0);
        } else {
            // x is too large
            if (xi < LOG_DOUBLE_MAX_VALUE) {
                hsine = h * MathLib.exp(xi);
            } else {
                final double t = MathLib.exp(xi * 0.5);
                hsine = (h * t) * t;
            }
            sinhcosh[1] = Math.abs(hsine);
        }
        sinhcosh[0] = hsine;
    }

    /**
     * Returns remainder.
     * @param angle Angle in radians.
     * @return Bits of double corresponding to remainder of (angle % (PI/2)),
     *         in [-PI/4,PI/4], with quadrant encoded in exponent bits.
     */
    private static long remainderPiO2(final double angle) {
        // Initialization
        double anglei = angle;

        // Negative result case
        boolean negateResult = false;
        if (anglei < 0.0) {
            anglei = -anglei;
            negateResult = true;
        }
        if (anglei <= NORMALIZE_ANGLE_MAX_MEDIUM_DOUBLE_PIO2) {
            // Case 1
            int n = (int) (anglei * PIO2_INV + 0.5);
            final double fn = n;
            anglei = (anglei - fn * PIO2_HI) - fn * PIO2_LO;
            // Ensuring range.
            // HI/LO can help a bit, even though we are always far from 0.
            if (anglei < -Math.PI / 4) {
                anglei = (anglei + PIO2_HI) + PIO2_LO;
                n--;
            } else if (anglei > Math.PI / 4) {
                anglei = (anglei - PIO2_HI) - PIO2_LO;
                n++;
            }
            if (negateResult) {
                anglei = -anglei;
            }
            return encodeRemainderAndQuadrant(anglei, n & 3);
        } else {
            // Case 2
            return heavyRemainderPiO2(anglei, negateResult);
        }
    }

    /**
     * Encode remainder in provided quadrant.
     * @param remainder Must have 1 for 2nd and 3rd exponent bits, which is the
     *        case for heavyRemPiO2 remainders (their absolute values are >=
     *        Double.longBitsToDouble(0x3000000000000000L)
     *        = 1.727233711018889E-77, and even if they were not, turning these
     *        bits from 0 to 1 on decoding would not change the absolute error
     *        much), and also works for +-Infinity or NaN encoding.
     * @param quadrant Must be in [0,3].
     * @return Bits holding remainder, and quadrant instead of
     *         reamainder's 2nd and 3rd exponent bits.
     */
    private static long encodeRemainderAndQuadrant(final double remainder,
            final int quadrant) {
        final long bits = Double.doubleToRawLongBits(remainder);
        return (bits & QUADRANT_BITS_0_MASK) | (((long) quadrant) << 60);
    }

    /**
     * Remainder using an accurate definition of PI.
     * Derived from a fdlibm treatment called __kernel_rem_pio2.
     * 
     * Not defining a non-strictfp version for FastMath, to avoid duplicating
     * its long and messy code, and because it's slow anyway, and should be
     * rarely used when speed matters.
     * 
     * @param angle Angle, in radians. Must not be NaN nor +-Infinity.
     * @param negateRem True if remainder must be negated before encoded into returned long.
     * @return Bits of double corresponding to remainder of (angle % (PI/2)),
     *         in [-PI/4,PI/4], with quadrant encoded in exponent bits.
     */
    // CHECKSTYLE: stop MethodLength check
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Jafama code kept as such
    private static strictfp long heavyRemainderPiO2(final double angle,
            final boolean negateRem) {
        // CHECKSTYLE: resume MethodLength check
        // CHECKSTYLE: resume CyclomaticComplexity check

        /*
         * fdlibm treatments unrolled, to avoid garbage and be OOME-free,
         * corresponding to:
         * 1) initial jk = 4 (precision = 3 = 64 bits (extended)),
         * which is more accurate than using precision = 2
         * (53 bits, double), even though we work with doubles
         * and use strictfp!
         * 2) max lengths of 8 for f[], 6 for q[], fq[] and iq[].
         * 3) at most one recomputation (one goto).
         * These limitations were experimentally found to
         * be sufficient for billions of random doubles
         * of random magnitudes.
         * For the rare cases that our unrolled treatments can't handle,
         * we fall back to a JDK-based implementation.
         */

        int n;
        int i;
        final int j;
        int ih;
        double fw;

        /*
         * Turning angle into 24-bits integer chunks.
         * Done outside __kernel_rem_pio2, but we factor it inside our method.
         */

        // Reworking exponent to have a value < 2^24.
        final long lx = Double.doubleToRawLongBits(angle);
        final long exp = ((lx >> 52) & 0x7FF) - (1023 + 23);
        double z = Double.longBitsToDouble(lx - (exp << 52));

        final double x0 = (int) z;
        z = (z - x0) * TWO_POW_24;
        final double x1 = (int) z;
        z = (z - x1) * TWO_POW_24;
        final double x2 = (int) z;

        final int e0 = (int) exp;
        // in [1,3]
        final int nx = (x2 == 0.0) ? ((x1 == 0.0) ? 1 : 2) : 3;

        final double f0;
        final double f1;
        final double f2;
        final double f3;
        final double f4;
        double f5;
        double f6;
        final double f7;
        double q0;
        double q1;
        double q2;
        double q3;
        double q4;
        double q5;
        int iq0;
        int iq1;
        int iq2;
        int iq3;
        int iq4;
        int iq5;

        final int jk = 4;

        final int jx = nx - 1;
        final int jv = Math.max(0, (e0 - 3) / 24);
        // In fdlibm, this is q0, but we prefer to use q0 for q[0].
        final int qZero = e0 - 24 * (jv + 1);

        j = jv - jx;
        if (jx == 0) {
            f6 = 0.0;
            f5 = 0.0;
            f4 = (j >= -4) ? TWO_OVER_PI_TAB[j + 4] : 0.0;
            f3 = (j >= -3) ? TWO_OVER_PI_TAB[j + 3] : 0.0;
            f2 = (j >= -2) ? TWO_OVER_PI_TAB[j + 2] : 0.0;
            f1 = (j >= -1) ? TWO_OVER_PI_TAB[j + 1] : 0.0;
            f0 = (j >= 0) ? TWO_OVER_PI_TAB[j] : 0.0;

            q0 = x0 * f0;
            q1 = x0 * f1;
            q2 = x0 * f2;
            q3 = x0 * f3;
            q4 = x0 * f4;
        } else if (jx == 1) {
            f6 = 0.0;
            f5 = (j >= -5) ? TWO_OVER_PI_TAB[j + 5] : 0.0;
            f4 = (j >= -4) ? TWO_OVER_PI_TAB[j + 4] : 0.0;
            f3 = (j >= -3) ? TWO_OVER_PI_TAB[j + 3] : 0.0;
            f2 = (j >= -2) ? TWO_OVER_PI_TAB[j + 2] : 0.0;
            f1 = (j >= -1) ? TWO_OVER_PI_TAB[j + 1] : 0.0;
            f0 = (j >= 0) ? TWO_OVER_PI_TAB[j] : 0.0;

            q0 = x0 * f1 + x1 * f0;
            q1 = x0 * f2 + x1 * f1;
            q2 = x0 * f3 + x1 * f2;
            q3 = x0 * f4 + x1 * f3;
            q4 = x0 * f5 + x1 * f4;
        } else { // jx == 2
            f6 = (j >= -6) ? TWO_OVER_PI_TAB[j + 6] : 0.0;
            f5 = (j >= -5) ? TWO_OVER_PI_TAB[j + 5] : 0.0;
            f4 = (j >= -4) ? TWO_OVER_PI_TAB[j + 4] : 0.0;
            f3 = (j >= -3) ? TWO_OVER_PI_TAB[j + 3] : 0.0;
            f2 = (j >= -2) ? TWO_OVER_PI_TAB[j + 2] : 0.0;
            f1 = (j >= -1) ? TWO_OVER_PI_TAB[j + 1] : 0.0;
            f0 = (j >= 0) ? TWO_OVER_PI_TAB[j] : 0.0;

            q0 = x0 * f2 + x1 * f1 + x2 * f0;
            q1 = x0 * f3 + x1 * f2 + x2 * f1;
            q2 = x0 * f4 + x1 * f3 + x2 * f2;
            q3 = x0 * f5 + x1 * f4 + x2 * f3;
            q4 = x0 * f6 + x1 * f5 + x2 * f4;
        }

        double twoPowQZero = twoPowNormal(qZero);

        int jz = jk;

        /*
         * Unrolling of first round.
         */

        z = q4;
        fw = (int) (TWO_POW_N24 * z);
        iq0 = (int) (z - TWO_POW_24 * fw);
        z = q3 + fw;
        fw = (int) (TWO_POW_N24 * z);
        iq1 = (int) (z - TWO_POW_24 * fw);
        z = q2 + fw;
        fw = (int) (TWO_POW_N24 * z);
        iq2 = (int) (z - TWO_POW_24 * fw);
        z = q1 + fw;
        fw = (int) (TWO_POW_N24 * z);
        iq3 = (int) (z - TWO_POW_24 * fw);
        z = q0 + fw;
        iq4 = 0;
        iq5 = 0;

        z = (z * twoPowQZero) % 8.0;
        n = (int) z;
        z -= n;

        ih = 0;
        if (qZero > 0) {
            // Parentheses against code formatter bug.
            i = (iq3 >> (24 - qZero));
            n += i;
            iq3 -= i << (24 - qZero);
            ih = iq3 >> (23 - qZero);
        } else if (qZero == 0) {
            ih = iq3 >> 23;
        } else if (z >= 0.5) {
            ih = 2;
        }

        if (ih > 0) {
            n += 1;
            // carry = 1 is common case,
            // so using it as initial value.
            int carry = 1;
            if (iq0 != 0) {
                iq0 = 0x1000000 - iq0;
                iq1 = 0xFFFFFF - iq1;
                iq2 = 0xFFFFFF - iq2;
                iq3 = 0xFFFFFF - iq3;
            } else if (iq1 != 0) {
                iq1 = 0x1000000 - iq1;
                iq2 = 0xFFFFFF - iq2;
                iq3 = 0xFFFFFF - iq3;
            } else if (iq2 != 0) {
                iq2 = 0x1000000 - iq2;
                iq3 = 0xFFFFFF - iq3;
            } else if (iq3 != 0) {
                iq3 = 0x1000000 - iq3;
            } else {
                carry = 0;
            }
            if (qZero > 0) {
                if (qZero == 1) {
                    iq3 &= 0x7FFFFF;
                } else if (qZero == 2) {
                    iq3 &= 0x3FFFFF;
                }
            }
            if (ih == 2) {
                z = 1.0 - z;
                if (carry != 0) {
                    z -= twoPowQZero;
                }
            }
        }

        if (z == 0.0) {
            if (iq3 == 0) {
                // With random values of random magnitude,
                // probability for this to happen seems lower than 1e-6.
                // jz would be more than just incremented by one,
                // which our unrolling doesn't support.
                return jdkRemainderPiO2(angle, negateRem);
            }
            if (jx == 0) {
                f5 = TWO_OVER_PI_TAB[jv + 5];
                q5 = x0 * f5;
            } else if (jx == 1) {
                f6 = TWO_OVER_PI_TAB[jv + 5];
                q5 = x0 * f6 + x1 * f5;
            } else { // jx == 2
                f7 = TWO_OVER_PI_TAB[jv + 5];
                q5 = x0 * f7 + x1 * f6 + x2 * f5;
            }

            jz++;

            /*
             * Unrolling of second round.
             */

            z = q5;
            fw = (int) (TWO_POW_N24 * z);
            iq0 = (int) (z - TWO_POW_24 * fw);
            z = q4 + fw;
            fw = (int) (TWO_POW_N24 * z);
            iq1 = (int) (z - TWO_POW_24 * fw);
            z = q3 + fw;
            fw = (int) (TWO_POW_N24 * z);
            iq2 = (int) (z - TWO_POW_24 * fw);
            z = q2 + fw;
            fw = (int) (TWO_POW_N24 * z);
            iq3 = (int) (z - TWO_POW_24 * fw);
            z = q1 + fw;
            fw = (int) (TWO_POW_N24 * z);
            iq4 = (int) (z - TWO_POW_24 * fw);
            z = q0 + fw;
            iq5 = 0;

            z = (z * twoPowQZero) % 8.0;
            n = (int) z;
            z -= n;

            ih = 0;
            if (qZero > 0) {
                // Parentheses against code formatter bug.
                i = (iq4 >> (24 - qZero));
                n += i;
                iq4 -= i << (24 - qZero);
                ih = iq4 >> (23 - qZero);
            } else if (qZero == 0) {
                ih = iq4 >> 23;
            } else if (z >= 0.5) {
                ih = 2;
            }

            if (ih > 0) {
                n += 1;
                // carry = 1 is common case,
                // so using it as initial value.
                int carry = 1;
                if (iq0 != 0) {
                    iq0 = 0x1000000 - iq0;
                    iq1 = 0xFFFFFF - iq1;
                    iq2 = 0xFFFFFF - iq2;
                    iq3 = 0xFFFFFF - iq3;
                    iq4 = 0xFFFFFF - iq4;
                } else if (iq1 != 0) {
                    iq1 = 0x1000000 - iq1;
                    iq2 = 0xFFFFFF - iq2;
                    iq3 = 0xFFFFFF - iq3;
                    iq4 = 0xFFFFFF - iq4;
                } else if (iq2 != 0) {
                    iq2 = 0x1000000 - iq2;
                    iq3 = 0xFFFFFF - iq3;
                    iq4 = 0xFFFFFF - iq4;
                } else if (iq3 != 0) {
                    iq3 = 0x1000000 - iq3;
                    iq4 = 0xFFFFFF - iq4;
                } else if (iq4 != 0) {
                    iq4 = 0x1000000 - iq4;
                } else {
                    carry = 0;
                }
                if (qZero > 0) {
                    if (qZero == 1) {
                        iq4 &= 0x7FFFFF;
                    } else if (qZero == 2) {
                        iq4 &= 0x3FFFFF;
                    }
                }
                if (ih == 2) {
                    z = 1.0 - z;
                    if (carry != 0) {
                        z -= twoPowQZero;
                    }
                }
            }

            if (z == 0.0) {
                if (iq4 == 0) {
                    // Case not encountered in tests, but still handling it.
                    // Would require a third loop unrolling.
                    return jdkRemainderPiO2(angle, negateRem);
                } else {
                    // z == 0.0, and iq4 != 0,
                    // so we remove 24 from qZero only once,
                    // but since we no longer use qZero,
                    // we just bother to multiply its 2-power
                    // by 2^-24.
                    jz--;
                    twoPowQZero *= TWO_POW_N24;
                }
            }
        }

        /*
         * After loop.
         */

        if (z != 0.0) {
            z /= twoPowQZero;
            if (z >= TWO_POW_24) {
                fw = (int) (TWO_POW_N24 * z);
                if (jz == jk) {
                    iq4 = (int) (z - TWO_POW_24 * fw);
                    jz++; // jz to 5
                    // Not using qZero anymore so not updating it.
                    twoPowQZero *= TWO_POW_24;
                    iq5 = (int) fw;
                } else { // jz == jk+1 == 5
                    // Case not encountered in tests, but still handling it.
                    // Would require use of iq6, with jz = 6.
                    return jdkRemainderPiO2(angle, negateRem);
                }
            } else {
                if (jz == jk) {
                    iq4 = (int) z;
                } else { // jz == jk+1 == 5
                    // Case not encountered in tests, but still handling it.
                    iq5 = (int) z;
                }
            }
        }

        fw = twoPowQZero;

        if (jz == 5) {
            q5 = fw * iq5;
            fw *= TWO_POW_N24;
        } else {
            q5 = 0.0;
        }
        q4 = fw * iq4;
        fw *= TWO_POW_N24;
        q3 = fw * iq3;
        fw *= TWO_POW_N24;
        q2 = fw * iq2;
        fw *= TWO_POW_N24;
        q1 = fw * iq1;
        fw *= TWO_POW_N24;
        q0 = fw * iq0;

        /*
         * We just use HI part of the result.
         */

        fw = PIO2_TAB0 * q5;
        fw += PIO2_TAB0 * q4 + PIO2_TAB1 * q5;
        fw += PIO2_TAB0 * q3 + PIO2_TAB1 * q4 + PIO2_TAB2 * q5;
        fw += PIO2_TAB0 * q2 + PIO2_TAB1 * q3 + PIO2_TAB2 * q4 + PIO2_TAB3 * q5;
        fw += PIO2_TAB0 * q1 + PIO2_TAB1 * q2 + PIO2_TAB2 * q3 + PIO2_TAB3 * q4 + PIO2_TAB4 * q5;
        fw += PIO2_TAB0 * q0 + PIO2_TAB1 * q1 + PIO2_TAB2 * q2 + PIO2_TAB3 * q3 + PIO2_TAB4 * q4 + PIO2_TAB5 * q5;

        if ((ih != 0) ^ negateRem) {
            fw = -fw;
        }

        return encodeRemainderAndQuadrant(fw, n & 3);
    }

    /**
     * Returns remainder.
     * @param angle Angle, in radians
     * @param negateRem true if remainder is negative
     * @return Bits of double corresponding to remainder of (angle % (PI/2)),
     *         in [-PI/4,PI/4], with quadrant encoded in exponent bits.
     */
    private static strictfp long jdkRemainderPiO2(final double angle,
            final boolean negateRem) {
        final double sin = StrictMath.sin(angle);
        final double cos = StrictMath.cos(angle);

        /*
         * Computing quadrant first, and then computing
         * atan2, to make sure its result ends up in [-PI/4,PI/4],
         * i.e. has maximum accuracy.
         */

        final int q;
        final double sinForAtan2;
        final double cosForAtan2;
        if (cos >= (SQRT_2 / 2)) {
            // [-PI/4,PI/4]
            q = 0;
            sinForAtan2 = sin;
            cosForAtan2 = cos;
        } else if (cos <= -(SQRT_2 / 2)) {
            // [3*PI/4,5*PI/4]
            q = 2;
            sinForAtan2 = -sin;
            cosForAtan2 = -cos;
        } else if (sin > 0.0) {
            // [PI/4,3*PI/4]
            q = 1;
            sinForAtan2 = -cos;
            cosForAtan2 = sin;
        } else {
            // [5*PI/4,7*PI/4]
            q = 3;
            sinForAtan2 = cos;
            cosForAtan2 = -sin;
        }

        final double fw = StrictMath.atan2(sinForAtan2, cosForAtan2);

        return encodeRemainderAndQuadrant(negateRem ? -fw : fw, q);
    }

    /**
     * Returns 2^power.
     * @param power Must be in normal values range.
     * @return 2^power.
     */
    private static double twoPowNormal(final int power) {
        return Double.longBitsToDouble(((long) (power + MAX_DOUBLE_EXPONENT)) << 52);
    }

    /**
     * Returns remainder.
     * @param bits number in long form
     * @return remainder
     */
    private static double decodeRemainder(final long bits) {
        return Double.longBitsToDouble((bits & QUADRANT_BITS_0_MASK) | QUADRANT_PLACE_BITS);
    }

    /**
     * Returns quadrant in 0, 1, 2, 3.
     * @param bits number in long form
     * @return quadrant in 0, 1, 2, 3
     */
    private static int decodeQuadrant(final long bits) {
        return ((int) (bits >> 60)) & 3;
    }

    /**
     * Private class for sin cos arrays.
     */
    private static final class MyTSinCos {
        /** Sin array. */
        private static final double[] SIN_TAB = new double[SIN_COS_TABS_SIZE];
        /** Cos array. */
        private static final double[] COS_TAB = new double[SIN_COS_TABS_SIZE];
        static {
            init();
        }

        /**
         * Initialize sin/cos arrays.
         */
        private static strictfp void init() {
            final int sinCosPiIndex = (SIN_COS_TABS_SIZE - 1) / 2;
            final int sinCosPiMul2Index = 2 * sinCosPiIndex;
            final int sinCosPiMul05Index = sinCosPiIndex / 2;
            final int sinCosPiMul15Index = 3 * sinCosPiIndex / 2;
            for (int i = 0; i < SIN_COS_TABS_SIZE; i++) {
                // angle: in [0,2*PI] (doesn't seem to help to have it in [-PI,PI]).
                final double angle = i * SIN_COS_DELTA_HI + i * SIN_COS_DELTA_LO;
                double sinAngle = StrictMath.sin(angle);
                double cosAngle = StrictMath.cos(angle);
                // For indexes corresponding to zero cosine or sine, we make sure
                // the value is zero and not an epsilon, since each value
                // corresponds to sin-or-cos(i*PI/n), where PI is a more accurate
                // definition of PI than Math.PI.
                // This allows for a much better accuracy for results close to zero.
                if (i == sinCosPiIndex) {
                    sinAngle = 0.0;
                } else if (i == sinCosPiMul2Index) {
                    sinAngle = 0.0;
                } else if (i == sinCosPiMul05Index) {
                    cosAngle = 0.0;
                } else if (i == sinCosPiMul15Index) {
                    cosAngle = 0.0;
                }
                SIN_TAB[i] = sinAngle;
                COS_TAB[i] = cosAngle;
            }
        }
    }

    // CHECKSTYLE: resume MagicNumber check
    // CHECKSTYLE: resume FinalParameters check
    // CHECKSTYLE: resume FinalLocalVariable check
}
