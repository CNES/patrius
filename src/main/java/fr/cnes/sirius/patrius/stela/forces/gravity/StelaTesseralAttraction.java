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
 * @history created 13/02/2013
 *
 * HISTORY
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11:DM:DM-3287:22/05/2023:[PATRIUS] Courtes periodes traînee atmospherique et prs
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-2922:15/11/2021:[PATRIUS] suppression de l'utilisation de la reflexion Java dans patrius 
 * VERSION:4.8:FA:FA-3009:15/11/2021:[PATRIUS] IllegalArgumentException SolarActivityToolbox
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:64:30/05/2013:changed constructor inputs order and renamed class
 * VERSION::FA:63:19/08/2013:changed parameters names
 * VERSION::FA:180:17/03/2014:removed a break instruction inside a while loop
 * VERSION::FA:345:30/10/2014:modified comments ratio
 * VERSION::DM:317:04/03/2015: STELA integration in CIRF with referential choice (ICRF, CIRF or MOD)
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.stela.forces.gravity;

import java.util.ArrayList;
import java.util.List;

import fr.cnes.sirius.patrius.forces.gravity.potential.PotentialCoefficientsProvider;
import fr.cnes.sirius.patrius.math.util.ArithmeticUtils;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.stela.JavaMathAdapter;
import fr.cnes.sirius.patrius.stela.bodies.EarthRotation;
import fr.cnes.sirius.patrius.stela.forces.AbstractStelaLagrangeContribution;
import fr.cnes.sirius.patrius.stela.orbits.OrbitNatureConverter;
import fr.cnes.sirius.patrius.stela.orbits.StelaEquinoctialOrbit;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusExceptionWrapper;

/**
 * This class represent the tesseral perturbations
 * 
 * @concurrency not thread-safe
 * @concurrency.comment not thread-safe due to use of mutable attributes
 * 
 * @author Rami Houdroge
 * 
 * @version $Id$
 * 
 * @since 1.3
 * 
 */
public final class StelaTesseralAttraction extends AbstractStelaLagrangeContribution {

     /** Serializable UID. */
    private static final long serialVersionUID = -3090059150241155780L;

    /** Integration step. */
    private static final double DEFAULT_INTEGRATION_STEP = 86400;

    /** Minimum number of integration step period before taking into account tesseral harmonics. */
    private static final int DEFAULT_NB_STEPS = 5;

    /** Default Tesseral GTO q<sub>max</sub>. */
    private static final int DEFAULT_TESSERAL_QMAX = 2;

    /** Default Tesseral GTO order */
    private static final int DEFAULT_TESSERAL_ORDER = 7;

    /** Tesseral q<sub>max</sub>. */
    private final int tesseralQMAX;

    /** Tesseral GTO order */
    private final int tesseralORDER;

    /** Integration step (s). */
    private final double integrationStep;

    /** Number of integration step min tesseral period. */
    private final int nbIntegrationStep;

    /** List of quads (n, m, p, q). */
    private final List<TesseralQuad> quadsList;

    /** Potential coefficients provider */
    private final PotentialCoefficientsProvider dataProvider;

    /**
     * Constructor.
     * 
     * @param provider
     *        potential coefficients provider
     */
    public StelaTesseralAttraction(final PotentialCoefficientsProvider provider) {
        this(provider, DEFAULT_TESSERAL_ORDER, DEFAULT_TESSERAL_QMAX, DEFAULT_INTEGRATION_STEP,
            DEFAULT_NB_STEPS);
    }

    /**
     * Constructor.
     * 
     * @param provider
     *        potential coefficients provider
     * @param tesseralOrder
     *        the max order in the Kaula developpement
     * @param qMax
     *        the maximum value of Q in the Kaula formulation
     * @param step
     *        integration step
     * @param nStep
     *        number of integration step
     */
    public StelaTesseralAttraction(final PotentialCoefficientsProvider provider,
        final int tesseralOrder, final int qMax,
        final double step, final int nStep) {
        super();

        this.integrationStep = step;
        this.nbIntegrationStep = nStep;

        this.quadsList = new ArrayList<>();
        this.dataProvider = provider;

        this.tesseralORDER = tesseralOrder;
        this.tesseralQMAX = qMax;
    }

    /** {@inheritDoc} */
    @Override
    public double[] computePerturbation(final StelaEquinoctialOrbit orbit) throws PatriusException {

        final double[] res = new double[6];

        // Quads list and eccentricity interval have been updated at the beginning of the integration step before

        // Algorithm for each quad
        for (int i = 0; i < this.quadsList.size(); i++) {
            final TesseralQuad quad = this.quadsList.get(i);

            // 1. Compute Fx(ix, iy) et Fy(ix, iy) and its derivatives
            final double[] f = computeF(orbit, quad);

            // 2. Compute eccentricity function
            final double[] g = computeEccentricityFunction(orbit, quad);

            // 3. Compute partial derivatives
            final double[] partialRes = this.computePartialDerivatives(orbit, quad, quad.getTaylorCoeffs(),
                quad.getDiffTaylorCoeffs(), f, g);

            for (int j = 0; j < res.length; j++) {
                res[j] += partialRes[j];
            }
        }
        this.dPot = res;
        return res;
    }

    /**
     * Compute final partial derivatives.
     * 
     * @param orbit
     *        position velocity in type 8
     * @param quad
     *        quad (n, m, p, q)
     * @param tay
     *        Taylor coefficients of eccentricity function
     * @param diffTay
     *        Taylor coefficients of eccentricity derivative function
     * @param f
     *        f function and its derivatives
     * @param g
     *        eccentricity function
     * @return final partial derivatives due to tesseral perturbation
     * @throws PatriusException
     *         if cannot load EOP data
     */
    private double[] computePartialDerivatives(final StelaEquinoctialOrbit orbit, final TesseralQuad quad,
                                               final double[] tay, final double[] diffTay, final double[] f,
                                               final double[] g) throws PatriusException {

        // Generic initialization
        final double mu = orbit.getMu();

        // Get the semi-major axis
        final double a = orbit.getA();
        // Get the first component of the eccentricity vector
        final double ex = orbit.getEquinoctialEx();
        // Get the second component of the eccentricity vector
        final double ey = orbit.getEquinoctialEy();
        // Compute eccentricity
        final double e = MathLib.sqrt(ex * ex + ey * ey);

        // Quad data initialization
        final int n = quad.getN();
        final int m = quad.getM();
        final int p = quad.getP();
        final int q = quad.getQ();

        final double fc = quad.getFc();
        final double fs = quad.getFs();

        // Inclination function initialization
        final double fx = f[0];
        final double fy = f[1];
        final double dFxdix = f[2];
        final double dFxdiy = f[3];
        final double dFydix = f[4];
        final double dFydiy = f[5];

        // Eccentricity function terms initialization
        final double eqcos = g[0];
        final double eqsin = g[1];
        final double diffEqcosEx = g[2];
        final double diffEqcosEy = g[3];
        final double diffEqsinEx = g[4];
        final double diffEqsinEy = g[5];

        // Computation
        final double deltaE = e - quad.getCentralEccentricity();
        final double deltaE2 = deltaE * deltaE;
        final double tayDev = tay[0] + tay[1] * deltaE + tay[2] * deltaE2;
        final double diffTayDev = diffTay[0] + diffTay[1] * deltaE + diffTay[2] * deltaE2;

        final double t16 = mu / (a * a);
        final double apn = MathLib.pow(MathLib.divide(this.dataProvider.getAe(), a), n);

        final double fxfy1 = fx * eqcos - fy * eqsin;
        final double fxfy2 = fx * eqsin + fy * eqcos;

        final double n2pq = n - 2. * p + q;
        final double sigma = n2pq * orbit.getLM() - m * (EarthRotation.getERA(orbit.getDate()) % (2 * FastMath.PI));

        final double[] sincos = MathLib.sinAndCos(sigma);
        final double sinSigma = sincos[0];
        final double cosSigma = sincos[1];
        final double fcCosSig = fc * cosSigma;
        final double fsSinSig = fs * sinSigma;
        final double fcSinSig = fc * sinSigma;
        final double fsCosSig = fs * cosSigma;

        final double fcfs1 = fcCosSig + fsSinSig;
        final double fcfs2 = fcSinSig - fsCosSig;

        final double t45 = fxfy1 * fcfs1 + fxfy2 * fcfs2;
        final double t53 = tayDev * mu;
        final double apnm1 = MathLib.divide(apn, a);
        final double t86 = apnm1 * t45;

        // partial derivatives wrp to each on the 6 parameters
        final double[] res = new double[6];
        res[0] = tayDev * (-t16 * apn * t45 - t16 * apn * n * t45);
        res[1] = t53 * apnm1
            * (fxfy1 * (-fcSinSig * n2pq + fsCosSig * n2pq) + fxfy2 * (fcCosSig * n2pq + fsSinSig * n2pq));
        res[2] = t53
            * apnm1
            * ((fx * diffEqcosEx - fy * diffEqsinEx) * fcfs1 + (fx * diffEqsinEx + fy * diffEqcosEx)
                * fcfs2) + 2. * diffTayDev * ex * mu * t86;
        res[3] = t53
            * apnm1
            * ((fx * diffEqcosEy - fy * diffEqsinEy) * fcfs1 + (fx * diffEqsinEy + fy * diffEqcosEy)
                * fcfs2) + 2. * diffTayDev * ey * mu * t86;
        res[4] = t53 * apnm1 * ((dFxdix * eqcos - dFydix * eqsin) * fcfs1 + (dFxdix * eqsin + dFydix * eqcos) * fcfs2);
        res[5] = t53 * apnm1 * ((dFxdiy * eqcos - dFydiy * eqsin) * fcfs1 + (dFxdiy * eqsin + dFydiy * eqcos) * fcfs2);
        // return final partial derivatives due to tesseral perturbation
        return res;
    }

    /**
     * Compute eccentricity function G(e) terms e<sup>abs(q)</sup>cos(q&omega;), e<sup>abs(q)</sup>sin(q&omega;) and its
     * derivatives with respect to e<sub>x</sub> and e<sub>y</sub>.
     * 
     * @param orbit
     *        position velocity in type 8
     * @param quad
     *        quad (n, m, p, q)
     * @return eccentricity function G(e) terms e<sup>abs(q)</sup>cos(q&omega;), e<sup>abs(q)</sup>sin(q&omega;) and its
     *         derivatives with respect to e<sub>x</sub> and e<sub>y</sub>
     */
    protected double[] computeEccentricityFunction(final StelaEquinoctialOrbit orbit, final TesseralQuad quad) {

        // Initialization
        final double ex = orbit.getEquinoctialEx();
        final double ey = orbit.getEquinoctialEy();

        double eqcos = 0;
        double eqsin = 0;
        double diffEqcosEx = 0;
        double diffEqcosEy = 0;
        double diffEqsinEx = 0;
        double diffEqsinEy = 0;

        final int q = quad.getQ();
        final int absq = MathLib.abs(quad.getQ());

        final int max0 = (int) MathLib.floor(absq / 2.);
        final int maxm1 = (int) MathLib.floor((absq - 1.) / 2.);
        final int maxm2 = (int) MathLib.floor((absq - 2.) / 2.);

        // Precomputation of binomial coefficients (abs(q) i) (in a tab for re-use)
        final double[] binom = new double[2 * max0 + 2];
        for (int i = 0; i < binom.length; i++) {
            binom[i] = JavaMathAdapter.binomialCoefficientGeneric(absq, i);
        }

        // eqcos
        for (int j = 0; j <= max0; j++) {
            eqcos += MathLib.pow(-1, j) * binom[2 * j] * MathLib.pow(ex, absq - 2 * j) * MathLib.pow(ey, 2 * j);
        }

        // eqsin
        for (int j = 0; j <= maxm1; j++) {
            eqsin += MathLib.signum(q) * MathLib.pow(-1, j) * binom[2 * j + 1] * MathLib.pow(ex, absq - 2 * j - 1)
                * MathLib.pow(ey, 2 * j + 1);
        }

        // diff_eqcos_ex
        for (int j = 0; j <= maxm1; j++) {
            diffEqcosEx += MathLib.pow(-1, j) * binom[2 * j] * MathLib.pow(ex, absq - 2 * j - 1)
                    * MathLib.pow(ey, 2 * j) * (absq - 2 * j);
        }

        // diff_eqcos_ey
        for (int j = 1; j <= max0; j++) {
            diffEqcosEy += MathLib.pow(-1, j) * binom[2 * j] * MathLib.pow(ex, absq - 2 * j)
                    * MathLib.pow(ey, 2 * j - 1) * 2 * j;
        }

        // diff_eqsin_ex
        for (int j = 0; j <= maxm2; j++) {
            diffEqsinEx += MathLib.signum(q) * MathLib.pow(-1, j) * binom[2 * j + 1]
                    * MathLib.pow(ex, absq - 2 * j - 2)
                * MathLib.pow(ey, 2 * j + 1) * (absq - 2 * j - 1);
        }

        // diff_eqsin_ey
        for (int j = 0; j <= maxm1; j++) {
            diffEqsinEy += MathLib.signum(q) * MathLib.pow(-1, j) * binom[2 * j + 1]
                    * MathLib.pow(ex, absq - 2 * j - 1)
                * MathLib.pow(ey, 2 * j) * (2 * j + 1);
        }

        // Return final result
        return new double[] { eqcos, eqsin, diffEqcosEx, diffEqcosEy, diffEqsinEx, diffEqsinEy };
    }

    /**
     * Compute inclination function F and its partial derivatives with respect to i<sub>x</sub> and i<sub>y</sub>.
     * 
     * @param orbit
     *        orbit
     * @param quad
     *        quad (n, m, p, q)
     * @return inclination function F and its partial derivatives
     */
    // CHECKSTYLE: stop CommentRatio check
    protected double[] computeF(final StelaEquinoctialOrbit orbit, final TesseralQuad quad) {
        // CHECKSTYLE: resume CommentRatio check

        // ============================== Initialization ===============================
        // get the first component of the inclination vector
        final double ix = orbit.getIx();
        // get the second component of the inclination vector
        final double iy = orbit.getIy();

        final int n = quad.getN();
        final int m = quad.getM();
        final int p = quad.getP();

        final int h = m - n + 2 * p;
        final int absh = MathLib.abs(h);
        final int alpha = MathLib.max(0, n - m - 2 * p);
        final int beta = MathLib.min(n - m, 2 * n - 2 * p);
        final int mPrime = (int) MathLib.floor(absh / 2.);
        final int mSecond = (int) MathLib.floor((absh + 3.) / 2.);

        final int c;
        if (h >= 0) {
            c = 3 * n - m - 2 * p;
        } else {
            c = n + m + 2 * p;
        }

        final int[] d = new int[beta + 1];
        for (int k = alpha; k <= beta; k++) {
            if (h >= 0) {
                d[k] = k;
            } else {
                d[k] = h + k;
            }
        }

        // Compute A
        final int delta;
        if ((n - m) % 2 == 0) {
            delta = (int) MathLib.floor((n - m) / 2.);
        } else {
            delta = (int) MathLib.floor((n - m + 1.) / 2.);
        }

        final double a = MathLib.pow(-1, delta) * ArithmeticUtils.factorialDouble(n + m)
            / (MathLib.pow(2, n) * ArithmeticUtils.factorialDouble(p) * ArithmeticUtils.factorialDouble(n - p));

        // Compute other terms
        final double ixiycdiv2 = MathLib.pow(1. - ix * ix - iy * iy, c / 2.);
        final double ixiycm1div2 = MathLib.pow(1. - ix * ix - iy * iy, c / 2. - 1.);
        final double ixiy = (ix * ix + iy * iy) / (1. - ix * ix - iy * iy);

        // Compute B coefficients (in a tab for re-use)
        final double[] b = new double[beta + 1];
        for (int k = alpha; k <= beta; k++) {
            b[k] = MathLib.pow(-1, k) * JavaMathAdapter.binomialCoefficientGeneric(2 * n - 2 * p, k)
                * JavaMathAdapter.binomialCoefficientGeneric(2 * p, n - m - k);
        }

        // Compute H coefficients (in a tab for re-use)
        final double[] hTab = new double[mPrime + 1];
        for (int j = 0; j <= mPrime; j++) {
            hTab[j] = MathLib.pow(-1, j) * JavaMathAdapter.binomialCoefficientGeneric(absh, 2 * j);
        }

        // Compute D coefficients (in a tab for re-use)
        final double[] dTab = new double[mSecond + 1];
        for (int j = 0; j <= mSecond; j++) {
            dTab[j] = MathLib.pow(-1, j) * MathLib.signum(h)
                    * JavaMathAdapter.binomialCoefficientGeneric(absh, 2 * j - 3);
        }

        // Compute ixiy ^ d and its derivatives with respect to ix and iy
        final double[] ixiypd = new double[beta + 1];
        final double[] ixiypddix = new double[beta + 1];
        final double[] ixiypddiy = new double[beta + 1];
        final double tx = 2 * ix / (1 - ix * ix - iy * iy);
        final double ty = 2 * iy / (1 - ix * ix - iy * iy);
        for (int k = alpha; k <= beta; k++) {
            ixiypd[k] = MathLib.pow(ixiy, d[k]);

            if (d[k] >= 1) {
                ixiypddix[k] = d[k] * MathLib.pow(ixiy, d[k] - 1) * tx * (1. + ixiy);
                ixiypddiy[k] = d[k] * MathLib.pow(ixiy, d[k] - 1) * ty * (1. + ixiy);
            }
        }

        // =============================== Final computation ===============================

        final double fx = computeFx(alpha, beta, mPrime, a, b, hTab, ix, iy, absh, ixiycdiv2, ixiypd);
        final double fy = computeFy(alpha, beta, mSecond, a, b, dTab, ix, iy, absh, ixiycdiv2, ixiypd);
        final double dFxdix = computeFxdix(c, alpha, beta, mPrime, a, b, hTab, ix, iy, absh, ixiycdiv2, ixiycm1div2,
            ixiypd, ixiypddix);
        final double dFxdiy = computeFxdiy(c, alpha, beta, mPrime, a, b, hTab, ix, iy, absh, ixiycdiv2, ixiycm1div2,
            ixiypd, ixiypddiy);
        final double dFydix = computeFydix(c, alpha, beta, mSecond, a, b, dTab, ix, iy, absh, ixiycdiv2, ixiycm1div2,
            ixiypd, ixiypddix);
        final double dFydiy = computeFydiy(c, alpha, beta, mSecond, a, b, dTab, ix, iy, absh, ixiycdiv2, ixiycm1div2,
            ixiypd, ixiypddiy);

        // Return result
        return new double[] { fx, fy, dFxdix, dFxdiy, dFydix, dFydiy };
    }

    /**
     * Inclination function F<sub>x</sub>(i<sub>x</sub>, i<sub>y</sub>) computation.
     * 
     * @param alpha
     *        alpha coefficient
     * @param beta
     *        beta coefficient
     * @param mPrime
     *        m' coefficient
     * @param a
     *        A coefficient
     * @param b
     *        B coefficient
     * @param h
     *        H coefficient
     * @param ix
     *        i<sub>x</sub> coefficient
     * @param iy
     *        i<sub>y</sub> coefficient
     * @param absh
     *        abs(h) coefficient
     * @param ixiycdiv2
     *        ixiycdiv2 coefficient
     * @param ixiypd
     *        ixiypd coefficient
     * @return F<sub>x</sub>(i<sub>x</sub>, i<sub>y</sub>)
     */
    private static double computeFx(final int alpha, final int beta, final int mPrime, final double a,
                                    final double[] b,
                                    final double[] h, final double ix, final double iy, final int absh,
                                    final double ixiycdiv2, final double[] ixiypd) {

        double sumX = 0;
        for (int k = alpha; k <= beta; k++) {
            for (int j = 0; j <= mPrime; j++) {
                sumX += b[k] * h[j] * ixiypd[k] * MathLib.pow(ix, absh - 2 * j) * MathLib.pow(iy, 2 * j);
            }
        }

        return a * ixiycdiv2 * sumX;
    }

    /**
     * Inclination function F<sub>y</sub>(i<sub>x</sub>, i<sub>y</sub>) computation.
     * 
     * @param alpha
     *        alpha
     * @param beta
     *        beta
     * @param mSecond
     *        m''
     * @param a
     *        A
     * @param b
     *        B
     * @param d
     *        D
     * @param ix
     *        i<sub>x</sub>
     * @param iy
     *        i<sub>y</sub>
     * @param absh
     *        abs(h)
     * @param ixiycdiv2
     *        ixiycdiv2
     * @param ixiypd
     *        ixiypd
     * @return F<sub>y</sub>(i<sub>x</sub>, i<sub>y</sub>)
     */
    private static double computeFy(final int alpha, final int beta, final int mSecond, final double a,
                                    final double[] b, final double[] d, final double ix, final double iy,
                                    final int absh, final double ixiycdiv2, final double[] ixiypd) {

        double sumY = 0;
        for (int k = alpha; k <= beta; k++) {
            for (int j = 2; j <= mSecond; j++) {
                sumY += b[k] * d[j] * ixiypd[k] * MathLib.pow(ix, absh - 2 * j + 3) * MathLib.pow(iy, 2 * j - 3);
            }
        }

        return -a * ixiycdiv2 * sumY;
    }

    /**
     * Inclination function derivative dF<sub>x</sub>(i<sub>x</sub>, i<sub>y</sub>)/di<sub>x</sub> computation.
     * 
     * @param c
     *        c
     * @param alpha
     *        alpha
     * @param beta
     *        beta
     * @param mPrime
     *        m'
     * @param a
     *        A
     * @param b
     *        B
     * @param h
     *        H
     * @param ix
     *        i<sub>x</sub>
     * @param iy
     *        i<sub>y</sub>
     * @param absh
     *        abs(h)
     * @param ixiycdiv2
     *        ixiycdiv2
     * @param ixiycm1div2
     *        ixiycm1div2
     * @param ixiypd
     *        ixiypd
     * @param ixiypddix
     *        ixiypddix
     * @return dF<sub>x</sub>(i<sub>x</sub>, i<sub>y</sub>)/di<sub>x</sub>
     */
    private static double computeFxdix(final int c, final int alpha, final int beta, final int mPrime, final double a,
                                final double[] b, final double[] h, final double ix, final double iy, final int absh,
                                final double ixiycdiv2, final double ixiycm1div2, final double[] ixiypd,
                                final double[] ixiypddix) {
        // compute t1, t2, t3
        // Temporary variables
        final double t1 = a * c / 2. * ixiycm1div2 * (-2. * ix);
        final double t2 = a * ixiycdiv2;
        final double t3 = a * ixiycdiv2;

        // initialize sum to zero
        double sum1 = 0;
        double sum2 = 0;
        double sum3 = 0;
        // Computation
        for (int k = alpha; k <= beta; k++) {
            for (int j = 0; j <= mPrime; j++) {
                sum1 += b[k] * h[j] * ixiypd[k] * MathLib.pow(ix, absh - 2 * j) * MathLib.pow(iy, 2 * j);
                sum2 += b[k] * h[j] * ixiypddix[k] * MathLib.pow(ix, absh - 2 * j) * MathLib.pow(iy, 2 * j);

                if (absh - 2 * j >= 1) {
                    sum3 += b[k] * h[j] * ixiypd[k] * (absh - 2 * j) * MathLib.pow(ix, absh - 2 * j - 1)
                            * MathLib.pow(iy, 2 * j);
                }
            }
        }
        // return Fxdix
        return t1 * sum1 + t2 * sum2 + t3 * sum3;
    }

    /**
     * Inclination function derivative dF<sub>x</sub>(i<sub>x</sub>, i<sub>y</sub>)/di<sub>y</sub> computation.
     * 
     * @param c
     *        c coef for computation
     * @param alpha
     *        alpha coef for computation
     * @param beta
     *        beta coef for computation
     * @param mPrime
     *        m' coef for computation
     * @param a
     *        A coef for computation
     * @param b
     *        B coef for computation
     * @param h
     *        H coef for computation
     * @param ix
     *        i<sub>x</sub> coef for computation
     * @param iy
     *        i<sub>y</sub> coef for computation
     * @param absh
     *        abs(h) coef for computation
     * @param ixiycdiv2
     *        ixiycdiv2 coef for computation
     * @param ixiycm1div2
     *        ixiycm1div2 coef for computation
     * @param ixiypd
     *        ixiypd coef for computation
     * @param ixiypddiy
     *        ixiypddiy coef for computation
     * @return dF<sub>x</sub>(i<sub>x</sub>, i<sub>y</sub>)/di<sub>y</sub>
     */
    private static double computeFxdiy(final int c, final int alpha, final int beta, final int mPrime, final double a,
                                final double[] b, final double[] h, final double ix, final double iy, final int absh,
                                final double ixiycdiv2, final double ixiycm1div2, final double[] ixiypd,
                                final double[] ixiypddiy) {
        // compute t1, t2, t3
        // Temporary variables
        final double t1 = a * c / 2. * ixiycm1div2 * (-2. * iy);
        final double t2 = a * ixiycdiv2;
        final double t3 = a * ixiycdiv2;

        // initialize sum to zero
        double sum1 = 0;
        double sum2 = 0;
        double sum3 = 0;
        // Computation
        for (int k = alpha; k <= beta; k++) {
            for (int j = 0; j <= mPrime; j++) {
                sum1 += b[k] * h[j] * ixiypd[k] * MathLib.pow(ix, absh - 2 * j) * MathLib.pow(iy, 2 * j);
                sum2 += b[k] * h[j] * ixiypddiy[k] * MathLib.pow(ix, absh - 2 * j) * MathLib.pow(iy, 2 * j);

                if (2 * j >= 1) {
                    sum3 += b[k] * h[j] * ixiypd[k] * (2 * j) * MathLib.pow(ix, absh - 2 * j)
                            * MathLib.pow(iy, 2 * j - 1);
                }
            }
        }
        // return Fxdiy
        return t1 * sum1 + t2 * sum2 + t3 * sum3;
    }

    /**
     * Inclination function derivative dF<sub>y</sub>(i<sub>x</sub>, i<sub>y</sub>)/di<sub>x</sub> computation.
     * 
     * @param c
     *        c coeff
     * @param alpha
     *        alpha coeff
     * @param beta
     *        beta coeff
     * @param mSecond
     *        m' coeff
     * @param a
     *        A coeff
     * @param b
     *        B coeff
     * @param d
     *        D coeff
     * @param ix
     *        i<sub>x</sub> coeff
     * @param iy
     *        i<sub>y</sub> coeff
     * @param absh
     *        abs(h) coeff
     * @param ixiycdiv2
     *        ixiycdiv2 coeff
     * @param ixiycm1div2
     *        ixiycm1div2 coeff
     * @param ixiypd
     *        ixiypd coeff
     * @param ixiypddix
     *        ixiypddix coeff
     * @return dF<sub>y</sub>(i<sub>x</sub>, i<sub>y</sub>)/di<sub>x</sub>
     */
    private static double computeFydix(final int c, final int alpha, final int beta, final int mSecond, final double a,
                                final double[] b, final double[] d, final double ix, final double iy, final int absh,
                                final double ixiycdiv2, final double ixiycm1div2, final double[] ixiypd,
                                final double[] ixiypddix) {
        // compute t1, t2, t3
        // Temporary variables
        final double t1 = -a * c / 2. * ixiycm1div2 * (-2. * ix);
        final double t2 = -a * ixiycdiv2;
        final double t3 = -a * ixiycdiv2;

        // initialize sum to zero
        double sum1 = 0;
        double sum2 = 0;
        double sum3 = 0;
        // Computation
        for (int k = alpha; k <= beta; k++) {
            for (int j = 2; j <= mSecond; j++) {
                sum1 += b[k] * d[j] * ixiypd[k] * MathLib.pow(ix, absh - 2 * j + 3) * MathLib.pow(iy, 2 * j - 3);
                sum2 += b[k] * d[j] * ixiypddix[k] * MathLib.pow(ix, absh - 2 * j + 3) * MathLib.pow(iy, 2 * j - 3);

                if (absh - 2 * j + 3 >= 1) {
                    sum3 += b[k] * d[j] * ixiypd[k] * (absh - 2 * j + 3.) * MathLib.pow(ix, absh - 2 * j + 2)
                        * MathLib.pow(iy, 2 * j - 3);
                }
            }
        }
        // return Fydix
        return t1 * sum1 + t2 * sum2 + t3 * sum3;
    }

    /**
     * Inclination function derivative dF<sub>y</sub>(i<sub>x</sub>, i<sub>y</sub>)/di<sub>y</sub> computation.
     * 
     * @param c
     *        c
     * @param alpha
     *        alpha
     * @param beta
     *        beta
     * @param mSecond
     *        m'
     * @param a
     *        A
     * @param b
     *        B
     * @param d
     *        D
     * @param ix
     *        i<sub>x</sub>
     * @param iy
     *        i<sub>y</sub>
     * @param absh
     *        abs(h)
     * @param ixiycdiv2
     *        ixiycdiv2
     * @param ixiycm1div2
     *        ixiycm1div2
     * @param ixiypd
     *        ixiypd
     * @param ixiypddiy
     *        ixiypddiy
     * @return dF<sub>y</sub>(i<sub>x</sub>, i<sub>y</sub>)/di<sub>y</sub>
     */
    private static double computeFydiy(final int c, final int alpha, final int beta, final int mSecond, final double a,
                                final double[] b, final double[] d, final double ix, final double iy, final int absh,
                                final double ixiycdiv2, final double ixiycm1div2, final double[] ixiypd,
                                final double[] ixiypddiy) {
        // compute t1, t2, t3
        // Temporary variables
        final double t1 = -a * c / 2. * ixiycm1div2 * (-2. * iy);
        final double t2 = -a * ixiycdiv2;
        final double t3 = -a * ixiycdiv2;

        // initialize sum to zero
        double sum1 = 0;
        double sum2 = 0;
        double sum3 = 0;
        // Computation
        for (int k = alpha; k <= beta; k++) {
            for (int j = 2; j <= mSecond; j++) {
                sum1 += b[k] * d[j] * ixiypd[k] * MathLib.pow(ix, absh - 2 * j + 3) * MathLib.pow(iy, 2 * j - 3);
                sum2 += b[k] * d[j] * ixiypddiy[k] * MathLib.pow(ix, absh - 2 * j + 3) * MathLib.pow(iy, 2 * j - 3);

                if (2 * j - 3 >= 1) {
                    sum3 += b[k] * d[j] * ixiypd[k] * (2 * j - 3.) * MathLib.pow(ix, absh - 2 * j + 3)
                            * MathLib.pow(iy, 2 * j - 4);
                }
            }
        }
        // return Fydiy
        return t1 * sum1 + t2 * sum2 + t3 * sum3;
    }

    /**
     * Compute quads (n, m, p, q).
     * 
     * @param orbit
     *        orbit
     */
    public void updateQuads(final Orbit orbit) {

        try {
            // Get the semi-major axis
            final double a = orbit.getA();
            // Get the central acceleration constant
            final double mu = orbit.getMu();
            final double mdot = MathLib.sqrt(mu / (a * a * a));
            final double thetadot = EarthRotation.getERADerivative(orbit.getDate());
            final double puser = this.integrationStep * this.nbIntegrationStep / Constants.JULIAN_DAY;
            // Tesseral GTO order
            final int nmax = this.tesseralORDER;
            // Tesseral qmax
            final int qmax = this.tesseralQMAX;

            for (int n = 2; n <= nmax; n++) {
                for (int m = 1; m <= n; m++) {
                    for (int p = 0; p <= n; p++) {
                        for (int q = -qmax; q <= qmax; q++) {

                            final double sigmadot = (n - 2 * p + q) * mdot - m * thetadot;
                            final double pt = MathLib.divide(thetadot, MathLib.abs(sigmadot));
                            // reduce cyclomatic complexity
                            this.computeCurrentQuad(orbit, puser, n, m, p, q, pt);
                        }
                    }
                }
            }
        } catch (final PatriusException e) {
            // exception raised by computeCurrentQuad private method
            throw new PatriusExceptionWrapper(e);
        }
    }

    /**
     * Private method to reduce cyclomatic complexity.
     * 
     * @param orbit
     *        orbit
     * @param puser
     *        current puser
     * @param n
     *        current n
     * @param m
     *        current m
     * @param p
     *        current p
     * @param q
     *        current n
     * @param pP
     *        current pP
     * @throws PatriusException
     *         error when creating the TesseralQuad
     */
    protected void computeCurrentQuad(final Orbit orbit,
            final double puser,
            final int n,
            final int m,
            final int p,
            final int q,
            final double pP) throws PatriusException {
        // Try to find current quad in quad list
        int index = -1;
        boolean goOn = true;
        for (int i = 0; i < this.quadsList.size() && goOn; i++) {
            final TesseralQuad tempQuad = this.quadsList.get(i);
            if (tempQuad.getN() == n && tempQuad.getM() == m && tempQuad.getP() == p
                && tempQuad.getQ() == q) {
                index = i;
                goOn = false;
            }
        }

        // Check if condition on (n, m, p, q) is satisfied
        if (pP > puser) {
            if (index == -1) {
                // 2nd case: condition is satisfied and quad not in the list: it is added
                this.quadsList.add(new TesseralQuad(this.dataProvider, n, m, p, q, orbit));
            } else {
                // 1st case: condition is satisfied and quad already in the list: eccentricity is
                // updated
                this.quadsList.get(index).updateEccentricityInterval(orbit);
            }
        } else {
            if (index != -1) {
                // 3rd case: condition is not satisfied and quad already in the list: it is deleted
                this.quadsList.remove(index);
            }
            // 4th case: condition is not satisfied and quad not in the list: nothing to do
        }
    }

    /** {@inheritDoc} */
    @Override
    public double[] computeShortPeriods(final StelaEquinoctialOrbit orbit,
            final OrbitNatureConverter converter) throws PatriusException {
        // not implemented yet
        return new double[6];
    }

    /** {@inheritDoc} */
    @Override
    public double[][] computePartialDerivatives(final StelaEquinoctialOrbit orbit) throws PatriusException {
        // not implemented yet
        return new double[6][6];
    }

    /**
     * Getter for the quad list.
     * 
     * @return the quadsList
     */
    public List<TesseralQuad> getQuadsList() {
        return this.quadsList;
    }
}
