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
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:64:30/05/2013:renamed class
 * VERSION::FA:183:14/03/2014:Corrected javadoc
 * VERSION::DM:315:26/02/2015:add zonal terms J8 to J15
 * VERSION::DM:317:04/03/2015: STELA integration in CIRF with referential choice (ICRF, CIRF or MOD)
 * VERSION::DM:523:08/02/2016: add solid tides effects in STELA PATRIUS
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.stela.forces.gravity;

import fr.cnes.sirius.patrius.forces.gravity.potential.PotentialCoefficientsProvider;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.stela.JavaMathAdapter;
import fr.cnes.sirius.patrius.stela.forces.AbstractStelaLagrangeContribution;
import fr.cnes.sirius.patrius.stela.orbits.StelaEquinoctialOrbit;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

//CHECKSTYLE:OFF

/**
 * <p>
 * Class computing Zonal perturbations
 * </p>
 * <p>
 * computes Zonal perturbations, short periods and partial derivatives depending on the degree of development asked
 * </p>
 * <p>
 * The class is adapted from STELA EarthPotentialGTO in fr.cnes.los.stela.elib.business.implementation.earthPotential
 * </p>
 * 
 * @concurrency conditionally thread-safe
 * @concurrency.comment thread safe if the PotentialCoefficientsProvider used is thread safe
 * 
 * 
 * @author Cedric Dental
 * 
 * @version $Id$
 * 
 * @since 1.3
 * 
 */
public class StelaZonalAttraction extends AbstractStelaLagrangeContribution {

     /** Serializable UID. */
    private static final long serialVersionUID = 4836663314116306825L;

    /** Central body reference radius (m). */
    private final double rEq;
    /** Central body coefficients */
    private double[] j;
    /** Degree of development for zonal perturbations */
    private final int zonalDegreeMaxPerturbation;
    /** Boolean: true J2 is computed */
    private final boolean isJ2SquareComputed;
    /** Degree of development for zonal short periods */
    private final int zonalDegreeMaxSP;
    /** Degree of development for zonal partial derivatives */
    private final int zonalDegreeMaxPD;
    /** Boolean: true partial derivatives from J2 are computed */
    private final boolean isJ2SquareParDerComputed;

    /**
     * Constructor.
     * 
     * @param provider
     *        gravity field coefficients provider
     * @param inZonalDegreeMaxPerturbation
     *        degree of development for zonal perturbations (max 15)
     * @param inIsJ2SquareComputed
     *        true J2² is computed
     * @param inZonalDegreeMaxSP
     *        the degree of development for zonal short periods (max 2)
     * @param inZonalDegreeMaxPD
     *        the degree of development for zonal partial derivatives (max 7)
     * @param inIsJ2SquareParDerComputed
     *        true partial derivatives from J2 are computed
     * 
     * @throws PatriusException
     *         Orekit exception needed for using the provider
     */
    public StelaZonalAttraction(final PotentialCoefficientsProvider provider, final int inZonalDegreeMaxPerturbation,
        final boolean inIsJ2SquareComputed, final int inZonalDegreeMaxSP, final int inZonalDegreeMaxPD,
        final boolean inIsJ2SquareParDerComputed)
        throws PatriusException {

        this.zonalDegreeMaxPerturbation = inZonalDegreeMaxPerturbation;
        this.isJ2SquareComputed = inIsJ2SquareComputed;
        this.zonalDegreeMaxSP = inZonalDegreeMaxSP;
        this.zonalDegreeMaxPD = inZonalDegreeMaxPD;
        this.isJ2SquareParDerComputed = inIsJ2SquareParDerComputed;

        this.rEq = provider.getAe();
        this.j = new double[this.zonalDegreeMaxPerturbation + 1];
        this.j = provider.getJ(false, this.zonalDegreeMaxPerturbation);

    }

    @Override
    public double[] computePerturbation(final StelaEquinoctialOrbit orbit) {

        // From J8, methods are set in another file because there are too long (thousands of lines)

        double[] dPotZonal = new double[6];
        switch (this.zonalDegreeMaxPerturbation) {
            case 2:
                dPotZonal = this.computeJ2(orbit);
                break;
            case 3:
                dPotZonal = this.computeJ3(orbit);
                break;
            case 4:
                dPotZonal = this.computeJ4(orbit);
                break;
            case 5:
                dPotZonal = this.computeJ5(orbit);
                break;
            case 6:
                dPotZonal = this.computeJ6(orbit);
                break;
            case 7:
                dPotZonal = this.computeJ7(orbit);
                break;
            case 8:
                dPotZonal = this.computeJ8(orbit);
                break;
            case 9:
                dPotZonal = this.computeJ9(orbit);
                break;
            case 10:
                dPotZonal = this.computeJ10(orbit);
                break;
            case 11:
                dPotZonal = this.computeJ11(orbit);
                break;
            case 12:
                dPotZonal = this.computeJ12(orbit);
                break;
            case 13:
                dPotZonal = this.computeJ13(orbit);
                break;
            case 14:
                dPotZonal = this.computeJ14(orbit);
                break;
            case 15:
                dPotZonal = this.computeJ15(orbit);
                break;
            default:
                break;
        }

        return dPotZonal;
    }

    @Override
    public double[] computeShortPeriods(final StelaEquinoctialOrbit orbit) {

        double[] shortPeriods = new double[6];
        if (this.zonalDegreeMaxSP >= 2) {
            // Currently only J2 short periods are available
            shortPeriods = this.computeJ2ShortPeriods(orbit);
        }

        return shortPeriods;
    }

    @Override
    public double[][] computePartialDerivatives(final StelaEquinoctialOrbit orbit) {

        double[][] partialDerivatives = new double[6][6];
        switch (this.zonalDegreeMaxPD) {
            case 2:
                partialDerivatives = this.computeJ2PartialDerivatives(orbit);
                break;
            case 3:
                partialDerivatives = this.computeJ3PartialDerivatives(orbit);
                break;
            case 4:
                partialDerivatives = this.computeJ4PartialDerivatives(orbit);
                break;
            case 5:
                partialDerivatives = this.computeJ5PartialDerivatives(orbit);
                break;
            case 6:
                partialDerivatives = this.computeJ6PartialDerivatives(orbit);
                break;
            case 7:
                partialDerivatives = this.computeJ7PartialDerivatives(orbit);
                break;
            default:
                break;
        }

        return partialDerivatives;
    }

    /**
     * <p>
     * Compute the effect of the 2nd degree development of the Zonal Perturbation
     * </p>
     * 
     * @param orbit
     *        an orbit
     * @return the J2 perturbation
     */
    public double[] computeJ2(final StelaEquinoctialOrbit orbit) {
        return this.computeJ2(orbit, orbit.getMu());
    }

    /**
     * <p>
     * Compute the effect of the 2nd degree development of the Zonal Perturbation with specific mu.
     * </p>
     * 
     * @param orbit
     *        an orbit
     * @param mu
     *        mu
     * @return the J2 perturbation
     */
    public double[] computeJ2(final StelaEquinoctialOrbit orbit, final double mu) {

        /** Orbital elements */
        final double a = orbit.getA();
        final double ex = orbit.getEquinoctialEx();
        final double ey = orbit.getEquinoctialEy();
        final double ix = orbit.getIx();
        final double iy = orbit.getIy();

        /** J2 Potential */
        final double[] dPotJ2 = new double[6];

        final double t2 = this.rEq * this.rEq;
        final double t3 = mu * this.j[2] * t2;
        final double t4 = ix * ix;
        final double t5 = iy * iy;
        final double t6 = 0.1e1 - t4 - t5;
        final double t7 = t4 + t5;
        final double t10 = -0.1e1 + 0.6e1 * t6 * t7;
        final double t11 = a * a;
        final double t12 = t11 * t11;
        final double t15 = ex * ex;
        final double t16 = ey * ey;
        final double t17 = 0.1e1 - t15 - t16;
        final double t18 = MathLib.sqrt(t17);
        final double t20 = 0.1e1 / t18 / t17;
        final double t25 = 0.1e1 / t11 / a;
        final double t26 = t10 * t25;
        final double t27 = t17 * t17;
        final double t29 = 0.1e1 / t18 / t27;

        dPotJ2[0] = 0.3e1 / 0.2e1 * t3 * t10 / t12 * t20;
        dPotJ2[1] = 0.0e0;
        dPotJ2[2] = -0.3e1 / 0.2e1 * t3 * t26 * t29 * ex;
        dPotJ2[3] = -0.3e1 / 0.2e1 * t3 * t26 * t29 * ey;
        dPotJ2[4] = -0.6e1 * t3 * (-t7 * ix + t6 * ix) * t25 * t20;
        dPotJ2[5] = -0.6e1 * t3 * (-t7 * iy + t6 * iy) * t25 * t20;

        if (this.zonalDegreeMaxPD == 2) {
            this.dPot = dPotJ2.clone();
        }

        return dPotJ2;

    }

    /**
     * <p>
     * Compute the effect of the 3rd degree development of the Zonal Perturbation
     * </p>
     * 
     * @param orbit
     *        an orbit
     * @return the J3 perturbation
     */
    public double[] computeJ3(final StelaEquinoctialOrbit orbit) {
        return this.computeJ3(orbit, orbit.getMu());
    }

    /**
     * <p>
     * Compute the effect of the 3rd degree development of the Zonal Perturbation with specific mu
     * </p>
     * 
     * @param orbit
     *        an orbit
     * @param mu
     *        mu
     * @return the J3 perturbation
     */
    public double[] computeJ3(final StelaEquinoctialOrbit orbit, final double mu) {

        /** Orbital elements */
        final double a = orbit.getA();
        final double ex = orbit.getEquinoctialEx();
        final double ey = orbit.getEquinoctialEy();
        final double ix = orbit.getIx();
        final double iy = orbit.getIy();

        /** J3 Potential */
        final double[] dPotJ3 = this.computeJ2(orbit, mu);

        final double t1 = mu * this.j[3];
        final double t2 = this.rEq * this.rEq;
        final double t3 = t2 * this.rEq;
        final double t4 = ix * ix;
        final double t5 = iy * iy;
        final double t6 = 0.1e1 - t4 - t5;
        final double t7 = MathLib.sqrt(t6);
        final double t9 = t1 * t3 * t7;
        final double t10 = t4 + t5;
        final double t13 = 0.5e1 * t6 * t10 - 0.1e1;
        final double t16 = -ey * ix + ex * iy;
        final double t17 = t13 * t16;
        final double t18 = a * a;
        final double t19 = t18 * t18;
        final double t22 = ex * ex;
        final double t23 = ey * ey;
        final double t24 = 0.1e1 - t22 - t23;
        final double t25 = t24 * t24;
        final double t26 = MathLib.sqrt(t24);
        final double t28 = 0.1e1 / t26 / t25;
        final double t34 = 0.1e1 / t19;
        final double t35 = t34 * t28;
        final double t42 = t34 / t26 / t25 / t24;
        final double t59 = t1 * t3 / t7;

        dPotJ3[0] += -0.12e2 * t9 * t17 / t19 / a * t28;
        dPotJ3[1] += 0.0e0;
        dPotJ3[2] += 0.3e1 * t9 * t13 * iy * t35 + 0.15e2 * t9 * t17 * t42 * ex;
        dPotJ3[3] += -0.3e1 * t9 * t13 * ix * t35 + 0.15e2 * t9 * t17 * t42 * ey;
        dPotJ3[4] += -0.3e1 * t59 * t17 * t35 * ix + 0.30e2 * t9 * (-ix * t10 + t6 * ix) * t16 * t35 - 0.3e1 * t9 * t13
            * ey * t35;
        dPotJ3[5] += -0.3e1 * t59 * t17 * t35 * iy + 0.30e2 * t9 * (-iy * t10 + t6 * iy) * t16 * t35 + 0.3e1 * t9 * t13
            * ex * t35;

        if (this.zonalDegreeMaxPD == 3) {
            this.dPot = dPotJ3.clone();
        }

        return dPotJ3;
    }

    /**
     * <p>
     * Compute the effect of the 4th degree development of the Zonal Perturbation
     * </p>
     * 
     * @param orbit
     *        an orbit
     * @return the J4 perturbation
     */
    public double[] computeJ4(final StelaEquinoctialOrbit orbit) {

        /** Orbital elements */
        final double a = orbit.getA();
        final double ex = orbit.getEquinoctialEx();
        final double ey = orbit.getEquinoctialEy();
        final double ix = orbit.getIx();
        final double iy = orbit.getIy();
        final double MU = orbit.getMu();

        /** J4 Potential */
        final double[] dPotJ4 = this.computeJ3(orbit);

        final double t2 = this.rEq * this.rEq;
        final double t3 = t2 * t2;
        final double t4 = MU * this.j[4] * t3;
        final double t5 = ix * ix;
        final double t6 = iy * iy;
        final double t7 = 0.1e1 - t5 - t6;
        final double t8 = t5 + t6;
        final double t9 = t7 * t8;
        final double t10 = ex * ex;
        final double t11 = ey * ey;
        final double t12 = t10 + t11;
        final double t15 = t7 * t7;
        final double t16 = t8 * t8;
        final double t17 = t15 * t16;
        final double t24 = t10 * t7;
        final double t27 = ex * iy;
        final double t28 = t7 * ix;
        final double t32 = t6 * t7;
        final double t35 = t10 * t15;
        final double t36 = t8 * t5;
        final double t40 = t8 * ix;
        final double t41 = t40 * ey;
        final double t44 = t6 * t15;
        final double t45 = t8 * t11;
        final double t48 = -0.90e2 * t9 * t12 + 0.350e3 * t17 * t12 - 0.40e2 * t9 + 0.3e1 * t10 + 0.3e1 * t11 + 0.140e3
            * t17 + 0.60e2 * t24 * t5 + 0.120e3 * t27 * t28 * ey + 0.60e2 * t32 * t11 - 0.280e3 * t35 * t36
            - 0.560e3 * t27 * t15 * t41 - 0.280e3 * t44 * t45 + 0.2e1;
        final double t49 = a * a;
        final double t50 = t49 * t49;
        final double t54 = 0.1e1 - t10 - t11;
        final double t55 = t54 * t54;
        final double t57 = MathLib.sqrt(t54);
        final double t59 = 0.1e1 / t57 / t55 / t54;
        final double t68 = ex * t7;
        final double t71 = t7 * iy;
        final double t72 = ey * ix;
        final double t75 = ex * t15;
        final double t78 = iy * t15;
        final double t83 = 0.1e1 / t50 / a;
        final double t88 = t48 * t83;
        final double t89 = t55 * t55;
        final double t91 = 0.1e1 / t57 / t89;
        final double t106 = t15 * t8;
        final double t107 = t106 * ix;
        final double t127 = t7 * t16;
        final double t128 = t12 * ix;
        final double t138 = t5 * ix;
        final double t177 = -0.240e3 * t27 * t5 * ey + 0.120e3 * t27 * t7 * ey - 0.120e3 * t6 * ix * t11 + 0.1120e4
            * t24 * t8 * t138 - 0.560e3 * t35 * t138 - 0.560e3 * t35 * t40 + 0.2240e4 * t27 * t7 * t36 * ey
            - 0.1120e4 * t27 * t15 * t5 * ey - 0.560e3 * t27 * t106 * ey + 0.1120e4 * t32 * t45 * ix - 0.560e3
            * t44 * ix * t11;
        final double t183 = iy * t8;
        final double t188 = t12 * iy;
        final double t205 = ex * t6;
        final double t210 = t6 * iy;
        final double t236 = -0.240e3 * t205 * t72 + 0.120e3 * t71 * t11 - 0.120e3 * t210 * t11 + 0.1120e4 * t24 * t36
            * iy - 0.560e3 * t35 * iy * t5 - 0.560e3 * t75 * t41 + 0.2240e4 * t205 * t7 * t41 - 0.1120e4 * t205
            * t15 * ix * ey - 0.560e3 * t78 * t45 + 0.1120e4 * t210 * t7 * t45 - 0.560e3 * t210 * t15 * t11;

        dPotJ4[0] += 0.15e2 / 0.16e2 * t4 * t48 / t50 / t49 * t59;
        dPotJ4[1] += 0.0e0;
        dPotJ4[2] += -0.3e1
            / 0.16e2
            * t4
            * (-0.180e3 * t9 * ex + 0.700e3 * t17 * ex + 0.6e1 * ex + 0.120e3 * t68 * t5 + 0.120e3 * t71 * t72
                - 0.560e3 * t75 * t36 - 0.560e3 * t78 * t41) * t83 * t59 - 0.21e2 / 0.16e2 * t4 * t88 * t91
            * ex;
        dPotJ4[3] += -0.3e1
            / 0.16e2
            * t4
            * (-0.180e3 * t9 * ey + 0.700e3 * t17 * ey + 0.6e1 * ey + 0.120e3 * t27 * t28 + 0.120e3 * t32 * ey
                - 0.560e3 * t27 * t107 - 0.560e3 * t44 * t8 * ey) * t83 * t59 - 0.21e2 / 0.16e2 * t4 * t88
            * t91 * ey;
        dPotJ4[4] += -0.3e1
            / 0.16e2
            * t4
            * (0.180e3 * t40 * t12 - 0.180e3 * t28 * t12 - 0.1400e4 * t127 * t128 + 0.1400e4 * t106 * t128 + 0.80e2
                * t40 - 0.80e2 * t28 - 0.560e3 * t127 * ix + 0.560e3 * t107 - 0.120e3 * t10 * t138 + 0.120e3
                * t24 * ix + t177) * t83 * t59;
        dPotJ4[5] += -0.3e1
            / 0.16e2
            * t4
            * (0.180e3 * t183 * t12 - 0.180e3 * t71 * t12 - 0.1400e4 * t127 * t188 + 0.1400e4 * t106 * t188
                + 0.80e2 * t183 - 0.80e2 * t71 - 0.560e3 * t127 * iy + 0.560e3 * t106 * iy - 0.120e3 * t10 * iy
                * t5 + 0.120e3 * t68 * t72 + t236) * t83 * t59;

        if (this.zonalDegreeMaxPD == 4) {
            this.dPot = dPotJ4.clone();
        }
        return dPotJ4;

    }

    /**
     * <p>
     * Compute the effect of the 5th degree development of the Zonal Perturbation
     * </p>
     * 
     * @param orbit
     *        an orbit
     * @return the J5 perturbation
     */
    public double[] computeJ5(final StelaEquinoctialOrbit orbit) {

        /** Orbital elements */
        final double a = orbit.getA();
        final double ex = orbit.getEquinoctialEx();
        final double ey = orbit.getEquinoctialEy();
        final double ix = orbit.getIx();
        final double iy = orbit.getIy();
        final double MU = orbit.getMu();

        /** J5 Potential */
        final double[] dPotJ5 = this.computeJ4(orbit);

        final double t1 = MU * this.j[5];
        final double t2 = this.rEq * this.rEq;
        final double t3 = t2 * t2;
        final double t4 = t3 * this.rEq;
        final double t5 = ix * ix;
        final double t6 = iy * iy;
        final double t7 = 0.1e1 - t5 - t6;
        final double t8 = MathLib.sqrt(t7);
        final double t10 = t1 * t4 * t8;
        final double t11 = ey * ix;
        final double t12 = ex * iy;
        final double t13 = -t11 + t12;
        final double t14 = ex * ex;
        final double t15 = t14 * t7;
        final double t18 = t7 * t7;
        final double t19 = t14 * t18;
        final double t20 = t5 + t6;
        final double t21 = t20 * t5;
        final double t24 = t7 * ix;
        final double t29 = t20 * ix;
        final double t30 = t29 * ey;
        final double t33 = t6 * t7;
        final double t34 = ey * ey;
        final double t37 = t6 * t18;
        final double t38 = t20 * t34;
        final double t41 = t7 * t20;
        final double t43 = t20 * t20;
        final double t44 = t18 * t43;
        final double t45 = t14 + t34;
        final double t53 = 0.56e2 * t15 * t5 - 0.252e3 * t19 * t21 + 0.112e3 * t12 * t24 * ey - 0.504e3 * t12 * t18
            * t30 + 0.56e2 * t33 * t34 - 0.252e3 * t37 * t38 - 0.168e3 * t41 + 0.441e3 * t44 * t45 + 0.504e3 * t44
            + 0.9e1 * t14 + 0.9e1 * t34 - 0.140e3 * t41 * t45 + 0.12e2;
        final double t54 = t13 * t53;
        final double t55 = a * a;
        final double t57 = t55 * t55;
        final double t60 = 0.1e1 - t14 - t34;
        final double t61 = t60 * t60;
        final double t62 = t61 * t61;
        final double t63 = MathLib.sqrt(t60);
        final double t65 = 0.1e1 / t63 / t62;
        final double t72 = 0.1e1 / t57 / t55;
        final double t73 = t72 * t65;
        final double t77 = ex * t7;
        final double t80 = ex * t18;
        final double t83 = t7 * iy;
        final double t86 = iy * t18;
        final double t102 = t72 / t63 / t62 / t60;
        final double t114 = t18 * t20;
        final double t115 = t114 * ix;
        final double t140 = t1 * t4 / t8;
        final double t147 = t5 * ix;
        final double t188 = t7 * t43;
        final double t189 = t45 * ix;
        final double t201 = -0.112e3 * t6 * ix * t34 + 0.1008e4 * t33 * t38 * ix - 0.504e3 * t37 * ix * t34 + 0.336e3
            * t29 - 0.336e3 * t24 - 0.1764e4 * t188 * t189 + 0.1764e4 * t114 * t189 - 0.2016e4 * t188 * ix
            + 0.2016e4 * t115 + 0.280e3 * t29 * t45 - 0.280e3 * t24 * t45;
        final double t224 = ex * t6;
        final double t238 = t6 * iy;
        final double t250 = iy * t20;
        final double t253 = t45 * iy;
        final double t266 = -0.504e3 * t86 * t38 + 0.1008e4 * t238 * t7 * t38 - 0.504e3 * t238 * t18 * t34 + 0.336e3
            * t250 - 0.336e3 * t83 - 0.1764e4 * t188 * t253 + 0.1764e4 * t114 * t253 - 0.2016e4 * t188 * iy
            + 0.2016e4 * t114 * iy + 0.280e3 * t250 * t45 - 0.280e3 * t83 * t45;

        dPotJ5[0] += -0.15e2 / 0.4e1 * t10 * t54 / t57 / t55 / a * t65;
        dPotJ5[1] += 0.0e0;
        dPotJ5[2] += 0.5e1
            / 0.8e1
            * t10
            * iy
            * t53
            * t73
            + 0.5e1
            / 0.8e1
            * t10
            * t13
            * (0.112e3 * t77 * t5 - 0.504e3 * t80 * t21 + 0.112e3 * t83 * t11 - 0.504e3 * t86 * t30 + 0.882e3 * t44
                * ex + 0.18e2 * ex - 0.280e3 * t41 * ex) * t73 + 0.45e2 / 0.8e1 * t10 * t54 * t102 * ex;
        dPotJ5[3] += -0.5e1
            / 0.8e1
            * t10
            * ix
            * t53
            * t73
            + 0.5e1
            / 0.8e1
            * t10
            * t13
            * (0.112e3 * t12 * t24 - 0.504e3 * t12 * t115 + 0.112e3 * t33 * ey - 0.504e3 * t37 * t20 * ey + 0.882e3
                * t44 * ey + 0.18e2 * ey - 0.280e3 * t41 * ey) * t73 + 0.45e2 / 0.8e1 * t10 * t54 * t102 * ey;
        dPotJ5[4] += -0.5e1
            / 0.8e1
            * t140
            * t54
            * t73
            * ix
            - 0.5e1
            / 0.8e1
            * t10
            * ey
            * t53
            * t73
            + 0.5e1
            / 0.8e1
            * t10
            * t13
            * (-0.112e3 * t14 * t147 + 0.112e3 * t15 * ix + 0.1008e4 * t15 * t20 * t147 - 0.504e3 * t19 * t147
                - 0.504e3 * t19 * t29 - 0.224e3 * t12 * t5 * ey + 0.112e3 * t12 * t7 * ey + 0.2016e4 * t12 * t7
                * t21 * ey - 0.1008e4 * t12 * t18 * t5 * ey - 0.504e3 * t12 * t114 * ey + t201) * t73;
        dPotJ5[5] += -0.5e1
            / 0.8e1
            * t140
            * t54
            * t73
            * iy
            + 0.5e1
            / 0.8e1
            * t10
            * ex
            * t53
            * t73
            + 0.5e1
            / 0.8e1
            * t10
            * t13
            * (-0.112e3 * t14 * iy * t5 + 0.1008e4 * t15 * t21 * iy - 0.504e3 * t19 * iy * t5 + 0.112e3 * t77 * t11
                - 0.224e3 * t224 * t11 - 0.504e3 * t80 * t30 + 0.2016e4 * t224 * t7 * t30 - 0.1008e4 * t224
                * t18 * ix * ey + 0.112e3 * t83 * t34 - 0.112e3 * t238 * t34 + t266) * t73;

        if (this.zonalDegreeMaxPD == 5) {
            this.dPot = dPotJ5.clone();
        }
        return dPotJ5;
    }

    /**
     * <p>
     * Compute the effect of the 6th degree development of the Zonal Perturbation
     * </p>
     * 
     * @param orbit
     *        an orbit
     * @return the J6 perturbation
     */
    public double[] computeJ6(final StelaEquinoctialOrbit orbit) {

        /** Orbital elements */
        final double a = orbit.getA();
        final double ex = orbit.getEquinoctialEx();
        final double ey = orbit.getEquinoctialEy();
        final double ix = orbit.getIx();
        final double iy = orbit.getIy();
        final double MU = orbit.getMu();

        /** J6 Potential */
        final double[] dPotJ6 = this.computeJ5(orbit);

        final double t2 = this.rEq * this.rEq;
        final double t3 = t2 * t2;
        final double t5 = MU * this.j[6] * t3 * t2;
        final double t6 = ex * ex;
        final double t7 = ey * ey;
        final double t8 = t6 + t7;
        final double t9 = ix * ix;
        final double t10 = iy * iy;
        final double t11 = 0.1e1 - t9 - t10;
        final double t12 = t11 * t11;
        final double t13 = t12 * t11;
        final double t14 = t8 * t13;
        final double t15 = t9 + t10;
        final double t16 = t15 * t15;
        final double t17 = t14 * t16;
        final double t18 = ex * ix;
        final double t19 = ey * iy;
        final double t20 = t18 * t19;
        final double t23 = t12 * t15;
        final double t24 = t6 * t9;
        final double t27 = t8 * t12;
        final double t28 = t27 * t15;
        final double t31 = t13 * t15;
        final double t32 = t6 * t6;
        final double t33 = t9 * t9;
        final double t34 = t32 * t33;
        final double t37 = t13 * t16;
        final double t38 = t7 * t10;
        final double t43 = t7 * t7;
        final double t44 = t10 * t10;
        final double t45 = t43 * t44;
        final double t50 = t8 * t11;
        final double t56 = ix * ey;
        final double t57 = t56 * iy;
        final double t63 = t31 * t6;
        final double t64 = t9 * t7;
        final double t65 = t64 * t10;
        final double t68 = t6 * ex;
        final double t69 = t31 * t68;
        final double t70 = t9 * ix;
        final double t71 = t70 * ey;
        final double t72 = t71 * iy;
        final double t75 = t23 * ex;
        final double t78 = t31 * ex;
        final double t79 = t7 * ey;
        final double t80 = t79 * ix;
        final double t81 = t10 * iy;
        final double t82 = t80 * t81;
        final double t85 = t11 * t6;
        final double t88 = t12 * t32;
        final double t91 = t11 * t7;
        final double t94 = -0.8e1 - 0.77616e5 * t17 * t20 + 0.20160e5 * t23 * t24 + 0.25200e5 * t28 * t20 + 0.11088e5
            * t31 * t34 - 0.55440e5 * t37 * t38 + 0.20160e5 * t23 * t38 + 0.11088e5 * t31 * t45 - 0.55440e5 * t37
            * t24 - 0.840e3 * t50 * t24 - 0.840e3 * t50 * t38 - 0.1680e4 * t50 * ex * t57 - 0.110880e6 * t37 * ex
            * t57 + 0.66528e5 * t63 * t65 + 0.44352e5 * t69 * t72 + 0.40320e5 * t75 * t57 + 0.44352e5 * t78 * t82
            - 0.1680e4 * t85 * t9 - 0.2520e4 * t88 * t33 - 0.1680e4 * t91 * t10;
        final double t95 = t12 * t43;
        final double t98 = t16 * t15;
        final double t101 = t27 * t16;
        final double t103 = t8 * t8;
        final double t104 = t103 * t13;
        final double t107 = t103 * t11;
        final double t110 = t15 * t7;
        final double t111 = t110 * t10;
        final double t114 = t16 * t7;
        final double t115 = t114 * t10;
        final double t118 = t15 * t6;
        final double t119 = t118 * t9;
        final double t122 = t16 * t6;
        final double t123 = t122 * t9;
        final double t126 = t11 * ex;
        final double t130 = t12 * ex;
        final double t133 = t12 * t6;
        final double t136 = t12 * t68;
        final double t141 = t12 * t16;
        final double t143 = t11 * t15;
        final double t145 = t50 * t15;
        final double t147 = t103 * t12;
        final double t153 = -0.10080e5 * t130 * t82 - 0.15120e5 * t133 * t65 - 0.10080e5 * t136 * t72 + 0.7392e4 * t13
            * t98 - 0.3024e4 * t141 + 0.336e3 * t143 + 0.2520e4 * t145 - 0.11025e5 * t147 * t16 - 0.15e2 * t103
            - 0.40e2 * t6 - 0.40e2 * t7;
        final double t155 = t94 - 0.2520e4 * t95 * t44 + 0.64680e5 * t14 * t98 - 0.25200e5 * t101 + 0.29106e5 * t104
            * t98 + 0.1050e4 * t107 * t15 + 0.12600e5 * t27 * t111 - 0.38808e5 * t14 * t115 + 0.12600e5 * t27
            * t119 - 0.38808e5 * t14 * t123 - 0.3360e4 * t126 * t57 + t153;
        final double t156 = a * a;
        final double t157 = t156 * t156;
        final double t158 = t157 * t157;
        final double t161 = 0.1e1 - t6 - t7;
        final double t162 = t161 * t161;
        final double t163 = t162 * t162;
        final double t165 = MathLib.sqrt(t161);
        final double t167 = 0.1e1 / t165 / t163 / t161;
        final double t177 = ex * t9;
        final double t195 = t16 * ex;
        final double t202 = t133 * t15;
        final double t205 = t15 * ex;
        final double t208 = t11 * ix;
        final double t211 = t12 * ix;
        final double t212 = t79 * t81;
        final double t221 = 0.116424e6 * t14 * t98 * ex + 0.44352e5 * t31 * t68 * t33 - 0.110880e6 * t37 * t177
            - 0.1680e4 * t126 * t38 + 0.40320e5 * t23 * t177 - 0.77616e5 * t68 * t13 * t16 * t9 + 0.133056e6 * t63
            * t72 + 0.133056e6 * t78 * t65 + 0.25200e5 * t136 * t15 * t9 - 0.44100e5 * t27 * t195 - 0.155232e6 * t6
            * t13 * t16 * t57 + 0.50400e5 * t202 * t57 + 0.4200e4 * t50 * t205 - 0.3360e4 * t208 * t19 - 0.10080e5
            * t211 * t212 - 0.1680e4 * t50 * t177 + 0.25200e5 * t28 * t57 - 0.77616e5 * t17 * t57;
        final double t222 = t68 * t11;
        final double t232 = ex * t13;
        final double t253 = t126 * t15;
        final double t261 = t130 * t16;
        final double t263 = -0.1680e4 * t222 * t9 - 0.80e2 * ex - 0.30240e5 * t130 * t65 - 0.30240e5 * t133 * t72
            + 0.25200e5 * t130 * t111 - 0.77616e5 * t232 * t115 + 0.44352e5 * t31 * t82 + 0.40320e5 * t23 * t57
            - 0.110880e6 * t37 * t57 - 0.1680e4 * t50 * t57 + 0.25200e5 * t27 * t205 * t9 - 0.77616e5 * t14 * t195
            * t9 - 0.3360e4 * t85 * t57 - 0.10080e5 * t136 * t33 + 0.5040e4 * t253 - 0.60e2 * t8 * ex - 0.3360e4
            * t126 * t9 + 0.129360e6 * t232 * t98 - 0.50400e5 * t261;
        final double t267 = 0.1e1 / t157 / t156 / a;
        final double t272 = t155 * t267;
        final double t275 = 0.1e1 / t165 / t163 / t162;
        final double t284 = ey * t11;
        final double t293 = ey * t10;
        final double t307 = t16 * ey;
        final double t310 = t79 * t12;
        final double t314 = ix * t7;
        final double t315 = t314 * t81;
        final double t318 = t9 * ey;
        final double t319 = t318 * t10;
        final double t322 = t15 * ey;
        final double t327 = t18 * iy;
        final double t339 = 0.44352e5 * t31 * t79 * t44 - 0.1680e4 * t284 * t24 - 0.3360e4 * t126 * ix * iy - 0.10080e5
            * t136 * t70 * iy - 0.1680e4 * t50 * t293 - 0.77616e5 * t79 * t13 * t16 * t10 - 0.110880e6 * t37 * t293
            + 0.40320e5 * t23 * t293 + 0.116424e6 * t14 * t98 * ey - 0.44100e5 * t27 * t307 + 0.25200e5 * t310
            * t15 * t10 + 0.133056e6 * t78 * t315 + 0.133056e6 * t63 * t319 + 0.4200e4 * t50 * t322 - 0.155232e6
            * t7 * t13 * t16 * t327 + 0.50400e5 * t7 * t12 * t15 * t327 - 0.1680e4 * t79 * t11 * t10 + 0.5040e4
            * t284 * t15;
        final double t340 = ey * t12;
        final double t360 = t68 * t70;
        final double t370 = ey * t13;
        final double t383 = -0.50400e5 * t340 * t16 - 0.80e2 * ey - 0.77616e5 * t17 * t327 + 0.25200e5 * t28 * t327
            + 0.40320e5 * t23 * t327 - 0.110880e6 * t37 * t327 - 0.1680e4 * t50 * t327 + 0.25200e5 * t27 * t322
            * t10 - 0.77616e5 * t14 * t307 * t10 + 0.44352e5 * t31 * t360 * iy - 0.30240e5 * t130 * t315
            - 0.30240e5 * t133 * t319 + 0.25200e5 * t340 * t119 - 0.77616e5 * t370 * t123 - 0.3360e4 * t91 * t327
            - 0.10080e5 * t310 * t44 - 0.3360e4 * t284 * t10 - 0.60e2 * t8 * ey + 0.129360e6 * t370 * t98;
        final double t394 = t177 * t19;
        final double t399 = t14 * t15;
        final double t403 = t9 * t79 * t81;
        final double t406 = t314 * t10;
        final double t414 = t70 * t7 * t10;
        final double t418 = t33 * ey * iy;
        final double t425 = t33 * ix;
        final double t432 = t12 * t98;
        final double t435 = t11 * t16;
        final double t440 = t8 * ix;
        final double t450 = -0.100800e6 * t145 * t394 + 0.465696e6 * t101 * t394 - 0.310464e6 * t399 * t394 + 0.40320e5
            * t126 * t403 - 0.50400e5 * t145 * t406 - 0.155232e6 * t399 * t406 + 0.232848e6 * t101 * t406
            + 0.60480e5 * t85 * t414 + 0.40320e5 * t222 * t418 - 0.10080e5 * t88 * t70 + 0.44352e5 * t37 * ix
            + 0.22176e5 * t13 * t425 * t32 + 0.40320e5 * t12 * t70 * t6 - 0.44352e5 * t432 * ix + 0.12096e5 * t435
            * ix - 0.12096e5 * t23 * ix - 0.5040e4 * t440 * t15 + 0.5040e4 * t50 * ix - 0.2100e4 * t103 * ix * t15
            + 0.2100e4 * t107 * ix;
        final double t452 = t6 * ix;
        final double t460 = t318 * iy;
        final double t487 = iy * ey * ex;
        final double t492 = t118 * t70;
        final double t504 = -0.221760e6 * t31 * t406 + 0.332640e6 * t141 * t406 - 0.80640e5 * t143 * t406 - 0.10080e5
            * t130 * t212 + 0.3360e4 * t8 * t9 * t487 + 0.25200e5 * t27 * t406 - 0.50400e5 * t50 * t492 + 0.1680e4
            * t440 * t38 - 0.3360e4 * t126 * t19 - 0.155232e6 * t14 * t492 + 0.232848e6 * t27 * t122 * t70;
        final double t512 = t70 * t6;
        final double t525 = ex * t79 * t81;
        final double t532 = t16 * ix;
        final double t535 = t98 * ix;
        final double t538 = ix * t15;
        final double t552 = t11 * t43;
        final double t566 = 0.100800e6 * t50 * t532 + 0.388080e6 * t14 * t532 - 0.388080e6 * t27 * t535 - 0.44100e5
            * t147 * t538 + 0.44100e5 * t107 * t532 + 0.10080e5 * t552 * t44 * ix + 0.25200e5 * t27 * t512
            + 0.6720e4 * t394 - 0.80640e5 * t143 * t512 - 0.66528e5 * t23 * t32 * t425 + 0.40320e5 * t211 * t38;
        final double t573 = t11 * t32;
        final double t597 = t23 * t68;
        final double t613 = -0.266112e6 * t75 * t403 - 0.399168e6 * t202 * t414 - 0.266112e6 * t597 * t418 - 0.161280e6
            * t253 * t460 - 0.77616e5 * t17 * t487 + 0.25200e5 * t28 * t487 + 0.133056e6 * t69 * t460 + 0.133056e6
            * t63 * t406 + 0.3360e4 * t512 + 0.672e3 * t208 - 0.672e3 * t538;
        final double t622 = t10 * ex * t56;
        final double t642 = t8 * iy;
        final double t645 = t44 * iy;
        final double t656 = iy * t6 * t9;
        final double t662 = t7 * iy;
        final double t672 = -0.310464e6 * t399 * t622 + 0.465696e6 * t101 * t622 - 0.100800e6 * t145 * t622 + 0.1680e4
            * t8 * t81 * t7 + 0.40320e5 * t12 * t81 * t7 + 0.44352e5 * t37 * iy + 0.12096e5 * t435 * iy - 0.2100e4
            * t103 * iy * t15 - 0.5040e4 * t642 * t15 + 0.22176e5 * t13 * t645 * t43 - 0.3360e4 * t91 * iy
            - 0.12096e5 * t23 * iy + 0.10080e5 * t552 * t645 + 0.3360e4 * t656 - 0.44352e5 * t432 * iy - 0.10080e5
            * t95 * t81 + 0.40320e5 * t23 * t662 + 0.44352e5 * t31 * t43 * t81 - 0.1680e4 * t50 * t662 - 0.3360e4
            * t126 * t56;
        final double t677 = t80 * t44;
        final double t680 = t64 * t81;
        final double t683 = t71 * t10;
        final double t691 = t81 * t7;
        final double t710 = t18 * ey;
        final double t716 = t110 * t81;
        final double t730 = -0.66528e5 * t23 * t43 * t645 + 0.1680e4 * t642 * t24 + 0.232848e6 * t27 * t114 * t81
            + 0.25200e5 * t27 * t656 + 0.80640e5 * t12 * t10 * t710 + 0.3360e4 * t8 * t10 * t710 - 0.50400e5 * t50
            * t716 - 0.155232e6 * t14 * t716 + 0.88704e5 * t13 * t44 * t18 * t79 + 0.10080e5 * t573 * t33 * iy
            + 0.25200e5 * t27 * t691;
        final double t745 = t360 * ey;
        final double t752 = iy * t15;
        final double t755 = t16 * iy;
        final double t758 = t98 * iy;
        final double t772 = t64 * iy;
        final double t775 = t80 * t10;
        final double t788 = 0.174636e6 * t104 * t755 - 0.174636e6 * t147 * t758 - 0.100800e6 * t27 * t752 + 0.100800e6
            * t50 * t755 + 0.388080e6 * t14 * t755 - 0.30240e5 * t133 * t772 - 0.30240e5 * t130 * t775 - 0.1680e4
            * t50 * t710 + 0.25200e5 * t27 * t110 * iy - 0.77616e5 * t14 * t114 * iy + 0.44352e5 * t31 * t745;
        final double t800 = t56 * t10;
        final double t833 = -0.266112e6 * t597 * t683 + 0.50400e5 * t27 * t10 * t710 - 0.672e3 * t752 + 0.3360e4 * t691
            + 0.672e3 * t11 * iy + 0.133056e6 * t63 * t772 + 0.133056e6 * t78 * t775 + 0.25200e5 * t28 * t710
            - 0.77616e5 * t17 * t710 + 0.5040e4 * t50 * iy + 0.2100e4 * t107 * iy;

        dPotJ6[0] += 0.35e2 / 0.128e3 * t5 * t155 / t158 * t167;
        dPotJ6[1] += 0.0e0;
        dPotJ6[2] += -0.5e1 / 0.128e3 * t5 * (t221 + t263) * t267 * t167 - 0.55e2 / 0.128e3 * t5 * t272 * t275 * ex;
        dPotJ6[3] += -0.5e1 / 0.128e3 * t5 * (t339 + t383) * t267 * t167 - 0.55e2 / 0.128e3 * t5 * t272 * t275 * ey;
        dPotJ6[4] += -0.5e1
            / 0.128e3
            * t5
            * (-0.3360e4 * t85 * ix - 0.66528e5 * t23 * t45 * ix - 0.77616e5 * t14 * t122 * ix + 0.25200e5 * t27
                * t118 * ix + 0.22176e5 * t13 * ix * t45 + t450 + t613 + 0.174636e6 * t104 * t532 - 0.174636e6
                * t147 * t535 - 0.100800e6 * t27 * t538 + t504 + 0.332640e6 * t141 * t512 - 0.221760e6 * t31
                * t512 + 0.665280e6 * t261 * t460 + 0.88704e5 * t13 * t33 * t68 * ey * iy + 0.88704e5 * t13
                * t9 * t525 + 0.80640e5 * t12 * t9 * t487 + 0.50400e5 * t27 * t9 * t487 + 0.1680e4 * t8 * t70
                * t6 + 0.3360e4 * t406 - 0.110880e6 * t37 * t452 - 0.1680e4 * t50 * t452 - 0.30240e5 * t136
                * t460 - 0.30240e5 * t133 * t406 + 0.40320e5 * t23 * t452 + 0.10080e5 * t573 * t425
                - 0.443520e6 * t78 * t460 + 0.44352e5 * t31 * t525 + 0.40320e5 * t23 * t487 - 0.110880e6 * t37
                * t487 - 0.1680e4 * t50 * t487 + t566 + 0.133056e6 * t13 * t70 * t6 * t7 * t10 + 0.44352e5
                * t31 * t32 * t70) * t267 * t167;
        dPotJ6[5] += -0.5e1
            / 0.128e3
            * t5
            * (-0.110880e6 * t37 * t710 + 0.232848e6 * t101 * t656 - 0.155232e6 * t399 * t656 - 0.50400e5 * t145
                * t656 - 0.161280e6 * t253 * t800 - 0.443520e6 * t78 * t800 - 0.221760e6 * t31 * t656
                + 0.133056e6 * t13 * t81 * t24 * t7 + t730 + t788 + 0.40320e5 * t23 * t710 - 0.80640e5 * t143
                * t691 + 0.6720e4 * t622 + t833 + 0.332640e6 * t141 * t656 - 0.44100e5 * t147 * t752
                + 0.44100e5 * t107 * t755 - 0.388080e6 * t27 * t758 + 0.665280e6 * t261 * t800 - 0.266112e6
                * t75 * t677 - 0.399168e6 * t202 * t680 + t672 - 0.10080e5 * t136 * t71 - 0.110880e6 * t37
                * t662 + 0.40320e5 * t126 * t677 + 0.60480e5 * t85 * t680 + 0.40320e5 * t222 * t683 - 0.80640e5
                * t143 * t656 - 0.221760e6 * t31 * t691 + 0.332640e6 * t141 * t691 + 0.88704e5 * t13 * t10
                * t745 - 0.66528e5 * t23 * t34 * iy + 0.22176e5 * t13 * iy * t34 + 0.40320e5 * t12 * iy * t24)
            * t267 * t167;

        if (this.zonalDegreeMaxPD == 6) {
            this.dPot = dPotJ6.clone();
        }
        return dPotJ6;
    }

    /**
     * <p>
     * Compute the effect of the 7th degree development of the Zonal Perturbation
     * </p>
     * 
     * @param orbit
     *        an orbit
     * @return the J7 perturbation
     */
    public double[] computeJ7(final StelaEquinoctialOrbit orbit) {

        /** Orbital elements */
        final double a = orbit.getA();
        final double ex = orbit.getEquinoctialEx();
        final double ey = orbit.getEquinoctialEy();
        final double ix = orbit.getIx();
        final double iy = orbit.getIy();
        final double MU = orbit.getMu();

        /** J7 Potential */
        final double[] dPotJ7 = this.computeJ6(orbit);

        final double t1 = MU * this.j[7];
        final double t2 = this.rEq * this.rEq;
        final double t4 = t2 * t2;
        final double t5 = t4 * t2 * this.rEq;
        final double t6 = ix * ix;
        final double t7 = iy * iy;
        final double t8 = 0.1e1 - t6 - t7;
        final double t9 = MathLib.sqrt(t8);
        final double t11 = t1 * t5 * t9;
        final double t13 = ey * ix;
        final double t14 = ex * iy - t13;
        final double t15 = t8 * t8;
        final double t16 = ex * ex;
        final double t17 = t16 * ex;
        final double t18 = t15 * t17;
        final double t19 = t6 * ix;
        final double t20 = ey * t19;
        final double t21 = t20 * iy;
        final double t24 = t15 * t16;
        final double t25 = ey * ey;
        final double t26 = t6 * t25;
        final double t27 = t26 * t7;
        final double t30 = t15 * ex;
        final double t31 = t25 * ey;
        final double t32 = ix * t31;
        final double t33 = t7 * iy;
        final double t34 = t32 * t33;
        final double t37 = t8 * ex;
        final double t38 = t13 * iy;
        final double t41 = t16 + t25;
        final double t42 = t8 * t15;
        final double t43 = t41 * t42;
        final double t44 = t6 + t7;
        final double t45 = t44 * t44;
        final double t46 = t45 * t16;
        final double t47 = t46 * t6;
        final double t50 = t15 * t41;
        final double t51 = t44 * t16;
        final double t52 = t51 * t6;
        final double t55 = t45 * t25;
        final double t56 = t55 * t7;
        final double t59 = t44 * t25;
        final double t60 = t59 * t7;
        final double t63 = t41 * t8;
        final double t64 = t63 * t44;
        final double t66 = t8 * t25;
        final double t69 = t41 * t41;
        final double t70 = t69 * t15;
        final double t73 = t8 * t16;
        final double t76 = t16 * t16;
        final double t77 = t76 * t15;
        final double t78 = t6 * t6;
        final double t81 = t25 * t25;
        final double t82 = t15 * t81;
        final double t83 = t7 * t7;
        final double t86 = t45 * t44;
        final double t89 = t50 * t45;
        final double t91 = t69 * t42;
        final double t94 = t69 * t8;
        final double t97 = t15 * t44;
        final double t98 = t97 * ex;
        final double t101 = -0.80e2 - 0.6336e4 * t18 * t21 - 0.9504e4 * t24 * t27 - 0.6336e4 * t30 * t34 - 0.4800e4
            * t37 * t38 - 0.30888e5 * t47 * t43 + 0.11088e5 * t50 * t52 - 0.30888e5 * t43 * t56 + 0.11088e5 * t50
            * t60 + 0.6000e4 * t64 - 0.2400e4 * t66 * t7 - 0.12474e5 * t70 * t45 - 0.2400e4 * t73 * t6 - 0.1584e4
            * t77 * t78 - 0.1584e4 * t82 * t83 + 0.102960e6 * t43 * t86 - 0.46200e5 * t89 + 0.28314e5 * t91 * t86
            + 0.1575e4 * t94 * t44 + 0.52800e5 * t98 * t38;
        final double t102 = t42 * t44;
        final double t103 = t102 * t17;
        final double t106 = t102 * t16;
        final double t112 = t42 * t45;
        final double t118 = t43 * t45;
        final double t119 = ex * ix;
        final double t120 = ey * iy;
        final double t121 = t119 * t120;
        final double t124 = t50 * t44;
        final double t128 = t25 * t7;
        final double t132 = t16 * t6;
        final double t137 = t81 * t83;
        final double t144 = t76 * t78;
        final double t149 = t102 * ex;
        final double t154 = t15 * t45;
        final double t156 = t8 * t44;
        final double t158 = -0.900e3 * t63 * t132 - 0.68640e5 * t112 * t132 + 0.6864e4 * t102 * t137 + 0.26400e5 * t97
            * t128 - 0.68640e5 * t112 * t128 + 0.6864e4 * t102 * t144 + 0.26400e5 * t97 * t132 + 0.27456e5 * t149
            * t34 + 0.34320e5 * t42 * t86 - 0.15840e5 * t154 + 0.2160e4 * t156;
        final double t160 = t101 + 0.27456e5 * t103 * t21 + 0.41184e5 * t106 * t27 - 0.1800e4 * t63 * ex * t38
            - 0.137280e6 * t112 * ex * t38 - 0.200e3 * t16 - 0.200e3 * t25 - 0.61776e5 * t118 * t121 + 0.22176e5
            * t124 * t121 - 0.50e2 * t69 - 0.900e3 * t63 * t128 + t158;
        final double t161 = t14 * t160;
        final double t162 = a * a;
        final double t163 = t162 * t162;
        final double t164 = t163 * t163;
        final double t167 = 0.1e1 - t16 - t25;
        final double t168 = t167 * t167;
        final double t169 = t168 * t168;
        final double t171 = MathLib.sqrt(t167);
        final double t173 = 0.1e1 / t171 / t169 / t168;
        final double t179 = 0.1e1 / t164;
        final double t180 = t179 * t173;
        final double t185 = t45 * ex;
        final double t189 = t44 * ex;
        final double t201 = ex * t42;
        final double t212 = t17 * t8;
        final double t219 = t37 * t44;
        final double t223 = t30 * t45;
        final double t225 = -0.400e3 * ex - 0.61776e5 * t43 * t185 * t6 + 0.22176e5 * t50 * t189 * t6 - 0.1800e4 * t63
            * t38 - 0.137280e6 * t112 * t38 + 0.52800e5 * t97 * t38 + 0.27456e5 * t102 * t34 - 0.61776e5 * t201
            * t56 + 0.22176e5 * t30 * t60 - 0.19008e5 * t24 * t21 - 0.19008e5 * t30 * t27 - 0.3600e4 * t73 * t38
            - 0.1800e4 * t212 * t6 - 0.4800e4 * t37 * t6 - 0.6336e4 * t18 * t78 + 0.12000e5 * t219 + 0.205920e6
            * t201 * t86 - 0.92400e5 * t223;
        final double t231 = ex * t6;
        final double t250 = t15 * ix;
        final double t251 = t31 * t33;
        final double t254 = t8 * ix;
        final double t263 = t24 * t44;
        final double t276 = 0.113256e6 * t43 * t86 * ex - 0.1800e4 * t37 * t128 - 0.137280e6 * t112 * t231 + 0.27456e5
            * t102 * t17 * t78 + 0.52800e5 * t97 * t231 - 0.61776e5 * t17 * t42 * t45 * t6 + 0.22176e5 * t18 * t44
            * t6 - 0.49896e5 * t50 * t185 - 0.1800e4 * t63 * t231 - 0.6336e4 * t250 * t251 - 0.4800e4 * t254 * t120
            + 0.6300e4 * t63 * t189 + 0.82368e5 * t106 * t21 + 0.82368e5 * t149 * t27 + 0.44352e5 * t263 * t38
            - 0.123552e6 * t16 * t42 * t45 * t38 - 0.61776e5 * t118 * t38 + 0.22176e5 * t124 * t38 - 0.200e3 * ex
            * t41;
        final double t286 = t179 / t171 / t169 / t168 / t167;
        final double t296 = ey * t15;
        final double t299 = t119 * iy;
        final double t303 = t45 * ey;
        final double t307 = t44 * ey;
        final double t317 = t17 * t19;
        final double t321 = t6 * ey;
        final double t322 = t321 * t7;
        final double t325 = ix * t25;
        final double t326 = t325 * t33;
        final double t329 = ey * t42;
        final double t332 = ey * t8;
        final double t344 = t15 * t31;
        final double t347 = 0.22176e5 * t296 * t52 - 0.3600e4 * t66 * t299 - 0.400e3 * ey - 0.61776e5 * t43 * t303 * t7
            + 0.22176e5 * t50 * t307 * t7 - 0.1800e4 * t63 * t299 - 0.137280e6 * t112 * t299 + 0.52800e5 * t97
            * t299 + 0.27456e5 * t102 * t317 * iy - 0.19008e5 * t24 * t322 - 0.19008e5 * t30 * t326 - 0.61776e5
            * t329 * t47 - 0.4800e4 * t332 * t7 - 0.1800e4 * t31 * t8 * t7 + 0.12000e5 * t332 * t44 + 0.205920e6
            * t329 * t86 - 0.92400e5 * t296 * t45 - 0.6336e4 * t344 * t83;
        final double t348 = ey * t7;
        final double t398 = -0.1800e4 * t63 * t348 - 0.6336e4 * t18 * t19 * iy - 0.4800e4 * t37 * ix * iy - 0.1800e4
            * t332 * t132 + 0.27456e5 * t102 * t31 * t83 + 0.52800e5 * t97 * t348 - 0.137280e6 * t112 * t348
            - 0.61776e5 * t31 * t42 * t45 * t7 + 0.22176e5 * t344 * t44 * t7 - 0.49896e5 * t50 * t303 + 0.113256e6
            * t43 * t86 * ey + 0.6300e4 * t63 * t307 + 0.82368e5 * t106 * t322 + 0.82368e5 * t149 * t326
            + 0.44352e5 * t15 * t25 * t44 * t299 - 0.123552e6 * t25 * t42 * t45 * t299 - 0.61776e5 * t118 * t299
            + 0.22176e5 * t124 * t299 - 0.200e3 * ey * t41;
        final double t411 = t1 * t5 / t9;
        final double t418 = t321 * iy;
        final double t421 = t325 * t7;
        final double t440 = t78 * ey * iy;
        final double t444 = t25 * t19 * t7;
        final double t448 = t6 * t31 * t33;
        final double t454 = t51 * t19;
        final double t463 = ey * iy * ex;
        final double t481 = ex * t31 * t33;
        final double t484 = -0.19008e5 * t18 * t418 - 0.19008e5 * t24 * t421 - 0.61776e5 * t43 * t46 * ix + 0.22176e5
            * t50 * t51 * ix - 0.41184e5 * t97 * t137 * ix - 0.105600e6 * t156 * t421 + 0.411840e6 * t154 * t421
            - 0.274560e6 * t102 * t421 + 0.25344e5 * t212 * t440 + 0.38016e5 * t73 * t444 + 0.25344e5 * t37 * t448
            + 0.185328e6 * t50 * t46 * t19 - 0.123552e6 * t43 * t454 - 0.44352e5 * t63 * t454 + 0.22176e5 * t50
            * t421 + 0.3600e4 * t41 * t6 * t463 + 0.105600e6 * t15 * t6 * t463 + 0.54912e5 * t42 * t78 * t17 * ey
            * iy + 0.82368e5 * t42 * t19 * t16 * t25 * t7 + 0.54912e5 * t42 * t6 * t481;
        final double t496 = t41 * ix;
        final double t499 = t78 * ix;
        final double t507 = t8 * t76;
        final double t515 = t8 * t45;
        final double t521 = t15 * t86;
        final double t532 = t231 * t120;
        final double t535 = t43 * t44;
        final double t538 = -0.6336e4 * t19 * t77 - 0.4800e4 * t73 * ix + 0.63360e5 * t515 * ix + 0.52800e5 * t19 * t15
            * t16 - 0.205920e6 * t521 * ix + 0.205920e6 * t112 * ix + 0.4800e4 * t421 - 0.3150e4 * t69 * ix * t44
            + 0.3150e4 * t94 * ix + 0.370656e6 * t89 * t532 - 0.247104e6 * t535 * t532;
        final double t543 = t16 * ix;
        final double t559 = t19 * t16;
        final double t578 = t8 * t81;
        final double t582 = t45 * ix;
        final double t585 = t44 * ix;
        final double t588 = t86 * ix;
        final double t593 = 0.13728e5 * t42 * ix * t137 + 0.52800e5 * t250 * t128 - 0.41184e5 * t97 * t76 * t499
            - 0.105600e6 * t156 * t559 + 0.9600e4 * t532 + 0.22176e5 * t50 * t559 + 0.6336e4 * t578 * t83 * ix
            + 0.49896e5 * t94 * t582 - 0.49896e5 * t70 * t585 - 0.617760e6 * t50 * t588 + 0.617760e6 * t43 * t582;
        final double t616 = t97 * t17;
        final double t637 = -0.164736e6 * t616 * t440 - 0.247104e6 * t263 * t444 - 0.164736e6 * t98 * t448 + 0.44352e5
            * t50 * t6 * t463 + 0.82368e5 * t106 * t421 + 0.82368e5 * t103 * t418 + 0.22176e5 * t124 * t463
            - 0.61776e5 * t118 * t463 + 0.4800e4 * t559 + 0.4320e4 * t254 - 0.4320e4 * t585;
        final double t652 = iy * t16 * t6;
        final double t659 = t13 * t7;
        final double t666 = t20 * t7;
        final double t669 = t26 * t33;
        final double t672 = t32 * t83;
        final double t676 = t119 * ey;
        final double t679 = t26 * iy;
        final double t682 = t32 * t7;
        final double t702 = 0.185328e6 * t89 * t652 - 0.123552e6 * t535 * t652 - 0.44352e5 * t64 * t652 + 0.823680e6
            * t223 * t659 - 0.549120e6 * t149 * t659 - 0.211200e6 * t219 * t659 - 0.164736e6 * t616 * t666
            - 0.247104e6 * t263 * t669 - 0.164736e6 * t98 * t672 + 0.44352e5 * t50 * t7 * t676 + 0.82368e5 * t106
            * t679 + 0.82368e5 * t149 * t682 + 0.22176e5 * t124 * t676 - 0.61776e5 * t118 * t676 - 0.41184e5 * t97
            * t144 * iy - 0.105600e6 * t156 * t652 + 0.25344e5 * t212 * t666 + 0.38016e5 * t73 * t669 + 0.25344e5
            * t37 * t672 + 0.22176e5 * t50 * t652;
        final double t706 = t59 * t33;
        final double t718 = t317 * ey;
        final double t755 = t83 * iy;
        final double t759 = t41 * iy;
        final double t762 = -0.19008e5 * t24 * t679 - 0.19008e5 * t30 * t682 - 0.61776e5 * t43 * t55 * iy + 0.22176e5
            * t50 * t59 * iy - 0.1800e4 * t63 * t676 - 0.137280e6 * t112 * t676 + 0.52800e5 * t97 * t676
            + 0.27456e5 * t102 * t718 + 0.1800e4 * t41 * t33 * t25 + 0.13728e5 * t42 * t755 * t81 - 0.12000e5
            * t759 * t44;
        final double t790 = t7 * ex * t13;
        final double t801 = t25 * iy;
        final double t813 = 0.3150e4 * t94 * iy - 0.247104e6 * t535 * t790 + 0.370656e6 * t89 * t790 - 0.88704e5 * t64
            * t790 - 0.6336e4 * t18 * t20 - 0.4800e4 * t37 * t13 - 0.1800e4 * t63 * t801 + 0.27456e5 * t102 * t81
            * t33 + 0.52800e5 * t97 * t801 - 0.137280e6 * t112 * t801 + 0.1800e4 * t759 * t132;
        final double t818 = t33 * t25;
        final double t837 = t45 * iy;
        final double t841 = t44 * iy;
        final double t844 = t86 * iy;
        final double t863 = -0.49896e5 * t70 * t841 - 0.617760e6 * t50 * t844 + 0.617760e6 * t43 * t837 + 0.184800e6
            * t63 * t837 - 0.184800e6 * t50 * t841 - 0.169884e6 * t70 * t844 + 0.169884e6 * t91 * t837 - 0.4320e4
            * t841 + 0.4800e4 * t818 + 0.4320e4 * t8 * iy - 0.4800e4 * t66 * iy;

        dPotJ7[0] += -0.21e2 / 0.16e2 * t11 * t161 / t164 / a * t173;
        dPotJ7[1] += 0.0e0;
        dPotJ7[2] += 0.21e2 / 0.128e3 * t11 * iy * t160 * t180 + 0.21e2 / 0.128e3 * t11 * t14 * (t225 + t276) * t180
            + 0.273e3 / 0.128e3 * t11 * t161 * t286 * ex;
        dPotJ7[3] += -0.21e2 / 0.128e3 * t11 * ix * t160 * t180 + 0.21e2 / 0.128e3 * t11 * t14 * (t347 + t398) * t180
            + 0.273e3 / 0.128e3 * t11 * t161 * t286 * ey;
        dPotJ7[4] += -0.21e2
            / 0.128e3
            * t411
            * t161
            * t180
            * ix
            - 0.21e2
            / 0.128e3
            * t11
            * ey
            * t160
            * t180
            + 0.21e2
            / 0.128e3
            * t11
            * t14
            * (t538 + t593 + t484 + 0.12000e5 * t63 * ix - 0.63360e5 * t97 * ix + 0.1800e4 * t41 * t19 * t16
                + 0.184800e6 * t63 * t582 - 0.184800e6 * t50 * t585 - 0.169884e6 * t70 * t588 + 0.169884e6
                * t91 * t582 + 0.185328e6 * t89 * t421 - 0.123552e6 * t535 * t421 - 0.44352e5 * t64 * t421
                + 0.823680e6 * t223 * t418 - 0.549120e6 * t149 * t418 - 0.211200e6 * t219 * t418 - 0.1800e4
                * t63 * t463 - 0.137280e6 * t112 * t463 + 0.52800e5 * t97 * t463 + 0.27456e5 * t102 * t481
                - 0.12000e5 * t496 * t44 + 0.6336e4 * t507 * t499 - 0.88704e5 * t64 * t532 - 0.1800e4 * t63
                * t543 - 0.137280e6 * t112 * t543 + 0.52800e5 * t97 * t543 - 0.6336e4 * t30 * t251 - 0.4800e4
                * t37 * t120 + 0.1800e4 * t496 * t128 + 0.411840e6 * t154 * t559 - 0.274560e6 * t102 * t559
                + 0.13728e5 * t42 * t499 * t76 + 0.27456e5 * t102 * t76 * t19 + t637) * t180;
        dPotJ7[5] += -0.21e2
            / 0.128e3
            * t411
            * t161
            * t180
            * iy
            + 0.21e2
            / 0.128e3
            * t11
            * ex
            * t160
            * t180
            + 0.21e2
            / 0.128e3
            * t11
            * t14
            * (t813 + 0.63360e5 * t515 * iy - 0.205920e6 * t521 * iy - 0.63360e5 * t97 * iy + 0.205920e6 * t112
                * iy + 0.12000e5 * t63 * iy - 0.6336e4 * t82 * t33 + 0.6336e4 * t578 * t755 - 0.123552e6 * t43
                * t706 - 0.44352e5 * t63 * t706 + 0.411840e6 * t154 * t652 - 0.274560e6 * t102 * t652
                + 0.13728e5 * t42 * iy * t144 + 0.6336e4 * t507 * t78 * iy - 0.3150e4 * t69 * iy * t44
                + 0.52800e5 * t15 * iy * t132 + 0.9600e4 * t790 + 0.52800e5 * t15 * t33 * t25 + 0.105600e6
                * t15 * t7 * t676 + 0.54912e5 * t42 * t7 * t718 - 0.41184e5 * t97 * t81 * t755 + t762 + t702
                + 0.4800e4 * t652 + 0.82368e5 * t42 * t33 * t132 * t25 + 0.54912e5 * t42 * t83 * t119 * t31
                - 0.105600e6 * t156 * t818 + 0.411840e6 * t154 * t818 - 0.274560e6 * t102 * t818 + 0.22176e5
                * t50 * t818 + 0.49896e5 * t94 * t837 + t863 + 0.3600e4 * t41 * t7 * t676 + 0.185328e6 * t50
                * t55 * t33) * t180;

        if (this.zonalDegreeMaxPD == 7) {
            this.dPot = dPotJ7.clone();
        }
        return dPotJ7;
    }

    /**
     * <p>
     * Compute the effect of the 8th degree development of the Zonal Perturbation
     * </p>
     * 
     * @param orbit
     *        an orbit
     * @return the J8 perturbation
     */
    public double[] computeJ8(final StelaEquinoctialOrbit orbit) {
        // Potential up to J7
        final double[] dPot = this.computeJ7(orbit);
        // J8 potential
        final double[] dPot8 = new StelaZonalAttractionJ8(this.rEq, this.j[8]).computePerturbation(orbit);
        // Add both contribution
        for (int i = 0; i < dPot8.length; i++) {
            dPot[i] += dPot8[i];
        }
        return dPot;
    }

    /**
     * <p>
     * Compute the effect of the 9th degree development of the Zonal Perturbation
     * </p>
     * 
     * @param orbit
     *        an orbit
     * @return the J9 perturbation
     */
    public double[] computeJ9(final StelaEquinoctialOrbit orbit) {
        // Potential up to J8
        final double[] dPot = this.computeJ8(orbit);
        // J9 potential
        final double[] dPot9 = new StelaZonalAttractionJ9(this.rEq, this.j[9]).computePerturbation(orbit);
        // Add both contribution
        for (int i = 0; i < dPot9.length; i++) {
            dPot[i] += dPot9[i];
        }
        return dPot;
    }

    /**
     * <p>
     * Compute the effect of the 10th degree development of the Zonal Perturbation
     * </p>
     * 
     * @param orbit
     *        an orbit
     * @return the J10 perturbation
     */
    public double[] computeJ10(final StelaEquinoctialOrbit orbit) {
        // Potential up to J9
        final double[] dPot = this.computeJ9(orbit);
        // J10 potential
        final double[] dPot10 = new StelaZonalAttractionJ10(this.rEq, this.j[10]).computePerturbation(orbit);
        // Add both contribution
        for (int i = 0; i < dPot10.length; i++) {
            dPot[i] += dPot10[i];
        }
        return dPot;
    }

    /**
     * <p>
     * Compute the effect of the 11th degree development of the Zonal Perturbation
     * </p>
     * 
     * @param orbit
     *        an orbit
     * @return the J11 perturbation
     */
    public double[] computeJ11(final StelaEquinoctialOrbit orbit) {
        // Potential up to J10
        final double[] dPot = this.computeJ10(orbit);
        // J11 potential
        final double[] dPot11 = new StelaZonalAttractionJ11(this.rEq, this.j[11]).computePerturbation(orbit);
        // Add both contribution
        for (int i = 0; i < dPot11.length; i++) {
            dPot[i] += dPot11[i];
        }
        return dPot;
    }

    /**
     * <p>
     * Compute the effect of the 12th degree development of the Zonal Perturbation
     * </p>
     * 
     * @param orbit
     *        an orbit
     * @return the J12 perturbation
     */
    public double[] computeJ12(final StelaEquinoctialOrbit orbit) {
        // Potential up to J11
        final double[] dPot = this.computeJ11(orbit);
        // J12 potential
        final double[] dPot12 = new StelaZonalAttractionJ12(this.rEq, this.j[12]).computePerturbation(orbit);
        // Add both contribution
        for (int i = 0; i < dPot12.length; i++) {
            dPot[i] += dPot12[i];
        }
        return dPot;
    }

    /**
     * <p>
     * Compute the effect of the 13th degree development of the Zonal Perturbation
     * </p>
     * 
     * @param orbit
     *        an orbit
     * @return the J13 perturbation
     */
    public double[] computeJ13(final StelaEquinoctialOrbit orbit) {
        // Potential up to J12
        final double[] dPot = this.computeJ12(orbit);
        // J13 potential
        final double[] dPot13 = new StelaZonalAttractionJ13(this.rEq, this.j[13]).computePerturbation(orbit);
        // Add both contribution
        for (int i = 0; i < dPot13.length; i++) {
            dPot[i] += dPot13[i];
        }
        return dPot;
    }

    /**
     * <p>
     * Compute the effect of the 14th degree development of the Zonal Perturbation
     * </p>
     * 
     * @param orbit
     *        an orbit
     * @return the J14 perturbation
     */
    public double[] computeJ14(final StelaEquinoctialOrbit orbit) {
        // Potential up to J13
        final double[] dPot = this.computeJ13(orbit);
        // J14 potential
        final double[] dPot14 = new StelaZonalAttractionJ14(this.rEq, this.j[14]).computePerturbation(orbit);
        // Add both contribution
        for (int i = 0; i < dPot14.length; i++) {
            dPot[i] += dPot14[i];
        }
        return dPot;
    }

    /**
     * <p>
     * Compute the effect of the 15th degree development of the Zonal Perturbation
     * </p>
     * 
     * @param orbit
     *        an orbit
     * @return the J15 perturbation
     */
    public double[] computeJ15(final StelaEquinoctialOrbit orbit) {
        // Potential up to J14
        final double[] dPot = this.computeJ14(orbit);
        // J15 potential
        final double[] dPot15 = new StelaZonalAttractionJ15(this.rEq, this.j[15]).computePerturbation(orbit);
        // Add both contribution
        for (int i = 0; i < dPot15.length; i++) {
            dPot[i] += dPot15[i];
        }
        return dPot;
    }

    /**
     * <p>
     * Compute the effect of the J2² of the Zonal Perturbation.
     * </p>
     * 
     * @param orbit
     *        an orbit
     * @return the J2² perturbation
     */
    public double[] computeJ2Square(final StelaEquinoctialOrbit orbit) {

        /** Orbital elements */
        final double a = orbit.getA();
        final double ex = orbit.getEquinoctialEx();
        final double ey = orbit.getEquinoctialEy();
        final double ix = orbit.getIx();
        final double iy = orbit.getIy();
        final double MU = orbit.getMu();

        /** J2² Potential */
        final double J22 = this.j[2] * this.j[2];
        final double[] dPotJ2Sq = new double[6];
        final double eta = MathLib.sqrt(1 - (ex * ex + ey * ey));
        final double Ci = 1 - 2 * (ix * ix + iy * iy);

        final double t1 = a * a;
        final double t2 = t1 * t1;
        final double t7 = MathLib.sqrt(MU / a);
        final double t9 = this.rEq * this.rEq;
        final double t10 = t9 * t9;
        final double t14 = eta * eta;
        final double t17 = t14 * t14;
        final double t18 = t17 * t17;
        final double t19 = 0.1e1 / t18;
        final double t21 = Ci * Ci;
        final double t22 = t21 * t21;
        final double t30 = 0.25e2 * t14;
        final double t50 = 0.1e1 / t17 / t14 / eta;

        final double Fact = 0.1e1 / t2 / a * t7 * t10 / 0.4e1;
        final double DW = 0.3e1 / 0.32e2 * t22 * t19 * (0.360e3 * eta + 0.45e2 * t14 + 0.385e3) + 0.3e1 / 0.32e2 * t21
            * t19
            * (0.90e2 - 0.126e3 * t14 - 0.192e3 * eta) + 0.3e1 / 0.32e2 * t19 * (0.24e2 * eta + t30 - 0.35e2);
        final double DOM = -0.3e1 / 0.8e1 * t21 * Ci * t19 * (0.5e1 * t14 + 0.36e2 * eta + 0.35e2) - 0.3e1 / 0.8e1 * Ci
            * t19
            * (-0.9e1 * t14 - 0.12e2 * eta + 0.5e1);
        final double DM = 0.3e1 / 0.32e2 * t22 * t50 * (0.144e3 * eta + t30 + 0.105e3) + 0.3e1 / 0.32e2 * t21 * t50
            * (0.30e2 - 0.90e2 * t14 - 0.96e2 * eta) + 0.3e1 / 0.32e2 * t50 * (0.16e2 * eta + t30 - 0.15e2);
        final double DWb = DW + DOM;

        dPotJ2Sq[0] = 0;
        dPotJ2Sq[1] = J22 * Fact * (DWb + DM);
        dPotJ2Sq[2] = -ey * J22 * Fact * DWb;
        dPotJ2Sq[3] = J22 * ex * Fact * DWb;
        dPotJ2Sq[4] = -iy * J22 * Fact * DOM;
        dPotJ2Sq[5] = J22 * ix * Fact * DOM;

        return dPotJ2Sq;
    }

    /**
     * Compute the short periods linked to J2 Potential effect
     * <p>
     * Compute the effect of the short periods generated by the 2nd degree development of the Zonal Perturbation
     * </p>
     * 
     * @param orbit
     *        the equinoctial orbit
     * 
     * @return shortPeriodsJ2 the J2 short periods
     */
    public double[] computeJ2ShortPeriods(final StelaEquinoctialOrbit orbit) {

        /** Orbital elements */
        final double a = orbit.getA();
        final double ex = orbit.getEquinoctialEx();
        final double ey = orbit.getEquinoctialEy();
        final double ix = orbit.getIx();
        final double iy = orbit.getIy();
        final double ksi = orbit.getLM();
        final double MU = orbit.getMu();

        // Get some T2 and T8 parameters
        final double nbar = MathLib.sqrt(MU / (a * a * a));
        final double e = MathLib.sqrt(ex * ex + ey * ey);
        final double pomPlusRaan = JavaMathAdapter.mod(MathLib.atan2(ey, ex), 2 * FastMath.PI);
        final double meanAnomaly = JavaMathAdapter.mod(ksi - pomPlusRaan, 2 * FastMath.PI);
        final double u = orbit.kepEq(e, meanAnomaly);
        final double eta = MathLib.sqrt(1 - (e * e));
        final double[] sincos = MathLib.sinAndCos(u);
        final double sinU = sincos[0];
        final double cosU = sincos[1];
        final double denom = 1. - e * cosU;
        final double nu = JavaMathAdapter.mod(MathLib.atan2(eta * sinU / denom, (cosU - e) / denom), 2 * FastMath.PI);
        final double f = JavaMathAdapter.mod(pomPlusRaan + u, 2 * FastMath.PI);

        /** J2 Potential */
        final double[] shortPeriodsJ2 = new double[6];

        // Compute derivatives
        final double t1 = MU * this.j[2];
        final double t2 = this.rEq * this.rEq;
        final double t3 = t1 * t2;
        final double t4 = a * a;
        final double t5 = t4 * t4;
        final double t7 = ex * ex;
        final double t8 = ey * ey;
        final double t9 = 0.1e1 - t7 - t8;
        final double t10 = MathLib.sqrt(t9);
        final double t12 = 0.1e1 / t10 / t9;
        final double t14 = 0.1e1 / nbar;
        final double t15 = ix * ix;
        final double t16 = iy * iy;
        final double t17 = t15 + t16;
        final double t18 = 0.1e1 - t15 - t16;
        final double t21 = 0.1e1 / 0.2e1 - 0.3e1 * t17 * t18;
        final double[] sincosf = MathLib.sinAndCos(f);
        final double t22 = sincosf[1];
        final double t23 = ex * t22;
        final double t24 = sincosf[0];
        final double t25 = ey * t24;
        final double t26 = 0.1e1 - t23 - t25;
        final double t27 = 0.1e1 / t26;
        final double t28 = t10 * t27;
        final double t29 = ex * t24;
        final double t30 = ey * t22;
        final double t31 = t29 - t30;
        final double t33 = nu - u + t28 * t31 + t29 - t30;
        final double t35 = t26 * t26;
        final double t36 = 0.1e1 / t35;
        final double t37 = t22 - ex;
        final double t39 = t24 - ey;
        final double t41 = 0.1e1 + t10;
        final double t42 = 0.1e1 / t41;
        final double t45 = ey * ix - ex * iy;
        final double t46 = t42 * t45;
        final double t48 = ix * t37 + iy * t39 + t46 * t31;
        final double t49 = t36 * t48;
        final double t54 = ex * ix + ey * iy;
        final double t55 = t42 * t54;
        final double t57 = -iy * t37 + ix * t39 - t55 * t31;
        final double t58 = t23 + t25 - t7 - t8;
        final double t61 = 0.4e1 * t27 * t58 + 0.3e1;
        final double t62 = t57 * t61;
        final double t64 = t48 * t48;
        final double t66 = t57 * t57;
        final double t68 = t36 * t64 - t36 * t66;
        final double t69 = t68 * t10;
        final double t70 = t27 * t31;
        final double t72 = t49 * t62 - t69 * t70;
        final double t74 = -t21 * t33 - t18 * t72;
        final double t80 = 0.1e1 / t4 / a;
        final double t82 = t1 * t2 * t80;
        final double t84 = t23 + t25;
        final double t85 = 0.1e1 + t10 - t23 - t25;
        final double t86 = 0.1e1 / t85;
        final double t88 = t31 * t31;
        final double t89 = t85 * t85;
        final double t90 = 0.1e1 / t89;
        final double t91 = t88 * t90;
        final double t94 = 0.1e1 / (0.1e1 + t91);
        final double t97 = t10 * t36;
        final double t103 = 0.1e1 / t35 / t26;
        final double t104 = t103 * t48;
        final double t108 = ix * t24;
        final double t111 = -t108 + iy * t22 + t46 * t84;
        final double t114 = iy * t24;
        final double t117 = t114 + ix * t22 - t55 * t84;
        final double t120 = t36 * t58;
        final double t126 = t103 * t64;
        final double t129 = t103 * t66;
        final double t131 = t36 * t57;
        final double t146 = t9 * t9;
        final double t149 = 0.1e1 / t10 / t146 * t14;
        final double t154 = t80 * t12;
        final double t155 = t22 * t27;
        final double t156 = t29 * t155;
        final double t157 = t24 * t24;
        final double t159 = ey * t157 * t27;
        final double t160 = t24 + t156 + t159;
        final double t162 = t31 * t90;
        final double t163 = 0.1e1 / t10;
        final double t166 = ex * t157 * t27;
        final double t167 = t25 * t155;
        final double t173 = t163 * t27;
        final double t176 = -t22 + t166 - t167;
        final double t186 = -t157 * t27 - 0.1e1;
        final double t188 = t114 * t155;
        final double t189 = t41 * t41;
        final double t190 = 0.1e1 / t189;
        final double t191 = t190 * t45;
        final double t192 = t31 * t163;
        final double t193 = t192 * ex;
        final double t196 = t42 * iy * t31;
        final double t198 = ix * t186 + t188 + t191 * t193 - t196 + t46 * t160;
        final double t202 = t108 * t155;
        final double t203 = t190 * t54;
        final double t206 = t42 * ix * t31;
        final double t208 = -iy * t186 + t202 - t203 * t193 - t206 - t55 * t160;
        final double t225 = t68 * t163;
        final double t228 = t36 * t31;
        final double t244 = t22 * t22;
        final double t246 = ex * t244 * t27;
        final double t247 = -t246 - t22 - t167;
        final double t251 = ey * t244 * t27;
        final double t259 = -t156 - t24 + t251;
        final double t269 = -t244 * t27 - 0.1e1;
        final double t271 = t192 * ey;
        final double t274 = t202 + iy * t269 + t191 * t271 + t206 + t46 * t247;
        final double t280 = -t188 + ix * t269 - t203 * t271 - t196 - t55 * t247;
        final double t318 = t22 - ex + t42 * ey * t31;
        final double t323 = t24 - ey - t42 * ex * t31;

        shortPeriodsJ2[0] = -0.3e1 * t3 / t5 * t12 * t14 * t74;
        shortPeriodsJ2[1] = t82
            * t12
            * t14
            * (-t21 * (0.2e1 * (t84 * t86 - t91) * t94 - t97 * t88 + t28 * t84 + t23 + t25) - t18
                * (-0.2e1 * t104 * t62 * t31 + t36 * t111 * t62 + t49 * t117 * t61 + 0.4e1 * t49 * t57
                    * (-t120 * t31 - t27 * t31) - 0.2e1
                    * (-t126 * t31 + t49 * t111 + t129 * t31 - t131 * t117) * t10 * t70 + t69 * t36 * t88 - t69
                    * t27 * t84)) * t27;
        shortPeriodsJ2[2] = 0.3e1
            * t82
            * t149
            * t74
            * ex
            + t3
            * t154
            * t14
            * (-t21
                * (0.2e1 * (t160 * t86 - t162 * (-t163 * ex - t22 + t166 - t167)) * t94 - t173 * t31 * ex - t97
                    * t31 * t176 + t28 * t160 + t24 + t156 + t159) - t18
                * (-0.2e1 * t104 * t62 * t176 + t36 * t198 * t62 + t49 * t208 * t61 + 0.4e1 * t49 * t57
                    * (-t120 * t176 + t27 * (t22 - t166 + t167 - 0.2e1 * ex)) - 0.2e1
                    * (-t126 * t176 + t49 * t198 + t129 * t176 - t131 * t208) * t10 * t70 + t225 * t70 * ex
                    + t69 * t228 * t176 - t69 * t27 * t160));
        shortPeriodsJ2[3] = 0.3e1
            * t82
            * t149
            * t74
            * ey
            + t3
            * t154
            * t14
            * (-t21
                * (0.2e1 * (t247 * t86 - t162 * (-t163 * ey - t156 - t24 + t251)) * t94 - t173 * t31 * ey - t97
                    * t31 * t259 + t28 * t247 - t246 - t22 - t167) - t18
                * (-0.2e1 * t104 * t62 * t259 + t36 * t274 * t62 + t49 * t280 * t61 + 0.4e1 * t49 * t57
                    * (-t120 * t259 + t27 * (t156 + t24 - t251 - 0.2e1 * ey)) - 0.2e1
                    * (-t126 * t259 + t49 * t274 + t129 * t259 - t131 * t280) * t10 * t70 + t225 * t70 * ey
                    + t69 * t228 * t259 - t69 * t27 * t247));
        shortPeriodsJ2[4] = t3
            * t154
            * t14
            * (-0.6e1 * (-ix * t18 + t17 * ix) * t33 + 0.2e1 * ix * t72 - t18
                * (t36 * t318 * t62 + t49 * t323 * t61 - 0.2e1 * (t49 * t318 - t131 * t323) * t10 * t70));
        shortPeriodsJ2[5] = t3
            * t154
            * t14
            * (-0.6e1 * (-iy * t18 + t17 * iy) * t33 + 0.2e1 * iy * t72 - t18
                * (t36 * t323 * t62 - t49 * t318 * t61 - 0.2e1 * (t49 * t323 + t131 * t318) * t10 * t70));

        return shortPeriodsJ2;

    }

    /**
     * Compute the partial derivatives due to J2 Potential effect
     * <p>
     * Compute the effect of the partial derivatives due to the 2nd degree development of the Zonal Perturbation
     * </p>
     * 
     * @param orbit
     *        the equinoctial orbit
     * 
     * @return partialDerivativesJ2 the J2 partial derivatives
     */
    public double[][] computeJ2PartialDerivatives(final StelaEquinoctialOrbit orbit) {

        /** Orbital elements */
        final double a = orbit.getA();
        final double ex = orbit.getEquinoctialEx();
        final double ey = orbit.getEquinoctialEy();
        final double ix = orbit.getIx();
        final double iy = orbit.getIy();
        final double MU = orbit.getMu();

        /** J2 Potential */
        final double[][] partialDerivativesJ2 = new double[6][6];

        final double t1 = MU * this.j[2];
        final double t2 = this.rEq * this.rEq;
        final double t3 = t1 * t2;
        final double t4 = ix * ix;
        final double t5 = iy * iy;
        final double t6 = 0.1e1 - t4 - t5;
        final double t7 = t4 + t5;
        final double t10 = -0.1e1 + 0.6e1 * t6 * t7;
        final double t11 = a * a;
        final double t12 = t11 * t11;
        final double t16 = ex * ex;
        final double t17 = ey * ey;
        final double t18 = 0.1e1 - t16 - t17;
        final double t19 = MathLib.sqrt(t18);
        final double t21 = 0.1e1 / t19 / t18;
        final double t25 = 0.1e1 / t12;
        final double t26 = t10 * t25;
        final double t27 = t18 * t18;
        final double t29 = 0.1e1 / t19 / t27;
        final double t30 = t29 * ex;
        final double t33 = 0.9e1 / 0.2e1 * t3 * t26 * t30;
        final double t34 = t29 * ey;
        final double t37 = 0.9e1 / 0.2e1 * t3 * t26 * t34;
        final double t40 = -ix * t7 + t6 * ix;
        final double t44 = 0.18e2 * t3 * t40 * t25 * t21;
        final double t47 = -t7 * iy + iy * t6;
        final double t51 = 0.18e2 * t3 * t47 * t25 * t21;
        final double t53 = 0.1e1 / t11 / a;
        final double t54 = t10 * t53;
        final double t57 = 0.1e1 / t19 / t27 / t18;
        final double t64 = 0.3e1 / 0.2e1 * t3 * t54 * t29;
        final double t72 = 0.15e2 / 0.2e1 * t1 * t2 * t10 * t53 * t57 * ex * ey;
        final double t73 = 0.12e2 * t40 * t53;
        final double t76 = 0.3e1 / 0.2e1 * t3 * t73 * t30;
        final double t77 = 0.12e2 * t47 * t53;
        final double t80 = 0.3e1 / 0.2e1 * t3 * t77 * t30;
        final double t88 = 0.3e1 / 0.2e1 * t3 * t73 * t34;
        final double t91 = 0.3e1 / 0.2e1 * t3 * t77 * t34;
        final double t103 = 0.24e2 * t3 * ix * iy * t53 * t21;

        partialDerivativesJ2[0][0] = -0.6e1 * t3 / t12 / a * t10 * t21;
        partialDerivativesJ2[0][2] = t33;
        partialDerivativesJ2[0][3] = t37;
        partialDerivativesJ2[0][4] = t44;
        partialDerivativesJ2[0][5] = t51;
        partialDerivativesJ2[2][0] = t33;
        partialDerivativesJ2[2][2] = -0.15e2 / 0.2e1 * t3 * t54 * t57 * t16 - t64;
        partialDerivativesJ2[2][3] = -t72;
        partialDerivativesJ2[2][4] = -t76;
        partialDerivativesJ2[2][5] = -t80;
        partialDerivativesJ2[3][0] = t37;
        partialDerivativesJ2[3][2] = -t72;
        partialDerivativesJ2[3][3] = -0.15e2 / 0.2e1 * t3 * t54 * t57 * t17 - t64;
        partialDerivativesJ2[3][4] = -t88;
        partialDerivativesJ2[3][5] = -t91;
        partialDerivativesJ2[4][0] = t44;
        partialDerivativesJ2[4][2] = -t76;
        partialDerivativesJ2[4][3] = -t88;
        partialDerivativesJ2[4][4] = -t3 * (-0.72e2 * t4 - 0.24e2 * t5 + 0.12e2) * t53 * t21 / 0.2e1;
        partialDerivativesJ2[4][5] = t103;
        partialDerivativesJ2[5][0] = t51;
        partialDerivativesJ2[5][2] = -t80;
        partialDerivativesJ2[5][3] = -t91;
        partialDerivativesJ2[5][4] = t103;
        partialDerivativesJ2[5][5] = -t3 * (-0.24e2 * t4 - 0.72e2 * t5 + 0.12e2) * t53 * t21 / 0.2e1;

        return partialDerivativesJ2;

    }

    /**
     * Compute the partial derivatives due to J3 Potential effect
     * <p>
     * Compute the effect of the partial derivatives due to the 3rd degree development of the Zonal Perturbation
     * </p>
     * 
     * @param orbit
     *        the equinoctial orbit
     * 
     * @return partialDerivativesJ3 the J3 partial derivatives
     */
    public double[][] computeJ3PartialDerivatives(final StelaEquinoctialOrbit orbit) {

        /** Orbital elements */
        final double a = orbit.getA();
        final double ex = orbit.getEquinoctialEx();
        final double ey = orbit.getEquinoctialEy();
        final double ix = orbit.getIx();
        final double iy = orbit.getIy();
        final double MU = orbit.getMu();

        /** J2 Potential */
        final double[][] partialDerivativesJ3 = this.computeJ2PartialDerivatives(orbit);

        final double t1 = MU * this.j[3];
        final double t2 = this.rEq * this.rEq;
        final double t3 = t2 * this.rEq;
        final double t4 = ix * ix;
        final double t5 = iy * iy;
        final double t6 = 0.1e1 - t4 - t5;
        final double t7 = MathLib.sqrt(t6);
        final double t8 = t3 * t7;
        final double t9 = t1 * t8;
        final double t10 = t4 + t5;
        final double t13 = 0.5e1 * t6 * t10 - 0.1e1;
        final double t16 = -ix * ey + iy * ex;
        final double t17 = t13 * t16;
        final double t18 = a * a;
        final double t19 = t18 * t18;
        final double t22 = ex * ex;
        final double t23 = ey * ey;
        final double t24 = 0.1e1 - t22 - t23;
        final double t25 = t24 * t24;
        final double t26 = MathLib.sqrt(t24);
        final double t28 = 0.1e1 / t26 / t25;
        final double t33 = t13 * iy;
        final double t35 = 0.1e1 / t19 / a;
        final double t36 = t35 * t28;
        final double t42 = 0.1e1 / t26 / t25 / t24;
        final double t43 = t35 * t42;
        final double t48 = -0.12e2 * t9 * t33 * t36 - 0.60e2 * t9 * t17 * t43 * ex;
        final double t49 = t13 * ix;
        final double t57 = 0.12e2 * t9 * t49 * t36 - 0.60e2 * t9 * t17 * t43 * ey;
        final double t59 = t3 / t7;
        final double t60 = t1 * t59;
        final double t66 = -ix * t10 + t6 * ix;
        final double t67 = 0.10e2 * t66 * t16;
        final double t70 = t13 * ey;
        final double t73 = t60 * t17 * t36 * ix - t9 * t67 * t36 + t9 * t70 * t36;
        final double t79 = -t10 * iy + iy * t6;
        final double t80 = 0.10e2 * t79 * t16;
        final double t83 = t13 * ex;
        final double t86 = t60 * t17 * t36 * iy - t9 * t80 * t36 - t9 * t83 * t36;
        final double t87 = 0.1e1 / t19;
        final double t88 = t87 * t42;
        final double t89 = t88 * ex;
        final double t93 = t25 * t25;
        final double t95 = 0.1e1 / t26 / t93;
        final double t96 = t87 * t95;
        final double t103 = 0.15e2 * t9 * t17 * t88;
        final double t105 = t88 * ey;
        final double t114 = t16 * t87;
        final double t120 = 0.15e2 * t9 * t33 * t105 - 0.15e2 * t9 * t49 * t89 + 0.105e3 * t1 * t8 * t13 * t114 * t95
            * ex * ey;
        final double t121 = t87 * t28;
        final double t122 = t121 * ix;
        final double t125 = 0.3e1 * t60 * t33 * t122;
        final double t131 = t1 * t59 * t13;
        final double t132 = t42 * ex;
        final double t142 = 0.15e2 * t9 * t70 * t89;
        final double t143 = -t125 + 0.30e2 * t9 * t66 * iy * t121 - 0.15e2 * t131 * t114 * t132 * ix + 0.15e2 * t9
            * t67 * t89 - t142;
        final double t156 = 0.3e1 * t1 * t3 * t7 * t13 * t121;
        final double t168 = -0.3e1 * t60 * t13 * t5 * t121 + 0.30e2 * t9 * t79 * iy * t121 + t156 - 0.15e2 * t131
            * t114 * t132 * iy + 0.15e2 * t9 * t80
            * t89 + 0.15e2 * t9 * t13 * t22 * t88;
        final double t185 = t42 * ey;
        final double t197 = 0.3e1 * t60 * t13 * t4 * t121 - 0.30e2 * t9 * t66 * ix * t121 - t156 - 0.15e2 * t131 * t114
            * t185 * ix + 0.15e2 * t9 * t67
            * t105 - 0.15e2 * t9 * t13 * t23 * t88;
        final double t209 = t125 - 0.30e2 * t9 * t79 * ix * t121 - 0.15e2 * t131 * t114 * t185 * iy + 0.15e2 * t9 * t80
            * t105 + t142;
        final double t212 = t3 / t6 / t7;
        final double t213 = t1 * t212;
        final double t226 = 0.3e1 * t60 * t17 * t121;
        final double t243 = t114 * t28 * ix * iy;
        final double t252 = t121 * iy;
        final double t269 = -0.3e1 * t1 * t212 * t13 * t243 - 0.3e1 * t60 * t80 * t122 - 0.3e1 * t60 * t83 * t122
            - 0.3e1 * t60 * t67 * t252 - 0.120e3
            * t9 * t243 + 0.30e2 * t9 * t66 * ex * t121 + 0.3e1 * t60 * t70 * t252 - 0.30e2 * t9 * t79 * ey * t121;

        partialDerivativesJ3[0][0] += 0.60e2 * t9 * t17 * t28 / t19 / t18;
        partialDerivativesJ3[0][2] += t48;
        partialDerivativesJ3[0][3] += t57;
        partialDerivativesJ3[0][4] += 0.12e2 * t73;
        partialDerivativesJ3[0][5] += 0.12e2 * t86;
        partialDerivativesJ3[2][0] += t48;
        partialDerivativesJ3[2][2] += 0.30e2 * t9 * t33 * t89 + 0.105e3 * t9 * t17 * t96 * t22 + t103;
        partialDerivativesJ3[2][3] += t120;
        partialDerivativesJ3[2][4] += t143;
        partialDerivativesJ3[2][5] += t168;
        partialDerivativesJ3[3][0] += t57;
        partialDerivativesJ3[3][2] += t120;
        partialDerivativesJ3[3][3] += -0.30e2 * t9 * t49 * t105 + 0.105e3 * t9 * t17 * t96 * t23 + t103;
        partialDerivativesJ3[3][4] += t197;
        partialDerivativesJ3[3][5] += t209;
        partialDerivativesJ3[4][0] += 0.12e2 * t73;
        partialDerivativesJ3[4][2] += t143;
        partialDerivativesJ3[4][3] += t197;
        partialDerivativesJ3[4][4] += -0.3e1 * t213 * t17 * t121 * t4 - 0.6e1 * t60 * t67 * t122 + 0.6e1 * t60 * t70
            * t122 - t226 + 0.3e1 * t9
            * (-0.60e2 * t4 - 0.20e2 * t5 + 0.10e2) * t16 * t121 - 0.60e2 * t9 * t66 * ey * t121;
        partialDerivativesJ3[4][5] += t269;
        partialDerivativesJ3[5][0] += 0.12e2 * t86;
        partialDerivativesJ3[5][2] += t168;
        partialDerivativesJ3[5][3] += t209;
        partialDerivativesJ3[5][4] += t269;
        partialDerivativesJ3[5][5] += -0.3e1 * t213 * t17 * t121 * t5 - 0.6e1 * t60 * t80 * t252 - 0.6e1 * t60 * t83
            * t252 - t226 + 0.3e1 * t9
            * (-0.20e2 * t4 - 0.60e2 * t5 + 0.10e2) * t16 * t121 + 0.60e2 * t9 * t79 * ex * t121;

        return partialDerivativesJ3;
    }

    /**
     * Compute the partial derivatives due to J4 Potential effect
     * <p>
     * Compute the effect of the partial derivatives due to the 4th degree development of the Zonal Perturbation
     * </p>
     * 
     * @param orbit
     *        the equinoctial orbit
     * 
     * @return partialDerivativesJ4 the J4 partial derivatives
     */
    public double[][] computeJ4PartialDerivatives(final StelaEquinoctialOrbit orbit) {

        /** Orbital elements */
        final double a = orbit.getA();
        final double ex = orbit.getEquinoctialEx();
        final double ey = orbit.getEquinoctialEy();
        final double ix = orbit.getIx();
        final double iy = orbit.getIy();
        final double MU = orbit.getMu();

        /** J2 Potential */
        final double[][] partialDerivativesJ4 = this.computeJ3PartialDerivatives(orbit);

        final double t1 = MU * this.j[4];
        final double t2 = this.rEq * this.rEq;
        final double t3 = t2 * t2;
        final double t4 = t1 * t3;
        final double t5 = ex * ex;
        final double t6 = ey * ey;
        final double t7 = t5 + t6;
        final double t8 = ix * ix;
        final double t9 = iy * iy;
        final double t10 = 0.1e1 - t8 - t9;
        final double t11 = t7 * t10;
        final double t12 = t8 + t9;
        final double t15 = t10 * t10;
        final double t16 = t7 * t15;
        final double t17 = t12 * t12;
        final double t20 = t10 * t12;
        final double t22 = t10 * t5;
        final double t25 = t10 * ex;
        final double t26 = ix * ey;
        final double t27 = t26 * iy;
        final double t30 = t6 * t10;
        final double t33 = t15 * t12;
        final double t34 = t5 * t8;
        final double t40 = t6 * t9;
        final double t43 = t15 * t17;
        final double t47 = 0.2e1 - 0.90e2 * t11 * t12 + 0.350e3 * t16 * t17 - 0.40e2 * t20 + 0.60e2 * t22 * t8
            + 0.120e3 * t25 * t27 + 0.60e2 * t9 * t30
            - 0.280e3 * t33 * t34 - 0.560e3 * t33 * ex * t27 - 0.280e3 * t33 * t40 + 0.140e3 * t43 + 0.3e1 * t5
            + 0.3e1 * t6;
        final double t48 = a * a;
        final double t50 = t48 * t48;
        final double t54 = 0.1e1 - t5 - t6;
        final double t55 = t54 * t54;
        final double t57 = MathLib.sqrt(t54);
        final double t59 = 0.1e1 / t57 / t55 / t54;
        final double t63 = t25 * t12;
        final double t65 = ex * t15;
        final double t70 = ix * t10;
        final double t71 = ey * iy;
        final double t74 = ex * t8;
        final double t80 = -0.180e3 * t63 + 0.700e3 * t65 * t17 + 0.120e3 * t25 * t8 + 0.120e3 * t70 * t71 - 0.560e3
            * t33 * t74 - 0.560e3 * t33 * t27
            + 0.6e1 * ex;
        final double t82 = 0.1e1 / t50 / t48;
        final double t87 = t47 * t82;
        final double t88 = t55 * t55;
        final double t90 = 0.1e1 / t57 / t88;
        final double t91 = t90 * ex;
        final double t95 = 0.15e2 / 0.16e2 * t4 * t80 * t82 * t59 + 0.105e3 / 0.16e2 * t4 * t87 * t91;
        final double t96 = ey * t10;
        final double t102 = ix * iy;
        final double t107 = ex * ix;
        final double t115 = -0.180e3 * t96 * t12 + 0.700e3 * ey * t15 * t17 + 0.120e3 * t25 * t102 + 0.120e3 * t96 * t9
            - 0.560e3 * t33 * t107 * iy
            - 0.560e3 * t33 * ey * t9 + 0.6e1 * ey;
        final double t120 = t90 * ey;
        final double t124 = 0.15e2 / 0.16e2 * t4 * t115 * t82 * t59 + 0.105e3 / 0.16e2 * t87 * t120 * t4;
        final double t125 = t7 * ix;
        final double t130 = t17 * ix;
        final double t133 = ix * t12;
        final double t138 = t8 * ix;
        final double t139 = t138 * t5;
        final double t148 = t6 * ix;
        final double t149 = t148 * t9;
        final double t153 = t15 * t138;
        final double t156 = t5 * ix;
        final double t160 = t8 * ey * iy;
        final double t163 = t15 * t8;
        final double t164 = ex * ey;
        final double t165 = t164 * iy;
        final double t172 = t15 * ix;
        final double t175 = t10 * t17;
        final double t180 = -0.120e3 * t149 + 0.1120e4 * t20 * t139 - 0.560e3 * t153 * t5 - 0.560e3 * t33 * t156
            + 0.2240e4 * t63 * t160 - 0.1120e4
            * t163 * t165 - 0.560e3 * t33 * t165 + 0.1120e4 * t20 * t149 - 0.560e3 * t172 * t40 - 0.560e3 * t175
            * ix + 0.560e3 * t33
            * ix;
        final double t181 = 0.180e3 * t125 * t12 - 0.180e3 * t11 * ix - 0.1400e4 * t11 * t130 + 0.1400e4 * t16 * t133
            + 0.80e2 * t133 - 0.80e2 * t70
            - 0.120e3 * t139 + 0.120e3 * t22 * ix - 0.240e3 * t74 * t71 + 0.120e3 * t25 * t71 + t180;
        final double t185 = 0.15e2 / 0.16e2 * t4 * t181 * t82 * t59;
        final double t186 = t7 * iy;
        final double t191 = t17 * iy;
        final double t194 = iy * t12;
        final double t198 = t10 * iy;
        final double t200 = iy * t5;
        final double t201 = t200 * t8;
        final double t203 = t9 * ex;
        final double t208 = t9 * iy;
        final double t209 = t208 * t6;
        final double t216 = t15 * iy;
        final double t220 = t9 * ix * ey;
        final double t223 = t15 * t9;
        final double t224 = t107 * ey;
        final double t231 = t15 * t208;
        final double t234 = iy * t6;
        final double t241 = 0.120e3 * t30 * iy + 0.1120e4 * t20 * t201 - 0.560e3 * t216 * t34 + 0.2240e4 * t63 * t220
            - 0.1120e4 * t223 * t224 - 0.560e3
            * t33 * t224 + 0.1120e4 * t20 * t209 - 0.560e3 * t231 * t6 - 0.560e3 * t33 * t234 - 0.560e3 * t175 * iy
            + 0.560e3 * t33
            * iy;
        final double t242 = 0.180e3 * t186 * t12 - 0.180e3 * t11 * iy - 0.1400e4 * t11 * t191 + 0.1400e4 * t16 * t194
            + 0.80e2 * t194 - 0.80e2 * t198
            - 0.120e3 * t201 - 0.240e3 * t203 * t26 + 0.120e3 * t26 * t25 - 0.120e3 * t209 + t241;
        final double t246 = 0.15e2 / 0.16e2 * t4 * t242 * t82 * t59;
        final double t247 = 0.180e3 * t20;
        final double t248 = 0.700e3 * t43;
        final double t249 = t10 * t8;
        final double t255 = 0.1e1 / t50 / a;
        final double t260 = t80 * t255;
        final double t264 = t47 * t255;
        final double t267 = 0.1e1 / t57 / t88 / t54;
        final double t274 = 0.21e2 / 0.16e2 * t4 * t264 * t90;
        final double t288 = t115 * t255;
        final double t298 = -0.3e1 / 0.16e2 * t4 * (0.120e3 * t70 * iy - 0.560e3 * t33 * t102) * t255 * t59 - 0.21e2
            / 0.16e2 * t4 * t260 * t120
            - 0.21e2 / 0.16e2 * t4 * t288 * t91 - 0.189e3 / 0.16e2 * t1 * t3 * t47 * t255 * t267 * t164;
        final double t302 = 0.120e3 * t25 * ix;
        final double t305 = t65 * t133;
        final double t307 = t138 * ex;
        final double t311 = 0.120e3 * t96 * iy;
        final double t320 = t33 * t71;
        final double t322 = 0.360e3 * t107 * t12 - t302 - 0.2800e4 * t25 * t130 + 0.1680e4 * t305 - 0.240e3 * t307
            - 0.240e3 * t160 + t311 + 0.2240e4
            * t20 * t307 - 0.1120e4 * t153 * ex + 0.2240e4 * t20 * t160 - 0.1120e4 * t163 * t71 - 0.560e3 * t320;
        final double t327 = t181 * t255;
        final double t331 = -0.3e1 / 0.16e2 * t4 * t322 * t255 * t59 - 0.21e2 / 0.16e2 * t4 * t327 * t91;
        final double t332 = iy * ex;
        final double t335 = t25 * iy;
        final double t339 = t65 * t194;
        final double t341 = t332 * t8;
        final double t342 = 0.240e3 * t341;
        final double t343 = 0.240e3 * t220;
        final double t344 = t70 * ey;
        final double t347 = 0.2240e4 * t20 * t341;
        final double t349 = 0.1120e4 * t216 * t74;
        final double t351 = 0.2240e4 * t20 * t220;
        final double t353 = 0.1120e4 * t223 * t26;
        final double t354 = t33 * t26;
        final double t356 = 0.360e3 * t332 * t12 - 0.360e3 * t335 - 0.2800e4 * t25 * t191 + 0.2800e4 * t339 - t342
            - t343 + 0.120e3 * t344 + t347 - t349
            + t351 - t353 - 0.560e3 * t354;
        final double t361 = t242 * t255;
        final double t365 = -0.3e1 / 0.16e2 * t4 * t356 * t255 * t59 - 0.21e2 / 0.16e2 * t4 * t361 * t91;
        final double t366 = t10 * t9;
        final double t391 = 0.360e3 * t26 * t12 - 0.360e3 * t344 - 0.2800e4 * t96 * t130 + 0.2800e4 * t354 - t342
            + 0.120e3 * t335 - t343 + t347 - t349
            - 0.560e3 * t339 + t351 - t353;
        final double t399 = -0.3e1 / 0.16e2 * t4 * t391 * t255 * t59 - 0.21e2 / 0.16e2 * t4 * t327 * t120;
        final double t405 = t203 * ix;
        final double t407 = t208 * ey;
        final double t418 = 0.360e3 * t71 * t12 - t311 - 0.2800e4 * t96 * t191 + 0.1680e4 * t320 - 0.240e3 * t405
            + t302 - 0.240e3 * t407 + 0.2240e4
            * t20 * t405 - 0.1120e4 * t223 * t107 - 0.560e3 * t305 + 0.2240e4 * t20 * t407 - 0.1120e4 * t231 * ey;
        final double t426 = -0.3e1 / 0.16e2 * t4 * t418 * t255 * t59 - 0.21e2 / 0.16e2 * t4 * t361 * t120;
        final double t427 = t20 * t34;
        final double t429 = t20 * t40;
        final double t432 = 0.3360e4 * t172 * t165;
        final double t442 = t15 * t6 * t9;
        final double t444 = t7 * t8;
        final double t450 = 0.1400e4 * t11 * t17;
        final double t452 = 0.1400e4 * t16 * t12;
        final double t455 = t8 * t8;
        final double t463 = 0.6720e4 * t63 * t27;
        final double t464 = -0.80e2 + 0.5600e4 * t427 + 0.1120e4 * t429 - t432 - 0.4480e4 * t138 * t12 * t165
            + 0.8960e4 * t10 * t138 * t165 - 0.4480e4
            * t20 * t8 - 0.560e3 * t442 + 0.2800e4 * t444 * t17 + 0.2800e4 * t16 * t8 - t450 + t452 - 0.560e3
            * t33 * t5 - 0.2240e4
            * t455 * t12 * t5 + 0.4480e4 * t10 * t455 * t5 + t463;
        final double t465 = t163 * t5;
        final double t471 = 0.180e3 * t7 * t12;
        final double t472 = 0.180e3 * t11;
        final double t474 = 0.560e3 * t175;
        final double t475 = 0.560e3 * t33;
        final double t481 = t12 * t8;
        final double t489 = 0.720e3 * t107 * t71;
        final double t490 = -0.2800e4 * t465 + 0.720e3 * t444 - 0.600e3 * t34 - 0.120e3 * t40 + t471 - t472 + 0.120e3
            * t22 - t474 + t475 + 0.1120e4
            * t163 + 0.1120e4 * t8 * t17 + 0.480e3 * t8 + 0.160e3 * t9 - 0.11200e5 * t11 * t481 - 0.2240e4 * t481
            * t40 + 0.4480e4
            * t249 * t40 - t489;
        final double t508 = t208 * t12;
        final double t511 = t10 * t208;
        final double t522 = t74 * ey;
        final double t525 = t148 * iy;
        final double t528 = 0.2800e4 * t186 * t130 + 0.2800e4 * t16 * t102 - 0.2240e4 * t194 * t139 + 0.4480e4 * t198
            * t139 - 0.1120e4 * t216 * t156
            - 0.1120e4 * t223 * t164 - 0.2240e4 * t508 * t148 + 0.4480e4 * t511 * t148 - 0.4480e4 * t20 * t102
            - 0.1120e4 * t163 * t164
            - 0.560e3 * t33 * t164 - 0.1120e4 * t172 * t234 + 0.2240e4 * t20 * t522 + 0.2240e4 * t20 * t525;
        final double t529 = t203 * ey;
        final double t535 = t200 * ix;
        final double t538 = t9 * t12;
        final double t556 = 0.2240e4 * t20 * t529 - 0.11200e5 * t11 * t133 * iy + 0.2240e4 * t20 * t535 - 0.4480e4
            * t538 * t522 + 0.8960e4 * t366
            * t522 + 0.120e3 * t25 * ey - 0.240e3 * t529 + 0.1120e4 * t216 * ix - 0.240e3 * t522 + 0.720e3 * t125
            * iy + 0.1120e4
            * t191 * ix - 0.240e3 * t535 - 0.240e3 * t525 + 0.320e3 * t102;
        final double t561 = 0.3e1 / 0.16e2 * t4 * (t528 + t556) * t255 * t59;
        final double t574 = t9 * t9;
        final double t586 = -0.80e2 + 0.1120e4 * t427 + 0.5600e4 * t429 - 0.11200e5 * t11 * t538 - 0.2240e4 * t538
            * t34 + 0.4480e4 * t366 * t34 - t432
            - 0.4480e4 * t508 * t224 + 0.8960e4 * t511 * t224 + 0.4480e4 * t10 * t574 * t6 - 0.2800e4 * t442
            - 0.2240e4 * t574 * t12
            * t6 - t450 + t452 - 0.560e3 * t33 * t6 + 0.2800e4 * t16 * t9;
        final double t589 = t7 * t9;
        final double t602 = -0.4480e4 * t20 * t9 + 0.2800e4 * t589 * t17 + t463 - 0.560e3 * t465 - 0.120e3 * t34
            - 0.600e3 * t40 + t471 - t472 - t474
            + t475 + 0.120e3 * t30 + 0.720e3 * t589 + 0.1120e4 * t223 + 0.1120e4 * t9 * t17 + 0.160e3 * t8
            + 0.480e3 * t9 - t489;

        partialDerivativesJ4[0][0] += -0.45e2 / 0.8e1 * t4 * t47 / t50 / t48 / a * t59;
        partialDerivativesJ4[0][2] += t95;
        partialDerivativesJ4[0][3] += t124;
        partialDerivativesJ4[0][4] += t185;
        partialDerivativesJ4[0][5] += t246;
        partialDerivativesJ4[2][0] += t95;
        partialDerivativesJ4[2][2] += -0.3e1 / 0.16e2 * t4
            * (-t247 + t248 + 0.120e3 * t249 - 0.560e3 * t33 * t8 + 0.6e1) * t255 * t59 - 0.21e2 / 0.8e1
            * t4 * t260 * t91 - 0.189e3 / 0.16e2 * t4 * t264 * t267 * t5 - t274;
        partialDerivativesJ4[2][3] += t298;
        partialDerivativesJ4[2][4] += t331;
        partialDerivativesJ4[2][5] += t365;
        partialDerivativesJ4[3][0] += t124;
        partialDerivativesJ4[3][2] += t298;
        partialDerivativesJ4[3][3] += -0.3e1 / 0.16e2 * t4
            * (-t247 + t248 + 0.120e3 * t366 - 0.560e3 * t33 * t9 + 0.6e1) * t255 * t59 - 0.21e2 / 0.8e1
            * t4 * t288 * t120 - 0.189e3 / 0.16e2 * t4 * t264 * t267 * t6 - t274;
        partialDerivativesJ4[3][4] += t399;
        partialDerivativesJ4[3][5] += t426;
        partialDerivativesJ4[4][0] += t185;
        partialDerivativesJ4[4][2] += t331;
        partialDerivativesJ4[4][3] += t399;
        partialDerivativesJ4[4][4] += -0.3e1 / 0.16e2 * t4 * (t464 + t490) * t255 * t59;
        partialDerivativesJ4[4][5] += -t561;
        partialDerivativesJ4[5][0] += t246;
        partialDerivativesJ4[5][2] += t365;
        partialDerivativesJ4[5][3] += t426;
        partialDerivativesJ4[5][4] += -t561;
        partialDerivativesJ4[5][5] += -0.3e1 / 0.16e2 * t4 * (t586 + t602) * t255 * t59;

        return partialDerivativesJ4;

    }

    /**
     * Compute the partial derivatives due to J5 Potential effect
     * <p>
     * Compute the effect of the partial derivatives due to the 5th degree development of the Zonal Perturbation
     * </p>
     * 
     * @param orbit
     *        the equinoctial orbit
     * 
     * @return partialDerivativesJ5 the J5 partial derivatives
     */
    public double[][] computeJ5PartialDerivatives(final StelaEquinoctialOrbit orbit) {

        /** Orbital elements */
        final double a = orbit.getA();
        final double ex = orbit.getEquinoctialEx();
        final double ey = orbit.getEquinoctialEy();
        final double ix = orbit.getIx();
        final double iy = orbit.getIy();
        final double MU = orbit.getMu();

        /** J2 Potential */
        final double[][] partialDerivativesJ5 = this.computeJ4PartialDerivatives(orbit);

        final double t1 = MU * this.j[5];
        final double t2 = this.rEq * this.rEq;
        final double t3 = t2 * t2;
        final double t4 = t3 * this.rEq;
        final double t5 = ix * ix;
        final double t6 = iy * iy;
        final double t7 = 0.1e1 - t5 - t6;
        final double t8 = MathLib.sqrt(t7);
        final double t9 = t4 * t8;
        final double t10 = t1 * t9;
        final double t11 = ex * iy;
        final double t12 = ey * ix;
        final double t13 = t11 - t12;
        final double t14 = t7 * t7;
        final double t15 = t5 + t6;
        final double t16 = t14 * t15;
        final double t17 = ex * ex;
        final double t18 = t17 * t5;
        final double t21 = t7 * t17;
        final double t25 = t12 * iy;
        final double t28 = t7 * ex;
        final double t31 = ey * ey;
        final double t32 = t31 * t6;
        final double t35 = t7 * t31;
        final double t38 = t7 * t15;
        final double t40 = t17 + t31;
        final double t41 = t40 * t7;
        final double t46 = t40 * t14;
        final double t47 = t15 * t15;
        final double t50 = t14 * t47;
        final double t52 = -0.252e3 * t16 * t18 + 0.56e2 * t21 * t5 - 0.504e3 * t16 * ex * t25 + 0.112e3 * t28 * t25
            - 0.252e3 * t16 * t32 + 0.56e2
            * t35 * t6 - 0.168e3 * t38 - 0.140e3 * t41 * t15 + 0.9e1 * t17 + 0.9e1 * t31 + 0.12e2 + 0.441e3 * t46
            * t47 + 0.504e3 * t50;
        final double t53 = t13 * t52;
        final double t54 = a * a;
        final double t55 = t54 * t54;
        final double t56 = t55 * t55;
        final double t58 = 0.1e1 - t17 - t31;
        final double t59 = t58 * t58;
        final double t60 = t59 * t59;
        final double t61 = MathLib.sqrt(t58);
        final double t63 = 0.1e1 / t61 / t60;
        final double t68 = iy * t52;
        final double t71 = 0.1e1 / t55 / t54 / a;
        final double t72 = t71 * t63;
        final double t76 = ex * t5;
        final double t83 = t7 * ix;
        final double t84 = ey * iy;
        final double t87 = t28 * t15;
        final double t90 = ex * t14;
        final double t93 = -0.504e3 * t16 * t76 + 0.112e3 * t28 * t5 - 0.504e3 * t16 * t25 + 0.112e3 * t83 * t84
            - 0.280e3 * t87 + 0.18e2 * ex + 0.882e3
            * t90 * t47;
        final double t94 = t13 * t93;
        final double t100 = 0.1e1 / t61 / t60 / t58;
        final double t101 = t71 * t100;
        final double t106 = -0.15e2 / 0.4e1 * t10 * t68 * t72 - 0.15e2 / 0.4e1 * t10 * t94 * t72 - 0.135e3 / 0.4e1
            * t10 * t53 * t101 * ex;
        final double t107 = ix * t52;
        final double t111 = ex * ix;
        final double t115 = ix * iy;
        final double t121 = t7 * ey;
        final double t130 = -0.504e3 * t16 * t111 * iy + 0.112e3 * t28 * t115 - 0.504e3 * t16 * ey * t6 + 0.112e3
            * t121 * t6 - 0.280e3 * t121 * t15
            + 0.18e2 * ey + 0.882e3 * ey * t14 * t47;
        final double t131 = t13 * t130;
        final double t139 = 0.15e2 / 0.4e1 * t10 * t107 * t72 - 0.15e2 / 0.4e1 * t10 * t131 * t72 - 0.135e3 / 0.4e1
            * t10 * t53 * t101 * ey;
        final double t141 = t4 / t8;
        final double t142 = t1 * t141;
        final double t146 = ey * t52;
        final double t149 = t5 * ix;
        final double t150 = t149 * t17;
        final double t153 = t14 * t149;
        final double t156 = t17 * ix;
        final double t163 = t5 * ey * iy;
        final double t166 = t14 * t5;
        final double t167 = ex * ey;
        final double t168 = t167 * iy;
        final double t178 = ix * t31;
        final double t179 = t178 * t6;
        final double t182 = t14 * ix;
        final double t186 = ix * t15;
        final double t189 = t40 * ix;
        final double t194 = t47 * ix;
        final double t199 = t7 * t47;
        final double t204 = 0.1008e4 * t38 * t179 - 0.504e3 * t182 * t32 - 0.112e3 * t179 + 0.336e3 * t186 - 0.336e3
            * t83 + 0.280e3 * t189 * t15
            - 0.280e3 * t41 * ix - 0.1764e4 * t41 * t194 + 0.1764e4 * t46 * t186 - 0.2016e4 * t199 * ix + 0.2016e4
            * t16 * ix;
        final double t205 = 0.1008e4 * t38 * t150 - 0.504e3 * t153 * t17 - 0.504e3 * t16 * t156 - 0.112e3 * t150
            + 0.112e3 * t21 * ix + 0.2016e4 * t87
            * t163 - 0.1008e4 * t166 * t168 - 0.504e3 * t16 * t168 - 0.224e3 * t76 * t84 + 0.112e3 * t28 * t84
            + t204;
        final double t206 = t13 * t205;
        final double t209 = t142 * t53 * t72 * ix + t10 * t146 * t72 - t10 * t206 * t72;
        final double t213 = ex * t52;
        final double t216 = iy * t17;
        final double t217 = t216 * t5;
        final double t220 = t14 * iy;
        final double t225 = t6 * ix * ey;
        final double t228 = t14 * t6;
        final double t229 = t111 * ey;
        final double t234 = t6 * ex;
        final double t239 = t6 * iy;
        final double t240 = t239 * t31;
        final double t243 = t14 * t239;
        final double t247 = t31 * iy;
        final double t253 = iy * t15;
        final double t255 = t7 * iy;
        final double t257 = t40 * iy;
        final double t262 = t47 * iy;
        final double t271 = -0.504e3 * t16 * t247 - 0.112e3 * t240 + 0.112e3 * t35 * iy + 0.336e3 * t253 - 0.336e3
            * t255 + 0.280e3 * t257 * t15
            - 0.280e3 * t41 * iy - 0.1764e4 * t41 * t262 + 0.1764e4 * t46 * t253 - 0.2016e4 * t199 * iy + 0.2016e4
            * t16 * iy;
        final double t272 = 0.1008e4 * t38 * t217 - 0.504e3 * t220 * t18 - 0.112e3 * t217 + 0.2016e4 * t87 * t225
            - 0.1008e4 * t228 * t229 - 0.504e3
            * t16 * t229 - 0.224e3 * t234 * t12 + 0.112e3 * t28 * t12 + 0.1008e4 * t38 * t240 - 0.504e3 * t243
            * t31 + t271;
        final double t273 = t13 * t272;
        final double t276 = t142 * t53 * t72 * iy - t10 * t213 * t72 - t10 * t273 * t72;
        final double t279 = 0.1e1 / t55 / t54;
        final double t280 = t279 * t63;
        final double t284 = t279 * t100;
        final double t285 = t284 * ex;
        final double t286 = t68 * t285;
        final double t291 = t7 * t5;
        final double t293 = 0.280e3 * t38;
        final double t294 = 0.882e3 * t50;
        final double t305 = 0.1e1 / t61 / t60 / t59;
        final double t306 = t279 * t305;
        final double t313 = 0.45e2 / 0.8e1 * t10 * t53 * t284;
        final double t319 = t284 * ey;
        final double t320 = t68 * t319;
        final double t339 = t107 * t285;
        final double t353 = 0.5e1 / 0.8e1 * t10 * iy * t130 * t280 + 0.45e2 / 0.8e1 * t10 * t320 - 0.5e1 / 0.8e1 * t10
            * ix * t93 * t280 + 0.5e1 / 0.8e1
            * t10 * t13 * (-0.504e3 * t16 * t115 + 0.112e3 * t83 * iy) * t280 + 0.45e2 / 0.8e1 * t10 * t94 * t319
            - 0.45e2 / 0.8e1
            * t10 * t339 + 0.45e2 / 0.8e1 * t10 * t131 * t285 + 0.495e3 / 0.8e1 * t1 * t9 * t13 * t52 * t279 * t305
            * ex * ey;
        final double t354 = t280 * ix;
        final double t355 = t68 * t354;
        final double t357 = 0.5e1 / 0.8e1 * t142 * t355;
        final double t369 = t149 * ex;
        final double t374 = t90 * t186;
        final double t377 = t28 * ix;
        final double t383 = t16 * t84;
        final double t386 = t121 * iy;
        final double t392 = 0.2016e4 * t38 * t369 - 0.1008e4 * t153 * ex + 0.2520e4 * t374 - 0.224e3 * t369 - 0.336e3
            * t377 + 0.2016e4 * t38 * t163
            - 0.1008e4 * t166 * t84 - 0.504e3 * t383 - 0.224e3 * t163 + 0.112e3 * t386 + 0.560e3 * t111 * t15
            - 0.3528e4 * t28 * t194;
        final double t398 = t1 * t141 * t13;
        final double t403 = 0.45e2 / 0.8e1 * t10 * t146 * t285;
        final double t407 = -t357 + 0.5e1 / 0.8e1 * t10 * iy * t205 * t280 - 0.5e1 / 0.8e1 * t142 * t94 * t354 - 0.5e1
            / 0.8e1 * t10 * ey * t93 * t280
            + 0.5e1 / 0.8e1 * t10 * t13 * t392 * t280 - 0.45e2 / 0.8e1 * t398 * t339 - t403 + 0.45e2 / 0.8e1 * t10
            * t206 * t285;
        final double t416 = 0.5e1 / 0.8e1 * t1 * t4 * t8 * t52 * t280;
        final double t421 = t280 * iy;
        final double t429 = t11 * t5;
        final double t431 = 0.2016e4 * t38 * t429;
        final double t433 = 0.1008e4 * t220 * t76;
        final double t434 = 0.224e3 * t429;
        final double t436 = 0.2016e4 * t38 * t225;
        final double t438 = 0.1008e4 * t228 * t12;
        final double t439 = t16 * t12;
        final double t441 = 0.224e3 * t225;
        final double t442 = t83 * ey;
        final double t446 = t28 * iy;
        final double t450 = t90 * t253;
        final double t452 = t431 - t433 - t434 + t436 - t438 - 0.504e3 * t439 - t441 + 0.112e3 * t442 + 0.560e3 * t11
            * t15 - 0.560e3 * t446 - 0.3528e4
            * t28 * t262 + 0.3528e4 * t450;
        final double t466 = -0.5e1 / 0.8e1 * t142 * t6 * t52 * t280 + t416 + 0.5e1 / 0.8e1 * t10 * iy * t272 * t280
            - 0.5e1 / 0.8e1 * t142 * t94 * t421
            + 0.5e1 / 0.8e1 * t10 * ex * t93 * t280 + 0.5e1 / 0.8e1 * t10 * t13 * t452 * t280 - 0.45e2 / 0.8e1
            * t398 * t286 + 0.45e2
            / 0.8e1 * t10 * t17 * t52 * t284 + 0.45e2 / 0.8e1 * t10 * t273 * t285;
        final double t471 = t107 * t319;
        final double t476 = t7 * t6;
        final double t514 = t431 - t433 - 0.504e3 * t450 - t434 + 0.112e3 * t446 + t436 - t438 - t441 + 0.560e3 * t12
            * t15 - 0.560e3 * t442 - 0.3528e4
            * t121 * t194 + 0.3528e4 * t439;
        final double t528 = 0.5e1 / 0.8e1 * t142 * t5 * t52 * t280 - t416 - 0.5e1 / 0.8e1 * t10 * ix * t205 * t280
            - 0.5e1 / 0.8e1 * t142 * t131 * t354
            - 0.5e1 / 0.8e1 * t10 * ey * t130 * t280 + 0.5e1 / 0.8e1 * t10 * t13 * t514 * t280 - 0.45e2 / 0.8e1
            * t398 * t471 - 0.45e2
            / 0.8e1 * t10 * t31 * t52 * t284 + 0.45e2 / 0.8e1 * t10 * t206 * t319;
        final double t540 = t234 * ix;
        final double t548 = t239 * ey;
        final double t560 = 0.2016e4 * t38 * t540 - 0.1008e4 * t228 * t111 - 0.504e3 * t374 - 0.224e3 * t540 + 0.112e3
            * t377 + 0.2016e4 * t38 * t548
            - 0.1008e4 * t243 * ey + 0.2520e4 * t383 - 0.224e3 * t548 - 0.336e3 * t386 + 0.560e3 * t84 * t15
            - 0.3528e4 * t121 * t262;
        final double t570 = t357 - 0.5e1 / 0.8e1 * t10 * ix * t272 * t280 - 0.5e1 / 0.8e1 * t142 * t131 * t421 + 0.5e1
            / 0.8e1 * t10 * ex * t130 * t280
            + 0.5e1 / 0.8e1 * t10 * t13 * t560 * t280 - 0.45e2 / 0.8e1 * t398 * t320 + t403 + 0.45e2 / 0.8e1 * t10
            * t273 * t319;
        final double t573 = t4 / t8 / t7;
        final double t574 = t1 * t573;
        final double t587 = 0.5e1 / 0.8e1 * t142 * t53 * t280;
        final double t594 = t40 * t5;
        final double t599 = 0.280e3 * t40 * t15;
        final double t600 = 0.280e3 * t41;
        final double t602 = 0.2016e4 * t199;
        final double t603 = 0.2016e4 * t16;
        final double t608 = 0.6048e4 * t87 * t25;
        final double t609 = t15 * t5;
        final double t614 = -0.336e3 + 0.2016e4 * t5 + 0.672e3 * t6 + 0.1120e4 * t594 - 0.560e3 * t18 - 0.112e3 * t32
            + t599 - t600 + 0.112e3 * t21
            - t602 + t603 + 0.4032e4 * t166 + 0.4032e4 * t5 * t47 + t608 - 0.14112e5 * t41 * t609 - 0.2016e4
            * t609 * t32;
        final double t618 = 0.672e3 * t111 * t84;
        final double t619 = t38 * t18;
        final double t621 = t38 * t32;
        final double t624 = 0.3024e4 * t182 * t168;
        final double t631 = t5 * t5;
        final double t636 = 0.1764e4 * t41 * t47;
        final double t638 = 0.1764e4 * t46 * t15;
        final double t639 = t166 * t17;
        final double t644 = t14 * t31 * t6;
        final double t655 = 0.4032e4 * t291 * t32 - t618 + 0.5040e4 * t619 + 0.1008e4 * t621 - t624 - 0.4032e4 * t149
            * t15 * t168 + 0.8064e4 * t7
            * t149 * t168 - 0.2016e4 * t631 * t15 * t17 - t636 + t638 - 0.2520e4 * t639 - 0.504e3 * t16 * t17
            - 0.504e3 * t644
            + 0.3528e4 * t594 * t47 + 0.3528e4 * t46 * t5 + 0.4032e4 * t7 * t631 * t17 - 0.16128e5 * t38 * t5;
        final double t692 = t239 * t15;
        final double t695 = t7 * t239;
        final double t706 = t76 * ey;
        final double t709 = 0.1344e4 * t115 + 0.3528e4 * t257 * t194 + 0.3528e4 * t46 * t115 - 0.2016e4 * t253 * t150
            + 0.4032e4 * t255 * t150
            - 0.1008e4 * t220 * t156 - 0.1008e4 * t228 * t167 - 0.2016e4 * t692 * t178 + 0.4032e4 * t695 * t178
            - 0.16128e5 * t38
            * t115 - 0.1008e4 * t166 * t167 - 0.504e3 * t16 * t167 - 0.1008e4 * t182 * t247 + 0.2016e4 * t38 * t706;
        final double t710 = t178 * iy;
        final double t713 = t234 * ey;
        final double t719 = t216 * ix;
        final double t722 = t6 * t15;
        final double t739 = 0.2016e4 * t38 * t710 + 0.2016e4 * t38 * t713 - 0.14112e5 * t41 * t186 * iy + 0.2016e4
            * t38 * t719 - 0.4032e4 * t722 * t706
            + 0.8064e4 * t476 * t706 - 0.224e3 * t719 + 0.4032e4 * t262 * ix - 0.224e3 * t713 - 0.224e3 * t706
            + 0.112e3 * t28 * ey
            + 0.1120e4 * t189 * iy - 0.224e3 * t710 + 0.4032e4 * t220 * ix;
        final double t744 = -t1 * t573 * t13 * t355 - t142 * t213 * t354 - t142 * t273 * t354 + t142 * t146 * t421
            - t10 * ey * t272 * t280 - t142
            * t206 * t421 + t10 * ex * t205 * t280 + t10 * t13 * (t709 + t739) * t280;
        final double t764 = t40 * t6;
        final double t775 = -0.336e3 + 0.672e3 * t5 + 0.2016e4 * t6 - 0.112e3 * t18 - 0.560e3 * t32 + t599 - t600
            - t602 + t603 + 0.112e3 * t35
            + 0.1120e4 * t764 + 0.4032e4 * t228 + 0.4032e4 * t6 * t47 - 0.14112e5 * t41 * t722 - 0.2016e4 * t722
            * t18 + 0.4032e4
            * t476 * t18;
        final double t776 = t6 * t6;
        final double t799 = t608 - 0.2016e4 * t776 * t15 * t31 + 0.4032e4 * t7 * t776 * t31 - 0.16128e5 * t38 * t6
            + 0.3528e4 * t764 * t47 - t618
            + 0.1008e4 * t619 + 0.5040e4 * t621 - t624 - 0.4032e4 * t692 * t229 + 0.8064e4 * t695 * t229 - t636
            - 0.504e3 * t16 * t31
            + t638 - 0.504e3 * t639 - 0.2520e4 * t644 + 0.3528e4 * t46 * t6;

        partialDerivativesJ5[0][0] += 0.105e3 / 0.4e1 * t10 * t53 / t56 * t63;
        partialDerivativesJ5[0][2] += t106;
        partialDerivativesJ5[0][3] += t139;
        partialDerivativesJ5[0][4] += 0.15e2 / 0.4e1 * t209;
        partialDerivativesJ5[0][5] += 0.15e2 / 0.4e1 * t276;
        partialDerivativesJ5[2][0] += t106;
        partialDerivativesJ5[2][2] += 0.5e1 / 0.4e1 * t10 * iy * t93 * t280 + 0.45e2 / 0.4e1 * t10 * t286 + 0.5e1
            / 0.8e1 * t10 * t13
            * (-0.504e3 * t16 * t5 + 0.112e3 * t291 - t293 + 0.18e2 + t294) * t280 + 0.45e2 / 0.4e1 * t10 * t94
            * t285 + 0.495e3
            / 0.8e1 * t10 * t53 * t306 * t17 + t313;
        partialDerivativesJ5[2][3] += t353;
        partialDerivativesJ5[2][4] += t407;
        partialDerivativesJ5[2][5] += t466;
        partialDerivativesJ5[3][0] += t139;
        partialDerivativesJ5[3][2] += t353;
        partialDerivativesJ5[3][3] += -0.5e1 / 0.4e1 * t10 * ix * t130 * t280 - 0.45e2 / 0.4e1 * t10 * t471 + 0.5e1
            / 0.8e1 * t10 * t13
            * (-0.504e3 * t16 * t6 + 0.112e3 * t476 - t293 + 0.18e2 + t294) * t280 + 0.45e2 / 0.4e1 * t10 * t131
            * t319 + 0.495e3
            / 0.8e1 * t10 * t53 * t306 * t31 + t313;
        partialDerivativesJ5[3][4] += t528;
        partialDerivativesJ5[3][5] += t570;
        partialDerivativesJ5[4][0] += 0.15e2 / 0.4e1 * t209;
        partialDerivativesJ5[4][2] += t407;
        partialDerivativesJ5[4][3] += t528;
        partialDerivativesJ5[4][4] += -0.5e1 / 0.8e1 * t574 * t53 * t280 * t5 + 0.5e1 / 0.4e1 * t142 * t146 * t354
            - 0.5e1 / 0.4e1 * t142 * t206 * t354
            - t587 - 0.5e1 / 0.4e1 * t10 * ey * t205 * t280 + 0.5e1 / 0.8e1 * t10 * t13 * (t614 + t655) * t280;
        partialDerivativesJ5[4][5] += 0.5e1 / 0.8e1 * t744;
        partialDerivativesJ5[5][0] += 0.15e2 / 0.4e1 * t276;
        partialDerivativesJ5[5][2] += t466;
        partialDerivativesJ5[5][3] += t570;
        partialDerivativesJ5[5][4] += 0.5e1 / 0.8e1 * t744;
        partialDerivativesJ5[5][5] += -0.5e1 / 0.8e1 * t574 * t53 * t280 * t6 - 0.5e1 / 0.4e1 * t142 * t213 * t421
            - 0.5e1 / 0.4e1 * t142 * t273 * t421
            - t587 + 0.5e1 / 0.4e1 * t10 * ex * t272 * t280 + 0.5e1 / 0.8e1 * t10 * t13 * (t775 + t799) * t280;

        return partialDerivativesJ5;

    }

    /**
     * Compute the partial derivatives due to J6 Potential effect
     * <p>
     * Compute the effect of the partial derivatives due to the 6th degree development of the Zonal Perturbation
     * </p>
     * 
     * @param orbit
     *        the equinoctial orbit
     * 
     * @return partialDerivativesJ6 the J6 partial derivatives
     */
    public double[][] computeJ6PartialDerivatives(final StelaEquinoctialOrbit orbit) {

        /** Orbital elements */
        final double a = orbit.getA();
        final double ex = orbit.getEquinoctialEx();
        final double ey = orbit.getEquinoctialEy();
        final double ix = orbit.getIx();
        final double iy = orbit.getIy();
        final double MU = orbit.getMu();

        /** J2 Potential */
        final double[][] partialDerivativesJ6 = this.computeJ5PartialDerivatives(orbit);

        final double t1 = MU * this.j[6];
        final double t2 = this.rEq * this.rEq;
        final double t3 = t2 * t2;
        final double t4 = t3 * t2;
        final double t5 = t1 * t4;
        final double t6 = ex * ex;
        final double t7 = ey * ey;
        final double t8 = t6 + t7;
        final double t9 = ix * ix;
        final double t10 = iy * iy;
        final double t11 = 0.1e1 - t9 - t10;
        final double t12 = t8 * t11;
        final double t14 = ey * ix;
        final double t15 = t14 * iy;
        final double t18 = t11 * t11;
        final double t19 = t11 * t18;
        final double t20 = t9 + t10;
        final double t21 = t19 * t20;
        final double t22 = t21 * ex;
        final double t23 = t7 * ey;
        final double t24 = ix * t23;
        final double t25 = t10 * iy;
        final double t26 = t24 * t25;
        final double t29 = t18 * t20;
        final double t30 = t29 * ex;
        final double t31 = t30 * t15;
        final double t33 = t21 * t6;
        final double t34 = t9 * t7;
        final double t35 = t34 * t10;
        final double t38 = t6 * t9;
        final double t39 = t29 * t38;
        final double t41 = t6 * ex;
        final double t42 = t21 * t41;
        final double t43 = t9 * ix;
        final double t44 = t43 * ey;
        final double t45 = t44 * iy;
        final double t48 = t7 * t10;
        final double t49 = t29 * t48;
        final double t51 = t6 * t6;
        final double t52 = t9 * t9;
        final double t53 = t51 * t52;
        final double t60 = t20 * t20;
        final double t61 = t19 * t60;
        final double t62 = t61 * t38;
        final double t64 = t61 * t48;
        final double t66 = t7 * t7;
        final double t67 = t10 * t10;
        final double t68 = t66 * t67;
        final double t71 = t18 * t60;
        final double t73 = t11 * t20;
        final double t75 = t60 * t20;
        final double t76 = t19 * t75;
        final double t79 = t61 * ex * t15;
        final double t81 = t18 * t6;
        final double t84 = t18 * ex;
        final double t87 = -0.8e1 - 0.1680e4 * t12 * ex * t15 + 0.44352e5 * t22 * t26 + 0.40320e5 * t31 + 0.66528e5
            * t33 * t35 + 0.20160e5 * t39
            + 0.44352e5 * t42 * t45 + 0.20160e5 * t49 + 0.11088e5 * t21 * t53 - 0.840e3 * t12 * t48 - 0.840e3 * t12
            * t38 - 0.55440e5
            * t62 - 0.55440e5 * t64 + 0.11088e5 * t21 * t68 - 0.3024e4 * t71 + 0.336e3 * t73 + 0.7392e4 * t76
            - 0.110880e6 * t79
            - 0.15120e5 * t81 * t35 - 0.10080e5 * t84 * t26;
        final double t88 = t8 * t19;
        final double t89 = t60 * t6;
        final double t90 = t89 * t9;
        final double t93 = t8 * t18;
        final double t94 = t20 * t6;
        final double t95 = t94 * t9;
        final double t98 = t20 * t7;
        final double t99 = t98 * t10;
        final double t102 = t60 * t7;
        final double t103 = t102 * t10;
        final double t106 = t41 * t18;
        final double t109 = t11 * ex;
        final double t110 = t109 * t15;
        final double t112 = t18 * t66;
        final double t115 = t93 * t60;
        final double t117 = t11 * t7;
        final double t118 = t117 * t10;
        final double t119 = 0.1680e4 * t118;
        final double t120 = t12 * t20;
        final double t123 = t88 * t75;
        final double t125 = t18 * t51;
        final double t128 = t11 * t6;
        final double t129 = t128 * t9;
        final double t130 = 0.1680e4 * t129;
        final double t131 = t8 * t8;
        final double t132 = t131 * t19;
        final double t135 = t131 * t11;
        final double t138 = t131 * t18;
        final double t141 = t93 * t20;
        final double t142 = ex * ix;
        final double t143 = ey * iy;
        final double t144 = t142 * t143;
        final double t147 = t88 * t60;
        final double t153 = 0.64680e5 * t123 - 0.2520e4 * t125 * t52 - t130 + 0.29106e5 * t132 * t75 + 0.1050e4 * t135
            * t20 - 0.11025e5 * t138 * t60
            + 0.25200e5 * t141 * t144 - 0.77616e5 * t144 * t147 - 0.15e2 * t131 - 0.40e2 * t6 - 0.40e2 * t7;
        final double t155 = t87 - 0.38808e5 * t88 * t90 + 0.12600e5 * t93 * t95 + 0.12600e5 * t93 * t99 - 0.38808e5
            * t88 * t103 - 0.10080e5 * t106
            * t45 - 0.3360e4 * t110 - 0.2520e4 * t112 * t67 - 0.25200e5 * t115 - t119 + 0.2520e4 * t120 + t153;
        final double t156 = a * a;
        final double t157 = t156 * t156;
        final double t158 = t157 * t157;
        final double t162 = 0.1e1 - t6 - t7;
        final double t163 = t162 * t162;
        final double t164 = t163 * t163;
        final double t166 = MathLib.sqrt(t162);
        final double t168 = 0.1e1 / t166 / t164 / t162;
        final double t176 = ex * t9;
        final double t183 = t18 * ix;
        final double t184 = t23 * t25;
        final double t189 = t11 * ix;
        final double t192 = t29 * t6;
        final double t197 = t61 * t6;
        final double t202 = t20 * t9;
        final double t207 = t41 * t52;
        final double t210 = t41 * t19;
        final double t211 = t60 * t9;
        final double t214 = t60 * ex;
        final double t217 = t20 * ex;
        final double t220 = 0.133056e6 * t22 * t35 + 0.133056e6 * t33 * t45 + 0.40320e5 * t29 * t176 - 0.1680e4 * t12
            * t176 + 0.25200e5 * t141 * t15
            - 0.10080e5 * t183 * t184 - 0.77616e5 * t147 * t15 - 0.3360e4 * t189 * t143 + 0.50400e5 * t192 * t15
            - 0.1680e4 * t109
            * t48 - 0.155232e6 * t197 * t15 - 0.60e2 * t8 * ex + 0.25200e5 * t106 * t202 - 0.110880e6 * t61 * t176
            + 0.44352e5 * t21
            * t207 - 0.77616e5 * t210 * t211 - 0.44100e5 * t93 * t214 + 0.4200e4 * t12 * t217;
        final double t221 = t75 * ex;
        final double t228 = t217 * t9;
        final double t231 = t214 * t9;
        final double t234 = ex * t19;
        final double t254 = t84 * t60;
        final double t256 = t41 * t11;
        final double t259 = t109 * t20;
        final double t263 = 0.116424e6 * t88 * t221 + 0.44352e5 * t21 * t26 - 0.1680e4 * t12 * t15 + 0.25200e5 * t93
            * t228 - 0.77616e5 * t88 * t231
            - 0.77616e5 * t234 * t103 + 0.25200e5 * t84 * t99 - 0.30240e5 * t84 * t35 - 0.3360e4 * t128 * t15
            - 0.110880e6 * t61 * t15
            + 0.40320e5 * t29 * t15 - 0.30240e5 * t81 * t45 - 0.80e2 * ex + 0.129360e6 * t234 * t75 - 0.10080e5
            * t106 * t52
            - 0.50400e5 * t254 - 0.1680e4 * t256 * t9 + 0.5040e4 * t259 - 0.3360e4 * t109 * t9;
        final double t264 = t220 + t263;
        final double t265 = 0.1e1 / t158;
        final double t270 = t155 * t265;
        final double t273 = 0.1e1 / t166 / t164 / t163;
        final double t274 = t273 * ex;
        final double t278 = 0.35e2 / 0.128e3 * t5 * t264 * t265 * t168 + 0.385e3 / 0.128e3 * t5 * t270 * t274;
        final double t279 = t20 * ey;
        final double t280 = t279 * t10;
        final double t285 = ey * t10;
        final double t288 = t43 * iy;
        final double t293 = t23 * t19;
        final double t294 = t60 * t10;
        final double t297 = t23 * t18;
        final double t298 = t20 * t10;
        final double t301 = t11 * ey;
        final double t304 = t7 * t29;
        final double t305 = t142 * iy;
        final double t310 = t23 * t67;
        final double t315 = t60 * ey;
        final double t318 = t9 * ey;
        final double t319 = t318 * t10;
        final double t322 = t75 * ey;
        final double t327 = t315 * t10;
        final double t332 = 0.25200e5 * t93 * t280 - 0.60e2 * t8 * ey - 0.1680e4 * t12 * t285 - 0.10080e5 * t106 * t288
            + 0.40320e5 * t29 * t285
            - 0.77616e5 * t293 * t294 + 0.25200e5 * t297 * t298 - 0.1680e4 * t301 * t38 + 0.50400e5 * t304 * t305
            + 0.4200e4 * t12
            * t279 + 0.44352e5 * t21 * t310 - 0.110880e6 * t61 * t285 - 0.44100e5 * t93 * t315 - 0.30240e5 * t81
            * t319 + 0.116424e6
            * t88 * t322 - 0.3360e4 * t117 * t305 - 0.77616e5 * t88 * t327 - 0.110880e6 * t61 * t305;
        final double t333 = ix * t7;
        final double t334 = t333 * t25;
        final double t339 = t18 * ey;
        final double t342 = ey * t19;
        final double t345 = t61 * t7;
        final double t348 = ix * iy;
        final double t353 = t41 * t43;
        final double t376 = t23 * t11;
        final double t379 = -0.30240e5 * t84 * t334 + 0.40320e5 * t29 * t305 + 0.25200e5 * t339 * t95 - 0.77616e5
            * t342 * t90 - 0.155232e6 * t345
            * t305 - 0.3360e4 * t109 * t348 - 0.1680e4 * t12 * t305 + 0.44352e5 * t21 * t353 * iy + 0.25200e5
            * t141 * t305 - 0.80e2
            * ey - 0.77616e5 * t147 * t305 + 0.133056e6 * t33 * t319 + 0.133056e6 * t22 * t334 - 0.50400e5 * t339
            * t60 - 0.3360e4
            * t301 * t10 - 0.10080e5 * t297 * t67 + 0.129360e6 * t342 * t75 + 0.5040e4 * t301 * t20 - 0.1680e4
            * t376 * t10;
        final double t380 = t332 + t379;
        final double t385 = t273 * ey;
        final double t389 = 0.35e2 / 0.128e3 * t5 * t380 * t265 * t168 + 0.385e3 / 0.128e3 * t5 * t270 * t385;
        final double t390 = t176 * t143;
        final double t393 = t88 * t20;
        final double t399 = t94 * t43;
        final double t402 = t333 * t10;
        final double t405 = t52 * ey;
        final double t406 = t405 * iy;
        final double t409 = t19 * t52;
        final double t410 = ey * t41;
        final double t411 = t410 * iy;
        final double t414 = t8 * t9;
        final double t415 = ey * ex;
        final double t416 = t415 * iy;
        final double t419 = t19 * t43;
        final double t420 = t6 * t7;
        final double t421 = t420 * t10;
        final double t424 = t19 * t9;
        final double t425 = ex * t23;
        final double t426 = t425 * t25;
        final double t429 = t9 * t23;
        final double t430 = t429 * t25;
        final double t433 = t43 * t7;
        final double t434 = t433 * t10;
        final double t439 = t89 * t43;
        final double t444 = t60 * ix;
        final double t447 = t9 * t18;
        final double t453 = 0.465696e6 * t115 * t390 - 0.310464e6 * t393 * t390 - 0.100800e6 * t120 * t390 + 0.672e3
            * t189 - 0.50400e5 * t12 * t399
            + 0.25200e5 * t93 * t402 + 0.40320e5 * t256 * t406 + 0.88704e5 * t409 * t411 + 0.3360e4 * t414 * t416
            + 0.133056e6 * t419
            * t421 + 0.88704e5 * t424 * t426 + 0.40320e5 * t109 * t430 + 0.60480e5 * t128 * t434 - 0.155232e6 * t88
            * t399 + 0.232848e6
            * t93 * t439 + 0.40320e5 * t29 * t416 + 0.100800e6 * t12 * t444 + 0.80640e5 * t447 * t416 - 0.80640e5
            * t73 * t402
            + 0.6720e4 * t390;
        final double t454 = ix * t20;
        final double t457 = t43 * t6;
        final double t464 = t51 * t43;
        final double t481 = t6 * ix;
        final double t491 = t318 * iy;
        final double t501 = t75 * ix;
        final double t506 = 0.332640e6 * t71 * t402 + 0.40320e5 * t29 * t481 - 0.77616e5 * t88 * t89 * ix - 0.30240e5
            * t81 * t402 - 0.110880e6 * t61
            * t416 - 0.30240e5 * t106 * t491 + 0.25200e5 * t93 * t94 * ix - 0.110880e6 * t61 * t481 - 0.1680e4
            * t12 * t481
            - 0.174636e6 * t138 * t501 + 0.388080e6 * t88 * t444;
        final double t515 = t52 * ix;
        final double t521 = t11 * t66;
        final double t525 = t19 * ix;
        final double t532 = t8 * ix;
        final double t554 = t8 * t43;
        final double t559 = 0.174636e6 * t132 * t444 - 0.161280e6 * t259 * t491 + 0.25200e5 * t141 * t416 - 0.77616e5
            * t147 * t416 + 0.133056e6 * t33
            * t402 + 0.133056e6 * t42 * t491 + 0.2100e4 * t135 * ix - 0.12096e5 * t29 * ix - 0.3360e4 * t128 * ix
            + 0.1680e4 * t554
            * t6 - 0.5040e4 * t532 * t20;
        final double t564 = t18 * t75;
        final double t569 = t19 * t515;
        final double t572 = t131 * ix;
        final double t577 = t11 * t51;
        final double t580 = t18 * t43;
        final double t585 = t11 * t60;
        final double t589 = t41 * t29;
        final double t602 = t93 * t9;
        final double t609 = 0.12096e5 * t585 * ix + 0.3360e4 * t457 - 0.266112e6 * t589 * t406 - 0.399168e6 * t192
            * t434 - 0.266112e6 * t30 * t430
            - 0.50400e5 * t120 * t402 + 0.232848e6 * t115 * t402 - 0.155232e6 * t393 * t402 + 0.50400e5 * t602
            * t416 + 0.665280e6
            * t254 * t491 - 0.443520e6 * t22 * t491;
        final double t612 = -0.66528e5 * t29 * t51 * t515 + t609 + 0.44352e5 * t21 * t464 - 0.3360e4 * t109 * t143
            + 0.5040e4 * t12 * ix - 0.80640e5
            * t73 * t457 + 0.40320e5 * t183 * t48 - 0.10080e5 * t84 * t184 + 0.3360e4 * t402 + t453 - 0.672e3
            * t454 - 0.100800e6 * t93
            * t454 + 0.40320e5 * t580 * t6 + 0.22176e5 * t569 * t51 - 0.2100e4 * t572 * t20 + 0.10080e5 * t577
            * t515 + 0.44100e5
            * t135 * t444 + t559 - 0.10080e5 * t125 * t43 + 0.1680e4 * t532 * t48 - 0.221760e6 * t21 * t457
            + 0.332640e6 * t71 * t457
            + 0.22176e5 * t525 * t68 + 0.25200e5 * t93 * t457 - 0.44352e5 * t564 * ix + 0.44352e5 * t61 * ix
            - 0.66528e5 * t29 * t68
            * ix + 0.10080e5 * t521 * t67 * ix - 0.388080e6 * t93 * t501 - 0.44100e5 * t138 * t454 + t506
            + 0.44352e5 * t21 * t426
            - 0.1680e4 * t12 * t416 - 0.221760e6 * t21 * t402;
        final double t616 = 0.35e2 / 0.128e3 * t5 * t612 * t265 * t168;
        final double t617 = t142 * ey;
        final double t620 = t98 * iy;
        final double t623 = t102 * iy;
        final double t626 = t24 * t10;
        final double t629 = t10 * ex;
        final double t630 = t629 * t14;
        final double t637 = t24 * t67;
        final double t640 = t34 * t25;
        final double t645 = t34 * iy;
        final double t648 = iy * t6;
        final double t649 = t648 * t9;
        final double t657 = t10 * ix * ey;
        final double t660 = t93 * t10;
        final double t663 = t11 * iy;
        final double t665 = iy * t20;
        final double t667 = t25 * t7;
        final double t671 = -0.110880e6 * t61 * t617 + 0.25200e5 * t93 * t620 - 0.77616e5 * t88 * t623 - 0.30240e5
            * t84 * t626 - 0.310464e6 * t393
            * t630 + 0.465696e6 * t115 * t630 - 0.100800e6 * t120 * t630 - 0.266112e6 * t30 * t637 - 0.399168e6
            * t192 * t640
            + 0.133056e6 * t22 * t626 + 0.133056e6 * t33 * t645 + 0.232848e6 * t115 * t649 - 0.155232e6 * t393
            * t649 - 0.50400e5
            * t120 * t649 + 0.665280e6 * t254 * t657 + 0.50400e5 * t660 * t617 + 0.672e3 * t663 - 0.672e3 * t665
            + 0.3360e4 * t667
            - 0.161280e6 * t259 * t657;
        final double t678 = t44 * t10;
        final double t681 = t8 * t10;
        final double t684 = t19 * t10;
        final double t685 = t353 * ey;
        final double t698 = t98 * t25;
        final double t701 = t102 * t25;
        final double t708 = t10 * t18;
        final double t717 = t19 * t67;
        final double t718 = t142 * t23;
        final double t721 = t60 * iy;
        final double t726 = -0.50400e5 * t12 * t698 + 0.232848e6 * t93 * t701 - 0.155232e6 * t88 * t698 + 0.40320e5
            * t256 * t678 + 0.80640e5 * t708
            * t617 - 0.80640e5 * t73 * t649 + 0.332640e6 * t71 * t649 - 0.221760e6 * t21 * t649 + 0.88704e5 * t717
            * t718 + 0.100800e6
            * t12 * t721 - 0.100800e6 * t93 * t665;
        final double t732 = t18 * iy;
        final double t735 = t7 * iy;
        final double t740 = t8 * iy;
        final double t743 = t67 * iy;
        final double t744 = t66 * t743;
        final double t756 = t75 * iy;
        final double t767 = t19 * iy;
        final double t772 = t66 * t25;
        final double t779 = -0.10080e5 * t106 * t44 - 0.388080e6 * t93 * t756 - 0.44100e5 * t138 * t665 + 0.44100e5
            * t135 * t721 + 0.174636e6 * t132
            * t721 - 0.174636e6 * t138 * t756 + 0.22176e5 * t767 * t53 + 0.25200e5 * t93 * t667 + 0.44352e5 * t21
            * t772 + 0.332640e6
            * t71 * t667 + 0.388080e6 * t88 * t721;
        final double t781 = t52 * iy;
        final double t786 = t18 * t25;
        final double t791 = t19 * t743;
        final double t794 = t131 * iy;
        final double t803 = t8 * t25;
        final double t824 = t19 * t25;
        final double t825 = t38 * t7;
        final double t830 = -0.12096e5 * t29 * iy + 0.5040e4 * t12 * iy - 0.10080e5 * t112 * t25 + 0.3360e4 * t649
            - 0.5040e4 * t740 * t20 + 0.10080e5
            * t521 * t743 - 0.30240e5 * t81 * t645 - 0.1680e4 * t12 * t617 + 0.44352e5 * t21 * t685 + 0.133056e6
            * t824 * t825
            + 0.40320e5 * t29 * t617;
        final double t833 = -0.77616e5 * t147 * t617 - 0.266112e6 * t589 * t678 + 0.3360e4 * t681 * t617 + 0.1680e4
            * t803 * t7 - 0.2100e4 * t794 * t20
            - 0.443520e6 * t22 * t657 + 0.25200e5 * t141 * t617 + 0.6720e4 * t630 + t830 - 0.221760e6 * t21 * t667
            - 0.110880e6 * t61
            * t735 - 0.1680e4 * t12 * t735 - 0.66528e5 * t29 * t53 * iy + 0.1680e4 * t740 * t38 - 0.66528e5 * t29
            * t744 + 0.40320e5
            * t29 * t735 - 0.3360e4 * t109 * t14 - 0.80640e5 * t73 * t667 + 0.40320e5 * t732 * t38 + t726
            - 0.3360e4 * t117 * iy
            + 0.40320e5 * t109 * t637 + 0.60480e5 * t128 * t640 + 0.25200e5 * t93 * t649 + 0.44352e5 * t61 * iy
            - 0.44352e5 * t564 * iy
            + 0.2100e4 * t135 * iy + 0.12096e5 * t585 * iy + 0.40320e5 * t786 * t7 + 0.22176e5 * t791 * t66
            + 0.88704e5 * t684 * t685
            + t779 + 0.10080e5 * t577 * t781 + t671;
        final double t837 = 0.35e2 / 0.128e3 * t5 * t833 * t265 * t168;
        final double t838 = 0.151200e6 * t31;
        final double t845 = t11 * t9;
        final double t847 = 0.50400e5 * t71;
        final double t848 = 0.5040e4 * t73;
        final double t849 = 0.129360e6 * t76;
        final double t850 = 0.465696e6 * t79;
        final double t851 = 0.10080e5 * t110;
        final double t858 = -0.80e2 + t838 + 0.126000e6 * t39 + 0.25200e5 * t49 - 0.388080e6 * t62 - 0.77616e5 * t64
            + 0.266112e6 * t22 * t45 - 0.3360e4
            * t845 - t847 + t848 + t849 - t850 - t851 + 0.133056e6 * t21 * t35 - 0.60480e5 * t84 * t45 - 0.30240e5
            * t447 * t48;
        final double t863 = t6 * t52;
        final double t868 = t128 * t20;
        final double t872 = t81 * t60;
        final double t876 = 0.44100e5 * t115;
        final double t879 = 0.4200e4 * t120;
        final double t880 = 0.116424e6 * t123;
        final double t881 = t6 * t19;
        final double t887 = 0.25200e5 * t93 * t202 - 0.77616e5 * t88 * t211 + 0.133056e6 * t21 * t863 - 0.110880e6
            * t61 * t9 + 0.8400e4 * t868
            + 0.40320e5 * t29 * t9 - 0.88200e5 * t872 - 0.1680e4 * t12 * t9 - t876 - t119 - 0.30240e5 * t81 * t52
            + t879 + t880
            + 0.232848e6 * t881 * t75 - 0.8400e4 * t129 - 0.180e3 * t6 - 0.60e2 * t7;
        final double t891 = 0.1e1 / t157 / t156 / a;
        final double t896 = t264 * t891;
        final double t900 = t155 * t891;
        final double t904 = 0.1e1 / t166 / t164 / t163 / t162;
        final double t911 = 0.55e2 / 0.128e3 * t5 * t900 * t273;
        final double t917 = t457 * iy;
        final double t920 = t721 * ix;
        final double t923 = t454 * iy;
        final double t930 = t7 * t19;
        final double t937 = t18 * t7;
        final double t946 = 0.266112e6 * t22 * t319 + 0.133056e6 * t21 * t334 + 0.133056e6 * t21 * t917 - 0.77616e5
            * t88 * t920 + 0.25200e5 * t93
            * t923 - 0.155232e6 * t881 * t920 + 0.50400e5 * t81 * t923 - 0.155232e6 * t930 * t920 + 0.50400e5
            * t339 * t228
            - 0.155232e6 * t342 * t231 + 0.50400e5 * t937 * t923 - 0.155232e6 * t234 * t327 + 0.50400e5 * t84
            * t280 - 0.60480e5 * t84
            * t319;
        final double t974 = -0.3360e4 * t117 * t348 + 0.40320e5 * t29 * t348 - 0.3360e4 * t109 * t285 + 0.232848e6
            * t342 * t221 - 0.30240e5 * t183
            * t667 - 0.3360e4 * t301 * t176 - 0.88200e5 * t339 * t214 + 0.8400e4 * t301 * t217 - 0.1680e4 * t12
            * t348 - 0.110880e6
            * t61 * t348 - 0.3360e4 * t128 * t348 - 0.30240e5 * t81 * t288 - 0.3360e4 * t189 * iy - 0.120e3 * t415;
        final double t983 = t380 * t891;
        final double t993 = -0.5e1 / 0.128e3 * t5 * (t946 + t974) * t891 * t168 - 0.55e2 / 0.128e3 * t5 * t896 * t385
            - 0.55e2 / 0.128e3 * t5 * t983
            * t274 - 0.715e3 / 0.128e3 * t1 * t4 * t155 * t891 * t904 * t415;
        final double t994 = t33 * t491;
        final double t996 = t868 * t491;
        final double t999 = t43 * ex;
        final double t1001 = t143 * t20;
        final double t1002 = t93 * t1001;
        final double t1004 = t315 * iy;
        final double t1005 = t88 * t1004;
        final double t1009 = t142 * t20;
        final double t1010 = t93 * t1009;
        final double t1013 = 0.10080e5 * t84 * t402;
        final double t1028 = t217 * t43;
        final double t1038 = -0.221760e6 * t994 - 0.201600e6 * t996 + 0.3360e4 * t353 + 0.6720e4 * t999 + 0.25200e5
            * t1002 - 0.77616e5 * t1005
            + 0.50400e5 * t81 * t1001 - 0.126000e6 * t1010 - t1013 - 0.155232e6 * t881 * t1004 - 0.161280e6 * t73
            * t491 - 0.266112e6
            * t29 * t430 + 0.266112e6 * t409 * t6 * ey * iy + 0.266112e6 * t419 * ex * t7 * t10 - 0.100800e6 * t12
            * t1028 + 0.465696e6
            * t93 * t214 * t43 + 0.50400e5 * t93 * t491 + 0.120960e6 * t109 * t434;
        final double t1042 = 0.10080e5 * t81 * t491;
        final double t1049 = t214 * ix;
        final double t1055 = t22 * t402;
        final double t1057 = t88 * t1049;
        final double t1061 = t84 * t454;
        final double t1063 = t29 * t143;
        final double t1073 = t38 * t143;
        final double t1077 = -0.310464e6 * t88 * t1028 + t1042 + 0.665280e6 * t71 * t491 - 0.443520e6 * t21 * t491
            + 0.120960e6 * t128 * t406
            + 0.176400e6 * t12 * t1049 - 0.698544e6 * t93 * t221 * ix - 0.44352e5 * t1055 + 0.543312e6 * t1057
            + 0.201600e6 * t109
            * t444 - 0.120960e6 * t1061 + 0.40320e5 * t1063 + 0.80640e5 * t447 * t143 - 0.161280e6 * t73 * t999
            - 0.776160e6 * t84
            * t501 + 0.40320e5 * t845 * t184 + 0.6720e4 * t1073 + 0.50400e5 * t93 * t999;
        final double t1083 = t41 * t515;
        final double t1086 = t20 * t43;
        final double t1087 = t210 * t1086;
        final double t1098 = t234 * t444;
        final double t1100 = t12 * t143;
        final double t1102 = t21 * t184;
        final double t1106 = t142 * t48;
        final double t1110 = t12 * t142;
        final double t1114 = t61 * t143;
        final double t1118 = 0.3360e4 * t414 * t143 - 0.8400e4 * t532 * t217 - 0.266112e6 * t29 * t1083 - 0.133056e6
            * t1087 + 0.465696e6 * t106 * t60
            * t43 - 0.100800e6 * t256 * t1086 - 0.443520e6 * t21 * t999 + 0.665280e6 * t71 * t999 + 0.554400e6
            * t1098 - 0.1680e4
            * t1100 + 0.44352e5 * t1102 + 0.88704e5 * t424 * t184 + 0.3360e4 * t1106 - 0.3360e4 * t128 * t143
            + 0.5040e4 * t1110
            + 0.50400e5 * t106 * t454 - 0.110880e6 * t1114 - 0.155232e6 * t210 * t444;
        final double t1122 = 0.3360e4 * t301 * iy;
        final double t1124 = 0.10080e5 * t106 * t43;
        final double t1134 = 0.3360e4 * t109 * ix;
        final double t1138 = 0.10080e5 * t297 * t25;
        final double t1149 = t872 * t491;
        final double t1151 = t259 * t402;
        final double t1153 = t254 * t402;
        final double t1155 = -0.3360e4 * t256 * ix - t1122 + t1124 + 0.3360e4 * t554 * ex + 0.6720e4 * t491 + 0.40320e5
            * t256 * t515 - 0.10080e5
            * t1009 + 0.80640e5 * t580 * ex + t1134 + 0.88704e5 * t569 * t41 - t1138 - 0.798336e6 * t192 * t406
            + 0.465696e6 * t115
            * t491 - 0.310464e6 * t393 * t491 - 0.100800e6 * t120 * t491 - 0.798336e6 * t30 * t434 + 0.931392e6
            * t1149 - 0.100800e6
            * t1151 + 0.465696e6 * t1153;
        final double t1162 = t612 * t891;
        final double t1166 = -0.5e1 / 0.128e3 * t5 * (t1038 + t1077 + t1118 + t1155) * t891 * t168 - 0.55e2 / 0.128e3
            * t5 * t1162 * t274;
        final double t1169 = 0.266112e6 * t824 * t176 * t7;
        final double t1171 = 0.161280e6 * t73 * t657;
        final double t1172 = ex * iy;
        final double t1173 = t1172 * t9;
        final double t1175 = 0.161280e6 * t73 * t1173;
        final double t1176 = t444 * ey;
        final double t1178 = 0.155232e6 * t881 * t1176;
        final double t1179 = t14 * t20;
        final double t1181 = 0.50400e5 * t81 * t1179;
        final double t1183 = 0.155232e6 * t234 * t623;
        final double t1185 = 0.50400e5 * t84 * t620;
        final double t1186 = t93 * t1179;
        final double t1188 = t21 * t626;
        final double t1190 = t44 * t6;
        final double t1191 = t21 * t1190;
        final double t1193 = t88 * t1176;
        final double t1195 = t84 * t645;
        final double t1197 = t81 * t657;
        final double t1200 = 0.665280e6 * t71 * t657;
        final double t1202 = 0.443520e6 * t21 * t657;
        final double t1204 = 0.50400e5 * t93 * t1173;
        final double t1206 = 0.50400e5 * t93 * t657;
        final double t1208 = 0.266112e6 * t29 * t637;
        final double t1210 = 0.266112e6 * t684 * t1190;
        final double t1211 = t1169 - t1171 - t1175 - t1178 + t1181 - t1183 + t1185 + 0.25200e5 * t1186 + 0.133056e6
            * t1188 + 0.133056e6 * t1191
            - 0.77616e5 * t1193 - 0.60480e5 * t1195 + 0.100800e6 * t1197 + t1200 - t1202 + t1204 + t1206 - t1208
            + t1210;
        final double t1213 = 0.120960e6 * t109 * t640;
        final double t1217 = t214 * iy;
        final double t1218 = t88 * t1217;
        final double t1220 = t202 * iy;
        final double t1221 = t210 * t1220;
        final double t1229 = 0.665280e6 * t71 * t1173;
        final double t1231 = 0.443520e6 * t21 * t1173;
        final double t1233 = 0.120960e6 * t128 * t678;
        final double t1234 = t234 * t698;
        final double t1240 = t1172 * t20;
        final double t1241 = t93 * t1240;
        final double t1245 = 0.266112e6 * t29 * t207 * iy;
        final double t1248 = t109 * iy;
        final double t1250 = 0.6720e4 * t1173;
        final double t1251 = 0.6720e4 * t657;
        final double t1254 = t1213 - 0.698544e6 * t93 * t221 * iy + 0.698544e6 * t1218 - 0.310464e6 * t1221
            + 0.465696e6 * t106 * t211 * iy - 0.100800e6
            * t256 * t1220 + t1229 - t1231 + t1233 - 0.310464e6 * t1234 + 0.465696e6 * t84 * t701 - 0.100800e6
            * t109 * t698
            - 0.176400e6 * t1241 - t1245 + 0.176400e6 * t12 * t1217 + 0.10080e5 * t1248 + t1250 + t1251
            + 0.201600e6 * t109 * t721;
        final double t1257 = t84 * t665;
        final double t1262 = t189 * ey;
        final double t1268 = 0.80640e5 * t708 * t14;
        final double t1270 = 0.80640e5 * t732 * t176;
        final double t1272 = 0.798336e6 * t192 * t678;
        final double t1274 = 0.100800e6 * t120 * t657;
        final double t1278 = 0.310464e6 * t393 * t1173;
        final double t1280 = 0.100800e6 * t120 * t1173;
        final double t1282 = 0.465696e6 * t115 * t657;
        final double t1284 = 0.310464e6 * t393 * t657;
        final double t1286 = 0.798336e6 * t30 * t640;
        final double t1289 = t33 * t657;
        final double t1292 = 0.465696e6 * t115 * t1173;
        final double t1293 = t29 * t14;
        final double t1295 = -0.10080e5 * t1240 - 0.201600e6 * t1257 + 0.3360e4 * ex * t25 * t7 - 0.3360e4 * t1262
            + 0.3360e4 * t41 * iy * t9 + t1268
            + t1270 - t1272 - t1274 - 0.201600e6 * t868 * t657 - t1278 - t1280 + t1282 - t1284 - t1286
            + 0.931392e6 * t872 * t657
            - 0.620928e6 * t1289 + t1292 + 0.40320e5 * t1293;
        final double t1296 = t22 * t645;
        final double t1299 = 0.3360e4 * t681 * t14;
        final double t1301 = 0.40320e5 * t256 * t781;
        final double t1303 = t106 * iy * t9;
        final double t1308 = 0.88704e5 * t767 * t207;
        final double t1311 = t12 * t1172;
        final double t1313 = t234 * t721;
        final double t1316 = 0.3360e4 * t740 * t176;
        final double t1321 = 0.40320e5 * t189 * t310;
        final double t1322 = t84 * t667;
        final double t1325 = 0.88704e5 * t717 * t24;
        final double t1326 = t12 * t14;
        final double t1328 = t81 * t44;
        final double t1330 = t23 * t10;
        final double t1331 = t183 * t1330;
        final double t1334 = 0.3360e4 * t128 * t14;
        final double t1335 = t61 * t14;
        final double t1338 = 0.3360e4 * t109 * t735;
        final double t1339 = 0.266112e6 * t1296 + t1299 + t1301 + 0.50400e5 * t1303 - 0.8400e4 * t740 * t217 + t1308
            - 0.776160e6 * t84 * t756
            + 0.8400e4 * t1311 + 0.776160e6 * t1313 + t1316 + 0.6720e4 * t6 * t10 * t14 + t1321 + 0.50400e5
            * t1322 + t1325 - 0.1680e4
            * t1326 - 0.30240e5 * t1328 - 0.30240e5 * t1331 - t1334 - 0.110880e6 * t1335 - t1338;
        final double t1346 = t833 * t891;
        final double t1350 = -0.5e1 / 0.128e3 * t5 * (t1211 + t1254 + t1295 + t1339) * t891 * t168 - 0.55e2 / 0.128e3
            * t5 * t1346 * t274;
        final double t1355 = t38 * t10;
        final double t1358 = t14 * t25;
        final double t1363 = t12 * t10;
        final double t1367 = -0.80e2 + t838 + 0.25200e5 * t39 + 0.126000e6 * t49 - 0.77616e5 * t62 - 0.388080e6 * t64
            - t847 + t848 + t849 - t850 - t851
            + 0.133056e6 * t21 * t1355 - 0.60480e5 * t84 * t1358 + 0.266112e6 * t22 * t1358 - 0.1680e4 * t1363
            - 0.110880e6 * t61 * t10;
        final double t1368 = t60 * t937;
        final double t1370 = t11 * t10;
        final double t1379 = t117 * t20;
        final double t1381 = t7 * t67;
        final double t1393 = -0.88200e5 * t1368 - 0.3360e4 * t1370 - t876 - 0.8400e4 * t118 + 0.232848e6 * t930 * t75
            + t879 + t880 - t130 + 0.40320e5
            * t29 * t10 - 0.30240e5 * t937 * t67 + 0.8400e4 * t1379 + 0.133056e6 * t21 * t1381 - 0.30240e5 * t81
            * t9 * t10 - 0.60e2
            * t6 - 0.180e3 * t7 + 0.25200e5 * t93 * t298 - 0.77616e5 * t88 * t294;
        final double t1414 = -0.10080e5 * t1179 + t1169 - t1171 - t1175 - t1178 + t1181 - t1183 + t1185 - 0.176400e6
            * t1186 - 0.310464e6 * t1188
            - 0.310464e6 * t1191 + 0.698544e6 * t1193 + 0.100800e6 * t1195 - 0.60480e5 * t1197 + t1200 - t1202
            + t1204 + t1206 - t1208;
        final double t1439 = t1210 + t1213 - 0.77616e5 * t1218 + 0.133056e6 * t1221 + t1229 - t1231 + t1233
            + 0.133056e6 * t1234 + 0.25200e5 * t1241
            - t1245 - 0.100800e6 * t301 * t399 + 0.465696e6 * t339 * t439 + 0.465696e6 * t297 * t294 * ix
            - 0.100800e6 * t376 * t298
            * ix + 0.176400e6 * t12 * t1176 - 0.698544e6 * t93 * t322 * ix - 0.201600e6 * t1379 * t1173
            + 0.931392e6 * t1368 * t1173
            - 0.3360e4 * t1248;
        final double t1448 = t1250 + t1251 + 0.3360e4 * t1190 + 0.40320e5 * t1257 + 0.10080e5 * t1262 + t1268 + t1270
            - t1272 - t1274 - t1278 - t1280
            + t1282 - t1284 - t1286 + 0.266112e6 * t1289 + t1292 + 0.3360e4 * t626 - 0.201600e6 * t1293
            - 0.620928e6 * t1296;
        final double t1465 = t1299 + t1301 - 0.30240e5 * t1303 + t1308 - 0.1680e4 * t1311 - 0.8400e4 * t532 * t279
            + 0.6720e4 * t34 * t1172 - 0.776160e6
            * t339 * t501 - 0.110880e6 * t1313 + t1316 + t1321 - 0.30240e5 * t1322 + t1325 + 0.8400e4 * t1326
            + 0.50400e5 * t1328
            + 0.50400e5 * t1331 - t1334 + 0.776160e6 * t1335 - t1338 + 0.201600e6 * t301 * t444;
        final double t1475 = -0.5e1 / 0.128e3 * t5 * (t1414 + t1439 + t1448 + t1465) * t891 * t168 - 0.55e2 / 0.128e3
            * t5 * t1162 * t385;
        final double t1478 = t25 * ey;
        final double t1481 = t333 * t67;
        final double t1484 = t318 * t25;
        final double t1487 = t629 * ix;
        final double t1501 = -0.44352e5 * t994 - 0.100800e6 * t996 + 0.6720e4 * t1478 + 0.3360e4 * t184 - 0.798336e6
            * t30 * t1481 - 0.798336e6 * t192
            * t1484 - 0.100800e6 * t120 * t1487 + 0.465696e6 * t115 * t1487 - 0.310464e6 * t393 * t1487
            - 0.126000e6 * t1002
            + 0.543312e6 * t1005 + 0.25200e5 * t1010 + t1013 - t1042 - 0.221760e6 * t1055 - 0.77616e5 * t1057
            + 0.40320e5 * t1061
            - 0.120960e6 * t1063;
        final double t1523 = t279 * t25;
        final double t1541 = 0.3360e4 * t1073 + 0.44352e5 * t1087 - 0.110880e6 * t1098 - 0.161280e6 * t73 * t1487
            + 0.266112e6 * t717 * t142 * t7
            - 0.266112e6 * t29 * t353 * t10 + 0.266112e6 * t824 * t38 * ey - 0.698544e6 * t93 * t322 * iy
            + 0.50400e5 * t937 * t1009
            - 0.155232e6 * t930 * t1049 - 0.310464e6 * t88 * t1523 + 0.465696e6 * t93 * t315 * t25 - 0.443520e6
            * t21 * t1487
            + 0.665280e6 * t71 * t1487 - 0.100800e6 * t12 * t1523 + 0.50400e5 * t93 * t1487 + 0.120960e6 * t109
            * t1481 + 0.120960e6
            * t128 * t1484;
        final double t1564 = 0.5040e4 * t1100 - 0.133056e6 * t1102 + 0.6720e4 * t1106 + 0.176400e6 * t12 * t1004
            - 0.1680e4 * t1110 + 0.554400e6 * t1114
            - 0.10080e5 * t1001 + 0.6720e4 * t1487 + 0.80640e5 * t786 * ey + 0.88704e5 * t791 * t23 + 0.3360e4
            * t803 * ey + 0.40320e5
            * t376 * t743 - 0.3360e4 * t376 * iy + t1122 - t1124 - t1134 + t1138 - 0.155232e6 * t293 * t721;
        final double t1589 = t20 * t25;
        final double t1604 = 0.50400e5 * t297 * t665 - 0.3360e4 * t117 * t142 + 0.50400e5 * t93 * t1478 + 0.88704e5
            * t684 * t353 + 0.3360e4 * t681
            * t142 - 0.266112e6 * t29 * t23 * t743 - 0.8400e4 * t740 * t279 + 0.665280e6 * t71 * t1478
            - 0.443520e6 * t21 * t1478
            - 0.776160e6 * t339 * t756 + 0.465696e6 * t297 * t60 * t25 - 0.100800e6 * t376 * t1589 + 0.40320e5
            * t256 * t43 * t10
            + 0.201600e6 * t301 * t721 + 0.80640e5 * t708 * t142 - 0.161280e6 * t73 * t1478 + 0.465696e6 * t1149
            - 0.201600e6 * t1151
            + 0.931392e6 * t1153;
        final double t1614 = -0.5e1 / 0.128e3 * t5 * (t1501 + t1541 + t1564 + t1604) * t891 * t168 - 0.55e2 / 0.128e3
            * t5 * t1346 * t385;
        final double t1627 = 0.1995840e7 * t192 * t35;
        final double t1628 = t589 * t45;
        final double t1630 = t30 * t26;
        final double t1632 = t73 * t41;
        final double t1637 = t12 * t60;
        final double t1652 = t52 * t9;
        final double t1653 = t1652 * t51;
        final double t1656 = 0.672e3 + 0.266112e6 * t42 * t15 + 0.201600e6 * t554 * t20 * t416 - 0.403200e6 * t12 * t43
            * t416 - 0.620928e6 * t88 * t43
            * t416 - t1627 - 0.1862784e7 * t1628 - 0.798336e6 * t1630 + 0.1064448e7 * t1632 * t515 * ey * iy
            - 0.931392e6 * t1637 * t35
            + 0.1862784e7 * t141 * t35 + 0.1064448e7 * t259 * t43 * t23 * t25 + 0.1596672e7 * t868 * t52 * t7 * t10
            - 0.24192e5 * t447
            - 0.24192e5 * t211 - 0.20160e5 * t1653 - 0.20160e5 * t414;
        final double t1660 = 0.5040e4 * t8 * t20;
        final double t1661 = 0.5040e4 * t12;
        final double t1663 = 0.12096e5 * t585;
        final double t1664 = 0.12096e5 * t29;
        final double t1665 = 0.2100e4 * t135;
        final double t1667 = 0.2100e4 * t131 * t20;
        final double t1668 = t131 * t9;
        final double t1670 = 0.44352e5 * t564;
        final double t1672 = 0.1995840e7 * t254 * t15;
        final double t1674 = 0.1330560e7 * t22 * t15;
        final double t1675 = t585 * ex;
        final double t1681 = 0.1397088e7 * t115 * t144;
        final double t1683 = 0.931392e6 * t393 * t144;
        final double t1684 = t999 * t143;
        final double t1687 = 0.16800e5 * t38 + 0.3360e4 * t48 - t1660 + t1661 - 0.3360e4 * t128 + t1663 - t1664 + t1665
            - t1667 - 0.8400e4 * t1668
            - t1670 + t1672 - t1674 - 0.2661120e7 * t1675 * t45 + 0.5322240e7 * t30 * t45 + t1681 - t1683
            - 0.1862784e7 * t1637 * t1684;
        final double t1692 = 0.302400e6 * t120 * t144;
        final double t1693 = 0.44352e5 * t61;
        final double t1695 = 0.665280e6 * t424 * t421;
        final double t1698 = t88 * t95;
        final double t1700 = t93 * t90;
        final double t1702 = t109 * t26;
        final double t1705 = 0.302400e6 * t128 * t35;
        final double t1706 = t525 * t426;
        final double t1708 = t12 * t99;
        final double t1710 = t88 * t99;
        final double t1712 = t93 * t103;
        final double t1716 = t18 * t52;
        final double t1723 = 0.3725568e7 * t141 * t1684 - t1692 + t1693 + t1695 + 0.133056e6 * t21 * t421 - 0.776160e6
            * t1698 + 0.1164240e7 * t1700
            + 0.120960e6 * t1702 + t1705 + 0.266112e6 * t1706 - 0.50400e5 * t1708 - 0.155232e6 * t1710
            + 0.232848e6 * t1712 - 0.60480e5
            * t106 * t15 - 0.1596672e7 * t1716 * t421 - 0.1064448e7 * t580 * t426 - 0.201600e6 * t12 * t35;
        final double t1725 = t52 * t20 * t6;
        final double t1732 = 0.241920e6 * t183 * t416;
        final double t1733 = t12 * t95;
        final double t1735 = t256 * t45;
        final double t1737 = t419 * t411;
        final double t1743 = t414 * t6;
        final double t1746 = t9 * t66 * t67;
        final double t1751 = t11 * t75;
        final double t1754 = t577 * t52;
        final double t1756 = t409 * t51;
        final double t1764 = t19 * t66 * t67;
        final double t1766 = t521 * t67;
        final double t1768 = 0.1862784e7 * t93 * t1725 - 0.931392e6 * t12 * t89 * t52 + t1732 - 0.252000e6 * t1733
            + 0.282240e6 * t1735 + 0.620928e6
            * t1737 + 0.100800e6 * t414 * t99 - 0.1330560e7 * t585 * t35 + 0.8400e4 * t1743 + 0.266112e6 * t73
            * t1746 - 0.532224e6
            * t71 * t9 + 0.177408e6 * t1751 * t9 + 0.90720e5 * t1754 + 0.199584e6 * t1756 - 0.266112e6 * t18
            * t1652 * t51 - 0.30240e5
            * t125 * t9 + 0.22176e5 * t1764 + 0.10080e5 * t1766;
        final double t1775 = t8 * t7 * t10;
        final double t1779 = 0.100800e6 * t1637;
        final double t1780 = 0.100800e6 * t141;
        final double t1781 = t447 * t6;
        final double t1784 = t937 * t10;
        final double t1799 = 0.174636e6 * t132 * t60;
        final double t1800 = 0.177408e6 * t21 * t9 - 0.110880e6 * t197 + 0.1680e4 * t1775 - 0.1680e4 * t12 * t6 + t1779
            - t1780 + 0.201600e6 * t1781
            + 0.40320e5 * t192 + 0.40320e5 * t1784 - 0.88200e5 * t138 * t9 - 0.201600e6 * t414 * t60 - 0.201600e6
            * t602 + 0.161280e6
            * t1725 - 0.322560e6 * t11 * t52 * t6 + 0.96768e5 * t73 * t9 - 0.20160e5 * t1746 + t1799;
        final double t1801 = 0.388080e6 * t147;
        final double t1803 = 0.174636e6 * t138 * t75;
        final double t1805 = 0.44100e5 * t135 * t60;
        final double t1807 = 0.44100e5 * t138 * t20;
        final double t1809 = 0.388080e6 * t93 * t75;
        final double t1816 = t18 * t515;
        final double t1839 = t1801 - t1803 + t1805 - t1807 - t1809 + 0.2661120e7 * t29 * t35 - 0.310464e6 * t88 * t35
            - 0.887040e6 * t419 * t416
            - 0.1064448e7 * t1816 * t411 - 0.443520e6 * t409 * t6 - 0.645120e6 * t11 * t43 * t416 + 0.322560e6
            * t1086 * t416
            - 0.88200e5 * t1668 * t60 - 0.201600e6 * t12 * t863 - 0.310464e6 * t88 * t863 - 0.120960e6 * t863
            * t48 - 0.80640e5 * t999
            * t184 + 0.133056e6 * t21 * t51 * t9;
        final double t1841 = t73 * t38;
        final double t1843 = t73 * t48;
        final double t1845 = t71 * t38;
        final double t1847 = t21 * t38;
        final double t1857 = t29 * t53;
        final double t1866 = t71 * t48;
        final double t1872 = t75 * t9;
        final double t1879 = -0.403200e6 * t1841 - 0.80640e5 * t1843 + 0.1663200e7 * t1845 - 0.1108800e7 * t1847
            - 0.266112e6 * t447 * t68 - 0.443520e6
            * t424 * t48 - 0.1330560e7 * t585 * t863 + 0.2661120e7 * t29 * t863 - 0.598752e6 * t1857 - 0.80640e5
            * t1083 * t143
            + 0.100800e6 * t8 * t52 * t94 - 0.77616e5 * t88 * t89 + 0.332640e6 * t1866 - 0.2095632e7 * t138 * t211
            + 0.698544e6 * t132
            * t202 + 0.1552320e7 * t12 * t1872 - 0.4656960e7 * t93 * t211 + 0.1552320e7 * t88 * t202;
        final double t1884 = t29 * t68;
        final double t1886 = t21 * t48;
        final double t1892 = t93 * t38;
        final double t1894 = 0.20160e5 * t144;
        final double t1897 = t93 * t48;
        final double t1906 = 0.10080e5 * t532 * t416;
        final double t1910 = 0.151200e6 * t93 * ix * t416;
        final double t1913 = 0.483840e6 * t259 * t15;
        final double t1914 = 0.698544e6 * t135 * t1872 + 0.352800e6 * t135 * t202 - 0.66528e5 * t1884 - 0.221760e6
            * t1886 + 0.266112e6 * t73 * t1653
            - 0.322560e6 * t845 * t48 + 0.126000e6 * t1892 + t1894 - 0.30240e5 * t81 * t48 + 0.25200e5 * t1897
            + 0.25200e5 * t93 * t94
            + 0.806400e6 * t12 * t202 + 0.161280e6 * t202 * t48 + t1906 - 0.1344e4 * t10 + t1910 - 0.4032e4 * t9
            - t1913;
        final double t1923 = t176 * t285;
        final double t1928 = t333 * iy;
        final double t1933 = t744 * ix;
        final double t1935 = t176 * ey;
        final double t1939 = iy * t51;
        final double t1940 = t1939 * t515;
        final double t1944 = t629 * ey;
        final double t1955 = t648 * ix;
        final double t1957 = -0.2688e4 * t348 + 0.3725568e7 * t141 * t1923 - 0.1862784e7 * t1637 * t1923 + 0.6720e4
            * t1928 - 0.24192e5 * t920
            - 0.24192e5 * t732 * ix - 0.20160e5 * t1933 + 0.6720e4 * t1935 - 0.3360e4 * t109 * ey - 0.20160e5
            * t1940 - 0.8400e4 * t572
            * iy + 0.6720e4 * t1944 - 0.20160e5 * t532 * iy - 0.77616e5 * t88 * t214 * ey - 0.161280e6 * t73
            * t1928 - 0.161280e6 * t73
            * t1935 + 0.6720e4 * t1955;
        final double t1976 = t501 * iy;
        final double t1991 = t425 * t67;
        final double t1999 = 0.266112e6 * t73 * t1933 + 0.2661120e7 * t29 * t334 - 0.443520e6 * t21 * t1955
            + 0.665280e6 * t71 * t1955 + 0.665280e6
            * t71 * t1944 - 0.443520e6 * t21 * t1944 - 0.887040e6 * t684 * t1935 + 0.698544e6 * t132 * t923
            - 0.2095632e7 * t138 * t920
            + 0.698544e6 * t135 * t1976 - 0.310464e6 * t88 * t334 - 0.1596672e7 * t580 * t420 * t25 - 0.266112e6
            * t29 * t464 * iy
            + 0.120960e6 * t256 * t319 + 0.352800e6 * t135 * t923 - 0.266112e6 * t29 * t1991 - 0.1064448e7 * t447
            * t1991 - 0.1064448e7
            * t1716 * t410 * t10;
        final double t2041 = 0.1552320e7 * t88 * t923 - 0.4656960e7 * t93 * t920 + 0.1552320e7 * t12 * t1976
            + 0.25200e5 * t93 * t217 * ey - 0.310464e6
            * t88 * t917 - 0.1330560e7 * t585 * t334 - 0.201600e6 * t12 * t917 - 0.201600e6 * t12 * t334
            + 0.100800e6 * t740 * t399
            + 0.80640e5 * t183 * t735 + 0.88704e5 * t525 * t772 - 0.80640e5 * t41 * t10 * t405 - 0.120960e6 * t25
            * t6 * t433
            - 0.80640e5 * t67 * ex * t429 + 0.40320e5 * t109 * t310 + 0.88704e5 * t767 * t464 + 0.177408e6 * t21
            * t348 - 0.266112e6
            * t1816 * t1939;
        final double t2053 = t41 * t9 * ey;
        final double t2075 = t18 * t743;
        final double t2083 = 0.177408e6 * t1751 * t348 - 0.532224e6 * t71 * t348 + 0.40320e5 * t256 * t405 + 0.80640e5
            * t447 * t415 + 0.40320e5 * t29
            * t415 + 0.266112e6 * t684 * t2053 + 0.266112e6 * t824 * t481 * t7 + 0.50400e5 * t93 * t1944
            + 0.50400e5 * t93 * t1955
            - 0.88200e5 * t794 * t444 - 0.88200e5 * t138 * t348 - 0.1680e4 * t12 * t415 + 0.40320e5 * t577 * t288
            - 0.443520e6 * t767
            * t457 + 0.3360e4 * t740 * t481 - 0.266112e6 * t2075 * t66 * ix - 0.443520e6 * t824 * t333 + 0.3360e4
            * t681 * t415;
        final double t2109 = t11 * t25;
        final double t2122 = 0.88704e5 * t717 * t425 + 0.3360e4 * t532 * t735 + 0.266112e6 * t73 * t1940 + 0.100800e6
            * t803 * t98 * ix - 0.201600e6
            * t740 * t444 - 0.201600e6 * t93 * t348 + 0.161280e6 * t665 * t457 - 0.322560e6 * t663 * t457
            + 0.80640e5 * t732 * t481
            + 0.80640e5 * t708 * t415 + 0.161280e6 * t1589 * t333 - 0.322560e6 * t2109 * t333 + 0.96768e5 * t73
            * t348 - 0.110880e6
            * t61 * t415 - 0.30240e5 * t106 * t318 + 0.3360e4 * t414 * t415 + 0.88704e5 * t409 * t410;
        final double t2133 = t425 * t10;
        final double t2150 = t429 * t10;
        final double t2164 = -0.30240e5 * t84 * t1330 - 0.161280e6 * t73 * t1944 - 0.443520e6 * t21 * t1935
            + 0.2661120e7 * t29 * t917 - 0.1330560e7
            * t585 * t917 + 0.133056e6 * t21 * t2133 + 0.665280e6 * t71 * t1935 + 0.665280e6 * t71 * t1928
            - 0.266112e6 * t29 * t207
            * ey - 0.443520e6 * t21 * t1928 - 0.266112e6 * t29 * t772 * ix + 0.50400e5 * t93 * t1935 + 0.120960e6
            * t109 * t2150
            + 0.266112e6 * t419 * t420 * iy + 0.266112e6 * t424 * t2133 + 0.465696e6 * t115 * t1935 + 0.266112e6
            * t33 * t1928
            + 0.806400e6 * t12 * t923;
        final double t2178 = t433 * iy;
        final double t2205 = -0.161280e6 * t73 * t1955 + 0.322560e6 * t298 * t1935 - 0.645120e6 * t1370 * t1935
            + 0.465696e6 * t115 * t1955 - 0.310464e6
            * t393 * t1955 - 0.798336e6 * t589 * t319 + 0.120960e6 * t128 * t2178 - 0.100800e6 * t120 * t1955
            - 0.931392e6 * t1637
            * t917 + 0.1862784e7 * t141 * t917 - 0.100800e6 * t120 * t1935 - 0.310464e6 * t393 * t1935
            + 0.1064448e7 * t1632 * t405
            * t10 - 0.931392e6 * t1637 * t334 + 0.1862784e7 * t141 * t334 + 0.1596672e7 * t868 * t433 * t25
            - 0.2661120e7 * t1675
            * t319 + 0.5322240e7 * t30 * t319;
        final double t2246 = -0.60480e5 * t81 * t1928 + 0.120960e6 * t128 * t334 + 0.133056e6 * t21 * t2053 + 0.50400e5
            * t93 * t1928 - 0.798336e6
            * t192 * t2178 - 0.798336e6 * t30 * t2150 - 0.100800e6 * t120 * t1928 - 0.310464e6 * t393 * t1928
            + 0.465696e6 * t115
            * t1928 + 0.1064448e7 * t259 * t429 * t67 - 0.100800e6 * t120 * t1944 + 0.40320e5 * t521 * t25 * ix
            + 0.465696e6 * t115
            * t1944 - 0.310464e6 * t393 * t1944 + 0.201600e6 * t681 * t20 * t1935 - 0.403200e6 * t1363 * t1935
            - 0.620928e6 * t88 * t10
            * t1935 - 0.798336e6 * t192 * t334;
        final double t2253 = 0.5e1 / 0.128e3 * t5 * (t1957 + t1999 + t2041 + t2083 + t2122 + t2164 + t2205 + t2246)
            * t891 * t168;
        final double t2257 = t10 * t51 * t52;
        final double t2262 = t67 * t10;
        final double t2263 = t2262 * t66;
        final double t2267 = 0.672e3 - t1627 - 0.798336e6 * t1628 - 0.1862784e7 * t1630 - 0.20160e5 * t2257
            - 0.1596672e7 * t18 * t67 * t825 - 0.20160e5
            * t2263 + 0.3360e4 * t38 + 0.16800e5 * t48 - t1660 + t1661 + t1663 - t1664 + t1665 - t1667 - t1670
            + t1672;
        final double t2268 = t142 * t1478;
        final double t2281 = -t1674 + t1681 - t1683 - t1692 + t1693 + 0.3725568e7 * t141 * t2268 - 0.1862784e7 * t1637
            * t2268 - 0.3360e4 * t117 + t1695
            - 0.155232e6 * t1698 + 0.232848e6 * t1700 + 0.282240e6 * t1702 + t1705 + 0.620928e6 * t1706
            - 0.252000e6 * t1708
            - 0.776160e6 * t1710 + 0.1164240e7 * t1712 + t1732;
        final double t2287 = t67 * t20 * t7;
        final double t2307 = t131 * t10;
        final double t2317 = -0.50400e5 * t1733 + 0.120960e6 * t1735 + 0.266112e6 * t1737 + 0.161280e6 * t2287
            - 0.322560e6 * t11 * t67 * t7 + 0.96768e5
            * t73 * t10 - 0.266112e6 * t18 * t2262 * t66 - 0.201600e6 * t681 * t60 + 0.177408e6 * t21 * t10
            - 0.532224e6 * t71 * t10
            + 0.177408e6 * t1751 * t10 - 0.443520e6 * t717 * t7 - 0.88200e5 * t2307 * t60 - 0.88200e5 * t138 * t10
            + 0.40320e5 * t304
            - 0.30240e5 * t112 * t10 - 0.1680e4 * t12 * t7;
        final double t2328 = -0.110880e6 * t345 - 0.201600e6 * t660 + 0.1680e4 * t1743 + 0.10080e5 * t1754 + 0.22176e5
            * t1756 + 0.199584e6 * t1764
            + 0.90720e5 * t1766 + 0.8400e4 * t1775 + t1779 - t1780 + 0.40320e5 * t1781 + 0.201600e6 * t1784
            + t1799 + t1801 - t1803
            + t1805 - t1807 - t1809;
        final double t2349 = t75 * t10;
        final double t2370 = -0.80640e5 * t25 * t41 * t44 - 0.310464e6 * t88 * t1381 + 0.100800e6 * t8 * t67 * t98
            - 0.201600e6 * t12 * t1381
            - 0.120960e6 * t67 * t6 * t34 - 0.80640e5 * t743 * ex * t24 - 0.1330560e7 * t585 * t1381 + 0.698544e6
            * t135 * t2349
            - 0.2095632e7 * t138 * t294 - 0.266112e6 * t708 * t53 - 0.443520e6 * t684 * t38 + 0.2661120e7 * t29
            * t1381 + 0.1552320e7
            * t12 * t2349 - 0.4656960e7 * t93 * t294 + 0.1552320e7 * t88 * t298 + 0.25200e5 * t93 * t98
            - 0.77616e5 * t88 * t102;
        final double t2394 = t24 * iy;
        final double t2401 = -0.30240e5 * t81 * t34 + 0.698544e6 * t132 * t298 + 0.352800e6 * t135 * t298 + 0.266112e6
            * t73 * t2263 - 0.8400e4 * t2307
            - 0.20160e5 * t681 - 0.24192e5 * t708 - 0.24192e5 * t294 - 0.2661120e7 * t1675 * t1358 + 0.133056e6
            * t21 * t66 * t10
            + 0.806400e6 * t12 * t298 + 0.161280e6 * t298 * t38 - 0.322560e6 * t1370 * t38 + 0.266112e6 * t22
            * t2394 - 0.80640e5
            * t1841 - 0.403200e6 * t1843 + 0.332640e6 * t1845 - 0.221760e6 * t1847;
        final double t2437 = -0.66528e5 * t1857 + 0.1663200e7 * t1866 - 0.598752e6 * t1884 - 0.1108800e7 * t1886
            + 0.25200e5 * t1892 + t1894
            + 0.126000e6 * t1897 - 0.403200e6 * t12 * t25 * t617 - 0.620928e6 * t88 * t25 * t617 + 0.1064448e7
            * t1632 * t44 * t25
            + 0.1064448e7 * t259 * t24 * t743 + 0.1596672e7 * t868 * t34 * t67 + 0.5322240e7 * t30 * t1358
            + 0.201600e6 * t803 * t20
            * t617 - 0.931392e6 * t1637 * t1355 + 0.1862784e7 * t141 * t1355 + 0.133056e6 * t21 * t825
            - 0.1330560e7 * t585 * t1355;
        final double t2467 = 0.2661120e7 * t29 * t1355 + 0.266112e6 * t73 * t2257 - 0.310464e6 * t88 * t1355
            - 0.201600e6 * t12 * t1355 + 0.100800e6
            * t681 * t95 + 0.1862784e7 * t93 * t2287 - 0.931392e6 * t12 * t102 * t67 - 0.887040e6 * t824 * t617
            - 0.1064448e7 * t2075
            * t718 - 0.1064448e7 * t786 * t685 + 0.322560e6 * t1589 * t617 - 0.645120e6 * t2109 * t617 - 0.60480e5
            * t84 * t2394
            + t1906 - 0.4032e4 * t10 + t1910 - 0.1344e4 * t9 - t1913;

        partialDerivativesJ6[0][0] += -0.35e2 / 0.16e2 * t5 * t155 / t158 / a * t168;
        partialDerivativesJ6[0][2] += t278;
        partialDerivativesJ6[0][3] += t389;
        partialDerivativesJ6[0][4] += t616;
        partialDerivativesJ6[0][5] += t837;
        partialDerivativesJ6[2][0] += t278;
        partialDerivativesJ6[2][2] += -0.5e1 / 0.128e3 * t5 * (t858 + t887) * t891 * t168 - 0.55e2 / 0.64e2 * t5 * t896
            * t274 - 0.715e3 / 0.128e3 * t5
            * t900 * t904 * t6 - t911;
        partialDerivativesJ6[2][3] += t993;
        partialDerivativesJ6[2][4] += t1166;
        partialDerivativesJ6[2][5] += t1350;
        partialDerivativesJ6[3][0] += t389;
        partialDerivativesJ6[3][2] += t993;
        partialDerivativesJ6[3][3] += -0.5e1 / 0.128e3 * t5 * (t1367 + t1393) * t891 * t168 - 0.55e2 / 0.64e2 * t5
            * t983 * t385 - 0.715e3 / 0.128e3 * t5
            * t900 * t904 * t7 - t911;
        partialDerivativesJ6[3][4] += t1475;
        partialDerivativesJ6[3][5] += t1614;
        partialDerivativesJ6[4][0] += t616;
        partialDerivativesJ6[4][2] += t1166;
        partialDerivativesJ6[4][3] += t1475;
        partialDerivativesJ6[4][4] += -0.5e1 / 0.128e3 * t5
            * (t1656 + t1687 + t1723 + t1768 + t1800 + t1839 + t1879 + t1914) * t891 * t168;
        partialDerivativesJ6[4][5] += -t2253;
        partialDerivativesJ6[5][0] += t837;
        partialDerivativesJ6[5][2] += t1350;
        partialDerivativesJ6[5][3] += t1614;
        partialDerivativesJ6[5][4] += -t2253;
        partialDerivativesJ6[5][5] += -0.5e1 / 0.128e3 * t5
            * (t2267 + t2281 + t2317 + t2328 + t2370 + t2401 + t2437 + t2467) * t891 * t168;

        return partialDerivativesJ6;

    }

    /**
     * Compute the partial derivatives due to J7 Potential effect
     * <p>
     * Compute the effect of the partial derivatives due to the 7th degree development of the Zonal Perturbation
     * </p>
     * 
     * @param orbit
     *        the equinoctial orbit
     * 
     * @return partialDerivativesJ7 the J7 partial derivatives
     */
    public double[][] computeJ7PartialDerivatives(final StelaEquinoctialOrbit orbit) {

        /** Orbital elements */
        final double a = orbit.getA();
        final double ex = orbit.getEquinoctialEx();
        final double ey = orbit.getEquinoctialEy();
        final double ix = orbit.getIx();
        final double iy = orbit.getIy();
        final double MU = orbit.getMu();

        /** J2 Potential */
        final double[][] partialDerivativesJ7 = this.computeJ6PartialDerivatives(orbit);

        final double t1 = MU * this.j[7];
        final double t2 = this.rEq * this.rEq;
        final double t4 = t2 * t2;
        final double t5 = t4 * t2 * this.rEq;
        final double t6 = ix * ix;
        final double t7 = iy * iy;
        final double t8 = 0.1e1 - t6 - t7;
        final double t9 = MathLib.sqrt(t8);
        final double t10 = t5 * t9;
        final double t11 = t1 * t10;
        final double t12 = ex * iy;
        final double t13 = ey * ix;
        final double t14 = t12 - t13;
        final double t15 = t8 * t8;
        final double t16 = t6 + t7;
        final double t17 = t15 * t16;
        final double t18 = ex * ex;
        final double t19 = t18 * t6;
        final double t20 = t17 * t19;
        final double t22 = t15 * t8;
        final double t23 = t22 * t16;
        final double t24 = t18 * ex;
        final double t25 = t23 * t24;
        final double t26 = t6 * ix;
        final double t27 = t26 * ey;
        final double t28 = t27 * iy;
        final double t31 = ey * ey;
        final double t32 = t31 * t7;
        final double t33 = t17 * t32;
        final double t35 = t18 * t18;
        final double t36 = t6 * t6;
        final double t37 = t35 * t36;
        final double t40 = t18 + t31;
        final double t41 = t40 * t8;
        final double t46 = t16 * t16;
        final double t47 = t22 * t46;
        final double t48 = t47 * t19;
        final double t50 = t31 * t31;
        final double t51 = t7 * t7;
        final double t52 = t50 * t51;
        final double t55 = t47 * t32;
        final double t57 = t40 * t40;
        final double t59 = 0.200e3 * t18;
        final double t60 = 0.200e3 * t31;
        final double t62 = t13 * iy;
        final double t63 = t47 * ex * t62;
        final double t65 = t23 * ex;
        final double t66 = t31 * ey;
        final double t67 = ix * t66;
        final double t68 = t7 * iy;
        final double t69 = t67 * t68;
        final double t72 = t23 * t18;
        final double t73 = t6 * t31;
        final double t74 = t73 * t7;
        final double t77 = t40 * t15;
        final double t78 = t16 * t18;
        final double t79 = t78 * t6;
        final double t82 = t16 * t31;
        final double t83 = t82 * t7;
        final double t86 = t40 * t22;
        final double t87 = t46 * t31;
        final double t88 = t87 * t7;
        final double t91 = t15 * t24;
        final double t94 = -0.80e2 + 0.26400e5 * t20 + 0.27456e5 * t25 * t28 + 0.26400e5 * t33 + 0.6864e4 * t23 * t37
            - 0.900e3 * t41 * t32 - 0.900e3
            * t41 * t19 - 0.68640e5 * t48 + 0.6864e4 * t23 * t52 - 0.68640e5 * t55 - 0.50e2 * t57 - t59 - t60
            - 0.137280e6 * t63
            + 0.27456e5 * t65 * t69 + 0.41184e5 * t72 * t74 + 0.11088e5 * t77 * t79 + 0.11088e5 * t77 * t83
            - 0.30888e5 * t86 * t88
            - 0.6336e4 * t91 * t28;
        final double t95 = t15 * ex;
        final double t98 = t46 * t18;
        final double t99 = t6 * t98;
        final double t102 = t8 * ex;
        final double t103 = t102 * t62;
        final double t105 = t15 * t18;
        final double t108 = t17 * ex;
        final double t109 = t108 * t62;
        final double t114 = t15 * t46;
        final double t116 = t8 * t16;
        final double t118 = t46 * t16;
        final double t119 = t22 * t118;
        final double t121 = t57 * t22;
        final double t125 = t57 * t8;
        final double t128 = t57 * t15;
        final double t131 = t86 * t118;
        final double t133 = t15 * t35;
        final double t136 = t41 * t16;
        final double t138 = t77 * t46;
        final double t140 = t8 * t18;
        final double t141 = t140 * t6;
        final double t143 = t8 * t31;
        final double t144 = t143 * t7;
        final double t146 = t15 * t50;
        final double t149 = t86 * t46;
        final double t150 = ex * ix;
        final double t151 = ey * iy;
        final double t152 = t150 * t151;
        final double t155 = t77 * t16;
        final double t158 = 0.1575e4 * t125 * t16 - 0.12474e5 * t128 * t46 + 0.102960e6 * t131 - 0.1584e4 * t133 * t36
            + 0.6000e4 * t136 - 0.46200e5
            * t138 - 0.2400e4 * t141 - 0.2400e4 * t144 - 0.1584e4 * t146 * t51 - 0.61776e5 * t149 * t152
            + 0.22176e5 * t155 * t152;
        final double t160 = t94 - 0.6336e4 * t95 * t69 - 0.30888e5 * t86 * t99 - 0.4800e4 * t103 - 0.9504e4 * t105
            * t74 + 0.52800e5 * t109 - 0.1800e4
            * t41 * ex * t62 - 0.15840e5 * t114 + 0.2160e4 * t116 + 0.34320e5 * t119 + 0.28314e5 * t121 * t118
            + t158;
        final double t161 = t14 * t160;
        final double t162 = a * a;
        final double t163 = t162 * t162;
        final double t164 = t163 * t163;
        final double t167 = 0.1e1 - t18 - t31;
        final double t168 = t167 * t167;
        final double t169 = t168 * t168;
        final double t171 = MathLib.sqrt(t167);
        final double t173 = 0.1e1 / t171 / t169 / t168;
        final double t178 = iy * t160;
        final double t180 = 0.1e1 / t164 / a;
        final double t181 = t180 * t173;
        final double t189 = ex * t6;
        final double t192 = t15 * ix;
        final double t193 = t66 * t68;
        final double t198 = t8 * ix;
        final double t205 = t16 * t6;
        final double t208 = t24 * t22;
        final double t209 = t6 * t46;
        final double t212 = t118 * ex;
        final double t215 = t24 * t36;
        final double t218 = t46 * ex;
        final double t221 = t16 * ex;
        final double t228 = t221 * t6;
        final double t231 = t218 * t6;
        final double t234 = 0.82368e5 * t65 * t74 + 0.82368e5 * t72 * t28 + 0.52800e5 * t17 * t189 - 0.6336e4 * t192
            * t193 - 0.1800e4 * t41 * t189
            - 0.4800e4 * t198 * t151 - 0.1800e4 * t102 * t32 - 0.137280e6 * t47 * t189 + 0.22176e5 * t91 * t205
            - 0.61776e5 * t208
            * t209 + 0.113256e6 * t86 * t212 + 0.27456e5 * t23 * t215 - 0.49896e5 * t77 * t218 + 0.6300e4 * t41
            * t221 + 0.27456e5
            * t23 * t69 - 0.1800e4 * t41 * t62 + 0.22176e5 * t77 * t228 - 0.61776e5 * t86 * t231;
        final double t247 = ex * t22;
        final double t250 = t17 * t18;
        final double t253 = t47 * t18;
        final double t265 = t95 * t46;
        final double t267 = t24 * t8;
        final double t274 = t102 * t16;
        final double t276 = 0.22176e5 * t95 * t83 - 0.19008e5 * t95 * t74 - 0.3600e4 * t140 * t62 - 0.137280e6 * t47
            * t62 + 0.52800e5 * t17 * t62
            - 0.19008e5 * t105 * t28 - 0.61776e5 * t247 * t88 + 0.44352e5 * t250 * t62 - 0.123552e6 * t253 * t62
            - 0.400e3 * ex
            - 0.61776e5 * t149 * t62 - 0.200e3 * t40 * ex + 0.22176e5 * t155 * t62 - 0.6336e4 * t91 * t36
            - 0.92400e5 * t265 - 0.1800e4
            * t267 * t6 - 0.4800e4 * t102 * t6 + 0.205920e6 * t247 * t118 + 0.12000e5 * t274;
        final double t277 = t234 + t276;
        final double t278 = t14 * t277;
        final double t285 = 0.1e1 / t171 / t169 / t168 / t167;
        final double t286 = t180 * t285;
        final double t291 = -0.21e2 / 0.16e2 * t11 * t178 * t181 - 0.21e2 / 0.16e2 * t11 * t278 * t181 - 0.273e3
            / 0.16e2 * t11 * t161 * t286 * ex;
        final double t292 = ix * t160;
        final double t296 = t47 * t31;
        final double t297 = t150 * iy;
        final double t304 = t6 * ey;
        final double t305 = t304 * t7;
        final double t308 = ix * t31;
        final double t309 = t308 * t68;
        final double t312 = t17 * t31;
        final double t315 = iy * t26;
        final double t318 = ey * t7;
        final double t321 = t66 * t15;
        final double t322 = t16 * t7;
        final double t325 = t66 * t22;
        final double t326 = t7 * t46;
        final double t331 = ey * t8;
        final double t334 = t66 * t51;
        final double t339 = t46 * ey;
        final double t342 = t16 * ey;
        final double t345 = t118 * ey;
        final double t348 = ix * iy;
        final double t351 = -0.123552e6 * t296 * t297 - 0.61776e5 * t149 * t297 + 0.22176e5 * t155 * t297 + 0.82368e5
            * t72 * t305 + 0.82368e5 * t65
            * t309 + 0.44352e5 * t312 * t297 - 0.6336e4 * t91 * t315 + 0.52800e5 * t17 * t318 + 0.22176e5 * t321
            * t322 - 0.61776e5
            * t325 * t326 - 0.1800e4 * t41 * t318 - 0.1800e4 * t331 * t19 + 0.27456e5 * t23 * t334 - 0.137280e6
            * t47 * t318
            - 0.49896e5 * t77 * t339 + 0.6300e4 * t41 * t342 + 0.113256e6 * t86 * t345 - 0.4800e4 * t102 * t348;
        final double t352 = t342 * t7;
        final double t357 = t339 * t7;
        final double t366 = ey * t22;
        final double t369 = ey * t15;
        final double t374 = t24 * t26;
        final double t385 = t66 * t8;
        final double t396 = 0.22176e5 * t77 * t352 - 0.137280e6 * t47 * t297 - 0.61776e5 * t86 * t357 - 0.19008e5
            * t105 * t305 - 0.3600e4 * t143 * t297
            - 0.19008e5 * t95 * t309 - 0.61776e5 * t366 * t99 + 0.22176e5 * t369 * t79 - 0.1800e4 * t41 * t297
            + 0.27456e5 * t23 * t374
            * iy + 0.52800e5 * t17 * t297 - 0.400e3 * ey - 0.200e3 * t40 * ey - 0.92400e5 * t369 * t46 - 0.1800e4
            * t385 * t7
            - 0.4800e4 * t331 * t7 + 0.12000e5 * t331 * t16 - 0.6336e4 * t321 * t51 + 0.205920e6 * t366 * t118;
        final double t397 = t351 + t396;
        final double t398 = t14 * t397;
        final double t406 = 0.21e2 / 0.16e2 * t11 * t292 * t181 - 0.21e2 / 0.16e2 * t11 * t398 * t181 - 0.273e3
            / 0.16e2 * t11 * t161 * t286 * ey;
        final double t408 = t5 / t9;
        final double t409 = t1 * t408;
        final double t413 = ey * t160;
        final double t416 = t304 * iy;
        final double t419 = t36 * ix;
        final double t423 = t77 * t6;
        final double t424 = ey * ex;
        final double t425 = t424 * iy;
        final double t430 = t189 * t151;
        final double t435 = t308 * t7;
        final double t440 = ix * t16;
        final double t443 = t46 * ix;
        final double t448 = t26 * t18;
        final double t456 = t35 * t26;
        final double t459 = t18 * ix;
        final double t466 = t40 * ix;
        final double t469 = -0.211200e6 * t274 * t416 - 0.41184e5 * t17 * t35 * t419 + 0.44352e5 * t423 * t425
            + 0.22176e5 * t155 * t425 + 0.370656e6
            * t138 * t430 - 0.61776e5 * t149 * t425 + 0.82368e5 * t72 * t435 + 0.82368e5 * t25 * t416 - 0.184800e6
            * t77 * t440
            + 0.184800e6 * t41 * t443 + 0.52800e5 * t192 * t32 - 0.105600e6 * t116 * t448 + 0.9600e4 * t430
            - 0.6336e4 * t95 * t193
            - 0.4800e4 * t102 * t151 + 0.27456e5 * t23 * t456 - 0.137280e6 * t47 * t459 - 0.1800e4 * t41 * t459
            + 0.52800e5 * t17
            * t459 + 0.1800e4 * t466 * t32;
        final double t474 = t6 * t66;
        final double t475 = t474 * t68;
        final double t478 = t26 * t31;
        final double t479 = t478 * t7;
        final double t482 = t17 * t24;
        final double t483 = t36 * ey;
        final double t484 = t483 * iy;
        final double t489 = t22 * ix;
        final double t492 = t86 * t16;
        final double t500 = t8 * t50;
        final double t506 = t118 * ix;
        final double t525 = 0.6336e4 * t500 * t51 * ix + 0.169884e6 * t121 * t443 - 0.169884e6 * t128 * t506
            + 0.617760e6 * t86 * t443 - 0.617760e6
            * t77 * t506 - 0.49896e5 * t128 * t440 + 0.49896e5 * t125 * t443 - 0.88704e5 * t136 * t430 - 0.247104e6
            * t492 * t430
            + 0.823680e6 * t265 * t416 - 0.549120e6 * t65 * t416;
        final double t530 = t78 * t26;
        final double t533 = t98 * t26;
        final double t546 = t22 * t6;
        final double t547 = ex * t66;
        final double t548 = t547 * t68;
        final double t551 = t26 * t22;
        final double t552 = t18 * t31;
        final double t553 = t552 * t7;
        final double t557 = t40 * t6;
        final double t560 = t22 * t36;
        final double t561 = t24 * ey;
        final double t562 = t561 * iy;
        final double t567 = t15 * t6;
        final double t586 = 0.3600e4 * t557 * t425 + 0.54912e5 * t560 * t562 - 0.105600e6 * t116 * t435 + 0.105600e6
            * t567 * t425 - 0.41184e5 * t17
            * t52 * ix - 0.274560e6 * t23 * t435 - 0.1800e4 * t41 * t425 + 0.411840e6 * t114 * t435 + 0.27456e5
            * t23 * t548
            - 0.61776e5 * t86 * t98 * ix - 0.19008e5 * t105 * t435;
        final double t602 = t15 * t118;
        final double t608 = t40 * t26;
        final double t613 = t57 * ix;
        final double t616 = t22 * t419;
        final double t619 = t8 * t35;
        final double t627 = t15 * t26;
        final double t630 = t8 * t46;
        final double t635 = 0.1800e4 * t608 * t18 + 0.3150e4 * t125 * ix - 0.3150e4 * t613 * t16 + 0.13728e5 * t616
            * t35 + 0.6336e4 * t619 * t419
            - 0.12000e5 * t466 * t16 + 0.12000e5 * t41 * ix + 0.4800e4 * t435 + 0.52800e5 * t627 * t18 + 0.63360e5
            * t630 * ix
            - 0.63360e5 * t17 * ix;
        final double t638 = 0.4320e4 * t198 + 0.4800e4 * t448 + 0.411840e6 * t114 * t448 - 0.44352e5 * t136 * t435
            - 0.164736e6 * t108 * t475 + t635
            - 0.4800e4 * t140 * ix - 0.205920e6 * t602 * ix + 0.205920e6 * t47 * ix - 0.4320e4 * t440 + t469 + t525
            + 0.38016e5 * t140
            * t479 - 0.123552e6 * t86 * t530 + 0.185328e6 * t77 * t533 + 0.52800e5 * t17 * t425 - 0.44352e5 * t41
            * t530 + 0.22176e5
            * t77 * t435 + 0.25344e5 * t267 * t484 + 0.25344e5 * t102 * t475 + 0.54912e5 * t546 * t548 + 0.82368e5
            * t551 * t553 + t586
            - 0.19008e5 * t91 * t416 - 0.137280e6 * t47 * t425 - 0.6336e4 * t133 * t26 + 0.22176e5 * t77 * t78 * ix
            - 0.247104e6 * t250
            * t479 - 0.164736e6 * t482 * t484 - 0.274560e6 * t23 * t448 + 0.13728e5 * t489 * t52 - 0.123552e6
            * t492 * t435
            + 0.185328e6 * t138 * t435 + 0.22176e5 * t77 * t448;
        final double t639 = t14 * t638;
        final double t642 = t409 * t161 * t181 * ix + t11 * t413 * t181 - t11 * t639 * t181;
        final double t646 = ex * t160;
        final double t649 = t7 * ex;
        final double t650 = t649 * t13;
        final double t658 = iy * t16;
        final double t661 = t46 * iy;
        final double t664 = t15 * iy;
        final double t669 = t68 * t31;
        final double t672 = t31 * iy;
        final double t683 = t22 * iy;
        final double t688 = t50 * t68;
        final double t693 = t40 * iy;
        final double t696 = t51 * iy;
        final double t697 = t50 * t696;
        final double t700 = -0.88704e5 * t136 * t650 - 0.247104e6 * t492 * t650 + 0.370656e6 * t138 * t650 + 0.9600e4
            * t650 - 0.184800e6 * t77 * t658
            + 0.184800e6 * t41 * t661 + 0.52800e5 * t664 * t19 - 0.4800e4 * t102 * t13 - 0.105600e6 * t116 * t669
            + 0.52800e5 * t17
            * t672 - 0.1800e4 * t41 * t672 - 0.6336e4 * t91 * t27 - 0.274560e6 * t23 * t669 - 0.137280e6 * t47
            * t672 + 0.13728e5
            * t683 * t37 + 0.22176e5 * t77 * t669 + 0.27456e5 * t23 * t688 + 0.411840e6 * t114 * t669 + 0.1800e4
            * t693 * t19
            - 0.41184e5 * t17 * t697;
        final double t701 = t36 * iy;
        final double t706 = t118 * iy;
        final double t718 = t7 * ix * ey;
        final double t723 = t22 * t68;
        final double t724 = t19 * t31;
        final double t728 = t150 * ey;
        final double t731 = t73 * iy;
        final double t736 = t374 * ey;
        final double t741 = t87 * iy;
        final double t744 = t67 * t7;
        final double t747 = t15 * t7;
        final double t750 = iy * t18;
        final double t751 = t750 * t6;
        final double t756 = t82 * iy;
        final double t759 = 0.52800e5 * t17 * t728 - 0.19008e5 * t105 * t731 - 0.1800e4 * t41 * t728 + 0.27456e5 * t23
            * t736 + 0.22176e5 * t155 * t728
            - 0.61776e5 * t86 * t741 - 0.19008e5 * t95 * t744 + 0.105600e6 * t747 * t728 - 0.105600e6 * t116 * t751
            - 0.137280e6 * t47
            * t728 + 0.22176e5 * t77 * t756;
        final double t762 = t82 * t68;
        final double t765 = t27 * t7;
        final double t768 = t87 * t68;
        final double t774 = t51 * t67;
        final double t777 = t73 * t68;
        final double t784 = t22 * t51;
        final double t785 = t150 * t66;
        final double t788 = t40 * t7;
        final double t792 = t22 * t7;
        final double t815 = 0.54912e5 * t792 * t736 + 0.411840e6 * t114 * t751 - 0.274560e6 * t23 * t751 - 0.61776e5
            * t149 * t728 - 0.164736e6 * t108
            * t774 - 0.164736e6 * t482 * t765 - 0.247104e6 * t250 * t777 + 0.82368e5 * t65 * t744 + 0.82368e5 * t72
            * t731 + 0.185328e6
            * t138 * t751 - 0.123552e6 * t492 * t751;
        final double t820 = t8 * iy;
        final double t825 = t77 * t7;
        final double t831 = t15 * t68;
        final double t849 = t40 * t68;
        final double t852 = t22 * t696;
        final double t855 = t57 * iy;
        final double t862 = 0.3150e4 * t125 * iy - 0.12000e5 * t693 * t16 + 0.63360e5 * t630 * iy - 0.6336e4 * t146
            * t68 - 0.205920e6 * t602 * iy
            + 0.205920e6 * t47 * iy + 0.1800e4 * t849 * t31 + 0.13728e5 * t852 * t50 - 0.3150e4 * t855 * t16
            - 0.63360e5 * t17 * iy
            - 0.4800e4 * t143 * iy;
        final double t865 = 0.44352e5 * t825 * t728 + 0.52800e5 * t831 * t31 + 0.6336e4 * t500 * t696 + t815
            - 0.44352e5 * t136 * t751 + 0.4320e4 * t820
            + 0.823680e6 * t265 * t718 + t862 + 0.12000e5 * t41 * iy - 0.4320e4 * t658 + 0.4800e4 * t669
            + 0.169884e6 * t121 * t661
            + t700 + 0.6336e4 * t619 * t701 + 0.617760e6 * t86 * t661 - 0.617760e6 * t77 * t706 - 0.49896e5 * t128
            * t658 + 0.49896e5
            * t125 * t661 - 0.169884e6 * t128 * t706 - 0.211200e6 * t274 * t718 - 0.549120e6 * t65 * t718
            + 0.82368e5 * t723 * t724
            + 0.4800e4 * t751 + t759 - 0.123552e6 * t86 * t762 + 0.25344e5 * t267 * t765 + 0.185328e6 * t77 * t768
            + 0.25344e5 * t102
            * t774 + 0.38016e5 * t140 * t777 + 0.22176e5 * t77 * t751 - 0.44352e5 * t41 * t762 + 0.54912e5 * t784
            * t785 + 0.3600e4
            * t788 * t728 - 0.41184e5 * t17 * t37 * iy;
        final double t866 = t14 * t865;
        final double t869 = t409 * t161 * t181 * iy - t11 * t646 * t181 - t11 * t866 * t181;
        final double t871 = 0.1e1 / t164;
        final double t872 = t871 * t173;
        final double t876 = t871 * t285;
        final double t877 = t876 * ex;
        final double t878 = t178 * t877;
        final double t886 = 0.370656e6 * t63;
        final double t893 = t18 * t36;
        final double t896 = 0.10800e5 * t103;
        final double t897 = 0.133056e6 * t109;
        final double t902 = -0.400e3 + 0.110880e6 * t20 + 0.22176e5 * t33 - 0.308880e6 * t48 - 0.61776e5 * t55
            - 0.600e3 * t18 - t60 - t886 - 0.19008e5
            * t567 * t32 + 0.22176e5 * t77 * t205 - 0.61776e5 * t86 * t209 + 0.82368e5 * t23 * t893 - t896 + t897
            + 0.82368e5 * t23
            * t74 - 0.38016e5 * t95 * t28;
        final double t905 = 0.92400e5 * t114;
        final double t906 = 0.12000e5 * t116;
        final double t907 = 0.205920e6 * t119;
        final double t908 = t8 * t6;
        final double t914 = 0.113256e6 * t131;
        final double t915 = t140 * t16;
        final double t919 = 0.6300e4 * t136;
        final double t920 = 0.49896e5 * t138;
        final double t923 = t18 * t22;
        final double t926 = t105 * t46;
        final double t930 = 0.164736e6 * t65 * t28 - t905 + t906 + t907 - 0.4800e4 * t908 + 0.52800e5 * t6 * t17
            - 0.137280e6 * t47 * t6 + t914
            + 0.12600e5 * t915 - 0.19008e5 * t105 * t36 + t919 - t920 - 0.9000e4 * t141 - 0.1800e4 * t144
            + 0.226512e6 * t923 * t118
            - 0.99792e5 * t926 - 0.1800e4 * t41 * t6;
        final double t939 = t169 * t169;
        final double t941 = 0.1e1 / t171 / t939;
        final double t942 = t871 * t941;
        final double t949 = 0.273e3 / 0.128e3 * t11 * t161 * t876;
        final double t955 = t876 * ey;
        final double t956 = t178 * t955;
        final double t989 = t440 * iy;
        final double t992 = -0.3600e4 * t102 * t318 - 0.3600e4 * t143 * t348 + 0.52800e5 * t17 * t348 + 0.226512e6
            * t366 * t212 - 0.19008e5 * t192
            * t669 - 0.3600e4 * t331 * t189 - 0.99792e5 * t369 * t218 + 0.12600e5 * t331 * t221 - 0.1800e4 * t41
            * t348 - 0.3600e4
            * t140 * t348 - 0.19008e5 * t105 * t315 - 0.137280e6 * t47 * t348 + 0.82368e5 * t23 * t309 + 0.44352e5
            * t105 * t989;
        final double t995 = t661 * ix;
        final double t998 = t448 * iy;
        final double t1005 = t31 * t15;
        final double t1008 = t31 * t22;
        final double t1024 = 0.22176e5 * t77 * t989 - 0.123552e6 * t923 * t995 + 0.82368e5 * t23 * t998 - 0.61776e5
            * t86 * t995 - 0.123552e6 * t366
            * t231 + 0.44352e5 * t1005 * t989 - 0.123552e6 * t1008 * t995 - 0.123552e6 * t247 * t357 + 0.44352e5
            * t95 * t352
            - 0.38016e5 * t95 * t305 + 0.44352e5 * t369 * t228 + 0.164736e6 * t65 * t305 - 0.400e3 * t424
            - 0.4800e4 * t198 * iy;
        final double t1033 = t292 * t877;
        final double t1047 = 0.21e2 / 0.128e3 * t11 * iy * t397 * t872 + 0.273e3 / 0.128e3 * t11 * t956 - 0.21e2
            / 0.128e3 * t11 * ix * t277 * t872
            + 0.21e2 / 0.128e3 * t11 * t14 * (t992 + t1024) * t872 + 0.273e3 / 0.128e3 * t11 * t278 * t955
            - 0.273e3 / 0.128e3 * t11
            * t1033 + 0.273e3 / 0.128e3 * t11 * t398 * t877 + 0.4095e4 / 0.128e3 * t1 * t10 * t14 * t160 * t871
            * t941 * ex * ey;
        final double t1048 = t872 * ix;
        final double t1049 = t178 * t1048;
        final double t1051 = 0.21e2 / 0.128e3 * t409 * t1049;
        final double t1065 = t95 * t440;
        final double t1069 = t24 * t419;
        final double t1072 = t16 * t26;
        final double t1073 = t208 * t1072;
        final double t1080 = t26 * ex;
        final double t1085 = t17 * t151;
        final double t1093 = t19 * t151;
        final double t1097 = t47 * t151;
        final double t1099 = t41 * t150;
        final double t1103 = 0.369600e6 * t102 * t443 - 0.264000e6 * t1065 - 0.12600e5 * t466 * t221 - 0.164736e6 * t17
            * t1069 - 0.137280e6 * t1073
            + 0.370656e6 * t91 * t46 * t26 - 0.88704e5 * t267 * t1072 - 0.549120e6 * t23 * t1080 + 0.823680e6
            * t114 * t1080
            + 0.52800e5 * t1085 + 0.105600e6 * t567 * t151 - 0.211200e6 * t116 * t1080 + 0.3600e4 * t557 * t151
            + 0.7200e4 * t1093
            + 0.44352e5 * t77 * t1080 - 0.137280e6 * t1097 + 0.9000e4 * t1099 - 0.3600e4 * t140 * t151;
        final double t1104 = t41 * t151;
        final double t1106 = t23 * t193;
        final double t1110 = t150 * t32;
        final double t1112 = t247 * t443;
        final double t1122 = t151 * t16;
        final double t1123 = t77 * t1122;
        final double t1125 = t339 * iy;
        final double t1126 = t86 * t1125;
        final double t1130 = t150 * t16;
        final double t1131 = t77 * t1130;
        final double t1133 = t218 * ix;
        final double t1134 = t86 * t1133;
        final double t1136 = t95 * t435;
        final double t1144 = -0.1800e4 * t1104 + 0.27456e5 * t1106 + 0.54912e5 * t546 * t193 + 0.3600e4 * t1110
            + 0.960960e6 * t1112 - 0.1235520e7 * t95
            * t506 + 0.25344e5 * t908 * t193 + 0.44352e5 * t91 * t440 - 0.123552e6 * t208 * t443 + 0.22176e5
            * t1123 - 0.61776e5
            * t1126 + 0.44352e5 * t105 * t1122 - 0.155232e6 * t1131 + 0.555984e6 * t1134 + 0.6336e4 * t1136
            - 0.123552e6 * t923 * t1125
            - 0.211200e6 * t116 * t416 + 0.199584e6 * t41 * t1133;
        final double t1151 = t105 * t416;
        final double t1159 = t221 * t26;
        final double t1180 = t65 * t435;
        final double t1182 = t72 * t416;
        final double t1184 = t915 * t416;
        final double t1186 = t926 * t416;
        final double t1188 = -0.679536e6 * t77 * t212 * ix + 0.76032e5 * t140 * t484 + 0.31680e5 * t1151 + 0.823680e6
            * t114 * t416 - 0.549120e6 * t23
            * t416 + 0.76032e5 * t102 * t479 - 0.247104e6 * t86 * t1159 - 0.88704e5 * t41 * t1159 + 0.370656e6
            * t77 * t218 * t26
            + 0.44352e5 * t77 * t416 - 0.164736e6 * t17 * t475 + 0.164736e6 * t560 * t18 * ey * iy + 0.164736e6
            * t551 * ex * t31 * t7
            + 0.3600e4 * t374 - 0.82368e5 * t1180 - 0.247104e6 * t1182 - 0.177408e6 * t1184 + 0.741312e6 * t1186;
        final double t1189 = t274 * t435;
        final double t1191 = t265 * t435;
        final double t1204 = t331 * iy;
        final double t1214 = t91 * t26;
        final double t1216 = t102 * ix;
        final double t1220 = t321 * t68;
        final double t1224 = -0.88704e5 * t1189 + 0.370656e6 * t1191 + 0.370656e6 * t138 * t416 - 0.247104e6 * t492
            * t416 - 0.88704e5 * t136 * t416
            - 0.494208e6 * t108 * t479 - 0.494208e6 * t250 * t484 + 0.9600e4 * t1080 - 0.4800e4 * t1204
            + 0.105600e6 * t627 * ex
            + 0.54912e5 * t616 * t24 - 0.24000e5 * t1130 + 0.9600e4 * t416 - 0.3600e4 * t267 * ix + 0.19008e5
            * t1214 + 0.14400e5
            * t1216 + 0.3600e4 * t608 * ex - 0.6336e4 * t1220 + 0.25344e5 * t267 * t419;
        final double t1232 = t1 * t408 * t14;
        final double t1237 = 0.273e3 / 0.128e3 * t11 * t413 * t877;
        final double t1241 = -t1051 + 0.21e2 / 0.128e3 * t11 * iy * t638 * t872 - 0.21e2 / 0.128e3 * t409 * t278
            * t1048 - 0.21e2 / 0.128e3 * t11 * ey
            * t277 * t872 + 0.21e2 / 0.128e3 * t11 * t14 * (t1103 + t1144 + t1188 + t1224) * t872 - 0.273e3
            / 0.128e3 * t1232 * t1033
            - t1237 + 0.273e3 / 0.128e3 * t11 * t639 * t877;
        final double t1250 = 0.21e2 / 0.128e3 * t1 * t5 * t9 * t160 * t872;
        final double t1255 = t872 * iy;
        final double t1264 = 0.105600e6 * t664 * t189;
        final double t1265 = t95 * t658;
        final double t1270 = 0.105600e6 * t747 * t13;
        final double t1272 = 0.25344e5 * t267 * t701;
        final double t1274 = 0.3600e4 * t788 * t13;
        final double t1275 = t17 * t13;
        final double t1278 = t91 * iy * t6;
        final double t1280 = t247 * t762;
        final double t1287 = 0.76032e5 * t102 * t777;
        final double t1288 = t105 * t718;
        final double t1291 = 0.823680e6 * t114 * t718;
        final double t1293 = 0.549120e6 * t23 * t718;
        final double t1294 = t12 * t6;
        final double t1296 = 0.44352e5 * t77 * t1294;
        final double t1298 = 0.44352e5 * t77 * t718;
        final double t1300 = 0.164736e6 * t17 * t774;
        final double t1301 = t27 * t18;
        final double t1303 = 0.164736e6 * t792 * t1301;
        final double t1304 = t1264 - 0.369600e6 * t1265 + 0.369600e6 * t102 * t661 + t1270 + t1272 + t1274 + 0.52800e5
            * t1275 + 0.44352e5 * t1278
            - 0.247104e6 * t1280 + 0.370656e6 * t95 * t768 - 0.88704e5 * t102 * t762 + t1287 + 0.88704e5 * t1288
            + t1291 - t1293
            + t1296 + t1298 - t1300 + t1303;
        final double t1305 = t13 * t16;
        final double t1306 = t77 * t1305;
        final double t1308 = t23 * t744;
        final double t1310 = t23 * t1301;
        final double t1312 = t443 * ey;
        final double t1313 = t86 * t1312;
        final double t1315 = t95 * t731;
        final double t1318 = 0.123552e6 * t923 * t1312;
        final double t1320 = 0.44352e5 * t105 * t1305;
        final double t1322 = 0.123552e6 * t247 * t741;
        final double t1324 = 0.44352e5 * t95 * t756;
        final double t1326 = 0.211200e6 * t116 * t718;
        final double t1328 = 0.211200e6 * t116 * t1294;
        final double t1331 = 0.164736e6 * t723 * t189 * t31;
        final double t1333 = 0.76032e5 * t140 * t765;
        final double t1335 = 0.549120e6 * t23 * t1294;
        final double t1337 = 0.823680e6 * t114 * t1294;
        final double t1338 = t205 * iy;
        final double t1346 = 0.164736e6 * t17 * t215 * iy;
        final double t1350 = 0.22176e5 * t1306 + 0.82368e5 * t1308 + 0.82368e5 * t1310 - 0.61776e5 * t1313 - 0.38016e5
            * t1315 - t1318 + t1320 - t1322
            + t1324 - t1326 - t1328 + t1331 + t1333 - t1335 + t1337 - 0.88704e5 * t267 * t1338 + 0.370656e6 * t91
            * t209 * iy - t1346
            - 0.679536e6 * t77 * t212 * iy;
        final double t1352 = t218 * iy;
        final double t1353 = t86 * t1352;
        final double t1355 = t208 * t1338;
        final double t1357 = t12 * t16;
        final double t1358 = t77 * t1357;
        final double t1363 = 0.54912e5 * t683 * t215;
        final double t1366 = t41 * t12;
        final double t1368 = t247 * t661;
        final double t1373 = 0.3600e4 * t693 * t189;
        final double t1378 = 0.25344e5 * t198 * t334;
        final double t1379 = t95 * t669;
        final double t1381 = t47 * t13;
        final double t1383 = t41 * t13;
        final double t1386 = 0.54912e5 * t784 * t67;
        final double t1387 = t66 * t7;
        final double t1388 = t192 * t1387;
        final double t1391 = 0.3600e4 * t140 * t13;
        final double t1392 = t105 * t27;
        final double t1394 = 0.679536e6 * t1353 - 0.247104e6 * t1355 - 0.199584e6 * t1358 + 0.199584e6 * t41 * t1352
            + t1363 - 0.12600e5 * t693 * t221
            + 0.12600e5 * t1366 + 0.1235520e7 * t1368 - 0.1235520e7 * t95 * t706 + t1373 + 0.7200e4 * t18 * t7
            * t13 + t1378
            + 0.44352e5 * t1379 - 0.137280e6 * t1381 - 0.1800e4 * t1383 + t1386 - 0.19008e5 * t1388 - t1391
            - 0.19008e5 * t1392;
        final double t1396 = 0.3600e4 * t102 * t672;
        final double t1397 = t65 * t731;
        final double t1401 = t72 * t718;
        final double t1404 = 0.370656e6 * t138 * t1294;
        final double t1406 = 0.247104e6 * t492 * t1294;
        final double t1408 = 0.88704e5 * t136 * t1294;
        final double t1410 = 0.370656e6 * t138 * t718;
        final double t1412 = 0.247104e6 * t492 * t718;
        final double t1414 = 0.494208e6 * t108 * t777;
        final double t1416 = 0.494208e6 * t250 * t765;
        final double t1418 = 0.88704e5 * t136 * t718;
        final double t1421 = t102 * iy;
        final double t1423 = 0.9600e4 * t1294;
        final double t1428 = t198 * ey;
        final double t1433 = 0.9600e4 * t718;
        final double t1434 = -t1396 + 0.164736e6 * t1397 + 0.741312e6 * t926 * t718 - 0.494208e6 * t1401 + t1404
            - t1406 - t1408 + t1410 - t1412 - t1414
            - t1416 - t1418 - 0.177408e6 * t915 * t718 + 0.24000e5 * t1421 + t1423 - 0.24000e5 * t1357 + 0.3600e4
            * ex * t68 * t31
            - 0.4800e4 * t1428 + 0.3600e4 * t24 * iy * t6 + t1433;
        final double t1450 = -0.21e2 / 0.128e3 * t409 * t7 * t160 * t872 + t1250 + 0.21e2 / 0.128e3 * t11 * iy * t865
            * t872 - 0.21e2 / 0.128e3 * t409
            * t278 * t1255 + 0.21e2 / 0.128e3 * t11 * ex * t277 * t872 + 0.21e2 / 0.128e3 * t11 * t14
            * (t1304 + t1350 + t1394 + t1434)
            * t872 - 0.273e3 / 0.128e3 * t1232 * t878 + 0.273e3 / 0.128e3 * t11 * t18 * t160 * t876 + 0.273e3
            / 0.128e3 * t11 * t866
            * t877;
        final double t1455 = t292 * t955;
        final double t1463 = t19 * t7;
        final double t1466 = t13 * t68;
        final double t1471 = -0.400e3 + 0.22176e5 * t20 + 0.110880e6 * t33 - 0.61776e5 * t48 - 0.308880e6 * t55 - t59
            - 0.600e3 * t31 - t886 - t896
            + t897 + 0.82368e5 * t23 * t1463 - 0.38016e5 * t95 * t1466 + 0.164736e6 * t65 * t1466 - t905 + t906
            + t907;
        final double t1472 = t8 * t7;
        final double t1474 = t31 * t51;
        final double t1486 = t41 * t7;
        final double t1488 = t143 * t16;
        final double t1498 = t1005 * t46;
        final double t1500 = -0.4800e4 * t1472 + 0.82368e5 * t23 * t1474 - 0.19008e5 * t105 * t6 * t7 - 0.61776e5 * t86
            * t326 + 0.22176e5 * t77 * t322
            - 0.19008e5 * t1005 * t51 - 0.1800e4 * t1486 + 0.12600e5 * t1488 + t914 + t919 - t920 - 0.1800e4 * t141
            - 0.9000e4 * t144
            + 0.52800e5 * t7 * t17 + 0.226512e6 * t1008 * t118 - 0.137280e6 * t47 * t7 - 0.99792e5 * t1498;
        final double t1537 = t1264 + 0.52800e5 * t1265 + t1270 + t1272 + t1274 - 0.369600e6 * t1275 - 0.19008e5 * t1278
            + 0.82368e5 * t1280 + t1287
            - 0.38016e5 * t1288 + t1291 - t1293 + t1296 + t1298 - t1300 + t1303 - 0.199584e6 * t1306 - 0.247104e6
            * t1308 - 0.247104e6
            * t1310;
        final double t1547 = 0.679536e6 * t1313 + 0.88704e5 * t1315 - t1318 + t1320 - t1322 + t1324 - t1326 - t1328
            + t1331 + t1333 - t1335 + t1337
            - t1346 - 0.61776e5 * t1353 + 0.82368e5 * t1355 + 0.22176e5 * t1358 + t1363 - 0.1800e4 * t1366
            - 0.88704e5 * t385 * t322
            * ix;
        final double t1569 = 0.199584e6 * t41 * t1312 - 0.679536e6 * t77 * t345 * ix - 0.137280e6 * t1368 - 0.88704e5
            * t331 * t530 + 0.370656e6 * t369
            * t533 + 0.370656e6 * t321 * t326 * ix + t1373 + t1378 - 0.19008e5 * t1379 + 0.1235520e7 * t1381
            + 0.12600e5 * t1383
            + t1386 + 0.44352e5 * t1388 - t1391 + 0.44352e5 * t1392 - t1396 - 0.494208e6 * t1397 + 0.164736e6
            * t1401 + t1404;
        final double t1587 = -t1406 - t1408 + t1410 - t1412 - t1414 - t1416 - t1418 - 0.177408e6 * t1488 * t1294
            + 0.741312e6 * t1498 * t1294
            + 0.369600e6 * t331 * t443 - 0.4800e4 * t1421 + 0.3600e4 * t744 + t1423 + 0.24000e5 * t1428 + 0.3600e4
            * t1301 + t1433
            - 0.24000e5 * t1305 - 0.12600e5 * t466 * t342 + 0.7200e4 * t73 * t12 - 0.1235520e7 * t369 * t506;
        final double t1603 = 0.21e2 / 0.128e3 * t409 * t6 * t160 * t872 - t1250 - 0.21e2 / 0.128e3 * t11 * ix * t638
            * t872 - 0.21e2 / 0.128e3 * t409
            * t398 * t1048 - 0.21e2 / 0.128e3 * t11 * ey * t397 * t872 + 0.21e2 / 0.128e3 * t11 * t14
            * (t1537 + t1547 + t1569 + t1587)
            * t872 - 0.273e3 / 0.128e3 * t1232 * t1455 - 0.273e3 / 0.128e3 * t11 * t31 * t160 * t876 + 0.273e3
            / 0.128e3 * t11 * t639
            * t955;
        final double t1619 = t68 * ey;
        final double t1642 = t16 * t68;
        final double t1655 = 0.369600e6 * t331 * t661 + 0.105600e6 * t747 * t150 - 0.211200e6 * t116 * t1619
            + 0.44352e5 * t77 * t1619 + 0.54912e5
            * t792 * t374 + 0.3600e4 * t788 * t150 - 0.164736e6 * t17 * t66 * t696 - 0.12600e5 * t693 * t342
            + 0.823680e6 * t114
            * t1619 - 0.549120e6 * t23 * t1619 - 0.1235520e7 * t369 * t706 + 0.370656e6 * t321 * t46 * t68
            - 0.88704e5 * t385 * t1642
            + 0.25344e5 * t267 * t26 * t7 - 0.123552e6 * t325 * t661 + 0.44352e5 * t321 * t658 - 0.3600e4 * t143
            * t150 + 0.52800e5
            * t1065;
        final double t1671 = t649 * ix;
        final double t1678 = 0.27456e5 * t1073 - 0.264000e6 * t1085 + 0.3600e4 * t1093 + 0.960960e6 * t1097 - 0.1800e4
            * t1099 + 0.9000e4 * t1104
            - 0.137280e6 * t1106 + 0.7200e4 * t1110 - 0.137280e6 * t1112 - 0.155232e6 * t1123 + 0.555984e6 * t1126
            + 0.22176e5 * t1131
            - 0.61776e5 * t1134 + 0.31680e5 * t1136 + 0.6336e4 * t1151 - 0.211200e6 * t116 * t1671 - 0.123552e6
            * t1008 * t1133
            + 0.44352e5 * t1005 * t1130;
        final double t1698 = t308 * t51;
        final double t1701 = t304 * t68;
        final double t1716 = -0.679536e6 * t77 * t345 * iy + 0.164736e6 * t723 * t19 * ey - 0.164736e6 * t17 * t374
            * t7 + 0.164736e6 * t784 * t150
            * t31 - 0.247104e6 * t1180 - 0.82368e5 * t1182 - 0.88704e5 * t1184 + 0.370656e6 * t1186 - 0.177408e6
            * t1189 + 0.741312e6
            * t1191 - 0.494208e6 * t108 * t1698 - 0.494208e6 * t250 * t1701 - 0.88704e5 * t136 * t1671 + 0.370656e6
            * t138 * t1671
            - 0.247104e6 * t492 * t1671 + 0.199584e6 * t41 * t1125 + 0.76032e5 * t140 * t1701 + 0.44352e5 * t77
            * t1671;
        final double t1726 = t342 * t68;
        final double t1749 = 0.76032e5 * t102 * t1698 + 0.370656e6 * t77 * t339 * t68 - 0.549120e6 * t23 * t1671
            + 0.823680e6 * t114 * t1671 - 0.88704e5
            * t41 * t1726 - 0.247104e6 * t86 * t1726 + 0.9600e4 * t1619 + 0.3600e4 * t193 + 0.14400e5 * t1204
            + 0.9600e4 * t1671
            + 0.54912e5 * t852 * t66 - 0.24000e5 * t1122 - 0.6336e4 * t1214 + 0.105600e6 * t831 * ey - 0.4800e4
            * t1216 + 0.19008e5
            * t1220 - 0.3600e4 * t385 * iy + 0.3600e4 * t849 * ey + 0.25344e5 * t385 * t696;
        final double t1761 = t1051 - 0.21e2 / 0.128e3 * t11 * ix * t865 * t872 - 0.21e2 / 0.128e3 * t409 * t398 * t1255
            + 0.21e2 / 0.128e3 * t11 * ex
            * t397 * t872 + 0.21e2 / 0.128e3 * t11 * t14 * (t1655 + t1678 + t1716 + t1749) * t872 - 0.273e3
            / 0.128e3 * t1232 * t956
            + t1237 + 0.273e3 / 0.128e3 * t11 * t866 * t955;
        final double t1764 = t5 / t9 / t8;
        final double t1765 = t1 * t1764;
        final double t1778 = 0.21e2 / 0.128e3 * t409 * t161 * t872;
        final double t1793 = t77 * t19;
        final double t1795 = 0.28800e5 * t152;
        final double t1800 = t114 * t32;
        final double t1806 = t118 * t6;
        final double t1813 = 0.4320e4 - 0.25920e5 * t6 - 0.8640e4 * t7 + 0.22176e5 * t77 * t78 + 0.1478400e7 * t41
            * t205 + 0.211200e6 * t205 * t32
            - 0.422400e6 * t908 * t32 + 0.110880e6 * t1793 + t1795 - 0.19008e5 * t105 * t32 - 0.61776e5 * t86 * t98
            + 0.411840e6
            * t1800 - 0.2038608e7 * t128 * t209 + 0.679536e6 * t121 * t205 + 0.2471040e7 * t41 * t1806
            - 0.7413120e7 * t77 * t209
            + 0.2471040e7 * t86 * t205;
        final double t1818 = t17 * t52;
        final double t1820 = t23 * t32;
        final double t1822 = t36 * t6;
        final double t1823 = t1822 * t35;
        final double t1827 = 0.741312e6 * t492 * t152;
        final double t1828 = t1080 * t151;
        final double t1832 = 0.266112e6 * t136 * t152;
        final double t1834 = 0.1111968e7 * t138 * t152;
        final double t1835 = t41 * t46;
        final double t1838 = t114 * t19;
        final double t1840 = t23 * t19;
        final double t1850 = t17 * t37;
        final double t1854 = 0.679536e6 * t125 * t1806 + 0.399168e6 * t125 * t205 - 0.41184e5 * t1818 - 0.274560e6
            * t1820 + 0.164736e6 * t116 * t1823
            - t1827 + 0.2965248e7 * t155 * t1828 - t1832 + t1834 - 0.1482624e7 * t1835 * t1828 + 0.2059200e7
            * t1838 - 0.1372800e7
            * t1840 - 0.164736e6 * t567 * t52 - 0.549120e6 * t546 * t32 - 0.1647360e7 * t630 * t893 + 0.3294720e7
            * t17 * t893
            - 0.370656e6 * t1850 - 0.50688e5 * t1069 * t151;
        final double t1870 = t116 * t19;
        final double t1872 = t116 * t32;
        final double t1874 = t77 * t32;
        final double t1877 = 0.2471040e7 * t265 * t62;
        final double t1879 = 0.1647360e7 * t65 * t62;
        final double t1880 = t630 * ex;
        final double t1897 = 0.88704e5 * t40 * t36 * t78 - 0.177408e6 * t41 * t893 - 0.247104e6 * t86 * t893
            - 0.76032e5 * t893 * t32 - 0.50688e5
            * t1080 * t193 + 0.82368e5 * t23 * t35 * t6 - 0.528000e6 * t1870 - 0.105600e6 * t1872 + 0.22176e5
            * t1874 + t1877 - t1879
            - 0.3294720e7 * t1880 * t28 + 0.6589440e7 * t108 * t28 - 0.741312e6 * t1835 * t74 + 0.1482624e7 * t155
            * t74 + 0.658944e6
            * t274 * t26 * t66 * t68 + 0.988416e6 * t915 * t36 * t31 * t7;
        final double t1898 = t108 * t69;
        final double t1900 = t116 * t24;
        final double t1915 = 0.1235520e7 * t250 * t74;
        final double t1916 = t482 * t28;
        final double t1921 = 0.633600e6 * t274 * t62;
        final double t1924 = 0.133056e6 * t77 * ix * t425;
        final double t1926 = 0.316800e6 * t192 * t425;
        final double t1931 = 0.12000e5 * t40 * t16;
        final double t1932 = 0.12000e5 * t41;
        final double t1934 = 0.63360e5 * t630;
        final double t1935 = -0.494208e6 * t1898 + 0.658944e6 * t1900 * t419 * ey * iy + 0.177408e6 * t608 * t16 * t425
            - 0.354816e6 * t41 * t26 * t425
            - 0.494208e6 * t86 * t26 * t425 - t1915 - 0.1153152e7 * t1916 + 0.164736e6 * t25 * t62 - t1921 + t1924
            + t1926 - 0.48000e5
            * t557 + 0.24000e5 * t19 + 0.4800e4 * t32 - t1931 + t1932 - 0.4800e4 * t140 + t1934;
        final double t1938 = 0.63360e5 * t17;
        final double t1939 = 0.3150e4 * t125;
        final double t1941 = 0.3150e4 * t57 * t16;
        final double t1942 = t57 * t6;
        final double t1944 = 0.205920e6 * t47;
        final double t1945 = 0.205920e6 * t602;
        final double t1952 = t36 * t16 * t18;
        final double t1962 = t15 * t36;
        final double t1965 = t77 * t88;
        final double t1967 = t41 * t83;
        final double t1969 = -t1938 + t1939 - t1941 - 0.12600e5 * t1942 + t1944 - t1945 - 0.126720e6 * t567
            - 0.126720e6 * t209 - 0.12672e5 * t1823
            - 0.177408e6 * t41 * t74 + 0.1482624e7 * t77 * t1952 - 0.741312e6 * t41 * t98 * t36 - 0.658944e6
            * t627 * t548 - 0.38016e5
            * t91 * t62 - 0.988416e6 * t1962 * t553 + 0.185328e6 * t1965 - 0.44352e5 * t1967;
        final double t1970 = t86 * t83;
        final double t1972 = t489 * t548;
        final double t1974 = t77 * t99;
        final double t1976 = t102 * t69;
        final double t1979 = 0.190080e6 * t140 * t74;
        final double t1980 = t86 * t79;
        final double t1982 = t551 * t562;
        final double t1985 = 0.411840e6 * t546 * t553;
        final double t1989 = 0.10800e5 * t466 * t425;
        final double t1992 = t41 * t79;
        final double t1994 = t267 * t28;
        final double t2000 = 0.184800e6 * t1835;
        final double t2001 = 0.184800e6 * t155;
        final double t2002 = t567 * t18;
        final double t2004 = -0.123552e6 * t1970 + 0.164736e6 * t1972 + 0.926640e6 * t1974 + 0.76032e5 * t1976 + t1979
            - 0.617760e6 * t1980 + 0.384384e6
            * t1982 + t1985 + 0.82368e5 * t23 * t553 + t1989 + 0.88704e5 * t557 * t83 - 0.221760e6 * t1992
            + 0.177408e6 * t1994
            - 0.1647360e7 * t630 * t74 - 0.99792e5 * t1942 * t46 + t2000 - t2001 + 0.264000e6 * t2002;
        final double t2007 = t1005 * t7;
        final double t2023 = t50 * t6 * t51;
        final double t2026 = 0.169884e6 * t121 * t46;
        final double t2027 = 0.617760e6 * t149;
        final double t2029 = 0.169884e6 * t128 * t118;
        final double t2031 = 0.49896e5 * t125 * t46;
        final double t2033 = 0.49896e5 * t128 * t16;
        final double t2035 = 0.617760e6 * t118 * t77;
        final double t2041 = 0.52800e5 * t250 + 0.52800e5 * t2007 - 0.99792e5 * t128 * t6 - 0.549120e6 * t560 * t18
            - 0.369600e6 * t557 * t46
            - 0.369600e6 * t423 + 0.211200e6 * t1952 - 0.422400e6 * t8 * t36 * t18 + 0.506880e6 * t116 * t6
            - 0.12672e5 * t2023 + t2026
            + t2027 - t2029 + t2031 - t2033 - t2035 - 0.164736e6 * t15 * t1822 * t35 - 0.19008e5 * t133 * t6;
        final double t2043 = t22 * t50 * t51;
        final double t2045 = t500 * t51;
        final double t2051 = t40 * t31 * t7;
        final double t2057 = t8 * t118;
        final double t2060 = t619 * t36;
        final double t2062 = t560 * t35;
        final double t2064 = t557 * t18;
        final double t2074 = t15 * t419;
        final double t2082 = 0.13728e5 * t2043 + 0.6336e4 * t2045 + 0.823680e6 * t23 * t6 - 0.137280e6 * t253
            + 0.1800e4 * t2051 - 0.1800e4 * t41 * t18
            - 0.2471040e7 * t114 * t6 + 0.823680e6 * t2057 * t6 + 0.57024e5 * t2060 + 0.123552e6 * t2062
            + 0.9000e4 * t2064
            + 0.164736e6 * t116 * t2023 + 0.3294720e7 * t17 * t74 - 0.1098240e7 * t551 * t425 - 0.247104e6 * t86
            * t74 - 0.658944e6
            * t2074 * t562 - 0.844800e6 * t8 * t26 * t425 + 0.422400e6 * t1072 * t425;
        final double t2108 = t189 * ey;
        final double t2115 = t308 * iy;
        final double t2118 = t478 * iy;
        final double t2121 = t474 * t7;
        final double t2147 = t750 * ix;
        final double t2150 = -0.88704e5 * t136 * t2108 - 0.247104e6 * t492 * t2108 + 0.370656e6 * t138 * t2108
            + 0.164736e6 * t72 * t2115 - 0.494208e6
            * t250 * t2118 - 0.494208e6 * t108 * t2121 - 0.88704e5 * t136 * t2115 - 0.247104e6 * t492 * t2115
            + 0.370656e6 * t138
            * t2115 + 0.658944e6 * t274 * t474 * t51 + 0.988416e6 * t915 * t478 * t68 - 0.3294720e7 * t1880 * t305
            + 0.6589440e7 * t108
            * t305 + 0.658944e6 * t1900 * t483 * t7 - 0.741312e6 * t1835 * t309 + 0.1482624e7 * t155 * t309
            - 0.88704e5 * t136 * t2147;
        final double t2156 = t189 * t318;
        final double t2181 = t8 * t68;
        final double t2188 = -0.741312e6 * t1835 * t998 + 0.1482624e7 * t155 * t998 - 0.17280e5 * t348 - 0.1482624e7
            * t1835 * t2156 + 0.2965248e7
            * t155 * t2156 + 0.370656e6 * t138 * t2147 - 0.247104e6 * t492 * t2147 - 0.494208e6 * t482 * t305
            - 0.369600e6 * t693
            * t443 - 0.369600e6 * t77 * t348 + 0.211200e6 * t658 * t448 - 0.422400e6 * t820 * t448 + 0.105600e6
            * t664 * t459
            + 0.105600e6 * t747 * t424 + 0.211200e6 * t1642 * t308 - 0.422400e6 * t2181 * t308 + 0.506880e6 * t116
            * t348 - 0.137280e6
            * t47 * t424;
        final double t2210 = t15 * t696;
        final double t2231 = -0.19008e5 * t91 * t304 + 0.3600e4 * t557 * t424 + 0.54912e5 * t560 * t561 - 0.19008e5
            * t95 * t1387 - 0.99792e5 * t855
            * t443 - 0.99792e5 * t128 * t348 - 0.1800e4 * t41 * t424 + 0.25344e5 * t619 * t315 - 0.549120e6 * t683
            * t448 + 0.3600e4
            * t693 * t459 - 0.164736e6 * t2210 * t50 * ix - 0.549120e6 * t723 * t308 + 0.3600e4 * t788 * t424
            + 0.54912e5 * t784 * t547
            + 0.3600e4 * t466 * t672 - 0.50688e5 * t7 * t24 * t483 - 0.76032e5 * t68 * t18 * t478 - 0.50688e5 * t51
            * ex * t474;
        final double t2238 = t35 * iy;
        final double t2264 = t649 * ey;
        final double t2271 = 0.25344e5 * t102 * t334 + 0.54912e5 * t683 * t456 + 0.823680e6 * t23 * t348 - 0.164736e6
            * t2074 * t2238 + 0.823680e6
            * t2057 * t348 - 0.2471040e7 * t114 * t348 + 0.25344e5 * t267 * t483 + 0.105600e6 * t567 * t424
            + 0.52800e5 * t17 * t424
            + 0.105600e6 * t192 * t672 + 0.25344e5 * t500 * t68 * ix + 0.54912e5 * t489 * t688 + 0.3294720e7 * t309
            * t17 + 0.823680e6
            * t114 * t2147 - 0.549120e6 * t23 * t2147 + 0.823680e6 * t114 * t2264 - 0.549120e6 * t23 * t2264
            - 0.1098240e7 * t792
            * t2108;
        final double t2276 = t506 * iy;
        final double t2294 = t547 * t51;
        final double t2314 = -0.247104e6 * t86 * t309 + 0.679536e6 * t125 * t2276 - 0.2038608e7 * t128 * t995
            + 0.679536e6 * t121 * t989 + 0.76032e5
            * t267 * t305 - 0.164736e6 * t17 * t456 * iy - 0.988416e6 * t627 * t552 * t68 - 0.658944e6 * t1962
            * t561 * t7 - 0.658944e6
            * t567 * t2294 - 0.164736e6 * t17 * t2294 + 0.399168e6 * t125 * t989 + 0.2471040e7 * t41 * t2276
            - 0.7413120e7 * t77 * t995
            + 0.2471040e7 * t86 * t989 + 0.22176e5 * t77 * t221 * ey - 0.247104e6 * t86 * t998 - 0.1647360e7 * t630
            * t309;
        final double t2329 = t24 * t6 * ey;
        final double t2335 = t2238 * t419;
        final double t2346 = t547 * t7;
        final double t2358 = -0.177408e6 * t41 * t998 - 0.177408e6 * t41 * t309 + 0.88704e5 * t693 * t530 + 0.44352e5
            * t77 * t2147 + 0.44352e5 * t77
            * t2264 + 0.164736e6 * t723 * t459 * t31 + 0.164736e6 * t792 * t2329 + 0.88704e5 * t849 * t82 * ix
            + 0.164736e6 * t116
            * t2335 - 0.211200e6 * t116 * t2264 + 0.3294720e7 * t17 * t998 - 0.549120e6 * t23 * t2108 - 0.1647360e7
            * t630 * t998
            + 0.82368e5 * t23 * t2346 + 0.823680e6 * t114 * t2108 + 0.823680e6 * t114 * t2115 - 0.164736e6 * t17
            * t215 * ey
            - 0.549120e6 * t23 * t2115;
        final double t2399 = 0.44352e5 * t77 * t2108 - 0.164736e6 * t17 * t688 * ix + 0.164736e6 * t551 * t552 * iy
            + 0.76032e5 * t102 * t2121
            + 0.164736e6 * t546 * t2346 + 0.1478400e7 * t41 * t989 - 0.211200e6 * t116 * t2147 + 0.422400e6 * t322
            * t2108 - 0.844800e6
            * t1472 * t2108 + 0.76032e5 * t140 * t2118 + 0.76032e5 * t140 * t309 - 0.38016e5 * t105 * t2115
            + 0.44352e5 * t77 * t2115
            + 0.82368e5 * t23 * t2329 - 0.494208e6 * t250 * t309 + 0.370656e6 * t138 * t2264 - 0.247104e6 * t492
            * t2264 + 0.177408e6
            * t788 * t16 * t2108;
        final double t2407 = t697 * ix;
        final double t2432 = -0.354816e6 * t1486 * t2108 - 0.494208e6 * t86 * t7 * t2108 - 0.88704e5 * t136 * t2264
            - 0.12672e5 * t2407 + 0.9600e4
            * t2108 - 0.4800e4 * t102 * ey - 0.12672e5 * t2335 - 0.12600e5 * t613 * iy - 0.48000e5 * t466 * iy
            + 0.9600e4 * t2147
            + 0.9600e4 * t2264 + 0.9600e4 * t2115 - 0.126720e6 * t995 - 0.126720e6 * t664 * ix - 0.61776e5 * t86
            * t218 * ey
            - 0.211200e6 * t116 * t2115 - 0.211200e6 * t116 * t2108 + 0.164736e6 * t116 * t2407;
        final double t2439 = -t1 * t1764 * t14 * t1049 - t409 * t646 * t1048 - t409 * t866 * t1048 + t409 * t413
            * t1255 - t11 * ey * t865 * t872 - t409
            * t639 * t1255 + t11 * ex * t638 * t872 + t11 * t14
            * (t2150 + t2188 + t2231 + t2271 + t2314 + t2358 + t2399 + t2432)
            * t872;
        final double t2462 = t150 * t1619;
        final double t2474 = 0.4320e4 - 0.8640e4 * t6 - 0.25920e5 * t7 + 0.22176e5 * t1793 + t1795 + 0.2059200e7
            * t1800 - 0.370656e6 * t1818
            - 0.1372800e7 * t1820 - 0.549120e6 * t784 * t31 - t1827 - t1832 + t1834 - 0.1482624e7 * t1835 * t2462
            + 0.2965248e7 * t155
            * t2462 - 0.741312e6 * t1835 * t1463 + 0.1482624e7 * t155 * t1463 - 0.494208e6 * t86 * t68 * t728;
        final double t2494 = t67 * iy;
        final double t2511 = t118 * t7;
        final double t2514 = 0.658944e6 * t1900 * t27 * t68 + 0.658944e6 * t274 * t67 * t696 + 0.988416e6 * t915 * t73
            * t51 + 0.6589440e7 * t108
            * t1466 + 0.177408e6 * t849 * t16 * t728 - 0.354816e6 * t41 * t68 * t728 - 0.3294720e7 * t1880 * t1466
            + 0.164736e6 * t65
            * t2494 + 0.411840e6 * t1838 - 0.274560e6 * t1840 - 0.41184e5 * t1850 - 0.105600e6 * t1870 - 0.528000e6
            * t1872 + 0.82368e5
            * t23 * t50 * t7 + 0.1478400e7 * t41 * t322 + 0.211200e6 * t322 * t19 - 0.422400e6 * t1472 * t19
            + 0.2471040e7 * t41
            * t2511;
        final double t2530 = t51 * t7;
        final double t2531 = t2530 * t50;
        final double t2554 = -0.7413120e7 * t77 * t326 + 0.2471040e7 * t86 * t322 + 0.22176e5 * t77 * t82 - 0.61776e5
            * t86 * t87 - 0.19008e5 * t105
            * t73 + 0.679536e6 * t121 * t322 + 0.399168e6 * t125 * t322 + 0.164736e6 * t116 * t2531 - 0.1647360e7
            * t630 * t1474
            + 0.679536e6 * t125 * t2511 - 0.2038608e7 * t128 * t326 - 0.164736e6 * t747 * t37 - 0.549120e6 * t792
            * t19 + 0.3294720e7
            * t17 * t1474 - 0.50688e5 * t68 * t24 * t27 - 0.247104e6 * t86 * t1474 + 0.88704e5 * t40 * t51 * t82;
        final double t2570 = t7 * t35 * t36;
        final double t2577 = t51 * t16 * t31;
        final double t2599 = -0.177408e6 * t41 * t1474 - 0.76032e5 * t51 * t18 * t73 - 0.50688e5 * t696 * ex * t67
            + 0.82368e5 * t23 * t724
            - 0.1647360e7 * t630 * t1463 + 0.3294720e7 * t17 * t1463 + 0.164736e6 * t116 * t2570 - 0.741312e6 * t41
            * t87 * t51
            + 0.1482624e7 * t77 * t2577 + 0.88704e5 * t788 * t79 - 0.177408e6 * t41 * t1463 - 0.247104e6 * t86
            * t1463 - 0.988416e6
            * t15 * t51 * t724 - 0.658944e6 * t831 * t736 - 0.658944e6 * t2210 * t785 - 0.1098240e7 * t723 * t728
            + 0.422400e6 * t1642
            * t728 - 0.844800e6 * t2181 * t728;
        final double t2609 = -0.38016e5 * t95 * t2494 + 0.110880e6 * t1874 + t1877 - t1879 - 0.1153152e7 * t1898
            - t1915 - 0.494208e6 * t1916 - t1921
            + t1924 + t1926 + 0.4800e4 * t19 + 0.24000e5 * t32 - t1931 + t1932 + t1934 - t1938 + t1939;
        final double t2612 = t57 * t7;
        final double t2625 = -t1941 + t1944 - t1945 - 0.12672e5 * t2531 - 0.4800e4 * t143 - 0.12600e5 * t2612
            - 0.48000e5 * t788 - 0.126720e6 * t747
            - 0.126720e6 * t326 + 0.926640e6 * t1965 - 0.221760e6 * t1967 - 0.617760e6 * t1970 + 0.384384e6 * t1972
            + 0.185328e6
            * t1974 + 0.177408e6 * t1976 + t1979 - 0.123552e6 * t1980 + 0.164736e6 * t1982;
        final double t2636 = t1985 + t1989 - 0.44352e5 * t1992 + 0.76032e5 * t1994 + 0.823680e6 * t23 * t7 + t2000
            - t2001 + 0.52800e5 * t2002
            + 0.264000e6 * t2007 + t2026 + t2027 - t2029 + t2031 - t2033 - t2035 + 0.123552e6 * t2043 + 0.57024e5
            * t2045 + 0.9000e4
            * t2051;
        final double t2667 = 0.6336e4 * t2060 + 0.13728e5 * t2062 + 0.1800e4 * t2064 - 0.99792e5 * t2612 * t46
            - 0.99792e5 * t128 * t7 + 0.52800e5
            * t312 - 0.12672e5 * t2570 - 0.19008e5 * t146 * t7 - 0.1800e4 * t41 * t31 - 0.137280e6 * t296
            - 0.369600e6 * t825
            + 0.211200e6 * t2577 - 0.422400e6 * t8 * t51 * t31 + 0.506880e6 * t116 * t7 - 0.164736e6 * t15 * t2530
            * t50 - 0.369600e6
            * t788 * t46 - 0.2471040e7 * t114 * t7 + 0.823680e6 * t2057 * t7;

        partialDerivativesJ7[0][0] += 0.189e3 / 0.16e2 * t11 * t161 / t164 / t162 * t173;
        partialDerivativesJ7[0][2] += t291;
        partialDerivativesJ7[0][3] += t406;
        partialDerivativesJ7[0][4] += 0.21e2 / 0.16e2 * t642;
        partialDerivativesJ7[0][5] += 0.21e2 / 0.16e2 * t869;
        partialDerivativesJ7[2][0] += t291;
        partialDerivativesJ7[2][2] += 0.21e2 / 0.64e2 * t11 * iy * t277 * t872 + 0.273e3 / 0.64e2 * t11 * t878 + 0.21e2
            / 0.128e3 * t11 * t14
            * (t902 + t930) * t872 + 0.273e3 / 0.64e2 * t11 * t278 * t877 + 0.4095e4 / 0.128e3 * t11 * t161 * t942
            * t18 + t949;
        partialDerivativesJ7[2][3] += t1047;
        partialDerivativesJ7[2][4] += t1241;
        partialDerivativesJ7[2][5] += t1450;
        partialDerivativesJ7[3][0] += t406;
        partialDerivativesJ7[3][2] += t1047;
        partialDerivativesJ7[3][3] += -0.21e2 / 0.64e2 * t11 * ix * t397 * t872 - 0.273e3 / 0.64e2 * t11 * t1455
            + 0.21e2 / 0.128e3 * t11 * t14
            * (t1471 + t1500) * t872 + 0.273e3 / 0.64e2 * t11 * t398 * t955 + 0.4095e4 / 0.128e3 * t11 * t161
            * t942 * t31 + t949;
        partialDerivativesJ7[3][4] += t1603;
        partialDerivativesJ7[3][5] += t1761;
        partialDerivativesJ7[4][0] += 0.21e2 / 0.16e2 * t642;
        partialDerivativesJ7[4][2] += t1241;
        partialDerivativesJ7[4][3] += t1603;
        partialDerivativesJ7[4][4] += -0.21e2 / 0.128e3 * t1765 * t161 * t872 * t6 + 0.21e2 / 0.64e2 * t409 * t413
            * t1048 - 0.21e2 / 0.64e2 * t409
            * t639 * t1048 - t1778 - 0.21e2 / 0.64e2 * t11 * ey * t638 * t872 + 0.21e2 / 0.128e3 * t11 * t14
            * (t1813 + t1854 + t1897 + t1935 + t1969 + t2004 + t2041 + t2082) * t872;
        partialDerivativesJ7[4][5] += 0.21e2 / 0.128e3 * t2439;
        partialDerivativesJ7[5][0] += 0.21e2 / 0.16e2 * t869;
        partialDerivativesJ7[5][2] += t1450;
        partialDerivativesJ7[5][3] += t1761;
        partialDerivativesJ7[5][4] += 0.21e2 / 0.128e3 * t2439;
        partialDerivativesJ7[5][5] += -0.21e2 / 0.128e3 * t1765 * t161 * t872 * t7 - 0.21e2 / 0.64e2 * t409 * t646
            * t1255 - 0.21e2 / 0.64e2 * t409
            * t866 * t1255 - t1778 + 0.21e2 / 0.64e2 * t11 * ex * t865 * t872 + 0.21e2 / 0.128e3 * t11 * t14
            * (t2474 + t2514 + t2554 + t2599 + t2609 + t2625 + t2636 + t2667) * t872;

        return partialDerivativesJ7;

    }

    /**
     * Compute the partial derivatives due to J2² Potential effect
     * <p>
     * Compute the effect of the partial derivatives due to J2² of the Zonal Perturbation
     * </p>
     * 
     * @param orbit
     *        the equinoctial orbit
     * 
     * @return partialDerivativesJ2Square the J2 partial derivatives
     */
    public double[][] computeJ2SquarePartialDerivatives(final StelaEquinoctialOrbit orbit) {

        /** Orbital elements */
        final double a = orbit.getA();
        final double ex = orbit.getEquinoctialEx();
        final double ey = orbit.getEquinoctialEy();
        final double ix = orbit.getIx();
        final double iy = orbit.getIy();
        final double MU = orbit.getMu();

        final double J22 = this.j[2] * this.j[2];

        /** J2 Potential */
        final double[][] partialDerivativesJ2Square = new double[6][6];

        final double t1 = a * a;
        final double t2 = t1 * t1;
        final double t4 = 0.1e1 / t2 / t1;
        final double t8 = MathLib.sqrt(MU / a);
        final double t9 = this.rEq * this.rEq;
        final double t10 = t9 * t9;
        final double t11 = t10 * t8;
        final double t12 = ex * ex;
        final double t13 = ey * ey;
        final double t14 = 0.1e1 - t12 - t13;
        final double t15 = MathLib.sqrt(t14);
        final double t19 = 0.360e3 * t15 + 0.430e3 - 0.45e2 * t12 - 0.45e2 * t13;
        final double t20 = t14 * t14;
        final double t21 = t20 * t20;
        final double t22 = 0.1e1 / t21;
        final double t23 = t22 * t19;
        final double t24 = ix * ix;
        final double t26 = iy * iy;
        final double t28 = 0.1e1 - 0.2e1 * t24 - 0.2e1 * t26;
        final double t29 = t28 * t28;
        final double t30 = t29 * t29;
        final double t32 = 0.3e1 / 0.32e2 * t30 * t23;
        final double t36 = -0.36e2 + 0.126e3 * t12 + 0.126e3 * t13 - 0.192e3 * t15;
        final double t37 = t22 * t36;
        final double t39 = 0.3e1 / 0.32e2 * t29 * t37;
        final double t41 = 0.25e2 * t12;
        final double t42 = 0.25e2 * t13;
        final double t43 = 0.24e2 * t15 - 0.10e2 - t41 - t42;
        final double t45 = 0.3e1 / 0.32e2 * t22 * t43;
        final double t49 = 0.40e2 - 0.5e1 * t12 - 0.5e1 * t13 + 0.36e2 * t15;
        final double t50 = t22 * t49;
        final double t51 = t29 * t28;
        final double t52 = t51 * t50;
        final double t53 = 0.3e1 / 0.8e1 * t52;
        final double t57 = -0.4e1 + 0.9e1 * t12 + 0.9e1 * t13 - 0.12e2 * t15;
        final double t58 = t22 * t57;
        final double t59 = t28 * t58;
        final double t60 = 0.3e1 / 0.8e1 * t59;
        final double t62 = 0.144e3 * t15 + 0.130e3 - t41 - t42;
        final double t65 = 0.1e1 / t15 / t20 / t14;
        final double t66 = t65 * t62;
        final double t72 = -0.60e2 + 0.90e2 * t12 + 0.90e2 * t13 - 0.96e2 * t15;
        final double t73 = t65 * t72;
        final double t77 = 0.16e2 * t15 + 0.10e2 - t41 - t42;
        final double t80 = t32 + t39 + t45 - t53 - t60 + 0.3e1 / 0.32e2 * t30 * t66 + 0.3e1 / 0.32e2 * t29 * t73
            + 0.3e1 / 0.32e2 * t65 * t77;
        final double t86 = 0.1e1 / t2 / t1 / a;
        final double t88 = 0.1e1 / t8;
        final double t96 = 0.1e1 / t2 / a;
        final double t97 = t96 * J22;
        final double t98 = 0.1e1 / t15;
        final double t99 = ex * t98;
        final double t105 = 0.3e1 / 0.32e2 * t30 * t22 * (-0.360e3 * t99 - 0.90e2 * ex);
        final double t107 = 0.1e1 / t21 / t14;
        final double t108 = t107 * t19;
        final double t109 = ex * t30;
        final double t111 = 0.3e1 / 0.4e1 * t109 * t108;
        final double t117 = 0.3e1 / 0.32e2 * t29 * t22 * (0.252e3 * ex + 0.192e3 * t99);
        final double t118 = t107 * t36;
        final double t119 = ex * t29;
        final double t121 = 0.3e1 / 0.4e1 * t119 * t118;
        final double t123 = 0.50e2 * ex;
        final double t126 = 0.3e1 / 0.32e2 * t22 * (-0.24e2 * t99 - t123);
        final double t127 = t107 * t43;
        final double t129 = 0.3e1 / 0.4e1 * ex * t127;
        final double t135 = 0.3e1 / 0.8e1 * t51 * t22 * (-0.10e2 * ex - 0.36e2 * t99);
        final double t136 = t107 * t49;
        final double t139 = 0.3e1 * ex * t51 * t136;
        final double t145 = 0.3e1 / 0.8e1 * t28 * t22 * (0.18e2 * ex + 0.12e2 * t99);
        final double t146 = t107 * t57;
        final double t149 = 0.3e1 * ex * t28 * t146;
        final double t156 = 0.1e1 / t15 / t21;
        final double t157 = t156 * t62;
        final double t166 = t156 * t72;
        final double t173 = t156 * t77;
        final double t176 = t105 + t111 + t117 + t121 + t126 + t129 - t135 - t139 - t145 - t149 + 0.3e1 / 0.32e2 * t30
            * t65 * (-0.144e3 * t99 - t123) + 0.21e2 / 0.32e2 * t109 * t157 + 0.3e1 / 0.32e2 * t29 * t65
            * (0.180e3 * ex + 0.96e2 * t99) + 0.21e2 / 0.32e2 * t119 * t166 + 0.3e1 / 0.32e2 * t65
            * (-0.16e2 * t99 - t123) + 0.21e2 / 0.32e2 * ex * t173;
        final double t180 = ey * t98;
        final double t186 = 0.3e1 / 0.32e2 * t30 * t22 * (-0.360e3 * t180 - 0.90e2 * ey);
        final double t187 = ey * t30;
        final double t189 = 0.3e1 / 0.4e1 * t187 * t108;
        final double t195 = 0.3e1 / 0.32e2 * t29 * t22 * (0.252e3 * ey + 0.192e3 * t180);
        final double t196 = ey * t29;
        final double t198 = 0.3e1 / 0.4e1 * t196 * t118;
        final double t200 = 0.50e2 * ey;
        final double t203 = 0.3e1 / 0.32e2 * t22 * (-0.24e2 * t180 - t200);
        final double t205 = 0.3e1 / 0.4e1 * ey * t127;
        final double t211 = 0.3e1 / 0.8e1 * t51 * t22 * (-0.10e2 * ey - 0.36e2 * t180);
        final double t214 = 0.3e1 * ey * t51 * t136;
        final double t220 = 0.3e1 / 0.8e1 * t28 * t22 * (0.18e2 * ey + 0.12e2 * t180);
        final double t223 = 0.3e1 * ey * t28 * t146;
        final double t245 = t186 + t189 + t195 + t198 + t203 + t205 - t211 - t214 - t220 - t223 + 0.3e1 / 0.32e2 * t30
            * t65 * (-0.144e3 * t180 - t200) + 0.21e2 / 0.32e2 * t187 * t157 + 0.3e1 / 0.32e2 * t29 * t65
            * (0.180e3 * ey + 0.96e2 * t180) + 0.21e2 / 0.32e2 * t196 * t166 + 0.3e1 / 0.32e2 * t65
            * (-0.16e2 * t180 - t200) + 0.21e2 / 0.32e2 * ey * t173;
        final double t249 = ix * t51;
        final double t251 = 0.3e1 / 0.2e1 * t249 * t23;
        final double t252 = ix * t28;
        final double t254 = 0.3e1 / 0.4e1 * t252 * t37;
        final double t257 = 0.9e1 / 0.2e1 * ix * t29 * t50;
        final double t259 = 0.3e1 / 0.2e1 * ix * t58;
        final double t268 = iy * t51;
        final double t270 = 0.3e1 / 0.2e1 * t268 * t23;
        final double t271 = iy * t28;
        final double t273 = 0.3e1 / 0.4e1 * t271 * t37;
        final double t276 = 0.9e1 / 0.2e1 * iy * t29 * t50;
        final double t278 = 0.3e1 / 0.2e1 * iy * t58;
        final double t287 = ey * J22;
        final double t289 = t32 + t39 + t45 - t53 - t60;
        final double t290 = t289 * t11;
        final double t294 = t10 * t88;
        final double t296 = MU * t289 * t294;
        final double t300 = t96 * t287;
        final double t302 = (t105 + t111 + t117 + t121 + t126 + t129 - t135 - t139 - t145 - t149) * t11;
        final double t305 = t290 * t97;
        final double t307 = (t186 + t189 + t195 + t198 + t203 + t205 - t211 - t214 - t220 - t223) * t11;
        final double t311 = (-t251 - t254 + t257 + t259) * t11;
        final double t315 = (-t270 - t273 + t276 + t278) * t11;
        final double t318 = ex * J22;
        final double t326 = t96 * t318;
        final double t335 = iy * J22;
        final double t337 = -t52 - t59;
        final double t338 = 0.3e1 / 0.8e1 * t337 * t11;
        final double t343 = 0.3e1 / 0.8e1 * MU * t337 * t294;
        final double t347 = t96 * t335;
        final double t349 = (-t135 - t139 - t145 - t149) * t11;
        final double t353 = (-t211 - t214 - t220 - t223) * t11;
        final double t357 = (t257 + t259) * t11;
        final double t360 = t338 * t97;
        final double t362 = (t276 + t278) * t11;
        final double t365 = ix * J22;
        final double t373 = t96 * t365;

        partialDerivativesJ2Square[1][0] = -0.5e1 / 0.4e1 * t80 * t11 * t4 * J22 - MU * t80 * t10 * t88 * t86 * J22
            / 0.8e1;
        partialDerivativesJ2Square[1][2] = t176 * t11 * t97 / 0.4e1;
        partialDerivativesJ2Square[1][3] = t245 * t11 * t97 / 0.4e1;
        partialDerivativesJ2Square[1][4] = (-t251 - t254 + t257 + t259 - 0.3e1 / 0.2e1 * t249 * t66 - 0.3e1 / 0.4e1
            * t252 * t73)
            * t11 * t97 / 0.4e1;
        partialDerivativesJ2Square[1][5] = (-t270 - t273 + t276 + t278 - 0.3e1 / 0.2e1 * t268 * t66 - 0.3e1 / 0.4e1
            * t271 * t73)
            * t11 * t97 / 0.4e1;
        partialDerivativesJ2Square[2][0] = 0.5e1 / 0.4e1 * t290 * t4 * t287 + t296 * t86 * t287 / 0.8e1;
        partialDerivativesJ2Square[2][2] = -t302 * t300 / 0.4e1;
        partialDerivativesJ2Square[2][3] = -t305 / 0.4e1 - t307 * t300 / 0.4e1;
        partialDerivativesJ2Square[2][4] = -t311 * t300 / 0.4e1;
        partialDerivativesJ2Square[2][5] = -t315 * t300 / 0.4e1;
        partialDerivativesJ2Square[3][0] = -0.5e1 / 0.4e1 * t290 * t4 * t318 - t296 * t86 * t318 / 0.8e1;
        partialDerivativesJ2Square[3][2] = t305 / 0.4e1 + t302 * t326 / 0.4e1;
        partialDerivativesJ2Square[3][3] = t307 * t326 / 0.4e1;
        partialDerivativesJ2Square[3][4] = t311 * t326 / 0.4e1;
        partialDerivativesJ2Square[3][5] = t315 * t326 / 0.4e1;
        partialDerivativesJ2Square[4][0] = 0.5e1 / 0.4e1 * t338 * t4 * t335 + t343 * t86 * t335 / 0.8e1;
        partialDerivativesJ2Square[4][2] = -t349 * t347 / 0.4e1;
        partialDerivativesJ2Square[4][3] = -t353 * t347 / 0.4e1;
        partialDerivativesJ2Square[4][4] = -t357 * t347 / 0.4e1;
        partialDerivativesJ2Square[4][5] = -t360 / 0.4e1 - t362 * t347 / 0.4e1;
        partialDerivativesJ2Square[5][0] = -0.5e1 / 0.4e1 * t338 * t4 * t365 - t343 * t86 * t365 / 0.8e1;
        partialDerivativesJ2Square[5][2] = t349 * t373 / 0.4e1;
        partialDerivativesJ2Square[5][3] = t353 * t373 / 0.4e1;
        partialDerivativesJ2Square[5][4] = t360 / 0.4e1 + t357 * t373 / 0.4e1;
        partialDerivativesJ2Square[5][5] = t362 * t373 / 0.4e1;

        return partialDerivativesJ2Square;

    }

    /**
     * Getter for the J2<sup>2</sup> computation flag.
     * 
     * @return true if J2<sup>2</sup> is computed
     */
    public boolean isJ2SquareComputed() {
        return this.isJ2SquareComputed;
    }

    /**
     * Getter for the J2<sup>2</sup> partial derivatives computation flag.
     * 
     * @return true if J2<sup>2</sup> partial derivatives are computed
     */
    public boolean isJ2SquareParDerComputed() {
        return this.isJ2SquareParDerComputed;
    }
}
