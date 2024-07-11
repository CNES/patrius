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
 * @history created 01/02/2013
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:289:30/11/2014:Refactoring of SpacecraftState and harmonization of state vector
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.stela.propagation;

import java.io.Serializable;

import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.stela.orbits.StelaEquinoctialOrbit;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Interface representing the Stela GTO propagator additional equations.
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id$
 * 
 * @since 1.3
 * 
 */
public interface StelaAdditionalEquations extends Serializable {

    /**
     * Get the name of the additional state.
     * 
     * @return name of the additional state
     */
    String getName();

    /**
     * Compute the derivatives related to the additional state parameters.
     * 
     * @param o
     *        current orbit information: date, kinematics
     * @param p
     *        current value of the additional parameters
     * @param pDot
     *        placeholder where the derivatives of the additional parameters
     *        should be put
     * @exception PatriusException
     *            if some specific error occurs
     */
    void computeDerivatives(StelaEquinoctialOrbit o, double[] p, double[] pDot) throws PatriusException;

    /**
     * @param state
     *        before adding additional state
     * @return the initial additional state
     * @throws PatriusException
     *         should not happen
     */
    SpacecraftState addInitialAdditionalState(final SpacecraftState state) throws PatriusException;

    /**
     * @return the additional equations dimension
     */
    int getEquationsDimension();
}
