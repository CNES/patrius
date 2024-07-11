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
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.ode.nonstiff;

import fr.cnes.sirius.patrius.math.ode.sampling.StepInterpolator;

/**
 * This class implements a step interpolator for second order
 * Runge-Kutta integrator.
 * 
 * <p>
 * This interpolator computes dense output inside the last step computed. The interpolation equation is consistent with
 * the integration scheme :
 * <ul>
 * <li>Using reference point at step start:<br>
 * y(t<sub>n</sub> + &theta; h) = y (t<sub>n</sub>) + &theta; h [(1 - &theta;) 
 * y'<sub>1</sub> + &theta; y'<sub>2</sub>]</li>
 * <li>Using reference point at step end:<br>
 * y(t<sub>n</sub> + &theta; h) = y (t<sub>n</sub> + h) + (1-&theta;) h [&theta; y'<sub>1</sub> - (1+&theta;)
 * y'<sub>2</sub>]</li>
 * </ul>
 * </p>
 * 
 * where &theta; belongs to [0 ; 1] and where y'<sub>1</sub> and y'<sub>2</sub> are the two
 * evaluations of the derivatives already computed during the
 * step.</p>
 * 
 * @see MidpointIntegrator
 * @version $Id: MidpointStepInterpolator.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 1.2
 */

class MidpointStepInterpolator
    extends RungeKuttaStepInterpolator {

    /** 0.5. */
    private static final double HALF = 0.5;

    /** Serializable version identifier */
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
    public MidpointStepInterpolator() {
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
    public MidpointStepInterpolator(final MidpointStepInterpolator interpolator) {
        super(interpolator);
    }

    /** {@inheritDoc} */
    @Override
    protected StepInterpolator doCopy() {
        return new MidpointStepInterpolator(this);
    }

    /** {@inheritDoc} */
    @Override
    protected void computeInterpolatedStateAndDerivatives(final double theta,
                                                          final double oneMinusThetaH) {

        // Initialization
        final double coeffDot2 = 2 * theta;
        final double coeffDot1 = 1 - coeffDot2;

        if ((this.previousState != null) && (theta <= HALF)) {
            // First case
            final double coeff1 = theta * oneMinusThetaH;
            final double coeff2 = theta * theta * this.h;
            for (int i = 0; i < this.interpolatedState.length; ++i) {
                final double yDot1 = this.yDotK[0][i];
                final double yDot2 = this.yDotK[1][i];
                this.interpolatedState[i] = this.previousState[i] + coeff1 * yDot1 + coeff2 * yDot2;
                this.interpolatedDerivatives[i] = coeffDot1 * yDot1 + coeffDot2 * yDot2;
            }
        } else {
            // Second case
            final double coeff1 = oneMinusThetaH * theta;
            final double coeff2 = oneMinusThetaH * (1.0 + theta);
            for (int i = 0; i < this.interpolatedState.length; ++i) {
                final double yDot1 = this.yDotK[0][i];
                final double yDot2 = this.yDotK[1][i];
                this.interpolatedState[i] = this.currentState[i] + coeff1 * yDot1 - coeff2 * yDot2;
                this.interpolatedDerivatives[i] = coeffDot1 * yDot1 + coeffDot2 * yDot2;
            }
        }

        // No result to return
        // Attributes have been updated
    }

}
