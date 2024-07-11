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

import java.util.ArrayList;
import java.util.Collection;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.MaxCountExceededException;

/**
 * Wrapper class enabling {@link FirstOrderDifferentialEquations basic simple} ODE instances to be used when processing
 * {@link JacobianMatrices}.
 * 
 * @version $Id: ParameterizedWrapper.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.0
 */
class ParameterizedWrapper implements ParameterizedODE {

    /** Basic FODE without parameter. */
    private final FirstOrderDifferentialEquations fode;

    /**
     * Simple constructor.
     * 
     * @param ode
     *        original first order differential equations
     */
    public ParameterizedWrapper(final FirstOrderDifferentialEquations ode) {
        this.fode = ode;
    }

    /**
     * Get the dimension of the underlying FODE.
     * 
     * @return dimension of the underlying FODE
     */
    public int getDimension() {
        return this.fode.getDimension();
    }

    /**
     * Get the current time derivative of the state vector of the underlying FODE.
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
    public void computeDerivatives(final double t, final double[] y, final double[] yDot) {
        this.fode.computeDerivatives(t, y, yDot);
    }

    /** {@inheritDoc} */
    @Override
    public Collection<String> getParametersNames() {
        return new ArrayList<String>();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isSupported(final String name) {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public double getParameter(final String name) {
        if (!this.isSupported(name)) {
            throw new UnknownParameterException(name);
        }
        return Double.NaN;
    }

    /** {@inheritDoc} */
    @Override
    public void setParameter(final String name, final double value) {
        // Nothing to do
    }
}
