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
 * @history creation 11/10/2012
 *
 * HISTORY
* VERSION:4.8:FA:FA-2964:15/11/2021:[PATRIUS] Javadoc incoherente pour TidalCorrection (UT1 correction) 
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:144:06/11/2013:Changed UT1-UTC correction to UT1-TAI correction
 * VERSION::DM:660:24/09/2016:add getters to frames configuration
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.configuration.tides;

import fr.cnes.sirius.patrius.frames.configuration.FrameConvention;
import fr.cnes.sirius.patrius.frames.configuration.eop.PoleCorrection;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;

//CHECKSTYLE: stop MagicNumber check
// Reason: model - Orekit code

/**
 * <p>
 * This class applies the integral Ray model (71 tidal waves) and Brzezinski-Mathews-Bretagnon-Capitaine-Bizouard model
 * (10 lunisolar waves) of the semidiurnal/diurnal variations in the Earth's orientation as recommended in the IERS 2003
 * Conventions (McCarthy, 2002).
 * </p>
 * <p>
 * This class is adapted for the fortran routine <a href="http://hpiers.obspm.fr/iers/models/interp.f">PMUT1_OCEANS</a>.
 * </p>
 * 
 * @see TidalCorrectionModel
 * @see TidalCorrectionCache
 * @author Rami Houdroge
 * @since 1.3
 * @version $Id: IERS2010TidalCorrection.java 18073 2017-10-02 16:48:07Z bignon $
 */
public class IERS2010TidalCorrection implements TidalCorrectionModel {

    /** Serial UID. */
    private static final long serialVersionUID = 1023921197160912494L;

    /** Arcsecond to radians. */
    private static final double SEC_RAD = Constants.ARC_SECONDS_TO_RADIANS;

    /**
     * Oceanic tidal terms. XSIN(j),XCOS(j),YSIN(j),YCOS(j),UTSIN(j),UTCOS(j)
     */
    private static final double[][] TIDAL_TERMS = { { -0.05, 0.94, -0.94, -0.05, 0.396, -0.078 },
        { 0.06, 0.64, -0.64, 0.06, 0.195, -0.059 }, { 0.30, 3.42, -3.42, 0.30, 1.034, -0.314 },
        { 0.08, 0.78, -0.78, 0.08, 0.224, -0.073 }, { 0.46, 4.15, -4.15, 0.45, 1.187, -0.387 },
        { 1.19, 4.96, -4.96, 1.19, 0.966, -0.474 }, { 6.24, 26.31, -26.31, 6.23, 5.118, -2.499 },
        { 0.24, 0.94, -0.94, 0.24, 0.172, -0.090 }, { 1.28, 4.99, -4.99, 1.28, 0.911, -0.475 },
        { -0.28, -0.77, 0.77, -0.28, -0.093, 0.070 }, { 9.22, 25.06, -25.06, 9.22, 3.025, -2.280 },
        { 48.82, 132.91, -132.90, 48.82, 16.020, -12.069 }, { -0.32, -0.86, 0.86, -0.32, -0.103, 0.078 },
        { -0.66, -1.72, 1.72, -0.66, -0.194, 0.154 }, { -0.42, -0.92, 0.92, -0.42, -0.083, 0.074 },
        { -0.30, -0.64, 0.64, -0.30, -0.057, 0.050 }, { -1.61, -3.46, 3.46, -1.61, -0.308, 0.271 },
        { -4.48, -9.61, 9.61, -4.48, -0.856, 0.751 }, { -0.90, -1.93, 1.93, -0.90, -0.172, 0.151 },
        { -0.86, -1.81, 1.81, -0.86, -0.161, 0.137 }, { 1.54, 3.03, -3.03, 1.54, 0.315, -0.189 },
        { -0.29, -0.58, 0.58, -0.29, -0.062, 0.035 }, { 26.13, 51.25, -51.25, 26.13, 5.512, -3.095 },
        { -0.22, -0.42, 0.42, -0.22, -0.047, 0.025 }, { -0.61, -1.20, 1.20, -0.61, -0.134, 0.070 },
        { 1.54, 3.00, -3.00, 1.54, 0.348, -0.171 }, { -77.48, -151.74, 151.74, -77.48, -17.620, 8.548 },
        { -10.52, -20.56, 20.56, -10.52, -2.392, 1.159 }, { 0.23, 0.44, -0.44, 0.23, 0.052, -0.025 },
        { -0.61, -1.19, 1.19, -0.61, -0.144, 0.065 }, { -1.09, -2.11, 2.11, -1.09, -0.267, 0.111 },
        { -0.69, -1.43, 1.43, -0.69, -0.288, 0.043 }, { -3.46, -7.28, 7.28, -3.46, -1.610, 0.187 },
        { -0.69, -1.44, 1.44, -0.69, -0.320, 0.037 }, { -0.37, -1.06, 1.06, -0.37, -0.407, -0.005 },
        { -0.17, -0.51, 0.51, -0.17, -0.213, -0.005 }, { -1.10, -3.42, 3.42, -1.09, -1.436, -0.037 },
        { -0.70, -2.19, 2.19, -0.70, -0.921, -0.023 }, { -0.15, -0.46, 0.46, -0.15, -0.193, -0.005 },
        { -0.03, -0.59, 0.59, -0.03, -0.396, -0.024 }, { -0.02, -0.38, 0.38, -0.02, -0.253, -0.015 },
        { -0.49, -0.04, 0.63, 0.24, -0.089, -0.011 }, { -1.33, -0.17, 1.53, 0.68, -0.224, -0.032 },
        { -6.08, -1.61, 3.13, 3.35, -0.637, -0.177 }, { -7.59, -2.05, 3.44, 4.23, -0.745, -0.222 },
        { -0.52, -0.14, 0.22, 0.29, -0.049, -0.015 }, { 0.47, 0.11, -0.10, -0.27, 0.033, 0.013 },
        { 2.12, 0.49, -0.41, -1.23, 0.141, 0.058 }, { -56.87, -12.93, 11.15, 32.88, -3.795, -1.556 },
        { -0.54, -0.12, 0.10, 0.31, -0.035, -0.015 }, { -11.01, -2.40, 1.89, 6.41, -0.698, -0.298 },
        { -0.51, -0.11, 0.08, 0.30, -0.032, -0.014 }, { 0.98, 0.11, -0.11, -0.58, 0.050, 0.022 },
        { 1.13, 0.11, -0.13, -0.67, 0.056, 0.025 }, { 12.32, 1.00, -1.41, -7.31, 0.605, 0.266 },
        { -330.15, -26.96, 37.58, 195.92, -16.195, -7.140 }, { -1.01, -0.07, 0.11, 0.60, -0.049, -0.021 },
        { 2.47, -0.28, -0.44, -1.48, 0.111, 0.034 }, { 9.40, -1.44, -1.88, -5.65, 0.425, 0.117 },
        { -2.35, 0.37, 0.47, 1.41, -0.106, -0.029 }, { -1.04, 0.17, 0.21, 0.62, -0.047, -0.013 },
        { -8.51, 3.50, 3.29, 5.11, -0.437, -0.019 }, { -144.13, 63.56, 59.23, 86.56, -7.547, -0.159 },
        { 1.19, -0.56, -0.52, -0.72, 0.064, 0.000 }, { 0.49, -0.25, -0.23, -0.29, 0.027, -0.001 },
        { -38.48, 19.14, 17.72, 23.11, -2.104, 0.041 }, { -11.44, 5.75, 5.32, 6.87, -0.627, 0.015 },
        { -1.24, 0.63, 0.58, 0.75, -0.068, 0.002 }, { -1.77, 1.79, 1.71, 1.04, -0.146, 0.037 },
        { -0.77, 0.78, 0.75, 0.45, -0.064, 0.017 }, { -0.33, 0.62, 0.65, 0.19, -0.049, 0.018 } };

    /** Multipliers of GMST + pi and Delaunay arguments. */
    private static final double[][] MULTIPLIERS = { { 1, -1, 0, -2, -2, -2 }, { 1, -2, 0, -2, 0, -1 },
        { 1, -2, 0, -2, 0, -2 }, { 1, 0, 0, -2, -2, -1 }, { 1, 0, 0, -2, -2, -2 }, { 1, -1, 0, -2, 0, -1 },
        { 1, -1, 0, -2, 0, -2 }, { 1, 1, 0, -2, -2, -1 }, { 1, 1, 0, -2, -2, -2 }, { 1, 0, 0, -2, 0, 0 },
        { 1, 0, 0, -2, 0, -1 }, { 1, 0, 0, -2, 0, -2 }, { 1, -2, 0, 0, 0, 0 }, { 1, 0, 0, 0, -2, 0 },
        { 1, -1, 0, -2, 2, -2 }, { 1, 1, 0, -2, 0, -1 }, { 1, 1, 0, -2, 0, -2 }, { 1, -1, 0, 0, 0, 0 },
        { 1, -1, 0, 0, 0, -1 }, { 1, 1, 0, 0, -2, 0 }, { 1, 0, -1, -2, 2, -2 }, { 1, 0, 0, -2, 2, -1 },
        { 1, 0, 0, -2, 2, -2 }, { 1, 0, 1, -2, 2, -2 }, { 1, 0, -1, 0, 0, 0 }, { 1, 0, 0, 0, 0, 1 },
        { 1, 0, 0, 0, 0, 0 }, { 1, 0, 0, 0, 0, -1 }, { 1, 0, 0, 0, 0, -2 }, { 1, 0, 1, 0, 0, 0 },
        { 1, 0, 0, 2, -2, 2 }, { 1, -1, 0, 0, 2, 0 }, { 1, 1, 0, 0, 0, 0 }, { 1, 1, 0, 0, 0, -1 },
        { 1, 0, 0, 0, 2, 0 }, { 1, 2, 0, 0, 0, 0 }, { 1, 0, 0, 2, 0, 2 }, { 1, 0, 0, 2, 0, 1 },
        { 1, 0, 0, 2, 0, 0 }, { 1, 1, 0, 2, 0, 2 }, { 1, 1, 0, 2, 0, 1 }, { 2, -3, 0, -2, 0, -2 },
        { 2, -1, 0, -2, -2, -2 }, { 2, -2, 0, -2, 0, -2 }, { 2, 0, 0, -2, -2, -2 }, { 2, 0, 1, -2, -2, -2 },
        { 2, -1, -1, -2, 0, -2 }, { 2, -1, 0, -2, 0, -1 }, { 2, -1, 0, -2, 0, -2 }, { 2, -1, 1, -2, 0, -2 },
        { 2, 1, 0, -2, -2, -2 }, { 2, 1, 1, -2, -2, -2 }, { 2, -2, 0, -2, 2, -2 }, { 2, 0, -1, -2, 0, -2 },
        { 2, 0, 0, -2, 0, -1 }, { 2, 0, 0, -2, 0, -2 }, { 2, 0, 1, -2, 0, -2 }, { 2, -1, 0, -2, 2, -2 },
        { 2, 1, 0, -2, 0, -2 }, { 2, -1, 0, 0, 0, 0 }, { 2, -1, 0, 0, 0, -1 }, { 2, 0, -1, -2, 2, -2 },
        { 2, 0, 0, -2, 2, -2 }, { 2, 0, 1, -2, 2, -2 }, { 2, 0, 0, 0, 0, 1 }, { 2, 0, 0, 0, 0, 0 },
        { 2, 0, 0, 0, 0, -1 }, { 2, 0, 0, 0, 0, -2 }, { 2, 1, 0, 0, 0, 0 }, { 2, 1, 0, 0, 0, -1 },
        { 2, 0, 0, 2, 0, 2 } };

    /** mas unit. */
    private static final double UNIT = 1.0e-6;

    /** Current correction. */
    private TidalCorrection currentSet = this.computeCorrections(AbsoluteDate.J2000_EPOCH);

    /** token. */
    private final Object token = new Object();

    /** {@inheritDoc} */
    @Override
    public PoleCorrection getPoleCorrection(final AbsoluteDate date) {
        synchronized (this.token) {
            if (MathLib.abs(this.currentSet.getDate().durationFrom(date)) < Precision.EPSILON) {
                return this.currentSet.getPoleCorrection();
            } else {
                this.currentSet = this.computeCorrections(date);
                return this.currentSet.getPoleCorrection();
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public double getUT1Correction(final AbsoluteDate date) {
        synchronized (this.token) {
            if (MathLib.abs(this.currentSet.getDate().durationFrom(date)) < Precision.EPSILON) {
                return this.currentSet.getUT1Correction();
            } else {
                this.currentSet = this.computeCorrections(date);
                return this.currentSet.getUT1Correction();
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public double getLODCorrection(final AbsoluteDate date) {
        synchronized (this.token) {
            if (MathLib.abs(this.currentSet.getDate().durationFrom(date)) < Precision.EPSILON) {
                return this.currentSet.getLODCorrection();
            } else {
                this.currentSet = this.computeCorrections(date);
                return this.currentSet.getLODCorrection();
            }
        }
    }

    /**
     * Private method to compute the corrections according to the IERS2000 model at a given date.
     * 
     * @param date
     *        date
     * @return the pole, UT1-TAI and length of day corrections
     */
    // CHECKSTYLE: stop CommentRatio check
    // Reason: model - Orekit code containing lots of arrays models
    private TidalCorrection computeCorrections(final AbsoluteDate date) {
        // CHECKSTYLE: resume CommentRatio check

        // Julian centuries elapsed since 1950-01-01T12:00:00.000 TT
        final double t = date.offsetFrom(AbsoluteDate.J2000_EPOCH, TimeScalesFactory.getTT())
            / Constants.JULIAN_CENTURY;
        final double t2 = t * t;
        final double t3 = t * t2;
        final double t4 = t2 * t2;

        final double[] arg = new double[6];
        final double[] darg = new double[6];

        arg[0] = (67310.54841 + (876600. * 3600. + 8640184.812866) * t + 0.093104 * t2 - 6.2e-6 * t3) * 15.0 + 648000.0;
        arg[0] = this.dmod(arg[0], 12960000.) * SEC_RAD;

        darg[0] = (876600. * 3600. + 8640184.812866 + 2. * 0.093104 * t - 3 * 6.2e-6 * t2) * 15.;
        darg[0] = darg[0] * SEC_RAD / 36525.0;

        arg[1] = -0.00024470 * t4 + 0.051635 * t3 + 31.8792 * t2 + 1717915923.2178 * t + 485868.249036;
        arg[1] = this.dmod(arg[1], 1296000.) * SEC_RAD;

        darg[1] = -4. * 0.00024470 * t3 + 3. * 0.051635 * t2 + 2. * 31.8792 * t + 1717915923.2178;
        darg[1] = darg[1] * SEC_RAD / 36525.0;

        arg[2] = -0.00001149 * t4 - 0.000136 * t3 - 0.5532 * t2 + 129596581.0481 * t + 1287104.79305;
        arg[2] = this.dmod(arg[2], 1296000.) * SEC_RAD;

        darg[2] = -4. * 0.00001149 * t3 - 3. * 0.000136 * t2 - 2. * 0.5532 * t + 129596581.0481;
        darg[2] = darg[2] * SEC_RAD / 36525.0;

        arg[3] = 0.00000417 * t4 - 0.001037 * t3 - 12.7512 * t2 + 1739527262.8478 * t + 335779.526232;
        arg[3] = this.dmod(arg[3], 1296000.) * SEC_RAD;

        darg[3] = 4. * 0.00000417 * t3 - 3. * 0.001037 * t2 - 2. * 12.7512 * t + 1739527262.8478;
        darg[3] = darg[3] * SEC_RAD / 36525.0;

        arg[4] = -0.00003169 * t4 + 0.006593 * t3 - 6.3706 * t2 + 1602961601.2090 * t + 1072260.70369;
        arg[4] = this.dmod(arg[4], 1296000.) * SEC_RAD;

        darg[4] = -4. * 0.00003169 * t3 + 3. * 0.006593 * t2 - 2. * 6.3706 * t + 1602961601.2090;
        darg[4] = darg[4] * SEC_RAD / 36525.0;

        arg[5] = -0.00005939 * t4 + 0.007702 * t3 + 7.4722 * t2 - 6962890.2665 * t + 450160.398036;
        arg[5] = this.dmod(arg[5], 1296000.) * SEC_RAD;

        darg[5] = -4. * 0.00005939 * t3 + 3. * 0.007702 * t2 + 2. * 7.4722 * t - 6962890.2665;
        darg[5] = darg[5] * SEC_RAD / 36525.0;

        double corX = 0.;
        double corY = 0.;
        double corUT1 = 0.;
        double corLOD = 0.;

        double ag;
        double dag;

        double c;
        double s;

        for (int line = 0; line < TIDAL_TERMS.length; line++) {

            ag = 0.;
            dag = 0.;

            for (int i = 0; i < 6; i++) {
                ag += MULTIPLIERS[line][i] * arg[i];
                dag += MULTIPLIERS[line][i] * darg[i];
            }

            ag = ag % (2 * FastMath.PI);

            final double[] sincos = MathLib.sinAndCos(ag);
            s = sincos[0];
            c = sincos[1];

            corX = corX + TIDAL_TERMS[line][1] * c + TIDAL_TERMS[line][0] * s;
            corY = corY + TIDAL_TERMS[line][3] * c + TIDAL_TERMS[line][2] * s;
            corUT1 = corUT1 + TIDAL_TERMS[line][5] * c + TIDAL_TERMS[line][4] * s;
            corLOD = corLOD - (-TIDAL_TERMS[line][5] * s + TIDAL_TERMS[line][4] * c) * dag;

        }

        // arcsecs to radians
        corX = corX * UNIT * SEC_RAD;
        corY = corY * UNIT * SEC_RAD;
        // seconds
        corUT1 = corUT1 * UNIT;
        corLOD = corLOD * UNIT;

        return new TidalCorrection(date, new PoleCorrection(corX, corY), corUT1, corLOD);
    }

    /**
     * Remainder.
     * 
     * @param d
     *        number
     * @param i
     *        number
     * @return remainder of d / i
     */
    private double dmod(final double d, final double i) {
        return d % i;
    }

    /** {@inheritDoc} */
    @Override
    public FrameConvention getOrigin() {
        return FrameConvention.IERS2010;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isDirect() {
        return true;
    }

    // CHECKSTYLE: resume MagicNumber check
}
