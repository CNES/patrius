/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 *
 * Copyright 2011-2017 CNES
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
 */
package fr.cnes.sirius.patrius.math.ode;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.MaxCountExceededException;

/**
 * This interface allows users to add secondary differential equations to a primary
 * set of differential equations.
 * <p>
 * In some cases users may need to integrate some problem-specific equations along with a primary set of differential
 * equations. One example is optimal control where adjoined parameters linked to the minimized hamiltonian must be
 * integrated.
 * </p>
 * <p>
 * This interface allows users to add such equations to a primary set of {@link FirstOrderDifferentialEquations first
 * order differential equations} thanks to the {@link ExpandableStatefulODE#addSecondaryEquations(SecondaryEquations)}
 * method.
 * </p>
 * 
 * @see ExpandableStatefulODE
 * @version $Id: SecondaryEquations.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.0
 */
public interface SecondaryEquations {

    /**
     * Get the dimension of the secondary state parameters.
     * 
     * @return dimension of the secondary state parameters
     */
    int getDimension();

    /**
     * Compute the derivatives related to the secondary state parameters.
     * 
     * @param t
     *        current value of the independent <I>time</I> variable
     * @param primary
     *        array containing the current value of the primary state vector
     * @param primaryDot
     *        array containing the derivative of the primary state vector
     * @param secondary
     *        array containing the current value of the secondary state vector
     * @param secondaryDot
     *        placeholder array where to put the derivative of the secondary state vector
     * @exception MaxCountExceededException
     *            if the number of functions evaluations is exceeded
     * @exception DimensionMismatchException
     *            if arrays dimensions do not match equations settings
     */
    void computeDerivatives(double t, double[] primary, double[] primaryDot,
                            double[] secondary, double[] secondaryDot);

}
