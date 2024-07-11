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
package fr.cnes.sirius.patrius.math.ode.nonstiff;

import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * This class implements the Gill fourth order Runge-Kutta
 * integrator for Ordinary Differential Equations .
 * 
 * <p>
 * This method is an explicit Runge-Kutta method, its Butcher-array is the following one :
 * 
 * <pre>
 *    0  |    0        0       0      0
 *   1/2 |   1/2       0       0      0
 *   1/2 | (q-1)/2  (2-q)/2    0      0
 *    1  |    0       -q/2  (2+q)/2   0
 *       |-------------------------------
 *       |   1/6    (2-q)/6 (2+q)/6  1/6
 * </pre>
 * 
 * where q = sqrt(2)
 * </p>
 * 
 * @see EulerIntegrator
 * @see ClassicalRungeKuttaIntegrator
 * @see MidpointIntegrator
 * @see ThreeEighthesIntegrator
 * @version $Id: GillIntegrator.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 1.2
 */

public class GillIntegrator extends RungeKuttaIntegrator {

    /** Time steps Butcher array. */
    private static final double[] STATIC_C = {
        1.0 / 2.0, 1.0 / 2.0, 1.0
    };

    /** Internal weights Butcher array. */
    private static final double[][] STATIC_A = {
        { 1.0 / 2.0 },
        { (MathLib.sqrt(2.0) - 1.0) / 2.0, (2.0 - MathLib.sqrt(2.0)) / 2.0 },
        { 0.0, -MathLib.sqrt(2.0) / 2.0, (2.0 + MathLib.sqrt(2.0)) / 2.0 }
    };

    /** Propagation weights Butcher array. */
    private static final double[] STATIC_B = {
        1.0 / 6.0, (2.0 - MathLib.sqrt(2.0)) / 6.0, (2.0 + MathLib.sqrt(2.0)) / 6.0, 1.0 / 6.0
    };

    /**
     * Simple constructor.
     * Build a fourth-order Gill integrator with the given step.
     * 
     * @param step
     *        integration step
     */
    public GillIntegrator(final double step) {
        super("Gill", STATIC_C, STATIC_A, STATIC_B, new GillStepInterpolator(), step);
    }

}
