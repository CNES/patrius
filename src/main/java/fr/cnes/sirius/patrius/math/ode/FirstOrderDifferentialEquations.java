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
package fr.cnes.sirius.patrius.math.ode;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.MaxCountExceededException;

/**
 * This interface represents a first order differential equations set.
 * 
 * <p>
 * This interface should be implemented by all real first order differential equation problems before they can be
 * handled by the integrators {@link FirstOrderIntegrator#integrate} method.
 * </p>
 * 
 * <p>
 * A first order differential equations problem, as seen by an integrator is the time derivative <code>dY/dt</code> of a
 * state vector <code>Y</code>, both being one dimensional arrays. From the integrator point of view, this derivative
 * depends only on the current time <code>t</code> and on the state vector <code>Y</code>.
 * </p>
 * 
 * <p>
 * For real problems, the derivative depends also on parameters that do not belong to the state vector (dynamical model
 * constants for example). These constants are completely outside of the scope of this interface, the classes that
 * implement it are allowed to handle them as they want.
 * </p>
 * 
 * @see FirstOrderIntegrator
 * @see FirstOrderConverter
 * @see SecondOrderDifferentialEquations
 * 
 * @version $Id: FirstOrderDifferentialEquations.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 1.2
 */

public interface FirstOrderDifferentialEquations {

    /**
     * Get the dimension of the problem.
     * 
     * @return dimension of the problem
     */
    int getDimension();

    /**
     * Get the current time derivative of the state vector.
     * 
     * @param t
     *        current value of the independent <I>time</I> variable
     * @param y
     *        array containing the current value of the state vector
     * @param yDot
     *        placeholder array where to put the time derivative of the state vector
     * @exception MaxCountExceededException
     *            if the number of functions evaluations is exceeded
     * @exception DimensionMismatchException
     *            if arrays dimensions do not match equations settings
     */
    void computeDerivatives(double t, double[] y, double[] yDot);

}
