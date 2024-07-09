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
import java.util.regex.Pattern;

import fr.cnes.sirius.patrius.data.DataLoader;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScale;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * Step by step ephemeris data loader.
 * </p>
 * <p>
 * This class is a PV ephemeris loader. The ephemeris file contains 8 columns. The date must be written in the two first
 * columns: the first column is the offset with respect to the fifties reference epoch, the second column is the second
 * number within the day. The position is stored in the third, forth and fifth columns. The velocity is stored in the
 * sixth, seventh and eighth columns. The allowed characters are : space, numbers, point, minus sign, plus sign and E.
 * </p>
 * <p>
 * Step by step, this loader read one line of the file, extracts the information and saves it in two tables. The list
 * 'dates' contains the absolute dates in the specified time scale (by default, TAI scale). Use 'setTScale' to modify
 * the time scale. The list 'ephemeris' contains the PV coordinates.
 * </p>
 * 
 * @useSample final PVEphemerisLoader loader = new PVEphemerisLoader("PV_ephem.ascii");
 *            loader.loadData(input,"PV_ephem.ascii");
 * 
 * @concurrency not thread safe
 * 
 * @author Gerald Mercadier
 * 
 * @version $Id: PVEphemerisLoader.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.0
 * 
 */
public class PVEphemerisLoader implements DataLoader {

    /** Default supported files name pattern. */
    private static final String DEFAULT_SUPPORTED_NAMES = ".*\\.eph$";

    /** Default time scale. */
    private static final TimeScale DEFAULT_TSCALE = TimeScalesFactory.getTAI();

    /** Table of read ephemeris. */
    private final ArrayList<PVCoordinates> ephemeris;

    /** Table of dates. */
    private final ArrayList<AbsoluteDate> dates;

    /** Allowed characters. */
    private final Pattern ephemerisPattern = Pattern.compile("\\s*[E\\s\\d\\.\053-]+\\s*");

    /** Time scale */
    private TimeScale tScale;

    /**
     * Build an ephemeris data loader.
     * 
     * @param inSupportedName
     *        name of the file that has to be loaded
     */
    public PVEphemerisLoader(final String inSupportedName) {
        this.ephemeris = new ArrayList<PVCoordinates>();
        this.dates = new ArrayList<AbsoluteDate>();
        this.tScale = DEFAULT_TSCALE;
    }

    @Override
    public final boolean stillAcceptsData() {
        return this.ephemeris.isEmpty();
    }

    @Override
    public final void loadData(final InputStream input, final String name) throws IOException, ParseException,
                                                                          PatriusException {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        try {

            // line will contain each read line
            String line;

            // dicedLine will contain the elements of the read line
            String[] dicedLine;

            // dataLineTemp will contain the result of the parsing of dicedLine
            ArrayList<Double> dataLineTemp;

            // dataLineTable will contain every read line
            final ArrayList<ArrayList<Double>> dataLineTable = new ArrayList<ArrayList<Double>>();

            // date will contain the read date
            AbsoluteDate date;

            // position will contain the read position
            Vector3D position;

            // velocity will contain the read velocity
            Vector3D velocity;

            // data will contain the read data (one line)
            ArrayList<Double> data;

            // offset with respect to the fifties reference epoch
            int offset;

            // second number within the day
            double secondInDay;

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

            // stores the extracted data in a list (dates and ephemeris)
            for (int i = 0; i < dataLineTable.size(); i++) {
                data = dataLineTable.get(i);

                // the first data is the offset
                offset = data.get(0).intValue();
                // the second data is the number of seconds
                secondInDay = data.get(1);
                // creates the date with tScale TimeScale
                date = new AbsoluteDate(new DateComponents(DateComponents.FIFTIES_EPOCH, offset), new TimeComponents(
                    secondInDay), this.tScale);
                // creates the position vector
                position = new Vector3D(data.get(2), data.get(3), data.get(4));
                // creates the velocity vector
                velocity = new Vector3D(data.get(5), data.get(6), data.get(7));
                // stores the date in the dates ArrayList
                this.dates.add(date);
                // stores the PV coordinates in the ephemeris ArrayList
                this.ephemeris.add(new PVCoordinates(position, velocity));
            }

        } finally {
            reader.close();
        }
    }

    /**
     * Give the list of PV coordinates which have been read.
     * 
     * @return the list of PV coordinates
     */
    public final ArrayList<PVCoordinates> getEphemeris() {
        return this.ephemeris;
    }

    /**
     * Give the list of dates which have been read.
     * 
     * @return the list of dates
     */
    public final ArrayList<AbsoluteDate> getDates() {
        return this.dates;
    }

    /**
     * Set the TimeScale for AbsoluteDate management
     * 
     * @param inTScale
     *        the TimeScale to set
     */
    public final void setTScale(final TimeScale inTScale) {
        this.tScale = inTScale;
    }

}
