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

import fr.cnes.sirius.patrius.math.exception.MaxCountExceededException;

/**
 * This interface represents a handler that should be called after
 * each successful step.
 * 
 * <p>
 * The ODE integrators compute the evolution of the state vector at some grid points that depend on their own internal
 * algorithm. Once they have found a new grid point (possibly after having computed several evaluation of the derivative
 * at intermediate points), they provide it to objects implementing this interface. These objects typically either
 * ignore the intermediate steps and wait for the last one, store the points in an ephemeris, or forward them to
 * specialized processing or output methods.
 * </p>
 * 
 * @see fr.cnes.sirius.patrius.math.ode.FirstOrderIntegrator
 * @see fr.cnes.sirius.patrius.math.ode.SecondOrderIntegrator
 * @see StepInterpolator
 * @version $Id: StepHandler.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 1.2
 */

public interface StepHandler {

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
     * @param interpolator
     *        interpolator for the last accepted step. For
     *        efficiency purposes, the various integrators reuse the same
     *        object on each call, so if the instance wants to keep it across
     *        all calls (for example to provide at the end of the integration a
     *        continuous model valid throughout the integration range, as the
     *        {@link fr.cnes.sirius.patrius.math.ode.ContinuousOutputModel
     *        ContinuousOutputModel} class does), it should build a local copy
     *        using the clone method of the interpolator and store this copy.
     *        Keeping only a reference to the interpolator and reusing it will
     *        result in unpredictable behavior (potentially crashing the application).
     * @param isLast
     *        true if the step is the last one
     * @exception MaxCountExceededException
     *            if the interpolator throws one because
     *            the number of functions evaluations is exceeded
     */
    void handleStep(StepInterpolator interpolator, boolean isLast);

}
