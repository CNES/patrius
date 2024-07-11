/**
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
 * @history creation 17/05/2013
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:92:17/05/2013:Creation
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.models.earth;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StreamTokenizer;
import java.nio.charset.Charset;
import java.text.ParseException;

import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Reader for COF file formats for geomagnetic models.
 * 
 * @see GeoMagneticModelReader
 * 
 * @concurrency not thread-safe
 * @concurrency.comment instance is mutable
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id: COFFileFormatReader.java 18070 2017-10-02 16:46:00Z bignon $
 * 
 * @since 2.1
 * 
 */
public final class COFFileFormatReader extends GeoMagneticModelReader {

    /**
     * Constructor.
     * 
     * @param supportedNames
     *        regular expression for supported files names
     */
    public COFFileFormatReader(final String supportedNames) {
        super(supportedNames);
    }

    /** {@inheritDoc} */
    @Override
    public void loadData(final InputStream input,
                         final String name) throws IOException, ParseException, PatriusException {

        // open data file and parse values
        final StreamTokenizer str = new StreamTokenizer(new InputStreamReader(input, Charset.forName("UTF-8")));

        while (true) {
            final GeoMagneticField model = this.readModel(str);
            if (model == null) {
                break;
            } else {
                this.add(model);
            }
        }
    }

    /**
     * Read the model from the given {@link StreamTokenizer}.
     * 
     * @param stream
     *        the stream to read the model from
     * @return the parsed geomagnetic field model
     * @throws IOException
     *         if an I/O error occurs
     */
    private GeoMagneticField readModel(final StreamTokenizer stream) throws IOException {

        // check whether there is another model available in the stream
        final int ttype = stream.nextToken();
        if (ttype == StreamTokenizer.TT_EOF) {
            // Direct return
            return null;
        }

        // Header data
        final String modelName = stream.sval;
        stream.nextToken();
        final double epoch = stream.nval;
        stream.nextToken();
        final int nMax = (int) stream.nval;
        stream.nextToken();
        final int nMaxSecVar = (int) stream.nval;

        // ignored
        stream.nextToken();

        stream.nextToken();
        final double startYear = stream.nval;

        stream.nextToken();
        final double endYear = stream.nval;

        // Initialize model
        final GeoMagneticField model = new GeoMagneticField(modelName, epoch, nMax, nMaxSecVar,
            startYear, endYear);

        // the rest is ignored
        stream.nextToken();
        stream.nextToken();

        // the min/max altitude values are ignored by now

        stream.nextToken();
        stream.nextToken();

        // loop to get model data from file
        boolean done = false;
        int n;
        int m;

        do {
            // Loop on lines
            stream.nextToken();
            n = (int) stream.nval;
            stream.nextToken();
            m = (int) stream.nval;

            stream.nextToken();
            final double gnm = stream.nval;
            stream.nextToken();
            final double hnm = stream.nval;
            stream.nextToken();
            final double dgnm = stream.nval;
            stream.nextToken();
            final double dhnm = stream.nval;

            model.setMainFieldCoefficients(n, m, gnm, hnm);
            if (n <= nMaxSecVar && m <= nMaxSecVar) {
                model.setSecularVariationCoefficients(n, m, dgnm, dhnm);
            }

            stream.nextToken();
            stream.nextToken();

            // Update control variable
            done = n == nMax && m == nMax;
        } while (!done);

        // Return result
        return model;
    }
}
