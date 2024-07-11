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

//CHECKSTYLE: stop CommentRatio check
//Reason: Commons-Math model kept as such

/**
 * This class represents an interpolator over the last step during an
 * ODE integration for the 5(4) Higham and Hall integrator.
 * 
 * @see HighamHall54Integrator
 * 
 * @version $Id: HighamHall54StepInterpolator.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 1.2
 */

// CHECKSTYLE: stop MagicNumber check
class HighamHall54StepInterpolator
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
     * in order to delay the initialization in some cases. The {@link EmbeddedRungeKuttaIntegrator} uses the prototyping
     * design pattern
     * to create the step interpolators by cloning an uninitialized model
     * and later initializing the copy.
     */
    public HighamHall54StepInterpolator() {
        super();
    }

    /**
     * Copy constructor.
     * 
     * @param interpolator
     *        interpolator to copy from. The copy is a deep
     *        copy: its arrays are separated from the original arrays of the
     *        instance
     */
    public HighamHall54StepInterpolator(final HighamHall54StepInterpolator interpolator) {
        super(interpolator);
    }

    /** {@inheritDoc} */
    @Override
    protected StepInterpolator doCopy() {
        return new HighamHall54StepInterpolator(this);
    }

    /** {@inheritDoc} */
    @Override
    protected void computeInterpolatedStateAndDerivatives(final double theta,
                                                          final double oneMinusThetaH) {

        final double bDot0 = 1 + theta * (-15.0 / 2.0 + theta * (16.0 - 10.0 * theta));
        final double bDot2 = theta * (459.0 / 16.0 + theta * (-729.0 / 8.0 + 135.0 / 2.0 * theta));
        final double bDot3 = theta * (-44.0 + theta * (152.0 - 120.0 * theta));
        final double bDot4 = theta * (375.0 / 16.0 + theta * (-625.0 / 8.0 + 125.0 / 2.0 * theta));
        final double bDot5 = theta * 5.0 / 8.0 * (2 * theta - 1);

        if ((this.previousState != null) && (theta <= HALF)) {
            final double hTheta = this.h * theta;
            final double b0 = hTheta * (1.0 + theta * (-15.0 / 4.0 + theta * (16.0 / 3.0 - 5.0 / 2.0 * theta)));
            final double b2 = hTheta * (theta * (459.0 / 32.0 + theta * (-243.0 / 8.0 + theta * 135.0 / 8.0)));
            final double b3 = hTheta * (theta * (-22.0 + theta * (152.0 / 3.0 + theta * -30.0)));
            final double b4 = hTheta * (theta * (375.0 / 32.0 + theta * (-625.0 / 24.0 + theta * 125.0 / 8.0)));
            final double b5 = hTheta * (theta * (-5.0 / 16.0 + theta * 5.0 / 12.0));
            for (int i = 0; i < this.interpolatedState.length; ++i) {
                final double yDot0 = this.yDotK[0][i];
                final double yDot2 = this.yDotK[2][i];
                final double yDot3 = this.yDotK[3][i];
                final double yDot4 = this.yDotK[4][i];
                final double yDot5 = this.yDotK[5][i];
                this.interpolatedState[i] =
                    this.previousState[i] + b0 * yDot0 + b2 * yDot2 + b3 * yDot3 + b4 * yDot4 + b5 * yDot5;
                this.interpolatedDerivatives[i] =
                    bDot0 * yDot0 + bDot2 * yDot2 + bDot3 * yDot3 + bDot4 * yDot4 + bDot5 * yDot5;
            }
        } else {
            final double theta2 = theta * theta;
            final double b0 = this.h
                * (-1.0 / 12.0 + theta * (1.0 + theta * (-15.0 / 4.0 + theta * (16.0 / 3.0 + theta * -5.0 / 2.0))));
            final double b2 = this.h
                * (-27.0 / 32.0 + theta2 * (459.0 / 32.0 + theta * (-243.0 / 8.0 + theta * 135.0 / 8.0)));
            final double b3 = this.h * (4.0 / 3.0 + theta2 * (-22.0 + theta * (152.0 / 3.0 + theta * -30.0)));
            final double b4 = this.h
                * (-125.0 / 96.0 + theta2 * (375.0 / 32.0 + theta * (-625.0 / 24.0 + theta * 125.0 / 8.0)));
            final double b5 = this.h * (-5.0 / 48.0 + theta2 * (-5.0 / 16.0 + theta * 5.0 / 12.0));
            for (int i = 0; i < this.interpolatedState.length; ++i) {
                final double yDot0 = this.yDotK[0][i];
                final double yDot2 = this.yDotK[2][i];
                final double yDot3 = this.yDotK[3][i];
                final double yDot4 = this.yDotK[4][i];
                final double yDot5 = this.yDotK[5][i];
                this.interpolatedState[i] =
                    this.currentState[i] + b0 * yDot0 + b2 * yDot2 + b3 * yDot3 + b4 * yDot4 + b5 * yDot5;
                this.interpolatedDerivatives[i] =
                    bDot0 * yDot0 + bDot2 * yDot2 + bDot3 * yDot3 + bDot4 * yDot4 + bDot5 * yDot5;
            }
        }

    }

    // CHECKSTYLE: resume MagicNumber check
    // CHECKSTYLE: resume CommentRatio check
}
