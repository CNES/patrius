/**
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
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.differentiation;

import java.util.Locale;

import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.exception.MathRuntimeException;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Implements Ridders method of polynomial extrapolation for differentiation
 * of real univariate functions.<br>
 * The algorithm implemented in this class comes from <i>Numerical Recipes in Fortran 77 :
 * the art of scientific computing.</i><br>
 * With respect to the <code>UnivariateDifferentiableFunction</code> implementation,
 * since this class uses a specific differentiation algorithm,
 * the returned <code>DerivativeStructure</code> instances are constant
 * ( they cannot provide derivatives other than the first order already computed when they are created).
 * 
 * @concurrency immutable
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id: RiddersDifferentiator.java 17603 2017-05-18 08:28:32Z bignon $
 * 
 * @since 1.2
 */
public final class RiddersDifferentiator implements UnivariateFunctionDifferentiator {

    /** Serial UID. */
    private static final long serialVersionUID = 1197148205609528273L;

    /** Estimated initial stepsize. */
    private final double h;

    /**
     * Constructor.
     * 
     * @param step
     *        estimated initial stepsize: it needs not be small, but rather should be an increment
     *        in x over which the function changes substantially
     */
    public RiddersDifferentiator(final double step) {
        this.h = step;
        if (this.h == 0) {
            // not a valid step: throws an exception
            throw new IllegalArgumentException(
                PatriusMessages.ZERO_NOT_ALLOWED.getLocalizedString(Locale.getDefault()));
        }
    }

    /**
     * Differentiates a <code>UnivariateFunction</code> on a single point using the Ridders method.
     * 
     * @param x
     *        value for the computation
     * @param f
     *        function to be derivated
     * @return the derivative value f'(x)
     */
    public double differentiate(final double x, final UnivariateFunction f) {
        final double con = 1.4;
        final double con2 = (con * con);
        final double big = 1.0e30;
        final int ntab = 10;
        final double safe = 2.0;

        int i = 0;
        int j = 0;

        double errt = 0.0;
        double fac = 0.0;
        double hh = 0.0;
        final double[][] a = new double[ntab + 1][ntab + 1];
        double ans = 0.0;

        hh = this.h;
        a[1][1] = (f.value(x + hh) - f.value(x - hh)) / (2.0 * hh);
        double err = big;

        // Successive columns in the Neville tableau will go to smaller
        // stepsizes and higher orders of extrapolation.
        for (i = 2; i <= ntab; i++) {
            hh /= con;
            // Try new, smaller stepsize.
            a[1][i] = (f.value(x + hh) - f.value(x - hh)) / (2.0 * hh);
            fac = con2;
            // Compute extrapolations of various orders, requiring no new function
            // evaluations.
            for (j = 2; j <= i; j++) {
                a[j][i] = (a[j - 1][i] * fac - a[j - 1][i - 1]) / (fac - 1.0);
                fac = con2 * fac;
                // The error strategy is to compare each new extrapolation to
                // one order lower, both at the present stepsize and the previous one.
                errt = MathLib.max(MathLib.abs(a[j][i] - a[j - 1][i]), MathLib.abs(a[j][i] - a[j - 1][i - 1]));
                // If error is decreased, save the improved answer.
                if (errt <= err) {
                    err = errt;
                    ans = a[j][i];
                }
            }
            // If higher order is worse by a significant factor SAFE, then quit early.
            if (MathLib.abs(a[i][i] - a[i - 1][i - 1]) >= safe * err) {
                return ans;
            }
        }
        return ans;
    }

    /** {@inheritDoc} */
    @Override
    public UnivariateDifferentiableFunction differentiate(final UnivariateFunction function) {
        // Returns this instance, encapsulating itself
        // as a UnivariateDifferentiableFunction.
        return new CapsRidders(this, function);
    }

    /**
     * Encapsulates the <code>RiddersDifferentiator</code> as a UnivariateDifferentiableFunction.
     * 
     * @since 1.3
     */
    private static final class CapsRidders implements UnivariateDifferentiableFunction {

        /** Serial UID. */
        private static final long serialVersionUID = -540784634817476964L;

        /** the function to be derivated. */
        private final UnivariateFunction function;
        /** a RiddersDifferentiator. */
        private final RiddersDifferentiator rdd;

        /**
         * Constructor.
         * 
         * @param rd
         *        a RiddersDifferentiator
         * @param f
         *        the function to be derivated
         */
        private CapsRidders(final RiddersDifferentiator rd, final UnivariateFunction f) {
            this.function = f;
            this.rdd = rd;
        }

        /** {@inheritDoc} */
        @Override
        public double value(final double x) {
            return this.function.value(x);
        }

        /** {@inheritDoc} */
        @Override
        public DerivativeStructure value(final DerivativeStructure t) {

            final int freePars = t.getFreeParameters();
            final int order = t.getOrder();
            if (freePars != 1 || order != 1) {
                // A limitation of the algorithm :
                // only one free parameter allowed
                // only first order allowed
                throw new MathRuntimeException(PatriusMessages.UNSUPPORTED_OPERATION);
            }

            final double retValue = this.function.value(t.getValue());
            final double retDeriv = this.rdd.differentiate(t.getValue(), this.function);
            final double[] retArray = { retValue, retDeriv };
            // Note the limitation : the returned DerivativeStructure is constant.
            return new DerivativeStructure(1, 1, retArray);
        }

    }
}
