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

import fr.cnes.sirius.patrius.math.ode.sampling.StepInterpolator;

/**
 * This class implements a step interpolator for the classical fourth
 * order Runge-Kutta integrator.
 * 
 * <p>
 * This interpolator allows to compute dense output inside the last step computed. The interpolation equation is
 * consistent with the integration scheme :
 * <ul>
 * <li>Using reference point at step start:<br>
 * y(t<sub>n</sub> + &theta; h) = y (t<sub>n</sub>) + &theta; (h/6) [ (6 - 9 &theta; + 4 &theta;<sup>2</sup>)
 * y'<sub>1</sub> + ( 6 &theta; - 4 &theta;<sup>2</sup>) (y'<sub>2</sub> + y'<sub>3</sub>) + ( -3 &theta; + 4
 * &theta;<sup>2</sup>) y'<sub>4</sub> ]</li>
 * <li>Using reference point at step end:<br>
 * y(t<sub>n</sub> + &theta; h) = y (t<sub>n</sub> + h) + (1 - &theta;) (h/6) [ (-4 &theta;^2 + 5 &theta; - 1)
 * y'<sub>1</sub> +(4 &theta;^2 - 2 &theta; - 2) (y'<sub>2</sub> + y'<sub>3</sub>) -(4 &theta;^2 + &theta; + 1)
 * y'<sub>4</sub> ]</li>
 * </ul>
 * </p>
 * 
 * where &theta; belongs to [0 ; 1] and where y'<sub>1</sub> to y'<sub>4</sub> are the four
 * evaluations of the derivatives already computed during the
 * step.</p>
 * 
 * Mod : Removed reference point at step start. Erratic behavior.
 * 
 * @see ClassicalRungeKuttaIntegrator
 * @version $Id: ClassicalRungeKuttaStepInterpolator.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 1.2
 */

class ClassicalRungeKuttaStepInterpolator
    extends RungeKuttaStepInterpolator {

     /** Serializable UID. */
    private static final long serialVersionUID = 20111120L;

    /**
     * Simple constructor.
     * This constructor builds an instance that is not usable yet, the {@link RungeKuttaStepInterpolator#reinitialize}
     * method should be
     * called before using the instance in order to initialize the
     * internal arrays. This constructor is used only in order to delay
     * the initialization in some cases. The {@link RungeKuttaIntegrator} class uses the prototyping design pattern to
     * create the step
     * interpolators by cloning an uninitialized model and latter initializing
     * the copy.
     */
    public ClassicalRungeKuttaStepInterpolator() {
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
    public ClassicalRungeKuttaStepInterpolator(final ClassicalRungeKuttaStepInterpolator interpolator) {
        super(interpolator);
    }

    /** {@inheritDoc} */
    @Override
    protected StepInterpolator doCopy() {
        return new ClassicalRungeKuttaStepInterpolator(this);
    }

    /** {@inheritDoc} */
    @Override
    protected void computeInterpolatedStateAndDerivatives(final double theta,
                                                          final double oneMinusThetaH) {

        // Intermediate variables
        final double fourTheta = 4 * theta;
        final double oneMinusTheta = 1 - theta;
        final double oneMinus2Theta = 1 - 2 * theta;
        final double s = oneMinusThetaH / 6.0;
        final double coeff1 = s * ((-fourTheta + 5) * theta - 1);
        final double coeff23 = s * ((fourTheta - 2) * theta - 2);
        final double coeff4 = s * ((-fourTheta - 1) * theta - 1);
        final double coeffDot1 = oneMinusTheta * oneMinus2Theta;
        final double coeffDot23 = 2 * theta * oneMinusTheta;
        final double coeffDot4 = -theta * oneMinus2Theta;
        for (int i = 0; i < this.interpolatedState.length; ++i) {
            // Interpolation
            final double yDot1 = this.yDotK[0][i];
            final double yDot23 = this.yDotK[1][i] + this.yDotK[2][i];
            final double yDot4 = this.yDotK[3][i];
            this.interpolatedState[i] =
                this.currentState[i] + coeff1 * yDot1 + coeff23 * yDot23 + coeff4 * yDot4;
            this.interpolatedDerivatives[i] =
                coeffDot1 * yDot1 + coeffDot23 * yDot23 + coeffDot4 * yDot4;

        }

        // No result to return
        // Attributes have been updated
    }
}
