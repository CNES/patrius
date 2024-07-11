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
 * @history created on 08/02/2013
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.stela.forces;

import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.stela.orbits.OrbitNatureConverter;
import fr.cnes.sirius.patrius.stela.orbits.StelaEquinoctialOrbit;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

//CHECKSTYLE:OFF
/**
 * <p>
 * Abstract Class for the computation of Gauss Equations and its derivatives
 * </p>
 * <p>
 * Computation of Gauss Equations and its derivatives Gives "GAUSS" attributes
 * </p>
 * 
 * @concurrency not thread-safe
 * @concurrency.comment not thread-safe because two threads can simultaneously modify global attributes of the class
 * 
 * @author Cedric Dental
 * 
 * @version $Id$
 * 
 * @since 1.3
 * 
 */
public abstract class AbstractStelaGaussContribution implements StelaForceModel {

     /** Serializable UID. */
    private static final long serialVersionUID = 6261930762760061459L;

    /**
     * Value of the perturbation derivative
     */
    protected double[] dPert = new double[6];
    /**
     * Type of the equations
     */
    private final String type;

    /**
     * Constructor of the class
     * 
     */
    public AbstractStelaGaussContribution() {
        this.type = "GAUSS";

    }

    /**
     * @return the type
     */
    @Override
    public String getType() {
        return this.type;
    }

    /**
     * @return the dPert
     */
    public double[] getdPert() {
        return this.dPert;
    }

    /**
     * Compute the dE/dt force derivatives for a given spacecraft state.
     * 
     * @param orbit
     *        current orbit information: date, kinematics
     * @param converter
     *        mean / osculating parameters converter
     * @return the perturbation dE/dt for the current force
     * @throws PatriusException
     *         if perturbation computation fails
     */
    public abstract double[] computePerturbation(final StelaEquinoctialOrbit orbit,
            final OrbitNatureConverter converter) throws PatriusException;

    /**
     * Compute the TNW Gauss Equation for GTO.
     * 
     * @param orbit
     *        current orbit information: date, kinematics
     * @return gaussEquations
     */
    protected double[][] computeGaussEquations(final StelaEquinoctialOrbit orbit) {

        /** Gauss Equations value */
        final double[][] gaussEquations = new double[6][3];

        // Stela Equinoctial Elements
        final double a = orbit.getA();
        final double ex = orbit.getEquinoctialEx();
        final double ey = orbit.getEquinoctialEy();
        final double ix = orbit.getIx();
        final double iy = orbit.getIy();
        final double lamEq = orbit.getLM();

        // other variables
        final double MU = orbit.getMu();
        final double wW = MathLib.atan2(ey, ex);
        final double e2 = ex * ex + ey * ey;
        final double f = orbit.kepEq(MathLib.sqrt(e2), lamEq - wW) + wW;

        // Force expressed in the TNW
        final double[] sincosf = MathLib.sinAndCos(f);
        final double t1 = sincosf[1];
        final double t2 = ex * t1;
        final double t3 = sincosf[0];
        final double t4 = ey * t3;
        final double t5 = 0.1e1 + t2 + t4;
        final double t6 = 0.1e1 - t2 - t4;
        final double t9 = MathLib.sqrt(t5 / t6);
        final double t10 = a * a;
        final double t14 = MathLib.sqrt(MU / t10 / a);
        final double t15 = 0.1e1 / t14;
        final double t20 = ex * t3 - ey * t1;
        final double t21 = 0.1e1 / a;
        final double t23 = t2 + t4;
        final double t24 = t23 * t23;
        final double t26 = MathLib.sqrt(0.1e1 - t24);
        final double t27 = 0.1e1 / t26;
        final double t29 = ex * ex;
        final double t30 = ey * ey;
        final double t32 = MathLib.sqrt(0.1e1 - t29 - t30);
        final double t34 = 0.1e1 / (0.1e1 + t32);
        final double t42 = MathLib.sqrt(t6 / t5);
        final double t46 = t15 * t21;
        final double t48 = t3 - ey;
        final double t50 = t1 - ex;
        final double t52 = t20 * t34;
        final double t56 = t52 * (ex * ix + ey * iy);
        final double t61 = ix * ix;
        final double t62 = iy * iy;
        final double t64 = MathLib.sqrt(0.1e1 - t61 - t62);
        final double t66 = t21 / t32 / t64;
        final double t70 = t20 * ey * t34;
        final double t73 = t46 * t27;
        final double t77 = t20 * ex * t34;
        final double t83 = ix * t48 - iy * t50 - t56;
        final double t98 = 0.1e1 - t61;
        final double t100 = ix * iy;
        final double t110 = 0.1e1 - t62;

        gaussEquations[0][0] = 0.2e1 * t9 * t15;
        gaussEquations[0][1] = 0.0e0;
        gaussEquations[0][2] = 0.0e0;
        gaussEquations[1][0] = -0.2e1 * t20 * t21 * t15 * t27 * (0.1e1 - t32 * t34 - t2 - t4);
        gaussEquations[1][1] = t42 * (0.1e1 + t32 + t23 * t34) * t46;
        gaussEquations[1][2] = (ix * t48 - iy * t50 - t56) * t15 * t66;
        gaussEquations[2][0] = 0.2e1 * t32 * (t32 * t1 - t70) * t73;
        gaussEquations[2][1] = -t42 * (ey + t3 - t77) * t46;
        gaussEquations[2][2] = -ey * t83 * t15 * t66;
        gaussEquations[3][0] = 0.2e1 * t32 * (t32 * t3 + t77) * t73;
        gaussEquations[3][1] = t42 * (ex + t1 + t70) * t46;
        gaussEquations[3][2] = ex * t83 * t15 * t66;
        gaussEquations[4][0] = 0.0e0;
        gaussEquations[4][1] = 0.0e0;
        gaussEquations[4][2] = (t98 * t50 - t100 * t48 + t52 * (t100 * ex + t98 * ey)) * t15 * t66 / 0.2e1;
        gaussEquations[5][0] = 0.0e0;
        gaussEquations[5][1] = 0.0e0;
        gaussEquations[5][2] = (t110 * t48 - t100 * t50 - t52 * (t100 * ey + t110 * ex)) * t15 * t66 / 0.2e1;

        return gaussEquations;

    }

    /**
     * Computation of the Gauss equation derivatives matrix in TNW frame.
     * 
     * @param orbit
     *        current orbit information: date, kinematics
     * @return gaussDerivativeEquations
     */
    protected double[][][] computeGaussDerivativeEquations(final StelaEquinoctialOrbit orbit) {

        /** Gauss Derivatives value */
        final double[][][] gaussDerivativeEquations = new double[6][3][6];

        /** Orbital elements */
        final double a = orbit.getA();
        final double ex = orbit.getEquinoctialEx();
        final double ey = orbit.getEquinoctialEy();
        final double ix = orbit.getIx();
        final double iy = orbit.getIy();
        final double lamEq = orbit.getLM();

        // other variables
        final double MU = orbit.getMu();
        /** Temporary coefficient for e2 (temporary value used to compute f). */
        final double e2 = ex * ex + ey * ey;
        /** Temporary coefficient for wW (temporary value used to compute f). */
        final double wW = MathLib.atan2(ey, ex);
        /** Temporary coefficient for f. */
        final double f = orbit.kepEq(MathLib.sqrt(e2), lamEq - wW) + wW;

        final double[] sincosf = MathLib.sinAndCos(f);
        final double t1 = sincosf[1];
        final double t2 = ex * t1;
        final double t3 = sincosf[0];
        final double t4 = ey * t3;
        final double t5 = 0.1e1 + t2 + t4;
        final double t6 = 0.1e1 - t2 - t4;
        final double t7 = 0.1e1 / t6;
        final double t9 = MathLib.sqrt(t5 * t7);
        final double t10 = a * a;
        final double t13 = MU / t10 / a;
        final double t14 = MathLib.sqrt(t13);
        final double t16 = 0.1e1 / t14 / t13;
        final double t18 = t10 * t10;
        final double t24 = ey * t1;
        final double t25 = ex * t3 - t24;
        final double t26 = 0.1e1 / t10;
        final double t28 = 0.1e1 / t14;
        final double t29 = t2 + t4;
        final double t30 = t29 * t29;
        final double t31 = 0.1e1 - t30;
        final double t32 = MathLib.sqrt(t31);
        final double t33 = 0.1e1 / t32;
        final double t34 = t28 * t33;
        final double t35 = ex * ex;
        final double t36 = ey * ey;
        final double t37 = 0.1e1 - t35 - t36;
        final double t38 = MathLib.sqrt(t37);
        final double t39 = 0.1e1 + t38;
        final double t40 = 0.1e1 / t39;
        final double t42 = 0.1e1 - t38 * t40 - t2 - t4;
        final double t43 = t34 * t42;
        final double t47 = 0.1e1 / t18 / a;
        final double t55 = 0.1e1 / t5;
        final double t57 = MathLib.sqrt(t6 * t55);
        final double t58 = t29 * t40;
        final double t59 = 0.1e1 + t38 + t58;
        final double t60 = t57 * t59;
        final double t62 = t16 * t47 * MU;
        final double t65 = t28 * t26;
        final double t68 = t3 - ey;
        final double t69 = ix * t68;
        final double t70 = t1 - ex;
        final double t71 = iy * t70;
        final double t72 = t25 * t40;
        final double t73 = ex * ix;
        final double t74 = ey * iy;
        final double t75 = t73 + t74;
        final double t76 = t72 * t75;
        final double t77 = t69 - t71 - t76;
        final double t80 = 0.1e1 / t38;
        final double t81 = ix * ix;
        final double t82 = iy * iy;
        final double t83 = 0.1e1 - t81 - t82;
        final double t84 = MathLib.sqrt(t83);
        final double t85 = 0.1e1 / t84;
        final double t86 = t80 * t85;
        final double t87 = t86 * MU;
        final double t90 = t77 * t28;
        final double t92 = t26 * t80 * t85;
        final double t95 = t38 * t1;
        final double t96 = t25 * ey;
        final double t97 = t96 * t40;
        final double t98 = t95 - t97;
        final double t99 = t38 * t98;
        final double t102 = t47 * t33 * MU;
        final double t105 = t65 * t33;
        final double t110 = t25 * ex * t40;
        final double t111 = ey + t3 - t110;
        final double t112 = t57 * t111;
        final double t119 = -iy * t70 + ix * t68 - t76;
        final double t120 = ey * t119;
        final double t124 = t47 * t80 * t85 * MU;
        final double t127 = t120 * t28;
        final double t130 = t38 * t3;
        final double t131 = t130 + t110;
        final double t132 = t38 * t131;
        final double t139 = ex + t1 + t97;
        final double t140 = t57 * t139;
        final double t145 = ex * t119;
        final double t149 = t145 * t28;
        final double t152 = 0.1e1 - t81;
        final double t154 = ix * iy;
        final double t158 = t154 * ex + t152 * ey;
        final double t160 = t152 * t70 - t154 * t68 + t72 * t158;
        final double t165 = t160 * t28;
        final double t169 = 0.1e1 - t82;
        final double t174 = t154 * ey + t169 * ex;
        final double t176 = t169 * t68 - t154 * t70 - t72 * t174;
        final double t181 = t176 * t28;
        final double t185 = 0.1e1 / t9;
        final double t188 = t6 * t6;
        final double t190 = t5 / t188;
        final double t195 = 0.1e1 / a;
        final double t198 = t25 * t195;
        final double t199 = t198 * t28;
        final double t201 = 0.1e1 / t32 / t31;
        final double t202 = t201 * t42;
        final double t203 = -t29 * t25;
        final double t206 = t25 * t25;
        final double t211 = 0.1e1 / t57;
        final double t212 = t211 * t59;
        final double t213 = t28 * t195;
        final double t215 = t5 * t5;
        final double t217 = t6 / t215;
        final double t220 = t213 * (t25 * t55 + t217 * t25);
        final double t229 = iy * t3;
        final double t230 = ix * t1;
        final double t232 = t229 + t230 - t58 * t75;
        final double t235 = t195 * t80;
        final double t236 = t235 * t85;
        final double t239 = t29 * ey * t40;
        final double t242 = t213 * t33;
        final double t244 = t99 * t28;
        final double t245 = t195 * t201;
        final double t246 = t245 * t203;
        final double t250 = t211 * t111;
        final double t254 = t29 * ex * t40;
        final double t262 = t213 * t86;
        final double t267 = t132 * t28;
        final double t271 = t211 * t139;
        final double t282 = t152 * t3;
        final double t290 = t169 * t1;
        final double t298 = t185 * t28;
        final double t299 = t3 * t3;
        final double t301 = ex * t299 * t7;
        final double t302 = t3 * t7;
        final double t303 = t24 * t302;
        final double t304 = t1 - t301 + t303;
        final double t309 = t2 * t302;
        final double t312 = t3 + t309 + ey * t299 * t7;
        final double t315 = t29 * t304;
        final double t318 = t80 * t40;
        final double t320 = t39 * t39;
        final double t321 = 0.1e1 / t320;
        final double t330 = t213 * (-t304 * t55 - t217 * t304);
        final double t333 = t80 * ex;
        final double t335 = t29 * t321;
        final double t341 = t230 * t302;
        final double t342 = t299 * t7;
        final double t343 = -t342 - 0.1e1;
        final double t345 = t312 * t40;
        final double t346 = t345 * t75;
        final double t347 = t25 * t321;
        final double t348 = t75 * t80;
        final double t350 = t347 * t348 * ex;
        final double t351 = t72 * ix;
        final double t355 = t90 * t195;
        final double t357 = 0.1e1 / t38 / t37;
        final double t358 = t357 * t85;
        final double t359 = t358 * ex;
        final double t363 = t80 * t98 * t28;
        final double t364 = t195 * t33;
        final double t365 = t364 * ex;
        final double t367 = t80 * t1;
        final double t372 = t312 * ey * t40;
        final double t373 = t321 * t80;
        final double t375 = t96 * t373 * ex;
        final double t379 = t245 * t315;
        final double t385 = t1 * t3 * t7;
        final double t387 = t312 * ex * t40;
        final double t389 = t25 * t35 * t373;
        final double t395 = t341 - iy * t343 - t346 - t350 - t351;
        final double t399 = t195 * t357;
        final double t402 = t127 * t399 * t85 * ex;
        final double t405 = t80 * t131 * t28;
        final double t407 = t80 * t3;
        final double t409 = t95 * t302;
        final double t422 = t119 * t28 * t236;
        final double t428 = t399 * t85;
        final double t432 = t154 * t385;
        final double t434 = t158 * t80;
        final double t437 = t72 * t154;
        final double t441 = t165 * t195;
        final double t447 = t174 * t80;
        final double t454 = t181 * t195;
        final double t457 = t1 * t1;
        final double t459 = ey * t457 * t7;
        final double t460 = t309 + t3 - t459;
        final double t467 = -ex * t457 * t7 - t1 - t303;
        final double t470 = t29 * t460;
        final double t482 = t213 * (-t460 * t55 - t217 * t460);
        final double t485 = t80 * ey;
        final double t492 = t457 * t7;
        final double t493 = -t492 - 0.1e1;
        final double t495 = t1 * t7;
        final double t496 = t229 * t495;
        final double t497 = t467 * t40;
        final double t498 = t497 * t75;
        final double t500 = t347 * t348 * ey;
        final double t501 = t72 * iy;
        final double t505 = t358 * ey;
        final double t508 = t364 * ey;
        final double t512 = t467 * ey * t40;
        final double t514 = t25 * t36 * t373;
        final double t518 = t245 * t470;
        final double t524 = t467 * ex * t40;
        final double t530 = ix * t493 - t496 - t498 - t500 - t501;
        final double t577 = -ey + t3 - t110;
        final double t581 = 0.1e1 / t84 / t83;
        final double t582 = t80 * t581;
        final double t583 = t582 * ix;
        final double t590 = t235 * t581 * ix;
        final double t598 = ix * t70;
        final double t600 = iy * t68;
        final double t601 = iy * ex;
        final double t602 = ix * ey;
        final double t617 = ex - t1 - t97;
        final double t620 = t582 * iy;
        final double t627 = t235 * t581 * iy;

        gaussDerivativeEquations[0][0][0] = 0.3e1 * t9 * t16 * MU / t18;
        gaussDerivativeEquations[1][0][0] = 0.2e1 * t25 * t26 * t43 - 0.3e1 * t25 * t47 * t16 * t33 * t42 * MU;
        gaussDerivativeEquations[1][1][0] = 0.3e1 / 0.2e1 * t60 * t62 - t60 * t65;
        gaussDerivativeEquations[1][2][0] = 0.3e1 / 0.2e1 * t77 * t16 * t47 * t87 - t90 * t92;
        gaussDerivativeEquations[2][0][0] = 0.3e1 * t99 * t16 * t102 - 0.2e1 * t99 * t105;
        gaussDerivativeEquations[2][1][0] = -0.3e1 / 0.2e1 * t112 * t62 + t112 * t65;
        gaussDerivativeEquations[2][2][0] = -0.3e1 / 0.2e1 * t120 * t16 * t124 + t127 * t92;
        gaussDerivativeEquations[3][0][0] = 0.3e1 * t132 * t16 * t102 - 0.2e1 * t132 * t105;
        gaussDerivativeEquations[3][1][0] = 0.3e1 / 0.2e1 * t140 * t62 - t140 * t65;
        gaussDerivativeEquations[3][2][0] = 0.3e1 / 0.2e1 * t145 * t16 * t124 - t149 * t92;
        gaussDerivativeEquations[4][2][0] = 0.3e1 / 0.4e1 * t160 * t16 * t47 * t87 - t165 * t92 / 0.2e1;
        gaussDerivativeEquations[5][2][0] = 0.3e1 / 0.4e1 * t176 * t16 * t47 * t87 - t181 * t92 / 0.2e1;

        gaussDerivativeEquations[0][0][1] = t7 * t185 * t28 * (-t25 * t7 - t190 * t25);
        gaussDerivativeEquations[1][0][1] = 0.2e1 * t7 * (-t29 * t195 * t43 - t199 * t202 * t203 - t206 * t195 * t34);
        gaussDerivativeEquations[1][1][1] = t7 * (t212 * t220 / 0.2e1 - t57 * t25 * t40 * t28 * t195);
        gaussDerivativeEquations[1][2][1] = t7 * t232 * t28 * t236;
        gaussDerivativeEquations[2][0][1] = 0.2e1 * t7 * (t38 * (-t130 - t239) * t242 + t244 * t246);
        gaussDerivativeEquations[2][1][1] = t7 * (-t250 * t220 / 0.2e1 - t57 * (t1 - t254) * t213);
        gaussDerivativeEquations[2][2][1] = -t7 * ey * t232 * t262;
        gaussDerivativeEquations[3][0][1] = 0.2e1 * t7 * (t38 * (t95 + t254) * t242 + t267 * t246);
        gaussDerivativeEquations[3][1][1] = t7 * (t271 * t220 / 0.2e1 + t57 * (-t3 + t239) * t213);
        gaussDerivativeEquations[3][2][1] = t7 * ex * t232 * t262;
        gaussDerivativeEquations[4][2][1] = t7 * (-t282 - t154 * t1 + t58 * t158) * t28 * t236 / 0.2e1;
        gaussDerivativeEquations[5][2][1] = t7 * (t290 + t154 * t3 - t58 * t174) * t28 * t236 / 0.2e1;

        gaussDerivativeEquations[0][0][2] = t298 * (t304 * t7 + t190 * t304);
        gaussDerivativeEquations[1][0][2] = -0.2e1 * t312 * t195 * t43 - 0.2e1 * t199 * t202 * t315 - 0.2e1 * t198
            * t34
            * (t318 * ex - t321 * ex - t1 + t301 - t303);
        gaussDerivativeEquations[1][1][2] = t212 * t330 / 0.2e1 + t57 * (-t333 + t304 * t40 + t335 * t333) * t213;
        gaussDerivativeEquations[1][2][2] = (t341 - iy * t343 - t346 - t350 - t351) * t28 * t236 + t355 * t359;
        gaussDerivativeEquations[2][0][2] = -0.2e1 * t363 * t365 + 0.2e1 * t38
            * (-t367 * ex - t38 * t299 * t7 - t372 - t375)
            * t242 + 0.2e1 * t244
            * t379;
        gaussDerivativeEquations[2][1][2] = -t250 * t330 / 0.2e1 - t57 * (t385 - t387 - t72 - t389) * t213;
        gaussDerivativeEquations[2][2][2] = -ey * t395 * t28 * t236 - t402;
        gaussDerivativeEquations[3][0][2] = -0.2e1 * t405 * t365 + 0.2e1 * t38
            * (-t407 * ex + t409 + t387 + t72 + t389) * t242
            + 0.2e1 * t267 * t379;
        gaussDerivativeEquations[3][1][2] = t271 * t330 / 0.2e1 + t57 * (0.1e1 - t342 + t372 + t375) * t213;
        gaussDerivativeEquations[3][2][2] = t422 + ex * t395 * t28 * t236 + t35 * t119 * t28 * t428;
        gaussDerivativeEquations[4][2][2] = (t152 * t343 - t432 + t345 * t158 + t347 * t434 * ex + t437) * t28 * t236
            / 0.2e1
            + t441 * t359 / 0.2e1;
        gaussDerivativeEquations[5][2][2] = (t290 * t302 - t154 * t343 - t345 * t174 - t347 * t447 * ex - t72 * t169)
            * t28 * t236
            / 0.2e1 + t454
            * t359 / 0.2e1;

        gaussDerivativeEquations[0][0][3] = t298 * (t460 * t7 + t190 * t460);
        gaussDerivativeEquations[1][0][3] = -0.2e1 * t467 * t195 * t43 - 0.2e1 * t199 * t202 * t470 - 0.2e1 * t198
            * t34
            * (t318 * ey - t321 * ey - t309 - t3 + t459);
        gaussDerivativeEquations[1][1][3] = t212 * t482 / 0.2e1 + t57 * (-t485 + t460 * t40 + t335 * t485) * t213;
        gaussDerivativeEquations[1][2][3] = (ix * t493 - t496 - t498 - t500 - t501) * t28 * t236 + t355 * t505;
        gaussDerivativeEquations[2][0][3] = -0.2e1 * t363 * t508 + 0.2e1 * t38
            * (-t367 * ey + t409 - t512 - t72 - t514) * t242
            + 0.2e1 * t244 * t518;
        gaussDerivativeEquations[2][1][3] = -t250 * t482 / 0.2e1 - t57 * (0.1e1 - t492 - t524 - t375) * t213;
        gaussDerivativeEquations[2][2][3] = -t422 - ey * t530 * t28 * t236 - t36 * t119 * t28 * t428;
        gaussDerivativeEquations[3][0][3] = -0.2e1 * t405 * t508 + 0.2e1 * t38
            * (-t407 * ey - t38 * t457 * t7 + t524 + t375)
            * t242 + 0.2e1 * t267
            * t518;
        gaussDerivativeEquations[3][1][3] = t271 * t482 / 0.2e1 + t57 * (t385 + t512 + t72 + t514) * t213;
        gaussDerivativeEquations[3][2][3] = ex * t530 * t28 * t236 + t402;
        gaussDerivativeEquations[4][2][3] = (t282 * t495 - t154 * t493 + t497 * t158 + t347 * t434 * ey + t72 * t152)
            * t28 * t236
            / 0.2e1 + t441
            * t505 / 0.2e1;
        gaussDerivativeEquations[5][2][3] = (t169 * t493 - t432 - t497 * t174 - t347 * t447 * ey - t437) * t28 * t236
            / 0.2e1
            + t454 * t505 / 0.2e1;

        gaussDerivativeEquations[1][2][4] = t577 * t28 * t236 + t355 * t583;
        gaussDerivativeEquations[2][2][4] = -ey * t577 * t28 * t236 - t127 * t590;
        gaussDerivativeEquations[3][2][4] = ex * t577 * t28 * t236 + t149 * t590;
        gaussDerivativeEquations[4][2][4] = (-0.2e1 * t598 - t600 + t72 * (t601 - 0.2e1 * t602)) * t28 * t236 / 0.2e1
            + t441 * t583
            / 0.2e1;
        gaussDerivativeEquations[5][2][4] = (-t71 - t72 * t74) * t28 * t236 / 0.2e1 + t454 * t583 / 0.2e1;

        gaussDerivativeEquations[1][2][5] = t617 * t28 * t236 + t355 * t620;
        gaussDerivativeEquations[2][2][5] = -ey * t617 * t28 * t236 - t127 * t627;
        gaussDerivativeEquations[3][2][5] = ex * t617 * t28 * t236 + t149 * t627;
        gaussDerivativeEquations[4][2][5] = (-t69 + t72 * t73) * t28 * t236 / 0.2e1 + t441 * t620 / 0.2e1;
        gaussDerivativeEquations[5][2][5] = (-0.2e1 * t600 - t598 - t72 * (t602 - 0.2e1 * t601)) * t28 * t236 / 0.2e1
            + t454 * t620
            / 0.2e1;

        return gaussDerivativeEquations;
    }

}
