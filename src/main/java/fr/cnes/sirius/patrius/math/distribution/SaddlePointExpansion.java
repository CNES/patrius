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
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.distribution;

import fr.cnes.sirius.patrius.math.special.Gamma;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;

//CHECKSTYLE: stop MagicNumber check
//Reason: model - Commons-Math code

/**
 * <p>
 * Utility class used by various distributions to accurately compute their respective probability mass functions. The
 * implementation for this class is based on the Catherine Loader's <a target="_blank"
 * href="http://www.herine.net/stat/software/dbinom.html">dbinom</a> routines.
 * </p>
 * <p>
 * This class is not intended to be called directly.
 * </p>
 * <p>
 * References:
 * <ol>
 * <li>Catherine Loader (2000). "Fast and Accurate Computation of Binomial Probabilities.". <a target="_blank"
 * href="http://www.herine.net/stat/papers/dbinom.pdf"> http://www.herine.net/stat/papers/dbinom.pdf</a></li>
 * </ol>
 * </p>
 * 
 * @since 2.1
 * @version $Id: SaddlePointExpansion.java 18108 2017-10-04 06:45:27Z bignon $
 */
final class SaddlePointExpansion {

    /** 1/2 * log(2 &#960;). */
    private static final double HALF_LOG_2_PI = 0.5 * MathLib.log(MathUtils.TWO_PI);

    /** exact Stirling expansion error for certain values. */
    private static final double[] EXACT_STIRLING_ERRORS = { 0.0,
        0.1534264097200273452913848,
        0.0810614667953272582196702,
        0.0548141210519176538961390,
        0.0413406959554092940938221,
        0.03316287351993628748511048,
        0.02767792568499833914878929,
        0.02374616365629749597132920,
        0.02079067210376509311152277,
        0.01848845053267318523077934,
        0.01664469118982119216319487,
        0.01513497322191737887351255,
        0.01387612882307074799874573,
        0.01281046524292022692424986,
        0.01189670994589177009505572,
        0.01110455975820691732662991,
        0.010411265261972096497478567,
        0.009799416126158803298389475,
        0.009255462182712732917728637,
        0.008768700134139385462952823,
        0.008330563433362871256469318,
        0.007934114564314020547248100,
        0.007573675487951840794972024,
        0.007244554301320383179543912,
        0.006942840107209529865664152,
        0.006665247032707682442354394,
        0.006408994188004207068439631,
        0.006171712263039457647532867,
        0.005951370112758847735624416,
        0.005746216513010115682023589,
        0.005554733551962801371038690
    };

    /**
     * Default constructor.
     */
    private SaddlePointExpansion() {
        super();
    }

    /**
     * Compute the error of Stirling's series at the given value.
     * <p>
     * References:
     * <ol>
     * <li>Eric W. Weisstein. "Stirling's Series." From MathWorld--A Wolfram Web Resource. <a target="_blank"
     * href="http://mathworld.wolfram.com/StirlingsSeries.html"> 
     * http://mathworld.wolfram.com/StirlingsSeries.html</a></li>
     * </ol>
     * </p>
     * 
     * @param z
     *        the value.
     * @return the Striling's series error.
     */
    public static double getStirlingError(final double z) {
        // Init return variable
        final double ret;
        if (z < 15.0) {
            // Compute error for value lesser than 15
            final double z2 = 2.0 * z;
            if (MathLib.floor(z2) == z2) {
                ret = EXACT_STIRLING_ERRORS[(int) z2];
            } else {
                ret = Gamma.logGamma(z + 1.0) - (z + 0.5) * MathLib.log(z) +
                    z - HALF_LOG_2_PI;
            }
        } else {
         // Compute error for value higher than 15
            final double z2 = z * z;
            ret = (0.083333333333333333333 -
                (0.00277777777777777777778 -
                    (0.00079365079365079365079365 -
                        (0.000595238095238095238095238 -
                            0.0008417508417508417508417508 /
                            z2) / z2) / z2) / z2) / z;
        }
        return ret;
    }

    /**
     * A part of the deviance portion of the saddle point approximation.
     * <p>
     * References:
     * <ol>
     * <li>Catherine Loader (2000). "Fast and Accurate Computation of Binomial Probabilities.". <a target="_blank"
     * href="http://www.herine.net/stat/papers/dbinom.pdf"> http://www.herine.net/stat/papers/dbinom.pdf</a></li>
     * </ol>
     * </p>
     * 
     * @param x
     *        the x value.
     * @param mu
     *        the average.
     * @return a part of the deviance.
     */
    public static double getDeviancePart(final double x, final double mu) {
        // Initialization
        final double ret;
        if (MathLib.abs(x - mu) < 0.1 * (x + mu)) {
            // Particular case
            final double d = x - mu;
            double v = d / (x + mu);
            double s1 = v * d;
            double s = Double.NaN;
            double ej = 2.0 * x * v;
            v = v * v;
            int j = 1;
            while (s1 != s) {
                s = s1;
                ej *= v;
                s1 = s + ej / ((j * 2) + 1);
                ++j;
            }
            ret = s1;
        } else {
            // General case
            ret = x * MathLib.log(x / mu) + mu - x;
        }
        // Return result
        return ret;
    }

    /**
     * Compute the logarithm of the PMF for a binomial distribution
     * using the saddle point expansion.
     * 
     * @param x
     *        the value at which the probability is evaluated.
     * @param n
     *        the number of trials.
     * @param p
     *        the probability of success.
     * @param q
     *        the probability of failure (1 - p).
     * @return log(p(x)).
     */
    public static double logBinomialProbability(final int x, final int n, final double p, final double q) {
        // Initialization
        double ret;
        if (x == 0) {
            // Case zero value
            if (p < 0.1) {
                ret = -getDeviancePart(n, n * q) - n * p;
            } else {
                ret = n * MathLib.log(q);
            }
        } else if (x == n) {
            // Case value = number of trials
            if (q < 0.1) {
                ret = -getDeviancePart(n, n * p) - n * q;
            } else {
                ret = n * MathLib.log(p);
            }
        } else {
            // General case
            ret = getStirlingError(n) - getStirlingError(x) -
                getStirlingError(n - x) - getDeviancePart(x, n * p) -
                getDeviancePart(n - x, n * q);
            final double f = (MathUtils.TWO_PI * x * (n - x)) / n;
            ret = -0.5 * MathLib.log(f) + ret;
        }
        // Return result
        return ret;
    }

    // CHECKSTYLE: stop MagicNumber check
}
