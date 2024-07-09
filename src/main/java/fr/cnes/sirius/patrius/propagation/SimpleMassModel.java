/**
 *
 * Copyright 2011-2017 CNES
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
* VERSION:4.6:DM:DM-2571:27/01/2021:[PATRIUS] Integrateur Stormer-Cowell 
* VERSION:4.6:FA:FA-2741:27/01/2021:[PATRIUS] Chaine de transformation de repere non optimale dans MSIS2000
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:184:17/12/2013:Throw exception if mass becomes negative. Fixed javadoc.
 * VERSION::FA:86:22/10/2013:Created the a simple MassProvider model
 * VERSION::FA:183:14/03/2014:Corrected javadoc
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::FA:373:12/01/2015: proper handling of mass event detection
 * VERSION::FA:673:12/09/2016: add getTotalMass(state)
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.List;

import fr.cnes.sirius.patrius.propagation.numerical.AdditionalEquations;
import fr.cnes.sirius.patrius.propagation.numerical.TimeDerivativesEquations;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusExceptionWrapper;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Simple implementation of {@link MassProvider}. The mass DOESNT vary!
 * 
 * @concurrency immutable
 * 
 * @author Rami Houdroge
 * @since 2.1
 * @version $Id: SimpleMassModel.java 18092 2017-10-02 17:12:58Z bignon $
 */
public class SimpleMassModel implements MassProvider {

    /** Serial UID. */
    private static final long serialVersionUID = -8616468892552908329L;

    /** The mass value. */
    private double myMass;
    /** The mass equation. */
    private final MassEquation eq;
    /** The mass model name. */
    private final String modelName;

    /**
     * Empty constructor for {@link Externalizable} use.
     */
    public SimpleMassModel() {
        this(0, "");
    }

    /**
     * Constructor. Simple variable mass model.<br>
     * The mass model name specified here is the name to be used with the
     * constructors of the thrust maneuvers.
     * 
     * @param mass
     *        mass of spacecraft
     * @param name
     *        mass model name
     */
    public SimpleMassModel(final double mass, final String name) {

        if (mass < 0) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.NOT_POSITIVE_MASS);
        }

        this.myMass = mass;
        this.modelName = name;
        this.eq = new MassEquation(name);
    }

    /** {@inheritDoc} */
    @Override
    public double getTotalMass() {
        return this.myMass;
    }

    /** {@inheritDoc} */
    @Override
    public double getTotalMass(final SpacecraftState state) {

        try {
            double mass = 0.;
            // Try to retrieve mass from state vector
            if (state.getAdditionalStatesMass().size() == 0) {
                // Mass is not in state vector, retrieve mass from mass provider
                mass = this.getTotalMass();
            } else {
                mass = state.getMass(this.modelName);
            }

            return mass;

        } catch (final PatriusException e) {
            // It cannot happen since a check on mass existence has been performed before
            throw new PatriusExceptionWrapper(e);
        }
    }

    /**
     * This model represents one part only. The expected partName is the name of
     * the model given at construction time. {@inheritDoc}
     */
    @Override
    public double getMass(final String partName) {
        this.checkName(partName);
        return this.myMass;
    }

    /**
     * This model represents one part only. The expected partName is the name of
     * the model given at construction time. {@inheritDoc}
     */
    @Override
    public void updateMass(final String partName, final double mass) {
        this.checkName(partName);
        this.myMass = mass;
    }

    /**
     * This model represents one part only. The expected partName is the name of
     * the model given at construction time. {@inheritDoc}
     */
    @Override
    public void addMassDerivative(final String partName, final double flowRate) {
        this.checkName(partName);
        this.eq.addMassDerivative(flowRate);
    }

    /** {@inheritDoc} */
    @Override
    public void setMassDerivativeZero(final String partName) {
        this.checkName(partName);
        this.eq.setMassDerivativeZero();
    }

    /**
     * Make sure this is the intended model.
     * 
     * @param partName
     *        the model name
     */
    private void checkName(final String partName) {
        if (!partName.contentEquals(this.modelName)) {
            throw new PatriusExceptionWrapper(new PatriusException(
                PatriusMessages.NO_VARIABLE_MASS_MODEL_FOUND, partName));
        }
    }

    /** {@inheritDoc} */
    @Override
    public AdditionalEquations getAdditionalEquation(final String name) {
        return this.eq;
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getAllPartsNames() {
        final List<String> list = new ArrayList<String>();
        list.add(this.modelName);
        return list;
    }

    /**
     * Mass equation.
     */
    class MassEquation implements AdditionalEquations {

        /** Generated Serial UID. */
        private static final long serialVersionUID = 4585447992608363108L;
        /** Equation name. */
        private String name;
        /** flow rate. */
        private double flowRate;

        /**
         * Empty constructor for {@link Externalizable} use.
         */
        public MassEquation() {
            this.name = "";
            this.flowRate = 0.;
        }

        /**
         * Create a new mass equation, for a given part with a name in the form "MASS_<name>".
         * 
         * @param nameIn
         *        the name of the mass model
         */
        protected MassEquation(final String nameIn) {
            this.name = MASS + nameIn;
            this.flowRate = 0.;
        }

        /** {@inheritDoc} */
        @Override
        public String getName() {
            return this.name;
        }

        /** {@inheritDoc} */
        @Override
        public void computeDerivatives(final SpacecraftState s,
                                       final TimeDerivativesEquations adder) throws PatriusException {

            // current value of the additional parameters
            final double[] p = s.getAdditionalState(this.getName());

            // Update part mass and associated additional equation
            SimpleMassModel.this.updateMass(SimpleMassModel.this.modelName, p[0]);
            adder.addAdditionalStateDerivative(this.getName(), new double[] { this.flowRate });
        }

        /**
         * Set the flow rate.
         * 
         * @param flow
         *        the flow rate
         */
        protected void addMassDerivative(final double flow) {
            this.flowRate += flow;
        }

        /**
         * Set the flow rate to zero.
         */
        protected void setMassDerivativeZero() {
            this.flowRate = 0.;
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
}
