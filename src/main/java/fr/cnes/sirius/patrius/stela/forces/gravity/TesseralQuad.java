/**
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
 * 
 * 
 * @history created on 13/02/2013
 * 
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:345:30/10/2014:modified comments ratio
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::FA:829:25/01/2017:Protection of trigonometric methods
 * VERSION::FA:830:25/01/2017:Protection of 0 / 0 division
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.stela.forces.gravity;

import java.io.Serializable;

import fr.cnes.sirius.patrius.forces.gravity.potential.PotentialCoefficientsProvider;
import fr.cnes.sirius.patrius.math.util.ArithmeticUtils;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.stela.JavaMathAdapter;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Tesseral harmonics quad (n, m, p, q) and related data.
 * 
 * @concurrency not thread-safe
 * @concurrency.comment use of internal mutable attributes
 * 
 * @author Emmanuel Bignon, Rami Houdroge
 * @version $Id$
 * @since 1.3
 */
public final class TesseralQuad implements Serializable {

    /** Serial UID. */
    private static final long serialVersionUID = 9114597829730116553L;

    /** Internal constants. */
    private static final double TESSERAL_GTO_K1 = 0;
    /** Internal constants. */
    private static final double TESSERAL_GTO_K2 = -0.01;
    /** Internal constants. */
    private static final double TESSERAL_GTO_DELTA_E_MAX = 0.02;
    /** Internal constants. */
    private static final double TESSERAL_GTO_E_MIN = 1E-3;
    /** Internal constants. */
    private static final int TESSERAL_GTO_DMIN = 10;
    /** Internal constants. */
    private static final int TESSERAL_GTO_DMAX = 40;
    /** Internal constants. */
    private static final double TESSERAL_GTO_CRIT = 0.001;
    /** Internal constants. */
    private static final double TESSERAL_GTO_H = 1E-3;
    /** Internal constants. */
    private static final double DEFAULT_DELTA_ECCENTRICITY = 0.02;
    /** Internal constants. */
    private static final double ONE_HUNDRED = 100;

    /** n coefficient. */
    private final int n;

    /** m coefficient. */
    private final int m;

    /** p coefficient. */
    private final int p;

    /** q coefficient. */
    private final int q;

    /** f<sub>c</sub>. */
    private double fc;

    /** f<sub>s</sub>. */
    private double fs;

    /** Central eccentricity e<sub>c</sub>. */
    private double centralEccentricity = Double.POSITIVE_INFINITY;

    /** Delta eccentricity &Delta;e. */
    private double deltaEccentricity;

    /** Taylor coefficients (up to 2nd order) of eccentricity function G(e) around e<sub>c</sub>. */
    private double[] taylorCoeffs;

    /** Taylor coefficients (up to 2nd order) of eccentricity function derivative G'(e) around e<sub>c</sub>. */
    private double[] diffTaylorCoeffs;

    /** Class variable used to store number of terms used in eccentricity function sum. */
    private int nSum;

    /**
     * Constructor.
     * 
     * @param provider
     *        potential coefficients provider
     * @param coefN
     *        n coefficient
     * @param coefM
     *        m coefficient
     * @param coefP
     *        p coefficient
     * @param coefQ
     *        q coefficient
     * @param orbit
     *        the orbit
     * @throws PatriusException
     *         if couldn't get potential coefficients data
     */
    public TesseralQuad(final PotentialCoefficientsProvider provider, final int coefN, final int coefM,
        final int coefP,
        final int coefQ, final Orbit orbit) throws PatriusException {
        this.n = coefN;
        this.m = coefM;
        this.p = coefP;
        this.q = coefQ;

        // Compute Fc and Fs
        this.computeFcFs(provider);

        // Compute eccentricity interval
        this.updateEccentricityInterval(orbit);
    }

    /**
     * Compute f<sub>c</sub> and f<sub>s</sub>.
     * 
     * @param provider
     *        potential coefficients provider
     * @throws PatriusException
     *         if unable to retrieve potential coefficients
     */
    private void computeFcFs(final PotentialCoefficientsProvider provider) throws PatriusException {

        final double cnm = provider.getC(this.n, this.m, false)[this.n][this.m];
        final double snm = provider.getS(this.n, this.m, false)[this.n][this.m];

        if ((this.n - this.m) % 2 == 0) {
            // Even case
            this.fc = cnm;
            this.fs = snm;
        } else {
            // Uneven case
            this.fc = -snm;
            this.fs = cnm;
        }
    }

    /**
     * Update eccentricity interval [e<sub>c</sub> - &Delta;e; e<sub>c</sub> + &Delta;e]. G(e) and G'(e) functions using
     * taylor approximations are valid upon this interval.
     * 
     * @param orbit
     *        the orbit
     */
    public void updateEccentricityInterval(final Orbit orbit) {

        // Compute central eccentricity
        final double ex = orbit.getEquinoctialEx();
        final double ey = orbit.getEquinoctialEy();
        final double e = MathLib.sqrt(ex * ex + ey * ey);

        // Check if eccentricity interval needs update
        boolean needUpdate = false;
        if (e < this.centralEccentricity - this.deltaEccentricity
            || e > this.centralEccentricity + this.deltaEccentricity) {
            needUpdate = true;
        }

        // Update eccentricity interval if necessary
        if (needUpdate) {

            // Compute delta interval
            final double k1 = TESSERAL_GTO_K1;
            final double k2 = TESSERAL_GTO_K2;
            final double deMax = TESSERAL_GTO_DELTA_E_MAX;

            if (e > TESSERAL_GTO_E_MIN) {
                this.centralEccentricity = e;
                this.deltaEccentricity = MathLib.min(k1 + k2 * MathLib.log(e), deMax);
            } else {
                this.centralEccentricity = TESSERAL_GTO_E_MIN;
                this.deltaEccentricity = DEFAULT_DELTA_ECCENTRICITY;
            }

            // Update Taylor coefficients of G(e) and G'(e)
            this.updateTaylorCoefficients();
        }
    }

    /**
     * Update Taylor coefficients (up to the 2nd order) of eccentricity function G(e) and eccentricity function
     * derivative G'(e) around e<sub>c</sub>
     */
    private void updateTaylorCoefficients() {

        // Initialization
        final double h = TESSERAL_GTO_H;

        // Compute Taylor coefficients using approximations for derivatives
        final double g = this.gec(this.centralEccentricity, true);
        final double gmh = this.gec(this.centralEccentricity - h, false);
        final double gph = this.gec(this.centralEccentricity + h, false);

        final double gprime = (gph - gmh) / (2. * h);
        final double gsecond = (gph + gmh - 2. * g) / (h * h);

        final double ge2 = this.ge2(this.centralEccentricity);
        final double ge2mh = this.ge2(this.centralEccentricity - h);
        final double ge2ph = this.ge2(this.centralEccentricity + h);
        final double ge2prime = (ge2ph - ge2mh) / (2. * h);
        final double ge2second = (ge2ph + ge2mh - 2. * ge2) / (h * h);

        // Final Taylor coefficients
        this.taylorCoeffs = new double[] { g, gprime, gsecond / 2. };
        this.diffTaylorCoeffs = new double[] { ge2, ge2prime, ge2second / 2. };
    }

    /**
     * Compute G(e).
     * 
     * @param e
     *        eccentricity
     * @param firstTime
     *        true if function is accessed for the first time
     * @return G(e)
     */
    private double gec(final double e, final boolean firstTime) {

        // Initialization
        final double eta = MathLib.sqrt(MathLib.max(0.0, 1. - e * e));
        final double beta2 = e * e / ((1. + eta) * (1. + eta));
        final int absq = MathLib.abs(this.q);

        // G(e) computation
        return MathLib.pow(-1. / (1. + eta), absq) * MathLib.pow(1. + beta2, this.n)
                * this.computeSumAg(eta, beta2, firstTime);
    }

    /**
     * Compute dG(e<sup>2</sup>)/de<sup>2</sup>.
     * 
     * @param e
     *        eccentricity
     * @return G(e)
     */
    private double ge2(final double e) {

        // Initialization
        final double eta = MathLib.sqrt(MathLib.max(0.0, 1. - e * e));
        final double beta2 = e * e / ((1. + eta) * (1. + eta));

        final int absq = MathLib.abs(this.q);
        final double betaq = MathLib.pow(1. + beta2, this.n);
        final double betaqm1 = MathLib.pow(1. + beta2, this.n - 1);
        final double etaq = MathLib.pow(-1. / (1. + eta), absq);
        final double coeff = 1. / ((1. + eta) * (1. + eta)) * (1. + e * e / (eta * (1. + eta)));

        // Compute sum of A terms (from ge2 methods).
        final double sum1 = this.computeSumA(eta, beta2);
        // Compute sum of A terms (from ge2 methods).
        final double sum2 = this.computeSumA2(eta, beta2);
        // Compute sum of A derivative terms (from ge2 methods).
        final double sum3 = this.computeDiffSumA(eta, beta2);

        return MathLib.pow(-1, absq) * absq / (2. * eta * MathLib.pow(1. + eta, absq + 1))
                * betaq * sum1 + etaq * betaqm1
                * this.n * coeff * sum1 + etaq * betaq * coeff * sum2 + etaq * betaq * sum3;
    }

    /**
     * Compute sum of A terms (from g method).
     * 
     * @param eta
     *        sqrt(1 - e<sup>2</sup>)
     * @param beta2
     *        e<sup>2</sup>/(1 + &eta;)<sup>2</sup>
     * @param firstTime
     *        true if function is accessed for the first time
     * @return sum of A terms
     */
    private double computeSumAg(final double eta, final double beta2, final boolean firstTime) {

        // Initialization
        final int dmin = TESSERAL_GTO_DMIN;
        final int dmax = TESSERAL_GTO_DMAX;
        final double crit = TESSERAL_GTO_CRIT / ONE_HUNDRED;

        // Sum computation
        double sum = 0;

        if (firstTime) {

            int d = 0;
            double ratio = Double.POSITIVE_INFINITY;
            while (!(d > dmin && ratio < crit) && d < dmax) {

                // Compute next term and add it to the sum
                final double term = MathLib.pow(beta2, d) * this.computeA(d, eta);
                sum += term;

                // Compute ratio (if term = 0 and sum = 0 => keep ratio as such to go to next term)
                if (!(term == 0 && sum == 0)) {
                    ratio = MathLib.abs(MathLib.divide(term, sum));
                }

                d++;
            }
            this.nSum = d;

        } else {
            for (int i = 0; i < this.nSum; i++) {
                sum += MathLib.pow(beta2, i) * this.computeA(i, eta);
            }
        }

        return sum;
    }

    /**
     * Compute sum of A terms (from ge2 methods).
     * 
     * @param eta
     *        sqrt(1 - e<sup>2</sup>)
     * @param beta2
     *        e<sup>2</sup>/(1 + &eta;)<sup>2</sup>
     * @return sum of A terms
     */
    private double computeSumA(final double eta, final double beta2) {

        // Sum computation
        double sum = 0;

        for (int i = 0; i < this.nSum; i++) {
            sum += MathLib.pow(beta2, i) * this.computeA(i, eta);
        }

        return sum;
    }

    /**
     * Compute sum of A terms (from ge2 methods).
     * 
     * @param eta
     *        sqrt(1 - e<sup>2</sup>)
     * @param beta2
     *        e<sup>2</sup>/(1 + &eta;)<sup>2</sup>
     * @return sum of A terms
     */
    private double computeSumA2(final double eta, final double beta2) {

        // Sum computation
        double sum = 0;

        for (int i = 1; i < this.nSum; i++) {
            sum += MathLib.pow(beta2, i - 1) * this.computeA(i, eta) * i;
        }

        return sum;
    }

    /**
     * Compute A term (from g and ge2 method)
     * 
     * @param d
     *        an index
     * @param eta
     *        ???
     * @return A term
     */
    private double computeA(final int d, final double eta) {

        // Computation of k, hr and hi indexes
        final int k = this.n - 2 * this.p + this.q;

        // Indexes
        final int hr;
        final int hi;
        if (this.q > 0) {
            // q > 0
            hr = d + this.q;
            hi = d;
        } else {
            hr = d;
            hi = d - this.q;
        }

        // 1st sum
        double sum1 = 0;
        for (int r = 0; r <= hr; r++) {
            final double pk = JavaMathAdapter.binomialCoefficientGeneric(-2 * (this.n - this.p), hr - r)
                    * MathLib.pow(-1, r)
                    * MathLib.pow(k / 2., r) / ArithmeticUtils.factorialDouble(r);
            sum1 += pk * MathLib.pow(1 + eta, r);
        }

        // 2nd sum
        double sum2 = 0;
        for (int i = 0; i <= hi; i++) {
            final double qk = JavaMathAdapter.binomialCoefficientGeneric(-2 * this.p, hi - i) * MathLib.pow(k / 2., i)
                / ArithmeticUtils.factorialDouble(i);
            sum2 += qk * MathLib.pow(1 + eta, i);
        }

        // Return result
        return sum1 * sum2;
    }

    /**
     * Compute sum of A derivative terms (from ge2 methods).
     * 
     * @param eta
     *        sqrt(1 - e<sup>2</sup>)
     * @param beta2
     *        e<sup>2</sup>/(1 + &eta;)<sup>2</sup>
     * @return sum of A terms
     */
    private double computeDiffSumA(final double eta, final double beta2) {

        // Sum computation
        double sum = 0;

        for (int i = 0; i < this.nSum; i++) {
            sum += MathLib.pow(beta2, i) * this.computeDiffA(i, eta);
        }

        return sum;
    }

    /**
     * Compute derivative of A with respect to e<sup>2</sup> (from ge2 methods).
     * 
     * @param d
     *        an index
     * @param eta
     *        sqrt(1 - e<sup>2</sup>)
     * @return derivative of A with respect to e<sup>2</sup>
     */
    private double computeDiffA(final int d, final double eta) {

        // Computation of k, hr and hi indexes
        final int k = this.n - 2 * this.p + this.q;

        // Computation of hr and hi
        final int hr;
        final int hi;
        // hr and hi depends on q coeffient sign
        if (this.q > 0) {
            hr = d + this.q;
            hi = d;
        } else {
            hr = d;
            hi = d - this.q;
        }

        // Computation of all of 4th terms
        // 1st
        double t1 = 0;
        for (int r = 1; r <= hr; r++) {
            final double pk = JavaMathAdapter.binomialCoefficientGeneric(-2 * (this.n - this.p), hr - r)
                    * MathLib.pow(-1, r)
                    * MathLib.pow(k / 2., r) / ArithmeticUtils.factorialDouble(r);
            t1 += pk * MathLib.pow(1 + eta, r - 1) * (-r / (2 * eta));
        }

        // 2nd
        double t2 = 0;
        for (int i = 0; i <= hi; i++) {
            final double qk = JavaMathAdapter.binomialCoefficientGeneric(-2 * this.p, hi - i) * MathLib.pow(k / 2., i)
                / ArithmeticUtils.factorialDouble(i);
            t2 += qk * MathLib.pow(1 + eta, i);
        }

        // 3rd
        double t3 = 0;
        for (int r = 0; r <= hr; r++) {
            final double pk = JavaMathAdapter.binomialCoefficientGeneric(-2 * (this.n - this.p), hr - r)
                    * MathLib.pow(-1, r)
                    * MathLib.pow(k / 2., r) / ArithmeticUtils.factorialDouble(r);
            t3 += pk * MathLib.pow(1 + eta, r);
        }

        // 4th
        double t4 = 0;
        for (int i = 1; i <= hi; i++) {
            final double qk = JavaMathAdapter.binomialCoefficientGeneric(-2 * this.p, hi - i) * MathLib.pow(k / 2., i)
                / ArithmeticUtils.factorialDouble(i);
            t4 += qk * MathLib.pow(1 + eta, i - 1) * (-i / (2 * eta));
        }
        // return derivative of A with respect to e<sup>2</sup> (from ge2 methods).
        return t1 * t2 + t3 * t4;
    }

    // =============================== GETTERS and SETTERS =================================

    /**
     * Getter for n coefficient.
     * 
     * @return n coefficient
     */
    public int getN() {
        return this.n;
    }

    /**
     * Getter for m coefficient.
     * 
     * @return m coefficient
     */
    public int getM() {
        return this.m;
    }

    /**
     * Getter for p coefficient.
     * 
     * @return p coefficient
     */
    public int getP() {
        return this.p;
    }

    /**
     * Getter for q coefficient.
     * 
     * @return q coefficient
     */
    public int getQ() {
        return this.q;
    }

    /**
     * Getter for f<sub>c</sub>.
     * 
     * @return f<sub>c</sub>
     */
    public double getFc() {
        return this.fc;
    }

    /**
     * Getter for f<sub>s</sub>.
     * 
     * @return f<sub>s</sub>
     */
    public double getFs() {
        return this.fs;
    }

    /**
     * Getter for the central eccentricity e<sub>c</sub>.
     * 
     * @return the central eccentricity e<sub>c</sub>
     */
    public double getCentralEccentricity() {
        return this.centralEccentricity;
    }

    /**
     * Getter for the delta eccentricity &Delta;e.
     * 
     * @return the delta eccentricity &Delta;e
     */
    public double getDeltaEccentricity() {
        return this.deltaEccentricity;
    }

    /**
     * Getter for the Taylor coefficients (up to the 2nd order) of the eccentricity function G(e).
     * 
     * @return the Taylor coefficients (up to the 2nd order) of the eccentricity function G(e)
     */
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    public double[] getTaylorCoeffs() {
        return this.taylorCoeffs;
    }

    /**
     * Getter for the Taylor coefficients (up to the 2nd order) of the eccentricity function derivative G'(e).
     * 
     * @return the Taylor coefficients (up to the 2nd order) of the eccentricity function derivative G'G(e)
     */
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    public double[] getDiffTaylorCoeffs() {
        return this.diffTaylorCoeffs;
    }

    /**
     * Getter for quads as an array.
     * 
     * @return quad list as an array
     */
    public int[] getQuad() {
        return new int[] { this.n, this.m, this.p, this.q };
    }
}
