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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Wrapper class to compute Jacobian matrices by finite differences for ODE
 * which do not compute them by themselves.
 * 
 * @version $Id: ParameterJacobianWrapper.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.0
 */
class ParameterJacobianWrapper implements ParameterJacobianProvider {

    /** Main ODE set. */
    private final FirstOrderDifferentialEquations fode;

    /** Raw ODE without Jacobian computation skill to be wrapped into a ParameterJacobianProvider. */
    private final ParameterizedODE pode;

    /** Steps for finite difference computation of the Jacobian df/dp w.r.t. parameters. */
    private final Map<String, Double> hParam;

    /**
     * Wrap a {@link ParameterizedODE} into a {@link ParameterJacobianProvider}.
     * 
     * @param fodeIn
     *        main first order differential equations set
     * @param podeIn
     *        secondary problem, without parameter Jacobian computation skill
     * @param paramsAndSteps
     *        parameters and steps to compute the Jacobians df/dp
     * @see JacobianMatrices#setParameterStep(String, double)
     */
    public ParameterJacobianWrapper(final FirstOrderDifferentialEquations fodeIn,
        final ParameterizedODE podeIn,
        final ParameterConfiguration[] paramsAndSteps) {
        this.fode = fodeIn;
        this.pode = podeIn;
        this.hParam = new HashMap<String, Double>();

        // set up parameters for jacobian computation
        for (final ParameterConfiguration param : paramsAndSteps) {
            final String name = param.getParameterName();
            if (podeIn.isSupported(name)) {
                this.hParam.put(name, param.getHP());
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public Collection<String> getParametersNames() {
        return this.pode.getParametersNames();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isSupported(final String name) {
        return this.pode.isSupported(name);
    }

    /** {@inheritDoc} */
    @Override
    public void computeParameterJacobian(final double t, final double[] y, final double[] yDot,
                                         final String paramName, final double[] dFdP) {

        final int n = this.fode.getDimension();
        if (this.pode.isSupported(paramName)) {
            // Parameter is supported
            final double[] tmpDot = new double[n];

            // compute the jacobian df/dp w.r.t. parameter
            final double p = this.pode.getParameter(paramName);
            final double hP = this.hParam.get(paramName);
            this.pode.setParameter(paramName, p + hP);
            this.fode.computeDerivatives(t, y, tmpDot);
            for (int i = 0; i < n; ++i) {
                dFdP[i] = (tmpDot[i] - yDot[i]) / hP;
            }
            this.pode.setParameter(paramName, p);
        } else {
            // Not supported, no computation
            Arrays.fill(dFdP, 0, n, 0.0);
        }

    }

}
