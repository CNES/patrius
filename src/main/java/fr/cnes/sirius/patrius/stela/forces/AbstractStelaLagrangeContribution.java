/**
 * 
 * Copyright 2011-2017 CNES
 *
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
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
 * @history 05/03/2013
 */
package fr.cnes.sirius.patrius.stela.forces;

import fr.cnes.sirius.patrius.stela.orbits.StelaEquinoctialOrbit;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * This abstract class represents a force with Lagrange attribute, to be used in a
 * {@link fr.cnes.sirius.patrius.stela.propagation.StelaGTOPropagator StelaGTOPropagator}.
 * </p>
 * 
 * @concurrency immutable
 * 
 * @author Cédric Dental
 * 
 * @version $Id$
 * 
 * @since 1.3
 * 
 */
public abstract class AbstractStelaLagrangeContribution implements StelaForceModel {

    /** Serial UID. */
    private static final long serialVersionUID = -8812833274223392714L;

    /**
     * Value of the potential derivative
     */
    protected double[] dPot = new double[6];
    /**
     * Type of the equations
     */
    private final String type;

    /**
     * Constructor of the class
     * 
     */
    public AbstractStelaLagrangeContribution() {
        this.type = "LAGRANGE";

    }

    /**
     * @return the type
     */
    @Override
    public String getType() {
        return this.type;
    }

    /**
     * @return the dPot
     */
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    public double[] getdPot() {
        return this.dPot;
    }

    /**
     * Compute the dE/dt force derivatives for a given spacecraft state.
     * 
     * @param orbit
     *        current orbit information: date, kinematics
     * @return the perturbation dE/dt for the current force
     * @throws PatriusException
     *         if perturbation computation fails
     */
    public abstract double[] computePerturbation(final StelaEquinoctialOrbit orbit) throws PatriusException;
}
