/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 */
/*
 *
 * HISTORY
* VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::FA:829:25/01/2017:Protection of trigonometric methods
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.analytical.tle;

import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.propagation.MassProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

//CHECKSTYLE: stop MagicNumber check
//CHECKSTYLE: stop CommentRatio check
// Reason: model - Orekit code

/**
 * This class contains the methods that compute deep space perturbation terms.
 * <p>
 * The user should not bother in this class since it is handled internaly by the {@link TLEPropagator}.
 * </p>
 * <p>
 * This implementation is largely inspired from the paper and source code <a
 * href="http://www.celestrak.com/publications/AIAA/2006-6753/">Revisiting Spacetrack Report #3</a> and is fully
 * compliant with its results and tests cases.
 * </p>
 * 
 * @author Felix R. Hoots, Ronald L. Roehrich, December 1980 (original fortran)
 * @author David A. Vallado, Paul Crawford, Richard Hujsak, T.S. Kelso (C++ translation and improvements)
 * @author Fabien Maussion (java translation)
 */
class DeepSDP4 extends SDP4 {

    /** Serializable UID. */
    private static final long serialVersionUID = 7155645502511295218L;

    /** Flag for compliance with Dundee modifications. */
    private static final boolean IS_DUNDEE_COMPLIANT = true;

    /** Model constant. */
    private static final double ZNS = 1.19459E-5;
    /** Model constant. */
    private static final double ZES = 0.01675;
    /** Model constant. */
    private static final double ZNL = 1.5835218E-4;
    /** Model constant. */
    private static final double ZEL = 0.05490;
    /** Model constant. */
    private static final double THDT = 4.3752691E-3;
    /** Model constant. */
    private static final double C1SS = 2.9864797E-6;
    /** Model constant. */
    private static final double C1L = 4.7968065E-7;

    /** Model constant. */
    private static final double ROOT22 = 1.7891679E-6;
    /** Model constant. */
    private static final double ROOT32 = 3.7393792E-7;
    /** Model constant. */
    private static final double ROOT44 = 7.3636953E-9;
    /** Model constant. */
    private static final double ROOT52 = 1.1428639E-7;
    /** Model constant. */
    private static final double ROOT54 = 2.1765803E-9;

    /** Model constant. */
    private static final double Q22 = 1.7891679E-6;
    /** Model constant. */
    private static final double Q31 = 2.1460748E-6;
    /** Model constant. */
    private static final double Q33 = 2.2123015E-7;

    /** Model constant. */
    private static final double C_FASX2 = 0.99139134268488593;
    /** Model constant. */
    private static final double S_FASX2 = 0.13093206501640101;
    /** Model constant. */
    private static final double C_2FASX4 = 0.87051638752972937;
    /** Model constant. */
    private static final double S_2FASX4 = -0.49213943048915526;
    /** Model constant. */
    private static final double C_3FASX6 = 0.43258117585763334;
    /** Model constant. */
    private static final double S_3FASX6 = 0.90159499016666422;

    /** Model constant. */
    private static final double C_G22 = 0.87051638752972937;
    /** Model constant. */
    private static final double S_G22 = -0.49213943048915526;
    /** Model constant. */
    private static final double C_G32 = 0.57972190187001149;
    /** Model constant. */
    private static final double S_G32 = 0.81481440616389245;
    /** Model constant. */
    private static final double C_G44 = -0.22866241528815548;
    /** Model constant. */
    private static final double S_G44 = 0.97350577801807991;
    /** Model constant. */
    private static final double C_G52 = 0.49684831179884198;
    /** Model constant. */
    private static final double S_G52 = 0.86783740128127729;
    /** Model constant. */
    private static final double C_G54 = -0.29695209575316894;
    /** Model constant. */
    private static final double S_G54 = -0.95489237761529999;

    /** Integration step (seconds). */
    private static final double SECULAR_INTEGRATION_STEP = 720.0;

    /** Integration order. */
    private static final int SECULAR_INTEGRATION_ORDER = 2;

    /** Intermediate value. */
    private double thgr;
    /** Intermediate value. */
    private double xnq;
    /** Intermediate value. */
    private double omegaq;
    /** Intermediate value. */
    private double zmol;
    /** Intermediate value. */
    private double zmos;
    /** Intermediate value. */
    private double savtsn;

    /** Intermediate value. */
    private double ee2;
    /** Intermediate value. */
    private double e3;
    /** Intermediate value. */
    private double xi2;
    /** Intermediate value. */
    private double xi3;
    /** Intermediate value. */
    private double xl2;
    /** Intermediate value. */
    private double xl3;
    /** Intermediate value. */
    private double xl4;
    /** Intermediate value. */
    private double xgh2;
    /** Intermediate value. */
    private double xgh3;
    /** Intermediate value. */
    private double xgh4;
    /** Intermediate value. */
    private double xh2;
    /** Intermediate value. */
    private double xh3;

    /** Intermediate value. */
    private double d2201;
    /** Intermediate value. */
    private double d2211;
    /** Intermediate value. */
    private double d3210;
    /** Intermediate value. */
    private double d3222;
    /** Intermediate value. */
    private double d4410;
    /** Intermediate value. */
    private double d4422;
    /** Intermediate value. */
    private double d5220;
    /** Intermediate value. */
    private double d5232;
    /** Intermediate value. */
    private double d5421;
    /** Intermediate value. */
    private double d5433;
    /** Intermediate value. */
    private double xlamo;

    /** Intermediate value. */
    private double sse;
    /** Intermediate value. */
    private double ssi;
    /** Intermediate value. */
    private double ssl;
    /** Intermediate value. */
    private double ssh;
    /** Intermediate value. */
    private double ssg;
    /** Intermediate value. */
    private double se2;
    /** Intermediate value. */
    private double si2;
    /** Intermediate value. */
    private double sl2;
    /** Intermediate value. */
    private double sgh2;
    /** Intermediate value. */
    private double sh2;
    /** Intermediate value. */
    private double se3;
    /** Intermediate value. */
    private double si3;
    /** Intermediate value. */
    private double sl3;
    /** Intermediate value. */
    private double sgh3;
    /** Intermediate value. */
    private double sh3;
    /** Intermediate value. */
    private double sl4;
    /** Intermediate value. */
    private double sgh4;

    /** Intermediate value. */
    private double del1;
    /** Intermediate value. */
    private double del2;
    /** Intermediate value. */
    private double del3;
    /** Intermediate value. */
    private double xfact;
    /** Intermediate value. */
    private double xli;
    /** Intermediate value. */
    private double xni;
    /** Intermediate value. */
    private double atime;

    /** Intermediate value. */
    private double[] derivs;

    /** Flag for resonant orbits. */
    private boolean resonant;

    /** Flag for synchronous orbits. */
    private boolean synchronous;

    /**
     * Constructor for a unique initial TLE.
     * 
     * @param initialTLE
     *        the TLE to propagate.
     * @param attitudeProvider
     *        provider for attitude computation
     * @param mass
     *        spacecraft mass provider
     * @exception PatriusException
     *            if some specific error occurs
     */
    protected DeepSDP4(final TLE initialTLE, final AttitudeProvider attitudeProvider,
        final MassProvider mass) throws PatriusException {
        super(initialTLE, attitudeProvider, mass);
    }

    /**
     * Constructor for a unique initial TLE.
     * 
     * @param initialTLE
     *        the TLE to propagate.
     * @param attitudeProviderForces
     *        provider for attitude computation in forces computation case
     * @param attitudeProviderEvents
     *        provider for attitude computation in events computation case
     * @param mass
     *        spacecraft mass provider
     * @exception PatriusException
     *            if some specific error occurs
     */
    protected DeepSDP4(final TLE initialTLE, final AttitudeProvider attitudeProviderForces,
        final AttitudeProvider attitudeProviderEvents, final MassProvider mass) throws PatriusException {
        super(initialTLE, attitudeProviderForces, attitudeProviderEvents, mass);
    }

    /**
     * Computes luni - solar terms from initial coordinates and epoch.
     * 
     * @exception PatriusException
     *            when UTC time steps can't be read
     */
    // CHECKSTYLE: stop MethodLength check
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Orekit code kept as such
    @Override
    protected void luniSolarTermsComputation() throws PatriusException {
        // CHECKSTYLE: resume MethodLength check
        // CHECKSTYLE: resume CyclomaticComplexity check

        final double[] sincosg = MathLib.sinAndCos(this.tle.getPerigeeArgument());
        final double sing = sincosg[0];
        final double cosg = sincosg[1];

        final double[] sincosq = MathLib.sinAndCos(this.tle.getRaan());
        final double sinq = sincosq[0];
        final double cosq = sincosq[1];
        final double aqnv = 1.0 / this.a0dp;

        // Compute julian days since 1900
        final double daysSince1900 =
            (this.tle.getDate().durationFrom(AbsoluteDate.JULIAN_EPOCH) +
                this.tle.getDate().timeScalesOffset(TimeScalesFactory.getUTC(), TimeScalesFactory.getTT()))
                / Constants.JULIAN_DAY - 2415020;

        double cc = C1SS;
        double ze = ZES;
        double zn = ZNS;
        double zsinh = sinq;
        double zcosh = cosq;

        this.thgr = thetaG(this.tle.getDate());
        this.xnq = this.xn0dp;
        this.omegaq = this.tle.getPerigeeArgument();

        final double xnodce = 4.5236020 - 9.2422029e-4 * daysSince1900;
        final double[] sincostem = MathLib.sinAndCos(xnodce);
        final double stem = sincostem[0];
        final double ctem = sincostem[1];
        final double cMinusGam = 0.228027132 * daysSince1900 - 1.1151842;
        final double gam = 5.8351514 + 0.0019443680 * daysSince1900;

        final double zcosil = 0.91375164 - 0.03568096 * ctem;
        final double zsinil = MathLib.sqrt(MathLib.max(0.0, 1.0 - zcosil * zcosil));
        final double zsinhl = 0.089683511 * stem / zsinil;
        final double zcoshl = MathLib.sqrt(MathLib.max(0.0, 1.0 - zsinhl * zsinhl));
        this.zmol = MathUtils.normalizeAngle(cMinusGam, FastMath.PI);

        double zx = 0.39785416 * stem / zsinil;
        final double zy = zcoshl * ctem + 0.91744867 * zsinhl * stem;
        zx = MathLib.atan2(zx, zy) + gam - xnodce;
        final double[] sincoszx = MathLib.sinAndCos(zx);
        final double zcosgl = sincoszx[1];
        final double zsingl = sincoszx[0];
        this.zmos = MathUtils.normalizeAngle(6.2565837 + 0.017201977 * daysSince1900, FastMath.PI);

        // Do solar terms
        this.savtsn = 1e20;

        double zcosi = 0.91744867;
        double zsini = 0.39785416;
        double zsing = -0.98088458;
        double zcosg = 0.1945905;

        double se = 0;
        double sgh = 0;
        double sh = 0;
        double si = 0;
        double sl = 0;

        // There was previously some convoluted logic here, but it boils
        // down to this: we compute the solar terms, then the lunar terms.
        // On a second pass, we recompute the solar terms, taking advantage
        // of the improved data that resulted from computing lunar terms.
        for (int iteration = 0; iteration < 2; ++iteration) {
            final double a1 = zcosg * zcosh + zsing * zcosi * zsinh;
            final double a3 = -zsing * zcosh + zcosg * zcosi * zsinh;
            final double a7 = -zcosg * zsinh + zsing * zcosi * zcosh;
            final double a8 = zsing * zsini;
            final double a9 = zsing * zsinh + zcosg * zcosi * zcosh;
            final double a10 = zcosg * zsini;
            final double a2 = this.cosi0 * a7 + this.sini0 * a8;
            final double a4 = this.cosi0 * a9 + this.sini0 * a10;
            final double a5 = -this.sini0 * a7 + this.cosi0 * a8;
            final double a6 = -this.sini0 * a9 + this.cosi0 * a10;
            final double x1 = a1 * cosg + a2 * sing;
            final double x2 = a3 * cosg + a4 * sing;
            final double x3 = -a1 * sing + a2 * cosg;
            final double x4 = -a3 * sing + a4 * cosg;
            final double x5 = a5 * sing;
            final double x6 = a6 * sing;
            final double x7 = a5 * cosg;
            final double x8 = a6 * cosg;
            final double z31 = 12 * x1 * x1 - 3 * x3 * x3;
            final double z32 = 24 * x1 * x2 - 6 * x3 * x4;
            final double z33 = 12 * x2 * x2 - 3 * x4 * x4;
            final double z11 = -6 * a1 * a5 + this.e0sq * (-24 * x1 * x7 - 6 * x3 * x5);
            final double z12 = -6 * (a1 * a6 + a3 * a5) +
                this.e0sq * (-24 * (x2 * x7 + x1 * x8) - 6 * (x3 * x6 + x4 * x5));
            final double z13 = -6 * a3 * a6 + this.e0sq * (-24 * x2 * x8 - 6 * x4 * x6);
            final double z21 = 6 * a2 * a5 + this.e0sq * (24 * x1 * x5 - 6 * x3 * x7);
            final double z22 = 6 * (a4 * a5 + a2 * a6) +
                this.e0sq * (24 * (x2 * x5 + x1 * x6) - 6 * (x4 * x7 + x3 * x8));
            final double z23 = 6 * a4 * a6 + this.e0sq * (24 * x2 * x6 - 6 * x4 * x8);
            final double s3 = cc / this.xnq;
            final double s2 = -0.5 * s3 / this.beta0;
            final double s4 = s3 * this.beta0;
            final double s1 = -15 * this.tle.getE() * s4;
            final double s5 = x1 * x3 + x2 * x4;
            final double s6 = x2 * x3 + x1 * x4;
            final double s7 = x2 * x4 - x1 * x3;
            double z1 = 3 * (a1 * a1 + a2 * a2) + z31 * this.e0sq;
            double z2 = 6 * (a1 * a3 + a2 * a4) + z32 * this.e0sq;
            double z3 = 3 * (a3 * a3 + a4 * a4) + z33 * this.e0sq;

            z1 = z1 + z1 + this.beta02 * z31;
            z2 = z2 + z2 + this.beta02 * z32;
            z3 = z3 + z3 + this.beta02 * z33;
            se = s1 * zn * s5;
            si = s2 * zn * (z11 + z13);
            sl = -zn * s3 * (z1 + z3 - 14 - 6 * this.e0sq);
            sgh = s4 * zn * (z31 + z33 - 6);
            if (this.tle.getI() < (FastMath.PI / 60.0)) {
                // inclination smaller than 3 degrees
                sh = 0;
            } else {
                sh = -zn * s2 * (z21 + z23);
            }
            this.ee2 = 2 * s1 * s6;
            this.e3 = 2 * s1 * s7;
            this.xi2 = 2 * s2 * z12;
            this.xi3 = 2 * s2 * (z13 - z11);
            this.xl2 = -2 * s3 * z2;
            this.xl3 = -2 * s3 * (z3 - z1);
            this.xl4 = -2 * s3 * (-21 - 9 * this.e0sq) * ze;
            this.xgh2 = 2 * s4 * z32;
            this.xgh3 = 2 * s4 * (z33 - z31);
            this.xgh4 = -18 * s4 * ze;
            this.xh2 = -2 * s2 * z22;
            this.xh3 = -2 * s2 * (z23 - z21);

            if (iteration == 0) {
                // we compute lunar terms only on the first pass:
                this.sse = se;
                this.ssi = si;
                this.ssl = sl;
                this.ssh = (this.tle.getI() < (FastMath.PI / 60.0)) ? 0 : sh / this.sini0;
                this.ssg = sgh - this.cosi0 * this.ssh;
                this.se2 = this.ee2;
                this.si2 = this.xi2;
                this.sl2 = this.xl2;
                this.sgh2 = this.xgh2;
                this.sh2 = this.xh2;
                this.se3 = this.e3;
                this.si3 = this.xi3;
                this.sl3 = this.xl3;
                this.sgh3 = this.xgh3;
                this.sh3 = this.xh3;
                this.sl4 = this.xl4;
                this.sgh4 = this.xgh4;
                zcosg = zcosgl;
                zsing = zsingl;
                zcosi = zcosil;
                zsini = zsinil;
                zcosh = zcoshl * cosq + zsinhl * sinq;
                zsinh = sinq * zcoshl - cosq * zsinhl;
                zn = ZNL;
                cc = C1L;
                ze = ZEL;
            }
        } // end of solar - lunar - solar terms computation

        this.sse += se;
        this.ssi += si;
        this.ssl += sl;
        this.ssg += sgh - ((this.tle.getI() < (FastMath.PI / 60.0)) ? 0 : (this.cosi0 / this.sini0 * sh));
        this.ssh += (this.tle.getI() < (FastMath.PI / 60.0)) ? 0 : sh / this.sini0;

        // Start the resonant-synchronous tests and initialization

        double bfact = 0;

        // if mean motion is 1.893053 to 2.117652 revs/day, and eccentricity >= 0.5,
        // start of the 12-hour orbit, e > 0.5 section
        if ((this.xnq >= 0.00826) && (this.xnq <= 0.00924) && (this.tle.getE() >= 0.5)) {

            final double g201 = -0.306 - (this.tle.getE() - 0.64) * 0.440;
            final double eoc = this.tle.getE() * this.e0sq;
            final double sini2 = this.sini0 * this.sini0;
            final double f220 = 0.75 * (1 + 2 * this.cosi0 + this.theta2);
            final double f221 = 1.5 * sini2;
            final double f321 = 1.875 * this.sini0 * (1 - 2 * this.cosi0 - 3 * this.theta2);
            final double f322 = -1.875 * this.sini0 * (1 + 2 * this.cosi0 - 3 * this.theta2);
            final double f441 = 35 * sini2 * f220;
            final double f442 = 39.3750 * sini2 * sini2;
            final double f522 = 9.84375 * this.sini0 * (sini2 * (1 - 2 * this.cosi0 - 5 * this.theta2) +
                0.33333333 * (-2 + 4 * this.cosi0 + 6 * this.theta2));
            final double f523 = this.sini0 * (4.92187512 * sini2 * (-2 - 4 * this.cosi0 + 10 * this.theta2) +
                6.56250012 * (1 + 2 * this.cosi0 - 3 * this.theta2));
            final double f542 =
                29.53125 * this.sini0 * (2 - 8 * this.cosi0 + this.theta2 * (-12 + 8 * this.cosi0 + 10 * this.theta2));
            final double f543 =
                29.53125 * this.sini0 * (-2 - 8 * this.cosi0 + this.theta2 * (12 + 8 * this.cosi0 - 10 * this.theta2));
            final double g211;
            final double g310;
            final double g322;
            final double g410;
            final double g422;
            final double g520;

            // it is resonant...
            this.resonant = true;
            // but it's not synchronous
            this.synchronous = false;

            // Geopotential resonance initialization for 12 hour orbits :
            if (this.tle.getE() <= 0.65) {
                g211 = 3.616 - 13.247 * this.tle.getE() + 16.290 * this.e0sq;
                g310 = -19.302 + 117.390 * this.tle.getE() - 228.419 * this.e0sq + 156.591 * eoc;
                g322 = -18.9068 + 109.7927 * this.tle.getE() - 214.6334 * this.e0sq + 146.5816 * eoc;
                g410 = -41.122 + 242.694 * this.tle.getE() - 471.094 * this.e0sq + 313.953 * eoc;
                g422 = -146.407 + 841.880 * this.tle.getE() - 1629.014 * this.e0sq + 1083.435 * eoc;
                g520 = -532.114 + 3017.977 * this.tle.getE() - 5740.032 * this.e0sq + 3708.276 * eoc;
            } else {
                g211 = -72.099 + 331.819 * this.tle.getE() - 508.738 * this.e0sq + 266.724 * eoc;
                g310 = -346.844 + 1582.851 * this.tle.getE() - 2415.925 * this.e0sq + 1246.113 * eoc;
                g322 = -342.585 + 1554.908 * this.tle.getE() - 2366.899 * this.e0sq + 1215.972 * eoc;
                g410 = -1052.797 + 4758.686 * this.tle.getE() - 7193.992 * this.e0sq + 3651.957 * eoc;
                g422 = -3581.69 + 16178.11 * this.tle.getE() - 24462.77 * this.e0sq + 12422.52 * eoc;
                if (this.tle.getE() <= 0.715) {
                    g520 = 1464.74 - 4664.75 * this.tle.getE() + 3763.64 * this.e0sq;
                } else {
                    g520 = -5149.66 + 29936.92 * this.tle.getE() - 54087.36 * this.e0sq + 31324.56 * eoc;
                }
            }

            final double g533;
            final double g521;
            final double g532;
            if (this.tle.getE() < 0.7) {
                g533 = -919.2277 + 4988.61 * this.tle.getE() - 9064.77 * this.e0sq + 5542.21 * eoc;
                g521 = -822.71072 + 4568.6173 * this.tle.getE() - 8491.4146 * this.e0sq + 5337.524 * eoc;
                g532 = -853.666 + 4690.25 * this.tle.getE() - 8624.77 * this.e0sq + 5341.4 * eoc;
            } else {
                g533 = -37995.78 + 161616.52 * this.tle.getE() - 229838.2 * this.e0sq + 109377.94 * eoc;
                g521 = -51752.104 + 218913.95 * this.tle.getE() - 309468.16 * this.e0sq + 146349.42 * eoc;
                g532 = -40023.88 + 170470.89 * this.tle.getE() - 242699.48 * this.e0sq + 115605.82 * eoc;
            }

            double temp1 = 3 * this.xnq * this.xnq * aqnv * aqnv;
            double temp = temp1 * ROOT22;
            this.d2201 = temp * f220 * g201;
            this.d2211 = temp * f221 * g211;
            temp1 *= aqnv;
            temp = temp1 * ROOT32;
            this.d3210 = temp * f321 * g310;
            this.d3222 = temp * f322 * g322;
            temp1 *= aqnv;
            temp = 2 * temp1 * ROOT44;
            this.d4410 = temp * f441 * g410;
            this.d4422 = temp * f442 * g422;
            temp1 *= aqnv;
            temp = temp1 * ROOT52;
            this.d5220 = temp * f522 * g520;
            this.d5232 = temp * f523 * g532;
            temp = 2 * temp1 * ROOT54;
            this.d5421 = temp * f542 * g521;
            this.d5433 = temp * f543 * g533;
            this.xlamo = this.tle.getMeanAnomaly() + this.tle.getRaan() + this.tle.getRaan() - this.thgr - this.thgr;
            bfact = this.xmdot + this.xnodot + this.xnodot - THDT - THDT;
            bfact += this.ssl + this.ssh + this.ssh;
        } else if ((this.xnq < 0.0052359877) && (this.xnq > 0.0034906585)) {
            // if mean motion is .8 to 1.2 revs/day : (geosynch)

            final double cosioPlus1 = 1.0 + this.cosi0;
            final double g200 = 1 + this.e0sq * (-2.5 + 0.8125 * this.e0sq);
            final double g300 = 1 + this.e0sq * (-6 + 6.60937 * this.e0sq);
            final double f311 = 0.9375 * this.sini0 * this.sini0 * (1 + 3 * this.cosi0) - 0.75 * cosioPlus1;
            final double g310 = 1 + 2 * this.e0sq;
            final double f220 = 0.75 * cosioPlus1 * cosioPlus1;
            final double f330 = 2.5 * f220 * cosioPlus1;

            this.resonant = true;
            this.synchronous = true;

            // Synchronous resonance terms initialization
            this.del1 = 3 * this.xnq * this.xnq * aqnv * aqnv;
            this.del2 = 2 * this.del1 * f220 * g200 * Q22;
            this.del3 = 3 * this.del1 * f330 * g300 * Q33 * aqnv;
            this.del1 = this.del1 * f311 * g310 * Q31 * aqnv;
            this.xlamo = this.tle.getMeanAnomaly() + this.tle.getRaan() + this.tle.getPerigeeArgument() - this.thgr;
            bfact = this.xmdot + this.omgdot + this.xnodot - THDT;
            bfact = bfact + this.ssl + this.ssg + this.ssh;
        } else {
            // it's neither a high-e 12-hours orbit nor a geosynchronous:
            this.resonant = false;
            this.synchronous = false;
        }

        if (this.resonant) {
            this.xfact = bfact - this.xnq;

            // Initialize integrator
            this.xli = this.xlamo;
            this.xni = this.xnq;
            this.atime = 0;
        }
        this.derivs = new double[SECULAR_INTEGRATION_ORDER];
    }

    /**
     * Computes secular terms from current coordinates and epoch.
     * 
     * @param t
     *        offset from initial epoch (minutes)
     */
    @Override
    protected void deepSecularEffects(final double t) {

        this.xll += this.ssl * t;
        this.omgadf += this.ssg * t;
        this.xnode += this.ssh * t;
        this.em = this.tle.getE() + this.sse * t;
        this.xinc = this.tle.getI() + this.ssi * t;

        if (this.resonant) {
            // If we're closer to t = 0 than to the currently-stored data
            // from the previous call to this function, then we're
            // better off "restarting", going back to the initial data.
            // The Dundee code rigs things up to _always_ take 720-minute
            // steps from epoch to end time, except for the final step.
            // Easiest way to arrange similar behavior in this code is
            // just to always do a restart, if we're in Dundee-compliant
            // mode.
            // Epoch restart
            this.atime = 0;
            this.xni = this.xnq;
            this.xli = this.xlamo;
            boolean lastIntegrationStep = false;
            // if |step|>|step max| then do one step at step max
            while (!lastIntegrationStep) {
                double delt = t - this.atime;
                if (delt > SECULAR_INTEGRATION_STEP) {
                    delt = SECULAR_INTEGRATION_STEP;
                } else if (delt < -SECULAR_INTEGRATION_STEP) {
                    delt = -SECULAR_INTEGRATION_STEP;
                } else {
                    lastIntegrationStep = true;
                }

                this.computeSecularDerivs();

                final double xldot = this.xni + this.xfact;

                double xlpow = 1.;
                this.xli += delt * xldot;
                this.xni += delt * this.derivs[0];
                double deltFactor = delt;
                for (int j = 2; j <= SECULAR_INTEGRATION_ORDER; ++j) {
                    xlpow *= xldot;
                    this.derivs[j - 1] *= xlpow;
                    deltFactor *= delt / j;
                    this.xli += deltFactor * this.derivs[j - 2];
                    this.xni += deltFactor * this.derivs[j - 1];
                }
                this.atime += delt;
            }
            this.xn = this.xni;
            final double temp = -this.xnode + this.thgr + t * THDT;
            this.xll = this.xli + temp + (this.synchronous ? -this.omgadf : temp);
        }
    }

    /**
     * Computes periodic terms from current coordinates and epoch.
     * 
     * @param t
     *        offset from initial epoch (min)
     */
    @Override
    protected void deepPeriodicEffects(final double t) {

        // If the time didn't change by more than 30 minutes,
        // there's no good reason to recompute the perturbations;
        // they don't change enough over so short a time span.
        // However, the Dundee code _always_ recomputes, so if
        // we're attempting to replicate its results, we've gotta
        // recompute everything, too.
        this.savtsn = t;

        // Update solar perturbations for time T
        double zm = this.zmos + ZNS * t;
        double zf = zm + 2 * ZES * MathLib.sin(zm);
        double[] sincoszf = MathLib.sinAndCos(zf);
        double sinzf = sincoszf[0];
        final double coszf = sincoszf[1];
        double f2 = 0.5 * sinzf * sinzf - 0.25;
        double f3 = -0.5 * sinzf * coszf;
        final double ses = this.se2 * f2 + this.se3 * f3;
        final double sis = this.si2 * f2 + this.si3 * f3;
        final double sls = this.sl2 * f2 + this.sl3 * f3 + this.sl4 * sinzf;
        final double sghs = this.sgh2 * f2 + this.sgh3 * f3 + this.sgh4 * sinzf;
        final double shs = this.sh2 * f2 + this.sh3 * f3;

        // Update lunar perturbations for time T
        zm = this.zmol + ZNL * t;
        zf = zm + 2 * ZEL * MathLib.sin(zm);
        sincoszf = MathLib.sinAndCos(zf);
        sinzf = sincoszf[0];
        f2 = 0.5 * sinzf * sinzf - 0.25;
        f3 = -0.5 * sinzf * sincoszf[1];
        final double sel = this.ee2 * f2 + this.e3 * f3;
        final double sil = this.xi2 * f2 + this.xi3 * f3;
        final double sll = this.xl2 * f2 + this.xl3 * f3 + this.xl4 * sinzf;
        final double sghl = this.xgh2 * f2 + this.xgh3 * f3 + this.xgh4 * sinzf;
        final double sh1 = this.xh2 * f2 + this.xh3 * f3;

        // Sum the solar and lunar contributions
        final double pe = ses + sel;
        final double pinc = sis + sil;
        final double pl = sls + sll;
        final double pgh = sghs + sghl;
        final double ph = shs + sh1;

        this.xinc += pinc;

        final double[] sincosis = MathLib.sinAndCos(xinc);
        final double sinis = sincosis[0];
        final double cosis = sincosis[1];

        /* Add solar/lunar perturbation correction to eccentricity: */
        this.em += pe;
        this.xll += pl;
        this.omgadf += pgh;
        this.xinc = MathUtils.normalizeAngle(this.xinc, 0);

        if (MathLib.abs(this.xinc) >= 0.2) {
            // Apply periodics directly
            final double tempVal = ph / sinis;
            this.omgadf -= cosis * tempVal;
            this.xnode += tempVal;
        } else {
            // Apply periodics with Lyddane modification
            final double[] sincosok = MathLib.sinAndCos(xnode);
            final double sinok = sincosok[0];
            final double cosok = sincosok[1];
            final double alfdp = ph * cosok + (pinc * cosis + sinis) * sinok;
            final double betdp = -ph * sinok + (pinc * cosis + sinis) * cosok;
            final double deltaXnode = MathUtils.normalizeAngle(MathLib.atan2(alfdp, betdp) - this.xnode, 0);
            final double dls = -this.xnode * sinis * pinc;
            this.omgadf += dls - cosis * deltaXnode;
            this.xnode += deltaXnode;
        }
    }

    /** Computes internal secular derivs. */
    private void computeSecularDerivs() {

        final double[] sincosxli = MathLib.sinAndCos(xli);
        final double sinLi = sincosxli[0];
        final double cosLi = sincosxli[1];
        final double sin2li = 2. * sinLi * cosLi;
        final double cos2li = 2. * cosLi * cosLi - 1.;

        // Dot terms calculated :
        if (this.synchronous) {
            final double sin3li = sin2li * cosLi + cos2li * sinLi;
            final double cos3li = cos2li * cosLi - sin2li * sinLi;
            double term1a = this.del1 * (sinLi * C_FASX2 - cosLi * S_FASX2);
            double term2a = this.del2 * (sin2li * C_2FASX4 - cos2li * S_2FASX4);
            double term3a = this.del3 * (sin3li * C_3FASX6 - cos3li * S_3FASX6);
            double term1b = this.del1 * (cosLi * C_FASX2 + sinLi * S_FASX2);
            double term2b = 2.0 * this.del2 * (cos2li * C_2FASX4 + sin2li * S_2FASX4);
            double term3b = 3.0 * this.del3 * (cos3li * C_3FASX6 + sin3li * S_3FASX6);

            for (int j = 0; j < SECULAR_INTEGRATION_ORDER; j += 2) {
                this.derivs[j] = term1a + term2a + term3a;
                this.derivs[j + 1] = term1b + term2b + term3b;
                if ((this.i + 2) < SECULAR_INTEGRATION_ORDER) {
                    term1a = -term1a;
                    term2a *= -4.0;
                    term3a *= -9.0;
                    term1b = -term1b;
                    term2b *= -4.0;
                    term3b *= -9.0;
                }
            }
        } else {
            // orbit is a 12-hour resonant one
            final double xomi = this.omegaq + this.omgdot * this.atime;
            final double[] sincosxomi = MathLib.sinAndCos(xomi);
            final double sinOmi = sincosxomi[0];
            final double cosOmi = sincosxomi[1];
            final double sinLiMOmi = sinLi * cosOmi - sinOmi * cosLi;
            final double sinLiPOmi = sinLi * cosOmi + sinOmi * cosLi;
            final double cosLiMOmi = cosLi * cosOmi + sinOmi * sinLi;
            final double cosLiPOmi = cosLi * cosOmi - sinOmi * sinLi;
            final double sin2omi = 2. * sinOmi * cosOmi;
            final double cos2omi = 2. * cosOmi * cosOmi - 1.;
            final double sin2liMOmi = sin2li * cosOmi - sinOmi * cos2li;
            final double sin2liPOmi = sin2li * cosOmi + sinOmi * cos2li;
            final double cos2liMOmi = cos2li * cosOmi + sinOmi * sin2li;
            final double cos2liPOmi = cos2li * cosOmi - sinOmi * sin2li;
            final double sin2liP2omi = sin2li * cos2omi + sin2omi * cos2li;
            final double cos2liP2omi = cos2li * cos2omi - sin2omi * sin2li;
            final double sin2omiPLi = sinLi * cos2omi + sin2omi * cosLi;
            final double cos2omiPLi = cosLi * cos2omi - sin2omi * sinLi;
            double term1a = this.d2201 * (sin2omiPLi * C_G22 - cos2omiPLi * S_G22) +
                this.d2211 * (sinLi * C_G22 - cosLi * S_G22) +
                this.d3210 * (sinLiPOmi * C_G32 - cosLiPOmi * S_G32) +
                this.d3222 * (sinLiMOmi * C_G32 - cosLiMOmi * S_G32) +
                this.d5220 * (sinLiPOmi * C_G52 - cosLiPOmi * S_G52) +
                this.d5232 * (sinLiMOmi * C_G52 - cosLiMOmi * S_G52);
            double term2a = this.d4410 * (sin2liP2omi * C_G44 - cos2liP2omi * S_G44) +
                this.d4422 * (sin2li * C_G44 - cos2li * S_G44) +
                this.d5421 * (sin2liPOmi * C_G54 - cos2liPOmi * S_G54) +
                this.d5433 * (sin2liMOmi * C_G54 - cos2liMOmi * S_G54);
            double term1b = this.d2201 * (cos2omiPLi * C_G22 + sin2omiPLi * S_G22) +
                this.d2211 * (cosLi * C_G22 + sinLi * S_G22) +
                this.d3210 * (cosLiPOmi * C_G32 + sinLiPOmi * S_G32) +
                this.d3222 * (cosLiMOmi * C_G32 + sinLiMOmi * S_G32) +
                this.d5220 * (cosLiPOmi * C_G52 + sinLiPOmi * S_G52) +
                this.d5232 * (cosLiMOmi * C_G52 + sinLiMOmi * S_G52);
            double term2b = 2.0 * (this.d4410 * (cos2liP2omi * C_G44 + sin2liP2omi * S_G44) +
                this.d4422 * (cos2li * C_G44 + sin2li * S_G44) +
                this.d5421 * (cos2liPOmi * C_G54 + sin2liPOmi * S_G54) +
                this.d5433 * (cos2liMOmi * C_G54 + sin2liMOmi * S_G54));

            for (int j = 0; j < SECULAR_INTEGRATION_ORDER; j += 2) {
                this.derivs[j] = term1a + term2a;
                this.derivs[j + 1] = term1b + term2b;
                if ((j + 2) < SECULAR_INTEGRATION_ORDER) {
                    term1a = -term1a;
                    term2a *= -4.0;
                    term1b = -term1b;
                    term2b *= -4.0;
                }
            }
        }
    }

    // CHECKSTYLE: resume MagicNumber check
    // CHECKSTYLE: resume CommentRatio check
}
