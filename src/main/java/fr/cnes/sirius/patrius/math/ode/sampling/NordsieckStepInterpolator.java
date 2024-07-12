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
package fr.cnes.sirius.patrius.math.ode.sampling;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;

import fr.cnes.sirius.patrius.math.exception.MaxCountExceededException;
import fr.cnes.sirius.patrius.math.linear.Array2DRowRealMatrix;
import fr.cnes.sirius.patrius.math.ode.EquationsMapper;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * This class implements an interpolator for integrators using Nordsieck representation.
 * 
 * <p>
 * This interpolator computes dense output around the current point. The interpolation equation is based on Taylor
 * series formulas.
 * 
 * @see fr.cnes.sirius.patrius.math.ode.nonstiff.AdamsBashforthIntegrator
 * @see fr.cnes.sirius.patrius.math.ode.nonstiff.AdamsMoultonIntegrator
 * @version $Id: NordsieckStepInterpolator.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 2.0
 */
@SuppressWarnings("PMD.NullAssignment")
public class NordsieckStepInterpolator extends AbstractStepInterpolator {

     /** Serializable UID. */
    private static final long serialVersionUID = -7179861704951334960L;

    /** State variation. */
    protected double[] stateVariation;

    /** Step size used in the first scaled derivative and Nordsieck vector. */
    private double scalingH;

    /**
     * Reference time for all arrays.
     * <p>
     * Sometimes, the reference time is the same as previousTime, sometimes it is the same as currentTime, so we use a
     * separate field to avoid any confusion.
     * </p>
     */
    private double referenceTime;

    /** First scaled derivative. */
    private double[] scaled;

    /** Nordsieck vector. */
    private Array2DRowRealMatrix nordsieck;

    /**
     * Simple constructor.
     * This constructor builds an instance that is not usable yet, the {@link AbstractStepInterpolator#reinitialize}
     * method should be called
     * before using the instance in order to initialize the internal arrays. This
     * constructor is used only in order to delay the initialization in
     * some cases.
     */
    public NordsieckStepInterpolator() {
        super();
        // Nothing to do
    }

    /**
     * Copy constructor.
     * 
     * @param interpolator
     *        interpolator to copy from. The copy is a deep
     *        copy: its arrays are separated from the original arrays of the
     *        instance
     */
    public NordsieckStepInterpolator(final NordsieckStepInterpolator interpolator) {
        super(interpolator);
        this.scalingH = interpolator.scalingH;
        this.referenceTime = interpolator.referenceTime;
        if (interpolator.scaled != null) {
            this.scaled = interpolator.scaled.clone();
        }
        if (interpolator.nordsieck != null) {
            this.nordsieck = new Array2DRowRealMatrix(interpolator.nordsieck.getDataRef(), true);
        }
        if (interpolator.stateVariation != null) {
            this.stateVariation = interpolator.stateVariation.clone();
        }
    }

    /** {@inheritDoc} */
    @Override
    protected StepInterpolator doCopy() {
        return new NordsieckStepInterpolator(this);
    }

    /**
     * Reinitialize the instance.
     * <p>
     * Beware that all arrays <em>must</em> be references to integrator arrays, in order to ensure proper update without
     * copy.
     * </p>
     * 
     * @param y
     *        reference to the integrator array holding the state at
     *        the end of the step
     * @param forward
     *        integration direction indicator
     * @param primaryMapper
     *        equations mapper for the primary equations set
     * @param secondaryMappers
     *        equations mappers for the secondary equations sets
     */
    @Override
    public void reinitialize(final double[] y, final boolean forward,
                             final EquationsMapper primaryMapper,
                             final EquationsMapper[] secondaryMappers) {
        super.reinitialize(y, forward, primaryMapper, secondaryMappers);
        this.stateVariation = new double[y.length];
    }

    /**
     * Reinitialize the instance.
     * <p>
     * Beware that all arrays <em>must</em> be references to integrator arrays, in order to ensure proper update without
     * copy.
     * </p>
     * 
     * @param time
     *        time at which all arrays are defined
     * @param stepSize
     *        step size used in the scaled and nordsieck arrays
     * @param scaledDerivative
     *        reference to the integrator array holding the first
     *        scaled derivative
     * @param nordsieckVector
     *        reference to the integrator matrix holding the
     *        nordsieck vector
     */
    public void reinitialize(final double time, final double stepSize,
                             final double[] scaledDerivative,
                             final Array2DRowRealMatrix nordsieckVector) {
        this.referenceTime = time;
        this.scalingH = stepSize;
        this.scaled = scaledDerivative;
        this.nordsieck = nordsieckVector;

        // make sure the state and derivatives will depend on the new arrays
        this.setInterpolatedTime(this.getInterpolatedTime());

    }

    /**
     * Rescale the instance.
     * <p>
     * Since the scaled and Nordiseck arrays are shared with the caller, this method has the side effect of rescaling
     * this arrays in the caller too.
     * </p>
     * 
     * @param stepSize
     *        new step size to use in the scaled and nordsieck arrays
     */
    public void rescale(final double stepSize) {

        // Get ratio
        final double ratio = stepSize / this.scalingH;
        // Compute first scaled derivative
        for (int i = 0; i < this.scaled.length; ++i) {
            this.scaled[i] *= ratio;
        }

        // Get reference data
        final double[][] nData = this.nordsieck.getDataRef();
        double power = ratio;
        // Loop on all elements in reference data and rescale it
        for (final double[] element : nData) {
            power *= ratio;
            final double[] nDataI = element;
            for (int j = 0; j < nDataI.length; ++j) {
                nDataI[j] *= power;
            }
        }

        // save new stepsize
        this.scalingH = stepSize;

    }

    /**
     * Get the state vector variation from current to interpolated state.
     * <p>
     * This method is aimed at computing y(t<sub>interpolation</sub>) -y(t<sub>current</sub>) accurately by avoiding the
     * cancellation errors that would occur if the subtraction were performed explicitly.
     * </p>
     * <p>
     * The returned vector is a reference to a reused array, so it should not be modified and it should be copied if it
     * needs to be preserved across several calls.
     * </p>
     * 
     * @return state vector at time {@link #getInterpolatedTime}
     * @see #getInterpolatedDerivatives()
     * @exception MaxCountExceededException
     *            if the number of functions evaluations is exceeded
     */
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    public double[] getInterpolatedStateVariation() {
        // compute and ignore interpolated state
        // to make sure state variation is computed as a side effect
        this.getInterpolatedState();
        return this.stateVariation;
    }

    /** {@inheritDoc} */
    @Override
    protected void computeInterpolatedStateAndDerivatives(final double theta, final double oneMinusThetaH) {

        // Absisa
        final double x = this.interpolatedTime - this.referenceTime;
        final double normalizedAbscissa = x / this.scalingH;

        // Init data
        Arrays.fill(this.stateVariation, 0.0);
        Arrays.fill(this.interpolatedDerivatives, 0.0);

        // apply Taylor formula from high order to low order,
        // for the sake of numerical accuracy
        final double[][] nData = this.nordsieck.getDataRef();
        for (int i = nData.length - 1; i >= 0; --i) {
            final int order = i + 2;
            final double[] nDataI = nData[i];
            final double power = MathLib.pow(normalizedAbscissa, order);
            for (int j = 0; j < nDataI.length; ++j) {
                final double d = nDataI[j] * power;
                this.stateVariation[j] += d;
                this.interpolatedDerivatives[j] += order * d;
            }
        }

        for (int j = 0; j < this.currentState.length; ++j) {
            // Compute interpolated data
            this.stateVariation[j] += this.scaled[j] * normalizedAbscissa;
            this.interpolatedState[j] = this.currentState[j] + this.stateVariation[j];
            this.interpolatedDerivatives[j] =
                (this.interpolatedDerivatives[j] + this.scaled[j] * normalizedAbscissa) / x;
        }

    }

    /** {@inheritDoc} */
    @Override
    public void writeExternal(final ObjectOutput oo) throws IOException {

        // save the state of the base class
        this.writeBaseExternal(oo);

        // save the local attributes
        oo.writeDouble(this.scalingH);
        oo.writeDouble(this.referenceTime);

        final int n = (this.currentState == null) ? -1 : this.currentState.length;
        if (this.scaled == null) {
            oo.writeBoolean(false);
        } else {
            oo.writeBoolean(true);
            for (int j = 0; j < n; ++j) {
                // Save first scale derivative
                oo.writeDouble(this.scaled[j]);
            }
        }

        if (this.nordsieck == null) {
            oo.writeBoolean(false);
        } else {
            oo.writeBoolean(true);
            // Save Nordsieck vector
            oo.writeObject(this.nordsieck);
        }

        // we don't save state variation, it will be recomputed

    }

    /** {@inheritDoc} */
    @Override
    public void readExternal(final ObjectInput oi) throws IOException, ClassNotFoundException {

        // read the base class
        final double t = this.readBaseExternal(oi);

        // read the local attributes
        this.scalingH = oi.readDouble();
        this.referenceTime = oi.readDouble();

        final int n = (this.currentState == null) ? -1 : this.currentState.length;
        final boolean hasScaled = oi.readBoolean();
        if (hasScaled) {
            this.scaled = new double[n];
            for (int j = 0; j < n; ++j) {
                this.scaled[j] = oi.readDouble();
            }
        } else {
            this.scaled = null;
        }

        final boolean hasNordsieck = oi.readBoolean();
        if (hasNordsieck) {
            // Read matrix
            this.nordsieck = (Array2DRowRealMatrix) oi.readObject();
        } else {
            // No matrix
            this.nordsieck = null;
        }

        if (hasScaled && hasNordsieck) {
            // we can now set the interpolated time and state
            this.stateVariation = new double[n];
            this.setInterpolatedTime(t);
        } else {
            this.stateVariation = null;
        }

    }

}
