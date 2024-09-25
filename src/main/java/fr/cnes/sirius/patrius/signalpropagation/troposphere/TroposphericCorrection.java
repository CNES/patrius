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
 * Copyright 2011-2012 Space Applications Services
 *
 * HISTORY
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11:DM:DM-3295:22/05/2023:[PATRIUS] Conditions meteorologiques variables dans modeles troposphere
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-2929:15/11/2021:[PATRIUS] Harmonisation des modeles de troposphere 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:88:18/11/2013: refactoring and renaming of the interface
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.signalpropagation.troposphere;

import fr.cnes.sirius.patrius.math.parameter.IParameterizable;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.time.AbsoluteDate;

/**
 * Defines a tropospheric model, used to calculate the signal delay for the signal path
 * imposed to electro-magnetic signals between an orbital satellite and a ground station.
 *
 * @author Thomas Neidhart
 */
public interface TroposphericCorrection extends IParameterizable {

    /**
     * Calculates the tropospheric signal delay for the signal path from a ground station to a
     * satellite at a given date.
     *
     * @param date
     *        date of meteo conditions
     * @param elevation
     *        the elevation of the satellite [rad]
     * @return the signal delay due to the troposphere [s]
     */
    double computeSignalDelay(final AbsoluteDate date, final double elevation);

    /**
     * Compute the signal delay derivative value with respect to the input parameter.
     *
     * @param p
     *        parameter
     * @param elevation
     *        the elevation of the satellite [rad]
     * @return the derivative value
     */
    double derivativeValue(final Parameter p, final double elevation);

    /**
     * Tell if the function is differentiable by the given parameter.
     *
     * @param p
     *        function parameter
     * @return true if the function is differentiable by the given parameter.
     */
    boolean isDifferentiableBy(final Parameter p);

}
