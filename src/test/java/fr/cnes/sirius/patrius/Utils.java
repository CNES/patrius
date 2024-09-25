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
* VERSION:4.13.5:DM:DM-319:03/07/2024:[PATRIUS] Assurer la compatibilite ascendante de la v4.13
* VERSION:4.13.2:DM:DM-222:08/03/2024:[PATRIUS] Assurer la compatibilité ascendante
* VERSION:4.13:DM:DM-103:08/12/2023:[PATRIUS] Optimisation du CIRFProvider
* VERSION:4.13:DM:DM-108:08/12/2023:[PATRIUS] Modele d'obliquite et de precession de la Terre
* VERSION:4.11.1:DM:DM-49:30/06/2023:[PATRIUS] Extraction arbre des reperes SPICE et link avec CelestialBodyFactory
* VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
* VERSION:4.9:DM:DM-3166:10/05/2022:[PATRIUS] Definir l'ICRF comme repere racine 
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
* VERSION:4.8:DM:DM-2922:15/11/2021:[PATRIUS] suppression de l'utilisation de la reflexion Java dans patrius 
* VERSION:4.7:DM:DM-2710:18/05/2021:Methode d'interpolation des EOP 
 * VERSION:4.5:DM:DM-2367:27/05/2020:Configuration de changement de repère simplifiee 
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:1301:06/09/2017:Generalized EOP history
 * END-HISTORY
 */
package fr.cnes.sirius.patrius;

import java.net.URISyntaxException;

import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.bsp.spice.SpiceKernelManager;
import fr.cnes.sirius.patrius.data.DataProvidersManager;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.SolarActivityDataFactory;
import fr.cnes.sirius.patrius.forces.gravity.potential.GravityFieldFactory;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.configuration.DiurnalRotation;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfiguration;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfigurationBuilder;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfigurationFactory;
import fr.cnes.sirius.patrius.frames.configuration.PolarMotion;
import fr.cnes.sirius.patrius.frames.configuration.eop.EOPHistoryFactory;
import fr.cnes.sirius.patrius.frames.configuration.eop.EOPInterpolators;
import fr.cnes.sirius.patrius.frames.configuration.eop.NoEOP2000History;
import fr.cnes.sirius.patrius.frames.configuration.libration.LibrationCorrectionModel;
import fr.cnes.sirius.patrius.frames.configuration.libration.LibrationCorrectionModelFactory;
import fr.cnes.sirius.patrius.frames.configuration.precessionnutation.PrecessionNutation;
import fr.cnes.sirius.patrius.frames.configuration.precessionnutation.PrecessionNutationModelFactory;
import fr.cnes.sirius.patrius.frames.configuration.sp.SPrimeModelFactory;
import fr.cnes.sirius.patrius.frames.configuration.tides.TidalCorrectionModel;
import fr.cnes.sirius.patrius.frames.configuration.tides.TidalCorrectionModelFactory;
import fr.cnes.sirius.patrius.models.earth.GeoMagneticFieldFactory;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.PatriusConfiguration;
import fr.cnes.sirius.patrius.utils.PatriusConfiguration.PatriusVersionCompatibility;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusExceptionWrapper;

public class Utils {

    // epsilon for tests
    public static final double epsilonTest = 1.e-12;

    // epsilon for eccentricity
    public static final double epsilonE = 1.e+5 * epsilonTest;

    // epsilon for circular eccentricity
    public static final double epsilonEcir = 1.e+8 * epsilonTest;

    // epsilon for angles
    public static final double epsilonAngle = 1.e+5 * epsilonTest;

    public static final double ae = 6378136.460;
    public static final double mu = 3.986004415e+14;

    public static void setDataRoot(final String root) {
        try {
            clear();
            final StringBuffer buffer = new StringBuffer();
            for (final String component : root.split(":")) {
                String componentPath;
                componentPath = Utils.class.getClassLoader().getResource(component).toURI().getPath();
                if (buffer.length() > 0) {
                    buffer.append(System.getProperty("path.separator"));
                }
                buffer.append(componentPath);
            }
            System.setProperty(DataProvidersManager.OREKIT_DATA_PATH, buffer.toString());
        } catch (final URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Clear data stored in JVM in static variables.
     */
    public static void clear() {
        PatriusConfiguration.setPatriusCompatibilityMode(PatriusVersionCompatibility.NEW_MODELS);
        FramesFactory.clearConfiguration();
        FramesFactory.clear();
        CelestialBodyFactory.clearCelestialBodyLoaders();
        EOPHistoryFactory.clearEOP1980HistoryLoaders();
        EOPHistoryFactory.clearEOP2000HistoryLoaders();
        TimeScalesFactory.clearTimeScales();
        TimeScalesFactory.clearUTCTAILoaders();
        GravityFieldFactory.clearPotentialCoefficientsReaders();
        GeoMagneticFieldFactory.clearModels();
        GeoMagneticFieldFactory.clearGeoMagneticModelReaders();
        SolarActivityDataFactory.clearSolarActivityDataReaders();
        DataProvidersManager.getInstance().clearProviders();
        DataProvidersManager.getInstance().clearLoadedDataNames();
        try {
            SpiceKernelManager.clearAllKernels();
        } catch (final PatriusException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets the default ZOOM configuration (always the same instance, not a new one).
     * 
     * @return default ZOOM configuration
     * @throws PatriusException
     *         if the EOP data cannot be loaded
     */
    public static FramesConfiguration getZOOMConfiguration() throws PatriusException {

        // Configurations builder
        final FramesConfigurationBuilder builder = new FramesConfigurationBuilder();

        // Tides and libration
        final TidalCorrectionModel tides = TidalCorrectionModelFactory.NO_TIDE;
        final LibrationCorrectionModel lib = LibrationCorrectionModelFactory.NO_LIBRATION;

        // Polar Motion
        final PolarMotion defaultPolarMotion = new PolarMotion(true, tides, lib, SPrimeModelFactory.SP_IERS2010);

        // Diurnal rotation
        final DiurnalRotation defaultDiurnalRotation = new DiurnalRotation(tides, lib);

        // Precession Nutation
        final PrecessionNutation precNut =
            new PrecessionNutation(false, PrecessionNutationModelFactory.PN_IERS2010_INTERPOLATED);

        builder.setDiurnalRotation(defaultDiurnalRotation);
        builder.setPolarMotion(defaultPolarMotion);
        builder.setCIRFPrecessionNutation(precNut);
        builder.setEOPHistory(EOPHistoryFactory.getEOP2000History(EOPInterpolators.LAGRANGE4));

        return builder.getConfiguration();
    }

    /**
     * Gets the default IERS2003 configuration (always the same instance, not a new one).
     * 
     * @param ignoreTides
     *        tides if tides are to be ignored, false otherwise
     * @return default IERS2003 configuration
     * @throws PatriusException
     *         if the EOP data cannot be loaded
     */
    public static FramesConfiguration getIERS2003ConfigurationWOEOP(final boolean ignoreTides) {

        // Configurations builder
        final FramesConfigurationBuilder builder = new FramesConfigurationBuilder();

        // Tides and libration
        final TidalCorrectionModel tides = ignoreTides ? TidalCorrectionModelFactory.NO_TIDE :
            TidalCorrectionModelFactory.TIDE_IERS2003_INTERPOLATED;
        final LibrationCorrectionModel lib = LibrationCorrectionModelFactory.NO_LIBRATION;

        // Polar Motion
        final PolarMotion defaultPolarMotion = new PolarMotion(false, tides, lib, SPrimeModelFactory.SP_IERS2003);

        // Diurnal rotation
        final DiurnalRotation defaultDiurnalRotation = new DiurnalRotation(tides, lib);

        // Precession Nutation
        final PrecessionNutation precNut = new PrecessionNutation(false,
            PrecessionNutationModelFactory.PN_IERS2003_INTERPOLATED);

        builder.setDiurnalRotation(defaultDiurnalRotation);
        builder.setPolarMotion(defaultPolarMotion);
        builder.setCIRFPrecessionNutation(precNut);
        builder.setEOPHistory(new NoEOP2000History());

        return builder.getConfiguration();
    }

    /**
     * Gets the default IERS2010 configuration (always the same instance, not a new one).
     * 
     * @return default IERS2010 configuration
     * @throws PatriusException
     *         if the EOP data cannot be loaded
     */
    public static FramesConfiguration getIERS2010Configuration() throws PatriusException {

        final FramesConfiguration iers2010 = FramesConfigurationFactory.getIERS2010Configuration();
        final FramesConfigurationBuilder fb = new FramesConfigurationBuilder(iers2010);
        // Replace EOP history by LAGRANGE 4 interpolation
        fb.setEOPHistory(EOPHistoryFactory.getEOP2000History(EOPInterpolators.LAGRANGE4));
        return fb.getConfiguration();
    }

    /**
     * Gets the default IERS2003 configuration (always the same instance, not a new one).
     * 
     * @param ignoreTides
     *        tides if tides are to be ignored, false otherwise
     * @return default IERS2003 configuration
     * @throws PatriusException
     *         if the EOP data cannot be loaded
     */
    public static FramesConfiguration getIERS2003Configuration(final boolean ignoreTides) throws PatriusException {

        final FramesConfiguration iers2003 = FramesConfigurationFactory.getIERS2003Configuration(ignoreTides);
        final FramesConfigurationBuilder fb = new FramesConfigurationBuilder(iers2003);
        // Replace EOP history by LAGRANGE 4 interpolation
        fb.setEOPHistory(EOPHistoryFactory.getEOP2000History(EOPInterpolators.LAGRANGE4));
        return fb.getConfiguration();
    }


    /**
     * Returns Celestlab IERS 2010 configuration.
     * Compared to IERS 2010 configuration, EOP are not used
     * @return Celestlab IERS 2010 configuration
     */
    public static FramesConfiguration getCelestlabConfiguration() {

        // Configurations builder
        final FramesConfigurationBuilder builder = new FramesConfigurationBuilder();

        // Tides and libration
        final TidalCorrectionModel tides = TidalCorrectionModelFactory.TIDE_IERS2010_INTERPOLATED;
        final LibrationCorrectionModel lib = LibrationCorrectionModelFactory.LIBRATION_IERS2010;

        // Polar Motion
        final PolarMotion defaultPolarMotion = new PolarMotion(false, tides, lib, SPrimeModelFactory.SP_IERS2010);

        // Diurnal rotation
        final DiurnalRotation defaultDiurnalRotation = new DiurnalRotation(tides, lib);

        // Precession Nutation
        final PrecessionNutation precNut =
            new PrecessionNutation(false, PrecessionNutationModelFactory.PN_IERS2010_INTERPOLATED);

        builder.setDiurnalRotation(defaultDiurnalRotation);
        builder.setPolarMotion(defaultPolarMotion);
        builder.setCIRFPrecessionNutation(precNut);
        try {
            builder.setEOPHistory(EOPHistoryFactory.getEOP2000History(EOPInterpolators.LAGRANGE4));
        } catch (final PatriusException e) {
            throw new PatriusExceptionWrapper(e);
        }

        return builder.getConfiguration();
    }
}
