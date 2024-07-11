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
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.6:DM:DM-2571:27/01/2021:[PATRIUS] Integrateur Stormer-Cowell 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.ode.nonstiff.cowell;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;

import fr.cnes.sirius.patrius.math.ode.sampling.AbstractStepInterpolator;
import fr.cnes.sirius.patrius.math.ode.sampling.StepInterpolator;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Cowell interpolator.
 * This interpolator is consistent with Cowell integrator.
 * 
 * @author Emmanuel Bignon
 * 
 * @since 4.6
 */
class CowellInterpolator extends AbstractStepInterpolator {

    /** Integration support. */
    private Support support;

    /** Previous state to current state. */
    private State previousState;

    /** Current state. */
    private State currentState;

    /** Integrator order. */
    private int order;

    /** Reference integrator. */
    private final CowellIntegrator integrator;

    /** Second order to first order state mapper. */
    private SecondOrderStateMapper mapper;

    /**
     * Empty constructor for {@link Externalizable} methods use.
     */
    @SuppressWarnings("PMD.NullAssignment")
    public CowellInterpolator() {
        super();
        this.support = null;
        this.previousState = null;
        this.currentState = null;
        this.order = -1;
        this.integrator = null;
        this.mapper = null;
    }

    /**
     * Constructor.
     * @param integrator Cowell integrator
     */
    public CowellInterpolator(final CowellIntegrator integrator) {
        super();
        this.support = integrator.getSupport();
        this.previousState = integrator.getPreviousState();
        this.currentState = integrator.getCurrentState();
        this.order = integrator.getOrder();
        this.integrator = integrator;
        this.mapper = integrator.getMapper();
    }

    /**
     * Copy constructor.
     * 
     * @param interpolator
     *        interpolator to copy from. The copy is a deep
     *        copy: its arrays are separated from the original arrays of the
     *        instance
     */
    public CowellInterpolator(final CowellInterpolator interpolator) {
        super(interpolator);
        this.support = new Support(interpolator.support);
        this.previousState = new State(interpolator.previousState);
        this.currentState = new State(interpolator.currentState);
        this.order = interpolator.order;
        this.integrator = interpolator.integrator;
        this.mapper = interpolator.mapper;
    }

    /** {@inheritDoc} */
    @Override
    protected StepInterpolator doCopy() {
        return new CowellInterpolator(this);
    }

    /** {@inheritDoc} */
    @Override
    public void shift() {
        this.support = integrator.getSupport();
        this.previousState = new State(integrator.getPreviousState());
        this.currentState = new State(integrator.getCurrentState());
        super.shift();
    }
    
    /**
     * Set second order / first order state mapper.
     * @param mapper second order / first order state mapper
     */
    public void setMapper(final SecondOrderStateMapper mapper) {
        this.mapper = mapper;
    }

    /** {@inheritDoc} */
    @Override
    protected void computeInterpolatedStateAndDerivatives(final double theta,
            final double oneMinusThetaH) {

        if (theta == 0) {
            // Start of time step
            // Retrieve previous state
            this.interpolatedState = mapper.buildFullState(previousState.y, previousState.yDot);
        } else if (theta == 1) {
            // End of time step
            // Retrieve current state
            this.interpolatedState = mapper.buildFullState(currentState.y, currentState.yDot);
        } else {
            // Regular case

            // Interpolation coefficients
            final double[][] gint = new double[order + 3][order + 2];
            final double[][] gpint = new double[order + 3][order + 2];
            // Gamma associated with gint and gpint
            final double hI = -oneMinusThetaH;
            final double[] gamma1 = support.computeGamma1(hI);
            final double[] gammap = support.computeGammap(hI);

            // Interpolation step
            final double ratio = hI / support.getSteps()[support.getPreviousSize()];

            // Calculate coefficients
            for (int i = 1; i <= support.getPreviousSize() + 1; i++) {
                for (int q = 1; q <= support.getPreviousSize() + 3 - i; q++) {
                    if (i == 1) {
                        gint[q][i] = 1.0 / q;
                        gpint[q][i] = MathLib.pow(-ratio, -q) / q;
                    } else {
                        final double coef = support.hIOverPsi(hI, i - 1);
                        gint[q][i] = gamma1[i - 1] * gint[q][i - 1] - coef * gint[q + 1][i - 1];
                        gpint[q][i] = gammap[i - 1] * gpint[q][i - 1] - coef * gpint[q + 1][i - 1];
                    }
                }
            }

            // Compute interpolation values
            final double[] y = new double[currentState.y.length];
            final double[] yDot = new double[currentState.yDot.length];
            for (int i = 0; i < y.length; i++) {
                double sum1 = 0.0;
                double sum2 = 0.0;
                for (int m = 1; m <= support.getPreviousSize() + 1; m++) {
                    sum1 += gint[1][m] * support.deltaAcc[m][i];
                    sum2 += (gint[2][m] + ratio * gpint[2][m]) * support.deltaAcc[m][i];
                }
                yDot[i] = currentState.yDot[i] + hI * sum1;
                y[i] = (1.0 + ratio) * currentState.y[i] - ratio * previousState.y[i] + hI * hI * sum2;
            }

            // Second order to first order conversion
            this.interpolatedState = mapper.buildFullState(y, yDot);
        }

        // Derivatives - Not computed
        Arrays.fill(this.interpolatedDerivatives, 0);
    }

    /** {@inheritDoc} */
    @Override
    public void writeExternal(final ObjectOutput oo) throws IOException {
        // save the state of the base class
        this.writeBaseExternal(oo);

        // save the local attributes
        // All attributes used for interpolation are stored
        oo.writeInt(support == null ? -1 : 1);
        if (support != null) {
            support.writeExternal(oo);
        }
        oo.writeInt(previousState == null ? -1 : 1);
        if (previousState != null) {
            previousState.writeExternal(oo);
        }
        oo.writeInt(currentState == null ? -1 : 1);
        if (currentState != null) {
            currentState.writeExternal(oo);
        }
        oo.writeObject(mapper);
        oo.writeInt(order);
    }

    /** {@inheritDoc} */
    @Override
    public void readExternal(final ObjectInput oi) throws IOException, ClassNotFoundException {
        // read the base class
        final double t = this.readBaseExternal(oi);

        // read the local attributes
        // Status indicates if attributes is supposed to be null or not
        final int supportStatus = oi.readInt();
        if (supportStatus > 0) {
            support = new Support();
            support.readExternal(oi);
        }
        final int previousStateStatus = oi.readInt();
        if (previousStateStatus > 0) {
            previousState = new State();
            previousState.readExternal(oi);
        }
        final int currentStateStatus = oi.readInt();
        if (currentStateStatus > 0) {
            currentState = new State();
            currentState.readExternal(oi);
        }
        mapper = (SecondOrderStateMapper) oi.readObject();
        order = oi.readInt();

        // Set the interpolated time and state
        this.setInterpolatedTime(t);
    }
}
