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
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1490:26/04/2018: major change to Coppola architecture
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.integration.sphere.lebedev;

/**
 * An interface representing a Lebedev function (i.e. a function taking a
 * LebedevGridPoint as argument).
 * 
 * @since 4.1
 *
 * @version $Id: SimpsonIntegrator.java 18108 2017-10-04 06:45:27Z bignon $
 */
public interface LebedevFunction {

    /**
     * Compute the value of the function at the given grid point.
     *
     * @param point
     *        the grid point at which the function must be evaluated
     *
     * @return the function value for the given grid point
     */
    double value(final LebedevGridPoint point);
}
