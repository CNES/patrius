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
package fr.cnes.sirius.patrius.math.ode;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.MaxCountExceededException;

/**
 * Interface expanding {@link FirstOrderDifferentialEquations first order
 * differential equations} in order to compute exactly the main state jacobian
 * matrix for {@link JacobianMatrices partial derivatives equations}.
 * 
 * @version $Id: MainStateJacobianProvider.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.0
 */
public interface MainStateJacobianProvider extends FirstOrderDifferentialEquations {

    /**
     * Compute the jacobian matrix of ODE with respect to main state.
     * 
     * @param t
     *        current value of the independent <I>time</I> variable
     * @param y
     *        array containing the current value of the main state vector
     * @param yDot
     *        array containing the current value of the time derivative of the main state vector
     * @param dFdY
     *        placeholder array where to put the jacobian matrix of the ODE w.r.t. the main state vector
     * @exception MaxCountExceededException
     *            if the number of functions evaluations is exceeded
     * @exception DimensionMismatchException
     *            if arrays dimensions do not match equations settings
     */
    void computeMainStateJacobian(double t, double[] y, double[] yDot, double[][] dFdY);

}
