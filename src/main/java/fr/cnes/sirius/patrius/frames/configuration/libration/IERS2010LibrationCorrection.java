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
 * @history creation 11/07/2012
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:144:17/12/2013:Corrected elapsed seconds computation (was in UTC)
 * VERSION::DM:144:06/11/2013:Changed UT1-UTC correction to UT1-TAI correction
 * VERSION::FA:180:18/03/2014:Optimized code by factorizing powers of t
 * VERSION::DM:660:24/09/2016:add getters to frames configuration
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.configuration.libration;

import fr.cnes.sirius.patrius.frames.configuration.FrameConvention;
import fr.cnes.sirius.patrius.frames.configuration.eop.PoleCorrection;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

//CHECKSTYLE: stop MagicNumber check
// Reason: model - Orekit code

/**
 * <p>
 * This class computes the diurnal lunisolar effect. <b>It is a java translation of the fortran subroutine PM_GRAVI
 * (provided by CNES and from IERS conventions, see chapter 5, tables 5.1a and 5.2a).</b>
 * </p>
 * 
 * @concurrency unconditionally thread-safe
 * 
 * @author G.Mercadier
 * 
 * @version $Id: IERS2010LibrationCorrection.java 18073 2017-10-02 16:48:07Z bignon $
 * 
 * @since 1.2
 */
public class IERS2010LibrationCorrection implements LibrationCorrectionModel {

    /** IUD. */
    private static final long serialVersionUID = 6397798546342916829L;

    /** pi/2. */
    private static final double HALF_PI = FastMath.PI / 2.0;

    /** Angular units conversion factor. */
    private static final double MICRO_ARC_SECONDS_TO_RADIANS = Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6;

    /** Array dimension. */
    private static final int NLINES = 10;

    /** Diurnal lunisolar tidal terms present in x (microas),y(microas). */
    /** NARG : Multipliers of GMST+pi and Delaunay arguments. */
    private static final int[][] NARG = new int[NLINES][6];

    /** XCOS array. */
    private static final double[] XCOS = new double[NLINES];

    /** XSIN array. */
    private static final double[] XSIN = new double[NLINES];

    /** YCOS array. */
    private static final double[] YCOS = new double[NLINES];

    /** YSIN array. */
    private static final double[] YSIN = new double[NLINES];

    /** Constant for arguments. */
    private static final double A00 = 67310.54841;

    /** Constant for arguments. */
    private static final double A01 = 876600.0 * 3600.0;

    /** Constant for arguments. */
    private static final double A02 = 8640184.812866;

    /** Constant for arguments. */
    private static final double A03 = 0.093104;

    /** Constant for arguments. */
    private static final double A04 = 6.2E-6;

    /** Constant for arguments. */
    private static final double A05 = 15.0;

    /** Constant for arguments. */
    private static final double A06 = 648000.0;

    /** Constant for arguments. */
    private static final double C = 1296000.0;

    /** Constant for arguments. */
    private static final double A10 = -0.00024470;

    /** Constant for arguments. */
    private static final double A11 = 0.051635;

    /** Constant for arguments. */
    private static final double A12 = 31.8792;

    /** Constant for arguments. */
    private static final double A13 = 1717915923.2178;

    /** Constant for arguments. */
    private static final double A14 = 485868.249036;

    /** Constant for arguments. */
    private static final double A20 = -0.00001149;

    /** Constant for arguments. */
    private static final double A21 = 0.000136;

    /** Constant for arguments. */
    private static final double A22 = 0.5532;

    /** Constant for arguments. */
    private static final double A23 = 129596581.0481;

    /** Constant for arguments. */
    private static final double A24 = 1287104.79305;

    /** Constant for arguments. */
    private static final double A30 = 0.00000417;

    /** Constant for arguments. */
    private static final double A31 = 0.001037;

    /** Constant for arguments. */
    private static final double A32 = 12.7512;

    /** Constant for arguments. */
    private static final double A33 = 1739527262.8478;

    /** Constant for arguments. */
    private static final double A34 = 335779.526232;

    /** Constant for arguments. */
    private static final double A40 = -0.00003169;

    /** Constant for arguments. */
    private static final double A41 = 0.006593;

    /** Constant for arguments. */
    private static final double A42 = 6.3706;

    /** Constant for arguments. */
    private static final double A43 = 1602961601.2090;

    /** Constant for arguments. */
    private static final double A44 = 1072260.70369;

    /** Constant for arguments. */
    private static final double A50 = -0.00005939;

    /** Constant for arguments. */
    private static final double A51 = 0.007702;

    /** Constant for arguments. */
    private static final double A52 = 7.4722;

    /** Constant for arguments. */
    private static final double A53 = 6962890.2665;

    /** Constant for arguments. */
    private static final double A54 = 450160.398036;

    /** Date of last pole correction computation. */
    private AbsoluteDate lastPoleDate;

    /** Last pole correction computation result. */
    private PoleCorrection lastPole;

    /** Token for synchronization. */
    private final Object lastPoleToken = new Object();

    /** Static initialisation for all arrays. */
    static {
        initNARGArray();
        initXCOSArray();
        initXSINArray();
        initYCOSArray();
        initYSINArray();
    }

    /**
     * Static initialisation for NARG array.
     */
    // CHECKSTYLE: stop CommentRatio check
    private static void initNARGArray() {
        // CHECKSTYLE: resume CommentRatio check

        // Initialize NARG arrays
        NARG[0] = new int[] { 1, -1, 0, -2, 0, -1 };
        NARG[1] = new int[] { 1, -1, 0, -2, 0, -2 };
        NARG[2] = new int[] { 1, 1, 0, -2, -2, -2 };
        NARG[3] = new int[] { 1, 0, 0, -2, 0, -1 };
        NARG[4] = new int[] { 1, 0, 0, -2, 0, -2 };
        NARG[5] = new int[] { 1, -1, 0, 0, 0, 0 };
        NARG[6] = new int[] { 1, 0, 0, -2, 2, -2 };
        NARG[7] = new int[] { 1, 0, 0, 0, 0, 0 };
        NARG[8] = new int[] { 1, 0, 0, 0, 0, -1 };
        NARG[9] = new int[] { 1, 1, 0, 0, 0, 0 };
    }

    /**
     * Static initialisation for XCOS array.
     */
    private static void initXCOSArray() {
        XCOS[0] = 0.25;
        XCOS[1] = 1.32;
        XCOS[2] = 0.25;
        XCOS[3] = 1.23;
        XCOS[4] = 6.52;
        XCOS[5] = -0.48;
        XCOS[6] = 2.73;
        XCOS[7] = -8.19;
        XCOS[8] = -1.11;
        XCOS[9] = -0.43;
    }

    /**
     * Static initialisation for XSIN array.
     */
    private static void initXSINArray() {
        XSIN[0] = -0.44;
        XSIN[1] = -2.31;
        XSIN[2] = -0.44;
        XSIN[3] = -2.14;
        XSIN[4] = -11.36;
        XSIN[5] = 0.84;
        XSIN[6] = -4.76;
        XSIN[7] = 14.27;
        XSIN[8] = 1.93;
        XSIN[9] = 0.76;
    }

    /**
     * Static initialisation for YCOS array.
     */
    private static void initYCOSArray() {
        YCOS[0] = -0.44;
        YCOS[1] = -2.31;
        YCOS[2] = -0.44;
        YCOS[3] = -2.14;
        YCOS[4] = -11.36;
        YCOS[5] = 0.84;
        YCOS[6] = -4.76;
        YCOS[7] = 14.27;
        YCOS[8] = 1.93;
        YCOS[9] = 0.76;
    }

    /**
     * Static initialisation for YSIN array.
     */
    private static void initYSINArray() {
        YSIN[0] = -0.25;
        YSIN[1] = -1.32;
        YSIN[2] = -0.25;
        YSIN[3] = -1.23;
        YSIN[4] = -6.52;
        YSIN[5] = 0.48;
        YSIN[6] = -2.73;
        YSIN[7] = 8.19;
        YSIN[8] = 1.11;
        YSIN[9] = 0.43;
    }

    /**
     * This method provides the diurnal lunisolar effect on polar motion in time domain. The computed corrections should
     * be added to EOP values.
     * 
     * <b>The fundamental lunisolar arguments are those of Simon and al.</b>
     * 
     * @param date
     *        date at which the correction is desired
     * @return pole correction
     * @throws PatriusException
     *         when an Orekit error occurs
     */
    @Override
    public final PoleCorrection getPoleCorrection(final AbsoluteDate date) throws PatriusException {

        synchronized (this.lastPoleToken) {
            if (date.equals(this.lastPoleDate)) {
                // The very same pole correction has just been computed.
                // We reuse the result.
                return this.lastPole;
            }
        }

        // Initialization
        double dx;
        double dy;
        double ag;

        // date time components of the given date
        // Julian century
        final double t = date.offsetFrom(AbsoluteDate.J2000_EPOCH, TimeScalesFactory.getTDB()) / Constants.JULIAN_DAY
            / Constants.JULIAN_DAY_CENTURY;

        // Intermediate variables
        final double t2 = t * t;
        final double t3 = t2 * t;
        final double t4 = t3 * t;

        // Arguments in the following order : chi=GMST+pi,l,lp,F,D,Omega
        final double[] arguments = new double[6];

        arguments[0] = (A00 + (A01 + A02) * t + A03 * t2 - A04 * t3) * A05 + A06;
        arguments[0] = (arguments[0] % C) * Constants.ARC_SECONDS_TO_RADIANS;

        arguments[1] = A10 * t4 + A11 * t3 + A12 * t2 + A13 * t + A14;
        arguments[1] = (arguments[1] % C) * Constants.ARC_SECONDS_TO_RADIANS;

        arguments[2] = A20 * t4 - A21 * t3 - A22 * t2 + A23 * t + A24;
        arguments[2] = (arguments[2] % C) * Constants.ARC_SECONDS_TO_RADIANS;

        arguments[3] = A30 * t4 - A31 * t3 - A32 * t2 + A33 * t + A34;
        arguments[3] = (arguments[3] % C) * Constants.ARC_SECONDS_TO_RADIANS;

        arguments[4] = A40 * t4 + A41 * t3 - A42 * t2 + A43 * t + A44;
        arguments[4] = (arguments[4] % C) * Constants.ARC_SECONDS_TO_RADIANS;

        arguments[5] = A50 * t4 + A51 * t3 + A52 * t2 - A53 * t + A54;
        arguments[5] = (arguments[5] % C) * Constants.ARC_SECONDS_TO_RADIANS;

        // compute corrections
        dx = 0;
        dy = 0;

        for (int j = 0; j < NLINES; j++) {
            ag = 0.;
            for (int i = 0; i < 6; i++) {
                ag = ag + (NARG[j][i]) * arguments[i];
            }
            ag = ag % (4. * HALF_PI);

            final double[] sincos = MathLib.sinAndCos(ag);
            final double sin = sincos[0];
            final double cos = sincos[1];

            dx = dx + XCOS[j] * cos + XSIN[j] * sin;
            dy = dy + YCOS[j] * cos + YSIN[j] * sin;
        }

        // radian
        dx = dx * MICRO_ARC_SECONDS_TO_RADIANS;
        dy = dy * MICRO_ARC_SECONDS_TO_RADIANS;

        synchronized (this.lastPoleToken) {
            // Return data
            this.lastPoleDate = date;
            this.lastPole = new PoleCorrection(dx, dy);
            return this.lastPole;
        }
    }

    /**
     * Get the dUT1 value. The correction is due to diurnal lunisolar effect.
     * 
     * @param date
     *        date at which the value is desired
     * @return dUT1 in seconds
     */
    @Override
    public final double getUT1Correction(final AbsoluteDate date) {
        // not yet implemented
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public FrameConvention getOrigin() {
        return FrameConvention.IERS2010;
    }

    // CHECKSTYLE: resume MagicNumber check
}
