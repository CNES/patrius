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
 *
 *
 * HISTORY
 * VERSION:4.13:DM:DM-120:08/12/2023:[PATRIUS] Merge de la branche patrius-for-lotus dans Patrius
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11.1:FA:FA-72:30/06/2023:[PATRIUS] Mauvaise prise en compte du MeteoConditionProvider dans les
 * AbstractTropoFactory
 * VERSION:4.11:DM:DM-3295:22/05/2023:[PATRIUS] Conditions meteorologiques variables dans modeles troposphere
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.signalpropagation;

import java.util.Objects;

import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Provides constant meteorological conditions on a given date interval.
 * 
 * @author William POLYCARPE (TSN)
 */
public class ConstantMeteorologicalConditionsProvider implements MeteorologicalConditionsProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = 3968112080829697589L;

    /** Meteo conditions : pressure, temperature, humidity. */
    private final MeteorologicalConditions constantMeteoConditions;

    /** Date interval on which conditions are constant. */
    private final AbsoluteDateInterval dateInterval;

    /**
     * Constructor with a default infinite date interval.
     * 
     * @param constantMeteoConditions
     *        constant meteorological conditions
     */
    public ConstantMeteorologicalConditionsProvider(final MeteorologicalConditions constantMeteoConditions) {
        this(constantMeteoConditions, AbsoluteDateInterval.INFINITY);
    }

    /**
     * Constructor.
     * 
     * @param constantMeteoConditions
     *        constant meteorological conditions
     * @param dateInterval
     *        date interval on which the conditions are constant
     */
    public ConstantMeteorologicalConditionsProvider(final MeteorologicalConditions constantMeteoConditions,
                                                    final AbsoluteDateInterval dateInterval) {
        this.constantMeteoConditions = constantMeteoConditions;
        this.dateInterval = dateInterval;
    }

    /** {@inheritDoc} */
    @Override
    public MeteorologicalConditions getMeteorologicalConditions(final AbsoluteDate date) {
        // Exception thrown if date is outside the definition interval
        if (!this.dateInterval.contains(date)) {
            throw PatriusException.createIllegalStateException(PatriusMessages.DATE_OUTSIDE_INTERVAL);
        }
        return this.constantMeteoConditions;
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
            final ConstantMeteorologicalConditionsProvider other = (ConstantMeteorologicalConditionsProvider) object;

            // Evaluate the provider parameters
            isEqual = Objects.equals(this.constantMeteoConditions, other.constantMeteoConditions)
                    && Objects.equals(this.dateInterval, other.dateInterval);
        }

        return isEqual;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hash(this.constantMeteoConditions, this.dateInterval);
    }
}
