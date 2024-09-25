/**
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
 * 
 * @history creation 21/01/2013
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:410:16/04/2015: Anomalies in the Patrius Javadoc
 * VERSION::FA:592:07/04/2016: Javadoc improvement
 * VERSION::DM:684:27/03/2018:add 2nd order RK6 interpolator
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.ode.nonstiff;

import fr.cnes.sirius.patrius.math.ode.sampling.StepInterpolator;

/**
 * <p>
 * Interpolator for {@link RungeKutta6Integrator}.
 * </p>
 * 
 * <p>
 * <b>Warning:</b> This interpolator currently performs a 2nd order interpolation issued from article <i>Dense output
 * for strong stability preserving Runge–Kutta methods, D. Ketcheson, 2016</i>. <br/>
 * Accuracy is however below 1m for standard timestep.
 * </p>
 * 
 * @concurrency not thread-safe
 * 
 * @author Cedric Dental
 * 
 * @version 1.3
 * 
 * @since 1.3
 * 
 */
@SuppressWarnings("PMD.NullAssignment")
public class RungeKutta6StepInterpolator extends RungeKuttaStepInterpolator {

    /** Butcher B array. */
    private final double[] b;

    /**
     * Empty constructor.
     */
    public RungeKutta6StepInterpolator() {
        super();
        this.b = null;
    }

    /**
     * Simple constructor. This constructor builds an instance that is not usable yet, the
     * {@link RungeKuttaStepInterpolator#reinitialize} method should be called before using the
     * instance in order to initialize the internal arrays. This constructor is used only in order
     * to delay the initialization in some cases. The {@link RungeKuttaIntegrator} class uses the
     * prototyping design pattern to create the step interpolators by cloning an uninitialized model
     * and latter initializing the copy.
     * 
     * @param bIn B butcher array
     */
    public RungeKutta6StepInterpolator(final double[] bIn) {
        super();
        this.b = bIn;
    }

    /**
     * Copy constructor.
     * 
     * @param interpolator interpolator to copy from. The copy is a deep copy: its arrays are
     *        separated from the original arrays of the instance
     */
    public RungeKutta6StepInterpolator(final RungeKutta6StepInterpolator interpolator) {
        super(interpolator);
        this.b = interpolator.b;
    }

    /** {@inheritDoc} */
    @Override
    protected StepInterpolator doCopy() {
        return new RungeKutta6StepInterpolator(this);
    }

    /** {@inheritDoc} */
    @Override
    protected void computeInterpolatedStateAndDerivatives(final double theta,
                                                          final double oneMinusThetaH) {

        // redefine theta
        double thetaFinal = theta;
        if (thetaFinal > 1.) {
            thetaFinal = 1.;
        } else if (thetaFinal < 0.) {
            thetaFinal = 0.;
        }
        // 2nd order interpolation
        final double length = this.currentState.length;
        for (int i = 0; i < length; i++) {
            // First stage
            double c = (1. - this.b[0]) * thetaFinal;
            double sum = (thetaFinal - c * thetaFinal) * this.yDotK[0][i];
            double sumDot = (1. - 2. * c) * this.yDotK[0][i];

            // Other stages
            for (int j = 1; j < this.b.length; j++) {
                c = thetaFinal * this.b[j] * this.yDotK[j][i];
                sum += thetaFinal * c;
                sumDot += 2. * c;
            }

            // State
            this.interpolatedState[i] = this.previousState[i] + this.h * sum;
            // Derivative (with respect to theta)
            this.interpolatedDerivatives[i] = sumDot;
        }
    }
}
