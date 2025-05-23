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
 * @history Created 07/11/2012
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3143:10/05/2022:[PATRIUS] Nouvelle interface OrbitEventDetector et nouvelles classes
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.gravity.variations.coefficients;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import fr.cnes.sirius.patrius.data.DataProvidersManager;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Variable gravity field coefficients factory
 * 
 * <p>
 * Loading of variable gravity field data follows PATRIUS standard way of handling data:
 * <ul>Data are automatically loaded based on provided {@link VariablePotentialCoefficientsProvider}.</ul>
 * <ul>User can provide its own implementation of these loaders and provide them using 
 * {@link #addVariablePotentialCoefficientsReader(VariablePotentialCoefficientsReader)}.</ul>
 * <ul>By default some loaders are added (see below). Loaders are used in the defined order below.
 * These loaders will use data provided by {@link DataProvidersManager}.</ul>
 * <ul>List of loaders can be cleared and reordered using {@link #clearVariablePotentialCoefficientsReaders()}.</ul>
 * </p>
 * <p>
 * By default, some loaders are added in the following order:<br/>
 * <ul>
 * <li>{@link GRGSRL02FormatReader}: GRGS RL02 file format. See <a
 * href="https://grace.obs-mip.fr/variable-models-grace-lageos/mean-fields/release-02/">
 * GRGS RL02 file format</a> for more information.
 * </li>
 * </ul>
 * There is no included data in PATRIUS by default. <br/>
 * Once loaded, data are stored in static variables and are used for variable gravity field computation accessible 
 * through {@link #getVariablePotentialProvider()} method.
 * </p>
 *
 * @concurrency not thread-safe
 * @concurrency.comment because of static fields
 * 
 * @see VariablePotentialCoefficientsReader
 * 
 * @author Rami Houdroge
 * 
 * @version $Id: VariableGravityFieldFactory.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 1.3
 * 
 */
public final class VariableGravityFieldFactory {

    /** EIGEN GRGS default filename */
    public static final String GRGSRL02_FILENAME = "EIGEN-GRGS.RL02bis.MEAN-FIELD";

    /** Readers */
    private static final List<VariablePotentialCoefficientsReader> READERS = new ArrayList<>();

    /** Private constructor */
    private VariableGravityFieldFactory() {
    }

    /** Add default readers */
    public static void addDefaultVariablePotentialCoefficientsReaders() {
        synchronized (READERS) {
            READERS.add(new GRGSRL02FormatReader(GRGSRL02_FILENAME));
        }
    }

    /**
     * Add a specific reader
     * 
     * @param reader
     *        the {@link VariablePotentialCoefficientsReader} to add to the list of readers
     */
    public static void addVariablePotentialCoefficientsReader(final VariablePotentialCoefficientsReader reader) {
        synchronized (READERS) {
            READERS.add(reader);
        }
    }

    /**
     * Clear readers
     */
    public static void clearVariablePotentialCoefficientsReaders() {
        synchronized (READERS) {
            READERS.clear();
        }
    }

    /**
     * Get the variable gravity field coefficients provider from the first supported file.
     * 
     * @return a variable gravity field coefficients provider containing already loaded data
     * @exception IOException
     *            if data can't be read
     * @exception ParseException
     *            if data can't be parsed
     * @exception PatriusException
     *            if some data is missing or if some loader specific error occurs
     */
    public static VariablePotentialCoefficientsProvider getVariablePotentialProvider() throws IOException,
                                                                                      ParseException, PatriusException {
        synchronized (READERS) {

            if (READERS.isEmpty()) {
                addDefaultVariablePotentialCoefficientsReaders();
            }

            // test the available READERS
            for (final VariablePotentialCoefficientsReader reader : READERS) {
                DataProvidersManager.getInstance().feed(reader.getSupportedNames(), reader);
                if (!reader.stillAcceptsData()) {
                    return reader;
                }
            }
        }

        throw new PatriusException(PatriusMessages.NO_GRAVITY_FIELD_DATA_LOADED);
    }

}
