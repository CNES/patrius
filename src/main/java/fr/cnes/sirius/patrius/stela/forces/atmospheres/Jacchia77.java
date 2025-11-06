package fr.cnes.sirius.patrius.stela.forces.atmospheres;

import fr.cnes.sirius.patrius.bodies.EllipsoidBodyShape;
import fr.cnes.sirius.patrius.bodies.EllipsoidPoint;
import fr.cnes.sirius.patrius.bodies.LLHCoordinatesSystem;
import fr.cnes.sirius.patrius.forces.atmospheres.Atmosphere;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.stela.JavaMathAdapter;
import fr.cnes.sirius.patrius.stela.bodies.EarthRotation;
import fr.cnes.sirius.patrius.stela.forces.solaractivity.IStelaSolarActivity;
import fr.cnes.sirius.patrius.stela.forces.solaractivity.variable.StelaVariableSolarActivity;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * <p>
 * This class implements the Jaccia77 atmospheric model.
 * </p>
 * <p>
 * This class is restricted to be used with {@link EllipsoidBodyShape}.
 * </p>
 * 
 * @concurrency thread-hostile
 * @concurrency.comment This class uses a {@link Jacchia77Data} which can read data through a public access
 *                      (initTempWmMaps()) which is not synchronized (thread lock) at the moment.
 * 
 * @author Thomas Rodrigues, Thibaut BONIT
 * HISTORY
 * VERSION:4.16:OPENFD-389:25/04/2025:[STELA-PATRIUS] Activites solaires additionnelles
 * VERSION:4.16:OPENFD-390:25/04/2025:[STELA-PATRIUS] Modeles d'atmosphere additionnels
 * END-HISTORY
 * @since 4.16
 */
public class Jacchia77 implements Atmosphere {

    /** Serializable UID. */
    private static final long serialVersionUID = -3677649897997390625L;

    /** Geomagnetic north pole latitude. */
    private static final double GEO_NORH_LAT = MathLib.toRadians(78.3);
    /** Geomagnetic north pole longitude. */
    private static final double GEO_NORH_LONG = MathLib.toRadians(291);
    /** Sin of geomagnetic north pole latitude. */
    private static final double SIN_GEO_NORH_LAT = MathLib.sin(GEO_NORH_LAT);
    /** Cosine of geomagnetic north longitude. */
    private static final double COS_GEO_NORH_LAT = MathLib.cos(GEO_NORH_LAT);

    /** Numerical constants used for density computation. */
    private static final double ONE = 1.;
    /** Numerical constants used for density computation. */
    private static final double HALF = 0.5;
    /** Numerical constants used for density computation. */
    private static final int TEN = 10;
    /** Numerical constants used for density computation. */
    private static final int TWO = 2;
    /** Numerical constants used for density computation. */
    private static final double C_1 = 0.1;
    /** Numerical constants used for density computation. */
    private static final double C_2 = 0.2;
    /** Numerical constants used for density computation. */
    private static final double C_3 = 1.26;
    /** Numerical constants used for density computation. */
    private static final double C_4 = 0.37;
    /** Numerical constants used for density computation. */
    private static final double C_5 = MathLib.toRadians(92);
    /** Numerical constants used for density computation. */
    private static final double C_6 = 5.48;
    /** Numerical constants used for density computation. */
    private static final double C_7 = 0.8;
    /** Numerical constants used for density computation. */
    private static final double C_8 = 101.8;
    /** Numerical constants used for density computation. */
    private static final double C_9 = 0.4;
    /** Numerical constants used for density computation. */
    private static final double C_10 = 12.0;
    /** Numerical constants used for density computation. */
    private static final double C_11 = 0.15;
    /** Numerical constants used for density computation. */
    private static final double C_12 = 0.24;
    /** Numerical constants used for density computation. */
    private static final double C_13 = 0.08;
    /** Numerical constants used for density computation. */
    private static final double C_14 = 57.5;
    /** Numerical constants used for density computation. */
    private static final double C_15 = 0.027;
    /** Numerical constants used for density computation. */
    private static final double C_16 = 0.006;
    /** Numerical constants used for density computation. */
    private static final double C_17 = 0.05;
    /** Numerical constants used for density computation. */
    private static final double C_18 = 0.04;
    /** Numerical constants used for density computation. */
    private static final double C_19 = 100;
    /** Numerical constants used for density computation. */
    private static final double C_20 = 0.25;
    /** Numerical constants used for density computation. */
    private static final double C_21 = 0.0954;
    /** Numerical constants used for density computation. */
    private static final double C_22 = 6.04;
    /** Numerical constants used for density computation. */
    private static final double C_23 = 1.65;
    /** Numerical constants used for density computation. */
    private static final double C_24 = 0.0284;
    /** Numerical constants used for density computation. */
    private static final double C_25 = 0.382;
    /** Numerical constants used for density computation. */
    private static final double C_26 = 0.467;
    /** Numerical constants used for density computation. */
    private static final double C_27 = 4.14;
    /** Numerical constants used for density computation. */
    private static final double C_28 = 4.26;

    /** Physical constants. */
    private static final double ECLIP = MathLib.toRadians(23.44);
    /** Physical constants. */
    private static final double BETA = MathLib.toRadians(-60);
    /** Physical constants. */
    private static final double KI = MathLib.toRadians(-75);
    /** Physical constants. */
    private static final double Z0 = 90.;

    /** Modulo for local solar time interval. */
    private static final double LST_MOD_1 = 3.66;
    /** Modulo for local solar time interval. */
    private static final double LST_MOD_2 = 24.0;

    /** Hours in one day. */
    private static final double HOURS_IN_DAY = 24;
    /** Adiabatic constant. */
    private static final double GAMMA = 1.4;
    /** Specific gas constant for air J*kg<sup>-1</sup>*K<sup>-1</sup>. */
    private static final double R = 287.058;
    /** 71 days. */
    private static final int TAU = 71;
    /** Duration of a tropical year (days) */
    private static final double TROPICAL_DUR = 365.2422;

    /** Weights for mean flux computation. */
    private final double[] weights = new double[6 * TAU + 1];

    /** Atmospheric model data and functions. */
    private final Jacchia77Data jacchia77Data;

    /** Solar activity. */
    private final IStelaSolarActivity solarActivity;

    /** Earth Shape. */
    private final EllipsoidBodyShape earth;

    /** Sun. */
    private final PVCoordinatesProvider sun;

    /** Cache mechanism - Input date. */
    private AbsoluteDate cachedDate;

    /** Cache mechanism - Input position. */
    private Vector3D cachedPosition;

    /** Cache mechanism - Input frame. */
    private Frame cachedFrame;

    /** Cache mechanism - Input flag. */
    private boolean cachedComputeDensityOnly;

    /** Cache mechanism - Output atmosphere data. */
    private JacchiaOutput cachedOutputData;

    /**
     * Simple constructor to build an instance of the class.
     * 
     * @param solarActivity
     *        Solar activity
     * @param earthBody
     *        Earth body
     * @param sun
     *        Sun
     */
    public Jacchia77(final IStelaSolarActivity solarActivity, final EllipsoidBodyShape earthBody,
                     final PVCoordinatesProvider sun) {

        // Store the inputs
        this.solarActivity = solarActivity;
        this.earth = earthBody;
        this.sun = sun;

        // Initialize the atmospheric model data and functions (density map)
        this.jacchia77Data = new Jacchia77Data();

        // Cache initialization
        this.cachedDate = AbsoluteDate.PAST_INFINITY;
        this.cachedPosition = Vector3D.NaN;
        this.cachedFrame = null;
        this.cachedComputeDensityOnly = false;
        this.cachedOutputData = null;

        // Precompute weights for mean flux computation
        for (int i = -3 * TAU; i <= 3 * TAU; i += 1) {
            this.weights[i + 3 * TAU] = MathLib.exp(-((double) i * i / (TAU * TAU)));
        }
    }

    /**
     * Getter for the atmospheric model output container.
     * <p>
     * Note: a cache mechanism avoid recomputation of the output container if it should be recomputed a second time with
     * the same inputs.
     * </p>
     * 
     * @param date
     *        current date
     * @param position
     *        current position in frame
     * @param frame
     *        the frame in which is defined the position
     * @return the atmospheric model output container
     * @throws PatriusException
     *         if a Patrius error occurs
     */
    public JacchiaOutput getData(final AbsoluteDate date, final Vector3D position, final Frame frame)
        throws PatriusException {
        // Compute the data container
        computeData(date, position, frame, false);
        return this.cachedOutputData;
    }

    /**
     * Getter for the density at the local point.
     * <p>
     * The default behavior of this method is to only compute the density (not the temperature nor the mean molar
     * mass).<br>
     * Note: a cache mechanism avoid recomputation of this parameter if it should be recomputed a second time with
     * the same inputs.
     * </p>
     * 
     * @param date
     *        current date
     * @param position
     *        current position in frame
     * @param frame
     *        the frame in which is defined the position
     * @return the local density (kg/m<sup>3</sup>)
     * @throws PatriusException
     *         if a Patrius error occurs
     */
    @Override
    public double getDensity(final AbsoluteDate date, final Vector3D position, final Frame frame)
        throws PatriusException {
        // Compute the data container with only the density
        computeData(date, position, frame, true);
        return this.cachedOutputData.getDensity();
    }

    /**
     * Getter for the temperature at the local point.
     * <p>
     * The default behavior of this method is to compute the temperature as well as the density and the mean molar
     * mass.<br>
     * Note: a cache mechanism avoid recomputation of these parameters if they should be recomputed a second time with
     * the same inputs.
     * </p>
     * 
     * @param date
     *        current date
     * @param position
     *        current position in frame
     * @param frame
     *        the frame in which is defined the position
     * @return the temperature (K)
     * @throws PatriusException
     *         if a Patrius error occurs
     */
    public double getTemperature(final AbsoluteDate date, final Vector3D position, final Frame frame)
        throws PatriusException {
        // Compute the data container with the temperature
        computeData(date, position, frame, false);
        return this.cachedOutputData.getTemperature();
    }

    /**
     * Getter for the mean molar mass at the local point.
     * <p>
     * The default behavior of this method is to compute the mean molar mass as well as the density and the
     * temperature.<br>
     * Note: a cache mechanism avoid recomputation of these parameters if they should be recomputed a second time with
     * the same inputs.
     * </p>
     * 
     * @param date
     *        current date
     * @param position
     *        current position in frame
     * @param frame
     *        the frame in which is defined the position
     * @return the mean molar mass (kg/mol)
     * @throws PatriusException
     *         if a Patrius error occurs
     */
    public double getMeanMolarMass(final AbsoluteDate date, final Vector3D position, final Frame frame)
        throws PatriusException {
        // Compute the data container with the mean molar mass
        computeData(date, position, frame, false);
        return this.cachedOutputData.getMeanMolarMass();
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getVelocity(final AbsoluteDate date, final Vector3D position, final Frame frame)
        throws PatriusException {

        final Transform bodyToFrame = this.earth.getBodyFrame().getTransformTo(frame, date);
        final Vector3D posInBody = bodyToFrame.getInverse().transformPosition(position);
        final PVCoordinates pvBody = new PVCoordinates(posInBody, Vector3D.ZERO);
        final PVCoordinates pvFrame = bodyToFrame.transformPVCoordinates(pvBody);
        return pvFrame.getVelocity();
    }

    /** {@inheritDoc} */
    @Override
    public double getSpeedOfSound(final AbsoluteDate date, final Vector3D position, final Frame frame)
        throws PatriusException {
        return MathLib.sqrt(GAMMA * R * getTemperature(date, position, frame));
    }

    /**
     * Getter for the solar activity.
     *
     * @return the solar activity
     */
    public IStelaSolarActivity getSolarActivity() {
        return this.solarActivity;
    }

    /**
     * Getter for the earth body.
     *
     * @return the earth body
     */
    public EllipsoidBodyShape getEarthBody() {
        return this.earth;
    }

    /**
     * Getter for the sun.
     *
     * @return the sun
     */
    public PVCoordinatesProvider getSun() {
        return this.sun;
    }

    /** {@inheritDoc} */
    @Override
    public Atmosphere copy() {
        return new Jacchia77(this.solarActivity, this.earth, this.sun);
    }

    /** {@inheritDoc} */
    @Override
    public void checkSolarActivityData(final AbsoluteDate start, final AbsoluteDate end) throws PatriusException {
        // Nothing to do
    }

    /**
     * Compute the atmospheric density, temperature and mean molar mass at the local point.
     * <p>
     * Note: a cache mechanism avoid recomputation of these parameters if they should be recomputed a second time with
     * the same inputs.
     * </p>
     * 
     * @param date
     *        current date
     * @param position
     *        current position in frame
     * @param frame
     *        the frame in which is defined the position
     * @param computeDensityOnly
     *        if {@code true} only the density should be computed (no computation of temperature and mean molar mass),
     *        otherwise the 3 parameters should be computed
     * @throws PatriusException
     *         if a Patrius error occurs
     */
    private void computeData(final AbsoluteDate date, final Vector3D position, final Frame frame,
                             final boolean computeDensityOnly)
        throws PatriusException {

        // Check the inputs cached values to see if the cachedDataOutput should be recomputed, otherwise nothing to do
        if (!this.cachedDate.equals(date.getDate()) || !this.cachedPosition.equals(position)
                || !this.cachedFrame.equals(frame) || this.cachedComputeDensityOnly != computeDensityOnly) {

            // Compute longitude, latitude, local solar time
            final EllipsoidPoint point = this.earth.buildPoint(position, frame, date, "satPoint");

            final double latitude = point.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC).getLatitude();

            // Note: to compute the longitude below, we combine the EllipsoidPoint#getLLHCoordinates method and
            // GeodPosition#getGeodeticLongitude method to be able to work with EllipsoidBodyShape earth model
            // (due to the fact that GeodPosition requires data which can only be provided by a OneAxisEllipsoid earth
            // representation which is more restrictive than an EllipsoidBodyShape)

            // Compute thetaLST
            final double thetaLST = point.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC).getLongitude();
            // Compute thetaGMST
            final double thetaERA = EarthRotation.getERA(date);
            // Compute longitude
            double longitude = JavaMathAdapter.mod(thetaLST - thetaERA, MathUtils.TWO_PI);
            if (longitude > MathLib.PI) {
                // Geodetic longitude has to be within ]-PI; PI]
                longitude = longitude - MathUtils.TWO_PI;
            }

            // Altitude is bounded in [0, max value in ALT_TABLE]
            double altitude = MathLib.max(0., point.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC).getHeight());

            // Density initialization
            double density = 0.;
            boolean computeDensity = true;
            final double temperature;
            final double meanMolarMass;

            // If the altitude is over the maximum altitude defined in ALT_TABLE,
            // altitude = maxAltitude & density = 0 (no need to compute it)
            if (altitude > Jacchia77Data.ALT_TABLE[Jacchia77Data.ALT_TABLE.length - 1]) {
                altitude = Jacchia77Data.ALT_TABLE[Jacchia77Data.ALT_TABLE.length - 1];
                computeDensity = false;
            }

            // Special case: the altitude is over the maximum (computeDensity = false)
            // and only the density has to be computed (computeDensityOnly = true)
            // Density is already 0 and no further computation is required
            if (!computeDensity && computeDensityOnly) {
                temperature = Double.NaN;
                meanMolarMass = Double.NaN;
            } else {

                // Normal case : the density and/or the temperature and mean molar mass have to be computed

                // Compute some constants
                final double sinLat = MathLib.sin(latitude);
                final double cosLat = MathLib.cos(latitude);
                final double sinPhi =
                    SIN_GEO_NORH_LAT * sinLat + COS_GEO_NORH_LAT * cosLat * MathLib.cos(longitude - GEO_NORH_LONG);
                final double sinPhi2 = sinPhi * sinPhi;
                final double dtAp = C_1 + C_2 * (ONE - sinPhi2);

                // Get Ap value at date t = t0 - deltaT
                final double ap = this.solarActivity.getAp(date.shiftedBy(-dtAp));

                // Compute Kp converting Ap with available conversion table
                final double kp = MathLib.min(Jacchia77Data.AP_FUNCTION.value(ap),
                    Jacchia77Data.AP_KP_TABLE[0][Jacchia77Data.AP_KP_TABLE[0].length - 1]);

                // Compute instant and mean flux F10.7
                final double raSat = MathLib.atan2(position.getY(), position.getX());
                final Vector3D positionSun = this.sun.getPVCoordinates(date, frame).getPosition();
                final double[] raDecSun = getRaDec(positionSun.normalize());
                final double dtFlux = C_3 + C_4 * MathLib.sin((raSat - raDecSun[0]) - C_5);

                final AbsoluteDate fluxDate = date.shiftedBy(-dtFlux);
                final double instantFlux = this.solarActivity.getInstantFluxValue(fluxDate);
                final double meanFlux = computeMeanFlux(fluxDate);

                // Compute the local solar time
                final Transform frameToCMOD = frame.getTransformTo(FramesFactory.getMOD(false), date);
                final Vector3D cmodPosition = frameToCMOD.transformPosition(position);
                final double tLoc = computeTLoc(date, cmodPosition);

                // Multiplicative factor due to dayly "bulge"
                final double lst2;
                if (tLoc > LST_MOD_1) {
                    lst2 = tLoc;
                } else {
                    lst2 = tLoc + LST_MOD_2;
                }
                final double value = MathLib.PI / C_10 * (lst2 - C_10);
                final double fValue = MathLib.pow(MathLib.cos((value + BETA) / 2), 3) + C_13
                        * MathLib.cos(3 * (value + BETA) + KI);
                final double factor = ONE + C_11 * (raDecSun[1] / ECLIP) * sinLat + C_12 * cosLat * (fValue - HALF);

                // Compute a temperature variation due to geomagnetic activity
                final double deltaTinf = C_14 * kp * (ONE + C_15 * MathLib.exp(C_9 * kp)) * sinPhi2 * sinPhi2;
                final double deltaTg = deltaTinf * MathLib.tanh(C_16 * (altitude / Constants.KM_TO_M - Z0));

                // Temperature Tc used in exospheric temperature computation
                final double tc = C_6 * MathLib.pow(instantFlux, C_7) + C_8 * MathLib.pow(meanFlux, C_9);

                // Compute the exospheric temperature
                final double tExo = tc * factor + deltaTg;

                // Check the exospheric temperature
                checkTExo(tExo);

                // Compute density if needed
                if (computeDensity) {
                    // Compute density logarithm by 2D linear interpolation
                    final double logRhoInterp = this.jacchia77Data.getRhoFunction().value(tExo, altitude);
                    density = computeDensity(logRhoInterp, altitude, date);
                }

                // Compute temperature and mean molar mass if wished
                if (computeDensityOnly) {
                    temperature = Double.NaN;
                    meanMolarMass = Double.NaN;
                } else {
                    final double[] tempAndMeanMolarMass = computeTemperatureAndMeanMolarMass(altitude, tExo);
                    temperature = tempAndMeanMolarMass[0];
                    meanMolarMass = tempAndMeanMolarMass[1];
                }
            }

            // Update the cached information
            this.cachedDate = date;
            this.cachedPosition = position;
            this.cachedFrame = frame;
            this.cachedComputeDensityOnly = computeDensityOnly;
            this.cachedOutputData = new JacchiaOutput(density, temperature, meanMolarMass);
        }
    }

    /**
     * Computes mean flux F10.7.
     *
     * @param solarActivity
     *        solarActivity provides coefficients of solar flux and geomagnetic activity
     * @param t0
     *        initial date at which instant flux is computed
     * @return the value of mean flux F10.7
     * @throws PatriusException
     *         if a problem occurs while reading the solar activity file
     */
    private double computeMeanFlux(final AbsoluteDate t0) throws PatriusException {

        // Initialization
        final double res;

        if (this.solarActivity instanceof StelaVariableSolarActivity) {
            res = ((StelaVariableSolarActivity) this.solarActivity).getMeanFlux(t0, this.weights);
        } else {
            // Initialization
            double sumW = 0;
            double meanFlux = 0.;
            AbsoluteDate t = t0.shiftedBy(-3 * TAU * Constants.JULIAN_DAY);

            // Loop on each weight and meanFlux
            for (final double weight : this.weights) {
                final double instantFlux = this.solarActivity.getInstantFluxValue(t);
                meanFlux += (weight * instantFlux);
                sumW += weight;
                t = t.shiftedBy(Constants.JULIAN_DAY);
            }
            res = MathLib.divide(meanFlux, sumW);
        }

        return res;
    }

    /**
     * Compute the density.
     * 
     * @param logRhoInterp
     *        density logarithm by 2D linear interpolation
     * @param altitude
     *        altitude
     * @param date
     *        date
     * @return the density
     */
    private double computeDensity(final double logRhoInterp, final double altitude, final AbsoluteDate date) {
        // Compute log density variation due to half-year variation
        // z / 100
        final double zOn100 = altitude / (Constants.KM_TO_M * C_19);
        final double fz = (C_17 + C_18 * zOn100 * zOn100) * MathLib.exp(-C_20 * zOn100);

        // Extract the year of "date" to build a new date defined the 1st of January from the same year
        final int dateYear = date.getComponents(TimeScalesFactory.getTAI()).getDate().getYear();
        final AbsoluteDate dateSameYear1stJan =
            new AbsoluteDate(new DateComponents(dateYear, 1, 1), TimeScalesFactory.getTAI());
        final double duration = date.durationFrom(dateSameYear1stJan); // in sec
        // Days gap between date and the first January of the same year
        final int days = (int) MathLib.floor(duration / Constants.JULIAN_DAY);

        final double phi = days / TROPICAL_DUR;
        final double tau =
            phi + C_21 * (MathLib.pow(HALF + HALF * MathLib.sin(MathUtils.TWO_PI * phi + C_22), C_23) - HALF);
        final double gt = C_24 + C_25 * (ONE + C_26 * MathLib.sin(MathUtils.TWO_PI * tau + C_27))
                * MathLib.sin(TWO * MathUtils.TWO_PI * tau + C_28);
        final double deltaLogRho = fz * gt;

        // Finally, compute the density by inversion of the density logarithm
        return MathLib.pow(TEN, logRhoInterp + deltaLogRho);
    }

    /**
     * Compute the temperature and the mean molar mass.
     * 
     * @param altitude
     *        altitude
     * @param tExo
     *        exospheric temperature
     * @return the temperature [0] and mean molar mass [1]
     */
    private double[] computeTemperatureAndMeanMolarMass(final double altitude, final double tExo) {
        // Initialization of temperature and mean molar mass interpolation maps
        this.jacchia77Data.initTempWmMaps();

        // Temperature and mean molar mass computation
        final double temperature = this.jacchia77Data.getTempFunction().value(tExo, altitude);
        final double meanMolarMass = this.jacchia77Data.getMeanMolarMassFunction().value(tExo, altitude);

        return new double[] { temperature, meanMolarMass };
    }

    /**
     * Check the exospheric temperature.
     * 
     * @param tExo
     *        exospheric temperature to check
     * @throws PatriusException
     *         if interpolation failed for tExo in densities map
     */
    private void checkTExo(final double tExo) throws PatriusException {
        // Check boundaries conditions for altitude and temperature :
        // 1) an exception is raised if temperature is lower or upper than the minimum (resp. maximum) one in TEMP_TABLE
        // 2) if the altitude is < 0., the computed quantities are computed for altitude = 0.
        // 3) if the altitude is > max altitude in ALT_TABLE, computed density is 0., temp. and molar mass are computed
        // at alt. = max

        // Perform 2D linear interpolation on (Texo, altitude) in densities map to retrieve the density logarithm
        if (tExo < Jacchia77Data.TEMP_TABLE[0]
                || tExo > Jacchia77Data.TEMP_TABLE[Jacchia77Data.TEMP_TABLE.length - 1]) {
            throw new PatriusException(PatriusMessages.STELA_2D_TEXO_OUT_OF_RANGE,
                tExo, Jacchia77Data.TEMP_TABLE[0], Jacchia77Data.TEMP_TABLE[Jacchia77Data.TEMP_TABLE.length - 1]);
        }
    }

    /**
     * Computes the local solar time.
     *
     * @param date
     *        date
     * @param position
     *        position in Mean of Date Frame
     * @return local solar time
     * @throws PatriusException
     *         if sun position cannot be computed in MOD reference frame
     */
    private double computeTLoc(final AbsoluteDate date, final Vector3D position) throws PatriusException {

        // compute thetaLST
        final double thetaLST = MathLib.atan2(position.getY(), position.getX());

        // compute thetaSun
        final Vector3D sunP = this.sun.getPVCoordinates(date, FramesFactory.getMOD(false)).getPosition();
        final double thetaSun = MathLib.atan2(sunP.getY(), sunP.getX());

        // local solar time is defined in [0;24[
        double resTLoc = (MathLib.PI + thetaLST - thetaSun) * HOURS_IN_DAY / MathUtils.TWO_PI;
        resTLoc = (resTLoc + HOURS_IN_DAY) % HOURS_IN_DAY;

        return resTLoc;
    }

    /**
     * Get the azimuth and elevation of the body.
     * 
     * @param position
     *        the position of the body
     * @return raDec the vector such that :<br>
     *         raDec[0] : azimuth (&alpha;) of the body, between -&pi; and +&pi ;<br>
     *         raDec[1] : elevation (&delta;) of the body, between -&pi;/2 and +&pi;/2
     */
    private double[] getRaDec(final Vector3D position) {
        final double[] raDec = new double[2];
        final double norm = position.getNorm();
        raDec[0] = MathLib.atan2(position.getY(), position.getX());
        raDec[1] = MathLib.asin(position.getZ() / norm);
        return raDec;
    }
}
