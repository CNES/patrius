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

/**
 * This interface represents a handler that should be called after
 * each successful fixed step.
 * 
 * <p>
 * This interface should be implemented by anyone who is interested in getting the solution of an ordinary differential
 * equation at fixed time steps. Objects implementing this interface should be wrapped within an instance of
 * {@link StepNormalizer} that itself is used as the general {@link StepHandler} by the integrator. The
 * {@link StepNormalizer} object is called according to the integrator internal algorithms and it calls objects
 * implementing this interface as necessary at fixed time steps.
 * </p>
 * 
 * @see StepHandler
 * @see StepNormalizer
 * @version $Id: FixedStepHandler.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 1.2
 */

public interface FixedStepHandler {

    /**
     * Initialize step handler at the start of an ODE integration.
     * <p>
     * This method is called once at the start of the integration. It may be used by the step handler to initialize some
     * internal data if needed.
     * </p>
     * 
     * @param t0
     *        start value of the independent <i>time</i> variable
     * @param y0
     *        array containing the start value of the state vector
     * @param t
     *        target time for the integration
     */
    void init(double t0, double[] y0, double t);

    /**
     * Handle the last accepted step
     * 
     * @param t
     *        time of the current step
     * @param y
     *        state vector at t. For efficiency purposes, the {@link StepNormalizer} class reuses the same array on
     *        each call, so if
     *        the instance wants to keep it across all calls (for example to
     *        provide at the end of the integration a complete array of all
     *        steps), it should build a local copy store this copy.
     * @param yDot
     *        derivatives of the state vector state vector at t.
     *        For efficiency purposes, the {@link StepNormalizer} class reuses
     *        the same array on each call, so if
     *        the instance wants to keep it across all calls (for example to
     *        provide at the end of the integration a complete array of all
     *        steps), it should build a local copy store this copy.
     * @param isLast
     *        true if the step is the last one
     */
    void handleStep(double t, double[] y, double[] yDot, boolean isLast);

}
