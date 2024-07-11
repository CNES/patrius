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
 * @history creation 20/03/2017
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:849:20/03/2017:Implementation of DragCoefficientProvider with file reader
 * VERSION::FA:1176:28/11/2017:add error message
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.models;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Generic reader to read a file containing aero coefficients and store these coefficients.
 * The file must have the following format :
 * - lines starting with/containing only "#" are comments
 * - the file is a column-files : each column is separated by spaces or tabs
 * 
 * This reader is used to build an implementation of {@link DragCoefficientProvider}.
 * 
 * @concurrency thread-safe
 * 
 * @author rodriguest
 * 
 * @version $Id$
 * 
 * @since 3.4
 * 
 */
public class InterpolatedDragReader {

    /**
     * Read the aero coefficients file by parsing it.
     * Parsing is done taking into account spaces or tabulations between
     * data, lines starting with/containing only "#" are being ignored.
     * The columns are stored in a matrix double[][].
     * 
     * @param filePath
     *        file path
     * @return file data
     * @throws IOException
     *         if file could not be read
     * @throws PatriusException thrown if line format is not correct
     */
    @SuppressWarnings({ "PMD.UseStringBufferForStringAppends", "PMD.AvoidStringBufferField", "resource",
        "PMD.SimplifyStartsWith" })
    public double[][] readFile(final String filePath) throws IOException, PatriusException {
        // Use a buffered reader
        final BufferedReader reader = new BufferedReader(new InputStreamReader(
            new FileInputStream(filePath), "UTF-8"));
        String line = reader.readLine();
        final List<String> items = new ArrayList<String>();

        // Lines starting or containing "#" are comments and are discarded
        while (line != null) {
            if (!line.contains("#")) {
                items.add(line);
            }
            line = reader.readLine();
        }

        // Find number of lines
        final int nbLines = items.size();

        // Find number of columns
        final String firstLine = items.get(0);
        // Use a splitter taking into account spaces or tabulations
        final StringTokenizer splitter = new StringTokenizer(firstLine);
        final int nbColumns = splitter.countTokens();

        final double[][] data = new double[nbLines][nbColumns];

        for (int i = 0; i < nbLines; i++) {
            final String[] dataString = items.get(i).trim().split("[ |\t]+");
            if (dataString.length < nbColumns) {
                throw new PatriusException(PatriusMessages.PDB_WRONG_LINES, i + 1);
            }
            for (int j = 0; j < nbColumns; j++) {
                if (dataString[j].startsWith("\uFEFF")) {
                    // Do not take into account start character for UTF-8 files
                    dataString[j] = dataString[j].substring(1);
                }
                data[i][j] = Double.parseDouble(dataString[j]);
            }
        }

        reader.close();
        return data;
    }
}
