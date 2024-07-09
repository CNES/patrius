/**
 * 
 * Copyright 2011-2017 CNES
 *
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
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
 * @history Created 12/07/2012
 */
package fr.cnes.sirius.patrius.forces.gravity.tides.coefficients;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.text.ParseException;

import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Reader for FES2004 formats. <br>
 * The proper way to use this it to call the {@link OceanTidesCoefficientsFactory#getCoefficientsProvider()
 * getCoefficientProvider} method. Indeed, the {@link OceanTidesCoefficientsFactory} will determine the best reader to
 * use, depending on file available in the file system.
 * 
 * @see OceanTidesCoefficientsReader
 * 
 * @concurrency not thread-safe
 * @concurrency.comment instance is mutable
 * 
 * @author Rami Houdroge
 * 
 * @version $Id: FES2004FormatReader.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 1.2
 * 
 */
public class FES2004FormatReader extends OceanTidesCoefficientsReader {

    /** Expected columns number of the FES2004 file. */
    private static final int EXPECTED_COLUMNS_NUMBER = 12;
    
    /** Index for line parsing */
    private static final int SEVEN = 7;

    /**
     * Constructor
     * 
     * @param supportedNames
     *        regular expression for supported files names
     */
    public FES2004FormatReader(final String supportedNames) {
        super(supportedNames);
    }

    /** {@inheritDoc} */
    @Override
    public void loadData(final InputStream input, final String name) throws IOException, ParseException,
                                                                    PatriusException {

        // buffer file data
        final BufferedReader r = new BufferedReader(new InputStreamReader(input, Charset.forName("UTF-8")));

        // temporary set
        OceanTidesCoefficientsSet c;

        // skip header
        r.readLine();
        r.readLine();
        r.readLine();

        for (String line = r.readLine(); line != null; line = r.readLine()) {

            // get the fields defining the current the potential terms
            final String[] tab = line.trim().split("\\s+");
            if (tab.length != EXPECTED_COLUMNS_NUMBER) {
                String loaderName = this.getClass().getName();
                loaderName = loaderName.substring(loaderName.lastIndexOf('.') + 1);
                throw new PatriusException(PatriusMessages.UNEXPECTED_FILE_FORMAT_ERROR_FOR_LOADER, name, loaderName);
            }

            final double doodson = Double.parseDouble(tab[0]);
            final int l = Integer.parseInt(tab[2]);
            final int m = Integer.parseInt(tab[3]);

            final double[] coeffs = new double[] { Double.parseDouble(tab[4]), Double.parseDouble(tab[5]),
                Double.parseDouble(tab[6]), Double.parseDouble(tab[SEVEN]), Double.parseDouble(tab[SEVEN + 1]),
                Double.parseDouble(tab[SEVEN + 2]), Double.parseDouble(tab[SEVEN + 3]),
                Double.parseDouble(tab[SEVEN + 4]) };

            // check that everything went as supposed to
            if (!this.checkArgsSanity(doodson, m, l, coeffs)) {
                String loaderName = this.getClass().getName();
                loaderName = loaderName.substring(loaderName.lastIndexOf('.') + 1);
                throw new PatriusException(PatriusMessages.UNEXPECTED_FILE_FORMAT_ERROR_FOR_LOADER, name, loaderName);
            }

            // convert to simple arrays
            c = new OceanTidesCoefficientsSet(doodson, l, m, coeffs[0], coeffs[1], coeffs[2], coeffs[3], coeffs[4],
                coeffs[5], coeffs[6], coeffs[SEVEN]);
            this.add(c);

        }

        // if the data map is empty, throw an exception
        if (this.isEmpty()) {
            throw new PatriusException(PatriusMessages.NO_SOLAR_ACTIVITY_FILE_LOADED, name);
        } else {
            this.readCompleted = true;
        }
        this.readCompleted = true;
    }

    /**
     * Check argulmetns sanity
     * 
     * @param doodson
     *        doodson number
     * @param m
     *        order
     * @param l
     *        degree
     * @param c
     *        coefficients
     * @return boolean true if everything is alright
     */
    private boolean checkArgsSanity(final double doodson, final int m, final int l, final double[] c) {
        boolean okCoeffs;
        okCoeffs = !Double.isNaN(doodson) && m >= 0 && l >= 0;

        for (final double value : c) {
            okCoeffs = okCoeffs && !Double.isNaN(value);
        }

        return okCoeffs;
    }

}
