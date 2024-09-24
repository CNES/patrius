/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
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
 * Copyright 2010-2011 Centre National d'Études Spatiales
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3166:10/05/2022:[PATRIUS] Definir l'ICRF comme repere racine 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * END-HISTORY
 */
package fr.cnes.sirius.patrius;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Map;

import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.JPLCelestialBodyLoader;
import fr.cnes.sirius.patrius.data.DataProvider;
import fr.cnes.sirius.patrius.data.DataProvidersManager;
import fr.cnes.sirius.patrius.data.DirectoryCrawler;
import fr.cnes.sirius.patrius.forces.gravity.potential.GravityFieldFactory;
import fr.cnes.sirius.patrius.forces.gravity.tides.coefficients.OceanTidesCoefficientsFactory;
import fr.cnes.sirius.patrius.forces.gravity.variations.coefficients.VariableGravityFieldFactory;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.configuration.DiurnalRotation;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfiguration;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfigurationBuilder;
import fr.cnes.sirius.patrius.frames.configuration.PolarMotion;
import fr.cnes.sirius.patrius.frames.configuration.PrecessionNutation;
import fr.cnes.sirius.patrius.frames.configuration.eop.EOPHistory;
import fr.cnes.sirius.patrius.frames.configuration.eop.EOPHistoryFactory;
import fr.cnes.sirius.patrius.frames.configuration.eop.EOPInterpolators;
import fr.cnes.sirius.patrius.frames.configuration.eop.NoEOP2000History;
import fr.cnes.sirius.patrius.frames.configuration.libration.LibrationCorrectionModel;
import fr.cnes.sirius.patrius.frames.configuration.libration.LibrationCorrectionModelFactory;
import fr.cnes.sirius.patrius.frames.configuration.precessionnutation.PrecessionNutationModel;
import fr.cnes.sirius.patrius.frames.configuration.precessionnutation.PrecessionNutationModelFactory;
import fr.cnes.sirius.patrius.frames.configuration.sp.SPrimeModel;
import fr.cnes.sirius.patrius.frames.configuration.sp.SPrimeModelFactory;
import fr.cnes.sirius.patrius.frames.configuration.tides.TidalCorrectionModel;
import fr.cnes.sirius.patrius.frames.configuration.tides.TidalCorrectionModelFactory;
import fr.cnes.sirius.patrius.models.earth.GeoMagneticFieldFactory;
import fr.cnes.sirius.patrius.time.TimeScale;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Utility class for the configuration of Patrius.
 *
 * @author Pierre Seimandi (GMV)
 */
public final class PatriusUtils {

    /**
     * Gets file corresponding to the specified system resource.
     *
     * @param resource
     *        the system resource
     * @return the file corresponding to the specified system resource
     * @throws IllegalArgumentException
     *         if the specified resource cannot be found
     */
    public static File getSystemResource(final String resource) {
        final URL url = ClassLoader.getSystemResource(resource);
        if (url == null) {
            final String msg = String.format("Could not find resource file '%s'", resource);
            throw new IllegalArgumentException(msg);
        }
        return new File(url.getFile());
    }

    /**
     * Adds a folder to the dataset providers list.
     *
     * @param folderName
     *        the name of the dataset folder to add
     * @throws PatriusException
     *         if the folder added is not a directory
     */
    public static void addDatasetFolder(final String folderName) throws PatriusException {
        final File folder = new File(folderName);
        addDatasetFolder(folder);
    }

    /**
     * Adds a folder to the dataset providers list.
     *
     * @param folder
     *        the dataset folder to add
     * @throws PatriusException
     *         if the folder added is not a directory
     */
    public static void addDatasetFolder(final File folder) throws PatriusException {
        final DataProvider provider = new DirectoryCrawler(folder);
        DataProvidersManager.getInstance().addProvider(provider);
    }

    /**
     * Gets the simplest frame configuration.
     * <p>
     * No EOP history is used.<br>
     * No pole or nutation corrections from EOP data.<br>
     * The models used are:
     * </p>
     * <ul>
     * <li>SPrime model: {@link SPrimeModelFactory#NO_SP NO_SP};
     * <li>Tidal correction model: {@link TidalCorrectionModelFactory#NO_TIDE NO_TIDE};
     * <li>Libration correction model: {@link LibrationCorrectionModelFactory#NO_LIBRATION
     * NO_LIBRATION}.
     * <li>Precession-nutation model: {@link PrecessionNutationModelFactory#NO_PN NO_PN};
     * </ul>
     *
     * @return the frames configuration build
     */
    public static FramesConfiguration getBasicConfiguration() {
        final FramesConfigurationBuilder builder = new FramesConfigurationBuilder();
        return builder.getConfiguration();
    }

    /**
     * Gets the frames configuration used by ZOOM.
     * <p>
     * The EOP history is interpolated using a fourth order Lagrange polynomial interpolator.<br>
     * Pole corrections from EOP data are enabled, but not the nutation corrections.<br>
     * The models used are:
     * </p>
     * <ul>
     * <li>SPrime model: {@link SPrimeModelFactory#SP_IERS2010 SP_IERS2010};
     * <li>Tidal correction model: {@link TidalCorrectionModelFactory#NO_TIDE NO_TIDE};
     * <li>Libration correction model: {@link LibrationCorrectionModelFactory#NO_LIBRATION
     * NO_LIBRATION}.
     * <li>Precession-nutation model:
     * {@link PrecessionNutationModelFactory#PN_IERS2010_INTERPOLATED_NON_CONSTANT
     * PN_IERS2010_INTERPOLATED_NON_CONSTANT};
     * </ul>
     *
     * @return the frames configuration build
     * @throws PatriusException
     *         if an error occurs during the initialization of the EOP
     */
    public static FramesConfiguration getZoomConfiguration() throws PatriusException {
        return getFramesConfiguration(EOPInterpolators.LAGRANGE4, true, false,
                TidalCorrectionModelFactory.NO_TIDE, LibrationCorrectionModelFactory.NO_LIBRATION,
                SPrimeModelFactory.SP_IERS2010,
                PrecessionNutationModelFactory.PN_IERS2010_INTERPOLATED_NON_CONSTANT);
    }

    /**
     * Gets the IERS2003 frames configuration.
     * <p>
     * The EOP history is interpolated using a fourth order Lagrange polynomial interpolator.<br>
     * Pole and nutation corrections from EOP data are both disabled.<br>
     * The models used are:
     * </p>
     * <ul>
     * <li>SPrime model: {@link SPrimeModelFactory#SP_IERS2003 SP_IERS2003};
     * <li>Tidal correction model: {@link TidalCorrectionModelFactory#TIDE_IERS2003_INTERPOLATED
     * TIDE_IERS2003_INTERPOLATED};
     * <li>Libration correction model: {@link LibrationCorrectionModelFactory#NO_LIBRATION
     * NO_LIBRATION}.
     * <li>Precession-nutation model:
     * {@link PrecessionNutationModelFactory#PN_IERS2003_DIRECT_CONSTANT
     * PN_IERS2003_DIRECT_CONSTANT};
     * </ul>
     *
     * @return the frames configuration build
     * @throws PatriusException
     *         if an error occurs during the initialization of the EOP
     */
    public static FramesConfiguration getIERS2003Configuration() throws PatriusException {
        return getIERS2003Configuration(true, false);
    }

    /**
     * Gets the IERS2003 frames configuration.
     *
     * <p>
     * If enabled, the EOP history is interpolated using a fourth order Lagrange polynomial
     * interpolator.<br>
     * Pole and nutation corrections from EOP data are both disabled.<br>
     * The models used are:
     * </p>
     * <ul>
     * <li>SPrime model: {@link SPrimeModelFactory#SP_IERS2003 SP_IERS2003};
     * <li>Tidal correction model: {@link TidalCorrectionModelFactory#TIDE_IERS2003_INTERPOLATED
     * TIDE_IERS2003_INTERPOLATED} or {@link TidalCorrectionModelFactory#NO_TIDE NO_TIDE};
     * <li>Libration correction model: {@link LibrationCorrectionModelFactory#NO_LIBRATION
     * NO_LIBRATION}.
     * <li>Precession-nutation model:
     * {@link PrecessionNutationModelFactory#PN_IERS2003_DIRECT_CONSTANT
     * PN_IERS2003_DIRECT_CONSTANT};
     * </ul>
     *
     * @param useEOP
     *        whether or not to enable the EOP
     * @param ignoreTides
     *        whether the tidal corrections are to be ignored or taken into account
     * @return default IERS2003 configuration
     * @throws PatriusException
     *         if an error occurs during the initialization of the EOP
     */
    public static FramesConfiguration getIERS2003Configuration(final boolean useEOP,
            final boolean ignoreTides) throws PatriusException {
        // EOP interpolator
        final EOPInterpolators eopInterpolator;
        if (useEOP) {
            eopInterpolator = EOPInterpolators.LAGRANGE4;
        } else {
            eopInterpolator = null;
        }

        // Tidal correction model
        final TidalCorrectionModel tidalCorrectionModel;
        if (ignoreTides) {
            tidalCorrectionModel = TidalCorrectionModelFactory.NO_TIDE;
        } else {
            tidalCorrectionModel = TidalCorrectionModelFactory.TIDE_IERS2003_INTERPOLATED;
        }

        return getFramesConfiguration(eopInterpolator, false, false, tidalCorrectionModel,
                LibrationCorrectionModelFactory.NO_LIBRATION, SPrimeModelFactory.SP_IERS2003,
                PrecessionNutationModelFactory.PN_IERS2003_INTERPOLATED_CONSTANT);
    }

    /**
     * Gets the IERS2010 frames configuration.
     * <p>
     * The EOP history is interpolated using a fourth order Lagrange polynomial interpolator.<br>
     * Pole and nutation corrections from EOP data are both enabled.<br>
     * The models used are:
     * </p>
     * <ul>
     * <li>SPrime model: {@link SPrimeModelFactory#SP_IERS2010 SP_IERS2010};
     * <li>Tidal correction model: {@link TidalCorrectionModelFactory#TIDE_IERS2010_INTERPOLATED
     * TIDE_IERS2010_INTERPOLATED};
     * <li>Libration correction model: {@link LibrationCorrectionModelFactory#LIBRATION_IERS2010
     * LIBRATION_IERS2010}.
     * <li>Precession-nutation model:
     * {@link PrecessionNutationModelFactory#PN_IERS2010_INTERPOLATED_NON_CONSTANT
     * PN_IERS2010_INTERPOLATED_NON_CONSTANT};
     * </ul>
     *
     * @return the frames configuration build
     * @throws PatriusException
     *         if an error occurs during the initialization of the EOP
     */
    public static FramesConfiguration getIERS2010Configuration() throws PatriusException {
        return getFramesConfiguration(EOPInterpolators.LAGRANGE4, true, true,
                TidalCorrectionModelFactory.TIDE_IERS2010_INTERPOLATED,
                LibrationCorrectionModelFactory.LIBRATION_IERS2010, SPrimeModelFactory.SP_IERS2010,
                PrecessionNutationModelFactory.PN_IERS2010_INTERPOLATED_NON_CONSTANT);
    }

    /**
     * Gets the IERS2010 frames configuration used by Celestlab.
     * <p>
     * The EOP history is interpolated using a fourth order Lagrange polynomial interpolator.<br>
     * Pole and nutation corrections from EOP data are both disabled (unlike the standard IERS 2010
     * frames configuration).<br>
     * The models used are:
     * </p>
     * <ul>
     * <li>SPrime model: {@link SPrimeModelFactory#SP_IERS2010 SP_IERS2010};
     * <li>Tidal correction model: {@link TidalCorrectionModelFactory#TIDE_IERS2010_INTERPOLATED
     * TIDE_IERS2010_INTERPOLATED};
     * <li>Libration correction model: {@link LibrationCorrectionModelFactory#LIBRATION_IERS2010
     * LIBRATION_IERS2010}.
     * <li>Precession-nutation model:
     * {@link PrecessionNutationModelFactory#PN_IERS2010_INTERPOLATED_NON_CONSTANT
     * PN_IERS2010_INTERPOLATED_NON_CONSTANT};
     * </ul>
     *
     * @return the frames configuration build
     * @throws PatriusException
     *         if an error occurs during the initialization of the EOP
     */
    public static FramesConfiguration getCelestlabConfiguration() throws PatriusException {
        return getFramesConfiguration(EOPInterpolators.LAGRANGE4, false, false,
                TidalCorrectionModelFactory.TIDE_IERS2010_INTERPOLATED,
                LibrationCorrectionModelFactory.LIBRATION_IERS2010, SPrimeModelFactory.SP_IERS2010,
                PrecessionNutationModelFactory.PN_IERS2010_INTERPOLATED_NON_CONSTANT);
    }

    /**
     * Gets the frames configuration used by STELA
     * <p>
     * No EOP history is used.<br>
     * No pole or nutation corrections from EOP data.<br>
     * The models used are:
     * </p>
     * <ul>
     * <li>SPrime model: {@link SPrimeModelFactory#NO_SP NO_SP};
     * <li>Tidal correction model: {@link TidalCorrectionModelFactory#NO_TIDE NO_TIDE};
     * <li>Libration correction model: {@link LibrationCorrectionModelFactory#NO_LIBRATION
     * NO_LIBRATION}.
     * <li>Precession-nutation model: {@link PrecessionNutationModelFactory#PN_STELA PN_STELA};
     * </ul>
     *
     * @return the frames configuration build
     * @throws PatriusException
     *         if an error occurs during the initialization of the EOP
     */
    public static FramesConfiguration getStelaConfiguration() throws PatriusException {
        return getFramesConfiguration(null, false, false, TidalCorrectionModelFactory.NO_TIDE,
                LibrationCorrectionModelFactory.NO_LIBRATION, SPrimeModelFactory.NO_SP,
                PrecessionNutationModelFactory.PN_STELA);
    }

    /**
     * Builds a new frames configuration using the specified parameters.
     * <p>
     * If the EOP interpolator is set to {@code null}, no EOP history will be used.
     * </p>
     *
     * @param eopInterpolator
     *        the EOP interpolator
     * @param eopPoleCorrections
     *        whether or not the pole corrections from EOP data are to be used
     * @param eopNutationCorrections
     *        whether or not nutation corrections from EOP data are to be used
     * @param tidalCorrectionModel
     *        the tidal correction model
     * @param librationCorrectionModel
     *        the libration correction model
     * @param sprimeModel
     *        the S-prime model
     * @param precessionNutationModel
     *        the precession-nutation model
     * @return the frame configuration build
     * @throws PatriusException
     *         if an error occurs during the EOP initialization
     */
    public static FramesConfiguration getFramesConfiguration(
            final EOPInterpolators eopInterpolator, final boolean eopPoleCorrections,
            final boolean eopNutationCorrections, final TidalCorrectionModel tidalCorrectionModel,
            final LibrationCorrectionModel librationCorrectionModel, final SPrimeModel sprimeModel,
            final PrecessionNutationModel precessionNutationModel) throws PatriusException {
        // EOP history
        final EOPHistory eop;
        if (eopInterpolator == null) {
            eop = new NoEOP2000History();
        } else {
            eop = EOPHistoryFactory.getEOP2000History(eopInterpolator);
        }

        // Polar motion
        final PolarMotion polarMotion = new PolarMotion(eopPoleCorrections, tidalCorrectionModel,
                librationCorrectionModel, sprimeModel);

        // Diurnal rotation
        final DiurnalRotation diurnalRotation = new DiurnalRotation(tidalCorrectionModel,
                librationCorrectionModel);

        // Precession nutation
        final PrecessionNutation precessionNutation = new PrecessionNutation(
                eopNutationCorrections, precessionNutationModel);

        // Frames configuration builder
        final FramesConfigurationBuilder builder = new FramesConfigurationBuilder();
        builder.setDiurnalRotation(diurnalRotation);
        builder.setPolarMotion(polarMotion);
        builder.setPrecessionNutation(precessionNutation);
        builder.setEOPHistory(eop);

        // Return the frames configuration build
        return builder.getConfiguration();
    }

    /**
     * Clears all the Patrius data providers.
     */
    public static void clearDataProviders() {
        final DataProvidersManager manager = DataProvidersManager.getInstance();

        // Clear the set of data file names that have been loaded and all the data providers .
        manager.clearLoadedDataNames();
        manager.clearProviders();

        // Clear the loaders for the celestial bodies
        clearCelestialBodiesConfiguration();

        // Clear the EOP history loaders
        clearEOPHistory();

        // Clear the time scales loaders
        clearTimeScalesConfiguration();

        // Clears the frame configuration
        clearFramesConfiguration();

        // Clear the loaders for the tides, gravity field and geo-magnetic field
        OceanTidesCoefficientsFactory.clearOceanTidesCoefficientsReaders();
        VariableGravityFieldFactory.clearVariablePotentialCoefficientsReaders();
        GravityFieldFactory.clearPotentialCoefficientsReaders();
        GeoMagneticFieldFactory.clearGeoMagneticModelReaders();
        GeoMagneticFieldFactory.clearModels();

        // Clear JPL ephemerides
        clearJPLEphemeridesConstants();
    }

    /**
     * Clears the current frames configuration.
     */
    public static void clearFramesConfiguration() {
        FramesFactory.setConfiguration(null);
    }

    /**
     * Clears the current data providers for the EOP history.
     */
    public static void clearEOPHistory() {
        EOPHistoryFactory.clearEOP1980HistoryLoaders();
        EOPHistoryFactory.clearEOP2000HistoryLoaders();
    }

    /**
     * Clears the current data providers for the time scales.
     */
    public static void clearTimeScalesConfiguration() {
        clearFactoryMaps(TimeScalesFactory.class);
        clearFactory(TimeScalesFactory.class, TimeScale.class);
        TimeScalesFactory.clearUTCTAILoaders();
    }

    /**
     * Clears the current data providers for the celestial bodies.
     */
    public static void clearCelestialBodiesConfiguration() {
        clearFactoryMaps(CelestialBodyFactory.class);
        CelestialBodyFactory.clearCelestialBodyLoaders();
    }

    /**
     * Clears any static map declared in the provided class.
     *
     * @param factoryClass
     *        the class which will have its static maps cleared
     */
    private static void clearFactoryMaps(final Class<?> factoryClass) {
        try {
            for (final Field field : factoryClass.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())
                        && Map.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    ((Map<?, ?>) field.get(null)).clear();
                }
            }
        } catch (final IllegalAccessException iae) {
            throw new RuntimeException(iae);
        }
    }

    /**
     * Sets to null any static fields of a class if their type matches a specific class.
     *
     * @param factoryClass
     *        the class which will have its static fields reseted
     * @param cachedFieldsClass
     *        the class to match for a static field to be reseted
     */
    private static void clearFactory(final Class<?> factoryClass, final Class<?> cachedFieldsClass) {
        try {
            for (final Field field : factoryClass.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())
                        && cachedFieldsClass.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    field.set(null, null);
                }
            }
        } catch (final IllegalAccessException iae) {
            throw new RuntimeException(iae);
        }
    }

    /**
     * Clears all the JPL ephemerides whose name is "CONSTANTS".
     */
    private static void clearJPLEphemeridesConstants() {
        try {
            for (final Field field : JPLCelestialBodyLoader.class.getDeclaredFields()) {
                if (field.getName().equals("CONSTANTS")) {
                    field.setAccessible(true);
                    ((Map<?, ?>) field.get(null)).clear();
                }
            }
        } catch (final IllegalAccessException iae) {
            throw new RuntimeException(iae);
        }
    }

    /**
     * Gets the EOP interpolator corresponding to the provided label.
     *
     * @param label
     *        the label
     * @return the corresponding interpolator, or {@code null} if the provided label is {@code null}
     *         or is "NO_EOP"
     */
    public static EOPInterpolators getEOPInterpolator(final String label) {
        EOPInterpolators result = null;

        if (label != null && !"NO_EOP".equals(label)) {
            result = EOPInterpolators.valueOf(label);
        }

        return result;
    }

    /**
     * Gets the SPrime model corresponding to the provided label.
     *
     * @param label
     *        the label
     * @return the corresponding model, or {@code null} if the provided label is {@code null}
     */
    public static SPrimeModel getSPrimeModel(final String label) {
        SPrimeModel result = null;

        if (label != null) {
            switch (label) {
                case "NO_SP":
                    result = SPrimeModelFactory.NO_SP;
                    break;
                case "IERS2003":
                    result = SPrimeModelFactory.SP_IERS2003;
                    break;
                case "IERS2010":
                    result = SPrimeModelFactory.SP_IERS2010;
                    break;
                default:
                    final String msg = String.format("Unsupported model (%s)", label);
                    throw new IllegalArgumentException(msg);
            }
        }

        return result;
    }

    /**
     * Gets the tidal correction model corresponding to the provided label.
     *
     * @param label
     *        the label
     * @return the corresponding model, or {@code null} if the provided label is {@code null}
     */
    public static TidalCorrectionModel getTidalCorrectionModel(final String label) {
        TidalCorrectionModel result = null;

        if (label != null) {
            switch (label) {
                case "NO_TIDE":
                    result = TidalCorrectionModelFactory.NO_TIDE;
                    break;
                case "IERS2003_INTERPOLATED":
                    result = TidalCorrectionModelFactory.TIDE_IERS2003_INTERPOLATED;
                    break;
                case "IERS2003_DIRECT":
                    result = TidalCorrectionModelFactory.TIDE_IERS2003_DIRECT;
                    break;
                case "IERS2010_INTERPOLATED":
                    result = TidalCorrectionModelFactory.TIDE_IERS2010_INTERPOLATED;
                    break;
                case "IERS2010_DIRECT":
                    result = TidalCorrectionModelFactory.TIDE_IERS2010_DIRECT;
                    break;
                default:
                    final String msg = String.format("Unsupported model (%s)", label);
                    throw new IllegalArgumentException(msg);
            }
        }

        return result;
    }

    /**
     * Gets the libration correction model corresponding to the provided label.
     *
     * @param label
     *        the label
     * @return the corresponding model, or {@code null} if the provided label is {@code null}
     */
    public static LibrationCorrectionModel getLibrationCorrectionModel(final String label) {
        LibrationCorrectionModel result = null;

        if (label != null) {
            switch (label) {
                case "NO_LIBRATION":
                    result = LibrationCorrectionModelFactory.NO_LIBRATION;
                    break;
                case "IERS2010":
                    result = LibrationCorrectionModelFactory.LIBRATION_IERS2010;
                    break;
                default:
                    final String msg = String.format("Unsupported model (%s)", label);
                    throw new IllegalArgumentException(msg);
            }
        }

        return result;
    }

    /**
     * Gets the precession-nutation model corresponding to the provided label.
     *
     * @param label
     *        the label
     * @return the corresponding model, or {@code null} if the provided label is {@code null}
     */
    public static PrecessionNutationModel getPrecessionNutationModel(final String label) {
        PrecessionNutationModel result = null;

        if (label != null) {
            switch (label) {
                case "NO_PN":
                    result = PrecessionNutationModelFactory.NO_PN;
                    break;
                case "IERS2003_DIRECT_CONSTANT":
                    result = PrecessionNutationModelFactory.PN_IERS2003_DIRECT_CONSTANT;
                    break;
                case "IERS2003_DIRECT_NON_CONSTANT":
                    result = PrecessionNutationModelFactory.PN_IERS2003_DIRECT_NON_CONSTANT;
                    break;
                case "IERS2003_INTERPOLATED_CONSTANT":
                    result = PrecessionNutationModelFactory.PN_IERS2003_INTERPOLATED_CONSTANT;
                    break;
                case "IERS2003_INTERPOLATED_NON_CONSTANT":
                    result = PrecessionNutationModelFactory.PN_IERS2003_INTERPOLATED_NON_CONSTANT;
                    break;
                case "IERS2010_DIRECT_CONSTANT":
                    result = PrecessionNutationModelFactory.PN_IERS2010_DIRECT_CONSTANT;
                    break;
                case "IERS2010_DIRECT_NON_CONSTANT":
                    result = PrecessionNutationModelFactory.PN_IERS2010_DIRECT_NON_CONSTANT;
                    break;
                case "IERS2010_INTERPOLATED_CONSTANT":
                    result = PrecessionNutationModelFactory.PN_IERS2010_INTERPOLATED_CONSTANT;
                    break;
                case "IERS2010_INTERPOLATED_NON_CONSTANT":
                    result = PrecessionNutationModelFactory.PN_IERS2010_INTERPOLATED_NON_CONSTANT;
                    break;
                case "STELA":
                    result = PrecessionNutationModelFactory.PN_STELA;
                    break;
                default:
                    final String msg = String.format("Unsupported model (%s)", label);
                    throw new IllegalArgumentException(msg);
            }
        }

        return result;
    }

    /**
     * Utility class.<br>
     * This private constructor avoids the creation of new instances.
     */
    private PatriusUtils() {
    }
}
