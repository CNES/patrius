/**
 * 
 * Copyright 2011-2017 CNES
 *
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
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
 */
package fr.cnes.sirius.patrius.validation.externaltools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;

import fr.cnes.sirius.patrius.data.DataLoader;
import fr.cnes.sirius.patrius.data.DataProvidersManager;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * Step by step ephemeris data loader.
 * </p>
 * <p>
 * This class is an ephemeris data loader for files which contain a list of ephemeris at different dates (1 line per
 * ephemeris). The date must be written in the two first columns and the matrix must be spread line after line in the
 * following columns. The allowed characters are : space, numbers, point, minus sign, plus sign and E.
 * </p>
 * <p>
 * Step by step, this loader read one line of the file, extracts the information and saves it in a map.
 * </p>
 * 
 * @useSample final TransitionMatricesDataLoader loaderGCRF = new
 *            TransitionMatricesDataLoader("Matrices_ITRS-TIRS.txt");
 *            loaderTIRF.loadData();
 * 
 * @concurrency not thread safe
 * 
 * @author Philippe Pavero
 * 
 * @version $Id: TransitionMatricesDataLoader.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.0
 * 
 */
public class TransitionMatricesDataLoader implements DataLoader {

    /** Default supported files name pattern. */
    private static final String DEFAULT_SUPPORTED_NAMES = "Matrices_.*\\.txt$";

    /** Table of read ephemeris. */
    private final SortedMap<AbsoluteDate, double[][]> ephemeris;

    /** Name of the file that has to be read. */
    private final String supportedName;

    /** Reference date. */
    private final AbsoluteDate referenceDate;

    /** Allowed characters. */
    private final Pattern ephemerisPattern = Pattern.compile("\\s*[eE\\s\\d\\.\053-]+\\s*");

    /**
     * Build an ephemeris data loader.
     * 
     * @param supportedName
     *        name of the file that has to be loaded
     * @param referenceDate
     *        reference date
     */
    public TransitionMatricesDataLoader(final String supportedName, final AbsoluteDate referenceDate) {
        this.supportedName = (supportedName == null) ? DEFAULT_SUPPORTED_NAMES : supportedName;
        this.ephemeris = new TreeMap<AbsoluteDate, double[][]>();
        this.referenceDate = referenceDate;
    }

    @Override
    public boolean stillAcceptsData() {
        return this.ephemeris.isEmpty();
    }

    @Override
    public void loadData(final InputStream input, final String name)
                                                                    throws IOException, ParseException,
                                                                    PatriusException {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        try {

            // will contain each read line
            String line;

            // will contain the elements of the read line
            String[] dicedLine;

            // will contain the result of the parsing of dicedLine
            ArrayList<Double> dataLineTemp = new ArrayList<Double>();

            // will contain every read line
            final ArrayList<ArrayList<Double>> dataLineTable = new ArrayList<ArrayList<Double>>();

            // checks whether the file can be read
            while (reader.ready()) {
                // gets the current line in the buffer
                line = reader.readLine();

                // checks whether the line matches the pattern, which would mean it contains data
                if (this.ephemerisPattern.matcher(line).matches()) {
                    // suppresses the blank characters at the beginning and at the end of the line
                    line = line.trim();

                    // cuts the line to pieces along the blank characters between each number
                    dicedLine = line.split("\\s+");
                    if (dicedLine.length > 1) {
                        // parses each of the String gotten through the split method into double precision number, and
                        // put it into a temporary list
                        dataLineTemp = new ArrayList<Double>();
                        for (final String element : dicedLine) {
                            dataLineTemp.add(Double.parseDouble(element));
                        }
                        // stores the temporary list that contains the data from the current line
                        dataLineTable.add(dataLineTemp);
                    }
                }
            }

            AbsoluteDate date;
            ArrayList<Double> data;
            int lineIndex;
            int columnIndex;

            // stores the extracted data in a map
            for (int i = 0; i < dataLineTable.size(); i++) {
                final double[][] transitionMatrix = new double[6][6];
                data = dataLineTable.get(i);
                date = new AbsoluteDate(this.referenceDate, data.get(0) * 24 * 3600 + data.get(1),
                    TimeScalesFactory.getUTC());
                // date = new AbsoluteDate(referenceDate, data.get(0) * 24 * 3600 + data.get(1));
                for (int j = 2; j < data.size(); j++) {
                    lineIndex = (j - 2) / 6;
                    columnIndex = (j - 2) % 6;
                    transitionMatrix[lineIndex][columnIndex] = data.get(j);
                }
                this.ephemeris.put(date, transitionMatrix);
            }

        } finally {
            reader.close();
        }
    }

    /**
     * Load ephemeris data.
     * 
     * @throws PatriusException
     *         if some data can't be read, some
     *         file content is corrupted or no data is available
     */
    public void loadData() throws PatriusException {
        // load the data from the configured data providers
        this.ephemeris.clear();
        DataProvidersManager.getInstance().feed(this.supportedName, this);
        if (this.ephemeris.isEmpty()) {
            throw new RuntimeException("no data available");
        }
    }

    /**
     * Give the list of ephemeris which have been read.
     * 
     * @return the list of ephemeris
     */
    public SortedMap<AbsoluteDate, double[][]> getEphemeris() {
        return this.ephemeris;
    }

}
