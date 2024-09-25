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
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11:DM:DM-3287:22/05/2023:[PATRIUS] Courtes periodes traînee atmospherique et prs
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:315:26/02/2015:add zonal terms J8 to J15
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.stela.forces.gravity;

import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.stela.forces.AbstractStelaLagrangeContribution;
import fr.cnes.sirius.patrius.stela.orbits.OrbitNatureConverter;
import fr.cnes.sirius.patrius.stela.orbits.StelaEquinoctialOrbit;

//CHECKSTYLE:OFF

/**
 * <p>
 * Class computing 9th order zonal perturbations. This class has package visibility since it is not supposed to be used
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
class StelaZonalAttractionJ9 extends AbstractStelaLagrangeContribution {

     /** Serializable UID. */
    private static final long serialVersionUID = -3371819042970022768L;

    /** The central body reference radius (m). */
    private final double rEq;
    /** The 9th order central body coefficients */
    private final double j9;

    /** Temporary coefficients. */
    private double t1;
    private double t2;
    private double t3;
    private double t4;
    private double t5;
    private double t6;
    private double t7;
    private double t8;
    private double t9;
    private double t11;
    private double t13;
    private double t14;
    private double t15;
    private double t16;
    private double t17;
    private double t18;
    private double t19;
    private double t20;
    private double t21;
    private double t22;
    private double t23;
    private double t24;
    private double t25;
    private double t28;
    private double t29;
    private double t31;
    private double t34;
    private double t35;
    private double t36;
    private double t39;
    private double t40;
    private double t42;
    private double t43;
    private double t45;
    private double t46;
    private double t48;
    private double t51;
    private double t52;
    private double t53;
    private double t56;
    private double t59;
    private double t62;
    private double t63;
    private double t64;
    private double t66;
    private double t67;
    private double t69;
    private double t72;
    private double t73;
    private double t74;
    private double t77;
    private double t78;
    private double t79;
    private double t82;
    private double t85;
    private double t86;
    private double t87;
    private double t93;
    private double t94;
    private double t97;
    private double t98;
    private double t99;
    private double t100;
    private double t101;
    private double t104;
    private double t105;
    private double t106;
    private double t109;
    private double t111;
    private double t112;
    private double t113;
    private double t114;
    private double t115;
    private double t121;
    private double t122;
    private double t125;
    private double t126;
    private double t129;
    private double t132;
    private double t136;
    private double t139;
    private double t140;
    private double t141;
    private double t142;
    private double t143;
    private double t144;
    private double t147;
    private double t148;
    private double t149;
    private double t152;
    private double t153;
    private double t154;
    private double t157;
    private double t158;
    private double t159;
    private double t162;
    private double t163;
    private double t164;
    private double t165;
    private double t166;
    private double t169;
    private double t170;
    private double t171;
    private double t174;
    private double t177;
    private double t180;
    private double t181;
    private double t184;
    private double t188;
    private double t190;
    private double t192;
    private double t193;
    private double t196;
    private double t197;
    private double t200;
    private double t201;
    private double t204;
    private double t205;
    private double t208;
    private double t209;
    private double t212;
    private double t221;
    private double t222;
    private double t225;
    private double t226;
    private double t229;
    private double t230;
    private double t233;
    private double t234;
    private double t239;
    private double t240;
    private double t247;
    private double t250;
    private double t251;
    private double t254;
    private double t257;
    private double t260;
    private double t263;
    private double t266;
    private double t269;
    private double t272;
    private double t276;
    private double t277;
    private double t278;
    private double t279;
    private double t282;
    private double t283;
    private double t284;
    private double t285;
    private double t288;
    private double t289;
    private double t292;
    private double t295;
    private double t297;
    private double t298;
    private double t299;
    private double t304;
    private double t307;
    private double t312;
    private double t317;
    private double t322;
    private double t329;
    private double t332;
    private double t339;
    private double t358;
    private double t361;
    private double t364;
    private double t365;
    private double t366;
    private double t368;
    private double t369;
    private double t372;
    private double t373;
    private double t374;
    private double t375;
    private double t376;
    private double t378;
    private double t385;
    private double t386;
    private double t391;
    private double t396;
    private double t399;
    private double t401;
    private double t403;
    private double t408;
    private double t412;
    private double t417;
    private double t422;
    private double t437;
    private double t450;
    private double t455;
    private double t461;
    private double t488;
    private double t491;
    private double t492;
    private double t495;
    private double t498;
    private double t501;
    private double t502;
    private double t505;
    private double t527;
    private double t530;
    private double t531;
    private double t534;
    private double t535;
    private double t544;
    private double t568;
    private double t591;
    private double t592;
    private double t595;
    private double t596;
    private double t601;
    private double t605;
    private double t606;
    private double t613;
    private double t625;
    private double t637;
    private double t660;
    private double t675;
    private double t686;
    private double t697;
    private double t700;
    private double t709;
    private double t712;
    private double t715;
    private double t720;
    private double t723;
    private double t728;
    private double t733;
    private double t742;
    private double t749;
    private double t750;
    private double t753;
    private double t754;
    private double t764;
    private double t765;
    private double t768;
    private double t769;
    private double t772;
    private double t774;
    private double t775;
    private double t778;
    private double t779;
    private double t786;
    private double t790;
    private double t792;
    private double t797;
    private double t800;
    private double t804;
    private double t808;
    private double t824;
    private double t827;
    private double t830;
    private double t833;
    private double t859;
    private double t868;
    private double t874;
    private double t879;
    private double t882;
    private double t903;
    private double t932;
    private double t935;
    private double t937;
    private double t941;
    private double t944;
    private double t949;
    private double t973;
    private double t984;
    private double t992;
    private double t995;
    private double t996;
    private double t1003;
    private double t1004;
    private double t1011;
    private double t1026;
    private double t1033;
    private double t1047;
    private double t1050;
    private double t1059;
    private double t1061;
    private double t1067;
    private double t1070;
    private double t1075;
    private double t1088;
    private double t1091;
    private double t1100;
    private double t1103;
    private double t1106;
    private double t1108;
    private double t1111;
    private double t1119;
    private double t1124;
    private double t1129;
    private double t1142;
    private double t1143;
    private double t1150;
    private double t1161;
    private double t1166;
    private double t1169;
    private double t1176;
    private double t1179;
    private double t1182;
    private double t1185;
    private double t1192;
    private double t1211;
    private double t1221;
    private double t1230;
    private double t1232;
    private double t1236;
    private double t1240;
    private double t1244;
    private double t1247;
    private double t1250;
    private double t1257;
    private double t1270;
    private double t1275;
    private double t1286;
    private double t1291;
    private double t1307;
    private double t1318;
    private double t1323;
    private double t1340;
    private double t1344;
    private double t1348;
    private double t1362;
    private double t1384;
    private double t1387;
    private double t1394;
    private double t1404;
    private double t1425;
    private double t1428;
    private double t1431;
    private double t1434;
    private double t1458;
    private double t1506;
    private double t1509;
    private double t1513;
    private double t1541;
    private double t1551;
    private double t1572;
    private double t1588;
    private double t1593;
    private double t1607;
    private double t1624;
    private double t1629;
    private double t1634;
    private double t1638;
    private double t1655;
    private double t1680;
    private double t1683;
    private double t1690;
    private double t1693;
    private double t1695;
    private double t1706;
    private double t1709;
    private double t1716;
    private double t1719;
    private double t1722;
    private double t1725;
    private double t1728;
    private double t1731;
    private double t1732;
    private double t1735;
    private double t1738;
    private double t1742;
    private double t1749;
    private double t1753;
    private double t1757;
    private double t1761;
    private double t1783;
    private double t1786;
    private double t1789;
    private double t1792;
    private double t1795;
    private double t1798;
    private double t1819;
    private double t1834;
    private double t1849;
    private double t1861;
    private double t1880;
    private double t1916;
    private double t1944;
    private double t1949;
    private double t1959;
    private double t1963;
    private double t1979;
    private double t1987;
    private double t2013;
    private double t2026;
    private double t2029;
    private double t2044;
    private double t2047;
    private double t2078;
    private double t2100;
    private double t2109;
    private double t2114;
    private double t2117;
    private double t2120;
    private double t2139;
    private double t2142;
    private double t2145;
    private double t2148;
    private double t2179;

    /**
     * Constructor
     * 
     * @param rEq
     *        equatorial radius (m)
     * @param j9
     *        9th order central body coefficient
     */
    public StelaZonalAttractionJ9(final double rEq, final double j9) {
        this.rEq = rEq;
        this.j9 = j9;
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
        this.derParUdeg9_1(a, ex, ey, ix, iy, mu);
        this.derParUdeg9_2(a, ex, ey, ix, iy);
        this.derParUdeg9_3(a, ex, ey, ix, iy);
        this.derParUdeg9_4(a, ex, ey, ix, iy);

        // Final result
        final double[] dPot = new double[6];

        dPot[0] = -0.15e2 / 0.512e3 * this.t11 * this.t365 / this.t369 / this.t366 / a * this.t378;
        dPot[1] = 0.0e0;
        dPot[2] =
            0.3e1 / 0.1024e4 * this.t11 * iy * this.t364 * this.t386 + 0.3e1 / 0.1024e4 * this.t11 * this.t14
                * (this.t422 + this.t455 + this.t488 + this.t527 + this.t568 + this.t601 + this.t637 + this.t675)
                * this.t386 + 0.51e2 / 0.1024e4 * this.t11 * this.t365
                * this.t686 * ex;
        dPot[3] =
            -0.3e1 / 0.1024e4 * this.t11 * ix * this.t364 * this.t386 + 0.3e1 / 0.1024e4 * this.t11 * this.t14
                * (this.t733 + this.t772 + this.t824 + this.t868 + this.t903 + this.t935 + this.t973 + this.t1011)
                * this.t386 + 0.51e2 / 0.1024e4 * this.t11 * this.t365
                * this.t686 * ey;
        dPot[4] =
            -0.3e1
                / 0.1024e4
                * this.t1026
                * this.t365
                * this.t386
                * ix
                - 0.3e1
                / 0.1024e4
                * this.t11
                * ey
                * this.t364
                * this.t386
                + 0.3e1
                / 0.1024e4
                * this.t11
                * this.t14
                * (this.t1070 + this.t1106 + this.t1142 + this.t1176 + this.t1211 + this.t1247 + this.t1291
                    + this.t1323 + this.t1362 + this.t1394 + this.t1425 + this.t1458
                    + this.t1509 + this.t1541 + this.t1572 + this.t1607) * this.t386;
        dPot[5] =
            -0.3e1
                / 0.1024e4
                * this.t1026
                * this.t365
                * this.t386
                * iy
                + 0.3e1
                / 0.1024e4
                * this.t11
                * ex
                * this.t364
                * this.t386
                + 0.3e1
                / 0.1024e4
                * this.t11
                * this.t14
                * (this.t1655 + this.t1693 + this.t1731 + this.t1783 + this.t1819 + this.t1849 + this.t1880
                    + this.t1916 + this.t1949 + this.t1979 + this.t2013 + this.t2044
                    + this.t2078 + this.t2109 + this.t2145 + this.t2179) * this.t386;

        return dPot;
    }

    @Override
    public double[] computeShortPeriods(final StelaEquinoctialOrbit orbit,
            final OrbitNatureConverter converter) {
        // Not available
        return new double[6];
    }

    @Override
    public double[][] computePartialDerivatives(final StelaEquinoctialOrbit orbit) {
        // Not available
        return new double[6][6];
    }

    /**
     * Partial derivative due to 9th order Earth potential zonal harmonics.
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
    private final void derParUdeg9_1(final double a, final double ex, final double ey, final double ix,
                                     final double iy, final double mu) {
        this.t1 = mu * this.j9;
        this.t2 = this.rEq * this.rEq;
        this.t3 = this.t2 * this.t2;
        this.t4 = this.t3 * this.t3;
        this.t5 = this.t4 * this.rEq;
        this.t6 = ix * ix;
        this.t7 = iy * iy;
        this.t8 = 0.1e1 - this.t6 - this.t7;
        this.t9 = MathLib.sqrt(this.t8);
        this.t11 = this.t1 * this.t5 * this.t9;
        this.t13 = ey * ix;
        this.t14 = ex * iy - this.t13;
        this.t15 = ex * ex;
        this.t16 = ey * ey;
        this.t17 = this.t15 + this.t16;
        this.t18 = this.t17 * this.t17;
        this.t19 = this.t18 * this.t17;
        this.t20 = this.t8 * this.t8;
        this.t21 = this.t20 * this.t8;
        this.t22 = this.t19 * this.t21;
        this.t23 = this.t6 + this.t7;
        this.t24 = this.t23 * this.t23;
        this.t25 = this.t24 * this.t23;
        this.t28 = this.t18 * this.t20;
        this.t29 = this.t28 * this.t24;
        this.t31 = this.t19 * this.t8;
        this.t34 = this.t15 * this.t15;
        this.t35 = this.t20 * this.t34;
        this.t36 = this.t6 * this.t6;
        this.t39 = this.t17 * this.t20;
        this.t40 = this.t39 * this.t24;
        this.t42 = this.t18 * this.t21;
        this.t43 = this.t42 * this.t25;
        this.t45 = this.t17 * this.t8;
        this.t46 = this.t45 * this.t23;
        this.t48 = this.t19 * this.t20;
        this.t51 = this.t20 * this.t20;
        this.t52 = this.t18 * this.t51;
        this.t53 = this.t24 * this.t24;
        this.t56 = this.t19 * this.t51;
        this.t59 = this.t17 * this.t51;
        this.t62 =
            0.6720e4 - 0.14723280e8 * this.t22 * this.t25 + 0.21189168e8 * this.t29 - 0.194040e6 * this.t31 * this.t23
                + 0.2690688e7 * this.t35
                * this.t36 + 0.23543520e8 * this.t40 - 0.110990880e9 * this.t43 - 0.1724800e7 * this.t46 + 0.2774772e7
                * this.t48 * this.t24
                + 0.194674480e9 * this.t52 * this.t53 + 0.26072475e8 * this.t56 * this.t53 + 0.209649440e9 * this.t59
                * this.t53;
        this.t63 = this.t18 * this.t8;
        this.t64 = this.t63 * this.t23;
        this.t66 = this.t17 * this.t21;
        this.t67 = this.t66 * this.t25;
        this.t69 = this.t8 * this.t15;
        this.t72 = this.t34 * this.t15;
        this.t73 = this.t21 * this.t72;
        this.t74 = this.t36 * this.t6;
        this.t77 = this.t16 * this.t16;
        this.t78 = this.t20 * this.t77;
        this.t79 = this.t7 * this.t7;
        this.t82 = this.t8 * this.t16;
        this.t85 = this.t77 * this.t16;
        this.t86 = this.t21 * this.t85;
        this.t87 = this.t79 * this.t7;
        this.t93 = this.t45 * ex;
        this.t94 = this.t13 * iy;
        this.t97 = this.t15 * ex;
        this.t98 = this.t39 * this.t97;
        this.t99 = this.t6 * ix;
        this.t100 = this.t99 * ey;
        this.t101 = this.t100 * iy;
        this.t104 = this.t39 * this.t15;
        this.t105 = this.t6 * this.t16;
        this.t106 = this.t105 * this.t7;
        this.t109 =
            -0.1509200e7 * this.t64 - 0.121080960e9 * this.t67 + 0.689920e6 * this.t69 * this.t6 + 0.549120e6
                * this.t73 * this.t74 + 0.2690688e7
                * this.t78 * this.t79 + 0.689920e6 * this.t82 * this.t7 + 0.549120e6 * this.t86 * this.t87 + 0.35280e5
                * this.t15 + 0.35280e5 * this.t16
                + 0.29400e5 * this.t18 + 0.1724800e7 * this.t93 * this.t94 + 0.2690688e7 * this.t98 * this.t101
                + 0.4036032e7 * this.t104 * this.t106;
        this.t111 = this.t39 * ex;
        this.t112 = this.t16 * ey;
        this.t113 = ix * this.t112;
        this.t114 = this.t7 * iy;
        this.t115 = this.t113 * this.t114;
        this.t121 = this.t21 * this.t24;
        this.t122 = this.t121 * ex;
        this.t125 = this.t21 * this.t23;
        this.t126 = this.t125 * this.t97;
        this.t129 = this.t125 * this.t15;
        this.t132 = this.t51 * this.t25;
        this.t136 = this.t125 * ex;
        this.t139 = this.t51 * this.t23;
        this.t140 = this.t34 * ex;
        this.t141 = this.t139 * this.t140;
        this.t142 = this.t36 * ix;
        this.t143 = this.t142 * ey;
        this.t144 = this.t143 * iy;
        this.t147 = this.t139 * this.t34;
        this.t148 = this.t36 * this.t16;
        this.t149 = this.t148 * this.t7;
        this.t152 = this.t139 * this.t97;
        this.t153 = this.t99 * this.t112;
        this.t154 = this.t153 * this.t114;
        this.t157 = this.t139 * this.t15;
        this.t158 = this.t6 * this.t77;
        this.t159 = this.t158 * this.t79;
        this.t162 = this.t139 * ex;
        this.t163 = this.t77 * ey;
        this.t164 = ix * this.t163;
        this.t165 = this.t79 * iy;
        this.t166 = this.t164 * this.t165;
        this.t169 =
            0.2690688e7 * this.t111 * this.t115 + 0.258720e6 * this.t63 * ex * this.t94 + 0.161441280e9 * this.t122
                * this.t94 - 0.107627520e9
                * this.t126 * this.t101 - 0.161441280e9 * this.t129 * this.t106 - 0.304944640e9 * this.t132 * ex
                * this.t94 - 0.107627520e9 * this.t136
                * this.t115 - 0.14002560e8 * this.t141 * this.t144 - 0.35006400e8 * this.t147 * this.t149
                - 0.46675200e8 * this.t152 * this.t154
                - 0.35006400e8 * this.t157 * this.t159 - 0.14002560e8 * this.t162 * this.t166;
        this.t170 = this.t51 * this.t24;
        this.t171 = this.t170 * this.t15;
        this.t174 = this.t170 * this.t97;
        this.t177 = this.t170 * ex;
        this.t180 = this.t20 * this.t23;
        this.t181 = this.t180 * ex;
        this.t184 = this.t20 * this.t24;
        this.t188 = this.t21 * this.t25;
        this.t190 = this.t8 * this.t23;
        this.t192 = this.t24 * this.t15;
        this.t193 = this.t192 * this.t6;
        this.t196 = this.t23 * this.t16;
        this.t197 = this.t196 * this.t7;
        this.t200 = this.t25 * this.t15;
        this.t201 = this.t200 * this.t6;
        this.t204 = this.t23 * this.t15;
        this.t205 = this.t204 * this.t6;
        this.t208 = this.t23 * this.t34;
        this.t209 = this.t208 * this.t36;
        this.t212 =
            0.392071680e9 * this.t171 * this.t106 + 0.261381120e9 * this.t174 * this.t101 + 0.261381120e9 * this.t177
                * this.t115 - 0.26906880e8
                * this.t181 * this.t94 + 0.3843840e7 * this.t184 + 0.32672640e8 * this.t51 * this.t53 - 0.19219200e8
                * this.t188 - 0.295680e6 * this.t190
                + 0.121080960e9 * this.t66 * this.t193 - 0.3027024e7 * this.t28 * this.t197 - 0.41715960e8 * this.t52
                * this.t201 - 0.18834816e8
                * this.t39 * this.t205 - 0.7413120e7 * this.t66 * this.t209;
        this.t221 = this.t24 * this.t34;
        this.t222 = this.t221 * this.t36;
        this.t225 = this.t24 * this.t77;
        this.t226 = this.t225 * this.t79;
        this.t229 = this.t25 * this.t16;
        this.t230 = this.t229 * this.t7;
        this.t233 = this.t24 * this.t16;
        this.t234 = this.t233 * this.t7;
        this.t239 = this.t23 * this.t77;
        this.t240 = this.t239 * this.t79;
        this.t247 = this.t21 * ex;
        this.t250 =
            0.20386080e8 * this.t42 * this.t193 - 0.3027024e7 * this.t28 * this.t205 - 0.239599360e9 * this.t59
                * this.t201 + 0.19253520e8 * this.t59
                * this.t222 + 0.19253520e8 * this.t59 * this.t226 - 0.41715960e8 * this.t52 * this.t230 + 0.121080960e9
                * this.t66 * this.t234
                - 0.18834816e8 * this.t39 * this.t197 - 0.7413120e7 * this.t66 * this.t240 + 0.20386080e8 * this.t42
                * this.t234 - 0.239599360e9
                * this.t59 * this.t230 + 0.3294720e7 * this.t247 * this.t166;
        this.t251 = this.t21 * this.t140;
        this.t254 = this.t21 * this.t34;
        this.t257 = this.t21 * this.t97;
        this.t260 = this.t21 * this.t15;
        this.t263 = this.t20 * ex;
        this.t266 = this.t20 * this.t15;
        this.t269 = this.t20 * this.t97;
        this.t272 = this.t8 * ex;
        this.t276 = this.t66 * this.t24;
        this.t277 = ex * ix;
        this.t278 = ey * iy;
        this.t279 = this.t277 * this.t278;
        this.t282 = this.t66 * this.t23;
        this.t283 = this.t15 * this.t6;
        this.t284 = this.t16 * this.t7;
        this.t285 = this.t283 * this.t284;
        this.t288 = this.t112 * this.t114;
        this.t289 = this.t277 * this.t288;
        this.t292 = this.t42 * this.t24;
        this.t295 =
            0.3294720e7 * this.t251 * this.t144 + 0.8236800e7 * this.t254 * this.t149 + 0.10982400e8 * this.t257
                * this.t154 + 0.8236800e7 * this.t260
                * this.t159 + 0.10762752e8 * this.t263 * this.t115 + 0.16144128e8 * this.t266 * this.t106
                + 0.10762752e8 * this.t269 * this.t101
                + 0.1379840e7 * this.t272 * this.t94 + 0.3675e4 * this.t19 + 0.242161920e9 * this.t276 * this.t279
                - 0.44478720e8 * this.t282 * this.t285
                - 0.29652480e8 * this.t282 * this.t289 + 0.40772160e8 * this.t292 * this.t279;
        this.t297 = this.t59 * this.t24;
        this.t298 = this.t97 * this.t99;
        this.t299 = this.t298 * this.t278;
        this.t304 = this.t28 * this.t23;
        this.t307 = this.t39 * this.t23;
        this.t312 = this.t59 * this.t25;
        this.t317 = this.t52 * this.t25;
        this.t322 = this.t34 * this.t36;
        this.t329 = this.t77 * this.t79;
        this.t332 =
            0.77014080e8 * this.t297 * this.t299 + 0.115521120e9 * this.t297 * this.t285 - 0.6054048e7 * this.t304
                * this.t279 - 0.37669632e8
                * this.t307 * this.t279 + 0.77014080e8 * this.t297 * this.t289 - 0.479198720e9 * this.t312 * this.t279
                - 0.29652480e8 * this.t282 * this.t299
                - 0.83431920e8 * this.t317 * this.t279 + 0.862400e6 * this.t45 * this.t283 + 0.672672e6 * this.t39
                * this.t322 + 0.129360e6 * this.t63
                * this.t283 + 0.862400e6 * this.t45 * this.t284 + 0.672672e6 * this.t39 * this.t329;
        this.t339 = this.t72 * this.t74;
        this.t358 = this.t85 * this.t87;
        this.t361 =
            0.129360e6 * this.t63 * this.t284 - 0.152472320e9 * this.t132 * this.t284 - 0.152472320e9 * this.t132
                * this.t283 - 0.2333760e7 * this.t139
                * this.t339 + 0.65345280e8 * this.t170 * this.t322 - 0.13453440e8 * this.t180 * this.t283
                + 0.80720640e8 * this.t121 * this.t283
                - 0.26906880e8 * this.t125 * this.t322 + 0.65345280e8 * this.t170 * this.t329 - 0.26906880e8
                * this.t125 * this.t329 + 0.80720640e8
                * this.t121 * this.t284 - 0.13453440e8 * this.t180 * this.t284 - 0.2333760e7 * this.t139 * this.t358;
        this.t364 = this.t62 + this.t109 + this.t169 + this.t212 + this.t250 + this.t295 + this.t332 + this.t361;
        this.t365 = this.t14 * this.t364;
        this.t366 = a * a;
        this.t368 = this.t366 * this.t366;
        this.t369 = this.t368 * this.t368;
        this.t372 = 0.1e1 - this.t15 - this.t16;
        this.t373 = this.t372 * this.t372;
        this.t374 = this.t373 * this.t373;
        this.t375 = this.t374 * this.t374;
        this.t376 = MathLib.sqrt(this.t372);
        this.t378 = 0.1e1 / this.t376 / this.t375;
        this.t385 = 0.1e1 / this.t369 / this.t366;
        this.t386 = this.t385 * this.t378;
        this.t391 = this.t140 * this.t20;
        this.t396 = ex * this.t51;
        this.t399 = this.t247 * this.t25;
    }

    /**
     * Partial derivative due to 9th order Earth potential zonal harmonics.
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
            derParUdeg9_2(final double a, final double ex, final double ey, final double ix, final double iy) {
        this.t401 = this.t263 * this.t24;
        this.t403 = this.t97 * this.t8;
        this.t408 = this.t272 * this.t23;
        this.t412 = this.t284 * ex;
        this.t417 = this.t45 * this.t15;
        this.t422 =
            0.70560e5 * ex + 0.1345344e7 * this.t391 * this.t36 + 0.10762752e8 * this.t269 * this.t36 + 0.419298880e9
                * this.t396 * this.t53
                - 0.242161920e9 * this.t399 + 0.47087040e8 * this.t401 + 0.1724800e7 * this.t403 * this.t6
                + 0.3294720e7 * this.t251 * this.t74
                - 0.3449600e7 * this.t408 + 0.1379840e7 * this.t272 * this.t6 - 0.166863840e9 * this.t312 * this.t412
                + 0.81544320e8 * this.t276
                * this.t412 + 0.1034880e7 * this.t417 * this.t94 - 0.322882560e9 * this.t129 * this.t101;
        this.t437 = this.t260 * this.t24;
        this.t450 = this.t266 * this.t23;
        this.t455 =
            -0.322882560e9 * this.t136 * this.t106 - 0.70012800e8 * this.t147 * this.t144 - 0.140025600e9 * this.t152
                * this.t149 - 0.140025600e9
                * this.t157 * this.t154 - 0.70012800e8 * this.t162 * this.t159 + 0.784143360e9 * this.t177 * this.t106
                + 0.784143360e9 * this.t171 * this.t101
                + 0.484323840e9 * this.t437 * this.t94 - 0.88957440e8 * this.t126 * this.t106 - 0.59304960e8
                * this.t129 * this.t115 + 0.154028160e9
                * this.t34 * this.t51 * this.t24 * this.t101 + 0.231042240e9 * this.t174 * this.t106 - 0.75339264e8
                * this.t450 * this.t94 + 0.154028160e9
                * this.t171 * this.t115;
        this.t461 = this.t254 * this.t23;
        this.t488 =
            -0.958397440e9 * this.t15 * this.t51 * this.t25 * this.t94 - 0.59304960e8 * this.t461 * this.t101
                - 0.12108096e8 * this.t307 * this.t412
                + 0.40772160e8 * this.t292 * this.t94 - 0.29652480e8 * this.t282 * this.t115 + 0.242161920e9
                * this.t276 * this.t94 + 0.8072064e7
                * this.t111 * this.t106 + 0.8072064e7 * this.t104 * this.t101 - 0.83431920e8 * this.t317 * this.t94
                - 0.479198720e9 * this.t312 * this.t94
                + 0.77014080e8 * this.t297 * this.t115 - 0.37669632e8 * this.t307 * this.t94 - 0.6054048e7 * this.t304
                * this.t94 + 0.22050e5 * this.t18
                * ex;
        this.t491 = this.t21 * ix;
        this.t492 = this.t163 * this.t165;
        this.t495 = this.t20 * ix;
        this.t498 = this.t8 * ix;
        this.t501 = this.t25 * ex;
        this.t502 = this.t501 * this.t6;
        this.t505 = this.t24 * this.t97;
        this.t527 =
            0.117600e6 * this.t17 * ex + 0.3294720e7 * this.t491 * this.t492 + 0.10762752e8 * this.t495 * this.t288
                + 0.1379840e7 * this.t498
                * this.t278 - 0.479198720e9 * this.t59 * this.t502 + 0.77014080e8 * this.t59 * this.t505 * this.t36
                + 0.1724800e7 * this.t45 * this.t94
                + 0.2690688e7 * this.t39 * this.t115 + 0.258720e6 * this.t63 * this.t94 + 0.161441280e9 * this.t121
                * this.t94 - 0.304944640e9 * this.t132
                * this.t94 - 0.107627520e9 * this.t125 * this.t115 - 0.14002560e8 * this.t139 * this.t166
                + 0.261381120e9 * this.t170 * this.t115
                - 0.26906880e8 * this.t180 * this.t94;
        this.t530 = this.t24 * ex;
        this.t531 = this.t530 * this.t6;
        this.t534 = this.t23 * ex;
        this.t535 = this.t534 * this.t6;
        this.t544 = this.t23 * this.t97;
        this.t568 =
            0.40772160e8 * this.t42 * this.t531 - 0.6054048e7 * this.t28 * this.t535 + 0.242161920e9 * this.t66
                * this.t531 - 0.83431920e8 * this.t52
                * this.t502 - 0.37669632e8 * this.t39 * this.t535 - 0.29652480e8 * this.t66 * this.t544 * this.t36
                + 0.517440e6 * this.t45 * this.t412
                - 0.166863840e9 * this.t59 * this.t25 * this.t97 * this.t6 + 0.81544320e8 * this.t66 * this.t505
                * this.t6 - 0.12108096e8 * this.t39 * this.t544
                * this.t6 + 0.38507040e8 * this.t396 * this.t226 + 0.242161920e9 * this.t247 * this.t234 - 0.37669632e8
                * this.t263 * this.t197
                - 0.14826240e8 * this.t247 * this.t240;
        this.t591 = ex * this.t6;
        this.t592 = this.t591 * this.t284;
        this.t595 = this.t99 * this.t15;
        this.t596 = this.t595 * this.t278;
        this.t601 =
            -0.479198720e9 * this.t396 * this.t230 + 0.16473600e8 * this.t254 * this.t144 + 0.32947200e8 * this.t257
                * this.t149 + 0.32947200e8
                * this.t260 * this.t154 + 0.16473600e8 * this.t247 * this.t159 + 0.32288256e8 * this.t263 * this.t106
                + 0.32288256e8 * this.t266 * this.t101
                + 0.3449600e7 * this.t69 * this.t94 + 0.5381376e7 * this.t35 * this.t101 + 0.8072064e7 * this.t269
                * this.t106 + 0.5381376e7 * this.t266
                * this.t115 - 0.88957440e8 * this.t282 * this.t592 + 0.231042240e9 * this.t297 * this.t596
                + 0.231042240e9 * this.t297 * this.t592;
        this.t605 = this.t15 * ix;
        this.t606 = this.t605 * this.t278;
        this.t613 = this.t97 * this.t6;
        this.t625 = this.t97 * this.t36;
        this.t637 =
            -0.88957440e8 * this.t282 * this.t596 - 0.333727680e9 * this.t312 * this.t606 - 0.24216192e8 * this.t307
                * this.t606 + 0.163088640e9
                * this.t276 * this.t606 + 0.517440e6 * this.t45 * this.t613 + 0.1724800e7 * this.t272 * this.t284
                + 0.1345344e7 * this.t263 * this.t329
                - 0.304944640e9 * this.t132 * this.t591 - 0.14002560e8 * this.t139 * this.t140 * this.t74
                + 0.261381120e9 * this.t170 * this.t625
                - 0.26906880e8 * this.t180 * this.t591 + 0.161441280e9 * this.t121 * this.t591 - 0.107627520e9
                * this.t125 * this.t625
                + 0.242161920e9 * this.t257 * this.t24 * this.t6;
        this.t660 = this.t53 * ex;
        this.t675 =
            -0.37669632e8 * this.t269 * this.t23 * this.t6 - 0.14826240e8 * this.t251 * this.t23 * this.t36
                - 0.479198720e9 * this.t97 * this.t51 * this.t25 * this.t6
                + 0.38507040e8 * this.t140 * this.t51 * this.t24 * this.t36 - 0.1164240e7 * this.t63 * this.t534
                - 0.88339680e8 * this.t42 * this.t501
                - 0.443963520e9 * this.t66 * this.t501 + 0.16648632e8 * this.t28 * this.t530 + 0.778697920e9 * this.t59
                * this.t660 + 0.156434850e9
                * this.t52 * this.t660 - 0.6036800e7 * this.t45 * this.t534 + 0.84756672e8 * this.t39 * this.t530
                + 0.1724800e7 * this.t45 * this.t591
                + 0.2690688e7 * this.t39 * this.t625 + 0.258720e6 * this.t63 * this.t591;
        this.t686 = this.t385 / this.t376 / this.t375 / this.t372;
        this.t697 = ey * this.t21;
        this.t700 = ey * this.t8;
        this.t709 = this.t20 * this.t112;
        this.t712 = ey * this.t51;
        this.t715 = ey * this.t20;
        this.t720 = this.t21 * this.t163;
        this.t723 = this.t277 * iy;
        this.t728 = this.t298 * iy;
        this.t733 =
            0.70560e5 * ey - 0.242161920e9 * this.t697 * this.t25 - 0.3449600e7 * this.t700 * this.t23 + 0.1724800e7
                * this.t112 * this.t8 * this.t7
                + 0.1345344e7 * this.t163 * this.t20 * this.t79 + 0.10762752e8 * this.t709 * this.t79 + 0.419298880e9
                * this.t712 * this.t53
                + 0.47087040e8 * this.t715 * this.t24 + 0.1379840e7 * this.t700 * this.t7 + 0.3294720e7 * this.t720
                * this.t87 + 0.242161920e9 * this.t276
                * this.t723 + 0.40772160e8 * this.t292 * this.t723 + 0.77014080e8 * this.t297 * this.t728 - 0.6054048e7
                * this.t304 * this.t723;
        this.t742 = this.t283 * ey;
        this.t749 = ix * this.t16;
        this.t750 = this.t749 * this.t114;
        this.t753 = this.t6 * ey;
        this.t754 = this.t753 * this.t7;
        this.t764 = this.t36 * ey;
        this.t765 = this.t764 * this.t7;
        this.t768 = this.t99 * this.t16;
        this.t769 = this.t768 * this.t114;
        this.t772 =
            -0.37669632e8 * this.t307 * this.t723 - 0.479198720e9 * this.t312 * this.t723 - 0.29652480e8 * this.t282
                * this.t728 - 0.83431920e8
                * this.t317 * this.t723 - 0.166863840e9 * this.t312 * this.t742 + 0.81544320e8 * this.t276 * this.t742
                - 0.12108096e8 * this.t307 * this.t742
                + 0.8072064e7 * this.t111 * this.t750 + 0.8072064e7 * this.t104 * this.t754 + 0.1034880e7 * this.t93
                * this.t749 * iy - 0.322882560e9
                * this.t129 * this.t754 - 0.322882560e9 * this.t136 * this.t750 - 0.70012800e8 * this.t147 * this.t765
                - 0.140025600e9 * this.t152 * this.t769;
        this.t774 = this.t6 * this.t112;
        this.t775 = this.t774 * this.t79;
        this.t778 = ix * this.t77;
        this.t779 = this.t778 * this.t165;
        this.t786 = this.t16 * this.t21;
        this.t790 = this.t112 * this.t21;
        this.t792 = this.t283 * this.t7;
        this.t797 = this.t277 * this.t114;
        this.t800 = this.t16 * this.t51;
        this.t804 = this.t112 * this.t51;
        this.t808 = this.t16 * this.t20;
        this.t824 =
            -0.140025600e9 * this.t157 * this.t775 - 0.70012800e8 * this.t162 * this.t779 + 0.784143360e9 * this.t171
                * this.t754 + 0.784143360e9
                * this.t177 * this.t750 + 0.484323840e9 * this.t786 * this.t24 * this.t723 - 0.88957440e8 * this.t790
                * this.t23 * this.t792 - 0.59304960e8
                * this.t77 * this.t21 * this.t23 * this.t797 + 0.154028160e9 * this.t800 * this.t24 * this.t728
                + 0.231042240e9 * this.t804 * this.t24 * this.t792
                - 0.75339264e8 * this.t808 * this.t23 * this.t723 + 0.154028160e9 * this.t77 * this.t51 * this.t24
                * this.t797 - 0.958397440e9 * this.t800
                * this.t25 * this.t723 - 0.59304960e8 * this.t786 * this.t23 * this.t728 + 0.22050e5 * this.t18 * ey;
        this.t827 = this.t112 * this.t7;
        this.t830 = ey * this.t7;
        this.t833 = this.t112 * this.t79;
        this.t859 = this.t23 * ey;
        this.t868 =
            0.117600e6 * this.t17 * ey + 0.517440e6 * this.t45 * this.t827 - 0.304944640e9 * this.t132 * this.t830
                + 0.261381120e9 * this.t170
                * this.t833 - 0.107627520e9 * this.t125 * this.t833 - 0.26906880e8 * this.t180 * this.t830
                - 0.14002560e8 * this.t139 * this.t163 * this.t87
                + 0.38507040e8 * this.t163 * this.t51 * this.t24 * this.t79 + 0.242161920e9 * this.t790 * this.t24
                * this.t7 - 0.37669632e8 * this.t709 * this.t23
                * this.t7 - 0.14826240e8 * this.t720 * this.t23 * this.t79 - 0.479198720e9 * this.t804 * this.t25
                * this.t7 - 0.1164240e7 * this.t63 * this.t859
                + 0.3294720e7 * this.t251 * this.t142 * iy + 0.10762752e8 * this.t269 * this.t99 * iy;
    }

    /**
     * Partial derivative due to 9th order Earth potential zonal harmonics.
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
            derParUdeg9_3(final double a, final double ex, final double ey, final double ix, final double iy) {
        this.t874 = this.t25 * ey;
        this.t879 = this.t24 * ey;
        this.t882 = this.t53 * ey;
        this.t903 =
            0.1379840e7 * this.t272 * ix * iy - 0.88339680e8 * this.t42 * this.t874 - 0.443963520e9 * this.t66
                * this.t874 + 0.16648632e8
                * this.t28 * this.t879 + 0.778697920e9 * this.t59 * this.t882 + 0.156434850e9 * this.t52 * this.t882
                - 0.6036800e7 * this.t45 * this.t859
                + 0.84756672e8 * this.t39 * this.t879 + 0.1724800e7 * this.t45 * this.t830 + 0.2690688e7 * this.t39
                * this.t833 + 0.258720e6 * this.t63
                * this.t830 + 0.161441280e9 * this.t121 * this.t830 + 0.1724800e7 * this.t700 * this.t283 + 0.1345344e7
                * this.t715 * this.t322;
        this.t932 = this.t859 * this.t7;
        this.t935 =
            0.16473600e8 * this.t247 * this.t779 + 0.16473600e8 * this.t254 * this.t765 + 0.32947200e8 * this.t257
                * this.t769 + 0.32947200e8
                * this.t260 * this.t775 + 0.32288256e8 * this.t263 * this.t750 + 0.32288256e8 * this.t266 * this.t754
                + 0.3449600e7 * this.t82 * this.t723
                + 0.5381376e7 * this.t808 * this.t728 + 0.8072064e7 * this.t709 * this.t792 + 0.5381376e7 * this.t78
                * this.t797 - 0.479198720e9
                * this.t712 * this.t201 + 0.38507040e8 * this.t712 * this.t222 - 0.166863840e9 * this.t59 * this.t25
                * this.t112 * this.t7 - 0.6054048e7 * this.t28
                * this.t932;
        this.t937 = this.t24 * this.t112;
        this.t941 = this.t874 * this.t7;
        this.t944 = this.t879 * this.t7;
        this.t949 = this.t23 * this.t112;
        this.t973 =
            0.77014080e8 * this.t59 * this.t937 * this.t79 - 0.83431920e8 * this.t52 * this.t941 + 0.242161920e9
                * this.t66 * this.t944 - 0.37669632e8
                * this.t39 * this.t932 - 0.29652480e8 * this.t66 * this.t949 * this.t79 + 0.40772160e8 * this.t42
                * this.t944 - 0.479198720e9 * this.t59 * this.t941
                + 0.1724800e7 * this.t45 * this.t723 + 0.2690688e7 * this.t39 * this.t728 + 0.81544320e8 * this.t66
                * this.t937 * this.t7 - 0.12108096e8
                * this.t39 * this.t949 * this.t7 - 0.37669632e8 * this.t715 * this.t205 - 0.14826240e8 * this.t697
                * this.t209 + 0.517440e6 * this.t45 * this.t742;
        this.t984 = this.t140 * this.t142;
        this.t992 = this.t283 * this.t830;
        this.t995 = this.t114 * this.t16;
        this.t996 = this.t277 * this.t995;
        this.t1003 = this.t16 * iy;
        this.t1004 = this.t277 * this.t1003;
        this.t1011 =
            0.242161920e9 * this.t697 * this.t193 + 0.258720e6 * this.t63 * this.t723 + 0.161441280e9 * this.t121
                * this.t723 - 0.107627520e9
                * this.t125 * this.t728 - 0.304944640e9 * this.t132 * this.t723 - 0.14002560e8 * this.t139 * this.t984
                * iy + 0.261381120e9 * this.t170
                * this.t728 - 0.26906880e8 * this.t180 * this.t723 - 0.88957440e8 * this.t282 * this.t992
                - 0.88957440e8 * this.t282 * this.t996
                + 0.231042240e9 * this.t297 * this.t992 + 0.231042240e9 * this.t297 * this.t996 + 0.163088640e9
                * this.t276 * this.t1004
                - 0.24216192e8 * this.t307 * this.t1004 - 0.333727680e9 * this.t312 * this.t1004;
        this.t1026 = this.t1 * this.t5 / this.t9;
        this.t1033 = this.t21 * this.t53;
        this.t1047 = this.t8 * this.t24;
        this.t1050 = this.t8 * this.t34;
        this.t1059 = this.t749 * this.t7;
        this.t1061 = this.t36 * this.t99;
        this.t1067 = this.t20 * this.t25;
        this.t1070 =
            -0.261381120e9 * this.t1033 * ix - 0.53813760e8 * this.t21 * this.t142 * this.t34 - 0.1724800e7 * this.t17
                * this.t99 * this.t15
                - 0.258720e6 * this.t18 * this.t99 * this.t15 - 0.3018400e7 * this.t63 * ix - 0.15375360e8 * this.t1047
                * ix - 0.10762752e8
                * this.t1050 * this.t142 + 0.10762752e8 * this.t35 * this.t99 + 0.1379840e7 * this.t69 * ix
                + 0.15375360e8 * this.t180 * ix
                - 0.1379840e7 * this.t1059 - 0.4667520e7 * this.t51 * this.t1061 * this.t72 - 0.115315200e9 * this.t121
                * ix + 0.115315200e9
                * this.t1067 * ix;
        this.t1075 = this.t20 * this.t72;
        this.t1088 = this.t17 * ix;
        this.t1091 = this.t753 * iy;
        this.t1100 = this.t764 * iy;
        this.t1103 = this.t768 * this.t7;
        this.t1106 =
            -0.3449600e7 * this.t45 * ix - 0.388080e6 * this.t31 * ix - 0.3294720e7 * this.t1075 * this.t1061
                + 0.261381120e9 * this.t132
                * ix + 0.388080e6 * this.t19 * ix * this.t23 - 0.26906880e8 * this.t20 * this.t99 * this.t15
                + 0.3294720e7 * this.t73 * this.t142
                + 0.3449600e7 * this.t1088 * this.t23 + 0.8072064e7 * this.t98 * this.t1091 + 0.8072064e7 * this.t104
                * this.t1059 - 0.322882560e9
                * this.t126 * this.t1091 - 0.322882560e9 * this.t129 * this.t1059 - 0.70012800e8 * this.t141
                * this.t1100 - 0.140025600e9 * this.t147
                * this.t1103;
        this.t1108 = this.t774 * this.t114;
        this.t1111 = this.t778 * this.t79;
        this.t1119 = ex * ey * iy;
        this.t1124 = this.t59 * this.t23;
        this.t1129 = this.t52 * this.t24;
        this.t1142 =
            -0.140025600e9 * this.t152 * this.t1108 - 0.70012800e8 * this.t157 * this.t1111 + 0.784143360e9 * this.t171
                * this.t1059
                + 0.784143360e9 * this.t174 * this.t1091 + 0.242161920e9 * this.t276 * this.t1119 - 0.154028160e9
                * this.t276 * this.t1111
                + 0.77014080e8 * this.t1124 * this.t1111 + 0.333727680e9 * this.t43 * this.t1059 - 0.250295760e9
                * this.t1129 * this.t1059
                - 0.726485760e9 * this.t40 * this.t1059 + 0.484323840e9 * this.t282 * this.t1059 + 0.75339264e8
                * this.t46 * this.t1059
                + 0.44478720e8 * this.t307 * this.t1111 - 0.122316480e9 * this.t29 * this.t1059;
        this.t1143 = this.t42 * this.t23;
        this.t1150 = this.t45 * this.t97;
        this.t1161 = this.t180 * this.t97;
        this.t1166 = this.t605 * this.t284;
        this.t1169 = this.t613 * this.t278;
        this.t1176 =
            0.81544320e8 * this.t1143 * this.t1059 + 0.1916794880e10 * this.t67 * this.t1059 - 0.1437596160e10
                * this.t297 * this.t1059
                - 0.10762752e8 * this.t1150 * this.t1100 - 0.16144128e8 * this.t417 * this.t1103 - 0.10762752e8
                * this.t93 * this.t1108
                - 0.968647680e9 * this.t401 * this.t1091 + 0.645765120e9 * this.t136 * this.t1091 + 0.645765120e9
                * this.t1161 * this.t1100
                + 0.968647680e9 * this.t450 * this.t1103 - 0.88957440e8 * this.t282 * this.t1166 + 0.231042240e9
                * this.t297 * this.t1169
                + 0.231042240e9 * this.t297 * this.t1166 - 0.88957440e8 * this.t282 * this.t1169;
        this.t1179 = this.t591 * this.t278;
        this.t1182 = this.t595 * this.t284;
        this.t1185 = this.t591 * this.t288;
        this.t1192 = this.t625 * this.t278;
        this.t1211 =
            0.968647680e9 * this.t282 * this.t1179 + 0.266872320e9 * this.t307 * this.t1182 + 0.177914880e9 * this.t307
                * this.t1185
                - 0.244632960e9 * this.t29 * this.t1179 + 0.163088640e9 * this.t1143 * this.t1179 - 0.616112640e9
                * this.t276 * this.t1192
                + 0.308056320e9 * this.t1124 * this.t1192 - 0.924168960e9 * this.t276 * this.t1182 + 0.462084480e9
                * this.t1124 * this.t1182
                + 0.24216192e8 * this.t64 * this.t1179 + 0.150678528e9 * this.t46 * this.t1179 - 0.616112640e9
                * this.t276 * this.t1185
                + 0.308056320e9 * this.t1124 * this.t1185 + 0.3833589760e10 * this.t67 * this.t1179;
        this.t1221 = ix * this.t23;
        this.t1230 = this.t125 * this.t140;
        this.t1232 = this.t74 * ey * iy;
        this.t1236 = this.t142 * this.t16 * this.t7;
        this.t1240 = this.t36 * this.t112 * this.t114;
        this.t1244 = this.t99 * this.t77 * this.t79;
        this.t1247 =
            -0.2875192320e10 * this.t297 * this.t1179 + 0.177914880e9 * this.t307 * this.t1192 + 0.667455360e9
                * this.t43 * this.t1179
                - 0.500591520e9 * this.t1129 * this.t1179 - 0.591360e6 * this.t498 + 0.591360e6 * this.t1221
                - 0.1379840e7 * this.t595
                + 0.2439557120e10 * this.t399 * this.t1091 - 0.1829667840e10 * this.t177 * this.t1091 + 0.645765120e9
                * this.t181 * this.t1108
                + 0.112020480e9 * this.t1230 * this.t1232 + 0.280051200e9 * this.t461 * this.t1236 + 0.373401600e9
                * this.t126 * this.t1240
                + 0.280051200e9 * this.t129 * this.t1244;
        this.t1250 = this.t6 * this.t163 * this.t165;
        this.t1257 = this.t121 * this.t97;
        this.t1270 = this.t15 * this.t16 * this.t7;
        this.t1275 = ex * this.t112 * this.t114;
        this.t1286 = this.t97 * ey * iy;
        this.t1291 =
            0.112020480e9 * this.t136 * this.t1250 - 0.3136573440e10 * this.t437 * this.t1103 + 0.1568286720e10
                * this.t157 * this.t1103
                - 0.2091048960e10 * this.t1257 * this.t1100 + 0.1045524480e10 * this.t152 * this.t1100
                - 0.2091048960e10 * this.t122 * this.t1108
                + 0.1045524480e10 * this.t162 * this.t1108 + 0.107627520e9 * this.t408 * this.t1091 - 0.88957440e8
                * this.t66 * this.t99 * this.t1270
                - 0.59304960e8 * this.t66 * this.t6 * this.t1275 - 0.12108096e8 * this.t28 * this.t6 * this.t1119
                - 0.75339264e8 * this.t39 * this.t6 * this.t1119
                - 0.59304960e8 * this.t66 * this.t36 * this.t1286 - 0.29652480e8 * this.t282 * this.t1275;
        this.t1307 = this.t34 * this.t142;
        this.t1318 = this.t34 * this.t99;
        this.t1323 =
            0.40772160e8 * this.t292 * this.t1119 - 0.6054048e7 * this.t304 * this.t1119 - 0.37669632e8 * this.t307
                * this.t1119 + 0.77014080e8
                * this.t297 * this.t1275 - 0.479198720e9 * this.t312 * this.t1119 - 0.83431920e8 * this.t317
                * this.t1119 + 0.12108096e8 * this.t64
                * this.t1059 - 0.2759680e7 * this.t1179 - 0.2690688e7 * this.t45 * this.t1307 + 0.3294720e7 * this.t247
                * this.t492 + 0.10762752e8
                * this.t263 * this.t288 + 0.1379840e7 * this.t272 * this.t278 + 0.1724800e7 * this.t45 * this.t605
                + 0.2690688e7 * this.t39 * this.t1318
                + 0.258720e6 * this.t63 * this.t605;
        this.t1340 = this.t8 * this.t77;
        this.t1344 = this.t20 * this.t85;
        this.t1348 = this.t18 * ix;
        this.t1362 =
            -0.304944640e9 * this.t132 * this.t605 - 0.14002560e8 * this.t139 * this.t72 * this.t142 + 0.261381120e9
                * this.t170 * this.t1318
                - 0.26906880e8 * this.t180 * this.t605 + 0.161441280e9 * this.t121 * this.t605 - 0.107627520e9
                * this.t125 * this.t1318
                - 0.10762752e8 * this.t1340 * this.t79 * ix - 0.3294720e7 * this.t1344 * this.t87 * ix - 0.258720e6
                * this.t1348 * this.t284
                + 0.1219778560e10 * this.t188 * this.t595 - 0.914833920e9 * this.t170 * this.t595 + 0.18670080e8
                * this.t125 * this.t72 * this.t1061
                - 0.522762240e9 * this.t121 * this.t1307 + 0.261381120e9 * this.t139 * this.t1307;
        this.t1384 = this.t25 * ix;
        this.t1387 = this.t24 * ix;
        this.t1394 =
            0.53813760e8 * this.t190 * this.t595 - 0.484323840e9 * this.t184 * this.t595 + 0.322882560e9 * this.t125
                * this.t595 + 0.161441280e9
                * this.t180 * this.t1307 - 0.53813760e8 * this.t491 * this.t329 - 0.26906880e8 * this.t495 * this.t284
                - 0.4667520e7 * this.t51 * ix
                * this.t358 - 0.37669632e8 * this.t39 * this.t595 - 0.14826240e8 * this.t66 * this.t1307 - 0.6054048e7
                * this.t28 * this.t595
                + 0.88339680e8 * this.t48 * this.t1384 - 0.88339680e8 * this.t22 * this.t1387 + 0.665945280e9
                * this.t28 * this.t1384 - 0.665945280e9
                * this.t42 * this.t1387;
        this.t1404 = this.t53 * ix;
        this.t1425 =
            -0.94174080e8 * this.t45 * this.t1387 + 0.94174080e8 * this.t39 * this.t1221 - 0.11099088e8 * this.t31
                * this.t1387 + 0.11099088e8
                * this.t48 * this.t1221 - 0.1557395840e10 * this.t42 * this.t1404 + 0.1557395840e10 * this.t52
                * this.t1384 - 0.208579800e9 * this.t22
                * this.t1404 + 0.208579800e9 * this.t56 * this.t1384 - 0.1677195520e10 * this.t66 * this.t1404
                + 0.1677195520e10 * this.t59 * this.t1384
                - 0.84756672e8 * this.t63 * this.t1387 + 0.84756672e8 * this.t28 * this.t1221 + 0.726485760e9
                * this.t39 * this.t1384 - 0.726485760e9
                * this.t66 * this.t1387;
        this.t1428 = this.t208 * this.t142;
        this.t1431 = this.t192 * this.t99;
        this.t1434 = this.t204 * this.t99;
        this.t1458 =
            -0.1724800e7 * this.t1088 * this.t284 + 0.44478720e8 * this.t39 * this.t1428 - 0.122316480e9 * this.t28
                * this.t1431 + 0.81544320e8
                * this.t42 * this.t1434 + 0.12108096e8 * this.t63 * this.t1434 - 0.1437596160e10 * this.t59
                * this.t1431 - 0.154028160e9 * this.t66 * this.t221
                * this.t142 + 0.77014080e8 * this.t59 * this.t1428 - 0.37669632e8 * this.t39 * this.t1059
                - 0.14826240e8 * this.t66 * this.t1111
                - 0.19768320e8 * this.t263 * this.t1250 - 0.19768320e8 * this.t391 * this.t1232 - 0.49420800e8
                * this.t35 * this.t1236 - 0.65894400e8
                * this.t269 * this.t1240;
        this.t1506 = ex * this.t163 * this.t165;
        this.t1509 =
            -0.49420800e8 * this.t266 * this.t1244 - 0.43051008e8 * this.t272 * this.t1108 - 0.64576512e8 * this.t69
                * this.t1103 - 0.43051008e8
                * this.t403 * this.t1100 - 0.3449600e7 * this.t17 * this.t6 * this.t1119 - 0.517440e6 * this.t18
                * this.t6 * this.t1119 - 0.215255040e9 * this.t21
                * this.t36 * this.t1286 - 0.322882560e9 * this.t21 * this.t99 * this.t1270 - 0.215255040e9 * this.t21
                * this.t6 * this.t1275 - 0.28005120e8
                * this.t51 * this.t74 * this.t140 * ey * iy - 0.70012800e8 * this.t51 * this.t142 * this.t34 * this.t16
                * this.t7 - 0.93350400e8 * this.t51 * this.t36
                * this.t97 * this.t112 * this.t114 - 0.70012800e8 * this.t51 * this.t99 * this.t15 * this.t77
                * this.t79 - 0.28005120e8 * this.t51 * this.t6 * this.t1506;
        this.t1513 = this.t200 * ix;
        this.t1541 =
            -0.53813760e8 * this.t20 * this.t6 * this.t1119 - 0.479198720e9 * this.t59 * this.t1513 + 0.77014080e8
                * this.t59 * this.t221 * this.t99
                + 0.16473600e8 * this.t251 * this.t1100 + 0.32947200e8 * this.t254 * this.t1103 + 0.32947200e8
                * this.t257 * this.t1108
                + 0.16473600e8 * this.t260 * this.t1111 + 0.32288256e8 * this.t266 * this.t1059 + 0.32288256e8
                * this.t269 * this.t1091 + 0.1724800e7
                * this.t45 * this.t1119 + 0.2690688e7 * this.t39 * this.t1275 + 0.258720e6 * this.t63 * this.t1119
                + 0.161441280e9 * this.t121 * this.t1119
                - 0.304944640e9 * this.t132 * this.t1119;
        this.t1551 = this.t200 * this.t99;
    }

    /**
     * Partial derivative due to 9th order Earth potential zonal harmonics.
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
            derParUdeg9_4(final double a, final double ex, final double ey, final double ix, final double iy) {
        this.t1572 =
            -0.107627520e9 * this.t125 * this.t1275 - 0.14002560e8 * this.t139 * this.t1506 + 0.261381120e9 * this.t170
                * this.t1275
                - 0.26906880e8 * this.t180 * this.t1119 + 0.1916794880e10 * this.t66 * this.t1551 - 0.2690688e7
                * this.t45 * this.t1111
                + 0.1219778560e10 * this.t188 * this.t1059 - 0.914833920e9 * this.t170 * this.t1059 - 0.522762240e9
                * this.t121 * this.t1111
                + 0.261381120e9 * this.t139 * this.t1111 + 0.161441280e9 * this.t180 * this.t1111 - 0.484323840e9
                * this.t184 * this.t1059
                + 0.322882560e9 * this.t125 * this.t1059 + 0.53813760e8 * this.t190 * this.t1059;
        this.t1588 = this.t192 * ix;
        this.t1593 = this.t204 * ix;
        this.t1607 =
            0.18670080e8 * this.t125 * this.t358 * ix - 0.726485760e9 * this.t39 * this.t1431 + 0.484323840e9
                * this.t66 * this.t1434
                - 0.6054048e7 * this.t28 * this.t1059 + 0.333727680e9 * this.t42 * this.t1551 - 0.250295760e9
                * this.t52 * this.t1431 + 0.75339264e8
                * this.t45 * this.t1434 + 0.242161920e9 * this.t66 * this.t1588 - 0.83431920e8 * this.t52 * this.t1513
                - 0.37669632e8 * this.t39 * this.t1593
                - 0.29652480e8 * this.t66 * this.t208 * this.t99 + 0.40772160e8 * this.t42 * this.t1588 - 0.6054048e7
                * this.t28 * this.t1593
                + 0.3018400e7 * this.t1348 * this.t23 - 0.1452971520e10 * this.t40 * this.t1179;
        this.t1624 = this.t17 * iy;
        this.t1629 = this.t18 * iy;
        this.t1634 = this.t79 * this.t114;
        this.t1638 = iy * this.t15 * this.t6;
        this.t1655 =
            -0.3018400e7 * this.t63 * iy + 0.3449600e7 * this.t1624 * this.t23 - 0.388080e6 * this.t31 * iy
                + 0.3018400e7 * this.t1629 * this.t23
                - 0.10762752e8 * this.t1340 * this.t165 - 0.3294720e7 * this.t1344 * this.t1634 - 0.1379840e7
                * this.t1638 + 0.1379840e7 * this.t82
                * iy - 0.3449600e7 * this.t45 * iy + 0.261381120e9 * this.t132 * iy + 0.10762752e8 * this.t78
                * this.t114 + 0.388080e6
                * this.t19 * iy * this.t23 - 0.15375360e8 * this.t1047 * iy + 0.115315200e9 * this.t1067 * iy;
        this.t1680 = this.t7 * ex * this.t13;
        this.t1683 = this.t298 * this.t830;
        this.t1690 = this.t283 * this.t1003;
        this.t1693 =
            -0.261381120e9 * this.t1033 * iy + 0.3294720e7 * this.t86 * this.t165 + 0.15375360e8 * this.t180 * iy
                - 0.115315200e9
                * this.t121 * iy - 0.4667520e7 * this.t51 * this.t1634 * this.t85 - 0.26906880e8 * this.t20 * this.t114
                * this.t16 - 0.53813760e8 * this.t21
                * this.t165 * this.t77 - 0.258720e6 * this.t18 * this.t114 * this.t16 - 0.1724800e7 * this.t17
                * this.t114 * this.t16 - 0.2875192320e10 * this.t297
                * this.t1680 + 0.177914880e9 * this.t307 * this.t1683 + 0.667455360e9 * this.t43 * this.t1680
                - 0.500591520e9 * this.t1129 * this.t1680
                - 0.88957440e8 * this.t282 * this.t1690;
        this.t1695 = this.t277 * this.t827;
        this.t1706 = this.t283 * this.t995;
        this.t1709 = this.t277 * this.t833;
        this.t1716 = this.t164 * this.t87;
        this.t1719 = this.t143 * this.t7;
        this.t1722 = this.t148 * this.t114;
        this.t1725 = this.t153 * this.t79;
        this.t1728 = this.t158 * this.t165;
        this.t1731 =
            -0.88957440e8 * this.t282 * this.t1695 + 0.231042240e9 * this.t297 * this.t1690 + 0.231042240e9 * this.t297
                * this.t1695
                - 0.1452971520e10 * this.t40 * this.t1680 + 0.968647680e9 * this.t282 * this.t1680 + 0.266872320e9
                * this.t307 * this.t1706
                + 0.177914880e9 * this.t307 * this.t1709 - 0.244632960e9 * this.t29 * this.t1680 + 0.163088640e9
                * this.t1143 * this.t1680
                - 0.19768320e8 * this.t263 * this.t1716 - 0.19768320e8 * this.t391 * this.t1719 - 0.49420800e8
                * this.t35 * this.t1722 - 0.65894400e8
                * this.t269 * this.t1725 - 0.49420800e8 * this.t266 * this.t1728;
        this.t1732 = this.t113 * this.t79;
        this.t1735 = this.t105 * this.t114;
        this.t1738 = this.t100 * this.t7;
        this.t1742 = this.t277 * ey;
        this.t1749 = this.t298 * ey;
        this.t1753 = this.t283 * this.t16;
        this.t1757 = this.t277 * this.t112;
        this.t1761 = this.t984 * ey;
        this.t1783 =
            -0.43051008e8 * this.t272 * this.t1732 - 0.64576512e8 * this.t69 * this.t1735 - 0.43051008e8 * this.t403
                * this.t1738 - 0.3449600e7
                * this.t17 * this.t7 * this.t1742 - 0.517440e6 * this.t18 * this.t7 * this.t1742 - 0.215255040e9
                * this.t21 * this.t7 * this.t1749 - 0.322882560e9
                * this.t21 * this.t114 * this.t1753 - 0.215255040e9 * this.t21 * this.t79 * this.t1757 - 0.28005120e8
                * this.t51 * this.t7 * this.t1761
                - 0.70012800e8 * this.t51 * this.t114 * this.t322 * this.t16 - 0.93350400e8 * this.t51 * this.t79
                * this.t298 * this.t112 - 0.70012800e8 * this.t51
                * this.t165 * this.t283 * this.t77 - 0.28005120e8 * this.t51 * this.t87 * this.t277 * this.t163
                - 0.53813760e8 * this.t20 * this.t7 * this.t1742;
        this.t1786 = this.t148 * iy;
        this.t1789 = this.t153 * this.t7;
        this.t1792 = this.t158 * this.t114;
        this.t1795 = this.t113 * this.t7;
        this.t1798 = this.t105 * iy;
        this.t1819 =
            0.16473600e8 * this.t254 * this.t1786 + 0.32947200e8 * this.t257 * this.t1789 + 0.32947200e8 * this.t260
                * this.t1792 + 0.32288256e8
                * this.t263 * this.t1795 + 0.32288256e8 * this.t266 * this.t1798 + 0.1724800e7 * this.t45 * this.t1742
                + 0.2690688e7 * this.t39 * this.t1749
                - 0.616112640e9 * this.t276 * this.t1683 + 0.308056320e9 * this.t1124 * this.t1683 - 0.924168960e9
                * this.t276 * this.t1706
                + 0.462084480e9 * this.t1124 * this.t1706 + 0.24216192e8 * this.t64 * this.t1680 + 0.150678528e9
                * this.t46 * this.t1680
                - 0.616112640e9 * this.t276 * this.t1709;
        this.t1834 = this.t322 * iy;
        this.t1849 =
            0.308056320e9 * this.t1124 * this.t1709 + 0.3833589760e10 * this.t67 * this.t1680 - 0.726485760e9
                * this.t40 * this.t1638
                + 0.484323840e9 * this.t282 * this.t1638 + 0.333727680e9 * this.t43 * this.t1638 - 0.250295760e9
                * this.t1129 * this.t1638
                + 0.75339264e8 * this.t46 * this.t1638 + 0.44478720e8 * this.t307 * this.t1834 - 0.122316480e9
                * this.t29 * this.t1638 + 0.81544320e8
                * this.t1143 * this.t1638 + 0.12108096e8 * this.t64 * this.t1638 + 0.1916794880e10 * this.t67
                * this.t1638 - 0.1437596160e10 * this.t297
                * this.t1638 - 0.154028160e9 * this.t276 * this.t1834;
        this.t1861 = this.t13 * this.t7;
        this.t1880 =
            0.77014080e8 * this.t1124 * this.t1834 + 0.8072064e7 * this.t104 * this.t1798 + 0.8072064e7 * this.t111
                * this.t1795 - 0.16144128e8
                * this.t417 * this.t1735 - 0.10762752e8 * this.t93 * this.t1732 - 0.968647680e9 * this.t401
                * this.t1861 + 0.645765120e9 * this.t136
                * this.t1861 + 0.645765120e9 * this.t1161 * this.t1738 + 0.968647680e9 * this.t450 * this.t1735
                + 0.2439557120e10 * this.t399 * this.t1861
                - 0.1829667840e10 * this.t177 * this.t1861 + 0.645765120e9 * this.t181 * this.t1732 + 0.112020480e9
                * this.t1230 * this.t1719
                + 0.280051200e9 * this.t461 * this.t1722;
        this.t1916 =
            0.373401600e9 * this.t126 * this.t1725 + 0.280051200e9 * this.t129 * this.t1728 + 0.112020480e9 * this.t136
                * this.t1716
                - 0.3136573440e10 * this.t437 * this.t1735 + 0.1568286720e10 * this.t157 * this.t1735 - 0.2091048960e10
                * this.t1257 * this.t1738
                + 0.1045524480e10 * this.t152 * this.t1738 - 0.2091048960e10 * this.t122 * this.t1732 + 0.1045524480e10
                * this.t162 * this.t1732
                + 0.107627520e9 * this.t408 * this.t1861 - 0.88957440e8 * this.t66 * this.t114 * this.t1753
                - 0.59304960e8 * this.t66 * this.t79 * this.t1757
                - 0.12108096e8 * this.t28 * this.t7 * this.t1742 - 0.75339264e8 * this.t39 * this.t7 * this.t1742
                - 0.59304960e8 * this.t66 * this.t7 * this.t1749;
        this.t1944 = this.t164 * this.t79;
        this.t1949 =
            0.77014080e8 * this.t297 * this.t1749 - 0.6054048e7 * this.t304 * this.t1742 - 0.37669632e8 * this.t307
                * this.t1742 - 0.479198720e9
                * this.t312 * this.t1742 - 0.29652480e8 * this.t282 * this.t1749 - 0.83431920e8 * this.t317
                * this.t1742 - 0.10762752e8 * this.t1150
                * this.t1738 - 0.322882560e9 * this.t129 * this.t1798 - 0.322882560e9 * this.t136 * this.t1795
                - 0.70012800e8 * this.t147 * this.t1786
                - 0.140025600e9 * this.t152 * this.t1789 - 0.140025600e9 * this.t157 * this.t1792 - 0.70012800e8
                * this.t162 * this.t1944
                + 0.784143360e9 * this.t171 * this.t1798;
        this.t1959 = iy * this.t23;
        this.t1963 = this.t77 * this.t114;
        this.t1979 =
            0.784143360e9 * this.t177 * this.t1795 + 0.242161920e9 * this.t276 * this.t1742 + 0.40772160e8 * this.t292
                * this.t1742 - 0.591360e6
                * this.t8 * iy - 0.1379840e7 * this.t995 + 0.591360e6 * this.t1959 - 0.304944640e9 * this.t132
                * this.t1003 + 0.261381120e9
                * this.t170 * this.t1963 - 0.107627520e9 * this.t125 * this.t1963 + 0.161441280e9 * this.t121
                * this.t1003 - 0.26906880e8 * this.t180
                * this.t1003 - 0.14002560e8 * this.t139 * this.t85 * this.t165 + 0.1724800e7 * this.t45 * this.t1003
                + 0.2690688e7 * this.t39 * this.t1963;
        this.t1987 = this.t77 * this.t165;
        this.t2013 =
            0.258720e6 * this.t63 * this.t1003 - 0.1724800e7 * this.t1624 * this.t283 - 0.258720e6 * this.t1629
                * this.t283 - 0.2690688e7 * this.t45
                * this.t1987 + 0.1219778560e10 * this.t188 * this.t995 - 0.914833920e9 * this.t170 * this.t995
                - 0.4667520e7 * this.t51 * iy * this.t339
                - 0.26906880e8 * this.t20 * iy * this.t283 - 0.53813760e8 * this.t21 * iy * this.t322 - 0.522762240e9
                * this.t121 * this.t1987
                + 0.261381120e9 * this.t139 * this.t1987 + 0.161441280e9 * this.t180 * this.t1987 - 0.484323840e9
                * this.t184 * this.t995
                + 0.322882560e9 * this.t125 * this.t995;
        this.t2026 = this.t25 * iy;
        this.t2029 = this.t24 * iy;
        this.t2044 =
            0.53813760e8 * this.t190 * this.t995 + 0.18670080e8 * this.t125 * this.t85 * this.t1634 - 0.6054048e7
                * this.t28 * this.t995
                - 0.37669632e8 * this.t39 * this.t995 - 0.14826240e8 * this.t66 * this.t1987 - 0.2759680e7 * this.t1680
                + 0.88339680e8 * this.t48
                * this.t2026 - 0.88339680e8 * this.t22 * this.t2029 + 0.665945280e9 * this.t28 * this.t2026
                - 0.665945280e9 * this.t42 * this.t2029
                - 0.94174080e8 * this.t45 * this.t2029 + 0.94174080e8 * this.t39 * this.t1959 - 0.11099088e8 * this.t31
                * this.t2029 + 0.11099088e8
                * this.t48 * this.t1959;
        this.t2047 = this.t53 * iy;
        this.t2078 =
            -0.1557395840e10 * this.t42 * this.t2047 + 0.1557395840e10 * this.t52 * this.t2026 - 0.208579800e9
                * this.t22 * this.t2047
                + 0.208579800e9 * this.t56 * this.t2026 - 0.1677195520e10 * this.t66 * this.t2047 + 0.1677195520e10
                * this.t59 * this.t2026
                - 0.84756672e8 * this.t63 * this.t2029 + 0.84756672e8 * this.t28 * this.t1959 + 0.726485760e9
                * this.t39 * this.t2026 - 0.726485760e9
                * this.t66 * this.t2029 - 0.10762752e8 * this.t1050 * this.t36 * iy - 0.3294720e7 * this.t1075
                * this.t74 * iy + 0.3294720e7 * this.t251
                * this.t143 + 0.10762752e8 * this.t269 * this.t100;
        this.t2100 = this.t196 * this.t114;
        this.t2109 =
            0.1379840e7 * this.t272 * this.t13 + 0.1219778560e10 * this.t188 * this.t1638 - 0.914833920e9 * this.t170
                * this.t1638 + 0.18670080e8
                * this.t125 * this.t339 * iy - 0.522762240e9 * this.t121 * this.t1834 + 0.261381120e9 * this.t139
                * this.t1834 + 0.53813760e8 * this.t190
                * this.t1638 - 0.484323840e9 * this.t184 * this.t1638 + 0.322882560e9 * this.t125 * this.t1638
                + 0.161441280e9 * this.t180 * this.t1834
                + 0.12108096e8 * this.t63 * this.t2100 - 0.37669632e8 * this.t39 * this.t1638 - 0.14826240e8 * this.t66
                * this.t1834 - 0.6054048e7
                * this.t28 * this.t1638;
        this.t2114 = this.t239 * this.t165;
        this.t2117 = this.t229 * this.t114;
        this.t2120 = this.t233 * this.t114;
        this.t2139 = this.t233 * iy;
        this.t2142 = this.t229 * iy;
        this.t2145 =
            -0.154028160e9 * this.t66 * this.t225 * this.t165 + 0.77014080e8 * this.t59 * this.t2114 + 0.333727680e9
                * this.t42 * this.t2117
                - 0.250295760e9 * this.t52 * this.t2120 - 0.726485760e9 * this.t39 * this.t2120 + 0.484323840e9
                * this.t66 * this.t2100
                + 0.75339264e8 * this.t45 * this.t2100 + 0.44478720e8 * this.t39 * this.t2114 - 0.122316480e9
                * this.t28 * this.t2120 + 0.81544320e8
                * this.t42 * this.t2100 + 0.1916794880e10 * this.t66 * this.t2117 - 0.1437596160e10 * this.t59
                * this.t2120 + 0.40772160e8 * this.t42
                * this.t2139 - 0.479198720e9 * this.t59 * this.t2142;
        this.t2148 = this.t196 * iy;
        this.t2179 =
            0.16473600e8 * this.t247 * this.t1944 - 0.6054048e7 * this.t28 * this.t2148 + 0.77014080e8 * this.t59
                * this.t225 * this.t114
                - 0.83431920e8 * this.t52 * this.t2142 + 0.242161920e9 * this.t66 * this.t2139 - 0.37669632e8
                * this.t39 * this.t2148 - 0.29652480e8
                * this.t66 * this.t239 * this.t114 - 0.2690688e7 * this.t45 * this.t1834 + 0.258720e6 * this.t63
                * this.t1742 + 0.161441280e9 * this.t121
                * this.t1742 - 0.107627520e9 * this.t125 * this.t1749 - 0.304944640e9 * this.t132 * this.t1742
                - 0.14002560e8 * this.t139 * this.t1761
                + 0.261381120e9 * this.t170 * this.t1749 - 0.26906880e8 * this.t180 * this.t1742;
    }
}
