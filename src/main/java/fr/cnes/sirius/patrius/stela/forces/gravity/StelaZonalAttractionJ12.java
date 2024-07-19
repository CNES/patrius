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
 * Class computing 12th order zonal perturbations. This class has package visibility since it is not supposed to be used
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
class StelaZonalAttractionJ12 extends AbstractStelaLagrangeContribution {

     /** Serializable UID. */
    private static final long serialVersionUID = -7899746357078516203L;

    /** The central body reference radius (m). */
    private final double rEq;
    /** The 12th order central body coefficients */
    private final double j12;

    /** Temporary coefficients. */
    private double t2;
    private double t3;
    private double t4;
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
    private double t20;
    private double t21;
    private double t22;
    private double t23;
    private double t24;
    private double t27;
    private double t28;
    private double t29;
    private double t30;
    private double t31;
    private double t32;
    private double t35;
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
    private double t51;
    private double t54;
    private double t55;
    private double t56;
    private double t59;
    private double t62;
    private double t63;
    private double t64;
    private double t65;
    private double t68;
    private double t69;
    private double t72;
    private double t73;
    private double t74;
    private double t75;
    private double t76;
    private double t79;
    private double t80;
    private double t81;
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
    private double t96;
    private double t97;
    private double t98;
    private double t99;
    private double t100;
    private double t103;
    private double t104;
    private double t107;
    private double t108;
    private double t111;
    private double t112;
    private double t113;
    private double t114;
    private double t117;
    private double t118;
    private double t119;
    private double t120;
    private double t123;
    private double t124;
    private double t127;
    private double t128;
    private double t131;
    private double t132;
    private double t135;
    private double t138;
    private double t139;
    private double t140;
    private double t141;
    private double t144;
    private double t147;
    private double t149;
    private double t150;
    private double t153;
    private double t156;
    private double t157;
    private double t158;
    private double t159;
    private double t162;
    private double t163;
    private double t164;
    private double t167;
    private double t168;
    private double t171;
    private double t174;
    private double t175;
    private double t178;
    private double t179;
    private double t184;
    private double t185;
    private double t186;
    private double t187;
    private double t188;
    private double t189;
    private double t190;
    private double t193;
    private double t194;
    private double t195;
    private double t198;
    private double t199;
    private double t200;
    private double t203;
    private double t204;
    private double t205;
    private double t206;
    private double t209;
    private double t210;
    private double t211;
    private double t214;
    private double t215;
    private double t216;
    private double t219;
    private double t220;
    private double t221;
    private double t224;
    private double t225;
    private double t228;
    private double t229;
    private double t230;
    private double t231;
    private double t238;
    private double t239;
    private double t242;
    private double t243;
    private double t246;
    private double t247;
    private double t252;
    private double t253;
    private double t254;
    private double t255;
    private double t256;
    private double t259;
    private double t262;
    private double t263;
    private double t264;
    private double t267;
    private double t268;
    private double t269;
    private double t272;
    private double t275;
    private double t278;
    private double t281;
    private double t282;
    private double t285;
    private double t288;
    private double t289;
    private double t292;
    private double t293;
    private double t296;
    private double t297;
    private double t304;
    private double t305;
    private double t306;
    private double t313;
    private double t314;
    private double t315;
    private double t318;
    private double t319;
    private double t320;
    private double t323;
    private double t324;
    private double t325;
    private double t326;
    private double t327;
    private double t330;
    private double t331;
    private double t332;
    private double t337;
    private double t338;
    private double t339;
    private double t342;
    private double t343;
    private double t344;
    private double t347;
    private double t348;
    private double t349;
    private double t352;
    private double t353;
    private double t354;
    private double t357;
    private double t359;
    private double t360;
    private double t361;
    private double t364;
    private double t365;
    private double t366;
    private double t369;
    private double t370;
    private double t371;
    private double t374;
    private double t375;
    private double t376;
    private double t379;
    private double t380;
    private double t381;
    private double t384;
    private double t385;
    private double t386;
    private double t389;
    private double t390;
    private double t397;
    private double t398;
    private double t403;
    private double t404;
    private double t407;
    private double t408;
    private double t409;
    private double t416;
    private double t417;
    private double t420;
    private double t421;
    private double t426;
    private double t427;
    private double t430;
    private double t437;
    private double t438;
    private double t445;
    private double t453;
    private double t454;
    private double t475;
    private double t486;
    private double t487;
    private double t502;
    private double t508;
    private double t509;
    private double t510;
    private double t513;
    private double t514;
    private double t529;
    private double t532;
    private double t535;
    private double t536;
    private double t543;
    private double t550;
    private double t553;
    private double t556;
    private double t561;
    private double t564;
    private double t569;
    private double t572;
    private double t573;
    private double t574;
    private double t575;
    private double t578;
    private double t579;
    private double t582;
    private double t587;
    private double t598;
    private double t601;
    private double t604;
    private double t607;
    private double t608;
    private double t611;
    private double t614;
    private double t629;
    private double t630;
    private double t631;
    private double t636;
    private double t639;
    private double t641;
    private double t648;
    private double t667;
    private double t670;
    private double t673;
    private double t676;
    private double t679;
    private double t682;
    private double t685;
    private double t688;
    private double t691;
    private double t694;
    private double t700;
    private double t703;
    private double t706;
    private double t711;
    private double t714;
    private double t717;
    private double t720;
    private double t723;
    private double t726;
    private double t729;
    private double t732;
    private double t735;
    private double t738;
    private double t741;
    private double t744;
    private double t747;
    private double t748;
    private double t751;
    private double t754;
    private double t757;
    private double t760;
    private double t763;
    private double t766;
    private double t769;
    private double t772;
    private double t775;
    private double t778;
    private double t784;
    private double t786;
    private double t789;
    private double t792;
    private double t795;
    private double t798;
    private double t801;
    private double t803;
    private double t804;
    private double t807;
    private double t810;
    private double t813;
    private double t816;
    private double t819;
    private double t822;
    private double t823;
    private double t826;
    private double t829;
    private double t832;
    private double t835;
    private double t838;
    private double t841;
    private double t844;
    private double t847;
    private double t849;
    private double t852;
    private double t855;
    private double t857;
    private double t860;
    private double t863;
    private double t866;
    private double t869;
    private double t872;
    private double t875;
    private double t878;
    private double t881;
    private double t884;
    private double t887;
    private double t890;
    private double t893;
    private double t896;
    private double t899;
    private double t900;
    private double t903;
    private double t906;
    private double t909;
    private double t911;
    private double t913;
    private double t916;
    private double t918;
    private double t920;
    private double t923;
    private double t925;
    private double t928;
    private double t931;
    private double t933;
    private double t936;
    private double t939;
    private double t942;
    private double t945;
    private double t948;
    private double t951;
    private double t954;
    private double t956;
    private double t958;
    private double t962;
    private double t965;
    private double t968;
    private double t970;
    private double t972;
    private double t975;
    private double t978;
    private double t981;
    private double t984;
    private double t987;
    private double t990;
    private double t993;
    private double t996;
    private double t999;
    private double t1002;
    private double t1006;
    private double t1009;
    private double t1012;
    private double t1015;
    private double t1018;
    private double t1023;
    private double t1025;
    private double t1028;
    private double t1031;
    private double t1034;
    private double t1036;
    private double t1038;
    private double t1039;
    private double t1041;
    private double t1044;
    private double t1046;
    private double t1048;
    private double t1050;
    private double t1053;
    private double t1058;
    private double t1061;
    private double t1068;
    private double t1074;
    private double t1079;
    private double t1090;
    private double t1097;
    private double t1098;
    private double t1101;
    private double t1102;
    private double t1107;
    private double t1116;
    private double t1127;
    private double t1130;
    private double t1139;
    private double t1142;
    private double t1153;
    private double t1156;
    private double t1159;
    private double t1162;
    private double t1167;
    private double t1184;
    private double t1189;
    private double t1192;
    private double t1194;
    private double t1211;
    private double t1219;
    private double t1220;
    private double t1222;
    private double t1224;
    private double t1228;
    private double t1240;
    private double t1245;
    private double t1246;
    private double t1247;
    private double t1249;
    private double t1253;
    private double t1254;
    private double t1256;
    private double t1257;
    private double t1259;
    private double t1261;
    private double t1267;
    private double t1270;
    private double t1277;
    private double t1302;
    private double t1317;
    private double t1338;
    private double t1374;
    private double t1388;
    private double t1389;
    private double t1392;
    private double t1393;
    private double t1398;
    private double t1403;
    private double t1416;
    private double t1433;
    private double t1434;
    private double t1439;
    private double t1440;
    private double t1443;
    private double t1444;
    private double t1447;
    private double t1451;
    private double t1456;
    private double t1457;
    private double t1462;
    private double t1463;
    private double t1466;
    private double t1503;
    private double t1523;
    private double t1541;
    private double t1576;
    private double t1614;
    private double t1617;
    private double t1626;
    private double t1641;
    private double t1657;
    private double t1661;
    private double t1664;
    private double t1671;
    private double t1674;
    private double t1677;
    private double t1680;
    private double t1687;
    private double t1688;
    private double t1691;
    private double t1700;
    private double t1703;
    private double t1704;
    private double t1709;
    private double t1724;
    private double t1725;
    private double t1728;
    private double t1731;
    private double t1734;
    private double t1737;
    private double t1746;
    private double t1751;
    private double t1754;
    private double t1755;
    private double t1763;
    private double t1766;
    private double t1777;
    private double t1790;
    private double t1797;
    private double t1800;
    private double t1805;
    private double t1812;
    private double t1834;
    private double t1875;
    private double t1902;
    private double t1913;
    private double t1928;
    private double t1933;
    private double t1954;
    private double t1957;
    private double t1962;
    private double t1979;
    private double t1990;
    private double t1993;
    private double t1996;
    private double t2002;
    private double t2005;
    private double t2022;
    private double t2025;
    private double t2038;
    private double t2071;
    private double t2078;
    private double t2115;
    private double t2130;
    private double t2149;
    private double t2152;
    private double t2162;
    private double t2163;
    private double t2168;
    private double t2169;
    private double t2172;
    private double t2175;
    private double t2178;
    private double t2183;
    private double t2184;
    private double t2187;
    private double t2194;
    private double t2195;
    private double t2198;
    private double t2199;
    private double t2202;
    private double t2203;
    private double t2206;
    private double t2209;
    private double t2212;
    private double t2223;
    private double t2228;
    private double t2229;
    private double t2234;
    private double t2243;
    private double t2246;
    private double t2264;
    private double t2271;
    private double t2285;
    private double t2288;
    private double t2320;
    private double t2324;
    private double t2349;
    private double t2358;
    private double t2393;
    private double t2398;
    private double t2431;
    private double t2442;
    private double t2461;
    private double t2464;
    private double t2469;
    private double t2499;
    private double t2506;
    private double t2515;
    private double t2518;
    private double t2520;
    private double t2546;
    private double t2554;
    private double t2559;
    private double t2562;
    private double t2568;
    private double t2569;
    private double t2572;
    private double t2575;
    private double t2578;
    private double t2581;
    private double t2582;
    private double t2585;
    private double t2586;
    private double t2589;
    private double t2594;
    private double t2595;
    private double t2599;
    private double t2604;
    private double t2608;
    private double t2609;
    private double t2612;
    private double t2614;
    private double t2617;
    private double t2618;
    private double t2624;
    private double t2625;
    private double t2628;
    private double t2633;
    private double t2634;
    private double t2637;
    private double t2638;
    private double t2641;
    private double t2642;
    private double t2645;
    private double t2646;
    private double t2651;
    private double t2654;
    private double t2659;
    private double t2668;
    private double t2675;
    private double t2681;
    private double t2684;
    private double t2691;
    private double t2698;
    private double t2701;
    private double t2704;
    private double t2705;
    private double t2710;
    private double t2719;
    private double t2722;
    private double t2723;
    private double t2728;
    private double t2733;
    private double t2734;
    private double t2737;
    private double t2740;
    private double t2741;
    private double t2744;
    private double t2745;
    private double t2748;
    private double t2749;
    private double t2752;
    private double t2755;
    private double t2756;
    private double t2761;
    private double t2764;
    private double t2765;
    private double t2768;
    private double t2773;
    private double t2780;
    private double t2800;
    private double t2801;
    private double t2805;
    private double t2808;
    private double t2809;
    private double t2812;
    private double t2813;
    private double t2816;
    private double t2817;
    private double t2822;
    private double t2825;
    private double t2834;
    private double t2839;
    private double t2840;
    private double t2843;
    private double t2844;
    private double t2847;
    private double t2853;
    private double t2862;
    private double t2865;
    private double t2868;
    private double t2870;
    private double t2871;
    private double t2874;
    private double t2875;
    private double t2878;
    private double t2881;
    private double t2882;
    private double t2885;
    private double t2888;
    private double t2889;
    private double t2892;
    private double t2893;
    private double t2896;
    private double t2897;
    private double t2900;
    private double t2901;
    private double t2910;
    private double t2913;
    private double t2914;
    private double t2923;
    private double t2924;
    private double t2930;
    private double t2931;
    private double t2934;
    private double t2935;
    private double t2938;
    private double t2939;
    private double t2952;
    private double t2961;
    private double t2970;
    private double t3008;
    private double t3019;
    private double t3025;
    private double t3034;
    private double t3035;
    private double t3049;
    private double t3067;
    private double t3068;
    private double t3071;
    private double t3072;
    private double t3075;
    private double t3076;
    private double t3079;
    private double t3080;
    private double t3083;
    private double t3084;
    private double t3087;
    private double t3088;
    private double t3093;
    private double t3094;
    private double t3097;
    private double t3098;
    private double t3101;
    private double t3102;
    private double t3103;
    private double t3138;
    private double t3144;
    private double t3147;
    private double t3181;
    private double t3185;
    private double t3189;
    private double t3196;
    private double t3221;
    private double t3225;
    private double t3229;
    private double t3231;
    private double t3271;
    private double t3284;
    private double t3285;
    private double t3290;
    private double t3291;
    private double t3294;
    private double t3297;
    private double t3310;
    private double t3315;
    private double t3328;
    private double t3329;
    private double t3338;
    private double t3351;
    private double t3354;
    private double t3359;
    private double t3378;
    private double t3387;
    private double t3396;
    private double t3402;
    private double t3433;
    private double t3469;
    private double t3506;
    private double t3541;
    private double t3559;
    private double t3568;
    private double t3582;
    private double t3583;
    private double t3596;
    private double t3619;
    private double t3658;
    private double t3694;
    private double t3730;
    private double t3751;
    private double t3767;
    private double t3771;
    private double t3775;
    private double t3797;
    private double t3816;
    private double t3820;
    private double t3830;
    private double t3883;
    private double t3929;
    private double t3966;
    private double t3981;
    private double t3984;
    private double t3987;
    private double t3990;
    private double t3995;
    private double t4003;
    private double t4006;
    private double t4011;
    private double t4014;
    private double t4019;
    private double t4022;
    private double t4025;
    private double t4028;
    private double t4033;
    private double t4036;
    private double t4041;
    private double t4048;
    private double t4053;
    private double t4058;
    private double t4061;
    private double t4065;
    private double t4078;
    private double t4085;
    private double t4093;
    private double t4100;
    private double t4103;
    private double t4122;
    private double t4125;
    private double t4132;
    private double t4135;
    private double t4138;
    private double t4141;
    private double t4145;
    private double t4149;
    private double t4177;
    private double t4183;
    private double t4188;
    private double t4195;
    private double t4200;
    private double t4208;
    private double t4221;
    private double t4224;
    private double t4228;
    private double t4241;
    private double t4246;
    private double t4251;
    private double t4259;
    private double t4264;
    private double t4273;
    private double t4276;
    private double t4281;
    private double t4289;
    private double t4294;
    private double t4299;
    private double t4310;
    private double t4315;
    private double t4323;
    private double t4339;
    private double t4346;
    private double t4353;
    private double t4384;
    private double t4390;
    private double t4395;
    private double t4401;
    private double t4404;
    private double t4412;
    private double t4415;
    private double t4418;
    private double t4425;
    private double t4426;
    private double t4455;
    private double t4458;
    private double t4463;
    private double t4495;
    private double t4507;
    private double t4512;
    private double t4539;
    private double t4543;
    private double t4545;
    private double t4556;
    private double t4575;
    private double t4609;
    private double t4620;
    private double t4627;
    private double t4646;
    private double t4651;
    private double t4659;
    private double t4662;
    private double t4682;
    private double t4688;
    private double t4699;
    private double t4707;
    private double t4720;
    private double t4727;
    private double t4730;
    private double t4754;
    private double t4757;
    private double t4760;
    private double t4765;
    private double t4780;
    private double t4788;
    private double t4797;
    private double t4798;
    private double t4823;
    private double t4831;
    private double t4834;
    private double t4836;
    private double t4841;
    private double t4864;
    private double t4869;
    private double t4895;
    private double t4910;
    private double t4944;
    private double t4947;
    private double t4948;
    private double t4974;
    private double t4983;
    private double t5015;
    private double t5022;
    private double t5048;
    private double t5063;
    private double t5091;
    private double t5107;
    private double t5124;
    private double t5134;
    private double t5137;
    private double t5140;
    private double t5143;
    private double t5146;
    private double t5153;
    private double t5162;
    private double t5163;
    private double t5184;
    private double t5201;
    private double t5229;
    private double t5232;
    private double t5239;
    private double t5271;
    private double t5277;
    private double t5301;
    private double t5304;
    private double t5310;
    private double t5315;
    private double t5324;
    private double t5328;
    private double t5332;
    private double t5336;
    private double t5340;
    private double t5348;
    private double t5381;
    private double t5420;
    private double t5453;
    private double t5458;
    private double t5487;
    private double t5497;
    private double t5507;
    private double t5518;
    private double t5528;
    private double t5532;
    private double t5535;
    private double t5544;
    private double t5563;
    private double t5579;
    private double t5582;
    private double t5600;
    private double t5601;
    private double t5610;
    private double t5617;
    private double t5634;
    private double t5637;
    private double t5644;
    private double t5649;
    private double t5652;
    private double t5655;
    private double t5666;
    private double t5673;
    private double t5676;
    private double t5685;
    private double t5706;
    private double t5716;
    private double t5719;
    private double t5722;
    private double t5725;
    private double t5728;
    private double t5731;
    private double t5744;
    private double t5770;
    private double t5777;
    private double t5796;
    private double t5813;
    private double t5818;
    private double t5845;
    private double t5853;
    private double t5870;
    private double t5879;
    private double t5896;
    private double t5901;
    private double t5912;
    private double t5927;
    private double t5946;
    private double t5977;
    private double t5989;
    private double t5994;
    private double t6011;
    private double t6044;
    private double t6078;
    private double t6109;
    private double t6139;
    private double t6142;
    private double t6143;
    private double t6174;
    private double t6207;
    private double t6238;
    private double t6270;
    private double t6303;
    private double t6314;
    private double t6317;
    private double t6320;
    private double t6324;
    private double t6337;
    private double t6346;
    private double t6349;
    private double t6352;
    private double t6367;
    private double t6374;
    private double t6383;
    private double t6386;
    private double t6394;
    private double t6419;
    private double t6432;
    private double t6435;
    private double t6438;
    private double t6441;
    private double t6454;
    private double t6459;
    private double t6464;
    private double t6467;
    private double t6472;
    private double t6488;
    private double t6491;
    private double t6494;
    private double t6507;
    private double t6524;
    private double t6527;
    private double t6537;
    private double t6540;
    private double t6547;
    private double t6559;
    private double t6564;
    private double t6565;
    private double t6568;
    private double t6571;
    private double t6585;
    private double t6588;
    private double t6591;
    private double t6594;
    private double t6597;
    private double t6600;
    private double t6604;
    private double t6607;
    private double t6617;
    private double t6622;
    private double t6625;
    private double t6638;
    private double t6641;
    private double t6646;
    private double t6656;
    private double t6669;
    private double t6672;
    private double t6675;
    private double t6681;
    private double t6684;
    private double t6692;
    private double t6698;
    private double t6701;
    private double t6704;
    private double t6721;
    private double t6722;
    private double t6729;
    private double t6742;
    private double t6745;
    private double t6748;
    private double t6751;
    private double t6756;
    private double t6759;
    private double t6764;
    private double t6769;
    private double t6774;
    private double t6779;
    private double t6794;
    private double t6797;
    private double t6798;
    private double t6803;
    private double t6806;
    private double t6809;
    private double t6832;
    private double t6838;
    private double t6851;
    private double t6854;
    private double t6861;
    private double t6866;
    private double t6869;
    private double t6879;
    private double t6882;
    private double t6895;
    private double t6904;
    private double t6907;
    private double t6922;
    private double t6925;
    private double t6930;
    private double t6933;
    private double t6936;
    private double t6943;
    private double t6946;
    private double t6949;
    private double t6956;
    private double t6982;
    private double t6996;
    private double t7004;
    private double t7011;
    private double t7024;
    private double t7039;
    private double t7046;
    private double t7053;
    private double t7056;
    private double t7059;
    private double t7062;
    private double t7066;
    private double t7077;
    private double t7082;
    private double t7093;
    private double t7097;
    private double t7104;
    private double t7108;
    private double t7114;
    private double t7120;
    private double t7151;
    private double t7183;
    private double t7219;
    private double t7233;
    private double t7236;
    private double t7239;
    private double t7258;
    private double t7290;
    private double t7300;
    private double t7313;
    private double t7324;
    private double t7351;
    private double t7354;
    private double t7357;
    private double t7371;
    private double t7394;
    private double t7401;
    private double t7404;
    private double t7427;
    private double t7429;
    private double t7434;
    private double t7457;
    private double t7462;
    private double t7465;
    private double t7472;
    private double t7499;
    private double t7502;
    private double t7530;
    private double t7539;
    private double t7542;
    private double t7545;
    private double t7576;
    private double t7607;
    private double t7629;
    private double t7641;
    private double t7655;
    private double t7676;
    private double t7677;
    private double t7709;
    private double t7743;
    private double t7762;
    private double t7769;
    private double t7774;
    private double t7779;
    private double t7815;
    private double t7846;
    private double t7878;
    private double t7897;
    private double t7910;
    private double t7925;
    private double t7943;
    private double t7974;
    private double t7987;
    private double t8008;
    private double t8016;
    private double t8049;
    private double t8094;
    private double t8097;
    private double t8136;
    private double t8169;
    private double t8190;
    private double t8201;
    private double t8234;
    private double t8265;
    private double t8297;
    private double t8330;
    private double t8364;
    private double t8395;
    private double t8427;
    private double t8458;
    private double t8486;
    private double t8495;
    private double t8526;
    private double t8558;
    private double t8591;

    /**
     * Constructor
     * 
     * @param rEq
     *        equatorial radius (m)
     * @param j12
     *        12th order central body coefficient
     */
    public StelaZonalAttractionJ12(final double rEq, final double j12) {
        this.rEq = rEq;
        this.j12 = j12;
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
        this.derParUdeg12_1(a, ex, ey, ix, iy, mu);
        this.derParUdeg12_2(a, ex, ey, ix, iy);
        this.derParUdeg12_3(a, ex, ey, ix, iy);
        this.derParUdeg12_4(a, ex, ey, ix, iy);
        this.derParUdeg12_5(a, ex, ey, ix, iy);
        this.derParUdeg12_6(a, ex, ey, ix, iy);
        this.derParUdeg12_7(a, ex, ey, ix, iy);
        this.derParUdeg12_8(a, ex, ey, ix, iy);
        this.derParUdeg12_9(a, ex, ey, ix, iy);
        this.derParUdeg12_10(a, ex, ey, ix, iy);
        this.derParUdeg12_11(a, ex, ey, ix, iy);
        this.derParUdeg12_12(a, ex, ey, ix, iy);
        this.derParUdeg12_13(a, ex, ey, ix, iy);

        final double[] dPot = new double[6];
        dPot[0] = 0.429e3 / 0.262144e6 * this.t6 * this.t1245 / this.t1249 / this.t1247 / this.t1246 * this.t1261;
        dPot[1] = 0.0e0;
        dPot[2] =
            -0.33e2
                / 0.262144e6
                * this.t6
                * (this.t1338 + this.t1834 + this.t1790 + this.t1302 + this.t2078 + this.t2038 + this.t1996
                    + this.t1541 + this.t1657 + this.t1614 + this.t1954 + this.t1576
                    + this.t2202 + this.t2152 + this.t2506 + this.t1416 + this.t1374 + this.t1913 + this.t1875
                    + this.t2246 + this.t2546 + this.t2469 + this.t1466 + this.t2431
                    + this.t2393 + this.t2358 + this.t1746 + this.t2115 + this.t2320 + this.t1503 + this.t1703 
                    + this.t2285)
                * this.t2554 * this.t1261 - 0.759e3
                / 0.262144e6 * this.t6 * this.t2559 * this.t2562 * ex;
        dPot[3] =
            -0.33e2
                / 0.262144e6
                * this.t6
                * (this.t3730 + this.t3694 + this.t3469 + this.t3049 + this.t3433 + this.t3775 + this.t3396
                    + this.t3181 + this.t3359 + this.t2773 + this.t3929 + this.t3541
                    + this.t2675 + this.t3138 + this.t3315 + this.t3830 + this.t2628 + this.t3619 + this.t3883
                    + this.t3582 + this.t2868 + this.t3506 + this.t3008 + this.t2822
                    + this.t3658 + this.t2970 + this.t3101 + this.t3271 + this.t2923 + this.t3229 + this.t3966 
                    + this.t2719)
                * this.t2554 * this.t1261 - 0.759e3
                / 0.262144e6 * this.t6 * this.t2559 * this.t2562 * ey;
        dPot[4] =
            -0.33e2
                / 0.262144e6
                * this.t6
                * (this.t4022 + this.t4353 + this.t4682 + this.t5348 + this.t6044 + this.t4646 + this.t5124
                    + this.t4539 + this.t5563 + this.t4224 + this.t5813 + this.t5091
                    + this.t5528 + this.t4425 + this.t5673 + this.t4177 + this.t5420 + this.t4384 + this.t4495
                    + this.t5634 + this.t4910 + this.t4609 + this.t6011 + this.t5015
                    + this.t5600 + this.t4575 + this.t5048 + this.t4869 + this.t4983 + this.t5453 + this.t4947
                    + this.t6109 + this.t5381 + this.t5977 + this.t4315 + this.t6078
                    + this.t4138 + this.t4458 + this.t4103 + this.t4273 + this.t4061 + this.t5487 + this.t4834
                    + this.t6270 + this.t4797 + this.t4760 + this.t6142 + this.t5777
                    + this.t5744 + this.t5201 + this.t4720 + this.t5162 + this.t5845 + this.t6207 + this.t5304
                    + this.t5706 + this.t6174 + this.t6238 + this.t5271 + this.t5239
                    + this.t6303 + this.t5946 + this.t5912 + this.t5879) * this.t2554 * this.t1261;
        dPot[5] =
            -0.33e2
                / 0.262144e6
                * this.t6
                * (this.t7059 + this.t7024 + this.t7502 + this.t6982 + this.t7539 + this.t7743 + this.t8591
                    + this.t8364 + this.t8458 + this.t8330 + this.t6527 + this.t8297
                    + this.t8201 + this.t7709 + this.t7324 + this.t8049 + this.t6454 + this.t7676 + this.t7290
                    + this.t8427 + this.t8008 + this.t8234 + this.t7641 + this.t7974
                    + this.t6386 + this.t6646 + this.t6607 + this.t6419 + this.t6949 + this.t7943 + this.t7151
                    + this.t6907 + this.t7114 + this.t7878 + this.t7910 + this.t7846
                    + this.t7815 + this.t6352 + this.t8395 + this.t6869 + this.t8169 + this.t6494 + this.t8495
                    + this.t7779 + this.t6832 + this.t8136 + this.t7258 + this.t7462
                    + this.t7219 + this.t7183 + this.t8558 + this.t7427 + this.t6797 + this.t6759 + this.t6564
                    + this.t7394 + this.t8265 + this.t6721 + this.t7607 + this.t6684
                    + this.t7357 + this.t8526 + this.t8097 + this.t7576) * this.t2554 * this.t1261;

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
     * Partial derivative due to 12th order Earth potential zonal harmonics.
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
     * @throws CommonException
     *         derParUdeg12 cannot be computed
     */
    private final void derParUdeg12_1(final double a, final double ex, final double ey, final double ix,
                                      final double iy, final double mu) {
        this.t2 = this.rEq * this.rEq;
        this.t3 = this.t2 * this.t2;
        this.t4 = this.t3 * this.t3;
        this.t6 = mu * this.j12 * this.t4 * this.t3;
        this.t7 = ex * ex;
        this.t8 = ey * ey;
        this.t9 = this.t7 + this.t8;
        this.t10 = this.t9 * this.t9;
        this.t11 = this.t10 * this.t9;
        this.t12 = ix * ix;
        this.t13 = iy * iy;
        this.t14 = 0.1e1 - this.t12 - this.t13;
        this.t15 = this.t14 * this.t14;
        this.t16 = this.t15 * this.t15;
        this.t17 = this.t16 * this.t15;
        this.t18 = this.t11 * this.t17;
        this.t19 = this.t12 + this.t13;
        this.t20 = this.t19 * this.t19;
        this.t21 = this.t20 * this.t20;
        this.t22 = this.t21 * this.t19;
        this.t23 = this.t22 * this.t7;
        this.t24 = this.t23 * this.t12;
        this.t27 = this.t15 * this.t14;
        this.t28 = this.t10 * this.t27;
        this.t29 = this.t7 * this.t7;
        this.t30 = this.t19 * this.t29;
        this.t31 = this.t12 * this.t12;
        this.t32 = this.t30 * this.t31;
        this.t35 = this.t9 * this.t15;
        this.t36 = this.t19 * this.t8;
        this.t37 = this.t36 * this.t13;
        this.t40 = this.t16 * this.t14;
        this.t41 = this.t11 * this.t40;
        this.t42 = this.t20 * this.t19;
        this.t43 = this.t8 * this.t8;
        this.t44 = this.t42 * this.t43;
        this.t45 = this.t13 * this.t13;
        this.t46 = this.t44 * this.t45;
        this.t49 = this.t11 * this.t16;
        this.t50 = this.t42 * this.t7;
        this.t51 = this.t50 * this.t12;
        this.t54 = this.t10 * this.t16;
        this.t55 = this.t20 * this.t29;
        this.t56 = this.t55 * this.t31;
        this.t59 = this.t11 * this.t15;
        this.t62 = this.t29 * this.t7;
        this.t63 = this.t19 * this.t62;
        this.t64 = this.t31 * this.t12;
        this.t65 = this.t63 * this.t64;
        this.t68 = this.t10 * this.t10;
        this.t69 = this.t68 * this.t15;
        this.t72 = this.t9 * this.t16;
        this.t73 = this.t72 * this.t42;
        this.t74 = ex * ix;
        this.t75 = ey * iy;
        this.t76 = this.t74 * this.t75;
        this.t79 = this.t9 * this.t17;
        this.t80 = this.t79 * this.t20;
        this.t81 = this.t7 * ex;
        this.t82 = this.t29 * this.t81;
        this.t83 = this.t12 * ix;
        this.t84 = this.t31 * this.t83;
        this.t85 = this.t82 * this.t84;
        this.t86 = this.t85 * this.t75;
        this.t89 =
            0.1792e4 - 0.1577712776640e13 * this.t18 * this.t24 - 0.17643225600e11 * this.t28 * this.t32
                - 0.2882880000e10 * this.t35 * this.t37
                - 0.73976302400e11 * this.t41 * this.t46 - 0.285337166400e12 * this.t49 * this.t51 + 0.197541115200e12
                * this.t54 * this.t56
                - 0.1513512000e10 * this.t59 * this.t37 - 0.4877558400e10 * this.t54 * this.t65 - 0.99099000e8
                * this.t69 * this.t37
                - 0.893923430400e12 * this.t73 * this.t76 + 0.89994311680e11 * this.t80 * this.t86;
        this.t90 = this.t10 * this.t15;
        this.t91 = this.t90 * this.t19;
        this.t94 = this.t9 * this.t40;
        this.t95 = this.t94 * this.t20;
        this.t96 = this.t81 * this.t83;
        this.t97 = this.t8 * ey;
        this.t98 = this.t13 * iy;
        this.t99 = this.t97 * this.t98;
        this.t100 = this.t96 * this.t99;
        this.t103 = this.t41 * this.t42;
        this.t104 = this.t74 * this.t99;
        this.t107 = this.t10 * this.t17;
        this.t108 = this.t107 * this.t22;
        this.t111 = this.t43 * ey;
        this.t112 = this.t45 * iy;
        this.t113 = this.t111 * this.t112;
        this.t114 = this.t96 * this.t113;
        this.t117 = this.t29 * ex;
        this.t118 = this.t31 * ix;
        this.t119 = this.t117 * this.t118;
        this.t120 = this.t119 * this.t99;
        this.t123 = this.t68 * this.t16;
        this.t124 = this.t123 * this.t42;
        this.t127 = this.t10 * this.t40;
        this.t128 = this.t127 * this.t42;
        this.t131 = this.t11 * this.t27;
        this.t132 = this.t131 * this.t19;
        this.t135 = this.t94 * this.t21;
        this.t138 = this.t72 * this.t20;
        this.t139 = this.t7 * this.t12;
        this.t140 = this.t8 * this.t13;
        this.t141 = this.t139 * this.t140;
        this.t144 = this.t107 * this.t21;
        this.t147 =
            -0.8475667200e10 * this.t91 * this.t76 + 0.5462865408000e13 * this.t95 * this.t100 - 0.295905209600e12
                * this.t103 * this.t104
                - 0.7795757249280e13 * this.t108 * this.t76 + 0.629960181760e12 * this.t80 * this.t114
                + 0.629960181760e12 * this.t80 * this.t120
                - 0.39630162000e11 * this.t124 * this.t76 - 0.3550862515200e13 * this.t128 * this.t104
                - 0.5390985600e10 * this.t132 * this.t104
                + 0.3277719244800e13 * this.t135 * this.t76 + 0.2413593262080e13 * this.t138 * this.t141
                + 0.5568398035200e13 * this.t144 * this.t104;
        this.t149 = this.t9 * this.t27;
        this.t150 = this.t149 * this.t20;
        this.t153 = this.t49 * this.t42;
        this.t156 = this.t43 * this.t8;
        this.t157 = this.t45 * this.t13;
        this.t158 = this.t156 * this.t157;
        this.t159 = this.t139 * this.t158;
        this.t162 = this.t54 * this.t19;
        this.t163 = this.t29 * this.t31;
        this.t164 = this.t163 * this.t140;
        this.t167 = this.t43 * this.t45;
        this.t168 = this.t139 * this.t167;
        this.t171 = this.t54 * this.t20;
        this.t174 = this.t68 * this.t40;
        this.t175 = this.t174 * this.t21;
        this.t178 = this.t28 * this.t19;
        this.t179 = this.t96 * this.t75;
        this.t184 = this.t40 * ex;
        this.t185 = this.t43 * this.t43;
        this.t186 = this.t185 * ey;
        this.t187 = ix * this.t186;
        this.t188 = this.t45 * this.t45;
        this.t189 = this.t188 * iy;
        this.t190 = this.t187 * this.t189;
        this.t193 = this.t14 * ex;
        this.t194 = ey * ix;
        this.t195 = this.t194 * iy;
        this.t198 = this.t15 * this.t7;
        this.t199 = this.t12 * this.t8;
        this.t200 = this.t199 * this.t13;
        this.t203 =
            0.109780070400e12 * this.t150 * this.t76 - 0.570674332800e12 * this.t153 * this.t76 + 0.314980090880e12
                * this.t80 * this.t159
                - 0.73163376000e11 * this.t162 * this.t164 + 0.4097149056000e13 * this.t95 * this.t168
                + 0.1185246691200e13 * this.t171 * this.t141
                + 0.157199642600e12 * this.t175 * this.t76 - 0.70572902400e11 * this.t178 * this.t179
                + 0.8352597052800e13 * this.t144 * this.t141
                + 0.1655413760e10 * this.t184 * this.t190 + 0.15375360e8 * this.t193 * this.t195 + 0.3459456000e10
                * this.t198 * this.t200;
        this.t204 = this.t15 * this.t81;
        this.t205 = this.t83 * ey;
        this.t206 = this.t205 * iy;
        this.t209 = this.t40 * this.t82;
        this.t210 = this.t84 * this.t97;
        this.t211 = this.t210 * this.t98;
        this.t214 = this.t16 * this.t82;
        this.t215 = this.t84 * ey;
        this.t216 = this.t215 * iy;
        this.t219 = this.t16 * this.t81;
        this.t220 = this.t83 * this.t111;
        this.t221 = this.t220 * this.t112;
        this.t224 = this.t42 * this.t8;
        this.t225 = this.t224 * this.t13;
        this.t228 = this.t29 * this.t29;
        this.t229 = this.t20 * this.t228;
        this.t230 = this.t31 * this.t31;
        this.t231 = this.t229 * this.t230;
        this.t238 = this.t20 * this.t8;
        this.t239 = this.t238 * this.t13;
        this.t242 = this.t21 * this.t29;
        this.t243 = this.t242 * this.t31;
        this.t246 = this.t19 * this.t43;
        this.t247 = this.t246 * this.t45;
        this.t252 = this.t228 * ex;
        this.t253 = this.t40 * this.t252;
        this.t254 = this.t230 * ix;
        this.t255 = this.t254 * ey;
        this.t256 = this.t255 * iy;
        this.t259 =
            0.2306304000e10 * this.t204 * this.t206 + 0.19864965120e11 * this.t209 * this.t211 + 0.21283891200e11
                * this.t214 * this.t216
                + 0.148987238400e12 * this.t219 * this.t221 - 0.737486830080e12 * this.t54 * this.t225
                + 0.11249288960e11 * this.t79 * this.t231
                - 0.3897878624640e13 * this.t107 * this.t24 - 0.1720802603520e13 * this.t94 * this.t46
                + 0.86451805440e11 * this.t28 * this.t239
                + 0.119523695200e12 * this.t18 * this.t243 - 0.38423024640e11 * this.t149 * this.t247
                - 0.38423024640e11 * this.t149 * this.t32
                + 0.1655413760e10 * this.t253 * this.t256;
        this.t262 = this.t15 * ex;
        this.t263 = ix * this.t97;
        this.t264 = this.t263 * this.t98;
        this.t267 = this.t40 * this.t62;
        this.t268 = this.t64 * this.t43;
        this.t269 = this.t268 * this.t45;
        this.t272 = this.t119 * this.t75;
        this.t275 = this.t18 * this.t21;
        this.t278 = this.t131 * this.t20;
        this.t281 = this.t42 * this.t156;
        this.t282 = this.t281 * this.t157;
        this.t285 = this.t68 * this.t27;
        this.t288 = this.t20 * this.t43;
        this.t289 = this.t288 * this.t45;
        this.t292 = this.t20 * this.t7;
        this.t293 = this.t292 * this.t12;
        this.t296 = this.t21 * this.t43;
        this.t297 = this.t296 * this.t45;
        this.t304 =
            0.2306304000e10 * this.t262 * this.t264 + 0.34763688960e11 * this.t267 * this.t269 - 0.29265350400e11
                * this.t162 * this.t272
                + 0.478094780800e12 * this.t275 * this.t179 + 0.64691827200e11 * this.t278 * this.t76
                - 0.494968714240e12 * this.t79 * this.t282
                + 0.2190087900e10 * this.t285 * this.t239 + 0.15852064800e11 * this.t49 * this.t289 + 0.86451805440e11
                * this.t28 * this.t293
                + 0.2598585749760e13 * this.t79 * this.t297 - 0.737486830080e12 * this.t54 * this.t51
                - 0.17643225600e11 * this.t28 * this.t247;
        this.t305 = this.t42 * this.t29;
        this.t306 = this.t305 * this.t31;
        this.t313 = this.t40 * this.t117;
        this.t314 = this.t118 * this.t111;
        this.t315 = this.t314 * this.t112;
        this.t318 = this.t40 * this.t29;
        this.t319 = this.t31 * this.t156;
        this.t320 = this.t319 * this.t157;
        this.t323 = this.t16 * ex;
        this.t324 = this.t43 * this.t97;
        this.t325 = ix * this.t324;
        this.t326 = this.t45 * this.t98;
        this.t327 = this.t325 * this.t326;
        this.t330 = this.t27 * this.t29;
        this.t331 = this.t31 * this.t8;
        this.t332 = this.t331 * this.t13;
        this.t337 = this.t40 * this.t228;
        this.t338 = this.t230 * this.t8;
        this.t339 = this.t338 * this.t13;
        this.t342 = this.t16 * this.t62;
        this.t343 = this.t64 * this.t8;
        this.t344 = this.t343 * this.t13;
        this.t347 = this.t40 * this.t81;
        this.t348 = this.t83 * this.t324;
        this.t349 = this.t348 * this.t326;
        this.t352 = this.t16 * this.t29;
        this.t353 = this.t31 * this.t43;
        this.t354 = this.t353 * this.t45;
        this.t357 =
            -0.1720802603520e13 * this.t94 * this.t306 - 0.2227359214080e13 * this.t79 * this.t24 + 0.1392099508800e13
                * this.t107 * this.t243
                + 0.41716426752e11 * this.t313 * this.t315 + 0.34763688960e11 * this.t318 * this.t320
                + 0.21283891200e11 * this.t323 * this.t327
                + 0.54890035200e11 * this.t330 * this.t332 - 0.73976302400e11 * this.t41 * this.t306 + 0.7449361920e10
                * this.t337 * this.t339
                + 0.74493619200e11 * this.t342 * this.t344 + 0.19864965120e11 * this.t347 * this.t349
                + 0.186234048000e12 * this.t352 * this.t354;
        this.t359 = this.t27 * this.t117;
        this.t360 = this.t118 * ey;
        this.t361 = this.t360 * iy;
        this.t364 = this.t27 * this.t7;
        this.t365 = this.t12 * this.t43;
        this.t366 = this.t365 * this.t45;
        this.t369 = this.t27 * this.t81;
        this.t370 = this.t83 * this.t97;
        this.t371 = this.t370 * this.t98;
        this.t374 = this.t16 * this.t7;
        this.t375 = this.t12 * this.t156;
        this.t376 = this.t375 * this.t157;
        this.t379 = this.t40 * this.t7;
        this.t380 = this.t12 * this.t185;
        this.t381 = this.t380 * this.t188;
        this.t384 = this.t27 * ex;
        this.t385 = ix * this.t111;
        this.t386 = this.t385 * this.t112;
        this.t389 = this.t21 * this.t7;
        this.t390 = this.t389 * this.t12;
        this.t397 = this.t21 * this.t8;
        this.t398 = this.t397 * this.t13;
        this.t403 = this.t22 * this.t8;
        this.t404 = this.t403 * this.t13;
        this.t407 =
            0.21956014080e11 * this.t359 * this.t361 + 0.54890035200e11 * this.t364 * this.t366 + 0.73186713600e11
                * this.t369 * this.t371
                + 0.74493619200e11 * this.t374 * this.t376 + 0.7449361920e10 * this.t379 * this.t381 + 0.21956014080e11
                * this.t384 * this.t386
                + 0.1638859622400e13 * this.t94 * this.t390 + 0.119523695200e12 * this.t18 * this.t297
                - 0.56246444800e11 * this.t107 * this.t282
                + 0.1109644536000e13 * this.t41 * this.t398 - 0.887715628800e12 * this.t127 * this.t46
                - 0.1577712776640e13 * this.t18 * this.t404;
        this.t408 = this.t42 * this.t62;
        this.t409 = this.t408 * this.t64;
        this.t416 = this.t19 * this.t185;
        this.t417 = this.t416 * this.t188;
        this.t420 = this.t19 * this.t7;
        this.t421 = this.t420 * this.t12;
        this.t426 = this.t20 * this.t62;
        this.t427 = this.t426 * this.t64;
        this.t430 = this.t68 * this.t17;
        this.t437 = this.t20 * this.t156;
        this.t438 = this.t437 * this.t157;
        this.t445 =
            -0.494968714240e12 * this.t79 * this.t409 - 0.19815081000e11 * this.t123 * this.t51 + 0.1638859622400e13
                * this.t94 * this.t398
                - 0.4552387840e10 * this.t94 * this.t417 - 0.2882880000e10 * this.t35 * this.t421 - 0.1347746400e10
                * this.t131 * this.t247
                + 0.273143270400e12 * this.t94 * this.t427 - 0.113547510440e12 * this.t430 * this.t404
                - 0.446961715200e12 * this.t72 * this.t225
                + 0.15852064800e11 * this.t49 * this.t56 + 0.29590520960e11 * this.t127 * this.t438 - 0.887715628800e12
                * this.t127 * this.t306
                + 0.78599821300e11 * this.t174 * this.t398;
        this.t453 = this.t19 * this.t228;
        this.t454 = this.t453 * this.t230;
        this.t475 =
            0.273143270400e12 * this.t94 * this.t438 + 0.29590520960e11 * this.t127 * this.t427 - 0.4552387840e10
                * this.t94 * this.t454
                + 0.197541115200e12 * this.t54 * this.t289 + 0.32345913600e11 * this.t131 * this.t239 - 0.1347746400e10
                * this.t131 * this.t32
                - 0.2227359214080e13 * this.t79 * this.t404 - 0.47888755200e11 * this.t72 * this.t65
                + 0.402265543680e12 * this.t72 * this.t56
                - 0.113547510440e12 * this.t430 * this.t24 - 0.1513512000e10 * this.t59 * this.t421 - 0.19815081000e11
                * this.t123 * this.t225;
        this.t486 = this.t19 * this.t156;
        this.t487 = this.t486 * this.t157;
        this.t502 =
            0.2598585749760e13 * this.t79 * this.t243 - 0.3897878624640e13 * this.t107 * this.t404 - 0.285337166400e12
                * this.t49 * this.t225
                + 0.1109644536000e13 * this.t41 * this.t390 + 0.54890035200e11 * this.t149 * this.t239
                - 0.47888755200e11 * this.t72 * this.t487
                + 0.1392099508800e13 * this.t107 * this.t297 + 0.32345913600e11 * this.t131 * this.t293
                + 0.2190087900e10 * this.t285 * this.t293
                - 0.4877558400e10 * this.t54 * this.t487 + 0.2796304230720e13 * this.t127 * this.t398 - 0.4237833600e10
                * this.t90 * this.t37;
        this.t508 = this.t16 * this.t117;
        this.t509 = this.t118 * this.t97;
        this.t510 = this.t509 * this.t98;
        this.t513 = this.t20 * this.t185;
        this.t514 = this.t513 * this.t188;
        this.t529 = this.t285 * this.t20;
        this.t532 = this.t10 * this.t14;
        this.t535 =
            -0.4237833600e10 * this.t90 * this.t421 + 0.54890035200e11 * this.t149 * this.t293 + 0.148987238400e12
                * this.t508 * this.t510
                + 0.11249288960e11 * this.t79 * this.t514 + 0.78599821300e11 * this.t174 * this.t390
                - 0.446961715200e12 * this.t72 * this.t51
                - 0.56246444800e11 * this.t107 * this.t409 - 0.99099000e8 * this.t69 * this.t421 + 0.2796304230720e13
                * this.t127 * this.t390
                + 0.402265543680e12 * this.t72 * this.t289 + 0.4380175800e10 * this.t529 * this.t76 + 0.60540480e8
                * this.t532 * this.t140;
        this.t536 = this.t62 * this.t64;
        this.t543 = this.t11 * this.t14;
        this.t550 = this.t228 * this.t230;
        this.t553 = this.t68 * this.t14;
        this.t556 = this.t9 * this.t14;
        this.t561 = this.t40 * this.t42;
        this.t564 = this.t16 * this.t20;
        this.t569 =
            0.245044800e9 * this.t28 * this.t536 + 0.504504000e9 * this.t90 * this.t163 + 0.2613811200e10 * this.t149
                * this.t536 + 0.20180160e8
                * this.t543 * this.t139 + 0.245044800e9 * this.t28 * this.t158 + 0.2613811200e10 * this.t149
                * this.t158 + 0.443414400e9 * this.t72 * this.t550
                + 0.1261260e7 * this.t553 * this.t139 + 0.46126080e8 * this.t556 * this.t140 + 0.36036000e8 * this.t59
                * this.t167
                - 0.595948953600e12 * this.t561 * this.t163 + 0.148987238400e12 * this.t564 * this.t163
                - 0.595948953600e12 * this.t561 * this.t167;
        this.t572 = this.t17 * this.t19;
        this.t573 = this.t228 * this.t7;
        this.t574 = this.t230 * this.t12;
        this.t575 = this.t573 * this.t574;
        this.t578 = this.t17 * this.t20;
        this.t579 = this.t185 * this.t188;
        this.t582 = this.t40 * this.t20;
        this.t587 = this.t16 * this.t42;
        this.t598 = this.t27 * this.t19;
        this.t601 = this.t15 * this.t19;
        this.t604 = this.t40 * this.t19;
    }

    /**
     * Partial derivative due to 12th order Earth potential zonal harmonics.
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
            derParUdeg12_2(final double a, final double ex, final double ey, final double ix, final double iy) {
        this.t607 =
            -0.692263936e9 * this.t572 * this.t575 + 0.57111774720e11 * this.t578 * this.t579 + 0.312873200640e12
                * this.t582 * this.t158
                + 0.148987238400e12 * this.t564 * this.t167 - 0.49662412800e11 * this.t587 * this.t139
                + 0.1210809600e10 * this.t35 * this.t167
                + 0.504504000e9 * this.t90 * this.t167 + 0.36036000e8 * this.t59 * this.t163 + 0.1210809600e10
                * this.t35 * this.t163
                - 0.15682867200e11 * this.t598 * this.t163 - 0.384384000e9 * this.t601 * this.t140 - 0.24831206400e11
                * this.t604 * this.t579;
        this.t608 = this.t17 * this.t21;
        this.t611 = this.t17 * this.t42;
        this.t614 = this.t17 * this.t22;
        this.t629 = this.t185 * this.t8;
        this.t630 = this.t188 * this.t13;
        this.t631 = this.t629 * this.t630;
        this.t636 = this.t16 * this.t19;
        this.t639 =
            0.856676620800e12 * this.t608 * this.t167 - 0.533043230720e12 * this.t611 * this.t536 - 0.228447098880e12
                * this.t614 * this.t139
                + 0.856676620800e12 * this.t608 * this.t163 + 0.60540480e8 * this.t532 * this.t139 + 0.20180160e8
                * this.t543 * this.t140
                + 0.1261260e7 * this.t553 * this.t140 + 0.443414400e9 * this.t72 * this.t579 + 0.46126080e8 * this.t556
                * this.t139 - 0.692263936e9
                * this.t572 * this.t631 + 0.312873200640e12 * this.t582 * this.t536 - 0.59594895360e11 * this.t636
                * this.t158;
        this.t641 = this.t40 * this.t21;
        this.t648 = this.t27 * this.t20;
        this.t667 =
            0.173818444800e12 * this.t641 * this.t139 - 0.49662412800e11 * this.t587 * this.t140 - 0.24831206400e11
                * this.t604 * this.t550
                + 0.6534528000e10 * this.t648 * this.t140 + 0.6534528000e10 * this.t648 * this.t139 - 0.384384000e9
                * this.t601 * this.t139
                - 0.533043230720e12 * this.t611 * this.t158 - 0.59594895360e11 * this.t636 * this.t536
                + 0.57111774720e11 * this.t578 * this.t550
                - 0.15682867200e11 * this.t598 * this.t167 - 0.228447098880e12 * this.t614 * this.t140
                + 0.173818444800e12 * this.t641 * this.t140;
        this.t670 = this.t72 * this.t19;
        this.t673 = this.t127 * this.t21;
        this.t676 = this.t582 * this.t7;
        this.t679 = this.t582 * this.t81;
        this.t682 = this.t582 * ex;
        this.t685 = this.t604 * ex;
        this.t688 = this.t532 * ex;
        this.t691 = this.t72 * ex;
        this.t694 = this.t28 * this.t117;
        this.t700 = this.t28 * ex;
        this.t703 = this.t59 * ex;
        this.t706 =
            0.478094780800e12 * this.t275 * this.t104 - 0.287332531200e12 * this.t670 * this.t272 + 0.5592608461440e13
                * this.t673 * this.t76
                + 0.4693098009600e13 * this.t676 * this.t366 + 0.6257464012800e13 * this.t679 * this.t371
                + 0.1877239203840e13 * this.t682
                * this.t386 - 0.198649651200e12 * this.t685 * this.t327 + 0.121080960e9 * this.t688 * this.t195
                + 0.3547315200e10 * this.t691 * this.t327
                + 0.1470268800e10 * this.t694 * this.t361 - 0.456894197760e12 * this.t614 * ex * this.t195
                + 0.1470268800e10 * this.t700 * this.t386
                + 0.144144000e9 * this.t703 * this.t264;
        this.t711 = this.t556 * ex;
        this.t714 = this.t72 * this.t29;
        this.t717 = this.t72 * this.t117;
        this.t720 = this.t59 * this.t81;
        this.t723 = this.t601 * ex;
        this.t726 = this.t611 * this.t29;
        this.t729 = this.t611 * this.t81;
        this.t732 = this.t564 * this.t81;
        this.t735 = this.t578 * this.t62;
        this.t738 = this.t604 * this.t62;
        this.t741 = this.t572 * this.t117;
        this.t744 = this.t578 * this.t81;
        this.t747 =
            0.92252160e8 * this.t711 * this.t195 + 0.31039008000e11 * this.t714 * this.t354 + 0.24831206400e11
                * this.t717 * this.t510
                + 0.144144000e9 * this.t720 * this.t206 - 0.768768000e9 * this.t723 * this.t195 - 0.7995648460800e13
                * this.t726 * this.t332
                - 0.10660864614400e14 * this.t729 * this.t371 + 0.595948953600e12 * this.t732 * this.t206
                + 0.1599129692160e13 * this.t735
                * this.t344 - 0.695273779200e12 * this.t738 * this.t344 - 0.174450511872e12 * this.t741 * this.t315
                + 0.3198259384320e13 * this.t744
                * this.t221;
        this.t748 = this.t608 * this.t7;
        this.t751 = this.t149 * this.t7;
        this.t754 = this.t72 * this.t82;
        this.t757 = this.t72 * this.t7;
        this.t760 = this.t72 * this.t81;
        this.t763 = this.t59 * this.t7;
        this.t766 = this.t35 * this.t81;
        this.t769 = this.t564 * this.t7;
        this.t772 = this.t572 * this.t81;
        this.t775 = this.t35 * this.t7;
        this.t778 = this.t72 * this.t62;
        this.t784 =
            0.5140059724800e13 * this.t748 * this.t200 + 0.39207168000e11 * this.t751 * this.t366 + 0.3547315200e10
                * this.t754 * this.t216
                + 0.12415603200e11 * this.t757 * this.t376 + 0.24831206400e11 * this.t760 * this.t221 + 0.216216000e9
                * this.t763 * this.t200
                + 0.4843238400e10 * this.t766 * this.t206 + 0.893923430400e12 * this.t769 * this.t200
                - 0.83071672320e11 * this.t772 * this.t349
                + 0.7264857600e10 * this.t775 * this.t200 + 0.12415603200e11 * this.t778 * this.t344 + 0.2522520e7
                * this.t553 * ex * this.t195;
        this.t786 = this.t28 * this.t29;
        this.t789 = this.t28 * this.t81;
        this.t792 = this.t28 * this.t7;
        this.t795 = this.t90 * ex;
        this.t798 = this.t149 * ex;
        this.t801 = this.t59 * this.t20;
        this.t803 = this.t68 * this.t9;
        this.t804 = this.t803 * this.t40;
        this.t807 = this.t636 * ex;
        this.t810 = this.t636 * this.t7;
        this.t813 = this.t636 * this.t117;
        this.t816 = this.t578 * this.t117;
        this.t819 = this.t578 * this.t82;
        this.t822 =
            0.3675672000e10 * this.t786 * this.t332 + 0.4900896000e10 * this.t789 * this.t371 + 0.3675672000e10
                * this.t792 * this.t366
                + 0.2018016000e10 * this.t795 * this.t264 + 0.15682867200e11 * this.t798 * this.t386 + 0.3178375200e10
                * this.t801
                - 0.29867932094e11 * this.t804 * this.t22 - 0.357569372160e12 * this.t807 * this.t386
                - 0.893923430400e12 * this.t810 * this.t366
                - 0.357569372160e12 * this.t813 * this.t361 + 0.3198259384320e13 * this.t816 * this.t510
                + 0.456894197760e12 * this.t819 * this.t216;
        this.t823 = this.t572 * this.t252;
        this.t826 = this.t598 * this.t7;
        this.t829 = this.t608 * ex;
        this.t832 = this.t16 * this.t185;
        this.t835 = this.t15 * this.t43;
        this.t838 = this.t40 * this.t629;
        this.t841 = this.t40 * this.t573;
        this.t844 = this.t27 * this.t62;
        this.t847 = this.t49 * this.t21;
        this.t849 = this.t27 * this.t156;
        this.t852 = this.t16 * this.t228;
        this.t855 = this.t556 * this.t19;
        this.t857 = this.t149 * this.t117;
        this.t860 =
            -0.6922639360e10 * this.t823 * this.t256 - 0.94097203200e11 * this.t826 * this.t200 + 0.3426706483200e13
                * this.t829 * this.t264
                + 0.2660486400e10 * this.t832 * this.t188 + 0.576576000e9 * this.t835 * this.t45 + 0.165541376e9
                * this.t838 * this.t630
                + 0.165541376e9 * this.t841 * this.t574 + 0.3659335680e10 * this.t844 * this.t64 + 0.399472032960e12
                * this.t847
                + 0.3659335680e10 * this.t849 * this.t157 + 0.2660486400e10 * this.t852 * this.t230 - 0.11531520e8
                * this.t855
                + 0.15682867200e11 * this.t857 * this.t361;
        this.t863 = this.t149 * this.t81;
        this.t866 = this.t149 * this.t29;
        this.t869 = this.t543 * ex;
        this.t872 = this.t90 * this.t7;
        this.t875 = this.t564 * ex;
        this.t878 = this.t582 * this.t29;
        this.t881 = this.t572 * this.t7;
        this.t884 = this.t587 * ex;
        this.t887 = this.t572 * ex;
        this.t890 = this.t604 * this.t117;
        this.t893 = this.t608 * this.t81;
        this.t896 = this.t611 * this.t7;
        this.t899 =
            0.52276224000e11 * this.t863 * this.t371 + 0.39207168000e11 * this.t866 * this.t332 + 0.40360320e8
                * this.t869 * this.t195
                + 0.3027024000e10 * this.t872 * this.t200 + 0.595948953600e12 * this.t875 * this.t264
                + 0.4693098009600e13 * this.t878 * this.t332
                - 0.31151877120e11 * this.t881 * this.t381 - 0.99324825600e11 * this.t884 * this.t195 - 0.6922639360e10
                * this.t887 * this.t190
                - 0.1390547558400e13 * this.t890 * this.t510 + 0.3426706483200e13 * this.t893 * this.t206
                - 0.7995648460800e13 * this.t896
                * this.t366;
        this.t900 = this.t611 * ex;
        this.t903 = this.t90 * this.t81;
        this.t906 = this.t35 * ex;
        this.t909 = this.t72 * this.t21;
        this.t911 = this.t553 * this.t19;
        this.t913 = this.t21 * this.t20;
        this.t916 = this.t123 * this.t21;
        this.t918 = this.t285 * this.t42;
        this.t920 = this.t803 * this.t17;
        this.t923 = this.t543 * this.t19;
        this.t925 = this.t14 * this.t7;
        this.t928 = this.t598 * ex;
        this.t931 =
            -0.3198259384320e13 * this.t900 * this.t386 + 0.2018016000e10 * this.t903 * this.t206 + 0.4843238400e10
                * this.t906 * this.t264
                + 0.55870214400e11 * this.t909 - 0.22702680e8 * this.t911 + 0.1392099508800e13 * this.t107 * this.t913
                + 0.133751796750e12
                * this.t916 - 0.17520703200e11 * this.t918 + 0.39741628654e11 * this.t920 * this.t913 - 0.70630560e8
                * this.t923 + 0.7687680e7
                * this.t925 * this.t12 - 0.62731468800e11 * this.t928 * this.t264;
        this.t933 = this.t582 * this.t117;
        this.t936 = this.t561 * this.t81;
        this.t939 = this.t578 * this.t29;
        this.t942 = this.t578 * this.t7;
        this.t945 = this.t572 * this.t29;
        this.t948 = this.t604 * this.t29;
        this.t951 = this.t598 * this.t81;
        this.t954 = this.t532 * this.t19;
        this.t956 = this.t54 * this.t21;
        this.t958 = this.t28 * this.t42;
        this.t962 = this.t803 * this.t27;
        this.t965 =
            0.1877239203840e13 * this.t933 * this.t361 - 0.2383795814400e13 * this.t936 * this.t206
                + 0.3997824230400e13 * this.t939 * this.t354
                + 0.1599129692160e13 * this.t942 * this.t376 - 0.145375426560e12 * this.t945 * this.t320
                - 0.1738184448000e13 * this.t948 * this.t354
                - 0.62731468800e11 * this.t951 * this.t206 - 0.57657600e8 * this.t954 + 0.307286179200e12 * this.t956
                - 0.41167526400e11
                * this.t958 + 0.624511307420e12 * this.t430 * this.t913 - 0.1095043950e10 * this.t962 * this.t42;
        this.t968 = this.t174 * this.t22;
        this.t970 = this.t94 * this.t22;
        this.t972 = this.t803 * this.t15;
        this.t975 = this.t648 * ex;
        this.t978 = this.t611 * this.t117;
        this.t981 = this.t604 * this.t81;
        this.t984 = this.t572 * this.t228;
        this.t987 = this.t636 * this.t29;
        this.t990 = this.t641 * ex;
        this.t993 = this.t561 * this.t7;
        this.t996 = this.t636 * this.t81;
        this.t999 = this.t604 * this.t82;
        this.t1002 =
            0.1840664906080e13 * this.t18 * this.t913 - 0.471598927800e12 * this.t968 - 0.191200289280e12 * this.t970
                + 0.64414350e8
                * this.t972 * this.t20 + 0.13069056000e11 * this.t975 * this.t195 - 0.3198259384320e13 * this.t978
                * this.t361 - 0.1390547558400e13
                * this.t981 * this.t221 - 0.31151877120e11 * this.t984 * this.t339 - 0.893923430400e12 * this.t987
                * this.t332 + 0.347636889600e12
                * this.t990 * this.t195 - 0.3575693721600e13 * this.t993 * this.t200 - 0.1191897907200e13 * this.t996
                * this.t371 - 0.198649651200e12
                * this.t999 * this.t216;
        this.t1006 = this.t578 * ex;
        this.t1009 = this.t561 * ex;
        this.t1012 = this.t572 * this.t62;
        this.t1015 = this.t604 * this.t7;
        this.t1018 = this.t572 * this.t82;
        this.t1023 = this.t35 * this.t20;
        this.t1025 = this.t803 * this.t14;
        this.t1028 = this.t803 * this.t16;
        this.t1031 = this.t15 * this.t29;
        this.t1034 = this.t41 * this.t22;
        this.t1036 = this.t127 * this.t22;
        this.t1038 =
            0.456894197760e12 * this.t1006 * this.t327 - 0.2383795814400e13 * this.t1009 * this.t264
                - 0.145375426560e12 * this.t1012 * this.t269
                - 0.695273779200e12 * this.t1015 * this.t376 - 0.83071672320e11 * this.t1018 * this.t211
                + 0.247484357120e12 * this.t79 * this.t913
                + 0.480480000e9 * this.t1023 - 0.1387386e7 * this.t1025 * this.t19 + 0.8421409425e10 * this.t1028
                * this.t21 + 0.576576000e9
                * this.t1031 * this.t31 - 0.1398152115360e13 * this.t1034 - 0.1065258754560e13 * this.t1036;
        this.t1039 = this.t149 * this.t42;
        this.t1041 = this.t14 * this.t8;
        this.t1044 = this.t131 * this.t42;
        this.t1046 = this.t69 * this.t20;
        this.t1048 = this.t90 * this.t20;
        this.t1050 = this.t94 * this.t42;
        this.t1053 = this.t74 * this.t113;
        this.t1058 = this.t127 * this.t20;
        this.t1061 = this.t94 * this.t19;
        this.t1068 =
            -0.7623616000e10 * this.t1039 + 0.7687680e7 * this.t1041 * this.t13 - 0.52831658880e11 * this.t1044
                + 0.1040539500e10
                * this.t1046 + 0.2522520000e10 * this.t1048 - 0.6883210414080e13 * this.t1050 * this.t104
                + 0.1638859622400e13 * this.t95
                * this.t1053 - 0.29265350400e11 * this.t162 * this.t1053 + 0.443857814400e12 * this.t1058 * this.t164
                - 0.36419102720e11 * this.t1061
                * this.t86 - 0.6883210414080e13 * this.t1050 * this.t179 + 0.790164460800e12 * this.t171 * this.t104;
        this.t1074 = this.t107 * this.t42;
        this.t1079 = this.t18 * this.t22;
        this.t1090 = this.t79 * this.t42;
        this.t1097 =
            -0.10324815621120e14 * this.t1050 * this.t141 - 0.254933719040e12 * this.t1061 * this.t114
                - 0.337478668800e12 * this.t1074
                * this.t272 - 0.70572902400e11 * this.t178 * this.t104 - 0.3155425553280e13 * this.t1079 * this.t76
                - 0.718331328000e12 * this.t670
                * this.t164 - 0.843696672000e12 * this.t1074 * this.t168 + 0.1609062174720e13 * this.t138 * this.t179
                + 0.177543125760e12
                * this.t1058 * this.t1053 - 0.2969812285440e13 * this.t1090 * this.t272 - 0.287332531200e12 * this.t670
                * this.t1053
                - 0.3550862515200e13 * this.t128 * this.t179;
        this.t1098 = this.t41 * this.t21;
        this.t1101 = this.t324 * this.t326;
        this.t1102 = this.t74 * this.t1101;
        this.t1107 = this.t79 * this.t21;
        this.t1116 = this.t49 * this.t20;
        this.t1127 = this.t163 * this.t167;
        this.t1130 =
            0.2219289072000e13 * this.t1098 * this.t76 + 0.89994311680e11 * this.t80 * this.t1102 - 0.127466859520e12
                * this.t1061 * this.t159
                + 0.15591514498560e14 * this.t1107 * this.t141 + 0.10394342999040e14 * this.t1107 * this.t179
                - 0.7424530713600e13 * this.t1090
                * this.t164 - 0.9899374284800e13 * this.t1090 * this.t100 + 0.63408259200e11 * this.t1116 * this.t104
                - 0.36419102720e11 * this.t1061
                * this.t1102 - 0.957775104000e12 * this.t670 * this.t100 - 0.5326293772800e13 * this.t128 * this.t141
                - 0.254933719040e12
                * this.t1061 * this.t120 - 0.318667148800e12 * this.t1061 * this.t1127;
        this.t1139 = this.t69 * this.t19;
        this.t1142 = this.t79 * this.t22;
        this.t1153 = this.t54 * this.t42;
        this.t1156 = this.t536 * this.t140;
        this.t1159 = this.t35 * this.t19;
        this.t1162 =
            0.5568398035200e13 * this.t144 * this.t179 - 0.337478668800e12 * this.t1074 * this.t1053 - 0.73163376000e11
                * this.t162 * this.t168
                - 0.198198000e9 * this.t1139 * this.t76 - 0.4454718428160e13 * this.t1142 * this.t76 - 0.5390985600e10
                * this.t132 * this.t179
                - 0.8086478400e10 * this.t132 * this.t141 + 0.177543125760e12 * this.t1058 * this.t272
                + 0.591810419200e12 * this.t1058 * this.t100
                - 0.1474973660160e13 * this.t1153 * this.t76 - 0.127466859520e12 * this.t1061 * this.t1156
                - 0.5765760000e10 * this.t1159 * this.t76;
        this.t1167 = this.t59 * this.t19;
        this.t1184 = this.t149 * this.t19;
        this.t1189 = this.t28 * this.t20;
        this.t1192 =
            -0.1124928896000e13 * this.t1074 * this.t100 + 0.1638859622400e13 * this.t95 * this.t272 - 0.3027024000e10
                * this.t1167 * this.t76
                - 0.443857814400e12 * this.t103 * this.t141 - 0.295905209600e12 * this.t103 * this.t179
                - 0.843696672000e12 * this.t1074 * this.t164
                + 0.790164460800e12 * this.t171 * this.t179 + 0.314980090880e12 * this.t80 * this.t1156
                + 0.10394342999040e14 * this.t1107
                * this.t104 - 0.718331328000e12 * this.t670 * this.t168 - 0.153692098560e12 * this.t1184 * this.t179
                - 0.153692098560e12 * this.t1184
                * this.t104 + 0.172903610880e12 * this.t1189 * this.t76;
        this.t1194 = this.t430 * this.t22;
        this.t1211 = this.t27 * this.t42;
        this.t1219 =
            -0.227095020880e12 * this.t1194 * this.t76 + 0.63408259200e11 * this.t1116 * this.t179 + 0.4097149056000e13
                * this.t95 * this.t164
                - 0.105859353600e12 * this.t178 * this.t141 - 0.7424530713600e13 * this.t1090 * this.t168
                - 0.2969812285440e13 * this.t1090
                * this.t1053 - 0.230538147840e12 * this.t1184 * this.t141 - 0.97551168000e11 * this.t162 * this.t100
                - 0.158412800e9 * this.t1211
                + 0.443857814400e12 * this.t1058 * this.t168 + 0.1609062174720e13 * this.t138 * this.t104
                + 0.787450227200e12 * this.t80 * this.t1127;
        this.t1220 = this.t14 * this.t19;
    }

    /**
     * Partial derivative due to 12th order Earth potential zonal harmonics.
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
            derParUdeg12_3(final double a, final double ex, final double ey, final double ix, final double iy) {
        this.t1222 = this.t16 * this.t21;
        this.t1224 = this.t15 * this.t20;
        this.t1228 = this.t40 * this.t22;
        this.t1240 =
            -0.279552e6 * this.t1220 + 0.1128691200e10 * this.t1222 + 0.10483200e8 * this.t1224 + 0.4845847552e10
                * this.t17 * this.t913
                - 0.3792402432e10 * this.t1228 + 0.4851e4 * this.t803 + 0.221760e6 * this.t10 + 0.49280e5 * this.t7
                + 0.49280e5 * this.t8
                + 0.258720e6 * this.t11 + 0.80850e5 * this.t68 + 0.95112388800e11 * this.t1116 * this.t141
                + 0.717142171200e12 * this.t275
                * this.t141;
        this.t1245 =
            this.t860 + this.t89 + this.t259 + this.t965 + this.t203 + this.t784 + this.t147 + this.t667 + this.t747
                + this.t822 + this.t706 + this.t639 + this.t1130 + this.t607 + this.t931
                + this.t535 + this.t1068 + this.t1038 + this.t569 + this.t1002 + this.t1240 + this.t1219 + this.t502
                + this.t1192 + this.t899 + this.t1097 + this.t475 + this.t407
                + this.t357 + this.t304 + this.t445 + this.t1162;
        this.t1246 = a * a;
        this.t1247 = this.t1246 * this.t1246;
        this.t1249 = this.t1247 * this.t1247;
        this.t1253 = 0.1e1 - this.t7 - this.t8;
        this.t1254 = this.t1253 * this.t1253;
        this.t1256 = this.t1254 * this.t1254;
        this.t1257 = this.t1256 * this.t1256;
        this.t1259 = MathLib.sqrt(this.t1253);
        this.t1261 = 0.1e1 / this.t1259 / this.t1257 / this.t1254 / this.t1253;
        this.t1267 = this.t81 * this.t12;
        this.t1270 = this.t117 * this.t15;
        this.t1277 = this.t35 * this.t29;
        this.t1302 =
            0.887040e6 * this.t9 * ex + 0.121080960e9 * this.t532 * this.t1267 + 0.2421619200e10 * this.t1270
                * this.t31
                + 0.10280119449600e14 * this.t748 * this.t206 - 0.15991296921600e14 * this.t900 * this.t366
                + 0.8072064000e10 * this.t1277
                * this.t206 + 0.24831206400e11 * this.t778 * this.t216 + 0.24831206400e11 * this.t691 * this.t376
                + 0.74493619200e11 * this.t757
                * this.t221 + 0.7351344000e10 * this.t786 * this.t361 + 0.124156032000e12 * this.t760 * this.t354
                + 0.124156032000e12 * this.t714
                * this.t510 + 0.432432000e9 * this.t763 * this.t206 + 0.12108096000e11 * this.t766 * this.t200
                + 0.18772392038400e14 * this.t679
                * this.t332 - 0.62303754240e11 * this.t887 * this.t381 - 0.6952737792000e13 * this.t948 * this.t510;
        this.t1317 = this.t532 * this.t7;
        this.t1338 =
            -0.3155425553280e13 * this.t1079 * this.t195 - 0.6883210414080e13 * this.t1050 * this.t264
                + 0.177543125760e12 * this.t1058
                * this.t386 - 0.287332531200e12 * this.t670 * this.t386 + 0.89994311680e11 * this.t80 * this.t327
                + 0.63408259200e11 * this.t1116
                * this.t264 - 0.36419102720e11 * this.t1061 * this.t327 + 0.242161920e9 * this.t1317 * this.t195
                + 0.1609062174720e13 * this.t138
                * this.t264 + 0.10394342999040e14 * this.t1107 * this.t264 - 0.2969812285440e13 * this.t1090
                * this.t386 - 0.5765760000e10
                * this.t1159 * this.t195 - 0.3027024000e10 * this.t1167 * this.t195 - 0.29265350400e11 * this.t162
                * this.t386 + 0.790164460800e12
                * this.t171 * this.t264 - 0.70572902400e11 * this.t178 * this.t264 + 0.6054048000e10 * this.t872
                * this.t206;
        this.t1374 =
            0.78414336000e11 * this.t866 * this.t361 - 0.1474973660160e13 * this.t1153 * this.t195 - 0.153692098560e12
                * this.t1184 * this.t264
                + 0.172903610880e12 * this.t1189 * this.t195 - 0.337478668800e12 * this.t1074 * this.t386
                - 0.198198000e9 * this.t1139 * this.t195
                - 0.4454718428160e13 * this.t1142 * this.t195 + 0.5227622400e10 * this.t384 * this.t158
                + 0.14702688000e11 * this.t789 * this.t332
                + 0.14702688000e11 * this.t792 * this.t371 + 0.7351344000e10 * this.t700 * this.t366
                + 0.156828672000e12 * this.t751 * this.t371
                + 0.156828672000e12 * this.t863 * this.t332 + 0.6054048000e10 * this.t795 * this.t200
                + 0.14529715200e11 * this.t906 * this.t200
                + 0.74493619200e11 * this.t717 * this.t344 - 0.14849061427200e14 * this.t729 * this.t366;
        this.t1388 = this.t20 * this.t117;
        this.t1389 = this.t1388 * this.t31;
        this.t1392 = this.t42 * this.t117;
        this.t1393 = this.t1392 * this.t31;
        this.t1398 = this.t20 * this.t82;
        this.t1403 = this.t21 * this.t117 * this.t31;
        this.t1416 =
            0.1552320e7 * this.t10 * ex + 0.98560e5 * ex - 0.3198259384320e13 * this.t611 * this.t386 + 0.4843238400e10
                * this.t35
                * this.t264 + 0.14898723840e11 * this.t184 * this.t381 - 0.9104775680e10 * this.t184 * this.t417
                - 0.893923430400e12 * this.t323
                * this.t225 + 0.95112388800e11 * this.t54 * this.t1389 - 0.3550862515200e13 * this.t94 * this.t1393
                + 0.546286540800e12 * this.t184
                * this.t438 + 0.118362083840e12 * this.t94 * this.t1398 * this.t64 + 0.5568398035200e13 * this.t79
                * this.t1403 + 0.208582133760e12
                * this.t318 * this.t315 + 0.139054755840e12 * this.t347 * this.t320 + 0.219560140800e12 * this.t369
                * this.t332 - 0.443857814400e12
                * this.t127 * this.t1393 + 0.59594895360e11 * this.t209 * this.t339;
        this.t1433 = this.t20 * this.t81;
        this.t1434 = this.t1433 * this.t12;
        this.t1439 = this.t19 * this.t81;
        this.t1440 = this.t1439 * this.t12;
        this.t1443 = this.t19 * this.t117;
        this.t1444 = this.t1443 * this.t31;
        this.t1447 = ex * this.t17;
        this.t1451 = this.t22 * this.t81 * this.t12;
        this.t1456 = this.t42 * this.t81;
        this.t1457 = this.t1456 * this.t12;
        this.t1462 = this.t21 * this.t81;
        this.t1463 = this.t1462 * this.t12;
        this.t1466 =
            0.446961715200e12 * this.t508 * this.t344 + 0.59594895360e11 * this.t379 * this.t349 + 0.744936192000e12
                * this.t219 * this.t354
                + 0.109780070400e12 * this.t330 * this.t361 + 0.109780070400e12 * this.t384 * this.t366
                + 0.219560140800e12 * this.t364 * this.t371
                + 0.148987238400e12 * this.t323 * this.t376 + 0.194075481600e12 * this.t28 * this.t1434
                + 0.17520703200e11 * this.t131 * this.t1434
                - 0.16951334400e11 * this.t35 * this.t1440 - 0.8086478400e10 * this.t28 * this.t1444
                - 0.4454718428160e13 * this.t1447 * this.t404
                - 0.908380083520e12 * this.t18 * this.t1451 - 0.9081072000e10 * this.t90 * this.t1440
                - 0.158520648000e12 * this.t49 * this.t1457
                + 0.3277719244800e13 * this.t184 * this.t398 + 0.628798570400e12 * this.t41 * this.t1463;
        this.t1503 =
            -0.224985779200e12 * this.t79 * this.t42 * this.t82 * this.t64 - 0.792792000e9 * this.t59 * this.t1440
                + 0.11185216922880e14 * this.t94
                * this.t1463 + 0.804531087360e12 * this.t323 * this.t289 + 0.6657867216000e13 * this.t127 * this.t1463
                + 0.109780070400e12
                * this.t384 * this.t239 - 0.95777510400e11 * this.t323 * this.t487 + 0.208582133760e12 * this.t313
                * this.t269 - 0.3550862515200e13
                * this.t128 * this.t264 - 0.227095020880e12 * this.t1194 * this.t195 - 0.295905209600e12 * this.t103
                * this.t264 - 0.7795757249280e13
                * this.t108 * this.t195 - 0.39630162000e11 * this.t124 * this.t195 - 0.893923430400e12 * this.t73
                * this.t195 - 0.8475667200e10 * this.t91
                * this.t195 + 0.157199642600e12 * this.t175 * this.t195 + 0.6918912000e10 * this.t262 * this.t200;
        this.t1523 = this.t19 * this.t82;
        this.t1541 =
            0.6918912000e10 * this.t198 * this.t206 + 0.139054755840e12 * this.t267 * this.t211 + 0.148987238400e12
                * this.t342 * this.t216
                + 0.446961715200e12 * this.t374 * this.t221 - 0.9466276659840e13 * this.t107 * this.t1451
                - 0.70572902400e11 * this.t149 * this.t1444
                - 0.5765760000e10 * this.t262 * this.t37 - 0.1712022998400e13 * this.t54 * this.t1457
                + 0.790164460800e12 * this.t72 * this.t1389
                - 0.19510233600e11 * this.t72 * this.t1523 * this.t64 + 0.744936192000e12 * this.t352 * this.t510
                + 0.22498577920e11 * this.t1447
                * this.t514 - 0.15591514498560e14 * this.t79 * this.t1451 - 0.3441605207040e13 * this.t184 * this.t46
                + 0.717142171200e12 * this.t107
                * this.t1403 - 0.76846049280e11 * this.t384 * this.t247 + 0.14898723840e11 * this.t337 * this.t256;
        this.t1576 =
            0.9686476800e10 * this.t198 * this.t264 + 0.31365734400e11 * this.t844 * this.t361 + 0.104552448000e12
                * this.t330 * this.t371
                + 0.78414336000e11 * this.t359 * this.t332 + 0.14529715200e11 * this.t204 * this.t200
                + 0.24831206400e11 * this.t214 * this.t344
                + 0.31365734400e11 * this.t364 * this.t386 + 0.78414336000e11 * this.t369 * this.t366 + 0.7094630400e10
                * this.t852 * this.t216
                + 0.24831206400e11 * this.t219 * this.t376 + 0.49662412800e11 * this.t352 * this.t221 + 0.9686476800e10
                * this.t1031 * this.t206
                + 0.184504320e9 * this.t925 * this.t195 + 0.62078016000e11 * this.t508 * this.t354 + 0.49662412800e11
                * this.t342 * this.t510
                + 0.7094630400e10 * this.t374 * this.t327 - 0.989937428480e12 * this.t1447 * this.t282;
        this.t1614 =
            0.345807221760e12 * this.t149 * this.t1434 + 0.5197171499520e13 * this.t1447 * this.t297
                - 0.2949947320320e13 * this.t72 * this.t1457
                - 0.5390985600e10 * this.t132 * this.t264 + 0.3277719244800e13 * this.t135 * this.t195
                + 0.5568398035200e13 * this.t144 * this.t264
                + 0.109780070400e12 * this.t150 * this.t195 - 0.570674332800e12 * this.t153 * this.t195
                - 0.1787846860800e13 * this.t807 * this.t366
                - 0.1787846860800e13 * this.t987 * this.t361 + 0.15991296921600e14 * this.t939 * this.t510
                + 0.144144000e9 * this.t59 * this.t264
                + 0.92252160e8 * this.t556 * this.t195 - 0.768768000e9 * this.t601 * this.t195 + 0.1877239203840e13
                * this.t582 * this.t386
                - 0.198649651200e12 * this.t604 * this.t327 + 0.121080960e9 * this.t532 * this.t195;
        this.t1617 = this.t1462 * this.t31;
        this.t1626 = this.t543 * this.t7;
        this.t1641 = this.t15 * ix;
        this.t1657 =
            0.3547315200e10 * this.t72 * this.t327 + 0.10394342999040e14 * this.t79 * this.t1617 + 0.3198259384320e13
                * this.t735 * this.t216
                - 0.62303754240e11 * this.t984 * this.t256 - 0.188194406400e12 * this.t928 * this.t200 + 0.20180160e8
                * this.t1626 * this.t195
                + 0.64691827200e11 * this.t278 * this.t195 + 0.5592608461440e13 * this.t673 * this.t195
                + 0.14702688000e11 * this.t857 * this.t332
                + 0.19603584000e11 * this.t866 * this.t371 + 0.14702688000e11 * this.t863 * this.t366 + 0.8072064000e10
                * this.t775 * this.t264
                + 0.2306304000e10 * this.t1641 * this.t99 - 0.4454718428160e13 * this.t81 * this.t17 * this.t22
                * this.t12 + 0.5197171499520e13
                * this.t117 * this.t17 * this.t21 * this.t31 - 0.3441605207040e13 * this.t313 * this.t42 * this.t31
                + 0.3426706483200e13 * this.t608 * this.t264;
        this.t1661 = ex * this.t12;
        this.t1664 = this.t81 * this.t31;
        this.t1671 = this.t913 * ex;
        this.t1674 = this.t22 * ex;
        this.t1677 = this.t42 * ex;
        this.t1680 = this.t21 * ex;
        this.t1687 = this.t40 * ix;
        this.t1688 = this.t186 * this.t189;
        this.t1691 = this.t14 * ix;
        this.t1700 = this.t167 * ex;
        this.t1703 =
            0.2522520e7 * this.t553 * this.t195 - 0.456894197760e12 * this.t614 * this.t1661 + 0.3426706483200e13
                * this.t608 * this.t1664
                + 0.242161920e9 * this.t556 * this.t1267 + 0.886828800e9 * this.t323 * this.t579 + 0.11043989436480e14
                * this.t107 * this.t1671
                - 0.298679320940e12 * this.t174 * this.t1674 - 0.10950439500e11 * this.t285 * this.t1677
                + 0.84214094250e11 * this.t123 * this.t1680
                - 0.4261035018240e13 * this.t94 * this.t1674 + 0.2396832197760e13 * this.t54 * this.t1680
                + 0.1655413760e10 * this.t1687 * this.t1688
                + 0.15375360e8 * this.t1691 * this.t75 + 0.1470268800e10 * this.t28 * this.t386 + 0.456894197760e12
                * this.t578 * this.t327
                - 0.2383795814400e13 * this.t561 * this.t264 + 0.216216000e9 * this.t90 * this.t1700;
        this.t1704 = this.t158 * ex;
        this.t1709 = this.t140 * ex;
        this.t1724 = this.t20 * ex;
        this.t1725 = this.t1724 * this.t12;
        this.t1728 = this.t1680 * this.t12;
        this.t1731 = this.t1433 * this.t31;
        this.t1734 = this.t1456 * this.t31;
        this.t1737 = this.t1388 * this.t64;
        this.t1746 =
            0.980179200e9 * this.t149 * this.t1704 + 0.2018016000e10 * this.t35 * this.t1700 + 0.121080960e9
                * this.t532 * this.t1709
                + 0.10090080e8 * this.t543 * this.t1709 + 0.242161920e9 * this.t556 * this.t1709 + 0.2018016000e10
                * this.t90 * this.t264
                + 0.15682867200e11 * this.t149 * this.t386 - 0.1390547558400e13 * this.t738 * this.t216
                + 0.1297296000e10 * this.t903 * this.t200
                + 0.4380175800e10 * this.t285 * this.t1725 + 0.2219289072000e13 * this.t41 * this.t1728
                + 0.63408259200e11 * this.t49 * this.t1731
                - 0.3550862515200e13 * this.t127 * this.t1734 + 0.177543125760e12 * this.t127 * this.t1737
                + 0.5568398035200e13 * this.t107
                * this.t1617 - 0.295905209600e12 * this.t41 * this.t1734 + 0.3277719244800e13 * this.t94 * this.t1728;
        this.t1751 = this.t1674 * this.t12;
        this.t1754 = this.t19 * ex;
        this.t1755 = this.t1754 * this.t12;
        this.t1763 = this.t1439 * this.t31;
        this.t1766 = this.t1443 * this.t64;
        this.t1777 = this.t1677 * this.t12;
        this.t1790 =
            -0.456894197760e12 * this.t614 * this.t195 - 0.227095020880e12 * this.t430 * this.t1751 - 0.3027024000e10
                * this.t59 * this.t1755
                - 0.8475667200e10 * this.t90 * this.t1755 - 0.36419102720e11 * this.t94 * this.t1523 * this.t230
                - 0.5390985600e10 * this.t131
                * this.t1763 - 0.287332531200e12 * this.t72 * this.t1766 + 0.13069056000e11 * this.t648 * this.t195
                + 0.347636889600e12 * this.t641
                * this.t195 - 0.62731468800e11 * this.t598 * this.t264 + 0.64691827200e11 * this.t131 * this.t1725
                - 0.39630162000e11 * this.t123
                * this.t1777 - 0.5765760000e10 * this.t35 * this.t1755 + 0.1609062174720e13 * this.t72 * this.t1731
                + 0.5568398035200e13 * this.t79
                * this.t1671 - 0.8388912692160e13 * this.t127 * this.t1674 + 0.1070014374000e13 * this.t49 * this.t1680;
        this.t1797 = this.t117 * this.t64;
        this.t1800 = this.t82 * this.t230;
        this.t1805 = this.t117 * this.t31;
        this.t1812 = this.t82 * this.t27;
        this.t1834 =
            -0.13873860e8 * this.t553 * this.t1754 + 0.13069056000e11 * this.t648 * this.t1661 - 0.768768000e9
                * this.t601 * this.t1661
                - 0.357569372160e12 * this.t636 * this.t1797 + 0.456894197760e12 * this.t578 * this.t1800
                + 0.2421619200e10 * this.t262 * this.t167
                + 0.216216000e9 * this.t90 * this.t1805 - 0.62731468800e11 * this.t598 * this.t1664
                - 0.3198259384320e13 * this.t611 * this.t1797
                + 0.5227622400e10 * this.t1812 * this.t64 - 0.989937428480e12 * this.t82 * this.t17 * this.t42
                * this.t64 - 0.5765760000e10 * this.t204
                * this.t19 * this.t12 + 0.546286540800e12 * this.t209 * this.t20 * this.t64 + 0.3277719244800e13
                * this.t347 * this.t21 * this.t12
                + 0.1877239203840e13 * this.t582 * this.t1797 + 0.347636889600e12 * this.t641 * this.t1661
                - 0.198649651200e12 * this.t604
                * this.t1800;
        this.t1875 =
            -0.99324825600e11 * this.t587 * this.t1661 + 0.1229144716800e13 * this.t72 * this.t1680 - 0.181621440e9
                * this.t543 * this.t1754
                - 0.3772791422400e13 * this.t41 * this.t1674 + 0.9594778152960e13 * this.t816 * this.t344
                - 0.4171642675200e13 * this.t890
                * this.t344 - 0.872252559360e12 * this.t945 * this.t315 + 0.9594778152960e13 * this.t942 * this.t221
                + 0.10280119449600e14
                * this.t829 * this.t200 + 0.5881075200e10 * this.t751 * this.t386 - 0.140165625600e12 * this.t131
                * this.t1677 + 0.397416286540e12
                * this.t430 * this.t1671 - 0.423783360e9 * this.t532 * this.t1754 + 0.22498577920e11 * this.t252
                * this.t17 * this.t20 * this.t230
                + 0.109780070400e12 * this.t369 * this.t20 * this.t12 - 0.893923430400e12 * this.t219 * this.t42
                * this.t12 - 0.9104775680e10 * this.t253
                * this.t19 * this.t230;
        this.t1902 = this.t27 * ix;
    }

    /**
     * Partial derivative due to 12th order Earth potential zonal harmonics.
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
            derParUdeg12_4(final double a, final double ex, final double ey, final double ix, final double iy) {
        this.t1913 =
            -0.95777510400e11 * this.t214 * this.t19 * this.t64 + 0.804531087360e12 * this.t508 * this.t20 * this.t31
                - 0.2383795814400e13 * this.t561
                * this.t1664 + 0.92252160e8 * this.t193 * this.t140 + 0.10090080e8 * this.t543 * this.t1267
                + 0.3547315200e10 * this.t72 * this.t1800
                + 0.144144000e9 * this.t59 * this.t1664 + 0.4843238400e10 * this.t35 * this.t1664 + 0.121080960e9
                * this.t532 * this.t1661
                + 0.92252160e8 * this.t556 * this.t1661 + 0.1470268800e10 * this.t28 * this.t1797 + 0.2018016000e10
                * this.t90 * this.t1664
                + 0.21956014080e11 * this.t1902 * this.t113 - 0.230630400e9 * this.t556 * this.t1754 + 0.8324316000e10
                * this.t59 * this.t1724
                + 0.19070251200e11 * this.t90 * this.t1724 + 0.10090080000e11 * this.t35 * this.t1724;
        this.t1928 = this.t82 * this.t64;
        this.t1933 = this.t342 * this.t19;
        this.t1954 =
            -0.316989953280e12 * this.t28 * this.t1677 + 0.4996090459360e13 * this.t18 * this.t1671 - 0.164670105600e12
                * this.t149 * this.t1677
                + 0.644143500e9 * this.t69 * this.t1724 + 0.2018016000e10 * this.t35 * this.t1805 + 0.980179200e9
                * this.t149 * this.t1928
                + 0.595948953600e12 * this.t564 * this.t1664 - 0.574665062400e12 * this.t1933 * this.t361
                + 0.1787846860800e13 * this.t875
                * this.t200 - 0.249215016960e12 * this.t881 * this.t349 - 0.31982593843200e14 * this.t729 * this.t332
                - 0.31982593843200e14
                * this.t896 * this.t371 + 0.1787846860800e13 * this.t769 * this.t206 + 0.8194298112000e13 * this.t933
                * this.t332
                - 0.5939624570880e13 * this.t896 * this.t386 + 0.118362083840e12 * this.t95 * this.t1704
                + 0.628798570400e12 * this.t1098
                * this.t1709;
        this.t1957 = this.t364 * this.t20;
        this.t1962 = this.t374 * this.t42;
        this.t1979 = this.t330 * this.t19;
        this.t1990 = this.t379 * this.t21;
        this.t1993 = this.t352 * this.t20;
        this.t1996 =
            0.4827186524160e13 * this.t732 * this.t200 + 0.219560140800e12 * this.t1957 * this.t195 + 0.629960181760e12
                * this.t744 * this.t376
                - 0.1787846860800e13 * this.t1962 * this.t195 + 0.179988623360e12 * this.t228 * this.t17 * this.t20
                * this.t216 + 0.10925730816000e14
                * this.t878 * this.t371 + 0.1259920363520e13 * this.t939 * this.t221 + 0.1259920363520e13 * this.t735
                * this.t510
                + 0.20788685998080e14 * this.t748 * this.t264 - 0.1436662656000e13 * this.t996 * this.t366
                - 0.307384197120e12 * this.t1979
                * this.t206 - 0.307384197120e12 * this.t826 * this.t264 - 0.509867438080e12 * this.t738 * this.t510
                - 0.637334297600e12 * this.t890
                * this.t354 + 0.8194298112000e13 * this.t679 * this.t366 + 0.6555438489600e13 * this.t1990 * this.t195
                + 0.3218124349440e13
                * this.t1993 * this.t206;
        this.t2002 = this.t198 * this.t19;
        this.t2005 = this.t267 * this.t20;
        this.t2022 = this.t337 * this.t19;
        this.t2025 = this.t318 * this.t42;
        this.t2038 =
            -0.13766420828160e14 * this.t993 * this.t264 - 0.461076295680e12 * this.t951 * this.t200 - 0.11531520000e11
                * this.t2002 * this.t195
                + 0.3277719244800e13 * this.t2005 * this.t361 + 0.3218124349440e13 * this.t769 * this.t264
                + 0.1574900454400e13 * this.t816
                * this.t354 + 0.629960181760e12 * this.t819 * this.t344 - 0.357569372160e12 * this.t636 * this.t386
                - 0.72838205440e11 * this.t1015
                * this.t327 - 0.1915550208000e13 * this.t987 * this.t371 + 0.3277719244800e13 * this.t676 * this.t386
                - 0.72838205440e11 * this.t2022
                * this.t216 - 0.13766420828160e14 * this.t2025 * this.t206 - 0.20649631242240e14 * this.t936
                * this.t200 - 0.509867438080e12
                * this.t948 * this.t221 - 0.1436662656000e13 * this.t813 * this.t332 - 0.5939624570880e13 * this.t62
                * this.t17 * this.t42 * this.t361;
        this.t2071 = this.t1267 * this.t140;
        this.t2078 =
            -0.574665062400e12 * this.t810 * this.t386 + 0.179988623360e12 * this.t942 * this.t327 - 0.254933719040e12
                * this.t981 * this.t376
                + 0.31183028997120e14 * this.t893 * this.t200 + 0.20788685998080e14 * this.t29 * this.t17 * this.t21
                * this.t206
                - 0.14849061427200e14 * this.t978 * this.t332 - 0.19798748569600e14 * this.t726 * this.t371
                - 0.872252559360e12 * this.t741
                * this.t269 + 0.4380175800e10 * this.t529 * this.t195 + 0.432432000e9 * this.t703 * this.t200
                + 0.14529715200e11 * this.t775 * this.t206
                + 0.478094780800e12 * this.t275 * this.t264 - 0.8909436856320e13 * this.t7 * this.t17 * this.t22
                * this.t195 - 0.254933719040e12
                * this.t999 * this.t344 - 0.48518870400e11 * this.t178 * this.t2071 - 0.581501706240e12 * this.t772
                * this.t320 - 0.6952737792000e13
                * this.t981 * this.t354;
        this.t2115 =
            -0.188194406400e12 * this.t826 * this.t206 - 0.19510233600e11 * this.t670 * this.t1704
                + 0.11185216922880e14 * this.t135 * this.t1709
                + 0.5568398035200e13 * this.t1107 * this.t1700 - 0.7151387443200e13 * this.t1009 * this.t200
                - 0.3575693721600e13 * this.t810
                * this.t371 + 0.9386196019200e13 * this.t878 * this.t361 - 0.7151387443200e13 * this.t993 * this.t206
                + 0.15991296921600e14
                * this.t744 * this.t354 + 0.3198259384320e13 * this.t1006 * this.t376 - 0.1390547558400e13 * this.t685
                * this.t376
                - 0.581501706240e12 * this.t1012 * this.t211 - 0.15991296921600e14 * this.t726 * this.t361
                - 0.4171642675200e13 * this.t1015
                * this.t221 - 0.249215016960e12 * this.t1018 * this.t339 - 0.3575693721600e13 * this.t996 * this.t332
                + 0.2219289072000e13
                * this.t1098 * this.t195;
        this.t2130 = this.t16 * ix;
        this.t2149 = this.t1661 * this.t167;
        this.t2152 =
            -0.158520648000e12 * this.t153 * this.t1709 + 0.717142171200e12 * this.t144 * this.t1700 - 0.16951334400e11
                * this.t1159 * this.t1709
                + 0.790164460800e12 * this.t138 * this.t1700 + 0.194075481600e12 * this.t1189 * this.t1709
                - 0.3550862515200e13 * this.t1050
                * this.t1700 - 0.9466276659840e13 * this.t108 * this.t1709 + 0.21283891200e11 * this.t2130 * this.t1101
                + 0.95112388800e11
                * this.t171 * this.t1700 - 0.8086478400e10 * this.t178 * this.t1700 - 0.908380083520e12 * this.t1079
                * this.t1709 - 0.224985779200e12
                * this.t1090 * this.t1704 + 0.6657867216000e13 * this.t673 * this.t1709 + 0.345807221760e12 * this.t150
                * this.t1709
                - 0.2949947320320e13 * this.t73 * this.t1709 - 0.70572902400e11 * this.t1184 * this.t1700
                - 0.1687393344000e13 * this.t1074
                * this.t2149;
        this.t2162 = this.t7 * ix;
        this.t2163 = this.t2162 * this.t75;
        this.t2168 = this.t29 * this.t83;
        this.t2169 = this.t2168 * this.t75;
        this.t2172 = this.t1392 * this.t64;
        this.t2175 = this.t2162 * this.t113;
        this.t2178 = this.t1267 * this.t167;
        this.t2183 = this.t62 * this.t118;
        this.t2184 = this.t2183 * this.t75;
        this.t2187 = this.t2168 * this.t99;
        this.t2194 = this.t83 * this.t7;
        this.t2195 = this.t2194 * this.t113;
        this.t2198 = this.t29 * this.t118;
        this.t2199 = this.t2198 * this.t99;
        this.t2202 =
            0.78414336000e11 * this.t798 * this.t366 - 0.792792000e9 * this.t1167 * this.t1709 - 0.443857814400e12
                * this.t128 * this.t1700
                - 0.9081072000e10 * this.t91 * this.t1709 + 0.691614443520e12 * this.t150 * this.t2163
                - 0.21305175091200e14 * this.t1050
                * this.t2071 + 0.22273592140800e14 * this.t1107 * this.t2169 - 0.2969812285440e13 * this.t79
                * this.t2172 - 0.1349914675200e13
                * this.t1090 * this.t2175 - 0.292653504000e12 * this.t670 * this.t2178 - 0.32345913600e11 * this.t178
                * this.t2169
                + 0.710172503040e12 * this.t95 * this.t2184 + 0.2367241676800e13 * this.t95 * this.t2187
                + 0.1638859622400e13 * this.t94 * this.t1737
                + 0.2306304000e10 * this.t204 * this.t31 + 0.1889880545280e13 * this.t80 * this.t2195
                + 0.3149800908800e13 * this.t80 * this.t2199;
        this.t2203 = this.t2194 * this.t99;
        this.t2206 = this.t2194 * this.t75;
        this.t2209 = this.t1664 * this.t140;
        this.t2212 = this.t1661 * this.t140;
        this.t2223 = this.t2198 * this.t75;
        this.t2228 = this.t62 * this.t84;
        this.t2229 = this.t2228 * this.t75;
        this.t2234 = this.t1661 * this.t158;
        this.t2243 = this.t1805 * this.t140;
        this.t2246 =
            -0.292653504000e12 * this.t162 * this.t2203 + 0.190224777600e12 * this.t1116 * this.t2206
                + 0.16388596224000e14 * this.t95
                * this.t2209 - 0.211718707200e12 * this.t178 * this.t2212 - 0.5899894640640e13 * this.t73 * this.t2163
                + 0.172903610880e12 * this.t28
                * this.t1725 + 0.8194298112000e13 * this.t95 * this.t2149 + 0.1434284342400e13 * this.t275 * this.t2206
                - 0.146326752000e12
                * this.t162 * this.t2223 - 0.292653504000e12 * this.t162 * this.t2209 + 0.629960181760e12 * this.t80
                * this.t2229
                + 0.16388596224000e14 * this.t95 * this.t2203 + 0.629960181760e12 * this.t80 * this.t2234
                - 0.7795757249280e13 * this.t107
                * this.t1751 + 0.22370433845760e14 * this.t135 * this.t2163 - 0.117061401600e12 * this.t670
                * this.t2184 - 0.292653504000e12
                * this.t670 * this.t2243;
        this.t2264 = this.t2162 * this.t99;
        this.t2271 = this.t193 * this.t19;
        this.t2285 =
            0.4827186524160e13 * this.t138 * this.t2212 + 0.16705194105600e14 * this.t144 * this.t2212
                - 0.211718707200e12 * this.t178
                * this.t2206 + 0.2370493382400e13 * this.t171 * this.t2212 - 0.4454718428160e13 * this.t79 * this.t1751
                - 0.282291609600e12
                * this.t1184 * this.t2169 + 0.33410388211200e14 * this.t1107 * this.t2071 - 0.32345913600e11
                * this.t178 * this.t2264
                + 0.22273592140800e14 * this.t1107 * this.t2264 - 0.153692098560e12 * this.t149 * this.t1763
                - 0.23063040e8 * this.t2271
                + 0.21956014080e11 * this.t359 * this.t64 - 0.1474973660160e13 * this.t54 * this.t1777
                + 0.35041406400e11 * this.t278 * this.t2163
                + 0.388150963200e12 * this.t1189 * this.t2163 + 0.2868568684800e13 * this.t144 * this.t2169
                - 0.6883210414080e13 * this.t94
                * this.t1734;
        this.t2288 = this.t384 * this.t42;
        this.t2320 =
            0.15375360e8 * this.t193 * this.t12 - 0.15247232000e11 * this.t2288 + 0.380449555200e12 * this.t171
                * this.t2169
                - 0.423437414400e12 * this.t1184 * this.t2071 - 0.1775431257600e13 * this.t128 * this.t2264
                - 0.31183028997120e14 * this.t1142
                * this.t2163 - 0.70572902400e11 * this.t28 * this.t1763 - 0.3424045996800e13 * this.t1153 * this.t2163
                - 0.33902668800e11
                * this.t1159 * this.t2163 + 0.4740986764800e13 * this.t138 * this.t2071 + 0.1257597140800e13
                * this.t1098 * this.t2163
                + 0.478094780800e12 * this.t18 * this.t1617 + 0.2868568684800e13 * this.t144 * this.t2264
                - 0.317041296000e12 * this.t153
                * this.t2163 - 0.14203450060800e14 * this.t1050 * this.t2264 - 0.390204672000e12 * this.t670
                * this.t2187 - 0.1816760167040e13
                * this.t1079 * this.t2163;
        this.t2324 = this.t323 * this.t21;
        this.t2349 = this.t81 * this.t14;
        this.t2358 =
            -0.3155425553280e13 * this.t18 * this.t1751 + 0.111740428800e12 * this.t2324 - 0.1436662656000e13
                * this.t670 * this.t2223
                + 0.89994311680e11 * this.t79 * this.t1398 * this.t230 + 0.790164460800e12 * this.t54 * this.t1731
                - 0.570674332800e12 * this.t49
                * this.t1777 - 0.29265350400e11 * this.t54 * this.t1766 + 0.109780070400e12 * this.t149 * this.t1725
                + 0.5592608461440e13 * this.t127
                * this.t1728 + 0.21283891200e11 * this.t214 * this.t230 + 0.494968714240e12 * this.t1447 * this.t913
                + 0.1655413760e10 * this.t253
                * this.t574 - 0.198198000e9 * this.t69 * this.t1755 + 0.92252160e8 * this.t2349 * this.t12
                + 0.40360320e8 * this.t543 * this.t1661
                - 0.99324825600e11 * this.t587 * this.t195 - 0.2873325312000e13 * this.t670 * this.t2203;
        this.t2393 =
            0.190224777600e12 * this.t1116 * this.t2212 + 0.1434284342400e13 * this.t275 * this.t2212
                - 0.2873325312000e13 * this.t670
                * this.t2209 + 0.4827186524160e13 * this.t138 * this.t2206 + 0.31183028997120e14 * this.t1107
                * this.t2206 - 0.1687393344000e13
                * this.t1074 * this.t2223 - 0.1349914675200e13 * this.t1090 * this.t2184 - 0.20649631242240e14
                * this.t1050 * this.t2212
                - 0.764801157120e12 * this.t1061 * this.t2195 + 0.1775431257600e13 * this.t1058 * this.t2209
                - 0.254933719040e12 * this.t1061
                * this.t2229 - 0.20649631242240e14 * this.t1050 * this.t2206 + 0.380449555200e12 * this.t171
                * this.t2264 + 0.31183028997120e14
                * this.t1107 * this.t2212 - 0.10652587545600e14 * this.t128 * this.t2206 - 0.254933719040e12
                * this.t1061 * this.t2234
                - 0.29698122854400e14 * this.t1090 * this.t2209;
        this.t2398 = this.t1797 * this.t140;
        this.t2431 =
            -0.29698122854400e14 * this.t1090 * this.t2203 - 0.764801157120e12 * this.t1061 * this.t2398
                + 0.710172503040e12 * this.t95
                * this.t2175 - 0.14203450060800e14 * this.t1050 * this.t2169 + 0.13315734432000e14 * this.t673
                * this.t2163
                - 0.18932553319680e14 * this.t108 * this.t2163 - 0.3374786688000e13 * this.t1090 * this.t2178
                + 0.4302853027200e13 * this.t144
                * this.t2071 + 0.887715628800e12 * this.t1058 * this.t2223 + 0.1775431257600e13 * this.t1058
                * this.t2203 - 0.14849061427200e14
                * this.t1090 * this.t2223 - 0.117061401600e12 * this.t670 * this.t2175 + 0.1775431257600e13 * this.t95
                * this.t2243
                + 0.3160657843200e13 * this.t138 * this.t2264 - 0.282291609600e12 * this.t1184 * this.t2264
                + 0.570674332800e12 * this.t171
                * this.t2071 - 0.16172956800e11 * this.t132 * this.t2212;
        this.t2442 = this.t1664 * this.t167;
        this.t2461 = this.t556 * this.t7;
        this.t2464 = this.t149 * this.t62;
        this.t2469 =
            -0.16172956800e11 * this.t132 * this.t2206 - 0.146326752000e12 * this.t162 * this.t2149
                + 0.16705194105600e14 * this.t144
                * this.t2206 - 0.10652587545600e14 * this.t128 * this.t2212 - 0.1274668595200e13 * this.t1061
                * this.t2199 - 0.1274668595200e13
                * this.t1061 * this.t2442 - 0.887715628800e12 * this.t103 * this.t2212 - 0.887715628800e12 * this.t103
                * this.t2206
                - 0.461076295680e12 * this.t1184 * this.t2206 - 0.1436662656000e13 * this.t670 * this.t2149
                + 0.2370493382400e13 * this.t171
                * this.t2206 + 0.3149800908800e13 * this.t80 * this.t2442 + 0.1889880545280e13 * this.t80 * this.t2398
                - 0.3374786688000e13
                * this.t1074 * this.t2209 + 0.484323840e9 * this.t2461 * this.t195 + 0.5881075200e10 * this.t2464
                * this.t361 - 0.1775431257600e13
                * this.t128 * this.t2169;
        this.t2499 = this.t90 * this.t29;
        this.t2506 =
            -0.3374786688000e13 * this.t1090 * this.t2243 + 0.3160657843200e13 * this.t138 * this.t2169
                + 0.1775431257600e13 * this.t95
                * this.t2178 - 0.4499715584000e13 * this.t1090 * this.t2187 - 0.18162144000e11 * this.t91 * this.t2163
                - 0.461076295680e12
                * this.t1184 * this.t2212 - 0.14849061427200e14 * this.t1090 * this.t2149 + 0.8194298112000e13
                * this.t95 * this.t2223
                - 0.3374786688000e13 * this.t1074 * this.t2203 + 0.887715628800e12 * this.t1058 * this.t2149
                - 0.1585584000e10 * this.t1167
                * this.t2163 - 0.2663146886400e13 * this.t128 * this.t2071 + 0.17520703200e11 * this.t278 * this.t1709
                + 0.864864000e9 * this.t872
                * this.t264 + 0.864864000e9 * this.t2499 * this.t206 + 0.9386196019200e13 * this.t682 * this.t366
                + 0.18772392038400e14 * this.t676
                * this.t371;
        this.t2515 = this.t252 * this.t16;
        this.t2518 = this.t184 * this.t22;
        this.t2520 = this.t262 * this.t20;
        this.t2546 =
            0.1638859622400e13 * this.t95 * this.t386 - 0.15591514498560e14 * this.t1142 * this.t1709
                - 0.1712022998400e13 * this.t1153
                * this.t1709 + 0.595948953600e12 * this.t564 * this.t264 + 0.886828800e9 * this.t2515 * this.t230
                - 0.382400578560e12 * this.t2518
                + 0.960960000e9 * this.t2520 + 0.15682867200e11 * this.t149 * this.t1797 + 0.2522520e7 * this.t553
                * this.t1661 + 0.40360320e8
                * this.t543 * this.t195 - 0.6922639360e10 * this.t572 * this.t252 * this.t574 + 0.48510e5 * this.t68
                * ex - 0.76846049280e11 * this.t359
                * this.t19 * this.t31 + 0.646800e6 * this.t11 * ex - 0.6922639360e10 * this.t572 * this.t190
                - 0.337478668800e12 * this.t107 * this.t2172
                - 0.893923430400e12 * this.t72 * this.t1777 + 0.157199642600e12 * this.t174 * this.t1728;
        this.t2554 = 0.1e1 / this.t1249 / this.t1247 / a;
        this.t2559 = this.t1245 * this.t2554;
        this.t2562 = 0.1e1 / this.t1259 / this.t1257 / this.t1256;
    }

    /**
     * Partial derivative due to 12th order Earth potential zonal harmonics.
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
            derParUdeg12_5(final double a, final double ex, final double ey, final double ix, final double iy) {
        this.t2568 = this.t83 * this.t8;
        this.t2569 = this.t2568 * this.t98;
        this.t2572 = ey * this.t17;
        this.t2575 = ey * this.t15;
        this.t2578 = ey * this.t40;
        this.t2581 = this.t20 * this.t111;
        this.t2582 = this.t2581 * this.t45;
        this.t2585 = this.t20 * this.t97;
        this.t2586 = this.t2585 * this.t13;
        this.t2589 = ey * this.t16;
        this.t2594 = this.t42 * this.t97;
        this.t2595 = this.t2594 * this.t13;
        this.t2599 = this.t21 * this.t111 * this.t45;
        this.t2604 = this.t19 * this.t324;
        this.t2608 = ey * this.t13;
        this.t2609 = this.t139 * this.t2608;
        this.t2612 = this.t97 * this.t17;
        this.t2614 = this.t139 * this.t13;
        this.t2617 = ix * this.t8;
        this.t2618 = this.t2617 * this.t98;
        this.t2624 = this.t42 * this.t111;
        this.t2625 = this.t2624 * this.t157;
        this.t2628 =
            0.156828672000e12 * this.t863 * this.t2569 - 0.989937428480e12 * this.t2572 * this.t409 - 0.5765760000e10
                * this.t2575 * this.t421
                - 0.9104775680e10 * this.t2578 * this.t454 + 0.790164460800e12 * this.t72 * this.t2582
                + 0.194075481600e12 * this.t28 * this.t2586
                - 0.95777510400e11 * this.t2589 * this.t65 + 0.804531087360e12 * this.t2589 * this.t56
                - 0.158520648000e12 * this.t49 * this.t2595
                + 0.717142171200e12 * this.t107 * this.t2599 + 0.5568398035200e13 * this.t79 * this.t2599
                - 0.19510233600e11 * this.t72 * this.t2604
                * this.t157 - 0.16172956800e11 * this.t132 * this.t2609 + 0.31183028997120e14 * this.t2612 * this.t21
                * this.t2614 + 0.14529715200e11
                * this.t906 * this.t2618 + 0.2421619200e10 * this.t111 * this.t15 * this.t45 - 0.2969812285440e13
                * this.t79 * this.t2625;
        this.t2633 = this.t19 * this.t111;
        this.t2634 = this.t2633 * this.t45;
        this.t2637 = this.t8 * iy;
        this.t2638 = this.t96 * this.t2637;
        this.t2641 = this.t97 * this.t13;
        this.t2642 = this.t139 * this.t2641;
        this.t2645 = this.t43 * this.t98;
        this.t2646 = this.t74 * this.t2645;
        this.t2651 = this.t74 * this.t2637;
        this.t2654 = this.t139 * ey;
        this.t2659 = this.t163 * ey;
        this.t2668 = this.t536 * ey;
        this.t2675 =
            0.960960000e9 * this.t2575 * this.t20 + 0.111740428800e12 * this.t2589 * this.t21 - 0.70572902400e11
                * this.t149 * this.t2634
                - 0.282291609600e12 * this.t1184 * this.t2638 + 0.33410388211200e14 * this.t1107 * this.t2642
                - 0.32345913600e11 * this.t178
                * this.t2646 + 0.22273592140800e14 * this.t1107 * this.t2646 - 0.3424045996800e13 * this.t1153
                * this.t2651 - 0.908380083520e12
                * this.t1079 * this.t2654 - 0.9081072000e10 * this.t91 * this.t2654 - 0.8086478400e10 * this.t178
                * this.t2659 - 0.16951334400e11
                * this.t1159 * this.t2654 + 0.1257597140800e13 * this.t1098 * this.t2651 - 0.158520648000e12
                * this.t153 * this.t2654
                + 0.118362083840e12 * this.t95 * this.t2668 + 0.5568398035200e13 * this.t1107 * this.t2659
                - 0.443857814400e12 * this.t128
                * this.t2659;
        this.t2681 = this.t74 * iy;
        this.t2684 = this.t119 * iy;
        this.t2691 = this.t96 * iy;
        this.t2698 = this.t111 * this.t157;
        this.t2701 = this.t163 * this.t2608;
        this.t2704 = this.t97 * this.t45;
        this.t2705 = this.t139 * this.t2704;
        this.t2710 = this.t111 * this.t45;
        this.t2719 =
            0.95112388800e11 * this.t171 * this.t2659 - 0.3550862515200e13 * this.t1050 * this.t2659
                + 0.157199642600e12 * this.t175 * this.t2681
                - 0.287332531200e12 * this.t670 * this.t2684 + 0.4380175800e10 * this.t529 * this.t2681
                + 0.64691827200e11 * this.t278 * this.t2681
                + 0.478094780800e12 * this.t275 * this.t2691 + 0.5592608461440e13 * this.t673 * this.t2681
                - 0.29265350400e11 * this.t162
                * this.t2684 + 0.1877239203840e13 * this.t582 * this.t2698 - 0.1436662656000e13 * this.t670
                * this.t2701 - 0.3374786688000e13
                * this.t1074 * this.t2705 + 0.1877239203840e13 * this.t582 * this.t2684 + 0.216216000e9 * this.t90
                * this.t2710 + 0.242161920e9
                * this.t556 * this.t2654 - 0.282291609600e12 * this.t1184 * this.t2646 + 0.570674332800e12 * this.t171
                * this.t2642;
        this.t2722 = this.t98 * this.t8;
        this.t2723 = this.t74 * this.t2722;
        this.t2728 = this.t139 * this.t2710;
        this.t2733 = this.t21 * ey;
        this.t2734 = this.t2733 * this.t13;
        this.t2737 = this.t2594 * this.t45;
        this.t2740 = this.t22 * ey;
        this.t2741 = this.t2740 * this.t13;
        this.t2744 = this.t21 * this.t97;
        this.t2745 = this.t2744 * this.t45;
        this.t2748 = this.t42 * ey;
        this.t2749 = this.t2748 * this.t13;
        this.t2752 = this.t2585 * this.t45;
        this.t2755 = this.t20 * ey;
        this.t2756 = this.t2755 * this.t13;
        this.t2761 = this.t2744 * this.t13;
        this.t2764 = this.t19 * this.t97;
        this.t2765 = this.t2764 * this.t13;
        this.t2768 = this.t2581 * this.t157;
        this.t2773 =
            -0.18932553319680e14 * this.t108 * this.t2651 - 0.211718707200e12 * this.t178 * this.t2723
                + 0.190224777600e12 * this.t1116
                * this.t2609 - 0.3374786688000e13 * this.t1090 * this.t2728 + 0.4302853027200e13 * this.t144
                * this.t2642 + 0.2219289072000e13
                * this.t41 * this.t2734 - 0.3550862515200e13 * this.t127 * this.t2737 - 0.3155425553280e13 * this.t18
                * this.t2741
                + 0.478094780800e12 * this.t18 * this.t2745 - 0.39630162000e11 * this.t123 * this.t2749
                + 0.790164460800e12 * this.t54 * this.t2752
                + 0.64691827200e11 * this.t131 * this.t2756 - 0.4454718428160e13 * this.t79 * this.t2741
                + 0.11185216922880e14 * this.t94
                * this.t2761 - 0.16951334400e11 * this.t35 * this.t2765 + 0.1638859622400e13 * this.t94 * this.t2768
                - 0.893923430400e12 * this.t72
                * this.t2749;
        this.t2780 = this.t2764 * this.t45;
        this.t2800 = this.t64 * this.t97;
        this.t2801 = this.t2800 * this.t45;
        this.t2805 = ix * this.t185 * this.t189;
        this.t2808 = this.t12 * ey;
        this.t2809 = this.t2808 * this.t13;
        this.t2812 = this.t84 * this.t8;
        this.t2813 = this.t2812 * this.t98;
        this.t2816 = this.t83 * this.t43;
        this.t2817 = this.t2816 * this.t112;
        this.t2822 =
            0.177543125760e12 * this.t127 * this.t2768 + 0.157199642600e12 * this.t174 * this.t2734 - 0.5390985600e10
                * this.t131 * this.t2780
                - 0.227095020880e12 * this.t430 * this.t2741 + 0.3277719244800e13 * this.t94 * this.t2734
                - 0.36419102720e11 * this.t94 * this.t2604
                * this.t188 - 0.337478668800e12 * this.t107 * this.t2625 + 0.15682867200e11 * this.t149 * this.t2684
                + 0.216216000e9 * this.t90
                * this.t2659 + 0.40360320e8 * this.t543 * this.t2681 + 0.6918912000e10 * this.t262 * this.t2618
                + 0.139054755840e12 * this.t267
                * this.t2801 + 0.14898723840e11 * this.t184 * this.t2805 + 0.6918912000e10 * this.t198 * this.t2809
                + 0.59594895360e11 * this.t209
                * this.t2813 + 0.744936192000e12 * this.t219 * this.t2817 - 0.2949947320320e13 * this.t72 * this.t2595;
        this.t2825 = this.t2624 * this.t45;
        this.t2834 = this.t8 * this.t15;
        this.t2839 = this.t111 * this.t16;
        this.t2840 = this.t163 * this.t45;
        this.t2843 = this.t43 * this.t16;
        this.t2844 = this.t119 * this.t98;
        this.t2847 = this.t74 * this.t326;
        this.t2853 = this.t22 * this.t97 * this.t13;
        this.t2862 = this.t74 * this.t98;
        this.t2865 = this.t8 * this.t27;
        this.t2868 =
            0.22498577920e11 * this.t2572 * this.t231 - 0.443857814400e12 * this.t127 * this.t2825 - 0.3441605207040e13
                * this.t2578 * this.t306
                - 0.4454718428160e13 * this.t2572 * this.t24 + 0.345807221760e12 * this.t149 * this.t2586
                + 0.9686476800e10 * this.t2834 * this.t2691
                + 0.184504320e9 * this.t1041 * this.t2681 + 0.62078016000e11 * this.t2839 * this.t2840
                + 0.49662412800e11 * this.t2843 * this.t2844
                + 0.7094630400e10 * this.t832 * this.t2847 + 0.5197171499520e13 * this.t2572 * this.t243
                - 0.15591514498560e14 * this.t79
                * this.t2853 - 0.1712022998400e13 * this.t54 * this.t2595 + 0.17520703200e11 * this.t131 * this.t2586
                + 0.95112388800e11 * this.t54
                * this.t2582 + 0.9686476800e10 * this.t835 * this.t2862 + 0.31365734400e11 * this.t2865 * this.t2684;
        this.t2870 = this.t43 * this.t27;
        this.t2871 = this.t96 * this.t98;
        this.t2874 = this.t97 * this.t27;
        this.t2875 = this.t163 * this.t13;
        this.t2878 = this.t97 * this.t15;
        this.t2881 = this.t97 * this.t16;
        this.t2882 = this.t536 * this.t13;
        this.t2885 = this.t74 * this.t112;
        this.t2888 = this.t111 * this.t27;
        this.t2889 = this.t139 * this.t45;
        this.t2892 = this.t8 * this.t16;
        this.t2893 = this.t85 * iy;
        this.t2896 = this.t324 * this.t16;
        this.t2897 = this.t139 * this.t157;
        this.t2900 = this.t156 * this.t16;
        this.t2901 = this.t96 * this.t112;
        this.t2910 = ey * this.t27;
        this.t2913 = this.t118 * this.t8;
        this.t2914 = this.t2913 * this.t98;
        this.t2923 =
            0.104552448000e12 * this.t2870 * this.t2871 + 0.78414336000e11 * this.t2874 * this.t2875 + 0.14529715200e11
                * this.t2878 * this.t2614
                + 0.24831206400e11 * this.t2881 * this.t2882 + 0.31365734400e11 * this.t849 * this.t2885
                + 0.78414336000e11 * this.t2888 * this.t2889
                + 0.7094630400e10 * this.t2892 * this.t2893 + 0.24831206400e11 * this.t2896 * this.t2897
                + 0.49662412800e11 * this.t2900 * this.t2901
                + 0.347636889600e12 * this.t641 * this.t2681 - 0.9081072000e10 * this.t90 * this.t2765 - 0.792792000e9
                * this.t59 * this.t2765
                + 0.109780070400e12 * this.t2910 * this.t293 + 0.446961715200e12 * this.t508 * this.t2914
                - 0.76846049280e11 * this.t2910 * this.t32
                + 0.546286540800e12 * this.t2578 * this.t427 - 0.908380083520e12 * this.t18 * this.t2853;
        this.t2924 = this.t20 * this.t324;
        this.t2930 = this.t118 * this.t43;
        this.t2931 = this.t2930 * this.t112;
        this.t2934 = this.t31 * this.t111;
        this.t2935 = this.t2934 * this.t157;
        this.t2938 = ix * this.t156;
        this.t2939 = this.t2938 * this.t326;
        this.t2952 = this.t324 * this.t157;
        this.t2961 = this.t324 * this.t188;
        this.t2970 =
            0.118362083840e12 * this.t94 * this.t2924 * this.t157 + 0.628798570400e12 * this.t41 * this.t2761
                + 0.208582133760e12 * this.t313
                * this.t2931 + 0.208582133760e12 * this.t318 * this.t2935 + 0.148987238400e12 * this.t323 * this.t2939
                - 0.224985779200e12 * this.t79
                * this.t42 * this.t324 * this.t157 + 0.6657867216000e13 * this.t127 * this.t2761 - 0.3550862515200e13
                * this.t94 * this.t2825
                - 0.9466276659840e13 * this.t107 * this.t2853 + 0.980179200e9 * this.t149 * this.t2952
                + 0.5227622400e10 * this.t2910 * this.t536
                + 0.242161920e9 * this.t556 * this.t2641 + 0.644143500e9 * this.t69 * this.t2755 + 0.456894197760e12
                * this.t578 * this.t2961
                - 0.2383795814400e13 * this.t561 * this.t2704 + 0.886828800e9 * this.t2589 * this.t550 + 0.144144000e9
                * this.t59 * this.t2704;
        this.t3008 =
            0.121080960e9 * this.t532 * this.t2608 + 0.1470268800e10 * this.t28 * this.t2698 + 0.15682867200e11
                * this.t149 * this.t2698
                + 0.92252160e8 * this.t556 * this.t2608 + 0.21956014080e11 * this.t359 * this.t118 * iy + 0.98560e5
                * ey + 0.4380175800e10
                * this.t285 * this.t2756 + 0.63408259200e11 * this.t49 * this.t2752 + 0.10394342999040e14 * this.t79
                * this.t2745 - 0.70572902400e11
                * this.t28 * this.t2780 + 0.92252160e8 * this.t556 * this.t2681 + 0.144144000e9 * this.t59 * this.t2691
                - 0.768768000e9 * this.t601
                * this.t2681 + 0.121080960e9 * this.t532 * this.t2681 + 0.1470268800e10 * this.t28 * this.t2684
                + 0.4843238400e10 * this.t35 * this.t2691
                + 0.595948953600e12 * this.t564 * this.t2691;
        this.t3019 = this.t252 * this.t254;
        this.t3025 = this.t2633 * this.t157;
        this.t3034 = this.t19 * ey;
        this.t3035 = this.t3034 * this.t13;
        this.t3049 =
            -0.456894197760e12 * this.t614 * this.t2681 + 0.2522520e7 * this.t553 * this.t2681 + 0.3547315200e10
                * this.t72 * this.t2893
                - 0.357569372160e12 * this.t636 * this.t2684 + 0.456894197760e12 * this.t578 * this.t2893
                - 0.6922639360e10 * this.t572 * this.t3019
                * iy + 0.109780070400e12 * this.t149 * this.t2756 - 0.287332531200e12 * this.t72 * this.t3025
                + 0.5568398035200e13 * this.t107
                * this.t2745 - 0.29265350400e11 * this.t54 * this.t3025 + 0.5592608461440e13 * this.t127 * this.t2734
                - 0.8475667200e10 * this.t90
                * this.t3035 - 0.5765760000e10 * this.t35 * this.t3035 - 0.295905209600e12 * this.t41 * this.t2737
                - 0.3027024000e10 * this.t59
                * this.t3035 - 0.198198000e9 * this.t69 * this.t3035 + 0.89994311680e11 * this.t79 * this.t2924
                * this.t188;
        this.t3067 = this.t31 * ey;
        this.t3068 = this.t3067 * this.t13;
        this.t3071 = this.t230 * ey;
        this.t3072 = this.t3071 * this.t13;
        this.t3075 = this.t64 * ey;
        this.t3076 = this.t3075 * this.t13;
        this.t3079 = this.t83 * this.t156;
        this.t3080 = this.t3079 * this.t326;
        this.t3083 = this.t31 * this.t97;
        this.t3084 = this.t3083 * this.t45;
        this.t3087 = this.t12 * this.t97;
        this.t3088 = this.t3087 * this.t45;
        this.t3093 = this.t12 * this.t111;
        this.t3094 = this.t3093 * this.t157;
        this.t3097 = this.t12 * this.t324;
        this.t3098 = this.t3097 * this.t188;
        this.t3101 =
            -0.6883210414080e13 * this.t94 * this.t2737 + 0.172903610880e12 * this.t28 * this.t2756 - 0.153692098560e12
                * this.t149 * this.t2780
                - 0.1474973660160e13 * this.t54 * this.t2749 + 0.980179200e9 * this.t149 * this.t2668 + 0.2018016000e10
                * this.t35 * this.t2659
                + 0.121080960e9 * this.t532 * this.t2654 + 0.10090080e8 * this.t543 * this.t2654 + 0.109780070400e12
                * this.t330 * this.t3068
                + 0.14898723840e11 * this.t337 * this.t3072 + 0.148987238400e12 * this.t342 * this.t3076
                + 0.139054755840e12 * this.t347 * this.t3080
                + 0.744936192000e12 * this.t352 * this.t3084 + 0.219560140800e12 * this.t364 * this.t3088
                + 0.219560140800e12 * this.t369
                * this.t2569 + 0.446961715200e12 * this.t374 * this.t3094 + 0.59594895360e11 * this.t379 * this.t3098;
        this.t3102 = ix * this.t43;
        this.t3103 = this.t3102 * this.t112;
        this.t3138 =
            0.109780070400e12 * this.t384 * this.t3103 + 0.3277719244800e13 * this.t2578 * this.t390 - 0.8086478400e10
                * this.t28 * this.t2634
                - 0.2383795814400e13 * this.t561 * this.t2691 - 0.62731468800e11 * this.t598 * this.t2691
                - 0.99324825600e11 * this.t587 * this.t2681
                + 0.3426706483200e13 * this.t608 * this.t2691 + 0.2018016000e10 * this.t90 * this.t2691
                + 0.13069056000e11 * this.t648 * this.t2681
                - 0.3198259384320e13 * this.t611 * this.t2684 - 0.62731468800e11 * this.t598 * this.t2704
                - 0.3198259384320e13 * this.t611
                * this.t2698 + 0.13069056000e11 * this.t648 * this.t2608 + 0.1229144716800e13 * this.t72 * this.t2733
                + 0.595948953600e12 * this.t564
                * this.t2704 - 0.99324825600e11 * this.t587 * this.t2608 - 0.357569372160e12 * this.t636 * this.t2698;
        this.t3144 = ey * this.t14;
        this.t3147 = this.t913 * ey;
        this.t3181 =
            -0.6922639360e10 * this.t572 * this.t186 * this.t630 + 0.92252160e8 * this.t3144 * this.t139
                + 0.5568398035200e13 * this.t79 * this.t3147
                - 0.3772791422400e13 * this.t41 * this.t2740 - 0.181621440e9 * this.t543 * this.t3034 + 0.10090080e8
                * this.t543 * this.t2641
                + 0.121080960e9 * this.t532 * this.t2641 + 0.3426706483200e13 * this.t608 * this.t2704
                - 0.198649651200e12 * this.t604 * this.t2961
                - 0.768768000e9 * this.t601 * this.t2608 + 0.2421619200e10 * this.t2575 * this.t163 + 0.2018016000e10
                * this.t35 * this.t2710
                + 0.347636889600e12 * this.t641 * this.t2608 - 0.456894197760e12 * this.t614 * this.t2608
                + 0.804531087360e12 * this.t2839 * this.t20
                * this.t45 + 0.109780070400e12 * this.t2874 * this.t20 * this.t13 - 0.95777510400e11 * this.t2896
                * this.t19 * this.t157;
        this.t3185 = this.t97 * this.t40;
        this.t3189 = this.t186 * this.t40;
        this.t3196 = this.t324 * this.t40;
        this.t3221 = this.t324 * this.t17;
        this.t3225 = this.t111 * this.t17;
    }

    /**
     * Partial derivative due to 12th order Earth potential zonal harmonics.
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
            derParUdeg12_6(final double a, final double ex, final double ey, final double ix, final double iy) {
        this.t3229 =
            -0.4454718428160e13 * this.t2612 * this.t22 * this.t13 + 0.3277719244800e13 * this.t3185 * this.t21
                * this.t13 - 0.9104775680e10
                * this.t3189 * this.t19 * this.t188 - 0.893923430400e12 * this.t2881 * this.t42 * this.t13
                + 0.546286540800e12 * this.t3196 * this.t20 * this.t157
                + 0.1070014374000e13 * this.t49 * this.t2733 - 0.140165625600e12 * this.t131 * this.t2748
                - 0.13873860e8 * this.t553 * this.t3034
                + 0.397416286540e12 * this.t430 * this.t3147 + 0.21283891200e11 * this.t214 * this.t84 * iy
                + 0.40360320e8 * this.t543 * this.t2608
                + 0.2522520e7 * this.t553 * this.t2608 + 0.3547315200e10 * this.t72 * this.t2961 + 0.4843238400e10
                * this.t35 * this.t2704
                + 0.2018016000e10 * this.t90 * this.t2704 - 0.989937428480e12 * this.t3221 * this.t42 * this.t157
                + 0.5197171499520e13 * this.t3225
                * this.t21 * this.t45;
        this.t3231 = this.t111 * this.t40;
        this.t3271 =
            -0.3441605207040e13 * this.t3231 * this.t42 * this.t45 - 0.76846049280e11 * this.t2888 * this.t19
                * this.t45 - 0.5765760000e10
                * this.t2878 * this.t19 * this.t13 + 0.22498577920e11 * this.t186 * this.t17 * this.t20 * this.t188
                + 0.11043989436480e14 * this.t107 * this.t3147
                - 0.298679320940e12 * this.t174 * this.t2740 - 0.8388912692160e13 * this.t127 * this.t2740
                - 0.4261035018240e13 * this.t94
                * this.t2740 - 0.10950439500e11 * this.t285 * this.t2748 + 0.84214094250e11 * this.t123 * this.t2733
                + 0.19070251200e11 * this.t90
                * this.t2755 + 0.2396832197760e13 * this.t54 * this.t2733 - 0.230630400e9 * this.t556 * this.t3034
                + 0.8324316000e10 * this.t59
                * this.t2755 + 0.4996090459360e13 * this.t18 * this.t3147 - 0.164670105600e12 * this.t149 * this.t2748
                + 0.10090080000e11 * this.t35
                * this.t2755;
        this.t3284 = this.t43 * this.t112;
        this.t3285 = this.t96 * this.t3284;
        this.t3290 = this.t156 * this.t326;
        this.t3291 = this.t74 * this.t3290;
        this.t3294 = this.t96 * this.t2722;
        this.t3297 = this.t74 * this.t3284;
        this.t3310 = this.t139 * this.t2698;
        this.t3315 =
            -0.316989953280e12 * this.t28 * this.t2748 + 0.15375360e8 * this.t193 * ix * iy + 0.2306304000e10
                * this.t204 * this.t83 * iy
                + 0.887715628800e12 * this.t1058 * this.t2701 - 0.20649631242240e14 * this.t1050 * this.t2609
                - 0.1274668595200e13 * this.t1061
                * this.t3285 + 0.190224777600e12 * this.t1116 * this.t2723 - 0.254933719040e12 * this.t1061
                * this.t3291 - 0.2873325312000e13
                * this.t670 * this.t3294 + 0.8194298112000e13 * this.t95 * this.t3297 - 0.146326752000e12 * this.t162
                * this.t3297
                + 0.2370493382400e13 * this.t171 * this.t2723 + 0.887715628800e12 * this.t1058 * this.t3297
                - 0.1436662656000e13 * this.t670
                * this.t3297 + 0.629960181760e12 * this.t80 * this.t3291 - 0.764801157120e12 * this.t1061 * this.t3310
                + 0.31183028997120e14
                * this.t1107 * this.t2609;
        this.t3328 = this.t156 * this.t112;
        this.t3329 = this.t74 * this.t3328;
        this.t3338 = this.t536 * this.t2608;
        this.t3351 = this.t119 * this.t2637;
        this.t3354 = this.t96 * this.t2645;
        this.t3359 =
            -0.14849061427200e14 * this.t1090 * this.t2701 - 0.29698122854400e14 * this.t1090 * this.t3294
                - 0.32345913600e11 * this.t178
                * this.t2638 - 0.48518870400e11 * this.t178 * this.t2642 - 0.1349914675200e13 * this.t1090 * this.t3329
                - 0.1687393344000e13
                * this.t1074 * this.t3297 - 0.292653504000e12 * this.t162 * this.t2705 + 0.1775431257600e13
                * this.t1058 * this.t3294
                - 0.254933719040e12 * this.t1061 * this.t3338 - 0.1585584000e10 * this.t1167 * this.t2651
                - 0.893923430400e12 * this.t2589 * this.t51
                - 0.15591514498560e14 * this.t1142 * this.t2654 + 0.717142171200e12 * this.t144 * this.t2659
                - 0.7795757249280e13 * this.t107
                * this.t2741 + 0.710172503040e12 * this.t95 * this.t3351 + 0.2367241676800e13 * this.t95 * this.t3354
                - 0.5899894640640e13 * this.t73
                * this.t2651;
        this.t3378 = this.t163 * this.t2641;
        this.t3387 = this.t163 * this.t2704;
        this.t3396 =
            -0.292653504000e12 * this.t670 * this.t2728 - 0.570674332800e12 * this.t153 * this.t2681
                - 0.893923430400e12 * this.t73 * this.t2681
                + 0.89994311680e11 * this.t80 * this.t2893 - 0.8475667200e10 * this.t91 * this.t2681
                + 0.710172503040e12 * this.t95 * this.t3329
                - 0.14203450060800e14 * this.t1050 * this.t2638 + 0.13315734432000e14 * this.t673 * this.t2651
                - 0.1349914675200e13 * this.t1090
                * this.t3351 + 0.1775431257600e13 * this.t95 * this.t3378 + 0.3160657843200e13 * this.t138 * this.t2646
                + 0.380449555200e12
                * this.t171 * this.t2646 - 0.117061401600e12 * this.t670 * this.t3329 + 0.3149800908800e13 * this.t80
                * this.t3387
                + 0.629960181760e12 * this.t80 * this.t3338 + 0.31183028997120e14 * this.t1107 * this.t2723
                - 0.2873325312000e13 * this.t670
                * this.t2705;
        this.t3402 = this.t119 * this.t2722;
        this.t3433 =
            -0.461076295680e12 * this.t1184 * this.t2723 - 0.10652587545600e14 * this.t128 * this.t2609
                - 0.764801157120e12 * this.t1061
                * this.t3402 - 0.1274668595200e13 * this.t1061 * this.t3387 - 0.1712022998400e13 * this.t1153
                * this.t2654 + 0.790164460800e12
                * this.t138 * this.t2659 - 0.19510233600e11 * this.t670 * this.t2668 - 0.70572902400e11 * this.t178
                * this.t2691 + 0.3277719244800e13
                * this.t135 * this.t2681 + 0.109780070400e12 * this.t150 * this.t2681 - 0.29698122854400e14
                * this.t1090 * this.t2705
                - 0.14849061427200e14 * this.t1090 * this.t3297 - 0.461076295680e12 * this.t1184 * this.t2609
                + 0.1775431257600e13 * this.t1058
                * this.t2705 - 0.887715628800e12 * this.t103 * this.t2609 - 0.3374786688000e13 * this.t1074
                * this.t3294 + 0.4827186524160e13
                * this.t138 * this.t2723;
        this.t3469 =
            -0.1687393344000e13 * this.t1074 * this.t2701 - 0.1775431257600e13 * this.t128 * this.t2646
                + 0.16388596224000e14 * this.t95
                * this.t3294 + 0.3149800908800e13 * this.t80 * this.t3285 + 0.1889880545280e13 * this.t80 * this.t3402
                - 0.10652587545600e14
                * this.t128 * this.t2723 - 0.292653504000e12 * this.t162 * this.t3294 + 0.8194298112000e13 * this.t95
                * this.t2701
                - 0.211718707200e12 * this.t178 * this.t2609 - 0.887715628800e12 * this.t103 * this.t2723
                + 0.194075481600e12 * this.t1189
                * this.t2654 + 0.17520703200e11 * this.t278 * this.t2654 - 0.9466276659840e13 * this.t108 * this.t2654
                - 0.70572902400e11
                * this.t1184 * this.t2659 - 0.31183028997120e14 * this.t1142 * this.t2651 - 0.317041296000e12
                * this.t153 * this.t2651
                + 0.1655413760e10 * this.t253 * this.t254 * iy;
        this.t3506 =
            -0.1816760167040e13 * this.t1079 * this.t2651 + 0.380449555200e12 * this.t171 * this.t2638
                + 0.628798570400e12 * this.t1098
                * this.t2654 - 0.224985779200e12 * this.t1090 * this.t2668 - 0.792792000e9 * this.t1167 * this.t2654
                + 0.11185216922880e14
                * this.t135 * this.t2654 + 0.6657867216000e13 * this.t673 * this.t2654 - 0.14203450060800e14
                * this.t1050 * this.t2646
                - 0.390204672000e12 * this.t670 * this.t3354 - 0.1787846860800e13 * this.t807 * this.t3103
                - 0.3575693721600e13 * this.t810
                * this.t3088 + 0.9594778152960e13 * this.t816 * this.t2914 - 0.188194406400e12 * this.t826 * this.t2809
                + 0.10280119449600e14
                * this.t829 * this.t2618 - 0.3155425553280e13 * this.t1079 * this.t2681 + 0.3160657843200e13
                * this.t138 * this.t2638
                - 0.4499715584000e13 * this.t1090 * this.t3354;
        this.t3541 =
            -0.18162144000e11 * this.t91 * this.t2651 + 0.1775431257600e13 * this.t95 * this.t2728 - 0.423437414400e12
                * this.t1184 * this.t2642
                + 0.16705194105600e14 * this.t144 * this.t2723 + 0.4827186524160e13 * this.t138 * this.t2609
                + 0.16705194105600e14 * this.t144
                * this.t2609 - 0.16172956800e11 * this.t132 * this.t2723 + 0.691614443520e12 * this.t150 * this.t2651
                - 0.21305175091200e14
                * this.t1050 * this.t2642 + 0.22273592140800e14 * this.t1107 * this.t2638 + 0.1889880545280e13
                * this.t80 * this.t3310
                - 0.2663146886400e13 * this.t128 * this.t2642 - 0.1775431257600e13 * this.t128 * this.t2638
                - 0.3374786688000e13 * this.t1090
                * this.t3378 + 0.1434284342400e13 * this.t275 * this.t2723 + 0.16388596224000e14 * this.t95
                * this.t2705 - 0.146326752000e12
                * this.t162 * this.t2701;
        this.t3559 = this.t2617 * iy;
        this.t3568 = this.t3102 * this.t98;
        this.t3582 =
            0.2370493382400e13 * this.t171 * this.t2609 - 0.31982593843200e14 * this.t729 * this.t2569
                + 0.3198259384320e13 * this.t735
                * this.t3076 - 0.1390547558400e13 * this.t738 * this.t3076 - 0.872252559360e12 * this.t741 * this.t2931
                + 0.15991296921600e14
                * this.t744 * this.t2817 + 0.10280119449600e14 * this.t748 * this.t2809 + 0.1609062174720e13
                * this.t138 * this.t2691 + 0.20180160e8
                * this.t869 * this.t3559 + 0.19603584000e11 * this.t863 * this.t2816 * this.t98 + 0.14702688000e11
                * this.t751 * this.t3093 * this.t45
                + 0.8072064000e10 * this.t906 * this.t3568 + 0.1787846860800e13 * this.t875 * this.t2618
                + 0.9386196019200e13 * this.t878
                * this.t3068 - 0.249215016960e12 * this.t881 * this.t3098 + 0.484323840e9 * this.t711 * this.t3559
                + 0.5881075200e10 * this.t857
                * this.t2913 * iy;
        this.t3583 = this.t3087 * this.t13;
        this.t3596 = this.t2568 * iy;
        this.t3619 =
            0.1297296000e10 * this.t872 * this.t3583 + 0.1787846860800e13 * this.t769 * this.t2809 - 0.581501706240e12
                * this.t772 * this.t3080
                - 0.15991296921600e14 * this.t726 * this.t3068 - 0.198649651200e12 * this.t604 * this.t2893
                + 0.864864000e9 * this.t795 * this.t3568
                + 0.864864000e9 * this.t903 * this.t3596 + 0.18772392038400e14 * this.t676 * this.t3088
                + 0.18772392038400e14 * this.t679
                * this.t2569 + 0.9386196019200e13 * this.t682 * this.t3103 - 0.1390547558400e13 * this.t685
                * this.t2939 - 0.570674332800e12
                * this.t49 * this.t2749 + 0.7351344000e10 * this.t786 * this.t3068 + 0.14702688000e11 * this.t789
                * this.t2569 + 0.14702688000e11
                * this.t792 * this.t3088 + 0.6054048000e10 * this.t795 * this.t2618 + 0.78414336000e11 * this.t798
                * this.t3103;
        this.t3658 =
            0.156828672000e12 * this.t751 * this.t3088 + 0.74493619200e11 * this.t757 * this.t3094 + 0.124156032000e12
                * this.t760 * this.t2817
                + 0.5881075200e10 * this.t798 * this.t2938 * this.t112 + 0.345807221760e12 * this.t150 * this.t2654
                - 0.2949947320320e13 * this.t73
                * this.t2654 + 0.78414336000e11 * this.t866 * this.t3068 + 0.6054048000e10 * this.t872 * this.t2809
                + 0.14529715200e11 * this.t775
                * this.t2809 + 0.24831206400e11 * this.t778 * this.t3076 + 0.74493619200e11 * this.t717 * this.t2914
                + 0.24831206400e11 * this.t691
                * this.t2939 + 0.432432000e9 * this.t763 * this.t2809 + 0.7351344000e10 * this.t700 * this.t3103
                + 0.432432000e9 * this.t703 * this.t2618
                - 0.3550862515200e13 * this.t128 * this.t2691 + 0.2219289072000e13 * this.t1098 * this.t2681;
        this.t3694 =
            0.10394342999040e14 * this.t1107 * this.t2691 + 0.177543125760e12 * this.t1058 * this.t2684
                - 0.1474973660160e13 * this.t1153
                * this.t2681 - 0.198198000e9 * this.t1139 * this.t2681 - 0.4454718428160e13 * this.t1142 * this.t2681
                - 0.5390985600e10 * this.t132
                * this.t2691 + 0.124156032000e12 * this.t714 * this.t3084 - 0.337478668800e12 * this.t1074 * this.t2684
                - 0.36419102720e11
                * this.t1061 * this.t2893 - 0.6883210414080e13 * this.t1050 * this.t2691 - 0.2969812285440e13
                * this.t1090 * this.t2684
                - 0.6952737792000e13 * this.t981 * this.t2817 - 0.62303754240e11 * this.t984 * this.t3072
                - 0.1787846860800e13 * this.t987
                * this.t3068 - 0.7795757249280e13 * this.t108 * this.t2681 - 0.39630162000e11 * this.t124 * this.t2681
                + 0.14702688000e11 * this.t866
                * this.t3083 * this.t13;
        this.t3730 =
            0.8072064000e10 * this.t766 * this.t3596 - 0.227095020880e12 * this.t1194 * this.t2681 + 0.63408259200e11
                * this.t1116 * this.t2691
                + 0.3198259384320e13 * this.t1006 * this.t2939 - 0.7151387443200e13 * this.t1009 * this.t2618
                - 0.581501706240e12 * this.t1012
                * this.t2801 - 0.4171642675200e13 * this.t1015 * this.t3094 - 0.249215016960e12 * this.t1018
                * this.t2813 - 0.6952737792000e13
                * this.t948 * this.t3084 - 0.62303754240e11 * this.t887 * this.t2805 - 0.4171642675200e13 * this.t890
                * this.t2914
                - 0.31982593843200e14 * this.t896 * this.t3088 - 0.15991296921600e14 * this.t900 * this.t3103
                - 0.5765760000e10 * this.t1159
                * this.t2681 - 0.7151387443200e13 * this.t993 * this.t2809 - 0.3575693721600e13 * this.t996
                * this.t2569 - 0.188194406400e12
                * this.t928 * this.t2618;
        this.t3751 = this.t8 * this.t40;
        this.t3767 = this.t8 * this.t17;
        this.t3771 = this.t43 * this.t40;
        this.t3775 =
            0.15991296921600e14 * this.t939 * this.t3084 + 0.9594778152960e13 * this.t942 * this.t3094
                - 0.872252559360e12 * this.t945
                * this.t2935 - 0.153692098560e12 * this.t1184 * this.t2691 - 0.295905209600e12 * this.t103 * this.t2691
                + 0.790164460800e12
                * this.t171 * this.t2691 + 0.12108096000e11 * this.t775 * this.t3583 + 0.242161920e9 * this.t688
                * this.t3559 + 0.1638859622400e13
                * this.t95 * this.t2684 - 0.3027024000e10 * this.t1167 * this.t2681 + 0.6555438489600e13 * this.t3751
                * this.t21 * this.t2681
                + 0.4827186524160e13 * this.t2881 * this.t20 * this.t2614 + 0.219560140800e12 * this.t2865 * this.t20
                * this.t2681
                + 0.629960181760e12 * this.t3221 * this.t20 * this.t2897 - 0.1787846860800e13 * this.t2892 * this.t42
                * this.t2681
                + 0.179988623360e12 * this.t3767 * this.t20 * this.t2893 + 0.10925730816000e14 * this.t3771 * this.t20
                * this.t2871;
        this.t3797 = this.t43 * this.t17;
        this.t3816 = this.t156 * this.t40;
        this.t3820 = this.t156 * this.t17;
        this.t3830 =
            0.172903610880e12 * this.t1189 * this.t2681 + 0.5568398035200e13 * this.t144 * this.t2691
                - 0.11531520000e11 * this.t2834 * this.t19
                * this.t2681 + 0.3277719244800e13 * this.t3751 * this.t20 * this.t2684 + 0.3218124349440e13
                * this.t2843 * this.t20 * this.t2862
                + 0.1574900454400e13 * this.t3225 * this.t20 * this.t2840 + 0.629960181760e12 * this.t2612 * this.t20
                * this.t2882
                + 0.20788685998080e14 * this.t3797 * this.t21 * this.t2862 - 0.1436662656000e13 * this.t2839 * this.t19
                * this.t2889
                - 0.574665062400e12 * this.t2892 * this.t19 * this.t2684 + 0.8194298112000e13 * this.t3231 * this.t20
                * this.t2889
                - 0.13766420828160e14 * this.t3751 * this.t42 * this.t2691 - 0.20649631242240e14 * this.t3185
                * this.t42 * this.t2614
                - 0.509867438080e12 * this.t3816 * this.t19 * this.t2901 + 0.1259920363520e13 * this.t3820 * this.t20
                * this.t2901
                + 0.1259920363520e13 * this.t3797 * this.t20 * this.t2844 + 0.8194298112000e13 * this.t3185 * this.t20
                * this.t2875;
        this.t3883 =
            -0.14849061427200e14 * this.t3225 * this.t42 * this.t2889 - 0.5939624570880e13 * this.t3820 * this.t42
                * this.t2885
                - 0.461076295680e12 * this.t2874 * this.t19 * this.t2614 + 0.179988623360e12 * this.t185 * this.t17
                * this.t20 * this.t2847
                - 0.254933719040e12 * this.t3196 * this.t19 * this.t2897 + 0.20788685998080e14 * this.t3767 * this.t21
                * this.t2691
                - 0.14849061427200e14 * this.t2612 * this.t42 * this.t2875 - 0.19798748569600e14 * this.t3797
                * this.t42 * this.t2871
                - 0.72838205440e11 * this.t185 * this.t40 * this.t19 * this.t2847 - 0.1915550208000e13 * this.t2843
                * this.t19 * this.t2871
                + 0.3277719244800e13 * this.t3816 * this.t20 * this.t2885 - 0.72838205440e11 * this.t3751 * this.t19
                * this.t2893 + 0.48510e5 * this.t68
                * ey - 0.13766420828160e14 * this.t3771 * this.t42 * this.t2862 - 0.307384197120e12 * this.t2865
                * this.t19 * this.t2691
                - 0.307384197120e12 * this.t2870 * this.t19 * this.t2862 - 0.509867438080e12 * this.t3771 * this.t19
                * this.t2844;
    }

    /**
     * Partial derivative due to 12th order Earth potential zonal harmonics.
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
            derParUdeg12_7(final double a, final double ex, final double ey, final double ix, final double iy) {
        this.t3929 =
            -0.637334297600e12 * this.t3231 * this.t19 * this.t2840 - 0.8909436856320e13 * this.t3767 * this.t22
                * this.t2681 - 0.254933719040e12
                * this.t3185 * this.t19 * this.t2882 - 0.5939624570880e13 * this.t3767 * this.t42 * this.t2684
                - 0.574665062400e12 * this.t2900 * this.t19
                * this.t2885 + 0.887040e6 * this.t9 * ey - 0.1436662656000e13 * this.t2881 * this.t19 * this.t2875
                + 0.3218124349440e13 * this.t2892
                * this.t20 * this.t2691 + 0.646800e6 * this.t11 * ey + 0.1552320e7 * this.t10 * ey - 0.423783360e9
                * this.t532 * this.t3034
                + 0.886828800e9 * this.t186 * this.t16 * this.t188 + 0.5227622400e10 * this.t324 * this.t27 * this.t157
                + 0.92252160e8 * this.t97 * this.t14
                * this.t13 - 0.382400578560e12 * this.t2578 * this.t22 - 0.15247232000e11 * this.t2910 * this.t42
                + 0.494968714240e12 * this.t2572
                * this.t913;
        this.t3966 =
            -0.23063040e8 * this.t3144 * this.t19 + 0.21956014080e11 * this.t2888 * this.t157 + 0.1609062174720e13
                * this.t72 * this.t2752
                + 0.15375360e8 * this.t3144 * this.t13 + 0.1655413760e10 * this.t3189 * this.t630 + 0.2306304000e10
                * this.t2878 * this.t45
                + 0.21283891200e11 * this.t2896 * this.t188 - 0.117061401600e12 * this.t670 * this.t3351
                - 0.292653504000e12 * this.t670 * this.t3378
                + 0.4740986764800e13 * this.t138 * this.t2642 - 0.20649631242240e14 * this.t1050 * this.t2723
                + 0.1434284342400e13 * this.t275
                * this.t2609 + 0.35041406400e11 * this.t278 * this.t2651 + 0.388150963200e12 * this.t1189 * this.t2651
                + 0.2868568684800e13
                * this.t144 * this.t2638 + 0.22370433845760e14 * this.t135 * this.t2651 - 0.33902668800e11 * this.t1159
                * this.t2651
                + 0.2868568684800e13 * this.t144 * this.t2646;
        this.t3981 = this.t3097 * this.t326;
        this.t3984 = this.t2938 * this.t157;
        this.t3987 = this.t3093 * this.t112;
        this.t3990 = this.t2930 * this.t45;
        this.t3995 = this.t3079 * this.t157;
        this.t4003 = iy * ey * ex;
        this.t4006 = this.t14 * this.t29;
        this.t4011 = this.t2617 * this.t13;
        this.t4014 = this.t2808 * iy;
        this.t4019 = this.t3075 * iy;
        this.t4022 =
            -0.249215016960e12 * this.t772 * this.t3981 + 0.24831206400e11 * this.t757 * this.t3984 + 0.74493619200e11
                * this.t760 * this.t3987
                - 0.47973890764800e14 * this.t878 * this.t3990 + 0.15991296921600e14 * this.t945 * this.t3990
                - 0.19189556305920e14 * this.t676
                * this.t3995 + 0.6396518768640e13 * this.t881 * this.t3995 - 0.950476800e9 * this.t648 * ix
                - 0.99324825600e11 * this.t587
                * this.t4003 - 0.2306304000e10 * this.t4006 * this.t118 + 0.2306304000e10 * this.t1031 * this.t83
                + 0.432432000e9 * this.t763 * this.t4011
                + 0.14529715200e11 * this.t766 * this.t4014 + 0.1787846860800e13 * this.t769 * this.t4011
                - 0.1390547558400e13 * this.t999
                * this.t4019;
        this.t4025 = this.t167 * ix;
        this.t4028 = this.t3067 * iy;
        this.t4033 = this.t2816 * this.t45;
        this.t4036 = this.t2568 * this.t13;
        this.t4041 = this.t2800 * this.t98;
        this.t4048 = this.t2812 * this.t13;
        this.t4053 = this.t3087 * this.t98;
        this.t4058 = this.t2913 * this.t13;
        this.t4061 =
            -0.872252559360e12 * this.t1012 * this.t3990 - 0.1580328921600e13 * this.t1189 * this.t4025
                + 0.9386196019200e13 * this.t933
                * this.t4028 - 0.7151387443200e13 * this.t936 * this.t4014 + 0.15991296921600e14 * this.t939
                * this.t4033 - 0.3575693721600e13
                * this.t987 * this.t4036 - 0.7151387443200e13 * this.t993 * this.t4011 - 0.581501706240e12 * this.t1018
                * this.t4041
                - 0.15991296921600e14 * this.t978 * this.t4028 - 0.4171642675200e13 * this.t981 * this.t3987
                - 0.249215016960e12 * this.t984
                * this.t4048 - 0.31982593843200e14 * this.t726 * this.t4036 - 0.31982593843200e14 * this.t729
                * this.t4053 + 0.1787846860800e13
                * this.t732 * this.t4014 + 0.9594778152960e13 * this.t735 * this.t4058;
        this.t4065 = this.t7 * this.t43 * this.t45;
        this.t4078 = this.t2934 * this.t112;
        this.t4085 = this.t430 * this.t21;
        this.t4093 = ex * this.t97 * this.t98;
        this.t4100 = this.t117 * ey * iy;
        this.t4103 =
            -0.146326752000e12 * this.t54 * this.t83 * this.t4065 - 0.6952737792000e13 * this.t948 * this.t4033
                + 0.3198259384320e13 * this.t942
                * this.t3984 - 0.581501706240e12 * this.t945 * this.t3995 - 0.3575693721600e13 * this.t996 * this.t4053
                - 0.4171642675200e13
                * this.t738 * this.t4058 - 0.872252559360e12 * this.t741 * this.t4078 + 0.9594778152960e13 * this.t744
                * this.t3987
                + 0.1362570125280e13 * this.t968 * this.t4011 - 0.1135475104400e13 * this.t4085 * this.t4011
                + 0.3575693721600e13 * this.t1039
                * this.t4011 - 0.2681770291200e13 * this.t138 * this.t4011 - 0.6883210414080e13 * this.t1050
                * this.t4093 - 0.1434284342400e13
                * this.t1098 * this.t4025 - 0.574665062400e12 * this.t72 * this.t64 * this.t4100;
        this.t4122 = this.t3083 * this.t98;
        this.t4125 = this.t18 * this.t42;
        this.t4132 = this.t123 * this.t20;
        this.t4135 = this.t389 * this.t83;
        this.t4138 =
            0.790164460800e12 * this.t162 * this.t4025 - 0.194075481600e12 * this.t801 * this.t4011 + 0.129383654400e12
                * this.t132 * this.t4011
                + 0.26728310568960e14 * this.t970 * this.t4011 - 0.22273592140800e14 * this.t1107 * this.t4011
                + 0.3075072000e10 * this.t2271
                * this.t4014 - 0.46930980096000e14 * this.t769 * this.t4033 + 0.18772392038400e14 * this.t1015
                * this.t4033 + 0.124156032000e12
                * this.t714 * this.t4033 + 0.124156032000e12 * this.t717 * this.t4122 + 0.956189561600e12 * this.t4125
                * this.t4025
                + 0.10280119449600e14 * this.t748 * this.t4011 + 0.158520648000e12 * this.t918 * this.t4011
                - 0.118890486000e12 * this.t4132
                * this.t4011 - 0.15777127766400e14 * this.t18 * this.t4135;
        this.t4141 = this.t30 * this.t118;
        this.t4145 = this.t83 * this.t185 * this.t188;
        this.t4149 = ex * this.t111 * this.t112;
        this.t4177 =
            0.105859353600e12 * this.t90 * this.t4141 - 0.74493619200e11 * this.t374 * this.t4145 + 0.15682867200e11
                * this.t149 * this.t4149
                + 0.139054755840e12 * this.t209 * this.t4041 + 0.8877156288000e13 * this.t1153 * this.t4025
                - 0.5326293772800e13 * this.t1058
                * this.t4025 + 0.18932553319680e14 * this.t1034 * this.t4011 - 0.15777127766400e14 * this.t275
                * this.t4011
                - 0.41120477798400e14 * this.t990 * this.t4053 + 0.432432000e9 * this.t720 * this.t4014
                + 0.9386196019200e13 * this.t676 * this.t4025
                + 0.18772392038400e14 * this.t679 * this.t4053 + 0.78414336000e11 * this.t751 * this.t4025
                + 0.24831206400e11 * this.t754
                * this.t4019 - 0.6054048000e10 * this.t59 * this.t12 * this.t4003;
        this.t4183 = this.t118 * this.t156 * this.t157;
        this.t4188 = this.t601 * this.t81;
        this.t4195 = this.t29 * this.t8 * this.t13;
        this.t4200 = this.t81 * ey * iy;
        this.t4208 = this.t81 * this.t97 * this.t98;
        this.t4221 = this.t12 * this.t186 * this.t189;
        this.t4224 =
            -0.153692098560e12 * this.t1184 * this.t4093 + 0.172903610880e12 * this.t1189 * this.t4003
                + 0.1744505118720e13 * this.t948
                * this.t4183 + 0.17381844480000e14 * this.t987 * this.t3990 + 0.376388812800e12 * this.t4188
                * this.t4028 + 0.794598604800e12
                * this.t2288 * this.t4014 - 0.146326752000e12 * this.t54 * this.t118 * this.t4195 - 0.141145804800e12
                * this.t28 * this.t31 * this.t4200
                - 0.16951334400e11 * this.t90 * this.t12 * this.t4003 - 0.195102336000e12 * this.t54 * this.t31
                * this.t4208 - 0.11531520000e11 * this.t35
                * this.t12 * this.t4003 - 0.2731432704000e13 * this.t138 * this.t3984 + 0.1092573081600e13 * this.t1061
                * this.t3984
                - 0.595948953600e12 * this.t875 * this.t4014 + 0.83071672320e11 * this.t685 * this.t4221;
        this.t4228 = this.t641 * this.t81;
        this.t4241 = this.t532 * this.t81;
        this.t4246 = this.t7 * this.t8 * this.t13;
        this.t4251 = this.t62 * this.t8 * this.t13;
        this.t4259 = this.t7 * this.t156 * this.t157;
        this.t4264 = ex * this.t324 * this.t326;
        this.t4273 =
            0.13905475584000e14 * this.t813 * this.t4041 - 0.41120477798400e14 * this.t4228 * this.t4028
                + 0.27413651865600e14 * this.t729
                * this.t4028 + 0.95947781529600e14 * this.t993 * this.t4033 - 0.47973890764800e14 * this.t942
                * this.t4033 + 0.38379112611840e14
                * this.t1009 * this.t3987 - 0.19189556305920e14 * this.t1006 * this.t3987 - 0.8072064000e10
                * this.t4241 * this.t4028
                - 0.211718707200e12 * this.t28 * this.t83 * this.t4246 - 0.254933719040e12 * this.t94 * this.t84
                * this.t4251 - 0.574665062400e12
                * this.t72 * this.t12 * this.t4149 - 0.254933719040e12 * this.t94 * this.t83 * this.t4259
                - 0.72838205440e11 * this.t94 * this.t12 * this.t4264
                - 0.1915550208000e13 * this.t72 * this.t31 * this.t4208 - 0.58530700800e11 * this.t54 * this.t12
                * this.t4149;
        this.t4276 = this.t82 * ey * iy;
        this.t4281 = this.t81 * this.t111 * this.t112;
        this.t4289 = this.t127 * this.t19;
        this.t4294 = this.t174 * this.t42;
        this.t4299 = this.t579 * ix;
        this.t4310 = this.t107 * this.t20;
        this.t4315 =
            -0.72838205440e11 * this.t94 * this.t230 * this.t4276 - 0.509867438080e12 * this.t94 * this.t31
                * this.t4281 - 0.141145804800e12
                * this.t28 * this.t12 * this.t4093 - 0.295905209600e12 * this.t171 * this.t3984 + 0.118362083840e12
                * this.t4289 * this.t3984
                - 0.785998213000e12 * this.t916 * this.t4011 + 0.628798570400e12 * this.t4294 * this.t4011
                + 0.13110876979200e14 * this.t1050
                * this.t4011 + 0.45523878400e11 * this.t670 * this.t4299 - 0.188194406400e12 * this.t951 * this.t4014
                - 0.6952737792000e13
                * this.t890 * this.t4122 + 0.10280119449600e14 * this.t893 * this.t4014 + 0.674957337600e12 * this.t128
                * this.t3984
                - 0.337478668800e12 * this.t4310 * this.t3984 - 0.11096445360000e14 * this.t847 * this.t4011;
        this.t4323 = this.t50 * this.t83;
        this.t4339 = this.t30 * this.t83;
        this.t4346 = this.t35 * this.t117;
        this.t4353 =
            0.8877156288000e13 * this.t103 * this.t4011 - 0.5765760000e10 * this.t35 * this.t4011 + 0.2282697331200e13
                * this.t131 * this.t4323
                - 0.456894197760e12 * this.t614 * this.t4003 - 0.1436662656000e13 * this.t72 * this.t118 * this.t4195
                - 0.15991296921600e14
                * this.t896 * this.t4025 + 0.6054048000e10 * this.t903 * this.t4014 + 0.8086478400e10 * this.t1167
                * this.t4025 - 0.16388596224000e14
                * this.t909 * this.t4011 - 0.70572902400e11 * this.t28 * this.t4339 + 0.4380175800e10 * this.t529
                * this.t4003 - 0.19372953600e11
                * this.t711 * this.t4053 - 0.94097203200e11 * this.t4346 * this.t4019 - 0.313657344000e12 * this.t766
                * this.t4122
                - 0.235243008000e12 * this.t1277 * this.t4058;
        this.t4384 =
            0.7351344000e10 * this.t694 * this.t4028 - 0.3218124349440e13 * this.t150 * this.t4025 + 0.1609062174720e13
                * this.t670 * this.t4025
                - 0.12108096000e11 * this.t1317 * this.t4036 - 0.4767591628800e13 * this.t975 * this.t4053
                + 0.2383795814400e13 * this.t807
                * this.t4053 - 0.46930980096000e14 * this.t1993 * this.t4058 + 0.18772392038400e14 * this.t948
                * this.t4058 + 0.373822525440e12
                * this.t1015 * this.t4145 + 0.478094780800e12 * this.t275 * this.t4093 + 0.345807221760e12 * this.t178
                * this.t4011 - 0.198198000e9
                * this.t1139 * this.t4003 - 0.4454718428160e13 * this.t1142 * this.t4003 - 0.337478668800e12
                * this.t1074 * this.t4149
                + 0.64691827200e11 * this.t278 * this.t4003;
        this.t4390 = this.t63 * this.t84;
        this.t4395 = this.t292 * this.t83;
        this.t4401 = this.t230 * this.t97 * this.t98;
        this.t4404 = this.t3071 * iy;
        this.t4412 = this.t453 * this.t254;
        this.t4415 = this.t23 * this.t83;
        this.t4418 = this.t598 * this.t117;
        this.t4425 =
            0.5592608461440e13 * this.t673 * this.t4003 - 0.3027024000e10 * this.t59 * this.t4011 + 0.39020467200e11
                * this.t28 * this.t4390
                - 0.198198000e9 * this.t69 * this.t4011 - 0.329340211200e12 * this.t35 * this.t4395 - 0.9225216000e10
                * this.t2349 * this.t4028
                - 0.198649651200e12 * this.t214 * this.t4401 - 0.170271129600e12 * this.t1812 * this.t4404
                - 0.1191897907200e13 * this.t369
                * this.t4078 - 0.134991467520e12 * this.t94 * this.t229 * this.t254 + 0.44997155840e11 * this.t79
                * this.t4412 + 0.18932553319680e14
                * this.t41 * this.t4415 + 0.2860554977280e13 * this.t4418 * this.t4019 - 0.38379112611840e14
                * this.t933 * this.t4041
                + 0.12793037537280e14 * this.t741 * this.t4041;
        this.t4426 = this.t582 * this.t82;
        this.t4455 = this.t292 * ix;
        this.t4458 =
            -0.5482730373120e13 * this.t4426 * this.t4404 + 0.1827576791040e13 * this.t1018 * this.t4404
                - 0.1474973660160e13 * this.t1153
                * this.t4003 + 0.17208026035200e14 * this.t73 * this.t4025 - 0.10324815621120e14 * this.t95
                * this.t4025 - 0.518710832640e12
                * this.t1048 * this.t4011 + 0.78414336000e11 * this.t857 * this.t4028 + 0.156828672000e12 * this.t863
                * this.t4053
                + 0.156828672000e12 * this.t866 * this.t4036 + 0.744936192000e12 * this.t352 * this.t4033
                + 0.109780070400e12 * this.t359
                * this.t4028 + 0.109780070400e12 * this.t364 * this.t4025 + 0.219560140800e12 * this.t369 * this.t4053
                + 0.148987238400e12
                * this.t374 * this.t3984 + 0.64691827200e11 * this.t131 * this.t4455;
        this.t4463 = this.t305 * this.t83;
        this.t4495 =
            0.4380175800e10 * this.t285 * this.t4455 - 0.6883210414080e13 * this.t94 * this.t4463 + 0.790164460800e12
                * this.t54 * this.t4141
                + 0.177543125760e12 * this.t1058 * this.t4149 - 0.287332531200e12 * this.t670 * this.t4149
                + 0.230538147840e12 * this.t1159
                * this.t4025 + 0.2860554977280e13 * this.t928 * this.t3987 + 0.7151387443200e13 * this.t826
                * this.t4033 + 0.26138112000e11
                * this.t598 * this.t4011 + 0.6396518768640e13 * this.t561 * this.t3984 - 0.3198259384320e13 * this.t578
                * this.t3984
                + 0.94097203200e11 * this.t601 * this.t4025 - 0.166143344640e12 * this.t17 * this.t31 * this.t81
                * this.t324 * this.t326
                + 0.59594895360e11 * this.t347 * this.t3981 - 0.5390985600e10 * this.t132 * this.t4093;
        this.t4507 = this.t117 * this.t97 * this.t98;
        this.t4512 = this.t29 * this.t43 * this.t45;
        this.t4539 =
            -0.1436662656000e13 * this.t72 * this.t83 * this.t4065 - 0.307384197120e12 * this.t149 * this.t31
                * this.t4200 - 0.307384197120e12
                * this.t149 * this.t12 * this.t4093 - 0.509867438080e12 * this.t94 * this.t64 * this.t4507
                - 0.637334297600e12 * this.t94 * this.t118 * this.t4512
                + 0.15991296921600e14 * this.t816 * this.t4122 + 0.3198259384320e13 * this.t819 * this.t4019
                - 0.1787846860800e13 * this.t810
                * this.t4025 - 0.10781971200e11 * this.t131 * this.t12 * this.t4093 + 0.6054048000e10 * this.t872
                * this.t4011 + 0.18772392038400e14
                * this.t878 * this.t4036 - 0.62303754240e11 * this.t881 * this.t4299 - 0.396396000e9 * this.t69
                * this.t12 * this.t4003 - 0.10781971200e11
                * this.t131 * this.t31 * this.t4200 - 0.16172956800e11 * this.t131 * this.t83 * this.t4246;
        this.t4543 = this.t604 * this.t252;
        this.t4545 = this.t574 * ey * iy;
        this.t4556 = this.t2162 * this.t158;
        this.t4575 =
            0.157199642600e12 * this.t175 * this.t4003 + 0.83071672320e11 * this.t4543 * this.t4545 + 0.564583219200e12
                * this.t2002 * this.t4036
                + 0.27413651865600e14 * this.t900 * this.t4053 - 0.29059430400e11 * this.t2461 * this.t4036
                - 0.99324825600e11 * this.t2464
                * this.t4048 + 0.629960181760e12 * this.t80 * this.t4556 - 0.3155425553280e13 * this.t1079 * this.t4003
                - 0.29265350400e11
                * this.t162 * this.t4149 + 0.63408259200e11 * this.t1116 * this.t4093 - 0.36419102720e11 * this.t1061
                * this.t4264
                + 0.1638859622400e13 * this.t95 * this.t4149 - 0.22054032000e11 * this.t2499 * this.t4058
                - 0.29405376000e11 * this.t903 * this.t4122
                - 0.22054032000e11 * this.t872 * this.t4033;
    }

    /**
     * Partial derivative due to 12th order Earth potential zonal harmonics.
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
            derParUdeg12_8(final double a, final double ex, final double ey, final double ix, final double iy) {
        this.t4609 =
            -0.8072064000e10 * this.t688 * this.t4053 + 0.2219289072000e13 * this.t1098 * this.t4003 + 0.89994311680e11
                * this.t80 * this.t4264
                - 0.99324825600e11 * this.t751 * this.t3995 - 0.198649651200e12 * this.t863 * this.t4078
                + 0.790164460800e12 * this.t171 * this.t4093
                + 0.5899894640640e13 * this.t958 * this.t4011 - 0.4424920980480e13 * this.t171 * this.t4011
                - 0.70572902400e11 * this.t178
                * this.t4093 - 0.62303754240e11 * this.t823 * this.t4404 - 0.188194406400e12 * this.t826 * this.t4011
                + 0.14529715200e11 * this.t775
                * this.t4011 - 0.461076295680e12 * this.t149 * this.t83 * this.t4246 + 0.74493619200e11 * this.t778
                * this.t4058 - 0.1787846860800e13
                * this.t813 * this.t4028 + 0.5568398035200e13 * this.t144 * this.t4093;
        this.t4620 = this.t41 * this.t20;
        this.t4627 = this.t149 * this.t82;
        this.t4646 =
            0.109780070400e12 * this.t150 * this.t4003 + 0.11531520000e11 * this.t855 * this.t4011 + 0.739763024000e12
                * this.t153 * this.t4025
                - 0.443857814400e12 * this.t4620 * this.t4025 - 0.94097203200e11 * this.t906 * this.t3987
                - 0.235243008000e12 * this.t775
                * this.t4033 - 0.28378521600e11 * this.t4627 * this.t4404 + 0.10394342999040e14 * this.t1107
                * this.t4093 + 0.14702688000e11
                * this.t786 * this.t4036 + 0.14702688000e11 * this.t789 * this.t4053 + 0.7351344000e10 * this.t792
                * this.t4025 + 0.6054048000e10
                * this.t923 * this.t4011 + 0.95947781529600e14 * this.t2025 * this.t4058 - 0.47973890764800e14
                * this.t939 * this.t4058
                + 0.127930375372800e15 * this.t936 * this.t4122;
        this.t4651 = this.t556 * this.t81;
        this.t4659 = this.t31 * this.t324 * this.t326;
        this.t4662 = this.t636 * this.t82;
        this.t4682 =
            -0.63965187686400e14 * this.t744 * this.t4122 - 0.864864000e9 * this.t1626 * this.t4036 - 0.19372953600e11
                * this.t4651 * this.t4028
                - 0.7151387443200e13 * this.t1957 * this.t4036 + 0.3575693721600e13 * this.t810 * this.t4036
                + 0.996860067840e12 * this.t981
                * this.t4659 + 0.1986496512000e13 * this.t4662 * this.t4404 + 0.3277719244800e13 * this.t135
                * this.t4003 + 0.1470268800e10
                * this.t28 * this.t2183 - 0.80720640e8 * this.t11 * this.t12 * this.t4003 + 0.2741365186560e13
                * this.t1228 * this.t4011
                - 0.2284470988800e13 * this.t608 * this.t4011 - 0.1738184448000e13 * this.t1222 * this.t4011
                + 0.1390547558400e13 * this.t561
                * this.t4011 - 0.4843238400e10 * this.t556 * this.t4025;
        this.t4688 = this.t420 * this.t83;
        this.t4699 = this.t64 * this.t111 * this.t112;
        this.t4707 = this.t84 * this.t43 * this.t45;
        this.t4720 =
            -0.2018016000e10 * this.t532 * this.t4025 - 0.39207168000e11 * this.t1224 * this.t4011 + 0.219560140800e12
                * this.t149 * this.t4688
                - 0.1390547558400e13 * this.t40 * this.t84 * this.t4251 - 0.2774772e7 * this.t1025 * ix
                + 0.6952737792000e13 * this.t1933
                * this.t4048 + 0.2093406142464e13 * this.t890 * this.t4699 - 0.31365734400e11 * this.t1902 * this.t167
                - 0.144144000e9 * this.t543
                * this.t2198 + 0.1744505118720e13 * this.t738 * this.t4707 + 0.6952737792000e13 * this.t810
                * this.t3995 + 0.996860067840e12
                * this.t999 * this.t4401 + 0.15375360e8 * this.t925 * ix - 0.5482730373120e13 * this.t682 * this.t3981
                + 0.1827576791040e13
                * this.t887 * this.t3981;
        this.t4727 = this.t55 * this.t83;
        this.t4730 = this.t63 * this.t118;
        this.t4754 = this.t23 * ix;
        this.t4757 = this.t420 * ix;
        this.t4760 =
            0.23837958144000e14 * this.t884 * this.t4053 - 0.14302774886400e14 * this.t682 * this.t4053 - 0.141261120e9
                * this.t543 * ix
                + 0.63408259200e11 * this.t49 * this.t4727 - 0.287332531200e12 * this.t72 * this.t4730
                + 0.6396518768640e13 * this.t1012 * this.t4048
                + 0.396396000e9 * this.t911 * this.t4011 - 0.570674332800e12 * this.t153 * this.t4003
                - 0.893923430400e12 * this.t73 * this.t4003
                - 0.397299302400e12 * this.t40 * this.t12 * this.t4264 - 0.184504320e9 * this.t9 * this.t12
                * this.t4003 - 0.348901023744e12 * this.t17
                * this.t64 * this.t117 * this.t111 * this.t112 + 0.1609062174720e13 * this.t72 * this.t4727
                - 0.227095020880e12 * this.t430 * this.t4754
                - 0.3027024000e10 * this.t59 * this.t4757;
        this.t4765 = this.t389 * ix;
        this.t4780 = this.t561 * this.t117;
        this.t4788 = this.t254 * this.t8 * this.t13;
        this.t4797 =
            0.14898723840e11 * this.t379 * this.t4299 + 0.3277719244800e13 * this.t94 * this.t4765 - 0.5390985600e10
                * this.t131 * this.t4339
                - 0.576576000e9 * this.t869 * this.t4053 - 0.8475667200e10 * this.t91 * this.t4003 - 0.295905209600e12
                * this.t103 * this.t4093
                - 0.78414336000e11 * this.t2520 * this.t4014 + 0.52276224000e11 * this.t928 * this.t4014
                + 0.38379112611840e14 * this.t4780
                * this.t4019 - 0.19189556305920e14 * this.t816 * this.t4019 + 0.13905475584000e14 * this.t996
                * this.t4078 + 0.373822525440e12
                * this.t2022 * this.t4788 + 0.7151387443200e13 * this.t1979 * this.t4058 - 0.3476368896000e13
                * this.t2324 * this.t4014
                + 0.2781095116800e13 * this.t1009 * this.t4014;
        this.t4798 = this.t648 * this.t81;
        this.t4823 = ex * this.t186 * this.t189;
        this.t4831 = this.t408 * this.t118;
        this.t4834 =
            -0.4767591628800e13 * this.t4798 * this.t4028 + 0.2383795814400e13 * this.t996 * this.t4028
                - 0.19189556305920e14 * this.t2005
                * this.t4048 + 0.5482730373120e13 * this.t2518 * this.t4014 - 0.4568941977600e13 * this.t829
                * this.t4014 - 0.8821612800e10
                * this.t795 * this.t3987 - 0.1191897907200e13 * this.t359 * this.t4041 - 0.785998213000e12 * this.t123
                * this.t4135
                + 0.628798570400e12 * this.t174 * this.t4323 + 0.3575693721600e13 * this.t149 * this.t4323
                - 0.2681770291200e13 * this.t72
                * this.t4395 - 0.13845278720e11 * this.t17 * this.t12 * this.t4823 - 0.125462937600e12 * this.t27
                * this.t31 * this.t4200
                - 0.5765760000e10 * this.t35 * this.t4757 - 0.2969812285440e13 * this.t79 * this.t4831;
        this.t4836 = this.t50 * ix;
        this.t4841 = this.t426 * this.t84;
        this.t4864 = this.t408 * this.t84;
        this.t4869 =
            -0.39630162000e11 * this.t123 * this.t4836 - 0.95777510400e11 * this.t72 * this.t3984 - 0.337478668800e12
                * this.t107 * this.t4841
                + 0.396396000e9 * this.t553 * this.t4688 - 0.27963042307200e14 * this.t54 * this.t4135
                + 0.22370433845760e14 * this.t127 * this.t4323
                - 0.11096445360000e14 * this.t49 * this.t4135 + 0.8877156288000e13 * this.t41 * this.t4323
                - 0.194075481600e12 * this.t59
                * this.t4395 + 0.129383654400e12 * this.t131 * this.t4688 - 0.13140527400e11 * this.t69 * this.t4395
                + 0.8760351600e10 * this.t285
                * this.t4688 - 0.9755116800e10 * this.t54 * this.t3984 + 0.5939624570880e13 * this.t94 * this.t4864
                - 0.2969812285440e13 * this.t79
                * this.t4841;
        this.t4895 = this.t55 * this.t118;
        this.t4910 =
            0.158520648000e12 * this.t285 * this.t4323 - 0.118890486000e12 * this.t123 * this.t4395 - 0.715138744320e12
                * this.t16 * this.t64
                * this.t4100 - 0.715138744320e12 * this.t16 * this.t12 * this.t4149 + 0.1537536000e10 * this.t1220
                * this.t4011 - 0.1787846860800e13
                * this.t16 * this.t83 * this.t4065 - 0.62303754240e11 * this.t17 * this.t83 * this.t7 * this.t185
                * this.t188 - 0.2781095116800e13 * this.t40 * this.t64
                * this.t4507 + 0.674957337600e12 * this.t127 * this.t4864 - 0.1580328921600e13 * this.t28 * this.t4895
                - 0.1712022998400e13
                * this.t49 * this.t4395 + 0.230538147840e12 * this.t35 * this.t4141 - 0.9104775680e10 * this.t94
                * this.t4299 + 0.11531520000e11
                * this.t556 * this.t4688 - 0.2695492800e10 * this.t131 * this.t4025 - 0.2731432704000e13 * this.t72
                * this.t4841;
        this.t4944 = this.t305 * this.t118;
        this.t4947 =
            0.1092573081600e13 * this.t94 * this.t4390 - 0.126816518400e12 * this.t131 * this.t4895 + 0.63408259200e11
                * this.t49 * this.t4141
                + 0.1362570125280e13 * this.t174 * this.t4415 - 0.1135475104400e13 * this.t430 * this.t4135
                + 0.6054048000e10 * this.t543
                * this.t4688 - 0.4454718428160e13 * this.t79 * this.t4754 - 0.49662412800e11 * this.t40 * this.t254
                * this.t228 + 0.8307167232e10
                * this.t604 * this.t631 * ix + 0.248312064000e12 * this.t636 * this.t4299 - 0.10280119449600e14
                * this.t641 * this.t4025
                + 0.6853412966400e13 * this.t611 * this.t4025 + 0.456894197760e12 * this.t578 * this.t4264
                - 0.2383795814400e13 * this.t561
                * this.t4093 + 0.20788685998080e14 * this.t79 * this.t4944;
        this.t4948 = this.t242 * this.t118;
        this.t4974 = this.t27 * this.t228;
        this.t4983 =
            -0.31183028997120e14 * this.t94 * this.t4948 + 0.5592608461440e13 * this.t127 * this.t4765
                - 0.13845278720e11 * this.t17 * this.t574
                * this.t252 * ey * iy + 0.8877156288000e13 * this.t54 * this.t4944 - 0.5326293772800e13 * this.t127
                * this.t4895
                - 0.295905209600e12 * this.t54 * this.t4841 + 0.118362083840e12 * this.t127 * this.t4390
                - 0.16705194105600e14 * this.t127
                * this.t4948 + 0.11136796070400e14 * this.t107 * this.t4944 - 0.417164267520e12 * this.t508
                * this.t4699 - 0.347636889600e12
                * this.t352 * this.t4183 - 0.21283891200e11 * this.t4974 * this.t254 + 0.5959489536000e13 * this.t587
                * this.t4025
                - 0.3575693721600e13 * this.t582 * this.t4025 + 0.157199642600e12 * this.t174 * this.t4765;
        this.t5015 =
            -0.893923430400e12 * this.t72 * this.t4836 - 0.337478668800e12 * this.t107 * this.t4831 - 0.198198000e9
                * this.t69 * this.t4757
                + 0.8086478400e10 * this.t59 * this.t4141 + 0.383110041600e12 * this.t149 * this.t4390
                - 0.3218124349440e13 * this.t149 * this.t4895
                + 0.1609062174720e13 * this.t72 * this.t4141 + 0.228447098880e12 * this.t572 * this.t4299
                - 0.3128732006400e13 * this.t564
                * this.t3984 + 0.1251492802560e13 * this.t604 * this.t3984 - 0.144144000e9 * this.t543 * this.t4025
                + 0.6918912000e10 * this.t198
                * this.t4011 + 0.3426706483200e13 * this.t608 * this.t2168 + 0.1655413760e10 * this.t184 * this.t1688
                + 0.15375360e8 * this.t193
                * this.t75;
        this.t5022 = this.t228 * this.t84;
        this.t5048 =
            -0.99324825600e11 * this.t587 * this.t2162 + 0.1877239203840e13 * this.t582 * this.t2183
                + 0.347636889600e12 * this.t641 * this.t2162
                - 0.198649651200e12 * this.t604 * this.t5022 + 0.121080960e9 * this.t532 * this.t2162 + 0.92252160e8
                * this.t556 * this.t2162
                - 0.35286451200e11 * this.t28 * this.t2198 - 0.3198259384320e13 * this.t611 * this.t2183
                - 0.456894197760e12 * this.t614 * this.t2162
                + 0.2306304000e10 * this.t262 * this.t99 - 0.8475667200e10 * this.t90 * this.t4011 + 0.16951334400e11
                * this.t532 * this.t4688
                + 0.45523878400e11 * this.t72 * this.t4412 - 0.58530700800e11 * this.t54 * this.t64 * this.t4100
                + 0.476759162880e12 * this.t598
                * this.t3984;
        this.t5063 = this.t21 * ix;
        this.t5091 =
            0.397299302400e12 * this.t1211 * this.t4011 - 0.297974476800e12 * this.t564 * this.t4011
                + 0.109780070400e12 * this.t149 * this.t4455
                + 0.744936192000e12 * this.t508 * this.t4122 - 0.3547315200e10 * this.t149 * this.t4299
                - 0.76846049280e11 * this.t149 * this.t2198
                - 0.298679320940e12 * this.t804 * this.t5063 - 0.9755116800e10 * this.t54 * this.t2228
                - 0.7795757249280e13 * this.t107 * this.t4754
                - 0.1390547558400e13 * this.t40 * this.t83 * this.t4259 - 0.166143344640e12 * this.t17 * this.t230
                * this.t82 * this.t97 * this.t98
                - 0.2781095116800e13 * this.t40 * this.t31 * this.t4281 - 0.62303754240e11 * this.t17 * this.t254
                * this.t228 * this.t8 * this.t13
                - 0.1787846860800e13 * this.t16 * this.t118 * this.t4195 + 0.1470268800e10 * this.t28 * this.t4149;
        this.t5107 = this.t228 * this.t254;
        this.t5124 =
            0.144144000e9 * this.t59 * this.t4093 + 0.13069056000e11 * this.t648 * this.t4003 + 0.347636889600e12
                * this.t641 * this.t4003
                - 0.1537536000e10 * this.t15 * this.t12 * this.t4003 - 0.1191897907200e13 * this.t648 * this.t4025
                + 0.595948953600e12 * this.t636
                * this.t4025 - 0.39207168000e11 * this.t1224 * this.t2194 + 0.228447098880e12 * this.t572 * this.t5107
                - 0.685341296640e12
                * this.t582 * this.t5107 + 0.476759162880e12 * this.t598 * this.t2228 + 0.1537536000e10 * this.t1220
                * this.t2194 + 0.144144000e9
                * this.t59 * this.t2168 + 0.4843238400e10 * this.t35 * this.t2168 - 0.62731468800e11 * this.t598
                * this.t2168 + 0.13069056000e11
                * this.t648 * this.t2162;
        this.t5134 = this.t543 * this.t81;
        this.t5137 = this.t68 * ix;
        this.t5140 = this.t42 * ix;
        this.t5143 = this.t20 * ix;
        this.t5146 = ix * this.t19;
        this.t5153 = this.t22 * ix;
        this.t5162 =
            -0.768768000e9 * this.t601 * this.t2162 - 0.357569372160e12 * this.t636 * this.t2183 + 0.456894197760e12
                * this.t578 * this.t5022
                - 0.198649651200e12 * this.t857 * this.t4041 - 0.576576000e9 * this.t5134 * this.t4028 + 0.45405360e8
                * this.t5137 * this.t19
                + 0.3195776263680e13 * this.t49 * this.t5140 - 0.12713500800e11 * this.t543 * this.t5143
                + 0.12713500800e11 * this.t59 * this.t5146
                - 0.4162158000e10 * this.t553 * this.t5143 + 0.4162158000e10 * this.t69 * this.t5146
                + 0.1912002892800e13 * this.t72 * this.t5153
                - 0.1912002892800e13 * this.t94 * this.t5063 + 0.247005158400e12 * this.t90 * this.t5140
                - 0.247005158400e12 * this.t28 * this.t5143;
        this.t5163 = this.t913 * ix;
        this.t5184 = this.t242 * this.t83;
        this.t5201 =
            -0.7494135689040e13 * this.t174 * this.t5163 + 0.7494135689040e13 * this.t430 * this.t5153
                + 0.26138112000e11 * this.t598
                * this.t2194 + 0.5899894640640e13 * this.t28 * this.t4323 - 0.4424920980480e13 * this.t54 * this.t4395
                + 0.172903610880e12 * this.t28
                * this.t4455 + 0.1877239203840e13 * this.t582 * this.t4149 - 0.198649651200e12 * this.t604 * this.t4264
                + 0.121080960e9 * this.t532
                * this.t4003 + 0.3547315200e10 * this.t72 * this.t4264 + 0.10394342999040e14 * this.t79 * this.t5184
                + 0.92252160e8 * this.t556
                * this.t4003 - 0.768768000e9 * this.t601 * this.t4003 - 0.397299302400e12 * this.t40 * this.t230
                * this.t4276 - 0.290750853120e12
                * this.t17 * this.t84 * this.t62 * this.t43 * this.t45 + 0.219560140800e12 * this.t330 * this.t4036;
        this.t5229 = this.t40 * this.t913;
        this.t5232 = this.t15 * this.t62;
        this.t5239 =
            -0.295905209600e12 * this.t41 * this.t4463 + 0.59594895360e11 * this.t337 * this.t4048 + 0.446961715200e12
                * this.t342 * this.t4058
                - 0.35286451200e11 * this.t28 * this.t4025 + 0.17208026035200e14 * this.t72 * this.t4944
                - 0.10324815621120e14 * this.t94
                * this.t4895 + 0.46774543495680e14 * this.t127 * this.t4415 - 0.38978786246400e14 * this.t107
                * this.t4135 - 0.1434284342400e13
                * this.t41 * this.t4948 + 0.956189561600e12 * this.t18 * this.t4944 - 0.76846049280e11 * this.t149
                * this.t4025 - 0.58150170624e11
                * this.t5229 * ix - 0.21956014080e11 * this.t5232 * this.t84 + 0.298679320940e12 * this.t1028
                * this.t5153 + 0.1390547558400e13
                * this.t561 * this.t2194;
        this.t5271 =
            -0.1738184448000e13 * this.t1222 * this.t2194 - 0.119189790720e12 * this.t2130 * this.t158
                + 0.1251492802560e13 * this.t604
                * this.t2228 - 0.3128732006400e13 * this.t564 * this.t2228 - 0.6922639360e10 * this.t572 * this.t573
                * this.t254 + 0.3547315200e10
                * this.t72 * this.t5022 + 0.2522520e7 * this.t553 * this.t2162 - 0.2383795814400e13 * this.t561
                * this.t2168 + 0.595948953600e12
                * this.t564 * this.t2168 - 0.3547315200e10 * this.t149 * this.t5107 - 0.297974476800e12 * this.t564
                * this.t2194 + 0.397299302400e12
                * this.t1211 * this.t2194 + 0.248312064000e12 * this.t636 * this.t5107 + 0.40360320e8 * this.t543
                * this.t2162 + 0.2018016000e10
                * this.t90 * this.t2168;
        this.t5277 = this.t1661 * this.t75;
        this.t5301 = this.t11 * ix;
    }

    /**
     * Partial derivative due to 12th order Earth potential zonal harmonics.
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
            derParUdeg12_9(final double a, final double ex, final double ey, final double ix, final double iy) {
        this.t5304 =
            0.15682867200e11 * this.t149 * this.t2183 + 0.21956014080e11 * this.t384 * this.t113 - 0.30750720e8
                * this.t5277
                + 0.21283891200e11 * this.t323 * this.t1101 - 0.4843238400e10 * this.t556 * this.t2198
                + 0.6853412966400e13 * this.t611 * this.t2198
                - 0.10280119449600e14 * this.t641 * this.t2198 - 0.2284470988800e13 * this.t608 * this.t2194
                + 0.2741365186560e13 * this.t1228
                * this.t2194 - 0.3198259384320e13 * this.t578 * this.t2228 + 0.6396518768640e13 * this.t561
                * this.t2228 - 0.49662412800e11
                * this.t1687 * this.t579 - 0.768768000e9 * this.t1641 * this.t140 + 0.94097203200e11 * this.t601
                * this.t2198 - 0.40360320e8 * this.t5301
                * this.t140;
        this.t5310 = this.t9 * ix;
        this.t5315 = this.t426 * this.t118;
        this.t5324 = this.t230 * this.t83;
        this.t5328 = this.t27 * this.t185;
        this.t5332 = this.t14 * this.t43;
        this.t5336 = this.t16 * this.t629;
        this.t5340 = this.t15 * this.t156;
        this.t5348 =
            -0.1384527872e10 * this.t17 * ix * this.t631 - 0.2522520e7 * this.t5137 * this.t140 - 0.92252160e8
                * this.t5310 * this.t140
                + 0.5959489536000e13 * this.t587 * this.t2198 + 0.1638859622400e13 * this.t94 * this.t5315
                - 0.3575693721600e13 * this.t582
                * this.t2198 - 0.1191897907200e13 * this.t648 * this.t2198 + 0.595948953600e12 * this.t636 * this.t2198
                + 0.8307167232e10 * this.t604
                * this.t573 * this.t5324 - 0.21283891200e11 * this.t5328 * this.t188 * ix - 0.2306304000e10
                * this.t5332 * this.t45 * ix
                - 0.1655413760e10 * this.t5336 * this.t630 * ix - 0.21956014080e11 * this.t5340 * this.t157 * ix
                + 0.45741696000e11 * this.t35
                * this.t5140 - 0.45741696000e11 * this.t149 * this.t5143;
        this.t5381 =
            -0.2969812285440e13 * this.t94 * this.t5163 + 0.2969812285440e13 * this.t79 * this.t5153 - 0.198198000e9
                * this.t69 * this.t2194
                - 0.8475667200e10 * this.t90 * this.t2194 - 0.9104775680e10 * this.t94 * this.t5107 - 0.2695492800e10
                * this.t131 * this.t2198
                - 0.95777510400e11 * this.t72 * this.t2228 - 0.3027024000e10 * this.t59 * this.t2194 - 0.5765760000e10
                * this.t35 * this.t2194
                - 0.22087978872960e14 * this.t41 * this.t5163 + 0.22087978872960e14 * this.t18 * this.t5153
                + 0.10652587545600e14 * this.t54
                * this.t5153 - 0.10652587545600e14 * this.t127 * this.t5063 - 0.1921920000e10 * this.t556 * this.t5143
                + 0.13981521153600e14
                * this.t49 * this.t5153;
        this.t5420 =
            -0.13981521153600e14 * this.t41 * this.t5063 - 0.1070014374000e13 * this.t285 * this.t5063
                + 0.1070014374000e13 * this.t123
                * this.t5140 + 0.105124219200e12 * this.t69 * this.t5140 - 0.105124219200e12 * this.t285 * this.t5143
                - 0.476899543848e12 * this.t804
                * this.t5163 + 0.5568398035200e13 * this.t107 * this.t5184 + 0.139054755840e12 * this.t318 * this.t3995
                - 0.1474973660160e13
                * this.t54 * this.t4836 - 0.2383795814400e13 * this.t16 * this.t31 * this.t4208 - 0.125462937600e12
                * this.t27 * this.t12 * this.t4093
                - 0.290750853120e12 * this.t17 * this.t118 * this.t29 * this.t156 * this.t157 - 0.3476368896000e13
                * this.t40 * this.t118 * this.t4512
                - 0.188194406400e12 * this.t27 * this.t83 * this.t4246 - 0.5045040e7 * this.t68 * this.t12 * this.t4003;
        this.t5453 =
            -0.242161920e9 * this.t10 * this.t12 * this.t4003 - 0.518710832640e12 * this.t90 * this.t4395
                + 0.345807221760e12 * this.t28 * this.t4688
                + 0.476899543848e12 * this.t920 * this.t5153 - 0.2458289433600e13 * this.t28 * this.t5063
                + 0.2458289433600e13 * this.t54
                * this.t5140 - 0.446961715200e12 * this.t149 * this.t5063 + 0.446961715200e12 * this.t72 * this.t5140
                + 0.4715989278000e13
                * this.t123 * this.t5153 - 0.4715989278000e13 * this.t174 * this.t5063 - 0.16705194105600e14
                * this.t127 * this.t5163
                + 0.16705194105600e14 * this.t107 * this.t5153 - 0.316989953280e12 * this.t131 * this.t5143
                - 0.10090080000e11 * this.t532
                * this.t5143 + 0.10090080000e11 * this.t90 * this.t5146;
        this.t5458 = this.t10 * ix;
        this.t5487 =
            -0.257657400e9 * this.t1025 * this.t5143 + 0.257657400e9 * this.t972 * this.t5146 - 0.121080960e9
                * this.t5458 * this.t140
                - 0.1470268800e10 * this.t90 * this.t2228 - 0.2018016000e10 * this.t532 * this.t2198 - 0.15682867200e11
                * this.t35 * this.t2228
                + 0.1921920000e10 * this.t35 * this.t5146 - 0.67371275400e11 * this.t962 * this.t5063
                + 0.67371275400e11 * this.t1028 * this.t5140
                + 0.6570263700e10 * this.t972 * this.t5140 - 0.6570263700e10 * this.t962 * this.t5143
                - 0.3195776263680e13 * this.t131 * this.t5063
                + 0.58150170624e11 * this.t614 * ix - 0.16554137600e11 * this.t2515 * this.t4545 - 0.9225216000e10
                * this.t193 * this.t4053
                - 0.347636889600e12 * this.t342 * this.t4707;
        this.t5497 = this.t16 * this.t22;
        this.t5507 = this.t14 * this.t20;
        this.t5518 = this.t1661 * this.t99;
        this.t5528 =
            -0.16554137600e11 * this.t323 * this.t4221 - 0.13837824000e11 * this.t925 * this.t4036 - 0.62731468800e11
                * this.t598 * this.t4093
                + 0.37924024320e11 * this.t5497 * ix - 0.37924024320e11 * this.t641 * ix - 0.40360320e8 * this.t11
                * this.t83 * this.t7
                + 0.41932800e8 * this.t601 * ix - 0.41932800e8 * this.t5507 * ix + 0.4843238400e10 * this.t35
                * this.t4093
                - 0.1384527872e10 * this.t17 * this.t5324 * this.t573 - 0.2522520e7 * this.t68 * this.t83 * this.t7
                + 0.44547184281600e14 * this.t1074
                * this.t5518 - 0.115315200e9 * this.t532 * ix + 0.115315200e9 * this.t5458 * this.t19
                - 0.119189790720e12 * this.t16 * this.t84 * this.t62;
        this.t5532 = this.t16 * this.t573;
        this.t5535 = this.t2194 * this.t140;
        this.t5544 = this.t1267 * this.t99;
        this.t5563 =
            -0.92252160e8 * this.t9 * this.t83 * this.t7 - 0.1655413760e10 * this.t5532 * this.t5324
                - 0.61948893726720e14 * this.t95 * this.t5535
                - 0.760899110400e12 * this.t278 * this.t5535 - 0.15375360e8 * this.t2194 + 0.9029529600e10 * this.t587
                * ix - 0.559104e6
                * this.t1691 - 0.3374786688000e13 * this.t1074 * this.t5544 - 0.121080960e9 * this.t10 * this.t83
                * this.t7 + 0.446961715200e12
                * this.t219 * this.t3987 - 0.62574640128000e14 * this.t732 * this.t4122 + 0.316989953280e12 * this.t59
                * this.t5140 - 0.768768000e9
                * this.t15 * this.t83 * this.t7 + 0.21283891200e11 * this.t852 * this.t84 + 0.2018016000e10 * this.t90
                * this.t4093;
        this.t5579 = this.t2194 * this.t158;
        this.t5582 = this.t79 * this.t19;
        this.t5600 =
            0.2774772e7 * this.t803 * ix * this.t19 - 0.8475667200e10 * this.t90 * this.t4757 - 0.36419102720e11
                * this.t94 * this.t453 * this.t84
                + 0.439120281600e12 * this.t1184 * this.t5277 + 0.4565394662400e13 * this.t1044 * this.t5277
                - 0.3424045996800e13 * this.t1116
                * this.t5277 - 0.3779761090560e13 * this.t95 * this.t5579 + 0.1259920363520e13 * this.t5582
                * this.t5579 + 0.790164460800e12
                * this.t54 * this.t4727 - 0.31365734400e11 * this.t27 * this.t118 * this.t29 + 0.16388596224000e14
                * this.t95 * this.t5544
                - 0.658680422400e12 * this.t1023 * this.t5277 + 0.2959052096000e13 * this.t153 * this.t5518
                - 0.1775431257600e13 * this.t4620
                * this.t5518 + 0.33902668800e11 * this.t954 * this.t5277;
        this.t5601 = this.t1664 * this.t99;
        this.t5610 = this.t1797 * this.t99;
        this.t5617 = this.t1805 * this.t75;
        this.t5634 =
            -0.54628654080000e14 * this.t138 * this.t5601 + 0.21851461632000e14 * this.t1061 * this.t5601
                + 0.1775431257600e13 * this.t1058
                * this.t5544 - 0.38379112611840e14 * this.t679 * this.t4078 + 0.2519840727040e13 * this.t5582
                * this.t5610 + 0.317041296000e12
                * this.t918 * this.t5277 - 0.237780972000e12 * this.t4132 * this.t5277 + 0.887715628800e12 * this.t1058
                * this.t5617
                + 0.35508625152000e14 * this.t1153 * this.t5518 - 0.21305175091200e14 * this.t1058 * this.t5518
                + 0.780409344000e12 * this.t178
                * this.t5601 + 0.2725140250560e13 * this.t968 * this.t5277 - 0.2270950208800e13 * this.t4085
                * this.t5277 + 0.93549086991360e14
                * this.t1036 * this.t5277 - 0.77957572492800e14 * this.t144 * this.t5277;
        this.t5637 = this.t1664 * this.t113;
        this.t5644 = this.t27 * this.t21;
        this.t5649 = this.t1664 * this.t75;
        this.t5652 = this.t49 * this.t19;
        this.t5655 = this.t2198 * this.t140;
        this.t5666 = this.t1797 * this.t75;
        this.t5673 =
            -0.7559522181120e13 * this.t95 * this.t5637 + 0.2519840727040e13 * this.t5582 * this.t5637
                - 0.7559522181120e13 * this.t95
                * this.t5610 - 0.9029529600e10 * this.t5644 * ix + 0.12793037537280e14 * this.t772 * this.t4078
                - 0.507266073600e12 * this.t278
                * this.t5649 + 0.253633036800e12 * this.t5652 * this.t5649 - 0.40971490560000e14 * this.t138
                * this.t5655 + 0.16388596224000e14
                * this.t1061 * this.t5655 + 0.635156121600e12 * this.t91 * this.t5535 + 0.13499146752000e14 * this.t128
                * this.t5601
                - 0.6749573376000e13 * this.t4310 * this.t5601 - 0.16388596224000e14 * this.t138 * this.t5666
                + 0.6555438489600e13 * this.t1061
                * this.t5666 + 0.1383228887040e13 * this.t1159 * this.t5535;
        this.t5676 = this.t2194 * this.t167;
        this.t5685 = this.t1661 * this.t113;
        this.t5706 =
            0.23063040000e11 * this.t855 * this.t5277 - 0.4438578144000e13 * this.t171 * this.t5676
                + 0.1775431257600e13 * this.t4289
                * this.t5676 + 0.89094368563200e14 * this.t1050 * this.t5676 - 0.44547184281600e14 * this.t80
                * this.t5676 + 0.35637747425280e14
                * this.t1050 * this.t5685 - 0.17818873712640e14 * this.t80 * this.t5685 - 0.3155425553280e13 * this.t18
                * this.t4754
                + 0.2219289072000e13 * this.t41 * this.t4765 + 0.63408259200e11 * this.t5652 * this.t4025
                - 0.248312064000e12 * this.t866
                * this.t3990 - 0.61680716697600e14 * this.t1990 * this.t4036 + 0.12108096000e11 * this.t923
                * this.t5277 + 0.4438578144000e13
                * this.t153 * this.t5535 - 0.2663146886400e13 * this.t4620 * this.t5535;
        this.t5716 = this.t1267 * this.t75;
        this.t5719 = this.t2162 * this.t140;
        this.t5722 = this.t1267 * this.t113;
        this.t5725 = this.t1805 * this.t99;
        this.t5728 = this.t2162 * this.t167;
        this.t5731 = this.t1928 * this.t75;
        this.t5744 =
            0.2959052096000e13 * this.t153 * this.t5649 - 0.1775431257600e13 * this.t4620 * this.t5649
                - 0.12872497397760e14 * this.t150
                * this.t5518 + 0.6436248698880e13 * this.t670 * this.t5518 - 0.16172956800e11 * this.t132 * this.t5716
                - 0.16172956800e11 * this.t132
                * this.t5719 + 0.1889880545280e13 * this.t80 * this.t5722 + 0.3149800908800e13 * this.t80 * this.t5725
                + 0.887715628800e12
                * this.t1058 * this.t5728 + 0.629960181760e12 * this.t80 * this.t5731 + 0.10124360064000e14 * this.t128
                * this.t5655
                - 0.5062180032000e13 * this.t4310 * this.t5655 - 0.6321315686400e13 * this.t1189 * this.t5649
                + 0.3160657843200e13 * this.t162
                * this.t5649 - 0.45405360e8 * this.t553 * ix;
        this.t5770 = this.t285 * this.t19;
        this.t5777 =
            0.1274668595200e13 * this.t670 * this.t5579 - 0.146326752000e12 * this.t162 * this.t5728 - 0.15375360e8
                * this.t4011
                + 0.41120477798400e14 * this.t896 * this.t4036 + 0.6918912000e10 * this.t204 * this.t4014
                - 0.570674332800e12 * this.t49 * this.t4836
                + 0.46774543495680e14 * this.t1036 * this.t4011 - 0.38978786246400e14 * this.t144 * this.t4011
                + 0.2282697331200e13 * this.t1044
                * this.t4011 - 0.1712022998400e13 * this.t1116 * this.t4011 + 0.5939624570880e13 * this.t1050
                * this.t3984 - 0.2969812285440e13
                * this.t80 * this.t3984 - 0.13140527400e11 * this.t1046 * this.t4011 + 0.8760351600e10 * this.t5770
                * this.t4011 - 0.126816518400e12
                * this.t278 * this.t4025 - 0.23063040e8 * this.t556 * ix;
        this.t5796 = this.t90 * this.t117;
        this.t5813 =
            0.23063040e8 * this.t5310 * this.t19 + 0.31183028997120e14 * this.t1107 * this.t5716 + 0.25029856051200e14
                * this.t981 * this.t4122
                - 0.18772392038400e14 * this.t875 * this.t3987 + 0.7508956815360e13 * this.t685 * this.t3987
                + 0.1986496512000e13 * this.t807
                * this.t3981 - 0.28378521600e11 * this.t798 * this.t3981 - 0.8821612800e10 * this.t5796 * this.t4019
                - 0.595948953600e12 * this.t364
                * this.t3995 + 0.68832104140800e14 * this.t73 * this.t5518 - 0.41299262484480e14 * this.t95
                * this.t5518 - 0.8605706054400e13
                * this.t1098 * this.t5535 + 0.5737137369600e13 * this.t4125 * this.t5535 + 0.4827186524160e13
                * this.t138 * this.t5719
                - 0.439120281600e12 * this.t204 * this.t4122;
        this.t5818 = this.t1800 * this.t75;
        this.t5845 =
            0.7151387443200e13 * this.t1039 * this.t5277 - 0.5363540582400e13 * this.t138 * this.t5277
                - 0.1079931740160e13 * this.t95
                * this.t5818 + 0.359977246720e12 * this.t5582 * this.t5818 - 0.10652587545600e14 * this.t128
                * this.t5716 - 0.329340211200e12
                * this.t198 * this.t4033 - 0.14849061427200e14 * this.t1090 * this.t5617 - 0.131736084480e12
                * this.t1270 * this.t4019
                - 0.1489872384000e13 * this.t330 * this.t3990 + 0.208582133760e12 * this.t313 * this.t4078
                - 0.198649651200e12 * this.t219
                * this.t4659 + 0.1434284342400e13 * this.t275 * this.t5719 - 0.595948953600e12 * this.t844 * this.t4048
                + 0.4827186524160e13
                * this.t138 * this.t5716 - 0.74493619200e11 * this.t852 * this.t4788;
        this.t5853 = this.t2168 * this.t140;
        this.t5870 = this.t2168 * this.t167;
        this.t5879 =
            -0.31183028997120e14 * this.t135 * this.t4025 + 0.20788685998080e14 * this.t1090 * this.t4025
                + 0.16705194105600e14 * this.t144
                * this.t5716 - 0.2873325312000e13 * this.t670 * this.t5853 - 0.1687393344000e13 * this.t1074
                * this.t5728 - 0.443857814400e12
                * this.t41 * this.t4895 + 0.190224777600e12 * this.t1116 * this.t5719 + 0.16705194105600e14 * this.t144
                * this.t5719
                + 0.739763024000e12 * this.t49 * this.t4944 - 0.10652587545600e14 * this.t128 * this.t5719
                - 0.1274668595200e13 * this.t1061
                * this.t5725 - 0.1274668595200e13 * this.t1061 * this.t5870 - 0.329340211200e12 * this.t1031
                * this.t4058 - 0.211718707200e12
                * this.t178 * this.t5716 - 0.1687393344000e13 * this.t1074 * this.t5617;
        this.t5896 = this.t2198 * this.t167;
        this.t5901 = this.t2183 * this.t140;
        this.t5912 =
            0.190224777600e12 * this.t1116 * this.t5716 + 0.16388596224000e14 * this.t95 * this.t5853
                - 0.211718707200e12 * this.t178
                * this.t5719 - 0.170271129600e12 * this.t384 * this.t3981 - 0.7795757249280e13 * this.t108 * this.t4003
                - 0.39630162000e11
                * this.t124 * this.t4003 + 0.5746650624000e13 * this.t1184 * this.t5676 + 0.2370493382400e13
                * this.t171 * this.t5719
                - 0.9449402726400e13 * this.t95 * this.t5896 + 0.3149800908800e13 * this.t80 * this.t5870
                + 0.1889880545280e13 * this.t80
                * this.t5901 + 0.13110876979200e14 * this.t94 * this.t4323 + 0.922152591360e12 * this.t1159
                * this.t5518 - 0.1037421665280e13
                * this.t1048 * this.t5277 + 0.691614443520e12 * this.t178 * this.t5277;
        this.t5927 = this.t2228 * this.t140;
        this.t5946 =
            -0.461076295680e12 * this.t1184 * this.t5716 - 0.764801157120e12 * this.t1061 * this.t5901
                - 0.20649631242240e14 * this.t1050
                * this.t5719 - 0.764801157120e12 * this.t1061 * this.t5722 - 0.131736084480e12 * this.t262 * this.t3987
                + 0.3149800908800e13
                * this.t5582 * this.t5896 - 0.3779761090560e13 * this.t95 * this.t5927 + 0.1259920363520e13
                * this.t5582 * this.t5927
                - 0.124732115988480e15 * this.t135 * this.t5518 + 0.83154743992320e14 * this.t1090 * this.t5518
                - 0.16388596224000e14 * this.t72
                * this.t4135 + 0.922152591360e12 * this.t1159 * this.t5649 - 0.20649631242240e14 * this.t1050
                * this.t5716 + 0.53262937728000e14
                * this.t1153 * this.t5535 - 0.31957762636800e14 * this.t1058 * this.t5535;
    }

    /**
     * Partial derivative due to 12th order Earth potential zonal harmonics.
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
    private final void derParUdeg12_10(final double a, final double ex, final double ey, final double ix,
                                       final double iy) {
        this.t5977 =
            0.2549337190400e13 * this.t670 * this.t5610 + 0.3186671488000e13 * this.t670 * this.t5896
                - 0.66820776422400e14 * this.t673
                * this.t5649 + 0.44547184281600e14 * this.t1074 * this.t5649 - 0.3198259384320e13 * this.t611
                * this.t4149 + 0.141261120e9
                * this.t5301 * this.t19 - 0.3550862515200e13 * this.t128 * this.t4093 - 0.227095020880e12 * this.t1194
                * this.t4003
                + 0.8194298112000e13 * this.t95 * this.t5728 - 0.254933719040e12 * this.t1061 * this.t5731
                - 0.6922639360e10 * this.t572 * this.t4823
                - 0.1390547558400e13 * this.t1015 * this.t3984 + 0.1775431257600e13 * this.t1058 * this.t5853
                + 0.177543125760e12 * this.t127
                * this.t5315 - 0.1436662656000e13 * this.t670 * this.t5728;
        this.t5989 = this.t564 * this.t117;
        this.t5994 = this.t587 * this.t81;
        this.t6011 =
            0.1434284342400e13 * this.t275 * this.t5716 - 0.146326752000e12 * this.t162 * this.t5617
                - 0.292653504000e12 * this.t162 * this.t5853
                + 0.595948953600e12 * this.t564 * this.t4093 + 0.376388812800e12 * this.t723 * this.t4053
                - 0.18772392038400e14 * this.t5989
                * this.t4019 + 0.7508956815360e13 * this.t890 * this.t4019 + 0.23837958144000e14 * this.t5994
                * this.t4028 - 0.14302774886400e14
                * this.t679 * this.t4028 - 0.292653504000e12 * this.t162 * this.t5544 + 0.40360320e8 * this.t543
                * this.t4003 - 0.15682867200e11
                * this.t35 * this.t3984 + 0.2298660249600e13 * this.t1184 * this.t5666 - 0.5737137369600e13
                * this.t1098 * this.t5518
                + 0.35756937216000e14 * this.t1962 * this.t4036;
        this.t6044 =
            -0.21454162329600e14 * this.t676 * this.t4036 + 0.9535183257600e13 * this.t951 * this.t4122
                + 0.3824758246400e13 * this.t4125
                * this.t5518 - 0.1436662656000e13 * this.t670 * this.t5617 - 0.1470268800e10 * this.t90 * this.t3984
                - 0.388150963200e12 * this.t801
                * this.t5277 + 0.258767308800e12 * this.t132 * this.t5277 - 0.5737137369600e13 * this.t1098
                * this.t5649 + 0.3824758246400e13
                * this.t4125 * this.t5649 - 0.55926084614400e14 * this.t956 * this.t5277 + 0.44740867691520e14
                * this.t128 * this.t5277
                + 0.26728310568960e14 * this.t94 * this.t4415 - 0.44547184281600e14 * this.t1107 * this.t5277
                + 0.32345913600e11 * this.t1167
                * this.t5649 + 0.4049744025600e13 * this.t128 * this.t5685 - 0.2024872012800e13 * this.t4310
                * this.t5685;
        this.t6078 =
            -0.26281054800e11 * this.t1046 * this.t5277 + 0.17520703200e11 * this.t5770 * this.t5277
                + 0.478094780800e12 * this.t18 * this.t5184
                - 0.2873325312000e13 * this.t670 * this.t5544 - 0.29698122854400e14 * this.t1090 * this.t5853
                - 0.29698122854400e14 * this.t1090
                * this.t5544 + 0.48518870400e11 * this.t1167 * this.t5535 - 0.1775431257600e13 * this.t171 * this.t5666
                - 0.22273592140800e14
                * this.t79 * this.t4135 + 0.710172503040e12 * this.t4289 * this.t5666 - 0.5918104192000e13 * this.t171
                * this.t5601
                + 0.2367241676800e13 * this.t4289 * this.t5601 + 0.585307008000e12 * this.t178 * this.t5676
                + 0.792792000e9 * this.t911 * this.t5277
                + 0.53456621137920e14 * this.t970 * this.t5277;
        this.t6109 =
            -0.357569372160e12 * this.t636 * this.t4149 + 0.234122803200e12 * this.t178 * this.t5666
                + 0.585307008000e12 * this.t178 * this.t5655
                + 0.11799789281280e14 * this.t958 * this.t5277 - 0.8849841960960e13 * this.t171 * this.t5277
                + 0.208582133760e12 * this.t267
                * this.t3990 - 0.9481973529600e13 * this.t1189 * this.t5535 + 0.4740986764800e13 * this.t162
                * this.t5535 + 0.14898723840e11
                * this.t253 * this.t4404 - 0.1775431257600e13 * this.t171 * this.t5685 + 0.710172503040e12 * this.t4289
                * this.t5685
                + 0.35637747425280e14 * this.t1050 * this.t5666 - 0.17818873712640e14 * this.t80 * this.t5666
                - 0.40971490560000e14 * this.t138
                * this.t5676 + 0.16388596224000e14 * this.t1061 * this.t5676;
        this.t6139 = this.t15 * this.t42;
        this.t6142 =
            0.3426706483200e13 * this.t608 * this.t4093 + 0.148987238400e12 * this.t214 * this.t4019
                + 0.1274668595200e13 * this.t670
                * this.t5927 + 0.559104e6 * this.t5146 + 0.2522520e7 * this.t553 * this.t4003 - 0.5765760000e10
                * this.t1159 * this.t4003
                - 0.2969812285440e13 * this.t1090 * this.t4149 + 0.89994311680e11 * this.t79 * this.t229 * this.t84
                + 0.1655413760e10 * this.t841
                * this.t254 + 0.423437414400e12 * this.t91 * this.t5649 + 0.21956014080e11 * this.t844 * this.t118
                + 0.31183028997120e14 * this.t1107
                * this.t5719 - 0.1571996426000e13 * this.t916 * this.t5277 + 0.1257597140800e13 * this.t4294
                * this.t5277 + 0.950476800e9
                * this.t6139 * ix;
        this.t6143 = this.t1661 * this.t1101;
        this.t6174 =
            0.359977246720e12 * this.t5582 * this.t6143 - 0.187098173982720e15 * this.t135 * this.t5535
                + 0.124732115988480e15 * this.t1090
                * this.t5535 - 0.254933719040e12 * this.t1061 * this.t4556 - 0.100231164633600e15 * this.t673
                * this.t5535 + 0.66820776422400e14
                * this.t1074 * this.t5535 + 0.32345913600e11 * this.t1167 * this.t5518 + 0.383110041600e12 * this.t1184
                * this.t3984
                - 0.16705194105600e14 * this.t673 * this.t4025 + 0.11136796070400e14 * this.t1074 * this.t4025
                + 0.1609062174720e13 * this.t138
                * this.t4093 - 0.3027024000e10 * this.t1167 * this.t4003 - 0.329340211200e12 * this.t1023 * this.t4011
                + 0.219560140800e12
                * this.t1184 * this.t4011 + 0.105859353600e12 * this.t91 * this.t4025;
        this.t6207 =
            -0.153692098560e12 * this.t149 * this.t4339 - 0.29265350400e11 * this.t54 * this.t4730
                - 0.22192890720000e14 * this.t847 * this.t5277
                + 0.17754312576000e14 * this.t103 * this.t5277 - 0.1079931740160e13 * this.t95 * this.t6143
                - 0.3550862515200e13 * this.t127
                * this.t4463 - 0.124732115988480e15 * this.t135 * this.t5649 + 0.83154743992320e14 * this.t1090
                * this.t5649
                + 0.89094368563200e14 * this.t1050 * this.t5655 - 0.3374786688000e13 * this.t1074 * this.t5853
                + 0.2370493382400e13 * this.t171
                * this.t5716 + 0.2298660249600e13 * this.t1184 * this.t5685 + 0.35508625152000e14 * this.t1153
                * this.t5649
                - 0.21305175091200e14 * this.t1058 * this.t5649 - 0.507266073600e12 * this.t278 * this.t5518;
        this.t6238 =
            0.253633036800e12 * this.t5652 * this.t5518 + 0.364191027200e12 * this.t670 * this.t6143
                + 0.7662200832000e13 * this.t1184
                * this.t5601 - 0.16388596224000e14 * this.t138 * this.t5685 + 0.6555438489600e13 * this.t1061
                * this.t5685 + 0.423437414400e12
                * this.t91 * this.t5518 + 0.380449555200e12 * this.t5652 * this.t5535 + 0.37865106639360e14
                * this.t1034 * this.t5277
                - 0.31554255532800e14 * this.t275 * this.t5277 + 0.234122803200e12 * this.t178 * this.t5685
                - 0.4438578144000e13 * this.t171
                * this.t5655 + 0.1775431257600e13 * this.t4289 * this.t5655 + 0.364191027200e12 * this.t670
                * this.t5818 + 0.68832104140800e14
                * this.t73 * this.t5649 - 0.41299262484480e14 * this.t95 * this.t5649;
        this.t6270 =
            -0.44547184281600e14 * this.t80 * this.t5655 + 0.118792491417600e15 * this.t1050 * this.t5601
                - 0.59396245708800e14 * this.t80
                * this.t5601 + 0.3160657843200e13 * this.t162 * this.t5518 + 0.103248156211200e15 * this.t73
                * this.t5535 + 0.2549337190400e13
                * this.t670 * this.t5637 + 0.4049744025600e13 * this.t128 * this.t5666 - 0.2024872012800e13
                * this.t4310 * this.t5666
                - 0.685341296640e12 * this.t582 * this.t4299 + 0.5746650624000e13 * this.t1184 * this.t5655
                + 0.10124360064000e14 * this.t128
                * this.t5676 - 0.5062180032000e13 * this.t4310 * this.t5676 - 0.6321315686400e13 * this.t1189
                * this.t5518 - 0.66820776422400e14
                * this.t673 * this.t5518 - 0.32777192448000e14 * this.t909 * this.t5277;
        this.t6303 =
            0.26221753958400e14 * this.t1050 * this.t5277 - 0.19308746096640e14 * this.t150 * this.t5535
                + 0.9654373048320e13 * this.t670
                * this.t5535 - 0.134991467520e12 * this.t95 * this.t4299 + 0.44997155840e11 * this.t5582 * this.t4299
                + 0.39020467200e11 * this.t178
                * this.t3984 - 0.27963042307200e14 * this.t956 * this.t4011 + 0.22370433845760e14 * this.t128
                * this.t4011 + 0.16951334400e11
                * this.t954 * this.t4011 - 0.12872497397760e14 * this.t150 * this.t5649 + 0.6436248698880e13
                * this.t670 * this.t5649
                - 0.887715628800e12 * this.t103 * this.t5719 - 0.887715628800e12 * this.t103 * this.t5716
                + 0.8194298112000e13 * this.t95
                * this.t5617 - 0.14849061427200e14 * this.t1090 * this.t5728 - 0.461076295680e12 * this.t1184
                * this.t5719;
        this.t6314 = this.t96 * this.t2608;
        this.t6317 = this.t163 * this.t2637;
        this.t6320 = this.t139 * this.t3284;
        this.t6324 = this.t13 * ex * this.t194;
        this.t6337 = this.t139 * this.t2722;
        this.t6346 = this.t119 * this.t2608;
        this.t6349 = this.t163 * this.t2722;
        this.t6352 =
            0.3824758246400e13 * this.t4125 * this.t6314 - 0.146326752000e12 * this.t162 * this.t6317
                + 0.585307008000e12 * this.t178
                * this.t6320 + 0.792792000e9 * this.t911 * this.t6324 + 0.53456621137920e14 * this.t970 * this.t6324
                + 0.5746650624000e13
                * this.t1184 * this.t6320 + 0.922152591360e12 * this.t1159 * this.t6314 - 0.44547184281600e14
                * this.t1107 * this.t6324
                + 0.32345913600e11 * this.t1167 * this.t6314 + 0.48518870400e11 * this.t1167 * this.t6337
                - 0.5737137369600e13 * this.t1098
                * this.t6314 - 0.55926084614400e14 * this.t956 * this.t6324 + 0.44740867691520e14 * this.t128
                * this.t6324 + 0.234122803200e12
                * this.t178 * this.t6346 + 0.585307008000e12 * this.t178 * this.t6349;
        this.t6367 = this.t96 * this.t2704;
        this.t6374 = this.t74 * this.t2698;
        this.t6383 = this.t74 * this.t2704;
        this.t6386 =
            -0.40971490560000e14 * this.t138 * this.t6320 + 0.16388596224000e14 * this.t1061 * this.t6320
                - 0.9481973529600e13 * this.t1189
                * this.t6337 + 0.4740986764800e13 * this.t162 * this.t6337 + 0.423437414400e12 * this.t91 * this.t6314
                - 0.1775431257600e13
                * this.t171 * this.t6346 + 0.710172503040e12 * this.t4289 * this.t6346 - 0.5918104192000e13 * this.t171
                * this.t6367
                + 0.11799789281280e14 * this.t958 * this.t6324 - 0.8849841960960e13 * this.t171 * this.t6324
                - 0.1775431257600e13 * this.t171
                * this.t6374 + 0.710172503040e12 * this.t4289 * this.t6374 - 0.100231164633600e15 * this.t673
                * this.t6337 + 0.66820776422400e14
                * this.t1074 * this.t6337 + 0.32345913600e11 * this.t1167 * this.t6383;
        this.t6394 = this.t139 * this.t3290;
        this.t6419 =
            -0.1571996426000e13 * this.t916 * this.t6324 + 0.1257597140800e13 * this.t4294 * this.t6324
                - 0.3424045996800e13 * this.t1116
                * this.t6324 - 0.3779761090560e13 * this.t95 * this.t6394 + 0.1259920363520e13 * this.t5582
                * this.t6394 + 0.35637747425280e14
                * this.t1050 * this.t6346 - 0.17818873712640e14 * this.t80 * this.t6346 + 0.2298660249600e13
                * this.t1184 * this.t6374
                + 0.35508625152000e14 * this.t1153 * this.t6314 - 0.21305175091200e14 * this.t1058 * this.t6314
                - 0.32777192448000e14
                * this.t909 * this.t6324 + 0.26221753958400e14 * this.t1050 * this.t6324 - 0.19308746096640e14
                * this.t150 * this.t6337
                + 0.9654373048320e13 * this.t670 * this.t6337 - 0.66820776422400e14 * this.t673 * this.t6383;
        this.t6432 = this.t119 * this.t2704;
        this.t6435 = this.t163 * this.t3284;
        this.t6438 = this.t536 * this.t2722;
        this.t6441 = this.t74 * this.t2961;
        this.t6454 =
            0.44547184281600e14 * this.t1074 * this.t6383 + 0.922152591360e12 * this.t1159 * this.t6383
                - 0.1037421665280e13 * this.t1048
                * this.t6324 + 0.691614443520e12 * this.t178 * this.t6324 + 0.53262937728000e14 * this.t1153
                * this.t6337 - 0.31957762636800e14
                * this.t1058 * this.t6337 + 0.2549337190400e13 * this.t670 * this.t6432 + 0.3186671488000e13
                * this.t670 * this.t6435
                + 0.1274668595200e13 * this.t670 * this.t6438 + 0.359977246720e12 * this.t5582 * this.t6441
                + 0.1274668595200e13 * this.t670
                * this.t6394 - 0.187098173982720e15 * this.t135 * this.t6337 + 0.124732115988480e15 * this.t1090
                * this.t6337
                - 0.658680422400e12 * this.t1023 * this.t6324 + 0.439120281600e12 * this.t1184 * this.t6324;
        this.t6459 = this.t74 * ey;
        this.t6464 = this.t550 * iy;
        this.t6467 = this.t163 * iy;
        this.t6472 = this.t85 * this.t2608;
        this.t6488 = iy * this.t62 * this.t64;
        this.t6491 = this.t96 * this.t2698;
        this.t6494 =
            0.4565394662400e13 * this.t1044 * this.t6324 + 0.40360320e8 * this.t543 * this.t6459 - 0.768768000e9
                * this.t601 * this.t6459
                - 0.3547315200e10 * this.t149 * this.t6464 + 0.5959489536000e13 * this.t587 * this.t6467
                - 0.3575693721600e13 * this.t582
                * this.t6467 + 0.359977246720e12 * this.t5582 * this.t6472 + 0.33902668800e11 * this.t954 * this.t6324
                - 0.54628654080000e14
                * this.t138 * this.t6367 + 0.21851461632000e14 * this.t1061 * this.t6367 - 0.22192890720000e14
                * this.t847 * this.t6324
                + 0.17754312576000e14 * this.t103 * this.t6324 - 0.1079931740160e13 * this.t95 * this.t6441
                - 0.95777510400e11 * this.t72
                * this.t6488 - 0.7559522181120e13 * this.t95 * this.t6491;
        this.t6507 = this.t397 * this.t98;
        this.t6524 = this.t74 * this.t2641;
        this.t6527 =
            0.2519840727040e13 * this.t5582 * this.t6491 - 0.7559522181120e13 * this.t95 * this.t6432
                + 0.2519840727040e13 * this.t5582
                * this.t6432 + 0.7151387443200e13 * this.t1039 * this.t6324 - 0.5363540582400e13 * this.t138
                * this.t6324 - 0.1079931740160e13
                * this.t95 * this.t6472 - 0.22273592140800e14 * this.t79 * this.t6507 - 0.124732115988480e15
                * this.t135 * this.t6314
                + 0.83154743992320e14 * this.t1090 * this.t6314 + 0.89094368563200e14 * this.t1050 * this.t6349
                - 0.2695492800e10 * this.t131
                * this.t6467 - 0.44547184281600e14 * this.t80 * this.t6349 + 0.118792491417600e15 * this.t1050
                * this.t6367
                - 0.59396245708800e14 * this.t80 * this.t6367 + 0.1434284342400e13 * this.t275 * this.t6524;
        this.t6537 = this.t403 * this.t98;
        this.t6540 = this.t163 * this.t2645;
        this.t6547 = this.t36 * this.t98;
        this.t6559 = this.t437 * this.t326;
        this.t6564 =
            0.2959052096000e13 * this.t153 * this.t6383 - 0.1775431257600e13 * this.t4620 * this.t6383
                + 0.93549086991360e14 * this.t1036
                * this.t6324 - 0.77957572492800e14 * this.t144 * this.t6324 + 0.26728310568960e14 * this.t94
                * this.t6537 - 0.1274668595200e13
                * this.t1061 * this.t6540 + 0.253633036800e12 * this.t5652 * this.t6383 + 0.364191027200e12 * this.t670
                * this.t6441
                + 0.129383654400e12 * this.t131 * this.t6547 - 0.45405360e8 * this.t553 * iy - 0.2018016000e10
                * this.t532 * this.t3284
                - 0.31365734400e11 * this.t27 * iy * this.t163 - 0.39207168000e11 * this.t1224 * this.t2722
                - 0.337478668800e12 * this.t107
                * this.t6559 - 0.11096445360000e14 * this.t49 * this.t6507;
        this.t6565 = this.t224 * this.t98;
        this.t6568 = this.t44 * this.t112;
        this.t6571 = this.t288 * this.t112;
        this.t6585 = this.t238 * this.t98;
        this.t6588 = this.t397 * iy;
        this.t6591 = this.t44 * this.t98;
        this.t6594 = this.t403 * iy;
        this.t6597 = this.t224 * iy;
        this.t6600 = this.t296 * this.t98;
        this.t6604 = iy * this.t7 * this.t12;
        this.t6607 =
            0.8877156288000e13 * this.t41 * this.t6565 + 0.8877156288000e13 * this.t54 * this.t6568
                - 0.5326293772800e13 * this.t127 * this.t6571
                + 0.18932553319680e14 * this.t41 * this.t6537 - 0.15777127766400e14 * this.t18 * this.t6507
                - 0.1191897907200e13 * this.t648
                * this.t6467 + 0.595948953600e12 * this.t636 * this.t6467 + 0.8307167232e10 * this.t604 * this.t575
                * iy - 0.194075481600e12
                * this.t59 * this.t6585 + 0.2219289072000e13 * this.t41 * this.t6588 - 0.3550862515200e13 * this.t127
                * this.t6591
                - 0.3155425553280e13 * this.t18 * this.t6594 - 0.39630162000e11 * this.t123 * this.t6597
                + 0.478094780800e12 * this.t18 * this.t6600
                + 0.26138112000e11 * this.t598 * this.t6604;
        this.t6617 = this.t296 * this.t112;
        this.t6622 = this.t281 * this.t326;
        this.t6625 = this.t246 * this.t112;
        this.t6638 = this.t437 * this.t112;
        this.t6641 = this.t246 * this.t98;
    }

    /**
     * Partial derivative due to 12th order Earth potential zonal harmonics.
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
    private final void derParUdeg12_11(final double a, final double ex, final double ey, final double ix,
                                       final double iy) {
        this.t6646 =
            -0.3027024000e10 * this.t59 * this.t6604 + 0.158520648000e12 * this.t285 * this.t6565 - 0.118890486000e12
                * this.t123 * this.t6585
                - 0.1434284342400e13 * this.t41 * this.t6617 + 0.956189561600e12 * this.t18 * this.t6568
                + 0.674957337600e12 * this.t127 * this.t6622
                + 0.790164460800e12 * this.t54 * this.t6625 + 0.7662200832000e13 * this.t1184 * this.t6367
                - 0.16388596224000e14 * this.t138
                * this.t6374 - 0.950476800e9 * this.t648 * iy - 0.227095020880e12 * this.t430 * this.t6594
                - 0.893923430400e12 * this.t72
                * this.t6597 + 0.177543125760e12 * this.t127 * this.t6638 - 0.5390985600e10 * this.t131 * this.t6641
                - 0.144144000e9 * this.t543
                * this.t6467;
        this.t6656 = this.t281 * this.t112;
        this.t6669 = this.t119 * ey;
        this.t6672 = this.t139 * this.t2637;
        this.t6675 = this.t119 * this.t2641;
        this.t6681 = this.t163 * this.t8;
        this.t6684 =
            -0.4843238400e10 * this.t556 * this.t6467 + 0.94097203200e11 * this.t601 * this.t6467 + 0.3277719244800e13
                * this.t94 * this.t6588
                - 0.36419102720e11 * this.t94 * this.t416 * this.t326 - 0.337478668800e12 * this.t107 * this.t6656
                - 0.1580328921600e13 * this.t28
                * this.t6571 + 0.6396518768640e13 * this.t561 * this.t6488 - 0.3198259384320e13 * this.t578
                * this.t6488 + 0.2741365186560e13
                * this.t1228 * this.t6604 + 0.13069056000e11 * this.t648 * this.t6459 - 0.3198259384320e13 * this.t611
                * this.t6669
                - 0.10652587545600e14 * this.t128 * this.t6672 - 0.764801157120e12 * this.t1061 * this.t6675
                - 0.9104775680e10 * this.t94
                * this.t6464 - 0.1436662656000e13 * this.t72 * this.t98 * this.t6681;
        this.t6692 = this.t416 * this.t189;
        this.t6698 = this.t319 * this.t112;
        this.t6701 = this.t325 * this.t157;
        this.t6704 = this.t331 * iy;
        this.t6721 =
            -0.8475667200e10 * this.t90 * this.t6604 - 0.16388596224000e14 * this.t72 * this.t6507
                + 0.13110876979200e14 * this.t94 * this.t6565
                + 0.45523878400e11 * this.t72 * this.t6692 - 0.1537536000e10 * this.t15 * this.t13 * this.t6459
                + 0.208582133760e12 * this.t318
                * this.t6698 + 0.148987238400e12 * this.t323 * this.t6701 + 0.109780070400e12 * this.t330 * this.t6704
                - 0.295905209600e12 * this.t41
                * this.t6591 + 0.739763024000e12 * this.t153 * this.t6467 - 0.443857814400e12 * this.t4620 * this.t6467
                + 0.2282697331200e13
                * this.t1044 * this.t6604 - 0.1712022998400e13 * this.t1116 * this.t6604 - 0.1580328921600e13
                * this.t1189 * this.t6467
                + 0.790164460800e12 * this.t162 * this.t6467;
        this.t6722 = this.t96 * this.t2641;
        this.t6729 = this.t139 * this.t2645;
        this.t6742 = this.t220 * this.t157;
        this.t6745 = this.t338 * this.t98;
        this.t6748 = this.t331 * this.t98;
        this.t6751 = this.t194 * this.t13;
        this.t6756 = this.t199 * this.t98;
        this.t6759 =
            0.16388596224000e14 * this.t95 * this.t6722 - 0.887715628800e12 * this.t103 * this.t6524
                + 0.31183028997120e14 * this.t1107
                * this.t6524 - 0.2873325312000e13 * this.t670 * this.t6729 - 0.461076295680e12 * this.t1184
                * this.t6524 + 0.16951334400e11
                * this.t532 * this.t6547 - 0.329340211200e12 * this.t1023 * this.t6604 + 0.219560140800e12 * this.t1184
                * this.t6604
                + 0.39020467200e11 * this.t178 * this.t6488 + 0.13905475584000e14 * this.t996 * this.t6742
                + 0.373822525440e12 * this.t2022
                * this.t6745 + 0.7151387443200e13 * this.t1979 * this.t6748 - 0.3476368896000e13 * this.t2324
                * this.t6751 + 0.2781095116800e13
                * this.t1009 * this.t6751 + 0.35756937216000e14 * this.t1962 * this.t6756;
        this.t6764 = this.t360 * this.t13;
        this.t6769 = this.t205 * this.t13;
        this.t6774 = this.t353 * this.t112;
        this.t6779 = this.t375 * this.t326;
        this.t6794 = this.t263 * this.t13;
        this.t6797 =
            -0.21454162329600e14 * this.t676 * this.t6756 - 0.18772392038400e14 * this.t5989 * this.t6764
                + 0.7508956815360e13 * this.t890
                * this.t6764 + 0.23837958144000e14 * this.t5994 * this.t6769 - 0.14302774886400e14 * this.t679
                * this.t6769
                - 0.47973890764800e14 * this.t878 * this.t6774 + 0.15991296921600e14 * this.t945 * this.t6774
                - 0.19189556305920e14 * this.t676
                * this.t6779 + 0.6396518768640e13 * this.t881 * this.t6779 + 0.674957337600e12 * this.t128 * this.t6488
                - 0.337478668800e12
                * this.t4310 * this.t6488 + 0.396396000e9 * this.t911 * this.t6604 - 0.27963042307200e14 * this.t956
                * this.t6604
                + 0.22370433845760e14 * this.t128 * this.t6604 + 0.14529715200e11 * this.t906 * this.t6794;
        this.t6798 = this.t370 * this.t13;
        this.t6803 = this.t370 * this.t45;
        this.t6806 = this.t263 * this.t45;
        this.t6809 = this.t42 * iy;
        this.t6832 =
            0.156828672000e12 * this.t863 * this.t6798 + 0.78414336000e11 * this.t866 * this.t6704 + 0.9535183257600e13
                * this.t951 * this.t6803
                + 0.376388812800e12 * this.t723 * this.t6806 + 0.316989953280e12 * this.t59 * this.t6809
                + 0.16951334400e11 * this.t954 * this.t6604
                + 0.45523878400e11 * this.t670 * this.t6464 - 0.785998213000e12 * this.t916 * this.t6604
                + 0.628798570400e12 * this.t4294
                * this.t6604 + 0.3575693721600e13 * this.t1039 * this.t6604 - 0.2681770291200e13 * this.t138
                * this.t6604 - 0.194075481600e12
                * this.t801 * this.t6604 + 0.129383654400e12 * this.t132 * this.t6604 - 0.13140527400e11 * this.t1046
                * this.t6604 + 0.8760351600e10
                * this.t5770 * this.t6604;
        this.t6838 = this.t319 * this.t326;
        this.t6851 = this.t187 * this.t630;
        this.t6854 = this.t509 * this.t45;
        this.t6861 = this.t365 * this.t112;
        this.t6866 = this.t385 * this.t157;
        this.t6869 =
            0.383110041600e12 * this.t1184 * this.t6488 - 0.8072064000e10 * this.t4241 * this.t6769
                + 0.1744505118720e13 * this.t948 * this.t6838
                + 0.17381844480000e14 * this.t987 * this.t6774 + 0.376388812800e12 * this.t4188 * this.t6769
                + 0.8086478400e10 * this.t1167
                * this.t6467 - 0.11096445360000e14 * this.t847 * this.t6604 + 0.8877156288000e13 * this.t103
                * this.t6604 + 0.83071672320e11
                * this.t685 * this.t6851 + 0.13905475584000e14 * this.t813 * this.t6854 - 0.41120477798400e14
                * this.t4228 * this.t6769
                + 0.27413651865600e14 * this.t729 * this.t6769 + 0.95947781529600e14 * this.t993 * this.t6861
                - 0.47973890764800e14 * this.t942
                * this.t6861 + 0.38379112611840e14 * this.t1009 * this.t6866;
        this.t6879 = this.t36 * iy;
        this.t6882 = this.t509 * this.t13;
        this.t6895 = this.t96 * ey;
        this.t6904 = this.t268 * this.t98;
        this.t6907 =
            -0.19189556305920e14 * this.t1006 * this.t6866 + 0.89994311680e11 * this.t79 * this.t513 * this.t326
                + 0.347636889600e12 * this.t641
                * this.t6459 + 0.476759162880e12 * this.t598 * this.t6488 - 0.198198000e9 * this.t69 * this.t6879
                + 0.446961715200e12 * this.t508
                * this.t6882 - 0.3027024000e10 * this.t59 * this.t6879 - 0.287332531200e12 * this.t670 * this.t6669
                + 0.4380175800e10 * this.t529
                * this.t6459 + 0.794598604800e12 * this.t2288 * this.t6751 - 0.595948953600e12 * this.t875 * this.t6751
                - 0.2383795814400e13
                * this.t561 * this.t6895 + 0.35637747425280e14 * this.t1050 * this.t6374 + 0.3198259384320e13
                * this.t1006 * this.t6701
                - 0.7151387443200e13 * this.t1009 * this.t6794 - 0.581501706240e12 * this.t1012 * this.t6904;
        this.t6922 = this.t220 * this.t45;
        this.t6925 = this.t338 * iy;
        this.t6930 = this.t375 * this.t112;
        this.t6933 = this.t210 * this.t13;
        this.t6936 = this.t9 * iy;
        this.t6943 = this.t199 * iy;
        this.t6946 = this.t385 * this.t45;
        this.t6949 =
            -0.94097203200e11 * this.t4346 * this.t6764 - 0.313657344000e12 * this.t766 * this.t6803
                - 0.235243008000e12 * this.t1277
                * this.t6748 - 0.23063040e8 * this.t556 * iy + 0.64691827200e11 * this.t278 * this.t6459
                - 0.6952737792000e13 * this.t981
                * this.t6922 - 0.62303754240e11 * this.t984 * this.t6925 - 0.1787846860800e13 * this.t987 * this.t6704
                - 0.4171642675200e13
                * this.t1015 * this.t6930 - 0.249215016960e12 * this.t1018 * this.t6933 + 0.23063040e8 * this.t6936
                * this.t19 - 0.518710832640e12
                * this.t1048 * this.t6604 + 0.345807221760e12 * this.t178 * this.t6604 + 0.10280119449600e14
                * this.t748 * this.t6943
                + 0.7351344000e10 * this.t700 * this.t6946;
        this.t6956 = this.t325 * this.t188;
        this.t6982 =
            0.432432000e9 * this.t703 * this.t6794 + 0.63408259200e11 * this.t1116 * this.t6895 - 0.39630162000e11
                * this.t124 * this.t6459
                - 0.28378521600e11 * this.t798 * this.t6956 - 0.8821612800e10 * this.t5796 * this.t6764
                - 0.31183028997120e14 * this.t135
                * this.t6467 + 0.20788685998080e14 * this.t1090 * this.t6467 - 0.31365734400e11 * this.t27 * this.t112
                * this.t43
                - 0.1738184448000e13 * this.t1222 * this.t6604 + 0.1390547558400e13 * this.t561 * this.t6604
                + 0.248312064000e12 * this.t636
                * this.t6464 + 0.17208026035200e14 * this.t73 * this.t6467 - 0.10324815621120e14 * this.t95
                * this.t6467 - 0.5765760000e10
                * this.t1159 * this.t6459 + 0.5899894640640e13 * this.t958 * this.t6604;
        this.t6996 = this.t74 * this.t97;
        this.t7004 = this.t163 * this.t43;
        this.t7011 = this.t119 * this.t97;
        this.t7024 =
            -0.4424920980480e13 * this.t171 * this.t6604 - 0.7151387443200e13 * this.t993 * this.t6943
                - 0.3575693721600e13 * this.t996
                * this.t6798 - 0.188194406400e12 * this.t928 * this.t6794 - 0.768768000e9 * this.t15 * this.t98
                * this.t8 - 0.125462937600e12 * this.t27
                * this.t45 * this.t6996 - 0.290750853120e12 * this.t17 * this.t326 * this.t163 * this.t156
                - 0.3476368896000e13 * this.t40 * this.t112 * this.t7004
                - 0.125462937600e12 * this.t27 * this.t13 * this.t6895 - 0.2781095116800e13 * this.t40 * this.t45
                * this.t7011 - 0.39207168000e11
                * this.t1224 * this.t6604 + 0.397299302400e12 * this.t1211 * this.t6604 - 0.297974476800e12 * this.t564
                * this.t6604
                - 0.685341296640e12 * this.t582 * this.t6464 + 0.228447098880e12 * this.t572 * this.t6464;
        this.t7039 = this.t365 * this.t98;
        this.t7046 = this.t380 * this.t326;
        this.t7053 = this.t343 * iy;
        this.t7056 = this.t348 * this.t157;
        this.t7059 =
            0.1638859622400e13 * this.t95 * this.t6669 - 0.3027024000e10 * this.t1167 * this.t6459
                - 0.15991296921600e14 * this.t726 * this.t6704
                - 0.31982593843200e14 * this.t729 * this.t6798 + 0.26728310568960e14 * this.t970 * this.t6604
                - 0.22273592140800e14 * this.t1107
                * this.t6604 + 0.15375360e8 * this.t1041 * iy + 0.219560140800e12 * this.t364 * this.t7039
                + 0.219560140800e12 * this.t369
                * this.t6798 + 0.446961715200e12 * this.t374 * this.t6930 + 0.59594895360e11 * this.t379 * this.t7046
                + 0.109780070400e12 * this.t384
                * this.t6946 + 0.14898723840e11 * this.t337 * this.t6925 + 0.148987238400e12 * this.t342 * this.t7053
                + 0.139054755840e12 * this.t347
                * this.t7056;
        this.t7062 = this.t353 * this.t98;
        this.t7066 = this.t96 * this.t111;
        this.t7077 = this.t96 * this.t97;
        this.t7082 = this.t85 * ey;
        this.t7093 = this.t74 * this.t111;
        this.t7097 = this.t139 * this.t43;
        this.t7104 = this.t3019 * ey;
        this.t7108 = this.t139 * this.t8;
        this.t7114 =
            0.744936192000e12 * this.t352 * this.t7062 - 0.2781095116800e13 * this.t40 * this.t157 * this.t7066
                - 0.62303754240e11 * this.t17
                * this.t98 * this.t550 * this.t8 - 0.1787846860800e13 * this.t16 * this.t98 * this.t6681
                - 0.2383795814400e13 * this.t16 * this.t45 * this.t7077
                - 0.357569372160e12 * this.t636 * this.t6669 + 0.456894197760e12 * this.t578 * this.t7082
                - 0.80720640e8 * this.t11 * this.t13
                * this.t6459 - 0.62303754240e11 * this.t17 * this.t189 * this.t139 * this.t185 - 0.715138744320e12
                * this.t16 * this.t157 * this.t7093
                - 0.1787846860800e13 * this.t16 * this.t112 * this.t7097 - 0.715138744320e12 * this.t16 * this.t13
                * this.t6669 - 0.13845278720e11
                * this.t17 * this.t13 * this.t7104 - 0.188194406400e12 * this.t27 * this.t98 * this.t7108 - 0.5045040e7
                * this.t68 * this.t13 * this.t6459;
        this.t7120 = this.t536 * this.t8;
        this.t7151 =
            -0.166143344640e12 * this.t17 * this.t188 * this.t96 * this.t324 - 0.1390547558400e13 * this.t40 * this.t98
                * this.t7120
                - 0.348901023744e12 * this.t17 * this.t157 * this.t119 * this.t111 + 0.956189561600e12 * this.t4125
                * this.t6467 - 0.6952737792000e13
                * this.t948 * this.t7062 + 0.15991296921600e14 * this.t939 * this.t7062 + 0.9594778152960e13
                * this.t942 * this.t6930
                - 0.872252559360e12 * this.t945 * this.t6698 + 0.1655413760e10 * this.t838 * this.t189
                + 0.4843238400e10 * this.t35 * this.t6895
                + 0.2522520e7 * this.t553 * this.t6459 + 0.3547315200e10 * this.t72 * this.t7082 - 0.6922639360e10
                * this.t572 * this.t7104
                + 0.230538147840e12 * this.t1159 * this.t6467 - 0.295905209600e12 * this.t103 * this.t6895;
        this.t7183 =
            0.790164460800e12 * this.t171 * this.t6895 - 0.37924024320e11 * this.t641 * iy + 0.1609062174720e13
                * this.t138 * this.t6895
                - 0.3155425553280e13 * this.t1079 * this.t6459 - 0.36419102720e11 * this.t1061 * this.t7082
                - 0.6883210414080e13 * this.t1050
                * this.t6895 - 0.337478668800e12 * this.t1074 * this.t6669 + 0.46774543495680e14 * this.t1036
                * this.t6604 - 0.38978786246400e14
                * this.t144 * this.t6604 - 0.1434284342400e13 * this.t1098 * this.t6467 + 0.37924024320e11 * this.t5497
                * iy - 0.134991467520e12
                * this.t95 * this.t6464 + 0.44997155840e11 * this.t5582 * this.t6464 + 0.18932553319680e14 * this.t1034
                * this.t6604
                - 0.15777127766400e14 * this.t275 * this.t6604;
        this.t7219 =
            0.105859353600e12 * this.t91 * this.t6467 - 0.121080960e9 * this.t10 * this.t98 * this.t8
                - 0.134991467520e12 * this.t94 * this.t513
                * this.t189 + 0.44997155840e11 * this.t79 * this.t6692 - 0.198198000e9 * this.t69 * this.t6604
                - 0.3218124349440e13 * this.t149
                * this.t6571 + 0.1609062174720e13 * this.t72 * this.t6625 + 0.15682867200e11 * this.t149 * this.t6669
                - 0.184504320e9 * this.t9 * this.t13
                * this.t6459 - 0.35286451200e11 * this.t28 * this.t6467 + 0.11531520000e11 * this.t556 * this.t6547
                + 0.739763024000e12 * this.t49
                * this.t6568 - 0.443857814400e12 * this.t41 * this.t6571 + 0.6054048000e10 * this.t543 * this.t6547
                - 0.9755116800e10 * this.t54
                * this.t6488 + 0.396396000e9 * this.t553 * this.t6547;
        this.t7233 = this.t288 * this.t98;
        this.t7236 = this.t238 * iy;
        this.t7239 = this.t486 * this.t112;
        this.t7258 =
            -0.99324825600e11 * this.t587 * this.t6459 + 0.3426706483200e13 * this.t608 * this.t6895 + 0.2018016000e10
                * this.t90 * this.t6895
                - 0.62731468800e11 * this.t598 * this.t6895 - 0.4454718428160e13 * this.t79 * this.t6594
                + 0.1609062174720e13 * this.t72 * this.t7233
                + 0.109780070400e12 * this.t149 * this.t7236 - 0.287332531200e12 * this.t72 * this.t7239
                + 0.5568398035200e13 * this.t107
                * this.t6600 - 0.13845278720e11 * this.t17 * this.t630 * this.t74 * this.t186 - 0.29265350400e11
                * this.t54 * this.t7239
                + 0.5592608461440e13 * this.t127 * this.t6588 - 0.8475667200e10 * this.t90 * this.t6879
                + 0.790164460800e12 * this.t54 * this.t7233
                + 0.64691827200e11 * this.t131 * this.t7236;
        this.t7290 =
            0.1877239203840e13 * this.t582 * this.t6669 - 0.92252160e8 * this.t9 * this.t98 * this.t8
                - 0.70572902400e11 * this.t28 * this.t6641
                - 0.570674332800e12 * this.t49 * this.t6597 - 0.2969812285440e13 * this.t79 * this.t6656
                + 0.4380175800e10 * this.t285 * this.t7236
                + 0.63408259200e11 * this.t49 * this.t7233 + 0.10394342999040e14 * this.t79 * this.t6600
                + 0.1537536000e10 * this.t1220 * this.t6604
                + 0.121080960e9 * this.t532 * this.t6459 + 0.1470268800e10 * this.t28 * this.t6669 - 0.7795757249280e13
                * this.t107 * this.t6594
                - 0.1191897907200e13 * this.t359 * this.t6854 + 0.744936192000e12 * this.t219 * this.t6922
                - 0.1474973660160e13 * this.t54
                * this.t6597;
        this.t7300 = this.t486 * this.t326;
        this.t7313 = this.t187 * this.t188;
    }

    /**
     * Partial derivative due to 12th order Earth potential zonal harmonics.
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
    private final void derParUdeg12_12(final double a, final double ex, final double ey, final double ix,
                                       final double iy) {
        this.t7324 =
            0.172903610880e12 * this.t28 * this.t7236 - 0.153692098560e12 * this.t149 * this.t6641 - 0.329340211200e12
                * this.t35 * this.t6585
                + 0.219560140800e12 * this.t149 * this.t6547 + 0.383110041600e12 * this.t149 * this.t7300
                - 0.16705194105600e14 * this.t127
                * this.t6617 + 0.11136796070400e14 * this.t107 * this.t6568 + 0.39020467200e11 * this.t28 * this.t7300
                - 0.27963042307200e14
                * this.t54 * this.t6507 + 0.22370433845760e14 * this.t127 * this.t6565 - 0.62303754240e11 * this.t887
                * this.t7313
                - 0.4171642675200e13 * this.t890 * this.t6882 - 0.41932800e8 * this.t5507 * iy - 0.31982593843200e14
                * this.t896 * this.t7039
                - 0.15991296921600e14 * this.t900 * this.t6946;
        this.t7351 = this.t139 * this.t3328;
        this.t7354 = this.t185 * this.t326;
        this.t7357 =
            0.58150170624e11 * this.t614 * iy + 0.8760351600e10 * this.t285 * this.t6547 - 0.126816518400e12
                * this.t131 * this.t6571
                + 0.63408259200e11 * this.t49 * this.t6625 - 0.31183028997120e14 * this.t94 * this.t6617
                + 0.20788685998080e14 * this.t79
                * this.t6568 - 0.5765760000e10 * this.t35 * this.t6879 + 0.6918912000e10 * this.t262 * this.t6794
                + 0.139054755840e12 * this.t267
                * this.t6904 + 0.14898723840e11 * this.t184 * this.t7313 + 0.6918912000e10 * this.t198 * this.t6943
                + 0.59594895360e11 * this.t209
                * this.t6933 - 0.9029529600e10 * this.t5644 * iy + 0.1889880545280e13 * this.t80 * this.t7351
                + 0.456894197760e12 * this.t578
                * this.t7354;
        this.t7371 = this.t74 * this.t324;
        this.t7394 =
            0.950476800e9 * this.t6139 * iy + 0.2306304000e10 * this.t835 * this.t98 - 0.115315200e9 * this.t532 * iy
                - 0.3128732006400e13 * this.t564 * this.t6488 + 0.1251492802560e13 * this.t604 * this.t6488
                - 0.397299302400e12 * this.t40
                * this.t188 * this.t7371 - 0.242161920e9 * this.t10 * this.t13 * this.t6459 + 0.46774543495680e14
                * this.t127 * this.t6537
                - 0.38978786246400e14 * this.t107 * this.t6507 + 0.2282697331200e13 * this.t131 * this.t6565
                - 0.1712022998400e13 * this.t49
                * this.t6585 + 0.5939624570880e13 * this.t94 * this.t6622 - 0.2969812285440e13 * this.t79 * this.t6559
                - 0.13140527400e11 * this.t69
                * this.t6585 + 0.2774772e7 * this.t803 * iy * this.t19;
        this.t7401 = this.t210 * this.t45;
        this.t7404 = this.t215 * this.t13;
        this.t7427 =
            -0.16554137600e11 * this.t323 * this.t6851 - 0.13837824000e11 * this.t925 * this.t6756 - 0.9225216000e10
                * this.t2349 * this.t6769
                - 0.198649651200e12 * this.t214 * this.t7401 - 0.170271129600e12 * this.t1812 * this.t7404
                - 0.1191897907200e13 * this.t369
                * this.t6742 + 0.5899894640640e13 * this.t28 * this.t6565 - 0.4424920980480e13 * this.t54 * this.t6585
                + 0.105859353600e12 * this.t90
                * this.t6625 + 0.17208026035200e14 * this.t72 * this.t6568 - 0.10324815621120e14 * this.t94
                * this.t6571 - 0.518710832640e12
                * this.t90 * this.t6585 + 0.345807221760e12 * this.t28 * this.t6547 + 0.230538147840e12 * this.t35
                * this.t6625 - 0.76846049280e11
                * this.t149 * this.t6467;
        this.t7429 = this.t255 * this.t13;
        this.t7434 = this.t268 * this.t112;
        this.t7457 = this.t185 * this.t189;
        this.t7462 =
            -0.16554137600e11 * this.t2515 * this.t7429 - 0.9225216000e10 * this.t193 * this.t6806 - 0.347636889600e12
                * this.t342 * this.t7434
                - 0.2774772e7 * this.t1025 * iy - 0.198649651200e12 * this.t604 * this.t7354 - 0.2383795814400e13
                * this.t561 * this.t2645
                + 0.26138112000e11 * this.t598 * this.t2722 - 0.768768000e9 * this.t601 * this.t2637
                + 0.2741365186560e13 * this.t1228 * this.t2722
                - 0.2284470988800e13 * this.t608 * this.t2722 - 0.1738184448000e13 * this.t1222 * this.t2722
                + 0.1390547558400e13 * this.t561
                * this.t2722 - 0.4843238400e10 * this.t556 * this.t3284 + 0.248312064000e12 * this.t636 * this.t7457
                - 0.10280119449600e14
                * this.t641 * this.t3284;
        this.t7465 = this.t10 * iy;
        this.t7472 = this.t188 * this.t98;
        this.t7499 = this.t21 * iy;
        this.t7502 =
            0.6853412966400e13 * this.t611 * this.t3284 - 0.121080960e9 * this.t7465 * this.t139 - 0.3547315200e10
                * this.t149 * this.t7457
                - 0.92252160e8 * this.t6936 * this.t139 + 0.8307167232e10 * this.t604 * this.t629 * this.t7472
                - 0.768768000e9 * this.t15 * iy * this.t139
                + 0.6396518768640e13 * this.t561 * this.t3290 - 0.3198259384320e13 * this.t578 * this.t3290
                - 0.119189790720e12 * this.t16 * iy
                * this.t536 + 0.94097203200e11 * this.t601 * this.t3284 + 0.397299302400e12 * this.t1211 * this.t2722
                - 0.297974476800e12 * this.t564
                * this.t2722 - 0.49662412800e11 * this.t40 * iy * this.t550 - 0.1191897907200e13 * this.t648
                * this.t3284 + 0.595948953600e12
                * this.t636 * this.t3284 - 0.2458289433600e13 * this.t28 * this.t7499;
        this.t7530 = this.t22 * iy;
        this.t7539 =
            0.2458289433600e13 * this.t54 * this.t6809 - 0.446961715200e12 * this.t149 * this.t7499 + 0.446961715200e12
                * this.t72 * this.t6809
                + 0.3426706483200e13 * this.t608 * this.t2645 + 0.40360320e8 * this.t543 * this.t2637 + 0.1537536000e10
                * this.t1220 * this.t2722
                - 0.95777510400e11 * this.t72 * this.t3290 - 0.9755116800e10 * this.t54 * this.t3290 - 0.8475667200e10
                * this.t90 * this.t2722
                - 0.9104775680e10 * this.t94 * this.t7457 - 0.2695492800e10 * this.t131 * this.t3284
                + 0.13981521153600e14 * this.t49 * this.t7530
                - 0.13981521153600e14 * this.t41 * this.t7499 - 0.1070014374000e13 * this.t285 * this.t7499
                + 0.1070014374000e13 * this.t123
                * this.t6809;
        this.t7542 = this.t20 * iy;
        this.t7545 = this.t913 * iy;
        this.t7576 =
            0.105124219200e12 * this.t69 * this.t6809 - 0.105124219200e12 * this.t285 * this.t7542 - 0.476899543848e12
                * this.t804 * this.t7545
                + 0.476899543848e12 * this.t920 * this.t7530 - 0.2306304000e10 * this.t4006 * this.t31 * iy
                + 0.4715989278000e13 * this.t123
                * this.t7530 - 0.4715989278000e13 * this.t174 * this.t7499 - 0.16705194105600e14 * this.t127
                * this.t7545 + 0.16705194105600e14
                * this.t107 * this.t7530 + 0.298679320940e12 * this.t1028 * this.t7530 + 0.476759162880e12 * this.t598
                * this.t3290 - 0.5765760000e10
                * this.t35 * this.t2722 - 0.1655413760e10 * this.t5532 * this.t574 * iy - 0.21956014080e11 * this.t5232
                * this.t64 * iy
                - 0.21283891200e11 * this.t4974 * this.t230 * iy;
        this.t7607 =
            0.45741696000e11 * this.t35 * this.t6809 - 0.45741696000e11 * this.t149 * this.t7542 - 0.2969812285440e13
                * this.t94 * this.t7545
                + 0.2969812285440e13 * this.t79 * this.t7530 - 0.30750720e8 * this.t6324 - 0.3027024000e10 * this.t59
                * this.t2722
                - 0.198198000e9 * this.t69 * this.t2722 - 0.22087978872960e14 * this.t41 * this.t7545
                + 0.22087978872960e14 * this.t18 * this.t7530
                + 0.10652587545600e14 * this.t54 * this.t7530 - 0.10652587545600e14 * this.t127 * this.t7499
                - 0.1921920000e10 * this.t556
                * this.t7542 + 0.15375360e8 * this.t193 * this.t194 + 0.2306304000e10 * this.t204 * this.t205
                + 0.21283891200e11 * this.t214 * this.t215;
        this.t7629 = this.t343 * this.t98;
        this.t7641 =
            0.1655413760e10 * this.t253 * this.t255 + 0.2522520e7 * this.t553 * this.t2637 + 0.3547315200e10 * this.t72
                * this.t7354
                - 0.6922639360e10 * this.t572 * this.t629 * this.t189 - 0.35286451200e11 * this.t28 * this.t3284
                + 0.13069056000e11 * this.t648
                * this.t2637 - 0.347636889600e12 * this.t352 * this.t6838 - 0.170271129600e12 * this.t384 * this.t6956
                - 0.329340211200e12
                * this.t1031 * this.t6748 - 0.74493619200e11 * this.t852 * this.t6745 - 0.595948953600e12 * this.t844
                * this.t7629 - 0.15375360e8
                * this.t6604 + 0.8194298112000e13 * this.t95 * this.t6317 - 0.211718707200e12 * this.t178 * this.t6672
                - 0.166143344640e12 * this.t17
                * this.t45 * this.t85 * this.t97;
        this.t7655 = this.t74 * this.t2710;
        this.t7676 =
            -0.1655413760e10 * this.t5336 * this.t7472 - 0.227095020880e12 * this.t1194 * this.t6459 - 0.49662412800e11
                * this.t40 * this.t189
                * this.t185 + 0.6555438489600e13 * this.t1061 * this.t6374 - 0.2873325312000e13 * this.t670
                * this.t6722 + 0.8194298112000e13
                * this.t95 * this.t7655 - 0.146326752000e12 * this.t162 * this.t7655 - 0.456894197760e12 * this.t614
                * this.t2637 + 0.347636889600e12
                * this.t641 * this.t2637 + 0.4843238400e10 * this.t35 * this.t2645 + 0.2018016000e10 * this.t90
                * this.t2645 - 0.357569372160e12
                * this.t636 * this.t3328 - 0.99324825600e11 * this.t587 * this.t2637 + 0.595948953600e12 * this.t564
                * this.t2645 - 0.19372953600e11
                * this.t711 * this.t6806;
        this.t7677 = iy * this.t19;
        this.t7709 =
            0.257657400e9 * this.t972 * this.t7677 + 0.1921920000e10 * this.t35 * this.t7677 - 0.67371275400e11
                * this.t962 * this.t7499
                + 0.67371275400e11 * this.t1028 * this.t6809 + 0.6570263700e10 * this.t972 * this.t6809
                - 0.6570263700e10 * this.t962 * this.t7542
                - 0.3195776263680e13 * this.t131 * this.t7499 + 0.3195776263680e13 * this.t49 * this.t6809
                - 0.12713500800e11 * this.t543
                * this.t7542 + 0.12713500800e11 * this.t59 * this.t7677 - 0.4162158000e10 * this.t553 * this.t7542
                + 0.4162158000e10 * this.t69
                * this.t7677 - 0.1384527872e10 * this.t17 * this.t7472 * this.t629 - 0.144144000e9 * this.t543
                * this.t3284 + 0.5959489536000e13
                * this.t587 * this.t3284;
        this.t7743 =
            -0.3575693721600e13 * this.t582 * this.t3284 - 0.1384527872e10 * this.t17 * iy * this.t575
                + 0.21956014080e11 * this.t359 * this.t360
                + 0.1912002892800e13 * this.t72 * this.t7530 - 0.1912002892800e13 * this.t94 * this.t7499
                + 0.247005158400e12 * this.t90 * this.t6809
                - 0.247005158400e12 * this.t28 * this.t7542 - 0.7494135689040e13 * this.t174 * this.t7545
                + 0.7494135689040e13 * this.t430
                * this.t7530 - 0.316989953280e12 * this.t131 * this.t7542 - 0.10090080000e11 * this.t532 * this.t7542
                + 0.10090080000e11 * this.t90
                * this.t7677 - 0.257657400e9 * this.t1025 * this.t7542 - 0.40360320e8 * this.t11 * this.t98 * this.t8
                - 0.76846049280e11 * this.t149
                * this.t3284;
        this.t7762 = this.t11 * iy;
        this.t7769 = this.t68 * iy;
        this.t7774 = this.t348 * this.t188;
        this.t7779 =
            0.1470268800e10 * this.t28 * this.t3328 + 0.15682867200e11 * this.t149 * this.t3328 + 0.92252160e8
                * this.t556 * this.t2637
                + 0.144144000e9 * this.t59 * this.t2645 + 0.121080960e9 * this.t532 * this.t2637 - 0.685341296640e12
                * this.t582 * this.t7457
                + 0.228447098880e12 * this.t572 * this.t7457 - 0.3128732006400e13 * this.t564 * this.t3290
                + 0.1251492802560e13 * this.t604
                * this.t3290 - 0.40360320e8 * this.t7762 * this.t139 - 0.1470268800e10 * this.t90 * this.t3290
                - 0.15682867200e11 * this.t35 * this.t3290
                - 0.2522520e7 * this.t7769 * this.t139 + 0.21956014080e11 * this.t849 * this.t112 - 0.198649651200e12
                * this.t219 * this.t7774
                - 0.1489872384000e13 * this.t330 * this.t6774;
        this.t7815 =
            -0.131736084480e12 * this.t1270 * this.t6764 - 0.329340211200e12 * this.t198 * this.t6861
                + 0.6952737792000e13 * this.t1933
                * this.t7629 - 0.290750853120e12 * this.t17 * this.t112 * this.t536 * this.t43 - 0.397299302400e12
                * this.t40 * this.t13 * this.t7082
                - 0.559104e6 * this.t14 * iy + 0.559104e6 * this.t7677 + 0.21283891200e11 * this.t832 * this.t326
                + 0.2298660249600e13
                * this.t1184 * this.t6346 - 0.5737137369600e13 * this.t1098 * this.t6383 + 0.3824758246400e13
                * this.t4125 * this.t6383
                + 0.16705194105600e14 * this.t144 * this.t6672 + 0.234122803200e12 * this.t178 * this.t6374
                - 0.6883210414080e13 * this.t94
                * this.t6591 + 0.2549337190400e13 * this.t670 * this.t6491;
        this.t7846 =
            0.4049744025600e13 * this.t128 * this.t6346 - 0.2024872012800e13 * this.t4310 * this.t6346
                + 0.423437414400e12 * this.t91
                * this.t6383 + 0.364191027200e12 * this.t670 * this.t6472 + 0.68832104140800e14 * this.t73 * this.t6314
                - 0.41299262484480e14
                * this.t95 * this.t6314 - 0.6321315686400e13 * this.t1189 * this.t6383 + 0.3160657843200e13 * this.t162
                * this.t6383
                + 0.103248156211200e15 * this.t73 * this.t6337 - 0.61948893726720e14 * this.t95 * this.t6337
                - 0.10652587545600e14 * this.t128
                * this.t6524 - 0.292653504000e12 * this.t162 * this.t6722 + 0.10124360064000e14 * this.t128
                * this.t6349 - 0.5062180032000e13
                * this.t4310 * this.t6349 - 0.6321315686400e13 * this.t1189 * this.t6314;
        this.t7878 =
            0.3160657843200e13 * this.t162 * this.t6314 - 0.9449402726400e13 * this.t95 * this.t6435
                + 0.3149800908800e13 * this.t5582
                * this.t6435 - 0.3779761090560e13 * this.t95 * this.t6438 + 0.1259920363520e13 * this.t5582
                * this.t6438 - 0.124732115988480e15
                * this.t135 * this.t6383 + 0.83154743992320e14 * this.t1090 * this.t6383 + 0.10124360064000e14
                * this.t128 * this.t6320
                - 0.5062180032000e13 * this.t4310 * this.t6320 - 0.12872497397760e14 * this.t150 * this.t6314
                + 0.6436248698880e13 * this.t670
                * this.t6314 + 0.68832104140800e14 * this.t73 * this.t6383 - 0.41299262484480e14 * this.t95
                * this.t6383 - 0.8605706054400e13
                * this.t1098 * this.t6337 + 0.5737137369600e13 * this.t4125 * this.t6337;
        this.t7897 = this.t536 * this.t2637;
        this.t7910 =
            -0.760899110400e12 * this.t278 * this.t6337 + 0.380449555200e12 * this.t5652 * this.t6337
                + 0.37865106639360e14 * this.t1034
                * this.t6324 - 0.31554255532800e14 * this.t275 * this.t6324 - 0.4438578144000e13 * this.t171
                * this.t6349 + 0.1775431257600e13
                * this.t4289 * this.t6349 - 0.764801157120e12 * this.t1061 * this.t7351 + 0.887715628800e12
                * this.t1058 * this.t7655
                - 0.1436662656000e13 * this.t670 * this.t7655 - 0.254933719040e12 * this.t1061 * this.t7897
                + 0.31183028997120e14 * this.t1107
                * this.t6672 - 0.14849061427200e14 * this.t1090 * this.t6317 - 0.29698122854400e14 * this.t1090
                * this.t6722 - 0.507266073600e12
                * this.t278 * this.t6383 + 0.4438578144000e13 * this.t153 * this.t6337;
        this.t7925 = this.t74 * this.t2952;
        this.t7943 =
            -0.2663146886400e13 * this.t4620 * this.t6337 + 0.2959052096000e13 * this.t153 * this.t6314
                - 0.1775431257600e13 * this.t4620
                * this.t6314 - 0.12872497397760e14 * this.t150 * this.t6383 + 0.6436248698880e13 * this.t670
                * this.t6383 + 0.190224777600e12
                * this.t1116 * this.t6524 - 0.254933719040e12 * this.t1061 * this.t7925 + 0.1775431257600e13
                * this.t1058 * this.t6722
                + 0.2367241676800e13 * this.t4289 * this.t6367 + 0.629960181760e12 * this.t80 * this.t7925
                - 0.16172956800e11 * this.t132
                * this.t6672 - 0.15375360e8 * this.t2722 + 0.432432000e9 * this.t763 * this.t6943 + 0.1787846860800e13
                * this.t769 * this.t6943
                - 0.581501706240e12 * this.t772 * this.t7056;
        this.t7974 =
            0.45405360e8 * this.t7769 * this.t19 + 0.9029529600e10 * this.t587 * iy - 0.439120281600e12 * this.t204
                * this.t6803
                - 0.595948953600e12 * this.t364 * this.t6779 - 0.5765760000e10 * this.t35 * this.t6604
                + 0.8086478400e10 * this.t59 * this.t6625
                + 0.109780070400e12 * this.t150 * this.t6459 - 0.570674332800e12 * this.t153 * this.t6459
                - 0.2969812285440e13 * this.t1090
                * this.t6669 - 0.3550862515200e13 * this.t128 * this.t6895 + 0.3277719244800e13 * this.t135
                * this.t6459 + 0.172903610880e12
                * this.t1189 * this.t6459 + 0.478094780800e12 * this.t275 * this.t6895 + 0.1638859622400e13 * this.t94
                * this.t6638
                + 0.157199642600e12 * this.t175 * this.t6459;
        this.t7987 = this.t380 * this.t189;
    }

    /**
     * Partial derivative due to 12th order Earth potential zonal harmonics.
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
    private final void derParUdeg12_13(final double a, final double ex, final double ey, final double ix,
                                       final double iy) {
        this.t8008 =
            -0.70572902400e11 * this.t178 * this.t6895 + 0.2219289072000e13 * this.t1098 * this.t6459
                + 0.157199642600e12 * this.t174
                * this.t6588 - 0.21283891200e11 * this.t5328 * this.t189 - 0.58530700800e11 * this.t54 * this.t13
                * this.t6669 + 0.373822525440e12
                * this.t1015 * this.t7987 - 0.26281054800e11 * this.t1046 * this.t6324 + 0.17520703200e11 * this.t5770
                * this.t6324
                - 0.388150963200e12 * this.t801 * this.t6324 + 0.258767308800e12 * this.t132 * this.t6324
                + 0.10394342999040e14 * this.t1107
                * this.t6895 - 0.7795757249280e13 * this.t108 * this.t6459 - 0.893923430400e12 * this.t73 * this.t6459
                + 0.89994311680e11 * this.t80
                * this.t7082 - 0.8475667200e10 * this.t91 * this.t6459;
        this.t8016 = this.t314 * this.t45;
        this.t8049 =
            -0.16388596224000e14 * this.t909 * this.t6604 + 0.13110876979200e14 * this.t1050 * this.t6604
                - 0.574665062400e12 * this.t72
                * this.t13 * this.t6669 + 0.208582133760e12 * this.t313 * this.t8016 - 0.16951334400e11 * this.t90
                * this.t13 * this.t6459
                - 0.195102336000e12 * this.t54 * this.t45 * this.t7077 - 0.211718707200e12 * this.t28 * this.t98
                * this.t7108 - 0.461076295680e12
                * this.t149 * this.t98 * this.t7108 - 0.11531520000e11 * this.t35 * this.t13 * this.t6459
                - 0.6054048000e10 * this.t59 * this.t13 * this.t6459
                - 0.28378521600e11 * this.t4627 * this.t7404 - 0.99324825600e11 * this.t751 * this.t6779
                - 0.198649651200e12 * this.t863 * this.t6742
                + 0.83071672320e11 * this.t4543 * this.t7429 + 0.564583219200e12 * this.t2002 * this.t6756
                - 0.41120477798400e14 * this.t990
                * this.t6806;
        this.t8094 = this.t139 * this.t156;
        this.t8097 =
            0.27413651865600e14 * this.t900 * this.t6806 - 0.29059430400e11 * this.t2461 * this.t6756
                - 0.99324825600e11 * this.t2464
                * this.t7629 - 0.1436662656000e13 * this.t72 * this.t112 * this.t7097 - 0.307384197120e12 * this.t149
                * this.t13 * this.t6895
                - 0.307384197120e12 * this.t149 * this.t45 * this.t6996 - 0.509867438080e12 * this.t94 * this.t45
                * this.t7011 - 0.637334297600e12
                * this.t94 * this.t112 * this.t7004 - 0.146326752000e12 * this.t54 * this.t112 * this.t7097
                - 0.396396000e9 * this.t69 * this.t13 * this.t6459
                - 0.10781971200e11 * this.t131 * this.t13 * this.t6895 - 0.16172956800e11 * this.t131 * this.t98
                * this.t7108 - 0.254933719040e12
                * this.t94 * this.t98 * this.t7120 - 0.574665062400e12 * this.t72 * this.t157 * this.t7093
                - 0.254933719040e12 * this.t94 * this.t326 * this.t8094;
        this.t8136 =
            -0.72838205440e11 * this.t94 * this.t188 * this.t7371 - 0.1915550208000e13 * this.t72 * this.t45
                * this.t7077 - 0.58530700800e11
                * this.t54 * this.t157 * this.t7093 - 0.72838205440e11 * this.t94 * this.t13 * this.t7082
                - 0.509867438080e12 * this.t94 * this.t157 * this.t7066
                - 0.141145804800e12 * this.t28 * this.t45 * this.t6996 + 0.2860554977280e13 * this.t928 * this.t6866
                + 0.7151387443200e13 * this.t826
                * this.t6861 + 0.2860554977280e13 * this.t4418 * this.t6764 - 0.38379112611840e14 * this.t933
                * this.t6854 + 0.12793037537280e14
                * this.t741 * this.t6854 - 0.5482730373120e13 * this.t4426 * this.t7404 + 0.1827576791040e13
                * this.t1018 * this.t7404
                - 0.146326752000e12 * this.t54 * this.t98 * this.t6681 - 0.141145804800e12 * this.t28 * this.t13
                * this.t6895;
        this.t8169 =
            -0.10781971200e11 * this.t131 * this.t45 * this.t6996 - 0.864864000e9 * this.t1626 * this.t6756
                - 0.19372953600e11 * this.t4651
                * this.t6769 - 0.7151387443200e13 * this.t1957 * this.t6756 + 0.3575693721600e13 * this.t810
                * this.t6756 + 0.996860067840e12
                * this.t981 * this.t7774 - 0.22054032000e11 * this.t2499 * this.t6748 - 0.29405376000e11 * this.t903
                * this.t6803 - 0.22054032000e11
                * this.t872 * this.t6861 - 0.8072064000e10 * this.t688 * this.t6806 - 0.94097203200e11 * this.t906
                * this.t6866 - 0.235243008000e12
                * this.t775 * this.t6861 - 0.19189556305920e14 * this.t2005 * this.t7629 + 0.5482730373120e13
                * this.t2518 * this.t6751
                - 0.4568941977600e13 * this.t829 * this.t6751;
        this.t8190 = this.t314 * this.t157;
        this.t8201 =
            0.95947781529600e14 * this.t2025 * this.t6748 - 0.47973890764800e14 * this.t939 * this.t6748
                + 0.127930375372800e15 * this.t936
                * this.t6803 - 0.63965187686400e14 * this.t744 * this.t6803 - 0.4767591628800e13 * this.t4798
                * this.t6769 + 0.2383795814400e13
                * this.t996 * this.t6769 - 0.198649651200e12 * this.t857 * this.t6854 - 0.576576000e9 * this.t5134
                * this.t6769 - 0.46930980096000e14
                * this.t1993 * this.t6748 + 0.18772392038400e14 * this.t948 * this.t6748 + 0.2093406142464e13
                * this.t890 * this.t8190
                - 0.38379112611840e14 * this.t679 * this.t6742 + 0.12793037537280e14 * this.t772 * this.t6742
                - 0.61680716697600e14 * this.t1990
                * this.t6756 + 0.41120477798400e14 * this.t896 * this.t6756;
        this.t8234 =
            -0.21956014080e11 * this.t5340 * this.t326 + 0.7508956815360e13 * this.t685 * this.t6866
                + 0.1986496512000e13 * this.t807
                * this.t6956 + 0.1986496512000e13 * this.t4662 * this.t7404 - 0.5482730373120e13 * this.t682
                * this.t6956 + 0.1827576791040e13
                * this.t887 * this.t6956 + 0.23837958144000e14 * this.t884 * this.t6806 - 0.14302774886400e14
                * this.t682 * this.t6806
                - 0.8821612800e10 * this.t795 * this.t6866 - 0.576576000e9 * this.t869 * this.t6806 - 0.248312064000e12
                * this.t866 * this.t6774
                + 0.6054048000e10 * this.t872 * this.t6943 + 0.1787846860800e13 * this.t875 * this.t6794
                + 0.9386196019200e13 * this.t878
                * this.t6704 + 0.3075072000e10 * this.t2271 * this.t6751;
        this.t8265 =
            -0.46930980096000e14 * this.t769 * this.t6861 + 0.18772392038400e14 * this.t1015 * this.t6861
                - 0.62574640128000e14 * this.t732
                * this.t6803 + 0.25029856051200e14 * this.t981 * this.t6803 - 0.18772392038400e14 * this.t875
                * this.t6866 - 0.249215016960e12
                * this.t881 * this.t7046 - 0.1787846860800e13 * this.t807 * this.t6946 - 0.3575693721600e13 * this.t810
                * this.t7039
                - 0.12108096000e11 * this.t1317 * this.t6756 - 0.4767591628800e13 * this.t975 * this.t6806
                + 0.2383795814400e13 * this.t807
                * this.t6806 + 0.9594778152960e13 * this.t816 * this.t6882 + 0.41932800e8 * this.t601 * iy
                + 0.115315200e9 * this.t7465 * this.t19
                + 0.24831206400e11 * this.t778 * this.t7053;
        this.t8297 =
            -0.188194406400e12 * this.t826 * this.t6943 + 0.10280119449600e14 * this.t829 * this.t6794
                + 0.14529715200e11 * this.t775
                * this.t6943 + 0.5939624570880e13 * this.t1050 * this.t6488 - 0.2969812285440e13 * this.t80
                * this.t6488 + 0.158520648000e12
                * this.t918 * this.t6604 - 0.118890486000e12 * this.t4132 * this.t6604 - 0.3218124349440e13 * this.t150
                * this.t6467
                + 0.1609062174720e13 * this.t670 * this.t6467 + 0.1362570125280e13 * this.t968 * this.t6604
                - 0.1135475104400e13 * this.t4085
                * this.t6604 + 0.6054048000e10 * this.t923 * this.t6604 + 0.6054048000e10 * this.t795 * this.t6794
                + 0.8877156288000e13 * this.t1153
                * this.t6467 - 0.5326293772800e13 * this.t1058 * this.t6467;
        this.t8330 =
            0.11531520000e11 * this.t855 * this.t6604 - 0.2731432704000e13 * this.t138 * this.t6488
                + 0.1092573081600e13 * this.t1061
                * this.t6488 + 0.7351344000e10 * this.t786 * this.t6704 + 0.14702688000e11 * this.t789 * this.t6798
                + 0.14702688000e11 * this.t792
                * this.t7039 - 0.78414336000e11 * this.t2520 * this.t6751 + 0.52276224000e11 * this.t928 * this.t6751
                + 0.38379112611840e14
                * this.t4780 * this.t6764 - 0.19189556305920e14 * this.t816 * this.t6764 - 0.126816518400e12
                * this.t278 * this.t6467
                + 0.63408259200e11 * this.t5652 * this.t6467 + 0.6853412966400e13 * this.t611 * this.t6467
                + 0.3198259384320e13 * this.t735
                * this.t7053 + 0.5568398035200e13 * this.t144 * this.t6895 + 0.124156032000e12 * this.t714 * this.t7062;
        this.t8364 =
            0.74493619200e11 * this.t717 * this.t6882 + 0.177543125760e12 * this.t1058 * this.t6669
                - 0.1474973660160e13 * this.t1153
                * this.t6459 + 0.5592608461440e13 * this.t673 * this.t6459 - 0.29265350400e11 * this.t162 * this.t6669
                - 0.153692098560e12
                * this.t1184 * this.t6895 - 0.5390985600e10 * this.t132 * this.t6895 + 0.24831206400e11 * this.t691
                * this.t6701 - 0.198198000e9
                * this.t1139 * this.t6459 - 0.4454718428160e13 * this.t1142 * this.t6459 + 0.18772392038400e14
                * this.t676 * this.t7039
                + 0.18772392038400e14 * this.t679 * this.t6798 + 0.9386196019200e13 * this.t682 * this.t6946
                - 0.1390547558400e13 * this.t685
                * this.t6701 - 0.3198259384320e13 * this.t611 * this.t3328;
        this.t8395 =
            -0.62731468800e11 * this.t598 * this.t2645 - 0.1390547558400e13 * this.t738 * this.t7053
                - 0.872252559360e12 * this.t741 * this.t8016
                + 0.15991296921600e14 * this.t744 * this.t6922 + 0.78414336000e11 * this.t798 * this.t6946
                + 0.156828672000e12 * this.t751
                * this.t7039 + 0.74493619200e11 * this.t757 * this.t6930 + 0.1744505118720e13 * this.t738 * this.t7434
                + 0.6952737792000e13
                * this.t810 * this.t6779 + 0.996860067840e12 * this.t999 * this.t7401 - 0.10280119449600e14 * this.t641
                * this.t6467
                + 0.124156032000e12 * this.t760 * this.t6922 - 0.295905209600e12 * this.t171 * this.t6488
                + 0.118362083840e12 * this.t4289
                * this.t6488 - 0.16705194105600e14 * this.t673 * this.t6467;
        this.t8427 =
            0.11136796070400e14 * this.t1074 * this.t6467 - 0.2284470988800e13 * this.t608 * this.t6604 + 0.141261120e9
                * this.t7762 * this.t19
                + 0.317041296000e12 * this.t918 * this.t6324 - 0.237780972000e12 * this.t4132 * this.t6324
                + 0.35508625152000e14 * this.t1153
                * this.t6383 - 0.21305175091200e14 * this.t1058 * this.t6383 + 0.780409344000e12 * this.t178
                * this.t6367 + 0.16388596224000e14
                * this.t95 * this.t6729 - 0.298679320940e12 * this.t804 * this.t7499 + 0.2725140250560e13 * this.t968
                * this.t6324
                - 0.2270950208800e13 * this.t4085 * this.t6324 - 0.507266073600e12 * this.t278 * this.t6314
                + 0.253633036800e12 * this.t5652
                * this.t6314 - 0.40971490560000e14 * this.t138 * this.t6349;
        this.t8458 =
            0.16388596224000e14 * this.t1061 * this.t6349 + 0.635156121600e12 * this.t91 * this.t6337 - 0.2306304000e10
                * this.t5332 * this.t112
                + 0.2370493382400e13 * this.t171 * this.t6524 - 0.20649631242240e14 * this.t1050 * this.t6672
                - 0.58150170624e11 * this.t5229
                * iy + 0.3149800908800e13 * this.t80 * this.t6540 + 0.629960181760e12 * this.t80 * this.t7897
                + 0.1877239203840e13 * this.t582
                * this.t3328 + 0.1383228887040e13 * this.t1159 * this.t6337 + 0.23063040000e11 * this.t855 * this.t6324
                - 0.4438578144000e13
                * this.t171 * this.t6320 + 0.1775431257600e13 * this.t4289 * this.t6320 + 0.89094368563200e14
                * this.t1050 * this.t6320
                - 0.44547184281600e14 * this.t80 * this.t6320;
        this.t8486 = this.t96 * this.t2710;
        this.t8495 =
            -0.17818873712640e14 * this.t80 * this.t6374 - 0.119189790720e12 * this.t16 * this.t326 * this.t156
                + 0.6396518768640e13 * this.t1012
                * this.t7629 - 0.141261120e9 * this.t543 * iy + 0.12108096000e11 * this.t923 * this.t6324
                - 0.20649631242240e14 * this.t1050
                * this.t6524 + 0.1434284342400e13 * this.t275 * this.t6672 - 0.1687393344000e13 * this.t1074
                * this.t6317 - 0.2522520e7 * this.t68
                * this.t98 * this.t8 - 0.1390547558400e13 * this.t40 * this.t326 * this.t8094 + 0.16705194105600e14
                * this.t144 * this.t6524
                + 0.3149800908800e13 * this.t80 * this.t8486 + 0.1889880545280e13 * this.t80 * this.t6675
                + 0.4827186524160e13 * this.t138
                * this.t6524 - 0.1436662656000e13 * this.t670 * this.t6317;
        this.t8526 =
            0.5746650624000e13 * this.t1184 * this.t6349 + 0.190224777600e12 * this.t1116 * this.t6672
                + 0.13499146752000e14 * this.t128
                * this.t6367 - 0.6749573376000e13 * this.t4310 * this.t6367 - 0.16388596224000e14 * this.t138
                * this.t6346 + 0.6555438489600e13
                * this.t1061 * this.t6346 + 0.4049744025600e13 * this.t128 * this.t6374 - 0.2024872012800e13
                * this.t4310 * this.t6374
                - 0.66820776422400e14 * this.t673 * this.t6314 + 0.44547184281600e14 * this.t1074 * this.t6314
                + 0.2370493382400e13 * this.t171
                * this.t6672 - 0.3374786688000e13 * this.t1074 * this.t6722 - 0.16172956800e11 * this.t132 * this.t6524
                + 0.4827186524160e13
                * this.t138 * this.t6672 - 0.887715628800e12 * this.t103 * this.t6672;
        this.t8558 =
            0.1775431257600e13 * this.t1058 * this.t6729 + 0.887715628800e12 * this.t1058 * this.t6317
                - 0.1274668595200e13 * this.t1061
                * this.t8486 - 0.211718707200e12 * this.t178 * this.t6524 - 0.3374786688000e13 * this.t1074
                * this.t6729 + 0.1362570125280e13
                * this.t174 * this.t6537 - 0.1135475104400e13 * this.t430 * this.t6507 + 0.3575693721600e13 * this.t149
                * this.t6565
                - 0.2681770291200e13 * this.t72 * this.t6585 - 0.295905209600e12 * this.t54 * this.t6559
                + 0.118362083840e12 * this.t127 * this.t7300
                - 0.785998213000e12 * this.t123 * this.t6507 + 0.628798570400e12 * this.t174 * this.t6565
                - 0.2731432704000e13 * this.t72
                * this.t6559 + 0.1092573081600e13 * this.t94 * this.t7300;
        this.t8591 =
            -0.417164267520e12 * this.t508 * this.t8190 - 0.461076295680e12 * this.t1184 * this.t6672
                - 0.29698122854400e14 * this.t1090
                * this.t6729 - 0.14849061427200e14 * this.t1090 * this.t7655 - 0.1687393344000e13 * this.t1074
                * this.t7655 - 0.292653504000e12
                * this.t162 * this.t6729 - 0.1470268800e10 * this.t90 * this.t6488 - 0.2018016000e10 * this.t532
                * this.t6467 - 0.15682867200e11
                * this.t35 * this.t6488 - 0.74493619200e11 * this.t374 * this.t7987 - 0.131736084480e12 * this.t262
                * this.t6866 - 0.198649651200e12
                * this.t604 * this.t7082 + 0.92252160e8 * this.t556 * this.t6459 + 0.144144000e9 * this.t59
                * this.t6895 - 0.456894197760e12 * this.t614
                * this.t6459 + 0.595948953600e12 * this.t564 * this.t6895;
    }
}
