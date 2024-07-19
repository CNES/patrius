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
 * @history created 28/02/2013
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:345:30/10/2014:modified comments ratio
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::FA:829:25/01/2017:Protection of trigonometric methods
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.stela.orbits;

import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.stela.JavaMathAdapter;

/**
 * Jacobian matrix converter: it is used to get Jacobian matrix from some equinoctial parameters to cartesian parameters
 * 
 * @concurrency unconditionally thread-safe
 * 
 * @author Emmanuel Bignon
 * @author Romain Pinede
 * @author Tiziana Sabatini
 * 
 * @version $Id$
 * 
 * @since 1.3
 */
public final class JacobianConverter {

    /**
     * Hidden constructor.
     */
    private JacobianConverter() {
    }

    /**
     * Computes Jacobian matrix from equinoctial to cartesian.
     * 
     * @param orbit
     *        Stela equinoctial orbit
     * @return the Jacobian matrix
     */
    // CHECKSTYLE: stop MethodLength check
    public static double[][] computeEquinoctialToCartesianJacobian(final StelaEquinoctialOrbit orbit) {
        // CHECKSTYLE: resume MethodLength check

        // Initialization
        final double[][] jacobian = new double[6][6];

        // get equinoctial parameters
        final double a = orbit.getA();
        final double lambdaEq = orbit.getLM();
        final double ex = orbit.getEquinoctialEx();
        final double ey = orbit.getEquinoctialEy();
        final double ix = orbit.getIx();
        final double iy = orbit.getIy();
        final double mu = orbit.getMu();

        // Eccentric longitude
        final double wW = MathLib.atan2(ey, ex);
        final double e2 = ex * ex + ey * ey;
        final double f = orbit.kepEq(MathLib.sqrt(e2), lambdaEq - wW) + wW;

        final double[] sincosf = MathLib.sinAndCos(f);
        final double sinF = sincosf[0];
        final double cosF = sincosf[1];

        // Other temporary variables
        final double r = a * (1 - ex * cosF - ey * sinF);
        final double n = MathLib.sqrt(MathLib.divide(mu, MathLib.pow(a, 3)));
        final double eta = MathLib.sqrt(MathLib.max(0.0, 1 - e2));
        final double nu = 1 / (1 + eta);
        final double iFact = MathLib.sqrt(MathLib.max(0.0, 1 - ix * ix - iy * iy));

        // Temporary Position/Velocity
        final double[] posJ = new double[2];
        final double[] velJ = new double[2];
        posJ[0] = a * ((1 - nu * ey * ey) * cosF + nu * ex * ey * sinF - ex);
        posJ[1] = a * ((1 - nu * ex * ex) * sinF + nu * ex * ey * cosF - ey);
        velJ[0] = n * a * a * MathLib.divide((-(1 - nu * ey * ey) * sinF + nu * ex * ey * cosF), r);
        velJ[1] = n * a * a * MathLib.divide(((1 - nu * ex * ex) * cosF - nu * ex * ey * sinF), r);

        // R matrix and R derivatives
        final double[][] rM = new double[3][2];
        final double[][] rDerIx = new double[3][2];
        final double[][] rDerIy = new double[3][2];

        rM[0][0] = 1 - 2 * iy * iy;
        rM[1][0] = 2 * ix * iy;
        rM[2][0] = -iy * iFact * 2;
        rM[0][1] = rM[1][0];
        rM[1][1] = 1 - 2 * ix * ix;
        rM[2][1] = 2 * ix * iFact;

        rDerIx[1][0] = 2 * iy;
        rDerIx[2][0] = MathLib.divide(2 * ix * iy, iFact);
        rDerIx[0][1] = rDerIx[1][0];
        rDerIx[1][1] = -ix * 4;
        rDerIx[2][1] = MathLib.divide(-(4 * ix * ix + 2 * iy * iy - 2), iFact);

        rDerIy[0][0] = -iy * 4;
        rDerIy[1][0] = 2 * ix;
        rDerIy[2][0] = MathLib.divide((4 * iy * iy + 2 * ix * ix - 2), iFact);
        rDerIy[0][1] = 2 * ix;
        rDerIy[2][1] = -rDerIx[2][0];

        // Position/Velocity in "natural" frame
        final double[] pos = JavaMathAdapter.matrixVectorMultiply(rM, posJ);
        final double[] vel = JavaMathAdapter.matrixVectorMultiply(rM, velJ);

        // X, Y, VX, VY derivatives related to ex, ey
        final double dXdex = MathLib.divide(ey * velJ[0], (n * (1 + eta)))
            + MathLib.divide(posJ[1] * velJ[0], (n * a * eta)) - a;
        final double dYdex = MathLib.divide(ey * velJ[1], (n * (1 + eta)))
            - MathLib.divide(posJ[0] * velJ[0], (n * a * eta));
        final double dVXdex = MathLib.divide(velJ[0] * velJ[1], (n * a * eta)) - MathLib.divide(n * a * a
            * (MathLib.divide(a * ey * posJ[0], 1 + eta)
            + MathLib.divide(posJ[0] * posJ[1], eta)), MathLib.pow(r, 3));
        final double dVYdex = MathLib.divide(-velJ[0] * velJ[0], (n * a * eta)) - MathLib.divide(n * a * a
            * (MathLib.divide(a * ey * posJ[1], 1 + eta)
            - MathLib.divide(posJ[0] * posJ[0], eta)), MathLib.pow(r, 3));

        final double dXdey = MathLib.divide(-ex * velJ[0], (n * (1 + eta)))
            + MathLib.divide(posJ[1] * velJ[1], (n * a * eta));
        final double dYdey = MathLib.divide(-ex * velJ[1], (n * (1 + eta)))
            - MathLib.divide(posJ[0] * velJ[1], (n * a * eta)) - a;
        final double dVXdey = MathLib.divide(velJ[1] * velJ[1], (n * a * eta)) + MathLib.divide(n * a * a
            * (MathLib.divide(a * ex * posJ[0], 1 + eta)
            - MathLib.divide(posJ[1] * posJ[1], eta)), MathLib.pow(r, 3));
        final double dVYdey = MathLib.divide(-velJ[0] * velJ[1], (n * a * eta)) + MathLib.divide(n * a * a
            * (MathLib.divide(a * ex * posJ[1], 1 + eta)
            + MathLib.divide(posJ[0] * posJ[1], eta)), MathLib.pow(r, 3));

        for (int i = 0; i < 3; i++) {
            // d(x,y,z,Vx,Vy,Vz)/da
            jacobian[i][0] = MathLib.divide(pos[i], a);
            jacobian[i + 3][0] = MathLib.divide(-vel[i], 2 * a);

            // d(x,y,z,Vx,Vy,Vz)/dlambdaEq
            jacobian[i][1] = MathLib.divide(vel[i], n);
            jacobian[i + 3][1] = -n * MathLib.pow(MathLib.divide(a, r), 3) * pos[i];

            // d(x,y,z,Vx,Vy,Vz)/dex
            jacobian[i][2] = rM[i][0] * dXdex + rM[i][1] * dYdex;
            jacobian[i + 3][2] = rM[i][0] * dVXdex + rM[i][1] * dVYdex;

            // d(x,y,z,Vx,Vy,Vz)/dey
            jacobian[i][3] = rM[i][0] * dXdey + rM[i][1] * dYdey;
            jacobian[i + 3][3] = rM[i][0] * dVXdey + rM[i][1] * dVYdey;

            // d(x,y,z,Vx,Vy,Vz)/dix
            jacobian[i][4] = rDerIx[i][0] * posJ[0] + rDerIx[i][1] * posJ[1];
            jacobian[i + 3][4] = rDerIx[i][0] * velJ[0] + rDerIx[i][1] * velJ[1];

            // d(x,y,z,Vx,Vy,Vz)/diy
            jacobian[i][5] = rDerIy[i][0] * posJ[0] + rDerIy[i][1] * posJ[1];
            jacobian[i + 3][5] = rDerIy[i][0] * velJ[0] + rDerIy[i][1] * velJ[1];
        }

        return jacobian;
    }
}
