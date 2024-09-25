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
 * @history created 13/09/2013
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:86:22/10/2013:Created the MassProvider interface
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:200:28/08/2014: dealing with a negative mass in the propagator
 * VERSION::FA:373:12/01/2015: proper handling of mass event detection
 * VERSION::FA:673:12/09/2016: add getTotalMass(state)
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation;

import java.io.Serializable;
import java.util.List;

import fr.cnes.sirius.patrius.propagation.numerical.AdditionalEquations;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Interface for spacecraft models that provide the mass.
 * 
 * @author Rami Houdroge
 * @since 2.1
 * @version $Id: MassProvider.java 18092 2017-10-02 17:12:58Z bignon $
 */
public interface MassProvider extends Cloneable, Serializable {

    /** Default prefix for additional equation from MassProvider. */
    String MASS = "MASS_";

    /**
     * Return the mass of the spacecraft.
     * 
     * @return spacecraft mass
     */
    double getTotalMass();

    /**
     * Return the mass of the spacecraft following the order.
     * <ul>
     * <li>If mass is in spacecraft state, mass from spacecraft state will be returned</li>
     * <li>Otherwise mass from mass provider is returned (same as {@link #getTotalMass()})</li>
     * </ul>
     * 
     * @param state
     *        spacecraft state
     * @return spacecraft mass
     */
    double getTotalMass(final SpacecraftState state);

    /**
     * Return the mass of the given part.
     * 
     * @param partName
     *        given part
     * @return mass of part
     */
    double getMass(final String partName);

    /**
     * Update the mass of the given part.
     * 
     * @param partName
     *        given part
     * @param mass
     *        mass of the given part
     * 
     * @throws PatriusException
     *         thrown if the mass becomes negative (PatriusMessages.SPACECRAFT_MASS_BECOMES_NEGATIVE)
     */
    void updateMass(final String partName, final double mass) throws PatriusException;

    /**
     * Set mass derivative to zero.
     * 
     * @param partName
     *        name of part whose mass derivative is set to zero
     */
    void setMassDerivativeZero(final String partName);

    /**
     * Add the mass derivate of the given part.
     * 
     * @param partName
     *        name of part subject to mass variation
     * @param flowRate
     *        flow rate of specified part
     */
    void addMassDerivative(final String partName, final double flowRate);

    /**
     * Get the mass equation related to the part.
     * 
     * @param name
     *        part name
     * @return the associated mass equation
     */
    AdditionalEquations getAdditionalEquation(final String name);

    /**
     * Get the list of the name of the parts.
     * 
     * @return the list of the spacecraft parts name.
     */
    List<String> getAllPartsNames();
}
