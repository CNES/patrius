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
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.ode.nonstiff;

/**
 * This class is used in the junit tests for the ODE integrators.
 * 
 * <p>
 * This specific problem is the following differential equation :
 * 
 * <pre>
 *    y' = 3x^5 - y
 * </pre>
 * 
 * when the initial condition is y(0) = -360, the solution of this equation degenerates to a simple quintic polynomial
 * function :
 * 
 * <pre>
 *   y (t) = 3x^5 - 15x^4 + 60x^3 - 180x^2 + 360x - 360
 * </pre>
 * 
 * </p>
 */
public class TestProblem6
    extends TestProblemAbstract {

    /** theoretical state */
    private final double[] y;

    /**
     * Simple constructor.
     */
    public TestProblem6() {
        super();
        final double[] y0 = { -360.0 };
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
    public TestProblem6(final TestProblem6 problem) {
        super(problem);
        this.y = problem.y.clone();
    }

    /** {@inheritDoc} */
    @Override
    public TestProblem6 copy() {
        return new TestProblem6(this);
    }

    @Override
    public void doComputeDerivatives(final double t, final double[] y, final double[] yDot) {

        // compute the derivatives
        final double t2 = t * t;
        final double t4 = t2 * t2;
        final double t5 = t4 * t;
        for (int i = 0; i < this.n; ++i) {
            yDot[i] = 3 * t5 - y[i];
        }

    }

    @Override
    public double[] computeTheoreticalState(final double t) {
        for (int i = 0; i < this.n; ++i) {
            this.y[i] = ((((3 * t - 15) * t + 60) * t - 180) * t + 360) * t - 360;
        }
        return this.y;
    }

}
