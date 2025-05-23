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
 * @history Created 13/07/2012
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3143:10/05/2022:[PATRIUS] Nouvelle interface OrbitEventDetector et nouvelles classes
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:569:02/03/2016:Correction in case of UTC shift
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.atmospheres.solarActivity;

import java.util.ArrayList;
import java.util.List;

import fr.cnes.sirius.patrius.data.DataProvidersManager;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Factory used to read solar activity files and return {@link SolarActivityDataProvider solar activity data providers}
 * 
 * <p>
 * Loading of Solar Activity data follows PATRIUS standard way of handling data:
 * <ul>Data are automatically loaded based on provided {@link SolarActivityDataProvider}.</ul>
 * <ul>User can provide its own implementation of these loaders and provide them using 
 * {@link #addSolarActivityDataReader(SolarActivityDataReader)}.</ul>
 * <ul>By default some loaders are added (see below). Loaders are used in the defined order below.
 * These loaders will use data provided by {@link DataProvidersManager}.</ul>
 * <ul>List of loaders can be cleared and reordered using {@link #clearSolarActivityDataReaders()}.</ul>
 * </p>
 * <p>
 * By default, some loaders are added in the following order:<br/>
 * <ul>
 * <li>{@link ACSOLFormatReader}: ACSOL file format.</li>
 * <li>{@link NOAAFormatReader}: NOAA file format.</li>
 * </ul>
 * There is no included data in PATRIUS by default. <br/>
 * Once loaded, data are stored in static variables and are used for solar activity retrieval through 
 * {@link #getSolarActivityDataProvider()} method.
 * </p>
 *
 * @concurrency thread-hostile
 * @concurrency.comment uses {@link DataProvidersManager} which is thread hostile
 * 
 * @author Rami Houdroge
 * @version $Id: SolarActivityDataFactory.java 17582 2017-05-10 12:58:16Z bignon $
 * @since 1.2
 * 
 */
public final class SolarActivityDataFactory {

    /**
     * Default file name for ACSOL
     */
    public static final String ACSOL_FILENAME = "ACSOL.act";
    /**
     * Default file name for NOAA
     */
    public static final String NOAA_FILENAME = "NOAA_ap_97-05.dat.txt";

    /**
     * Solar activity coefficients READERS.
     */
    private static final List<SolarActivityDataReader> READERS = new ArrayList<>();

    /**
     * Private constructor.
     * <p>
     * This class is a utility class, it should neither have a public nor a default constructor. This private
     * constructor prevents the compiler from generating one automatically.
     * </p>
     */
    private SolarActivityDataFactory() {
    }

    /**
     * Add a reader for solar activity
     * 
     * @param reader
     *        custom reader to add
     */
    public static void addSolarActivityDataReader(final SolarActivityDataReader reader) {
        synchronized (READERS) {
            READERS.add(reader);
        }
    }

    /**
     * Add the default READERS for solar activity
     * <p>
     * The default READERS supports ACSOL format with the default name {@link #ACSOL_FILENAME}
     * 
     * @throws PatriusException
     *         thrown if readers could not properly initialized
     * 
     * @see #addSolarActivityDataReader(SolarActivityDataReader)
     * @see #clearSolarActivityDataReaders()
     */
    public static void addDefaultSolarActivityDataReaders() throws PatriusException {
        synchronized (READERS) {
            READERS.add(new ACSOLFormatReader(ACSOL_FILENAME));
            READERS.add(new NOAAFormatReader(NOAA_FILENAME));
        }
    }

    /**
     * Clear solar activity READERS.
     * 
     * @see #addSolarActivityDataReader(SolarActivityDataReader)
     * @see #addDefaultSolarActivityDataReaders()
     */
    public static void clearSolarActivityDataReaders() {
        synchronized (READERS) {
            READERS.clear();
        }
    }

    /**
     * Get the solar activity provider from the first supported file.
     * <p>
     * If no {@link SolarActivityDataProvider} has been added by calling
     * {@link #addSolarActivityDataReader(SolarActivityDataReader) addSolarActivityDataReader} or if
     * {@link #clearSolarActivityDataReaders() clearSolarActivityDataReaders} has been called afterwards,the
     * {@link #addDefaultSolarActivityDataReaders() addDefaultSolarActivityDataReaders} method will be called
     * automatically.
     * </p>
     * 
     * @return an solar activity coefficients provider containing already loaded data
     * @exception PatriusException
     *            if some data is missing or if some loader specific error occurs
     */
    public static SolarActivityDataProvider getSolarActivityDataProvider() throws PatriusException {

        synchronized (READERS) {

            if (READERS.isEmpty()) {
                addDefaultSolarActivityDataReaders();
            }

            // test the available READERS
            for (final SolarActivityDataReader reader : READERS) {
                DataProvidersManager.getInstance().feed(reader.getSupportedNames(), reader);
                if (!reader.stillAcceptsData()) {
                    return reader;
                }
            }
        }

        throw new PatriusException(PatriusMessages.NO_SOLAR_ACTIVITY_FILE_LOADED);

    }

}
