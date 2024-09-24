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
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3144:10/05/2022:[PATRIUS] Classe TempDirectory en double 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.models.earth;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.tools.validationTool.TemporaryDirectory;

/**
 * 
 * Utils class to read and write test data result
 * 
 * 
 * @author chabaudp
 * 
 * @version $Id: ReferenceReader.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.3
 * 
 */
public class ReferenceReader {

    private static final String SEPPAT = "\\s+";

    /**
     * Method to read input from reference data file
     */
    public static List<GeoMagRefInput> readInput(final String filePath, final String fileName) throws IOException {

        Utils.setDataRoot(filePath);
        // Get file
        final InputStream is = filePath.getClass().getResourceAsStream(fileName);
        final BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line;
        String[] splittedLine;
        GeoMagRefInput refInput;
        final List<GeoMagRefInput> datasReference = new ArrayList<>();

        line = br.readLine();

        while ((line = br.readLine()) != null) {

            splittedLine = line.trim().split(SEPPAT);

            refInput = new GeoMagRefInput(Double.parseDouble(splittedLine[0]), Double.parseDouble(splittedLine[2]
                .substring(1, splittedLine[2].length())),
                Double.parseDouble(splittedLine[3]), Double.parseDouble(splittedLine[4]));

            datasReference.add(refInput);

        }

        return datasReference;

    }

    /**
     * 
     * Method to read output from reference data file
     * 
     * @param directory
     *        to find resources
     * @param name
     *        of the reference data file
     * @return
     * @throws IOException
     *         : should not happen
     * 
     * @see none
     * 
     * @since 1.3
     */
    public static List<GeoMagRefOutput>
            readOutputExternalReference(final String filePath, final String fileName)
                                                                                     throws IOException {

        Utils.setDataRoot(filePath);
        // Get file
        final InputStream is = filePath.getClass().getResourceAsStream(fileName);
        final BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line;
        String[] splittedLine;
        GeoMagRefOutput refOutput;
        final List<GeoMagRefOutput> datasReference = new ArrayList<>();

        line = br.readLine();

        while ((line = br.readLine()) != null) {

            splittedLine = line.trim().split(SEPPAT);

            refOutput = new GeoMagRefOutput(Double.parseDouble(splittedLine[10]), Double.parseDouble(splittedLine[11]),
                Double.parseDouble(splittedLine[12]),
                Double.parseDouble(splittedLine[7].substring(0, splittedLine[7].length() - 1)),
                Double.parseDouble(splittedLine[8].substring(0, splittedLine[8].length() - 1)),
                Double.parseDouble(splittedLine[5].substring(0, splittedLine[5].length() - 1)),
                Double.parseDouble(splittedLine[6].substring(0, splittedLine[6].length() - 1)),
                Double.parseDouble(splittedLine[13]), Double.parseDouble(splittedLine[9]));

            datasReference.add(refOutput);

        }

        return datasReference;

    }

    /**
     * 
     * Method to write result data for regression tests
     * 
     * @param fileName
     *        Name of the regression reference data file to write
     * @param geoMagElementList
     *        List of elements to write in the file
     * 
     * @see none
     * 
     * @since 1.3
     */
    public static void writeResultFile(final String fileName, final List<GeoMagneticElements> geoMagElementList) {

        final String GENERIC_DIRECTORY =
            TemporaryDirectory.getTemporaryDirectory("pdb.misc.results", "GeoMagValResults");

        // Output file for the results
        final String GENERIC_RESULT_FILE = GENERIC_DIRECTORY + File.separator + fileName;
        final File genDir = new File(GENERIC_DIRECTORY);
        genDir.mkdirs();

        // writer
        try {
            final BufferedWriter writer = new BufferedWriter(new FileWriter(GENERIC_RESULT_FILE));

            writer.write("X(nT) Y(nT) Z(nT) H(nT) F(nT) I(deg) D(deg)");
            writer.newLine();
            for (final GeoMagneticElements curentElement : geoMagElementList) {
                writer.write(Double.toString(curentElement.getFieldVector().getX()) + " ");
                writer.write(Double.toString(curentElement.getFieldVector().getY()) + " ");
                writer.write(Double.toString(curentElement.getFieldVector().getZ()) + " ");
                writer.write(Double.toString(curentElement.getHorizontalIntensity()) + " ");
                writer.write(Double.toString(curentElement.getTotalIntensity()) + " ");
                writer.write(Double.toString(curentElement.getInclination()) + " ");
                writer.write(Double.toString(curentElement.getDeclination()) + " ");
                writer.newLine();
            }

            writer.close();

        } catch (final IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     * 
     * Method to read the regression reference data file
     * 
     * @param filePath
     *        : resources directory where to find the regression reference data file
     * @param fileName
     *        : regression reference data file name
     * @return
     * @throws IOException
     *         : should not happen
     * 
     * @see none
     * 
     * @since 1.3
     */
    public static List<GeoMagRefOutput>
            readRegressionReference(final String filePath, final String fileName) throws IOException {

        Utils.setDataRoot(filePath);
        // Get file
        final InputStream is = filePath.getClass().getResourceAsStream(fileName);
        final BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line;
        String[] splittedLine;

        GeoMagRefOutput geoMagRegressionRef;
        final List<GeoMagRefOutput> geoMagRefList = new ArrayList<>();

        line = br.readLine();

        while ((line = br.readLine()) != null) {

            splittedLine = line.trim().split(SEPPAT);
            geoMagRegressionRef = new GeoMagRefOutput(Double.parseDouble(splittedLine[0]),
                Double.parseDouble(splittedLine[1]), Double.parseDouble(splittedLine[2]),
                Double.parseDouble(splittedLine[3]), Double.parseDouble(splittedLine[4]),
                Double.parseDouble(splittedLine[5]), Double.parseDouble(splittedLine[6]));

            geoMagRefList.add(geoMagRegressionRef);

        }

        return geoMagRefList;

    }

}
