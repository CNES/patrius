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
 * 
 * @history created 03/07/12
 */
package fr.cnes.sirius.patrius.tools.ephemeris;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScale;

/**
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id: UserEphemeris.java 17578 2017-05-10 12:20:20Z bignon $
 * 
 * @since 1.2
 * 
 */
public class UserEphemeris implements IUserEphemeris {

    /** The map containing the ephemeris. */
    private final Map<AbsoluteDate, PVCoordinates> data;

    /** The ephemeris reference frame. */
    private final Frame referenceFrame;

    /** Pattern for data lines. */
    private Pattern linePattern;

    /**
     * Constructor.
     * 
     * @param ephemerisFile
     *        the file containing the ephemeris
     * @param referenceEpoch
     *        the reference epoch
     * @param timeScale
     *        the reference time scale
     * @param frame
     *        the reference frame
     * @throws IOException
     *         input/output exception
     */
    public UserEphemeris(final String ephemerisFile, final DateComponents referenceEpoch, final TimeScale timeScale,
        final Frame frame)
        throws IOException {
        this.referenceFrame = frame;

        // sets the proper pattern to read the data file:
        this.setPattern();

        // initialises the class containing the ephemeris:
        this.data = new HashMap<AbsoluteDate, PVCoordinates>();

        // creates an InputStream from the data file:
        final InputStream isf = UserEphemeris.class.getClassLoader().getResourceAsStream(ephemerisFile);
        // set up a reader for line-oriented file
        final BufferedReader reader = new BufferedReader(new InputStreamReader(isf));

        boolean inHeader = true;
        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            boolean parsed = false;

            if (this.linePattern.matcher(line).matches()) {
                inHeader = false;
                // this is a data line, build an entry from the extracted fields
                final String[] fields = line.split(" +");
                final int days = Integer.parseInt(fields[0]);
                final double sec = Double.parseDouble(fields[1]);
                final double x = Double.parseDouble(fields[2]);
                final double y = Double.parseDouble(fields[3]);
                final double z = Double.parseDouble(fields[4]);
                final double dist = Double.parseDouble(fields[5]);
                final AbsoluteDate date = new AbsoluteDate(new DateComponents(referenceEpoch, days),
                    new TimeComponents(
                        sec), timeScale);
                // new AbsoluteDate(refrenceDate, days * 86400 + sec, timeScale);
                final Vector3D position = new Vector3D(dist, new Vector3D(x, y, z));
                final PVCoordinates pv = new PVCoordinates(position, Vector3D.ZERO);
                // adds the new line to the map:
                this.data.put(date, pv);
                parsed = true;
            }
            if (!(inHeader || parsed)) {
                throw new RuntimeException();
            }

            // check if we have read something
            if (inHeader) {
                throw new RuntimeException();
            }
        }
        reader.close();
    }

    /**
     * Sets the patterns used to read the input data file.
     */
    private void setPattern() {
        this.linePattern = Pattern.compile("^\\d+ +\\d+\\.\\d+(?: +-?\\d\\.\\d+\\w[+|-]\\d+){4}$");
    }

    @Override
    public Map<AbsoluteDate, PVCoordinates> getEphemeris() {
        return this.data;
    }

    @Override
    public Frame getReferenceFrame() {
        return this.referenceFrame;
    }
}
