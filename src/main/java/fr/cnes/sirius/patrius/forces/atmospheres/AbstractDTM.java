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
 *
 * HISTORY
 * VERSION:4.12.1:FA:FA-123:05/09/2023:[PATRIUS] Utilisation de getLLHCoordinates() au 
 *          lieu de getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC) 
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.7:FA:FA-2853:18/05/2021:Erreurs documentation javadoc suite au refactoring modèle d'atmosphère DTM 
 * VERSION:4.7:FA:FA-2762:18/05/2021:Probleme lors des controles qualite via la PIC 
 * VERSION:4.6:DM:DM-2563:27/01/2021:[PATRIUS] Ajout de la matrice de transition J2Secular 
 * VERSION:4.6:DM:DM-2528:27/01/2021:[PATRIUS] Integration du modele DTM 
 * VERSION:4.6:FA:FA-2692:27/01/2021:[PATRIUS] Robustification de AbstractGroundPointing dans le cas de vitesses
 * non significatives 
 * VERSION:4.6:DM:DM-2591:27/01/2021:[PATRIUS] Intigration et validation JOptimizer
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.forces.atmospheres;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.nio.charset.Charset;
import java.util.Arrays;

import fr.cnes.sirius.patrius.bodies.BodyPoint.BodyPointName;
import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.bodies.EllipsoidBodyShape;
import fr.cnes.sirius.patrius.bodies.EllipsoidPoint;
import fr.cnes.sirius.patrius.bodies.LLHCoordinatesSystem;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.exception.util.DummyLocalizable;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateTimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.PatriusRuntimeException;

// CHECKSTYLE: stop MagicNumber check
// Reason: model - Orekit code

/**
 * Common class for all DTM atmospheric models.
 *
 * @author Williams Zanga
 *
 * @since 4.6
 */
@SuppressWarnings("PMD.NullAssignment")
public abstract class AbstractDTM implements ExtendedAtmosphere {

    /** Identifier for hydrogen. */
    public static final int HYDROGEN = 1;

    /** Identifier for helium. */
    public static final int HELIUM = 2;

    /** Identifier for atomic oxygen. */
    public static final int ATOMIC_OXYGEN = 3;

    /** Identifier for molecular nitrogen. */
    public static final int MOLECULAR_NITROGEN = 4;

    /** Identifier for molecular oxygen. */
    public static final int MOLECULAR_OXYGEN = 5;

    /** Identifier for atomic nitrogen. */
    public static final int ATOMIC_NITROGEN = 6;

    /** Thermal diffusion coefficient. */
    protected static final double[] ALEFA = { 0, -0.40, -0.38, 0, 0, 0, 0 };

    /** Atomic mass H, He, O, N2, O2, N. */
    protected static final double[] MA = { 0, 1, 4, 16, 28, 32, 14 };

    /** Atomic mass H, He, O, N2, O2, N. */
    protected static final double[] VMA = { 0, 1.6606e-24, 6.6423e-24, 26.569e-24, 46.4958e-24, 53.1381e-24,
        23.2479e-24 };

    /** Polar Earth radius. */
    protected static final double RE = 6356.77;

    /** Reference altitude. */
    protected static final double ZLB0 = 120.0;

    /** Cosine of the latitude of the magnetic pole (79N, 71W). */
    protected static final double CPMG = .19081;

    /** Sine of the latitude of the magnetic pole (79N, 71W). */
    protected static final double SPMG = .98163;

    /** Longitude (in radians) of the magnetic pole (79N, 71W). */
    protected static final double XLMG = -1.2392;

    /** Gravity acceleration at 120 km altitude. */
    protected static final double GSURF = 980.665;

    /** Universal gas constant. */
    protected static final double RGAS = 831.4;

    /** 2 * &pi; / 365. */
    protected static final double ROT = 0.017214206;

    /** 2 * rot. */
    protected static final double ROT2 = 0.034428412;

    /** Serializable UID. */
    private static final long serialVersionUID = -8901940398967553588L;

    /** Adiabatic constant. */
    private static final double GAMMA = 1.4;

    /** Charset for data file reading. */
    private static final Charset CHARSET = Charset.forName("UTF-8");

    /** Elements coefficients. */
    protected double[] tt = null;

    /** Elements coefficients. */
    protected double[] h = null;

    /** Elements coefficients. */
    protected double[] he = null;

    /** Elements coefficients. */
    protected double[] o = null;

    /** Elements coefficients. */
    protected double[] az2 = null;

    /** Elements coefficients. */
    protected double[] o2 = null;

    /** Elements coefficients. */
    protected double[] az = null;

    /** Elements coefficients. */
    protected double[] t0 = null;

    /** Elements coefficients. */
    protected double[] tp = null;

    /** Partial derivatives. */
    protected double[] dtt = null;

    /** Partial derivatives. */
    protected double[] dh = null;

    /** Partial derivatives. */
    protected double[] dhe = null;

    /** Partial derivatives. */
    protected double[] dox = null;

    /** Partial derivatives. */
    protected double[] daz2 = null;

    /** Partial derivatives. */
    protected double[] do2 = null;

    /** Partial derivatives. */
    protected double[] daz = null;

    /** Partial derivatives. */
    protected double[] dt0 = null;

    /** Partial derivatives. */
    protected double[] dtp = null;

    /** Number of days in current year. */
    protected double cachedDay;

    /** Instant solar flux. f[1] = instantaneous flux; f[2] = 0. (not used). */
    protected final double[] cachedF = new double[3];

    /** Mean solar flux. fbar[1] = mean flux; fbar[2] = 0. (not used). */
    protected final double[] cachedFbar = new double[3];

    /**
     * Kp coefficients.
     * <ul>
     * <li>akp[1] = 3-hourly kp</li>
     * <li>akp[2] = 0 (not used)</li>
     * <li>akp[3] = mean kp of last 24 hours</li>
     * <li>akp[4] = 0 (not used)</li>
     * </ul>
     */
    protected final double[] akp = new double[5];

    /** Geodetic altitude in km (minimum altitude: 120 km). */
    protected double cachedAlti;

    /** Local solar time (rad). */
    protected double cachedHl;

    /** Geodetic Latitude (rad). */
    protected double alat;

    /** Geodetic longitude (rad). */
    protected double xlon;

    /** Cache mecanism - Temperature at altitude z (K). */
    @SuppressWarnings("PMD.AvoidUsingVolatile")
    protected volatile double cachedTemperature;

    /** Exospheric temperature. */
    @SuppressWarnings("PMD.AvoidUsingVolatile")
    protected volatile double tinf;

    /** Total density (g/cm3). */
    protected double ro;

    /**
     * Number of particles per cm3. cc(1) = hydrogen cc(2) = helium cc(3) = atomic oxygen cc(4) = molecular nitrogen
     * cc(5) = molecular oxygen cc(6) = atomic nitrogen
     */
    protected final double[] cc = new double[7];

    /** Legendre coefficient. */
    protected double p10;

    /** Legendre coefficient. */
    protected double p20;

    /** Legendre coefficient. */
    protected double p30;

    /** Legendre coefficient. */
    protected double p40;

    /** Legendre coefficient. */
    protected double p50;

    /** Legendre coefficient. */
    protected double p60;

    /** Legendre coefficient. */
    protected double p11;

    /** Legendre coefficient. */
    protected double p21;

    /** Legendre coefficient. */
    protected double p31;

    /** Legendre coefficient. */
    protected double p41;

    /** Legendre coefficient. */
    protected double p51;

    /** Legendre coefficient. */
    protected double p22;

    /** Legendre coefficient. */
    protected double p32;

    /** Legendre coefficient. */
    protected double p42;

    /** Legendre coefficient. */
    protected double p52;

    /** Legendre coefficient. */
    protected double p62;

    /** Legendre coefficient. */
    protected double p33;

    /** Legendre coefficient. */
    protected double p10mg;

    /** Legendre coefficient. */
    protected double p20mg;

    /** Legendre coefficient. */
    protected double p40mg;

    /** Intermediate values. */
    protected double ch;

    /** Intermediate values. */
    protected double sh;

    /** Intermediate values. */
    protected double c2h;

    /** Intermediate values. */
    protected double s2h;

    /** Intermediate values. */
    protected double c3h;

    /** Intermediate values. */
    protected double s3h;

    /** Sun position. */
    protected final PVCoordinatesProvider sun;

    /** External data container. */
    protected final DTMInputParameters inputParams;

    /** Earth body shape. */
    protected final EllipsoidBodyShape earth;

    /** Cache mecanism - Output atmosphere data. */
    private AtmosphereData cachedOutputData;

    /** Cache mecanism - Input date. */
    private AbsoluteDate cachedDate;

    /** Cache mecanism - Input frame. */
    private Frame cachedFrame;

    /** Cache mecanism - Input position. */
    private Vector3D cachedPosition;

    /** Resources text file. */
    private final String dataFileAtmosphericModel;

    /**
     * Simple constructor for independent computation.
     *
     * @param parameters
     *        the solar and magnetic activity data
     * @param sunIn
     *        the sun position
     * @param earthIn
     *        the earth body shape
     * @param dataFile
     *        the path to the file that contains the data of the model
     * @throws PatriusException
     *         if some resource file reading error occurs
     */
    public AbstractDTM(final DTMInputParameters parameters, final PVCoordinatesProvider sunIn,
                       final EllipsoidBodyShape earthIn, final String dataFile)
        throws PatriusException {

        this.earth = earthIn;
        this.sun = sunIn;
        this.inputParams = parameters;
        this.cachedDate = AbsoluteDate.PAST_INFINITY;
        this.cachedFrame = null;
        this.cachedPosition = Vector3D.ZERO;
        this.cachedOutputData = null;
        this.dataFileAtmosphericModel = dataFile;
        if (this.tt == null) {
            readcoefficients();
        }
    }

    /**
     * Load resource file.
     *
     * @return loaded resource file
     * @throws PatriusException
     *         if file not found
     */
    private InputStream loadRessource() throws PatriusException {
        // Load file (try with resource)
        InputStream in = AbstractDTM.class.getResourceAsStream(this.dataFileAtmosphericModel);
        if (in == null) {
            // Try with file
            try {
                in = new FileInputStream(this.dataFileAtmosphericModel);
            } catch (final IOException e) {
                // File not found
                throw new PatriusException(PatriusMessages.UNABLE_TO_FIND_RESOURCE, this.dataFileAtmosphericModel, e);
            }
        }
        return in;
    }

    /**
     * Getter for number of lines in data file.
     *
     * @return number of lines in data file
     * @throws PatriusException
     *         if file not found
     */
    private int getLinesNumber() throws PatriusException {
        // Get number of lines in data file
        final InputStream in = loadRessource();
        int numberLinesAtmosphericModel = 0;
        try {
            final InputStreamReader input = new InputStreamReader(in, CHARSET);
            final LineNumberReader reader = new LineNumberReader(input);
            while (reader.readLine() != null) {
                numberLinesAtmosphericModel++;
            }
            numberLinesAtmosphericModel = reader.getLineNumber() + 1;
            // Close readers
            reader.close();
            input.close();
        } catch (final IOException e) {
            // Should not happen
            throw new PatriusException(PatriusMessages.UNABLE_TO_FIND_RESOURCE, this.dataFileAtmosphericModel, e);
        }
        return numberLinesAtmosphericModel;
    }

    /**
     * Store the DTM model elements coefficients in internal arrays.
     *
     * @throws PatriusException
     *         if some resource file reading error occurs
     */
    @SuppressWarnings("PMD.DoNotThrowExceptionInFinally")
    private void readcoefficients() throws PatriusException {
        // Read coefficients in DTM file

        // Initialization
        final int size = getLinesNumber() + 1;
        this.tt = new double[size];
        this.h = new double[size];
        this.he = new double[size];
        this.o = new double[size];
        this.az2 = new double[size];
        this.o2 = new double[size];
        this.az = new double[size];
        this.t0 = new double[size];
        this.tp = new double[size];

        // partial derivative array initialization
        this.dtt = new double[size];
        this.dh = new double[size];
        this.dhe = new double[size];
        this.dox = new double[size];
        this.daz2 = new double[size];
        this.do2 = new double[size];
        this.daz = new double[size];
        this.dt0 = new double[size];
        this.dtp = new double[size];

        // Read data
        final InputStream in = loadRessource();
        BufferedReader r = null;
        try {
            r = new BufferedReader(new InputStreamReader(in, CHARSET));
            // Header
            r.readLine();
            r.readLine();
            int lineIndex = 1;

            // Data
            for (String line = r.readLine(); line != null; line = r.readLine()) {

                // Temporary variables
                final int num = lineIndex;
                String line2;
                lineIndex++;

                // Read each value:
                // - value
                // - dvalue
                line2 = line.substring(4);
                this.tt[num] = Double.parseDouble(line2.substring(0, 13).replace(' ', '0'));
                this.dtt[num] = Double.parseDouble(line2.substring(13, 13 + 9).replace(' ', '0'));

                line2 = line2.substring(13 + 9);
                this.h[num] = Double.parseDouble(line2.substring(0, 13).replace(' ', '0'));
                this.dh[num] = Double.parseDouble(line2.substring(13, 13 + 9).replace(' ', '0'));

                line2 = line2.substring(13 + 9);
                this.he[num] = Double.parseDouble(line2.substring(0, 13).replace(' ', '0'));
                this.dhe[num] = Double.parseDouble(line2.substring(13, 13 + 9).replace(' ', '0'));

                line2 = line2.substring(13 + 9);
                this.o[num] = Double.parseDouble(line2.substring(0, 13).replace(' ', '0'));
                this.dox[num] = Double.parseDouble(line2.substring(13, 13 + 9).replace(' ', '0'));

                line2 = line2.substring(13 + 9);
                this.az2[num] = Double.parseDouble(line2.substring(0, 13).replace(' ', '0'));
                this.daz2[num] = Double.parseDouble(line2.substring(13, 13 + 9).replace(' ', '0'));

                line2 = line2.substring(13 + 9);
                this.o2[num] = Double.parseDouble(line2.substring(0, 13).replace(' ', '0'));
                this.do2[num] = Double.parseDouble(line2.substring(13, 13 + 9).replace(' ', '0'));

                line2 = line2.substring(13 + 9);
                this.az[num] = Double.parseDouble(line2.substring(0, 13).replace(' ', '0'));
                this.daz[num] = Double.parseDouble(line2.substring(13, 13 + 9).replace(' ', '0'));

                line2 = line2.substring(13 + 9);
                this.t0[num] = Double.parseDouble(line2.substring(0, 13).replace(' ', '0'));
                this.dt0[num] = Double.parseDouble(line2.substring(13, 13 + 9).replace(' ', '0'));

                line2 = line2.substring(13 + 9);
                this.tp[num] = Double.parseDouble(line2.substring(0, 13).replace(' ', '0'));
                this.dtp[num] = Double.parseDouble(line2.substring(13, 13 + 9).replace(' ', '0'));
            }

        } catch (final IOException ioe) {
            // Exception
            throw new PatriusException(ioe, new DummyLocalizable(ioe.getMessage()));
        } finally {
            // Finally: close stream
            if (r != null) {
                try {
                    r.close();
                } catch (final IOException ioe) {
                    // Exception
                    throw new PatriusRuntimeException(ioe.getMessage(), ioe);
                }
            }
        }
        // No result to return
        // Class variables directly modified
    }

    /** Initialize the partial derivative arrays. */
    protected void initderivatives() {

        // Fill arrays with NaN
        // Arrays will be filled during computation
        Arrays.fill(this.dtt, Double.NaN);
        Arrays.fill(this.dh, Double.NaN);
        Arrays.fill(this.dhe, Double.NaN);
        Arrays.fill(this.dox, Double.NaN);
        Arrays.fill(this.daz2, Double.NaN);
        Arrays.fill(this.do2, Double.NaN);
        Arrays.fill(this.daz, Double.NaN);
        Arrays.fill(this.dt0, Double.NaN);
        Arrays.fill(this.dtp, Double.NaN);
    }

    /**
     * Compute Legendre polynomials wrt geographic pole.
     *
     * @param c
     *        sinus latitude
     * @param s
     *        cosinus latitude
     */
    protected void initializeLegendreCoefficients(final double c, final double s) {

        // square c
        final double c2 = c * c;
        // c to the fourth power
        final double c4 = c2 * c2;
        // square s
        final double s2 = s * s;
        // Legendre Coefficients
        this.p10 = c;
        this.p20 = 1.5 * c2 - 0.5;
        this.p30 = c * (2.5 * c2 - 1.5);
        this.p40 = 4.375 * c4 - 3.75 * c2 + 0.375;
        this.p50 = c * (7.875 * c4 - 8.75 * c2 + 1.875);
        this.p60 = (5.5 * c * this.p50 - 2.5 * this.p40) / 3.0;
        this.p11 = s;
        this.p21 = 3.0 * c * s;
        this.p31 = s * (7.5 * c2 - 1.5);
        this.p41 = c * s * (17.5 * c2 - 7.5);
        this.p51 = s * (39.375 * c4 - 26.25 * c2 + 1.875);
        this.p22 = 3.0 * s2;
        this.p32 = 15.0 * c * s2;
        this.p42 = s2 * (52.5 * c2 - 7.5);
        this.p52 = 3.0 * c * this.p42 - 2.0 * this.p32;
        this.p62 = 2.75 * c * this.p52 - 1.75 * this.p42;
        this.p33 = 15.0 * s * s2;
        // No result to return
        // Class variables modified directly
    }

    /**
     * Compute Legendre polynomials coefficients with respect to geographic and magnetic poles.
     */
    protected void computeGeographicMagneticCoefficients() {

        final double[] sincos = MathLib.sinAndCos(this.alat);
        final double c = sincos[0];
        final double s = sincos[1];

        // compute Legendre polynomials wrt geographic pole
        // Coefficients of polynomials
        initializeLegendreCoefficients(c, s);

        // compute Legendre polynomials wrt magnetic pole
        // (79N, 71W)
        final double clmlmg = MathLib.cos(this.xlon - XLMG);
        final double cmg = s * CPMG * clmlmg + c * SPMG;
        final double cmg2 = cmg * cmg;
        final double cmg4 = cmg2 * cmg2;
        this.p10mg = cmg;
        this.p20mg = 1.5 * cmg2 - 0.5;
        this.p40mg = 4.375 * cmg4 - 3.75 * cmg2 + 0.375;

        // local time
        final double hl0 = this.cachedHl;
        final double[] sincosh10 = MathLib.sinAndCos(hl0);
        this.sh = sincosh10[0];
        this.ch = sincosh10[1];
        // Optimisations for computation speed-up
        this.c2h = this.ch * this.ch - this.sh * this.sh;
        this.s2h = 2.0 * this.ch * this.sh;
        this.c3h = this.c2h * this.ch - this.s2h * this.sh;
        this.s3h = this.s2h * this.ch + this.c2h * this.sh;
    }

    /**
     * Getter for the local density.
     *
     * @param date
     *        current date
     * @param position
     *        current position in frame
     * @param frame
     *        the frame in which is defined the position
     * @return local density (kg/m<sup>3</sup>)
     * @throws PatriusException
     *         if date is out of range of solar activity model or if some frame conversion cannot be performed
     */
    @Override
    public double getDensity(final AbsoluteDate date, final Vector3D position, final Frame frame)
        throws PatriusException {
        computeTempDensity(date, position, frame);
        return this.cachedOutputData.getDensity();
    }

    /**
     * If input parameters are different from cached parameters, re compute cached density and temperature.
     *
     * @param date
     *        current date
     * @param position
     *        current position in frame
     * @param frame
     *        the frame in which is defined the position
     * @throws PatriusException
     *         if date is out of range of solar activity model
     *         if some frame conversion cannot be performed
     */
    private void computeTempDensity(final AbsoluteDate date, final Vector3D position, final Frame frame)
        throws PatriusException {
        if (this.cachedDate.compareTo(date) != 0 || position.distance(this.cachedPosition) != 0
                || !frame.equals(this.cachedFrame)) {
            final double iflux;
            final double mflux;
            final double thkp;
            final double tfkp;
            // Thread-safe reading of the not guaranteed thread-safe inputParams object
            synchronized (this.inputParams) {
                final AbsoluteDate iMaxDate = this.inputParams.getMaxDate();
                final AbsoluteDate iMinDate = this.inputParams.getMinDate();

                // check if data are available :
                if (date.compareTo(iMaxDate) > 0 || date.compareTo(iMinDate) < 0) {
                    throw new PatriusException(PatriusMessages.NO_SOLAR_ACTIVITY_AT_DATE, date, iMinDate, iMaxDate);
                }

                iflux = this.inputParams.getInstantFlux(date);
                mflux = this.inputParams.getMeanFlux(date);
                thkp = this.inputParams.getThreeHourlyKP(date);
                tfkp = this.inputParams.get24HoursKp(date);
            }

            // compute day number in current year
            final DateTimeComponents components = date.getComponents(TimeScalesFactory.getTAI());

            final int day = components.getDate().getDayOfYear();
            final int hour = components.getTime().getHour();
            final int min = components.getTime().getMinute();
            final int sec = (int) components.getTime().getSecond();
            final int msec = (int) ((components.getTime().getSecond() - sec) * 1000);

            // compute the correct parameter day number in current year
            // (float value instead of an integer)
            final double ndays = day + (hour * 3600 + min * 60 + sec + msec * 1E-3) / 86400 - 1;
            // compute geodetic position
            final EllipsoidPoint inBody = this.earth.buildPoint(position, frame, date, BodyPointName.DEFAULT);
            final double lat = inBody.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC).getLatitude();
            final double lon = inBody.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC).getLongitude();
            final double alt = inBody.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC).getHeight();

            // compute local solar time (computed in the CIRF frame)
            final Vector3D sunPos = this.sun.getPVCoordinates(date, FramesFactory.getCIRF()).getPosition();
            final Vector3D pos = frame.getTransformTo(FramesFactory.getCIRF(), date).transformPosition(position);
            final double hl = FastMath.PI + MathLib.atan2(sunPos.getX() * pos.getY() - sunPos.getY() * pos.getX(),
                sunPos.getX() * pos.getX() + sunPos.getY() * pos.getY());

            // get current solar activity data and compute
            final double density = this.getDensity(ndays, alt, lon, lat, hl, iflux, mflux, thkp, tfkp);

            // Build atmospheric data
            // Argon and anomalous oxygen not provided by the model
            this.cachedOutputData = new AtmosphereData(density, this.cachedTemperature, this.tinf, this.cc[2],
                this.cc[3], this.cc[4],
                this.cc[5], 0., this.cc[1], this.cc[6], 0.);

            // store input params used to compute these results in cache
            this.cachedDate = date;
            this.cachedPosition = position;
            this.cachedFrame = frame;
        }
    }

    /**
     * Getter for the local density with initial entries.
     *
     * @param day
     *        day of year
     * @param alti
     *        altitude in meters
     * @param lon
     *        local longitude (rad)
     * @param lat
     *        local latitude (rad)
     * @param hl
     *        local solar time in rad (O hr = 0 rad)
     * @param f
     *        instantaneous solar flux (F10.7)
     * @param fbar
     *        mean solar flux (F10.7)
     * @param akp3
     *        3 hrs geomagnetic activity index (1-9)
     * @param akp24
     *        Mean of last 24 hrs geomagnetic activity index (1-9)
     * @return the local density (kg/m<sup>3</sup>)
     * @throws PatriusException
     *         if altitude is outside of supported range
     */
    private double getDensity(final double day, final double alti, final double lon, final double lat, final double hl,
                              final double f, final double fbar, final double akp3, final double akp24)
        throws PatriusException {

        final double threshold = 120000;
        if (alti < threshold) {
            // Altitude threshold
            // DTM does not work under 120km
            throw new PatriusException(PatriusMessages.ALTITUDE_BELOW_ALLOWED_THRESHOLD, alti, threshold);
        }
        // Lock that keeps the internal state
        // consistent during computation
        synchronized (this) {
            this.cachedDay = day;
            this.cachedAlti = alti / 1000;
            this.xlon = lon;
            this.alat = lat;
            this.cachedHl = hl;
            this.cachedF[1] = f;
            this.cachedFbar[1] = fbar;
            this.akp[1] = akp3;
            this.akp[3] = akp24;
            densityComputationFromFortran();
            return this.ro * 1000;
        }
    }

    /**
     * This function performs the actual density compuation once the inputs values are saved into the global caches.
     * This method directly modifies class variables </br>
     * - ro </br>
     * - cc </br>
     * - d </br>
     * - cachedTemperature </br>
     * Hence no result is returned
     */
    protected abstract void densityComputationFromFortran();

    /**
     * Computation of function G.
     *
     * @param a
     *        vector of coefficients for computation
     * @param da
     *        vector of partial derivatives
     * @param ff0
     *        coefficient flag (1 for Ox, Az, He, T°; 0 for H and tp120)
     * @param kleEq
     *        season indicator flag (summer, winter, equinox)
     * @return value of G
     */
    protected abstract double gFunction(final double[] a, final double[] da, final int ff0, final int kleEq);

    /**
     * Getter for the inertial velocity of atmosphere molecules. Here the case is simplified : atmosphere is supposed to
     * have a null velocity in earth frame.
     *
     * @param date
     *        current date
     * @param position
     *        current position in frame
     * @param frame
     *        the frame in which is defined the position
     * @return velocity (m/s) (defined in the same frame as the position)
     * @throws PatriusException
     *         if some frame conversion cannot be performed
     */
    @Override
    public Vector3D getVelocity(final AbsoluteDate date, final Vector3D position, final Frame frame)
        throws PatriusException {

        final Transform bodyToFrame = this.earth.getBodyFrame().getTransformTo(frame, date);
        final Vector3D posInBody = bodyToFrame.getInverse().transformPosition(position);
        final PVCoordinates pvBody = new PVCoordinates(posInBody, new Vector3D(0, 0, 0));
        final PVCoordinates pvFrame = bodyToFrame.transformPVCoordinates(pvBody);
        return pvFrame.getVelocity();
    }

    /** {@inheritDoc} */
    @Override
    public double getSpeedOfSound(final AbsoluteDate date, final Vector3D position, final Frame frame)
        throws PatriusException {
        computeTempDensity(date, position, frame);
        return MathLib.sqrt(GAMMA * 287.058 * this.cachedTemperature);
    }

    /**
     * {@inheritDoc}
     * <p>
     * DTM provides all data mentioned in {@link AtmosphereData} except partial density of Argon and anomalous oxygen.
     * </p>
     */
    @Override
    public AtmosphereData getData(final AbsoluteDate date, final Vector3D position, final Frame frame)
        throws PatriusException {
        computeTempDensity(date, position, frame);
        return this.cachedOutputData;
    }

    /**
     * Getter for the solar and magnetic activity data.
     *
     * @return the solar and magnetic activity data
     */
    public DTMInputParameters getParameters() {
        return this.inputParams;
    }

    /**
     * Getter for the sun position.
     *
     * @return the the sun position
     */
    public PVCoordinatesProvider getSun() {
        return this.sun;
    }

    /**
     * Getter for the earth body shape.
     *
     * @return the the earth body shape
     */
    public BodyShape getEarth() {
        return this.earth;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The following attributes are not deeply copied:
     * <ul>
     * <li>inputParams: {@link DTMInputParameters}</li>
     * <li>sun: {@link PVCoordinatesProvider}</li>
     * <li>earth: {@link BodyShape}</li>
     * </ul>
     * </p>
     */
    @Override
    public abstract Atmosphere copy();

    /** {@inheritDoc} */
    @Override
    public void checkSolarActivityData(final AbsoluteDate start, final AbsoluteDate end) throws PatriusException {
        this.inputParams.checkSolarActivityData(start, end);
    }
    // CHECKSTYLE: resume MagicNumber check
}
