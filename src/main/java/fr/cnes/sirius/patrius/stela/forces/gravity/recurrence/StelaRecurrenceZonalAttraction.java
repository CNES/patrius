package fr.cnes.sirius.patrius.stela.forces.gravity.recurrence;

import fr.cnes.sirius.patrius.forces.gravity.potential.PotentialCoefficientsProvider;
import fr.cnes.sirius.patrius.math.exception.NotPositiveException;
import fr.cnes.sirius.patrius.math.util.CombinatoricsUtils;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.stela.forces.gravity.AbstractStelaZonalAttraction;
import fr.cnes.sirius.patrius.stela.orbits.OrbitNatureConverter;
import fr.cnes.sirius.patrius.stela.orbits.StelaEquinoctialOrbit;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Class representing the Earth zonal harmonics computed using recurrence methods.
 * <p>
 * Computes Zonal perturbations, short periods and partial derivatives using recurrence methods depending on the degree
 * of development asked.
 * </p>
 * <p>
 * The class is adapted from STELA RecurrenceZonalAcc in
 * fr.cnes.los.stela.elib.business.implementation.earthpotential.zonal.
 * </p>
 * 
 * @concurrency conditionally thread-safe
 * @concurrency.comment thread safe if the PotentialCoefficientsProvider used is thread safe
 * 
 * @author Maxime Ecochard, Thibaut BONIT
 * HISTORY
 * VERSION:4.16:OPENFD-391:25/04/2025:[STELA-PATRIUS] Implementation zonaux par recurrence
 * END-HISTORY
 * @since 4.16
 */
public class StelaRecurrenceZonalAttraction extends AbstractStelaZonalAttraction {

    /** Serializable UID. */
    private static final long serialVersionUID = 2962447392932920914L;

    /** Initial value for Legendre coefficients. */
    private static final double INIT_VAL = -99.0;

    /** Constant 1.5. */
    private static final double ONE_POINT_FIVE = 1.5;

    /** Gravity field coefficients provider. */
    private final PotentialCoefficientsProvider provider;

    /** Indicate if the zonal perturbation are normalized Legendre Polynomials or not. */
    private final boolean isNormalizedLegendrePolynomials;

    /** Power of two. */
    private final double[] powOfTwo;

    /** Alpha coefficients. */
    private final double[][][][][] alphaCoef;

    /** Beta coefficients. */
    private final double[][][][][] betaCoef;

    /**
     * Simple constructor.
     * <p>
     * By default, this constructor enables the J2² computation and uses normalized Legendre Polynomials for the zonal
     * perturbation.
     * </p>
     * 
     * @param provider
     *        Gravity field coefficients provider
     * @param zonalDegreeMaxPerturbation
     *        degree of development for zonal perturbations
     * @throws PatriusException
     *         if the requested maximal degree exceeds the available degree
     * @throws NotPositiveException
     *         if {@code zonalDegreeMaxPerturbation < 0}
     */
    public StelaRecurrenceZonalAttraction(final PotentialCoefficientsProvider provider,
                                          final int zonalDegreeMaxPerturbation)
        throws PatriusException {
        this(provider, zonalDegreeMaxPerturbation, true, true);
    }

    /**
     * Constructor with J2<sup>2</sup> flag.
     * 
     * @param provider
     *        Gravity field coefficients provider
     * @param zonalDegreeMaxPerturbation
     *        degree of development for zonal perturbations
     * @param isJ2SquareComputed
     *        if {@code true}, J2² is computed
     * @param isNormalizedLegendrePolynomials
     *        if {@code true}, use normalized Legendre Polynomials for the zonal perturbation
     * @throws PatriusException
     *         if the requested maximal degree exceeds the available degree
     * @throws NotPositiveException
     *         if {@code zonalDegreeMaxPerturbation < 0}
     */
    public StelaRecurrenceZonalAttraction(final PotentialCoefficientsProvider provider,
                                          final int zonalDegreeMaxPerturbation, final boolean isJ2SquareComputed,
                                          final boolean isNormalizedLegendrePolynomials)
        throws PatriusException {
        super(zonalDegreeMaxPerturbation, isJ2SquareComputed);

        this.provider = provider;
        this.isNormalizedLegendrePolynomials = isNormalizedLegendrePolynomials;

        // Constants computation
        // Power of two computation. Using double prevent from int/long overflow which would occur at order 32/64.
        this.powOfTwo = new double[2 * this.zonalDegreeMaxPerturbation + 1];
        this.powOfTwo[0] = 1;
        for (int n = 1; n <= 2 * this.zonalDegreeMaxPerturbation; n++) {
            this.powOfTwo[n] = 2 * this.powOfTwo[n - 1];
        }

        // Compute the zonal coefficients Jn
        final int nPMaxAlpha = this.zonalDegreeMaxPerturbation / 2;
        final int nPMaxBeta = (this.zonalDegreeMaxPerturbation - 1) / 2;
        final int maxJnIndex = MathLib.max(2 * nPMaxAlpha, 2 * nPMaxBeta + 1);
        final double[] jn = this.provider.getJ(this.isNormalizedLegendrePolynomials, maxJnIndex);

        // Alpha coefficients computation
        this.alphaCoef = new double[nPMaxAlpha + 1][4][nPMaxAlpha + 1][nPMaxAlpha][nPMaxAlpha];
        for (int np = 1; np <= nPMaxAlpha; np++) {
            for (int i = 1; i <= 3; i++) {
                computeAplhaCoeff(np, i, jn);
            }
        }

        // Beta coefficients computation
        this.betaCoef = new double[nPMaxBeta + 1][3][nPMaxBeta + 1][nPMaxBeta + 1][nPMaxBeta + 1];
        for (int np = 1; np <= nPMaxBeta; np++) {
            for (int i = 1; i <= 2; i++) {
                computeBetaCoeff(np, i, jn);
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * <b>Note: the short periods (forces switches and degrees) are not used for this force model (return 0 array).</b>
     * </p>
     */
    @Override
    public double[] computeShortPeriods(final StelaEquinoctialOrbit orbit, final OrbitNatureConverter converter) {
        return new double[] { 0., 0., 0., 0., 0., 0. };
    }

    /** {@inheritDoc} */
    @Override
    public double[][] computePartialDerivatives(final StelaEquinoctialOrbit orbit) throws PatriusException {
        // Initialization
        final StelaRecurrenceZonalEquation zonalEq = buildStelaRecurrenceZonalEquation(orbit);

        // Jn zonal terms computation
        final double[][] nPartialDerivatives = new double[this.zonalDegreeMaxPerturbation + 1][6];
        for (int n = 2; n <= this.zonalDegreeMaxPerturbation; n++) {
            final double[] nDegParDer = nDegZonalPartialDerivatives(zonalEq, n);
            nPartialDerivatives[n] = nDegParDer;
        }

        return nPartialDerivatives;
    }

    /** {@inheritDoc} */
    @Override
    public double[] computePerturbation(final StelaEquinoctialOrbit orbit) throws PatriusException {

        // Compute the partial derivatives
        final double[][] nPartialDerivatives = computePartialDerivatives(orbit);

        // Add all contributions to compute zonal terms J2 to Jn (n: order of potential development)
        final double[] zonalPartialDerivatives = new double[6];
        for (int n = 2; n <= this.zonalDegreeMaxPerturbation; n++) {
            for (int i = 0; i < 6; i++) {
                zonalPartialDerivatives[i] += nPartialDerivatives[n][i];
            }
        }

        // Conversion of terms J2 to Jn using Poisson brackets
        final double[] dZonal = getPlanEq(orbit, zonalPartialDerivatives);

        // J22 must only be activated if order is higher than 2
        if (this.zonalDegreeMaxPerturbation >= 2 && this.isJ2SquareComputed) {
            // J22 (does not need conversion using Poisson brackets)
            final double[] derParUdeg22 = computeJ2Square(orbit);
            for (int i = 0; i < dZonal.length; i++) {
                dZonal[i] += derParUdeg22[i];
            }
        }

        return dZonal;
    }

    /** {@inheritDoc} */
    @Override
    public double[] computeJ2Square(final StelaEquinoctialOrbit orbit) throws PatriusException {
        return derParUdeg22(orbit);
    }

    /**
     * Build the {@link StelaRecurrenceZonalEquation} object from this class initialized parameters.
     * 
     * @return the {@link StelaRecurrenceZonalEquation} object
     */
    public StelaRecurrenceZonalEquation buildStelaRecurrenceZonalEquation(final StelaEquinoctialOrbit orbit) {
        return new StelaRecurrenceZonalEquation(orbit, this.zonalDegreeMaxPerturbation, this.powOfTwo, this.alphaCoef,
            this.betaCoef);
    }

    /**
     * Compute Jn zonal term (n: order of potential development).
     * 
     * @param zonalEq
     *        zonal terms
     * @param n
     *        order
     * @return Jn zonal term
     */
    public double[] nDegZonalPartialDerivatives(final StelaRecurrenceZonalEquation zonalEq, final int n) {

        final double betaN;
        if (this.isNormalizedLegendrePolynomials) {
            betaN = MathLib.sqrt(2. * n + 1.);
        } else {
            betaN = 1.;
        }

        final double meanU;
        final double parDerA;
        final double parDerXi = 0.;
        final double parDerEx;
        final double parDerEy;
        final double parDerIx;
        final double parDerIy;

        if (n % 2 == 0) {
            // Even case
            final int np = n / 2;
            meanU = zonalEq.computeEvenMeanPotential(np);
            parDerA = zonalEq.computeEvenParDerA(np, meanU) / betaN;
            parDerEx = zonalEq.computeEvenParDerEx(np, meanU) / betaN;
            parDerEy = zonalEq.computeEvenParDerEy(np, meanU) / betaN;
            parDerIx = zonalEq.computeEvenParDerIx(np, meanU) / betaN;
            parDerIy = zonalEq.computeEvenParDerIy(np, meanU) / betaN;

        } else {
            // Odd case
            final int np = (n - 1) / 2;
            meanU = zonalEq.computeOddMeanPotential(np);
            parDerA = zonalEq.computeOddParDerA(np, meanU) / betaN;
            parDerEx = zonalEq.computeOddParDerEx(np, meanU) / betaN;
            parDerEy = zonalEq.computeOddParDerEy(np, meanU) / betaN;
            parDerIx = zonalEq.computeOddParDerIx(np, meanU) / betaN;
            parDerIy = zonalEq.computeOddParDerIy(np, meanU) / betaN;
        }

        return new double[] { parDerA, parDerXi, parDerEx, parDerEy, parDerIx, parDerIy };
    }

    /**
     * Partial derivative due to 2nd order Earth potential zonal harmonics (J22).
     * 
     * @param orbit
     *        a position-velocity in equinoctial parameters
     * @return partial derivatives in equinoctial parameters
     * @throws PatriusException
     *         if the provider doesn't support 2nd degree
     */
    public double[] derParUdeg22(final StelaEquinoctialOrbit orbit) throws PatriusException {

        // Initialization
        final double a = orbit.getA();
        final double ex = orbit.getEquinoctialEx();
        final double ey = orbit.getEquinoctialEy();
        final double ix = orbit.getIx();
        final double iy = orbit.getIy();

        final double eta = MathLib.sqrt(1 - (ex * ex + ey * ey));
        final double ci = 1. - 2 * (ix * ix + iy * iy);

        final double t1 = a * a;
        final double t2 = t1 * t1;
        final double t7 = MathLib.sqrt(Constants.CNES_STELA_MU / a);
        final double t9 = Constants.STELA_LOS_EARTH_RADIUS * Constants.STELA_LOS_EARTH_RADIUS;
        final double t10 = t9 * t9;
        final double t14 = eta * eta;
        final double t17 = t14 * t14;
        final double t18 = t17 * t17;
        final double t19 = 0.1e1 / t18;
        final double t21 = ci * ci;
        final double t22 = t21 * t21;
        final double t30 = 0.25e2 * t14;
        final double t50 = 0.1e1 / t17 / t14 / eta;

        final double fact = 0.1e1 / t2 / a * t7 * t10 / 0.4e1;
        final double dw = 0.3e1 / 0.32e2 * t22 * t19 * (0.360e3 * eta + 0.45e2 * t14 + 0.385e3)
                + 0.3e1 / 0.32e2 * t21 * t19
                        * (0.90e2 - 0.126e3 * t14 - 0.192e3 * eta)
                + 0.3e1 / 0.32e2 * t19 * (0.24e2 * eta + t30 - 0.35e2);
        final double dom =
            -0.3e1 / 0.8e1 * t21 * ci * t19 * (0.5e1 * t14 + 0.36e2 * eta + 0.35e2) - 0.3e1 / 0.8e1 * ci * t19
                    * (-0.9e1 * t14 - 0.12e2 * eta + 0.5e1);
        final double dm = 0.3e1 / 0.32e2 * t22 * t50 * (0.144e3 * eta + t30 + 0.105e3) + 0.3e1 / 0.32e2 * t21 * t50
                * (0.30e2 - 0.90e2 * t14 - 0.96e2 * eta) + 0.3e1 / 0.32e2 * t50 * (0.16e2 * eta + t30 - 0.15e2);
        final double dwb = dw + dom;

        final double j2 = this.provider.getJ(this.isNormalizedLegendrePolynomials, 2)[2];
        final double j22 = j2 * j2;

        final double[] derParUdeg22 = new double[6];
        derParUdeg22[0] = 0.;
        derParUdeg22[1] = j22 * fact * (dwb + dm);
        derParUdeg22[2] = -ey * j22 * fact * dwb;
        derParUdeg22[3] = j22 * ex * fact * dwb;
        derParUdeg22[4] = -iy * j22 * fact * dom;
        derParUdeg22[5] = j22 * ix * fact * dom;

        return derParUdeg22;
    }

    /**
     * Indicate if the zonal perturbation are normalized Legendre Polynomials or not.
     * 
     * @return {@code true} if the zonal perturbation are normalized Legendre Polynomials, {@code false} otherwise
     */
    public boolean isNormalizedLegendrePolynomials() {
        return this.isNormalizedLegendrePolynomials;
    }

    /**
     * Compute alpha coefficients.
     * 
     * @param np
     *        first indice for alpha coefficients
     * @param i
     *        second indice for alpha coefficients
     * @param jn
     *        Jn the zonal coefficients
     * @return alpha coefficients
     */
    private double[][][][][] computeAplhaCoeff(final int np, final int i, final double[] jn) {

        // Compute the alpha coefficients
        for (int kp2 = 0; kp2 <= np; kp2++) {
            for (int kp1 = 0; kp1 <= np - 1; kp1++) {
                for (int lp2 = 0; lp2 <= np - 1; lp2++) {
                    this.alphaCoef[np][i][kp2][kp1][lp2] = computeAlpha(np, i, kp2, kp1, lp2, jn);
                }
            }
        }

        return this.alphaCoef;
    }

    /**
     * Compute an alpha coefficient.<br>
     * (FAST NT-zonaux-hautsdegres) Eq. (43), (44), (45) and (28).
     * 
     * @param np
     *        n'
     * @param i
     *        i : 1, 2 or 3
     * @param kp2
     *        k'2
     * @param kp1
     *        k'1
     * @param lp2
     *        l'2
     * @param jn
     *        Jn the zonal coefficients
     * @return alpha coefficient
     */
    private double computeAlpha(final int np, final int i, final int kp2, final int kp1, final int lp2,
                                final double[] jn) {

        // Alpha(i) computation (i = 1, 2, 3)
        final int alphaI = computeAlphaI(np, i, kp2, kp1, lp2);

        final double alpha;
        if (alphaI == 0) {
            alpha = 0.;
        } else {

            final double legendreCoef;
            if (this.isNormalizedLegendrePolynomials) {
                legendreCoef = computeNormalizedLegendreCoef(2 * kp2, 2 * np);
            } else {
                legendreCoef = computeUnnormalizedLegendreCoef(2 * kp2, 2 * np);
            }

            double c1 = 0.;
            double c2 = 0.;
            double c3 = 0.;
            // c1 definition
            if (2 * np - 1 >= 2 * kp1) {
                c1 = CombinatoricsUtils.binomialCoefficientDouble(2 * np - 1, 2 * kp1);
            }
            // c2 definition
            if (2 * kp1 >= kp1 - lp2) {
                c2 = CombinatoricsUtils.binomialCoefficientDouble(2 * kp1, kp1 - lp2);
            }
            // c3 definition
            if (2 * kp2 >= kp2 - lp2) {
                c3 = CombinatoricsUtils.binomialCoefficientDouble(2 * kp2, kp2 - lp2);
            }

            final double powLp2;
            if (lp2 % 2 == 0) {
                powLp2 = 1.;
            } else {
                powLp2 = -1.;
            }
            alpha = MathLib.divide(legendreCoef * jn[2 * np] * c1 * alphaI * powLp2 * c2 * c3,
                this.powOfTwo[2 * kp1 + 2 * kp2]);
        }

        return alpha;
    }

    /**
     * Private method to reduce cyclomatic complexity of the main method.
     * 
     * @param np
     *        n'
     * @param i
     *        i : 1, 2 or 3
     * @param kp2
     *        k'2
     * @param kp1
     *        k'1
     * @param lp2
     *        l'2
     * @return alpha i term
     */
    private static int computeAlphaI(final int np, final int i, final int kp2, final int kp1, final int lp2) {

        // Evaluate condition 1
        boolean condition1 = i == 1;
        condition1 &= kp2 >= 0;
        condition1 &= kp2 <= np;
        condition1 &= kp1 >= 0;
        condition1 &= kp1 <= np - 1;
        condition1 &= lp2 == 0;

        // Evaluate condition 2
        boolean condition2 = i == 2;
        condition2 &= kp2 >= 1;
        condition2 &= kp2 <= np;
        condition2 &= kp1 >= 1;
        condition2 &= kp1 <= kp2 - 1;
        condition2 &= lp2 >= 1;
        condition2 &= lp2 <= kp1;

        // Evaluate condition 3
        boolean condition3 = i == 3;
        condition3 &= kp2 >= 1;
        condition3 &= kp2 <= np - 1;
        condition3 &= kp1 >= kp2;
        condition3 &= kp1 <= np - 1;
        condition3 &= lp2 >= 1;
        condition3 &= lp2 <= kp2;

        // Implementation note: we evaluate conditions outside a if condition to reduce the conditional operators and
        // the cyclomatic complexity (quality rules)

        final int alphaI;
        // Alpha(i) computation (i = 1, 2, 3)
        if (condition1) {
            alphaI = 1;
        } else if (condition2) {
            alphaI = 2;
        } else if (condition3) {
            alphaI = 2;
        } else {
            alphaI = 0;
        }

        return alphaI;
    }

    /**
     * Compute beta coefficients.
     * 
     * @param np
     *        first indice for alpha coefficients
     * @param i
     *        second indice for alpha coefficients
     * @param jn
     *        Jn the zonal coefficients
     * @return beta coefficients
     */
    private double[][][][][] computeBetaCoeff(final int np, final int i, final double[] jn) {

        // Compute the beta coefficients
        for (int kp2 = 0; kp2 <= np; kp2++) {
            for (int kp1 = 0; kp1 <= np; kp1++) {
                for (int lp2 = 0; lp2 <= np; lp2++) {
                    this.betaCoef[np][i][kp2][kp1][lp2] = computeBeta(np, i, kp2, kp1, lp2, jn);
                }
            }
        }

        return this.betaCoef;
    }

    /**
     * Compute a beta coefficient.<br>
     * (FAST NT-zonaux-hauts degres) Eq. (47), (48) and (39).
     * 
     * @param np
     *        n'
     * @param i
     *        i : 1 or 2
     * @param kp2
     *        k'2
     * @param kp1
     *        k'1
     * @param lp2
     *        l'2
     * @param jn
     *        Jn the zonal coefficients
     * @return beta coefficient
     */
    private double computeBeta(final int np, final int i, final int kp2, final int kp1, final int lp2,
                               final double[] jn) {

        // Beta(i) computation (i = 1, 2)
        final double betaI = computeBetaI(np, i, kp2, kp1, lp2);

        final double beta;
        if (Double.compare(betaI, 0.) == 0) {
            beta = 0.;
        } else {

            final double legendreCoef;
            if (this.isNormalizedLegendrePolynomials) {
                legendreCoef = computeNormalizedLegendreCoef(2 * kp2 + 1, 2 * np + 1);
            } else {
                legendreCoef = computeUnnormalizedLegendreCoef(2 * kp2 + 1, 2 * np + 1);
            }

            double c1 = 0.;
            double c2 = 0.;
            double c3 = 0.;
            // c1 definition
            if (2 * np >= 2 * kp1 + 1) {
                c1 = CombinatoricsUtils.binomialCoefficientDouble(2 * np, 2 * kp1 + 1);
            }
            // c2 definition
            if (2 * kp1 + 1 >= kp1 - lp2) {
                c2 = CombinatoricsUtils.binomialCoefficientDouble(2 * kp1 + 1, kp1 - lp2);
            }
            // c3 definition
            if (2 * kp2 + 1 >= kp2 - lp2) {
                c3 = CombinatoricsUtils.binomialCoefficientDouble(2 * kp2 + 1, kp2 - lp2);
            }

            final double powLp2;
            if (lp2 % 2 == 0) {
                powLp2 = 1.;
            } else {
                powLp2 = -1.;
            }
            beta = MathLib.divide(legendreCoef * jn[2 * np + 1] * c1 * betaI * powLp2 * c2 * c3,
                this.powOfTwo[2 * (kp1 + kp2)]);
        }

        return beta;
    }

    /**
     * Private method to reduce cyclomatic complexity of the main method.
     * 
     * @param np
     *        n'
     * @param i
     *        i : 1, 2 or 3
     * @param kp2
     *        k'2
     * @param kp1
     *        k'1
     * @param lp2
     *        l'2
     * @return beta i term
     */
    private static double computeBetaI(final int np, final int i, final int kp2, final int kp1, final int lp2) {

        // Evaluate condition 1
        boolean condition1 = i == 1;
        condition1 &= kp2 >= 1;
        condition1 &= kp2 <= np;
        condition1 &= kp1 >= 0;
        condition1 &= kp1 <= kp2 - 1;
        condition1 &= lp2 >= 0;
        condition1 &= lp2 <= kp1;

        // Evaluate condition 2
        boolean condition2 = i == 2;
        condition2 &= kp2 >= 0;
        condition2 &= kp2 <= np - 1;
        condition2 &= kp1 >= kp2;
        condition2 &= kp1 <= np;
        condition2 &= lp2 >= 0;
        condition2 &= lp2 <= kp2;

        // Implementation note: we evaluate conditions outside a if condition to reduce the conditional operators and
        // the cyclomatic complexity (quality rules)

        final double betaI;
        if (condition1) {
            betaI = 0.5;
        } else if (condition2) {
            betaI = 0.5;
        } else {
            betaI = 0.;
        }

        return betaI;
    }

    /**
     * Compute an a(k,n) coefficient of normalized Legendre polynomial.<br>
     * (FAST NT-zonaux-hautsdegres) Eq. (9) and (10).
     * 
     * @param kf
     *        k
     * @param nf
     *        n
     * @return Legendre coefficient
     */
    private double computeNormalizedLegendreCoef(final int kf, final int nf) {

        // Initialize l1/l2
        final int l1;
        if (kf <= 2) {
            l1 = 3;
        } else {
            l1 = kf + 1;
        }
        final int l2;
        if (nf <= 2) {
            l2 = 3;
        } else {
            l2 = nf + 1;
        }

        // Initialize a(k,n)
        final double[][] akn = new double[l1][l2];
        akn[0][0] = 1.;
        akn[0][1] = 0.;
        akn[1][1] = MathLib.sqrt(3.);
        akn[0][2] = -MathLib.sqrt(5.) / 2.;
        akn[1][2] = 0.;
        akn[2][2] = 3. * MathLib.sqrt(5.) / 2.;

        // Test if (k,n) is a pair of initialization values
        double legendreCoef = initializeAkn(akn, kf, nf);

        // Compute a(k,n) in the general case (n >= 3)
        if (Double.compare(legendreCoef, INIT_VAL) == 0) {
            for (int n = 3; n <= nf; n++) {
                final double c1 = MathLib.sqrt((2. * n + 1) / (2. * n - 3.));
                final double c2 = MathLib.sqrt((2. * n + 1) / (2. * n - 1.));
                for (int k = 0; k <= kf; k++) {
                    akn[k][n] = computeNormalizedAkn(akn, k, n, c1, c2);
                }
            }
            legendreCoef = akn[kf][nf];
        }

        return legendreCoef;
    }

    /**
     * Compute an a(k,n) coefficient of normalized Legendre polynomial.
     * 
     * @param akn
     *        a(k,n) in entry
     * @param k
     *        degree
     * @param n
     *        order
     * @param c1
     *        coefficient for normalization
     * @param c2
     *        coefficient for normalization
     * @return a(k,n) coefficient
     */
    private double computeNormalizedAkn(final double[][] akn, final int k, final int n, final double c1,
                                        final double c2) {
        double res;
        if (k == 0) {
            res = -(n - 1.) / n * c1 * akn[0][n - 2];
        } else if (k == n - 1) {
            res = (2. * n - 1.) / n * c2 * akn[n - 2][n - 1];
        } else if (k == n) {
            res = (2. * n - 1.) / n * c2 * akn[n - 1][n - 1];
        } else {
            res = (2. * n - 1.) / n * c2 * akn[k - 1][n - 1] - (n - 1.) / n * c1 * akn[k][n - 2];
        }
        return res;
    }

    /**
     * Compute an a(k,n) coefficient of unnormalized Legendre polynomial.<br>
     * (FAST NT-zonaux-hautsdegres) Eq. (5) and (6).
     * 
     * @param kf
     *        k degree
     * @param nf
     *        n order
     * @return Legendre coefficient
     */
    private double computeUnnormalizedLegendreCoef(final int kf, final int nf) {

        // Initialize l1/l2
        final int l1;
        if (kf <= 2) {
            l1 = 3;
        } else {
            l1 = kf + 1;
        }
        final int l2;
        if (nf <= 2) {
            l2 = 3;
        } else {
            l2 = nf + 1;
        }

        // Initialize a(k,n)
        final double[][] akn = new double[l1][l2];
        akn[0][0] = 1;
        akn[0][1] = 0;
        akn[1][1] = 1;
        akn[0][2] = -0.5;
        akn[1][2] = 0;
        akn[2][2] = ONE_POINT_FIVE;

        // Test if (k,n) is a pair of initialization values
        double legendreCoef = initializeAkn(akn, kf, nf);

        // Compute a(k,n) in the general case (n >= 3)
        if (Double.compare(legendreCoef, INIT_VAL) == 0) {
            for (int n = 3; n <= nf; n++) {
                for (int k = 0; k <= kf; k++) {
                    akn[k][n] = computeUnnormalizedAkn(akn, k, n);
                }
            }
            legendreCoef = akn[kf][nf];
        }

        return legendreCoef;
    }

    /**
     * Compute an a(k,n) coefficient of unnormalized Legendre polynomial.
     * 
     * @param akn
     *        a(k,n) in entry
     * @param k
     *        degree
     * @param n
     *        order
     * @return a(k,n) coefficient
     */
    private double computeUnnormalizedAkn(final double[][] akn, final int k, final int n) {
        final double res;
        if (k == 0) {
            res = -(n - 1.) / n * akn[0][n - 2];
        } else if (k == n - 1) {
            res = (2. * n - 1.) / n * akn[n - 2][n - 1];
        } else if (k == n) {
            res = (2. * n - 1.) / n * akn[n - 1][n - 1];
        } else {
            res = (2. * n - 1.) / n * akn[k - 1][n - 1] - (n - 1.) / n * akn[k][n - 2];
        }

        return res;
    }

    /**
     * Initialize a(k,n) coefficient.
     * 
     * @param akn
     *        a(k,n) in entry
     * @param kf
     *        k degree
     * @param nf
     *        n order
     * @return Legendre coefficient
     */
    private double initializeAkn(final double[][] akn, final int kf, final int nf) {

        // Test if [k,n] is a pair of initialization values
        final double legendreCoef;
        if (kf == 0 && nf == 2) {
            legendreCoef = akn[0][2];
        } else if (kf == 2 && nf == 2) {
            legendreCoef = akn[2][2];
        } else {
            legendreCoef = INIT_VAL;
        }

        /*
         * Note: In STELA's code there is a switch condition which also associates the following values:
         * * kf == 0 & nf == 0 : legendreCoef = akn[0][0]
         * * kf == 0 & nf == 1 : legendreCoef = akn[0][1]
         * * kf == 1 & nf == 1 : legendreCoef = akn[1][1]
         * * kf == 1 & nf == 2 : legendreCoef = akn[1][2]
         * But as the kf / nf initialization doesn't seems to allow theses values we can't test and cover these
         * conditions, hence they're not tested.
         */

        return legendreCoef;
    }

    /**
     * Lagrange Planetary Equations in equinoctial parameters.
     *
     * @param orbit
     *        a position-velocity in equinoctial parameters
     * @param equinPartialDerivatives
     *        Partial derivatives in equinoctial parameters
     * @return Lagrange Planetary Equations in equinoctial parameters
     */
    private double[] getPlanEq(final StelaEquinoctialOrbit orbit, final double[] equinPartialDerivatives) {

        // Initialization
        final double a = orbit.getA();
        final double ex = orbit.getEquinoctialEx();
        final double ey = orbit.getEquinoctialEy();
        final double ix = orbit.getIx();
        final double iy = orbit.getIy();

        // Temporary coefficients
        final double eta = MathLib.sqrt(1. - (ex * ex + ey * ey));
        final double etaOverEtaUp = eta / (eta + 1.);
        final double oneOver2Eta = 1. / (2 * eta);
        final double oneOverNa = MathLib.sqrt(a / Constants.CNES_STELA_MU);
        final double oneOverNaSq = oneOverNa / a;
        final double coefI = ix * equinPartialDerivatives[4] + iy * equinPartialDerivatives[5];
        final double coefE = ey * equinPartialDerivatives[2] - ex * equinPartialDerivatives[3];

        // Type 8 Derivatives
        final double daDt = 2 * equinPartialDerivatives[1] * oneOverNa;
        final double dxiDt = -2 * oneOverNa * equinPartialDerivatives[0] + oneOverNaSq * etaOverEtaUp
                * (ex * equinPartialDerivatives[2] + ey * equinPartialDerivatives[3])
                + oneOverNaSq * coefI * oneOver2Eta;

        // Eccentricity vector
        final double dexDt = oneOverNaSq
                * (-etaOverEtaUp * ex * equinPartialDerivatives[1] - eta * equinPartialDerivatives[3]
                        - ey * oneOver2Eta * coefI);
        final double deyDt = oneOverNaSq
                * (-etaOverEtaUp * ey * equinPartialDerivatives[1] + eta * equinPartialDerivatives[2]
                        + ex * oneOver2Eta * coefI);

        // Inclination vector
        final double dixDt = oneOverNaSq * oneOver2Eta
                * (-ix * equinPartialDerivatives[1] + ix * coefE - equinPartialDerivatives[5] / 2.);
        final double diyDt = oneOverNaSq * oneOver2Eta
                * (-iy * equinPartialDerivatives[1] + iy * coefE + equinPartialDerivatives[4] / 2.);

        // Result
        return new double[] { daDt, dxiDt, dexDt, deyDt, dixDt, diyDt };
    }
}
