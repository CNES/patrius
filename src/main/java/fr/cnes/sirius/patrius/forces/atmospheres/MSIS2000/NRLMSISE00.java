/*
 * $Id: NRLMSISE00.java 17582 2017-05-10 12:58:16Z bignon $
 * =============================================================
 * Copyright (c) CNES 2010
 * This software is part of STELA, a CNES tool for long term
 * orbit propagation. This source file is licensed as described
 * in the file LICENCE which is part of this distribution
 * =============================================================
 */
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
 * @history Created 24/04/2012
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-2922:15/11/2021:[PATRIUS] suppression de l'utilisation de la reflexion Java dans patrius 
 * VERSION:4.8:FA:FA-3009:15/11/2021:[PATRIUS] IllegalArgumentException SolarActivityToolbox
 * VERSION:4.5:FA:FA-2364:27/05/2020:Problèmes rencontres dans le modèle MSIS00
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:130:08/10/2013:MSIS2000 model update
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.atmospheres.MSIS2000;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.util.MathLib;

// CHECKSTYLE: stop MagicNumber

/**
 * NRLMSISE-00 atmospheric model. <br>
 * Methods of this class are adapted from the C source code of the NRLMSISE-00 model
 * developed by Mike Picone, Alan Hedin, and Doug Drob, and implemented by Dominik Brodowski.
 * 
 * The NRLMSISE-00 model was developed by Mike Picone, Alan Hedin, and
 * Doug Drob. They also wrote a NRLMSISE-00 distribution package in
 * FORTRAN which is available at
 * http://uap-www.nrl.navy.mil/models_web/msis/msis_home.htm
 * 
 * Dominik Brodowski implemented and maintains this C version. You can
 * reach him at mail@brodo.de. See the file "DOCUMENTATION" for details,
 * and check http://www.brodo.de/english/pub/nrlmsise/index.html for
 * updated releases of this package.
 * 
 * @concurrency thread-hostile
 * 
 * @author Vincent Ruch, Rami Houdroge
 * 
 * @version $Id: NRLMSISE00.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 1.2
 * 
 */
@SuppressWarnings("PMD.AvoidProtectedMethodInFinalClassNotExtending")
public final class NRLMSISE00 implements Serializable {

     /** Serializable UID. */
    private static final long serialVersionUID = -2490685921726654567L;

    // POWER7
    /** POWER7 : pt. */
    private static double[] pt = NRLMSISE00Data.PT;
    /** POWER7 : pd. */
    private static double[][] pd = NRLMSISE00Data.PD;
    /** POWER7 : ps. */
    private static double[] ps = NRLMSISE00Data.PS;
    /** POWER7 : pdl. */
    private static double[] pdl = NRLMSISE00Data.PDL;
    /** POWER7 : ptl. */
    private static double[][] ptl = NRLMSISE00Data.PTL;
    /** POWER7 : pma. */
    private static double[][] pma = NRLMSISE00Data.PMA;

    // LOWER7
    /** LOWER7 : ptm. */
    private static double[] ptm = NRLMSISE00Data.PTM;
    /** LOWER7 : pdm. */
    private static double[] pdm = NRLMSISE00Data.PDM;
    /** LOWER7 : pavgm. */
    private static double[] pavgm = NRLMSISE00Data.PAVGM;

    /** Perfect gaz constant. */
    private static final double RGAS = 831.4;

    /** 50.0 */
    private static final double C_50 = 50.0;

    // ------------------------- SHARED VARIABLES ------------------------

    // DMIX
    /** DMIX : dm04. */
    private double dm04;
    /** DMIX : dm16. */
    private double dm16;
    /** DMIX : dm28. */
    private double dm28;
    /** DMIX : dm32. */
    private double dm32;
    /** DMIX : dm40. */
    private double dm40;
    /** DMIX : dm01. */
    private double dm01;
    /** DMIX : dm14. */
    private double dm14;

    // MESO7
    /** MESO7 : meso_tn1. */
    private final double[] mesoTn1 = new double[5];
    /** MESO7 : meso_tn2. */
    @SuppressWarnings("PMD.SingularField")
    private final double[] mesoTn2 = new double[4];
    /** MESO7 : meso_tn3. */
    @SuppressWarnings("PMD.SingularField")
    private final double[] mesoTn3 = new double[6];
    /** MESO7 : meso_tgn1. */
    private final double[] mesoTgn1 = new double[2];
    /** MESO7 : meso_tgn2. */
    @SuppressWarnings("PMD.SingularField")
    private final double[] mesoTgn2 = new double[2];
    /** MESO7 : meso_tgn3. */
    private final double[] mesoTgn3 = new double[2];

    // LPOLY
    /** LPOLY : plg. */
    private final double[] plg = new double[36];
    /** LPOLY : ctloc. */
    private double ctloc;
    /** LPOLY : stloc. */
    private double stloc;
    /** LPOLY : c2tloc. */
    private double c2tloc;
    /** LPOLY : s2tloc. */
    private double s2tloc;
    /** LPOLY : s3tloc. */
    private double s3tloc;
    /** LPOLY : c3tloc. */
    private double c3tloc;
    /** LPOLY : apdf. */
    private double apdf;
    /** LPOLY : apt. */
    private final double[] apt = new double[4];

    // PARMB
    /** PARMB : gsurf. */
    private double gsurf = 0;
    /** PARMB : re. */
    private double re = 0;

    /** local cache for globe7 LPOLY : ctloc. */
    private double oldTloc;

    /** g0cacheKey */
    private double g0cacheKey;
    /** g0Value */
    private double g0Value;
    /** oldSg0 */
    private double oldSg0;
    /** p24 */
    private double p24;
    /** p25 */
    private double p25;
    /** oldEx */
    private double oldEx;

    /**
     * calculate Legendre polynomial.
     * 
     * @param input
     *        input
     */
    private void calculateLegendrePolynomial(final Input input) {

        // Calculate Legende Polynomials

        // Constants to speed-up computations
        // radian conversion
        final double rad = MathLib.toRadians(input.getgLat());
        final double[] sincos = MathLib.sinAndCos(rad);
        final double c = sincos[0];
        final double s = sincos[1];
        // c square
        final double c2 = c * c;
        // s square
        final double s2 = s * s;
        final double c4 = c2 * c2;

        this.plg[1] = c;
        this.plg[2] = 0.5 * (3.0 * c2 - 1.0);
        this.plg[3] = 0.5 * (5.0 * c * c2 - 3.0 * c);
        this.plg[4] = (35.0 * c4 - 30.0 * c2 + 3.0) * 0.125;
        this.plg[5] = (63.0 * c2 * c2 * c - 70.0 * c2 * c + 15.0 * c) * 0.125;
        this.plg[6] = (11.0 * c * this.plg[5] - 5.0 * this.plg[4]) / 6.0;
        // plg[7] = (13.0 * c * plg[6] - 6.0 * plg[5]) / 7.0;
        this.plg[10] = s;
        this.plg[11] = 3.0 * c * s;
        this.plg[12] = 1.5 * (5.0 * c2 - 1.0) * s;
        this.plg[13] = 2.5 * (7.0 * c2 * c - 3.0 * c) * s;
        this.plg[14] = 1.875 * (21.0 * c4 - 14.0 * c2 + 1.0) * s;
        this.plg[15] = (11.0 * c * this.plg[14] - 6.0 * this.plg[13]) * 0.2;
        // plg[16] = (13.0 * c * plg[15] - 7.0 * plg[14]) / 6.0;
        // plg[17] = (15.0 * c * plg[16] - 8.0 * plg[15]) / 7.0;
        this.plg[20] = 3.0 * s2;
        this.plg[21] = 15.0 * s2 * c;
        this.plg[22] = 7.5 * (7.0 * c2 - 1.0) * s2;
        this.plg[23] = 3.0 * c * this.plg[22] - 2.0 * this.plg[21];
        this.plg[24] = (11.0 * c * this.plg[23] - 7.0 * this.plg[22]) * 0.25;
        this.plg[25] = (13.0 * c * this.plg[24] - 8.0 * this.plg[23]) * 0.2;
        this.plg[30] = 15.0 * s2 * s;
        this.plg[31] = 105.0 * s2 * s * c;
        this.plg[32] = (9.0 * c * this.plg[31] - 7. * this.plg[30]) * 0.5;
        this.plg[33] = (11.0 * c * this.plg[32] - 8. * this.plg[31]) / 3.0;
    }

    /**
     * ccor.<br>
     * CHEMISTRY/DISSOCIATION CORRECTION FOR MSIS MODELS.
     * 
     * @param alt
     *        altitude
     * @param r
     *        target ratio
     * @param h1
     *        transition scale length
     * @param zh
     *        altitude of 1/2 r
     * @return exp(e)
     */
    protected double ccor(final double alt, final double r, final double h1, final double zh) {
        // Initialization
        double retd = 0.;
        double e;
        // e = difference between altitude and altitude of 1/2 target ration, devided by transition
        //     scale length
        e = (alt - zh) / h1;
        if (e > 70) {
            // result : exp(0)
            retd = MathLib.exp(0);
        } else if (e < -70) {
            // result : exp(r)
            retd = MathLib.exp(r);
        } else {
            // result = exp(r / (1 + exp(e)))
            final double ex = MathLib.exp(e);
            e = r / (1.0 + ex);
            retd = MathLib.exp(e);
        }
        return retd;
    }

    /**
     * ccor2.<br>
     * CHEMISTRY/DISSOCIATION CORRECTION FOR MSIS MODELS
     * 
     * @param alt
     *        altitude
     * @param r
     *        target ratio
     * @param h1
     *        transition scale length
     * @param zh
     *        altitude of 1/2 r
     * @param h2
     *        transition scale length #2
     * @return exp(ccor2v)
     */
    protected double ccor2(final double alt, final double r, final double h1, final double zh, final double h2) {
        double retd = 0.;
        final double e1;
        final double e2;
        e1 = (alt - zh) / h1;
        e2 = (alt - zh) / h2;
        if ((e1 > 70) || (e2 > 70)) {
            // Extreme case 1
            retd = MathLib.exp(0);
        } else if ((e1 < -70) && (e2 < -70)) {
            // Extreme case 2
            retd = MathLib.exp(r);
        } else {
            // General case
            final double ex1 = MathLib.exp(e1);
            final double ex2 = MathLib.exp(e2);
            final double ccor2v = r / (1.0 + 0.5 * (ex1 + ex2));
            retd = MathLib.exp(ccor2v);
        }
        return retd;
    }

    /**
     * Calculate temperature and density profiles for lower atmosphere.
     * 
     * @param alt
     *        alt
     * @param d0
     *        d0
     * @param xm
     *        xm
     * @param tz
     *        tz
     * @param mn3
     *        mn3
     * @param zn3
     *        zn3
     * @param tn3
     *        tn3
     * @param tgn3
     *        tgn3
     * @param mn2
     *        mn2
     * @param zn2
     *        zn2
     * @param tn2
     *        tn2
     * @param tgn2
     *        tgn2
     * @return density and temperature for lower atmosphere
     */
    // CHECKSTYLE: stop MethodLength check
    protected double[] densm(final double alt, final double d0, final double xm, final double tz, final int mn3,
            final double[] zn3,
            final double[] tn3, final double[] tgn3, final int mn2, final double[] zn2,
            final double[] tn2,
            final double[] tgn2) {
        // CHECKSTYLE: resume MethodLength check
        // initialize xs array
        final double[] xs = new double[10];
        // initialize ys array
        final double[] ys = new double[10];
        // initialize y2out array
        final double[] y2out = new double[10];
        double z = 0;
        double z1 = 0;
        double z2 = 0;
        double t1 = 0;
        double t2 = 0;
        double zg = 0;
        double zgdif = 0;
        double yd1 = 0;
        double yd2 = 0;
        double x = 0;
        double y = 0;
        double yi = 0;
        double expl = 0;
        double gamm = 0;
        double glb = 0;
        double densmTmp = 0;
        int mn = 0;
        int k;
        densmTmp = d0;
        double rez = 0;
        double temp = tz;
        if (alt > zn2[0]) {
            rez = returnSwitch(xm, tz, d0);
        } else {

            // STRATOSPHERE/MESOSPHERE TEMPERATURE
            mn = mn2;
            z = (alt > zn2[mn - 1] ? alt : zn2[mn - 1]);
            z1 = zn2[0];
            z2 = zn2[mn - 1];
            t1 = tn2[0];
            t2 = tn2[mn - 1];
            zg = this.zeta(z, z1);
            zgdif = this.zeta(z2, z1);
            // set up spline nodes
            for (k = mn - 1; k >= 0; k--) {
                xs[k] = this.zeta(zn2[k], z1) / zgdif;
                ys[k] = 1.0 / tn2[k];
            }
            yd1 = -tgn2[0] / (t1 * t1) * zgdif;
            yd2 = -tgn2[1] / (t2 * t2) * zgdif * MathLib.pow((this.re + z2) / (this.re + z1), 2);

            // calculate spline coefficients
            this.spline(xs, ys, mn, yd1, yd2, y2out);
            x = zg / zgdif;
            y = this.splint(xs, ys, y2out, mn, x);

            // temperature at altitude
            double th = 1.0 / y;
            if (xm != 0.0) {
                // calculate stratosphere / mesospehere density
                glb = this.gsurf / (MathLib.pow(1.0 + z1 / this.re, 2));
                gamm = xm * glb * zgdif / RGAS;

                // Integrate temperature profile
                yi = this.splini(xs, ys, y2out, mn, x);
                expl = gamm * yi;
                if (expl > C_50) {
                    expl = C_50;
                }
                // Density at altitude
                densmTmp = densmTmp * (t1 / th) * MathLib.exp(-expl);
            }

            if (alt > zn3[0]) {
                rez = returnSwitch(xm, th, densmTmp);
            } else {

                // Troposhere / stratosphere temperature
                z = alt;
                mn = mn3;
                z1 = zn3[0];
                z2 = zn3[mn - 1];
                t1 = tn3[0];
                t2 = tn3[mn - 1];
                zg = this.zeta(z, z1);
                zgdif = this.zeta(z2, z1);

                // set up spline nodes
                for (k = mn - 1; k >= 0; k--) {
                    xs[k] = this.zeta(zn3[k], z1) / zgdif;
                    ys[k] = 1.0 / tn3[k];
                }
                yd1 = -tgn3[0] / (t1 * t1) * zgdif;
                yd2 = -tgn3[1] / (t2 * t2) * zgdif * MathLib.pow((this.re + z2) / (this.re + z1), 2);

                // calculate spline coefficients
                this.spline(xs, ys, mn, yd1, yd2, y2out);
                x = zg / zgdif;
                y = this.splint(xs, ys, y2out, mn, x);

                // temperature at altitude
                th = 1.0 / y;
                if (xm != 0.0) {
                    // calculate tropospheric / stratosphere density
                    glb = this.gsurf / (MathLib.pow(1.0 + z1 / this.re, 2));
                    gamm = xm * glb * zgdif / RGAS;

                    // Integrate temperature profile
                    yi = this.splini(xs, ys, y2out, mn, x);
                    expl = gamm * yi;
                    if (expl > C_50) {
                        expl = C_50;
                    }
                    // Density at altitude
                    densmTmp = densmTmp * (t1 / th) * MathLib.exp(-expl);
                }
                rez = returnSwitch(xm, th, densmTmp);
            }
            temp = th;
        }
        return new double[] { rez, temp};
    }

    /**
     * First return switch of the densm method.
     * 
     * @param xm
     *        xm
     * @param tz
     *        tz
     * @param elseValue
     *        the value return is ne "else" case
     * @return the right return
     */
    private static double returnSwitch(final double xm, final double tz, final double elseValue) {
        if (xm == 0.0) {
            return tz;
        }
        return elseValue;
    }

    /**
     * Calculate temperature and density profiles for MSIS models.
     * New lower thermo polynomial.
     * 
     * @param alt
     *        alt
     * @param dlb
     *        dlb
     * @param tinf
     *        tinf
     * @param tlb
     *        tlb
     * @param xm
     *        xm
     * @param alpha
     *        alpha
     * @param tz
     *        tz
     * @param zlb
     *        zlb
     * @param s2
     *        s2
     * @param mn1
     *        mn1
     * @param zn1
     *        zn1
     * @param tn1
     *        tn1
     * @param tgn1
     *        tgn1
     * @return densu
     */
    protected double densu(final double alt, final double dlb, final double tinf, final double tlb, final double xm,
                           final double alpha,
                           final Double[] tz, final double zlb, final double s2, final int mn1, final double[] zn1,
                           final double[] tn1,
                           final double[] tgn1) {
        double yd2 = 0;
        double yd1 = 0;
        double x = 0.0;
        double y = 0;
        double densuTemp = 1.0;
        double za = 0;
        double z;
        double zg2 = 0;
        double tt = 0;
        double ta = 0;
        double dta = 0;
        double z1 = 0.0;
        double z2 = 0;
        double t1 = 0.0;
        double t2 = 0;
        double zg = 0;
        double zgdif = 0.0;
        int mn = 0;

        final double[] xs = new double[5];
        final double[] ys = new double[5];
        final double[] y2out = new double[5];
        // joining altitudes of Bates and spline
        za = zn1[0];
        z = (alt > za ? alt : za);
        // geopotential altitude difference from ZLB
        zg2 = this.zeta(z, zlb);

        // Bates temperature
        tt = tinf - (tinf - tlb) * MathLib.exp(-s2 * zg2);
        ta = tt;
        tz[1] = tt;
        densuTemp = tz[1];

        if (alt < za) {
            /*
             * calculate temperature below ZA temperature gradient at ZA from
             * Bates profile
             */
            dta = (tinf - ta) * s2 * MathLib.pow((this.re + zlb) / (this.re + za), 2);
            tgn1[0] = dta;
            tn1[0] = ta;
            mn = mn1;
            z = (alt > zn1[mn - 1] ? alt : zn1[mn - 1]);
            z1 = zn1[0];
            z2 = zn1[mn - 1];
            t1 = tn1[0];
            t2 = tn1[mn - 1];
            // geopotential difference from z1
            zg = this.zeta(z, z1);
            zgdif = this.zeta(z2, z1);
            // set up spline nodes
            for (int k = 0; k < mn; k++) {
                xs[k] = this.zeta(zn1[k], z1) / zgdif;
                ys[k] = 1.0 / tn1[k];
            }
            // end node derivatives
            yd1 = -tgn1[0] / (t1 * t1) * zgdif;
            yd2 = -tgn1[1] / (t2 * t2) * zgdif * MathLib.pow((this.re + z2) / (this.re + z1), 2);
            // calculate spline coefficients
            this.spline(xs, ys, mn, yd1, yd2, y2out);
            x = zg / zgdif;
            y = this.splint(xs, ys, y2out, mn, x);
            // temperature at altitude
            tz[1] = 1.0 / y;
            densuTemp = tz[1];
        }
        if (xm == 0) {
            return densuTemp;
        }

        return this.computeDensuTemp(alt, dlb, tinf, tlb, xm, alpha, tz, zlb, s2, x, za, zg2,
            tt, z1, t1, zgdif, mn, xs, ys, y2out);
    }

    /**
     * Private method to minimise cyclomatic complexity of
     * the previous "densu" method.
     * 
     * @precondition
     * 
     * @param alt
     *        alt
     * @param dlb
     *        dlb
     * @param tinf
     *        tinf
     * @param tlb
     *        tlb
     * @param xm
     *        xm
     * @param alpha
     *        alpha
     * @param tz
     *        tz
     * @param zlb
     *        zlb
     * @param s2
     *        s2
     * @param x
     *        x
     * @param za
     *        za
     * @param zg2
     *        zg2
     * @param tt
     *        tt
     * @param z1
     *        z1
     * @param t1
     *        t1
     * @param zgdif
     *        zgdif
     * @param mn
     *        mn
     * @param xs
     *        xs
     * @param ys
     *        ys
     * @param y2out
     *        y2out
     * @return densu
     */
    private double computeDensuTemp(final double alt, final double dlb, final double tinf, final double tlb,
                                    final double xm, final double alpha, final Double[] tz, final double zlb,
                                    final double s2, final double x,
                                    final double za, final double zg2, final double tt, final double z1,
                                    final double t1, final double zgdif,
                                    final int mn, final double[] xs, final double[] ys, final double[] y2out) {
        // Init variables
        double densuTemp;
        double glb;
        double expl;
        double yi = 0;
        double densa = 0;
        double gamma = 0;
        double gamm = 0;
        // calculate density above za
        glb = this.gsurf / (MathLib.pow(1.0 + zlb / this.re, 2));
        gamma = xm * glb / (s2 * RGAS * tinf);
        expl = MathLib.exp(-s2 * gamma * zg2);

        // Clamp values
        if (expl > 50.0) {
            expl = 50.0;
        }
        if (tt <= 0) {
            expl = 50.0;
        }

        // density at altitude
        densa = dlb * MathLib.pow((tlb / tt), ((1.0 + alpha + gamma))) * expl;
        densuTemp = densa;
        double rez = 0;
        if (alt >= za) {
            rez = densuTemp;
        } else {
            // calculate density below za
            glb = this.gsurf / (MathLib.pow(1.0 + z1 / this.re, 2));
            gamm = xm * glb * zgdif / RGAS;

            // integrate spline temperatures
            yi = this.splini(xs, ys, y2out, mn, x);
            expl = gamm * yi;
            if (expl > 50.0) {
                expl = 50.0;
            }
            if (tz[1] <= 0) {
                expl = 50.0;
            }
            // density at altitude
            densuTemp = densuTemp * MathLib.pow((t1 / tz[1]), (1.0 + alpha)) * MathLib.exp(-expl);

            rez = densuTemp;
        }
        return rez;
    }

    /**
     * dnet Turbopause correction for MSIS models root mean density.
     * 
     * @param dd
     *        diffusive density
     * @param dm
     *        full mixed density
     * @param zhm
     *        transition scale length
     * @param xmm
     *        full mixed molecular weight
     * @param xm
     *        species molecular weight
     * @return combined density
     */
    protected double dnet(final double dd, final double dm, final double zhm, final double xmm, final double xm) {
        // Initialization
        double retd = 0.;
        double ddd = dd;
        // Check values of diffusive density and full mixed density before computing result
        if (((dm > 0) && (dd > 0))) {
            double a = zhm / (xmm - xm);
            final double ylog = a * MathLib.log(dm / ddd);
            if (ylog < -10) {
                retd = ddd;
            } else if (ylog > 10) {
                retd = dm;
            } else {
                // result : dd * (1 + exp((zhm/(xmm - xm))* log(dm/dd)))^(1/(zhm/(xmm - xm)))
                a = ddd * MathLib.pow((1.0 + MathLib.exp(ylog)), (1.0 / a));
                retd = a;
            }
        } else {
            // Set ddd if both densities are null
            if ((dd == 0) && (dm == 0)) {
                ddd = 1;
            }
            // if one of the densities is null, set the result to the other
            if (dm == 0) {
                retd = ddd;
            } else if (ddd == 0) {
                retd = dm;
            }
        }

        return retd;

    }

    /**
     * glatf.
     * 
     * @param lat
     *        latitude
     */
    private void glatf(final double lat) {
        final double c2 = MathLib.cos(2.0 * MathLib.toRadians(lat));
        this.gsurf = 980.616 * (1.0 - 0.0026373 * c2);
        this.re = this.gsurf / (3.085462E-6 + 2.27E-9 * c2) * 2.0E-5;
    }

    /**
     * glob7s : version of globe for lower atmosphere 10/26/99.
     * 
     * @param p
     *        p
     * @param input
     *        input
     * @param flags
     *        flags
     * @return glob7s
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    private double glob7s(final double[] p, final Input input, final Flags flags) {
        // CHECKSTYLE: resume CyclomaticComplexity check

        // confirm parameter set
        final double pset = 2.0;
        if (p[99] == 0) {
            p[99] = pset;
        }
        if (p[99] != pset) {
            return -1;
        }

        // Initialize data
        final double dr = 1.72142E-2;

        final double cd32 = MathLib.cos(dr * (input.getDoy() - p[31]));
        final double cd18 = MathLib.cos(2.0 * dr * (input.getDoy() - p[17]));
        final double cd14 = MathLib.cos(dr * (input.getDoy() - p[13]));
        final double cd39 = MathLib.cos(2.0 * dr * (input.getDoy() - p[38]));

        // Initialize result
        final double[] tGlob7s = new double[14];

        // F10.7
        final double dfa = input.getF107A() - 150.0;
        tGlob7s[0] = p[21] * dfa;

        // time independent
        tGlob7s[1] =
            p[1] * this.plg[2] + p[2] * this.plg[4] + p[22] * this.plg[6] + p[26] * this.plg[1] + p[14] * this.plg[3]
                    + p[59] * this.plg[5];

        // SYMMETRICAL ANNUAL
        tGlob7s[2] = (p[18] + p[47] * this.plg[2] + p[29] * this.plg[4]) * cd32;

        // SYMMETRICAL SEMIANNUAL
        tGlob7s[3] = (p[15] + p[16] * this.plg[2] + p[30] * this.plg[4]) * cd18;

        // ASYMMETRICAL ANNUAL
        tGlob7s[4] = (p[9] * this.plg[1] + p[10] * this.plg[3] + p[20] * this.plg[5]) * cd14;

        // ASYMMETRICAL SEMIANNUAL
        tGlob7s[5] = (p[37] * this.plg[1]) * cd39;

        // DIURNAL
        if (flags.bool(flags.getSw(7))) {
            final double t71 = p[11] * this.plg[11] * cd14 * flags.getSwc(5);
            final double t72 = p[12] * this.plg[11] * cd14 * flags.getSwc(5);
            tGlob7s[6] =
                ((p[3] * this.plg[10] + p[4] * this.plg[12] + t71) * this.ctloc + (p[6] * this.plg[10] + p[7]
                        * this.plg[12] + t72)
                        * this.stloc);
        }

        // SEMIDIURNAL
        if (flags.bool(flags.getSw(8))) {
            final double t81 = (p[23] * this.plg[21] + p[35] * this.plg[23]) * cd14 * flags.getSwc(5);
            final double t82 = (p[33] * this.plg[21] + p[36] * this.plg[23]) * cd14 * flags.getSwc(5);
            tGlob7s[7] =
                ((p[5] * this.plg[20] + p[41] * this.plg[22] + t81) * this.c2tloc + (p[8] * this.plg[20] + p[42]
                        * this.plg[22] + t82)
                        * this.s2tloc);
        }

        // TERDIURNAL
        if (flags.bool(flags.getSw(14))) {
            tGlob7s[13] = p[39] * this.plg[30] * this.s3tloc + p[40] * this.plg[30] * this.c3tloc;
        }

        // MAGNETIC ACTIVITY
        if (flags.bool(flags.getSw(9))) {
            if (flags.getSw(9) == 1) {
                tGlob7s[8] = this.apdf * (p[32] + p[45] * this.plg[2] * flags.getSwc(2));
            }
            if (flags.getSw(9) == -1) {
                tGlob7s[8] = (p[50] * this.apt[0] + p[96] * this.plg[2] * this.apt[0] * flags.getSwc(2));
            }
        }

        // LONGITUDINAL
        if (!((flags.getSw(10) == 0) || (flags.getSw(11) == 0) || (input.getgLong() <= -1000.0))) {
            tGlob7s[10] =
                (1.0
                        + this.plg[1]
                        * (p[80] * flags.getSwc(5) * MathLib.cos(dr * (input.getDoy() - p[81])) + p[85]
                                * flags.getSwc(6)
                                * MathLib.cos(2.0 * dr * (input.getDoy() - p[86]))) + p[83] * flags.getSwc(3)
                        * MathLib.cos(dr * (input.getDoy() - p[84])) + p[87] * flags.getSwc(4)
                        * MathLib.cos(2.0 * dr * (input.getDoy() - p[88])))
                        * ((p[64] * this.plg[11] + p[65] * this.plg[13] + p[66] * this.plg[15] + p[74] * this.plg[10]
                                + p[75] * this.plg[12] + p[76]
                                * this.plg[14])
                                * MathLib.cos(MathLib.toRadians(input.getgLong())) + (p[90] * this.plg[11] + p[91]
                                * this.plg[13]
                                + p[92] * this.plg[15] + p[77]
                                * this.plg[10] + p[78] * this.plg[12] + p[79] * this.plg[14])
                                * MathLib.sin(MathLib.toRadians(input.getgLong())));
        }
        
        // Build result
        double tt = 0;
        for (int i = 13; i >= 0; i--) {
            tt += MathLib.abs(flags.getSw(i + 1)) * tGlob7s[i];
        }
        return tt;
    }

    /**
     * globe7 : CALCULATE G(L) FUNCTION.
     * 
     * @param p
     *        p
     * @param input
     *        input
     * @param flags
     *        flags
     * @return g(l)
     */
    // CHECKSTYLE: stop MethodLength check
    // CHECKSTYLE: stop CyclomaticComplexity check
    private double globe7(final double[] p, final Input input, final Flags flags) {
        // CHECKSTYLE: resume CyclomaticComplexity check
        // CHECKSTYLE: resume MethodLength check
        // Upper Thermosphere Parameters
        double[] tGlob7 = new double[14];
        double apd = 0;
        final double sr = 7.2722E-5;
        final double dr = 1.72142E-2;
        final double hr = 0.2618;

        double df = 0;
        double dfa = 0;
        double f1 = 0;
        double f2 = 0;
        double tinf = 0;
        final ApCoef ap;
        double cd32 = 0;
        double cd18 = 0;
        double cd14 = 0;
        double cd39 = 0;

        final double tloc = input.getLst();
        final int doy = input.getDoy();

        // quick cache from a call to another
        if (tloc != this.oldTloc) {

            if (!(((flags.getSw(7) == 0) && (flags.getSw(8) == 0)) && (flags.getSw(14) == 0))) {
                final double[] sincos = MathLib.sinAndCos(hr * tloc);
                this.stloc = sincos[0];
                this.ctloc = sincos[1];
                this.c2tloc = (2 * this.ctloc * this.ctloc) - 1;
                this.s2tloc = 2 * this.stloc * this.ctloc;
                this.c3tloc = (this.c2tloc * this.ctloc) - (this.s2tloc * this.stloc);
                this.s3tloc = (this.s2tloc * this.ctloc) + (this.stloc * this.c2tloc);
            }
        }

        cd32 = MathLib.cos(dr * (doy - p[31]));
        cd18 = MathLib.cos(2.0 * dr * (doy - p[17]));
        cd14 = MathLib.cos(dr * (doy - p[13]));
        cd39 = MathLib.cos(2.0 * dr * (doy - p[38]));

        // F10.7 EFFECT
        df = input.getF107() - input.getF107A();
        dfa = input.getF107A() - 150.0;
        tGlob7[0] = p[19] * df * (1.0 + p[59] * dfa) + p[20] * df * df + p[21] * dfa + p[29] * MathLib.pow(dfa, 2.0);
        final double t1 = p[19] * df + p[20] * df * df;
        f1 = 1.0 + (p[47] * dfa + t1) * flags.getSwc(1);
        f2 = 1.0 + (p[49] * dfa + t1) * flags.getSwc(1);

        // TIME INDEPENDENT
        tGlob7[1] =
            (p[1] * this.plg[2] + p[2] * this.plg[4] + p[22] * this.plg[6]) + (p[14] * this.plg[2]) * dfa
                    * flags.getSwc(1)
                    + p[26] * this.plg[1];

        // SYMMETRICAL ANNUAL
        tGlob7[2] = p[18] * cd32;

        // SYMMETRICAL SEMIANNUAL
        tGlob7[3] = (p[15] + p[16] * this.plg[2]) * cd18;

        // ASYMMETRICAL ANNUAL
        tGlob7[4] = f1 * (p[9] * this.plg[1] + p[10] * this.plg[3]) * cd14;

        // ASYMMETRICAL SEMIANNUAL
        tGlob7[5] = p[37] * this.plg[1] * cd39;

        // DIURNAL
        if (flags.bool(flags.getSw(7))) {
            final double t71 = (p[11] * this.plg[11]) * cd14 * flags.getSwc(5);
            final double t72 = (p[12] * this.plg[11]) * cd14 * flags.getSwc(5);
            tGlob7[6] =
                f2
                        * ((p[3] * this.plg[10] + p[4] * this.plg[12] + p[27] * this.plg[14] + t71) * this.ctloc + (p[6]
                                * this.plg[10] + p[7]
                                * this.plg[12] + p[28]
                                * this.plg[14] + t72)
                                * this.stloc);
        }

        /* SEMIDIURNAL */
        if (flags.bool(flags.getSw(8))) {
            final double t81 = (p[23] * this.plg[21] + p[35] * this.plg[23]) * cd14 * flags.getSwc(5);
            final double t82 = (p[33] * this.plg[21] + p[36] * this.plg[23]) * cd14 * flags.getSwc(5);
            tGlob7[7] =
                f2
                        * ((p[5] * this.plg[20] + p[41] * this.plg[22] + t81) * this.c2tloc + (p[8] * this.plg[20]
                                + p[42]
                                * this.plg[22] + t82)
                                * this.s2tloc);
        }

        /* TERDIURNAL */
        if (flags.bool(flags.getSw(14))) {
            tGlob7[13] =
                f2
                        * ((p[39] * this.plg[30] + (p[93] * this.plg[31] + p[46] * this.plg[33]) * cd14
                                * flags.getSwc(5))
                                * this.s3tloc +
                        (p[40] * this.plg[30] + (p[94]
                                * this.plg[31] + p[48] * this.plg[33])
                                * cd14 * flags.getSwc(5))
                                * this.c3tloc);
        }

        /* Magnetic activity based on daily ap */
        if (flags.getSw(9) == -1) {
            ap = input.getApA();
            if (p[51] != 0) {
                double exp1;
                exp1 = MathLib.exp(-10800.0 * MathLib.abs(p[51])
                        / (1.0 + p[138] * (45.0 - MathLib.abs(input.getgLat()))));
                if (exp1 > 0.99999) {
                    exp1 = 0.99999;
                }
                if (p[24] < 1.0E-4) {
                    p[24] = 1.0E-4;
                }
                this.apt[0] = this.sg0(exp1, p, ap.getAp());
                /*
                 * apt[1]=sg2(exp1,p,ap.ap);
                 * apt[2]=sg0(exp2,p,ap.ap);
                 * apt[3]=sg2(exp2,p,ap.ap);
                 */
                if (flags.bool(flags.getSw(9))) {
                    tGlob7[8] = this.apt[0]
                            * (p[50] + p[96] * this.plg[2] + p[54] * this.plg[4]
                                    + (p[125] * this.plg[1] + p[126] * this.plg[3] + p[127] * this.plg[5]) * cd14
                                    * flags.getSwc(5) + (p[128] * this.plg[10] + p[129] * this.plg[12] + p[130]
                                    * this.plg[14])
                                    * flags.getSwc(7)
                                    * MathLib.cos(hr * (tloc - p[131])));
                }
            }
        } else {
            apd = input.getAp() - 4.0;
            double p44 = p[43];
            final double p45 = p[44];
            if (p44 < 0) {
                p44 = 1.0E-5;
            }
            this.apdf = apd + (p45 - 1.0) * (apd + (MathLib.exp(-p44 * apd) - 1.0) / p44);
            if (flags.bool(flags.getSw(9))) {
                tGlob7[8] = this.apdf
                        * (p[32] + p[45] * this.plg[2] + p[34] * this.plg[4]
                                + (p[100] * this.plg[1] + p[101] * this.plg[3] + p[102] * this.plg[5]) * cd14
                                * flags.getSwc(5) + (p[121] * this.plg[10] + p[122] * this.plg[12] + p[123]
                                * this.plg[14])
                                * flags.getSwc(7)
                                * MathLib.cos(hr * (tloc - p[124])));
            }
        }

        if (flags.bool(flags.getSw(10)) && (input.getgLong() > -1000.0)) {

            tGlob7 = this.fillT(p, input, flags, tGlob7, sr, dfa, cd14);
        }

        // Params not used: 82, 89, 99, 139-149
        tinf = p[30];
        for (int i = 13; i >= 0; i--) {
            tinf += MathLib.abs(flags.getSw(i + 1)) * tGlob7[i];
        }
        this.oldTloc = tloc;
        return tinf;
    }

    /**
     * Fill the "t" array.
     * 
     * @param p
     *        p
     * @param input
     *        input
     * @param flags
     *        flags
     * @param t
     *        t_glob7
     * @param sr
     *        sr
     * @param dfa
     *        dfa
     * @param cd14
     *        cd14
     * @return filled t
     */
    private double[] fillT(final double[] p, final Input input, final Flags flags, final double[] t, final double sr,
                           final double dfa, final double cd14) {
        // longitudinal
        if (flags.bool(flags.getSw(11))) {
            final double[] sincos = MathLib.sinAndCos(MathLib.toRadians(input.getgLong()));
            final double sin = sincos[0];
            final double cos = sincos[1];
            t[10] =
                (1.0 + p[80] * dfa * flags.getSwc(1))
                        * ((p[64] * this.plg[11] + p[65] * this.plg[13] + p[66] * this.plg[15] + p[103] * this.plg[10]
                                + p[104] * this.plg[12]
                                + p[105] * this.plg[14] + flags
                            .getSwc(5) * (p[109] * this.plg[10] + p[110] * this.plg[12] + p[111] * this.plg[14]) * cd14)
                                * cos + (p[90] * this.plg[11] + p[91]
                                * this.plg[13] + p[92] * this.plg[15]
                                + p[106] * this.plg[10] + p[107] * this.plg[12] + p[108] * this.plg[14] + flags
                            .getSwc(5)
                                * (p[112] * this.plg[10] + p[113] * this.plg[12] + p[114] * this.plg[14]) * cd14)
                                * sin);
        }

        // ut and mixed ut, longitude
        if (flags.bool(flags.getSw(12))) {
            t[11] = (1.0 + p[95] * this.plg[1])
                    * (1.0 + p[81] * dfa * flags.getSwc(1))
                    * (1.0 + p[119] * this.plg[1] * flags.getSwc(5) * cd14)
                    * ((p[68] * this.plg[1] + p[69] * this.plg[3] + p[70] * this.plg[5]) * MathLib.cos(sr
                            * (input.getSec() - p[71])));
            t[11] += flags.getSwc(11) * (p[76] * this.plg[21] + p[77] * this.plg[23] + p[78] * this.plg[25])
                    * MathLib.cos(sr * (input.getSec() - p[79]) + 2.0 * MathLib.toRadians(input.getgLong()))
                    * (1.0 + p[137] * dfa * flags.getSwc(1));
        }

        // ut, longitude magnetic activity
        if (flags.bool(flags.getSw(13))) {
            if (flags.getSw(9) == -1) {
                if (flags.bool(p[51])) {
                    t[12] = this.apt[0]
                            * flags.getSwc(11)
                            * (1. + p[132] * this.plg[1])
                            * ((p[52] * this.plg[11] + p[98] * this.plg[13] + p[67] * this.plg[15]) * MathLib
                                .cos(MathLib
                                    .toRadians(input.getgLong()
                                            - p[97]))) + this.apt[0] * flags.getSwc(11) * flags.getSwc(5)
                            * (p[133] * this.plg[10] + p[134] * this.plg[12] + p[135] * this.plg[14]) * cd14
                            * MathLib.cos(MathLib.toRadians(input.getgLong() - p[136])) + this.apt[0]
                            * flags.getSwc(12)
                            * (p[55] * this.plg[1] + p[56] * this.plg[3] + p[57] * this.plg[5])
                            * MathLib.cos(sr * (input.getSec() - p[58]));
                }
            } else {
                t[12] = this.apdf
                        * flags.getSwc(11)
                        * (1.0 + p[120] * this.plg[1])
                        * ((p[60] * this.plg[11] + p[61] * this.plg[13] + p[62] * this.plg[15]) * MathLib.cos(MathLib
                            .toRadians(input.getgLong() - p[63])))
                        + this.apdf * flags.getSwc(11) * flags.getSwc(5)
                        * (p[115] * this.plg[10] + p[116] * this.plg[12] + p[117] * this.plg[14])
                        * cd14 * MathLib.cos(MathLib.toRadians(input.getgLong() - p[118])) + this.apdf
                        * flags.getSwc(12)
                        * (p[83] * this.plg[1] + p[84] * this.plg[3] + p[85] * this.plg[5])
                        * MathLib.cos(sr * (input.getSec() - p[75]));
            }
        }
        return t;
    }

    /**
     * gtd7 : Neutral Atmosphere Empirical Model from the surface to lower exosphere.
     * 
     * @param input
     *        ipnut
     * @param flags
     *        flags
     * @param output
     *        output
     */
    // CHECKSTYLE: stop MethodLength check
    protected void gtd7(final Input input, final Flags flags, final Output output) {
        // CHECKSTYLE: resume MethodLength check
        double xlat;
        final double xmm;
        final double[] zn2 = { 72.5, 55.0, 45.0, 32.5 };
        final double altt;
        final double tmp;
        final double dm28m;
        final Output soutput = new Output();

        flags.tselec();

        // Latitude variation of gravity (none for getSw()[2]=0)
        xlat = input.getgLat();
        if (flags.getSw(2) == 0) {
            xlat = 45.0;
        }
        this.glatf(xlat);

        xmm = pdm[24];

        // THERMOSPHERE / MESOSPHERE (above zn2[0])
        altt = (input.getAlt() > zn2[0] ? input.getAlt() : zn2[0]);

        tmp = input.getAlt();
        input.setAlt(altt);
        this.gts7(input, flags, soutput);
        input.setAlt(tmp);
        // metric adjustment:
        dm28m = (flags.bool(flags.getSw(0)) ? this.dm28 * 1.0E6 : this.dm28);
        output.getT()[0] = soutput.getT(0);
        output.getT()[1] = soutput.getT(1);
        if (input.getAlt() >= zn2[0]) {
            // get the arrays ref and copy values
            final double[] soutD = soutput.getD();
            final double[] outD = output.getD();
            System.arraycopy(soutD, 0, outD, 0, 9);
            return;
        }

        // Temporary variables
        final int mn3 = 5;
        final double[] zn3 = { 32.5, 20.0, 15.0, 10.0, 0.0 };
        final int mn2 = 4;
        final double zmix = 62.5;
        Double tz = new Double(0);
        double dmc;
        double dmr;
        final double dz28;

        /*
         * LOWER MESOSPHERE/UPPER STRATOSPHERE (between zn3[0] and zn2[0])
         * Temperature at nodes and gradients at end nodes
         * Inverse temperature a linear function of spherical harmonics
         */
        this.mesoTgn2[0] = this.mesoTgn1[1];
        this.mesoTn2[0] = this.mesoTn1[4];
        this.mesoTn2[1] = pma[0][0] * pavgm[0] / (1.0 - flags.getSw(20) * this.glob7s(pma[0], input, flags));
        this.mesoTn2[2] = pma[1][0] * pavgm[1] / (1.0 - flags.getSw(20) * this.glob7s(pma[1], input, flags));
        this.mesoTn2[3] =
            pma[2][0] * pavgm[2] / (1.0 - flags.getSw(20) * flags.getSw(22) * this.glob7s(pma[2], input, flags));
        this.mesoTgn2[1] =
            pavgm[8] * pma[9][0] * (1.0 + flags.getSw(20) * flags.getSw(22) * this.glob7s(pma[9], input, flags))
                    * this.mesoTn2[3] * this.mesoTn2[3] / (MathLib.pow(pma[2][0] * pavgm[2], 2));
        this.mesoTn3[0] = this.mesoTn2[3];

        if (input.getAlt() < zn3[0]) {
            /*
             * LOWER STRATOSPHERE AND TROPOSPHERE (below zn3[0])
             * Temperature at nodes and gradients at end nodes
             * Inverse temperature a linear function of spherical harmonics
             */
            this.mesoTgn3[0] = this.mesoTgn2[1];
            this.mesoTn3[1] = pma[3][0] * pavgm[3] / (1.0 - flags.getSw(22) * this.glob7s(pma[3], input, flags));
            this.mesoTn3[2] = pma[4][0] * pavgm[4] / (1.0 - flags.getSw(22) * this.glob7s(pma[4], input, flags));
            this.mesoTn3[3] = pma[5][0] * pavgm[5] / (1.0 - flags.getSw(22) * this.glob7s(pma[5], input, flags));
            this.mesoTn3[4] = pma[6][0] * pavgm[6] / (1.0 - flags.getSw(22) * this.glob7s(pma[6], input, flags));
            this.mesoTgn3[1] =
                pma[7][0] * pavgm[7] * (1.0 + flags.getSw(22) * this.glob7s(pma[7], input, flags)) * this.mesoTn3[4]
                        * this.mesoTn3[4] / (MathLib.pow(pma[6][0] * pavgm[6], 2));
        }

        /* LINEAR TRANSITION TO FULL MIXING BELOW zn2[0] */

        dmc = 0;
        if (input.getAlt() > zmix) {
            dmc = 1.0 - (zn2[0] - input.getAlt()) / (zn2[0] - zmix);
        }
        dz28 = soutput.getD()[2];

        // N2 density
        dmr = soutput.getD()[2] / dm28m - 1.0;
        final double[] res = this.densm(input.getAlt(), dm28m, xmm, tz, mn3, zn3,
            this.mesoTn3, this.mesoTgn3, mn2, zn2, this.mesoTn2, this.mesoTgn2);
        output.getD()[2] = res[0];
        tz = res[1];
        output.getD()[2] = output.getD(2) * (1.0 + dmr * dmc);

        // HE density
        dmr = soutput.getD()[0] / (dz28 * pdm[1]) - 1.0;
        output.getD()[0] = output.getD(2) * pdm[1] * (1.0 + dmr * dmc);

        // O density
        output.getD()[1] = 0;
        output.getD()[8] = 0;

        // O2 density
        dmr = soutput.getD()[3] / (dz28 * pdm[31]) - 1.0;
        output.getD()[3] = output.getD(2) * pdm[31] * (1.0 + dmr * dmc);

        // AR density
        dmr = soutput.getD()[4] / (dz28 * pdm[41]) - 1.0;
        output.getD()[4] = output.getD(2) * pdm[41] * (1.0 + dmr * dmc);

        // Hydrogen density
        output.getD()[6] = 0;

        // Atomic nitrogen density
        output.getD()[7] = 0;

        // Total mass density
        output.getD()[5] = 1.66E-24 * (4.0 * output.getD(0) + 16.0 * output.getD(1) + 28.0 * output.getD(2) + 32.0
                * output.getD(3)
                + 40.0 * output.getD(4) + output.getD(6) + 14.0 * output.getD(7));

        if (flags.bool(flags.getSw(0))) {
            output.getD()[5] = output.getD(5) * 0.001;
        }

        // temperature at altitude
        output.getT()[1] = tz;
    }

    /**
     * gts7 : Thermospheric portion of NRLMSISE-00
     * See GTD7 for more extensive comments
     * alt > 72.5 km!
     * 
     * @param input
     *        input
     * @param flags
     *        flags
     * @param output
     *        output
     */
    // CHECKSTYLE: stop MethodLength check
    // CHECKSTYLE: stop CyclomaticComplexity check
    private void gts7(final Input input, final Flags flags, final Output output) {
        // CHECKSTYLE: resume CyclomaticComplexity check
        // CHECKSTYLE: resume MethodLength check

        this.calculateLegendrePolynomial(input);

        final double za;
        double z;
        final double[] zn1 = { 120.0, 110.0, 100.0, 90.0, 72.5 };
        double tinf = 0;
        final double g0;
        final int mn1 = 5;

        final Double[] tz = new Double[2];
        java.util.Arrays.fill(tz, 0.0);

        final double dr = 1.72142E-2;
        final double[] alpha = { -0.38, 0.0, 0.0, 0.0, 0.17, 0.0, -0.38, 0.0, 0.0 };
        final double[] altl = { 200.0, 300.0, 160.0, 250.0, 240.0, 450.0, 320.0, 450.0 };
        final double dd;
        // final double hc216;
        // final double hcc232;

        final Double[] outputT = output.getT();
        final double[] outputD = output.getD();
        za = pdl[40];
        zn1[0] = za;
        java.util.Arrays.fill(outputD, 0.0);

        // TINF VARIATIONS NOT IMPORTANT BELOW ZA OR ZN1(1)
        if (input.getAlt() > zn1[0]) {
            tinf = ptm[0] * pt[0] * (1.0 + flags.getSw(16) * this.globe7(pt, input, flags));
        } else {
            tinf = ptm[0] * pt[0];
        }
        outputT[0] = tinf;

        // GRADIENT VARIATIONS NOT IMPORTANT BELOW ZN1(5)
        if (input.getAlt() > zn1[4]) {
            g0 = ptm[3] * ps[0] * (1.0 + flags.getSw(19) * this.globe7(ps, input, flags));
        } else {
            g0 = ptm[3] * ps[0];
        }
        final double tlb = ptm[1] * (1.0 + flags.getSw(17) * this.globe7(pd[3], input, flags)) * pd[3][0];
        final double s = g0 / (tinf - tlb);

        /*
         * Lower thermosphere temp variations not significant for
         * density above 300 km
         */
        if (input.getAlt() < 300.0) {
            this.mesoTn1[1] = ptm[6] * ptl[0][0] / (1.0 - flags.getSw(18) * this.glob7s(ptl[0], input, flags));
            this.mesoTn1[2] = ptm[2] * ptl[1][0] / (1.0 - flags.getSw(18) * this.glob7s(ptl[1], input, flags));
            this.mesoTn1[3] = ptm[7] * ptl[2][0] / (1.0 - flags.getSw(18) * this.glob7s(ptl[2], input, flags));
            this.mesoTn1[4] =
                ptm[4] * ptl[3][0] / (1.0 - flags.getSw(18) * flags.getSw(20) * this.glob7s(ptl[3], input, flags));
            this.mesoTgn1[1] =
                ptm[8] * pma[8][0] * (1.0 + flags.getSw(18) * flags.getSw(20) * this.glob7s(pma[8], input, flags))
                        * this.mesoTn1[4]
                        * this.mesoTn1[4] / (MathLib.pow(ptm[4] * ptl[3][0], 2));
        } else {
            this.mesoTn1[1] = ptm[6] * ptl[0][0];
            this.mesoTn1[2] = ptm[2] * ptl[1][0];
            this.mesoTn1[3] = ptm[7] * ptl[2][0];
            this.mesoTn1[4] = ptm[4] * ptl[3][0];
            this.mesoTgn1[1] =
                ptm[8] * pma[8][0] * this.mesoTn1[4] * this.mesoTn1[4] / (MathLib.pow(ptm[4] * ptl[3][0], 2));
        }

        // N2 variation factor at Zlb
        final double g28 = flags.getSw(21) * this.globe7(pd[2], input, flags);

        // VARIATION OF TURBOPAUSE HEIGHT
        final double zhf = pdl[49]
                * (1.0 + flags.getSw(5) * pdl[24] * MathLib.sin(MathLib.toRadians(input.getgLat()))
                        * MathLib.cos(dr * (input.getDoy() - pt[13])));
        outputT[0] = tinf;
        final double xmm = pdm[24];
        z = input.getAlt();

        // **** N2 DENSITY ****

        // Diffusive density at Zlb
        final double db28 = pdm[20] * MathLib.exp(g28) * pd[2][0];
        // Diffusive density at Alt
        outputD[2] =
            this.densu(z, db28, tinf, tlb, 28.0, alpha[2], output.getT(), ptm[5], s, mn1, zn1, this.mesoTn1,
                this.mesoTgn1);
        // Turbopause
        final double zh28 = pdm[22] * zhf;
        final double zhm28 = pdm[23] * pdl[30];
        final double xmd = 28.0 - xmm;
        // Mixed density at Zlb
        final double b28 =
            this.densu(zh28, db28, tinf, tlb, xmd, (alpha[2] - 1.0), tz, ptm[5], s, mn1, zn1, this.mesoTn1,
                this.mesoTgn1);
        if (flags.bool(flags.getSw(15)) && (z <= altl[2])) {
            // Mixed density at Alt
            this.dm28 =
                this.densu(z, b28, tinf, tlb, xmm, alpha[2], tz, ptm[5], s, mn1, zn1, this.mesoTn1, this.mesoTgn1);
            // Net density at Alt
            outputD[2] = this.dnet(output.getD(2), this.dm28, zhm28, xmm, 28.0);
        }

        // **** HE DENSITY ****

        // Density variation factor at Zlb
        final double g4 = flags.getSw(21) * this.globe7(pd[0], input, flags);
        // Diffusive density at Zlb
        final double db04 = pdm[0] * MathLib.exp(g4) * pd[0][0];
        // Diffusive density at Alt
        outputD[0] =
            this.densu(z, db04, tinf, tlb, 4., alpha[0], output.getT(), ptm[5], s, mn1, zn1, this.mesoTn1,
                this.mesoTgn1);
        double rl = 0;
        if (flags.bool(flags.getSw(15)) && (z < altl[0])) {
            // Turbopause
            final double zh04 = pdm[2];
            // Mixed density at Zlb
            final double b04 =
                this.densu(zh04, db04, tinf, tlb, 4. - xmm, alpha[0] - 1., output.getT(), ptm[5], s, mn1,
                    zn1, this.mesoTn1, this.mesoTgn1);
            // Mixed density at Alt
            this.dm04 =
                this.densu(z, b04, tinf, tlb, xmm, 0., output.getT(), ptm[5], s, mn1, zn1, this.mesoTn1, this.mesoTgn1);
            final double zhm04 = zhm28;
            // Net density at Alt
            outputD[0] = this.dnet(output.getD(0), this.dm04, zhm04, xmm, 4.);
            // Correction to specified mixing ratio at ground
            rl = MathLib.log(b28 * pdm[1] / b04);
            final double zc04 = pdm[4] * pdl[25];
            final double hc04 = pdm[5] * pdl[26];
            // Net density corrected at Alt
            outputD[0] *= this.ccor(z, rl, hc04, zc04);
        }

        // **** O DENSITY ****

        // Density variation factor at Zlb
        double g = flags.getSw(21) * this.globe7(pd[1], input, flags);
        // Diffusive density at Zlb
        double db = pdm[10] * MathLib.exp(g) * pd[1][0];
        double zh = 0;
        double b = 0;
        double zhm = 0;
        double hc = 0;
        double zc = 0;
        final double hc2;
        double hcc = 0;
        final double hcc2;
        double zcc = 0;
        double rc = 0;
        // Diffusive density at Alt
        outputD[1] =
            this.densu(z, db, tinf, tlb, 16., alpha[1], output.getT(), ptm[5], s, mn1, zn1, this.mesoTn1,
                this.mesoTgn1);
        if (flags.bool(flags.getSw(15)) && (z <= altl[1])) {
            // Turbopause
            zh = pdm[12];
            // Mixed density at Zlb
            b =
                this.densu(zh, db, tinf, tlb, 16.0 - xmm, (alpha[1] - 1.0), output.getT(), ptm[5], s, mn1, zn1,
                    this.mesoTn1,
                    this.mesoTgn1);
            // Mixed density at Alt
            this.dm16 =
                this.densu(z, b, tinf, tlb, xmm, 0., output.getT(), ptm[5], s, mn1, zn1, this.mesoTn1, this.mesoTgn1);
            zhm = zhm28;
            // Net density at Alt
            outputD[1] = this.dnet(output.getD(1), this.dm16, zhm, xmm, 16.);
            rl = pdm[11] * pdl[41] * (1.0 + flags.getSw(1) * pdl[23] * (input.getF107A() - 150.0));
            hc = pdm[15] * pdl[28];
            zc = pdm[14] * pdl[27];
            hc2 = pdm[15] * pdl[29];
            outputD[1] = output.getD(1) * this.ccor2(z, rl, hc, zc, hc2);

            // Chemistry correction
            hcc = pdm[17] * pdl[38];
            zcc = pdm[16] * pdl[37];
            rc = pdm[13] * pdl[39];
            // Net density corrected at Alt
            outputD[1] *= this.ccor(z, rc, hcc, zcc);
        }

        // **** O2 DENSITY ****

        // Density variation factor at Zlb
        g = flags.getSw(21) * this.globe7(pd[4], input, flags);
        // Diffusive density at Zlb
        db = pdm[30] * MathLib.exp(g) * pd[4][0];
        // Diffusive density at Alt
        outputD[3] =
            this.densu(z, db, tinf, tlb, 32., alpha[3], output.getT(), ptm[5], s, mn1, zn1,
                this.mesoTn1, this.mesoTgn1);
        if (flags.bool(flags.getSw(15))) {
            if (z <= altl[3]) {
                // Turbopause
                zh = pdm[32];
                // Mixed density at Zlb
                b =
                    this.densu(zh, db, tinf, tlb, 32. - xmm, alpha[3] - 1., output.getT(), ptm[5], s, mn1, zn1,
                        this.mesoTn1,
                        this.mesoTgn1);
                // Mixed density at Alt
                this.dm32 =
                    this.densu(z, b, tinf, tlb, xmm, 0., output.getT(), ptm[5], s, mn1, zn1, this.mesoTn1,
                        this.mesoTgn1);
                zhm = zhm28;
                // Net density at Alt
                outputD[3] = this.dnet(output.getD(3), this.dm32, zhm, xmm, 32.);
                // Correction to specified mixing ratio at ground
                rl = MathLib.log(b28 * pdm[31] / b);
                hc = pdm[35] * pdl[32];
                zc = pdm[34] * pdl[31];
                outputD[3] = output.getD(3) * this.ccor(z, rl, hc, zc);
            }
            // Correction for general departure from diffusive equilibrium above Zlb
            hcc = pdm[37] * pdl[47];
            hcc2 = pdm[37] * pdl[22];
            zcc = pdm[36] * pdl[46];
            rc = pdm[33] * pdl[48] * (1. + flags.getSw(1) * pdl[23] * (input.getF107A() - 150.));
            // Net density corrected at Alt
            outputD[3] *= this.ccor2(z, rc, hcc, zcc, hcc2);
        }

        // **** AR DENSITY ****

        // Density variation factor at Zlb
        g = flags.getSw(20) * this.globe7(pd[5], input, flags);
        // Diffusive density at Zlb
        db = pdm[40] * MathLib.exp(g) * pd[5][0];
        // Diffusive density at Alt
        outputD[4] =
            this.densu(z, db, tinf, tlb, 40., alpha[4], output.getT(), ptm[5], s, mn1, zn1,
                this.mesoTn1, this.mesoTgn1);
        if (flags.bool(flags.getSw(15)) && (z <= altl[4])) {
            // Turbopause
            zh = pdm[42];
            // Mixed density at Zlb
            b =
                this.densu(zh, db, tinf, tlb, 40. - xmm, alpha[4] - 1., output.getT(), ptm[5], s, mn1, zn1,
                    this.mesoTn1,
                    this.mesoTgn1);
            // Mixed density at Alt
            this.dm40 =
                this.densu(z, b, tinf, tlb, xmm, 0., output.getT(), ptm[5], s, mn1, zn1, this.mesoTn1, this.mesoTgn1);
            zhm = zhm28;
            // Net density at Alt
            outputD[4] = this.dnet(output.getD(4), this.dm40, zhm, xmm, 40.);
            // Correction to specified mixing ratio at ground
            rl = MathLib.log(b28 * pdm[41] / b);
            hc = pdm[45] * pdl[34];
            zc = pdm[44] * pdl[33];
            // Net density corrected at Alt
            outputD[4] *= this.ccor(z, rl, hc, zc);
        }

        // **** HYDROGEN DENSITY ****

        // Density variation factor at Zlb
        final double g1 = flags.getSw(21) * this.globe7(pd[6], input, flags);
        // Diffusive density at Zlb
        final double db01 = pdm[50] * MathLib.exp(g1) * pd[6][0];
        // Diffusive density at Alt
        outputD[6] =
            this.densu(z, db01, tinf, tlb, 1., alpha[6], output.getT(), ptm[5], s, mn1, zn1, this.mesoTn1,
                this.mesoTgn1);
        if (flags.bool(flags.getSw(15)) && (z <= altl[6])) {
            // Turbopause
            final double zh01 = pdm[52];
            // Mixed density at Zlb
            final double b01 =
                this.densu(zh01, db01, tinf, tlb, 1. - xmm, alpha[6] - 1., output.getT(), ptm[5], s, mn1,
                    zn1, this.mesoTn1, this.mesoTgn1);
            // Mixed density at Alt
            this.dm01 =
                this.densu(z, b01, tinf, tlb, xmm, 0., output.getT(), ptm[5], s, mn1, zn1, this.mesoTn1, this.mesoTgn1);
            final double zhm01 = zhm28;
            // Net density at Alt
            outputD[6] = this.dnet(output.getD(6), this.dm01, zhm01, xmm, 1.);
            // Correction to specified mixing ratio at ground
            rl = MathLib.log(b28 * pdm[51] * MathLib.abs(pdl[42]) / b01);
            final double hc01 = pdm[55] * pdl[36];
            final double zc01 = pdm[54] * pdl[35];
            outputD[6] = output.getD(6) * this.ccor(z, rl, hc01, zc01);
            // Chemistry correction
            final double hcc01 = pdm[57] * pdl[44];
            final double zcc01 = pdm[56] * pdl[43];
            final double rc01 = pdm[53] * pdl[45];
            // Net density corrected at Alt
            outputD[6] *= this.ccor(z, rc01, hcc01, zcc01);
        }

        // **** ATOMIC NITROGEN DENSITY ****

        // Density variation factor at Zlb
        final double g14 = flags.getSw(21) * this.globe7(pd[7], input, flags);
        // Diffusive density at Zlb
        final double db14 = pdm[60] * MathLib.exp(g14) * pd[7][0];
        // Diffusive density at Alt
        outputD[7] =
            this.densu(z, db14, tinf, tlb, 14., alpha[7], output.getT(), ptm[5], s, mn1, zn1, this.mesoTn1,
                this.mesoTgn1);
        if (flags.bool(flags.getSw(15)) && (z <= altl[7])) {
            // Turbopause
            final double zh14 = pdm[62];
            // Mixed density at Zlb
            final double b14 =
                this.densu(zh14, db14, tinf, tlb, 14. - xmm, alpha[7] - 1., output.getT(), ptm[5], s, mn1,
                    zn1, this.mesoTn1, this.mesoTgn1);
            // Mixed density at Alt
            this.dm14 =
                this.densu(z, b14, tinf, tlb, xmm, 0., output.getT(), ptm[5], s, mn1, zn1, this.mesoTn1, this.mesoTgn1);
            final double zhm14 = zhm28;
            // Net density at Alt
            outputD[7] = this.dnet(output.getD(7), this.dm14, zhm14, xmm, 14.);
            // Correction to specified mixing ratio at ground
            rl = MathLib.log(b28 * pdm[61] * MathLib.abs(pdl[2]) / b14);
            final double hc14 = pdm[65] * pdl[1];
            final double zc14 = pdm[64] * pdl[0];
            outputD[7] = output.getD(7) * this.ccor(z, rl, hc14, zc14);
            // Chemistry correction
            final double hcc14 = pdm[67] * pdl[4];
            final double zcc14 = pdm[66] * pdl[3];
            final double rc14 = pdm[63] * pdl[5];
            // Net density corrected at Alt
            outputD[7] *= this.ccor(z, rc14, hcc14, zcc14);
        }

        // **** Anomalous OXYGEN DENSITY ****

        final double g16h = flags.getSw(21) * this.globe7(pd[8], input, flags);
        final double db16h = pdm[70] * MathLib.exp(g16h) * pd[8][0];
        final double tho = pdm[79] * pdl[6];
        dd =
            this.densu(z, db16h, tho, tho, 16., alpha[8], output.getT(), ptm[5], s, mn1, zn1, this.mesoTn1,
                this.mesoTgn1);
        final double zsht = pdm[75];
        final double zmho = pdm[74];
        final double zsho = this.scalh(zmho, 16.0, tho);
        outputD[8] = dd * MathLib.exp(-zsht / zsho * (MathLib.exp(-(z - zmho) / zsht) - 1.));

        // total mass density
        outputD[5] = 1.66E-24 * (4.0 * outputD[0] + 16.0 * outputD[1] + 28.0 * outputD[2] + 32.0 * outputD[3]
                + 40.0 * outputD[4] + outputD[6] + 14.0 * outputD[7]);

        // Temperature
        z = MathLib.abs(input.getAlt());
        this.densu(z, 1.0, tinf, tlb, 0.0, 0.0, output.getT(), ptm[5], s, mn1, zn1, this.mesoTn1, this.mesoTgn1);
        if (flags.bool(flags.getSw(0))) {
            for (int i = 8; i >= 0; i--) {
                outputD[i] *= 1.0E6;
            }
            outputD[5] *= 0.001;
        }
    }

    /**
     * scalh.
     * 
     * @param alt
     *        altitude
     * @param xm
     *        xm
     * @param temp
     *        temperature
     * @return scalh
     */
    private double scalh(final double alt, final double xm, final double temp) {
        return RGAS * temp * MathLib.pow(1.0 + alt / this.re, 2) / (this.gsurf * xm);
    }

    /**
     * go.<br>
     * Eq. A24d
     * 
     * @param a
     *        a
     * @param p
     *        p
     * @return g0
     */
    private double g00(final double a, final double[] p) {
        final double localG0cacheKey = a + p[25] + p[24];

        if (localG0cacheKey != this.g0cacheKey) {
            final double absP24 = MathLib.abs(p[24]);
            this.g0Value = (a - 4.0 + (p[25] - 1.0) * (a - 4.0 + (MathLib.exp(-absP24 * (a - 4.0)) - 1.0) / absP24));
            this.g0cacheKey = localG0cacheKey;
        }
        return this.g0Value;
    }

    /**
     * sg0<br>
     * Eq. A24a
     * 
     * @param ex
     *        ex
     * @param p
     *        p
     * @param ap
     *        ap
     * @return sg0
     */
    private double sg0(final double ex, final double[] p, final double[] ap) {
        // Cache for computation speed-up
        if (this.p24 != p[24] || this.p25 != p[25] || ex != this.oldEx) {
            // Invalidate cache
            this.p24 = p[24];
            this.p25 = p[25];
            this.oldEx = ex;

            // Power data are optimed
            // SInce this method is very time consuming
            final double exp05 = MathLib.sqrt(ex);
            final double exp2 = ex * ex;
            final double exp3 = ex * exp2;
            final double exp4 = ex * exp3;
            final double exp8 = exp4 * exp4;
            final double exp12 = exp4 * exp8;
            final double exp19 = exp12 * exp4 * exp3;
            final double sumex = 1.0 + (1.0 - exp19) / (1.0 - ex) * exp05;
            this.oldSg0 =
                (this.g00(ap[1], p)
                        + (this.g00(ap[2], p) * ex + this.g00(ap[3], p) * exp2 + this.g00(ap[4], p) * exp3 + (this
                    .g00(ap[5], p) * exp4 + this.g00(ap[6],
                    p) * exp12)
                        * (1.0 - exp8) / (1.0 - ex)))
                        / sumex;
        }
        return this.oldSg0;
    }

    /**
     * spline.<br>
     * Calculate 2nd derivatives of cubic spline interpolation function
     * adapted from numerical recipes by press et al.
     * 
     * @param x
     *        array of abscissa in ascending order by x
     * @param y
     *        array of tabulated function in ascending order by x
     * @param n
     *        size of arrays x,y
     * @param yp1
     *        specified derivative at x[0]; values >= 1E30 signal second
     *        derivative zero
     * @param ypn
     *        specified derivative at x[n-1]; values >= 1E30 signal second
     *        derivative zero
     * @param y2
     *        output array of second derivatives
     */
    protected void spline(final double[] x, final double[] y, final int n, final double yp1, final double ypn,
            final double[] y2) {

        // Initializations
        double sig;
        double p;
        final double qn;
        final double un;
        final double[] u = new double[n];

        // Set the first value of y2 and u depending on yp1
        if (yp1 > 0.99E30) {
            // zero
            y2[0] = 0;
            u[0] = 0;
        } else {
            y2[0] = -0.5;
            // compute u[0] from x, y and yp1
            u[0] = (3.0 / (x[1] - x[0])) * ((y[1] - y[0]) / (x[1] - x[0]) - yp1);
        }
        // Complete u and y2 except for the first index
        for (int i = 1; i < (n - 1); i++) {
            sig = (x[i] - x[i - 1]) / (x[i + 1] - x[i - 1]);
            p = sig * y2[i - 1] + 2.0;
            y2[i] = (sig - 1.0) / p;
            u[i] = (6.0 * ((y[i + 1] - y[i]) / (x[i + 1] - x[i]) - (y[i] - y[i - 1]) / (x[i] - x[i - 1]))
                    / (x[i + 1] - x[i - 1]) - sig
                    * u[i - 1])
                    / p;
        }
        // Set the value of qn and un depending on ypn
        if (ypn > 0.99E30) {
            qn = 0;
            un = 0;
        } else {
            qn = 0.5;
            un = (3.0 / (x[n - 1] - x[n - 2])) * (ypn - (y[n - 1] - y[n - 2]) / (x[n - 1] - x[n - 2]));
        }
        // Set last value of y2
        y2[n - 1] = (un - qn * u[n - 2]) / (qn * y2[n - 2] + 1.0);
        // Update all values of y2 except the last one
        for (int k = n - 2; k >= 0; k--) {
            y2[k] = y2[k] * y2[k + 1] + u[k];
        }
    }

    /**
     * splini integrate cubic spline function from xa(1) to x.
     * 
     * @param xa
     *        array of abscissa in ascending order by x
     * @param ya
     *        array of tabulated function in ascending order by x
     * @param y2a
     *        array of second derivatives
     * @param n
     *        size of arrays xa,ya,y2a
     * @param x
     *        abscissa end point for integration
     * @return yi
     */
    private double splini(final double[] xa, final double[] ya, final double[] y2a, final int n, final double x) {
        // Initializations
        int klo = 0;
        int khi = 1;
        // Local variables declarations
        double xx;
        double h;
        double h1;
        double a;
        double b;
        double a2;
        double b2;
        double yi = 0;
        // Loop on xa indexes n-1 times except if  the abscissa end point for integration
        // has been reached
        while ((x > xa[klo]) && (khi < n)) {
            xx = x;
            if (khi < (n - 1)) {
                xx = (x < xa[khi] ? x : xa[khi]);
            }
            h = xa[khi] - xa[klo];
            h1 = 1 / h;
            a = (xa[khi] - xx) * h1;
            b = (xx - xa[klo]) * h1;
            a2 = a * a;
            b2 = b * b;
            // Compute result 
            yi += ((1.0 - a2) * ya[klo] / 2.0 + b2 * ya[khi] / 2.0 +
                    ((-(1.0 + a2 * a2) / 4.0 + a2 / 2.0) * y2a[klo] + (b2
                            * b2 / 4.0 - b2 / 2.0)
                            * y2a[khi])
                            * h * h / 6.0)
                    * h;
            // Next step
            klo++;
            khi++;
        }
        return yi;
    }

    /**
     * splint calculate cubic spline interpolation value adapted from numerical recipes by press et al.
     * 
     * @param xa
     *        array of abscissa in ascending order by x
     * @param ya
     *        array of tabulated function in ascending order by x
     * @param y2a
     *        array of second derivatives
     * @param n
     *        size of arrays xa,ya,y2a
     * @param x
     *        abscissa for interpolation
     * @return splint
     */
    protected double splint(final double[] xa, final double[] ya, final double[] y2a, final int n, final double x) {
        // Initialization
        int klo = 0;
        int khi = n - 1;
        int k;
        // Loop to set the indexes khi and klo
        while ((khi - klo) > 1) {
            k = (khi + klo) >> 1;
            if (xa[k] > x) {
                khi = k;
            } else {
                klo = k;
            }
        }
        // Compute intermediate values for splint computation from the indexes
        final double h = xa[khi] - xa[klo];
        final double h1 = 1 / h;
        final double a = (xa[khi] - x) * h1;
        final double b = (x - xa[klo]) * h1;
        // Compute splint
        return a * ya[klo] + b * ya[khi] + ((a * a * a - a) * y2a[klo] + (b * b * b - b) * y2a[khi]) * h * h
                / 6.0;
    }

    /**
     * zeta.
     * 
     * @param zz
     *        zz
     * @param zl
     *        zl
     * @return zeta
     */
    private double zeta(final double zz, final double zl) {
        return ((zz - zl) * (this.re + zl) / (this.re + zz));
    }

    // ------------------------- MAIN METHODS ------------------------

    /**
     * gtd7d.<br>
     * This subroutine provides Effective Total Mass Density for output
     * d[5] which includes contributions from "anomalous oxygen" which can
     * affect satellite drag above 500 km. See the section "output" for
     * additional details.
     * 
     * @param input
     *        input
     * @param flags
     *        flags
     * @param output
     *        output
     */
    public void gtd7d(final Input input, final Flags flags, final Output output) {
        this.gtd7(input, flags, output);
        final double density = 1.66E-24 * (4.0 * output.getD(0) + 16.0 * output.getD(1) + 28.0 * output.getD(2) + 32.0
                * output.getD(3)
                + 40.0 * output.getD(4) + output.getD(6) + 14.0 * output.getD(7) + 16.0 * output.getD(8));
        output.setD(5, density);
    }
}
