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
 * VERSION:4.15:OPENFD-221:21/11/2024:[STELA-PATRIUS] Interpolateur STELA précis
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
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.utils.Constants;


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


    /** Interpolator polynomial coefficients bj as defined in Shampine book. */
    private final double[][] b;

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
    public RungeKutta6StepInterpolator(final double[][] bIn) {
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

        if (Precision.equals(theta, 0)) {
            // Lower endpoint - Immediate return
            System.arraycopy(this.previousState, 0, this.interpolatedState, 0, this.previousState.length);
        } else if (Precision.equals(theta, 1)) {
            // Upper endpoint - Immediate return
            System.arraycopy(this.currentState, 0, this.interpolatedState, 0, this.currentState.length);
        } else {
            // Intermediate points

            // Precompute powers of theta
            final double[] powTheta = new double[b[yDotK.length - 1].length + 1];
            for (int i = 0; i < powTheta.length; i++) {
                powTheta[i] = FastMath.pow(theta, i);
            }

            // Formula (6.1) and (6.6) from Shampine book
            for (int i = 0; i < this.interpolatedState.length; ++i) {
                double delta = 0;
                for (int j = 0; j < yDotK.length; j++) {
                    double bj = 0;
                    for (int q = 0; q < b[j].length; q++) {
                        // Index 0 represents order 1
                        bj += b[j][q] * powTheta[q + 1];
                    }
                    delta += bj * this.yDotK[j][i];
                }
                this.interpolatedState[i] = this.previousState[i] + this.h * delta;
            }
            
            // If an error has occurred in the derivatives computation (incoherent state),
            // linear interpolation is used instead
            if (isIncoherentState(this.interpolatedState)) {
                for (int i = 0; i < 6; i++) {
                    interpolatedDerivatives[i] = currentState[i] - previousState[i];
                    interpolatedState[i] = previousState[i] + theta * interpolatedDerivatives[i];
                }
            }

            // No result to return
            // Attributes (variable interpolatedState) have been updated
        }
    }
    
    /**
     * Check if state is coherent.
     * @param currY current state
     * @return true if state is coherent
     */
    public boolean isIncoherentState(final double[] currY) {

        // Initialization
        boolean test1;
        boolean test2;
        boolean test3;
        boolean test4;
        
        if (currY.length < 6) {
            test1 = false;
            test2 = false;
            test3 = false;
            test4 = false;
        } else {
            try {
                final double currEcc;
                final double currEccSq = currY[2] * currY[2] + currY[3] * currY[3]; 
                // Compute eccentricity
                if(Double.isFinite(currEccSq)){
                    currEcc = FastMath.sqrt(currEccSq); 
                } else {
                    throw new ArithmeticException();
                }
                // If perigee is negative
                test1 = currY[0] * (1. - currEcc) - Constants.CNES_STELA_AE < 0;
                // If eccentricity is greater than 1
                test2 = currEcc >= 1;
                // If sin(i/2) is greater than 1
                final double siniOver2Sq = currY[4] * currY[4] + currY[5] * currY[5]; 
                if (Double.isFinite(siniOver2Sq)) {
                    test3 = FastMath.sqrt(siniOver2Sq) >= 1;
                } else {
                    throw new ArithmeticException();
                }
                // If a component of the inclination vector is NAN or infinite
                test4 = (Double.isFinite(currY[4]) || Double.isFinite(currY[5]));
            } catch (final ArithmeticException e) {

                test1 = true;
                test2 = true;
                test3 = true;
                test4 = true;
            }
        }

        // Correction only if there is an unexpected value
        return test1 || test2 || test3 || test4;
    }
}
