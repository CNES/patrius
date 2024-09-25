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
import fr.cnes.sirius.patrius.propagation.MassProvider;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

//CHECKSTYLE: stop MagicNumber check
//CHECKSTYLE: stop CommentRatio check
// Reason: model - Orekit code

/**
 * This class contains methods to compute propagated coordinates with the SGP4 model.
 * <p>
 * The user should not bother in this class since it is handled internaly by the {@link TLEPropagator}.
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
@SuppressWarnings("PMD.ShortClassName")
class SGP4 extends TLEPropagator {

    /** Serializable UID. */
    private static final long serialVersionUID = -7860984112560308900L;

    /** Eccentricity threshold. */
    private static final double E_LIM = 1e-6;

    /** If perige is less than 220 km, some calculus are avoided. */
    private boolean lessThan220;

    /** (1 + eta * cos(M0))<sup>3</sup>. */
    private double delM0;

    /** Intermediate value. */
    private double d2;
    /** Intermediate value. */
    private double d3;
    /** Intermediate value. */
    private double d4;
    /** Intermediate value. */
    private double t3cof;
    /** Intermediate value. */
    private double t4cof;
    /** Intermediate value. */
    private double t5cof;
    /** Intermediate value. */
    private double sinM0;
    /** Intermediate value. */
    private double omgcof;
    /** Intermediate value. */
    private double xmcof;
    /** Intermediate value. */
    private double c5;

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
    protected SGP4(final TLE initialTLE, final AttitudeProvider attitudeProvider,
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
    protected SGP4(final TLE initialTLE, final AttitudeProvider attitudeProviderForces,
        final AttitudeProvider attitudeProviderEvents, final MassProvider mass) throws PatriusException {
        super(initialTLE, attitudeProviderForces, attitudeProviderEvents, mass);
    }

    /**
     * Initialization proper to each propagator (SGP or SDP).
     */
    @Override
    protected void sxpInitialize() {

        // For perigee less than 220 kilometers, the equations are truncated to
        // linear variation in sqrt a and quadratic variation in mean anomaly.
        // Also, the c3 term, the delta omega term, and the delta m term are dropped.
        this.lessThan220 = this.perige < 220;
        if (!this.lessThan220) {
            final double[] sincosM = MathLib.sinAndCos(this.tle.getMeanAnomaly());
            final double sinM = sincosM[0];
            final double cosM = sincosM[1];
            final double c1sq = this.c1 * this.c1;
            this.delM0 = 1.0 + this.eta * cosM;
            this.delM0 *= this.delM0 * this.delM0;
            this.d2 = 4 * this.a0dp * this.tsi * c1sq;
            final double temp = this.d2 * this.tsi * this.c1 / 3.0;
            this.d3 = (17 * this.a0dp + this.s4) * temp;
            this.d4 = 0.5 * temp * this.a0dp * this.tsi * (221 * this.a0dp + 31 * this.s4) * this.c1;
            this.t3cof = this.d2 + 2 * c1sq;
            this.t4cof = 0.25 * (3 * this.d3 + this.c1 * (12 * this.d2 + 10 * c1sq));
            this.t5cof =
                0.2 * (3 * this.d4 + 12 * this.c1 * this.d3 + 6 * this.d2 * this.d2 + 15 * c1sq * (2 * this.d2 + c1sq));
            this.sinM0 = sinM;
            if (this.tle.getE() < 1e-4) {
                this.omgcof = 0.;
                this.xmcof = 0.;
            } else {
                final double c3 = this.coef * this.tsi * TLEConstants.A3OVK2 * this.xn0dp *
                    TLEConstants.NORMALIZED_EQUATORIAL_RADIUS * this.sini0 / this.tle.getE();
                this.xmcof = -TLEConstants.TWO_THIRD * this.coef * this.tle.getBStar() *
                    TLEConstants.NORMALIZED_EQUATORIAL_RADIUS / this.eeta;
                this.omgcof = this.tle.getBStar() * c3 * MathLib.cos(this.tle.getPerigeeArgument());
            }
        }

        this.c5 =
            2 * this.coef1 * this.a0dp * this.beta02 * (1 + 2.75 * (this.etasq + this.eeta) + this.eeta * this.etasq);
        // initialized
    }

    /**
     * Propagation proper to each propagator (SGP or SDP).
     * 
     * @param tSince
     *        the offset from initial epoch (min)
     */
    @Override
    protected void sxpPropagate(final double tSince) {

        // Update for secular gravity and atmospheric drag.
        final double xmdf = this.tle.getMeanAnomaly() + this.xmdot * tSince;
        final double omgadf = this.tle.getPerigeeArgument() + this.omgdot * tSince;
        final double xn0ddf = this.tle.getRaan() + this.xnodot * tSince;
        this.omega = omgadf;
        double xmp = xmdf;
        final double tsq = tSince * tSince;
        this.xnode = xn0ddf + this.xnodcf * tsq;
        double tempa = 1 - this.c1 * tSince;
        double tempe = this.tle.getBStar() * this.c4 * tSince;
        double templ = this.t2cof * tsq;

        if (!this.lessThan220) {
            final double delomg = this.omgcof * tSince;
            double delm = 1. + this.eta * MathLib.cos(xmdf);
            delm = this.xmcof * (delm * delm * delm - this.delM0);
            final double temp = delomg + delm;
            xmp = xmdf + temp;
            this.omega = omgadf - temp;
            final double tcube = tsq * tSince;
            final double tfour = tSince * tcube;
            tempa = tempa - this.d2 * tsq - this.d3 * tcube - this.d4 * tfour;
            tempe = tempe + this.tle.getBStar() * this.c5 * (MathLib.sin(xmp) - this.sinM0);
            templ = templ + this.t3cof * tcube + tfour * (this.t4cof + tSince * this.t5cof);
        }

        this.a = this.a0dp * tempa * tempa;
        this.e = this.tle.getE() - tempe;

        // A highly arbitrary lower limit on e, of 1e-6:
        if (this.e < E_LIM) {
            this.e = E_LIM;
        }

        this.xl = xmp + this.omega + this.xnode + this.xn0dp * templ;

        this.i = this.tle.getI();

    }

    // CHECKSTYLE: resume MagicNumber check
    // CHECKSTYLE: resume CommentRatio check
}
