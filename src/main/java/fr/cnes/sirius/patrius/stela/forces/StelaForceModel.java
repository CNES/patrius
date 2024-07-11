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
 * 
 * @history created 18/01/2013
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:410:16/04/2015: Anomalies in the Patrius Javadoc
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.stela.forces;

import java.io.Serializable;

import fr.cnes.sirius.patrius.forces.ForceModel;
import fr.cnes.sirius.patrius.stela.orbits.StelaEquinoctialOrbit;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This interface represents a force modifying spacecraft motion for a
 * {@link fr.cnes.sirius.patrius.stela.propagation.StelaGTOPropagator StelaGTOPropagator}.
 * <p>
 * Objects implementing this interface are intended to be added to a
 * {@link fr.cnes.sirius.patrius.stela.propagation.StelaGTOPropagator semianalytical Stela GTO propagator} before the
 * propagation is started.
 * 
 * @see ForceModel
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id$
 * 
 * @since 1.3
 * 
 */
public interface StelaForceModel extends Serializable {

    /**
     * Compute the short periodic variations for a given spacecraft state.
     * 
     * @param orbit
     *        current orbit information: date, kinematics
     * @return the short periodic variations of the current force
     * @throws PatriusException
     *         if short periods computation fails
     */
    double[] computeShortPeriods(final StelaEquinoctialOrbit orbit) throws PatriusException;

    /**
     * Compute the partial derivatives for a given spacecraft state.
     * 
     * @param orbit
     *        current orbit information: date, kinematics
     * @return the partial derivatives of the current force
     * @throws PatriusException
     *         if partial derivatives computation fails
     */
    double[][] computePartialDerivatives(final StelaEquinoctialOrbit orbit) throws PatriusException;

    /**
     * @return the type
     */
    String getType();

}
