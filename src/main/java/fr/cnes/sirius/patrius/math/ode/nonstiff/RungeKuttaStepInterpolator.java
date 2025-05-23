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
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.ode.nonstiff;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import fr.cnes.sirius.patrius.math.ode.AbstractIntegrator;
import fr.cnes.sirius.patrius.math.ode.EquationsMapper;
import fr.cnes.sirius.patrius.math.ode.sampling.AbstractStepInterpolator;

/**
 * This class represents an interpolator over the last step during an
 * ODE integration for Runge-Kutta and embedded Runge-Kutta integrators.
 * 
 * @see RungeKuttaIntegrator
 * @see EmbeddedRungeKuttaIntegrator
 * 
 * @version $Id: RungeKuttaStepInterpolator.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 1.2
 */
//CHECKSTYLE: stop AbstractClassName check
@SuppressWarnings({"PMD.AbstractNaming", "PMD.NullAssignment"})
abstract class RungeKuttaStepInterpolator
    extends AbstractStepInterpolator {
    // CHECKSTYLE: resume AbstractClassName check

    /** Previous state. */
    protected double[] previousState;

    /** Slopes at the intermediate points */
    protected double[][] yDotK;

    /** Reference to the integrator. */
    // CHECKSTYLE: stop IllegalType check
    // Reason: Commons-Math code kept as such
    protected AbstractIntegrator integrator;

    // CHECKSTYLE: resume IllegalType check

    /**
     * Simple constructor.
     * This constructor builds an instance that is not usable yet, the {@link #reinitialize} method should be called
     * before using the
     * instance in order to initialize the internal arrays. This
     * constructor is used only in order to delay the initialization in
     * some cases. The {@link RungeKuttaIntegrator} and {@link EmbeddedRungeKuttaIntegrator} classes use the prototyping
     * design
     * pattern to create the step interpolators by cloning an
     * uninitialized model and latter initializing the copy.
     */
    protected RungeKuttaStepInterpolator() {
        super();
        this.previousState = null;
        this.yDotK = null;
        this.integrator = null;
    }

    /**
     * Copy constructor.
     * 
     * <p>
     * The copied interpolator should have been finalized before the copy, otherwise the copy will not be able to
     * perform correctly any interpolation and will throw a {@link NullPointerException} later. Since we don't want this
     * constructor to throw the exceptions finalization may involve and since we don't want this method to modify the
     * state of the copied interpolator, finalization is <strong>not</strong> done automatically, it remains under user
     * control.
     * </p>
     * 
     * <p>
     * The copy is a deep copy: its arrays are separated from the original arrays of the instance.
     * </p>
     * 
     * @param interpolator
     *        interpolator to copy from.
     */
    public RungeKuttaStepInterpolator(final RungeKuttaStepInterpolator interpolator) {

        super(interpolator);

        if (interpolator.currentState == null) {
            this.previousState = null;
            this.yDotK = null;
        } else {
            this.previousState = interpolator.previousState.clone();

            this.yDotK = new double[interpolator.yDotK.length][];
            for (int k = 0; k < interpolator.yDotK.length; ++k) {
                this.yDotK[k] = interpolator.yDotK[k].clone();
            }
        }

        // we cannot keep any reference to the equations in the copy
        // the interpolator should have been finalized before
        this.integrator = null;

    }

    /**
     * Reinitialize the instance
     * <p>
     * Some Runge-Kutta integrators need fewer functions evaluations than their counterpart step interpolators. So the
     * interpolator should perform the last evaluations they need by themselves. The {@link RungeKuttaIntegrator
     * RungeKuttaIntegrator} and {@link EmbeddedRungeKuttaIntegrator EmbeddedRungeKuttaIntegrator} abstract classes call
     * this method in order to let the step interpolator perform the evaluations it needs. These evaluations will be
     * performed during the call to <code>doFinalize</code> if any, i.e. only if the step handler either calls the
     * {@link AbstractStepInterpolator#finalizeStep finalizeStep} method or the
     * {@link AbstractStepInterpolator#getInterpolatedState
     * getInterpolatedState} method (for an interpolator which needs a finalization) or if it clones the step
     * interpolator.
     * </p>
     * 
     * @param rkIntegrator
     *        integrator being used
     * @param y
     *        reference to the integrator array holding the state at
     *        the end of the step
     * @param yDotArray
     *        reference to the integrator array holding all the
     *        intermediate slopes
     * @param forward
     *        integration direction indicator
     * @param primaryMapper
     *        equations mapper for the primary equations set
     * @param secondaryMappers
     *        equations mappers for the secondary equations sets
     */
    // CHECKSTYLE: stop IllegalType check
    // Reason: Commons-Math code kept as such
    public void reinitialize(final AbstractIntegrator rkIntegrator,
                             final double[] y, final double[][] yDotArray, final boolean forward,
                             final EquationsMapper primaryMapper,
                             final EquationsMapper[] secondaryMappers) {
        // CHECKSTYLE: resume IllegalType check
        this.reinitialize(y, forward, primaryMapper, secondaryMappers);
        this.previousState = null;
        this.yDotK = yDotArray;
        this.integrator = rkIntegrator;
    }

    /** {@inheritDoc} */
    @Override
    public void shift() {
        this.previousState = this.currentState.clone();
        super.shift();
    }

    /** {@inheritDoc} */
    @Override
    public void writeExternal(final ObjectOutput oo) throws IOException {

        // save the state of the base class
        this.writeBaseExternal(oo);

        // save the local attributes
        final int n = (this.currentState == null) ? -1 : this.currentState.length;
        for (int i = 0; i < n; ++i) {
            oo.writeDouble(this.previousState[i]);
        }

        final int kMax = (this.yDotK == null) ? -1 : this.yDotK.length;
        oo.writeInt(kMax);
        for (int k = 0; k < kMax; ++k) {
            for (int i = 0; i < n; ++i) {
                oo.writeDouble(this.yDotK[k][i]);
            }
        }

        // we do not save any reference to the equations

    }

    /** {@inheritDoc} */
    @Override
    public void readExternal(final ObjectInput oi) throws IOException, ClassNotFoundException {

        // read the base class
        final double t = this.readBaseExternal(oi);

        // read the local attributes
        final int n = (this.currentState == null) ? -1 : this.currentState.length;
        if (n < 0) {
            // no previous state
            this.previousState = null;
        } else {
            // read previous state
            this.previousState = new double[n];
            for (int i = 0; i < n; ++i) {
                this.previousState[i] = oi.readDouble();
            }
        }

        // Read derivatives
        final int kMax = oi.readInt();
        this.yDotK = (kMax < 0) ? null : new double[kMax][];
        for (int k = 0; k < kMax; ++k) {
            this.yDotK[k] = (n < 0) ? null : new double[n];
            for (int i = 0; i < n; ++i) {
                this.yDotK[k][i] = oi.readDouble();
            }
        }

        this.integrator = null;

        if (this.currentState == null) {
            // there is no current state, initialize interpolated time
            this.interpolatedTime = t;
        } else {
            // we can now set the interpolated time and state
            this.setInterpolatedTime(t);
        }

    }

}
