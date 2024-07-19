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
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11.1:FA:FA-72:30/06/2023:[PATRIUS] Mauvaise prise en compte du MeteoConditionProvider dans les
 * AbstractTropoFactory
 * VERSION:4.11:DM:DM-3295:22/05/2023:[PATRIUS] Conditions meteorologiques variables dans modeles troposphere
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.signalpropagation.troposphere;

import java.util.concurrent.ConcurrentHashMap;

import fr.cnes.sirius.patrius.bodies.EllipsoidPoint;
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
     * @param meteoConditionsProvider
     *        Provider for the meteorological conditions
     * @param point
     *        Point
     * @return the tropospheric correction model
     */
    public TroposphericCorrection getTropoCorrection(final MeteorologicalConditionsProvider meteoConditionsProvider,
                                                     final EllipsoidPoint point) {
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

        /** Point. */
        private final EllipsoidPoint point;

        /**
         * Unique key description used to store data within a Map.
         *
         * @param meteoConditionsProvider
         *        Provider for the meteorological conditions
         * @param point
         *        Point
         */
        public TroposphericCorrectionKey(final MeteorologicalConditionsProvider meteoConditionsProvider,
                                         final EllipsoidPoint point) {
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
         * Getter for the point.
         *
         * @return the point
         */
        public EllipsoidPoint getPoint() {
            return this.point;
        }
    }
}
