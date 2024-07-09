/**
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
 * @history Created 19/07/2012
 *
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:90:03/10/2013:moved GravityToolbox to Orekit
 * VERSION::FA:228:26/03/2014:Removed unused parameter
 * VERSION::FA:410:16/04/2015: Anomalies in the Patrius Javadoc
 * VERSION::FA:464:24/06/2015:Analytical computation of the partial derivatives
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.gravity;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.analysis.polynomials.HelmholtzPolynomial;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Toolbox for tides.
 * 
 * @concurrency immutable
 * 
 * @author Julie Anton, Gerald Mercadier, Rami Houdroge
 * 
 * @version $Id: GravityToolbox.java 18082 2017-10-02 16:54:17Z bignon $
 * 
 * @since 1.2
 */
public final class GravityToolbox {

    /** 0.25. */
    private static final double ZERO_POINT_TWENTYFIVE = 0.25;

    /** 0.5. */
    private static final double ZERO_POINT_FIVE = 0.5;

    /** 1.5. */
    private static final double ONE_POINT_FIVE = 1.5;

    /** 2.5. */
    private static final double TWO_POINT_FIVE = 2.5;

    /** 1e-100. */
    private static final double VALIM = 1e-100;

    /** 7 */
    private static final int C_7 = 7;

    /**
     * Utility class, private constructor.
     */
    private GravityToolbox() {
    }

    /**
     * Method to compute the acceleration, from Balmino algorithm (see {@link BalminoAttractionModel}).
     * 
     * @param pv
     *        PV coordinates of the spacecraft in body frame
     * @param coefficientsC
     *        array of "C" coeffs
     * @param coefficientsS
     *        array of "S" coeffs
     * @param muc
     *        Central body attraction coefficient
     * @param eqRadius
     *        Reference equatorial radius of the potential
     * @param degree
     *        Number of zonal coefficients
     * @param order
     *        Number of tesseral coefficients
     * @param helm
     *        Helmholtz polynomial
     * 
     * @return acceleration vector
     * 
     */
    public static Vector3D
            computeBalminoAcceleration(final PVCoordinates pv,
                                       final double[][] coefficientsC, final double[][] coefficientsS,
                                       final double muc, final double eqRadius,
                                       final int degree, final int order, final HelmholtzPolynomial helm) {
        // get the position in body frame and compute polynomial value
        final Vector3D pos = pv.getPosition();
        helm.computeHelmholtzPolynomial(pos);
        final double[][] phelm = helm.getPh();
        final double[][] dpphelm = helm.getDpph();
        // internal variables
        final double[] dkez = new double[3];
        // length degree + 1
        final double[] tdzr = new double[degree + 1];
        final double[] tdzz = new double[degree + 1];
        final double[] gamma = new double[degree + 1];
        final double[] sigma = new double[degree + 1];
        // intialize some variables
        final double x = pos.getX();
        final double y = pos.getY();
        final double z = pos.getZ();
        final double rsatel = pos.getNorm();
        final double r2 = pos.getNormSq();
        final double[] directorCosines = { x / rsatel, y / rsatel, z / rsatel };
        // powers of earth radius on satellite radius ratio
        final double gmr2 = muc / r2;
        final double[] asr = new double[degree + 1];
        final double ratio = eqRadius / rsatel;
        asr[0] = gmr2;
        for (int i = 1; i < asr.length; i++) {
            asr[i] = asr[i - 1] * ratio;
        }

        // Zonal coefficients (starting l = 0)
        double dzr = 0.;
        double dzz = 0.;
        double prod;
        for (int n = 1; n < tdzz.length; n++) {
            prod = asr[n] * coefficientsC[n][0];
            tdzr[n] = -(n + 1) * prod * phelm[n][0];
            tdzz[n] = prod * dpphelm[n][0];
            dzr = dzr + tdzr[n];
            dzz = dzz + tdzz[n];
        }

        // Tesseral effects (starting m = 1)
        // avec :
        // dkez(1)=d.ksi
        // dkez(2)=d.eta
        // dkez(3)=d.zeta
        final double ppi;
        final double fact;
        final double dr;
        double aux;
        double auxp;
        double dtr = 0;
        final double[] acc = new double[3];
        gamma[0] = 1;
        int m1;
        for (int m = 1; m < sigma.length; m++) {
            m1 = m - 1;
            if (MathLib.abs(gamma[m1]) + MathLib.abs(sigma[m1]) > VALIM) {
                gamma[m] = directorCosines[0] * gamma[m1] - directorCosines[1] * sigma[m1];
                sigma[m] = directorCosines[0] * sigma[m1] + directorCosines[1] * gamma[m1];
            }

        }

        for (int l = 2; l < degree + 1; l++) {
            for (int m = 1; m < MathLib.min(l + 1, order + 1); m++) {
                aux = asr[l] * (coefficientsC[l][m] * gamma[m] + coefficientsS[l][m] * sigma[m]);
                auxp = aux * phelm[l][m];
                dtr -= (l + 1) * auxp;
                dkez[0] += m * asr[l] * phelm[l][m] *
                    (coefficientsC[l][m] * gamma[m - 1] + coefficientsS[l][m] * sigma[m - 1]);
                dkez[1] -= m * asr[l] * phelm[l][m] *
                    (coefficientsC[l][m] * sigma[m - 1] - coefficientsS[l][m] * gamma[m - 1]);
                dkez[2] += aux * dpphelm[l][m];
            }
        }

        dr = dzr + dtr;
        dkez[2] += dzz;
        ppi = directorCosines[0] * dkez[0] + directorCosines[1] * dkez[1] + directorCosines[2] * dkez[2];
        fact = dr - ppi;
        acc[0] = dkez[0] + fact * directorCosines[0];
        acc[1] = dkez[1] + fact * directorCosines[1];
        acc[2] = dkez[2] + fact * directorCosines[2];
        // // somme zonaux et tesseraux
        //
        // for (int i = 0; i < order + 1; i++) {
        // tdr[i] = tdzr[i] + tdtr[i];
        // tfact[i] = tdr[i] - tppi[i];
        // }
        //
        // for (int i = 0; i < degree + 1; i++) {
        // tdkez3[i] = tdzz[i];
        // tppi[i] = t[0] * tdkez1[i] + t[1] * tdkez2[i] + t[2] * tdkez3[i];
        //
        // lacc[0][i] =
        // }
        return new Vector3D(acc);
    }

    /**
     * Method to compute the acceleration, from Droziner algorithm (see {@link DrozinerAttractionModel}).
     * 
     * @param pv
     *        PV coordinates of the spacecraft
     * @param frame
     *        frame in which the acceleration is computed
     * @param coefficientsC
     *        array of "C" coeffs
     * @param coefficientsS
     *        array of "S" coeffs
     * @param muc
     *        Central body attraction coefficient
     * @param eqRadius
     *        Reference equatorial radius of the potential
     * @param threshold
     *        threshold
     * @param degree
     *        Number of zonal coefficients
     * @param order
     *        Number of tesseral coefficients
     * @throws PatriusException
     *         if an Orekit error occurs
     * 
     * @return acceleration vector
     * 
     */
    // CHECKSTYLE: stop MethodLength check
    // CHECKSTYLE: stop NestedBlockDepth check
    // Reason: Orekit code kept as such
    public static Vector3D computeDrozinerAcceleration(final PVCoordinates pv, final Frame frame,
            final double[][] coefficientsC, final double[][] coefficientsS, final double muc, final double eqRadius,
            final double threshold, final int degree, final int order) throws PatriusException {
        // CHECKSTYLE: resume MethodLength check
        // Droziner algorithm (see DrozinerAttractionModel class)

        // get the position in body frame
        final double xBody = pv.getPosition().getX();
        final double yBody = pv.getPosition().getY();
        final double zBody = pv.getPosition().getZ();
        // Computation of intermediate variables
        final double r12 = xBody * xBody + yBody * yBody;
        final double r1 = MathLib.sqrt(r12);
        final double r2 = r12 + zBody * zBody;
        final double r = MathLib.sqrt(r2);
        // check sanity of r1
        checkRadius(r1, threshold);
        final double r3 = r2 * r;
        final double aeOnr = eqRadius / r;
        final double zOnr = zBody / r;
        final double r1Onr = r1 / r;
        // Definition of the first acceleration terms
        final double mMuOnr3 = -muc / r3;
        final double xDotDotk = xBody * mMuOnr3;
        final double yDotDotk = yBody * mMuOnr3;
        // Zonal part of acceleration
        double sumA = 0.0;
        double sumB = 0.0;
        double bk1 = zOnr;
        double bk0 = aeOnr * (3 * bk1 * bk1 - 1.0);
        final double[] cC = coefficientsC[0];
        double jk = -cC[1];
        // first zonal term
        sumA += jk * (2 * aeOnr * bk1 - zOnr * bk0);
        sumB += jk * bk0;
        // other terms
        for (int k = 2; k <= degree; k++) {
            // Loop on degree
            final double bk2 = bk1;
            bk1 = bk0;
            final double p = (1.0 + k) / k;
            bk0 = aeOnr * ((1 + p) * zOnr * bk1 - (k * aeOnr * bk2) / (k - 1));
            final double ak0 = p * aeOnr * bk1 - zOnr * bk0;
            jk = -cC[k];
            sumA += jk * ak0;
            sumB += jk * bk0;
        }

        // calculate the acceleration
        // Basic part
        final double p = -sumA / (r1Onr * r1Onr);
        double aX = xDotDotk * p;
        double aY = yDotDotk * p;
        double aZ = muc * sumB / r2;
        // Tessereal-sectorial part of acceleration
        if (order > 0) {
            // latitude and longitude in body frame
            final double cosL = xBody / r1;
            final double sinL = yBody / r1;
            // intermediate variables
            double betaKminus1 = aeOnr;
            double cosjm1L = cosL;
            double sinjm1L = sinL;
            double sinjL = sinL;
            double cosjL = cosL;
            double betaK = 0;
            double bkj = 0.0;
            double bkm1j = 3 * betaKminus1 * zOnr * r1Onr;
            double bkm2j = 0;
            double bkminus1kminus1 = bkm1j;
            // first terms
            double gkj = coefficientsC[1][1] * cosL + coefficientsS[1][1] * sinL;
            double hkj = coefficientsC[1][1] * sinL - coefficientsS[1][1] * cosL;
            double akj = 2 * r1Onr * betaKminus1 - zOnr * bkminus1kminus1;
            double dkj = (akj + zOnr * bkminus1kminus1) * ZERO_POINT_FIVE;
            double sum1 = akj * gkj;
            double sum2 = bkminus1kminus1 * gkj;
            double sum3 = dkj * hkj;
            // the other terms
            for (int j = 1; j <= order; ++j) {
                // Loop on order
                double innerSum1 = 0.0;
                double innerSum2 = 0.0;
                double innerSum3 = 0.0;
                final double[] cJ = coefficientsC[j];
                final double[] sJ = coefficientsS[j];
                for (int k = 2; k <= degree; ++k) {
                    // Loop on degree
                    if (k < cJ.length) {
                        // Several case
                        gkj = cJ[k] * cosjL + sJ[k] * sinjL;
                        hkj = cJ[k] * sinjL - sJ[k] * cosjL;
                        if (j <= (k - 2)) {
                            // General case
                            bkj = aeOnr * (zOnr * bkm1j * (2.0 * k + 1.0) / (k - j) -
                                aeOnr * bkm2j * (k + j) / (k - 1 - j));
                            akj = aeOnr * bkm1j * (k + 1.0) / (k - j) - zOnr * bkj;
                        } else if (j == (k - 1)) {
                            // n - 1 case
                            betaK = aeOnr * (2.0 * k - 1.0) * r1Onr * betaKminus1;
                            bkj = aeOnr * (2.0 * k + 1.0) * zOnr * bkm1j - betaK;
                            akj = aeOnr * (k + 1.0) * bkm1j - zOnr * bkj;
                            betaKminus1 = betaK;
                        } else if (j == k) {
                            // n case
                            bkj = (2 * k + 1) * aeOnr * r1Onr * bkminus1kminus1;
                            akj = (k + 1) * r1Onr * betaK - zOnr * bkj;
                            bkminus1kminus1 = bkj;
                        }

                        dkj = (akj + zOnr * bkj) * j / (k + 1.0);
                        bkm2j = bkm1j;
                        bkm1j = bkj;
                        innerSum1 += akj * gkj;
                        innerSum2 += bkj * gkj;
                        innerSum3 += dkj * hkj;
                    }
                }

                sum1 += innerSum1;
                sum2 += innerSum2;
                sum3 += innerSum3;
                sinjL = sinjm1L * cosL + cosjm1L * sinL;
                cosjL = cosjm1L * cosL - sinjm1L * sinL;
                sinjm1L = sinjL;
                cosjm1L = cosjL;
            }

            // compute the acceleration
            final double r2Onr12 = r2 / (r1 * r1);
            final double p1 = r2Onr12 * xDotDotk;
            final double p2 = r2Onr12 * yDotDotk;
            aX += p1 * sum1 - p2 * sum3;
            aY += p2 * sum1 + p1 * sum3;
            aZ -= muc * sum2 / r2;
        }

        // Return result
        return new Vector3D(aX, aY, aZ);
    }

    // CHECKSTYLE: resume NestedBlockDepth check

    /**
     * <p>
     * Method to compute the acceleration. This method has been implemented in order to validate the force model only.
     * The reason is that for the validation context, we do not want to set up an instance of the SpacecraftState object
     * to avoid the inertial frame of the spacecraft orbit.
     * </p>
     * 
     * Method taken from {@link CunninghamAttractionModel}
     * 
     * @param pv
     *        PV coordinates of the spacecraft in the rotating frame of the central body
     * @param equatorialRadius
     *        equatorial radius of earth
     * @param coefC
     *        C coefficients array
     * @param coefS
     *        S coefficients array
     * @param degree
     *        degree
     * @param order
     *        order
     * @param mu
     *        gravitation constant
     * 
     * @return acceleration vector
     * 
     * @see CunninghamAttractionModel
     */
    // CHECKSTYLE: stop MethodLength check
    // CHECKSTYLE: stop CommentRatio check
    // Reason: Orekit code kept as such
    public static Vector3D computeCunninghamAcceleration(final PVCoordinates pv, final double equatorialRadius,
                                                         final double[][] coefC, final double[][] coefS,
                                                         final int degree, final int order, final double mu) {
        // CHECKSTYLE: resume MethodLength check
        // CHECKSTYLE: resume CommentRatio check
        final double x = pv.getPosition().getX();
        final double y = pv.getPosition().getY();
        final double z = pv.getPosition().getZ();
        final double x2 = x * x;
        final double y2 = y * y;
        final double z2 = z * z;
        final double r2 = x2 + y2 + z2;
        final double r = MathLib.sqrt(r2);
        // define some intermediate variables
        final double onR2 = 1 / r2;
        final double onR3 = onR2 / r;
        final double rEqOnR2 = equatorialRadius / r2;
        final double rEqOnR4 = rEqOnR2 / r2;
        final double rEq2OnR2 = equatorialRadius * rEqOnR2;
        double cmx = -x * rEqOnR2;
        double cmy = -y * rEqOnR2;
        double cmz = -z * rEqOnR2;
        final double dx = -2 * cmx;
        final double dy = -2 * cmy;
        final double dz = -2 * cmz;
        // intermediate variables gradients
        // since dcy/dx = dcx/dy, dcz/dx = dcx/dz and dcz/dy = dcy/dz,
        // we reuse the existing variables
        double dcmxdx = (x2 - y2 - z2) * rEqOnR4;
        double dcmxdy = dx * y * onR2;
        double dcmxdz = dx * z * onR2;
        double dcmydy = (y2 - x2 - z2) * rEqOnR4;
        double dcmydz = dy * z * onR2;
        double dcmzdz = (z2 - x2 - y2) * rEqOnR4;
        final double ddxdx = -2 * dcmxdx;
        final double ddxdy = -2 * dcmxdy;
        final double ddxdz = -2 * dcmxdz;
        final double ddydy = -2 * dcmydy;
        final double ddydz = -2 * dcmydz;
        final double ddzdz = -2 * dcmzdz;
        final double donr2dx = -dx * rEqOnR2;
        final double donr2dy = -dy * rEqOnR2;
        final double donr2dz = -dz * rEqOnR2;
        // potential coefficients (4 per matrix)
        double vrn = 0.0;
        double vin = 0.0;
        double vrd = 1.0 / (equatorialRadius * r);
        double vid = 0.0;
        double vrn1 = 0.0;
        double vin1 = 0.0;
        double vrn2 = 0.0;
        double vin2 = 0.0;
        // gradient coefficients (4 per matrix)
        double gradXVrn = 0.0;
        double gradXVin = 0.0;
        double gradXVrd = -x * onR3 / equatorialRadius;
        double gradXVid = 0.0;
        double gradXVrn1 = 0.0;
        double gradXVin1 = 0.0;
        double gradXVrn2 = 0.0;
        double gradXVin2 = 0.0;
        double gradYVrn = 0.0;
        double gradYVin = 0.0;
        double gradYVrd = -y * onR3 / equatorialRadius;
        double gradYVid = 0.0;
        double gradYVrn1 = 0.0;
        double gradYVin1 = 0.0;
        double gradYVrn2 = 0.0;
        double gradYVin2 = 0.0;
        double gradZVrn = 0.0;
        double gradZVin = 0.0;
        double gradZVrd = -z * onR3 / equatorialRadius;
        double gradZVid = 0.0;
        double gradZVrn1 = 0.0;
        double gradZVin1 = 0.0;
        double gradZVrn2 = 0.0;
        double gradZVin2 = 0.0;
        // acceleration coefficients
        double vdX = 0.0;
        double vdY = 0.0;
        double vdZ = 0.0;
        // start calculating
        for (int m = 0; m <= order; m++) {
            // intermediate variables to compute incrementation
            final double[] cm = coefC[m];
            final double[] sm = coefS[m];
            double cx = cmx;
            double cy = cmy;
            double cz = cmz;
            double dcxdx = dcmxdx;
            double dcxdy = dcmxdy;
            double dcxdz = dcmxdz;
            double dcydy = dcmydy;
            double dcydz = dcmydz;
            double dczdz = dcmzdz;
            for (int n = m; n <= degree; n++) {
                if (n == m) {
                    // calculate the first element of the next column
                    vrn = equatorialRadius * vrd;
                    vin = equatorialRadius * vid;
                    gradXVrn = equatorialRadius * gradXVrd;
                    gradXVin = equatorialRadius * gradXVid;
                    gradYVrn = equatorialRadius * gradYVrd;
                    gradYVin = equatorialRadius * gradYVid;
                    gradZVrn = equatorialRadius * gradZVrd;
                    gradZVin = equatorialRadius * gradZVid;
                    final double tmpGradXVrd = (cx + dx) * gradXVrd - (cy + dy) * gradXVid + (dcxdx + ddxdx) * vrd -
                        (dcxdy + ddxdy) * vid;
                    gradXVid = (cy + dy) * gradXVrd + (cx + dx) * gradXVid + (dcxdy + ddxdy) * vrd + (dcxdx + ddxdx) *
                        vid;
                    gradXVrd = tmpGradXVrd;
                    final double tmpGradYVrd = (cx + dx) * gradYVrd - (cy + dy) * gradYVid + (dcxdy + ddxdy) * vrd -
                        (dcydy + ddydy) * vid;
                    gradYVid = (cy + dy) * gradYVrd + (cx + dx) * gradYVid + (dcydy + ddydy) * vrd + (dcxdy + ddxdy) *
                        vid;
                    gradYVrd = tmpGradYVrd;
                    final double tmpGradZVrd = (cx + dx) * gradZVrd - (cy + dy) * gradZVid + (dcxdz + ddxdz) * vrd -
                        (dcydz + ddydz) * vid;
                    gradZVid = (cy + dy) * gradZVrd + (cx + dx) * gradZVid + (dcydz + ddydz) * vrd + (dcxdz + ddxdz) *
                        vid;
                    gradZVrd = tmpGradZVrd;
                    final double tmpVrd = (cx + dx) * vrd - (cy + dy) * vid;
                    vid = (cy + dy) * vrd + (cx + dx) * vid;
                    vrd = tmpVrd;
                } else if (n == m + 1) {
                    // calculate the second element of the column
                    vrn = cz * vrn1;
                    vin = cz * vin1;
                    gradXVrn = cz * gradXVrn1 + dcxdz * vrn1;
                    gradXVin = cz * gradXVin1 + dcxdz * vin1;
                    gradYVrn = cz * gradYVrn1 + dcydz * vrn1;
                    gradYVin = cz * gradYVin1 + dcydz * vin1;
                    gradZVrn = cz * gradZVrn1 + dczdz * vrn1;
                    gradZVin = cz * gradZVin1 + dczdz * vin1;
                } else {
                    // calculate the other elements of the column
                    final double inv = 1.0 / (n - m);
                    final double coeff = n + m - 1.0;
                    vrn = (cz * vrn1 - coeff * rEq2OnR2 * vrn2) * inv;
                    vin = (cz * vin1 - coeff * rEq2OnR2 * vin2) * inv;
                    gradXVrn = (cz * gradXVrn1 - coeff * rEq2OnR2 * gradXVrn2 + dcxdz * vrn1 -
                        coeff * donr2dx * vrn2) * inv;
                    gradXVin = (cz * gradXVin1 - coeff * rEq2OnR2 * gradXVin2 + dcxdz * vin1 -
                        coeff * donr2dx * vin2) * inv;
                    gradYVrn = (cz * gradYVrn1 - coeff * rEq2OnR2 * gradYVrn2 + dcydz * vrn1 -
                        coeff * donr2dy * vrn2) * inv;
                    gradYVin = (cz * gradYVin1 - coeff * rEq2OnR2 * gradYVin2 + dcydz * vin1 -
                        coeff * donr2dy * vin2) * inv;
                    gradZVrn = (cz * gradZVrn1 - coeff * rEq2OnR2 * gradZVrn2 + dczdz * vrn1 -
                        coeff * donr2dz * vrn2) * inv;
                    gradZVin = (cz * gradZVin1 - coeff * rEq2OnR2 * gradZVin2 + dczdz * vin1 -
                        coeff * donr2dz * vin2) * inv;
                }

                // increment variables
                cx += dx;
                cy += dy;
                cz += dz;
                dcxdx += ddxdx;
                dcxdy += ddxdy;
                dcxdz += ddxdz;
                dcydy += ddydy;
                dcydz += ddydz;
                dczdz += ddzdz;
                vrn2 = vrn1;
                vin2 = vin1;
                gradXVrn2 = gradXVrn1;
                gradXVin2 = gradXVin1;
                gradYVrn2 = gradYVrn1;
                gradYVin2 = gradYVin1;
                gradZVrn2 = gradZVrn1;
                gradZVin2 = gradZVin1;
                vrn1 = vrn;
                vin1 = vin;
                gradXVrn1 = gradXVrn;
                gradXVin1 = gradXVin;
                gradYVrn1 = gradYVrn;
                gradYVin1 = gradYVin;
                gradZVrn1 = gradZVrn;
                gradZVin1 = gradZVin;
                // compute the acceleration due to the Cnm and Snm coefficients
                // ( as the matrix is inversed, Cnm actually is Cmn )
                // avoid doing the processing if not necessary
                if (cm[n] != 0.0 || sm[n] != 0.0) {
                    vdX += cm[n] * gradXVrn + sm[n] * gradXVin;
                    vdY += cm[n] * gradYVrn + sm[n] * gradYVin;
                    vdZ += cm[n] * gradZVrn + sm[n] * gradZVin;
                }

            }

            // increment variables
            cmx += dx;
            cmy += dy;
            cmz += dz;
            dcmxdx += ddxdx;
            dcmxdy += ddxdy;
            dcmxdz += ddxdz;
            dcmydy += ddydy;
            dcmydz += ddydz;
            dcmzdz += ddzdz;
        }

        return new Vector3D(mu * vdX, mu * vdY, mu * vdZ);
    }

    /**
     * Denormalize an array of coefficients.
     * 
     * @param tab
     *        normalized coefficients array
     * 
     * @comments works up to degree + order = 179
     * @return unnormalized coefficients array
     */
    public static double[][] deNormalize(final double[][] tab) {
        final double[][] res = new double[tab.length][tab[tab.length - 1].length];
        /*
         * Container for k(l)(m)
         * k(l)(m) is the coefficient such that
         * C(l)(m) = SQRT( k(l)(m) ) * C_normalized(l)(m)
         */
        double current;
        // container for k(l)(0), initial value is - k(0)(0)
        double last = -1;
        for (int l = 0; l < tab.length; l++) {
            /*
             * At loop start, last = k(l-1)(0)
             * compute current = k(l)(0) based on 'last' and store for next degrees
             */
            current = last * (2 * l + 1) / (2 * l - 1);
            last = current;
            res[l][0] = MathLib.sqrt(current) * tab[l][0];
            current = current * 2;
            for (int m = 1; m <= MathLib.min(l, tab[l].length - 1); m++) {
                /*
                 * At loop start,
                 * current = k(l)(m-1)
                 * Compute k(l)(m) based on 'current'
                 */
                current = current / ((l - m + 1.) * (l + m));
                // compute denormalized value
                res[l][m] = MathLib.sqrt(current) * tab[l][m];
            }

        }

        return res;
    }

    /**
     * Normalize an array of coefficients.
     * 
     * @param tab
     *        normalized coefficients array
     * 
     * @comments works up to degree + order = 179
     * @return unnormalized coefficients array
     */
    public static double[][] normalize(final double[][] tab) {
        final double[][] res = new double[tab.length][tab[tab.length - 1].length];
        /*
         * Container for k(l)(m)
         * k(l)(m) is the coefficient such that
         * C(l)(m) = SQRT( k(l)(m) ) * C_normalized(l)(m)
         */
        double current;
        // container for k(l)(0), initial value is - k(0)(0)
        double last = -1;
        for (int l = 0; l < tab.length; l++) {
            /*
             * At loop start, last = k(l-1)(0)
             * compute current = k(l)(0) based on 'last' and store for next degrees
             */
            current = last * (2 * l + 1) / (2 * l - 1);
            last = current;
            res[l][0] = tab[l][0] / MathLib.sqrt(current);
            current = current * 2;
            for (int m = 1; m <= MathLib.min(l, tab[l].length - 1); m++) {
                /*
                 * At loop start,
                 * current = k(l)(m-1)
                 * Compute k(l)(m) based on 'current'
                 */
                current = current / ((l - m + 1.) * (l + m));
                // compute denormalized value
                res[l][m] = tab[l][m] / MathLib.sqrt(current);
            }

        }

        // Return result
        return res;
    }

    /**
     * Unnormalize a coefficients array.
     * 
     * @param normalized
     *        normalized coefficients array
     * @return unnormalized array
     */
    public static double[][] unNormalize(final double[][] normalized) {
        // allocate a triangular array
        final double[][] unNormalized = new double[normalized.length][];
        unNormalized[0] = new double[] { normalized[0][0] };

        // initialization
        double factN = 1.0;
        double mfactNMinusM = 1.0;
        double mfactNPlusM = 1.0;
        // unnormalize the coefficients
        for (int n = 1; n < normalized.length; n++) {
            // Loop on coefficients
            final double[] uRow = new double[n + 1];
            final double[] nRow = normalized[n];
            final double coeffN = 2.0 * (2 * n + 1);
            factN *= n;
            mfactNMinusM = factN;
            mfactNPlusM = factN;
            uRow[0] = MathLib.sqrt(2 * n + 1) * normalized[n][0];
            for (int m = 1; m < uRow.length; m++) {
                mfactNPlusM *= n + m;
                mfactNMinusM /= n - m + 1;
                uRow[m] = MathLib.sqrt((coeffN * mfactNMinusM) / mfactNPlusM) * nRow[m];
            }

            unNormalized[n] = uRow;
        }

        // Return result
        return unNormalized;
    }

    /**
     * Sanity check for r1.
     * 
     * @param r1
     *        must not be under THRESHOLD
     * @param threshold
     *        precision threshold
     * @throws PatriusException
     *         if one of them is wrong
     */
    private static void checkRadius(final double r1, final double threshold) throws PatriusException {
        if (r1 <= threshold) {
            throw new PatriusException(PatriusMessages.POLAR_TRAJECTORY, r1);
        }

    }

    /**
     * Compute the partial derivatives of the acceleration (Cunningham algorithm) with respect to the position.
     * 
     * @param pv
     *        : position and velocity of the spacecraft
     * @param date
     *        : date
     * @param equatorialRadius
     *        equatorial radius
     * @param mu
     *        gravitational parameter
     * @param c
     *        C coefficients
     * @param s
     *        S coefficients
     * @return array of the partial derivatives
     * 
     *         See the following article :
     *         "On the computation of spherical harmonic terms needed during the numerical integration of
     *         the orbital motion of an artifical satellite"
     *         , Leland E. Cunningham
     * 
     */
    // CHECKSTYLE: stop MethodLength check
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Orekit code kept as such
    public static double[][] computeDAccDPos(final PVCoordinates pv, final AbsoluteDate date,
                                             final double equatorialRadius,
                                             final double mu, final double[][] c, final double[][] s) {
        // CHECKSTYLE: resume MethodLength check
        // CHECKSTYLE: resume CyclomaticComplexity check
        // satellite position in the given frame
        final double x = pv.getPosition().getX();
        final double y = pv.getPosition().getY();
        final double z = pv.getPosition().getZ();
        final double x2 = x * x;
        final double y2 = y * y;
        final double z2 = z * z;
        final double r2 = x2 + y2 + z2;
        final double r = MathLib.sqrt(r2);
        // intermediate variables
        final double zOnR2 = z / r2;
        final double ae2OnR2 = equatorialRadius * equatorialRadius / r2;
        final double aeOnR2 = equatorialRadius / r2;
        // degree and order
        final int order = c.length - 1;
        final int degree = c[order].length - 1;
        /*
         * The Earth's gravitationnal potential, -V, can be written :
         * V = ∑ ∑ Req^n * (Cnm - iSnm) * Vnm
         * where :
         * Vnm = 1/r^(n+1) * (cos(m lambda) + i sin(m lambda)) * Pnm(sin(phi))
         * Pnm are the associated Legendre polynomials
         * Req is the equatorial radius of the Earth
         * The partial derivatives of the acceleration with respect to the position are obtained by differentiation of
         * the Vnm terms. The derivative of Vnm with respect to the position is a linear combination of Vnm terms.
         * The Vnm terms are given by 2 recurrence relations. Since the Vnm terms are complex, they will be stored in a
         * 2 dimensional array of doubles.
         */
        /*
         * First the Vnn terms are computed. They are guven by the following relation (see the given reference) :
         * Vnn = (2n-1) (x+iy) / r2 * Req * Vn-1,n-1
         * V00 = 1/r
         */
        final double[][] vnn = new double[2][degree + 3];
        // Computation of V00 : real part
        vnn[0][0] = 1 / r;
        // Computation of V00 : imaginary part
        vnn[1][0] = 0.;
        double cst;
        for (int n = 1; n <= degree + 2; n++) {
            cst = (2 * n - 1) * aeOnR2;
            // Computation of Vnn : real part
            vnn[0][n] = cst * (x * vnn[0][n - 1] - y * vnn[1][n - 1]);
            // Computation of Vnn : imaginary part
            vnn[1][n] = cst * (y * vnn[0][n - 1] + x * vnn[1][n - 1]);
        }

        // initialization
        // jacobian matrix
        final double[][] der = new double[3][3];
        /*
         * In order to compute the second derivatives of Vnm wrt the position, the terms Vn+2,m-2 ; Vn+2,m-1 ; Vn+2,m ;
         * Vn+2,m+1 and Vn+2,m+2 are needed.
         */
        // temporary tables of Vnm terms
        final double[][] tmpNplus2M = new double[2][5];
        final double[][] tmpNplus3M = new double[2][5];
        final double[][] tmpNplus2Mplus1 = new double[2][5];
        final double[][] tmpNplus3Mplus1 = new double[2][5];
        // initialization
        // tmpNplus2M = [ . . v20 v21 v22]
        // v22
        tmpNplus2M[0][4] = vnn[0][2];
        tmpNplus2M[1][4] = vnn[1][2];
        // v21
        tmpNplus2M[0][3] = 3 * zOnR2 * equatorialRadius * vnn[0][1];
        tmpNplus2M[1][3] = 3 * zOnR2 * equatorialRadius * vnn[1][1];
        // v20
        tmpNplus2M[0][2] = ae2OnR2 * vnn[0][0] * (ONE_POINT_FIVE * z * z / r2 - ZERO_POINT_FIVE);
        // tmpNplus3M = [. . v30 v31 v32]
        // v32
        tmpNplus3M[0][4] = 5 * zOnR2 * equatorialRadius * vnn[0][2];
        tmpNplus3M[1][4] = 5 * zOnR2 * equatorialRadius * vnn[1][2];
        // v31
        tmpNplus3M[0][3] = TWO_POINT_FIVE * zOnR2 * equatorialRadius * tmpNplus2M[0][3]
            - ONE_POINT_FIVE * ae2OnR2 * vnn[0][1];
        tmpNplus3M[1][3] = TWO_POINT_FIVE * zOnR2 * equatorialRadius * tmpNplus2M[1][3]
            - ONE_POINT_FIVE * ae2OnR2 * vnn[1][1];
        // v30
        tmpNplus3M[0][2] = 1 / 3. * (5 * zOnR2 * equatorialRadius * tmpNplus2M[0][2] - 2 * ae2OnR2 * zOnR2 *
            equatorialRadius * vnn[0][0]);
        /*
         * Loop on the order m
         */
        for (int m = 0; m <= order; m++) {
            // case m = 0
            if (m == 0) {
                // compute d²Vn,0
                computeWithMequal0(der, tmpNplus2M, 0, equatorialRadius, mu, c);
                if (m + 1 <= degree) {
                    // compute d²Vn+1,0
                    computeWithMequal0(der, tmpNplus3M, 1, equatorialRadius, mu, c);
                }

            } else if (m == 1) {
                // case m = 1
                // compute d²Vn,1
                computeWithMequal1(der, tmpNplus2M, 1, equatorialRadius, mu, c, s);
                if (m + 1 <= degree) {
                    // compute d²Vn+1,1
                    computeWithMequal1(der, tmpNplus3M, 2, equatorialRadius, mu, c, s);
                }

            } else {
                // general case
                // compute d²Vn,m
                computeDer(der, tmpNplus2M, m, m, equatorialRadius, mu, c, s);
                if (m + 1 <= degree) {
                    // compute d²Vn+1,m
                    computeDer(der, tmpNplus3M, m + 1, m, equatorialRadius, mu, c, s);
                }

            }

            /*
             * To compute the derivatives d²Vn+2,m, the terms Vm+4,m-2; Vm+4,m-1; Vm+4,m; Vm+4,m+1 and Vm+4,m+2n are
             * needed. They are obtained from the temporary arrays tmpNplus2M and tmpNplus3M which contain those terms :
             * tmpNplus2M = {Vm+2,m-2 Vm+2,m-1 Vm+2,m Vm+2,m+1 Vm+2,m+2} , n = m
             * tmpNplus3M = {Vm+3,m-2 Vm+3,m-1 Vm+3,m Vm+3,m+1 Vm+3,m+2} , n = m+1
             * The terms Vn+4,j (with m-2<=j<=m+2) are given by the following recurrence relation :
             * (n-m) Vnm = (2n-1) z / r2 * Req * Vn-1,m - (n+m-1) / r2 * Req * Vn-2,m
             * The arrays tmpNplus2M and tmpNplus3M are updated :
             * tmpNplus2M = {Vm+3,m-2 Vm+3,m-1 Vm+3,m Vm+3,m+1 Vm+3,m+2} , n = m+1
             * tmpNplus3M = {Vm+4,m-2 Vm+4,m-1 Vm+4,m Vnm+4,m+1 Vm+4,m+2} , n = m+2
             */
            recurrence(m + 4, m, tmpNplus2M, tmpNplus3M, zOnR2, ae2OnR2, equatorialRadius);
            /*
             * The values of tmpNplus2M and tmpNplus3M are stored for the next loop on m
             */
            for (int i = 0; i < 5; i++) {
                tmpNplus2Mplus1[0][i] = tmpNplus2M[0][i];
                tmpNplus2Mplus1[1][i] = tmpNplus2M[1][i];
                tmpNplus3Mplus1[0][i] = tmpNplus3M[0][i];
                tmpNplus3Mplus1[1][i] = tmpNplus3M[1][i];
            }

            /*
             * Loop on the degree n from n = m + 2
             */
            for (int n = m + 2; n <= degree; n++) {
                // case m = 0
                if (m == 0) {
                    computeWithMequal0(der, tmpNplus3M, n, equatorialRadius, mu, c);
                } else if (m == 1) {
                    // case m = 1
                    computeWithMequal1(der, tmpNplus3M, n, equatorialRadius, mu, c, s);
                } else {
                    // general case
                    computeDer(der, tmpNplus3M, n, m, equatorialRadius, mu, c, s);
                }

                // update of tmpNplus2M and tmpNplus3M for the next loop on n
                if (n + 1 <= degree) {
                    recurrence(n + 3, m, tmpNplus2M, tmpNplus3M, zOnR2, ae2OnR2, equatorialRadius);
                }

            }

            /*
             * m will be incremented, therefore, the arrays tmpNplus2M and tmpNplus3M have to be updated.
             * We have :
             * tmpNplus2Mplus1 = {Vm+2,m-2 Vm+2,m-1 Vm+2,m Vm+2,m+1 Vm+2,m+2} , n = m
             * tmpNplus3Mplus1 = {Vm+3,m-2 Vm+3,m-1 Vm+3,m Vm+3,m+1 Vm+3,m+2} , n = m+1
             * We need :
             * tmpNplus2M = {Vm+2,m-1 Vm+2,m Vm+2,m+1 Vm+2,m+2 Vm+2,m+3} , n = m
             * tmpNplus3M = {Vm+3,m-1 Vm+3,m Vm+3,m+1 Vm+3,m+2 Vm+3,m+3} , n = m+1
             * The term Vm+3,m+3 has already been computed.
             * The term Vm+2,m+3 is given (see the article) by the relation : Vnm = (2n-1) z / r2 * Req * Vn-1,m when
             * n=m+1
             */
            for (int i = 0; i < 4; i++) {
                tmpNplus2M[0][i] = tmpNplus2Mplus1[0][i + 1];
                tmpNplus2M[1][i] = tmpNplus2Mplus1[1][i + 1];
                tmpNplus3M[0][i] = tmpNplus3Mplus1[0][i + 1];
                tmpNplus3M[1][i] = tmpNplus3Mplus1[1][i + 1];
            }

            if (m + 3 <= degree + 2) {
                tmpNplus2M[0][4] = vnn[0][m + 3];
                tmpNplus2M[1][4] = vnn[1][m + 3];
                tmpNplus3M[0][4] = (2 * m + C_7) * zOnR2 * equatorialRadius * tmpNplus2M[0][4];
                tmpNplus3M[1][4] = (2 * m + C_7) * zOnR2 * equatorialRadius * tmpNplus2M[1][4];
            }

        }

        der[0][1] = der[1][0];
        der[0][2] = der[2][0];
        der[1][2] = der[2][1];
        return der;
    }

    /**
     * Formulae to compute the partial second derivatives of Vnm wrt the position.
     * 
     * @param der
     *        array of the partial derivatives of the acceleration wrt the position which will be updated
     * @param vnm
     *        terms Vn+2,m-2 Vn+2,m-1 Vn+2,m Vn+2,m+1 Vn+2,m+2 (needed to compute d2Vnm)
     * @param n
     *        degree
     * @param m
     *        order
     * @param equatorialRadius
     *        equatorial radius
     * @param mu
     *        gravitational parameter
     * @param cCoefs
     *        C coefficients
     * @param sCoefs
     *        S coefficients
     * 
     *        See the article
     *        "On the computation of spherical harmonic terms needed during the numerical integration of the
     *        orbital motion of an artifical satellite"
     *        , Leland E. Cunningham for the formulae.
     */
    private static void computeDer(final double[][] der, final double[][] vnm, final int n, final int m,
                                   final double equatorialRadius, final double mu, final double[][] cCoefs,
                                   final double[][] sCoefs) {
        // Initialization
        //
        final double muOnAe2 = mu / (equatorialRadius * equatorialRadius);
        final double c = cCoefs[m][n];
        final double s = sCoefs[m][n];
        final double c2 = (n - m + 2) * (n - m + 1);
        final double c3 = (n - m + 3) * c2;
        final double c4 = (n - m + 4) * c3;

        // Derivatives
        der[0][0] += ZERO_POINT_TWENTYFIVE * muOnAe2 *
            (vnm[0][4] * c + vnm[1][4] * s - 2 * c2 * (vnm[0][2] * c + vnm[1][2] * s) + c4 *
                (vnm[0][0] * c + vnm[1][0] * s));
        der[1][0] += ZERO_POINT_TWENTYFIVE * muOnAe2 * (vnm[1][4] * c - vnm[0][4] * s + c4 * (-vnm[1][0] * c
            + vnm[0][0] * s));
        der[2][0] += ZERO_POINT_FIVE * muOnAe2 *
            ((n - m + 1) * (vnm[0][3] * c + vnm[1][3] * s) - c3 * (vnm[0][1] * c + vnm[1][1] * s));
        der[1][1] += -ZERO_POINT_TWENTYFIVE * muOnAe2 *
            (vnm[0][4] * c + vnm[1][4] * s + 2 * c2 * (vnm[0][2] * c + vnm[1][2] * s) + c4 *
                (vnm[0][0] * c + vnm[1][0] * s));
        der[2][1] += ZERO_POINT_FIVE * muOnAe2 *
            ((n - m + 1) * (c * vnm[1][3] - s * vnm[0][3]) + c3 * (vnm[1][1] * c - vnm[0][1] * s));
        der[2][2] += c2 * muOnAe2 * (c * vnm[0][2] + s * vnm[1][2]);
    }

    /**
     * Formulae to compute the partial second derivatives of Vn0 wrt the position.
     * 
     * @param der
     *        array of the partial derivatives of the acceleration wrt the position which will be updated
     * @param vnm
     *        terms Vn+2,m-2 Vn+2,m-1 Vn+2,m Vn+2,m+1 Vn+2,m+2 (needed to compute d2Vnm)
     * @param n
     *        degree
     * @param equatorialRadius
     *        equatorial radius
     * @param mu
     *        gravitational parameter
     * @param cCoefs
     *        C coefficients
     * 
     *        See the article
     *        "On the computation of spherical harmonic terms needed during the numerical
     *        integration of the orbital motion of an artifical satellite"
     *        , Leland E. Cunningham for the formulae.
     */
    private static void computeWithMequal0(final double[][] der, final double[][] vnm, final int n,
                                           final double equatorialRadius, final double mu, final double[][] cCoefs) {
        final double muOnAe2 = mu / (equatorialRadius * equatorialRadius);
        final double c = cCoefs[0][n];
        // d²vn0/dx² term
        der[0][0] += ZERO_POINT_FIVE * c * muOnAe2 * (vnm[0][4] - (n + 1) * (n + 2) * vnm[0][2]);
        // d²vn0/dxdy term
        der[1][0] += c * ZERO_POINT_FIVE * muOnAe2 * vnm[1][4];
        // d²vn0/dxdz
        der[2][0] += c * muOnAe2 * (n + 1) * vnm[0][3];
        // d²vn0/dy²
        der[1][1] += -ZERO_POINT_FIVE * c * muOnAe2 * (vnm[0][4] + (n + 1) * (n + 2) * vnm[0][2]);
        // d²vn0/dydz
        der[2][1] += c * muOnAe2 * (n + 1) * vnm[1][3];
        // d²vn0/dz²
        der[2][2] += c * muOnAe2 * (n + 2) * (n + 1) * vnm[0][2];
    }

    /**
     * Formulae to compute the partial second derivatives of Vn1 wrt the position.
     * 
     * @param der
     *        : array of the partial derivatives of the acceleration wrt the position which will be updated
     * @param vnm
     *        : terms Vn+2,m-2 Vn+2,m-1 Vn+2,m Vn+2,m+1 Vn+2,m+2 (needed to compute d2Vnm)
     * @param n
     *        : degree
     * @param equatorialRadius
     *        equatorial radius
     * @param mu
     *        gravitational parameter
     * @param cCoefs
     *        C coefficients
     * @param sCoefs
     *        S coefficients
     * 
     *        See the article
     *        "On the computation of spherical harmonic terms needed during the numerical
     *        integration of the orbital motion of an artifical satellite"
     *        , Leland E. Cunningham for the formulae.
     */
    private static void computeWithMequal1(final double[][] der, final double[][] vnm, final int n,
                                           final double equatorialRadius, final double mu, final double[][] cCoefs,
                                           final double[][] sCoefs) {
        final double muOnAe2 = mu / (equatorialRadius * equatorialRadius);
        final double c = cCoefs[1][n];
        final double s = sCoefs[1][n];
        // d²vn1/dx² term
        der[0][0] += ZERO_POINT_TWENTYFIVE * muOnAe2 *
            (c * vnm[0][4] + s * vnm[1][4] - n * (n + 1) * (3 * c * vnm[0][2] + s * vnm[1][2]));
        // d²vn1/dxdy term
        der[1][0] += ZERO_POINT_TWENTYFIVE * muOnAe2 * (vnm[1][4]
            * c - vnm[0][4] * s - n * (n + 1) * (c * vnm[1][2] + s * vnm[0][2]));
        // d²vn1/dxdz
        der[2][0] += ZERO_POINT_FIVE * muOnAe2 *
            (n * (vnm[0][3] * c + vnm[1][3] * s) - n * (n + 1) * (n + 2) * (c * vnm[0][1] + s * vnm[1][1]));
        // d²vn1/dy²
        der[1][1] += -ZERO_POINT_TWENTYFIVE * muOnAe2 *
            (c * vnm[0][4] + s * vnm[1][4] + n * (n + 1) * (c * vnm[0][2] + 3 * s * vnm[1][2]));
        // d²vn1/dydz
        der[2][1] += ZERO_POINT_FIVE * muOnAe2 *
            (n * (c * vnm[1][3] - s * vnm[0][3]) + n * (n + 1) * (n + 2) * (c * vnm[1][1] - s * vnm[0][1]));
        // d²vn1/dz²
        der[2][2] += n * (n + 1) * muOnAe2 * (vnm[0][2] * c + s * vnm[1][2]);
    }

    /**
     * Recurrence relation to compute Vn,k from Vn-1,k and Vn-2,m (with m-2 <= k <= m+2).
     * 
     * (n-m) Vnm = (2n-1) z / r2 * Req * Vn-1,m - (n+m-1) / r2 * Req * Vn-2,m
     * 
     * @param n
     *        degree
     * @param m
     *        order
     * @param v1nm
     *        terms {Vn-2,m-2 Vn-2,m-1 Vn-2,m Vn-2,m+1 Vn-2,m+2} which will be updated
     * @param v2nm
     *        terms {Vn-1,m-2 Vn-1,m-1 Vn-1,m Vn-1,m+1 Vn-1,m+2} which will be updated
     * @param zOnR2
     *        z over r2 with r2 = x2 + y2 + z2 and (x,y,z) the position of the satellite
     * @param ae2OnR2
     *        Req over r2 with Req the equatorial radius of the Earth with r2 = x2 + y2 + z2 and (x,y,z) the
     *        position of the satellite
     * @param equatorialRadius
     *        equatorial radius
     */
    private static void recurrence(final int n, final int m, final double[][] v1nm, final double[][] v2nm,
                                   final double zOnR2,
                                   final double ae2OnR2, final double equatorialRadius) {
        final double[][] newtmpline = new double[2][5];
        final double k = n - m;
        // Vn+2_m-2
        newtmpline[0][0] = 1 / (k + 2.) *
            ((2 * n - 1) * zOnR2 * equatorialRadius * v2nm[0][0] - (n + m - 3) * ae2OnR2 * v1nm[0][0]);
        newtmpline[1][0] = 1 / (k + 2.) *
            ((2 * n - 1) * zOnR2 * equatorialRadius * v2nm[1][0] - (n + m - 3) * ae2OnR2 * v1nm[1][0]);
        // Vn+2_m-1
        newtmpline[0][1] = 1 / (k + 1.) *
            ((2 * n - 1) * zOnR2 * equatorialRadius * v2nm[0][1] - (n + m - 2) * ae2OnR2 * v1nm[0][1]);
        newtmpline[1][1] = 1 / (k + 1.) *
            ((2 * n - 1) * zOnR2 * equatorialRadius * v2nm[1][1] - (n + m - 2) * ae2OnR2 * v1nm[1][1]);
        // Vn+2_m
        newtmpline[0][2] = 1 / k *
            ((2 * n - 1) * zOnR2 * equatorialRadius * v2nm[0][2] - (n + m - 1) * ae2OnR2 * v1nm[0][2]);
        newtmpline[1][2] = 1 / k *
            ((2 * n - 1) * zOnR2 * equatorialRadius * v2nm[1][2] - (n + m - 1) * ae2OnR2 * v1nm[1][2]);
        // Vn+2_m+1
        newtmpline[0][3] = 1 / (k - 1.) *
            ((2 * n - 1) * zOnR2 * equatorialRadius * v2nm[0][3] - (n + m) * ae2OnR2 * v1nm[0][3]);
        newtmpline[1][3] = 1 / (k - 1.) *
            ((2 * n - 1) * zOnR2 * equatorialRadius * v2nm[1][3] - (n + m) * ae2OnR2 * v1nm[1][3]);
        // Vn+2_m+2
        newtmpline[0][4] = 1 / (k - 2.) *
            ((2 * n - 1) * zOnR2 * equatorialRadius * v2nm[0][4] - (n + m + 1) * ae2OnR2 * v1nm[0][4]);
        newtmpline[1][4] = 1 / (k - 2.) *
            ((2 * n - 1) * zOnR2 * equatorialRadius * v2nm[1][4] - (n + m + 1) * ae2OnR2 * v1nm[1][4]);

        for (int i = 0; i < 5; i++) {
            v1nm[0][i] = v2nm[0][i];
            v1nm[1][i] = v2nm[1][i];
        }

        for (int i = 0; i < 5; i++) {
            v2nm[0][i] = newtmpline[0][i];
            v2nm[1][i] = newtmpline[1][i];
        }

    }

}
