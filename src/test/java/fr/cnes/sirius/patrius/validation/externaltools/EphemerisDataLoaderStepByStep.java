/**
 * 
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
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.validation.externaltools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;

import fr.cnes.sirius.patrius.data.DataLoader;
import fr.cnes.sirius.patrius.data.DataProvidersManager;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * Step by step ephemeris data loader.
 * </p>
 * <p>
 * This class is an ephemeris data loader for files which contain a list of ephemeris at different dates (1 line per
 * ephemeris). The date must be written in the two first columns, the position in the third, forth and fifth columns and
 * the velocity in the sixth, seventh and eighth columns. The allowed characters are : space, numbers, point, minus
 * sign, plus sign and E.
 * </p>
 * <p>
 * Step by step, this loader read one line of the file, extracts the information and converts the position velocity
 * coordinates from one frame (the initial frame of expression) to another frame. At the end, the loader has read and
 * converted the file from one frame to another one. Both frames have to be specified by the user at the beginning
 * (construction of the loader).
 * </p>
 * 
 * @useSample final EphemerisDataLoaderStepByStep loaderITRF = new EphemerisDataLoaderStepByStep("spot1-ITRF.eph",
 *            FramesFactory.getITRF2008(), FramesFactory.getGCRF(), new AbsoluteDate(DateComponents.FIFTIES_EPOCH,
 *            TimeComponents.H00, TimeScalesFactory.getTAI())); loaderITRF.loadData(); final
 *            SortedMap<AbsoluteDate,PVCoordinates> result = loaderITRF.getEphemeris();
 * 
 * @concurrency not thread safe
 * 
 * @author Julie Anton
 * 
 * @version $Id: EphemerisDataLoaderStepByStep.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.0
 * 
 */
public class EphemerisDataLoaderStepByStep implements DataLoader {

    /** Default supported files name pattern. */
    private static final String DEFAULT_SUPPORTED_NAMES = ".*\\.eph$";

    /** Table of ephemeris. */
    private final SortedMap<AbsoluteDate, PVCoordinates> ephemeris;

    /** Name of the file that has to be read. */
    private final String supportedName;

    /** Initial frame of expression. */
    private final Frame from;

    /** Final frame of expression. */
    private final Frame to;

    /** Reference date. */
    private final AbsoluteDate referenceDate;

    /** Allowed characters. */
    private final Pattern ephemerisPattern = Pattern.compile("\\s*[E\\s\\d\\.\053-]+\\s*");

    /**
     * Build an ephemeris data loader.
     * 
     * @param supportedName
     *        name of the file that has to be loaded
     * @param from
     *        initial frame of expression
     * @param to
     *        final from of expression
     * @param referenceDate
     *        reference date
     */
    public EphemerisDataLoaderStepByStep(final String supportedName, final Frame from, final Frame to,
        final AbsoluteDate referenceDate) {
        this.supportedName = (supportedName == null) ? DEFAULT_SUPPORTED_NAMES : supportedName;
        this.ephemeris = new TreeMap<>();
        this.from = from;
        this.to = to;
        this.referenceDate = referenceDate;
    }

    @Override
    public boolean stillAcceptsData() {
        return this.ephemeris.isEmpty();
    }

    @Override
    public void loadData(final InputStream input, final String name) throws IOException, ParseException,
                                                                    PatriusException {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        try {

            // will contain each read line
            String line;

            // will contain the elements of the read line
            String[] dicedLine;

            Transform transform;
            PVCoordinates pv;
            AbsoluteDate date;
            Vector3D position;
            Vector3D velocity;

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
                    if (dicedLine.length == 8) {
                        // parses each of the String gotten through the split method into double precision number

                        // extracts the date
                        date = new AbsoluteDate(this.referenceDate, Double.parseDouble(dicedLine[0]) * 24 * 3600
                            + Double.parseDouble(dicedLine[1]));
                        // extracts the position
                        position = new Vector3D(Double.parseDouble(dicedLine[2]), Double.parseDouble(dicedLine[3]),
                            Double.parseDouble(dicedLine[4]));
                        // extracts the velocity
                        velocity = new Vector3D(Double.parseDouble(dicedLine[5]), Double.parseDouble(dicedLine[6]),
                            Double.parseDouble(dicedLine[7]));
                        // compute the transformation between the initial frame and the final one
                        transform = this.from.getTransformTo(this.to, date);
                        // compute the position velocity coordinates in the new frame
                        pv = transform.transformPVCoordinates(new PVCoordinates(position, velocity));
                        // save the result in a map
                        this.ephemeris.put(date, pv);
                    }
                }
            }
        } finally {
            reader.close();
        }
    }

    /**
     * Load ephemeris data.
     * 
     * @throws PatriusException
     *         if some data can't be read, some file content is corrupted or no data is available
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
     * Give the list of ephemeris computed in the new frame.
     * 
     * @return the list of ephemeris
     */
    public SortedMap<AbsoluteDate, PVCoordinates> getEphemeris() {
        return this.ephemeris;
    }
}
