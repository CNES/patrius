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
 * VERSION:4.11:DM:DM-3295:22/05/2023:[PATRIUS] Conditions meteorologiques variables dans modeles troposphere
 * VERSION:4.11:FA:FA-3314:22/05/2023:[PATRIUS] Anomalie evaluation ForceModel SpacecraftState en ITRF
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.signalpropagation;

import java.io.Serializable;

import fr.cnes.sirius.patrius.time.AbsoluteDate;

/**
 * This interface is used to compute meteorological conditions at a given date, allowing to adapt the computation of
 * atmospheric effects to the moment when a signal propagates through the atmosphere.
 * 
 * @author William POLYCARPE (TSN)
 */
@FunctionalInterface
public interface MeteorologicalConditionsProvider extends Serializable {

    /**
     * Returns the meteorological conditions at a given date.
     * 
     * @param date
     *        date of meteo conditions
     * @return MeteorologicalConditions (temperature, pressure, humidity) at date
     */
    MeteorologicalConditions getMeteorologicalConditions(final AbsoluteDate date);
}
