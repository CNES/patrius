/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 *
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
 */
/*
 * HISTORY
* VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:226:12/09/2014: problem with event detections.
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.math.ode.sampling;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import fr.cnes.sirius.patrius.math.exception.MaxCountExceededException;
import fr.cnes.sirius.patrius.math.ode.EquationsMapper;

/**
 * This abstract class represents an interpolator over the last step
 * during an ODE integration.
 * 
 * <p>
 * The various ODE integrators provide objects extending this class to the step handlers. The handlers can use these
 * objects to retrieve the state vector at intermediate times between the previous and the current grid points (dense
 * output).
 * </p>
 * 
 * @see fr.cnes.sirius.patrius.math.ode.FirstOrderIntegrator
 * @see fr.cnes.sirius.patrius.math.ode.SecondOrderIntegrator
 * @see StepHandler
 * 
 * @version $Id: AbstractStepInterpolator.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 1.2
 * 
 */
@SuppressWarnings("PMD.NullAssignment")
public abstract class AbstractStepInterpolator
    implements StepInterpolator {

    /** current time step */
    protected double h;

    /** current state */
    protected double[] currentState;

    /** interpolated time */
    protected double interpolatedTime;

    /** interpolated state */
    protected double[] interpolatedState;

    /** interpolated derivatives */
    protected double[] interpolatedDerivatives;

    /** interpolated primary state */
    protected double[] interpolatedPrimaryState;

    /** interpolated primary derivatives */
    protected double[] interpolatedPrimaryDerivatives;

    /** interpolated secondary state */
    protected double[][] interpolatedSecondaryState;

    /** interpolated secondary derivatives */
    protected double[][] interpolatedSecondaryDerivatives;

    /** global previous time */
    private double globalPreviousTime;

    /** global current time */
    private double globalCurrentTime;

    /** soft previous time */
    private double softPreviousTime;

    /** soft current time */
    private double softCurrentTime;

    /** indicate if the step has been finalized or not. */
    private boolean finalized;

    /** integration direction. */
    private boolean forward;

    /** indicator for dirty state. */
    private boolean dirtyState;

    /** Equations mapper for the primary equations set. */
    private EquationsMapper primaryMapper;

    /** Equations mappers for the secondary equations sets. */
    private EquationsMapper[] secondaryMappers;

    /**
     * Simple constructor.
     * This constructor builds an instance that is not usable yet, the {@link #reinitialize} method should be called
     * before using the
     * instance in order to initialize the internal arrays. This
     * constructor is used only in order to delay the initialization in
     * some cases. As an example, the {@link fr.cnes.sirius.patrius.math.ode.nonstiff.EmbeddedRungeKuttaIntegrator}
     * class uses the prototyping design pattern to create the step
     * interpolators by cloning an uninitialized model and latter
     * initializing the copy.
     */
    protected AbstractStepInterpolator() {
        this.globalPreviousTime = Double.NaN;
        this.globalCurrentTime = Double.NaN;
        this.softPreviousTime = Double.NaN;
        this.softCurrentTime = Double.NaN;
        this.h = Double.NaN;
        this.interpolatedTime = Double.NaN;
        this.currentState = null;
        this.finalized = false;
        this.forward = true;
        this.dirtyState = true;
        this.primaryMapper = null;
        this.secondaryMappers = null;
        this.allocateInterpolatedArrays(-1);
    }

    /**
     * Simple constructor.
     * 
     * @param y
     *        reference to the integrator array holding the state at
     *        the end of the step
     * @param forwardIn
     *        integration direction indicator
     * @param primaryMapperIn
     *        equations mapper for the primary equations set
     * @param secondaryMappersIn
     *        equations mappers for the secondary equations sets
     */
    protected AbstractStepInterpolator(final double[] y, final boolean forwardIn,
        final EquationsMapper primaryMapperIn,
        final EquationsMapper[] secondaryMappersIn) {

        this.globalPreviousTime = Double.NaN;
        this.globalCurrentTime = Double.NaN;
        this.softPreviousTime = Double.NaN;
        this.softCurrentTime = Double.NaN;
        this.h = Double.NaN;
        this.interpolatedTime = Double.NaN;
        this.currentState = y;
        this.finalized = false;
        this.forward = forwardIn;
        this.dirtyState = true;
        this.primaryMapper = primaryMapperIn;
        this.secondaryMappers = (secondaryMappersIn == null) ? null : secondaryMappersIn.clone();
        this.allocateInterpolatedArrays(y.length);

    }

    /**
     * Copy constructor.
     * 
     * <p>
     * The copied interpolator should have been finalized before the copy, otherwise the copy will not be able to
     * perform correctly any derivative computation and will throw a {@link NullPointerException} later. Since we don't
     * want this constructor to throw the exceptions finalization may involve and since we don't want this method to
     * modify the state of the copied interpolator, finalization is <strong>not</strong> done automatically, it remains
     * under user control.
     * </p>
     * 
     * <p>
     * The copy is a deep copy: its arrays are separated from the original arrays of the instance.
     * </p>
     * 
     * @param interpolator
     *        interpolator to copy from.
     */
    protected AbstractStepInterpolator(final AbstractStepInterpolator interpolator) {

        this.globalPreviousTime = interpolator.globalPreviousTime;
        this.globalCurrentTime = interpolator.globalCurrentTime;
        this.softPreviousTime = interpolator.softPreviousTime;
        this.softCurrentTime = interpolator.softCurrentTime;
        this.h = interpolator.h;
        this.interpolatedTime = interpolator.interpolatedTime;

        if (interpolator.currentState == null) {
            this.currentState = null;
            this.primaryMapper = null;
            this.secondaryMappers = null;
            this.allocateInterpolatedArrays(-1);
        } else {
            this.currentState = interpolator.currentState.clone();
            this.interpolatedState = interpolator.interpolatedState.clone();
            this.interpolatedDerivatives = interpolator.interpolatedDerivatives.clone();
            this.interpolatedPrimaryState = interpolator.interpolatedPrimaryState.clone();
            this.interpolatedPrimaryDerivatives = interpolator.interpolatedPrimaryDerivatives.clone();
            this.interpolatedSecondaryState = new double[interpolator.interpolatedSecondaryState.length][];
            this.interpolatedSecondaryDerivatives = new double[interpolator.interpolatedSecondaryDerivatives.length][];
            for (int i = 0; i < this.interpolatedSecondaryState.length; ++i) {
                this.interpolatedSecondaryState[i] = interpolator.interpolatedSecondaryState[i].clone();
                this.interpolatedSecondaryDerivatives[i] = interpolator.interpolatedSecondaryDerivatives[i].clone();
            }
        }

        this.finalized = interpolator.finalized;
        this.forward = interpolator.forward;
        this.dirtyState = interpolator.dirtyState;
        this.primaryMapper = interpolator.primaryMapper;
        this.secondaryMappers = (interpolator.secondaryMappers == null) ?
            null : interpolator.secondaryMappers.clone();

    }

    /**
     * Allocate the various interpolated states arrays.
     * 
     * @param dimension
     *        total dimension (negative if arrays should be set to null)
     */
    private void allocateInterpolatedArrays(final int dimension) {
        // Initialization
        if (dimension < 0) {
            // Undefined dimension
            // Arrays set to null
            this.interpolatedState = null;
            this.interpolatedDerivatives = null;
            this.interpolatedPrimaryState = null;
            this.interpolatedPrimaryDerivatives = null;
            this.interpolatedSecondaryState = null;
            this.interpolatedSecondaryDerivatives = null;
        } else {
            // General case
            //
            this.interpolatedState = new double[dimension];
            this.interpolatedDerivatives = new double[dimension];
            this.interpolatedPrimaryState = new double[this.primaryMapper.getDimension()];
            this.interpolatedPrimaryDerivatives = new double[this.primaryMapper.getDimension()];
            if (this.secondaryMappers == null) {
                this.interpolatedSecondaryState = null;
                this.interpolatedSecondaryDerivatives = null;
            } else {
                this.interpolatedSecondaryState = new double[this.secondaryMappers.length][];
                this.interpolatedSecondaryDerivatives = new double[this.secondaryMappers.length][];
                for (int i = 0; i < this.secondaryMappers.length; ++i) {
                    this.interpolatedSecondaryState[i] = new double[this.secondaryMappers[i].getDimension()];
                    this.interpolatedSecondaryDerivatives[i] = new double[this.secondaryMappers[i].getDimension()];
                }
            }
        }
    }

    /**
     * Reinitialize the instance
     * 
     * @param y
     *        reference to the integrator array holding the state at the end of the step
     * @param isForward
     *        integration direction indicator
     * @param primary
     *        equations mapper for the primary equations set
     * @param secondary
     *        equations mappers for the secondary equations sets
     */
    public void reinitialize(final double[] y, final boolean isForward,
                             final EquationsMapper primary,
                             final EquationsMapper[] secondary) {
        // Initialization
        //
        this.globalPreviousTime = Double.NaN;
        this.globalCurrentTime = Double.NaN;
        this.softPreviousTime = Double.NaN;
        this.softCurrentTime = Double.NaN;
        this.h = Double.NaN;
        this.interpolatedTime = Double.NaN;
        this.currentState = y;
        this.finalized = false;
        this.forward = isForward;
        this.dirtyState = true;
        this.primaryMapper = primary;
        this.secondaryMappers = secondary.clone();
        // Allocate arrays
        //
        this.allocateInterpolatedArrays(y.length);

    }

    /** {@inheritDoc} */
    @Override
    public StepInterpolator copy() {

        // finalize the step before performing copy
        this.finalizeStep();

        // create the new independent instance
        return this.doCopy();

    }

    /**
     * Really copy the finalized instance.
     * <p>
     * This method is called by {@link #copy()} after the step has been finalized. It must perform a deep copy to have
     * an new instance completely independent for the original instance.
     * 
     * @return a copy of the finalized instance
     */
    protected abstract StepInterpolator doCopy();

    /**
     * Shift one step forward.
     * Copy the current time into the previous time, hence preparing the
     * interpolator for future calls to {@link #storeTime storeTime}
     */
    public void shift() {
        this.globalPreviousTime = this.globalCurrentTime;
        this.softPreviousTime = this.globalPreviousTime;
        this.softCurrentTime = this.globalCurrentTime;
    }

    /**
     * Store the current step time.
     * 
     * @param t
     *        current time
     */
    public void storeTime(final double t) {

        this.globalCurrentTime = t;
        this.softCurrentTime = this.globalCurrentTime;
        this.h = this.globalCurrentTime - this.globalPreviousTime;
        this.setInterpolatedTime(t);

        // the step is not finalized anymore
        this.finalized = false;

    }

    /**
     * Restrict step range to a limited part of the global step.
     * <p>
     * This method can be used to restrict a step and make it appear as if the original step was smaller. Calling this
     * method <em>only</em> changes the value returned by {@link #getPreviousTime()}, it does not change any other
     * property
     * </p>
     * 
     * @param softPreviousTimeIn
     *        start of the restricted step
     * @since 2.2
     */
    public void setSoftPreviousTime(final double softPreviousTimeIn) {
        this.softPreviousTime = softPreviousTimeIn;
    }

    /**
     * Restrict step range to a limited part of the global step.
     * <p>
     * This method can be used to restrict a step and make it appear as if the original step was smaller. Calling this
     * method <em>only</em> changes the value returned by {@link #getCurrentTime()}, it does not change any other
     * property
     * </p>
     * 
     * @param softCurrentTimeIn
     *        end of the restricted step
     * @since 2.2
     */
    public void setSoftCurrentTime(final double softCurrentTimeIn) {
        this.softCurrentTime = softCurrentTimeIn;
    }

    /**
     * Get the previous global grid point time.
     * 
     * @return previous global grid point time
     */
    public double getGlobalPreviousTime() {
        return this.globalPreviousTime;
    }

    /**
     * Get the current global grid point time.
     * 
     * @return current global grid point time
     */
    public double getGlobalCurrentTime() {
        return this.globalCurrentTime;
    }

    /**
     * Get the previous soft grid point time.
     * 
     * @return previous soft grid point time
     * @see #setSoftPreviousTime(double)
     */
    @Override
    public double getPreviousTime() {
        return this.softPreviousTime;
    }

    /**
     * Get the current soft grid point time.
     * 
     * @return current soft grid point time
     * @see #setSoftCurrentTime(double)
     */
    @Override
    public double getCurrentTime() {
        return this.softCurrentTime;
    }

    /** {@inheritDoc} */
    @Override
    public double getInterpolatedTime() {
        return this.interpolatedTime;
    }

    /** {@inheritDoc} */
    @Override
    public void setInterpolatedTime(final double time) {
        this.interpolatedTime = time;
        this.dirtyState = true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isForward() {
        return this.forward;
    }

    /**
     * Compute the state and derivatives at the interpolated time.
     * This is the main processing method that should be implemented by
     * the derived classes to perform the interpolation.
     * 
     * @param theta
     *        normalized interpolation abscissa within the step
     *        (theta is zero at the previous time step and one at the current time step)
     * @param oneMinusThetaH
     *        time gap between the interpolated time and
     *        the current time
     * @exception MaxCountExceededException
     *            if the number of functions evaluations is exceeded
     */
    protected abstract void computeInterpolatedStateAndDerivatives(double theta,
                                                                   double oneMinusThetaH);

    /**
     * Lazy evaluation of complete interpolated state.
     * 
     * @exception MaxCountExceededException
     *            if the number of functions evaluations is exceeded
     */
    private void evaluateCompleteInterpolatedState() {
        // lazy evaluation of the state
        if (this.dirtyState) {
            final double oneMinusThetaH = this.globalCurrentTime - this.interpolatedTime;
            final double theta = (this.h == 0) ? 0 : (this.h - oneMinusThetaH) / this.h;
            this.computeInterpolatedStateAndDerivatives(theta, oneMinusThetaH);
            this.dirtyState = false;
        }
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    public double[] getInterpolatedState() {
        this.evaluateCompleteInterpolatedState();
        this.primaryMapper.extractEquationData(this.interpolatedState,
            this.interpolatedPrimaryState);
        return this.interpolatedPrimaryState;
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    public double[] getInterpolatedDerivatives() {
        this.evaluateCompleteInterpolatedState();
        this.primaryMapper.extractEquationData(this.interpolatedDerivatives,
            this.interpolatedPrimaryDerivatives);
        return this.interpolatedPrimaryDerivatives;
    }

    /** {@inheritDoc} */
    @Override
    public double[] getInterpolatedSecondaryState(final int index) {
        this.evaluateCompleteInterpolatedState();
        this.secondaryMappers[index].extractEquationData(this.interpolatedState,
            this.interpolatedSecondaryState[index]);
        return this.interpolatedSecondaryState[index];
    }

    /** {@inheritDoc} */
    @Override
    public double[] getInterpolatedSecondaryDerivatives(final int index) {
        this.evaluateCompleteInterpolatedState();
        this.secondaryMappers[index].extractEquationData(this.interpolatedDerivatives,
            this.interpolatedSecondaryDerivatives[index]);
        return this.interpolatedSecondaryDerivatives[index];
    }

    /**
     * Finalize the step.
     * 
     * <p>
     * Some embedded Runge-Kutta integrators need fewer functions evaluations than their counterpart step interpolators.
     * These interpolators should perform the last evaluations they need by themselves only if they need them. This
     * method triggers these extra evaluations. It can be called directly by the user step handler and it is called
     * automatically if {@link #setInterpolatedTime} is called.
     * </p>
     * 
     * <p>
     * Once this method has been called, <strong>no</strong> other evaluation will be performed on this step. If there
     * is a need to have some side effects between the step handler and the differential equations (for example update
     * some data in the equations once the step has been done), it is advised to call this method explicitly from the
     * step handler before these side effects are set up. If the step handler induces no side effect, then this method
     * can safely be ignored, it will be called transparently as needed.
     * </p>
     * 
     * <p>
     * <strong>Warning</strong>: since the step interpolator provided to the step handler as a parameter of the
     * {@link StepHandler#handleStep handleStep} is valid only for the duration of the {@link StepHandler#handleStep
     * handleStep} call, one cannot simply store a reference and reuse it later. One should first finalize the instance,
     * then copy this finalized instance into a new object that can be kept.
     * </p>
     * 
     * <p>
     * This method calls the protected <code>doFinalize</code> method if it has never been called during this step and
     * set a flag indicating that it has been called once. It is the <code>
     * doFinalize</code> method which should perform the evaluations. This wrapping prevents from calling
     * <code>doFinalize</code> several times and hence evaluating the differential equations too often. Therefore,
     * subclasses are not allowed not reimplement it, they should rather reimplement <code>doFinalize</code>.
     * </p>
     * 
     * @exception MaxCountExceededException
     *            if the number of functions evaluations is exceeded
     */
    public final void finalizeStep() {
        if (!this.finalized) {
            this.doFinalize();
            this.finalized = true;
        }
    }

    /**
     * Really finalize the step.
     * The default implementation of this method does nothing.
     * 
     * @exception MaxCountExceededException
     *            if the number of functions evaluations is exceeded
     */
    @SuppressWarnings("PMD.EmptyMethodInAbstractClassShouldBeAbstract")
    protected void doFinalize() {
        // Nothing to do
    }

    /** {@inheritDoc} */
    @Override
    public abstract void writeExternal(ObjectOutput oo) throws IOException;

    /** {@inheritDoc} */
    @Override
    public abstract void readExternal(ObjectInput oi) throws IOException, ClassNotFoundException;

    /**
     * Save the base state of the instance.
     * This method performs step finalization if it has not been done
     * before.
     * 
     * @param oo
     *        stream where to save the state
     * @exception IOException
     *            in case of write error
     */
    protected void writeBaseExternal(final ObjectOutput oo) throws IOException {

        // Write data
        if (this.currentState == null) {
            // No state
            oo.writeInt(-1);
        } else {
            oo.writeInt(this.currentState.length);
        }
        oo.writeDouble(this.globalPreviousTime);
        oo.writeDouble(this.globalCurrentTime);
        oo.writeDouble(this.softPreviousTime);
        oo.writeDouble(this.softCurrentTime);
        oo.writeDouble(this.h);
        oo.writeBoolean(this.forward);
        oo.writeObject(this.primaryMapper);
        oo.write(this.secondaryMappers.length);
        for (final EquationsMapper mapper : this.secondaryMappers) {
            oo.writeObject(mapper);
        }

        // Write state
        if (this.currentState != null) {
            for (final double element : this.currentState) {
                oo.writeDouble(element);
            }
        }

        oo.writeDouble(this.interpolatedTime);

        // we do not store the interpolated state,
        // it will be recomputed as needed after reading

        try {
            // finalize the step (and don't bother saving the now true flag)
            this.finalizeStep();
        } catch (final MaxCountExceededException mcee) {
            final IOException ioe = new IOException(mcee.getLocalizedMessage());
            ioe.initCause(mcee);
            throw ioe;
        }

    }

    /**
     * Read the base state of the instance.
     * This method does <strong>neither</strong> set the interpolated
     * time nor state. It is up to the derived class to reset it
     * properly calling the {@link #setInterpolatedTime} method later,
     * once all rest of the object state has been set up properly.
     * 
     * @param oi
     *        stream where to read the state from
     * @return interpolated time to be set later by the caller
     * @exception IOException
     *            in case of read error
     * @exception ClassNotFoundException
     *            if an equation mapper class
     *            cannot be found
     */
    protected double readBaseExternal(final ObjectInput oi) throws IOException, ClassNotFoundException {

        // Dimension
        final int dimension = oi.readInt();
        // Read data :
        // Time data
        this.globalPreviousTime = oi.readDouble();
        this.globalCurrentTime = oi.readDouble();
        this.softPreviousTime = oi.readDouble();
        this.softCurrentTime = oi.readDouble();
        this.h = oi.readDouble();
        // integration direction
        this.forward = oi.readBoolean();
        // Equation mappers
        this.primaryMapper = (EquationsMapper) oi.readObject();
        this.secondaryMappers = new EquationsMapper[oi.read()];
        for (int i = 0; i < this.secondaryMappers.length; ++i) {
            this.secondaryMappers[i] = (EquationsMapper) oi.readObject();
        }
        this.dirtyState = true;

        if (dimension < 0) {
            this.currentState = null;
        } else {
            // Read state
            this.currentState = new double[dimension];
            for (int i = 0; i < this.currentState.length; ++i) {
                this.currentState[i] = oi.readDouble();
            }
        }

        // we do NOT handle the interpolated time and state here
        this.interpolatedTime = Double.NaN;
        this.allocateInterpolatedArrays(dimension);

        this.finalized = true;

        // Return last data
        return oi.readDouble();

    }

}
