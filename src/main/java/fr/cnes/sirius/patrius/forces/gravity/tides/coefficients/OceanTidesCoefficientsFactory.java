/**
 * 
 * Copyright 2011-2022 CNES
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
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.gravity.tides.coefficients;

import java.util.ArrayList;
import java.util.List;

import fr.cnes.sirius.patrius.data.DataProvidersManager;
import fr.cnes.sirius.patrius.forces.gravity.potential.GravityFieldFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Factory used to read ocean tides coefficients files in different formats and return an
 * {@link OceanTidesCoefficientsProvider}
 * 
 * <p>
 * Loading of ocean tides coefficients data follows PATRIUS standard way of handling data:
 * <ul>Data are automatically loaded based on provided {@link OceanTidesCoefficientsProvider}.</ul>
 * <ul>User can provide its own implementation of these loaders and provide them using 
 * {@link #addOceanTidesCoefficientsReader(OceanTidesCoefficientsReader)}.</ul>
 * <ul>By default some loaders are added (see below). Loaders are used in the defined order below.
 * These loaders will use data provided by {@link DataProvidersManager}.</ul>
 * <ul>List of loaders can be cleared and reordered using {@link #clearOceanTidesCoefficientsReaders()}.</ul>
 * </p>
 * <p>
 * By default, some loaders are added in the following order:<br/>
 * <ul>
 * <li>{@link FES2004FormatReader}: FES 2004 file format.
 * </li>
 * </ul>
 * There is no included data in PATRIUS by default. <br/>
 * Once loaded, data are stored in static variables and are used for ocean tides computation accessible through 
 * {@link #getCoefficientsProvider()} method.
 * </p>
 *
 * @see GravityFieldFactory
 * 
 * @concurrency thread-hostile
 * @concurrency.comment uses a thread-hostile DataProvidersManager
 * 
 * @author Rami Houdroge
 * 
 * @version $Id: OceanTidesCoefficientsFactory.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 1.2
 * 
 */
public final class OceanTidesCoefficientsFactory {

    /**
     * Default file name for FES2004
     */
    public static final String FES2004_FILENAME = "fes2004_gr";

    /**
     * Ocean tides coefficients READERS.
     */
    private static final List<OceanTidesCoefficientsReader> READERS = new ArrayList<>();

    /**
     * Private constructor.
     * <p>
     * This class is a utility class, it should neither have a public nor a default constructor. This private
     * constructor prevents the compiler from generating one automatically.
     * </p>
     */
    private OceanTidesCoefficientsFactory() {
    }

    /**
     * Add a reader for ocean tides
     * 
     * @param reader
     *        custom reader to add for the ocean tides coefficients
     */
    public static void addOceanTidesCoefficientsReader(final OceanTidesCoefficientsReader reader) {
        synchronized (READERS) {
            READERS.add(reader);
        }
    }

    /**
     * Add the default READERS for ocean tides coefficients
     * <p>
     * The default READERS supports FES2004 format with the default name {@link #FES2004_FILENAME}
     * 
     * @see #addOceanTidesCoefficientsReader(OceanTidesCoefficientsReader)
     * @see #clearOceanTidesCoefficientsReaders()
     */
    public static void addDefaultOceanTidesCoefficientsReaders() {
        synchronized (READERS) {
            READERS.add(new FES2004FormatReader(FES2004_FILENAME));
        }
    }

    /**
     * Clear ocean tides coefficients READERS.
     * 
     * @see #addOceanTidesCoefficientsReader(OceanTidesCoefficientsReader)
     * @see #addDefaultOceanTidesCoefficientsReaders()
     */
    public static void clearOceanTidesCoefficientsReaders() {
        synchronized (READERS) {
            READERS.clear();
        }
    }

    /**
     * Get the ocean tides coefficients provider from the first supported file.
     * <p>
     * If no {@link OceanTidesCoefficientsProvider} has been added by calling
     * {@link #addOceanTidesCoefficientsReader(OceanTidesCoefficientsReader) addOceanTidesCoefficientsReader} or if
     * {@link #clearOceanTidesCoefficientsReaders() clearOceanTidesCoefficientsReaders} has been called afterwards,the
     * {@link #addDefaultOceanTidesCoefficientsReaders() addDefaultOceanTidesCoefficientsReaders} method will be called
     * automatically.
     * </p>
     * 
     * @return an ocean tides coefficients provider containing already loaded data
     * @exception PatriusException
     *            if some data is missing or if some loader specific error occurs
     */
    public static OceanTidesCoefficientsProvider getCoefficientsProvider() throws PatriusException {

        synchronized (READERS) {

            if (READERS.isEmpty()) {
                addDefaultOceanTidesCoefficientsReaders();
            }

            // test the available READERS
            for (final OceanTidesCoefficientsReader reader : READERS) {
                DataProvidersManager.getInstance().feed(reader.getSupportedNames(), reader);
                if (!reader.stillAcceptsData()) {
                    return reader;
                }
            }
        }

        throw new PatriusException(PatriusMessages.NO_OCEAN_TIDES_COEFFICIENTS_FILES_LOADED);

    }
}
