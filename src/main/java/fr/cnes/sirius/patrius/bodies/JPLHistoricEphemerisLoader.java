/**
 * Copyright 2011-2022 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * HISTORY
 * VERSION:4.11.1:DM:DM-49:30/06/2023:[PATRIUS] Extraction arbre des reperes SPICE et link avec CelestialBodyFactory
 * VERSION:4.10.1:FA:FA-3267:02/12/2022:[PATRIUS] Anomalie dans gestion acceleration null du PVCoordinates (suite)
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.10:FA:FA-3202:03/11/2022:[PATRIUS] Renommage dans UserCelestialBody
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import fr.cnes.sirius.patrius.data.DataLoader;
import fr.cnes.sirius.patrius.data.DataProvidersManager;
import fr.cnes.sirius.patrius.frames.CelestialBodyFrame;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScale;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.time.TimeStampedCache;
import fr.cnes.sirius.patrius.time.TimeStampedGenerator;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.PatriusConfiguration;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.TimeStampedCacheException;

/**
 * Loader for JPL ephemerides binary files (DE 4xx) and similar formats (INPOP 06/08/10).
 * It loads only the {@link CelestialBodyEphemeris}. For {@link CelestialBody} loader,
 * see dedicated class.
 * <p>
 * JPL ephemerides binary files contain ephemerides for all solar system planets.
 * </p>
 * <p>
 * The JPL ephemerides binary files are recognized thanks to their base names, which must match the pattern
 * <code>[lu]nx[mp]####.ddd</code> (or <code>[lu]nx[mp]####.ddd.gz</code> for gzip-compressed files) where # stands for
 * a digit character and where ddd is an ephemeris type (typically 405 or 406).
 * </p>
 * <p>
 * Currently accepted JPL formats are: DE200/DE 202/DE 403/DE 405/DE 406/DE 410/DE 413/DE 414/DE 418/DE 421/DE 422/DE
 * 423 and DE 430.
 * </p>
 * <p>
 * The loader supports files encoded in big-endian as well as in little-endian notation. Usually, big-endian files are
 * named <code>unx[mp]####.ddd</code>, while little-endian files are named <code>lnx[mp]####.ddd</code>.
 * </p>
 * <p>
 * The IMCCE ephemerides binary files are recognized thanks to their base names, which must match the pattern
 * <code>inpop*.dat</code> (or <code>inpop*.dat.gz</code> for gzip-compressed files) where * stands for any string.
 * </p>
 * <p>
 * Currently accepted IMCCE formats are: INPOP 06b/06c/08a/10a/10b/10e/13c/17a/19a.
 * </p>
 * <p>
 * The loader supports files encoded in big-endian as well as in little-endian notation. Usually, big-endian files
 * contain <code>bigendian</code> in their names, while little-endian files contain <code>littleendian</code> in their
 * names.
 * </p>
 * <p>
 * The loader supports files in TDB or TCB time scales.
 * </p>
 * <p>
 * Note: the time scale isn't serialized.
 * </p>
 *
 * @author Emmanuel Bignon
 *
 * @since 4.10
 */
public class JPLHistoricEphemerisLoader implements JPLEphemerisLoader {
    
    /** Default supported files name pattern for JPL DE files. */
    public static final String DEFAULT_DE_SUPPORTED_NAMES = "^[lu]nx[mp](\\d\\d\\d\\d)\\.(?:4\\d\\d)$";

    /** Default supported files name pattern for IMCCE INPOP files. */
    public static final String DEFAULT_INPOP_SUPPORTED_NAMES = "^inpop.*\\.dat$";

    /** Serializable UID. */
    private static final long serialVersionUID = -8215287568823783794L;

    /** 50 days. */
    private static final double FIFTY_DAYS = 50;

    /** DE number used by INPOP files. */
    private static final int INPOP_DE_NUMBER = 100;

    /** Maximal number of constants in headers. */
    private static final int CONSTANTS_MAX_NUMBER = 400;

    /** Offset of the ephemeris type in first header record. */
    private static final int HEADER_EPHEMERIS_TYPE_OFFSET = 2840;

    /** Offset of the record size (for INPOP files) in first header record. */
    private static final int HEADER_RECORD_SIZE_OFFSET = 2856;

    /** Offset of the start epoch in first header record. */
    private static final int HEADER_START_EPOCH_OFFSET = 2652;

    /** Offset of the end epoch in first header record. */
    private static final int HEADER_END_EPOCH_OFFSET = 2660;

    /** Offset of the astronomical unit in first header record. */
    private static final int HEADER_ASTRONOMICAL_UNIT_OFFSET = 2680;

    /** Offset of the Earth-Moon mass ratio in first header record. */
    private static final int HEADER_EM_RATIO_OFFSET = 2688;

    /** Offset of Chebishev coefficients indices in first header record. */
    private static final int HEADER_CHEBISHEV_INDICES_OFFSET = 2696;

    /** Offset of libration coefficients indices in first header record. */
    private static final int HEADER_LIBRATION_INDICES_OFFSET = 2844;

    /** Offset of chunks duration in first header record. */
    private static final int HEADER_CHUNK_DURATION_OFFSET = 2668;

    /** Offset of the constants names in first header record. */
    private static final int HEADER_CONSTANTS_NAMES_OFFSET = 252;

    /** Offset of the constants values in second header record. */
    private static final int HEADER_CONSTANTS_VALUES_OFFSET = 0;

    /** Offset of the range start in the data records. */
    private static final int DATA_START_RANGE_OFFSET = 0;

    /** Offset of the range end in the data records. */
    private static final int DATE_END_RANGE_OFFSET = 8;

    /** The constant name for the astronomical unit. */
    private static final String CONSTANT_AU = "AU";

    /** The constant name for the earth-moon mass ratio. */
    private static final String CONSTANT_EMRAT = "EMRAT";

    /** 1 byte (long). */
    private static final long BYTE_LONG = 0xffL;

    /** 1 byte (int). */
    private static final int BYTE_INT = 0xff;

    /** 4 bytes. */
    private static final long BYTE4 = 0xffffffffL;

    /** Km to m ratio. */
    private static final double KM_TO_M = 1000.;

    /** 56. */
    private static final int C1 = 56;

    /** 48. */
    private static final int C2 = 48;

    /** 40. */
    private static final int C3 = 40;

    /** 32. */
    private static final int C4 = 32;

    /** 24. */
    private static final int C5 = 24;

    /** 16. */
    private static final int C6 = 16;

    /** 7. */
    private static final int C_7 = 7;

    /** 8. */
    private static final int C_8 = 8;

    /** 9. */
    private static final int C_9 = 9;

    /** 10. */
    private static final int C_10 = 10;

    /** AU threshold. */
    private static final double THRESHOLD_AU = 0.001;

    /** Mass-ratio threshold. */
    private static final double THRESHOLD_MASS_RATIO = 1E-8;

    /** Min AU. */
    private static final double MIN_AU = 1.4e11;

    /** Max AU. */
    private static final double MAX_AU = 1.6e11;

    /** Min Earth-Moon ratio. */
    private static final double MIN_EM_RATIO = 80;

    /** Max Earth-Moon ratio. */
    private static final double MAX_EM_RATIO = 82;

    /** Number of Chebychev coefficients. */
    private static final int CHEBYCHEV_NUMBER = 12;

    /** Half integer range. */
    private static final int HALF_INTEGER_RANGE = 15;

    /** Max time span. */
    private static final int MAX_TIMESPAN = 100;

    /** Cache size in seconds. */
    private static double cacheSize = FIFTY_DAYS * Constants.JULIAN_DAY;

    /** Regular expression for supported files names. */
    private final String supportedNames;

    /** Ephemeris for selected body. */
    private transient ThreadLocal<TimeStampedCache<PosVelChebyshev>> ephemerides;

    /** Constants defined in the file. */
    private transient ThreadLocal<AtomicReference<Map<String, Double>>> constants;

    /** Ephemeris type to generate. */
    private final EphemerisType generateType;

    /** Ephemeris type to load. */
    private final EphemerisType loadType;

    /** Chunks duration (in seconds). */
    private double maxChunksDuration;

    /** Time scale of the date coordinates. */
    private transient TimeScale timeScale;

    /** Indicator for binary file endianness. */
    private boolean bigEndian;

    /**
     * Create a loader for JPL ephemerides binary files.
     *
     * @param supportedNamesIn
     *        regular expression for supported files names
     * @param generateTypeIn
     *        ephemeris type to generate
     * @exception PatriusException
     *            if the header constants cannot be read
     */
    public JPLHistoricEphemerisLoader(final String supportedNamesIn,
            final EphemerisType generateTypeIn) throws PatriusException {
        supportedNames = supportedNamesIn;
        constants = new ThreadLocal<AtomicReference<Map<String, Double>>>(){
            /** {@inheritDoc} */
            @Override
            protected AtomicReference<Map<String, Double>> initialValue() {
                return new AtomicReference<>();
            }
        };

        maxChunksDuration = Double.NaN;
        generateType = generateTypeIn;
        if (generateTypeIn == EphemerisType.SOLAR_SYSTEM_BARYCENTER) {
            loadType = EphemerisType.EARTH_MOON;
        } else if (generateTypeIn == EphemerisType.EARTH_MOON) {
            loadType = EphemerisType.MOON;
        } else {
            loadType = generateTypeIn;
        }

        ephemerides = new EphemerisPerThread().getThreadLocal();
    }

    /**
     * Load celestial body ephemeris.
     *
     * @param name
     *        name of the celestial body
     * @return loaded celestial body
     * @throws PatriusException
     *         if the body cannot be loaded
     */
    @Override
    public CelestialBodyEphemeris loadCelestialBodyEphemeris(final String name) throws PatriusException {

        // Initialization
        final CelestialBodyEphemeris res;

        switch (generateType) {
            case SOLAR_SYSTEM_BARYCENTER:
                final CelestialBodyFrame icrf = CelestialBodyFactory.getBody(CelestialBodyFactory.EARTH_MOON).getICRF();
                res = new JPLCelestialBodyEphemeris(name, -1.0, icrf);
                break;
            case EARTH_MOON:
                res = new JPLCelestialBodyEphemeris(name, 1.0 / (1.0 + getLoadedEarthMoonMassRatio()),
                        FramesFactory.getGCRF());
                break;
            case EARTH:
                res = new EarthEphemeris();
                break;
            case MOON:
                res = new JPLCelestialBodyEphemeris(name, 1.0, FramesFactory.getGCRF());
                break;
            default:
                // General case
                res = new JPLCelestialBodyEphemeris(name, 1.0, FramesFactory.getICRF());
                break;
        }

        // Return result
        return res;
    }

    /**
     * Get astronomical unit.
     *
     * @return astronomical unit in meters
     * @exception PatriusException
     *            if constants cannot be loaded
     */
    public double getLoadedAstronomicalUnit() throws PatriusException {
        return KM_TO_M * getLoadedConstant(CONSTANT_AU);
    }

    /**
     * Get Earth/Moon mass ratio.
     *
     * @return Earth/Moon mass ratio
     * @exception PatriusException
     *            if constants cannot be loaded
     */
    public double getLoadedEarthMoonMassRatio() throws PatriusException {
        return getLoadedConstant(CONSTANT_EMRAT);
    }

    /**
     * Get the gravitational coefficient of a body.
     *
     * @param body
     *        body for which the gravitational coefficient is requested
     * @return gravitational coefficient in m<sup>3</sup>/s<sup>2</sup>
     * @exception PatriusException
     *            if constants cannot be loaded
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Orekit code kept as such
    public double getLoadedGravitationalCoefficient(final EphemerisType body)
        throws PatriusException {
        // CHECKSTYLE: resume ReturnCount check
        // CHECKSTYLE: resume CyclomaticComplexity check

        // coefficient in au<sup>3</sup>/day<sup>2</sup>
        final double rawGM;
        switch (body) {
            case SOLAR_SYSTEM_BARYCENTER:
                return getLoadedGravitationalCoefficient(EphemerisType.SUN) +
                        getLoadedGravitationalCoefficient(EphemerisType.MERCURY) +
                        getLoadedGravitationalCoefficient(EphemerisType.VENUS) +
                        getLoadedGravitationalCoefficient(EphemerisType.EARTH_MOON) +
                        getLoadedGravitationalCoefficient(EphemerisType.MARS) +
                        getLoadedGravitationalCoefficient(EphemerisType.JUPITER) +
                        getLoadedGravitationalCoefficient(EphemerisType.SATURN) +
                        getLoadedGravitationalCoefficient(EphemerisType.URANUS) +
                        getLoadedGravitationalCoefficient(EphemerisType.NEPTUNE) +
                        getLoadedGravitationalCoefficient(EphemerisType.PLUTO);
            case SUN:
                rawGM = getLoadedConstant("GMS", "GM_Sun");
                break;
            case MERCURY:
                rawGM = getLoadedConstant("GM1", "GM_Mer");
                break;
            case VENUS:
                rawGM = getLoadedConstant("GM2", "GM_Ven");
                break;
            case EARTH_MOON:
                rawGM = getLoadedConstant("GMB", "GM_EMB");
                break;
            case EARTH:
                // Deduced from E/M mass ratio
                return getLoadedEarthMoonMassRatio() *
                        getLoadedGravitationalCoefficient(EphemerisType.MOON);
            case MOON:
                // Deduced from E/M mass ratio
                return getLoadedGravitationalCoefficient(EphemerisType.EARTH_MOON) /
                        (1.0 + getLoadedEarthMoonMassRatio());
            case MARS:
                rawGM = getLoadedConstant("GM4", "GM_Mar");
                break;
            case JUPITER:
                rawGM = getLoadedConstant("GM5", "GM_Jup");
                break;
            case SATURN:
                rawGM = getLoadedConstant("GM6", "GM_Sat");
                break;
            case URANUS:
                rawGM = getLoadedConstant("GM7", "GM_Ura");
                break;
            case NEPTUNE:
                rawGM = getLoadedConstant("GM8", "GM_Nep");
                break;
            case PLUTO:
                rawGM = getLoadedConstant("GM9", "GM_Plu");
                break;
            default:
                // SHould not happen
                throw PatriusException.createInternalError(null);
        }

        // Compute final coefficient
        final double au = getLoadedAstronomicalUnit();
        return rawGM * au * au * au / (Constants.JULIAN_DAY * Constants.JULIAN_DAY);

    }

    /**
     * Get a constant defined in the ephemerides headers.
     * <p>
     * Note that since constants are defined in the JPL headers files, they are available as soon as one file is
     * available, even if it doesn't match the desired central date. This is because the header must be parsed before
     * the dates can be checked.
     * </p>
     * <p>
     * There are alternate names for constants since for example JPL names are different from INPOP names (Sun gravity:
     * GMS or GM_Sun, Mercury gravity: GM4 or GM_Mar...).
     * </p>
     *
     * @param names
     *        alternate names of the constant
     * @return value of the constant of NaN if the constant is not defined
     * @exception PatriusException
     *            if constants cannot be loaded
     */
    public double getLoadedConstant(final String... names) throws PatriusException {

        // lazy loading of constants
        Map<String, Double> map = constants.get().get();
        if (map == null) {
            final ConstantsParser parser = new ConstantsParser();
            if (!DataProvidersManager.getInstance().feed(supportedNames, parser)) {
                throw new PatriusException(PatriusMessages.NO_JPL_EPHEMERIDES_BINARY_FILES_FOUND);
            }
            map = parser.getConstants();
            constants.get().compareAndSet(null, map);
        }
        // loop on the names
        for (final String name : names) {
            // test if the map owns the name
            if (map.containsKey(name)) {
                // return the double value with the associated key
                return map.get(name).doubleValue();
            }
        }

        return Double.NaN;
    }

    /**
     * Get the maximal chunks duration.
     *
     * @return chunks maximal duration in seconds
     */
    public double getMaxChunksDuration() {
        return maxChunksDuration;
    }

    /**
     * Read first header record.
     *
     * @param input
     *        input stream
     * @param name
     *        name of the file (or zip entry)
     * @return record record where to put bytes
     * @exception PatriusException
     *            if the stream does not contain a JPL ephemeris
     * @exception IOException
     *            if a read error occurs
     */
    private byte[] readFirstRecord(final InputStream input, final String name) throws PatriusException, IOException {

        // read first part of record, up to the ephemeris type
        final byte[] firstPart = new byte[HEADER_RECORD_SIZE_OFFSET + 4];
        if (!readInRecord(input, firstPart, 0)) {
            throw new PatriusException(PatriusMessages.UNABLE_TO_READ_JPL_HEADER, name);
        }

        // detect the endian format
        detectEndianess(firstPart);

        // get the ephemerides type
        final int deNum = extractInt(firstPart, HEADER_EPHEMERIS_TYPE_OFFSET);

        // the record size for this file
        int recordSize = 0;

        if (deNum == INPOP_DE_NUMBER) {
            // INPOP files have an extended DE format, which includes also the record size
            recordSize = extractInt(firstPart, HEADER_RECORD_SIZE_OFFSET) << 3;
        } else {
            // compute the record size for original JPL files
            recordSize = computeRecordSize(firstPart, name);
        }

        if (recordSize <= 0) {
            throw new PatriusException(PatriusMessages.UNABLE_TO_READ_JPL_HEADER, name);
        }

        // build a record with the proper size and finish read of the first complete record
        final int start = firstPart.length;
        final byte[] record = new byte[recordSize];
        System.arraycopy(firstPart, 0, record, 0, firstPart.length);
        if (!readInRecord(input, record, start)) {
            throw new PatriusException(PatriusMessages.UNABLE_TO_READ_JPL_HEADER, name);
        }

        return record;
    }

    /**
     * Parse constants from first two header records.
     *
     * @param first
     *        first header record
     * @param second
     *        second header record
     * @param name
     *        name of the file (or zip entry)
     * @return map of parsed constants
     */
    private Map<String, Double> parseConstants(final byte[] first, final byte[] second, final String name) {

        final Map<String, Double> map = new ConcurrentHashMap<>();

        for (int i = 0; i < CONSTANTS_MAX_NUMBER; ++i) {
            // Note: for extracting the strings from the binary file, it makes no difference
            // if the file is stored in big-endian or little-endian notation
            final String constantName = extractString(first,
                    HEADER_CONSTANTS_NAMES_OFFSET + i * 6, 6);
            if (constantName.length() == 0) {
                // no more constants to read
                break;
            }
            final double constantValue = extractDouble(second, HEADER_CONSTANTS_VALUES_OFFSET + 8 * i);
            map.put(constantName, constantValue);
        }

        // INPOP files do not have constants for AU and EMRAT, thus extract them from
        // the header record and create a constant for them to be consistent with JPL files
        if (!map.containsKey(CONSTANT_AU)) {
            map.put(CONSTANT_AU, extractDouble(first, HEADER_ASTRONOMICAL_UNIT_OFFSET));
        }

        if (!map.containsKey(CONSTANT_EMRAT)) {
            map.put(CONSTANT_EMRAT, extractDouble(first, HEADER_EM_RATIO_OFFSET));
        }

        return map;
    }

    /**
     * Read bytes into the current record array.
     *
     * @param input
     *        input stream
     * @param record
     *        record where to put bytes
     * @param start
     *        start index where to put bytes
     * @return true if record has been filled up
     * @exception IOException
     *            if a read error occurs
     */
    private static boolean readInRecord(final InputStream input, final byte[] record,
            final int start) throws IOException {
        int index = start;
        while (index != record.length) {
            final int n = input.read(record, index, record.length - index);
            if (n < 0) {
                return false;
            }
            index += n;
        }
        return true;
    }

    /**
     * Detect whether the JPL ephemerides file is stored in big-endian or
     * little-endian notation.
     *
     * @param record
     *        the array containing the binary JPL header
     */
    private void detectEndianess(final byte[] record) {

        // default to big-endian
        bigEndian = true;

        // first try to read the DE number in big-endian format
        // the number is stored as unsigned int, so we have to convert it properly
        final long deNum = extractInt(record, HEADER_EPHEMERIS_TYPE_OFFSET) & BYTE4;

        // simple heuristic: if the read value is larger than half the range of an integer
        // assume the file is in little-endian format
        if (deNum > (1 << HALF_INTEGER_RANGE)) {
            bigEndian = false;
        }
    }

    /**
     * Calculate the record size of a JPL ephemerides file.
     *
     * @param record
     *        the byte array containing the header record
     * @param name
     *        the name of the data file
     * @return the record size for this file
     * @throws PatriusException
     *         if the file contains unexpected data
     */
    private int computeRecordSize(final byte[] record, final String name) throws PatriusException {

        int recordSize = 0;
        boolean ok = true;
        // JPL files always have 3 position components
        final int nComp = 3;

        // iterate over the coefficient ptr array and sum up the record size
        // the coeffPtr array has the dimensions [12][nComp]
        for (int j = 0; j < CHEBYCHEV_NUMBER; j++) {
            final int nCompCur = (j == CHEBYCHEV_NUMBER - 1) ? 2 : nComp;

            // Note: the array element coeffPtr[j][0] is not needed for the calculation
            final int idx = HEADER_CHEBISHEV_INDICES_OFFSET + j * nComp * 4;
            final int coeffPtr1 = extractInt(record, idx + 4);
            final int coeffPtr2 = extractInt(record, idx + 8);

            // sanity checks
            ok = ok && (coeffPtr1 >= 0 || coeffPtr2 >= 0);

            recordSize += coeffPtr1 * coeffPtr2 * nCompCur;
        }

        // the libration ptr array has the dimension [3]
        // Note: the array element libratPtr[0] is not needed for the calculation
        final int libratPtr1 = extractInt(record, HEADER_LIBRATION_INDICES_OFFSET + 4);
        final int libratPtr2 = extractInt(record, HEADER_LIBRATION_INDICES_OFFSET + 8);

        // sanity checks
        ok = ok && (libratPtr1 >= 0 || libratPtr2 >= 0);

        recordSize += libratPtr1 * libratPtr2 * nComp + 2;
        recordSize <<= 3;

        if (!ok || recordSize <= 0) {
            // Wrong file type
            throw new PatriusException(PatriusMessages.NOT_A_JPL_EPHEMERIDES_BINARY_FILE, name);
        }

        // Return result
        return recordSize;
    }

    /**
     * Extract a date from a record.
     *
     * @param record
     *        record to parse
     * @param offset
     *        offset of the double within the record
     * @return extracted date
     */
    private AbsoluteDate extractDate(final byte[] record, final int offset) {

        final double t = extractDouble(record, offset);
        int jDay = (int) MathLib.floor(t);
        double seconds = (t + 1. / 2. - jDay) * Constants.JULIAN_DAY;
        if (seconds >= Constants.JULIAN_DAY) {
            ++jDay;
            seconds -= Constants.JULIAN_DAY;
        }
        return new AbsoluteDate(new DateComponents(DateComponents.JULIAN_EPOCH, jDay),
            new TimeComponents(seconds), timeScale);
    }

    /**
     * Extract a double from a record.
     * <p>
     * Double numbers are stored according to IEEE 754 standard, with most significant byte first.
     * </p>
     *
     * @param record
     *        record to parse
     * @param offset
     *        offset of the double within the record
     * @return extracted double
     */
    private double extractDouble(final byte[] record, final int offset) {
        // Initialisation
        // Get all bytes
        final long l8 = (record[offset + 0]) & BYTE_LONG;
        final long l7 = (record[offset + 1]) & BYTE_LONG;
        final long l6 = (record[offset + 2]) & BYTE_LONG;
        final long l5 = (record[offset + 3]) & BYTE_LONG;
        final long l4 = (record[offset + 4]) & BYTE_LONG;
        final long l3 = (record[offset + 5]) & BYTE_LONG;
        final long l2 = (record[offset + 6]) & BYTE_LONG;
        final long l1 = (record[offset + C_7]) & BYTE_LONG;
        long l;
        if (bigEndian) {
            // Big endian
            l = (l8 << C1);
            l |= (l7 << C2);
            l |= (l6 << C3);
            l |= (l5 << C4);
            l |= (l4 << C5);
            l |= (l3 << C6);
            l |= (l2 << C_8);
            l |= l1;
        } else {
            // Little endian
            // Opposite
            l = (l1 << C1);
            l |= (l2 << C2);
            l |= (l3 << C3);
            l |= (l4 << C4);
            l |= (l5 << C5);
            l |= (l6 << C6);
            l |= (l7 << C_8);
            l |= l8;
        }
        // Return result
        // Conversion to double
        return Double.longBitsToDouble(l);
    }

    /**
     * Extract an int from a record.
     *
     * @param record
     *        record to parse
     * @param offset
     *        offset of the double within the record
     * @return extracted int
     */
    private int extractInt(final byte[] record, final int offset) {
        final int l4 = (record[offset + 0]) & BYTE_INT;
        final int l3 = (record[offset + 1]) & BYTE_INT;
        final int l2 = (record[offset + 2]) & BYTE_INT;
        final int l1 = (record[offset + 3]) & BYTE_INT;

        if (bigEndian) {
            return (l4 << C5) | (l3 << C6) | (l2 << C_8) | l1;
        }
        return (l1 << C5) | (l2 << C6) | (l3 << C_8) | l4;
    }

    /**
     * Extract a String from a record.
     *
     * @param record
     *        record to parse
     * @param offset
     *        offset of the string within the record
     * @param length
     *        maximal length of the string
     * @return extracted string, with whitespace characters stripped
     */
    private static String extractString(final byte[] record, final int offset, final int length) {
        try {
            return new String(record, offset, length, "US-ASCII").trim();
        } catch (final UnsupportedEncodingException uee) {
            throw PatriusException.createInternalError(uee);
        }
    }

    /**
     * Set ephemeris cache size in days. For best performances, cache size should be consistent with consideredd
     * physical timespan.
     * Default value is {@link #FIFTY_DAYS}.
     *
     * @param days number of days
     */
    public static void setCacheSize(final int days) {
        cacheSize = days * Constants.JULIAN_DAY;
    }

    /**
     * Custom deserialization is needed.
     *
     * @param stream
     *        Object stream
     * @throws IOException if an I/O error occurs
     * @throws ClassNotFoundException if the class of a serialized object cannot be found
     */
    private void readObject(final ObjectInputStream stream) throws IOException,
        ClassNotFoundException {
        stream.defaultReadObject();
        // manually deserialize and initialize the ThreadLocal parameters
        constants = new ThreadLocal<AtomicReference<Map<String, Double>>>(){
            /** {@inheritDoc} */
            @Override
            protected AtomicReference<Map<String, Double>> initialValue() {
                return new AtomicReference<>();
            }
        };
        ephemerides = new EphemerisPerThread().getThreadLocal();
    }

    /** Local parser for header constants. */
    private class ConstantsParser implements DataLoader {

        /** Local constants map. */
        private Map<String, Double> localConstants;

        /**
         * Get the local constants map.
         *
         * @return local constants map
         */
        public Map<String, Double> getConstants() {
            return localConstants;
        }

        /** {@inheritDoc} */
        @Override
        public boolean stillAcceptsData() {
            return localConstants == null;
        }

        /** {@inheritDoc} */
        @Override
        public void loadData(final InputStream input,
                             final String name) throws IOException, ParseException, PatriusException {

            // read first header record
            final byte[] first = readFirstRecord(input, name);

            // the second record contains the values of the constants used for least-square filtering
            final byte[] second = new byte[first.length];
            if (!readInRecord(input, second, 0)) {
                throw new PatriusException(PatriusMessages.UNABLE_TO_READ_JPL_HEADER, name);
            }

            localConstants = parseConstants(first, second, name);
        }

    }

    /** Local parser for Chebyshev polynomials. */
    private class EphemerisParser implements DataLoader, TimeStampedGenerator<PosVelChebyshev> {

         /** Serializable UID. */
        private static final long serialVersionUID = 8808047350880399695L;

        /** List of Chebyshev polynomials read. */
        private final List<PosVelChebyshev> entries;

        /** Start of range we are interested in. */
        private AbsoluteDate start;

        /** End of range we are interested in. */
        private AbsoluteDate end;

        /** Current file start epoch. */
        private AbsoluteDate startEpoch;

        /** Current file final epoch. */
        private AbsoluteDate finalEpoch;

        /** Current file chunks duration (in seconds). */
        private double chunksDuration;

        /** Index of the first data for selected body. */
        private int firstIndex;

        /** Number of coefficients for selected body. */
        private int coeffs;

        /** Number of chunks for the selected body. */
        private int chunks;

        /** Number of components contained in the file. */
        private int components;

        /** Unit of the position coordinates (as a multiple of meters). */
        private double positionUnit;

        /**
         * Simple constructor.
         */
        public EphemerisParser() {
            entries = new ArrayList<>();
            chunksDuration = Double.NaN;
        }

        /** {@inheritDoc} */
        @Override
        public List<PosVelChebyshev> generate(final PosVelChebyshev existing,
                                              final AbsoluteDate date) throws TimeStampedCacheException {

            try {
                // prepare reading
                entries.clear();
                if (existing == null) {
                    // we want ephemeris data for the first time, set up an arbitrary first range
                    start = date.shiftedBy(-cacheSize);
                    end = date.shiftedBy(+cacheSize);
                } else if (existing.getDate().compareTo(date) <= 0) {
                    // we want to extend an existing range towards future dates
                    start = existing.getDate();
                    end = date;
                } else {
                    // we want to extend an existing range towards past dates
                    start = date;
                    end = existing.getDate();
                }

                // get new entries in the specified data range
                if (!DataProvidersManager.getInstance().feed(supportedNames, this)) {
                    throw new PatriusException(PatriusMessages.NO_JPL_EPHEMERIDES_BINARY_FILES_FOUND);
                }

                Collections.sort(entries, (o1, o2) -> o1.getDate().compareTo(o2.getDate()));

                return entries;

            } catch (final PatriusException oe) {
                throw new TimeStampedCacheException(oe);
            }
        }

        /** {@inheritDoc} */
        @Override
        public boolean stillAcceptsData() {
            // Special case for Earth: we do not really load any ephemeris data
            // Other cases: we have to look for data in all available ephemerides files as there
            // may be data overlaps that result in incomplete data
            return (generateType != EphemerisType.EARTH);
        }

        /** {@inheritDoc} */
        // CHECKSTYLE: stop CyclomaticComplexity check
        // Reason: Orekit code kept as such
        @Override
        public void loadData(final InputStream input, final String name) throws PatriusException, IOException {
            // CHECKSTYLE: resume CyclomaticComplexity check

            // read first header record
            final byte[] first = readFirstRecord(input, name);

            // the second record contains the values of the constants used for least-square filtering
            final byte[] second = new byte[first.length];
            if (!readInRecord(input, second, 0)) {
                throw new PatriusException(PatriusMessages.UNABLE_TO_READ_JPL_HEADER, name);
            }

            if (constants.get().get() == null) {
                constants.get().compareAndSet(null,
                    parseConstants(first, second, name));
            }

            // check astronomical unit consistency
            final double au = 1000 * extractDouble(first, HEADER_ASTRONOMICAL_UNIT_OFFSET);
            if ((au < MIN_AU) || (au > MAX_AU)) {
                throw new PatriusException(PatriusMessages.NOT_A_JPL_EPHEMERIDES_BINARY_FILE, name);
            }
            if (MathLib.abs(getLoadedAstronomicalUnit() - au) >= THRESHOLD_AU) {
                throw new PatriusException(PatriusMessages.INCONSISTENT_ASTRONOMICAL_UNIT_IN_FILES,
                    getLoadedAstronomicalUnit(), au);
            }

            // check Earth-Moon mass ratio consistency
            final double emRat = extractDouble(first, HEADER_EM_RATIO_OFFSET);
            if ((emRat < MIN_EM_RATIO) || (emRat > MAX_EM_RATIO)) {
                throw new PatriusException(PatriusMessages.NOT_A_JPL_EPHEMERIDES_BINARY_FILE, name);
            }
            if (MathLib.abs(getLoadedEarthMoonMassRatio() - emRat) >= THRESHOLD_MASS_RATIO) {
                throw new PatriusException(PatriusMessages.INCONSISTENT_EARTH_MOON_RATIO_IN_FILES,
                    getLoadedEarthMoonMassRatio(), emRat);
            }

            // parse first header record
            parseFirstHeaderRecord(first, name);

            if (startEpoch.compareTo(end) < 0 && finalEpoch.compareTo(start) > 0) {
                // this file contains data in the range we are looking for, read it
                final byte[] record = new byte[first.length];
                while (readInRecord(input, record, 0)) {
                    final AbsoluteDate rangeStart = parseDataRecord(record);
                    if (rangeStart.compareTo(end) > 0) {
                        // we have already exceeded the range we were interested in,
                        // we interrupt parsing here
                        return;
                    }
                }
            }
        }

        /**
         * Parse the first header record.
         *
         * @param record
         *        first header record
         * @param name
         *        name of the file (or zip entry)
         * @exception PatriusException
         *            if the header is not a JPL ephemerides binary file header
         */
        // CHECKSTYLE: stop CyclomaticComplexity check
        // Reason: Orekit code kept as such
        private void parseFirstHeaderRecord(final byte[] record, final String name) throws PatriusException {
            // CHECKSTYLE: resume CyclomaticComplexity check

            // get the ephemerides type
            final int deNum = extractInt(record, HEADER_EPHEMERIS_TYPE_OFFSET);

            // as default, 3 polynomial coefficients for the cartesian coordinates
            // (x, y, z) are contained in the file, positions are in kilometers
            // and times are in TDB
            components = 3;
            positionUnit = KM_TO_M;
            timeScale = TimeScalesFactory.getTDB();

            if (deNum == INPOP_DE_NUMBER) {
                // an INPOP file may contain 6 components (including coefficients for the velocity vector)
                final double format = getLoadedConstant("FORMAT");
                if (!Double.isNaN(format) && (int) MathLib.IEEEremainder(format, C_10) != 1) {
                    components = 6;
                }

                // INPOP files may have their polynomials expressed in AU
                final double unite = getLoadedConstant("UNITE");
                if (!Double.isNaN(unite) && (int) unite == 0) {
                    positionUnit = getLoadedAstronomicalUnit();
                }

                // INPOP files may have their times expressed in TCB
                final double timesc = getLoadedConstant("TIMESC");
                if (!Double.isNaN(timesc) && (int) timesc == 1) {
                    timeScale = TimeScalesFactory.getTCB();
                }
            }

            // extract covered date range
            startEpoch = extractDate(record, HEADER_START_EPOCH_OFFSET);
            finalEpoch = extractDate(record, HEADER_END_EPOCH_OFFSET);
            boolean ok = finalEpoch.compareTo(startEpoch) > 0;

            // indices of the Chebyshev coefficients for each ephemeris
            for (int i = 0; i < CHEBYCHEV_NUMBER; ++i) {
                final int row1 = extractInt(record, HEADER_CHEBISHEV_INDICES_OFFSET + (4 + 8)
                        * i);
                final int row2 = extractInt(record, HEADER_CHEBISHEV_INDICES_OFFSET + 4
                        + (4 + 8) * i);
                final int row3 = extractInt(record, HEADER_CHEBISHEV_INDICES_OFFSET + 8
                        + (4 + 8) * i);
                ok = ok && (row1 >= 0) && (row2 >= 0) && (row3 >= 0);
                final boolean c1 = (i == 0) && (loadType == EphemerisType.MERCURY);
                final boolean c2 = (i == 1) && (loadType == EphemerisType.VENUS);
                final boolean c3 = (i == 2) && (loadType == EphemerisType.EARTH_MOON);
                final boolean c4 = (i == 3) && (loadType == EphemerisType.MARS);
                final boolean c5 = (i == 4) && (loadType == EphemerisType.JUPITER);
                final boolean c6 = (i == 5) && (loadType == EphemerisType.SATURN);
                final boolean c7 = (i == 6) && (loadType == EphemerisType.URANUS);
                final boolean c8 = (i == C_7) && (loadType == EphemerisType.NEPTUNE);
                final boolean c9 = (i == C_8) && (loadType == EphemerisType.PLUTO);
                final boolean c10 = (i == C_9) && (loadType == EphemerisType.MOON);
                final boolean c11 = (i == C_10) && (loadType == EphemerisType.SUN);
                final boolean c1c2c3c4 = c1 || c2 || c3 || c4;
                final boolean c5c6c7c8 = c5 || c6 || c7 || c8;
                final boolean c9c10c11 = c9 || c10 || c11;
                if (c1c2c3c4 || c5c6c7c8 || c9c10c11) {
                    firstIndex = row1;
                    coeffs = row2;
                    chunks = row3;
                }
            }

            // compute chunks duration
            final double timeSpan = extractDouble(record, HEADER_CHUNK_DURATION_OFFSET);
            ok = ok && (timeSpan > 0) && (timeSpan < MAX_TIMESPAN);
            chunksDuration = Constants.JULIAN_DAY * (timeSpan / chunks);
            if (Double.isNaN(maxChunksDuration)) {
                maxChunksDuration = chunksDuration;
            } else {
                maxChunksDuration = MathLib.max(maxChunksDuration,
                    chunksDuration);
            }

            // sanity checks
            if (!ok) {
                throw new PatriusException(PatriusMessages.NOT_A_JPL_EPHEMERIDES_BINARY_FILE, name);
            }
        }

        /**
         * Parse regular ephemeris record.
         *
         * @param record
         *        record to parse
         * @return date of the last parsed chunk
         * @exception PatriusException
         *            if the header is not a JPL ephemerides binary file header
         */
        private AbsoluteDate parseDataRecord(final byte[] record) throws PatriusException {

            // extract time range covered by the record
            final AbsoluteDate rangeStart = extractDate(record, DATA_START_RANGE_OFFSET);
            if (rangeStart.compareTo(startEpoch) < 0) {
                throw new PatriusException(PatriusMessages.OUT_OF_RANGE_EPHEMERIDES_DATE, rangeStart,
                    startEpoch,
                    finalEpoch);
            }

            final AbsoluteDate rangeEnd = extractDate(record, DATE_END_RANGE_OFFSET);
            if (rangeEnd.compareTo(finalEpoch) > 0) {
                throw new PatriusException(PatriusMessages.OUT_OF_RANGE_EPHEMERIDES_DATE, rangeEnd,
                    startEpoch,
                    finalEpoch);
            }

            if (rangeStart.compareTo(end) > 0 || rangeEnd.compareTo(start) < 0) {
                // we are not interested in this record, don't parse it
                return rangeEnd;
            }

            // loop over chunks inside the time range
            AbsoluteDate chunkEnd = rangeStart;
            final int nbChunks = chunks;
            final int nbCoeffs = coeffs;
            final int first = firstIndex;
            final double duration = chunksDuration;
            for (int i = 0; i < nbChunks; ++i) {

                // set up chunk validity range
                final AbsoluteDate chunkStart = chunkEnd;
                chunkEnd = (i == nbChunks - 1) ? rangeEnd : rangeStart.shiftedBy((i + 1) * duration);

                // extract Chebyshev coefficients for the selected body
                // and convert them from kilometers to meters
                final double[] xCoeffs = new double[nbCoeffs];
                final double[] yCoeffs = new double[nbCoeffs];
                final double[] zCoeffs = new double[nbCoeffs];

                for (int k = 0; k < nbCoeffs; ++k) {
                    // by now, only use the position components
                    // if there are also velocity components contained in the file, ignore them
                    final int index = first + components * i * nbCoeffs + k - 1;
                    xCoeffs[k] =
                        positionUnit
                                * extractDouble(record, C_8 * index);
                    yCoeffs[k] =
                        positionUnit
                                * extractDouble(record, C_8 * (index + nbCoeffs));
                    zCoeffs[k] =
                        positionUnit
                                * extractDouble(record, C_8 * (index + 2 * nbCoeffs));
                }

                // build the position-velocity model for current chunk
                entries.add(new PosVelChebyshev(chunkStart, duration, xCoeffs, yCoeffs, zCoeffs));
            }

            return rangeStart;
        }
    }

    /** Local celestial body class. */
    private class JPLCelestialBodyEphemeris implements CelestialBodyEphemeris {

        /** Serializable UID. */
        private static final long serialVersionUID = -2941415197776129165L;

        /** Scaling factor for position-velocity. */
        private final double scale;

        /** Parent frame. */
        private final Frame parentFrame;

        /**
         * Simple constructor.
         *
         * @param name
         *            name of the body
         * @param scaleIn
         *            scaling factor for position-velocity
         * @param parentFrame
         *            parent frame (usually it should be the ICRF centered on the parent body)
         */
        public JPLCelestialBodyEphemeris(final String name, final double scaleIn, final Frame parentFrame) {
            scale = scaleIn;
            this.parentFrame = parentFrame;
        }

        /** {@inheritDoc} */
        @Override
        public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {

            boolean isOK = false;
            // get raw PV from chebyshev polynomials
            PosVelChebyshev chebyshev = null;
            try {
                // Try with latest data
                chebyshev = ephemerides.get().getLatest();
                if (chebyshev.inRange(date)) {
                    // Required date is in latest data range
                    isOK = true;
                }
            } catch (final IllegalStateException e) {
                // No data has been loaded yet
                isOK = false;
            }

            if (!isOK) {
                try {
                    // Try load neighbor data
                    chebyshev = ephemerides.get().getNeighbors(date)[0];
                } catch (final TimeStampedCacheException tce) {
                    // Date out of ephemeris bounds
                    // Try latest again (unclear)
                    chebyshev = ephemerides.get().getLatest();
                    if (!chebyshev.inRange(date)) {
                        // we were not able to recover from the error, the date is too far
                        throw tce;
                    }
                }
            }

//            PosVelChebyshev chebyshev;
//            try {
//                chebyshev = ephemerides.get().getNeighbors(date)[0];
//            } catch (final TimeStampedCacheException tce) {
//                // we cannot bracket the date, check if the last available chunk covers the specified date
//                chebyshev = ephemerides.get().getLatest();
//                if (!chebyshev.inRange(date)) {
//                    // we were not able to recover from the error, the date is too far
//                    throw tce;
//                }
//            }
            final PVCoordinates pv = chebyshev.getPositionVelocity(date);
            final PVCoordinates scaledPV = new PVCoordinates(scale, pv);

            // the raw PV are relative to the parent frame
            final Transform transform = parentFrame.getTransformTo(frame, date, true);

            // convert to request frame
            return transform.transformPVCoordinates(scaledPV);

        }

        /** {@inheritDoc} */
        @Override
        public Frame getNativeFrame(final AbsoluteDate date,
                final Frame frame) throws PatriusException {
            return parentFrame;
        }
    }

    /**
     * Provides per-thread Ephemeris.
     *
     * @author Emmanuel Bignon
     *
     * @version $Id: PrecessionNutationPerThread.java 18073 2017-10-02 16:48:07Z bignon $
     *
     * @since 4.5
     */
    private class EphemerisPerThread {

        /**
         * Will hold all thread-specific model instances.
         */
        private final ThreadLocal<TimeStampedCache<PosVelChebyshev>> threadLocalModel =
            new ThreadLocal<TimeStampedCache<PosVelChebyshev>>(){
                /** {@inheritDoc} */
                @Override
                protected TimeStampedCache<PosVelChebyshev> initialValue() {
                return new TimeStampedCache<>(2, PatriusConfiguration.getCacheSlotsNumber(),
                        Double.POSITIVE_INFINITY, cacheSize,
                        new EphemerisParser(), PosVelChebyshev.class);
                }
            };

        /**
         * Returns the thread-specific model instance.
         *
         * @return a model
         */
        public ThreadLocal<TimeStampedCache<PosVelChebyshev>> getThreadLocal() {
            return threadLocalModel;
        }
    }
}
