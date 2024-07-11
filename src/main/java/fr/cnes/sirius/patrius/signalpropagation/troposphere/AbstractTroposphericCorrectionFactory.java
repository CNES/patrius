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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.signalpropagation.troposphere;

import java.util.concurrent.ConcurrentHashMap;

import fr.cnes.sirius.patrius.bodies.GeodeticPoint;

/**
 * Tropospheric correction model factory.
 * <p>
 * This class can initialize and store in cache {@link TroposphericCorrection} correction models.
 * </p>
 * <p>
 * The tropospheric corrections models are organized within a {@link ConcurrentHashMap} to assure multi-thread usage
 * safety.
 * </p>
 *
 * @author bonitt
 */
public abstract class AbstractTroposphericCorrectionFactory {

    /** Cached {@link TroposphericCorrection} correction models. */
    private final ConcurrentHashMap<TroposphericCorrectionKey, TroposphericCorrection> correctionModelsCache;

    /**
     * Simple constructor.
     */
    public AbstractTroposphericCorrectionFactory() {
        this.correctionModelsCache = new ConcurrentHashMap<>();
    }

    /**
     * Getter for a tropospheric correction model.
     * <p>
     * This method looks if the asking model is already initialized.<br>
     * If it's the case the model is directly returned, otherwise the model is initialized, stored and returned.
     * </p>
     *
     * @param pressure
     *        Ground atmospheric pressure [Pa]
     * @param temperature
     *        Ground atmospheric temperature [K]
     * @param humidity
     *        Ground atmospheric humidity [%]
     * @param point
     *        Geodetic point
     * @return the tropospheric correction model
     */
    public TroposphericCorrection getTropoCorrection(final double pressure, final double temperature,
                                                     final double humidity, final GeodeticPoint point) {
        return this.correctionModelsCache.computeIfAbsent(new TroposphericCorrectionKey(pressure, temperature,
            humidity, point), (key) -> {
                return buildTropoCorrection(key);
            });
    }

    /**
     * Return the tropospheric correction model provided by the factory.
     *
     * @param key
     *        Key containing all the data the tropospheric correction model need
     * @return the tropospheric correction model
     */
    protected abstract TroposphericCorrection buildTropoCorrection(TroposphericCorrectionKey key);

    /** Unique key description used to store data within a Map. */
    protected static class TroposphericCorrectionKey {

        /** Pressure [Pa]. */
        private final double pressure;

        /** Temperature [K]. */
        private final double temperature;

        /** Relative humidity [%]. */
        private final double humidity;

        /** Geodetic point. */
        private final GeodeticPoint point;

        /**
         * Unique key description used to store data within a Map.
         *
         * @param pressure
         *        Pressure [Pa]
         * @param temperature
         *        Temperature [K]
         * @param humidity
         *        Relative humidity [%]
         * @param point
         *        Geodetic point
         */
        public TroposphericCorrectionKey(final double pressure, final double temperature, final double humidity,
                                         final GeodeticPoint point) {
            this.pressure = pressure;
            this.temperature = temperature;
            this.humidity = humidity;
            this.point = point;
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
         * Getter for the humidity [%].
         *
         * @return the humidity
         */
        public double getHumidity() {
            return this.humidity;
        }

        /**
         * Getter for the geodetic point.
         *
         * @return the geodetic point
         */
        public GeodeticPoint getPoint() {
            return this.point;
        }
    }
}
