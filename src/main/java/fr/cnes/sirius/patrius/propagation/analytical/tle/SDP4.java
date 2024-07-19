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
* VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.analytical.tle;

import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.propagation.MassProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

//CHECKSTYLE: stop MagicNumber check
// Reason: model - Orekit code

/**
 * This class contains methods to compute propagated coordinates with the SDP4 model.
 * <p>
 * The user should not bother in this class since it is handled internally by the {@link TLEPropagator}.
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
 */
//CHECKSTYLE: stop AbstractClassName check
@SuppressWarnings({"PMD.AbstractNaming", "PMD.ShortClassName"})
abstract class SDP4 extends TLEPropagator {
    // CHECKSTYLE: resume AbstractClassName check

    /** Serializable UID. */
    private static final long serialVersionUID = 6739431592307605737L;

    /** New perigee argument. */
    protected double omgadf;

    /** New mean motion. */
    protected double xn;

    /** Parameter for xl computation. */
    protected double xll;

    /** New eccentricity. */
    protected double em;

    /** New inclination. */
    protected double xinc;

    /**
     * Constructor for a unique initial TLE.
     * 
     * @param initialTLE
     *        the TLE to propagate.
     * @param attitudeProvider
     *        provider for attitude computation
     * @param mass
     *        spacecraft mass provider
     * @exception PatriusException
     *            if some specific error occurs
     */
    protected SDP4(final TLE initialTLE, final AttitudeProvider attitudeProvider,
        final MassProvider mass) throws PatriusException {
        super(initialTLE, attitudeProvider, mass);
    }

    /**
     * Constructor for a unique initial TLE.
     * 
     * @param initialTLE
     *        the TLE to propagate.
     * @param attitudeProviderForces
     *        provider for attitude computation in forces computation case
     * @param attitudeProviderEvents
     *        provider for attitude computation in events computation case
     * @param mass
     *        spacecraft mass provider
     * @exception PatriusException
     *            if some specific error occurs
     */
    protected SDP4(final TLE initialTLE, final AttitudeProvider attitudeProviderForces,
        final AttitudeProvider attitudeProviderEvents, final MassProvider mass) throws PatriusException {
        super(initialTLE, attitudeProviderForces, attitudeProviderEvents, mass);
    }

    /**
     * Initialization proper to each propagator (SGP or SDP).
     * 
     * @exception PatriusException
     *            when UTC time steps can't be read
     */
    @Override
    protected void sxpInitialize() throws PatriusException {
        this.luniSolarTermsComputation();
    } // End of initialization

    /**
     * Propagation proper to each propagator (SGP or SDP).
     * 
     * @param tSince
     *        the offset from initial epoch (minutes)
     */
    @Override
    protected void sxpPropagate(final double tSince) {

        // Update for secular gravity and atmospheric drag
        this.omgadf = this.tle.getPerigeeArgument() + this.omgdot * tSince;
        final double xnoddf = this.tle.getRaan() + this.xnodot * tSince;
        final double tSinceSq = tSince * tSince;
        this.xnode = xnoddf + this.xnodcf * tSinceSq;
        this.xn = this.xn0dp;

        // Update for deep-space secular effects
        this.xll = this.tle.getMeanAnomaly() + this.xmdot * tSince;

        this.deepSecularEffects(tSince);

        final double tempa = 1 - this.c1 * tSince;
        this.a = MathLib.pow(TLEConstants.XKE / this.xn, TLEConstants.TWO_THIRD) * tempa * tempa;
        this.em -= this.tle.getBStar() * this.c4 * tSince;

        // Update for deep-space periodic effects
        this.xll += this.xn0dp * this.t2cof * tSinceSq;

        this.deepPeriodicEffects(tSince);

        this.xl = this.xll + this.omgadf + this.xnode;

        // Dundee change: Reset cosio, sinio for new xinc:
        final double[] sincos = MathLib.sinAndCos(this.xinc);
        this.cosi0 = sincos[1];
        this.sini0 = sincos[0];
        this.e = this.em;
        this.i = this.xinc;
        this.omega = this.omgadf;
        // end of calculus, go for PV computation
    }

    /**
     * Computes SPACETRACK#3 compliant earth rotation angle.
     * 
     * @param date
     *        the current date
     * @return the ERA (rad)
     * @exception PatriusException
     *            when UTC time steps can't be read
     */
    protected static double thetaG(final AbsoluteDate date) throws PatriusException {

        // Reference: The 1992 Astronomical Almanac, page B6.
        final double omegaE = 1.00273790934;
        final double jd = (date.durationFrom(AbsoluteDate.JULIAN_EPOCH) +
            date.timeScalesOffset(TimeScalesFactory.getUTC(), TimeScalesFactory.getTT())
            ) / Constants.JULIAN_DAY;

        // Earth rotations per sidereal day (non-constant)
        final double ut = (jd + 1. / 2.) % 1;
        final double secondsPerDay = Constants.JULIAN_DAY;
        /* 1.5 Jan 2000 = JD 2451545. */
        final double jd2000 = 2451545.0;
        final double tCen = (jd - ut - jd2000) / 36525.;
        double gmst = 24110.54841 +
            tCen * (8640184.812866 + tCen * (0.093104 - tCen * 6.2E-6));
        gmst = (gmst + secondsPerDay * omegaE * ut) % secondsPerDay;
        if (gmst < 0.) {
            gmst += secondsPerDay;
        }

        return MathUtils.TWO_PI * gmst / secondsPerDay;

    }

    /**
     * Computes luni - solar terms from initial coordinates and epoch.
     * 
     * @exception PatriusException
     *            when UTC time steps can't be read
     */
    protected abstract void luniSolarTermsComputation() throws PatriusException;

    /**
     * Computes secular terms from current coordinates and epoch.
     * 
     * @param t
     *        offset from initial epoch (min)
     */
    protected abstract void deepSecularEffects(double t);

    /**
     * Computes periodic terms from current coordinates and epoch.
     * 
     * @param t
     *        offset from initial epoch (min)
     */
    protected abstract void deepPeriodicEffects(double t);

    // CHECKSTYLE: resume MagicNumber check
}
