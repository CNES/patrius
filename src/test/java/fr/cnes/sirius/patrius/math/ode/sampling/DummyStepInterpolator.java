/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.ode.sampling;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import fr.cnes.sirius.patrius.math.ode.EquationsMapper;

/**
 * This class is a step interpolator that does nothing.
 * 
 * <p>
 * This class is used when the {@link StepHandler "step handler"} set up by the user does not need step interpolation.
 * It does not recompute the state when {@link AbstractStepInterpolator#setInterpolatedTime
 * setInterpolatedTime} is called. This implies the interpolated state is always the state at the end of the current
 * step.
 * </p>
 * 
 * @see StepHandler
 * 
 * @version $Id: DummyStepInterpolator.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 1.2
 */

public class DummyStepInterpolator
    extends AbstractStepInterpolator {

     /** Serializable UID. */
    private static final long serialVersionUID = 1708010296707839488L;

    /** Current derivative. */
    private double[] currentDerivative;

    /**
     * Simple constructor.
     * This constructor builds an instance that is not usable yet, the
     * <code>AbstractStepInterpolator.reinitialize</code> protected method
     * should be called before using the instance in order to initialize
     * the internal arrays. This constructor is used only in order to delay
     * the initialization in some cases. As an example, the
     * {@link fr.cnes.sirius.patrius.math.ode.nonstiff.EmbeddedRungeKuttaIntegrator} uses
     * the prototyping design pattern to create the step interpolators by
     * cloning an uninitialized model and latter initializing the copy.
     */
    public DummyStepInterpolator() {
        super();
        this.currentDerivative = null;
    }

    /**
     * Simple constructor.
     * 
     * @param y
     *        reference to the integrator array holding the state at
     *        the end of the step
     * @param yDot
     *        reference to the integrator array holding the state
     *        derivative at some arbitrary point within the step
     * @param forward
     *        integration direction indicator
     */
    public DummyStepInterpolator(final double[] y, final double[] yDot, final boolean forward) {
        super(y, forward, new EquationsMapper(0, y.length), new EquationsMapper[0]);
        this.currentDerivative = yDot;
    }

    /**
     * Copy constructor.
     * 
     * @param interpolator
     *        interpolator to copy from. The copy is a deep
     *        copy: its arrays are separated from the original arrays of the
     *        instance
     */
    public DummyStepInterpolator(final DummyStepInterpolator interpolator) {
        super(interpolator);
        this.currentDerivative = interpolator.currentDerivative.clone();
    }

    /**
     * Really copy the finalized instance.
     * 
     * @return a copy of the finalized instance
     */
    @Override
    protected StepInterpolator doCopy() {
        return new DummyStepInterpolator(this);
    }

    /**
     * Compute the state at the interpolated time.
     * In this class, this method does nothing: the interpolated state
     * is always the state at the end of the current step.
     * 
     * @param theta
     *        normalized interpolation abscissa within the step
     *        (theta is zero at the previous time step and one at the current time step)
     * @param oneMinusThetaH
     *        time gap between the interpolated time and
     *        the current time
     */
    @Override
    protected void computeInterpolatedStateAndDerivatives(final double theta, final double oneMinusThetaH) {
        System.arraycopy(this.currentState, 0, this.interpolatedState, 0, this.currentState.length);
        System.arraycopy(this.currentDerivative, 0, this.interpolatedDerivatives, 0, this.currentDerivative.length);
    }

    /**
     * Write the instance to an output channel.
     * 
     * @param out
     *        output channel
     * @exception IOException
     *            if the instance cannot be written
     */
    @Override
    public void writeExternal(final ObjectOutput out)
                                                     throws IOException {

        // save the state of the base class
        this.writeBaseExternal(out);

        if (this.currentDerivative != null) {
            for (final double element : this.currentDerivative) {
                out.writeDouble(element);
            }
        }

    }

    /**
     * Read the instance from an input channel.
     * 
     * @param in
     *        input channel
     * @exception IOException
     *            if the instance cannot be read
     */
    @Override
    public void readExternal(final ObjectInput in)
                                                  throws IOException, ClassNotFoundException {

        // read the base class
        final double t = this.readBaseExternal(in);

        if (this.currentState == null) {
            this.currentDerivative = null;
        } else {
            this.currentDerivative = new double[this.currentState.length];
            for (int i = 0; i < this.currentDerivative.length; ++i) {
                this.currentDerivative[i] = in.readDouble();
            }
        }

        // we can now set the interpolated time and state
        this.setInterpolatedTime(t);

    }

}
