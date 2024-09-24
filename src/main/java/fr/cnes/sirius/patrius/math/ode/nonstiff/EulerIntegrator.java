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
package fr.cnes.sirius.patrius.math.ode.nonstiff;

/**
 * This class implements a simple Euler integrator for Ordinary
 * Differential Equations.
 * 
 * <p>
 * The Euler algorithm is the simplest one that can be used to integrate ordinary differential equations. It is a simple
 * inversion of the forward difference expression : <code>f'=(f(t+h)-f(t))/h</code> which leads to
 * <code>f(t+h)=f(t)+hf'</code>. The interpolation scheme used for dense output is the linear scheme already used for
 * integration.
 * </p>
 * 
 * <p>
 * This algorithm looks cheap because it needs only one function evaluation per step. However, as it uses linear
 * estimates, it needs very small steps to achieve high accuracy, and small steps lead to numerical errors and
 * instabilities.
 * </p>
 * 
 * <p>
 * This algorithm is almost never used and has been included in this package only as a comparison reference for more
 * useful integrators.
 * </p>
 * 
 * @see MidpointIntegrator
 * @see ClassicalRungeKuttaIntegrator
 * @see GillIntegrator
 * @see ThreeEighthesIntegrator
 * @version $Id: EulerIntegrator.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 1.2
 */

public class EulerIntegrator extends RungeKuttaIntegrator {

    /** Time steps Butcher array. */
    private static final double[] STATIC_C = {
        };

    /** Internal weights Butcher array. */
    private static final double[][] STATIC_A = {
        };

    /** Propagation weights Butcher array. */
    private static final double[] STATIC_B = {
        1.0
    };

    /**
     * Simple constructor.
     * Build an Euler integrator with the given step.
     * 
     * @param step
     *        integration step
     */
    public EulerIntegrator(final double step) {
        super("Euler", STATIC_C, STATIC_A, STATIC_B, new EulerStepInterpolator(), step);
    }

}
