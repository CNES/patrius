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
 * @history creation 22/10/2013
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.6:DM:DM-2571:27/01/2021:[PATRIUS] Integrateur Stormer-Cowell 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:184:17/12/2013:Throw exception if mass becomes negative
 * VERSION::FA:86:22/10/2013:New mass management system
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:200:28/08/2014: dealing with a negative mass in the propagator
 * VERSION::FA:373:12/01/2015: proper handling of mass event detection
 * VERSION::FA:706:13/12/2016: synchronisation problem with the Assemby mass
 * VERSION::FA:1852:05/10/201/8: move mass flow rate test in MassEquation
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.properties;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import fr.cnes.sirius.patrius.propagation.MassProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.numerical.AdditionalEquations;
import fr.cnes.sirius.patrius.propagation.numerical.TimeDerivativesEquations;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This class represents the mass equation with a name in the form "MASS_<part name>".
 * 
 * @concurrency not thread-safe
 * @concurrency.comment internal mutable attributes
 * 
 * @version $Id$
 * @see MassProvider
 */
public class MassEquation implements AdditionalEquations {

    /** Default prefix for mass equation */
    public static final String PREFIX = "MASS_";
    /** Generated Serial UID */
    private static final long serialVersionUID = 4585447992608363108L;
    /** Equation name */
    private String name;
    /** flow rate */
    private double flowRate;

    /**
     * Empty constructor for {@link Externalizable} use.
     */
    public MassEquation() {
        this.name = "";
        this.flowRate = 0.;
    }

    /**
     * Create a new mass equation, for a given part. The name of the created equation is set by default in the
     * following form : "MASS_<part name>".
     * 
     * @param partName
     *        name of part subject to this equation
     */
    public MassEquation(final String partName) {
        this.name = genName(partName);
        this.flowRate = 0.;
    }

    /**
     * Get the name of the additional equation. The name is in the following form : "MASS_<part name>".
     * 
     * @return name of the additional equation with the prefix "MASS_"
     */
    @Override
    public String getName() {
        return this.name;
    }

    /** {@inheritDoc} */
    @Override
    public void computeDerivatives(final SpacecraftState s,
                                   final TimeDerivativesEquations adder) throws PatriusException {
        // The mass flow-rate should be negative
        if (this.flowRate > 0) {
            throw PatriusException.createIllegalArgumentException(
                PatriusMessages.POSITIVE_FLOW_RATE, this.flowRate);
        }

        adder.addAdditionalStateDerivative(this.getName(), new double[] { this.flowRate });
    }

    /**
     * Set the flow rate to zero.
     * 
     * @since 2.3.1
     */
    public void setMassDerivativeZero() {
        this.flowRate = 0;
    }

    /**
     * Set the flow rate.
     * 
     * @param flow
     *        rate
     */
    public void addMassDerivative(final double flow) {
        this.flowRate += flow;
    }

    /**
     * Generate a name in the form "MASS_<part name>" for this equation.
     * 
     * @param name
     *        part name
     * @return full name of the equation
     */
    public static String genName(final String name) {
        return PREFIX + name;
    }

    /** {@inheritDoc} */
    @Override
    public double[] computeSecondDerivatives(final SpacecraftState s) throws PatriusException {
        return new double[] { 0. };
    }

    /** {@inheritDoc} */
    @Override
    public int getFirstOrderDimension() {
        return 1;
    }

    /** {@inheritDoc} */
    @Override
    public int getSecondOrderDimension() {
        return 1;
    }

    /** {@inheritDoc} */
    @Override
    public double[] buildAdditionalState(final double[] y,
            final double[] yDot) {
        return y;
    }

    /** {@inheritDoc} */
    @Override
    public double[] extractY(final double[] additionalState) {
        return additionalState;
    }

    /** {@inheritDoc} */
    @Override
    public double[] extractYDot(final double[] additionalState) {
        return new double[] { flowRate };
    }

    /** {@inheritDoc} */
    @Override
    public void writeExternal(final ObjectOutput oo) throws IOException {
        oo.writeObject(name);
        oo.writeDouble(flowRate);
    }

    /** {@inheritDoc} */
    @Override
    public void readExternal(final ObjectInput oi) throws IOException, ClassNotFoundException {
        name = (String) oi.readObject();
        flowRate = oi.readDouble();
    }
}
