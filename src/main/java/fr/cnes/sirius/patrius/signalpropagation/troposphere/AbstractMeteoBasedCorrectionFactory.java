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
 */
package fr.cnes.sirius.patrius.signalpropagation.troposphere;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import fr.cnes.sirius.patrius.bodies.BodyPoint;
import fr.cnes.sirius.patrius.signalpropagation.AngularCorrection;
import fr.cnes.sirius.patrius.signalpropagation.MeteorologicalConditionsProvider;

/**
 * Meteorologically based correction model factory.
 * <p>
 * This class can initialize and store in cache meteorologically based correction models.
 * </p>
 * <p>
 * The correction models are organized within a {@link ConcurrentHashMap} to ensure multi-thread safety.
 * </p>
 *
 * @param <T>
 *        Meteorologically based correction type (can be {@link AngularCorrection} or {@link TroposphericCorrection} for
 *        instance)
 *
 * @author bonitt
 * 
 * @since 4.13
 */
public abstract class AbstractMeteoBasedCorrectionFactory<T> {

    /** Cached correction models. */
    private final ConcurrentHashMap<MeteoBasedKey, T> correctionModelsCache;

    /**
     * Simple constructor.
     */
    public AbstractMeteoBasedCorrectionFactory() {
        this.correctionModelsCache = new ConcurrentHashMap<>();
    }

    /**
     * Getter for a meteorologically based correction model.
     * <p>
     * This method looks if the required model is already initialized.<br>
     * If it's the case the model is directly returned, otherwise the model is initialized, stored and returned.
     * </p>
     *
     * @param meteoConditionsProvider
     *        Provider for the meteorological conditions
     * @param point
     *        Point
     * @return the correction model
     */
    public T getCorrectionModel(final MeteorologicalConditionsProvider meteoConditionsProvider, final BodyPoint point) {
        return this.correctionModelsCache.computeIfAbsent(new MeteoBasedKey(meteoConditionsProvider, point),
            (key) -> {
                return buildMeteoBasedCorrection(key);
            });
    }

    /**
     * Return the meteorologically based correction model provided by the factory.
     *
     * @param key
     *        Key containing the required information to initialize the correction model
     * @return the correction model
     */
    protected abstract T buildMeteoBasedCorrection(MeteoBasedKey key);

    /** Unique key description used to store data within a Map. */
    protected static class MeteoBasedKey {

        /** Meteorological conditions provider. */
        private final MeteorologicalConditionsProvider meteoConditionsProvider;

        /** Point. */
        private final BodyPoint point;

        /**
         * Unique key description used to store data within a Map.
         *
         * @param meteoConditionsProvider
         *        Provider for the meteorological conditions
         * @param point
         *        Point
         */
        public MeteoBasedKey(final MeteorologicalConditionsProvider meteoConditionsProvider, final BodyPoint point) {
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
        public BodyPoint getPoint() {
            return this.point;
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
                final MeteoBasedKey other = (MeteoBasedKey) object;

                // Evaluate the invalidation method components
                isEqual = Objects.equals(this.meteoConditionsProvider, other.meteoConditionsProvider)
                        && Objects.equals(this.point, other.point);
            }

            return isEqual;
        }

        /** {@inheritDoc} */
        @Override
        public int hashCode() {
            return Objects.hash(this.meteoConditionsProvider, this.point);
        }
    }
}
