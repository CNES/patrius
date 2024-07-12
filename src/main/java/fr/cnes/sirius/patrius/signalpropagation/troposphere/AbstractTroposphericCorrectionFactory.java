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
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11.1:FA:FA-72:30/06/2023:[PATRIUS] Mauvaise prise en compte du MeteoConditionProvider dans les AbstractTropoFactory
 * VERSION:4.11:DM:DM-3295:22/05/2023:[PATRIUS] Conditions meteorologiques variables dans modeles troposphere
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.signalpropagation.troposphere;

import java.util.concurrent.ConcurrentHashMap;

import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.signalpropagation.ConstantMeteorologicalConditionsProvider;
import fr.cnes.sirius.patrius.signalpropagation.MeteorologicalConditions;
import fr.cnes.sirius.patrius.signalpropagation.MeteorologicalConditionsProvider;

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
     * @deprecated as of release 4.11, better use {@link #getTropoCorrection(MeteorologicalConditions, GeodeticPoint)}
     */
    @Deprecated
    public TroposphericCorrection getTropoCorrection(final double pressure, final double temperature,
                                                     final double humidity, final GeodeticPoint point) {

        final MeteorologicalConditionsProvider meteoConditionsProvider =
            new ConstantMeteorologicalConditionsProvider(new MeteorologicalConditions(pressure, temperature, humidity));
        return this.getTropoCorrection(meteoConditionsProvider, point);
    }

    /**
     * Getter for a tropospheric correction model.
     * <p>
     * This method looks if the asking model is already initialized.<br>
     * If it's the case the model is directly returned, otherwise the model is initialized, stored and returned.
     * </p>
     *
     * @param meteoConditionsProvider
     *        Provider for the meteorological conditions
     * @param point
     *        Geodetic point
     * @return the tropospheric correction model
     */
    public TroposphericCorrection getTropoCorrection(final MeteorologicalConditionsProvider meteoConditionsProvider,
                                                     final GeodeticPoint point) {
        return this.correctionModelsCache.computeIfAbsent(
            new TroposphericCorrectionKey(meteoConditionsProvider, point),
            (key) -> {
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

        /** Meteorological conditions provider. */
        private final MeteorologicalConditionsProvider meteoConditionsProvider;

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
         * @deprecated as of release 4.11, better use
         *        {@link TroposphericCorrectionKey #TroposphericCorrectionKey(MeteorologicalConditions, GeodeticPoint)}
         */
        @Deprecated
        public TroposphericCorrectionKey(final double pressure, final double temperature,
                                         final double humidity, final GeodeticPoint point) {
            this(new ConstantMeteorologicalConditionsProvider(new MeteorologicalConditions(pressure, temperature,
                humidity)), point);
        }

        /**
         * Unique key description used to store data within a Map.
         *
         * @param meteoConditionsProvider
         *        Provider for the meteorological conditions
         * @param point
         *        Geodetic point
         */
        public TroposphericCorrectionKey(final MeteorologicalConditionsProvider meteoConditionsProvider,
                                         final GeodeticPoint point) {
            this.meteoConditionsProvider = meteoConditionsProvider;
            this.point = point;
        }

        /**
         * Getter for the meteorological conditions provider.
         *
         * @return the meteorological conditions provider
         */
        public MeteorologicalConditionsProvider getMeteoConditionsProvider() {
            return this.meteoConditionsProvider;
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
