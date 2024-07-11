/**
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
 * @history creation 15/04/2015
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:317:15/04/2015: STELA integration in CIRF with referential choice (ICRF, CIRF or MOD)
 * VERSION::DM:660:24/09/2016:add getters to frames configuration
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.configuration.precessionnutation;

import fr.cnes.sirius.patrius.frames.configuration.FrameConvention;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;

/**
 * STELA specific precession/nutation model.
 * 
 * @author Emmanuel Bignon
 * @concurrency thread-safe
 * @version $Id: StelaPrecessionNutationModel.java 18073 2017-10-02 16:48:07Z bignon $
 * @since 3.0
 */
public final class StelaPrecessionNutationModel implements PrecessionNutationModel {

     /** Serializable UID. */
    private static final long serialVersionUID = 5842920285869618886L;

    /** Conversion from microarcsec to radians. */
    private static final double UNIT = 4.84813681109536E-12;

    /** Delaunay variable coefficient for F. */
    private static final double DELAUNAY_VAR_F_0 = 1.6279050815;
    /** Delaunay variable coefficient for Fdot. */
    private static final double DELAUNAY_VAR_F_1 = 8433.4661569164;
    /** Delaunay variable coefficient for D. */
    private static final double DELAUNAY_VAR_D_0 = 5.1984665887;
    /** Delaunay variable coefficient for Ddot. */
    private static final double DELAUNAY_VAR_D_1 = 7771.3771455937;
    /** Delaunay variable coefficient for Om. */
    private static final double DELAUNAY_VAR_OM_0 = 2.1824391966;
    /** Delaunay variable coefficient for Omdot. */
    private static final double DELAUNAY_VAR_OM_1 = -33.7570459536;
    /** Delaunay variable coefficient for Lp. */
    private static final double DELAUNAY_VAR_LP_0 = 6.2400601269;
    /** Delaunay variable coefficient for Lpdot. */
    private static final double DELAUNAY_VAR_LP_1 = 628.3019551714;

    /** Coefficient 1 for x. */
    private static final int X_COEFF_0 = 2004191898;
    /** Coefficient 2 for x. */
    private static final int X_COEFF_1 = -429783;
    /** Coefficient 3 for x. */
    private static final int X_COEFF_2 = -198618;
    /** Coefficient 4 for x. */
    private static final int X_COEFF_3 = -6844318;
    /** Coefficient 5 for x. */
    private static final int X_COEFF_4 = -523908;
    /** Coefficient 6 for x. */
    private static final int X_COEFF_5 = +205833;
    /** Coefficient 7 for x. */
    private static final int X_COEFF_6 = +58707;
    /** Coefficient 8 for x. */
    private static final int X_COEFF_7 = -20558;
    /** Coefficient 9 for x. */
    private static final int X_COEFF_8 = +12814;
    /** Coefficient 10 for x. */
    private static final int X_COEFF_9 = -8585;
    /** Coefficient 11 for x. */
    private static final int X_COEFF_10 = +5096;
    /** Coefficient 12 for x. */
    private static final int X_COEFF_11 = -3310;

    /** Coefficient 1 for y. */
    private static final int Y_COEFF_0 = -22407275;
    /** Coefficient 2 for y. */
    private static final int Y_COEFF_1 = 9205236;
    /** Coefficient 3 for y. */
    private static final int Y_COEFF_2 = 573033;
    /** Coefficient 4 for y. */
    private static final int Y_COEFF_3 = 153042;
    /** Coefficient 5 for y. */
    private static final int Y_COEFF_4 = -89618;
    /** Coefficient 6 for y. */
    private static final int Y_COEFF_5 = 22438;
    /** Coefficient 7 for y. */
    private static final int Y_COEFF_6 = 11714;
    /** Coefficient 8 for y. */
    private static final int Y_COEFF_7 = -9593;
    /** Coefficient 9 for y. */
    private static final int Y_COEFF_8 = 7387;
    /** Coefficient 10 for y. */
    private static final int Y_COEFF_9 = -6918;

    /** Coefficient 1 for s. */
    private static final int S_COEFF_0 = 3809;
    /** Coefficient 2 for s. */
    private static final int S_COEFF_1 = -72574;

    /** Cached date. */
    private static AbsoluteDate cachedDate = AbsoluteDate.PAST_INFINITY;

    /** Cached CIP parameters. */
    private static double[] cachedCIP = null;

    /** Cached CIP derivatives. */
    private static double[] cachedCIPDerivative = null;

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    public double[] getCIPMotion(final AbsoluteDate date) {

        if (date.durationFrom(cachedDate) != 0) {
            computeCIPMotion(date);
        }

        return cachedCIP;
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    public double[] getCIPMotionTimeDerivative(final AbsoluteDate date) {

        if (date.durationFrom(cachedDate) != 0) {
            computeCIPMotion(date);
        }

        return cachedCIPDerivative;
    }

    /**
     * Compute CIP motion [x, y, s] and its time derivatives.
     * 
     * @param date
     *        a date
     */
    //CHECKSTYLE: stop MethodLength check
    private static void computeCIPMotion(final AbsoluteDate date) {
        //CHECKSTYLE: resume MethodLength check

        // t parameter (TT converted date in J2000)
        final double t = date.offsetFrom(AbsoluteDate.J2000_EPOCH, TimeScalesFactory.getTT()) /
            Constants.JULIAN_CENTURY;

        // Delaunay variables (Table C.1)
        final double f = DELAUNAY_VAR_F_0 + DELAUNAY_VAR_F_1 * t;
        final double d = DELAUNAY_VAR_D_0 + DELAUNAY_VAR_D_1 * t;
        final double om = DELAUNAY_VAR_OM_0 + DELAUNAY_VAR_OM_1 * t;
        final double lp = DELAUNAY_VAR_LP_0 + DELAUNAY_VAR_LP_1 * t;

        // Derivatives of Delaunay variables
        final double ttdot = 1.0 / Constants.JULIAN_CENTURY;
        final double fdot = DELAUNAY_VAR_F_1;
        final double ddot = DELAUNAY_VAR_D_1;
        final double omdot = DELAUNAY_VAR_OM_1;
        final double lpdot = DELAUNAY_VAR_LP_1;

        // Precompute some values to speed-up computation
        final double[] sincosom = MathLib.sinAndCos(om);
        final double[] sincoslp = MathLib.sinAndCos(lp);
        final double[] sincoslp2f2d2om = MathLib.sinAndCos(lp + 2 * f - 2 * d + 2 * om);
        final double[] sincos2f2dom = MathLib.sinAndCos(2 * f - 2 * d + om);
        final double sinom = sincosom[0];
        final double cosom = sincosom[1];
        final double sin2f2d2om = MathLib.sin(2 * f - 2 * d + 2 * om);
        final double sinlp = sincoslp[0];
        final double coslp = sincoslp[1];
        final double sinlp2f2d2om = sincoslp2f2d2om[0];
        final double coslp2f2d2om = sincoslp2f2d2om[1];
        final double cos2f2d2om = MathLib.cos(2 * f - 2 * d + 2 * om);
        final double sinlp2f2d2om2 = MathLib.sin(lp - 2 * f + 2 * d - 2 * om);
        final double coslp2f2dom2 = MathLib.cos(lp - 2 * f + 2 * d - 2 * om);
        final double sin2f2dom = sincos2f2dom[0];
        final double cos2f2dom = sincos2f2dom[1];

        // X
        final double x = (
            X_COEFF_0 * t +
                X_COEFF_1 * t * t +
                X_COEFF_2 * t * t * t +
                X_COEFF_3 * sinom +
                X_COEFF_4 * sin2f2d2om +
                X_COEFF_5 * t * cosom +
                X_COEFF_6 * sinlp +
                X_COEFF_7 * sinlp2f2d2om +
                X_COEFF_8 * t * cos2f2d2om +
                X_COEFF_9 * sinlp2f2d2om2 +
                X_COEFF_10 * sin2f2dom +
            X_COEFF_11 * t * sinom ) * UNIT;

        final double xdot = (
            X_COEFF_0 * 1 +
                X_COEFF_1 * (2 * t) +
                X_COEFF_2 * (3 * t * t) +
                X_COEFF_3 * (omdot * cosom) +
                X_COEFF_4 * ((2 * fdot - 2 * ddot + 2 * omdot) * cos2f2d2om) +
                X_COEFF_5 * (cosom - t * omdot * sinom) +
                X_COEFF_6 * (lpdot * coslp) +
                X_COEFF_7 * ((lpdot + 2 * fdot - 2 * ddot + 2 * omdot) * coslp2f2d2om) +
                X_COEFF_8 * (cos2f2d2om - t * (2 * fdot - 2 * ddot + 2 * omdot) * sin2f2d2om) +
                X_COEFF_9 * ((lpdot - 2 * fdot + 2 * ddot - 2 * omdot) * coslp2f2dom2) +
                X_COEFF_10 * ((2 * fdot - 2 * ddot + omdot) * cos2f2dom) +
            X_COEFF_11 * (sinom + t * omdot * cosom) ) * UNIT * ttdot;

        // Y
        final double y = (
            Y_COEFF_0 * t * t +
                Y_COEFF_1 * cosom +
                Y_COEFF_2 * cos2f2d2om +
                Y_COEFF_3 * t * sinom +
                Y_COEFF_4 * MathLib.cos(2 * om) +
                Y_COEFF_5 * coslp2f2d2om +
                Y_COEFF_6 * t * sin2f2d2om +
                Y_COEFF_7 * coslp2f2dom2 +
                Y_COEFF_8 * coslp +
            Y_COEFF_9 * cos2f2dom ) * UNIT;

        final double ydot = (
            Y_COEFF_0 * (2 * t) +
                Y_COEFF_1 * (-omdot * sinom) +
                Y_COEFF_2 * (-(2 * fdot - 2 * ddot + 2 * omdot) * sin2f2d2om) +
                Y_COEFF_3 * (sinom + t * omdot * cosom) +
                Y_COEFF_4 * (-2 * omdot * MathLib.sin(2 * om)) +
                Y_COEFF_5 * (-(lpdot + 2 * fdot - 2 * ddot + 2 * omdot) * sinlp2f2d2om) +
                Y_COEFF_6 * (sin2f2d2om + t * (2 * fdot - 2 * ddot + 2 * omdot) * cos2f2d2om) +
                Y_COEFF_7 * (-(lpdot - 2 * fdot + 2 * ddot - 2 * omdot) * sinlp2f2d2om2) +
                Y_COEFF_8 * (-lpdot * sinlp) +
            Y_COEFF_9 * (-(2 * fdot - 2 * ddot + omdot) * sin2f2dom) ) * UNIT * ttdot;

        // S
        final double s = -(x * y) / 2 + (S_COEFF_0 * t + S_COEFF_1 * t * t * t) * UNIT;

        final double sdot = -(xdot * y + x * ydot) / 2 + (
            S_COEFF_0 * 1 + S_COEFF_1 * (3 * t * t)) * UNIT * ttdot;

        // Store data
        cachedDate = date;
        cachedCIP = new double[] { x, y, s };
        cachedCIPDerivative = new double[] { xdot, ydot, sdot };
    }

    /** {@inheritDoc} */
    @Override
    public boolean isDirect() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public FrameConvention getOrigin() {
        return FrameConvention.STELA;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isConstant() {
        return false;
    }
}
