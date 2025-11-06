package fr.cnes.sirius.patrius.stela.forces.gravity.recurrence;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.StelaEquinoctialParameters;
import fr.cnes.sirius.patrius.stela.orbits.StelaEquinoctialOrbit;
import fr.cnes.sirius.patrius.utils.Constants;

/**
 * This class carries the Earth zonal harmonics recurrence methods equations and is meant to be used by
 * {@link StelaRecurrenceZonalAttraction}.
 * <p>
 * The class is adapted from STELA RecurrenceZonalEq in
 * fr.cnes.los.stela.elib.business.implementation.earthpotential.zonal.
 * </p>
 * 
 * @concurrency conditionally thread-safe
 * @concurrency.comment thread safe if the inputs arrays are not modified (as they are stored directly for performance
 *                      purpose)
 * 
 * @author Maxime Ecochard, Thibaut BONIT
 * HISTORY
 * VERSION:4.16:OPENFD-391:25/04/2025:[STELA-PATRIUS] Implementation zonaux par recurrence
 * END-HISTORY
 * @since 4.16
 */
public class StelaRecurrenceZonalEquation implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = -4637918954919913109L;

    /** a. */
    private final double a;

    /** ex. */
    private final double ex;

    /** ey. */
    private final double ey;

    /** ix. */
    private final double ix;

    /** iy. */
    private final double iy;

    /** Mu / Sma. */
    private final double muSma;

    /** e*e = ex*ex + ey*ey. */
    private final double e2;

    /** eta = (1 - e*e)^0.5. */
    private final double eta;

    /** cos(i/2) = (1 - (sin(i/2)^2)^0.5. */
    private final double cosi2;

    /** (cos(i/2)^n */
    private final double[] cosi2n;

    /** ((sin(i/2)^2)^n */
    private final double[] sin2i2n;

    /** (e*e)^n */
    private final double[] e2n;

    /** eta^n */
    private final double[] etaN;

    /** (R_0 / Sma)^n */
    private final double[] r0SmaN;

    /** Uq */
    private final double[] u;

    /** Vq */
    private final double[] v;

    /** d(Uq) / d(ex) */
    private final double[] dUdEx;

    /** d(Vq) / d(ex) */
    private final double[] dVdEx;

    /** d(Uq) / d(ey) */
    private final double[] dUdEy;

    /** d(Vq) / d(ey) */
    private final double[] dVdEy;

    /** d(Uq) / d(ix) */
    private final double[] dUdIx;

    /** d(Vq) / d(ix) */
    private final double[] dVdIx;

    /** d(Uq) / d(iy) */
    private final double[] dUdIy;

    /** d(Vq) / d(iy) */
    private final double[] dVdIy;

    /** Power of two. */
    private final double[] powOfTwo;

    /** Alpha coefficients. */
    private final double[][][][][] alphaCoef;

    /** Beta coefficients. */
    private final double[][][][][] betaCoef;

    /**
     * Constructor.
     * <p>
     * Note that the given arrays are stored directly. They aren't copied for performance purpose.<br>
     * This class is designed to be commonly used by {@link StelaRecurrenceZonalAttraction} (hence the protected
     * constructor) which compute these arrays only once when initialized.
     * </p>
     * 
     * @param orbit
     *        orbit information: date, kinematics
     * @param zonalDegreeMaxPerturbation
     *        degree of development for zonal perturbations
     * @param powOfTwo
     *        Power of two
     * @param alphaCoef
     *        Alpha coefficients
     * @param betaCoef
     *        Beta coefficients
     */
    protected StelaRecurrenceZonalEquation(final StelaEquinoctialOrbit orbit, final int zonalDegreeMaxPerturbation,
                                           final double[] powOfTwo, final double[][][][][] alphaCoef,
                                           final double[][][][][] betaCoef) {

        // Store the inputs
        this.powOfTwo = powOfTwo;
        this.alphaCoef = alphaCoef;
        this.betaCoef = betaCoef;

        // Compute extended orbital variables (independent from the order of the potential development)
        final StelaEquinoctialParameters equinoxParameters = orbit.getEquinoctialParameters();
        this.a = equinoxParameters.getA();
        this.ex = equinoxParameters.getEquinoctialEx();
        this.ey = equinoxParameters.getEquinoctialEy();
        this.ix = equinoxParameters.getIx();
        this.iy = equinoxParameters.getIy();

        this.muSma = Constants.CNES_STELA_MU / this.a;
        this.e2 = this.ex * this.ex + this.ey * this.ey;
        this.eta = MathLib.sqrt(1 - this.e2);
        final double r0Sma = Constants.CNES_STELA_AE / this.a;
        final double sin2i2 = this.ix * this.ix + this.iy * this.iy;
        this.cosi2 = MathLib.sqrt(1 - sin2i2);
        final double eixy = this.ex * this.ix + this.ey * this.iy;
        final double eiyx = this.ey * this.ix - this.ex * this.iy;

        // Power tables declaration
        this.cosi2n = new double[zonalDegreeMaxPerturbation + 1];
        this.sin2i2n = new double[zonalDegreeMaxPerturbation / 2 + 1];
        this.e2n = new double[zonalDegreeMaxPerturbation / 2 + 1];
        this.etaN = new double[2 * zonalDegreeMaxPerturbation + 1];
        this.r0SmaN = new double[zonalDegreeMaxPerturbation + 1];

        // Sequences declaration
        this.u = new double[zonalDegreeMaxPerturbation + 1];
        this.v = new double[zonalDegreeMaxPerturbation + 1];
        this.dUdEx = new double[zonalDegreeMaxPerturbation + 1];
        this.dVdEx = new double[zonalDegreeMaxPerturbation + 1];
        this.dUdEy = new double[zonalDegreeMaxPerturbation + 1];
        this.dVdEy = new double[zonalDegreeMaxPerturbation + 1];
        this.dUdIx = new double[zonalDegreeMaxPerturbation + 1];
        this.dVdIx = new double[zonalDegreeMaxPerturbation + 1];
        this.dUdIy = new double[zonalDegreeMaxPerturbation + 1];
        this.dVdIy = new double[zonalDegreeMaxPerturbation + 1];

        // Power tables initialization
        this.cosi2n[0] = 1.;
        this.sin2i2n[0] = 1.;
        this.e2n[0] = 1.;
        this.etaN[0] = 1.;
        this.r0SmaN[0] = 1.;

        // Sequences initialization
        this.u[0] = 1.;
        this.v[0] = 0.;
        this.dUdEx[0] = 0.;
        this.dVdEx[0] = 0.;
        this.dUdEy[0] = 0.;
        this.dVdEy[0] = 0.;
        this.dUdIx[0] = 0.;
        this.dVdIx[0] = 0.;
        this.dUdIy[0] = 0.;
        this.dVdIy[0] = 0.;

        // Power tables computation
        // Data are computed once and for all
        for (int i = 1; i < this.e2n.length; i++) {
            this.e2n[i] = this.e2 * this.e2n[i - 1];
        }
        for (int i = 1; i < this.cosi2n.length; i++) {
            this.cosi2n[i] = this.cosi2 * this.cosi2n[i - 1];
        }
        for (int i = 1; i < this.sin2i2n.length; i++) {
            this.sin2i2n[i] = sin2i2 * this.sin2i2n[i - 1];
        }
        for (int i = 1; i < this.etaN.length; i++) {
            this.etaN[i] = this.eta * this.etaN[i - 1];
        }
        for (int i = 1; i < this.r0SmaN.length; i++) {
            this.r0SmaN[i] = r0Sma * this.r0SmaN[i - 1];
        }

        // Sequences computation : Compute the u and v sequence terms (and their partial derivative sequence terms)
        // This part contains the recurrence part of the formulation
        for (int i = 1; i < this.u.length; i++) {
            this.u[i] = (this.u[i - 1] * eixy - this.v[i - 1] * eiyx);
            this.v[i] = (this.v[i - 1] * eixy + this.u[i - 1] * eiyx);

            this.dUdEx[i] = (eixy * this.dUdEx[i - 1] - eiyx * this.dVdEx[i - 1]
                    + this.ix * this.u[i - 1] + this.iy * this.v[i - 1]);
            this.dVdEx[i] = (eixy * this.dVdEx[i - 1] + eiyx * this.dUdEx[i - 1]
                    + this.ix * this.v[i - 1] - this.iy * this.u[i - 1]);

            this.dUdEy[i] = (eixy * this.dUdEy[i - 1] - eiyx * this.dVdEy[i - 1]
                    + this.iy * this.u[i - 1] - this.ix * this.v[i - 1]);
            this.dVdEy[i] = (eixy * this.dVdEy[i - 1] + eiyx * this.dUdEy[i - 1]
                    + this.iy * this.v[i - 1] + this.ix * this.u[i - 1]);

            this.dUdIx[i] = (eixy * this.dUdIx[i - 1] - eiyx * this.dVdIx[i - 1]
                    + this.ex * this.u[i - 1] - this.ey * this.v[i - 1]);
            this.dVdIx[i] = (eixy * this.dVdIx[i - 1] + eiyx * this.dUdIx[i - 1]
                    + this.ex * this.v[i - 1] + this.ey * this.u[i - 1]);

            this.dUdIy[i] = (eixy * this.dUdIy[i - 1] - eiyx * this.dVdIy[i - 1]
                    + this.ey * this.u[i - 1] + this.ex * this.v[i - 1]);
            this.dVdIy[i] = (eixy * this.dVdIy[i - 1] + eiyx * this.dUdIy[i - 1]
                    + this.ey * this.v[i - 1] - this.ex * this.u[i - 1]);
        }
    }

    /**
     * Compute the mean potential U at a given order of development (even case).<br>
     * (FAST NT-zonaux-hautsdegres) Eq. (42).
     * 
     * @param np
     *        n' (order of development)
     * @return U (even case)
     */
    public double computeEvenMeanPotential(final int np) {

        // variable initialization
        double s1 = 0.;
        double s2 = 0.;
        double s3 = 0.;

        // variable declaration
        double cos;
        double sin;
        double alpha;
        double e;
        double uq;

        // c1 definition
        final double c1 = MathLib.divide(-this.muSma * this.r0SmaN[2 * np], this.etaN[4 * np - 1]);

        // s1 calculation
        for (int kp2 = 0; kp2 <= np; kp2++) {
            for (int kp1 = 0; kp1 <= np - 1; kp1++) {
                final int lp2 = 0;

                cos = this.cosi2n[2 * kp2];
                sin = this.sin2i2n[kp2];
                alpha = this.alphaCoef[np][1][kp2][kp1][lp2];
                e = this.e2n[kp1];

                s1 += this.powOfTwo[2 * kp2] * cos * sin * alpha * e;
            }
        }

        // s2 calculation
        for (int kp2 = 1; kp2 <= np; kp2++) {
            for (int kp1 = 1; kp1 <= kp2 - 1; kp1++) {
                for (int lp2 = 1; lp2 <= kp1; lp2++) {

                    cos = this.cosi2n[2 * kp2];
                    sin = this.sin2i2n[kp2 - lp2];
                    alpha = this.alphaCoef[np][2][kp2][kp1][lp2];
                    e = this.e2n[kp1 - lp2];
                    uq = this.u[2 * lp2];

                    s2 += this.powOfTwo[2 * kp2] * cos * sin * alpha * e * uq;
                }
            }
        }

        // s3 calculation
        for (int kp2 = 1; kp2 <= np - 1; kp2++) {
            for (int kp1 = kp2; kp1 <= np - 1; kp1++) {
                for (int lp2 = 1; lp2 <= kp2; lp2++) {

                    cos = this.cosi2n[2 * kp2];
                    sin = this.sin2i2n[kp2 - lp2];
                    alpha = this.alphaCoef[np][3][kp2][kp1][lp2];
                    e = this.e2n[kp1 - lp2];
                    uq = this.u[2 * lp2];

                    s3 += this.powOfTwo[2 * kp2] * cos * sin * alpha * e * uq;
                }
            }
        }

        // mean potential U at a given order of development (np)
        return c1 * (s1 + s2 + s3);
    }

    /**
     * Compute the mean potential (at a given order of development) partial derivatives with respect to a (even
     * case).<br>
     * (FAST NT-zonaux-hautsdegres) Eq. (67).
     * 
     * @param np
     *        n' (order of development)
     * @param meanU
     *        U (even)
     * @return dU/da (even case)
     */
    public double computeEvenParDerA(final int np, final double meanU) {
        return MathLib.divide(-(2 * np + 1) * meanU, this.a);
    }

    /**
     * Compute the mean potential (at a given order of development) partial derivatives with respect to ex (even
     * case).<br>
     * (FAST NT-zonaux-hautsdegres) Eq. (7).
     * 
     * @param np
     *        n' (order of development)
     * @param meanU
     *        U (even)
     * @return dU/dex
     */
    public double computeEvenParDerEx(final int np, final double meanU) {

        // variable declaration
        double s1 = 0.;
        double s2 = 0.;
        double s3 = 0.;
        final double s4;
        final double s5;

        // c1 and c2 definition
        final double c1 = MathLib.divide((4 * np - 1) * this.ex * meanU, this.eta * this.eta);
        final double c2 = MathLib.divide(-this.muSma * this.r0SmaN[2 * np], this.etaN[4 * np - 1]);

        if (Double.compare(this.ex, 0.) != 0) {

            // s1 calculation
            s1 = computeEvenParDerS1Exy(np, this.ex);

            // s2 calculation
            s2 = computeEvenParDerS2Exy(np, this.ex);

            // s3 calculation
            s3 = computeEvenParDerS3Exy(np, this.ex);
        }

        // s4 calculation
        s4 = computeEvenParDerS4Exy(np, this.dUdEx);

        // s5 calculation
        s5 = computeEvenParDerS5Exy(np, this.dUdEx);

        // mean potential (at a given order of development (np)) partial derivatives
        return c1 + c2 * (s1 + s2 + s3) + c2 * (s4 + s5);
    }

    /**
     * Compute the mean potential (at a given order of development) partial derivatives with respect to ey (even
     * case).<br>
     * (FAST NT-zonaux-hautsdegres) Eq. (74).
     * 
     * @param np
     *        n' (order of development)
     * @param meanU
     *        U (even)
     * @return dU/dey
     */
    public double computeEvenParDerEy(final int np, final double meanU) {

        // variable declaration
        double s1 = 0.;
        double s2 = 0.;
        double s3 = 0.;
        final double s4;
        final double s5;

        // c1 and c2 definition
        final double c1 = MathLib.divide((4 * np - 1) * this.ey * meanU, this.eta * this.eta);
        final double c2 = MathLib.divide(-this.muSma * this.r0SmaN[2 * np], this.etaN[4 * np - 1]);

        if (Double.compare(this.ey, 0.) != 0) {

            // s1 calculation
            s1 = computeEvenParDerS1Exy(np, this.ey);

            // s2 calculation
            s2 = computeEvenParDerS2Exy(np, this.ey);

            // s3 calculation
            s3 = computeEvenParDerS3Exy(np, this.ey);
        }

        // s4 calculation
        s4 = computeEvenParDerS4Exy(np, this.dUdEy);

        // s5 calculation
        s5 = computeEvenParDerS5Exy(np, this.dUdEy);

        // mean potential (at a given order of development (np)) partial derivatives with respect to ey
        return c1 + c2 * (s1 + s2 + s3) + c2 * (s4 + s5);
    }

    /**
     * Compute the mean potential (at a given order of development) partial derivatives with respect to ix (even
     * case).<br>
     * (FAST NT-zonaux-hautsdegres) Eq. (77).
     * 
     * @param np
     *        n' (order of development)
     * @param meanU
     *        U (even)
     * @return dU/dix
     */
    public double computeEvenParDerIx(final int np, final double meanU) {

        // variable declaration
        double s1 = 0.;
        double s2 = 0.;
        double s3 = 0.;
        final double s4;
        final double s5;

        // c1 definition
        final double c1 = MathLib.divide(-this.muSma * this.r0SmaN[2 * np], this.etaN[4 * np - 1]);

        if (Double.compare(this.ix, 0.) != 0) {

            // s1 calculation
            s1 = computeEvenParDerS1Ixy(np, this.ix);

            // s2 calculation
            s2 = computeEvenParDerS2Ixy(np, this.ix);

            // s3 calculation
            s3 = computeEvenParDerS3Ixy(np, this.ix);
        }

        // s4 calculation
        s4 = computeEvenParDerS4Ixy(np, this.dUdIx);

        // s5 calculation
        s5 = computeEvenParDerS5Ixy(np, this.dUdIx);

        // mean potential (at a given order of development (np)) partial derivatives with respect to ix
        return c1 * (s1 + s2 + s3) + c1 * (s4 + s5);
    }

    /**
     * Compute the mean potential (at a given order of development) partial derivatives with respect to iy (even
     * case).<br>
     * (FAST NT-zonaux-hautsdegres) Eq. (78).
     * 
     * @param np
     *        n' (order of development)
     * @param meanU
     *        U (even)
     * @return dU/diy
     */
    public double computeEvenParDerIy(final int np, final double meanU) {

        // variable declaration
        double s1 = 0.;
        double s2 = 0.;
        double s3 = 0.;
        final double s4;
        final double s5;

        // c1 definition
        final double c1 = MathLib.divide(-this.muSma * this.r0SmaN[2 * np], this.etaN[4 * np - 1]);

        if (Double.compare(this.iy, 0.) != 0) {

            // s1 calculation
            s1 = computeEvenParDerS1Ixy(np, this.iy);

            // s2 calculation
            s2 = computeEvenParDerS2Ixy(np, this.iy);

            // s3 calculation
            s3 = computeEvenParDerS3Ixy(np, this.iy);
        }

        // s4 calculation
        s4 = computeEvenParDerS4Ixy(np, this.dUdIy);

        // s5 calculation
        s5 = computeEvenParDerS5Ixy(np, this.dUdIy);

        // mean potential (at a given order of development (np)) partial derivatives with respect to iy
        return c1 * (s1 + s2 + s3) + c1 * (s4 + s5);
    }

    /**
     * Compute the mean potential U at a given order of development (odd case).<br>
     * (FAST NT-zonaux-hautsdegres) Eq. (46).
     * 
     * @param np
     *        n' (order of development)
     * @return U (odd case)
     */
    public double computeOddMeanPotential(final int np) {

        // variable initialization
        double s1 = 0.;
        double s2 = 0.;

        // variable declaration
        double cos;
        double sin;
        double beta;
        double e;
        double vq;

        // c1 definition
        final double c1 = MathLib.divide(-this.muSma * this.r0SmaN[2 * np + 1], this.etaN[4 * np + 1]);

        // s1 calculation
        for (int kp2 = 1; kp2 <= np; kp2++) {
            for (int kp1 = 0; kp1 <= kp2; kp1++) {
                for (int lp2 = 0; lp2 <= kp1; lp2++) {

                    cos = this.cosi2n[2 * kp2 + 1];
                    sin = this.sin2i2n[kp2 - lp2];
                    beta = this.betaCoef[np][1][kp2][kp1][lp2];
                    e = this.e2n[kp1 - lp2];
                    vq = this.v[2 * lp2 + 1];

                    s1 += this.powOfTwo[2 * kp2 + 1] * cos * sin * beta * e * vq;
                }
            }
        }

        // s2 calculation
        for (int kp2 = 0; kp2 <= np - 1; kp2++) {
            for (int kp1 = kp2; kp1 <= np - 1; kp1++) {
                for (int lp2 = 0; lp2 <= kp2; lp2++) {

                    cos = this.cosi2n[2 * kp2 + 1];
                    sin = this.sin2i2n[kp2 - lp2];
                    beta = this.betaCoef[np][2][kp2][kp1][lp2];
                    e = this.e2n[kp1 - lp2];
                    vq = this.v[2 * lp2 + 1];

                    s2 += this.powOfTwo[2 * kp2 + 1] * cos * sin * beta * e * vq;
                }
            }
        }

        // mean potential U at a given order of development (np)
        return c1 * (s1 + s2);
    }

    /**
     * Compute the mean potential (at a given order of development) partial derivatives with respect to a (odd
     * case).<br>
     * (FAST NT-zonaux-hautsdegres) Eq. (68).
     * 
     * @param np
     *        n' (order of development)
     * @param meanU
     *        U (odd)
     * @return dU/da (odd case)
     */
    public double computeOddParDerA(final int np, final double meanU) {
        return MathLib.divide(-(2. * np + 2.) * meanU, this.a);
    }

    /**
     * Compute the mean potential (at a given order of development) partial derivatives with respect to ex (odd
     * case).<br>
     * (FAST NT-zonaux-hautsdegres) Eq. (75).
     * 
     * @param np
     *        n' (order of development)
     * @param meanU
     *        U (odd)
     * @return dU/dex (odd case)
     */
    public double computeOddParDerEx(final int np, final double meanU) {

        // variable declaration
        double s1 = 0.;
        double s2 = 0.;
        final double s3;
        final double s4;

        // c1 and c2 definition
        final double c1 = MathLib.divide((4 * np + 1) * this.ex * meanU, this.eta * this.eta);
        final double c2 = MathLib.divide(-this.muSma * this.r0SmaN[2 * np + 1], this.etaN[4 * np + 1]);

        if (Double.compare(this.ex, 0.) != 0) {

            // s1 calculation
            s1 = computeOddParDerS1Exy(np, this.ex);

            // s2 calculation
            s2 = computeOddParDerS2Exy(np, this.ex);
        }

        // s3 calculation
        s3 = computeOddParDerS3Exy(np, this.dVdEx);

        // s4 calculation
        s4 = computeOddParDerS4Exy(np, this.dVdEx);

        // mean potential (at a given order of development (np)) partial derivatives with respect to ex (odd case)
        return c1 + c2 * (s1 + s2) + c2 * (s3 + s4);
    }

    /**
     * Compute the mean potential (at a given order of development) partial derivatives with respect to ey (odd
     * case).<br>
     * (FAST NT-zonaux-hautsdegres) Eq. (76).
     * 
     * @param np
     *        n' (order of development)
     * @param meanU
     *        U (odd)
     * @return dU/dey (odd case)
     */
    public double computeOddParDerEy(final int np, final double meanU) {

        // variable declaration
        double s1 = 0.;
        double s2 = 0.;
        final double s3;
        final double s4;

        // c1 and c2 definition
        final double c1 = MathLib.divide((4 * np + 1) * this.ey * meanU, this.eta * this.eta);
        final double c2 = MathLib.divide(-this.muSma * this.r0SmaN[2 * np + 1], this.etaN[4 * np + 1]);

        if (Double.compare(this.ey, 0.) != 0) {

            // s1 calculation
            s1 = computeOddParDerS1Exy(np, this.ey);

            // s2 calculation
            s2 = computeOddParDerS2Exy(np, this.ey);
        }

        // s3 calculation
        s3 = computeOddParDerS3Exy(np, this.dVdEy);

        // s4 calculation
        s4 = computeOddParDerS4Exy(np, this.dVdEy);

        // mean potential (at a given order of development (np)) partial derivatives with respect to ey (odd case)
        return c1 + c2 * (s1 + s2) + c2 * (s3 + s4);
    }

    /**
     * Compute the mean potential (at a given order of development) partial derivatives with respect to ix (odd
     * case).<br>
     * (FAST NT-zonaux-hautsdegres) Eq. (79).
     * 
     * @param np
     *        n' (order of development)
     * @param meanU
     *        U (odd)
     * @return dU/dix (odd case)
     */
    public double computeOddParDerIx(final int np, final double meanU) {

        // variable declaration
        double s1 = 0.;
        double s2 = 0.;
        final double s3;
        final double s4;

        // c1 definition
        final double c1 = MathLib.divide(-this.muSma * this.r0SmaN[2 * np + 1], this.etaN[4 * np + 1]);

        if (Double.compare(this.ix, 0.) != 0) {

            // s1 calculation
            s1 = computeOddParDerS1Ixy(np, this.ix);

            // s2 calculation
            s2 = computeOddParDerS2Ixy(np, this.ix);
        }

        // s3 calculation
        s3 = computeOddParDerS3Ixy(np, this.dVdIx);

        // s4 calculation
        s4 = computeOddParDerS4Ixy(np, this.dVdIx);

        // mean potential (at a given order of development (np)) partial derivatives with respect to ix (odd case)
        return c1 * (s1 + s2) + c1 * (s3 + s4);
    }

    /**
     * Compute the mean potential (at a given order of development) partial derivatives with respect to iy (odd
     * case).<br>
     * (FAST NT-zonaux-hautsdegres) Eq. (80).
     * 
     * @param np
     *        n' (order of development)
     * @param meanU
     *        U (odd)
     * @return dU/diy (odd case)
     */
    public double computeOddParDerIy(final int np, final double meanU) {

        // variable declaration
        double s1 = 0.;
        double s2 = 0.;
        final double s3;
        final double s4;

        // c1 definition
        final double c1 = MathLib.divide(-this.muSma * this.r0SmaN[2 * np + 1], this.etaN[4 * np + 1]);

        if (Double.compare(this.iy, 0.) != 0) {

            // s1 calculation
            s1 = computeOddParDerS1Ixy(np, this.iy);

            // s2 calculation
            s2 = computeOddParDerS2Ixy(np, this.iy);
        }

        // s3 calculation
        s3 = computeOddParDerS3Ixy(np, this.dVdIy);

        // s4 calculation
        s4 = computeOddParDerS4Ixy(np, this.dVdIy);

        // mean potential (at a given order of development (np)) partial derivatives with respect to iy (odd case)
        return c1 * (s1 + s2) + c1 * (s3 + s4);
    }

    /**
     * Compute s1 term for even partial derivative Ex/Ey.
     * 
     * @param np
     *        n' (order of development)
     * @param exy
     *        ex or ey value
     * @return s1 term
     */
    private double computeEvenParDerS1Exy(final int np, final double exy) {

        // variable declaration
        double s1 = 0.;
        double cos;
        double sin;
        double alpha;
        double e;

        for (int kp2 = 0; kp2 <= np; kp2++) {
            for (int kp1 = 0; kp1 <= np - 1; kp1++) {
                final int lp2 = 0;

                cos = this.cosi2n[2 * kp2];
                sin = this.sin2i2n[kp2];
                alpha = this.alphaCoef[np][1][kp2][kp1][lp2];
                e = computePowOfE2(kp1 - 1);

                s1 += this.powOfTwo[2 * kp2] * cos * sin * alpha * kp1 * 2. * exy * e;
            }
        }

        return s1;
    }

    /**
     * Compute s2 term for even partial derivative Ex/Ey.
     * 
     * @param np
     *        n' (order of development)
     * @param exy
     *        ex or ey value
     * @return s2 term
     */
    private double computeEvenParDerS2Exy(final int np, final double exy) {

        // variable declaration
        double s2 = 0.;
        double cos;
        double sin;
        double alpha;
        double e;
        double uq;

        for (int kp2 = 1; kp2 <= np; kp2++) {
            for (int kp1 = 1; kp1 <= kp2 - 1; kp1++) {
                for (int lp2 = 1; lp2 <= kp1; lp2++) {

                    cos = this.cosi2n[2 * kp2];
                    sin = this.sin2i2n[kp2 - lp2];
                    alpha = this.alphaCoef[np][2][kp2][kp1][lp2];
                    e = computePowOfE2(kp1 - lp2 - 1);
                    uq = this.u[2 * lp2];

                    s2 += this.powOfTwo[2 * kp2] * cos * sin * alpha * (kp1 - lp2) * 2. * exy * e * uq;
                }
            }
        }

        return s2;
    }

    /**
     * Compute s3 term for even partial derivative Ex/Ey.
     * 
     * @param np
     *        n' (order of development)
     * @param exy
     *        ex or ey value
     * @return s3 term
     */
    private double computeEvenParDerS3Exy(final int np, final double exy) {

        // variable declaration
        double s3 = 0.;
        double cos;
        double sin;
        double alpha;
        double e;
        double uq;

        for (int kp2 = 1; kp2 <= np - 1; kp2++) {
            for (int kp1 = kp2; kp1 <= np - 1; kp1++) {
                for (int lp2 = 1; lp2 <= kp2; lp2++) {

                    cos = this.cosi2n[2 * kp2];
                    sin = this.sin2i2n[kp2 - lp2];
                    alpha = this.alphaCoef[np][3][kp2][kp1][lp2];
                    e = computePowOfE2(kp1 - lp2 - 1);
                    uq = this.u[2 * lp2];

                    s3 += this.powOfTwo[2 * kp2] * cos * sin * alpha * (kp1 - lp2) * 2. * exy * e * uq;
                }
            }
        }

        return s3;
    }

    /**
     * Compute s4 term for even partial derivative Ex/Ey.
     * 
     * @param np
     *        n' (order of development)
     * @param dUdExy
     *        dUdEx or dUdEy array
     * @return s4 term
     */
    private double computeEvenParDerS4Exy(final int np, final double[] dUdExy) {

        // variable declaration
        double s4 = 0.;
        double cos;
        double sin;
        double alpha;
        double e;
        double parDerUqExy;

        for (int kp2 = 1; kp2 <= np; kp2++) {
            for (int kp1 = 1; kp1 <= kp2 - 1; kp1++) {
                for (int lp2 = 1; lp2 <= kp1; lp2++) {

                    cos = this.cosi2n[2 * kp2];
                    sin = this.sin2i2n[kp2 - lp2];
                    alpha = this.alphaCoef[np][2][kp2][kp1][lp2];
                    e = this.e2n[kp1 - lp2];
                    parDerUqExy = dUdExy[2 * lp2];

                    s4 += this.powOfTwo[2 * kp2] * cos * sin * alpha * e * parDerUqExy;
                }
            }
        }

        return s4;
    }

    /**
     * Compute s5 term for even partial derivative Ex/Ey.
     * 
     * @param np
     *        n' (order of development)
     * @param dUdExy
     *        dUdEx or dUdEy array
     * @return s5 term
     */
    private double computeEvenParDerS5Exy(final int np, final double[] dUdExy) {

        // variable declaration
        double s5 = 0.;
        double cos;
        double sin;
        double alpha;
        double e;
        double parDerUqExy;

        for (int kp2 = 1; kp2 <= np - 1; kp2++) {
            for (int kp1 = kp2; kp1 <= np - 1; kp1++) {
                for (int lp2 = 1; lp2 <= kp2; lp2++) {

                    cos = this.cosi2n[2 * kp2];
                    sin = this.sin2i2n[kp2 - lp2];
                    alpha = this.alphaCoef[np][3][kp2][kp1][lp2];
                    e = this.e2n[kp1 - lp2];
                    parDerUqExy = dUdExy[2 * lp2];

                    s5 += this.powOfTwo[2 * kp2] * cos * sin * alpha * e * parDerUqExy;
                }
            }
        }

        return s5;
    }

    /**
     * Compute s1 term for even partial derivative Ix/Iy.
     * 
     * @param np
     *        n' (order of development)
     * @param ixy
     *        ix or iy value
     * @return s1 term
     */
    private double computeEvenParDerS1Ixy(final int np, final double ixy) {

        // variable declaration
        double s1 = 0.;
        double parDerTermI;
        double alpha;
        double e;
        final int lp2 = 0;

        for (int kp2 = 0; kp2 <= np; kp2++) {
            for (int kp1 = 0; kp1 <= np - 1; kp1++) {

                parDerTermI = computeParDerTermI(ixy, 2 * kp2, kp2);
                alpha = this.alphaCoef[np][1][kp2][kp1][lp2];
                e = this.e2n[kp1];

                s1 += this.powOfTwo[2 * kp2] * parDerTermI * alpha * e;
            }
        }

        return s1;
    }

    /**
     * Compute s2 term for even partial derivative Ix/Iy.
     * 
     * @param np
     *        n' (order of development)
     * @param ixy
     *        ix or iy value
     * @return s2 term
     */
    private double computeEvenParDerS2Ixy(final int np, final double ixy) {

        // variable declaration
        double s2 = 0.;
        double parDerTermI;
        double alpha;
        double e;
        double uq;

        for (int kp2 = 1; kp2 <= np; kp2++) {
            for (int kp1 = 1; kp1 <= kp2 - 1; kp1++) {
                for (int lp2 = 1; lp2 <= kp1; lp2++) {

                    parDerTermI = computeParDerTermI(ixy, 2 * kp2, kp2 - lp2);
                    alpha = this.alphaCoef[np][2][kp2][kp1][lp2];
                    e = this.e2n[kp1 - lp2];
                    uq = this.u[2 * lp2];

                    s2 += this.powOfTwo[2 * kp2] * parDerTermI * alpha * e * uq;
                }
            }
        }

        return s2;
    }

    /**
     * Compute s3 term for even partial derivative Ix/Iy.
     * 
     * @param np
     *        n' (order of development)
     * @param exy
     *        ix or iy value
     * @return s3 term
     */
    private double computeEvenParDerS3Ixy(final int np, final double ixy) {

        // variable declaration
        double s3 = 0.;
        double parDerTermI;
        double alpha;
        double e;
        double uq;

        for (int kp2 = 1; kp2 <= np - 1; kp2++) {
            for (int kp1 = kp2; kp1 <= np - 1; kp1++) {
                for (int lp2 = 1; lp2 <= kp2; lp2++) {

                    parDerTermI = computeParDerTermI(ixy, 2 * kp2, kp2 - lp2);
                    alpha = this.alphaCoef[np][3][kp2][kp1][lp2];
                    e = this.e2n[kp1 - lp2];
                    uq = this.u[2 * lp2];

                    s3 += this.powOfTwo[2 * kp2] * parDerTermI * alpha * e * uq;
                }
            }
        }

        return s3;
    }

    /**
     * Compute s4 term for even partial derivative Ix/Iy.
     * 
     * @param np
     *        n' (order of development)
     * @param dUdIxy
     *        dUdIx or dUdIy array
     * @return s4 term
     */
    private double computeEvenParDerS4Ixy(final int np, final double[] dUdIxy) {

        // variable declaration
        double s4 = 0.;
        double cos;
        double sin;
        double alpha;
        double e;
        double parDerUqIxy;

        for (int kp2 = 1; kp2 <= np; kp2++) {
            for (int kp1 = 1; kp1 <= kp2 - 1; kp1++) {
                for (int lp2 = 1; lp2 <= kp1; lp2++) {

                    cos = this.cosi2n[2 * kp2];
                    sin = this.sin2i2n[kp2 - lp2];
                    alpha = this.alphaCoef[np][2][kp2][kp1][lp2];
                    e = this.e2n[kp1 - lp2];
                    parDerUqIxy = dUdIxy[2 * lp2];

                    s4 += this.powOfTwo[2 * kp2] * cos * sin * alpha * e * parDerUqIxy;
                }
            }
        }

        return s4;
    }

    /**
     * Compute s5 term for even partial derivative Ix/Iy.
     * 
     * @param np
     *        n' (order of development)
     * @param dUdIxy
     *        dUdIx or dUdIy array
     * @return s5 term
     */
    private double computeEvenParDerS5Ixy(final int np, final double[] dUdIxy) {

        // variable declaration
        double s5 = 0.;
        double cos;
        double sin;
        double alpha;
        double e;
        double parDerUqIxy;

        for (int kp2 = 1; kp2 <= np - 1; kp2++) {
            for (int kp1 = kp2; kp1 <= np - 1; kp1++) {
                for (int lp2 = 1; lp2 <= kp2; lp2++) {

                    cos = this.cosi2n[2 * kp2];
                    sin = this.sin2i2n[kp2 - lp2];
                    alpha = this.alphaCoef[np][3][kp2][kp1][lp2];
                    e = this.e2n[kp1 - lp2];
                    parDerUqIxy = dUdIxy[2 * lp2];

                    s5 += this.powOfTwo[2 * kp2] * cos * sin * alpha * e * parDerUqIxy;
                }
            }
        }

        return s5;
    }

    /**
     * Compute s1 term for odd partial derivative Ex/Ey.
     * 
     * @param np
     *        n' (order of development)
     * @param exy
     *        ex or ey value
     * @return s1 term
     */
    private double computeOddParDerS1Exy(final int np, final double exy) {

        // variable declaration
        double s1 = 0.;
        double cos;
        double sin;
        double beta;
        double e;
        double vq;

        for (int kp2 = 1; kp2 <= np; kp2++) {
            for (int kp1 = 0; kp1 <= kp2; kp1++) {
                for (int lp2 = 0; lp2 <= kp1; lp2++) {

                    cos = this.cosi2n[2 * kp2 + 1];
                    sin = this.sin2i2n[kp2 - lp2];
                    beta = this.betaCoef[np][1][kp2][kp1][lp2];
                    e = computePowOfE2(kp1 - lp2 - 1);
                    vq = this.v[2 * lp2 + 1];

                    s1 += this.powOfTwo[2 * kp2 + 1] * cos * sin * beta * (kp1 - lp2) * 2. * exy * e * vq;
                }
            }
        }

        return s1;
    }

    /**
     * Compute s2 term for odd partial derivative Ex/Ey.
     * 
     * @param np
     *        n' (order of development)
     * @param exy
     *        ex or ey value
     * @return s2 term
     */
    private double computeOddParDerS2Exy(final int np, final double exy) {

        // variable declaration
        double s2 = 0.;
        double cos;
        double sin;
        double beta;
        double e;
        double vq;

        for (int kp2 = 0; kp2 <= np - 1; kp2++) {
            for (int kp1 = kp2; kp1 <= np - 1; kp1++) {
                for (int lp2 = 0; lp2 <= kp2; lp2++) {

                    cos = this.cosi2n[2 * kp2 + 1];
                    sin = this.sin2i2n[kp2 - lp2];
                    beta = this.betaCoef[np][2][kp2][kp1][lp2];
                    e = computePowOfE2(kp1 - lp2 - 1);
                    vq = this.v[2 * lp2 + 1];

                    s2 += this.powOfTwo[2 * kp2 + 1] * cos * sin * beta * (kp1 - lp2) * 2. * exy * e * vq;
                }
            }
        }

        return s2;
    }

    /**
     * Compute s3 term for odd partial derivative Ex/Ey.
     * 
     * @param np
     *        n' (order of development)
     * @param dVdExy
     *        dVdEx or dVdEy array
     * @return s3 term
     */
    private double computeOddParDerS3Exy(final int np, final double[] dVdExy) {

        // variable declaration
        double s3 = 0.;
        double cos;
        double sin;
        double beta;
        double e;
        double parDerVqExy;

        for (int kp2 = 1; kp2 <= np; kp2++) {
            for (int kp1 = 0; kp1 <= kp2; kp1++) {
                for (int lp2 = 0; lp2 <= kp1; lp2++) {

                    cos = this.cosi2n[2 * kp2 + 1];
                    sin = this.sin2i2n[kp2 - lp2];
                    beta = this.betaCoef[np][1][kp2][kp1][lp2];
                    e = this.e2n[kp1 - lp2];
                    parDerVqExy = dVdExy[2 * lp2 + 1];

                    s3 += this.powOfTwo[2 * kp2 + 1] * cos * sin * beta * e * parDerVqExy;
                }
            }
        }

        return s3;
    }

    /**
     * Compute s4 term for odd partial derivative Ex/Ey.
     * 
     * @param np
     *        n' (order of development)
     * @param dVdExy
     *        dVdEx or dVdEy array
     * @return s4 term
     */
    private double computeOddParDerS4Exy(final int np, final double[] dVdExy) {

        // variable declaration
        double s4 = 0.;
        double cos;
        double sin;
        double beta;
        double e;
        double parDerVqExy;

        for (int kp2 = 0; kp2 <= np - 1; kp2++) {
            for (int kp1 = kp2; kp1 <= np - 1; kp1++) {
                for (int lp2 = 0; lp2 <= kp2; lp2++) {

                    cos = this.cosi2n[2 * kp2 + 1];
                    sin = this.sin2i2n[kp2 - lp2];
                    beta = this.betaCoef[np][2][kp2][kp1][lp2];
                    e = this.e2n[kp1 - lp2];
                    parDerVqExy = dVdExy[2 * lp2 + 1];

                    s4 += this.powOfTwo[2 * kp2 + 1] * cos * sin * beta * e * parDerVqExy;
                }
            }
        }

        return s4;
    }

    /**
     * Compute s1 term for odd partial derivative Ix/Iy.
     * 
     * @param np
     *        n' (order of development)
     * @param ixy
     *        ix or iy value
     * @return s1 term
     */
    private double computeOddParDerS1Ixy(final int np, final double ixy) {

        // variable declaration
        double s1 = 0.;
        double parDerTermI;
        double beta;
        double e;
        double vq;

        for (int kp2 = 1; kp2 <= np; kp2++) {
            for (int kp1 = 0; kp1 <= kp2; kp1++) {
                for (int lp2 = 0; lp2 <= kp1; lp2++) {

                    parDerTermI = computeParDerTermI(ixy, 2 * kp2 + 1, kp2 - lp2);
                    beta = this.betaCoef[np][1][kp2][kp1][lp2];
                    e = this.e2n[kp1 - lp2];
                    vq = this.v[2 * lp2 + 1];

                    s1 += this.powOfTwo[2 * kp2 + 1] * parDerTermI * beta * e * vq;
                }
            }
        }

        return s1;
    }

    /**
     * Compute s2 term for odd partial derivative Ix/Iy.
     * 
     * @param np
     *        n' (order of development)
     * @param ixy
     *        ix or iy value
     * @return s2 term
     */
    private double computeOddParDerS2Ixy(final int np, final double ixy) {

        // variable declaration
        double s2 = 0.;
        double parDerTermI;
        double beta;
        double e;
        double vq;

        for (int kp2 = 0; kp2 <= np - 1; kp2++) {
            for (int kp1 = kp2; kp1 <= np - 1; kp1++) {
                for (int lp2 = 0; lp2 <= kp2; lp2++) {

                    parDerTermI = computeParDerTermI(ixy, 2 * kp2 + 1, kp2 - lp2);
                    beta = this.betaCoef[np][2][kp2][kp1][lp2];
                    e = this.e2n[kp1 - lp2];
                    vq = this.v[2 * lp2 + 1];

                    s2 += this.powOfTwo[2 * kp2 + 1] * parDerTermI * beta * e * vq;
                }
            }
        }

        return s2;
    }

    /**
     * Compute s3 term for odd partial derivative Ix/Iy.
     * 
     * @param np
     *        n' (order of development)
     * @param dVdIxy
     *        dVdIx or dVdIy array
     * @return s3 term
     */
    private double computeOddParDerS3Ixy(final int np, final double[] dVdIxy) {

        // variable declaration
        double s3 = 0.;
        double cos;
        double sin;
        double beta;
        double e;
        double parDerVqIxy;

        for (int kp2 = 1; kp2 <= np; kp2++) {
            for (int kp1 = 0; kp1 <= kp2; kp1++) {
                for (int lp2 = 0; lp2 <= kp1; lp2++) {

                    cos = this.cosi2n[2 * kp2 + 1];
                    sin = this.sin2i2n[kp2 - lp2];
                    beta = this.betaCoef[np][1][kp2][kp1][lp2];
                    e = this.e2n[kp1 - lp2];
                    parDerVqIxy = dVdIxy[2 * lp2 + 1];

                    s3 += this.powOfTwo[2 * kp2 + 1] * cos * sin * beta * e * parDerVqIxy;
                }
            }
        }

        return s3;
    }

    /**
     * Compute s4 term for odd partial derivative Ix/Iy.
     * 
     * @param np
     *        n' (order of development)
     * @param dVdIxy
     *        dVdIx or dVdIy array
     * @return s4 term
     */
    private double computeOddParDerS4Ixy(final int np, final double[] dVdIxy) {

        // variable declaration
        double s4 = 0.;
        double cos;
        double sin;
        double beta;
        double e;
        double parDerVqIxy;

        for (int kp2 = 0; kp2 <= np - 1; kp2++) {
            for (int kp1 = kp2; kp1 <= np - 1; kp1++) {
                for (int lp2 = 0; lp2 <= kp2; lp2++) {

                    cos = this.cosi2n[2 * kp2 + 1];
                    sin = this.sin2i2n[kp2 - lp2];
                    beta = this.betaCoef[np][2][kp2][kp1][lp2];
                    e = this.e2n[kp1 - lp2];
                    parDerVqIxy = dVdIxy[2 * lp2 + 1];

                    s4 += this.powOfTwo[2 * kp2 + 1] * cos * sin * beta * e * parDerVqIxy;
                }
            }
        }

        return s4;
    }

    /**
     * Compute a partial derivative with respect to an inclination vector component (ix or iy).<br>
     * This is an intermediate computation step towards the mean potential partial derivatives computation.
     * 
     * @param ixOrIy
     *        ix or ix
     * @param j1
     *        order for cos
     * @param j2
     *        order for sin
     * @return d/d(ixOrIy)(cos(i/2)^j1 * (sin(i/2)^2)^j2)
     */
    private double computeParDerTermI(final double ixOrIy, final int j1, final int j2) {

        final double dcos;
        final double sin;
        if (j1 == 0) {
            dcos = 0.;
            sin = 0.;
        } else if (j1 == 1) {
            dcos = MathLib.divide(-j1 * ixOrIy, this.cosi2);
            sin = this.sin2i2n[j2];
        } else {
            dcos = -j1 * ixOrIy * this.cosi2n[j1 - 2];
            sin = this.sin2i2n[j2];
        }

        final double cos;
        final double dsin;
        if (j2 == 0) {
            cos = 0.;
            dsin = 0.;
        } else {
            cos = this.cosi2n[j1];
            dsin = 2. * j2 * ixOrIy * this.sin2i2n[j2 - 1];
        }

        return dcos * sin + cos * dsin;
    }

    /**
     * Compute power of e^2.
     * 
     * @param n
     *        power (n can be equal to -1)
     * @return (e^2)^n
     */
    private double computePowOfE2(final int n) {

        final double result;
        if (n == -1) {
            result = MathLib.divide(1., this.e2);
        } else {
            result = this.e2n[n];
        }

        return result;
    }
}
