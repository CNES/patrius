/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
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
 */
package fr.cnes.sirius.patrius.math.ode.nonstiff;

import fr.cnes.sirius.patrius.math.ode.sampling.StepInterpolator;

/**
 * This class implements a linear interpolator for step.
 * 
 * <p>
 * This interpolator computes dense output inside the last step computed. The interpolation equation is consistent with
 * the integration scheme :
 * <ul>
 * <li>Using reference point at step start:<br>
 * y(t<sub>n</sub> + &theta; h) = y (t<sub>n</sub>) + &theta; h y'</li>
 * <li>Using reference point at step end:<br>
 * y(t<sub>n</sub> + &theta; h) = y (t<sub>n</sub> + h) - (1-&theta;) h y'</li>
 * </ul>
 * </p>
 * 
 * where &theta; belongs to [0 ; 1] and where y' is the evaluation of
 * the derivatives already computed during the step.</p>
 * 
 * @see EulerIntegrator
 * @version $Id: EulerStepInterpolator.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 1.2
 */

class EulerStepInterpolator
    extends RungeKuttaStepInterpolator {

    /** 0.5. */
    private static final double HALF = 0.5;

    /** Serializable version identifier. */
    private static final long serialVersionUID = 20111120L;

    /**
     * Simple constructor.
     * This constructor builds an instance that is not usable yet, the
     * {@link fr.cnes.sirius.patrius.math.ode.sampling.AbstractStepInterpolator#reinitialize} method should be called
     * before using the instance in order to
     * initialize the internal arrays. This constructor is used only
     * in order to delay the initialization in some cases. The {@link RungeKuttaIntegrator} class uses the prototyping
     * design pattern
     * to create the step interpolators by cloning an uninitialized model
     * and later initializing the copy.
     */
    public EulerStepInterpolator() {
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
    public EulerStepInterpolator(final EulerStepInterpolator interpolator) {
        super(interpolator);
    }

    /** {@inheritDoc} */
    @Override
    protected StepInterpolator doCopy() {
        return new EulerStepInterpolator(this);
    }

    /** {@inheritDoc} */
    @Override
    protected void computeInterpolatedStateAndDerivatives(final double theta,
                                                          final double oneMinusThetaH) {
        if ((this.previousState != null) && (theta <= HALF)) {
            // a previous state exists and theta is closer to the previous step than
            // to the current step
            for (int i = 0; i < this.interpolatedState.length; ++i) {
                // compute interpolated state from previous state
                this.interpolatedState[i] = this.previousState[i] + theta * this.h * this.yDotK[0][i];
            }
            System.arraycopy(this.yDotK[0], 0, this.interpolatedDerivatives, 0, this.interpolatedDerivatives.length);
        } else {
            for (int i = 0; i < this.interpolatedState.length; ++i) {
                // compute interpolated state from current state
                this.interpolatedState[i] = this.currentState[i] - oneMinusThetaH * this.yDotK[0][i];
            }
            System.arraycopy(this.yDotK[0], 0, this.interpolatedDerivatives, 0, this.interpolatedDerivatives.length);
        }

    }

}