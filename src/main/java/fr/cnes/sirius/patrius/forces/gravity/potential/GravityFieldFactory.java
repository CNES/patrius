/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3143:10/05/2022:[PATRIUS] Nouvelle interface OrbitEventDetector et nouvelles classes
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.gravity.potential;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import fr.cnes.sirius.patrius.data.DataProvidersManager;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Factory used to read gravity field files in several supported formats.
 * 
 * <p>
 * Loading of gravity field data follows PATRIUS standard way of handling data:
 * <ul>Data are automatically loaded based on provided {@link PotentialCoefficientsProvider}.</ul>
 * <ul>User can provide its own implementation of these loaders and provide them using 
 * {@link #addPotentialCoefficientsReader(PotentialCoefficientsReader)}.</ul>
 * <ul>By default some loaders are added (see below). Loaders are used in the defined order below.
 * These loaders will use data provided by {@link DataProvidersManager}.</ul>
 * <ul>List of loaders can be cleared and reordered using {@link #clearPotentialCoefficientsReaders()}.</ul>
 * </p>
 * <p>
 * By default, some loaders are added in the following order:<br/>
 * <ul>
 * <li>{@link ICGEMFormatReader}: ICGEM file format. See <a
 * href="http://op.gfz-potsdam.de/grace/results/grav/g005_ICGEM-Format.pdf">the ICGEM-format</a> for more information
 * </li>
 * <li>{@link SHMFormatReader}: SHM file format. See <a
 * href="http://www.gfz-potsdam.de/grace/results/"> Potsdam university website</a> for more information
 * </li>
 * <li>{@link EGMFormatReader}: EGM file format.
 * </li>
 * <li>{@link GRGSFormatReader}: GRGS file format.
 * </li>
 * </ul>
 * There is no included data in PATRIUS by default. <br/>
 * Once loaded, data are stored in static variables and are used for gravity force computation accessible through 
 * {@link #getPotentialProvider()} method.
 * </p>
 *
 * @author Fabien Maussion
 * @author Pascal Parraud
 * @author Luc Maisonobe
 */
public final class GravityFieldFactory {

    /** Default regular expression for ICGEM files. */
    public static final String ICGEM_FILENAME = "^(.*\\.gfc)|(g(\\d)+_eigen[-_](\\w)+_coef)$";

    /** Default regular expression for SHM files. */
    public static final String SHM_FILENAME = "^eigen[-_](\\w)+_coef$";

    /** Default regular expression for EGM files. */
    public static final String EGM_FILENAME = "^egm\\d\\d_to\\d.*$";

    /** Default regular expression for GRGS files. */
    public static final String GRGS_FILENAME = "^grim\\d_.*$";

    /** Potential READERS. */
    private static final List<PotentialCoefficientsReader> READERS = new ArrayList<>();

    /**
     * Private constructor.
     * <p>
     * This class is a utility class, it should neither have a public nor a default constructor. This private
     * constructor prevents the compiler from generating one automatically.
     * </p>
     */
    private GravityFieldFactory() {
    }

    /**
     * Add a reader for gravity fields.
     * 
     * @param reader
     *        custom reader to add for the gravity field
     * @see #addDefaultPotentialCoefficientsReaders()
     * @see #clearPotentialCoefficientsReaders()
     */
    public static void addPotentialCoefficientsReader(final PotentialCoefficientsReader reader) {
        synchronized (READERS) {
            READERS.add(reader);
        }
    }

    /**
     * Add the default READERS for gravity fields.
     * <p>
     * The default READERS supports ICGEM, SHM, EGM and GRGS formats with the default names {@link #ICGEM_FILENAME},
     * {@link #SHM_FILENAME}, {@link #EGM_FILENAME}, {@link #GRGS_FILENAME} and don't allow missing coefficients.
     * </p>
     * 
     * @see #addPotentialCoefficientsReader(PotentialCoefficientsReader)
     * @see #clearPotentialCoefficientsReaders()
     */
    public static void addDefaultPotentialCoefficientsReaders() {
        synchronized (READERS) {
            READERS.add(new ICGEMFormatReader(ICGEM_FILENAME, false));
            READERS.add(new SHMFormatReader(SHM_FILENAME, false));
            READERS.add(new EGMFormatReader(EGM_FILENAME, false));
            READERS.add(new GRGSFormatReader(GRGS_FILENAME, false));
        }
    }

    /**
     * Clear gravity field READERS.
     * 
     * @see #addPotentialCoefficientsReader(PotentialCoefficientsReader)
     * @see #addDefaultPotentialCoefficientsReaders()
     */
    public static void clearPotentialCoefficientsReaders() {
        synchronized (READERS) {
            READERS.clear();
        }
    }

    /**
     * Get the gravity field coefficients provider from the first supported file.
     * <p>
     * If no {@link PotentialCoefficientsReader} has been added by calling
     * {@link #addPotentialCoefficientsReader(PotentialCoefficientsReader)
     * addPotentialCoefficientsReader} or if {@link #clearPotentialCoefficientsReaders()
     * clearPotentialCoefficientsReaders} has been called afterwards,the
     * {@link #addDefaultPotentialCoefficientsReaders() addDefaultPotentialCoefficientsReaders} method will be called
     * automatically.
     * </p>
     * 
     * @return a gravity field coefficients provider containing already loaded data
     * @exception IOException
     *            if data can't be read
     * @exception ParseException
     *            if data can't be parsed
     * @exception PatriusException
     *            if some data is missing
     *            or if some loader specific error occurs
     */
    public static PotentialCoefficientsProvider getPotentialProvider()
                                                                      throws IOException, ParseException,
                                                                      PatriusException {

        synchronized (READERS) {

            if (READERS.isEmpty()) {
                addDefaultPotentialCoefficientsReaders();
            }

            // test the available READERS
            for (final PotentialCoefficientsReader reader : READERS) {
                DataProvidersManager.getInstance().feed(reader.getSupportedNames(), reader);
                if (!reader.stillAcceptsData()) {
                    return reader;
                }
            }
        }

        throw new PatriusException(PatriusMessages.NO_GRAVITY_FIELD_DATA_LOADED);

    }

}
