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
 * @history creation 28/11/2012
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2367:27/05/2020:Configuration de changement de repère simplifiee 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:144:06/11/2013:Changed UT1-UTC correction to UT1-TAI correction
 * VERSION::DM:317:06/03/2015: STELA integration in CIRF with referential choice (ICRF, CIRF or MOD)
 * VERSION::FA:1301:06/09/2017:Generalized EOP history
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.configuration;

import fr.cnes.sirius.patrius.frames.configuration.eop.EOPHistoryFactory;
import fr.cnes.sirius.patrius.frames.configuration.eop.EOPInterpolators;
import fr.cnes.sirius.patrius.frames.configuration.libration.LibrationCorrectionModel;
import fr.cnes.sirius.patrius.frames.configuration.libration.LibrationCorrectionModelFactory;
import fr.cnes.sirius.patrius.frames.configuration.precessionnutation.PrecessionNutationModel;
import fr.cnes.sirius.patrius.frames.configuration.precessionnutation.PrecessionNutationModelFactory;
import fr.cnes.sirius.patrius.frames.configuration.sp.SPrimeModelFactory;
import fr.cnes.sirius.patrius.frames.configuration.tides.TidalCorrectionModel;
import fr.cnes.sirius.patrius.frames.configuration.tides.TidalCorrectionModelFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusExceptionWrapper;

/**
 * Frames configuration factory. Contains useful configurations.
 * 
 * @author Rami Houdroge
 * 
 * @since 1.3
 * 
 * @version $Id: FramesConfigurationFactory.java 18073 2017-10-02 16:48:07Z bignon $
 */
public final class FramesConfigurationFactory {

    /**
     * Private constructor.
     */
    private FramesConfigurationFactory() {
    }

    /**
     * Gets the default IERS2010 configuration (always the same instance, not a new one).
     * 
     * @return default IERS2010 configuration
     */
    public static FramesConfiguration getIERS2010Configuration() {

        // Configurations builder
        final FramesConfigurationBuilder builder = new FramesConfigurationBuilder();

        // Tides and libration
        final TidalCorrectionModel tides = TidalCorrectionModelFactory.TIDE_IERS2010_INTERPOLATED;
        final LibrationCorrectionModel lib = LibrationCorrectionModelFactory.LIBRATION_IERS2010;

        // Polar Motion
        final PolarMotion defaultPolarMotion = new PolarMotion(true, tides, lib, SPrimeModelFactory.SP_IERS2010);

        // Diurnal rotation
        final DiurnalRotation defaultDiurnalRotation = new DiurnalRotation(tides, lib);

        // Precession Nutation
        final PrecessionNutation precNut = new PrecessionNutation(true,
            PrecessionNutationModelFactory.PN_IERS2010_INTERPOLATED_NON_CONSTANT);

        builder.setDiurnalRotation(defaultDiurnalRotation);
        builder.setPolarMotion(defaultPolarMotion);
        builder.setPrecessionNutation(precNut);
        try {
            builder.setEOPHistory(EOPHistoryFactory.getEOP2000History(EOPInterpolators.LAGRANGE4));
        } catch (final PatriusException e) {
            throw new PatriusExceptionWrapper(e);
        }

        return builder.getConfiguration();
    }

    /**
     * Gets the default IERS2003 configuration (always the same instance, not a new one).
     * 
     * @param ignoreTides
     *        tides if tides are to be ignored, false otherwise
     * @return default IERS2003 configuration
     */
    public static FramesConfiguration getIERS2003Configuration(final boolean ignoreTides) {

        // Configurations builder
        final FramesConfigurationBuilder builder = new FramesConfigurationBuilder();

        // Tides and libration
        final TidalCorrectionModel tides = ignoreTides ? TidalCorrectionModelFactory.NO_TIDE :
            TidalCorrectionModelFactory.TIDE_IERS2003_INTERPOLATED;
        final LibrationCorrectionModel lib = LibrationCorrectionModelFactory.NO_LIBRATION;

        // Polar Motion
        final PolarMotion defaultPolarMotion = new PolarMotion(true, tides, lib, SPrimeModelFactory.SP_IERS2003);

        // Diurnal rotation
        final DiurnalRotation defaultDiurnalRotation = new DiurnalRotation(tides, lib);

        // Precession Nutation
        final PrecessionNutation precNut = new PrecessionNutation(false,
            PrecessionNutationModelFactory.PN_IERS2003_INTERPOLATED_CONSTANT);

        builder.setDiurnalRotation(defaultDiurnalRotation);
        builder.setPolarMotion(defaultPolarMotion);
        builder.setPrecessionNutation(precNut);
        try {
            builder.setEOPHistory(EOPHistoryFactory.getEOP2000History(EOPInterpolators.LAGRANGE4));
        } catch (final PatriusException e) {
            throw new PatriusExceptionWrapper(e);
        }

        return builder.getConfiguration();
    }

    /**
     * Gets the official STELA configuration (always the same instance, not a new one).
     * 
     * @return official STELA configuration
     */
    public static FramesConfiguration getStelaConfiguration() {

        // Configurations builder
        final FramesConfigurationBuilder builder = new FramesConfigurationBuilder();

        // Precession/nutation
        builder.setPrecessionNutation(new PrecessionNutation(true, PrecessionNutationModelFactory.PN_STELA));

        return builder.getConfiguration();
    }

    /**
     * Gets a simple configuration (always the same instance, not a new one).
     * It contains only an optional precession / nutation model
     * {@link PrecessionNutationModelFactory#PN_IERS2010_INTERPOLATED_NON_CONSTANT} without use of EOP data.
     * This configuration is useful if you don't want to provide or don't have EOP data while keeping good model
     * accuracy.
     * Particularly this configuration always returns TIRF = ITRF.
     * 
     * @param usePrecessionNutationModel true if default IERS precession-nutation model should be used (without any need
     *        for additional data), false if no precession-nutation model shall be used (in this case CIRF frame = GCRF
     *        frame)
     * @return simple frame configuration
     */
    public static FramesConfiguration getSimpleConfiguration(final boolean usePrecessionNutationModel) {

        // Configurations builder
        final FramesConfigurationBuilder builder = new FramesConfigurationBuilder();

        // Precession Nutation
        final PrecessionNutationModel pnModel;
        if (usePrecessionNutationModel) {
            pnModel = PrecessionNutationModelFactory.PN_IERS2010_INTERPOLATED_NON_CONSTANT;
        } else {
            pnModel = PrecessionNutationModelFactory.NO_PN;
        }
        builder.setPrecessionNutation(new PrecessionNutation(false, pnModel));

        return builder.getConfiguration();
    }
}
