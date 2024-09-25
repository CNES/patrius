/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 * HISTORY
 * VERSION:4.13:DM:DM-120:08/12/2023:[PATRIUS] Merge de la branche patrius-for-lotus dans Patrius
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11:DM:DM-3218:22/05/2023:[PATRIUS] Evolution de format des fichiers SP3
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration gestion attractions gravitationnelles
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.6:FA:FA-2504:27/01/2021:[PATRIUS] Consommation de fichiers SP3 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
/*
 * 
 */
package fr.cnes.sirius.patrius.files.sp3;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Scanner;

import fr.cnes.sirius.patrius.files.general.OrbitFile.TimeSystem;
import fr.cnes.sirius.patrius.files.general.OrbitFileParser;
import fr.cnes.sirius.patrius.files.general.SatelliteInformation;
import fr.cnes.sirius.patrius.files.general.SatelliteTimeCoordinate;
import fr.cnes.sirius.patrius.files.sp3.SP3File.SP3FileType;
import fr.cnes.sirius.patrius.files.sp3.SP3File.SP3OrbitType;
import fr.cnes.sirius.patrius.math.exception.util.DummyLocalizable;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScale;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

//CHECKSTYLE: stop MagicNumber check
//Reason: model - Orekit code

/**
 * A parser for the SP3 orbit file format. It supports the original format as
 * well as the latest SP3-c version.
 * <p>
 * <b>Note:</b> this parser is thread-safe, so calling {@link #parse} from different threads is allowed.
 * </p>
 * 
 * @see <a href="http://igscb.jpl.nasa.gov/igscb/data/format/sp3_docu.txt">SP3-a file format</a>
 * @see <a href="http://igscb.jpl.nasa.gov/igscb/data/format/sp3c.txt">SP3-c file format</a>
 * @author Thomas Neidhart
 */
@SuppressWarnings("PMD.NullAssignment")
public class SP3Parser implements OrbitFileParser {

    /** Double hash. */
    private static final String DOUBLE_HASH = "##";

    /** km to m conversion factor. */
    private static final double M_IN_KM = 1000.;
    
    /** {@inheritDoc} */
    @Override
    @SuppressWarnings({"PMD.EmptyCatchBlock", "PMD.PreserveStackTrace"})
    public SP3File parse(final String fileName) throws PatriusException {

        InputStream stream = null;

        try {
            // Parse input file
            stream = new FileInputStream(fileName);
            return this.parse(stream);
        } catch (final FileNotFoundException e) {
            throw new PatriusException(PatriusMessages.UNABLE_TO_FIND_FILE, fileName);
        } finally {
            try {
                if (stream != null) {
                    // close stream
                    stream.close();
                }
            } catch (final IOException e) {
                // ignore
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public SP3File parse(final InputStream stream) throws PatriusException {
        try {
            return parseInternal(stream);
        } catch (final IOException e) {
            throw new PatriusException(e, new DummyLocalizable(e.getMessage()));
        }
    }

    /**
     * Parses the SP3 file from the given {@link InputStream} and
     * returns a {@link SP3File} object.
     * 
     * @param stream
     *        the stream to read the SP3File from
     * @return the parsed {@link SP3File} object
     * @throws PatriusException
     *         if the file could not be parsed successfully
     * @throws IOException
     *         if an error occurs while reading from the stream
     */
    @SuppressWarnings("PMD.EmptyCatchBlock")
    private static SP3File parseInternal(final InputStream stream) throws PatriusException, IOException {

        // Initialize reader
        final BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));

        // initialize internal data structures
        final ParseInfo pi = new ParseInfo();

        String line = reader.readLine();
        int lineNumber = 1;
        try {
            // Loop on header lines
            while (line != null && line.matches("[\\#\\+/%].*")) {
                parseHeaderLine(lineNumber++, line, pi);
                line = reader.readLine();
            }

            // now handle the epoch/position/velocity entries
            while (line != null && !line.startsWith("EOF")) {
                if (line.length() > 0) {
                    parseContentLine(line, pi);
                }
                line = reader.readLine();
            }
        } finally {
            try {
                // Close reader
                reader.close();
            } catch (final IOException e1) {
                // ignore
            }
        }

        // Return result
        return pi.file;
    }

    /**
     * Parses a header line from the SP3 file (line number 1 - 22).
     * 
     * @param lineNumber
     *        the current line number
     * @param line
     *        the line as read from the SP3 file
     * @param pi
     *        the current {@link ParseInfo} object
     * @throws PatriusException
     *         if a non-supported construct is found
     */
    // CHECKSTYLE: stop MethodLength check
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Orekit code kept as such
    private static void parseHeaderLine(final int lineNumber, final String line, final ParseInfo pi)
        throws PatriusException {
        // CHECKSTYLE: resume MethodLength check
        // CHECKSTYLE: resume CyclomaticComplexity check

        final SP3File file = pi.file;
        final Scanner scanner = new Scanner(line).useDelimiter("\\s+").useLocale(Locale.US);

        // CHECKSTYLE: stop FallThrough check

        if (line.matches("\\#[a-z].*")) {
            scanner.skip("#");
            final String v = scanner.next();

            final char version = v.substring(0, 1).toLowerCase(Locale.getDefault()).charAt(0);
            if (version != 'a' && version != 'b' && version != 'c' && version != 'd') {
                throw new PatriusException(PatriusMessages.SP3_UNSUPPORTED_VERSION, version);
            }

            pi.hasVelocityEntries = "V".equals(v.substring(1, 2));

            final int year = Integer.parseInt(v.substring(2));
            final int month = scanner.nextInt();
            final int day = scanner.nextInt();
            final int hour = scanner.nextInt();
            final int minute = scanner.nextInt();
            final double second = scanner.nextDouble();

            final AbsoluteDate epoch = new AbsoluteDate(year, month, day,
                hour, minute, second,
                TimeScalesFactory.getGPS());

            file.setEpoch(epoch);

            final int numEpochs = scanner.nextInt();
            file.setNumberOfEpochs(numEpochs);

            // data used indicator
            file.setDataUsed(scanner.next());

            file.setCoordinateSystem(scanner.next());
            file.setOrbitType(SP3OrbitType.valueOf(scanner.next()));
            file.setAgency(scanner.next());

        }
        if (line.startsWith(DOUBLE_HASH)) {
            // additional date/time references in gps/julian day notation

            scanner.skip(DOUBLE_HASH);

            // gps week
            file.setGpsWeek(scanner.nextInt());
            // seconds of week
            file.setSecondsOfWeek(scanner.nextDouble());
            // epoch interval
            file.setEpochInterval(scanner.nextDouble());
            // julian day
            file.setJulianDay(scanner.nextInt());
            // day fraction
            file.setDayFraction(scanner.nextDouble());
        }
        if (line.startsWith("+ ")) {
            // line 3 contains the number of satellites
            if (lineNumber == 3) {
                pi.maxSatellites = Integer.parseInt(line.substring(3, 6).trim());
                // fall-through intended - the line contains already the first
                // entries
            }
            // the following lines contain additional satellite ids
            final int lineLength7 = line.length();
            int count = file.getSatelliteCount();
            int startIdx7 = 9;
            while (count < pi.maxSatellites && (startIdx7 + 3) <= lineLength7) {
                final String satId = line.substring(startIdx7, startIdx7 + 3).trim();
                file.addSatellite(satId);
                startIdx7 += 3;
                count++;
            }
        }
        if (line.startsWith("++")) {
            // the following lines contain general accuracy information for each satellite
            final int lineLength12 = 60;
            int satIdx = (lineNumber - 8) * 17;
            int startIdx12 = 9;
            while (satIdx < pi.maxSatellites && (startIdx12 + 3) <= lineLength12) {
                final SatelliteInformation satInfo = file.getSatellite(satIdx++);
                final int exponent = Integer.parseInt(line.substring(startIdx12, startIdx12 + 3).trim());
                // the accuracy is calculated as 2**exp (in m) -> can be safely
                // converted to an integer as there will be no fraction
                satInfo.setAccuracy((int) MathLib.pow(2d, exponent));
                startIdx12 += 3;
            }
        }
        if (line.startsWith("%c") && file.getTimeSystem() == null) {
            file.setType(getFileType(line.substring(3, 5).trim()));

            // now identify the time system in use
            final String tsStr = line.substring(9, 12).trim();
            TimeSystem ts = TimeSystem.GPS;
            if (!"ccc".equalsIgnoreCase(tsStr)) {
                ts = TimeSystem.valueOf(tsStr);
            }
            file.setTimeSystem(ts);

            switch (ts) {
                case GPS:
                    pi.timeScale = TimeScalesFactory.getGPS();
                    break;

                case GAL:
                    pi.timeScale = TimeScalesFactory.getGST();
                    break;

                case GLO:
                case QZS:
                    throw new PatriusException(PatriusMessages.SP3_UNSUPPORTED_TIMESYSTEM, ts.name());

                case TAI:
                    pi.timeScale = TimeScalesFactory.getTAI();
                    break;

                case UTC:
                    pi.timeScale = TimeScalesFactory.getUTC();
                    break;

                default:
                    pi.timeScale = TimeScalesFactory.getGPS();
                    break;
            }
        }

        // CHECKSTYLE: resume FallThrough check

    }

    /**
     * Parses a single content line as read from the SP3 file.
     * 
     * @param line
     *        a string containing the line
     * @param pi
     *        the current {@link ParseInfo} object
     */
    private static void parseContentLine(final String line, final ParseInfo pi) {
        // EP and EV lines are ignored so far

        final SP3File file = pi.file;

        switch (line.charAt(0)) {
            case '*':
                final int year = Integer.parseInt(line.substring(3, 7).trim());
                final int month = Integer.parseInt(line.substring(8, 10).trim());
                final int day = Integer.parseInt(line.substring(11, 13).trim());
                final int hour = Integer.parseInt(line.substring(14, 16).trim());
                final int minute = Integer.parseInt(line.substring(17, 19).trim());
                final double second = Double.parseDouble(line.substring(20, 31).trim());

                pi.latestEpoch = new AbsoluteDate(year, month, day,
                    hour, minute, second,
                    pi.timeScale);
        
                break;

            case 'P':
                final String satelliteId = line.substring(1, 4).trim();

                if (file.containsSatellite(satelliteId)) {
                    final double x = Double.parseDouble(line.substring(4, 18).trim());
                    final double y = Double.parseDouble(line.substring(18, 32).trim());
                    final double z = Double.parseDouble(line.substring(32, 46).trim());

                    // the position values are in km and have to be converted to m
                    pi.latestPosition = new Vector3D(x * M_IN_KM, y * M_IN_KM, z * M_IN_KM);

                    // clock (microsec)
                    pi.latestClock = Double.parseDouble(line.substring(46, 60).trim());

                    // the additional items are optional and not read yet

                    // if (line.length() >= 73) {
                    // // x-sdev (b**n mm)
                    // int xStdDevExp = Integer.valueOf(line.substring(61,
                    // 63).trim());
                    // // y-sdev (b**n mm)
                    // int yStdDevExp = Integer.valueOf(line.substring(64,
                    // 66).trim());
                    // // z-sdev (b**n mm)
                    // int zStdDevExp = Integer.valueOf(line.substring(67,
                    // 69).trim());
                    // // c-sdev (b**n psec)
                    // int cStdDevExp = Integer.valueOf(line.substring(70,
                    // 73).trim());
                    //
                    // pi.posStdDevRecord =
                    // new PositionStdDevRecord(Math.pow(pi.posVelBase, xStdDevExp),
                    // Math.pow(pi.posVelBase,
                    // yStdDevExp), Math.pow(pi.posVelBase, zStdDevExp),
                    // Math.pow(pi.clockBase, cStdDevExp));
                    //
                    // String clockEventFlag = line.substring(74, 75);
                    // String clockPredFlag = line.substring(75, 76);
                    // String maneuverFlag = line.substring(78, 79);
                    // String orbitPredFlag = line.substring(79, 80);
                    // }

                    if (!pi.hasVelocityEntries) {
                        final SatelliteTimeCoordinate coord =
                            new SatelliteTimeCoordinate(pi.latestEpoch,
                                pi.latestPosition,
                                pi.latestClock);
                        file.addSatelliteCoordinate(satelliteId, coord);
                    }
                } else {
                    pi.latestPosition = null;
                }
                break;

            case 'V':
                final String satelliteIdV = line.substring(1, 4).trim();

                if (file.containsSatellite(satelliteIdV)) {
                    final double xv = Double.parseDouble(line.substring(4, 18).trim());
                    final double yv = Double.parseDouble(line.substring(18, 32).trim());
                    final double zv = Double.parseDouble(line.substring(32, 46).trim());

                    // the velocity values are in dm/s and have to be converted to
                    // m/s
                    final Vector3D velocity = new Vector3D(xv / 10d, yv / 10d, zv / 10d);
                    
                    // the clock rate change values are in 10^(-4) microseconds/second and are not converted
                    final double clockRateChange = Double.parseDouble(line.substring(46, 60).trim());

                    // the additional items are optional and not read yet

                    // if (line.length() >= 73) {
                    // // xvel-sdev (b**n 10**-4 mm/sec)
                    // int xVstdDevExp = Integer.valueOf(line.substring(61,
                    // 63).trim());
                    // // yvel-sdev (b**n 10**-4 mm/sec)
                    // int yVstdDevExp = Integer.valueOf(line.substring(64,
                    // 66).trim());
                    // // zvel-sdev (b**n 10**-4 mm/sec)
                    // int zVstdDevExp = Integer.valueOf(line.substring(67,
                    // 69).trim());
                    // // clkrate-sdev (b**n 10**-4 psec/sec)
                    // int clkStdDevExp = Integer.valueOf(line.substring(70,
                    // 73).trim());
                    // }

                    final SatelliteTimeCoordinate coord =
                        new SatelliteTimeCoordinate(pi.latestEpoch,
                            new PVCoordinates(pi.latestPosition, velocity),
                            pi.latestClock,
                            clockRateChange);
                    file.addSatelliteCoordinate(satelliteIdV, coord);
                }
                break;

            default:
                // ignore everything else
                break;
        }
    }

    /**
     * Returns the {@link SP3FileType} that corresponds to a given string
     * in a SP3 file.
     * 
     * @param fileType
     *        file type as string
     * @return file type as enum
     */
    // CHECKSTYLE: stop NestedBlockDepth check
    // Reason: Orekit code kept as such
    private static SP3FileType getFileType(final String fileType) {
        SP3FileType type = SP3FileType.UNDEFINED;
        if ("G".equalsIgnoreCase(fileType)) {
            // Type GPS
            type = SP3FileType.GPS;
        } else if ("M".equalsIgnoreCase(fileType)) {
            // Type Mixed
            type = SP3FileType.MIXED;
        } else if ("R".equalsIgnoreCase(fileType)) {
            // Type Glonass
            type = SP3FileType.GLONASS;
        } else if ("L".equalsIgnoreCase(fileType)) {
            // Tyep Leo
            type = SP3FileType.LEO;
        } else if ("E".equalsIgnoreCase(fileType)) {
            // Type Galileo
            type = SP3FileType.GALILEO;
        } else if ("C".equalsIgnoreCase(fileType)) {
            // Type Compass
            type = SP3FileType.COMPASS;
        } else if ("J".equalsIgnoreCase(fileType)) {
            // Type Qzss
            type = SP3FileType.QZSS;
        }
        return type;
    }

    // CHECKSTYLE: resume NestedBlockDepth check

    /**
     * Transient data used for parsing a sp3 file. The data is kept in a
     * separate data structure to make the parser thread-safe.
     * <p>
     * <b>Note</b>: The class intentionally does not provide accessor methods, as it is only used internally for parsing
     * a SP3 file.
     * </p>
     */
    private static class ParseInfo {

        /** The corresponding SP3File object. */
        private final SP3File file;

        /** The latest epoch as read from the SP3 file. */
        private AbsoluteDate latestEpoch;

        /** The latest position as read from the SP3 file. */
        private Vector3D latestPosition;

        /** The latest clock value as read from the SP3 file. */
        private double latestClock;

        /** Indicates if the SP3 file has velocity entries. */
        private boolean hasVelocityEntries;

        /** The timescale used in the SP3 file. */
        private TimeScale timeScale;

        /** The number of satellites as contained in the SP3 file. */
        private int maxSatellites;

        /** The base for pos/vel. */
        // private double posVelBase;

        /** The base for clock/rate. */
        // private double clockBase;

        /** Create a new {@link ParseInfo} object. */
        protected ParseInfo() {
            this.file = new SP3File();
            this.latestEpoch = null;
            this.latestPosition = null;
            this.latestClock = 0.0d;
            this.hasVelocityEntries = false;
            this.timeScale = TimeScalesFactory.getGPS();
            this.maxSatellites = 0;
            // posVelBase = 2d;
            // clockBase = 2d;
        }
    }

    // CHECKSTYLE: resume MagicNumber check
}
