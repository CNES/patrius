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
 * @history 05/03/2013
 *
 * HISTORY
* VERSION:4.3:DM:DM-2089:15/05/2019:[PATRIUS] passage a Java 8
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:523:08/02/2016: add solid tides effects in STELA PATRIUS
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.stela.forces;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.stela.orbits.StelaEquinoctialOrbit;

//CHECKSTYLE:OFF
/**
 * <p>
 * Class for the computation of Lagrange Equations and its derivatives
 * </p>
 * <p>
 * Computation of Lagrange Equations and its derivatives
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
public class StelaLagrangeEquations implements Serializable {

    /** Serial UID. */
    private static final long serialVersionUID = 7299637294515676969L;

    /**
     * simple constructor
     */
    public StelaLagrangeEquations() {
    }

    /**
     * Compute the Lagrange Equation for GTO (Poisson Bracket).
     * 
     * @param orbit
     *        current state information: date, kinematics, attitude
     * @return Lagrange equations
     */
    public double[][] computeLagrangeEquations(final StelaEquinoctialOrbit orbit) {
        return this.computeLagrangeEquations(orbit, orbit.getMu());
    }

    /**
     * Compute the Lagrange Equation for GTO (Poisson Bracket) with specific mu.
     * 
     * @param orbit
     *        current state information: date, kinematics, attitude
     * @param mu
     *        mu
     * @return Lagrange equations
     */
    public double[][] computeLagrangeEquations(final StelaEquinoctialOrbit orbit, final double mu) {

        final double[][] lagrangeEquations = new double[6][6];
        /** Orbital elements */
        final double a = orbit.getA();
        final double ex = orbit.getEquinoctialEx();
        final double ey = orbit.getEquinoctialEy();
        final double ix = orbit.getIx();
        final double iy = orbit.getIy();

        /** Generated Code for computation */
        final double t1 = a * a;
        final double t5 = MathLib.sqrt(mu / t1 / a);
        final double t6 = 0.1e1 / t5;
        final double t9 = 0.2e1 * t6 / a;
        final double t10 = ex * ex;
        final double t11 = ey * ey;
        final double t13 = MathLib.sqrt(0.1e1 - t10 - t11);
        final double t15 = 0.1e1 / t1;
        final double t16 = t6 * t15;
        final double t19 = t16 / (0.1e1 + t13);
        final double t20 = t13 * ex * t19;
        final double t22 = t13 * ey * t19;
        final double t24 = 0.1e1 / t13;
        final double t25 = t15 * t24;
        final double t27 = ix * t6 * t25 / 0.2e1;
        final double t30 = iy * t6 * t25 / 0.2e1;
        final double t32 = t13 * t6 * t15;
        final double t34 = t16 * t24;
        final double t36 = ey * ix * t34 / 0.2e1;
        final double t39 = ey * iy * t34 / 0.2e1;
        final double t42 = ex * ix * t34 / 0.2e1;
        final double t45 = ex * iy * t34 / 0.2e1;
        final double t46 = t34 / 0.4e1;

        lagrangeEquations[0][0] = 0.0e0;
        lagrangeEquations[0][1] = -t9;
        lagrangeEquations[0][2] = 0.0e0;
        lagrangeEquations[0][3] = 0.0e0;
        lagrangeEquations[0][4] = 0.0e0;
        lagrangeEquations[0][5] = 0.0e0;
        lagrangeEquations[1][0] = t9;
        lagrangeEquations[1][1] = 0.0e0;
        lagrangeEquations[1][2] = -t20;
        lagrangeEquations[1][3] = -t22;
        lagrangeEquations[1][4] = -t27;
        lagrangeEquations[1][5] = -t30;
        lagrangeEquations[2][0] = 0.0e0;
        lagrangeEquations[2][1] = t20;
        lagrangeEquations[2][2] = 0.0e0;
        lagrangeEquations[2][3] = t32;
        lagrangeEquations[2][4] = t36;
        lagrangeEquations[2][5] = t39;
        lagrangeEquations[3][0] = 0.0e0;
        lagrangeEquations[3][1] = t22;
        lagrangeEquations[3][2] = -t32;
        lagrangeEquations[3][3] = 0.0e0;
        lagrangeEquations[3][4] = -t42;
        lagrangeEquations[3][5] = -t45;
        lagrangeEquations[4][0] = 0.0e0;
        lagrangeEquations[4][1] = t27;
        lagrangeEquations[4][2] = -t36;
        lagrangeEquations[4][3] = t42;
        lagrangeEquations[4][4] = 0.0e0;
        lagrangeEquations[4][5] = t46;
        lagrangeEquations[5][0] = 0.0e0;
        lagrangeEquations[5][1] = t30;
        lagrangeEquations[5][2] = -t39;
        lagrangeEquations[5][3] = t45;
        lagrangeEquations[5][4] = -t46;
        lagrangeEquations[5][5] = 0.0e0;

        return lagrangeEquations;

    }

    /**
     * Computation of the Lagrange equation derivatives matrix (Poisson Bracket derivatives).
     * 
     * @param orbit
     *        current state information: date, kinematics, attitude
     * @return Lagrange equations
     */
    public double[][][] computeLagrangeDerivativeEquations(final StelaEquinoctialOrbit orbit) {

        final double[][][] lagrangeDerivativeEquations = new double[6][6][6];

        /** Orbital elements */
        final double a = orbit.getA();
        final double ex = orbit.getEquinoctialEx();
        final double ey = orbit.getEquinoctialEy();
        final double ix = orbit.getIx();
        final double iy = orbit.getIy();

        // other variables
        final double MU = orbit.getMu();

        /** Generated Code for computation */

        final double t1 = a * a;
        final double t3 = 0.1e1 / t1 / a;
        final double t4 = MU * t3;
        final double t5 = MathLib.sqrt(t4);
        final double t7 = 0.1e1 / t5 / t4;
        final double t8 = t1 * t1;
        final double t10 = 0.1e1 / t8 / t1;
        final double t12 = ex * ex;
        final double t13 = ey * ey;
        final double t14 = 0.1e1 - t12 - t13;
        final double t15 = MathLib.sqrt(t14);
        final double t16 = 0.1e1 / t15;
        final double t20 = 0.1e1 / t5;
        final double t21 = t20 * t3;
        final double t22 = t21 * t16;
        final double t24 = 0.3e1 / 0.8e1 * t7 * t10 * t16 * MU - t22 / 0.2e1;
        final double t25 = ex * iy;
        final double t28 = t10 * t16 * MU;
        final double t32 = -0.3e1 / 0.4e1 * t25 * t7 * t28 + t25 * t22;
        final double t33 = ey * iy;
        final double t38 = 0.3e1 / 0.4e1 * t33 * t7 * t28 - t33 * t22;
        final double t42 = iy * t20;
        final double t43 = t3 * t16;
        final double t45 = -0.3e1 / 0.4e1 * iy * t7 * t28 + t42 * t43;
        final double t46 = ex * ix;
        final double t51 = -0.3e1 / 0.4e1 * t46 * t7 * t28 + t46 * t22;
        final double t52 = ey * ix;
        final double t57 = 0.3e1 / 0.4e1 * t52 * t7 * t28 - t52 * t22;
        final double t61 = ix * t20;
        final double t63 = -0.3e1 / 0.4e1 * ix * t7 * t28 + t61 * t43;
        final double t68 = t15 * t20;
        final double t71 = 0.3e1 / 0.2e1 * t15 * t7 * t10 * MU - 0.2e1 * t68 * t3;
        final double t72 = t15 * ey;
        final double t74 = 0.1e1 + t15;
        final double t75 = 0.1e1 / t74;
        final double t77 = t10 * t75 * MU;
        final double t80 = t21 * t75;
        final double t83 = -0.3e1 / 0.2e1 * t72 * t7 * t77 + 0.2e1 * t72 * t80;
        final double t84 = t15 * ex;
        final double t90 = -0.3e1 / 0.2e1 * t84 * t7 * t77 + 0.2e1 * t84 * t80;
        final double t96 = 0.1e1 / t1;
        final double t97 = t20 * t96;
        final double t99 = -0.3e1 * t7 / t8 / a * MU + 0.2e1 * t97;
        final double t103 = 0.1e1 / t15 / t14;
        final double t106 = t97 * t103 * ex / 0.4e1;
        final double t107 = t96 * t16;
        final double t108 = t42 * t107;
        final double t110 = t97 * t103;
        final double t112 = -t108 - t12 * iy * t110;
        final double t114 = t96 * t103;
        final double t115 = t114 * ex;
        final double t117 = t33 * t20 * t115 / 0.2e1;
        final double t119 = t42 * t115 / 0.2e1;
        final double t120 = t61 * t107;
        final double t123 = -t120 - t12 * ix * t110;
        final double t126 = t52 * t20 * t115 / 0.2e1;
        final double t128 = t61 * t115 / 0.2e1;
        final double t129 = t16 * t20;
        final double t131 = t129 * t96 * ex;
        final double t134 = t96 * t75;
        final double t138 = t74 * t74;
        final double t140 = t96 / t138;
        final double t143 = t16 * ey * t20 * t134 * ex - ey * t20 * t140 * ex;
        final double t145 = t97 * t75;
        final double t147 = t68 * t134;
        final double t150 = t16 * t12 * t145 - t147 - t12 * t20 * t140;
        final double t154 = t97 * t103 * ey / 0.4e1;
        final double t157 = t108 + t13 * iy * t110;
        final double t158 = t114 * ey;
        final double t160 = t42 * t158 / 0.2e1;
        final double t163 = t120 + t13 * ix * t110;
        final double t165 = t61 * t158 / 0.2e1;
        final double t167 = t129 * t96 * ey;
        final double t172 = t16 * t13 * t145 - t147 - t13 * t20 * t140;
        final double t174 = t131 / 0.2e1;
        final double t175 = t167 / 0.2e1;
        final double t177 = t97 * t16 / 0.2e1;

        lagrangeDerivativeEquations[0][0][1] = t99;
        lagrangeDerivativeEquations[0][1][0] = -t99;
        lagrangeDerivativeEquations[0][1][2] = t90;
        lagrangeDerivativeEquations[0][1][3] = t83;
        lagrangeDerivativeEquations[0][1][4] = t63;
        lagrangeDerivativeEquations[0][1][5] = t45;
        lagrangeDerivativeEquations[0][2][1] = -t90;
        lagrangeDerivativeEquations[0][2][3] = t71;
        lagrangeDerivativeEquations[0][2][4] = t57;
        lagrangeDerivativeEquations[0][2][5] = t38;
        lagrangeDerivativeEquations[0][3][1] = -t83;
        lagrangeDerivativeEquations[0][3][2] = -t71;
        lagrangeDerivativeEquations[0][3][4] = t51;
        lagrangeDerivativeEquations[0][3][5] = t32;
        lagrangeDerivativeEquations[0][4][1] = -t63;
        lagrangeDerivativeEquations[0][4][2] = -t57;
        lagrangeDerivativeEquations[0][4][3] = -t51;
        lagrangeDerivativeEquations[0][4][5] = t24;
        lagrangeDerivativeEquations[0][5][1] = -t45;
        lagrangeDerivativeEquations[0][5][2] = -t38;
        lagrangeDerivativeEquations[0][5][3] = -t32;
        lagrangeDerivativeEquations[0][5][4] = -t24;
        lagrangeDerivativeEquations[2][1][2] = t150;
        lagrangeDerivativeEquations[2][1][3] = t143;
        lagrangeDerivativeEquations[2][1][4] = -t128;
        lagrangeDerivativeEquations[2][1][5] = -t119;
        lagrangeDerivativeEquations[2][2][1] = -t150;
        lagrangeDerivativeEquations[2][2][3] = -t131;
        lagrangeDerivativeEquations[2][2][4] = t126;
        lagrangeDerivativeEquations[2][2][5] = t117;
        lagrangeDerivativeEquations[2][3][1] = -t143;
        lagrangeDerivativeEquations[2][3][2] = t131;
        lagrangeDerivativeEquations[2][3][4] = t123 / 0.2e1;
        lagrangeDerivativeEquations[2][3][5] = t112 / 0.2e1;
        lagrangeDerivativeEquations[2][4][1] = t128;
        lagrangeDerivativeEquations[2][4][2] = -t126;
        lagrangeDerivativeEquations[2][4][3] = -t123 / 0.2e1;
        lagrangeDerivativeEquations[2][4][5] = t106;
        lagrangeDerivativeEquations[2][5][1] = t119;
        lagrangeDerivativeEquations[2][5][2] = -t117;
        lagrangeDerivativeEquations[2][5][3] = -t112 / 0.2e1;
        lagrangeDerivativeEquations[2][5][4] = -t106;
        lagrangeDerivativeEquations[3][1][2] = t143;
        lagrangeDerivativeEquations[3][1][3] = t172;
        lagrangeDerivativeEquations[3][1][4] = -t165;
        lagrangeDerivativeEquations[3][1][5] = -t160;
        lagrangeDerivativeEquations[3][2][1] = -t143;
        lagrangeDerivativeEquations[3][2][3] = -t167;
        lagrangeDerivativeEquations[3][2][4] = t163 / 0.2e1;
        lagrangeDerivativeEquations[3][2][5] = t157 / 0.2e1;
        lagrangeDerivativeEquations[3][3][1] = -t172;
        lagrangeDerivativeEquations[3][3][2] = t167;
        lagrangeDerivativeEquations[3][3][4] = -t126;
        lagrangeDerivativeEquations[3][3][5] = -t117;
        lagrangeDerivativeEquations[3][4][1] = t165;
        lagrangeDerivativeEquations[3][4][2] = -t163 / 0.2e1;
        lagrangeDerivativeEquations[3][4][3] = t126;
        lagrangeDerivativeEquations[3][4][5] = t154;
        lagrangeDerivativeEquations[3][5][1] = t160;
        lagrangeDerivativeEquations[3][5][2] = -t157 / 0.2e1;
        lagrangeDerivativeEquations[3][5][3] = t117;
        lagrangeDerivativeEquations[3][5][4] = -t154;
        lagrangeDerivativeEquations[4][1][4] = -t177;
        lagrangeDerivativeEquations[4][2][4] = t175;
        lagrangeDerivativeEquations[4][3][4] = -t174;
        lagrangeDerivativeEquations[4][4][1] = t177;
        lagrangeDerivativeEquations[4][4][2] = -t175;
        lagrangeDerivativeEquations[4][4][3] = t174;
        lagrangeDerivativeEquations[5][1][5] = -t177;
        lagrangeDerivativeEquations[5][2][5] = t175;
        lagrangeDerivativeEquations[5][3][5] = -t174;
        lagrangeDerivativeEquations[5][5][1] = t177;
        lagrangeDerivativeEquations[5][5][2] = -t175;
        lagrangeDerivativeEquations[5][5][3] = t174;

        return lagrangeDerivativeEquations;

    }

}
