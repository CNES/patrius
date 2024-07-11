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
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.ode.nonstiff;

import fr.cnes.sirius.patrius.math.ode.FirstOrderDifferentialEquations;
import fr.cnes.sirius.patrius.math.ode.events.EventHandler;

/**
 * This class is used as the base class of the problems that are
 * integrated during the junit tests for the ODE integrators.
 */
public abstract class TestProblemAbstract
    implements FirstOrderDifferentialEquations {

    /** Dimension of the problem. */
    protected int n;

    /** Number of functions calls. */
    protected int calls;

    /** Initial time */
    protected double t0;

    /** Initial state */
    protected double[] y0;

    /** Final time */
    protected double t1;

    /** Error scale */
    protected double[] errorScale;

    /**
     * Simple constructor.
     */
    protected TestProblemAbstract() {
        this.n = 0;
        this.calls = 0;
        this.t0 = 0;
        this.y0 = null;
        this.t1 = 0;
        this.errorScale = null;
    }

    /**
     * Copy constructor.
     * 
     * @param problem
     *        problem to copy
     */
    protected TestProblemAbstract(final TestProblemAbstract problem) {
        this.n = problem.n;
        this.calls = problem.calls;
        this.t0 = problem.t0;
        if (problem.y0 == null) {
            this.y0 = null;
        } else {
            this.y0 = problem.y0.clone();
        }
        if (problem.errorScale == null) {
            this.errorScale = null;
        } else {
            this.errorScale = problem.errorScale.clone();
        }
        this.t1 = problem.t1;
    }

    /**
     * Copy operation.
     * 
     * @return a copy of the instance
     */
    public abstract TestProblemAbstract copy();

    /**
     * Set the initial conditions
     * 
     * @param t0
     *        initial time
     * @param y0
     *        initial state vector
     */
    protected void setInitialConditions(final double t0, final double[] y0) {
        this.calls = 0;
        this.n = y0.length;
        this.t0 = t0;
        this.y0 = y0.clone();
    }

    /**
     * Set the final conditions.
     * 
     * @param t1
     *        final time
     */
    protected void setFinalConditions(final double t1) {
        this.t1 = t1;
    }

    /**
     * Set the error scale
     * 
     * @param errorScale
     *        error scale
     */
    protected void setErrorScale(final double[] errorScale) {
        this.errorScale = errorScale.clone();
    }

    @Override
    public int getDimension() {
        return this.n;
    }

    /**
     * Get the initial time.
     * 
     * @return initial time
     */
    public double getInitialTime() {
        return this.t0;
    }

    /**
     * Get the initial state vector.
     * 
     * @return initial state vector
     */
    public double[] getInitialState() {
        return this.y0;
    }

    /**
     * Get the final time.
     * 
     * @return final time
     */
    public double getFinalTime() {
        return this.t1;
    }

    /**
     * Get the error scale.
     * 
     * @return error scale
     */
    public double[] getErrorScale() {
        return this.errorScale;
    }

    /**
     * Get the events handlers.
     * 
     * @return events handlers
     */
    public EventHandler[] getEventsHandlers() {
        return new EventHandler[0];
    }

    /**
     * Get the theoretical events times.
     * 
     * @return theoretical events times
     */
    public double[] getTheoreticalEventsTimes() {
        return new double[0];
    }

    /**
     * Get the number of calls.
     * 
     * @return nuber of calls
     */
    public int getCalls() {
        return this.calls;
    }

    @Override
    public void computeDerivatives(final double t, final double[] y, final double[] yDot) {
        ++this.calls;
        this.doComputeDerivatives(t, y, yDot);
    }

    abstract public void doComputeDerivatives(double t, double[] y, double[] yDot);

    /**
     * Compute the theoretical state at the specified time.
     * 
     * @param t
     *        time at which the state is required
     * @return state vector at time t
     */
    abstract public double[] computeTheoreticalState(double t);

}
