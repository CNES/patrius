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
 */
/*
 * HISTORY
* VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
* VERSION:4.6:DM:DM-2563:27/01/2021:[PATRIUS] Ajout de la matrice de transition J2Secular 
* VERSION:4.6:FA:FA-2564:27/01/2021:[PATRIUS] Probleme avec le constructeur TLE (Bstar) 
 * VERSION:4.5:FA:FA-2357:27/05/2020:champs TLE mal considere 
* VERSION:4.4:DM:DM-2135:04/10/2019:[PATRIUS] Methodes equals() et hashCode() non implementees dans la classe TLE
* VERSION:4.4:FA:FA-2296:04/10/2019:[PATRIUS] Meilleure robustesse a la lecture de TLEs
* VERSION:4.4:FA:FA-2258:04/10/2019:[PATRIUS] ecriture TLE et angles negatif
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS/COLOSUS] Mise en conformite code avec nouveau standard codage DYNVOL
 * VERSION::FA:1470:19/03/2018:Anomaly TLE reading
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * VERSION::FA:1970:03/01/2019:quality corrections
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.analytical.tle;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.regex.Pattern;

import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScale;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.time.TimeStamped;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

//CHECKSTYLE: stop MagicNumber check
//CHECKSTYLE: stop CommentRatio check
// Reason: model - Orekit code

/**
 * This class is a container for a single set of TLE data.
 *
 * <p>
 * TLE sets can be built either by providing directly the two lines, in which case parsing is
 * performed internally or by providing the already parsed elements.
 * </p>
 * <p>
 * TLE are not transparently convertible to {@link fr.cnes.sirius.patrius.orbits.Orbit Orbit}
 * instances. They are significant only with respect to their dedicated {@link TLEPropagator
 * propagator}, which also computes position and velocity coordinates. Any attempt to directly use
 * orbital parameters like {@link #getE() eccentricity}, {@link #getI() inclination}, etc. without
 * any reference to the {@link TLEPropagator TLE propagator} is prone to errors.
 * </p>
 * <p>
 * More information on the TLE format can be found on the <a
 * href="http://www.celestrak.com/">CelesTrak website.</a>
 * </p>
 *
 * @author Fabien Maussion
 * @author Luc Maisonobe
 */
@SuppressWarnings({"PMD.ShortClassName", "PMD.NullAssignment"})
public class TLE implements TimeStamped, Serializable {

    /** Identifier for default type of ephemeris (SGP4/SDP4). */
    public static final int DEFAULT = 0;

    /** Identifier for SGP type of ephemeris. */
    public static final int SGP = 1;

    /** Identifier for SGP4 type of ephemeris. */
    public static final int SGP4 = 2;

    /** Identifier for SDP4 type of ephemeris. */
    public static final int SDP4 = 3;

    /** Identifier for SGP8 type of ephemeris. */
    public static final int SGP8 = 4;

    /** Identifier for SDP8 type of ephemeris. */
    public static final int SDP8 = 5;

    /** Pattern for line 1. */
    private static final Pattern LINE_1_PATTERN = Pattern
            .compile("1 [ 0-9]{5}[A-Z] [ 0-9]{5,7}[ A-Z]{1,3} [ 0-9]{5}[.][ 0-9]{8} [ 0+-][.][ 0-9]{8} "
                    + "[ +-]*[ 0-9]{5}[+-0][ 0-9] [ +-]*[ 0-9]{5}[+-0][ 0-9] [ 0-9] [ 0-9]{3,4}[ 0-9]");

    /** Pattern for line 2. */
    private static final Pattern LINE_2_PATTERN = Pattern
            .compile("2 [ 0-9]{5} [ 0-9]{1,3}[.][ 0-9]{4} [ 0-9]{1,3}[.][ 0-9]{4} [ 0-9]{7} "
                    + "[ 0-9]{1,3}[.][ 0-9]{4} [ 0-9]{1,3}[.][ 0-9]{4} [ 0-9]{2}[.][ 0-9]{13}[ 0-9]");

    /** International symbols for parsing. */
    private static final DecimalFormatSymbols SYMBOLS = new DecimalFormatSymbols(Locale.US);

    /** TLE line length. */
    private static final int TLE_LENGTH = 69;

    /** Number of years in a century. */
    private static final int YEARS_IN_CENTURY = 100;

    /** Serializable UID. */
    private static final long serialVersionUID = -1596648022319057689L;

    /** Root int for hash code. */
    private static final int ROOTINT = 17;

    /** Dot. */
    private static final String DOT = ".";
    
    /** The satellite number. */
    private final int satelliteNumber;

    /** Classification (U for unclassified). */
    private final char classification;

    /** Launch year. */
    private final int launchYear;

    /** Launch number. */
    private final int launchNumber;

    /** Piece of launch (from "A" to "ZZZ"). */
    private final String launchPiece;

    /** Type of ephemeris. */
    private final int ephemerisType;

    /** Element number. */
    private final int elementNumber;

    /** the TLE current date. */
    private final AbsoluteDate epoch;

    /** Mean motion (rad/s). */
    private final double meanMotion;

    /** Mean motion first derivative (rad/s<sup>2</sup>). */
    private final double meanMotionFirstDerivative;

    /** Mean motion second derivative (rad/s<sup>3</sup>). */
    private final double meanMotionSecondDerivative;

    /** Eccentricity. */
    private final double eccentricity;

    /** Inclination (rad). */
    private final double inclination;

    /** Argument of perigee (rad). */
    private final double pa;

    /** Right Ascension of the Ascending node (rad). */
    private final double raan;

    /** Mean anomaly (rad). */
    private final double meanAnomaly;

    /** Revolution number at epoch. */
    private final int revolutionNumberAtEpoch;

    /** Ballistic coefficient. */
    private final double bStar;

    /** First line. */
    private String line1;

    /** Second line. */
    private String line2;

    /**
     * Simple constructor from unparsed two lines.
     * <p>
     * The static method {@link #isFormatOK(String, String)} should be called before trying to build
     * this object.
     * <p>
     *
     * @param line1In
     *        the first element (69 char String)
     * @param line2In
     *        the second element (69 char String)
     * @exception PatriusException
     *            if some format error occurs or lines are inconsistent
     */
    public TLE(final String line1In, final String line2In) throws PatriusException {

        // identification
        this.satelliteNumber = parseInteger(line1In, 2, 5);
        final int satNum2 = parseInteger(line2In, 2, 5);
        if (this.satelliteNumber != satNum2) {
            throw new PatriusException(PatriusMessages.TLE_LINES_DO_NOT_REFER_TO_SAME_OBJECT,
                    line1In, line2In);
        }
        this.classification = line1In.charAt(7);

        // Check the year format
        final CharSequence stringTrim = line1In.substring(9, 18);
        boolean isBigYear = false;
        int nbDigit = 0;
        for (int i = 0; i < stringTrim.length(); i++) {
            if (Character.isDigit(stringTrim.charAt(i))) {
                nbDigit++;
            }
        }
        // The year is entire
        if (nbDigit > 6) {
            isBigYear = true;
            this.launchYear = parseYear(line1In, 9, isBigYear);
            this.launchNumber = parseInteger(line1In, 13, 3);
            this.launchPiece = line1In.substring(16, 17).trim();
        } else {
            // Only two last number of the year
            this.launchYear = parseYear(line1In, 9, isBigYear);
            this.launchNumber = parseInteger(line1In, 11, 3);
            this.launchPiece = line1In.substring(14, 17).trim();
        }

        this.ephemerisType = parseInteger(line1In, 62, 1);
        this.elementNumber = parseInteger(line1In, 64, 4);

        // Date format transform (nota: 27/31250 == 86400/100000000)
        final int year = parseYear(line1In, 18, false);
        final int dayInYear = parseInteger(line1In, 20, 3);
        final long df = 27L * parseInteger(line1In, 24, 8);
        final int secondsA = (int) (df / 31250L);
        final double secondsB = (df % 31250L) / 31250.0;
        this.epoch = new AbsoluteDate(new DateComponents(year, dayInYear), new TimeComponents(
                secondsA, secondsB), TimeScalesFactory.getUTC());

        // mean motion development
        // converted from rev/day, 2 * rev/day^2 and 6 * rev/day^3 to rad/s,
        // rad/s^2 and rad/s^3
        this.meanMotion = parseDouble(line2In, 52, 11) * FastMath.PI / (Constants.JULIAN_DAY / 2.);
        this.meanMotionFirstDerivative = parseDouble(line1In, 33, 10) * FastMath.PI / 1.86624e9;
        this.meanMotionSecondDerivative = Double.parseDouble((line1In.substring(44, 45) + '.'
                + line1In.substring(45, 50) + 'e' + line1In.substring(50, 52)).replace(' ', '0'))
                * FastMath.PI / 5.3747712e13;
        this.eccentricity = Double.parseDouble(DOT + line2In.substring(26, 33).replace(' ', '0'));
        this.inclination = MathLib.toRadians(parseDouble(line2In, 8, 8));
        this.pa = MathLib.toRadians(parseDouble(line2In, 34, 8));
        this.raan = MathLib.toRadians(Double.parseDouble(line2In.substring(17, 25).replace(' ', '0')));
        this.meanAnomaly = MathLib.toRadians(parseDouble(line2In, 43, 8));

        this.revolutionNumberAtEpoch = parseInteger(line2In, 63, 5);

        this.bStar = Double.parseDouble((line1In.substring(53, 54) + '.'
                + line1In.substring(54, 59) + 'e' + line1In.substring(59, 61)).replace(' ', '0'));
        // save the lines
        this.line1 = line1In;
        this.line2 = line2In;

    }

    /**
     * Simple constructor from already parsed elements.
     *
     * @param satelliteNumberIn
     *        satellite number
     * @param classificationIn
     *        classification (U for unclassified)
     * @param launchYearIn
     *        launch year (all digits)
     * @param launchNumberIn
     *        launch number
     * @param launchPieceIn
     *        launch piece
     * @param ephemerisTypeIn
     *        type of ephemeris
     * @param elementNumberIn
     *        element number
     * @param epochIn
     *        elements epoch
     * @param meanMotionIn
     *        mean motion (rad/s)
     * @param meanMotionFirstDerivativeIn
     *        mean motion first derivative (rad/s<sup>2</sup>)
     * @param meanMotionSecondDerivativeIn
     *        mean motion second derivative (rad/s<sup>3</sup>)
     * @param e
     *        eccentricity
     * @param i
     *        inclination (rad). In case of negative inclination, inclination will be set to positive value and 180&deg;
     *        will be added to RAAN value
     * @param paIn
     *        argument of perigee (rad). It will then be normalized in [0, 360&deg;]
     * @param raanIn
     *        right ascension of ascending node (rad). It will then be normalized in [0, 360&deg;]
     * @param meanAnomalyIn
     *        mean anomaly (rad). It will then be normalized in [0, 360&deg;]
     * @param revolutionNumberAtEpochIn
     *        revolution number at epoch
     * @param bStarIn
     *        ballistic coefficient
     * @throws PatriusException thrown if inclination is negative
     */
    public TLE(final int satelliteNumberIn, final char classificationIn, final int launchYearIn,
            final int launchNumberIn, final String launchPieceIn, final int ephemerisTypeIn,
            final int elementNumberIn, final AbsoluteDate epochIn, final double meanMotionIn,
            final double meanMotionFirstDerivativeIn, final double meanMotionSecondDerivativeIn,
            final double e, final double i, final double paIn, final double raanIn,
            final double meanAnomalyIn, final int revolutionNumberAtEpochIn,
            final double bStarIn) throws PatriusException {

        // identification
        this.satelliteNumber = satelliteNumberIn;
        this.classification = classificationIn;
        this.launchYear = launchYearIn;
        this.launchNumber = launchNumberIn;
        this.launchPiece = launchPieceIn;
        this.ephemerisType = ephemerisTypeIn;
        this.elementNumber = elementNumberIn;

        // orbital parameters
        this.epoch = epochIn;
        this.meanMotion = meanMotionIn;
        this.meanMotionFirstDerivative = meanMotionFirstDerivativeIn;
        this.meanMotionSecondDerivative = meanMotionSecondDerivativeIn;
        this.eccentricity = e;
        this.pa = MathUtils.normalizeAngle(paIn, FastMath.PI);
        this.meanAnomaly = MathUtils.normalizeAngle(meanAnomalyIn, FastMath.PI);
        this.inclination = i;
        this.raan = MathUtils.normalizeAngle(raanIn, FastMath.PI);
        if (i < 0) {
            // Negative inclination not allowed
            throw new PatriusException(PatriusMessages.NEGATIVE_INCLINATION, i);
        }

        this.revolutionNumberAtEpoch = revolutionNumberAtEpochIn;
        this.bStar = bStarIn;

        // don't build the line until really needed
        this.line1 = null;
        this.line2 = null;

    }

    /**
     * Get the first line.
     *
     * @return first line
     * @exception PatriusException
     *            if UTC conversion cannot be done
     */
    public String getLine1() throws PatriusException {
        if (this.line1 == null) {
            this.buildLine1();
        }
        return this.line1;
    }

    /**
     * Get the second line.
     *
     * @return second line
     */
    public String getLine2() {
        if (this.line2 == null) {
            this.buildLine2();
        }
        return this.line2;
    }

    /**
     * Build the line 1 from the parsed elements.
     *
     * @exception PatriusException
     *            if UTC conversion cannot be done
     */
    private void buildLine1() throws PatriusException {

        final StringBuffer buffer = new StringBuffer();
        final DecimalFormat f38 = new DecimalFormat("000.00000000", SYMBOLS);

        buffer.append('1');

        buffer.append(' ');
        buffer.append(addPadding(this.satelliteNumber, '0', 5, true));
        buffer.append(this.classification);

        buffer.append(' ');
        buffer.append(addPadding(this.launchYear % YEARS_IN_CENTURY, '0', 2, true));
        buffer.append(addPadding(this.launchNumber, '0', 3, true));
        buffer.append(addPadding(this.launchPiece, ' ', 3, false));

        buffer.append(' ');
        final TimeScale utc = TimeScalesFactory.getUTC();
        final int year = this.epoch.getComponents(utc).getDate().getYear();
        buffer.append(addPadding(year % YEARS_IN_CENTURY, '0', 2, true));
        final double day = 1.0 + this.epoch.durationFrom(new AbsoluteDate(year, 1, 1, utc))
                / Constants.JULIAN_DAY;
        buffer.append(f38.format(day));

        buffer.append(' ');
        final double n1 = this.meanMotionFirstDerivative * 1.86624e9 / FastMath.PI;
        final String sn1 = addPadding(new DecimalFormat(".00000000", SYMBOLS).format(n1), ' ', 10, true);
        buffer.append(sn1);

        buffer.append(' ');
        final double n2 = this.meanMotionSecondDerivative * 5.3747712e13 / FastMath.PI;
        buffer.append(addPadding(formatExponentMarkerFree(n2, 5), ' ', 8, true));

        buffer.append(' ');
        buffer.append(addPadding(formatExponentMarkerFree(this.bStar, 5), ' ', 8, true));

        buffer.append(' ');
        buffer.append(this.ephemerisType);

        buffer.append(' ');
        buffer.append(addPadding(this.elementNumber, ' ', 4, true));

        buffer.append(Integer.toString(checksum(buffer)));

        this.line1 = buffer.toString();

    }

    /**
     * Format a real number without 'e' exponent marker.
     *
     * @param d
     *        number to format
     * @param mantissaSize
     *        size of the mantissa (not counting initial '-' or ' ' for
     *        sign)
     * @return formatted number
     */
    private static String formatExponentMarkerFree(final double d, final int mantissaSize) {
        final double dAbs = MathLib.abs(d);
        final double logd = MathLib.log10(dAbs);
        int exponent = (dAbs < 1.0e-9) ? -9 : (int) MathLib.ceil(logd);
        final long mantissa;
        if (logd == (int) logd) {
            // Specific case: d is under the form 1E-x
            mantissa = 10000;
            exponent += 1;
        } else {
            mantissa = MathLib.round(dAbs * MathLib.pow(10.0, mantissaSize - exponent));
        }
        if (mantissa == 0) {
            exponent = 0;
        }
        final String sMantissa = addPadding((int) mantissa, '0', mantissaSize, true);
        final String sExponent = Integer.toString(MathLib.abs(exponent));
        return (d < 0 ? '-' : ' ') + sMantissa + (exponent <= 0 ? '-' : '+') + sExponent;
    }

    /**
     * Build the line 2 from the parsed elements.
     */
    private void buildLine2() {

        final StringBuffer buffer = new StringBuffer();
        final DecimalFormat f34 = new DecimalFormat("##0.0000", SYMBOLS);
        final DecimalFormat f211 = new DecimalFormat("#0.00000000", SYMBOLS);

        buffer.append('2');

        buffer.append(' ');
        buffer.append(addPadding(this.satelliteNumber, '0', 5, true));

        buffer.append(' ');
        buffer.append(addPadding(f34.format(MathLib.toDegrees(this.inclination)), ' ', 8, true));
        buffer.append(' ');
        buffer.append(addPadding(f34.format(MathLib.toDegrees(this.raan)), ' ', 8, true));
        buffer.append(' ');
        buffer.append(addPadding((int) MathLib.rint(this.eccentricity * 1.0e7), '0', 7, true));
        buffer.append(' ');
        buffer.append(addPadding(f34.format(MathLib.toDegrees(this.pa)), ' ', 8, true));
        buffer.append(' ');
        buffer.append(addPadding(f34.format(MathLib.toDegrees(this.meanAnomaly)), ' ', 8, true));

        buffer.append(' ');
        buffer.append(addPadding(f211.format(this.meanMotion * Constants.JULIAN_DAY / (2. * FastMath.PI)), ' ', 11,
            true));
        buffer.append(addPadding(this.revolutionNumberAtEpoch, ' ', 5, true));

        buffer.append(Integer.toString(checksum(buffer)));

        this.line2 = buffer.toString();

    }

    /**
     * Add padding characters before an integer.
     *
     * @param k
     *        integer to pad
     * @param c
     *        padding character
     * @param size
     *        desired size
     * @param rightJustified
     *        if true, the resulting string is right justified (i.e. space
     *        are added to the left)
     * @return padded string
     */
    private static String addPadding(final int k, final char c, final int size, final boolean rightJustified) {
        return addPadding(Integer.toString(k), c, size, rightJustified);
    }

    /**
     * Add padding characters to a string.
     *
     * @param string
     *        string to pad
     * @param c
     *        padding character
     * @param size
     *        desired size
     * @param rightJustified
     *        if true, the resulting string is right justified (i.e. space
     *        are added to the left)
     * @return padded string
     */
    private static String addPadding(final String string, final char c, final int size,
            final boolean rightJustified) {

        final StringBuffer padding = new StringBuffer();
        for (int i = 0; i < size; ++i) {
            padding.append(c);
        }

        if (rightJustified) {
            final String concatenated = padding + string;
            final int l = concatenated.length();
            return concatenated.substring(l - size, l);
        }

        return (string + padding).substring(0, size);
    }

    /**
     * Parse a double.
     *
     * @param line
     *        line to parse
     * @param start
     *        start index of the first character
     * @param length
     *        length of the string
     * @return value of the double
     */
    private static double parseDouble(final String line, final int start, final int length) {
        return Double.parseDouble(line.substring(start, start + length).replace(' ', '0'));
    }

    /**
     * Parse an integer.
     *
     * @param line
     *        line to parse
     * @param start
     *        start index of the first character
     * @param length
     *        length of the string
     * @return value of the integer
     */
    private static int parseInteger(final String line, final int start, final int length) {
        return Integer.parseInt(line.substring(start, start + length).replace(' ', '0'));
    }

    /**
     * Parse a year written on 2 digits.
     *
     * @param line
     *        line to parse
     * @param start
     *        start index of the first character
     * @param isBigYear true if year is coded on 4 digits
     * @return value of the year
     */
    private static int parseYear(final String line, final int start, final boolean isBigYear) {
        if (isBigYear) {
            return parseInteger(line, start, 4);
        }
        final int year = 2000 + parseInteger(line, start, 2);
        return (year > 2056) ? (year - YEARS_IN_CENTURY) : year;
    }

    /**
     * Get the satellite id.
     *
     * @return the satellite number
     */
    public int getSatelliteNumber() {
        return this.satelliteNumber;
    }

    /**
     * Get the classification.
     *
     * @return classification
     */
    public char getClassification() {
        return this.classification;
    }

    /**
     * Get the launch year.
     *
     * @return the launch year
     */
    public int getLaunchYear() {
        return this.launchYear;
    }

    /**
     * Get the launch number.
     *
     * @return the launch number
     */
    public int getLaunchNumber() {
        return this.launchNumber;
    }

    /**
     * Get the launch piece.
     *
     * @return the launch piece
     */
    public String getLaunchPiece() {
        return this.launchPiece;
    }

    /**
     * Get the type of ephemeris.
     *
     * @return the ephemeris type (one of {@link #DEFAULT}, {@link #SGP}, {@link #SGP4},
     *         {@link #SGP8}, {@link #SDP4}, {@link #SDP8})
     */
    public int getEphemerisType() {
        return this.ephemerisType;
    }

    /**
     * Get the element number.
     *
     * @return the element number
     */
    public int getElementNumber() {
        return this.elementNumber;
    }

    /**
     * Get the TLE current date.
     *
     * @return the epoch
     */
    @Override
    public AbsoluteDate getDate() {
        return this.epoch;
    }

    /**
     * Get the mean motion.
     *
     * @return the mean motion (rad/s)
     */
    public double getMeanMotion() {
        return this.meanMotion;
    }

    /**
     * Get the mean motion first derivative.
     *
     * @return the mean motion first derivative (rad/s<sup>2</sup>)
     */
    public double getMeanMotionFirstDerivative() {
        return this.meanMotionFirstDerivative;
    }

    /**
     * Get the mean motion second derivative.
     *
     * @return the mean motion second derivative (rad/s<sup>3</sup>)
     */
    public double getMeanMotionSecondDerivative() {
        return this.meanMotionSecondDerivative;
    }

    /**
     * Get the eccentricity.
     *
     * @return the eccentricity
     */
    public double getE() {
        return this.eccentricity;
    }

    /**
     * Get the inclination in [0, &pi;].
     *
     * @return the inclination (rad)
     */
    public double getI() {
        return this.inclination;
    }

    /**
     * Get the argument of perigee in [0, 2&pi;].
     *
     * @return omega (rad)
     */
    public double getPerigeeArgument() {
        return this.pa;
    }

    /**
     * Get Right Ascension of the Ascending node in [0, 2&pi;].
     *
     * @return the raan (rad)
     */
    public double getRaan() {
        return this.raan;
    }

    /**
     * Get the mean anomaly in [0, 2&pi;].
     *
     * @return the mean anomaly (rad)
     */
    public double getMeanAnomaly() {
        return this.meanAnomaly;
    }

    /**
     * Get the revolution number.
     *
     * @return the revolutionNumberAtEpoch
     */
    public int getRevolutionNumberAtEpoch() {
        return this.revolutionNumberAtEpoch;
    }

    /**
     * Get the ballistic coefficient.
     *
     * @return bStar
     */
    public double getBStar() {
        return this.bStar;
    }

    /**
     * Get a string representation of this TLE set.
     * <p>
     * The representation is simply the two lines separated by the platform line separator.
     * </p>
     *
     * @return string representation of this TLE set
     */
    @Override
    public String toString() {
        try {
            return this.getLine1() + System.getProperty("line.separator") + this.getLine2();
        } catch (final PatriusException oe) {
            throw PatriusException.createInternalError(oe);
        }
    }

    /**
     * Check the lines format validity.
     *
     * @param line1
     *        the first element
     * @param line2
     *        the second element
     * @return true if format is recognized (non null lines, 69 characters
     *         length, line content), false if not
     * @exception PatriusException
     *            if checksum is not valid
     */
    public static boolean isFormatOK(final String line1, final String line2)
            throws PatriusException {

        boolean res;
        if (line1 == null || line1.length() != TLE_LENGTH || line2 == null
                || line2.length() != TLE_LENGTH) {
            // Basic check on line existence and length
            res = false;
        } else if (!(LINE_1_PATTERN.matcher(line1).matches() && LINE_2_PATTERN.matcher(line2)
                .matches())) {
            res = false;
        } else {
            // General case

            // check sums
            final int checksum1 = checksum(line1);
            if (Integer.parseInt(line1.substring(TLE_LENGTH - 1)) != (checksum1 % 10)) {
                throw new PatriusException(PatriusMessages.TLE_CHECKSUM_ERROR, 1,
                        line1.substring(TLE_LENGTH - 1), checksum1 % 10, line1);
            }

            final int checksum2 = checksum(line2);
            if (Integer.parseInt(line2.substring(TLE_LENGTH - 1)) != (checksum2 % 10)) {
                throw new PatriusException(PatriusMessages.TLE_CHECKSUM_ERROR, 2,
                        line2.substring(TLE_LENGTH - 1), checksum2 % 10, line2);
            }

            res = true;

            // Other complementary checks
            if (!DOT.equals(line1.substring(23, 24))) {
                // 24th character is a dot
                res = false;
            }
        }

        // Return result
        return res;

    }

    /**
     * Compute the checksum of the first 68 characters of a line.
     *
     * @param line
     *        line to check
     * @return checksum
     */
    private static int checksum(final CharSequence line) {
        int sum = 0;
        for (int j = 0; j < TLE_LENGTH - 1; j++) {
            final char c = line.charAt(j);
            if (Character.isDigit(c)) {
                sum += Character.digit(c, 10);
            } else if (c == '-') {
                ++sum;
            }
        }
        return sum % 10;
    }

    /**
     * Test for the equality of two TLE objects.
     * <p>
     * TLE objects are considered equals if they have the same attributes
     * </p>
     * 
     * @param object
     *        Object to test for equality to this
     * @return true if two TLE are equal
     */
    @Override
    public boolean equals(final Object object) {
        boolean isEqual = true;

        if (object == this) {
            isEqual = true;
        } else if (object instanceof TLE) {
            final TLE other = (TLE) object;

            isEqual &= (this.getSatelliteNumber() == other.getSatelliteNumber());
            isEqual &= (this.getClassification() == other.getClassification());
            isEqual &= (this.getLaunchYear() == other.getLaunchYear());
            isEqual &= (this.getLaunchNumber() == other.getLaunchNumber());
            isEqual &= (this.getLaunchPiece().equals(other.getLaunchPiece()));
            isEqual &= (this.getEphemerisType() == other.getEphemerisType());
            isEqual &= (this.getElementNumber() == other.getElementNumber());
            isEqual &= (this.getDate().equals(other.getDate()));
            isEqual &= (this.getMeanMotion() == other.getMeanMotion());
            isEqual &= (this.getMeanMotionFirstDerivative() == other.getMeanMotionFirstDerivative());
            isEqual &= (this.getMeanMotionSecondDerivative() == other
                    .getMeanMotionSecondDerivative());
            isEqual &= (this.getE() == other.getE());
            isEqual &= (this.getI() == other.getI());
            isEqual &= (this.getPerigeeArgument() == other.getPerigeeArgument());
            isEqual &= (this.getRaan() == other.getRaan());
            isEqual &= (this.getMeanAnomaly() == other.getMeanAnomaly());
            isEqual &= (this.getRevolutionNumberAtEpoch() == other.getRevolutionNumberAtEpoch());
            isEqual &= (this.getBStar() == other.getBStar());

        } else {
            isEqual = false;
        }

        return isEqual;
    }

    /**
     * Get a hashCode for the TLE.
     * 
     * @return a hash code value for this object
     */
    @Override
    public int hashCode() {

        // A not zero random "root int"
        int result = ROOTINT;
        // An efficient multiplier (JVM optimizes 31 * i as (i << 5) - 1 )
        final int effMult = 31;
        // Good hashcode : it's the same
        // for "equal" TLE, but
        // reasonably sure it's different otherwise.
        result = effMult * result + this.getSatelliteNumber();
        result = effMult * result + MathUtils.hash(this.getClassification());
        result = effMult * result + this.getLaunchYear();
        result = effMult * result + this.getLaunchNumber();
        result = effMult * result + this.getLaunchPiece().hashCode();
        result = effMult * result + this.getEphemerisType();
        result = effMult * result + this.getElementNumber();
        result = effMult * result + this.getDate().hashCode();
        result = effMult * result + MathUtils.hash(this.getMeanMotion());
        result = effMult * result + MathUtils.hash(this.getMeanMotionFirstDerivative());
        result = effMult * result + MathUtils.hash(this.getMeanMotionSecondDerivative());
        result = effMult * result + MathUtils.hash(this.getE());
        result = effMult * result + MathUtils.hash(this.getI());
        result = effMult * result + MathUtils.hash(this.getPerigeeArgument());
        result = effMult * result + MathUtils.hash(this.getRaan());
        result = effMult * result + MathUtils.hash(this.getMeanAnomaly());
        result = effMult * result + this.getRevolutionNumberAtEpoch();
        result = effMult * result + MathUtils.hash(this.getBStar());

        return result;
    }

    // CHECKSTYLE: resume MagicNumber check
    // CHECKSTYLE: resume CommentRatio check
}
