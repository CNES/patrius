/*
 * Copyright 2019-2020 CNES
 * Copyright 2011-2014 JOptimizer
* HISTORY
* VERSION:4.6:DM:DM-2591:27/01/2021:[PATRIUS] Intigration et validation JOptimizer
* END-HISTORY
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package fr.cnes.sirius.patrius.math.optim.joptimizer;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;


/**
 * Utility class for JOptimizer tests.
 */
public final class TestUtils {
    
    /** Message */
    static final String HASH = "#";
    
    /**
     * Private constructor
     */
    private TestUtils() {
    }

    /**
     * Load array from file
     * 
     * @param classpathFileName file path
     * @return array
     * @throws IOException if an error occurs while reading.
     */
    public static final double[] loadDoubleArrayFromFile(String classpathFileName) throws IOException {
        final String[][] mapMatrix = getAllValuesFromAFile(classpathFileName, ',');
        final double[] v = new double[mapMatrix.length];
        for (int i = 0; i < mapMatrix.length; i++) {
            v[i] = Double.parseDouble(mapMatrix[i][0]);
        }
        return v;
    }

    /**
     * Load matrix from file
     * 
     * @param classpathFileName file path
     * @param fieldSeparator separator
     * @return matrix
     * @throws IOException if an error occurs while reading.
     */
    public static final double[][] loadDoubleMatrixFromFile(String classpathFileName,
            char fieldSeparator) throws IOException {
        final String[][] mapMatrix = getAllValuesFromAFile(classpathFileName, fieldSeparator);
        final double[][] m = new double[mapMatrix.length][mapMatrix[0].length];
        for (int i = 0; i < mapMatrix.length; i++) {
            for (int j = 0; j < mapMatrix[0].length; j++) {
                m[i][j] = Double.parseDouble(mapMatrix[i][j]);
            }
        }
        return m;
    }

    /**
     * Load matrix from file
     * 
     * @param classpathFileName file path
     * @return matrix
     * @throws IOException if an error occurs while reading.
     */
    public static final double[][] loadDoubleMatrixFromFile(String classpathFileName)
            throws IOException {
        return loadDoubleMatrixFromFile(classpathFileName, ",".charAt(0));
    }

    /**
     * Get all the values from the file.
     * 
     * @param classpathFileName path of the file to read
     * @param delimiter delimiter between values in the file
     * @return all the values from the file or null if there are no more values.
     * @throws IOException if an error occurs while reading.
     */
    public static String[][] getAllValuesFromAFile(String classpathFileName, char delimiter)
            throws IOException {

        Path path = null;
        try {
            path = Paths.get(ClassLoader.getSystemResource(classpathFileName).toURI());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        final BufferedReader br = Files.newBufferedReader(path);
        final ArrayList<String[]> v = new ArrayList<String[]>();
        String line;
        while ((line = br.readLine()) != null) {
            v.add(line.split(String.valueOf(delimiter)));
        }
        if (v.size() == 0) {
            return null;
        }
        final String[][] result = new String[v.size()][];
        return v.toArray(result);
    }
}
