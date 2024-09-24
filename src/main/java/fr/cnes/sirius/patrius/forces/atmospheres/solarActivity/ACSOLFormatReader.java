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
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2445:27/05/2020:optimisation de SolarActivityReader 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:131:28/10/2013:Fixed bug in ap ref dates
 * VERSION::FA:569:02/03/2016:Correction in case of UTC shift
 * VERSION::FA:587:24/01/2017:Generic ACSOL header
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
import fr.cnes.sirius.patrius.time.DateTimeComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.time.UTCScale;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This class reads ACSOL format solar activity data
 * 
 * @concurrency not thread-safe
 * @concurrency.comment instance is mutable
 * 
 * @author Rami Houdroge
 * @version $Id: ACSOLFormatReader.java 17582 2017-05-10 12:58:16Z bignon $
 * @since 1.2
 */
public class ACSOLFormatReader extends SolarActivityDataReader {

     /** Serializable UID. */
    private static final long serialVersionUID = 6499414183689236207L;

    /** Expected columns number of the ACSOL file. */
    private static final int EXPECTED_COLUMNS_NUMBER = 11;
    
    /** Second dimension for APKP */
    private static final int APKP_DIM = 8;

    /**
     * Constructor.
     * 
     * @param supportedNames
     *        regular expression for supported files names
     */
    public ACSOLFormatReader(final String supportedNames) {
        super(supportedNames);
    }

    /** {@inheritDoc} */
    @Override
    public void loadData(final InputStream input, final String name) throws IOException, ParseException,
                                                                    PatriusException {

        // buffer file data
        final BufferedReader r = new BufferedReader(new InputStreamReader(input, Charset.forName("UTF-8")));

        // utc time scale
        final UTCScale utc = TimeScalesFactory.getUTC();

        for (String line = r.readLine(); line != null; line = r.readLine()) {

            if (line.contains("#")) {
                // Skip header
                continue;
            }

            final String[] tab = line.trim().split("\\s+");
            if (tab.length != EXPECTED_COLUMNS_NUMBER) {
                // incorrect number of elements
                String loaderName = this.getClass().getName();
                loaderName = loaderName.substring(loaderName.lastIndexOf('.') + 1);
                throw new PatriusException(PatriusMessages.UNEXPECTED_FILE_FORMAT_ERROR_FOR_LOADER, name, loaderName);
            }

            /*
             * # Fichier activite solaire - format ACSOL.act # Origine des donnees : NOAA # Colonne 1 : JJ : Jour julien
             * depuis 1950.0 (entier) # Colonne 2 : SEC : Nombre de secondes dans le jour (TUC) # Colonne 3 : F : Flux
             * observe a la date JJ+SEC # Colonne 4-11 : AP1-AP8 : AP trihoraires sur les intervalles JJ+[0h,3h], ...,
             * JJ+[21h,24h] (TUC) 5844 61200 82 2 5 2 0 2 0 3 3 5845 61200 78.9 22 9 5 5 5 6 7 9 5846 61200 78.5 15 5 2
             * 2 3 6 6 6
             */

            final int day = Integer.parseInt(tab[0]);
            final int sec = Integer.parseInt(tab[1]);
            final double f = Double.parseDouble(tab[2]);
            final Double[][] apkp = new Double[2][APKP_DIM];
            for (int i = 0; i < APKP_DIM; i++) {
                apkp[0][i] = Double.parseDouble(tab[3 + i]);
                SolarActivityToolbox.checkApSanity(apkp[0][i]);
                apkp[1][i] = SolarActivityToolbox.apToKp(apkp[0][i]);
            }

            // convert to data set
            final DateTimeComponents currentDateTimeFlux = new DateTimeComponents(new DateTimeComponents(
                DateComponents.FIFTIES_EPOCH, TimeComponents.H00), day * 86400.0 + sec);
            final AbsoluteDate dateFlux = new AbsoluteDate(currentDateTimeFlux, utc);

            final DateTimeComponents currentDateTimeAp = new DateTimeComponents(new DateTimeComponents(
                DateComponents.FIFTIES_EPOCH, TimeComponents.H00), day * 86400.0);
            final AbsoluteDate dateAp = new AbsoluteDate(currentDateTimeAp, utc);

            this.addF107(dateFlux, f);
            this.addApKp(dateAp, apkp);

        }

        // if the data map is empty, throw an exception
        if (this.isEmpty()) {
            throw new PatriusException(PatriusMessages.NO_SOLAR_ACTIVITY_FILE_LOADED, name);
        } else {
            this.readCompleted = true;
        }
    }

}
