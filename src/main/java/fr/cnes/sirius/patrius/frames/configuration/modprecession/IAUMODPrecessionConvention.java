/**
 * Copyright 2023-2023 CNES
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
 * HISTORY
 * VERSION:4.13:DM:DM-108:08/12/2023:[PATRIUS] Modele d'obliquite et de precession de la Terre
 * VERSION:4.13:DM:DM-120:08/12/2023:[PATRIUS] Merge de la branche patrius-for-lotus dans Patrius
 * VERSION:4.13:DM:DM-101:08/12/2023:[PATRIUS] Harmonisation des eclipses pour les evenements et pour la PRS
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.configuration.modprecession;

import fr.cnes.sirius.patrius.utils.Constants;

//CHECKSTYLE: stop MagicNumber check
//Reason: model with empirical values

/**
 * This class lists all the available precession conventions used in conjunction with MOD and Ecliptic MOD frames.
 * <p>
 * Values from "Expressions for IAU 2000 precession quantities, N. Capitaine, P.T. Wallace, and J. Chapront, 2003".
 * </p>
 *
 * @author Emmanuel Bignon
 *
 * @since 4.13
 */
@SuppressWarnings("PMD.MethodReturnsInternalArray")
//Reason: computation time optimizations
public enum IAUMODPrecessionConvention {

    /** IAU 1976 convention. Coefficients in arcsec as provided in official data. */
    IAU1976(new double[] { 84381.448, -46.8150, -0.00059, 0.001813 }, new double[] { 0, 2306.2181, 0.30188, 0.017998 },
            new double[] { 0, 2004.3109, -0.42665, -0.041833 }, new double[] { 0, 2306.2181, 1.09468, 0.018203 }),

    /** IAU 2000 convention. Coefficients in arcsec as provided in official data. */
    IAU2000(new double[] { 84381.448, -46.84024, -0.00059, 0.001813 }, new double[] { 2.5976176, 2306.0809506,
        0.3019015, 0.0179663, -0.0000327, -0.0000002 }, new double[] { 0, 2004.1917476, -0.4269353, -0.0418251,
            -0.0000601, -0.0000001 }, new double[] { -2.5976176, 2306.0803226, 1.0947790, 0.0182273, 0.0000470,
                -0.0000003 });

    /** Obliquity coefficients. Coefficients in radians. */
    private final double[] obliquityCoefs;

    /** Precession coefficients (zeta). Coefficients in radians. */
    private final double[] precessionZetaCoefs;

    /** Precession coefficients (theta). Coefficients in radians. */
    private final double[] precessionThetaCoefs;

    /** Precession coefficients (Z). Coefficients in radians. */
    private final double[] precessionZCoefs;

    /**
     * COnstructor.
     * 
     * @param obliquityCoefs Obliquity coefficients
     * @param precessionZetaCoefs Precession coefficients (zeta) in arcsec
     * @param precessionThetaCoefs Precession coefficients (theta) in arcsec
     * @param precessionZCoefs Precession coefficients (Z) in arc
     */
    private IAUMODPrecessionConvention(final double[] obliquityCoefs,
            final double[] precessionZetaCoefs,
            final double[] precessionThetaCoefs,
            final double[] precessionZCoefs) {
        this.obliquityCoefs = convertToRadians(obliquityCoefs);
        this.precessionZetaCoefs = convertToRadians(precessionZetaCoefs);
        this.precessionThetaCoefs = convertToRadians(precessionThetaCoefs);
        this.precessionZCoefs = convertToRadians(precessionZCoefs);
    }

    /**
     * Convert array from arcsec to radians.
     * @param array an array
     * @return array in radians
     */
    private double[] convertToRadians(final double[] array) {
        // Copy is performed only once at construction. No performance issues
        // Original data are kept in arcsec on purpose
        final double[] result = new double[array.length];
        System.arraycopy(array, 0, result, 0, array.length);
        for (int i = 0; i < array.length; i++) {
            result[i] *= Constants.ARC_SECONDS_TO_RADIANS;
        }
        return result;
    }

    /**
     * Returns the obliquity coefficients.
     * @return the obliquity coefficients in radians
     */
    public double[] getObliquityCoefs() {
        return obliquityCoefs;
    }

    /**
     * Returns the precession coefficients (zeta).
     * @return the precession coefficients (zeta) in radians
     */
    public double[] getPrecessionZetaCoefs() {
        return precessionZetaCoefs;
    }

    /**
     * Returns the precession coefficients (theta).
     * @return the precession coefficients (theta) in radians
     */
    public double[] getPrecessionThetaCoefs() {
        return precessionThetaCoefs;
    }

    /**
     * Returns the precession coefficients (Z).
     * @return the precession coefficients (zeta) in radians
     */
    public double[] getPrecessionZCoefs() {
        return precessionZCoefs;
    }

    // CHECKSTYLE: resume MagicNumber check
}
