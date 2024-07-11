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
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.ode.nonstiff;

import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * This class is used in the junit tests for the ODE integrators.
 * 
 * <p>
 * This specific problem is the following differential equation :
 * 
 * <pre>
 *    y' = t^3 - t y
 * </pre>
 * 
 * with the initial condition y (0) = 0. The solution of this equation is the following function :
 * 
 * <pre>
 *   y (t) = t^2 + 2 (exp (- t^2 / 2) - 1)
 * </pre>
 * 
 * </p>
 */
public class TestProblem2
    extends TestProblemAbstract {

    /** theoretical state */
    private final double[] y;

    /**
     * Simple constructor.
     */
    public TestProblem2() {
        super();
        final double[] y0 = { 0.0 };
        this.setInitialConditions(0.0, y0);
        this.setFinalConditions(1.0);
        final double[] errorScale = { 1.0 };
        this.setErrorScale(errorScale);
        this.y = new double[y0.length];
    }

    /**
     * Copy constructor.
     * 
     * @param problem
     *        problem to copy
     */
    public TestProblem2(final TestProblem2 problem) {
        super(problem);
        this.y = problem.y.clone();
    }

    /** {@inheritDoc} */
    @Override
    public TestProblem2 copy() {
        return new TestProblem2(this);
    }

    @Override
    public void doComputeDerivatives(final double t, final double[] y, final double[] yDot) {

        // compute the derivatives
        for (int i = 0; i < this.n; ++i) {
            yDot[i] = t * (t * t - y[i]);
        }

    }

    @Override
    public double[] computeTheoreticalState(final double t) {
        final double t2 = t * t;
        final double c = t2 + 2 * (MathLib.exp(-0.5 * t2) - 1);
        for (int i = 0; i < this.n; ++i) {
            this.y[i] = c;
        }
        return this.y;
    }

}
