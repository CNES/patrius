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
 * @history Created 20/08/2012
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3143:10/05/2022:[PATRIUS] Nouvelle interface OrbitEventDetector et nouvelles classes
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2445:27/05/2020:optimisation de SolarActivityReader 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:569:02/03/2016:Correction in case of UTC shift
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.atmospheres.solarActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.text.ParseException;

import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.time.UTCScale;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This class reads NOAA format solar activity data
 * 
 * @concurrency not thread-safe
 * @concurrency.comment instance is mutable
 * 
 * @author Rami Houdroge
 * @version $Id: NOAAFormatReader.java 17582 2017-05-10 12:58:16Z bignon $
 * @since 1.2
 */
public class NOAAFormatReader extends SolarActivityDataReader {

     /** Serializable UID. */
    private static final long serialVersionUID = -4525639758504636121L;

    /** Constant. */
    private static final int C_1900 = 1900;
    /** Constant. */
    private static final int C_2000 = 2000;
    /** Constant. */
    private static final int C_50 = 50;
    /** Constant. */
    private static final int C_11 = 11;
    /** Expected columns number of the NOAA file. */
    private static final int EXPECTED_COLUMNS_NUMBER = 13;
    /** Second dimension for APKP */
    private static final int APKP_DIM = 8;

    /**
     * Constructor.
     * 
     * @param supportedNames
     *        regular expression for supported files names
     */
    public NOAAFormatReader(final String supportedNames) {
        super(supportedNames);
    }

    /** {@inheritDoc} */
    @Override
    public void loadData(final InputStream input, final String name) throws IOException, ParseException,
                                                                    PatriusException {

        // buffer file data
        final BufferedReader r = new BufferedReader(new InputStreamReader(input, Charset.forName("UTF-8")));

        // skip header
        r.readLine();

        // utc time scale
        final UTCScale utc = TimeScalesFactory.getUTC();

        for (String line = r.readLine(); line != null; line = r.readLine()) {

            final String[] tab = line.trim().split("\\s+");
            if (tab.length != EXPECTED_COLUMNS_NUMBER) {
                // incorrect number of elements
                String loaderName = this.getClass().getName();
                loaderName = loaderName.substring(loaderName.lastIndexOf('.') + 1);
                throw new PatriusException(PatriusMessages.UNEXPECTED_FILE_FORMAT_ERROR_FOR_LOADER, name, loaderName);
            }

            /*
             * # The table below column headings are DOY, F10, FB (background, 81-day avg),
             * 3-hr Ap (at 00-03UT, 03-06 UT, ... 21-00 UT), year, and an unused character flag.
             * 1 72 78 7 6 3 3 4 4 4 5 97 F
             */

            final int day = Integer.parseInt(tab[0]);
            final double f = Double.parseDouble(tab[1]);
            final Double[][] apkp = new Double[2][APKP_DIM];
            for (int i = 0; i < APKP_DIM; i++) {
                apkp[0][i] = Double.parseDouble(tab[3 + i]);
                SolarActivityToolbox.checkApSanity(apkp[0][i]);
                apkp[1][i] = SolarActivityToolbox.apToKp(apkp[0][i]);
            }
            int year = Integer.parseInt(tab[C_11]);
            // Note : won't work from year 2050 onwards
            year = year < C_50 ? year + C_2000 : year + C_1900;

            final AbsoluteDate date = new AbsoluteDate(new DateComponents(year, day), TimeComponents.H00,
                utc);

            // convert to data set
            this.addF107(date, f);
            this.addApKp(date, apkp);
        }

        // if the data map is empty, throw an exception
        if (this.isEmpty()) {
            throw new PatriusException(PatriusMessages.NO_SOLAR_ACTIVITY_FILE_LOADED, name);
        } else {
            this.readCompleted = true;
        }
    }

}
