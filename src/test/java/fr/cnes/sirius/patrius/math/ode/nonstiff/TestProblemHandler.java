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
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.ode.nonstiff;

import fr.cnes.sirius.patrius.math.exception.MaxCountExceededException;
import fr.cnes.sirius.patrius.math.ode.ODEIntegrator;
import fr.cnes.sirius.patrius.math.ode.sampling.StepHandler;
import fr.cnes.sirius.patrius.math.ode.sampling.StepInterpolator;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * This class is used to handle steps for the test problems
 * integrated during the junit tests for the ODE integrators.
 */
public class TestProblemHandler
    implements StepHandler {

    /** Associated problem. */
    private final TestProblemAbstract problem;

    /** Maximal errors encountered during the integration. */
    private double maxValueError;
    private double maxTimeError;

    /** Error at the end of the integration. */
    private double lastError;

    /** Time at the end of integration. */
    private double lastTime;

    /** ODE solver used. */
    private final ODEIntegrator integrator;

    /** Expected start for step. */
    private double expectedStepStart;

    /**
     * Simple constructor.
     * 
     * @param problem
     *        problem for which steps should be handled
     * @param integrator
     *        ODE solver used
     */
    public TestProblemHandler(final TestProblemAbstract problem, final ODEIntegrator integrator) {
        this.problem = problem;
        this.integrator = integrator;
        this.maxValueError = 0;
        this.maxTimeError = 0;
        this.lastError = 0;
        this.expectedStepStart = Double.NaN;
    }

    @Override
    public void init(final double t0, final double[] y0, final double t) {
        this.maxValueError = 0;
        this.maxTimeError = 0;
        this.lastError = 0;
        this.expectedStepStart = Double.NaN;
    }

    @Override
    public void handleStep(final StepInterpolator interpolator, final boolean isLast) throws MaxCountExceededException {

        final double start = this.integrator.getCurrentStepStart();
        if (MathLib.abs((start - this.problem.getInitialTime()) / this.integrator.getCurrentSignedStepsize()) > 0.001) {
            // multistep integrators do not handle the first steps themselves
            // so we have to make sure the integrator we look at has really started its work
            if (!Double.isNaN(this.expectedStepStart)) {
                // the step should either start at the end of the integrator step
                // or at an event if the step is split into several substeps
                double stepError = MathLib.max(this.maxTimeError, MathLib.abs(start - this.expectedStepStart));
                for (final double eventTime : this.problem.getTheoreticalEventsTimes()) {
                    stepError = MathLib.min(stepError, MathLib.abs(start - eventTime));
                }
                this.maxTimeError = MathLib.max(this.maxTimeError, stepError);
            }
            this.expectedStepStart = start + this.integrator.getCurrentSignedStepsize();
        }

        final double pT = interpolator.getPreviousTime();
        final double cT = interpolator.getCurrentTime();
        final double[] errorScale = this.problem.getErrorScale();

        // store the error at the last step
        if (isLast) {
            final double[] interpolatedY = interpolator.getInterpolatedState();
            final double[] theoreticalY = this.problem.computeTheoreticalState(cT);
            for (int i = 0; i < interpolatedY.length; ++i) {
                final double error = MathLib.abs(interpolatedY[i] - theoreticalY[i]);
                this.lastError = MathLib.max(error, this.lastError);
            }
            this.lastTime = cT;
        }

        // walk through the step
        for (int k = 0; k <= 20; ++k) {

            final double time = pT + (k * (cT - pT)) / 20;
            interpolator.setInterpolatedTime(time);
            final double[] interpolatedY = interpolator.getInterpolatedState();
            final double[] theoreticalY = this.problem.computeTheoreticalState(interpolator.getInterpolatedTime());

            // update the errors
            for (int i = 0; i < interpolatedY.length; ++i) {
                final double error = errorScale[i] * MathLib.abs(interpolatedY[i] - theoreticalY[i]);
                this.maxValueError = MathLib.max(error, this.maxValueError);
            }
        }
    }

    /**
     * Get the maximal value error encountered during integration.
     * 
     * @return maximal value error
     */
    public double getMaximalValueError() {
        return this.maxValueError;
    }

    /**
     * Get the maximal time error encountered during integration.
     * 
     * @return maximal time error
     */
    public double getMaximalTimeError() {
        return this.maxTimeError;
    }

    /**
     * Get the error at the end of the integration.
     * 
     * @return error at the end of the integration
     */
    public double getLastError() {
        return this.lastError;
    }

    /**
     * Get the time at the end of the integration.
     * 
     * @return time at the end of the integration.
     */
    public double getLastTime() {
        return this.lastTime;
    }

}
