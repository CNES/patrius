/**
 *
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
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.signalpropagation.troposphere;

import java.util.ArrayList;
import java.util.Locale;

import fr.cnes.sirius.patrius.bodies.BodyPoint;
import fr.cnes.sirius.patrius.math.analysis.BivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.interpolation.BiLinearIntervalsInterpolator;
import fr.cnes.sirius.patrius.math.analysis.interpolation.BivariateGridInterpolator;
import fr.cnes.sirius.patrius.math.analysis.interpolation.LinearInterpolator;
import fr.cnes.sirius.patrius.math.analysis.interpolation.UnivariateInterpolator;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.signalpropagation.FiniteDistanceAngularCorrection;
import fr.cnes.sirius.patrius.signalpropagation.MeteorologicalConditions;
import fr.cnes.sirius.patrius.signalpropagation.MeteorologicalConditionsProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;

//CHECKSTYLE: stop MagicNumber check
//Reason: Model with empirical values

/**
 * This class represent a tropospheric refraction model. It is directly extracted from [1].
 *
 * <p>
 * This class uses interpolation tables. Be aware that it accepts extrapolation of these table to some extent. When the
 * extrapolation is likely to go too far compared to the table resolution, a verification is done. This is the case for
 * the maximum zenithal distance and the wavelength of the signal. For the other values, no check is performed.
 * </p>
 *
 * <p>
 * <b>Source:</b> [1] "Introduction aux ephemerides astronomiques" Bureau des longitudes, edition 1997"
 * </p>
 *
 * <p>
 * All private methods will use the units of the source (which are often not SI) to keep as close as possible to the
 * book formulas. All public methods will use international units.
 * </p>
 *
 * @author veuillh, amouroum
 * 
 * @since 4.13
 */
public class AstronomicalRefractionModel implements FiniteDistanceAngularCorrection {

    /**
     * Default threshold value for the iterative algorithm used to compute the elevation correction from geometric
     * elevation [rad].<br>
     * Justification: This tropospheric model is essentially used for Astrometric TAROT measurements. The precision of
     * these measurements is currently around 1e-6 rad. It is safe to take 2 orders of magnitude lower than this
     * precision.
     */
    public static final double DEFAULT_THRESHOLD = 1e-8;

    /**
     * Default max iteration number for the iterative algorithm used to compute the elevation correction from geometric
     * elevation.
     */
    public static final int DEFAULT_MAX_ITER = 15;

    /** Serializable UID. */
    private static final long serialVersionUID = -5454375357288046466L;

    /** Linear univariate interpolator. */
    private static final UnivariateInterpolator UNIVARIATE_INTERPOLATOR = new LinearInterpolator();

    /**
     * Linear bivariate interpolator.<br>
     * The BiLinearIntervalsInterpolator extrapolate with linear model, the elements outside the interpolation table.
     */
    private static final BivariateGridInterpolator BIVARIATE_INTERPOLATOR = new BiLinearIntervalsInterpolator();

    /** Maximum apparent zenithal distance allowed for tables linear extrapolations [deg]. */
    private static final double MAX_TOLERATED_APPARENT_ZENITHAL_DISTANCE = 95;

    /** Ground refractivity in normal conditions (see [1] p.193, Equation (7.3.8)) [dimensionless]. */
    private static final double GROUND_REFRACTIVITY_NORMAL_CONDITIONS = 0.000277117;

    /** Ground pressure in normal conditions (see [1] p.193, Equation (7.3.8)) [Pa]. */
    private static final double GROUND_PRESSURE_NORMAL_CONDITIONS = 101325.;

    /** Conversion factor from arc minutes to radians: PI/(180*60). */
    private static final double ARC_MINUTES_TO_RADIANS = Math.PI / 180 / 60;

    /** Absolute zero for temperatures [K°]. */
    private static final double ABSOLUTE_ZERO = 273.15;

    /** Meteorological conditions provider. */
    private final MeteorologicalConditionsProvider meteoConditionsProvider;

    /** Wavelength of the signal [nanometer]. */
    private final double wavelengthNanometer;

    /** The geodetic position of the ground station. */
    private final BodyPoint point;

    /**
     * Threshold for the iterative algorithm used to compute the elevation correction from geometric elevation [rad].
     */
    private final double threshold;

    /**
     * Max iteration number for the iterative algorithm used to compute the elevation correction from geometric
     * elevation.
     */
    private final int maxIter;

    /**
     * Simple constructor.<br>
     * The tropospheric correction is applied to the geometric elevation, and the use of values outside the tables
     * throws an exception.
     *
     * @param point
     *        The geodetic point of the ground station
     * @param meteoConditionsProvider
     *        The meteorological condition provider at the ground station
     * @param wavelengthNanometer
     *        The wavelength of the signal [nanometer]
     * @throws IllegalArgumentException
     *         if the provided wavelength is outside the tolerated wavelength bound for tables linear extrapolations
     */
    public AstronomicalRefractionModel(final BodyPoint point,
                                       final MeteorologicalConditionsProvider meteoConditionsProvider,
                                       final double wavelengthNanometer) {
        this(point, meteoConditionsProvider, wavelengthNanometer, DEFAULT_THRESHOLD, DEFAULT_MAX_ITER);
    }

    /**
     * Main constructor.<br>
     * The use of values outside the tables throws an exception.
     *
     * @param point
     *        The geodetic point of the ground station
     * @param meteoConditionsProvider
     *        The meteorological condition provider at the ground station
     * @param wavelengthNanometer
     *        The wavelength of the signal [nanometer]
     * @param threshold
     *        Threshold for the iterative algorithm used to compute the elevation correction from geometric elevation
     *        [rad]
     * @param maxIter
     *        Max iteration number for the iterative algorithm used to compute the elevation correction from geometric
     *        elevation
     * @throws IllegalArgumentException
     *         if the provided wavelength is outside the tolerated wavelength bound for tables linear extrapolations
     */
    public AstronomicalRefractionModel(final BodyPoint point,
                                       final MeteorologicalConditionsProvider meteoConditionsProvider,
                                       final double wavelengthNanometer, final double threshold, final int maxIter) {
        // Check the wavelength is inside the tolerated wavelength bound for tables linear extrapolations
        checkWavelength(wavelengthNanometer);

        this.point = point;
        this.meteoConditionsProvider = meteoConditionsProvider;
        this.wavelengthNanometer = wavelengthNanometer;
        this.threshold = threshold;
        this.maxIter = maxIter;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isDifferentiableBy(final Parameter p) {
        return false; // No supported parameter yet
    }

    /** {@inheritDoc} */
    @Override
    public boolean supportsParameter(final Parameter param) {
        return false; // No supported parameter yet
    }

    /** {@inheritDoc} */
    @Override
    public ArrayList<Parameter> getParameters() {
        return new ArrayList<>(); // No supported parameter yet
    }

    /** {@inheritDoc} */
    @Override
    public double derivativeValueFromGeometricElevation(final Parameter p, final double geometricElevation) {
        return 0.; // No supported parameter yet
    }

    /** {@inheritDoc} */
    @Override
    public double derivativeValueFromApparentElevation(final Parameter p, final double elevation) {
        return 0.; // No supported parameter yet
    }

    /**
     * Compute the tropospheric correction from the apparent elevation and distance.
     *
     * <p>
     * This method takes into account the finite distance of the observed object to add a parallax correction.
     * </p>
     *
     * @param date
     *        The date at which we want to compute the tropospheric correction
     * @param apparentElevation
     *        The apparent elevation (with atmosphere) [rad]
     * @param distance
     *        The distance to the object [m]. Can be {@link Double#POSITIVE_INFINITY} (equivalent to not take into
     *        account the parallax correction)
     * @return the elevation correction [rad] so that :
     *         {@code apparent_elevation = geometric_elevation + elevation_correction}
     */
    @Override
    public double computeElevationCorrectionFromApparentElevation(final AbsoluteDate date,
                                                                  final double apparentElevation,
                                                                  final double distance) {
        final MeteorologicalConditions meteoConditions = this.meteoConditionsProvider.getMeteorologicalConditions(date);
        return computeElevationCorrectionFromApparentElevation(apparentElevation, meteoConditions.getPressure(),
            meteoConditions.getTemperature(), meteoConditions.getHumidity(), this.wavelengthNanometer,
            this.point.getLLHCoordinates().getLatitude(), this.point.getLLHCoordinates().getHeight(), distance);
    }

    /**
     * Compute the tropospheric correction from the geometric elevation and distance.
     *
     * <p>
     * Note that this method uses an iterative algorithm to convert the geometric elevation into an apparent elevation.
     * Note that if the convergence is not reached within the {@link #getMaxIter()}, no exception is thrown.
     * </p>
     * <p>
     * This method takes into account the finite distance of the observed object to add a parallax correction.
     * </p>
     *
     * @param date
     *        The date at which we want to compute the tropospheric correction
     * @param geometricElevation
     *        The geometric elevation (without atmosphere) [rad]
     * @param distance
     *        The distance to the object [m]. Can be {@link Double#POSITIVE_INFINITY} (equivalent to not take into
     *        account the parallax correction).
     * @return the elevation correction [rad] so that :
     *         {@code apparent_elevation = geometric_elevation + elevation_correction}
     * @throw IllegalStateException
     *        if the max number of iteration is reached
     */
    @Override
    public double computeElevationCorrectionFromGeometricElevation(final AbsoluteDate date,
                                                                   final double geometricElevation,
                                                                   final double distance) {
        final MeteorologicalConditions meteoConditions = this.meteoConditionsProvider.getMeteorologicalConditions(date);
        return computeElevationCorrectionFromGeometricElevation(geometricElevation, meteoConditions.getPressure(),
            meteoConditions.getTemperature(), meteoConditions.getHumidity(), this.wavelengthNanometer,
            this.point.getLLHCoordinates().getLatitude(), this.point.getLLHCoordinates().getHeight(), distance,
            this.threshold, this.maxIter);
    }

    /** {@inheritDoc} */
    @Override
    public double getMinimalToleratedApparentElevation() {
        return zenithalDistanceToElevation(MathLib.toRadians(MAX_TOLERATED_APPARENT_ZENITHAL_DISTANCE));
    }

    /**
     * Getter for the meteo conditions provider.
     *
     * @return the meteo conditions provider
     */
    public MeteorologicalConditionsProvider getMeteoConditionsProvider() {
        return this.meteoConditionsProvider;
    }

    /**
     * Getter for the wavelength [nanometer].
     *
     * @return the wavelength
     */
    public double getWavelengthNanometer() {
        return this.wavelengthNanometer;
    }

    /**
     * Getter for the position where the model should be applied.
     *
     * @return the point where the model should be applied
     */
    public BodyPoint getPoint() {
        return this.point;
    }

    /**
     * Getter for the threshold used in the convergence algorithm to compute the correction from the geometric
     * elevation.
     *
     * @return the threshold [rad]
     */
    public double getThreshold() {
        return this.threshold;
    }

    /**
     * Getter for the maximum iteration number used in the convergence algorithm to compute the correction from the
     * geometric elevation.
     *
     * <p>
     * Note that if the convergence is not reached within this max iteration number, no exception is thrown.
     * </p>
     *
     * @return the maximum iteration number
     */
    public int getMaxIter() {
        return this.maxIter;
    }

    /**
     * Compute the ground refractivity by taking into account elevation, pressure and temperature.
     *
     * <p>
     * See [1] p. 194 Equation (7.3.9)
     * </p>
     *
     * @param apparentElevation
     *        The apparent elevation (with atmosphere) [rad]
     * @param pressure
     *        The pressure [Pa]
     * @param temperature
     *        The temperature [K°]
     * @return the ground refraction [dimensionless]
     */
    public static double computeGroundRefractivity(final double apparentElevation, final double pressure,
                                                   final double temperature) {
        final double temperatureCelsius = kelvinToCelsius(temperature);
        final double apparentZenithalDistanceDegree = MathLib.toDegrees(elevationToZenithalDistance(apparentElevation));
        final double coeffA = CoefficientATable.interpolate(apparentZenithalDistanceDegree, temperatureCelsius);
        return computeGroundRefractivityFromCoeffA(coeffA, pressure, temperatureCelsius);
    }

    /**
     * Compute the OO' distance.
     *
     * <p>
     * This distance aims at computing a parallax effect for the elevation correction at finite distance.
     * </p>
     *
     * <p>
     * See [1] p.203 Equation (7.3.19)
     * </p>
     *
     * @param groundRefractivity
     *        The ground refractivity [dimensionless]
     * @param elevationCorrection
     *        The apparent elevation correction for an object at an infinite distance [rad]
     * @param apparentElevation
     *        Apparent elevation (with atmosphere) (for an object at infinite distance) [rad]
     * @return the distance OO' [m]
     */
    public static double computeOOPrime(final double groundRefractivity, final double elevationCorrection,
                                        final double apparentElevation) {
        final double refraction = elevationCorrection;
        final double cotangentZenithalDistance = MathLib.tan(apparentElevation);
        return Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS
                * (groundRefractivity - refraction * cotangentZenithalDistance + 0.5 * refraction * refraction);
    }

    /**
     * Compute the parallax correction due to a finite distance object.
     *
     * @param geometricElevation
     *        The geometric elevation (without atmosphere) (for an object at infinite distance) [rad]
     * @param oOPrime
     *        The fictive distance to compute the parallax [m]
     * @param distance
     *        The distance to the object [m]
     * @return the parallax correction so that : {@code elevationFiniteDistance = elevationInfiniteDistance + parallax}
     */
    public static double computeParallaxCorrection(final double geometricElevation, final double oOPrime,
                                                   final double distance) {
        final double sinZenithalDistance = MathLib.cos(geometricElevation);
        return -oOPrime * sinZenithalDistance / distance;
    }

    /**
     * Convert elevation to zenithal distance [rad].
     *
     * @param elevation
     *        The elevation [rad]
     * @return the zenithal distance
     */
    public static double elevationToZenithalDistance(final double elevation) {
        return MathUtils.HALF_PI - elevation;
    }

    /**
     * Convert zenithal distance to elevation [rad].
     *
     * @param zenithalDistance
     *        The zenithal distance [rad]
     * @return the elevation
     */
    public static double zenithalDistanceToElevation(final double zenithalDistance) {
        return MathUtils.HALF_PI - zenithalDistance;
    }

    /**
     * Compute the tropospheric correction from the apparent elevation and the provided conditions.
     *
     * <p>
     * This method takes into account the finite distance of the observed object to add a parallax correction.
     * </p>
     *
     * @param apparentElevation
     *        The apparent elevation [rad]
     * @param pressure
     *        The pressure [Pa]
     * @param temperature
     *        The temperature [K°]
     * @param relativeHumidity
     *        The relative humidity (from 0 to 100) [%]
     * @param wavelengthNanometer
     *        The wavelength [nanometer]
     * @param latitude
     *        The latitude [rad]
     * @param altitude
     *        The altitude [m]
     * @param distance
     *        The distance of the observed object [m]. Can be {@link Double#POSITIVE_INFINITY} (equivalent to not take
     *        into account the parallax correction).
     * @return the elevation correction [rad] so that :
     *         {@code apparentElevation = geometricElevation + elevationCorrection}
     * @throws IllegalArgumentException
     *         if the apparent zenithal distance is greater than the maximum apparent zenithal distance allowed for
     *         tables linear extrapolations<br>
     *         if the provided wavelength is outside the tolerated wavelength bound for tables linear extrapolations
     */
    public static double computeElevationCorrectionFromApparentElevation(final double apparentElevation,
                                                                         final double pressure,
                                                                         final double temperature,
                                                                         final double relativeHumidity,
                                                                         final double wavelengthNanometer,
                                                                         final double latitude, final double altitude,
                                                                         final double distance) {
        // Initializations
        final double apparentZenithalDistanceDegree = MathLib.toDegrees(elevationToZenithalDistance(apparentElevation));
        final double temperatureCelsius = kelvinToCelsius(temperature);
        final double latitudeDegree = MathLib.toDegrees(latitude);
        final double coeffA = CoefficientATable.interpolate(apparentZenithalDistanceDegree, temperatureCelsius);

        // Compute refraction
        double elevationCorrection = computeRefraction(apparentZenithalDistanceDegree, pressure, temperatureCelsius,
            relativeHumidity, wavelengthNanometer, latitudeDegree, altitude, coeffA);

        if (Double.isFinite(distance)) {
            // Compute the parallax correction
            final double geometricElevationAtInfiniteDistance = apparentElevation - elevationCorrection;
            final double groundRefractivity = computeGroundRefractivityFromCoeffA(coeffA, pressure, temperatureCelsius);
            final double ooPrime = computeOOPrime(groundRefractivity, elevationCorrection, apparentElevation);
            final double parallaxeCorrection = computeParallaxCorrection(geometricElevationAtInfiniteDistance, ooPrime,
                distance);
            elevationCorrection += parallaxeCorrection;
        }

        return elevationCorrection;
    }

    /**
     * Compute the tropospheric correction from the geometric elevation and the provided conditions.
     *
     * <p>
     * The input is the geometric elevation while the model takes the apparent elevation as an input. An iterative
     * algorithm (fix point) is used to call the model with the apparent elevation, hence the threshold and maxIter
     * arguments. Note that if the convergence is not reached within the maxIter, no exception is thrown.
     * </p>
     *
     * <p>
     * This method takes into account the finite distance of the observed object to add a parallax correction.
     * </p>
     *
     * @param geometricElevation
     *        The geometric elevation [rad]
     * @param pressure
     *        The pressure [Pa]
     * @param temperature
     *        The temperature [K°]
     * @param relativeHumidity
     *        The relative humidity (from 0 to 100) [%]
     * @param wavelengthNanometer
     *        The wavelength [nanometer]
     * @param latitude
     *        The latitude [rad]
     * @param altitude
     *        The altitude [m]
     * @param distance
     *        The distance of the observed object [m]. Can be {@link Double#POSITIVE_INFINITY} (equivalent to not take
     *        into account the parallax correction).
     * @param threshold
     *        Threshold for the iterative algorithm [rad]
     * @param maxIter
     *        Max iteration number
     * @return the geometric elevation [rad]
     * @throw IllegalStateException
     *        if the max number of iteration is reached
     */
    public static double computeElevationCorrectionFromGeometricElevation(final double geometricElevation,
                                                                          final double pressure,
                                                                          final double temperature,
                                                                          final double relativeHumidity,
                                                                          final double wavelengthNanometer,
                                                                          final double latitude,
                                                                          final double altitude, final double distance,
                                                                          final double threshold, final int maxIter) {
        // Build the function x=g(x) to apply the fix point method, where x is the elevation correction
        final DoubleToDoubleFunction func = elevCorrection -> {
            return computeElevationCorrectionFromApparentElevation(geometricElevation + elevCorrection,
                pressure, temperature, relativeHumidity, wavelengthNanometer, latitude, altitude, distance);
        };

        return computeFixPoint(func, 0, threshold, maxIter);
    }

    /**
     * Compute the tropospheric refraction with the provided conditions.
     *
     * <p>
     * The refraction (R) allows to pass from the apparent zenithal distance (z_app) to the geometric zenithal distance
     * (z_geo) as follows: {@code z_geo = z_app + R}.<br>
     * Equivalently, in elevation formulation, we have {@code el_geo = el_app - R}.
     * </p>
     *
     * <p>
     * See [1] p.196 §7.3.2
     * </p>
     *
     * @param zenithalDistanceDegree
     *        The apparent zenithal distance [deg]
     * @param pressure
     *        The pressure [Pa]
     * @param temperatureCelsius
     *        The temperature [C°]
     * @param relativeHumidity
     *        The relative humidity (from 0 to 100) [%]
     * @param wavelengthNanometer
     *        The wavelength [nanometer]
     * @param latitudeDegree
     *        The latitude [deg]
     * @param altitude
     *        The altitude [m]
     * @param coeffA
     *        The coefficient A [dimensionless]
     * @return the tropospheric correction [rad]
     * @throws IllegalArgumentException
     *         if the apparent zenithal distance is greater than the maximum apparent zenithal distance allowed for
     *         tables linear extrapolations<br>
     *         if the provided wavelength is outside the tolerated wavelength bound for tables linear extrapolations
     */
    private static double computeRefraction(final double zenithalDistanceDegree, final double pressure,
                                            final double temperatureCelsius,
                                            final double relativeHumidity, final double wavelengthNanometer,
                                            final double latitudeDegree,
                                            final double altitude, final double coeffA) {

        // Check the apparent zenithal distance
        if (zenithalDistanceDegree > MAX_TOLERATED_APPARENT_ZENITHAL_DISTANCE) {
            throw new IllegalArgumentException(String.format(Locale.US,
                "The apparent zenithal distance (%fdeg) is greater than the maximum zenithal distance allowed (%fdeg)",
                zenithalDistanceDegree, MAX_TOLERATED_APPARENT_ZENITHAL_DISTANCE));
        }

        // Refraction in normal conditions
        double refraction = computeNormalConditionsRefraction(zenithalDistanceDegree);

        // Pressure and Temperature correction
        refraction *= computePTCorrectionFactor(zenithalDistanceDegree, pressure, temperatureCelsius, coeffA);

        // Wavelength correction
        refraction *= computeWavelengthCorrectionFactor(zenithalDistanceDegree, wavelengthNanometer);

        // Humidity correction
        refraction *= computeHumidityCorrectionFactor(zenithalDistanceDegree, temperatureCelsius, relativeHumidity);

        // Latitude correction
        refraction *= computeLatitudeCorrectionFactor(zenithalDistanceDegree, latitudeDegree);

        // Altitude correction
        refraction *= computeAltitudeCorrectionFactor(zenithalDistanceDegree, altitude);

        return refraction;
    }

    /**
     * Compute the refraction in normal conditions.
     *
     * <p>
     * See [1] p.196 §7.3.2.1
     * </p>
     *
     * @param zenithalDistanceDegree
     *        The apparent zenithal distance
     * @return the refraction in normal conditions [rad]
     */
    private static double computeNormalConditionsRefraction(final double zenithalDistanceDegree) {
        final double r0;
        if (zenithalDistanceDegree < CoefficientR0Table.MIN_ZENITHAL_DISTANCE_DEGREE) {
            // If below the CoefficientR0.MIN_ZENITHAL_DISTANCE_DEGREE, use p.193 Equation (7.3.7)
            final double tanZenithalDistance = MathLib.tan(Math.toRadians(zenithalDistanceDegree));
            final double tanZenithalDistanceCubic = tanZenithalDistance * tanZenithalDistance * tanZenithalDistance;
            r0 = (57.085 * tanZenithalDistance - 0.0666 * tanZenithalDistanceCubic) * Constants.ARC_SECONDS_TO_RADIANS;
        } else {
            // Otherwise, use the R0 table
            r0 = CoefficientR0Table.interpolate(zenithalDistanceDegree);
        }
        return r0;
    }

    /**
     * Compute the refraction correction factor due to pressure and temperature departing from the normal conditions.
     *
     * <p>
     * See [1] p.195 Equation (7.3.13)
     * </p>
     *
     * @param zenithalDistanceDegree
     *        The apparent zenithal distance [deg]
     * @param pressure
     *        Pressure [Pa]
     * @param temperatureCelsius
     *        Temperature [C°]
     * @param coeffA
     *        Coefficient A [dimensionless]
     * @return the correction factor [dimensionless]
     */
    private static double computePTCorrectionFactor(final double zenithalDistanceDegree, final double pressure,
                                                    final double temperatureCelsius, final double coeffA) {
        final double pressureCorrectionFactor = pressure / GROUND_PRESSURE_NORMAL_CONDITIONS;
        final double temperatureCorrectionFactor = 1.0552126 / (1. + 0.00368084 * temperatureCelsius);
        final double coeffB = CoefficientBTable.interpolate(zenithalDistanceDegree, pressure);
        final double idealGasLawCorrectionFactor = coeffA * coeffB;
        return pressureCorrectionFactor * temperatureCorrectionFactor * idealGasLawCorrectionFactor;
    }

    /**
     * Compute the refraction correction factor due to a wavelength different from the normal conditions.
     *
     * <p>
     * See [1] p.195 Equation (7.3.14)
     * </p>
     *
     * @param zenithalDistanceDegree
     *        The apparent zenithal distance [deg]
     * @param wavelengthNanometer
     *        The wavelength [nanometer]
     * @return the correction factor [dimensionless]
     * @throws IllegalArgumentException
     *         if the provided wavelength is outside the tolerated wavelength bound for tables linear extrapolations
     */
    private static double computeWavelengthCorrectionFactor(final double zenithalDistanceDegree,
                                                            final double wavelengthNanometer) {

        // Check the wavelength is inside the tolerated wavelength bound for tables linear extrapolations
        checkWavelength(wavelengthNanometer);

        final double coeffC;
        if (zenithalDistanceDegree < CoefficientCTable.MIN_ZENITHAL_DISTANCE_DEGREE) {
            coeffC = 1;
        } else {
            coeffC = CoefficientCTable.interpolate(wavelengthNanometer, zenithalDistanceDegree);
        }
        final double wavelengthMicroMeterSquared = wavelengthNanometer * wavelengthNanometer * 1e-6;
        return (0.98282 + 0.005981 / wavelengthMicroMeterSquared) * coeffC;
    }

    /**
     * Compute the refraction correction factor due to humidity.
     *
     * <p>
     * See [1] p.196 Equation (7.3.15)
     * </p>
     *
     * @param zenithalDistanceDegree
     *        The apparent zenithal distance [deg]
     * @param temperatureCelsius
     *        The temperature [C°]
     * @param relativeHumidity
     *        The relative humidity (from 0 to 100) [%]
     * @return the correction factor [dimensionless]
     */
    private static double computeHumidityCorrectionFactor(final double zenithalDistanceDegree,
                                                          final double temperatureCelsius,
                                                          final double relativeHumidity) {
        final double saturatingWaterVaporPartialPressure = CoefficientFTable.interpolate(temperatureCelsius);
        final double waterVaporPartialPressure = relativeHumidity * saturatingWaterVaporPartialPressure / 100.;
        final double coeffD;
        if (zenithalDistanceDegree < CoefficientDTable.MIN_ZENITHAL_DISTANCE_DEGREE) {
            coeffD = 1.;
        } else {
            coeffD = CoefficientDTable.interpolate(waterVaporPartialPressure, zenithalDistanceDegree);
        }

        final double firstOrder = -0.152e-5 * waterVaporPartialPressure;
        final double secondOrder = -0.55e-9 * waterVaporPartialPressure * waterVaporPartialPressure;
        return (1. + firstOrder + secondOrder) * coeffD;
    }

    /**
     * Compute the refraction correction factor due to the latitude.
     *
     * <p>
     * See [1] p.196 §7.3.2.5
     * </p>
     *
     *
     * @param zenithalDistanceDegree
     *        The apparent zenithal distance [deg]
     * @param latitudeDegree
     *        The latitude [deg]
     * @return the correction factor [dimensionless]
     */
    private static double computeLatitudeCorrectionFactor(final double zenithalDistanceDegree,
                                                          final double latitudeDegree) {
        final double coeffE;
        if (zenithalDistanceDegree < CoefficientETable.MIN_ZENITHAL_DISTANCE_DEGREE) {
            coeffE = 0.;
        } else {
            coeffE = CoefficientETable.interpolate(latitudeDegree, zenithalDistanceDegree);
        }
        return 1. + coeffE;
    }

    /**
     * Compute the refraction correction factor due to the altitude.
     *
     * <p>
     * See [1] p.196 §7.3.2.6
     * </p>
     *
     * @param zenithalDistanceDegree
     *        The apparent zenithal distance [deg]
     * @param altitude
     *        The altitude [m]
     * @return the correction factor [dimensionless]
     */
    private static double computeAltitudeCorrectionFactor(final double zenithalDistanceDegree, final double altitude) {
        final double coeffH;
        if (zenithalDistanceDegree < CoefficientHTable.MIN_ZENITHAL_DISTANCE_DEGREE) {
            coeffH = 1.;
        } else {
            coeffH = CoefficientHTable.interpolate(altitude, zenithalDistanceDegree);
        }
        return coeffH;
    }

    /**
     * Compute ground refractivity by taking into account coefficient A, pressure and temperature.
     *
     * <p>
     * See [1] p. 194 Equation (7.3.9)
     * </p>
     *
     * @param coeffA
     *        Coefficient A computed from {@link CoefficientATable}
     * @param pressure
     *        The pressure [Pa]
     * @param temperatureCelsius
     *        The temperature [C°]
     * @return the ground refractivity [rad]
     */
    private static double computeGroundRefractivityFromCoeffA(final double coeffA, final double pressure,
                                                              final double temperatureCelsius) {
        final double pressureCorrectionRatio = pressure / GROUND_PRESSURE_NORMAL_CONDITIONS;
        final double temperatureCorrectionRatio = 1.0552126 / (1. + 0.00368084 * temperatureCelsius);
        return GROUND_REFRACTIVITY_NORMAL_CONDITIONS * pressureCorrectionRatio * temperatureCorrectionRatio * coeffA;
    }

    /**
     * Fix point solver using the fix point method.
     *
     * <p>
     * Note that if the threshold is not reached within the maxIter iterations, the result is still returned.<br>
     * This choice was made because at very low elevations, the algorithm struggles to converge. However, since the
     * model is also of poorer precision at these low elevations, a lower accuracy is acceptable. It is then convenient
     * to avoid an exception in this case.
     * </p>
     *
     * @param func
     *        The function for which the fix point needs to be found
     * @param initialGuess
     *        Initial guess of the fix point value
     * @param threshold
     *        The threshold to detect convergence
     * @param maxIter
     *        The maximum iterations allowed
     * @return the fix point value
     */
    private static double computeFixPoint(final DoubleToDoubleFunction func, final double initialGuess,
                                          final double threshold, final int maxIter) {

        // Initializations
        int nbIter = 0;
        double fixPoint = initialGuess;

        // Loop
        double oldFixPoint;
        do {
            oldFixPoint = fixPoint;
            fixPoint = func.apply(oldFixPoint);
            nbIter++;
        } while (MathLib.abs(fixPoint - oldFixPoint) > threshold && nbIter < maxIter);

        // Implementation note: purposely do not check the convergence (see javadoc)

        return fixPoint;
    }

    /**
     * Convert Kelvin degrees to Celsius degrees.
     *
     * @param temperatureInKelvin
     *        The temperature [K°]
     * @return the temperature in Celsius [C°]
     */
    private static double kelvinToCelsius(final double temperatureInKelvin) {
        return temperatureInKelvin - ABSOLUTE_ZERO;
    }

    /**
     * Check if the specified wavelength is inside the tolerated wavelength bound for tables linear extrapolations.
     *
     * @param wavelengthNanometer
     *        The wavelength of the signal to check [nanometer]
     * @throws IllegalArgumentException
     *         if the provided wavelength is outside the tolerated wavelength bound for tables linear extrapolations
     */
    private static void checkWavelength(final double wavelengthNanometer) {
        // Check the wavelength is inside the tolerated wavelength bound for tables linear extrapolations
        if (wavelengthNanometer < CoefficientCTable.MIN_WAVELENGTH
                || wavelengthNanometer > CoefficientCTable.MAX_WAVELENGTH) {
            throw new IllegalArgumentException(
                String.format(Locale.US, "The provided wavelength (%.0gnm) is outside the tolerance [%.0f, %.0f]nm",
                    wavelengthNanometer, CoefficientCTable.MIN_WAVELENGTH, CoefficientCTable.MAX_WAVELENGTH));
        }
    }

    /** Functional interface used for {@link AstronomicalRefractionModel#computeFixPoint}. */
    @FunctionalInterface
    private interface DoubleToDoubleFunction {

        /**
         * Applies this function to the given argument.
         *
         * @param value
         *        The function argument
         * @return the function value
         */
        public double apply(double value);
    }

    /**
     * Coefficient A interpolation table.
     *
     * <p>
     * The coefficient A is a factor that takes into account a correction to the ideal gas expansion law due to the
     * temperature and light ray direction in the atmosphere (see [1] p.194 Equation (7.3.10) and p.197 §7.3.2.2).
     * </p>
     *
     * <p>
     * See [1] p.199 Table 7.3.4.
     * </p>
     */
    private static class CoefficientATable {

        /** X-axis representing the zenithal distance [deg]. */
        private static final double[] X_AXIS = new double[] {
            // 0 to 40 degrees: 10 degrees steps
            0., 10., 20., 30., 40.,
            // 45 to 65 degrees: 5 degrees steps
            45., 50., 55., 60., 65.,
            // 70 to 79 degrees: 1 degrees steps
            70., 71., 72., 73., 74., 75., 76., 77., 78., 79.,
            // 80 to 84.5 degrees: 0.5 degrees steps
            80., 80.5, 81., 81.5, 82., 82.5, 83., 83.5, 84., 84.5,
            // 85 to 90 degrees: 0.1 degrees steps
            85., 85. + 1. / 6., 85. + 2. / 6., 85. + 3. / 6., 85. + 4. / 6., 85. + 5. / 6.,
            86., 86. + 1. / 6, 86. + 2. / 6, 86. + 3. / 6, 86. + 4. / 6., 86. + 5. / 6.,
            87., 87. + 1. / 6., 87. + 2. / 6, 87. + 3. / 6, 87. + 4. / 6, 87. + 5. / 6.,
            88., 88. + 1. / 6., 88. + 2. / 6., 88. + 3. / 6, 88. + 4. / 6, 88. + 5. / 6., 89.,
            89. + 1. / 6., 89. + 2. / 6., 89. + 3. / 6., 89. + 4. / 6., 89. + 5. / 6., 90. };

        /** Y-axis representing the temperature [C°]. */
        private static final double[] Y_AXIS = new double[] { -30, -10, 10, 30 };

        /** Values representing the coefficient A [dimensionless]. */
        private static final double[][] COEFF_A_VALUES = new double[][] {
            // 0 to 40 degrees: 10 degrees steps
            { 1.00000, 1.00000, 1.00000, 1.00000 },
            { 1.00000, 1.00000, 1.00000, 1.00000 },
            { 1.00000, 1.00000, 1.00000, 1.00000 },
            { 1.00000, 1.00000, 1.00000, 1.00000 },
            { 1.00003, 1.00002, 1.00000, 0.99999 },
            // 45 to 65 degrees: 5 degrees steps
            { 1.00010, 1.00005, 1.00001, 0.99997 },
            { 1.00020, 1.00011, 1.00002, 0.99994 },
            { 1.00036, 1.00019, 1.00004, 0.99989 },
            { 1.00058, 1.00031, 1.00006, 0.99983 },
            { 1.00096, 1.00051, 1.00010, 0.99970 },
            // 70 to 79 degrees: 1 degrees steps
            { 1.00162, 1.00088, 1.00017, 0.99949 },
            { 1.00181, 1.00099, 1.00019, 0.99942 },
            { 1.00205, 1.00111, 1.00022, 0.99935 },
            { 1.00232, 1.00126, 1.00025, 0.99927 },
            { 1.00263, 1.00144, 1.00028, 0.99916 },
            { 1.00303, 1.00165, 1.00033, 0.99904 },
            { 1.00349, 1.00191, 1.00038, 0.99888 },
            { 1.00406, 1.00222, 1.00044, 0.99870 },
            { 1.00476, 1.00260, 1.00051, 0.99847 },
            { 1.00565, 1.00310, 1.00061, 0.99819 },
            // 80 to 84.5 degrees: 0.5 degrees steps
            { 1.00680, 1.00372, 1.00074, 0.99781 },
            { 1.00751, 1.00411, 1.00081, 0.99759 },
            { 1.00832, 1.00455, 1.00090, 0.99733 },
            { 1.00925, 1.00506, 1.00100, 0.99703 },
            { 1.01035, 1.00566, 1.00112, 0.99668 },
            { 1.01163, 1.00636, 1.00126, 0.99627 },
            { 1.01317, 1.00720, 1.00142, 0.99579 },
            { 1.01498, 1.00820, 1.00162, 0.99520 },
            { 1.01720, 1.00941, 1.00185, 0.99450 },
            { 1.01992, 1.01088, 1.00214, 0.99365 },
            // 85 to 90 degrees: 0.1 degrees steps
            { 1.02328, 1.01271, 1.00250, 0.99260 },
            { 1.02458, 1.01341, 1.00264, 0.99219 },
            { 1.02598, 1.01417, 1.00279, 0.99176 },
            { 1.02750, 1.01499, 1.00295, 0.99129 },
            { 1.02915, 1.01588, 1.00312, 0.99078 },
            { 1.03095, 1.01686, 1.00331, 0.99022 },
            { 1.03290, 1.01791, 1.00352, 0.98962 },
            { 1.03504, 1.01906, 1.00374, 0.98897 },
            { 1.03738, 1.02031, 1.00398, 0.98826 },
            { 1.03995, 1.02168, 1.00425, 0.98748 },
            { 1.04276, 1.02319, 1.00454, 0.98664 },
            { 1.04586, 1.02485, 1.00486, 0.98571 },
            { 1.04930, 1.02667, 1.00521, 0.98468 },
            { 1.05310, 1.02870, 1.00560, 0.98356 },
            { 1.05732, 1.03094, 1.00603, 0.98232 },
            { 1.06204, 1.03343, 1.00651, 0.98094 },
            { 1.06733, 1.03621, 1.00704, 0.97942 },
            { 1.07327, 1.03932, 1.00763, 0.97772 },
            { 1.07996, 1.04282, 1.00829, 0.97583 },
            { 1.08757, 1.04677, 1.00904, 0.97372 },
            { 1.09624, 1.05124, 1.00987, 0.97134 },
            { 1.10616, 1.05633, 1.01082, 0.96867 },
            { 1.11761, 1.06216, 1.01190, 0.96565 },
            { 1.13092, 1.06886, 1.01313, 0.96223 },
            { 1.14647, 1.07660, 1.01454, 0.95833 },
            { 1.16484, 1.08563, 1.01617, 0.95388 },
            { 1.18675, 1.09623, 1.01805, 0.94878 },
            { 1.21316, 1.10876, 1.02025, 0.94289 },
            { 1.24550, 1.12374, 1.02283, 0.93609 },
            { 1.28567, 1.14184, 1.02588, 0.92814 },
            { 1.37050, 1.17963, 1.03230, 0.91170 }
        };

        /** Interpolation function. */
        private static final BivariateFunction COEFF_A_INTERPOLATOR =
            BIVARIATE_INTERPOLATOR.interpolate(X_AXIS, Y_AXIS, COEFF_A_VALUES);

        /**
         * Interpolate the coefficient A table.
         *
         * @param zenithalDistanceDegree
         *        The apparent zenithal distance [deg]
         * @param temperatureCelsius
         *        The temperature [C°]
         * @return the coefficient A [dimensionless]
         */
        private static double interpolate(final double zenithalDistanceDegree, final double temperatureCelsius) {
            return COEFF_A_INTERPOLATOR.value(zenithalDistanceDegree, temperatureCelsius);
        }
    }

    /**
     * Coefficient B interpolation table.
     *
     * <p>
     * The coefficient B is a factor that takes into account a correction to the ideal gas expansion law due to the
     * pressure and light ray direction in the atmosphere (see [1] p.194 Equation (7.3.12) and p.197 §7.3.2.2).
     * </p>
     *
     * <p>
     * See [1] p.199 Table 7.3.5.
     * </p>
     */
    private static class CoefficientBTable {

        /** X-axis representing the zenithal distance [deg]. */
        private static final double[] X_AXIS = new double[] {
            // 0 to 60 degrees: 10 degrees steps
            0., 10., 20., 30., 40., 50., 60.,
            // 65 : 5 degrees steps
            65.,
            // 70 to 84 degrees: 1 degree steps
            70., 71., 72., 73., 74., 75., 76., 77., 78., 79., 80., 81., 82., 83., 84.,
            // 85 to 86.5 degrees: 0.5 degrees steps
            85., 85.5, 86., 86.5,
            // 87 to 90 degrees: 0.25 degrees steps
            87., 87.25, 87.5, 87.75, 88., 88.25, 88.5, 88.75, 89., 89.25, 89.5, 89.75, 90. };

        /** Y-axis representing the pressure [Pa]. */
        private static final double[] Y_AXIS = new double[] {
            50000., 70000., 90000., 110000.
        };

        /** Values representing the coefficient B [dimensionless]. */
        private static final double[][] COEFF_B_VALUES = new double[][] {
            // 0 to 60 degrees: 10 degrees steps
            { 0.99979, 0.99989, 0.99995, 1.00003 },
            { 0.99979, 0.99989, 0.99995, 1.00003 },
            { 0.99979, 0.99989, 0.99995, 1.00003 },
            { 0.99979, 0.99988, 0.99995, 1.00004 },
            { 0.99979, 0.99985, 0.99994, 1.00004 },
            { 0.99972, 0.99982, 0.99993, 1.00005 },
            { 0.99958, 0.99974, 0.99991, 1.00007 },
            // 65 : 5 degrees steps
            { 0.99951, 0.99967, 0.99988, 1.00009 },
            // 70 to 84 degrees: 1 degree steps
            { 0.99929, 0.99956, 0.99985, 1.00012 },
            { 0.99922, 0.99952, 0.99983, 1.00013 },
            { 0.99915, 0.99948, 0.99981, 1.00015 },
            { 0.99908, 0.99945, 0.99980, 1.00016 },
            { 0.99901, 0.99937, 0.99977, 1.00017 },
            { 0.99887, 0.99930, 0.99975, 1.00019 },
            { 0.99873, 0.99922, 0.99973, 1.00021 },
            { 0.99859, 0.99915, 0.99968, 1.00025 },
            { 0.99838, 0.99900, 0.99964, 1.00028 },
            { 0.99817, 0.99885, 0.99959, 1.00032 },
            { 0.99781, 0.99867, 0.99951, 1.00038 },
            { 0.99739, 0.99841, 0.99942, 1.00044 },
            { 0.99683, 0.99808, 0.99930, 1.00054 },
            { 0.99612, 0.99760, 0.99914, 1.00067 },
            { 0.99500, 0.99697, 0.99890, 1.00085 },
            // 85 to 86.5 degrees: 0.5 degrees steps
            { 0.99359, 0.99605, 0.99857, 1.00110 },
            { 0.99261, 0.99546, 0.99835, 1.00127 },
            { 0.99142, 0.99473, 0.99808, 1.00148 },
            { 0.98995, 0.99381, 0.99774, 1.00174 },
            // 87 to 90 degrees: 0.25 degrees steps
            { 0.98807, 0.99267, 0.99733, 1.00204 },
            { 0.98695, 0.99197, 0.99707, 1.00227 },
            { 0.98570, 0.99120, 0.99679, 1.00248 },
            { 0.98430, 0.99032, 0.99646, 1.00274 },
            { 0.98278, 0.98937, 0.99611, 1.00302 },
            { 0.98097, 0.98824, 0.99570, 1.00334 },
            { 0.97897, 0.98696, 0.99522, 1.00371 },
            { 0.97662, 0.98553, 0.99469, 1.00414 },
            { 0.97400, 0.98390, 0.99407, 1.00463 },
            { 0.97105, 0.98197, 0.99336, 1.00519 },
            { 0.96755, 0.97979, 0.99255, 1.00584 },
            { 0.96360, 0.97730, 0.99160, 1.00660 },
            { 0.95925, 0.97452, 0.99061, 1.00785 }
        };

        /** Interpolation function. */
        private static final BivariateFunction COEFF_B_INTERPOLATOR =
            BIVARIATE_INTERPOLATOR.interpolate(X_AXIS, Y_AXIS, COEFF_B_VALUES);

        /**
         * Interpolate the coefficient B table.
         *
         * @param zenithalDistanceDegree
         *        The apparent zenithal distance [deg]
         * @param pressure
         *        The pressure [Pa]
         * @return the coefficient B [dimensionless]
         */
        private static double interpolate(final double zenithalDistanceDegree, final double pressure) {
            return COEFF_B_INTERPOLATOR.value(zenithalDistanceDegree, pressure);
        }
    }

    /**
     * Coefficient C interpolation table.
     *
     * <p>
     * The coefficient C is a factor that takes into account the wavelength correction to the refraction for big
     * zenithal distances (see [1] p.197 §7.3.2.3).
     * </p>
     *
     * <p>
     * See [1] p.201 Table 7.3.6.
     * </p>
     */
    private static class CoefficientCTable {

        /** X-axis representing the wavelength [nanometer]. */
        private static final double[] X_AXIS = new double[] { 400., 420., 440., 460., 480., 500., 520., 540., 560.,
            580., 600., 620., 640., 660., 680., 700. };

        /** Y-axis representing the zenithal distance [deg]. */
        private static final double[] Y_AXIS = new double[] { 85, 88, 90 };

        /** Values representing the coefficient C [dimensionless]. */
        private static final double[][] COEFF_C_VALUES = new double[][] {
            { 1.00025, 1.00071, 1.00168 },
            { 1.00021, 1.00058, 1.00143 },
            { 1.00018, 1.00048, 1.00110 },
            { 1.00014, 1.00039, 1.00092 },
            { 1.00011, 1.00030, 1.00074 },
            { 1.00008, 1.00023, 1.00058 },
            { 1.00005, 1.00016, 1.00046 },
            { 1.00003, 1.00010, 1.00032 },
            { 1.00002, 1.00003, 1.00023 },
            { 1.00000, 1.00001, 1.00007 },
            { 0.99999, 0.99998, 0.99995 },
            { 0.99998, 0.99996, 0.99988 },
            { 0.99997, 0.99992, 0.99982 },
            { 0.99997, 0.99988, 0.99977 },
            { 0.99996, 0.99986, 0.99962 },
            { 0.99995, 0.99984, 0.99968 }
        };

        /** Interpolation function. */
        private static final BivariateFunction COEFF_C_INTERPOLATOR =
            BIVARIATE_INTERPOLATOR.interpolate(X_AXIS, Y_AXIS, COEFF_C_VALUES);

        /** Minimal zenithal distance to interpolate in the table. */
        private static final double MIN_ZENITHAL_DISTANCE_DEGREE = 85.;

        /** Minimal tolerated wavelength for tables linear extrapolations [nanometer]. */
        private static final double MIN_WAVELENGTH = 300.;

        /** Maximal tolerated wavelength for tables linear extrapolations [nanometer]. */
        private static final double MAX_WAVELENGTH = 800.;

        /**
         * Interpolate the coefficient C table.
         *
         * @param wavelengthNanometer
         *        The wavelength [nanometer]
         * @param zenithalDistanceDegree
         *        The apparent zenithal distance [deg]
         * @return the coefficient C [dimensionless]
         */
        private static double interpolate(final double wavelengthNanometer, final double zenithalDistanceDegree) {
            return COEFF_C_INTERPOLATOR.value(wavelengthNanometer, zenithalDistanceDegree);
        }
    }

    /**
     * Coefficient D interpolation table.
     *
     * <p>
     * The coefficient D is a factor that takes into account the water vapor partial pressure correction to the
     * refraction for big zenithal distances (see [1] p.197 §7.3.2.4).
     * </p>
     *
     * <p>
     * See [1] p.201 Table 7.3.7.
     * </p>
     */
    private static class CoefficientDTable {

        /** X-axis representing the water vapor pressure [Pa]. */
        private static final double[] X_AXIS = new double[] {
            0., 200., 400., 600., 800.,
            1000., 1200., 1400., 1600., 1800.,
            2000., 2200., 2400., 2600., 2800.,
            3000., 3200., 3400., 3600. };

        /** Y-axis representing the zenithal distance [deg]. */
        private static final double[] Y_AXIS = new double[] { 70., 80., 85., 88., 90. };

        /** Values representing the coefficient D [dimensionless]. */
        private static final double[][] COEFF_D_VALUES = new double[][] {
            { 1.00000, 1.00000, 1.00000, 1.00000, 1.00000 },
            { 1.00000, 0.99999, 0.99997, 0.99989, 0.99959 },
            { 1.00000, 0.99998, 0.99994, 0.99975, 0.99906 },
            { 0.99999, 0.99996, 0.99987, 0.99959, 0.99843 },
            { 0.99999, 0.99994, 0.99982, 0.99942, 0.99774 },

            { 0.99998, 0.99992, 0.99976, 0.99922, 0.99675 },
            { 0.99998, 0.99991, 0.99970, 0.99901, 0.99588 },
            { 0.99997, 0.99989, 0.99965, 0.99878, 0.99491 },
            { 0.99997, 0.99988, 0.99958, 0.99855, 0.99383 },
            { 0.99996, 0.99986, 0.99950, 0.99825, 0.99254 },

            { 0.99996, 0.99985, 0.99941, 0.99797, 0.99125 },
            { 0.99995, 0.99981, 0.99931, 0.99765, 0.98989 },
            { 0.99995, 0.99978, 0.99922, 0.99731, 0.98830 },
            { 0.99994, 0.99975, 0.99912, 0.99696, 0.98662 },
            { 0.99994, 0.99971, 0.99901, 0.99659, 0.98487 },

            { 0.99993, 0.99967, 0.99889, 0.99618, 0.98308 },
            { 0.99992, 0.99964, 0.99878, 0.99576, 0.98110 },
            { 0.99991, 0.99960, 0.99866, 0.99533, 0.97905 },
            { 0.99990, 0.99956, 0.99854, 0.99487, 0.97686 }
        };

        /** Interpolation function. */
        private static final BivariateFunction COEFF_D_INTERPOLATOR =
            BIVARIATE_INTERPOLATOR.interpolate(X_AXIS, Y_AXIS, COEFF_D_VALUES);

        /** Minimal zenithal distance to interpolate in the table. */
        private static final double MIN_ZENITHAL_DISTANCE_DEGREE = 70.;

        /**
         * Interpolate the coefficient D table.
         *
         * @param waterVaporPartialPressure
         *        The water vapor partial pressure [Pa]
         * @param zenithalDistanceDegree
         *        The apparent zenithal distance [deg]
         * @return the coefficient D [dimensionless]
         */
        private static double interpolate(final double waterVaporPartialPressure, final double zenithalDistanceDegree) {
            return COEFF_D_INTERPOLATOR.value(waterVaporPartialPressure, zenithalDistanceDegree);
        }
    }

    /**
     * Coefficient E interpolation table.
     *
     * <p>
     * The coefficient E is a factor that takes into account the latitude correction to the refraction for big zenithal
     * distances (see [1] p.197 §7.3.2.5).
     * </p>
     *
     * <p>
     * See [1] p.202 Table 7.3.8.
     * </p>
     */
    private static class CoefficientETable {

        /** X-axis representing the latitude [deg]. */
        private static final double[] X_AXIS = new double[] { 0., 10., 20., 30., 40., 50., 60., 70. };

        /** Y-axis representing the zenithal distance [deg]. */
        private static final double[] Y_AXIS = new double[] { 70., 80., 82., 84., 86., 87., 88., 89., 90. };

        /** Values representing the coefficient E [dimensionless]. */
        private static final double[][] COEFF_E_VALUES = new double[][] {
            { -6.e-5, -23.e-5, -32.e-5, -53.e-5, -90.e-5, -124.e-5, -175.e-5, -255.e-5, -400.e-5 },
            { -5.e-5, -21.e-5, -30.e-5, -48.e-5, -85.e-5, -115.e-5, -163.e-5, -240.e-5, -378.e-5 },
            { -4.e-5, -16.e-5, -25.e-5, -39.e-5, -69.e-5, -94.e-5, -132.e-5, -195.e-5, -313.e-5 },
            { -2.e-5, -11.e-5, -18.e-5, -25.e-5, -46.e-5, -62.e-5, -88.e-5, -129.e-5, -210.e-5 },
            { 0.e-5, -2.e-5, -5.e-5, -9.e-5, -16.e-5, -22.e-5, -32.e-5, -48.e-5, -87.e-5 },
            { 0.e-5, 2.e-5, 4.e-5, 7.e-5, 13.e-5, 19.e-5, 25.e-5, 37.e-5, 58.e-5 },
            { 2.e-5, 9.e-5, 14.e-5, 23.e-5, 41.e-5, 58.e-5, 83.e-5, 120.e-5, 182.e-5 },
            { 4.e-5, 16.e-5, 23.e-5, 37.e-5, 65.e-5, 90.e-5, 129.e-5, 186.e-5, 283.e-5 }
        };

        /** Interpolation function. */
        private static final BivariateFunction COEFF_E_INTERPOLATOR =
            BIVARIATE_INTERPOLATOR.interpolate(X_AXIS, Y_AXIS, COEFF_E_VALUES);

        /** Minimal zenithal distance to interpolate in the table. */
        private static final double MIN_ZENITHAL_DISTANCE_DEGREE = 70.;

        /**
         * Interpolate the coefficient E table.
         *
         * @param latitudeDegree
         *        The latitude [deg]
         * @param zenithalDistanceDegree
         *        The apparent zenithal distance [deg]
         * @return the coefficient E [dimensionless]
         */
        private static double interpolate(final double latitudeDegree, final double zenithalDistanceDegree) {
            return COEFF_E_INTERPOLATOR.value(MathLib.abs(latitudeDegree), zenithalDistanceDegree);
        }
    }

    /**
     * Coefficient F interpolation table.
     *
     * <p>
     * The coefficient F represent the saturating water vapor pressure in the air, function of the temperature.
     * </p>
     *
     * <p>
     * Interpolation of the log of the pressure to have a more linear dependence between temperature and pressure.
     * </p>
     *
     * <p>
     * See http://fr.wikipedia.org/wiki/Pression_de_vapeur_saturante, Dupré formula.
     * </p>
     */
    private static class CoefficientFTable {

        /** Constant 100. */
        private static final double ONE_HUNDRED = 100.;

        /** X-axis representing the temperature [C°]. */
        private static final double[] X_AXIS =
            new double[] { -60., -40., -20., -10., 0., 5., 10., 15., 20., 25., 30., 40., 50., 60., 100. };

        /** Values representing the log of the coefficient F [milli-bar]. */
        private static final double[] COEFF_F_VALUES =
            new double[] { MathLib.log(0.001), MathLib.log(0.13), MathLib.log(1.03), MathLib.log(2.6),
                MathLib.log(6.10), MathLib.log(8.72), MathLib.log(12.3), MathLib.log(17.0), MathLib.log(23.4),
                MathLib.log(31.7), MathLib.log(42.4), MathLib.log(73.8), MathLib.log(123.), MathLib.log(199.),
                MathLib.log(1013.25) };

        /** Interpolation function. */
        private static final UnivariateFunction COEFF_F_INTERPOLATOR =
            UNIVARIATE_INTERPOLATOR.interpolate(X_AXIS, COEFF_F_VALUES);

        /**
         * Interpolate the coefficient F table.
         *
         * @param temperatureCelsius
         *        The temperature [C°]
         * @return the coefficient F [Pa]
         */
        private static double interpolate(final double temperatureCelsius) {
            // Apply exponential to recover the pressure from the log of the pressure
            final double coeffF = MathLib.exp(COEFF_F_INTERPOLATOR.value(temperatureCelsius));
            return coeffF * ONE_HUNDRED; // conversion from millibar to Pascal
        }
    }

    /**
     * Coefficient H interpolation table.
     *
     * <p>
     * The coefficient H is a factor that takes into account the altitude correction to the refraction for big zenithal
     * distances (see [1] p.197 §7.3.2.6).
     * </p>
     *
     * <p>
     * See [1] p.202 Table 7.3.9.
     * </p>
     */
    private static class CoefficientHTable {

        /** X-axis representing the altitude [m]. */
        private static final double[] X_AXIS =
            new double[] { 0., 500., 1000., 1500., 2000., 2500., 3000., 3500., 4000., 4500., 5000. };

        /** Y-axis representing the zenithal distance [deg]. */
        private static final double[] Y_AXIS = new double[] { 80.0, 85.0, 88.0, 90.0 };

        /** Values representing the coefficient E [dimensionless]. */
        private static final double[][] COEFF_H_VALUES = new double[][] {
            { 1.00000, 1.00000, 1.00000, 1.0000 },
            { 0.99998, 0.99952, 0.99608, 0.9727 },
            { 0.99996, 0.99905, 0.99218, 0.9460 },
            { 0.99994, 0.99857, 0.98835, 0.9198 },
            { 0.99992, 0.99815, 0.98453, 0.8942 },
            { 0.99990, 0.99772, 0.98083, 0.8693 },
            { 0.99988, 0.99730, 0.97720, 0.8449 },
            { 0.99986, 0.99688, 0.97368, 0.8209 },
            { 0.99984, 0.99651, 0.97023, 0.7979 },
            { 0.99983, 0.99619, 0.96700, 0.7751 },
            { 0.99981, 0.99587, 0.96387, 0.7532 }
        };

        /** Interpolation function. */
        private static final BivariateFunction COEFF_H_INTERPOLATOR =
            BIVARIATE_INTERPOLATOR.interpolate(X_AXIS, Y_AXIS, COEFF_H_VALUES);

        /** Minimal zenithal distance to interpolate in the table. */
        private static final double MIN_ZENITHAL_DISTANCE_DEGREE = 80.;

        /**
         * Interpolate the coefficient H table.
         *
         * @param altitude
         *        The altitude [m]
         * @param zenithalDistanceDegree
         *        The apparent zenithal distance [deg]
         * @return the coefficient H [dimensionless]
         */
        private static double interpolate(final double altitude, final double zenithalDistanceDegree) {
            return COEFF_H_INTERPOLATOR.value(altitude, zenithalDistanceDegree);
        }
    }

    /**
     * Coefficient R0 interpolation table.
     *
     * <p>
     * The coefficient R0 is the refraction in normal conditions for big zenithal distances (see [1] p.196 §7.3.2.1).
     * </p>
     *
     * <p>
     * Validity range: from 70 to 90 degrees of zenithal distances.
     * </p>
     *
     * <p>
     * See [1] p.198 Table 7.3.3.
     * </p>
     */
    private static class CoefficientR0Table {

        /**
         * Step [deg] of the X-axis representing the zenithal distance [deg].<br>
         * The x-axis ranges from 70 to 90 degrees, with a step of 10 arc minutes.
         */
        private static final double STEP = 1. / 6;

        /** Values representing the coefficient R0 [arc-minute]. */
        private static final double[] R0_VALUES =
            new double[] { 2.59118, 2.61443, 2.63802, 2.66200, 2.68638, 2.71112, 2.73627, 2.76183,
                2.78785, 2.81428, 2.84118, 2.86853, 2.89633, 2.92462, 2.95338, 2.98270, 3.01258,
                3.04290, 3.07390, 3.10533, 3.13747, 3.17015, 3.20345, 3.23742, 3.27203, 3.30733,
                3.34333, 3.38002, 3.41750, 3.45572, 3.49468, 3.53450, 3.57518, 3.61675, 3.65913,
                3.70253, 3.74678, 3.79208, 3.83838, 3.88578, 3.93432, 3.98390, 4.03468, 4.08667,
                4.13988, 4.19448, 4.25038, 4.30763, 4.36645, 4.42668, 4.48858, 4.55207, 4.61720,
                4.68413, 4.75290, 4.82368, 4.89630, 4.97117, 5.04812, 5.12730, 5.20882, 5.29283,
                5.37947, 5.46888, 5.56095, 5.65612, 5.75425, 5.85570, 5.96058, 6.06888, 6.18115,
                6.29722, 6.41755, 6.54212, 6.67143, 6.80548, 6.94462, 7.08922, 7.23967, 7.39603,
                7.55890, 7.72838, 7.90513, 8.08960, 8.28207, 8.48338, 8.69358, 8.91372, 9.14447,
                9.38618, 9.63982, 9.90647, 10.18658, 10.48138, 10.79217, 11.11985, 11.46565,
                11.83147, 12.21825, 12.62812, 13.06318, 13.52505, 14.01615, 14.53913, 15.09693,
                15.69273, 16.32972, 17.01173, 17.73980, 18.52972, 19.37532, 20.28705, 21.27153,
                22.33617, 23.49032, 24.74280, 26.10530, 27.59045, 29.21317, 30.98908, 32.96618 };

        /** Minimal zenithal distance to interpolate in the table. */
        private static final double MIN_ZENITHAL_DISTANCE_DEGREE = 70.;

        /** Maximal zenithal distance to extrapolate the table. */
        private static final double MAX_ZENITHAL_DISTANCE_DEGREE = 90.;

        /** Linear extrapolation coefficient for zenithal distances greater than 90 deg. */
        private static final double EXTRAPOLATION_SLOPE =
            (R0_VALUES[R0_VALUES.length - 1] - R0_VALUES[R0_VALUES.length - 2]) / STEP;

        /**
         * Interpolate the coefficient A table. Linear extrapolation if outside the table.
         *
         * @param zenithalDistanceDegree
         *        The apparent zenithal distance [deg]
         * @return the coefficient R0 [rad]
         */
        private static double interpolate(final double zenithalDistanceDegree) {
            // Linear interpolation
            final double iDouble = (zenithalDistanceDegree - MIN_ZENITHAL_DISTANCE_DEGREE) / STEP;
            final int i = (int) iDouble;
            final double iFrac = (iDouble - i);
            double r0ArcMinute = 0.;
            final double overshoot = (zenithalDistanceDegree - MAX_ZENITHAL_DISTANCE_DEGREE) / STEP;

            if (overshoot == 0) {
                // We are exactly on 90° zenithal distance
                r0ArcMinute = R0_VALUES[i];
            } else if (overshoot > 0.) {
                r0ArcMinute = R0_VALUES[R0_VALUES.length - 1] + overshoot * EXTRAPOLATION_SLOPE;
            } else {
                r0ArcMinute = (1. - iFrac) * R0_VALUES[i] + iFrac * R0_VALUES[i + 1];
            }

            // Return the coefficient R0
            return r0ArcMinute * ARC_MINUTES_TO_RADIANS;
        }
    }

    //CHECKSTYLE: resume MagicNumber check
}
