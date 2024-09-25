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
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11.1:FA:FA-72:30/06/2023:[PATRIUS] Mauvaise prise en compte du MeteoConditionProvider dans les
 * AbstractTropoFactory
 * VERSION:4.11:DM:DM-3295:22/05/2023:[PATRIUS] Conditions meteorologiques variables dans modeles troposphere
 * VERSION:4.11:DM:DM-3235:22/05/2023:[PATRIUS][TEMPS_CALCUL] Attitude spacecraft state lazy
 * END-HISTORY
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

    /** Standard reference pressure [Pa]. */
    public static final double P0 = 101325.;

    /** Standard reference temperature [°C]. */
    public static final double T0 = 18.;

    /** Standard reference relative humidity [%]. */
    public static final double RH0 = 50.;

    /** Standard reference altitude [m]. */
    public static final double H0 = 0.;

    /** Absolute zero for temperatures. */
    public static final double ABSOLUTE_ZERO = 273.15;

    /** Standard meteorological conditions: {@link #P0}, {@link #T0} (in [°K]) and {@link #RH0}. */
    public static final MeteorologicalConditions STANDARD = new MeteorologicalConditions(P0, T0 + ABSOLUTE_ZERO, RH0);

    /** Serializable UID. */
    private static final long serialVersionUID = -2648794555757149644L;

    /** One hundred. */
    private static final double HUNDRED = 100.;

    /** Pressure [Pa]. */
    private final double pressure;

    /** Temperature [°K]. */
    private final double temperature;

    /** Relative humidity [%]. */
    private final double humidity;

    /**
     * Standard meteorological conditions constructor with all the variables set at once.
     *
     * @param pressure
     *        Pressure [Pa]
     * @param temperature
     *        Temperature [°K]
     * @param humidity
     *        Relative humidity [%]
     * @throws NotPositiveException
     *         if {@code pressure < 0}
     * @throws NotStrictlyPositiveException
     *         if {@code temperature <= 0}
     * @throws OutOfRangeException
     *         if {@code humidity < 0} or if {@code humidity > 100}
     */
    public MeteorologicalConditions(final double pressure, final double temperature,
                                    final double humidity) {
        if (pressure < 0.) {
            throw new NotPositiveException(pressure);
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
     * Getter for the temperature [°K].
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
        double humiditySum = 0;
        for (final MeteorologicalConditions meteorologicalCondition : meteoConditionsCollection) {
            pressureSum += meteorologicalCondition.getPressure();
            temperatureSum += meteorologicalCondition.getTemperature();
            humiditySum += meteorologicalCondition.getHumidity();
        }
        return new MeteorologicalConditions(pressureSum / nbValues, temperatureSum / nbValues,
            humiditySum / nbValues);
    }

    /**
     * Computes standard model values [P, T, RH] for provided altitude given reference values
     * [P0, T0, RH0 H0] with:
     * <ul>
     * <li>P = pressure [Pa]</li>
     * <li>T = temperature [K]</li>
     * <li>RH = relative humidity [%]</li>
     * </ul>
     * 
     * @param referenceMeteoConditions
     *        reference temperature, pressure and relative humidity
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
     * <li>P = pressure [Pa] - P0 = 101325 [Pa]</li>
     * <li>T = temperature [K] - T0 = 18 [°C]</li>
     * <li>RH = humidity rate [%] - RH0 = 50 [%]</li>
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
