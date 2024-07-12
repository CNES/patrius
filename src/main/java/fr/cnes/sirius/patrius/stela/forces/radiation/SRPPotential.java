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
 * @history 12/02/2013
 *
 * HISTORY
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11:DM:DM-3287:22/05/2023:[PATRIUS] Courtes periodes traînee atmospherique et prs
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.stela.forces.radiation;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.stela.forces.AbstractStelaLagrangeContribution;
import fr.cnes.sirius.patrius.stela.orbits.OrbitNatureConverter;
import fr.cnes.sirius.patrius.stela.orbits.StelaEquinoctialOrbit;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * This class represents the PRS Lagrange contribution in the STELA propagator context.
 * </p>
 * <p>Note that short periods are not available with this model.</p>
 * 
 * @concurrency conditionally thread-safe
 * @concurrency.comment thread safe if the CelestialBody used is thread safe
 * 
 * @author Cédric Dental, Rami Houdroge
 * 
 * @version $Id$
 * 
 * @since 1.3
 * 
 */
public class SRPPotential extends AbstractStelaLagrangeContribution {

     /** Serializable UID. */
    private static final long serialVersionUID = 8827195669963190465L;
    /** Astronomical unit (m). */
    private static final double UA = Constants.CNES_STELA_UA;
    /** Sun coefficient at 1 Astronomical Unit (N/m<sup>2</sup>). */
    private static final double C0 = Constants.CONST_SOL_STELA;
    
    /** -0.2e1 */
    private static final double Q2 = -0.2e1;
    /** -0.3e1 */
    private static final double Q3 = -0.3e1;
    /** -0.4e1 */
    private static final double Q4 = -0.4e1;

    /** Mass */
    private final double mass;
    /** Surface */
    private final double surface;
    /** Reflection coefficient */
    private final double cr;
    /** Sun */
    private final PVCoordinatesProvider inSun;

    /**
     * create a SRP Potential model using the sun and spacecraft characteristics.
     * 
     * @param sun
     *        sun as a celestial body
     * @param m
     *        mass of spacecraft
     * @param s
     *        surface of spacecraft
     * @param c
     *        relexion coefficient of spacecraft
     */
    public SRPPotential(final PVCoordinatesProvider sun, final double m, final double s, final double c) {
        super();
        this.mass = m;
        this.surface = s;
        this.cr = c;
        this.inSun = sun;
    }

    /** {@inheritDoc} */
    @Override
    public double[] computePerturbation(final StelaEquinoctialOrbit orbit) throws PatriusException {

        // Initialize dE/dt force derivatives 
        final double[] dPotSRP = new double[6];

        // Get orbit parameters
        final double a = orbit.getA();
        final double ex = orbit.getEquinoctialEx();
        final double ey = orbit.getEquinoctialEy();
        final double ix = orbit.getIx();
        final double iy = orbit.getIy();

        // Sun coordinates
        final Vector3D sunP = this.inSun.getPVCoordinates(orbit.getDate(), orbit.getFrame()).getPosition();

        // Normalize coordinates
        final double rp = sunP.getNorm();
        final Vector3D sunCD = sunP.scalarMultiply(MathLib.divide(1., rp));
        final double xp = sunCD.getX();
        final double yp = sunCD.getY();
        final double zp = sunCD.getZ();

        // Intermediate computations
        final double t1 = this.cr * C0;
        final double t2 = UA * UA;
        final double t4 = rp * rp;
        final double t5 = 0.1e1 / t4;
        final double t7 = iy * iy;
        final double t10 = (0.1e1 - 0.2e1 * t7) * xp;
        final double t11 = ix * iy;
        final double t12 = t11 * yp;
        final double t14 = ix * ix;
        final double t16 = MathLib.sqrt(0.1e1 - t14 - t7);
        final double t18 = iy * t16 * zp;
        final double t24 = (0.1e1 - 0.2e1 * t14) * yp;
        final double t25 = t11 * xp;
        final double t28 = ix * t16 * zp;
        final double t33 = 0.1e1 / this.mass;
        final double t38 = t1 * t2 * t5;
        final double t39 = this.surface * a;
        final double t55 = 0.1e1 / t16;
        final double t58 = iy * t55 * zp * ix;
        final double t61 = ix * yp;
        final double t63 = iy * xp;
        final double t66 = 0.2e1 * t16 * zp;

        // dPotSRP[0] = -3 / 2 * [cr * 0.45605E-05] * [1.49598022291E11²] * [1 / rp²] * surface
        //              * (- ([(1 - 2*iy²) * xp] + 2 * [ix * iy * yp] 
        //              - 2 * [iy * MathLib.sqrt(1 - ix² - iy²) * zp]) * ex - 
        //              ([(1 - 2*ix²) * yp] + 2 * [ix * iy * xp]
        //              + 2 * [ix * MathLib.sqrt(1 - ix² - iy²) * zp]) * ey) / mass
        dPotSRP[0] = Q3 / 0.2e1 * t1 * t2 * t5 * this.surface
            * (-(t10 + 0.2e1 * t12 - 0.2e1 * t18) * ex - (t24 + 0.2e1 * t25 + 0.2e1 * t28) * ey) * t33;
        
        // dPotSRP[2] = -[cr * 0.45605E-05 * 1.49598022291E11² * 1 / rp²] * [surface * a]
        //              * (-3 / 2 * [(1 - 2 * iy²) * xp] - 3 * [ix * iy * yp] 
        //              + 3 * [iy * MathLib.sqrt(1 - ix² - iy²) * zp]) / mass
        dPotSRP[2] = -t38 * t39 * (Q3 / 0.2e1 * t10 - 0.3e1 * t12 + 0.3e1 * t18) * t33;
        
        // dPotSRP[3] = -[cr * 0.45605E-05 * 1.49598022291E11² * 1 / rp²] * [surface * a]
        //              * (-3 / 2 * [(1 - 2*ix²) * yp] - 3 * [ix * iy * xp] 
        //              - 3 * [ix * MathLib.sqrt(1 - ix² - iy²) * zp]) / mass
        dPotSRP[3] = -t38 * t39 * (Q3 / 0.2e1 * t24 - 0.3e1 * t25 - 0.3e1 * t28) * t33;
        
        // dPotSRP[4] = -3 / 2 * [cr * 0.45605E-05 * 1.49598022291E11² * 1 / rp²] * [surface * a]
        //              * (-2 * (iy * yp + [iy * (1 / MathLib.sqrt(1 - ix² - iy²)) * zp * ix])
        //              * ex - (-4 * ix * yp + 2 * iy * xp + 2 * MathLib.sqrt(1 - ix² - iy²) * zp 
        //              - 2 * ix² * [1 / MathLib.sqrt(1 - ix² - iy²)] * zp) * ey) / mass
        dPotSRP[4] = Q3 / 0.2e1 * t38 * t39
            * (Q2 * (iy * yp + t58) * ex - (Q4 * t61 + 0.2e1 * t63 + t66 - 0.2e1 * t14 * t55 * zp) * ey)
            * t33;
        
        // dPotSRP[5] = -3 / 2 * [cr * 0.45605E-05 * 1.49598022291E11² * 1 / rp²] * [surface * a]
        //              * (-(-4 * [iy * xp] + 2 * [ix * yp] - [2 * MathLib.sqrt(1 - ix² - iy²) * zp]
        //              + 2 * iy² * [1 / MathLib.sqrt(1 - ix² - iy²)] * zp) * ex 
        //              - 2 * (ix * xp - [iy * [1 / MathLib.sqrt(1 - ix² - iy²)] * zp * ix]) * ey)
        //              / mass
        dPotSRP[5] = Q3 / 0.2e1 * t38 * t39
            * (-(Q4 * t63 + 0.2e1 * t61 - t66 + 0.2e1 * t7 * t55 * zp) * ex - 0.2e1 * (ix * xp - t58) * ey)
            * t33;

        // Set potential derivative
        this.dPot = dPotSRP;

        // Return computation result
        return dPotSRP;

    }

    /** {@inheritDoc} */
    @Override
    public double[] computeShortPeriods(final StelaEquinoctialOrbit orbit,
            final OrbitNatureConverter converter) {
        return new double[6];
    }

    /** {@inheritDoc} */
    // CHECKSTYLE: stop MethodLength check
    @Override
    public double[][] computePartialDerivatives(final StelaEquinoctialOrbit orbit) throws PatriusException {
        // CHECKSTYLE: resume MethodLength check

        // Initialize partial derivatives array
        final double[][] partialDerivatives = new double[6][6];

        // Get orbit parameters
        final double a = orbit.getA();
        final double ex = orbit.getEquinoctialEx();
        final double ey = orbit.getEquinoctialEy();
        final double ix = orbit.getIx();
        final double iy = orbit.getIy();

        // Get sun position
        final Vector3D sunP = this.inSun.getPVCoordinates(orbit.getDate(), orbit.getFrame()).getPosition();

        // Nomralize position
        final double rp = sunP.getNorm();
        final Vector3D sunCD = sunP.scalarMultiply(MathLib.divide(1., rp));
        final double xp = sunCD.getX();
        final double yp = sunCD.getY();
        final double zp = sunCD.getZ();

        // Intermediate computation factors
        final double t1 = this.cr * C0;
        final double t2 = UA * UA;
        final double t3 = t1 * t2;
        final double t4 = rp * rp;
        // 1 / rp²
        final double t5 = 0.1e1 / t4;
        // surface / rp²
        final double t6 = t5 * this.surface;
        final double t7 = iy * iy;
        final double t12 = ix * iy;
        final double t15 = ix * ix;
        // 1 - ix² - iy²
        final double t16 = 0.1e1 - t15 - t7;
        final double t17 = MathLib.sqrt(t16);
        final double t22 = 0.1e1 / this.mass;
        // t25 = cr * 4.5605E-6 * 1.49598022291E11² * surface / rp² * (-3 / 2 * (1 - 2 * iy²) * xp
        //       - 3 * ix * iy * yp + 3 * iy * MathLib.sqrt(1 - ix² - iy²) * zp) / mass
        final double t25 = t3 * t6
            * (-0.3e1 / 0.2e1 * (0.1e1 - 0.2e1 * t7) * xp - 0.3e1 * t12 * yp + 0.3e1 * iy * t17 * zp) * t22;
        // t38 = cr * 4.5605E-6 * 1.49598022291E11² * surface / rp² * (-3 / 2 * (1 - 2 * ix²) * yp
        //       - 3 * ix * iy * xp - 3 * ix * MathLib.sqrt(1 - ix² - iy²) * zp) / mass
        final double t38 = t3 * t6
            * (-0.3e1 / 0.2e1 * (0.1e1 - 0.2e1 * t15) * yp - 0.3e1 * t12 * xp - 0.3e1 * ix * t17 * zp) * t22;
        // 1 / MathLib.sqrt(1 - ix² - iy²)
        final double t40 = 0.1e1 / t17;
        // iy / MathLib.sqrt(1 - ix² - iy²)
        final double t41 = iy * t40;
        final double t42 = zp * ix;
        // (iy / MathLib.sqrt(1 - ix² - iy²)) * (zp * ix)
        final double t43 = t41 * t42;
        final double t44 = iy * yp + t43;
        final double t46 = ix * yp;
        final double t48 = iy * xp;
        // zp * MathLib.sqrt(1 - ix² - iy²)
        final double t50 = t17 * zp;
        final double t51 = 0.2e1 * t50;
        final double t53 = t15 * t40 * zp;
        // t60 = 3 / 2 * cr * 4.5605E-6 * 1.49598022291E11² * surface / rp² * (-2 
        //       * [iy * yp + (iy / MathLib.sqrt(1 - ix² - iy²)) * (zp * ix)] * ex 
        //       - (-4 * ix * yp + 2 * iy * xp + [2 * zp * MathLib.sqrt(1 - ix² - iy²)]
        //       - 2 * [ix² * (1 / MathLib.sqrt(1 - ix² - iy²)) * zp]) * ey) / mass
        final double t60 = 0.3e1 / 0.2e1 * t3 * t6
            * (-0.2e1 * t44 * ex - (-0.4e1 * t46 + 0.2e1 * t48 + t51 - 0.2e1 * t53) * ey) * t22;
        final double t64 = t7 * t40 * zp;
        final double t69 = ix * xp - t43;
        final double t74 = 0.3e1 / 0.2e1 * t3 * t6
            * (-(-0.4e1 * t48 + 0.2e1 * t46 - t51 + 0.2e1 * t64) * ex - 0.2e1 * t69 * ey) * t22;
        final double t76 = t1 * t2 * t5;
        final double t77 = this.surface * a;
        final double t80 = -0.3e1 * t76 * t77 * t44 * t22;
        final double t83 = 0.3e1 * t50;
        final double t88 = t76 * t77 * (0.6e1 * t48 - 0.3e1 * t46 + t83 - 0.3e1 * t64) * t22;
        final double t95 = t76 * t77 * (0.6e1 * t46 - 0.3e1 * t48 - t83 + 0.3e1 * t53) * t22;
        final double t98 = -0.3e1 * t76 * t77 * t69 * t22;
        final double t100 = 0.1e1 / t17 / t16;
        final double t103 = iy * t100 * zp * t15;
        final double t104 = t41 * zp;
        final double t109 = t40 * zp * ix;
        final double t122 = t7 * t100 * t42;
        final double t130 = 0.3e1 / 0.2e1 * t76 * t77
            * (-0.2e1 * (yp + t109 + t122) * ex - 0.2e1 * (xp - t104 - t103) * ey) * t22;

        partialDerivatives[0][2] = -t25;
        partialDerivatives[0][3] = -t38;
        partialDerivatives[0][4] = -t60;
        partialDerivatives[0][5] = -t74;
        partialDerivatives[2][0] = -t25;
        partialDerivatives[2][4] = -t80;
        partialDerivatives[2][5] = -t88;
        partialDerivatives[3][0] = -t38;
        partialDerivatives[3][4] = -t95;
        partialDerivatives[3][5] = -t98;
        partialDerivatives[4][0] = -t60;
        partialDerivatives[4][2] = -t80;
        partialDerivatives[4][3] = -t95;
        partialDerivatives[4][4] = Q3 / 0.2e1 * t76 * t77
            * (Q2 * (t103 + t104) * ex - (Q4 * yp - 0.6e1 * t109 - 0.2e1 * t15 * ix * t100 * zp) * ey)
            * t22;
        partialDerivatives[4][5] = -t130;
        partialDerivatives[5][0] = -t74;
        partialDerivatives[5][2] = -t88;
        partialDerivatives[5][3] = -t98;
        partialDerivatives[5][4] = -t130;
        partialDerivatives[5][5] = Q3 / 0.2e1 * t76 * t77
            * (-(Q4 * xp + 0.6e1 * t104 + 0.2e1 * t7 * iy * t100 * zp) * ex - 0.2e1 * (-t109 - t122) * ey)
            * t22;

        // Return computed partial derivatives
        return partialDerivatives;
    }

}
