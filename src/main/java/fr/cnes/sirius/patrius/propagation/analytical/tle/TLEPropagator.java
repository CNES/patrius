/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 */
/*
 *
 * HISTORY
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::FA:381:15/12/2014:Propagator tolerances and default mass and attitude issues
 * VERSION::FA:468:22/10/2015:Proper handling of ephemeris mode for analytical propagators
 * VERSION::FA:829:25/01/2017:Protection of trigonometric methods
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.analytical.tle;

import java.io.Serializable;

import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.AbstractPropagator;
import fr.cnes.sirius.patrius.propagation.MassProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

//CHECKSTYLE: stop MagicNumber check
//CHECKSTYLE: stop CommentRatio check
// Reason: model - Orekit code

/**
 * This class provides elements to propagate TLE's.
 * <p>
 * The models used are SGP4 and SDP4, initially proposed by NORAD as the unique convenient propagator for TLE's. Inputs
 * and outputs of this propagator are only suited for NORAD two lines elements sets, since it uses estimations and mean
 * values appropriate for TLE's only.
 * </p>
 * <p>
 * Deep- or near- space propagator is selected internally according to NORAD recommendations so that the user has not to
 * worry about the used computation methods. One instance is created for each TLE (this instance can only be get using
 * {@link #selectExtrapolator(TLE)} method, and can compute {@link PVCoordinates position and velocity coordinates} at
 * any time. Maximum accuracy is guaranteed in a 24h range period before and after the provided TLE epoch (of course
 * this accuracy is not really measurable nor predictable: according to <a
 * href="http://www.celestrak.com/">CelesTrak</a>, the precision is close to one kilometer and error won't probably rise
 * above 2 km).
 * </p>
 * <p>
 * This implementation is largely inspired from the paper and source code <a
 * href="http://www.celestrak.com/publications/AIAA/2006-6753/">Revisiting Spacetrack Report #3</a> and is fully
 * compliant with its results and tests cases.
 * </p>
 * 
 * @author Felix R. Hoots, Ronald L. Roehrich, December 1980 (original fortran)
 * @author David A. Vallado, Paul Crawford, Richard Hujsak, T.S. Kelso (C++ translation and improvements)
 * @author Fabien Maussion (java translation)
 * @see TLE
 */
//CHECKSTYLE: stop AbstractClassName check
@SuppressWarnings({"PMD.AbstractNaming", "PMD.ConstructorCallsOverridableMethod"})
public abstract class TLEPropagator extends AbstractPropagator implements Serializable {
    // CHECKSTYLE: resume AbstractClassName check

    /** Serializable UID. */
    private static final long serialVersionUID = 6389584529961457799L;

    /** Eccentricity threshold. */
    private static final double E_LIM = 1e-6;

    /** Deep space limit. */
    private static final double DEEP_SPACE_LIMIT = 6.4;

    /** Earth gravity coefficient in m<sup>3</sup>/s<sup>2</sup>. */
    private static final double MU =
        TLEConstants.XKE * TLEConstants.XKE *
            TLEConstants.EARTH_RADIUS * TLEConstants.EARTH_RADIUS * TLEConstants.EARTH_RADIUS *
            (1000 * 1000 * 1000) / (60 * 60);

    /** Initial state. */
    protected final TLE tle;

    /** final RAAN. */
    protected double xnode;

    /** final semi major axis. */
    protected double a;

    /** final eccentricity. */
    protected double e;

    /** final inclination. */
    protected double i;

    /** final perigee argument. */
    protected double omega;

    /** L from SPTRCK #3. */
    protected double xl;

    /** original recovered semi major axis. */
    protected double a0dp;

    /** original recovered mean motion. */
    protected double xn0dp;

    /** cosinus original inclination. */
    protected double cosi0;

    /** cos io squared. */
    protected double theta2;

    /** sinus original inclination. */
    protected double sini0;

    /** common parameter for mean anomaly (M) computation. */
    protected double xmdot;

    /** common parameter for perigee argument (omega) computation. */
    protected double omgdot;

    /** common parameter for raan (OMEGA) computation. */
    protected double xnodot;

    /** original eccentricity squared. */
    protected double e0sq;
    /** 1 - e2. */
    protected double beta02;

    /** sqrt (1 - e2). */
    protected double beta0;

    /** perigee, expressed in KM and ALTITUDE. */
    protected double perige;

    /** eta squared. */
    protected double etasq;

    /** original eccentricity * eta. */
    protected double eeta;

    /** s* new value for the contant s. */
    protected double s4;

    /** tsi from SPTRCK #3. */
    protected double tsi;

    /** eta from SPTRCK #3. */
    protected double eta;

    /** coef for SGP C3 computation. */
    protected double coef;

    /** coef for SGP C5 computation. */
    protected double coef1;

    /** C1 from SPTRCK #3. */
    protected double c1;

    /** C2 from SPTRCK #3. */
    protected double c2;

    /** C4 from SPTRCK #3. */
    protected double c4;

    /** common parameter for raan (OMEGA) computation. */
    protected double xnodcf;

    /** 3/2 * C1. */
    protected double t2cof;

    /** TLE frame. */
    private final Frame frame;

    /**
     * Protected constructor for derived classes.
     * 
     * @param initialTLE
     *        the unique TLE to propagate
     * @param attitudeProvider
     *        provider for attitude computation
     * @param mass
     *        spacecraft mass (kg)
     * @exception PatriusException
     *            if some specific error occurs
     */
    protected TLEPropagator(final TLE initialTLE, final AttitudeProvider attitudeProvider,
        final MassProvider mass) throws PatriusException {
        super(attitudeProvider);
        this.setStartDate(initialTLE.getDate());
        this.tle = initialTLE;
        this.frame = FramesFactory.getTEME();
        this.addAdditionalStateProvider(mass);
        this.initializeCommons();
        this.sxpInitialize();
        // set the initial state
        super.resetInitialState(new SpacecraftState(this.propagateOrbit(initialTLE.getDate()), mass));
    }

    /**
     * Protected constructor for derived classes.
     * 
     * @param initialTLE
     *        the unique TLE to propagate
     * @param attitudeProviderForces
     *        provider for attitude computation in forces computation case
     * @param attitudeProviderEvents
     *        provider for attitude computation in events computation case
     * @param mass
     *        spacecraft mass (kg)
     * @exception PatriusException
     *            if some specific error occurs
     */
    protected TLEPropagator(final TLE initialTLE, final AttitudeProvider attitudeProviderForces,
        final AttitudeProvider attitudeProviderEvents, final MassProvider mass) throws PatriusException {
        super(attitudeProviderForces, attitudeProviderEvents);
        this.setStartDate(initialTLE.getDate());
        this.tle = initialTLE;
        this.frame = FramesFactory.getTEME();
        this.addAdditionalStateProvider(mass);
        this.initializeCommons();
        this.sxpInitialize();
        // set the initial state
        super.resetInitialState(new SpacecraftState(this.propagateOrbit(initialTLE.getDate()), mass));
    }

    /**
     * Selects the extrapolator to use with the selected TLE.
     * 
     * @param tle
     *        the TLE to propagate.
     * @return the correct propagator.
     * @exception PatriusException
     *            if the underlying model cannot be initialized
     */
    public static TLEPropagator selectExtrapolator(final TLE tle) throws PatriusException {
        return selectExtrapolator(tle, null, null);
    }

    /**
     * Selects the extrapolator to use with the selected TLE.
     * 
     * @param tle
     *        the TLE to propagate.
     * @param attitudeProvider
     *        provider for attitude computation
     * @param mass
     *        spacecraft mass (kg)
     * @return the correct propagator.
     * @exception PatriusException
     *            if the underlying model cannot be initialized
     */
    public static TLEPropagator selectExtrapolator(final TLE tle, final AttitudeProvider attitudeProvider,
                                                   final MassProvider mass) throws PatriusException {

        final double a1 = MathLib.pow(TLEConstants.XKE / (tle.getMeanMotion() * 60.0), TLEConstants.TWO_THIRD);
        final double cosi0 = MathLib.cos(tle.getI());
        final double temp = TLEConstants.CK2 * 1.5 * (3 * cosi0 * cosi0 - 1.0) *
            MathLib.pow(1.0 - tle.getE() * tle.getE(), -1.5);
        final double delta1 = temp / (a1 * a1);
        final double a0 = a1 * (1.0 - delta1 * (TLEConstants.ONE_THIRD + delta1 * (delta1 * 134.0 / 81.0 + 1.0)));
        final double delta0 = temp / (a0 * a0);

        // recover original mean motion :
        final double xn0dp = tle.getMeanMotion() * 60.0 / (delta0 + 1.0);

        // Period >= 225 minutes is deep space
        if (MathUtils.TWO_PI / (xn0dp * TLEConstants.MINUTES_PER_DAY) >= (1.0 / DEEP_SPACE_LIMIT)) {
            return new DeepSDP4(tle, attitudeProvider, mass);
        } else {
            return new SGP4(tle, attitudeProvider, mass);
        }
    }

    /**
     * Selects the extrapolator to use with the selected TLE.
     * 
     * @param tle
     *        the TLE to propagate.
     * @param attitudeProviderForces
     *        provider for attitude computation in forces computation case
     * @param attitudeProviderEvents
     *        provider for attitude computation in events computation case
     * @param mass
     *        spacecraft mass (kg)
     * @return the correct propagator.
     * @exception PatriusException
     *            if the underlying model cannot be initialized
     */
    public static TLEPropagator selectExtrapolator(final TLE tle, final AttitudeProvider attitudeProviderForces,
                                                   final AttitudeProvider attitudeProviderEvents,
                                                   final MassProvider mass) throws PatriusException {

        final double a1 = MathLib.pow(TLEConstants.XKE / (tle.getMeanMotion() * 60.0), TLEConstants.TWO_THIRD);
        final double cosi0 = MathLib.cos(tle.getI());
        final double temp = TLEConstants.CK2 * 1.5 * (3 * cosi0 * cosi0 - 1.0) *
            MathLib.pow(1.0 - tle.getE() * tle.getE(), -1.5);
        final double delta1 = temp / (a1 * a1);
        final double a0 = a1 * (1.0 - delta1 * (TLEConstants.ONE_THIRD + delta1 * (delta1 * 134.0 / 81.0 + 1.0)));
        final double delta0 = temp / (a0 * a0);

        // recover original mean motion :
        final double xn0dp = tle.getMeanMotion() * 60.0 / (delta0 + 1.0);

        // Period >= 225 minutes is deep space
        if (MathUtils.TWO_PI / (xn0dp * TLEConstants.MINUTES_PER_DAY) >= (1.0 / DEEP_SPACE_LIMIT)) {
            return new DeepSDP4(tle, attitudeProviderForces, attitudeProviderEvents, mass);
        } else {
            return new SGP4(tle, attitudeProviderForces, attitudeProviderEvents, mass);
        }
    }

    /**
     * Get the extrapolated position and velocity from an initial TLE.
     * 
     * @param date
     *        the final date
     * @return the final PVCoordinates
     * @exception PatriusException
     *            if propagation cannot be performed at given date
     */
    public PVCoordinates getPVCoordinates(final AbsoluteDate date) throws PatriusException {

        this.sxpPropagate(date.durationFrom(this.tle.getDate()) / 60.0);

        // Compute PV with previous calculated parameters
        return this.computePVCoordinates();
    }

    /**
     * Computation of the first commons parameters.
     */
    private void initializeCommons() {

        final double a1 = MathLib.pow(TLEConstants.XKE / (this.tle.getMeanMotion() * 60.0), TLEConstants.TWO_THIRD);
        this.cosi0 = MathLib.cos(this.tle.getI());
        this.theta2 = this.cosi0 * this.cosi0;
        final double x3thm1 = 3.0 * this.theta2 - 1.0;
        this.e0sq = this.tle.getE() * this.tle.getE();
        this.beta02 = 1.0 - this.e0sq;
        this.beta0 = MathLib.sqrt(MathLib.max(0.0, this.beta02));
        final double tval = TLEConstants.CK2 * 1.5 * x3thm1 / (this.beta0 * this.beta02);
        final double delta1 = tval / (a1 * a1);
        final double a0 = a1 * (1.0 - delta1 * (TLEConstants.ONE_THIRD + delta1 * (1.0 + 134.0 / 81.0 * delta1)));
        final double delta0 = tval / (a0 * a0);

        // recover original mean motion and semi-major axis :
        this.xn0dp = this.tle.getMeanMotion() * 60.0 / (delta0 + 1.0);
        this.a0dp = a0 / (1.0 - delta0);

        // Values of s and qms2t :
        // unmodified value for s
        this.s4 = TLEConstants.S;
        // unmodified value for q0ms2T
        double q0ms24 = TLEConstants.QOMS2T;

        // perige
        this.perige =
            (this.a0dp * (1 - this.tle.getE()) - TLEConstants.NORMALIZED_EQUATORIAL_RADIUS) * TLEConstants.EARTH_RADIUS;

        // For perigee below 156 km, the values of s and qoms2t are changed :
        if (this.perige < 156.0) {
            if (this.perige <= 98.0) {
                this.s4 = 20.0;
            } else {
                this.s4 = this.perige - 78.0;
            }
            final double tempVal = (120.0 - this.s4) * TLEConstants.NORMALIZED_EQUATORIAL_RADIUS
                / TLEConstants.EARTH_RADIUS;
            final double tempValSquared = tempVal * tempVal;
            q0ms24 = tempValSquared * tempValSquared;
            // new value for q0ms2T and s
            this.s4 = this.s4 / TLEConstants.EARTH_RADIUS + TLEConstants.NORMALIZED_EQUATORIAL_RADIUS;
        }

        final double pinv = 1.0 / (this.a0dp * this.beta02);
        final double pinvsq = pinv * pinv;
        this.tsi = 1.0 / (this.a0dp - this.s4);
        this.eta = this.a0dp * this.tle.getE() * this.tsi;
        this.etasq = this.eta * this.eta;
        this.eeta = this.tle.getE() * this.eta;

        // abs because pow 3.5 needs positive value
        final double psisq = MathLib.abs(1.0 - this.etasq);
        final double tsiSquared = this.tsi * this.tsi;
        this.coef = q0ms24 * tsiSquared * tsiSquared;
        this.coef1 = this.coef / MathLib.pow(psisq, 3.5);

        // C2 and C1 coefficients computation :
        this.c2 = this.coef1 * this.xn0dp * (this.a0dp * (1.0 + 1.5 * this.etasq + this.eeta * (4.0 + this.etasq)) +
            0.75 * TLEConstants.CK2 * this.tsi / psisq * x3thm1 * (8.0 + 3.0 * this.etasq * (8.0 + this.etasq)));
        this.c1 = this.tle.getBStar() * this.c2;
        this.sini0 = MathLib.sin(this.tle.getI());

        final double x1mth2 = 1.0 - this.theta2;

        // C4 coefficient computation :
        this.c4 = 2.0
            * this.xn0dp
            * this.coef1
            * this.a0dp
            * this.beta02
            * (this.eta * (2.0 + 0.5 * this.etasq) +
                this.tle.getE() * (0.5 + 2.0 * this.etasq) -
            2
                * TLEConstants.CK2
                * this.tsi
                / (this.a0dp * psisq)
                *
                (-3.0 * x3thm1 * (1.0 - 2.0 * this.eeta + this.etasq * (1.5 - 0.5 * this.eeta)) +
                0.75 * x1mth2 * (2.0 * this.etasq - this.eeta * (1.0 + this.etasq))
                    * MathLib.cos(2.0 * this.tle.getPerigeeArgument())));

        final double theta4 = this.theta2 * this.theta2;
        final double temp1 = 3 * TLEConstants.CK2 * pinvsq * this.xn0dp;
        final double temp2 = temp1 * TLEConstants.CK2 * pinvsq;
        final double temp3 = 1.25 * TLEConstants.CK4 * pinvsq * pinvsq * this.xn0dp;

        // atmospheric and gravitation coefs :(Mdf and OMEGAdf)
        this.xmdot = this.xn0dp +
            0.5 * temp1 * this.beta0 * x3thm1 +
            0.0625 * temp2 * this.beta0 * (13.0 - 78.0 * this.theta2 + 137.0 * theta4);

        final double x1m5th = 1.0 - 5.0 * this.theta2;

        this.omgdot = -0.5 * temp1 * x1m5th +
            0.0625 * temp2 * (7.0 - 114.0 * this.theta2 + 395.0 * theta4) +
            temp3 * (3.0 - 36.0 * this.theta2 + 49.0 * theta4);

        final double xhdot1 = -temp1 * this.cosi0;

        this.xnodot =
            xhdot1 + (0.5 * temp2 * (4.0 - 19.0 * this.theta2) + 2.0 * temp3 * (3.0 - 7.0 * this.theta2)) * this.cosi0;
        this.xnodcf = 3.5 * this.beta02 * xhdot1 * this.c1;
        this.t2cof = 1.5 * this.c1;

    }

    /**
     * Retrieves the position and velocity.
     * 
     * @return the computed PVCoordinates.
     * @exception PatriusException
     *            if current orbit is out of supported range
     *            (too large eccentricity, too low perigee ...)
     */
    // CHECKSTYLE: stop MethodLength check
    private PVCoordinates computePVCoordinates() throws PatriusException {
        // CHECKSTYLE: resume MethodLength check

        if (this.e > (1 - E_LIM)) {
            throw new PatriusException(PatriusMessages.TOO_LARGE_ECCENTRICITY_FOR_PROPAGATION_MODEL, this.e);
        }

        // Dundee changes: items dependent on cosio get recomputed:
        final double cosi0Sq = this.cosi0 * this.cosi0;
        final double x3thm1 = 3.0 * cosi0Sq - 1.0;
        final double x1mth2 = 1.0 - cosi0Sq;
        final double x7thm1 = 7.0 * cosi0Sq - 1.0;

        // Long period periodics
        final double[] sincosOmega = MathLib.sinAndCos(this.omega);
        final double sinOmega = sincosOmega[0];
        final double cosOmega = sincosOmega[1];
        final double axn = this.e * cosOmega;
        double temp = 1.0 / (this.a * (1.0 - this.e * this.e));
        final double xlcof = 0.125 * TLEConstants.A3OVK2 * this.sini0 * (3.0 + 5.0 * this.cosi0) / (1.0 + this.cosi0);
        final double aycof = 0.25 * TLEConstants.A3OVK2 * this.sini0;
        final double xll = temp * xlcof * axn;
        final double aynl = temp * aycof;
        final double xlt = this.xl + xll;
        final double ayn = this.e * sinOmega + aynl;
        final double elsq = axn * axn + ayn * ayn;
        final double capu = MathUtils.normalizeAngle(xlt - this.xnode, FastMath.PI);
        double epw = capu;
        double ecosE = 0;
        double esinE = 0;
        double sinEPW = 0;
        double cosEPW = 0;

        // Solve Kepler's' Equation.
        final double newtonRaphsonEpsilon = 1e-12;
        for (int j = 0; j < 10; j++) {

            boolean doSecondOrderNewtonRaphson = true;

            final double[] sincosepw = MathLib.sinAndCos(epw);
            sinEPW = sincosepw[0];
            cosEPW = sincosepw[1];
            ecosE = axn * cosEPW + ayn * sinEPW;
            esinE = axn * sinEPW - ayn * cosEPW;
            final double f = capu - epw + esinE;
            if (MathLib.abs(f) < newtonRaphsonEpsilon) {
                break;
            }
            final double fdot = 1.0 - ecosE;
            double deltaEpw = f / fdot;
            if (j == 0) {
                final double maxNewtonRaphson = 1.25 * MathLib.abs(this.e);
                doSecondOrderNewtonRaphson = false;
                if (deltaEpw > maxNewtonRaphson) {
                    deltaEpw = maxNewtonRaphson;
                } else if (deltaEpw < -maxNewtonRaphson) {
                    deltaEpw = -maxNewtonRaphson;
                } else {
                    doSecondOrderNewtonRaphson = true;
                }
            }
            if (doSecondOrderNewtonRaphson) {
                deltaEpw = f / (fdot + 0.5 * esinE * deltaEpw);
            }
            epw += deltaEpw;
        }

        // Short period preliminary quantities
        temp = 1.0 - elsq;
        final double pl = this.a * temp;
        final double r = this.a * (1.0 - ecosE);
        double temp2 = this.a / r;
        final double betal = MathLib.sqrt(MathLib.max(0.0, temp));
        temp = esinE / (1.0 + betal);
        final double cosu = temp2 * (cosEPW - axn + ayn * temp);
        final double sinu = temp2 * (sinEPW - ayn - axn * temp);
        final double u = MathLib.atan2(sinu, cosu);
        final double sin2u = 2.0 * sinu * cosu;
        final double cos2u = 2.0 * cosu * cosu - 1.0;
        final double temp1 = TLEConstants.CK2 / pl;
        temp2 = temp1 / pl;

        // Update for short periodics
        final double rk = r * (1.0 - 1.5 * temp2 * betal * x3thm1) + 0.5 * temp1 * x1mth2 * cos2u;
        final double uk = u - 0.25 * temp2 * x7thm1 * sin2u;
        final double xnodek = this.xnode + 1.5 * temp2 * this.cosi0 * sin2u;
        final double xinck = this.i + 1.5 * temp2 * this.cosi0 * this.sini0 * cos2u;

        // Orientation vectors
        final double[] sincosuk = MathLib.sinAndCos(uk);
        final double[] sincosik = MathLib.sinAndCos(xinck);
        final double[] sincosok = MathLib.sinAndCos(xnodek);
        final double sinuk = sincosuk[0];
        final double cosuk = sincosuk[1];
        final double sinik = sincosik[0];
        final double cosik = sincosik[1];
        final double sinnok = sincosok[0];
        final double cosnok = sincosok[1];
        final double xmx = -sinnok * cosik;
        final double xmy = cosnok * cosik;
        final double ux = xmx * sinuk + cosnok * cosuk;
        final double uy = xmy * sinuk + sinnok * cosuk;
        final double uz = sinik * sinuk;

        // Position and velocity
        final double cr = 1000 * rk * TLEConstants.EARTH_RADIUS;
        final Vector3D pos = new Vector3D(cr * ux, cr * uy, cr * uz);

        final double rdot = TLEConstants.XKE * MathLib.sqrt(this.a) * esinE / r;
        final double rfdot = TLEConstants.XKE * MathLib.sqrt(MathLib.max(0.0, pl)) / r;
        final double xn = TLEConstants.XKE / (this.a * MathLib.sqrt(this.a));
        final double rdotk = rdot - xn * temp1 * x1mth2 * sin2u;
        final double rfdotk = rfdot + xn * temp1 * (x1mth2 * cos2u + 1.5 * x3thm1);
        final double vx = xmx * cosuk - cosnok * sinuk;
        final double vy = xmy * cosuk - sinnok * sinuk;
        final double vz = sinik * cosuk;

        final double cv = 1000.0 * TLEConstants.EARTH_RADIUS / 60.0;
        final Vector3D vel = new Vector3D(cv * (rdotk * ux + rfdotk * vx),
            cv * (rdotk * uy + rfdotk * vy),
            cv * (rdotk * uz + rfdotk * vz));

        // Return result
        return new PVCoordinates(pos, vel);
    }

    /**
     * Initialization proper to each propagator (SGP or SDP).
     * 
     * @exception PatriusException
     *            if some specific error occurs
     */
    protected abstract void sxpInitialize() throws PatriusException;

    /**
     * Propagation proper to each propagator (SGP or SDP).
     * 
     * @param t
     *        the offset from initial epoch (min)
     * @exception PatriusException
     *            if current state cannot be propagated
     */
    protected abstract void sxpPropagate(double t) throws PatriusException;

    /** {@inheritDoc} */
    @Override
    protected Orbit propagateOrbit(final AbsoluteDate date) throws PropagationException {
        try {
            return new CartesianOrbit(this.getPVCoordinates(date), FramesFactory.getTEME(), date, MU);
        } catch (final PatriusException oe) {
            throw new PropagationException(oe);
        }
    }

    /**
     * Get the underlying TLE.
     * 
     * @return underlying TLE
     */
    public TLE getTLE() {
        return this.tle;
    }

    /** {@inheritDoc} */
    @Override
    public Frame getFrame() {
        return this.frame;
    }

    // CHECKSTYLE: resume MagicNumber check
    // CHECKSTYLE: resume CommentRatio check
}
