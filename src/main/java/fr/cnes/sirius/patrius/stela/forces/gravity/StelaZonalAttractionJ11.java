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
 * 
 * @history created on 26/02/2015
 *
 * HISTORY
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
import fr.cnes.sirius.patrius.stela.orbits.StelaEquinoctialOrbit;

//CHECKSTYLE:OFF

/**
 * <p>
 * Class computing 11th order zonal perturbations. This class has package visibility since it is not supposed to be used
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
class StelaZonalAttractionJ11 extends AbstractStelaLagrangeContribution {

     /** Serializable UID. */
    private static final long serialVersionUID = 2455621257528599223L;

    /** The central body reference radius (m). */
    private final double rEq;
    /** The 11th order central body coefficients */
    private final double j11;

    /** Temporary coefficients. */
    private double t1;
    private double t2;
    private double t4;
    private double t5;
    private double t6;
    private double t7;
    private double t8;
    private double t9;
    private double t10;
    private double t12;
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
    private double t26;
    private double t29;
    private double t30;
    private double t31;
    private double t32;
    private double t33;
    private double t34;
    private double t35;
    private double t36;
    private double t37;
    private double t38;
    private double t39;
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
    private double t60;
    private double t63;
    private double t64;
    private double t68;
    private double t69;
    private double t70;
    private double t71;
    private double t72;
    private double t75;
    private double t76;
    private double t77;
    private double t78;
    private double t79;
    private double t82;
    private double t83;
    private double t84;
    private double t85;
    private double t86;
    private double t89;
    private double t90;
    private double t91;
    private double t94;
    private double t95;
    private double t98;
    private double t99;
    private double t100;
    private double t101;
    private double t104;
    private double t107;
    private double t110;
    private double t113;
    private double t114;
    private double t115;
    private double t116;
    private double t119;
    private double t122;
    private double t123;
    private double t126;
    private double t131;
    private double t132;
    private double t133;
    private double t134;
    private double t135;
    private double t138;
    private double t139;
    private double t140;
    private double t141;
    private double t142;
    private double t143;
    private double t146;
    private double t147;
    private double t148;
    private double t151;
    private double t152;
    private double t153;
    private double t156;
    private double t158;
    private double t159;
    private double t162;
    private double t165;
    private double t166;
    private double t167;
    private double t168;
    private double t171;
    private double t174;
    private double t175;
    private double t176;
    private double t179;
    private double t180;
    private double t183;
    private double t184;
    private double t187;
    private double t192;
    private double t195;
    private double t196;
    private double t197;
    private double t200;
    private double t201;
    private double t202;
    private double t205;
    private double t206;
    private double t207;
    private double t210;
    private double t211;
    private double t214;
    private double t215;
    private double t218;
    private double t221;
    private double t222;
    private double t231;
    private double t232;
    private double t233;
    private double t242;
    private double t243;
    private double t248;
    private double t253;
    private double t262;
    private double t263;
    private double t266;
    private double t271;
    private double t274;
    private double t283;
    private double t302;
    private double t303;
    private double t306;
    private double t313;
    private double t315;
    private double t316;
    private double t317;
    private double t320;
    private double t323;
    private double t329;
    private double t332;
    private double t335;
    private double t338;
    private double t341;
    private double t344;
    private double t347;
    private double t348;
    private double t349;
    private double t352;
    private double t355;
    private double t358;
    private double t359;
    private double t360;
    private double t361;
    private double t364;
    private double t367;
    private double t370;
    private double t373;
    private double t376;
    private double t379;
    private double t382;
    private double t383;
    private double t384;
    private double t387;
    private double t390;
    private double t393;
    private double t399;
    private double t405;
    private double t406;
    private double t409;
    private double t410;
    private double t413;
    private double t414;
    private double t417;
    private double t418;
    private double t421;
    private double t422;
    private double t429;
    private double t430;
    private double t433;
    private double t434;
    private double t437;
    private double t440;
    private double t441;
    private double t444;
    private double t447;
    private double t450;
    private double t451;
    private double t452;
    private double t453;
    private double t454;
    private double t457;
    private double t460;
    private double t463;
    private double t466;
    private double t471;
    private double t474;
    private double t479;
    private double t480;
    private double t485;
    private double t489;
    private double t490;
    private double t495;
    private double t498;
    private double t499;
    private double t502;
    private double t503;
    private double t508;
    private double t513;
    private double t514;
    private double t515;
    private double t516;
    private double t517;
    private double t522;
    private double t523;
    private double t528;
    private double t529;
    private double t530;
    private double t533;
    private double t534;
    private double t539;
    private double t540;
    private double t557;
    private double t558;
    private double t563;
    private double t566;
    private double t569;
    private double t572;
    private double t575;
    private double t576;
    private double t581;
    private double t584;
    private double t587;
    private double t592;
    private double t595;
    private double t598;
    private double t603;
    private double t605;
    private double t606;
    private double t617;
    private double t624;
    private double t626;
    private double t629;
    private double t631;
    private double t633;
    private double t635;
    private double t637;
    private double t640;
    private double t642;
    private double t644;
    private double t646;
    private double t649;
    private double t651;
    private double t653;
    private double t658;
    private double t661;
    private double t664;
    private double t667;
    private double t670;
    private double t671;
    private double t674;
    private double t677;
    private double t680;
    private double t683;
    private double t686;
    private double t690;
    private double t692;
    private double t694;
    private double t696;
    private double t698;
    private double t704;
    private double t708;
    private double t709;
    private double t710;
    private double t711;
    private double t713;
    private double t716;
    private double t717;
    private double t718;
    private double t719;
    private double t721;
    private double t723;
    private double t730;
    private double t731;
    private double t735;
    private double t738;
    private double t747;
    private double t750;
    private double t755;
    private double t758;
    private double t761;
    private double t764;
    private double t773;
    private double t776;
    private double t777;
    private double t778;
    private double t781;
    private double t784;
    private double t787;
    private double t788;
    private double t791;
    private double t802;
    private double t803;
    private double t819;
    private double t822;
    private double t824;
    private double t825;
    private double t828;
    private double t836;
    private double t868;
    private double t884;
    private double t891;
    private double t906;
    private double t937;
    private double t940;
    private double t941;
    private double t944;
    private double t947;
    private double t953;
    private double t956;
    private double t959;
    private double t962;
    private double t963;
    private double t966;
    private double t967;
    private double t976;
    private double t983;
    private double t989;
    private double t990;
    private double t995;
    private double t1003;
    private double t1009;
    private double t1020;
    private double t1037;
    private double t1046;
    private double t1051;
    private double t1068;
    private double t1075;
    private double t1080;
    private double t1092;
    private double t1116;
    private double t1147;
    private double t1151;
    private double t1187;
    private double t1209;
    private double t1216;
    private double t1219;
    private double t1223;
    private double t1226;
    private double t1228;
    private double t1233;
    private double t1268;
    private double t1275;
    private double t1282;
    private double t1285;
    private double t1298;
    private double t1309;
    private double t1345;
    private double t1348;
    private double t1353;
    private double t1364;
    private double t1367;
    private double t1375;
    private double t1386;
    private double t1399;
    private double t1409;
    private double t1414;
    private double t1417;
    private double t1418;
    private double t1421;
    private double t1428;
    private double t1429;
    private double t1432;
    private double t1435;
    private double t1438;
    private double t1439;
    private double t1442;
    private double t1443;
    private double t1446;
    private double t1457;
    private double t1475;
    private double t1479;
    private double t1483;
    private double t1487;
    private double t1491;
    private double t1492;
    private double t1495;
    private double t1499;
    private double t1503;
    private double t1507;
    private double t1511;
    private double t1519;
    private double t1520;
    private double t1529;
    private double t1532;
    private double t1554;
    private double t1557;
    private double t1561;
    private double t1564;
    private double t1567;
    private double t1584;
    private double t1585;
    private double t1594;
    private double t1597;
    private double t1633;
    private double t1642;
    private double t1643;
    private double t1646;
    private double t1647;
    private double t1652;
    private double t1653;
    private double t1658;
    private double t1659;
    private double t1662;
    private double t1665;
    private double t1670;
    private double t1674;
    private double t1681;
    private double t1685;
    private double t1688;
    private double t1689;
    private double t1694;
    private double t1697;
    private double t1698;
    private double t1701;
    private double t1704;
    private double t1710;
    private double t1711;
    private double t1714;
    private double t1717;
    private double t1720;
    private double t1723;
    private double t1726;
    private double t1729;
    private double t1730;
    private double t1733;
    private double t1736;
    private double t1737;
    private double t1748;
    private double t1751;
    private double t1754;
    private double t1757;
    private double t1760;
    private double t1763;
    private double t1768;
    private double t1775;
    private double t1782;
    private double t1797;
    private double t1798;
    private double t1805;
    private double t1806;
    private double t1809;
    private double t1810;
    private double t1813;
    private double t1814;
    private double t1817;
    private double t1818;
    private double t1821;
    private double t1822;
    private double t1825;
    private double t1826;
    private double t1829;
    private double t1830;
    private double t1835;
    private double t1838;
    private double t1845;
    private double t1863;
    private double t1870;
    private double t1884;
    private double t1886;
    private double t1896;
    private double t1938;
    private double t1958;
    private double t1977;
    private double t2015;
    private double t2051;
    private double t2087;
    private double t2125;
    private double t2141;
    private double t2150;
    private double t2169;
    private double t2173;
    private double t2177;
    private double t2182;
    private double t2192;
    private double t2195;
    private double t2198;
    private double t2209;
    private double t2214;
    private double t2221;
    private double t2229;
    private double t2237;
    private double t2238;
    private double t2243;
    private double t2246;
    private double t2254;
    private double t2257;
    private double t2260;
    private double t2264;
    private double t2265;
    private double t2270;
    private double t2273;
    private double t2292;
    private double t2299;
    private double t2302;
    private double t2311;
    private double t2314;
    private double t2317;
    private double t2328;
    private double t2339;
    private double t2348;
    private double t2353;
    private double t2364;
    private double t2367;
    private double t2376;
    private double t2378;
    private double t2401;
    private double t2412;
    private double t2415;
    private double t2446;
    private double t2481;
    private double t2484;
    private double t2493;
    private double t2496;
    private double t2500;
    private double t2504;
    private double t2507;
    private double t2510;
    private double t2513;
    private double t2516;
    private double t2519;
    private double t2526;
    private double t2536;
    private double t2542;
    private double t2545;
    private double t2548;
    private double t2553;
    private double t2562;
    private double t2567;
    private double t2570;
    private double t2572;
    private double t2581;
    private double t2590;
    private double t2595;
    private double t2601;
    private double t2606;
    private double t2610;
    private double t2613;
    private double t2616;
    private double t2619;
    private double t2622;
    private double t2626;
    private double t2630;
    private double t2633;
    private double t2643;
    private double t2651;
    private double t2654;
    private double t2661;
    private double t2670;
    private double t2695;
    private double t2699;
    private double t2714;
    private double t2717;
    private double t2733;
    private double t2746;
    private double t2749;
    private double t2756;
    private double t2784;
    private double t2819;
    private double t2834;
    private double t2845;
    private double t2855;
    private double t2860;
    private double t2868;
    private double t2883;
    private double t2889;
    private double t2894;
    private double t2930;
    private double t2937;
    private double t2944;
    private double t2956;
    private double t2959;
    private double t2966;
    private double t2971;
    private double t2996;
    private double t3001;
    private double t3015;
    private double t3040;
    private double t3047;
    private double t3050;
    private double t3083;
    private double t3124;
    private double t3158;
    private double t3191;
    private double t3228;
    private double t3239;
    private double t3262;
    private double t3277;
    private double t3299;
    private double t3303;
    private double t3310;
    private double t3313;
    private double t3340;
    private double t3345;
    private double t3350;
    private double t3375;
    private double t3391;
    private double t3394;
    private double t3407;
    private double t3411;
    private double t3414;
    private double t3417;
    private double t3420;
    private double t3423;
    private double t3426;
    private double t3429;
    private double t3430;
    private double t3433;
    private double t3436;
    private double t3442;
    private double t3449;
    private double t3454;
    private double t3458;
    private double t3461;
    private double t3464;
    private double t3469;
    private double t3476;
    private double t3479;
    private double t3481;
    private double t3486;
    private double t3489;
    private double t3492;
    private double t3495;
    private double t3504;
    private double t3509;
    private double t3512;
    private double t3517;
    private double t3520;
    private double t3525;
    private double t3530;
    private double t3555;
    private double t3558;
    private double t3579;
    private double t3585;
    private double t3594;
    private double t3595;
    private double t3616;
    private double t3628;
    private double t3631;
    private double t3651;
    private double t3654;
    private double t3657;
    private double t3665;
    private double t3672;
    private double t3677;
    private double t3709;
    private double t3743;
    private double t3753;
    private double t3760;
    private double t3765;
    private double t3780;
    private double t3792;
    private double t3795;
    private double t3814;
    private double t3815;
    private double t3827;
    private double t3830;
    private double t3833;
    private double t3836;
    private double t3848;
    private double t3851;
    private double t3854;
    private double t3859;
    private double t3862;
    private double t3865;
    private double t3868;
    private double t3876;
    private double t3883;
    private double t3889;
    private double t3905;
    private double t3908;
    private double t3911;
    private double t3926;
    private double t3930;
    private double t3934;
    private double t3948;
    private double t3951;
    private double t3954;
    private double t3957;
    private double t3983;
    private double t3986;
    private double t3991;
    private double t3994;
    private double t3995;
    private double t4000;
    private double t4005;
    private double t4014;
    private double t4019;
    private double t4032;
    private double t4061;
    private double t4068;
    private double t4103;
    private double t4133;
    private double t4136;
    private double t4170;
    private double t4188;
    private double t4199;
    private double t4206;
    private double t4242;
    private double t4283;
    private double t4310;
    private double t4317;
    private double t4351;
    private double t4384;
    private double t4416;
    private double t4451;
    private double t4484;
    private double t4517;
    private double t4553;
    private double t4589;

    /**
     * Constructor
     * 
     * @param rEq
     *        equatorial radius (m)
     * @param j11
     *        11th order central body coefficient
     */
    public StelaZonalAttractionJ11(final double rEq, final double j11) {
        this.rEq = rEq;
        this.j11 = j11;
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
        this.derParUdeg11_1(a, ex, ey, ix, iy, mu);
        this.derParUdeg11_2(a, ex, ey, ix, iy);
        this.derParUdeg11_3(a, ex, ey, ix, iy);
        this.derParUdeg11_4(a, ex, ey, ix, iy);
        this.derParUdeg11_5(a, ex, ey, ix, iy);
        this.derParUdeg11_6(a, ex, ey, ix, iy);
        this.derParUdeg11_7(a, ex, ey, ix, iy);

        final double[] dPot = new double[6];
        dPot[0] = -0.165e3 / 0.4096e4 * this.t12 * this.t709 / this.t713 / this.t711 / a * this.t723;
        dPot[1] = 0.0e0;
        dPot[2] =
            0.55e2
                / 0.16384e5
                * this.t12
                * iy
                * this.t708
                * this.t731
                + 0.55e2
                / 0.16384e5
                * this.t12
                * this.t15
                * (this.t776 + this.t822 + this.t868 + this.t906 + this.t947 + this.t995 + this.t1037 + this.t1075
                    + this.t1116 + this.t1151 + this.t1187 + this.t1228 + this.t1268
                    + this.t1309 + this.t1348 + this.t1386) * this.t731 + 0.1155e4 / 0.16384e5 * this.t12 * this.t709
                * this.t1399 * ex;
        dPot[3] =
            -0.55e2
                / 0.16384e5
                * this.t12
                * ix
                * this.t708
                * this.t731
                + 0.55e2
                / 0.16384e5
                * this.t12
                * this.t15
                * (this.t1457 + this.t1511 + this.t1554 + this.t1594 + this.t1633 + this.t1681 + this.t1736
                    + this.t1782 + this.t1838 + this.t1884 + this.t1938 + this.t1977
                    + this.t2015 + this.t2051 + this.t2087 + this.t2125) * this.t731 + 0.1155e4 / 0.16384e5 * this.t12
                * this.t709 * this.t1399 * ey;
        dPot[4] =
            -0.55e2
                / 0.16384e5
                * this.t2141
                * this.t709
                * this.t731
                * ix
                - 0.55e2
                / 0.16384e5
                * this.t12
                * ey
                * this.t708
                * this.t731
                + 0.55e2
                / 0.16384e5
                * this.t12
                * this.t15
                * (this.t2616 + this.t3375 + this.t3228 + this.t3191 + this.t2746 + this.t2221 + this.t3040
                    + this.t3001 + this.t2339 + this.t2695 + this.t2302 + this.t2264
                    + this.t2182 + this.t2860 + this.t3340 + this.t2819 + this.t3299 + this.t3262 + this.t2784
                    + this.t2412 + this.t2376 + this.t3158 + this.t2570 + this.t3124
                    + this.t2966 + this.t2930 + this.t2481 + this.t2526 + this.t2446 + this.t2661 + this.t3083 
                    + this.t2894)
                * this.t731;
        dPot[5] =
            -0.55e2
                / 0.16384e5
                * this.t2141
                * this.t709
                * this.t731
                * iy
                + 0.55e2
                / 0.16384e5
                * this.t12
                * ex
                * this.t708
                * this.t731
                + 0.55e2
                / 0.16384e5
                * this.t12
                * this.t15
                * (this.t4283 + this.t3555 + this.t3520 + this.t3780 + this.t4242 + this.t4416 + this.t4553
                    + this.t3476 + this.t4103 + this.t3672 + this.t4068 + this.t3957
                    + this.t3429 + this.t3631 + this.t3743 + this.t4484 + this.t3905 + this.t4589 + this.t4451
                    + this.t4032 + this.t4351 + this.t4517 + this.t4384 + this.t4206
                    + this.t4317 + this.t3709 + this.t4170 + this.t4136 + this.t3859 + this.t3994 + this.t3814 
                    + this.t3594)
                * this.t731;

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
     * Partial derivative due to 11th order Earth potential zonal harmonics.
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
    private final void derParUdeg11_1(final double a, final double ex, final double ey, final double ix,
                                      final double iy, final double mu) {
        this.t1 = mu * this.j11;
        this.t2 = this.rEq * this.rEq;
        this.t4 = this.t2 * this.t2;
        this.t5 = this.t4 * this.t4;
        this.t6 = this.t5 * this.t2 * this.rEq;
        this.t7 = ix * ix;
        this.t8 = iy * iy;
        this.t9 = 0.1e1 - this.t7 - this.t8;
        this.t10 = MathLib.sqrt(this.t9);
        this.t12 = this.t1 * this.t6 * this.t10;
        this.t14 = ey * ix;
        this.t15 = ex * iy - this.t14;
        this.t16 = this.t9 * this.t9;
        this.t17 = this.t16 * this.t16;
        this.t18 = this.t7 + this.t8;
        this.t19 = this.t18 * this.t17;
        this.t20 = ex * ex;
        this.t21 = this.t20 * this.t20;
        this.t22 = this.t19 * this.t21;
        this.t23 = this.t7 * this.t7;
        this.t24 = ey * ey;
        this.t25 = this.t24 * this.t23;
        this.t26 = this.t25 * this.t8;
        this.t29 = this.t9 * this.t17;
        this.t30 = this.t29 * this.t18;
        this.t31 = this.t20 * ex;
        this.t32 = this.t30 * this.t31;
        this.t33 = this.t7 * ix;
        this.t34 = this.t24 * this.t24;
        this.t35 = this.t34 * ey;
        this.t36 = this.t33 * this.t35;
        this.t37 = this.t8 * this.t8;
        this.t38 = this.t37 * iy;
        this.t39 = this.t36 * this.t38;
        this.t42 = this.t16 * this.t9;
        this.t43 = this.t18 * this.t18;
        this.t44 = this.t42 * this.t43;
        this.t45 = this.t44 * ex;
        this.t46 = this.t14 * iy;
        this.t49 = this.t16 * this.t18;
        this.t50 = this.t24 * this.t8;
        this.t53 = this.t42 * this.t18;
        this.t54 = this.t21 * this.t23;
        this.t57 = this.t20 + this.t24;
        this.t58 = this.t57 * this.t57;
        this.t59 = this.t58 * this.t58;
        this.t60 = this.t59 * this.t9;
        this.t63 = this.t43 * this.t43;
        this.t64 = this.t29 * this.t63;
        this.t68 = this.t19 * this.t31;
        this.t69 = this.t24 * ey;
        this.t70 = this.t33 * this.t69;
        this.t71 = this.t8 * iy;
        this.t72 = this.t70 * this.t71;
        this.t75 = this.t43 * this.t18;
        this.t76 = this.t29 * this.t75;
        this.t77 = this.t76 * this.t20;
        this.t78 = this.t7 * this.t24;
        this.t79 = this.t78 * this.t8;
        this.t82 = this.t30 * this.t20;
        this.t83 = this.t34 * this.t24;
        this.t84 = this.t7 * this.t83;
        this.t85 = this.t37 * this.t8;
        this.t86 = this.t84 * this.t85;
        this.t89 = this.t34 * this.t34;
        this.t90 = this.t37 * this.t37;
        this.t91 = this.t89 * this.t90;
        this.t94 = this.t17 * this.t43;
        this.t95 = this.t34 * this.t37;
        this.t98 =
            -0.8064e4 + 0.4353523200e10 * this.t22 * this.t26 + 0.632067072e9 * this.t32 * this.t39 - 0.1283143680e10
                * this.t45 * this.t46
                + 0.62899200e8 * this.t49 * this.t50 + 0.449100288e9 * this.t53 * this.t54 + 0.315315e6 * this.t60
                * this.t18 - 0.8126576640e10 * this.t64
                * ex * this.t46 + 0.5804697600e10 * this.t72 * this.t68 + 0.25598716416e11 * this.t77 * this.t79
                + 0.316033536e9 * this.t82 * this.t86
                + 0.11286912e8 * this.t30 * this.t91 - 0.2437972992e10 * this.t94 * this.t95;
        this.t99 = this.t21 * this.t21;
        this.t100 = this.t23 * this.t23;
        this.t101 = this.t99 * this.t100;
        this.t104 = this.t17 * this.t75;
        this.t107 = this.t20 * this.t7;
        this.t110 = this.t83 * this.t85;
        this.t113 = this.t29 * this.t43;
        this.t114 = this.t21 * this.t20;
        this.t115 = this.t23 * this.t7;
        this.t116 = this.t114 * this.t115;
        this.t119 = this.t57 * this.t9;
        this.t122 = this.t58 * this.t57;
        this.t123 = this.t122 * this.t9;
        this.t126 = this.t58 * this.t9;
        this.t131 = this.t122 * this.t42;
        this.t132 = this.t131 * this.t43;
        this.t133 = ex * ix;
        this.t134 = ey * iy;
        this.t135 = this.t133 * this.t134;
        this.t138 = this.t57 * this.t17;
        this.t139 = this.t138 * this.t18;
        this.t140 = this.t21 * ex;
        this.t141 = this.t23 * ix;
        this.t142 = this.t140 * this.t141;
        this.t143 = this.t142 * this.t134;
        this.t146 = this.t58 * this.t17;
        this.t147 = this.t146 * this.t43;
        this.t148 = this.t107 * this.t50;
        this.t151 = this.t57 * this.t29;
        this.t152 = this.t151 * this.t43;
        this.t153 = this.t107 * this.t95;
        this.t156 =
            0.11286912e8 * this.t30 * this.t101 + 0.2708858880e10 * this.t104 * this.t50 - 0.4063288320e10 * this.t64
                * this.t107 + 0.290234880e9
                * this.t19 * this.t110 - 0.677214720e9 * this.t113 * this.t116 - 0.5503680e7 * this.t119 * this.t107
                - 0.229320e6 * this.t123 * this.t50
                - 0.2751840e7 * this.t126 * this.t107 + 0.2708858880e10 * this.t104 * this.t107 - 0.204787440e9
                * this.t132 * this.t135
                + 0.354731520e9 * this.t139 * this.t143 - 0.1729316160e10 * this.t147 * this.t148 - 0.2200947840e10
                * this.t152 * this.t153;
        this.t158 = this.t58 * this.t29;
        this.t159 = this.t158 * this.t63;
        this.t162 = this.t151 * this.t63;
        this.t165 = this.t58 * this.t42;
        this.t166 = this.t165 * this.t18;
        this.t167 = this.t31 * this.t33;
        this.t168 = this.t167 * this.t134;
        this.t171 = this.t138 * this.t43;
        this.t174 = this.t53 * ex;
        this.t175 = ix * this.t69;
        this.t176 = this.t175 * this.t71;
        this.t179 = this.t57 * this.t42;
        this.t180 = this.t179 * this.t43;
        this.t183 = this.t57 * this.t16;
        this.t184 = this.t183 * this.t18;
        this.t187 = this.t179 * this.t18;
        this.t192 = this.t54 * this.t50;
        this.t195 = this.t158 * this.t75;
        this.t196 = this.t69 * this.t71;
        this.t197 = this.t133 * this.t196;
        this.t200 = this.t76 * this.t31;
        this.t201 = this.t33 * ey;
        this.t202 = this.t201 * iy;
        this.t205 = this.t113 * this.t140;
        this.t206 = this.t141 * ey;
        this.t207 = this.t206 * iy;
        this.t210 =
            -0.16507108800e11 * this.t159 * this.t135 - 0.27731942784e11 * this.t162 * this.t135 + 0.189034560e9
                * this.t166 * this.t168
                - 0.14366626560e11 * this.t171 * this.t148 + 0.1796401152e10 * this.t174 * this.t176 - 0.4041902592e10
                * this.t180 * this.t135
                + 0.369847296e9 * this.t184 * this.t135 + 0.2474634240e10 * this.t187 * this.t148 + 0.283551840e9
                * this.t166 * this.t148
                - 0.2200947840e10 * this.t152 * this.t192 + 0.2200947840e10 * this.t195 * this.t197 + 0.17065810944e11
                * this.t200 * this.t202
                - 0.4063288320e10 * this.t205 * this.t207;
        this.t211 = this.t167 * this.t196;
        this.t214 = this.t58 * this.t16;
        this.t215 = this.t214 * this.t18;
        this.t218 = this.t138 * this.t75;
        this.t221 = this.t122 * this.t17;
        this.t222 = this.t221 * this.t75;
        this.t231 = this.t30 * this.t21;
        this.t232 = this.t23 * this.t34;
        this.t233 = this.t232 * this.t37;
        this.t242 = this.t122 * this.t16;
        this.t243 = this.t242 * this.t18;
        this.t248 =
            -0.2934597120e10 * this.t152 * this.t211 + 0.198132480e9 * this.t215 * this.t135 + 0.17878468608e11
                * this.t218 * this.t135
                + 0.960731200e9 * this.t222 * this.t135 + 0.1649756160e10 * this.t187 * this.t197 - 0.2751840e7
                * this.t126 * this.t50
                + 0.1649756160e10 * this.t187 * this.t168 + 0.790083840e9 * this.t231 * this.t233 + 0.886828800e9
                * this.t139 * this.t153
                - 0.1152877440e10 * this.t147 * this.t168 - 0.9577751040e10 * this.t171 * this.t197 + 0.17297280e8
                * this.t243 * this.t135
                - 0.880379136e9 * this.t152 * this.t143;
        this.t253 = this.t53 * this.t31;
        this.t262 = this.t35 * this.t38;
        this.t263 = this.t133 * this.t262;
        this.t266 = this.t146 * this.t75;
        this.t271 = this.t165 * this.t43;
        this.t274 = this.t151 * this.t75;
        this.t283 =
            -0.229320e6 * this.t123 * this.t107 + 0.1796401152e10 * this.t253 * this.t202 - 0.5728320e7 * this.t179
                * this.t116 - 0.2358720e7
                * this.t214 * this.t54 + 0.2200947840e10 * this.t195 * this.t168 + 0.354731520e9 * this.t139
                * this.t263 + 0.10375896960e11 * this.t266
                * this.t135 + 0.3301421760e10 * this.t195 * this.t148 - 0.2268414720e10 * this.t271 * this.t135
                + 0.17607582720e11 * this.t274 * this.t197
                - 0.9577751040e10 * this.t168 * this.t171 + 0.886828800e9 * this.t139 * this.t192 + 0.189034560e9
                * this.t166 * this.t197;
        this.t302 = this.t122 * this.t29;
        this.t303 = this.t302 * this.t63;
        this.t306 = this.t104 * ex;
        this.t313 =
            -0.2437972992e10 * this.t94 * this.t54 + 0.26411374080e11 * this.t274 * this.t148 - 0.1152877440e10
                * this.t147 * this.t197
                + 0.4266452736e10 * this.t76 * this.t54 - 0.5503680e7 * this.t119 * this.t50 + 0.17607582720e11
                * this.t274 * this.t168
                - 0.880379136e9 * this.t152 * this.t263 - 0.5728320e7 * this.t179 * this.t110 + 0.1182438400e10
                * this.t139 * this.t211
                - 0.1559004720e10 * this.t303 * this.t135 + 0.5417717760e10 * this.t306 * this.t46 - 0.677214720e9
                * this.t113 * this.t110
                + 0.4266452736e10 * this.t76 * this.t95;
        this.t315 = this.t30 * this.t140;
        this.t316 = this.t141 * this.t69;
        this.t317 = this.t316 * this.t71;
        this.t320 = this.t183 * ex;
        this.t323 = this.t214 * this.t31;
        this.t329 = this.t179 * this.t21;
        this.t332 = this.t179 * this.t31;
        this.t335 = this.t179 * this.t140;
        this.t338 = this.t94 * ex;
        this.t341 = this.t214 * this.t20;
        this.t344 = this.t113 * this.t21;
        this.t347 = this.t19 * ex;
        this.t348 = ix * this.t35;
        this.t349 = this.t348 * this.t38;
        this.t352 = this.t53 * this.t20;
        this.t355 = this.t19 * this.t140;
        this.t358 =
            0.632067072e9 * this.t315 * this.t317 - 0.88058880e8 * this.t320 * this.t176 - 0.9434880e7 * this.t323
                * this.t202 - 0.458640e6 * this.t123
                * ex * this.t46 - 0.85924800e8 * this.t329 * this.t26 - 0.114566400e9 * this.t332 * this.t72
                - 0.34369920e8 * this.t335 * this.t207
                - 0.9751891968e10 * this.t338 * this.t176 - 0.14152320e8 * this.t341 * this.t79 - 0.10158220800e11
                * this.t344 * this.t26
                + 0.1741409280e10 * this.t347 * this.t349 + 0.2694601728e10 * this.t352 * this.t79 + 0.1741409280e10
                * this.t355 * this.t207;
        this.t359 = this.t19 * this.t20;
        this.t360 = this.t7 * this.t34;
        this.t361 = this.t360 * this.t37;
        this.t364 = this.t183 * this.t20;
        this.t367 = this.t179 * this.t20;
        this.t370 = this.t179 * ex;
        this.t373 = this.t214 * ex;
        this.t376 = this.t183 * this.t31;
        this.t379 = this.t94 * this.t20;
        this.t382 = this.t30 * this.t114;
        this.t383 = this.t115 * this.t24;
        this.t384 = this.t383 * this.t8;
        this.t387 = this.t94 * this.t31;
        this.t390 = this.t49 * ex;
        this.t393 = this.t119 * ex;
        this.t399 =
            0.4353523200e10 * this.t359 * this.t361 - 0.132088320e9 * this.t364 * this.t79 - 0.85924800e8 * this.t367
                * this.t361 - 0.34369920e8
                * this.t370 * this.t349 - 0.9434880e7 * this.t373 * this.t176 - 0.88058880e8 * this.t376 * this.t202
                - 0.14627837952e11 * this.t379 * this.t79
                + 0.316033536e9 * this.t382 * this.t384 - 0.9751891968e10 * this.t387 * this.t202 + 0.125798400e9
                * this.t390 * this.t46
                - 0.11007360e8 * this.t393 * this.t46 - 0.127008e6 * this.t58 - 0.72576e5 * this.t20 - 0.72576e5
                * this.t24;
        this.t405 = this.t75 * this.t21;
        this.t406 = this.t405 * this.t23;
        this.t409 = this.t18 * this.t34;
        this.t410 = this.t409 * this.t37;
        this.t413 = this.t75 * this.t20;
        this.t414 = this.t413 * this.t7;
        this.t417 = this.t43 * this.t20;
        this.t418 = this.t417 * this.t7;
        this.t421 = this.t75 * this.t24;
        this.t422 = this.t421 * this.t8;
        this.t429 = this.t43 * this.t24;
        this.t430 = this.t429 * this.t8;
        this.t433 = this.t75 * this.t34;
        this.t434 = this.t433 * this.t37;
        this.t437 = this.t113 * this.t20;
        this.t440 = this.t18 * this.t21;
        this.t441 = this.t440 * this.t23;
        this.t444 =
            -0.52920e5 * this.t122 - 0.3969e4 * this.t59 + 0.4401895680e10 * this.t151 * this.t406 + 0.47258640e8
                * this.t165 * this.t410
                + 0.5187948480e10 * this.t146 * this.t414 - 0.1134207360e10 * this.t165 * this.t418 + 0.480365600e9
                * this.t221 * this.t422
                - 0.2358720e7 * this.t214 * this.t95 + 0.412439040e9 * this.t179 * this.t410 - 0.1134207360e10
                * this.t165 * this.t430
                + 0.4401895680e10 * this.t151 * this.t434 - 0.10158220800e11 * this.t437 * this.t361 + 0.47258640e8
                * this.t165 * this.t441;
        this.t447 = this.t17 * this.t31;
        this.t450 = this.t21 * this.t31;
        this.t451 = this.t17 * this.t450;
        this.t452 = this.t23 * this.t33;
        this.t453 = this.t452 * ey;
        this.t454 = this.t453 * iy;
        this.t457 = this.t16 * this.t31;
        this.t460 = this.t16 * this.t20;
        this.t463 = this.t9 * ex;
        this.t466 = this.t16 * ex;
        this.t471 = this.t113 * ex;
        this.t474 = this.t113 * this.t31;
        this.t479 = this.t18 * this.t24;
        this.t480 = this.t479 * this.t8;
        this.t485 =
            0.5187948480e10 * this.t146 * this.t422 - 0.150492160e9 * this.t447 * this.t39 - 0.21498880e8 * this.t451
                * this.t454 - 0.105670656e9
                * this.t457 * this.t202 - 0.158505984e9 * this.t460 * this.t79 - 0.4193280e7 * this.t463 * this.t46
                - 0.105670656e9 * this.t466 * this.t176
                + 0.412439040e9 * this.t179 * this.t441 - 0.4063288320e10 * this.t471 * this.t349 - 0.13544294400e11
                * this.t474 * this.t72
                + 0.480365600e9 * this.t221 * this.t414 + 0.184923648e9 * this.t183 * this.t480 - 0.2020951296e10
                * this.t179 * this.t418;
        this.t489 = this.t43 * this.t21;
        this.t490 = this.t489 * this.t23;
        this.t495 = this.t17 * this.t140;
        this.t498 = this.t43 * this.t34;
        this.t499 = this.t498 * this.t37;
        this.t502 = this.t63 * this.t20;
        this.t503 = this.t502 * this.t7;
        this.t508 = this.t126 * ex;
        this.t513 = this.t30 * ex;
        this.t514 = this.t34 * this.t69;
        this.t515 = ix * this.t514;
        this.t516 = this.t37 * this.t71;
        this.t517 = this.t515 * this.t516;
        this.t522 = this.t18 * this.t83;
        this.t523 = this.t522 * this.t85;
        this.t528 =
            0.8648640e7 * this.t242 * this.t480 - 0.288219360e9 * this.t146 * this.t490 + 0.8939234304e10 * this.t138
                * this.t414 - 0.150492160e9
                * this.t495 * this.t317 - 0.2394437760e10 * this.t138 * this.t499 - 0.8253554400e10 * this.t158
                * this.t503 - 0.779502360e9 * this.t302
                * this.t503 - 0.5503680e7 * this.t508 * this.t46 - 0.2020951296e10 * this.t179 * this.t430
                + 0.90295296e8 * this.t513 * this.t517
                - 0.102393720e9 * this.t131 * this.t418 + 0.59121920e8 * this.t138 * this.t523 + 0.550236960e9
                * this.t158 * this.t434;
        this.t529 = this.t63 * this.t24;
        this.t530 = this.t529 * this.t8;
        this.t533 = this.t18 * this.t20;
        this.t534 = this.t533 * this.t7;
        this.t539 = this.t18 * this.t114;
        this.t540 = this.t539 * this.t115;
        this.t557 = this.t43 * this.t114;
        this.t558 = this.t557 * this.t115;
        this.t563 = this.t17 * this.t114;
        this.t566 =
            -0.779502360e9 * this.t302 * this.t530 + 0.8648640e7 * this.t242 * this.t534 - 0.2394437760e10 * this.t138
                * this.t490 + 0.59121920e8
                * this.t138 * this.t540 - 0.102393720e9 * this.t131 * this.t430 - 0.288219360e9 * this.t146 * this.t499
                + 0.99066240e8 * this.t214 * this.t534
                + 0.99066240e8 * this.t214 * this.t480 - 0.8253554400e10 * this.t158 * this.t530 - 0.13865971392e11
                * this.t151 * this.t530
                + 0.8939234304e10 * this.t138 * this.t422 - 0.146729856e9 * this.t151 * this.t558 + 0.184923648e9
                * this.t183 * this.t534
                - 0.75246080e8 * this.t563 * this.t384;
        this.t569 = this.t42 * this.t21;
        this.t572 = this.t17 * ex;
        this.t575 = this.t43 * this.t83;
        this.t576 = this.t575 * this.t85;
        this.t581 = this.t17 * this.t21;
        this.t584 = this.t42 * this.t20;
        this.t587 = this.t42 * this.t140;
        this.t592 = this.t42 * ex;
        this.t595 = this.t17 * this.t20;
        this.t598 = this.t42 * this.t31;
    }

    /**
     * Partial derivative due to 11th order Earth potential zonal harmonics.
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
            derParUdeg11_2(final double a, final double ex, final double ey, final double ix, final double iy) {
        this.t603 = this.t183 * this.t43;
        this.t605 =
            -0.458265600e9 * this.t569 * this.t26 - 0.21498880e8 * this.t572 * this.t517 - 0.146729856e9 * this.t151
                * this.t576 + 0.550236960e9
                * this.t158 * this.t406 - 0.188115200e9 * this.t581 * this.t233 - 0.458265600e9 * this.t584 * this.t361
                - 0.183306240e9 * this.t587 * this.t207
                - 0.13865971392e11 * this.t151 * this.t503 - 0.183306240e9 * this.t592 * this.t349 - 0.75246080e8
                * this.t595 * this.t86
                - 0.611020800e9 * this.t598 * this.t72 - 0.4063288320e10 * this.t64 * this.t50 - 0.110073600e9
                * this.t603;
        this.t606 = this.t76 * ex;
        this.t617 = this.t30 * this.t450;
        this.t624 = this.t165 * this.t75;
        this.t626 = this.t59 * this.t16;
        this.t629 = this.t221 * this.t63;
        this.t631 = this.t123 * this.t18;
        this.t633 = this.t214 * this.t43;
        this.t635 =
            0.17065810944e11 * this.t606 * this.t176 + 0.62899200e8 * this.t49 * this.t107 + 0.449100288e9 * this.t53
                * this.t95 - 0.641571840e9
                * this.t44 * this.t107 - 0.22014720e8 * this.t183 * this.t54 + 0.90295296e8 * this.t617 * this.t454
                - 0.641571840e9 * this.t44 * this.t50
                - 0.22014720e8 * this.t183 * this.t95 + 0.1852538688e10 * this.t624 - 0.7027020e7 * this.t626
                * this.t43 - 0.3242467800e10
                * this.t629 + 0.4127760e7 * this.t631 - 0.208039104e9 * this.t633;
        this.t637 = this.t59 * this.t42;
        this.t640 = this.t131 * this.t75;
        this.t642 = this.t146 * this.t63;
        this.t644 = this.t126 * this.t18;
        this.t646 = this.t63 * this.t18;
        this.t649 = this.t242 * this.t43;
        this.t651 = this.t138 * this.t63;
        this.t653 = this.t59 * this.t17;
        this.t658 = this.t42 * this.t114;
        this.t661 = this.t59 * this.t29;
        this.t664 = this.t9 * this.t20;
        this.t667 = this.t17 * this.t89;
        this.t670 =
            0.63996075e8 * this.t637 * this.t75 + 0.819149760e9 * this.t640 - 0.7263127872e10 * this.t642 + 0.9631440e7
                * this.t644
                + 0.5282274816e10 * this.t151 * this.t646 - 0.90810720e8 * this.t649 - 0.3724680960e10 * this.t651
                - 0.255194225e9 * this.t653
                * this.t63 + 0.4677014160e10 * this.t302 * this.t646 - 0.30551040e8 * this.t658 * this.t115
                + 0.370263621e9 * this.t661 * this.t646
                - 0.2096640e7 * this.t664 * this.t7 - 0.2687360e7 * this.t667 * this.t90;
        this.t671 = this.t9 * this.t24;
        this.t674 = this.t16 * this.t21;
        this.t677 = this.t16 * this.t34;
        this.t680 = this.t17 * this.t99;
        this.t683 = this.t42 * this.t83;
        this.t686 = this.t119 * this.t18;
        this.t690 = this.t179 * this.t75;
        this.t692 = this.t9 * this.t18;
        this.t694 = this.t75 * this.t42;
        this.t696 = this.t17 * this.t63;
        this.t698 = this.t16 * this.t43;
        this.t704 =
            -0.2096640e7 * this.t671 * this.t8 - 0.26417664e8 * this.t674 * this.t23 - 0.26417664e8 * this.t677
                * this.t37 - 0.2687360e7 * this.t680
                * this.t100 - 0.30551040e8 * this.t683 * this.t85 + 0.5241600e7 * this.t686 + 0.10399478544e11
                * this.t158 * this.t646
                + 0.962357760e9 * this.t690 + 0.524160e6 * this.t692 + 0.89107200e8 * this.t694 - 0.338607360e9
                * this.t696 - 0.10483200e8
                * this.t698 + 0.474050304e9 * this.t29 * this.t646 + 0.290234880e9 * this.t19 * this.t116;
        this.t708 =
            this.t98 + this.t156 + this.t210 + this.t248 + this.t283 + this.t313 + this.t358 + this.t399 + this.t444
                + this.t485 + this.t528 + this.t566 + this.t605 + this.t635 + this.t670
                + this.t704;
        this.t709 = this.t15 * this.t708;
        this.t710 = a * a;
        this.t711 = this.t710 * this.t710;
        this.t713 = this.t711 * this.t711;
        this.t716 = 0.1e1 - this.t20 - this.t24;
        this.t717 = this.t716 * this.t716;
        this.t718 = this.t717 * this.t717;
        this.t719 = this.t718 * this.t718;
        this.t721 = MathLib.sqrt(this.t716);
        this.t723 = 0.1e1 / this.t721 / this.t719 / this.t717;
        this.t730 = 0.1e1 / this.t713 / this.t711;
        this.t731 = this.t730 * this.t723;
        this.t735 = this.t31 * this.t23;
        this.t738 = ex * this.t7;
        this.t747 = this.t140 * this.t115;
        this.t750 = this.t43 * ex;
        this.t755 = this.t18 * ex;
        this.t758 = this.t42 * ix;
        this.t761 = this.t31 * this.t7;
        this.t764 = this.t75 * ex;
        this.t773 = this.t140 * this.t23;
        this.t776 =
            -0.9434880e7 * this.t214 * this.t735 - 0.11007360e8 * this.t119 * this.t738 - 0.5503680e7 * this.t126
                * this.t738 - 0.88058880e8
                * this.t183 * this.t735 - 0.458640e6 * this.t123 * this.t738 - 0.34369920e8 * this.t179 * this.t747
                - 0.544864320e9 * this.t214 * this.t750
                - 0.56216160e8 * this.t242 * this.t750 + 0.38525760e8 * this.t119 * this.t755 - 0.183306240e9
                * this.t758 * this.t262 - 0.1375920e7
                * this.t126 * this.t761 + 0.7410154752e10 * this.t179 * this.t764 + 0.4914898560e10 * this.t165
                * this.t764 - 0.832156416e9 * this.t183
                * this.t750 - 0.9751891968e10 * this.t94 * this.t735 - 0.9434880e7 * this.t183 * this.t773;
        this.t777 = this.t20 * ix;
        this.t778 = this.t777 * this.t134;
        this.t781 = this.t761 * this.t50;
        this.t784 = this.t777 * this.t196;
        this.t787 = this.t21 * this.t141;
        this.t788 = this.t787 * this.t134;
        this.t791 = this.t738 * this.t50;
        this.t802 = this.t21 * this.t33;
        this.t803 = this.t802 * this.t134;
        this.t819 = this.t735 * this.t50;
        this.t822 =
            0.5764387200e10 * this.t266 * this.t778 + 0.1134207360e10 * this.t187 * this.t781 + 0.8803791360e10
                * this.t274 * this.t784
                + 0.1773657600e10 * this.t139 * this.t788 - 0.28733253120e11 * this.t171 * this.t791 - 0.66028435200e11
                * this.t162 * this.t778
                - 0.11456640e8 * this.t592 * this.t110 - 0.1228724640e10 * this.t271 * this.t778 - 0.11007360e8
                * this.t463 * this.t50
                + 0.756138240e9 * this.t187 * this.t803 - 0.6917264640e10 * this.t171 * this.t781 + 0.792529920e9
                * this.t184 * this.t778
                + 0.17065810944e11 * this.t76 * this.t735 + 0.17878468608e11 * this.t447 * this.t75 * this.t7
                + 0.41503587840e11 * this.t218 * this.t778
                + 0.567103680e9 * this.t166 * this.t791 - 0.8803791360e10 * this.t152 * this.t819;
        this.t824 = this.t33 * this.t20;
        this.t825 = this.t824 * this.t196;
        this.t828 = this.t738 * this.t95;
        this.t836 = this.t824 * this.t134;
        this.t868 =
            -0.8803791360e10 * this.t152 * this.t825 - 0.4401895680e10 * this.t152 * this.t828 - 0.3458632320e10
                * this.t147 * this.t791
                - 0.4041902592e10 * this.t598 * this.t43 * this.t7 + 0.567103680e9 * this.t166 * this.t836
                + 0.24766560e8 * this.t126 * this.t755
                + 0.118243840e9 * this.t451 * this.t18 * this.t115 - 0.27731942784e11 * this.t31 * this.t29 * this.t63
                * this.t7 + 0.103783680e9 * this.t215
                * this.t778 + 0.369847296e9 * this.t457 * this.t7 * this.t18 - 0.4611509760e10 * this.t171 * this.t803
                + 0.13205687040e11 * this.t274
                * this.t781 - 0.9073658880e10 * this.t180 * this.t778 - 0.4788875520e10 * this.t495 * this.t43
                * this.t23 - 0.4063288320e10 * this.t113
                * this.t747 - 0.3458632320e10 * this.t147 * this.t836 + 0.1773657600e10 * this.t139 * this.t828;
        this.t884 = this.t646 * ex;
        this.t891 = this.t63 * ex;
        this.t906 =
            0.4949268480e10 * this.t187 * this.t836 + 0.5417717760e10 * this.t104 * this.t738 - 0.4401895680e10
                * this.t152 * this.t788
                + 0.4949268480e10 * this.t187 * this.t791 + 0.90295296e8 * this.t30 * this.t450 * this.t100
                - 0.8126576640e10 * this.t64 * this.t738
                + 0.756138240e9 * this.t187 * this.t784 + 0.2962108968e10 * this.t302 * this.t884 - 0.4611509760e10
                * this.t171 * this.t784
                + 0.2522520e7 * this.t123 * this.t755 - 0.29052511488e11 * this.t138 * this.t891 + 0.6602843520e10
                * this.t195 * this.t791
                + 0.52822748160e11 * this.t274 * this.t836 + 0.52822748160e11 * this.t274 * this.t791 + 0.6602843520e10
                * this.t195 * this.t836
                - 0.9354028320e10 * this.t159 * this.t778 - 0.1283143680e10 * this.t44 * this.t738;
        this.t937 = this.t9 * ix;
        this.t940 = ix * this.t17;
        this.t941 = this.t514 * this.t516;
        this.t944 = this.t16 * ix;
        this.t947 =
            0.511968600e9 * this.t131 * this.t764 + 0.8803791360e10 * this.t274 * this.t803 - 0.2041553800e10
                * this.t221 * this.t891
                + 0.28062084960e11 * this.t158 * this.t884 + 0.1796401152e10 * this.t53 * this.t735 - 0.44029440e8
                * this.t466 * this.t95
                + 0.1741409280e10 * this.t19 * this.t747 - 0.28733253120e11 * this.t171 * this.t836 + 0.3547315200e10
                * this.t139 * this.t819
                + 0.3547315200e10 * this.t139 * this.t825 + 0.125798400e9 * this.t49 * this.t738 - 0.11007360e8
                * this.t119 * this.t761
                - 0.19454806800e11 * this.t146 * this.t891 + 0.41597914176e11 * this.t151 * this.t884 - 0.4193280e7
                * this.t937 * this.t134
                - 0.21498880e8 * this.t940 * this.t941 - 0.105670656e9 * this.t944 * this.t196;
        this.t953 = this.t764 * this.t7;
        this.t956 = this.t750 * this.t7;
        this.t959 = this.t891 * this.t7;
        this.t962 = this.t18 * this.t31;
        this.t963 = this.t962 * this.t23;
        this.t966 = this.t43 * this.t31;
        this.t967 = this.t966 * this.t23;
        this.t976 = this.t755 * this.t7;
        this.t983 = this.t43 * this.t140;
        this.t989 = this.t75 * this.t31;
        this.t990 = this.t989 * this.t23;
        this.t995 =
            0.8803791360e10 * this.t140 * this.t29 * this.t75 * this.t23 - 0.145152e6 * ex + 0.17878468608e11
                * this.t138 * this.t953
                - 0.4041902592e10 * this.t179 * this.t956 - 0.16507108800e11 * this.t158 * this.t959 + 0.189034560e9
                * this.t165 * this.t963
                - 0.1152877440e10 * this.t146 * this.t967 + 0.960731200e9 * this.t221 * this.t953 + 0.10375896960e11
                * this.t146 * this.t953
                + 0.1649756160e10 * this.t179 * this.t963 + 0.17297280e8 * this.t242 * this.t976 - 0.9577751040e10
                * this.t138 * this.t967
                + 0.369847296e9 * this.t183 * this.t976 - 0.880379136e9 * this.t151 * this.t983 * this.t115
                - 0.2268414720e10 * this.t165 * this.t956
                + 0.17607582720e11 * this.t151 * this.t990 - 0.204787440e9 * this.t131 * this.t956;
        this.t1003 = this.t18 * this.t140;
        this.t1009 = this.t95 * ex;
        this.t1020 = this.t50 * ex;
        this.t1037 =
            0.1796401152e10 * this.t53 * this.t176 - 0.8126576640e10 * this.t64 * this.t46 - 0.1283143680e10 * this.t44
                * this.t46
                + 0.354731520e9 * this.t138 * this.t1003 * this.t115 + 0.198132480e9 * this.t214 * this.t976
                - 0.9434880e7 * this.t183 * this.t1009
                + 0.17065810944e11 * this.t76 * this.t176 - 0.27731942784e11 * this.t151 * this.t959 + 0.2200947840e10
                * this.t158 * this.t990
                - 0.1559004720e10 * this.t302 * this.t959 - 0.11007360e8 * this.t119 * this.t1020 - 0.1375920e7
                * this.t126 * this.t1020
                - 0.34369920e8 * this.t179 * this.t349 - 0.9434880e7 * this.t214 * this.t176 + 0.2200947840e10
                * this.t151 * this.t75 * this.t140 * this.t23
                + 0.17878468608e11 * this.t572 * this.t422 - 0.88058880e8 * this.t183 * this.t176;
        this.t1046 = this.t962 * this.t7;
        this.t1051 = this.t966 * this.t7;
        this.t1068 = ex * this.t29;
        this.t1075 =
            0.5417717760e10 * this.t104 * this.t46 + 0.1741409280e10 * this.t19 * this.t349 - 0.9751891968e10
                * this.t94 * this.t176 - 0.458640e6
                * this.t123 * this.t46 + 0.51891840e8 * this.t214 * this.t1046 + 0.396264960e9 * this.t183 * this.t1046
                - 0.614362320e9 * this.t165
                * this.t1051 - 0.150492160e9 * this.t572 * this.t86 - 0.1833062400e10 * this.t584 * this.t72
                - 0.916531200e9 * this.t592 * this.t361
                - 0.916531200e9 * this.t569 * this.t207 - 0.752460800e9 * this.t447 * this.t233 - 0.451476480e9
                * this.t495 * this.t384
                - 0.1833062400e10 * this.t598 * this.t26 - 0.293459712e9 * this.t1068 * this.t576 + 0.118243840e9
                * this.t572 * this.t523
                - 0.4041902592e10 * this.t592 * this.t430;
        this.t1080 = this.t63 * this.t31 * this.t7;
        this.t1092 = this.t989 * this.t7;
        this.t1116 =
            -0.4677014160e10 * this.t158 * this.t1080 - 0.4788875520e10 * this.t572 * this.t499 - 0.33014217600e11
                * this.t151 * this.t1080
                - 0.27731942784e11 * this.t1068 * this.t530 - 0.1152877440e10 * this.t138 * this.t983 * this.t23
                + 0.2882193600e10 * this.t146
                * this.t1092 + 0.369847296e9 * this.t466 * this.t480 + 0.189034560e9 * this.t179 * this.t1003
                * this.t23 - 0.451476480e9 * this.t595 * this.t39
                - 0.150492160e9 * this.t563 * this.t454 - 0.317011968e9 * this.t460 * this.t202 - 0.317011968e9
                * this.t466 * this.t79
                - 0.171849600e9 * this.t587 * this.t26 - 0.229132800e9 * this.t569 * this.t72 - 0.68739840e8
                * this.t658 * this.t207 - 0.176117760e9
                * this.t460 * this.t176;
        this.t1147 = this.t463 * this.t18;
        this.t1151 =
            0.824878080e9 * this.t592 * this.t410 + 0.8803791360e10 * this.t1068 * this.t434 - 0.752460800e9
                * this.t581 * this.t317
                - 0.176117760e9 * this.t674 * this.t202 - 0.171849600e9 * this.t598 * this.t361 - 0.68739840e8
                * this.t584 * this.t349
                - 0.264176640e9 * this.t457 * this.t79 - 0.5503680e7 * this.t126 * this.t46 + 0.90295296e8 * this.t30
                * this.t517 - 0.4063288320e10
                * this.t113 * this.t349 + 0.125798400e9 * this.t49 * this.t46 - 0.11007360e8 * this.t119 * this.t46
                + 0.20751793920e11 * this.t138 * this.t1092
                - 0.4536829440e10 * this.t179 * this.t1051 - 0.22014720e8 * this.t664 * this.t46 + 0.10483200e8
                * this.t1147 + 0.51891840e8
                * this.t215 * this.t1020;
        this.t1187 =
            -0.171849600e9 * this.t370 * this.t361 - 0.4536829440e10 * this.t180 * this.t1020 + 0.20751793920e11
                * this.t218 * this.t1020
                + 0.189034560e9 * this.t187 * this.t1009 + 0.2200947840e10 * this.t274 * this.t1009 - 0.4677014160e10
                * this.t159 * this.t1020
                + 0.396264960e9 * this.t184 * this.t1020 - 0.1152877440e10 * this.t171 * this.t1009 - 0.614362320e9
                * this.t271 * this.t1020
                + 0.632067072e9 * this.t513 * this.t86 + 0.1896201216e10 * this.t82 * this.t39 + 0.17414092800e11
                * this.t68 * this.t26
                + 0.51197432832e11 * this.t606 * this.t79 + 0.17414092800e11 * this.t359 * this.t72 + 0.51197432832e11
                * this.t77 * this.t202
                - 0.20316441600e11 * this.t344 * this.t207 + 0.5389203456e10 * this.t352 * this.t202;
        this.t1209 = this.t581 * this.t43;
        this.t1216 = this.t31 * this.t9;
        this.t1219 = this.t572 * this.t63;
        this.t1223 = this.t460 * this.t18;
        this.t1226 = this.t592 * this.t75;
    }

    /**
     * Partial derivative due to 11th order Earth potential zonal harmonics.
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
            derParUdeg11_3(final double a, final double ex, final double ey, final double ix, final double iy) {
        this.t1228 =
            0.3160335360e10 * this.t32 * this.t233 + 0.824878080e9 * this.t587 * this.t18 * this.t23 - 0.33014217600e11
                * this.t162 * this.t1020
                + 0.2364876800e10 * this.t22 * this.t72 + 0.709463040e9 * this.t359 * this.t349 - 0.264176640e9
                * this.t364 * this.t202
                - 0.1760758272e10 * this.t437 * this.t349 + 0.35215165440e11 * this.t21 * this.t29 * this.t75
                * this.t202 + 0.52822748160e11 * this.t200
                * this.t79 - 0.19155502080e11 * this.t1209 * this.t202 + 0.1773657600e10 * this.t355 * this.t26
                + 0.35215165440e11 * this.t77 * this.t176
                - 0.11007360e8 * this.t1216 * this.t7 - 0.7449361920e10 * this.t1219 + 0.4949268480e10 * this.t253
                * this.t79 + 0.739694592e9
                * this.t1223 * this.t46 + 0.1924715520e10 * this.t1226;
        this.t1233 = this.t140 * this.t16;
        this.t1268 =
            -0.105670656e9 * this.t457 * this.t23 - 0.44029440e8 * this.t1233 * this.t23 - 0.1760758272e10 * this.t114
                * this.t29 * this.t43 * this.t207
                - 0.19155502080e11 * this.t379 * this.t176 - 0.16507108800e11 * this.t159 * this.t46 - 0.204787440e9
                * this.t132 * this.t46
                + 0.5389203456e10 * this.t174 * this.t79 + 0.8707046400e10 * this.t22 * this.t207 + 0.8707046400e10
                * this.t347 * this.t361
                + 0.960731200e9 * this.t222 * this.t46 - 0.4041902592e10 * this.t180 * this.t46 - 0.27731942784e11
                * this.t162 * this.t46
                + 0.198132480e9 * this.t215 * this.t46 + 0.17878468608e11 * this.t218 * this.t46 + 0.2200947840e10
                * this.t195 * this.t176
                - 0.37739520e8 * this.t364 * this.t176 + 0.632067072e9 * this.t382 * this.t454;
        this.t1275 = this.t563 * this.t18;
        this.t1282 = this.t595 * this.t75;
        this.t1285 = this.t584 * this.t43;
        this.t1298 = this.t569 * this.t18;
        this.t1309 =
            0.1896201216e10 * this.t315 * this.t384 - 0.29255675904e11 * this.t379 * this.t202 - 0.29255675904e11
                * this.t338 * this.t79
                + 0.709463040e9 * this.t1275 * this.t207 - 0.4401895680e10 * this.t205 * this.t26 - 0.5869194240e10
                * this.t344 * this.t72
                + 0.35756937216e11 * this.t1282 * this.t46 - 0.8083805184e10 * this.t1285 * this.t46 - 0.28733253120e11
                * this.t387 * this.t79
                - 0.55463885568e11 * this.t20 * this.t29 * this.t63 * this.t46 - 0.4401895680e10 * this.t474
                * this.t361 + 0.3299512320e10 * this.t352
                * this.t176 + 0.3299512320e10 * this.t1298 * this.t202 + 0.1773657600e10 * this.t68 * this.t361
                - 0.343699200e9 * this.t367 * this.t72
                - 0.343699200e9 * this.t332 * this.t26 - 0.28304640e8 * this.t373 * this.t79;
        this.t1345 = this.t126 * this.t20;
        this.t1348 =
            -0.293459712e9 * this.t450 * this.t29 * this.t43 * this.t115 - 0.264176640e9 * this.t320 * this.t79
                - 0.28304640e8 * this.t341 * this.t202
                - 0.171849600e9 * this.t329 * this.t207 + 0.10375896960e11 * this.t266 * this.t46 - 0.2268414720e10
                * this.t271 * this.t46
                + 0.1649756160e10 * this.t187 * this.t176 - 0.9577751040e10 * this.t171 * this.t176 + 0.17297280e8
                * this.t243 * this.t46
                + 0.369847296e9 * this.t184 * this.t46 - 0.1152877440e10 * this.t147 * this.t176 - 0.880379136e9
                * this.t152 * this.t349
                + 0.17607582720e11 * this.t274 * this.t176 + 0.189034560e9 * this.t166 * this.t176 - 0.1559004720e10
                * this.t303 * this.t46
                + 0.354731520e9 * this.t139 * this.t349 - 0.2751840e7 * this.t1345 * this.t46;
        this.t1353 = this.t183 * this.t21;
        this.t1364 = this.t119 * this.t20;
        this.t1367 = this.t466 * this.t43;
        this.t1375 = this.t450 * this.t42;
        this.t1386 =
            -0.56609280e8 * this.t376 * this.t79 - 0.40632883200e11 * this.t474 * this.t26 - 0.37739520e8 * this.t1353
                * this.t202
                + 0.3160335360e10 * this.t231 * this.t317 - 0.40632883200e11 * this.t437 * this.t72 - 0.20316441600e11
                * this.t471 * this.t361
                + 0.2882193600e10 * this.t266 * this.t1020 - 0.22014720e8 * this.t1364 * this.t46 - 0.220147200e9
                * this.t1367 - 0.4193280e7
                * this.t463 * this.t7 - 0.183306240e9 * this.t587 * this.t115 + 0.10564549632e11 * this.t1068
                * this.t646 - 0.11456640e8 * this.t1375
                * this.t115 - 0.21498880e8 * this.t451 * this.t100 - 0.31752e5 * this.t122 * ex - 0.317520e6 * this.t58
                * ex - 0.508032e6 * this.t57
                * ex;
        this.t1399 = this.t730 / this.t721 / this.t719 / this.t717 / this.t716;
        this.t1409 = this.t646 * ey;
        this.t1414 = this.t43 * ey;
        this.t1417 = ey * this.t8;
        this.t1418 = this.t107 * this.t1417;
        this.t1421 = this.t75 * ey;
        this.t1428 = this.t69 * this.t37;
        this.t1429 = this.t107 * this.t1428;
        this.t1432 = this.t18 * ey;
        this.t1435 = this.t63 * ey;
        this.t1438 = this.t24 * iy;
        this.t1439 = this.t167 * this.t1438;
        this.t1442 = this.t69 * this.t8;
        this.t1443 = this.t107 * this.t1442;
        this.t1446 = this.t133 * this.t1438;
        this.t1457 =
            0.41597914176e11 * this.t151 * this.t1409 + 0.28062084960e11 * this.t158 * this.t1409 - 0.544864320e9
                * this.t214 * this.t1414
                - 0.28733253120e11 * this.t171 * this.t1418 + 0.7410154752e10 * this.t179 * this.t1421
                - 0.3458632320e10 * this.t147 * this.t1418
                - 0.56216160e8 * this.t242 * this.t1414 - 0.8803791360e10 * this.t152 * this.t1429 + 0.38525760e8
                * this.t119 * this.t1432
                - 0.19454806800e11 * this.t146 * this.t1435 - 0.4611509760e10 * this.t171 * this.t1439
                + 0.13205687040e11 * this.t274 * this.t1443
                - 0.9073658880e10 * this.t180 * this.t1446 - 0.832156416e9 * this.t183 * this.t1414 - 0.105670656e9
                * this.t457 * this.t33 * iy
                - 0.4193280e7 * this.t463 * ix * iy;
        this.t1475 = this.t35 * this.t17;
        this.t1479 = this.t69 * this.t16;
        this.t1483 = this.t35 * this.t42;
        this.t1487 = this.t35 * this.t29;
        this.t1491 = this.t34 * this.t71;
        this.t1492 = this.t133 * this.t1491;
        this.t1495 = this.t514 * this.t17;
        this.t1499 = this.t69 * this.t42;
        this.t1503 = this.t69 * this.t17;
        this.t1507 = this.t69 * this.t29;
        this.t1511 =
            0.4914898560e10 * this.t165 * this.t1421 - 0.458640e6 * this.t123 * this.t1417 - 0.21498880e8 * this.t451
                * this.t452 * iy
                + 0.103783680e9 * this.t215 * this.t1446 - 0.9434880e7 * this.t214 * this.t1428 - 0.88058880e8
                * this.t183 * this.t1428
                + 0.1134207360e10 * this.t187 * this.t1443 + 0.24766560e8 * this.t126 * this.t1432 - 0.4788875520e10
                * this.t1475 * this.t43 * this.t37
                + 0.369847296e9 * this.t1479 * this.t18 * this.t8 + 0.824878080e9 * this.t1483 * this.t18 * this.t37
                + 0.8803791360e10 * this.t1487 * this.t75
                * this.t37 + 0.8803791360e10 * this.t274 * this.t1492 + 0.118243840e9 * this.t1495 * this.t18
                * this.t85 - 0.4041902592e10 * this.t1499
                * this.t43 * this.t8 + 0.17878468608e11 * this.t1503 * this.t75 * this.t8 - 0.27731942784e11
                * this.t1507 * this.t63 * this.t8;
        this.t1519 = this.t71 * this.t24;
        this.t1520 = this.t167 * this.t1519;
        this.t1529 = this.t35 * this.t37;
        this.t1532 = ey * this.t16;
        this.t1554 =
            0.5417717760e10 * this.t104 * this.t1417 - 0.9751891968e10 * this.t94 * this.t1428 - 0.29052511488e11
                * this.t138 * this.t1435
                - 0.8803791360e10 * this.t152 * this.t1520 - 0.1283143680e10 * this.t44 * this.t1417 + 0.1796401152e10
                * this.t53 * this.t1428
                - 0.8126576640e10 * this.t64 * this.t1417 - 0.9434880e7 * this.t183 * this.t1529 - 0.44029440e8
                * this.t1532 * this.t54
                + 0.125798400e9 * this.t49 * this.t1417 + 0.90295296e8 * this.t30 * this.t514 * this.t90 - 0.1375920e7
                * this.t126 * this.t1442
                + 0.2522520e7 * this.t123 * this.t1432 + 0.2962108968e10 * this.t302 * this.t1409 + 0.511968600e9
                * this.t131 * this.t1421
                - 0.2041553800e10 * this.t221 * this.t1435 - 0.293459712e9 * this.t514 * this.t29 * this.t43 * this.t85;
        this.t1557 = this.t9 * ey;
        this.t1561 = this.t54 * this.t1417;
        this.t1564 = this.t35 * this.t85;
        this.t1567 = this.t133 * this.t1519;
        this.t1584 = this.t34 * this.t38;
        this.t1585 = this.t133 * this.t1584;
        this.t1594 =
            0.567103680e9 * this.t166 * this.t1418 - 0.11007360e8 * this.t1557 * this.t107 - 0.145152e6 * ey
                - 0.4401895680e10 * this.t152
                * this.t1561 + 0.1741409280e10 * this.t19 * this.t1564 + 0.6602843520e10 * this.t195 * this.t1567
                - 0.28733253120e11 * this.t171
                * this.t1567 + 0.4949268480e10 * this.t187 * this.t1418 + 0.6602843520e10 * this.t195 * this.t1418
                + 0.4949268480e10 * this.t187
                * this.t1567 + 0.3547315200e10 * this.t139 * this.t1429 + 0.8803791360e10 * this.t274 * this.t1439
                - 0.4611509760e10 * this.t171
                * this.t1492 + 0.1773657600e10 * this.t139 * this.t1585 + 0.41503587840e11 * this.t218 * this.t1446
                - 0.4063288320e10 * this.t113
                * this.t1564 - 0.11007360e8 * this.t119 * this.t1442;
        this.t1597 = this.t42 * ey;
        this.t1633 =
            -0.11456640e8 * this.t1597 * this.t116 - 0.9354028320e10 * this.t159 * this.t1446 - 0.183306240e9
                * this.t587 * this.t141 * iy
                - 0.11007360e8 * this.t119 * this.t1417 - 0.34369920e8 * this.t179 * this.t1564 + 0.52822748160e11
                * this.t274 * this.t1418
                - 0.3458632320e10 * this.t147 * this.t1567 - 0.5503680e7 * this.t126 * this.t1417 - 0.4401895680e10
                * this.t152 * this.t1585
                + 0.17065810944e11 * this.t76 * this.t1428 + 0.3547315200e10 * this.t139 * this.t1520 + 0.567103680e9
                * this.t166 * this.t1567
                + 0.756138240e9 * this.t187 * this.t1492 + 0.756138240e9 * this.t187 * this.t1439 + 0.1773657600e10
                * this.t139 * this.t1561
                + 0.792529920e9 * this.t184 * this.t1446 + 0.5764387200e10 * this.t266 * this.t1446;
        this.t1642 = this.t7 * ey;
        this.t1643 = this.t1642 * this.t8;
        this.t1646 = ix * this.t24;
        this.t1647 = this.t1646 * this.t71;
        this.t1652 = this.t141 * this.t24;
        this.t1653 = this.t1652 * this.t71;
        this.t1658 = this.t18 * this.t69;
        this.t1659 = this.t1658 * this.t8;
        this.t1662 = this.t17 * ey;
        this.t1665 = this.t133 * iy;
        this.t1670 = this.t450 * this.t452;
        this.t1674 = this.t167 * iy;
        this.t1681 =
            0.52822748160e11 * this.t274 * this.t1567 - 0.6917264640e10 * this.t171 * this.t1443 - 0.66028435200e11
                * this.t162 * this.t1446
                - 0.1228724640e10 * this.t271 * this.t1446 - 0.317011968e9 * this.t460 * this.t1643 - 0.317011968e9
                * this.t466 * this.t1647
                + 0.824878080e9 * this.t1597 * this.t441 - 0.451476480e9 * this.t495 * this.t1653 - 0.4041902592e10
                * this.t1597 * this.t418
                + 0.51891840e8 * this.t214 * this.t1659 + 0.17878468608e11 * this.t1662 * this.t414 - 0.8126576640e10
                * this.t64 * this.t1665
                - 0.1283143680e10 * this.t44 * this.t1665 + 0.90295296e8 * this.t30 * this.t1670 * iy - 0.9434880e7
                * this.t214 * this.t1674
                + 0.5417717760e10 * this.t104 * this.t1665 + 0.1796401152e10 * this.t53 * this.t1674;
        this.t1685 = this.t142 * iy;
        this.t1688 = this.t75 * this.t69;
        this.t1689 = this.t1688 * this.t8;
        this.t1694 = this.t16 * this.t24;
        this.t1697 = this.t43 * this.t69;
        this.t1698 = this.t1697 * this.t8;
        this.t1701 = ey * this.t29;
        this.t1704 = this.t18 * this.t35;
        this.t1710 = this.t33 * this.t34;
        this.t1711 = this.t1710 * this.t38;
        this.t1714 = this.t107 * ey;
        this.t1717 = this.t107 * this.t37;
        this.t1720 = this.t133 * this.t38;
        this.t1723 = this.t107 * this.t8;
        this.t1726 = this.t54 * this.t8;
        this.t1729 = this.t34 * this.t42;
        this.t1730 = this.t167 * this.t71;
        this.t1733 = this.t42 * this.t24;
        this.t1736 =
            0.17065810944e11 * this.t76 * this.t1674 - 0.4063288320e10 * this.t113 * this.t1685 + 0.2882193600e10
                * this.t146 * this.t1689
                - 0.22014720e8 * this.t671 * this.t1665 - 0.176117760e9 * this.t1694 * this.t1674 - 0.4536829440e10
                * this.t179 * this.t1698
                + 0.8803791360e10 * this.t1701 * this.t406 + 0.189034560e9 * this.t179 * this.t1704 * this.t37
                + 0.20751793920e11 * this.t138 * this.t1689
                - 0.752460800e9 * this.t447 * this.t1711 - 0.11007360e8 * this.t119 * this.t1714 - 0.171849600e9
                * this.t1483 * this.t1717
                - 0.68739840e8 * this.t683 * this.t1720 - 0.264176640e9 * this.t1479 * this.t1723 - 0.171849600e9
                * this.t1499 * this.t1726
                - 0.229132800e9 * this.t1729 * this.t1730 - 0.68739840e8 * this.t1733 * this.t1685;
        this.t1737 = this.t133 * this.t71;
        this.t1748 = this.t1658 * this.t37;
        this.t1751 = this.t1421 * this.t8;
        this.t1754 = this.t1414 * this.t8;
        this.t1757 = this.t1697 * this.t37;
        this.t1760 = this.t1688 * this.t37;
        this.t1763 = this.t1435 * this.t8;
        this.t1768 = this.t43 * this.t35;
        this.t1775 = this.t63 * this.t69 * this.t8;
        this.t1782 =
            -0.176117760e9 * this.t677 * this.t1737 - 0.9751891968e10 * this.t94 * this.t1674 - 0.88058880e8
                * this.t183 * this.t1674
                - 0.5503680e7 * this.t126 * this.t1665 + 0.125798400e9 * this.t49 * this.t1665 + 0.189034560e9
                * this.t165 * this.t1748
                + 0.960731200e9 * this.t221 * this.t1751 - 0.204787440e9 * this.t131 * this.t1754 - 0.1152877440e10
                * this.t146 * this.t1757
                + 0.2200947840e10 * this.t158 * this.t1760 - 0.1559004720e10 * this.t302 * this.t1763
                + 0.17878468608e11 * this.t138 * this.t1751
                - 0.880379136e9 * this.t151 * this.t1768 * this.t85 + 0.396264960e9 * this.t183 * this.t1659
                - 0.33014217600e11 * this.t151 * this.t1775
                - 0.4788875520e10 * this.t1662 * this.t490 + 0.118243840e9 * this.t1662 * this.t540;
        this.t1797 = ix * this.t83;
        this.t1798 = this.t1797 * this.t516;
        this.t1805 = ix * this.t34;
        this.t1806 = this.t1805 * this.t38;
        this.t1809 = this.t7 * this.t35;
        this.t1810 = this.t1809 * this.t85;
        this.t1813 = this.t33 * this.t24;
        this.t1814 = this.t1813 * this.t71;
        this.t1817 = this.t7 * this.t69;
        this.t1818 = this.t1817 * this.t37;
        this.t1821 = this.t23 * this.t69;
        this.t1822 = this.t1821 * this.t37;
        this.t1825 = this.t115 * ey;
        this.t1826 = this.t1825 * this.t8;
        this.t1829 = this.t23 * ey;
        this.t1830 = this.t1829 * this.t8;
        this.t1835 = this.t54 * ey;
        this.t1838 =
            -0.614362320e9 * this.t165 * this.t1698 - 0.1152877440e10 * this.t138 * this.t1768 * this.t37
                + 0.369847296e9 * this.t1532 * this.t534
                + 0.2200947840e10 * this.t151 * this.t75 * this.t35 * this.t37 - 0.150492160e9 * this.t572 * this.t1798
                - 0.293459712e9 * this.t1701
                * this.t558 - 0.27731942784e11 * this.t1701 * this.t503 - 0.916531200e9 * this.t592 * this.t1806
                - 0.451476480e9 * this.t595 * this.t1810
                - 0.1833062400e10 * this.t598 * this.t1814 - 0.1833062400e10 * this.t584 * this.t1818 - 0.752460800e9
                * this.t581 * this.t1822
                - 0.150492160e9 * this.t563 * this.t1826 - 0.916531200e9 * this.t569 * this.t1830 - 0.1375920e7
                * this.t126 * this.t1714
                - 0.9434880e7 * this.t183 * this.t1835;
        this.t1845 = this.t1432 * this.t8;
        this.t1863 = this.t24 * this.t17;
        this.t1870 = this.t34 * this.t29;
    }

    /**
     * Partial derivative due to 11th order Earth potential zonal harmonics.
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
            derParUdeg11_4(final double a, final double ex, final double ey, final double ix, final double iy) {
        this.t1884 =
            0.10375896960e11 * this.t146 * this.t1751 + 0.1649756160e10 * this.t179 * this.t1748 + 0.17607582720e11
                * this.t151 * this.t1760
                + 0.369847296e9 * this.t183 * this.t1845 + 0.198132480e9 * this.t214 * this.t1845 - 0.16507108800e11
                * this.t158 * this.t1763
                + 0.354731520e9 * this.t138 * this.t1704 * this.t85 + 0.10483200e8 * this.t1557 * this.t18
                - 0.34369920e8 * this.t179 * this.t1685
                - 0.31752e5 * this.t122 * ey - 0.317520e6 * this.t58 * ey - 0.19155502080e11 * this.t1863 * this.t43
                * this.t1674
                + 0.1773657600e10 * this.t1503 * this.t18 * this.t1726 + 0.35215165440e11 * this.t1870 * this.t75
                * this.t1737 + 0.3299512320e10
                * this.t1729 * this.t18 * this.t1737 + 0.3299512320e10 * this.t1733 * this.t18 * this.t1674
                + 0.709463040e9 * this.t83 * this.t17 * this.t18 * this.t1720;
        this.t1886 = this.t34 * this.t17;
        this.t1896 = this.t24 * this.t29;
        this.t1938 =
            0.2364876800e10 * this.t1886 * this.t18 * this.t1730 - 0.4401895680e10 * this.t1507 * this.t43 * this.t1726
                + 0.52822748160e11
                * this.t1507 * this.t75 * this.t1723 + 0.35215165440e11 * this.t1896 * this.t75 * this.t1674
                - 0.1760758272e10 * this.t83 * this.t29 * this.t43
                * this.t1720 + 0.4949268480e10 * this.t1499 * this.t18 * this.t1723 - 0.1760758272e10 * this.t1896
                * this.t43 * this.t1685 + 0.739694592e9
                * this.t1694 * this.t18 * this.t1665 + 0.1773657600e10 * this.t1475 * this.t18 * this.t1717
                - 0.19155502080e11 * this.t1886 * this.t43 * this.t1737
                + 0.709463040e9 * this.t1863 * this.t18 * this.t1685 - 0.4401895680e10 * this.t1487 * this.t43
                * this.t1717 - 0.28733253120e11
                * this.t1503 * this.t43 * this.t1723 - 0.55463885568e11 * this.t1896 * this.t63 * this.t1665
                - 0.220147200e9 * this.t1532 * this.t43
                - 0.458640e6 * this.t123 * this.t1665 - 0.8083805184e10 * this.t1733 * this.t43 * this.t1665;
        this.t1958 = this.t1646 * iy;
        this.t1977 =
            -0.5869194240e10 * this.t1870 * this.t43 * this.t1730 + 0.35756937216e11 * this.t1863 * this.t75
                * this.t1665 + 0.1649756160e10
                * this.t187 * this.t1674 - 0.2268414720e10 * this.t271 * this.t1665 - 0.21498880e8 * this.t1495
                * this.t90 - 0.105670656e9 * this.t1479
                * this.t37 - 0.56609280e8 * this.t364 * this.t1817 * this.t8 - 0.1152877440e10 * this.t147 * this.t1674
                - 0.2751840e7 * this.t508 * this.t1958
                - 0.880379136e9 * this.t152 * this.t1685 + 0.369847296e9 * this.t184 * this.t1665 + 0.17297280e8
                * this.t243 * this.t1665
                + 0.5389203456e10 * this.t174 * this.t1647 + 0.17414092800e11 * this.t68 * this.t1814
                + 0.51197432832e11 * this.t77 * this.t1643
                + 0.3160335360e10 * this.t231 * this.t1822 + 0.1896201216e10 * this.t315 * this.t1653;
        this.t2015 =
            -0.28304640e8 * this.t341 * this.t1643 + 0.8707046400e10 * this.t22 * this.t1830 + 0.3160335360e10
                * this.t32 * this.t1711
                + 0.1896201216e10 * this.t82 * this.t1810 + 0.51197432832e11 * this.t606 * this.t1647 - 0.37739520e8
                * this.t376 * this.t1813 * iy
                + 0.17607582720e11 * this.t274 * this.t1674 + 0.2200947840e10 * this.t195 * this.t1674
                + 0.10375896960e11 * this.t266 * this.t1665
                - 0.1559004720e10 * this.t303 * this.t1665 - 0.264176640e9 * this.t320 * this.t1647 - 0.343699200e9
                * this.t332 * this.t1814
                + 0.20751793920e11 * this.t218 * this.t1714 - 0.4536829440e10 * this.t180 * this.t1714 - 0.28304640e8
                * this.t373 * this.t1647
                - 0.264176640e9 * this.t364 * this.t1643 - 0.171849600e9 * this.t329 * this.t1830;
        this.t2051 =
            -0.20316441600e11 * this.t471 * this.t1806 - 0.40632883200e11 * this.t474 * this.t1814 - 0.40632883200e11
                * this.t437 * this.t1818
                - 0.343699200e9 * this.t367 * this.t1818 - 0.171849600e9 * this.t370 * this.t1806 - 0.22014720e8
                * this.t393 * this.t1958
                + 0.632067072e9 * this.t513 * this.t1798 - 0.29255675904e11 * this.t379 * this.t1643 - 0.11007360e8
                * this.t119 * this.t1665
                - 0.9577751040e10 * this.t171 * this.t1674 + 0.632067072e9 * this.t382 * this.t1826 + 0.17414092800e11
                * this.t359 * this.t1818
                + 0.8707046400e10 * this.t347 * this.t1806 - 0.20316441600e11 * this.t344 * this.t1830
                - 0.29255675904e11 * this.t338 * this.t1647
                + 0.1741409280e10 * this.t19 * this.t1685 - 0.37739520e8 * this.t320 * this.t1805 * this.t71;
        this.t2087 =
            -0.33014217600e11 * this.t162 * this.t1714 - 0.9577751040e10 * this.t138 * this.t1757 + 0.5389203456e10
                * this.t352 * this.t1643
                - 0.4041902592e10 * this.t179 * this.t1754 + 0.17297280e8 * this.t242 * this.t1845 - 0.4677014160e10
                * this.t159 * this.t1714
                - 0.614362320e9 * this.t271 * this.t1714 + 0.189034560e9 * this.t187 * this.t1835 - 0.1152877440e10
                * this.t171 * this.t1835
                + 0.2882193600e10 * this.t266 * this.t1714 + 0.189034560e9 * this.t166 * this.t1674 - 0.4041902592e10
                * this.t180 * this.t1665
                - 0.27731942784e11 * this.t162 * this.t1665 + 0.17878468608e11 * this.t218 * this.t1665 + 0.960731200e9
                * this.t222 * this.t1665
                + 0.198132480e9 * this.t215 * this.t1665 + 0.354731520e9 * this.t139 * this.t1685;
        this.t2125 =
            -0.204787440e9 * this.t132 * this.t1665 - 0.16507108800e11 * this.t159 * this.t1665 - 0.11456640e8
                * this.t514 * this.t42 * this.t85
                + 0.10564549632e11 * this.t1701 * this.t646 - 0.2268414720e10 * this.t165 * this.t1754
                + 0.2200947840e10 * this.t274 * this.t1835
                + 0.396264960e9 * this.t184 * this.t1714 + 0.51891840e8 * this.t215 * this.t1714 - 0.4677014160e10
                * this.t158 * this.t1775
                - 0.27731942784e11 * this.t151 * this.t1763 - 0.7449361920e10 * this.t1662 * this.t63 - 0.4193280e7
                * this.t1557 * this.t8
                + 0.1924715520e10 * this.t1597 * this.t75 - 0.183306240e9 * this.t1483 * this.t85 - 0.44029440e8
                * this.t35 * this.t16 * this.t37
                - 0.11007360e8 * this.t69 * this.t9 * this.t8 - 0.508032e6 * this.t57 * ey;
        this.t2141 = this.t1 * this.t6 / this.t10;
        this.t2150 = this.t114 * this.t452;
        this.t2169 = this.t100 * ix;
        this.t2173 = this.t738 * this.t134;
        this.t2177 = this.t114 * this.t141;
        this.t2182 =
            0.17065810944e11 * this.t76 * this.t802 + 0.6772147200e10 * this.t94 * this.t2150 - 0.2708858880e10
                * this.t30 * this.t2150
                + 0.580469760e9 * this.t940 * this.t110 + 0.40632883200e11 * this.t696 * this.t824 - 0.32506306560e11
                * this.t76 * this.t824
                - 0.21670871040e11 * this.t694 * this.t824 + 0.16253153280e11 * this.t94 * this.t824 - 0.9751891968e10
                * this.t94 * this.t802
                - 0.458640e6 * this.t123 * this.t777 - 0.112869120e9 * this.t19 * this.t99 * this.t2169 + 0.8386560e7
                * this.t2173 - 0.183306240e9
                * this.t592 * this.t262 - 0.34369920e8 * this.t179 * this.t2177 - 0.9434880e7 * this.t214 * this.t802;
        this.t2192 = this.t57 * ix;
        this.t2195 = this.t777 * this.t50;
        this.t2198 = this.t122 * ix;
        this.t2209 = this.t735 * this.t134;
        this.t2214 = this.t824 * this.t50;
        this.t2221 =
            -0.21498880e8 * this.t572 * this.t941 + 0.22573824e8 * this.t29 * ix * this.t91 + 0.88058880e8 * this.t119
                * this.t787
                + 0.898200576e9 * this.t758 * this.t95 + 0.11007360e8 * this.t2192 * this.t50 + 0.4949268480e10
                * this.t187 * this.t2195 + 0.458640e6
                * this.t2198 * this.t50 - 0.2694601728e10 * this.t49 * this.t787 + 0.125798400e9 * this.t944 * this.t50
                + 0.25598716416e11 * this.t113
                * this.t787 - 0.42664527360e11 * this.t104 * this.t787 - 0.38311004160e11 * this.t139 * this.t2209
                + 0.76622008320e11 * this.t180
                * this.t2209 - 0.57466506240e11 * this.t139 * this.t2214 + 0.114933012480e12 * this.t180 * this.t2214
                - 0.9751891968e10 * this.t19
                * this.t787;
        this.t2229 = this.t9 * this.t34;
        this.t2237 = this.t146 * this.t18;
        this.t2238 = this.t738 * this.t196;
        this.t2243 = this.t787 * this.t50;
        this.t2246 = this.t42 * this.t89;
        this.t2254 = this.t43 * ix;
        this.t2257 = this.t75 * ix;
        this.t2260 = this.t16 * this.t83;
        this.t2264 =
            0.19503783936e11 * this.t44 * this.t787 - 0.221855542272e12 * this.t274 * this.t2173 + 0.277319427840e12
                * this.t651 * this.t2173
                + 0.105670656e9 * this.t2229 * this.t37 * ix + 0.158468244480e12 * this.t152 * this.t2214
                - 0.264113740800e12 * this.t218
                * this.t2214 - 0.4611509760e10 * this.t2237 * this.t2238 + 0.9223019520e10 * this.t271 * this.t2238
                - 0.7094630400e10 * this.t187
                * this.t2243 + 0.21498880e8 * this.t2246 * this.t90 * ix + 0.118243840e9 * this.t138 * this.t2150
                + 0.198132480e9 * this.t214 * this.t824
                + 0.5774146560e10 * this.t179 * this.t2254 - 0.5774146560e10 * this.t183 * this.t2257 + 0.183306240e9
                * this.t2260 * this.t85 * ix;
        this.t2265 = this.t63 * ix;
        this.t2270 = this.t646 * ix;
        this.t2273 = this.t735 * this.t196;
        this.t2292 = this.t302 * this.t75;
        this.t2299 = this.t158 * this.t43;
        this.t2302 =
            0.2041553800e10 * this.t637 * this.t2265 + 0.46770141600e11 * this.t302 * this.t2265 - 0.46770141600e11
                * this.t221 * this.t2270
                - 0.9459507200e10 * this.t187 * this.t2273 + 0.105645496320e12 * this.t152 * this.t2209 + 0.440294400e9
                * this.t119 * this.t2254
                - 0.176075827200e12 * this.t218 * this.t2209 - 0.1134207360e10 * this.t215 * this.t2238
                + 0.103994785440e12 * this.t158 * this.t2265
                - 0.103994785440e12 * this.t146 * this.t2270 + 0.369847296e9 * this.t183 * this.t824 + 0.17297280e8
                * this.t242 * this.t824
                - 0.12472037760e11 * this.t2292 * this.t2173 + 0.15590047200e11 * this.t629 * this.t2173
                + 0.58105022976e11 * this.t165 * this.t2265
                + 0.13205687040e11 * this.t2299 * this.t2209;
        this.t2311 = this.t738 * this.t262;
        this.t2314 = this.t761 * this.t134;
        this.t2317 = this.t151 * this.t18;
        this.t2328 = this.t824 * this.t95;
        this.t2339 =
            0.383976450e9 * this.t637 * this.t2254 - 0.22009478400e11 * this.t266 * this.t2209 - 0.383976450e9
                * this.t626 * this.t2257
                - 0.2837852160e10 * this.t187 * this.t2311 - 0.3458632320e10 * this.t147 * this.t2314 - 0.3521516544e10
                * this.t2317 * this.t2311
                + 0.8803791360e10 * this.t171 * this.t2311 - 0.2041553800e10 * this.t653 * this.t2257
                + 0.29797447680e11 * this.t179 * this.t2265
                + 0.13834529280e11 * this.t271 * this.t2214 - 0.8803791360e10 * this.t2317 * this.t2328
                + 0.22009478400e11 * this.t171 * this.t2328
                - 0.58105022976e11 * this.t146 * this.t2257 - 0.1134207360e10 * this.t215 * this.t2209
                - 0.3702636210e10 * this.t653 * this.t2270;
        this.t2348 = this.t131 * this.t18;
        this.t2353 = this.t761 * this.t196;
        this.t2364 = this.t58 * ix;
        this.t2367 = ix * this.t18;
        this.t2376 =
            0.62255381760e11 * this.t147 * this.t2173 - 0.83007175680e11 * this.t624 * this.t2173 - 0.6917264640e10
                * this.t2237 * this.t2214
                - 0.29797447680e11 * this.t138 * this.t2257 - 0.819149760e9 * this.t2348 * this.t2173 + 0.1228724640e10
                * this.t649 * this.t2173
                + 0.3547315200e10 * this.t139 * this.t2353 - 0.4914898560e10 * this.t242 * this.t2257 + 0.3702636210e10
                * this.t661 * this.t2265
                - 0.132056870400e12 * this.t195 * this.t2173 + 0.9434880e7 * this.t126 * this.t787 + 0.5503680e7
                * this.t2364 * this.t50
                - 0.832156416e9 * this.t214 * this.t2367 + 0.832156416e9 * this.t126 * this.t2254 + 0.165071088000e12
                * this.t642 * this.t2173
                + 0.4914898560e10 * this.t131 * this.t2254;
        this.t2378 = this.t747 * this.t134;
        this.t2401 = this.t773 * this.t134;
        this.t2412 =
            -0.2837852160e10 * this.t187 * this.t2378 - 0.440294400e9 * this.t183 * this.t2367 + 0.34369920e8
                * this.t183 * this.t2150
                + 0.28108080e8 * this.t60 * this.t2254 - 0.363242880e9 * this.t242 * this.t2367 + 0.363242880e9
                * this.t123 * this.t2254
                - 0.25939742400e11 * this.t221 * this.t2257 + 0.25939742400e11 * this.t131 * this.t2265
                - 0.11115232128e11 * this.t214 * this.t2257
                + 0.52822748160e11 * this.t151 * this.t2265 - 0.52822748160e11 * this.t138 * this.t2270
                + 0.1773657600e10 * this.t139 * this.t2401
                - 0.28108080e8 * this.t626 * this.t2367 - 0.251596800e9 * this.t692 * this.t824 - 0.2321879040e10
                * this.t53 * this.t2150
                + 0.3849431040e10 * this.t698 * this.t824;
        this.t2415 = this.t777 * this.t95;
        this.t2446 =
            -0.2566287360e10 * this.t53 * this.t824 + 0.1773657600e10 * this.t139 * this.t2415 + 0.11115232128e11
                * this.t165 * this.t2254
                + 0.1741409280e10 * this.t19 * this.t2177 + 0.125798400e9 * this.t49 * this.t777 - 0.1283143680e10
                * this.t44 * this.t777
                + 0.1796401152e10 * this.t53 * this.t802 - 0.88058880e8 * this.t183 * this.t802 + 0.52822748160e11
                * this.t274 * this.t2314
                - 0.4401895680e10 * this.t152 * this.t2415 + 0.19808530560e11 * this.t2299 * this.t2214
                - 0.33014217600e11 * this.t266 * this.t2214
                + 0.824878080e9 * this.t179 * this.t787 - 0.9898536960e10 * this.t184 * this.t2238 - 0.9898536960e10
                * this.t184 * this.t2209
                - 0.4193280e7 * this.t463 * this.t134;
        this.t2481 =
            -0.105670656e9 * this.t466 * this.t196 + 0.5417717760e10 * this.t104 * this.t777 - 0.9073658880e10
                * this.t166 * this.t2173
                + 0.13610488320e11 * this.t633 * this.t2173 - 0.5503680e7 * this.t126 * this.t777 + 0.52822748160e11
                * this.t274 * this.t2195
                + 0.90295296e8 * this.t30 * this.t99 * this.t452 - 0.8126576640e10 * this.t64 * this.t777
                - 0.4063288320e10 * this.t113 * this.t2177
                + 0.4949268480e10 * this.t187 * this.t2314 + 0.567103680e9 * this.t166 * this.t2314 + 0.94517280e8
                * this.t165 * this.t787
                - 0.3458632320e10 * this.t147 * this.t2195 - 0.7094630400e10 * this.t187 * this.t2328 - 0.11007360e8
                * this.t119 * this.t777;
        this.t2484 = this.t802 * this.t50;
        this.t2493 = this.t1646 * this.t8;
        this.t2496 = this.t440 * this.t141;
        this.t2500 = this.t23 * this.t35 * this.t38;
        this.t2504 = this.t100 * ey * iy;
        this.t2507 = this.t1829 * iy;
        this.t2510 = this.t417 * this.t33;
        this.t2513 = this.t1797 * this.t85;
        this.t2516 = this.t1817 * this.t71;
        this.t2519 = this.t95 * ix;
        this.t2526 =
            0.567103680e9 * this.t166 * this.t2195 - 0.8803791360e10 * this.t152 * this.t2484 + 0.6602843520e10
                * this.t195 * this.t2195
                + 0.3547315200e10 * this.t139 * this.t2484 - 0.28733253120e11 * this.t171 * this.t2314 + 0.369847296e9
                * this.t183 * this.t2493
                - 0.283551840e9 * this.t214 * this.t2496 + 0.1203937280e10 * this.t598 * this.t2500 + 0.171991040e9
                * this.t1375 * this.t2504
                + 0.422682624e9 * this.t1216 * this.t2507 + 0.12125707776e11 * this.t183 * this.t2510 - 0.150492160e9
                * this.t595 * this.t2513
                - 0.1833062400e10 * this.t598 * this.t2516 - 0.2694601728e10 * this.t49 * this.t2519 - 0.2566287360e10
                * this.t53 * this.t2493
                + 0.3849431040e10 * this.t698 * this.t2493;
    }

    /**
     * Partial derivative due to 11th order Earth potential zonal harmonics.
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
            derParUdeg11_5(final double a, final double ex, final double ey, final double ix, final double iy) {
        this.t2536 = this.t533 * this.t33;
        this.t2542 = this.t502 * ix;
        this.t2545 = this.t533 * ix;
        this.t2548 = this.t489 * this.t33;
        this.t2553 = ey * iy * ex;
        this.t2562 = this.t31 * ey * iy;
        this.t2567 = this.t413 * this.t33;
        this.t2570 =
            0.9434880e7 * this.t126 * this.t2519 + 0.88058880e8 * this.t119 * this.t2519 - 0.32506306560e11 * this.t76
                * this.t2493
                + 0.40632883200e11 * this.t696 * this.t2493 - 0.8083805184e10 * this.t179 * this.t2536 + 0.354731520e9
                * this.t138 * this.t539 * this.t141
                - 0.27731942784e11 * this.t151 * this.t2542 + 0.17297280e8 * this.t242 * this.t2545 - 0.9577751040e10
                * this.t138 * this.t2548
                + 0.22014720e8 * this.t57 * this.t7 * this.t2553 + 0.118243840e9 * this.t138 * this.t2513
                + 0.369847296e9 * this.t183 * this.t2545
                + 0.3592802304e10 * this.t42 * this.t23 * this.t2562 + 0.53635405824e11 * this.t138 * this.t2510
                - 0.71513874432e11 * this.t179
                * this.t2567;
        this.t2572 = this.t115 * this.t69 * this.t71;
        this.t2581 = this.t502 * this.t33;
        this.t2590 = this.t140 * ey * iy;
        this.t2595 = this.t539 * this.t452;
        this.t2601 = this.t489 * this.t141;
        this.t2606 = this.t413 * ix;
        this.t2610 = this.t33 * this.t83 * this.t85;
        this.t2613 = this.t1821 * this.t71;
        this.t2616 =
            0.1203937280e10 * this.t587 * this.t2572 - 0.409574880e9 * this.t131 * this.t2536 + 0.614362320e9
                * this.t242 * this.t2510
                - 0.6236018880e10 * this.t302 * this.t2567 + 0.7795023600e10 * this.t221 * this.t2581
                - 0.66028435200e11 * this.t158 * this.t2567
                + 0.82535544000e11 * this.t146 * this.t2581 + 0.3482818560e10 * this.t17 * this.t115 * this.t2590
                - 0.34594560e8 * this.t123 * this.t2536
                - 0.586919424e9 * this.t151 * this.t2595 + 0.1467298560e10 * this.t138 * this.t557 * this.t452
                + 0.3301421760e10 * this.t158 * this.t2601
                - 0.16507108800e11 * this.t158 * this.t2542 + 0.17878468608e11 * this.t138 * this.t2606 + 0.601968640e9
                * this.t584 * this.t2610
                + 0.3666124800e10 * this.t457 * this.t2613;
        this.t2619 = this.t1710 * this.t37;
        this.t2622 = this.t1825 * iy;
        this.t2626 = this.t141 * this.t34 * this.t37;
        this.t2630 = this.t452 * this.t24 * this.t8;
        this.t2633 = this.t1652 * this.t8;
        this.t2643 = ex * this.t69 * this.t71;
        this.t2651 = ex * this.t35 * this.t38;
        this.t2654 = this.t440 * this.t33;
        this.t2661 =
            0.2749593600e10 * this.t460 * this.t2619 + 0.1099837440e10 * this.t1233 * this.t2622 + 0.1504921600e10
                * this.t569 * this.t2626
                + 0.601968640e9 * this.t658 * this.t2630 + 0.2749593600e10 * this.t674 * this.t2633 - 0.110927771136e12
                * this.t151 * this.t2567
                + 0.5417717760e10 * this.t104 * this.t2553 - 0.1559004720e10 * this.t302 * this.t2542 - 0.9751891968e10
                * this.t94 * this.t2643
                - 0.458640e6 * this.t123 * this.t2553 + 0.34369920e8 * this.t183 * this.t2513 + 0.1741409280e10
                * this.t19 * this.t2651
                + 0.1649756160e10 * this.t179 * this.t2654 + 0.25598716416e11 * this.t113 * this.t2519
                - 0.42664527360e11 * this.t104 * this.t2519;
        this.t2670 = this.t1809 * this.t38;
        this.t2695 =
            -0.2708858880e10 * this.t30 * this.t2513 + 0.6772147200e10 * this.t94 * this.t2513 - 0.150492160e9
                * this.t451 * this.t2622
                + 0.189034560e9 * this.t165 * this.t2654 - 0.451476480e9 * this.t447 * this.t2670 - 0.34369920e8
                * this.t179 * this.t2651
                + 0.960731200e9 * this.t221 * this.t2606 - 0.9577751040e10 * this.t138 * this.t2496 + 0.19155502080e11
                * this.t179 * this.t2601
                - 0.472975360e9 * this.t179 * this.t2595 - 0.396264960e9 * this.t126 * this.t2536 + 0.198132480e9
                * this.t214 * this.t2493
                - 0.21670871040e11 * this.t694 * this.t2493 - 0.2321879040e10 * this.t53 * this.t2513 - 0.9751891968e10
                * this.t19 * this.t2519
                + 0.19503783936e11 * this.t44 * this.t2519;
        this.t2699 = this.t21 * this.t24 * this.t8;
        this.t2714 = ex * this.t514 * this.t516;
        this.t2717 = this.t417 * ix;
        this.t2733 = this.t20 * this.t24 * this.t8;
        this.t2746 =
            0.8707046400e10 * this.t17 * this.t141 * this.t2699 + 0.632067072e9 * this.t29 * this.t33 * this.t20
                * this.t83 * this.t85 + 0.125798400e9
                * this.t49 * this.t2553 - 0.11007360e8 * this.t119 * this.t2553 - 0.5503680e7 * this.t126 * this.t2553
                + 0.90295296e8 * this.t30 * this.t2714
                - 0.2268414720e10 * this.t165 * this.t2717 + 0.31127690880e11 * this.t146 * this.t2510
                - 0.41503587840e11 * this.t165 * this.t2567
                - 0.4536829440e10 * this.t165 * this.t2536 + 0.6805244160e10 * this.t214 * this.t2510 + 0.11007360e8
                * this.t58 * this.t7 * this.t2553
                + 0.5389203456e10 * this.t42 * this.t33 * this.t2733 + 0.1580167680e10 * this.t29 * this.t141
                * this.t21 * this.t34 * this.t37
                + 0.3592802304e10 * this.t42 * this.t7 * this.t2643 + 0.107270811648e12 * this.t171 * this.t2173;
        this.t2749 = this.t31 * this.t69 * this.t71;
        this.t2756 = this.t405 * this.t141;
        this.t2784 =
            0.11609395200e11 * this.t17 * this.t23 * this.t2749 + 0.824878080e9 * this.t179 * this.t2519
                + 0.26411374080e11 * this.t151 * this.t2601
                - 0.44018956800e11 * this.t138 * this.t2756 + 0.94517280e8 * this.t165 * this.t2519 + 0.2882193600e10
                * this.t221 * this.t2510
                - 0.143027748864e12 * this.t690 * this.t2173 - 0.3842924800e10 * this.t131 * this.t2567
                + 0.105645496320e12 * this.t152 * this.t2238
                - 0.176075827200e12 * this.t218 * this.t2238 + 0.17065810944e11 * this.t76 * this.t2643 - 0.112869120e9
                * this.t19 * this.t91 * ix
                - 0.752460800e9 * this.t495 * this.t2613 - 0.4041902592e10 * this.t179 * this.t2717 + 0.16253153280e11
                * this.t94 * this.t2493
                + 0.6602843520e10 * this.t195 * this.t2314;
        this.t2819 =
            -0.28733253120e11 * this.t171 * this.t2195 + 0.9223019520e10 * this.t271 * this.t2209 - 0.4611509760e10
                * this.t2237 * this.t2209
                - 0.69189120e8 * this.t631 * this.t2173 - 0.3521516544e10 * this.t2317 * this.t2378 + 0.8803791360e10
                * this.t171 * this.t2378
                - 0.38311004160e11 * this.t139 * this.t2238 + 0.76622008320e11 * this.t180 * this.t2238
                - 0.8803791360e10 * this.t2317 * this.t2243
                + 0.22009478400e11 * this.t171 * this.t2243 - 0.1479389184e10 * this.t686 * this.t2173
                - 0.14847805440e11 * this.t184 * this.t2214
                - 0.1701311040e10 * this.t215 * this.t2214 + 0.13205687040e11 * this.t2299 * this.t2238
                - 0.22009478400e11 * this.t266 * this.t2238;
        this.t2834 = this.t221 * this.t43;
        this.t2845 = this.t1813 * this.t8;
        this.t2855 = this.t20 * this.t34 * this.t37;
        this.t2860 =
            -0.7685849600e10 * this.t640 * this.t2173 - 0.16167610368e11 * this.t187 * this.t2173 + 0.24251415552e11
                * this.t603 * this.t2173
                - 0.8803791360e10 * this.t152 * this.t2353 - 0.11738388480e11 * this.t2317 * this.t2273
                + 0.29345971200e11 * this.t171 * this.t2273
                - 0.792529920e9 * this.t644 * this.t2173 + 0.5764387200e10 * this.t2834 * this.t2173 + 0.22573824e8
                * this.t29 * this.t2169 * this.t99
                + 0.458640e6 * this.t122 * this.t33 * this.t20 - 0.1283143680e10 * this.t44 * this.t2553
                + 0.634023936e9 * this.t664 * this.t2845
                + 0.11007360e8 * this.t57 * this.t33 * this.t20 + 0.10483200e8 * this.t119 * ix + 0.8707046400e10
                * this.t17 * this.t33 * this.t2855
                - 0.10483200e8 * this.t2192 * this.t18;
        this.t2868 = this.t7 * this.t514 * this.t516;
        this.t2883 = this.t119 * this.t31;
        this.t2889 = this.t19 * this.t450;
        this.t2894 =
            -0.9434880e7 * this.t214 * this.t2643 + 0.251596800e9 * this.t16 * this.t7 * this.t2553 + 0.171991040e9
                * this.t592 * this.t2868
                + 0.10375896960e11 * this.t146 * this.t2606 + 0.31127690880e11 * this.t147 * this.t2493 - 0.204787440e9
                * this.t131 * this.t2717
                - 0.1048320e7 * this.t2367 + 0.17607582720e11 * this.t274 * this.t2643 - 0.34594560e8 * this.t631
                * this.t2493 + 0.1048320e7
                * this.t937 + 0.352235520e9 * this.t2883 * this.t2507 + 0.4193280e7 * this.t824 - 0.1152877440e10
                * this.t146 * this.t2548
                - 0.902952960e9 * this.t2889 * this.t2504 - 0.58511351808e11 * this.t359 * this.t2845;
        this.t2930 =
            0.117022703616e12 * this.t1285 * this.t2845 - 0.4401895680e10 * this.t152 * this.t2401 - 0.27731942784e11
                * this.t162 * this.t2553
                - 0.1152877440e10 * this.t146 * this.t2496 - 0.739694592e9 * this.t686 * this.t2493 - 0.4041902592e10
                * this.t180 * this.t2553
                + 0.206219520e9 * this.t320 * this.t2670 - 0.1152877440e10 * this.t147 * this.t2643 + 0.515548800e9
                * this.t364 * this.t2619
                - 0.41503587840e11 * this.t624 * this.t2493 + 0.189034560e9 * this.t166 * this.t2643 + 0.37739520e8
                * this.t508 * this.t2516
                - 0.1559004720e10 * this.t303 * this.t2553 + 0.3299512320e10 * this.t179 * this.t23 * this.t2562
                + 0.1773657600e10 * this.t138 * this.t33
                * this.t2855 + 0.3299512320e10 * this.t179 * this.t7 * this.t2643;
        this.t2937 = this.t42 * this.t99;
        this.t2944 = this.t1642 * iy;
        this.t2956 = this.t9 * this.t21;
        this.t2959 = this.t16 * this.t114;
        this.t2966 =
            -0.880379136e9 * this.t152 * this.t2651 + 0.17297280e8 * this.t242 * this.t2493 + 0.21498880e8 * this.t2937
                * this.t2169
                + 0.528353280e9 * this.t1364 * this.t2845 - 0.16167610368e11 * this.t1223 * this.t2845 - 0.317011968e9
                * this.t457 * this.t2944
                + 0.4193280e7 * this.t2493 - 0.317011968e9 * this.t460 * this.t2493 + 0.354731520e9 * this.t139
                * this.t2651 - 0.2474634240e10
                * this.t184 * this.t2519 + 0.8255520e7 * this.t123 * ix + 0.105670656e9 * this.t2956 * this.t141
                + 0.183306240e9 * this.t2959 * this.t452
                - 0.34828185600e11 * this.t352 * this.t2619 - 0.13931274240e11 * this.t174 * this.t2670;
        this.t2971 = this.t53 * this.t140;
        this.t2996 = this.t183 * this.t140;
        this.t3001 =
            -0.44018956800e11 * this.t218 * this.t2519 + 0.10375896960e11 * this.t266 * this.t2553 - 0.13931274240e11
                * this.t2971 * this.t2622
                - 0.4536829440e10 * this.t166 * this.t2493 + 0.6805244160e10 * this.t633 * this.t2493
                + 0.26411374080e11 * this.t152 * this.t2519
                - 0.204787440e9 * this.t132 * this.t2553 - 0.16507108800e11 * this.t159 * this.t2553 + 0.56609280e8
                * this.t1345 * this.t2845
                - 0.40632883200e11 * this.t231 * this.t2633 + 0.101582208000e12 * this.t1209 * this.t2633
                - 0.39007567872e11 * this.t347 * this.t2516
                + 0.78015135744e11 * this.t45 * this.t2516 + 0.687398400e9 * this.t376 * this.t2613 + 0.206219520e9
                * this.t2996 * this.t2622
                + 0.352235520e9 * this.t393 * this.t2516;
        this.t3015 = this.t126 * this.t31;
        this.t3040 =
            0.515548800e9 * this.t1353 * this.t2633 + 0.19155502080e11 * this.t180 * this.t2519 - 0.9577751040e10
                * this.t139 * this.t2519
                + 0.1467298560e10 * this.t171 * this.t2513 + 0.32506306560e11 * this.t338 * this.t2944 - 0.586919424e9
                * this.t2317 * this.t2513
                + 0.37739520e8 * this.t3015 * this.t2507 - 0.6320670720e10 * this.t355 * this.t2572 + 0.567103680e9
                * this.t165 * this.t33 * this.t2733
                + 0.2364876800e10 * this.t138 * this.t23 * this.t2749 + 0.709463040e9 * this.t138 * this.t7
                * this.t2651 - 0.88058880e8 * this.t183
                * this.t2643 + 0.378069120e9 * this.t165 * this.t7 * this.t2643 - 0.7900838400e10 * this.t22
                * this.t2626 + 0.1099837440e10 * this.t466
                * this.t2670 - 0.105670656e9 * this.t674 * this.t33;
        this.t3047 = this.t405 * this.t33;
        this.t3050 = this.t49 * this.t31;
        this.t3083 =
            0.138659713920e12 * this.t138 * this.t2581 - 0.21498880e8 * this.t680 * this.t452 - 0.183306240e9
                * this.t658 * this.t141
                + 0.2200947840e10 * this.t158 * this.t3047 - 0.10778406912e11 * this.t3050 * this.t2507
                - 0.43341742080e11 * this.t1226 * this.t2944
                - 0.739694592e9 * this.t119 * this.t2536 - 0.5502369600e10 * this.t146 * this.t2756 + 0.378069120e9
                * this.t165 * this.t23 * this.t2562
                + 0.396264960e9 * this.t214 * this.t7 * this.t2553 + 0.739694592e9 * this.t183 * this.t7 * this.t2553
                + 0.34594560e8 * this.t242 * this.t7
                * this.t2553 + 0.1649756160e10 * this.t187 * this.t2643 - 0.2268414720e10 * this.t271 * this.t2553
                + 0.1264134144e10 * this.t29
                * this.t115 * this.t140 * this.t69 * this.t71 + 0.3482818560e10 * this.t7 * this.t17 * this.t2651;
        this.t3124 =
            -0.4193280e7 * this.t664 * ix + 0.180590592e9 * this.t29 * this.t7 * this.t2714 - 0.880379136e9 * this.t151
                * this.t557 * this.t141
                - 0.251596800e9 * this.t692 * this.t2493 + 0.632067072e9 * this.t29 * this.t452 * this.t114 * this.t24
                * this.t8 - 0.171849600e9 * this.t367
                * this.t2519 + 0.5389203456e10 * this.t352 * this.t2493 - 0.264176640e9 * this.t364 * this.t2493
                + 0.4949268480e10 * this.t179 * this.t33
                * this.t2733 + 0.8707046400e10 * this.t355 * this.t2507 - 0.40632883200e11 * this.t344 * this.t2845
                - 0.28304640e8 * this.t341
                * this.t2493 + 0.8707046400e10 * this.t359 * this.t2519 + 0.917280e6 * this.t122 * this.t7 * this.t2553
                + 0.198132480e9 * this.t214
                * this.t2545;
        this.t3158 =
            -0.171849600e9 * this.t335 * this.t2507 - 0.343699200e9 * this.t332 * this.t2516 - 0.343699200e9
                * this.t329 * this.t2845
                + 0.1773657600e10 * this.t138 * this.t141 * this.t2699 - 0.28304640e8 * this.t323 * this.t2944
                + 0.138659713920e12 * this.t651
                * this.t2493 - 0.110927771136e12 * this.t274 * this.t2493 + 0.5389203456e10 * this.t253 * this.t2944
                + 0.3160335360e10 * this.t315
                * this.t2613 + 0.7795023600e10 * this.t629 * this.t2493 - 0.752460800e9 * this.t581 * this.t2619
                - 0.916531200e9 * this.t587 * this.t2507
                - 0.916531200e9 * this.t584 * this.t2519 - 0.8255520e7 * this.t2198 * this.t18 + 0.2305754880e10
                * this.t165 * this.t2601
                + 0.17607582720e11 * this.t151 * this.t3047;
        this.t3191 =
            -0.2708858880e10 * this.t104 * ix - 0.6236018880e10 * this.t2292 * this.t2493 + 0.3301421760e10
                * this.t2299 * this.t2519
                - 0.5502369600e10 * this.t266 * this.t2519 - 0.40632883200e11 * this.t474 * this.t2516
                - 0.20316441600e11 * this.t437 * this.t2519
                + 0.614362320e9 * this.t649 * this.t2493 - 0.1152877440e10 * this.t2237 * this.t2519 + 0.709463040e9
                * this.t138 * this.t115 * this.t2590
                + 0.53635405824e11 * this.t171 * this.t2493 - 0.71513874432e11 * this.t690 * this.t2493
                + 0.1896201216e10 * this.t382 * this.t2633
                - 0.409574880e9 * this.t2348 * this.t2493 + 0.3160335360e10 * this.t231 * this.t2619 - 0.29255675904e11
                * this.t387 * this.t2944;
        this.t3228 =
            -0.451476480e9 * this.t563 * this.t2633 + 0.51197432832e11 * this.t200 * this.t2944 + 0.422682624e9
                * this.t463 * this.t2516
                - 0.2474634240e10 * this.t183 * this.t2496 - 0.20316441600e11 * this.t205 * this.t2507
                + 0.17414092800e11 * this.t68 * this.t2516
                + 0.1796401152e10 * this.t53 * this.t2643 + 0.180590592e9 * this.t29 * this.t100 * this.t450 * ey * iy
                - 0.4063288320e10 * this.t113
                * this.t2651 - 0.1833062400e10 * this.t569 * this.t2845 + 0.17414092800e11 * this.t22 * this.t2845
                + 0.51197432832e11 * this.t77
                * this.t2493 + 0.1896201216e10 * this.t32 * this.t2670 + 0.632067072e9 * this.t82 * this.t2513
                + 0.632067072e9 * this.t617 * this.t2622
                + 0.580469760e9 * this.t17 * this.t452 * this.t114;
    }

    /**
     * Partial derivative due to 11th order Earth potential zonal harmonics.
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
            derParUdeg11_6(final double a, final double ex, final double ey, final double ix, final double iy) {
        this.t3239 = this.t42 * this.t63;
        this.t3262 =
            -0.19262880e8 * this.t2364 * this.t18 + 0.2305754880e10 * this.t271 * this.t2519 - 0.29255675904e11
                * this.t379 * this.t2493
                - 0.264176640e9 * this.t376 * this.t2944 + 0.2708858880e10 * this.t3239 * ix - 0.472975360e9
                * this.t187 * this.t2513
                - 0.396264960e9 * this.t644 * this.t2493 - 0.66028435200e11 * this.t195 * this.t2493 + 0.82535544000e11
                * this.t642 * this.t2493
                + 0.534643200e9 * this.t44 * ix + 0.19262880e8 * this.t126 * ix - 0.9577751040e10 * this.t171
                * this.t2643
                - 0.8083805184e10 * this.t187 * this.t2493 + 0.12125707776e11 * this.t603 * this.t2493 + 0.17297280e8
                * this.t243 * this.t2553;
        this.t3277 = this.t17 * this.t646;
        this.t3299 =
            0.960731200e9 * this.t222 * this.t2553 + 0.17878468608e11 * this.t218 * this.t2553 - 0.170658109440e12
                * this.t306 * this.t2516
                + 0.102394865664e12 * this.t471 * this.t2516 - 0.3160335360e10 * this.t359 * this.t2610 - 0.283551840e9
                * this.t215 * this.t2519
                - 0.8126576640e10 * this.t64 * this.t2553 - 0.4740503040e10 * this.t3277 * ix + 0.1264134144e10
                * this.t29 * this.t23 * this.t31
                * this.t35 * this.t38 - 0.3160335360e10 * this.t1275 * this.t2630 + 0.198132480e9 * this.t215
                * this.t2553 + 0.7698862080e10 * this.t1367
                * this.t2944 - 0.5132574720e10 * this.t174 * this.t2944 - 0.6320670720e10 * this.t68 * this.t2500
                - 0.34828185600e11 * this.t1298
                * this.t2633 + 0.81265766400e11 * this.t1219 * this.t2944;
        this.t3303 = this.t44 * this.t31;
        this.t3310 = this.t9 * this.t43;
        this.t3313 = this.t16 * this.t75;
        this.t3340 =
            -0.65012613120e11 * this.t606 * this.t2944 + 0.78015135744e11 * this.t3303 * this.t2507 - 0.39007567872e11
                * this.t68 * this.t2507
                + 0.369847296e9 * this.t184 * this.t2553 + 0.41932800e8 * this.t3310 * ix - 0.534643200e9 * this.t3313
                * ix + 0.5503680e7
                * this.t58 * this.t33 * this.t20 + 0.125798400e9 * this.t16 * this.t33 * this.t20 + 0.898200576e9
                * this.t42 * this.t141 * this.t21 - 0.41932800e8
                * this.t49 * ix - 0.630630e6 * this.t59 * ix * this.t18 + 0.630630e6 * this.t60 * ix + 0.4740503040e10
                * this.t64 * ix
                - 0.255987164160e12 * this.t1282 * this.t2845 + 0.153592298496e12 * this.t437 * this.t2845
                - 0.46437580800e11 * this.t253
                * this.t2613;
        this.t3345 = this.t94 * this.t140;
        this.t3350 = this.t104 * this.t31;
        this.t3375 =
            -0.10778406912e11 * this.t390 * this.t2516 - 0.16253153280e11 * this.t315 * this.t2622 + 0.40632883200e11
                * this.t3345 * this.t2622
                + 0.102394865664e12 * this.t474 * this.t2507 - 0.170658109440e12 * this.t3350 * this.t2507
                + 0.2200947840e10 * this.t195 * this.t2643
                - 0.54177177600e11 * this.t32 * this.t2613 - 0.16253153280e11 * this.t513 * this.t2670
                + 0.40632883200e11 * this.t338 * this.t2670
                - 0.902952960e9 * this.t347 * this.t2868 - 0.503193600e9 * this.t1147 * this.t2944 + 0.2882193600e10
                * this.t2834 * this.t2493
                - 0.3842924800e10 * this.t640 * this.t2493 + 0.135442944000e12 * this.t387 * this.t2613
                - 0.40632883200e11 * this.t82 * this.t2619
                + 0.101582208000e12 * this.t379 * this.t2619;
        this.t3391 = this.t107 * this.t1519;
        this.t3394 = this.t167 * ey;
        this.t3407 = this.t646 * iy;
        this.t3411 = this.t421 * this.t71;
        this.t3414 = this.t36 * this.t85;
        this.t3417 = this.t453 * this.t8;
        this.t3420 = this.t43 * iy;
        this.t3423 = this.t201 * this.t8;
        this.t3426 = this.t78 * this.t71;
        this.t3429 =
            -0.6917264640e10 * this.t2237 * this.t3391 - 0.9577751040e10 * this.t171 * this.t3394 - 0.4193280e7
                * this.t463 * this.t14
                - 0.21498880e8 * this.t451 * this.t453 - 0.105670656e9 * this.t457 * this.t201 + 0.94517280e8
                * this.t165 * this.t1584
                - 0.1283143680e10 * this.t44 * this.t1438 - 0.103994785440e12 * this.t146 * this.t3407 + 0.4193280e7
                * this.t1519
                - 0.41503587840e11 * this.t165 * this.t3411 + 0.1203937280e10 * this.t598 * this.t3414 + 0.171991040e9
                * this.t1375 * this.t3417
                + 0.440294400e9 * this.t119 * this.t3420 + 0.422682624e9 * this.t1216 * this.t3423 + 0.634023936e9
                * this.t664 * this.t3426;
        this.t3430 = this.t63 * iy;
        this.t3433 = this.t479 * iy;
        this.t3436 = this.t429 * this.t71;
        this.t3442 = this.t133 * ey;
        this.t3449 = this.t116 * iy;
        this.t3454 = this.t175 * this.t37;
        this.t3458 = iy * this.t21 * this.t23;
        this.t3461 = this.t409 * this.t38;
        this.t3464 = this.t479 * this.t71;
        this.t3469 = this.t433 * this.t38;
        this.t3476 =
            0.103994785440e12 * this.t158 * this.t3430 + 0.369847296e9 * this.t183 * this.t3433 + 0.2882193600e10
                * this.t221 * this.t3436
                - 0.3842924800e10 * this.t131 * this.t3411 + 0.11007360e8 * this.t58 * this.t8 * this.t3442
                + 0.180590592e9 * this.t29 * this.t90 * this.t133
                * this.t514 - 0.2708858880e10 * this.t30 * this.t3449 + 0.6772147200e10 * this.t94 * this.t3449
                + 0.422682624e9 * this.t463 * this.t3454
                + 0.824878080e9 * this.t179 * this.t3458 - 0.2474634240e10 * this.t183 * this.t3461 - 0.4536829440e10
                * this.t165 * this.t3464
                + 0.6805244160e10 * this.t214 * this.t3436 - 0.44018956800e11 * this.t138 * this.t3469
                + 0.31127690880e11 * this.t146 * this.t3436
                + 0.369847296e9 * this.t183 * this.t1519;
        this.t3479 = this.t8 * ex * this.t14;
        this.t3481 = this.t522 * this.t516;
        this.t3486 = this.t421 * iy;
        this.t3489 = this.t36 * this.t37;
        this.t3492 = this.t75 * iy;
        this.t3495 = this.t78 * iy;
        this.t3504 = this.t498 * this.t38;
        this.t3509 = this.t25 * this.t71;
        this.t3512 = this.t316 * this.t37;
        this.t3517 = this.t409 * this.t71;
        this.t3520 =
            0.8386560e7 * this.t3479 - 0.472975360e9 * this.t179 * this.t3481 + 0.5774146560e10 * this.t179
                * this.t3420 + 0.10375896960e11
                * this.t146 * this.t3486 - 0.752460800e9 * this.t447 * this.t3489 - 0.5774146560e10 * this.t183
                * this.t3492 - 0.317011968e9 * this.t460
                * this.t3495 + 0.21498880e8 * this.t2937 * this.t100 * iy + 0.183306240e9 * this.t2959 * this.t115 * iy
                + 0.19155502080e11
                * this.t179 * this.t3504 + 0.46770141600e11 * this.t302 * this.t3430 - 0.34828185600e11 * this.t1298
                * this.t3509 + 0.1203937280e10
                * this.t587 * this.t3512 - 0.46770141600e11 * this.t221 * this.t3407 + 0.189034560e9 * this.t165
                * this.t3517;
        this.t3525 = this.t516 * this.t83;
        this.t3530 = this.t529 * this.t71;
        this.t3555 =
            0.198132480e9 * this.t214 * this.t1519 - 0.66028435200e11 * this.t158 * this.t3411 + 0.118243840e9
                * this.t138 * this.t3525
                + 0.17297280e8 * this.t242 * this.t1519 + 0.82535544000e11 * this.t146 * this.t3530 + 0.383976450e9
                * this.t637 * this.t3420
                - 0.6320670720e10 * this.t68 * this.t3414 - 0.383976450e9 * this.t626 * this.t3492 - 0.9434880e7
                * this.t214 * this.t3394
                + 0.5417717760e10 * this.t104 * this.t3442 - 0.2041553800e10 * this.t653 * this.t3492 - 0.34594560e8
                * this.t123 * this.t3464
                - 0.739694592e9 * this.t119 * this.t3464 + 0.2041553800e10 * this.t637 * this.t3430 + 0.94517280e8
                * this.t165 * this.t3458
                - 0.9577751040e10 * this.t138 * this.t3461;
        this.t3558 = this.t498 * this.t71;
        this.t3579 = this.t529 * iy;
        this.t3585 = this.t429 * iy;
        this.t3594 =
            -0.9577751040e10 * this.t138 * this.t3558 + 0.105670656e9 * this.t2956 * this.t23 * iy + 0.1796401152e10
                * this.t53 * this.t3394
                + 0.22573824e8 * this.t29 * iy * this.t101 + 0.16253153280e11 * this.t94 * this.t1519
                - 0.21670871040e11 * this.t694 * this.t1519
                - 0.2321879040e10 * this.t53 * this.t3525 + 0.3702636210e10 * this.t661 * this.t3430 + 0.198132480e9
                * this.t214 * this.t3433
                - 0.16507108800e11 * this.t158 * this.t3579 + 0.354731520e9 * this.t138 * this.t522 * this.t38
                - 0.4041902592e10 * this.t179 * this.t3585
                - 0.3702636210e10 * this.t653 * this.t3407 + 0.58105022976e11 * this.t165 * this.t3430
                + 0.17065810944e11 * this.t76 * this.t3394;
        this.t3595 = this.t142 * ey;
        this.t3616 = this.t25 * iy;
        this.t3628 = this.t316 * this.t8;
        this.t3631 =
            -0.4063288320e10 * this.t113 * this.t3595 - 0.9751891968e10 * this.t19 * this.t1584 - 0.204787440e9
                * this.t131 * this.t3585
                - 0.1152877440e10 * this.t146 * this.t3558 - 0.4193280e7 * this.t671 * iy + 0.19503783936e11 * this.t44
                * this.t1584
                - 0.29797447680e11 * this.t138 * this.t3492 + 0.29797447680e11 * this.t179 * this.t3430
                - 0.58105022976e11 * this.t146 * this.t3492
                - 0.251596800e9 * this.t692 * this.t1519 - 0.916531200e9 * this.t569 * this.t3616 + 0.251596800e9
                * this.t16 * this.t8 * this.t3442
                - 0.110927771136e12 * this.t151 * this.t3411 + 0.138659713920e12 * this.t138 * this.t3530
                + 0.17297280e8 * this.t242 * this.t3433
                - 0.451476480e9 * this.t495 * this.t3628;
        this.t3651 = this.t57 * iy;
        this.t3654 = this.t58 * iy;
        this.t3657 = this.t90 * iy;
        this.t3665 = this.t122 * iy;
        this.t3672 =
            -0.2321879040e10 * this.t53 * this.t3449 - 0.8126576640e10 * this.t64 * this.t3442 - 0.458640e6 * this.t123
                * this.t1438
                - 0.32506306560e11 * this.t76 * this.t1519 + 0.40632883200e11 * this.t696 * this.t1519
                - 0.2694601728e10 * this.t49 * this.t1584
                + 0.580469760e9 * this.t17 * iy * this.t116 + 0.125798400e9 * this.t16 * iy * this.t107 + 0.11007360e8
                * this.t3651 * this.t107
                + 0.5503680e7 * this.t3654 * this.t107 - 0.112869120e9 * this.t19 * this.t89 * this.t3657
                - 0.42664527360e11 * this.t104 * this.t1584
                + 0.34369920e8 * this.t183 * this.t3525 + 0.458640e6 * this.t3665 * this.t107 - 0.2708858880e10
                * this.t30 * this.t3525
                + 0.6772147200e10 * this.t94 * this.t3525;
        this.t3677 = this.t83 * this.t38;
        this.t3709 =
            -0.5503680e7 * this.t126 * this.t1438 - 0.11007360e8 * this.t119 * this.t1438 - 0.34369920e8 * this.t179
                * this.t3677 + 0.824878080e9
                * this.t179 * this.t1584 - 0.4063288320e10 * this.t113 * this.t3677 + 0.17065810944e11 * this.t76
                * this.t1491 + 0.90295296e8 * this.t30
                * this.t89 * this.t516 - 0.2566287360e10 * this.t53 * this.t1519 + 0.3849431040e10 * this.t698
                * this.t1519 + 0.898200576e9 * this.t42
                * iy * this.t54 + 0.9434880e7 * this.t126 * this.t1584 + 0.88058880e8 * this.t119 * this.t1584
                - 0.1283143680e10 * this.t44 * this.t3442
                - 0.880379136e9 * this.t151 * this.t575 * this.t38 - 0.27731942784e11 * this.t151 * this.t3579
                - 0.11115232128e11 * this.t214 * this.t3492;
        this.t3743 =
            0.52822748160e11 * this.t151 * this.t3430 - 0.2694601728e10 * this.t49 * this.t3458 + 0.88058880e8
                * this.t119 * this.t3458
                - 0.52822748160e11 * this.t138 * this.t3407 - 0.183306240e9 * this.t587 * this.t206 + 0.17878468608e11
                * this.t138 * this.t3486
                + 0.25598716416e11 * this.t113 * this.t1584 + 0.9434880e7 * this.t126 * this.t3458 - 0.4914898560e10
                * this.t242 * this.t3492
                + 0.25598716416e11 * this.t113 * this.t3458 - 0.42664527360e11 * this.t104 * this.t3458 + 0.125798400e9
                * this.t49 * this.t3442
                - 0.458640e6 * this.t123 * this.t3442 - 0.9751891968e10 * this.t19 * this.t3458 + 0.19503783936e11
                * this.t44 * this.t3458;
        this.t3753 = iy * this.t20 * this.t7;
        this.t3760 = this.t433 * this.t71;
        this.t3765 = iy * this.t18;
        this.t3780 =
            0.3301421760e10 * this.t158 * this.t3504 - 0.5502369600e10 * this.t146 * this.t3469 - 0.6236018880e10
                * this.t302 * this.t3411
                + 0.7795023600e10 * this.t221 * this.t3530 + 0.17297280e8 * this.t242 * this.t3753 + 0.11115232128e11
                * this.t165 * this.t3420
                - 0.2566287360e10 * this.t53 * this.t3753 + 0.2200947840e10 * this.t158 * this.t3760 - 0.1559004720e10
                * this.t302 * this.t3579
                - 0.832156416e9 * this.t214 * this.t3765 + 0.118243840e9 * this.t138 * this.t3449 - 0.409574880e9
                * this.t131 * this.t3464
                + 0.614362320e9 * this.t242 * this.t3436 - 0.1152877440e10 * this.t146 * this.t3461 + 0.2305754880e10
                * this.t165 * this.t3504
                + 0.198132480e9 * this.t214 * this.t3753;
        this.t3792 = this.t1670 * ey;
        this.t3795 = this.t348 * this.t85;
        this.t3814 =
            0.832156416e9 * this.t126 * this.t3420 - 0.396264960e9 * this.t126 * this.t3464 - 0.9751891968e10
                * this.t94 * this.t3394
                - 0.11007360e8 * this.t119 * this.t3442 + 0.4914898560e10 * this.t131 * this.t3420 + 0.90295296e8
                * this.t30 * this.t3792
                + 0.1099837440e10 * this.t466 * this.t3795 + 0.34369920e8 * this.t183 * this.t3449 + 0.28108080e8
                * this.t60 * this.t3420
                - 0.363242880e9 * this.t242 * this.t3765 + 0.363242880e9 * this.t123 * this.t3420 - 0.25939742400e11
                * this.t221 * this.t3492
                + 0.25939742400e11 * this.t131 * this.t3430 - 0.440294400e9 * this.t183 * this.t3765 - 0.28108080e8
                * this.t626 * this.t3765;
        this.t3815 = this.t515 * this.t90;
        this.t3827 = this.t360 * this.t38;
        this.t3830 = this.t206 * this.t8;
        this.t3833 = this.t232 * this.t38;
        this.t3836 = this.t383 * this.t71;
        this.t3848 = this.t232 * this.t71;
        this.t3851 = this.t383 * iy;
        this.t3854 = this.t348 * this.t37;
        this.t3859 =
            0.171991040e9 * this.t592 * this.t3815 - 0.586919424e9 * this.t151 * this.t3481 + 0.1467298560e10
                * this.t138 * this.t575 * this.t516
                + 0.53635405824e11 * this.t138 * this.t3436 - 0.71513874432e11 * this.t179 * this.t3411
                + 0.2749593600e10 * this.t460 * this.t3827
                + 0.1099837440e10 * this.t1233 * this.t3830 + 0.1504921600e10 * this.t569 * this.t3833 + 0.601968640e9
                * this.t658 * this.t3836
                + 0.2749593600e10 * this.t674 * this.t3509 + 0.632067072e9 * this.t29 * this.t516 * this.t107
                * this.t83 + 0.180590592e9 * this.t29 * this.t8
                * this.t3792 - 0.752460800e9 * this.t581 * this.t3848 - 0.150492160e9 * this.t563 * this.t3851
                - 0.916531200e9 * this.t592 * this.t3854
                - 0.9751891968e10 * this.t94 * this.t1491;
        this.t3862 = this.t84 * this.t38;
        this.t3865 = this.t70 * this.t8;
        this.t3868 = this.t360 * this.t71;
        this.t3876 = this.t107 * this.t24;
        this.t3883 = this.t107 * this.t34;
        this.t3889 = this.t133 * this.t35;
        this.t3905 =
            -0.451476480e9 * this.t595 * this.t3862 - 0.1833062400e10 * this.t598 * this.t3865 - 0.1833062400e10
                * this.t584 * this.t3868
                + 0.632067072e9 * this.t29 * this.t71 * this.t116 * this.t24 + 0.5389203456e10 * this.t42 * this.t71
                * this.t3876 + 0.3482818560e10 * this.t17
                * this.t8 * this.t3595 + 0.8707046400e10 * this.t17 * this.t38 * this.t3883 + 0.5417717760e10
                * this.t104 * this.t1438 + 0.3482818560e10
                * this.t17 * this.t85 * this.t3889 + 0.917280e6 * this.t122 * this.t8 * this.t3442 + 0.1741409280e10
                * this.t19 * this.t3677 - 0.9434880e7
                * this.t214 * this.t1491 - 0.88058880e8 * this.t183 * this.t1491 - 0.8126576640e10 * this.t64
                * this.t1438 + 0.1796401152e10 * this.t53
                * this.t1491;
    }

    /**
     * Partial derivative due to 11th order Earth potential zonal harmonics.
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
            derParUdeg11_7(final double a, final double ex, final double ey, final double ix, final double iy) {
        this.t3908 = this.t84 * this.t516;
        this.t3911 = this.t70 * this.t37;
        this.t3926 = this.t133 * this.t69;
        this.t3930 = this.t167 * this.t69;
        this.t3934 = this.t54 * this.t24;
        this.t3948 = this.t133 * this.t1428;
        this.t3951 = this.t133 * this.t1529;
        this.t3954 = this.t167 * this.t1417;
        this.t3957 =
            0.369847296e9 * this.t183 * this.t3753 + 0.601968640e9 * this.t584 * this.t3908 + 0.3666124800e10
                * this.t457 * this.t3911
                + 0.1264134144e10 * this.t29 * this.t37 * this.t142 * this.t69 + 0.3592802304e10 * this.t42 * this.t8
                * this.t3394 + 0.1580167680e10 * this.t29
                * this.t38 * this.t54 * this.t34 + 0.3592802304e10 * this.t42 * this.t37 * this.t3926
                + 0.11609395200e11 * this.t17 * this.t37 * this.t3930
                + 0.8707046400e10 * this.t17 * this.t71 * this.t3934 + 0.1264134144e10 * this.t29 * this.t85
                * this.t167 * this.t35 - 0.112869120e9 * this.t19
                * this.t101 * iy - 0.32506306560e11 * this.t76 * this.t3753 + 0.40632883200e11 * this.t696 * this.t3753
                + 0.76622008320e11
                * this.t180 * this.t3948 + 0.1773657600e10 * this.t139 * this.t3951 + 0.9223019520e10 * this.t271
                * this.t3954;
        this.t3983 = this.t133 * this.t1564;
        this.t3986 = this.t167 * this.t1428;
        this.t3991 = this.t107 * this.t1438;
        this.t3994 =
            -0.38311004160e11 * this.t139 * this.t3948 + 0.105645496320e12 * this.t152 * this.t3948 - 0.176075827200e12
                * this.t218 * this.t3948
                - 0.38311004160e11 * this.t139 * this.t3954 + 0.76622008320e11 * this.t180 * this.t3954
                - 0.4611509760e10 * this.t2237 * this.t3954
                - 0.264113740800e12 * this.t218 * this.t3391 - 0.4611509760e10 * this.t2237 * this.t3948
                + 0.9223019520e10 * this.t271 * this.t3948
                + 0.105645496320e12 * this.t152 * this.t3954 - 0.176075827200e12 * this.t218 * this.t3954
                + 0.158468244480e12 * this.t152
                * this.t3391 + 0.8803791360e10 * this.t171 * this.t3983 - 0.9459507200e10 * this.t187 * this.t3986
                - 0.1134207360e10 * this.t215
                * this.t3948 + 0.52822748160e11 * this.t274 * this.t3991;
        this.t3995 = this.t133 * this.t1442;
        this.t4000 = this.t167 * this.t1442;
        this.t4005 = this.t54 * this.t1438;
        this.t4014 = this.t142 * this.t1417;
        this.t4019 = this.t54 * this.t1519;
        this.t4032 =
            -0.3458632320e10 * this.t147 * this.t3995 - 0.4401895680e10 * this.t152 * this.t3951 + 0.3547315200e10
                * this.t139 * this.t4000
                - 0.3521516544e10 * this.t2317 * this.t3983 + 0.1773657600e10 * this.t139 * this.t4005 + 0.567103680e9
                * this.t166 * this.t3995
                + 0.52822748160e11 * this.t274 * this.t3995 - 0.69189120e8 * this.t631 * this.t3479 - 0.3521516544e10
                * this.t2317 * this.t4014
                + 0.8803791360e10 * this.t171 * this.t4014 - 0.7094630400e10 * this.t187 * this.t4019
                - 0.14847805440e11 * this.t184 * this.t3391
                - 0.1479389184e10 * this.t686 * this.t3479 - 0.22009478400e11 * this.t266 * this.t3948
                - 0.1701311040e10 * this.t215 * this.t3391
                - 0.8803791360e10 * this.t2317 * this.t4019;
        this.t4061 = this.t107 * this.t1491;
        this.t4068 =
            0.22009478400e11 * this.t171 * this.t4019 + 0.13205687040e11 * this.t2299 * this.t3948 + 0.1228724640e10
                * this.t649 * this.t3479
                - 0.819149760e9 * this.t2348 * this.t3479 + 0.6602843520e10 * this.t195 * this.t3995 - 0.2837852160e10
                * this.t187 * this.t4014
                + 0.567103680e9 * this.t166 * this.t3991 - 0.4401895680e10 * this.t152 * this.t4005 + 0.4949268480e10
                * this.t187 * this.t3991
                - 0.28733253120e11 * this.t171 * this.t3991 - 0.28733253120e11 * this.t171 * this.t3995
                + 0.4949268480e10 * this.t187 * this.t3995
                + 0.3547315200e10 * this.t139 * this.t4061 - 0.8803791360e10 * this.t152 * this.t4000 + 0.6602843520e10
                * this.t195 * this.t3991;
        this.t4103 =
            -0.143027748864e12 * this.t690 * this.t3479 - 0.11738388480e11 * this.t2317 * this.t3986 - 0.630630e6
                * this.t59 * iy * this.t18
                + 0.29345971200e11 * this.t171 * this.t3986 + 0.458640e6 * this.t122 * this.t71 * this.t24
                - 0.792529920e9 * this.t644 * this.t3479
                + 0.107270811648e12 * this.t171 * this.t3479 - 0.7685849600e10 * this.t640 * this.t3479
                - 0.16167610368e11 * this.t187 * this.t3479
                + 0.24251415552e11 * this.t603 * this.t3479 - 0.12472037760e11 * this.t2292 * this.t3479
                + 0.15590047200e11 * this.t629 * this.t3479
                - 0.2837852160e10 * this.t187 * this.t3983 + 0.5764387200e10 * this.t2834 * this.t3479
                + 0.13205687040e11 * this.t2299 * this.t3954
                - 0.22009478400e11 * this.t266 * this.t3954;
        this.t4133 = this.t107 * this.t1584;
        this.t4136 =
            -0.57466506240e11 * this.t139 * this.t3391 + 0.114933012480e12 * this.t180 * this.t3391 - 0.221855542272e12
                * this.t274 * this.t3479
                + 0.277319427840e12 * this.t651 * this.t3479 - 0.9898536960e10 * this.t184 * this.t3948
                + 0.13610488320e11 * this.t633 * this.t3479
                - 0.9073658880e10 * this.t166 * this.t3479 - 0.1134207360e10 * this.t215 * this.t3954
                + 0.19808530560e11 * this.t2299 * this.t3391
                - 0.33014217600e11 * this.t266 * this.t3391 + 0.62255381760e11 * this.t147 * this.t3479
                - 0.83007175680e11 * this.t624 * this.t3479
                - 0.132056870400e12 * this.t195 * this.t3479 + 0.165071088000e12 * this.t642 * this.t3479
                - 0.8803791360e10 * this.t2317 * this.t4133;
        this.t4170 =
            0.22009478400e11 * this.t171 * this.t4133 - 0.9898536960e10 * this.t184 * this.t3954 - 0.7094630400e10
                * this.t187 * this.t4133
                + 0.13834529280e11 * this.t271 * this.t3391 - 0.3458632320e10 * this.t147 * this.t3991
                - 0.8803791360e10 * this.t152 * this.t4061
                + 0.17878468608e11 * this.t218 * this.t3442 - 0.34369920e8 * this.t179 * this.t3595 + 0.1649756160e10
                * this.t179 * this.t3517
                + 0.5389203456e10 * this.t352 * this.t3495 + 0.125798400e9 * this.t49 * this.t1438 - 0.2268414720e10
                * this.t165 * this.t3585
                + 0.198132480e9 * this.t215 * this.t3442 + 0.22014720e8 * this.t57 * this.t8 * this.t3442
                + 0.17607582720e11 * this.t151 * this.t3760
                + 0.960731200e9 * this.t221 * this.t3486;
        this.t4188 = this.t175 * this.t8;
        this.t4199 = this.t515 * this.t85;
        this.t4206 =
            0.1048320e7 * this.t9 * iy + 0.11007360e8 * this.t57 * this.t71 * this.t24 - 0.251596800e9 * this.t692
                * this.t3753
                - 0.21670871040e11 * this.t694 * this.t3753 + 0.16253153280e11 * this.t94 * this.t3753
                + 0.1741409280e10 * this.t19 * this.t3595
                - 0.5503680e7 * this.t126 * this.t3442 - 0.317011968e9 * this.t466 * this.t4188 - 0.8083805184e10
                * this.t179 * this.t3464
                - 0.88058880e8 * this.t183 * this.t3394 + 0.3849431040e10 * this.t698 * this.t3753 + 0.12125707776e11
                * this.t183 * this.t3436
                - 0.150492160e9 * this.t572 * this.t4199 - 0.40632883200e11 * this.t474 * this.t3865 - 0.20316441600e11
                * this.t471 * this.t3854;
        this.t4242 =
            0.632067072e9 * this.t513 * this.t4199 + 0.1649756160e10 * this.t187 * this.t3394 - 0.40632883200e11
                * this.t437 * this.t3868
                + 0.34594560e8 * this.t242 * this.t8 * this.t3442 + 0.138659713920e12 * this.t651 * this.t3753
                - 0.110927771136e12 * this.t274
                * this.t3753 + 0.709463040e9 * this.t138 * this.t8 * this.t3595 + 0.352235520e9 * this.t2883
                * this.t3423 + 0.117022703616e12 * this.t1285
                * this.t3426 - 0.58511351808e11 * this.t359 * this.t3426 + 0.37739520e8 * this.t508 * this.t3454
                + 0.206219520e9 * this.t320 * this.t3795
                + 0.515548800e9 * this.t364 * this.t3827 - 0.16167610368e11 * this.t1223 * this.t3426 + 0.528353280e9
                * this.t1364 * this.t3426
                + 0.3299512320e10 * this.t179 * this.t8 * this.t3394;
        this.t4283 =
            0.3299512320e10 * this.t179 * this.t37 * this.t3926 + 0.2364876800e10 * this.t138 * this.t37 * this.t3930
                - 0.13931274240e11 * this.t174
                * this.t3795 - 0.34828185600e11 * this.t352 * this.t3827 - 0.13931274240e11 * this.t2971 * this.t3830
                + 0.378069120e9 * this.t165
                * this.t8 * this.t3394 + 0.396264960e9 * this.t214 * this.t8 * this.t3442 + 0.567103680e9 * this.t165
                * this.t71 * this.t3876 + 0.4949268480e10
                * this.t179 * this.t71 * this.t3876 + 0.739694592e9 * this.t183 * this.t8 * this.t3442 + 0.189034560e9
                * this.t166 * this.t3394
                + 0.10375896960e11 * this.t266 * this.t3442 - 0.16507108800e11 * this.t159 * this.t3442 - 0.902952960e9
                * this.t2889 * this.t3417
                - 0.170658109440e12 * this.t306 * this.t3454 + 0.102394865664e12 * this.t471 * this.t3454;
        this.t4310 = this.t14 * this.t8;
        this.t4317 =
            0.101582208000e12 * this.t1209 * this.t3509 - 0.40632883200e11 * this.t231 * this.t3509 + 0.78015135744e11
                * this.t3303 * this.t3423
                - 0.39007567872e11 * this.t68 * this.t3423 - 0.3160335360e10 * this.t1275 * this.t3836
                + 0.8707046400e10 * this.t347 * this.t3854
                + 0.17414092800e11 * this.t359 * this.t3868 + 0.56609280e8 * this.t1345 * this.t3426 + 0.78015135744e11
                * this.t45 * this.t3454
                - 0.39007567872e11 * this.t347 * this.t3454 - 0.28304640e8 * this.t341 * this.t3495 - 0.29255675904e11
                * this.t338 * this.t4188
                - 0.20316441600e11 * this.t344 * this.t3616 - 0.503193600e9 * this.t1147 * this.t4310
                + 0.101582208000e12 * this.t379 * this.t3827
                - 0.40632883200e11 * this.t82 * this.t3827;
        this.t4351 =
            0.135442944000e12 * this.t387 * this.t3911 - 0.54177177600e11 * this.t32 * this.t3911 + 0.40632883200e11
                * this.t338 * this.t3795
                - 0.16253153280e11 * this.t513 * this.t3795 - 0.902952960e9 * this.t347 * this.t3815 - 0.27731942784e11
                * this.t162 * this.t3442
                - 0.2268414720e10 * this.t271 * this.t3442 - 0.28304640e8 * this.t373 * this.t4188 + 0.19155502080e11
                * this.t180 * this.t3458
                - 0.9577751040e10 * this.t139 * this.t3458 - 0.34594560e8 * this.t631 * this.t3753 - 0.264176640e9
                * this.t364 * this.t3495
                + 0.10483200e8 * this.t119 * iy - 0.3842924800e10 * this.t640 * this.t3753 + 0.2882193600e10
                * this.t2834 * this.t3753;
        this.t4384 =
            0.2305754880e10 * this.t271 * this.t3458 - 0.1152877440e10 * this.t2237 * this.t3458 - 0.29255675904e11
                * this.t379 * this.t3495
                + 0.632067072e9 * this.t382 * this.t3851 - 0.171849600e9 * this.t370 * this.t3854 - 0.343699200e9
                * this.t367 * this.t3868
                - 0.3160335360e10 * this.t359 * this.t3908 + 0.7698862080e10 * this.t1367 * this.t4310
                - 0.5132574720e10 * this.t174 * this.t4310
                - 0.5502369600e10 * this.t266 * this.t3458 + 0.3301421760e10 * this.t2299 * this.t3458 - 0.739694592e9
                * this.t686 * this.t3753
                + 0.1467298560e10 * this.t171 * this.t3449 - 0.586919424e9 * this.t2317 * this.t3449 - 0.4041902592e10
                * this.t180 * this.t3442
                + 0.960731200e9 * this.t222 * this.t3442;
        this.t4416 =
            0.2200947840e10 * this.t195 * this.t3394 + 0.12125707776e11 * this.t603 * this.t3753 - 0.8083805184e10
                * this.t187 * this.t3753
                + 0.41932800e8 * this.t3310 * iy - 0.264176640e9 * this.t320 * this.t4188 - 0.343699200e9 * this.t332
                * this.t3865
                - 0.171849600e9 * this.t329 * this.t3616 - 0.46437580800e11 * this.t253 * this.t3911 - 0.10778406912e11
                * this.t390 * this.t3454
                + 0.40632883200e11 * this.t3345 * this.t3830 - 0.16253153280e11 * this.t315 * this.t3830
                - 0.170658109440e12 * this.t3350
                * this.t3423 + 0.102394865664e12 * this.t474 * this.t3423 + 0.81265766400e11 * this.t1219 * this.t4310
                - 0.65012613120e11 * this.t606
                * this.t4310;
        this.t4451 =
            -0.255987164160e12 * this.t1282 * this.t3426 + 0.153592298496e12 * this.t437 * this.t3426
                - 0.71513874432e11 * this.t690 * this.t3753
                + 0.53635405824e11 * this.t171 * this.t3753 + 0.82535544000e11 * this.t642 * this.t3753
                - 0.66028435200e11 * this.t195 * this.t3753
                - 0.396264960e9 * this.t644 * this.t3753 + 0.898200576e9 * this.t42 * this.t38 * this.t34 + 0.5503680e7
                * this.t58 * this.t71 * this.t24
                + 0.7795023600e10 * this.t629 * this.t3753 - 0.6236018880e10 * this.t2292 * this.t3753 + 0.614362320e9
                * this.t649 * this.t3753
                - 0.409574880e9 * this.t2348 * this.t3753 - 0.472975360e9 * this.t187 * this.t3449 - 0.7900838400e10
                * this.t22 * this.t3833
                - 0.10778406912e11 * this.t3050 * this.t3423;
        this.t4484 =
            0.354731520e9 * this.t139 * this.t3595 - 0.43341742080e11 * this.t1226 * this.t4310 + 0.32506306560e11
                * this.t338 * this.t4310
                - 0.6320670720e10 * this.t355 * this.t3512 + 0.37739520e8 * this.t3015 * this.t3423 + 0.3160335360e10
                * this.t32 * this.t3489
                + 0.8707046400e10 * this.t22 * this.t3616 + 0.1896201216e10 * this.t82 * this.t3862 + 0.51197432832e11
                * this.t606 * this.t4188
                + 0.352235520e9 * this.t393 * this.t3454 + 0.206219520e9 * this.t2996 * this.t3830 + 0.687398400e9
                * this.t376 * this.t3911
                + 0.515548800e9 * this.t1353 * this.t3509 - 0.8255520e7 * this.t3665 * this.t18 - 0.204787440e9
                * this.t132 * this.t3442;
        this.t4517 =
            -0.880379136e9 * this.t152 * this.t3595 + 0.17297280e8 * this.t243 * this.t3442 - 0.44018956800e11
                * this.t218 * this.t3458
                + 0.183306240e9 * this.t2260 * this.t516 + 0.26411374080e11 * this.t152 * this.t3458 + 0.369847296e9
                * this.t184 * this.t3442
                - 0.41503587840e11 * this.t624 * this.t3753 + 0.31127690880e11 * this.t147 * this.t3753
                + 0.51197432832e11 * this.t77 * this.t3495
                + 0.17414092800e11 * this.t68 * this.t3865 + 0.5389203456e10 * this.t174 * this.t4188 + 0.6805244160e10
                * this.t633 * this.t3753
                - 0.4536829440e10 * this.t166 * this.t3753 - 0.283551840e9 * this.t215 * this.t3458 - 0.2474634240e10
                * this.t184 * this.t3458
                - 0.10483200e8 * this.t3651 * this.t18;
        this.t4553 =
            -0.1152877440e10 * this.t147 * this.t3394 + 0.17607582720e11 * this.t274 * this.t3394 + 0.3160335360e10
                * this.t231 * this.t3848
                + 0.1773657600e10 * this.t138 * this.t71 * this.t3934 + 0.22573824e8 * this.t29 * this.t3657 * this.t89
                - 0.105670656e9 * this.t677 * this.t71
                + 0.1896201216e10 * this.t315 * this.t3628 - 0.1048320e7 * this.t3765 + 0.125798400e9 * this.t71
                * this.t16 * this.t24 + 0.19262880e8
                * this.t126 * iy + 0.4740503040e10 * this.t64 * iy - 0.183306240e9 * this.t683 * this.t38
                - 0.2708858880e10 * this.t104 * iy
                + 0.105670656e9 * this.t2229 * this.t38 - 0.19262880e8 * this.t3654 * this.t18 - 0.4740503040e10
                * this.t3277 * iy;
        this.t4589 =
            -0.534643200e9 * this.t3313 * iy + 0.630630e6 * this.t60 * iy + 0.21498880e8 * this.t2246 * this.t3657
                - 0.1559004720e10
                * this.t303 * this.t3442 - 0.21498880e8 * this.t667 * this.t516 + 0.534643200e9 * this.t44 * iy
                + 0.4193280e7 * this.t3753
                + 0.8255520e7 * this.t123 * iy + 0.2708858880e10 * this.t3239 * iy - 0.41932800e8 * this.t49 * iy
                + 0.580469760e9
                * this.t17 * this.t516 * this.t83 + 0.1773657600e10 * this.t138 * this.t38 * this.t3883 + 0.709463040e9
                * this.t138 * this.t85 * this.t3889
                + 0.378069120e9 * this.t165 * this.t37 * this.t3926 + 0.26411374080e11 * this.t151 * this.t3504
                - 0.283551840e9 * this.t214 * this.t3461;

    }
}
