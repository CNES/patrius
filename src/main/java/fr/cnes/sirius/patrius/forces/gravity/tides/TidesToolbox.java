/**
 * 
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
 * VERSION::DM:90:03/10/2013:moved GravityToolbox to Orekit, created TidesToolbox
 * VERSION::FA:180:18/03/2014:optimized code
 * VERSION::DM:241:01/10/2014:improved tides conception
 * VERSION::FA:345:30/10/2014:modified comments ratio
 * VERSION::FA:410:16/04/2015: Anomalies in the Patrius Javadoc
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.gravity.tides;

import fr.cnes.sirius.patrius.forces.gravity.tides.TidesStandards.TidesStandard;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateTimeComponents;
import fr.cnes.sirius.patrius.time.TimeScale;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusExceptionWrapper;

/**
 * Tides toolbox
 * 
 * @concurrency immutable
 * 
 * @author Rami Houdroge
 * @version $$
 * @since 2.1
 * 
 */
public final class TidesToolbox {
    /**
     * Days in julian century
     */
    private static final int DAYS_IN_CENTURY = 36525;
    /**
     * Coefficient for the mean sidereal time computation.
     */
    private static final double TSIDM_A0 = 1.7533685592330;
    /**
     * Coefficient for the mean sidereal time computation.
     */
    private static final double TSIDM_A1 = 1.7202791805307e-02;
    /**
     * Coefficient for the mean sidereal time computation.
     */
    private static final double TSIDM_A2 = 5.0752099941136e-15;
    /**
     * Coefficient for the mean sidereal time computation.
     */
    private static final double TSIDM_A3 = -9.2530975681943e-24;
    /**
     * Constant 0.5
     */
    private static final double ZERO_POINT_FIVE = 0.5;
    /**
     * Constant 1e-5
     */
    private static final double TEN_MINUS_FIVE = 1.e-5;
    /**
     * Constant 1e-2
     */
    private static final double TEN_MINUS_TWO = 1.e-2;
    /**
     * Constant 1e-1
     * */
    private static final double TEN_MINUS_ONE = 1.e-1;
    /**
     * Constant 10
     * */
    private static final int TEN = 10;
    /**
     * Constant 100
     * */
    private static final double ONE_HUNDRED = 100.0;
    /**
     * Constant 1000
     * */
    private static final double ONE_THOUSAND = 1000.0;

    /** Number of seconds in an hour. */
    private static final double SEC_IN_HOUR = 3600.0;

    /**
     * GINS 2004 cf41 constant
     */
    private static final double GINS2004_NUTATION_CF41 = -6962890.2665;

    /**
     * IERS 1996 cf41 constant
     */
    private static final double IERS1996_NUTATION_CF41 = -6962890.2665;

    /**
     * Private constructor
     */
    private TidesToolbox() {
    }

    /**
     * Doodson number decomposition as a sextuplet of integers.
     * 
     * The six small integers multipliers encode the frequency of the tidal argument concerned and form the Doodson
     * numbers: in practice all except the first are usually biased upwards by +5 to avoid negative numbers in the
     * notation. (In the case that the biased multiple exceeds 9, the system adopts X for 10, and E for 11.)
     * 
     * For example, the Doodson number 273.555 means that the tidal frequency is composed of twice the first Doodson
     * argument, +2 times the second, -2 times the third and zero times each of the other three.
     * 
     * See http://en.wikipedia.org/wiki/Arthur_Thomas_Doodson
     * 
     * @comments see obelixutil.f90 : function nDoodson(nonde)
     * 
     * @param doodsonNumber
     *        : Doodson number (xxx.xxx)
     * @return Doodson sextuplet
     */
    public static int[] nDoodson(final double doodsonNumber) {
        final int[] sextuplet = new int[6];
        final int n2;
        final int n3;
        final int n4;
        final int n5;
        // first sextuplet component:
        sextuplet[0] = (int) (doodsonNumber * TEN_MINUS_TWO);
        n2 = (int) (doodsonNumber * TEN_MINUS_ONE);
        // second sextuplet component:
        sextuplet[1] = n2 - sextuplet[0] * TEN - 5;
        n3 = (int) doodsonNumber;
        // third sextuplet component:
        sextuplet[2] = n3 - n2 * TEN - 5;
        n4 = (int) (doodsonNumber * TEN);
        // fourth sextuplet component:
        sextuplet[3] = n4 - n3 * TEN - 5;
        n5 = (int) (doodsonNumber * ONE_HUNDRED);
        // fifth sextuplet component:
        sextuplet[4] = n5 - n4 * TEN - 5;
        // sixth sextuplet component:
        sextuplet[5] = (int) MathLib.round(doodsonNumber * ONE_THOUSAND - n5 * TEN - 5 + TEN_MINUS_FIVE);

        return sextuplet;
    }

    /**
     * 
     * Method to compute the Doodson fundamental arguments.
     * 
     * @param date
     *        the considered absolute date
     * @param standard
     *        the tides standard to use
     * @return a table of the nutation arguments
     * @throws PatriusException
     *         when an Orekit error occurs
     * 
     * @comments see OBELIX library: module mc_argfond.f90/subroutine orc_argfond_doodson
     * 
     */
    public static double[][] computeFundamentalArguments(final AbsoluteDate date,
                                                         final TidesStandard standard) throws PatriusException {

        // fundamental arguments
        final double[][] fundamentalArg = new double[6][2];

        // compute the mean sideral time
        final double meanSideralTime = computeSideralTime(date);

        // J2000 date time components in TT scale
        final DateTimeComponents j2000 = (AbsoluteDate.J2000_EPOCH).getComponents(TimeScalesFactory.getTT());

        // date time components of the given date in TAI scale
        final DateTimeComponents d = date.getComponents(TimeScalesFactory.getTAI());

        // duration from the J2000 epoch to the given date in TT scale (julian days)
        final double jd = (d.offsetFrom(j2000) + TimeScalesFactory.getTT().offsetFromTAI(date)) / Constants.JULIAN_DAY;

        // compute the fundamental arguments from the luni-solar nutation theory
        final double[][] nutationArguments = computeNutationArguments(jd, standard);

        // compute the Doodson fundamental arguments
        fundamentalArg[1][0] = nutationArguments[2][0] + nutationArguments[4][0];
        fundamentalArg[2][0] = fundamentalArg[1][0] - nutationArguments[3][0];
        fundamentalArg[3][0] = fundamentalArg[1][0] - nutationArguments[0][0];
        fundamentalArg[4][0] = -nutationArguments[4][0];
        fundamentalArg[5][0] = fundamentalArg[2][0] - nutationArguments[1][0];
        fundamentalArg[0][0] = meanSideralTime + FastMath.PI - fundamentalArg[1][0];

        // compute the Doodson fundamental arguments derivatives
        fundamentalArg[1][1] = nutationArguments[2][1] + nutationArguments[4][1];
        fundamentalArg[2][1] = fundamentalArg[1][1] - nutationArguments[3][1];
        fundamentalArg[3][1] = fundamentalArg[1][1] - nutationArguments[0][1];
        fundamentalArg[4][1] = -nutationArguments[4][1];
        fundamentalArg[5][1] = fundamentalArg[2][1] - nutationArguments[1][1];
        final double tetamp = TSIDM_A1 + 2 * jd * TSIDM_A2 + 3 * jd * jd * TSIDM_A3 + MathUtils.TWO_PI;
        fundamentalArg[0][1] = tetamp - fundamentalArg[1][1];

        return fundamentalArg;
    }

    /**
     * 
     * Method to compute the fundamental arguments from the luni-solar nutation theory.
     * 
     * @param jd
     *        duration from the J2000 epoch to the given date with TT scale in julian days
     * @param standard
     *        the tides standard to use
     * @return a table of the nutation arguments and their first and second derivatives in columns
     * 
     * @comments see OBELIX library : module mc_argfond.f90/subroutine orc_argfond_nutls
     * 
     */
    public static double[][] computeNutationArguments(final double jd, final TidesStandard standard) {

        // nutation coefficients from IERS2003 standard
        TerrestrialTidesDataProvider iers2003Standard = null;
        try {
            iers2003Standard = new TerrestrialTidesDataProvider(TidesStandard.IERS2003);
        } catch (final PatriusException e) {
            // should never happen
            throw new PatriusExceptionWrapper(e);
        }
        final double[][] cf = iers2003Standard.getNutationCoefficients();

        // fundamental arguments from the luni-solar nutation theory
        final double[][] nutationArguments = new double[5][2];

        switch (standard) {
            case IERS1996:
                cf[4][1] = IERS1996_NUTATION_CF41;
                break;
            case GINS2004:
                cf[4][1] = GINS2004_NUTATION_CF41;
                break;
            default:
                // nothing to do
                break;
        }

        // duration from the J2000 epoch to the given date with TT scale (in julian century)
        final double jc = jd / Constants.JULIAN_DAY_CENTURY;

        // compute nutation arguments and their derivatives
        final double jc2 = jc * jc;
        final double jc3 = jc2 * jc;
        final double jc4 = jc3 * jc;
        for (int i = 0; i < 5; i++) {
            nutationArguments[i][0] = cf[i][0] * MathUtils.DEG_TO_RAD
                + (cf[i][1] * jc + cf[i][2] * jc2 + cf[i][3] * jc3 + cf[i][4] * jc4) * MathUtils.DEG_TO_RAD
                / SEC_IN_HOUR;

            nutationArguments[i][0] = nutationArguments[i][0] % MathUtils.TWO_PI;

            // derivatives
            nutationArguments[i][1] = (cf[i][1] + 2 * cf[i][2] * jc + 3 * cf[i][3] * jc2 + 4 * cf[i][4] * jc3)
                * MathUtils.DEG_TO_RAD / SEC_IN_HOUR / DAYS_IN_CENTURY;
        }

        return nutationArguments;
    }

    /**
     * Compute the mean sideral time as a polynomial of degree 3.
     * 
     * @comment the formula is only valid for UT1 scale.
     * @comment see mc_argfond.f90 : function orc_tsid_moyen
     * @param date
     *        the considered absolute date
     * @return mean sideral time as an angle between 0 and 2PI.
     * @throws PatriusException
     *         when an Orekit error occurs
     */
    private static double computeSideralTime(final AbsoluteDate date) throws PatriusException {
        // UT1 time scale
        final TimeScale ut1 = TimeScalesFactory.getUT1();

        // duration from the J2000 epoch to the given date with UT1 scale in julian days
        final double jj = (date.durationFrom(AbsoluteDate.J2000_EPOCH) + ut1.offsetFromTAI(date) - TimeScalesFactory
            .getTT().offsetFromTAI(date)) / Constants.JULIAN_DAY;

        final double jj2 = jj * jj;
        final double jj3 = jj2 * jj;

        // decimal part of the julian days duration (example : djf = 0 at midnight and djf = 0.5 at noon)
        final double djf = jj - (int) jj + ZERO_POINT_FIVE;

        double tSideral = TSIDM_A0 + jj * TSIDM_A1 + jj2 * TSIDM_A2 + jj3 * TSIDM_A3 + MathUtils.TWO_PI
            * djf;

        tSideral = tSideral % MathUtils.TWO_PI;

        if (tSideral < Precision.EPSILON) {
            tSideral += MathUtils.TWO_PI;
        }

        return tSideral;
    }
}
