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
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.11.1:FA:FA-69:30/06/2023:[PATRIUS] Amélioration de la gestion des attractions gravitationnelles dans le
 * propagateur
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.ode;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.MaxCountExceededException;
import fr.cnes.sirius.patrius.math.exception.NoBracketingException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;

/**
 * This interface represents a first order integrator for
 * differential equations.
 * 
 * <p>
 * The classes which are devoted to solve first order differential equations should implement this interface. The
 * problems which can be handled should implement the {@link FirstOrderDifferentialEquations} interface.
 * </p>
 * 
 * @see FirstOrderDifferentialEquations
 * @see fr.cnes.sirius.patrius.math.ode.sampling.StepHandler
 * @see fr.cnes.sirius.patrius.math.ode.events.EventHandler
 * @version $Id: FirstOrderIntegrator.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 1.2
 */

public interface FirstOrderIntegrator extends ODEIntegrator, Serializable {

    /**
     * Integrate the differential equations up to the given time.
     * <p>
     * This method solves an Initial Value Problem (IVP).
     * </p>
     * <p>
     * Since this method stores some internal state variables made available in its public interface during integration
     * ({@link #getCurrentSignedStepsize()}), it is <em>not</em> thread-safe.
     * </p>
     * 
     * @param equations
     *        differential equations to integrate
     * @param t0
     *        initial time
     * @param y0
     *        initial value of the state vector at t0
     * @param t
     *        target time for the integration
     *        (can be set to a value smaller than <code>t0</code> for backward integration)
     * @param y
     *        placeholder where to put the state vector at each successful
     *        step (and hence at the end of integration), can be the same object as y0
     * @return stop time, will be the same as target time if integration reached its
     *         target, but may be different if some {@link fr.cnes.sirius.patrius.math.ode.events.EventHandler} stops it
     *         at some point.
     * @exception DimensionMismatchException
     *            if arrays dimension do not match equations settings
     * @exception NumberIsTooSmallException
     *            if integration step is too small
     * @exception MaxCountExceededException
     *            if the number of functions evaluations is exceeded
     * @exception NoBracketingException
     *            if the location of an event cannot be bracketed
     */
    double integrate(FirstOrderDifferentialEquations equations,
                     double t0, double[] y0, double t, double[] y);

}
