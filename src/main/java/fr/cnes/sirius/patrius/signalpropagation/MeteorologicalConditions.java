/**
 * HISTORY
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11.1:FA:FA-72:30/06/2023:[PATRIUS] Mauvaise prise en compte du MeteoConditionProvider dans les
 * AbstractTropoFactory
 * VERSION:4.11:DM:DM-3295:22/05/2023:[PATRIUS] Conditions meteorologiques variables dans modeles troposphere
 * VERSION:4.11:DM:DM-3235:22/05/2023:[PATRIUS][TEMPS_CALCUL] Attitude spacecraft state lazy
 * END-HISTORY
 */
/*
 */
/*
 */
/*
 */
/*
 */
/*
 */
package fr.cnes.sirius.patrius.signalpropagation;

import java.io.Serializable;
import java.util.Collection;
import java.util.Objects;

import fr.cnes.sirius.patrius.math.exception.NotPositiveException;
import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Simple container for pressure/temperature/humidity (PTH) triplets to describe meteorological conditions.
 * <p>
 * Instances of this class are guaranteed to be immutable.
 * </p>
 *
 * @author bonitt
 */
public class MeteorologicalConditions implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = -2648794555757149644L;

    /** Standard reference pressure (Pascal). */
    private static final double P0 = 101325;

    /** Standard reference temperature (Celsuis). */
    private static final double T0 = 18;

    /** Standard reference humidity percentage. */
    private static final double RH0 = 50.0;

    /** Standard reference altitude. */
    private static final double H0 = 0;

    /** Absolute zero for temperatures. */
    private static final double ABSOLUTE_ZERO = 273.15;

    /** One hundred. */
    private static final double HUNDRED = 100.;

    /** Pressure [Pa]. */
    private final double pressure;

    /** Temperature [K]. */
    private final double temperature;

    /** Relative humidity [%]. */
    private final double humidity;

    /**
     * Standard meteorological conditions constructor with all the variables set at once.
     * 
     * @param pressure
     *        Pressure [Pa]
     * @param temperature
     *        Temperature [K]
     * @param humidity
     *        Relative humidity [%]
     * @throws NotPositiveException
     *         if {@code pressure <= 0}
     * @throws NotStrictlyPositiveException
     *         if {@code temperature <= 0}
     * @throws OutOfRangeException
     *         if {@code humidity < 0} or if {@code humidity > 100}
     */
    public MeteorologicalConditions(final double pressure, final double temperature,
                                    final double humidity) {
        if (pressure < 0.) {
            throw new NotPositiveException(temperature);
        }
        if (temperature <= 0.) {
            throw new NotStrictlyPositiveException(temperature);
        }
        if ((humidity < 0.) || (humidity > HUNDRED)) {
            throw new OutOfRangeException(humidity, 0., HUNDRED);
        }
        this.pressure = pressure;
        this.temperature = temperature;
        this.humidity = humidity;
    }

    /**
     * Getter for the pressure [Pa].
     *
     * @return the pressure
     */
    public double getPressure() {
        return this.pressure;
    }

    /**
     * Getter for the temperature [K].
     *
     * @return the temperature
     */
    public double getTemperature() {
        return this.temperature;
    }

    /**
     * Getter for the relative humidity [%].
     *
     * @return the relative humidity
     */
    public double getHumidity() {
        return this.humidity;
    }

    /**
     * Compute the mean of the meteorological conditions
     *
     * @param meteoConditionsCollection
     *        set of meteo conditions to average
     * @return mean values of meteo conditions
     */
    public static MeteorologicalConditions mean(final Collection<MeteorologicalConditions> meteoConditionsCollection) {
        final int nbValues = meteoConditionsCollection.size();
        double pressureSum = 0;
        double temperatureSum = 0;
        double relativeHumiditySum = 0;
        for (final MeteorologicalConditions meteorologicalCondition : meteoConditionsCollection) {
            pressureSum += meteorologicalCondition.getPressure();
            temperatureSum += meteorologicalCondition.getTemperature();
            relativeHumiditySum += meteorologicalCondition.getHumidity();
        }
        return new MeteorologicalConditions(pressureSum / nbValues, temperatureSum / nbValues,
            relativeHumiditySum / nbValues);
    }

    /**
     * Computes standard model values [P, T, RH] for provided altitude given reference values
     * [P0, T0, RH0 H0] with:
     * <ul>
     * <li>P = pressure [Pa]</li>
     * <li>T = temperature [K]</li>
     * <li>RH = humidity rate [%]</li>
     * </ul>
     * 
     * @param referenceMeteoConditions
     *        reference temperature, pressure and humidity
     * @param referenceAltitude
     *        reference altitude
     * @param altitude
     *        altitude for which values [P, T, RH] should be returned
     * @return [P, T, RH] values
     */
    public static MeteorologicalConditions
        computeStandardValues(final MeteorologicalConditions referenceMeteoConditions, final double referenceAltitude,
                              final double altitude) {

        final double referencePressure = referenceMeteoConditions.getPressure();
        final double referenceTemperature = referenceMeteoConditions.getTemperature();
        final double referenceHumidity = referenceMeteoConditions.getHumidity();

        final double pressure = referencePressure * MathLib.pow(1 - 0.0000226 * (altitude - referenceAltitude), 5.225);
        final double temperature = referenceTemperature - 0.0065 * (altitude - referenceAltitude);
        final double humidity = referenceHumidity * MathLib.exp(-0.0006396 * (altitude - referenceAltitude));
        return new MeteorologicalConditions(pressure, temperature, humidity);
    }

    /**
     * Computes standard model values [P, T, R] for provided altitude with standard reference values
     * [P0, T0, RH0] provided by tropospheric models :
     * <ul>
     * <li>P = pressure [Pa] - P0 = 101325 Pa</li>
     * <li>T = temperature [K] - T0 = 18 degree Celsius</li>
     * <li>RH = humidity rate [%] - RH0 = 50%</li>
     * </ul>
     * 
     * @param altitude
     *        altitude for which values [P, T, RH] should be returned
     * @return standard model values [P, T, RH]
     */
    public static MeteorologicalConditions computeStandardValues(final double altitude) {

        // Standard reference values
        final double pressure = P0;
        final double temperature = T0 + ABSOLUTE_ZERO;
        // humidity as a percentage
        final double humidity = RH0;

        final MeteorologicalConditions meteoConditions = new MeteorologicalConditions(pressure, temperature, humidity);

        return computeStandardValues(meteoConditions, H0, altitude);
    }

    /**
     * Get a String representation of this meteorological conditions.
     * 
     * @return a String representation of this meteorological conditions
     */
    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{" + this.pressure + "[Pa]; " + this.temperature + "[K]; "
                + this.humidity + "[%]}\n";
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object object) {
        boolean isEqual = false;

        if (object == this) {
            // Identity
            isEqual = true;
        } else if ((object != null) && (object.getClass() == this.getClass())) {
            // Same object type: check all attributes
            final MeteorologicalConditions other = (MeteorologicalConditions) object;

            // Evaluate the meteorological conditions container components
            isEqual = Objects.equals(new Double(this.pressure), new Double(other.pressure))
                    && Objects.equals(new Double(this.temperature), new Double(other.temperature))
                    && Objects.equals(new Double(this.humidity), new Double(other.humidity));
        }

        return isEqual;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hash(new Double(this.pressure), new Double(this.temperature), new Double(
            this.humidity));
    }
}
