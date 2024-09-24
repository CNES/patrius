/**
 * 
 * Copyright 2011-2022 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * 
 * @history Created 04/07/2014
 *
 * HISTORY
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration gestion attractions gravitationnelles
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:281:04/07/2014:add factory class for earth gravitational model.
 * VERSION::DM:700:13/03/2017:Add model name
 * VERSION::DM:1174:26/06/2017:allow incomplete coefficients files
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.gravity;

import java.io.IOException;
import java.text.ParseException;

import fr.cnes.sirius.patrius.forces.gravity.potential.EGMFormatReader;
import fr.cnes.sirius.patrius.forces.gravity.potential.GRGSFormatReader;
import fr.cnes.sirius.patrius.forces.gravity.potential.GravityFieldFactory;
import fr.cnes.sirius.patrius.forces.gravity.potential.ICGEMFormatReader;
import fr.cnes.sirius.patrius.forces.gravity.potential.PotentialCoefficientsProvider;
import fr.cnes.sirius.patrius.forces.gravity.potential.PotentialCoefficientsReader;
import fr.cnes.sirius.patrius.forces.gravity.potential.SHMFormatReader;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.PatriusRuntimeException;

/**
 * <p>
 * Factory class for earth gravitational model. This factory provides earth gravitational model by giving the potential
 * file name, the degree and the order.
 * </p>
 * 
 * @useSample
 *            <p>
 *            final GravityModel model = EarthGravitationalModelFactory.getGravitationalModel(potentialFileName, n, m);
 *            </p>
 * 
 * @concurrency not thread-safe
 * @concurrency.comment the presence of static methods makes this class not thread-safe
 * 
 * @see BalminoGravityModel
 * @see CunninghamGravityModel
 * @see DrozinerGravityModel
 * @see GravityFieldFactory
 * 
 * @author Charlotte Maggiorani
 * 
 * @version $Id: EarthGravitationalModelFactory.java 17680 2017-07-07 15:00:50Z bignon $
 * 
 * @since 2.3
 * 
 */
public final class EarthGravitationalModelFactory {

    /**
     * Gravity field names enumerate.
     * 
     * @since 2.3
     */
    public static enum GravityFieldNames {
        /** ICGEM files */
        ICGEM(GravityFieldFactory.ICGEM_FILENAME),
        /** SHM files */
        SHM(GravityFieldFactory.SHM_FILENAME),
        /** EGM files */
        EGM(GravityFieldFactory.EGM_FILENAME),
        /** GRGS files */
        GRGS(GravityFieldFactory.GRGS_FILENAME);

        /** File name. */
        private String regEx;

        /**
         * Simple constructor.
         * 
         * @param regularEx
         *        regular expression of file name.
         */
        private GravityFieldNames(final String regularEx) {
            this.regEx = regularEx;
        }
    }

    /**
     * Simple constructor.
     * 
     * @since 2.3
     */
    private EarthGravitationalModelFactory() {
    }

    /**
     * Create an default instance of a gravitational field of a celestial body using Balmino model and specific data.
     * Missing coefficients are allowed.
     * 
     * @param potentialFileName
     *        the gravity field name
     * @param filename
     *        gravity data filename
     * @param n
     *        the degree
     * @param m
     *        the order
     * @return an instance of the gravitational field of a celestial body
     * @throws IOException
     *         if data can't be read
     * @throws ParseException
     *         if data can't be parsed
     * @throws PatriusException
     *         if some data is missing or if some loader specific error occurs
     * @see BalminoGravityModel
     * @since 3.4
     */
    public static GravityModel getGravitationalModel(final GravityFieldNames potentialFileName,
                                                   final String filename, final int n, final int m)
        throws IOException,
        ParseException,
        PatriusException {
        return getGravitationalModel(potentialFileName, filename, n, m, true);
    }

    /**
     * Create an default instance of a gravitational field of a celestial body using Balmino model and specific data.
     * 
     * @param potentialFileName
     *        the gravity field name
     * @param filename
     *        gravity data filename
     * @param n
     *        the degree
     * @param m
     *        the order
     * @return an instance of the gravitational field of a celestial body
     * @param missingCoefficientsAllowed
     *        if true, allows missing coefficients in the input data
     * @throws IOException
     *         if data can't be read
     * @throws ParseException
     *         if data can't be parsed
     * @throws PatriusException
     *         if some data is missing or if some loader specific error occurs
     * @see BalminoGravityModel
     * @since 3.4
     */
    public static GravityModel getGravitationalModel(final GravityFieldNames potentialFileName,
                                                   final String filename, final int n, final int m,
                                                   final boolean missingCoefficientsAllowed)
        throws IOException,
        ParseException,
        PatriusException {
        return getBalmino(potentialFileName, filename, n, m, missingCoefficientsAllowed);
    }

    /**
     * Create an instance of a central body attraction with normalized coefficients, Helmholtz Polynomials (Balmino
     * model) and specific data.
     * 
     * @param potentialFileName
     *        the gravity field name
     * @param filename
     *        gravity data filename
     * @param n
     *        the degree
     * @param m
     *        the order
     * @param missingCoefficientsAllowed
     *        if true, allows missing coefficients in the input data
     * @return an instance of the gravitational field of a celestial body
     * @throws IOException
     *         if data can't be read
     * @throws ParseException
     *         if data can't be parsed
     * @throws PatriusException
     *         if some data is missing or if some loader specific error occurs
     * @see BalminoGravityModel
     * @since 3.4
     */
    public static GravityModel getBalmino(final GravityFieldNames potentialFileName, final String filename,
                                        final int n, final int m,
                                        final boolean missingCoefficientsAllowed)
        throws IOException, ParseException,
        PatriusException {
        final PotentialCoefficientsProvider provider = getModel(potentialFileName, filename,
            missingCoefficientsAllowed);
        return new BalminoGravityModel(FramesFactory.getITRF(), provider.getAe(), provider.getMu(), provider.getC(n,
            m, true), provider.getS(n, m, true));
    }

    /**
     * Create an instance of the gravitational field of a celestial body using Cunningham model and specific data.
     * 
     * @param potentialFileName
     *        the gravity field name
     * @param filename
     *        gravity data filename
     * @param n
     *        the degree
     * @param m
     *        the order
     * @param missingCoefficientsAllowed
     *        if true, allows missing coefficients in the input data
     * @return an instance of the gravitational field of a celestial body
     * @throws IOException
     *         if data can't be read
     * @throws ParseException
     *         if data can't be parsed
     * @throws PatriusException
     *         if some data is missing or if some loader specific error occurs
     * @see CunninghamGravityModel
     * @since 3.4
     */
    public static GravityModel getCunningham(final GravityFieldNames potentialFileName, final String filename,
                                           final int n, final int m,
                                           final boolean missingCoefficientsAllowed)
        throws IOException, ParseException,
        PatriusException {
        final PotentialCoefficientsProvider provider = getModel(potentialFileName, filename,
            missingCoefficientsAllowed);
        return new CunninghamGravityModel(FramesFactory.getITRF(), provider.getAe(), provider.getMu(),
            provider.getC(n, m, false), provider.getS(n, m, false));
    }

    /**
     * Create an instance of the gravitational field of a celestial body using Droziner model and specific data.
     * 
     * @param potentialFileName
     *        the gravity field name
     * @param filename
     *        gravity data filename
     * @param n
     *        the degree
     * @param m
     *        the order
     * @param missingCoefficientsAllowed
     *        if true, allows missing coefficients in the input data
     * @return an instance of the gravitational field of a celestial body
     * @throws IOException
     *         if data can't be read
     * @throws ParseException
     *         if data can't be parsed
     * @throws PatriusException
     *         if some data is missing or if some loader specific error occurs
     * @see DrozinerGravityModel
     * @since 3.4
     */
    public static GravityModel getDroziner(final GravityFieldNames potentialFileName, final String filename,
                                         final int n, final int m,
                                         final boolean missingCoefficientsAllowed)
        throws IOException, ParseException,
        PatriusException {
        final PotentialCoefficientsProvider provider = getModel(potentialFileName, filename,
            missingCoefficientsAllowed);
        return new DrozinerGravityModel(FramesFactory.getITRF(), provider.getAe(), provider.getMu(), provider.getC(
            n, m, false), provider.getS(n, m, false));
    }

    /**
     * This class returns a factory used to read gravity field files in several supported formats.
     * 
     * @param name
     *        the gravity field name
     * @param filename
     *        the data file name
     * @param missingCoefficientsAllowed
     *        if true, allows missing coefficients in the input data
     * @return a gravity field coefficients provider
     * @throws IOException
     *         if data can't be read
     * @throws ParseException
     *         if data can't be parsed
     * @throws PatriusException
     *         if some data is missing or if some loader specific error occurs
     * 
     * @see GravityFieldFactory
     * 
     * @since 2.3
     */
    private static PotentialCoefficientsProvider getModel(final GravityFieldNames name,
                                                          final String filename,
                                                          final boolean missingCoefficientsAllowed)
        throws IOException,
        ParseException,
        PatriusException {

        // Clear gravity field READERS
        GravityFieldFactory.clearPotentialCoefficientsReaders();
        PotentialCoefficientsReader r = null;

        // Get regexp
        String regexp = name.regEx;
        if (filename != null) {
            regexp = filename;
        }

        // Build reader depending on file type
        switch (name) {
            case EGM:
                r = new EGMFormatReader(regexp, missingCoefficientsAllowed);
                break;
            case GRGS:
                r = new GRGSFormatReader(regexp, missingCoefficientsAllowed);
                break;
            case ICGEM:
                r = new ICGEMFormatReader(regexp, missingCoefficientsAllowed);
                break;
            case SHM:
                r = new SHMFormatReader(regexp, missingCoefficientsAllowed);
                break;
            default:
                // Cannot happen
                throw new PatriusRuntimeException(PatriusMessages.INTERNAL_ERROR, null);
        }

        // Add reader
        GravityFieldFactory.addPotentialCoefficientsReader(r);
        // return the gravity field coefficients provider
        return GravityFieldFactory.getPotentialProvider();
    }
}
