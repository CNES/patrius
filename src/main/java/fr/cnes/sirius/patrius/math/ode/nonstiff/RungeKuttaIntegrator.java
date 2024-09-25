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
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:189:18/12/2013:Cancelled changes made by previous commit
 * VERSION::FA:350:27/03/2015: Proper handling of first time-step
 * VERSION::FA:379:27/03/2015: Proper handling of first time-step
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.math.ode.nonstiff;

import fr.cnes.sirius.patrius.math.ode.AbstractIntegrator;
import fr.cnes.sirius.patrius.math.ode.ExpandableStatefulODE;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * This class implements the common part of all fixed step Runge-Kutta
 * integrators for Ordinary Differential Equations.
 * 
 * <p>
 * These methods are explicit Runge-Kutta methods, their Butcher arrays are as follows :
 * 
 * <pre>
 *    0  |
 *   c2  | a21
 *   c3  | a31  a32
 *   ... |        ...
 *   cs  | as1  as2  ...  ass-1
 *       |--------------------------
 *       |  b1   b2  ...   bs-1  bs
 * </pre>
 * 
 * </p>
 * 
 * @see EulerIntegrator
 * @see ClassicalRungeKuttaIntegrator
 * @see GillIntegrator
 * @see MidpointIntegrator
 * @version $Id: RungeKuttaIntegrator.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 1.2
 */
//CHECKSTYLE: stop AbstractClassName check
@SuppressWarnings("PMD.AbstractNaming")
public abstract class RungeKuttaIntegrator extends AbstractIntegrator {
    // CHECKSTYLE: resume AbstractClassName check

    /** Time steps from Butcher array (without the first zero). */
    private final double[] c;

    /** Internal weights from Butcher array (without the first empty row). */
    private final double[][] a;

    /** External weights for the high order method from Butcher array. */
    private final double[] b;

    /** Prototype of the step interpolator. */
    private final RungeKuttaStepInterpolator prototype;

    /** Integration step. */
    private final double step;

    /**
     * Simple constructor.
     * Build a Runge-Kutta integrator with the given
     * step. The default step handler does nothing.
     * 
     * @param name
     *        name of the method
     * @param cIn
     *        time steps from Butcher array (without the first zero)
     * @param aIn
     *        internal weights from Butcher array (without the first empty row)
     * @param bIn
     *        propagation weights for the high order method from Butcher array
     * @param prototypeIn
     *        prototype of the step interpolator to use
     * @param stepIn
     *        integration step
     */
    protected RungeKuttaIntegrator(final String name,
        final double[] cIn, final double[][] aIn, final double[] bIn,
        final RungeKuttaStepInterpolator prototypeIn,
        final double stepIn) {
        super(name);
        this.c = cIn;
        this.a = aIn;
        this.b = bIn;
        this.prototype = prototypeIn;
        this.step = MathLib.abs(stepIn);
    }

    /** {@inheritDoc} */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Commons-Math code kept as such
    @Override
    public void integrate(final ExpandableStatefulODE equations, final double t) {
        // CHECKSTYLE: resume CyclomaticComplexity check

        // sanity check
        sanityChecks(equations, t);
        this.setEquations(equations);
        // get integration direction
        final boolean forward = t > equations.getTime();

        // create some internal working arrays
        final double[] y0 = equations.getCompleteState();
        final double[] y = y0.clone();
        final int stages = this.c.length + 1;
        final double[][] yDotK = new double[stages][];
        for (int i = 0; i < stages; ++i) {
            yDotK[i] = new double[y0.length];
        }
        final double[] yTmp = y0.clone();
        final double[] yDotTmp = new double[y0.length];

        // set up an interpolator sharing the integrator arrays
        final RungeKuttaStepInterpolator interpolator = (RungeKuttaStepInterpolator) this.prototype.copy();
        interpolator.reinitialize(this, yTmp, yDotK, forward,
            equations.getPrimaryMapper(), equations.getSecondaryMappers());
        interpolator.storeTime(equations.getTime());

        // set up integration control objects
        this.stepStart = equations.getTime();
        this.stepSize = forward ? this.step : -this.step;
        this.initIntegration(equations.getTime(), y0, t);

        // Stepsize control for next step
        final double nextT0 = this.stepStart + this.stepSize;
        final boolean nextIsLast0 = forward ? (nextT0 > t) : (nextT0 < t);
        if (nextIsLast0) {
            this.stepSize = t - this.stepStart;
            this.stepSize = avoidOvershoot(this.stepStart, t, this.stepSize, forward);
            this.isLastStep = true;
        } else {
            this.isLastStep = false;
        }

        // main integration loop
        do {
            // shift one step forward
            interpolator.shift();

            // first stage
            this.computeDerivatives(this.stepStart, y, yDotK[0]);

            // next stages
            for (int k = 1; k < stages; ++k) {

                for (int j = 0; j < y0.length; ++j) {
                    double sum = this.a[k - 1][0] * yDotK[0][j];
                    for (int l = 1; l < k; ++l) {
                        sum += this.a[k - 1][l] * yDotK[l][j];
                    }
                    yTmp[j] = y[j] + this.stepSize * sum;
                }

                this.computeDerivatives(this.stepStart + this.c[k - 1] * this.stepSize, yTmp, yDotK[k]);

            }

            // estimate the state at the end of the step
            for (int j = 0; j < y0.length; ++j) {
                double sum = this.b[0] * yDotK[0][j];
                for (int l = 1; l < stages; ++l) {
                    sum += this.b[l] * yDotK[l][j];
                }
                yTmp[j] = y[j] + this.stepSize * sum;
            }

            // discrete events handling
            interpolator.storeTime(this.stepStart + this.stepSize);
            System.arraycopy(yTmp, 0, y, 0, y0.length);
            System.arraycopy(yDotK[stages - 1], 0, yDotTmp, 0, y0.length);
            this.stepStart = this.acceptStep(interpolator, y, yDotTmp, t);

            if (!this.isLastStep) {

                // prepare next step
                interpolator.storeTime(this.stepStart);

                // stepsize control for next step
                final double nextT = this.stepStart + this.stepSize;
                final boolean nextIsLast = forward ? (nextT > t) : (nextT < t);
                if (nextIsLast) {
                    this.stepSize = t - this.stepStart;
                    // Ensure that stepStart + stepSize is equal to t
                    this.stepSize = avoidOvershoot(this.stepStart, t, this.stepSize, forward);
                }
            }

        } while (!this.isLastStep);

        // dispatch results
        equations.setTime(this.stepStart);
        equations.setCompleteState(y);

        // reset step start time and size
        this.stepStart = Double.NaN;
        this.stepSize = Double.NaN;
    }
}
