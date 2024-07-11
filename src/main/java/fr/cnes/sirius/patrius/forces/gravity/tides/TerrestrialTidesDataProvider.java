/**
 * 
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
 * @history creation 08/12/2014
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:241:08/12/2014:improved tides conception
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.gravity.tides;

import fr.cnes.sirius.patrius.forces.gravity.tides.TidesStandards.TidesStandard;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * <p>
 * Terrestrial tides parameters given by the IERS 2003 standard.
 * </p>
 * 
 * @concurrency thread safe
 * 
 * @author maggioranic
 * 
 * @version $Id: TerrestrialTidesDataProvider.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 2.3.1
 * 
 */
@SuppressWarnings("PMD.MethodReturnsInternalArray")
public final class TerrestrialTidesDataProvider implements ITerrestrialTidesDataProvider {

    // QA exemption : MagicNumber disabled for the file
    // CHECKSTYLE: stop MagicNumberCheck

    /** Serial UID. */
    private static final long serialVersionUID = -846424396390236244L;

    /** Flat array for the constant nutation coefficients. */
    private static final double[] FLAT_COEFS =
    { 134.96340251,
        1717915923.2178,
        31.8792,
        0.051635,
        -0.00024470,
        357.52910918,
        129596581.0481,
        -0.5532,
        0.000136,
        -0.00001149,
        93.27209062,
        1739527262.8478,
        -12.7512,
        -0.001037,
        0.00000417,
        297.85019547,
        1602961601.2090,
        -6.3706,
        0.006593,
        -0.00003169,
        125.04455501,
        Double.NaN,
        7.4722,
        0.007702,
        -0.00005939 };

    /** Dimension. */
    private static final int DIM = 5;

    /** Constant nutation coefficients. */
    private static double[][] cf;

    /** Number of waves. */
    private static final int WAVE_NUMBER = 71;

    /**
     * Doodson numbers table. This table lists the most important waves.
     * 
     * @comment see Obelix algorithm for constant values : fms_charger_std_iers2003.f90
     */
    private static final double[] DOODSON = { 55.565, 55.575, 56.554, 57.555, 57.565, 58.554, 63.655, 65.445, 65.455,
        65.465, 65.655, 73.555, 75.355, 75.555, 75.565, 75.575, 83.655, 85.455, 85.465, 93.555, 95.355, 125.755,
        127.555, 135.645, 135.655, 137.455, 145.545, 145.555, 147.555, 153.655, 155.445, 155.455, 155.655, 155.665,
        157.455, 157.465, 162.556, 163.545, 163.555, 164.554, 164.556, 165.345, 165.535, 165.545, 165.555, 165.565,
        165.575, 166.455, 166.544, 166.554, 166.556, 166.564, 167.355, 167.365, 167.555, 167.565, 168.554, 173.655,
        173.665, 175.445, 175.455, 175.465, 183.555, 185.355, 185.555, 185.565, 185.575, 195.455, 195.465, 245.655,
        255.555 };

    /**
     * Anelasticity correction Love number2.
     * see Obelix algorithm for constant values : fms_charger_std_iers2003.f90
     */
    private static final double[] NLOVE2 = new double[] { 0.3019, 0.0, 0.2983, -0.00144, 0.30102, -0.0013 };

    /**
     * Anelasticity correction Love number3.
     * see Obelix algorithm for constant values : fms_charger_std_iers2003.f90
     */
    private static final double[] NLOVE3 = new double[] { 0.093, 0.093, 0.093, 0.094 };

    /**
     * Love ellipticity.
     * see Obelix algorithm for constant values : fms_charger_std_iers2003.f90
     */
    private static final double[] NLOVEELLIPTICITY = new double[] { -0.00089, -0.00080, -0.00057 };

    /**
     * Frequency correction.
     * The reference for Love number corrections is OBELIX library (L12) (see fms_charger_std_iers2003.f90).
     */
    private static final double[][] ADK = new double[WAVE_NUMBER][2];

    /**
     * Tide standard
     */
    private final TidesStandard standard;

    /**
     * Simple constructor.
     * 
     * @throws PatriusException
     *         thrown if standard is different from IERS 2003
     * 
     * @since 2.3.1
     */
    public TerrestrialTidesDataProvider() throws PatriusException {
        this(TidesStandard.IERS2003);
    }

    /**
     * Simple constructor.
     * 
     * @param tideStandard
     *        the tide standard
     * @throws PatriusException
     *         thrown if standard is different from IERS 2003
     * 
     * @since 2.3.1
     */
    public TerrestrialTidesDataProvider(final TidesStandard tideStandard) throws PatriusException {
        if (tideStandard != TidesStandard.IERS2003) {
            throw new PatriusException(PatriusMessages.WRONG_STANDARD);
        }
        this.standard = tideStandard;
    }

    /** Static inits for all arrays. */
    static {
        /*
         * shared nutation coefficients between the different standards.
         */
        cf = new double[DIM][DIM];

        for (int i = 0; i < DIM; i++) {
            for (int j = 0; j < DIM; j++) {
                cf[i][j] = FLAT_COEFS[i * DIM + j];
            }
        }
        initFrequencyCorrection();
    }

    /** {@inheritDoc} */
    @Override
    public double[] getAnelasticityCorrectionLoveNumber2() {
        // Note : static init.
        return NLOVE2;
    }

    /** {@inheritDoc} */
    @Override
    public double[] getAnelasticityCorrectionLoveNumber3() {
        // Note : static init.
        return NLOVE3;
    }

    /** {@inheritDoc} */
    @Override
    public double[] getEllipticityCorrectionLoveNumber2() {
        // Note : static init.
        return NLOVEELLIPTICITY;
    }

    /** Static init for adk array. */
    // CHECKSTYLE: stop MethodLength check
    private static void initFrequencyCorrection() {
        // CHECKSTYLE: resume MethodLength check

        // adk[i][0] contains the real part of Love number correction
        // adk[i][1] contains the imaginary part of Love number correction

        // store real and imaginary parts of Love numbers
        ADK[0][0] = 16.6e-12;
        ADK[0][1] = -6.7e-12;

        // store real and imaginary parts of Love numbers
        ADK[1][0] = -0.1e-12;
        ADK[1][1] = 0.1e-12;

        // store real and imaginary parts of Love numbers
        ADK[2][0] = -1.2e-12;
        ADK[2][1] = 0.8e-12;

        // store real and imaginary parts of Love numbers
        ADK[3][0] = -5.5e-12;
        ADK[3][1] = 4.3e-12;

        // store real and imaginary parts of Love numbers
        ADK[4][0] = 0.1e-12;
        ADK[4][1] = -0.1e-12;

        // store real and imaginary parts of Love numbers
        ADK[5][0] = -0.3e-12;
        ADK[5][1] = 0.2e-12;

        // store real and imaginary parts of Love numbers
        ADK[6][0] = -0.3e-12;
        ADK[6][1] = 0.7e-12;

        // store real and imaginary parts of Love numbers
        ADK[7][0] = 0.1e-12;
        ADK[7][1] = -0.2e-12;

        // store real and imaginary parts of Love numbers
        ADK[8][0] = -1.2e-12;
        ADK[8][1] = 3.7e-12;

        // store real and imaginary parts of Love numbers
        ADK[9][0] = 0.1e-12;
        ADK[9][1] = -0.2e-12;

        // store real and imaginary parts of Love numbers
        ADK[10][0] = 0.1e-12;
        ADK[10][1] = -0.2e-12;

        // store real and imaginary parts of Love numbers
        ADK[11][0] = 0.0e-12;
        ADK[11][1] = 0.6e-12;

        // store real and imaginary parts of Love numbers
        ADK[12][0] = 0.0e-12;
        ADK[12][1] = 0.3e-12;

        // store real and imaginary parts of Love numbers
        ADK[13][0] = 0.6e-12;
        ADK[13][1] = 6.3e-12;

        // store real and imaginary parts of Love numbers
        ADK[14][0] = 0.2e-12;
        ADK[14][1] = 2.6e-12;

        // store real and imaginary parts of Love numbers
        ADK[15][0] = 0.0e-12;
        ADK[15][1] = 0.2e-12;

        // store real and imaginary parts of Love numbers
        ADK[16][0] = 0.1e-12;
        ADK[16][1] = 0.2e-12;

        // store real and imaginary parts of Love numbers
        ADK[17][0] = 0.4e-12;
        ADK[17][1] = 1.1e-12;

        // store real and imaginary parts of Love numbers
        ADK[18][0] = 0.2e-12;
        ADK[18][1] = 0.5e-12;

        // store real and imaginary parts of Love numbers
        ADK[19][0] = 0.1e-12;
        ADK[19][1] = 0.2e-12;

        // store real and imaginary parts of Love numbers
        ADK[20][0] = 0.1e-12;
        ADK[20][1] = 0.1e-12;

        // store real and imaginary parts of Love numbers
        ADK[21][0] = -0.1e-12;
        ADK[21][1] = 0.0e-12;

        // store real and imaginary parts of Love numbers
        ADK[22][0] = -0.1e-12;
        ADK[22][1] = 0.0e-12;

        // store real and imaginary parts of Love numbers
        ADK[23][0] = -0.1e-12;
        ADK[23][1] = 0.0e-12;

        // store real and imaginary parts of Love numbers
        ADK[24][0] = -0.7e-12;
        ADK[24][1] = 0.1e-12;

        // store real and imaginary parts of Love numbers
        ADK[25][0] = -0.1e-12;
        ADK[25][1] = 0.0e-12;

        // store real and imaginary parts of Love numbers
        ADK[26][0] = -1.3e-12;
        ADK[26][1] = 0.1e-12;

        // store real and imaginary parts of Love numbers
        ADK[27][0] = -6.8e-12;
        ADK[27][1] = 0.6e-12;

        // store real and imaginary parts of Love numbers
        ADK[28][0] = 0.1e-12;
        ADK[28][1] = 0.0e-12;

        // store real and imaginary parts of Love numbers
        ADK[29][0] = 0.1e-12;
        ADK[29][1] = 0.0e-12;

        // store real and imaginary parts of Love numbers
        ADK[30][0] = 0.1e-12;
        ADK[30][1] = 0.0e-12;

        // store real and imaginary parts of Love numbers
        ADK[31][0] = 0.4e-12;
        ADK[31][1] = 0.0e-12;

        // store real and imaginary parts of Love numbers
        ADK[32][0] = 1.3e-12;
        ADK[32][1] = -0.1e-12;

        // store real and imaginary parts of Love numbers
        ADK[33][0] = 0.3e-12;
        ADK[33][1] = 0.0e-12;

        // store real and imaginary parts of Love numbers
        ADK[34][0] = 0.3e-12;
        ADK[34][1] = 0.0e-12;

        // store real and imaginary parts of Love numbers
        ADK[35][0] = 0.1e-12;
        ADK[35][1] = 0.0e-12;

        // store real and imaginary parts of Love numbers
        ADK[36][0] = -1.9e-12;
        ADK[36][1] = 0.1e-12;

        // store real and imaginary parts of Love numbers
        ADK[37][0] = 0.5e-12;
        ADK[37][1] = 0.0e-12;

        // store real and imaginary parts of Love numbers
        ADK[38][0] = -43.4e-12;
        ADK[38][1] = 2.9e-12;

        // store real and imaginary parts of Love numbers
        ADK[39][0] = 0.6e-12;
        ADK[39][1] = 0.0e-12;

        // store real and imaginary parts of Love numbers
        ADK[40][0] = 1.6e-12;
        ADK[40][1] = -0.1e-12;

        // Extracted part (method length reduction)
        part2GetFreqCorr();
    }

    /**
     * Extracted part from the method initFrequencyCorrection, to reduce some quality metrics.
     */
    private static void part2GetFreqCorr() {
        // store real and imaginary parts of Love numbers
        ADK[41][0] = 0.1e-12;
        ADK[41][1] = 0.0e-12;

        // store real and imaginary parts of Love numbers
        ADK[42][0] = 0.1e-12;
        ADK[42][1] = 0.0e-12;

        // store real and imaginary parts of Love numbers
        ADK[43][0] = -8.8e-12;
        ADK[43][1] = 0.5e-12;

        // store real and imaginary parts of Love numbers
        ADK[44][0] = 470.9e-12;
        ADK[44][1] = -30.2e-12;

        // store real and imaginary parts of Love numbers
        ADK[45][0] = 68.1e-12;
        ADK[45][1] = -4.6e-12;

        // store real and imaginary parts of Love numbers
        ADK[46][0] = -1.6e-12;
        ADK[46][1] = 0.1e-12;

        // store real and imaginary parts of Love numbers
        ADK[47][0] = 0.1e-12;
        ADK[47][1] = 0.0e-12;

        // store real and imaginary parts of Love numbers
        ADK[48][0] = -0.1e-12;
        ADK[48][1] = 0.0e-12;

        // store real and imaginary parts of Love numbers
        ADK[49][0] = -20.6e-12;
        ADK[49][1] = -0.3e-12;

        // store real and imaginary parts of Love numbers
        ADK[50][0] = 0.3e-12;
        ADK[50][1] = 0.0e-12;

        // store real and imaginary parts of Love numbers
        ADK[51][0] = -0.3e-12;
        ADK[51][1] = 0.0e-12;

        // store real and imaginary parts of Love numbers
        ADK[52][0] = -0.2e-12;
        ADK[52][1] = 0.0e-12;

        // store real and imaginary parts of Love numbers
        ADK[53][0] = -0.1e-12;
        ADK[53][1] = 0.0e-12;

        // store real and imaginary parts of Love numbers
        ADK[54][0] = -5.0e-12;
        ADK[54][1] = 0.3e-12;

        // store real and imaginary parts of Love numbers
        ADK[55][0] = 0.2e-12;
        ADK[55][1] = 0.0e-12;

        // store real and imaginary parts of Love numbers
        ADK[56][0] = -0.2e-12;
        ADK[56][1] = 0.0e-12;

        // store real and imaginary parts of Love numbers
        ADK[57][0] = -0.5e-12;
        ADK[57][1] = 0.0e-12;

        // store real and imaginary parts of Love numbers
        ADK[58][0] = -0.1e-12;
        ADK[58][1] = 0.0e-12;

        // store real and imaginary parts of Love numbers
        ADK[59][0] = 0.1e-12;
        ADK[59][1] = 0.0e-12;

        // store real and imaginary parts of Love numbers
        ADK[60][0] = -2.1e-12;
        ADK[60][1] = 0.1e-12;

        // store real and imaginary parts of Love numbers
        ADK[61][0] = -0.4e-12;
        ADK[61][1] = 0.0e-12;

        // store real and imaginary parts of Love numbers
        ADK[62][0] = -0.2e-12;
        ADK[62][1] = 0.0e-12;

        // store real and imaginary parts of Love numbers
        ADK[63][0] = -0.1e-12;
        ADK[63][1] = 0.0e-12;

        // store real and imaginary parts of Love numbers
        ADK[64][0] = -0.6e-12;
        ADK[64][1] = 0.0e-12;

        // store real and imaginary parts of Love numbers
        ADK[65][0] = -0.4e-12;
        ADK[65][1] = 0.0e-12;

        // store real and imaginary parts of Love numbers
        ADK[66][0] = -0.1e-12;
        ADK[66][1] = 0.0e-12;

        // store real and imaginary parts of Love numbers
        ADK[67][0] = -0.1e-12;
        ADK[67][1] = 0.0e-12;

        // store real and imaginary parts of Love numbers
        ADK[68][0] = -0.1e-12;
        ADK[68][1] = 0.0e-12;

        // store real and imaginary parts of Love numbers
        ADK[69][0] = -0.3e-12;
        ADK[69][1] = 0.0e-12;

        // store real and imaginary parts of Love numbers
        ADK[70][0] = -1.2e-12;
        ADK[70][1] = 0.0e-12;
    }

    /** {@inheritDoc} */
    @Override
    public double[][] getFrequencyCorrection() {
        // Note : static init.
        return ADK;
    }

    /** {@inheritDoc} */
    @Override
    public double[][] getNutationCoefficients() {
        // see Obelix algorithm for constant values : fms_charger_std_iers2003.f90
        cf[4][1] = -6962890.5431;
        return cf;
    }

    /** {@inheritDoc} */
    @Override
    public double[] getDoodsonNumbers() {
        return DOODSON;
    }

    /** {@inheritDoc} */
    @Override
    public TidesStandard getStandard() {
        return this.standard;
    }
}
