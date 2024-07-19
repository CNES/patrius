/**
 * Copyright 2011-2022 CNES
 * Copyright 2011-2012 Space Applications Services
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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3143:10/05/2022:[PATRIUS] Nouvelle interface OrbitEventDetector et nouvelles classes
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:92:17/05/2013:Added list of geomagnetic readers and methods to add/remove readers
 * VERSION::FA:1465:26/04/2018:multi-thread environment optimisation
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.models.earth;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import fr.cnes.sirius.patrius.data.DataProvidersManager;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Factory for different {@link GeoMagneticField} models.
 * <p>
 * Loading of geomagnetic field data follows PATRIUS standard way of handling data:
 * <ul>Data are automatically loaded based on provided {@link GeoMagneticModelReader}.</ul>
 * <ul>User can provide its own implementation of these loaders and provide them using 
 * {@link #addGeoMagneticModelReader(FieldModel, GeoMagneticModelReader)}.</ul>
 * <ul>By default some loaders are added (see below). Loaders are used in the defined order below.
 * These loaders will use data provided by {@link DataProvidersManager}.</ul>
 * <ul>List of loaders can be cleared and reordered using {@link #clearGeoMagneticModelReaders()}.</ul>
 * </p>
 * <p>
 * By default, some loaders are added in the following order:<br/>
 * <ul>
 * <li>{@link COFFileFormatReader}: COF file format. See <a
 * href="https://www.ngdc.noaa.gov/geomag/WMM/wmmformat.shtml">COF file format</a> for more information.
 * </li>
 * </ul>
 * There is no included data in PATRIUS by default. <br/>
 * Once loaded, data are stored in static variables and are used for geomagnetic field computation (IGRF and WMM)
 * available through various getters.
 * </p>
 * 
 * @author Thomas Neidhart
 */
@SuppressWarnings({"PMD.NullAssignment", "PMD.LooseCoupling", "PMD.NonThreadSafeSingleton"})
public final class GeoMagneticFieldFactory {

    /** The currently supported geomagnetic field models. */
    public enum FieldModel {
        /** World Magnetic Model. */
        WMM,
        /** International Geomagnetic Reference Field. */
        IGRF
    }

    /** Geomagnetic field models IGRF_READERS. */
    private static final List<GeoMagneticModelReader> IGRF_READERS = new ArrayList<>();

    /** Geomagnetic field models WMM_READERS. */
    private static final List<GeoMagneticModelReader> WMM_READERS = new ArrayList<>();

    /** Pattern IGRF file name. */
    private static String igrfFileName = "^IGRF\\.COF$";

    /** Pattern WMM file name. */
    private static String wmmFileName = "^WMM\\.COF$";

    /** Loaded IGRF models. */
    private static TreeMap<Integer, GeoMagneticField> igrfModels = null;

    /** Loaded WMM models. */
    private static TreeMap<Integer, GeoMagneticField> wmmModels = null;

    /**
     * Private constructor.
     * <p>
     * This class is a utility class, it should neither have a public nor a default constructor. This private
     * constructor prevents the compiler from generating one automatically.
     * </p>
     */
    private GeoMagneticFieldFactory() {
    }

    /**
     * Add a reader for geomagnetic models.
     * 
     * @param type the field model type
     * @param reader custom reader to add for the geomagnetic models
     * @throws PatriusException when the input field model type does not exist
     */
    public static void addGeoMagneticModelReader(final FieldModel type,
                                                 final GeoMagneticModelReader reader) throws PatriusException {
        switch (type) {
            case WMM:
                synchronized (WMM_READERS) {
                    WMM_READERS.add(reader);
                }
                break;
            case IGRF:
                synchronized (IGRF_READERS) {
                    IGRF_READERS.add(reader);
                }
                break;
            default:
                throw new PatriusException(PatriusMessages.NON_EXISTENT_GEOMAGNETIC_MODEL,
                    type.name());
        }
    }

    /**
     * Add a default reader for geomagnetic models.
     * 
     * @param type the field model type
     * @throws PatriusException when the input field model type does not exist
     */
    public static void addDefaultGeoMagneticModelReader(final FieldModel type)
                                                                              throws PatriusException {
        switch (type) {
            case WMM:
                synchronized (WMM_READERS) {
                    WMM_READERS.add(new COFFileFormatReader(wmmFileName));
                }
                break;
            case IGRF:
                synchronized (IGRF_READERS) {
                    IGRF_READERS.add(new COFFileFormatReader(igrfFileName));
                }
                break;
            default:
                throw new PatriusException(PatriusMessages.NON_EXISTENT_GEOMAGNETIC_MODEL,
                    type.name());
        }
    }

    /**
     * Clear geomagnetic models readers.
     * 
     * @see #addGeoMagneticModelReader(FieldModel, GeoMagneticModelReader)
     * @see #addDefaultGeoMagneticModelReader(FieldModel)
     */
    public static void clearGeoMagneticModelReaders() {
        synchronized (IGRF_READERS) {
            IGRF_READERS.clear();
        }
        synchronized (WMM_READERS) {
            WMM_READERS.clear();
        }
    }

    /**
     * Method to reset igrf and wmm models to null.
     * 
     */
    public static void clearModels() {
        synchronized (GeoMagneticFieldFactory.class) {
            igrfModels = null;
            wmmModels = null;
        }
    }

    /**
     * Get the {@link GeoMagneticField} for the given model type and year.
     * 
     * @param type the field model type
     * @param year the year in AbsoluteDate format
     * @return a {@link GeoMagneticField} for the given year and model
     * @throws PatriusException if the models could not be loaded
     * @see GeoMagneticField#getDecimalYear(AbsoluteDate)
     */
    public static GeoMagneticField getField(final FieldModel type, final AbsoluteDate year)
                                                                                           throws PatriusException {

        switch (type) {
            case WMM:
                return getWMM(year);
            case IGRF:
                return getIGRF(year);
            default:
                throw new PatriusException(PatriusMessages.NON_EXISTENT_GEOMAGNETIC_MODEL,
                    type.name(), year.toString());
        }
    }

    /**
     * Get the {@link GeoMagneticField} for the given model type and year.
     * 
     * @param type the field model type
     * @param year the decimal year
     * @return a {@link GeoMagneticField} for the given year and model
     * @throws PatriusException if the models could not be loaded
     * @see GeoMagneticField#getDecimalYear(int, int, int)
     */
    public static GeoMagneticField getField(final FieldModel type, final double year)
                                                                                     throws PatriusException {

        switch (type) {
            case WMM:
                return getWMM(year);
            case IGRF:
                return getIGRF(year);
            default:
                throw new PatriusException(PatriusMessages.NON_EXISTENT_GEOMAGNETIC_MODEL,
                    type.name(), year);
        }
    }

    /**
     * Get the IGRF model for the given year.
     * 
     * @param year the year in AbsoluteDate format
     * @return a {@link GeoMagneticField} for the given year
     * @throws PatriusException if the IGRF models could not be loaded
     * @see GeoMagneticField#getDecimalYear(AbsoluteDate)
     */
    public static GeoMagneticField getIGRF(final AbsoluteDate year) throws PatriusException {
        /** Use of Double-Check locking because of thrown exception */
        if (igrfModels == null) {
            synchronized (GeoMagneticFieldFactory.class) {
                if (igrfModels == null) {
                    igrfModels = loadModels(FieldModel.IGRF);
                }
            }
        }
        return getModel(FieldModel.IGRF, igrfModels, GeoMagneticField.getDecimalYear(year));
    }

    /**
     * Get the IGRF model for the given year.
     * 
     * @param year the decimal year
     * @return a {@link GeoMagneticField} for the given year
     * @throws PatriusException if the IGRF models could not be loaded
     * @see GeoMagneticField#getDecimalYear(int, int, int)
     */
    public static GeoMagneticField getIGRF(final double year) throws PatriusException {
        /** Use of Double-Check locking because of thrown exception */
        if (igrfModels == null) {
            synchronized (GeoMagneticFieldFactory.class) {
                if (igrfModels == null) {
                    igrfModels = loadModels(FieldModel.IGRF);
                }
            }
        }
        return getModel(FieldModel.IGRF, igrfModels, year);
    }

    /**
     * Get the WMM model for the given year.
     * 
     * @param year the year in AbsoluteDate format
     * @return a {@link GeoMagneticField} for the given year
     * @throws PatriusException if the WMM models could not be loaded
     * @see GeoMagneticField#getDecimalYear(AbsoluteDate)
     */
    public static GeoMagneticField getWMM(final AbsoluteDate year) throws PatriusException {
        /** Use of Double-Check locking because of thrown exception */
        if (wmmModels == null) {
            synchronized (GeoMagneticFieldFactory.class) {
                if (wmmModels == null) {
                    wmmModels = loadModels(FieldModel.WMM);
                }
            }
        }
        return getModel(FieldModel.WMM, wmmModels, GeoMagneticField.getDecimalYear(year));
    }

    /**
     * Get the WMM model for the given year.
     * 
     * @param year the decimal year
     * @return a {@link GeoMagneticField} for the given year
     * @throws PatriusException if the WMM models could not be loaded
     * @see GeoMagneticField#getDecimalYear(int, int, int)
     */
    public static GeoMagneticField getWMM(final double year) throws PatriusException {
        /** Use of Double-Check locking because of thrown exception */
        if (wmmModels == null) {
            synchronized (GeoMagneticFieldFactory.class) {
                if (wmmModels == null) {
                    wmmModels = loadModels(FieldModel.WMM);
                }
            }
        }
        return getModel(FieldModel.WMM, wmmModels, year);
    }

    /**
     * Loads the geomagnetic model files from the given filename. The loaded models are inserted in
     * a {@link TreeMap} with their epoch as key in order to retrieve them in a sorted manner.
     * 
     * @param type the field model type
     * @return a {@link TreeMap} of all loaded models
     * @throws PatriusException if the models could not be loaded
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Orekit code kept as such
    private static TreeMap<Integer, GeoMagneticField> loadModels(final FieldModel type)
                                                                                       throws PatriusException {
        // CHECKSTYLE: resume CyclomaticComplexity check

        // Initialization
        TreeMap<Integer, GeoMagneticField> loadedModels = null;

        switch (type) {
            case WMM:
                // WMM
                if (WMM_READERS.isEmpty()) {
                    addDefaultGeoMagneticModelReader(type);
                }
                for (final GeoMagneticModelReader reader : WMM_READERS) {
                    DataProvidersManager.getInstance().feed(reader.getSupportedNames(), reader);
                    if (!reader.stillAcceptsData()) {
                        final Collection<GeoMagneticField> models = reader.getModels();
                        if (models != null) {
                            loadedModels = new TreeMap<>();
                            for (final GeoMagneticField model : models) {
                                // round to a precision of two digits after the comma
                                final int epoch = (int) MathLib.round(model.getEpoch() * 100d);
                                loadedModels.put(epoch, model);
                            }
                        }
                    }
                }
                break;
            case IGRF:
                // IGRF
                if (IGRF_READERS.isEmpty()) {
                    addDefaultGeoMagneticModelReader(type);
                }
                for (final GeoMagneticModelReader reader : IGRF_READERS) {
                    DataProvidersManager.getInstance().feed(reader.getSupportedNames(), reader);
                    if (!reader.stillAcceptsData()) {
                        final Collection<GeoMagneticField> models = reader.getModels();
                        if (models != null) {
                            loadedModels = new TreeMap<>();
                            for (final GeoMagneticField model : models) {
                                // round to a precision of two digits after the comma
                                final int epoch = (int) MathLib.round(model.getEpoch() * 100d);
                                loadedModels.put(epoch, model);
                            }
                        }
                    }
                }
                break;
            default:
                throw new PatriusException(PatriusMessages.NON_EXISTENT_GEOMAGNETIC_MODEL,
                    type.name());
        }

        // if no models could be loaded -> throw exception
        if (loadedModels == null || loadedModels.isEmpty()) {
            throw new PatriusException(PatriusMessages.UNABLE_TO_FIND_RESOURCE);
        }

        // Return result
        return loadedModels;
    }

    /**
     * Gets a geomagnetic field model for the given year. In case the specified year does not match
     * an existing model epoch, the resulting field is generated by either time-transforming an
     * existing model using its secular variation coefficients, or by linear interpolating two
     * existing models.
     * 
     * @param type the type of the field (e.g. WMM or IGRF)
     * @param models all loaded field models, sorted by their epoch
     * @param year the epoch of the resulting field model
     * @return a {@link GeoMagneticField} model for the given year
     * @throws PatriusException if the specified year is out of range of the available models
     */
    private static GeoMagneticField
            getModel(final FieldModel type,
                     final TreeMap<Integer, GeoMagneticField> models, final double year)
                                                                                        throws PatriusException {

        // Get epoch
        final int epoch = (int) MathLib.round(year * 100d);
        final SortedMap<Integer, GeoMagneticField> head = models.headMap(epoch + 1);

        if (head.isEmpty()) {
            // Exception
            throw new PatriusException(PatriusMessages.NON_EXISTENT_GEOMAGNETIC_MODEL, type.name(),
                year);
        }

        GeoMagneticField model = models.get(head.lastKey());
        if (model.getEpoch() < year) {
            if (model.supportsTimeTransform()) {
                model = model.transformModel(year);
            } else {
                final SortedMap<Integer, GeoMagneticField> tail = models.tailMap(epoch);
                if (tail.isEmpty()) {
                    // Exception
                    throw new PatriusException(PatriusMessages.NON_EXISTENT_GEOMAGNETIC_MODEL,
                        type.name(), year);
                }
                model = model.transformModel(models.get(tail.firstKey()), year);
            }
        }

        // Return result
        return model;
    }
}
