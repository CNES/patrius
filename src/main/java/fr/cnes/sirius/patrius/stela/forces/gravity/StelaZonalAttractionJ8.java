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
 * @history created on 26/02/2015
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:315:26/02/2015:add zonal terms J8 to J15
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.stela.forces.gravity;

import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.stela.forces.AbstractStelaLagrangeContribution;
import fr.cnes.sirius.patrius.stela.orbits.StelaEquinoctialOrbit;

//CHECKSTYLE:OFF

/**
 * <p>
 * Class computing 8th order zonal perturbations. This class has package visibility since it is not supposed to be used
 * stand-alone (use {@link StelaZonalAttraction} instead). This code has been set aside since generated code is too long
 * to be contained in {@link StelaZonalAttraction}.
 * </p>
 * <p>
 * The class is adapted from STELA EarthPotentialGTO8 in fr.cnes.los.stela.elib.business.implementation.earthPotential
 * </p>
 * 
 * @concurrency thread-safe
 * @author Emmanuel Bignon
 * @version $Id$
 * @since 3.0
 * 
 */
class StelaZonalAttractionJ8 extends AbstractStelaLagrangeContribution {

    /** Serial UID. */
    private static final long serialVersionUID = 843557526132039375L;

    /** The central body reference radius (m). */
    private final double rEq;
    /** The 8th order central body coefficients */
    private final double j8;

    /** Temporary coefficients. */
    private double t2;
    private double t3;
    private double t4;
    private double t5;
    private double t6;
    private double t7;
    private double t8;
    private double t9;
    private double t10;
    private double t11;
    private double t12;
    private double t13;
    private double t14;
    private double t15;
    private double t16;
    private double t17;
    private double t18;
    private double t19;
    private double t22;
    private double t23;
    private double t26;
    private double t27;
    private double t28;
    private double t29;
    private double t30;
    private double t31;
    private double t32;
    private double t33;
    private double t36;
    private double t37;
    private double t40;
    private double t41;
    private double t42;
    private double t43;
    private double t44;
    private double t45;
    private double t46;
    private double t49;
    private double t50;
    private double t53;
    private double t54;
    private double t57;
    private double t58;
    private double t59;
    private double t62;
    private double t65;
    private double t68;
    private double t69;
    private double t70;
    private double t73;
    private double t76;
    private double t81;
    private double t82;
    private double t83;
    private double t84;
    private double t87;
    private double t90;
    private double t93;
    private double t96;
    private double t107;
    private double t108;
    private double t109;
    private double t112;
    private double t114;
    private double t116;
    private double t119;
    private double t121;
    private double t123;
    private double t124;
    private double t126;
    private double t128;
    private double t130;
    private double t131;
    private double t136;
    private double t141;
    private double t143;
    private double t144;
    private double t146;
    private double t148;
    private double t151;
    private double t156;
    private double t163;
    private double t164;
    private double t167;
    private double t168;
    private double t171;
    private double t172;
    private double t175;
    private double t176;
    private double t179;
    private double t180;
    private double t183;
    private double t192;
    private double t193;
    private double t196;
    private double t197;
    private double t200;
    private double t201;
    private double t204;
    private double t205;
    private double t210;
    private double t211;
    private double t218;
    private double t219;
    private double t220;
    private double t221;
    private double t222;
    private double t225;
    private double t226;
    private double t227;
    private double t228;
    private double t229;
    private double t230;
    private double t233;
    private double t234;
    private double t235;
    private double t238;
    private double t239;
    private double t240;
    private double t243;
    private double t244;
    private double t245;
    private double t248;
    private double t249;
    private double t250;
    private double t253;
    private double t254;
    private double t255;
    private double t258;
    private double t259;
    private double t260;
    private double t263;
    private double t264;
    private double t265;
    private double t272;
    private double t275;
    private double t277;
    private double t280;
    private double t283;
    private double t286;
    private double t289;
    private double t292;
    private double t295;
    private double t298;
    private double t301;
    private double t304;
    private double t307;
    private double t313;
    private double t316;
    private double t317;
    private double t320;
    private double t326;
    private double t329;
    private double t332;
    private double t335;
    private double t338;
    private double t341;
    private double t344;
    private double t347;
    private double t350;
    private double t353;
    private double t356;
    private double t359;
    private double t360;
    private double t361;
    private double t362;
    private double t366;
    private double t367;
    private double t369;
    private double t371;
    private double t373;
    private double t377;
    private double t378;
    private double t381;
    private double t382;
    private double t389;
    private double t390;
    private double t393;
    private double t405;
    private double t414;
    private double t432;
    private double t435;
    private double t440;
    private double t443;
    private double t454;
    private double t460;
    private double t461;
    private double t464;
    private double t467;
    private double t474;
    private double t477;
    private double t481;
    private double t490;
    private double t509;
    private double t512;
    private double t517;
    private double t522;
    private double t526;
    private double t529;
    private double t536;
    private double t563;
    private double t593;
    private double t600;
    private double t602;
    private double t607;
    private double t615;
    private double t624;
    private double t635;
    private double t648;
    private double t657;
    private double t662;
    private double t667;
    private double t672;
    private double t673;
    private double t675;
    private double t681;
    private double t684;
    private double t687;
    private double t701;
    private double t705;
    private double t709;
    private double t713;
    private double t717;
    private double t726;
    private double t734;
    private double t739;
    private double t742;
    private double t745;
    private double t748;
    private double t759;
    private double t762;
    private double t763;
    private double t766;
    private double t772;
    private double t773;
    private double t780;
    private double t785;
    private double t798;
    private double t801;
    private double t806;
    private double t807;
    private double t810;
    private double t811;
    private double t814;
    private double t815;
    private double t818;
    private double t819;
    private double t822;
    private double t823;
    private double t826;
    private double t827;
    private double t832;
    private double t835;
    private double t838;
    private double t841;
    private double t850;
    private double t853;
    private double t856;
    private double t860;
    private double t863;
    private double t868;
    private double t886;
    private double t891;
    private double t904;
    private double t923;
    private double t937;
    private double t948;
    private double t965;
    private double t996;
    private double t1009;
    private double t1014;
    private double t1017;
    private double t1026;
    private double t1032;
    private double t1035;
    private double t1040;
    private double t1041;
    private double t1044;
    private double t1047;
    private double t1052;
    private double t1057;
    private double t1062;
    private double t1067;
    private double t1075;
    private double t1088;
    private double t1094;
    private double t1133;
    private double t1141;
    private double t1147;
    private double t1159;
    private double t1172;
    private double t1187;
    private double t1204;
    private double t1212;
    private double t1215;
    private double t1226;
    private double t1231;
    private double t1239;
    private double t1243;
    private double t1266;
    private double t1270;
    private double t1274;
    private double t1277;
    private double t1279;
    private double t1306;
    private double t1311;
    private double t1315;
    private double t1328;
    private double t1335;
    private double t1346;
    private double t1347;
    private double t1358;
    private double t1368;
    private double t1376;
    private double t1378;
    private double t1380;
    private double t1391;
    private double t1409;
    private double t1413;
    private double t1414;
    private double t1446;
    private double t1455;
    private double t1458;
    private double t1473;
    private double t1480;
    private double t1495;
    private double t1500;
    private double t1503;
    private double t1512;
    private double t1514;
    private double t1517;
    private double t1524;
    private double t1545;
    private double t1554;
    private double t1569;
    private double t1575;
    private double t1583;
    private double t1592;
    private double t1595;
    private double t1598;
    private double t1601;
    private double t1604;
    private double t1607;
    private double t1612;
    private double t1617;
    private double t1622;
    private double t1626;
    private double t1630;
    private double t1633;
    private double t1635;
    private double t1642;
    private double t1659;
    private double t1662;
    private double t1665;
    private double t1668;
    private double t1671;
    private double t1673;
    private double t1676;
    private double t1703;
    private double t1720;
    private double t1748;
    private double t1770;
    private double t1781;
    private double t1782;
    private double t1811;
    private double t1829;
    private double t1842;
    private double t1846;
    private double t1854;
    private double t1856;
    private double t1859;
    private double t1868;
    private double t1875;
    private double t1882;
    private double t1912;
    private double t1917;
    private double t1918;
    private double t1921;
    private double t1926;
    private double t1931;
    private double t1934;
    private double t1939;
    private double t1954;
    private double t1985;
    private double t2014;
    private double t2038;
    private double t2052;
    private double t2057;
    private double t2060;
    private double t2072;
    private double t2077;
    private double t2086;
    private double t2088;
    private double t2103;
    private double t2106;
    private double t2119;
    private double t2149;

    /**
     * Constructor
     * 
     * @param rEq
     *        equatorial radius (m)
     * @param j8
     *        8th order central body coefficient
     */
    public StelaZonalAttractionJ8(final double rEq, final double j8) {
        this.rEq = rEq;
        this.j8 = j8;
    }

    @Override
    public double[] computePerturbation(final StelaEquinoctialOrbit orbit) {

        // Orbital elements
        final double a = orbit.getA();
        final double ex = orbit.getEquinoctialEx();
        final double ey = orbit.getEquinoctialEy();
        final double ix = orbit.getIx();
        final double iy = orbit.getIy();
        final double mu = orbit.getMu();

        // Code split in sub-methods to save computation time
        this.derParUdeg8_1(a, ex, ey, ix, iy, mu);
        this.derParUdeg8_2(a, ex, ey, ix, iy);
        this.derParUdeg8_3(a, ex, ey, ix, iy);
        this.derParUdeg8_4(a, ex, ey, ix, iy);

        // Final result
        final double[] dPot = new double[6];

        dPot[0] = 0.63e2 / 0.2048e4 * this.t5 * this.t359 / this.t362 / this.t360 * this.t373;
        dPot[1] = 0.0e0;
        dPot[2] =
            -0.7e1 / 0.2048e4 * this.t5
                * (this.t414 + this.t454 + this.t490 + this.t526 + this.t563 + this.t593 + this.t624 + this.t662)
                * this.t667 * this.t373
                - 0.105e3 / 0.2048e4 * this.t5 * this.t672 * this.t675 * ex;
        dPot[3] =
            -0.7e1 / 0.2048e4 * this.t5
                * (this.t726 + this.t766 + this.t801 + this.t850 + this.t891 + this.t923 + this.t965 + this.t996)
                * this.t667 * this.t373
                - 0.105e3 / 0.2048e4 * this.t5 * this.t672 * this.t675 * ey;
        dPot[4] =
            -0.7e1
                / 0.2048e4
                * this.t5
                * (this.t1044 + this.t1088 + this.t1133 + this.t1172 + this.t1204 + this.t1239 + this.t1277
                    + this.t1311 + this.t1346 + this.t1378 + this.t1413 + this.t1446
                    + this.t1480 + this.t1512 + this.t1545 + this.t1583) * this.t667 * this.t373;
        dPot[5] =
            -0.7e1
                / 0.2048e4
                * this.t5
                * (this.t1633 + this.t1671 + this.t1703 + this.t1748 + this.t1781 + this.t1811 + this.t1846
                    + this.t1882 + this.t1917 + this.t1954 + this.t1985 + this.t2014
                    + this.t2052 + this.t2086 + this.t2119 + this.t2149) * this.t667 * this.t373;

        return dPot;
    }

    @Override
    public double[] computeShortPeriods(final StelaEquinoctialOrbit orbit) {
        // Not available
        return new double[6];
    }

    @Override
    public double[][] computePartialDerivatives(final StelaEquinoctialOrbit orbit) {
        // Not available
        return new double[6][6];
    }

    /**
     * Partial derivative due to 8th order Earth potential zonal harmonics.
     * 
     * @param a
     *        semi-major axis.
     * @param ex
     *        x eccentricity.
     * @param ey
     *        y eccentricity.
     * @param ix
     *        x inclination.
     * @param iy
     *        y inclination.
     * @param mu
     *        MU
     */
    private void derParUdeg8_1(final double a, final double ex, final double ey, final double ix, final double iy,
                               final double mu) {

        this.t2 = this.rEq * this.rEq;
        this.t3 = this.t2 * this.t2;
        this.t4 = this.t3 * this.t3;
        this.t5 = mu * this.j8 * this.t4;
        this.t6 = ex * ex;
        this.t7 = ey * ey;
        this.t8 = this.t6 + this.t7;
        this.t9 = this.t8 * this.t8;
        this.t10 = ix * ix;
        this.t11 = iy * iy;
        this.t12 = 0.1e1 - this.t10 - this.t11;
        this.t13 = this.t12 * this.t12;
        this.t14 = this.t9 * this.t13;
        this.t15 = this.t10 + this.t11;
        this.t16 = this.t14 * this.t15;
        this.t17 = ex * ix;
        this.t18 = ey * iy;
        this.t19 = this.t17 * this.t18;
        this.t22 = this.t8 * this.t13;
        this.t23 = this.t22 * this.t15;
        this.t26 = this.t13 * this.t13;
        this.t27 = this.t8 * this.t26;
        this.t28 = this.t15 * this.t15;
        this.t29 = this.t27 * this.t28;
        this.t30 = this.t7 * ey;
        this.t31 = this.t11 * iy;
        this.t32 = this.t30 * this.t31;
        this.t33 = this.t17 * this.t32;
        this.t36 = this.t28 * this.t15;
        this.t37 = this.t27 * this.t36;
        this.t40 = this.t13 * this.t12;
        this.t41 = this.t8 * this.t40;
        this.t42 = this.t41 * this.t15;
        this.t43 = this.t6 * ex;
        this.t44 = this.t10 * ix;
        this.t45 = this.t43 * this.t44;
        this.t46 = this.t45 * this.t18;
        this.t49 = this.t9 * this.t26;
        this.t50 = this.t49 * this.t36;
        this.t53 = this.t8 * this.t12;
        this.t54 = this.t6 * this.t10;
        this.t57 = this.t6 * this.t6;
        this.t58 = this.t10 * this.t10;
        this.t59 = this.t57 * this.t58;
        this.t62 = this.t9 * this.t12;
        this.t65 = this.t7 * this.t11;
        this.t68 = this.t7 * this.t7;
        this.t69 = this.t11 * this.t11;
        this.t70 = this.t68 * this.t69;
        this.t73 =
            0.80e2 - 0.1164240e7 * this.t16 * this.t19 - 0.5544000e7 * this.t23 * this.t19 + 0.22239360e8 * this.t29
                * this.t33 - 0.86486400e8
                * this.t37 * this.t19 - 0.8072064e7 * this.t42 * this.t46 - 0.20386080e8 * this.t50 * this.t19
                + 0.100800e6 * this.t53 * this.t54 + 0.166320e6
                * this.t22 * this.t59 + 0.18900e5 * this.t62 * this.t54 + 0.100800e6 * this.t53 * this.t65 + 0.166320e6
                * this.t22 * this.t70;
        this.t76 = this.t26 * this.t36;
        this.t81 = this.t26 * this.t15;
        this.t82 = this.t57 * this.t6;
        this.t83 = this.t58 * this.t10;
        this.t84 = this.t82 * this.t83;
        this.t87 = this.t26 * this.t28;
        this.t90 = this.t13 * this.t15;
        this.t93 = this.t40 * this.t28;
        this.t96 = this.t40 * this.t15;
        this.t107 = this.t68 * this.t7;
        this.t108 = this.t69 * this.t11;
        this.t109 = this.t107 * this.t108;
        this.t112 =
            0.18900e5 * this.t62 * this.t65 - 0.17297280e8 * this.t76 * this.t65 - 0.17297280e8 * this.t76 * this.t54
                - 0.823680e6 * this.t81 * this.t84
                + 0.14414400e8 * this.t87 * this.t59 - 0.1330560e7 * this.t90 * this.t54 + 0.8648640e7 * this.t93
                * this.t54 - 0.5765760e7 * this.t96
                * this.t59 + 0.14414400e8 * this.t87 * this.t70 - 0.5765760e7 * this.t96 * this.t70 + 0.8648640e7
                * this.t93 * this.t65 - 0.1330560e7
                * this.t90 * this.t65 - 0.823680e6 * this.t81 * this.t109;
        this.t114 = this.t13 * this.t28;
        this.t116 = this.t28 * this.t28;
        this.t119 = this.t40 * this.t36;
        this.t121 = this.t12 * this.t15;
        this.t123 = this.t9 * this.t40;
        this.t124 = this.t123 * this.t36;
        this.t126 = this.t22 * this.t28;
        this.t128 = this.t53 * this.t15;
        this.t130 = this.t9 * this.t8;
        this.t131 = this.t130 * this.t13;
        this.t136 = this.t130 * this.t26;
        this.t141 = this.t62 * this.t15;
        this.t143 =
            0.95040e5 * this.t114 + 0.1029600e7 * this.t26 * this.t116 - 0.549120e6 * this.t119 - 0.5760e4 * this.t121
                - 0.15135120e8 * this.t124
                + 0.1663200e7 * this.t126 - 0.90720e5 * this.t128 + 0.436590e6 * this.t131 * this.t28 + 0.29729700e8
                * this.t49 * this.t116
                + 0.5521230e7 * this.t136 * this.t116 + 0.19459440e8 * this.t27 * this.t116 - 0.126000e6 * this.t141;
        this.t144 = this.t14 * this.t28;
        this.t146 = this.t41 * this.t36;
        this.t148 = this.t41 * this.t28;
        this.t151 = this.t54 * this.t65;
        this.t156 = this.t123 * this.t28;
        this.t163 = this.t28 * this.t6;
        this.t164 = this.t163 * this.t10;
        this.t167 = this.t15 * this.t7;
        this.t168 = this.t167 * this.t11;
        this.t171 = this.t36 * this.t6;
        this.t172 = this.t171 * this.t10;
        this.t175 = this.t15 * this.t6;
        this.t176 = this.t175 * this.t10;
        this.t179 = this.t15 * this.t57;
        this.t180 = this.t179 * this.t58;
        this.t183 =
            0.2425500e7 * this.t144 - 0.10090080e8 * this.t146 + 0.40360320e8 * this.t148 * this.t19 - 0.12108096e8
                * this.t42 * this.t151
                - 0.8072064e7 * this.t42 * this.t33 + 0.9081072e7 * this.t156 * this.t19 + 0.22239360e8 * this.t29
                * this.t46 + 0.33359040e8 * this.t29
                * this.t151 + 0.20180160e8 * this.t41 * this.t164 - 0.582120e6 * this.t14 * this.t168 - 0.10193040e8
                * this.t49 * this.t172 - 0.2772000e7
                * this.t22 * this.t176 - 0.2018016e7 * this.t41 * this.t180;
        this.t192 = this.t28 * this.t57;
        this.t193 = this.t192 * this.t58;
        this.t196 = this.t28 * this.t68;
        this.t197 = this.t196 * this.t69;
        this.t200 = this.t36 * this.t7;
        this.t201 = this.t200 * this.t11;
        this.t204 = this.t28 * this.t7;
        this.t205 = this.t204 * this.t11;
        this.t210 = this.t15 * this.t68;
        this.t211 = this.t210 * this.t69;
        this.t218 = this.t40 * ex;
        this.t219 = this.t68 * ey;
        this.t220 = ix * this.t219;
        this.t221 = this.t69 * iy;
        this.t222 = this.t220 * this.t221;
        this.t225 =
            0.4540536e7 * this.t123 * this.t164 - 0.582120e6 * this.t14 * this.t176 - 0.43243200e8 * this.t27
                * this.t172 + 0.5559840e7 * this.t27
                * this.t193 + 0.5559840e7 * this.t27 * this.t197 - 0.10193040e8 * this.t49 * this.t201 + 0.20180160e8
                * this.t41 * this.t205 - 0.2772000e7
                * this.t22 * this.t168 - 0.2018016e7 * this.t41 * this.t211 + 0.4540536e7 * this.t123 * this.t205
                - 0.43243200e8 * this.t27 * this.t201
                + 0.1153152e7 * this.t218 * this.t222;
        this.t226 = this.t57 * ex;
        this.t227 = this.t40 * this.t226;
        this.t228 = this.t58 * ix;
        this.t229 = this.t228 * ey;
        this.t230 = this.t229 * iy;
    }

    /**
     * Partial derivative due to 8th order Earth potential zonal harmonics.
     * 
     * @param a
     *        semi-major axis.
     * @param ex
     *        x eccentricity.
     * @param ey
     *        y eccentricity.
     * @param ix
     *        x inclination.
     * @param iy
     *        y inclination.
     */
    private final void
            derParUdeg8_2(final double a, final double ex, final double ey, final double ix, final double iy) {

        this.t233 = this.t40 * this.t57;
        this.t234 = this.t58 * this.t7;
        this.t235 = this.t234 * this.t11;
        this.t238 = this.t40 * this.t43;
        this.t239 = this.t44 * this.t30;
        this.t240 = this.t239 * this.t31;
        this.t243 = this.t40 * this.t6;
        this.t244 = this.t10 * this.t68;
        this.t245 = this.t244 * this.t69;
        this.t248 = this.t13 * ex;
        this.t249 = ix * this.t30;
        this.t250 = this.t249 * this.t31;
        this.t253 = this.t13 * this.t6;
        this.t254 = this.t10 * this.t7;
        this.t255 = this.t254 * this.t11;
        this.t258 = this.t13 * this.t43;
        this.t259 = this.t44 * ey;
        this.t260 = this.t259 * iy;
        this.t263 = this.t12 * ex;
        this.t264 = ix * ey;
        this.t265 = this.t264 * iy;
        this.t272 = this.t12 * this.t6;
        this.t275 =
            0.1153152e7 * this.t227 * this.t230 + 0.2882880e7 * this.t233 * this.t235 + 0.3843840e7 * this.t238
                * this.t240 + 0.2882880e7 * this.t243
                * this.t245 + 0.2217600e7 * this.t248 * this.t250 + 0.3326400e7 * this.t253 * this.t255 + 0.2217600e7
                * this.t258 * this.t260 + 0.120960e6
                * this.t263 * this.t265 + 0.175e3 * this.t130 + 0.840e3 * this.t6 + 0.840e3 * this.t7 + 0.1050e4
                * this.t9 + 0.60480e5 * this.t272 * this.t10;
        this.t277 = this.t40 * this.t82;
        this.t280 = this.t13 * this.t68;
        this.t283 = this.t12 * this.t7;
        this.t286 = this.t13 * this.t57;
        this.t289 = this.t40 * this.t107;
        this.t292 = this.t130 * this.t40;
        this.t295 = this.t130 * this.t12;
        this.t298 = this.t53 * ex;
        this.t301 = this.t22 * this.t43;
        this.t304 = this.t22 * this.t6;
        this.t307 = this.t22 * ex;
        this.t313 = this.t93 * ex;
        this.t316 =
            0.192192e6 * this.t277 * this.t83 + 0.554400e6 * this.t280 * this.t69 + 0.60480e5 * this.t283 * this.t11
                + 0.554400e6 * this.t286 * this.t58
                + 0.192192e6 * this.t289 * this.t108 - 0.2774772e7 * this.t292 * this.t36 - 0.22050e5 * this.t295
                * this.t15 + 0.201600e6 * this.t298
                * this.t265 + 0.665280e6 * this.t301 * this.t260 + 0.997920e6 * this.t304 * this.t255 + 0.665280e6
                * this.t307 * this.t250 + 0.37800e5
                * this.t62 * ex * this.t265 + 0.17297280e8 * this.t313 * this.t265;
        this.t317 = this.t96 * this.t43;
        this.t320 = this.t96 * this.t6;
        this.t326 = this.t96 * ex;
        this.t329 = this.t81 * this.t226;
        this.t332 = this.t81 * this.t57;
        this.t335 = this.t81 * this.t43;
        this.t338 = this.t81 * this.t6;
        this.t341 = this.t81 * ex;
        this.t344 = this.t87 * this.t6;
        this.t347 = this.t87 * this.t43;
        this.t350 = this.t87 * ex;
        this.t353 = this.t90 * ex;
        this.t356 =
            -0.23063040e8 * this.t317 * this.t260 - 0.34594560e8 * this.t320 * this.t255 - 0.34594560e8 * this.t76 * ex
                * this.t265 - 0.23063040e8
                * this.t326 * this.t250 - 0.4942080e7 * this.t329 * this.t230 - 0.12355200e8 * this.t332 * this.t235
                - 0.16473600e8 * this.t335 * this.t240
                - 0.12355200e8 * this.t338 * this.t245 - 0.4942080e7 * this.t341 * this.t222 + 0.86486400e8 * this.t344
                * this.t255 + 0.57657600e8
                * this.t347 * this.t260 + 0.57657600e8 * this.t350 * this.t250 - 0.2661120e7 * this.t353 * this.t265;
        this.t359 = this.t73 + this.t112 + this.t143 + this.t183 + this.t225 + this.t275 + this.t316 + this.t356;
        this.t360 = a * a;
        this.t361 = this.t360 * this.t360;
        this.t362 = this.t361 * this.t361;
        this.t366 = 0.1e1 - this.t6 - this.t7;
        this.t367 = this.t366 * this.t366;
        this.t369 = this.t367 * this.t367;
        this.t371 = MathLib.sqrt(this.t366);
        this.t373 = 0.1e1 / this.t371 / this.t369 / this.t367 / this.t366;
        this.t377 = ex * this.t10;
        this.t378 = this.t377 * this.t65;
        this.t381 = this.t44 * this.t6;
        this.t382 = this.t381 * this.t18;
        this.t389 = this.t6 * ix;
        this.t390 = this.t389 * this.t18;
        this.t393 = this.t43 * this.t10;
        this.t405 = this.t43 * this.t58;
        this.t414 =
            -0.24216192e8 * this.t42 * this.t378 + 0.66718080e8 * this.t29 * this.t382 + 0.66718080e8 * this.t29
                * this.t378 - 0.24216192e8 * this.t42
                * this.t382 - 0.81544320e8 * this.t37 * this.t390 + 0.75600e5 * this.t53 * this.t393 + 0.201600e6
                * this.t263 * this.t65 + 0.332640e6
                * this.t248 * this.t70 - 0.34594560e8 * this.t76 * this.t377 - 0.4942080e7 * this.t81 * this.t226
                * this.t83 + 0.57657600e8 * this.t87 * this.t405
                - 0.2661120e7 * this.t90 * this.t377 + 0.17297280e8 * this.t93 * this.t377 - 0.23063040e8 * this.t96
                * this.t405;
        this.t432 = this.t15 * ex;
        this.t435 = this.t36 * ex;
        this.t440 = this.t28 * ex;
        this.t443 = this.t116 * ex;
        this.t454 =
            0.40360320e8 * this.t238 * this.t28 * this.t10 - 0.5544000e7 * this.t258 * this.t15 * this.t10
                - 0.4036032e7 * this.t227 * this.t15 * this.t58
                - 0.86486400e8 * this.t43 * this.t26 * this.t36 * this.t10 + 0.11119680e8 * this.t226 * this.t26
                * this.t28 * this.t58 - 0.132300e6 * this.t62
                * this.t432 - 0.16648632e8 * this.t123 * this.t435 - 0.60540480e8 * this.t41 * this.t435 + 0.2619540e7
                * this.t14 * this.t440
                + 0.118918800e9 * this.t27 * this.t443 + 0.33127380e8 * this.t49 * this.t443 - 0.504000e6 * this.t53
                * this.t432 + 0.9702000e7 * this.t22
                * this.t440 + 0.201600e6 * this.t53 * this.t377;
        this.t460 = this.t40 * ix;
        this.t461 = this.t219 * this.t221;
        this.t464 = this.t13 * ix;
        this.t467 = this.t12 * ix;
        this.t474 = this.t43 * this.t12;
        this.t477 = this.t248 * this.t28;
        this.t481 = ex * this.t26;
        this.t490 =
            0.665280e6 * this.t22 * this.t405 + 0.37800e5 * this.t62 * this.t377 + 0.1153152e7 * this.t460 * this.t461
                + 0.2217600e7 * this.t464 * this.t32
                + 0.120960e6 * this.t467 * this.t18 + 0.1050e4 * this.t9 * ex + 0.4200e4 * this.t8 * ex + 0.201600e6
                * this.t474 * this.t10
                + 0.3326400e7 * this.t477 + 0.120960e6 * this.t263 * this.t10 + 0.38918880e8 * this.t481 * this.t116
                - 0.4656960e7 * this.t23 * this.t390
                + 0.36324288e8 * this.t148 * this.t390 + 0.9081072e7 * this.t156 * this.t265;
        this.t509 = this.t440 * this.t10;
        this.t512 = this.t432 * this.t10;
        this.t517 = this.t435 * this.t10;
        this.t522 = this.t15 * this.t43;
        this.t526 =
            -0.8072064e7 * this.t42 * this.t250 + 0.40360320e8 * this.t148 * this.t265 + 0.1995840e7 * this.t307
                * this.t255 + 0.1995840e7 * this.t304
                * this.t260 - 0.20386080e8 * this.t50 * this.t265 - 0.86486400e8 * this.t37 * this.t265 + 0.22239360e8
                * this.t29 * this.t250
                - 0.5544000e7 * this.t23 * this.t265 - 0.1164240e7 * this.t16 * this.t265 + 0.9081072e7 * this.t123
                * this.t509 - 0.1164240e7 * this.t14
                * this.t512 + 0.40360320e8 * this.t41 * this.t509 - 0.20386080e8 * this.t49 * this.t517 - 0.5544000e7
                * this.t22 * this.t512 - 0.8072064e7
                * this.t41 * this.t522 * this.t58;
        this.t529 = this.t65 * ex;
        this.t536 = this.t28 * this.t43;
        this.t563 =
            0.75600e5 * this.t53 * this.t529 - 0.40772160e8 * this.t27 * this.t36 * this.t43 * this.t10 + 0.18162144e8
                * this.t41 * this.t536 * this.t10
                - 0.2328480e7 * this.t22 * this.t522 * this.t10 + 0.11119680e8 * this.t481 * this.t197 + 0.40360320e8
                * this.t218 * this.t205
                - 0.5544000e7 * this.t248 * this.t168 - 0.4036032e7 * this.t218 * this.t211 - 0.86486400e8 * this.t481
                * this.t201 + 0.5765760e7
                * this.t233 * this.t230 + 0.11531520e8 * this.t238 * this.t235 + 0.11531520e8 * this.t243 * this.t240
                + 0.5765760e7 * this.t218 * this.t245
                + 0.6652800e7 * this.t248 * this.t255;
        this.t593 =
            0.6652800e7 * this.t253 * this.t260 + 0.403200e6 * this.t272 * this.t265 + 0.1330560e7 * this.t286
                * this.t260 + 0.1995840e7 * this.t258
                * this.t255 + 0.1330560e7 * this.t253 * this.t250 - 0.86486400e8 * this.t27 * this.t517 + 0.22239360e8
                * this.t27 * this.t536 * this.t58
                + 0.201600e6 * this.t53 * this.t265 + 0.665280e6 * this.t22 * this.t250 + 0.37800e5 * this.t62
                * this.t265 + 0.17297280e8 * this.t93
                * this.t265 - 0.34594560e8 * this.t76 * this.t265 - 0.23063040e8 * this.t96 * this.t250 - 0.4942080e7
                * this.t81 * this.t222;
        this.t600 = this.t263 * this.t15;
        this.t602 = this.t226 * this.t13;
        this.t607 = this.t218 * this.t36;
        this.t615 = this.t53 * this.t6;
        this.t624 =
            0.57657600e8 * this.t87 * this.t250 - 0.2661120e7 * this.t90 * this.t265 + 0.1680e4 * ex - 0.181440e6
                * this.t600 + 0.332640e6
                * this.t602 * this.t58 + 0.1153152e7 * this.t227 * this.t83 - 0.20180160e8 * this.t607 + 0.2217600e7
                * this.t258 * this.t58 - 0.40772160e8
                * this.t37 * this.t529 + 0.18162144e8 * this.t148 * this.t529 + 0.151200e6 * this.t615 * this.t265
                - 0.69189120e8 * this.t320 * this.t260
                - 0.69189120e8 * this.t326 * this.t255 - 0.24710400e8 * this.t332 * this.t230;
        this.t635 = this.t243 * this.t28;
        this.t648 = this.t253 * this.t15;
        this.t657 = this.t233 * this.t15;
        this.t662 =
            -0.49420800e8 * this.t335 * this.t235 - 0.49420800e8 * this.t338 * this.t240 - 0.24710400e8 * this.t341
                * this.t245 + 0.172972800e9
                * this.t350 * this.t255 + 0.172972800e9 * this.t344 * this.t260 + 0.80720640e8 * this.t635 * this.t265
                - 0.24216192e8 * this.t317 * this.t255
                - 0.16144128e8 * this.t320 * this.t250 + 0.44478720e8 * this.t57 * this.t26 * this.t28 * this.t260
                + 0.66718080e8 * this.t347 * this.t255
                - 0.11088000e8 * this.t648 * this.t265 + 0.44478720e8 * this.t344 * this.t250 - 0.172972800e9 * this.t6
                * this.t26 * this.t36 * this.t265
                - 0.16144128e8 * this.t657 * this.t260 - 0.2328480e7 * this.t23 * this.t529;
        this.t667 = 0.1e1 / this.t362 / a;
        this.t672 = this.t359 * this.t667;
        this.t673 = this.t369 * this.t369;
        this.t675 = 0.1e1 / this.t371 / this.t673;
        this.t681 = this.t30 * this.t11;
        this.t684 = ey * this.t11;
        this.t687 = this.t30 * this.t69;
        this.t701 = this.t30 * this.t40;
        this.t705 = this.t30 * this.t13;
        this.t709 = this.t219 * this.t40;
        this.t713 = this.t30 * this.t26;
        this.t717 = this.t15 * ey;
        this.t726 =
            0.75600e5 * this.t53 * this.t681 - 0.34594560e8 * this.t76 * this.t684 + 0.57657600e8 * this.t87
                * this.t687 - 0.23063040e8 * this.t96
                * this.t687 - 0.2661120e7 * this.t90 * this.t684 - 0.4942080e7 * this.t81 * this.t219 * this.t108
                + 0.11119680e8 * this.t219 * this.t26 * this.t28
                * this.t69 + 0.40360320e8 * this.t701 * this.t28 * this.t11 - 0.5544000e7 * this.t705 * this.t15
                * this.t11 - 0.4036032e7 * this.t709 * this.t15
                * this.t69 - 0.86486400e8 * this.t713 * this.t36 * this.t11 - 0.132300e6 * this.t62 * this.t717
                + 0.1153152e7 * this.t227 * this.t228 * iy
                + 0.2217600e7 * this.t258 * this.t44 * iy;
        this.t734 = this.t36 * ey;
        this.t739 = this.t28 * ey;
    }

    /**
     * Partial derivative due to 8th order Earth potential zonal harmonics.
     * 
     * @param a
     *        semi-major axis.
     * @param ex
     *        x eccentricity.
     * @param ey
     *        y eccentricity.
     * @param ix
     *        x inclination.
     * @param iy
     *        y inclination.
     */
    private final void
            derParUdeg8_3(final double a, final double ex, final double ey, final double ix, final double iy) {

        this.t742 = this.t116 * ey;
        this.t745 = ey * this.t40;
        this.t748 = this.t12 * ey;
        this.t759 = this.t54 * this.t684;
        this.t762 = this.t31 * this.t7;
        this.t763 = this.t17 * this.t762;
        this.t766 =
            0.120960e6 * this.t263 * ix * iy + 0.1050e4 * this.t9 * ey + 0.4200e4 * this.t8 * ey - 0.16648632e8
                * this.t123 * this.t734
                - 0.60540480e8 * this.t41 * this.t734 + 0.2619540e7 * this.t14 * this.t739 + 0.118918800e9 * this.t27
                * this.t742 - 0.20180160e8
                * this.t745 * this.t36 + 0.120960e6 * this.t748 * this.t11 + 0.332640e6 * this.t219 * this.t13
                * this.t69 + 0.17297280e8 * this.t93 * this.t684
                + 0.201600e6 * this.t30 * this.t12 * this.t11 - 0.24216192e8 * this.t42 * this.t759 - 0.24216192e8
                * this.t42 * this.t763;
        this.t772 = this.t7 * iy;
        this.t773 = this.t17 * this.t772;
        this.t780 = this.t17 * iy;
        this.t785 = this.t45 * iy;
        this.t798 = this.t54 * ey;
        this.t801 =
            0.66718080e8 * this.t29 * this.t759 + 0.66718080e8 * this.t29 * this.t763 + 0.36324288e8 * this.t148
                * this.t773 - 0.4656960e7 * this.t23
                * this.t773 - 0.81544320e8 * this.t37 * this.t773 + 0.40360320e8 * this.t148 * this.t780 + 0.9081072e7
                * this.t156 * this.t780
                + 0.22239360e8 * this.t29 * this.t785 - 0.1164240e7 * this.t16 * this.t780 - 0.5544000e7 * this.t23
                * this.t780 - 0.86486400e8 * this.t37
                * this.t780 - 0.8072064e7 * this.t42 * this.t785 - 0.20386080e8 * this.t50 * this.t780 - 0.40772160e8
                * this.t37 * this.t798;
        this.t806 = ix * this.t68;
        this.t807 = this.t806 * this.t221;
        this.t810 = this.t58 * ey;
        this.t811 = this.t810 * this.t11;
        this.t814 = this.t44 * this.t7;
        this.t815 = this.t814 * this.t31;
        this.t818 = this.t10 * this.t30;
        this.t819 = this.t818 * this.t69;
        this.t822 = ix * this.t7;
        this.t823 = this.t822 * this.t31;
        this.t826 = this.t10 * ey;
        this.t827 = this.t826 * this.t11;
        this.t832 = this.t7 * this.t13;
        this.t835 = this.t54 * this.t11;
        this.t838 = this.t17 * this.t31;
        this.t841 = ey * this.t26;
        this.t850 =
            0.18162144e8 * this.t148 * this.t798 - 0.2328480e7 * this.t23 * this.t798 + 0.5765760e7 * this.t218
                * this.t807 + 0.5765760e7 * this.t233
                * this.t811 + 0.11531520e8 * this.t238 * this.t815 + 0.11531520e8 * this.t243 * this.t819 + 0.6652800e7
                * this.t248 * this.t823
                + 0.6652800e7 * this.t253 * this.t827 + 0.403200e6 * this.t283 * this.t780 + 0.1330560e7 * this.t832
                * this.t785 + 0.1995840e7 * this.t705
                * this.t835 + 0.1330560e7 * this.t280 * this.t838 - 0.86486400e8 * this.t841 * this.t172 + 0.11119680e8
                * this.t841 * this.t193
                - 0.40772160e8 * this.t27 * this.t36 * this.t30 * this.t11;
        this.t853 = this.t717 * this.t11;
        this.t856 = this.t28 * this.t30;
        this.t860 = this.t734 * this.t11;
        this.t863 = this.t739 * this.t11;
        this.t868 = this.t15 * this.t30;
        this.t886 = ey * this.t13;
        this.t891 =
            -0.1164240e7 * this.t14 * this.t853 + 0.22239360e8 * this.t27 * this.t856 * this.t69 - 0.20386080e8
                * this.t49 * this.t860 + 0.40360320e8
                * this.t41 * this.t863 - 0.5544000e7 * this.t22 * this.t853 - 0.8072064e7 * this.t41 * this.t868
                * this.t69 + 0.9081072e7 * this.t123 * this.t863
                - 0.86486400e8 * this.t27 * this.t860 + 0.201600e6 * this.t53 * this.t780 + 0.665280e6 * this.t22
                * this.t785 + 0.18162144e8 * this.t41
                * this.t856 * this.t11 - 0.2328480e7 * this.t22 * this.t868 * this.t11 - 0.5544000e7 * this.t886
                * this.t176 - 0.4036032e7 * this.t745 * this.t180;
        this.t904 = this.t226 * this.t228;
        this.t923 =
            0.75600e5 * this.t53 * this.t798 + 0.40360320e8 * this.t745 * this.t164 + 0.37800e5 * this.t62 * this.t780
                + 0.17297280e8 * this.t93 * this.t780
                - 0.23063040e8 * this.t96 * this.t785 - 0.34594560e8 * this.t76 * this.t780 - 0.4942080e7 * this.t81
                * this.t904 * iy + 0.57657600e8
                * this.t87 * this.t785 - 0.2661120e7 * this.t90 * this.t780 + 0.1995840e7 * this.t307 * this.t823
                + 0.1995840e7 * this.t304 * this.t827
                + 0.151200e6 * this.t298 * this.t822 * iy - 0.69189120e8 * this.t320 * this.t827 - 0.69189120e8
                * this.t326 * this.t823;
        this.t937 = this.t7 * this.t40;
        this.t948 = this.t7 * this.t26;
        this.t965 =
            -0.24710400e8 * this.t332 * this.t811 - 0.49420800e8 * this.t335 * this.t815 - 0.49420800e8 * this.t338
                * this.t819 - 0.24710400e8
                * this.t341 * this.t807 + 0.172972800e9 * this.t344 * this.t827 + 0.172972800e9 * this.t350 * this.t823
                + 0.80720640e8 * this.t937 * this.t28
                * this.t780 - 0.24216192e8 * this.t701 * this.t15 * this.t835 - 0.16144128e8 * this.t68 * this.t40
                * this.t15 * this.t838 + 0.44478720e8 * this.t948
                * this.t28 * this.t785 + 0.66718080e8 * this.t713 * this.t28 * this.t835 - 0.11088000e8 * this.t832
                * this.t15 * this.t780 + 0.44478720e8 * this.t68
                * this.t26 * this.t28 * this.t838 - 0.172972800e9 * this.t948 * this.t36 * this.t780;
        this.t996 =
            -0.16144128e8 * this.t937 * this.t15 * this.t785 + 0.1680e4 * ey + 0.33127380e8 * this.t49 * this.t742
                - 0.504000e6 * this.t53 * this.t717
                + 0.9702000e7 * this.t22 * this.t739 + 0.201600e6 * this.t53 * this.t684 + 0.665280e6 * this.t22
                * this.t687 + 0.37800e5 * this.t62 * this.t684
                + 0.201600e6 * this.t748 * this.t54 + 0.332640e6 * this.t886 * this.t59 + 0.2217600e7 * this.t705
                * this.t69 + 0.3326400e7 * this.t886
                * this.t28 + 0.38918880e8 * this.t841 * this.t116 + 0.1153152e7 * this.t709 * this.t108 - 0.181440e6
                * this.t748 * this.t15;
        this.t1009 = this.t58 * this.t44;
        this.t1014 = ix * this.t15;
        this.t1017 = this.t12 * this.t57;
        this.t1026 = this.t12 * this.t28;
        this.t1032 = this.t10 * this.t219 * this.t221;
        this.t1035 = this.t814 * this.t11;
        this.t1040 = this.t93 * this.t43;
        this.t1041 = this.t810 * iy;
        this.t1044 =
            -0.1647360e7 * this.t26 * this.t1009 * this.t82 - 0.11520e5 * this.t467 + 0.11520e5 * this.t1014
                - 0.120960e6 * this.t381
                - 0.2217600e7 * this.t1017 * this.t228 - 0.252000e6 * this.t62 * ix - 0.3294720e7 * this.t93 * ix
                - 0.44100e5 * this.t295 * ix
                - 0.380160e6 * this.t1026 * ix + 0.8236800e7 * this.t76 * ix + 0.39536640e8 * this.t326 * this.t1032
                - 0.691891200e9 * this.t635
                * this.t1035 + 0.345945600e9 * this.t338 * this.t1035 - 0.461260800e9 * this.t1040 * this.t1041;
        this.t1047 = this.t818 * this.t31;
        this.t1052 = this.t826 * iy;
        this.t1057 = this.t6 * this.t7 * this.t11;
        this.t1062 = ex * this.t30 * this.t31;
        this.t1067 = iy * ey * ex;
        this.t1075 = this.t43 * ey * iy;
        this.t1088 =
            0.230630400e9 * this.t335 * this.t1041 - 0.461260800e9 * this.t313 * this.t1047 + 0.230630400e9 * this.t341
                * this.t1047
                + 0.10644480e8 * this.t600 * this.t1052 - 0.24216192e8 * this.t41 * this.t44 * this.t1057
                - 0.16144128e8 * this.t41 * this.t10 * this.t1062
                - 0.2328480e7 * this.t14 * this.t10 * this.t1067 - 0.11088000e8 * this.t22 * this.t10 * this.t1067
                - 0.16144128e8 * this.t41 * this.t58 * this.t1075
                - 0.8072064e7 * this.t42 * this.t1062 + 0.9081072e7 * this.t156 * this.t1067 - 0.1164240e7 * this.t16
                * this.t1067 - 0.5544000e7
                * this.t23 * this.t1067 + 0.22239360e8 * this.t29 * this.t1062;
        this.t1094 = this.t822 * this.t11;
        this.t1133 =
            -0.86486400e8 * this.t37 * this.t1067 - 0.20386080e8 * this.t50 * this.t1067 + 0.2328480e7 * this.t141
                * this.t1094 - 0.8870400e7
                * this.t263 * this.t1047 - 0.13305600e8 * this.t272 * this.t1035 - 0.8870400e7 * this.t474 * this.t1041
                - 0.403200e6 * this.t8 * this.t10
                * this.t1067 - 0.75600e5 * this.t9 * this.t10 * this.t1067 - 0.46126080e8 * this.t40 * this.t58
                * this.t1075 - 0.69189120e8 * this.t40 * this.t44
                * this.t1057 - 0.46126080e8 * this.t40 * this.t10 * this.t1062 - 0.9884160e7 * this.t26 * this.t83
                * this.t226 * ey * iy - 0.24710400e8
                * this.t26 * this.t228 * this.t57 * this.t7 * this.t11 - 0.32947200e8 * this.t26 * this.t58 * this.t43
                * this.t30 * this.t31;
        this.t1141 = ex * this.t219 * this.t221;
        this.t1147 = this.t171 * ix;
        this.t1159 = this.t806 * this.t69;
        this.t1172 =
            -0.24710400e8 * this.t26 * this.t44 * this.t6 * this.t68 * this.t69 - 0.9884160e7 * this.t26 * this.t10
                * this.t1141 - 0.5322240e7 * this.t13 * this.t10
                * this.t1067 - 0.86486400e8 * this.t27 * this.t1147 + 0.22239360e8 * this.t27 * this.t192 * this.t44
                + 0.5765760e7 * this.t227 * this.t1041
                + 0.11531520e8 * this.t233 * this.t1035 + 0.11531520e8 * this.t238 * this.t1047 + 0.5765760e7
                * this.t243 * this.t1159 + 0.6652800e7
                * this.t253 * this.t1094 + 0.6652800e7 * this.t258 * this.t1052 + 0.201600e6 * this.t53 * this.t1067
                + 0.665280e6 * this.t22 * this.t1062
                + 0.37800e5 * this.t62 * this.t1067;
        this.t1187 = this.t171 * this.t44;
        this.t1204 =
            0.17297280e8 * this.t93 * this.t1067 - 0.34594560e8 * this.t76 * this.t1067 - 0.23063040e8 * this.t96
                * this.t1062 - 0.4942080e7
                * this.t81 * this.t1141 + 0.57657600e8 * this.t87 * this.t1062 - 0.2661120e7 * this.t90 * this.t1067
                + 0.345945600e9 * this.t41 * this.t1187
                - 0.665280e6 * this.t53 * this.t1159 + 0.138378240e9 * this.t119 * this.t1094 - 0.103783680e9
                * this.t87 * this.t1094 - 0.115315200e9
                * this.t93 * this.t1159 + 0.57657600e8 * this.t81 * this.t1159 + 0.34594560e8 * this.t90 * this.t1159
                - 0.51891840e8 * this.t114 * this.t1094;
        this.t1212 = this.t163 * this.t44;
        this.t1215 = this.t175 * this.t44;
        this.t1226 = this.t163 * ix;
        this.t1231 = this.t175 * ix;
        this.t1239 =
            0.34594560e8 * this.t96 * this.t1094 + 0.5322240e7 * this.t121 * this.t1094 + 0.6589440e7 * this.t96
                * this.t109 * ix - 0.121080960e9
                * this.t22 * this.t1212 + 0.80720640e8 * this.t41 * this.t1215 - 0.1164240e7 * this.t14 * this.t1094
                + 0.81544320e8 * this.t123 * this.t1187
                - 0.61158240e8 * this.t49 * this.t1212 + 0.11088000e8 * this.t53 * this.t1215 + 0.40360320e8 * this.t41
                * this.t1226 - 0.20386080e8
                * this.t49 * this.t1147 - 0.5544000e7 * this.t22 * this.t1231 - 0.8072064e7 * this.t41 * this.t179
                * this.t44 + 0.9081072e7 * this.t123 * this.t1226;
        this.t1243 = this.t179 * this.t228;
        this.t1266 = this.t83 * ey * iy;
        this.t1270 = this.t228 * this.t7 * this.t11;
        this.t1274 = this.t58 * this.t30 * this.t31;
        this.t1277 =
            -0.1164240e7 * this.t14 * this.t1231 + 0.12108096e8 * this.t22 * this.t1243 - 0.27243216e8 * this.t14
                * this.t1212 + 0.18162144e8
                * this.t123 * this.t1215 + 0.2328480e7 * this.t62 * this.t1215 - 0.259459200e9 * this.t27 * this.t1212
                - 0.44478720e8 * this.t41 * this.t192
                * this.t228 + 0.22239360e8 * this.t27 * this.t1243 - 0.5544000e7 * this.t22 * this.t1094 - 0.4036032e7
                * this.t41 * this.t1159
                - 0.6918912e7 * this.t248 * this.t1032 - 0.6918912e7 * this.t602 * this.t1266 - 0.17297280e8
                * this.t286 * this.t1270 - 0.23063040e8
                * this.t258 * this.t1274;
        this.t1279 = this.t44 * this.t68 * this.t69;
        this.t1306 = this.t27 * this.t15;
        this.t1311 =
            -0.17297280e8 * this.t253 * this.t1279 + 0.1995840e7 * this.t301 * this.t1052 + 0.1995840e7 * this.t304
                * this.t1094 - 0.69189120e8
                * this.t317 * this.t1052 - 0.69189120e8 * this.t320 * this.t1094 - 0.24710400e8 * this.t329
                * this.t1041 - 0.49420800e8 * this.t332
                * this.t1035 - 0.49420800e8 * this.t335 * this.t1047 - 0.24710400e8 * this.t338 * this.t1159
                + 0.172972800e9 * this.t344 * this.t1094
                + 0.172972800e9 * this.t347 * this.t1052 + 0.40360320e8 * this.t148 * this.t1067 - 0.44478720e8
                * this.t148 * this.t1159
                + 0.22239360e8 * this.t1306 * this.t1159 + 0.81544320e8 * this.t124 * this.t1094;
        this.t1315 = this.t49 * this.t28;
        this.t1328 = this.t123 * this.t15;
        this.t1335 = this.t53 * this.t43;
        this.t1346 =
            -0.61158240e8 * this.t1315 * this.t1094 - 0.121080960e9 * this.t126 * this.t1094 + 0.80720640e8 * this.t42
                * this.t1094
                + 0.11088000e8 * this.t128 * this.t1094 + 0.12108096e8 * this.t23 * this.t1159 - 0.27243216e8
                * this.t144 * this.t1094 + 0.18162144e8
                * this.t1328 * this.t1094 + 0.345945600e9 * this.t146 * this.t1094 - 0.259459200e9 * this.t29
                * this.t1094 - 0.2661120e7 * this.t1335
                * this.t1041 - 0.3991680e7 * this.t615 * this.t1035 - 0.2661120e7 * this.t298 * this.t1047
                - 0.103783680e9 * this.t477 * this.t1052
                + 0.69189120e8 * this.t326 * this.t1052;
        this.t1347 = this.t90 * this.t43;
        this.t1358 = this.t96 * this.t226;
        this.t1368 = this.t9 * ix;
        this.t1376 = this.t377 * this.t18;
        this.t1378 =
            0.138378240e9 * this.t1347 * this.t1041 + 0.207567360e9 * this.t648 * this.t1035 + 0.276756480e9
                * this.t607 * this.t1052
                - 0.207567360e9 * this.t350 * this.t1052 + 0.138378240e9 * this.t353 * this.t1047 + 0.39536640e8
                * this.t1358 * this.t1266
                + 0.98841600e8 * this.t657 * this.t1270 + 0.131788800e9 * this.t317 * this.t1274 + 0.98841600e8
                * this.t320 * this.t1279 - 0.120960e6
                * this.t1094 + 0.252000e6 * this.t1368 * this.t15 - 0.37800e5 * this.t9 * this.t44 * this.t6
                - 0.181440e6 * this.t53 * ix - 0.241920e6
                * this.t1376;
        this.t1380 = this.t57 * this.t228;
        this.t1391 = this.t57 * this.t44;
        this.t1409 = this.t12 * this.t68;
        this.t1413 =
            -0.665280e6 * this.t53 * this.t1380 + 0.1153152e7 * this.t218 * this.t461 + 0.2217600e7 * this.t248
                * this.t32 + 0.120960e6 * this.t263
                * this.t18 + 0.201600e6 * this.t53 * this.t389 + 0.665280e6 * this.t22 * this.t1391 + 0.37800e5
                * this.t62 * this.t389 - 0.34594560e8
                * this.t76 * this.t389 - 0.4942080e7 * this.t81 * this.t82 * this.t228 + 0.57657600e8 * this.t87
                * this.t1391 - 0.2661120e7 * this.t90 * this.t389
                + 0.17297280e8 * this.t93 * this.t389 - 0.23063040e8 * this.t96 * this.t1391 - 0.2217600e7 * this.t1409
                * this.t69 * ix;
        this.t1414 = this.t13 * this.t107;
        this.t1446 =
            -0.1153152e7 * this.t1414 * this.t108 * ix - 0.37800e5 * this.t1368 * this.t65 + 0.138378240e9 * this.t119
                * this.t381
                - 0.103783680e9 * this.t87 * this.t381 + 0.6589440e7 * this.t96 * this.t82 * this.t1009 - 0.115315200e9
                * this.t93 * this.t1380
                + 0.57657600e8 * this.t81 * this.t1380 + 0.5322240e7 * this.t121 * this.t381 - 0.51891840e8 * this.t114
                * this.t381 + 0.34594560e8
                * this.t96 * this.t381 + 0.34594560e8 * this.t90 * this.t1380 - 0.11531520e8 * this.t460 * this.t70
                - 0.2661120e7 * this.t464 * this.t65
                - 0.1647360e7 * this.t26 * ix * this.t109;
        this.t1455 = this.t36 * ix;
        this.t1458 = this.t28 * ix;
        this.t1473 = this.t116 * ix;
        this.t1480 =
            -0.5544000e7 * this.t22 * this.t381 - 0.4036032e7 * this.t41 * this.t1380 - 0.1164240e7 * this.t14
                * this.t381 + 0.16648632e8 * this.t131
                * this.t1455 - 0.16648632e8 * this.t292 * this.t1458 + 0.90810720e8 * this.t14 * this.t1455
                - 0.90810720e8 * this.t123 * this.t1458
                - 0.6652800e7 * this.t53 * this.t1458 + 0.6652800e7 * this.t22 * this.t1014 - 0.1746360e7 * this.t295
                * this.t1458 + 0.1746360e7
                * this.t131 * this.t1014 - 0.237837600e9 * this.t123 * this.t1473 + 0.237837600e9 * this.t49
                * this.t1455 - 0.44169840e8 * this.t292
                * this.t1473;
        this.t1495 = this.t8 * ix;
        this.t1500 = this.t389 * this.t65;
        this.t1503 = this.t393 * this.t18;
        this.t1512 =
            0.44169840e8 * this.t136 * this.t1455 - 0.155675520e9 * this.t41 * this.t1473 + 0.155675520e9 * this.t27
                * this.t1455 - 0.9702000e7
                * this.t62 * this.t1458 + 0.9702000e7 * this.t14 * this.t1014 + 0.60540480e8 * this.t22 * this.t1455
                - 0.60540480e8 * this.t41 * this.t1458
                - 0.201600e6 * this.t1495 * this.t65 - 0.242161920e9 * this.t126 * this.t1376 - 0.24216192e8 * this.t42
                * this.t1500 + 0.66718080e8
                * this.t29 * this.t1503 + 0.66718080e8 * this.t29 * this.t1500 - 0.24216192e8 * this.t42 * this.t1503
                + 0.161441280e9 * this.t42 * this.t1376;
        this.t1514 = this.t381 * this.t65;
        this.t1517 = this.t377 * this.t32;
        this.t1524 = this.t405 * this.t18;
        this.t1545 =
            0.72648576e8 * this.t23 * this.t1514 + 0.48432384e8 * this.t23 * this.t1517 - 0.54486432e8 * this.t144
                * this.t1376 + 0.36324288e8
                * this.t1328 * this.t1376 - 0.177914880e9 * this.t148 * this.t1524 + 0.88957440e8 * this.t1306
                * this.t1524 - 0.266872320e9 * this.t148
                * this.t1514 + 0.133436160e9 * this.t1306 * this.t1514 + 0.4656960e7 * this.t141 * this.t1376
                + 0.22176000e8 * this.t128 * this.t1376
                - 0.177914880e9 * this.t148 * this.t1517 + 0.88957440e8 * this.t1306 * this.t1517 + 0.691891200e9
                * this.t146 * this.t1376
                - 0.518918400e9 * this.t29 * this.t1376;
        this.t1554 = this.t13 * this.t82;
        this.t1569 = this.t40 * this.t116;
        this.t1575 = this.t13 * this.t36;
        this.t1583 =
            0.48432384e8 * this.t23 * this.t1524 + 0.163088640e9 * this.t124 * this.t1376 - 0.122316480e9 * this.t1315
                * this.t1376 + 0.2217600e7
                * this.t286 * this.t44 - 0.1153152e7 * this.t1554 * this.t1009 + 0.44100e5 * this.t130 * ix * this.t15
                + 0.181440e6 * this.t1495 * this.t15
                - 0.201600e6 * this.t8 * this.t44 * this.t6 + 0.380160e6 * this.t90 * ix + 0.1153152e7 * this.t277
                * this.t228 - 0.8236800e7 * this.t1569
                * ix - 0.11531520e8 * this.t40 * this.t228 * this.t57 + 0.3294720e7 * this.t1575 * ix - 0.2661120e7
                * this.t13 * this.t44 * this.t6
                + 0.120960e6 * this.t272 * ix;
        this.t1592 = this.t229 * this.t11;
        this.t1595 = this.t234 * this.t31;
    }

    /**
     * Partial derivative due to 8th order Earth potential zonal harmonics.
     * 
     * @param a
     *        semi-major axis.
     * @param ex
     *        x eccentricity.
     * @param ey
     *        y eccentricity.
     * @param ix
     *        x inclination.
     * @param iy
     *        y inclination.
     */
    private final void
            derParUdeg8_4(final double a, final double ex, final double ey, final double ix, final double iy) {

        this.t1598 = this.t239 * this.t69;
        this.t1601 = this.t244 * this.t221;
        this.t1604 = this.t220 * this.t108;
        this.t1607 = this.t254 * this.t31;
        this.t1612 = this.t259 * this.t11;
        this.t1617 = this.t249 * this.t69;
        this.t1622 = this.t264 * this.t11;
        this.t1626 = this.t54 * this.t7;
        this.t1630 = this.t17 * this.t30;
        this.t1633 =
            0.39536640e8 * this.t1358 * this.t1592 + 0.98841600e8 * this.t657 * this.t1595 + 0.131788800e9 * this.t317
                * this.t1598
                + 0.98841600e8 * this.t320 * this.t1601 + 0.39536640e8 * this.t326 * this.t1604 - 0.691891200e9
                * this.t635 * this.t1607
                + 0.345945600e9 * this.t338 * this.t1607 - 0.461260800e9 * this.t1040 * this.t1612 + 0.230630400e9
                * this.t335 * this.t1612
                - 0.461260800e9 * this.t313 * this.t1617 + 0.230630400e9 * this.t341 * this.t1617 + 0.10644480e8
                * this.t600 * this.t1622
                - 0.24216192e8 * this.t41 * this.t31 * this.t1626 - 0.16144128e8 * this.t41 * this.t69 * this.t1630;
        this.t1635 = this.t17 * ey;
        this.t1642 = this.t45 * ey;
        this.t1659 = this.t254 * iy;
        this.t1662 = this.t249 * this.t11;
        this.t1665 = this.t234 * iy;
        this.t1668 = this.t239 * this.t11;
        this.t1671 =
            -0.2328480e7 * this.t14 * this.t11 * this.t1635 - 0.11088000e8 * this.t22 * this.t11 * this.t1635
                - 0.16144128e8 * this.t41 * this.t11 * this.t1642
                + 0.22239360e8 * this.t29 * this.t1642 - 0.1164240e7 * this.t16 * this.t1635 - 0.5544000e7 * this.t23
                * this.t1635 - 0.86486400e8
                * this.t37 * this.t1635 - 0.8072064e7 * this.t42 * this.t1642 - 0.20386080e8 * this.t50 * this.t1635
                - 0.2661120e7 * this.t1335 * this.t1612
                - 0.69189120e8 * this.t320 * this.t1659 - 0.69189120e8 * this.t326 * this.t1662 - 0.24710400e8
                * this.t332 * this.t1665
                - 0.49420800e8 * this.t335 * this.t1668;
        this.t1673 = this.t244 * this.t31;
        this.t1676 = this.t220 * this.t69;
        this.t1703 =
            -0.49420800e8 * this.t338 * this.t1673 - 0.24710400e8 * this.t341 * this.t1676 + 0.172972800e9 * this.t344
                * this.t1659
                + 0.172972800e9 * this.t350 * this.t1662 + 0.40360320e8 * this.t148 * this.t1635 + 0.9081072e7
                * this.t156 * this.t1635 - 0.6918912e7
                * this.t248 * this.t1604 - 0.6918912e7 * this.t602 * this.t1592 - 0.17297280e8 * this.t286 * this.t1595
                - 0.23063040e8 * this.t258 * this.t1598
                - 0.17297280e8 * this.t253 * this.t1601 - 0.8870400e7 * this.t263 * this.t1617 - 0.13305600e8
                * this.t272 * this.t1607 - 0.8870400e7
                * this.t474 * this.t1612;
        this.t1720 = this.t904 * ey;
        this.t1748 =
            -0.403200e6 * this.t8 * this.t11 * this.t1635 - 0.75600e5 * this.t9 * this.t11 * this.t1635 - 0.46126080e8
                * this.t40 * this.t11 * this.t1642
                - 0.69189120e8 * this.t40 * this.t31 * this.t1626 - 0.46126080e8 * this.t40 * this.t69 * this.t1630
                - 0.9884160e7 * this.t26 * this.t11 * this.t1720
                - 0.24710400e8 * this.t26 * this.t31 * this.t59 * this.t7 - 0.32947200e8 * this.t26 * this.t69
                * this.t45 * this.t30 - 0.24710400e8 * this.t26
                * this.t221 * this.t54 * this.t68 - 0.9884160e7 * this.t26 * this.t108 * this.t17 * this.t219
                - 0.5322240e7 * this.t13 * this.t11 * this.t1635
                + 0.5765760e7 * this.t233 * this.t1665 + 0.11531520e8 * this.t238 * this.t1668 + 0.11531520e8
                * this.t243 * this.t1673;
        this.t1770 = iy * this.t6 * this.t10;
        this.t1781 =
            0.6652800e7 * this.t248 * this.t1662 + 0.6652800e7 * this.t253 * this.t1659 + 0.201600e6 * this.t53
                * this.t1635 + 0.665280e6 * this.t22
                * this.t1642 - 0.181440e6 * this.t53 * iy + 0.1153152e7 * this.t289 * this.t221 + 0.44100e5 * this.t130
                * iy * this.t15 - 0.120960e6
                * this.t762 - 0.11520e5 * this.t12 * iy - 0.121080960e9 * this.t126 * this.t1770 + 0.80720640e8
                * this.t42 * this.t1770
                + 0.81544320e8 * this.t124 * this.t1770 - 0.61158240e8 * this.t1315 * this.t1770 + 0.11088000e8
                * this.t128 * this.t1770;
        this.t1782 = this.t59 * iy;
        this.t1811 =
            0.12108096e8 * this.t23 * this.t1782 - 0.27243216e8 * this.t144 * this.t1770 + 0.18162144e8 * this.t1328
                * this.t1770 + 0.2328480e7
                * this.t141 * this.t1770 + 0.345945600e9 * this.t146 * this.t1770 - 0.259459200e9 * this.t29
                * this.t1770 - 0.44478720e8 * this.t148
                * this.t1782 + 0.22239360e8 * this.t1306 * this.t1782 + 0.1995840e7 * this.t304 * this.t1659
                + 0.1995840e7 * this.t307 * this.t1662
                - 0.3991680e7 * this.t615 * this.t1607 - 0.2661120e7 * this.t298 * this.t1617 - 0.103783680e9
                * this.t477 * this.t1622 + 0.69189120e8
                * this.t326 * this.t1622;
        this.t1829 = this.t68 * this.t221;
        this.t1842 = this.t69 * this.t31;
        this.t1846 =
            0.138378240e9 * this.t1347 * this.t1612 + 0.207567360e9 * this.t648 * this.t1607 + 0.276756480e9
                * this.t607 * this.t1622
                - 0.207567360e9 * this.t350 * this.t1622 + 0.138378240e9 * this.t353 * this.t1617 - 0.2661120e7
                * this.t13 * iy * this.t54
                - 0.11531520e8 * this.t40 * iy * this.t59 - 0.115315200e9 * this.t93 * this.t1829 + 0.57657600e8
                * this.t81 * this.t1829
                + 0.34594560e8 * this.t90 * this.t1829 - 0.51891840e8 * this.t114 * this.t762 + 0.34594560e8 * this.t96
                * this.t762 + 0.5322240e7
                * this.t121 * this.t762 + 0.6589440e7 * this.t96 * this.t107 * this.t1842;
        this.t1854 = this.t11 * ex * this.t264;
        this.t1856 = this.t36 * iy;
        this.t1859 = this.t28 * iy;
        this.t1868 = iy * this.t15;
        this.t1875 = this.t116 * iy;
        this.t1882 =
            -0.1164240e7 * this.t14 * this.t762 - 0.5544000e7 * this.t22 * this.t762 - 0.4036032e7 * this.t41
                * this.t1829 - 0.241920e6 * this.t1854
                + 0.16648632e8 * this.t131 * this.t1856 - 0.16648632e8 * this.t292 * this.t1859 + 0.90810720e8
                * this.t14 * this.t1856 - 0.90810720e8
                * this.t123 * this.t1859 - 0.6652800e7 * this.t53 * this.t1859 + 0.6652800e7 * this.t22 * this.t1868
                - 0.1746360e7 * this.t295 * this.t1859
                + 0.1746360e7 * this.t131 * this.t1868 - 0.237837600e9 * this.t123 * this.t1875 + 0.237837600e9
                * this.t49 * this.t1856
                - 0.44169840e8 * this.t292 * this.t1875;
        this.t1912 = this.t167 * this.t31;
        this.t1917 =
            0.44169840e8 * this.t136 * this.t1856 - 0.155675520e9 * this.t41 * this.t1875 + 0.155675520e9 * this.t27
                * this.t1856 - 0.9702000e7
                * this.t62 * this.t1859 + 0.9702000e7 * this.t14 * this.t1868 + 0.60540480e8 * this.t22 * this.t1856
                - 0.60540480e8 * this.t41 * this.t1859
                - 0.2217600e7 * this.t1017 * this.t58 * iy - 0.1153152e7 * this.t1554 * this.t83 * iy + 0.1153152e7
                * this.t227 * this.t229
                + 0.2217600e7 * this.t258 * this.t259 + 0.120960e6 * this.t263 * this.t264 + 0.80720640e8 * this.t41
                * this.t1912 + 0.11088000e8
                * this.t53 * this.t1912;
        this.t1918 = this.t210 * this.t221;
        this.t1921 = this.t204 * this.t31;
        this.t1926 = this.t200 * this.t31;
        this.t1931 = this.t204 * iy;
        this.t1934 = this.t200 * iy;
        this.t1939 = this.t167 * iy;
        this.t1954 =
            0.12108096e8 * this.t22 * this.t1918 - 0.27243216e8 * this.t14 * this.t1921 + 0.18162144e8 * this.t123
                * this.t1912 + 0.345945600e9
                * this.t41 * this.t1926 - 0.259459200e9 * this.t27 * this.t1921 + 0.9081072e7 * this.t123 * this.t1931
                - 0.86486400e8 * this.t27 * this.t1934
                + 0.5765760e7 * this.t218 * this.t1676 - 0.1164240e7 * this.t14 * this.t1939 + 0.22239360e8 * this.t27
                * this.t196 * this.t31
                - 0.20386080e8 * this.t49 * this.t1934 + 0.40360320e8 * this.t41 * this.t1931 - 0.5544000e7 * this.t22
                * this.t1939 - 0.8072064e7
                * this.t41 * this.t210 * this.t31;
        this.t1985 =
            -0.665280e6 * this.t53 * this.t1782 + 0.37800e5 * this.t62 * this.t1635 + 0.17297280e8 * this.t93
                * this.t1635 - 0.23063040e8 * this.t96
                * this.t1642 - 0.34594560e8 * this.t76 * this.t1635 - 0.4942080e7 * this.t81 * this.t1720
                + 0.57657600e8 * this.t87 * this.t1642
                - 0.2661120e7 * this.t90 * this.t1635 + 0.138378240e9 * this.t119 * this.t1770 - 0.103783680e9
                * this.t87 * this.t1770 + 0.6589440e7
                * this.t96 * this.t84 * iy - 0.115315200e9 * this.t93 * this.t1782 + 0.57657600e8 * this.t81
                * this.t1782 + 0.5322240e7 * this.t121
                * this.t1770;
        this.t2014 =
            -0.51891840e8 * this.t114 * this.t1770 + 0.34594560e8 * this.t96 * this.t1770 + 0.34594560e8 * this.t90
                * this.t1782 + 0.2328480e7
                * this.t62 * this.t1912 - 0.5544000e7 * this.t22 * this.t1770 - 0.4036032e7 * this.t41 * this.t1782
                - 0.1164240e7 * this.t14 * this.t1770
                - 0.44478720e8 * this.t41 * this.t196 * this.t221 + 0.22239360e8 * this.t27 * this.t1918 + 0.81544320e8
                * this.t123 * this.t1926
                - 0.61158240e8 * this.t49 * this.t1921 - 0.121080960e9 * this.t22 * this.t1921 + 0.11520e5 * this.t1868
                - 0.8236800e7 * this.t1569
                * iy;
        this.t2038 = this.t68 * this.t31;
        this.t2052 =
            -0.2217600e7 * this.t1409 * this.t221 - 0.380160e6 * this.t1026 * iy - 0.1647360e7 * this.t26 * this.t1842
                * this.t107 - 0.2661120e7
                * this.t13 * this.t31 * this.t7 - 0.11531520e8 * this.t40 * this.t221 * this.t68 - 0.37800e5 * this.t9
                * this.t31 * this.t7 - 0.201600e6 * this.t8 * this.t31
                * this.t7 - 0.34594560e8 * this.t76 * this.t772 + 0.57657600e8 * this.t87 * this.t2038 - 0.23063040e8
                * this.t96 * this.t2038
                + 0.17297280e8 * this.t93 * this.t772 - 0.2661120e7 * this.t90 * this.t772 - 0.4942080e7 * this.t81
                * this.t107 * this.t221 + 0.201600e6
                * this.t53 * this.t772;
        this.t2057 = this.t8 * iy;
        this.t2060 = this.t9 * iy;
        this.t2072 = this.t45 * this.t684;
        this.t2077 = this.t54 * this.t762;
        this.t2086 =
            0.665280e6 * this.t22 * this.t2038 + 0.37800e5 * this.t62 * this.t772 - 0.201600e6 * this.t2057 * this.t54
                - 0.37800e5 * this.t2060 * this.t54
                - 0.665280e6 * this.t53 * this.t1829 + 0.138378240e9 * this.t119 * this.t762 - 0.103783680e9 * this.t87
                * this.t762 - 0.1647360e7
                * this.t26 * iy * this.t84 - 0.177914880e9 * this.t148 * this.t2072 + 0.88957440e8 * this.t1306
                * this.t2072 - 0.266872320e9 * this.t148
                * this.t2077 + 0.133436160e9 * this.t1306 * this.t2077 + 0.4656960e7 * this.t141 * this.t1854
                + 0.22176000e8 * this.t128 * this.t1854;
        this.t2088 = this.t17 * this.t687;
        this.t2103 = this.t54 * this.t772;
        this.t2106 = this.t17 * this.t681;
        this.t2119 =
            -0.177914880e9 * this.t148 * this.t2088 + 0.88957440e8 * this.t1306 * this.t2088 + 0.691891200e9
                * this.t146 * this.t1854
                - 0.518918400e9 * this.t29 * this.t1854 + 0.48432384e8 * this.t23 * this.t2072 + 0.163088640e9
                * this.t124 * this.t1854
                - 0.122316480e9 * this.t1315 * this.t1854 - 0.24216192e8 * this.t42 * this.t2103 - 0.24216192e8
                * this.t42 * this.t2106
                + 0.66718080e8 * this.t29 * this.t2103 + 0.66718080e8 * this.t29 * this.t2106 - 0.242161920e9
                * this.t126 * this.t1854
                + 0.161441280e9 * this.t42 * this.t1854 + 0.72648576e8 * this.t23 * this.t2077;
        this.t2149 =
            0.48432384e8 * this.t23 * this.t2088 - 0.54486432e8 * this.t144 * this.t1854 + 0.36324288e8 * this.t1328
                * this.t1854 + 0.8236800e7
                * this.t76 * iy + 0.252000e6 * this.t2060 * this.t15 + 0.380160e6 * this.t90 * iy - 0.44100e5
                * this.t295 * iy + 0.120960e6
                * this.t283 * iy + 0.2217600e7 * this.t280 * this.t31 - 0.1153152e7 * this.t1414 * this.t1842
                - 0.252000e6 * this.t62 * iy
                + 0.181440e6 * this.t2057 * this.t15 + 0.3294720e7 * this.t1575 * iy - 0.120960e6 * this.t1770
                - 0.3294720e7 * this.t93 * iy;
    }
}
